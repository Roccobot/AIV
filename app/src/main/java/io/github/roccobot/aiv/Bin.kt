package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Il cestino: le fotografie eliminate ci restano finché non lo si svuota.
 *
 * ⚠️⚠️ **NASCE DALLA 0.64, e cambia il significato di 'elimina'** (richiesta dell'utente,
 * 2026-08-30): fuori dal cestino eliminare vuol dire **spostare qui**, e solo qui dentro
 * vuol dire cancellare. La conferma che diceva *vanno via dal telefono per sempre* non era
 * più vera e non lo dice più.
 *
 * ⚠️⚠️ **STA DENTRO LA CARTELLA DELL'APP, e la scelta risolve da sé metà della richiesta**:
 * `Android/data/<app>/files/bin` **il MediaStore non lo indicizza affatto**, per costruzione
 * del sistema. Quindi il cestino non compare nell'elenco delle cartelle, non compare fra le
 * miniature di nessuna cartella e non compare nei risultati di una ricerca, senza che
 * nessuno debba filtrarlo da nessuna parte. Un `/sdcard/.cestino` avrebbe voluto un filtro
 * in tre punti, e il giorno che uno dei tre si dimentica le foto eliminate ricompaiono nella
 * galleria.
 * ⚠️ **Il costo dichiarato**: quella cartella è dell'app, quindi **disinstallare l'app
 * svuota il cestino**. È il comportamento che si attende da un cestino di un'app e non da
 * uno di sistema, e va saputo.
 * ⚠️ **Conseguenza tecnica, non evidente**: là dentro il MediaStore non vede niente, quindi
 * il cestino **non si può elencare** con una query come fa una cartella qualunque. Si elenca
 * leggendo la cartella, e le sue foto viaggiano come indirizzi `file://` invece di
 * `content://`. Tutto il resto dell'app li tratta uguale, ed è la ragione per cui la griglia,
 * il visualizzatore e le operazioni funzionano qui senza un ramo apposta.
 *
 * ⚠️⚠️ **LA PROVENIENZA SI SCRIVE, perché da un file spostato non si ricava**: 'ripristina'
 * deve riportare ogni fotografia **nella cartella dove stava**, e quel percorso non esiste
 * più da nessuna parte appena il file si è mosso. Vive in un archivio accanto al cestino,
 * una riga per file (vedi [INDEX]).
 */
object Bin {

    /**
     * Dove sta il cestino. **Solo il percorso**: non crea niente.
     *
     * ⚠️⚠️ **IL PERCORSO SI CALCOLA UNA VOLTA SOLA, e non è un'ottimizzazione da manuale**:
     * questa funzione la chiama anche `DestinationDialog` **durante la composizione**, per
     * spegnere il tasto quando si è dentro il cestino, e `getExternalFilesDir` tocca il
     * disco (per giunta creando la cartella se manca). Senza la memoria, ogni ricomposizione
     * pagherebbe un accesso al filesystem sul thread dell'interfaccia.
     * ⚠️ **Non crea la cartella**: a crearla è [ready], cioè le operazioni che ci devono
     * scrivere. Un elenco su una cartella che non esiste torna vuoto da sé, che è la
     * risposta giusta per un cestino mai usato.
     * ⚠️ **Il ripiego sulla memoria interna** serve al caso in cui la memoria condivisa non
     * ci sia (non capita su un telefono, capita su un emulatore senza scheda): meglio un
     * cestino che funziona in un posto meno comodo che un'eliminazione che non si può fare.
     */
    fun dir(context: Context): File = known ?: run {
        val home = context.getExternalFilesDir(null) ?: context.filesDir
        File(home, FOLDER).also { known = it }
    }

    /** La cartella del cestino, creata se manca: per chi ci deve scrivere. */
    private fun ready(context: Context): File = dir(context).also { runCatching { it.mkdirs() } }

    /** Vedi [dir]: il percorso non cambia mai per tutta la vita del processo. */
    @Volatile
    private var known: File? = null

    /**
     * Se quel percorso è il cestino, o sta dentro il cestino.
     *
     * ⚠️ **Il confronto è sul separatore**, come per le cartelle escluse: con un
     * `startsWith` nudo una cartella chiamata `bin2` accanto alla nostra risulterebbe
     * dentro di lei.
     */
    fun holds(context: Context, dir: File): Boolean {
        val own = dir(context).absolutePath
        val path = dir.absolutePath
        return path == own || path.startsWith("$own/")
    }

    /**
     * Manda nel cestino le immagini scelte, ricordandosi da dove venivano.
     *
     * ⚠️⚠️ **UN FILE SENZA PERCORSO NON PUÒ ANDARE NEL CESTINO, e fallisce invece di
     * essere cancellato**: una fotografia arrivata da una chat non ha un file che l'app
     * possa spostare, e trattare quel caso come 'elimina per sempre' vorrebbe dire che lo
     * stesso gesto a volte è reversibile e a volte no, senza dirlo.
     * ⚠️⚠️ **UN PERCORSO CON UN CARATTERE DI TABULAZIONE O UN RITORNO A CAPO SI RIFIUTA**,
     * e il file resta dov'è: l'archivio della provenienza è un file di righe e colonne, e
     * quei due caratteri sono i suoi separatori. Meglio un'eliminazione che non avviene di
     * una che avviene senza poter più tornare indietro. Sono nomi che nessun telefono
     * scrive, ma un file arriva anche da fuori.
     * ⚠️ **Il nome se lo sceglie [FileTree.freeName]**, quindi due foto con lo stesso nome
     * convivono nel cestino: è la ragione per cui l'archivio è indicizzato sul nome **nel
     * cestino** e porta il percorso d'origine per intero.
     * ⚠️ **Si dice al MediaStore che le origini non ci sono più**, o la galleria (e la
     * griglia di questa app) continuerebbe a mostrare miniature di file spostati.
     */
    suspend fun send(context: Context, uris: List<Uri>): FileTree.Outcome =
        withContext(Dispatchers.IO + NonCancellable) {
            lock.withLock {
                val bin = ready(context)
                val records = read(context).toMutableList()
                val touched = ArrayList<String>(uris.size)
                var done = 0
                var failed = 0
                val now = System.currentTimeMillis()
                for (uri in uris) {
                    val from = FileTree.fileOf(context, uri)
                    if (from == null || from.absolutePath.hasSeparators()) {
                        failed++
                        continue
                    }
                    val to = FileTree.freeName(bin, from.name)
                    if (FileTree.carry(from, to)) {
                        done++
                        touched += from.absolutePath
                        records += Record(to.name, now, from.absolutePath)
                    } else {
                        failed++
                    }
                }
                write(context, records)
                FileTree.scan(context, touched)
                FileTree.Outcome(done, failed)
            }
        }

    /**
     * Riporta ogni immagine nella cartella dove stava.
     *
     * ⚠️⚠️ **SENZA LA PROVENIENZA NON SI RIPRISTINA, e non si indovina**: un file che si
     * trova nel cestino senza una riga d'archivio (ci è finito a mano, o l'archivio si è
     * perso) fallisce. Rimetterlo 'da qualche parte' sarebbe peggio: chi non lo trova più
     * penserebbe che sia stato cancellato.
     * ⚠️ **La cartella d'origine si ricrea se non c'è più**: nel tempo passato nel cestino
     * qualcuno può averla cancellata, e il ripristino non deve fallire per quello.
     * ⚠️ **Il nome torna quello di prima**, anche se nel cestino era stato cambiato per non
     * sovrapporsi a un altro: l'archivio porta il percorso originale per intero, ed è
     * esattamente per questo.
     * ⚠️ **Le destinazioni si fanno scandire**, o la fotografia ripristinata resterebbe
     * invisibile alla galleria fino al prossimo giro dello scanner di sistema.
     */
    suspend fun restore(context: Context, uris: List<Uri>): FileTree.Outcome =
        withContext(Dispatchers.IO + NonCancellable) {
            lock.withLock {
                val records = read(context).toMutableList()
                val byName = records.associateBy { it.name }
                val touched = ArrayList<String>(uris.size)
                var done = 0
                var failed = 0
                for (uri in uris) {
                    val from = FileTree.fileOf(context, uri)
                    val record = from?.let { byName[it.name] }
                    val target = record?.origin?.let { File(it) }
                    val parent = target?.parentFile
                    if (from == null || target == null || parent == null) {
                        failed++
                        continue
                    }
                    runCatching { parent.mkdirs() }
                    val to = FileTree.freeName(parent, target.name)
                    if (FileTree.carry(from, to)) {
                        done++
                        touched += to.absolutePath
                        records -= record
                    } else {
                        failed++
                    }
                }
                write(context, records)
                FileTree.scan(context, touched)
                FileTree.Outcome(done, failed)
            }
        }

    /**
     * Svuota il cestino: cancella tutto, per davvero.
     *
     * ⚠️ **L'archivio si azzera insieme ai file e non prima**: se qualche cancellazione
     * fallisce, le righe di quei file restano, o resterebbero nel cestino senza poter più
     * essere ripristinate.
     */
    suspend fun empty(context: Context): FileTree.Outcome =
        withContext(Dispatchers.IO + NonCancellable) {
            lock.withLock {
                val bin = ready(context)
                val files = runCatching { bin.listFiles() }.getOrNull().orEmpty().filter { it.isFile }
                var done = 0
                var failed = 0
                val left = read(context).toMutableList()
                for (file in files) {
                    if (runCatching { file.delete() }.getOrDefault(false)) {
                        done++
                        left.removeAll { it.name == file.name }
                    } else {
                        failed++
                    }
                }
                write(context, left)
                FileTree.Outcome(done, failed)
            }
        }

    /**
     * Che cosa c'è nel cestino, dall'ultima eliminata alla prima.
     *
     * ⚠️⚠️ **L'ORDINE VIENE DALL'ARCHIVIO E NON DALLA DATA DEL FILE**: uno spostamento non
     * cambia la data di modifica, quindi ordinare per quella darebbe l'ordine in cui le
     * fotografie sono state **scattate**, non quello in cui sono state eliminate. In un
     * cestino l'ultima buttata è quella che si cerca.
     * ⚠️ **I file senza riga d'archivio stanno in fondo**, in ordine di nome: non si sa
     * quando sono arrivati, e metterli in mezzo sarebbe inventare una data.
     * ⚠️ Indirizzi `file://`: là dentro il MediaStore non vede niente. Vedi la nota in
     * testa a questo oggetto.
     */
    suspend fun list(context: Context): List<Uri> = withContext(Dispatchers.IO) {
        val files = runCatching { dir(context).listFiles() }.getOrNull().orEmpty()
            .filter { it.isFile }
            .associateBy { it.name }
        ordered(files.keys.toList(), read(context)).mapNotNull { files[it]?.toUri() }
    }

    /**
     * L'ordine del cestino: prima le eliminate di recente, poi quelle di cui non si sa
     * niente.
     *
     * ⚠️ **Funzione pura, e non è un vezzo**: prende dei nomi e restituisce dei nomi,
     * quindi si prova su una JVM normale senza Android intorno, ed è precisamente così che
     * è stata verificata. La stessa ragione per cui il tokenizzatore di CLIP non sa niente
     * di file.
     * ⚠️ **Le righe che descrivono file spariti si saltano**: l'archivio può parlare di
     * roba che non c'è più (cancellata a mano, o un'eliminazione a metà), e non deve far
     * comparire buchi nella griglia.
     */
    fun ordered(names: List<String>, records: List<Record>): List<String> {
        val there = names.toSet()
        val known = LinkedHashSet<String>(names.size)
        for (record in records.sortedByDescending { it.at }) {
            if (record.name in there) known += record.name
        }
        return known.toList() + names.filterNot { it in known }.sortedBy { it.lowercase() }
    }

    /** Una riga d'archivio: come si chiama qui, quando è arrivata, da dove veniva. */
    class Record(val name: String, val at: Long, val origin: String)

    /**
     * Le righe di un archivio, saltando quelle rovinate.
     *
     * ⚠️ **Una riga per file, colonne separate da tabulazione**, come gli appunti di
     * `Recents`: nessuna libreria, nessuna dipendenza, e un file che si legge a occhio se un
     * giorno qualcosa non torna.
     * ⚠️ **Le righe malformate si scartano** invece di far fallire la lettura: un archivio
     * rovinato a metà deve far perdere i file che descriveva, non tutti gli altri.
     * ⚠️ Pura come [ordered], e per la stessa ragione.
     */
    fun records(text: String): List<Record> = text.lineSequence().mapNotNull { line ->
        val parts = line.split('\t')
        if (parts.size != 3) return@mapNotNull null
        val at = parts[1].toLongOrNull() ?: return@mapNotNull null
        if (parts[0].isBlank() || parts[2].isBlank()) return@mapNotNull null
        Record(parts[0], at, parts[2])
    }.toList()

    /** L'archivio come testo. L'inversa di [records]. */
    fun text(records: List<Record>): String =
        records.joinToString("\n") { "${it.name}\t${it.at}\t${it.origin}" }

    private fun read(context: Context): List<Record> {
        val file = index(context)
        return records(runCatching { if (file.isFile) file.readText() else "" }.getOrDefault(""))
    }

    private fun write(context: Context, records: List<Record>) {
        runCatching { index(context).writeText(text(records)) }
    }

    /**
     * L'archivio delle provenienze, **accanto** al cestino e non dentro.
     *
     * ⚠️ Dentro sarebbe un file in più che l'elenco dovrebbe filtrare, e prima o poi
     * comparirebbe fra le miniature come una fotografia rotta.
     */
    private fun index(context: Context): File = File(dir(context).parentFile, INDEX)

    /** Se un testo contiene i separatori dell'archivio. Vedi [send]. */
    private fun String.hasSeparators(): Boolean = contains('\t') || contains('\n')

    private const val FOLDER = "bin"
    private const val INDEX = "bin.tsv"

    /**
     * ⚠️ **Le tre operazioni non si sovrappongono**: leggono e riscrivono lo stesso
     * archivio, e due che si intrecciassero perderebbero le righe dell'altra. Non è
     * teorico: 'svuota' su un cestino grosso dura, e nel frattempo l'interfaccia resta
     * viva.
     */
    private val lock = Mutex()
}

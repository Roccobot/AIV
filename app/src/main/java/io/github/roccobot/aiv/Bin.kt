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
 * Il cestino: le immagini eliminate restano qui finché non lo si svuota. Fuori dal cestino
 * eliminare vuol dire spostare qui, e solo qui dentro vuol dire cancellare.
 *
 * ⚠️ **È nella cartella dell'app** (`Android/data/<app>/files/bin`), che il MediaStore non
 * indicizza per costruzione: il cestino non compare fra le cartelle, le miniature e le ricerche
 * senza nessun filtro. Il costo: disinstallare l'app svuota il cestino. E il cestino non si elenca
 * con una query ma leggendo la cartella, quindi i suoi file viaggiano come `file://`, che il resto
 * dell'app tratta come i `content://`.
 *
 * ⚠️ **La provenienza si scrive**, perché da un file spostato non si ricava: 'ripristina' riporta
 * ogni immagine nella sua cartella, e quel percorso vive in un archivio accanto al cestino, una
 * riga per file (vedi [INDEX]).
 */
object Bin {

    /**
     * Dov'è il cestino: solo il percorso, senza creare niente.
     *
     * Il percorso si calcola una volta sola: `DestinationDialog` chiama [holds] durante la
     * composizione, e `getExternalFilesDir` tocca il disco (e crea la cartella se manca). A crearla
     * è [ready], per chi ci deve scrivere; un elenco su una cartella che non esiste torna vuoto. Il
     * ripiego sulla memoria interna serve quando la memoria condivisa manca (un emulatore senza
     * scheda).
     */
    fun dir(context: Context): File = known ?: run {
        val home = context.getExternalFilesDir(null) ?: context.filesDir
        File(home, FOLDER).also { known = it }
    }

    /** La cartella del cestino, creata se manca: per chi ci deve scrivere. */
    private fun ready(context: Context): File = dir(context).also { runCatching { it.mkdirs() } }

    /**
     * Dice che a questo percorso del cestino il contenuto è cambiato: un file è arrivato, o se n'è
     * andato.
     *
     * ⚠️ **`FileTree.scan` qui non arriva**: il MediaStore non indicizza il cestino, quindi la
     * chiamata a [Thumbs.forget] dentro `scan` non si fa mai per un file di qui. E i percorsi si
     * riusano ([FileTree.freeName]): uscito un `foto.jpg`, il prossimo `foto.jpg` ritrova lo stesso
     * indirizzo, e la cache di Coil, che ha l'indirizzo per chiave, servirebbe la miniatura
     * vecchia. Si chiama quando un file arriva e quando se ne va.
     */
    private fun touch(context: Context, file: File) = Thumbs.forget(context, file.toUri())

    /** Vedi [dir]: il percorso non cambia per tutta la durata del processo. */
    @Volatile
    private var known: File? = null

    /**
     * Se quel percorso è il cestino, o è dentro il cestino. Il confronto è sul separatore, come per
     * le cartelle escluse: con un `startsWith` nudo una cartella `bin2` accanto risulterebbe
     * dentro.
     */
    fun holds(context: Context, dir: File): Boolean {
        val own = dir(context).absolutePath
        val path = dir.absolutePath
        return path == own || path.startsWith("$own/")
    }

    /**
     * Manda nel cestino le immagini scelte, ricordando da dove venivano.
     *
     * ⚠️ **Un file senza percorso non va nel cestino, e fallisce**: un'immagine arrivata da una
     * chat non ha un file da spostare, e cancellarla per sempre renderebbe lo stesso gesto a volte
     * reversibile e a volte no.
     * ⚠️ **Un percorso con una tabulazione o un ritorno a capo si rifiuta**, e il file resta dov'è:
     * sono i separatori dell'archivio. Nessun telefono li scrive, ma un file arriva anche da fuori.
     * Il nome lo sceglie [FileTree.freeName], quindi due file con lo stesso nome convivono: per
     * questo l'archivio è indicizzato sul nome nel cestino e contiene il percorso d'origine intero.
     * Le origini si dichiarano sparite al MediaStore, o la galleria mostrerebbe file spostati.
     */
    suspend fun send(context: Context, uris: List<Uri>): Sent =
        withContext(Dispatchers.IO + NonCancellable) {
            lock.withLock {
                val bin = ready(context)
                val records = read(context).toMutableList()
                val moved = ArrayList<Pair<File, File>>(uris.size)
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
                        touch(context, to)
                        moved += from to to
                        records += Record(to.name, now, from.absolutePath, KIND_SENT)
                    } else {
                        failed++
                    }
                }
                // ⚠️ **Senza l'archivio scritto i file tornano a casa** (rilievo E12): nel cestino
                // senza la loro riga non si potrebbero ripristinare. Quello che non riesce a
                // tornare resta nel cestino e si conta come riuscito, perché è comunque là.
                if (moved.isNotEmpty() && !write(context, records)) {
                    val back = moved.filter { (from, to) -> FileTree.carry(to, from) }
                    back.forEach { (_, to) -> touch(context, to) }
                    failed += back.size
                    moved.removeAll(back.toSet())
                }
                FileTree.scan(context, moved.map { it.first.absolutePath })
                Sent(FileTree.Outcome(moved.size, failed), moved.map { it.second.toUri() })
            }
        }

    /**
     * Che cosa ha fatto un'eliminazione: com'è andata, e dove sono finiti i file. Il secondo dato
     * si sa solo qui: l'offerta di disfare (vedi [Undo]) chiama [restore] su quei file, e
     * [FileTree.freeName] può averne cambiato il nome. [landed] contiene i soli file passati
     * davvero, quindi il suo conto è già [FileTree.Outcome.done].
     */
    data class Sent(val outcome: FileTree.Outcome, val landed: List<Uri>)

    /**
     * Mette nel cestino una copia di [from], lasciando l'originale dov'è.
     *
     * ⚠️ **Copia e non sposta, al contrario di [send]**: il file sta per essere riscritto
     * dall'editor, e quello che si salva è la versione di prima. La riga d'archivio contiene il
     * percorso originale, quindi il ripristino sa dove rimettere la copia; se là c'è la versione
     * modificata, `restore` la mette accanto con [FileTree.freeName]. Niente `FileTree.scan`
     * sull'origine: l'originale non si è mosso, e l'editor scandisce comunque dopo aver scritto.
     *
     * @return la copia appena messa nel cestino, o `null` se non si è potuta fare. Restituisce il
     * file
     *   perché un chiamante deve riprenderlo: la copia fatta prima di un editor esterno si toglie
     *   se quell'editor non ha salvato niente, e il nome può essere cambiato.
     */
    suspend fun keep(context: Context, from: File): File? =
        withContext(Dispatchers.IO + NonCancellable) {
            if (from.absolutePath.hasSeparators()) return@withContext null
            lock.withLock {
                val to = FileTree.freeName(ready(context), from.name)
                val copied = runCatching {
                    from.inputStream().use { input -> to.outputStream().use { input.copyTo(it) } }
                    true
                }.getOrDefault(false)
                if (!copied) {
                    to.delete()
                    return@withLock null
                }
                touch(context, to)
                val records = read(context).toMutableList()
                records += Record(
                    to.name,
                    System.currentTimeMillis(),
                    from.absolutePath,
                    KIND_KEPT
                )
                // Una copia senza la sua riga non tornerebbe mai al suo posto: si dice che la copia
                // non c'è, e l'editor non salva sopra l'originale.
                if (!write(context, records)) {
                    to.delete()
                    touch(context, to)
                    return@withLock null
                }
                to
            }
        }

    /**
     * Toglie dal cestino una copia che si è rivelata inutile, file e riga d'archivio.
     *
     * ⚠️ **È il rimedio a una copia di troppo, non una cancellazione offerta all'utente**: si
     * chiama solo su una copia appena fatta da [keep] e identica all'originale. Un cestino che si
     * svuota da sé tradirebbe la promessa dell'interruttore. La riga se ne va anche se il file
     * resta: una riga che punta a un file cancellato manderebbe il ripristino in errore, mentre un
     * file orfano l'utente lo vede e lo toglie a mano.
     */
    suspend fun drop(context: Context, kept: File): Boolean =
        withContext(Dispatchers.IO + NonCancellable) {
            lock.withLock {
                val gone = runCatching { kept.delete() }.getOrDefault(false)
                if (gone) touch(context, kept)
                write(context, read(context).filterNot { it.name == kept.name })
                gone
            }
        }

    /**
     * Riporta ogni immagine nella cartella dov'era.
     *
     * ⚠️ **Senza la provenienza non si ripristina, e non si indovina**: un file senza riga
     * d'archivio fallisce, perché rimetterlo 'da qualche parte' lo farebbe credere cancellato. La
     * cartella d'origine si ricrea se non c'è più, e il nome torna quello di prima (l'archivio
     * contiene il percorso intero). Le destinazioni si fanno scandire, o la galleria non le
     * vedrebbe.
     * ⚠️ **Qui si scrive la cronologia** (vedi [History]), il solo posto che può farlo: la riga
     * d'archivio sparisce col ripristino, e tutte le vie del ripristino passano da qui.
     */
    suspend fun restore(context: Context, uris: List<Uri>): FileTree.Outcome =
        withContext(Dispatchers.IO + NonCancellable) {
            lock.withLock {
                val records = read(context).toMutableList()
                val byName = records.associateBy { it.name }
                val touched = ArrayList<String>(uris.size)
                var done = 0
                var failed = 0
                // Un istante solo per tutta l'operazione: è quello che fa un gruppo nella
                // cronologia.
                val now = System.currentTimeMillis()
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
                        touch(context, from)
                        touched += to.absolutePath
                        records -= record
                    } else {
                        failed++
                    }
                }
                write(context, records)
                History.add(context, now, touched)
                FileTree.scan(context, touched)
                FileTree.Outcome(done, failed)
            }
        }

    /**
     * Svuota il cestino: cancella tutto, per davvero.
     *
     * L'archivio si aggiorna insieme ai file: le righe di quelli che non si cancellano restano, o
     * non si potrebbero più ripristinare. Conta anche i byte liberati (richiesta dell'utente): il
     * peso si legge prima della cancellazione e si somma solo se riesce.
     */
    suspend fun empty(context: Context): FileTree.Outcome =
        withContext(Dispatchers.IO + NonCancellable) {
            lock.withLock {
                val bin = ready(context)
                val files = runCatching { bin.listFiles() }.getOrNull().orEmpty().filter { it.isFile }
                var done = 0
                var failed = 0
                var freed = 0L
                val left = read(context).toMutableList()
                for (file in files) {
                    val peso = runCatching { file.length() }.getOrDefault(0L)
                    if (runCatching { file.delete() }.getOrDefault(false)) {
                        done++
                        freed += peso
                        touch(context, file)
                        left.removeAll { it.name == file.name }
                    } else {
                        failed++
                    }
                }
                write(context, left)
                FileTree.Outcome(done, failed, freed = freed)
            }
        }

    /**
     * Che cosa c'è nel cestino, dall'ultima eliminata alla prima.
     *
     * ⚠️ **L'ordine viene dall'archivio e non dalla data del file**: uno spostamento non cambia la
     * data di modifica, che darebbe l'ordine degli scatti. I file senza riga vanno in fondo, in
     * ordine di nome. Indirizzi `file://`, come dice la nota in testa.
     *
     * ⚠️ **Qui l'archivio si pota**: l'eliminazione definitiva di una selezione passa da
     * `FileTree.delete`, che non sa niente di questo archivio, e nel cestino è la via normale. Si
     * pota qui, dove si ha già l'elenco dei file che esistono, così vale anche per una via d'uscita
     * futura; si riscrive il file solo se la potatura ha tolto qualcosa, come la purga di
     * `History.batches`.
     */
    suspend fun list(context: Context): List<Uri> = withContext(Dispatchers.IO) {
        val files = runCatching { dir(context).listFiles() }.getOrNull().orEmpty()
            .filter { it.isFile }
            .associateBy { it.name }
        val records = lock.withLock {
            val all = read(context)
            val kept = all.filter { it.name in files }
            if (kept.size != all.size) write(context, kept)
            kept
        }
        ordered(files.keys.toList(), records)
            .filterNot { MediaFiles.ignored(it) }
            .mapNotNull { files[it]?.toUri() }
    }

    /**
     * L'ordine del cestino: prima le eliminate di recente, poi quelle di cui non si sa niente. Le
     * righe che descrivono file spariti si saltano, per non avere buchi nella griglia. Funzione
     * pura, così si prova su una JVM senza Android.
     */
    fun ordered(names: List<String>, records: List<Record>): List<String> {
        val there = names.toSet()
        val known = LinkedHashSet<String>(names.size)
        for (record in records.sortedByDescending { it.at }) {
            if (record.name in there) known += record.name
        }
        return known.toList() + names.filterNot { it in known }.sortedBy { it.lowercase() }
    }

    /**
     * Una riga d'archivio: come si chiama qui, quando è arrivata, da dove veniva, e perché.
     *
     * ⚠️ **La quarta colonna distingue una copia di sicurezza da un'eliminazione**: le copie
     * dell'editor non scadono (scelta `fuori` dell'utente a `d-cestino-editor`). Guardare se
     * l'originale esiste non basta: chi modifica un'immagine e poi la elimina ha una copia di
     * sicurezza senza originale.
     * ⚠️ **`null` vuol dire riga del formato a tre colonne**, non 'eliminazione': si ricade sulla
     * prova dell'originale, che sbaglia solo nel verso di tenere un file più a lungo. Vedi
     * [expiring].
     */
    class Record(val name: String, val at: Long, val origin: String, val kind: String? = null)

    /** Una riga scritta da [send]: il file è stato eliminato, e può scadere. */
    const val KIND_SENT = "del"

    /** Una riga scritta da [keep]: è una copia di sicurezza, e non scade mai. */
    const val KIND_KEPT = "bak"

    /**
     * I nomi dei file che hanno passato il loro tempo e che lo svuotamento automatico può togliere.
     *
     * Funzione pura, come [ordered] e [records]: che l'originale esista lo dice chi chiama, con
     * [esiste]. Una riga senza `kind` scade solo se il suo originale non c'è più. `giorni <= 0`, il
     * valore di fabbrica, non toglie niente.
     */
    fun expiring(
        records: List<Record>,
        giorni: Int,
        adesso: Long,
        esiste: (String) -> Boolean
    ): List<String> {
        if (giorni <= 0) return emptyList()
        val limite = adesso - giorni.toLong() * DAY_MS
        return records.filter { record ->
            if (record.at > limite) return@filter false
            when (record.kind) {
                KIND_KEPT -> false
                KIND_SENT -> true
                else -> !esiste(record.origin)
            }
        }.map { it.name }
    }

    /**
     * Toglie dal cestino i file che hanno passato il loro tempo, e torna quanti ne ha tolti.
     *
     * Le tre decisioni dell'utente sono in `Rules.md` § '🗑️ Lo svuotamento automatico del cestino':
     * gira solo ad app aperta (`d-cestino-chiusa`), il conto è per file (`d-cestino-quando`), e le
     * copie di sicurezza restano (`d-cestino-editor`). La riga d'archivio se ne va col file, come
     * in [empty].
     */
    suspend fun sweep(context: Context, giorni: Int): Int =
        withContext(Dispatchers.IO + NonCancellable) {
            if (giorni <= 0) return@withContext 0
            lock.withLock {
                val records = read(context)
                val scaduti = expiring(
                    records = records,
                    giorni = giorni,
                    adesso = System.currentTimeMillis(),
                    esiste = { path -> runCatching { File(path).exists() }.getOrDefault(true) }
                ).toSet()
                if (scaduti.isEmpty()) return@withLock 0
                val bin = dir(context)
                var tolti = 0
                val rimasti = records.toMutableList()
                for (nome in scaduti) {
                    val file = File(bin, nome)
                    if (!file.isFile || runCatching { file.delete() }.getOrDefault(false)) {
                        tolti++
                        rimasti.removeAll { it.name == nome }
                    }
                }
                if (tolti > 0) write(context, rimasti)
                tolti
            }
        }

    /**
     * Che cosa c'è nel cestino adesso: le righe d'archivio e i file, per il backup.
     *
     * Si fotografa sotto il lock e si copia fuori: la copia di cento immagini dura, e il lock
     * bloccherebbe un'eliminazione fatta nel frattempo. Un file sparito fra le due cose, chi copia
     * lo salta. Le righe senza file restano fuori, come in [list].
     */
    internal suspend fun snapshot(context: Context): Pair<List<Record>, List<File>> =
        withContext(Dispatchers.IO) {
            lock.withLock {
                val files = runCatching { dir(context).listFiles() }.getOrNull().orEmpty()
                    .filter { it.isFile && !it.name.hasSeparators() }
                val names = files.map { it.name }.toSet()
                read(context).filter { it.name in names } to files
            }
        }

    /**
     * Aggiunge al cestino i file di un backup, con le loro righe d'archivio.
     *
     * ⚠️ **Si aggiunge e non si sostituisce**, al contrario del resto del backup: sostituire
     * cancellerebbe per sempre il cestino di adesso. I doppioni si saltano: una riga è la stessa
     * con origine, istante e peso uguali, un file senza riga con nome e peso uguali. L'istante
     * resta quello del backup, perché dice quando il file è stato eliminato: lo svuotamento
     * automatico lo conta da allora, e un file vecchio può andarsene al primo giro. Il nome lo
     * sceglie [FileTree.freeName], e la riga segue il nome nuovo.
     *
     * @param incoming i file del backup, già estratti accanto al cestino, con la loro riga se c'è:
     *   sullo stesso volume lo spostamento è una rinomina, e non una seconda copia di tutto.
     * @return quanti file non sono entrati. Un doppione non conta, perché è già nel cestino.
     */
    internal suspend fun adopt(context: Context, incoming: List<Pair<File, Record?>>): Int =
        withContext(Dispatchers.IO + NonCancellable) {
            lock.withLock {
                val bin = ready(context)
                val records = read(context).toMutableList()
                // Una mappa che cresce: due righe uguali dentro lo stesso backup entrerebbero tutte
                // e due se il confronto guardasse solo i file di prima dell'importazione.
                val sizes = runCatching { bin.listFiles() }.getOrNull().orEmpty()
                    .filter { it.isFile }
                    .associateTo(HashMap()) { it.name to it.length() }
                var left = 0
                for ((file, record) in incoming) {
                    val peso = file.length()
                    val doppione = if (record != null) {
                        records.any {
                            it.origin == record.origin && it.at == record.at && sizes[it.name] == peso
                        }
                    } else {
                        sizes[file.name] == peso
                    }
                    if (doppione) {
                        file.delete()
                        continue
                    }
                    val to = FileTree.freeName(bin, file.name)
                    if (!FileTree.carry(file, to)) {
                        left++
                        continue
                    }
                    touch(context, to)
                    sizes[to.name] = peso
                    if (record != null) records += Record(to.name, record.at, record.origin, record.kind)
                }
                write(context, records)
                left
            }
        }

    /**
     * Le righe di un archivio, saltando quelle rovinate.
     *
     * Una riga per file, colonne separate da tabulazione, come gli appunti di `Recents`: nessuna
     * libreria, e un file che si legge a occhio. Una riga malformata si scarta, così un archivio
     * rovinato a metà perde solo i file che descriveva. Pura come [ordered].
     */
    fun records(text: String): List<Record> = text.lineSequence().mapNotNull { line ->
        val parts = line.split('\t')
        // Tre o quattro colonne: le righe a tre sono del formato di prima, e rifiutarle toglierebbe
        // la provenienza ai file già nel cestino. Che cosa vuol dire l'assenza della quarta è su
        // [Record].
        if (parts.size != 3 && parts.size != 4) return@mapNotNull null
        val at = parts[1].toLongOrNull() ?: return@mapNotNull null
        if (parts[0].isBlank() || parts[2].isBlank()) return@mapNotNull null
        Record(parts[0], at, parts[2], parts.getOrNull(3)?.takeIf { it.isNotBlank() })
    }.toList()

    /**
     * L'archivio come testo, l'inversa di [records]. Una riga senza `kind` si riscrive a tre
     * colonne, così un archivio del formato vecchio resta identico a se stesso.
     */
    fun text(records: List<Record>): String = records.joinToString("\n") { r ->
        if (r.kind == null) "${r.name}\t${r.at}\t${r.origin}"
        else "${r.name}\t${r.at}\t${r.origin}\t${r.kind}"
    }

    private fun read(context: Context): List<Record> {
        val file = index(context)
        return records(runCatching { if (file.isFile) file.readText() else "" }.getOrDefault(""))
    }

    /** Riscrive l'archivio con [AtomicWrite], e risponde se è andata. */
    private fun write(context: Context, records: List<Record>): Boolean =
        AtomicWrite.text(index(context), text(records))

    /**
     * L'archivio delle provenienze, accanto al cestino e non dentro: dentro sarebbe un file in più
     * da filtrare dall'elenco, che comparirebbe fra le miniature come un'immagine rotta.
     */
    private fun index(context: Context): File = File(dir(context).parentFile, INDEX)

    /** Se un testo contiene i separatori dell'archivio. Vedi [send]. */
    private fun String.hasSeparators(): Boolean = contains('\t') || contains('\n')

    /** Quanto dura un giorno, per [expiring]. */
    private const val DAY_MS = 24L * 60L * 60L * 1000L

    private const val FOLDER = "bin"
    private const val INDEX = "bin.tsv"

    /**
     * Le operazioni sull'archivio non si sovrappongono: leggono e riscrivono lo stesso file, e due
     * che si intrecciassero perderebbero le righe dell'altra. 'Svuota' su un cestino grosso dura, e
     * l'interfaccia resta viva nel frattempo.
     */
    private val lock = Mutex()
}

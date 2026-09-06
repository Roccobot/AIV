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
    suspend fun send(context: Context, uris: List<Uri>): Sent =
        withContext(Dispatchers.IO + NonCancellable) {
            lock.withLock {
                val bin = ready(context)
                val records = read(context).toMutableList()
                val touched = ArrayList<String>(uris.size)
                val landed = ArrayList<Uri>(uris.size)
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
                        landed += to.toUri()
                        records += Record(to.name, now, from.absolutePath, KIND_SENT)
                    } else {
                        failed++
                    }
                }
                write(context, records)
                FileTree.scan(context, touched)
                Sent(FileTree.Outcome(done, failed), landed)
            }
        }

    /**
     * Che cosa ha fatto un'eliminazione: com'è andata, e **dove sono finiti** i file.
     *
     * ⚠️⚠️ **I DUE PEZZI VANNO INSIEME PERCHÉ IL SECONDO SI PUÒ SAPERE SOLO QUI, DALLA `1.60`**:
     * l'offerta di disfare (vedi [Undo]) deve chiamare [restore] su **quei** file, e i nomi nel
     * cestino non sono quelli d'origine, perché [FileTree.freeName] li cambia quando due file
     * si chiamano uguale. Ricavarli da fuori vorrebbe dire rileggere il cestino e indovinare
     * quali sono gli ultimi arrivati, che è vero finché non lo è più.
     * ⚠️ **[landed] porta i soli file passati davvero**: quelli falliti non ci sono, quindi il
     * suo conto è già [FileTree.Outcome.done] e chi lo usa non deve rifare la sottrazione.
     */
    data class Sent(val outcome: FileTree.Outcome, val landed: List<Uri>)

    /**
     * Mette nel cestino una **copia** di [from], lasciando l'originale dov'è.
     *
     * ⚠️⚠️ **COPIA E NON SPOSTA, ed è tutta la differenza con [send]**: qui il file non se ne
     * va da nessuna parte, sta per essere **riscritto sopra** dall'editor, e quello che si
     * salva è la sua versione di prima. Chiamare `send` distruggerebbe proprio la cosa che si
     * voleva proteggere.
     * ⚠️ **La riga d'archivio porta il percorso ORIGINALE**, come per un'eliminazione, quindi
     * il ripristino sa dove rimettere la copia. Se là intanto c'è la versione modificata,
     * `restore` non la schiaccia: usa [FileTree.freeName] e le si mette accanto, che è
     * l'unico esito onesto quando esistono due versioni della stessa fotografia.
     * ⚠️ **Niente `FileTree.scan` sull'origine**: l'originale non si è mosso, e dichiararlo
     * cambiato qui vorrebbe dire una scansione in più subito prima di quella che l'editor fa
     * comunque dopo aver scritto.
     *
     * @return la copia appena messa nel cestino, o `null` se non si è potuta fare.
     *
     * ⚠️⚠️ **RESTITUISCE IL FILE E NON UN SÌ O NO, dalla 1.13, perché un chiamante ha
     * bisogno di RIPRENDERSELA**: la copia fatta prima di aprire un editor **esterno** va
     * buttata se al ritorno si scopre che quell'editor non ha salvato niente, e senza sapere
     * come si chiama non la si ritrova (il nome può essere cambiato: vedi
     * [FileTree.freeName]). Chi vuole solo sapere se è andata bene guarda se è `null`.
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
                val records = read(context).toMutableList()
                records += Record(
                    to.name,
                    System.currentTimeMillis(),
                    from.absolutePath,
                    KIND_KEPT
                )
                write(context, records)
                to
            }
        }

    /**
     * Toglie dal cestino una copia che si è rivelata inutile, file e riga d'archivio.
     *
     * ⚠️⚠️ **È IL RIMEDIO A UNA COPIA DI TROPPO, NON UNA CANCELLAZIONE OFFERTA ALL'UTENTE, e
     * la distinzione tiene in piedi la promessa del cestino**: si chiama solo su una copia
     * che [keep] ha appena fatto e che si è dimostrata identica all'originale, cioè su un
     * file che non contiene niente che non esista ancora. Un cestino che si svuota da sé
     * sarebbe il contrario di quello che l'utente ha chiesto accendendo l'interruttore.
     * ⚠️ **La riga d'archivio se ne va anche se il file resta**: una riga che punta a un file
     * cancellato manderebbe il ripristino in errore, e un file senza riga finisce comunque in
     * fondo alla lista senza poter tornare a casa. Fra i due mali, il file orfano è quello
     * che l'utente può ancora vedere e buttare a mano.
     */
    suspend fun drop(context: Context, kept: File): Boolean =
        withContext(Dispatchers.IO + NonCancellable) {
            lock.withLock {
                val gone = runCatching { kept.delete() }.getOrDefault(false)
                write(context, read(context).filterNot { it.name == kept.name })
                gone
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
     * ⚠️⚠️ **QUI SI SCRIVE LA CRONOLOGIA, dalla 0.76**, e questo è il solo posto che può
     * farlo: la riga d'archivio di un file **sparisce** appena lo si ripristina, quindi
     * l'istante in cui si sa dov'è finito è questo. Vale per tutte le vie del ripristino, il
     * riquadro del visualizzatore compreso, perché passano tutte da qui. Vedi [History].
     */
    suspend fun restore(context: Context, uris: List<Uri>): FileTree.Outcome =
        withContext(Dispatchers.IO + NonCancellable) {
            lock.withLock {
                val records = read(context).toMutableList()
                val byName = records.associateBy { it.name }
                val touched = ArrayList<String>(uris.size)
                var done = 0
                var failed = 0
                // ⚠️ Un istante solo per tutta l'operazione, ed è quello che rende un gruppo
                // un gruppo nella cronologia: dentro il ciclo, dieci file darebbero dieci
                // gruppi da un file.
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
     *
     * ⚠️⚠️ **QUI L'ARCHIVIO SI POTA, e fino alla 1.46 non lo faceva nessuno.** Le vie d'uscita
     * dal cestino sono quattro e tre cancellano la propria riga ([restore], [drop], [empty]);
     * la quarta, l'eliminazione definitiva di una selezione, passa da `FileTree.delete` e non
     * sa niente di questo archivio, quindi lasciava dietro una riga per ogni file. Nel cestino
     * quella e la via **normale**, e l'archivio cresceva per sempre.
     * ⚠️ **Si pota QUI e non nel punto che cancella**, ed e una scelta: un rimedio nel
     * chiamante va ricordato, e il giorno che nasce una quinta uscita torna il difetto. Qui il
     * conto torna da se, perche questa funzione ha gia in mano l'elenco dei file che
     * **esistono**: una riga che non trova il suo file non descrive piu niente, e chi apre il
     * cestino e esattamente il momento in cui accorgersene.
     * ⚠️ **E chiude anche quello che si e accumulato fin qui**, che un rimedio nel chiamante non
     * avrebbe fatto. Stessa scelta e stessa forma della purga di `History.batches`: si riscrive
     * il file **solo** se la potatura ha tolto qualcosa, o si scriverebbe a ogni apertura.
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
        ordered(files.keys.toList(), records).mapNotNull { files[it]?.toUri() }
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
     * file che non ci sono più (cancellati a mano, o un'eliminazione a metà), e non deve far
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

    /**
     * Una riga d'archivio: come si chiama qui, quando è arrivata, da dove veniva, e **perché**
     * ci è finita.
     *
     * ⚠️⚠️ **LA QUARTA COLONNA NASCE CON LO SVUOTAMENTO AUTOMATICO, dalla `1.75`, E SERVE A
     * DISTINGUERE UNA COPIA DI SICUREZZA DA UN'ELIMINAZIONE**: l'utente ha stabilito che le
     * copie dell'editor **non scadono** (giro della 1.67, `d-cestino-editor`: `fuori`), e fino
     * a qui le due cose erano indistinguibili, perché [send] e [keep] scrivevano la stessa
     * riga.
     * ⚠️ **Non basta guardare se il file d'origine esiste ancora**, che sarebbe la via senza
     * formato nuovo: per una copia di sicurezza l'originale c'è, per un'eliminazione no, ma chi
     * modifica una fotografia e **poi** la elimina si ritrova una copia di sicurezza il cui
     * originale non esiste più, cioè esattamente il file che non deve scadere.
     * ⚠️⚠️ **`null` VUOL DIRE 'RIGA DI PRIMA DELLA `1.75`', e non si legge come 'eliminazione'**:
     * là il perché non è scritto, quindi si ricade sulla prova dell'originale, che sui file
     * vecchi sbaglia solo nel verso di **tenere** qualcosa più a lungo. Vedi [expiring].
     */
    class Record(val name: String, val at: Long, val origin: String, val kind: String? = null)

    /** Una riga scritta da [send]: il file è stato eliminato, e può scadere. */
    const val KIND_SENT = "del"

    /** Una riga scritta da [keep]: è una copia di sicurezza, e non scade mai. */
    const val KIND_KEPT = "bak"

    /**
     * I nomi dei file che hanno passato il loro tempo e che lo svuotamento automatico può
     * togliere.
     *
     * ⚠️⚠️ **FUNZIONE PURA, come [ordered] e [records], e per la stessa ragione**: prende righe
     * e istanti e restituisce nomi, quindi si prova su una JVM senza Android intorno. Che
     * l'originale esista ancora lo dice chi chiama, con [esiste], perché quella è l'unica parte
     * che tocca il disco.
     * ⚠️ **Una riga senza `kind` scade solo se il suo originale NON c'è più**: è il ripiego per
     * le righe scritte prima della `1.75`, e sbaglia solo nel verso di tenere un file in più.
     * ⚠️ **`giorni <= 0` non toglie niente**, ed è il valore di fabbrica: lo svuotamento
     * automatico è spento, quindi questa funzione deve poter essere chiamata lo stesso senza
     * fare danni.
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
     * ⚠️⚠️ **GIRA SOLO MENTRE L'APP È APERTA, ED È UNA SUA DECISIONE PRESA DUE VOLTE** (giro
     * della 1.67, `d-cestino-chiusa`: **`aperta`**; e ripetuta il 2026-09-06 perché il brief
     * l'aveva registrata al contrario: *nulla deve avvenire al di fuori dell'app aperta in primo
     * piano*). Quindi niente operazione programmata di sistema e nessuna libreria in più: chi
     * chiama questa funzione è l'app, quando è in scena.
     * ⚠️ **Il conto è per FILE e non per cestino** (`d-cestino-quando`: `file`): ognuno se ne va
     * quando ha compiuto il suo tempo, come fanno i cestini di sistema, invece di svuotare tutto
     * a intervalli.
     * ⚠️ **Le copie di sicurezza dell'editor restano** (`d-cestino-editor`: `fuori`): una rete
     * che sparisce da sé non è una rete, e si tolgono solo svuotando il cestino a mano.
     * ⚠️ **La riga d'archivio se ne va col file**, come in [empty]: una riga orfana manderebbe
     * il ripristino in errore.
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
        // ⚠️⚠️ **TRE O QUATTRO, e le tre sono le righe scritte prima della `1.75`**: rifiutarle
        // vorrebbe dire che aggiornando l'app ogni file già nel cestino perde la sua provenienza,
        // cioè non si può più ripristinare. La colonna nuova è quindi facoltativa per sempre, e
        // che cosa vuol dire la sua assenza sta su [Record].
        if (parts.size != 3 && parts.size != 4) return@mapNotNull null
        val at = parts[1].toLongOrNull() ?: return@mapNotNull null
        if (parts[0].isBlank() || parts[2].isBlank()) return@mapNotNull null
        Record(parts[0], at, parts[2], parts.getOrNull(3)?.takeIf { it.isNotBlank() })
    }.toList()

    /**
     * L'archivio come testo. L'inversa di [records].
     *
     * ⚠️ **Una riga senza `kind` si riscrive a tre colonne**, non a quattro con l'ultima vuota:
     * così un archivio mai toccato dalla `1.75` resta identico a se stesso, e il formato vecchio
     * e quello nuovo non diventano due modi di scrivere la stessa cosa.
     */
    fun text(records: List<Record>): String = records.joinToString("\n") { r ->
        if (r.kind == null) "${r.name}\t${r.at}\t${r.origin}"
        else "${r.name}\t${r.at}\t${r.origin}\t${r.kind}"
    }

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

    /** Quanto dura un giorno, per [expiring]. */
    private const val DAY_MS = 24L * 60L * 60L * 1000L

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

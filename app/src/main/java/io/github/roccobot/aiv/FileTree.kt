package io.github.roccobot.aiv

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * Le cartelle del telefono come le vede il filesystem, non come le racconta la galleria, e le
 * operazioni che ci si fanno sopra.
 *
 * ⚠️ **Non è `Folder`**: la destinazione di copia e sposta è una vista del filesystem e non
 * degli album (precisazione dell'utente). `Folder` interroga il MediaStore, quindi conosce solo
 * le cartelle che contengono già immagini, e lascerebbe fuori quelle vuote o escluse.
 * Serve `MANAGE_EXTERNAL_STORAGE`: senza, `listFiles` sulla memoria condivisa risponde `null`.
 *
 * ⚠️ **Le operazioni che scrivono sono `NonCancellable`**: girano nello scope della schermata, e
 * uscendo dalla cartella si annullerebbe l'avviso al MediaStore, che viene dopo i cicli. La
 * galleria resterebbe indietro. Le letture ([children], [namesOf]) restano annullabili.
 */
object FileTree {

    /**
     * Da dove si comincia a navigare: la memoria condivisa, più le schede se ce ne sono.
     *
     * ⚠️ **Le schede si ricavano dalle cartelle dell'app e non da `StorageManager`**:
     * `StorageVolume.getDirectory` esiste solo da Android 11, `getExternalFilesDirs` risponde
     * con una voce per volume su tutte le versioni. Si taglia a `/Android/`, perché il percorso
     * è `<radice>/Android/data/<pacchetto>/files`.
     * Insieme ordinato e non lista: senza scheda le due strade danno la stessa cartella.
     */
    fun roots(context: Context): List<File> {
        val found = LinkedHashSet<File>()
        runCatching { Environment.getExternalStorageDirectory() }.getOrNull()?.let { found += it }
        runCatching { context.getExternalFilesDirs(null) }.getOrNull()
            ?.filterNotNull()
            ?.forEach { dir ->
                val path = dir.absolutePath
                val cut = path.indexOf("/Android/")
                if (cut > 0) found += File(path.substring(0, cut))
            }
        return found.filter { runCatching { it.isDirectory }.getOrDefault(false) }
    }

    /**
     * Le sottocartelle di una cartella, in ordine alfabetico e senza distinguere le
     * maiuscole.
     *
     * Solo cartelle, perché qui si sceglie una destinazione; le nascoste (col punto) restano
     * fuori, perché sono cartelle di servizio.
     * `listFiles` risponde `null` su una cartella illeggibile, anche col permesso pieno
     * (`/storage/emulated/0/Android/data`): là si mostra una cartella vuota, non un errore.
     */
    suspend fun children(dir: File): List<File> = withContext(Dispatchers.IO) {
        runCatching { dir.listFiles() }.getOrNull().orEmpty()
            .filter { it.isDirectory && !it.name.startsWith(".") }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Copia le immagini scelte dentro [dest], e dice com'è andata.
     *
     * ⚠️ **Non sovrascrive mai**: un nome già preso diventa `nome (2).ext` ([freeName]). Una
     * copia che sovrascrive è una cancellazione.
     * Una copia fallita non ferma le altre; il conto dei falliti torna a chi chiama.
     * Passa dal resolver e non dal `File`, a differenza di sposta e rinomina: l'origine si deve
     * solo leggere, e un flusso si apre anche su un indirizzo senza percorso sul disco.
     */
    suspend fun copy(context: Context, uris: List<Uri>, dest: File): Outcome =
        withContext(Dispatchers.IO + NonCancellable) {
            var done = 0
            var failed = 0
            val written = ArrayList<String>(uris.size)
            for (uri in uris) {
                val ok = runCatching {
                    val name = displayName(context, uri) ?: return@runCatching false
                    val target = freeName(dest, name)
                    val input = context.contentResolver.openInputStream(uri)
                        ?: return@runCatching false
                    if (!input.use { pour(it, target) }) return@runCatching false
                    written += target.absolutePath
                    true
                }.getOrDefault(false)
                if (ok) done++ else failed++
            }
            scan(context, written)
            // La copia si disfa cancellando quello che si è scritto: l'originale non si è mosso.
            Outcome(done, failed, written.map { Undoable(made = it) })
        }

    /**
     * Duplica le immagini scelte dove sono, senza chiedere una destinazione (il tocco lungo su
     * 'Copia').
     *
     * ⚠️ **Chiama [copy] un file per volta**, perché qui la destinazione è diversa per ogni
     * file (in una ricerca le cartelle possono essere venti): così la logica che non sovrascrive
     * e che rimedia a metà strada esiste una volta sola. Il prezzo è una scansione del
     * MediaStore per file.
     * Il nome lo sceglie [freeName], quindi il duplicato è `nome (2).ext`.
     * Un'immagine senza file (arrivata da una chat) non si duplica: non ha una cartella.
     */
    suspend fun duplicate(context: Context, uris: List<Uri>): Outcome =
        withContext(Dispatchers.IO + NonCancellable) {
            var done = 0
            var failed = 0
            // I passi da disfare si sommano come i conti, o il duplicato non si annullerebbe.
            val undo = ArrayList<Undoable>(uris.size)
            for (uri in uris) {
                val here = fileOf(context, uri)?.parentFile
                if (here == null) {
                    failed++
                    continue
                }
                val out = copy(context, listOf(uri), here)
                done += out.done
                failed += out.failed
                undo += out.undo
            }
            Outcome(done, failed, undo)
        }

    /**
     * Sposta le immagini scelte dentro [dest], con [carry].
     *
     * ⚠️ **Spostare nella cartella in cui si è già non fa niente**: senza il controllo,
     * [freeName] inventerebbe `nome (2).ext` e lo spostamento diventerebbe un duplicato.
     */
    suspend fun move(context: Context, uris: List<Uri>, dest: File): Outcome =
        withContext(Dispatchers.IO + NonCancellable) {
            var done = 0
            var failed = 0
            val touched = ArrayList<String>(uris.size * 2)
            // Un file rimasto nella sua cartella non ha un passo da disfare.
            val passi = ArrayList<Undoable>(uris.size)
            for (uri in uris) {
                val from = fileOf(context, uri)
                if (from == null) {
                    failed++
                    continue
                }
                if (from.parentFile?.absolutePath == dest.absolutePath) {
                    done++
                    continue
                }
                val to = freeName(dest, from.name)
                val ok = carry(from, to)
                if (ok) {
                    done++
                    touched += from.absolutePath
                    touched += to.absolutePath
                    passi += Undoable(made = to.absolutePath, from = from.absolutePath)
                } else {
                    failed++
                }
            }
            scan(context, touched)
            Outcome(done, failed, passi)
        }

    /**
     * Cancella le immagini scelte, per davvero.
     *
     * ⚠️ **Non passa da nessun cestino** (né da [Bin] né da `IS_TRASHED` del MediaStore): il file
     * sparisce dal disco, quindi chi chiama deve aver chiesto conferma.
     * Prima il file e poi, come ripiego, la riga: `contentResolver.delete` vale solo per gli
     * indirizzi del MediaStore, e nella griglia girano anche indirizzi `file://`.
     */
    suspend fun delete(context: Context, uris: List<Uri>): Outcome =
        withContext(Dispatchers.IO + NonCancellable) {
            var done = 0
            var failed = 0
            val gone = ArrayList<String>(uris.size)
            for (uri in uris) {
                val file = fileOf(context, uri)
                val ok = runCatching {
                    if (file != null && file.delete()) return@runCatching true
                    uri.scheme?.lowercase() == "content" &&
                        context.contentResolver.delete(uri, null, null) > 0
                }.getOrDefault(false)
                if (ok) {
                    done++
                    file?.let { gone += it.absolutePath }
                } else {
                    failed++
                }
            }
            scan(context, gone)
            Outcome(done, failed)
        }

    /**
     * Rinomina in blocco: [template] con i cancelletti al posto del numero, e [start] come
     * primo numero.
     *
     * ⚠️ **L'ordine è alfabetico, non quello della griglia** (istruzione dell'utente): la
     * griglia ordina per data. L'ordine è in [ordered].
     * ⚠️⚠️ **Due fasi, o si perderebbero dei file**: una rinomina alla volta, `b.jpg -> a.jpg`
     * sovrascriverebbe l'`a.jpg` che deve ancora diventare `c.jpg`, e i cicli succedono ogni volta
     * che si rinumera una serie numerata. Prima tutti vanno su un nome provvisorio, poi su quello
     * vero.
     * I nomi provvisori cominciano col punto: se l'app si ferma fra le due fasi, restano file
     * nascosti e non immagini senza nome in galleria.
     * Il numero è legato alla posizione: un file che non si riesce a spostare lascia il suo numero
     * vuoto, senza far slittare quelli dopo.
     * L'estensione viene dal file e non dal modello (istruzione dell'utente), salvo [extension].
     */
    suspend fun rename(
        context: Context,
        uris: List<Uri>,
        template: String,
        start: Int,
        /**
         * L'estensione da mettere a tutti, senza il punto, o `null` per tenere quella di ogni
         * file.
         *
         * ⚠️ **Cambia il nome e non il formato**: `foto.jpg` rinominato `foto.png` resta un
         * JPEG. Il pannello che la raccoglie lo dice.
         */
        extension: String? = null
    ): Outcome = withContext(Dispatchers.IO + NonCancellable) {
        val files = ordered(context, uris)
        var failed = uris.size - files.size
        var done = 0
        val touched = ArrayList<String>(files.size * 2)
        val stamp = System.currentTimeMillis()

        // Fase 1: tutti al riparo, sotto un nome che non può essere quello di nessuno.
        val parked = ArrayList<Pair<File, Pair<File, String>>>(files.size)
        files.forEachIndexed { at, file ->
            val dir = file.parentFile
            val wanted = renderName(
                template,
                start + at,
                extension ?: file.name.substringAfterLast('.', "")
            )
            val temp = if (dir == null) null else File(dir, ".aiv-$stamp-$at")
            if (temp != null && runCatching { file.renameTo(temp) }.getOrDefault(false)) {
                parked += temp to (file to wanted)
            } else {
                failed++
            }
        }

        // Fase 2: il nome vero. Un nome già preso adesso è di un file fuori dalla selezione, e
        // vale la regola della copia.
        for ((temp, job) in parked) {
            val (origin, wanted) = job
            touched += origin.absolutePath
            val target = origin.parentFile?.let { freeName(it, wanted) }
            if (target != null && runCatching { temp.renameTo(target) }.getOrDefault(false)) {
                done++
                touched += target.absolutePath
            } else {
                failed++
                // Si rimette com'era: col nome provvisorio, nascosto, sparirebbe dalla galleria.
                runCatching { temp.renameTo(origin) }
            }
        }

        scan(context, touched)
        Outcome(done, failed)
    }

    /**
     * I nomi delle immagini scelte, nell'ordine in cui la rinomina le numererà: per l'anteprima
     * della finestra, che usa lo stesso [ordered] di [rename].
     */
    suspend fun namesOf(context: Context, uris: List<Uri>): List<String> =
        withContext(Dispatchers.IO) { ordered(context, uris).map { it.name } }

    /**
     * Quante ne sono passate e quante no: la risposta di ogni operazione su un gruppo.
     *
     * @property undo i passi per tornare indietro. Vuoto vuol dire che non si disfa: operazione
     *   irreversibile, o niente di toccato.
     * @property freed i byte liberati, contati sui soli file cancellati davvero. Lo misura solo lo
     *   svuotamento del cestino (richiesta dell'utente, 2.86); zero vuol dire 'non misurato', e
     *   la frase resta quella senza peso (`outcomeText`).
     */
    data class Outcome(
        val done: Int,
        val failed: Int,
        val undo: List<Undoable> = emptyList(),
        val freed: Long = 0
    )

    /**
     * Un file che si può rimettere com'era, e come.
     *
     * Due casi in un tipo solo, distinti da [from]: una copia si disfa cancellando quello che è
     * stato scritto, uno spostamento riportando il file da dove veniva.
     * ⚠️ **Si tiene il percorso e non l'indirizzo**: l'indirizzo nel MediaStore di un file appena
     * scritto arriva dopo la scansione, quando 'Annulla' è già in scena.
     *
     * @property made il file che l'operazione ha creato: quello che c'è adesso.
     * @property from da dove veniva, per uno spostamento; `null` per una copia, dove l'originale
     *   non si è mosso e non c'è niente da rimettere a posto.
     */
    data class Undoable(val made: String, val from: String? = null)

    /**
     * Rimette com'erano i file di [steps], e dice come è andata.
     *
     * Il ritorno di uno spostamento passa dallo stesso [carry] dell'andata, quindi funziona anche
     * fra due volumi.
     * ⚠️ **Un file che non c'è più conta come riuscito**: fra l'operazione e 'Annulla' qualcuno
     * può averlo cancellato da un'altra app, e il risultato voluto c'è già.
     * La destinazione di un ritorno può essere occupata: si sceglie un nome libero come
     * all'andata.
     */
    suspend fun revert(context: Context, steps: List<Undoable>): Outcome =
        withContext(Dispatchers.IO + NonCancellable) {
            var done = 0
            var failed = 0
            val touched = ArrayList<String>(steps.size * 2)
            for (step in steps) {
                val made = File(step.made)
                val home = step.from
                if (home == null) {
                    val ok = runCatching { !made.exists() || made.delete() }.getOrDefault(false)
                    if (ok) done++ else failed++
                    touched += step.made
                    continue
                }
                if (!made.exists()) {
                    done++
                    continue
                }
                val back = File(home)
                val target = if (back.exists()) {
                    freeName(back.parentFile ?: back, back.name)
                } else {
                    back
                }
                if (carry(made, target)) {
                    done++
                    touched += step.made
                    touched += target.absolutePath
                } else {
                    failed++
                }
            }
            scan(context, touched)
            Outcome(done, failed)
        }

    /**
     * I file dietro gli indirizzi scelti, in ordine naturale ([naturalCompare]).
     *
     * Gli indirizzi senza un file sul disco restano fuori, e chi chiama conta la differenza come
     * fallita. Il percorso rompe i pareggi fra file omonimi in cartelle diverse.
     */
    internal fun ordered(context: Context, uris: List<Uri>): List<File> =
        uris.mapNotNull { fileOf(context, it) }
            .sortedWith { a, b ->
                val byName = naturalCompare(a.name, b.name)
                if (byName != 0) byName else a.absolutePath.compareTo(b.absolutePath)
            }

    /**
     * Sposta un file da una parte all'altra, e dice se ci è riuscito. La usano anche il cestino
     * e l'annullamento.
     *
     * Prima prova a rinominare: nella stessa memoria `renameTo` è istantaneo e non tocca i byte.
     * Il ripiego (copia e cancellazione) serve quando si cambia volume, dove `renameTo` fallisce.
     * ⚠️ **Nel ripiego l'originale si cancella solo a copia riuscita, e se la cancellazione
     * fallisce si cancella la copia**: altrimenti resterebbe un file perso o due file uguali.
     */
    internal fun carry(from: File, to: File): Boolean = runCatching {
        if (from.renameTo(to)) return@runCatching true
        if (!from.inputStream().use { pour(it, to) }) return@runCatching false
        if (from.delete()) {
            true
        } else {
            to.delete()
            false
        }
    }.getOrDefault(false)

    /**
     * La cartella [name] dentro [parent], creata se non c'è, oppure `null` se non si è potuta
     * creare. La usano le due 'Nuova cartella', quella dell'albero e quella della destinazione.
     *
     * ⚠️ **Un nome già preso risponde con la cartella che c'è** (rilievi D10 ed E29 della
     * revisione Setaccio): `mkdirs` risponde `false` anche quando la cartella esiste, e chi crea
     * una cartella ci vuole entrare.
     */
    suspend fun makeFolder(parent: File, name: String): File? = withContext(Dispatchers.IO) {
        val folder = File(parent, name)
        runCatching { folder.takeIf { it.isDirectory || it.mkdirs() } }.getOrNull()
    }

    /**
     * Versa [input] in [target], che non esiste ancora, e dice se è arrivato in fondo.
     *
     * ⚠️ **Una copia interrotta non lascia niente** (rilievo D31): con la memoria piena a metà,
     * resterebbe un'immagine troncata col nome giusto. [AtomicWrite] non serve: [target] è un
     * nome libero, e non c'è un file vecchio da proteggere.
     */
    internal fun pour(input: InputStream, target: File): Boolean {
        val ok = runCatching { target.outputStream().use { input.copyTo(it) } }.isSuccess
        if (!ok) runCatching { target.delete() }
        return ok
    }

    /**
     * Un nome libero dentro [dir], partendo da [name].
     *
     * Il contatore va prima dell'estensione: `foto (2).jpg` e non `foto.jpg (2)`, o il file
     * smetterebbe di essere un'immagine per il sistema.
     * Il tetto c'è perché il disco lo può scrivere anche qualcun altro: un caso patologico non
     * deve girare per sempre.
     * È `internal` perché lo usa anche il cestino, che deve sapere quale nome ha ottenuto.
     */
    internal fun freeName(dir: File, name: String): File {
        val first = File(dir, name)
        if (!first.exists()) return first
        val stem = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        val tail = if (ext.isEmpty()) "" else ".$ext"
        for (n in 2..999) {
            val candidate = File(dir, "$stem ($n)$tail")
            if (!candidate.exists()) return candidate
        }
        return File(dir, "$stem (${System.currentTimeMillis()})$tail")
    }

    /**
     * Il nome che il sistema dà a un indirizzo: quello che la copia conserva.
     *
     * È `internal` perché la usa anche [Names], per il nome sotto le miniature: una sola
     * risposta, col ripiego sull'ultimo pezzo dell'indirizzo.
     */
    internal fun displayName(context: Context, uri: Uri): String? {
        if (uri.scheme?.lowercase() == "file") return uri.lastPathSegment
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null, null, null
            )?.use { c ->
                val at = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (at >= 0 && c.moveToFirst()) c.getString(at) else null
            }
        }.getOrNull() ?: uri.lastPathSegment
    }

    /**
     * Le cartelle in cui sono [uris], nella forma di [portablePath]: le origini di una copia o
     * di uno spostamento, che la finestra della destinazione disattiva (nota C dell'utente, 4.45).
     *
     * Un file il cui percorso non si legge non aggiunge niente: la finestra lascia sceglibile la
     * sua cartella, e la copia funziona comunque.
     * ⚠️ **Una domanda ogni [SOURCES_BATCH] righe** (rilievo E8 della revisione Setaccio): la
     * finestra aspetta questa risposta prima di comparire, e una domanda per file, con tremila
     * file scelti, voleva dire secondi senza risposta al tocco (misurato in `OriginiTest`). Gli
     * indirizzi del MediaStore (`<collezione>/<numero>`) si chiedono per collezione con
     * `_id IN (...)`; gli altri uno alla volta.
     */
    internal fun sourcesOf(context: Context, uris: List<Uri>): Set<String> {
        val folders = mutableSetOf<String>()
        fun add(path: String?) {
            path?.let(::File)?.parentFile?.absolutePath?.let(::portablePath)?.let(folders::add)
        }
        val (rows, others) = uris.partition {
            it.scheme?.lowercase() == "content" && it.lastPathSegment?.toLongOrNull() != null
        }
        others.forEach { add(fileOf(context, it)?.absolutePath) }
        rows.groupBy { it.buildUpon().path(it.path?.substringBeforeLast('/')).build() }
            .forEach { (collection, members) ->
                members.chunked(SOURCES_BATCH).forEach { batch ->
                    val ids = batch.map { it.lastPathSegment!! }
                    @Suppress("DEPRECATION")
                    val column = MediaStore.Images.Media.DATA
                    val read = runCatching {
                        context.contentResolver.query(
                            collection,
                            arrayOf(column),
                            "${MediaStore.MediaColumns._ID} IN (${ids.joinToString(",") { "?" }})",
                            ids.toTypedArray(),
                            null
                        )?.use { c ->
                            val at = c.getColumnIndex(column)
                            while (at >= 0 && c.moveToNext()) add(c.getString(at))
                        }
                    }
                    // Un fornitore che rifiuta la domanda si interroga un indirizzo alla volta.
                    if (read.isFailure || read.getOrNull() == null) {
                        batch.forEach { add(fileOf(context, it)?.absolutePath) }
                    }
                }
            }
        return folders
    }

    /** Quante righe chiede una domanda di [sourcesOf]: ben sotto i 999 argomenti di SQLite. */
    private const val SOURCES_BATCH = 500

    /**
     * Il file vero dietro un indirizzo, o `null` se non ce n'è uno raggiungibile.
     *
     * ⚠️ **`DATA` è deprecata e serve lo stesso**, come in `Folder`: è la sola colonna che dice
     * dov'è il file, e spostare o rinominare vuol dire toccarlo dov'è. Con l'accesso a tutti i
     * file quel percorso è scrivibile.
     * Si controlla che sia davvero un file: una riga del MediaStore può sopravvivere al file, e
     * agire su un percorso inesistente conterebbe come riuscito.
     * È `internal` anche per le informazioni sul file: da Android 10 il MediaStore nasconde la
     * posizione EXIF servita da un `content://`, e dal file vero le coordinate arrivano intere
     * (`factsOf`).
     */
    internal fun fileOf(context: Context, uri: Uri): File? {
        val path = when (uri.scheme?.lowercase()) {
            "file" -> uri.path
            else -> runCatching {
                @Suppress("DEPRECATION")
                val column = MediaStore.Images.Media.DATA
                context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { c ->
                    val at = c.getColumnIndex(column)
                    if (at >= 0 && c.moveToFirst()) c.getString(at) else null
                }
            }.getOrNull()
        } ?: return null
        return runCatching { File(path).takeIf { it.isFile } }.getOrNull()
    }

    /**
     * Avvisa il MediaStore dei percorsi toccati, e aspetta che abbia finito.
     *
     * ⚠️ **Senza l'avviso la galleria resta indietro**: il provider non vede i file scritti a
     * mano. Vale sui due percorsi di uno spostamento o di una rinomina: la partenza toglie la riga
     * vecchia, l'arrivo mette quella nuova.
     * ⚠️ **Si aspetta**, perché la scansione è asincrona e la schermata si ricaricherebbe sui dati
     * di prima. Il tetto [SCAN_WAIT_MS] evita che un richiamo mai arrivato blocchi la selezione.
     *
     * ⚠️ **Cancella anche le miniature di [Thumbs] dei percorsi toccati**: chi fa scandire un
     * percorso dichiara che il contenuto è cambiato, e quella cache è indicizzata
     * sull'indirizzo (segnalazione A2 dell'utente, giro della 2.23: un file ripristinato dal
     * cestino mostrava la miniatura della versione modificata). Le chiavi sono due:
     * - quella del `file://`, usata dalla vista 'Cartelle di sistema', si cancella subito, senza
     *   aspettare il richiamo;
     * - quella del `content://` si cancella nel richiamo; se lo scanner non risponde con un
     *   indirizzo, resta.
     */
    internal suspend fun scan(context: Context, paths: List<String>) {
        val list = paths.distinct().filter { it.isNotBlank() }
        if (list.isEmpty()) return
        list.forEach { Thumbs.forget(context, Uri.fromFile(File(it))) }
        runCatching {
            withTimeoutOrNull(SCAN_WAIT_MS) {
                suspendCancellableCoroutine { cont ->
                    val left = AtomicInteger(list.size)
                    MediaScannerConnection.scanFile(context, list.toTypedArray(), null) { _, uri ->
                        if (uri != null) Thumbs.forget(context, uri)
                        if (left.decrementAndGet() == 0 && cont.isActive) cont.resume(Unit)
                    }
                }
            }
        }
    }

    /** Quanto si aspetta il MediaStore, al massimo, prima di rileggere la cartella. */
    private const val SCAN_WAIT_MS = 8_000L
}

/**
 * Il nome finale di un file rinominato: [template] coi cancelletti sostituiti da [number].
 *
 * Ogni gruppo di cancelletti diventa il numero, con tante cifre quanti cancelletti: `##` dà
 * `01`. Si sostituiscono tutti i gruppi, perché un cancelletto rimasto nel nome sembra un
 * difetto.
 * ⚠️ **Un numero più lungo del gruppo non si taglia**: con `##` il file 100 si chiama `100` e
 * non `00`, o due file avrebbero lo stesso nome.
 * La barra si sostituisce con uno spazio, come nella finestra della cartella nuova: in un nome
 * su Android non può esserci, e lo trasformerebbe in un percorso.
 * Punti e spazi in coda si tolgono: sono legali su ext4 ma non su una scheda FAT.
 */
internal fun renderName(template: String, number: Int, extension: String): String {
    val body = StringBuilder()
    var i = 0
    while (i < template.length) {
        if (template[i] == '#') {
            var run = 0
            while (i < template.length && template[i] == '#') {
                run++
                i++
            }
            body.append(number.toString().padStart(run, '0'))
        } else {
            body.append(template[i])
            i++
        }
    }
    val stem = body.toString().replace('/', ' ').trim().trimEnd('.', ' ')
    val safe = stem.ifEmpty { number.toString() }
    return if (extension.isEmpty()) safe else "$safe.$extension"
}

/**
 * Quante cifre proporre per numerare [count] file a partire da [start].
 *
 * Minimo due, poi quante ne vuole il numero più alto (istruzione dell'utente). Il minimo
 * esiste perché `Foto 1` e `Foto 10` si ordinano male come testo.
 */
internal fun hashesFor(count: Int, start: Int): Int {
    val highest = start.toLong() + count.toLong() - 1L
    return maxOf(2, highest.coerceAtLeast(0L).toString().length)
}

/**
 * Il modello da proporre, ricavato dal nome del primo file in ordine alfabetico.
 *
 * Si taglia alla prima cifra (istruzione dell'utente: `Museo1ABC_02` propone `Museo ##`): la
 * parte davanti è quella scritta da una persona, il resto è la numerazione che si sta per rifare.
 * Un nome che comincia con una cifra si tiene intero: tagliato, il modello sarebbe vuoto.
 */
internal fun suggestTemplate(firstName: String, count: Int, start: Int): String {
    val stem = firstName.substringBeforeLast('.', firstName)
    val head = stem.takeWhile { !it.isDigit() }.trim().trimEnd('_', '-', ' ')
    val base = head.ifEmpty { stem }
    return "$base ${"#".repeat(hashesFor(count, start))}"
}

/**
 * Confronta due nomi come li ordinerebbe una persona: lettere in ordine alfabetico, e
 * gruppi di cifre confrontati come **numeri**.
 *
 * ⚠️ **Non è un confronto lessicografico**: per carattere `IMG_10` viene prima di `IMG_9`, e
 * una serie numerata si rinumererebbe in disordine. È l'ordine alfabetico chiesto dall'utente,
 * letto in modo che regga le serie numerate.
 * Gli zeri iniziali non contano nel valore ma rompono i pareggi: `foto 007` e `foto 7` valgono
 * lo stesso numero, e il confronto finale sul testo li ordina sempre allo stesso modo.
 */
internal fun naturalCompare(a: String, b: String): Int {
    var i = 0
    var j = 0
    while (i < a.length && j < b.length) {
        val ca = a[i]
        val cb = b[j]
        if (ca.isDigit() && cb.isDigit()) {
            var si = i
            var sj = j
            while (i < a.length && a[i].isDigit()) i++
            while (j < b.length && b[j].isDigit()) j++
            while (si < i - 1 && a[si] == '0') si++
            while (sj < j - 1 && b[sj] == '0') sj++
            val la = i - si
            val lb = j - sj
            if (la != lb) return la - lb
            for (k in 0 until la) {
                val step = a[si + k] - b[sj + k]
                if (step != 0) return step
            }
        } else {
            val step = ca.lowercaseChar().compareTo(cb.lowercaseChar())
            if (step != 0) return step
            i++
            j++
        }
    }
    val rest = (a.length - i) - (b.length - j)
    if (rest != 0) return rest
    return a.compareTo(b)
}

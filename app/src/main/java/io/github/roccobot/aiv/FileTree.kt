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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * Le cartelle del telefono come le vede il **filesystem**, non come le racconta la
 * galleria, e le operazioni che ci si fanno sopra.
 *
 * ⚠️⚠️ **È UN'ALTRA COSA DA `Folder`, e la differenza è il motivo per cui questo file
 * esiste** (precisazione dell'utente, 2026-08-29: la destinazione di copia e sposta deve
 * essere *una vista del filesystem, NON degli album di foto*). `Folder` interroga il
 * MediaStore, quindi per costruzione conosce **solo le cartelle che contengono già
 * fotografie**: come elenco di destinazioni è esattamente l'insieme sbagliato, perché
 * lascia fuori le cartelle vuote, quelle che l'utente ha escluso e quelle che di immagini
 * non ne hanno mai viste.
 * ⚠️ **Serve il permesso pesante**, che l'app ha già: senza `MANAGE_EXTERNAL_STORAGE` una
 * `listFiles` sulla memoria condivisa torna `null`, e non per un errore ma per progetto.
 *
 * ⚠️⚠️ **LE QUATTRO OPERAZIONI SONO `NonCancellable`, e non è eccesso di zelo**: girano
 * nello scope della schermata, quindi uscire dalla cartella mentre lavorano le
 * annullerebbe. I cicli non hanno punti di sospensione e arriverebbero comunque in fondo,
 * ma l'avviso al MediaStore, che sta dopo, no: la galleria resterebbe con le foto appena
 * cancellate e senza quelle appena copiate, e nessuno saprebbe perché. Le letture
 * ([children], [namesOf]) restano annullabili, perché di una lettura buttata via non
 * resta niente.
 */
object FileTree {

    /**
     * Da dove si comincia a navigare: la memoria condivisa, più le schede se ce ne sono.
     *
     * ⚠️⚠️ **Le schede si ricavano dalle cartelle DELL'APP e non da `StorageManager`**, ed
     * è la via che funziona su tutte le versioni: `StorageVolume.getDirectory` esiste solo
     * da Android 11, mentre `getExternalFilesDirs` torna una voce per ogni volume montato
     * da sempre. Il punto in cui tagliare è `/Android/`, che sta in mezzo per definizione:
     * il percorso è `<radice>/Android/data/<pacchetto>/files`.
     * ⚠️ Insieme ordinato e non lista: sui telefoni senza scheda le due strade dànno la
     * **stessa** cartella, e senza questo comparirebbe due volte.
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
     * ⚠️ **Solo cartelle**: qui si sceglie una destinazione, e un elenco che mostrasse
     * anche i file costringerebbe a scorrere trecento fotografie per trovare la cartella
     * sotto.
     * ⚠️ **Le nascoste (quelle col punto) restano fuori**: sono cartelle di servizio, e in
     * un elenco di destinazioni sono rumore. Chi un domani ne avesse bisogno tolga questo
     * filtro, che è una riga sola e non una struttura.
     * ⚠️ `listFiles` torna `null` su una cartella che non si può leggere, e succede anche
     * col permesso pesante (`/storage/emulated/0/Android/data`, per dirne una): là si
     * mostra una cartella **vuota**, non un errore, perché non poterci entrare non è un
     * guasto dell'app.
     */
    suspend fun children(dir: File): List<File> = withContext(Dispatchers.IO) {
        runCatching { dir.listFiles() }.getOrNull().orEmpty()
            .filter { it.isDirectory && !it.name.startsWith(".") }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Copia le immagini scelte dentro [dest], e dice com'è andata.
     *
     * ⚠️⚠️ **NON SOVRASCRIVE MAI: un nome già preso diventa `nome (2).ext`.** Una copia
     * che sovrascrive è una cancellazione travestita, e questa funzione è nata proprio
     * perché la copia doveva essere l'operazione che non può far danni.
     * ⚠️ **Una copia andata storta non ferma le altre**: su cinquanta file, un permesso
     * negato o un disco pieno a metà strada non deve buttare via il lavoro fatto. Il conto
     * dei falliti torna a chi chiama, che lo dice.
     * ⚠️ **Passa dal resolver e non dal `File`**, al contrario di sposta e rinomina: qui
     * l'origine si deve solo **leggere**, e un flusso si apre anche su un indirizzo che
     * non ha un percorso sul disco. Le altre due il percorso ce l'hanno per forza, perché
     * devono toccare il file dov'è.
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
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { input.copyTo(it) }
                    } ?: return@runCatching false
                    written += target.absolutePath
                    true
                }.getOrDefault(false)
                if (ok) done++ else failed++
            }
            scan(context, written)
            Outcome(done, failed)
        }

    /**
     * Sposta le immagini scelte dentro [dest].
     *
     * ⚠️⚠️ **PRIMA PROVA A RINOMINARE, e non è un'ottimizzazione**: dentro la stessa
     * memoria un `renameTo` è istantaneo e non tocca i byte, mentre copiare e cancellare
     * trenta fotografie da dieci megabyte vuol dire trecento megabyte letti e riscritti,
     * con la finestra in cui esistono due volte. Il ripiego serve **solo** quando si
     * cambia volume, dove `renameTo` fallisce per costruzione.
     * ⚠️⚠️ **Nel ripiego l'originale si cancella solo a copia RIUSCITA, e se la
     * cancellazione fallisce si butta via la copia**: il contrario lascerebbe o un file
     * perso o due file uguali, e il secondo è quello che si scopre sei mesi dopo.
     * ⚠️ **Spostare dentro la cartella in cui si è già non fa niente**, e va detto qui:
     * senza questo controllo `freeName` inventerebbe `nome (2).ext` e lo spostamento
     * diventerebbe una duplicazione col nome storpiato.
     */
    suspend fun move(context: Context, uris: List<Uri>, dest: File): Outcome =
        withContext(Dispatchers.IO + NonCancellable) {
            var done = 0
            var failed = 0
            val touched = ArrayList<String>(uris.size * 2)
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
                val ok = runCatching {
                    if (from.renameTo(to)) return@runCatching true
                    from.inputStream().use { input -> to.outputStream().use { input.copyTo(it) } }
                    if (from.delete()) {
                        true
                    } else {
                        to.delete()
                        false
                    }
                }.getOrDefault(false)
                if (ok) {
                    done++
                    touched += from.absolutePath
                    touched += to.absolutePath
                } else {
                    failed++
                }
            }
            scan(context, touched)
            Outcome(done, failed)
        }

    /**
     * Cancella le immagini scelte, per davvero.
     *
     * ⚠️⚠️ **NON C'È UN CESTINO, e chi chiama deve averlo chiesto prima.** Il MediaStore
     * ne ha uno dall'API 30 (`IS_TRASHED`), ma ci si finisce solo passando dal provider
     * con la richiesta apposita; un file cancellato dal disco sparisce e basta. Qui si
     * cancella dal disco, quindi l'operazione è irreversibile e la conferma non è una
     * cortesia.
     * ⚠️ **Prima il file e poi, come ripiego, la riga**: `contentResolver.delete` su una
     * riga del MediaStore porta via anche il file, ma vale solo per gli indirizzi che una
     * riga ce l'hanno, e nella griglia ne girano anche di `file://` (vedi il ripiego sul
     * disco di `Folder`). Il `File` invece li copre tutti e due.
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
     * ⚠️⚠️ **L'ORDINE È ALFABETICO E NON QUELLO DELLA GRIGLIA** (istruzione dell'utente,
     * 2026-08-29: *l'ordine NON deve dipendere dall'ordine di visualizzazione in griglia,
     * bensì sempre e comunque ordine alfabetico A-Z*). La griglia ordina per data, quindi
     * numerare nel suo ordine darebbe una sequenza che dipende da quando le foto sono
     * state scattate invece che da come si chiamano.
     * ⚠️⚠️ **DUE FASI, e senza la prima si perderebbero dei file.** Rinominando uno per
     * uno, `b.jpg -> a.jpg` sovrascriverebbe l'`a.jpg` che deve ancora diventare
     * `c.jpg`, e i cicli fra i nomi non sono un caso di laboratorio: succedono ogni volta
     * che si rinumera una serie già numerata. Quindi prima tutti i file vanno su un nome
     * temporaneo, e solo dopo prendono quello definitivo, quando nessuno dei nomi voluti
     * è più occupato da uno di loro.
     * ⚠️ **I nomi temporanei cominciano col punto**, cioè sono nascosti: se qualcosa
     * interrompesse l'app fra le due fasi, resterebbero dei file di servizio invece di
     * comparire nella galleria come fotografie senza nome.
     * ⚠️ **Il numero è legato alla POSIZIONE, non al giro**: si calcola tutto prima, così
     * un file che non si riesce a spostare lascia il suo numero vuoto invece di far
     * slittare di uno tutti quelli dopo.
     * ⚠️ **L'estensione non si tocca mai** (istruzione dell'utente): viene dal file, non
     * dal template, e chi scrive un punto nel template si ritrova quel punto **prima**
     * dell'estensione vera.
     */
    suspend fun rename(
        context: Context,
        uris: List<Uri>,
        template: String,
        start: Int
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
            val wanted = renderName(template, start + at, file.name.substringAfterLast('.', ""))
            val temp = if (dir == null) null else File(dir, ".aiv-$stamp-$at")
            if (temp != null && runCatching { file.renameTo(temp) }.getOrDefault(false)) {
                parked += temp to (file to wanted)
            } else {
                failed++
            }
        }

        // Fase 2: il nome vero. Un nome già preso adesso è per forza di un file ESTRANEO
        // alla selezione, e allora vale la stessa regola della copia.
        for ((temp, job) in parked) {
            val (origin, wanted) = job
            touched += origin.absolutePath
            val target = origin.parentFile?.let { freeName(it, wanted) }
            if (target != null && runCatching { temp.renameTo(target) }.getOrDefault(false)) {
                done++
                touched += target.absolutePath
            } else {
                failed++
                // ⚠️ Si rimette com'era: lasciare un file col nome temporaneo, cioè
                // nascosto, vorrebbe dire farlo sparire dalla galleria senza dirlo.
                runCatching { temp.renameTo(origin) }
            }
        }

        scan(context, touched)
        Outcome(done, failed)
    }

    /**
     * I nomi delle immagini scelte, **nell'ordine in cui la rinomina le numererà**.
     *
     * ⚠️ Esiste per l'anteprima del dialogo, e deve usare lo stesso ordinamento di
     * [rename] o l'anteprima mentirebbe. Per questo l'ordine sta in [ordered], che è una
     * funzione sola usata da tutte e due.
     */
    suspend fun namesOf(context: Context, uris: List<Uri>): List<String> =
        withContext(Dispatchers.IO) { ordered(context, uris).map { it.name } }

    /** Quante ne sono passate e quante no: la risposta di ogni operazione su un gruppo. */
    data class Outcome(val done: Int, val failed: Int)

    /**
     * I file dietro gli indirizzi scelti, in ordine alfabetico.
     *
     * ⚠️ Gli indirizzi che non arrivano a un file sul disco **cadono**, e chi chiama conta
     * la differenza come fallita: non c'è modo di rinominare qualcosa che non si sa dov'è.
     * ⚠️ Il percorso rompe i pareggi, perché due file con lo stesso nome in due cartelle
     * diverse non devono ordinarsi a caso: la selezione viene da una cartella sola, ma
     * questo non è un motivo per lasciare l'ordine indefinito.
     */
    private fun ordered(context: Context, uris: List<Uri>): List<File> =
        uris.mapNotNull { fileOf(context, it) }
            .sortedWith { a, b ->
                val byName = naturalCompare(a.name, b.name)
                if (byName != 0) byName else a.absolutePath.compareTo(b.absolutePath)
            }

    /**
     * Un nome libero dentro [dir], partendo da [name].
     *
     * ⚠️ **L'estensione non si tocca e il contatore va prima di lei**: `foto (2).jpg` e non
     * `foto.jpg (2)`, o il file smetterebbe di essere una fotografia per il sistema.
     * ⚠️ Il tetto esiste perché questo è un ciclo su un disco che qualcun altro può
     * scrivere: senza, un caso patologico girerebbe per sempre invece di fallire.
     */
    private fun freeName(dir: File, name: String): File {
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

    /** Il nome del file dietro un indirizzo, che è quello che la copia deve conservare. */
    private fun displayName(context: Context, uri: Uri): String? {
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
     * Il file vero dietro un indirizzo, e `null` quando non ce n'è uno.
     *
     * ⚠️⚠️ **`DATA` è deprecata e serve lo stesso**, come già in `Folder`: è la sola
     * colonna che dice **dove** sta il file, e spostare o rinominare vuol dire toccare il
     * file dov'è. Con l'accesso a tutti i file quel percorso è scrivibile, ed è la ragione
     * per cui queste operazioni possono esistere.
     * ⚠️ **Si controlla che sia davvero un file**: una riga del MediaStore può restare
     * dopo che il file è sparito, e agire su un percorso morto conterebbe come riuscito.
     */
    private fun fileOf(context: Context, uri: Uri): File? {
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
     * Avvisa il MediaStore dei percorsi toccati, e **aspetta** che abbia finito.
     *
     * ⚠️⚠️ **SENZA L'AVVISO LA GALLERIA RESTA INDIETRO**: scrivendo i file a mano il
     * provider non se ne accorge, quindi le copie non comparirebbero e le cancellate
     * resterebbero, finché il sistema non rifà una scansione per conto suo, cioè quando
     * vuole lui. Vale sui **due** percorsi di uno spostamento o di una rinomina, la
     * partenza e l'arrivo: il primo toglie la riga vecchia, il secondo mette quella nuova.
     * ⚠️⚠️ **E SI ASPETTA, o la schermata si ricaricherebbe sui dati di prima.** La
     * scansione è asincrona: chi rilegge la cartella un istante dopo averla chiesta si
     * ritrova ancora le foto appena cancellate, che è precisamente il difetto che
     * l'operazione doveva togliere.
     * ⚠️ Il tetto di attesa non è pessimismo: un richiamo che non arriva mai
     * bloccherebbe la selezione per sempre, e una galleria in ritardo di qualche secondo
     * è un guaio molto più piccolo.
     */
    private suspend fun scan(context: Context, paths: List<String>) {
        val list = paths.distinct().filter { it.isNotBlank() }
        if (list.isEmpty()) return
        runCatching {
            withTimeoutOrNull(SCAN_WAIT_MS) {
                suspendCancellableCoroutine { cont ->
                    val left = AtomicInteger(list.size)
                    MediaScannerConnection.scanFile(context, list.toTypedArray(), null) { _, _ ->
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
 * ⚠️⚠️ **OGNI GRUPPO DI CANCELLETTI DIVENTA IL NUMERO, e la lunghezza del gruppo è il
 * numero di cifre**: due cancelletti dànno `01`, tre dànno `001`. Si sostituiscono
 * **tutti** i gruppi e non solo il primo, perché un cancelletto lasciato dentro un nome di
 * file si legge come un difetto, non come una scelta.
 * ⚠️ **Un numero più lungo del gruppo non si taglia**: con due cancelletti il file numero
 * 100 si chiama `100` e non `00`. Perdere una cifra vorrebbe dire due file con lo stesso
 * nome, cioè il contrario di quello che la numerazione serve a fare.
 * ⚠️ **La barra si ripulisce invece di essere rifiutata**, come nel dialogo della cartella
 * nuova: è l'unico carattere che su Android non può stare in un nome, e una incollata per
 * sbaglio trasformerebbe un file in una cartella inesistente.
 * ⚠️ **Punti e spazi in coda cadono**: un nome che finisce così è legale su ext4 e non lo
 * è su una scheda formattata FAT, e la rinomina fallirebbe solo su certi telefoni.
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
 * ⚠️ **Minimo due, e poi quante ne vuole il numero più alto** (istruzione dell'utente,
 * 2026-08-29: *il numero di # (cifre) dev'essere minimo 2, e poi proposto in base al totale
 * di file da rinominare: es. 84 -> ##, 124 -> ###*). Il minimo esiste perché `Foto 1` e
 * `Foto 2` si ordinano male appena si arriva a dieci, ed è il difetto che la numerazione
 * dovrebbe evitare.
 */
internal fun hashesFor(count: Int, start: Int): Int {
    val highest = start.toLong() + count.toLong() - 1L
    return maxOf(2, highest.coerceAtLeast(0L).toString().length)
}

/**
 * Il template da proporre, ricavato dal nome del **primo** file dell'ordine alfabetico.
 *
 * ⚠️⚠️ **SI TAGLIA ALLA PRIMA CIFRA** (istruzione dell'utente, 2026-08-29: *il template di
 * base proposto per il nome dev'essere il nome del primo file senza parti extra. Es.
 * 'Museo1ABC_02' -> template proposto: `Museo ##`*). La parte davanti alle cifre è
 * quella che una persona ha scritto apposta; tutto quello che viene dopo è la numerazione
 * della macchina fotografica, cioè proprio quello che si sta per rifare.
 * ⚠️ **Un nome che comincia con una cifra si tiene intero**: tagliarlo darebbe un template
 * vuoto, e una proposta vuota è peggio di una proposta lunga.
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
 * ⚠️⚠️ **NON È UN CONFRONTO LESSICOGRAFICO, e la differenza si vede subito**: in ordine di
 * carattere `IMG_10` viene prima di `IMG_9`, quindi una cartella di foto numerate si
 * rinumererebbe a casaccio, che è esattamente il caso per cui la rinomina in blocco esiste.
 * L'utente ha chiesto *ordine alfabetico A-Z* in contrapposizione all'ordine della griglia,
 * che è per data: questo è quell'ordine, con l'unica lettura che non tradisce le serie
 * numerate.
 * ⚠️ **Gli zeri iniziali non contano nel valore ma rompono i pareggi**: `foto 007` e
 * `foto 7` valgono lo stesso numero, e senza l'ultimo confronto sul testo grezzo il loro
 * ordine dipenderebbe da come la lista è arrivata.
 */
private fun naturalCompare(a: String, b: String): Int {
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

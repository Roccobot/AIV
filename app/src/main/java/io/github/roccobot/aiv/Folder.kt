package io.github.roccobot.aiv

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.text.Collator
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Le altre immagini della stessa cartella, perché un'immagine aperta si possa sfogliare.
 *
 * ⚠️ **Il selettore di sistema non risponde a questa domanda**: restituisce il permesso per un
 * solo elemento, da Android 13 un indirizzo `content://media/picker/...` che dà accesso a
 * quell'immagine e a niente intorno. Non c'è un genitore da risalire: la cartella vive nel
 * MediaStore, e leggerlo vuole un permesso, quello che l'utente ha accettato di dare.
 */
object Folder {

    /** Una cartella, in ordine, e la posizione dell'immagine sullo schermo dentro di lei. */
    data class Series(val items: List<Uri>, val index: Int) {
        val size: Int get() = items.size
        fun at(position: Int): Uri? = items.getOrNull(position)

        /**
         * Le stesse immagini dall'ultima alla prima, con quella aperta ancora al suo posto.
         *
         * ⚠️ **Si gira la serie, non il gesto**: indice mostrato, verso della strisciata e
         * ordine della lista vengono tutti da qui e restano coerenti. Girando il solo gesto il
         * contatore farebbe `5/7` e poi `4/7` mentre il dito va avanti.
         * È la sua stessa inversa: una funzione sola gira e torna indietro (vedi
         * `ViewerViewModel.step`).
         */
        fun reversed(): Series = Series(items.reversed(), items.lastIndex - index)

        /**
         * Di quanti posti si sposta davvero una strisciata di [delta], saltando i filmati
         * quando [skipVideos].
         *
         * ⚠️ **È sulla serie e non nel modello**, perché la usano il modello per il passo e la
         * schermata per le vicine che scivolano dal bordo: con due conti diversi, sfogliando
         * compariva per un istante il video che l'impostazione esclude (riscontro dell'utente).
         * Se di là ci sono solo video il passo esce dalla serie, e chi chiama trova `null` da
         * [at], come al capolinea: fermarsi sul video mostrerebbe quello che l'impostazione
         * esclude.
         */
        fun stepping(delta: Int, skipVideos: Boolean): Int {
            if (!skipVideos || delta == 0) return delta
            val verso = if (delta > 0) 1 else -1
            var step = delta
            while (true) {
                val uri = at(index + step) ?: return step
                if (!Videos.isVideo(uri)) return step
                step += verso
            }
        }

        /**
         * L'indirizzo che una strisciata di [delta] raggiunge davvero, e `null` se non c'è.
         *
         * È [stepping] più [at], per chi vuole l'immagine e non il numero di posti: la
         * schermata, per disegnare le vicine e per sapere se il gesto ha ancora dove andare.
         */
        fun toward(delta: Int, skipVideos: Boolean): Uri? = at(index + stepping(delta, skipVideos))
    }

    /**
     * L'esito della ricerca, col perché quando non c'è una serie.
     *
     * ⚠️ **Non un `Series?`**: con un `null` e basta, una strisciata che non fa niente non si
     * distingue da una strisciata guasta. L'esito che dice la ragione rende la funzione
     * diagnosticabile, e la riga dei dettagli lo stampa.
     */
    sealed interface Lookup {
        /** La cartella c'è, e questa è. */
        data class Found(val series: Series) : Lookup

        /** Manca il permesso: la sola causa che la persona può rimuovere. */
        data object NoPermission : Lookup

        /**
         * L'indirizzo non sa dire nome e peso di quello che ha aperto.
         * Distinto da [NotInGallery]: qui la domanda non ha raggiunto il MediaStore, e i due
         * casi vogliono correzioni diverse.
         */
        data object Unreadable : Lookup

        /**
         * Il MediaStore non conosce questa immagine: una chat, il Web, un file sciolto.
         * [detail] contiene quello che si è riusciti a leggere, per sapere quale chiave ha
         * fallito. La riga dei dettagli lo stampa.
         */
        data class NotInGallery(val detail: String) : Lookup

        /** La cartella esiste e ha questa immagine sola: non c'è niente da sfogliare. */
        data object Alone : Lookup

        /**
         * La cartella ha più immagini ma quella aperta non è fra loro.
         * ⚠️ Non dovrebbe succedere: se compare, il difetto è nel modo in cui si risale alla
         * riga, non nei dati.
         */
        data object Lost : Lookup

        /** La serie quando c'è, e null per tutti gli esiti che spiegano perché non c'è. */
        val seriesOrNull: Series? get() = (this as? Found)?.series

        /**
         * Lo stesso esito con la serie girata; gli esiti senza serie restano sé stessi.
         *
         * È sull'esito e non solo sulla serie perché chi gira non sa quale caso ha in mano:
         * senza, ogni chiamante ripeterebbe il `when`.
         */
        fun reversed(): Lookup = if (this is Found) Found(series.reversed()) else this
    }

    /**
     * Una cartella di immagini, come la mostra l'elenco iniziale.
     *
     * [cover] è l'immagine più recente, e costa zero query: [buckets] scorre le righe in ordine
     * di data, quindi la prima riga di ogni cartella è la sua copertina predefinita. È `null`
     * quando il provider non serve la colonna dell'id, e la cartella si mostra lo stesso col suo
     * simbolo. Non è l'immagine con cui la cartella si apre: col verso cronologico l'apertura
     * parte dall'altro capo (vedi `atSequenceStart`).
     *
     * [path] è il percorso della cartella sul disco, per l'esclusione (`Settings.hiddenFolders`,
     * senza la radice del volume: vedi `portablePath`); non costa una colonna in più. ⚠️ Può
     * essere `null`, quando il provider non serve la colonna `DATA`, e allora la cartella non si
     * può nascondere per percorso: una stringa vuota si confonderebbe con la radice.
     */
    data class Bucket(
        val id: Long,
        val name: String,
        /**
         * Quante immagini e quanti video, contati a parte (richiesta dell'utente: due
         * contatori con due icone). Il tipo è già nella riga, e il conto si fa nella stessa
         * passata.
         */
        val pictures: Int,
        val clips: Int,
        val cover: Uri?,
        val path: String? = null,
        /**
         * Quando è stato modificato il file più recente della cartella, in secondi: la chiave di
         * [FolderOrder.RECENT].
         *
         * È il `DATE_MODIFIED` della prima riga, che arriva per prima come la copertina: non
         * costa query.
         */
        val latest: Long = 0L
    ) {
        /** Quanti elementi in tutto, per chi non ha bisogno della distinzione. */
        val count: Int get() = pictures + clips
    }

    /**
     * Le cartelle di immagini del telefono, in ordine alfabetico (richiesta dell'utente, 1.83:
     * davanti alle cartelle si sa già il nome di quella che si cerca). Un altro ordine lo decide
     * `FolderSelection.arrange`, con [Bucket.latest].
     *
     * ⚠️ **Si ordina alla fine e non nella query**: le righe arrivano per data, così la prima di
     * ogni cartella è la sua immagine più recente, cioè la copertina e [Bucket.latest].
     * Il confronto passa da un [Collator]: `compareTo` fra stringhe ordina per codice, con le
     * maiuscole prima delle minuscole e le lettere accentate in fondo.
     * ⚠️ **Il raggruppamento si fa qui e non nella query**: il `GROUP BY` del MediaStore ha
     * cambiato forma fra le versioni di Android, e alcuni provider lo rifiutano; una passata
     * sulle righe è uguale dappertutto.
     */
    suspend fun buckets(context: Context): List<Bucket> = withContext(Dispatchers.IO) {
        if (!granted(context)) return@withContext emptyList()
        val found = LinkedHashMap<Long, Bucket>()
        runCatching {
            context.contentResolver.query(
                TABLE,
                COLUMNS,
                MEDIA,
                null,
                "${MediaStore.Images.Media.DATE_MODIFIED} DESC, ${MediaStore.Images.Media._ID} DESC"
            )?.use { c ->
                // ⚠️ Le colonne si risolvono una volta, prima del ciclo: dentro, un
                // `getString` su una colonna che il provider non serve darebbe errore a metà
                // elenco, e quello raccolto fin lì tornerebbe come se fosse tutto.
                val bucketAt = c.column(MediaStore.Images.Media.BUCKET_ID)
                    ?: return@use
                val nameAt = c.column(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val idAt = c.column(MediaStore.Images.Media._ID)
                @Suppress("DEPRECATION")
                val pathAt = c.column(MediaStore.Images.Media.DATA)
                val kindAt = c.column(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val dateAt = c.column(MediaStore.Images.Media.DATE_MODIFIED)
                while (c.moveToNext()) {
                    val id = c.getLong(bucketAt)
                    val clip = c.isClip(kindAt)
                    val seen = found[id]
                    found[id] = if (seen == null) {
                        // Il nome della cartella può mancare: allora si ricava dal percorso
                        // della riga.
                        val name = nameAt?.let { c.getString(it) }?.takeIf { it.isNotBlank() }
                            ?: pathAt?.let { c.getString(it) }?.trimEnd('/')
                                ?.substringBeforeLast('/')?.substringAfterLast('/')
                                ?.takeIf { it.isNotBlank() }
                            ?: id.toString()
                        // ⚠️ La copertina si prende solo qui, sulla prima riga, che è la più
                        // recente: presa anche dopo diventerebbe la più vecchia.
                        Bucket(
                            id = id,
                            name = name,
                            pictures = if (clip) 0 else 1,
                            clips = if (clip) 1 else 0,
                            cover = idAt?.let { uriOf(c.getLong(it), clip) },
                            path = folderOf(pathAt?.let { c.getString(it) }),
                            latest = dateAt?.let { c.getLong(it) } ?: 0L
                        )
                    } else if (clip) {
                        seen.copy(clips = seen.clips + 1)
                    } else {
                        seen.copy(pictures = seen.pictures + 1)
                    }
                }
            }
        }
        // Il collatore si prende una volta per chiamata: `getInstance` legge la lingua in vigore
        // e costruisce le sue tabelle.
        val ordine = Collator.getInstance()
        found.values.sortedWith(compareBy(ordine) { it.name })
    }

    /**
     * Tutte le immagini e tutti i video delle cartelle che l'elenco iniziale mostra, dalla più
     * recente.
     *
     * Serve a 'Genera miniature' ([Warmup]). Una cartella nascosta resta fuori, e il minuto di
     * 'Mostra nascoste' non conta: è un prestito e non una scelta.
     * Una query sola, come [buckets], con lo stesso filtro e lo stesso ordine; il confronto con
     * le nascoste si fa una volta per cartella, non per riga.
     * Una riga senza percorso entra, come in `Bucket.isHidden`: una miniatura in più costa meno
     * di una cartella visibile lasciata fuori.
     * Dalla più recente, perché sono le immagini che si aprono per prime: se la generazione si
     * ferma a metà, quello che è fatto è quello che serve di più.
     */
    suspend fun everything(context: Context, hidden: Set<String>,
        selection: FolderSelection = FolderSelection(hidden = hidden)): List<Uri> =
        withContext(Dispatchers.IO) {
            if (!granted(context)) return@withContext emptyList()
            val found = mutableListOf<Uri>()
            runCatching {
                context.contentResolver.query(
                    TABLE,
                    COLUMNS,
                    MEDIA,
                    null,
                    "${MediaStore.Images.Media.DATE_MODIFIED} DESC, ${MediaStore.Images.Media._ID} DESC"
                )?.use { c ->
                    // ⚠️ Le colonne si risolvono una volta, per la ragione scritta in [buckets].
                    val idAt = c.column(MediaStore.Images.Media._ID) ?: return@use
                    @Suppress("DEPRECATION")
                    val pathAt = c.column(MediaStore.Images.Media.DATA)
                    val kindAt = c.column(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    val esclusa = HashMap<String?, Boolean>()
                    while (c.moveToNext()) {
                        val cartella = folderOf(pathAt?.let { c.getString(it) })
                        val fuori = esclusa.getOrPut(cartella) {
                            !selection.visible(cartella)
                        }
                        if (!fuori) found.add(uriOf(c.getLong(idAt), c.isClip(kindAt)))
                    }
                }
            }
            found
        }

    /**
     * La cartella che contiene il file in [file], cioè il suo percorso meno il nome.
     *
     * Una funzione sola per [buckets] e per [everything]: due modi di rispondere darebbero due
     * elenchi di nascoste che non coincidono.
     */
    private fun folderOf(file: String?): String? =
        file?.substringBeforeLast('/')?.takeIf { it.isNotBlank() }

    /**
     * Quanto pesa una cartella, quante immagini e quanti video contiene, e dov'è su disco: i
     * dati delle pastiglie dell'intestazione, che dalla griglia non si sanno (là ci sono gli
     * indirizzi, non le righe).
     *
     * ⚠️ **Una query sola per tutti i numeri**: il peso di un `content://` si legge dalla sua
     * riga, quindi partendo dagli indirizzi servirebbe una query per file. Si chiede al bucket,
     * con la stessa condizione con cui la cartella è stata letta.
     * Un peso che non si legge vale zero: una riga senza `SIZE` esiste (un file appena arrivato,
     * un provider avaro), e un totale un po' più basso è meglio di nessun totale.
     */
    suspend fun weigh(context: Context, bucket: Long): Facts = withContext(Dispatchers.IO) {
        if (!granted(context)) return@withContext Facts()
        var bytes = 0L
        var clips = 0
        var shots = 0
        var path: String? = null
        runCatching {
            context.contentResolver.query(
                TABLE,
                arrayOf(
                    MediaStore.Images.Media.SIZE,
                    MediaStore.Files.FileColumns.MEDIA_TYPE,
                    @Suppress("DEPRECATION") MediaStore.MediaColumns.DATA
                ),
                media("${MediaStore.Images.Media.BUCKET_ID} = ?"),
                arrayOf(bucket.toString()),
                null
            )?.use { c ->
                val sizeAt = c.column(MediaStore.Images.Media.SIZE)
                val kindAt = c.column(MediaStore.Files.FileColumns.MEDIA_TYPE)
                @Suppress("DEPRECATION")
                val dataAt = c.column(MediaStore.MediaColumns.DATA)
                while (c.moveToNext()) {
                    bytes += sizeAt?.let { if (c.isNull(it)) 0L else c.getLong(it) } ?: 0L
                    if (c.isClip(kindAt)) clips++ else shots++
                    if (path == null && dataAt != null && !c.isNull(dataAt)) {
                        path = c.getString(dataAt).substringBeforeLast('/', "").ifBlank { null }
                    }
                }
            }
        }
        Facts(bytes, clips, shots, path)
    }

    /**
     * Rinomina la cartella in [path], e dice dove è finita (tocco lungo sul nome
     * dell'intestazione, richiesta dell'utente).
     *
     * ⚠️ **Si rinomina sul disco e non nel `MediaStore`**: il provider conosce i file, non le
     * directory, e una cartella con file che non indicizza resterebbe mezza spostata. Con
     * l'accesso a tutti i file, `renameTo` è atomico e sposta tutto quello che c'è dentro.
     *
     * ⚠️ **Il `BUCKET_ID` cambia**: il MediaStore lo ricava dal percorso, quindi la cartella
     * rinominata è per lui una cartella nuova. Chi chiama riapre la griglia sull'identificatore
     * nuovo e vi trasferisce quello che era appeso al vecchio (la copertina scelta e il colore).
     * L'identificatore nuovo si chiede al MediaStore, non si calcola: la formula è un dettaglio
     * del provider, già cambiato fra due versioni di Android, e un numero sbagliato darebbe una
     * cartella vuota senza errori.
     *
     * Lo scan riguarda i file, non la cartella: si raccolgono i percorsi prima (per togliere le
     * righe vecchie) e dopo (per le nuove), come [FileTree.scan] fa per copie e spostamenti.
     * ⚠️ **Il nome si controlla anche qui, non solo nella finestra**: una barra trasformerebbe la
     * rinomina in uno spostamento, e i due nomi speciali del file system porterebbero altrove.
     */
    suspend fun rename(context: Context, path: String, name: String): Renamed =
        withContext(Dispatchers.IO) {
            val pulito = name.trim()
            if (pulito.isEmpty() || pulito.contains('/') || pulito == "." || pulito == "..") {
                return@withContext Renamed.BAD_NAME
            }
            val vecchia = File(path)
            val casa = vecchia.parentFile
            if (casa == null || !vecchia.isDirectory) return@withContext Renamed.FAILED
            val nuova = File(casa, pulito)
            if (nuova.absolutePath == vecchia.absolutePath) return@withContext Renamed.FAILED
            // Un nome già preso non si rinumera come per i file: l'utente ha scritto un nome, e
            // riceverne un altro sarebbe una sorpresa. Si dice che è occupato.
            if (nuova.exists()) return@withContext Renamed.TAKEN
            val prima = runCatching {
                vecchia.walkTopDown().filter { it.isFile }.map { it.absolutePath }.toList()
            }.getOrDefault(emptyList())
            if (!runCatching { vecchia.renameTo(nuova) }.getOrDefault(false)) {
                return@withContext Renamed.FAILED
            }
            val dopo = prima.map { nuova.absolutePath + it.removePrefix(vecchia.absolutePath) }
            FileTree.scan(context, prima + dopo)
            Renamed(Renamed.Esito.DONE, bucketAt(context, nuova.absolutePath), nuova.absolutePath)
        }

    /** Com'è andata una rinomina di cartella, e dov'è finita. */
    class Renamed(val esito: Esito, val bucket: Long?, val path: String?) {

        enum class Esito { DONE, TAKEN, BAD_NAME, FAILED }

        companion object {
            val TAKEN = Renamed(Esito.TAKEN, null, null)
            val BAD_NAME = Renamed(Esito.BAD_NAME, null, null)
            val FAILED = Renamed(Esito.FAILED, null, null)
        }
    }

    /**
     * La cartella del MediaStore in [path], o `null` quando non contiene immagini: la chiave del
     * suo colore (`FolderTints`), per 'Faro', che parte da una cartella su disco.
     */
    suspend fun bucketOf(context: Context, path: String): Long? =
        withContext(Dispatchers.IO) { bucketAt(context, path) }

    /**
     * L'identificatore che il `MediaStore` dà alla cartella in [path], letto da un file che vi
     * è dentro.
     *
     * `null` quando la cartella non ha media indicizzati: allora non compare nell'elenco
     * iniziale, e non c'è identificatore da conoscere.
     * ⚠️ **Il `LIKE` restringe, il confronto esatto decide**: da solo prenderebbe le
     * sottocartelle, che hanno un bucket loro, e i suoi jolly (`%` e `_`, comune nei nomi)
     * allargherebbero il risultato. Senza il `LIKE` la query leggerebbe tutta la galleria, senza
     * il confronto risponderebbe con la cartella sbagliata.
     */
    private fun bucketAt(context: Context, path: String): Long? = runCatching {
        context.contentResolver.query(
            TABLE,
            arrayOf(MediaStore.Images.Media.BUCKET_ID, @Suppress("DEPRECATION") MediaStore.MediaColumns.DATA),
            media("${@Suppress("DEPRECATION") MediaStore.MediaColumns.DATA} LIKE ?"),
            arrayOf("$path/%"),
            null
        )?.use { c ->
            val bucketAt = c.column(MediaStore.Images.Media.BUCKET_ID)
            @Suppress("DEPRECATION")
            val dataAt = c.column(MediaStore.MediaColumns.DATA)
            if (bucketAt == null || dataAt == null) return@use null
            while (c.moveToNext()) {
                if (c.isNull(dataAt)) continue
                if (c.getString(dataAt).substringBeforeLast('/', "") == path) {
                    return@use c.getLong(bucketAt)
                }
            }
            null
        }
    }.getOrNull()

    /**
     * Quello che di una cartella si sa solo interrogando il `MediaStore`.
     *
     * Zero vale anche 'non lo so ancora': le pastiglie si mostrano solo sopra lo zero, quindi una
     * cartella non ancora pesata e una vuota si comportano allo stesso modo, senza lampeggiare.
     * Tutti i campi vengono dalla stessa query ([weigh]).
     *
     * @param shots quante immagini (risposta a `d-front-altro`): le righe che non sono video,
     *   contate e non sottratte dal totale, perché una riga di tipo sconosciuto non è né l'una né
     *   l'altra cosa.
     * @param path la cartella su disco, presa dalla prima riga che ce l'ha: serve alla rinomina
     *   della cartella (`ViewerViewModel.renameFolder`).
     *   ⚠️ **Si legge da `DATA`, deprecata, e non da `RELATIVE_PATH`**, che esiste dall'API 29,
     *   mentre l'app arriva ad Android 9. Leggere `DATA` è ancora lecito; è deprecato scriverla.
     */
    data class Facts(
        val bytes: Long = 0L,
        val clips: Int = 0,
        val shots: Int = 0,
        val path: String? = null
    )

    /**
     * La cartella intera, posizionata sulla prima immagine dell'ordine di base, cioè la più
     * recente.
     *
     * ⚠️ **Chi apre deve poi spostarsi sull'inizio della sequenza scelta** (vedi
     * `ViewerViewModel.openGrid`): col verso invertito la più recente è l'ultima.
     * Una cartella con un'immagine sola qui vale [Lookup.Found] e non [Lookup.Alone], al
     * contrario di [seriesAround]: qui la domanda è 'apri questa cartella', e la risposta deve
     * comunque contenere l'immagine da aprire.
     */
    suspend fun newestIn(context: Context, bucket: Long): Lookup = withContext(Dispatchers.IO) {
        if (!granted(context)) return@withContext Lookup.NoPermission
        val ids = urisOf(context, bucket)
        if (ids.isEmpty()) return@withContext Lookup.Unreadable
        Lookup.Found(Series(ids, 0))
    }

    /**
     * Le immagini del telefono il cui **nome** contiene [text], in tutte le cartelle.
     *
     * ⚠️ **Risponde con un [Lookup] e non con una lista**: i risultati sono una serie come quella
     * di una cartella, quindi la griglia li mostra, il visualizzatore li sfoglia, la selezione
     * multipla ci lavora e il verso segue l'impostazione, senza un secondo percorso parallelo.
     * ⚠️ **Le cartelle nascoste restano fuori** ([selection]), o una cartella esclusa
     * riapparirebbe in fondo a una ricerca: le impostazioni le sa il modello, il MediaStore no.
     * `LIKE` distingue gli accenti e non le maiuscole: SQLite normalizza solo le lettere ASCII,
     * quindi *foto* trova *FOTO* ma *città* non trova *CITTA*.
     * `%` e `_` nel testo cercato si neutralizzano: senza l'escape `foto_01` troverebbe anche
     * `foto-01`.
     * Nessun tetto ai risultati: la griglia è pigra, e un tetto silenzioso direbbe 'non c'è
     * altro' mentendo.
     */
    suspend fun byName(
        context: Context,
        text: String,
        hidden: Set<String>,
        bucket: Long? = null,
        selection: FolderSelection = FolderSelection(hidden = hidden)
    ): Lookup = withContext(Dispatchers.IO) {
        if (!granted(context)) return@withContext Lookup.NoPermission
        val needle = text.trim()
        if (needle.isEmpty()) return@withContext Lookup.Found(Series(emptyList(), 0))
        val pattern = "%" + needle.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
        val found = mutableListOf<Uri>()
        /*
         * Il confine della cartella è una condizione della query, non un filtro dopo: il
         * `BUCKET_ID` è indicizzato, e chiedere tutta la galleria a ogni lettera battuta per
         * scartarne quasi tutta sarebbe lento.
         * Le cartelle nascoste si filtrano lo stesso: in una ricerca ristretta il caso non
         * capita, ma il filtro non sa da dove arriva il confine.
         */
        val dove = if (bucket == null) {
            media("${MediaStore.Images.Media.DISPLAY_NAME} LIKE ? ESCAPE '\\'")
        } else {
            media(
                "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ? ESCAPE '\\' AND " +
                    "${MediaStore.Images.Media.BUCKET_ID} = ?"
            )
        }
        val args = if (bucket == null) arrayOf(pattern) else arrayOf(pattern, bucket.toString())
        runCatching {
            context.contentResolver.query(
                TABLE,
                COLUMNS,
                dove,
                args,
                "${MediaStore.Images.Media.DATE_MODIFIED} DESC, ${MediaStore.Images.Media._ID} DESC"
            )?.use { c ->
                val idAt = c.column(MediaStore.Images.Media._ID) ?: return@use
                @Suppress("DEPRECATION")
                val pathAt = c.column(MediaStore.Images.Media.DATA)
                val kindAt = c.column(MediaStore.Files.FileColumns.MEDIA_TYPE)
                while (c.moveToNext()) {
                    val dir = pathAt?.let { c.getString(it) }
                        ?.substringBeforeLast('/')
                        ?.takeIf { it.isNotBlank() }
                    if (!selection.visible(dir)) continue
                    found += uriOf(c.getLong(idAt), c.isClip(kindAt))
                }
            }
        }
        Lookup.Found(Series(found, 0))
    }

    /**
     * Tutte le immagini e i video delle cartelle che la griglia mostra, nell'ordine del Feed
     * ([Feed.order]), o `null` senza il permesso.
     *
     * Le cartelle sono quelle di [selection], lo stesso filtro della griglia e della ricerca
     * (scelta D1 dell'utente). Il cestino è fuori per costruzione: AIV GitHub lo tiene nella
     * cartella dell'app, dove il MediaStore non guarda, e il cestino di sistema di AIV Play è
     * escluso di serie da una query.
     * `DATE_TAKEN` si legge anche dalla tabella dei file, come le altre colonne di [COLUMNS]; per
     * una riga che non ce l'ha il cursore dà `null`.
     * [peeking] mostra anche qui le cartelle nascoste (richiesta dell'utente, 6.03): 'Mostra
     * nascoste' aggiunge le loro immagini al Feed finché dura, come aggiunge le cartelle alla
     * griglia.
     */
    suspend fun feed(
        context: Context,
        selection: FolderSelection,
        date: FeedDate,
        peeking: Boolean
    ): Feed.Page? =
        withContext(Dispatchers.IO) {
            if (!granted(context)) return@withContext null
            val rows = mutableListOf<Feed.Row>()
            runCatching {
                context.contentResolver.query(TABLE, COLUMNS + TAKEN, MEDIA, null, null)?.use { c ->
                    val idAt = c.column(MediaStore.Images.Media._ID) ?: return@use
                    @Suppress("DEPRECATION")
                    val pathAt = c.column(MediaStore.Images.Media.DATA)
                    val kindAt = c.column(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    val modifiedAt = c.column(MediaStore.Images.Media.DATE_MODIFIED)
                    val takenAt = c.column(TAKEN)
                    while (c.moveToNext()) {
                        val dir = pathAt?.let { c.getString(it) }
                            ?.substringBeforeLast('/')
                            ?.takeIf { it.isNotBlank() }
                        val id = c.getLong(idAt)
                        rows += Feed.Row(
                            uri = uriOf(id, c.isClip(kindAt)),
                            id = id,
                            taken = takenAt?.takeUnless { c.isNull(it) }?.let { c.getLong(it) },
                            modified = modifiedAt?.let { c.getLong(it) } ?: 0L,
                            dir = dir
                        )
                    }
                }
            }
            Feed.order(Feed.shown(rows, selection, peeking), date)
        }

    /** La data dello scatto, che legge solo il Feed: vedi [feed]. */
    private const val TAKEN = MediaStore.Images.Media.DATE_TAKEN

    /**
     * Se le cartelle si possono leggere. Il permesso dipende dalla variante, e lo dice [Store]:
     * in `github` è l'accesso a tutti i file (scelta dell'utente), in `play` sono i permessi su
     * immagini e video, e l'accesso parziale conta.
     */
    fun granted(context: Context): Boolean = Store.granted(context)

    /**
     * La pagina delle impostazioni dove si concede l'accesso a tutti i file, nella sola
     * variante `github`: `MANAGE_EXTERNAL_STORAGE` è un permesso speciale, si concede con un
     * interruttore nelle impostazioni e non con il solito 'Consenti'.
     *
     * ⚠️ Sono due: quella mirata all'app manca su qualche sistema, e senza il ripiego
     * sull'elenco generale la richiesta fallirebbe con un'eccezione.
     */
    fun settingsIntents(context: Context): List<Intent> = listOf(
        Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            "package:${context.packageName}".toUri()
        ),
        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
    )

    // ⚠️ L'ordine è quello degli indici usati sotto (0 id, 1 bucket, 2 nome, 3 peso,
    // 4 percorso, 5 nome della cartella, 6 tipo, 7 data): una colonna nuova va in fondo.
    // `DATA` è deprecata e serve lo stesso: è la sola colonna che dice dov'è il file, ed è la
    // chiave con cui l'indirizzo del selettore si riporta alla riga giusta.
    // Le costanti sono quelle di `Images` anche sulla tabella dei file: sono `String`
    // identiche in `ImageColumns` e in `VideoColumns`, perché la colonna è nella tabella e non
    // nella vista. Quelle di `MediaStore.MediaColumns` esistono solo da Android 10, e `minSdk`
    // è 28.
    @Suppress("DEPRECATION")
    private val COLUMNS = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Files.FileColumns.MEDIA_TYPE,
        // Per la griglia ordinata per data (`Bucket.latest`).
        MediaStore.Images.Media.DATE_MODIFIED
    )

    /**
     * La tabella che si interroga: quella dei file.
     *
     * ⚠️ **Una query sola per immagini e video**: `Images.Media` e `Video.Media` sono due filtri
     * sulla stessa tabella, e interrogarle a parte vorrebbe dire fondere due cursori a mano con
     * un secondo ordinamento in Kotlin da tenere d'accordo con quello di SQLite.
     * Il volume si scrive per esteso: `MediaStore.VOLUME_EXTERNAL` esiste da Android 10, e l'app
     * arriva ad Android 9.
     * Non è privata perché `ViewerViewModel` vi registra l'osservatore dei file arrivati da
     * fuori, e la tabella da ascoltare deve essere quella che si legge.
     */
    val TABLE: Uri = MediaStore.Files.getContentUri("external")

    /**
     * Il filtro che tiene fuori tutto ciò che non è un'immagine o un video.
     *
     * ⚠️ **Serve in ogni query di questo file**: la tabella dei file contiene anche documenti,
     * musica e cartelle (tipo 0), e senza il filtro la griglia mostrerebbe i PDF.
     */
    private val MEDIA = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN " +
        "(${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}, " +
        "${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}) AND ${MediaFiles.selection}"

    /** Quel filtro più una condizione propria, che è il caso normale. */
    private fun media(and: String): String = "$MEDIA AND ($and)"

    /**
     * La cartella dell'immagine in [uri], o il perché non c'è.
     *
     * Una cartella con quell'immagine sola non è una serie, e vale `Alone`.
     */
    suspend fun seriesAround(context: Context, uri: Uri): Lookup = withContext(Dispatchers.IO) {
        if (!granted(context)) return@withContext Lookup.NoPermission
        val card = identify(context, uri) ?: return@withContext Lookup.Unreadable
        val found = locate(context, uri, card)
            // Il ripiego sul disco: se il MediaStore non riconosce l'immagine, la cartella si
            // legge lo stesso con l'accesso a tutti i file, dal percorso che il selettore
            // dichiara. Resta un ripiego perché dà indirizzi `file://`, un secondo genere di
            // indirizzo accanto ai `content://` del MediaStore.
            ?: return@withContext fromDisk(card) ?: Lookup.NotInGallery(card.evidence())
        list(context, found)
    }

    /**
     * Che cosa si è riusciti a sapere di quello che è stato aperto.
     *
     * ⚠️ [size] vale -1 quando chi serve l'indirizzo non lo dichiara, e [path] è null quando
     * non è un indirizzo del selettore: sono assenze, non valori, e non si confrontano.
     */
    private data class Card(val name: String, val size: Long, val path: String?, val synthetic: Boolean) {
        val sizeKnown: Boolean get() = size >= 0

        /**
         * Il percorso solo quando è vero: quello del selettore,
         * `/sdcard/.transforms/synthetic/picker/...`, è una via FUSE inventata, non il posto
         * del file.
         */
        val realPath: String?
            get() = path?.takeUnless { it.contains(SYNTHETIC_MARK) }

        /** Compatta, perché finisce in una riga sola sopra un'immagine. */
        fun evidence(): String = buildString {
            append(name)
            append(if (sizeKnown) ", $size B" else ", peso ignoto")
            if (synthetic) append(", nome sintetico")
            if (path == null) append(", senza percorso")
            else if (realPath == null) append(", percorso sintetico")
        }
    }

    /**
     * ⚠️ **Il selettore dichiara un nome e un percorso che non esistono**. Dal sorgente AOSP
     * (`PickerDbFacade`, commenti sui due `getProjection...`), per un indirizzo del selettore:
     * - `_display_name` vale `<media-id>.<estensione>`;
     * - `_data` vale `/sdcard/.transforms/synthetic/picker/<utente>/<autorità>/media/<quel nome>`.
     *
     * Nessuno dei due combacia con la riga del MediaStore, quindi cercare per nome o per
     * percorso non trova mai niente. I dati veri sono il peso e l'id in coda all'indirizzo, che
     * per gli elementi locali è l'id del MediaStore.
     */
    private const val SYNTHETIC_MARK = "/.transforms/synthetic/"

    /**
     * Dove il MediaStore ha trovato l'immagine: la cartella, la riga e che cosa è.
     *
     * [video] serve a ricostruire l'indirizzo giusto ([uriOf]): la stessa riga ha due
     * indirizzi nelle due tabelle, e cercare quello sbagliato darebbe [Lookup.Lost].
     */
    private data class Located(val bucket: Long, val id: Long, val video: Boolean)

    private fun identify(context: Context, uri: Uri): Card? =
        runCatching {
            val wanted = mutableListOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
            // Il percorso è la chiave buona, e da Android 13 il selettore lo serve:
            // `PickerMediaColumns.DATA` risponde col percorso vero del file scelto (così la usa
            // `PickerUriResolver.getPickerFileFromUri`), e la riga si trova per uguaglianza.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                wanted += MediaStore.PickerMediaColumns.DATA
            }
            context.contentResolver.query(uri, wanted.toTypedArray(), null, null, null)?.use { c ->
                if (!c.moveToFirst()) return@use null
                val name = c.column(OpenableColumns.DISPLAY_NAME)?.let { c.getString(it) }
                    ?: return@use null
                val sizeAt = c.column(OpenableColumns.SIZE)
                val size = if (sizeAt == null || c.isNull(sizeAt)) -1L else c.getLong(sizeAt)
                val pathAt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    c.column(MediaStore.PickerMediaColumns.DATA)
                } else {
                    null
                }
                val path = if (pathAt == null || c.isNull(pathAt)) null else c.getString(pathAt)
                // Il nome è sintetico quando è l'id in coda all'indirizzo con
                // un'estensione appiccicata: vedi SYNTHETIC_MARK per il perché.
                val synthetic = name.substringBeforeLast('.') == uri.lastPathSegment
                Card(name, size, path?.takeIf { it.isNotBlank() }, synthetic)
            }
        }.getOrNull()

    /**
     * ⚠️ `getColumnIndex` risponde -1 su una colonna che il provider non serve, e
     * `getString(-1)` dà errore: chiedere una proiezione non garantisce di riceverla, quindi
     * ogni lettura passa di qui.
     */
    private fun Cursor.column(name: String): Int? = getColumnIndex(name).takeIf { it >= 0 }

    /**
     * Quale riga del MediaStore è l'immagine sullo schermo: la cartella e l'id.
     *
     * ⚠️ **Di un indirizzo del selettore nome e percorso sono inventati** (vedi
     * `SYNTHETIC_MARK`): come chiave non trovano niente, in silenzio. Restano veri il peso e
     * l'id in coda all'indirizzo.
     * ⚠️ **L'id si controlla sul peso, non sul nome**: un indirizzo qualunque finisce con un
     * numero che può essere la riga di qualcun altro, e col nome sintetico del selettore la
     * riga giusta sarebbe trovata e poi scartata.
     */
    private fun locate(context: Context, uri: Uri, card: Card): Located? {
        // 1. L'id in coda all'indirizzo, verificato sul peso. Per un elemento locale
        // del selettore quell'id È l'id del MediaStore, quindi è la via esatta.
        val guessed = runCatching { ContentUris.parseId(uri) }.getOrNull()
        if (guessed != null && guessed > 0) {
            rowById(context, guessed)?.let { row ->
                val sameSize = !card.sizeKnown || row.size == card.size
                // Il nome si confronta solo quando è vero: sintetico, non dice nulla.
                val sameName = card.synthetic || row.name == card.name
                if (sameSize && sameName) return Located(row.bucket, guessed, row.video)
            }
        }

        // 2. Il percorso, ma solo quello VERO: uguaglianza esatta.
        card.realPath?.let { path ->
            rowByWhere(context, "${MediaStore.Images.Media.DATA} = ?", arrayOf(path))?.let { return it }
        }

        // 3. Nome e peso, e solo se il nome è vero: è la coppia più selettiva.
        if (!card.synthetic && card.sizeKnown) {
            rowByWhere(
                context,
                "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.SIZE} = ?",
                arrayOf(card.name, card.size.toString())
            )?.let { return it }
        }

        // 4. Il solo peso, quando il nome non è utilizzabile. Poco selettivo, e ci si arriva
        // solo se le tre vie sopra hanno fallito: due file con lo stesso numero esatto di byte
        // sono rari, e una cartella forse sbagliata è meglio di un gesto che non fa niente.
        if (card.sizeKnown) {
            rowByWhere(
                context,
                "${MediaStore.Images.Media.SIZE} = ?",
                arrayOf(card.size.toString())
            )?.let { return it }
        }

        // 5. Il solo nome, ultima spiaggia e solo se vero.
        if (!card.synthetic) {
            rowByWhere(
                context,
                "${MediaStore.Images.Media.DISPLAY_NAME} = ?",
                arrayOf(card.name)
            )?.let { return it }
        }
        return null
    }

    /**
     * Le estensioni che si considerano immagini leggendo una cartella dal disco.
     *
     * Serve un elenco perché una directory contiene di tutto, e qui il MediaStore, che sa
     * distinguere, non c'è.
     *
     * ⚠️ **Deve coincidere coi filtri del manifesto**, dove vive l'idea che l'app ha di
     * 'immagine': un formato elencato qui e non là si vede in una cartella ma l'app non si offre
     * di aprirlo, e viceversa. Nessun controllo confronta i due elenchi, in due linguaggi
     * diversi: un formato nuovo entra in tutti e due, e in [ImageActions] (`IMAGE_SUFFIXES`).
     * Un formato si dichiara solo se il decodificatore lo legge: per questo TIFF e i RAW non ci
     * sono, mentre `svgz` (androidsvg lo decomprime da sé) e `dng` (lo legge il decodificatore di
     * sistema, richiesta dell'utente) sì. Il DNG non si riscrive: salvarlo crea una copia JPEG
     * accanto (risposta `D1`, vedi `ImageEdit.canOverwrite`).
     */
    private val EXTENSIONS = setOf(
        "jpg", "jpeg", "jpe", "png", "gif", "webp", "avif", "heic", "heif", "bmp",
        "svg", "svgz", "dng"
    )

    /**
     * Immagine o video: quello che una cartella letta dal disco deve mostrare.
     *
     * Le estensioni dei video sono in [Videos], con tutto quello che li riguarda: una copia qui
     * si separerebbe al primo formato aggiunto.
     */
    private fun File.isMedia(): Boolean = isPicture(extension) || extension.lowercase() in
        Videos.EXTENSIONS

    /**
     * Se quell'estensione è di un'immagine.
     *
     * Interna e non privata: la usa anche [Tree], la vista 'Cartelle di sistema', che legge il
     * disco e non ha nessuno che dica il tipo di un file.
     */
    internal fun isPicture(ext: String): Boolean = ext.lowercase() in EXTENSIONS

    /**
     * La cartella letta dal filesystem, quando il MediaStore non riconosce l'immagine.
     *
     * ⚠️ **L'ordine imita quello di [urisOf]** (data decrescente, poi nome al posto dell'id):
     * la stessa cartella deve dare la stessa sequenza da tutte e due le vie, e i due posti si
     * tengono d'accordo a mano.
     */
    private fun fromDisk(card: Card): Lookup? {
        // Solo il percorso vero: quello sintetico del selettore punta a una via FUSE inventata,
        // la cui cartella non contiene le immagini vicine.
        val path = card.realPath ?: return null
        // Il selettore può dichiarare `/sdcard`, che è un collegamento: AOSP fa la stessa
        // sostituzione in `PickerUriResolver.getPickerFileFromUri`.
        val real = path.replaceFirst("/sdcard", "/storage/emulated/0")
        val file = runCatching { File(real) }.getOrNull() ?: return null
        val dir = file.parentFile ?: return null
        val siblings = runCatching { dir.listFiles() }.getOrNull() ?: return null

        val images = siblings
            .filter { it.isFile && it.isMedia() }
            .sortedWith(compareByDescending<File> { it.lastModified() }.thenByDescending { it.name })
        if (images.size < 2) return Lookup.Alone
        val index = images.indexOfFirst { it.absolutePath == file.absolutePath }
            .takeIf { it >= 0 }
            ?: images.indexOfFirst { it.name == file.name }.takeIf { it >= 0 }
            ?: return Lookup.Lost
        return Lookup.Found(Series(images.map { Uri.fromFile(it) }, index))
    }

    /**
     * Quello che di una riga serve a riconoscerla, con un nome per ogni campo.
     */
    private data class Row(val bucket: Long, val size: Long, val name: String, val video: Boolean)

    /** Cartella, peso, nome e tipo della riga con quell'id, per poterla verificare. */
    private fun rowById(context: Context, id: Long): Row? = runCatching {
        context.contentResolver.query(
            TABLE,
            COLUMNS,
            media("${MediaStore.Images.Media._ID} = ?"),
            arrayOf(id.toString()),
            null
        )?.use { c ->
            if (!c.moveToFirst()) return@use null
            Row(c.getLong(1), c.getLong(3), c.getString(2) ?: "", c.isClip(KIND_AT))
        }
    }.getOrNull()

    /** La prima riga che soddisfa la condizione, con la sua cartella e il suo id. */
    private fun rowByWhere(context: Context, where: String, args: Array<String>): Located? =
        runCatching {
            context.contentResolver.query(
                TABLE, COLUMNS, media(where), args, null
            )?.use { c ->
                if (c.moveToFirst()) Located(c.getLong(1), c.getLong(0), c.isClip(KIND_AT)) else null
            }
        }.getOrNull()

    /**
     * Se la riga su cui è fermo il cursore è un video.
     *
     * La colonna può mancare, e allora la risposta è 'immagine'.
     */
    private fun Cursor.isClip(at: Int?): Boolean =
        at != null && !isNull(at) && getInt(at) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO

    /**
     * L'indice della colonna del tipo dentro [COLUMNS], per chi legge per posizione.
     *
     * Le due funzioni qui sopra leggono le colonne per indice: la proiezione è [COLUMNS], quindi
     * la posizione è nota. Una colonna nuova va in fondo, e questo numero resta buono.
     */
    private const val KIND_AT = 6

    /**
     * Tutta la cartella, nell'ordine di base di [urisOf], con dentro l'immagine aperta.
     *
     * Per data e non per nome: in ordine lessicografico `IMG_10` viene prima di `IMG_9`. L'id
     * risolve i pari merito, così due immagini con la stessa data hanno sempre lo stesso ordine.
     * Il verso scelto dall'utente non si decide qui: si ottiene girando la serie
     * (`Series.reversed`, e l'impostazione in `ViewerViewModel.folder`).
     */
    private fun list(context: Context, found: Located): Lookup {
        val ids = urisOf(context, found.bucket)
        if (ids.size < 2) return Lookup.Alone
        // ⚠️ L'indice si trova per id, la chiave della riga, che non può essere sintetica:
        // nome e percorso sono proprio i dati che il selettore inventa.
        val index = ids.indexOf(uriOf(found.id, found.video))
        if (index < 0) return Lookup.Lost
        return Lookup.Found(Series(ids, index))
    }

    /**
     * Gli indirizzi di una cartella nell'ordine di base, dall'immagine più recente alla più
     * vecchia.
     *
     * ⚠️ **Decrescente è il verso predefinito** (decisione dell'utente: è il più naturale su uno
     * smartphone); l'impostazione serve a tornare all'ordine cronologico. Rimesso crescente,
     * rovescerebbe il comportamento di tutta l'app.
     * Indirizzi e non id: la stessa riga ha due indirizzi nelle due tabelle, e l'indirizzo
     * contiene il tipo (vedi [Videos]). Il confronto resta esatto, perché sono costruiti tutti
     * da [uriOf].
     */
    private fun urisOf(context: Context, bucket: Long): List<Uri> {
        val found = mutableListOf<Uri>()
        runCatching {
            context.contentResolver.query(
                TABLE,
                COLUMNS,
                media("${MediaStore.Images.Media.BUCKET_ID} = ?"),
                arrayOf(bucket.toString()),
                "${MediaStore.Images.Media.DATE_MODIFIED} DESC, ${MediaStore.Images.Media._ID} DESC"
            )?.use { c -> while (c.moveToNext()) found.add(uriOf(c.getLong(0), c.isClip(KIND_AT))) }
        }
        return found
    }

    /**
     * L'indirizzo di una riga, nella tabella che le compete.
     *
     * ⚠️ **Non si costruisce sulla tabella dei file, anche se è quella che si interroga**:
     * `content://media/external/file/12` non dice se è un'immagine o un video,
     * `.../video/media/12` sì. Col tipo nell'indirizzo `Videos.isVideo` è una funzione pura,
     * senza query, chiamabile per ogni riquadro che scorre.
     */
    private fun uriOf(id: Long, video: Boolean): Uri = ContentUris.withAppendedId(
        if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        id
    )
}

/**
 * La radice dell'archivio primario in testa a un percorso, cioè la parte che cambia da un
 * telefono all'altro.
 *
 * Le forme sono quelle con cui Android nomina l'archivio del telefono: col numero dell'utente
 * (`/storage/emulated/0`, e `10` per un profilo di lavoro), coi due nomi storici (`/sdcard`,
 * `/mnt/sdcard`) e con `/storage/self/primary`. `emulated` e `self` da soli non sono un volume:
 * senza il numero, `/storage/emulated/legacy` si leggerebbe come l'archivio primario.
 * Deve finire su una barra o sul percorso, o `/sdcardx` si leggerebbe come `/sdcard`.
 */
private val PRIMARY_ROOT = Regex(
    "^(?:/storage/emulated/\\d+|/storage/self/primary|/sdcard|/mnt/sdcard)(?=/|$)"
)

/**
 * La radice di un volume rimovibile, una scheda o una chiavetta, scritta intera:
 * `/storage/1234-5678`.
 *
 * Esclude i due nomi che [PRIMARY_ROOT] tratta, `emulated` e `self`, che non sono un volume a sé.
 */
private val REMOVABLE_ROOT = Regex("^/storage/(?!emulated(?:/|$)|self(?:/|$))[^/]+$")

/**
 * Il percorso di una cartella come lo scrive l'elenco delle nascoste.
 *
 * ⚠️ **Sull'archivio del telefono senza la radice, su una scheda intero** (nota dell'utente sulla
 * voce `nascoste-percorsi`: nascondere solo quella cartella, ma riconoscibile su qualsiasi
 * dispositivo).
 * - L'archivio del telefono perde la radice, la parte che da un telefono all'altro cambia:
 *   `DCIM/Temp` vuol dire quella cartella sul telefono, su qualunque telefono.
 * - Una scheda tiene il percorso intero: il suo identificativo è quello della scheda, e Android
 *   la monta sempre sotto `/storage/<identificativo>`. Quindi `/storage/1234-5678/DCIM` vuol dire
 *   quella cartella su quella scheda, anche in un altro telefono.
 * La radice dell'archivio diventa la stringa vuota, e una radice nasconde solo se stessa (vedi
 * [covers]): le immagini che sono proprio là, non le cartelle dentro.
 * Un percorso fuori da un volume resta intero, come una scheda.
 */
internal fun portablePath(path: String): String {
    val root = PRIMARY_ROOT.find(path) ?: return path.trimEnd('/').ifEmpty { path }
    return path.substring(root.range.last + 1).trim('/')
}

/**
 * Se la voce [entry] copre la cartella [own], scritte tutte e due nella forma di [portablePath].
 *
 * ⚠️ **Il confronto è sul separatore**: con un `startsWith` nudo, escludere `.../Foo`
 * toglierebbe anche `.../Foo2` e `.../Foobar`. Con la barra dopo si nasconde `Foo` e tutto
 * quello che c'è dentro.
 * ⚠️ **La radice di un volume copre solo se stessa**, quella del telefono (la voce vuota) come
 * quella di una scheda: la voce vuota con la barra dopo coprirebbe ogni percorso fuori
 * dall'archivio, e la radice di una scheda la scheda intera.
 */
private fun covers(entry: String, own: String): Boolean =
    own == entry || (entry.isNotEmpty() && !REMOVABLE_ROOT.matches(entry) && own.startsWith("$entry/"))

/**
 * Le voci dell'elenco delle nascoste che coprono la cartella in [path], un percorso intero.
 *
 * Tutte e non una, perché servono a rimostrare una cartella (vedi il dialogo in `FolderScreen`):
 * una cartella dentro una nascosta è coperta da lei, e togliere la sola voce col suo nome non la
 * farebbe tornare.
 */
internal fun coveringOf(hidden: Set<String>, path: String): List<String> {
    if (hidden.isEmpty()) return emptyList()
    val own = portablePath(path)
    return hidden.filter { covers(it, own) }
}

/** Se la cartella in [path], un percorso intero, è fra quelle che l'utente ha escluso. */
internal fun hiddenIn(hidden: Set<String>, path: String): Boolean {
    if (hidden.isEmpty()) return false
    val own = portablePath(path)
    return hidden.any { covers(it, own) }
}

/**
 * Se la cartella in [path] è proprio una voce dell'elenco, e non soltanto coperta da una.
 *
 * Serve al segno della vista 'Cartelle di sistema', che segna le cartelle nascoste e non quelle
 * che contengono: dentro una cartella nascosta ogni riga avrebbe il segno, che non direbbe più
 * niente.
 */
internal fun listedIn(hidden: Set<String>, path: String): Boolean = portablePath(path) in hidden

/**
 * Il nome di una voce dell'elenco delle nascoste, per il pannello e per la pagina delle impostazioni.
 *
 * La radice di un volume non ha un nome, e si scrive con la barra.
 */
internal fun hiddenName(entry: String): String = entry.substringAfterLast('/').ifEmpty { "/" }

/**
 * Il percorso di una voce come lo si legge: con la barra davanti, come lo scrive l'utente
 * (`/DCIM/Temp`). La barra dice che il percorso parte dalla radice: senza, `DCIM/Temp` si
 * leggerebbe come un nome qualunque.
 */
internal fun hiddenShown(entry: String): String = if (entry.startsWith('/')) entry else "/$entry"

/**
 * Se questa cartella è fra quelle che l'utente ha escluso.
 *
 * Una cartella senza percorso non si nasconde mai: il provider può non servire la colonna, e
 * meglio mostrarne una di troppo che nasconderne una a caso.
 * È qui perché la usano due chiamanti: una cartella esclusa non compare nemmeno fra le
 * destinazioni di una copia (vedi `destinations`), e una seconda copia del confronto potrebbe
 * divergere sul separatore o sulla radice del volume.
 */
internal fun Folder.Bucket.isHidden(hidden: Set<String>): Boolean {
    val own = path ?: return false
    return hiddenIn(hidden, own)
}

/** Un punto in cui andare a capo prima di ogni maiuscola che segue una minuscola. */
internal fun camelBreak(name: String): String =
    buildString(name.length + 4) {
        for (i in name.indices) {
            val c = name[i]
            if (i > 0 && name[i - 1].isLowerCase() && c.isUpperCase()) append('\u200B')
            append(c)
        }
    }


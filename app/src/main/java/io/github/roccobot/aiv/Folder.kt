package io.github.roccobot.aiv

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The other pictures in the same folder, so that one opened photo can be leafed
 * through instead of being a dead end.
 *
 * ⚠️⚠️ **The picker cannot answer this question, and that is the whole reason this
 * file exists.** What comes back from the system picker is a permit for ONE item:
 * on Android 13 and up a `content://media/picker/...` address that grants access
 * to that picture and to nothing around it. There is no parent to walk up to. The
 * folder lives in the MediaStore, and reading the MediaStore needs a permission,
 * which is what the user agreed to pay.
 */
object Folder {

    /** One folder, in order, and where the picture on screen sits inside it. */
    data class Series(val items: List<Uri>, val index: Int) {
        val size: Int get() = items.size
        fun at(position: Int): Uri? = items.getOrNull(position)
    }

    /**
     * L'esito della ricerca, col PERCHÉ quando non c'è una serie.
     *
     * ⚠️⚠️ **Nato dopo due giri a vuoto, e il difetto era di processo prima che di
     * codice**: finché questa funzione rispondeva `null` e basta, una strisciata che
     * non faceva niente era indistinguibile da una strisciata guasta, e da fuori non
     * c'era modo di sapere quale delle due fosse. Due versioni sono andate perse a
     * indovinare. Un esito che dice la ragione costa quattro stringhe e chiude la
     * domanda in un'occhiata.
     * ⚠️ Quindi chi un domani volesse 'semplificare' rimettendo un `Series?` sappia
     * che toglierebbe l'unica cosa che rende diagnosticabile la funzione.
     */
    sealed interface Lookup {
        /** La cartella c'è, e questa è. */
        data class Found(val series: Series) : Lookup

        /** Manca il permesso: la sola causa che la persona può rimuovere. */
        data object NoPermission : Lookup

        /**
         * L'indirizzo non sa dire nome e peso di quello che ha aperto.
         * ⚠️ Distinto da [NotInGallery] apposta: qui la domanda non ha nemmeno
         * raggiunto il MediaStore, e i due casi vogliono correzioni diverse.
         */
        data object Unreadable : Lookup

        /**
         * Il MediaStore non conosce questa immagine: una chat, il web, un file
         * sciolto. [detail] porta quello che si era riusciti a leggere, e non è un
         * vezzo: è la differenza fra 'non funziona' e sapere quale chiave ha
         * fallito. La riga dei dettagli lo stampa.
         */
        data class NotInGallery(val detail: String) : Lookup

        /** La cartella esiste e ha questa foto sola: non c'è niente da sfogliare. */
        data object Alone : Lookup

        /**
         * La cartella ha più foto ma l'aperta non è fra loro.
         * ⚠️ Questo NON dovrebbe succedere, ed è scritto apposta: se compare, il
         * difetto è nel modo in cui si risale alla riga, non nei dati.
         */
        data object Lost : Lookup

        /** La serie quando c'è, e null per tutti gli esiti che spiegano perché non c'è. */
        val seriesOrNull: Series? get() = (this as? Found)?.series
    }

    /**
     * ⚠️⚠️ **Il permesso è quello PESANTE, l'accesso a tutti i file, ed è una scelta
     * dell'utente**: *preferisco chiedere un permesso pesante prima e poi essere a
     * posto per sempre*. Quello leggero (`READ_MEDIA_IMAGES`) sarebbe bastato a
     * leggere il MediaStore, e la differenza che si paga volentieri è questa: da
     * Android 14 quello leggero apre la porta all'accesso **parziale**, dove la
     * persona spunta tre foto e il MediaStore ne mostra tre, e una cartella da
     * quattrocento risponde 'tre' senza che niente segnali l'inganno. L'accesso a
     * tutti i file quella scelta non ce l'ha.
     *
     * ⚠️ **Non è un dialogo ma una PAGINA DI SISTEMA**: `MANAGE_EXTERNAL_STORAGE` è
     * un permesso speciale, si concede con un interruttore nelle impostazioni e non
     * con il solito 'Consenti'. Da qui esce quindi un intent, non una richiesta.
     */
    fun granted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            // Fino ad Android 10 il permesso ampio non esiste, e quello classico
            // sull'archivio fa già vedere tutto: là si chiede quello, col dialogo.
            ContextCompat.checkSelfPermission(context, legacyPermission) ==
                PackageManager.PERMISSION_GRANTED
        }

    /** Quello da chiedere col dialogo, e serve solo sotto Android 11. */
    const val legacyPermission: String = Manifest.permission.READ_EXTERNAL_STORAGE

    /**
     * La pagina delle impostazioni dove si concede l'accesso a tutti i file.
     *
     * ⚠️ Ne esistono DUE, e la seconda non è un lusso: quella mirata all'app manca
     * su qualche sistema, e senza il ripiego sull'elenco generale la richiesta
     * morirebbe con un'eccezione invece di portare da qualche parte.
     */
    fun settingsIntents(context: Context): List<Intent> = listOf(
        Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            "package:${context.packageName}".toUri()
        ),
        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
    )

    // ⚠️ L'ordine è quello degli indici usati sotto (0 id, 1 bucket, 2 nome, 3 peso,
    // 4 percorso): chi ne aggiunge una la mette IN FONDO.
    // ⚠️ `DATA` è deprecata nell'API e serve lo stesso: è la sola colonna che dice
    // dove sta il file, ed è la chiave con cui l'indirizzo del selettore si riporta
    // alla riga giusta senza indovinare.
    @Suppress("DEPRECATION")
    private val COLUMNS = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATA
    )

    /**
     * The folder the picture at [uri] belongs to, or the REASON there is none.
     *
     * ⚠️ Una cartella con quella foto sola non è una serie, e vale `Alone`: offrire
     * di sfogliarla sarebbe un gesto che non porta da nessuna parte.
     */
    suspend fun seriesAround(context: Context, uri: Uri): Lookup = withContext(Dispatchers.IO) {
        if (!granted(context)) return@withContext Lookup.NoPermission
        val card = identify(context, uri) ?: return@withContext Lookup.Unreadable
        val found = locate(context, uri, card)
            // ⚠️⚠️ IL RIPIEGO SUL DISCO, ED È QUELLO PER CUI IL PERMESSO PESANTE È
            // STATO PRESO. Se il MediaStore non riconosce la foto, la cartella si
            // legge lo stesso: `MANAGE_EXTERNAL_STORAGE` dà i file, non un indice, e
            // una directory non può 'non essere indicizzata'. Serve solo il
            // percorso, che il selettore dichiara.
            // ⚠️ Resta un RIPIEGO e non la via principale: dal MediaStore vengono
            // indirizzi `content://` come quello con cui la foto è stata aperta,
            // mentre di qui vengono `file://`, che il caricatore regge ma che sono
            // un secondo genere di indirizzo in circolo.
            ?: return@withContext fromDisk(card) ?: Lookup.NotInGallery(card.evidence())
        list(context, found)
    }

    /**
     * Che cosa si è riusciti a sapere di quello che è stato aperto.
     *
     * ⚠️ [size] vale -1 quando chi serve l'indirizzo non lo dichiara, e [path] è null
     * quando non è un indirizzo del selettore: **sono assenze, non valori**, e il
     * codice che cerca deve trattarle come tali invece di confrontarle.
     */
    private data class Card(val name: String, val size: Long, val path: String?, val synthetic: Boolean) {
        val sizeKnown: Boolean get() = size >= 0

        /**
         * Il percorso solo quando è VERO.
         *
         * ⚠️⚠️ Quello del selettore non lo è: `/sdcard/.transforms/synthetic/picker/...`
         * è una via FUSE inventata per l'occasione, non il posto dove il file sta.
         */
        val realPath: String?
            get() = path?.takeUnless { it.contains(SYNTHETIC_MARK) }

        /** Compatta, perché finisce in una riga sola sopra una fotografia. */
        fun evidence(): String = buildString {
            append(name)
            append(if (sizeKnown) ", $size B" else ", peso ignoto")
            if (synthetic) append(", nome sintetico")
            if (path == null) append(", senza percorso")
            else if (realPath == null) append(", percorso sintetico")
        }
    }

    /**
     * ⚠️⚠️ **IL SELETTORE DICE UN NOME E UN PERCORSO CHE NON ESISTONO**, e questa
     * costante è metà del rimedio. Dal sorgente AOSP (`PickerDbFacade`, commenti sui
     * due `getProjection...`), per un indirizzo del selettore:
     * - `_display_name` vale **`<media-id>.<estensione>`**;
     * - `_data` vale **`/sdcard/.transforms/synthetic/picker/<utente>/<autorità>/media/<quel nome>`**.
     *
     * Nessuno dei due combacia con quello che il MediaStore ha in riga, quindi
     * cercare per nome o per percorso non trova **mai** niente: è il difetto per cui
     * la funzione ha risposto 'non è nella galleria' dalla 0.19 alla 0.25.
     * L'unico dato vero che il selettore serve è il **peso**, e l'id in coda
     * all'indirizzo, che per gli elementi locali **è** l'id del MediaStore.
     */
    private const val SYNTHETIC_MARK = "/.transforms/synthetic/"

    /** Dove il MediaStore ha davvero trovato la foto: la cartella e la riga. */
    private data class Located(val bucket: Long, val id: Long)

    private fun identify(context: Context, uri: Uri): Card? =
        runCatching {
            val wanted = mutableListOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
            // ⚠️⚠️ IL PERCORSO È LA CHIAVE BUONA, e da Android 13 il selettore lo
            // serve: `PickerMediaColumns.DATA` è pubblica e risponde col percorso
            // vero del file scelto (`PickerUriResolver.getPickerFileFromUri` la usa
            // esattamente così). Con quello la riga del MediaStore si trova per
            // uguaglianza, senza indovinare per nome e peso.
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
     * `getString(-1)` **solleva**: chiedere una proiezione non garantisce di
     * riceverla, quindi ogni lettura passa di qui.
     */
    private fun Cursor.column(name: String): Int? = getColumnIndex(name).takeIf { it >= 0 }

    /**
     * Quale riga del MediaStore è la foto sullo schermo: la cartella e l'id.
     *
     * ⚠️⚠️ **CHE COSA SI PUÒ CONFRONTARE, che è la lezione di sei versioni.** Di un
     * indirizzo del selettore il nome e il percorso sono **inventati** (vedi
     * `SYNTHETIC_MARK`): usarli come chiave non trova mai niente, e per giunta in
     * silenzio. Restano veri il **peso** e l'**id in coda all'indirizzo**, e sono
     * esattamente i due su cui questa funzione poggia adesso.
     *
     * ⚠️ **L'id resta CONTROLLATO e non creduto**, perché un indirizzo qualunque
     * finisce con un numero e quel numero può essere la riga di qualcun altro. Ma il
     * controllo è sul **peso**, non sul nome: prima era sul nome, e siccome il nome
     * del selettore è sintetico **la riga giusta veniva trovata e poi buttata via**.
     * È questo il difetto che ha resistito più a lungo.
     */
    private fun locate(context: Context, uri: Uri, card: Card): Located? {
        // 1. L'id in coda all'indirizzo, verificato sul peso. Per un elemento locale
        // del selettore quell'id È l'id del MediaStore, quindi è la via esatta.
        val guessed = runCatching { ContentUris.parseId(uri) }.getOrNull()
        if (guessed != null && guessed > 0) {
            rowById(context, guessed)?.let { row ->
                val sameSize = !card.sizeKnown || row.second == card.size
                // Il nome si confronta solo quando è vero: sintetico, non dice nulla.
                val sameName = card.synthetic || row.third == card.name
                if (sameSize && sameName) return Located(row.first, guessed)
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

        // 4. Il solo peso, quando il nome non è utilizzabile. ⚠️ Poco selettivo, e ci
        // si arriva solo se le tre vie sopra hanno fallito: due file con lo stesso
        // numero esatto di byte esistono, ma sono rari, e una cartella forse
        // sbagliata è comunque meglio di un gesto che non fa niente.
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
     * ⚠️ Un elenco serve perché una directory contiene di tutto, e il MediaStore,
     * che qui non c'è, era proprio quello che sapeva distinguere. Sono le stesse
     * dei filtri del manifest, che è dove vive l'idea che l'app ha di 'immagine'.
     */
    private val EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "avif", "heic", "heif", "tif", "tiff", "bmp"
    )

    /**
     * La cartella letta dal FILESYSTEM, quando il MediaStore non riconosce la foto.
     *
     * ⚠️ L'ordine imita quello del MediaStore (data e poi nome) invece di inventarne
     * uno suo: sfogliare la stessa cartella deve dare la stessa sequenza, da
     * qualunque delle due vie sia arrivata.
     */
    private fun fromDisk(card: Card): Lookup? {
        // ⚠️ Solo il percorso VERO: quello sintetico del selettore punta a una via
        // FUSE inventata, la cui cartella non contiene le foto vicine.
        val path = card.realPath ?: return null
        // ⚠️ Il selettore può dichiarare `/sdcard`, che è un collegamento: AOSP fa
        // la stessa sostituzione in `PickerUriResolver.getPickerFileFromUri`.
        val real = path.replaceFirst("/sdcard", "/storage/emulated/0")
        val file = runCatching { File(real) }.getOrNull() ?: return null
        val dir = file.parentFile ?: return null
        val siblings = runCatching { dir.listFiles() }.getOrNull() ?: return null

        val images = siblings
            .filter { it.isFile && it.extension.lowercase() in EXTENSIONS }
            .sortedWith(compareBy({ it.lastModified() }, { it.name }))
        if (images.size < 2) return Lookup.Alone
        val index = images.indexOfFirst { it.absolutePath == file.absolutePath }
            .takeIf { it >= 0 }
            ?: images.indexOfFirst { it.name == file.name }.takeIf { it >= 0 }
            ?: return Lookup.Lost
        return Lookup.Found(Series(images.map { Uri.fromFile(it) }, index))
    }

    /** Cartella, peso e nome della riga con quell'id, per poterla verificare. */
    private fun rowById(context: Context, id: Long): Triple<Long, Long, String>? = runCatching {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            COLUMNS,
            "${MediaStore.Images.Media._ID} = ?",
            arrayOf(id.toString()),
            null
        )?.use { c ->
            if (!c.moveToFirst()) return@use null
            Triple(c.getLong(1), c.getLong(3), c.getString(2) ?: "")
        }
    }.getOrNull()

    /** La prima riga che soddisfa la condizione, con la sua cartella e il suo id. */
    private fun rowByWhere(context: Context, where: String, args: Array<String>): Located? =
        runCatching {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, COLUMNS, where, args, null
            )?.use { c -> if (c.moveToFirst()) Located(c.getLong(1), c.getLong(0)) else null }
        }.getOrNull()

    /**
     * Everything in that bucket, in a fixed order, with the opened picture found
     * inside it.
     *
     * ⚠️ **Ordered by date and not by name**, and the reason is that names sort
     * badly: `IMG_10` comes before `IMG_9` in any lexicographic order, so a folder
     * of numbered photos would leaf through in a jumbled sequence. The id breaks
     * ties, so two photos with the same timestamp still have one order rather than
     * whichever the database feels like today.
     */
    private fun list(context: Context, found: Located): Lookup {
        val items = mutableListOf<Uri>()
        var index = -1
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            COLUMNS,
            "${MediaStore.Images.Media.BUCKET_ID} = ?",
            arrayOf(found.bucket.toString()),
            "${MediaStore.Images.Media.DATE_MODIFIED} ASC, ${MediaStore.Images.Media._ID} ASC"
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                // ⚠️⚠️ L'INDICE SI TROVA PER ID, e non più confrontando nome o peso:
                // l'id è la chiave della riga, quindi non può sbagliare e non può
                // essere sintetico. Prima si riconfrontavano nome e percorso, cioè
                // proprio i due dati che il selettore INVENTA, e la foto appena
                // trovata rischiava di finire 'non ritrovata nella sua cartella'.
                if (id == found.id) index = items.size
                items.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id))
            }
        }
        if (items.size < 2) return Lookup.Alone
        if (index < 0) return Lookup.Lost
        return Lookup.Found(Series(items, index))
    }
}

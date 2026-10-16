package io.github.roccobot.aiv

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.LruCache
import android.webkit.MimeTypeMap
import androidx.annotation.StringRes
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Che cosa si sa delle immagini scelte: il conto e il peso sempre, tutto il resto solo
 * quando è **una sola**.
 *
 * Il peso si somma, il resto no: nome e misure di dieci immagini non si riassumono senza
 * inventare. Con una sola la risposta è esatta, ed è [one].
 */
class Facts(val count: Int, val bytes: Long, val one: OneFile? = null)

/**
 * I dati di un file, campo per campo.
 *
 * ⚠️ **`null` vuol dire 'non c'è', e la riga non si scrive** (richiesta dell'utente): niente
 * stringhe di ripiego come 'n.d.'. I campi sono separati e i numeri restano numeri
 * (millisecondi, gradi decimali, secondi, millimetri): il formato lo decide l'interfaccia, che
 * conosce la lingua e le impostazioni.
 */
class OneFile(
    val name: String?,
    /** Il tipo MIME per intero (`image/jpeg`): l'interfaccia ne mostra la parte utile. */
    val mime: String?,
    val width: Int?,
    val height: Int?,
    /** Quando l'immagine è stata scattata, dai dati EXIF o dal MediaStore. Millisecondi epoch. */
    val taken: Long?,
    /** Quando il file è stato modificato l'ultima volta. */
    val modified: Long?,
    /** Il percorso della cartella che lo contiene, senza il nome del file. */
    val folder: String?,
    val encoding: Encoding?,
    /**
     * Come tiene i suoi colori. Vedi [coloursOf].
     *
     * È un dato diverso da [encoding], anche se si legge negli stessi byte: quello dice come
     * l'immagine è compressa, questo che cosa contiene un pixel (su un PNG a tavolozza, 'PNG, 8
     * bit' e 'colore indicizzato, 256 colori').
     */
    val colours: Colours?,
    /** Fotogrammi e durata, se è animata. `null` su un'immagine ferma. Vedi [motionOf]. */
    val motion: Motion?,
    val focalMm: Double?,
    val exposureSec: Double?,
    val iso: Int?,
    val latitude: Double?,
    val longitude: Double?,
    val altitudeM: Double?
)

/**
 * I campi delle informazioni, nell'ordine in cui l'utente li ha chiesti.
 *
 * ⚠️ **L'ordine di questo `enum` è il valore di fabbrica**: riordinare le costanti cambia la
 * scheda di ogni telefono che non ha mai toccato l'impostazione. Metodo colore, fotogrammi e
 * durata sono dopo la codifica perché parlano di come è fatto il file; chi ha già riordinato la
 * lista li riceve in coda da [factOrderOf].
 * [always] sono le quattro righe che ogni file ha (nome, tipo, pixel e peso): l'utente le vuole
 * sempre visibili, quindi non hanno l'interruttore, ma restano spostabili. Il nome è anche fisso
 * in testa, e lo garantisce [factOrderOf].
 * Il gettone non è il nome della costante: vale la regola di [Choice].
 * L'etichetta della riga vive qui, e non in un `when` dell'interfaccia, perché serve in due posti
 * (la riga delle impostazioni e l'etichetta nella scheda).
 */
enum class FactField(
    override val token: String,
    @StringRes val label: Int,
    val always: Boolean = false
) : Choice {
    NAME("name", R.string.facts_name, always = true),
    /* Bloccato come nome, pixel e peso, su richiesta dell'utente (voce `riordino-scorre`). */
    KIND("kind", R.string.facts_kind, always = true),
    PIXELS("pixels", R.string.facts_pixels, always = true),
    SIZE("size", R.string.facts_size, always = true),
    TAKEN("taken", R.string.facts_taken),
    MODIFIED("modified", R.string.facts_modified),
    FOLDER("folder", R.string.facts_folder),
    ENCODING("encoding", R.string.facts_encoding),
    COLOURS("colours", R.string.facts_colours),
    FRAMES("frames", R.string.facts_frames),
    DURATION("duration", R.string.facts_duration),
    CAMERA("camera", R.string.facts_camera),
    PLACE("place", R.string.facts_place)
}

/**
 * L'ordine salvato, ricondotto a qualcosa di sensato.
 *
 * ⚠️ **I campi che mancano si aggiungono in coda**: così un campo nuovo compare anche a chi ha già
 * riordinato la lista, invece di nascere invisibile.
 * I gettoni sconosciuti si scartano: un archivio scritto da una versione più nuova, o a mano, non
 * deve far andare in errore l'app. Il nome torna sempre in testa.
 */
fun factOrderOf(tokens: List<String>): List<FactField> {
    val known = tokens.mapNotNull { t -> FactField.entries.firstOrNull { it.token == t } }
    val ordered = known.distinct() + FactField.entries.filterNot { it in known }
    return listOf(FactField.NAME) + ordered.filterNot { it == FactField.NAME }
}

/**
 * Quanto pesa una selezione, e nient'altro: il peso in testa alla selezione la chiama al posto di
 * [factsOf], che con un file solo legge anche misure ed EXIF. [factsOf] la chiama a sua volta,
 * così la somma è scritta in un posto solo.
 */
suspend fun weightOf(context: Context, uris: List<Uri>): Long = withContext(Dispatchers.IO) {
    var bytes = 0L
    for (uri in uris) bytes += sizeOf(context, uri)
    bytes
}

/**
 * I dati di una selezione, letti dal disco.
 *
 * Le misure si leggono dalle intestazioni (`inJustDecodeBounds`), senza decodificare i pixel.
 * Il peso viene dal filesystem per un `file://` e dalla colonna del provider per il resto.
 *
 * ⚠️ **Si passa dal file vero quando c'è**: da Android 10 il MediaStore toglie la posizione dai
 * dati EXIF che serve con un `content://`, salvo un permesso in più. AIV GitHub apre il file;
 * AIV Play chiede quel permesso (scelta R1 dell'utente, rilievo C21) e legge l'EXIF
 * dall'originale ([withPlace]). Per un'immagine senza percorso né permesso la riga della
 * posizione non compare.
 */
suspend fun factsOf(context: Context, uris: List<Uri>): Facts = withContext(Dispatchers.IO) {
    val bytes = weightOf(context, uris)
    if (uris.size != 1) return@withContext Facts(uris.size, bytes)

    val uri = uris.first()
    val file = FileTree.fileOf(context, uri)

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching {
        streamOf(context, uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
    }
    val exif = runCatching {
        if (file != null) ExifInterface(file)
        else streamOf(context, withPlace(context, uri))?.use { ExifInterface(it) }
    }.recoverCatching {
        // L'originale è stato rifiutato (un permesso tolto nel frattempo): l'EXIF senza la
        // posizione vale comunque per le altre righe.
        streamOf(context, uri)?.use { ExifInterface(it) }
    }.getOrNull()

    // ⚠️ Le misure si girano come dice l'EXIF: il file conserva l'immagine come l'ha letta il
    // sensore, e contano le misure di come la si vede.
    val turned = exif?.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    ) in setOf(
        ExifInterface.ORIENTATION_ROTATE_90,
        ExifInterface.ORIENTATION_ROTATE_270,
        ExifInterface.ORIENTATION_TRANSPOSE,
        ExifInterface.ORIENTATION_TRANSVERSE
    )
    /*
     * ⚠️ **Su un AVIF e su un SVG `BitmapFactory` non dà le misure**: sul primo passa dal
     * decodificatore di sistema che si rifiuta (vedi [Avif]), il secondo non è un formato a
     * pixel. Le misure le dà l'intestazione ([Avif.dimensions] gira già i lati) o il documento
     * SVG. Si legge solo quando l'altra strada ha fallito.
     */
    val fallback = if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        runCatching {
            streamOf(context, uri)?.use { stream ->
                val head = ByteArray(Avif.HEAD)
                val got = stream.readFully(head, Avif.HEAD)
                val head2 = head.copyOf(got)
                when {
                    Avif.looksLike(head2) -> Avif.dimensions(head2)
                    /*
                     * Qui si riapre il file: un SVG va letto intero per sapere quanto è grande,
                     * e questo flusso ha già consumato i primi 128 kB. Non si disegna niente.
                     */
                    Svg.looksLike(head2) -> streamOf(context, uri)
                        ?.use { Svg.dimensions(it) }
                    else -> null
                }
            }
        }.getOrNull()
    } else {
        null
    }
    val width = bounds.outWidth.takeIf { it > 0 } ?: fallback?.width
    val height = bounds.outHeight.takeIf { it > 0 } ?: fallback?.height

    val place = exif?.latLong
    Facts(
        count = 1,
        bytes = bytes,
        one = OneFile(
            name = nameOf(context, uri),
            mime = mimeOf(context, uri),
            width = if (turned) height else width,
            height = if (turned) width else height,
            taken = exifMoment(exif) ?: takenFromStore(context, uri),
            modified = file?.lastModified()?.takeIf { it > 0L } ?: modifiedFromStore(context, uri),
            folder = file?.parent,
            encoding = runCatching {
                streamOf(context, uri)?.use { encodingOf(it) }
            }.getOrNull(),
            /*
             * ⚠️ **Tre aperture, una per lettore**: ognuno legge il file in avanti fino a un
             * punto diverso (la codifica al primo SOF, il metodo colore ai chunk, i fotogrammi
             * fino in fondo), e un `InputStream` di un `content://` non si riavvolge.
             * La trasparenza di una GIF è una quarta lettura, solo per le GIF: è nei blocchi
             * dei fotogrammi, non nell'intestazione.
             */
            colours = runCatching {
                val read = streamOf(context, uri)?.use { coloursOf(it) }
                if (read != null && read.model == Colours.Model.INDEXED && !read.transparent &&
                    streamOf(context, uri)
                        ?.use { gifTransparencyOf(it) } == true
                ) {
                    Colours(read.model, read.bitsPerChannel, read.palette, true)
                } else {
                    read
                }
            }.getOrNull(),
            motion = runCatching {
                streamOf(context, uri)?.use { motionOf(it) }
            }.getOrNull(),
            focalMm = exif?.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                ?.takeIf { it > 0.0 },
            exposureSec = exif?.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
                ?.takeIf { it > 0.0 },
            iso = exif?.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0)
                ?.takeIf { it > 0 },
            latitude = place?.getOrNull(0),
            longitude = place?.getOrNull(1),
            // ⚠️ Zero è un'altitudine legittima (il livello del mare), quindi il ripiego è un
            // numero che nessun luogo ha.
            altitudeM = exif?.getAltitude(NO_ALTITUDE)?.takeIf { it != NO_ALTITUDE }
        )
    )
}

/** Un'altitudine che nessun posto ha, per distinguere 'zero metri' da 'non lo so'. */
private const val NO_ALTITUDE = -1e9

/**
 * La data di scatto dai dati EXIF, letta dal tag e convertita a mano.
 *
 * ⚠️ **Non si usa `ExifInterface.dateTimeOriginal`**: è dichiarata a uso interno della libreria
 * (`RestrictTo`), e lint la segnala come errore. Il tag è pubblico ed è lo stesso dato.
 * ⚠️ **Si legge con `Locale.US`**: `SimpleDateFormat` legge l'anno secondo il calendario della
 * lingua del telefono, e col tailandese (che conta dal 543 a.C.) il 2026 diventerebbe un altro
 * anno.
 * L'EXIF scrive l'ora dell'orologio senza fuso: la si interpreta nel fuso del telefono, che è
 * anche quello in cui si mostra, quindi la riga riporta l'ora scritta dalla fotocamera.
 * `DATETIME` fa da ripiego a `DATETIME_ORIGINAL`, perché qualche apparecchio scrive solo il primo.
 */
private fun exifMoment(exif: ExifInterface?): Long? {
    val text = exif?.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        ?: exif?.getAttribute(ExifInterface.TAG_DATETIME)
        ?: return null
    return runCatching {
        SimpleDateFormat(EXIF_PATTERN, Locale.US).parse(text)?.time?.takeIf { it > 0L }
    }.getOrNull()
}

/** Come l'EXIF scrive un istante, dalla sua specifica. */
private const val EXIF_PATTERN = "yyyy:MM:dd HH:mm:ss"

/**
 * Il flusso dei byte del file in [uri], da qualunque parte arrivi.
 *
 * ⚠️ **Ogni lettura di questo file passa da qui**: il `ContentResolver` su un `https` va in
 * errore, e ogni `runCatching` intorno lo nasconderebbe, lasciando la scheda vuota.
 * I byte remoti non si riscaricano: li ha messi in cache `ImageSource.loadRemote` aprendo
 * l'immagine. Se non ci sono ancora si restituisce `null`, e la scheda tratta il dato come
 * assente. Ogni lettore apre e chiude il suo flusso (vedi le tre aperture in [factsOf]).
 */
private fun streamOf(context: Context, uri: Uri): java.io.InputStream? =
    when (uri.scheme?.lowercase()) {
        "http", "https" -> RemoteCache.read(context, uri)?.inputStream()
        else -> context.contentResolver.openInputStream(uri)
    }

/**
 * L'indirizzo da cui l'EXIF di un'immagine del MediaStore arriva con le coordinate.
 *
 * ⚠️ `MediaStore.setRequireOriginal` chiede i byte con la posizione, e si può chiedere solo col
 * permesso della posizione ([Store.seesLocation]): senza, aprire quell'indirizzo va in errore.
 * Vale da Android 10, dove il sistema comincia a togliere la posizione.
 */
internal fun withPlace(context: Context, uri: Uri): Uri =
    if (uri.authority == MediaStore.AUTHORITY && Store.seesLocation(context) &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    ) MediaStore.setRequireOriginal(uri) else uri

/**
 * Quanti byte pesa il file dietro un indirizzo, o `0` se non si sa.
 *
 * La usa anche il registro dei download del visualizzatore, dove il peso è metà della firma di un
 * file già scaricato ([DownloadLog]): una lettura sola per la stessa misura.
 */
internal fun sizeOf(context: Context, uri: Uri): Long {
    /*
     * Una memoria per indirizzo: il peso in testa alla selezione si rifà a ogni cambiamento, e
     * senza memoria interrogherebbe il `ContentResolver` centinaia di volte.
     * ⚠️ **Chi riscrive un file lo dimentica** ([forgetSize], chiamata da `Thumbs.forget`;
     * rilievo C20): altrimenti dopo un salvataggio sopra l'originale la scheda `Info` e la
     * selezione direbbero il peso di prima.
     * ⚠️ **Lo zero non si ricorda**: è anche il ripiego di una lettura andata male (un indirizzo
     * di rete non ancora in cache), e diventerebbe un peso sbagliato per sempre.
     */
    pesi.get(uri)?.let { return it }
    val letto = readSize(context, uri)
    if (letto > 0L) pesi.put(uri, letto)
    return letto
}

/** Dimentica il peso di [uri]: il file è stato riscritto. Vedi [sizeOf]. */
internal fun forgetSize(uri: Uri) {
    pesi.remove(uri)
}

/**
 * I pesi già letti, per indirizzo. Vedi la nota su [sizeOf].
 *
 * Con un tetto (rilievo C20), o crescerebbe di un indirizzo per ogni file mai pesato. Quattromila
 * coprono la selezione di una cartella grande; un peso uscito si rilegge.
 */
private val pesi = LruCache<Uri, Long>(4096)

private fun readSize(context: Context, uri: Uri): Long {
    if (uri.scheme?.lowercase() == "file") {
        return uri.path?.let { runCatching { File(it).length() }.getOrNull() } ?: 0L
    }
    /*
     * ⚠️ Un indirizzo di rete non ha una riga nel `ContentResolver`, e la query darebbe zero: il
     * peso vero è quello dei byte scaricati, che la cache ha già.
     */
    if (uri.scheme?.lowercase() in setOf("http", "https")) {
        return RemoteCache.read(context, uri)?.size?.toLong() ?: 0L
    }
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { c ->
                val at = c.getColumnIndex(OpenableColumns.SIZE)
                if (at >= 0 && c.moveToFirst() && !c.isNull(at)) c.getLong(at) else null
            }
    }.getOrNull() ?: 0L
}

private fun nameOf(context: Context, uri: Uri): String? {
    if (uri.scheme?.lowercase() == "file") return uri.lastPathSegment
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c ->
                val at = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (at >= 0 && c.moveToFirst()) c.getString(at) else null
            }
    }.getOrNull() ?: uri.lastPathSegment
}

/**
 * Il tipo MIME, chiesto prima a chi serve il file e poi all'estensione.
 *
 * L'estensione è il ripiego: un `content://` può servire un JPEG con un nome in `.png`, e là il
 * provider ha ragione; per un `file://` il provider non c'è. La codifica vera la dice
 * [encodingOf], che guarda i byte.
 */
private fun mimeOf(context: Context, uri: Uri): String? {
    runCatching { context.contentResolver.getType(uri) }.getOrNull()?.let { return it }
    /*
     * ⚠️ **Su un indirizzo di rete si guardano i byte prima del nome**: un server può scegliere il
     * formato secondo l'header `Accept`, e AIV chiede come un browser (`image/avif` in testa),
     * quindi un indirizzo in `.jpeg` può arrivare come AVIF. Il tipo della barra viene dalla
     * risposta HTTP, e questa riga deve dire lo stesso.
     */
    if (uri.scheme?.lowercase() in setOf("http", "https")) {
        val head = runCatching {
            streamOf(context, uri)?.use { stream ->
                val buffer = ByteArray(Svg.SNIFF)
                val got = stream.readFully(buffer, Svg.SNIFF)
                buffer.copyOf(got)
            }
        }.getOrNull()
        if (head != null) sniffMime(head)?.let { return it }
    }
    val extension = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()
    if (extension.isNullOrEmpty()) return null
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
}

/**
 * Il tipo che i primi byte di un file dichiarano, e `null` se non lo dicono.
 *
 * AVIF e SVG usano i riconoscitori di `Avif` e `Svg`, che sono più di una firma (marchi del
 * contenitore, dichiarazione XML in testa): qui si aggiungono solo le firme degli altri formati.
 * Il TIFF c'è anche se l'app non lo apre: la funzione dice che cosa sono questi byte, e un file
 * col suo formato dà un messaggio d'errore migliore.
 */
private fun sniffMime(head: ByteArray): String? {
    fun ascii(at: Int, text: String): Boolean =
        head.size >= at + text.length &&
            String(head, at, text.length, Charsets.US_ASCII) == text
    fun byteAt(at: Int): Int = if (head.size > at) head[at].toInt() and 0xFF else -1
    return when {
        Avif.looksLike(head) -> "image/avif"
        byteAt(0) == 0xFF && byteAt(1) == 0xD8 && byteAt(2) == 0xFF -> "image/jpeg"
        byteAt(0) == 0x89 && ascii(1, "PNG") -> "image/png"
        ascii(0, "GIF8") -> "image/gif"
        ascii(0, "RIFF") && ascii(8, "WEBP") -> "image/webp"
        ascii(0, "BM") -> "image/bmp"
        ascii(0, "II") && byteAt(2) == 0x2A && byteAt(3) == 0x00 -> "image/tiff"
        ascii(0, "MM") && byteAt(2) == 0x00 && byteAt(3) == 0x2A -> "image/tiff"
        Svg.looksLike(head) -> Svg.MIME
        else -> null
    }
}

/**
 * Il nome del formato ricavato da un tipo MIME: `image/svg+xml` diventa `SVG`.
 *
 * La chiamano tutti e due i posti che scrivono il formato, il pannello delle informazioni e la
 * barra delle informazioni del visualizzatore, perché dicano la stessa cosa (voce `info-svg`).
 * Il suffisso dopo il `+` è la sintassi in cui il formato è scritto (RFC 6839), non il formato.
 * ⚠️ **Maiuscolo invariante, non della lingua del telefono**: la regola turca della `i`
 * cambierebbe `image/tiff` in `TİFF`.
 */
fun kindOf(mime: String?): String? = mime
    ?.substringAfterLast('/')
    ?.substringBefore('+')
    ?.takeIf { it.isNotBlank() }
    ?.uppercase(Locale.ROOT)

/**
 * La data di scatto secondo il MediaStore, quando l'EXIF non ce l'ha.
 *
 * Le immagini scaricate spesso perdono l'EXIF, e il MediaStore la ricava anche dal nome del file
 * (`IMG_20260830_154212`).
 */
private fun takenFromStore(context: Context, uri: Uri): Long? = runCatching {
    val column = MediaStore.Images.Media.DATE_TAKEN
    context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { c ->
        val at = c.getColumnIndex(column)
        if (at >= 0 && c.moveToFirst() && !c.isNull(at)) c.getLong(at).takeIf { it > 0L } else null
    }
}.getOrNull()

/**
 * L'ultima modifica secondo il MediaStore.
 *
 * ⚠️ **Quella colonna è in secondi**, al contrario di `DATE_TAKEN`, che è in millisecondi: letta
 * come millisecondi darebbe il 1970 su ogni immagine.
 */
private fun modifiedFromStore(context: Context, uri: Uri): Long? = runCatching {
    val column = MediaStore.Images.Media.DATE_MODIFIED
    context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { c ->
        val at = c.getColumnIndex(column)
        if (at >= 0 && c.moveToFirst() && !c.isNull(at)) {
            c.getLong(at).takeIf { it > 0L }?.times(1000L)
        } else null
    }
}.getOrNull()

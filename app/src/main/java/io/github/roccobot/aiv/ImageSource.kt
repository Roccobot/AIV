package io.github.roccobot.aiv

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer

/**
 * Quello che il visualizzatore sa dell'immagine sullo schermo. I numeri sono quelli della scheda
 * delle informazioni, e si misurano durante la decodifica: chiederli dopo vorrebbe dire leggere
 * il file due volte.
 */
data class LoadedImage(
    val bitmap: ImageBitmap,
    val mimeType: String?,
    val byteSize: Long?,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val sampled: Boolean,
    val displayName: String?
)

sealed interface LoadResult {
    data class Ok(val image: LoadedImage) : LoadResult
    data class Failed(val reason: Reason, val detail: String?) : LoadResult

    enum class Reason { NO_IMAGE, UNSUPPORTED, TOO_LARGE, OPEN_FAILED }
}

/**
 * Le regole del caricamento, e il loro perché:
 *
 * - Decodifica `ImageDecoder` (API 28 e oltre): apre JPEG, PNG, GIF, WebP, HEIF e, da API 31,
 *   AVIF, e applica da sé l'orientamento EXIF.
 * - ⚠️ **Per l'AVIF il supporto da API 31 è solo dichiarato**: il decodificatore AV1 del
 *   telefono sui file veri si rifiuta (la misura è su [Avif]). Quando `ImageDecoder` fallisce e
 *   i byte dicono AVIF si riprova con libavif, in quest'ordine: dove il telefono ce la fa,
 *   decodifica lui, con la sua accelerazione e il suo orientamento EXIF.
 * - L'SVG è un ripiego per la ragione opposta: `ImageDecoder` conosce solo i formati a pixel, e
 *   il vettore non lo dichiara nemmeno (vedi [Svg]). Il sintomo è lo stesso, `decodeBitmap` che
 *   va in errore, e i due tentativi si distinguono dai byte e non dall'estensione.
 * - Le immagini remote si scaricano prima in memoria: un flusso non si riavvolge, e
 *   `ImageDecoder` legge l'intestazione, decide il campionamento e poi legge i pixel.
 * - Le immagini enormi si campionano. Il tetto dipende dalla memoria che l'app può usare
 *   davvero: una foto da 64 megapixel in ARGB_8888 pesa circa 256 MB.
 */
object ImageSource {

    private const val MAX_REMOTE_BYTES = 96L * 1024 * 1024
    private const val CONNECT_TIMEOUT_MS = 20_000
    private const val READ_TIMEOUT_MS = 60_000

    /**
     * [onProgress] riceve la frazione già scaricata, solo per gli indirizzi remoti che
     * dichiarano la propria lunghezza. Per un file locale non è chiamata: una barra che salta
     * da zero a cento in un fotogramma è peggio di nessuna barra. Chi non riceve niente mostra
     * il giro indeterminato.
     */
    suspend fun load(
        context: Context,
        uri: Uri?,
        onProgress: (Float) -> Unit = {}
    ): LoadResult = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext LoadResult.Failed(LoadResult.Reason.NO_IMAGE, null)
        try {
            when (uri.scheme?.lowercase()) {
                "http", "https" -> loadRemote(context, uri, onProgress)
                else -> loadLocal(context, uri)
            }
        } catch (e: OutOfMemoryError) {
            LoadResult.Failed(LoadResult.Reason.TOO_LARGE, e.message)
        } catch (e: Exception) {
            // ⚠️ `CancellationException` è una `Exception`: senza questa riga chi lascia il
            // visualizzatore durante uno scaricamento vedrebbe 'non si è potuta aprire'.
            if (e is CancellationException) throw e
            LoadResult.Failed(LoadResult.Reason.OPEN_FAILED, e.message)
        }
    }

    private fun loadLocal(context: Context, uri: Uri): LoadResult {
        val resolver = context.contentResolver
        val source = ImageDecoder.createSource(resolver, uri)
        return decode(
            source = source,
            mimeType = resolver.getType(uri),
            byteSize = localSize(resolver, uri),
            displayName = localName(resolver, uri),
            // Una lambda e non i byte: il ripiego vuole il file intero in memoria, e così lo
            // legge solo quando `ImageDecoder` ha già fallito; il percorso normale legge in
            // streaming.
            raw = { resolver.openInputStream(uri)?.use { it.readBytes() } }
        )
    }

    /** I byte tenuti da parte, aperti come un'immagine scaricata adesso. */
    private fun fromCache(uri: Uri, cached: ByteArray): LoadResult = decode(
        source = ImageDecoder.createSource(ByteBuffer.wrap(cached)),
        mimeType = null,
        byteSize = cached.size.toLong(),
        displayName = uri.lastPathSegment,
        raw = { cached }
    )

    private suspend fun loadRemote(context: Context, uri: Uri, onProgress: (Float) -> Unit): LoadResult {
        // La cache si guarda prima di aprire la connessione: una copia fresca non tocca la
        // rete. Il tipo dichiarato non si conserva coi byte, perché `ImageDecoder` lo ricava
        // dal contenuto.
        val cached = RemoteCache.entry(context, uri)
        if (cached != null && cached.fresh) return fromCache(uri, cached.bytes)
        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            // Alcune CDN rispondono 403 a una richiesta senza queste intestazioni, e una pagina
            // d'errore chiamata .jpg non si decodifica: meglio presentarsi come un browser.
            setRequestProperty("Accept", WEB_ACCEPT)
            setRequestProperty("User-Agent", WEB_AGENT)
            // Una copia scaduta si rivalida: se il server dice che è la stessa, risponde `304`
            // senza corpo e si riapre quella. Vedi la nota su [RemoteCache].
            cached?.etag?.let { setRequestProperty("If-None-Match", it) }
            cached?.lastModified?.let { setRequestProperty("If-Modified-Since", it) }
        }
        try {
            // Senza rete, una copia scaduta vale più di un errore: forse non è la più recente,
            // ma è l'immagine.
            val status = try {
                connection.responseCode
            } catch (e: IOException) {
                if (cached != null) return fromCache(uri, cached.bytes)
                throw e
            }
            if (status == HttpURLConnection.HTTP_NOT_MODIFIED && cached != null) {
                RemoteCache.renew(
                    context, uri, cached,
                    connection.getHeaderField("Cache-Control"),
                    connection.getHeaderField("ETag"),
                    connection.getHeaderField("Last-Modified")
                )
                return fromCache(uri, cached.bytes)
            }
            if (status !in 200..299) {
                return LoadResult.Failed(LoadResult.Reason.OPEN_FAILED, "HTTP $status")
            }
            val declaredType = connection.contentType?.substringBefore(';')?.trim()
            // `contentLengthLong`: `contentLength` è un `int` e oltre i 2 GB torna -1, cioè
            // 'lunghezza sconosciuta'.
            val declaredLength = connection.contentLengthLong
            val bytes = connection.inputStream.use { input ->
                val buffer = ByteArrayOutputStream(
                    declaredLength.coerceIn(64L * 1024, 4L * 1024 * 1024).toInt()
                )
                val chunk = ByteArray(64 * 1024)
                var total = 0L
                var told = -1
                while (true) {
                    // ⚠️ `input.read` è bloccante e un `Job` annullato non la interrompe:
                    // senza questa riga lo scaricamento proseguirebbe fino in fondo. Fra un
                    // pezzo e l'altro è l'unico punto in cui la coroutine se ne accorge.
                    currentCoroutineContext().ensureActive()
                    val read = input.read(chunk)
                    if (read <= 0) break
                    total += read
                    if (total > MAX_REMOTE_BYTES) {
                        return LoadResult.Failed(LoadResult.Reason.TOO_LARGE, null)
                    }
                    buffer.write(chunk, 0, read)
                    // Si riferisce solo quando cambia il punto percentuale: ogni chiamata è
                    // una scrittura di stato, cioè un giro di composizione.
                    if (declaredLength > 0) {
                        val percent = (total * 100 / declaredLength).toInt()
                        if (percent != told) {
                            told = percent
                            onProgress(percent / 100f)
                        }
                    }
                }
                buffer.toByteArray()
            }
            if (bytes.isEmpty()) return LoadResult.Failed(LoadResult.Reason.OPEN_FAILED, "empty answer")
            val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
            val result = decode(
                source = source,
                mimeType = declaredType,
                byteSize = bytes.size.toLong(),
                displayName = uri.lastPathSegment,
                raw = { bytes }
            )
            // ⚠️ **In cache va solo quello che si è aperto davvero**: altrimenti ci finirebbe
            // anche la pagina di errore che un server serve con stato 200 e nome `.jpg`, e
            // 'Riprova' rileggerebbe quei byte senza toccare la rete. Uno scaricamento perso
            // si rifà, una cache avvelenata no.
            if (result is LoadResult.Ok) {
                RemoteCache.write(
                    context, uri, bytes,
                    connection.getHeaderField("Cache-Control"),
                    connection.getHeaderField("ETag"),
                    connection.getHeaderField("Last-Modified")
                )
            }
            return result
        } finally {
            connection.disconnect()
        }
    }

    private fun decode(
        source: ImageDecoder.Source,
        mimeType: String?,
        byteSize: Long?,
        displayName: String?,
        raw: () -> ByteArray?
    ): LoadResult {
        var fullWidth = 0
        var fullHeight = 0
        var sampled = false
        // Il tipo letto dai byte, per quando chi chiama non lo sa: un'immagine ripresa dalla
        // cache, o un `content://` di cui il resolver non dichiara niente. Senza, la riga dei
        // dettagli direbbe '?'.
        var decodedType: String? = null
        val bitmap: Bitmap = try {
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                fullWidth = info.size.width
                fullHeight = info.size.height
                decodedType = info.mimeType
                val sample = sampleSizeFor(info.size)
                if (sample > 1) {
                    sampled = true
                    decoder.setTargetSampleSize(sample)
                }
                // Software e non hardware: un bitmap hardware non si rilegge, e i lavori sui
                // pixel devono leggerlo.
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        } catch (e: Throwable) {
            // ⚠️ `Throwable`, perché può arrivare un `OutOfMemoryError`: lo raccoglie [load]
            // come 'troppo grande', quindi si rilancia, e un ripiego finirebbe la memoria una
            // seconda volta. Lo stesso per l'annullamento, che non è un errore di formato.
            if (e is OutOfMemoryError || e is CancellationException) throw e
            return fallback(raw, mimeType, byteSize, displayName)
                ?: LoadResult.Failed(LoadResult.Reason.UNSUPPORTED, e.message)
        }
        return LoadResult.Ok(
            LoadedImage(
                bitmap = bitmap.asImageBitmap(),
                mimeType = mimeType ?: decodedType,
                byteSize = byteSize,
                pixelWidth = fullWidth.takeIf { it > 0 } ?: bitmap.width,
                pixelHeight = fullHeight.takeIf { it > 0 } ?: bitmap.height,
                sampled = sampled,
                displayName = displayName
            )
        )
    }

    /**
     * Il secondo tentativo, per i soli formati che il sistema non apre: AVIF e SVG.
     *
     * Torna `null` quando non c'è niente da tentare: così chi chiama conserva il proprio errore,
     * che è quello vero.
     *
     * I byte si leggono una volta sola per i due ripieghi. Decide il riconoscimento dei byte, non
     * l'estensione: un file rinominato a mano si apre comunque.
     */
    private fun fallback(
        raw: () -> ByteArray?,
        mimeType: String?,
        byteSize: Long?,
        displayName: String?
    ): LoadResult? {
        val bytes = try {
            raw()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        } ?: return null
        return when {
            Avif.ready && Avif.looksLike(bytes) ->
                fromAvif(bytes, mimeType, byteSize, displayName)
            Svg.looksLike(bytes) -> fromSvg(bytes, mimeType, byteSize, displayName)
            else -> null
        }
    }

    /**
     * L'AVIF, con libavif.
     *
     * La misura la decide lo stesso [pixelBudget] del percorso normale: libavif scala dentro il
     * bitmap che gli si dà.
     */
    private fun fromAvif(
        bytes: ByteArray,
        mimeType: String?,
        byteSize: Long?,
        displayName: String?
    ): LoadResult? {
        val decoded = Avif.decode(bytes, pixelBudget()) ?: return null
        return LoadResult.Ok(
            LoadedImage(
                bitmap = decoded.bitmap.asImageBitmap(),
                // Il tipo si dichiara qui: la strada normale lo ricava da `ImageDecoder`, che su
                // questo file non è arrivato in fondo.
                mimeType = mimeType ?: "image/avif",
                byteSize = byteSize ?: bytes.size.toLong(),
                pixelWidth = decoded.fullWidth,
                pixelHeight = decoded.fullHeight,
                sampled = decoded.sampled,
                displayName = displayName
            )
        )
    }

    /**
     * L'SVG, con androidsvg.
     *
     * Qui il budget è un tetto e non una misura: un vettore non ha pixel suoi, e la misura si
     * sceglie. È [Svg.BOX], ridotta dal budget su un telefono a corto di memoria (il perché è
     * là).
     */
    private fun fromSvg(
        bytes: ByteArray,
        mimeType: String?,
        byteSize: Long?,
        displayName: String?
    ): LoadResult? {
        val drawn = Svg.render(bytes, Svg.BOX, pixelBudget()) ?: return null
        return LoadResult.Ok(
            LoadedImage(
                bitmap = drawn.bitmap.asImageBitmap(),
                mimeType = mimeType ?: Svg.MIME,
                byteSize = byteSize ?: bytes.size.toLong(),
                // Le misure dichiarate dal documento, non quelle del bitmap: il perché è su
                // [Svg.dimensions].
                pixelWidth = drawn.fullWidth,
                pixelHeight = drawn.fullHeight,
                sampled = drawn.sampled,
                displayName = displayName
            )
        )
    }

    /**
     * I pixel di un file, da qualunque strada arrivino: il decodificatore di sistema e, se quello
     * fallisce, il ripiego di [rescue].
     *
     * ⚠️ **È l'unica copia di questa catena** per chi non passa da [load] (l'anteprima
     * dell'editor, il suo salvataggio e la conversione): con le copie sparse, un ripiego nuovo
     * dimenticato in una lasciava la schermata a girare per sempre senza un errore.
     * I chiamanti differiscono solo per la misura: l'anteprima la vuole campionata, chi riscrive
     * il file la vuole intera.
     *
     * @param box il lato lungo massimo, oppure 0 per 'grande quanto viene'.
     */
    fun pixels(context: Context, uri: Uri, box: Int): Bitmap? = runCatching {
        ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(context.contentResolver, uri)
        ) { decoder, info, _ ->
            /*
             * ⚠️ **Il lato lungo arriva esattamente a [box]**: un campionamento a potenze di due
             * lo lascerebbe fino a quasi il doppio, e ogni lavoro per pixel dell'editor costerebbe
             * fino a quattro volte tanto. `ImageDecoder` scala a qualunque misura.
             */
            if (box > 0) {
                val wide = info.size.width
                val high = info.size.height
                val long = max(wide, high)
                if (long > box) {
                    val k = box.toFloat() / long
                    decoder.setTargetSize(
                        (wide * k).roundToInt().coerceAtLeast(1),
                        (high * k).roundToInt().coerceAtLeast(1)
                    )
                }
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
        }
    }.getOrNull() ?: rescue(context, uri, box)

    /**
     * I pixel di un file che `ImageDecoder` non apre (AVIF e SVG), per chi non passa da [load].
     *
     * Si chiama solo dopo che `ImageDecoder` ha già fallito, con un `?:`: su un JPEG non è
     * nemmeno interpellata.
     *
     * @param box il lato lungo massimo, oppure 0 per 'grande quanto viene'.
     */
    fun rescue(context: Context, uri: Uri, box: Int): Bitmap? {
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        } ?: return null
        return when {
            // Due funzioni a seconda del riquadro: `thumbnail` chiede a libavif una misura
            // esatta, `decode` l'immagine intera entro il budget, per chi sta per ritagliare
            // e riscrivere.
            Avif.ready && Avif.looksLike(bytes) -> if (box > 0) {
                Avif.thumbnail(bytes, box)
            } else {
                Avif.decode(bytes, pixelBudget())?.bitmap
            }
            // Su un vettore [Svg.BOX] è anche il tetto di 'grande quanto viene': non c'è una
            // risoluzione vera, e la misura più grande sensata è quella del visualizzatore.
            Svg.looksLike(bytes) -> Svg.render(
                bytes = bytes,
                box = if (box > 0) minOf(box, Svg.BOX) else Svg.BOX,
                cap = pixelBudget()
            )?.bitmap
            else -> null
        }
    }

    /**
     * Il campionamento è una potenza di due e dipende dal budget di pixel, non da un tetto fisso:
     * la stessa foto va bene su un telefono recente ed è impossibile su uno vecchio.
     */
    private fun sampleSizeFor(size: Size): Int {
        val budget = pixelBudget()
        var sample = 1
        var pixels = size.width.toLong() * size.height.toLong()
        while (pixels > budget && sample < 16) {
            sample *= 2
            pixels = (size.width.toLong() / sample) * (size.height.toLong() / sample)
        }
        return sample
    }

    private fun pixelBudget(): Long {
        val runtime = Runtime.getRuntime()
        val usable = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
        // Quattro byte per pixel, e solo un terzo di quello che resta: il resto è per la copia
        // che Compose tiene mentre disegna, e per tutto il resto dell'app.
        return maxOf(4_000_000L, usable / 3 / 4)
    }

    private fun localSize(resolver: ContentResolver, uri: Uri): Long? = try {
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            val column = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (column >= 0 && cursor.moveToFirst() && !cursor.isNull(column)) cursor.getLong(column) else null
        }
    } catch (e: Exception) {
        null
    }

    private fun localName(resolver: ContentResolver, uri: Uri): String? = try {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
        } ?: uri.lastPathSegment
    } catch (e: Exception) {
        uri.lastPathSegment
    }
}

/**
 * Le firme dei formati che l'app riconosce dai primi byte, scritte una volta sola: miniature,
 * filigrana, colori e SVG leggono le stesse, e ognuno dei riconoscimenti non cresce per conto suo.
 */
internal object Signatures {
    /** Gli otto byte con cui comincia ogni PNG. */
    private val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    /** Se [head] comincia con la firma del PNG. */
    fun png(head: ByteArray): Boolean =
        head.size >= PNG.size && PNG.indices.all { head[it] == PNG[it] }

    /**
     * Se [head] comincia con la firma gzip, `1f 8b`, cioè un `.svgz`: i byte e non l'estensione,
     * perché esistono `.svg` compressi e `.svgz` in chiaro, e un decodificatore riceve i byte.
     */
    fun gzip(head: ByteArray): Boolean =
        head.size >= 2 && head[0] == 0x1f.toByte() && head[1] == 0x8b.toByte()
}

/*
 * Le intestazioni con cui l'app chiede un'immagine al web, una volta sola per il caricatore, le
 * serie web e le azioni sull'immagine: un server che risponde in base a loro darebbe a ognuno un
 * file diverso, e il difetto sarebbe invisibile.
 */
internal const val WEB_ACCEPT = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8"
internal const val WEB_AGENT = "Mozilla/5.0 (Android) AIV"

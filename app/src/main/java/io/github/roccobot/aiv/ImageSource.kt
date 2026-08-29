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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer

/**
 * What the viewer knows about the picture on screen. The numbers are what the
 * details panel shows, so they are measured while decoding rather than guessed
 * afterwards: asking the file a second time would mean reading it twice.
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
 * Loading rules, and why they are these:
 *
 * - ImageDecoder does the decoding (API 28 and up). It handles JPEG, PNG, GIF,
 *   WebP, HEIF and, from API 31, AVIF, and it applies the EXIF orientation on
 *   its own. Writing that by hand would be a second implementation of something
 *   the platform already gets right.
 * - Remote images are downloaded into memory first. A stream cannot be rewound,
 *   and ImageDecoder needs to read the header, decide the sample size and then
 *   read the pixels: with a plain stream that means either two requests or a
 *   temporary file.
 * - Huge pictures are sampled down instead of crashing. The ceiling is tied to
 *   the memory the app may actually use, not to a number picked by hand: a
 *   64 megapixel photo is roughly 256 MB as ARGB_8888, which no phone hands to
 *   a single app.
 */
object ImageSource {

    private const val MAX_REMOTE_BYTES = 96L * 1024 * 1024
    private const val CONNECT_TIMEOUT_MS = 20_000
    private const val READ_TIMEOUT_MS = 60_000

    /**
     * [onProgress] riceve la frazione già scaricata, e **solo** per gli indirizzi
     * remoti che dichiarano la propria lunghezza.
     *
     * ⚠️ Non viene chiamata affatto per un file locale, e non è una dimenticanza: là non
     * c'è nessuna attesa da raccontare, e una barra che salta da zero a cento in un
     * fotogramma è peggio di nessuna barra. Chi non riceve niente mostra il giro
     * indeterminato, che è la risposta onesta a 'non so quanto manca'.
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
            // ⚠️⚠️ L'ANNULLAMENTO NON È UN ERRORE DI APERTURA, e senza questa riga
            // sarebbe diventato tale: `CancellationException` è una `Exception`, quindi
            // chi lascia il visualizzatore mentre un'immagine scende si vedrebbe scrivere
            // 'non si è potuta aprire' su una cosa che ha abbandonato lui.
            if (e is CancellationException) throw e
            LoadResult.Failed(LoadResult.Reason.OPEN_FAILED, e.message)
        }
    }

    private fun loadLocal(context: Context, uri: Uri): LoadResult {
        val resolver = context.contentResolver
        val source = ImageDecoder.createSource(resolver, uri)
        return decode(source, resolver.getType(uri), localSize(resolver, uri), localName(resolver, uri))
    }

    private suspend fun loadRemote(context: Context, uri: Uri, onProgress: (Float) -> Unit): LoadResult {
        // ⚠️⚠️ LA CACHE SI GUARDA PRIMA DI APRIRE LA CONNESSIONE, e questo è l'unico
        // punto in cui va guardata: chi ha già questi byte non deve nemmeno sapere che
        // esiste una rete. Il tipo dichiarato non si conserva insieme ai byte perché
        // `ImageDecoder` lo ricava dal contenuto, e un tipo salvato è un secondo dato da
        // tenere d'accordo con il primo.
        RemoteCache.read(context, uri)?.let { cached ->
            return decode(
                ImageDecoder.createSource(ByteBuffer.wrap(cached)),
                null,
                cached.size.toLong(),
                uri.lastPathSegment
            )
        }
        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            // Some CDNs answer 403 to a request without these, and an error page
            // renamed .jpg decodes into nothing: better to look like a browser.
            setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android) AIV")
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                return LoadResult.Failed(LoadResult.Reason.OPEN_FAILED, "HTTP $status")
            }
            val declaredType = connection.contentType?.substringBefore(';')?.trim()
            // ⚠️ `contentLengthLong` e non `contentLength`: quello vecchio è un `int` e
            // torna **-1** oltre i 2 GB, cioè trasformerebbe un file enorme in 'lunghezza
            // sconosciuta' proprio nel caso in cui la barra serve di più.
            val declaredLength = connection.contentLengthLong
            val bytes = connection.inputStream.use { input ->
                val buffer = ByteArrayOutputStream(
                    declaredLength.coerceIn(64L * 1024, 4L * 1024 * 1024).toInt()
                )
                val chunk = ByteArray(64 * 1024)
                var total = 0L
                var told = -1
                while (true) {
                    // ⚠️⚠️ QUI L'ANNULLAMENTO DIVENTA VERO, e senza questa riga non lo
                    // era affatto: `input.read` è una chiamata bloccante, quindi un
                    // `Job` annullato non la interrompe e il download proseguirebbe
                    // fino in fondo su una connessione dati, per un'immagine che
                    // nessuno guarderà. Fra un pezzo e l'altro c'è l'unico punto in cui
                    // questa coroutine può accorgersene.
                    currentCoroutineContext().ensureActive()
                    val read = input.read(chunk)
                    if (read <= 0) break
                    total += read
                    if (total > MAX_REMOTE_BYTES) {
                        return LoadResult.Failed(LoadResult.Reason.TOO_LARGE, null)
                    }
                    buffer.write(chunk, 0, read)
                    // ⚠️⚠️ SI RIFERISCE SOLO AL PUNTO PERCENTUALE CHE CAMBIA, e la
                    // parsimonia è il punto: ogni chiamata è una scrittura di stato,
                    // cioè un giro di composizione, e a 64 KB per pezzo un file da 10 MB
                    // ne farebbe centosessanta invece di cento. Peggio: su un file
                    // piccolo sarebbero tutte nello stesso fotogramma.
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
            val result = decode(source, declaredType, bytes.size.toLong(), uri.lastPathSegment)
            // ⚠️⚠️ SI TIENE DA PARTE SOLO QUELLO CHE SI È APERTO DAVVERO, ed è
            // l'ordine opposto a quello che verrebbe naturale. Tenerli prima
            // salverebbe il download da una decodifica che finisce la memoria, ma
            // metterebbe in cache anche la **pagina di errore** che un server serve con
            // stato 200 e nome `.jpg`: da lì in poi 'Riprova' rileggerebbe quei byte
            // rotti senza toccare la rete, cioè il tasto smetterebbe di poter
            // funzionare. Un download perso si rifà, una cache avvelenata no.
            if (result is LoadResult.Ok) RemoteCache.write(context, uri, bytes)
            return result
        } finally {
            connection.disconnect()
        }
    }

    private fun decode(
        source: ImageDecoder.Source,
        mimeType: String?,
        byteSize: Long?,
        displayName: String?
    ): LoadResult {
        var fullWidth = 0
        var fullHeight = 0
        var sampled = false
        // ⚠️ Il tipo **letto dai byte**, per quando chi chiama non lo sa: succede a una
        // immagine ripresa dalla cache (là si tengono i byte e non le intestazioni HTTP)
        // e a un `content://` di cui il resolver non dichiara niente. Senza, la riga dei
        // dettagli direbbe '?' la seconda volta che si guarda la stessa immagine, e
        // sembrerebbe che l'app abbia dimenticato qualcosa.
        var decodedType: String? = null
        val bitmap: Bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            fullWidth = info.size.width
            fullHeight = info.size.height
            decodedType = info.mimeType
            val sample = sampleSizeFor(info.size)
            if (sample > 1) {
                sampled = true
                decoder.setTargetSampleSize(sample)
            }
            // SOFTWARE, not hardware: a hardware bitmap cannot be read back, and
            // the details panel plus any future pixel work need to read it.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
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
     * The sample size is a power of two, as ImageDecoder wants, and it comes
     * from the pixel budget rather than from a fixed cap: the same photo is
     * fine on a recent phone and impossible on an old one.
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
        // Four bytes per pixel, and only a third of what is left: the rest is
        // for the copy Compose keeps while drawing, and for everything else.
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

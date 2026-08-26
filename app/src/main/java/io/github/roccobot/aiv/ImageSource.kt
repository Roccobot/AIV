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
import kotlinx.coroutines.Dispatchers
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

    suspend fun load(context: Context, uri: Uri?): LoadResult = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext LoadResult.Failed(LoadResult.Reason.NO_IMAGE, null)
        try {
            when (uri.scheme?.lowercase()) {
                "http", "https" -> loadRemote(context, uri)
                else -> loadLocal(context, uri)
            }
        } catch (e: OutOfMemoryError) {
            LoadResult.Failed(LoadResult.Reason.TOO_LARGE, e.message)
        } catch (e: Exception) {
            LoadResult.Failed(LoadResult.Reason.OPEN_FAILED, e.message)
        }
    }

    private fun loadLocal(context: Context, uri: Uri): LoadResult {
        val resolver = context.contentResolver
        val source = ImageDecoder.createSource(resolver, uri)
        return decode(source, resolver.getType(uri), localSize(resolver, uri), localName(resolver, uri))
    }

    private fun loadRemote(context: Context, uri: Uri): LoadResult {
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
            val bytes = connection.inputStream.use { input ->
                val buffer = ByteArrayOutputStream(maxOf(connection.contentLength, 64 * 1024))
                val chunk = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val read = input.read(chunk)
                    if (read <= 0) break
                    total += read
                    if (total > MAX_REMOTE_BYTES) {
                        return LoadResult.Failed(LoadResult.Reason.TOO_LARGE, null)
                    }
                    buffer.write(chunk, 0, read)
                }
                buffer.toByteArray()
            }
            if (bytes.isEmpty()) return LoadResult.Failed(LoadResult.Reason.OPEN_FAILED, "empty answer")
            val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
            return decode(source, declaredType, bytes.size.toLong(), uri.lastPathSegment)
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
        val bitmap: Bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            fullWidth = info.size.width
            fullHeight = info.size.height
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
                mimeType = mimeType,
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

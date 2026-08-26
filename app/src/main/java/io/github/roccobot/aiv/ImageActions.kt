package io.github.roccobot.aiv

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * What the long press menu can do with the picture on screen.
 *
 * ⚠️⚠️ **Save and Share hand over the ORIGINAL file, not a PNG re-encoded from
 * the pixels on screen**, and the difference is not academic: a JPEG photograph
 * written out as PNG comes out several times heavier, and one that was sampled
 * down to fit in memory would be saved at the reduced size, silently. The
 * original is copied straight from wherever it came from, so what lands in the
 * gallery is the file the address points at.
 *
 * Copying to the clipboard is the one place where a PNG IS the right answer: the
 * app that pastes wants an image it can read now, not an address it has to go and
 * fetch, so there the pixels are re-encoded on purpose.
 */
object ImageActions {

    /** The authority declared in the manifest. Written once, or the two would drift. */
    private fun authority(context: Context) = "${context.packageName}.files"

    /** Everything handed to another app goes through here and nowhere else. */
    private fun shareDir(context: Context) = File(context.cacheDir, "share").apply { mkdirs() }

    // ── Clipboard ───────────────────────────────────────────────────────────

    /**
     * Puts the picture itself in the clipboard, as a content URI: an app that
     * pastes gets read access from the system, which is the whole reason this goes
     * through a FileProvider instead of a plain file path.
     */
    fun copyImage(context: Context, image: LoadedImage): Boolean = try {
        val file = File(shareDir(context), "clipboard.png")
        file.outputStream().use { out ->
            image.bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(context, authority(context), file)
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, "AIV", uri))
        true
    } catch (e: Exception) {
        false
    }

    /**
     * The first web address the clipboard holds, or null.
     *
     * Two shapes are accepted, because both happen: a piece of text that contains
     * an address, and a clip that is itself a URI (what a gallery puts there).
     * ⚠️ A URL is looked for INSIDE the text rather than requiring the text to be
     * one: what gets copied from a page is often an address with a word or a
     * newline stuck to it, and refusing that would look like the clipboard was
     * empty.
     */
    fun urlInClipboard(context: Context): Uri? {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val clip = clipboard.primaryClip ?: return null
        for (i in 0 until clip.itemCount) {
            val item = clip.getItemAt(i)
            item.uri?.let { return it }
            val text = item.coerceToText(context)?.toString() ?: continue
            WEB_ADDRESS.find(text)?.let { return it.value.toUri() }
        }
        return null
    }

    private val WEB_ADDRESS = Regex("""https?://[^\s"'<>\\]+""", RegexOption.IGNORE_CASE)

    // ── Does this address lead to a picture? ────────────────────────────────

    /**
     * ⚠️ The extension is asked FIRST and the network only when it says nothing.
     * Plenty of image addresses end in `.jpg` and answering them without a
     * request keeps the common case instant; but plenty of others end in nothing
     * at all (a CDN with an id, a query string), and refusing those on the
     * strength of the name would turn away perfectly good pictures. So the
     * fallback is a HEAD, which asks the server what it is going to send.
     */
    suspend fun leadsToImage(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val scheme = uri.scheme?.lowercase()
        // A local address is not ours to interrogate: the resolver will say soon
        // enough, and a content:// URI has no extension to read anyway.
        if (scheme != "http" && scheme != "https") return@withContext true
        val path = uri.path?.lowercase() ?: ""
        if (IMAGE_SUFFIXES.any { path.endsWith(it) }) return@withContext true
        try {
            val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 8_000
                readTimeout = 8_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android) AIV")
            }
            try {
                val type = connection.contentType?.substringBefore(';')?.trim()?.lowercase()
                connection.responseCode in 200..299 && type != null && type.startsWith("image/")
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            // A HEAD that fails is not a verdict: some servers refuse the method
            // outright. Letting it through means the loader gets its say, and the
            // loader's error message is the accurate one.
            true
        }
    }

    private val IMAGE_SUFFIXES = listOf(
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".heic", ".heif", ".avif"
    )

    // ── Copying the original out ────────────────────────────────────────────

    /**
     * Streams the original file to [out]. Streamed and not buffered: the ceiling
     * on a picture this viewer will open is high, and holding a second copy of it
     * in memory to write it to disk would be the one avoidable way to run out.
     */
    suspend fun copyOriginalTo(context: Context, uri: Uri, out: OutputStream): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val input = when (uri.scheme?.lowercase()) {
                    "http", "https" -> (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 20_000
                        readTimeout = 60_000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Android) AIV")
                    }.inputStream
                    else -> context.contentResolver.openInputStream(uri)
                } ?: return@withContext false
                input.use { it.copyTo(out) }
                true
            } catch (e: Exception) {
                false
            }
        }

    /**
     * A file name for the picture: the one the source gave, or one built from the
     * address, and in both cases with an extension that matches the type. Without
     * the extension the gallery cannot tell what it is holding.
     */
    fun fileName(image: LoadedImage, uri: Uri?): String {
        val fromSource = image.displayName?.takeIf { it.isNotBlank() }
            ?: uri?.lastPathSegment?.takeIf { it.isNotBlank() }
            ?: "image"
        val cleaned = fromSource.substringBefore('?').replace(Regex("""[\\/:*?"<>|]"""), "_")
        if (Regex("""\.[a-z0-9]{2,5}$""", RegexOption.IGNORE_CASE).containsMatchIn(cleaned)) return cleaned
        val suffix = image.mimeType?.substringAfter('/')?.lowercase()?.let { if (it == "jpeg") "jpg" else it }
        return if (suffix.isNullOrBlank()) cleaned else "$cleaned.$suffix"
    }

    // ── Sharing ─────────────────────────────────────────────────────────────

    /** Hands the original to whatever the person picks from the chooser. */
    suspend fun share(context: Context, image: LoadedImage, uri: Uri?): Boolean {
        if (uri == null) return false
        val file = File(shareDir(context), fileName(image, uri))
        val ok = file.outputStream().use { copyOriginalTo(context, uri, it) }
        if (!ok) return false
        val content = FileProvider.getUriForFile(context, authority(context), file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = image.mimeType ?: "image/*"
            putExtra(Intent.EXTRA_STREAM, content)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
        return true
    }

}

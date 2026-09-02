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
     * Mette negli appunti i **nomi** dei file scelti, uno per riga.
     *
     * ⚠️⚠️ **ORDINE ALFABETICO DI NOME E POI DI ESTENSIONE** (richiesta dell'utente,
     * 2026-08-31), che è una cosa diversa dall'ordinare il nome intero: così `foto.jpg` e
     * `foto.png` restano vicine invece di finire una prima e una dopo `foto2.jpg`. E
     * l'ordine dentro ognuno dei due pezzi è quello **naturale**, cioè `foto9` prima di
     * `foto10`, come dappertutto in questa app.
     * ⚠️ **L'ordine NON è quello della griglia**, che è per data: chi copia una lista la
     * incolla da qualche parte per leggerla, e una lista di nomi si legge in ordine di nome.
     * ⚠️ [folder] è il percorso da mettere in testa, e `null` quando l'impostazione è
     * spenta: la decisione sta in chi chiama, che le impostazioni le ha già in mano.
     */
    suspend fun copyNames(context: Context, uris: List<Uri>, folder: String? = null): Int {
        val names = uris.map { Names.of(context, it) }.sortedWith { a, b ->
            val stem = naturalCompare(a.substringBeforeLast('.', a), b.substringBeforeLast('.', b))
            if (stem != 0) stem else naturalCompare(a.substringAfterLast('.', ""), b.substringAfterLast('.', ""))
        }
        copyText(context, (listOfNotNull(folder) + names).joinToString("\n"))
        return names.size
    }

    /**
     * Mette negli appunti UN nome, intero.
     *
     * ⚠️⚠️ **PRENDE IL NOME GIÀ IN MANO A CHI CHIAMA, e non lo va a rileggere**: chi la usa è
     * il dialogo delle info, che il nome ce l'ha perché lo sta mostrando, e una seconda
     * interrogazione al MediaStore potrebbe rispondere una cosa diversa da quella che si legge
     * a schermo. Quello che si copia dev'essere quello che si vede.
     * ⚠️ **È [copyNames] con una lista di uno? No, e la differenza conta**: quella legge i nomi
     * dagli indirizzi, li ordina e ci può mettere davanti il percorso della cartella. Qui il
     * risultato deve essere il nome e basta, senza una riga in più da cancellare a mano dopo
     * averlo incollato.
     */
    fun copyName(context: Context, name: String) = copyText(context, name)

    /** Il testo negli appunti, con l'etichetta di casa: l'unico posto che parla col sistema. */
    private fun copyText(context: Context, text: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("AIV", text))
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

    /**
     * Se l'indirizzo **si dichiara** un'immagine, cioè senza chiedere a nessuno.
     *
     * ⚠️⚠️ **NON è [leadsToImage] con meno passi: è la domanda che si può fare SENZA
     * RETE**, e serve dove una richiesta sarebbe fuori posto. Il caso per cui esiste è
     * l'avvio dell'app, che guarda negli appunti: là una `HEAD` farebbe aspettare
     * l'apertura dell'app a un server, e per un indirizzo che la persona magari aveva
     * copiato per tutt'altro. ⚠️ Il prezzo, dichiarato: un'immagine servita da un
     * indirizzo senza estensione (moltissime, sui siti moderni) qui risponde **no**. È
     * il verso giusto in cui sbagliare, perché l'errore opposto sarebbe aprire da soli
     * qualcosa che nessuno ha chiesto.
     */
    fun looksLikeImage(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return false
        val path = uri.path?.lowercase() ?: return false
        return IMAGE_SUFFIXES.any { path.endsWith(it) }
    }

    /**
     * Le code che [looksLikeImage] riconosce.
     *
     * ⚠️⚠️ **`.svg` C'È DALLA 1.31, e l'ha chiesto l'utente insieme alla visualizzazione**
     * (*va anche aggiunta l'estensione nello schema di ricerca dell'URL da aprire
     * all'avvio*): senza, un indirizzo di SVG copiato negli appunti non veniva nemmeno
     * proposto, cioè il formato si sapeva aprire ma non si sapeva **offrire**.
     * ⚠️ **Non è l'elenco del manifest e non è quello di [Folder]**, ed è la ragione per cui
     * i tre non si possono unire senza pensarci: questo decide se **proporre da soli** un
     * indirizzo che nessuno ha chiesto di aprire, quindi sbaglia nel verso giusto se è più
     * corto.
     * ⚠️ **Dalla `1.32` i tre elenchi combaciano di nuovo su `tif`**, che era la sola
     * discordanza rimasta: qui non c'era mai stato, e nella `1.32` è uscito anche dagli altri
     * due perché l'app non lo sa aprire. Il perché sta su [Folder] e nel manifest.
     */
    private val IMAGE_SUFFIXES = listOf(
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".heic", ".heif", ".avif", ".svg"
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

    /**
     * Consegna PIÙ immagini insieme, quelle scelte nella griglia.
     *
     * ⚠️⚠️ **UN `content://` DEL MEDIASTORE SI PASSA COM'È, e non si ricopia**: è già un
     * indirizzo che il sistema sa concedere in lettura, e copiarne cinquanta nella cache
     * vorrebbe dire scrivere qualche gigabyte per un gesto che dura un secondo. La copia
     * resta per i `file://`, che un'altra app non può leggere e che il nostro FileProvider
     * serve solo dalla sua cartella (`file_paths.xml`, una cartella sola apposta).
     * ⚠️ **Il tipo dichiarato è un jolly**, e qui non si può fare di meglio: una selezione
     * può mescolare JPEG e HEIF, e dichiararne uno solo direbbe il falso su metà. ⚠️ Ma è
     * il jolly **della selezione** e non sempre quello delle immagini, dalla `0.83`: vedi
     * la nota nel corpo.
     * ⚠️⚠️ **E quel jolly NON si scrive dentro un commento a blocco**: in Kotlin i
     * commenti a blocco si ANNIDANO, quindi una barra seguita da un asterisco ne apre un
     * secondo, e la chiusura che dovrebbe chiudere questo chiude quello. L'errore che ne
     * esce parla di commento non chiuso a fine file, cioè lontanissimo dalla riga
     * colpevole. Per la stessa ragione qui non si scrive nemmeno la sequenza di
     * chiusura: nominarla la esegue.
     * ⚠️ **Quello che non si riesce a consegnare si SCARTA invece di far fallire tutto**:
     * su una selezione grande, una copia andata storta non deve mangiarsi le altre
     * quarantanove. Se non ne resta nessuna, torna `false` e chi chiama lo dice.
     */
    suspend fun shareMany(context: Context, uris: List<Uri>): Boolean {
        // ⚠️ **Il tipo si ricava dalla selezione dalla `0.83`**, da quando una cartella può
        // contenere anche filmati: dichiarare `image/*` su un video farebbe comparire nel
        // dialogo gli editor di fotografie, cioè programmi che quel file non aprono. Il
        // jolly buono per un insieme misto è quello generale.
        val kind = when {
            uris.none { Videos.isVideo(it) } -> "image/*"
            uris.all { Videos.isVideo(it) } -> "video/*"
            else -> "*/*"
        }
        val ready = ArrayList<Uri>(uris.size)
        for (uri in uris) {
            if (uri.scheme?.lowercase() == "content") {
                ready += uri
                continue
            }
            val file = File(shareDir(context), uri.lastPathSegment?.substringAfterLast('/') ?: continue)
            val ok = runCatching {
                file.outputStream().use { copyOriginalTo(context, uri, it) }
            }.getOrDefault(false)
            if (ok) ready += FileProvider.getUriForFile(context, authority(context), file)
        }
        if (ready.isEmpty()) return false
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = kind
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ready)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
        return true
    }

}

package io.github.roccobot.aiv

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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

    /**
     * Quanto puo restare in giro nella cartella delle consegne, fra una consegna e l'altra.
     *
     * ⚠️⚠️ **FINO ALLA 1.46 NON C'ERA NESSUN TETTO, e quella cartella cresceva per sempre**:
     * ogni consegna ci scrive una copia INTERA dell'immagine, e niente la ripuliva. Consegnare
     * cinquanta immagini lasciava cinquanta file interi sul telefono, che nessuna schermata
     * dell'app mostra. Le altre due riserve su disco il loro limite ce l'avevano gia, quindi
     * qui la coppia prendere e rilasciare era fatta a meta.
     * ⚠️ **Il tetto governa quello che AVANZA, non la consegna in corso**: si pota all'inizio,
     * quindi un lotto piu grande del tetto passa comunque intero e viene potato alla consegna
     * dopo. Con un tetto applicato in coda, invece, la consegna si mangerebbe i propri file.
     */
    private const val MAX_SHARE_BYTES = 64L * 1024 * 1024

    /**
     * Le copie piu vecchie escono finche la cartella delle consegne rientra nel tetto.
     *
     * ⚠️⚠️ **SI POTA PRIMA DI SCRIVERE, ed e il contrario di quello che fa la riserva remota**
     * (là si sfoltisce dopo, e il perche sta su `RemoteCache`). Qui non si puo fare altrimenti:
     * il permesso di lettura che l'altra app riceve vive quanto la consegna, quindi cancellare
     * subito dopo vorrebbe dire toglierle il file da sotto le mani mentre lo sta leggendo. Alla
     * consegna dopo, invece, quel permesso e finito e la copia e spazzatura.
     * ⚠️ **Sta in `cacheDir`**, quindi in ogni caso Android puo svuotarla quando lo spazio
     * finisce: questa potatura serve a non arrivarci.
     */
    private fun trimShare(context: Context) {
        val files = shareDir(context).listFiles()?.filter { it.isFile } ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_SHARE_BYTES) return
        for (file in files.sortedBy { it.lastModified() }) {
            if (total <= MAX_SHARE_BYTES) return
            val size = file.length()
            if (file.delete()) total -= size
        }
    }

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
    fun urlInClipboard(context: Context): Uri? = clipInClipboard(context)?.address

    /**
     * L'indirizzo negli appunti **e quando ci è finito**.
     *
     * ⚠️⚠️ **L'ISTANTE SERVE A DISTINGUERE 'COPIATO ADESSO' DA 'STA LÀ DA TRE GIORNI', dalla
     * 1.45** (segnalazione dell'utente, 2026-09-03: *se l'URL è negli appunti, non si apre
     * all'avvio*). Chi apre l'app da sé guarda un solo indirizzo per volta e non lo riapre
     * (vedi `clipboardDone`), ma ricopiare lo stesso link è una richiesta esplicita, e col
     * solo confronto delle stringhe era indistinguibile da un link vecchio.
     * ⚠️ **Lo dice il sistema**: `ClipDescription.getTimestamp` è l'ora in cui quel contenuto
     * è stato messo negli appunti. Un'ora nostra direbbe quando lo abbiamo letto, che non
     * risponde alla domanda.
     * ⚠️ **Zero se il sistema non lo sa**, e allora resta il confronto dell'indirizzo: è il
     * comportamento della `0.94`, cioè si perde la novità e non si sbaglia.
     */
    fun clipInClipboard(context: Context): Clip? {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val clip = clipboard.primaryClip ?: return null
        val at = clip.description?.timestamp ?: 0L
        for (i in 0 until clip.itemCount) {
            val item = clip.getItemAt(i)
            item.uri?.let { return Clip(it, at) }
            val text = item.coerceToText(context)?.toString() ?: continue
            WEB_ADDRESS.find(text)?.let { return Clip(it.value.toUri(), at) }
        }
        return null
    }

    /** Quello che gli appunti portano: l'indirizzo e l'istante in cui ci è stato messo. */
    class Clip(val address: Uri, val at: Long)

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
     * ⚠️ **Dalla `1.32` i tre elenchi combaciano su `tif`**: qui non c'era mai stato, e nella
     * `1.32` è uscito anche dagli altri due perché l'app non lo sa aprire. Il perché sta su
     * [Folder] e nel manifest.
     * ⚠️⚠️ **MA QUESTA RIGA DICHIARAVA QUELLA DISCORDANZA L'ULTIMA, ED ERA FALSO**: ne
     * restavano due, `bmp` e `jpe`. La prima è chiusa dalla `1.47`, che ha messo il BMP anche
     * nei filtri del manifest; la seconda resta, ed è **voluta**, perché `.jpe` è una grafia
     * che nessuno scrive in un indirizzo e questo elenco sbaglia nel verso giusto quando è
     * più corto. ⚠️ Un conto scritto a mano in un commento invecchia al primo elenco toccato:
     * qui la lezione è costata due discordanze passate inosservate per quindici versioni.
     * ⚠️ **`.svgz` arriva con la `1.34`**, su richiesta dell'utente, e nei tre elenchi
     * insieme: il decodificatore lo leggeva già (vedi [Folder]), quindi qui non c'era niente
     * da aspettare.
     */
    private val IMAGE_SUFFIXES = listOf(
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".heic", ".heif", ".avif",
        ".svg", ".svgz"
    )

    // ── Copying the original out ────────────────────────────────────────────

    /**
     * Streams the original file to [out]. Streamed and not buffered: the ceiling
     * on a picture this viewer will open is high, and holding a second copy of it
     * in memory to write it to disk would be the one avoidable way to run out.
     */
    suspend fun copyOriginalTo(context: Context, uri: Uri, out: OutputStream): Boolean =
        withContext(Dispatchers.IO) {
            /*
             * ⚠️⚠️ **LA CONNESSIONE SI TIENE IN UNA VARIABILE E SI CHIUDE NEL BLOCCO FINALE, e
             * fino alla 1.46 questo era il solo punto dell'app che non lo faceva**: chiudeva il
             * flusso e basta. L'effetto e piccolo, perche chiudere il flusso rende il
             * collegamento al serbatoio dei riusi, ma un errore che arrivi PRIMA di quella
             * chiusura lascia la connessione aperta fino al raccoglitore. Adesso la coppia e
             * intera, come negli altri punti che aprono la rete.
             */
            var connection: HttpURLConnection? = null
            try {
                val input = when (uri.scheme?.lowercase()) {
                    "http", "https" -> (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 20_000
                        readTimeout = 60_000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Android) AIV")
                        connection = this
                    }.inputStream
                    else -> context.contentResolver.openInputStream(uri)
                } ?: return@withContext false
                input.use { it.copyTo(out) }
                true
            } catch (e: Exception) {
                false
            } finally {
                connection?.disconnect()
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

    /**
     * Il nome spezzato in due: quello che si può cambiare, e il suffisso che non si tocca.
     *
     * ⚠️⚠️ **SERVE PERCHÉ LA FINESTRA DEL NOME CHIEDE IL SOLO NOME** (specifica dell'utente
     * per la `1.77`: *una finestra col solo nome, ispirata a 'Rinomina', precompilata col nome
     * senza estensione*). Chi la apre non deve poter cancellare il suffisso: senza di lui la
     * galleria non sa che cosa tiene in mano, ed è la stessa ragione per cui [fileName] lo
     * aggiunge quando manca.
     * ⚠️⚠️ **IL TAGLIO È ESATTAMENTE QUELLO DI [fileName], e devono restare uguali**: da due a
     * cinque caratteri alfanumerici dopo l'ultimo punto. Se questa funzione fosse più stretta,
     * un nome che [fileName] considera già completo verrebbe qui letto come senza suffisso, e
     * salvando se ne prenderebbe un secondo in coda.
     * ⚠️ **Quindi anche una coda di sole cifre conta come suffisso** (`foto.2026` si spezza in
     * `foto` e `.2026`): non è una bella lettura, ma è la stessa che fa il resto dell'app, e due
     * letture diverse dello stesso nome sarebbero peggio.
     * ⚠️ **Quello che è ricomposto è identico all'intero**, sempre: la coppia non perde né
     * aggiunge un carattere, ed è la sola proprietà su cui si può contare quando si rimette
     * insieme un nome battuto a mano.
     */
    fun splitName(full: String): Pair<String, String> {
        val dot = full.lastIndexOf('.')
        if (dot <= 0) return full to ""
        val tail = full.substring(dot + 1)
        if (!Regex("""^[a-z0-9]{2,5}$""", RegexOption.IGNORE_CASE).matches(tail)) return full to ""
        return full.substring(0, dot) to full.substring(dot)
    }

    /**
     * Se in questo telefono si può scrivere in Download senza chiedere un permesso.
     *
     * ⚠️⚠️ **SI CHIEDE PRIMA, E NON SI DEDUCE DA UN `false` DI [saveToDownloads]**: quella
     * funzione dice `no` sia per un guasto sia perché la via non esiste, e chi deve decidere se
     * tornare al selettore di sistema non può distinguere i due casi da un booleano. Con questa
     * la decisione si prende prima di provare, e il `false` dell'altra vuol dire una cosa sola.
     */
    val downloadsWritable: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /**
     * Scrive l'originale in **Download**, e dice se ce l'ha fatta.
     *
     * ⚠️⚠️ **SEMPRE DOWNLOAD, E NIENTE SELETTORE, dalla `1.77`** (istruzione dell'utente:
     * *niente scelta della cartella, sempre Downloads*). Fino alla `1.76` questo gesto passava
     * dal selettore di sistema (`ACTION_CREATE_DOCUMENT`), che lasciava scegliere il posto e
     * costava due schermate a ogni salvataggio. La cartella pubblica dei download è l'unica in
     * cui si può scrivere **senza chiedere un permesso**, e questo conta anche in vista di Play,
     * dove `MANAGE_EXTERNAL_STORAGE` va motivato.
     *
     * ⚠️⚠️ **DA ANDROID 10 IN SU, E SU 9 NON C'È NIENTE DA FARE QUI**: `MediaStore.Downloads`
     * nasce con l'API 29, e su 28 la stessa cartella si raggiunge solo con
     * `WRITE_EXTERNAL_STORAGE`, cioè con un permesso in più che questa app non chiede. Là questa
     * funzione dice **no** e il chiamante torna al selettore di sistema: è una via in meno, non
     * una funzione mancante, ed è dichiarata invece di essere scoperta.
     *
     * ⚠️ **`IS_PENDING` è la metà che si dimentica**: un file scritto senza di lui compare in
     * galleria mentre lo si sta ancora copiando, quindi un download interrotto lascia in vista
     * un'immagine tagliata. Con la riga in sospeso, il resto del telefono la vede solo alla
     * fine.
     * ⚠️ **I nomi doppi li risolve il MediaStore**, che aggiunge `(1)` da sé: farlo qui vorrebbe
     * dire elencare la cartella per indovinare un nome libero, e fra l'elenco e la scrittura
     * qualcun altro può aver creato quel file.
     *
     * @param name il nome **senza** suffisso, come lo scrive la finestra. Nullo vuol dire
     *   'quello che aveva', ed è il caso del salvataggio diretto.
     */
    suspend fun saveToDownloads(
        context: Context,
        image: LoadedImage,
        uri: Uri?,
        name: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext false
        val whole = fileName(image, uri)
        val (_, suffix) = splitName(whole)
        val chosen = name?.trim()?.takeIf { it.isNotBlank() }
        val display = if (chosen == null) whole else safeName(chosen) + suffix
        val fields = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, display)
            image.mimeType?.let { put(MediaStore.Downloads.MIME_TYPE, it) }
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val row = try {
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, fields)
        } catch (e: Exception) {
            null
        } ?: return@withContext false
        val written = try {
            resolver.openOutputStream(row)?.use { out -> copyOriginalTo(context, uri, out) } ?: false
        } catch (e: Exception) {
            false
        }
        if (!written) {
            /*
             * ⚠️ **La riga a metà si cancella**: senza, in Download resterebbe un file vuoto e
             * in sospeso, che la galleria non mostra e che nessuno sa di avere.
             */
            try {
                resolver.delete(row, null, null)
            } catch (e: Exception) {
                // Niente da fare di più: la riga resta in sospeso e non si vede.
            }
            return@withContext false
        }
        try {
            resolver.update(
                row,
                ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                null,
                null
            )
        } catch (e: Exception) {
            return@withContext false
        }
        true
    }

    /**
     * Toglie da un nome scritto a mano i caratteri che un file non può portare.
     *
     * ⚠️ **Gli stessi di [fileName]**, e per la stessa ragione: `MediaStore` rifiuta o riscrive
     * un `DISPLAY_NAME` che contiene una barra, e un nome riscritto da lui non è quello che la
     * persona ha appena battuto.
     */
    private fun safeName(raw: String): String =
        raw.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifBlank { "image" }

    // ── Sharing ─────────────────────────────────────────────────────────────

    /** Hands the original to whatever the person picks from the chooser. */
    suspend fun share(context: Context, image: LoadedImage, uri: Uri?): Boolean {
        if (uri == null) return false
        trimShare(context)
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
        // ⚠️ La potatura sta QUI e non dentro il giro: là cancellerebbe le copie appena
        // scritte da questo stesso lotto. Vedi [trimShare].
        trimShare(context)
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

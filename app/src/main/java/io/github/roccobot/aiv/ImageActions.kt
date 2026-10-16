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
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
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
 * Che cosa può fare il menu del tocco lungo con l'immagine sullo schermo.
 *
 * ⚠️ **Salva e Condividi consegnano il file originale, non un PNG ricodificato dai pixel a
 * schermo**: un JPEG scritto come PNG pesa molte volte di più, e uno campionato per entrare in
 * memoria sarebbe salvato ridotto, in silenzio. L'originale si copia da dove viene.
 *
 * Negli appunti invece il PNG è la risposta giusta: l'app che incolla vuole un'immagine da
 * leggere subito, non un indirizzo da andare a prendere.
 */
object ImageActions {

    /** L'autorità dichiarata nel manifesto, scritta una volta sola. */
    private fun authority(context: Context) = "${context.packageName}.files"

    /** La cartella delle consegne: tutto quello che passa a un'altra app passa da qui. */
    private fun shareDir(context: Context) = File(context.cacheDir, "share").apply { mkdirs() }

    /**
     * Quanto può restare nella cartella delle consegne, fra una consegna e l'altra.
     *
     * Ogni consegna vi scrive una copia intera dell'immagine, che nessuna schermata mostra:
     * senza tetto la cartella crescerebbe per sempre.
     * ⚠️ **Il tetto governa quello che avanza, non la consegna in corso**: si pota all'inizio,
     * quindi un lotto più grande del tetto passa intero e si pota alla consegna dopo.
     */
    private const val MAX_SHARE_BYTES = 64L * 1024 * 1024

    /**
     * Le copie più vecchie si cancellano finché la cartella delle consegne rientra nel tetto.
     *
     * ⚠️ **Si pota prima di scrivere**, al contrario di `RemoteCache`: il permesso di lettura che
     * l'altra app riceve dura quanto la consegna, e cancellare subito dopo le toglierebbe il file
     * mentre lo legge. Alla consegna dopo quel permesso è finito.
     * La cartella è in `cacheDir`, che Android può svuotare quando lo spazio finisce: questa
     * potatura serve a non arrivarci.
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

    // ── Appunti ─────────────────────────────────────────────────────────────

    /**
     * Mette negli appunti l'immagine stessa, come `content://`: l'app che incolla riceve dal
     * sistema il permesso di lettura, ed è la ragione del FileProvider.
     *
     * ⚠️ **Sospende, e il PNG si scrive su `Dispatchers.IO`**: un PNG a piena misura di una foto
     * da 24 megapixel richiede secondi, e dentro il tocco fermerebbe l'app. Il menu la passa a
     * [MenuOps], il cui ambito sopravvive al menu.
     */
    suspend fun copyImage(context: Context, image: LoadedImage): Boolean = try {
        val file = withContext(Dispatchers.IO) {
            File(shareDir(context), "clipboard.png").also { file ->
                file.outputStream().use { out ->
                    image.bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
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
     * Ordine alfabetico di nome e poi di estensione (richiesta dell'utente): così `foto.jpg` e
     * `foto.png` restano vicine, senza `foto2.jpg` in mezzo. Dentro ognuno dei due pezzi l'ordine
     * è quello naturale, `foto9` prima di `foto10`, come in tutta l'app.
     * Non è l'ordine della griglia, che è per data: una lista di nomi si legge in ordine di nome.
     * [folder] è il percorso da mettere in testa, `null` quando l'impostazione è spenta: decide
     * chi chiama, che ha le impostazioni.
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
     * Prende il nome che chi chiama ha già, senza rileggerlo: il dialogo delle info lo sta
     * mostrando, e una seconda interrogazione al MediaStore potrebbe rispondere altro. Si copia
     * quello che si vede.
     * Non è [copyNames] con una lista di uno: quella può mettere in testa il percorso della
     * cartella, qui serve il nome e basta.
     */
    fun copyName(context: Context, name: String) = copyText(context, name)

    /** Il testo negli appunti, con l'etichetta di casa: l'unico posto che parla col sistema. */
    private fun copyText(context: Context, text: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("AIV", text))
    }

    /**
     * Il primo indirizzo web negli appunti, o `null`.
     *
     * Valgono due forme, perché capitano tutte e due: un testo che contiene un indirizzo, e un
     * elemento che è già un URI (quello che mette una galleria).
     * L'indirizzo si cerca dentro il testo: quello che si copia da una pagina ha spesso una
     * parola o un a capo attaccati.
     */
    fun urlInClipboard(context: Context): Uri? = clipInClipboard(context)?.address

    /**
     * L'indirizzo negli appunti **e quando ci è finito**.
     *
     * ⚠️ **L'istante distingue 'copiato adesso' da 'copiato tre giorni fa'**: l'avvio non riapre
     * un indirizzo già aperto (vedi `clipboardDone`), ma ricopiare lo stesso link è una
     * richiesta esplicita, e il solo confronto delle stringhe non la riconosce.
     * È l'ora del sistema, `ClipDescription.getTimestamp`, cioè quando il contenuto è stato
     * messo negli appunti, non quando l'app l'ha letto. Zero se il sistema non lo sa: allora
     * resta il confronto dell'indirizzo.
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

    /** Quello che gli appunti contengono: l'indirizzo e l'istante in cui vi è stato messo. */
    class Clip(val address: Uri, val at: Long)

    private val WEB_ADDRESS = Regex("""https?://[^\s"'<>\\]+""", RegexOption.IGNORE_CASE)

    // ── Questo indirizzo porta a un'immagine? ───────────────────────────────

    /**
     * Prima si guarda l'estensione, e la rete solo quando l'estensione non dice niente: molti
     * indirizzi finiscono in `.jpg` e rispondono subito, ma molti altri non hanno estensione (una
     * CDN con un identificativo, una coda), e rifiutarli per il nome scarterebbe immagini buone.
     * Il ripiego è una HEAD, che chiede al server che cosa manderà.
     */
    suspend fun leadsToImage(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val scheme = uri.scheme?.lowercase()
        // Un indirizzo locale non si interroga: risponderà il resolver, e un content:// non ha
        // un'estensione da leggere.
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
            // Una HEAD che fallisce non è un verdetto: alcuni server rifiutano il metodo. Si
            // lascia decidere il caricatore, il cui messaggio d'errore è quello esatto.
            true
        }
    }

    /**
     * Se l'immagine arriva dalla rete, cioè se non c'è nessun file su cui lavorare.
     *
     * Spegne 'Rinomina' per le immagini del Web (richiesta dell'utente, 1.83): un indirizzo
     * `http` non ha un nome di file che l'app possa cambiare.
     * La domanda si fa sullo schema e non sul nome: un indirizzo remoto può finire con `.jpg` o
     * no.
     */
    fun isRemote(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()
        return scheme == "http" || scheme == "https"
    }

    /**
     * Se l'indirizzo **si dichiara** un'immagine, cioè senza chiedere a nessuno.
     *
     * È la domanda che si può fare senza rete, per l'avvio dell'app che guarda negli appunti:
     * là una `HEAD` farebbe aspettare l'apertura a un server, per un indirizzo forse copiato per
     * tutt'altro.
     * ⚠️ Un'immagine servita da un indirizzo senza estensione qui risponde no: è il verso giusto
     * in cui sbagliare, perché l'errore opposto aprirebbe da sola una cosa che nessuno ha
     * chiesto.
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
     * ⚠️ **Non è l'elenco del manifesto né quello di [Folder]**, e i tre non si uniscono senza
     * pensarci: questo decide se proporre da sola un indirizzo che nessuno ha chiesto di aprire,
     * quindi se è più corto sbaglia nel verso giusto. Per questo manca `.jpe`, una grafia che
     * nessuno scrive in un indirizzo. Un formato nuovo entra nei tre elenchi insieme.
     */
    private val IMAGE_SUFFIXES = listOf(
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".heic", ".heif", ".avif",
        ".svg", ".svgz", ".dng"
    )

    // ── La copia dell'originale ─────────────────────────────────────────────

    /**
     * Copia l'originale in [out], in streaming: il tetto delle immagini che l'app apre è alto, e
     * una seconda copia in memoria per scriverla su disco esaurirebbe la memoria senza ragione.
     */
    suspend fun copyOriginalTo(context: Context, uri: Uri, out: OutputStream): Boolean =
        withContext(Dispatchers.IO) {
            // La connessione si tiene in una variabile e si chiude nel blocco finale, come negli
            // altri punti che aprono la rete: un errore prima della chiusura del flusso la
            // lascerebbe aperta fino al raccoglitore.
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
     * Un nome di file per l'immagine: quello della sorgente, o uno ricavato dall'indirizzo, in
     * tutti e due i casi con un'estensione che corrisponde al tipo. Senza estensione la galleria
     * non sa che cosa contiene il file.
     */
    fun fileName(image: LoadedImage, uri: Uri?): String {
        val fromSource = image.displayName?.takeIf { it.isNotBlank() }
            ?: uri?.lastPathSegment?.takeIf { it.isNotBlank() }
            ?: "image"
        val cleaned = fromSource.substringBefore('?').replace(FORBIDDEN, "_")
        val own = Regex("""\.[a-z0-9]{2,5}$""", RegexOption.IGNORE_CASE).find(cleaned)?.value
        val base = if (own != null) cleaned.dropLast(own.length) else cleaned
        val suffix = own ?: image.mimeType?.substringAfter('/')?.lowercase()
            ?.let { if (it == "jpeg") "jpg" else it }
            ?.takeIf { it.isNotBlank() }?.let { ".$it" }
            ?: ""
        return fitBytes(base, NAME_BYTES).ifBlank { "image" } + suffix
    }

    /**
     * I caratteri che un nome di file non può contenere: quelli che il filesystem o il
     * MediaStore rifiutano, e i caratteri di controllo, fra cui il carattere nullo che un `%00`
     * dell'indirizzo diventa una volta decodificato.
     */
    private val FORBIDDEN = Regex("""[\\/:*?"<>|\p{Cntrl}]""")

    /**
     * ⚠️ **Il tetto del nome è in byte, non in caratteri**: il filesystem ne accetta 255 per
     * nome, e oltre `FileOutputStream` dà errore. L'ultimo pezzo dell'indirizzo di un'immagine
     * del Web è spesso un identificativo di centinaia di caratteri, e una lettera accentata o un
     * ideogramma occupano da due a quattro byte. 180 lasciano il posto al suffisso e al
     * contatore di [FileTree.freeName].
     */
    private const val NAME_BYTES = 180

    /** [text] accorciato a [max] byte in UTF-8, senza spezzare un carattere. */
    internal fun fitBytes(text: String, max: Int): String {
        var bytes = 0
        var end = 0
        while (end < text.length) {
            val cp = text.codePointAt(end)
            val size = String(Character.toChars(cp)).toByteArray(Charsets.UTF_8).size
            if (bytes + size > max) break
            bytes += size
            end += Character.charCount(cp)
        }
        return text.substring(0, end)
    }

    /**
     * Il nome spezzato in due: quello che si può cambiare, e il suffisso che non si tocca.
     *
     * Serve alla finestra del nome, che chiede il solo nome (specifica dell'utente, 1.77): chi la
     * apre non deve poter cancellare il suffisso, per la stessa ragione per cui [fileName] lo
     * aggiunge quando manca.
     * ⚠️ **Il taglio è esattamente quello di [fileName], e devono restare uguali**: da due a
     * cinque caratteri alfanumerici dopo l'ultimo punto. Più stretta, un nome già completo per
     * [fileName] sarebbe letto qui senza suffisso, e salvando ne prenderebbe un secondo.
     * Quindi anche una coda di sole cifre conta come suffisso (`foto.2026`).
     * Le due parti ricomposte sono sempre identiche all'intero.
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
     * Si chiede prima di provare: [saveToDownloads] risponde `false` sia per un guasto sia perché
     * la via non esiste, e chi deve decidere se tornare al selettore di sistema non distingue i
     * due casi da un booleano.
     */
    val downloadsWritable: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /**
     * Scrive l'originale in **Download**, e dice se ce l'ha fatta.
     *
     * Sempre Download, senza selettore (istruzione dell'utente, 1.77): è l'unica cartella
     * pubblica in cui si scrive senza chiedere un permesso, e su Play `MANAGE_EXTERNAL_STORAGE`
     * va motivato.
     *
     * ⚠️ **Da Android 10 in su**: `MediaStore.Downloads` esiste dall'API 29, e su 28 la cartella
     * si raggiunge solo con `WRITE_EXTERNAL_STORAGE`, che AIV Play non chiede (il gesto resta lo
     * stesso nelle due varianti). Là la funzione risponde no e il chiamante torna al selettore di
     * sistema.
     *
     * ⚠️ **`IS_PENDING`**: senza, il file compare in galleria mentre si sta ancora copiando, e un
     * download interrotto lascia in vista un'immagine tagliata. Con la riga in sospeso, il resto
     * del telefono la vede solo alla fine.
     * I nomi doppi li risolve il MediaStore, che aggiunge `(1)` da sé: elencare la cartella per
     * trovare un nome libero lascerebbe a un altro il tempo di creare quel file.
     *
     * @param name il nome senza suffisso, come lo scrive la finestra; nullo vuol dire 'quello che
     *   aveva', ed è il caso del salvataggio diretto.
     * @param suffix il suffisso scelto nella finestra, col punto, vuoto per 'nessuno'; nullo vuol
     *   dire 'quello che aveva'.
     */
    suspend fun saveToDownloads(
        context: Context,
        image: LoadedImage,
        uri: Uri?,
        name: String? = null,
        suffix: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext false
        val whole = fileName(image, uri)
        val had = splitName(whole).second
        val tail = suffix ?: had
        val chosen = name?.trim()?.takeIf { it.isNotBlank() }
        val display = if (chosen == null) whole else safeName(chosen) + tail
        /*
         * ⚠️ **Se il suffisso è cambiato, il tipo dichiarato segue il nome e non i byte**:
         * quando tipo ed estensione del `DISPLAY_NAME` non concordano, il `MediaStore` aggiunge
         * l'estensione del tipo, e un `foto.png` dichiarato `image/jpeg` finirebbe in Download
         * come `foto.png.jpg`, senza errori. Che il file menta è voluto: cambiare l'estensione
         * non converte niente, e il pannellino lo dice (vedi `ExtensionDialog`). Le altre regole
         * sono su [declaredType].
         */
        val declared = declaredType(image, tail, had)
        intoDownloads(context, display, declared) { out -> copyOriginalTo(context, uri, out) }
    }

    /**
     * Un file che l'app ha scritto nella sua cartella, copiato in Download (gli editor di AIV
     * Play): la stessa strada di [saveToDownloads], perché in AIV Play quella è l'unica cartella
     * condivisa in cui l'app può scrivere.
     *
     * Il tipo segue il nome, come in [declaredType]: gli editor scrivono JPEG e PNG, e il nome lo
     * dice già.
     */
    suspend fun fileToDownloads(context: Context, file: File): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext false
        val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
        intoDownloads(context, file.name, type) { out ->
            file.inputStream().use { it.copyTo(out) }
            true
        }
    }

    /**
     * La riga in Download, scritta e poi rilasciata: la parte comune a [saveToDownloads] e
     * [fileToDownloads]. Le note su `IS_PENDING` sono su [saveToDownloads].
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun intoDownloads(
        context: Context,
        display: String,
        type: String?,
        write: suspend (OutputStream) -> Boolean
    ): Boolean {
        val fields = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, display)
            type?.let { put(MediaStore.Downloads.MIME_TYPE, it) }
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val row = try {
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, fields)
        } catch (e: Exception) {
            null
        } ?: return false
        val written = try {
            resolver.openOutputStream(row)?.use { write(it) } ?: false
        } catch (e: Exception) {
            false
        }
        if (!written) {
            // La riga a metà si cancella: senza, in Download resterebbe un file vuoto e in
            // sospeso, che la galleria non mostra.
            try {
                resolver.delete(row, null, null)
            } catch (e: Exception) {
                // Se anche la cancellazione fallisce non resta altro: la riga rimane in
                // sospeso e la galleria non la mostra.
            }
            return false
        }
        try {
            resolver.update(
                row,
                ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                null,
                null
            )
        } catch (e: Exception) {
            return false
        }
        return true
    }

    /**
     * Il tipo da dichiarare a chi scrive il file, che segue il nome e non i byte.
     *
     * Una funzione sola per i due chiamanti, [saveToDownloads] e [saveToFolder].
     * ⚠️ **Se il suffisso non è cambiato resta [LoadedImage.mimeType]**: il nome può mentire già in
     * partenza (il JPEG di Pexels dichiarato AVIF), e il tipo vero è quello misurato dal
     * caricamento.
     * Un'estensione sconosciuta lascia il tipo nullo, e allora lo deduce il fornitore dal nome.
     */
    private fun declaredType(image: LoadedImage, tail: String, had: String): String? =
        if (tail.equals(had, ignoreCase = true)) image.mimeType else {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(tail.removePrefix(".").lowercase())
        }

    /**
     * Scrive l'originale nella cartella scelta dall'utente, e dice se ce l'ha fatta.
     *
     * 'Percorso' apre `ACTION_OPEN_DOCUMENT_TREE`, che restituisce una cartella, e a scrivere è
     * questa funzione al tocco su 'Salva' (voce `scarica-percorso`: scegliere la destinazione,
     * non salvare).
     *
     * ⚠️ **Il permesso sulla cartella va preso in modo persistente**: lo fa il richiamo che
     * riceve l'albero (`takePersistableUriPermission`), e senza, il giorno dopo qui si avrebbe
     * una `SecurityException`. L'indirizzo memorizzato da solo non basta.
     * Un tipo sconosciuto diventa `application/octet-stream`, perché `createDocument` vuole un
     * tipo. I nomi doppi li risolve il fornitore, come il `MediaStore`.
     * Il documento a metà si cancella, come la riga in sospeso di [saveToDownloads].
     */
    suspend fun saveToFolder(
        context: Context,
        image: LoadedImage,
        uri: Uri?,
        tree: Uri,
        name: String? = null,
        suffix: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext false
        val whole = fileName(image, uri)
        val had = splitName(whole).second
        val tail = suffix ?: had
        val chosen = name?.trim()?.takeIf { it.isNotBlank() }
        val display = if (chosen == null) whole else safeName(chosen) + tail
        val declared = declaredType(image, tail, had) ?: "application/octet-stream"
        val resolver = context.contentResolver
        val target = try {
            val parent = DocumentsContract.buildDocumentUriUsingTree(
                tree,
                DocumentsContract.getTreeDocumentId(tree)
            )
            DocumentsContract.createDocument(resolver, parent, declared, display)
        } catch (e: Exception) {
            null
        } ?: return@withContext false
        val written = try {
            resolver.openOutputStream(target)?.use { out -> copyOriginalTo(context, uri, out) }
                ?: false
        } catch (e: Exception) {
            false
        }
        if (!written) {
            try {
                DocumentsContract.deleteDocument(resolver, target)
            } catch (e: Exception) {
                // Se anche la cancellazione fallisce non resta altro: nella cartella rimane un
                // file vuoto, che si vede e si cancella a mano.
            }
        }
        written
    }

    /**
     * Toglie da un nome scritto a mano i caratteri che un file non può portare.
     *
     * Gli stessi caratteri di [fileName]: `MediaStore` rifiuta o riscrive un `DISPLAY_NAME` che
     * contiene una barra, e il nome non sarebbe più quello scritto.
     * La usa anche il file di uno stile solo (`StyleSettings`), che prende il nome dallo stile:
     * 'T&O - Blu/Rosso' ha una barra.
     */
    internal fun safeName(raw: String): String =
        raw.replace(FORBIDDEN, "_").trim().ifBlank { "image" }

    // ── Condivisione ────────────────────────────────────────────────────────

    /**
     * Consegna l'originale all'app che la persona sceglie dal selettore.
     *
     * ⚠️ **La copia è su `Dispatchers.IO` e dentro un `runCatching`**: un nome che il filesystem
     * rifiuta darebbe un errore che nessuno raccoglie, e l'app si chiuderebbe; e la potatura
     * della cartella non deve girare sul thread dell'interfaccia.
     */
    suspend fun share(context: Context, image: LoadedImage, uri: Uri?): Boolean {
        if (uri == null) return false
        val file = withContext(Dispatchers.IO) {
            runCatching {
                trimShare(context)
                val file = File(shareDir(context), fileName(image, uri))
                val ok = file.outputStream().use { copyOriginalTo(context, uri, it) }
                file.takeIf { ok }
            }.getOrNull()
        } ?: return false
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
     * Gli stessi file, in indirizzi che un'altra app può leggere.
     *
     * ⚠️ **Un `content://` del MediaStore si passa com'è**: il sistema sa già concederlo in
     * lettura, e copiarne cinquanta nella cache scriverebbe gigabyte per un gesto di un secondo.
     * Si copiano solo i `file://`, che un'altra app non può leggere e che il FileProvider serve
     * solo dalla sua cartella (`file_paths.xml`).
     * Quello che non si riesce a preparare si scarta: una copia andata storta non deve far
     * perdere le altre. Chi chiama guarda quante ne tornano.
     * La usano la condivisione e la consegna al selettore di sistema (`ViewerActivity`,
     * modalità scelta): una copia sola, che segue il FileProvider.
     * Chi chiama aggiunge `FLAG_GRANT_READ_URI_PERMISSION` all'intento: un indirizzo preparato
     * non concede niente da sé.
     */
    suspend fun readableOutside(context: Context, uris: List<Uri>): List<Uri> {
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
        return ready
    }

    /**
     * Consegna più immagini insieme, quelle scelte nella griglia.
     *
     * Il tipo dichiarato è un jolly della selezione: una selezione può mescolare JPEG e HEIF, o
     * immagini e video, e un tipo solo direbbe il falso su una parte.
     * ⚠️ **Il jolly generale non si scrive in un commento a blocco**: in Kotlin i commenti a
     * blocco si annidano, e la barra seguita dall'asterisco ne aprirebbe un secondo, con un
     * errore di commento non chiuso a fine file.
     * Se nessun file si riesce a preparare torna `false`, e chi chiama lo dice.
     */
    suspend fun shareMany(context: Context, uris: List<Uri>): Boolean {
        // La potatura è prima del giro: dentro cancellerebbe le copie appena scritte da questo
        // stesso lotto. Vedi [trimShare].
        trimShare(context)
        // Il tipo si ricava dalla selezione: `image/*` su un video farebbe comparire nel
        // dialogo gli editor di immagini, che quel file non aprono. Per un insieme misto vale
        // il jolly generale.
        val kind = when {
            uris.none { Videos.isVideo(it) } -> "image/*"
            uris.all { Videos.isVideo(it) } -> "video/*"
            else -> "*/*"
        }
        val ready = readableOutside(context, uris)
        if (ready.isEmpty()) return false
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = kind
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(ready))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
        return true
    }

}

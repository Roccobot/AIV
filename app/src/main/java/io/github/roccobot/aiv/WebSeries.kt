package io.github.roccobot.aiv

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Come si sfoglia un'immagine che è sul Web, dove non esiste nessuna cartella.
 *
 * ⚠️ **AIV riceve l'indirizzo di un'immagine, mai quello della pagina che la conteneva.** Non
 * c'è un genitore da risalire né un elenco da leggere: la vicina si può solo indovinare
 * trasformando l'indirizzo, e poi verificare che esista. Da qui la scala di criteri e la domanda
 * di rete prima del gesto.
 *
 * Prima la verifica (decisione dell'utente): quando la vicina non esiste, la strisciata non fa
 * niente, senza aprire un errore. La domanda di rete si fa in anticipo, appena l'immagine
 * corrente è pronta, o il dito aspetterebbe la rete.
 *
 * La serie del Web è larga tre e si rifà a ogni passo, mentre quella di una cartella nasce
 * intera: una serie remota non ha fine nota, e costruirla tutta vorrebbe dire interrogare il
 * server finché non risponde di no. Al resto dell'app basta una lista con un indice.
 */
object WebSeries {

    /**
     * I criteri, **nell'ordine in cui si provano**: dal più certo e gratuito al più largo.
     *
     * ⚠️ **Il primo che verifica vince e si blocca** per quella serie: senza il blocco la
     * cascata si ripercorrerebbe a ogni strisciata, con molte più domande di rete per immagine.
     * Il nome del file vince sul percorso perché è più vicino all'immagine; la lettera viene
     * dopo i numeri perché è la più rara e la più facile da confondere con l'ultima lettera di
     * una parola.
     */
    enum class Rule {
        /** L'ultimo numero nel nome del file: `foto_012.jpg` -> `foto_013.jpg`. */
        NAME_NUMBER,

        /** Il numero nel percorso, quando il nome non ne ha: `/galleria/12/grande.jpg`. */
        PATH_NUMBER,

        /** Il numero nella coda dell'indirizzo: `view.php?id=4821`. */
        QUERY_NUMBER,

        /** La lettera in coda al nome, dove non c'è nessun numero: `tavola_a.png`. */
        NAME_LETTER,

        /**
         * L'indice della cartella che la contiene, **chiesto al server**.
         *
         * È l'unico che chiede l'elenco al server, e dà una serie vera: ordinata, finita, e
         * senza una domanda di rete per ogni vicina. Molti server rispondono all'indirizzo di
         * una cartella con l'elenco dei suoi file.
         * Viene per ultimo perché costa una pagina scaricata invece di una domanda secca.
         */
        FOLDER_INDEX,

        /**
         * Le immagini della pagina che si è aperta.
         *
         * ⚠️ **Non è nella cascata**: gli altri partono dall'indirizzo di un'immagine e cercano
         * la vicina, questo parte da una pagina, che è un altro ingresso. [around] lo salta
         * ([GUESSED]) e ci si arriva solo da [fromPage], cioè dal dialogo 'Apri un indirizzo'
         * quando l'indirizzo non porta a un'immagine.
         * Niente filtro nuovo nel manifesto né bersaglio di condivisione in più: l'utente ha
         * rifiutato che AIV accetti qualunque testo condiviso (voce `brave-link`).
         * Come [FOLDER_INDEX] dà la serie intera, quindi non si rifà a ogni passo.
         */
        PAGE_LINKS
    }

    /**
     * I criteri che [around] percorre: tutti tranne quelli che partono da una pagina.
     *
     * Si ricava per esclusione: un criterio nuovo entra nella cascata da sé, e chi ne aggiunge
     * uno che parte da altro deve nominarlo qui.
     */
    private val GUESSED = Rule.entries - Rule.PAGE_LINKS

    /**
     * I criteri che danno la serie intera, per cui non si rifà la finestra a ogni passo:
     * rifarla vorrebbe dire riscaricare la stessa pagina a ogni immagine.
     */
    val WHOLE = setOf(Rule.FOLDER_INDEX, Rule.PAGE_LINKS)

    /** La finestra costruita attorno a un indirizzo, col criterio che l'ha prodotta. */
    data class Window(val lookup: Folder.Lookup, val rule: Rule?)

    /** Se questo indirizzo vive sulla rete, cioè se questo file lo riguarda. */
    fun isWeb(uri: Uri): Boolean = uri.scheme?.lowercase() in SCHEMES

    /**
     * Lo stesso indirizzo in sicuro, perché in chiaro non si aprirebbe affatto.
     *
     * ⚠️ **Android blocca il traffico in chiaro di serie** da `targetSdk 28`, e qui non c'è
     * `usesCleartextTraffic`: un indirizzo `http://` si fermerebbe al caricatore, mentre il
     * manifesto dichiara i filtri per `http`.
     * Si riscrive, senza permettere il chiaro (decisione dell'utente): quasi tutti i siti
     * rispondono anche in sicuro, e permettere il chiaro nel manifesto varrebbe per gli
     * indirizzi di chiunque. Chi resta in chiaro davvero non riceve un errore muto: il modello
     * sa di aver riscritto, e lo dice.
     */
    fun secured(uri: Uri): Uri =
        if (uri.scheme.equals("http", ignoreCase = true)) {
            uri.buildUpon().scheme("https").build()
        } else {
            uri
        }

    /**
     * La finestra attorno a [uri]: la vicina di prima, lei, e la vicina di dopo.
     *
     * [locked] è il criterio già scelto per questa serie, quando c'è: passandolo si salta la
     * cascata.
     *
     * Le due verifiche partono insieme: in fila raddoppierebbero l'attesa. Un criterio che non
     * produce nemmeno un candidato si salta senza toccare la rete, e per questo la cascata può
     * essere lunga.
     */
    suspend fun around(uri: Uri, locked: Rule? = null): Window = withContext(Dispatchers.IO) {
        val rules = if (locked != null) listOf(locked) else GUESSED
        for (rule in rules) {
            // L'indice produce la serie intera, quindi salta i candidati e le due verifiche.
            if (rule == Rule.FOLDER_INDEX) {
                val whole = fromIndex(uri) ?: continue
                return@withContext Window(whole, rule)
            }
            val ahead = candidate(uri, rule, 1)
            val behind = candidate(uri, rule, -1)
            if (ahead == null && behind == null) continue
            val (next, previous) = coroutineScope {
                val one = async { ahead?.takeIf { exists(it) } }
                val other = async { behind?.takeIf { exists(it) } }
                one.await() to other.await()
            }
            if (next == null && previous == null) continue
            val items = listOfNotNull(previous, uri, next)
            val found = Folder.Lookup.Found(Folder.Series(items, if (previous != null) 1 else 0))
            return@withContext Window(found, rule)
        }
        // `Alone` e non `Lost`: nessun criterio ha trovato una vicina, cioè l'immagine non ha
        // nessuno accanto, il caso che la riga dei dettagli e la strisciata sanno trattare.
        Window(Folder.Lookup.Alone, null)
    }

    /**
     * L'indirizzo che verrebbe spostandosi di [delta] posti secondo [rule], se ha senso.
     *
     * È una funzione pura: non tocca la rete e non sa se quello che produce esista.
     */
    fun candidate(uri: Uri, rule: Rule, delta: Int): Uri? = when (rule) {
        Rule.NAME_NUMBER -> onFileName(uri) { stem -> bumped(stem, delta) }
        Rule.PATH_NUMBER -> onFolderPath(uri, delta)
        Rule.QUERY_NUMBER -> onQuery(uri, delta)
        Rule.NAME_LETTER -> onFileName(uri) { stem -> lettered(stem, delta) }
        // I due che danno la serie intera ([WHOLE]) non hanno un vicino da calcolare: la loro
        // serie nasce in [fromIndex] e in [fromPage]. Niente `else`, che assorbirebbe in
        // silenzio un criterio nuovo.
        Rule.FOLDER_INDEX, Rule.PAGE_LINKS -> null
    }

    // ── La pagina che conteneva l'immagine ──────────────────────────────────

    /**
     * Le immagini di una **pagina**, nell'ordine in cui vi compaiono.
     *
     * ⚠️ **L'ordine è quello del documento, non quello naturale dei nomi** come in
     * [fromIndex]: una pagina è impaginata da qualcuno, e quell'ordine è l'informazione che si
     * va a prendere.
     * Si guardano `src` e `href` insieme: le miniature sono negli `img` e le versioni grandi nei
     * collegamenti che le avvolgono. `distinct` tiene la prima apparizione.
     * Niente lettore di HTML, come in [fromIndex]: si raccolgono due attributi.
     * Basta un'immagine sola, al contrario di [fromIndex]: una pagina con una sola immagine
     * grande è il caso di quasi tutti i siti di scansioni.
     * Il tetto di lettura è quello di [fetch].
     */
    suspend fun fromPage(uri: Uri): Folder.Series? = withContext(Dispatchers.IO) {
        val page = fetch(uri) ?: return@withContext null
        val base = uri.toString()
        val items = SOURCES.findAll(page)
            .mapNotNull { resolved(base, it.groupValues[1].trim()) }
            .filter { ImageActions.looksLikeImage(Uri.parse(it)) }
            .distinct()
            .map(Uri::parse)
            .toList()
        if (items.isEmpty()) null else Folder.Series(items, 0)
    }

    /**
     * Se questo indirizzo risponde con una immagine.
     *
     * Un solo secondo tentativo, poi è no: senza una risposta il gesto non fa niente, e un
     * 'forse' ripetuto sarebbe un dito che aspetta.
     * ⚠️ **[ImageActions.leadsToImage] davanti a un errore di rete risponde sì**, ed è giusto
     * per un indirizzo digitato da una persona; questa vicina invece non l'ha chiesta nessuno.
     * Stessa forma, verdetto opposto, e va tenuto così.
     */
    suspend fun exists(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        probe(uri) ?: probe(uri) ?: false
    }

    // ── I criteri, uno per uno ──────────────────────────────────────────────

    /**
     * Applica [change] al nome del file, lasciando stare percorso ed estensione.
     *
     * ⚠️ **L'estensione si stacca prima di cercare il numero**: esistono estensioni con una
     * cifra (`.jp2`, `.mp4`), e si sfoglierebbe cambiando formato.
     * Si lavora sul percorso codificato: le cifre non si codificano mai, e tutto il resto resta
     * byte per byte com'era; dal percorso decodificato un indirizzo con uno spazio o un accento
     * tornerebbe riscritto.
     */
    private fun onFileName(uri: Uri, change: (String) -> String?): Uri? {
        val path = uri.encodedPath ?: return null
        val cut = path.lastIndexOf('/')
        val segment = path.substring(cut + 1)
        if (segment.isEmpty()) return null
        val dot = segment.lastIndexOf('.')
        val stem = if (dot > 0) segment.substring(0, dot) else segment
        val extension = if (dot > 0) segment.substring(dot) else ""
        val moved = change(stem) ?: return null
        return uri.buildUpon().encodedPath(path.substring(0, cut + 1) + moved + extension).build()
    }

    /**
     * Il numero nel percorso, **escluso** l'ultimo segmento.
     *
     * L'esclusione tiene separati i due criteri: senza, questo troverebbe sempre il numero del
     * nome del file. Serve ai siti che numerano la pagina e non il file.
     */
    private fun onFolderPath(uri: Uri, delta: Int): Uri? {
        val path = uri.encodedPath ?: return null
        val cut = path.lastIndexOf('/')
        if (cut <= 0) return null
        val moved = bumped(path.substring(0, cut), delta) ?: return null
        return uri.buildUpon().encodedPath(moved + path.substring(cut)).build()
    }

    /**
     * Il numero nella coda dell'indirizzo, scegliendo quale con due regole in fila.
     *
     * ⚠️ **Con due numeri e nessun nome riconosciuto il criterio passa**: in `?id=4821&w=800`
     * cambiare quello sbagliato darebbe la stessa immagine larga 801 pixel, un doppione
     * presentato come vicina.
     * Si lavora sulla coda codificata e si ricuce a mano: `Uri.Builder` riordina e ricodifica
     * tutto, e un indirizzo firmato (le firme sono spesso nella coda) non resisterebbe.
     */
    private fun onQuery(uri: Uri, delta: Int): Uri? {
        val query = uri.encodedQuery ?: return null
        val parts = query.split('&')
        val numeric = parts.indices.filter { at ->
            val value = parts[at].substringAfter('=', "")
            value.isNotEmpty() && value.all { it.isDigit() }
        }
        if (numeric.isEmpty()) return null
        val chosen = numeric.firstOrNull { parts[it].substringBefore('=').lowercase() in SEQUENCE_KEYS }
            ?: numeric.singleOrNull()
            ?: return null
        val name = parts[chosen].substringBefore('=')
        val moved = bumped(parts[chosen].substringAfter('='), delta) ?: return null
        val fresh = parts.toMutableList().also { it[chosen] = "$name=$moved" }
        return uri.buildUpon().encodedQuery(fresh.joinToString("&")).build()
    }

    /**
     * L'ultimo numero di [text] spostato di [delta], conservando gli zeri davanti.
     *
     * ⚠️ **Da `007` si va a `008`, non a `8`**: su un server sono due file diversi. La
     * larghezza si conserva finché il numero vi entra, poi cresce: da `999` a `1000`.
     * Sotto zero non si va: `foto_-1.jpg` non è mai un file.
     */
    private fun bumped(text: String, delta: Int): String? {
        val hit = DIGITS.findAll(text).lastOrNull() ?: return null
        val value = hit.value.toLongOrNull() ?: return null
        val moved = value + delta
        if (moved < 0) return null
        val written = moved.toString().padStart(hit.value.length, '0')
        return text.substring(0, hit.range.first) + written + text.substring(hit.range.last + 1)
    }

    /**
     * La lettera in coda al nome spostata di [delta]: `tavola_a` -> `tavola_b`.
     *
     * Solo dove non c'è nessuna cifra, e solo dopo un `_` o un `-`: senza queste due condizioni
     * `tramonto.jpg` diventerebbe `tramontp.jpg`. È la convenzione delle scansioni a due pagine
     * e dei fumetti.
     * Dall'ultima lettera dell'alfabeto non si prosegue: `_z` non diventa `_aa`.
     */
    private fun lettered(stem: String, delta: Int): String? {
        if (stem.length < 3 || stem.any { it.isDigit() }) return null
        if (stem[stem.length - 2] !in HINGES) return null
        val last = stem.last()
        val moved = last + delta
        val ok = (last in 'a'..'z' && moved in 'a'..'z') || (last in 'A'..'Z' && moved in 'A'..'Z')
        return if (ok) stem.dropLast(1) + moved else null
    }

    // ── L'indice della cartella ─────────────────────────────────────────────

    /**
     * La serie intera, letta dall'elenco che il server dà per la cartella.
     *
     * ⚠️ **L'immagine aperta deve comparire nell'elenco, o il criterio passa**: all'indirizzo
     * di una cartella un server può rispondere anche con la pagina di copertina del sito, che
     * ha immagini sue (loghi, miniature di altri articoli). Se fra quelle non c'è l'immagine
     * aperta, l'elenco non è il suo.
     * Niente lettore di HTML: si raccolgono i collegamenti, e una libreria in più nell'APK
     * farebbe la stessa riga.
     * L'ordine è quello naturale dei nomi: un indice non ordinato farebbe venire `foto10`
     * prima di `foto9`.
     */
    private fun fromIndex(uri: Uri): Folder.Lookup.Found? {
        val path = uri.encodedPath ?: return null
        val cut = path.lastIndexOf('/')
        if (cut < 0) return null
        val parent = uri.buildUpon()
            .encodedPath(path.substring(0, cut + 1))
            .encodedQuery(null)
            .fragment(null)
            .build()
        val page = fetch(parent) ?: return null
        val here = uri.toString()
        val items = LINKS.findAll(page)
            .mapNotNull { resolved(parent.toString(), it.groupValues[1]) }
            .filter { ImageActions.looksLikeImage(Uri.parse(it)) }
            .distinct()
            .sortedWith { one, other -> naturalCompare(fileName(one), fileName(other)) }
            .toList()
        val index = items.indexOf(here)
        if (index < 0 || items.size < 2) return null
        return Folder.Lookup.Found(Folder.Series(items.map(Uri::parse), index))
    }

    /** Il collegamento reso assoluto, che in un indice è quasi sempre relativo. */
    private fun resolved(base: String, href: String): String? = try {
        java.net.URI(base).resolve(href).toString()
    } catch (e: Exception) {
        null
    }

    /** L'ultimo pezzo dell'indirizzo, che è quello su cui si ordina. */
    private fun fileName(address: String): String =
        address.substringBefore('?').substringBefore('#').substringAfterLast('/')

    /**
     * Una pagina, come testo, fino a un tetto ([PAGE_CAP]).
     *
     * All'indirizzo può rispondere qualunque cosa, anche un file da cento megabyte: se i
     * collegamenti non sono nel primo mezzo megabyte, quella non è la pagina cercata.
     */
    private fun fetch(uri: Uri): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = open(uri).apply { setRequestProperty("Accept", "text/html,*/*;q=0.8") }
            if (connection.responseCode !in 200..299) return null
            val room = ByteArray(PAGE_CAP)
            val read = connection.inputStream.use { fill(it, room) }
            if (read <= 0) null else String(room, 0, read, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    // ── La verifica ─────────────────────────────────────────────────────────

    /**
     * Una domanda secca al server: sì, no, oppure **null** che vuol dire 'non risponde'.
     *
     * I tre casi che contano, e perché sono trattati così:
     * - **2xx con un tipo che comincia per `image/`**: esiste, ed è l'unico sì pieno.
     * - **2xx con `text/html`**: è la pagina d'errore travestita da successo, e capita spesso;
     *   senza il controllo sul tipo la strisciata aprirebbe pagine HTML come immagini.
     * - **405 o 501**: il server rifiuta il metodo, non l'indirizzo, e un no qui spegnerebbe la
     *   strisciata su un sito intero. Si guardano i primi byte.
     */
    private fun probe(uri: Uri): Boolean? {
        var connection: HttpURLConnection? = null
        return try {
            connection = open(uri).apply { requestMethod = "HEAD" }
            when (connection.responseCode) {
                in 200..299 -> connection.contentType.orEmpty().startsWith("image/", true)
                405, 501 -> sniff(uri)
                else -> false
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * I primi byte, per i server che non rispondono a HEAD.
     *
     * Si chiede un pezzetto con `Range`, non il file intero; un server che ignora `Range`
     * risponde tutto, ma si leggono i primi byte e si chiude.
     * La firma nei byte decide dopo il tipo dichiarato: alcuni server mandano
     * `application/octet-stream` per qualunque file.
     */
    private fun sniff(uri: Uri): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            connection = open(uri).apply { setRequestProperty("Range", "bytes=0-${SNIFF - 1}") }
            if (connection.responseCode !in 200..299) return false
            val declared = connection.contentType.orEmpty()
            if (declared.startsWith("image/", true)) return true
            if (declared.isNotEmpty() && !declared.startsWith("application/octet-stream", true)) {
                return false
            }
            val head = ByteArray(SNIFF)
            val read = connection.inputStream.use { fill(it, head) }
            signed(head, read)
        } catch (e: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }

    /** Riempie quanto può, perché un flusso di rete dà quello che ha sotto mano. */
    private fun fill(stream: InputStream, into: ByteArray): Int {
        var done = 0
        while (done < into.size) {
            val step = stream.read(into, done, into.size - done)
            if (step <= 0) break
            done += step
        }
        return done
    }

    /**
     * Se questi primi byte sono la testa di un'immagine.
     *
     * Le firme sono quelle dei formati che il decodificatore sa aprire, e non una di più:
     * riconoscere un formato che poi non si apre sposterebbe l'errore.
     */
    private fun signed(head: ByteArray, size: Int): Boolean {
        fun at(index: Int): Int = if (index < size) head[index].toInt() and 0xFF else -1
        return when {
            at(0) == 0xFF && at(1) == 0xD8 && at(2) == 0xFF -> true
            at(0) == 0x89 && at(1) == 0x50 && at(2) == 0x4E && at(3) == 0x47 -> true
            at(0) == 0x47 && at(1) == 0x49 && at(2) == 0x46 -> true
            at(0) == 0x42 && at(1) == 0x4D -> true
            at(0) == 0x52 && at(1) == 0x49 && at(8) == 0x57 && at(9) == 0x45 -> true
            at(4) == 0x66 && at(5) == 0x74 && at(6) == 0x79 && at(7) == 0x70 -> true
            else -> false
        }
    }

    /**
     * La connessione, con le stesse intestazioni del caricatore vero.
     *
     * ⚠️ Stesso `User-Agent` e stesso `Accept` di [ImageSource]: un server che risponde
     * diversamente a due clienti direbbe sì alla verifica e no al caricamento.
     */
    private fun open(uri: Uri): HttpURLConnection =
        (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            connectTimeout = PROBE_MS
            readTimeout = PROBE_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept", ACCEPT)
            setRequestProperty("User-Agent", AGENT)
        }

    private val SCHEMES = setOf("http", "https")
    private val DIGITS = Regex("""\d+""")
    private val HINGES = setOf('_', '-')
    private val LINKS = Regex("""href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    /**
     * `src` e `href` insieme, per [fromPage].
     *
     * ⚠️ **Un'espressione sola**: due ricerche separate darebbero prima tutti gli `href` e poi
     * tutti gli `src`, perdendo l'ordine del documento. L'ordine dell'alternativa non cambia
     * quello dei risultati (`findAll` scorre il testo).
     * Il `[\w-]*` prende anche `data-src` e simili: i siti che caricano le immagini durante lo
     * scorrimento mettono là l'indirizzo vero.
     */
    private val SOURCES = Regex(
        """(?:[\w-]*src|href)\s*=\s*["']([^"']+)["']""",
        RegexOption.IGNORE_CASE
    )

    /** I nomi che in una coda significano 'quale della serie'. */
    private val SEQUENCE_KEYS = setOf("id", "page", "p", "n", "num", "index", "offset", "start")

    private const val PROBE_MS = 8_000
    private const val SNIFF = 64
    private const val PAGE_CAP = 512 * 1024
    private const val ACCEPT = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8"
    private const val AGENT = "Mozilla/5.0 (Android) AIV"
}

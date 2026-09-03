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
 * Come si sfoglia una fotografia che sta sul Web, dove non esiste nessuna cartella.
 *
 * ⚠️⚠️ **AIV riceve l'indirizzo di UNA immagine, mai quello della pagina che la
 * conteneva, ed è il fatto che regge tutto questo file.** Non c'è un genitore da
 * risalire come nel MediaStore e non c'è un elenco da leggere: la vicina si può solo
 * **indovinare** trasformando l'indirizzo, e poi **verificare** che esista davvero. Da
 * qui vengono le due scelte che sembrano complicazioni e non lo sono: la scala di
 * criteri, e la domanda di rete prima che il dito possa rispondere.
 *
 * ⚠️⚠️ **PRIMA LA VERIFICA** (decisione dell'utente, 2026-08-29): quando la vicina non
 * esiste, la strisciata **non fa niente** invece di aprirla e mostrare un errore al posto
 * di una fotografia. Il prezzo è dichiarato, ed è una domanda di rete: per questo la si
 * fa **in anticipo**, appena la corrente è pronta, e non al momento del gesto, o il dito
 * aspetterebbe la rete.
 *
 * ⚠️ **La serie del Web è LARGA TRE e si rifà a ogni passo**, mentre quella di una
 * cartella nasce intera e non si tocca più. Non è una svista: una serie remota non ha
 * fine nota, e costruirla tutta vorrebbe dire interrogare il server finché non risponde
 * di no, cioè decine di richieste per un gesto che forse nessuno farà. Tre indirizzi
 * bastano a far funzionare tutto il resto della macchina, che di una serie sa solo che è
 * una lista con un indice.
 */
object WebSeries {

    /**
     * I criteri, **nell'ordine in cui si provano**: dal più certo e gratuito al più largo.
     *
     * ⚠️⚠️ **Il primo che verifica VINCE e si blocca**: da lì in poi quell'indirizzo usa
     * sempre quel criterio. Senza il blocco la cascata si ripercorrerebbe a ogni
     * strisciata, e una serie che il primo criterio non copre pagherebbe otto domande di
     * rete per ogni fotografia invece di due.
     * ⚠️ **L'ordine non è estetico**: il nome del file vince sul percorso perché è più
     * vicino all'immagine, e la lettera viene per ultima perché è la più rara e la più
     * facile da confondere con l'ultima lettera di una parola.
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
         * ⚠️⚠️ **È l'unico che smette di indovinare e CHIEDE**, e per questo dà una serie
         * **vera**: ordinata, finita, e senza una domanda di rete per ogni vicina. Molti
         * server rispondono all'indirizzo di una cartella con l'elenco dei suoi file, ed è
         * lì che si trova quello che i quattro criteri sopra possono solo dedurre.
         * ⚠️ Viene per ultimo perché è il primo che **costa una pagina scaricata** invece
         * di una domanda secca: ci si arriva solo quando i gratuiti hanno fallito.
         */
        FOLDER_INDEX,

        /**
         * Le immagini della **pagina** che si è aperta: il sesto gradino della cascata.
         *
         * ⚠️⚠️ **NON STA NELLA CASCATA, e non è una dimenticanza**: gli altri cinque
         * partono dall'indirizzo di **un'immagine** e cercano la vicina, questo parte
         * dall'indirizzo di una **pagina**, che è un altro ingresso e non un altro
         * tentativo. Per questo [around] lo salta ([GUESSED] è la lista che percorre) e ci
         * si arriva solo da [fromPage]. Stava scritto così anche nella proposta approvata
         * il 2026-08-31: *prima va cambiato il modo di condividere verso l'app, è una
         * decisione a sé, non un gradino di questa scala*.
         * ⚠️⚠️ **LA PORTA D'INGRESSO NON È NUOVA, ed è la ragione per cui questo gradino si
         * è potuto fare adesso** (istruzione dell'utente, 2026-09-03: *inizia a
         * lavorarci*): il dialogo 'Apri un indirizzo' esiste dalla `0.41`, e un indirizzo
         * che non porta a un'immagine là dentro era un vicolo cieco (*L'URL non punta a
         * un'immagine*, con l'esempio `esempio.it/pagina` scritto nel commento). Adesso
         * quel vicolo prova la pagina. ⚠️ **Niente filtro nuovo nel manifesto e nessun
         * bersaglio di condivisione in più**: la strada che l'avrebbe voluto è quella che
         * l'utente ha rifiutato (voce `brave-link`, *dovrebbe accettare qualunque testo
         * condiviso* -> **no**), e con lei AIV comparirebbe nel pannello di ogni frase
         * condivisa.
         * ⚠️ Come [FOLDER_INDEX] dà la serie **intera**, quindi non si rifà a ogni passo.
         */
        PAGE_LINKS
    }

    /**
     * I criteri che [around] percorre: tutti tranne quelli che partono da una pagina.
     *
     * ⚠️ Si ricava per **esclusione** e non elencando i cinque: così un criterio nuovo
     * entra nella cascata da sé, che è il caso normale, e chi ne aggiunge uno che parte da
     * altro deve nominarlo qui, cioè accorgersene.
     */
    private val GUESSED = Rule.entries - Rule.PAGE_LINKS

    /**
     * I criteri che danno la serie **intera**, e per cui non si rifà la finestra a ogni
     * passo: rifarla vorrebbe dire riscaricare la stessa pagina a ogni fotografia.
     */
    val WHOLE = setOf(Rule.FOLDER_INDEX, Rule.PAGE_LINKS)

    /** La finestra costruita attorno a un indirizzo, col criterio che l'ha prodotta. */
    data class Window(val lookup: Folder.Lookup, val rule: Rule?)

    /** Se questo indirizzo vive sulla rete, cioè se questo file lo riguarda. */
    fun isWeb(uri: Uri): Boolean = uri.scheme?.lowercase() in SCHEMES

    /**
     * Lo stesso indirizzo in sicuro, perché in chiaro non si aprirebbe affatto.
     *
     * ⚠️⚠️ **Android BLOCCA il traffico in chiaro per impostazione predefinita** da
     * `targetSdk 28` in su, e qui non c'è nessun `usesCleartextTraffic` né configurazione
     * di rete: un indirizzo `http://` arriva fino al caricatore e si ferma lì. Il manifesto
     * però dichiara i filtri per `http`, quindi l'app **si offre** di aprire una cosa che
     * non sa aprire, ed è un difetto che nessuno vedeva.
     * ⚠️ **La correzione è riscrivere, non aprire il chiaro** (decisione dell'utente,
     * 2026-08-31): quasi tutti i siti oggi rispondono anche in sicuro o reindirizzano da
     * soli, quindi la riscrittura recupera quasi tutti quei collegamenti; permettere il
     * traffico in chiaro nel manifesto varrebbe invece per gli indirizzi di chiunque, e
     * questa app apre gli indirizzi che le passa il mondo.
     * ⚠️ Chi resta in chiaro per davvero non riceve un errore muto: il modello sa di aver
     * riscritto, e lo dice.
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
     * [locked] è il criterio già scelto per questa serie, quando ce n'è uno: passandolo si
     * salta la cascata, che è tutto il senso del blocco.
     *
     * ⚠️ Le due verifiche partono **insieme**: sono due attese di rete indipendenti, e
     * metterle in fila raddoppierebbe il tempo prima che la strisciata risponda.
     * ⚠️ Un criterio che non produce nemmeno un candidato **non costa niente** e si salta
     * senza toccare la rete: è la ragione per cui la cascata può essere lunga.
     */
    suspend fun around(uri: Uri, locked: Rule? = null): Window = withContext(Dispatchers.IO) {
        val rules = if (locked != null) listOf(locked) else GUESSED
        for (rule in rules) {
            // ⚠️ L'indice non produce una vicina: produce la serie intera, quindi salta
            // tutto il giro dei candidati e delle due verifiche.
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
        // ⚠️ `Alone` e non `Lost`: nessun criterio ha trovato una vicina, che è
        // esattamente 'questa fotografia non ha nessuno accanto', cioè il caso che la
        // riga dei dettagli e la strisciata sanno già trattare.
        Window(Folder.Lookup.Alone, null)
    }

    /**
     * L'indirizzo che verrebbe spostandosi di [delta] posti secondo [rule], se ha senso.
     *
     * È una funzione **pura**: non tocca la rete e non sa se quello che produce esista.
     * Serve così, perché è l'unica parte che si può leggere e correggere guardandola.
     */
    fun candidate(uri: Uri, rule: Rule, delta: Int): Uri? = when (rule) {
        Rule.NAME_NUMBER -> onFileName(uri) { stem -> bumped(stem, delta) }
        Rule.PATH_NUMBER -> onFolderPath(uri, delta)
        Rule.QUERY_NUMBER -> onQuery(uri, delta)
        Rule.NAME_LETTER -> onFileName(uri) { stem -> lettered(stem, delta) }
        // ⚠️ I due che danno la serie intera ([WHOLE]) non hanno un vicino da calcolare:
        // la loro serie nasce in [fromIndex] e in [fromPage]. Questi due rami esistono
        // perché il `when` sia completo invece di avere un `else` che un domani
        // inghiottirebbe un criterio nuovo.
        Rule.FOLDER_INDEX, Rule.PAGE_LINKS -> null
    }

    // ── La pagina che conteneva l'immagine ──────────────────────────────────

    /**
     * Le immagini di una **pagina**, nell'ordine in cui vi compaiono.
     *
     * ⚠️⚠️ **L'ORDINE È QUELLO DEL DOCUMENTO e non quello naturale dei nomi**, al contrario
     * di [fromIndex], e la differenza è il senso del gradino: l'indice di una cartella è un
     * elenco che il server genera, quindi l'ordine vero è quello dei numeri nei nomi; una
     * pagina invece è **impaginata da qualcuno**, e quell'ordine è l'informazione che si
     * sta andando a prendere. Riordinare per nome butterebbe via la sola cosa che una
     * pagina sa e una cartella no.
     * ⚠️ **Si guardano `src` e `href` insieme**: le miniature stanno negli `img` e le
     * versioni grandi nei collegamenti che le avvolgono, e prendere solo gli uni darebbe
     * una galleria di francobolli. La `distinct` tiene la **prima** apparizione, cioè
     * l'ordine in cui la pagina le presenta.
     * ⚠️⚠️ **NIENTE LETTORE DI HTML, come in [fromIndex]**: qui non si interpreta una
     * pagina, si raccolgono due attributi. Un lettore vero sarebbe una libreria in più
     * nell'APK per fare le stesse due espressioni.
     * ⚠️ **Serve almeno una immagine, non due**: una pagina con una sola immagine grande è
     * il caso di quasi tutti i siti di scansioni, e aprirla è già quello che si voleva. È
     * il contrario di [fromIndex], che ne vuole due perché là una sola vorrebbe dire
     * 'l'elenco non è il suo'.
     * ⚠️ **Il tetto di lettura è quello di [fetch]**, mezzo megabyte: se le immagini non
     * sono nella prima parte del documento, quella non è una galleria.
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
     * ⚠️⚠️ **Un solo secondo tentativo, e poi è no.** La rete che non risponde non è la
     * rete che dice di no, ma per la strisciata le due cose devono finire allo stesso
     * posto: la decisione presa è che senza una risposta il gesto non fa niente, e un
     * 'forse' ripetuto all'infinito sarebbe un dito che aspetta.
     * ⚠️ **La funzione gemella [ImageActions.leadsToImage] davanti a un errore di rete
     * risponde SÌ**, ed è giusto per un indirizzo che una persona ha digitato: quella
     * concede il beneficio del dubbio a una richiesta esplicita. Qui il dubbio va
     * dall'altra parte, perché questa vicina non l'ha chiesta nessuno. Stessa forma,
     * verdetto opposto, e va tenuto così.
     */
    suspend fun exists(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        probe(uri) ?: probe(uri) ?: false
    }

    // ── I criteri, uno per uno ──────────────────────────────────────────────

    /**
     * Applica [change] al nome del file, lasciando stare percorso ed estensione.
     *
     * ⚠️⚠️ **L'estensione si stacca PRIMA di cercare il numero**, e non è pignoleria:
     * esistono estensioni con una cifra dentro (`.jp2`, `.mp4`), e senza questo taglio
     * l'ultimo numero del nome sarebbe quello, cioè si sfoglierebbe cambiando formato.
     * ⚠️ Si lavora sul percorso **codificato**: le cifre non si codificano mai, quindi il
     * conto torna lo stesso, e tutto ciò che sta attorno resta byte per byte com'era.
     * Passando dal percorso decodificato, un indirizzo con uno spazio o un accento
     * tornerebbe indietro riscritto.
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
     * ⚠️ L'esclusione è ciò che tiene separati i due criteri: senza, questo troverebbe
     * sempre il numero del nome del file e il primo criterio non servirebbe a niente.
     * Serve ai siti che numerano la pagina invece del file.
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
     * ⚠️⚠️ **Con due numeri e nessun nome riconosciuto il criterio PASSA**, invece di
     * tirare a indovinare: in una coda come `?id=4821&w=800` cambiare quello sbagliato
     * darebbe la stessa fotografia larga 801 pixel, cioè un doppione presentato come una
     * vicina. Meglio lasciar provare il criterio dopo.
     * ⚠️ Si lavora sulla coda **codificata** e si ricuce a mano invece di ricostruirla coi
     * parametri: `Uri.Builder` rimette in fila e ricodifica tutto, e un indirizzo firmato
     * (le firme stanno spesso nella coda) non sopravvive a una riscrittura.
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
     * L'ultimo numero di [text] spostato di [delta], **conservando gli zeri davanti**.
     *
     * ⚠️⚠️ **Il riempimento di zeri è la parte che sbaglia chi va di fretta**: da `007` si
     * va a `008` e non a `8`, perché su un server quei due sono due file diversi e il
     * secondo non esiste. La larghezza si conserva finché il numero ci sta dentro, e
     * quando cresce di una cifra si lascia crescere: da `999` a `1000`.
     * ⚠️ Sotto zero non si va: `foto_0.jpg` all'indietro non produce `foto_-1.jpg`, che
     * non è mai un file.
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
     * ⚠️ **Solo dove non c'è nessuna cifra**, e solo dopo un `_` o un `-`: senza queste due
     * condizioni il criterio prenderebbe l'ultima lettera di una parola qualunque, e
     * `tramonto.jpg` diventerebbe `tramontp.jpg`. È la convenzione delle scansioni a due
     * pagine e dei fumetti, non una regola generale sui nomi.
     * ⚠️ Dall'ultima lettera dell'alfabeto non si prosegue: `_z` non diventa `_aa`, che
     * sarebbe una convenzione inventata da noi.
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
     * ⚠️⚠️ **La fotografia aperta DEVE comparire nell'elenco, o il criterio passa**, ed è
     * questa condizione a rendere sicuro tutto il resto. All'indirizzo di una cartella un
     * server può rispondere con l'elenco dei file, ma può anche rispondere con la pagina
     * di copertina del sito, che di immagini ne ha (i loghi, le miniature di altri
     * articoli) e non c'entrano niente. Se fra quelle non c'è quella che si sta
     * guardando, l'elenco non è il suo, e sfogliarlo porterebbe altrove.
     * ⚠️ **Niente lettore di HTML**, e non è pigrizia: qui non si interpreta una pagina, si
     * raccolgono i suoi collegamenti, che è l'unica cosa che serve. Un lettore vero
     * sarebbe una libreria in più nell'APK per fare la stessa riga.
     * ⚠️ L'ordine è quello **naturale** e non quello in cui i collegamenti compaiono: un
     * indice generato dal server è quasi sempre già ordinato, ma quando non lo è la
     * sequenza salterebbe, e `foto10` verrebbe prima di `foto9`.
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
     * La pagina della cartella, come testo, **fino a un tetto**.
     *
     * ⚠️ Il tetto c'è perché all'indirizzo di una cartella può rispondere qualunque cosa,
     * compreso un file da cento megabyte: qui si stanno cercando dei collegamenti, e se
     * non sono nei primi mezzo megabyte quella non è la pagina che cerchiamo.
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
     * - **2xx con `text/html`**: è la pagina d'errore travestita da successo, e capita
     *   spesso. Senza il controllo sul tipo, ogni indirizzo di quei server risponderebbe
     *   di sì e la strisciata aprirebbe pagine HTML come se fossero fotografie.
     * - **405 o 501**: il server rifiuta il metodo, non l'indirizzo, e dire di no qui
     *   spegnerebbe lo sfogliatore su un sito intero. Si guardano i primi byte.
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
     * ⚠️ Si chiede un **pezzetto** con `Range`, non il file: verificare l'esistenza di una
     * vicina scaricando una fotografia intera sarebbe più costoso della fotografia che si
     * sta guardando. Un server che ignora `Range` risponde comunque tutto, ma noi
     * leggiamo i primi byte e chiudiamo.
     * ⚠️ **La firma nei byte è l'ultima parola**, dopo il tipo dichiarato: alcuni server
     * mandano `application/octet-stream` per qualunque file, e su quelli il tipo non
     * distingue una fotografia da un archivio.
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
     * Se questi primi byte sono la testa di una fotografia.
     *
     * Le firme sono quelle dei formati che il decodificatore sa aprire, e non una di più:
     * riconoscere un formato che poi non si apre sposterebbe l'errore, non lo toglierebbe.
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
     * diversamente a due clienti diversi direbbe di sì alla verifica e di no al
     * caricamento, e il difetto sarebbe invisibile da qui.
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
     * ⚠️ **Una espressione sola e non due passate**: l'ordine del documento è quello che
     * questo gradino va a prendere, e due ricerche separate darebbero prima tutti gli
     * `href` e poi tutti gli `src`, cioè un ordine inventato da noi. ⚠️ **`src` prima di
     * `href` nell'alternativa** non cambia l'ordine dei risultati (`findAll` scorre il
     * testo), e serve solo a leggersi come 'l'immagine, o il collegamento che la avvolge'.
     * ⚠️ Prende anche `data-src` e simili per via del `[\w-]*`: i siti che caricano le
     * immagini a scorrimento mettono l'indirizzo vero là, e senza quel pezzo una galleria
     * moderna risponderebbe 'nessuna immagine'.
     */
    private val SOURCES = Regex(
        """(?:[\w-]*src|href)\s*=\s*["']([^"']+)["']""",
        RegexOption.IGNORE_CASE
    )

    /** I nomi che in una coda significano 'quale della serie', in ordine di quanto sono chiari. */
    private val SEQUENCE_KEYS = setOf("id", "page", "p", "n", "num", "index", "offset", "start")

    private const val PROBE_MS = 8_000
    private const val SNIFF = 64
    private const val PAGE_CAP = 512 * 1024
    private const val ACCEPT = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8"
    private const val AGENT = "Mozilla/5.0 (Android) AIV"
}

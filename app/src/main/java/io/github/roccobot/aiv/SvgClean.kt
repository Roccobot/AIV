package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * La pulizia di un SVG **dentro l'app**, senza rete e senza browser.
 *
 * ⚠️⚠️ **È LA VARIANTE B, scelta dall'utente fra tre** (2026-09-02, dopo l'analisi che gli era
 * stata presentata: *la mia raccomandazione: B, con l'interfaccia che dice la verità e il
 * collegamento a CleanSVG che resta per il lavoro completo*), e le altre due erano: **A**, una
 * WebView con la pagina CleanSVG e SVGO dentro l'APK, e **C**, come prima, cioè il solo
 * collegamento.
 * ⚠️⚠️ **QUINDI QUESTA NON RISCRIVE I TRACCIATI, e l'interfaccia lo dice invece di lasciarlo
 * capire**: quello lo fa SVGO, che è la libreria vera di CleanSVG, pesa 805.016 byte non
 * compressi e pubblica **una versione al mese**. Dentro l'APK sarebbe finita una copia
 * congelata di quella libreria e della pagina, da riallineare a mano a ogni rilascio: due
 * fonti di verità, che è la trappola che le nostre regole vietano da mesi.
 * ⚠️⚠️ **E SUI FILE DELL'UTENTE IL GRASSO NON SONO I TRACCIATI, ED È MISURATO**: nel suo
 * `imageCopyNew1.svg`, 90.964 byte, il **disegno** sta in 693 e il 99,2% è il manifest C2PA.
 * Il '99%' che CleanSVG promette lo porta la rifinitura, non SVGO, che aggiunge qualche punto
 * percentuale su quello che resta.
 *
 * ⚠️⚠️ **IL CUORE È IL PORTO DI `rifinisci()` DELLA PAGINA, riga per riga** (vive in
 * `roccobot.github.io/CleanSVG/index.html`), e i due si devono poter confrontare: stessi 25
 * URI di namespace, stessi tre elementi, stesso ordine dei passi (commenti, elementi,
 * attributi, dichiarazioni `xmlns` rimaste appese), stesse sette famiglie del conto. Chi
 * cambia una delle due liste cambi anche l'altra, o i due strumenti cominciano a dare due
 * risultati diversi sullo stesso file.
 *
 * ⚠️⚠️ **UN SVG È INPUT NON FIDATO, e il parser va disarmato**: un file può dichiarare
 * un'entità esterna e farsi leggere un file del telefono o aprire una connessione (XXE). Qui
 * la lettura del DTD esterno e le entità esterne sono **spente** e la lavorazione è in modo
 * sicuro. ⚠️ **Il DOCTYPE interno resta ammesso** e non è una dimenticanza: i file vecchi di
 * Illustrator e Inkscape ne portano uno, e rifiutarli vorrebbe dire non pulire proprio quelli
 * che ne hanno più bisogno.
 */
object SvgClean {

    /**
     * Che cosa è stato tolto, per famiglia.
     *
     * ⚠️ **Le sette famiglie sono quelle della pagina, e restano sette**: gli attributi degli
     * editor si contano a parte dai gestori `on...` perché fusi in una voce sola un file di
     * Inkscape senza una riga di codice dichiarava 'sei script', cioè un allarme al posto di
     * un'informazione. La nota sta anche là, su `contoNuovo`.
     * ⚠️ **`foreignObject` si conta fra gli script**, come nella pagina: è la porta con cui un
     * SVG ospita HTML, quindi sta con quello che esegue e non coi metadati.
     */
    data class Count(
        var metadati: Int = 0,
        var commenti: Int = 0,
        var editor: Int = 0,
        var script: Int = 0,
        var namespace: Int = 0,
        var attributi: Int = 0,
        var gestori: Int = 0
    ) {
        val total: Int get() = metadati + commenti + editor + script + namespace + attributi + gestori
    }

    sealed interface Result {
        /** Pulito e scritto: `before` e `after` sono i byte del file, non del testo in memoria. */
        data class Done(val count: Count, val before: Long, val after: Long) : Result

        /** Il file era già pulito: non si riscrive niente, e non è un errore. */
        data object Nothing : Result

        data class Failed(@StringRes val why: Int) : Result
    }

    /**
     * Legge, ripulisce e **sovrascrive**, con la copia di prima nel cestino.
     *
     * ⚠️⚠️ **NIENTE SOVRASCRITTURA SILENZIOSA, ed è la regola del backup dell'editor**: se
     * l'interruttore è acceso (di fabbrica lo è) la versione di prima finisce nel cestino
     * **prima** che l'originale venga toccato, e una copia che non riesce **ferma** la
     * pulizia. Chi ha acceso quell'interruttore ha chiesto di non poter perdere l'originale.
     * ⚠️ **Se non c'è niente da togliere non si scrive**: riscrivere un file identico gli
     * cambierebbe la data e, con l'interruttore acceso, gli metterebbe una copia nel cestino
     * per niente.
     * ⚠️ **`NonCancellable` come le altre operazioni sui file**: a metà scrittura una
     * cancellazione lascerebbe un SVG troncato dove prima c'era un disegno.
     */
    suspend fun clean(context: Context, uri: Uri, backup: Boolean): Result =
        withContext(Dispatchers.IO + NonCancellable) {
            val file = FileTree.fileOf(context, uri)
                ?: return@withContext Result.Failed(R.string.edit_no_file)
            val before = file.length()
            val grezzo = runCatching { file.readBytes() }.getOrNull()
                ?: return@withContext Result.Failed(R.string.clean_failed)
            /*
             * ⚠️⚠️ **UN `.svgz` È UN SVG IN UN INVOLUCRO GZIP, e l'app li apre**: senza questo
             * ramo la pulizia di uno di loro leggerebbe byte compressi come testo e
             * risponderebbe 'non è un SVG leggibile', che è vero della lettura e falso del
             * file. ⚠️ **Si riconosce dai due byte di firma e non dall'estensione**: un `.svg`
             * servito gzippato esiste, e un `.svgz` con dentro testo in chiaro pure.
             */
            val zippato = grezzo.size >= 2 &&
                grezzo[0] == 0x1F.toByte() && grezzo[1] == 0x8B.toByte()
            val testo = runCatching {
                if (zippato) {
                    GZIPInputStream(ByteArrayInputStream(grezzo)).use { it.readBytes() }
                        .toString(Charsets.UTF_8)
                } else {
                    grezzo.toString(Charsets.UTF_8)
                }
            }.getOrNull() ?: return@withContext Result.Failed(R.string.clean_failed)

            val count = Count()
            val pulito = runCatching { refine(testo, count) }.getOrNull()
                ?: return@withContext Result.Failed(R.string.clean_not_svg)
            if (count.total == 0) return@withContext Result.Nothing

            if (backup && Bin.keep(context, file) == null) {
                return@withContext Result.Failed(R.string.edit_no_backup)
            }
            // ⚠️ Rientra come è uscito: un `.svgz` ripulito e scritto in chiaro resterebbe un
            // file col nome sbagliato, e chi lo apre altrove si aspetta il gzip.
            val scritto = runCatching {
                if (zippato) {
                    file.outputStream().use { fuori ->
                        GZIPOutputStream(fuori).use { it.write(pulito.toByteArray()) }
                    }
                } else {
                    file.writeText(pulito)
                }
                true
            }.getOrDefault(false)
            if (!scritto) return@withContext Result.Failed(R.string.clean_failed)
            // ⚠️ La scansione come dopo ogni scrittura dell'editor: senza, il MediaStore
            // continua a dichiarare il peso di prima e la miniatura vecchia resta in cache.
            FileTree.scan(context, listOf(file.absolutePath))
            Result.Done(count, before, file.length())
        }

    /**
     * Il cuore: il documento ripulito, e [count] riempito con quello che è uscito.
     *
     * ⚠️ **Separata da [clean] perché è PURA**: entra un testo, esce un testo, e si può
     * provare senza un file, senza un contesto e senza un telefono.
     */
    fun refine(testo: String, count: Count): String {
        val doc = parse(testo)

        // I commenti, dovunque stiano, anche fuori dall'elemento radice.
        for (c in collect(doc) { it.nodeType == Node.COMMENT_NODE }) {
            count.commenti++
            c.parentNode?.removeChild(c)
        }

        // Gli elementi: quelli di un namespace da buttare, più metadati, script e foreignObject.
        val morituri = ArrayList<Pair<Element, String>>()
        for (n in doc.getElementsByTagName("*").list()) {
            val el = n as? Element ?: continue
            val nome = (el.localName ?: el.nodeName).lowercase()
            when {
                daTogliere(el.namespaceURI) -> morituri += el to "editor"
                el.namespaceURI == NS_SVG && nome in ELEMENTI_DA_TOGLIERE ->
                    morituri += el to if (nome == "metadata") "metadati" else "script"
            }
        }
        for ((el, famiglia) in morituri) {
            val padre = el.parentNode ?: continue
            when (famiglia) {
                "editor" -> count.editor++
                "metadati" -> count.metadati++
                else -> count.script++
            }
            padre.removeChild(el)
        }

        // Gli attributi: quelli di un namespace da buttare e i gestori `on...`, che in un file
        // di disegno non hanno niente da fare.
        for (n in doc.getElementsByTagName("*").list()) {
            val el = n as? Element ?: continue
            for (a in el.attributes.list()) {
                val nome = a.nodeName.lowercase()
                // ⚠️ Le dichiarazioni si trattano dopo, quando si sa quali sono ancora in uso.
                if (nome.startsWith("xmlns")) continue
                if (daTogliere(a.namespaceURI)) {
                    count.attributi++
                    el.removeAttributeNode(a as org.w3c.dom.Attr)
                } else if (nome.startsWith("on")) {
                    count.gestori++
                    el.removeAttributeNode(a as org.w3c.dom.Attr)
                }
            }
        }

        /*
         * ⚠️⚠️ **SI TOLGONO SOLO LE DICHIARAZIONI CHE NESSUNO USA PIÙ**: toglierne una ancora
         * in uso spezzerebbe il documento, e un file che non si apre più è il danno peggiore
         * che un pulitore possa fare. Il conto degli usi si rifà DOPO le due potature, perché
         * è proprio quello che le rende inutili.
         */
        val usati = HashSet<String>()
        for (n in doc.getElementsByTagName("*").list()) {
            val el = n as? Element ?: continue
            el.namespaceURI?.let { usati += it }
            for (a in el.attributes.list()) a.namespaceURI?.let { usati += it }
        }
        val radice = doc.documentElement
        for (a in radice.attributes.list()) {
            if (!a.nodeName.lowercase().startsWith("xmlns:")) continue
            if (a.nodeValue in usati) continue
            count.namespace++
            radice.removeAttributeNode(a as org.w3c.dom.Attr)
        }

        return serialize(doc)
    }

    /**
     * Il parser, disarmato. Vedi la nota in testa: un SVG è input non fidato.
     *
     * ⚠️ **`isNamespaceAware` è indispensabile e non un'opzione**: senza, `namespaceURI` è
     * `null` su tutto e questa pulizia, che lavora **per namespace**, non troverebbe niente.
     */
    private fun parse(testo: String): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
            for (nome in DISARMO) optional { setFeature(nome, false) }
            optional { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
        }
        return factory.newDocumentBuilder().apply {
            /*
             * ⚠️⚠️ **QUESTA È LA GARANZIA VERA, e le opzioni qui sopra sono un di più**: chi
             * risolve un'entità esterna è il risolutore, e un risolutore che risponde 'vuoto'
             * a tutti non può aprire né un file del telefono né una connessione. Non dipende
             * da nessuna opzione, quindi vale su qualunque parser, anche su uno che le rifiuta
             * tutte come quello di Android.
             */
            setEntityResolver { _, _ -> InputSource(StringReader("")) }
        }.parse(ByteArrayInputStream(testo.toByteArray()))
    }

    /**
     * Le tre opzioni di irrobustimento che si **provano**, perché non tutti i parser le hanno.
     *
     * ⚠️⚠️ **SU ANDROID NESSUNA DI LORO ESISTE, E PRIMA ERANO OBBLIGATORIE: È IL DIFETTO CHE
     * FACEVA RISPONDERE 'QUESTO FILE NON È UN SVG LEGGIBILE' A OGNI FILE** (riscontro
     * `pulitore`, 2026-09-02: *risponde sempre 'Questo file non è un SVG leggibile'*).
     * `DocumentBuilderFactoryImpl` di Android accetta **due soli** nomi di opzione,
     * `http://xml.org/sax/features/namespaces` e `.../validation`, e su qualunque altro
     * **solleva** `ParserConfigurationException`: letto nel sorgente AOSP di `libcore`
     * (`luni/src/main/java/org/apache/harmony/xml/parsers/DocumentBuilderFactoryImpl.java`,
     * il `throw` nel ramo `else` di `setFeature`), non ricordato. Quindi la prima riga del
     * vecchio blocco sollevava, `parse` non tornava mai, e il `runCatching` di [clean]
     * traduceva tutto in 'non è un SVG'.
     * ⚠️⚠️ **E NESSUNA PROVA SU MACCHINA LO AVREBBE PRESO**: sul JVM il parser è Xerces, che
     * quelle opzioni le ha tutte, quindi lo stesso codice passa. Il difetto vive **solo** dove
     * gira l'app, ed è la ragione per cui adesso la sicurezza non poggia più su di loro ma sul
     * risolutore di entità, che è portabile.
     * ⚠️ **Si provano lo stesso e non si cancellano**: dove esistono (un domani, o in una prova
     * sul JVM) chiudono la porta un passo prima del risolutore, e non costano niente.
     */
    private val DISARMO = listOf(
        "http://xml.org/sax/features/external-general-entities",
        "http://xml.org/sax/features/external-parameter-entities",
        "http://apache.org/xml/features/nonvalidating/load-external-dtd"
    )

    /**
     * Esegue [blocco] e **ingoia** il rifiuto di un parser che non conosce quell'opzione.
     *
     * ⚠️ **Ingoia solo questo**, e non un errore qualsiasi: un'opzione che manca è una
     * differenza fra parser, non un guasto, e la sicurezza non dipende da lei (vedi [DISARMO]).
     */
    private inline fun optional(blocco: () -> Unit) {
        runCatching(blocco)
    }

    /**
     * Il documento come testo.
     *
     * ⚠️ **Senza il prologo `<?xml ...?>`**, come fa `XMLSerializer` nella pagina: un SVG
     * autonomo è valido anche senza, e riaggiungerlo qui farebbe due file diversi dallo stesso
     * documento a seconda di dove lo si è pulito.
     * ⚠️ **Niente `indent`**: rientrare un documento pulito lo farebbe **crescere**, ed è
     * l'opposto di quello che questo comando promette.
     */
    private fun serialize(doc: Document): String {
        val writer = StringWriter()
        TransformerFactory.newInstance().apply {
            // ⚠️ `optional` per la stessa ragione del parser: un'opzione che una piattaforma
            // non conosce non deve far fallire la scrittura. Vedi [DISARMO].
            optional { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
        }.newTransformer().apply {
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            setOutputProperty(OutputKeys.INDENT, "no")
        }.transform(DOMSource(doc), StreamResult(writer))
        return writer.toString()
    }

    /**
     * I nodi che soddisfano [ok], raccolti **prima** di toccarli.
     *
     * ⚠️ **La lista si fa e poi si pota**: le raccolte del DOM sono vive, quindi togliere un
     * nodo mentre le si scorre salta il successivo. È lo stesso motivo per cui la pagina
     * raccoglie i commenti in un array prima di rimuoverli.
     */
    private fun collect(radice: Node, ok: (Node) -> Boolean): List<Node> {
        val fuori = ArrayList<Node>()
        fun scendi(n: Node) {
            if (ok(n)) fuori += n
            val figli = n.childNodes
            for (i in 0 until figli.length) scendi(figli.item(i))
        }
        scendi(radice)
        return fuori
    }

    /** La stessa istantanea per le due raccolte vive del DOM. Vedi [collect]. */
    private fun org.w3c.dom.NodeList.list(): List<Node> =
        (0 until length).mapNotNull { item(it) }

    private fun org.w3c.dom.NamedNodeMap.list(): List<Node> =
        (0 until length).mapNotNull { item(it) }

    private fun daTogliere(uri: String?): Boolean =
        uri != null && uri.lowercase() in NS_DA_TOGLIERE

    /**
     * I 25 namespace degli editor, copiati dalla pagina.
     *
     * ⚠️⚠️ **QUI DENTRO C'È ANCHE LA PROVENIENZA C2PA**, negli URI `ns.adobe.com/xap`: è XMP,
     * cioè il posto in cui una fotocamera o Photoshop scrivono chi ha fatto che cosa. Toglierla
     * **è una perdita**, non solo un alleggerimento, e per questo l'interfaccia lo dice invece
     * di nasconderlo in un conto di byte.
     */
    private val NS_DA_TOGLIERE = setOf(
        "http://sodipodi.sourceforge.net/dtd/sodipodi-0.0.dtd",
        "http://sodipodi.sourceforge.net/dtd/sodipodi-0.dtd",
        "http://inkscape.sourceforge.net/dtd/sodipodi-0.0.dtd",
        "http://www.inkscape.org/namespaces/inkscape",
        "http://ns.adobe.com/adobeillustrator/10.0/",
        "http://ns.adobe.com/adobesvgviewerextensions/3.0/",
        "http://ns.adobe.com/extensibility/1.0/",
        "http://ns.adobe.com/flows/1.0/",
        "http://ns.adobe.com/graphs/1.0/",
        "http://ns.adobe.com/imagereplacement/1.0/",
        "http://ns.adobe.com/saveforweb/1.0/",
        "http://ns.adobe.com/variables/1.0/",
        "http://ns.adobe.com/xap/1.0/",
        "http://ns.adobe.com/xap/1.0/mm/",
        "http://ns.adobe.com/xap/1.0/stype/resourceref#",
        "http://purl.org/dc/elements/1.1/",
        "http://creativecommons.org/ns#",
        "http://web.resource.org/cc/",
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
        "http://www.serif.com/",
        "http://boxy-svg.com/",
        "http://www.figma.com/figma/ns",
        "http://www.bohemiancoding.com/sketch/ns",
        "http://www.vector.evaxdesign.sk",
        "http://www.corel.com/coreldraw/"
    )

    /**
     * I tre elementi che escono anche se sono SVG di pieno diritto.
     *
     * ⚠️ **`title` e `desc` NON sono qui, e non è una dimenticanza**: sono **accessibilità**,
     * non scarto, e la pagina li tiene per la stessa ragione. Lo stesso vale per `viewBox`, che
     * nessuno tocca: un SVG senza viewBox smette di ridimensionarsi, cioè il modo più facile
     * di rovinare un file credendo di alleggerirlo.
     */
    private val ELEMENTI_DA_TOGLIERE = setOf("metadata", "script", "foreignobject")

    private const val NS_SVG = "http://www.w3.org/2000/svg"
}

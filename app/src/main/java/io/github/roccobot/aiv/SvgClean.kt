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
 * Variante B, scelta dall'utente fra tre (le altre: una WebView con CleanSVG e SVGO dentro
 * l'APK, o il solo collegamento a CleanSVG).
 * ⚠️ **Non riscrive i tracciati, e l'interfaccia lo dice**: quello lo fa SVGO, che pubblica una
 * versione al mese, e una sua copia nell'APK sarebbe una seconda fonte di verità da riallineare a
 * mano. Sui file dell'utente il peso è nei metadati: in un SVG di 90.964 byte il disegno ne
 * occupava 693, e il resto era il manifest C2PA.
 *
 * ⚠️⚠️ **È il porto di `rifinisci()` della pagina CleanSVG** (`CleanSVG/index.html`), e i due si
 * devono poter confrontare: stessi 25 URI di namespace, stessi tre elementi, stesso ordine dei
 * passi (commenti, elementi, attributi, dichiarazioni `xmlns` rimaste appese), stesse sette
 * famiglie del conto. Chi cambia una delle due liste cambi anche l'altra.
 *
 * ⚠️⚠️ **Un SVG è input non fidato, e il parser va disarmato**: un'entità esterna può far leggere
 * un file del telefono o aprire una connessione (XXE); vedi [parse]. Il DOCTYPE interno resta
 * ammesso, perché i file vecchi di Illustrator e Inkscape ne hanno uno.
 */
object SvgClean {

    /**
     * Che cosa è stato tolto, per famiglia.
     *
     * Le sette famiglie della pagina. Gli attributi degli editor si contano a parte dai gestori
     * `on...`: fusi, un file di Inkscape senza codice dichiarerebbe 'sei script' (la stessa nota
     * è sulla pagina, su `contoNuovo`). `foreignObject` si conta fra gli script, come nella
     * pagina: è la via con cui un SVG ospita HTML.
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
     * ⚠️⚠️ **Niente sovrascrittura silenziosa**, come il backup dell'editor: con l'interruttore
     * acceso (lo è di fabbrica) la versione di prima va nel cestino prima che l'originale sia
     * toccato, e una copia che non riesce ferma la pulizia.
     * Se non c'è niente da togliere non si scrive: cambierebbe la data e metterebbe una copia nel
     * cestino senza ragione. `NonCancellable` come le altre operazioni sui file.
     */
    suspend fun clean(context: Context, uri: Uri, backup: Boolean): Result =
        withContext(Dispatchers.IO + NonCancellable) {
            val file = FileTree.fileOf(context, uri)
                ?: return@withContext Result.Failed(R.string.edit_no_file)
            val before = file.length()
            val grezzo = runCatching { file.readBytes() }.getOrNull()
                ?: return@withContext Result.Failed(R.string.clean_failed)
            /*
             * ⚠️ **Un `.svgz` è un SVG in un involucro gzip**: senza questo ramo si leggerebbero
             * byte compressi come testo. Si riconosce dai due byte di firma e non
             * dall'estensione: esistono `.svg` gzippati e `.svgz` in chiaro.
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
            // ⚠️ Si riscrive nella forma in cui è stato letto: un `.svgz` in chiaro avrebbe il
            // nome sbagliato. Con [AtomicWrite], perché un'interruzione non lasci il documento
            // troncato; il gzip si chiude qui, prima, perché [AtomicWrite] vuole il flusso aperto
            // per il suo `sync`.
            val scritto = AtomicWrite.write(file) { fuori ->
                if (zippato) {
                    val zip = GZIPOutputStream(fuori)
                    zip.write(pulito.toByteArray())
                    zip.finish()
                } else {
                    fuori.write(pulito.toByteArray())
                }
            }
            if (!scritto) return@withContext Result.Failed(R.string.clean_failed)
            // La scansione come dopo ogni scrittura dell'editor: senza, il MediaStore dichiara
            // il peso di prima e la miniatura vecchia resta in cache.
            FileTree.scan(context, listOf(file.absolutePath))
            Result.Done(count, before, file.length())
        }

    /**
     * Il cuore: il documento ripulito, e [count] riempito con quello che è uscito.
     *
     * Separata da [clean] perché è pura: si prova senza file, contesto e telefono.
     */
    fun refine(testo: String, count: Count): String {
        val doc = parse(testo)

        // I commenti, dovunque siano, anche fuori dall'elemento radice.
        for (c in collect(doc) { it.nodeType == Node.COMMENT_NODE }) {
            count.commenti++
            c.parentNode?.removeChild(c)
        }

        // Gli elementi: quelli di un namespace da togliere, più metadati, script e foreignObject.
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

        // Gli attributi: quelli di un namespace da togliere e i gestori `on...`, che in un file
        // di disegno non servono.
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
         * ⚠️⚠️ **Si tolgono solo le dichiarazioni che nessuno usa più**: toglierne una in uso
         * renderebbe il documento illeggibile. Il conto degli usi si rifà dopo le due potature,
         * che sono quelle che le rendono inutili.
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
     * Il parser, disarmato: un SVG è input non fidato.
     *
     * ⚠️ **`isNamespaceAware` è indispensabile**: senza, `namespaceURI` è `null` su tutto, e
     * questa pulizia lavora per namespace.
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
             * ⚠️⚠️ **Questa è la garanzia vera, le opzioni sopra sono un di più**: un risolutore
             * che risponde 'vuoto' a tutto non apre né un file del telefono né una connessione,
             * su qualunque parser, anche quello di Android che rifiuta le opzioni.
             */
            setEntityResolver { _, _ -> InputSource(StringReader("")) }
        }.parse(ByteArrayInputStream(testo.toByteArray()))
    }

    /**
     * Le tre opzioni di irrobustimento che si provano, perché non tutti i parser le hanno.
     *
     * ⚠️⚠️ **Su Android nessuna esiste**: `DocumentBuilderFactoryImpl` di `libcore` accetta solo
     * `namespaces` e `validation`, e su qualunque altro nome va in errore con
     * `ParserConfigurationException`. Rese obbligatorie, ogni file risulterebbe 'non leggibile'.
     * Nessuna prova sul JVM lo vede, perché là il parser è Xerces e le ha tutte: per questo la
     * sicurezza poggia sul risolutore di entità, che è portabile. Dove esistono, chiudono la porta
     * un passo prima.
     */
    private val DISARMO = listOf(
        "http://xml.org/sax/features/external-general-entities",
        "http://xml.org/sax/features/external-parameter-entities",
        "http://apache.org/xml/features/nonvalidating/load-external-dtd"
    )

    /**
     * Esegue [blocco] e ignora il rifiuto di un parser che non conosce quell'opzione: è una
     * differenza fra parser, non un guasto, e la sicurezza non dipende da lei (vedi [DISARMO]).
     */
    private inline fun optional(blocco: () -> Unit) {
        runCatching(blocco)
    }

    /**
     * Il documento come testo.
     *
     * Senza il prologo `<?xml ...?>`, come `XMLSerializer` nella pagina: con, lo stesso documento
     * darebbe due file diversi secondo dove lo si è pulito. Niente `indent`, che farebbe crescere
     * il file.
     */
    private fun serialize(doc: Document): String {
        val writer = StringWriter()
        TransformerFactory.newInstance().apply {
            // `optional` come nel parser: un'opzione sconosciuta non fa fallire la scrittura.
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
     * ⚠️ **Prima si raccoglie, poi si pota**: le raccolte del DOM sono vive, e togliere un nodo
     * mentre le si scorre salta il successivo. La pagina fa lo stesso.
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
     * ⚠️ **Contiene anche la provenienza C2PA**, negli URI `ns.adobe.com/xap` (XMP, dove una
     * fotocamera o Photoshop scrivono chi ha fatto che cosa): toglierla è una perdita, e
     * l'interfaccia lo dice.
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
     * ⚠️ **`title` e `desc` restano**: sono accessibilità, e la pagina li tiene. Anche `viewBox`
     * non si tocca: senza, un SVG smette di ridimensionarsi.
     */
    private val ELEMENTI_DA_TOGLIERE = setOf("metadata", "script", "foreignobject")

    private const val NS_SVG = "http://www.w3.org/2000/svg"
}

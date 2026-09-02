package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.util.Size
import com.caverock.androidsvg.SVG
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Gli SVG, e perché qui non c'era niente da provare prima.
 *
 * ⚠️⚠️ **ANDROID NON SA DISEGNARE UN SVG, e non l'ha mai saputo.** Non è il caso dell'AVIF,
 * dove il sistema dichiara il formato e poi si rifiuta: qui `ImageDecoder` non lo dichiara
 * affatto, perché conosce i formati **a pixel**. La grafica vettoriale del sistema è
 * un'altra cosa, cioè `VectorDrawable`, che è un XML di Android e **non** un SVG: non ha il
 * CSS, non ha `<text>`, non ha i filtri, e vive dentro le risorse dell'app invece che in un
 * file che si apre. Quindi la scelta non era 'sistema o libreria', era 'libreria o niente'.
 *
 * ⚠️ **La libreria è quella che Coil si porta dietro**, letto nel POM di `coil-svg-android`
 * 3.6.0 e non ricordato: `com.caverock:androidsvg-aar:1.4`, dichiarata là come dipendenza a
 * runtime. Ferma dal 2019, ma è la scelta di chi fa il caricatore che questo progetto già
 * usa, e pesa 202.395 byte di AAR **tutti Java**: nessuna libreria nativa, quindi nessun
 * byte per architettura e nessuna esclusione da fare come per libavif.
 *
 * ⚠️⚠️ **UN VETTORE NON HA PIXEL, e questo è il compromesso da sapere prima di provare lo
 * zoom.** Tutto il visualizzatore lavora su un `ImageBitmap`: la scheda delle info, la
 * copia, la condivisione, l'editor, l'esportazione. Quindi l'SVG si **rasterizza** una
 * volta, con il lato lungo a [BOX] pixel, e da lì in poi è un'immagine come le altre. Su
 * uno schermo da 1080 punti quello basta a ingrandire quasi due volte senza vedere
 * sgranare; oltre, si sgrana come un PNG. ⚠️ **La strada giusta sarebbe ridisegnare a ogni
 * livello di zoom**, e non si è fatta: vorrebbe dire un secondo tipo di immagine dentro il
 * visualizzatore e in tutto quello che ci passa. Sta scritto qui perché è il primo posto in
 * cui guardare se l'utente chiederà gli SVG nitidi a 40 ingrandimenti.
 *
 * ⚠️⚠️ **IL `.svgz` C'È DALLA `1.34`, e la decodifica non è costata NIENTE**: il parser legge
 * i primi due byte e, se sono la firma gzip, si avvolge da sé in un `GZIPInputStream` (letto
 * nel bytecode di `SVGParser.parse`). L'ha chiesto l'utente (giro della `1.31`, voce
 * `svg-elenchi`). Quello che è costato qualcosa è **riconoscerlo**: vedi [looksLike], perché
 * un file compresso non contiene la stringa `<svg` da nessuna parte.
 */
object Svg {

    /**
     * Se la libreria c'è, **e insieme la sua unica impostazione di sicurezza**.
     *
     * ⚠️⚠️ **`setInternalEntitiesEnabled(false)` CHIUDE LA BOMBA DI ESPANSIONE**, e la
     * misura sta nel bytecode del parser, non nella documentazione. Che cosa fa androidsvg
     * quando legge un SVG:
     * - la strada normale (`XmlPullParser`) mette `process-docdecl` a **false**, cioè non
     *   guarda nemmeno il `DOCTYPE`;
     * - ma se nel testo compare `<!ENTITY` **cambia parser** e passa a SAX apposta per
     *   espandere le entità, scrivendo *'Switching to SAX parser to process entities'*.
     * Da lì un file di poche righe può chiedere gigabyte di memoria, che è l'attacco noto
     * come 'billion laughs'. Con questo interruttore quel cambio di parser non avviene.
     * ⚠️ **L'XXE invece era già impossibile**, e non per merito nostro: lo stesso ramo SAX
     * mette a **false** `external-general-entities` e `external-parameter-entities`, e
     * questo progetto non registra nessun `SVGExternalFileResolver`, quindi un SVG non può
     * leggere un file del telefono né aprire una connessione.
     * ⚠️ **Il prezzo, dichiarato**: un SVG che definisce entità proprie perde quei pezzi.
     * Sono rarissimi (nessun editor li produce) e il baratto è a favore della sicurezza.
     */
    val ready: Boolean = try {
        SVG.setInternalEntitiesEnabled(false)
        true
    } catch (t: Throwable) {
        false
    }

    /** Il tipo MIME, per la barra delle info: nessuno l'ha letto dal file. */
    const val MIME = "image/svg+xml"

    /**
     * Quanti byte bastano a riconoscere il formato.
     *
     * ⚠️ Non è una firma di due byte come negli altri formati: un SVG è **testo**, e
     * `<svg` può stare dopo la dichiarazione XML, un `DOCTYPE`, un commento di licenza e
     * qualche riga di intestazione. Mille byte li coprono tutti, ed è la stessa misura che
     * Coil usa nel suo `DecodeUtils.isSvg` (letto nel bytecode, dove il limite è `1024l`).
     */
    const val SNIFF = 1024

    /**
     * Il byte zero, quello che separa le lettere di un testo in UTF-16.
     *
     * ⚠️ Scritto per **codepoint** e non incollato, come vuole la regola del repo sui
     * caratteri invisibili: incollato, questo file conterrebbe un byte che nessun editor
     * mostra e che `grep` chiamerebbe binario, e infatti è successo scrivendolo.
     */
    private const val NUL = '\u0000'

    /**
     * Il lato lungo a cui si rasterizza per il visualizzatore.
     *
     * ⚠️ **Il numero è un baratto fra nitidezza e memoria, e questi sono i due lati**: un
     * quadrato di 2048 pixel costa 16,8 MB di heap come ARGB_8888, che su un budget da
     * quattro megapixel è già il tetto; il doppio costerebbe quattro volte tanto per
     * guadagnare un solo raddoppio di zoom. Su uno schermo da 1080 punti di lato corto
     * questo dà circa 1,9 ingrandimenti prima di vedere il pixel.
     * ⚠️ **Il budget lo può ridurre, e non lo alza mai**: chi ha poca memoria libera riceve
     * meno pixel, come per ogni altro formato.
     */
    const val BOX = 2048

    /**
     * Riconosce un SVG dai primi byte, senza costruire niente.
     *
     * ⚠️ **I byte NUL si buttano prima di guardare**, e serve a un caso vero: un SVG salvato
     * in UTF-16 mette un NUL fra ogni lettera, quindi il tag di apertura non somiglierebbe a
     * niente. Costa un filtro su mille byte e copre le due codifiche più diffuse con una
     * sola ricerca. ⚠️ Il carattere è scritto per **codepoint** e non incollato, come vuole la
     * regola del repo sui caratteri invisibili.
     * ⚠️ **Un falso positivo non fa danni**, ed è la ragione per cui basta cercare il tag:
     * nel visualizzatore questa domanda si fa **dopo** che `ImageDecoder` ha già fallito,
     * quindi al peggio si prova a disegnare un SVG che non c'è e si torna all'errore di
     * prima; nella griglia il decodificatore torna `null` e la richiesta prosegue.
     */
    fun looksLike(head: ByteArray): Boolean {
        if (head.isEmpty()) return false
        val letto = if (gzipped(head)) unzip(head) ?: return false else head
        val text = String(letto, 0, minOf(letto.size, SNIFF), Charsets.ISO_8859_1)
            .filter { it != NUL }
        return text.contains("<svg", ignoreCase = true)
    }

    /**
     * La firma gzip, cioè un `.svgz`.
     *
     * ⚠️⚠️ **SENZA QUESTO, DICHIARARE `.svgz` SAREBBE STATO IL DIFETTO DEL TIFF**: la
     * `1.34` mette quell'estensione nei filtri del manifest e nei due elenchi, e il parser
     * sa decomprimerla da sé, **ma il riconoscimento del contenuto no**. Un file compresso
     * non contiene la stringa `<svg` da nessuna parte, quindi [looksLike] rispondeva no e la
     * miniatura non si faceva: un formato dichiarato e non aperto, che è esattamente
     * l'errore che i `.tif` sono usciti per non commettere più.
     * ⚠️ **Due byte e non l'estensione**: qui non c'è nessun nome di file (un decodificatore
     * riceve i byte), ed è la stessa ragione per cui `Avif.looksLike` guarda la scatola
     * `ftyp` e non la coda del nome.
     */
    private fun gzipped(head: ByteArray): Boolean =
        head.size >= 2 && head[0] == GZIP_1 && head[1] == GZIP_2

    /**
     * I primi [SNIFF] byte di un `.svgz`, scompattati.
     *
     * ⚠️ **Si legge SOLO quel tanto e si butta il resto**: qui la domanda è 'somiglia a un
     * SVG', non 'disegnalo', e un `.svgz` di dieci megabyte scompattato per intero per
     * guardarne il primo tag sarebbe memoria buttata a ogni riquadro della griglia.
     * ⚠️ **Il troncamento non fa male**: `GZIPInputStream` legge a blocchi e chiudere prima
     * della fine è legittimo. ⚠️ E un archivio rotto torna `null` invece di sollevare, come
     * ogni altro riconoscimento di questo file.
     */
    private fun unzip(head: ByteArray): ByteArray? = try {
        GZIPInputStream(ByteArrayInputStream(head)).use { zip ->
            val fuori = ByteArray(SNIFF)
            var quanti = 0
            while (quanti < SNIFF) {
                val n = zip.read(fuori, quanti, SNIFF - quanti)
                if (n <= 0) break
                quanti += n
            }
            if (quanti == 0) null else fuori.copyOf(quanti)
        }
    } catch (t: Throwable) {
        null
    }

    /** I due byte della firma gzip, scritti per valore: `1f 8b`. */
    private const val GZIP_1: Byte = 0x1f
    private const val GZIP_2: Byte = 0x8b.toByte()

    /** Come [render], per chi ha già i byte in mano. */
    fun render(bytes: ByteArray, box: Int, cap: Long = FREE): Rendered? =
        render(ByteArrayInputStream(bytes), box, cap)

    /**
     * Nessun tetto ai pixel.
     *
     * ⚠️ **Chi chiede un riquadro piccolo non ha bisogno di un budget**, ed è il caso delle
     * miniature: il lato lungo è già 512, quindi il tetto non potrebbe scattare mai e
     * passarlo sarebbe un numero da tenere d'accordo con un altro per niente. Serve invece
     * al visualizzatore, che chiede [BOX].
     */
    const val FREE = Long.MAX_VALUE

    /**
     * Disegna il documento in un bitmap col lato lungo a [box], mai oltre [cap] pixel.
     *
     * Torna `null` quando il documento non si legge, non dichiara nessuna misura, o il
     * disegno fallisce: chi chiama conserva **il proprio** errore invece di riceverne uno
     * nuovo.
     *
     * ⚠️ **Lo stream si chiude da sé**, e non è una dimenticanza del chiamante: `SVGParser`
     * lo chiude in fondo alla propria `parse` (letto nel bytecode). Chiuderlo due volte non
     * fa niente, quindi un `use` di chi chiama resta legittimo.
     */
    fun render(input: InputStream, box: Int, cap: Long = FREE): Rendered? {
        val svg = parse(input) ?: return null
        val full = sizeOf(svg) ?: return null
        val target = fit(full, box, cap)
        val bitmap = try {
            Bitmap.createBitmap(target.width, target.height, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            return null
        }
        val done = try {
            /*
             * ⚠️⚠️ **LE DUE RIGHE DEL `viewBox` SONO QUELLO CHE FA SCALARE IL DISEGNO**, e
             * senza di esse un SVG con `width="24"` uscirebbe grande 24 pixel in un angolo di
             * un bitmap da 2048. La tecnica è letta dal wrapper di `coil-svg` 3.6.0, non
             * inventata: se il documento non ha un `viewBox` gliene si dà uno pari alla sua
             * misura dichiarata, poi si mettono larghezza e altezza al **100%**, e da quel
             * momento il documento riempie qualunque riquadro gli si passi.
             */
            if (svg.documentViewBox == null) {
                svg.setDocumentViewBox(0f, 0f, full.width.toFloat(), full.height.toFloat())
            }
            svg.setDocumentWidth("100%")
            svg.setDocumentHeight("100%")
            svg.renderToCanvas(
                Canvas(bitmap),
                RectF(0f, 0f, target.width.toFloat(), target.height.toFloat())
            )
            true
        } catch (t: Throwable) {
            /*
             * ⚠️⚠️ **`Throwable` E NON `Exception`, e qui c'è un motivo MISURATO**: per
             * disegnare un `clip-path` androidsvg passa da `CanvasLegacy`, che raggiunge per
             * **riflessione** `Canvas.MATRIX_SAVE_FLAG` e `Canvas.save(int)`. Quei due membri
             * **non esistono più nell'SDK** (verificato con `javap` sull'`android.jar` di API
             * 37: la costante non c'è e `save` ha solo la forma senza argomenti), e vivono nel
             * runtime come API rimosse. È il motivo per cui la libreria usa la riflessione, ma
             * se un domani cadessero anche da là il fallimento arriverebbe come un `Error` e
             * non come un'eccezione. Con questa riga diventa 'formato non supportato' invece
             * di un arresto dell'app.
             */
            false
        }
        if (!done) {
            bitmap.recycle()
            return null
        }
        return Rendered(
            bitmap = bitmap,
            fullWidth = full.width,
            fullHeight = full.height,
            // ⚠️ Qui 'ridotta' vuol dire davvero ridotta: un SVG di 24 unità disegnato a
            // 2048 pixel è **ingrandito**, e dirlo campionato sarebbe il contrario del vero.
            sampled = target.width < full.width || target.height < full.height
        )
    }

    /** Quello che il disegno sa dire, oltre ai pixel. */
    data class Rendered(
        val bitmap: Bitmap,
        val fullWidth: Int,
        val fullHeight: Int,
        val sampled: Boolean
    )

    /**
     * Le misure **dichiarate dal documento**, per la scheda delle informazioni.
     *
     * ⚠️⚠️ **SERVE PERCHÉ `BitmapFactory` DI UN SVG NON DICE NIENTE**, ed è lo stesso buco
     * dell'AVIF con una causa diversa: là il decodificatore si rifiuta, qui il formato non
     * lo conosce nessuno. Senza questa lettura la scheda scriverebbe '?' sulle misure.
     * ⚠️⚠️ **E le misure sono quelle del DOCUMENTO, non quelle del bitmap che si vede**, che
     * è la scelta da capire per non leggerla come un difetto: un vettore non ha pixel, e
     * l'unico numero che appartiene al **file** è quello che il file dichiara. I 2048 pixel
     * del disegno li ha scelti [BOX], cioè noi, e annunciarli come misura dell'immagine
     * vorrebbe dire far dire al file una cosa che non c'è scritta.
     */
    fun dimensions(input: InputStream): Size? = parse(input)?.let { sizeOf(it) }

    /** Il documento, o `null` se questo non è un SVG leggibile. */
    private fun parse(input: InputStream): SVG? {
        if (!ready) return null
        return try {
            SVG.getFromInputStream(input)
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * La misura dichiarata: prima larghezza e altezza, poi il `viewBox`.
     *
     * ⚠️ **In quest'ordine e non al contrario**: `width` e `height` sono la misura a cui il
     * documento chiede di essere disegnato, il `viewBox` è il sistema di coordinate del
     * contenuto, e i due possono differire (`width="100"` con `viewBox="0 0 24 24"` è
     * normalissimo). ⚠️ E il primo **manca spesso**: quando la misura è in percentuale
     * androidsvg torna **-1**, ed è là che il `viewBox` diventa l'unica risposta.
     */
    private fun sizeOf(svg: SVG): Size? {
        val width = svg.documentWidth
        val height = svg.documentHeight
        if (width > 0f && height > 0f) {
            return Size(ceil(width).toInt(), ceil(height).toInt())
        }
        val box = svg.documentViewBox ?: return null
        if (box.width() <= 0f || box.height() <= 0f) return null
        return Size(ceil(box.width()).toInt(), ceil(box.height()).toInt())
    }

    /**
     * Il riquadro in cui disegnare: lato lungo [box], proporzioni del documento, mai oltre
     * [cap] pixel in tutto.
     *
     * ⚠️ **Si ingrandisce anche quando il documento è più piccolo**, ed è tutto il punto di
     * un vettore: un'icona di 24 unità a 24 pixel sarebbe illeggibile a schermo pieno.
     */
    private fun fit(full: Size, box: Int, cap: Long): Size {
        val long = maxOf(full.width, full.height).toFloat()
        val scale = box / long
        var width = (full.width * scale).roundToInt().coerceAtLeast(1)
        var height = (full.height * scale).roundToInt().coerceAtLeast(1)
        val pixels = width.toLong() * height.toLong()
        if (pixels > cap && cap > 0) {
            val shrink = sqrt(cap.toDouble() / pixels.toDouble()).toFloat()
            width = (width * shrink).roundToInt().coerceAtLeast(1)
            height = (height * shrink).roundToInt().coerceAtLeast(1)
        }
        return Size(width, height)
    }
}

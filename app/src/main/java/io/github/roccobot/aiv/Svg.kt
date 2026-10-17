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
 * Gli SVG.
 *
 * ⚠️⚠️ **Android non sa disegnare un SVG**: `ImageDecoder` conosce i soli formati a pixel, e
 * `VectorDrawable` è un XML di Android, non un SVG (niente CSS, `<text>` né filtri). Quindi
 * serve una libreria: `com.caverock:androidsvg-aar:1.4`, quella che `coil-svg-android` dichiara
 * come dipendenza, tutta Java (nessuna libreria nativa da escludere per architettura).
 *
 * ⚠️⚠️ **Un vettore non ha pixel, e il visualizzatore lavora su un `ImageBitmap`** (scheda delle
 * informazioni, copia, condivisione, editor, esportazione): l'SVG si rasterizza una volta col lato
 * lungo a [BOX], e oltre circa due ingrandimenti si sgrana come un PNG. Ridisegnare a ogni livello
 * di zoom vorrebbe un secondo tipo di immagine in tutto il visualizzatore.
 *
 * Il `.svgz` (richiesto dall'utente, voce `svg-elenchi`) il parser lo decomprime da sé quando
 * legge la firma gzip; riconoscerlo è compito di [looksLike].
 */
object Svg {

    /**
     * Se la libreria c'è, insieme alla sua unica impostazione di sicurezza.
     *
     * ⚠️⚠️ **`setInternalEntitiesEnabled(false)` chiude la bomba di espansione**: androidsvg
     * legge col `XmlPullParser` senza guardare il `DOCTYPE`, ma se nel testo compare `<!ENTITY`
     * passa a SAX per espandere le entità, e da lì un file di poche righe può chiedere gigabyte
     * di memoria ('billion laughs'). Con l'interruttore spento il cambio di parser non avviene.
     * L'XXE è già impossibile: il ramo SAX spegne le entità esterne, e il progetto non registra
     * nessun `SVGExternalFileResolver`. Il prezzo: un SVG che definisce entità proprie perde quei
     * pezzi, e nessun editor ne produce.
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
     * Un SVG è testo, e `<svg` può trovarsi dopo la dichiarazione XML, un `DOCTYPE` e un
     * commento di licenza: 1024 byte li coprono, ed è la stessa misura di `DecodeUtils.isSvg` di
     * Coil.
     */
    const val SNIFF = 1024

    /**
     * Il byte zero, quello che separa le lettere di un testo in UTF-16.
     *
     * ⚠️ Scritto per codepoint, come vuole la regola del repo sui caratteri invisibili:
     * incollato, il file conterrebbe un byte che nessun editor mostra e che `grep` chiamerebbe
     * binario.
     */
    private const val NUL = '\u0000'

    /**
     * Il lato lungo a cui si rasterizza per il visualizzatore.
     *
     * ⚠️ **Un compromesso fra nitidezza e memoria**: un quadrato di 2048 pixel costa 16,8 MB di
     * heap come ARGB_8888, e il doppio costerebbe quattro volte tanto per un solo raddoppio di
     * zoom. Su uno schermo da 1080 punti di lato corto dà circa 1,9 ingrandimenti prima di vedere
     * il pixel. Il budget di memoria lo può ridurre, mai alzare.
     */
    const val BOX = 2048

    /**
     * Riconosce un SVG dai primi byte, senza costruire niente.
     *
     * I byte NUL si scartano prima di guardare: un SVG in UTF-16 mette un NUL fra ogni lettera,
     * e così una ricerca sola copre le due codifiche.
     * ⚠️⚠️ **Un falso positivo nella griglia costa la miniatura**: un `null` da `decode` di Coil
     * rimanda al decodificatore successivo con la stessa sorgente, ma `SVGParser.parse` chiude lo
     * stream anche quando fallisce, e su un `content://` con lui si chiude il descrittore, quindi
     * falliscono anche i decodificatori di serie. Nel visualizzatore la domanda si fa dopo che
     * `ImageDecoder` ha fallito, e là un falso positivo è innocuo.
     * ⚠️⚠️ **Per questo il tag non basta: il primo segno di un SVG è `<`**, dopo un eventuale BOM
     * e gli spazi che XML ammette. Un PNG di ChatGPT contiene nel manifesto C2PA un'icona SVG,
     * col tag al byte 305; nessun formato a pixel comincia con `<`, quindi la condizione li
     * esclude tutti.
     * Il BOM si toglie per firma e non per insieme di caratteri: letto in ISO-8859-1, un insieme
     * toglierebbe anche il byte `0xFF` con cui comincia un JPEG.
     * La sorgente chiusa resta: un `peek()` la terrebbe aperta ma porterebbe in memoria il
     * documento intero, che `SvgThumbnailDecoder` (in `Thumbs.kt`) esiste per evitare; e un file
     * che comincia con `<` è testo, che nessun decodificatore a pixel legge comunque.
     */
    fun looksLike(head: ByteArray): Boolean {
        if (head.isEmpty()) return false
        val letto = if (gzipped(head)) unzip(head) ?: return false else head
        val text = String(letto, 0, minOf(letto.size, SNIFF), Charsets.ISO_8859_1)
            .filter { it != NUL }
        val corpo = BOMS.firstOrNull { text.startsWith(it) }?.let { text.substring(it.length) } ?: text
        val primo = corpo.firstOrNull { it !in XML_SPACE }
        return primo == '<' && text.contains("<svg", ignoreCase = true)
    }

    /**
     * I tre BOM che un SVG può avere, letti come li legge [looksLike], in ISO-8859-1 e senza i
     * NUL: UTF-8, UTF-16 little-endian e big-endian. Scritti per codepoint, come [NUL].
     */
    private val BOMS = listOf("\u00EF\u00BB\u00BF", "\u00FF\u00FE", "\u00FE\u00FF")

    /** Gli spazi che XML ammette prima del primo segno, e nessun altro: la produzione `S`. */
    private val XML_SPACE = setOf(' ', '\t', '\r', '\n')

    /**
     * La firma gzip, cioè un `.svgz`.
     *
     * ⚠️ **Senza, il `.svgz` sarebbe dichiarato nel manifest e non riconosciuto**: un file
     * compresso non contiene `<svg`, e [looksLike] risponderebbe no. Due byte e non
     * l'estensione, perché un decodificatore riceve i byte e non il nome (come `Avif.looksLike`).
     */
    private fun gzipped(head: ByteArray): Boolean = Signatures.gzip(head)

    /**
     * I primi [SNIFF] byte di un `.svgz`, scompattati.
     *
     * Si legge solo quel tanto: scompattare per intero un `.svgz` grande per guardarne il primo
     * tag sprecherebbe memoria a ogni riquadro della griglia. `GZIPInputStream` legge a blocchi,
     * e chiudere prima della fine è legittimo. Un archivio rotto torna `null`.
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

    /** Come [render], per chi ha già i byte in mano. */
    fun render(bytes: ByteArray, box: Int, cap: Long = FREE): Rendered? =
        render(ByteArrayInputStream(bytes), box, cap)

    /**
     * Nessun tetto ai pixel.
     *
     * Le miniature chiedono un lato di 512, quindi un tetto non scatterebbe mai; il budget serve
     * al visualizzatore, che chiede [BOX].
     */
    const val FREE = Long.MAX_VALUE

    /**
     * Disegna il documento in un bitmap col lato lungo a [box], mai oltre [cap] pixel.
     *
     * Torna `null` quando il documento non si legge, non dichiara nessuna misura, o il disegno
     * fallisce: chi chiama conserva il proprio errore.
     *
     * Lo stream lo chiude `SVGParser` in fondo alla sua `parse`; chiuderlo due volte non fa
     * niente, quindi un `use` di chi chiama resta legittimo.
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
             * ⚠️⚠️ **Il `viewBox` e il 100% fanno scalare il disegno**: senza, un SVG con
             * `width="24"` risulterebbe di 24 pixel in un angolo del bitmap. È la tecnica di
             * `coil-svg`: un documento senza `viewBox` ne riceve uno pari alla sua misura, poi
             * larghezza e altezza al 100% gli fanno riempire qualunque riquadro.
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
             * ⚠️⚠️ **`Throwable` e non `Exception`**: per un `clip-path` androidsvg raggiunge per
             * riflessione `Canvas.MATRIX_SAVE_FLAG` e `Canvas.save(int)`, che nell'SDK non
             * esistono più e vivono nel runtime come API rimosse. Se sparissero anche da là il
             * fallimento arriverebbe come un `Error`: così diventa 'formato non supportato' e
             * non un arresto dell'app.
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
            // 'Ridotta' vuol dire davvero ridotta: un SVG di 24 unità disegnato a 2048 pixel è
            // ingrandito.
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
     * Le misure dichiarate dal documento, per la scheda delle informazioni: `BitmapFactory` di un
     * SVG non dice niente.
     *
     * ⚠️ Sono le misure del documento e non del bitmap che si vede: un vettore non ha pixel, e
     * l'unico numero che appartiene al file è quello che il file dichiara; quello del disegno lo
     * sceglie [BOX].
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
     * In quest'ordine: `width` e `height` sono la misura a cui il documento chiede di essere
     * disegnato, il `viewBox` è il sistema di coordinate del contenuto, e possono differire. Con
     * una misura in percentuale androidsvg torna -1, e allora resta il `viewBox`.
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
     * Si ingrandisce anche quando il documento è più piccolo: un'icona di 24 unità a 24 pixel
     * sarebbe illeggibile a schermo pieno.
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

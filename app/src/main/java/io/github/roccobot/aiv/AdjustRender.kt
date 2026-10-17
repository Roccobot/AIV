package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorSpace
import android.graphics.HardwareRenderer
import android.hardware.HardwareBuffer
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RenderNode
import android.graphics.Shader.TileMode
import android.media.ImageReader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Shader as ComposeShader

/**
 * Far girare il conto dell'editor completo **fuori schermo**, cioè sul file pieno invece che
 * sull'anteprima.
 *
 * ⚠️⚠️ **Esiste perché un `RuntimeShader` non gira su una tela di memoria**: quel programma vive
 * sulla scheda grafica, e un `Canvas` costruito sopra un `Bitmap` lo ignora. Salvare vuol dire
 * applicare lo stesso conto dell'anteprima al file pieno, e l'unica strada che non lo riscrive in
 * Kotlin è disegnare davvero, su una superficie che non si vede: un [ImageReader] (la superficie),
 * un [HardwareRenderer] (il motore) e un [RenderNode] (la scena). Il buffer che esce diventa una
 * mappa di pixel leggibile con `Bitmap.wrapHardwareBuffer`.
 *
 * ⚠️⚠️ **Si lavora a tessere perché una texture ha un tetto, diverso da telefono a telefono**:
 * un'immagine da ventiquattro megapixel è larga seimila pixel. Il minimo garantito è [TILE].
 * - ⚠️ **Le tessere si sovrappongono, e il bordo si scarta dopo**: i moduli Dettaglio ed Effetti
 *   guardano i vicini, e senza un bordo il filtro dell'ultima colonna di una tessera leggerebbe il
 *   bordo ripetuto, cioè su ogni giunzione comparirebbe una riga. Il conto vive in [tileBox], e la
 *   larghezza del bordo la dicono i filtri stessi ([bleedFor]).
 * - Il passo si stringe di quanto il bordo cresce, o una tessera col bordo supererebbe il tetto.
 *
 * ⚠️⚠️ **Prima di fidarsi si prova, con [works]**: se il percorso non funziona su un telefono, ne
 * esce un'immagine nera, che scritta sul file prende il posto dell'originale. Una prova su un
 * quadrato di colore noto costa un millesimo di secondo.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal object AdjustRender {

    /**
     * [source] con [look] applicato, oppure `null` se il percorso grafico non ha funzionato.
     *
     * ⚠️ **La mappa di partenza non si tocca e non si ricicla**: chi chiama può averla ancora in
     * mano, e qui non si sa se sia l'unica copia.
     */
    fun apply(source: Bitmap, look: Look): Bitmap? {
        // ⚠️ Si guarda il colore e non tutto il `Look`: con la sola geometria mossa lo shader non
        // ha niente da applicare. La deformazione è la passata dopo, in `Geometry.kt`.
        if (look.plain) return null
        if (!works()) return null
        val w = source.width
        val h = source.height
        if (w <= 0 || h <= 0) return null

        /*
         * ⚠️ **La misura è il lato lungo dell'immagine intera, non della tessera**: il Dettaglio
         * ragiona in frazioni del lato, e il lato di una tessera darebbe un filtro più stretto
         * proprio dove le tessere sono più d'una, diverso dall'anteprima.
         */
        val span = maxOf(w, h).toFloat()
        val bleed = bleedFor(look, span)
        val step = (TILE - 2 * bleed).coerceAtLeast(1)

        val out = try {
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            return null
        }
        val canvas = Canvas(out)

        var y = 0
        while (y < h) {
            var x = 0
            val th = minOf(step, h - y)
            while (x < w) {
                val tw = minOf(step, w - x)
                val box = tileBox(w, h, x, y, tw, th, bleed)
                /*
                 * ⚠️ La tessera si ritaglia dal sorgente: uno shader che legge il bitmap intero
                 * costringerebbe la scheda grafica a caricarlo tutto, che è quello che le tessere
                 * evitano.
                 */
                val piece = runCatching {
                    Bitmap.createBitmap(
                        source, box.read.left, box.read.top, box.read.width(), box.read.height()
                    )
                }.getOrNull() ?: run { out.recycle(); return null }
                /*
                 * ⚠️⚠️ **La tessera dichiara dove si trova**: la vignettatura si misura dal centro
                 * dell'immagine intera e la grana nasce dalla coordinata assoluta, quindi una
                 * tessera che dichiarasse sé stessa darebbe un angolo scuro a ogni giunzione e la
                 * stessa grana ripetuta a scacchi. L'origine è quella del pezzo letto
                 * (`box.read`), perché lo shader gira su quei pixel, bordo compreso.
                 */
                val dove = Framed.tile(
                    box.read.left.toFloat(), box.read.top.toFloat(),
                    w.toFloat(), h.toFloat()
                )
                val done = draw(piece, look, box.read.width(), box.read.height(), dove)
                if (piece !== source) piece.recycle()
                if (done == null) {
                    out.recycle()
                    return null
                }
                canvas.drawBitmap(done, box.take, box.put, null)
                done.recycle()
                x += tw
            }
            y += th
        }
        return out
    }

    /**
     * Una tessera disegnata davvero, con lo shader dentro.
     *
     * ⚠️ **Tutto quello che si apre qui si chiude qui**: un [HardwareRenderer] non chiuso tiene
     * aperto un contesto grafico, uno per tessera. Il `finally` copre anche la strada dell'errore.
     */
    private fun draw(piece: Bitmap, look: Look, w: Int, h: Int, where: Framed): Bitmap? {
        var reader: ImageReader? = null
        var renderer: HardwareRenderer? = null
        var node: RenderNode? = null
        return try {
            reader = ImageReader.newInstance(
                w, h, PixelFormat.RGBA_8888, 1,
                HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
            )
            renderer = HardwareRenderer()
            renderer.setSurface(reader.surface)
            node = RenderNode("aiv-look")
            node.setPosition(0, 0, w, h)

            val image = BitmapShader(piece, TileMode.CLAMP, TileMode.CLAMP)
            /*
             * ⚠️ **Il filtro lineare serve al Dettaglio**, che legge i vicini a distanze che non
             * cadono su un pixel intero: senza, ogni campione si arrotonderebbe al pixel più vicino
             * e il vicinato avrebbe meno punti di quanti ne chiede. L'anteprima fa lo stesso.
             */
            image.setFilterMode(BitmapShader.FILTER_MODE_LINEAR)
            val shader = lookShader(image as ComposeShader, look, where) ?: return null
            val paint = Paint().apply { this.shader = shader }

            val canvas = node.beginRecording()
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            node.endRecording()

            renderer.setContentRoot(node)
            /*
             * ⚠️ **`setWaitForPresent` rende il disegno sincrono**: senza, `syncAndDraw` torna
             * prima che la scheda grafica abbia finito, e il buffer si legge mezzo vuoto, in modo
             * intermittente.
             */
            renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()

            val got = reader.acquireNextImage() ?: return null
            try {
                val buffer = got.hardwareBuffer ?: return null
                try {
                    val wrapped = Bitmap.wrapHardwareBuffer(
                        buffer, ColorSpace.get(ColorSpace.Named.SRGB)
                    ) ?: return null
                    // La copia serve: una mappa di tipo `HARDWARE` non si legge pixel per pixel
                    // e non si comprime.
                    wrapped.copy(Bitmap.Config.ARGB_8888, false)
                } finally {
                    buffer.close()
                }
            } finally {
                got.close()
            }
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        } finally {
            node?.discardDisplayList()
            renderer?.destroy()
            reader?.close()
        }
    }

    /**
     * Se il percorso grafico fuori schermo funziona su questo telefono.
     *
     * Si misura una volta e si ricorda, perché la risposta dipende dall'apparecchio e non
     * dall'immagine. ⚠️ **Si ricorda il solo `true`**: la prima prova potrebbe cadere su una
     * scheda grafica occupata, e un `false` ricordato spegnerebbe l'editor completo fino al
     * riavvio dell'app.
     *
     * La prova è un quadrato bianco col conto a riposo, tutti i cursori a zero: là il programma
     * restituisce quello che riceve, quindi un risultato diverso dal bianco dice che il percorso
     * è rotto, non che il conto sbaglia.
     */
    fun works(): Boolean {
        if (proven) return true
        val probe = try {
            Bitmap.createBitmap(PROBE, PROBE, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            return false
        }
        probe.eraseColor(android.graphics.Color.WHITE)
        val done = draw(probe, Look.NONE, PROBE, PROBE, Framed.whole(PROBE.toFloat(), PROBE.toFloat()))
        probe.recycle()
        if (done == null) return false
        val pixel = done.getPixel(PROBE / 2, PROBE / 2)
        done.recycle()
        val ok = android.graphics.Color.red(pixel) > NEAR &&
            android.graphics.Color.green(pixel) > NEAR &&
            android.graphics.Color.blue(pixel) > NEAR
        if (ok) proven = true
        return ok
    }

    /** Vedi [works]: si ricorda il solo esito positivo. */
    @Volatile
    private var proven = false

    /**
     * Il lato di una tessera, in pixel.
     *
     * ⚠️ **2048 è il minimo che la specifica grafica garantisce**: da OpenGL ES 3.0 ogni
     * apparecchio accetta una texture di questo lato, e Android 13 non gira su niente di meno. Un
     * numero più grande fallirebbe proprio sui telefoni deboli.
     */
    private const val TILE = 2048

    /** Il lato del quadrato di prova: più piccolo di così non si può leggere un pixel di mezzo. */
    private const val PROBE = 4

    /**
     * Quanto un canale deve essere vicino al pieno perché la prova sia passata.
     *
     * Non si chiede 255 esatto: fra il buffer e la mappa di pixel c'è una conversione di spazio
     * colore, e un livello di scarto è normale. Un percorso rotto dà zero.
     */
    private const val NEAR = 250
}

/**
 * Quanto bordo vuole una tessera, su un'immagine il cui lato lungo vale [span].
 *
 * ⚠️ **Due moduli guardano i vicini, e una tessera ha un bordo solo**: il Dettaglio legge a un
 * raggio e gli Effetti a un altro, quindi si prende il massimo. Sommarli sprecherebbe spazio,
 * perché i due filtri girano sulla stessa tessera e non uno sull'uscita dell'altro.
 *
 * È una funzione a sé perché il banco la possa chiamare: dentro [AdjustRender.apply] disegnare
 * vuole una scheda grafica.
 */
internal fun bleedFor(look: Look, span: Float): Int =
    maxOf(look.detail.bleed(span), look.effects.bleed(span))

/**
 * I tre riquadri di una tessera: da dove si legge, che cosa si tiene, e dove si mette.
 *
 * ⚠️ **Sono tre perché il bordo si taglia ai margini dell'immagine**: una tessera d'angolo non ha
 * vicini da un lato, quindi [read] è più stretto di quanto il bordo chiederebbe, e [take] deve
 * saperlo. Là il filtro legge il bordo ripetuto, ed è giusto: oltre l'immagine non c'è niente.
 */
internal data class TileBox(val read: Rect, val take: Rect, val put: Rect)

/**
 * Dove leggere, che cosa tenere e dove mettere la tessera che comincia a ([x], [y]) ed è larga
 * [tw] per [th], su un'immagine di [w] per [h], con [bleed] pixel di bordo.
 *
 * ⚠️ **Vive fuori dall'oggetto e non usa niente di Android 13**, perché il banco di prova la
 * possa misurare: un `bleed` sbagliato di un pixel darebbe una riga sulle giunzioni, visibile
 * solo su un'immagine grande guardata da vicino.
 *
 * Le tessere si sovrappongono in lettura e non in scrittura: [put] sono esattamente i pixel della
 * tessera, quindi quello che si copia non dipende dall'ordine.
 */
internal fun tileBox(w: Int, h: Int, x: Int, y: Int, tw: Int, th: Int, bleed: Int): TileBox {
    val left = maxOf(0, x - bleed)
    val top = maxOf(0, y - bleed)
    val right = minOf(w, x + tw + bleed)
    val bottom = minOf(h, y + th + bleed)
    val inX = x - left
    val inY = y - top
    return TileBox(
        read = Rect(left, top, right, bottom),
        take = Rect(inX, inY, inX + tw, inY + th),
        put = Rect(x, y, x + tw, y + th)
    )
}

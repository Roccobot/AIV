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
 * ⚠️⚠️ **ESISTE PERCHÉ UN `RuntimeShader` NON GIRA SU UNA TELA DI MEMORIA, e la
 * documentazione di Android lo dice in una riga**: quel tipo di programma vive sulla scheda
 * grafica, quindi un `Canvas` costruito sopra un `Bitmap` lo ignora. Salvare vuol dire applicare
 * lo **stesso** conto dell'anteprima a venti megapixel, e l'unica strada che non lo riscrive una
 * seconda volta in Kotlin è disegnare davvero, su una superficie che non si vede.
 * - **I tre pezzi che servono**, e nessuno di loro è facoltativo: un [ImageReader], che è la
 *   superficie dove il disegno atterra; un [HardwareRenderer], che è il motore; un [RenderNode],
 *   che è la scena da disegnare. Quello che esce è un buffer della scheda grafica, e
 *   `Bitmap.wrapHardwareBuffer` lo rende una mappa di pixel leggibile.
 *
 * ⚠️⚠️ **SI LAVORA A TESSERE PERCHÉ UNA TEXTURE HA UN TETTO, e non è lo stesso su ogni
 * telefono**: una fotografia da ventiquattro megapixel è larga seimila pixel, e non tutte le
 * schede grafiche accettano una texture così. Il minimo che ogni apparecchio con Android 13
 * garantisce è [TILE], quindi si lavora in quadrati di quel lato: chiedere alla scheda quanto
 * regge vorrebbe dire aprire un contesto grafico per una domanda sola.
 * - ⚠️⚠️ **LE TESSERE NON SI SOVRAPPONGONO, E QUESTO DIPENDE DAL CONTO**: le operazioni della
 *   Luce guardano **un pixel per volta**, quindi due tessere accostate non hanno nessuna
 *   cucitura. Chi aggiungesse un'operazione che guarda i vicini (una nitidezza, una chiarezza,
 *   una sfocatura) deve dare a ogni tessera un bordo di sovrapposizione e buttarlo via dopo, o
 *   sulle giunzioni comparirebbe una riga. È la cosa da guardare per prima quando i moduli
 *   cresceranno.
 *
 * ⚠️⚠️ **E PRIMA DI FIDARSI SI PROVA, con [works]**: se questo percorso non funziona su un
 * telefono (una scheda grafica che rifiuta il formato, un buffer che non arriva), quello che se
 * ne ricava è un'immagine **nera**, e scritta sul file prende il posto della fotografia. Una
 * prova su un quadrato di colore noto costa un millesimo di secondo e distingue 'non ha
 * funzionato' da 'è venuto nero davvero'.
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
        if (look.idle) return null
        if (!works()) return null
        val w = source.width
        val h = source.height
        if (w <= 0 || h <= 0) return null

        val out = try {
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            return null
        }
        val canvas = Canvas(out)

        var y = 0
        while (y < h) {
            var x = 0
            val th = minOf(TILE, h - y)
            while (x < w) {
                val tw = minOf(TILE, w - x)
                /*
                 * ⚠️ **La tessera si ritaglia dal sorgente invece di traslare lo shader**: uno
                 * shader che legge il bitmap intero costringerebbe comunque la scheda grafica a
                 * caricarlo tutto, che è esattamente quello che le tessere esistono per evitare.
                 */
                val piece = runCatching { Bitmap.createBitmap(source, x, y, tw, th) }
                    .getOrNull() ?: run { out.recycle(); return null }
                val done = draw(piece, look, tw, th)
                if (piece !== source) piece.recycle()
                if (done == null) {
                    out.recycle()
                    return null
                }
                canvas.drawBitmap(done, Rect(0, 0, tw, th), Rect(x, y, x + tw, y + th), null)
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
     * ⚠️⚠️ **TUTTO QUELLO CHE SI APRE QUI SI CHIUDE QUI**, e non è pignoleria: un
     * [HardwareRenderer] non chiuso tiene in vita un contesto grafico, e su un'immagine di dodici
     * tessere sarebbero dodici. Il `finally` copre anche la strada dell'errore, che è quella in
     * cui una perdita non si nota.
     */
    private fun draw(piece: Bitmap, look: Look, w: Int, h: Int): Bitmap? {
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
            val shader = lookShader(image as ComposeShader, look) ?: return null
            val paint = Paint().apply { this.shader = shader }

            val canvas = node.beginRecording()
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            node.endRecording()

            renderer.setContentRoot(node)
            /*
             * ⚠️ **`setWaitForPresent` è la riga che rende il disegno SINCRONO**: senza,
             * `syncAndDraw` torna prima che la scheda grafica abbia finito, e quello che si
             * legge dal buffer è mezzo vuoto. Il difetto sarebbe intermittente, che è il modo
             * peggiore in cui può presentarsi.
             */
            renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()

            val got = reader.acquireNextImage() ?: return null
            try {
                val buffer = got.hardwareBuffer ?: return null
                try {
                    val wrapped = Bitmap.wrapHardwareBuffer(
                        buffer, ColorSpace.get(ColorSpace.Named.SRGB)
                    ) ?: return null
                    // ⚠️ La copia serve: quello che arriva è una mappa di tipo HARDWARE, che
                    // non si può leggere pixel per pixel né comprimere.
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
     * ⚠️⚠️ **SI MISURA UNA VOLTA E SI RICORDA**: la risposta dipende dall'apparecchio e non
     * dall'immagine, quindi rifarla per ogni tessera costerebbe dodici disegni in più per
     * saperlo dodici volte. ⚠️ **Ma non si ricorda un `false` come definitivo**: la prima prova
     * potrebbe cadere su una scheda grafica occupata, e una risposta negativa ricordata per
     * sempre spegnerebbe l'editor completo fino al riavvio dell'app. Si ricorda il solo `true`.
     *
     * ⚠️ **La prova è un quadrato BIANCO con il conto a riposo**, cioè con tutti i cursori a
     * zero: là il programma deve restituire esattamente quello che ha ricevuto, quindi un
     * risultato diverso dal bianco dice che il percorso è rotto e non che il conto sbaglia.
     */
    fun works(): Boolean {
        if (proven) return true
        val probe = try {
            Bitmap.createBitmap(PROBE, PROBE, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            return false
        }
        probe.eraseColor(android.graphics.Color.WHITE)
        val done = draw(probe, Look.NONE, PROBE, PROBE)
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
     * ⚠️ **2048 è il minimo che la specifica grafica garantisce**, non una misura di comodo: da
     * OpenGL ES 3.0 in poi ogni apparecchio deve accettare una texture di questo lato, e Android
     * 13 non gira su niente di meno. Un numero più grande andrebbe bene quasi dappertutto, e
     * fallirebbe proprio sui telefoni deboli, cioè quelli su cui una fotografia grande è già il
     * caso difficile.
     */
    private const val TILE = 2048

    /** Il lato del quadrato di prova: più piccolo di così non si può leggere un pixel di mezzo. */
    private const val PROBE = 4

    /**
     * Quanto un canale deve essere vicino al pieno perché la prova sia passata.
     *
     * ⚠️ **Non si chiede 255 esatto**: fra il buffer della scheda grafica e la mappa di pixel c'è
     * una conversione di spazio colore, e un livello di scarto è normale. Un percorso rotto non dà
     * 254: dà zero.
     */
    private const val NEAR = 250
}

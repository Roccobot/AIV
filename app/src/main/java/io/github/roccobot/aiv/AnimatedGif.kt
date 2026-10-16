package io.github.roccobot.aiv

import android.graphics.Bitmap
import com.bumptech.glide.gifdecoder.GifDecoder
import com.bumptech.glide.gifdecoder.GifHeaderParser
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import java.nio.ByteBuffer

/**
 * Le GIF animate, lette dal decodificatore di Glide.
 *
 * Perché questa dipendenza: `com.github.bumptech.glide:gifdecoder` pesa circa 18 KB, non ha
 * librerie native né dipendenze transitive nuove, ed è sotto licenza BSD a due clausole. Sono 7
 * classi: se un giorno desse problemi, i sorgenti si copiano nel repo dietro [Animated] e nessun
 * chiamante cambia.
 * Le alternative non bastano: `android.graphics.Movie` non conta i fotogrammi e non vede quelli
 * con ritardo 0; `ImageDecoder` e `AnimatedImageDrawable` sanno riprodurre ma non contare né
 * cercare; `android-gif-drawable` e Fresco aggiungono librerie native (`.so`).
 */
class AnimatedGif private constructor(
    private val decoder: GifDecoder,
    private val pool: Pool
) : Animated {

    /**
     * Le ultime due tele consegnate da [current], la più vecchia in testa.
     *
     * ⚠️ **Due tele che si alternano**: il decodificatore non restituisce al serbatoio il
     * fotogramma che consegna, quindi lo fa il lettore. Prima di comporre il fotogramma nuovo torna
     * al serbatoio la tela di due giri fa: quella del giro prima è ancora in mostra, e riscriverla
     * la strapperebbe mentre si disegna. Rilievo C13, presidiato da `TeleGifTest`.
     */
    private val handed = ArrayDeque<Bitmap>(2)

    /**
     * Se [close] è già stato chiamato.
     *
     * ⚠️ **Un decodificatore chiuso va in errore invece di dirlo**: `GifDecoder.clear()` azzera
     * l'intestazione, e da lì `frameCount`, `getDelay` e `advance` cadono su un riferimento nullo,
     * dentro una coroutine che chiude l'app. Con questa bandierina chiudere due volte o usare un
     * lettore chiuso costa un fotogramma mancante; [AnimatedWebp] si comporta già così da sé.
     */
    private var closed = false

    override val width: Int get() = if (closed) 0 else decoder.width
    override val height: Int get() = if (closed) 0 else decoder.height
    override val frameCount: Int get() = if (closed) 0 else runCatching { decoder.frameCount }.getOrDefault(0)
    override val index: Int get() = if (closed) 0 else decoder.currentFrameIndex

    /**
     * `getNetscapeLoopCount` è il numero scritto nel file, con 0 per 'per sempre' come in
     * [Animated]. Senza l'estensione NETSCAPE il decodificatore risponde con un valore negativo,
     * che qui vale 'una volta sola'.
     */
    override val loopCount: Int
        get() = decoder.netscapeLoopCount.let { if (it < 0) 1 else it }

    override fun delayOf(index: Int): Int =
        if (closed) 0 else runCatching { decoder.getDelay(index) }.getOrDefault(0).coerceAtLeast(0)

    /**
     * ⚠️ **`getNextFrame` non è il prossimo: è il corrente.** `advance()` sposta l'indice e
     * `getNextFrame()` compone il fotogramma a quell'indice; chiamato due volte senza `advance()`
     * restituisce lo stesso fotogramma.
     * ⚠️ **Il Bitmap appartiene al lettore ed è riusato due giri dopo** ([handed]): chi lo vuole
     * tenere (l'esportazione, una cache) ne fa una copia.
     */
    override fun current(): Bitmap? {
        if (closed) return null
        if (handed.size == 2) pool.release(handed.removeFirst())
        val frame = runCatching { decoder.nextFrame }.getOrNull() ?: return null
        handed.addLast(frame)
        return frame
    }

    override fun advance() {
        if (closed) return
        runCatching { decoder.advance() }
    }

    /**
     * ⚠️ **`resetFrameIndex` da solo non basta**: riporta l'indice a -1, mentre [AnimatedWebp]
     * dopo il riavvolgimento è sul fotogramma 0. L'`advance` qui sotto pareggia i due lettori, o
     * ogni salto all'indietro sbaglierebbe di uno sulle sole GIF.
     * È privata perché la chiama solo [seek]: riavvolgere senza ricomporre la pila lascia la scia.
     */
    private fun rewind() {
        if (closed) return
        runCatching {
            decoder.resetFrameIndex()
            decoder.advance()
        }
    }

    /**
     * ⚠️ **Ogni passo chiede anche il fotogramma**: è la lettura a comporre la toppa sulla tela,
     * non `advance()`. Con i soli `advance()` il fotogramma d'arrivo si posa sopra quello che
     * c'era, e resta la scia. Il valore restituito non serve: serve l'effetto sulla tela interna.
     * All'indietro si riavvolge, perché la pila si può solo rifare da capo.
     * ⚠️ **Il ciclo conta i passi e non confronta gli indici**: `advance()` ricomincia dopo
     * l'ultimo fotogramma, e un `while (index != target)` potrebbe non finire mai.
     */
    override fun seek(target: Int) {
        if (closed) return
        runCatching {
            val quanti = decoder.frameCount
            if (quanti <= 0) return
            val dove = target.coerceIn(0, quanti - 1)
            if (dove < decoder.currentFrameIndex) {
                rewind()
                // ⚠️ Anche il fotogramma 0 va chiesto: è la sua lettura a ripulire la tela,
                // perché è il solo indice senza un precedente da conservare. Senza, la scia resta.
                decoder.nextFrame?.let(pool::release)
            }
            var passi = dove - decoder.currentFrameIndex
            if (passi < 0) passi = 0
            // Le tele composte di passaggio non le vede nessuno: tornano subito al serbatoio.
            repeat(passi) {
                decoder.advance()
                decoder.nextFrame?.let(pool::release)
            }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        runCatching { decoder.clear() }
    }

    companion object {

        /** Quante tele tiene al più il serbatoio: vedi [Pool]. */
        private const val SPARE = 3

        /**
         * Apre una GIF già letta in memoria, e `null` se non è leggibile.
         *
         * Si controlla lo stato dell'intestazione: su un file troncato il parser restituisce
         * comunque un `GifHeader`, con uno stato di errore e zero fotogrammi.
         */
        fun open(bytes: ByteArray): Animated? = runCatching {
            val buffer = ByteBuffer.wrap(bytes)
            val parser = GifHeaderParser().setData(buffer)
            val header = parser.parseHeader()
            parser.clear()
            if (header.status != GifDecoder.STATUS_OK || header.numFrames <= 0) return null
            val pool = Pool()
            val decoder = StandardGifDecoder(pool, header, buffer)
            // ⚠️ ARGB_8888: le GIF hanno la trasparenza a chiave, e in RGB_565 il colore
            // trasparente diventa un colore vero, di solito nero.
            decoder.setDefaultBitmapConfig(Bitmap.Config.ARGB_8888)
            decoder.advance()
            AnimatedGif(decoder, pool)
        }.getOrNull()
    }

    /**
     * Da dove il decodificatore prende i suoi Bitmap e i suoi vettori.
     *
     * Lo pretende l'interfaccia, e serve al riuso: allocare una tela grande quanto l'immagine
     * venti volte al secondo darebbe al garbage collector un lavoro che si vede come scatti.
     * ⚠️ **Il decodificatore non restituisce il fotogramma che consegna** (letto nel bytecode di
     * `StandardGifDecoder.setPixels`): restituisce solo la tela del fotogramma precedente. Le tele
     * consegnate le restituisce il lettore ([current], [seek]).
     * Al più [SPARE] tele: le due che si alternano e quella del precedente.
     * ⚠️ **Tele software, mai `HARDWARE`**: un Bitmap `HARDWARE` non si legge pixel per pixel né
     * si comprime su file, e l'esportazione ne ha bisogno.
     */
    private class Pool : GifDecoder.BitmapProvider {

        private val spare = ArrayDeque<Bitmap>(SPARE)

        override fun obtain(width: Int, height: Int, config: Bitmap.Config): Bitmap {
            val kept = spare.firstOrNull {
                !it.isRecycled && it.width == width && it.height == height && it.config == config
            }
            if (kept != null) {
                spare.remove(kept)
                // ⚠️ Si azzera prima di ridarla: il decodificatore disegna una toppa sopra quello
                // che trova, cioè il fotogramma di due giri fa.
                kept.eraseColor(0)
                return kept
            }
            DrawWork.canvases++
            return Bitmap.createBitmap(width, height, config)
        }

        override fun release(bitmap: Bitmap) {
            if (spare.any { it === bitmap }) return
            if (spare.size == SPARE) spare.removeFirst()
            spare.addLast(bitmap)
        }

        override fun obtainByteArray(size: Int): ByteArray = ByteArray(size)

        override fun release(bytes: ByteArray) = Unit

        override fun obtainIntArray(size: Int): IntArray = IntArray(size)

        override fun release(array: IntArray) = Unit
    }
}

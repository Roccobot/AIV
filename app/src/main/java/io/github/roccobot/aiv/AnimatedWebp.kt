package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect

/**
 * Le WebP animate, lette dal contenitore e ricomposte qui.
 *
 * ⚠️ **Android non dà i fotogrammi**: `ImageDecoder` sa dire se un file è animato e
 * `AnimatedImageDrawable` lo riproduce, ma nessuna API dice quanti fotogrammi ha, quanto durano,
 * né ne dà uno a scelta (verificato con `javap` su `android.jar`, API 37).
 *
 * Una WebP animata è un contenitore RIFF: ogni fotogramma è in un chunk `ANMF` con posizione,
 * misura, ritardo, i due metodi di composizione e i dati compressi, un blocco `VP8 ` o `VP8L`,
 * cioè il contenuto di una WebP ferma. Il contenitore si legge a mano (conti, ritardi,
 * ripetizioni sono numeri in chiaro); per i pixel si antepone al blocco compresso
 * un'intestazione WebP minima, e `BitmapFactory` decodifica il risultato come un file
 * qualunque. La decodifica resta al sistema, che ha `libwebp`.
 * La composizione ([compose], [decode]) è stata confrontata con un decodificatore di riferimento
 * su due WebP animate di prova, una opaca e una trasparente, quasi tutte toppe parziali con
 * `dispose to background` e fusione alfa: scarto massimo 0 su ogni pixel. La prova è girata sul
 * `libwebp` di un PC, non su quello di Android.
 *
 * ⚠️ **Il costo della scelta**: nessuna dipendenza, ma la composizione dei fotogrammi è scritta
 * qui, mentre per le GIF la fa il decodificatore di Glide. Se le WebP si vedono male dove le GIF
 * si vedono bene, il sospetto va cercato qui.
 */
class AnimatedWebp private constructor(
    private val bytes: ByteArray,
    override val width: Int,
    override val height: Int,
    override val loopCount: Int,
    private val frames: List<Frame>
) : Animated {

    /**
     * Un fotogramma dentro il contenitore: dov'è, quanto dura, e come va posato.
     *
     * @param at dove cominciano i dati compressi dentro [bytes].
     * @param size quanti byte sono.
     * @param clearBefore se, **finito** questo fotogramma, il suo riquadro va ripulito prima
     *   di disegnare il successivo (il metodo di disposal della specifica).
     * @param replace se questo fotogramma **sostituisce** i pixel sotto di sé invece di
     *   fondersi con loro (il metodo di blending).
     */
    class Frame(
        val x: Int,
        val y: Int,
        val w: Int,
        val h: Int,
        val delay: Int,
        val at: Int,
        val size: Int,
        val clearBefore: Boolean,
        val replace: Boolean
    )

    override val frameCount: Int get() = frames.size
    override fun delayOf(index: Int): Int = frames.getOrNull(index)?.delay ?: 0

    override var index: Int = 0
        private set

    /**
     * La tela su cui i fotogrammi si posano uno sopra l'altro.
     *
     * Una sola, riusata, e diversa da quelle che si consegnano ([outputs]): una per fotogramma
     * vorrebbe dire una tela grande quanto l'immagine venti volte al secondo (stessa scelta di
     * `AnimatedGif.Pool`).
     * È allocata alla prima richiesta: chi apre il file può scoprire che il fotogramma è uno solo
     * e richiuderlo (`Animations.open`) senza aver pagato la tela.
     */
    private var canvasBitmap: Bitmap? = null

    /** Quale fotogramma la tela sta mostrando adesso, e -1 se è vuota. */
    private var drawn: Int = -1

    /**
     * Le due tele che si consegnano a turno, e quale delle due è in mostra.
     *
     * ⚠️ **La pila non si consegna mai** (rilievo C14): il fotogramma dopo la riscrive su
     * `Dispatchers.Default` mentre il disegno può leggerla, per esempio durante una pizzicata.
     * La pila si compone su [canvasBitmap], che nessuno vede, e si copia nella tela che non è in
     * mostra (presidiato da `TeleWebpTest`).
     */
    private val outputs = arrayOfNulls<Bitmap>(2)
    private var shown = -1

    /**
     * La tela riusata in cui si decodifica una toppa, grande quanto l'immagine: evita
     * un'allocazione per fotogramma, come il serbatoio della GIF (rilievo C14).
     */
    private var patch: Bitmap? = null

    override fun current(): Bitmap? = runCatching {
        val tela = canvasBitmap
            ?: newCanvas().also {
                canvasBitmap = it
                drawn = -1
            }
        if (drawn == index && shown >= 0) return@runCatching outputs[shown]
        compose(tela)
        val next = if (shown == 0) 1 else 0
        val out = outputs[next] ?: newCanvas().also { outputs[next] = it }
        Canvas(out).drawBitmap(tela, 0f, 0f, COPY)
        shown = next
        out
    }.getOrNull()

    private fun newCanvas(): Bitmap {
        DrawWork.canvases++
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    override fun advance() {
        index = if (index + 1 >= frames.size) 0 else index + 1
    }

    /**
     * Qui basta spostare l'indice, al contrario della GIF: [compose] confronta il fotogramma
     * che la tela mostra con quello chiesto, e all'indietro ripulisce e rifà la pila da capo.
     */
    override fun seek(target: Int) {
        if (frames.isEmpty()) return
        index = target.coerceIn(0, frames.size - 1)
    }

    /**
     * ⚠️ **Le due tele d'uscita non si riciclano**: una può essere ancora in mano al disegno,
     * che andrebbe in errore. Le libera il raccoglitore.
     */
    override fun close() {
        canvasBitmap?.takeIf { !it.isRecycled }?.recycle()
        canvasBitmap = null
        patch?.takeIf { !it.isRecycled }?.recycle()
        patch = null
        outputs.fill(null)
        shown = -1
    }

    /**
     * Porta la tela dal fotogramma che mostra a quello chiesto.
     *
     * In avanti si prosegue, all'indietro si ricomincia: un fotogramma è una toppa sopra quello di
     * prima, quindi il fotogramma N esiste solo come risultato dei precedenti, e tornare indietro
     * di uno costa N. È una proprietà del formato: una cache dei fotogrammi composti andrebbe
     * sopra questa classe.
     */
    private fun compose(tela: Bitmap) {
        val canvas = Canvas(tela)
        var from = drawn + 1
        if (drawn > index || drawn < 0) {
            tela.eraseColor(0)
            from = 0
        }
        for (i in from..index) {
            // ⚠️ Il disposal è del fotogramma precedente e si applica prima di disegnare questo,
            // come dice la specifica: invertiti, cancellerebbero la toppa appena posata.
            frames.getOrNull(i - 1)?.takeIf { it.clearBefore }?.let { old ->
                canvas.drawRect(
                    Rect(old.x, old.y, old.x + old.w, old.y + old.h),
                    CLEAR
                )
            }
            val frame = frames[i]
            val piece = decode(frame) ?: continue
            if (frame.replace) {
                // Sostituzione: prima si vuota il riquadro, se no la trasparenza del fotogramma
                // nuovo lascerebbe vedere quello di sotto.
                canvas.drawRect(
                    Rect(frame.x, frame.y, frame.x + frame.w, frame.y + frame.h),
                    CLEAR
                )
            }
            canvas.drawBitmap(piece, frame.x.toFloat(), frame.y.toFloat(), null)
            if (piece !== patch) piece.recycle()
        }
        drawn = index
    }

    /**
     * I pixel di un fotogramma, decodificati dal sistema.
     *
     * Davanti al blocco compresso si scrivono 12 byte, `RIFF`, la lunghezza e `WEBP`, e il
     * risultato è un file WebP valido.
     * ⚠️ **Tranne quando il fotogramma ha un canale alfa a parte**: i dati cominciano con un
     * blocco `ALPH`, che va dichiarato in un chunk `VP8X`, o il decodificatore rifiuta il file.
     * Senza questo ramo le WebP animate con trasparenza perderebbero fotogrammi, in silenzio.
     */
    private fun decode(frame: Frame): Bitmap? = runCatching {
        val body = bytes.copyOfRange(frame.at, frame.at + frame.size)
        val alpha = body.size >= 4 && String(body, 0, 4, Charsets.US_ASCII) == "ALPH"
        val out = if (alpha) wrapWithAlpha(body, frame) else wrap(body)
        val riuso = patch ?: newCanvas().also { patch = it }
        val opzioni = BitmapFactory.Options().apply {
            inMutable = true
            inBitmap = riuso
        }
        // Se il sistema rifiuta la tela riusata (una configurazione diversa), si decodifica in
        // una tela nuova: un'allocazione in più, e il fotogramma c'è.
        runCatching { BitmapFactory.decodeByteArray(out, 0, out.size, opzioni) }.getOrNull()
            ?: BitmapFactory.decodeByteArray(out, 0, out.size).also { DrawWork.canvases++ }
    }.getOrNull()

    /** `RIFF` + lunghezza + `WEBP` + i dati: il file WebP più piccolo che esista. */
    private fun wrap(body: ByteArray): ByteArray {
        val out = ByteArray(12 + body.size)
        "RIFF".toByteArray(Charsets.US_ASCII).copyInto(out, 0)
        writeLe32(out, 4, 4 + body.size)
        "WEBP".toByteArray(Charsets.US_ASCII).copyInto(out, 8)
        body.copyInto(out, 12)
        return out
    }

    /** Come [wrap], ma con davanti il `VP8X` che dichiara il canale alfa. */
    private fun wrapWithAlpha(body: ByteArray, frame: Frame): ByteArray {
        val vp8x = ByteArray(18)
        "VP8X".toByteArray(Charsets.US_ASCII).copyInto(vp8x, 0)
        writeLe32(vp8x, 4, 10)
        vp8x[8] = 0x10                       // il solo bit acceso è quello dell'alfa
        writeLe24(vp8x, 12, frame.w - 1)     // la specifica scrive le misure meno uno
        writeLe24(vp8x, 15, frame.h - 1)
        val out = ByteArray(12 + vp8x.size + body.size)
        "RIFF".toByteArray(Charsets.US_ASCII).copyInto(out, 0)
        writeLe32(out, 4, 4 + vp8x.size + body.size)
        "WEBP".toByteArray(Charsets.US_ASCII).copyInto(out, 8)
        vp8x.copyInto(out, 12)
        body.copyInto(out, 12 + vp8x.size)
        return out
    }

    companion object {

        /** Il pennello che vuota un riquadro invece di dipingerlo. */
        private val CLEAR = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }

        /** Il pennello che copia la pila in una tela d'uscita, trasparenza compresa. */
        private val COPY = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        }

        /**
         * Legge il contenitore e ne ricava l'elenco dei fotogrammi.
         *
         * ⚠️ **I chunk RIFF sono allineati a due byte**: dopo un corpo di lunghezza dispari c'è
         * un byte di riempimento. Senza saltarlo la lettura perde il passo dal primo fotogramma
         * di misura dispari in poi.
         */
        fun open(bytes: ByteArray): Animated? = runCatching {
            if (bytes.size < 21) return null
            if (String(bytes, 0, 4, Charsets.US_ASCII) != "RIFF") return null
            if (String(bytes, 8, 4, Charsets.US_ASCII) != "WEBP") return null
            var pos = 12
            var width = 0
            var height = 0
            var loops = 0
            val frames = ArrayList<Frame>()
            while (pos + 8 <= bytes.size) {
                val fourcc = String(bytes, pos, 4, Charsets.US_ASCII)
                val size = readLe32(bytes, pos + 4)
                val body = pos + 8
                if (size < 0 || body + size > bytes.size) break
                when (fourcc) {
                    "VP8X" -> if (size >= 10) {
                        width = readLe24(bytes, body + 4) + 1
                        height = readLe24(bytes, body + 7) + 1
                    }
                    // Nel chunk ANIM i primi 4 byte sono il colore di sfondo, che qui non serve:
                    // la tela nasce trasparente, e lo sfondo lo decide il visualizzatore.
                    "ANIM" -> if (size >= 6) loops = readLe16(bytes, body + 4)
                    "ANMF" -> if (size >= 16) {
                        val flags = bytes[body + 15].toInt()
                        frames.add(
                            Frame(
                                // La specifica scrive la posizione in unità di due pixel.
                                x = readLe24(bytes, body) * 2,
                                y = readLe24(bytes, body + 3) * 2,
                                w = readLe24(bytes, body + 6) + 1,
                                h = readLe24(bytes, body + 9) + 1,
                                delay = readLe24(bytes, body + 12),
                                at = body + 16,
                                size = size - 16,
                                clearBefore = (flags and 0x01) != 0,
                                replace = (flags and 0x02) != 0
                            )
                        )
                    }
                }
                pos = body + size + (size and 1)
            }
            if (width <= 0 || height <= 0 || frames.isEmpty()) return null
            AnimatedWebp(bytes, width, height, loops, frames)
        }.getOrNull()

        private fun readLe16(b: ByteArray, at: Int): Int =
            (b[at].toInt() and 0xFF) or ((b[at + 1].toInt() and 0xFF) shl 8)

        private fun readLe24(b: ByteArray, at: Int): Int =
            (b[at].toInt() and 0xFF) or
                ((b[at + 1].toInt() and 0xFF) shl 8) or
                ((b[at + 2].toInt() and 0xFF) shl 16)

        private fun readLe32(b: ByteArray, at: Int): Int =
            readLe24(b, at) or ((b[at + 3].toInt() and 0xFF) shl 24)

        private fun writeLe24(b: ByteArray, at: Int, value: Int) {
            b[at] = (value and 0xFF).toByte()
            b[at + 1] = ((value shr 8) and 0xFF).toByte()
            b[at + 2] = ((value shr 16) and 0xFF).toByte()
        }

        private fun writeLe32(b: ByteArray, at: Int, value: Int) {
            writeLe24(b, at, value)
            b[at + 3] = ((value shr 24) and 0xFF).toByte()
        }
    }
}

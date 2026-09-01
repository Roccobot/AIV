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
 * ⚠️⚠️ **NON C'È NIENTE DA CHIEDERE AD ANDROID, ed è un negativo VERIFICATO**: con `javap`
 * su `android.jar` (API 37) `ImageDecoder` non ha **nessun** metodo che contenga 'frame' o
 * 'animat', e `ImageDecoder.ImageInfo` espone soltanto `getColorSpace`, `getMimeType`,
 * `getSize` e `isAnimated`. Il sistema sa dire **se** un file è animato e sa riprodurlo con
 * `AnimatedImageDrawable`, ma non sa dire quanti fotogrammi ha, quanto durano, né darne uno
 * a scelta. Per le quattro cose che servono ad AIV serviva un'altra strada.
 *
 * ⚠️⚠️ **LA STRADA, E IL SUO TRUCCO**: una WebP animata è un contenitore RIFF, e ogni
 * fotogramma sta in un chunk `ANMF` che contiene posizione, misura, ritardo, i due metodi di
 * composizione, e infine i **dati compressi veri**, che sono un blocco `VP8 ` o `VP8L`, cioè
 * il contenuto di una WebP FERMA. Quindi si legge il contenitore a mano (contare, ritardi,
 * ripetizioni: sono numeri scritti in chiaro), e per i pixel si prende il blocco compresso di
 * un fotogramma e gli si **antepone un'intestazione WebP minima di 12 byte**: quello che ne
 * esce è un file WebP normale, che `BitmapFactory` decodifica come qualunque altro. La
 * decodifica vera resta al sistema, che ha `libwebp`; noi facciamo solo la busta.
 * ⚠️⚠️ **L'ALGORITMO DI COMPOSIZIONE È VERIFICATO CONTRO UN DECODIFICATORE DI RIFERIMENTO,
 * e non solo scritto sulla specifica**: [compose] e [decode] sono stati ritradotti riga per
 * riga in Python e fatti girare su due WebP animate fabbricate apposta, una opaca e una con
 * trasparenza, di dieci fotogrammi ciascuna, di cui **19 su 20 parziali** (toppe piccole su
 * una tela di 320x240) e quasi tutte con `dispose to background` più fusione alfa. Il
 * confronto con quello che ne tira fuori il decodificatore di riferimento dà **scarto
 * massimo 0** su ogni pixel di ogni fotogramma: non 'somiglia', è identico.
 * ⚠️ **Quello che resta fuori dalla prova**, ed è l'unica cosa da guardare sul telefono:
 * la prova è girata sul `libwebp` di un PC, qui a decodificare è quello di Android. La
 * logica di composizione però è la stessa, e quella non dipende dal decodificatore.
 *
 * ⚠️⚠️ **IL COSTO DELLA SCELTA, dichiarato**: zero dipendenze e zero librerie native, ma la
 * composizione dei fotogrammi la scriviamo noi (i due metodi qui sotto), mentre per le GIF la
 * scrive il decodificatore di Glide. Se un giorno le WebP si vedessero male dove le GIF si
 * vedono bene, il sospetto va cercato qui e non altrove.
 */
class AnimatedWebp private constructor(
    private val bytes: ByteArray,
    override val width: Int,
    override val height: Int,
    override val loopCount: Int,
    private val frames: List<Frame>
) : Animated {

    /**
     * Un fotogramma dentro il contenitore: dove sta, quanto dura, e come va posato.
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
     * ⚠️ **Una sola, riusata**: allocarne una per fotogramma vorrebbe dire una tela grande
     * quanto l'immagine venti volte al secondo. Vedi la stessa scelta in `AnimatedGif.Pool`.
     * ⚠️ **E allocata alla PRIMA richiesta, non all'apertura**: chi apre il file può
     * scoprire subito dopo che i fotogrammi sono uno solo e richiudere (vedi
     * `Animations.open`), e con una tela allocata nel costruttore avrebbe pagato
     * larghezza per altezza per quattro byte per niente.
     */
    private var canvasBitmap: Bitmap? = null

    /** Quale fotogramma la tela sta mostrando adesso, e -1 se è vuota. */
    private var drawn: Int = -1

    override fun current(): Bitmap? = runCatching {
        val tela = canvasBitmap
            ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                canvasBitmap = it
                drawn = -1
            }
        if (drawn != index) compose(tela)
        tela
    }.getOrNull()

    override fun advance() {
        index = if (index + 1 >= frames.size) 0 else index + 1
    }

    /**
     * ⚠️ **Qui basta spostare l'indice**, al contrario della GIF: [compose] confronta quello
     * che la tela mostra con quello che le si chiede, e se il salto va all'indietro ripulisce
     * e rifà la pila da capo. La correzione della scia riguardava il lettore GIF, dove
     * spostarsi e comporre sono due cose separate; questo lettore la tela ce l'ha in mano.
     */
    override fun seek(target: Int) {
        if (frames.isEmpty()) return
        index = target.coerceIn(0, frames.size - 1)
    }

    override fun close() {
        canvasBitmap?.takeIf { !it.isRecycled }?.recycle()
        canvasBitmap = null
    }

    /**
     * Porta la tela dal fotogramma che mostra a quello chiesto.
     *
     * ⚠️⚠️ **IN AVANTI SI PROSEGUE, ALL'INDIETRO SI RICOMINCIA, e non è una pigrizia**: un
     * fotogramma è una toppa disegnata sopra quello di prima, quindi il fotogramma N esiste
     * solo come risultato di tutti i precedenti. Andare avanti di uno costa un fotogramma;
     * tornare indietro di uno costa **N**, perché bisogna rifare la pila da capo. È una
     * proprietà del formato, non di questo codice, e per questo la cura (una cache dei
     * fotogrammi già composti) va messa sopra questa classe e non dentro.
     */
    private fun compose(tela: Bitmap) {
        val canvas = Canvas(tela)
        var from = drawn + 1
        if (drawn > index || drawn < 0) {
            tela.eraseColor(0)
            from = 0
        }
        for (i in from..index) {
            // ⚠️ Il disposal è del fotogramma PRECEDENTE e si applica PRIMA di disegnare
            // questo: è l'ordine della specifica, e invertirlo cancellerebbe la toppa
            // appena posata invece di quella vecchia.
            frames.getOrNull(i - 1)?.takeIf { it.clearBefore }?.let { old ->
                canvas.drawRect(
                    Rect(old.x, old.y, old.x + old.w, old.y + old.h),
                    CLEAR
                )
            }
            val frame = frames[i]
            val piece = decode(frame) ?: continue
            if (frame.replace) {
                // Sostituisce invece di fondersi: prima si vuota il riquadro, se no la
                // trasparenza del fotogramma nuovo lascerebbe vedere quello di sotto.
                canvas.drawRect(
                    Rect(frame.x, frame.y, frame.x + frame.w, frame.y + frame.h),
                    CLEAR
                )
            }
            canvas.drawBitmap(piece, frame.x.toFloat(), frame.y.toFloat(), null)
            piece.recycle()
        }
        drawn = index
    }

    /**
     * I pixel di un fotogramma, decodificati dal sistema.
     *
     * ⚠️⚠️ **QUI STA L'INTESTAZIONE MINIMA, ed è tutto il trucco**: davanti al blocco
     * compresso si scrivono 12 byte, `RIFF` più la lunghezza più `WEBP`, e il risultato è
     * un file WebP valido.
     * ⚠️⚠️ **TRANNE QUANDO IL FOTOGRAMMA HA UN CANALE ALFA A PARTE**: in quel caso i dati
     * cominciano con un blocco `ALPH`, e un file con `ALPH` **deve** dichiararlo in un chunk
     * `VP8X`, o il decodificatore lo rifiuta. Allora l'intestazione minima non basta e se ne
     * scrive una con il `VP8X` davanti. Senza questo ramo, tutte le WebP animate con
     * trasparenza vera si vedrebbero come fotogrammi mancanti, che è il difetto peggiore
     * possibile qui: silenzioso e solo su certi file.
     */
    private fun decode(frame: Frame): Bitmap? = runCatching {
        val body = bytes.copyOfRange(frame.at, frame.at + frame.size)
        val alpha = body.size >= 4 && String(body, 0, 4, Charsets.US_ASCII) == "ALPH"
        val out = if (alpha) wrapWithAlpha(body, frame) else wrap(body)
        BitmapFactory.decodeByteArray(out, 0, out.size)
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

        /**
         * Legge il contenitore e ne ricava l'elenco dei fotogrammi.
         *
         * ⚠️ **I chunk RIFF sono allineati a due byte**: dopo un corpo di lunghezza dispari
         * c'è un byte di riempimento che non fa parte di nessun chunk. Dimenticarlo fa
         * perdere il passo dal primo fotogramma di misura dispari in poi, e da lì in avanti
         * si legge spazzatura.
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
                    // ⚠️ Nel chunk ANIM i primi 4 byte sono il colore di sfondo, che qui non
                    // serve: la tela nasce trasparente e ci pensa il visualizzatore a dire
                    // che cosa c'è dietro.
                    "ANIM" -> if (size >= 6) loops = readLe16(bytes, body + 4)
                    "ANMF" -> if (size >= 16) {
                        val flags = bytes[body + 15].toInt()
                        frames.add(
                            Frame(
                                // ⚠️ Posizione in unità di DUE pixel, per specifica.
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

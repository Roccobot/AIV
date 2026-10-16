package io.github.roccobot.aiv

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Il tasto **'Auto'** dell'editor completo: che cosa scrivere nei cursori guardando l'immagine.
 *
 * Imita 'Colore automatico' di Photoshop, annullabile (richiesta dell'utente, giro della 2.31):
 * porta gli estremi dell'intervallo tonale al nero e al bianco e toglie la dominante. Sistema
 * anche la luce media, con esposizione e contrasto (risposta `piu` a `d-auto-quanto`): in tutto
 * sei cursori, e nessun altro si muove.
 *
 * ⚠️ **I sei si calcolano nell'ordine in cui la catena li applica**: nello shader l'esposizione
 * viene prima dei punti, quindi prima il guadagno dalla mediana, poi i due estremi già esposti,
 * per ultimo il contrasto sulla distribuzione che ne risulta. Calcolati tutti sull'immagine di
 * partenza, i punti di bianco e di nero mancherebbero il bersaglio.
 *
 * ⚠️ **Scrive nei cursori e non dipinge niente**: il risultato è un [Look] come un altro, quindi
 * entra nella storia dei passi e 'Annulla' lo disfa. I valori compaiono sui cursori, e si può
 * continuare a mano da lì.
 *
 * I sei cursori si sostituiscono, non si sommano: 'Auto' dice dove dovrebbero essere, a
 * prescindere da dove erano, e due tocchi di fila danno la stessa immagine.
 */
object Auto {

    /**
     * Quanta parte dei pixel si lascia fuori da ogni estremo, cioè il *clip* di Photoshop.
     *
     * ⚠️ Senza il taglio un solo punto nero (un granello di rumore, la cornice di una scansione)
     * porterebbe il percentile basso a zero, e il punto di nero non si muoverebbe. Photoshop
     * taglia mezzo per cento da ogni parte.
     */
    const val CLIP = 0.005f

    /**
     * Quanti punti si leggono dall'immagine, per lato, e non di più.
     *
     * Basta un campione: percentili e medie su centomila punti valgono quanto su due milioni, e
     * leggere l'anteprima intera costerebbe una pausa visibile a ogni tocco.
     */
    const val PROBE = 320

    /**
     * I sei valori che 'Auto' scrive dentro [look], guardando i pixel [pixels].
     *
     * I pixel arrivano già campionati ([probe]): la funzione è pura, e il banco la prova senza
     * aprire file.
     */
    fun tuned(look: Look, pixels: IntArray): Look {
        if (pixels.isEmpty()) return look
        val conto = histogram(pixels)
        val n = pixels.size
        val neutro = grey(pixels)
        val lo = percentile(conto, n, CLIP) / 255f
        val hi = percentile(conto, n, 1f - CLIP) / 255f
        val stop = exposureFor(conto, n)
        val gain = 2f.pow(stop)
        /*
         * ⚠️ **Un'immagine piatta non si stira**: con i due percentili quasi uguali, allungare
         * l'intervallo trasformerebbe una nebbia in un mosaico di due colori. L'esposizione
         * resta: una nebbia scura si porta comunque al grigio.
         */
        if (hi - lo < FLAT) {
            return look.copy(
                light = look.light.copy(exposure = stop, contrast = 0f, blacks = 0f, whites = 0f),
                chroma = look.chroma.copy(temp = neutro.first, tint = neutro.second)
            )
        }
        val loG = shifted(lo, gain)
        val hiG = shifted(hi, gain)
        val blacks = (-loG / POINT_SHIFT).coerceIn(-1f, 1f)
        val whites = ((1f - hiG) / POINT_SHIFT).coerceIn(-1f, 1f)
        return look.copy(
            light = look.light.copy(
                exposure = stop,
                contrast = contrastFor(conto, n, gain, loG, hiG),
                blacks = blacks,
                whites = whites
            ),
            chroma = look.chroma.copy(temp = neutro.first, tint = neutro.second)
        )
    }

    /**
     * I pixel di [src] letti a passo largo: al massimo [PROBE] per lato.
     *
     * Vive qui perché è la metà impura dello stesso conto: trasforma in numeri il bitmap di chi
     * chiama 'Auto'.
     */
    fun probe(src: Bitmap): IntArray {
        val passo = max(1, max(src.width, src.height) / PROBE)
        val w = (src.width + passo - 1) / passo
        val h = (src.height + passo - 1) / passo
        if (w <= 0 || h <= 0) return IntArray(0)
        val out = IntArray(w * h)
        var i = 0
        var y = 0
        while (y < src.height) {
            var x = 0
            while (x < src.width) {
                out[i++] = src.getPixel(x, y)
                x += passo
            }
            y += passo
        }
        return if (i == out.size) out else out.copyOf(i)
    }

    /**
     * Quanti pixel cadono su ognuno dei 256 livelli di **luminanza percettiva**.
     *
     * ⚠️ **Sulla luminanza, non per canale**, al contrario di Photoshop, che toglie la dominante
     * stirando ogni canale: qui i punti sono due cursori soli, e la dominante la toglie il
     * bilanciamento, l'altra metà di [tuned]. Stirando anche per canale le due correzioni si
     * sommerebbero sullo stesso difetto.
     * La tabella la leggono i punti, l'esposizione e il contrasto: si calcola una volta sola.
     */
    private fun histogram(pixels: IntArray): IntArray {
        val conto = IntArray(256)
        for (p in pixels) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val y = (r * 54 + g * 183 + b * 19) shr 8
            conto[y.coerceIn(0, 255)]++
        }
        return conto
    }

    /**
     * Il cursore dell'esposizione, in stop, che porta la **mediana** al grigio di mezzo.
     *
     * ⚠️ **Il bersaglio è `MID` perché è il perno del contrasto**: la curva a S dello shader
     * lavora attorno a `0,5`, e una mediana spostata darebbe una S che allarga da una parte e
     * schiaccia dall'altra.
     * La mediana, non la media: un cielo bianco o un fondo nero spostano la media di parecchi
     * livelli.
     * ⚠️ **Il conto è l'inversa di `gain`**, che moltiplica la luce lineare:
     * `2^stop = linear(MID) / linear(m)`. In sRGB il guadagno sarebbe sbagliato di molto.
     * La corsa è quella del cursore ([Light.EXPOSURE_RANGE]): un'immagine quasi nera chiederebbe
     * più stop di quanti il cursore ne possa mostrare.
     */
    private fun exposureFor(conto: IntArray, totale: Int): Float {
        val m = percentile(conto, totale, 0.5f) / 255f
        val luce = max(linear(m), TINY)
        val stop = (kotlin.math.ln(linear(MID) / luce) / kotlin.math.ln(2.0)).toFloat()
        return stop.coerceIn(-Light.EXPOSURE_RANGE, Light.EXPOSURE_RANGE)
    }

    /**
     * Il cursore del contrasto, dalla **dispersione** dei toni dopo l'esposizione e i punti.
     *
     * ⚠️ **Si misura dove il cursore agirà**, sull'immagine già esposta e stirata: il contrasto è
     * il quarto passo della catena, e sull'immagine di partenza la dispersione risulterebbe quasi
     * sempre troppo bassa.
     * ⚠️ **Il legame fra `k` e la dispersione è la pendenza al perno**: `sCurve` posa sulla
     * diagonale una gobba che al perno vale [CONTRAST_RISE] per `k`, quindi la pendenza è
     * `1 + k * CONTRAST_RISE`, e portare la dispersione da `s` a [SPREAD] chiede
     * `k = (SPREAD / s - 1) / CONTRAST_RISE`. Chi cambia la curva cambia questo conto.
     * È un'approssimazione del primo ordine: lontano dal perno la curva è più piatta della
     * tangente, quindi il contrasto è semmai un po' timido, il verso giusto per un comando
     * automatico.
     * Il tetto [AUTO_K] lo tiene innocuo: mezza corsa corregge un'immagine piatta, oltre si
     * deciderebbe l'aspetto dell'immagine.
     * Va solo in su, come in Photoshop: un'immagine più dispersa del bersaglio ha un carattere, non
     * un difetto.
     */
    private fun contrastFor(conto: IntArray, totale: Int, gain: Float, lo: Float, hi: Float): Float {
        val ampiezza = max(hi - lo, TINY.toFloat())
        var somma = 0.0
        var quadri = 0.0
        for (i in conto.indices) {
            val quanti = conto[i]
            if (quanti == 0) continue
            val v = ((shifted(i / 255f, gain) - lo) / ampiezza).coerceIn(0f, 1f).toDouble()
            somma += quanti * v
            quadri += quanti * v * v
        }
        val media = somma / totale
        val sparso = sqrt(max(quadri / totale - media * media, 0.0)).toFloat()
        if (sparso < TINY) return 0f
        return ((SPREAD / sparso - 1f) / CONTRAST_RISE).coerceIn(0f, AUTO_K)
    }

    /** Il livello [v] dopo un'esposizione di [gain]: la stessa moltiplicazione, in luce lineare. */
    private fun shifted(v: Float, gain: Float): Float =
        srgb((linear(v) * gain).coerceIn(0.0, 1.0))

    /**
     * I due cursori del bilanciamento, cioè `temp` e `tint`, dalla dominante media.
     *
     * ⚠️ **La media si fa in luce lineare**, dove il bilanciamento moltiplica: in sRGB due
     * immagini con la stessa dominante ed esposizioni diverse darebbero correzioni diverse.
     * ⚠️ **Il conto è l'inversa di `balance`, con due rapporti**: là i moltiplicatori sono
     * `(1 + w*WB_REACH, 1 + g*WB_REACH, 1 - w*WB_REACH)` normalizzati sulla luminanza, quindi la
     * temperatura è il rapporto fra il primo e il terzo, la tinta quello del secondo con la loro
     * media geometrica, e la normalizzazione si semplifica.
     * L'ipotesi è quella di ogni bilanciamento automatico, cioè che la media sia grigia: su un
     * tramonto è falsa, e il comando si annulla con un tocco.
     */
    private fun grey(pixels: IntArray): Pair<Float, Float> {
        var sr = 0.0
        var sg = 0.0
        var sb = 0.0
        for (p in pixels) {
            sr += linear(((p shr 16) and 0xFF) / 255f)
            sg += linear(((p shr 8) and 0xFF) / 255f)
            sb += linear((p and 0xFF) / 255f)
        }
        val n = pixels.size
        val mr = max(sr / n, TINY).toFloat()
        val mg = max(sg / n, TINY).toFloat()
        val mb = max(sb / n, TINY).toFloat()
        // Il rapporto che la temperatura deve produrre fra il rosso e il blu.
        val rb = mb / mr
        val temp = ((rb - 1f) / (WB_REACH * (1f + rb))).coerceIn(-1f, 1f)
        val w = temp * WB_REACH
        val medio = sqrt(max((1f + w) * (1f - w), TINY.toFloat()))
        val tint = ((medio * sqrt(mr * mb) / mg - 1f) / WB_REACH).coerceIn(-1f, 1f)
        return temp to tint
    }

    /** Il livello sotto cui cade la frazione [quanta] dei pixel. */
    private fun percentile(conto: IntArray, totale: Int, quanta: Float): Int {
        val soglia = (totale * quanta).toInt().coerceIn(0, totale)
        var somma = 0
        for (i in conto.indices) {
            somma += conto[i]
            if (somma > soglia) return i
        }
        return 255
    }

    /** Da sRGB a luce lineare, con la curva vera: è la gemella di `toLinear` dello shader. */
    private fun linear(v: Float): Double {
        val x = v.toDouble()
        return if (x <= 0.04045) x / 12.92 else ((x + 0.055) / 1.055).pow(2.4)
    }

    /** Il ritorno da luce lineare a sRGB, cioè l'inversa esatta di [linear]. */
    private fun srgb(x: Double): Float =
        (if (x <= 0.0031308) x * 12.92 else 1.055 * x.pow(1.0 / 2.4) - 0.055).toFloat()

    /**
     * Di quanto un cursore dei punti sposta il proprio estremo: è `POINT_SHIFT` dello shader.
     *
     * ⚠️ **Le tre costanti qui sotto sono copie di valori di `LOOK_AGSL`**, che il compilatore
     * tratta come testo: cambiarne uno di là lascerebbe questo conto sul numero vecchio, senza
     * errori. Le tiene d'accordo `SviluppoTest`, che legge la stringa e confronta. Chi ne aggiunge
     * una quarta la mette qui e nella prova. Sono `internal` per la prova.
     */
    internal const val POINT_SHIFT = 0.25f

    /** Quanto arriva il bilanciamento: è `WB_REACH` dello shader, e vale la nota qui sopra. */
    internal const val WB_REACH = 0.3f

    /**
     * Di quanto la curva del contrasto alza la pendenza al perno, a fondo corsa: è `CONTRAST_RISE`
     * dello shader, e vale la nota qui sopra. Lo legge [contrastFor], che dalla dispersione dei
     * toni ricava il valore del cursore.
     */
    internal const val CONTRAST_RISE = 1f

    /** Sotto questa distanza fra i due percentili l'immagine si dichiara piatta: vedi [tuned]. */
    private const val FLAT = 0.02f

    /** Il tono a cui l'esposizione porta la mediana: vedi [exposureFor]. */
    private const val MID = 0.5f

    /**
     * La dispersione dei toni a cui punta il contrasto automatico.
     *
     * Il numero è fra due misurabili: una distribuzione uniforme su tutto l'intervallo vale
     * `0,289`, un'immagine addensata attorno al centro resta sotto `0,18`. [AUTO_K] lo tiene
     * comunque innocuo.
     */
    private const val SPREAD = 0.25f

    /** Quanta corsa del contrasto il conto automatico può spendere: vedi [contrastFor]. */
    private const val AUTO_K = 0.5f

    /** Il minimo con cui si divide, per non dividere mai per zero. */
    private const val TINY = 1e-6
}

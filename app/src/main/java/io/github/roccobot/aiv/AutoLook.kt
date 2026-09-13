package io.github.roccobot.aiv

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Il tasto **'Auto'** dell'editor completo: che cosa scrivere nei cursori guardando l'immagine.
 *
 * ⚠️⚠️ **È 'COLORE AUTOMATICO' DI PHOTOSHOP, ED È SUA RICHIESTA ALLA LETTERA** (campo libero del
 * giro della `2.31`: *aggiungi un tasto 'Auto' che imita 'Colore automatico' di Photoshop ...
 * dev'essere annullabile*). Quel comando fa **due** cose e non una: porta i due estremi
 * dell'intervallo tonale al nero e al bianco, e **toglie la dominante** neutralizzando i tre canali
 * uno per uno. Qui le due cose diventano quattro cursori, e nessun altro si muove.
 *
 * ⚠️⚠️ **SCRIVE NEI CURSORI E NON DIPINGE NIENTE, ED È QUELLO CHE LO RENDE ANNULLABILE**: il
 * risultato è un [Look] come un altro, quindi entra nella storia dei passi come un gesto qualunque
 * e 'Annulla' lo disfa. Un comando che avesse toccato i pixel avrebbe avuto bisogno di una strada
 * sua per tornare indietro, e di un secondo posto in cui dire che l'immagine è cambiata.
 * ⚠️ **E si vede**: i quattro valori compaiono sui cursori dei due moduli, quindi dopo un 'Auto' si
 * può continuare a mano da dove il conto è arrivato.
 *
 * ⚠️⚠️ **I QUATTRO CURSORI SI SOSTITUISCONO INVECE DI SOMMARSI**: 'Auto' risponde alla domanda
 * *dove dovrebbero stare questi quattro*, e quella risposta non dipende da dove stavano prima.
 * Sommando, due tocchi di fila darebbero due immagini diverse e il secondo non vorrebbe dire
 * niente. ⚠️ **Gli altri cursori non si toccano**: chi ha già scelto un contrasto o una vividezza
 * li tiene, perché il comando che ha chiesto lui riguarda i punti e la dominante.
 */
object Auto {

    /**
     * Quanta parte dei pixel si lascia fuori da ogni estremo, cioè il *clip* di Photoshop.
     *
     * ⚠️⚠️ **SENZA IL TAGLIO BASTA UN PIXEL PER ROVINARE IL CONTO**: un singolo punto nero (un
     * granello di rumore, la cornice di una scansione) porterebbe il percentile basso a zero, e
     * allora il punto di nero non si muoverebbe affatto. Photoshop taglia mezzo per cento da ogni
     * parte, ed è il numero che si ritrova in ogni pannello di livelli automatici.
     */
    const val CLIP = 0.005f

    /**
     * Quanti punti si leggono dall'immagine, per lato, e non di più.
     *
     * ⚠️⚠️ **SI CAMPIONA INVECE DI LEGGERE TUTTO, E IL CONTO NON CAMBIA**: quello che si misura
     * sono percentili e medie, cioè due statistiche che su duecentomila punti valgono quanto su
     * due milioni. Leggere un'anteprima intera a ogni tocco costerebbe una pausa visibile per una
     * cifra che non si muoverebbe di un livello.
     */
    const val PROBE = 320

    /**
     * I quattro valori che 'Auto' scrive dentro [look], guardando i pixel [pixels].
     *
     * ⚠️ **I pixel arrivano già campionati** (vedi [probe]): questa funzione è pura, quindi il banco
     * la può misurare senza aprire nessun file.
     */
    fun tuned(look: Look, pixels: IntArray): Look {
        if (pixels.isEmpty()) return look
        val punti = points(pixels)
        val neutro = grey(pixels)
        return look.copy(
            light = look.light.copy(blacks = punti.first, whites = punti.second),
            chroma = look.chroma.copy(temp = neutro.first, tint = neutro.second)
        )
    }

    /**
     * I pixel di [src] letti a passo largo: al massimo [PROBE] per lato.
     *
     * ⚠️ **Vive qui e non nella schermata** perché è la metà impura dello stesso conto: chi chiama
     * 'Auto' ha un bitmap in mano, e questa riga è quella che lo trasforma in numeri.
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
     * I due cursori dei punti, cioè `blacks` e `whites`, dai percentili della **luminanza**.
     *
     * ⚠️⚠️ **IL CONTO È L'INVERSA DELLA FORMULA DELLO SHADER, E NON UNA TARATURA**: là l'intervallo
     * si ridisegna fra `floorAt = -blacks * POINT_SHIFT` e `ceilAt = 1 - whites * POINT_SHIFT`,
     * quindi per portare il percentile basso a zero serve `floorAt = lo`, cioè
     * `blacks = -lo / POINT_SHIFT`. Scritto come una taratura a occhio, il giorno che quel numero
     * dello shader cambiasse i due conti direbbero due cose diverse.
     * ⚠️ **Si misura sulla LUMINANZA e non per canale**, al contrario di Photoshop: là ogni canale
     * si stira per conto suo, e quello **è** il modo in cui toglie la dominante; qui i punti sono
     * due cursori soli, e la dominante la toglie il bilanciamento, che è l'altra metà di [tuned].
     * Stirando anche per canale, le due correzioni si sommerebbero sullo stesso difetto.
     */
    private fun points(pixels: IntArray): Pair<Float, Float> {
        val conto = IntArray(256)
        for (p in pixels) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val y = (r * 54 + g * 183 + b * 19) shr 8
            conto[y.coerceIn(0, 255)]++
        }
        val lo = percentile(conto, pixels.size, CLIP) / 255f
        val hi = percentile(conto, pixels.size, 1f - CLIP) / 255f
        /*
         * ⚠️ **Un'immagine piatta non si stira**: se i due percentili si toccano, allungare
         * l'intervallo vorrebbe dire moltiplicare per un numero enorme quello che c'è in mezzo,
         * cioè trasformare una nebbia in un mosaico di due colori.
         */
        if (hi - lo < FLAT) return 0f to 0f
        val blacks = (-lo / POINT_SHIFT).coerceIn(-1f, 1f)
        val whites = ((1f - hi) / POINT_SHIFT).coerceIn(-1f, 1f)
        return blacks to whites
    }

    /**
     * I due cursori del bilanciamento, cioè `temp` e `tint`, dalla dominante media.
     *
     * ⚠️⚠️ **LA MEDIA SI FA IN LUCE LINEARE, PERCHÉ È LÀ CHE IL BILANCIAMENTO MOLTIPLICA**: una
     * media di valori sRGB è la media di numeri passati per una curva, quindi due immagini con la
     * stessa dominante ma esposizioni diverse darebbero due correzioni diverse.
     * ⚠️⚠️ **E IL CONTO È L'INVERSA DI `balance`, CIOÈ DUE RAPPORTI E NON DUE DIFFERENZE**: là i
     * moltiplicatori sono `(1 + w*WB_REACH, 1 + g*WB_REACH, 1 - w*WB_REACH)` normalizzati sulla
     * luminanza, quindi quello che conta è il **rapporto** fra il primo e il terzo (che è la
     * temperatura) e quello del secondo con la loro media geometrica (che è la tinta). La
     * normalizzazione si semplifica da sé: due rapporti non la vedono.
     * ⚠️ **L'ipotesi dichiarata è quella di ogni bilanciamento automatico**: che la media
     * dell'immagine debba essere grigia. Su un tramonto è falsa, e là 'Auto' toglie il caldo che
     * uno voleva: è la ragione per cui il comando si annulla con un tocco.
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

    /**
     * Di quanto un cursore dei punti sposta il proprio estremo: è `POINT_SHIFT` dello shader.
     *
     * ⚠️ **È ricopiato e la copia si dichiara**, perché quel valore vive dentro una stringa di
     * programma e da Kotlin non si può leggere. A tenerli d'accordo è la prova del banco, che
     * misura il giro completo: `tuned` su un'immagine nota, e i punti che ne escono.
     */
    private const val POINT_SHIFT = 0.25f

    /** Quanto arriva il bilanciamento: è `WB_REACH` dello shader, e vale la nota qui sopra. */
    private const val WB_REACH = 0.3f

    /** Sotto questa distanza fra i due percentili l'immagine si dichiara piatta: vedi [points]. */
    private const val FLAT = 0.02f

    /** Il minimo con cui si divide, per non dividere mai per zero. */
    private const val TINY = 1e-6
}

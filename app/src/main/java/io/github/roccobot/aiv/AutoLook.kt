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
 * uno per uno.
 *
 * ⚠️⚠️ **E DALLA `2.34` SISTEMA ANCHE LA LUCE MEDIA, CHE È LA SUA RISPOSTA `piu` A
 * `d-auto-quanto`** (giro della `2.32`: *che tocchi anche esposizione e contrasto*, cioè *un colpo
 * solo che sistema anche la luce media, in un tasto solo*). Photoshop quelle due le tiene in due
 * comandi separati ('Tono automatico' e 'Contrasto automatico'); qui la domanda gli chiedeva se
 * accorparle, e la risposta è sì. I cursori mossi passano da quattro a **sei**, e nessun altro si
 * muove.
 *
 * ⚠️⚠️ **I SEI SI CALCOLANO NELL'ORDINE IN CUI LA CATENA LI APPLICA, E SENZA QUELLA CURA I DUE
 * NUOVI ROMPEREBBERO I DUE VECCHI**: nello shader l'esposizione viene **prima** dei punti, quindi
 * i percentili misurati sull'immagine com'è non sono più quelli che i punti troveranno. Il conto
 * segue la catena: prima il guadagno dalla mediana, poi i due estremi **già esposti**, e per ultimo
 * il contrasto sulla distribuzione che ne esce. Calcolati tutti sull'immagine di partenza, i punti
 * di bianco e di nero mancherebbero il bersaglio di quanto l'esposizione ha spostato.
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
     * I sei valori che 'Auto' scrive dentro [look], guardando i pixel [pixels].
     *
     * ⚠️ **I pixel arrivano già campionati** (vedi [probe]): questa funzione è pura, quindi il banco
     * la può misurare senza aprire nessun file.
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
         * ⚠️ **Un'immagine piatta non si stira**: se i due percentili si toccano, allungare
         * l'intervallo vorrebbe dire moltiplicare per un numero enorme quello che c'è in mezzo,
         * cioè trasformare una nebbia in un mosaico di due colori. ⚠️ **L'esposizione invece resta**:
         * una nebbia scura si porta al grigio lo stesso, ed è la sola cosa sensata da farle.
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
     * Quanti pixel cadono su ognuno dei 256 livelli di **luminanza percettiva**.
     *
     * ⚠️ **Si misura sulla LUMINANZA e non per canale**, al contrario di Photoshop: là ogni canale
     * si stira per conto suo, e quello **è** il modo in cui toglie la dominante; qui i punti sono
     * due cursori soli, e la dominante la toglie il bilanciamento, che è l'altra metà di [tuned].
     * Stirando anche per canale, le due correzioni si sommerebbero sullo stesso difetto.
     * ⚠️ **Lo leggono in tre** (i punti, l'esposizione e il contrasto), ed è la ragione per cui non
     * vive più dentro il conto dei punti: tre giri sui pixel per la stessa tabella sarebbero tre
     * volte il costo del comando.
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
     * ⚠️⚠️ **IL BERSAGLIO È `MID` PERCHÉ È IL PERNO DEL CONTRASTO, e le due cose vanno insieme**:
     * la curva a S dello shader lavora attorno a `0,5`, quindi un'immagine la cui luce media cade
     * lì riceve un contrasto simmetrico, mentre una mediana spostata darebbe una S che allarga da
     * una parte e schiaccia dall'altra. Il numero non è una taratura: è quello che la formula del
     * cursore accanto usa come centro.
     * ⚠️⚠️ **SI GUARDA LA MEDIANA E NON LA MEDIA**, ed è la stessa ragione per cui i punti tagliano
     * mezzo per cento: un cielo bianco o un fondo nero tirano la media di parecchi livelli, mentre
     * la mediana dice dove cade davvero la metà dell'immagine.
     * ⚠️ **Il conto è l'inversa di `gain`**, che moltiplica la **luce lineare**: per portare il
     * tono `m` al bersaglio serve `2^stop = linear(MID) / linear(m)`. Fatto in sRGB darebbe un
     * guadagno sbagliato di quanto la curva del formato è storta, cioè di molto.
     * ⚠️ **La corsa è quella del cursore** ([Light.EXPOSURE_RANGE]): una fotografia quasi nera
     * chiederebbe sei stop, e scriverne uno che il cursore non può mostrare vorrebbe dire un
     * comando che sposta la manopola oltre il suo fondo.
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
     * ⚠️⚠️ **SI MISURA DOVE IL CURSORE AGIRÀ, cioè sull'immagine già esposta e già stirata**: il
     * contrasto è il quarto passo della catena, e una dispersione letta sull'immagine di partenza
     * direbbe quanto era piatta prima che gli altri tre la aprissero, cioè quasi sempre troppo poco.
     * ⚠️⚠️ **IL LEGAME FRA `k` E LA DISPERSIONE È LA PENDENZA AL PERNO, E IL NUMERO È MISURATO**:
     * `sCurve` mescola la retta con `smoothstep`, che nel mezzo ha pendenza **1,5**, quindi la
     * miscela ha pendenza `1 + 0,5k` e i toni vicini al perno si allontanano di tanto. Portare la
     * dispersione da `s` a [SPREAD] chiede perciò `k = 2 * (SPREAD / s - 1)`. ⚠️ **È
     * un'approssimazione del primo ordine e si dichiara**: lontano dal perno la curva è più piatta
     * della sua tangente, quindi il contrasto che ne esce è semmai un po' timido, che è il verso
     * giusto in cui sbagliare per un comando automatico.
     * ⚠️ **Il tetto [AUTO_K] tiene il conto innocuo**: mezza corsa corregge un'immagine piatta, e
     * oltre quella un comando automatico starebbe decidendo l'aspetto della fotografia invece di
     * correggerla.
     * ⚠️⚠️ **E IL CONTRASTO AUTOMATICO VA SOLO IN SU, come quello di Photoshop**: un'immagine più
     * dispersa del bersaglio non ha un difetto da correggere, ha un carattere, e spianarla vorrebbe
     * dire che 'Auto' toglie qualcosa a chi lo tocca su una fotografia già buona. Quello che il
     * comando sa fare è aprire una fotografia piatta.
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
        return (2f * (SPREAD / sparso - 1f)).coerceIn(0f, AUTO_K)
    }

    /** Il livello [v] dopo un'esposizione di [gain]: la stessa moltiplicazione, in luce lineare. */
    private fun shifted(v: Float, gain: Float): Float =
        srgb((linear(v) * gain).coerceIn(0.0, 1.0))

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

    /** Il ritorno da luce lineare a sRGB, cioè l'inversa esatta di [linear]. */
    private fun srgb(x: Double): Float =
        (if (x <= 0.0031308) x * 12.92 else 1.055 * x.pow(1.0 / 2.4) - 0.055).toFloat()

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

    /** Sotto questa distanza fra i due percentili l'immagine si dichiara piatta: vedi [tuned]. */
    private const val FLAT = 0.02f

    /** Il tono a cui l'esposizione porta la mediana: vedi [exposureFor]. */
    private const val MID = 0.5f

    /**
     * La dispersione dei toni a cui punta il contrasto automatico.
     *
     * ⚠️ **Il numero cade fra due che si possono misurare**: una distribuzione perfettamente uniforme
     * su tutto l'intervallo vale `0,289`, cioè un istogramma piatto che nessuna fotografia ha e che
     * nessuno vuole, e un'immagine addensata attorno al centro resta sotto `0,18`. Il bersaglio è a
     * metà strada, e a tenerlo innocuo c'è comunque [AUTO_K].
     */
    private const val SPREAD = 0.25f

    /** Quanta corsa del contrasto il conto automatico può spendere: vedi [contrastFor]. */
    private const val AUTO_K = 0.5f

    /** Il minimo con cui si divide, per non dividere mai per zero. */
    private const val TINY = 1e-6
}

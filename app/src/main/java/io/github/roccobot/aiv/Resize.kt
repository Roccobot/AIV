package io.github.roccobot.aiv

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Il **ridimensionamento**: a che misura l'editor scrive l'immagine quando la salva.
 *
 * È un parametro del salvataggio, con un tasto che lo imposta e un interruttore che lo applica,
 * come la filigrana (risposta `editor` dell'utente a `d-resize-dove`, 2.70).
 *
 * Non vive in `Look`, per la stessa ragione della filigrana: un `Look` è l'aspetto che uno stile
 * trasferisce da un'immagine all'altra, questo dice come scrivere il file. Arriva al salvataggio
 * come un argomento a sé, e non si vede sul palco: cambia quanti pixel si scrivono, non che cosa
 * si vede.
 *
 * ⚠️ **Le proporzioni si mantengono sempre**: chi vuole cambiare il rapporto ha il modulo
 * Ritaglio, che toglie pixel e non li stira. Il valore chiesto governa un lato e l'altro segue.
 * ⚠️ **Non si ingrandisce mai**: un lato più lungo di quello del file darebbe un'immagine più
 * pesante e non più nitida. Un piano che non rimpicciolisce non fa niente, e la finestra lo dice.
 */
object Resize {

    /**
     * Che cosa governa il valore chiesto.
     *
     * 'Lato lungo' e 'Lato corto' valgono per le verticali come per le orizzontali, larghezza e
     * altezza servono a chi ha un vincolo su una dimensione sola, la percentuale a chi non ragiona
     * in pixel, e il libero a chi scrive i pixel di destinazione (elenco dell'utente, voce
     * `resize-ripristina`).
     * L'ordine dei gettoni è quello di dichiarazione, quindi si riordina qui; con '%' in prima
     * fila i gettoni entrano in due righe (mockup dell'utente, voce `resize-finestra`).
     * L'archivio salva il token, quindi l'ordine non lo tocca.
     * Nel conto [FREE] governa la larghezza: nella finestra si scrivono tutti e due i campi, ma
     * l'altezza si traduce nella larghezza corrispondente prima di arrivare qui, così l'archivio
     * tiene un numero solo.
     */
    enum class Mode(override val token: String) : Choice {
        LONG("long"),
        SHORT("short"),
        SHARE("share"),
        WIDE("wide"),
        TALL("tall"),
        FREE("free")
    }

    /**
     * Se un modo dice la stessa cosa su **qualunque** immagine, cioè se ha senso come
     * predefinito.
     *
     * 'Rendi predefinito' si attiva solo per questi tre (scelta dell'utente, voce
     * `resize-finestra`): lato lungo, lato corto e percentuale non dipendono da come l'immagine è
     * girata, mentre larghezza, altezza e libero fissano un numero su un lato preciso, e su
     * un'immagine girata dall'altra parte darebbero un'area molto diversa.
     */
    fun portable(mode: Mode): Boolean =
        mode == Mode.LONG || mode == Mode.SHORT || mode == Mode.SHARE

    /** Quale dei due lati un modo governa, quando ne governa uno solo. */
    enum class Side { WIDE, TALL }

    /** I confini del valore, che dipendono dal modo: pixel per i cinque, per cento per l'altro. */
    const val MIN_PX = 16
    const val MAX_PX = 20_000
    const val MIN_SHARE = 1
    const val MAX_SHARE = 99

    /** Il valore dei pixel da cui si riparte, e quello della percentuale. */
    const val DEFAULT_PX = 1600
    const val DEFAULT_SHARE = 50

    /**
     * Il piano che **non fa niente**, cioè quello di fabbrica.
     *
     * Di fabbrica la finestra mostra due campi con le misure dell'immagine e nessun gettone
     * acceso (voce `resize-ripristina`): un tetto di ventimila pixel non rimpicciolisce nessuna
     * immagine, e la finestra lo mostra come le misure che l'immagine ha già.
     * Lo leggono il valore di fabbrica delle preferenze e il ripiego del modello finché le
     * impostazioni non sono arrivate.
     */
    val NONE = Plan(Mode.FREE, MAX_PX)

    /** Quanto un valore può valere, in questo modo. */
    fun range(mode: Mode): IntRange =
        if (mode == Mode.SHARE) MIN_SHARE..MAX_SHARE else MIN_PX..MAX_PX

    /**
     * Quale dei due campi della finestra comanda in questo modo, o `null` quando non ne comanda
     * uno solo: nel **libero** si scrivono tutti e due, nella **percentuale** nessuno dei due.
     *
     * Per il lato lungo e per quello corto lo decide la forma dell'immagine (richiesta
     * dell'utente): lo stesso gettone accende il campo di sopra su un'immagine orizzontale e
     * quello di sotto su una verticale. Il quadrato va sulla larghezza, ma i due lati sono lo
     * stesso numero e la scelta è indifferente.
     */
    fun edge(mode: Mode, w: Int, h: Int): Side? = when (mode) {
        Mode.WIDE -> Side.WIDE
        Mode.TALL -> Side.TALL
        Mode.LONG -> if (w >= h) Side.WIDE else Side.TALL
        Mode.SHORT -> if (w >= h) Side.TALL else Side.WIDE
        Mode.FREE, Mode.SHARE -> null
    }

    /**
     * Il valore del piano, ricavato da quello che c'è scritto nei campi.
     *
     * I due campi contengono le misure di destinazione, che un gettone non cambia: il valore è la
     * loro lettura secondo la regola scelta, quindi cambiare gettone non tocca nessun numero.
     * Il lato lungo e quello corto si leggono dai campi: con le proporzioni tenute, il più grande
     * dei due è il lato lungo per costruzione.
     */
    fun valueOf(mode: Mode, wide: Int, tall: Int, share: Int): Int = when (mode) {
        Mode.LONG -> max(wide, tall)
        Mode.SHORT -> min(wide, tall)
        Mode.WIDE, Mode.FREE -> wide
        Mode.TALL -> tall
        Mode.SHARE -> share
    }

    /**
     * Quanto vale l'altro campo, quando in [side] si scrive [value] su un'immagine di [w] per [h].
     *
     * È l'aritmetica di [Plan.sizeFor] su un lato solo, e tiene allineato il campo che non si sta
     * scrivendo, così i due numeri dicono il rapporto che il salvataggio rispetterà.
     * Non scende sotto un pixel, come il conto vero: un'immagine molto allungata con un lato al
     * minimo darebbe zero sull'altro.
     */
    fun mate(side: Side, value: Int, w: Int, h: Int): Int {
        if (w <= 0 || h <= 0) return 1
        val out = if (side == Side.WIDE) value.toDouble() * h / w else value.toDouble() * w / h
        return max(1, out.roundToInt())
    }

    /**
     * La percentuale che su un'immagine larga [w] dà una larghezza di [wide].
     *
     * Serve al cambio di gettone, l'unico punto in cui un ridimensionamento passa da un'unità
     * all'altra: toccando '%' la finestra mostra la percentuale che corrisponde ai due campi.
     * ⚠️ Chi la chiama la riporta nei confini: un'immagine appena rimpicciolita darebbe 99,6 per
     * cento, che arrotondato è fuori dalla corsa.
     */
    fun shareOf(wide: Int, w: Int): Int =
        if (w <= 0) DEFAULT_SHARE else (wide * 100.0 / w).roundToInt()

    /**
     * A che misura si scrive, e con che regola.
     *
     * Il valore è già un numero dentro i confini del suo modo: quello che l'utente scrive lo
     * ripulisce la finestra.
     */
    data class Plan(val mode: Mode, val value: Int) {

        /**
         * Che misura viene fuori per un'immagine di [w] per [h], o `null` quando non c'è niente da
         * fare.
         *
         * ⚠️ Risponde `null` anche quando la misura coincide: senza, il salvataggio riscriverebbe
         * il file per ottenere quello che c'era già, perdendo qualità per niente.
         */
        fun sizeFor(w: Int, h: Int): Pair<Int, Int>? {
            if (w <= 0 || h <= 0) return null
            val scala = when (mode) {
                Mode.LONG -> value.toFloat() / max(w, h)
                Mode.SHORT -> value.toFloat() / min(w, h)
                Mode.WIDE, Mode.FREE -> value.toFloat() / w
                Mode.TALL -> value.toFloat() / h
                Mode.SHARE -> value / 100f
            }
            // Il tetto a uno è il 'non si ingrandisce' scritto una volta sola, per tutti i modi.
            if (scala >= 1f) return null
            val nw = max(1, (w * scala).roundToInt())
            val nh = max(1, (h * scala).roundToInt())
            return if (nw == w && nh == h) null else nw to nh
        }
    }

    /**
     * Quanti pixel il salvataggio avrà davanti: [longSide] è il lato lungo del **file**,
     * l'anteprima dà le proporzioni, e poi si applicano la posa e il ritaglio.
     *
     * ⚠️ **Le proporzioni vengono dall'anteprima, per via dell'EXIF**: `inJustDecodeBounds`
     * legge le misure del flusso codificato, che per una fotografia scattata in verticale sono
     * quelle orizzontali, perché la rotazione è in un tag e la applica chi decodifica. Il lato
     * lungo è lo stesso prima e dopo un quarto di giro, quindi la misura viene da [Pixels] e la
     * forma dall'anteprima già raddrizzata (la divisione che il KDoc di [Pixels] dichiara).
     *
     * La posa e il ritaglio entrano nel conto perché `Resize` si applica dopo il taglio (vedi
     * `ImageEdit.redraw`): senza, la finestra direbbe la misura di partenza.
     */
    fun frameSize(
        longSide: Int,
        previewW: Int,
        previewH: Int,
        turns: Int,
        crop: ImageEdit.Crop
    ): Pair<Int, Int>? {
        if (longSide <= 0 || previewW <= 0 || previewH <= 0) return null
        val scala = longSide.toFloat() / max(previewW, previewH)
        val dritto = turns % 2 == 0
        val w = (if (dritto) previewW else previewH) * scala * (crop.right - crop.left)
        val h = (if (dritto) previewH else previewW) * scala * (crop.bottom - crop.top)
        return max(1, w.roundToInt()) to max(1, h.roundToInt())
    }

    /**
     * L'immagine ridimensionata, o `null` quando non c'è niente da fare.
     *
     * Col filtro acceso: senza, rimpicciolire scarta i pixel che non cadono sulla griglia nuova, e
     * l'immagine risulta sgranata e piena di scalini (come per il pennello della filigrana).
     * Chi chiama ricicla quello che ha passato solo se riceve un bitmap diverso, come in ogni
     * passata di questa catena.
     * ⚠️ **Un errore di memoria si propaga, non diventa `null`**: `null` vuol dire 'niente da
     * fare', e il salvataggio scriverebbe il file alla misura piena dicendo 'salvato'. La chiamano
     * i soli salvataggi, che raccolgono l'errore e rispondono 'troppo grande'.
     */
    fun apply(image: Bitmap, plan: Plan): Bitmap? {
        val (w, h) = plan.sizeFor(image.width, image.height) ?: return null
        return Bitmap.createScaledBitmap(image, w, h, true)
    }
}

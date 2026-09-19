package io.github.roccobot.aiv

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Il **ridimensionamento**: a che misura l'editor scrive l'immagine quando la salva.
 *
 * ⚠️⚠️ **DALLA `2.70`, ED È LA SUA RISPOSTA `editor` A `d-resize-dove`** (giro della `2.67`: *mi
 * piacerebbe più nell'editor, fatto in modo che funzioni tipo un parametro del salvataggio (un
 * parametro complicato, ovvio, ma tipo 'Salva alle dimensioni...'). Nella pratica, comunque, si
 * chiamerà 'Ridimensiona' e sarà costituito da un tasto più un interruttore. Imposti il
 * ridimensionamento dal tasto e poi l'interruttore stabilisce se il ridimensionamento si applica al
 * salvataggio, come per il watermark. Se si entra nell'opzione per configurarlo, una volta che
 * premo 'OK' l'interruttore è acceso e salva con ridimensionamento se non lo spengo*).
 *
 * ⚠️⚠️ **NON VIVE IN `Look`, PER LA STESSA RAGIONE DELLA FILIGRANA**: un `Look` è un **aspetto**,
 * cioè quello che uno stile si porta da un'immagine all'altra; questo dice **come scrivere il
 * file**, e in un preset non vorrebbe dire niente. Arriva al salvataggio come un argomento a sé.
 * ⚠️ **E per la stessa ragione non si vede sul palco**: l'immagine su cui si lavora resta quella,
 * perché ridimensionare non cambia che cosa si vede ma quanti pixel si scrivono.
 *
 * ⚠️⚠️ **LE PROPORZIONI SI MANTENGONO SEMPRE, E NON È UN'OPZIONE CHE MANCA**: un ridimensionamento
 * che le rompe deforma la fotografia, e chi vuole cambiare il rapporto ha il modulo Ritaglio, che
 * toglie dei pixel invece di stirarli. Quindi il valore chiesto governa **un** lato e l'altro
 * segue.
 * ⚠️⚠️ **E NON SI INGRANDISCE MAI**: i pixel che mancano non li inventa nessuno, quindi chiedere
 * un lato più lungo di quello che il file ha darebbe un'immagine più pesante e non più nitida. Un
 * piano che non rimpicciolisce non fa niente, e la finestra lo **dice** invece di lasciar credere
 * che il salvataggio ingrandirà.
 */
object Resize {

    /**
     * Che cosa governa il valore chiesto.
     *
     * ⚠️ **Quattro modi e non uno**: 'Lato lungo' è quello che serve quasi sempre (una misura che
     * vale per le verticali come per le orizzontali), gli altri due lati servono a chi ha un
     * vincolo su una dimensione sola, e la percentuale a chi non ragiona in pixel.
     */
    enum class Mode(override val token: String) : Choice {
        LONG("long"),
        WIDE("wide"),
        TALL("tall"),
        SHARE("share")
    }

    /** I confini del valore, che dipendono dal modo: pixel per i primi tre, per cento per l'altro. */
    const val MIN_PX = 16
    const val MAX_PX = 20_000
    const val MIN_SHARE = 1
    const val MAX_SHARE = 99

    /** Il valore di fabbrica dei pixel, e quello della percentuale. */
    const val DEFAULT_PX = 1600
    const val DEFAULT_SHARE = 50

    /** Quanto un valore può valere, in questo modo. */
    fun range(mode: Mode): IntRange =
        if (mode == Mode.SHARE) MIN_SHARE..MAX_SHARE else MIN_PX..MAX_PX

    /**
     * A che misura si scrive, e con che regola.
     *
     * ⚠️ **Il valore è un intero e non un testo**: quello che l'utente scrive lo ripulisce la
     * finestra, e qui arriva un numero dentro i confini del suo modo.
     */
    data class Plan(val mode: Mode, val value: Int) {

        /**
         * Che misura viene fuori per un'immagine di [w] per [h], o `null` quando non c'è niente da
         * fare.
         *
         * ⚠️⚠️ **RISPONDE `null` ANCHE QUANDO LA MISURA COINCIDE, ed è quello che tiene onesto il
         * tasto 'Salva'**: chiedere mille pixel a un'immagine che ne ha mille non è un lavoro, e
         * senza questa riga il salvataggio riscriverebbe il file per ottenere quello che c'era già,
         * cioè spenderebbe qualità per niente.
         */
        fun sizeFor(w: Int, h: Int): Pair<Int, Int>? {
            if (w <= 0 || h <= 0) return null
            val scala = when (mode) {
                Mode.LONG -> value.toFloat() / max(w, h)
                Mode.WIDE -> value.toFloat() / w
                Mode.TALL -> value.toFloat() / h
                Mode.SHARE -> value / 100f
            }
            // ⚠️ Il tetto a uno è il 'non si ingrandisce' scritto una volta sola: vale per tutti e
            // quattro i modi, e senza di lui ognuno avrebbe la propria condizione da ricordare.
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
     * ⚠️⚠️ **LE PROPORZIONI VENGONO DALL'ANTEPRIMA E NON DALL'INTESTAZIONE DEL FILE, E LA
     * RAGIONE È L'EXIF**: `inJustDecodeBounds` legge le misure del flusso codificato, che per
     * una fotografia scattata in verticale sono quelle orizzontali, perché la rotazione vive in
     * un tag e la applica chi decodifica. Il lato lungo invece è lo stesso prima e dopo un
     * quarto di giro, quindi i due dati si prendono ognuno da chi lo conosce: la misura da
     * [Pixels], la forma dall'anteprima già raddrizzata. È la stessa divisione che il KDoc di
     * [Pixels] dichiara.
     *
     * ⚠️ **La posa e il ritaglio entrano nel conto**, o la finestra direbbe la misura di
     * partenza mentre il salvataggio lavora su quella tagliata: `Resize` si applica **dopo** il
     * taglio, come si vede in `ImageEdit.redraw`.
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
     * ⚠️ **Col filtro acceso**: senza, rimpicciolire vuol dire buttare via i pixel che non cadono
     * sulla griglia nuova, cioè un'immagine sgranata e piena di scalini. È la riga gemella di
     * quella del pennello della filigrana.
     * ⚠️ **Chi chiama ricicla quello che ha passato solo se riceve un bitmap diverso**, che è la
     * convenzione di ogni passata di questa catena.
     */
    fun apply(image: Bitmap, plan: Plan): Bitmap? {
        val (w, h) = plan.sizeFor(image.width, image.height) ?: return null
        return runCatching { Bitmap.createScaledBitmap(image, w, h, true) }.getOrNull()
    }
}

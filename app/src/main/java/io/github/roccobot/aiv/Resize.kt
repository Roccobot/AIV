package io.github.roccobot.aiv

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min
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
     * ⚠️ **Sei modi e non uno**: 'Lato lungo' e 'Lato corto' sono le misure che valgono per le
     * verticali come per le orizzontali, la larghezza e l'altezza servono a chi ha un vincolo su
     * una dimensione sola, la percentuale a chi non ragiona in pixel, e il libero a chi i pixel di
     * destinazione li scrive e basta.
     * ⚠️⚠️ **DUE SONO NATI CON LA `2.77`, ED È IL SUO ELENCO ALLA LETTERA** (voce
     * `resize-ripristina`: *I chip devono essere: `Lato lungo`, `Lato corto`, `Larghezza`,
     * `Altezza`, `Pixel` (libero) e `Percentuale` (campo unico)*).
     * ⚠️⚠️ **L'ORDINE È QUELLO DEL SUO MOCKUP DELLA `2.81`, E PRIMA ERA QUELLO DELLA SUA FRASE**
     * (voce `resize-finestra`: *'Percentuale' diventa '%' e va nella prima riga dei gettoni: più
     * compatto (bastano due righe) e più elegante*). La percentuale era in coda, quindi la fila
     * veniva di tre righe; con la parola più corta della famiglia in prima fila le due righe si
     * riempiono da sé. ⚠️ **L'ordine dei gettoni è quello di dichiarazione**, quindi si riordina
     * qui e non nella finestra, e l'archivio non se ne accorge perché là vive il token.
     * ⚠️⚠️ **NEL CONTO [FREE] GOVERNA LA LARGHEZZA, E LA LIBERTÀ È DELLA FINESTRA**: là i due
     * campi si scrivono tutti e due, e quello dell'altezza si traduce nella larghezza
     * corrispondente prima di arrivare qui. Un piano con due numeri avrebbe voluto una seconda
     * chiave nell'archivio per un dato che le proporzioni rendono uno solo.
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
     * ⚠️⚠️ **I TRE SONO SUOI, E LA RAGIONE È CHE GLI ALTRI DIPENDONO DALLA FOTOGRAFIA APERTA**
     * (voce `resize-finestra`: *'Rendi predefinito' è cliccabile solo se sono attivi 'Lato
     * lungo', 'Lato corto' o '%'*). Il lato lungo, quello corto e la percentuale non guardano
     * come l'immagine è girata, quindi milleseicento pixel di lato lungo valgono uguale su una
     * verticale e su un'orizzontale; la larghezza, l'altezza e il libero fissano invece un
     * numero su un lato preciso, e portato su un'immagine girata dall'altra parte darebbe
     * un'area molto diversa da quella che si è scelta guardando.
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
     * ⚠️⚠️ **DALLA `2.77`, ED È QUELLO CHE RENDE VERA LA SUA RIGA AL PRIMO GIRO** (voce
     * `resize-ripristina`: *Di default ci devono essere due campi compilabili con il numero dei
     * pixel di destinazione, e nessun chip deve essere selezionato ... con le misure correnti
     * precompilate*). Fino alla `2.76` era 'Lato lungo 1600', quindi la finestra si apriva con un
     * gettone acceso e un numero che nessuno aveva scritto; un tetto di ventimila pixel invece
     * non rimpicciolisce nessuna fotografia, e la finestra lo mostra come le misure che
     * l'immagine ha già.
     * ⚠️ **Vive qui e non in tre posti**: lo leggono il valore di fabbrica delle preferenze (che
     * sono due campi) e il ripiego del modello finché le impostazioni non sono arrivate.
     */
    val NONE = Plan(Mode.FREE, MAX_PX)

    /** Quanto un valore può valere, in questo modo. */
    fun range(mode: Mode): IntRange =
        if (mode == Mode.SHARE) MIN_SHARE..MAX_SHARE else MIN_PX..MAX_PX

    /**
     * Quale dei due campi della finestra comanda in questo modo, o `null` quando non ne comanda
     * uno solo: nel **libero** si scrivono tutti e due, nella **percentuale** nessuno dei due.
     *
     * ⚠️⚠️ **PER IL LATO LUNGO E PER QUELLO CORTO LO DECIDE LA FORMA DELL'IMMAGINE, ed è la sua
     * frase alla lettera** (*se il lato lungo è la larghezza, quel campo è compilabile e l'altro
     * (a opacità ridotta/disattivato) si aggiorna automaticamente*). Quindi lo stesso gettone
     * accende il campo di sopra su una fotografia orizzontale e quello di sotto su una verticale.
     * ⚠️ **Il quadrato cade sulla larghezza**, e non è una scelta di merito: là i due lati sono lo
     * stesso numero, quindi quale dei due si scriva non cambia niente.
     */
    fun edge(mode: Mode, w: Int, h: Int): Side? = when (mode) {
        Mode.WIDE -> Side.WIDE
        Mode.TALL -> Side.TALL
        Mode.LONG -> if (w >= h) Side.WIDE else Side.TALL
        Mode.SHORT -> if (w >= h) Side.TALL else Side.WIDE
        Mode.FREE, Mode.SHARE -> null
    }

    /**
     * Il valore che un piano porta, ricavato da quello che c'è scritto nei campi.
     *
     * ⚠️⚠️ **I CAMPI SONO LA VERITÀ E IL VALORE SI RICAVA, DALLA `2.77`**: fino alla `2.76` il
     * numero era uno e il modo diceva come leggerlo, quindi passando da un gettone all'altro la
     * finestra doveva inventarsi che cosa scrivere. Adesso i due campi portano le misure di
     * destinazione, che un gettone non cambia, e il valore del piano è la loro lettura secondo la
     * regola scelta: cambiare gettone non tocca nessun numero.
     * ⚠️ **Il lato lungo e quello corto si leggono dai campi e non dall'immagine**: le proporzioni
     * sono tenute, quindi il più grande dei due campi è il lato lungo per costruzione.
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
     * ⚠️ **È la stessa aritmetica di [Plan.sizeFor] su un lato solo**, e serve alla finestra per
     * tenere allineato il campo che non si sta scrivendo: senza, i due numeri direbbero un
     * rapporto che il salvataggio non rispetterà, perché le proporzioni si mantengono sempre.
     * ⚠️ **Non scende sotto un pixel**, come il conto vero: un'immagine molto allungata con un
     * lato portato al minimo darebbe zero sull'altro.
     */
    fun mate(side: Side, value: Int, w: Int, h: Int): Int {
        if (w <= 0 || h <= 0) return 1
        val out = if (side == Side.WIDE) value.toDouble() * h / w else value.toDouble() * w / h
        return max(1, out.roundToInt())
    }

    /**
     * La percentuale che su un'immagine larga [w] dà una larghezza di [wide].
     *
     * ⚠️ **Serve al cambio di gettone**, che è l'unico posto in cui un ridimensionamento passa da
     * un'unità all'altra: senza, toccando 'Percentuale' la finestra mostrerebbe un numero che con
     * i due campi non c'entra. ⚠️ **Chi la chiama la riporta nei confini**, perché un'immagine
     * appena rimpicciolita darebbe un 99,6 per cento che arrotondato esce dalla corsa.
     */
    fun shareOf(wide: Int, w: Int): Int =
        if (w <= 0) DEFAULT_SHARE else (wide * 100.0 / w).roundToInt()

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
                Mode.SHORT -> value.toFloat() / min(w, h)
                Mode.WIDE, Mode.FREE -> value.toFloat() / w
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

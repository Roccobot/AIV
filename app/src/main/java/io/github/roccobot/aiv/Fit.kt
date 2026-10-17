package io.github.roccobot.aiv

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/*
 * Dove un'immagine si posa nel visualizzatore quando la barra delle info è sullo schermo.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️ L'adattamento si calcola sullo spazio che la barra lascia libero, non su tutto lo schermo
 * (richiesta dell'utente, 3.60): altrimenti un'immagine adattata all'altezza ha una striscia
 * nascosta sotto la barra, e niente la può mostrare.
 * ⚠️ La regola vive qui, una volta sola, perché la chiedono tre posti: `ImageCanvas`,
 * l'anteprima che occupa il posto mentre il file si apre (`plannedSize`) e l'immagine vicina che
 * entra durante lo scorrimento. Due copie di un adattamento in questa app hanno già divergito.
 */

/** Il bordo della vista a cui è attaccata la barra delle info. */
enum class BarEdge { TOP, BOTTOM }

/**
 * Quello che l'immagine deve sapere della barra delle info.
 *
 * @property size l'altezza della barra in pixel, margine dell'area sicura compreso; zero quando
 *   la barra è nascosta. La anima il chiamante, così una barra che si nasconde rende lo spazio
 *   in modo morbido.
 * @property spans i tratti orizzontali, in pixel della vista, dove la barra disegna testo o il
 *   marchio dell'app. Li legge solo la tolleranza.
 * @property system la parte di [size] che appartiene al sistema (la barra di stato sopra una
 *   barra in alto, la barra di navigazione sotto una in basso). La tolleranza non la raggiunge mai.
 */
data class BarSpace(
    val edge: BarEdge,
    val size: Float,
    val spans: List<ClosedFloatingPointRange<Float>> = emptyList(),
    val system: Float = 0f
)

/**
 * La fascia verticale in cui un'immagine si posa: [top] e [bottom] in pixel della vista.
 *
 * @property scale la scala di riposo, in pixel della vista per pixel dell'immagine.
 */
data class RestPlace(val scale: Float, val top: Float, val bottom: Float) {
    val height: Float get() = bottom - top
    val centre: Float get() = (top + bottom) / 2f
}

/**
 * La quota dell'altezza della vista che un'immagine può passare sotto la barra (regola
 * dell'utente: al massimo il 5%), e solo dove la barra non ha testo sopra.
 */
const val BAR_TOLERANCE = 0.05f

/**
 * Dove si posa un'immagine di [picWidth] x [picHeight] in una vista di [viewWidth] x [viewHeight].
 *
 * @param cap la scala di riposo massima (un pixel del file per pixel dello schermo quando
 *   'Ingrandisci le immagini piccole' è spento), o `null` quando un'immagine piccola può crescere.
 *
 * ⚠️ La tolleranza vale solo con la vista più larga che alta (decisione A3): lì l'altezza è il
 * lato scarso, e il 5% vale molta immagine. In verticale la barra prende comunque poca altezza.
 * ⚠️ L'immagine va sotto la barra solo dove la barra non ha testo: la sua larghezza, centrata,
 * deve restare lontana da ogni [BarSpace.spans]. Quando il 5% pieno raggiungerebbe un tratto,
 * l'immagine cresce solo fin dove il tratto più vicino permette, e per niente se un tratto copre
 * il centro.
 * La sovrapposizione si usa solo quando all'immagine serve: una limitata dalla larghezza, o da
 * [cap], si posa nella fascia libera come le altre.
 * ⚠️ La sovrapposizione si ferma alla barra di sistema (voce `3.70-01`): su un tablet la barra di
 * stato resta sullo schermo, e il 5% dell'altezza supera la riga di testo della barra delle info,
 * quindi l'immagine arriverebbe all'orologio e alle icone del sistema. Su un telefono in
 * orizzontale le barre di sistema sono nascoste, e non cambia niente.
 */
fun restPlace(
    viewWidth: Float,
    viewHeight: Float,
    picWidth: Float,
    picHeight: Float,
    cap: Float?,
    bar: BarSpace?
): RestPlace {
    val reserve = (bar?.size ?: 0f).coerceIn(0f, viewHeight)
    val freeHeight = viewHeight - reserve
    if (picWidth <= 0f || picHeight <= 0f || viewWidth <= 0f || freeHeight <= 0f) {
        return RestPlace(0f, 0f, viewHeight)
    }

    val byWidth = viewWidth / picWidth
    var fit = min(byWidth, freeHeight / picHeight)
    if (bar != null && reserve > 0f && viewWidth > viewHeight && freeHeight / picHeight < byWidth) {
        val allowance = min(viewHeight * BAR_TOLERANCE, (reserve - bar.system).coerceAtLeast(0f))
        val wanted = min(byWidth, (freeHeight + allowance) / picHeight)
        val clear = clearWidth(viewWidth / 2f, bar.spans)
        fit = max(fit, min(wanted, clear / picWidth))
    }
    val scale = if (cap == null) fit else min(fit, cap)

    // La sovrapposizione usata davvero: zero per un'immagine che entra nella fascia libera.
    val overlap = (picHeight * scale - freeHeight).coerceIn(0f, reserve)
    return when (bar?.edge) {
        BarEdge.TOP -> RestPlace(scale, reserve - overlap, viewHeight)
        BarEdge.BOTTOM -> RestPlace(scale, 0f, freeHeight + overlap)
        null -> RestPlace(scale, 0f, viewHeight)
    }
}

/**
 * Il tratto più largo centrato su [centre] che non tocca nessuno di [spans].
 *
 * Un tratto che contiene il centro lascia zero: l'immagine non può andare sotto la barra.
 */
fun clearWidth(centre: Float, spans: List<ClosedFloatingPointRange<Float>>): Float {
    var half = Float.POSITIVE_INFINITY
    for (span in spans) {
        half = if (centre in span) 0f
        else min(half, min(abs(span.start - centre), abs(span.endInclusive - centre)))
    }
    return 2f * half
}

/**
 * Dove può andare il centro dell'immagine lungo un asse, come scarto dal centro della vista.
 *
 * @param extent la misura dell'immagine lungo l'asse, alla scala corrente.
 * @param view la misura della vista lungo l'asse.
 * @param top l'inizio della fascia di riposo, [bottom] la sua fine.
 *
 * ⚠️ La fascia di riposo fa da finestra: un'immagine non più grande della fascia resta al suo
 * centro, e una più grande si sposta solo finché continua a coprirla. Così ogni parte di
 * un'immagine ingrandita, compresa la striscia sotto la barra, si può portare nello spazio
 * libero. Senza barra la fascia è la vista.
 * Tenere l'immagine anche dentro la vista non funziona: fra la fascia e la vista resterebbe
 * ferma contro il bordo opposto, e la striscia sotto la barra non si vedrebbe mai.
 */
fun clampAxis(candidate: Float, extent: Float, view: Float, top: Float, bottom: Float): Float {
    val mid = view / 2f
    if (extent <= bottom - top) return (top + bottom) / 2f - mid
    return (mid + candidate).coerceIn(bottom - extent / 2f, top + extent / 2f) - mid
}

/**
 * Dove uno zoom animato mette lo scarto dell'immagine, per una scala che passa da un valore a un
 * altro di [ratio] (scala nuova su scala vecchia).
 *
 * @param pivot lo scarto del centro della fascia di riposo dal centro della vista
 *   ([RestPlace.centre] meno metà della vista): il punto attorno a cui lo zoom cresce.
 *
 * ⚠️ Lo zoom cresce attorno al centro della fascia libera, non a quello della vista (voce
 * `3.71-01`): un'immagine a riposo è sul centro della fascia, più in basso di quello della vista
 * quando la barra è in alto, e scalare attorno al centro della vista moltiplicherebbe anche quello
 * scarto, facendola scivolare mentre cresce. Attorno al centro della fascia cresce sul posto.
 */
fun zoomOffset(offset: Float, pivot: Float, ratio: Float): Float = pivot + (offset - pivot) * ratio

package io.github.roccobot.aiv

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/*
 * Where a picture rests in the viewer when the info bar is on screen.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️⚠️ Since 3.60 the fit is computed on the space the bar leaves free, not on the whole
 * screen (user's request, 2026-10-04, with the `Viewer_NOPE` mockup): before, a picture
 * fitted to the full height had a strip hidden under the bar, and nothing could show it.
 * ⚠️ The rule lives here, once, because three places ask it: `ImageCanvas`, the preview
 * that stands in while the file opens (`plannedSize`), and the neighbour that slides in
 * while swiping. Two copies of a fit already diverged once in this app (the jump of 0.56).
 */

/** Which edge of the view the info bar is glued to. */
enum class BarEdge { TOP, BOTTOM }

/**
 * What the picture has to know about the info bar.
 *
 * @property size the height of the bar in pixels, safe-area padding included; zero when the
 *   bar is hidden. It is animated by the caller, so a hidden bar gives the space back smoothly.
 * @property spans the horizontal stretches, in pixels of the view, where the bar draws text
 *   or the app mark. Only the tolerance reads them.
 * @property system the part of [size] that belongs to the system (the status bar over a bar at
 *   the top, the navigation bar under one at the bottom). The tolerance never reaches it.
 */
data class BarSpace(
    val edge: BarEdge,
    val size: Float,
    val spans: List<ClosedFloatingPointRange<Float>> = emptyList(),
    val system: Float = 0f
)

/**
 * The vertical band a picture rests in: [top] and [bottom] in pixels of the view.
 *
 * @property scale the rest scale, in view pixels per picture pixel.
 */
data class RestPlace(val scale: Float, val top: Float, val bottom: Float) {
    val height: Float get() = bottom - top
    val centre: Float get() = (top + bottom) / 2f
}

/**
 * The share of the view height a picture may spend under the bar (user's rule: *al massimo
 * il 5% dell'altezza*), and only where the bar has no text over it.
 */
const val BAR_TOLERANCE = 0.05f

/**
 * Where a picture of [picWidth] x [picHeight] rests in a view of [viewWidth] x [viewHeight].
 *
 * @param cap the largest rest scale allowed (one pixel of the file per pixel of the screen
 *   when 'Enlarge small images' is off), or `null` when a small picture may grow.
 *
 * ⚠️⚠️ The tolerance applies only when the view is wider than tall (decision A3): there the
 * height is the scarce side, and 5% of it is worth a lot of picture. In portrait the bar
 * takes a small share of the height anyway.
 * ⚠️ The picture may go under the bar only where the bar has no text: its width, centred, has
 * to stay clear of every [BarSpace.spans]. When the full 5% would reach a span, the picture
 * grows only as far as the nearest span allows, and not at all if a span covers the centre.
 * ⚠️ The overlap is spent only when the picture needs it: one limited by the width, or by
 * [cap], rests in the free band like any other.
 * ⚠️⚠️ Since 3.71 the overlap stops at the system bar (item `3.70-01` not approved: *su tablet
 * in orizzontale le immagini alte vanno ancora a finire sotto l'overlay info*). On a tablet the
 * status bar stays on screen, and 5% of the height is more than the bar's own text row: the
 * picture crossed the whole bar and reached the clock and the icons of the system, which are
 * text too. On a phone held sideways the system bars are hidden, and nothing changes.
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

    // The overlap actually spent: zero for a picture that fits in the free band.
    val overlap = (picHeight * scale - freeHeight).coerceIn(0f, reserve)
    return when (bar?.edge) {
        BarEdge.TOP -> RestPlace(scale, reserve - overlap, viewHeight)
        BarEdge.BOTTOM -> RestPlace(scale, 0f, freeHeight + overlap)
        null -> RestPlace(scale, 0f, viewHeight)
    }
}

/**
 * The widest stretch centred on [centre] that touches none of [spans].
 *
 * ⚠️ A span that contains the centre leaves zero: the picture cannot get under the bar at all.
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
 * Where the centre of the picture may go along one axis, as an offset from the view's centre.
 *
 * @param extent the picture's size along the axis, at the current scale.
 * @param view the view's size along the axis.
 * @param top the start of the rest band, [bottom] its end.
 *
 * ⚠️⚠️ The rest band works as the window: a picture no larger than the band stays at its
 * centre, and a larger one moves only as far as it keeps covering the band. So every part of a
 * zoomed picture, the strip under the bar included, can be brought into the free space.
 * Without a bar the band is the view, and the rule is the one of every version before 3.60.
 * ⚠️ Discarded: also keeping the picture inside the view. Between the band and the view it
 * stayed pinned to the far edge, and the strip under the bar could never be seen.
 */
fun clampAxis(candidate: Float, extent: Float, view: Float, top: Float, bottom: Float): Float {
    val mid = view / 2f
    if (extent <= bottom - top) return (top + bottom) / 2f - mid
    return (mid + candidate).coerceIn(bottom - extent / 2f, top + extent / 2f) - mid
}

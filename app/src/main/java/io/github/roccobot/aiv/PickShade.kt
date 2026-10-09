package io.github.roccobot.aiv

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import kotlin.math.pow

/*
 * The shade over the grid above the selection sheet.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️⚠️ **IT EXISTS BECAUSE THE SELECTION IS THE ONE SURFACE THE VEIL CANNOT REACH** (his note E on
 * the 4.45 round: *dovendo lasciare interattiva la griglia non può ricoprirla con il velo. Una
 * soluzione potrebbe essere un'ombra personalizzata solo per coprire quel caso. Dovrebbe essere
 * netta ma graduale abbastanza da essere discreta, e gli indicatori-checkbox dovrebbero ignorarla*).
 * The sheet stays without a veil on purpose, so the grid can still be picked from; the shade only
 * darkens, and takes no touch.
 * ⚠️⚠️ **IT IS DRAWN TWICE, WITH ONE FORMULA**: behind the cells, on the screen's whole column
 * (since 4.48; on the grid, in 4.46, it was boxed in by the grid's margins), where it covers the
 * gaps between them and the margins; and inside every cell, above the picture and below the tick. A
 * single layer over the grid would cover the ticks, which his note keeps on top; a layer inside the
 * cells alone would leave the gaps bright, which his mockup darkens too. Both read the same window
 * position, so the two halves meet without a seam.
 * ⚠️ **The numbers are his mockup's, measured** (1200 x 2670 px, light theme): the shade starts 340
 * px above the sheet, about 120 dp on his phone, and reaches 49% black at the sheet's edge (halved
 * in 4.48, see [SHADE_MAX]), growing
 * faster near the bottom (about a tenth at half height, three tenths at three quarters), which a
 * power of 1.7 fits within a hundredth.
 * ⚠️ **On the dark theme it is white**, like the veil (`veilInk`): his own choice for the veil, that
 * the opposite colour reads as a surface on either theme. A reading of his note, declared on the
 * test item.
 */

/** How tall the shade is above the sheet: 340 px of his phone, at a density of about 2.9. */
internal val SHADE_TALL = 120.dp

/**
 * How dark the shade is at the sheet's edge: half of his mockup's 49% since 4.48 (his note on
 * `4.46-04`: *l'opacità massima dev'essere la metà di quella che vedo nella 4.46*).
 */
internal const val SHADE_MAX = 0.245f

/** The curve of his mockup: the shade at height fraction t below its top is `SHADE_MAX * t^1.7`. */
private const val SHADE_CURVE = 1.7f

/** Stops enough for a smooth ramp: at 120 dp a step is a few pixels tall, under the eye. */
private const val SHADE_STOPS = 16

/**
 * The shade's colour at full strength for the theme in force: black on light, white on dark.
 */
@Composable
internal fun shadeInk(): Color = if (LocalAivLight.current) Color.Black else Color.White

/**
 * Where the sheet's top edge is in the window, in pixels, or `null` when no sheet covers the bottom.
 * It reads [FootStage], so it follows the sheet's own animation by construction.
 */
@Composable
internal fun sheetTop(): Float? {
    val copre = FootStage.covers
    if (copre <= 0) return null
    return (LocalWindowInfo.current.containerSize.height - copre).toFloat()
}

/**
 * Draws the shade on this node, in its own place: [behind] under its content (the grid, for the
 * gaps) or over it (a cell, under the tick that comes after it).
 *
 * ⚠️ [top] `null` draws nothing: outside a selection, or before the sheet has a place.
 */
@Composable
internal fun Modifier.pickShade(top: Float?, behind: Boolean): Modifier {
    if (top == null) return this
    val alto = with(LocalDensity.current) { SHADE_TALL.toPx() }
    return this then PickShadeElement(top, alto, shadeInk(), behind)
}

private data class PickShadeElement(
    val top: Float,
    val tall: Float,
    val ink: Color,
    val behind: Boolean
) : ModifierNodeElement<PickShadeNode>() {
    override fun create() = PickShadeNode(top, tall, ink, behind)
    override fun update(node: PickShadeNode) {
        node.top = top
        node.tall = tall
        node.ink = ink
        node.behind = behind
        node.invalidateDraw()
    }
}

/**
 * ⚠️⚠️ **IT REDRAWS WHEN IT MOVES, AND THAT IS WHY IT IS A NODE**: a cell of a lazy grid moves on a
 * layer while it scrolls, without being drawn again, so a shade computed once would scroll with the
 * picture. Knowing its place in the window, the node asks for a new drawing when that place changes.
 */
private class PickShadeNode(
    var top: Float,
    var tall: Float,
    var ink: Color,
    var behind: Boolean
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    private var y = Float.NaN

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val ora = coordinates.positionInWindow().y
        if (ora != y) {
            y = ora
            invalidateDraw()
        }
    }

    /**
     * ⚠️⚠️ **The ramp is laid with the noise that takes its bands away, since 4.90** (his report of
     * 2026-10-08: *vedo di nuovo un po' di banding*): over a light photo the shade goes through about
     * fifty levels in 120 dp, a step every seven pixels. The reckoning lives at the head of
     * `Dither.kt`. From [top] minus [tall] to [top], in the drawing's own coordinates: clear above,
     * [SHADE_MAX] at the sheet's edge and below it, where the sheet covers the rest.
     */
    private val ramp = GrainedRamp(SHADE_RAMP, SHADE_MAX)

    override fun ContentDrawScope.draw() {
        if (!behind) drawContent()
        if (!y.isNaN()) ramp.paint(this, ink, top - tall - y, top - y)
        if (behind) drawContent()
    }
}

/** The stops of his curve, as fractions of [SHADE_MAX]. */
private val SHADE_RAMP = List(SHADE_STOPS + 1) { i ->
    val t = i / SHADE_STOPS.toFloat()
    t to t.pow(SHADE_CURVE)
}

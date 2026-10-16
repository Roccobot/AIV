package io.github.roccobot.aiv

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
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
 * L'ombra sulla griglia sopra la scheda della selezione.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️ **Esiste perché la selezione è l'unica superficie che la Sfocatura dietro i pannelli non può
 * coprire** (nota E dell'utente sul giro della 4.45): la griglia deve restare toccabile per
 * scegliere, quindi la scheda resta senza sfocatura. L'ombra scurisce soltanto e non prende tocchi.
 * ⚠️ **Si disegna due volte, con la stessa formula**: dietro le celle, su tutta la colonna dello
 * schermo, dove copre gli spazi fra le celle e i margini; e dentro ogni cella, sopra l'immagine e
 * sotto la spunta. Uno strato solo sopra la griglia coprirebbe le spunte, che la sua nota vuole in
 * primo piano; uno strato solo dentro le celle lascerebbe chiari gli spazi, che il suo modello
 * scurisce. Tutte e due leggono la stessa posizione nella finestra, quindi le due metà si uniscono
 * senza giunta.
 * I numeri sono misurati sul suo modello (1200 x 2670 px, tema chiaro): l'ombra comincia 340 px
 * sopra la scheda, circa 120 dp sul suo telefono, e cresce più in fretta in basso (circa un decimo
 * a metà altezza, tre decimi a tre quarti), cosa che una potenza di 1,7 riproduce entro un
 * centesimo.
 * Col tema scuro è bianca, come la sfocatura (`veilInk`): il colore opposto si legge come una
 * superficie con tutti e due i temi.
 */

/** Quanto è alta l'ombra sopra la scheda: 340 px del suo telefono, a una densità di circa 2,9. */
internal val SHADE_TALL = 120.dp

/** Quanto è scura l'ombra al bordo della scheda: metà del 49% del suo modello (nota su `4.46-04`). */
internal const val SHADE_MAX = 0.245f

/** La curva del suo modello: alla frazione t d'altezza dall'alto l'ombra vale `SHADE_MAX * t^1.7`. */
private const val SHADE_CURVE = 1.7f

/** Tappe sufficienti per una rampa liscia: a 120 dp un passo è alto pochi pixel, sotto la soglia dell'occhio. */
private const val SHADE_STOPS = 16

/**
 * Il colore dell'ombra a piena forza per il tema in vigore: nero col chiaro, bianco con lo scuro.
 */
@Composable
internal fun shadeInk(): Color = if (LocalAivLight.current) Color.Black else Color.White

/**
 * Dov'è il bordo alto della scheda nella finestra, in pixel, o `null` se nessuna scheda copre il
 * fondo. Legge [FootStage], quindi segue per costruzione l'animazione della scheda.
 */
@Composable
internal fun sheetTop(): Float? {
    val copre = FootStage.covers
    if (copre <= 0) return null
    return (LocalWindowInfo.current.containerSize.height - copre).toFloat()
}

/**
 * Disegna l'ombra su questo nodo, al suo posto: sotto il contenuto con [behind] (la griglia, per
 * gli spazi) o sopra (una cella, sotto la spunta che viene dopo).
 *
 * Con [top] `null` non disegna niente: fuori da una selezione, o prima che la scheda abbia un posto.
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
 * ⚠️ **È un nodo perché deve ridisegnarsi quando si muove**: una cella di una griglia pigra scorre
 * su uno strato senza essere ridisegnata, quindi un'ombra calcolata una volta scorrerebbe con
 * l'immagine. Il nodo conosce il suo posto nella finestra e chiede un disegno nuovo quando cambia.
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
     * La rampa è stesa col rumore che toglie le bande: su un'immagine chiara l'ombra attraversa una
     * cinquantina di livelli in 120 dp, un gradino ogni sette pixel (il conto è in testa a
     * `Dither.kt`). Va da [top] meno [tall] a [top]: trasparente sopra, [SHADE_MAX] al bordo della
     * scheda e sotto, dove la scheda copre il resto.
     */
    private val ramp = GrainedRamp(SHADE_RAMP, SHADE_MAX)

    /*
     * ⚠️ **La rampa è nelle coordinate della finestra, e la tela si sposta sotto di lei**: con gli
     * estremi nelle coordinate della cella, ogni cella in vista costruirebbe un gradiente nuovo a
     * ogni fotogramma di scorrimento. Così la rampa resta la stessa e cambia solo dove la si legge
     * (rilievo D26, presidiato da `RampeTest`).
     */
    override fun ContentDrawScope.draw() {
        if (!behind) drawContent()
        val qui = y
        if (!qui.isNaN()) {
            val inizio = top - tall
            val fine = top
            translate(top = -qui) {
                ramp.paint(this, ink, inizio, fine, top = qui, bottom = qui + size.height)
            }
        }
        if (behind) drawContent()
    }
}

/** Le tappe della sua curva, come frazioni di [SHADE_MAX]. */
private val SHADE_RAMP = List(SHADE_STOPS + 1) { i ->
    val t = i / SHADE_STOPS.toFloat()
    t to t.pow(SHADE_CURVE)
}

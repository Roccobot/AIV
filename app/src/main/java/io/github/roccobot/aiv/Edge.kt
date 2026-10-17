package io.github.roccobot.aiv

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * L'**ombra** intorno a una superficie che si apre sopra la schermata, quando è lei la scelta.
 *
 * È l'alternativa alla sfocatura, mai insieme a lei (istruzione dell'utente, 1.81).
 * ⚠️ **L'ombra esce dal pannello**, e la finestra di un `Popup` grande quanto il pannello la
 * taglierebbe di netto sul suo rettangolo: un alone che finisce con uno spigolo si legge come un
 * quadrato sfocato, il difetto che l'utente ha bocciato due volte. Lo evita il margine che i menu
 * si danno quando l'ombra è in vigore ([LIFT_ROOM]); la finestra di un dialogo e quella di una
 * scheda in fondo sono già grandi quanto lo schermo.
 *
 * ⚠️ **È un nodo e non un `@Composable`**: nei dialoghi il modificatore è scritto fuori dalla
 * finestra che poi lo ospita, e un nodo legge i suoi `CompositionLocal` (qui [LocalAivDepth])
 * dalla posizione in cui è attaccato, cioè dentro.
 * L'ombra la mette il sistema dal contorno del `RenderNode`, come `Modifier.shadow`: dipinta a
 * mano (`setShadowLayer`) sarebbe in software e con un aspetto diverso da ogni altra app.
 * Non ritaglia niente (`clip` spento): ritaglia la `Surface` con la sua forma.
 */
fun Modifier.lifted(round: Dp): Modifier = this then LiftElement(round, top = false)

/**
 * La stessa ombra per una superficie **appoggiata al bordo di sotto**: solo i due angoli in cima.
 *
 * La forma dichiara i soli angoli di sopra, come la sua `Surface`: con quattro angoli tondi
 * l'ombra girerebbe anche sotto il bordo dello schermo, con due archi tagliati a metà.
 */
fun Modifier.liftedTop(round: Dp): Modifier = this then LiftElement(round, top = true)

private data class LiftElement(
    private val round: Dp,
    private val top: Boolean
) : ModifierNodeElement<LiftNode>() {
    override fun create() = LiftNode(round, top)

    override fun update(node: LiftNode) {
        node.round = round
        node.top = top
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "lifted"
    }
}

private class LiftNode(
    var round: Dp,
    var top: Boolean
) : Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {

    /*
     * Misura e piazza senza cambiare niente: l'elevazione si dichiara sul layer con cui il
     * figlio è posato. Un nodo di disegno non potrebbe, perché l'ombra cade fuori dal suo
     * riquadro.
     */
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val posato = measurable.measure(constraints)
        val alza = currentValueOf(LocalAivDepth) == PanelDepth.SHADOW
        return layout(posato.width, posato.height) {
            if (!alza) return@layout posato.place(0, 0)
            posato.placeWithLayer(0, 0) {
                shadowElevation = PANEL_LIFT.toPx()
                shape = if (top) {
                    RoundedCornerShape(topStart = round, topEnd = round)
                } else {
                    RoundedCornerShape(round)
                }
                clip = false
            }
        }
    }
}

/**
 * Quanto si alza una superficie che si apre sopra la schermata.
 *
 * Un numero solo per tutte, come lo stondamento. La scala di Material (menu 3, dialogo 6,
 * scheda in fondo 1) è tarata per un'app con l'oscuramento di sistema dietro; qui l'ombra è la
 * sola cosa che stacca la superficie, e tre numeri la staccherebbero in tre modi.
 * 8 e non 12: a dodici l'alone si legge come una macchia intorno al pannello.
 * ⚠️ Il margine dei menu, [LIFT_ROOM], dipende da questo numero.
 */
private val PANEL_LIFT = 8.dp

/**
 * Quanta aria trasparente vuole intorno a sé una superficie che getta l'ombra.
 *
 * ⚠️ **Serve a due tagli diversi, e ne basta uno a far sparire l'ombra agli angoli**: la
 * finestra di un menu, grande quanto il pannello, e il buffer in cui Compose disegna un
 * sottoalbero con opacità minore di uno, grande quanto il nodo che ha quell'opacità. Con l'aria
 * dentro il nodo dell'opacità, i due tagli cadono sull'aria e non sull'ombra.
 * Il doppio dell'elevazione: un'ombra di Android si allarga più o meno quanto l'elevazione e
 * scende di circa la metà, e scritto come multiplo segue [PANEL_LIFT].
 * Solo con l'ombra: con la sfocatura la finestra di un menu deve restare grande quanto il
 * pannello.
 */
internal val LIFT_ROOM = PANEL_LIFT * 2f

/**
 * Il raggio di un **pannello**: i dialoghi e le tre schede appoggiate al bordo di sotto.
 *
 * Un numero solo per tutti i pannelli: uno stondamento dice che cosa è una superficie, non
 * quanto è larga.
 * 28 è `shapes.extraLarge`, la forma che Material dà a un `AlertDialog`; i menu hanno 20 per non
 * confondersi con un pannello.
 * È qui perché l'ombra ha bisogno dello stesso numero per girare intorno al pannello.
 */
val PANEL_ROUND = 28.dp

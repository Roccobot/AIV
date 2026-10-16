package io.github.roccobot.aiv

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Il **bordo d'accento** intorno a tutto quello che si apre sopra la schermata.
 *
 * ⚠️ **Oggi è spento** ([ACCENT_EDGE]): i modificatori restano chiamati e il disegno intatto,
 * così il bordo si riaccende con una riga.
 *
 * È il contorno della superficie, quindi passa da una riga sola per tutte. Va su tutte le
 * superfici dell'app, anche su quelle che non coprono niente e non hanno la Sfocatura dietro i
 * pannelli (decisione dell'utente, giro della `1.54`): la sfocatura dice 'mi apro sopra
 * qualcosa', il bordo dice 'sono una superficie di questa app', e non dipende dal suo
 * interruttore. Chi lo ha si trova cercando i chiamanti di [edged] e [edgedTop].
 *
 * ⚠️ **Il raggio si riduce del rientro**: due rettangoli stondati concentrici hanno raggi
 * diversi, e quello interno vale l'esterno meno la distanza fra i due. Con il raggio del
 * pannello il tratto si staccherebbe dal bordo nei soli angoli. Per questo il bordo prende un
 * raggio e non una `Shape`, da cui il raggio non si può ricavare.
 */
fun Modifier.edged(round: Dp): Modifier = this then EdgeElement(round, fuori = false)

/**
 * Lo stesso bordo per una superficie **appoggiata al bordo di sotto**, ma disegnato di **fuori**.
 *
 * Di fuori perché si vedano solo il lato di sopra e i due archi (richiesta dell'utente, voce
 * `bordo-ovunque`): i fianchi di una scheda in fondo coincidono coi bordi dello schermo, quindi
 * una linea che corre fuori da loro è già fuori dal vetro, e dove interrompere il tratto lo
 * decide lo schermo.
 * Il lato inferiore non si disegna: sull'ultima fila di pixel si leggerebbe come un taglio.
 */
fun Modifier.edgedTop(round: Dp): Modifier = this then EdgeElement(round, fuori = true)

private data class EdgeElement(
    private val round: Dp,
    private val fuori: Boolean
) : ModifierNodeElement<EdgeNode>() {
    override fun create() = EdgeNode(round, fuori)

    override fun update(node: EdgeNode) {
        node.round = round
        node.fuori = fuori
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "edged"
    }
}

private class EdgeNode(
    var round: Dp,
    var fuori: Boolean
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    /*
     * ⚠️ **Il colore si legge dal nodo, non da un `@Composable`**: nei dialoghi il modificatore è
     * scritto fuori dalla finestra che poi lo ospita, e un nodo legge i suoi `CompositionLocal`
     * dalla posizione in cui è attaccato, cioè dentro.
     * `LocalColorScheme` di Material non è pubblico, quindi il colore arriva da [aivAccent], che
     * legge le stesse costanti del tema.
     */
    override fun ContentDrawScope.draw() {
        drawContent()
        if (!ACCENT_EDGE) return
        val spesso = EDGE.toPx()
        val colore = aivAccent(currentValueOf(LocalAivLight))
        /*
         * Di fuori il rientro è negativo e il conto del raggio è lo stesso (`raggio - rientro`):
         * il tratto corre a mezzo spessore oltre il bordo, e il suo raggio è quello del pannello
         * più mezzo spessore.
         */
        if (fuori) {
            val rientro = -spesso / 2f
            drawPath(
                path = treLati(size, round.toPx() - rientro, rientro),
                color = colore,
                style = Stroke(spesso)
            )
            return
        }
        /*
         * Il tratto si disegna dentro, con un rientro di mezzo spessore: uno `Stroke` è a
         * cavallo della linea, e senza rientro metà del bordo finirebbe fuori dalla superficie
         * (tagliata dalla finestra su un menu, sopra la sfocatura su un dialogo). Il raggio
         * segue il rientro, come dice il KDoc di [edged].
         */
        val rientro = spesso / 2f - SCONFINA
        inset(rientro) {
            drawRoundRect(
                color = colore,
                cornerRadius = CornerRadius((round.toPx() - rientro).coerceAtLeast(0f)),
                style = Stroke(spesso)
            )
        }
    }
}

/**
 * Il contorno dei tre lati: su per il fianco sinistro, i due angoli in cima, giù per il destro.
 *
 * Parte e finisce sul bordo di sotto, che non è disegnato: un `Path` aperto lascia due estremità
 * nette. Il [round] è già quello della linea, corretto del rientro, e l'[inset] può essere
 * negativo: allora i fianchi corrono fuori dallo schermo e si vede la sola cima.
 */
private fun treLati(size: Size, round: Float, inset: Float): Path {
    val left = inset
    val right = size.width - inset
    val top = inset
    val bottom = size.height
    val r = round.coerceIn(0f, minOf(size.width / 2f - inset, size.height - inset))
    return Path().apply {
        moveTo(left, bottom)
        lineTo(left, top + r)
        arcTo(Rect(Offset(left, top), Size(r * 2, r * 2)), 180f, 90f, false)
        lineTo(right - r, top)
        arcTo(Rect(Offset(right - r * 2, top), Size(r * 2, r * 2)), 270f, 90f, false)
        lineTo(right, bottom)
    }
}

/**
 * Se il bordo d'accento si disegna.
 *
 * ⚠️ **Spento per scelta dell'utente** (pannelli nudi, opzione A, 4.38), con la richiesta di
 * poterlo recuperare: pannelli e schede tengono solo il colore della superficie. Per riaccenderlo
 * basta `true`, perché [edged] e [edgedTop] sono ancora chiamati da ogni superficie.
 * Nessuna misura dipende da lui: il bordo è un disegno sopra il pannello e non occupa spazio.
 * Presidiato da `BordoTest`.
 */
internal const val ACCENT_EDGE = false

/**
 * Quanto è spesso il bordo.
 *
 * 2dp, scelta dell'utente fra 2 e 3 (giro della `1.55`). In dp e non in pixel veri: i suoi
 * '2-3px' erano misurati su un mockup, dove un pixel della pagina è un dp del telefono.
 */
private val EDGE = 2.dp

/**
 * Di quanto il tratto **sconfina** oltre il bordo del pannello, in pixel veri.
 *
 * ⚠️ **Mezzo pixel, contro l'antialiasing sommato due volte**: il pannello sfuma l'ultimo pixel
 * della sua curva e il tratto sfuma il suo, e dove le due coperture valgono metà ciascuna resta
 * scoperto un quarto di pixel di sfondo; sull'arco la scaletta lo mette in fila e si vede un filo
 * chiaro o scuro. Con lo sconfinamento il pixel a cavallo del bordo cade dentro il tratto.
 * Sui lati dritti di un menu il pannello è grande quanto la finestra, e quel mezzo pixel cade
 * fuori; resta sugli archi, dove serve.
 */
private const val SCONFINA = 0.5f

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
 * È un nodo e non un `@Composable` per la stessa ragione del bordo (il colore letto dal nodo).
 * L'ombra la mette il sistema dal contorno del `RenderNode`, come `Modifier.shadow`: dipinta a
 * mano (`setShadowLayer`) sarebbe in software e con un aspetto diverso da ogni altra app.
 * Non ritaglia niente (`clip` spento): ritaglia la `Surface` con la sua forma, e un ritaglio qui
 * taglierebbe il bordo delle schede in fondo, che corre di fuori.
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
 * È qui perché il bordo d'accento ha bisogno dello stesso numero per il conto dei raggi
 * concentrici.
 */
val PANEL_ROUND = 28.dp

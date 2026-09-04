package io.github.roccobot.aiv

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Il **bordo d'accento** intorno a tutto quello che si apre sopra la schermata.
 *
 * ⚠️⚠️ **NASCE NELLA `1.54` E PRENDE IL POSTO DELL'OMBRA** (richiesta dell'utente, 2026-09-04:
 * *se al posto dell'ombra mettessimo una righina di 2-3px del colore di accento intorno ai
 * pannelli a comparsa, alle bottomsheet, ecc.? Io a quel punto toglierei l'ombra che è l'unica
 * cosa che non riesce a convincermi*, e poi *potrebbe essere l'elemento distintivo che cercavo
 * e che con la righina color accento non ha funzionato*). Non è un ritocco estetico fra tanti:
 * è il terzo tentativo di dare all'app un segno riconoscibile, dopo la striscia d'accento nata
 * nella `1.29` e ritirata nella `1.38`.
 * - ⚠️ **La differenza con quella striscia, che è la ragione per cui questa volta può
 *   funzionare**: quella era un elemento **in più** dentro il pannello, disegnato in tre modi
 *   diversi perché i posti in cui disegnarla erano tre; questo è il **contorno** della
 *   superficie, cioè una cosa che ogni superficie ha già, e passa da una riga sola.
 *
 * ⚠️⚠️ **E L'OMBRA CHE SE NE VA È QUELLA CHE LUI VEDEVA COME 'IL QUADRATO SFOCATO'**, che è il
 * difetto bocciato due volte: l'ombra di un pannello **esce** dal pannello, ma la finestra di un
 * `Popup` è grande quanto il pannello, quindi quel poco che esce viene **tagliato di netto sul
 * rettangolo della finestra**. Un alone che finisce con uno spigolo è esattamente la cosa che
 * l'occhio legge come un quadrato intorno a un riquadro stondato. Con la `1.53` la finestra ha
 * smesso di essere più grande del disegno, e da allora quel taglio cade sul bordo del pannello:
 * più preciso e più visibile insieme.
 *
 * ⚠️ **Il bordo NON dipende dall'interruttore della sfocatura**: quella è una funzione che si
 * accende, questo è il modo in cui l'app è fatta. Chi ha il velo spento vede comunque il bordo.
 *
 * ⚠️ **Dove va lo dice l'elenco che esiste già**, cioè quello del velo (`Veil.kt`): i menu, i
 * dialoghi e la scheda in fondo. Le due superfici esenti dal velo (la fila della selezione e
 * quella del ritaglio) restano esenti anche qui, per la stessa ragione: non si aprono sopra
 * niente, stanno **dentro** la schermata e si continua a toccare quello che hanno sotto.
 */
fun Modifier.edged(shape: Shape): Modifier = this then EdgeElement(shape, round = null)

/**
 * Lo stesso bordo per una superficie **appoggiata al bordo di sotto**: fa il giro su tre lati.
 *
 * ⚠️ **Il lato inferiore non si disegna, e non è una svista**: la scheda in fondo arriva al
 * bordo dello schermo, quindi quella riga cadrebbe sull'ultima fila di pixel, dove metà di lei
 * è fuori e l'altra metà si legge come un taglio. Un bordo che si chiude fuori campo non
 * chiude niente.
 */
fun Modifier.edgedTop(round: Dp): Modifier = this then EdgeElement(shape = null, round = round)

private data class EdgeElement(
    private val shape: Shape?,
    private val round: Dp?
) : ModifierNodeElement<EdgeNode>() {
    override fun create() = EdgeNode(shape, round)

    override fun update(node: EdgeNode) {
        node.shape = shape
        node.round = round
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "edged"
    }
}

private class EdgeNode(
    var shape: Shape?,
    var round: Dp?
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    /*
     * ⚠️⚠️ **IL COLORE SI LEGGE DAL NODO E NON DA UN `@Composable`**, per la stessa ragione del
     * velo: questo modificatore lo scrivono anche i dialoghi, dove la riga è **scritta** fuori
     * dalla finestra che poi lo ospita. Un nodo legge i suoi `CompositionLocal` dalla posizione
     * in cui è **attaccato**, che è dentro.
     * ⚠️ **La tavolozza di Material da un nodo non si raggiunge** (`LocalColorScheme` non è
     * pubblico), quindi il colore arriva da [aivAccent], che legge le stesse due costanti del
     * tema: la fonte resta una sola.
     */
    override fun ContentDrawScope.draw() {
        drawContent()
        val spesso = EDGE.toPx()
        val colore = aivAccent(currentValueOf(LocalAivLight))
        val tondo = round
        if (tondo != null) {
            drawPath(
                path = treLati(size, tondo.toPx(), spesso / 2f),
                color = colore,
                style = Stroke(spesso)
            )
            return
        }
        val forma = shape ?: return
        /*
         * ⚠️ **Il tratto si disegna DENTRO, con un rientro di mezzo spessore**: uno `Stroke`
         * sta a cavallo della linea, quindi senza rientro metà del bordo finirebbe fuori dalla
         * superficie, cioè tagliata dalla finestra su un menu e sovrapposta al velo su un
         * dialogo. Rientrando, il contorno resta tutto sul pannello.
         */
        inset(spesso / 2f) {
            drawOutline(
                outline = forma.createOutline(size, layoutDirection, this),
                color = colore,
                style = Stroke(spesso)
            )
        }
    }
}

/**
 * Il contorno dei tre lati: su per il fianco sinistro, i due angoli in cima, giù per il destro.
 *
 * ⚠️ **Parte e finisce sul bordo di sotto**, che non viene disegnato: un `Path` aperto lascia
 * due estremità nette, ed è quello che serve a una superficie che continua fuori dallo schermo.
 */
private fun treLati(size: Size, round: Float, inset: Float): Path {
    val left = inset
    val right = size.width - inset
    val top = inset
    val bottom = size.height
    val r = round.coerceAtMost(minOf(size.width / 2f - inset, size.height - inset))
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
 * Quanto è spesso il bordo.
 *
 * ⚠️ **2dp e non 2 pixel**: l'utente ha detto *2-3px* guardando un mockup, dove un pixel della
 * pagina è un dp del telefono. Presi come pixel veri sarebbero due terzi di dp su uno schermo a
 * tripla densità, cioè un filo che sparisce; presi come dp sono la riga che ha visto.
 */
private val EDGE = 2.dp

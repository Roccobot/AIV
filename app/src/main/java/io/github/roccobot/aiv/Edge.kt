package io.github.roccobot.aiv

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
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
 * ⚠️⚠️ **IL RAGGIO SI RIDUCE DEL RIENTRO, E NELLA `1.54` NON LO FACEVA** (riscontro dell'utente,
 * giro della `1.54`: *sembra che la linea di accento non abbia il raggio di stondatura
 * corretto*). Due rettangoli stondati **concentrici** hanno raggi diversi: quello interno vale
 * quello esterno **meno** la distanza fra i due. Disegnando il tratto a un rientro di mezzo
 * spessore ma tenendo il raggio del pannello, l'arco resta più largo del dovuto e nei quattro
 * angoli si stacca dal bordo, mentre sui lati dritti combacia. Ecco perché il difetto si vedeva
 * **solo** negli angoli, che è la cosa che lui ha descritto.
 * - ⚠️ **Per questo il bordo prende un RAGGIO e non una `Shape`**: da una forma qualunque il
 *   raggio non si può ricavare, quindi non si può nemmeno correggere. Con una `Dp` il conto è
 *   una sottrazione, e le superfici che lo chiamano hanno tutte gli angoli tondi uguali.
 *
 * ⚠️ **Il bordo NON dipende dall'interruttore della sfocatura**: quella è una funzione che si
 * accende, questo è il modo in cui l'app è fatta. Chi ha il velo spento vede comunque il bordo.
 *
 * ⚠️⚠️ **VA SU TUTTE LE SUPERFICI DI QUESTA SPECIE, E DALLA `1.55` L'ELENCO NON È PIÙ QUELLO
 * DEL VELO** (decisione dell'utente, giro della `1.54`: *voglio la riga anche lì: in realtà
 * dappertutto. Capisco che quella fa eccezione perché non è in sovrapposizione e non ha
 * sfocatura o velo ... Ma per coerenza deve avere il tratto intorno come tutti gli altri
 * elementi simili*). Nella `1.54` i due elenchi coincidevano, e le due superfici esenti dal velo
 * (la scheda della selezione e quella dell'editor) erano rimaste senza bordo.
 * - **La differenza fra i due elenchi, che è la ragione per cui adesso divergono**: il velo dice
 *   *mi apro sopra qualcosa*, quindi non lo vuole chi resta dentro la schermata e lascia toccare
 *   quello che ha sotto; il bordo dice *sono una superficie di questa app*, e quello vale anche
 *   per chi non copre niente.
 * - **Quindi il bordo ce l'hanno**: i menu, i dialoghi, la scheda delle informazioni, la scheda
 *   della selezione e il pannello dei comandi dell'editor. Le ultime due restano senza velo.
 */
fun Modifier.edged(round: Dp): Modifier = this then EdgeElement(round, tre = false)

/**
 * Lo stesso bordo per una superficie **appoggiata al bordo di sotto**: fa il giro su tre lati.
 *
 * ⚠️ **Il lato inferiore non si disegna, e non è una svista**: la scheda in fondo arriva al
 * bordo dello schermo, quindi quella riga cadrebbe sull'ultima fila di pixel, dove metà di lei
 * è fuori e l'altra metà si legge come un taglio. Un bordo che si chiude fuori campo non
 * chiude niente.
 */
fun Modifier.edgedTop(round: Dp): Modifier = this then EdgeElement(round, tre = true)

private data class EdgeElement(
    private val round: Dp,
    private val tre: Boolean
) : ModifierNodeElement<EdgeNode>() {
    override fun create() = EdgeNode(round, tre)

    override fun update(node: EdgeNode) {
        node.round = round
        node.tre = tre
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "edged"
    }
}

private class EdgeNode(
    var round: Dp,
    var tre: Boolean
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
        /*
         * ⚠️ **Il tratto si disegna DENTRO, con un rientro di mezzo spessore**: uno `Stroke`
         * sta a cavallo della linea, quindi senza rientro metà del bordo finirebbe fuori dalla
         * superficie, cioè tagliata dalla finestra su un menu e sovrapposta al velo su un
         * dialogo. Rientrando, il contorno resta tutto sul pannello.
         * ⚠️⚠️ **E IL RAGGIO SEGUE IL RIENTRO**: la linea corre a mezzo spessore dentro il
         * bordo, quindi il suo raggio è quello del pannello meno mezzo spessore. È la
         * correzione del giro della `1.54`, e senza di lei l'arco si stacca negli angoli.
         */
        val dentro = round.toPx() - spesso / 2f
        if (tre) {
            drawPath(
                path = treLati(size, dentro, spesso / 2f),
                color = colore,
                style = Stroke(spesso)
            )
            return
        }
        inset(spesso / 2f) {
            drawRoundRect(
                color = colore,
                cornerRadius = CornerRadius(dentro.coerceAtLeast(0f)),
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
 * ⚠️ Il [round] che arriva qui è già quello **della linea**, cioè ridotto del rientro: la
 * correzione degli angoli vale per la scheda in fondo come per i pannelli.
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
 * Quanto è spesso il bordo.
 *
 * ⚠️ **3dp e non 3 pixel** (riscontro dell'utente, giro della `1.54`: *correggi, e portalo a
 * 3px*). Lui ha detto *2-3px* guardando un mockup, dove un pixel della pagina è un dp del
 * telefono; poi ha visto i 2dp sul telefono e ha chiesto il gradino successivo **in quella
 * stessa unità**. Presi come pixel veri sarebbero un dp su uno schermo a tripla densità, cioè
 * più sottili di quello che ha bocciato.
 */
private val EDGE = 3.dp

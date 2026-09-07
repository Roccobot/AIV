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
 *   della selezione, il pannello dei comandi dell'editor e la notifica di 'Annulla'. Le ultime
 *   tre restano senza velo.
 * - ⚠️ **La notifica mancava da questo elenco fino alla `1.78`**, benché il bordo lo prenda
 *   dalla `1.69` e il suo KDoc lo rivendichi: l'elenco si controlla cercando i chiamanti dei
 *   due modificatori qui sotto, che è la sola misura che non invecchia.
 */
fun Modifier.edged(round: Dp): Modifier = this then EdgeElement(round, fuori = false)

/**
 * Lo stesso bordo per una superficie **appoggiata al bordo di sotto**, ma disegnato di **fuori**.
 *
 * ⚠️⚠️ **DI FUORI DALLA `1.56`, ED È UNA PROVA CHIESTA DA LUI** (riscontro del giro della
 * `1.55`, voce `bordo-ovunque`: *le bottomsheet non stanno bene con la riga intorno. Vorrei fare
 * una prova con la linea di 2dp color accento che appare verso l'esterno, in modo da stare solo
 * sul lato sopra e sulla curva per poi sparire fuori dallo schermo*). Non è una variante
 * grafica del bordo di dentro: cambia **che cosa si vede**, e per una ragione geometrica.
 * - **Perché di fuori la riga si accorcia da sé**: i fianchi di una scheda in fondo stanno sui
 *   bordi dello schermo, quindi una linea che corre **fuori** da quei fianchi è già fuori dal
 *   vetro. Restano il lato di sopra e i due archi, che è esattamente l'elenco che ha chiesto, e
 *   nessuno deve decidere dove interrompere il tratto: lo decide il bordo dello schermo.
 * - ⚠️ **Il lato inferiore non c'è comunque**: la scheda arriva al bordo di sotto, e una riga
 *   sull'ultima fila di pixel si legge come un taglio. Un bordo che si chiude fuori campo non
 *   chiude niente.
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
         * ⚠️⚠️ **DI FUORI IL RIENTRO È NEGATIVO, e il conto del raggio è lo stesso**: il tratto
         * corre a mezzo spessore **oltre** il bordo, quindi il suo raggio è quello del pannello
         * **più** mezzo spessore. La formula sotto (`raggio - rientro`) copre i due casi senza
         * biforcarsi, perché spostarsi in fuori è rientrare di un numero negativo.
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
         * ⚠️ **Il tratto si disegna DENTRO, con un rientro di mezzo spessore**: uno `Stroke`
         * sta a cavallo della linea, quindi senza rientro metà del bordo finirebbe fuori dalla
         * superficie, cioè tagliata dalla finestra su un menu e sovrapposta al velo su un
         * dialogo. Rientrando, il contorno resta tutto sul pannello.
         * ⚠️⚠️ **E IL RAGGIO SEGUE IL RIENTRO**: la linea corre a mezzo spessore dentro il
         * bordo, quindi il suo raggio è quello del pannello meno mezzo spessore. È la
         * correzione del giro della `1.54`, e senza di lei l'arco si stacca negli angoli.
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
 * ⚠️ **Parte e finisce sul bordo di sotto**, che non viene disegnato: un `Path` aperto lascia
 * due estremità nette, ed è quello che serve a una superficie che continua fuori dallo schermo.
 * ⚠️ Il [round] che arriva qui è già quello **della linea**, cioè corretto del rientro, e
 * l'[inset] può essere **negativo**: allora i due fianchi corrono fuori dai bordi dello schermo
 * e di questo tracciato si vede la sola cima.
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
 * ⚠️⚠️ **2dp, ED È UN RITORNO** (riscontro dell'utente, giro della `1.55`: *torna a 2dp
 * (preferisco)*). La `1.54` era uscita a 2dp, la `1.55` li aveva portati a 3 su sua richiesta, e
 * visti in mano ha scelto i primi. ⚠️ **Il numero è in dp e non in pixel veri**: lui ha detto
 * *2-3px* guardando un mockup, dove un pixel della pagina è un dp del telefono; presi come pixel
 * veri sarebbero due terzi di dp su uno schermo a tripla densità, cioè un filo invisibile.
 */
private val EDGE = 2.dp

/**
 * Di quanto il tratto **sconfina** oltre il bordo del pannello, in pixel veri.
 *
 * ⚠️⚠️ **MEZZO PIXEL, ED È LA CORREZIONE DEL GIRO DELLA `1.55`** (*si intravedono dei pixel di
 * sfondo chiaro/scuro oltre la curva verde*). Il conto dei raggi era già giusto: quello che
 * restava è l'**antialiasing sommato due volte**. Il pannello disegna la sua curva sfumando
 * l'ultimo pixel, il tratto disegna la propria sfumando il suo, e dove le due coperture valgono
 * mezzo e mezzo quello che resta scoperto è un quarto di pixel di quello che sta dietro. Sui
 * lati dritti non si vede, sull'arco la scaletta lo mette in fila e diventa un filo di sfondo.
 * - **Perché mezzo pixel basta**: il pixel a cavallo del bordo, che il pannello copre a metà,
 *   con lo sconfinamento cade **dentro** il tratto e viene coperto tutto. Un pixel intero non
 *   servirebbe a niente in più e allargherebbe il disegno.
 * - ⚠️ **E dove non serve viene tolto da sé**: sui lati dritti di un menu il pannello è grande
 *   quanto la finestra, quindi quel mezzo pixel cade fuori e si perde; sugli archi, che sono
 *   rientrati rispetto agli spigoli della finestra, resta. Cioè sopravvive esattamente dove il
 *   difetto c'era.
 */
private const val SCONFINA = 0.5f

/**
 * L'**ombra** intorno a una superficie che si apre sopra la schermata, quando è lei la scelta.
 *
 * ⚠️⚠️ **NASCE NELLA `1.81` E NON SOSTITUISCE IL BORDO: SOSTITUISCE LA SFOCATURA** (istruzione
 * dell'utente, 2026-09-07: *facciamo che si può scegliere tra sfocatura e ombreggiatura (MAI
 * insieme)*). Il bordo d'accento resta in tutti e tre i casi, perché non è una funzione che si
 * accende: il perché sta in testa a questo file.
 * ⚠️⚠️ **ED È IL RITORNO DI QUELLO CHE LA `1.54` AVEVA TOLTO**, quindi la ragione per cui era
 * uscita va riletta prima di toccare questa riga: l'ombra di un pannello **esce** dal pannello, e
 * la finestra di un `Popup` era grande quanto il pannello, quindi quel poco che usciva veniva
 * tagliato di netto sul rettangolo della finestra. Un alone che finisce con uno spigolo è il
 * 'quadrato sfocato' che lui ha bocciato due volte. ⚠️ **A togliere la causa è il margine che i
 * menu si dànno quando questa scelta è in vigore** (`LIFT_ROOM` in `Menus.kt`): la finestra
 * diventa più grande del pannello, e l'ombra ha dove cadere. Fuori dai menu il problema non
 * esiste, perché la finestra di un dialogo e quella di una scheda in fondo sono già grandi
 * quanto lo schermo.
 *
 * ⚠️⚠️ **È UN NODO E NON UN `@Composable`, per la stessa ragione del bordo**: questa riga la
 * scrivono anche i dialoghi di Material, dove il modificatore è **scritto** fuori dalla finestra
 * che poi lo ospita. Un nodo legge i suoi `CompositionLocal` dalla posizione in cui è
 * **attaccato**.
 * ⚠️⚠️ **E L'OMBRA LA CHIEDE AL LAYER DEL PIAZZAMENTO, non a un disegno fatto a mano**: quella
 * di Android nasce dal contorno del `RenderNode`, quindi la mette il sistema con la forma
 * dichiarata, come fa `Modifier.shadow`. Dipingerla a mano (un `Paint` con `setShadowLayer`)
 * vorrebbe dire rifare in software una cosa che il compositore fa in hardware, e con un aspetto
 * diverso da quello di ogni altra app.
 * ⚠️ **Non ritaglia niente** (`clip` resta spento): a ritagliare il contenuto ci pensa la
 * `Surface` con la sua forma, e un ritaglio qui taglierebbe il bordo d'accento delle schede in
 * fondo, che corre di **fuori**.
 */
fun Modifier.lifted(round: Dp): Modifier = this then LiftElement(round, top = false)

/**
 * La stessa ombra per una superficie **appoggiata al bordo di sotto**: solo i due angoli in cima.
 *
 * ⚠️ **La forma dichiara i soli angoli di sopra**, come la `Surface` che la porta: con quattro
 * angoli tondi l'ombra girerebbe anche sotto il bordo dello schermo, dove non c'è niente da
 * staccare, e in cambio disegnerebbe due archi che il vetro taglia a metà.
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
     * ⚠️ **Misura e piazza senza toccare niente**: quello che cambia è il **layer** con cui il
     * figlio viene posato, che è il posto in cui si dichiara un'elevazione. Un nodo di disegno
     * non potrebbe farlo, perché un'ombra sta fuori dal riquadro che quel nodo ha per disegnare.
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
 * ⚠️⚠️ **UN NUMERO SOLO PER TUTTE, come lo stondamento e come il bordo**: 8dp. La scala di
 * Material ne avrebbe uno per specie (un menu 3, un dialogo 6, una scheda in fondo 1), e quei
 * numeri sono tarati per un'app che ha **anche** il velo di sistema dietro: qui l'ombra è la
 * sola cosa che stacca la superficie da quello che copre, quindi con tre numeri diversi tre
 * superfici della stessa app staccherebbero in tre modi. La `1.28` ha già fatto questa strada
 * con i raggi dei menu, che erano diventati tre.
 * ⚠️ **8 e non 12**: a dodici l'alone si allarga fino a leggersi come una macchia intorno al
 * pannello, che è il difetto per cui l'ombra era uscita dall'app; a otto si vede il distacco e
 * non si guarda l'ombra. ⚠️ **E il margine dei menu dipende da questo numero**: chi lo alza
 * guardi `LIFT_ROOM`, o l'ombra torna a sbattere contro il bordo della finestra.
 */
private val PANEL_LIFT = 8.dp

/**
 * Quanta aria trasparente vuole intorno a sé una superficie che getta l'ombra.
 *
 * ⚠️⚠️ **SERVE A DUE TAGLI DIVERSI, E NE BASTA UNO A FAR SPARIRE L'OMBRA AGLI ANGOLI.** Il
 * primo è la **finestra** di un menu, grande quanto il pannello disegnato: quello che esce dal
 * suo rettangolo non lo disegna nessuno. Il secondo è il **buffer** in cui Compose disegna un
 * sottoalbero con opacità minore di uno, grande quanto il nodo che porta quell'opacità: là
 * l'ombra sbatte contro il bordo del buffer. Con l'aria dentro il nodo dell'opacità, i due tagli
 * cadono su di lei invece che sull'ombra.
 * ⚠️ **Il doppio dell'elevazione, e non un numero a sé**: un'ombra di Android si allarga più o
 * meno quanto l'elevazione che la genera, e scende di circa la metà. Scritto come multiplo, chi
 * alza [PANEL_LIFT] si porta dietro anche questo.
 * ⚠️ **La paga solo chi ha scelto l'ombra**: con la sfocatura la finestra di un menu deve
 * restare grande **quanto** il pannello (è la cornice del giro della `1.51`), quindi l'aria non
 * si dà 'per sicurezza' a tutti e due i casi.
 */
internal val LIFT_ROOM = PANEL_LIFT * 2f

/**
 * Il raggio di un **pannello**: i dialoghi e le tre schede appoggiate al bordo di sotto.
 *
 * ⚠️⚠️ **ERA SCRITTO IN QUATTRO POSTI FINO ALLA `1.78`, e il precedente dice come finisce**:
 * `SHEET_CORNER` nella scheda della selezione, `SHEET_ROUND` in quella delle informazioni e in
 * quella dell'editor, e un `28.dp` dentro il modificatore dei dialoghi. La `1.28` ha già fatto
 * questa strada coi menu: fino alla `1.27` ogni menu portava il suo numero, ed erano diventati
 * **tre** diversi (8, 8 e 16), nati come 'dipende dalla forma del contenuto', che è una ragione
 * plausibile e sbagliata. Uno stondamento dice **che cosa è** quella superficie, non quanto è
 * larga.
 * ⚠️ **28 e non 20 come i menu**: 28 è `shapes.extraLarge`, cioè la forma che Material dà a un
 * `AlertDialog`, e i menu stanno a 20 apposta per non confondersi con un pannello.
 * ⚠️ **Vive qui perché il bordo d'accento ha bisogno dello stesso numero**: due riquadri
 * stondati concentrici hanno raggi diversi, e il conto che li lega sta in questo file. Con il
 * raggio scritto altrove, il tratto e il pannello potevano divergere senza che niente lo dicesse.
 */
val PANEL_ROUND = 28.dp

package io.github.roccobot.aiv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import kotlin.math.roundToInt

/*
 * Il Menu basso: i comandi del telefono in verticale, in una barra lungo il fondo dello schermo.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * È il terzo 'Elemento interattivo principale', accanto al FAB e alla pillola (nota D del giro della
 * 4.04, mockup `bottom_menu_full.png`). La barra occupa tutta la larghezza fino al vetro, nel colore
 * d'accento, ed è SOPRA la griglia (risposta G5), così un aspetto trasparente o traslucido lascia
 * vedere le immagini sotto.
 * Due forme, sullo stesso valore della pillola ([PhonePill]): a scomparsa ([PhonePill.SLIDE]), dove a
 * riposo resta solo una pillola verticale di due tasti nell'angolo del FAB, e fissa, sempre aperta.
 * Entra dal basso come una scheda, e veloce (risposta M1): i suoi tasti prendono quasi subito il
 * posto di quelli della pillola piegata.
 * I due tasti nell'angolo sono il salto (risposta M2): sulla pillola piegata quello in alto è sempre
 * 'in cima', e quello in basso, il marchio dell'app, diventa 'in fondo' durante lo scorrimento; nella
 * barra aperta i due tasti dove era la pillola diventano le due frecce.
 */

/** L'altezza della riga di tasti della barra, sopra l'area dei gesti che copre anche lei. */
internal val BAR_ROW = 56.dp

/**
 * Quanto dell'area dei gesti prende la riga di tasti della barra: la riga è più bassa di tanto
 * rispetto alla cima dell'inset di navigazione (nota B del giro della 4.32). Con un inset più
 * piccolo la riga si ferma sul bordo dello schermo.
 */
internal val BAR_DROP = 8.dp

/** Da quanti tasti la barra li distribuisce su tutta la larghezza: cinque (nota N2). */
internal const val BAR_SPREAD = 5

/**
 * La distanza laterale delle pillole e del loro tondo dal vetro: tre margini della griglia, 24dp,
 * lo stesso dei menu ([MENU_INSET]).
 *
 * Scelta A2 dell'utente (nota A del giro della 4.20): la pillola che occupa la riga resta dentro i
 * bordi delle miniature, e il tondo rientra degli stessi 8dp per coincidere col bordo della pillola
 * aperta. Vale solo di lato: dal basso resta il margine del FAB.
 */
internal val PILL_SIDE = MENU_INSET

/**
 * Dove sono le pillole e il loro tondo, nella casa e in ogni griglia: gli inset di sistema,
 * [PILL_SIDE] di lato e i 16dp del FAB ([HUB_PAD]) dal basso.
 *
 * ⚠️ **Un posto solo per le due schermate** (nota B del giro della 4.25): con due posti il tondo
 * saltava fra casa e cartelle.
 * ⚠️ **Il centro del tondo è il centro di ogni elemento principale** (regola dell'utente, 4.31): un
 * elemento di un altro [side] tiene lo stesso centro, quindi il FAB da 40dp rientra di 2dp su
 * tutti e due gli assi.
 *
 * @param side il lato dell'elemento: quello del tondo ([PILL_KEY]), salvo il FAB.
 */
internal fun Modifier.pillCorner(side: Dp = PILL_KEY): Modifier = composed {
    windowInsetsPadding(steadyDrawing())
        .padding(horizontal = cornerSide(side), vertical = HUB_PAD + (PILL_KEY - side) / 2)
}

/**
 * La distanza laterale dal vetro di un elemento principale di [side], dentro gli inset di sistema:
 * quella che [pillCorner] scrive, e che legge un menu ancorato al FAB ([rememberMenuSpot]).
 */
internal fun cornerSide(side: Dp): Dp = PILL_SIDE + (PILL_KEY - side) / 2

/**
 * Il margine di una barra con pochi tasti dal lato a cui si appoggia: quello delle pillole
 * ([PILL_SIDE]), così il tasto d'angolo cade sotto quello della pillola piegata.
 */
internal val BAR_SIDE = PILL_SIDE

/**
 * Quanto ci mette la barra a entrare o a uscire: un terzo meno dei 160 ms della pillola (risposta M1).
 *
 * Con la curva della pillola, che copre quasi tutta la strada nel primo terzo: dopo tre fotogrammi
 * la barra è quasi su, e i suoi tasti prendono il posto di quelli della pillola piegata.
 */
private const val BAR_IN_MS = 110

/**
 * Lo spazio che l'elemento principale prende sopra il bordo inferiore, oltre all'inset di sistema:
 * quello che una griglia lascia sotto l'ultima riga per poterla scorrere sopra.
 *
 * La pillola piegata del Menu basso è alta due tasti, quindi chiede un tasto in più della pillola
 * orizzontale; la barra aperta o fissa chiede la sua riga.
 */
@Composable
internal fun controlRoom(): Dp {
    val look = LocalPillLook.current
    return when {
        // Il Menu 'Start' si piega nella stessa pillola di due tasti del Menu basso a scomparsa.
        look.corner -> PILL_KEY * 2 + PILL_AIR * 2
        !look.bar -> PILL_KEY + PILL_AIR * 2
        look.mode == PhonePill.SLIDE -> PILL_KEY * 2 + PILL_AIR * 2
        else -> BAR_ROW - BAR_DROP
    }
}

/**
 * Quali superfici sono sotto la linea dei gesti, perché l'attività la dipinga bianca.
 *
 * ⚠️ **La linea la colora il sistema**: l'app può chiedere solo icone chiare o scure sulla barra
 * di navigazione. Sull'accento della barra la linea scura del tema chiaro quasi sparirebbe, quindi
 * con una barra su l'attività chiede quella chiara, come nel mockup.
 * Una mappa e non un interruttore, come [FootStage]: durante la dissolvenza fra due schermate le
 * barre in scena sono due, e quella che esce non deve togliere la richiesta dell'altra.
 */
internal object BarStage {
    private val sotto = mutableStateMapOf<Any, Unit>()

    /** Se adesso una barra è sotto la linea dei gesti. */
    val under: Boolean get() = sotto.isNotEmpty()

    fun on(chi: Any) { sotto[chi] = Unit }

    fun off(chi: Any) { sotto.remove(chi) }
}

/**
 * Il Menu basso, nella forma che l'impostazione chiede.
 *
 * @param entries i comandi, già nell'ordine del lato preferito (`mirrored`).
 * @param corner dove è la pillola piegata: gli inset e i margini del FAB.
 * @param modifier il posto della barra, lungo il fondo dello schermo.
 * @param mark il tasto in basso della pillola piegata, a riposo: il glifo del FAB senza la sua
 *   freccia, perché la freccia che lo sostituisce qui è sempre 'in fondo'.
 */
@Composable
internal fun BottomMenu(
    entries: List<PillEntry>,
    arm: JumpArm?,
    fabLabel: String,
    holdLabel: String?,
    onHold: (() -> Unit)?,
    backdrop: Backdrop?,
    atEnd: Boolean,
    slide: Boolean,
    corner: Modifier,
    modifier: Modifier,
    onJump: (Int) -> Unit,
    mark: @Composable (String?) -> Unit
) {
    if (!slide) {
        Bar(entries, arm, backdrop, atEnd, onJump, modifier)
        return
    }
    var open by rememberSaveable { mutableStateOf(false) }
    // Indietro piega la barra, come chiuderebbe il menu del FAB: una barra aperta è un menu.
    BackHandler(enabled = open) { open = false }
    val p by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(BAR_IN_MS, easing = PILL_EASE),
        label = "bar"
    )
    val close = PillEntry(icon = Glyphs.Material.Close, label = stringResource(R.string.pick_close)) { open = false }
    // Un tasto piega la barra prima di agire, come una riga di menu chiude il suo menu.
    val keys = entries.map { e -> e.copy(onTap = { open = false; e.onTap() }) }
        .let { if (atEnd) it + close else listOf(close) + it }
    Box(
        contentAlignment = if (atEnd) Alignment.BottomEnd else Alignment.BottomStart,
        modifier = modifier.fillMaxWidth()
    ) {
        if (p < 1f) {
            FoldedPill(
                arm = arm,
                fabLabel = fabLabel,
                holdLabel = holdLabel,
                onHold = onHold,
                backdrop = backdrop,
                enabled = !open,
                onOpen = { open = true },
                onJump = onJump,
                mark = mark,
                modifier = corner.graphicsLayer { alpha = 1f - p }
            )
        }
        if (p > 0f) {
            Bar(
                keys, arm, backdrop, atEnd, onJump,
                Modifier.graphicsLayer { translationY = size.height * (1f - p) },
                enabled = open
            )
        }
    }
}

/**
 * Il Menu 'Start': i comandi in un pannello a colonne nell'angolo preferito.
 *
 * Ogni sua forma è dell'utente (nota D del giro della 4.04, `bottom_menu_corner.png`, risposte G1,
 * C1, C2 e C3): sempre a scomparsa; aperto, una griglia di tre colonne nella casa e di due in una
 * cartella (decisione C3, [CORNER_SMALL]). Si ferma sopra la linea dei gesti (G1), che tiene il suo
 * colore.
 * La × prende il posto del marchio: la cella d'angolo è dove era il tasto in basso della pillola
 * piegata. Per i mancini ogni riga è specchiata.
 * Durante lo scorrimento la cella d'angolo e quella sopra diventano il salto (risposta M2).
 * Cresce dal suo angolo, coi tempi e la curva del Menu basso.
 * ⚠️ **Si chiude dopo ogni tocco** (voce `4.20-01`): aperto, copre una miniatura della griglia.
 * A riposo è un tondo solo, salvo la pillola scelta dall'utente ([PillLook.cornerRound]).
 * ⚠️ **Il tondo a riposo si comporta come quello della pillola a scomparsa** (richiesta
 * dell'utente, 6.03): durante lo scorrimento resta un tasto, il glifo diventa la freccia del FAB e
 * un tocco salta dove la freccia punta ([RoundRest]). La pillola a riposo
 * ([PillLook.cornerRound] `false`) tiene i suoi due tasti.
 *
 * @param entries le celle nell'ordine di lettura dei destri, senza la ×.
 * @param roundLabel che cosa fa adesso il tondo, secondo la freccia del FAB, come quello della
 *   pillola a scomparsa.
 * @param onLeap il salto del tondo durante lo scorrimento: quello del FAB ([JumpArm.leap]).
 * @param fabGlyph il glifo del tondo: il marchio dell'app, o la freccia quando il salto è armato.
 */
@Composable
internal fun CornerMenu(
    entries: List<PillEntry>,
    arm: JumpArm?,
    fabLabel: String,
    holdLabel: String?,
    onHold: (() -> Unit)?,
    backdrop: Backdrop?,
    atEnd: Boolean,
    corner: Modifier,
    modifier: Modifier,
    onJump: (Int) -> Unit,
    mark: @Composable (String?) -> Unit,
    roundLabel: String,
    onLeap: () -> Unit,
    fabGlyph: @Composable (String?) -> Unit
) {
    var open by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = open) { open = false }
    val p by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(BAR_IN_MS, easing = PILL_EASE),
        label = "corner"
    )
    val round = LocalPillLook.current.cornerRound
    val close = PillEntry(icon = Glyphs.Material.Close, label = stringResource(R.string.pick_close)) { open = false }
    val cells = entries.map { e ->
        e.copy(onTap = { open = false; e.onTap() }, onHold = e.onHold?.let { hold -> { open = false; hold() } })
    } + close
    var panel by remember { mutableStateOf(Rect.Zero) }
    val watcher = remember { Any() }
    DisposableEffect(open) {
        if (open) OutsideTouch.on(watcher) { at -> if (!panel.contains(at)) open = false }
        onDispose { OutsideTouch.off(watcher) }
    }
    val rows = cornerRows(cells, atEnd) { it.short }
    val columns = rows.maxOf { it.size }
    val tall = startRow(rows.size, columns)
    val q = arm?.shown ?: 0f
    val armed = arm?.armed == true
    val top = stringResource(R.string.jump_top)
    val bottom = stringResource(R.string.jump_bottom)
    Box(
        contentAlignment = if (atEnd) Alignment.BottomEnd else Alignment.BottomStart,
        modifier = modifier.then(corner)
    ) {
        if (p < 1f && round) {
            RoundRest(
                label = roundLabel,
                holdLabel = holdLabel,
                onHold = onHold,
                backdrop = backdrop,
                enabled = !open,
                armed = armed,
                onOpen = { open = true },
                onLeap = onLeap,
                glyph = fabGlyph,
                modifier = Modifier.graphicsLayer { alpha = 1f - p }
            )
        } else if (p < 1f) {
            FoldedPill(
                arm = arm,
                fabLabel = fabLabel,
                holdLabel = holdLabel,
                onHold = onHold,
                backdrop = backdrop,
                enabled = !open,
                onOpen = { open = true },
                onJump = onJump,
                mark = mark,
                modifier = Modifier.graphicsLayer { alpha = 1f - p }
            )
        }
        if (p > 0f) {
            Column(
                modifier = Modifier
                    .startShift(atEnd, tall)
                    .graphicsLayer {
                        alpha = p
                        scaleX = 0.6f + 0.4f * p
                        scaleY = 0.6f + 0.4f * p
                        transformOrigin = TransformOrigin(if (atEnd) 1f else 0f, 1f)
                    }
                    .declaresFoot()
                    .testTag(START_PANEL_TAG)
                    .onGloballyPositioned { panel = it.boundsInRoot() }
                    .buttonFill(backdrop, pillAccent(), RoundedCornerShape(CORNER_CELL / 2))
                    .startRoom(atEnd, rows.size, columns)
            ) {
                rows.forEachIndexed { r, row ->
                    Row(horizontalArrangement = if (atEnd) Arrangement.End else Arrangement.Start) {
                        row.forEachIndexed { c, cell ->
                            val inCorner = if (atEnd) c == row.lastIndex else c == 0
                            val jump = when {
                                arm == null || !inCorner -> 0
                                r == rows.lastIndex -> 1
                                r == rows.lastIndex - 1 -> -1
                                else -> 0
                            }
                            val usable = cell.copy(enabled = cell.enabled && open)
                            StartCell(tall) {
                                if (jump == 0) {
                                    PillKey(
                                        entry = usable, size = CORNER_CELL, enabled = usable.enabled && !armed,
                                        holdLabel = usable.holdLabel.takeIf { usable.onHold != null },
                                        glyph = { StartFace(cell) }
                                    )
                                } else {
                                    JumpKey(
                                        usable, jump, q, armed, CORNER_CELL, top, bottom, onJump,
                                        holdLabel = usable.holdLabel.takeIf { usable.onHold != null },
                                        rest = { StartFace(cell) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Chi vuole sapere di ogni tocco sull'app, ovunque cada: il Menu 'Start' aperto, che si chiude
 * quando il dito scende fuori (nota su `4.25-02`: qualsiasi tocco fuori, anche un trascinamento).
 *
 * La radice osserva e non consuma, quindi il trascinamento che chiude il menu scorre anche la
 * griglia.
 * ⚠️ **L'osservatore è sempre nell'albero** (voce `4.30-03`: un gesto unico): se ci fosse solo
 * mentre qualcuno ascolta, il tocco che chiude il menu lo toglierebbe dalla radice a metà gesto, e
 * il cambio dei modificatori della radice annullerebbe il trascinamento. Presidiato da
 * `MenuInferioreTest`, con un trascinamento a passi.
 * Non è un nodo sopra lo schermo (`Rules.md` § '👆 Che cosa fa il tocco FUORI da una finestra'):
 * è un modificatore della radice, che ogni tocco attraversa comunque, e senza ascoltatori non fa
 * niente.
 */
internal object OutsideTouch {
    private val listeners = mutableStateMapOf<Any, (Offset) -> Unit>()

    fun on(who: Any, listener: (Offset) -> Unit) { listeners[who] = listener }

    fun off(who: Any) { listeners.remove(who) }

    /** Un tocco in [at], nelle coordinate della radice. */
    fun press(at: Offset) { listeners.values.toList().forEach { it(at) } }
}

/**
 * Le celle del Menu 'Start' in righe, la cella d'angolo per ultima: le dispongono così il menu e la
 * sua copia nell'aiuto del primo avvio ([CornerMenuCopy]).
 *
 * Le righe si riempiono dal basso, così l'ultima, con la cella d'angolo, è sempre piena: il vuoto di
 * un numero dispari va alla riga in alto, lontano dal pollice.
 * ⚠️ **Quindi la riga in alto può essere la più corta, e la misura del pannello si conta sulla più
 * larga** (giro della 5.10): contata sulla prima riga, con sette celle nella casa le righe
 * risultavano alte un terzo.
 */
private fun <T> cornerRows(cells: List<T>, atEnd: Boolean, short: (T) -> String?): List<List<T>> {
    val columns = if (cells.size > CORNER_SMALL) 3 else 2
    val rows = cells.reversed().chunked(columns).map { it.reversed().toMutableList() }.reversed()
    /*
     * ⚠️ **L'etichetta più lunga non prende mai l'angolo arrotondato della riga in basso** (voce
     * `4.34-01`): l'arrotondamento ruba spazio orizzontale. Quell'angolo è la prima cella della riga
     * in basso, letta da destri; la voce che c'è si scambia con quella sopra la ×. In una cartella
     * si scambiano 'Impostazioni' e 'Cestino'. La lunghezza si conta in caratteri.
     */
    if (rows.size > 1) {
        val longest = cells.maxByOrNull { short(it)?.length ?: 0 }
        val corner = rows.last()[0]
        val above = rows[rows.size - 2]
        if (corner === longest && above.size == columns) {
            rows.last()[0] = above.last()
            above[above.lastIndex] = corner
        }
    }
    return rows.map { row -> if (atEnd) row else row.reversed() }
}

/**
 * Il Menu 'Start' aperto come lo disegna l'aiuto del primo avvio, nell'arancio dell'aiuto, con [key]
 * (la copia del tondo) nella cella d'angolo dove il menu aperto ha la ×.
 *
 * Richiesta dell'utente (4.31): se il menu di fabbrica è il Menu 'Start', il primo avvio lo mostra
 * aperto, perché il tondo da solo non dice niente dei comandi dietro.
 * Le celle sono un disegno e risponde solo il tasto, come in ogni [HintVeil]: il tasto insegna i
 * suoi due gesti, e una cella che agisse uscirebbe dall'aiuto per una porta che l'aiuto non spiega.
 * La cella d'angolo è il posto del tondo, quindi la copia è sopra il tasto vero.
 */
@Composable
internal fun CornerMenuCopy(entries: List<PillEntry>, atEnd: Boolean, key: @Composable () -> Unit) {
    val rows = cornerRows(entries + null, atEnd) { it?.short }
    val columns = rows.maxOf { it.size }
    val tall = startRow(rows.size, columns)
    Column(
        modifier = Modifier
            .startShift(atEnd, tall)
            .testTag(CORNER_COPY_TAG)
            .background(HINT_MARK, RoundedCornerShape(CORNER_CELL / 2))
            .startRoom(atEnd, rows.size, columns)
    ) {
        rows.forEach { row ->
            // Una riga in alto più corta si appoggia al lato dell'angolo, come nel menu aperto.
            Row(horizontalArrangement = if (atEnd) Arrangement.End else Arrangement.Start) {
                row.forEach { cell ->
                    StartCell(tall) {
                        if (cell == null) {
                            key()
                        } else {
                            CompositionLocalProvider(LocalContentColor provides HINT_INK) { StartFace(cell) }
                        }
                    }
                }
            }
        }
    }
}

/** L'etichetta di [CornerMenuCopy] che legge il banco: le sue celle sono disegni, senza semantica. */
internal const val CORNER_COPY_TAG = "corner-copy"

/** L'etichetta del pannello del Menu 'Start' aperto, di cui il banco legge i confini. */
internal const val START_PANEL_TAG = "start-panel"

/** Fino a quante celle, × compresa, il Menu 'Start' è un 2x2 (decisione C3). */
private const val CORNER_SMALL = 4

/**
 * L'altezza di una cella del Menu 'Start', e il lato del suo tasto.
 *
 * Più grande del tasto di una pillola (nota F del giro della 4.32: icone più grandi e più
 * distanziate): il pannello si sposta in fuori ([startShift]) e la × tiene il centro del tondo.
 */
private val CORNER_CELL = 64.dp

/**
 * La larghezza di una colonna del Menu 'Start', più larga del tasto perché l'etichetta abbia
 * spazio: 'Impostazioni' entra intera, alla misura delle altre (voce `4.33-07` B).
 */
private val START_COLUMN = 72.dp

/**
 * Quanto il pannello del Menu 'Start' sporge oltre le celle: verso il vetro, in basso e in alto
 * (voce `4.33-07` B, `startMenu.png`: i bordi del pannello non devono avvicinarsi troppo a quelli
 * delle miniature sotto).
 * Con [START_EDGE] il lato del pannello cade a 4dp dal vetro (misurato sul banco a 393dp), fuori
 * dai bordi delle miniature della casa (12dp) e di una griglia (8dp). Un bordo a filo con le
 * miniature di una griglia è proprio il difetto ottico da evitare.
 */
private val START_EDGE = 6.dp
private val START_FOOT = 4.dp
private val START_HEAD = 10.dp

/** Il lato del glifo, più grande di quello di una pillola ([PILL_GLYPH]) come la cella. */
private val START_GLYPH = 28.dp

/** Lo spazio fra il glifo di una cella del Menu 'Start' e la sua etichetta. */
private val START_LABEL_GAP = 3.dp

/** Il corpo massimo e minimo dell'etichetta corta: si riduce perché una parola lunga entri. */
private val START_LABEL = 10.sp
private val START_LABEL_MIN = 7.sp

/**
 * L'altezza di una riga del Menu 'Start', perché il pannello sia alto quanto è largo (voce
 * `4.34-01`: il menu sempre quadrato, ancorato in basso).
 *
 * Le righe si dividono quello che la larghezza lascia dopo gli spazi sopra e sotto: 68dp nel 3x3
 * della casa e in un 2x2. Il fondo resta dov'è il tondo, quindi il pannello cresce verso l'alto.
 * ⚠️ **Un numero pari di dp, e il resto va allo spazio sopra** ([startHead]): un terzo di 208 è
 * 69,33dp, che arrotondato al pixel lascia il pannello non quadrato; a 69 il centro della cella
 * cade su mezzo pixel, e la × un pixel fuori dal centro del tondo. Misurato sul banco.
 */
private fun startRow(rows: Int, columns: Int): Dp =
    ((START_COLUMN * columns + START_EDGE - START_HEAD - START_FOOT) / rows).value.toInt().let { it - it % 2 }.dp

/** Lo spazio sopra le righe del Menu 'Start': [START_HEAD] e quello che [startRow] lascia della larghezza. */
private fun startHead(rows: Int, columns: Int): Dp =
    START_COLUMN * columns + START_EDGE - START_FOOT - startRow(rows, columns) * rows

/**
 * Sposta il pannello del Menu 'Start' in fuori, di lato verso il vetro e in basso, così il centro
 * della cella d'angolo è quello del tondo, il centro di ogni elemento principale ([pillCorner]).
 */
private fun Modifier.startShift(atEnd: Boolean, row: Dp): Modifier {
    val x = START_EDGE + START_COLUMN / 2 - PILL_KEY / 2
    return offset(x = if (atEnd) x else -x, y = START_FOOT + (row - PILL_KEY) / 2)
}

/** Lo spazio che [START_EDGE], [START_FOOT] e [startHead] danno al pannello, dentro il fondo. */
private fun Modifier.startRoom(atEnd: Boolean, rows: Int, columns: Int): Modifier = padding(
    start = if (atEnd) 0.dp else START_EDGE,
    end = if (atEnd) START_EDGE else 0.dp,
    top = startHead(rows, columns),
    bottom = START_FOOT
)

/** Una cella del Menu 'Start', del menu o della copia dell'aiuto: il tasto al centro della colonna. */
@Composable
private fun StartCell(row: Dp, content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(START_COLUMN, row)) { content() }
}

/**
 * La faccia di una cella del Menu 'Start': il glifo, e sotto l'etichetta corta quando la voce ne
 * ha una ([PillEntry.short]). La × non ce l'ha, e tiene il glifo da solo al centro.
 */
@Composable
private fun StartFace(entry: PillEntry) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = entry.icon, contentDescription = null, modifier = Modifier.size(START_GLYPH))
        entry.short?.let { label ->
            // Lo spazio fra icona ed etichetta, con le righe più alte (voce `4.34-01`).
            Spacer(Modifier.height(START_LABEL_GAP))
            BasicText(
                text = label,
                style = TextStyle(color = LocalContentColor.current, textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(START_LABEL_MIN, START_LABEL, 0.5.sp),
                // Più larga del tasto, oltre il quale l'etichetta sporge: il tasto non ritaglia
                // ([PillKey]).
                modifier = Modifier.requiredWidth(START_COLUMN - 4.dp)
            )
        }
    }
}

/**
 * La barra: tutti i tasti su una riga, distribuiti sulla larghezza, sopra l'area dei gesti.
 *
 * Durante lo scorrimento i due tasti dell'angolo preferito diventano il salto (risposta M2): a
 * destra gli ultimi due, a sinistra i primi due, dove erano i due tasti della pillola piegata.
 * Il fondo è l'aspetto dei pulsanti principali ([buttonFill]), come dice il testo di
 * quell'impostazione.
 */
@Composable
private fun Bar(
    keys: List<PillEntry>,
    arm: JumpArm?,
    backdrop: Backdrop?,
    atEnd: Boolean,
    onJump: (Int) -> Unit,
    modifier: Modifier,
    enabled: Boolean = true
) {
    val q = arm?.shown ?: 0f
    val armed = arm?.armed == true
    val count = keys.size
    val inner = if (atEnd) count - 2 else 1
    val outer = if (atEnd) count - 1 else 0
    val top = stringResource(R.string.jump_top)
    val bottom = stringResource(R.string.jump_bottom)
    val chi = remember { Any() }
    DisposableEffect(chi) {
        BarStage.on(chi)
        onDispose { BarStage.off(chi) }
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .declaresBand()
            .buttonFill(backdrop, pillAccent(), RectangleShape)
    ) {
        val key = min(PILL_KEY, maxWidth / count.coerceAtLeast(1))
        /*
         * Da [BAR_SPREAD] tasti in su si distribuiscono su tutta la larghezza; con meno si
         * raccolgono sul lato preferito, con la spaziatura della pillola, e la × conta (regola
         * dell'utente, nota N2 del giro della 4.15).
         * Il margine è quello dell'angolo del FAB ([BAR_SIDE]), così il tasto d'angolo cade dove era
         * quello della pillola piegata.
         */
        val spread = count >= BAR_SPREAD
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Row(
            horizontalArrangement = when {
                spread -> Arrangement.SpaceEvenly
                atEnd -> Arrangement.End
                else -> Arrangement.Start
            },
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = (navBottom - BAR_DROP).coerceAtLeast(0.dp))
                .height(BAR_ROW)
                .padding(horizontal = if (spread) 0.dp else BAR_SIDE)
        ) {
            keys.forEachIndexed { i, entry ->
                val jump = when {
                    arm == null || count < 2 -> 0
                    i == inner -> -1
                    i == outer -> 1
                    else -> 0
                }
                val usable = entry.copy(enabled = entry.enabled && enabled)
                if (jump == 0) {
                    PillKey(entry = usable, size = key, enabled = usable.enabled && !armed)
                } else {
                    JumpKey(usable, jump, q, armed, key, top, bottom, onJump)
                }
            }
        }
    }
}

/**
 * Quello che resta della barra a scomparsa a riposo: una pillola verticale di due tasti
 * nell'angolo del FAB.
 *
 * Il tasto in alto è sempre 'in cima'; quello in basso è il marchio dell'app, che apre la barra e
 * tiene il tocco lungo del FAB, e durante lo scorrimento diventa 'in fondo' con la dissolvenza del
 * salto (risposta M2).
 * Dove la schermata non ha il salto resta solo il marchio: un tasto in alto che non fa niente
 * sarebbe un comando che mente.
 * Il tondo a riposo del Menu 'Start' è un'altra cosa ([RoundRest]).
 */
@Composable
private fun FoldedPill(
    arm: JumpArm?,
    fabLabel: String,
    holdLabel: String?,
    onHold: (() -> Unit)?,
    backdrop: Backdrop?,
    enabled: Boolean,
    onOpen: () -> Unit,
    onJump: (Int) -> Unit,
    mark: @Composable (String?) -> Unit,
    modifier: Modifier
) {
    val q = arm?.shown ?: 0f
    val armed = arm?.armed == true
    val top = stringResource(R.string.jump_top)
    val bottom = stringResource(R.string.jump_bottom)
    Column(modifier = modifier.declaresFoot().pillFill(backdrop)) {
        if (arm != null) {
            PillKey(
                entry = PillEntry(Glyphs.BrowseTop, top, enabled = enabled) { onJump(-1) },
                size = PILL_KEY
            )
        }
        val opener = PillEntry(Glyphs.Material.Close, fabLabel, enabled = enabled, onHold = onHold) { onOpen() }
        JumpKey(
            entry = opener,
            jump = 1,
            q = q,
            armed = armed && enabled,
            size = PILL_KEY,
            top = top,
            bottom = bottom,
            onJump = onJump,
            holdLabel = holdLabel,
            rest = { CompositionLocalProvider(LocalRoundKey provides true) { mark(null) } }
        )
    }
}

/**
 * Il tondo a riposo del Menu 'Start': lo stesso tondo della pillola a scomparsa, chiusa
 * (richiesta dell'utente, 6.03).
 *
 * Un tasto solo anche durante lo scorrimento: il glifo è quello del FAB ([glyph]), che diventa la
 * freccia quando il salto è armato, e il tocco salta dove punta ([onLeap]); altrimenti apre il
 * menu. Il tocco lungo è quello del FAB, come sul tondo della pillola a scomparsa.
 */
@Composable
private fun RoundRest(
    label: String,
    holdLabel: String?,
    onHold: (() -> Unit)?,
    backdrop: Backdrop?,
    enabled: Boolean,
    armed: Boolean,
    onOpen: () -> Unit,
    onLeap: () -> Unit,
    glyph: @Composable (String?) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier.declaresFoot().pillFill(backdrop)) {
        PillKey(
            entry = PillEntry(Glyphs.Material.Close, label, enabled = enabled, onHold = onHold) {
                if (armed) onLeap() else onOpen()
            },
            size = PILL_KEY,
            holdLabel = holdLabel,
            glyph = { CompositionLocalProvider(LocalRoundKey provides true) { glyph(null) } }
        )
    }
}

/**
 * Dichiara occupata tutta la fascia in basso, fino alla cima di questo nodo: un avviso sale sopra
 * la barra, come sopra la scheda della selezione ([FootStage.cover]).
 */
@Composable
private fun Modifier.declaresBand(): Modifier {
    val quota = remember { Any() }
    val finestra = LocalWindowInfo.current.containerSize
    DisposableEffect(quota) { onDispose { FootStage.off(quota) } }
    return this.onGloballyPositioned {
        FootStage.cover(quota, finestra.height - it.positionInWindow().y.roundToInt())
    }
}

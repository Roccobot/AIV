package io.github.roccobot.aiv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import kotlin.math.roundToInt

/*
 * The bottom menu: the commands of a phone held upright in a bar across the bottom of the screen.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️⚠️ Since 4.15 (the user's note D on the 4.04 round, with the mockup `bottom_menu_full.png`, and
 * his answers in chat): the third 'Elemento interattivo principale', next to the FAB and the pill.
 * The bar spans the whole width down to the glass, in the accent, and lies OVER the grid (answer
 * G5: *sovrapporre il menu alla griglia*), so a transparent or translucent look shows the pictures
 * underneath.
 * ⚠️ Two forms, on the same value as the pill ([PhonePill]): sliding ([PhonePill.SLIDE]), where at
 * rest only a vertical pill of two keys is left in the FAB's corner, and fixed, always open.
 * ⚠️⚠️ It enters from below like a sheet, and fast (his answer M1: *talmente veloce che le due voci
 * a destra prendono 'naturalmente' e quasi istantaneamente il posto delle due icone di destra*):
 * the stretching animation of the sliding pill, which he described first, is not used here.
 * ⚠️⚠️ The two keys in the corner are the jump (answer M2): on the folded pill the upper one is
 * always 'in cima', and the lower one, the app's mark, turns into 'in fondo' while scrolling; in the
 * open bar the two keys where the pill was turn into the two arrows.
 */

/** The height of the bar's row of keys, above the gesture area it also covers. */
internal val BAR_ROW = 56.dp

/** From how many keys the bar spreads them across its width: five (note N2). */
internal const val BAR_SPREAD = 5

/**
 * How far the pills and their round key keep from the glass, sideways: three margins of the grid,
 * 24dp, the same inset as the menus ([MENU_INSET]).
 *
 * ⚠️⚠️ **SINCE 4.25, HIS CHOICE A2** (after the preview of the two widths, note A on the 4.20
 * round): the pill that spans the row stays shorter than the thumbnails, inside their edges, and the
 * round key moves in by the same 8dp so it coincides with the open pill's edge. Until 4.20 both kept
 * the FAB's 16dp, and an open pill ended neither on the thumbnails' edge nor on its own key's.
 * ⚠️ **Sideways only**: from the bottom they keep the FAB's margin, and the FAB keeps its corner.
 */
internal val PILL_SIDE = MENU_INSET

/**
 * Where the pills and their round key sit, in the home and in every grid: the system's insets,
 * [PILL_SIDE] sideways and the FAB's 16dp ([HUB_PAD]) from the bottom.
 *
 * ⚠️⚠️ **ONE PLACE FOR BOTH SCREENS, SINCE 4.30, AND IT IS HIS NOTE** (B on the 4.25 round: *tra home
 * e cartelle il tondo col glifo salta da una posizione all'altra ... Vanno unificate posizioni e
 * dimensioni del tondo*): until 4.25 the home wrote 16dp from the bottom and a grid its own 12dp
 * plus 8, so the key jumped by 4dp between the two.
 */
internal fun Modifier.pillCorner(): Modifier = composed {
    windowInsetsPadding(steadyDrawing()).padding(horizontal = PILL_SIDE, vertical = HUB_PAD)
}

/**
 * The margin of a bar with few keys from the side it leans on: the pills' ([PILL_SIDE]), so the
 * corner key falls under the folded pill's.
 */
internal val BAR_SIDE = PILL_SIDE

/**
 * How long the bar takes to come in or go: a third less than the pill's 160 ms (answer M1).
 *
 * ⚠️ **With the pill's curve**, which covers most of the way in the first third: at 110 ms the bar
 * is nearly up after three frames, which is what lets its keys take the place of the folded pill's.
 */
private const val BAR_IN_MS = 110

/**
 * How much room the main control takes above the bottom edge, besides the system's inset: what a
 * grid leaves under its last row so that it can scroll above it.
 *
 * ⚠️ **The folded pill of the bottom menu is two keys tall**, so it asks for one key more than the
 * horizontal pill; the open or fixed bar asks for its row.
 */
@Composable
internal fun controlRoom(): Dp {
    val look = LocalPillLook.current
    return when {
        // ⚠️ The corner menu folds into the same two-key pill as the sliding bottom menu.
        look.corner -> PILL_KEY * 2 + PILL_AIR * 2
        !look.bar -> PILL_KEY + PILL_AIR * 2
        look.mode == PhonePill.SLIDE -> PILL_KEY * 2 + PILL_AIR * 2
        else -> BAR_ROW
    }
}

/**
 * Which surfaces lie under the gesture line, so that the activity paints it white.
 *
 * ⚠️⚠️ **THE LINE IS COLOURED BY THE SYSTEM, WHOLE** (the analysis given in chat for note D): the
 * app can only ask for light or dark icons on the navigation bar. On the accent of the bar the dark
 * line of the light theme would almost vanish, so while a bar is up the activity asks for the light
 * one, as his mockup shows.
 * ⚠️ **A map and not a flag**, like [FootStage]: during the crossfade between two screens two bars
 * are on stage, and the one that leaves must not take the other's request with it.
 */
internal object BarStage {
    private val sotto = mutableStateMapOf<Any, Unit>()

    /** Whether a bar is under the gesture line now. */
    val under: Boolean get() = sotto.isNotEmpty()

    fun on(chi: Any) { sotto[chi] = Unit }

    fun off(chi: Any) { sotto.remove(chi) }
}

/**
 * The bottom menu, in the form the setting asks for.
 *
 * @param entries the commands, already in the order of the preferred side (`mirrored`).
 * @param corner where the folded pill sits: the insets and margins the FAB had.
 * @param modifier the bar's place, across the bottom of the screen.
 * @param mark the folded pill's lower key at rest: the FAB's glyph without its own chevron, because
 *   the arrow that replaces it here is always 'in fondo'.
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
    // ⚠️ Back folds the bar, as it would close the FAB's menu: an open bar is a menu.
    BackHandler(enabled = open) { open = false }
    val p by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(BAR_IN_MS, easing = PILL_EASE),
        label = "bar"
    )
    val close = PillEntry(icon = Icons.Default.Close, label = stringResource(R.string.pick_close)) { open = false }
    // ⚠️ A key folds the bar before acting, as a menu row closes its menu.
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
 * The corner menu: the commands in a panel of three columns in the preferred corner.
 *
 * ⚠️⚠️ **SINCE 4.20, AND EVERY SHAPE OF IT IS HIS** (note D on the 4.04 round, `bottom_menu_corner.png`,
 * and the answers G1, C1, C2 and C3): always sliding (*il menu angolare sempre*), from the same
 * vertical pill as the sliding bottom menu; open, a grid of three columns in the home (3x3: the three
 * views, then 'Mostra nascoste', Cerca and Indirizzo, then Cestino, Impostazioni and the ×) and of
 * two in a folder (2x2). It stops above the gesture line (G1), so the line keeps its colour.
 * ⚠️ **The × takes the place of the mark**: the panel stands on the folded pill's corner, and its
 * corner cell is where the pill's lower key was. A left-handed panel mirrors every row.
 * ⚠️ **While scrolling, the corner cell and the one above it turn into the jump** (answer M2, read
 * on the panel): they are where the folded pill's two keys were.
 * ⚠️ **It grows out of its corner**, in the bottom menu's time and curve: it is the same gesture.
 * ⚠️⚠️ **SINCE 4.25 IT CLOSES AFTER EVERY TAP, AND IT IS HIS RULE** (`4.20-01`: *al contrario della
 * pillola a scomparsa, che rimane aperta, il menu angolare deve chiudersi dopo ogni interazione*):
 * open, it covers a thumbnail of the grid. In 4.20 a tap on the current view left it open.
 * ⚠️⚠️ **AND AT REST IT IS A SINGLE ROUND KEY, unless he chose the pill** (`4.20-01`, R2-R4, and
 * [PillLook.cornerRound]): while scrolling the key stretches upwards into the vertical pill of the
 * two jumps, following the jump's own state, as the sliding pill stretches sideways.
 *
 * @param entries the cells in the right-handed reading order, without the ×.
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
    mark: @Composable (String?) -> Unit
) {
    var open by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = open) { open = false }
    val p by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(BAR_IN_MS, easing = PILL_EASE),
        label = "corner"
    )
    val close = PillEntry(icon = Icons.Default.Close, label = stringResource(R.string.pick_close)) { open = false }
    val cells = entries.map { e -> e.copy(onTap = { open = false; e.onTap() }) } + close
    var panel by remember { mutableStateOf(Rect.Zero) }
    val watcher = remember { Any() }
    DisposableEffect(open) {
        if (open) OutsideTouch.on(watcher) { at -> if (!panel.contains(at)) open = false }
        onDispose { OutsideTouch.off(watcher) }
    }
    val columns = if (cells.size > CORNER_SMALL) 3 else 2
    // ⚠️ The rows are filled from the top, so that the last one, with the ×, is always full: a
    // gap left by an odd count goes to the top row, away from the thumb.
    val rows = cells.reversed().chunked(columns).map { it.reversed() }.reversed()
        .map { row -> if (atEnd) row else row.reversed() }
    val q = arm?.shown ?: 0f
    val armed = arm?.armed == true
    val top = stringResource(R.string.jump_top)
    val bottom = stringResource(R.string.jump_bottom)
    Box(
        contentAlignment = if (atEnd) Alignment.BottomEnd else Alignment.BottomStart,
        modifier = modifier.then(corner)
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
                round = LocalPillLook.current.cornerRound,
                modifier = Modifier.graphicsLayer { alpha = 1f - p }
            )
        }
        if (p > 0f) {
            Column(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = p
                        scaleX = 0.6f + 0.4f * p
                        scaleY = 0.6f + 0.4f * p
                        transformOrigin = TransformOrigin(if (atEnd) 1f else 0f, 1f)
                    }
                    .declaresFoot()
                    .onGloballyPositioned { panel = it.boundsInRoot() }
                    .buttonFill(backdrop, pillAccent(), RoundedCornerShape(PILL_KEY / 2))
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
                            if (jump == 0) {
                                PillKey(entry = usable, size = CORNER_CELL, enabled = usable.enabled && !armed)
                            } else {
                                JumpKey(usable, jump, q, armed, CORNER_CELL, top, bottom, onJump)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Who wants to know about every press on the app, wherever it lands: the open Start menu, which
 * closes when the finger goes down outside it.
 *
 * ⚠️⚠️ **SINCE 4.30, HIS NOTE** (on `4.25-02`: *QUALSIASI tocco fuori, anche un trascinamento sulla
 * griglia ad esempio, o un tocco in un'area vuota*): until 4.25 only its own cells closed it.
 * ⚠️ **The root watches and does not consume**, so the drag that closes the menu also scrolls the
 * grid. And the watcher is in the tree only while somebody listens ([active]): a node over the
 * whole screen exists only when it is needed (`Rules.md`, § 'Che cosa fa il tocco FUORI da una
 * finestra').
 */
internal object OutsideTouch {
    private val listeners = mutableStateMapOf<Any, (Offset) -> Unit>()

    /** Whether somebody is listening, so that [AivTheme] puts the watcher in the tree. */
    val active: Boolean get() = listeners.isNotEmpty()

    fun on(who: Any, listener: (Offset) -> Unit) { listeners[who] = listener }

    fun off(who: Any) { listeners.remove(who) }

    /** A press at [at], in the root's coordinates. */
    fun press(at: Offset) { listeners.values.toList().forEach { it(at) } }
}

/** Up to how many cells, the × included, the corner menu is a 2x2 (decision C3). */
private const val CORNER_SMALL = 4

/** The side of a corner menu's cell: a pill's key, so the corner cell is the folded pill's. */
private val CORNER_CELL = PILL_KEY

/**
 * The bar itself: every key on one row, spread across the width, over the gesture area.
 *
 * ⚠️ **The two keys at the preferred corner turn into the jump while scrolling** (answer M2): on the
 * right the last two, on the left the first two, which are where the folded pill's two keys were.
 * ⚠️ **The fill is the main buttons' look** ([buttonFill]), as his text for that setting says.
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
         * ⚠️⚠️ **WITH FEW KEYS THEY GATHER ON THE PREFERRED SIDE, SINCE 4.20, AND IT IS HIS RULE**
         * (note N2 on the 4.15 round: *quando le icone sono 5, 6 o 7 ... si distribuiscono per tutta
         * la larghezza disponibile. Ma se sono 2, 3 o 4 devono stare sul lato preferito*), with the
         * pill's own spacing, and the × counts (*le tre (4 a scomparsa)*). In 4.15 every bar spread
         * its keys, and the bin's three were lost across the screen.
         * ⚠️ **The margin is the FAB's corner** ([BAR_SIDE]), so the corner key lands where the
         * folded pill's key was.
         */
        val spread = count >= BAR_SPREAD
        Row(
            horizontalArrangement = when {
                spread -> Arrangement.SpaceEvenly
                atEnd -> Arrangement.End
                else -> Arrangement.Start
            },
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
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
 * What is left of the sliding bar at rest: a vertical pill of two keys in the FAB's corner.
 *
 * ⚠️ **The upper key is always 'in cima'**, and the lower one is the app's mark, which opens the bar
 * and keeps the FAB's long press; while scrolling it turns into 'in fondo' with the jump's own
 * crossfade (answer M2: *quella sopra è già ⌃ (in cima), l'altra (glifo) deve diventare 'in fondo'
 * con il solito meccanismo*).
 * ⚠️ **Where the screen has no jump only the mark is left**: an upper key that does nothing would
 * be a command that lies.
 * ⚠️ **With [round] the upper key exists only while scrolling** (the corner menu's round rest,
 * since 4.25): it grows out of the round key with the jump's state, and goes with it.
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
    modifier: Modifier,
    round: Boolean = false
) {
    val q = arm?.shown ?: 0f
    val armed = arm?.armed == true
    val top = stringResource(R.string.jump_top)
    val bottom = stringResource(R.string.jump_bottom)
    Column(modifier = modifier.declaresFoot().pillFill(backdrop)) {
        if (arm != null && !round) {
            PillKey(
                entry = PillEntry(Glyphs.BrowseTop, top, enabled = enabled) { onJump(-1) },
                size = PILL_KEY
            )
        } else if (arm != null && q > 0f) {
            /*
             * ⚠️ **The key keeps its size and the box around it grows**, anchored at the bottom:
             * the glyph rises out of the round key instead of being squashed, as the sliding
             * pill's keys come out from behind its round one.
             */
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .height(PILL_KEY * q)
                    .clipToBounds()
                    .graphicsLayer { alpha = q }
            ) {
                PillKey(
                    entry = PillEntry(Glyphs.BrowseTop, top, enabled = enabled && armed) { onJump(-1) },
                    size = PILL_KEY,
                    modifier = Modifier.wrapContentHeight(align = Alignment.Bottom, unbounded = true)
                )
            }
        }
        val opener = PillEntry(Icons.Default.Close, fabLabel, enabled = enabled, onHold = onHold) { onOpen() }
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
 * Declares the whole bottom band as taken, up to this node's top: a notice rises above the bar, as
 * it rises above the selection's sheet ([FootStage.cover]).
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

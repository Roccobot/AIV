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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
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
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(BAR_ROW)
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

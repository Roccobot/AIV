package io.github.roccobot.aiv

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/*
 * The accent pill that stands in for the FAB on wide and tall screens.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️⚠️ Since 3.70 (user's request of 2026-10-04, mockups `Tablet_H` and `Tablet_V`): with the
 * phone or the tablet held sideways the FAB's entries live in a vertical pill on the right of the
 * grid, at the bottom since 3.71, and on a tablet held upright in a horizontal pill at the bottom
 * right. Where the pill is, the
 * FAB is not (decision B4).
 * ⚠️ The entries are the FAB's own, read from the same place: when the FAB offers other or more
 * choices, the pill follows (*se il FAB prevede scelte diverse o più scelte, la pillola si
 * adatta*). That is why a screen hands a list to the pill and does not write a second one.
 * ⚠️ The jump to the top or the bottom (the chevron on the FAB) does not move here: a pill is not
 * a place that changes glyph while scrolling (decision B4).
 * ⚠️⚠️ Since 4.00 a long press on a key shows its label in a tooltip, and does nothing else
 * (answers B1 and B2 of 2026-10-05): a key without a written label needs a way to say what it
 * is, and the long press is that way in every pill, the wide ones included. So the long press of
 * 'Mostra nascoste' lives in the FAB's menu only. The fill follows the setting ([pillFill]).
 */

/**
 * One command of the pill.
 *
 * @property label what a screen reader says, and what the FAB's menu row says: the same string.
 * @property enabled whether the command can act now (the bin's two actions on an empty bin).
 */
data class PillEntry(
    val icon: ImageVector,
    val label: String,
    val enabled: Boolean = true,
    /**
     * The group the entry belongs to, for the FAB's menu, which draws a divider between groups.
     * The pill does not draw dividers: its keys are icons, and a line between two icons in a
     * capsule reads as a seam.
     */
    val group: Int = 0,
    /**
     * What a long press does in the FAB's menu, where the row has one ('Mostra nascoste').
     *
     * ⚠️ **A pill does not read it**: there the long press shows the label (see the header).
     */
    val onHold: (() -> Unit)? = null,
    val onTap: () -> Unit
)

/** How far apart the pill keeps from the screen's edges and from what it sits under. */
val PILL_AIR = 8.dp

/** The side of a pill's key, which is also the pill's thickness. */
val PILL_KEY = 44.dp

/** The side of a pill's glyph. */
internal val PILL_GLYPH = 22.dp

/**
 * The pill, standing on its own.
 *
 * @param vertical a column at the bottom of the grid's right side (wide screens) or a row at the
 *   bottom right corner (tall).
 *
 * ⚠️ The colours are the accent and its ink, as in the mockup: the pill says 'commands' the way
 * the FAB did, with the theme's main colour. Since 4.00 the accent can be at 80% or frosted
 * glass ([pillFill]), and [backdrop] is what the glass blurs.
 * ⚠️ A pill with no entry draws nothing: an empty capsule is a control that does nothing.
 * ⚠️ Since 3.71 the horizontal pill holds the three icons alone (item `3.70-07`: *la pillola
 * dev'essere compatta (solo le tre icone)*): the search field that led it in 3.70 is gone.
 */
@Composable
fun ActionPill(
    entries: List<PillEntry>,
    vertical: Boolean,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null
) {
    if (entries.isEmpty()) return
    CompositionLocalProvider(LocalContentColor provides pillInk()) {
        if (vertical) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier.pillFill(backdrop)
            ) {
                entries.forEach { PillKey(it) }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier.pillFill(backdrop)
            ) {
                entries.forEach { PillKey(it) }
            }
        }
    }
}

/**
 * One key of a pill: the icon alone, with its label for screen readers and in a tooltip.
 *
 * ⚠️⚠️ **THE LONG PRESS SHOWS THE LABEL, AND ONLY THAT** (answer B1, 2026-10-05: *nessuna funzione
 * secondaria*), in a tooltip next to the key (answer B3: *fumetto*), above it, where the finger
 * does not cover it. The one exception is [holdLabel]: the round key of [PhonePill.SLIDE] at
 * rest is the FAB, and keeps the FAB's long press (answer C1).
 *
 * @param size the key's side: smaller than [PILL_KEY] only when the pill would not fit.
 * @param glyph what the key shows, where it is not just [PillEntry.icon] (the round key, and the
 *   two keys that turn into the jump).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PillKey(
    entry: PillEntry,
    size: Dp = PILL_KEY,
    enabled: Boolean = entry.enabled,
    holdLabel: String? = null,
    modifier: Modifier = Modifier,
    glyph: (@Composable () -> Unit)? = null
) {
    val tip = rememberTooltipState()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val hold = entry.onHold.takeIf { holdLabel != null }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(entry.label) } },
        state = tip,
        enableUserInput = false,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                // ⚠️ The touch ripple is round (his note on the 4.04 round): it was the cell's
                // square, which inside a rounded pill reads as a mistake.
                .clip(CircleShape)
                .combinedClickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = entry.onTap,
                    onLongClickLabel = holdLabel,
                    onLongClick = {
                        haptics.performHapticFeedback(HOLD_BUZZ)
                        if (hold != null) hold() else scope.launch { tip.show() }
                    }
                )
                .semantics { contentDescription = entry.label }
                .alpha(if (entry.enabled) 1f else DISABLED_INK)
        ) {
            if (glyph != null) {
                glyph()
            } else {
                Icon(imageVector = entry.icon, contentDescription = null, modifier = Modifier.size(PILL_GLYPH))
            }
        }
    }
}

/**
 * 'Cerca nelle cartelle', or 'Cerca in *X*' with the folder's name in bold.
 *
 * ⚠️ The template carries `%1$s`: it is split on a sentinel in place of the name, so languages
 * that put the name elsewhere stay right.
 */
@Composable
fun searchInvitation(folderName: String?): AnnotatedString {
    if (folderName == null) {
        return buildAnnotatedString { append(stringResource(R.string.folders_rail_search)) }
    }
    val marker = "\u0001"
    val parts = stringResource(R.string.folders_rail_search_in, marker).split(marker, limit = 2)
    return buildAnnotatedString {
        append(parts[0])
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(folderName) }
        if (parts.size > 1) append(parts[1])
    }
}

/** How much ink a key keeps when it cannot act: the value Material uses for disabled content. */
private const val DISABLED_INK = 0.38f

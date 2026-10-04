package io.github.roccobot.aiv

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/*
 * The accent pill that stands in for the FAB on wide and tall screens.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️⚠️ Since 3.70 (user's request of 2026-10-04, mockups `Tablet_H` and `Tablet_V`): with the
 * phone or the tablet held sideways the FAB's entries live in a vertical pill under the filter
 * key, and on a tablet held upright in a horizontal pill at the bottom. Where the pill is, the
 * FAB is not (decision B4).
 * ⚠️ The entries are the FAB's own, read from the same place: when the FAB offers other or more
 * choices, the pill follows (*se il FAB prevede scelte diverse o più scelte, la pillola si
 * adatta*). That is why a screen hands a list to the pill and does not write a second one.
 * ⚠️ The jump to the top or the bottom (the chevron on the FAB) does not move here: a pill is not
 * a place that changes glyph while scrolling (decision B4).
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
    /** What a long press does, where the menu row has one ('Mostra nascoste'). */
    val onHold: (() -> Unit)? = null,
    val onTap: () -> Unit
)

/** How far apart the pill keeps from the screen's edges and from what it sits under. */
val PILL_AIR = 8.dp

/** The side of a pill's key, which is also the pill's thickness. */
val PILL_KEY = 44.dp

/**
 * The pill, standing on its own.
 *
 * @param vertical a column under the filter key (wide screens) or a row at the bottom (tall).
 * @param lead what comes before the keys in a horizontal pill: the search field of `Tablet_V`.
 *
 * ⚠️ The colours are the accent and its ink, as in the mockup: the pill says 'commands' the way
 * the FAB did, with the theme's main colour.
 * ⚠️ A pill with no entry and no lead draws nothing: an empty capsule is a control that does
 * nothing.
 */
@Composable
fun ActionPill(
    entries: List<PillEntry>,
    vertical: Boolean,
    modifier: Modifier = Modifier,
    lead: (@Composable RowScope.() -> Unit)? = null
) {
    if (entries.isEmpty() && lead == null) return
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(PILL_KEY / 2),
        modifier = modifier
    ) {
        if (vertical) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                entries.forEach { PillKey(it) }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = if (lead != null) 6.dp else 0.dp)
            ) {
                lead?.invoke(this)
                entries.forEach { PillKey(it) }
            }
        }
    }
}

/** One key of the pill: the icon alone, with its label for screen readers. */
@Composable
private fun PillKey(entry: PillEntry) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(PILL_KEY)
            .combinedClickable(
                enabled = entry.enabled,
                role = Role.Button,
                onClick = entry.onTap,
                onLongClick = entry.onHold
            )
            .semantics { contentDescription = entry.label }
            .alpha(if (entry.enabled) 1f else DISABLED_INK)
    ) {
        Icon(imageVector = entry.icon, contentDescription = null, modifier = Modifier.size(22.dp))
    }
}

/**
 * The search field at the head of the horizontal pill (`Tablet_V`): a tap that opens the search,
 * with 'Cerca in *folder*' as its invitation, the folder in bold.
 *
 * ⚠️ It is the same field the folder column had at its foot until 3.70 (`RailSearch`), moved into
 * the pill (decision B7), with the same two strings: a new wording would cost a string in every
 * language for the same meaning.
 */
@Composable
fun RowScope.PillSearch(folderName: String?, onSearch: () -> Unit) {
    val placeholder = searchInvitation(folderName)
    Icon(
        imageVector = Icons.Default.Search,
        contentDescription = null,
        modifier = Modifier.padding(start = 6.dp).size(22.dp)
    )
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .widthIn(min = 160.dp, max = 320.dp)
            .height(32.dp)
            .border(1.dp, MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onSearch)
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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

package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.launch

/**
 * The actions of the Feed's selection, from 6.03 (his request: *il feed deve avere la selezione
 * multipla*): the same sheet, keys and order as a folder's grid ([PickSheet]).
 *
 * ⚠️⚠️ **The addresses are the MediaStore's, as in a folder's grid**, so the operations are the
 * grid's: AIV Play keeps only what it allows ([Store.allows]), as there.
 * ⚠️ **'Lista' writes the names without a folder heading**: the pictures of the Feed come from many
 * folders, and the grid's heading is the path of the one folder it shows.
 * ⚠️ **'Tutti' and 'Inverti' work on the whole stream**, the one the screen has: the Feed is one list,
 * not a folder per month.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 */
@Composable
internal fun feedPickActions(
    chosen: Set<Uri>,
    items: List<Uri>,
    binOn: Boolean,
    onChosen: (Set<Uri>) -> Unit,
    onJob: (FileJob) -> Unit
): List<PadAction> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val res = LocalResources.current
    val list = remember(chosen) { chosen.toList() }
    return listOf(
        PadAction(PadKey.RENAME, Glyphs.TextCursor, R.string.pick_rename) {
            onJob(FileJob.Rename(list))
        },
        PadAction(PadKey.INFO, Icons.Outlined.Info, R.string.pick_info) {
            onJob(FileJob.Facts(list))
        },
        PadAction(PadKey.MOVE, Glyphs.FolderPairDashed, R.string.pick_move) {
            onJob(FileJob.Transfer(list, move = true))
        },
        PadAction(
            key = PadKey.COPY,
            icon = Glyphs.FolderPair,
            label = R.string.menu_copy_here,
            onHold = { onJob(FileJob.Duplicate(list)) },
            holdLabel = R.string.pick_duplicate
        ) { onJob(FileJob.Transfer(list, move = false)) },
        PadAction(PadKey.SHARE, Icons.Default.Share, R.string.menu_share) {
            val now = list
            scope.launch { ImageActions.shareMany(context, now) }
        },
        PadAction(
            key = PadKey.DELETE,
            icon = Glyphs.PickDelete,
            label = R.string.pick_delete,
            onHold = if (!binOn) null else {
                { onJob(FileJob.Delete(list, forGood = true)) }
            },
            holdLabel = if (!binOn) null else R.string.pick_forever
        ) {
            onJob(FileJob.Delete(list, forGood = !binOn))
        },
        PadAction(PadKey.LIST, Icons.AutoMirrored.Outlined.FormatListBulleted, R.string.pick_list) {
            val now = list
            scope.launch {
                ImageActions.copyNames(context, now, null)
                Notices.say(res.getString(R.string.pick_list_done))
            }
        },
        PadAction(
            key = PadKey.ALL,
            icon = Glyphs.PickAll,
            label = R.string.pick_all_short,
            onHold = { onChosen(emptySet()) },
            holdLabel = R.string.pick_none
        ) {
            onChosen(items.toSet())
        },
        PadAction(PadKey.NONE, Glyphs.PickNone, R.string.pick_none) {
            onChosen(emptySet())
        },
        PadAction(PadKey.INVERT, Glyphs.PickInvert, R.string.pick_invert) {
            onChosen(items.toSet() - chosen)
        }
    )
        .filter { Store.allows(it.key) }
        .inOrder(LocalPadLook.current.pick)
}

/**
 * Which picture of the Feed is under a point, as a place in [Feed.Page.items], or `null` over a
 * month heading, a gap or nothing.
 *
 * ⚠️ **The grid's own index counts the month headings too**, so the picture is found by its key,
 * the address the Feed gives every cell ([at] maps it back to the stream).
 */
internal fun LazyGridState.feedIndexAt(point: Offset, at: Map<String, Int>): Int? =
    layoutInfo.visibleItemsInfo.firstOrNull {
        point.x >= it.offset.x && point.x < it.offset.x + it.size.width &&
            point.y >= it.offset.y && point.y < it.offset.y + it.size.height
    }?.let { at[it.key as? String] }

/**
 * The selection while a finger drags from [from] to [hit]: the range is added to [base], or taken
 * from it when the drag started on a chosen picture ([off]), as in a folder's grid.
 */
internal fun feedSpan(items: List<Uri>, base: Set<Uri>, from: Int, hit: Int, off: Boolean): Set<Uri> {
    val span = items.subList(minOf(from, hit), maxOf(from, hit) + 1).toSet()
    return if (off) base - span else base + span
}

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
 * Le azioni della selezione del Feed: lo stesso pannello, le stesse chiavi e lo stesso ordine della
 * griglia di una cartella ([PickSheet]).
 *
 * Gli indirizzi sono quelli del MediaStore, come nella griglia, quindi le operazioni sono le sue, e
 * AIV Play tiene solo quelle che permette ([Store.allows]).
 * 'Lista' scrive i nomi senza l'intestazione della cartella: le immagini del Feed vengono da molte
 * cartelle. 'Tutti' e 'Inverti' lavorano sull'intero flusso, perché il Feed è un elenco solo e non
 * una cartella per mese.
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
 * L'immagine del Feed sotto un punto, come posizione in [Feed.Page.items], o `null` sopra
 * l'intestazione di un mese, uno spazio o niente.
 *
 * ⚠️ L'indice della griglia conta anche le intestazioni dei mesi, quindi l'immagine si trova per
 * chiave, l'indirizzo che il Feed dà a ogni cella ([at] la riporta al flusso).
 */
internal fun LazyGridState.feedIndexAt(point: Offset, at: Map<String, Int>): Int? =
    layoutInfo.visibleItemsInfo.firstOrNull {
        point.x >= it.offset.x && point.x < it.offset.x + it.size.width &&
            point.y >= it.offset.y && point.y < it.offset.y + it.size.height
    }?.let { at[it.key as? String] }

/**
 * La selezione mentre il dito trascina da [from] a [hit]: l'intervallo si aggiunge a [base], o se ne
 * toglie quando il trascinamento è partito da un'immagine scelta ([off]), come nella griglia.
 */
internal fun feedSpan(items: List<Uri>, base: Set<Uri>, from: Int, hit: Int, off: Boolean): Set<Uri> {
    val span = items.subList(minOf(from, hit), maxOf(from, hit) + 1).toSet()
    return if (off) base - span else base + span
}

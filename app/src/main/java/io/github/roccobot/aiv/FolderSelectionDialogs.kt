package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun AuthorizeFolderDialog(
    path: String, selection: FolderSelection,
    onChange: (FolderSelection) -> Unit, onDismiss: () -> Unit
) {
    val included = listedIn(selection.included, path)
    var hasMedia by remember(path) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(path) {
        hasMedia = withContext(Dispatchers.IO) { Tree.list(File(path), true, false).any { it.media } }
    }
    AlertDialog(
        onDismissRequest = onDismiss, modifier = Modifier.lowered(onDismiss), properties = loweredWindow(onDismiss),
        title = { Text(stringResource(if (included) R.string.folder_remove else R.string.folder_authorize_title)) },
        text = {
            Column {
                Text(hiddenShown(portablePath(path)))
                val description = stringResource(if (included) R.string.folder_remove_desc else R.string.folder_authorize_desc)
                Text(if (!included && hasMedia == false) stringResource(R.string.folder_authorize_question) else description)
                if (!included && hasMedia == false) Text(stringResource(R.string.folder_authorize_empty),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = { TextButton(onClick = {
            val own = portablePath(path)
            onChange(selection.copy(included = if (included) selection.included - own else selection.included + own))
            onDismiss()
        }, enabled = included || hasMedia != null) { Text(stringResource(if (included) R.string.folder_remove else R.string.folder_authorize)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

/**
 * Long-press on a system-folder row: authorize in included mode, or hide/show choices.
 *
 * ⚠️⚠️ **Dalla `3.26` il piede della scheda tiene Annulla a sinistra e l'azione principale
 * a destra** (giro 3.24, voce `3.13-08`): le scelte intermedie stanno nel corpo, allineate
 * a sinistra e come tasti pieni, mai "nel vuoto" al centro della scheda.
 * ⚠️⚠️ **Nascondendo una cartella con le sue sottocartelle si registrano anche i figli
 * presenti** (stessa voce): altrimenti restano coperti dal padre ma senza voce in elenco
 * e senza la dicitura "nascosta" nella vista di sistema.
 */
@Composable
internal fun SystemFolderDialog(
    path: String, selection: FolderSelection,
    onChange: (FolderSelection) -> Unit, onDismiss: () -> Unit
) {
    var step by remember(path) { mutableIntStateOf(0) }
    var children by remember(path) { mutableStateOf<List<File>?>(null) }
    LaunchedEffect(path) { children = withContext(Dispatchers.IO) { childFolders(File(path)) } }
    val covered = selection.covering(path)
    val included = selection.mode == FolderMode.INCLUDED
    if (step == 1 && included) {
        AuthorizeFolderDialog(path, selection, onChange, onDismiss)
        return
    }
    if (step == 1 && covered.isEmpty() && children?.isEmpty() == true) {
        AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.lowered(onDismiss), properties = loweredWindow(onDismiss),
            title = { Text(stringResource(R.string.hide_folder_title, File(path).name)) },
            text = { Text(stringResource(R.string.hide_folder_desc)) },
            confirmButton = { TextButton(onClick = { onChange(selection.exclude(listOf(path), false)); onDismiss() }) {
                Text(stringResource(R.string.hide_folder_do)) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
        return
    }
    if (step == 1 && covered.isNotEmpty()) {
        AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.lowered(onDismiss), properties = loweredWindow(onDismiss),
            title = { Text(stringResource(R.string.show_folder_title, hiddenName(covered.minBy { it.length }))) },
            text = { Text(stringResource(R.string.show_folder_desc)) },
            confirmButton = { TextButton(onClick = { onChange(selection.remove(covered)); onDismiss() }) {
                Text(stringResource(R.string.settings_hidden_show)) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
        return
    }
    val name = File(path).name
    val primaryLabel = stringResource(if (included) {
        if (listedIn(selection.included, path)) R.string.folder_remove else R.string.folder_authorize
    } else if (covered.isNotEmpty()) R.string.settings_hidden_show else R.string.hide_folder_do)
    val primaryEnabled = included || covered.isNotEmpty() || children != null
    Sheet(
        title = if (step == 0) name else stringResource(R.string.folder_hide_choices),
        onDismiss = onDismiss,
        foot = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                if (step == 0) {
                    // ⚠️ **Tasto pieno a destra** (giro 3.24, `3.13-03` / `3.13-08`):
                    // Autorizza/Nascondi non è più una riga nel vuoto della scheda.
                    FilledTonalButton(
                        onClick = { step = 1 },
                        enabled = primaryEnabled
                    ) { Text(primaryLabel) }
                }
            }
        }
    ) {
        if (step == 0) {
            // The primary action lives in the foot (3.13-08). The body has 'Faro' from the 6.20.
            PinHomeKey(path, onDismiss)
        } else {
            val descendants = children.orEmpty()
            fun hideBranch(paths: List<String>, recursive: Boolean) {
                onChange(selection.exclude(paths, recursive))
                onDismiss()
            }
            if (descendants.size >= 2) {
                FilledTonalButton(
                    onClick = {
                        hideBranch(listOf(path) + descendants.map { it.absolutePath }, true)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.folder_with_children, name)) }
                FilledTonalButton(
                    onClick = {
                        hideBranch(descendants.map { it.absolutePath }, true)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.folder_only_children, name)) }
            }
            FilledTonalButton(
                onClick = { hideBranch(listOf(path), false) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.folder_only_named, name)) }
            if (descendants.size == 1) {
                FilledTonalButton(
                    onClick = {
                        hideBranch(listOf(path) + descendants.map { it.absolutePath }, true)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.folder_with_child, name)) }
            }
        }
    }
}

/**
 * 'Aggiungi alla schermata Home' ('Faro', from the 6.20): the folder becomes an icon of its own on
 * the home screen, which opens it here, in 'Cartelle di sistema' (`Shortcuts.pinFolder`).
 *
 * ⚠️ **Only from 'Cartelle di sistema'**, his rule (note C on the giro of the 6.10: *solo dalla
 * vista cartelle di sistema*): this sheet is the long press of that view only, so the key needs no
 * condition on the view. AIV Play has no such view.
 * ⚠️ **Missing when the launcher does not take pinned icons**: a key that does nothing is worse
 * than a missing one. Android asks where to put the icon, so the sheet closes at once.
 */
@Composable
private fun PinHomeKey(path: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    if (!remember(context) { Shortcuts.canPin(context) }) return
    val scope = rememberCoroutineScope()
    FilledTonalButton(
        onClick = {
            scope.launch(NonCancellable) { Shortcuts.pinFolder(context.applicationContext, path) }
            onDismiss()
        },
        modifier = Modifier.fillMaxWidth()
    ) { Text(stringResource(R.string.folder_pin_home)) }
}

@Composable
internal fun SiblingFoldersDialog(path: String, selection: FolderSelection,
    onChange: (FolderSelection) -> Unit, onDismiss: () -> Unit) {
    val parent = File(path).parentFile ?: return
    var children by remember(path) { mutableStateOf<List<File>?>(null) }
    LaunchedEffect(path) { children = withContext(Dispatchers.IO) { childFolders(parent) } }
    AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.lowered(onDismiss), properties = loweredWindow(onDismiss),
        title = { Text(stringResource(R.string.folder_siblings)) },
        text = { Text(stringResource(R.string.folder_siblings_desc, parent.name)) },
        confirmButton = { TextButton(onClick = {
            onChange(selection.exclude(children.orEmpty().map { it.absolutePath }, true)); onDismiss()
        }, enabled = children != null) { Text(stringResource(R.string.hide_folder_do)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}

/**
 * Il tocco lungo su una cartella della casa: chiede, non nasconde subito.
 *
 * ⚠️⚠️ **È LO STESSO DIALOGO DEL TELEFONO E DEL RAIL TABLET, DALLA `3.42`**
 * (`3.41-01`): prima viveva solo dentro [FolderScreen], e il rail non aveva il
 * gesto. Le voci restano quelle (nascondi, mostra, sorelle, autorizza in modalità
 * incluse). Il tocco lungo apre questo, il conferma esegue.
 * ⚠️ **Le sorelle stanno qui dentro e non in un secondo stato del chiamante**:
 * aprirle chiude la domanda e lascia solo il loro dialogo, come prima.
 */
@Composable
internal fun FolderHoldDialog(
    bucket: Folder.Bucket,
    selection: FolderSelection,
    onHide: (Folder.Bucket) -> Unit,
    onUnhide: (Collection<String>) -> Unit,
    onSelectionChange: (FolderSelection) -> Unit,
    onDismiss: () -> Unit,
) {
    var siblings by remember { mutableStateOf<String?>(null) }
    siblings?.let { path ->
        SiblingFoldersDialog(path, selection, onSelectionChange, onDismiss)
        return
    }
    /*
     * ⚠️⚠️ **FROM THE `5.80` THE LONG PRESS ASKS WHICH ACTION FIRST** (his request of 2026-10-12,
     * answer `B1`: grid, list and tablet column): 'Nascondi' or 'Mostra' goes on to the questions
     * below, unchanged; the favourite is flipped at once, because adding or removing it hides
     * nothing and is undone by the same gesture.
     */
    var chosen by remember { mutableStateOf(false) }
    if (!chosen) {
        FolderActionDialog(
            bucket = bucket,
            selection = selection,
            onVisibility = { chosen = true },
            onFavourite = { path ->
                onSelectionChange(selection.flipFavourite(path))
                onDismiss()
            },
            onDismiss = onDismiss
        )
        return
    }
    if (selection.mode == FolderMode.INCLUDED) {
        val path = bucket.path ?: return
        AuthorizeFolderDialog(path, selection, onSelectionChange, onDismiss)
        return
    }
    /*
     * ⚠️⚠️ **IL TOCCO LUNGO SU UNA CARTELLA IN PRESTITO PROPONE IL CONTRARIO, DALLA
     * `1.93`**. Con 'Mostra nascoste' acceso una cartella nascosta è in scena, e il
     * gesto offre di rimostrarla, non di nasconderla un'altra volta.
     * ⚠️⚠️ **LE VOCI CHE LA COPRONO, DALLA `2.96`**: una cartella dentro una nascosta
     * è coperta dall'antenata. Si tolgono tutte, e il titolo nomina la più in alto.
     */
    val coprono = bucket.path?.let { selection.covering(it) }.orEmpty()
    val nascosta = coprono.isNotEmpty()
    val propria = bucket.path?.let(::portablePath)
    val alta = coprono.minByOrNull { it.length }
    val nome = if (alta == null || alta == propria) bucket.name else hiddenName(alta)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(onDismiss), properties = loweredWindow(onDismiss),
        title = {
            Text(stringResource(
                if (nascosta) R.string.show_folder_title else R.string.hide_folder_title,
                nome
            ))
        },
        text = {
            Column {
                Text(stringResource(
                    if (nascosta) R.string.show_folder_desc else R.string.hide_folder_desc
                ))
                val path = bucket.path
                var peers by remember(path, selection) { mutableStateOf<List<File>>(emptyList()) }
                LaunchedEffect(path, selection) {
                    if (path != null) peers = withContext(Dispatchers.IO) {
                        siblingFolders(path, selection)
                    }
                }
                if (!nascosta && peers.isNotEmpty()) FilledTonalButton(
                    onClick = { siblings = path },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text(stringResource(R.string.folder_siblings)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nascosta) onUnhide(coprono) else onHide(bucket)
                onDismiss()
            }) {
                Text(stringResource(
                    if (nascosta) R.string.settings_hidden_show else R.string.hide_folder_do
                ))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * 'Scegli l'azione sulla cartella': the first step of the long press on a folder, from the `5.80`.
 *
 * ⚠️ **Two equal rounded keys, one above the other**, his specification; the favourite text may go
 * on two lines, which he allowed (*possono stare anche su sue righe*).
 * ⚠️ **The first key says what the gesture would do**: 'Mostra' for a folder on loan during 'Mostra
 * nascoste', 'Nascondi' otherwise, also in the included mode, where it leads to taking away the
 * authorization (the dialog that follows says so).
 * ⚠️ **A folder without a path has no favourite key**: it could not be found again (see
 * `Folder.Bucket.path`), and an enabled key that does nothing is worse than a missing one.
 */
@Composable
internal fun FolderActionDialog(
    bucket: Folder.Bucket,
    selection: FolderSelection,
    onVisibility: () -> Unit,
    onFavourite: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val path = bucket.path
    val shown = selection.mode == FolderMode.EXCLUDED &&
        path?.let { selection.covering(it).isNotEmpty() } == true
    val favourite = path?.let(selection::favourite) == true
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(onDismiss), properties = loweredWindow(onDismiss),
        title = { Text(stringResource(R.string.folder_action_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionKey(
                    stringResource(if (shown) R.string.settings_hidden_show else R.string.hide_folder_do),
                    onVisibility
                )
                if (path != null) ActionKey(
                    stringResource(if (favourite) R.string.favourite_remove else R.string.favourite_add)
                ) { onFavourite(path) }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * The question of the grid order, asked once while [FolderSelection.order] is `null`, from the
 * `5.80`.
 *
 * ⚠️⚠️ **TEXT AND KEYS ARE HIS** (request of 2026-10-12): the question as the body, and two keys
 * as wide as each other, one above the other, `Ordine alfabetico` and `Più recenti in alto`. The
 * first is not the chip's name on purpose (*il nome diverso è voluto*).
 * ⚠️ **No title**: the question is the whole window, and a title would say it twice.
 * ⚠️ **Not modal**: it asks for no written input (`Rules.md` § '👆 Che cosa fa il tocco FUORI da una
 * finestra'). Closed without an answer it comes back at the next start, because the order is
 * still not chosen.
 */
@Composable
internal fun FolderOrderDialog(onPick: (FolderOrder) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(onDismiss), properties = loweredWindow(onDismiss),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.folder_order_ask),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                ActionKey(stringResource(R.string.folder_order_alpha_long)) { onPick(FolderOrder.ALPHA) }
                ActionKey(stringResource(R.string.folder_order_recent)) { onPick(FolderOrder.RECENT) }
            }
        },
        confirmButton = {}
    )
}

/** One of the two keys of [FolderActionDialog]: full width, rounded, at least as tall as a key. */
@Composable
private fun ActionKey(text: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = ACTION_KEY)
    ) {
        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleSmall)
    }
}

/** The height of a key of [FolderActionDialog]: larger than a text key, as he asked (*grandi*). */
private val ACTION_KEY = 56.dp


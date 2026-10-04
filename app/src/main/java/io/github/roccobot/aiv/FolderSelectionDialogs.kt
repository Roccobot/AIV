package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
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
            // Body stays empty on purpose: the primary action lives in the foot (3.13-08).
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


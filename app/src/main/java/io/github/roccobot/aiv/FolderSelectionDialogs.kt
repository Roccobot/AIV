package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
        onDismissRequest = onDismiss, modifier = Modifier.lowered(onDismiss),
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
        AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.lowered(onDismiss),
            title = { Text(stringResource(R.string.hide_folder_title, File(path).name)) },
            text = { Text(stringResource(R.string.hide_folder_desc)) },
            confirmButton = { TextButton(onClick = { onChange(selection.exclude(listOf(path), false)); onDismiss() }) {
                Text(stringResource(R.string.hide_folder_do)) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
        return
    }
    if (step == 1 && covered.isNotEmpty()) {
        AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.lowered(onDismiss),
            title = { Text(stringResource(R.string.show_folder_title, hiddenName(covered.minBy { it.length }))) },
            text = { Text(stringResource(R.string.show_folder_desc)) },
            confirmButton = { TextButton(onClick = { onChange(selection.remove(covered)); onDismiss() }) {
                Text(stringResource(R.string.settings_hidden_show)) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
        return
    }
    val name = File(path).name
    Sheet(title = if (step == 0) name else stringResource(R.string.folder_hide_choices), onDismiss = onDismiss) {
        if (step == 0) {
            TextButton(onClick = { step = 1 }, enabled = included || covered.isNotEmpty() || children != null,
                modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (included) {
                    if (listedIn(selection.included, path)) R.string.folder_remove else R.string.folder_authorize
                } else if (covered.isNotEmpty()) R.string.settings_hidden_show else R.string.hide_folder_do))
            }
        } else {
            val descendants = children.orEmpty()
            if (descendants.size >= 2) {
                TextButton(onClick = { onChange(selection.exclude(listOf(path), true)); onDismiss() }) {
                    Text(stringResource(R.string.folder_with_children, name)) }
                TextButton(onClick = { onChange(selection.exclude(descendants.map { it.absolutePath }, true)); onDismiss() }) {
                    Text(stringResource(R.string.folder_only_children, name)) }
            }
            TextButton(onClick = { onChange(selection.exclude(listOf(path), false)); onDismiss() }) {
                Text(stringResource(R.string.folder_only_named, name)) }
            if (descendants.size == 1) TextButton(onClick = { onChange(selection.exclude(listOf(path), true)); onDismiss() }) {
                Text(stringResource(R.string.folder_with_child, name)) }
        }
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
    }
}

@Composable
internal fun SiblingFoldersDialog(path: String, selection: FolderSelection,
    onChange: (FolderSelection) -> Unit, onDismiss: () -> Unit) {
    val parent = File(path).parentFile ?: return
    var children by remember(path) { mutableStateOf<List<File>?>(null) }
    LaunchedEffect(path) { children = withContext(Dispatchers.IO) { childFolders(parent) } }
    AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.lowered(onDismiss),
        title = { Text(stringResource(R.string.folder_siblings)) },
        text = { Text(stringResource(R.string.folder_siblings_desc, parent.name)) },
        confirmButton = { TextButton(onClick = {
            onChange(selection.exclude(children.orEmpty().map { it.absolutePath }, true)); onDismiss()
        }, enabled = children != null) { Text(stringResource(R.string.hide_folder_do)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}

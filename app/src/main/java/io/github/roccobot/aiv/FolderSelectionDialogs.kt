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
 * Il tocco lungo su una riga di 'Cartelle di sistema': autorizza nella modalità delle incluse,
 * altrimenti le scelte per nascondere o mostrare.
 *
 * Il piede della scheda ha 'Annulla' a sinistra e l'azione principale a destra; le scelte
 * intermedie sono nel corpo, come tasti pieni a tutta larghezza (voce `3.13-08`).
 * ⚠️ **Nascondendo una cartella con le sue sottocartelle si registrano anche i figli presenti**:
 * altrimenti resterebbero coperti dal padre, ma senza una voce in elenco e senza la dicitura
 * 'nascosta' nella vista di sistema.
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
                    // L'azione principale è un tasto pieno a destra (voci `3.13-03` e `3.13-08`).
                    FilledTonalButton(
                        onClick = { step = 1 },
                        enabled = primaryEnabled
                    ) { Text(primaryLabel) }
                }
            }
        }
    ) {
        if (step == 0) {
            // L'azione principale è nel piede; il corpo ha 'Aggiungi alla schermata Home'.
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
 * 'Aggiungi alla schermata Home': la cartella diventa un'icona sua nella schermata Home, che la
 * apre qui, in 'Cartelle di sistema' (`Shortcuts.pinFolder`).
 *
 * Solo da 'Cartelle di sistema', per regola dell'utente (nota C del giro della 6.10): questa
 * scheda è il tocco lungo di quella sola vista, quindi il tasto non ha bisogno di condizioni.
 * AIV Play non ha quella vista.
 * Il tasto manca quando il launcher non accetta icone fissate. Android chiede dove mettere
 * l'icona, quindi la scheda si chiude subito.
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
 * È la stessa finestra per il telefono e per la colonna del tablet (voce `3.41-01`): nascondi,
 * mostra, sorelle, autorizza nella modalità delle incluse. Il tocco lungo la apre, la conferma
 * esegue.
 * Le sorelle vivono qui dentro e non in un secondo stato del chiamante: aprirle chiude la domanda
 * e lascia solo la loro finestra.
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
     * Il tocco lungo chiede prima quale azione (risposta `B1` dell'utente: griglia, lista e
     * colonna del tablet): 'Nascondi' o 'Mostra' prosegue con le domande sotto; la preferita si
     * cambia subito, perché non nasconde niente e lo stesso gesto la annulla.
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
     * Con 'Mostra nascoste' acceso una cartella nascosta è visibile, e il gesto offre di
     * rimostrarla. Una cartella dentro una nascosta è coperta dall'antenata: si tolgono tutte le
     * voci che la coprono, e il titolo nomina la più in alto.
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
 * 'Scegli l'azione sulla cartella': il primo passo del tocco lungo su una cartella.
 *
 * Due tasti arrotondati uguali, uno sopra l'altro, come li ha descritti l'utente; il testo della
 * preferita può andare su due righe.
 * Il primo tasto dice che cosa farebbe il gesto: 'Mostra' per una cartella nascosta resa visibile
 * da 'Mostra nascoste', altrimenti 'Nascondi', anche nella modalità delle incluse, dove porta a
 * togliere l'autorizzazione (lo dice la finestra che segue).
 * Una cartella senza percorso non ha il tasto della preferita: non si potrebbe ritrovare
 * (`Folder.Bucket.path`).
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
 * La domanda sull'ordine della griglia, fatta finché [FolderSelection.order] è `null`.
 *
 * ⚠️ **Testo e tasti sono dell'utente**: la domanda come corpo e due tasti larghi uguali, uno
 * sopra l'altro. Il primo ha di proposito un nome diverso da quello del chip.
 * Senza titolo: la domanda è tutta la finestra. Non è modale, perché non chiede input scritto
 * (`Rules.md` § '👆 Che cosa fa il tocco FUORI da una finestra'); chiusa senza risposta, torna
 * al prossimo avvio.
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

/** Un tasto di [FolderActionDialog] e di [FolderOrderDialog]: a tutta larghezza, arrotondato. */
@Composable
private fun ActionKey(text: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = ACTION_KEY)
    ) {
        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleSmall)
    }
}

/** L'altezza minima di un [ActionKey]: più alto di un tasto di testo, come ha chiesto l'utente. */
private val ACTION_KEY = 56.dp


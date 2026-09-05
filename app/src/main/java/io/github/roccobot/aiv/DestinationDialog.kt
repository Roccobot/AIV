package io.github.roccobot.aiv

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File

/**
 * Dove mettere le fotografie scelte: un giro fra le cartelle **vere** del telefono.
 *
 * ⚠️⚠️ **È UN DIALOGO A TUTTO SCHERMO E NON UNA SCHERMATA DEL MODELLO, e la ragione è la
 * SELEZIONE**: quello che si sta per copiare vive in `GridScreen` e se ne va con lei
 * (deliberatamente: uscire da una cartella vuol dire 'lascia stare'). Navigando davvero
 * verso un'altra schermata la selezione sparirebbe proprio mentre la si sta usando, e per
 * evitarlo bisognerebbe spostarla nel modello, cioè farle sopravvivere anche quando non
 * serve. Un dialogo sta **sopra** la griglia, che resta viva sotto.
 *
 * ⚠️ **Mostra il filesystem e non gli album** (richiesta dell'utente): vedi la nota in
 * testa a `FileTree` per il perché l'elenco della galleria sarebbe l'insieme sbagliato.
 *
 * ⚠️ **[action] dice che cosa succederà, e non è un dettaglio di parole**: copiare e
 * spostare chiedono la stessa cartella e fanno due cose diverse, una innocua e una no.
 * L'ultimo posto in cui si può ancora distinguerle è il tasto che le avvia.
 */
@Composable
fun DestinationDialog(
    @StringRes action: Int,
    onDismiss: () -> Unit,
    onPick: (File) -> Unit
) {
    val context = LocalContext.current
    val roots = remember { FileTree.roots(context) }

    /**
     * Dove si sta guardando, e `null` finché si è all'elenco delle memorie.
     *
     * ⚠️ **La radice non è una cartella qualunque**: sui telefoni con la scheda ce ne sono
     * due, e senza un livello sopra non ci sarebbe modo di passare dall'una all'altra. Su
     * un telefono senza scheda ne ha una sola, e allora si entra dritti dentro.
     */
    var here by remember { mutableStateOf(roots.singleOrNull()) }
    var children by remember { mutableStateOf<List<File>?>(null) }
    var naming by remember { mutableStateOf(false) }

    /*
     * ⚠️⚠️ **IL CESTINO NON È UNA DESTINAZIONE, ed è una richiesta esplicita**: copiarci
     * dentro vorrebbe dire mettere una fotografia in un posto che si svuota, e spostarcela
     * sarebbe eliminarla passando dalla porta di servizio, senza la conferma e senza che
     * l'archivio delle provenienze ne sappia niente. Quindi sparisce dall'elenco.
     * ⚠️ **Il tasto in fondo controlla di nuovo**, e non è una ripetizione inutile: ci si
     * arriva anche entrando nella cartella dell'app da un altro ramo, e in quel caso
     * l'elenco non c'entra. Due controlli per due strade diverse.
     */
    LaunchedEffect(here) {
        children = here?.let { dir -> FileTree.children(dir).filterNot { Bin.holds(context, it) } }
    }

    /** Risale di un livello, e torna `false` quando non c'è più niente sopra. */
    fun up(): Boolean {
        val at = here ?: return false
        // ⚠️ Il confronto è sul PERCORSO e non sull'oggetto: due `File` costruiti in due
        // modi diversi non sono uguali fra loro nemmeno quando indicano la stessa cosa.
        if (roots.any { it.absolutePath == at.absolutePath }) {
            // Con una memoria sola non esiste un elenco a cui tornare: si esce.
            if (roots.size <= 1) return false
            here = null
            return true
        }
        here = at.parentFile ?: return false
        return true
    }

    Dialog(
        onDismissRequest = onDismiss,
        // ⚠️ Senza questo un dialogo resta stretto in mezzo allo schermo, e qui dentro c'è
        // un elenco da scorrere: la larghezza predefinita lo renderebbe una fessura.
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler { if (!up()) onDismiss() }

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (!up()) onDismiss() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = here?.name ?: stringResource(R.string.dest_storages),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // ⚠️ Il percorso intero sotto il nome, e non al suo posto: due
                        // cartelle possono chiamarsi uguale, ma un percorso da settanta
                        // caratteri come titolo non si legge.
                        here?.let {
                            Text(
                                text = it.absolutePath,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.MiddleEllipsis
                            )
                        }
                    }
                    if (here != null) {
                        IconButton(onClick = { naming = true }) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = stringResource(R.string.dest_new)
                            )
                        }
                    }
                }

                HorizontalDivider()

                val listed = if (here == null) roots else children
                // ⚠️ Il peso sta sul contenitore e non sull'elenco: così il tasto in fondo
                // resta in fondo anche quando la cartella è vuota, invece di saltare a
                // metà schermo.
                Box(modifier = Modifier.weight(1f)) {
                when {
                    listed == null -> Unit
                    listed.isEmpty() -> Text(
                        text = stringResource(R.string.dest_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(items = listed, key = { it.absolutePath }) { dir ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { here = dir }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(text = dir.name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                }

                // ⚠️⚠️ **SI SCEGLIE LA CARTELLA IN CUI SI È, non una toccata nell'elenco**:
                // il tocco su una riga ENTRA, e deve farlo, o non si potrebbe mai arrivare
                // in fondo a un ramo. Le due cose sono gesti diversi apposta, e il tasto
                // dice quale cartella prenderebbe.
                here?.let { dir ->
                    Button(
                        onClick = { onPick(dir) },
                        enabled = !Bin.holds(context, dir),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(stringResource(action, dir.name))
                    }
                }
            }
        }

        if (naming) {
            NewFolderDialog(
                onDismiss = { naming = false },
                onCreate = { name ->
                    naming = false
                    val parent = here ?: return@NewFolderDialog
                    val made = File(parent, name)
                    if (runCatching { made.mkdirs() }.getOrDefault(false)) here = made
                }
            )
        }
    }
}

/**
 * Il nome di una cartella nuova.
 *
 * ⚠️ **I nomi si ripuliscono invece di essere rifiutati**: la barra è l'unico carattere
 * che su Android non può stare in un nome di file, e uno incollato per sbaglio
 * trasformerebbe una cartella in due. Toglierlo è più utile di un messaggio d'errore.
 */
@Composable
private fun NewFolderDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val clean = text.replace('/', ' ').trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(null),
        title = { Text(stringResource(R.string.dest_new)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(clean) },
                enabled = clean.isNotEmpty()
            ) { Text(stringResource(R.string.dest_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

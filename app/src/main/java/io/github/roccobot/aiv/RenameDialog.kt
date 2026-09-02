package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Rinominare in blocco: un template col numero dentro, e il primo numero.
 *
 * ⚠️⚠️ **L'ANTEPRIMA NON È UN ORNAMENTO: è l'unica cosa che rende reversibile una
 * decisione irreversibile.** Rinominare ottanta file è un gesto che non si annulla, e
 * senza vedere prima come verranno i nomi l'unico modo di accorgersi di un template
 * sbagliato sarebbe averlo già applicato. Mostra i primi e **l'ultimo**, che è quello che
 * dice se le cifre bastano.
 * ⚠️ **Il template si propone UNA VOLTA e poi non si tocca più**: ricalcolarlo mentre si
 * cambia il primo numero riscriverebbe sotto le dita un testo che si sta scrivendo.
 * ⚠️ **I nomi arrivano già ordinati da `FileTree.namesOf`**, che usa lo stesso ordinamento
 * della rinomina vera: se l'anteprima ordinasse per conto suo, mostrerebbe un abbinamento
 * che poi non succede.
 */
@Composable
fun RenameDialog(
    uris: List<Uri>,
    onDismiss: () -> Unit,
    onRename: (template: String, start: Int) -> Unit
) {
    val context = LocalContext.current
    val names by produceState<List<String>?>(null, uris) {
        value = FileTree.namesOf(context, uris)
    }

    var template by rememberSaveable { mutableStateOf("") }
    var start by rememberSaveable { mutableStateOf("1") }
    var proposed by rememberSaveable { mutableStateOf(false) }

    // ⚠️⚠️ **UN FILE SOLO NON È UNA RINOMINA IN BLOCCO, e dalla 1.25 non ne ha più l'aria**
    // (riscontro dell'utente, 2026-09-02: *`Rinomina` sul file singolo deve partire dal nome
    // originale, non da un template di rinomina batch*). Con un file la schermata proponeva
    // `Museo ##`, chiedeva da che numero partire e spiegava i cancelletti: tre cose che
    // servono a numerare ottanta foto e nessuna che serva a cambiare un nome.
    val singolo = uris.size == 1

    val listed = names
    LaunchedEffect(listed) {
        if (proposed || listed.isNullOrEmpty()) return@LaunchedEffect
        proposed = true
        // ⚠️ Il nome **senza estensione**, perché l'estensione la rimette `renderName`: con
        // lei dentro il template il file diventerebbe `foto.jpg.jpg`.
        template = if (singolo) listed.first().substringBeforeLast('.', listed.first())
        else suggestTemplate(listed.first(), listed.size, start.toIntOrNull() ?: 1)
    }

    val first = start.toIntOrNull()
    val clean = template.trim()
    // ⚠️ Il cancelletto è obbligatorio **solo** quando i file sono più di uno: con un file
    // solo questa schermata è la rinomina normale, e pretendere un numero dentro il nome
    // sarebbe pretendere una numerazione da un solo elemento.
    val numbered = clean.contains('#')
    val ready = listed != null && clean.isNotEmpty() && first != null && first >= 0 &&
        (uris.size == 1 || numbered)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_rename)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = template,
                    onValueChange = { template = it },
                    label = { Text(stringResource(R.string.rename_template)) },
                    singleLine = true,
                    // ⚠️ Con un file solo il tasto della tastiera dice 'fine' e non 'avanti':
                    // sotto non c'è più nessuna casella dove andare.
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (singolo) ImeAction.Done else ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                // ⚠️ Il primo numero e la spiegazione dei cancelletti escono di scena con un
                // file solo: là non c'è niente da numerare, e una casella che non decide
                // niente si legge come una cosa da riempire.
                if (!singolo) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = start,
                        // ⚠️ Si filtra alle cifre invece di validare dopo: una tastiera
                        // numerica su Android serve comunque virgole e segni, e un numero
                        // negativo qui non vuol dire niente.
                        onValueChange = { typed -> start = typed.filter { it.isDigit() }.take(6) },
                        label = { Text(stringResource(R.string.rename_start)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.rename_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (listed != null && first != null && clean.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.rename_preview),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    for (row in previewOf(listed, clean, first)) {
                        Text(
                            text = row,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(clean, first ?: 1) },
                enabled = ready
            ) { Text(stringResource(R.string.pick_rename)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Le righe dell'anteprima: i primi tre abbinamenti e **l'ultimo**.
 *
 * ⚠️ L'ultimo c'è perché porta il numero più alto, che è il solo modo di vedere se le
 * cifre del template bastano: con `##` e centoventi file, la riga finale dice `120` e si
 * capisce al volo che i nomi non si ordineranno come ci si aspetta.
 */
private fun previewOf(names: List<String>, template: String, start: Int): List<String> {
    if (names.isEmpty()) return emptyList()
    val rows = ArrayList<String>(5)
    val head = minOf(names.size, 3)
    for (at in 0 until head) rows += pairing(names[at], template, start + at)
    if (names.size > head + 1) rows += "..."
    if (names.size > head) rows += pairing(names.last(), template, start + names.lastIndex)
    return rows
}

/** Un nome di adesso e quello di dopo, sulla stessa riga. */
private fun pairing(name: String, template: String, number: Int): String {
    val extension = name.substringAfterLast('.', "")
    return "$name  ->  ${renderName(template, number, extension)}"
}

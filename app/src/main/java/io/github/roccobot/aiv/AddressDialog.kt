package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch

/**
 * Aprire un indirizzo diretto: si digita, o si sceglie fra quelli già visti.
 *
 * ⚠️⚠️ **QUI DENTRO SONO FINITE TRE VIE CHE PRIMA ERANO TRE TASTI** della schermata
 * iniziale, sparita con la `0.41`: digitare un URL, incollarlo dagli appunti e riaprire
 * un recente. Non è un accorpamento per far posto: sono la stessa domanda ('quale
 * indirizzo?') con tre modi di rispondere, e tenerle separate costringeva a scegliere il
 * *modo* prima di scegliere la *cosa*.
 *
 * ⚠️ **Gli appunti si leggono per RIEMPIRE il campo, non per aprire da soli**: chi arriva
 * qui con un indirizzo copiato lo trova già scritto e tocca 'Apri', chi non ce l'ha
 * digita, e nessuno dei due incontra un messaggio d'errore per una cosa che non aveva
 * chiesto. ⚠️ Da Android 12 il sistema annuncia ogni lettura degli appunti: qui succede
 * su un tocco esplicito, che è il caso in cui quell'avviso è giusto.
 *
 * ⚠️ **I recenti sono SOLO indirizzi remoti** (`Recents.remember` scarta tutto il resto),
 * ed è la ragione per cui vivono qui e non altrove: un elenco di foto del telefono
 * sarebbe un doppione della griglia, un elenco di indirizzi no.
 */
@Composable
fun AddressDialog(
    recents: List<RecentImage>,
    onOpen: (Uri) -> Unit,
    onForget: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var typed by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<Int?>(null) }

    // ⚠️ Alla comparsa e una volta sola: l'app ha il fuoco (il dialogo è appena stato
    // aperto da un tocco), che da Android 10 è la condizione per poter leggere.
    LaunchedEffect(Unit) {
        if (typed.isEmpty()) typed = ImageActions.urlInClipboard(context)?.toString().orEmpty()
    }

    /**
     * ⚠️⚠️ **L'INDIRIZZO SI CONTROLLA PRIMA DI APRIRE, e vale la richiesta che costa**:
     * chi scrive 'esempio.it/pagina' se lo sente dire **qui**, con il campo ancora
     * davanti e il testo ancora dentro, invece di ritrovarsi nel visualizzatore con un
     * errore generico e la propria riga da riscrivere. ⚠️ Non è un doppione dell'errore
     * del visualizzatore: quello dice 'non si è aperta', questo dice 'non è un'immagine',
     * e sono due cose diverse.
     * ⚠️ Su un indirizzo che finisce in `.jpg` la verifica **non tocca la rete**
     * (`leadsToImage` risponde dall'estensione), quindi il caso comune non aspetta
     * niente.
     */
    fun go(uri: Uri?) {
        if (uri == null) return
        problem = null
        busy = true
        scope.launch {
            if (ImageActions.leadsToImage(uri)) {
                onDismiss()
                onOpen(uri)
            } else {
                problem = R.string.url_not_image
                busy = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.hub_url)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.url_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    enabled = !busy,
                    keyboardActions = KeyboardActions(onGo = { go(typedToUri(typed)) })
                )
                problem?.let {
                    Text(
                        text = stringResource(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (busy) CircularProgressIndicator(Modifier.size(20.dp))
                if (recents.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.url_recent),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // ⚠️ Un tetto all'altezza, non al numero: se ne tengono otto, e otto
                    // righe intere spingerebbero i tasti del dialogo fuori dai telefoni
                    // bassi. Scorrendo ci sono tutte.
                    Column(
                        modifier = Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState())
                    ) {
                        recents.forEach { entry ->
                            TextButton(
                                onClick = { go(entry.address.toUri()) },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = entry.name.ifBlank { entry.address },
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    TextButton(onClick = onForget, enabled = !busy) {
                        Text(stringResource(R.string.url_forget))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { go(typedToUri(typed)) }, enabled = typed.isNotBlank() && !busy) {
                Text(stringResource(R.string.url_open))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.url_cancel)) }
        }
    )
}

/**
 * Quello che si è scritto, come indirizzo, con l'unica riparazione sempre sicura: senza
 * schema si mette `https://`.
 *
 * ⚠️ Digitare 'esempio.it/gatto.jpg' è quello che fanno tutti, e rifiutarlo sarebbe
 * pedanteria e non prudenza.
 */
private fun typedToUri(typed: String): Uri? {
    val text = typed.trim()
    if (text.isEmpty()) return null
    val hasScheme = Regex("^[a-z][a-z0-9+.-]*://", RegexOption.IGNORE_CASE).containsMatchIn(text)
    return (if (hasScheme) text else "https://$text").toUri()
}

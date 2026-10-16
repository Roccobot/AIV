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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch

/**
 * Aprire un indirizzo diretto: si digita, si incolla o si sceglie fra quelli già visti. Sono la
 * stessa domanda ('quale indirizzo?') con tre modi di rispondere, quindi vivono in una finestra sola.
 *
 * ⚠️ **Gli appunti riempiono il campo, non aprono da soli**: chi ha un indirizzo copiato lo trova
 * scritto e tocca 'Apri', e nessuno incontra un errore per una cosa che non ha chiesto. Da
 * Android 12 il sistema annuncia ogni lettura degli appunti: qui succede dopo un tocco esplicito.
 *
 * I recenti sono solo indirizzi remoti (`Recents.remember` scarta il resto): un elenco di immagini
 * del telefono sarebbe un doppione della griglia.
 */
@Composable
fun AddressDialog(
    recents: List<RecentImage>,
    onOpen: (Uri) -> Unit,
    /**
     * Le immagini di una pagina, quando l'indirizzo non conduce a un'immagine (vedi
     * `WebSeries.Rule.PAGE_LINKS`).
     */
    onOpenPage: (Folder.Series) -> Unit,
    onForget: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Salvato: girando il telefono quello che si è scritto resta nel campo.
    var typed by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<Int?>(null) }

    // ⚠️ Alla comparsa e una volta sola: l'app ha il fuoco (il dialogo è appena stato aperto da
    // un tocco), che da Android 10 è la condizione per leggere gli appunti.
    LaunchedEffect(Unit) {
        if (typed.isEmpty()) typed = ImageActions.urlInClipboard(context)?.toString().orEmpty()
    }

    /**
     * ⚠️ **L'indirizzo si controlla prima di aprire**: chi ha sbagliato lo sa qui, col testo
     * ancora nel campo, e non nel visualizzatore con un errore generico. Su un indirizzo che
     * finisce in `.jpg` la verifica non tocca la rete (`leadsToImage` risponde dall'estensione).
     *
     * Un indirizzo che non è un'immagine si prova come pagina: se contiene immagini, si aprono
     * come serie, nell'ordine in cui la pagina le presenta. La pagina si prova solo dopo il no
     * dell'immagine, quindi l'indirizzo di un'immagine non paga niente in più. L'errore
     * ([R.string.url_not_image]) dice tutte e due le cose, perché si sono provate tutte e due.
     */
    fun go(uri: Uri?) {
        if (uri == null) return
        problem = null
        busy = true
        scope.launch {
            if (ImageActions.leadsToImage(uri)) {
                onDismiss()
                onOpen(uri)
                return@launch
            }
            val page = WebSeries.fromPage(uri)
            if (page != null) {
                onDismiss()
                onOpenPage(page)
            } else {
                problem = R.string.url_not_image
                busy = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(null),
        properties = loweredWindow(null),
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
                    /*
                     * ⚠️ **Il tetto di una riga va sul segnaposto**: `singleLine` vincola il
                     * testo che si digita e non questo `Text`, che senza tetto va a capo dopo
                     * un punto o una barra e stacca il dominio dal suo TLD. Un esempio più
                     * corto sposterebbe la soglia senza toglierla, perché la larghezza dipende
                     * dallo schermo e dal corpo del testo del telefono.
                     * L'ellissi è in mezzo: l'esempio dice che un indirizzo diretto finisce
                     * con l'estensione di un'immagine, e l'ellissi in fondo la toglierebbe
                     * (la stessa ragione scritta su `nameWithExt`).
                     * L'esempio è uno solo per tutte le lingue (istruzione dell'utente, giro
                     * della 1.47): la stringa è `translatable="false"`, e il verificatore delle
                     * lingue controlla che nessuna cartella ne tenga una copia.
                     */
                    placeholder = {
                        Text(
                            text = stringResource(R.string.url_hint),
                            maxLines = 1,
                            overflow = TextOverflow.MiddleEllipsis
                        )
                    },
                    singleLine = true,
                    // La stessa stondatura dei riquadri della rinomina: il numero vive in un
                    // posto solo.
                    shape = BOX_SHAPE,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    enabled = !busy,
                    keyboardActions = KeyboardActions(onGo = { go(typedToUri(typed)) }),
                    /*
                     * ⚠️ **L'errore appartiene al campo**: scritto fuori, in un nodo fratello,
                     * il campo non si colorerebbe e un lettore di schermo non incontrerebbe il
                     * messaggio. I due parametri vanno insieme: `isError` dà il colore e lo
                     * stato, `supportingText` mette la frase dove il lettore la trova.
                     */
                    isError = problem != null,
                    supportingText = problem?.let {
                        { Text(text = stringResource(it), style = MaterialTheme.typography.bodySmall) }
                    }
                )
                if (busy) CircularProgressIndicator(Modifier.size(20.dp))
                if (recents.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.url_recent),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    /*
                     * Un tetto all'altezza, non al numero: otto righe intere spingerebbero i
                     * tasti del dialogo fuori dai telefoni bassi, e scorrendo ci sono tutte.
                     * ⚠️⚠️ **Il tetto tiene anche in piedi lo scorrimento annidato**: questa
                     * colonna scorre dentro il corpo di un `AlertDialog`, che scorre già, e due
                     * scorrimenti sullo stesso asse misurano il contenuto interno con altezza
                     * infinita, quindi `checkScrollableContainerConstraints` va in errore (vedi
                     * la testa di `Sheet.kt`). `heightIn` precede `verticalScroll`, e il
                     * vincolo che arriva allo scorrimento interno è finito: togliere il tetto
                     * rimette il crash.
                     */
                    Column(
                        modifier = Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState())
                    ) {
                        recents.forEach { entry ->
                            TextButton(
                                onClick = { go(entry.address.toUri()) },
                                enabled = !busy,
                                /*
                                 * Allineato a sinistra e col colore del testo: un `TextButton`
                                 * centra il contenuto e lo tinge di accento, mentre gli elenchi
                                 * da toccare si leggono come righe di testo a sinistra, e
                                 * l'accento come inchiostro di parole è sotto la soglia di
                                 * contrasto (vedi `accentInk` in `Theme.kt`). Resta un pulsante,
                                 * col suo ruolo e il suo bersaglio.
                                 */
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = entry.name.ifBlank { entry.address },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Quello che si è scritto, come indirizzo, con l'unica riparazione sempre sicura: senza schema si
 * mette `https://`, perché 'esempio.it/gatto.jpg' è quello che digitano tutti.
 */
private fun typedToUri(typed: String): Uri? {
    val text = typed.trim()
    if (text.isEmpty()) return null
    val hasScheme = Regex("^[a-z][a-z0-9+.-]*://", RegexOption.IGNORE_CASE).containsMatchIn(text)
    return (if (hasScheme) text else "https://$text").toUri()
}

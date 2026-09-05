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
import androidx.compose.ui.text.style.TextOverflow
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
    /**
     * Le immagini di una **pagina**, quando l'indirizzo non porta a un'immagine.
     *
     * ⚠️ Vedi `WebSeries.Rule.PAGE_LINKS`: è il sesto gradino dello sfogliatore Web, e
     * questo dialogo è la sua porta d'ingresso perché ce n'era già una.
     */
    onOpenPage: (Folder.Series) -> Unit,
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
     *
     * ⚠️⚠️ **E DALLA 1.42 UN INDIRIZZO CHE NON È UN'IMMAGINE NON È PIÙ UN VICOLO CIECO: si
     * prova la PAGINA** (istruzione dell'utente, 2026-09-03, sul sesto gradino dello
     * sfogliatore Web: *inizia a lavorarci*). Se là dentro ci sono immagini, si aprono come
     * serie, nell'ordine in cui la pagina le presenta. È esattamente il caso che il commento
     * qui sopra portava come esempio di errore, `esempio.it/pagina`.
     * ⚠️ **La pagina si prova SOLO dopo il no dell'immagine**, e l'ordine è la cosa che
     * tiene basso il costo: chi incolla l'indirizzo di una fotografia non paga niente in
     * più, perché quel ramo non ci arriva.
     * ⚠️ **L'errore adesso dice due cose invece di una** ([R.string.url_not_image]): a
     * questo punto si è provato l'indirizzo **e** la pagina, e un messaggio che nominasse
     * solo la prima racconterebbe metà di quello che è stato fatto.
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
                     * ⚠️⚠️ **IL TETTO DI UNA RIGA STA SUL SEGNAPOSTO, e senza di lui il
                     * dominio si stacca dal suo TLD** (riscontro dell'utente sul giro della
                     * `1.46`: *ha un url di esempio che va a capo con il TLD*). `singleLine`
                     * vincola il testo che si digita e non questo `Text`, che senza tetto di
                     * righe va a capo appena è più largo dello spazio: in un indirizzo il
                     * layout rompe dopo un punto o una barra, ed è così che il dominio si è
                     * trovato su una riga e il suo TLD su quella dopo.
                     * ⚠️ **L'ellissi sta in MEZZO e non in fondo**: l'esempio serve a dire
                     * che un indirizzo diretto finisce con l'estensione di un'immagine, e
                     * l'ellissi in fondo mangerebbe proprio quella coda. È la stessa ragione
                     * scritta su `nameWithExt`.
                     * ⚠️ **Un esempio più corto non sarebbe il rimedio**, e la `1.48` non
                     * toglie questo tetto pur avendo accorciato l'esempio: la larghezza che
                     * serve dipende dallo schermo e dal corpo del testo scelto nel telefono,
                     * quindi una stringa più corta sposta la soglia invece di toglierla.
                     * ⚠️⚠️ **E DALLA 1.48 L'ESEMPIO È UNO SOLO PER TUTTE LE LINGUE**
                     * (istruzione dell'utente, giro della `1.47`: *metti come testo di esempio
                     * `https://example.page/img.png`. Non occorre che sia multilingua*). Un
                     * indirizzo non è testo: `example.page` e `img.png` si leggono uguali in
                     * ventotto lingue, e tenerne ventotto copie voleva dire ventotto posti in
                     * cui cambiare un dominio. La stringa si dichiara `translatable="false"`,
                     * e il verificatore delle lingue da lì in poi controlla il **rovescio**,
                     * cioè che nessuna cartella se ne tenga una copia rimasta indietro.
                     */
                    placeholder = {
                        Text(
                            text = stringResource(R.string.url_hint),
                            maxLines = 1,
                            overflow = TextOverflow.MiddleEllipsis
                        )
                    },
                    singleLine = true,
                    // ⚠️ La stessa stondatura dei riquadri della rinomina, e per la stessa
                    // ragione: il riscontro le ha messe insieme (*oltre alla stondatura
                    // sbagliata, vedi 'Rinomina'*). Il numero vive in un posto solo.
                    shape = BOX_SHAPE,
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
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

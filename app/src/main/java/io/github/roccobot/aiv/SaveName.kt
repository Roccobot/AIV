package io.github.roccobot.aiv

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * La finestra che chiede **il nome** di quello che si sta salvando.
 *
 * ⚠️⚠️ **NASCE NELLA `1.77` CON LA TAPPA DEL SALVATAGGIO IN DOWNLOAD, e la sua specifica è
 * dell'utente**: *una finestra col solo nome, ispirata a 'Rinomina', precompilata col nome senza
 * estensione*. Le due vie che la aprono sono l'impostazione accesa e il **tocco lungo** su
 * 'Scarica', che vale per quella volta sola.
 *
 * ⚠️⚠️ **IL SOLO NOME, E IL SUFFISSO NON SI TOCCA**: quello che si batte qui è la parte davanti
 * al punto, e l'estensione si vede accanto al campo come un'etichetta spenta. Non è una
 * prudenza: senza il suffisso giusto la galleria non sa che cosa tiene in mano, ed è la stessa
 * ragione per cui `ImageActions.fileName` lo aggiunge quando manca. Chi vuole cambiare il
 * formato ha 'Esporta/Converti', che è un'altra cosa e lo dice.
 *
 * ⚠️⚠️ **È UNA MODALE VERA, E LE DUE RIGHE VANNO INSIEME** (`Modifier.lowered(null)` e
 * `properties = loweredWindow(null)`): esiste per raccogliere un input scritto, che è il solo
 * caso in cui l'app non si chiude toccando fuori. Il criterio e il difetto che l'ha fatto
 * scrivere due volte vivono in `AIV/CLAUDE.md`, § '👆 Che cosa fa il tocco FUORI da una
 * finestra'.
 *
 * @param full il nome intero che il file avrebbe, suffisso compreso.
 * @param onSave riceve il **solo nome**, senza suffisso: a rimetterlo ci pensa chi salva.
 */
@Composable
fun SaveNameDialog(
    full: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val (base, suffix) = remember(full) { ImageActions.splitName(full) }
    /*
     * ⚠️⚠️ **IL CURSORE PARTE IN FONDO, e non è il valore di serie** (è la stessa correzione
     * chiesta per 'Rinomina' nel giro della `1.59`): con la selezione a zero il cursore cade
     * davanti alla prima lettera, quindi chi vuole aggiungere qualcosa in coda deve prima
     * spostarsi. Qui il gesto tipico è appunto aggiungere, non riscrivere da zero.
     * ⚠️ **`rememberSaveable` e non `remember`**: girando il telefono con la finestra aperta si
     * perderebbe quello che si è battuto, ed è esattamente il lavoro che una modale protegge.
     * ⚠️⚠️ **E VUOLE `TextFieldValue.Saver`, O VA IN ERRORE APPENA SI APRE**: un `TextFieldValue`
     * in un `Bundle` non ci entra, e senza il suo salvatore `rememberSaveable` **non** avvisa
     * per iscritto, lancia. ⚠️ **Non è un difetto teorico**: la prima stesura non ce l'aveva, ha
     * compilato senza una parola e il banco di prova l'ha presa alla prima corsa
     * (`IllegalArgumentException: cannot be saved using the current SaveableStateRegistry`).
     */
    var typed by rememberSaveable(full, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(base, TextRange(base.length)))
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(full) { focus.requestFocus() }

    val pulito = typed.text.trim()
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(null),
        properties = loweredWindow(null),
        /*
         * ⚠️⚠️ **IL TITOLO È SUO E NON RIUSA `menu_save`, e il difetto l'ha trovato il banco**:
         * con il nome del comando in cima e 'Salva' sul tasto, in inglese la finestra dice
         * **Save** due volte, e con lei tedesco, francese e spagnolo, dove i due verbi
         * coincidono. In italiano non si vedeva ('Scarica' e 'Salva'), che è il modo tipico in
         * cui un difetto di testo passa: la lingua in cui si scrive è quella in cui non si
         * vede. Adesso il titolo dice **che cosa si chiede** e il tasto **che cosa fa**.
         */
        title = { Text(stringResource(R.string.save_name_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    singleLine = true,
                    /*
                     * ⚠️ **Il suffisso è un'etichetta in coda al campo e non un testo dentro**:
                     * dentro sarebbe cancellabile, e questa finestra promette il contrario.
                     * Spento di colore perché non è un comando: dice soltanto come finirà il
                     * nome.
                     */
                    suffix = if (suffix.isEmpty()) null else {
                        {
                            Text(
                                text = suffix,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (pulito.isNotBlank()) onSave(pulito) }
                    )
                )
                /*
                 * ⚠️⚠️ **IL CHIP DELLA DATA HA DUE GESTI, ED È LA SUA SPECIFICA**: *inserisce
                 * `YYYYMMDD` al cursore, e col tocco lungo sostituisce tutto il nome*. Il tocco
                 * breve aggiunge, il lungo rifà: sono le due cose che si vogliono davvero fare
                 * con una data in un nome di file, e nessuna delle due si ottiene dall'altra
                 * senza cancellare a mano.
                 * ⚠️ **Non è un `Button` di Material**, e non per gusto: nessuno dei suoi tasti
                 * accetta il tocco lungo, e un tasto che ne ha bisogno si compone da una
                 * `Surface` con `combinedClickable`. La forma resta quella di un chip tonale.
                 * ⚠️ **Un bersaglio solo**, come le righe con interruttore: dentro non c'è
                 * niente di toccabile, o un lettore di schermo annuncerebbe due voci per un
                 * comando solo.
                 */
                Surface(
                    onClick = { typed = withDate(typed) },
                    modifier = Modifier.align(Alignment.Start),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.save_name_date),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { typed = withDate(typed) },
                                onLongClick = {
                                    val oggi = today()
                                    typed = TextFieldValue(oggi, TextRange(oggi.length))
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(pulito) },
                enabled = pulito.isNotBlank()
            ) {
                Text(stringResource(R.string.editor_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Il testo con la data di oggi infilata dove sta il cursore.
 *
 * ⚠️ **Sostituisce la selezione, se c'è**: è quello che fa qualunque campo di testo quando si
 * scrive con del testo selezionato, e comportarsi diversamente sorprenderebbe.
 * ⚠️ **Il cursore resta DOPO la data**, che è il posto da cui si continua a scrivere.
 */
private fun withDate(value: TextFieldValue): TextFieldValue {
    val oggi = today()
    val da = value.selection.min
    val a = value.selection.max
    val testo = value.text.substring(0, da) + oggi + value.text.substring(a)
    return TextFieldValue(testo, TextRange(da + oggi.length))
}

/**
 * La data di oggi come `YYYYMMDD`, che è la forma che ha chiesto lui.
 *
 * ⚠️ **Senza separatori di proposito**: in un nome di file un punto aprirebbe un finto suffisso e
 * una barra non si può scrivere affatto. In più questa forma si ordina da sé in alfabetico, che è
 * la ragione per cui la usa la fotocamera di ogni telefono.
 * ⚠️ **`java.time` si può usare da qui**: l'app dichiara `minSdk 28` e quelle classi sono nel
 * sistema dalla 26.
 */
private fun today(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

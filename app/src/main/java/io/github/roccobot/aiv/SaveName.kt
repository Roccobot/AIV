package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
 * La finestra che chiede il nome di quello che si salva in Download, ispirata a 'Rinomina'. La
 * aprono l'impostazione accesa e il tocco lungo su 'Scarica', che vale per quella volta sola.
 *
 * ⚠️ **Il campo contiene il solo nome**: l'estensione si vede accanto come un'etichetta spenta,
 * perché senza il suffisso giusto la galleria non sa che file è (la stessa ragione per cui
 * `ImageActions.fileName` lo aggiunge quando manca). La cambia il comando 'Estensione', con la
 * stessa griglia di sicurezza di 'Rinomina'; il formato si cambia con 'Esporta/Converti'.
 *
 * Sotto il campo tre comandi solo-testo, disegnati da [Quiet] come in 'Rinomina' (richiesta
 * dell'utente). 'Destinazione' ed 'Estensione' sono sulla riga del titolo, in quest'ordine e
 * allineati a destra; se siano scritti o disegnati lo decide [TitleRow].
 *
 * ⚠️⚠️ **È una modale vera, e le due righe vanno insieme** (`Modifier.lowered(null)` e
 * `properties = loweredWindow(null)`): vedi `Rules.md`, § '👆 Che cosa fa il tocco FUORI da una
 * finestra'.
 *
 * @param full il nome intero che il file avrebbe, suffisso compreso.
 * @param hold se la finestra è stata aperta col tocco lungo: allora i due comandi della riga del
 *   titolo ci sono tutti e due, anche a impostazioni spente. Non ha un valore di serie, perché chi
 *   apre la finestra sa da quale via.
 * @param folder il nome della cartella in cui il file finirà, da mostrare sotto il campo, oppure
 *   `null` per quella di serie. Serve perché il percorso si ricorda: senza una riga che lo dice,
 *   una cartella scelta tempo fa sarebbe una destinazione invisibile.
 * @param onPickFolder il gesto di 'Destinazione': apre il selettore di cartella e non salva
 *   niente. `null` vuol dire che il comando non c'è: lo decide chi chiama, che ha le impostazioni.
 * @param onUseDownloads scorda la cartella scelta e torna a Download; `null` quando [folder] è già
 *   quella di serie.
 * @param onSave riceve il nome senza suffisso e il suffisso scelto, col punto: li riunisce chi
 *   salva, che sa che cosa dichiarare al `MediaStore`.
 */
@Composable
fun SaveNameDialog(
    full: String,
    hold: Boolean,
    folder: String?,
    onPickFolder: (() -> Unit)?,
    onUseDownloads: (() -> Unit)?,
    onDismiss: () -> Unit,
    onSave: (name: String, suffix: String) -> Unit
) {
    val (base, had) = remember(full) { ImageActions.splitName(full) }
    /*
     * Il cursore parte in fondo, come in 'Rinomina': qui il gesto tipico è aggiungere in coda.
     * `rememberSaveable`, perché girando il telefono si perderebbe quello che si è scritto.
     * ⚠️⚠️ **Vuole `TextFieldValue.Saver`, o va in errore appena si apre**: un `TextFieldValue`
     * non entra in un `Bundle`, e senza il suo salvatore `rememberSaveable` lancia
     * `IllegalArgumentException` (il compilatore non lo vede, il banco di prova sì).
     */
    var typed by rememberSaveable(full, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(base, TextRange(base.length)))
    }
    /*
     * ⚠️ **Il suffisso è uno stato**, perché quello che si vede accanto al campo e quello che
     * finisce nel nome salvato siano la stessa cosa anche dopo 'Estensione'.
     * Vuoto vuol dire nessuna estensione (un file che non ne aveva, o il pannellino svuotato):
     * allora l'etichetta accanto al campo sparisce.
     */
    var suffisso by rememberSaveable(full) { mutableStateOf(had) }
    val gate = extensionGate(
        where = ExtWhere.DOWNLOAD,
        /*
         * ⚠️ **Il tocco lungo scavalca tutte e due le opzioni** (riscontro dell'utente, voce
         * `save-quando`): rende disponibili sia 'Destinazione' sia 'Estensione', con la stessa
         * regola per i due comandi della stessa riga; le opzioni valgono per il tocco normale.
         */
        force = hold,
        // Il pannellino lavora senza il punto, come in 'Rinomina', e il punto lo rimette questa riga.
        initial = { suffisso.removePrefix(".") },
        onPick = { suffisso = if (it.isBlank()) "" else ".$it" }
    )
    val focus = remember { FocusRequester() }
    LaunchedEffect(full) { focus.requestFocus() }

    val pulito = typed.text.trim()
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(null),
        properties = loweredWindow(null),
        /*
         * ⚠️ **Il titolo è suo e non riusa `menu_save`**: col nome del comando in cima e 'Salva'
         * sul tasto, in inglese, tedesco, francese e spagnolo la finestra direbbe lo stesso verbo
         * due volte. Il titolo dice che cosa si chiede, il tasto che cosa fa.
         *
         * I due comandi della riga del titolo, nell'ordine scelto dall'utente: prima
         * 'Destinazione', poi 'Estensione'. A destra va la fila, quindi anche un comando solo è
         * allineato a destra. Qui si dice quali comandi ci sono e in che ordine; se siano scritti
         * o disegnati lo decide [TitleRow] dalla misura (risposta `d-pill-soglia`).
         */
        title = {
            val dest = stringResource(R.string.save_name_dest)
            val ext = stringResource(R.string.rename_ext)
            TitleRow(
                title = stringResource(R.string.save_name_title),
                commands = buildList {
                    if (onPickFolder != null) {
                        add(TitleCommand(dest, Glyphs.FolderDownload, onPickFolder))
                    }
                    if (gate.allowed) add(TitleCommand(ext, Glyphs.Extension, gate.open))
                }
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    singleLine = true,
                    /*
                     * Il suffisso è un'etichetta in coda al campo: dentro sarebbe cancellabile.
                     * Spento di colore perché non è un comando.
                     */
                    suffix = if (suffisso.isEmpty()) null else {
                        {
                            Text(
                                text = suffisso,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (pulito.isNotBlank()) onSave(pulito, suffisso) }
                    )
                )
                /*
                 * La fila e l'ordine sono quelli di 'Rinomina': prima i due comandi che agiscono
                 * sulla selezione, poi quello che scrive.
                 * ⚠️ **`FlowRow`**: tre comandi in una finestra larga 280dp non entrano in ogni
                 * lingua (in tedesco 'Alles auswählen' da solo è mezza riga), e andando a capo
                 * restano leggibili. `Arrangement.End` senza spaziatura: l'aria fra i comandi la
                 * mette il riempimento di [Quiet], e una spaziatura in più manderebbe a capo una
                 * fila che ci sta.
                 * 'Data' (specifica dell'utente): il tocco breve inserisce `YYYYMMDD` al cursore,
                 * il tocco lungo sostituisce tutto il nome.
                 */
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Quiet(
                        text = stringResource(R.string.rename_select_all),
                        enabled = typed.text.isNotEmpty(),
                        onTap = {
                            typed = typed.copy(selection = TextRange(0, typed.text.length))
                        }
                    )
                    Quiet(
                        text = stringResource(R.string.rename_clear),
                        enabled = typed.text.isNotEmpty(),
                        onTap = { typed = TextFieldValue() }
                    )
                    Quiet(
                        text = stringResource(R.string.save_name_date),
                        enabled = true,
                        onTap = { typed = withDate(typed) },
                        onHold = {
                            val oggi = today()
                            typed = TextFieldValue(oggi, TextRange(oggi.length))
                        }
                    )
                }
                /*
                 * La cartella si scrive perché si ricorda: chi salva deve sapere dove va il file.
                 * C'è solo quando è stata scelta; con quella di serie la riga non compare.
                 *
                 * ⚠️⚠️ **La riga ha la via del ritorno a Download, perché il selettore non la
                 * dà**: per la privacy rifiuta di mostrare quella cartella, quindi senza questo
                 * comando una cartella scelta sarebbe definitiva. Il ritorno scorda l'albero
                 * scelto e riporta a `MediaStore.Downloads`, senza passare dal selettore.
                 */
                if (folder != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.save_name_path, folder),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (onUseDownloads != null) {
                            Quiet(
                                text = stringResource(R.string.save_name_default),
                                enabled = true,
                                onTap = onUseDownloads
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(pulito, suffisso) },
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
 * Il testo con la data di oggi infilata dov'è il cursore.
 *
 * Sostituisce la selezione, se c'è, come qualunque campo di testo, e lascia il cursore dopo la
 * data. La usa anche 'Rinomina', perché 'Data' si comporti uguale nelle due finestre.
 */
internal fun withDate(value: TextFieldValue): TextFieldValue {
    val oggi = today()
    val da = value.selection.min
    val a = value.selection.max
    val testo = value.text.substring(0, da) + oggi + value.text.substring(a)
    return TextFieldValue(testo, TextRange(da + oggi.length))
}

/**
 * La data di oggi come `YYYYMMDD`, la forma chiesta dall'utente.
 *
 * Senza separatori: in un nome di file un punto aprirebbe un finto suffisso e una barra non si
 * può scrivere. Questa forma si ordina da sé in ordine alfabetico. `java.time` è nel sistema dalla
 * API 26, e l'app dichiara `minSdk 28`.
 */
internal fun today(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

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
 * La finestra che chiede **il nome** di quello che si sta salvando.
 *
 * ⚠️⚠️ **NASCE NELLA `1.77` CON LA TAPPA DEL SALVATAGGIO IN DOWNLOAD, e la sua specifica è
 * dell'utente**: *una finestra col solo nome, ispirata a 'Rinomina', precompilata col nome senza
 * estensione*. Le due vie che la aprono sono l'impostazione accesa e il **tocco lungo** su
 * 'Scarica', che vale per quella volta sola.
 *
 * ⚠️⚠️ **IL CAMPO PORTA IL SOLO NOME**: quello che si batte qui è la parte davanti al punto, e
 * l'estensione si vede accanto al campo come un'etichetta spenta. Non è una prudenza: senza il
 * suffisso giusto la galleria non sa che cosa tiene in mano, ed è la stessa ragione per cui
 * `ImageActions.fileName` lo aggiunge quando manca.
 * ⚠️⚠️ **MA DALLA `1.78` L'ESTENSIONE SI PUÒ CAMBIARE, DIETRO LA SUA GRIGLIA DI SICUREZZA**
 * (riscontro del giro della `1.77`, voce `scarica-download`: *deve esserci lo stesso pulsante
 * 'Estensione' (con identico funzionamento) della funzione 'Rinomina'*). Non contraddice il
 * blocco qui sopra: dal campo il suffisso resta fuori, e a cambiarlo è un comando a sé che
 * esiste solo se l'impostazione è accesa. Chi vuole cambiare il **formato** ha
 * 'Esporta/Converti', che è un'altra cosa e lo dice il pannellino stesso.
 *
 * ⚠️⚠️ **I COMANDI SOTTO IL CAMPO SONO TRE E SONO SOLO-TESTO, ED È LA SUA RICHIESTA ALLA
 * LETTERA**: *tutti con lo stile solo-testo, senza tasto/pillola già usato in 'Rinomina'*.
 * Quindi anche 'Data', che fino alla `1.77` era un gettone tonale, e il pezzo che li disegna è
 * [Quiet], lo stesso di 'Rinomina': copiarne la forma qui sarebbe il primo posto in cui
 * divergere.
 * ⚠️⚠️ **MA 'Estensione' NON È PIÙ FRA LORO, DALLA `1.80`: STA SULLA RIGA DEL TITOLO**
 * (riscontro del giro della `1.79`, voce `scarica-comandi`: *mi ero espresso male ... 'Estensione'
 * deve apparire sotto forma di icona a destra, allineato alla linea di base del titolo*). Con lei
 * c'è **'Destinazione'**, l'altro comando, e l'ordine è il suo: *prima 'Percorso' e poi
 * 'Estensione' ultima a destra*. Le due compaiono una per volta o insieme, e da sole stanno
 * comunque a destra, perché è la fila che si allinea al bordo e non ogni comando per conto suo.
 * ⚠️⚠️ **UNO SOLO È UNA PASTIGLIA COL TESTO, DUE SONO DUE ICONE, DALLA `1.82`** (riscontro del
 * giro della `1.81`, campo libero punto C: *chiaramente non possono coesistere due pulsanti
 * testuali in 'Scarica' ... in quel caso si usano le icone, prima 'Destinazione' e poi
 * 'Estensione', allineate a destra. Quando solo una delle due è attiva, si torna al pulsante
 * testuale*). La `1.81` le teneva testuali facendo cedere il titolo, cioè mandandolo a capo due
 * volte, e il ripiego delle icone era già autorizzato in quel giro.
 * ⚠️ **La forma la decide il CONTO dei comandi in scena, non le due condizioni**: scritte due
 * volte, il giorno che una cambia si otterrebbe una pastiglia accanto a un'icona.
 *
 * ⚠️⚠️ **È UNA MODALE VERA, E LE DUE RIGHE VANNO INSIEME** (`Modifier.lowered(null)` e
 * `properties = loweredWindow(null)`): esiste per raccogliere un input scritto, che è il solo
 * caso in cui l'app non si chiude toccando fuori. Il criterio e il difetto che l'ha fatto
 * scrivere due volte vivono in `AIV/CLAUDE.md`, § '👆 Che cosa fa il tocco FUORI da una
 * finestra'.
 *
 * @param full il nome intero che il file avrebbe, suffisso compreso.
 * @param hold se la finestra è stata aperta col **tocco lungo**, che vale per quella volta sola:
 *   allora i due comandi della riga del titolo ci sono tutti e due, a impostazioni spente
 *   (campo libero del giro della `1.79`, punto D). ⚠️ **Non ha un valore di serie**, perché è
 *   una delle due vie con cui questa finestra si apre e chi la apre lo sa.
 * @param folder il nome della cartella in cui il file finirà, da mostrare sotto il campo, oppure
 *   `null` per quella di serie. ⚠️ **Serve perché il percorso adesso si RICORDA**: senza una riga
 *   che lo dice, una cartella scelta un mese fa sarebbe una destinazione invisibile.
 * @param onPickFolder il gesto di 'Destinazione': apre il selettore di **cartella** e non salva
 *   niente. ⚠️ **`null` vuol dire che il comando non c'è**, cioè l'impostazione è spenta oppure la
 *   finestra non è stata aperta col tocco lungo: la scelta la fa chi chiama, perché è lui ad
 *   avere le impostazioni in mano.
 * @param onUseDownloads scorda la cartella scelta e torna a Download. ⚠️ **`null` quando non c'è
 *   niente da scordare**, cioè quando [folder] è già quella di serie.
 * @param onSave riceve il nome senza suffisso **e** il suffisso scelto, col punto: a rimetterli
 *   insieme ci pensa chi salva, perché è lui a sapere che cosa dichiarare al `MediaStore`.
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
    /*
     * ⚠️⚠️ **IL SUFFISSO È UNO STATO DALLA `1.78`, e prima era una costante**: adesso il comando
     * 'Estensione' lo cambia, quindi quello che si vede accanto al campo e quello che finisce nel
     * nome salvato devono essere **la stessa cosa**. Tenerne uno fisso e passare l'altro avrebbe
     * dato una finestra che promette un nome e ne scrive un altro.
     * ⚠️ **Vuoto vuol dire 'nessuna estensione'**, che è il caso di un file che non ne aveva e di
     * chi svuota il pannellino: là l'etichetta accanto al campo sparisce, invece di mostrare un
     * punto solo.
     */
    var suffisso by rememberSaveable(full) { mutableStateOf(had) }
    val gate = extensionGate(
        where = ExtWhere.DOWNLOAD,
        /*
         * ⚠️⚠️ **IL TOCCO LUNGO SCAVALCA TUTTE E DUE LE OPZIONI, DALLA `1.83`, E ADESSO LA
         * REGOLA È UNA SOLA** (riscontro del giro della `1.82`, voce `save-quando` non approvata:
         * *qualunque sia lo stato di 'Consenti la rinomina al salvataggio', la pressione lunga su
         * 'Scarica' rende sempre disponibili sia 'Destinazione' che 'Estensione'*). Il tocco
         * lungo è la **versione con tutto**, e le due opzioni valgono per il tocco normale.
         * ⚠️⚠️ **LE DUE CONDIZIONI NON SONO PIÙ DIVERSE, ed erano il difetto**: la `1.82` faceva
         * dipendere 'Destinazione' dal gesto **e** dall'opzione, ed 'Estensione' dalla sola
         * opzione, cioè due regole per due comandi che vivono sulla stessa riga. Chi apriva la
         * finestra col tocco lungo ne trovava uno solo, senza poter sapere perché.
         */
        force = hold,
        // ⚠️ Il pannellino lavora **senza** il punto, come in 'Rinomina', e il punto lo rimette
        // questa riga: è la stessa convenzione, quindi le due finestre si comportano uguale.
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
         * ⚠️⚠️ **IL TITOLO È SUO E NON RIUSA `menu_save`, e il difetto l'ha trovato il banco**:
         * con il nome del comando in cima e 'Salva' sul tasto, in inglese la finestra dice
         * **Save** due volte, e con lei tedesco, francese e spagnolo, dove i due verbi
         * coincidono. In italiano non si vedeva ('Scarica' e 'Salva'), che è il modo tipico in
         * cui un difetto di testo passa: la lingua in cui si scrive è quella in cui non si
         * vede. Adesso il titolo dice **che cosa si chiede** e il tasto **che cosa fa**.
         */
        /*
         * ⚠️⚠️ **I DUE COMANDI STANNO SULLA RIGA DEL TITOLO, E L'ORDINE È IL SUO** (riscontro
         * del giro della `1.79`, voce `scarica-comandi`: *le icone potrebbero essere presenti
         * una per volta (in tal caso l'unica va allineata a destra) o insieme (in tal caso,
         * prima 'Percorso' e poi 'Estensione' ultima a destra)*).
         * ⚠️ **A destra ci va la FILA e non ogni icona per conto suo**, e così il caso 'una
         * sola' viene da sé: `SpaceBetween` spinge la fila al bordo, quindi l'unica icona in
         * scena è già allineata a destra senza che nessuno la sposti.
         * ⚠️⚠️ **QUI SI DICE QUALI COMANDI CI SONO E IN CHE ORDINE, E BASTA**: se siano scritti o
         * disegnati lo decide [TitleRow] dalla misura, che dalla `1.86` è la sua risposta a
         * `d-pill-soglia`. Il conto che la `1.82` faceva qui vive là, dove c'è anche la
         * larghezza.
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
                     * ⚠️ **Il suffisso è un'etichetta in coda al campo e non un testo dentro**:
                     * dentro sarebbe cancellabile, e questa finestra promette il contrario.
                     * Spento di colore perché non è un comando: dice soltanto come finirà il
                     * nome.
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
                 * ⚠️⚠️ **LA FILA È LA STESSA DI 'Rinomina', E L'ORDINE ANCHE**: prima i due che
                 * agiscono sulla selezione, poi quello che scrive. Le due finestre portano gli
                 * stessi comandi, quindi chi impara una posizione la ritrova nell'altra.
                 * ⚠️⚠️ **E DALLA `1.80` SONO TRE E NON QUATTRO, perché 'Estensione' è salita
                 * sulla riga del titolo** (vedi la nota là sopra): quella non lavorava sul campo
                 * come le altre, apriva un'altra finestra, ed è la ragione per cui in questa
                 * fila stava in testa invece che in coda.
                 * ⚠️ **`FlowRow` e non `Row`**: tre comandi in una finestra larga 280dp non ci
                 * stanno in ogni lingua, e in tedesco 'Alles auswählen' da solo è mezza riga.
                 * Andando a capo restano leggibili invece di stringersi.
                 * ⚠️ **`Arrangement.End` senza spaziatura**: l'aria fra i comandi la mette il
                 * riempimento di [Quiet], e sommarci una spaziatura li allontanerebbe di tre
                 * volte tanto, mandando a capo una fila che ci sta.
                 * ⚠️⚠️ **IL TOCCO LUNGO DELLA DATA È LA SUA SPECIFICA**: *inserisce `YYYYMMDD`
                 * al cursore, e col tocco lungo sostituisce tutto il nome*. Il tocco breve
                 * aggiunge, il lungo rifà: sono le due cose che si vogliono davvero fare con una
                 * data in un nome di file, e nessuna delle due si ottiene dall'altra senza
                 * cancellare a mano.
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
                 * ⚠️⚠️ **LA CARTELLA SI SCRIVE PERCHÉ ADESSO SI RICORDA, DALLA `1.81`**
                 * (riscontro del giro della `1.80`, campo libero punto E: *Io voglio che sia
                 * memorizzato il percorso in modo che il file sia salvato lì alla fine, ma solo
                 * alla pressione di 'Salva'*). Una destinazione scelta una volta e poi
                 * invisibile è peggio di nessuna destinazione: chi salva non sa dove sta
                 * mandando il file.
                 * ⚠️ **C'è solo quando è stata scelta**: con la cartella di serie la riga non
                 * comparirebbe a dire 'Download', che è quello che l'app fa da sempre e che
                 * nessuno ha bisogno di leggere ogni volta.
                 */
                /*
                 * ⚠️⚠️ **E DALLA `1.82` PORTA LA VIA DEL RITORNO, PERCHÉ IL SELETTORE NON LA
                 * DÀ** (riscontro del giro della `1.81`, voce `save-percorso`: *se cambio
                 * cartella di download, non posso più tornare a storage/emulated/0/Download. Il
                 * file picker mi dice che 'per tutelare la mia privacy' non posso scegliere
                 * quella cartella*). Non è una comodità: senza questa riga una cartella scelta
                 * una volta era **definitiva**, perché la sola via per cambiarla passava da un
                 * selettore che quella cartella si rifiuta di mostrare.
                 * ⚠️ **Il ritorno non passa dal selettore ed è la ragione per cui funziona**:
                 * scordare l'albero scelto riporta alla strada di serie, che è
                 * `MediaStore.Downloads` e non un percorso da scegliere.
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
 * Il testo con la data di oggi infilata dove sta il cursore.
 *
 * ⚠️ **Sostituisce la selezione, se c'è**: è quello che fa qualunque campo di testo quando si
 * scrive con del testo selezionato, e comportarsi diversamente sorprenderebbe.
 * ⚠️ **Il cursore resta DOPO la data**, che è il posto da cui si continua a scrivere.
 * ⚠️⚠️ **NON È PIÙ PRIVATA DALLA `1.78`, e la ragione è la sua richiesta**: *voglio che
 * 'Rinomina' abbia 'Data', che inserisce YYYYMMDD **esattamente come implementato** in
 * 'Scarica'*. 'Esattamente come' regge solo se il gesto lo fa la stessa funzione; due copie del
 * conto sul cursore sarebbero due comportamenti che si somigliano.
 */
internal fun withDate(value: TextFieldValue): TextFieldValue {
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
internal fun today(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

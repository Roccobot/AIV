package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.times
import kotlinx.coroutines.launch

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
    onRename: (template: String, start: Int, extension: String?) -> Unit
) {
    val context = LocalContext.current
    val names by produceState<List<String>?>(null, uris) {
        value = FileTree.namesOf(context, uris)
    }

    var template by rememberSaveable { mutableStateOf("") }
    var start by rememberSaveable { mutableStateOf("1") }
    var proposed by rememberSaveable { mutableStateOf(false) }

    /*
     * ⚠️ **`null` vuol dire 'ognuno tiene la sua', dalla 1.30** (richiesta dell'utente,
     * 2026-09-02), e non è la stessa cosa di una stringa vuota, che vorrebbe dire 'nessuna
     * estensione'. Il pannellino la propone già riempita con quella corrente, quindi finché
     * non lo si apre questa resta `null` e i file conservano ognuno la propria: importa con
     * una selezione mista, dove mettere l'estensione del primo a tutti sarebbe un danno.
     */
    var extension by rememberSaveable { mutableStateOf<String?>(null) }

    /*
     * ⚠️⚠️ **IL CANCELLO DELL'ESTENSIONE È UNO, E DALLA `1.78` LO CONDIVIDE CON LA FINESTRA DEL
     * SALVATAGGIO**: là serve *lo stesso pulsante 'Estensione' (con identico funzionamento)*
     * (riscontro del giro della `1.77`, voce `scarica-download`), e 'identico' regge solo se il
     * pezzo è lo stesso. Che cosa fa, e perché si porta dietro le proprie finestre, sta su
     * [extensionGate].
     */
    val gate = extensionGate(
        where = ExtWhere.RENAME,
        // ⚠️ Qui la griglia di sicurezza non si scavalca mai: il tocco lungo che la scavalca
        // per una volta è quello su 'Scarica', e questa finestra si apre da una voce di menu.
        force = false,
        // ⚠️ Il valore di partenza è quello **corrente**: l'estensione già scelta se c'è,
        // altrimenti quella del primo file, che con una selezione omogenea è quella di
        // tutti. Senza il punto, come chiesto.
        initial = { extension ?: names?.firstOrNull()?.substringAfterLast('.', "").orEmpty() },
        onPick = { extension = it }
    )

    // ⚠️⚠️ **UN FILE SOLO NON È UNA RINOMINA IN BLOCCO, e dalla 1.25 non ne ha più l'aria**
    // (riscontro dell'utente, 2026-09-02: *`Rinomina` sul file singolo deve partire dal nome
    // originale, non da un template di rinomina batch*). Con un file la schermata proponeva
    // `Museo ##`, chiedeva da che numero partire e spiegava i cancelletti: tre cose che
    // servono a numerare ottanta foto e nessuna che serva a cambiare un nome.
    val singolo = uris.size == 1

    val listed = names
    LaunchedEffect(listed) {
        if (proposed || listed == null) return@LaunchedEffect
        // ⚠️ Si segna proposto anche con l'elenco VUOTO, e la riga sotto dipende da questo:
        // è quella che apre la finestra, e senza di lei un tocco su 'Rinomina' non aprirebbe
        // più niente nel caso in cui i nomi non si riescono a leggere.
        proposed = true
        if (listed.isEmpty()) return@LaunchedEffect
        // ⚠️ Il nome **senza estensione**, perché l'estensione la rimette `renderName`: con
        // lei dentro il template il file diventerebbe `foto.jpg.jpg`.
        template = if (singolo) listed.first().substringBeforeLast('.', listed.first())
        else suggestTemplate(listed.first(), listed.size, start.toIntOrNull() ?: 1)
    }

    /*
     * ⚠️⚠️ **NON SI APRE FINCHÉ NON HA I NOMI E IL TEMPLATE, ed è il rimedio che la `1.25` ha
     * già scelto per la scheda delle informazioni** (vedi `FileOps`): `FileTree.namesOf` passa
     * da `Dispatchers.IO` senza condizioni, quindi al primo fotogramma i nomi non ci sono
     * **per costruzione**, e questa finestra si disegnava vuota e poi cresceva di tutto il
     * blocco dell'anteprima. È il salto che l'utente vede ancora dopo la `1.45`.
     * ⚠️⚠️ **LA GUARDIA È `proposed` E NON `listed != null`, e la differenza è la rotazione**:
     * `proposed` sopravvive alla ricreazione dell'attività e `names` no, perché viene da un
     * `produceState` che riparte da capo. Guardando i nomi, girare il telefono con la finestra
     * aperta la farebbe sparire e tornare.
     * ⚠️ **Il prezzo è un'attesa fra il tocco e la finestra**, ed è dichiarato: con un file
     * solo è un salto di thread, con una selezione grande è una lettura per file. Il baratto è
     * lo stesso che la `1.25` ha già accettato.
     */
    if (!proposed) return

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
        modifier = Modifier.lowered(null),
        properties = loweredWindow(null),
        /*
         * ⚠️⚠️ **'Estensione' STA SULLA RIGA DEL TITOLO, dalla 1.34, e nella 1.30 stava
         * nella fila dei tasti**: era una mia lettura sbagliata della richiesta, e l'utente
         * l'ha chiarita con un mockup (voce `ren-ext`: *'Estensione' in alto a destra,
         * allineato con gli altri elementi ma sulla stessa riga del titolo; intendevo QUEL
         * 'Rinomina'*). La sua frase della `1.30` diceva 'a destra di Rinomina', e i
         * 'Rinomina' in questo dialogo sono **due**: il titolo e il tasto di conferma. Ho
         * scelto quello sbagliato.
         * ⚠️ **E così la fila dei tasti torna quella di Material**: 'Annulla' e 'Rinomina',
         * la conferma in fondo a destra. La nota che dichiarava l'ordine strano è decaduta
         * insieme al tasto che la rendeva necessaria.
         * ⚠️⚠️ **È STATA UN'ICONA NELLA SOLA `1.80`, E DALLA `1.81` È DI NUOVO LA PASTIGLIA COL
         * TESTO** (riscontro del giro della `1.80`, voce `rinomina-icona`: *Ho cambiato idea: in
         * 'Rinomina', il tasto 'Estensione' deve tornare come prima (testuale, tasto stondato a
         * destra, linea di base del titolo della finestra)*). Il pezzo che la disegna è
         * [TitlePill], lo stesso della finestra del salvataggio: le due finestre portano gli
         * stessi comandi, quindi la forma è una.
         * ⚠️ **Il titolo tiene il peso**: con una pastiglia accanto, senza il peso un titolo
         * lungo spingerebbe il comando oltre il bordo invece di andare a capo lui.
         */
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.pick_rename),
                    modifier = Modifier.weight(1f)
                )
                if (gate.allowed) {
                    TitlePill(
                        text = stringResource(R.string.rename_ext),
                        onTap = gate.open
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                /*
                 * ⚠️⚠️ **IL CAMPO PASSA DA UN `TextFieldValue` DALLA `1.60`, e serve solo a
                 * portare la POSIZIONE DEL CURSORE** (riscontro del giro della `1.59`: *al tocco
                 * sul campo di testo, il cursore deve posizionarsi in fondo: un eventuale nome
                 * lungo deve essere già visualizzato nella sua ultima parte e il cursore deve
                 * lampeggiare alla fine*). Con la versione a `String` la posizione del cursore
                 * non esiste come dato: la tiene il campo per conto suo, e nessuno da fuori la
                 * può mettere in fondo.
                 * ⚠️ **La verità resta [template]**, e questo lo rispecchia: è quello che
                 * l'anteprima e il tasto leggono, ed è quello che sopravvive alla rotazione
                 * (`rememberSaveable`). Il valore col cursore no, ed è giusto: dopo una
                 * rotazione l'effetto qui sotto lo rifà con la coda in vista.
                 * ⚠️ **Una casella lunga si porta il cursore dietro**: un campo a riga sola
                 * scorre fin dove sta il cursore, quindi mettendolo in fondo la coda del nome è
                 * quella che si vede. È la seconda metà della richiesta, e viene da sé.
                 */
                var campo by remember { mutableStateOf(TextFieldValue()) }
                LaunchedEffect(template) {
                    if (campo.text != template) {
                        campo = TextFieldValue(template, TextRange(template.length))
                    }
                }
                /*
                 * ⚠️⚠️ **IL FUOCO SI CHIEDE ALL'APERTURA, DALLA `1.62`, E LA `1.60` CI PROVAVA
                 * DAL POSTO SBAGLIATO** (riscontro del giro della `1.60`: *il campo di testo non
                 * scorre e il cursore non si posiziona automaticamente in fondo*). La `1.60`
                 * metteva il cursore in fondo quando il **fuoco arrivava**, e quel momento è
                 * esattamente quello in cui il tocco che lo porta sta anche decidendo dove
                 * mettere il cursore: il gesto arriva dopo e vince, quindi il cursore finiva
                 * dove cadeva il dito.
                 * ⚠️⚠️ **E SENZA FUOCO UN CAMPO A RIGA SOLA NON SCORRE AFFATTO**: mostra la testa
                 * del testo, qualunque sia la selezione. È la seconda metà del riscontro, e non
                 * si poteva togliere spostando il cursore: si toglie dando il fuoco.
                 * ⚠️ **Così il gesto torna a fare il suo mestiere**: chi apre trova la coda del
                 * nome e il cursore in fondo, e chi tocca in mezzo al testo sposta il cursore
                 * dove ha toccato, che è quello che un campo di testo deve fare.
                 * ⚠️ **Una volta sola per apertura** (`LaunchedEffect(Unit)`), o a ogni
                 * ricomposizione il fuoco tornerebbe qui strappandolo a chi lo avesse preso.
                 */
                val fuoco = remember { FocusRequester() }
                LaunchedEffect(Unit) { fuoco.requestFocus() }
                OutlinedTextField(
                    value = campo,
                    onValueChange = { scritto ->
                        campo = scritto
                        template = scritto.text
                    },
                    label = { Text(stringResource(R.string.rename_template)) },
                    singleLine = true,
                    // ⚠️ Con un file solo il tasto della tastiera dice 'fine' e non 'avanti':
                    // sotto non c'è più nessuna casella dove andare.
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (singolo) ImeAction.Done else ImeAction.Next
                    ),
                    shape = BOX_SHAPE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(fuoco)
                )
                /*
                 * ⚠️⚠️ **I DUE COMANDI SUL NOME, dalla `1.60`** (richiesta dell'utente, giro
                 * della `1.59`: *sarebbero utili due tasti 'Seleziona tutto' e 'Svuota' che
                 * agiscono sul campo nome, ma non so come inserirli. Dovrebbero essere molto
                 * sobri e poco invadenti*).
                 * ⚠️⚠️ **LA FORMA È QUELLA CHE HA GIÀ APPROVATO**, cioè il 'Ripristina' dei
                 * riquadri del riordino: a destra, in un corpo più piccolo, e sbiadito quando
                 * non c'è niente da fare. Inventarne una seconda per la stessa specie di comando
                 * vorrebbe dire due modi di dire 'questo è secondario'.
                 * ⚠️ **Sbiaditi e non spariti**, per la stessa ragione scritta là: un comando
                 * che compare e scompare si cerca proprio nel momento in cui non c'è.
                 * ⚠️ **Non toccano il fuoco**: chi li usa ha la tastiera aperta, e una selezione
                 * o uno svuotamento che la chiudessero costringerebbero a un tocco in più per
                 * riaprirla.
                 */
                /*
                 * ⚠️⚠️ **'Data' È ARRIVATA CON LA `1.78`, E LA SUA RAGIONE È LA SIMMETRIA**
                 * (campo libero del giro della `1.77`: *così come voglio che in 'Scarica' ci
                 * siano 'Seleziona tutto' e 'Svuota', voglio che 'Rinomina' abbia 'Data', che
                 * inserisce YYYYMMDD esattamente come implementato in 'Scarica'*). I due gesti
                 * sono gli stessi di là, e li fanno le stesse due funzioni: il tocco breve
                 * infila la data dove sta il cursore, il lungo rifà il nome da capo.
                 * ⚠️ **Anche il verso della fila è quello di 'Scarica'**, e non è un caso: le
                 * due finestre portano gli stessi comandi, quindi chi impara una posizione la
                 * ritrova nell'altra.
                 * ⚠️⚠️ **QUI I COMANDI TOCCANO DUE COSE, E DIMENTICARE LA SECONDA NON DÀ
                 * ERRORE**: la verità è [template], e [campo] la rispecchia solo per portare la
                 * posizione del cursore. Un comando che scrivesse il solo [campo] cambierebbe
                 * quello che si legge senza cambiare quello che l'anteprima e il tasto leggono.
                 * ⚠️ **`Arrangement.End` senza spaziatura**: l'aria fra i comandi la mette il
                 * riempimento di [Quiet], e sommarci una spaziatura li allontanerebbe di tre
                 * volte tanto, mandando a capo una fila che ci sta.
                 */
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Quiet(
                        text = stringResource(R.string.rename_select_all),
                        enabled = campo.text.isNotEmpty(),
                        onTap = {
                            campo = campo.copy(selection = TextRange(0, campo.text.length))
                        }
                    )
                    Quiet(
                        text = stringResource(R.string.rename_clear),
                        enabled = campo.text.isNotEmpty(),
                        onTap = {
                            campo = TextFieldValue()
                            template = ""
                        }
                    )
                    /*
                     * ⚠️⚠️ **COL TOCCO LUNGO E PIÙ FILE LA DATA PORTA I CANCELLETTI, DALLA
                     * `1.80`, E IL CONTO È IL SUO** (risposta alla domanda `d-data-blocco` del
                     * giro della `1.79`: *con più file, scrivi AAAAMMDD più uno spazio seguito
                     * da un numero di cancelletti adeguato alla dimensione del set. Ad esempio,
                     * se sono 100 file, aggiunge 3 cancelletti; con 50 file aggiungi 2
                     * cancelletti. È la stessa logica di creazione del primo template*).
                     * ⚠️ **'La stessa logica' è la stessa FUNZIONE**, [hashesFor], che è quella
                     * da cui esce il template proposto all'apertura: due conti che si somigliano
                     * darebbero due numeri diversi il giorno che uno dei due cambia.
                     * ⚠️⚠️ **E COSÌ 'Rinomina' NON RESTA PIÙ SPENTO**: fino alla `1.79` il tocco
                     * lungo lasciava un template senza cancelletti, cioè un nome uguale per
                     * tutti, e il tasto di conferma restava spento finché non se ne aggiungeva
                     * uno a mano. Era il prezzo di 'esattamente come in Scarica', dove il file è
                     * uno solo, e la sua risposta lo toglie.
                     * ⚠️ **Con un file solo resta la sola data**: là non c'è niente da numerare,
                     * ed è la stessa ragione per cui questa finestra con un file solo non chiede
                     * il primo numero.
                     * ⚠️⚠️ **IL CONTO È SU [uris] E NON SUI NOMI LETTI**, che è la differenza fra
                     * dire il vero e dire due: se i nomi non si riescono a leggere, `listed` è
                     * una lista vuota e da lì uscirebbero due cancelletti per qualunque
                     * selezione, senza nessun errore. Quanti file sono lo si sa comunque, ed è
                     * il numero che [hashesFor] chiede.
                     */
                    Quiet(
                        text = stringResource(R.string.save_name_date),
                        enabled = true,
                        onTap = {
                            val next = withDate(campo)
                            campo = next
                            template = next.text
                        },
                        onHold = {
                            val oggi = today()
                            val nuovo = if (singolo) oggi else {
                                val cifre = hashesFor(uris.size, first ?: 1)
                                "$oggi ${"#".repeat(cifre)}"
                            }
                            campo = TextFieldValue(nuovo, TextRange(nuovo.length))
                            template = nuovo
                        }
                    )
                }
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
                        shape = BOX_SHAPE,
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
                    for (row in previewOf(listed, clean, first, extension)) {
                        PreviewRow(row)
                    }
                }
            }
        },
        /*
         * ⚠️ **La fila dei tasti è quella di Material**, 'Annulla' e 'Rinomina', dalla `1.34`:
         * il comando dell'estensione sta sulla riga del titolo (vedi la nota là sopra), e la
         * nota che dichiarava un ordine strano è decaduta insieme al tasto che la rendeva
         * necessaria.
         */
        confirmButton = {
            TextButton(
                onClick = { onRename(clean, first ?: 1, extension) },
                enabled = ready
            ) { Text(stringResource(R.string.pick_rename)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Che cosa un chiamante deve sapere del comando 'Estensione'.
 *
 * @property allowed se il comando esiste, cioè se l'impostazione è accesa.
 * @property open il gesto: apre l'avviso la prima volta, il pannellino dopo.
 */
internal class ExtensionGate(val allowed: Boolean, val open: () -> Unit)

/**
 * Quale delle due finestre sta chiedendo il comando 'Estensione'.
 *
 * ⚠️⚠️ **NASCE NELLA `1.81` PERCHÉ I CHIP SONO DUE** (riscontro del giro della `1.80`, campo
 * libero punto A): fino alla `1.80` le due finestre leggevano lo stesso interruttore, quindi
 * accendere l'estensione in 'Rinomina' la accendeva anche in 'Scarica'. Adesso ognuna legge il
 * proprio chip, e a dire quale si legge è questo parametro.
 * ⚠️ **Non ha un valore di serie**, come `force`: chi apre una finestra con questo comando
 * dichiara di quale delle due si tratta, e non lo può fare per omissione.
 */
internal enum class ExtWhere { RENAME, DOWNLOAD }

/**
 * Il comando 'Estensione' con tutto quello che gli serve: la griglia di sicurezza, l'avviso
 * della prima volta e il pannellino.
 *
 * ⚠️⚠️ **IL TASTO C'È SOLO SE L'IMPOSTAZIONE È ACCESA, dalla 1.36**, ed è la griglia di
 * sicurezza chiesta dall'utente: il perché per esteso sta su [Settings.extRename], e in breve è
 * che cambiare l'estensione non converte niente e può far sparire un'immagine dalle viste. Di
 * fabbrica è spenta.
 * ⚠️⚠️ **E DALLA `1.81` GLI INTERRUTTORI SONO DUE, uno per finestra**: [Settings.extRename] e
 * [Settings.extDownload], che nel pannello sono i due chip di una voce sola. A dire quale si
 * legge è [where].
 *
 * ⚠️⚠️ **LE DUE LETTURE SI FANNO QUI E NON ARRIVANO DA FUORI, ed è una scelta contro la
 * convenzione dei dialoghi di questo file** (`FileJobDialogs` dichiara di non sapere niente
 * delle impostazioni e si fa passare i campi delle info). La ragione è il numero di posti: le
 * tre schermate che aprono la rinomina sono griglia, albero e visualizzatore, e solo l'ultima
 * ha le impostazioni in mano; le altre due dovrebbero farsi passare un booleano dai **loro**
 * chiamanti, cioè quattro firme in più per un valore che si legge in una riga. La stessa strada
 * la fanno già i veli (`Hint.flow`), che nascono in schermate che non hanno lo stato dell'app.
 * ⚠️ **`false` come valore iniziale**: mentre la lettura è in corso il tasto non c'è, che è il
 * verso prudente. Al contrario comparirebbe per un istante anche a chi l'ha spento.
 *
 * ⚠️⚠️ **DALLA `1.80` LA GRIGLIA SI PUÒ SCAVALCARE PER UNA VOLTA, ED È LA SUA RICHIESTA**
 * (riscontro del giro della `1.79`, campo libero punto D: *la pressione lunga su 'Scarica'
 * metterà a disposizione la finestra di download con entrambe le icone-tasto attive
 * ('Percorso', 'Estensione')*). Il tocco lungo è già il gesto che accende la rinomina al volo,
 * quindi accende anche questo comando: è lo stesso 'per questa volta sola'.
 * ⚠️ **Quello che il tocco lungo NON scavalca è l'avviso**: la prima volta la finestrella con
 * il rischio compare comunque, perché è quella che protegge, non l'interruttore.
 *
 * ⚠️⚠️ **SI PORTA DIETRO LE PROPRIE FINESTRE, E QUELLO È IL PUNTO**: chi lo chiama ottiene un
 * tasto che funziona, non due righe da ricordare in fondo alla funzione. È lo stesso criterio
 * per cui `lowered()` porta il velo (`AIV/CLAUDE.md`, § '📍 Che cosa vuol dire 'centrato''): un
 * avviso o un pannellino dimenticati non danno nessun errore, danno un tasto che non fa niente.
 * ⚠️ **L'ordine nella composizione non decide chi sta sopra**: le due finestre nascono quando si
 * tocca il comando, cioè quando il dialogo che le apre è già in scena, quindi arrivano dopo di
 * lui nel gestore delle finestre qualunque sia il posto di questa chiamata.
 *
 * @param where quale delle due finestre lo sta chiedendo, cioè quale dei due chip si legge.
 * @param force se il comando c'è **comunque**, cioè anche a impostazione spenta.
 *   ⚠️ **Non ha un valore di serie di proposito**: chi apre una finestra con questo comando
 *   dichiara se sta scavalcando la griglia di sicurezza, e non lo può fare per omissione. È lo
 *   stesso criterio del parametro di `Modifier.lowered`.
 * @param initial l'estensione da cui parte il pannellino, **senza** il punto. È una funzione e
 *   non un valore perché si legge nell'istante in cui il pannellino si apre.
 * @param onPick riceve l'estensione scelta, senza punto; vuota vuol dire 'nessuna'.
 */
@Composable
internal fun extensionGate(
    where: ExtWhere,
    force: Boolean,
    initial: () -> String,
    onPick: (String) -> Unit
): ExtensionGate {
    val context = LocalContext.current
    val allowed by produceState(false, where) {
        SettingsStore.flow(context).collect {
            value = when (where) {
                ExtWhere.RENAME -> it.extRename
                ExtWhere.DOWNLOAD -> it.extDownload
            }
        }
    }
    val warned by produceState(true) { Hint.EXT_WARN.flow(context).collect { value = it } }
    var warning by rememberSaveable { mutableStateOf(false) }
    var asking by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    /*
     * ⚠️⚠️ **È UN VELO IN UNA FINESTRA SUA, e la ragione sta su [HintNotice]**: chi chiama è già
     * dentro un dialogo, e un velo steso sul contenuto coprirebbe la finestrella invece dello
     * schermo. L'utente ha chiesto un avviso *in mezzo allo schermo*.
     * ⚠️ **Si archivia alla chiusura e si apre il pannellino nello stesso gesto**: l'avviso si
     * legge una volta sola, e chi ha toccato 'Estensione' voleva aprirlo.
     */
    if (warning) {
        HintNotice(
            text = stringResource(R.string.hint_ext_warn),
            onDone = {
                warning = false
                asking = true
                scope.launch { Hint.EXT_WARN.remember(context) }
            }
        )
    }
    if (asking) {
        ExtensionDialog(
            initial = initial(),
            onDismiss = { asking = false },
            onPick = { asking = false; onPick(it) }
        )
    }
    /*
     * ⚠️ **Il velo PRIMA del pannellino e non insieme**: l'avviso dice che cosa comporta la cosa
     * che si sta per fare, e uno che comparisse sopra il campo già aperto arriverebbe dopo il
     * gesto. Chi lo chiude trova il pannellino, quindi il tocco non va perso.
     */
    return ExtensionGate(force || allowed) { if (warned) asking = true else warning = true }
}

/**
 * Il pannellino della sola estensione.
 *
 * ⚠️⚠️ **CAMBIA IL NOME E NON IL FORMATO, e lo dice** (nota in fondo): rinominare `foto.jpg`
 * in `foto.png` lascia dentro un JPEG con l'etichetta sbagliata, e l'app che poi lo apre si
 * fida del contenuto e non del nome, quindi il file funziona ma mente. Chi vuole cambiare
 * davvero formato usa 'Esporta/Converti', e la nota lo manda là.
 * ⚠️ **Il punto non si scrive**, e il campo lo scarta insieme a tutto quello che un nome di
 * file non può contenere: il punto lo rimette [renderName], e uno scritto qui darebbe
 * `foto..jpg`.
 */
@Composable
private fun ExtensionDialog(
    initial: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    var typed by rememberSaveable { mutableStateOf(initial) }
    val clean = typed.trim().trimStart('.')
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(null),
        properties = loweredWindow(null),
        title = { Text(stringResource(R.string.rename_ext)) },
        text = {
            /*
             * ⚠️⚠️ **LO SCORRIMENTO SERVE AL TETTO DELLA `1.62`, e fino alla `1.80` non c'era**
             * (censimento della UI del 2026-09-05): il tetto dà a una superficie centrata un
             * massimo pari alla finestra meno l'aria, e la nota che lo introduce dà per
             * acquisito che il contenuto porti già uno scorrimento dentro di sé. Qui non
             * c'era, quindi a tastiera aperta, dove il tetto scende ancora, la nota sotto il
             * campo veniva tagliata **senza modo di raggiungerla**.
             */
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = typed,
                    // ⚠️ Si filtra mentre si scrive invece di validare dopo: qui dentro va
                    // una parola di tre lettere, e un messaggio d'errore per un carattere
                    // che non doveva entrare costa più della lettera che si è tolta.
                    onValueChange = { t -> typed = t.filter { it.isLetterOrDigit() }.take(12) },
                    label = { Text(stringResource(R.string.rename_ext_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = BOX_SHAPE,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.rename_ext_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            // ⚠️ Si riusa `editor_apply` ('Applica') invece di aggiungere una stringa: è la
            // stessa parola per la stessa idea, esiste già in 28 lingue, e una copia sarebbe
            // un secondo posto da tenere d'accordo col primo.
            TextButton(onClick = { onPick(clean) }) {
                Text(stringResource(R.string.editor_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Un comando sulla riga del titolo di una finestra: una pastiglia col suo testo dentro.
 *
 * ⚠️⚠️ **ERA UN'ICONA NELLA `1.80` ED È TORNATA TESTUALE NELLA `1.81`, PERCHÉ HA CAMBIATO
 * IDEA** (riscontro del giro della `1.80`, voce `rinomina-icona`: *Ho cambiato idea: in
 * 'Rinomina', il tasto 'Estensione' deve tornare come prima (testuale, tasto stondato a destra,
 * linea di base del titolo della finestra)*). Quello che torna è **esattamente** la forma della
 * `1.34`, che lui aveva approvato: `FilledTonalButton`, raggio grande, riempimento stretto e
 * testo in `labelLarge`.
 * ⚠️ **Sta in una funzione perché i chiamanti sono tre**: 'Estensione' in 'Rinomina',
 * 'Estensione' e 'Destinazione' in 'Scarica'. Copiata, il giorno che la misura cambia ne
 * cambierebbe uno solo, che è la trappola già scritta su [Quiet].
 *
 * ⚠️⚠️ **IL RIEMPIMENTO VERTICALE È ZERO, ED È IL NUMERO CHE LA FA STARE NELLA RIGA**: un
 * `FilledTonalButton` di serie è alto 40dp e il suo riempimento ne aggiunge, quindi in una riga
 * di titolo darebbe una riga alta il doppio del testo. I 12dp orizzontali sono quelli della
 * `1.34`.
 * ⚠️ **'Allineato alla linea di base' si ottiene centrando**: la pastiglia è un riquadro, e il
 * suo testo sta al centro. È quello che faceva la `1.34`, cioè la forma approvata.
 * ⚠️⚠️ **E DUE PASTIGLIE NELLA STESSA RIGA CI STANNO PERCHÉ IL TITOLO CEDE**: nel chiamante il
 * titolo porta `weight(1f)` e queste no, quindi le pastiglie misurano il testo che hanno dentro
 * e il titolo si adatta andando a capo. Il verso opposto (peso alle pastiglie) le
 * comprimerebbe, cioè taglierebbe la parola che lui vuole leggere.
 */
@Composable
internal fun TitlePill(text: String, onTap: () -> Unit) {
    FilledTonalButton(
        onClick = onTap,
        shape = MaterialTheme.shapes.large,
        contentPadding = TITLE_PILL_PAD
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Quanto stringe una pastiglia della riga del titolo, che sta in una fila già piena. */
private val TITLE_PILL_PAD = PaddingValues(horizontal = 12.dp, vertical = 0.dp)

/**
 * Le righe dell'anteprima: i primi tre abbinamenti e **l'ultimo**.
 *
 * ⚠️ L'ultimo c'è perché porta il numero più alto, che è il solo modo di vedere se le
 * cifre del template bastano: con `##` e centoventi file, la riga finale dice `120` e si
 * capisce al volo che i nomi non si ordineranno come ci si aspetta.
 */
private fun previewOf(
    names: List<String>,
    template: String,
    start: Int,
    extension: String?
): List<Pairing> {
    if (names.isEmpty()) return emptyList()
    val rows = ArrayList<Pairing>(5)
    val head = minOf(names.size, 3)
    for (at in 0 until head) rows += pairing(names[at], template, start + at, extension)
    if (names.size > head + 1) rows += Pairing(null, null)
    if (names.size > head) {
        rows += pairing(names.last(), template, start + names.lastIndex, extension)
    }
    return rows
}

/**
 * Un nome di adesso e quello di dopo. Con tutti e due a `null` è la riga dei puntini, cioè
 * il buco fra i primi tre abbinamenti e l'ultimo.
 */
private data class Pairing(val before: String?, val after: String?)

private fun pairing(
    name: String,
    template: String,
    number: Int,
    extension: String?
): Pairing = Pairing(
    before = name,
    after = renderName(template, number, extension ?: name.substringAfterLast('.', ""))
)

/**
 * Una riga dell'anteprima: due pastiglie di colore diverso, una **sopra l'altra**.
 *
 * ⚠️⚠️ **AFFIANCATE ERANO SBAGLIATE, e la ragione è la larghezza dei nomi veri** (riscontro
 * dell'utente, 2026-09-02: *ho spesso a che fare con nomi lunghi, e su una colonna larga
 * praticamente il 35% dello schermo i loro nomi lunghissimi dovrebbero andare a capo molte
 * volte. Proviamo la versione sopra -> sotto*). Fino alla `1.29` stavano una accanto all'altra
 * con peso uguale, cioè ognuna su un terzo del dialogo: su un nome di quaranta caratteri quel
 * terzo diventa cinque righe, e cinque righe per due nomi sono dieci righe per un abbinamento.
 * Impilate, ognuna ha tutta la larghezza. ⚠️ **L'altezza cresce e non è un problema**, parole
 * sue: due righe intere si leggono meglio di dieci spezzoni.
 * ⚠️ **La freccia scende con loro**: fra le due pastiglie diventa un `↓`, perché una freccia a
 * destra fra due cose incolonnate indicherebbe il verso sbagliato.
 *
 * ⚠️ **I nomi passano da [nameWithExt]**: senza, il layout va a capo dentro l'estensione, e
 * un `.a` su una riga e un `vif` sull'altra non si leggono più come AVIF. È la prima delle tre
 * richieste del giro della 1.30, e vale come regola generale.
 * ⚠️⚠️ **E L'ESTENSIONE È IN GRASSETTO DALLA 1.37** (riscontro `ext-griglia`: *evidenzia
 * l'estensione, punto incluso, mettendola in grassetto*). Fino alla 1.36 questi due nomi
 * passavano da `unbroken`, che è una `String` nuda: il peso lo dava la pastiglia a tutto il
 * testo. ⚠️ **Negli altri posti c'era già** (la pastiglia di 'Info', la griglia, la barra del
 * visualizzatore) perché quelli passano da [fitName], che l'estensione la compone col
 * grassetto dalla 0.82: era l'anteprima l'unica fuori.
 */
@Composable
private fun PreviewRow(row: Pairing) {
    if (row.before == null || row.after == null) {
        Text(
            text = "...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 2.dp)
        )
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        /*
         * ⚠️⚠️ **LA PASTIGLIA DI ADESSO ERA INVISIBILE SUL TEMA SCURO, E IL CONTRASTO ERA
         * 1,00, cioè lo STESSO COLORE** (riscontro `ext-grassetto`, 2026-09-02: *nel tema scuro
         * manca la pillola intorno al nome corrente*). Non era un'impressione: `surfaceVariant`
         * vale `#2A312F` e la superficie del dialogo (`surfaceContainerHigh`) vale `#283130`,
         * cioè due punti di rosso di differenza. Sul tema chiaro il rapporto era 1,03, il
         * minimo che si intravede, ed è la ragione per cui il difetto si vedeva da una parte
         * sola.
         * ⚠️ **Adesso è INCAVATA e non rilevata**: `surfaceContainerLowest` è il gradino più
         * lontano dalla superficie del dialogo in tutti e due i temi (1,39 sullo scuro, 1,13
         * sul chiaro, misurati), ed è anche il ruolo giusto: questa è la casella di partenza,
         * l'altra è il risultato.
         * ⚠️⚠️ **E IL BORDO NON È DECORAZIONE: è la 'pillola INTORNO al nome' che lui ha
         * nominato**, e l'unica cosa che la fa esistere quando due superfici vicine si
         * somigliano comunque.
         * ⚠️⚠️ **E DALLA `1.47` CE L'HANNO TUTTE E DUE** (riscontro della `1.45`: *contorno: o
         * mai, o sempre; non regola generale, ma almeno in questo contesto*). Prima erano una
         * contornata e una piena, e quella coppia era raccontata qui come l'abbinamento
         * normale fra un punto di partenza e un risultato: in realtà nessuno l'aveva scelta,
         * la pastiglia dell'anteprima era nuda perché il filo aveva `null` come valore di
         * serie. A dire quale è la partenza e quale il risultato basta il riempimento, che è
         * già diverso, e il filo torna a dire soltanto dove finisce un riquadro.
         * ⚠️ **Quello dell'anteprima prende `primary`**, cioè il token da cui 'Annulla' e
         * 'Rinomina' prendono il colore del loro testo, perché era la richiesta alla lettera.
         * ⚠️⚠️ **IL COLORE DEL FILO NON È PIÙ `outlineVariant`, DALLA 1.45, perché sul tema
         * SCURO non si vedeva** (domanda dell'utente, 2026-09-03: *sbaglio o ha un filetto di
         * contorno solo nel tema chiaro?*). Non sbagliava, e il perché il rapporto di
         * contrasto dicesse il contrario sta su [hairline], insieme alla misura e alla via
         * scartata: qui basta sapere che il colore lo decide il tema e non questo dialogo.
         */
        NamePill(
            text = nameWithExt(row.before),
            back = MaterialTheme.colorScheme.surfaceContainerLowest,
            front = MaterialTheme.colorScheme.onSurfaceVariant,
            weight = FontWeight.Normal,
            border = BorderStroke(BOX_EDGE, hairline()),
            modifier = Modifier.fillMaxWidth()
        )
        /*
         * ⚠️⚠️ **UN'ICONA CENTRATA E NON UNA FRECCIA DI TESTO, dalla 1.34** (mockup
         * dell'utente, voce `ren-ext`: *una freccia più visibile e centrata, probabilmente
         * in grigio*). Il carattere `U+2193` a corpo di testo era **una lettera**: sottile
         * come il testo intorno, allineata a sinistra col rientro di una lettera, e nella
         * fila di due pastiglie larghe non si vedeva. Un'icona ha un peso suo e sta in mezzo
         * fra le due, che è dove l'occhio la cerca.
         * ⚠️ **Non porta descrizione**, come le altre icone decorative: quello che dice lo
         * dicono le due pastiglie, e un lettore di schermo che annunciasse 'freccia in basso'
         * fra due nomi di file leggerebbe un'informazione in più che non aggiunge niente.
         */
        Icon(
            imageVector = Icons.Filled.ArrowDownward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally).size(ARROW_SIZE)
        )
        NamePill(
            text = nameWithExt(row.after),
            back = MaterialTheme.colorScheme.secondaryContainer,
            front = MaterialTheme.colorScheme.onSecondaryContainer,
            weight = FontWeight.Medium,
            border = BorderStroke(BOX_EDGE, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Quanto è grande la freccia fra le due pastiglie dell'anteprima.
 *
 * ⚠️ **20 e non 24**: è la misura di Material per un'icona **dentro** un testo, e qui la
 * freccia sta fra due righe di nomi. A 24 diventava il pezzo più grosso del dialogo.
 * ⚠️ **Al posto di `ARROW_INDENT`, che era 10dp di rientro**: con la freccia centrata un
 * rientro da sinistra non vuol più dire niente.
 */
private val ARROW_SIZE = 20.dp

/**
 * Un comando secondario: piccolo, a destra, e sbiadito quando non serve.
 *
 * ⚠️ **È la stessa forma del 'Ripristina' dei riquadri del riordino**, e sta in una funzione
 * perché qui ne servono due: la ragione per cui quella forma è questa sta là, e ripeterla in
 * due punti di questo file sarebbe il primo posto in cui divergere.
 *
 * ⚠️⚠️ **DALLA `1.78` NON È PIÙ PRIVATO, ED È UNA SUA RICHIESTA**: *tutti con lo stile
 * solo-testo, senza tasto/pillola già usato in 'Rinomina'* (riscontro del giro della `1.77`,
 * voce `scarica-download`). La finestra del nome del salvataggio prende gli stessi comandi di
 * questa, quindi la forma deve essere **una**: copiata là, il giorno che il colore o il corpo
 * cambiano ne cambierebbe uno solo.
 *
 * @param onHold il tocco lungo, o `null` per un comando che non ne ha.
 *   ⚠️ **Il valore di serie è `null` di proposito, e qui è ammesso**: dei quattro comandi che
 *   oggi passano da qui uno solo ha un secondo gesto ('Data'). ⚠️ **Non è il caso del filo di
 *   `NamePill`**, dove un valore di serie ha lasciato una pastiglia senza bordo per due
 *   versioni: là l'assenza non si vedeva, qui un tocco lungo che non c'è non promette niente
 *   a nessuno, perché nessuno lo cerca se il comando non lo dichiara.
 */
@Composable
internal fun Quiet(
    text: String,
    enabled: Boolean,
    onTap: () -> Unit,
    onHold: (() -> Unit)? = null
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = accentInk(),
        modifier = Modifier
            /*
             * ⚠️ **Un bersaglio solo anche coi due gesti**: `combinedClickable` è un nodo, non
             * due, quindi un lettore di schermo annuncia un comando. È la stessa regola delle
             * righe con interruttore del pannello delle impostazioni.
             * ⚠️⚠️ **IL RUOLO E IL BERSAGLIO ARRIVANO CON LA `1.81`, e il difetto era doppio**
             * (censimento della UI del 2026-09-05): senza il ruolo un lettore di schermo
             * leggeva una parola e non un comando, e senza il minimo il bersaglio veniva alto
             * poco più della metà dei 48dp dovuti, perché `labelMedium` è 12sp su 16 di
             * interlinea più 6dp di margine per lato. Il colore, che era `primary`, è passato
             * ad [accentInk] nella stessa passata: come parole l'accento vero non si legge.
             * ⚠️ **`tapRoom()` va DOPO il gesto**, o non serve a niente: il perché vive su
             * `TAP_MIN`, in `Talk.kt`.
             */
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onTap,
                onLongClick = onHold
            )
            .tapRoom()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .alpha(if (enabled) 1f else OFF_INK)
    )
}


@Composable
private fun NamePill(
    text: AnnotatedString,
    back: Color,
    front: Color,
    weight: FontWeight,
    /**
     * Il filo intorno.
     *
     * ⚠️⚠️ **NON HA UN VALORE DI SERIE, ed è quello il difetto che la `1.47` toglie**: con
     * `null` come valore di serie la pastiglia dell'anteprima è rimasta senza filo per due
     * versioni, non perché qualcuno l'avesse deciso ma perché nessuno aveva scritto niente.
     * Senza valore di serie, 'o tutte o nessuna' non è più una regola da ricordare: è una
     * cosa che il compilatore chiede.
     */
    border: BorderStroke,
    modifier: Modifier = Modifier
) {
    /*
     * ⚠️⚠️ **IL TESTO SI STRINGE PER NON LASCIARE UN CARATTERE DA SOLO, dalla `1.60`**
     * (riscontro del giro della `1.59`: *se si rischia di andare a capo con un solo carattere,
     * per me va bene anche riscrivere il testo con un carattere leggermente più piccolo per
     * farcelo stare, entro una certa soglia*). È la **prima** delle tre vie che ha indicato;
     * la terza (andare a capo con tutta l'estensione) è quella che regge sotto, e vive su
     * `BREAK_HERE` in `Names.kt`.
     * ⚠️⚠️ **LA SECONDA VIA, L'ELLISSI, NON SI FA, e va detto invece di lasciarlo intendere**:
     * qui i tre punti li metterebbe `TextOverflow` alla **fine**, cioè mangerebbero proprio
     * l'estensione, che è la parte che lui ha chiesto di salvare sempre. In un'anteprima di
     * rinomina, un nome accorciato non si può nemmeno confrontare con l'altro.
     * ⚠️ **La misura arriva da `onTextLayout` e non da un conto sui caratteri**: quanti ne
     * stanno su una riga dipende da quali sono, e un tetto scritto a mano sarebbe sbagliato in
     * ogni lingua e a ogni larghezza.
     * ⚠️ **Converge per costruzione**: si scende di un gradino per volta e mai sotto
     * [NAME_FLOOR], quindi il giro è finito anche quando stringere non toglie l'orfano.
     * ⚠️ **La chiave è il testo**: cambiando nome si riparte dalla misura piena, o il primo
     * nome difficile rimpicciolirebbe per sempre tutti quelli dopo.
     */
    var stretta by remember(text) { mutableStateOf(1f) }
    // ⚠️ La scalatura passa da `shrunk` di `Names.kt`, dalla 1.62: era scritta qui e ora
    // vive dove vive anche il misuratore che la applica, o le due potrebbero divergere.
    val stile = MaterialTheme.typography.bodySmall.shrunk(stretta)
    Surface(
        color = back,
        shape = BOX_SHAPE,
        border = border,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = stile,
            onTextLayout = { steso ->
                if (stretta > NAME_FLOOR && orphan(steso)) stretta -= NAME_STEP
            },
            color = front,
            fontWeight = weight,
            /*
             * ⚠️ **Nessun tetto di righe dalla 1.30**, e prima erano due: impilate le
             * pastiglie hanno tutta la larghezza, quindi un nome ci sta quasi sempre in una
             * riga o due, e un tetto taglierebbe proprio i nomi lunghissimi per cui l'utente
             * ha chiesto questa forma. L'anteprima mostra al massimo cinque abbinamenti: non
             * può crescere senza limite.
             */
            /*
             * ⚠️⚠️ **8 SOPRA E SOTTO DALLA 1.48, ED ERANO 4** (riscontro dell'utente, giro
             * della 1.47: *dai un po' più di spazio sopra e sotto ai due riquadri di rinomina
             * prima/dopo, perché i caratteri minuscoli con i discendenti (q, g, ecc.) sono
             * veramente a filo*). Non era una svista di misura: un riempimento **simmetrico**
             * intorno a un testo si **vede** asimmetrico, perché l'ascendente di un carattere
             * porta con sé dell'aria sopra le maiuscole che il discendente non porta sotto la
             * coda della `g`. Con 4 da tutte e due le parti, sopra si sommavano il
             * riempimento e quell'aria, sotto c'era il solo riempimento.
             * ⚠️ **Quanta sia quell'aria dipende dal carattere di sistema**, quindi non si
             * scrive un numero qui: la cura è dare al lato stretto abbastanza spazio da non
             * dipendere da quel margine, non pareggiare i due lati con una misura presa da un
             * carattere che sul telefono di qualcun altro è un altro.
             * ⚠️ **E il riempimento resta uguale sui quattro lati**: la pastiglia di una riga
             * viene così alta quanto un contenitore piccolo di Material, che è un posto in cui
             * atterrare invece di un numero scelto a occhio.
             */
            modifier = Modifier.padding(8.dp)
        )
    }
}

/**
 * Se l'ultima riga porta un carattere solo, cioè un orfano.
 *
 * ⚠️ **Il conto è in unità di codice e non in caratteri visibili**, e va bene: l'estensione
 * porta i giuntori invisibili di `Names.kt`, quindi la sua riga conta sempre più di uno e non
 * si può scambiare per un orfano. Chi cerca il numero vero di lettere qui non lo troverebbe, e
 * qui non serve.
 * ⚠️ **Una riga sola non ha orfani**: se il nome sta tutto su una riga non c'è niente da
 * stringere, e senza questa guardia un nome cortissimo si stringerebbe fino al fondo.
 */
private fun orphan(steso: TextLayoutResult): Boolean {
    val ultima = steso.lineCount - 1
    if (ultima <= 0) return false
    return steso.getLineEnd(ultima, visibleEnd = true) - steso.getLineStart(ultima) <= 1
}


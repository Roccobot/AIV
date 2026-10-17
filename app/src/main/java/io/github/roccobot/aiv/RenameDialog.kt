package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.times
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Rinominare in blocco: un template col numero dentro, e il primo numero.
 *
 * ⚠️ **L'anteprima è il solo modo di vedere un template sbagliato prima di applicarlo**:
 * rinominare ottanta file non si annulla. Mostra i primi e l'ultimo, che dice se le cifre bastano.
 * Il template si propone una volta sola: ricalcolarlo mentre si cambia il primo numero
 * riscriverebbe un testo che si sta scrivendo.
 * I nomi arrivano già ordinati da `FileTree.namesOf`, con l'ordinamento della rinomina vera:
 * ordinati qui, l'anteprima potrebbe mostrare un abbinamento che poi non succede.
 */
@Composable
fun RenameDialog(
    uris: List<Uri>,
    onDismiss: () -> Unit,
    onRename: (template: String, start: Int, extension: String?) -> Unit,
    /**
     * Il nome della **cartella** da rinominare, quando è una cartella e non dei file.
     *
     * È la stessa finestra della rinomina del file singolo, senza percorso né estensione
     * (richiesta dell'utente): un parametro, non una finestra gemella che divergerebbe.
     * ⚠️ **Il campo parte dal nome intero, punto compreso** (stessa richiesta): per i file il
     * suffisso è fuori dal campo perché è un dato del formato, ma una cartella `foto.old` ha
     * solo un nome col punto dentro. Il comando 'Estensione' non c'è.
     */
    folder: String? = null
) {
    val context = LocalContext.current
    val names by produceState<List<String>?>(null, uris, folder) {
        value = folder?.let { listOf(it) } ?: FileTree.namesOf(context, uris)
    }

    var template by rememberSaveable { mutableStateOf("") }
    var start by rememberSaveable { mutableStateOf("1") }
    var proposed by rememberSaveable { mutableStateOf(false) }

    /*
     * ⚠️ **`null` vuol dire 'ognuno tiene la sua'** (richiesta dell'utente), diverso dalla
     * stringa vuota, 'nessuna estensione'. Finché il pannellino non si apre resta `null`: con una
     * selezione mista l'estensione del primo a tutti sarebbe un danno.
     */
    var extension by rememberSaveable { mutableStateOf<String?>(null) }

    /*
     * Il cancello dell'estensione è condiviso con la finestra del salvataggio (voce
     * `scarica-download`: lo stesso pulsante, con identico funzionamento). Che cosa fa è su
     * [extensionGate].
     */
    val gate = extensionGate(
        where = ExtWhere.RENAME,
        // Qui la griglia di sicurezza non si scavalca mai: la scavalca per una volta il tocco
        // lungo su 'Scarica', e questa finestra si apre da una voce di menu.
        force = false,
        // Il valore di partenza è quello corrente: l'estensione già scelta, altrimenti quella
        // del primo file. Senza il punto.
        initial = { extension ?: names?.firstOrNull()?.substringAfterLast('.', "").orEmpty() },
        onPick = { extension = it }
    )

    // ⚠️ Un file solo non è una rinomina in blocco (riscontro dell'utente): parte dal nome
    // originale, senza primo numero né spiegazione dei cancelletti. Una cartella è un nome solo,
    // e vale lo stesso.
    val singolo = folder != null || uris.size == 1

    val listed = names
    LaunchedEffect(listed) {
        if (proposed || listed == null) return@LaunchedEffect
        // ⚠️ Proposto anche con l'elenco vuoto: `proposed` apre la finestra, e senza, con nomi
        // illeggibili, 'Rinomina' non aprirebbe niente.
        proposed = true
        if (listed.isEmpty()) return@LaunchedEffect
        // Il nome senza estensione, che la rimette `renderName` (se no `foto.jpg.jpg`); per una
        // cartella il nome intero ([folder]).
        template = when {
            folder != null -> listed.first()
            singolo -> listed.first().substringBeforeLast('.', listed.first())
            else -> suggestTemplate(listed.first(), listed.size, start.toIntOrNull() ?: 1)
        }
    }

    /*
     * ⚠️ **Non si apre finché non ha i nomi e il template**, come la scheda delle informazioni
     * (`FileOps`): `FileTree.namesOf` passa da `Dispatchers.IO`, quindi al primo fotogramma i nomi
     * non ci sono, e la finestra si disegnerebbe vuota per poi crescere.
     * La guardia è `proposed`, non `listed != null`: `proposed` sopravvive alla rotazione, `names`
     * riparte da capo, e la finestra sparirebbe e tornerebbe.
     * Il prezzo è un'attesa fra il tocco e la finestra: un salto di thread con un file, una
     * lettura per file con una selezione grande.
     */
    if (!proposed) return

    val first = start.toIntOrNull()
    val clean = template.trim()
    val numbered = clean.contains('#')
    // ⚠️ Il cancelletto è obbligatorio solo con più nomi da fare, e la condizione è su [singolo],
    // non su `uris.size`: con una cartella quell'elenco è vuoto, e il tasto resterebbe spento.
    val ready = listed != null && clean.isNotEmpty() && first != null && first >= 0 &&
        (singolo || numbered)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(null),
        properties = loweredWindow(null),
        /*
         * 'Estensione' è sulla riga del titolo, a destra (voci `ren-ext` e `rinomina-icona`), e
         * la fila dei tasti resta quella di Material: 'Annulla' e 'Rinomina'. La riga la compone
         * [TitleRow], come nella finestra del salvataggio, che ha gli stessi comandi: decide se la
         * pastiglia resta scritta o diventa un'icona secondo lo spazio accanto al titolo.
         */
        title = {
            TitleRow(
                title = stringResource(R.string.pick_rename),
                // Con una cartella il comando non c'è: non c'è un file di cui cambiare il suffisso.
                commands = if (!gate.allowed || folder != null) emptyList() else listOf(
                    TitleCommand(
                        text = stringResource(R.string.rename_ext),
                        glyph = Glyphs.Extension,
                        onTap = gate.open
                    )
                )
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                /*
                 * ⚠️ **Il campo usa un `TextFieldValue` per la posizione del cursore**, che deve
                 * essere in fondo (riscontro del giro della 1.59): con una `String` il cursore
                 * lo tiene il campo, e da fuori non si sposta. Un campo a riga sola scorre fin
                 * dov'è il cursore, quindi si vede la coda del nome.
                 * La verità resta [template], che legge l'anteprima e che sopravvive alla
                 * rotazione; dopo una rotazione l'effetto qui sotto rifà il campo.
                 */
                var campo by remember { mutableStateOf(TextFieldValue()) }
                LaunchedEffect(template) {
                    if (campo.text != template) {
                        campo = TextFieldValue(template, TextRange(template.length))
                    }
                }
                /*
                 * ⚠️ **Il fuoco si chiede all'apertura** (riscontro del giro della 1.60): senza
                 * fuoco un campo a riga sola mostra la testa del testo e non scorre; e un cursore
                 * messo in fondo all'arrivo del fuoco perderebbe contro il tocco che lo porta.
                 * Così chi apre trova la coda del nome, e chi tocca in mezzo sposta il cursore.
                 * Una volta sola per apertura (`LaunchedEffect(Unit)`), o a ogni ricomposizione
                 * il fuoco tornerebbe qui.
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
                    // Con un nome solo il tasto della tastiera dice 'fine': sotto non c'è
                    // un'altra casella.
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (singolo) ImeAction.Done else ImeAction.Next
                    ),
                    shape = BOX_SHAPE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(fuoco)
                )
                /*
                 * I comandi sul nome: 'Seleziona tutto', 'Svuota' (richiesta dell'utente: sobri)
                 * e 'Data', come in 'Scarica', con le stesse funzioni e lo stesso verso della
                 * fila. Il tocco breve su 'Data' infila la data dov'è il cursore, il lungo rifà il
                 * nome da capo.
                 * La forma è quella del 'Ripristina' dei riquadri del riordino, già approvata: a
                 * destra, corpo più piccolo, sbiaditi e non spariti quando non c'è niente da fare.
                 * Non toccano il fuoco, così la tastiera resta aperta.
                 * ⚠️ **Ogni comando scrive [template] e [campo] insieme**: un comando che scrivesse
                 * solo [campo] cambierebbe quello che si legge e non l'anteprima, senza errori.
                 * `Arrangement.End` senza spaziatura: l'aria la mette il riempimento di [Quiet].
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
                     * Col tocco lungo e più file la data è seguita da uno spazio e dai
                     * cancelletti che servono al numero dei file (risposta `d-data-blocco`):
                     * il conto è [hashesFor], lo stesso del template proposto all'apertura.
                     * Così il template resta valido e 'Rinomina' acceso. Con un nome solo resta
                     * la sola data.
                     * ⚠️ **Il conto è su [uris], non sui nomi letti**: con nomi illeggibili
                     * `listed` è vuoto, e darebbe due cancelletti per qualunque selezione.
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
                // Il primo numero e la spiegazione dei cancelletti solo con più nomi: con uno
                // solo una casella che non decide niente sembrerebbe da riempire.
                if (!singolo) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = start,
                        // Si filtra alle cifre mentre si scrive: la tastiera numerica di Android
                        // offre comunque virgole e segni, e un numero negativo qui non vale.
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
                    PreviewLines(previewOf(listed, clean, first, extension))
                }
            }
        },
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
 * Ognuna delle due finestre ha il suo chip nell'impostazione (riscontro del giro della 1.80,
 * punto A), e questo dice quale si legge. Non ha un valore di serie, come `force`: chi apre la
 * finestra lo dichiara.
 */
internal enum class ExtWhere { RENAME, DOWNLOAD }

/**
 * Il comando 'Estensione' con tutto quello che gli serve: la griglia di sicurezza, l'avviso
 * della prima volta e il pannellino.
 *
 * ⚠️ **Il comando c'è solo con l'impostazione accesa**, la griglia di sicurezza chiesta
 * dall'utente e spenta di fabbrica: cambiare l'estensione non converte niente e può far sparire
 * un'immagine dalle viste (il perché è su [Settings.extRename]). Gli interruttori sono due,
 * [Settings.extRename] e [Settings.extDownload], i due chip di una voce sola; quale si legge lo
 * dice [where].
 *
 * Le letture si fanno qui, contro la convenzione dei dialoghi di questo file (`FileJobDialogs` si
 * fa passare tutto): delle tre schermate che aprono la rinomina (griglia, albero, visualizzatore)
 * solo l'ultima ha le impostazioni, e passarle costerebbe quattro firme in più. Come i veli
 * (`Hint.flow`). Il valore iniziale è `false`: durante la lettura il comando non c'è, invece di
 * comparire per un istante a chi l'ha spento.
 *
 * Il tocco lungo su 'Scarica' scavalca la griglia per una volta (richiesta dell'utente, giro della
 * 1.79, punto D), come accende la rinomina al volo. Non scavalca l'avviso: la prima volta compare
 * comunque, perché è lui che protegge.
 *
 * ⚠️ **Si porta dietro le proprie finestre**: chi lo chiama ottiene un tasto che funziona, senza
 * righe da ricordare; un avviso o un pannellino dimenticati non darebbero errori, solo un tasto
 * che non fa niente. Lo stesso criterio di `lowered()` col velo.
 * L'ordine nella composizione non decide quale finestra è sopra: le due nascono al tocco, col
 * dialogo già in scena, e arrivano dopo di lui nel gestore delle finestre.
 *
 * @param where quale delle due finestre lo chiede, cioè quale chip si legge.
 * @param force se il comando c'è anche a impostazione spenta. Non ha un valore di serie: chi apre
 *   la finestra dichiara se scavalca la griglia, come col parametro di `Modifier.lowered`.
 * @param initial l'estensione da cui parte il pannellino, senza il punto: una funzione, perché si
 *   legge quando il pannellino si apre.
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
     * Un velo in una finestra sua ([HintNotice]): chi chiama è già dentro un dialogo, e un velo
     * sul contenuto coprirebbe la finestrella e non lo schermo.
     * Alla chiusura l'avviso si archivia e il pannellino si apre: si legge una volta sola, e chi
     * ha toccato 'Estensione' voleva il pannellino.
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
     * L'avviso prima del pannellino, non insieme: dice che cosa comporta il gesto prima del gesto.
     * ⚠️ **In AIV Play il comando non c'è mai, nemmeno col tocco lungo su 'Scarica'** (nota
     * dell'utente sul giro della 5.20): l'impostazione è sempre spenta (`Store.fit`), ma `force`
     * la scavalcherebbe, quindi serve `Store.fullSettings`.
     */
    return ExtensionGate(Store.fullSettings && (force || allowed)) { if (warned) asking = true else warning = true }
}

/**
 * Il pannellino della sola estensione.
 *
 * ⚠️ **Cambia il nome e non il formato, e la nota in fondo lo dice**: `foto.jpg` rinominata
 * `foto.png` resta un JPEG con l'etichetta sbagliata. Per cambiare formato c'è
 * 'Esporta/Converti'.
 * Il campo scarta il punto con tutto quello che non è lettera o cifra: il punto lo rimette
 * [renderName], e scritto qui darebbe `foto..jpg`.
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
             * ⚠️ **Lo scorrimento serve al tetto delle superfici centrate** (`Centred.kt`), che
             * presume un contenuto capace di scorrere: senza, a tastiera aperta la nota sotto il
             * campo sarebbe tagliata e irraggiungibile.
             */
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                /*
                 * Il campo prende il fuoco appena il pannello compare, come quello del nome in
                 * [RenameDialog] (nota C del giro della 4.36): un pannello senza un campo col
                 * fuoco chiuderebbe la tastiera, e le due finestre si ridisporrebbero con un
                 * salto. La causa è ragionata e non misurata: il banco non ha tastiera.
                 */
                val fuoco = remember { FocusRequester() }
                LaunchedEffect(Unit) { fuoco.requestFocus() }
                OutlinedTextField(
                    value = typed,
                    // Si filtra mentre si scrive: qui va una parola di tre lettere, e un
                    // messaggio d'errore costerebbe più della lettera tolta.
                    onValueChange = { t -> typed = t.filter { it.isLetterOrDigit() }.take(12) },
                    label = { Text(stringResource(R.string.rename_ext_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = BOX_SHAPE,
                    modifier = Modifier.fillMaxWidth().focusRequester(fuoco)
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
            // `editor_apply` ('Applica'): la stessa parola per la stessa idea, già in 28 lingue.
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
 * Un comando della riga del titolo, nelle due forme che può prendere.
 *
 * Contiene tutte e due, testo e glifo, perché a scegliere è [TitleRow], non il chiamante.
 */
internal class TitleCommand(
    val text: String,
    val glyph: ImageVector,
    val onTap: () -> Unit,
    /**
     * Se il comando si può toccare.
     *
     * Per un comando che vale solo in certi casi, come 'Rendi predefinito' (attivo solo con 'Lato
     * lungo', 'Lato corto' o '%'): spento e non assente, perché un tasto che compare e sparisce
     * farebbe ballare la riga del titolo.
     */
    val enabled: Boolean = true,
    /**
     * Quante righe il testo della pastiglia può prendere.
     *
     * Due è il caso di una locuzione: [TitleRow] misura il testo su una riga, e 'Rendi
     * predefinito' diventerebbe sempre un'icona. A due righe la pastiglia si misura sulla parola
     * più lunga, e resta scritta come nel mockup. Non alza la riga: due righe di `labelMedium`
     * entrano nei 40dp di altezza minima di un `FilledTonalButton`.
     */
    val lines: Int = 1
)

/**
 * La riga del titolo di una finestra coi suoi comandi: il titolo a sinistra, la fila a destra.
 *
 * ⚠️ **Fra pastiglia e icona sceglie la misura** (risposta `misura` a `d-pill-soglia`): con una
 * parola lunga (il polacco *Miejsce docelowe*, la più lunga delle ventotto lingue) il titolo
 * accanto andrebbe a capo. Con due comandi sempre le icone (riscontro del giro della 1.81, punto
 * C: due pulsanti scritti non possono coesistere).
 * ⚠️ **Si misura il titolo**: la pastiglia entra sempre, perché a cedere è il titolo ([TitlePill]
 * non ha peso), quindi 'entra' vuol dire che il titolo resta su una riga. [TITLE_ROW_GAP] è la
 * distanza sotto la quale le due parole si toccherebbero, e vive nel conto e non nel layout:
 * nel layout sposterebbe anche la riga con le icone.
 */
@Composable
internal fun TitleRow(
    title: String,
    commands: List<TitleCommand>,
    /**
     * La riga sotto il titolo, quando la finestra ne ha una.
     *
     * È parte della riga del titolo (voce `resize-finestra`): nel corpo della finestra sembrerebbe
     * la prima riga del contenuto, e la pastiglia si centrerebbe sul solo titolo.
     */
    subtitle: AnnotatedString? = null
) {
    val righello = rememberTextMeasurer()
    val corpoTitolo = MaterialTheme.typography.headlineSmall
    /*
     * I due corpi si leggono qui: il tema si legge in composizione, e la lambda di `map` non lo è,
     * quindi [titlePillStyle] là dentro non compilerebbe.
     */
    val corpoUno = titlePillStyle(1)
    val corpoDue = titlePillStyle(2)
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val quanto = { testo: String, stile: TextStyle ->
            with(density) {
                righello.measure(AnnotatedString(testo), stile, maxLines = 1).size.width.toDp()
            }
        }
        /*
         * ⚠️ **La larghezza si misura una volta e la usano in due**: il conto che sceglie fra
         * pastiglia e icona, e la pastiglia quando va a capo. Se il disegno si dimensionasse da sé,
         * il conto prometterebbe una pastiglia larga quanto la parola più lunga e ne comparirebbe
         * una larga quanto la frase (voce `resize-finestra-2`).
         */
        val larghezze = commands.map { comando ->
            // Con più righe si misura la parola più lunga, dove il testo si spezza: la frase
            // intera porterebbe sempre all'icona.
            val parola =
                if (comando.lines > 1) comando.text.split(' ').maxBy { it.length }
                else comando.text
            /*
             * Si misura col corpo con cui [TitlePill] scrive, che a due righe scende di un
             * gradino ([titlePillStyle]): col corpo pieno la larghezza sarebbe più larga del testo.
             */
            maxOf(
                TITLE_PILL_MIN,
                quanto(parola, if (comando.lines > 1) corpoDue else corpoUno) +
                    TITLE_PILL_SIDE * 2 + TITLE_PILL_SLACK
            )
        }
        val aIcone = commands.size > 1 || larghezze.any { pastiglia ->
            quanto(title, corpoTitolo) + TITLE_ROW_GAP + pastiglia > maxWidth
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Il titolo prende il peso: senza, una parola lunga spingerebbe i comandi oltre il
            // bordo.
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for ((posto, comando) in commands.withIndex()) {
                    if (aIcone) {
                        TitleIcon(
                            glyph = comando.glyph,
                            description = comando.text,
                            onTap = comando.onTap,
                            enabled = comando.enabled
                        )
                    } else {
                        TitlePill(
                            text = comando.text,
                            onTap = comando.onTap,
                            enabled = comando.enabled,
                            lines = comando.lines,
                            /*
                             * ⚠️ **La larghezza imposta è quella che fa andare a capo**: una
                             * pastiglia che si dimensiona sul contenuto non costringe mai il
                             * testo a spezzarsi. Larga quanto la parola più lunga, la frase va
                             * su due righe. A una riga la larghezza del contenuto è già quella
                             * del conto.
                             */
                            modifier = if (comando.lines > 1) {
                                Modifier.width(larghezze[posto])
                            } else {
                                Modifier
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Un comando sulla riga del titolo di una finestra: una pastiglia col suo testo dentro.
 *
 * La forma approvata dall'utente (voce `rinomina-icona`: testuale, tasto stondato a destra,
 * sulla linea di base del titolo): `FilledTonalButton`, raggio grande, riempimento stretto. Una
 * funzione sola per i tre chiamanti ('Estensione' in 'Rinomina', 'Estensione' e 'Destinazione'
 * in 'Scarica').
 *
 * ⚠️ **Il riempimento verticale è zero**: un `FilledTonalButton` di serie è alto 40dp e il
 * riempimento ne aggiunge, e la riga del titolo diventerebbe alta il doppio del testo. La linea
 * di base si ottiene centrando: il testo è al centro del riquadro.
 * La pastiglia non ha peso, il titolo sì (`weight(1f)` nel chiamante): a cedere è il titolo, che va
 * a capo, e la parola nella pastiglia resta intera.
 */
@Composable
internal fun TitlePill(
    text: String,
    onTap: () -> Unit,
    enabled: Boolean = true,
    lines: Int = 1,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onTap,
        enabled = enabled,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        contentPadding = TITLE_PILL_PAD
    ) {
        /*
         * Il corpo è un gradino sotto quello di un pulsante (nota dell'utente sulla voce
         * `save-regola`), e scende ancora a due righe ([titlePillStyle]). L'allineamento non
         * cambia: il `FilledTonalButton` ha la sua altezza minima, e il testo è al centro.
         */
        Text(
            text = text,
            style = titlePillStyle(lines),
            maxLines = lines,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Il corpo del testo di una pastiglia del titolo, che dipende da quante righe può prendere.
 *
 * A due righe il corpo passa a `labelSmall` e l'interlinea a [TITLE_PILL_LEAD] (nota dell'utente
 * sulla voce `resize-pastiglia`); a una riga resta `labelMedium`, il corpo approvato.
 * ⚠️ **Lo leggono il conto e il disegno**: [TitleRow] misura la parola più lunga con questo corpo
 * per imporre la larghezza, e un corpo diverso da quello di [TitlePill] darebbe una frase che non
 * entra, o una pastiglia più larga del testo.
 */
@Composable
private fun titlePillStyle(lines: Int): TextStyle {
    val base =
        if (lines > 1) MaterialTheme.typography.labelSmall
        else MaterialTheme.typography.labelMedium
    if (lines <= 1 || !base.fontSize.isSpecified) return base
    return base.copy(lineHeight = base.fontSize * TITLE_PILL_LEAD)
}

/**
 * Quanto vale l'interlinea di una pastiglia a due righe, in rapporto al proprio corpo.
 *
 * Un rapporto, perché il corpo lo dà il tema: in `sp` resterebbe indietro se la tipografia
 * cambiasse. Il valore di serie di `labelSmall` supera il 145%, l'aria di un paragrafo: qui le
 * righe sono due parole, e con quell'aria si leggerebbero come due cose.
 */
private const val TITLE_PILL_LEAD = 1.27f

/**
 * Il riempimento orizzontale di una pastiglia del titolo, che è anche metà del conto di
 * [TitleRow]: la sua larghezza è la parola più due volte questo numero.
 *
 * ⚠️ **È scritta prima di [TITLE_PILL_PAD], che la legge**: le proprietà di un file si
 * inizializzano nell'ordine in cui sono scritte, e al contrario il riempimento risulterebbe zero.
 */
private val TITLE_PILL_SIDE = 12.dp

/** Il riempimento di una pastiglia della riga del titolo, che è in una fila già piena. */
private val TITLE_PILL_PAD = PaddingValues(horizontal = TITLE_PILL_SIDE, vertical = 0.dp)

/**
 * La larghezza minima di un `FilledTonalButton`, che Material non espone come costante
 * pubblica: sotto quel numero la pastiglia non si stringe, quindi una parola corta non la
 * rimpicciolisce e il conto di [TitleRow] deve saperlo.
 */
private val TITLE_PILL_MIN = 58.dp

/**
 * Il pelo d'aria che la pastiglia a due righe ha in più della parola che la misura.
 *
 * Serve all'arrotondamento: la misura passa da pixel a punti e torna a pixel, e una frazione
 * persa manderebbe in ellissi proprio la parola misurata. È dentro il conto, quindi il numero che
 * sceglie la forma e quello che la disegna restano lo stesso.
 */
private val TITLE_PILL_SLACK = 1.dp

/**
 * L'aria che deve restare fra il titolo e la pastiglia perché il testo resti scritto.
 *
 * Non è un margine del layout: il perché è su [TitleRow].
 */
private val TITLE_ROW_GAP = 8.dp

/**
 * Lo stesso comando di [TitlePill], come **icona**: la forma dei comandi quando nella riga del
 * titolo sono due, o quando la pastiglia non entra ([TitleRow]; regola dell'utente, giro della
 * 1.81, punto C).
 *
 * Il glifo lo passa il chiamante ([Glyphs.FolderDownload], [Glyphs.Extension]). La descrizione
 * parlata è la stessa parola della pastiglia: per un lettore di schermo le due forme sono lo
 * stesso comando.
 * Il bersaglio è 40dp e non i 48 di serie: un `IconButton` intero alzerebbe la riga del titolo, e
 * sotto i 40dp il tocco diventa difficile.
 */
@Composable
internal fun TitleIcon(
    glyph: ImageVector,
    description: String,
    onTap: () -> Unit,
    enabled: Boolean = true
) {
    IconButton(onClick = onTap, enabled = enabled, modifier = Modifier.size(TITLE_ICON_TAP)) {
        Icon(imageVector = glyph, contentDescription = description)
    }
}

/** Il bersaglio di un comando-icona della riga del titolo. */
private val TITLE_ICON_TAP = 40.dp

/**
 * Le righe dell'anteprima: tutti gli abbinamenti fino a tre file, e con di più i primi due, lo
 * stacco e **l'ultimo**; fra due abbinamenti che si seguono, il separatore.
 *
 * ⚠️ **L'ultimo c'è perché ha il numero più alto**, il solo modo di vedere se le cifre del
 * template bastano: con `##` e centoventi file la riga finale dice `120`.
 * I primi due, lo stacco e l'ultimo sono la specifica dell'utente (giro della 2.90). Lo stacco
 * nasce dove l'indice salta, quindi c'è solo quando nasconde almeno un nome.
 */
internal fun previewOf(
    names: List<String>,
    template: String,
    start: Int,
    extension: String?
): List<PreviewLine> {
    val shown = if (names.size <= PREVIEW_HEAD + 1) names.indices.toList()
    else (0 until PREVIEW_HEAD) + names.lastIndex
    val lines = ArrayList<PreviewLine>(2 * shown.size)
    for ((k, at) in shown.withIndex()) {
        if (k > 0) lines += if (at > shown[k - 1] + 1) PreviewLine.Skip else PreviewLine.Rule
        lines += pairing(names[at], template, start + at, extension)
    }
    return lines
}

/** Quanti abbinamenti l'anteprima mostra prima dello stacco. */
private const val PREVIEW_HEAD = 2

/**
 * Una riga dell'anteprima: un abbinamento, il separatore fra due abbinamenti, o lo stacco.
 *
 * Separatore e stacco sono righe della sequenza, non un dettaglio del disegno: così il banco li
 * prova chiamando [previewOf], senza montare niente.
 */
internal sealed interface PreviewLine {
    /** Un nome di adesso e quello di dopo. */
    data class Pairing(val before: String, val after: String) : PreviewLine

    /** Il separatore fra due abbinamenti che si seguono. */
    data object Rule : PreviewLine

    /** Lo stacco: fra i due abbinamenti accanto ci sono dei nomi che l'anteprima non mostra. */
    data object Skip : PreviewLine
}

private fun pairing(
    name: String,
    template: String,
    number: Int,
    extension: String?
): PreviewLine.Pairing = PreviewLine.Pairing(
    before = name,
    after = renderName(template, number, extension ?: name.substringAfterLast('.', ""))
)

/**
 * L'anteprima disegnata: le righe di [previewOf], nell'ordine in cui arrivano.
 *
 * È il mockup dell'utente, misurato (giro della 2.90): dentro un abbinamento [PAIR_GAP], fra due
 * abbinamenti [BETWEEN], così il risultato di un file è più vicino al proprio nome che al nome del
 * file dopo. Le misure vengono dai suoi pixel, a 1,625 per dp.
 * Separatore e stacco occupano lo stesso posto ([Between]), e il passo fra le coppie resta uguale.
 */
@Composable
internal fun PreviewLines(lines: List<PreviewLine>) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        for (line in lines) {
            when (line) {
                is PreviewLine.Pairing -> PreviewPair(line)
                PreviewLine.Rule -> Between { FadedRule() }
                PreviewLine.Skip -> Between { PairSkip() }
            }
        }
    }
}

/**
 * Un abbinamento dell'anteprima: due pastiglie di colore diverso, una **sopra l'altra**.
 *
 * ⚠️ **Impilate, perché i nomi veri sono lunghi** (riscontro dell'utente): affiancate, ognuna su
 * un terzo del dialogo, un nome di quaranta caratteri andrebbe su cinque righe. Impilate hanno
 * tutta la larghezza; l'altezza cresce, e due righe intere si leggono meglio di dieci spezzoni.
 * Fra le due la freccia scende.
 *
 * I nomi passano da [nameWithExt]: senza, il layout andrebbe a capo dentro l'estensione (`.a` su
 * una riga, `vif` sull'altra). L'estensione è in grassetto, punto compreso, come in ogni altro
 * nome dell'app (riscontro `ext-griglia`).
 */
@Composable
private fun PreviewPair(line: PreviewLine.Pairing) {
    Column(Modifier.fillMaxWidth()) {
        /*
         * ⚠️ **Il nome di adesso è incavato**: `surfaceContainerLowest` è il gradino più lontano
         * dalla superficie del dialogo in tutti e due i temi (contrasto 1,39 sullo scuro, 1,13 sul
         * chiaro, misurati); `surfaceVariant` sul tema scuro aveva lo stesso colore del dialogo
         * (riscontro `ext-grassetto`). È la casella di partenza, l'altra il risultato.
         * Il bordo è la 'pillola intorno al nome' che l'utente ha chiesto, e ce l'hanno tutte e
         * due (riscontro della 1.45: contorno o mai o sempre); partenza e risultato li distingue
         * il riempimento. Il filo del nome di adesso lo decide il tema ([hairline]); quello del
         * risultato è `primary`, il colore del testo di 'Annulla' e 'Rinomina', come chiesto.
         */
        NamePill(
            text = nameWithExt(line.before),
            back = MaterialTheme.colorScheme.surfaceContainerLowest,
            front = MaterialTheme.colorScheme.onSurfaceVariant,
            weight = FontWeight.Normal,
            border = BorderStroke(BOX_EDGE, hairline()),
            modifier = Modifier.fillMaxWidth()
        )
        PairArrow()
        NamePill(
            text = nameWithExt(line.after),
            back = MaterialTheme.colorScheme.secondaryContainer,
            front = MaterialTheme.colorScheme.onSecondaryContainer,
            weight = FontWeight.Medium,
            border = BorderStroke(BOX_EDGE, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * La freccia fra le due pastiglie di un abbinamento: nasce da quella di sopra e si ferma poco
 * sopra quella di sotto.
 *
 * Un'icona centrata, in grigio (voce `ren-ext`): il carattere `U+2193` era sottile come il testo
 * e allineato a sinistra. Senza descrizione: quello che dice lo dicono le due pastiglie.
 *
 * ⚠️ **È più alta del suo posto, e il gambo si taglia sul bordo di sopra**, come nel mockup: il
 * gambo esce dalla pastiglia di sopra come se le passasse dietro, e la punta si ferma a [TIP_AIR]
 * da quella di sotto (misurato sui suoi pixel, entro un pixel).
 * Si posa dalla punta ([ARROW_TIP], dalla griglia di Material), così il conto regge a qualunque
 * [ARROW_SIZE]. Il gambo lo taglia `clipToBounds`: la pastiglia di sopra è disegnata prima e non
 * copre niente.
 * Non è un disegno nuovo in `res/`: è la stessa icona tagliata, e a zero pixel di scarto vince
 * Material (`Rules.md` § '🖌️ Come entra un disegno').
 */
@Composable
private fun PairArrow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PAIR_GAP)
            .clipToBounds()
    ) {
        Icon(
            imageVector = Glyphs.Material.ArrowDownward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.layout { measurable, constraints ->
                val side = ARROW_SIZE.roundToPx()
                val arrow = measurable.measure(Constraints.fixed(side, side))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val tip = (side * ARROW_TIP).roundToInt()
                    arrow.place(
                        x = (constraints.maxWidth - side) / 2,
                        y = constraints.maxHeight - TIP_AIR.roundToPx() - tip
                    )
                }
            }
        )
    }
}

/**
 * Il posto fra due abbinamenti, alto [BETWEEN], con in mezzo il separatore (`FadedRule`) o lo
 * stacco ([PairSkip]).
 *
 * Il separatore è quello dell'editor (nota dell'utente su `d-filo-uno`), uno per l'app, in
 * `Theme.kt`; il posto e l'aria sono di qui.
 */
@Composable
private fun Between(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(BETWEEN),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * Lo stacco fra il secondo abbinamento e l'ultimo: tre punti al posto del separatore, come nella
 * specifica dell'utente.
 *
 * È `MoreHoriz` alla misura e nella tinta della freccia: i tre punti occupano le stesse sedici
 * unità della V, e lo stacco si legge come un pezzo dell'anteprima. Senza descrizione, come la
 * freccia: i numeri dei nomi dicono già quanti ne mancano.
 */
@Composable
private fun PairSkip() {
    Icon(
        imageVector = Glyphs.Material.MoreHoriz,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(ARROW_SIZE)
    )
}

/**
 * La misura della freccia fra le due pastiglie dell'anteprima, e dello stacco.
 *
 * 20, la misura di Material per un'icona dentro un testo: qui la freccia è fra due righe di nomi,
 * e a 24 sarebbe il pezzo più grosso del dialogo.
 */
private val ARROW_SIZE = 20.dp

/**
 * Lo spazio fra le due pastiglie di un abbinamento, cioè il posto della freccia: venti pixel del
 * mockup.
 */
private val PAIR_GAP = 12.dp

/**
 * L'aria fra la punta della freccia e la pastiglia di sotto: tre pixel del mockup. Tiene la punta
 * staccata dal filo, così si legge come una freccia che indica.
 */
private val TIP_AIR = 2.dp

/**
 * Dove cade la punta di `ArrowDownward` nel suo lato: a 20 unità su 24, perché la griglia di
 * Material lascia quattro unità di margine sotto il disegno.
 */
private const val ARROW_TIP = 20f / 24f

/**
 * Il posto fra due abbinamenti, col separatore o lo stacco in mezzo: cinquantadue pixel del
 * mockup, col filo a metà. Uno solo per le due righe, così i posti fra le coppie sono uguali.
 */
private val BETWEEN = 32.dp

/**
 * Un comando secondario: piccolo, a destra, e sbiadito quando non serve.
 *
 * La stessa forma del 'Ripristina' dei riquadri del riordino, una sola per questa finestra e per
 * quella del nome del salvataggio, che ha gli stessi comandi (voce `scarica-download`: solo
 * testo, senza pastiglia).
 *
 * @param onHold il tocco lungo, o `null` per un comando che non ne ha. Qui il valore di serie è
 *   ammesso: solo 'Data' ha un secondo gesto, e un tocco lungo assente non promette niente. Il
 *   filo di `NamePill` invece non ne ha, perché là un'assenza non si vedrebbe.
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
             * Un bersaglio solo coi due gesti: `combinedClickable` è un nodo, quindi un lettore di
             * schermo annuncia un comando. Col ruolo legge un comando e non una parola; senza
             * `tapRoom()` il bersaglio sarebbe alto poco più della metà dei 48dp (`labelMedium`,
             * 16 di interlinea più 6dp per lato). Il colore è [accentInk]: come testo l'accento
             * pieno non si legge.
             * ⚠️ **`tapRoom()` va dopo il gesto**: il perché è su `TAP_MIN`, in `Talk.kt`.
             */
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onTap,
                // La vibrazione è quella di [withHaptics]: quella di Compose è spenta.
                onLongClick = onHold?.let { withHaptics(it) },
                hapticFeedbackEnabled = false
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
     * Non ha un valore di serie: con `null` di serie una pastiglia è rimasta senza filo senza
     * che nessuno l'avesse deciso. Così 'o tutte o nessuna' lo chiede il compilatore.
     */
    border: BorderStroke,
    modifier: Modifier = Modifier
) {
    /*
     * ⚠️ **Il testo si stringe per non lasciare un carattere da solo** (riscontro del giro della
     * 1.59: un corpo leggermente più piccolo, entro una soglia); se non basta, si va a capo con
     * tutta l'estensione (`BREAK_HERE` in `Names.kt`). L'ellissi no: `TextOverflow` la metterebbe
     * alla fine, sopra l'estensione, che l'utente vuole sempre visibile.
     * La misura viene da `onTextLayout`: quanti caratteri entrano su una riga dipende da quali
     * sono. Si scende di un gradino per volta e mai sotto [NAME_FLOOR], quindi il giro finisce.
     * La chiave è il testo: con un nome nuovo si riparte dalla misura piena.
     */
    var stretta by remember(text) { mutableStateOf(1f) }
    // La scalatura è `shrunk` di `Names.kt`, accanto al misuratore che la applica.
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
             * Nessun tetto di righe: impilate, le pastiglie hanno tutta la larghezza, e un tetto
             * taglierebbe i nomi lunghissimi per cui l'utente ha chiesto questa forma.
             * L'anteprima mostra al massimo tre abbinamenti.
             */
            /*
             * 8dp per lato (riscontro dell'utente sul giro della 1.47: i discendenti erano a
             * filo). Un riempimento simmetrico si vede asimmetrico, perché sopra c'è anche l'aria
             * dell'ascendente; quanta sia dipende dal carattere di sistema, quindi si dà spazio
             * abbastanza al lato stretto invece di pareggiare con un numero. Uguale sui quattro
             * lati, la pastiglia di una riga è alta quanto un contenitore piccolo di Material.
             */
            modifier = Modifier.padding(8.dp)
        )
    }
}

/**
 * Se l'ultima riga contiene un carattere solo, cioè un orfano.
 *
 * Il conto è in unità di codice e non in caratteri visibili: l'estensione contiene i giuntori
 * invisibili di `Names.kt`, quindi la sua riga conta sempre più di uno e non risulta orfana.
 * Una riga sola non ha orfani: senza questa guardia un nome cortissimo si stringerebbe fino al
 * fondo.
 */
private fun orphan(steso: TextLayoutResult): Boolean {
    val ultima = steso.lineCount - 1
    if (ultima <= 0) return false
    return steso.getLineEnd(ultima, visibleEnd = true) - steso.getLineStart(ultima) <= 1
}


package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    var asking by rememberSaveable { mutableStateOf(false) }

    /*
     * ⚠️⚠️ **IL TASTO C'È SOLO SE L'IMPOSTAZIONE È ACCESA, dalla 1.36**, ed è la griglia di
     * sicurezza chiesta dall'utente: il perché per esteso sta su [Settings.extEdit], e in
     * breve è che cambiare l'estensione non converte niente e può far sparire un'immagine
     * dalle viste. Di fabbrica è spenta.
     * ⚠️⚠️ **LE DUE LETTURE SI FANNO QUI E NON ARRIVANO DA FUORI, ed è una scelta contro la
     * convenzione di questo file** (`FileJobDialogs` dichiara di non sapere niente delle
     * impostazioni e si fa passare i campi delle info). La ragione è il numero di posti: le
     * tre schermate che aprono questi dialoghi sono griglia, albero e visualizzatore, e solo
     * l'ultima ha le impostazioni in mano; le altre due dovrebbero farsi passare un booleano
     * dai **loro** chiamanti, cioè quattro firme in più per un valore che si legge in una
     * riga. La stessa strada la fanno già i veli (`Hint.flow`), che nascono in schermate che
     * non hanno lo stato dell'app.
     * ⚠️ **`false` come valore iniziale**: mentre la lettura è in corso il tasto non c'è, che
     * è il verso prudente. Al contrario comparirebbe per un istante anche a chi l'ha spento.
     */
    val extAllowed by produceState(false) {
        SettingsStore.flow(context).collect { value = it.extEdit }
    }
    val warned by produceState(true) { Hint.EXT_WARN.flow(context).collect { value = it } }
    var warning by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
        modifier = Modifier.lowered(),
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
         */
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.pick_rename))
                if (extAllowed) {
                    FilledTonalButton(
                        /*
                         * ⚠️ **Il velo PRIMA del pannellino e non insieme**: l'avviso dice che
                         * cosa comporta la cosa che si sta per fare, e uno che comparisse
                         * sopra il campo già aperto arriverebbe dopo il gesto. Chi lo chiude
                         * trova il pannellino, quindi il tocco non va perso.
                         */
                        onClick = { if (warned) asking = true else warning = true },
                        shape = MaterialTheme.shapes.large,
                        contentPadding = EXT_PAD
                    ) {
                        Text(
                            text = stringResource(R.string.rename_ext),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        },
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
                    for (row in previewOf(listed, clean, first, extension)) {
                        PreviewRow(row)
                    }
                }
            }
        },
        /*
         * ⚠️ **La fila dei tasti è quella di Material**, 'Annulla' e 'Rinomina', dalla `1.34`:
         * il tasto dell'estensione sta sulla riga del titolo (vedi la nota là sopra), e la nota
         * che dichiarava un ordine strano è decaduta insieme al tasto che la rendeva necessaria.
         * ⚠️ **L'ETICHETTA DELL'ALTRO TASTO È LA CORTA, 'Estensione', e la ragione è una
         * misura**: 'Cambia estensione' vuole circa 150dp, e accanto al titolo su un dialogo
         * largo 280 la riga andrebbe a capo. L'utente aveva previsto il caso (*se non ci sta,
         * solo 'Estensione'*).
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

    /*
     * ⚠️⚠️ **È UN VELO IN UNA FINESTRA SUA, e la ragione sta su [HintNotice]**: qui dentro
     * siamo già in un dialogo, e un velo steso sul contenuto coprirebbe la finestrella invece
     * dello schermo. L'utente ha chiesto un avviso *in mezzo allo schermo*.
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
            // ⚠️ Il valore di partenza è quello **corrente**: l'estensione già scelta se c'è,
            // altrimenti quella del primo file, che con una selezione omogenea è quella di
            // tutti. Senza il punto, come chiesto.
            initial = extension ?: listed?.firstOrNull()?.substringAfterLast('.', "").orEmpty(),
            onDismiss = { asking = false },
            onPick = { extension = it; asking = false }
        )
    }
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
private fun ExtensionDialog(initial: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var typed by rememberSaveable { mutableStateOf(initial) }
    val clean = typed.trim().trimStart('.')
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(),
        title = { Text(stringResource(R.string.rename_ext)) },
        text = {
            Column {
                OutlinedTextField(
                    value = typed,
                    // ⚠️ Si filtra mentre si scrive invece di validare dopo: qui dentro va
                    // una parola di tre lettere, e un messaggio d'errore per un carattere
                    // che non doveva entrare costa più della lettera che si è tolta.
                    onValueChange = { t -> typed = t.filter { it.isLetterOrDigit() }.take(12) },
                    label = { Text(stringResource(R.string.rename_ext_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
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

/** Quanto stringe il tasto dell'estensione, che sta in una fila già piena. */
private val EXT_PAD = PaddingValues(horizontal = 12.dp, vertical = 0.dp)

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
         * somigliano comunque. Le due pastiglie diventano così una **contornata** e una
         * **piena**, che è l'abbinamento normale fra un punto di partenza e un risultato.
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
            border = BorderStroke(PILL_EDGE, hairline()),
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
 * Il filo intorno alla pastiglia di partenza.
 *
 * ⚠️ **1dp, cioè il minimo che si vede**: qui il bordo non deve pesare come una cornice, deve
 * solo dire dove finisce la pastiglia. Il perché del bordo sta in [PreviewRow].
 */
private val PILL_EDGE = 1.dp

@Composable
private fun NamePill(
    text: AnnotatedString,
    back: Color,
    front: Color,
    weight: FontWeight,
    modifier: Modifier = Modifier,
    /** Il filo intorno, che ha la sola pastiglia di partenza. Vedi la nota in [PreviewRow]. */
    border: BorderStroke? = null
) {
    Surface(
        color = back,
        shape = MaterialTheme.shapes.small,
        border = border,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = front,
            fontWeight = weight,
            /*
             * ⚠️ **Nessun tetto di righe dalla 1.30**, e prima erano due: impilate le
             * pastiglie hanno tutta la larghezza, quindi un nome ci sta quasi sempre in una
             * riga o due, e un tetto taglierebbe proprio i nomi lunghissimi per cui l'utente
             * ha chiesto questa forma. L'anteprima mostra al massimo cinque abbinamenti: non
             * può crescere senza limite.
             */
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

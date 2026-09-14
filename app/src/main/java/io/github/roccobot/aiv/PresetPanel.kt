package io.github.roccobot.aiv

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Il corpo del modulo **Stili**: quelli di casa, quelli propri, e il comando che salva.
 *
 * ⚠️⚠️ **DALLA `2.50` È UN MODULO E NON PIÙ UN PANNELLO, ED È SUA ISTRUZIONE** (campo libero del
 * giro della `2.40`, punto 2: *il modulo Preset, che può restare senza titolo come gli altri, deve
 * vivere nello stesso spazio della bottomsheet, quindi niente bottomsheet nuova a tutta pagina. Si
 * scorre nella solita area in basso*). Fino alla `2.40` era una scheda che si apriva sopra la
 * scheda dell'editor, cioè una superficie in più davanti all'immagine su cui si sta lavorando.
 * - ⚠️⚠️ **LA RAGIONE CHE LO TENEVA FUORI ERA L'ALTEZZA, E ADESSO NON SI PAGA**: dalla `2.33` la
 *   scheda è alta quanto il **modulo più alto**, quindi un elenco che cresce con quello che si
 *   salva l'avrebbe alzata per tutti e sette gli altri. Qui l'elenco **scorre dentro l'altezza
 *   comune** invece di dettarla, e per questo il suo corpo resta fuori dalla misura (vedi
 *   `SteadyBody`, che non lo conta).
 * - ⚠️ **Senza titolo, come gli altri sei**: il gettone acceso dice già dove si è, e un titolo
 *   dentro la scheda costerebbe una riga a chi guarda l'immagine.
 *
 * ⚠️⚠️ **I DUE GESTI SONO SUOI, E SONO DUE COSE DIVERSE** (stesso campo libero, punto 3: *tocco
 * normale = modifica assoluta; tocco prolungato = modifica additiva*): il tocco porta l'immagine
 * **dove il preset dice**, il tocco lungo tocca i soli moduli che quel preset dichiara e lascia
 * stare gli altri. Il conto delle due strade vive su [Preset.applyTo] e [Preset.addTo].
 * - ⚠️ **Il tocco lungo si annuncia**, con la vibrazione di casa e con l'etichetta che un lettore
 *   di schermo legge: un gesto che non si vede e non si sente non lo scopre nessuno.
 *
 * ⚠️ **L'immagine cambia mentre si scorre l'elenco**, ed è l'altra metà della sua richiesta (*man
 * mano che si tocca un predefinito o un altro, l'immagine deve aggiornarsi in tempo reale*): il
 * modulo vive nella scheda, quindi il palco resta in scena e il confronto si fa senza chiudere
 * niente.
 */
@Composable
fun PresetBody(
    /** Quello che si ha davanti, cioè il candidato al salvataggio. */
    look: Look,
    /**
     * Quanto è alta la scheda, cioè l'altezza comune misurata sugli altri moduli.
     *
     * ⚠️ **Arriva da fuori e non si misura qui**: è il modulo più alto a dettarla, e questo corpo
     * è l'unico che si adatta invece di contribuire.
     */
    height: Dp,
    /** Applica un preset: `add` vero è il tocco lungo, cioè l'additiva. */
    onPick: (Preset, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    /*
     * ⚠️ **L'elenco si legge una volta e si riscrive a ogni gesto che lo cambia**: il file lo
     * scrivono questo corpo e la pagina delle impostazioni, e rileggerlo a ogni ricomposizione
     * vorrebbe dire aprire un file per ogni fotogramma di un'animazione.
     */
    var house by remember { mutableStateOf(Presets.house(context)) }
    var mine by remember { mutableStateOf(Presets.mine(context)) }
    var naming by remember { mutableStateOf(false) }

    /*
     * ⚠️ **Il comando è spento quando non c'è niente da salvare**, e la domanda se la fa
     * [Preset.of]: quella funzione tiene i soli cinque moduli di colore, quindi il [Look] che ne
     * esce è a riposo esattamente quando un preset preso adesso non direbbe niente. Un secondo
     * conto scritto qui direbbe la stessa cosa con altre parole, e divergerebbe il giorno che i
     * moduli sono sei.
     */
    val worth = !Preset.of("", look).look.idle

    Column(modifier = modifier.fillMaxWidth().height(if (height > 0.dp) height else PRESET_FALL)) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(PRESET_GAP)
        ) {
            /*
             * ⚠️⚠️ **'STILI AIV' VIENE PRIMA E 'STILI PERSONALI' DOPO, ED È SUA ISTRUZIONE** (nota
             * sulla voce `preset-salva`: *'Di serie' ... deve restare, ma diventa 'Stili AIV', a
             * sottolineare che fanno parte dell'app, mentre quelli salvati, in basso, diventeranno
             * 'Stili personali'*). Fino alla `2.40` i propri stavano in cima, cioè l'elenco si
             * apriva su un gruppo che al primo avvio è vuoto.
             */
            item { PresetGroup(stringResource(R.string.look_preset_house)) }
            items(house, key = { "h-" + it.key }) { p ->
                PresetRow(preset = p, onPick = onPick)
            }
            if (mine.isNotEmpty()) {
                item { PresetGroup(stringResource(R.string.look_preset_mine)) }
                items(mine, key = { "m-" + it.key }) { p ->
                    PresetRow(preset = p, onPick = onPick)
                }
            }
        }
        /*
         * ⚠️ **Il comando resta FUORI dall'elenco che scorre**, cioè in fondo al corpo e non in
         * fondo alle righe: con venti stili di casa, dentro la lista si raggiungerebbe scorrendo
         * fino in fondo ogni volta. ⚠️ **E non costa altezza a nessuno**, perché questo corpo la
         * sua altezza la riceve invece di dettarla.
         */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { naming = true }, enabled = worth) {
                Text(stringResource(R.string.look_preset_save))
            }
        }
    }

    if (naming) {
        PresetNameDialog(
            title = stringResource(R.string.look_preset_save),
            initial = "",
            onDismiss = { naming = false },
            onSave = { nome ->
                mine = Presets.save(context, nome, look)
                naming = false
            }
        )
    }
}

/**
 * Il titolino che divide gli stili dell'app da quelli propri.
 *
 * ⚠️ **C'è anche quando i propri non ci sono**, cioè al primo avvio: senza, i venti di casa si
 * leggerebbero come 'gli stili' e basta, e il comando in fondo non avrebbe niente che lo spieghi.
 */
@Composable
private fun PresetGroup(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = PRESET_GAP)
    )
}

/**
 * Una riga dell'elenco: il nome, il tocco che applica e il tocco lungo che aggiunge.
 *
 * ⚠️ **Il bersaglio del tocco è la riga intera e non il nome**, come ogni altra riga di quest'app:
 * un testo alto una riga è un bersaglio che si manca.
 * ⚠️⚠️ **QUI NON SI CANCELLA NIENTE, DALLA `2.50`**: riordinare, rinominare e cancellare vivono
 * nella pagina 'Stili di modifica' delle impostazioni, che è quello che ha chiesto lui, e un
 * comando che elimina a un tocco da un elenco che si tocca per provare sarebbe la via più corta
 * per perdere uno stile mentre lo si sta confrontando.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetRow(preset: Preset, onPick: (Preset, Boolean) -> Unit) {
    val add = withHaptics { onPick(preset, true) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BOX_SHAPE)
            .combinedClickable(
                onClick = { onPick(preset, false) },
                onLongClick = add,
                onLongClickLabel = stringResource(R.string.look_preset_add)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = preset.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * La finestra che chiede il nome di uno stile: quello nuovo, o quello da rinominare.
 *
 * ⚠️ **È una modale vera**, quindi porta tutte e due le righe ([Modifier.lowered] con `null` e
 * `properties = loweredWindow(null)`): esiste per raccogliere un input scritto, che è il criterio
 * di '👆 Che cosa fa il tocco FUORI da una finestra'. Una a metà si chiude toccando sotto il
 * pannello, e il nome scritto si perde.
 * ⚠️ **Una sola finestra per i due gesti, dalla `2.50`**: salvare e rinominare chiedono la stessa
 * cosa, e due finestre vorrebbero dire due campi da tenere allineati (il taglio del nome, lo
 * scorrimento, il tasto spento a campo vuoto).
 */
@Composable
fun PresetNameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    val clean = text.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(null),
        properties = loweredWindow(null),
        title = { Text(title) },
        text = {
            // ⚠️ Lo scorrimento serve al tetto della `1.62`, come in ogni altra finestra con un
            // campo: il perché per esteso vive nel pannellino dell'estensione, in `RenameDialog.kt`.
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = text,
                    // ⚠️ Il taglio si fa QUI e non al salvataggio: un nome che si accorcia dopo
                    // aver toccato 'Salva' comparirebbe nell'elenco diverso da come è stato
                    // scritto, e nessuno saprebbe perché.
                    onValueChange = { text = it.take(Preset.NAME_MAX) },
                    label = { Text(stringResource(R.string.look_preset_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = BOX_SHAPE,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(clean) },
                enabled = clean.isNotEmpty()
            ) { Text(stringResource(R.string.editor_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Quanto è alto il corpo degli stili finché l'altezza comune non è misurata.
 *
 * ⚠️ **Serve a non chiedere a una lista che scorre di essere alta quanto vuole**: `LazyColumn` va
 * in errore se la misura in altezza è infinita, e questo corpo la sua altezza la riceve. Al primo
 * fotogramma di una schermata il numero non c'è ancora, e questo è il valore che la tiene in piedi
 * fino al secondo.
 */
private val PRESET_FALL = 220.dp

/** L'aria fra due righe dell'elenco, e sopra un titolino di gruppo. */
private val PRESET_GAP = 2.dp

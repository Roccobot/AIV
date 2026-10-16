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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
 * Il corpo del modulo **Stili**: quelli di casa e quelli propri, in un elenco solo che scorre.
 *
 * È un modulo nella scheda dell'editor, come gli altri, e non una scheda in più sopra l'immagine
 * (istruzione dell'utente, 2.50): il palco resta in scena e l'immagine cambia mentre si provano
 * gli stili.
 * ⚠️ **L'elenco scorre dentro l'altezza comune, senza dettarla**: la scheda è alta quanto il
 * modulo più alto, e un elenco che cresce con quello che si salva la alzerebbe per tutti. Per
 * questo il suo corpo è fuori dalla misura (vedi `SteadyBody`). Senza titolo, come gli altri:
 * il gettone acceso dice già dove si è.
 *
 * I due gesti sono dell'utente: il tocco porta l'immagine dove lo stile dice, il tocco lungo
 * somma lo stile a quello che c'è (risposta `B1`, 4.97), così più stili si combinano. Il conto è
 * su [Preset.applyTo] e [Preset.addTo]. Il tocco lungo si annuncia con la vibrazione e con
 * l'etichetta per il lettore di schermo, o non lo scopre nessuno.
 *
 * Il comando che salva è sulla barra delle icone ([PresetSaveButton]): il corpo è solo l'elenco,
 * e gli stili salvati li passa il chiamante in [mine].
 */
@Composable
fun PresetBody(
    /**
     * Quanto è alta la scheda, cioè l'altezza comune misurata sugli altri moduli.
     *
     * Arriva da fuori: la detta il modulo più alto, e questo corpo è l'unico che vi si adatta.
     */
    height: Dp,
    /**
     * Gli stili salvati, che vivono nella schermata perché il comando che li crea vive sulla barra.
     *
     * Non si rileggono qui dal file: chi scrive e chi mostra sono due pezzi diversi, quindi lo
     * stato vive sopra tutti e due, o dopo un salvataggio l'elenco sarebbe vecchio fino alla
     * riapertura dell'editor.
     */
    mine: List<Preset>,
    /** Applica uno stile: `add` vero è il tocco lungo, cioè la somma. */
    onPick: (Preset, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    /*
     * Quelli di casa si leggono una volta: li cambia solo la pagina delle impostazioni, e al
     * ritorno questo corpo si compone da capo. Rileggerli a ogni ricomposizione aprirebbe un
     * file a ogni fotogramma di un'animazione.
     */
    val house = remember { Presets.house(context) }

    Column(modifier = modifier.fillMaxWidth().height(if (height > 0.dp) height else PRESET_FALL)) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(PRESET_GAP)
        ) {
            /*
             * Il titolo 'Stili AIV' qui non si scrive (scelta dell'utente, 2.52: i pixel verticali
             * sono preziosi), ma il nome resta, e la pagina delle impostazioni lo usa. C'è un
             * separatore solo, quello degli stili salvati, che vanno sotto quelli dell'app.
             */
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
    }
}

/**
 * Il comando che salva quello che si ha davanti come stile nuovo, con la sua finestra del nome.
 *
 * È sulla barra delle icone, in basso a sinistra (richiesta dell'utente, 2.52), e quindi è
 * un'icona: quella barra è tutta di icone, e una scritta in mezzo la farebbe leggere in due modi.
 * Il nome resta come descrizione parlata, per il lettore di schermo e per il banco. Il glifo è
 * quello di Material arrotondato (voce `salva-glifo`), in `res/drawable/ic_preset_save.xml`.
 *
 * È spento quando non c'è niente da salvare, e lo decide [Preset.of]: tiene i soli sei moduli
 * dello stile (Luce, Colore, HSL, Dettaglio, Curve, Effetti), quindi il [Look] che ne risulta è a
 * riposo esattamente quando uno stile preso adesso non direbbe niente. Un secondo conto scritto
 * qui potrebbe divergere dal primo.
 */
@Composable
fun PresetSaveButton(
    /** Quello che si ha davanti, cioè il candidato al salvataggio. */
    look: Look,
    /** Falso mentre l'editor è occupato, come per gli altri comandi della barra. */
    enabled: Boolean,
    /** L'elenco nuovo dei propri, che la schermata tiene per il corpo del modulo. */
    onSaved: (List<Preset>) -> Unit
) {
    val context = LocalContext.current
    var naming by remember { mutableStateOf(false) }
    val worth = !Preset.of("", look).look.idle

    IconButton(onClick = { naming = true }, enabled = enabled && worth) {
        Icon(Glyphs.PresetSave, stringResource(R.string.look_preset_save))
    }

    if (naming) {
        PresetNameDialog(
            title = stringResource(R.string.look_preset_save),
            initial = "",
            onDismiss = { naming = false },
            onSave = { nome ->
                onSaved(Presets.save(context, nome, look))
                naming = false
            }
        )
    }
}

/**
 * Il titolino che divide gli stili dell'app da quelli propri.
 *
 * C'è solo se c'è almeno uno stile salvato: un separatore senza niente sotto annuncerebbe una
 * parte che non esiste.
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
 * Il bersaglio del tocco è la riga intera, come in ogni altra riga dell'app: un testo alto una
 * riga è un bersaglio che si manca.
 * Qui non si cancella niente: riordinare, rinominare e cancellare sono nella pagina 'Stili di
 * modifica' delle impostazioni (richiesta dell'utente), perché in un elenco che si tocca per
 * provare un comando che elimina farebbe perdere uno stile mentre lo si confronta.
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
                hapticFeedbackEnabled = false,
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
 * ⚠️ **È una modale vera**, quindi ha tutte e due le righe ([Modifier.lowered] con `null` e
 * `properties = loweredWindow(null)`): raccoglie un input scritto (`Rules.md` § '👆 Che cosa fa
 * il tocco FUORI da una finestra'). Con una sola delle due si chiude toccando sotto il pannello,
 * e il nome scritto si perde.
 * Una sola finestra per salvare e rinominare, che chiedono la stessa cosa: due finestre
 * vorrebbero dire due campi da tenere allineati (il taglio del nome, lo scorrimento, il tasto
 * spento a campo vuoto).
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
            // Lo scorrimento c'è in ogni finestra con un campo: il perché è in `RenameDialog.kt`.
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = text,
                    // Il taglio si fa mentre si scrive: un nome accorciato dopo 'Salva'
                    // comparirebbe nell'elenco diverso da come è stato scritto.
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
 * ⚠️ `LazyColumn` va in errore se la misura in altezza è infinita, e al primo fotogramma di una
 * schermata l'altezza comune non c'è ancora: questo valore la sostituisce fino al secondo.
 */
private val PRESET_FALL = 220.dp

/** L'aria fra due righe dell'elenco, e sopra un titolino di gruppo. */
private val PRESET_GAP = 2.dp

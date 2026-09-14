package io.github.roccobot.aiv

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp

/**
 * Il pannello dei preset: i propri, i venti di casa, e il tasto che salva quello che si ha davanti.
 *
 * ⚠️⚠️ **NON È UN OTTAVO MODULO, ED È UNA SCELTA MISURATA**: dalla `2.33` la scheda dell'editor è
 * alta quanto il **modulo più alto** ([SteadyBody]), quindi un modulo fatto di un elenco che cresce
 * con quello che l'utente salva alzerebbe la scheda di tutti e sette gli altri, cioè accorcerebbe
 * il palco anche a chi i preset non li usa. Il pannello invece si apre, si usa e se ne va.
 * - ⚠️ **Il tasto vive accanto ad 'Auto'**, che è il suo parente stretto: tutti e due scrivono nei
 *   cursori invece di dipingere, quindi quello che ne esce è un passo della storia come un altro.
 *
 * ⚠️⚠️ **IL TOCCO APPLICA E IL PANNELLO RESTA APERTO**: un preset si sceglie **confrontando**, e
 * un pannello che si chiudesse a ogni tocco costringerebbe a riaprirlo per provare il prossimo.
 * L'immagine si vede sopra la scheda, quindi il confronto si fa senza toccare altro, e per uscire
 * ci sono il tocco fuori e la crocetta, come in ogni scheda di quest'app.
 *
 * ⚠️ **Non c'è nessun segno su quale sia in scena**, e non è una dimenticanza: applicato un preset,
 * chi muove un cursore se ne allontana subito, e un segno che resta acceso direbbe una cosa falsa
 * un istante dopo. Quello che si vede è l'immagine.
 */
@Composable
fun PresetSheet(
    /** Quello che si ha davanti, cioè il candidato al salvataggio. */
    look: Look,
    /** Che cosa fare del preset scelto: lo applica e lo mette nella storia dei passi. */
    onPick: (Preset) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    /*
     * ⚠️ **I propri vivono in uno stato e quelli di casa no**: i primi si salvano e si cancellano
     * di qui, quindi l'elenco cambia sotto gli occhi; i secondi sono una costante del programma e
     * non hanno niente da ricordare. ⚠️ **Si legge una volta sola all'apertura**: il file lo
     * scrive questo pannello e nessun altro, quindi rileggerlo a ogni ricomposizione vorrebbe dire
     * aprire un file per ogni fotogramma di un'animazione.
     */
    var mine by remember { mutableStateOf(Presets.mine(context)) }
    var naming by remember { mutableStateOf(false) }

    /*
     * ⚠️ **Il tasto è spento quando non c'è niente da salvare**, e la domanda se la fa [Preset.of]:
     * quella funzione tiene i soli cinque moduli di colore, quindi il [Look] che ne esce è a riposo
     * esattamente quando un preset preso adesso non direbbe niente. Un secondo conto scritto qui
     * direbbe la stessa cosa con altre parole, e divergerebbe il giorno che i moduli sono sei.
     */
    val worth = !Preset.of("", look).look.idle

    Sheet(
        title = stringResource(R.string.look_presets),
        onDismiss = onDismiss,
        foot = {
            TextButton(onClick = { naming = true }, enabled = worth) {
                Text(stringResource(R.string.look_preset_save))
            }
        }
    ) {
        if (mine.isNotEmpty()) {
            PresetGroup(stringResource(R.string.look_preset_mine))
            mine.forEach { p ->
                PresetRow(
                    preset = p,
                    onPick = { onPick(p) },
                    /*
                     * ⚠️⚠️ **SI CANCELLA CON UN'OFFERTA DI RIMETTERLO E NON CON UNA CONFERMA**, ed
                     * è il criterio di casa applicato: una conferma protegge quello che si
                     * perderebbe, e qui non si perde niente finché la notifica è in scena. In più
                     * è la stessa voce con cui l'app parla di ogni altra eliminazione.
                     * ⚠️ **Rimetterlo è salvarlo di nuovo**, cioè la stessa strada dell'andata: un
                     * secondo percorso di ripristino sarebbe un secondo modo di scrivere lo stesso
                     * file.
                     */
                    onRemove = {
                        mine = Presets.remove(context, p.name)
                        Notices.offer(
                            // ⚠️ La frase non nomina il preset, e non è una perdita: la riga porta
                            // 'Annulla', quindi quello che si è appena toccato è l'unica cosa di
                            // cui possa parlare. Un nome dentro un testo vuole un segnaposto in
                            // ventotto lingue, e la prima a sbagliarlo sarebbe quella che nessuno
                            // rilegge.
                            text = context.getString(R.string.look_preset_gone),
                            action = context.getString(R.string.pick_undo)
                        ) { mine = Presets.save(context, p.name, p.look) }
                    }
                )
            }
        }
        PresetGroup(stringResource(R.string.look_preset_house))
        HOUSE.forEach { p -> PresetRow(preset = p, onPick = { onPick(p) }, onRemove = null) }
    }

    if (naming) {
        PresetNameDialog(
            onDismiss = { naming = false },
            onSave = { nome ->
                mine = Presets.save(context, nome, look)
                naming = false
            }
        )
    }
}

/**
 * Il titolino che divide i propri da quelli di casa.
 *
 * ⚠️ **C'è anche quando i propri non ci sono**, cioè al primo avvio: senza, i venti di casa si
 * leggerebbero come 'i preset' e basta, e il tasto in fondo non avrebbe niente che lo spieghi.
 */
@Composable
private fun PresetGroup(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * Una riga dell'elenco: il nome, e per i propri il comando che lo toglie.
 *
 * ⚠️ **Il bersaglio del tocco è la riga intera e non il nome**, come ogni altra riga di quest'app:
 * un testo alto una riga è un bersaglio che si manca.
 */
@Composable
private fun PresetRow(preset: Preset, onPick: () -> Unit, onRemove: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BOX_SHAPE)
            .clickable(onClick = onPick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = preset.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        /*
         * ⚠️ **Un'icona e non un tocco lungo**: il tocco lungo non si annuncia e non si vede,
         * quindi chi ha salvato un preset per sbaglio non avrebbe niente da cui capire come
         * toglierlo. ⚠️ **E il glifo è quello di casa dell'eliminazione**, perché lo stesso gesto
         * non può avere due segni.
         */
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(Glyphs.PickDelete, stringResource(R.string.look_preset_remove))
            }
        }
    }
}

/**
 * La finestra che chiede il nome di un preset nuovo.
 *
 * ⚠️ **È una modale vera**, quindi porta tutte e due le righe ([Modifier.lowered] con `null` e
 * `properties = loweredWindow(null)`): esiste per raccogliere un input scritto, che è il criterio
 * di '👆 Che cosa fa il tocco FUORI da una finestra'. Una a metà si chiude toccando sotto il
 * pannello, e il nome scritto si perde.
 */
@Composable
private fun PresetNameDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val clean = text.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(null),
        properties = loweredWindow(null),
        title = { Text(stringResource(R.string.look_preset_save)) },
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

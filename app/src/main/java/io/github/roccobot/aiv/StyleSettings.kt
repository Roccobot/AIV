package io.github.roccobot.aiv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * La pagina 'Stili di modifica': l'elenco degli stili, riordinabile, rinominabile e sfoltibile.
 *
 * ⚠️⚠️ **NASCE NELLA `2.50` ED È SUA ISTRUZIONE ALLA LETTERA** (nota sulla voce `preset-salva` del
 * giro della `2.40`: *servirà una nuova sezione delle Impostazioni (visibile a chi ha l'editor
 * completo): 'Stili di modifica', in cui si possono riordinare, rinominare e cancellare a piacere
 * sia i predefiniti di fabbrica che quelli creati dall'utente*). Fino alla `2.40` un preset di
 * casa non si poteva toccare e uno proprio si poteva solo cancellare, dall'elenco stesso.
 *
 * ⚠️⚠️ **I DUE GRUPPI SI RIORDINANO SEPARATAMENTE, E NON È UNA SEMPLIFICAZIONE**: l'elenco vero
 * ha gli stili dell'app sopra e i propri sotto (sua istruzione: *quelli salvati, in basso*),
 * quindi un ordine unico permetterebbe di infilare un proprio in mezzo a quelli di casa, cioè di
 * chiedere una cosa che l'elenco non sa mostrare.
 *
 * ⚠️ **Un preset di casa cancellato non sparisce dall'app**: l'archivio ne tiene la chiave fra i
 * nascosti, perché i suoi valori vivono nel programma. 'Ripristina' porta via quell'elenco insieme
 * a tutto il resto, e per questo il comando è uno e non due.
 */
@Composable
fun StyleSettings(
    /** Il guscio che scorre, per il trascinamento che arriva al bordo. Vedi [Reorderable]. */
    scroll: ScrollState
) {
    val context = LocalContext.current
    var house by remember { mutableStateOf(Presets.house(context)) }
    var mine by remember { mutableStateOf(Presets.mine(context)) }
    var renaming by remember { mutableStateOf<Preset?>(null) }
    var asking by remember { mutableStateOf(false) }

    /*
     * ⚠️ **Si rilegge dal disco invece di ritoccare la lista in mano**: ogni comando passa da
     * [Presets], che scrive il file e applica le sue regole (un nome doppio che si scarta, una
     * chiave che non esiste più); ricostruendo qui l'elenco nuovo ci sarebbero due idee di che
     * cosa sia successo, e la prima a divergere sarebbe questa.
     */
    fun rileggi() {
        house = Presets.house(context)
        mine = Presets.mine(context)
    }

    fun togli(p: Preset) {
        val dove = Presets.remove(context, p)
        rileggi()
        Notices.offer(
            text = context.getString(R.string.look_preset_gone),
            action = context.getString(R.string.pick_undo)
        ) {
            Presets.restore(context, p, dove)
            rileggi()
        }
    }

    /*
     * ⚠️⚠️ **IL CONTRATTO SI RICORDA, o si registra di nuovo a ogni ricomposizione**: è la stessa
     * trappola scritta in `ConvertDialog`, e qui il tipo non cambia mai, quindi la chiave è
     * l'assenza di chiavi.
     */
    val scrittura = remember { ActivityResultContracts.CreateDocument(STYLE_MIME) }
    val esporta = rememberLauncherForActivityResult(scrittura) { dove ->
        if (dove == null) return@rememberLauncherForActivityResult
        val fatto = runCatching {
            context.contentResolver.openOutputStream(dove)?.use { flusso ->
                flusso.write(Presets.export(context).toByteArray())
            } != null
        }.getOrDefault(false)
        Notices.say(
            context.getString(
                if (fatto) R.string.settings_styles_saved else R.string.toast_save_failed
            )
        )
    }
    val lettura = remember { ActivityResultContracts.OpenDocument() }
    val importa = rememberLauncherForActivityResult(lettura) { da ->
        if (da == null) return@rememberLauncherForActivityResult
        val testo = runCatching {
            context.contentResolver.openInputStream(da)?.use { it.readBytes().decodeToString() }
        }.getOrNull()
        val fatto = testo != null && Presets.load(context, testo)
        rileggi()
        Notices.say(
            context.getString(
                if (fatto) R.string.settings_styles_loaded else R.string.settings_styles_bad
            )
        )
    }

    StyleGroup(stringResource(R.string.look_preset_house))
    Reorderable(
        items = house,
        scroll = scroll,
        onMove = { da, a ->
            Presets.move(context, house[da], da, a)
            rileggi()
        }
    ) { p, _ ->
        StyleRow(preset = p, onRename = { renaming = p }, onRemove = { togli(p) })
    }

    StyleGroup(stringResource(R.string.look_preset_mine))
    if (mine.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_styles_none),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp)
        )
    } else {
        Reorderable(
            items = mine,
            scroll = scroll,
            onMove = { da, a ->
                Presets.move(context, mine[da], da, a)
                rileggi()
            }
        ) { p, _ ->
            StyleRow(preset = p, onRename = { renaming = p }, onRemove = { togli(p) })
        }
    }

    /*
     * ⚠️ **I tre comandi vivono in fondo e non sopra l'elenco**: sono quello che si fa una volta
     * ogni tanto, mentre l'elenco è quello che si guarda. ⚠️ **'Ripristina' è il primo dei tre e
     * non l'ultimo**, perché è l'unico che toglie qualcosa: metterlo accanto a 'Importa' vorrebbe
     * dire due comandi adiacenti di cui uno cancella.
     */
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = { asking = true }) {
            Text(stringResource(R.string.settings_styles_reset))
        }
        TextButton(onClick = { esporta.launch(STYLE_FILE) }) {
            Text(stringResource(R.string.settings_styles_export))
        }
        TextButton(onClick = { importa.launch(arrayOf(STYLE_MIME)) }) {
            Text(stringResource(R.string.settings_styles_import))
        }
    }

    val chi = renaming
    if (chi != null) {
        PresetNameDialog(
            title = stringResource(R.string.pick_rename),
            initial = chi.name,
            onDismiss = { renaming = null },
            onSave = { nome ->
                Presets.rename(context, chi, nome)
                rileggi()
                renaming = null
            }
        )
    }

    /*
     * ⚠️⚠️ **QUESTO CHIEDE CONFERMA, AL CONTRARIO DELLA CANCELLAZIONE DI UNO STILE**, e il
     * criterio è quello di casa: una conferma protegge quello che si perderebbe, e qui si
     * perdono **tutti** gli stili propri in un colpo, cioè una cosa che nessuna notifica da tre
     * secondi potrebbe rimettere. Il testo è suo alla lettera.
     * ⚠️ **Non è una modale vera**: non raccoglie niente di scritto, quindi il tocco fuori vale
     * 'Annulla', che è l'esito sicuro (§ '👆 Che cosa fa il tocco FUORI da una finestra').
     */
    if (asking) {
        AlertDialog(
            onDismissRequest = { asking = false },
            modifier = Modifier.lowered { asking = false },
            properties = loweredWindow { asking = false },
            title = { Text(stringResource(R.string.settings_styles_reset)) },
            text = { Text(stringResource(R.string.settings_styles_reset_ask)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        Presets.reset(context)
                        rileggi()
                        asking = false
                    }
                ) { Text(stringResource(R.string.settings_styles_reset)) }
            },
            dismissButton = {
                TextButton(onClick = { asking = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

/** Il titolino che divide gli stili dell'app da quelli propri, come nell'elenco vero. */
@Composable
private fun StyleGroup(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
    )
}

/**
 * Una riga dell'elenco: il nome e i due comandi che lo toccano.
 *
 * ⚠️ **Il rientro a destra è il posto della manopola del trascinamento**, che [Reorderable]
 * disegna sopra la riga: senza, un nome lungo le finirebbe sotto. È la stessa misura delle righe
 * dei campi info.
 * ⚠️ **Due icone e non un tocco lungo**: qui si viene per **gestire** l'elenco, quindi i comandi
 * si vedono; il tocco lungo è il gesto dell'elenco vero, dove serve a comporre uno stile.
 */
@Composable
private fun StyleRow(preset: Preset, onRename: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = STYLE_HANDLE_ROOM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = preset.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRename) {
            Icon(Glyphs.TextCursor, stringResource(R.string.pick_rename))
        }
        IconButton(onClick = onRemove) {
            Icon(Glyphs.PickDelete, stringResource(R.string.look_preset_remove))
        }
    }
}

/** Quanto spazio una riga lascia alla manopola del trascinamento: vedi [HANDLE]. */
private val STYLE_HANDLE_ROOM = HANDLE + 4.dp

/**
 * Che cosa si scrive e che cosa si legge quando gli stili escono dall'app.
 *
 * ⚠️ **JSON e non XML**, fra i due che lui ha nominato (*si potrà salvare un XML o JSON*): è già
 * il formato dell'archivio, quindi il file esportato è esattamente quello che vive su disco e non
 * c'è una seconda scrittura da tenere allineata alla prima.
 */
private const val STYLE_MIME = "application/json"

/** Il nome che il selettore propone quando si esporta. */
private const val STYLE_FILE = "aiv-styles.json"

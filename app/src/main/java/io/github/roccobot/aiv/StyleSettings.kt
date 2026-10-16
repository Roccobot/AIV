package io.github.roccobot.aiv

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * La pagina 'Stili di modifica': l'elenco degli stili, riordinabile, rinominabile e sfoltibile.
 *
 * Vale per gli stili dell'app e per quelli salvati (istruzione dell'utente, voce `preset-salva`).
 *
 * ⚠️ **I due gruppi si riordinano separatamente**: l'elenco del modulo Stili ha quelli dell'app
 * sopra e quelli salvati sotto, e un ordine unico permetterebbe di chiedere una mescolanza che
 * l'elenco non sa mostrare.
 *
 * Uno stile dell'app cancellato non sparisce dal programma, che ne contiene i valori: l'archivio
 * ne tiene la chiave fra i nascosti. 'Ripristina' svuota anche quell'elenco, per questo il comando
 * è uno solo.
 */
@Composable
fun StyleSettings(
    /** Il guscio che scorre, per il trascinamento che arriva al bordo. Vedi [Reorderable]. */
    scroll: ScrollState
) {
    val context = LocalContext.current
    // Le frasi degli avvisi da `LocalResources`, come nella griglia: il perché è là.
    val res = LocalResources.current
    var house by remember { mutableStateOf(Presets.house(context)) }
    var mine by remember { mutableStateOf(Presets.mine(context)) }
    var renaming by remember { mutableStateOf<Preset?>(null) }
    var asking by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf<Preset?>(null) }

    /*
     * Dopo ogni comando si rilegge dal disco: [Presets] scrive il file e applica le sue regole
     * (un nome doppio che si scarta, una chiave che non esiste più), e un elenco ricostruito qui
     * potrebbe divergere dal suo.
     */
    fun rileggi() {
        house = Presets.house(context)
        mine = Presets.mine(context)
    }

    fun togli(p: Preset) {
        val dove = Presets.remove(context, p)
        rileggi()
        Notices.offer(
            text = res.getString(R.string.look_preset_gone),
            action = res.getString(R.string.pick_undo)
        ) {
            Presets.restore(context, p, dove)
            rileggi()
        }
    }

    // ⚠️ Il contratto si ricorda, o si registra di nuovo a ogni ricomposizione (la trappola è
    // scritta in `ConvertDialog`); il tipo non cambia mai, quindi senza chiavi.
    val scrittura = remember { ActivityResultContracts.CreateDocument(STYLE_MIME) }
    /*
     * ⚠️ **Le letture e le scritture girano su `Dispatchers.IO`**: il documento arriva dal
     * selettore di sistema e può essere in un cloud, e sul thread principale fermerebbe l'app per
     * qualche secondo. Girano nell'ambito dei lavori sui file ([FileWork]), come la filigrana:
     * lasciata la pagina, l'avviso dell'esito arriva lo stesso.
     */
    fun scrivi(dove: Uri, testo: () -> String, riuscito: Int) {
        FileWork.scope.launch {
            val fatto = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(dove)?.use { flusso ->
                        flusso.write(testo().toByteArray())
                    } != null
                }.getOrDefault(false)
            }
            Notices.say(res.getString(if (fatto) riuscito else R.string.toast_save_failed))
        }
    }
    val esporta = rememberLauncherForActivityResult(scrittura) { dove ->
        if (dove == null) return@rememberLauncherForActivityResult
        scrivi(dove, { Presets.export(context) }, R.string.settings_styles_saved)
    }
    // Lo stile da esportare si ricorda in [sending]: il selettore di sistema torna con il solo
    // indirizzo del file. Il contratto è quello dell'archivio, cambia solo il testo scritto.
    val esportaUno = rememberLauncherForActivityResult(scrittura) { dove ->
        val chi = sending
        sending = null
        if (dove == null || chi == null) return@rememberLauncherForActivityResult
        scrivi(dove, { Presets.exportOne(chi) }, R.string.settings_style_saved)
    }
    fun mandaFuori(p: Preset) {
        sending = p
        esportaUno.launch(ImageActions.safeName(p.name) + ONE_STYLE_SUFFIX)
    }
    val lettura = remember { ActivityResultContracts.OpenDocument() }
    val importa = rememberLauncherForActivityResult(lettura) { da ->
        if (da == null) return@rememberLauncherForActivityResult
        FileWork.scope.launch {
            val esito = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(da)?.use { it.readBytes().decodeToString() }
                }.getOrNull()?.let { Presets.load(context, it) }
            }
            rileggi()
            Notices.say(
                res.getString(
                    when (esito) {
                        Presets.Loaded.BOOK -> R.string.settings_styles_loaded
                        Presets.Loaded.ONE -> R.string.settings_style_loaded
                        null -> R.string.settings_styles_bad
                    }
                )
            )
        }
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
        StyleRow(
            preset = p,
            onRename = { renaming = p },
            onExport = { mandaFuori(p) },
            onRemove = { togli(p) }
        )
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
            StyleRow(
                preset = p,
                onRename = { renaming = p },
                onExport = { mandaFuori(p) },
                onRemove = { togli(p) }
            )
        }
    }

    /*
     * I tre comandi sono in fondo: si usano di rado, l'elenco è quello che si guarda.
     * 'Ripristina' è il primo perché è l'unico che toglie qualcosa, e non deve essere accanto a
     * 'Importa'.
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
        TextButton(onClick = { importa.launch(arrayOf(STYLE_ANY)) }) {
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
     * ⚠️ **Questo chiede conferma, la cancellazione di uno stile no**: qui si perdono tutti gli
     * stili salvati in un colpo, e nessuna notifica da tre secondi li rimetterebbe. Il testo è
     * dell'utente.
     * Non è una modale vera: non raccoglie niente di scritto, quindi il tocco fuori vale 'Annulla'
     * (`Rules.md` § '👆 Che cosa fa il tocco FUORI da una finestra').
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

/** Il titolino che divide gli stili dell'app da quelli salvati. */
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
 * Una riga dell'elenco: il nome e i tre comandi che lo toccano.
 *
 * Il rientro a destra è il posto della manopola che [Reorderable] disegna sopra la riga: senza,
 * un nome lungo le finirebbe sotto. È la stessa misura delle righe dei campi info.
 * Icone visibili: qui si viene per gestire l'elenco; il tocco lungo è il gesto del modulo Stili,
 * dove serve a comporre uno stile.
 * Il cestino è l'ultimo: il comando che toglie qualcosa va in fondo alla riga, come nel Disegno.
 */
@Composable
private fun StyleRow(
    preset: Preset,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onRemove: () -> Unit
) {
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
        IconButton(onClick = onExport) {
            Icon(Glyphs.Download, stringResource(R.string.settings_styles_export))
        }
        IconButton(onClick = onRemove) {
            Icon(Glyphs.PickDelete, stringResource(R.string.look_preset_remove))
        }
    }
}

/** Quanto spazio una riga lascia alla manopola del trascinamento: vedi [HANDLE]. */
private val STYLE_HANDLE_ROOM = HANDLE + 4.dp

/**
 * Il tipo con cui gli stili escono dall'app.
 *
 * Il contenuto è JSON, il formato dell'archivio: il file esportato è quello su disco, senza una
 * seconda scrittura da tenere allineata.
 * ⚠️ **Il tipo dichiarato è generico, non `application/json`**: con quello un fornitore di
 * documenti rimette il suffisso del tipo, e il file risulterebbe `.aivcollection.json`. È lo
 * stesso tipo del file di impostazioni (vedi `Backup.MIME`).
 */
private const val STYLE_MIME = "application/octet-stream"

/**
 * Che file il selettore lascia scegliere all'importazione: tutti.
 *
 * Tutti, come per il file di impostazioni: un file con un suffisso sconosciuto arriva col tipo
 * generico, e uno esportato prima della `2.95` arriva come JSON, quindi un filtro per tipo ne
 * nasconderebbe uno. Se il testo è un archivio o uno stile solo lo dice [Presets.load].
 */
private const val STYLE_ANY = "*/*"

/**
 * Il nome che il selettore propone quando si esporta.
 *
 * L'estensione `.aivcollection` è scelta dell'utente (2.95). Lo stile singolo ha la sua,
 * [ONE_STYLE_SUFFIX].
 */
private const val STYLE_FILE = "aiv-styles.aivcollection"

/**
 * Il suffisso del file di uno stile solo, dopo il nome dello stile.
 *
 * `.aivstyle` (risposta `dopo` a `d-stile-singolo`, 4.96). Il contenuto è l'oggetto che
 * l'archivio scrive per ogni stile ([Presets.exportOne]), e all'importazione lo riconosce
 * [Presets.load].
 */
private const val ONE_STYLE_SUFFIX = ".aivstyle"

package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * The settings screen.
 *
 * Five of the nine carry a description and the rest do not, which is a choice
 * rather than an omission: an explanation under a setting whose name already says
 * everything is noise, and after three of them nobody reads the fourth. The ones
 * that have one are the ones where the name cannot carry the meaning: what the
 * checkerboard is FOR, what 'enlarge' does when it is off, what 100% means, what the
 * reverse reading order is the reverse OF, and what exactly opens at startup.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onStartFolder: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back)
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Spacer(Modifier.height(8.dp))

        // ⚠️ Il tema dell'APP sta per primo e prima di quello dello sfondo, che gli somiglia
        // ma risponde a un'altra domanda (vedi `UiTheme`): messo dopo, si leggerebbe come
        // una variante di quello, e sono due assi indipendenti.
        Choices(
            label = stringResource(R.string.settings_ui_theme),
            detail = stringResource(R.string.settings_ui_theme_desc),
            options = UiTheme.entries,
            selected = settings.uiTheme,
            nameOf = {
                stringResource(
                    when (it) {
                        UiTheme.SYSTEM -> R.string.settings_system
                        // ⚠️ Stringhe PROPRIE e non quelle della tinta del fondo, che
                        // sono al femminile perché dicono 'tinta chiara': qui il nome è
                        // 'tema', e riusarle darebbe 'Tema: Chiara'.
                        UiTheme.LIGHT -> R.string.settings_theme_light
                        UiTheme.DARK -> R.string.settings_theme_dark
                    }
                )
            },
            onSelect = { onChange(settings.copy(uiTheme = it)) }
        )

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        Choices(
            label = stringResource(R.string.settings_background),
            detail = stringResource(R.string.settings_bg_desc),
            options = BgType.entries,
            selected = settings.bgType,
            nameOf = {
                stringResource(
                    when (it) {
                        BgType.CHECKER -> R.string.settings_bg_checker
                        BgType.SOLID -> R.string.settings_bg_solid
                    }
                )
            },
            onSelect = { onChange(settings.copy(bgType = it)) }
        )

        Choices(
            label = stringResource(R.string.settings_bg_theme),
            detail = null,
            options = BgTheme.entries,
            selected = settings.bgTheme,
            nameOf = {
                stringResource(
                    when (it) {
                        BgTheme.AUTO -> R.string.settings_auto
                        BgTheme.LIGHT -> R.string.settings_light
                        BgTheme.DARK -> R.string.settings_dark
                    }
                )
            },
            onSelect = { onChange(settings.copy(bgTheme = it)) }
        )

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        SwitchRow(
            label = stringResource(R.string.settings_fit_grow),
            detail = stringResource(R.string.settings_fit_grow_desc),
            checked = settings.fitGrow,
            onChange = { onChange(settings.copy(fitGrow = it)) }
        )

        Text(
            text = stringResource(R.string.settings_zoom_max) + "   " +
                settings.zoomMax.roundToInt() + "x",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp)
        )
        Slider(
            value = settings.zoomMax,
            onValueChange = { onChange(settings.copy(zoomMax = it.roundToInt().toFloat())) },
            valueRange = SettingsStore.ZOOM_MAX_MIN..SettingsStore.ZOOM_MAX_MAX,
            modifier = Modifier.fillMaxWidth()
        )

        Choices(
            label = stringResource(R.string.settings_scale_mode),
            detail = stringResource(R.string.settings_scale_desc),
            options = ScaleMode.entries,
            selected = settings.scaleMode,
            nameOf = {
                stringResource(
                    when (it) {
                        ScaleMode.PHYSICAL -> R.string.settings_scale_physical
                        ScaleMode.LOGICAL -> R.string.settings_scale_logical
                    }
                )
            },
            onSelect = { onChange(settings.copy(scaleMode = it)) }
        )

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        // ⚠️⚠️ **LA STESSA STRINGA DEL MENU DEL VISUALIZZATORE** (`details_bar`, richiesta
        // dell'utente: *deve chiamarsi così sia in questo menu che nelle impostazioni*). Due
        // stringhe uguali si sarebbero separate al primo ritocco di una delle due; una
        // stringa sola non può. ⚠️ Il nome vecchio era 'Pannello dei dettagli' in cinque
        // lingue e 'barra' nelle altre undici: la rinomina ha toccato le cinque.
        Choices(
            label = stringResource(R.string.details_bar),
            detail = null,
            options = InfoPosition.entries,
            selected = settings.infoPosition,
            nameOf = {
                stringResource(
                    when (it) {
                        InfoPosition.TOP -> R.string.settings_top
                        InfoPosition.BOTTOM -> R.string.settings_bottom
                    }
                )
            },
            onSelect = { onChange(settings.copy(infoPosition = it)) }
        )

        SwitchRow(
            label = stringResource(R.string.settings_info_visible),
            detail = null,
            checked = settings.infoVisible,
            onChange = { onChange(settings.copy(infoVisible = it)) }
        )

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        FactFields(settings = settings, onChange = onChange)

        // ⚠️ Con la ricerca immagine è uscito anche il suo motore, che era l'ultima
        // voce di questo elenco, e con lei il separatore che la staccava: un filo
        // sopra il nulla è peggio di nessun filo.

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        /*
         * ⚠️⚠️ **L'ELENCO DELLE NASCOSTE È METÀ DELLA FUNZIONE, non un di più**: si
         * nasconde con un tocco lungo, cioè da un'altra schermata e senza lasciare
         * traccia, quindi se non ci fosse un posto in cui rivedere che cosa si è nascosto
         * l'unico modo di riavere una cartella sarebbe indovinare che esiste
         * quest'impostazione. Una funzione che toglie roba deve dire dove l'ha messa.
         * ⚠️ Compare **solo quando c'è qualcosa**: una sezione sempre presente e quasi
         * sempre vuota è rumore in una schermata che si scorre.
         */
        if (settings.hiddenFolders.isNotEmpty()) {
            Text(
                text = stringResource(R.string.settings_hidden),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = stringResource(R.string.settings_hidden_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // ⚠️ Ordinate, e non nell'ordine in cui sono state nascoste: un insieme non ha
            // un ordine proprio, quindi senza questo le righe si rimescolerebbero da sole
            // fra un'apertura e l'altra.
            settings.hiddenFolders.sorted().forEach { path ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        // ⚠️ Il nome davanti e il percorso sotto: due cartelle possono
                        // chiamarsi uguale, quindi il percorso è l'unica cosa che le
                        // distingue, ma è anche lungo e illeggibile come titolo.
                        text = path.substringAfterLast('/'),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        onChange(settings.copy(hiddenFolders = settings.hiddenFolders - path))
                    }) { Text(stringResource(R.string.settings_hidden_show)) }
                }
                Text(
                    text = path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
        }

        // ⚠️ Le colonne stanno accanto alle voci di sfoglio e non a quelle del
        // visualizzatore, perché riguardano la schermata delle cartelle: chi le cerca le
        // cerca vicino a come si trovano le foto.
        Choices(
            label = stringResource(R.string.settings_folder_columns),
            detail = stringResource(R.string.settings_folder_columns_desc),
            options = FOLDER_COLUMNS.map { Columns(it) },
            selected = Columns(settings.folderColumns),
            nameOf = { it.n.toString() },
            onSelect = { onChange(settings.copy(folderColumns = it.n)) }
        )

        // ⚠️ Attaccata alle colonne e senza separatore in mezzo: parlano della **stessa**
        // griglia, una di quante colonne ha e l'altra di cosa si legge sotto le copertine.
        SwitchRow(
            label = stringResource(R.string.settings_folder_count),
            detail = stringResource(R.string.settings_folder_count_desc),
            checked = settings.folderCount,
            onChange = { onChange(settings.copy(folderCount = it)) }
        )

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        SwitchRow(
            label = stringResource(R.string.settings_reverse_order),
            detail = stringResource(R.string.settings_reverse_order_desc),
            checked = settings.reverseSequence,
            onChange = { onChange(settings.copy(reverseSequence = it)) }
        )

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        SwitchRow(
            label = stringResource(R.string.settings_clipboard),
            detail = stringResource(R.string.settings_clipboard_desc),
            checked = settings.clipboardStart,
            onChange = { onChange(settings.copy(clipboardStart = it)) }
        )

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        SwitchRow(
            label = stringResource(R.string.settings_start_folder),
            detail = stringResource(R.string.settings_start_folder_desc),
            checked = settings.openAtStart,
            // ⚠️ Acceso senza una cartella scelta porta ALL'ELENCO invece di accendersi
            // e non fare niente: un interruttore che dipende da un'altra voce e non lo
            // dice è il modo classico di far sembrare rotta un'impostazione.
            onChange = {
                if (it && settings.startFolder == null) onStartFolder()
                else onChange(settings.copy(openAtStart = it))
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = settings.startFolderName.ifBlank {
                    stringResource(R.string.settings_start_folder_none)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onStartFolder) {
                Text(stringResource(R.string.settings_start_folder_pick))
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        // ⚠️⚠️ **`fillMaxWidth` NON È DECORAZIONE: senza, il blocco NON è centrato**, ed è
        // il difetto che l'utente ha visto (2026-08-29). `Identity` centra i propri figli
        // fra loro, ma senza larghezza propria prende quella del testo più lungo e questa
        // colonna la posa a sinistra, perché il suo allineamento è quello di serie: il
        // blocco risultava centrato **su sé stesso** e spostato verso il bordo. Dandogli
        // tutta la larghezza, il centro dei figli è il centro dello schermo.
        // ⚠️ L'icona è IDENTICA a quella della schermata iniziale, e nella 0.47 non lo
        // era: la ragione, e il fatto che la questione sia già stata decisa nei due versi,
        // stanno accanto ad `AppIcon`.
        Identity(iconSize = 72.dp, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Quali dati mostrare quando si apre 'Info' su una fotografia, e in che ordine.
 *
 * ⚠️⚠️ **DUE COMANDI PER RIGA PERCHÉ SONO DUE DOMANDE DIVERSE**, richiesta dell'utente
 * (2026-08-30: *scegliere quali campi e in quale ordine*): la casella dice **se** il campo
 * si vede, le frecce **dove** sta. Un elenco che rispondesse a una sola delle due avrebbe
 * evaso metà della richiesta.
 *
 * ⚠️⚠️ **LE FRECCE E NON IL TRASCINAMENTO, e la scelta è dichiarata**: riordinare una lista
 * col dito in Compose vuol dire scriversi il gesto, la misura delle righe e lo scorrimento
 * automatico ai bordi (lo stesso lavoro che nella griglia è costato una versione), e questa
 * lista è di dieci righe dentro una schermata che già scorre. Due frecce sono meno eleganti
 * e sempre chiare, anche a chi non sa che quella lista si potrebbe trascinare.
 *
 * ⚠️ **Il nome del file non ha nessuno dei due comandi**: è sempre visibile e sempre in
 * testa, come l'utente ha chiesto, quindi mostrargli una casella spenta o una freccia
 * inerte sarebbe offrire una scelta che non c'è. Gli altri due campi obbligatori (pixel e
 * peso) portano il **lucchetto** al posto della casella e le frecce sì: 'sempre visibile'
 * non vuol dire 'in posizione fissa'.
 *
 * ⚠️ **La prima freccia su e l'ultima freccia giù restano spente** invece di sparire: una
 * fila di comandi che cambia lunghezza da riga a riga si legge peggio di una in cui uno è
 * grigio.
 */
@Composable
private fun FactFields(settings: Settings, onChange: (Settings) -> Unit) {
    Text(
        text = stringResource(R.string.settings_facts),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 12.dp)
    )
    Text(
        text = stringResource(R.string.settings_facts_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val order = settings.factOrder
    order.forEachIndexed { at, field ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (field.always) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.settings_facts_always),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp).size(18.dp)
                )
            } else {
                Checkbox(
                    checked = field !in settings.factOff,
                    onCheckedChange = { on ->
                        val off = if (on) settings.factOff - field else settings.factOff + field
                        onChange(settings.copy(factOff = off))
                    }
                )
            }
            Text(
                text = stringResource(field.label),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            // ⚠️ Il nome sta in testa e non si muove: la posizione 1 è la prima che può
            // salire, e la sua salita si ferma a 1, non a 0.
            IconButton(
                onClick = { onChange(settings.copy(factOrder = order.moved(at, at - 1))) },
                enabled = at > 1
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.settings_facts_up)
                )
            }
            IconButton(
                onClick = { onChange(settings.copy(factOrder = order.moved(at, at + 1))) },
                enabled = at in 1 until order.lastIndex
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.settings_facts_down)
                )
            }
        }
    }
}

/**
 * La stessa lista con un elemento spostato.
 *
 * ⚠️ Si toglie e si rimette invece di scambiare i due: lo scambio funziona solo fra vicini,
 * e il giorno che servisse un trascinamento vero questa funzione va già bene.
 */
private fun List<FactField>.moved(from: Int, to: Int): List<FactField> {
    if (from !in indices || to !in indices || from == to) return this
    val out = toMutableList()
    out.add(to, out.removeAt(from))
    return out
}

/**
 * Un numero di colonne, vestito da [Choice] per entrare nella fila di pastiglie.
 *
 * ⚠️ Esiste perché [Choices] parla di scelte con un gettone, e le colonne sono un numero:
 * questo è l'adattatore, non un'impostazione in più. ⚠️ È una `data class` per
 * l'uguaglianza, che è come la fila riconosce quale pastiglia è accesa.
 */
private data class Columns(val n: Int) : Choice {
    override val token: String get() = n.toString()
}

/**
 * A row of chips for one choice. FlowRow and not Row: two of these carry labels a
 * sentence long, and on a narrow screen a fixed row would push them off the edge
 * instead of wrapping.
 */
@Composable
private fun <T : Choice> Choices(
    label: String,
    detail: String?,
    options: List<T>,
    selected: T,
    nameOf: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 12.dp)
    )
    detail?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(nameOf(option)) }
            )
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    detail: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
            detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

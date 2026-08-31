package io.github.roccobot.aiv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Le tre pagine delle impostazioni: la radice e le due che le pendono sotto.
 *
 * ⚠️⚠️ **QUATTRO GRUPPI E DUE SOTTO-PAGINE**, richiesta dell'utente (*le impostazioni sono
 * ancora da riordinare e mettere in sotto-pagine*). Prima era una colonna sola di dodici
 * voci separate da dieci filetti, che si scorreva per due schermate e mezzo senza sapere
 * dove si era: un filetto dice 'qui cambia argomento' ma non dice **quale**, e dieci
 * filetti di fila non dicono più niente. Un titolo di gruppo fa entrambe le cose, quindi i
 * filetti sono spariti tutti tranne quello sopra il piede.
 *
 * ⚠️ **Sotto-pagina sono le due voci che sono ELENCHI**, e non le altre: le cartelle
 * nascoste e i dati di Info crescono col numero di righe, hanno comandi propri riga per
 * riga, ed erano da sole più di metà dell'altezza della schermata. Tutto il resto è una
 * riga a testa e in una sotto-pagina costerebbe un tocco senza guadagnare niente.
 *
 * ⚠️⚠️ **OGNI RIGA CHE APRE UNA PAGINA DICE CHE COSA C'È DENTRO** ('3 cartelle', '7 dati'),
 * e non è decorazione: nascondere un elenco dietro un tocco fa perdere l'unica cosa che
 * l'elenco aperto diceva da sé, cioè quanto è lungo. Senza il riepilogo bisogna entrare
 * per sapere se c'è qualcosa, che è esattamente il costo che le sotto-pagine dovevano
 * togliere.
 *
 * ⚠️ Cinque delle voci portano una spiegazione e le altre no, che è una scelta e non una
 * dimenticanza: una spiegazione sotto un'impostazione il cui nome dice già tutto è rumore,
 * e dopo tre nessuno legge la quarta. Quelle che ce l'hanno sono quelle in cui il nome non
 * può portare il senso: a che cosa SERVE la scacchiera, che cosa fa 'ingrandisci' quando è
 * spento, che cosa significa 100%, di che cosa è il rovescio l'ordine inverso, e che cosa
 * si apre esattamente all'avvio.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onStartFolder: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var page by remember { mutableStateOf(Page.ROOT) }
    // ⚠️⚠️ **LO SCORRIMENTO DELLA RADICE VIVE QUI E NON DENTRO LA PAGINA**: le tre pagine
    // stanno in tre rami di un `when`, quindi uno stato ricordato dentro `Shell` nascerebbe
    // nuovo a ogni ritorno, e si tornerebbe indietro trovandosi in cima. Tenuto qui,
    // sopravvive al passaggio, perché `SettingsScreen` non esce di scena. Le sotto-pagine
    // invece il proprio lo vogliono nuovo: si entra dall'inizio.
    val rootScroll = rememberScrollState()
    // ⚠️ Vince su quello dell'attività (`ViewerActivity`, che qui chiama `leaveSettings`)
    // perché è registrato DOPO: il dispatcher di Android serve l'ultimo arrivato fra quelli
    // accesi. È lo stesso annidamento della selezione nella griglia, che regge da versioni.
    BackHandler(enabled = page != Page.ROOT) { page = Page.ROOT }

    when (page) {
        Page.ROOT -> Shell(
            title = stringResource(R.string.settings_title),
            onBack = onBack,
            modifier = modifier,
            scroll = rootScroll
        ) {
            RootPage(
                settings = settings,
                onChange = onChange,
                onStartFolder = onStartFolder,
                onOpen = { page = it }
            )
        }

        Page.FACTS -> Shell(
            title = stringResource(R.string.settings_facts),
            onBack = { page = Page.ROOT },
            modifier = modifier
        ) {
            Detail(stringResource(R.string.settings_facts_desc))
            FactFields(settings = settings, onChange = onChange)
        }

        Page.HIDDEN -> Shell(
            title = stringResource(R.string.settings_hidden),
            onBack = { page = Page.ROOT },
            modifier = modifier
        ) {
            HiddenFolders(settings = settings, onChange = onChange)
        }
    }
}

/** Quale delle tre pagine si sta guardando. */
private enum class Page { ROOT, FACTS, HIDDEN }

/**
 * Le impostazioni di una riga sola, nei loro quattro gruppi.
 *
 * ⚠️ L'ordine dei gruppi va dal generale al particolare: prima com'è fatta l'app, poi come
 * si guarda una foto, poi come si trovano le foto, e per ultimo che cosa succede
 * all'accensione, che è la voce che si tocca una volta e non si guarda più.
 */
@Composable
private fun ColumnScope.RootPage(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onStartFolder: () -> Unit,
    onOpen: (Page) -> Unit
) {
    Group(stringResource(R.string.settings_group_look))

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

    Group(stringResource(R.string.settings_group_viewer))

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

    // ⚠️⚠️ **LA STESSA STRINGA DEL MENU DEL VISUALIZZATORE** (`details_bar`, richiesta
    // dell'utente: *deve chiamarsi così sia in questo menu che nelle impostazioni*). Due
    // stringhe uguali si sarebbero separate al primo ritocco di una delle due; una
    // stringa sola non può.
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

    PageRow(
        label = stringResource(R.string.settings_facts),
        summary = pluralStringResource(
            R.plurals.settings_facts_count,
            settings.factRows.size,
            settings.factRows.size
        ),
        onOpen = { onOpen(Page.FACTS) }
    )

    Group(stringResource(R.string.settings_group_browse))

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

    // ⚠️ Attaccata alle colonne: parlano della **stessa** griglia, una di quante colonne
    // ha e l'altra di cosa si legge sotto le copertine.
    SwitchRow(
        label = stringResource(R.string.settings_folder_count),
        detail = stringResource(R.string.settings_folder_count_desc),
        checked = settings.folderCount,
        onChange = { onChange(settings.copy(folderCount = it)) }
    )

    SwitchRow(
        label = stringResource(R.string.settings_reverse_order),
        detail = stringResource(R.string.settings_reverse_order_desc),
        checked = settings.reverseSequence,
        onChange = { onChange(settings.copy(reverseSequence = it)) }
    )

    /*
     * ⚠️⚠️ **L'ELENCO DELLE NASCOSTE È METÀ DELLA FUNZIONE, non un di più**: si
     * nasconde con un tocco lungo, cioè da un'altra schermata e senza lasciare
     * traccia, quindi se non ci fosse un posto in cui rivedere che cosa si è nascosto
     * l'unico modo di riavere una cartella sarebbe indovinare che esiste
     * quest'impostazione. Una funzione che toglie roba deve dire dove l'ha messa.
     * ⚠️ Compare **solo quando c'è qualcosa**, e la sotto-pagina non ha cambiato la
     * scelta: una riga sempre presente e quasi sempre vuota è rumore in una schermata
     * che si scorre. La pagina invece la stringa vuota la sa dire, perché ci si può
     * restare dentro dopo aver rimostrato l'ultima.
     */
    if (settings.hiddenFolders.isNotEmpty()) {
        PageRow(
            label = stringResource(R.string.settings_hidden),
            summary = pluralStringResource(
                R.plurals.settings_hidden_count,
                settings.hiddenFolders.size,
                settings.hiddenFolders.size
            ),
            onOpen = { onOpen(Page.HIDDEN) }
        )
    }

    Group(stringResource(R.string.settings_group_start))

    SwitchRow(
        label = stringResource(R.string.settings_clipboard),
        detail = stringResource(R.string.settings_clipboard_desc),
        checked = settings.clipboardStart,
        onChange = { onChange(settings.copy(clipboardStart = it)) }
    )

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

    // ⚠️ **L'UNICO filetto che resta**, e resta perché non separa due gruppi ma dice che
    // i gruppi sono finiti: sotto non c'è un'altra impostazione, c'è il piede.
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

/**
 * Il guscio comune alle tre pagine: la colonna che scorre, la freccia e il titolo.
 *
 * ⚠️ Il titolo prende `weight`, e non è un dettaglio: 'Informazioni sul file' in tedesco e
 * in tamil è quasi il doppio, e senza peso una `Row` lo taglia invece di mandarlo a capo.
 */
@Composable
private fun Shell(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    scroll: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(scroll)
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
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

/**
 * Il titolo di un gruppo.
 *
 * ⚠️ `titleMedium` nel colore primario, in mezzo alle due misure che c'erano già: il titolo
 * della pagina è `headlineSmall` e il nome di un'impostazione `titleSmall`, quindi il
 * gruppo ha bisogno di stare fra i due per leggersi come un livello e non come una voce.
 * Il colore fa il resto del lavoro, e da solo non basterebbe: chi non distingue quel blu
 * vede comunque un testo più grande.
 */
@Composable
private fun Group(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
    )
}

/** La spiegazione sotto un titolo, nello stile che tutte le voci usano. */
@Composable
private fun Detail(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Una riga che apre una sotto-pagina.
 *
 * ⚠️ Il gallone è `AutoMirrored`, come la freccia Indietro: in persiano, urdu e arabo una
 * pagina si apre verso sinistra, e un gallone fisso puntato a destra indicherebbe il verso
 * sbagliato. È la stessa ragione per cui le pillole sono in una `Row` e non allineate a
 * `End`. ⚠️ Nessuna descrizione per lo schermo: la riga intera è il comando, e il lettore
 * legge già nome e riepilogo. Una parola in più ('apri') li ripeterebbe.
 */
@Composable
private fun PageRow(label: String, summary: String, onOpen: () -> Unit) {
    Row(
        // ⚠️ `clickable` PRIMA di `padding`: così il tocco prende anche il margine, e la
        // riga arriva ai 48dp di bersaglio senza scriverli.
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Le cartelle nascoste, una per riga, con il comando per rimostrarle.
 *
 * ⚠️ La pagina resta in piedi anche quando l'elenco si vuota, e per questo esiste
 * `settings_hidden_none`: si arriva qui con tre cartelle, si rimostrano tutte e tre, e una
 * pagina che si svuotasse in silenzio sembrerebbe rotta. La riga che porta qui invece
 * sparisce, ma la si rivede solo tornando indietro.
 */
@Composable
private fun HiddenFolders(settings: Settings, onChange: (Settings) -> Unit) {
    Detail(stringResource(R.string.settings_hidden_desc))

    if (settings.hiddenFolders.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_hidden_none),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        return
    }

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
 * lista è di dieci righe. Due frecce sono meno eleganti e sempre chiare, anche a chi non sa
 * che quella lista si potrebbe trascinare.
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
    detail?.let { Detail(it) }
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
            detail?.let { Detail(it) }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

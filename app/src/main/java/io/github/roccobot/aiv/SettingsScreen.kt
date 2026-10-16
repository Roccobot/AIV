package io.github.roccobot.aiv

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import java.text.Normalizer
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Le pagine delle impostazioni: la radice e quelle che le pendono sotto.
 *
 * Gruppi con un titolo e sotto-pagine: un titolo di gruppo dice dove cambia argomento e quale,
 * quindi fra i gruppi non ci sono filetti, salvo quello sopra il piede. Le pagine sono quelle
 * di [Page] e i gruppi sono le chiamate a [Group]: i conti non si scrivono nei commenti, perché
 * diventano falsi (`rules/Roccobot.md` § '🔢 I conti si contano, non si scrivono').
 *
 * ⚠️ **Quando una voce diventa sotto-pagina lo dice `Rules.md`**, § '⚙️ Dove va
 * un'impostazione, e chi la deve trovare': un elenco con comandi riga per riga, voci delicate,
 * una famiglia oltre la soglia dell'utente, un comando che ha bisogno di un paragrafo, una
 * famiglia che ne contiene un'altra. La soglia si conta sulla famiglia e non sulla sezione, e
 * una riga sola che non rientra in nessun caso, dietro un tocco, costerebbe senza guadagnare
 * niente.
 *
 * ⚠️ **Ogni riga che apre una pagina dice quanto è lungo quello che c'è dentro**: senza il
 * riepilogo bisognerebbe entrare per saperlo. Il riepilogo si calcola (`R.plurals`) o si
 * compone dalle stesse stringhe della pagina, mai si scrive a mano: una frase fissa come
 * `settings_zoom_page_summary` è rimasta falsa in tutte le lingue per molte versioni.
 *
 * ⚠️ **La ricerca deve trovare ogni voce, dovunque viva, e la copertura si scrive nello stesso
 * giro della sotto-pagina.** [LocalQuery] è fornito nel solo ramo della radice, quindi una voce
 * dietro un tocco resta fuori dalla ricerca, salvo che passi da [PageOfRows] (pagine fatte di
 * righe) o dal parametro `extra` di [PageRow] (elenchi con comandi riga per riga). Il riepilogo
 * non basta: è così che le voci dello zoom sono rimaste fuori dalla ricerca.
 * Si trovano anche i titoli (voce `imp-ricerca`): quello di una pagina rimette in scena la riga
 * che la apre ([PageOfRows]), quello di una sezione fa comparire il titolo con le sue voci
 * ([Section] e [LocalSection]).
 *
 * La spiegazione sotto una voce ha un criterio: ce l'ha la voce il cui nome non basta a dire il
 * senso, perché sotto un nome che dice già tutto è rumore. Oggi ce l'hanno quasi tutte, e la
 * potatura è una decisione dell'utente, aperta nel brief.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onStartFolder: () -> Unit,
    onResetHints: () -> Unit,
    /** Apre il selettore dell'app di modifica: la finestra la fa il modello. Vedi `chooseEditor`. */
    onChooseEditor: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    /*
     * ⚠️ **Salvata e non solo ricordata**: `ViewerActivity` non dichiara `configChanges`, quindi
     * una rotazione ricrea la composizione, e con un `remember` si tornerebbe nella radice
     * perdendo anche la ricerca. Si salvano i nomi delle pagine ([PAGE_STACK]); il valore vero è
     * [page].
     */
    val stack = rememberSaveable(saver = PAGE_STACK) { mutableStateListOf() }
    val page = stack.lastOrNull() ?: Page.ROOT
    // ⚠️ **Lo scorrimento della radice è qui e non dentro la pagina**: ogni pagina di [Page] è
    // un ramo di un `when`, quindi uno stato ricordato dentro `Shell` nascerebbe nuovo a ogni
    // ritorno, in cima. Qui resta, perché `SettingsScreen` non esce di scena. Le sotto-pagine
    // invece si aprono dall'inizio.
    val rootScroll = rememberScrollState()
    // Qui e non dentro `RootPage`, per la stessa ragione: andare in una sotto-pagina e tornare
    // non cancella la ricerca. Si salva, perché una rotazione non annulla una ricerca.
    var query by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    // Due funzioni: chi apre una pagina o torna indietro non deve sapere che la navigazione è
    // una pila.
    fun open(next: Page) {
        /*
         * ⚠️ **Una ricerca finisce quando porta a una pagina** (nota dell'utente sulla voce
         * `imp-cerca-titoli`): è il modo di arrivare a una voce, e trovarla ancora accesa al
         * ritorno costringerebbe a svuotare il campo. È scritto qui perché questa è la sola
         * porta: ogni `PageRow` apre la sua pagina passando di qua.
         * Vale per la navigazione e non per ogni tocco: svuotare il campo dopo un interruttore
         * glielo farebbe sparire da sotto il dito. Lo scorrimento torna in cima solo se la
         * ricerca c'era, o la radice si azzererebbe a ogni sotto-pagina.
         */
        if (query.isNotBlank()) {
            query = ""
            scope.launch { rootScroll.scrollTo(0) }
        }
        stack.add(next)
    }
    fun back() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    // ⚠️ Vince su quello dell'attività (`ViewerActivity`, che qui chiama `leaveSettings`)
    // perché è registrato dopo: il dispatcher di Android serve l'ultimo fra quelli accesi,
    // come per la selezione nella griglia.
    BackHandler(enabled = stack.isNotEmpty()) { back() }
    // Con una ricerca in corso Indietro la annulla e non esce, come ogni ricerca in un elenco.
    // I due gestori non sono mai accesi insieme.
    BackHandler(enabled = page == Page.ROOT && query.isNotBlank()) { query = "" }

    /*
     * ⚠️ **La cache delle miniature si misura, si svuota e si riempie qui, senza il modello**:
     * il modello tiene lo stato che altre schermate osservano, e una cartella di file temporanei
     * che Android può svuotare da sé non lo è. La generazione vive in [Warmup], un oggetto di
     * processo, e le serve solo l'elenco delle cartelle nascoste, che la schermata ha già.
     * ⚠️ **La misura va su un thread di I/O**: la cartella può tenere decine di migliaia di file
     * (risposta `disco` a `d-mini-disco`), cioè altrettante `stat` sul thread che disegna. Il
     * primo valore è l'ultima misura di questo processo ([AvifCache.known]), e senza misura la
     * riga non dice niente, perché 'nessuna miniatura' per un istante sarebbe falso.
     * ⚠️ [emptied] e [Warmup.finished] sono le chiavi che fanno rifare la misura a
     * [produceState] dopo uno svuotamento o una generazione: senza, la riga direbbe i megabyte di
     * prima.
     */
    val context = LocalContext.current
    var emptied by remember { mutableIntStateOf(0) }
    val thumbBytes by produceState(AvifCache.known, emptied, Warmup.finished) {
        value = withContext(Dispatchers.IO) { AvifCache.bytes(context) }
    }
    val clearThumbs: () -> Unit = {
        scope.launch {
            withContext(Dispatchers.IO) { AvifCache.clear(context) }
            Thumbs.forgetAll(context)
            emptied++
        }
    }
    val generateThumbs: () -> Unit = { Warmup.start(context, settings.hiddenFolders, settings.folderSelection) }

    /*
     * Due colonne da 600dp in su (mockup `settings`): l'indice a lato e la pagina scelta nello
     * spazio principale. Sul telefono resta la pila a pieno schermo.
     * Il salto dall'indice svuota la pila e apre la pagina, così sul tablet si cambia sezione
     * senza risalire; Indietro dentro la pagina usa la pila come sul telefono.
     */
    val widthDp = windowWidthDp()
    val dual = Adaptive.sideAvailable(widthDp)
    fun jump(target: Page) {
        if (query.isNotBlank()) {
            query = ""
            scope.launch { rootScroll.scrollTo(0) }
        }
        stack.clear()
        if (target != Page.ROOT) stack.add(target)
    }

    @Composable
    fun Pages(chrome: Modifier) {
    // Una dissolvenza fra due pagine e non un taglio netto: vedi [CUT_FADE_MS].
    AnimatedContent(
        targetState = page,
        transitionSpec = { fadeIn(tween(CUT_FADE_MS)) togetherWith fadeOut(tween(CUT_FADE_MS)) using null },
        label = "pagina"
    ) { shown ->
    when (shown) {
        Page.ROOT -> Shell(
            title = stringResource(R.string.settings_title),
            onBack = onBack,
            modifier = chrome,
            scroll = rootScroll,
            version = true
        ) {
            SearchField(query = query, onQuery = { query = it })
            /*
             * ⚠️ **La colonna in più dice se la ricerca ha trovato qualcosa**: le righe si
             * filtrano una per una e nessuno le conta, e il layout senza figli misura zero.
             * Contarle durante la composizione mostrerebbe 'nessun risultato' per un
             * fotogramma anche quando i risultati ci sono.
             * La spaziatura è quella del guscio, o le voci qui dentro si stringerebbero.
             */
            var empty by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier.onSizeChanged { empty = it.height == 0 },
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CompositionLocalProvider(LocalQuery provides query) {
                    RootPage(
                        settings = settings,
                        onChange = onChange,
                        onStartFolder = onStartFolder,
                        onResetHints = onResetHints,
                        onChooseEditor = onChooseEditor,
                        thumbBytes = thumbBytes,
                        onClearThumbs = clearThumbs,
                        onGenerateThumbs = generateThumbs,
                        onOpen = { open(it) }
                    )
                }
            }
            // Una pagina vuota sotto un campo di ricerca si legge come un'app guasta: la riga
            // dice che la ricerca ha funzionato e non ha trovato niente.
            if (empty && query.isNotBlank()) {
                Text(
                    text = stringResource(R.string.settings_search_none),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }

        /*
         * Le pagine del primo livello (risposta `livelli` a `d-imp-strada`): famiglie che ne
         * contengono un'altra, per la ragione scritta in `Rules.md`, § '⚙️ Dove va
         * un'impostazione, e chi la deve trovare'. Il titolo di ognuna dice la domanda a cui le
         * sue voci rispondono.
         * ⚠️ **Il corpo è una funzione a sé** perché lo compone anche la radice durante una
         * ricerca (vedi [PageOfRows]): scritto qui dentro, mentre si cerca non si troverebbe.
         */
        Page.FOLDERS -> Shell(
            title = stringResource(R.string.settings_group_browse),
            onBack = { back() },
            modifier = chrome
        ) {
            FoldersPage(settings = settings, onChange = onChange, onOpen = { open(it) })
        }

        Page.VIEWER -> Shell(
            title = stringResource(R.string.settings_group_viewer),
            onBack = { back() },
            modifier = chrome
        ) {
            ViewerPage(settings = settings, onChange = onChange, onOpen = { open(it) })
        }

        Page.INFO -> Shell(
            title = stringResource(R.string.settings_page_info),
            onBack = { back() },
            modifier = chrome
        ) {
            InfoPage(settings = settings, onChange = onChange, onOpen = { open(it) })
        }

        Page.CONTROLS -> Shell(
            title = stringResource(R.string.settings_page_controls),
            onBack = { back() },
            modifier = chrome
        ) {
            ControlsPage(settings = settings, onChange = onChange, onOpen = { open(it) })
        }

        Page.EDITING -> Shell(
            title = stringResource(R.string.settings_page_editing),
            onBack = { back() },
            modifier = chrome
        ) {
            EditingPage(
                settings = settings,
                onChange = onChange,
                onChooseEditor = onChooseEditor,
                onOpen = { open(it) }
            )
        }

        Page.FACTS -> {
            /*
             * ⚠️ **Lo scorrimento della pagina arriva fin dentro il riordino**: l'elenco è più
             * alto della finestra, e senza il trascinamento si fermerebbe al bordo dello
             * schermo (vedi [Reorderable]). Nasce qui perché lo usano il guscio e l'elenco.
             */
            val scroll = rememberScrollState()
            Shell(
                title = stringResource(R.string.settings_facts),
                onBack = { back() },
                modifier = chrome,
                scroll = scroll
            ) {
                Detail(stringResource(R.string.settings_facts_desc))
                FactFields(settings = settings, onChange = onChange, scroll = scroll)
            }
        }

        /*
         * Un elenco con comandi per riga, come i campi info: lo scorrimento nasce qui per il
         * trascinamento che arriva al bordo (vedi [Reorderable]).
         * È sotto 'Editor e salvataggio' e non in una sezione sua: quella pagina è già quella
         * dell'editor, e una sezione con lo stesso nome ripeterebbe la parola.
         */
        Page.STYLES -> {
            val scroll = rememberScrollState()
            Shell(
                title = stringResource(R.string.settings_styles),
                onBack = { back() },
                modifier = chrome,
                scroll = scroll
            ) {
                Detail(stringResource(R.string.settings_styles_desc))
                StyleSettings(scroll = scroll)
            }
        }

        Page.BUTTONS -> Shell(
            title = stringResource(R.string.settings_buttons),
            onBack = { back() },
            modifier = chrome
        ) {
            ButtonOrders(settings = settings, onChange = onChange)
        }

        // La pagina non scorre per intero: scorre il contenuto, e 'Aggiungi cartella' resta su
        // una fascia fissa in fondo (vedi [HiddenFolders]).
        Page.HIDDEN -> Shell(
            title = stringResource(R.string.folder_selection),
            onBack = { back() },
            modifier = chrome,
            scrolls = false
        ) {
            HiddenFolders(settings = settings, onChange = onChange)
        }

        // Un elenco con comandi per riga, come i campi info, sotto una fascia fissa: scorre solo
        // l'elenco, ed è il suo scorrimento che serve al trascinamento (vedi [FavouriteFolders]).
        Page.FAVOURITES -> Shell(
            title = stringResource(R.string.favourite_folders),
            onBack = { back() },
            modifier = chrome,
            scrolls = false
        ) {
            FavouriteFolders(
                list = settings.favouriteFolders,
                onList = { onChange(settings.copy(favouriteFolders = it)) }
            )
        }

        Page.ZOOM -> Shell(
            title = stringResource(R.string.settings_zoom_page),
            onBack = { back() },
            modifier = chrome
        ) {
            ZoomAndFit(settings = settings, onChange = onChange)
        }

        Page.VIEWS -> Shell(
            title = stringResource(R.string.view_options),
            onBack = { back() },
            modifier = chrome
        ) {
            ViewOptionsPage(settings = settings, onChange = onChange)
        }

        /*
         * La filigrana è una pagina dentro 'Editor e salvataggio', come 'Rinomina e download' e
         * gli stili: la famiglia è una sola (che logo scrivere sulle immagini salvate) e le sue
         * voci sono oltre la soglia.
         */
        Page.MARK -> Shell(
            title = stringResource(R.string.settings_mark),
            onBack = { back() },
            modifier = chrome
        ) {
            MarkPage(settings = settings, onChange = onChange)
        }

        /*
         * 'Rinomina e download' (richiesta dell'utente): le sue voci rispondono alla stessa
         * domanda, che nome ha il file salvato e dove finisce, e sono oltre la soglia. La pagina
         * è fatta di righe, quindi la ricerca la copre con [PageOfRows].
         */
        Page.SAVING -> Shell(
            title = stringResource(R.string.settings_rename_download),
            onBack = { back() },
            modifier = chrome
        ) {
            RenameAndDownload(settings = settings, onChange = onChange)
        }

        /*
         * 'Gestisci le miniature memorizzate', una sotto-pagina per due comandi (specifica
         * dell'utente): è il caso del comando che ha bisogno di un paragrafo, e il paragrafo è
         * nelle conferme dei due tasti, 'Svuota la cache delle miniature' e 'Genera miniature'.
         * ⚠️ **Non usa `lowered()`**: il 15% più in basso è la definizione di 'centrato' per
         * quello che si apre in mezzo allo schermo con una finestra propria, e questa è una
         * schermata intera, col contenuto già sotto la testata.
         */
        Page.THUMBS -> Shell(
            title = stringResource(R.string.settings_thumbs),
            onBack = { back() },
            modifier = chrome,
            scrolls = false
        ) {
            /*
             * ⚠️ **`BoxWithConstraints` e non solo una colonna che scorre**: dentro un
             * `verticalScroll` un figlio è alto quanto il contenuto, e 'al centro' non vuol dire
             * niente. Sapendo lo spazio, la colonna si fa alta almeno così e centra; con un
             * contenuto più alto (schermo piccolo in orizzontale, testo ingrandito) scorre.
             */
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val room = maxHeight
                ThumbsCard(
                    head = null,
                    onClear = clearThumbs,
                    onGenerate = generateThumbs,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = room)
                )
            }
        }

        /*
         * Una sotto-pagina per due ragioni (`Rules.md` § '⚙️ Dove va un'impostazione, e chi la
         * deve trovare'): è un comando che ha bisogno di un paragrafo, perché chi esporta deve
         * sapere che cosa il file contiene e che una password dimenticata non si recupera; e le
         * sue caselle sono una famiglia sola oltre la soglia.
         * ⚠️ **La ricerca la trova dalla riga che la apre**, con le parole di dentro in `extra`: due
         * tasti e una barra d'avanzamento fra i risultati sarebbero comandi fuori contesto.
         */
        Page.BACKUP -> Shell(
            title = stringResource(R.string.backup_title),
            onBack = { back() },
            modifier = chrome,
            scrolls = false
        ) {
            BackupPage()
        }

        Page.LOOK -> Shell(
            title = stringResource(R.string.settings_page_look),
            onBack = { back() },
            modifier = chrome
        ) {
            LookPage(settings = settings, onChange = onChange)
        }

        Page.START -> Shell(
            title = stringResource(R.string.settings_group_start),
            onBack = { back() },
            modifier = chrome
        ) {
            StartPage(
                settings = settings,
                onChange = onChange,
                onStartFolder = onStartFolder
            )
        }

        Page.BIN -> Shell(
            title = stringResource(R.string.bin_title),
            onBack = { back() },
            modifier = chrome
        ) {
            BinPage(settings = settings, onChange = onChange)
        }
    }
    } // AnimatedContent
    } // Pages

    if (dual) {
        SettingsTabletSplit(
            panelOnStart = settings.hand == Hand.RIGHT,
            rail = {
                SettingsIndexRail(
                    selected = page,
                    onPick = { jump(it) },
                    width = Adaptive.sideWidth(widthDp)
                )
            },
            detail = { Pages(Modifier.fillMaxSize()) },
            modifier = modifier
        )

    } else {
        Pages(modifier)
    }

    // ⚠️ Fuori dal `when`: la generazione si chiede anche dalla radice durante una ricerca, e
    // la sua pagina deve comparire in tutti e due i casi.
    Warmup.run?.let { WarmupPage(it, onCancel = { Warmup.cancel() }) }
}

/**
 * Quale pagina si sta guardando.
 *
 * La navigazione è una pila di questi valori (risposta `livelli` a `d-imp-strada`): una
 * sotto-pagina ne può aprire un'altra, e Indietro risale di un gradino.
 * ⚠️ **`ROOT` non entra mai nella pila**: la radice è la pila vuota, e mettercela dentro
 * darebbe due modi di dire la stessa cosa, con un Indietro che a volte non esce.
 * L'ordine delle voci è libero: la pila si salva per nome ([PAGE_STACK]).
 */
internal enum class Page {
    ROOT,
    FOLDERS, VIEWER, INFO, CONTROLS, EDITING,
    FACTS, HIDDEN, ZOOM, VIEWS, THUMBS, BUTTONS, SAVING, STYLES, MARK, BACKUP,
    // LOOK: tema e profondità dei pannelli; START: avvio.
    LOOK, START,
    // Le voci del cestino, sotto-pagina di 'Gestione dei file'.
    BIN,
    FAVOURITES
}

/**
 * Come la pila delle pagine sopravvive a una rotazione.
 *
 * Salva i nomi, che il `Bundle` sa scrivere e che non cambiano quando le voci di [Page] cambiano
 * ordine. Un nome che questa versione non conosce (uno stato salvato da una versione più nuova,
 * dopo un ritorno indietro) si scarta: la pila riparte più corta, e non va in errore. Il
 * ripristino restituisce una lista di stato, la stessa che si modifica aprendo una pagina.
 * ⚠️ **Uno stato salvato fino alla `6.29` contiene numeri d'ordine**: si leggono con
 * [PAGE_ORDER_629], l'ordine di allora.
 */
internal val PAGE_STACK = listSaver<SnapshotStateList<Page>, Any>(
    save = { it.map(Page::name) },
    restore = { saved ->
        saved.mapNotNull { voce ->
            val nome = if (voce is Int) PAGE_ORDER_629.getOrNull(voce) else voce as? String
            Page.entries.firstOrNull { it.name == nome && it != Page.ROOT }
        }.toMutableStateList()
    }
)

/**
 * L'ordine di [Page] fino alla `6.29`, quando la pila si salvava per numero d'ordine: serve solo
 * a leggere uno stato salvato allora. `NAV`, la pagina 'Navigazione' che non si apriva più dalla
 * `3.41`, è diventata il Visualizzatore, che ne contiene le voci.
 */
private val PAGE_ORDER_629 = listOf(
    "ROOT",
    "FOLDERS", "VIEWER", "INFO", "CONTROLS", "EDITING",
    "FACTS", "HIDDEN", "ZOOM", "VIEWS", "THUMBS", "BUTTONS", "SAVING", "STYLES", "MARK", "BACKUP",
    "LOOK", "VIEWER", "START",
    "BIN",
    "FAVOURITES"
)

/**
 * Che cosa si sta cercando nelle impostazioni, e stringa vuota quando non si cerca.
 *
 * ⚠️ **Un `CompositionLocal` e non un parametro in ogni riga**: ogni voce è una chiamata a sé
 * con le sue stringhe, e passare il testo cercato a mano vorrebbe dire un parametro per voce,
 * con il rischio di una voce nuova che non si filtra.
 * Filtra la riga e non l'elenco: non esiste un modello di dati delle impostazioni, e l'unico
 * posto che conosce le parole di una riga è la riga stessa.
 */
private val LocalQuery = compositionLocalOf { "" }

/**
 * Il titolo della sezione in cui la riga vive, per chi cerca il nome di una sezione.
 *
 * Serve alla voce `imp-ricerca`: i titoli di sezione spariscono durante una ricerca (vedi
 * [Group]) e nessuna riga contiene quella parola, quindi senza questo cercando 'Aspetto' non
 * comparirebbe niente.
 * ⚠️ **Per questo [Section] è un contenitore e [Group] no**: in Compose un valore arriva ai
 * figli e non ai fratelli, quindi un titolo scritto come riga a sé non può dire niente alle voci
 * che lo seguono.
 */
private val LocalSection = compositionLocalOf { "" }

/**
 * Se una riga con questi testi deve comparire adesso.
 *
 * Senza ricerca in corso compare tutto: la stringa vuota è l'assenza di filtro.
 * Il confronto ignora maiuscole e accenti: chi cerca la qualità la digita quasi sempre senza
 * accento, e in molte lingue è la regola.
 * Il nome della sezione vale come un testo della riga: chi cerca una sezione cerca le sue
 * voci. Il risultato dice perché è un risultato, perché [Section] rimette in scena il proprio
 * titolo sopra quelle righe.
 */
@Composable
private fun shown(vararg texts: String?): Boolean {
    val query = plain(LocalQuery.current)
    if (query.isEmpty()) return true
    if (plain(LocalSection.current).contains(query)) return true
    return texts.any { it != null && plain(it).contains(query) }
}

/**
 * Il testo ridotto a quello che serve per confrontarlo: minuscolo e senza segni.
 *
 * ⚠️ **`Normalizer` e non una tabella di lettere**: la scomposizione canonica stacca il segno
 * dalla lettera in tutte le lingue, dall'italiano al vietnamita, mentre una tabella scritta a
 * mano coprirebbe le sole vocali accentate italiane.
 */
private fun plain(text: String): String =
    Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
        .replace(SIGNS, "")

private val SIGNS = Regex("\\p{Mn}+")

/**
 * Un blocco che non è una riga di serie, e che la ricerca deve poter nascondere.
 *
 * ⚠️ Serve ai pochi blocchi scritti a mano (l'editor, la cartella d'avvio, il ripristino
 * degli avvisi): quelli costruiti con [Choices], [SwitchRow] e [PageRow] si filtrano da sé.
 */
@Composable
internal fun Searchable(vararg texts: String?, content: @Composable () -> Unit) {
    if (shown(*texts)) content()
}

/**
 * Il campo in cui si scrive che cosa si cerca, in testa alla pagina.
 *
 * Scorre con l'elenco e non è fisso in testata: appena si scrive l'elenco si accorcia e il
 * campo resta in vista da sé, mentre una testata fissa costerebbe uno strato in più su ogni
 * pagina di [Page].
 * La crocetta compare solo con qualcosa scritto: senza niente da cancellare sarebbe un
 * bersaglio che si preme per sbaglio.
 */
@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQuery,
        // Una riga sola: un campo che si allarga spingerebbe l'elenco giù mentre lo si guarda.
        singleLine = true,
        placeholder = { Text(stringResource(R.string.settings_search)) },
        leadingIcon = {
            // Senza descrizione: la lente ripete il suggerimento del campo, e il lettore di
            // schermo leggerebbe 'cerca' due volte.
            Icon(imageVector = Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQuery("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.settings_search_clear)
                    )
                }
            }
        },
        /*
         * Una pastiglia e non un riquadro (riscontro dell'utente): un campo squadrato col
         * contorno si legge come un modulo da compilare, la pastiglia piena come una ricerca.
         * ⚠️ **Il filetto di Material si toglie in tutti e tre gli stati**, o ricompare toccando
         * il campo: su una forma già chiusa raddoppierebbe il contorno.
         * Il fondo è `surfaceContainerHigh`, lo stesso del pannello della selezione, quindi
         * segue il tema chiaro e scuro.
         */
        shape = RoundedCornerShape(SEARCH_PILL),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Quanto è stondata la pastiglia della ricerca: metà della sua altezza, cioè un mezzo cerchio. */
private val SEARCH_PILL = 28.dp

/**
 * La pagina piatta: le sezioni, le voci che si toccano qui, e le porte delle famiglie.
 *
 * L'ordine delle sezioni va dal generale al particolare, e finisce con quello che si tocca
 * una volta e non si guarda più: è un criterio stabile, mentre un ordine per frequenza si
 * riaprirebbe a ogni voce aggiunta.
 * Una famiglia grande vive dietro una porta (risposta `livelli` a `d-imp-strada`), e qui restano
 * le voci che nessuna famiglia raccoglie: scorrendo si leggono domande e non opzioni.
 * ⚠️ **Dove va una voce nuova lo dice `Rules.md`**, § '⚙️ Dove va un'impostazione, e chi la
 * deve trovare': va letto prima di aggiungere una riga. Qui accanto a ogni sezione c'è il suo
 * perché.
 */
@Composable
private fun ColumnScope.RootPage(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onStartFolder: () -> Unit,
    onResetHints: () -> Unit,
    onChooseEditor: () -> Unit,
    /** Quanto occupano le miniature tenute su disco: è il riepilogo della riga che le svuota. */
    thumbBytes: Long,
    onClearThumbs: () -> Unit,
    onGenerateThumbs: () -> Unit,
    onOpen: (Page) -> Unit
) {
    Section(stringResource(R.string.settings_group_look)) {

        // Prima sotto-pagina di 'Aspetto', per scelta dell'utente (nota del giro della `4.04`).
        PageOfRows(
            label = stringResource(R.string.settings_page_controls),
            // L'elemento interattivo principale c'è solo sul telefono, e il riepilogo dice quello
            // che la pagina contiene davvero.
            summary = listOfNotNull(
                stringResource(R.string.settings_main_control)
                    .takeIf { pillOffered(LocalConfiguration.current.smallestScreenWidthDp) },
                stringResource(R.string.settings_hand),
                stringResource(R.string.settings_labels),
                stringResource(R.string.settings_buttons)
            ).joinToString(SUMMARY_JOIN),
            onOpen = { onOpen(Page.CONTROLS) }
        ) { ControlsPage(settings = settings, onChange = onChange, onOpen = onOpen) }

        // Tema e pannelli (voce `3.27-03`): dietro una porta come le cartelle e il
        // visualizzatore, così la radice non mescola pastiglie e porte.
        PageOfRows(
            label = stringResource(R.string.settings_page_look),
            // Le voci che AIV Play fissa non ci sono, e il riepilogo non le nomina
            // (`Store.fullSettings`).
            summary = listOfNotNull(
                stringResource(R.string.settings_ui_theme),
                if (Store.fullSettings) stringResource(R.string.settings_depth) else null,
                stringResource(R.string.settings_button_look)
            ).joinToString(SUMMARY_JOIN),
            onOpen = { onOpen(Page.LOOK) }
        ) { LookPage(settings = settings, onChange = onChange) }

        /*
         * Le famiglie che rispondono a 'che cosa vedo' (risposta `livelli` a `d-imp-strada`),
         * dietro un tocco, col riepilogo di quello che c'è dentro.
         * Sono in 'Aspetto' e non in una sezione ciascuna: una sezione con la sola porta della
         * sua famiglia ripeterebbe la parola ('Cartelle' sopra 'Cartelle'), e una voce sola non
         * prende un titolo. Quei titoli oggi sono i titoli delle pagine.
         * L'ordine va dal contenitore al contenuto: le cartelle, l'immagine aperta, quello che
         * l'app dice di lei.
         */
        PageOfRows(
            label = stringResource(R.string.settings_group_browse),
            // Il riepilogo nomina soltanto le voci che la pagina mostra: una voce assente nel
            // riepilogo manderebbe a cercare una riga che dentro non c'è.
            summary = buildList {
                add(stringResource(R.string.settings_front))
                if (Store.fullSettings) add(stringResource(R.string.settings_colour))
                add(stringResource(R.string.view_options))
                add(stringResource(R.string.folder_selection))
            }.joinToString(SUMMARY_JOIN),
            onOpen = { onOpen(Page.FOLDERS) }
        ) { FoldersPage(settings = settings, onChange = onChange, onOpen = onOpen) }

        PageOfRows(
            label = stringResource(R.string.settings_group_viewer),
            summary = listOfNotNull(
                stringResource(R.string.settings_background),
                stringResource(R.string.settings_bg_theme),
                stringResource(R.string.settings_zoom_page),
                stringResource(R.string.settings_clip_autoplay),
                stringResource(R.string.settings_images_only),
                if (Store.fullSettings) stringResource(R.string.settings_reverse_order) else null
            ).joinToString(SUMMARY_JOIN),
            onOpen = { onOpen(Page.VIEWER) }
        ) { ViewerPage(settings = settings, onChange = onChange, onOpen = onOpen) }

        PageOfRows(
            label = stringResource(R.string.settings_page_info),
            summary = listOf(
                stringResource(R.string.settings_info_visible),
                stringResource(R.string.settings_facts),
                stringResource(R.string.settings_anim_counter),
                stringResource(R.string.settings_pick_weight)
            ).joinToString(SUMMARY_JOIN),
            onOpen = { onOpen(Page.INFO) }
        ) { InfoPage(settings = settings, onChange = onChange, onOpen = onOpen) }

    }

    /*
     * 'Gestione dei file': la domanda è come non si perde un file quando lo si modifica o lo si
     * cancella, e il legame fra le sue porte lo dice la stringa della copia di sicurezza, che
     * finisce nel cestino. Il cestino è una porta (voce `3.40-04`, per scelta dell'utente).
     * L'ordine segue il percorso di un file: come si modifica, come si salva, che cosa succede
     * se si cancella.
     */
    Section(stringResource(R.string.settings_group_files)) {

        PageOfRows(
            label = stringResource(R.string.settings_page_editing),
            summary = listOfNotNull(
                // AIV Play non ha né la scelta dell'editor né la copia di sicurezza: ha solo i
                // suoi editor, che non scrivono mai sull'originale.
                if (Store.files) stringResource(R.string.settings_editor) else null,
                if (Store.files) stringResource(R.string.settings_editor_backup) else null,
                stringResource(R.string.settings_quality),
                stringResource(R.string.settings_styles),
                stringResource(R.string.settings_mark)
            ).joinToString(SUMMARY_JOIN),
            onOpen = { onOpen(Page.EDITING) }
        ) {
            EditingPage(
                settings = settings,
                onChange = onChange,
                onChooseEditor = onChooseEditor,
                onOpen = onOpen
            )
        }

        // Una porta sua (`d-settings-order`): rinomina e download si usano anche senza
        // l'editor, quindi non sono dentro 'Editor e salvataggio'.
        PageOfRows(
            label = stringResource(R.string.settings_rename_download),
            summary = listOfNotNull(
                stringResource(R.string.settings_save_rename),
                stringResource(R.string.settings_download_path),
                if (Store.fullSettings) stringResource(R.string.settings_ext_edit) else null
            ).joinToString(SUMMARY_JOIN),
            onOpen = { onOpen(Page.SAVING) }
        ) { RenameAndDownload(settings = settings, onChange = onChange) }

        // Il cestino, ultima porta della sezione. AIV Play non ha un cestino suo
        // (`Store.files`): da Android 11 mostra quello di sistema, che tiene i file con regole
        // sue, quindi qui non c'è niente da impostare.
        if (Store.files) {
            PageOfRows(
                label = stringResource(R.string.bin_title),
                summary = listOf(
                    stringResource(R.string.settings_bin),
                    stringResource(R.string.settings_bin_sweep)
                ).joinToString(SUMMARY_JOIN),
                onOpen = { onOpen(Page.BIN) }
            ) { BinPage(settings = settings, onChange = onChange) }
        }
    }

    Section(stringResource(R.string.settings_group_start)) {
        // Appunti e cartella d'avvio dietro una porta (voce `3.27-03`).
        PageOfRows(
            label = stringResource(R.string.settings_group_start),
            summary = listOf(
                stringResource(R.string.settings_clipboard),
                stringResource(R.string.settings_start_folder)
            ).joinToString(SUMMARY_JOIN),
            onOpen = { onOpen(Page.START) }
        ) {
            StartPage(
                settings = settings,
                onChange = onChange,
                onStartFolder = onStartFolder
            )
        }
    }

    /*
     * 'Avanzate', in fondo alla pagina (richiesta dell'utente): ci sono le funzioni che possono
     * fare danni, e il titolo è metà dell'avviso; chi scorre fin qui cerca qualcosa di insolito.
     * Con la memoria grafica ci sono le voci sull'app stessa: aggiornamento, miniature,
     * esportazione e avvisi (voce `3.40-04`).
     */
    Section(stringResource(R.string.settings_group_advanced)) {

        /*
         * La memoria grafica è qui e non fra le impostazioni della griglia: chi la cerca si
         * chiede come rendere l'app più leggera, non come vedere le cartelle. Tocca il modo in
         * cui il telefono tiene la memoria e può fare danni: il perché misurato è su
         * [Settings.gpuThumbs].
         */
        SwitchRow(
            label = stringResource(R.string.settings_gpu_thumbs),
            detail = stringResource(R.string.settings_gpu_thumbs_desc),
            checked = settings.gpuThumbs,
            onChange = { onChange(settings.copy(gpuThumbs = it)) }
        )

        /*
         * L'aggiornamento automatico è qui perché è una domanda sull'app stessa, come
         * miniature, esportazione e avvisi. Scelte C1 e D1 dell'utente: l'interruttore, acceso
         * di fabbrica, e `Controlla ora`, che cerca subito e dice com'è andata. Solo in AIV
         * GitHub (`Store.updates`).
         */
        if (Store.updates) {
            val updatesLabel = stringResource(R.string.settings_updates)
            val updatesDesc = stringResource(R.string.settings_updates_desc)
            val updatesCheck = stringResource(R.string.settings_updates_check)
            Searchable(updatesLabel, updatesDesc, updatesCheck) {
                SwitchRow(
                    label = updatesLabel,
                    detail = updatesDesc,
                    checked = settings.autoUpdate,
                    onChange = { onChange(settings.copy(autoUpdate = it)) }
                )
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { scope.launch { Updater.check(context, asked = true) } }) {
                        Text(updatesCheck)
                    }
                }
            }
        }

        val thumbsLabel = stringResource(R.string.settings_thumbs)
        val thumbsClear = stringResource(R.string.settings_thumbs_do)
        val thumbsMake = stringResource(R.string.settings_thumbs_gen)
        val thumbsSummary =
            when {
                thumbBytes < 0L -> ""
                thumbBytes == 0L -> stringResource(R.string.settings_thumbs_empty)
                else -> formatBytes(thumbBytes)
            }
        PageOfRows(
            label = thumbsLabel,
            summary = thumbsSummary,
            onOpen = { onOpen(Page.THUMBS) }
        ) {
            Searchable(thumbsLabel, thumbsClear, thumbsMake) {
                ThumbsCard(head = thumbsLabel, onClear = onClearThumbs, onGenerate = onGenerateThumbs)
            }
        }

        val backupLabel = stringResource(R.string.backup_title)
        val backupDesc = stringResource(R.string.backup_desc)
        val backupWords = listOf(
            stringResource(R.string.backup_export),
            stringResource(R.string.backup_import),
            stringResource(R.string.backup_lock),
            stringResource(R.string.backup_password_title)
        )
        Searchable(backupLabel, backupDesc, *backupWords.toTypedArray()) {
            PageRow(
                label = backupLabel,
                summary = backupDesc,
                onOpen = { onOpen(Page.BACKUP) },
                extra = backupWords
            )
        }

        val resetLabel = stringResource(R.string.settings_reset_hints)
        val resetDesc = stringResource(R.string.settings_reset_hints_desc)
        Searchable(resetLabel, resetDesc) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(text = resetLabel, style = MaterialTheme.typography.titleSmall)
                    Detail(resetDesc)
                }
                val done = stringResource(R.string.settings_reset_hints_done)
                TextButton(onClick = {
                    onResetHints()
                    Notices.say(done)
                }) { Text(stringResource(R.string.settings_reset_hints_do)) }
            }
        }
    }

    // Mentre si cerca il piede non c'è: il filetto e il logo chiudono la pagina intera, non
    // un elenco di risultati.
    if (LocalQuery.current.isNotBlank()) return

    // Il solo filetto della pagina: non separa due gruppi, dice che i gruppi sono finiti.
    HorizontalDivider(Modifier.padding(vertical = 12.dp))
    // ⚠️ **`fillMaxWidth` non è decorazione**: `Identity` centra i figli fra loro, ma senza
    // larghezza propria è largo quanto il testo più lungo, e questa colonna lo posa a sinistra.
    // Con tutta la larghezza il centro dei figli è il centro dello schermo.
    // L'icona è la stessa della schermata iniziale: il perché è accanto ad `AppIcon`.
    Identity(iconSize = 72.dp, modifier = Modifier.fillMaxWidth())

    Spacer(Modifier.height(24.dp))
}

/**
 * Tema dell'app e profondità dietro i menu e i pannelli (voce `3.27-03`).
 */
@Composable
private fun LookPage(
    settings: Settings,
    onChange: (Settings) -> Unit
) {
    // Il tema dell'app è prima di quello dello sfondo, che gli somiglia ma risponde a un'altra
    // domanda (vedi `UiTheme`): messo dopo, sembrerebbe una sua variante.
    Choices(
        label = stringResource(R.string.settings_ui_theme),
        detail = stringResource(R.string.settings_ui_theme_desc),
        options = UiTheme.entries,
        selected = settings.uiTheme,
        nameOf = {
            stringResource(
                when (it) {
                    UiTheme.SYSTEM -> R.string.settings_system
                    // ⚠️ Stringhe proprie e non quelle della tinta del fondo, che sono
                    // al femminile: riusarle darebbe 'Tema: Chiara'.
                    UiTheme.LIGHT -> R.string.settings_theme_light
                    UiTheme.DARK -> R.string.settings_theme_dark
                }
            )
        },
        onSelect = { onChange(settings.copy(uiTheme = it)) }
    )

    /*
     * La spiegazione dichiara il costo: chi sceglie deve saperlo, o leggerà la lentezza come
     * un difetto dell'app.
     * Pastiglie e non interruttori: le scelte si escludono, vedi [PanelDepth].
     * AIV Play non ha la voce: là resta solo l'ombra (`Store.fit`), e una scelta con una
     * risposta sola non è una scelta.
     */
    if (Store.fullSettings) Choices(
        label = stringResource(R.string.settings_depth),
        detail = stringResource(R.string.settings_depth_desc),
        options = PanelDepth.entries,
        selected = settings.panelDepth,
        nameOf = {
            stringResource(
                when (it) {
                    PanelDepth.BLUR -> R.string.settings_depth_blur
                    PanelDepth.SHADOW -> R.string.settings_depth_shadow
                    PanelDepth.NONE -> R.string.settings_depth_none
                }
            )
        },
        onSelect = { onChange(settings.copy(panelDepth = it)) }
    )

    // L'aspetto dei pulsanti principali, in fondo a 'Tema e dettagli grafici' per scelta
    // dell'utente (nota E del giro della `4.04`). La scelta dell'elemento è in cima a
    // 'Pulsanti e indicatori'.
    ButtonLookChoices(settings = settings, onChange = onChange)
}

/**
 * Che cosa fare all'avvio: appunti e cartella iniziale (voce `3.27-03`).
 */
@Composable
private fun StartPage(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onStartFolder: () -> Unit
) {
    SwitchRow(
        label = stringResource(R.string.settings_clipboard),
        detail = stringResource(R.string.settings_clipboard_desc),
        checked = settings.clipboardStart,
        onChange = { onChange(settings.copy(clipboardStart = it)) }
    )
    // L'interruttore e la riga della cartella sono un blocco solo per la ricerca: la riga sotto
    // non ha un titolo suo, e da sola mostrerebbe un nome di cartella senza contesto.
    val startLabel = stringResource(R.string.settings_start_folder)
    val startDesc = stringResource(R.string.settings_start_folder_desc)
    Searchable(startLabel, startDesc) {
        SwitchRow(
            label = startLabel,
            detail = startDesc,
            checked = settings.openAtStart,
            // ⚠️ Acceso senza una cartella scelta apre la scelta della cartella: acceso senza
            // effetto, l'interruttore sembrerebbe guasto.
            onChange = {
                if (it && settings.startFolder == null) onStartFolder()
                else onChange(settings.copy(openAtStart = it))
            }
        )
        ValueAndPick(
            value = settings.startFolderName.ifBlank {
                stringResource(R.string.settings_start_folder_none)
            },
            pick = stringResource(R.string.settings_start_folder_pick),
            onPick = onStartFolder
        )
    }
}

/**
 * La pagina 'Cartelle': come si vedono le cartelle di casa, e quali.
 *
 * La domanda che tiene insieme le sue voci è una sola, come si riconosce e come si vede una
 * cartella, e le voci sono oltre la soglia dell'utente. Contiene altre pagine: vedi [Page].
 */
@Composable
private fun FoldersPage(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onOpen: (Page) -> Unit
) {
    /*
     * L'ordine delle cartelle, prima voce della pagina: titolo, paragrafo e pastiglie sono
     * dell'utente. Nessuna pastiglia è accesa finché non sceglie, perché non c'è un valore di
     * fabbrica; la finestra che lo chiede è nella home (`FolderOrderDialog`).
     * ⚠️ **È la stessa scelta della finestra, con nomi diversi di proposito** (scelta
     * dell'utente).
     */
    Choices(
        label = stringResource(R.string.folder_order),
        detail = stringResource(R.string.folder_order_desc),
        options = FolderOrder.entries,
        selected = settings.folderOrder,
        nameOf = { stringResource(it.chip()) },
        onSelect = { onChange(settings.copy(folderOrder = it)) }
    )

    /*
     * La data del Feed, sotto 'Disposizione in vista griglia': titolo, paragrafo e pastiglie
     * sono dell'utente. È la stessa scelta delle opzioni di visualizzazione del Feed, con le
     * stesse due pastiglie; qui il paragrafo dice che cos'è il Feed, perché è il posto in cui
     * ne legge chi non l'ha mai aperto.
     */
    Choices(
        label = stringResource(R.string.feed_order),
        detail = stringResource(R.string.feed_order_desc),
        options = FeedDate.entries,
        selected = settings.feedDate,
        nameOf = { stringResource(it.chip()) },
        onSelect = { onChange(settings.copy(feedDate = it)) }
    )

    /*
     * Gli elementi dell'intestazione di una cartella (risposta a `d-frontespizio`): pastiglie
     * indipendenti, cioè tutte le combinazioni sono ammesse, e per questo [Toggles] e non
     * [Choices]. Una voce sola e non una per pastiglia, perché rispondono alla stessa domanda
     * (`Rules.md` § '⚙️ Dove va un'impostazione, e chi la deve trovare'), e con le cartelle e non
     * in 'Aspetto', perché la domanda nomina una cartella.
     * La ricerca le trova per nome: [Toggles] passa i nomi delle pastiglie a `shown`.
     * AIV Play ha le sole 'Info' e 'Seleziona tutti': il gradiente è sempre acceso e il titolo
     * graziato non c'è (`Store.fit`). Le pastiglie sono un elenco di [Chip], così nome, valore e
     * tocco viaggiano insieme in tutte e due le varianti.
     */
    val frontChips = listOfNotNull(
        Chip(stringResource(R.string.settings_front_wash), settings.frontWash) {
            it.copy(frontWash = !it.frontWash)
        }.takeIf { Store.fullSettings },
        Chip(stringResource(R.string.settings_front_serif), settings.frontSerif) {
            it.copy(frontSerif = !it.frontSerif)
        }.takeIf { Store.fullSettings },
        Chip(stringResource(R.string.settings_front_facts), settings.frontFacts) {
            it.copy(frontFacts = !it.frontFacts)
        },
        Chip(stringResource(R.string.pick_all), settings.frontPickAll) {
            it.copy(frontPickAll = !it.frontPickAll)
        }
    )
    Toggles(
        label = stringResource(R.string.settings_front),
        detail = stringResource(R.string.settings_front_desc),
        names = frontChips.map { it.name },
        on = frontChips.map { it.on },
        onFlip = { at -> onChange(frontChips[at].flip(settings)) }
    )

    /*
     * Dove si vede il colore di una cartella: la domanda è come si riconosce una cartella nella
     * schermata iniziale, quindi è con le cartelle (trasloco approvato dall'utente, con la
     * stessa chiave e lo stesso testo). Viene dopo l'intestazione: quella parla della cartella
     * aperta, questa della stessa cartella vista da fuori.
     * Pastiglie e non interruttori, come la profondità dei pannelli: gli stili si escludono.
     * Ha una gemella nel dialogo delle opzioni (`ColourChips` in `FolderScreen.kt`), con gli
     * stessi nomi dalla stessa funzione.
     * AIV Play non ha la voce: là c'è il solo 'Nome', sempre acceso (`Store.fit`), e la gemella
     * del dialogo sparisce con lei.
     */
    if (Store.fullSettings) Choices(
        label = stringResource(R.string.settings_colour),
        detail = stringResource(R.string.settings_colour_desc),
        options = FolderColour.entries,
        selected = settings.folderColour,
        nameOf = { stringResource(it.label()) },
        onSelect = { onChange(settings.copy(folderColour = it)) }
    )

    /*
     * Le opzioni di visualizzazione: le voci su come si presentano le viste della home sono una
     * domanda sola e oltre la soglia, quindi dietro un tocco. Una scelta da guardare ha la sua
     * casa qui e la scorciatoia nel dialogo della home, come le colonne, e la ricerca delle
     * impostazioni la trova.
     * ⚠️ **Il titolo è la stessa stringa del dialogo**, di proposito: dice che il dialogo è una
     * scorciatoia alla stessa scelta, non un secondo posto in cui vive.
     */
    PageOfRows(
        label = stringResource(R.string.view_options),
        // Composto dai nomi che la pagina usa come titolini: nessuna stringa nuova, e se un
        // titolino cambia cambia anche il riepilogo.
        summary = listOfNotNull(
            stringResource(R.string.view_grid),
            stringResource(R.string.view_list),
            // ⚠️ `hub_view_tree` ('Cartelle di sistema') e non `view_tree` ('Cartelle'), che
            // ripeterebbe il titolo di questa sezione. Non c'è in AIV Play (`Store.views`).
            stringResource(R.string.hub_view_tree).takeIf { FolderView.TREE in Store.views }
        ).joinToString(SUMMARY_JOIN),
        onOpen = { onOpen(Page.VIEWS) }
    ) { ViewOptionsPage(settings = settings, onChange = onChange) }

    // La scelta del modo resta raggiungibile anche quando i due elenchi sono vuoti.
    PageRow(
        label = stringResource(R.string.folder_selection),
        summary = stringResource(if (settings.folderMode == FolderMode.INCLUDED)
            R.string.folder_mode_included else R.string.folder_mode_excluded),
        onOpen = { onOpen(Page.HIDDEN) },
        extra = listOf(stringResource(R.string.folder_selection_desc),
            stringResource(R.string.folder_mode_included), stringResource(R.string.folder_mode_excluded)) +
            (settings.includedFolders + settings.folderSelection.excluded).map(::hiddenShown).sorted()
    )

    /*
     * Una sotto-pagina perché è un elenco che cresce, con comandi per riga (`Rules.md` § '⚙️ Dove
     * va un'impostazione, e chi la deve trovare'), e in fondo per scelta dell'utente. La ricerca
     * la trova per le parole della pagina e per le cartelle che contiene, come le cartelle
     * incluse ed escluse.
     */
    PageRow(
        label = stringResource(R.string.favourite_folders),
        summary = if (settings.favouriteFolders.isEmpty()) stringResource(R.string.favourite_folders_none)
        else settings.favouriteFolders.joinToString(SUMMARY_JOIN) { hiddenName(it) },
        onOpen = { onOpen(Page.FAVOURITES) },
        extra = listOf(stringResource(R.string.favourite_folders_desc)) +
            settings.favouriteFolders.map(::hiddenShown)
    )
}

/**
 * Il nome di un [FolderOrder] sulla sua pastiglia, nelle impostazioni e nelle opzioni di
 * visualizzazione. La finestra che chiede l'ordine usa nomi più lunghi.
 */
internal fun FolderOrder.chip(): Int = when (this) {
    FolderOrder.ALPHA -> R.string.folder_order_alpha
    FolderOrder.RECENT -> R.string.folder_order_recent
}

/** Il nome di un [FeedDate] sulla sua pastiglia, nelle impostazioni e nelle opzioni: dell'utente. */
internal fun FeedDate.chip(): Int = when (this) {
    FeedDate.TAKEN -> R.string.feed_date_taken
    FeedDate.MODIFIED -> R.string.feed_date_modified
}

/**
 * 'Cartelle preferite': le cartelle preferite nell'ordine in cui le mostrano la griglia e
 * l'elenco (richiesta dell'utente).
 *
 * Le maniglie sono quelle dei campi info, cioè [Reorderable], come le ha chieste l'utente, e lo
 * scorrimento della pagina arriva al trascinamento.
 * ⚠️ **'Aggiungi cartella' segue il contenuto finché l'elenco è vuoto, poi è una fascia fissa in
 * fondo** (nota A dell'utente): scorre solo l'elenco, quindi il tasto resta sempre in vista,
 * come la fascia fissa di 'Esporta e importa'. Il dialogo delle destinazioni è quello delle
 * nascoste: una cartella già presente non si aggiunge due volte.
 * La pagina vuota centra la sua riga e la stacca dalla spiegazione: senza altro sotto, quella
 * riga è il contenuto della pagina.
 * La × toglie subito una cartella, senza chiedere: una preferita non nasconde niente e torna
 * con una pressione lunga, quindi una conferma non proteggerebbe niente.
 * Gli stessi pezzi compongono [FavouritesSheet], che le opzioni di visualizzazione aprono sopra
 * la home: elenco e tasto sono scritti una volta, e cambia solo la cornice.
 */
@Composable
private fun ColumnScope.FavouriteFolders(list: List<String>, onList: (List<String>) -> Unit) {
    Detail(stringResource(R.string.favourite_folders_desc))
    if (list.isEmpty()) {
        FavouritesNone()
    } else {
        // Lo scorrimento dell'elenco è quello che il trascinamento muove vicino ai bordi (vedi
        // [Reorderable]).
        val scroll = rememberScrollState()
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll)) {
            FavouritesList(list, onList, scroll)
        }
    }
    FavouritesAdd(list, onList, Modifier.padding(top = if (list.isEmpty()) 0.dp else 12.dp))
}

/**
 * 'Cartelle preferite' sopra la home, dalla pressione lunga sul pulsante principale (nota B
 * dell'utente, mockup `homeLongPress.png`).
 *
 * ⚠️ **Un pannello e non un salto nelle impostazioni**, per la ragione scritta su [MarkSheet]:
 * Indietro deve riportare alla home di partenza, non risalire la pila delle impostazioni. Il
 * pannello scorre da sé, quindi l'elenco non ha uno scorrimento suo, e 'Aggiungi cartella' è il
 * suo piede fisso.
 */
@Composable
internal fun FavouritesSheet(list: List<String>, onList: (List<String>) -> Unit, onDismiss: () -> Unit) {
    Sheet(
        title = stringResource(R.string.favourite_folders),
        onDismiss = onDismiss,
        foot = { FavouritesAdd(list, onList) }
    ) {
        Detail(stringResource(R.string.favourite_folders_desc))
        if (list.isEmpty()) FavouritesNone() else FavouritesList(list, onList, scroll = null)
    }
}

/** La riga della pagina vuota: centrata, e staccata dalla spiegazione sopra. */
@Composable
private fun FavouritesNone() {
    Text(
        text = stringResource(R.string.favourite_folders_none),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
    )
}

/** Le preferite con le loro maniglie e la loro ×. */
@Composable
private fun FavouritesList(list: List<String>, onList: (List<String>) -> Unit, scroll: ScrollState?) {
    Reorderable(
        items = list,
        scroll = scroll,
        onMove = { da, a -> onList(list.moved(da, a)) }
    ) { entry, _ ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = HANDLE_ROOM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    camelBreak(hiddenName(entry)),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(hiddenShown(entry), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { onList(list - entry) }) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.favourite_remove))
            }
        }
    }
}

/** 'Aggiungi cartella', col dialogo delle destinazioni che apre. */
@Composable
private fun FavouritesAdd(list: List<String>, onList: (List<String>) -> Unit, modifier: Modifier = Modifier) {
    var picking by remember { mutableStateOf(false) }
    FilledTonalButton(onClick = { picking = true }, modifier = modifier.fillMaxWidth()) {
        Text(stringResource(R.string.folder_add))
    }
    if (picking) {
        // AIV GitHub apre direttamente l'albero, come per le cartelle incluse; AIV Play non legge
        // l'albero (`Store.files`) e mostra le cartelle con immagini, che sono comunque le sole
        // che una preferita può portare in cima.
        CompositionLocalProvider(
            LocalDestLook provides DestLook(
                view = if (Store.files) FolderView.TREE else FolderView.GRID,
                folderMode = FolderMode.EXCLUDED
            )
        ) {
            DestinationDialog(
                action = R.string.favourite_add,
                // Scegliere una preferita non sposta file, quindi qui non c'è nessuna origine.
                from = emptySet(),
                onDismiss = { picking = false },
                onPick = { dir ->
                    val path = portablePath(dir.absolutePath)
                    if (path !in list) onList(list + path)
                    picking = false
                },
                alreadyListed = list.toSet()
            )
        }
    }
}

/**
 * La pagina 'Visualizzatore': come si guarda un'immagine aperta.
 *
 * Lo sfondo è qui perché che cosa c'è dietro un'immagine trasparente è una domanda sul
 * visualizzatore, non sull'aspetto dell'app; la riproduzione dei video perché dice che cosa fa
 * un video quando lo si apre.
 * 'Adattamento e zoom' resta una pagina a sé, dentro questa: le sue voci sono delicate, e il
 * tocco in più è una protezione.
 */
@Composable
private fun ViewerPage(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onOpen: (Page) -> Unit
) {
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

    /*
     * ⚠️ **Le voci delicate sono in una sotto-pagina, per scelta dell'utente**: sono le sole
     * che cambiano il modo in cui un'immagine è misurata, e sbagliarle rende ogni immagine
     * diversa da come ci si aspetta senza che si capisca perché.
     */
    PageOfRows(
        label = stringResource(R.string.settings_zoom_page),
        // Il riepilogo si compone dai titoli delle voci: se una voce entra, esce o cambia nome,
        // il riepilogo la segue (vedi la nota su [SettingsScreen]).
        summary = listOf(
            stringResource(R.string.settings_fit_grow),
            stringResource(R.string.settings_zoom_max),
            stringResource(R.string.settings_scale_mode),
            stringResource(R.string.settings_zoom_menu)
        ).joinToString(SUMMARY_JOIN),
        onOpen = { onOpen(Page.ZOOM) }
    ) { ZoomAndFit(settings = settings, onChange = onChange) }

    /*
     * Ha una spiegazione sua: staccata da 'Sfoglia solo le immagini', di cui è il rovescio,
     * senza testo resterebbe una riga senza contesto. Il perché sia spenta di fabbrica è su
     * `Settings.clipAutoplay`.
     */
    SwitchRow(
        label = stringResource(R.string.settings_clip_autoplay),
        detail = stringResource(R.string.settings_clip_autoplay_desc),
        checked = settings.clipAutoplay,
        onChange = { onChange(settings.copy(clipAutoplay = it)) }
    )

    // Le due voci di navigazione, qui e non in una pagina loro (voce `3.40-04`).
    SwitchRow(
        label = stringResource(R.string.settings_images_only),
        detail = stringResource(R.string.settings_images_only_desc),
        checked = settings.imagesOnly,
        onChange = { onChange(settings.copy(imagesOnly = it)) }
    )
    // AIV Play non ha la voce: là la sequenza va sempre dalla più recente (`Store.fit`).
    if (Store.fullSettings) SwitchRow(
        label = stringResource(R.string.settings_reverse_order),
        detail = stringResource(R.string.settings_reverse_order_desc),
        checked = settings.reverseSequence,
        onChange = { onChange(settings.copy(reverseSequence = it)) }
    )
}

/**
 * La pagina 'Informazioni': che cosa l'app dice di un file.
 *
 * L'interruttore della barra delle info, l'elenco dei dati, il contatore dei fotogrammi e il peso
 * di una selezione rispondono alla stessa domanda nello stesso posto.
 * L'elenco dei dati resta una pagina a sé, dentro questa: è un elenco con comandi riga per riga,
 * e la ricerca lo copre con `extra`.
 */
@Composable
private fun InfoPage(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onOpen: (Page) -> Unit
) {
    // Le risorse e non `stringResource`, che è componibile: la riga dei campi ricorda i loro
    // nomi in un `remember`, dove si legge una risorsa con l'oggetto delle risorse.
    val res = LocalResources.current

    /*
     * Interruttore e riga subordinata (riscontro `chip-colonna` dell'utente): 'Barra delle
     * info' con l'interruttore, e sotto 'Posizione' con le pastiglie 'In alto' e 'In basso'.
     * La riga è [InfoSideRow], in un file suo perché è identica in due posti.
     */
    SwitchRow(
        label = stringResource(R.string.settings_info_visible),
        detail = null,
        checked = settings.infoVisible,
        onChange = { onChange(settings.copy(infoVisible = it)) }
    )

    /*
     * Le pastiglie si toccano solo a barra accesa (stessa richiesta), ma il valore non si
     * perde: `infoPosition` è un campo suo, e il lato scelto si ritrova riaccendendo la barra.
     * La ricerca le trova anche spente: nasconderle direbbe che l'impostazione non esiste.
     */
    if (shown(
            stringResource(R.string.settings_info_position),
            null,
            infoSideName(InfoPosition.TOP),
            infoSideName(InfoPosition.BOTTOM)
        )
    ) {
        InfoSideRow(
            selected = settings.infoPosition,
            enabled = settings.infoVisible,
            onSelect = { onChange(settings.copy(infoPosition = it)) },
            /*
             * ⚠️ **Niente rientro a sinistra** (riscontro `barra-impostazioni`): la
             * subordinazione la dicono il corpo leggero del titolo e le pastiglie che si
             * spengono con l'interruttore. Resta il solo distacco in alto fra le due righe.
             */
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    /*
     * Accanto alla barra delle info e non fra le voci dell'editor: tutte e due dicono che cosa
     * si vede sopra l'immagine mentre la si guarda, e chi cerca l'una trova l'altra.
     */
    SwitchRow(
        label = stringResource(R.string.settings_anim_counter),
        detail = stringResource(R.string.settings_anim_counter_desc),
        checked = settings.animCounter,
        onChange = { onChange(settings.copy(animCounter = it)) }
    )

    PageRow(
        label = stringResource(R.string.settings_facts),
        summary = pluralStringResource(
            R.plurals.settings_facts_count,
            settings.factRows.size,
            settings.factRows.size
        ),
        onOpen = { onOpen(Page.FACTS) },
        /*
         * ⚠️ **Questa pagina non si appiattisce nella ricerca**: è un elenco con comandi per
         * riga che lavorano sull'ordine intero, e in un elenco filtrato sposterebbero un campo
         * in una posizione che non si vede. I nomi dei campi entrano fra i testi che la ricerca
         * confronta su questa riga: chi cerca 'fotocamera' trova la riga che porta a quella
         * voce, senza stringhe nuove.
         * Ricordati, o si ricostruirebbero a ogni tocco su una voce del pannello. La chiave è
         * l'oggetto delle risorse e non il `Context`, perché è lui che cambia con la lingua.
         */
        extra = remember(res, settings.factOrder) {
            settings.factOrder.map { res.getString(it.label) }
        }
    )

    // Il costo che questo interruttore toglie è descritto su `Settings.pickWeight`.
    SwitchRow(
        label = stringResource(R.string.settings_pick_weight),
        detail = stringResource(R.string.settings_pick_weight_desc),
        checked = settings.pickWeight,
        onChange = { onChange(settings.copy(pickWeight = it)) }
    )

}

/**
 * Le voci del cestino: se è acceso, e per quanto si tengono i file. Ultima sotto-pagina di
 * 'Gestione dei file' (voce `3.40-04`).
 */
@Composable
private fun BinPage(
    settings: Settings,
    onChange: (Settings) -> Unit
) {
    SwitchRow(
        label = stringResource(R.string.settings_bin),
        detail = stringResource(R.string.settings_bin_desc),
        checked = settings.binOn,
        onChange = { onChange(settings.copy(binOn = it)) }
    )
    Choices(
        label = stringResource(R.string.settings_bin_sweep),
        detail = stringResource(R.string.settings_bin_sweep_desc),
        options = BinKeep.entries,
        selected = settings.binKeep,
        nameOf = {
            stringResource(
                when (it) {
                    BinKeep.NEVER -> R.string.bin_sweep_never
                    BinKeep.WEEK -> R.string.bin_sweep_week
                    BinKeep.MONTH -> R.string.bin_sweep_month
                    BinKeep.QUARTER -> R.string.bin_sweep_quarter
                }
            )
        },
        onSelect = { onChange(settings.copy(binKeep = it)) }
    )
}

/**
 * La pagina 'Pulsanti e indicatori': come si presentano i comandi che si usano e i segni che
 * li accompagnano.
 */
@Composable
private fun ControlsPage(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onOpen: (Page) -> Unit
) {
    // Le risorse e non `stringResource`, che è componibile: la riga dei tasti ricorda i loro
    // nomi in un `remember`, dove si legge una risorsa con l'oggetto delle risorse.
    val res = LocalResources.current

    /*
     * L'elemento interattivo principale, prima voce della pagina per scelta dell'utente (nota E
     * del giro della `4.04`). Sul tablet non c'è, perché in verticale ha già la sua pillola: lo
     * decide [pillOffered], lo stesso conto di [pillMode].
     */
    if (pillOffered(LocalConfiguration.current.smallestScreenWidthDp)) {
        MainControlChoices(settings = settings, onChange = onChange)
    }

    Choices(
        label = stringResource(R.string.settings_hand),
        /*
         * La voce dice da che parte è il FAB. ⚠️ **La chiave resta `hand`**, dal tempo in cui
         * diceva quale mano si usa: la domanda ha cambiato forma ma non verso, quindi chi aveva
         * scelto la sinistra ritrova la sinistra, mentre una chiave nuova avrebbe rimesso il
         * valore di fabbrica.
         */
        detail = stringResource(R.string.settings_hand_desc),
        options = Hand.entries,
        selected = settings.hand,
        nameOf = {
            stringResource(
                when (it) {
                    Hand.RIGHT -> R.string.settings_right
                    Hand.LEFT -> R.string.settings_left
                }
            )
        },
        onSelect = { onChange(settings.copy(hand = it)) }
    )

    /*
     * La domanda della famiglia è come si presentano i comandi che si usano: da che parte sono,
     * come si leggono, in che ordine. È dietro una porta perché contiene già un'altra famiglia,
     * l'ordine dei pulsanti.
     */
    SwitchRow(
        label = stringResource(R.string.settings_labels),
        detail = stringResource(R.string.settings_labels_desc),
        checked = settings.padLabels,
        onChange = { onChange(settings.copy(padLabels = it)) }
    )

    /*
     * L'indicatore dell'ultimo elemento visto, qui per richiesta dell'utente (punto D del giro
     * accorpato): non è un comando, ma il titolo della pagina nomina gli indicatori.
     * Senza spiegazione: il titolo dice che cos'è, e il resto lo dicono i nomi delle scelte.
     * AIV Play non ha né il segno né la voce (`Store.marksLast`).
     */
    if (Store.marksLast) Choices(
        label = stringResource(R.string.settings_last_mark),
        detail = null,
        options = LastMark.entries,
        selected = settings.lastMark,
        nameOf = {
            stringResource(
                when (it) {
                    LastMark.FRAME -> R.string.settings_last_mark_frame
                    LastMark.CORNER -> R.string.settings_last_mark_corner
                    LastMark.DOT -> R.string.settings_last_mark_dot
                }
            )
        },
        onSelect = { onChange(settings.copy(lastMark = it)) }
    )

    /*
     * ⚠️ **La ricerca trova i tasti da qui**, come i campi delle info: la pagina è un elenco con
     * maniglie che lavorano sull'ordine intero, quindi non si appiattisce, e i nomi dei tasti
     * arrivano qui come testi in più.
     */
    PageRow(
        label = stringResource(R.string.settings_buttons),
        summary = null,
        onOpen = { onOpen(Page.BUTTONS) },
        // Ricordati come i nomi dei campi delle info, per la stessa ragione.
        // I moduli ci sono anche dove l'editor completo non c'è: toglierli darebbe una ricerca
        // che risponde in modo diverso secondo la versione di Android.
        extra = remember(res) {
            (MENU_KEYS + PICK_KEYS + TURN_KEYS + STEP_KEYS + MOD_KEYS).distinct()
                .map { res.getString(it.label()) }
        }
    )

}

/**
 * La pagina 'Editor e salvataggio': con che cosa si modifica un file e con che nome si salva.
 *
 * Il titolo non ripete quello della sezione da cui si arriva, 'Gestione dei file', che
 * contiene anche altre porte.
 */
@Composable
private fun EditingPage(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onChooseEditor: () -> Unit,
    onOpen: (Page) -> Unit
) {
    /*
     * ⚠️ **È lo stesso selettore del primo utilizzo** (richiesta dell'utente): la finestra è
     * una sola, [EditorPicker], aperta dal modello (`chooseEditor`). Due finestre gemelle
     * divergerebbero alla prima voce aggiunta.
     */
    val editorLabel = stringResource(R.string.settings_editor)
    val editorDesc = stringResource(R.string.settings_editor_desc)
    // AIV Play ha solo i suoi editor (`ViewerViewModel.edit`): non c'è niente da scegliere.
    if (Store.files) Searchable(editorLabel, editorDesc) {
        val context = LocalContext.current
        val noEditor = stringResource(R.string.settings_editor_none)
        // Ricordato e non chiesto a ogni disegno, perché leggerlo interroga il `PackageManager`;
        // la chiave è la scelta. ⚠️ Fuori dal thread principale (rilievo B13): finché il
        // sistema non risponde la riga resta vuota, non 'nessuna'.
        val editorFound by produceState<String?>("", settings.editorApp) {
            value = withContext(Dispatchers.IO) { Editors.labelOf(context, settings.editorApp) }
        }
        val editorName = editorFound ?: noEditor
        EditorChoice(editorLabel, editorDesc, editorName, onChooseEditor)
    }

    /*
     * La copia di sicurezza vale per tutti gli editor, anche quelli di altre app: la copia si
     * fa prima di lanciarli. È sotto la scelta dell'app perché è la stessa faccenda.
     * Non c'è in AIV Play, i cui editor non scrivono mai sull'originale (`ImageEdit.outDir`).
     */
    if (Store.files) {
        SwitchRow(
            label = stringResource(R.string.settings_editor_backup),
            detail = stringResource(R.string.settings_editor_backup_desc),
            checked = settings.editorBackup,
            onChange = { onChange(settings.copy(editorBackup = it)) }
        )
    }

    /*
     * La qualità è nella famiglia dell'editor perché risponde alla sua domanda, che cosa
     * succede quando si modifica un'immagine; 'Rinomina e download' risponde a che nome ha il
     * file e dove finisce. Là il nome, qui i pixel.
     * ⚠️ **Riguarda il solo editor completo**, come dice la sua spiegazione: l'editor semplice
     * non ricomprime quando può evitarlo.
     */
    if (advancedEditorAvailable()) {
        Choices(
            label = stringResource(R.string.settings_quality),
            detail = stringResource(R.string.settings_quality_desc),
            options = Quality.entries,
            selected = settings.editorQuality,
            nameOf = {
                stringResource(
                    when (it) {
                        Quality.HIGH -> R.string.quality_high
                        Quality.MAX -> R.string.quality_max
                        Quality.LOSSLESS -> R.string.quality_lossless
                    }
                )
            },
            onSelect = { onChange(settings.copy(editorQuality = it)) }
        )
    }

    /*
     * Gli stili dell'editor completo (nota dell'utente sulla voce `preset-salva`).
     * ⚠️ **Non si appiattiscono nella ricerca**, come l'elenco dei campi info: è un elenco che
     * si riordina sull'ordine intero, quindi i nomi dei suoi comandi entrano fra i testi che la
     * ricerca confronta su questa riga.
     * C'è dove c'è l'editor completo, come la qualità: gli stili sono suoi.
     */
    if (advancedEditorAvailable()) {
        PageRow(
            label = stringResource(R.string.settings_styles),
            summary = stringResource(R.string.settings_styles_desc),
            onOpen = { onOpen(Page.STYLES) },
            extra = listOf(
                stringResource(R.string.settings_styles_reset),
                stringResource(R.string.settings_styles_export),
                stringResource(R.string.settings_styles_import)
            )
        )
    }

    /*
     * La filigrana (voce `filigrana`, scelta dell'utente): la sua domanda è quella di questa
     * pagina, che cosa succede quando si modifica un'immagine.
     * La ricerca la copre appiattendo il corpo con `PageOfRows`, come le altre pagine fatte di
     * righe: dentro non c'è un elenco che si riordina.
     */
    PageOfRows(
        label = stringResource(R.string.settings_mark),
        summary = stringResource(R.string.settings_mark_desc),
        onOpen = { onOpen(Page.MARK) }
    ) { MarkPage(settings = settings, onChange = onChange) }

}

/**
 * La riga dell'editor di immagini: il titolo col tasto 'Imposta', la spiegazione e l'app in
 * vigore.
 *
 * Il tasto è sulla riga del titolo (nota dell'utente del giro della `4.99`): sotto la
 * spiegazione non sarebbe chiaro a che cosa si riferisce. La cartella d'avvio tiene la forma di
 * [ValueAndPick], perché la nota nominava questa riga sola.
 * ⚠️ **'Imposta' e il nome finiscono sulla stessa verticale** (nota su `5.01-02`), e la riga
 * dell'app è tutta a destra: il tasto si sposta della larghezza del margine interno del
 * `TextButton`, così il bersaglio resta intero e il testo arriva al bordo.
 */
@Composable
internal fun EditorChoice(label: String, detail: String, current: String, onChoose: () -> Unit) {
    val margine = ButtonDefaults.TextButtonContentPadding.calculateEndPadding(LocalLayoutDirection.current)
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onChoose, modifier = Modifier.offset(x = margine)) {
                Text(stringResource(R.string.settings_editor_pick))
            }
        }
        Detail(detail)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)
        ) {
            Text(
                text = stringResource(R.string.settings_editor_app),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = current, style = MaterialTheme.typography.titleSmall)
        }
    }
}

/**
 * Il guscio comune a tutte le pagine: la colonna che scorre, la freccia e il titolo.
 *
 * ⚠️ Il titolo prende `weight`: 'Informazioni sul file' in tedesco e in tamil è quasi il doppio,
 * e senza peso una `Row` lo taglierebbe e non lo manderebbe a capo.
 */
@Composable
private fun Shell(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    scroll: ScrollState = rememberScrollState(),
    /**
     * Se la colonna scorre tutta intera.
     *
     * Spento, il contenuto prende l'altezza, ed è l'unico modo di avere un figlio con `weight`:
     * in una colonna che scorre lo spazio avanzato non esiste. Lo spegne la pagina che scorre
     * per conto suo dentro un figlio.
     */
    scrolls: Boolean = true,
    /**
     * Se in fondo alla riga del titolo compare il numero di versione.
     *
     * Solo nella pagina principale, accanto al titolo (richiesta dell'utente): su ogni
     * sotto-pagina sarebbe la stessa riga ripetuta, un ornamento e non una firma.
     */
    version: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .then(if (scrolls) Modifier.verticalScroll(scroll) else Modifier)
                .padding(horizontal = PAGE_SIDE, vertical = 12.dp),
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
                    // La linea di base serve all'allineamento col numero di versione, qui sotto.
                    modifier = Modifier.weight(1f).alignByBaseline().heading()
                )
                if (version) {
                    /*
                     * ⚠️ **Il numero arriva da `BuildConfig` e non da una stringa**: la fonte
                     * unica del `versionName` è `app/build.gradle.kts`, e una stringa scritta qui
                     * sarebbe un secondo posto da tenere d'accordo.
                     * 'AIV' e non 'v' (richiesta dell'utente): dice di che cosa è la versione, in
                     * un pannello che non nomina l'app altrove. È il nome proprio, uguale in tutte
                     * le lingue, quindi non è una stringa da tradurre.
                     * Discreto: il corpo più piccolo del tema, l'inchiostro tenue e il monospazio,
                     * che dà a tutte le cifre la stessa larghezza.
                     */
                    Text(
                        text = "AIV " + BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = VERSION_FADE),
                        /*
                         * Appoggiato sulla linea di base del titolo (riscontro dell'utente):
                         * centrato in verticale, un corpo piccolo accanto a uno grande sembra
                         * sospeso. ⚠️ **`alignByBaseline` va sui due testi**: la riga allinea solo
                         * i figli che lo chiedono. La freccia resta centrata, perché un glifo non
                         * ha linea di base.
                         */
                        modifier = Modifier.alignByBaseline().padding(end = VERSION_END)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

/**
 * Il titolo di un gruppo.
 *
 * `titleMedium`, fra il titolo della pagina (`headlineSmall`) e il nome di un'impostazione
 * (`titleSmall`): così il gruppo si legge come un livello e non come una voce, anche per chi non
 * distingue la tinta.
 * ⚠️ **Il colore è [accentInk] e non `primary`**: l'accento vero come colore di un testo non si
 * legge (2,43 su questo fondo, contro i 4,5 che servono).
 * È un'intestazione, cioè un punto in cui un lettore di schermo si ferma navigando per
 * intestazioni.
 */
@Composable
private fun Group(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = accentInk(),
        modifier = Modifier.heading().padding(top = 24.dp, bottom = 4.dp)
    )
}

/**
 * Una sezione: il suo titolo, e le voci che ci vivono dentro.
 *
 * ⚠️ **È un contenitore e non una riga a sé** ([Group]) per la ricerca: in Compose un valore
 * arriva ai figli e non ai fratelli, quindi solo così le voci sanno in che sezione vivono
 * ([LocalSection]).
 * Mentre si cerca i titoli spariscono, perché i risultati vengono da sezioni mescolate. Il
 * titolo torna quando la parola cercata è la sua: allora le righe che compaiono sono tutte
 * sue, e il titolo spiega perché sono lì.
 */
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    if (LocalQuery.current.isBlank() || shown(title)) Group(title)
    CompositionLocalProvider(LocalSection provides title) { content() }
}

/**
 * Una riga che mostra il **valore in vigore** di una scelta che si fa altrove, col tasto che la
 * apre.
 *
 * Scritta una volta perché le righe che la usano devono restare identiche: due disposizioni per
 * lo stesso mestiere farebbero cercare il tasto due volte. Quello che cambia sono il testo e il
 * tasto, cioè i parametri.
 */
@Composable
private fun ValueAndPick(value: String, pick: String, onPick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onPick) { Text(pick) }
    }
}

/**
 * I testi delle impostazioni con l'enfasi: un termine fra apici diventa grassetto, e
 * `ATTENZIONE` diventa **Attenzione** (voci `3.26-03` e `3.37-03`), in tutte le sezioni e
 * sotto-pagine. `'FAB'` resta fra apici, per scelta dell'utente.
 * Le elisioni italiane (`l'app`, `un'immagine`) non sono coppie di apici.
 */
internal fun emphasizeSettingsCopy(raw: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < raw.length) {
        if (raw.startsWith("ATTENZIONE", i)) {
            val from = length
            append("Attenzione")
            addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), from, length)
            i += "ATTENZIONE".length
            continue
        }
        if (raw[i] == '\'') {
            val before = if (i > 0) raw[i - 1] else ' '
            // Elisione: lettera subito prima (l'app, un'immagine, dell'editor).
            if (before.isLetter()) {
                append('\'')
                i++
                continue
            }
            val close = raw.indexOf('\'', i + 1)
            if (close > i + 1) {
                val term = raw.substring(i + 1, close)
                if (term == "FAB") {
                    append("'FAB'")
                    i = close + 1
                    continue
                }
                if (term.length in 1..48 && '\n' !in term) {
                    val from = length
                    append(term)
                    addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), from, length)
                    i = close + 1
                    continue
                }
            }
        }
        append(raw[i])
        i++
    }
}

/**
 * La spiegazione sotto un titolo, nello stile che tutte le voci usano.
 *
 * Non è privata perché la usa anche la Cronologia: una sua copia divergerebbe. Il `modifier`
 * serve alla riga di un file ripristinato, che le passa un peso.
 */
@Composable
internal fun Detail(text: String, modifier: Modifier = Modifier) {
    Text(
        text = emphasizeSettingsCopy(text),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/**
 * Una riga che apre una sotto-pagina.
 *
 * ⚠️ La freccia è `AutoMirrored`, come quella di Indietro: in persiano, urdu e arabo una pagina
 * si apre verso sinistra, e una freccia fissa a destra indicherebbe il verso sbagliato.
 * Nessuna descrizione per lo schermo: la riga intera è il comando, e il lettore legge già nome e
 * riepilogo.
 */
@Composable
private fun PageRow(
    label: String,
    /**
     * La riga di spiegazione sotto il titolo, e `null` per una riga che non ne ha.
     *
     * Può mancare, come per 'Ordine dei pulsanti' (riscontro dell'utente): quella pagina si
     * spiega da sé appena si apre.
     * ⚠️ **Senza riepilogo la riga resta nella ricerca grazie a [extra]**: una riga senza
     * riepilogo e senza `extra` si troverebbe col solo titolo.
     */
    summary: String?,
    onOpen: () -> Unit,
    /**
     * Le parole delle righe che vivono **dentro** la pagina, per la ricerca.
     *
     * Serve alle pagine che sono elenchi con comandi riga per riga, che non si appiattiscono
     * nella radice: la riga che apre la pagina risponde anche alle parole di dentro. Vuoto per
     * le altre.
     */
    extra: List<String> = emptyList()
) {
    if (!shown(label, summary ?: "", *extra.toTypedArray())) return
    Row(
        // ⚠️ `clickable` prima di `padding`: il tocco prende anche il margine, e la riga arriva
        // ai 48dp di bersaglio senza scriverli. [Modifier.bordo] prima ancora: l'alone arriva ai
        // fianchi dello schermo e non si ferma al rientro della pagina.
        modifier = Modifier
            .fillMaxWidth()
            .bordo()
            .clickable(onClick = onOpen)
            .padding(horizontal = PAGE_SIDE, vertical = ROW_HIGH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
            summary?.let { Detail(it) }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Una sotto-pagina fatta di righe: la riga che la apre, oppure il suo corpo mentre si cerca.
 *
 * ⚠️ **È la copertura della ricerca per le sotto-pagine**: `LocalQuery` è fornito nel solo ramo
 * della radice, e senza questo una voce dietro un tocco resterebbe fuori dalla ricerca. Funziona
 * perché il corpo si compone dentro il provider della radice e le righe si filtrano da sé; un
 * blocco scritto a mano si avvolge in [Searchable].
 * Sopra il corpo appiattito non c'è il titolo della pagina, come per [Group]: i risultati
 * vengono da posti mescolati. Le pagine che sono elenchi non si appiattiscono: la loro
 * copertura è `extra` di [PageRow].
 * Si annida da sé: una pagina dentro una pagina si appiattisce a sua volta. Lo presidia
 * `ImpostazioniTest`, che trova una voce di 'Adattamento e zoom', due livelli sotto.
 * ⚠️ **La riga resta quando è il titolo della pagina a corrispondere** (voce `imp-ricerca`),
 * perché nessuna voce di dentro contiene quella parola. Guarda il titolo e non il riepilogo:
 * il riepilogo nomina le voci di dentro, e cercando 'Sfondo' la porta del visualizzatore
 * prenderebbe il posto della voce vera.
 */
@Composable
private fun PageOfRows(
    label: String,
    summary: String,
    onOpen: () -> Unit,
    body: @Composable () -> Unit
) {
    if (LocalQuery.current.isBlank() || shown(label)) {
        PageRow(label = label, summary = summary, onOpen = onOpen)
    } else {
        // ⚠️ Il corpo di una pagina non appartiene alla sezione della sua porta: con la sezione
        // di fuori, cercarne il nome farebbe comparire queste voci senza dire da dove vengono.
        CompositionLocalProvider(LocalSection provides "") { body() }
    }
}

/**
 * I due comandi delle miniature memorizzate, 'Svuota la cache delle miniature' e 'Genera miniature',
 * con le loro conferme.
 *
 * Un componente solo per i due posti in cui compare: la sotto-pagina e, mentre si cerca, la
 * radice che la appiattisce ([PageOfRows]). [head] è il titolo, solo nella radice: nella
 * sotto-pagina lo dice già la testata.
 *
 * La specifica è dell'utente: due tasti senza introduzione, nel suo ordine, e il paragrafo
 * nelle conferme.
 * - Le conferme hanno il solo testo, senza titolo: la domanda è nel testo, e un titolo
 *   ripeterebbe il nome del tasto appena toccato.
 * - I loro tasti sono 'Sì' e 'Annulla', uguali nelle due finestre. ⚠️ **Il 'Sì' ha il colore di
 *   sempre e non quello dell'errore**: svuotare non fa danni, le miniature si rifanno da sé.
 * - Non sono modali vere (`Rules.md` § '👆 Che cosa fa il tocco FUORI da una finestra'): il
 *   tocco fuori vale 'Annulla', l'esito sicuro.
 * - 'Genera' c'è su ogni versione di Android: sotto Android 10 tutto passa dalla decodifica, che
 *   si tiene (`KeepingDecoderFactory`, in `Thumbs.kt`).
 * - I due tasti sono larghi uguali (`IntrinsicSize.Max`): con due larghezze si leggerebbero
 *   come comandi di peso diverso.
 *
 * 'Svuota' avvisa con la notifica di casa, come 'Ripristina gli avvisi': l'effetto si vede in
 * un'altra schermata, e un comando che non dà segno si preme due volte. 'Genera' lo dice alla
 * fine, dalla sua pagina ([Warmup]).
 * ⚠️ **È `internal` per il banco**, che la monta coi due comandi finti: dentro la schermata,
 * 'Sì' su 'Genera' farebbe partire una generazione vera.
 */
@Composable
internal fun ThumbsCard(
    head: String?,
    onClear: () -> Unit,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val done = stringResource(R.string.settings_thumbs_done)
    var asking by remember { mutableStateOf<ThumbsAsk?>(null) }
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        // `CenterVertically` dentro `spacedBy` mette il blocco in mezzo allo spazio avanzato:
        // nella radice, dove spazio avanzato non ce n'è, non fa niente.
        verticalArrangement = Arrangement.spacedBy(THUMBS_GAP, Alignment.CenterVertically)
    ) {
        if (head != null) Text(text = head, style = MaterialTheme.typography.titleSmall)
        Column(
            modifier = Modifier.width(IntrinsicSize.Max),
            verticalArrangement = Arrangement.spacedBy(THUMBS_SPLIT)
        ) {
            ThumbsButton(stringResource(R.string.settings_thumbs_do)) { asking = ThumbsAsk.CLEAR }
            ThumbsButton(stringResource(R.string.settings_thumbs_gen)) { asking = ThumbsAsk.GENERATE }
        }
    }
    asking?.let { chiesto ->
        val chiudi = { asking = null }
        AlertDialog(
            onDismissRequest = chiudi,
            modifier = Modifier.lowered(chiudi),
            properties = loweredWindow(chiudi),
            text = { Text(stringResource(chiesto.question)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        asking = null
                        when (chiesto) {
                            ThumbsAsk.CLEAR -> {
                                onClear()
                                Notices.say(done)
                            }
                            ThumbsAsk.GENERATE -> onGenerate()
                        }
                    }
                ) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = chiudi) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

/** Uno dei due tasti delle miniature: la stessa aria, e il testo centrato se va a capo. */
@Composable
private fun ThumbsButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, contentPadding = THUMBS_PAD, modifier = Modifier.fillMaxWidth()) {
        Text(text = label, textAlign = TextAlign.Center)
    }
}

/** Quale delle due conferme è in scena, con la sua domanda. */
private enum class ThumbsAsk(@StringRes val question: Int) {
    CLEAR(R.string.settings_thumbs_ask),
    GENERATE(R.string.settings_thumbs_gen_ask)
}

/**
 * L'aria fra il titolo e i due tasti, che c'è solo nella radice: abbastanza perché il titolo non
 * si legga come l'etichetta del primo tasto.
 */
private val THUMBS_GAP = 28.dp

/**
 * L'aria fra i due tasti: meno di quella dal titolo, perché sono una coppia, e abbastanza perché un
 * dito non prenda l'altro.
 */
private val THUMBS_SPLIT = 12.dp

/**
 * L'aria dentro i tasti delle miniature, più grande del valore di Material (richiesta
 * dell'utente): quello è calcolato per un'etichetta di una riga, e con due righe il testo si
 * legge stipato fra i bordi.
 *
 * ⚠️ **Sotto c'è più aria che sopra, e i 4dp sono misurati**: la spaziatura di riga di
 * `labelLarge` cade più sopra la prima riga che sotto l'ultima, e senza discendenti il testo
 * sembra più in basso del centro. Sulla schermata dell'utente lo scarto era di 11 px (83 sopra,
 * 72 sotto, su un tasto di 263), poco più di 3dp, arrotondati in su. L'aria simmetrica ne
 * riduceva solo il peso (voce `thumbs-pulsante`, non approvata).
 * Con un'etichetta su una riga sola la prima e l'ultima riga coincidono, e il numero resta
 * quello misurato.
 * `start`/`end` e non `horizontal`, perché `PaddingValues` non prende i fianchi insieme con i
 * due lati separati, e in una lingua che si scrive da destra i fianchi si scambiano.
 */
private val THUMBS_PAD =
    PaddingValues(start = 28.dp, top = 16.dp, end = 28.dp, bottom = 20.dp)

/**
 * Quanto è tenue il numero di versione accanto al titolo.
 *
 * 'Un po' in trasparenza', come ha chiesto l'utente: la riga si legge da chi la cerca e sparisce
 * per gli altri. Si applica all'inchiostro già attenuato delle spiegazioni.
 */
private const val VERSION_FADE = 0.55f

/**
 * Quanto rientra il numero di versione dal bordo destro della colonna.
 *
 * 12 e non zero (riscontro dell'utente): il numero deve distare dal bordo destro quanto la
 * freccia dal sinistro. La freccia è dentro un `IconButton` da 48dp con un glifo da 24, quindi
 * il suo inchiostro parte 12dp più in dentro del rientro della colonna, e il numero, che non ha
 * una scatola, quei 12dp li deve aggiungere.
 * ⚠️ **È il conto di due misure di Material**: con un tocco minimo di 56 il rientro della
 * freccia sarebbe 16, e questo numero andrebbe rifatto.
 */
private val VERSION_END = 12.dp

/**
 * Il rientro laterale di una pagina delle impostazioni.
 *
 * ⚠️ **È una costante e non un valore dentro la colonna**: lo rimettono anche le righe che si
 * toccano, dopo essersi allargate fino ai fianchi con [Modifier.bordo], e due numeri diversi
 * sposterebbero il testo di una riga toccabile.
 */
internal val PAGE_SIDE = 20.dp

/**
 * Quanta aria sopra e sotto il testo di una riga che si tocca.
 *
 * Sopra e sotto: con l'aria solo sopra l'alone finirebbe sull'ultima riga di testo, e un alone
 * che tocca il testo si legge come un ritaglio sbagliato.
 */
private val ROW_HIGH = 12.dp

/**
 * L'aria di una riga stretta (nota dell'utente su `backup-caselle`).
 *
 * ⚠️ **Non di più**: la riga resta sopra i 44 punti, cioè un bersaglio che il dito prende.
 */
private val ROW_TIGHT = 10.dp

/**
 * Allarga chi lo porta fino ai fianchi dello schermo, oltre il rientro della pagina.
 *
 * Riscontro dell'utente: l'alone di una riga toccabile arriva ai bordi, così il bersaglio si
 * vede grande quanto è.
 * ⚠️ **Il rientro della colonna resta**: lo usano anche titoli, separatori e blocchi scritti a
 * mano, che non si toccano. Tolto alla colonna, ognuno dovrebbe rimetterselo.
 * In Compose un rientro negativo non esiste, quindi lo fa un nodo di layout: misura il figlio
 * più largo di due rientri, dichiara al genitore la misura di prima, e posa il figlio spostato
 * di un rientro a sinistra.
 * I bordi sono quelli utili: la colonna della pagina ha già `safeDrawingPadding`, quindi
 * l'alone si ferma dove comincia un intaglio o una barra di sistema.
 */
private fun Modifier.bordo() = layout { misurabile, vincoli ->
    val extra = PAGE_SIDE.roundToPx() * 2
    val posato = misurabile.measure(vincoli.offset(horizontal = extra))
    layout(posato.width - extra, posato.height) { posato.place(-PAGE_SIDE.roundToPx(), 0) }
}

/**
 * Il separatore fra due voci nel riepilogo di una sotto-pagina: la stessa virgola con cui l'app
 * unisce gli altri elenchi che si leggono (`FileOps`, i contatori sotto una copertina).
 */
private const val SUMMARY_JOIN = ", "

/**
 * La sotto-pagina 'Opzioni di visualizzazione': come si presentano le viste della home.
 *
 * Le voci sono quelle del dialogo del tocco lungo nella schermata iniziale, e questa è la loro
 * casa: là compaiono solo quelle della vista scelta, qui tutte, sotto il titolino della vista.
 * Una preferenza, una chiave, un valore di fabbrica: le due superfici scrivono la stessa cosa.
 * ⚠️ **Chi tocca una di queste voci tocca due posti**: la riga è scritta due volte con due
 * componenti, e un cambiamento di forma va fatto in entrambi.
 * L'ordine è quello delle viste nel dialogo.
 */
@Composable
private fun ViewOptionsPage(settings: Settings, onChange: (Settings) -> Unit) {
    Section(stringResource(R.string.view_grid)) {

        // Le colonne sono anche nella scorciatoia del tocco lungo sul FAB, che scrive questa
        // stessa impostazione.
        Choices(
            label = stringResource(R.string.settings_folder_columns),
            detail = stringResource(R.string.settings_folder_columns_desc),
            options = FOLDER_COLUMNS.map { Columns(it) },
            selected = Columns(settings.folderColumns),
            nameOf = { it.n.toString() },
            onSelect = { onChange(settings.copy(folderColumns = it.n)) }
        )

        // Accanto alle colonne: parlano della stessa griglia, una di quante colonne ha e l'altra
        // di che cosa si legge sotto le copertine.
        SwitchRow(
            label = stringResource(R.string.settings_folder_count),
            detail = stringResource(R.string.settings_folder_count_desc),
            checked = settings.folderCount,
            onChange = { onChange(settings.copy(folderCount = it)) }
        )

        // Subito dopo: quella dice che cosa si legge sotto una cartella, questa sotto
        // un'immagine, cioè la stessa domanda in due griglie.
        SwitchRow(
            label = stringResource(R.string.settings_grid_names),
            detail = stringResource(R.string.settings_grid_names_desc),
            checked = settings.gridNames,
            onChange = { onChange(settings.copy(gridNames = it)) }
        )
    }

    Section(stringResource(R.string.view_list)) {

        SwitchRow(
            label = stringResource(R.string.list_count),
            detail = null,
            checked = settings.listCount,
            onChange = { onChange(settings.copy(listCount = it)) }
        )

        /*
         * Qui pastiglie e nella scorciatoia uno slider, di proposito: nel dialogo lo slider dice
         * una scala, qui le pastiglie sono in riga come le altre scelte del pannello. La
         * preferenza e la scala sono le stesse.
         */
        Choices(
            label = stringResource(R.string.text_size),
            detail = null,
            options = TextSize.entries,
            selected = settings.listText,
            nameOf = { stringResource(it.label()) },
            onSelect = { onChange(settings.copy(listText = it)) }
        )

        // Descrive l'elenco, quindi è accanto alle sue altre opzioni (`d-settings-order`).
        SwitchRow(
            label = stringResource(R.string.settings_list_path),
            detail = stringResource(R.string.settings_list_path_desc),
            checked = settings.listPath,
            onChange = { onChange(settings.copy(listPath = it)) }
        )
    }

    // 'Cartelle di sistema' e non 'Cartelle', il nome corto della vista nel dialogo, che
    // ripeterebbe il titolo della sezione da cui si arriva qui.
    // AIV Play non ha questa vista (`Store.views`), quindi nemmeno i suoi interruttori.
    if (FolderView.TREE in Store.views) Section(stringResource(R.string.hub_view_tree)) {

        SwitchRow(
            label = stringResource(R.string.tree_show_hidden),
            detail = null,
            checked = settings.treeHidden,
            onChange = { onChange(settings.copy(treeHidden = it)) }
        )

        SwitchRow(
            label = stringResource(R.string.tree_pictures),
            detail = null,
            checked = settings.treePictures,
            onChange = { onChange(settings.copy(treePictures = it)) }
        )
    }
}

/**
 * La sotto-pagina 'Adattamento e zoom': le voci che decidono come si **misura** un'immagine.
 */
@Composable
private fun ZoomAndFit(settings: Settings, onChange: (Settings) -> Unit) {
    SwitchRow(
        label = stringResource(R.string.settings_fit_grow),
        detail = stringResource(R.string.settings_fit_grow_desc),
        checked = settings.fitGrow,
        onChange = { onChange(settings.copy(fitGrow = it)) }
    )

    // ⚠️ In `Searchable`, perché un `Text` e uno `Slider` non si filtrano da sé: quando la
    // pagina si appiattisce nella radice durante una ricerca, resterebbero sempre in scena.
    val zoomMaxLabel = stringResource(R.string.settings_zoom_max)
    Searchable(zoomMaxLabel) {
        /*
         * ⚠️ **Il cursore scrive quando il dito si alza**: scrivendo a ogni fotogramma, una
         * strisciata sull'intera corsa ordinerebbe quasi duecento riscritture di `DataStore`.
         * Il valore mostrato viene dallo stato locale, o il numero resterebbe fermo sotto il
         * dito; lo stato riparte quando la preferenza cambia da fuori (chiave
         * `settings.zoomMax`), così un ripristino dei valori di fabbrica sposta il cursore.
         */
        var quanto by remember(settings.zoomMax) { mutableFloatStateOf(settings.zoomMax) }
        Text(
            text = zoomMaxLabel + "   " + quanto.roundToInt() + "x",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp)
        )
        ValueSlider(
            value = quanto,
            onValueChange = { quanto = it.roundToInt().toFloat() },
            onValueChangeFinished = { onChange(settings.copy(zoomMax = quanto)) },
            valueRange = SettingsStore.ZOOM_MAX_MIN..SettingsStore.ZOOM_MAX_MAX,
            modifier = Modifier.fillMaxWidth()
        )
    }

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
        stacked = true,
        onSelect = { onChange(settings.copy(scaleMode = it)) }
    )

    /*
     * Lo zoom nel menu è qui per correzione dell'utente: parla di zoom, e chi cerca
     * un'impostazione la cerca per argomento, non per dove compare.
     * Spenta di fabbrica: 'Adatta alla vista' e '100%' fanno quello che fa già il doppio tocco,
     * e sarebbero in mezzo a comandi che agiscono sul file. La descrizione dice solo che cosa
     * fa (istruzione dell'utente); il perché è qui.
     */
    SwitchRow(
        label = stringResource(R.string.settings_zoom_menu),
        detail = stringResource(R.string.settings_zoom_menu_desc),
        checked = settings.zoomInMenu,
        onChange = { onChange(settings.copy(zoomInMenu = it)) }
    )
}

/**
 * I due elenchi indipendenti, nella stessa pagina della scelta del modo.
 *
 * ⚠️ **'Aggiungi cartella' è una fascia fissa in fondo, e il resto scorre sopra** (nota A
 * dell'utente), come in 'Cartelle preferite' ([FavouriteFolders]): dopo un elenco lungo il tasto
 * finirebbe fuori dallo schermo.
 */
@Composable
private fun ColumnScope.HiddenFolders(settings: Settings, onChange: (Settings) -> Unit) {
    val included = settings.folderMode == FolderMode.INCLUDED
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HiddenFoldersList(settings, onChange, included)
    }
    if (included) IncludedAdd(settings, onChange)
}

/** La spiegazione, le pastiglie del modo e l'elenco di [HiddenFolders]: la parte che scorre. */
@Composable
private fun HiddenFoldersList(settings: Settings, onChange: (Settings) -> Unit, included: Boolean) {
    FolderSelectionDetail()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.oneOf()) {
        FolderMode.entries.forEach { mode ->
            FilterChip(
                selected = settings.folderMode == mode,
                onClick = { onChange(settings.copy(folderMode = mode)) },
                label = {
                    // Pastiglia normale, con lo stesso carattere delle altre (`3.26-03`): il
                    // grassetto è nel paragrafo descrittivo.
                    Text(
                        stringResource(
                            if (mode == FolderMode.INCLUDED) R.string.folder_mode_included
                            else R.string.folder_mode_excluded
                        )
                    )
                }
            )
        }
    }
    val entries = if (included) settings.includedFolders else settings.folderSelection.excluded
    Text(stringResource(if (included) R.string.folder_authorized else R.string.settings_hidden),
        style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
    if (entries.isEmpty()) Detail(stringResource(if (included) R.string.folder_included_none else R.string.settings_hidden_none))
    var removing by remember { mutableStateOf<String?>(null) }
    entries.sorted().forEach { entry ->
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                // La × a destra e il nome più largo, spezzato sul camelCase e con l'ellissi dopo
                // due righe (note B dell'utente).
                Text(
                    camelBreak(hiddenName(entry)),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(hiddenShown(entry), style = MaterialTheme.typography.bodySmall)
                if (!included && entry in settings.hiddenExactFolders && entry !in settings.hiddenFolders)
                    Text(stringResource(R.string.folder_only_this), style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = { removing = entry }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(
                        if (included) R.string.folder_remove else R.string.settings_hidden_show
                    )
                )
            }
        }
    }
    removing?.let { entry ->
        AlertDialog(
            onDismissRequest = { removing = null },
            modifier = Modifier.lowered { removing = null },
            properties = loweredWindow { removing = null },
            title = { Text(stringResource(R.string.folder_list_remove_title)) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(
                        if (included) settings.copy(includedFolders = settings.includedFolders - entry)
                        else settings.withFolders(settings.folderSelection.remove(listOf(entry)))
                    )
                    removing = null
                }) { Text(stringResource(R.string.folder_list_remove_do)) }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * 'Aggiungi cartella' nel modo delle cartelle incluse (`3.13-03`): autorizza una cartella senza
 * passare da 'Cartelle di sistema', con lo stesso dialogo delle destinazioni dell'app. È la
 * fascia fissa in fondo alla pagina ([HiddenFolders]).
 */
@Composable
private fun IncludedAdd(settings: Settings, onChange: (Settings) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    FilledTonalButton(
        onClick = { picking = true },
        modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
    ) {
        Text(stringResource(R.string.folder_add))
    }
    if (picking) {
        // ⚠️ Subito l'albero (vista TREE e un modo escluse finto): nel modo incluse il dialogo
        // partirebbe dalle scorciatoie già autorizzate, cioè proprio l'elenco da allungare.
        CompositionLocalProvider(
            LocalDestLook provides DestLook(
                view = FolderView.TREE,
                folderMode = FolderMode.EXCLUDED
            )
        ) {
            DestinationDialog(
                action = R.string.folder_authorize,
                // Autorizzare una cartella non sposta file, quindi qui non c'è nessuna origine.
                from = emptySet(),
                onDismiss = { picking = false },
                onPick = { dir ->
                    val path = portablePath(dir.absolutePath)
                    // Una cartella già in elenco non si aggiunge di nuovo (`3.27-02`).
                    if (path !in settings.includedFolders) {
                        onChange(settings.copy(includedFolders = settings.includedFolders + path))
                    }
                    picking = false
                },
                alreadyListed = settings.includedFolders
            )
        }
    }
}

/**
 * La spiegazione della pagina delle cartelle, coi nomi dei modi in grassetto (`3.26-03`,
 * `3.13-01`): non fra apici e non nelle pastiglie.
 *
 * In corpo piccolo (nota A dell'utente): è il paragrafo più lungo delle impostazioni, e col
 * corpo medio prendeva mezza pagina sopra le pastiglie.
 */
@Composable
private fun FolderSelectionDetail() {
    val included = stringResource(R.string.folder_mode_included)
    val excluded = stringResource(R.string.folder_mode_excluded)
    val raw = stringResource(R.string.folder_selection_desc)
    val annotated = buildAnnotatedString {
        append(raw)
        for (term in listOf(included, excluded)) {
            var from = 0
            while (true) {
                val at = raw.indexOf(term, from)
                if (at < 0) break
                addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), at, at + term.length)
                from = at + term.length
            }
        }
    }
    Text(
        annotated,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

/**
 * Quali dati mostrare quando si apre 'Info' su un'immagine, e in che ordine.
 *
 * Due comandi per riga perché sono due domande (richiesta dell'utente): la casella dice se il
 * campo si vede, la maniglia di [Reorderable] dove si trova.
 *
 * ⚠️ **Il nome del file non ha nessuno dei due comandi**: è sempre visibile e sempre in testa,
 * come ha chiesto l'utente, e una casella spenta offrirebbe una scelta che non c'è. Gli altri
 * campi obbligatori (pixel e peso) hanno il lucchetto al posto della casella, e la maniglia sì:
 * 'sempre visibile' non vuol dire 'in posizione fissa'.
 */
@Composable
private fun FactFields(
    settings: Settings,
    onChange: (Settings) -> Unit,
    /** Il guscio che scorre, per il trascinamento che arriva al bordo. Vedi [Reorderable]. */
    scroll: ScrollState
) {
    val order = settings.factOrder
    /*
     * La riga in testa, il nome del file, non si muove (`fixed = 1`): `factOrderOf` lo rimette
     * comunque in testa, e qui la sua riga non ha la maniglia, quindi non si prova nemmeno a
     * spostarlo.
     */
    Reorderable(
        items = order,
        fixed = 1,
        scroll = scroll,
        onMove = { da, a -> onChange(settings.copy(factOrder = order.moved(da, a))) }
    ) { field, _ ->
        val acceso = field !in settings.factOff
        val flip = { on: Boolean ->
            val off = if (on) settings.factOff - field else settings.factOff + field
            onChange(settings.copy(factOff = off))
        }
        Row(
            // ⚠️ Il rientro a destra è il posto della maniglia, che [Reorderable] disegna sopra
            // la riga: senza, un'etichetta lunga le finirebbe sotto.
            /*
             * ⚠️ **La riga intera è il comando**, come in `SwitchRow` (`Rules.md` § '⚙️ Dove va
             * un'impostazione, e chi la deve trovare'). Col trascinamento non c'è conflitto: la
             * maniglia è un fratello disegnato dopo la riga nello stesso `Box`, quindi il tocco
             * su di lei vince. La casella non ha un gesto suo (`onCheckedChange = null`), o un
             * lettore di schermo annuncerebbe due voci per una scelta.
             */
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = HANDLE_ROOM)
                .then(
                    if (field.always) Modifier
                    else Modifier.toggleable(
                        value = acceso,
                        role = Role.Checkbox,
                        onValueChange = flip
                    )
                ),
            verticalAlignment = Alignment.CenterVertically,
            /*
             * L'aria fra il segno e il nome è una sola (voce `riordino-scorre`): i nomi sono
             * allineati, dopo la casella come dopo il lucchetto.
             */
            horizontalArrangement = Arrangement.spacedBy(FACT_GAP)
        ) {
            /*
             * ⚠️ **Il lucchetto è nella scatola della casella** (stessa voce): una `Checkbox`
             * non cliccabile misura 20dp più 2dp per lato, quindi il suo centro cade a 12dp dal
             * bordo, e il lucchetto deve cadere lì. Il glifo resta più piccolo: grande come
             * una casella, peserebbe più delle scelte vere.
             */
            Box(
                modifier = Modifier.size(FACT_MARK),
                contentAlignment = Alignment.Center
            ) {
                if (field.always) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.settings_facts_always),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Checkbox(checked = acceso, onCheckedChange = null)
                }
            }
            Text(
                text = stringResource(field.label),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Lo spazio che una riga lascia alla maniglia del trascinamento: si ricava da [HANDLE], la
 * misura vera della maniglia, più un filo d'aria che la stacca dal testo.
 */
private val HANDLE_ROOM = HANDLE + 4.dp

/**
 * La scatola del segno di una riga dei campi: la casella spunta o il lucchetto.
 *
 * ⚠️ **24dp è la misura di una `Checkbox` che non si tocca** (20 di disegno più 2 di riempimento
 * per lato): un'altra misura disallineerebbe le righe col lucchetto da quelle con la casella.
 */
private val FACT_MARK = 24.dp

/** L'aria fra il segno di una riga dei campi e il suo nome. */
private val FACT_GAP = 14.dp

/**
 * Gli elenchi di tasti, uno per riquadro.
 *
 * ⚠️ **Un elenco per riquadro e non uno solo**: i riquadri delle azioni sui file hanno insiemi
 * diversi in ordini diversi, dettati dall'utente, e nessuno è il sottoinsieme ordinato di un
 * altro; le file dell'editor sono separate perché il filetto in mezzo dice che sono due mestieri.
 * Il perché per esteso è accanto alle chiavi, in `Settings.Store`.
 * Ogni elenco è un gruppo con un titolo e non una pagina: una pagina per riquadro costerebbe un
 * tocco per ognuno.
 * Nessun tasto è in posizione obbligata, al contrario del nome del file nei campi delle info.
 */
@Composable
private fun ButtonOrders(settings: Settings, onChange: (Settings) -> Unit) {
    /*
     * La riga delle istruzioni è dell'utente (riscontro del giro della `1.56`): il gesto è un
     * tocco lungo, l'unico che non si scopre toccando, e senza questa riga il riquadro sembra
     * una figura e non uno strumento.
     */
    Detail(stringResource(R.string.settings_buttons_how))
    /*
     * Questa dice una cosa che dal riquadro non si vede (richiesta dell'utente): nel cestino il
     * riquadro ha 'Ripristina' al posto di 'Rinomina', con lo stesso ordine.
     * AIV Play ha il cestino e 'Rinomina' solo da Android 11 (`Store.renames`): prima la frase
     * non avrebbe niente da spiegare.
     */
    if (Store.renames) Detail(stringResource(R.string.settings_buttons_bin))
    PadOrder(
        title = stringResource(R.string.settings_buttons_menu),
        order = settings.menuOrder,
        difetto = MENU_KEYS,
        columns = PAD_COLUMNS,
        onOrder = { onChange(settings.copy(menuOrder = it)) }
    )
    PadOrder(
        title = stringResource(R.string.pick_actions),
        order = settings.pickOrder,
        difetto = PICK_KEYS,
        columns = SHEET_COLUMNS,
        onOrder = { onChange(settings.copy(pickOrder = it)) }
    )
    PadOrder(
        title = stringResource(R.string.settings_buttons_turn),
        order = settings.turnOrder,
        difetto = TURN_KEYS,
        // ⚠️ La misura della lista e non [SHEET_KEYS]: con 'Rifletti' questa fila ha un tasto
        // in più della seconda, e la replica deve andare a capo dove ci va il riquadro vero.
        columns = TURN_KEYS.size,
        onOrder = { onChange(settings.copy(turnOrder = it)) }
    )
    PadOrder(
        title = stringResource(R.string.settings_buttons_steps),
        order = settings.stepOrder,
        difetto = STEP_KEYS,
        columns = SHEET_KEYS,
        onOrder = { onChange(settings.copy(stepOrder = it)) }
    )
    /*
     * ⚠️ **Il riquadro dei moduli c'è solo dove c'è l'editor completo** (sotto Android 13 no:
     * vedi `advancedEditorAvailable`): mostrarlo farebbe riordinare una fila che non si apre. La
     * preferenza si salva lo stesso, perché un archivio non cambia forma con la versione di
     * Android.
     * La replica è di sole icone, come la fila vera: i nomi non entrano in nessuna larghezza, e
     * restano quelli che un lettore di schermo annuncia.
     */
    if (advancedEditorAvailable()) {
        PadOrder(
            title = stringResource(R.string.settings_buttons_mods),
            order = settings.modOrder,
            difetto = MOD_KEYS,
            columns = MOD_KEYS.size,
            labels = false,
            onOrder = { onChange(settings.copy(modOrder = stylesLast(it))) }
        )
    }
}

/*
 * ⚠️ Le colonne delle repliche sono quelle dei riquadri veri ([PAD_COLUMNS], [SHEET_KEYS]), mai
 * una copia: un riquadro riordinato su tre colonne e mostrato su due direbbe il falso sul
 * risultato.
 */

/**
 * Un riquadro da riordinare, col suo titolo.
 *
 * Perché è un riquadro e non un elenco di righe, e a che cosa la replica deve somigliare, lo dice
 * [PadArrange].
 */
@Composable
private fun PadOrder(
    title: String,
    order: List<PadKey>,
    /**
     * L'ordine di fabbrica di questo riquadro, quello a cui 'Ripristina' riporta.
     *
     * Arriva da chi chiama: le liste di fabbrica vivono in `Settings.kt` accanto ai campi che le
     * usano come valore iniziale, e una copia qui potrebbe divergere.
     */
    difetto: List<PadKey>,
    columns: Int,
    onOrder: (List<PadKey>) -> Unit,
    /** Se le celle mostrano la parola sotto il glifo: vedi [PadArrange]. */
    labels: Boolean = true
) {
    /*
     * Un 'Ripristina' per ogni riquadro, sulla riga del suo titolo, discreto e a destra
     * (richiesta dell'utente): chi ha guastato l'ordine di un riquadro vuole rimettere a posto
     * quello, e un comando unico cancellerebbe anche gli altri.
     * Sparisce mentre si cerca, col titolo: senza il titolo accanto non si saprebbe che cosa
     * ripristina.
     * ⚠️ **Chiede conferma, per decisione dell'utente** (riscontro del giro della `1.59`): una
     * disposizione rifatta è lavoro, e qui non c'è modo di disfare.
     */
    var chiedendo by remember { mutableStateOf(false) }
    if (LocalQuery.current.isBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Group(title)
            Text(
                text = stringResource(R.string.settings_buttons_reset),
                style = MaterialTheme.typography.labelMedium,
                color = accentInk(),
                modifier = Modifier
                    .clickable(enabled = order != difetto, role = Role.Button) {
                        chiedendo = true
                    }
                    .tapRoom()
                    .padding(start = 12.dp, top = 24.dp, bottom = 4.dp)
                    .alpha(if (order == difetto) OFF_INK else 1f)
            )
        }
    }
    PadArrange(
        order = order,
        columns = columns,
        onOrder = onOrder,
        modifier = Modifier.padding(bottom = ARRANGE_GAP),
        labels = labels
    )
    /*
     * ⚠️ **Fuori dal ramo della ricerca**, che nasconde la riga del titolo: là dentro, una
     * ricerca cominciata a dialogo aperto lo toglierebbe di scena a metà.
     * Il tasto di conferma riusa la parola del comando: è lo stesso comando un tocco dopo. Niente
     * paragrafo sotto la domanda, che dice già tutto.
     */
    if (chiedendo) {
        AlertDialog(
            onDismissRequest = { chiedendo = false },
            modifier = Modifier.lowered { chiedendo = false },
            properties = loweredWindow { chiedendo = false },
            title = { Text(stringResource(R.string.settings_buttons_reset_ask)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        chiedendo = false
                        onOrder(difetto)
                    }
                ) { Text(stringResource(R.string.settings_buttons_reset)) }
            },
            dismissButton = {
                TextButton(onClick = { chiedendo = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}


/** L'aria fra un riquadro e il titolo del successivo. */
private val ARRANGE_GAP = 12.dp

/**
 * Un numero di colonne, vestito da [Choice] per entrare nella fila di pastiglie.
 *
 * Un adattatore: [Choices] parla di scelte con un nome, e le colonne sono un numero. È una
 * `data class` per l'uguaglianza, con cui la fila riconosce la pastiglia accesa.
 */
private data class Columns(val n: Int) : Choice {
    override val token: String get() = n.toString()
}

/**
 * Una fila di pastiglie per una scelta. `FlowRow` e non `Row`: alcune hanno per nome una frase,
 * e su uno schermo stretto una fila fissa le spingerebbe oltre il bordo e non andrebbe a capo.
 */
@Composable
internal fun <T : Choice> Choices(
    label: String,
    detail: String?,
    options: List<T>,
    /** `null` quando non c'è ancora una scelta: l'ordine delle cartelle non ha un valore di fabbrica. */
    selected: T?,
    nameOf: @Composable (T) -> String,
    /**
     * Una pastiglia per riga, larga quanto la pagina (nota B dell'utente su 'Rendering dei
     * pixel'): per le pastiglie che hanno per nome una frase, che una fila misurerebbe una per una.
     */
    stacked: Boolean = false,
    onSelect: (T) -> Unit
) {
    // Anche i nomi delle pastiglie entrano nella ricerca: 'Scacchiera' o 'In alto' sono le
    // parole con cui si pensa all'impostazione, più del titolo ('Sfondo', 'Posizione').
    // Calcolati una volta e riusati sotto: `nameOf` è una `stringResource`.
    val names = options.map { nameOf(it) }
    if (!shown(label, detail, *names.toTypedArray())) return
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 12.dp)
    )
    detail?.let { Detail(it) }
    if (stacked) {
        Column(modifier = Modifier.oneOf()) {
            options.forEachIndexed { at, option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = {
                        Text(names[at], textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    },
                    modifier = Modifier.fillMaxWidth().picked(option == selected)
                )
            }
        }
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.oneOf()) {
        options.forEachIndexed { at, option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(names[at]) },
                modifier = Modifier.picked(option == selected)
            )
        }
    }
}

/**
 * 'Elemento interattivo principale': che cosa comanda l'app sul telefono in verticale, e per la
 * pillola come si presenta.
 *
 * In cima a 'Pulsanti e indicatori' (nota E del giro della `4.04`): titolo, pastiglie e
 * paragrafo sono dell'utente.
 * La seconda fila c'è per la pillola e per il Menu basso: il FAB non ha un modo a scomparsa. Il
 * valore è uno, e per il menu la seconda pastiglia si chiama `Fisso` e non `Estesa`.
 * La ricerca guarda anche i nomi delle pastiglie di tutte e due le file: chi cerca 'Estesa'
 * trova la voce anche col FAB scelto.
 * AIV Play ha le sole pastiglie FAB e pillola (nota dell'utente), con un paragrafo suo che non
 * nomina i menu (`settings_main_control_desc_play`). Gli elenchi vengono da `Store.mainControls`.
 */
@Composable
private fun MainControlChoices(settings: Settings, onChange: (Settings) -> Unit) {
    val label = stringResource(R.string.settings_main_control)
    val detail = stringResource(
        if (Store.fullSettings) R.string.settings_main_control_desc else R.string.settings_main_control_desc_play
    )
    val controls = Store.mainControls
    val controlNames = controls.map {
        stringResource(
            when (it) {
                MainControl.FAB -> R.string.main_control_fab
                MainControl.PILL -> R.string.main_control_pill
                MainControl.BOTTOM -> R.string.main_control_bar
                MainControl.CORNER -> R.string.main_control_corner
            }
        )
    }
    val modes = PhonePill.entries.filter { it != PhonePill.OFF }
    // Il menu è maschile, la pillola femminile: la seconda pastiglia cambia nome, il valore no.
    val bar = settings.mainControl == MainControl.BOTTOM
    val modeNames = modes.map {
        stringResource(
            when {
                it == PhonePill.SLIDE -> R.string.pill_slide
                bar -> R.string.bar_fixed
                else -> R.string.pill_extended
            }
        )
    }
    // 'Fisso' è la pastiglia del solo Menu basso: dove il menu non c'è, la ricerca non lo trova.
    val searched = controlNames + modeNames +
        listOfNotNull(stringResource(R.string.bar_fixed).takeIf { MainControl.BOTTOM in controls }) +
        // E le due forme a riposo del Menu 'Start', dove quel menu c'è.
        if (MainControl.CORNER in controls) {
            listOf(stringResource(R.string.start_round), stringResource(R.string.start_pill))
        } else {
            emptyList()
        }
    if (!shown(label, detail, *searched.toTypedArray())) return
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 12.dp)
    )
    Detail(detail)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.oneOf()) {
            controls.forEachIndexed { at, control ->
                FilterChip(
                    selected = control == settings.mainControl,
                    onClick = { onChange(settings.copy(mainControl = control)) },
                    label = { Text(controlNames[at]) },
                    modifier = Modifier.picked(control == settings.mainControl)
                )
            }
        }
        /*
         * Il Menu 'Start' è sempre a scomparsa, e la sua seconda fila dice come riposa: tondo o
         * pillola, scelti qui per scelta dell'utente. I nomi sono quelli della casella del menu
         * che prima li alternava, e che oggi apre il Feed.
         */
        if (settings.mainControl == MainControl.CORNER) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.oneOf()) {
                listOf(true, false).forEach { round ->
                    FilterChip(
                        selected = round == settings.cornerRound,
                        onClick = { onChange(settings.copy(cornerRound = round)) },
                        label = { Text(stringResource(if (round) R.string.start_round else R.string.start_pill)) },
                        // I due disegni dell'utente della vecchia casella, uno per forma.
                        leadingIcon = {
                            Icon(
                                imageVector = if (round) Glyphs.RestRound else Glyphs.RestPill,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        },
                        modifier = Modifier.picked(round == settings.cornerRound)
                    )
                }
            }
        }
        if (settings.mainControl == MainControl.PILL || settings.mainControl == MainControl.BOTTOM) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.oneOf()) {
                modes.forEachIndexed { at, mode ->
                    FilterChip(
                        selected = mode == settings.phonePill,
                        onClick = { onChange(settings.copy(phonePill = mode)) },
                        label = { Text(modeNames[at]) },
                        modifier = Modifier.picked(mode == settings.phonePill)
                    )
                }
            }
        }
    }
}

/**
 * 'Aspetto dei pulsanti principali': di che cosa sono fatti il FAB e le pillole.
 *
 * Una voce sola, in fondo a 'Tema e dettagli grafici', che vale anche per il FAB (nota E del
 * giro della `4.04`). C'è su ogni apparecchio, tablet compreso.
 * Il nome 'Traslucido' è dell'utente; la chiave della stringa resta `pill_glass`.
 * ⚠️ **'Traslucido' non compare sotto Android 12** (risposta dell'utente): il vetro vuole un
 * `RenderEffect`, e una pastiglia senza effetto è peggio di una pastiglia che non c'è.
 */
@Composable
private fun ButtonLookChoices(settings: Settings, onChange: (Settings) -> Unit) {
    Choices(
        label = stringResource(R.string.settings_button_look),
        detail = stringResource(R.string.settings_button_look_desc),
        options = PillFill.entries.filter { it != PillFill.GLASS || glassAvailable() },
        selected = settings.pillFill,
        nameOf = {
            stringResource(
                when (it) {
                    PillFill.SOLID -> R.string.pill_solid
                    PillFill.TRANSLUCENT -> R.string.pill_translucent
                    PillFill.GLASS -> R.string.pill_glass
                }
            )
        },
        onSelect = { onChange(settings.copy(pillFill = it)) }
    )
    /*
     * I cursori del traslucido (nota N1 dell'utente): compaiono solo col traslucido scelto, e si
     * nascondono tutti spegnendo [GLASS_TUNING]. Il significato di ognuno è su [GlassTune].
     * Scrivono quando il dito si alza, come il cursore dello zoom.
     */
    if (GLASS_TUNING && settings.pillFill == PillFill.GLASS && glassAvailable()) {
        /*
         * ⚠️ **L'anteprima segue il dito** (correzione dell'utente: alla cieca non si regola): i
         * cursori muovono [vivo], che l'anteprima legge a ogni fotogramma, e la preferenza si
         * scrive quando il dito si alza. [vivo] riparte quando il valore cambia da fuori, così un
         * ripristino rimette a posto cursori e anteprima.
         */
        var vivo by remember(settings.glass) { mutableStateOf(settings.glass) }
        val scrivi = { onChange(settings.copy(glass = vivo)) }
        GlassPreview(vivo)
        GlassSlider(R.string.glass_radius, vivo.radius, GlassTune.RADIUS_RANGE, "dp", { vivo = vivo.copy(radius = it) }, scrivi)
        GlassSlider(R.string.glass_intensity, vivo.intensity, GlassTune.INTENSITY_RANGE, "%", { vivo = vivo.copy(intensity = it) }, scrivi)
        GlassSlider(R.string.glass_tint, vivo.tint, GlassTune.TINT_RANGE, "%", { vivo = vivo.copy(tint = it) }, scrivi)
        GlassSlider(R.string.glass_light, vivo.shift, GlassTune.SHIFT_RANGE, "%", { vivo = vivo.copy(shift = it) }, scrivi)
        /*
         * I due colori (risposta B3 dell'utente): uno per tema, e il predefinito è l'accento del
         * vetro in quel tema ([pillAccent]). L'anteprima mostra quello del tema in vigore.
         */
        GlassColourRow(R.string.glass_colour_light, light = true, tune = settings.glass) {
            onChange(settings.copy(glass = settings.glass.withColour(true, it)))
        }
        GlassColourRow(R.string.glass_colour_dark, light = false, tune = settings.glass) {
            onChange(settings.copy(glass = settings.glass.withColour(false, it)))
        }
        /*
         * 'Ripristina' riporta tutto ai valori di fabbrica, colori compresi: chi ha mosso i cursori
         * non vede i valori di fabbrica nuovi, che valgono solo dove l'archivio non ha niente.
         */
        TextButton(
            onClick = { onChange(settings.copy(glass = GlassTune())) },
            enabled = settings.glass != GlassTune(),
            modifier = Modifier.padding(top = 4.dp)
        ) { Text(stringResource(R.string.glass_reset)) }
    }
}

/**
 * Uno dei due colori del traslucido: il nome, e un tondo col colore in vigore in quel tema.
 *
 * ⚠️ **Il tocco apre il selettore** ([GlassColourDialog]), che scrive quando si tocca 'Applica';
 * 'Predefinito' rimette l'accento, cioè toglie la scelta dall'archivio.
 */
@Composable
private fun GlassColourRow(label: Int, light: Boolean, tune: GlassTune, onPick: (Int?) -> Unit) {
    val name = stringResource(label)
    val fallback = aivAccent(!light)
    val current = tune.colourFor(light)?.let { Color(it) } ?: fallback
    var picking by remember { mutableStateOf(false) }
    Searchable(name) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clickable(role = Role.Button, onClickLabel = name) { picking = true }
                .padding(vertical = 8.dp)
        ) {
            Text(text = name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(SWATCH)
                    .background(current, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
    }
    if (picking) {
        GlassColourDialog(
            title = name,
            initial = current,
            onPick = { picking = false; onPick(it) },
            onDismiss = { picking = false }
        )
    }
}

/**
 * Il selettore di un colore del traslucido: tonalità, saturazione e luminanza, col colore che ne
 * risulta in cima.
 *
 * HSL e non una ruota: cursori con le parole del modulo HSL dell'editor completo, già tradotte,
 * e un cursore si muove di un'unità per volta.
 * Si chiude toccando fuori, come 'Annulla': non raccoglie un testo scritto, quindi non è una
 * modale vera (`Rules.md`, § '👆 Che cosa fa il tocco FUORI da una finestra').
 *
 * @param onPick il colore scelto in ARGB, o `null` per tornare all'accento ('Predefinito').
 */
@Composable
private fun GlassColourDialog(title: String, initial: Color, onPick: (Int?) -> Unit, onDismiss: () -> Unit) {
    val hsl = remember(initial) { FloatArray(3).also { ColorUtils.colorToHSL(initial.toArgb(), it) } }
    var hue by remember(initial) { mutableFloatStateOf(hsl[0]) }
    var sat by remember(initial) { mutableFloatStateOf(hsl[1]) }
    var lum by remember(initial) { mutableFloatStateOf(hsl[2]) }
    val colour = Color(ColorUtils.HSLToColor(floatArrayOf(hue, sat, lum)))
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(onDismiss),
        properties = loweredWindow(onDismiss),
        title = { Text(title) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SWATCH_TALL)
                        .background(colour, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                )
                HslSlider(R.string.look_hue, hue, 0f..360f, "°") { hue = it }
                HslSlider(R.string.look_saturation, sat * 100f, 0f..100f, "%") { sat = it / 100f }
                HslSlider(R.string.look_lum, lum * 100f, 0f..100f, "%") { lum = it / 100f }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(colour.toArgb()) }) { Text(stringResource(R.string.editor_apply)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onPick(null) }) { Text(stringResource(R.string.glass_default)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    )
}

/** Un cursore del selettore: il nome col valore intero accanto, e il cursore sotto. */
@Composable
private fun HslSlider(label: Int, value: Float, range: ClosedFloatingPointRange<Float>, unit: String, onMove: (Float) -> Unit) {
    Text(
        text = "${stringResource(label)}   ${value.roundToInt()}$unit",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 12.dp)
    )
    ValueSlider(value = value, onValueChange = onMove, valueRange = range, modifier = Modifier.fillMaxWidth())
}

/** Il tondo che mostra un colore del traslucido nella sua riga. */
private val SWATCH = 28.dp

/** Quanto è alto il colore risultante in cima al selettore. */
private val SWATCH_TALL = 48.dp

/**
 * Un cursore del traslucido: il nome col valore accanto, e il cursore sotto.
 *
 * ⚠️ **Muove il valore dal vivo a ogni passo** ([onMove]), perché l'anteprima lo segua, e scrive
 * la preferenza solo quando il dito si alza ([onDone]).
 */
@Composable
private fun GlassSlider(
    label: Int,
    value: Int,
    range: IntRange,
    unit: String,
    onMove: (Int) -> Unit,
    onDone: () -> Unit
) {
    val name = stringResource(label)
    Searchable(name) {
        Text(
            text = "$name   $value$unit",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp)
        )
        ValueSlider(
            value = value.toFloat(),
            onValueChange = { onMove(it.roundToInt()) },
            onValueChangeFinished = onDone,
            valueRange = range.first.toFloat()..range.last.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * L'anteprima del traslucido: una pillola di vetro sopra un fondo a righe colorate, coi valori
 * dei cursori così come sono adesso.
 *
 * Il fondo è disegnato e non è un'immagine: con righe sottili e colori saturi raggio e intensità
 * si vedono meglio, e non c'è un file da includere.
 * ⚠️ **È il vetro vero** ([buttonFill] con lo stesso [Backdrop] delle schermate), col tono scelto
 * passato da [LocalPillLook]: quello che si vede qui è quello che si vedrà sulle pillole.
 */
@Composable
private fun GlassPreview(tune: GlassTune) {
    val layer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    val backdrop = remember(layer) { Backdrop(layer) }
    val look = LocalPillLook.current.copy(fill = PillFill.GLASS, glass = tune)
    Box(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .height(PREVIEW_TALL)
            .clip(RoundedCornerShape(PREVIEW_ROUND))
            // Muta per il lettore di schermo: è un disegno, e le icone finte non sono comandi.
            .clearAndSetSemantics { }
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize().backdropSource(backdrop)) {
            val step = PREVIEW_STRIPE.toPx()
            var x = -size.height
            var i = 0
            while (x < size.width + size.height) {
                drawLine(
                    color = PREVIEW_COLOURS[i % PREVIEW_COLOURS.size],
                    start = androidx.compose.ui.geometry.Offset(x, size.height),
                    end = androidx.compose.ui.geometry.Offset(x + size.height, 0f),
                    strokeWidth = step
                )
                x += step
                i++
            }
        }
        CompositionLocalProvider(LocalPillLook provides look, LocalContentColor provides pillInk()) {
            Row(modifier = Modifier.align(Alignment.Center).pillFill(backdrop)) {
                listOf(Icons.Default.Search, Icons.Default.Close, Icons.Default.Settings).forEach {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(PILL_KEY)) {
                        Icon(it, contentDescription = null, modifier = Modifier.size(PILL_GLYPH))
                    }
                }
            }
        }
        /*
         * Il bordo d'accento (punto B dell'utente, 5dp interni e sovrapposti): copre i bordi
         * seghettati delle righe contro lo stondamento, ed è disegnato dopo, cioè sopra.
         */
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(PREVIEW_EDGE, aivAccent(LocalAivLight.current), RoundedCornerShape(PREVIEW_ROUND))
        )
    }
}

/** Lo spessore del bordo d'accento dentro l'anteprima: i 5dp dell'utente. */
private val PREVIEW_EDGE = 5.dp

/** Il raggio degli angoli dell'anteprima, comune al ritaglio e al bordo. */
private val PREVIEW_ROUND = 16.dp

/** L'altezza dell'anteprima del traslucido: abbastanza per la pillola e qualche riga intorno. */
private val PREVIEW_TALL = 96.dp

/** La larghezza di ogni riga dietro la pillola dell'anteprima. */
private val PREVIEW_STRIPE = 10.dp

/** I colori delle righe: saturi e lontani fra loro, dove sfocatura e intensità si vedono meglio. */
private val PREVIEW_COLOURS = listOf(
    Color(0xFFE53935), Color(0xFFFDD835), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFFFFFFF)
)

/**
 * Una voce con l'interruttore.
 *
 * ⚠️ **La riga intera è il comando**: il solo interruttore sarebbe un bersaglio da 32dp. Il tocco
 * è un `toggleable` con `role = Role.Switch`, e l'interruttore non ha un gesto suo, o un lettore
 * di schermo annuncerebbe due voci per una scelta. Lo mette il componente, così una voce nuova
 * ce l'ha da sé.
 * Il `toggleable` va prima del `padding`, come su [PageRow]: il bersaglio prende anche il
 * margine e arriva ai 48dp senza scriverli.
 */
@Composable
internal fun SwitchRow(
    label: String,
    detail: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    extra: List<String> = emptyList()
) {
    if (!shown(label, detail, *extra.toTypedArray())) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bordo()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = PAGE_SIDE, vertical = ROW_HIGH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
            detail?.let { Detail(it) }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * Una voce con la casella: la gemella di [SwitchRow] per le scelte che si sommano.
 *
 * Una casella e non un interruttore: un interruttore dice che una cosa è accesa da adesso in poi,
 * qui si sceglie che cosa entra in un'operazione che parte dopo, come distingue Material.
 * La riga intera è il bersaglio, con `Role.Checkbox`, per la ragione scritta su [SwitchRow].
 */
@Composable
internal fun CheckRow(
    label: String,
    detail: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    /**
     * Se la riga porta meno aria sopra e sotto: [ROW_TIGHT] invece di [ROW_HIGH].
     *
     * La chiede la pagina 'Esporta e importa', perché i suoi tasti si vedano senza scorrere
     * (vedi `BackupPage`).
     */
    tight: Boolean = false
) {
    if (!shown(label, detail)) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bordo()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
            .padding(horizontal = PAGE_SIDE, vertical = if (tight) ROW_TIGHT else ROW_HIGH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
            detail?.let { Detail(it) }
        }
        Checkbox(checked = checked, onCheckedChange = null)
    }
}


/**
 * La sotto-pagina **'Rinomina e download'**: le voci che decidono con che nome un file si salva e
 * dove finisce.
 *
 * ⚠️ **Il significato delle voci è dell'utente** (voce `tocco-lungo-due`), ed è scritto qui
 * perché è il solo posto in cui si vedono insieme:
 * - **'Consenti la rinomina al salvataggio'** riguarda la sola 'Scarica' e mostra la finestra
 *   del nome. *Non aggiunge nulla di per sé*: se è spenta, la si accende al volo con un tocco
 *   lungo su 'Scarica', e *in quel caso 'Percorso' è sempre attiva*.
 * - **'Scegli il percorso di download'** riguarda la sola 'Scarica' e *si applica se è attivo*
 *   il primo: aggiunge il tasto che sceglie la cartella.
 * - **'Consenti la modifica dell'estensione'** riguarda *sia 'Rinomina' che 'Scarica'*, e le due
 *   pastiglie dicono in quale delle due.
 *
 * La subordinazione della seconda voce si legge e non si impone: l'interruttore resta toccabile
 * anche con la rinomina spenta, e a non avere effetto è il comando dentro una finestra che allora
 * non si apre. Una riga disattivata sembrerebbe guasta senza dire perché; il perché lo dice la sua
 * spiegazione.
 */
@Composable
private fun RenameAndDownload(settings: Settings, onChange: (Settings) -> Unit) {
    SwitchRow(
        label = stringResource(R.string.settings_save_rename),
        detail = stringResource(R.string.settings_save_rename_desc),
        checked = settings.saveRename,
        onChange = { onChange(settings.copy(saveRename = it)) }
    )
    /*
     * ⚠️ **Spegnerla dimentica la cartella scelta**: senza, i file continuerebbero ad andare
     * nella cartella scelta mesi prima mentre il comando che la mostra e la cambia non c'è più
     * (il vicolo cieco della voce `save-percorso`, dall'altra parte).
     * Accenderla non ripesca la cartella di prima: una destinazione non ricompare senza che
     * nessuno l'abbia chiesta.
     */
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    SwitchRow(
        label = stringResource(R.string.settings_download_path),
        detail = stringResource(R.string.settings_download_path_desc),
        checked = settings.downloadPath,
        onChange = {
            if (!it) scope.launch { DownloadFolder.forget(context) }
            onChange(settings.copy(downloadPath = it))
        }
    )
    /*
     * ⚠️ **Le due pastiglie sono indipendenti, non una scelta fra due**: spente di fabbrica e
     * attive per ciascuna funzione, come le ha chieste l'utente. Per questo [Toggles] e non
     * `Choices`, che dichiara una scelta sola anche a un lettore di schermo.
     * I nomi sono le stringhe dei comandi veri, `menu_save` e `pick_rename`, senza copie da
     * tenere d'accordo in tutte le lingue.
     * AIV Play non ha la voce: là l'estensione non si cambia (`Store.fit`).
     */
    if (Store.fullSettings) Toggles(
        label = stringResource(R.string.settings_ext_edit),
        detail = stringResource(R.string.settings_ext_edit_desc),
        names = listOf(
            stringResource(R.string.menu_save),
            stringResource(R.string.pick_rename)
        ),
        on = listOf(settings.extDownload, settings.extRename),
        onFlip = { at ->
            onChange(
                if (at == 0) settings.copy(extDownload = !settings.extDownload)
                else settings.copy(extRename = !settings.extRename)
            )
        }
    )
}

/**
 * Una pastiglia di [Toggles] col suo valore e col cambiamento che fa il tocco: serve dove AIV
 * Play mostra solo alcune pastiglie di una voce.
 */
private class Chip(val name: String, val on: Boolean, val flip: (Settings) -> Settings)

/**
 * Una voce con più pastiglie **indipendenti**: ognuna si accende e si spegne da sé.
 *
 * ⚠️ **Non è [Choices]**: là le pastiglie sono una scelta sola, e il componente lo dichiara a un
 * lettore di schermo; qui ogni combinazione è ammessa. Il ruolo di serie di `FilterChip` è quello
 * giusto: un tasto col proprio stato, senza un gruppo che dichiari l'esclusività.
 * Anche i nomi delle pastiglie entrano nella ricerca, come su [Choices].
 */
@Composable
private fun Toggles(
    label: String,
    detail: String?,
    names: List<String>,
    on: List<Boolean>,
    onFlip: (Int) -> Unit
) {
    if (!shown(label, detail, *names.toTypedArray())) return
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 12.dp)
    )
    detail?.let { Detail(it) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        names.forEachIndexed { at, name ->
            FilterChip(
                selected = on[at],
                onClick = { onFlip(at) },
                label = { Text(name) }
            )
        }
    }
}

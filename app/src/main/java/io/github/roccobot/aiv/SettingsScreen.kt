package io.github.roccobot.aiv

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.Normalizer
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
    onResetHints: () -> Unit,
    /** Apre il selettore dell'app di modifica: la finestra la fa il modello. Vedi `chooseEditor`. */
    onChooseEditor: () -> Unit,
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
    // ⚠️ Sta QUI e non dentro `RootPage` per la stessa ragione dello scorrimento: una
    // sotto-pagina e il ritorno non devono cancellare quello che si stava cercando.
    var query by remember { mutableStateOf("") }
    // ⚠️ Vince su quello dell'attività (`ViewerActivity`, che qui chiama `leaveSettings`)
    // perché è registrato DOPO: il dispatcher di Android serve l'ultimo arrivato fra quelli
    // accesi. È lo stesso annidamento della selezione nella griglia, che regge da versioni.
    BackHandler(enabled = page != Page.ROOT) { page = Page.ROOT }
    // ⚠️ Con una ricerca in corso Indietro la annulla invece di uscire, ed è quello che fa
    // ogni ricerca dentro un elenco: uscire dalle impostazioni lasciando l'elenco filtrato
    // costringerebbe a rientrare per rivederlo intero. I due non sono mai accesi insieme.
    BackHandler(enabled = page == Page.ROOT && query.isNotBlank()) { query = "" }

    when (page) {
        Page.ROOT -> Shell(
            title = stringResource(R.string.settings_title),
            onBack = onBack,
            modifier = modifier,
            scroll = rootScroll
        ) {
            SearchField(query = query, onQuery = { query = it })
            /*
             * ⚠️⚠️ **LA COLONNA IN PIÙ È IL MODO DI SAPERE SE LA RICERCA HA TROVATO
             * QUALCOSA, e non un annidamento di troppo**: le righe si filtrano una per una e
             * nessuno le conta, quindi l'unica cosa che sa quante ne sono rimaste è il
             * **layout**, che senza figli misura zero. Contarle durante la composizione
             * vorrebbe dire leggere un totale scritto da chi viene dopo, cioè mostrare
             * 'nessun risultato' per un fotogramma anche quando i risultati ci sono.
             * ⚠️ La spaziatura è la stessa del guscio, se no le voci si stringerebbero fra
             * loro appena entrano in questa colonna.
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
                        onOpen = { page = it }
                    )
                }
            }
            // ⚠️ Una pagina vuota sotto un campo di ricerca si legge come un'app rotta, non
            // come 'non c'è niente': la riga dice che la ricerca ha funzionato e non ha
            // trovato, che sono due cose diverse.
            if (empty && query.isNotBlank()) {
                Text(
                    text = stringResource(R.string.settings_search_none),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
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

        Page.ZOOM -> Shell(
            title = stringResource(R.string.settings_zoom_page),
            onBack = { page = Page.ROOT },
            modifier = modifier
        ) {
            ZoomAndFit(settings = settings, onChange = onChange)
        }
    }
}

/** Quale delle quattro pagine si sta guardando. */
private enum class Page { ROOT, FACTS, HIDDEN, ZOOM }

/**
 * Che cosa si sta cercando nelle impostazioni, e stringa vuota quando non si cerca.
 *
 * ⚠️⚠️ **UN `CompositionLocal` E NON UN PARAMETRO IN VENTOTTO RIGHE** (richiesta
 * dell'utente, 2026-09-01: *aggiungi un 'cerca' nelle impostazioni*). Le voci di questa
 * schermata sono una trentina, e ognuna è una chiamata a sé con le sue stringhe: passare il
 * testo cercato a mano vorrebbe dire trenta parametri da tenere d'accordo, e la voce nuova
 * che si dimentica di filtrarsi resterebbe in scena mentendo.
 * ⚠️ **Filtra la RIGA e non l'elenco**: non esiste un modello di dati delle impostazioni da
 * setacciare, esistono i composabili che le disegnano, e l'unico posto che conosce le
 * parole di una riga è la riga stessa.
 */
private val LocalQuery = compositionLocalOf { "" }

/**
 * Se una riga con questi testi deve comparire adesso.
 *
 * ⚠️ **Senza ricerca in corso compare tutto**, ed è il caso normale: la stringa vuota non è
 * un filtro che non trova niente, è l'assenza di filtro.
 * ⚠️ **Confronto senza maiuscole e senza accenti**: chi cerca 'cestino' lo scrive minuscolo,
 * e chi cerca la qualità la digita quasi sempre senza accento. Le ventotto lingue rendono il
 * secondo caso la regola e non l'eccezione.
 */
@Composable
private fun shown(vararg texts: String?): Boolean {
    val query = plain(LocalQuery.current)
    if (query.isEmpty()) return true
    return texts.any { it != null && plain(it).contains(query) }
}

/**
 * Il testo ridotto a quello che serve per confrontarlo: minuscolo e senza segni.
 *
 * ⚠️ **`Normalizer` e non una tabella di lettere**: la scomposizione canonica stacca il segno
 * dalla lettera in **tutte** le lingue, e il filtro che toglie i segni combinanti vale per
 * l'italiano come per il vietnamita. Una tabella scritta a mano coprirebbe le vocali
 * accentate italiane e sbaglierebbe le altre ventisette lingue.
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
private fun Searchable(vararg texts: String?, content: @Composable () -> Unit) {
    if (shown(*texts)) content()
}

/**
 * Il campo in cui si scrive che cosa si cerca, in testa alla pagina.
 *
 * ⚠️ **Scorre con l'elenco invece di restare inchiodato in testata**, ed è una scelta: appena
 * si scrive qualcosa l'elenco si accorcia a poche righe e il campo resta in vista da sé,
 * mentre una testata fissa costerebbe uno strato in più su tutte e quattro le pagine per un
 * caso che non capita.
 * ⚠️ La crocetta compare **solo con qualcosa scritto**: un tasto che non ha niente da
 * cancellare è un bersaglio che si preme per sbaglio.
 */
@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQuery,
        // ⚠️ Una riga sola: il testo cercato è una parola, e un campo che si allarga
        // spingerebbe l'elenco giù mentre lo si guarda.
        singleLine = true,
        placeholder = { Text(stringResource(R.string.settings_search)) },
        leadingIcon = {
            // ⚠️ Senza descrizione per lo schermo: la lente ripete quello che il campo dice
            // già col suo suggerimento, e il lettore leggerebbe 'cerca' due volte.
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
         * ⚠️⚠️ **UNA PASTIGLIA E NON UN RIQUADRO, dalla 1.21** (riscontro dell'utente,
         * 2026-09-01: *forse la barra di ricerca va migliorata esteticamente*). Il campo
         * squadrato con il contorno che c'era prima è il vestito di un **modulo da
         * compilare**, e in cima a una pagina fatta di righe e interruttori era l'unico
         * oggetto con un bordo: si leggeva come un dato da inserire invece che come un
         * filtro. La pastiglia piena è la forma con cui una ricerca si presenta dappertutto.
         * ⚠️ **Il filetto sotto se ne va con lui**: la riga che Material disegna sotto un
         * campo di testo serve a dire dove si scrive, e su una forma già chiusa raddoppia il
         * contorno. Si toglie negli **stati tutti e tre**, o ricompare toccando il campo.
         * ⚠️ **Il fondo è `surfaceContainerHigh` e non un colore scritto a mano**: è lo
         * stesso della bottomsheet della selezione, quindi segue il tema chiaro e scuro
         * senza che nessuno lo ritocchi.
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
    onResetHints: () -> Unit,
    onChooseEditor: () -> Unit,
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

    /*
     * ⚠️⚠️ **LE TRE VOCI DELICATE STANNO IN UNA SOTTO-PAGINA, per volontà dell'utente**
     * (2026-09-01: *sono impostazioni delicate: le voglio in una sotto-pagina 'Adattamento e
     * zoom'*). Sono le sole del pannello che cambiano il modo in cui un'immagine viene
     * **misurata** invece di che cosa si vede intorno: sbagliarle non rompe niente, ma rende
     * ogni fotografia diversa da come ci si aspetta, e chi le incontra per caso scorrendo
     * l'elenco non ha modo di saperlo.
     * ⚠️ **Restano nel gruppo del visualizzatore**, in cima: la sotto-pagina le raccoglie, non
     * le sposta altrove.
     */
    PageRow(
        label = stringResource(R.string.settings_zoom_page),
        summary = stringResource(R.string.settings_zoom_page_summary),
        onOpen = { onOpen(Page.ZOOM) }
    )

    /*
     * ⚠️ **Le due della selezione stanno QUI, accanto a quelle della vista**, e non in un
     * gruppo loro: una voce sola non fa un gruppo, e due mezze voci in fondo alla pagina
     * sarebbero più difficili da trovare di due righe fra quelle che si leggono già.
     */
    Choices(
        label = stringResource(R.string.settings_hand),
        // ⚠️ La spiegazione c'è dalla 1.17, ed è una richiesta: 'Posizione delle funzioni
        // principali' dice DOVE finiscono, non in base a che cosa si sceglie, e chi legge
        // 'Destra' o 'Sinistra' senza quella riga deve indovinare se parlano della mano o
        // del lato dello schermo.
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

    SwitchRow(
        label = stringResource(R.string.settings_list_path),
        // ⚠️ Anche qui la spiegazione arriva dopo l'etichetta, e per la stessa ragione: il
        // titolo dice che cosa si copia, non DOVE finisce nella lista, e 'in cima' è
        // esattamente il dettaglio che decide se l'interruttore serve.
        detail = stringResource(R.string.settings_list_path_desc),
        checked = settings.listPath,
        onChange = { onChange(settings.copy(listPath = it)) }
    )

    // ⚠️ Accanto alle altre voci della selezione, e non fra quelle del visualizzatore: parla
    // della testata che compare scegliendo, come il lato dominante e il percorso in testa
    // alla lista. Il costo che l'interruttore esiste per togliere sta su `Settings.pickWeight`.
    SwitchRow(
        label = stringResource(R.string.settings_pick_weight),
        detail = stringResource(R.string.settings_pick_weight_desc),
        checked = settings.pickWeight,
        onChange = { onChange(settings.copy(pickWeight = it)) }
    )

    /*
     * ⚠️⚠️ **UNA RIGA SOLA CON TRE GETTONI, e nella 1.26 questa è la TERZA forma in tre
     * versioni**: vale la pena saperlo per non riproporre le prime due. La `1.20` aveva un
     * interruttore più una riga 'Posizione' sotto; la `1.25` ha reso quella riga subordinata
     * (titolo leggero, rientrata, gettoni a destra); la `1.26` le fonde, ed è ancora una
     * richiesta dell'utente (2026-09-02: *'Posizione' adesso è l'unica riga che parte con un
     * rientro. Semplifichiamo: tutto in un unico posto. Tre chip: Disattivata, In alto, In
     * basso*).
     * ⚠️ **Il difetto che toglie non era la subordinazione ma la SOLITUDINE del rientro**:
     * una riga rientrata in una pagina dove nessun'altra lo è si legge come un errore di
     * impaginazione prima che come una gerarchia. Un rientro dice qualcosa solo dove ce ne
     * sono altri.
     * ⚠️⚠️ **'Disattivata' NON è un terzo valore nei dati, ed è voluto**: sotto restano
     * `infoVisible` e `infoPosition`, così spegnendo la barra e riaccendendola si ritrova il
     * lato che si era scelto. Un enum a tre valori l'avrebbe dimenticato, e questa riga è
     * proprio quella che si tocca per provare l'una e l'altra posizione.
     */
    Choices(
        label = stringResource(R.string.settings_info_visible),
        detail = null,
        options = InfoChoice.entries,
        selected = InfoChoice.of(settings),
        nameOf = {
            stringResource(
                when (it) {
                    InfoChoice.OFF -> R.string.settings_off
                    InfoChoice.TOP -> R.string.settings_top
                    InfoChoice.BOTTOM -> R.string.settings_bottom
                }
            )
        },
        /*
         * ⚠️⚠️ **IMPILATE DALLA 1.36, ed è la QUARTA forma di questa riga in quattro
         * versioni** (riscontro dell'utente, 2026-09-02: *voglio che vada bene in tutte le
         * lingue. Facciamo tre gettoni impilati e centrati, tutti della stessa larghezza*).
         * In riga stavano bene in italiano e andavano a capo altrove: il perché per esteso
         * sta sul parametro di [Choices].
         * ⚠️ **L'ordine è il suo**: 'In alto', 'In basso', 'Disattivata', cioè le due
         * posizioni prima e lo spegnimento in fondo. È anche l'ordine dell'enum, perché una
         * lista e la sua vista che divergono sono un difetto in attesa.
         */
        stacked = true,
        onSelect = { onChange(it.applyTo(settings)) }
    )

    /*
     * ⚠️ **Accanto alla barra delle info e non fra le voci dell'editor**: sono le due sole
     * impostazioni che dicono che cosa si vede SOPRA l'immagine mentre la si guarda, e chi
     * cerca l'una trova l'altra.
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
        onOpen = { onOpen(Page.FACTS) }
    )

    /*
     * ⚠️⚠️ **STA NEL GRUPPO DEL VISUALIZZATORE perché è di là che si modifica**: la voce
     * 'Modifica' vive nel menu del tocco lungo, e chi cerca l'impostazione la cerca accanto
     * alle altre cose di quel menu, non in un gruppo di sistema.
     * ⚠️⚠️ **È LO STESSO SELETTORE del primo utilizzo** (richiesta dell'utente), e la parola
     * 'stesso' è tecnica e non descrittiva: la finestra è una sola, [EditorPicker], aperta
     * dal modello (`chooseEditor`) invece che da questa schermata. Due finestre gemelle
     * sarebbero divergite alla prima voce aggiunta.
     */
    val editorLabel = stringResource(R.string.settings_editor)
    val editorDesc = stringResource(R.string.settings_editor_desc)
    Searchable(editorLabel, editorDesc) {
        val context = LocalContext.current
        val noEditor = stringResource(R.string.settings_editor_none)
        // ⚠️ Ricordato, e non chiesto a ogni disegno: leggerlo vuol dire interrogare il
        // `PackageManager`, cioè elencare le app del telefono. La chiave è la scelta, e in più
        // la frase di ripiego, che cambia quando cambia la lingua.
        val editorName = remember(settings.editorApp, noEditor) {
            Editors.labelOf(context, settings.editorApp)
        } ?: noEditor
        // ⚠️ La forma è ESATTAMENTE quella della cartella d'avvio qui sotto (titolo e
        // spiegazione, poi una riga con il valore in vigore e il tasto): sono la stessa cosa,
        // cioè una scelta che si fa altrove e qui si mostra, e due disposizioni diverse per lo
        // stesso mestiere farebbero cercare il tasto due volte.
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = editorLabel, style = MaterialTheme.typography.titleSmall)
            Detail(editorDesc)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = editorName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onChooseEditor) {
                Text(stringResource(R.string.settings_editor_pick))
            }
        }
    }

    /*
     * ⚠️⚠️ **VALE PER TUTTI GLI EDITOR DALLA `1.13`, ed è il rovescio di quello che c'era
     * scritto qui** (domanda dell'utente: *vale solo per l'editor interno o per tutti quelli
     * che supportano 'Modifica'?*). Fino alla `1.12` copriva il solo editor di casa, e la
     * nota di allora spiegava perché un'app di fuori non si potesse coprire: la copia si fa
     * **prima** di lanciarla, quindi si può eccome. Sta sotto la scelta dell'app perché è la
     * stessa faccenda, non perché ne riguardi una sola.
     */
    SwitchRow(
        label = stringResource(R.string.settings_editor_backup),
        detail = stringResource(R.string.settings_editor_backup_desc),
        checked = settings.editorBackup,
        onChange = { onChange(settings.copy(editorBackup = it)) }
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

    // ⚠️ Subito dopo, e non altrove: quella dice cosa si legge sotto una **cartella**,
    // questa cosa si legge sotto una **foto**. Sono la stessa domanda in due griglie, e
    // separarle vorrebbe dire cercarle in due posti.
    SwitchRow(
        label = stringResource(R.string.settings_grid_names),
        detail = stringResource(R.string.settings_grid_names_desc),
        checked = settings.gridNames,
        onChange = { onChange(settings.copy(gridNames = it)) }
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

    /*
     * ⚠️⚠️ **STA IN QUESTO GRUPPO PER MANCANZA DI UNO MIGLIORE, e vale dirlo**: il cestino
     * non è una faccenda di cartelle, ma è da questa schermata che si raggiunge (la voce sta
     * nel menu del suo tastino), e un gruppo nuovo per un interruttore solo sarebbe un titolo
     * con una riga sotto. Il giorno che le voci del cestino diventano due, il gruppo si fa.
     * ⚠️ **Ultima del gruppo, dopo le nascoste**: le tre voci sopra parlano della griglia
     * delle copertine, e infilarsi in mezzo a loro le avrebbe spezzate.
     */
    SwitchRow(
        label = stringResource(R.string.settings_bin),
        detail = stringResource(R.string.settings_bin_desc),
        checked = settings.binOn,
        onChange = { onChange(settings.copy(binOn = it)) }
    )

    /*
     * ⚠️ **Sta qui accanto al cestino per la stessa ragione dichiarata sopra**: parla del
     * visualizzatore, che non ha un gruppo suo in questa schermata, e un titolo nuovo per una
     * riga sarebbe un gruppo con dentro un interruttore.
     */
    SwitchRow(
        label = stringResource(R.string.settings_images_only),
        detail = stringResource(R.string.settings_images_only_desc),
        checked = settings.imagesOnly,
        onChange = { onChange(settings.copy(imagesOnly = it)) }
    )

    Group(stringResource(R.string.settings_group_start))

    SwitchRow(
        label = stringResource(R.string.settings_clipboard),
        detail = stringResource(R.string.settings_clipboard_desc),
        checked = settings.clipboardStart,
        onChange = { onChange(settings.copy(clipboardStart = it)) }
    )

    // ⚠️ L'interruttore e la riga della cartella si mostrano e si nascondono INSIEME, e per
    // questo la ricerca li tratta come un blocco solo: la riga sotto non ha un titolo suo, e
    // rimasta sola direbbe un nome di cartella senza dire di che cosa parla.
    val startLabel = stringResource(R.string.settings_start_folder)
    val startDesc = stringResource(R.string.settings_start_folder_desc)
    Searchable(startLabel, startDesc) {
        SwitchRow(
            label = startLabel,
            detail = startDesc,
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
    }

    /*
     * ⚠️⚠️ **IL GRUPPO DELLE 'FUNZIONALITÀ AVANZATE' NASCE NELLA 1.36 CON UNA VOCE SOLA, e
     * questa volta il gruppo per una riga si fa** (richiesta dell'utente, 2026-09-02: *nelle
     * impostazioni creiamo una nuova sezione 'Funzionalità avanzate' al cui interno c'è una
     * voce...*). Due note qui sopra dicono il contrario per il cestino e per lo sfoglio delle
     * sole immagini, e la differenza non è il numero di righe: quelle due sono impostazioni
     * normali senza un tema proprio, questa è una funzione che **può fare danni** e il titolo
     * è metà dell'avviso. Un interruttore così in mezzo agli altri sarebbe uno dei tanti.
     * ⚠️ **In fondo alla pagina, dopo tutti i gruppi**: chi scorre fin qui sta cercando
     * qualcosa di insolito, ed è esattamente il pubblico di questa voce.
     * ⚠️ **Il paragrafo è il `detail` della riga**, cioè lo stesso posto delle altre
     * spiegazioni: l'utente l'ha chiesto *sotto* la voce, che è dove `SwitchRow` lo mette già.
     * Il testo è **suo, parola per parola**.
     */
    Group(stringResource(R.string.settings_group_advanced))

    SwitchRow(
        label = stringResource(R.string.settings_ext_edit),
        detail = stringResource(R.string.settings_ext_edit_desc),
        checked = settings.extEdit,
        onChange = { onChange(settings.copy(extEdit = it)) }
    )

    /*
     * ⚠️⚠️ **STA FUORI DAI QUATTRO GRUPPI, in fondo, e non è una dimenticanza**: non è
     * un'impostazione, è un'**azione** sulla memoria dell'app, e non risponde a nessuna
     * delle quattro domande che i gruppi fanno. Metterla dentro uno di loro direbbe che è
     * una preferenza di quel tema; darle un gruppo suo vorrebbe dire un titolo per una riga
     * sola, cioè una parola in più che non aiuta a trovarla. In fondo è il posto dove le
     * azioni di ripristino stanno in ogni schermata di impostazioni, ed è dove si guarda.
     * ⚠️ Chiesta *per motivi di test*, ma resta utile: i veli si mostrano una volta e mai
     * più, quindi senza questa l'unica via per rivederli era cancellare i dati dell'app,
     * che si porta via anche le impostazioni.
     */
    val resetLabel = stringResource(R.string.settings_reset_hints)
    val resetDesc = stringResource(R.string.settings_reset_hints_desc)
    Searchable(resetLabel, resetDesc) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
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
            val context = LocalContext.current
            val done = stringResource(R.string.settings_reset_hints_done)
            TextButton(onClick = {
                onResetHints()
                // ⚠️ L'avviso serve perché l'effetto non si vede QUI: i veli tornano in
                // un'altra schermata, e un tasto che non dà segno di aver fatto qualcosa si
                // preme due volte.
                Toast.makeText(context, done, Toast.LENGTH_SHORT).show()
            }) { Text(stringResource(R.string.settings_reset_hints_do)) }
        }
    }

    // ⚠️ Mentre si cerca il piede non c'è: il filetto dice 'le impostazioni finiscono qui' e
    // il logo è la firma della pagina intera, e sotto due risultati direbbero l'una e l'altra
    // cosa di un elenco che non è la pagina.
    if (LocalQuery.current.isNotBlank()) return

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
    // ⚠️ Mentre si cerca i titoli di gruppo NON compaiono: i risultati vengono da gruppi
    // diversi e mescolati, e un titolo rimasto in piedi sopra due righe che non gli
    // appartengono direbbe il falso. È il comportamento di ogni ricerca in un elenco.
    if (LocalQuery.current.isNotBlank()) return
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
    if (!shown(label, summary)) return
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
/**
 * La sotto-pagina 'Adattamento e zoom': le tre voci che decidono come si misura un'immagine.
 *
 * ⚠️ **Sono esattamente quelle che stavano in cima al gruppo del visualizzatore, nello stesso
 * ordine**: chi le conosceva le ritrova dove le lasciate, un gradino più in là.
 */
@Composable
private fun ZoomAndFit(settings: Settings, onChange: (Settings) -> Unit) {
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

    /*
     * ⚠️⚠️ **STA QUI DALLA 1.26, e nella 1.25 stava nella pagina principale**: è una
     * correzione dell'utente (2026-09-02: *l'opzione relativa allo zoom va messa nella
     * sotto-pagina 'Adattamento e zoom' delle impostazioni. Quando aggiungi un'impostazione
     * nuova, attenzione a metterla nel posto giusto*). L'avevo messa accanto alla barra
     * delle info perché parla di quello che si vede **sopra** l'immagine; ma parla di
     * **zoom**, e questa è la pagina dello zoom: chi cerca un'impostazione la cerca per
     * argomento, non per dove compare.
     * ⚠️ **Spenta di fabbrica**: 'Adatta alla vista' e '100%' fanno quello che il doppio
     * tocco fa già, e stavano in mezzo a comandi che agiscono sul file. La descrizione nomina
     * il doppio tocco, perché una riga che dice solo 'rimetti due voci' non direbbe da che
     * cosa si viene.
     */
    SwitchRow(
        label = stringResource(R.string.settings_zoom_menu),
        detail = stringResource(R.string.settings_zoom_menu_desc),
        checked = settings.zoomInMenu,
        onChange = { onChange(settings.copy(zoomInMenu = it)) }
    )
}

/**
 * Le tre scelte della barra delle info come le vede chi guarda le impostazioni.
 *
 * ⚠️⚠️ **È una vista e non un dato**: sotto restano `infoVisible` e `infoPosition`, e la
 * ragione sta nella riga che la usa. Vive qui e non in `Settings.kt` perché non è un valore
 * salvato: è il modo in cui questa pagina presenta due valori che restano due.
 */
private enum class InfoChoice(override val token: String) : Choice {
    // ⚠️ **L'ordine è quello che si vede**, dalla `1.36`: le pastiglie seguono `entries`, e
    // l'utente le ha chieste 'In alto', 'In basso', 'Disattivata'. Fino alla `1.35`
    // 'Disattivata' era la prima, com'era stata chiesta allora.
    TOP("top"), BOTTOM("bottom"), OFF("off");

    fun applyTo(settings: Settings): Settings = when (this) {
        OFF -> settings.copy(infoVisible = false)
        TOP -> settings.copy(infoVisible = true, infoPosition = InfoPosition.TOP)
        BOTTOM -> settings.copy(infoVisible = true, infoPosition = InfoPosition.BOTTOM)
    }

    companion object {
        fun of(settings: Settings): InfoChoice = when {
            !settings.infoVisible -> OFF
            settings.infoPosition == InfoPosition.TOP -> TOP
            else -> BOTTOM
        }
    }
}

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
    /**
     * Le pastiglie **una sopra l'altra**, tutte della stessa larghezza e centrate.
     *
     * ⚠️⚠️ **NASCE NELLA 1.36 PER FARLE STARE IN 28 LINGUE** (riscontro dell'utente,
     * 2026-09-02, sulla posizione della barra info: *voglio che vada bene in tutte le lingue.
     * Facciamo tre gettoni impilati e centrati, tutti della stessa larghezza, sufficiente per
     * la parola più lunga nella lingua che esige più spazio*). In fila, `FlowRow` mandava a
     * capo dove capitava, quindi la stessa riga era una fila in italiano, due in tedesco e
     * tre in tamil: tre disposizioni diverse della stessa scelta.
     * ⚠️ **La larghezza NON è un numero**: è la massima intrinseca della colonna, cioè quella
     * della pastiglia più larga **nella lingua in vigore**. Un numero in dp sarebbe giusto in
     * una lingua e sbagliato nelle altre 27, che è esattamente il difetto da togliere.
     * ⚠️ **Non è il default**: le altre file (sfondo, tema, colonne, mano) stanno bene in
     * riga, e impilarle tutte allungherebbe la pagina di tre schermate.
     */
    stacked: Boolean = false,
    onSelect: (T) -> Unit
) {
    // ⚠️⚠️ **ANCHE I NOMI DELLE PASTIGLIE entrano nella ricerca**, e non solo il titolo della
    // riga: 'Scacchiera', 'Chiaro' e 'In alto' sono i nomi con cui si pensa a
    // quell'impostazione, mentre il titolo che le contiene ('Sfondo', 'Posizione') è la
    // parola che non si ricorda. Cercare quello che si vuole ottenere è il caso normale.
    // ⚠️ Calcolati UNA volta e riusati sotto: `nameOf` è una `stringResource`, e chiamarla
    // due volte per pastiglia raddoppierebbe le letture a ogni tasto premuto.
    val names = options.map { nameOf(it) }
    if (!shown(label, detail, *names.toTypedArray())) return
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 12.dp)
    )
    detail?.let { Detail(it) }
    if (stacked) {
        // ⚠️ **Due colonne annidate e non una**: quella di fuori occupa la riga e centra,
        // quella di dentro prende la larghezza della pastiglia più larga e la impone a tutte.
        // Con una sola colonna, `fillMaxWidth` sulle pastiglie le farebbe larghe quanto la
        // pagina, e `wrapContentWidth` le farebbe ognuna della propria misura.
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.width(IntrinsicSize.Max),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEachIndexed { at, option ->
                    FilterChip(
                        selected = option == selected,
                        onClick = { onSelect(option) },
                        // ⚠️ Il testo prende tutta la pastiglia e si centra: senza,
                        // resterebbe attaccato a sinistra e le tre parole di lunghezza
                        // diversa sembrerebbero disallineate dentro pastiglie uguali.
                        label = {
                            Text(
                                text = names[at],
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { at, option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(names[at]) }
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
    if (!shown(label, detail)) return
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

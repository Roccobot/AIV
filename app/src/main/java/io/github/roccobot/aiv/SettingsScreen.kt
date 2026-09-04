package io.github.roccobot.aiv

import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.Normalizer
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Le pagine delle impostazioni: la radice e quelle che le pendono sotto.
 *
 * ⚠️⚠️ **GRUPPI CON UN TITOLO, E SOTTO-PAGINE**, richiesta dell'utente (*le impostazioni sono
 * ancora da riordinare e mettere in sotto-pagine*). Prima era una colonna sola di dodici voci
 * separate da dieci filetti, che si scorreva per due schermate e mezzo senza sapere dove si
 * era: un filetto dice 'qui cambia argomento' ma non dice **quale**, e dieci filetti di fila
 * non dicono più niente. Un titolo di gruppo fa entrambe le cose, quindi i filetti sono
 * spariti tutti tranne quello sopra il piede.
 * ⚠️⚠️ **QUANTI SIANO NON STA SCRITTO, ED È UNA REGOLA** (istruzione dell'utente, 2026-09-03:
 * *è davvero necessario un testo che tenga il conto delle quantità di elementi UI? Non si
 * riuscirà mai a tenerla aggiornata*). Qui c'era scritto 'quattro gruppi e due sotto-pagine',
 * più 'le tre pagine' e 'cinque delle voci': **tre conti su tre erano diventati falsi**, e si
 * ricavano tutti con un comando o leggendo [Page]. Il criterio universale sta in
 * `rules/Roccobot.md` § '🔢 I conti si contano, non si scrivono'.
 * - ⚠️ **Il 'dodici voci' qui sopra RESTA, e non è un'eccezione di comodo**: è al passato e
 *   dichiarato tale, cioè descrive com'era prima di questo riordino, e un tempo passato non
 *   invecchia. Quello che invecchia è il conto di com'è **adesso**.
 *
 * ⚠️⚠️ **SOTTO-PAGINA SI DIVENTA IN QUATTRO MODI, e la regola completa vive in `AIV/CLAUDE.md`**,
 * § '⚙️ Dove va un'impostazione, e chi la deve trovare': perché la voce è un **elenco** che
 * cresce e porta comandi propri riga per riga (le cartelle nascoste e i dati di Info erano da
 * sole più di metà dell'altezza della schermata); perché le voci sono **delicate** e il tocco
 * in più è una protezione ('Adattamento e zoom', nata così su richiesta dell'utente: *sono
 * impostazioni delicate*); perché la **famiglia** ha superato la soglia dell'utente
 * (*fino a 2-3 opzioni correlate basta una sotto-sezione; più di 2-3 si va con la
 * sotto-pagina*), che è il modo con cui nasce 'Opzioni di visualizzazione'; oppure perché è un
 * **comando che ha bisogno di un paragrafo**, che è il quarto e nasce con 'Elimina le
 * miniature memorizzate'.
 * - ⚠️ **Una riga sola che non è né un elenco né delicata, in una sotto-pagina costerebbe un
 *   tocco senza guadagnare niente**, ed è la clausola che vale più delle altre.
 * - ⚠️ **La soglia si conta sulla FAMIGLIA e non sulla sezione**: 'Aspetto' porta il tema, la
 *   coppia dello sfondo e il velo, cioè tre famiglie, e nessuna arriva alla soglia. Contandola
 *   sulla sezione, il tema dell'app finirebbe dietro un tocco.
 *
 * ⚠️⚠️ **OGNI RIGA CHE APRE UNA PAGINA DICE QUANTO È LUNGO QUELLO CHE C'È DENTRO**, e non è
 * decorazione: nascondere un elenco dietro un tocco fa perdere l'unica cosa che l'elenco
 * aperto diceva da sé, cioè la sua lunghezza. Senza il riepilogo bisogna entrare per sapere
 * se c'è qualcosa, che è esattamente il costo che le sotto-pagine dovevano togliere.
 * - ⚠️⚠️ **E IL RIEPILOGO SI CALCOLA O SI COMPONE, MAI SI SCRIVE A MANO**, per la stessa
 *   ragione dei conti nei commenti: quello delle cartelle e quello di Info contano le righe
 *   (`R.plurals`), quelli delle altre due si compongono dalle stesse stringhe che la pagina
 *   usa dentro. Il precedente è misurato: `settings_zoom_page_summary` era una frase fissa che
 *   nominava tre argomenti, la `1.26` ne ha portato là un quarto, e quella frase è rimasta
 *   falsa in ventisette lingue fino alla `1.46`.
 *
 * ⚠️⚠️ **LA RICERCA DEVE TROVARE OGNI VOCE, DOVUNQUE VIVA, e la copertura si scrive nello
 * stesso giro della sotto-pagina.** [LocalQuery] è fornito nel solo ramo della radice, quindi
 * una voce spostata dietro un tocco **esce** dalla ricerca: le due vie con cui non esce sono
 * [PageOfRows], per le pagine fatte di righe, e il parametro `extra` di [PageRow], per quelle
 * che sono elenchi con comandi riga per riga. ⚠️ **Fidarsi del riepilogo non è una terza
 * via**: è quello che ha lasciato fuori dalla ricerca le voci dello zoom per venti versioni.
 *
 * ⚠️⚠️ **LA SPIEGAZIONE SOTTO UNA VOCE HA UN CRITERIO, E IL CODICE SE NE È ALLONTANATO.** Il
 * criterio, che regge: una spiegazione sotto un'impostazione il cui nome dice già tutto è
 * rumore, e dopo tre nessuno legge la quarta; quindi ce l'ha la voce in cui il nome non può
 * portare il senso (a che cosa SERVE la scacchiera, che cosa fa 'ingrandisci' quando è spento,
 * che cosa significa 100%, di che cosa è il rovescio l'ordine inverso, che cosa si apre
 * esattamente all'avvio). ⚠️ **Ma oggi la spiegazione ce l'hanno quasi tutte**, quindi il
 * criterio è scritto e non applicato, e la potatura è una decisione dell'utente perché cambia
 * quello che legge: sta fra le voci aperte del brief. Fino alla `1.46` qui c'era scritto che le
 * voci con la spiegazione sono cinque, ed è quel numero fermo ad aver nascosto lo scostamento.
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
     * ⚠️⚠️ **SALVATO E NON SOLO RICORDATO, dalla `1.46`**: `ViewerActivity` non dichiara
     * `configChanges`, quindi ruotare il telefono ricrea la composizione, e con un `remember`
     * semplice si tornava nella radice perdendo anche quello che si stava cercando. Con una
     * sotto-pagina in più lo stato perso pesa di più.
     * ⚠️ **Si salva l'ORDINALE e non la costante**: un `Int` non pone dubbi su che cosa il
     * salvataggio sappia serializzare, mentre un enum ci arriva solo se qualcuno gli scrive un
     * `Saver`. Il valore vero resta [page], che si legge sotto.
     */
    var pageAt by rememberSaveable { mutableIntStateOf(Page.ROOT.ordinal) }
    val page = Page.entries[pageAt]
    // ⚠️⚠️ **LO SCORRIMENTO DELLA RADICE VIVE QUI E NON DENTRO LA PAGINA**: le tre pagine
    // stanno in tre rami di un `when`, quindi uno stato ricordato dentro `Shell` nascerebbe
    // nuovo a ogni ritorno, e si tornerebbe indietro trovandosi in cima. Tenuto qui,
    // sopravvive al passaggio, perché `SettingsScreen` non esce di scena. Le sotto-pagine
    // invece il proprio lo vogliono nuovo: si entra dall'inizio.
    val rootScroll = rememberScrollState()
    // ⚠️ Sta QUI e non dentro `RootPage` per la stessa ragione dello scorrimento: una
    // sotto-pagina e il ritorno non devono cancellare quello che si stava cercando. E si
    // salva, come la pagina, perché una rotazione non è un modo di annullare una ricerca.
    var query by rememberSaveable { mutableStateOf("") }
    // ⚠️ Vince su quello dell'attività (`ViewerActivity`, che qui chiama `leaveSettings`)
    // perché è registrato DOPO: il dispatcher di Android serve l'ultimo arrivato fra quelli
    // accesi. È lo stesso annidamento della selezione nella griglia, che regge da versioni.
    BackHandler(enabled = page != Page.ROOT) { pageAt = Page.ROOT.ordinal }
    // ⚠️ Con una ricerca in corso Indietro la annulla invece di uscire, ed è quello che fa
    // ogni ricerca dentro un elenco: uscire dalle impostazioni lasciando l'elenco filtrato
    // costringerebbe a rientrare per rivederlo intero. I due non sono mai accesi insieme.
    BackHandler(enabled = page == Page.ROOT && query.isNotBlank()) { query = "" }

    /*
     * ⚠️⚠️ **LA CACHE DELLE MINIATURE SI MISURA E SI SVUOTA QUI, SENZA PASSARE DAL MODELLO**,
     * e non è una scorciatoia: il modello tiene lo **stato dell'app**, cioè quello che altre
     * schermate osservano, e una cartella di file temporanei non è stato di nessuno. Il
     * contesto basta a leggerla e a svuotarla, e mettere un campo nel modello vorrebbe dire
     * tenerlo d'accordo con un disco che si svuota anche da solo (Android può farlo quando lo
     * spazio finisce).
     * ⚠️ **La misura è sincrona, lo svuotamento no**: leggere è un `stat` per file sotto un
     * tetto di poche centinaia, e il valore serve **prima** del primo fotogramma o la riga
     * mostrerebbe un riepilogo falso per un istante; cancellare invece sono altrettante
     * scritture, e quelle vanno su un thread di I/O.
     * ⚠️ [emptied] non è un contatore di cortesia: è la sola cosa che dice a [remember] di
     * rifare la misura dopo uno svuotamento, e senza di lui la riga continuerebbe a dire i
     * megabyte di prima finché non si esce dalle impostazioni.
     */
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var emptied by remember { mutableIntStateOf(0) }
    val thumbBytes = remember(emptied) { AvifCache.bytes(context) }
    val clearThumbs: () -> Unit = {
        scope.launch {
            withContext(Dispatchers.IO) { AvifCache.clear(context) }
            Thumbs.forgetAll(context)
            emptied++
        }
    }

    when (page) {
        Page.ROOT -> Shell(
            title = stringResource(R.string.settings_title),
            onBack = onBack,
            modifier = modifier,
            scroll = rootScroll,
            version = true
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
                        thumbBytes = thumbBytes,
                        onClearThumbs = clearThumbs,
                        onOpen = { pageAt = it.ordinal }
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
            onBack = { pageAt = Page.ROOT.ordinal },
            modifier = modifier
        ) {
            Detail(stringResource(R.string.settings_facts_desc))
            FactFields(settings = settings, onChange = onChange)
        }

        Page.HIDDEN -> Shell(
            title = stringResource(R.string.settings_hidden),
            onBack = { pageAt = Page.ROOT.ordinal },
            modifier = modifier
        ) {
            HiddenFolders(settings = settings, onChange = onChange)
        }

        Page.ZOOM -> Shell(
            title = stringResource(R.string.settings_zoom_page),
            onBack = { pageAt = Page.ROOT.ordinal },
            modifier = modifier
        ) {
            ZoomAndFit(settings = settings, onChange = onChange)
        }

        Page.VIEWS -> Shell(
            title = stringResource(R.string.view_options),
            onBack = { pageAt = Page.ROOT.ordinal },
            modifier = modifier
        ) {
            ViewOptionsPage(settings = settings, onChange = onChange)
        }

        /*
         * ⚠️⚠️ **UNA SOTTO-PAGINA PER UN'AZIONE SOLA, ed è un quarto modo di diventarlo**
         * (richiesta dell'utente, 2026-09-04: *un 'Elimina le miniature memorizzate' con un >
         * che ti porta ad una sotto-schermata dove c'è un avviso al centro ... Sotto, un
         * pulsante*). I tre modi scritti in `AIV/CLAUDE.md` sono l'elenco che cresce, la voce
         * delicata e la famiglia oltre la soglia, e questa non è nessuno dei tre: non è
         * delicata (*non è un'operazione con risvolti potenzialmente dannosi*, parole sue) e
         * non è una famiglia. È un **comando che ha bisogno di un paragrafo**, e un paragrafo
         * di quattro righe in una riga della pagina piatta la farebbe alta il doppio delle
         * altre per una cosa che si fa una volta l'anno.
         * ⚠️ **Non porta `lowered()`**, e non è una dimenticanza: il 15% più in basso è la
         * definizione di 'centrato' per quello che **si apre in mezzo** allo schermo, cioè
         * dialoghi, pannelli e menu, che hanno una finestra propria e un centro da spostare.
         * Questa è una schermata intera, e il suo contenuto sta già sotto la testata.
         */
        Page.THUMBS -> Shell(
            title = stringResource(R.string.settings_thumbs),
            onBack = { pageAt = Page.ROOT.ordinal },
            modifier = modifier,
            scrolls = false
        ) {
            /*
             * ⚠️⚠️ **`BoxWithConstraints` E NON UNA COLONNA CHE SCORRE**, perché le due cose
             * che servono qui si escludono a vicenda: dentro un `verticalScroll` l'altezza di
             * un figlio è quella del suo contenuto, quindi non c'è nessuno spazio avanzato da
             * distribuire e 'al centro' non vuol dire più niente. Sapendo quanto spazio c'è,
             * la colonna si fa alta **almeno** così e centra; e quando il contenuto è più alto
             * (schermo piccolo di traverso, corpo del testo ingrandito) scorre invece di
             * essere tagliato.
             */
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val room = maxHeight
                ThumbsCard(
                    head = null,
                    onClear = clearThumbs,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = room)
                )
            }
        }
    }
}

/**
 * Quale pagina si sta guardando.
 *
 * ⚠️ **La profondità è UNO**: una sotto-pagina non ne apre un'altra, perché la navigazione è
 * questo valore e nient'altro, senza una pila, e Indietro riporta alla radice. Una famiglia
 * che ne conterrebbe un'altra tiene nella pagina piatta la riga che apre la seconda.
 */
private enum class Page { ROOT, FACTS, HIDDEN, ZOOM, VIEWS, THUMBS }

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
 * La pagina piatta: le sezioni, e le righe che aprono le sotto-pagine.
 *
 * ⚠️ L'ordine delle sezioni va dal generale al particolare: prima com'è fatta l'app, poi come
 * si guarda un'immagine, poi come si sfoglia, come si comanda e come si modifica, poi come si
 * trovano le cartelle, e per ultimo che cosa succede all'accensione, che è la voce che si
 * tocca una volta e non si guarda più. È un criterio **stabile**: ordinare per frequenza, o
 * per la domanda che si fa più spesso, si riaprirebbe a ogni voce aggiunta.
 * ⚠️⚠️ **DOVE VA UNA VOCE NUOVA LO DICE UNA REGOLA, e non questo elenco**: sta in
 * `AIV/CLAUDE.md` § '⚙️ Dove va un'impostazione, e chi la deve trovare', e la cosa da leggere
 * prima di aggiungere una riga è quella. Qui accanto a ogni sezione c'è il **perché** di
 * quella sezione, che è l'altra metà.
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

    /*
     * ⚠️⚠️ **ULTIMA DEL GRUPPO E NON SUBITO DOPO IL TEMA**, che pure le somiglia: le due
     * righe sopra sono una coppia (che cosa c'è dietro la fotografia, e di che tinta), e
     * infilarsi in mezzo a loro le spezzerebbe. Questa parla di quello che c'è dietro le
     * **finestre**, che è un'altra domanda.
     * ⚠️ **La spiegazione dichiara il costo**, che è la ragione per cui l'interruttore esiste:
     * chi lo accende deve sapere che cosa sta comprando, o leggerà la lentezza come un
     * difetto dell'app.
     */
    SwitchRow(
        label = stringResource(R.string.settings_veil),
        detail = stringResource(R.string.settings_veil_desc),
        checked = settings.veil,
        onChange = { onChange(settings.copy(veil = it)) }
    )

    Group(stringResource(R.string.settings_group_viewer))

    /*
     * ⚠️⚠️ **LE VOCI DELICATE STANNO IN UNA SOTTO-PAGINA, per volontà dell'utente**
     * (2026-09-01: *sono impostazioni delicate: le voglio in una sotto-pagina 'Adattamento e
     * zoom'*). Sono le sole del pannello che cambiano il modo in cui un'immagine viene
     * **misurata** invece di che cosa si vede intorno: sbagliarle non rompe niente, ma rende
     * ogni immagine diversa da come ci si aspetta, e chi le incontra per caso scorrendo
     * l'elenco non ha modo di saperlo.
     * ⚠️ **Restano nel gruppo del visualizzatore**, in cima: la sotto-pagina le raccoglie, non
     * le sposta altrove. Era la richiesta alla lettera.
     */
    PageOfRows(
        label = stringResource(R.string.settings_zoom_page),
        // ⚠️⚠️ **IL RIEPILOGO SI COMPONE DAI TITOLI DELLE VOCI, dalla 1.46, e prima era una
        // frase a mano**: `settings_zoom_page_summary` nominava tre argomenti e la pagina ne
        // portava quattro dalla 1.26, cioè era invecchiata in silenzio in ventisette lingue.
        // Composto così non può: se una voce entra, esce o cambia nome, il riepilogo la segue.
        summary = listOf(
            stringResource(R.string.settings_fit_grow),
            stringResource(R.string.settings_zoom_max),
            stringResource(R.string.settings_scale_mode),
            stringResource(R.string.settings_zoom_menu)
        ).joinToString(SUMMARY_JOIN),
        onOpen = { onOpen(Page.ZOOM) }
    ) { ZoomAndFit(settings = settings, onChange = onChange) }

    /*
     * ⚠️⚠️ **INTERRUTTORE PIÙ RIGA SUBORDINATA, dalla 1.38, ED È UN RITORNO ALLA FORMA DELLA
     * 1.25** (riscontro `chip-colonna`, 2026-09-02: *torna indietro nelle impostazioni -> due
     * righe: 'Barra delle info' in linea con l'interruttore off/on; 'Posizione'
     * gerarchicamente subordinata alla riga precedente, in linea e allineati a destra, i chip
     * 'In alto' 'In basso' in questo ordine*).
     * ⚠️⚠️ **LA 1.26 AVEVA FUSO LE DUE RIGHE IN TRE GETTONI, E LA RICHIESTA ERA DELL'ALTRO
     * POSTO: È UN MIO SCAMBIO, e lui lo ha detto per esteso** (*quando l'ho chiesto per il
     * pannello l'hai fatta nelle impostazioni, e viceversa; di conseguenza ti ho sempre dato il
     * feedback sbagliato*). I tre gettoni impilati che questa riga si porta dietro da tre
     * versioni sono nati da quello scambio, e ogni riscontro che li ha limati stava limando la
     * cosa sbagliata. Non è quindi la 'quinta forma in cinque versioni': è la 1.25 rimessa dove
     * era, con l'aggiunta che segue.
     * ⚠️ **La forma corretta di 'gerarchicamente subordinata' è quella già scritta**: si veda
     * [InfoSideRow], che è la riga della 1.25 tornata in scena, e che adesso vive in un file
     * suo perché la vogliono **identica** in due posti.
     */
    SwitchRow(
        label = stringResource(R.string.settings_info_visible),
        detail = null,
        checked = settings.infoVisible,
        onChange = { onChange(settings.copy(infoVisible = it)) }
    )

    /*
     * ⚠️⚠️ **I DUE GETTONI SI TOCCANO SOLO A BARRA ACCESA, dalla 1.38** (stessa richiesta: *in
     * entrambi i casi, i due chip della posizione sono selezionabili solo quando l'interruttore
     * principale è ON*), e questa è la parte NUOVA rispetto alla 1.25, dove restavano sempre
     * attivi.
     * ⚠️⚠️ **MA IL VALORE SOTTO NON SI PERDE, ed è la ragione per cui `infoPosition` resta un
     * campo suo**: spenta la barra, il lato scelto rimane scritto e si ritrova riaccendendola.
     * Spegnere i gettoni è una cosa che riguarda quello che si può toccare, non quello che si
     * ricorda.
     * ⚠️ **Chi cerca 'In alto' con la ricerca li trova comunque**, spenti: nasconderli
     * direbbe che quell'impostazione non esiste, mentre esiste e ha un interruttore sopra.
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
             * ⚠️⚠️ **NIENTE RIENTRO A SINISTRA, dalla 1.41, e la 1.38 lo aveva rimesso**
             * (riscontro `barra-impostazioni`, 2026-09-03: *di nuovo 'Posizione' con rientro
             * -> deve stare allineato a sinistra e basta, senza spazi/indentazioni
             * iniziali*). 'Gerarchicamente subordinata' lo dicono il corpo leggero del
             * titolo e il fatto che i gettoni si spengono con l'interruttore sopra: uno
             * scalino a sinistra è una terza cosa, e non l'ha chiesta.
             * ⚠️ **Resta il solo distacco in alto**, che non è un rientro ma l'aria fra due
             * righe.
             */
            modifier = Modifier.padding(top = 4.dp)
        )
    }

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
        onOpen = { onOpen(Page.FACTS) },
        /*
         * ⚠️⚠️ **QUESTA PAGINA NON SI APPIATTISCE NELLA RICERCA, e i suoi campi si cercano da
         * qui**: dentro c'è un ELENCO con due comandi per riga, la casella e le due frecce, e
         * le frecce lavorano sull'ordine INTERO, quindi in un elenco filtrato manderebbero un
         * campo in una posizione che non si vede. La copertura è l'altra: i nomi dei campi
         * entrano fra i testi che la ricerca confronta su questa riga, e chi cerca 'fotocamera'
         * trova la riga che porta dove quella voce vive. Costa zero stringhe, perché quei nomi
         * esistono già in tutte le lingue.
         */
        extra = settings.factOrder.map { stringResource(it.label) }
    )

    /*
     * ⚠️⚠️ **SEZIONE NUOVA NELLA `1.46`, E NASCE PER SCIOGLIERE UN RIPIEGO CHE IL CODICE
     * CONFESSAVA**: lo sfoglio delle sole immagini stava fra le cartelle con la scusa scritta
     * accanto (*parla del visualizzatore, che non ha un gruppo suo in questa schermata*), e
     * quella frase era **falsa** da quando esiste il gruppo del visualizzatore qui sopra.
     * Nessun referto l'aveva vista, e una collocazione che ha bisogno di giustificarsi è una
     * famiglia che non esiste ancora.
     * ⚠️⚠️ **IL TITOLO È UNA PAROLA SOLA DALLA 1.48, E PRIMA NE NOMINAVA DUE** ('Video e
     * scorrimento', riscritto dall'utente nel giro della `1.46`). Il titolo vecchio elencava
     * quello che c'è dentro; questo dice **che cosa si viene a fare qui**, cioè decidere come
     * ci si muove fra un'immagine e l'altra, ed è la domanda che tiene insieme tutte e tre le
     * voci: che cosa si salta, che cosa parte da sé, e da che parte si va.
     * ⚠️ **Un titolo che elenca non è vietato**, e in questa schermata ce ne sono ancora: la
     * regola dice che una sezione a nessuna domanda risponde, dice dove si è, quindi un titolo
     * vale l'altro finché lo dice bene.
     */
    Group(stringResource(R.string.settings_group_clips))

    SwitchRow(
        label = stringResource(R.string.settings_images_only),
        detail = stringResource(R.string.settings_images_only_desc),
        checked = settings.imagesOnly,
        onChange = { onChange(settings.copy(imagesOnly = it)) }
    )

    /*
     * ⚠️⚠️ **SENZA SPIEGAZIONE, E ACCANTO ALLA VOCE CHE LA DÀ** (richiesta dell'utente,
     * 2026-09-03: *senza testo esplicativo*). Le due voci sono l'una il rovescio dell'altra,
     * quella spegne il **gesto** e questa accende il **tocco**, e la descrizione di sopra
     * nomina già il tocco: staccarle avrebbe lasciato una voce muta senza niente intorno che
     * la spieghi. Il perché sia spenta di fabbrica sta su `Settings.clipAutoplay`.
     */
    SwitchRow(
        label = stringResource(R.string.settings_clip_autoplay),
        detail = null,
        checked = settings.clipAutoplay,
        onChange = { onChange(settings.copy(clipAutoplay = it)) }
    )

    // ⚠️ Una voce sola non prende un titolo suo, e va nella famiglia la cui domanda le sta
    // più vicina: il verso dello scorrimento sta coi video perché è l'altra cosa che il
    // gesto di sfogliare decide.
    SwitchRow(
        label = stringResource(R.string.settings_reverse_order),
        detail = stringResource(R.string.settings_reverse_order_desc),
        checked = settings.reverseSequence,
        onChange = { onChange(settings.copy(reverseSequence = it)) }
    )

    /*
     * ⚠️⚠️ **SEZIONE NUOVA NELLA `1.46`, e che le tre siano una famiglia lo diceva già il
     * codice**: la testata della selezione stava *accanto alle altre voci della selezione,
     * come il lato dominante e il percorso in testa alla lista*. Quello che mancava era il
     * titolo, e la nota di allora diceva anche perché (*una voce sola non fa un gruppo, e due
     * mezze voci in fondo alla pagina sarebbero più difficili da trovare*): con tre voci il
     * gruppo si fa, e quella scusa non serve più.
     * ⚠️ **La mano NON sta sotto 'Aspetto'**: non è come l'app è vestita, e sotto quel titolo
     * nessuno la cerca.
     * ⚠️ **Il titolo dice 'indicatori' e non più 'selezione' dalla 1.48** (riscritto
     * dall'utente nel giro della `1.46`): due delle tre voci accendono qualcosa che si
     * **guarda** (il percorso in testa alla lista, la testata della selezione), e 'selezione'
     * ne nominava una sola delle due.
     */
    Group(stringResource(R.string.settings_group_input))

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

    // ⚠️ Il costo che quest'interruttore esiste per togliere sta su `Settings.pickWeight`.
    SwitchRow(
        label = stringResource(R.string.settings_pick_weight),
        detail = stringResource(R.string.settings_pick_weight_desc),
        checked = settings.pickWeight,
        onChange = { onChange(settings.copy(pickWeight = it)) }
    )

    /*
     * ⚠️⚠️ **SEZIONE NUOVA NELLA `1.46`, E MANTIENE UNA PROMESSA CHE IL CODICE AVEVA MESSO PER
     * ISCRITTO**: il cestino stava fra le cartelle *PER MANCANZA DI UNO MIGLIORE*, con la riga
     * *il giorno che le voci del cestino diventano due, il gruppo si fa*. La famiglia è una
     * domanda sola: come non si perde un file quando lo si modifica o lo si cancella. E il
     * legame fra le due metà non è supposto, lo dice la stringa pubblicata della copia di
     * sicurezza, che finisce proprio nel cestino.
     * ⚠️ **Tre voci, quindi sotto-SEZIONE e non sotto-pagina**: la soglia è dell'utente (*fino
     * a 2-3 opzioni correlate basta una sotto-sezione della pagina principale*), e un cancello
     * sul cestino allontanerebbe la risposta a 'come recupero un file cancellato'.
     * ⚠️ **L'ordine interno segue il percorso di un file**: con che cosa si modifica, se ne
     * resta una copia, e se cancellare si può disfare.
     * ⚠️⚠️ **IL TITOLO NON NOMINA PIÙ IL CESTINO DALLA 1.48** ('Modifica e cestino', riscritto
     * dall'utente nel giro della `1.46`), e la voce del cestino è **rimasta qui**: chi la cerca
     * la trova con la ricerca, che confronta il titolo della voce e non quello della sezione.
     * La parola nuova, 'backup', nomina la copia di sicurezza, che prima nel titolo non
     * compariva pur essendo la voce in mezzo.
     */
    Group(stringResource(R.string.settings_group_files))

    /*
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
        // ⚠️ La forma è ESATTAMENTE quella della cartella d'avvio (titolo e spiegazione, poi
        // una riga con il valore in vigore e il tasto): sono la stessa cosa, cioè una scelta
        // che si fa altrove e qui si mostra, e due disposizioni diverse per lo stesso
        // mestiere farebbero cercare il tasto due volte.
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

    // ⚠️ Ultima della sezione, e non è un ordine casuale: le due sopra parlano di una
    // modifica, questa di una cancellazione, e il cestino è la rete che le raccoglie tutte
    // e due.
    SwitchRow(
        label = stringResource(R.string.settings_bin),
        detail = stringResource(R.string.settings_bin_desc),
        checked = settings.binOn,
        onChange = { onChange(settings.copy(binOn = it)) }
    )

    Group(stringResource(R.string.settings_group_browse))

    /*
     * ⚠️⚠️ **PAGINA NUOVA, ED È LA SOLA AGGIUNTA DI STRUTTURA DELLA `1.46`**: le voci che
     * decidono come si presentano gli elenchi di casa sono una domanda sola e sono più di
     * tre, quindi la soglia dell'utente le manda dietro un tocco.
     * ⚠️⚠️ **E QUATTRO DI LORO PRIMA NON SI RAGGIUNGEVANO AFFATTO DA QUI**: le opzioni della
     * vista a elenco e di quella ad albero vivevano soltanto nel dialogo della schermata
     * iniziale, quindi la ricerca delle impostazioni non le trovava. Non era una scelta
     * dichiarata come quella di `Settings.folderView`, che l'eccezione ce l'ha scritta nel
     * KDoc: era un buco, e il precedente di casa è che una scelta da guardare ha **casa e
     * scorciatoia insieme**, come le colonne.
     * ⚠️ **Il titolo è LA STESSA stringa che titola il dialogo**, ed è deliberato: due
     * superfici con lo stesso titolo sono la prova visibile che il dialogo è una scorciatoia
     * alla stessa cosa, e non un secondo posto in cui quella scelta vive per conto suo.
     */
    PageOfRows(
        label = stringResource(R.string.view_options),
        // ⚠️ Composto dai tre nomi che la pagina usa come titolini: zero stringhe nuove, e se
        // un titolino cambia cambia anche il riepilogo.
        summary = listOf(
            stringResource(R.string.view_grid),
            stringResource(R.string.view_list),
            // ⚠️ `hub_view_tree` ('Cartelle di sistema') e NON `view_tree` ('Cartelle'), che
            // collide col titolo di questa sezione.
            stringResource(R.string.hub_view_tree)
        ).joinToString(SUMMARY_JOIN),
        onOpen = { onOpen(Page.VIEWS) }
    ) { ViewOptionsPage(settings = settings, onChange = onChange) }

    /*
     * ⚠️⚠️ **L'ELENCO DELLE NASCOSTE È METÀ DELLA FUNZIONE, non un di più**: si nasconde con
     * un tocco lungo, cioè da un'altra schermata e senza lasciare traccia, quindi se non ci
     * fosse un posto in cui rivedere che cosa si è nascosto l'unico modo di riavere una
     * cartella sarebbe indovinare che esiste quest'impostazione. Una funzione che toglie
     * qualcosa deve dire dove l'ha messa.
     * ⚠️ Compare **solo quando c'è qualcosa**, e la sotto-pagina non ha cambiato la scelta:
     * una riga sempre presente e quasi sempre vuota è rumore in una schermata che si scorre.
     * La pagina invece la stringa vuota la sa dire, perché ci si può restare dentro dopo aver
     * rimostrato l'ultima.
     */
    if (settings.hiddenFolders.isNotEmpty()) {
        PageRow(
            label = stringResource(R.string.settings_hidden),
            summary = pluralStringResource(
                R.plurals.settings_hidden_count,
                settings.hiddenFolders.size,
                settings.hiddenFolders.size
            ),
            onOpen = { onOpen(Page.HIDDEN) },
            // ⚠️ Anche questa pagina è un ELENCO e non si appiattisce, perché ogni riga porta
            // il suo tasto 'Mostra': la copertura sono i percorsi, che entrano fra i testi da
            // confrontare e non costano una stringa, perché sono dati. Guadagno collaterale:
            // una cartella nascosta diventa cercabile per nome, cosa che prima non era.
            extra = settings.hiddenFolders.sorted()
        )
    }

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
     * ⚠️⚠️ **IL RIEPILOGO È LA MISURA, e per una volta 'quanto c'è dentro' non sono righe ma
     * megabyte**: la pagina che si apre non è un elenco, quindi la sua lunghezza non dice
     * niente, mentre la cosa che il comando riguarda una misura ce l'ha. Ed è calcolata, come
     * vuole la regola: una frase fissa qui direbbe due volte quello che il titolo già dice.
     * ⚠️ **La misura la formatta il SISTEMA** (`Formatter`), che la scrive con l'unità e il
     * separatore decimale della lingua in corso: una stringa nostra sarebbe una traduzione in
     * ventotto lingue per dire quello che Android dice già.
     * ⚠️ Il caso zero ha la sua frase perché '0 B' si legge come un difetto, non come 'non c'è
     * niente da buttare'.
     */
    val thumbsLabel = stringResource(R.string.settings_thumbs)
    val thumbsWarn = stringResource(R.string.settings_thumbs_warn)
    val thumbsSummary =
        if (thumbBytes <= 0L) stringResource(R.string.settings_thumbs_empty)
        else Formatter.formatShortFileSize(LocalContext.current, thumbBytes)
    PageOfRows(
        label = thumbsLabel,
        summary = thumbsSummary,
        onOpen = { onOpen(Page.THUMBS) }
    ) {
        // ⚠️ `Searchable` perché il corpo è scritto a mano: le righe di serie si filtrano da
        // sé, un paragrafo con un tasto no, e resterebbe in scena a ogni ricerca.
        Searchable(thumbsLabel, thumbsWarn) {
            ThumbsCard(head = thumbsLabel, onClear = onClearThumbs)
        }
    }

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
    /**
     * Se la colonna scorre tutta intera.
     *
     * ⚠️ Spento vuol dire che **il contenuto si prende l'altezza**, ed è l'unico modo di avere
     * un figlio con `weight`: in una colonna che scorre lo spazio avanzato non esiste, perché
     * l'altezza è la somma dei figli. Lo spegne la pagina che ha una cosa sola da mettere in
     * mezzo, e da lì in poi lo scorrimento se lo fa quel figlio.
     */
    scrolls: Boolean = true,
    /**
     * Se in fondo alla riga del titolo compare il numero di versione.
     *
     * ⚠️⚠️ **SOLO NELLA PAGINA PRINCIPALE, e per richiesta** (utente, 2026-09-04: *nelle
     * impostazioni, in linea con il titolo 'Impostazioni' ma a destra, vorrei che apparisse il
     * numero della versione dell'app*). Su ogni sotto-pagina sarebbe la stessa riga ripetuta
     * cinque volte, e il numero smetterebbe di essere una firma per diventare un ornamento.
     */
    version: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .then(if (scrolls) Modifier.verticalScroll(scroll) else Modifier)
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
            if (version) {
                /*
                 * ⚠️⚠️ **IL NUMERO ARRIVA DA `BuildConfig` E NON DA UNA STRINGA**: la fonte
                 * unica del `versionName` è `app/build.gradle.kts`, e il tag del rilascio la
                 * conferma invece di ripeterla. Una stringa scritta a mano qui sarebbe il
                 * secondo posto in cui scriverla, e il primo a mentire.
                 * ⚠️ **La `v` minuscola non si traduce**, ed è voluto: è la forma che ha chiesto
                 * lui (*come `v1.54`*), la stessa che si legge sulla paginetta di download, e in
                 * ventotto lingue una parola tradotta accanto a un numero sarebbe una riga da
                 * mantenere per non dire niente di più.
                 * ⚠️ **Discreto vuol dire questi tre pezzi insieme**: il corpo più piccolo che
                 * il tema abbia, il monospazio, e l'inchiostro tenue. Il monospazio serve a una
                 * cosa precisa: le cifre hanno tutte la stessa larghezza, quindi il numero non
                 * cambia ingombro passando dalla `1.9` alla `1.10`.
                 */
                Text(
                    text = "v" + BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = VERSION_FADE)
                )
            }
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
private fun PageRow(
    label: String,
    summary: String,
    onOpen: () -> Unit,
    /**
     * Le parole delle righe che vivono **dentro** la pagina, per la ricerca.
     *
     * ⚠️ Serve alle pagine che sono ELENCHI con comandi riga per riga, che non si possono
     * appiattire nella radice: là la copertura è questa, cioè la riga che apre la pagina
     * risponde anche alle parole di dentro. Vuoto per le altre, che si appiattiscono.
     */
    extra: List<String> = emptyList()
) {
    if (!shown(label, summary, *extra.toTypedArray())) return
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
 * Una sotto-pagina fatta di RIGHE: la riga che la apre, oppure il suo corpo mentre si cerca.
 *
 * ⚠️⚠️ **È LA COPERTURA DELLA RICERCA PER LE SOTTO-PAGINE, e senza di lei una voce spostata
 * dietro un tocco ESCE dalla ricerca**: `LocalQuery` è fornito nel solo ramo della radice, le
 * sotto-pagine non lo ricevono, e là con la ricerca vuota [shown] risponde di sì a tutto.
 * Mandare una famiglia in sotto-pagina senza questo peggiorerebbe l'app.
 * ⚠️ **Funziona perché il corpo si compone DENTRO il provider della radice**: le righe si
 * filtrano già da sé, quindi non serve nessun elenco di voci da tenere aggiornato. L'unico
 * pezzo che non si filtra da sé è un blocco scritto a mano, e quello si avvolge in
 * [Searchable].
 * ⚠️ **Sopra il corpo appiattito non si stampa il titolo della pagina**, per coerenza con la
 * scelta già presa in [Group]: mentre si cerca i titoli non compaiono, perché i risultati
 * vengono da posti diversi e mescolati.
 * ⚠️ **Non vale per le pagine che sono ELENCHI**: là il corpo non si appiattisce, e la
 * copertura è il parametro `extra` di [PageRow].
 */
@Composable
private fun PageOfRows(
    label: String,
    summary: String,
    onOpen: () -> Unit,
    body: @Composable () -> Unit
) {
    if (LocalQuery.current.isBlank()) PageRow(label = label, summary = summary, onOpen = onOpen)
    else body()
}

/**
 * L'avviso sulle miniature memorizzate e il pulsante che le butta.
 *
 * ⚠️⚠️ **UN COMPONENTE SOLO PER I DUE POSTI IN CUI COMPARE**: la sotto-pagina e, mentre si
 * cerca, la pagina piatta che la appiattisce ([PageOfRows]). Scriverlo due volte vorrebbe dire
 * un avviso che si aggiorna in un posto e non nell'altro, che è il difetto che la copertura
 * della ricerca doveva togliere.
 * ⚠️ [head] è il titolo, e c'è **solo** nella pagina piatta: nella sotto-pagina lo dice già la
 * testata, e ripeterlo direbbe la stessa cosa due volte a mezzo centimetro di distanza.
 * ⚠️ **Il testo dell'avviso è dell'utente**, con una parola cambiata: dove lui aveva scritto
 * 'foto grandi' qui c'è 'immagini grandi', perché questa app apre anche tavole, scansioni e
 * schermate, e la regola di non chiamarle fotografie è sua (`AIV/CLAUDE.md`, § '🗣️ Come si
 * chiamano le cose').
 * ⚠️ **Il tasto avvisa con un `Toast`**: quello che è successo si vede in un'altra schermata,
 * e un comando che non dà segno di aver fatto qualcosa si preme due volte. È la stessa scelta,
 * e la stessa ragione, di 'Ripristina gli avvisi'.
 */
@Composable
private fun ThumbsCard(head: String?, onClear: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val done = stringResource(R.string.settings_thumbs_done)
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        // ⚠️ `CenterVertically` dentro `spacedBy` è quello che mette il blocco in mezzo allo
        // spazio avanzato: senza spazio avanzato non fa niente, che è esattamente quello che
        // serve nella pagina piatta.
        verticalArrangement = Arrangement.spacedBy(THUMBS_GAP, Alignment.CenterVertically)
    ) {
        if (head != null) Text(text = head, style = MaterialTheme.typography.titleSmall)
        Text(
            text = stringResource(R.string.settings_thumbs_warn),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = {
            onClear()
            Toast.makeText(context, done, Toast.LENGTH_SHORT).show()
        }) {
            Text(
                text = stringResource(R.string.settings_thumbs_do),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** L'aria fra l'avviso e il pulsante che lo esegue: abbastanza perché non si tocchino. */
private val THUMBS_GAP = 28.dp

/**
 * Quanto è tenue il numero di versione accanto al titolo.
 *
 * ⚠️ **'Un po' in trasparenza' è una sua parola, e questo è il numero che le dà un valore**: la
 * riga deve leggersi da chi la cerca e sparire per chi non la cerca. Parte dall'inchiostro già
 * attenuato delle spiegazioni, non da quello del titolo, quindi il velo è il secondo di due.
 */
private const val VERSION_FADE = 0.55f

/**
 * Che cosa sta fra due voci nel riepilogo di una sotto-pagina.
 *
 * ⚠️ È la stessa virgola con cui l'app già incolla gli elenchi che si leggono (`FileOps`
 * quando dice che cosa è stato fatto, e i due contatori sotto una copertina): un separatore
 * nuovo per questo posto sarebbe una seconda convenzione per lo stesso mestiere.
 */
private const val SUMMARY_JOIN = ", "

/**
 * La sotto-pagina 'Opzioni di visualizzazione': come si presentano i tre elenchi di casa.
 *
 * ⚠️⚠️ **LE SETTE VOCI SONO LE STESSE del dialogo del tocco lungo nella schermata iniziale, e
 * questa è la loro CASA**: là compaiono nude e solo quelle della vista scelta, perché un
 * dialogo con tutte sarebbe un secondo pannello; qui hanno il titolino della vista e stanno
 * tutte insieme, perché una pagina di impostazioni si scorre.
 * ⚠️ **Una preferenza, una chiave, un valore di fabbrica**: le due superfici scrivono la
 * stessa cosa e passano dallo stesso salvataggio, com'è già per le colonne (*che resta globale
 * per tutte le cartelle*). Un valore 'della sessione' sarebbe una terza cosa da capire.
 * ⚠️ **Chi tocca una di queste voci tocca DUE posti**: la riga è scritta due volte con due
 * componenti, e un cambiamento di forma va fatto in entrambi o divergono. È il costo
 * dichiarato di avere casa e scorciatoia.
 * ⚠️ **L'ordine è quello delle tre viste**, non quello in cui le voci sono nate: chi arriva
 * qui sta guardando una vista, e la trova dove la vista sta nel dialogo.
 */
@Composable
private fun ViewOptionsPage(settings: Settings, onChange: (Settings) -> Unit) {
    Group(stringResource(R.string.view_grid))

    // ⚠️ Le colonne restano anche nella scorciatoia del tocco lungo sul tastino, e non è un
    // doppione: la scorciatoia scrive questa stessa impostazione.
    Choices(
        label = stringResource(R.string.settings_folder_columns),
        detail = stringResource(R.string.settings_folder_columns_desc),
        options = FOLDER_COLUMNS.map { Columns(it) },
        selected = Columns(settings.folderColumns),
        nameOf = { it.n.toString() },
        onSelect = { onChange(settings.copy(folderColumns = it.n)) }
    )

    // ⚠️ Attaccata alle colonne: parlano della **stessa** griglia, una di quante colonne ha e
    // l'altra di che cosa si legge sotto le copertine.
    SwitchRow(
        label = stringResource(R.string.settings_folder_count),
        detail = stringResource(R.string.settings_folder_count_desc),
        checked = settings.folderCount,
        onChange = { onChange(settings.copy(folderCount = it)) }
    )

    // ⚠️ Subito dopo, e non altrove: quella dice che cosa si legge sotto una **cartella**,
    // questa che cosa si legge sotto un'**immagine**. Sono la stessa domanda in due griglie,
    // e separarle vorrebbe dire cercarle in due posti.
    SwitchRow(
        label = stringResource(R.string.settings_grid_names),
        detail = stringResource(R.string.settings_grid_names_desc),
        checked = settings.gridNames,
        onChange = { onChange(settings.copy(gridNames = it)) }
    )

    Group(stringResource(R.string.view_list))

    SwitchRow(
        label = stringResource(R.string.list_count),
        detail = null,
        checked = settings.listCount,
        onChange = { onChange(settings.copy(listCount = it)) }
    )

    /*
     * ⚠️⚠️ **QUI SONO PASTIGLIE E NELLA SCORCIATOIA È UNO SLIDER, e non è un'incoerenza**: là
     * il gesto dice che si sta girando una manopola su una scala, e lo spazio è quello di un
     * dialogo; qui la pagina si scorre e le tre pastiglie stanno in riga come tutte le altre
     * scelte del pannello, dove uno slider a tre fermi sarebbe l'unico oggetto di quel genere.
     * La preferenza è la stessa e la scala pure: cambia il vestito, non la scelta.
     */
    Choices(
        label = stringResource(R.string.text_size),
        detail = null,
        options = TextSize.entries,
        selected = settings.listText,
        nameOf = { stringResource(it.label()) },
        onSelect = { onChange(settings.copy(listText = it)) }
    )

    // ⚠️ 'Cartelle di sistema' e non 'Cartelle': il secondo è il nome corto della vista nel
    // dialogo, e come titolino collide col titolo della sezione da cui si arriva qui.
    Group(stringResource(R.string.hub_view_tree))

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

/**
 * La sotto-pagina 'Adattamento e zoom': le voci che decidono come si **misura** un'immagine.
 *
 * ⚠️ **Sono quelle che stavano in cima al gruppo del visualizzatore, nello stesso ordine**:
 * chi le conosceva le ritrova dove le aveva lasciate, un gradino più in là.
 * ⚠️ **Fino alla `1.46` qui c'era scritto 'le tre voci'**, e la `1.26` lo aveva reso falso
 * portando qui anche lo zoom nel menu. Il criterio che lo vieta sta in
 * `rules/Roccobot.md` § '🔢 I conti si contano, non si scrivono'.
 */
@Composable
private fun ZoomAndFit(settings: Settings, onChange: (Settings) -> Unit) {
    SwitchRow(
        label = stringResource(R.string.settings_fit_grow),
        detail = stringResource(R.string.settings_fit_grow_desc),
        checked = settings.fitGrow,
        onChange = { onChange(settings.copy(fitGrow = it)) }
    )

    // ⚠️ Avvolto in `Searchable`, e senza di lui è l'unico pezzo di questa pagina che non si
    // filtra da sé: un `Text` più uno `Slider` nudi resterebbero in scena mentendo, quando la
    // pagina si appiattisce nella radice durante una ricerca. Il testo da confrontare è quello
    // che la riga già mostra, quindi non costa una stringa.
    val zoomMaxLabel = stringResource(R.string.settings_zoom_max)
    Searchable(zoomMaxLabel) {
        Text(
            text = zoomMaxLabel + "   " + settings.zoomMax.roundToInt() + "x",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp)
        )
        Slider(
            value = settings.zoomMax,
            onValueChange = { onChange(settings.copy(zoomMax = it.roundToInt().toFloat())) },
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
     * tocco fa già, e stavano in mezzo a comandi che agiscono sul file.
     * ⚠️⚠️ **LA DESCRIZIONE NON NOMINA PIÙ IL DOPPIO TOCCO, dalla `1.46`** (istruzione
     * dell'utente, 2026-09-03), e la nota di prima diceva che quel rimando serviva *perché una
     * riga che dice solo 'rimetti due voci' non direbbe da che cosa si viene*. Adesso la riga
     * dice che cosa **fa** e basta, cioè aggiungere due voci al menu contestuale: il perché
     * sia spenta è una faccenda di chi scrive il codice e non di chi legge un'impostazione, e
     * vive qui.
     */
    SwitchRow(
        label = stringResource(R.string.settings_zoom_menu),
        detail = stringResource(R.string.settings_zoom_menu_desc),
        checked = settings.zoomInMenu,
        onChange = { onChange(settings.copy(zoomInMenu = it)) }
    )
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
 *
 * ⚠️⚠️ **QUI VIVEVA ANCHE LA VARIANTE A PASTIGLIE IMPILATE, e la `1.46` l'ha cancellata perché
 * NON LA CHIAMAVA NESSUNO**: era nata nella `1.36` per far stare tre gettoni in ventotto
 * lingue (*tutti della stessa larghezza, sufficiente per la parola più lunga nella lingua che
 * esige più spazio*), e la `1.38` l'ha superata riportando la posizione della barra info alla
 * forma della `1.25`, cioè a [InfoSideRow]. Il ragionamento che portava resta là dove serve
 * ancora: la larghezza non è un numero in dp ma la massima intrinseca della colonna, perché
 * un numero sarebbe giusto in una lingua e sbagliato nelle altre.
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

/**
 * Una voce con l'interruttore.
 *
 * ⚠️⚠️ **LA RIGA INTERA È IL COMANDO, dalla `1.46`, e prima non lo era affatto**: si toccava
 * il solo interruttore, che è un bersaglio da 32dp in una pagina fatta di righe alte il
 * doppio. Il tocco è un `toggleable` con `role = Role.Switch`, e dentro l'interruttore non
 * c'è niente: due bersagli per una scelta sola farebbero annunciare due voci a un lettore di
 * schermo, che è il difetto che il dialogo delle opzioni aveva.
 * ⚠️ **Lo mette il componente e non il chiamante**, così una voce nuova ce l'ha per
 * costruzione: nessuno si ricorda di aggiungere un modificatore a una riga che sta già bene.
 * ⚠️ Il `toggleable` va **prima** del `padding`, per la ragione già scritta su [PageRow]: così
 * il bersaglio prende anche il margine e arriva ai 48dp senza scriverli.
 */
@Composable
private fun SwitchRow(
    label: String,
    detail: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    if (!shown(label, detail)) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(top = 12.dp),
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


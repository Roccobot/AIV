package io.github.roccobot.aiv

import android.net.Uri
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Le cartelle di immagini del telefono: **la schermata iniziale dell'app** (decisione
 * dell'utente: chi apre un visualizzatore vuole vedere immagini, e le altre vie sono dietro il
 * FAB).
 *
 * Serve a due cose: aprire una cartella adesso, e scegliere quella da aprire all'avvio. È la
 * stessa domanda, quindi la stessa schermata. Nella seconda veste il FAB non c'è e il gesto
 * Indietro sì: si risponde a una domanda.
 *
 * Le viste sono modi diversi di guardare la stessa cosa: `FolderView`.
 */
@Composable
fun FolderScreen(
    view: FolderView,
    /**
     * Quante colonne mostrano le copertine. Vedi `FOLDER_COLUMNS` in `Settings.kt`.
     *
     * Decide solo la larghezza delle copertine: quante righe si vedono è una conseguenza,
     * perché l'intestazione prende [HEADER_SHARE] dello schermo e la griglia il resto.
     */
    columns: Int,
    /**
     * Se sotto la copertina si vede il conto delle immagini. Vedi `Settings.folderCount`.
     * Spento, la riga si accorcia e ne entrano di più: l'intestazione non si adatta.
     */
    counted: Boolean,
    /**
     * Dove si vede il colore di una cartella, fuori dalla sua intestazione. Vedi [FolderColour].
     */
    colour: FolderColour,
    /**
     * Il colore scelto per ogni cartella che ne ha uno. Vedi `ViewerViewModel.folderTints`.
     *
     * La mappa arriva intera: le voci sono poche e vivono in una chiave sola dell'archivio,
     * le celle sono decine (`FolderTints.all`).
     */
    tints: Map<Long, Int>,
    /**
     * La copertina scelta a mano per ogni cartella che ne ha una. Vedi `FolderCovers`.
     * Arriva come la mappa delle tinte: chi non compare tiene la copertina predefinita,
     * l'immagine più recente.
     */
    covers: Map<Long, Uri> = emptyMap(),
    /** I percorsi da non mostrare. Vedi `Settings.hiddenFolders`. */
    hidden: Set<String>,
    selection: FolderSelection = FolderSelection(hidden = hidden),
    onSelectionChange: (FolderSelection) -> Unit = {},
    onHide: (Folder.Bucket) -> Unit,
    /**
     * Se le nascoste sono in scena adesso, per il minuto di 'Mostra nascoste'. Vedi
     * `ViewerViewModel.peeking`.
     *
     * Le cartelle in più si riconoscono: al 70% di inchiostro e col segno del vuoto in un
     * angolo (specifica dell'utente).
     */
    peeking: Boolean = false,
    /** Accende e spegne la vista temporanea. */
    onPeek: (Boolean) -> Unit = {},
    /**
     * Rimostra per sempre, cioè toglie queste voci da `Settings.hiddenFolders`.
     *
     * ⚠️ **Le voci arrivano tutte insieme**: una cartella dentro una nascosta è coperta
     * dall'antenata, e rimostrarla vuol dire togliere anche quella voce. Due chiamate partirebbero
     * dallo stesso elenco, e la seconda rimetterebbe quello che la prima ha tolto.
     */
    onUnhide: (Collection<String>) -> Unit = {},
    recents: List<RecentImage>,
    onPick: (Folder.Bucket) -> Unit,
    onOpen: (Uri) -> Unit,
    /** Vedi l'omonimo di [Hub]: le immagini di una pagina, dal dialogo dell'indirizzo. */
    onOpenPage: (Folder.Series) -> Unit,
    onView: (FolderView) -> Unit,
    onForget: () -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    onBin: () -> Unit,
    /**
     * Il Feed, e `null` finché la prima lettura non è finita. Vedi `ViewerViewModel.feed`.
     *
     * Nel Menu 'Start' il Feed ha preso la casella dell'interruttore fra tondo e pillola (scelta
     * dell'utente, 6.02), che è nelle impostazioni, sotto l'elemento principale.
     */
    feed: Feed.Page?,
    /** Quale data ordina il Feed. Vedi [FeedDate]. */
    feedDate: FeedDate,
    onFeedDate: (FeedDate) -> Unit,
    /**
     * Chiede al modello di leggere il Feed, dicendo se il permesso c'è: legge solo quando è
     * cambiato qualcosa da cui il flusso dipende (`ViewerViewModel.readFeed`).
     */
    onFeedRead: (Boolean) -> Unit,
    onFeedLeft: () -> Unit = {},
    /** Un'immagine toccata nel Feed: il flusso e la posizione. */
    onFeedOpen: (List<Uri>, Int) -> Unit,
    /**
     * Un'operazione della selezione del Feed è finita: il flusso si rilegge comunque
     * (`ViewerViewModel.rereadFeed`), perché rinominare o spostare non cambia nessuna sua chiave.
     */
    onFeedChanged: () -> Unit,
    /** Se nel Feed c'è una selezione aperta: `ViewerViewModel.feedBusy`. */
    onFeedBusy: (Boolean) -> Unit = {},
    /**
     * Cambia il numero di colonne: lo scrive la scorciatoia del tocco lungo.
     *
     * È la stessa impostazione delle preferenze: il numero resta globale per tutte le cartelle,
     * come chiesto.
     */
    onColumns: (Int) -> Unit,
    /**
     * Le opzioni delle altre due viste, dal popup del tocco lungo. Vedi `Settings`.
     *
     * Senza valore predefinito apposta: con uno, un chiamante che si dimenticasse di passarle
     * mostrerebbe opzioni scollegate dalle preferenze, e il compilatore tacerebbe.
     */
    listCount: Boolean,
    listText: TextSize,
    treeHidden: Boolean,
    treePictures: Boolean,
    onListCount: (Boolean) -> Unit,
    onListText: (TextSize) -> Unit,
    onTreeHidden: (Boolean) -> Unit,
    onTreePictures: (Boolean) -> Unit,
    /**
     * Dov'è la vista 'Cartelle di sistema', e `null` vuol dire in cima.
     *
     * Vive nel modello: da questa schermata si esce a ogni immagine aperta, e un `remember`
     * locale riporterebbe in cima a ogni ritorno.
     */
    treePath: String?,
    /** Se eliminare vuol dire mandare nel cestino: serve alle azioni della vista ad albero. */
    binOn: Boolean,
    /** I campi delle informazioni, per la voce 'Info' della vista ad albero. */
    factFields: List<FactField>,
    onTreePath: (String?) -> Unit,
    /** Un'immagine toccata nella vista delle cartelle di sistema: la serie e la posizione. */
    onTreeOpen: (List<Uri>, Int) -> Unit,
    /**
     * Se questa è la veste **'scegli la cartella d'avvio'** invece della casa: il dato del
     * chiamante (`Screen.Folders.forStart`), senza dedurlo da altri parametri.
     */
    forStart: Boolean,
    /** Torna indietro dalla scelta della cartella d'avvio; vale solo con [forStart] acceso. */
    onBack: () -> Unit = {},
    /**
     * Le cartelle del telefono, non filtrate, e `null` finché la prima lettura non è finita.
     * Vedi `ViewerViewModel.buckets`.
     *
     * Arriva dal modello: da qui si esce a ogni immagine aperta, e un elenco tenuto qui
     * costerebbe a ogni ritorno una query sul MediaStore, con la rotella al posto delle copertine.
     */
    buckets: List<Folder.Bucket>?,
    /**
     * Chiede al modello di rileggere, dicendogli se il permesso c'è: il permesso lo sa questa
     * schermata, che lo rinfresca al ritorno dalla pagina di sistema.
     */
    onRead: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var granted by remember { mutableStateOf(Folder.granted(context)) }

    /**
     * Se in AIV Play la persona ha concesso solo alcune immagini (da Android 14, [Store]).
     *
     * Si rilegge a ogni ritorno nell'app, con [granted]: il permesso cambia anche dalle
     * impostazioni di sistema, e Android dice di non tenerlo da parte. In AIV GitHub è sempre
     * `false`.
     */
    var partial by remember { mutableStateOf(Store.access(context) == Store.Access.PARTIAL) }
    val reread = {
        granted = Folder.granted(context)
        partial = Store.access(context) == Store.Access.PARTIAL
    }
    LifecycleResumeEffect(Unit) {
        reread()
        onPauseOrDispose { }
    }

    /**
     * La cartella che si sta per nascondere, e `null` quando non se ne sta nascondendo
     * nessuna.
     *
     * ⚠️ **Il tocco lungo chiede, non fa**: un tocco lungo per sbaglio farebbe sparire una
     * cartella senza dire dov'è finita, e sembrerebbe che l'app abbia perso delle immagini.
     */
    var hiding by remember { mutableStateOf<Folder.Bucket?>(null) }

    /** Se il pannello delle cartelle nascoste è aperto: lo apre il tocco lungo sulla voce. */
    var listing by remember { mutableStateOf(false) }

    /** Se si sta scegliendo la dimensione della griglia col tocco lungo sul FAB. */
    var sizing by remember { mutableStateOf(false) }
    /** Se 'Cartelle preferite' è aperta sopra la casa, dalla riga in fondo alle opzioni. */
    var favouring by remember { mutableStateOf(false) }
    /** Selezione multipla in Cartelle di sistema: nasconde il FAB sotto [PickSheet]. */
    var treeBusy by remember { mutableStateOf(false) }

    /**
     * Le immagini scelte nel Feed (richiesta dell'utente, 6.03). Salvabile, come quella della
     * griglia: sopravvive alla rotazione.
     * Contano solo gli indirizzi ancora nel flusso: dopo un'operazione, o una lettura che ha perso
     * un'immagine, le altre restano scelte (effetto qui sotto).
     */
    var feedChosen by rememberSaveable(stateSaver = UriSetSaver) { mutableStateOf(emptySet()) }
    val feedPicking = !forStart && view == FolderView.FEED && feedChosen.isNotEmpty()
    /** L'operazione aperta dalla scheda del Feed, come `job` nella griglia. */
    var feedTask by rememberSaveable(stateSaver = FileJobSaver) { mutableStateOf<FileJob?>(null) }
    /** L'altezza della scheda del Feed, per lo spazio sotto l'ultima riga. */
    var feedSheet by remember { mutableIntStateOf(0) }
    LaunchedEffect(feed) {
        val there = feed?.items?.toSet() ?: return@LaunchedEffect
        if (!there.containsAll(feedChosen)) feedChosen = feedChosen intersect there
    }
    // Uscire dal Feed lascia la selezione, come uscire da una cartella.
    LaunchedEffect(view) { if (view != FolderView.FEED) feedChosen = emptySet() }
    BackHandler(enabled = feedPicking) { feedChosen = emptySet() }
    // ⚠️ Con una selezione aperta il modello rimanda le riletture del Feed da fuori (rilievo D15),
    // come per la griglia di una cartella: una pagina nuova è una chiave nuova del gesto, e il
    // trascinamento sotto il dito finirebbe. La lettura si paga a selezione chiusa.
    LaunchedEffect(feedPicking) { onFeedBusy(feedPicking) }
    DisposableEffect(Unit) { onDispose { onFeedBusy(false) } }

    /**
     * Se si sta scrivendo il nome di una sottocartella nuova.
     *
     * Lo stato è qui e non nel menu, come `sizing`: il dialogo si cerca nella schermata.
     */
    var making by remember { mutableStateOf(false) }

    /**
     * Se il velo che insegna la scorciatoia delle colonne si è già visto.
     *
     * Parte da 'già visto', come nella griglia: il valore vero arriva dall'archivio dopo la
     * prima composizione, e da `false` il velo comparirebbe per un fotogramma a chi l'ha chiuso.
     */
    val columnsSeen by produceState(initialValue = true, context) {
        Hint.COLUMNS.flow(context).collect { value = it }
    }

    /**
     * La bandierina locale, perché l'archivio risponde con un giro di ritardo: il velo
     * resterebbe steso un fotogramma o due, col dialogo che si apre sotto di lui.
     */
    var columnsOff by remember { mutableStateOf(false) }

    /** Se il velo del Feed si è già visto: parte da 'già visto' come [columnsSeen]. */
    val feedSeen by produceState(initialValue = true, context) {
        Hint.FEED.flow(context).collect { value = it }
    }

    /** La bandierina locale del velo del Feed, come [columnsOff]. */
    var feedOff by remember { mutableStateOf(false) }

    // L'esito si rilegge dallo stato delle cose, come nel visualizzatore: [Store] sceglie la
    // strada della variante; qui il tocco è della persona, quindi la pagina non si spiega.
    val askAccess = Store.rememberAccessRequest(explain = false) { reread() }
    val allowAll = Store.rememberAppPage { reread() }

    // Il permesso può essere appena arrivato: il modello decide da sé se c'è davvero
    // qualcosa da rileggere.
    LaunchedEffect(granted) { onRead(granted) }
    // ⚠️ Il Feed si legge solo quando è sullo schermo: una casa in griglia non paga una query su
    // tutte le immagini del telefono. Cartelle, data e 'Mostra nascoste' sono chiavi perché
    // cambiano il flusso. Uscendo lo si dice (rilievo D16), e il modello smette di rileggerlo a
    // ogni notizia dal disco.
    LaunchedEffect(granted, view, forStart, selection, feedDate, peeking) {
        if (view == FolderView.FEED && !forStart) onFeedRead(granted) else onFeedLeft()
    }

    // Il filtro è qui, non in `Folder.buckets`, che resta un elenco puro: che cosa l'utente non
    // vuole guardare è una faccenda della schermata. È un filtro e non una chiave di rilettura:
    // nascondere una cartella non cambia il disco, e si vede al fotogramma dopo senza una query.
    // Con 'Mostra nascoste' acceso il filtro le lascia passare; quali siano nascoste lo dice
    // comunque [hidden], per il segno del vuoto e l'inchiostro ridotto.
    val folders = buckets?.filter { selection.visible(it.path, peeking) }

    // Da qui in giù la casa ha l'intestazione e il FAB, la scelta della cartella d'avvio la
    // freccia Indietro.
    val home = !forStart

    /**
     * Quali cartelle sono in scena in prestito: le nascoste, solo finché dura 'Mostra nascoste'.
     * Si ricava da due dati che ci sono già.
     */
    val prestate = if (peeking && selection.mode == FolderMode.EXCLUDED)
        buckets.orEmpty().filter { it.path?.let(selection::hidden) == true }
            .mapNotNull { it.path?.let(::portablePath) }.toSet() else emptySet()

    /*
     * Il rientro di sotto non è qui ma nel `contentPadding` delle viste (richiesta dell'utente:
     * la griglia fino al bordo inferiore): sul contenitore toglierebbe lo spazio prima che la
     * griglia disegni, mentre così le cartelle scorrono sotto la barra gestuale e l'ultima riga
     * resta raggiungibile. La sfumatura in fondo arriva al vetro, e tiene la barra gestuale su un
     * fondo neutro.
     * ⚠️ **`steadyDrawing` e non `safeDrawing`** (rilievo D13), come nella griglia di una
     * cartella: tornando dal visualizzatore le barre ricompaiono animate, e `safeDrawing` farebbe
     * scendere a scatti la fascia e il Feed. Il perché è su [steadyDrawing].
     */
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(
                steadyDrawing().only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
            )
    ) {
        val density = LocalDensity.current

        val nameStyle = folderNameStyle(columns)

        /*
         * ⚠️ **L'intestazione è una frazione fissa dello schermo** (istruzione dell'utente): è
         * uno spazio deliberato, che tiene le cartelle in basso a portata di pollice, non quello
         * che avanza dopo un numero esatto di righe. Quante righe si vedono è una conseguenza, e
         * a volte l'ultima è mezza: è il segno che sotto c'è dell'altro.
         */
        val headerMax = if (home) maxHeight * HEADER_SHARE else 0.dp
        val headerPx = with(density) { headerMax.toPx() }

        /**
         * Quanta parte dell'intestazione è già chiusa, da 0 (tutta aperta) a 1.
         *
         * ⚠️ **Vive dove vive lo scorrimento**, in `rememberSaveable` (il `SaveableStateHolder` di
         * `AivApp`): la fascia è funzione della posizione di scorrimento, e al ritorno nella casa
         * le due tornano insieme per costruzione (voce `scorri-torna`: l'intestazione solo in cima
         * alla lista). La griglia di una cartella ha lo stesso invariante come regola, perché là
         * il salto all'immagine da cui si torna scorre la lista senza lo scorrimento annidato.
         * Una frazione e non pixel: la rotazione cambia l'altezza della fascia.
         */
        var shutFrac by rememberSaveable { mutableFloatStateOf(0f) }

        /**
         * Quanti pixel di fascia sono chiusi adesso: la frazione riportata sull'altezza di oggi.
         *
         * Una lambda e non un valore: si legge in misura e disegno, dove un valore nuovo costa un
         * ridisegno e non una ricomposizione (come `shut` di [FrontBand]).
         */
        val shut = { shutFrac * headerPx }

        /*
         * Gli scorrimenti sono qui perché li muovono i tasti del salto; sopravvivono alla
         * schermata grazie al `SaveableStateHolder` di `AivApp`. Uno per vista: la posizione in
         * una non vuol dire niente nelle altre.
         */
        val coverScroll = rememberLazyGridState()
        val rowScroll = rememberLazyListState()
        val feedScroll = rememberLazyGridState()
        /** La griglia sullo schermo, copertine o Feed, e `null` nei due elenchi. */
        val gridScroll: LazyGridState? = when (view) {
            FolderView.GRID -> coverScroll
            FolderView.FEED -> feedScroll
            else -> null
        }

        /**
         * L'intestazione si chiude prima che l'elenco scorra, quindi funziona anche con due
         * cartelle: il perché è su [frontScroll].
         */
        val paging = remember(headerPx, view) {
            frontScroll(
                quanto = headerPx,
                chiuso = shut,
                // I pixel dello scorrimento tornano frazione qui, il solo posto in cui si scrive;
                // a fascia zero (la scelta della cartella d'avvio) non si divide.
                chiudi = { quanti -> shutFrac = if (headerPx > 0f) quanti / headerPx else 0f },
                // In cima alla griglia o all'elenco (voce `3.13-02`): con poche cartelle il
                // trascinamento verso il basso deve riaprire l'intestazione.
                inCima = {
                    if (gridScroll != null)
                        gridScroll.firstVisibleItemIndex == 0 &&
                            gridScroll.firstVisibleItemScrollOffset == 0
                    else
                        rowScroll.firstVisibleItemIndex == 0 &&
                            rowScroll.firstVisibleItemScrollOffset == 0
                }
            )
        }

        /*
         * Il glifo del FAB diventa la freccia scorrendo (il meccanismo è in `Jump.kt`): lo stato
         * è qui perché lo leggono il contenitore, che gli passa il gesto, e [Hub], che disegna il
         * FAB.
         * ⚠️ **Nella vista ad albero il gesto non si monta**: l'elenco è di [TreeList], che non
         * espone lo scorrimento, e il salto muoverebbe una lista fuori scena.
         */
        val scorre: ScrollableState = gridScroll ?: rowScroll
        val arm = rememberJumpArm(
            state = scorre,
            up = { shut() + (gridScroll?.jumpUpPixels() ?: rowScroll.jumpUpPixels()) },
            down = { gridScroll?.jumpDownPixels() ?: rowScroll.jumpDownPixels() }
        )
        /*
         * Quello che la pillola di vetro sfoca: la colonna intera, sorella della pillola e non
         * madre (il perché è su [Backdrop]). Fuori dal vetro non registra niente.
         */
        val backdrop = rememberBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .backdropSource(backdrop)
                /*
                 * ⚠️ **Il gesto si guarda prima di `paging`** (ordine misurato): il `nestedScroll`
                 * scritto per primo riceve per primo il delta, e `frontScroll` ne consuma la parte
                 * che chiude l'intestazione. Dopo, al motore del glifo arriverebbe zero.
                 */
                .then(
                    if (view == FolderView.TREE) Modifier else Modifier.nestedScroll(arm.watch)
                )
                .nestedScroll(paging)
                // Il margine laterale permette due colonne di copertine su uno schermo da 360dp:
                // il conto è in `FOLDER_CELL`.
                .padding(SCREEN_PAD)
        ) {
            if (home) {
                // L'icona si stringe quando l'intestazione è bassa (in orizzontale, e con quattro
                // colonne, dove restano poco più di 180dp): se no sarebbe tagliata sopra e sotto.
                Header(headerPx, minOf(HEADER_ICON, headerMax * 0.5f), shut)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back)
                        )
                    }
                    Text(
                        text = stringResource(R.string.folders_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.heading()
                    )
                }
            }
            Spacer(Modifier.height(HEADER_GAP))
            if (home && partial) PartialAccess(onAllowAll = allowAll)

            when {
                !granted -> AccessPlea(onAsk = askAccess, modifier = Modifier.padding(top = 24.dp))

                // Prima dei rami che guardano `folders`: la vista delle cartelle di sistema legge
                // il disco, non il MediaStore, e non aspetta l'elenco.
                // ⚠️ **`home &&`**: la cartella d'avvio dev'essere una cartella del MediaStore,
                // perché l'avvio ne apre la griglia; nella scelta si finisce sull'elenco in fondo.
                view == FolderView.TREE && home ->
                    TreeList(
                        path = treePath,
                        hidden = hidden,
                        selection = selection,
                        onSelectionChange = onSelectionChange,
                        binOn = binOn,
                        showHidden = treeHidden,
                        onlyPictures = treePictures,
                        factFields = factFields,
                        onPath = onTreePath,
                        onOpen = onTreeOpen,
                        onBusy = { treeBusy = it }
                    )

                /*
                 * Il Feed (richiesta dell'utente, 6.02): prima dei rami delle cartelle, come
                 * l'albero, perché legge il suo flusso e non le aspetta. Come l'albero esiste solo
                 * nella casa: la scelta della cartella d'avvio vuole una cartella.
                 */
                view == FolderView.FEED && home -> when {
                    feed == null -> CircularProgressIndicator(
                        Modifier.padding(top = 24.dp).size(28.dp).align(Alignment.CenterHorizontally)
                    )
                    feed.items.isEmpty() -> Text(
                        text = stringResource(R.string.feed_none),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                    // Lo stesso vano che riempie l'altezza della griglia: con poche immagini un
                    // trascinamento sul vuoto deve riaprire l'intestazione.
                    else -> Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        FeedGrid(
                            page = feed,
                            columns = columns,
                            state = feedScroll,
                            chosen = feedChosen,
                            onChosen = { feedChosen = it },
                            sheetRoom = with(LocalDensity.current) { feedSheet.toDp() },
                            modifier = Modifier.fillMaxSize()
                        ) { at -> onFeedOpen(feed.items, at) }
                    }
                }

                folders == null -> CircularProgressIndicator(
                    Modifier.padding(top = 24.dp).size(28.dp).align(Alignment.CenterHorizontally)
                )

                folders!!.isEmpty() -> Text(
                    text = stringResource(R.string.folders_none),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 24.dp)
                )

                /*
                 * ⚠️ **Il vano scorrevole riempie l'altezza** (voce `3.13-02`): con poche cartelle
                 * (modalità incluse) griglia ed elenco resterebbero alti quanto il contenuto, e il
                 * trascinamento sul vuoto non riaprirebbe l'intestazione. Come in `GridScreen`.
                 */
                view == FolderView.GRID ->
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        // La griglia segue l'ordine scelto e l'elenco no (richiesta dell'utente);
                        // i preferiti vanno primi in tutti e due.
                        Covers(
                            selection.arrange(folders!!, grid = true), columns, prestate, counted,
                            nameStyle, colour, tints, covers,
                            onPick, coverScroll, modifier = Modifier.fillMaxSize(), off = emptySet()
                        ) { hiding = it }
                    }
                else ->
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Rows(
                            selection.arrange(folders!!, grid = false), prestate, listCount, listText,
                            colour, tints, covers, onPick,
                            rowScroll, modifier = Modifier.fillMaxSize(), off = emptySet()
                        ) {
                            hiding = it
                        }
                    }
            }
        }

        // Il FAB manca nella scelta della cartella d'avvio, a cui si arriva dalle impostazioni, e
        // durante una selezione in Cartelle di sistema o nel Feed, dove la scheda prende il suo
        // posto.
        if (home && !treeBusy && !feedPicking) {
            /*
             * La sfumatura in fondo, verso il colore di fondo, dalla quota del FAB (richiesta
             * dell'utente): suggerisce che la griglia scorre e inghiotte quello che c'è sotto. A
             * riposo non copre niente, perché [BELOW_FAB] tiene l'ultima cartella sopra di lei. È
             * in `Front.kt`, una decisione sola per l'app; qui si vede sempre, perché il FAB c'è
             * sempre.
             * ⚠️ **È prima del FAB**: in un `Box` l'ultimo figlio si disegna sopra.
             * Non prende i tocchi: ha solo un modificatore di disegno, e Compose fa la prova del
             * tocco solo sui nodi di puntatore.
             */
            GroundFade(modifier = Modifier.align(Alignment.BottomCenter))
            Hub(
                view = view,
                /*
                 * Il salto passa dallo scorrimento annidato, come un dito: così arrivando in cima
                 * riapre l'intestazione. Nella vista ad albero non si arma mai.
                 */
                arm = arm,
                nested = paging,
                columns = columns,
                granted = granted,
                recents = recents,
                onOpen = onOpen,
                onOpenPage = onOpenPage,
                onView = onView,
                onForget = onForget,
                onSettings = onSettings,
                onSearch = onSearch,
                onBin = onBin,
                hiddenCount = if (selection.mode == FolderMode.EXCLUDED) selection.excluded.size else 0,
                peeking = peeking,
                onPeek = onPeek,
                onPeekList = { listing = true },
                onSize = { sizing = true },
                backdrop = backdrop,
                /*
                 * Le condizioni del menu: la vista ad albero, dentro una cartella. Alla radice
                 * `treePath` è nullo, e l'elenco è fatto di volumi.
                 */
                onNewFolder = if (view == FolderView.TREE && treePath != null) {
                    { making = true }
                } else {
                    null
                },
                // Il Menu basso si posa in fondo a tutta larghezza, e tiene l'angolo per la sola
                // pillola in cui si piega. Il rientro di sistema lo mette [pillCorner], perché il
                // contenitore lo lascia al `contentPadding` della griglia.
                modifier = Modifier.align(
                    if (LocalPillLook.current.bar) Alignment.BottomCenter else fabSide()
                ),
                // Pillole e tondo a [PILL_SIDE] dal vetro (scelta A2), e il FAB col centro sul
                // centro del tondo.
                corner = Modifier.pillCorner(
                    if (LocalPillLook.current.mode != PhonePill.OFF) PILL_KEY else FAB_SIZE
                )
            )
        }

        /*
         * Il velo che insegna la scorciatoia delle colonne ([HintVeil], come i due della
         * griglia), solo quando c'è una griglia da dimensionare: nella casa, col permesso, con
         * delle cartelle, nella vista griglia. Al primo avvio insegnare a dimensionare una
         * griglia vuota sarebbe rumore sopra la richiesta del permesso.
         * Il tocco sulla copia non apre il menu: si insegna il tocco lungo.
         * Con la pillola estesa il velo non compare: non c'è il tocco lungo da insegnare
         * (risposta B1); con la pillola a scomparsa il tondo lo tiene, e la copia è tonda.
         */
        val pill = LocalPillLook.current.mode
        val hint = home && granted && view == FolderView.GRID &&
            !folders.isNullOrEmpty() && !columnsSeen && !columnsOff && pill != PhonePill.EXTENDED

        /** Il velo si archivia appena l'utente fa la cosa che insegnava, o appena la salta. */
        val hintDone: () -> Unit = {
            columnsOff = true
            scope.launch { Hint.COLUMNS.remember(context) }
        }

        if (hint) {
            /*
             * Col Menu 'Start' il velo lo mostra aperto (richiesta dell'utente, 4.31): le caselle
             * disegnate, e nell'angolo la copia del tondo, che risponde. La frase dice le due cose
             * che il tasto fa.
             */
            val start = LocalPillLook.current.corner
            HintVeil(
                text = stringResource(if (start) R.string.corner_hint else R.string.columns_hint),
                // L'angolo del comando vero, tondo o FAB ([pillCorner]); il perché è su [HintVeil].
                inset = Modifier.pillCorner(if (pill != PhonePill.OFF) PILL_KEY else FAB_SIZE),
                onDone = hintDone
            ) {
                val tasto = @Composable {
                    TapHoldFab(
                        label = stringResource(R.string.hub_open),
                        // Sul Menu 'Start' del velo la copia è gialla (voce `4.33-05`): l'arancione
                        // del pannello la nasconderebbe ([HINT_KEY]).
                        container = if (start) HINT_KEY else HINT_MARK,
                        ink = if (start) HINT_KEY_INK else HINT_INK,
                        holdLabel = stringResource(R.string.columns_title),
                        onTap = hintDone,
                        onHold = { hintDone(); sizing = true },
                        round = pill == PhonePill.SLIDE,
                        // Lo stesso glifo del FAB vero: la copia è il suo ritratto.
                        glyph = { Marchio(it) }
                    )
                }
                if (start) {
                    // Le stesse caselle del menu vero, da [hubEntries]: qui sono un disegno, e i
                    // comandi che chiedono uno stato di [Hub] restano vuoti.
                    CornerMenuCopy(
                        entries = hubEntries(
                            view = view,
                            granted = granted,
                            hiddenCount = if (selection.mode == FolderMode.EXCLUDED) selection.excluded.size else 0,
                            peeking = peeking,
                            onPeek = onPeek,
                            onPeekList = null,
                            onView = onView,
                            onSearch = onSearch,
                            onAddress = {},
                            onPickImage = {},
                            onNewFolder = null,
                            onBin = onBin,
                            onSettings = onSettings,
                            steady = true
                        ),
                        atEnd = LocalPadLook.current.hand == Hand.RIGHT,
                        key = tasto
                    )
                } else {
                    tasto()
                }
            }
        }

        // La scheda della selezione del Feed, al posto del tasto principale, nascosto mentre
        // qualcosa è scelto, come nella griglia di una cartella.
        if (home && view == FolderView.FEED) {
            PickSheet(
                visible = feedPicking,
                actions = feedPickActions(
                    chosen = feedChosen,
                    items = feed?.items.orEmpty(),
                    binOn = binOn,
                    onChosen = { feedChosen = it },
                    onJob = { feedTask = it }
                ),
                onHeight = { feedSheet = it }
            )
        }

        /*
         * Il velo del Feed, la prima volta che il flusso è sullo schermo (richiesta dell'utente:
         * spiegare la scelta della data). Solo con immagini da mostrare: su un flusso vuoto o
         * senza permesso la frase non spiegherebbe niente.
         */
        if (home && granted && view == FolderView.FEED && !feed?.items.isNullOrEmpty() &&
            !feedSeen && !feedOff
        ) {
            // Il suo schizzo del flusso, col testo di 'Disposizione in vista feed' (nota su
            // `6.02-04`; il perché è su [HintFeed]).
            HintFeed(
                text = stringResource(R.string.feed_order_desc),
                newer = stringResource(R.string.hint_feed_newer),
                older = stringResource(R.string.hint_feed_older),
                onDone = {
                    feedOff = true
                    scope.launch { Hint.FEED.remember(context) }
                }
            )
        }
    }

    // Le operazioni del Feed: la selezione si svuota prima del lavoro, come nella griglia, e il
    // flusso si rilegge dopo.
    FileJobDialogs(job = feedTask, fields = factFields, onClose = { feedTask = null }) { kind, work ->
        feedChosen = emptySet()
        // In [FileWork], come ogni lavoro sui file: il perché è là.
        FileWork.scope.launch {
            val out = work()
            if (kind.speaks(out)) Notices.say(outcomeText(context.resources, out, kind), NOTICE_LONG_MS)
            onFeedChanged()
        }
    }

    /*
     * La finestra è quella del selettore di destinazione ([NewFolderDialog], in
     * `DestinationDialog.kt`), non una copia.
     * Creata la cartella ci si entra: l'albero si rilegge quando cambia il percorso, quindi la
     * cartella nuova si vede, e chi la crea vuole metterci qualcosa.
     * ⚠️ **Se la creazione fallisce si resta dove si è, e lo si dice** (rilievo D10): una cartella
     * che non esiste mostrerebbe un elenco vuoto che sembra un guasto. Un nome già preso porta
     * nella cartella che c'è ([FileTree.makeFolder]).
     */
    if (making) {
        NewFolderDialog(
            onDismiss = { making = false },
            onCreate = { name ->
                making = false
                val dove = treePath
                if (dove != null) {
                    scope.launch {
                        val nata = FileTree.makeFolder(java.io.File(dove), name)
                        if (nata != null) onTreePath(nata.absolutePath)
                        else Notices.say(context.getString(R.string.folder_create_fail), NOTICE_LONG_MS)
                    }
                }
            }
        )
    }

    if (sizing) {
        ViewOptions(
            view = view,
            columns = columns,
            order = selection.order,
            listCount = listCount,
            listText = listText,
            treeHidden = treeHidden,
            treePictures = treePictures,
            feedDate = feedDate,
            onView = onView,
            onColumns = onColumns,
            onOrder = { onSelectionChange(selection.copy(order = it)) },
            onFeedDate = onFeedDate,
            onListCount = onListCount,
            onListText = onListText,
            onTreeHidden = onTreeHidden,
            onTreePictures = onTreePictures,
            onFavourites = {
                sizing = false
                favouring = true
            },
            onDismiss = { sizing = false }
        )
    }
    if (favouring) {
        FavouritesSheet(
            list = selection.favourites,
            onList = { onSelectionChange(selection.copy(favourites = it)) },
            onDismiss = { favouring = false }
        )
    }

    /*
     * Il pannello è la pagina 'Cartelle nascoste' delle impostazioni (richiesta dell'utente),
     * con le stesse stringhe per titolo e comando.
     * Lo scorrimento non si aggiunge: [Sheet] scorre già, e due scorrimenti verticali annidati
     * nello stesso verso fanno andare Compose in errore.
     */
    if (listing) {
        Sheet(title = stringResource(R.string.settings_hidden), onDismiss = { listing = false }) {
            selection.excluded.sorted().forEach { voce ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Il nome davanti e il percorso sotto, come nelle impostazioni: due
                        // cartelle possono chiamarsi uguale. I testi li scrivono `hiddenName` e
                        // `hiddenShown`, col percorso dalla radice del volume.
                        val nome = hiddenName(voce)
                        val percorso = hiddenShown(voce)
                        Text(text = nome, style = MaterialTheme.typography.bodyLarge)
                        // La radice di un volume ha nome e percorso uguali: una riga basta.
                        if (percorso != nome) {
                            Text(
                                text = percorso,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(onClick = {
                        onUnhide(listOf(voce))
                        // L'ultima riga che esce chiude il pannello, che resterebbe vuoto.
                        if (selection.excluded.size <= 1) listing = false
                    }) { Text(stringResource(R.string.settings_hidden_show)) }
                }
            }
        }
    }

    /*
     * L'ordine della griglia si chiede qui, una volta (risposta `C1`: anche a chi aggiorna):
     * la casa è dove c'è la griglia. Aspetta le cartelle, così non copre la richiesta del
     * permesso, e un rifiuto vale per la sola sessione.
     * Sullo schermo largo la casa è la colonna delle cartelle, un elenco che non segue l'ordine:
     * la domanda aspetta il telefono in verticale o le impostazioni.
     */
    var orderAsked by rememberSaveable { mutableStateOf(false) }
    if (home && selection.order == null && !orderAsked && !folders.isNullOrEmpty()) {
        FolderOrderDialog(
            onPick = { onSelectionChange(selection.copy(order = it)) },
            onDismiss = { orderAsked = true }
        )
    }

    hiding?.let { bucket ->
        FolderHoldDialog(
            bucket = bucket,
            selection = selection,
            onHide = onHide,
            onUnhide = onUnhide,
            onSelectionChange = onSelectionChange,
            onDismiss = { hiding = null },
        )
    }
}

/**
 * Il corpo del nome di una cartella, che dipende da quante colonne ci sono.
 *
 * A quattro colonne scende (richiesta dell'utente): la cella è larga 78dp su 360, e a
 * `titleSmall` due parole entrano a stento. `labelMedium` tiene il peso medio, che distingue il
 * nome dal conto sotto.
 */
@Composable
internal fun folderNameStyle(columns: Int): TextStyle =
    if (columns >= NARROW_COLUMNS) MaterialTheme.typography.labelMedium
    else MaterialTheme.typography.titleSmall

/**
 * L'intestazione dell'app, che si chiude scorrendo.
 *
 * ⚠️ **È spazio deliberato, perché ogni cartella sia raggiungibile col pollice** a una mano
 * (richiesta dell'utente); scorrendo la vista arriva al 100%. L'altezza la decide [HEADER_SHARE].
 *
 * Il contenuto si misura all'altezza piena e si ritaglia, centrato in quello che resta: così
 * l'icona sale mentre lo spazio si stringe, senza deformarsi ([FrontBand]).
 * L'opacità va col quadrato della frazione aperta: a metà corsa è già al 25%, e l'ultimo tratto è
 * spazio vuoto che si richiude.
 * Lo stato si legge in misura e disegno: il trascinamento non fa ricomporre la griglia.
 */
@Composable
private fun Header(fullPx: Float, icon: Dp, shut: () -> Float) {
    FrontBand(fullPx = fullPx, shut = shut) { aperto ->
        Identity(
            iconSize = icon,
            modifier = Modifier.graphicsLayer { alpha = aperto() * aperto() }
        )
    }
}

/** Come si chiama una vista nel menu. Vedi la nota dentro [Hub]. */
@StringRes
private fun FolderView.label(): Int = when (this) {
    FolderView.GRID -> R.string.hub_view_grid
    FolderView.LIST -> R.string.hub_view_list
    FolderView.TREE -> R.string.hub_view_tree
    // Il nome dell'utente, dappertutto: come 'Cartelle di sistema', è un nome e non una descrizione.
    FolderView.FEED -> R.string.view_feed
}

/** Il nome breve di una vista, sotto la sua icona nel Menu 'Start' (nota F del giro della 4.32). */
private fun FolderView.short(): Int = when (this) {
    FolderView.GRID -> R.string.start_grid
    FolderView.LIST -> R.string.start_list
    FolderView.TREE -> R.string.start_tree
    FolderView.FEED -> R.string.view_feed
}

/**
 * Il glifo di una vista nel menu.
 *
 * Griglia ed elenco li ha disegnati l'utente; 'Cartelle di sistema' usa la cartella di Material,
 * la stessa che l'app usa dappertutto.
 */
private val FolderView.glyph: ImageVector
    @Composable get() = when (this) {
        FolderView.GRID -> Glyphs.ViewGrid
        FolderView.LIST -> Glyphs.ViewList
        FolderView.TREE -> Icons.Default.Folder
        // La pila di immagini, scelta A5 dell'utente.
        FolderView.FEED -> Glyphs.Feed
    }

/**
 * Il nome corto della vista, per le pastiglie del dialogo delle opzioni.
 *
 * Due nomi per la stessa cosa, voluti (richiesta dell'utente): nel menu del FAB la voce è accanto
 * ad 'Apri un indirizzo' e 'Impostazioni' e dice di che cosa parla; nel dialogo il titolo è già
 * 'Opzioni di visualizzazione', e ripeterlo allungherebbe ogni pastiglia.
 */
private fun FolderView.shortLabel(): Int = when (this) {
    FolderView.GRID -> R.string.view_grid
    FolderView.LIST -> R.string.view_list
    FolderView.TREE -> R.string.view_tree
    FolderView.FEED -> R.string.view_feed
}

/**
 * Il FAB in basso a destra, e tutto quello che c'è dietro.
 *
 * ⚠️ **È il menu dell'app**: impostazioni, indirizzo, selettore di sistema e scelta della vista
 * non hanno un altro posto. In basso a destra perché il pollice ci arriva senza cambiare presa;
 * piccolo per non competere con le immagini (richiesta dell'utente).
 *
 * ⚠️ **Il selettore di sistema compare solo se il permesso manca** (decisione dell'utente): è la
 * sola via per aprire un'immagine senza il permesso, e col permesso sarebbe una seconda strada
 * per quello che le cartelle già fanno.
 */
@Composable
private fun Hub(
    view: FolderView,
    /**
     * Lo stato del glifo del FAB, che scorrendo diventa la freccia del salto. Lo riempie il gesto
     * raccolto dal contenitore; il meccanismo è in `Jump.kt`.
     */
    arm: JumpArm,
    /**
     * Lo scorrimento annidato della schermata, che la corsa attraversa come un dito: senza, il
     * salto arriverebbe in cima con l'intestazione chiusa, e l'utente la vuole aperta.
     */
    nested: NestedScrollConnection,
    /**
     * Quante colonne ha la griglia sotto: serve alla larghezza del menu, non al FAB. È la
     * scelta, lo stesso valore della griglia; le colonne vere le ricava `spread`, e la misura è
     * su `menuFloor`.
     */
    columns: Int,
    /**
     * Se l'app ha il permesso sui file. Arriva da chi chiama, che lo aggiorna al ritorno dalla
     * pagina di sistema: letto qui, niente lo farebbe ricomporre.
     */
    granted: Boolean,
    recents: List<RecentImage>,
    onOpen: (Uri) -> Unit,
    /**
     * Apre le immagini di una pagina, quando l'indirizzo digitato è una pagina e non
     * un'immagine. Un parametro, perché la schermata non conosce il modello.
     */
    onOpenPage: (Folder.Series) -> Unit,
    onView: (FolderView) -> Unit,
    onForget: () -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    onBin: () -> Unit,
    /**
     * Quante cartelle sono nascoste: la voce 'Mostra nascoste' c'è solo se ce n'è una. Un conto
     * basta; l'elenco lo mostra il pannello della schermata.
     */
    hiddenCount: Int,
    /** Se le nascoste sono in scena adesso: decide il testo e il glifo della voce. */
    peeking: Boolean,
    /** Il tocco sulla voce: accende il minuto, o lo spegne subito. */
    onPeek: (Boolean) -> Unit,
    /** Il tocco lungo sulla voce: apre il pannello delle nascoste. */
    onPeekList: () -> Unit,
    /**
     * Il tocco lungo: la scorciatoia della dimensione della griglia. Il dialogo lo apre chi
     * chiama, come il velo: sono cose della schermata.
     */
    onSize: () -> Unit,
    /**
     * Crea una sottocartella nella cartella in cui si è, e `null` fuori da una cartella: alla
     * radice dell'albero e nelle altre viste. Lo decide chi chiama, che sa dove si è.
     */
    onNewFolder: (() -> Unit)? = null,
    /** Quello che la pillola di vetro sfoca, o `null` fuori dal vetro. Vedi [Backdrop]. */
    backdrop: Backdrop? = null,
    /**
     * I rientri e il margine dell'angolo del FAB, dentro [modifier].
     *
     * Separati: il Menu basso arriva ai bordi dello schermo, e l'angolo resta alla sola pillola in
     * cui si piega. Vedi `corner` in [PhonePillBar].
     */
    corner: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    val menu = rememberMenuState()
    var asking by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { picked -> picked?.let(onOpen) }
    val voci = hubEntries(
        view = view,
        granted = granted,
        hiddenCount = hiddenCount,
        peeking = peeking,
        onPeek = onPeek,
        onPeekList = onPeekList,
        onView = onView,
        onSearch = onSearch,
        onAddress = { asking = true },
        onPickImage = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onNewFolder = onNewFolder,
        onBin = onBin,
        onSettings = onSettings
    )
    val angolo = if (LocalPillLook.current.corner) hubEntries(
        view = view,
        granted = granted,
        hiddenCount = hiddenCount,
        peeking = peeking,
        onPeek = onPeek,
        onPeekList = onPeekList,
        onView = onView,
        onSearch = onSearch,
        onAddress = { asking = true },
        onPickImage = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onNewFolder = onNewFolder,
        onBin = onBin,
        onSettings = onSettings,
        steady = true
    ) else null

    /*
     * Al posto del FAB può esserci la pillola (richiesta dell'utente): le stesse voci, nello
     * stesso ordine, da [hubEntries]. Col tondo restano il salto e il tocco lungo delle colonne
     * (risposta C1); con la pillola estesa il salto diventa i due tasti in fondo, e il tocco
     * lungo non c'è (risposte C2 e B1).
     */
    if (LocalPillLook.current.mode != PhonePill.OFF) {
        PhonePillBar(
            entries = voci,
            arm = arm,
            nested = nested,
            fabLabel = jumpLabel(arm, stringResource(R.string.hub_open)),
            holdLabel = stringResource(R.string.columns_title),
            onHold = onSize,
            backdrop = backdrop,
            modifier = modifier,
            corner = corner,
            mark = { Marchio(it) },
            restLabel = stringResource(R.string.hub_open),
            cornerEntries = angolo,
            fabGlyph = { JumpGlyph(arm) { Marchio(it) } }
        )
    } else Box(modifier = modifier.then(corner)) {
        /*
         * ⚠️ **Il menu è scritto prima del FAB, e l'ordine conta**: il FAB si stacca in una
         * finestra sua per restare sopra il velo (`lifted` in [TapHoldFab]), e fra finestre dello
         * stesso tipo comanda l'ordine della composizione. Dopo, il menu coprirebbe il FAB.
         * L'ancoraggio è un punto nell'angolo di questo riquadro, qualunque sia l'ordine.
         * Il menu passa dalla superficie unica dei menu ([MenuShell]), che dà raggio, velo e
         * animazione uguali a tutti. Le icone sono in parte di Material e in parte nostre: quelle
         * di Material rendono a zero pixel di scarto dal glifo approvato (nota in testa a
         * `Glyphs.kt`).
         */
        MenuShell(
            state = menu,
            position = rememberMenuAtAnchor(),
            /*
             * La larghezza la detta la griglia sotto (voce `menu-bordo`): più di due celle e del
             * loro spazio, senza cadere a filo coi bordi delle colonne. Il conto è su `menuFloor`;
             * con due colonne di fabbrica il menu si ferma a `MENU_INSET` dai lati.
             */
            minWidth = menuFloor(spread(columns, LocalWindowInfo.current), FOLDER_GAP)
        ) {
            // Le voci le scrive [hubEntries], una volta sola, e le legge anche la pillola dello
            // schermo largo (`HomePill`). Qui il menu le dispone, col filetto fra i gruppi.
            voci.forEachIndexed { i, voce ->
                if (i > 0 && voce.group != voci[i - 1].group) HorizontalDivider()
                MenuRow(
                    text = voce.label,
                    icon = voce.icon,
                    onTap = { menu.close(); voce.onTap() },
                    onHold = voce.onHold?.let { tieni -> { menu.close(); tieni() } }
                )
            }
        }
        /*
         * [TapHoldFab] e non `SmallFloatingActionButton`, che prende un `onClick` solo e non
         * vedrebbe il tocco lungo. Una `Surface` da 40dp, quadrata con gli angoli appena smussati
         * come chiesto: il tondo pieno direbbe 'azione principale', e qui lo sono le cartelle.
         * Senza animazione d'ingresso (riscontro dell'utente sul giro della 1.74); la pressione
         * invece resta, in [TapHoldFab], approvata nello stesso riscontro.
         */
        val altroTema = aivLauncher(!LocalAivLight.current)
        /*
         * A tasto armato il tocco fa il salto e non apre il menu: il comando vive sul FAB, e il
         * tratto armato è quello in cui si vede la freccia, che sparisce un secondo dopo l'ultimo
         * scorrimento. L'etichetta segue il comando, per chi usa un lettore di schermo.
         */
        val salto = rememberCoroutineScope()
        TapHoldFab(
            label = jumpLabel(arm, stringResource(R.string.hub_open)),
            /*
             * ⚠️ **I colori sono quelli dell'icona dell'app** (richiesta dell'utente), presi dalle
             * sue risorse (`launcher_background`, `launcher_foreground`) con [aivLauncher], che
             * segue il tema scelto dentro l'app: `colorResource` leggerebbe quello di sistema
             * (`Rules.md` § '🌗 Il tema scelto DENTRO l'app non è quello di sistema').
             * La coppia è quella dell'altro tema (richiesta dell'utente), e il colore di questo
             * tema torna alla pressione: li scambia [TapHoldFab].
             * Il contrasto è quello dell'icona (2,42 chiaro, 3,25 scuro; il perché in
             * `colors.xml`): colori scelti dall'utente, per un glifo e non per del testo.
             */
            container = altroTema.first,
            ink = altroTema.second,
            holdLabel = stringResource(R.string.columns_title),
            // `visible` e non `wanted`: il FAB resta staccato per tutta l'uscita del menu, o
            // rientrerebbe nella finestra dell'app sotto il velo che se ne va.
            lifted = menu.visible,
            pressed = menu.wanted,
            backdrop = backdrop,
            onTap = { if (arm.armed) salto.launch { arm.leap(nested) } else menu.open() },
            onHold = onSize,
            glyph = { JumpGlyph(arm) { Marchio(it) } }
        )
    }

    if (asking) {
        AddressDialog(
            recents = recents,
            onOpen = onOpen,
            onOpenPage = onOpenPage,
            onForget = onForget,
            onDismiss = { asking = false }
        )
    }
}

/**
 * Le voci del FAB della schermata iniziale, in ordine e coi loro gruppi.
 *
 * Un elenco solo, che leggono il menu ([Hub]), le pillole e, sullo schermo largo, la pillola
 * d'accento ([HomePill]; scelta B5: tutte le voci del FAB di casa). Il gruppo dice dove il menu
 * mette il filetto: viste, azioni, impostazioni.
 */
@Composable
private fun hubEntries(
    view: FolderView,
    granted: Boolean,
    hiddenCount: Int,
    peeking: Boolean,
    onPeek: (Boolean) -> Unit,
    onPeekList: (() -> Unit)?,
    onView: (FolderView) -> Unit,
    onSearch: () -> Unit,
    onAddress: () -> Unit,
    onPickImage: () -> Unit,
    onNewFolder: (() -> Unit)?,
    onBin: () -> Unit,
    onSettings: () -> Unit,
    /**
     * Le voci del Menu 'Start' invece di quelle del FAB (decisione C2): 'Mostra nascoste' sempre
     * al suo posto, spenta quando non ce n'è una e sostituita da 'Seleziona immagine' senza il
     * permesso, così ogni icona resta nella sua casella.
     * La prima riga sono le altre viste, col Feed per primo nella casella che era
     * dell'interruttore fra tondo e pillola (scelta dell'utente, 6.02). In AIV Play non c'è la
     * vista ad albero.
     * Con 'Nuova cartella' il Feed resta fuori (nota E del giro della 4.32): le caselle sarebbero
     * dieci, e la decima aprirebbe una quarta riga. Dentro l'albero il Feed è nel menu principale.
     */
    steady: Boolean = false
): List<PillEntry> = buildList {
    // ⚠️ **La voce nomina la vista che si ottiene, non quella in cui si è**: una riga di menu è
    // una richiesta, non un indicatore di stato, quindi in griglia si legge 'Visualizzazione
    // lista'. Il menu mostra le viste diverse da quella corrente. Le etichette si cambiano
    // insieme e nella stessa forma.
    // 'Cartelle di sistema' e 'Feed' non seguono la forma delle altre: sono nomi che l'utente ha
    // dato alle viste, non descrizioni.
    Store.views.filter { it != view && !(steady && it == FolderView.FEED && onNewFolder != null) }
        .sortedBy { !(steady && it == FolderView.FEED) }
        .forEach { other ->
        add(PillEntry(other.glyph, stringResource(other.label()), group = 0, short = stringResource(other.short())) { onView(other) })
    }

    /*
     * 'Mostra nascoste' è nel gruppo delle viste: risponde a 'quali cartelle guardo', l'altro
     * lato di 'come le guardo'. Nel menu del FAB c'è solo se ce n'è una nascosta (senza, il
     * tocco lungo aprirebbe un pannello vuoto); nel Menu 'Start' è spenta.
     * La riga nomina quello che il tocco fa, come le viste: con le nascoste in scena dice
     * 'Nascondi cartelle'. I due glifi sono dell'utente, una cartella con un occhio.
     */
    if (steady && !granted) {
        add(PillEntry(Icons.Default.Image, stringResource(R.string.hub_pick), group = 0, short = stringResource(R.string.start_pick)) { onPickImage() })
    } else if (hiddenCount > 0 || steady) {
        add(
            PillEntry(
                icon = if (peeking) Glyphs.FolderEyeOff else Glyphs.FolderEye,
                label = stringResource(
                    if (peeking) R.string.hub_unpeek else R.string.hub_peek
                ),
                group = 0,
                enabled = hiddenCount > 0,
                short = stringResource(if (peeking) R.string.start_hide else R.string.start_show),
                onHold = onPeekList,
                holdLabel = onPeekList?.let { stringResource(R.string.settings_hidden) },
                onTap = { onPeek(!peeking) }
            )
        )
    }

    // Cerca è in cima alle azioni: è la domanda più frequente di chi apre questo menu senza
    // sapere già dove andare.
    add(PillEntry(Icons.Default.Search, stringResource(R.string.hub_search), group = 1, short = stringResource(R.string.hub_search)) { onSearch() })
    val indirizzo = PillEntry(Icons.Default.Public, stringResource(R.string.hub_url), group = 1, short = stringResource(R.string.start_url)) { onAddress() }
    val impostazioni = PillEntry(Icons.Default.Settings, stringResource(R.string.hub_settings), group = 2, short = stringResource(R.string.hub_settings)) { onSettings() }
    if (!steady) add(indirizzo)
    /*
     * Solo se il permesso manca (decisione dell'utente; il perché è su [Hub]). Sparisce da sé
     * quando il permesso arriva: `granted` si rinfresca al ritorno dalla richiesta (`askAccess`
     * in chi chiama), e il menu si ricompone.
     */
    if (!granted && !steady) {
        add(PillEntry(Icons.Default.Image, stringResource(R.string.hub_pick), group = 1) { onPickImage() })
    }
    // ⚠️ **Il cestino si raggiunge solo da qui**: le sue immagini sono nella cartella dell'app,
    // dove il MediaStore non guarda, quindi non compaiono fra le cartelle. È in fondo alle azioni:
    // un posto dove si va, ma il meno frequentato.
    /*
     * 'Nuova cartella' c'è solo dentro una cartella della vista ad albero, sopra 'Cestino'
     * (riscontro del giro della 1.81, punto B). Alla radice l'elenco è fatto di volumi, e le altre
     * viste elencano le cartelle che il MediaStore conosce, un indice: una cartella creata là non
     * comparirebbe finché non contiene un'immagine.
     */
    if (onNewFolder != null) {
        add(PillEntry(Glyphs.FolderNew, stringResource(R.string.dest_new), group = 1, short = stringResource(R.string.start_new)) { onNewFolder() })
    }
    val cestino = PillEntry(Glyphs.Bin, stringResource(R.string.bin_title), group = 1, short = stringResource(R.string.bin_title)) { onBin() }
    /*
     * Nel Menu 'Start' 'Impostazioni' viene prima di 'Apri URL' e del cestino (nota dell'utente
     * sul giro della 5.02): con la × in fondo, 'Impostazioni' è sopra la ×, e in basso ci sono le
     * due voci meno usate. Lettura dichiarata: con 'Crea', nell'albero, vale la stessa regola. Il
     * FAB, la pillola e il Menu basso tengono l'altro ordine.
     */
    // AIV Play ha il cestino di sistema solo da Android 11 (`Store.hasBin`).
    if (steady) {
        add(impostazioni)
        add(indirizzo)
        if (Store.hasBin) add(cestino)
    } else {
        if (Store.hasBin) add(cestino)
        add(impostazioni)
    }
}

/**
 * Quanto spazio in più del FAB chiede sotto l'ultima cartella il comando in vigore: la pillola
 * verticale del Menu basso a scomparsa è alta due tasti. Vedi [controlRoom].
 */
@Composable
private fun extraRoom(): Dp = (controlRoom() - PILL_KEY - PILL_AIR * 2).coerceAtLeast(0.dp)

/**
 * La pillola della schermata iniziale sullo schermo largo: le voci del FAB di casa, con le due
 * finestre che alcune aprono (l'indirizzo e il selettore di sistema).
 *
 * Le finestre vivono qui: sullo schermo largo la schermata delle cartelle non è in scena, quindi
 * chi apre la finestra deve anche tenerla.
 * Il tocco lungo su 'Mostra nascoste' qui non c'è: il pannello delle nascoste è della schermata
 * delle cartelle, e la stessa pagina è nelle impostazioni.
 */
@Composable
fun HomePill(
    view: FolderView,
    granted: Boolean,
    hiddenCount: Int,
    peeking: Boolean,
    onPeek: (Boolean) -> Unit,
    recents: List<RecentImage>,
    onOpen: (Uri) -> Unit,
    onOpenPage: (Folder.Series) -> Unit,
    onForget: () -> Unit,
    onView: (FolderView) -> Unit,
    onSearch: () -> Unit,
    onBin: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var asking by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { picked -> picked?.let(onOpen) }
    ActionPill(
        entries = hubEntries(
            view = view,
            granted = granted,
            hiddenCount = hiddenCount,
            peeking = peeking,
            onPeek = onPeek,
            onPeekList = null,
            onView = onView,
            onSearch = onSearch,
            onAddress = { asking = true },
            onPickImage = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onNewFolder = null,
            onBin = onBin,
            onSettings = onSettings
        ),
        vertical = true,
        modifier = modifier
    )
    if (asking) {
        AddressDialog(
            recents = recents,
            onOpen = onOpen,
            onOpenPage = onOpenPage,
            onForget = onForget,
            onDismiss = { asking = false }
        )
    }
}

/**
 * Le opzioni di visualizzazione, chieste col tocco lungo sul FAB.
 *
 * La prima fila sceglie la vista (richiesta dell'utente), e sotto compaiono le sue opzioni:
 * colonne e disposizione per la griglia, contatore e corpo del testo per l'elenco, file nascosti
 * e filtro delle immagini per le cartelle di sistema, colonne e data per il Feed. Griglia ed
 * elenco finiscono con la riga che apre 'Cartelle preferite' ([FavouritesSheet]). È il mockup
 * `homeLongPress.png` (nota B del giro della 5.80); il colore delle cartelle è solo nelle
 * impostazioni.
 *
 * ⚠️ **Sono le stesse impostazioni delle preferenze** (richiesta dell'utente: globali per tutte
 * le cartelle): una via più corta, e chi la usa le ritrova cambiate là. Le pastiglie sono quelle
 * delle impostazioni, `FilterChip` coi valori nudi, e ogni fila riempie la larghezza
 * ([JustifiedChips]).
 * ⚠️ **Scegliere non chiude**: la schermata dietro si rifà a ogni pastiglia toccata, e si possono
 * confrontare 2, 3 e 4 colonne senza riaprire niente. Il tasto in basso dice che si è finito.
 */
@Composable
internal fun ViewOptions(
    view: FolderView,
    columns: Int,
    order: FolderOrder?,
    listCount: Boolean,
    listText: TextSize,
    treeHidden: Boolean,
    treePictures: Boolean,
    feedDate: FeedDate,
    onView: (FolderView) -> Unit,
    onColumns: (Int) -> Unit,
    onOrder: (FolderOrder) -> Unit,
    onFeedDate: (FeedDate) -> Unit,
    onListCount: (Boolean) -> Unit,
    onListText: (TextSize) -> Unit,
    onTreeHidden: (Boolean) -> Unit,
    onTreePictures: (Boolean) -> Unit,
    onFavourites: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(onDismiss),
        properties = loweredWindow(onDismiss),
        title = { Text(stringResource(R.string.view_options)) },
        text = {
            // Il contenuto scorre: con l'ordine e la riga dei preferiti la griglia non entra in una
            // finestra bassa (telefono in orizzontale), e l'ultima riga sarebbe irraggiungibile.
            Column(
                verticalArrangement = Arrangement.spacedBy(OPTION_GAP),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                /*
                 * Una fila che va a capo: le viste sono quattro ('Vista a griglia', 'Vista a
                 * elenco', 'Cartelle di sistema' e 'Feed'; tre in AIV Play, senza l'albero), e su
                 * uno schermo stretto o in una lingua che le scrive lunghe uscirebbero dal dialogo.
                 */
                JustifiedChips {
                    Store.views.forEach { one ->
                        OptionChip(stringResource(one.shortLabel()), one == view) { onView(one) }
                    }
                }
                HorizontalDivider()
                // Le opzioni della sola vista scelta: con tutte insieme, quasi tutte spente, sarebbe
                // un pannello di impostazioni e non una scorciatoia.
                when (view) {
                    FolderView.GRID -> {
                        OptionLabel(R.string.columns_title)
                        JustifiedChips {
                            FOLDER_COLUMNS.forEach { n ->
                                OptionChip(n.toString(), n == columns) { onColumns(n) }
                            }
                        }
                        // Solo nella griglia, come nel mockup: l'elenco tiene l'ordine alfabetico,
                        // e la scelta là non cambierebbe niente.
                        OptionLabel(R.string.folder_order_short)
                        JustifiedChips {
                            FolderOrder.entries.forEach { one ->
                                OptionChip(stringResource(one.chip()), one == order) { onOrder(one) }
                            }
                        }
                        FavouritesRow(onFavourites)
                    }
                    FolderView.LIST -> {
                        OptionSwitch(R.string.list_count, listCount, onListCount)
                        OptionLabel(R.string.text_size)
                        /*
                         * Un cursore a tre fermi (richiesta dell'utente): le misure sono una
                         * scala, e tre pastiglie in fila andrebbero a capo.
                         * ⚠️ **`steps` conta gli scalini in mezzo**, non i fermi: per tre
                         * posizioni se ne dichiara uno.
                         */
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(OPTION_GAP)
                        ) {
                            ValueSlider(
                                value = listText.ordinal.toFloat(),
                                onValueChange = { at ->
                                    val step = at.roundToInt()
                                        .coerceIn(0, TextSize.entries.lastIndex)
                                    val want = TextSize.entries[step]
                                    if (want != listText) onListText(want)
                                },
                                valueRange = 0f..TextSize.entries.lastIndex.toFloat(),
                                steps = TextSize.entries.size - 2,
                                modifier = Modifier.weight(1f)
                            )
                            // La larghezza minima tiene fermo il cursore: 'Normale' e 'Piccolo'
                            // non misurano uguale, e il cursore cambierebbe lunghezza a ogni passo.
                            Text(
                                text = stringResource(listText.label()),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.widthIn(min = SIZE_NAME)
                            )
                        }
                        FavouritesRow(onFavourites)
                    }
                    // Senza la riga dei preferiti: l'albero mostra il disco com'è, e i preferiti
                    // non vi cambiano niente.
                    FolderView.TREE -> {
                        OptionSwitch(R.string.tree_show_hidden, treeHidden, onTreeHidden)
                        OptionSwitch(R.string.tree_pictures, treePictures, onTreePictures)
                    }
                    /*
                     * Il Feed: le colonne e il titolino della data coi due gettoni (specifica
                     * dell'utente). Le colonne sono quelle della griglia, un'impostazione sola per
                     * le due griglie. Niente preferiti né ordine: il Feed non ha cartelle.
                     */
                    FolderView.FEED -> {
                        OptionLabel(R.string.columns_title)
                        JustifiedChips {
                            FOLDER_COLUMNS.forEach { n ->
                                OptionChip(n.toString(), n == columns) { onColumns(n) }
                            }
                        }
                        OptionLabel(R.string.feed_date)
                        JustifiedChips {
                            FeedDate.entries.forEach { one ->
                                OptionChip(stringResource(one.chip()), one == feedDate) { onFeedDate(one) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pick_close)) }
        }
    )
}

/**
 * Una fila di pastiglie che riempie la larghezza del dialogo, e va a capo quando non entrano.
 *
 * Nota B dell'utente sul giro della 5.80: la prima pastiglia comincia sulla verticale dei
 * titolini, l'ultima finisce sul bordo destro. Ogni riga divide l'avanzo in parti uguali fra le
 * sue pastiglie, e lo stacco resta [OPTION_GAP].
 * ⚠️ **È anche quello che allinea la prima a sinistra**: una pastiglia più stretta dei 48dp di un
 * bersaglio riceve da Material un margine trasparente sui due lati, e il bordo di '2' cadrebbe a
 * destra del titolino. Misurata alla larghezza finale, sempre più larga, quel margine non c'è.
 * Presidiato da `VistaOpzioniTest`.
 */
@Composable
private fun JustifiedChips(content: @Composable () -> Unit) {
    Layout(content = content, modifier = Modifier.fillMaxWidth().oneOf()) { items, constraints ->
        val width = constraints.maxWidth
        val gap = OPTION_GAP.roundToPx()
        val natural = items.map { it.maxIntrinsicWidth(constraints.maxHeight).coerceAtMost(width) }
        val lines = mutableListOf<List<Int>>()
        var line = mutableListOf<Int>()
        var used = 0
        natural.forEachIndexed { at, w ->
            val need = if (line.isEmpty()) w else used + gap + w
            if (line.isNotEmpty() && need > width) {
                lines += line
                line = mutableListOf(at)
                used = w
            } else {
                line += at
                used = need
            }
        }
        if (line.isNotEmpty()) lines += line
        val placed = lines.map { members ->
            val taken = members.sumOf { natural[it] } + gap * (members.size - 1)
            val spare = (width - taken).coerceAtLeast(0)
            members.mapIndexed { k, at ->
                // I pixel del resto vanno all'ultima pastiglia, così la riga finisce sul bordo.
                val share = spare / members.size + if (k == members.lastIndex) spare % members.size else 0
                items[at].measure(Constraints.fixedWidth(natural[at] + share))
            }
        }
        val height = placed.sumOf { row -> row.maxOf { it.height } }
        layout(width, height) {
            var y = 0
            placed.forEach { row ->
                var x = 0
                row.forEach { chip ->
                    chip.placeRelative(x, y)
                    x += chip.width + gap
                }
                y += row.maxOf { it.height }
            }
        }
    }
}

/** Una pastiglia delle opzioni di visualizzazione, col nome centrato quando [JustifiedChips] la allarga. */
@Composable
private fun OptionChip(label: String, chosen: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = chosen,
        onClick = onClick,
        label = { Text(label, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        modifier = Modifier.picked(chosen)
    )
}

/** La riga che apre 'Cartelle preferite' sopra la casa, in fondo alle opzioni di visualizzazione. */
@Composable
private fun FavouritesRow(onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.favourite_folders),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Come si chiama uno stile del colore, nel pannello delle impostazioni.
 *
 * Il solo posto che dà un nome a ogni stile del colore.
 */
@StringRes
internal fun FolderColour.label(): Int = when (this) {
    FolderColour.NONE -> R.string.settings_colour_none
    FolderColour.EDGE -> R.string.settings_colour_edge
    FolderColour.FRAME -> R.string.settings_colour_frame
    FolderColour.NAME -> R.string.settings_colour_name
    FolderColour.GLOW -> R.string.settings_colour_glow
}

/** Il titolino sopra una fila di pastiglie, dentro il dialogo delle opzioni. */
@Composable
private fun OptionLabel(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Una voce con l'interruttore, dentro il dialogo delle opzioni.
 *
 * ⚠️ **Un bersaglio solo**: il tocco è un `toggleable` con `role = Role.Switch` su tutta la
 * riga, e l'interruttore non ha un `onCheckedChange` suo, o un lettore di schermo annuncerebbe
 * due voci per una scelta. È la stessa forma della riga nella pagina delle impostazioni, e la
 * riga intera è anche il bersaglio che un dito non manca.
 * ⚠️ **L'etichetta prende il peso**: senza, una riga lunga spinge il suo interruttore fuori
 * squadra rispetto a quello sopra; col peso il testo va a capo e l'interruttore resta
 * incolonnato, centrato sulle due righe.
 */
@Composable
private fun OptionSwitch(@StringRes text: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OPTION_GAP)
    ) {
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * I due contatori sotto una copertina: le immagini e, se ce ne sono, i filmati (scelta
 * dell'utente: un numero unico non dice se una cartella contiene video o immagini).
 *
 * Un contatore a zero non compare: accanto alla sua icona sarebbe rumore, in una riga larga
 * quanto una cella.
 * ⚠️ **`clearAndSetSemantics` e non `semantics`**: il tocco della scheda fonde le semantiche
 * dei figli, quindi una descrizione aggiunta lascerebbe anche i numeri nudi, e TalkBack
 * leggerebbe '1284 immagini 1284 12 video 12'. Qui le semantiche dei figli si tolgono e resta
 * la frase intera.
 */
@Composable
private fun Tally(bucket: Folder.Bucket) {
    val pictures = pluralStringResource(
        R.plurals.folders_count, bucket.pictures, bucket.pictures
    )
    val clips = pluralStringResource(R.plurals.folders_clips, bucket.clips, bucket.clips)
    val spoken = when {
        bucket.clips == 0 -> pictures
        bucket.pictures == 0 -> clips
        else -> "$pictures, $clips"
    }
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(COUNT_GAP)
    ) {
        if (bucket.pictures > 0 || bucket.clips == 0) {
            TallyPair(Icons.Outlined.Image, bucket.pictures)
        }
        if (bucket.clips > 0) TallyPair(Icons.Outlined.Movie, bucket.clips)
    }
}

/** Un'icona e il suo numero: metà di [Tally]. */
@Composable
private fun TallyPair(icon: ImageVector, howMany: Int) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(COUNT_ICON)
    )
    Text(
        text = howMany.toString(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
    )
}

/**
 * I due corpi di una riga d'elenco, per la misura scelta.
 *
 * ⚠️ **Sono stili del tema e non numeri in sp**: il tema li scala già col corpo di sistema,
 * quindi questa scelta si somma all'ingrandimento dei caratteri di Android. Con degli sp
 * scritti a mano, 'grande' risulterebbe più piccolo proprio per chi ha ingrandito i caratteri.
 */
@Composable
private fun TextSize.title() = when (this) {
    TextSize.SMALL -> MaterialTheme.typography.bodyMedium
    TextSize.NORMAL -> MaterialTheme.typography.titleSmall
    TextSize.LARGE -> MaterialTheme.typography.titleMedium
}

@Composable
private fun TextSize.detail() = when (this) {
    TextSize.SMALL -> MaterialTheme.typography.labelSmall
    TextSize.NORMAL -> MaterialTheme.typography.bodySmall
    TextSize.LARGE -> MaterialTheme.typography.bodyMedium
}

/** Le cartelle come copertine. */
@Composable
internal fun Covers(
    folders: List<Folder.Bucket>,
    columns: Int,
    /**
     * I percorsi delle cartelle in scena **in prestito**, cioè col minuto di 'Mostra nascoste'.
     *
     * Un insieme e non un booleano per cella: chi chiama ha già l'elenco delle nascoste, e la
     * selezione comune calcola quali bucket sono in prestito. Il confronto è esatto,
     * perché un'esclusione della sola cartella non segna i discendenti. Vuoto, nessuna è in
     * prestito.
     * ⚠️ **Senza valore di serie, come [covers]**: chi apre una vista di cartelle dichiara quali
     * sono in prestito, e il compilatore lo presidia. Con un valore di serie la finestra delle
     * destinazioni mostrava una cartella in prestito senza il suo segno.
     */
    peeked: Set<String>,
    counted: Boolean,
    /** Il corpo del nome, già scelto e già misurato da chi chiama. Vedi [FolderCard]. */
    nameStyle: TextStyle,
    /** Dove si vede il colore di una cartella. Vedi [FolderColour]. */
    colour: FolderColour,
    /** Il colore di ogni cartella che ne ha uno, per identificatore. */
    tints: Map<Long, Int>,
    /**
     * La copertina scelta a mano per ogni cartella che ne ha una. Vedi `FolderCovers`.
     *
     * ⚠️ **Senza valore di serie**: con `emptyMap()` di serie la finestra delle destinazioni lo
     * ereditava senza scriverlo, e `coverIn` ricadeva sempre sulla copertina predefinita. Chi
     * apre una vista di cartelle dichiara le copertine che usa, e il compilatore lo presidia,
     * come per [tints] e per il parametro di `Modifier.lowered`.
     */
    covers: Map<Long, Uri>,
    onPick: (Folder.Bucket) -> Unit,
    /**
     * Lo scorrimento della griglia.
     *
     * Si passa da fuori perché i due tasti del salto lo devono muovere; il valore di serie
     * serve agli altri chiamanti (la finestra delle destinazioni e il banco di prova).
     * ⚠️ **È in fondo all'elenco perché i chiamanti passano gli argomenti per posizione**: messo
     * in mezzo, uno di loro consegnerebbe una lambda a questo parametro.
     */
    state: LazyGridState = rememberLazyGridState(),
    /**
     * Di serie `fillMaxWidth`. La home passa `fillMaxSize` dentro un vano con `weight(1f)`
     * (voce `3.13-02`): senza, con poche cartelle i trascinamenti sul vuoto non riaprono
     * l'intestazione.
     */
    modifier: Modifier = Modifier.fillMaxWidth(),
    /**
     * Le cartelle che qui non si possono scegliere, nella forma di [portablePath]: quelle da cui
     * vengono i file, quando questa vista sceglie dove copiarli o spostarli (nota C dell'utente,
     * `4.45`).
     *
     * ⚠️ **Senza valore di serie**, come [covers]: chi chiama lo dichiara, quindi la finestra
     * delle destinazioni non lo può dimenticare. La home passa un insieme vuoto.
     */
    off: Set<String>,
    onHide: (Folder.Bucket) -> Unit
) {
    LazyVerticalGrid(
        state = state,
        // ⚠️ **Colonne fisse e non adattive**: il numero lo sceglie l'utente, e un minimo in dp
        // farebbe valere la scelta solo sugli schermi larghi. `FOLDER_CELL` dice a quante
        // colonne una copertina smette di servire.
        // Il numero scelto vale sul lato corto, come nella griglia delle immagini: [spread]
        // aggiunge colonne quando la finestra è larga, e il perché è sulla funzione.
        columns = GridCells.Fixed(spread(columns, LocalWindowInfo.current)),
        horizontalArrangement = Arrangement.spacedBy(FOLDER_GAP),
        verticalArrangement = Arrangement.spacedBy(FOLDER_GAP),
        // Lo spazio in fondo tiene l'ultima cartella libera dal FAB, che la coprirebbe
        // proprio a fine scorrimento, e dalla barra gestuale: il contenitore non ha il
        // rientro di sotto, perché le copertine arrivino al vetro.
        contentPadding = PaddingValues(bottom = BELOW_FAB + extraRoom() + bottomInset()),
        modifier = modifier
    ) {
        items(
            items = folders,
            key = { it.id },
            // Un tipo solo per tutte le celle, così Compose riusa la composizione di quelle
            // che escono dallo schermo mentre si scorre.
            contentType = { FOLDER_KIND }
        ) { bucket ->
            FolderCard(
                bucket = bucket,
                counted = counted,
                peeked = bucket.path?.let { listedIn(peeked, it) } == true,
                off = bucket.path?.let { listedIn(off, it) } == true,
                nameStyle = nameStyle,
                colour = colour,
                cover = bucket.coverIn(covers),
                tint = frontTintOf(tints[bucket.id]),
                onClick = { onPick(bucket) },
                // ⚠️ Niente [withHaptics] qui: la vibrazione la aggiunge [FolderCard], e una
                // seconda copia la raddoppierebbe.
                onLongClick = { onHide(bucket) }
            )
        }
    }
}

/**
 * Il Feed come griglia: un'intestazione per ogni mese (scelta C1 dell'utente) e sotto le
 * immagini, dalla più recente.
 *
 * ⚠️ **Le celle sono quelle della griglia di una cartella** ([Thumbnail], [gridGap]): sono le
 * stesse immagini, e un secondo disegno della miniatura si separerebbe dal primo alla prima
 * modifica.
 * Le colonne sono quelle scelte per le cartelle ([columns], `Settings.folderColumns`), contate
 * sul lato corto da [spread] come in ogni griglia dell'app: la pressione lunga sul Menu 'Start'
 * le cambia qui come nella griglia delle copertine.
 * ⚠️ **Il nome del mese viene dallo schema di sistema per la lingua** (`MMMMy`): l'italiano
 * scrive `ottobre 2026`, il giapponese `2026年10月`, e uno schema scritto qui andrebbe bene per
 * una lingua sola.
 * La selezione multipla è quella della griglia (richiesta dell'utente, `6.03`): la pressione
 * lunga sceglie, il trascinamento sceglie da un'immagine all'altra, i bordi scorrono, e mentre
 * qualcosa è scelto un tocco sceglie e non apre. Il perché di ogni parte è nella griglia di una
 * cartella (`GridScreen`); qui le intestazioni dei mesi sono anch'esse elementi della griglia,
 * quindi un'immagine si trova per chiave ([feedIndexAt]).
 */
@Composable
private fun FeedGrid(
    page: Feed.Page,
    columns: Int,
    state: LazyGridState,
    chosen: Set<Uri>,
    onChosen: (Set<Uri>) -> Unit,
    /** Lo spazio del pannello della selezione sotto l'ultima riga, `0.dp` senza selezione. */
    sheetRoom: Dp,
    modifier: Modifier,
    onOpen: (Int) -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val months = remember(locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMMy"), locale)
    }
    val picking = chosen.isNotEmpty()
    val haptics = LocalHapticFeedback.current
    val keys = remember(page) { page.items.withIndex().associate { (i, uri) -> uri.toString() to i } }
    // ⚠️ Letti dentro il gesto e non come sue chiavi, come nella griglia di una cartella: una
    // chiave che cambia annulla il trascinamento sotto il dito.
    val current by rememberUpdatedState(chosen)
    val choose by rememberUpdatedState(onChosen)
    var dragAt by remember { mutableStateOf<Offset?>(null) }
    var dragFrom by remember { mutableStateOf<Int?>(null) }
    var dragBase by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var dragOff by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val edgePx = with(density) { EDGE_BAND.toPx() }
    val speedPx = with(density) { EDGE_SPEED.toPx() }
    val grab = Modifier.pointerInput(page) {
        detectDragGesturesAfterLongPress(
            onDragStart = { at ->
                val hit = state.feedIndexAt(at, keys)
                if (hit != null) {
                    dragFrom = hit
                    dragBase = current
                    dragOff = page.items[hit] in current
                    haptics.performHapticFeedback(HOLD_BUZZ)
                    choose(feedSpan(page.items, current, hit, hit, dragOff))
                    dragAt = at
                }
            },
            onDrag = { change, _ ->
                change.consume()
                if (dragFrom != null) dragAt = change.position
            },
            onDragEnd = { dragFrom = null; dragAt = null },
            onDragCancel = { dragFrom = null; dragAt = null }
        )
    }
    LaunchedEffect(dragAt != null, page) {
        // La cella del giro precedente (rilievo D7): come nella griglia, la selezione si
        // ricostruisce solo quando il dito arriva su un'altra cella.
        var last = -1
        while (dragAt != null) {
            withFrameNanos { }
            val at = dragAt ?: break
            val from = dragFrom ?: break
            val height = state.layoutInfo.viewportSize.height.toFloat()
            val push = when {
                height <= 0f -> 0f
                at.y < edgePx -> -(edgePx - at.y) / edgePx
                at.y > height - edgePx -> (at.y - (height - edgePx)) / edgePx
                else -> 0f
            }
            if (push != 0f) state.scrollBy(push.coerceIn(-1f, 1f) * speedPx)
            val hit = state.feedIndexAt(at, keys) ?: continue
            if (hit == last) continue
            last = hit
            choose(feedSpan(page.items, dragBase, from, hit, dragOff))
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(spread(columns, LocalWindowInfo.current)),
        state = state,
        horizontalArrangement = Arrangement.spacedBy(gridGap()),
        verticalArrangement = Arrangement.spacedBy(gridGap()),
        // Lo stesso spazio in fondo delle copertine: l'ultima riga resta libera dal Menu 'Start'
        // e dalla barra gestuale. Durante la selezione il pannello prende il posto del tasto, e
        // lo spazio è il suo.
        contentPadding = PaddingValues(
            bottom = bottomInset() + if (picking) sheetRoom else BELOW_FAB + extraRoom()
        ),
        modifier = modifier.then(grab)
    ) {
        page.months.forEachIndexed { m, month ->
            val end = page.months.getOrNull(m + 1)?.start ?: page.items.size
            item(
                key = "month-${month.year}-${month.month}",
                span = { GridItemSpan(maxLineSpan) },
                contentType = MONTH_KIND
            ) {
                Text(
                    text = YearMonth.of(month.year, month.month).format(months)
                        .replaceFirstChar { it.titlecase(locale) },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(top = if (m == 0) 0.dp else MONTH_GAP, bottom = 4.dp)
                        .heading()
                )
            }
            items(
                count = end - month.start,
                // L'indirizzo è la chiave, come nella griglia di una cartella: le miniature già
                // decodificate restano con la loro immagine quando il flusso si sposta.
                key = { page.items[month.start + it].toString() },
                contentType = { THUMB_KIND }
            ) { i ->
                val at = month.start + i
                val uri = page.items[at]
                Thumbnail(
                    uri = uri,
                    position = at + 1,
                    total = page.items.size,
                    marked = false,
                    chosen = uri in chosen,
                    named = false,
                    mark = LastMark.FRAME,
                    room = 0,
                    shade = null
                ) {
                    // ⚠️ Il primo ramo è la coda di una pressione lunga, come nella griglia di una
                    // cartella: il tocco della cella scatta al rilascio, comunque sia durata la
                    // pressione.
                    when {
                        dragFrom != null -> Unit
                        picking -> {
                            haptics.performHapticFeedback(HOLD_BUZZ)
                            onChosen(if (uri in chosen) chosen - uri else chosen + uri)
                        }
                        else -> onOpen(at)
                    }
                }
            }
        }
    }
}

/** Un tipo solo per le intestazioni dei mesi del Feed, come [FOLDER_KIND] per le copertine. */
private const val MONTH_KIND = "month"

/** Lo spazio sopra l'intestazione di un mese, dalla seconda: un mese apre un gruppo nuovo. */
private val MONTH_GAP = 16.dp

/**
 * Le cartelle come elenco.
 *
 * Anche qui ogni riga mostra la copertina, piccola: la stessa icona di cartella davanti a ogni
 * riga non dice che cosa c'è dentro. Questa vista serve a leggere più nomi insieme.
 */
@Composable
internal fun Rows(
    folders: List<Folder.Bucket>,
    /**
     * Come l'omonimo di [Covers]: le cartelle in scena col minuto di 'Mostra nascoste', e senza
     * valore di serie per la stessa ragione.
     */
    peeked: Set<String>,
    /** Se sotto il nome si legge il conto. Vedi `Settings.listCount`. */
    counted: Boolean,
    /** Il corpo del testo scelto per questa vista. Vedi `Settings.listText`. */
    size: TextSize,
    /**
     * Dove si vede il colore di una cartella. Vedi [FolderColour].
     *
     * Gli stessi modi della griglia, con le stesse misure: la copertina di una riga è più
     * piccola, ma un filetto è una riga e non una frazione della cella, e rimpicciolirlo qui
     * vorrebbe dire due numeri per la stessa decisione.
     */
    colour: FolderColour,
    /** Il colore di ogni cartella che ne ha uno, per identificatore. */
    tints: Map<Long, Int>,
    /** Le copertine scelte a mano, e senza valore di serie: vedi il gemello di [Covers]. */
    covers: Map<Long, Uri>,
    onPick: (Folder.Bucket) -> Unit,
    /** Lo scorrimento dell'elenco: vedi il gemello di [Covers]. */
    state: LazyListState = rememberLazyListState(),
    /** Vedi il gemello di [Covers]: di serie `fillMaxWidth`, in home `fillMaxSize`. */
    modifier: Modifier = Modifier.fillMaxWidth(),
    /** Le cartelle che qui non si scelgono, senza valore di serie: vedi il gemello di [Covers]. */
    off: Set<String>,
    onHide: (Folder.Bucket) -> Unit
) {
    LazyColumn(
        state = state,
        // Lo spazio in fondo come in [Covers]: il contenitore non ha il rientro di sotto,
        // quindi quello per la barra gestuale si somma qui.
        contentPadding = PaddingValues(bottom = BELOW_FAB + extraRoom() + bottomInset()),
        modifier = modifier
    ) {
        items(items = folders, key = { it.id }, contentType = { ROW_KIND }) { bucket ->
            val tinta = frontTintOf(tints[bucket.id])
            val prestata = bucket.path?.let { listedIn(peeked, it) } == true
            val spenta = bucket.path?.let { listedIn(off, it) } == true
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (spenta) OFF_INK else if (prestata) PEEK_INK else 1f)
                    .combinedClickable(
                        enabled = !spenta,
                        role = Role.Button,
                        onClick = { onPick(bucket) },
                        onLongClick = withHaptics { onHide(bucket) },
                        hapticFeedbackEnabled = false
                    )
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val shape = RoundedCornerShape(FOLDER_CORNER)
                Box(
                    modifier = Modifier
                        .size(ROW_COVER)
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Cover(bucket.coverIn(covers))
                    FolderMark(colour, tinta, shape)
                    if (prestata) PeekMark(Modifier.align(Alignment.TopEnd))
                }
                Column(modifier = Modifier.weight(1f)) {
                    /*
                     * Il corpo lo sceglie l'utente, e i due testi si muovono insieme: il nome
                     * sale di un gradino e il conto lo segue, o a corpo grande la riga
                     * diventerebbe un titolo con una didascalia minuscola sotto.
                     */
                    Text(
                        text = bucket.name,
                        style = size.title(),
                        color = folderNameInk(colour, tinta),
                        fontWeight = folderNameWeight(colour),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    /*
                     * Nell'elenco i due conti sono parole e non icone, al contrario delle
                     * copertine: la riga è larga, e le parole si leggono meglio di due
                     * pittogrammi appaiati.
                     */
                    if (counted) Text(
                        text = listOfNotNull(
                            pluralStringResource(
                                R.plurals.folders_count, bucket.pictures, bucket.pictures
                            ).takeIf { bucket.pictures > 0 || bucket.clips == 0 },
                            pluralStringResource(
                                R.plurals.folders_clips, bucket.clips, bucket.clips
                            ).takeIf { bucket.clips > 0 }
                        ).joinToString(", "),
                        style = size.detail(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider()
        }
    }
}

/**
 * Una cartella: la copertina, il nome e quante immagini contiene.
 *
 * ⚠️ **La miniatura è la stessa richiesta della griglia e del visualizzatore**
 * (`Thumbs.request`, 512 px), anche per la copertina piccola dell'elenco: la misura è parte
 * della chiave di cache, quindi una copertina già vista altrove non costa niente, e chiederla
 * più piccola genererebbe una seconda volta la stessa immagine.
 * La copertina è decorativa e non ha descrizione: sotto ci sono già il nome e il conto, e
 * `clickable` fonde le semantiche dei figli, quindi TalkBack legge una voce sola.
 * Senza copertina compare l'icona della cartella e non un riquadro vuoto, che sembrerebbe una
 * cella guasta.
 */
@Composable
private fun FolderCard(
    bucket: Folder.Bucket,
    counted: Boolean,
    /**
     * Il corpo del nome, che dipende dalle colonne.
     *
     * Arriva da fuori: chi chiama lo ha già scelto con [folderNameStyle], e una seconda
     * chiamata qui sarebbe un secondo posto da cui i due potrebbero divergere.
     */
    nameStyle: TextStyle,
    /** Dove si vede il colore di questa cartella. Vedi [FolderColour]. */
    colour: FolderColour,
    /**
     * L'immagine da mostrare: quella scelta a mano se c'è, altrimenti la più recente.
     *
     * La sceglie chi chiama, con [coverIn]: risolverla qui vorrebbe dire passare a ogni cella la
     * mappa intera delle copertine. Anche la tinta arriva già risolta.
     */
    cover: Uri?,
    /** Il colore di questa cartella, o `null` se non ne ha scelto uno. */
    tint: Color?,
    /** Se questa cartella è in scena **in prestito**, cioè col minuto di 'Mostra nascoste'. */
    peeked: Boolean = false,
    /** Se questa cartella non si può scegliere: vedi `off` su [Covers]. */
    off: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val shape = RoundedCornerShape(FOLDER_CORNER)
    Column(
        // Il tocco lungo nasconde, con lo stesso gesto nelle due viste.
        modifier = Modifier
            .alpha(if (off) OFF_INK else if (peeked) PEEK_INK else 1f)
            .combinedClickable(
                enabled = !off,
                role = Role.Button,
                onClick = onClick,
                onLongClick = withHaptics(onLongClick),
                hapticFeedbackEnabled = false
            ),
        verticalArrangement = Arrangement.spacedBy(CARD_GAP)
    ) {
        /*
         * Il segno del prestito è dentro la copertina e sotto la stessa opacità della cella,
         * come vuole la specifica dell'utente: l'opacità ridotta dice che la cartella è
         * nascosta, il segno dice perché si vede. Vedi [PeekMark].
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Cover(cover)
            FolderMark(colour, tint, shape)
            if (peeked) PeekMark(Modifier.align(Alignment.TopEnd))
        }
        /*
         * Al massimo un a capo, con qualunque numero di colonne ([NAME_LINES], scelta
         * dell'utente). L'ellissi resta perché due righe non bastano sempre: senza
         * `Ellipsis` un nome lungo si taglierebbe senza dire che manca qualcosa.
         */
        Text(
            text = bucket.name,
            style = nameStyle,
            color = folderNameInk(colour, tint),
            fontWeight = folderNameWeight(colour),
            maxLines = NAME_LINES,
            overflow = TextOverflow.Ellipsis
        )
        /*
         * Prima l'icona, poi il numero (proposta B del mockup, scelta dell'utente): l'icona
         * ha larghezza fissa e resta incolonnata sotto il nome, e il numero cresce verso
         * destra. Il conto non ha la parola 'immagini', che su quattro colonne si tagliava:
         * il plurale `folders_count` serve alla descrizione per TalkBack ([Tally]), e
         * `maxLines = 1` tiene uguale l'altezza delle celle vicine.
         */
        if (counted) Tally(bucket)
    }
}

/**
 * Il segno che dice 'questa cartella è nascosta, la stai vedendo in prestito'.
 *
 * La specifica di 'Mostra nascoste' vuole un `∅` del colore dell'accento nell'angolo in alto a
 * destra: è un carattere e non un'icona, perché vuol dire 'insieme vuoto' in ogni lingua. È una
 * funzione sola perché lo disegnano le due viste della schermata iniziale.
 * ⚠️ **La pastiglia sotto il segno non è decorazione**: un carattere d'accento su una copertina
 * chiara sparisce, e la pastiglia gli dà il contrasto.
 */
@Composable
private fun PeekMark(modifier: Modifier = Modifier) {
    Text(
        text = PEEK_MARK,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(PEEK_EDGE)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = PEEK_PAD_INK))
            .padding(horizontal = PEEK_PAD, vertical = 0.dp)
    )
}

/**
 * Il colore di una cartella, disegnato dentro la sua copertina.
 *
 * Una funzione sola per le due viste e per i tre stili disegnati sulla copertina, perché le due
 * celle non divergano. Lo stile [FolderColour.NAME] non passa di qui: è il colore di un testo,
 * su [folderNameInk].
 * Senza una tinta scelta disegna nell'accento dell'app: vedi [folderInk].
 * ⚠️ **È dentro il riquadro ritagliato della copertina**, e i tre stili ci contano: il filetto
 * segue la curva degli angoli e l'alone non esce dal riquadro. Fuori dal `clip` il difetto si
 * vedrebbe solo guardando.
 */
@Composable
private fun BoxScope.FolderMark(colour: FolderColour, tint: Color?, shape: Shape) {
    val inchiostro = folderInk(tint)
    when (colour) {
        FolderColour.EDGE -> Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(TINT_EDGE)
                .background(inchiostro)
        )
        FolderColour.FRAME -> Box(
            modifier = Modifier.matchParentSize().border(TINT_FRAME, inchiostro, shape)
        )
        FolderColour.GLOW -> Box(
            modifier = Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0f to inchiostro.copy(alpha = TINT_GLOW),
                    TINT_GLOW_END to Color.Transparent
                )
            )
        )
        FolderColour.NONE, FolderColour.NAME -> Unit
    }
}

/**
 * Di che colore è una cartella: il suo, o l'accento dell'app.
 *
 * ⚠️ **Una cartella senza scelta non è una cartella senza colore** (correzione dell'utente,
 * `1.89`): il colore di fabbrica è l'accento del tema, quindi uno stile acceso si applica a
 * tutte le cartelle e non solo a quelle con una tinta. Senza ripiego, uno stile applicato a metà
 * elenco si leggerebbe come un difetto.
 * È lo stesso ripiego dell'intestazione, dove il gradiente prende `primary`.
 */
@Composable
private fun folderInk(tint: Color?): Color = tint ?: MaterialTheme.colorScheme.primary

/**
 * Di che colore si scrive il nome di una cartella: il suo, o quello di sempre.
 *
 * [Color.Unspecified] e non il colore del tema: così il testo prende quello del suo stile, senza
 * scriverlo una seconda volta qui. Senza una tinta scelta scrive nell'accento: vedi [folderInk].
 */
@Composable
private fun folderNameInk(colour: FolderColour, tint: Color?): Color =
    if (colour == FolderColour.NAME) folderInk(tint) else Color.Unspecified

/**
 * Quanto pesa il nome di una cartella tinta: **un gradino in più**, e viene dal mockup.
 *
 * ⚠️ **Il peso è metà dello stile 'nome'**, non una rifinitura: una tinta chiara su un fondo
 * chiaro si legge peggio dell'inchiostro di sempre, e il gradino di peso restituisce il
 * contrasto perso. Nel mockup erano dichiarati insieme.
 * `null` vuol dire il peso dello stile: negli altri stili il nome non si tocca. Il peso non
 * guarda se la tinta è scelta, perché il nome è tinto in tutti e due i casi (vedi [folderInk]).
 */
@Composable
private fun folderNameWeight(colour: FolderColour): FontWeight? =
    if (colour == FolderColour.NAME) FontWeight.Medium else null

/**
 * Quanto è spesso il filetto sotto la copertina: **quattro punti**, che è il numero del mockup.
 *
 * Su un filetto di quattro punti le tinte si distinguono, mentre su una riga più sottile le
 * varianti dello stesso verde si confondono.
 */
private val TINT_EDGE = 4.dp

/**
 * Quanto è spessa la cornice: **due punti**, come il bordo d'accento dell'app.
 *
 * Lo stesso numero di `Edge.kt`, il bordo delle superfici dell'app: una cornice più grossa
 * intorno a una copertina sembrerebbe un elemento di un'altra famiglia.
 */
private val TINT_FRAME = 2.dp

/** Quanto copre l'alone nel suo punto più forte, in cima alla copertina. Dal mockup. */
private const val TINT_GLOW = 0.55f

/** Dove l'alone è finito, in frazione dell'altezza della copertina. Dal mockup. */
private const val TINT_GLOW_END = 0.72f

@Composable
private fun Cover(uri: Uri?) {
    if (uri == null) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )
        return
    }
    val context = LocalContext.current
    val model = remember(uri, context) { Thumbs.request(context, uri) }
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Il lato per cui una copertina è disegnata, e il metro del numero di colonne che l'utente
 * sceglie.
 *
 * Il conto: le colonne che ci stanno sono `(spazio + distacco) / (minimo + distacco)`. Su uno
 * schermo da 360dp, tolti i 12 di margine per lato, restano 336: `(336 + 12) / (150 + 12)` fa
 * 2,1, quindi **due**, il valore di fabbrica dell'impostazione. A tre colonne una copertina
 * misura 104dp, a quattro 78dp.
 * ⚠️ **Due e non tre come le immagini**: sotto la copertina ci sono un nome e un conto, e su
 * tre colonne un nome vero (`WhatsApp Images`) si riduce a una sigla. Le miniature di una
 * cartella non hanno didascalia, e misurano 108dp.
 */
private val FOLDER_CELL = 150.dp

/** Il distacco fra le copertine: più largo di quello delle foto, perché qui separa schede. */
private val FOLDER_GAP = 12.dp

/**
 * Il margine della schermata, uguale sui quattro lati.
 *
 * ⚠️ **Il valore laterale non è estetico**: è quello che permette **due** colonne di
 * copertine su uno schermo da 360dp, e il conto è in [FOLDER_CELL]. Allargarlo stringe le
 * copertine.
 */
private val SCREEN_PAD = 12.dp

/** Lo stacco fra l'intestazione (o il titolo) e quello che viene sotto. */
private val HEADER_GAP = 8.dp

/** Il distacco fra la copertina e le sue due righe di testo, dentro una scheda. */
private val CARD_GAP = 6.dp

/**
 * Lo smusso della copertina.
 *
 * Più tondo dei 4dp delle miniature: una cella molto smussata si legge come un contenitore,
 * una quasi quadrata come un'immagine, e l'occhio distingue così le due schermate.
 */
private val FOLDER_CORNER = 12.dp

/** La copertina dell'elenco: grande quanto due righe di testo, che è l'altezza della riga. */
private val ROW_COVER = 48.dp

/**
 * L'opacità di una cartella vista **in prestito**: il 70%, dalla specifica dell'utente.
 *
 * Un'opacità e non un grigio: la copertina resta la sua, e la cella si legge come una cosa di
 * passaggio.
 */
private const val PEEK_INK = 0.7f

/** Il segno del prestito: l'insieme vuoto, dalla specifica dell'utente. */
private const val PEEK_MARK = "∅"

/** Quanto il segno rientra dall'angolo della copertina. */
private val PEEK_EDGE = 4.dp

/** Quanta aria ai fianchi del segno, dentro la sua pastiglia. */
private val PEEK_PAD = 6.dp

/** Quanto copre la pastiglia sotto il segno: abbastanza da staccarlo da una copertina chiara. */
private const val PEEK_PAD_INK = 0.85f

/**
 * Lo spazio fra le voci del dialogo delle opzioni, e fra le pastiglie di una sua fila.
 *
 * Lo stesso numero per le due cose: il dialogo è una colonna di file di pastiglie, e due
 * spaziature diverse darebbero una griglia storta.
 */
private val OPTION_GAP = 8.dp

/**
 * Quanto spazio si tiene per il nome della misura accanto allo slider.
 *
 * Tarato sulla parola più lunga fra le tre ('Normale' in italiano): senza un minimo, lo
 * slider si accorcerebbe passando da 'Piccolo' a 'Grande', e il fermo si sposterebbe sotto il
 * dito.
 */
private val SIZE_NAME = 72.dp

/**
 * Quanto è larga l'icona accanto al conto delle immagini.
 *
 * 15dp e non 16: accanto a un corpo di 12sp (`bodySmall`) un'icona da 16 pesa più del numero,
 * che è il dato.
 * ⚠️ A corpo di sistema piccolo è l'icona e non il testo a decidere l'altezza della riga del
 * conto, perché sp e dp non scalano insieme.
 */
private val COUNT_ICON = 15.dp

/** La distanza dell'icona dal numero: abbastanza da non toccarlo, non tanto da separarli. */
private val COUNT_GAP = 4.dp

/**
 * Quante righe può prendere il nome di una cartella: **due**, cioè un a capo e non più di uno
 * (scelta dell'utente, `0.77`). È una costante perché è una sua scelta e non un dettaglio del
 * disegno.
 */
private const val NAME_LINES = 2

/**
 * Da quante colonne il nome della cartella passa a un corpo più piccolo. Vedi
 * [folderNameStyle].
 *
 * Quattro è anche il massimo delle impostazioni, quindi la condizione vale per un solo valore:
 * è scritta come soglia perché con più colonne la risposta giusta sarebbe la stessa.
 */
private const val NARROW_COLUMNS = 4

/** Un tipo solo per tutte le copertine, che permette a Compose di riusarle. */
private const val FOLDER_KIND = "folder"

/** Come sopra, per le righe dell'elenco. */
private const val ROW_KIND = "folder-row"

/**
 * Le cartelle senza il permesso: che cosa serve e il tasto per chiederlo.
 *
 * Il testo dice quale permesso serve, e i permessi sono due: quello di `github` nomina tutti i
 * file e una pagina di sistema, che in AIV Play non esistono.
 * ⚠️ **Uno solo per la home e per la colonna laterale** (rilievo D28): una copia nella colonna
 * del tablet aveva il solo testo di AIV GitHub e niente tasto, quindi da là il permesso non si
 * poteva chiedere.
 */
@Composable
fun AccessPlea(onAsk: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(
                if (Store.files) R.string.folders_permission else R.string.folders_permission_media
            ),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onAsk) { Text(stringResource(R.string.folders_grant)) }
    }
}

/**
 * La riga che dice, in AIV Play, che l'app vede solo le immagini che la persona ha scelto, col
 * tasto che le richiede tutte.
 *
 * ⚠️ **Senza questa riga l'accesso parziale inganna**, ed è la ragione per cui la variante
 * `github` non lo chiede: una cartella da quattrocento immagini direbbe 'tre', senza dire perché.
 * ⚠️ **Il tasto apre la pagina di AIV nelle impostazioni di sistema** (nota dell'utente sulla
 * prova `5.10-03`): con l'accesso parziale già dato, il dialogo di sistema ripropone la scelta
 * delle immagini, mentre nella pagina dell'app `Foto e video` si concede per intero. Al ritorno
 * la schermata rilegge il permesso.
 * Gli allineamenti sono quelli della griglia: la frase comincia dove cominciano le copertine e
 * il testo del tasto finisce dove finiscono, quindi il tasto non ha il margine interno di
 * `TextButton`, che lo spostava di 12dp verso il centro. Frase e tasto condividono la linea di
 * base.
 */
@Composable
private fun PartialAccess(onAllowAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = PARTIAL_GAP).testTag(PARTIAL_TAG)
    ) {
        Text(
            text = stringResource(R.string.folders_partial),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).alignByBaseline()
        )
        // ⚠️ Il bersaglio del dito cresce verso l'alto, il basso e la frase, mai verso il bordo:
        // là il testo deve toccare il margine della griglia. Il testo vive in un nodo suo, così
        // il banco misura lui e non il bersaglio.
        Box(
            modifier = Modifier
                .alignByBaseline()
                .clickable(role = Role.Button, onClick = onAllowAll)
                .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.folders_partial_all),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Lo stacco fra la riga dell'accesso parziale e la griglia, uguale a quello fra le copertine. */
private val PARTIAL_GAP = FOLDER_GAP

/** Il tag della riga dell'accesso parziale, che il banco misura. */
internal const val PARTIAL_TAG = "partial-access"

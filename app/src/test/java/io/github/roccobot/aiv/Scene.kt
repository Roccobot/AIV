package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Le **scene condivise** fra le prove: le schermate vere, montate con gli argomenti minimi.
 *
 * ⚠️⚠️ **UN ELENCO DI ARGOMENTI SCRITTO DUE VOLTE È DUE ELENCHI CHE DIVERGONO**, ed è lo stesso
 * difetto che il censimento della UI ha censito nel codice vero: `FolderScreen` ne vuole
 * ventotto, quindi la seconda prova che la monta non se li riscrive, la chiama da qui. Il giorno
 * che quella firma cambia, il posto da correggere è uno.
 */

/**
 * La schermata iniziale con gli argomenti minimi.
 *
 * ⚠️ **Di serie è senza cartelle e senza permesso**: quello che le prime prove guardavano è il
 * FAB, che c'è in ogni caso perché dipende dalla vista scelta e non dai dati, quindi una casa
 * vuota era la scena più piccola che lo contenesse.
 * ⚠️⚠️ **DALLA `1.92` LE CARTELLE SI POSSONO PASSARE, e servono a chi misura lo scorrimento**:
 * senza righe non c'è niente da scorrere, quindi una prova sull'intestazione che si chiude non
 * avrebbe nessun gesto da fare. Le copertine chiedono le miniature al caricatore, che su una
 * macchina senza telefono non risponde: la cella resta vuota e il **nome** si legge lo stesso,
 * che è quello che una prova cerca nell'albero.
 *
 * @param buckets le cartelle finte da mostrare, vuote di serie.
 */
@Composable
internal fun Casa(
    buckets: List<Folder.Bucket> = emptyList(),
    /** I percorsi nascosti, per le prove di 'Mostra nascoste'. */
    hidden: Set<String> = emptySet(),
    /**
     * ⚠️ **The grid order is already chosen by default** (from the `5.80`): with `null` the home
     * asks it in a window, which would cover what the other tests look at. The test of that window
     * passes a selection without it.
     */
    selection: FolderSelection = FolderSelection(hidden = hidden, order = FolderOrder.ALPHA),
    /** Riceve la selezione cambiata, per le prove della disposizione e delle preferite. */
    onSelectionChange: (FolderSelection) -> Unit = {},
    /** Se le nascoste sono in scena col minuto in corso. */
    peeking: Boolean = false,
    /** Riceve le voci che 'Mostra' toglie: dalla `2.96` sono tutte quelle che coprono la cartella. */
    onUnhide: (Collection<String>) -> Unit = {},
    /** The view, for the tests of the Feed (6.02); the grid of covers by default. */
    view: FolderView = FolderView.GRID,
    /** Receives the view chosen in a menu. */
    onView: (FolderView) -> Unit = {},
    /** The Feed's stream, `null` while it is read. */
    feed: Feed.Page? = null,
    /** Receives the date chosen in the view options of the Feed. */
    onFeedDate: (FeedDate) -> Unit = {},
    /** Receives the stream and the position of a picture touched in the Feed. */
    onFeedOpen: (List<android.net.Uri>, Int) -> Unit = { _, _ -> },
    /** Counts the readings of the Feed the home asks for (6.03, 'Mostra nascoste'). */
    onFeedRead: (Boolean) -> Unit = {}
) {
    FolderScreen(
        view = view,
        columns = 3,
        counted = true,
        colour = FolderColour.NONE,
        tints = emptyMap(),
        hidden = hidden,
        selection = selection,
        onSelectionChange = onSelectionChange,
        onHide = {},
        peeking = peeking,
        onPeek = {},
        onUnhide = onUnhide,
        recents = emptyList(),
        onPick = {},
        onOpen = {},
        onOpenPage = {},
        onView = onView,
        onForget = {},
        onSettings = {},
        onSearch = {},
        onBin = {},
        feed = feed,
        feedDate = FeedDate.TAKEN,
        onFeedDate = onFeedDate,
        onFeedRead = onFeedRead,
        onFeedOpen = onFeedOpen,
        onFeedChanged = {},
        onColumns = {},
        listCount = true,
        listText = TextSize.NORMAL,
        treeHidden = false,
        treePictures = false,
        onListCount = {},
        onListText = {},
        onTreeHidden = {},
        onTreePictures = {},
        treePath = null,
        binOn = false,
        factFields = emptyList(),
        onTreePath = {},
        onTreeOpen = { _, _ -> },
        forStart = false,
        buckets = buckets,
        onRead = {}
    )
}

/**
 * L'editor completo con gli argomenti minimi, su tutto lo schermo.
 *
 * ⚠️ **Di serie è un'immagine intonsa che nessun comando può salvare**: niente filigrana e niente
 * ridimensionamento, perché tutti e due accenderebbero 'Salva' a immagine non toccata, e la
 * maggior parte delle prove guarda proprio quando 'Salva' si accende. I loro casi vivono in
 * `FiligranaTest` e in `RidimensionaTest`, che passano i propri valori.
 * ⚠️ **Il tema e i `CompositionLocal` restano a chi chiama**: il lato del FAB e l'ordine dei
 * moduli viaggiano così anche nell'app, e ogni classe li sceglie per sé.
 *
 * @param uri l'immagine da aprire: un file vero, perché finché l'anteprima non è decodificata i
 *   comandi sono spenti (vedi [quadrato]).
 * @param resize il piano di ridimensionamento configurato, che esiste anche quando non si applica.
 */
@Composable
internal fun Editore(
    uri: Uri = quadrato(),
    marked: Boolean = false,
    marking: Boolean = false,
    hasMark: Boolean = false,
    onMark: (Boolean) -> Unit = {},
    onMarkSetup: () -> Unit = {},
    resize: Resize.Plan = Resize.Plan(Resize.Mode.LONG, Resize.DEFAULT_PX),
    saved: Resize.Plan = Resize.NONE,
    resizing: Boolean = false,
    onResize: (Resize.Plan?) -> Unit = {},
    onResizeDefault: (Resize.Plan) -> Unit = {},
    onSave: (Look, Boolean) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AdvancedEditorScreen(
            uri = uri,
            busy = false,
            marked = marked,
            marking = marking,
            hasMark = hasMark,
            onMark = onMark,
            onMarkSetup = onMarkSetup,
            resize = resize,
            saved = saved,
            resizing = resizing,
            onResize = onResize,
            onResizeDefault = onResizeDefault,
            onSave = onSave,
            onBack = onBack
        )
    }
}

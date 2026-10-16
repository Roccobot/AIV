package io.github.roccobot.aiv

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * L'elenco delle cartelle a colonna, per il tablet: l'elenco a lato e il contenuto della
 * cartella nello spazio principale (mockup `publish/tablet.html`). Sul telefono ci sono
 * [FolderScreen] a pieno schermo e poi [GridScreen]; da 600 dp in su
 * ([Adaptive.sideAvailable]) le due cose sono insieme.
 * Non è la casa del telefono: niente intestazione, niente griglia di copertine, solo i nomi, la
 * cartella scelta e i comandi. La vista ad albero resta fuori (lo decide il chiamante): ha una
 * navigazione sua.
 * La forma (collaudo `3.38-03` e voce `3.41-01`): elenco in basso, con la maniglia per alzarlo
 * quando c'è spazio; in fondo Cerca, e sotto Cestino e Impostazioni con l'etichetta.
 */

@Composable
fun FolderRail(
    buckets: List<Folder.Bucket>?,
    selected: Long?,
    /** Nome della cartella scelta, per l'invito 'Cerca in *X*'; `null` per la ricerca globale. */
    selectedName: String? = null,
    selection: FolderSelection,
    peeking: Boolean,
    colour: FolderColour,
    tints: Map<Long, Int>,
    onPick: (Folder.Bucket) -> Unit,
    /**
     * Nasconde, come la conferma del tocco lungo sul telefono. Il gesto apre
     * [FolderHoldDialog], che chiama questa funzione.
     */
    onHide: (Folder.Bucket) -> Unit,
    /** Rimostra le voci che coprono la cartella. Vedi [FolderScreen]. */
    onUnhide: (Collection<String>) -> Unit,
    /** Modalità incluse e sorelle: la stessa scrittura delle impostazioni. */
    onSelectionChange: (FolderSelection) -> Unit,
    onRead: (Boolean) -> Unit,
    onSearch: () -> Unit,
    onBin: () -> Unit,
    onSettings: () -> Unit,
    width: Dp,
    /**
     * Quanto si alza il blocco elenco dal fondo, in pixel.
     *
     * ⚠️ **Lo tiene il modello** (voce `3.39-03`): le tre case del tablet (cartelle, griglia,
     * ricerca) ricreano ciascuna un [FolderRail], e un `remember` locale azzererebbe l'altezza
     * al tocco su una cartella.
     */
    liftPx: Float,
    onLift: (Float) -> Unit,
    /** Indice del primo elemento visibile nell'elenco cartelle (persistito nel modello). */
    listIndex: Int,
    /** Offset in pixel del primo elemento visibile. */
    listOffset: Int,
    onListScroll: (index: Int, offset: Int) -> Unit,
    /**
     * Se in fondo alla colonna ci sono Cerca, Cestino e Impostazioni.
     *
     * Spento sul tablet in verticale (scelta B7): là i tre comandi sono nella pillola in basso,
     * e due copie a pochi centimetri si leggerebbero come due cose diverse.
     */
    chrome: Boolean = true,
    modifier: Modifier = Modifier
) {
    val dragSpace = remember { RailDragSpace() }

    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                // ⚠️ Spazio fermo: il dito si misura qui, non sul blocco che si sposta.
                .onGloballyPositioned { dragSpace.parent = it }
                // L'inset alto (l'orologio) è qui, perché Cerca è in fondo. L'inset orizzontale
                // è solo qui e su Cerca: la riga di Cestino e Impostazioni è centrata sulla
                // colonna intera.
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                    )
                )
        ) {
            val density = LocalDensity.current
            val areaPx = with(density) { maxHeight.toPx() }
            // L'altezza massima del blocco (maniglia ed elenco), per limitare l'alzata.
            val blockCapPx = areaPx
            val maxLift = (areaPx - 48f).coerceAtLeast(0f)
            /*
             * ⚠️ **L'alzata segue il dito nello spazio fermo** (voce `3.41-01`): il delta locale
             * è calcolato nelle coordinate del nodo che `offset` sposta, quindi contiene anche
             * lo spostamento già applicato, e la lista torna indietro. L'ancora è la posizione
             * del dito nel riquadro che non si muove ([railLiftForFinger]).
             * ⚠️ **Mentre il dito è giù il modello non riscrive l'alzata**: `onLift` ricompone il
             * chiamante, e l'effetto che riallinea `liftPx` rimetterebbe il valore del
             * fotogramma prima. Il disegno usa solo lo stato locale.
             */
            var lift by remember { mutableFloatStateOf(liftPx) }
            val maxLiftNow = rememberUpdatedState(maxLift)
            val onLiftNow = rememberUpdatedState(onLift)
            LaunchedEffect(liftPx) {
                if (!dragSpace.dragging && lift != liftPx) lift = liftPx
            }
            val shown = lift.coerceIn(0f, maxLift)
            LaunchedEffect(maxLift, liftPx) {
                if (liftPx > maxLift) onLift(maxLift)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset { IntOffset(0, -shown.roundToInt()) }
                    .heightIn(max = with(density) { blockCapPx.toDp() })
            ) {
                /*
                 * La maniglia è sopra l'elenco, al centro, con due righe tenui (voce `3.71-06`),
                 * come quella di una scheda di Android.
                 * ⚠️ **Mai in fondo alla colonna**: con la navigazione a gesti il gesto di sistema
                 * dal bordo inferiore vince sul trascinamento, e la maniglia non si prende.
                 * Il riquadro del tocco ([HANDLE_TOUCH]) è più grande delle due righe, che sono
                 * sottili. Il nome per i lettori di schermo è `folders_rail_move`.
                 */
                val handleInk = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = HANDLE_INK)
                val handleName = stringResource(R.string.folders_rail_move)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = HANDLE_TOUCH * 2, height = HANDLE_TOUCH)
                        .semantics { contentDescription = handleName }
                        .onGloballyPositioned { dragSpace.handle = it }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    dragSpace.dragging = true
                                    dragSpace.anchored = false
                                },
                                onDragEnd = {
                                    dragSpace.dragging = false
                                    onLiftNow.value(lift)
                                },
                                onDragCancel = {
                                    dragSpace.dragging = false
                                    onLiftNow.value(lift)
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    val y = dragSpace.fingerY(change.position) ?: return@detectVerticalDragGestures
                                    if (!dragSpace.anchored) {
                                        // Il primo delta, oltre la soglia, è il solo locale
                                        // affidabile: il nodo non si è ancora mosso. L'ancora
                                        // è lì, e da lì si usa solo lo spazio fermo.
                                        dragSpace.anchorY = y - dragAmount
                                        dragSpace.anchorLift = lift
                                        dragSpace.anchored = true
                                    }
                                    val next = railLiftForFinger(
                                        dragSpace.anchorLift,
                                        dragSpace.anchorY,
                                        y,
                                        maxLiftNow.value,
                                    )
                                    if (next != lift) {
                                        lift = next
                                        onLiftNow.value(next)
                                    }
                                },
                            )
                        }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(HANDLE_GAP)) {
                        repeat(2) {
                            Box(
                                Modifier
                                    .size(width = HANDLE_LINE, height = HANDLE_STROKE)
                                    .background(handleInk, RoundedCornerShape(50))
                            )
                        }
                    }
                }
                FolderRailList(
                    buckets = buckets,
                    selected = selected,
                    selection = selection,
                    peeking = peeking,
                    colour = colour,
                    tints = tints,
                    onPick = onPick,
                    onHide = onHide,
                    onUnhide = onUnhide,
                    onSelectionChange = onSelectionChange,
                    onRead = onRead,
                    listIndex = listIndex,
                    listOffset = listOffset,
                    onListScroll = onListScroll,
                    rowInset = if (chrome) RAIL_ROW_INSET else RAIL_ROW_INSET_TALL,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = with(density) { (areaPx - 32f).coerceAtLeast(0f).toDp() })
                )
            }
        }

        if (chrome) {
            RailSearch(
                folderName = selectedName?.takeIf { selected != null },
                onSearch = onSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    // Solo l'inset orizzontale: l'orologio è in cima alla colonna, e un inset
                    // alto aprirebbe un vuoto sopra Cestino.
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                    )
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp)
            )

            /*
             * Cestino e Impostazioni in basso, con l'etichetta (collaudo `3.38-03`): accanto a
             * Cerca, come icone nude, sembravano azioni di cancellazione sulla lista.
             * ⚠️ **Centrati sulla colonna intera** (voce `3.40-01`): la riga è fuori dall'inset
             * orizzontale. Il testo è centrato in [RailAction], se no un'etichetta lunga
             * resterebbe a sinistra del suo riquadro.
             */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AIV Play ha il cestino solo da Android 11 (`Store.hasBin`).
                if (Store.hasBin) {
                    RailAction(
                        icon = { tint ->
                            Icon(
                                imageVector = Glyphs.Bin,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = stringResource(R.string.bin_title),
                        onClick = onBin,
                        modifier = Modifier.weight(1f)
                    )
                }
                RailAction(
                    icon = { tint ->
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = stringResource(R.string.hub_settings),
                    onClick = onSettings,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * L'elenco delle cartelle della colonna, con i suoi tre stati vuoti (permesso, lettura, nessuna
 * cartella) e il tocco lungo che apre [FolderHoldDialog].
 *
 * Lo usano due colonne: [FolderRail], che lo alza con la maniglia, e [FolderRailWide], che lo
 * ancora in basso fra il 40 e il 70% dell'altezza.
 * [modifier] va al solo elenco pieno, che deve rispettare l'altezza concessa dalla colonna; i tre
 * stati vuoti sono una riga di testo o un anello.
 */
@Composable
fun FolderRailList(
    buckets: List<Folder.Bucket>?,
    selected: Long?,
    selection: FolderSelection,
    peeking: Boolean,
    colour: FolderColour,
    tints: Map<Long, Int>,
    onPick: (Folder.Bucket) -> Unit,
    onHide: (Folder.Bucket) -> Unit,
    onUnhide: (Collection<String>) -> Unit,
    onSelectionChange: (FolderSelection) -> Unit,
    onRead: (Boolean) -> Unit,
    listIndex: Int,
    listOffset: Int,
    onListScroll: (index: Int, offset: Int) -> Unit,
    /** Il margine di ogni riga verso i bordi della colonna. */
    rowInset: Dp = RAIL_ROW_INSET,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(listIndex, listOffset)
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) -> onListScroll(index, offset) }
    }

    val context = LocalContext.current
    // ⚠️ Riletto a ogni ritorno nell'app e dopo la domanda, come nella casa del telefono
    // (rilievo D28): il permesso si concede anche dalle impostazioni di sistema.
    var granted by remember { mutableStateOf(Folder.granted(context)) }
    LifecycleResumeEffect(Unit) {
        granted = Folder.granted(context)
        onPauseOrDispose { }
    }
    val askAccess = Store.rememberAccessRequest(explain = false) { granted = Folder.granted(context) }
    LaunchedEffect(granted) { onRead(granted) }

    // La colonna è un elenco: prima i preferiti, e nessun ordine da griglia.
    val folders = buckets?.filter { selection.visible(it.path, peeking) }
        ?.let { selection.arrange(it, grid = false) }
    var holding by remember { mutableStateOf<Folder.Bucket?>(null) }

    when {
        !granted -> AccessPlea(onAsk = askAccess, modifier = Modifier.padding(16.dp))
        folders == null -> Box(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(Modifier.size(28.dp))
        }
        folders.isEmpty() -> Text(
            text = stringResource(R.string.folders_none),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
        else -> LazyColumn(state = listState, modifier = modifier) {
            items(folders, key = { it.id }) { bucket ->
                val chosen = bucket.id == selected
                val tinta = frontTintOf(tints[bucket.id])
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (chosen) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                        .combinedClickable(
                            role = Role.Button,
                            onClick = { onPick(bucket) },
                            onLongClick = withHaptics { holding = bucket },
                            hapticFeedbackEnabled = false,
                        )
                        .padding(horizontal = rowInset, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = when {
                            colour == FolderColour.NONE ->
                                MaterialTheme.colorScheme.onSurfaceVariant
                            tinta != null -> tinta
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(22.dp)
                    )
                    /*
                     * Il nome va a capo sulle giunture camelCase (richiesta
                     * dell'utente): [camelBreak], lo stesso criterio delle
                     * destinazioni e delle cartelle nascoste. Due righe al massimo,
                     * poi l'ellissi.
                     */
                    Text(
                        text = camelBreak(bucket.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (chosen) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
    holding?.let { bucket ->
        FolderHoldDialog(
            bucket = bucket,
            selection = selection,
            onHide = onHide,
            onUnhide = onUnhide,
            onSelectionChange = onSelectionChange,
            onDismiss = { holding = null },
        )
    }
}

/**
 * La colonna delle cartelle sullo schermo largo (telefono e tablet in orizzontale).
 *
 * L'elenco è ancorato in basso e non si sposta (scelta B2 dell'utente): niente maniglia, perché
 * in orizzontale l'altezza è poca. La sua altezza la decide il numero di cartelle (scelta B3), fra
 * [RAIL_LIST_MIN] e [RAIL_LIST_MAX] della colonna, e oltre scorre dentro.
 * Sopra c'è la testa ([head]) che la schermata sceglie: nella griglia di una cartella gradiente,
 * icona e pastiglie dell'intestazione, nella schermata iniziale l'intestazione di casa. Prende lo
 * spazio che l'elenco lascia, fra il 30 e il 60%.
 * Cerca, Cestino e Impostazioni non sono in fondo: sullo schermo largo sono nella pillola sotto
 * il filtro (decisione B4).
 */
@Composable
fun FolderRailWide(
    width: Dp,
    head: @Composable BoxScope.() -> Unit,
    list: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
            )
    ) {
        val alto = maxHeight
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
                content = head
            )
            list(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = alto * RAIL_LIST_MIN, max = alto * RAIL_LIST_MAX)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            )
        }
    }
}

/** La quota minima della colonna che l'elenco occupa sullo schermo largo: vedi [FolderRailWide]. */
const val RAIL_LIST_MIN = 0.4f

/** La quota massima della colonna che l'elenco occupa sullo schermo largo. */
const val RAIL_LIST_MAX = 0.7f

/**
 * Quanto alzare il blocco perché il dito, nello spazio fermo, sia andato da
 * [anchorY] a [fingerY].
 *
 * ⚠️ **Non si sommano i delta locali**: se il nodo si è spostato di D e il dito di F, il delta
 * locale vale F - D, e riapplicarlo fa oscillare il blocco. Qui F è [fingerY] - [anchorY],
 * misurati tutti e due nel riquadro che non si muove.
 * Dito verso l'alto: [fingerY] cala e l'alzata cresce (il blocco sale).
 */
internal fun railLiftForFinger(
    anchorLift: Float,
    anchorY: Float,
    fingerY: Float,
    maxLift: Float,
): Float = (anchorLift - (fingerY - anchorY)).coerceIn(0f, maxLift.coerceAtLeast(0f))

/** Coordinate del riquadro fermo e della maniglia, senza stato di composizione. */
private class RailDragSpace {
    var parent: LayoutCoordinates? = null
    var handle: LayoutCoordinates? = null
    var dragging: Boolean = false
    var anchored: Boolean = false
    var anchorY: Float = 0f
    var anchorLift: Float = 0f

    fun fingerY(local: Offset): Float? {
        val parent = parent ?: return null
        val handle = handle ?: return null
        if (!parent.isAttached || !handle.isAttached) return null
        return parent.localPositionOf(handle, local).y
    }
}

/**
 * Cerca, sopra Cestino e Impostazioni: lente a sinistra e campo contornato (mockup).
 *
 * Il campo è un tocco che apre la ricerca ([Screen.Search]), non un filtro sulle cartelle:
 * l'invito ('Cerca nelle cartelle', 'Cerca in *X*') parla di immagini.
 * La `Row` allinea al centro, come chiede il mockup, anche se il cerchio della lente Material è
 * nella metà superiore del glifo.
 */
@Composable
private fun RailSearch(
    folderName: String?,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    // L'invito lo compone [searchInvitation], che serve anche alla pillola del tablet in
    // verticale: il grassetto è scritto una volta sola.
    val placeholder = searchInvitation(folderName)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = stringResource(R.string.hub_search),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(28.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable(role = Role.Button, onClick = onSearch)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Un'azione in fondo alla colonna: icona ed etichetta. */
@Composable
private fun RailAction(
    icon: @Composable (tint: androidx.compose.ui.graphics.Color) -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = MaterialTheme.colorScheme.onSurface
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        icon(tint)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Il riquadro destro vuoto: nessuna cartella ancora scelta.
 *
 * Compare quando la casa è a due colonne e non si è ancora toccata una cartella; sul telefono
 * si apre subito la griglia. In alto l'identità della casa del telefono (collaudo `3.38-04`:
 * logo, titolo, firma con link), sotto l'invito a scegliere una cartella.
 */
@Composable
fun FoldersTabletHint(
    modifier: Modifier = Modifier,
    /**
     * Se sopra l'invito c'è l'identità dell'app.
     *
     * Spenta sullo schermo largo: là l'identità è nella testa della colonna delle cartelle
     * (scelta B5), e due loghi a pochi centimetri si leggerebbero come un errore.
     */
    identity: Boolean = true,
    /**
     * Lo spazio della pillola, che non fa parte dell'area utile: l'invito si centra in quello che
     * resta (voce `3.70-05`).
     */
    pillRoom: PaddingValues = PaddingValues()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(pillRoom)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (identity) {
            Identity(iconSize = HEADER_ICON)
            Spacer(Modifier.height(48.dp))
        }
        /*
         * Al 50% di opacità (voci `3.70-05`, `3.70-06` e `3.70-07`): è un invito, non un
         * contenuto, e la frase dice dove si sceglie.
         */
        Text(
            text = stringResource(R.string.folders_tablet_pick),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = PICK_HINT_INK)
        )
    }
}

/**
 * Il margine delle righe dell'elenco verso i bordi della colonna.
 *
 * Lo legge anche la testa di casa in orizzontale ([RailIdentity]), che si allinea alle cartelle.
 */
internal val RAIL_ROW_INSET = 12.dp

/** Il margine delle righe sul tablet in verticale, più largo (voce `3.70-07`). */
private val RAIL_ROW_INSET_TALL = 20.dp

/** Lunghezza e spessore delle due righe della maniglia, e lo stacco fra loro. */
private val HANDLE_LINE = 28.dp
private val HANDLE_STROKE = 2.dp
private val HANDLE_GAP = 4.dp

/** L'altezza del riquadro che la maniglia offre al dito; la larghezza è il doppio. */
private val HANDLE_TOUCH = 24.dp

/**
 * Quanto inchiostro hanno le righe della maniglia.
 *
 * La metà di [OFF_INK] (voce `3.72-02`: opacità dimezzata).
 */
private const val HANDLE_INK = OFF_INK / 2

/** L'opacità dell'invito a scegliere una cartella: il numero è dell'utente (voce `3.70-05`). */
private const val PICK_HINT_INK = 0.5f

/**
 * La riga a due colonne, cartelle e contenuto, come nel mockup.
 *
 * Il lato segue [Hand], come nel visualizzatore: per i destri l'elenco è a sinistra, per i
 * mancini a destra.
 */
@Composable
fun FoldersTabletSplit(
    panelOnStart: Boolean,
    rail: @Composable () -> Unit,
    detail: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize()) {
        if (panelOnStart) {
            rail()
        }
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            detail()
        }
        if (!panelOnStart) {
            rail()
        }
    }
}

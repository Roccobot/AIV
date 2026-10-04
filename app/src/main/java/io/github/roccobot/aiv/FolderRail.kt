package io.github.roccobot.aiv

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.DragIndicator
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * L'elenco delle cartelle a colonna, per il layout tablet.
 *
 * ⚠️⚠️ **NASCE CON LA `3.32`**: il mockup (`publish/tablet.html`, rotte `folders` / `grid`)
 * tiene l'elenco a lato e il contenuto della cartella nello spazio principale. Sul
 * telefono restano [FolderScreen] a pieno schermo e poi [GridScreen]; da 600 dp in su
 * ([Adaptive.sideAvailable]) le due cose restano insieme.
 * ⚠️ **Non sostituisce la casa sul telefono**: niente intestazione che si chiude, niente
 * griglia di copertine. Qui servono i nomi, la cartella scelta e un passaggio alle
 * impostazioni, come nel mockup.
 * ⚠️ **La vista ad albero resta fuori** (si decide nel chiamante): la sua navigazione è
 * un percorso a sé, e il mockup le dà una rotta propria (`system`).
 * ⚠️⚠️ **RIDISEGNATA NELLA `3.39`** (collaudo `3.38-03`, mockup
 * `aiv-338-mockup-tablet.png`): niente intestazione Cartelle; in cima solo Cerca con
 * placeholder dinamico; Cestino e Impostazioni in basso con etichetta; elenco di
 * default in basso, con maniglia per alzarlo/abbassarlo quando c'è spazio.
 * ⚠️⚠️ **DALLA `3.42` CERCA STA SOPRA CESTINO E IMPOSTAZIONI** (`3.41-01`), non in cima.
 */

@Composable
fun FolderRail(
    buckets: List<Folder.Bucket>?,
    selected: Long?,
    /** Nome della cartella scelta, per il placeholder "Cerca in *X*". Null = globale. */
    selectedName: String? = null,
    selection: FolderSelection,
    peeking: Boolean,
    colour: FolderColour,
    tints: Map<Long, Int>,
    onPick: (Folder.Bucket) -> Unit,
    /**
     * Nasconde, come il conferma del tocco lungo sul telefono.
     * Il gesto apre [FolderHoldDialog]: non chiama questo da solo.
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
     * ⚠️⚠️ **VIVE FUORI, DALLA `3.40`** (`3.39-03`): le tre case tablet (cartelle / griglia /
     * ricerca) ricreano ciascuna un [FolderRail], e un `remember` locale azzerava il lift al
     * tap su una cartella. Il modello lo tiene, così la posizione sopravvive al cambio di
     * schermata.
     */
    liftPx: Float,
    onLift: (Float) -> Unit,
    /** Indice del primo elemento visibile nell'elenco cartelle (persistito nel modello). */
    listIndex: Int,
    /** Offset in pixel del primo elemento visibile. */
    listOffset: Int,
    onListScroll: (index: Int, offset: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(listIndex, listOffset)
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) -> onListScroll(index, offset) }
    }

    val context = LocalContext.current
    var granted by remember { mutableStateOf(Folder.granted(context)) }
    LaunchedEffect(granted) { onRead(granted) }

    val folders = buckets?.filter { selection.visible(it.path, peeking) }
    val dragSpace = remember { RailDragSpace() }
    var holding by remember { mutableStateOf<Folder.Bucket?>(null) }


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
                // ⚠️⚠️ **L'OROLOGIO COPRE LA CIMA, DALLA `3.42`**: Cerca è scesa sopra
                // Cestino e Impostazioni, quindi l'inset alto sta sull'elenco, non sul campo.
                // L'inset orizzontale resta solo qui e su Cerca: la riga in basso è centrata
                // sulla colonna intera (`3.41`).
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                    )
                )
        ) {
            val density = LocalDensity.current
            val areaPx = with(density) { maxHeight.toPx() }
            // Stima dell'altezza del blocco (maniglia + elenco): serve a limitare il lift.
            val blockCapPx = areaPx
            val maxLift = (areaPx - 48f).coerceAtLeast(0f)
            /*
             * ⚠️⚠️ **IL LIFT SEGUE IL DITO NELLO SPAZIO FERMO, DALLA `3.42`** (`3.41-01`).
             * La `3.41` accumulava il delta locale, e non bastava: quel delta è calcolato
             * nelle coordinate del nodo che `offset` sposta. A ogni evento il dito e il
             * nodo si sono mossi insieme, quindi il delta contiene anche lo spostamento
             * già applicato e la lista torna indietro. L'ancora è la posizione del dito
             * nel riquadro che non si muove, e il lift è la distanza da lì.
             * ⚠️ **Mentre il dito è giù il modello non riscrive il lift**: `onLift`
             * ricompone il chiamante, e l'effetto che riallinea `liftPx` rimetteva il
             * valore del frame prima. Il disegno usa solo lo stato locale.
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
                 * ⚠️ **Maniglia a destra sopra l'elenco** (mockup): sei puntini, trascinabile
                 * in verticale. ContentDescription sul tocco: altrimenti è decorazione muta.
                 */
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 4.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    /*
                     * ⚠️⚠️ **MANIGLIA PIU DISCRETA DALLA `3.40`** (`3.39-03`): troppo
                     * contrastata sul fondo scuro del rail. Icona più piccola e tinta al
                     * 38% dell'onSurfaceVariant, così resta trovabile al tocco senza
                     * competere con Cerca e con l'elenco.
                     */
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = stringResource(R.string.folders_rail_move),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        modifier = Modifier
                            .size(20.dp)
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
                                            // L'oltre-soglia è l'unico delta locale affidabile:
                                            // il nodo non si è ancora mosso. L'ancora sta su
                                            // quel punto, e da lì si usa solo lo spazio fermo.
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
                    )
                }
                when {
                    !granted -> Text(
                        text = stringResource(R.string.folders_permission),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
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
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = with(density) { (areaPx - 32f).coerceAtLeast(0f).toDp() })
                    ) {
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
                                    )
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
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
                                 * ⚠️ **Il nome va a capo sulle giunture camelCase, dalla
                                 * `3.54`** (sua richiesta): [camelBreak] è lo stesso criterio
                                 * delle destinazioni e delle cartelle nascoste. Due righe al
                                 * massimo, poi l'ellissi in fondo; senza giunture il nome
                                 * lungo si spezzava dove capitava.
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
            }
        }

        RailSearch(
            folderName = selectedName?.takeIf { selected != null },
            onSearch = onSearch,
            modifier = Modifier
                .fillMaxWidth()
                // ⚠️ Inset orizzontale come quando il campo stava in cima: il disegno
                // non cambia, cambia solo il posto. Niente inset alto: l'orologio non
                // è qui, e un inset alto aprirebbe un vuoto sopra Cestino.
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                )
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp)
        )

        /*
         * ⚠️⚠️ **Cestino e Impostazioni IN BASSO CON ETICHETTA, DALLA `3.39`**
         * (`3.38-03`): non più icone in testata accanto a Cerca (sembravano azioni di
         * cancellazione sulla lista). Due colonne, icona sopra e testo sotto.
         */
        /*
         * ⚠️⚠️ **CENTRATI SULLA COLONNA INTERA, DALLA `3.41`** (`3.40-01`): le due colonne
         * uguali della `3.40` stavano dentro l'inset orizzontale, quindi il centro del
         * contenuto non era il centro della colonna. Qui la riga è larga quanto il rail.
         * Il testo è centrato: senza `TextAlign.Center` un'etichetta lunga resta a sinistra
         * del suo riquadro e la coppia sembra spostata.
         */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
 * Quanto alzare il blocco perché il dito, nello spazio fermo, sia andato da
 * [anchorY] a [fingerY].
 *
 * ⚠️ **Non si sommano i delta locali.** Se il nodo si è spostato di D e il dito
 * di F, il delta locale vale F - D: riapplicarlo sposta di F - D e al giro dopo
 * di D - (F - D), cioè oscilla. Qui F è [fingerY] - [anchorY], misurati entrambi
 * nel riquadro che non si muove.
 * ⚠️ Dito verso l'alto: [fingerY] cala e il lift cresce (il blocco sale).
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
 * Cerca sopra Cestino e Impostazioni: lente a sinistra + campo contornato (mockup).
 *
 * ⚠️ **Il campo è un tocco che apre la ricerca**, non un filtro locale sulle cartelle:
 * il placeholder dice "Cerca nelle cartelle" / "Cerca in *X*", cioè immagini, e la
 * schermata [Screen.Search] è già quella strada.
 * ⚠️ **Il testo del campo è centrato in verticale sul tondo della lente**: la `Row`
 * allinea al centro, e la lente Material ha il cerchio sulla metà superiore del glifo
 * ma l'allineamento centrale della riga è quello che il mockup chiede sul campo.
 */
@Composable
private fun RailSearch(
    folderName: String?,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val placeholder = if (folderName == null) {
        buildAnnotatedString { append(stringResource(R.string.folders_rail_search)) }
    } else {
        /*
         * ⚠️ **Grassetto solo sul nome**: il template porta un `%1$s`, si spezza sul
         * segnaposto sostituito con un carattere sentinella, e il nome va in mezzo
         * in grassetto. Così le lingue che mettono il nome altrove restano corrette.
         */
        val marker = "\u0001"
        val raw = stringResource(R.string.folders_rail_search_in, marker)
        val parts = raw.split(marker, limit = 2)
        buildAnnotatedString {
            append(parts[0])
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(folderName) }
            if (parts.size > 1) append(parts[1])
        }
    }
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

/** Un'azione in basso nel rail: icona + etichetta, non un'icona nuda. */
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
 * ⚠️ Solo sul tablet, quando la casa è a due colonne e non si è ancora toccata una
 * cartella. Sul telefono questa situazione non esiste: si apre la griglia subito.
 * ⚠️⚠️ **DALLA `3.39` È LA STESSA IDENTITÀ DELLA HOME TELEFONO** (`3.38-04`, mockup):
 * logo + titolo + firma con link, più in basso "Tocca una cartella per iniziare".
 */
@Composable
fun FoldersTabletHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Identity(iconSize = HEADER_ICON)
        Spacer(Modifier.height(48.dp))
        Text(
            text = stringResource(R.string.folders_tablet_pick),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Riga a due colonne per Cartelle + contenuto, allineata al mockup.
 *
 * ⚠️ **Il lato segue [Hand]** come nel Visualizzatore: destri -> elenco a sinistra;
 * mancini -> elenco a destra.
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

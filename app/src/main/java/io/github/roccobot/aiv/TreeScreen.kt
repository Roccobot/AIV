package io.github.roccobot.aiv

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * La vista 'Cartelle di sistema', la terza dopo la griglia e la lista: si naviga la memoria come
 * in un gestore di file (richiesta dell'utente); immagini e video si aprono nel visualizzatore,
 * gli altri file col sistema.
 * ⚠️ **Giustifica l'accesso a tutti i file davanti al Play Store** (vedi [Tree]): senza questa
 * vista il permesso non si potrebbe chiedere.
 *
 * ⚠️ **Non è una terza resa dello stesso elenco**: griglia e lista mostrano le cartelle del
 * MediaStore, questa legge il disco ([Tree]). Sotto non condividono niente.
 *
 * ⚠️ **Gli indirizzi che escono di qui sono `file://`**: ricavare quello del MediaStore per ogni
 * riga costerebbe una query per file. Le miniature passano quindi da `ThumbnailUtils`, e per
 * consegnare un file a un altro programma serve [openWithSystem].
 */
@Composable
fun TreeList(
    /** Dove si è, e `null` vuol dire 'in cima'. Vive nel modello: vedi `ViewerViewModel`. */
    path: String?,
    /** I percorsi che l'utente ha nascosto, per segnarli. Vedi `Settings.hiddenFolders`. */
    hidden: Set<String>,
    selection: FolderSelection = FolderSelection(hidden = hidden),
    onSelectionChange: (FolderSelection) -> Unit = {},
    /** Se eliminare vuol dire mandare nel cestino. Vedi `Settings.binOn`. */
    binOn: Boolean,
    /** Se si vedono anche i file che cominciano per punto. Vedi `Settings.treeHidden`. */
    showHidden: Boolean,
    /** Se le cartelle senza immagini spariscono. Vedi `Settings.treePictures`. */
    onlyPictures: Boolean,
    /** I campi delle informazioni, nell'ordine scelto. Vedi `Settings.factRows`. */
    factFields: List<FactField>,
    onPath: (String?) -> Unit,
    onOpen: (List<Uri>, Int) -> Unit,
    /**
     * Dice alla casa se la selezione tiene impegnato il FAB, come [GridScreen.onBusy].
     * Senza, il FAB resterebbe sopra [PickSheet] e ruberebbe i tocchi della scheda.
     */
    onBusy: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val res = LocalResources.current
    val scope = rememberCoroutineScope()
    val roots = remember(context) { Tree.roots(context) }

    /**
     * Quante volte la cartella è stata toccata da un'operazione.
     *
     * Serve a rileggere senza cambiare cartella: dopo un'eliminazione o una rinomina l'elenco
     * descrive un disco che non c'è più.
     */
    var tick by remember { mutableIntStateOf(0) }

    /**
     * Selezione multipla come in griglia e lista: il tocco lungo su un'immagine o un video entra
     * in selezione.
     * ⚠️ **Ha per chiave la cartella** (`path`): cambiando cartella la selezione si azzera, o
     * resterebbero indirizzi di file che non si vedono più.
     */
    var chosen by rememberSaveable(path, stateSaver = UriSetSaver) {
        mutableStateOf(emptySet())
    }
    val picking = chosen.isNotEmpty()
    LaunchedEffect(picking) { onBusy(picking) }
    DisposableEffect(Unit) { onDispose { onBusy(false) } }
    BackHandler(enabled = picking) { chosen = emptySet() }

    var folderActing by remember { mutableStateOf<String?>(null) }
    folderActing?.let { SystemFolderDialog(it, selection, onSelectionChange) { folderActing = null } }
    // Salvabile, come nelle altre schermate che chiamano `FileJobDialogs`, perché ruotando la
    // finestra aperta non si chiuda. Che cosa si salva lo dice `FileJobSaver`.
    var job by rememberSaveable(stateSaver = FileJobSaver) { mutableStateOf<FileJob?>(null) }

    /**
     * Svuota la selezione prima di lanciare, come nella griglia: il lavoro vive in [FileWork] e
     * sopravvive alla scheda e alla rotazione, e una selezione aperta inviterebbe a ripeterlo.
     */
    val perform: (FileKind, suspend () -> FileTree.Outcome) -> Unit = { kind, work ->
        chosen = emptySet()
        FileWork.scope.launch {
            val out = work()
            // Il cestino non dà l'avviso, lo decide [FileKind.speaks]: la sua notifica dice la
            // stessa cosa e offre di annullare, e due messaggi si coprirebbero.
            if (kind.speaks(out)) {
                Notices.say(outcomeText(res, out, kind), NOTICE_LONG_MS)
            }
            tick++
        }
    }

    /*
     * Con una memoria sola non si mostra l'elenco delle memorie, come nei gestori di file: una
     * voce sola da toccare per forza non decide niente.
     */
    val here = path ?: roots.singleOrNull()?.file?.absolutePath

    /*
     * Lo scorrimento dell'elenco vive qui, perché lo muovano i due tasti del salto (risposta
     * `tutto` dell'utente a `d-salti-dove`). L'elenco delle memorie non ne ha bisogno: entra
     * tutto nello schermo.
     */
    val scroll = rememberLazyListState()

    /**
     * L'elenco della cartella corrente. Vive qui perché [PickSheet] e 'Tutti' lo vedano anche
     * fuori dal ramo che disegna le righe.
     */
    var spots by remember(here) { mutableStateOf<List<Tree.Spot>?>(null) }
    LaunchedEffect(here, tick, showHidden, onlyPictures) {
        spots = if (here == null) null else Tree.list(File(here), showHidden, onlyPictures)
    }

    // A tutta altezza: serve al `weight` del riquadro che centra 'la cartella è vuota' (vedi
    // più sotto).
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (here == null) {
                Roots(roots, onPath)
                return@Column
            }
            val dir = remember(here) { File(here) }
            val up = remember(dir, roots) { Tree.parent(dir, roots) }
            PathBar(dir, up, roots, onPath)
            when {
                spots == null -> Unit
                /*
                 * La frase è al centro dello spazio sotto la barra del percorso (richiesta
                 * dell'utente): in cima si leggerebbe come l'inizio di un elenco.
                 * ⚠️ **Il `weight` funziona solo perché la colonna è a tutta altezza**: senza
                 * `fillMaxSize` il riquadro sarebbe alto zero e la frase tornerebbe in cima.
                 */
                spots!!.isEmpty() -> Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        // La stessa chiave della griglia: una frase sola per la stessa idea.
                        text = stringResource(R.string.folder_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        // Il margine dal basso è l'altezza del FAB, o la frase finirebbe sotto
                        // di lui.
                        modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = BELOW_FAB)
                    )
                }
                else -> {
                    /*
                     * Da 600 dp due colonne (modello `system` dell'utente): le cartelle a lato e
                     * i file nello spazio principale. Sul telefono un elenco solo.
                     */
                    val widthDp = LocalConfiguration.current.screenWidthDp
                    val dual = Adaptive.sideAvailable(widthDp)
                    val all = spots!!
                    if (dual) {
                        val folders = all.filter { it.folder }
                        val files = all.filter { !it.folder }
                        val panelOnStart = LocalPadLook.current.hand == Hand.RIGHT
                        @Composable
                        fun TreeFiles() {
                            Spots(
                                spots = files.ifEmpty { all },
                                scroll = scroll,
                                selection = selection,
                                chosen = chosen,
                                picking = picking,
                                onPath = onPath,
                                onOpen = onOpen,
                                onToggle = { uri -> chosen = chosen.toggleUri(uri) },
                                onHoldMedia = { uri ->
                                    chosen = if (picking) chosen.toggleUri(uri) else setOf(uri)
                                },
                                onHoldFolder = { folderActing = it },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        if (folders.isEmpty()) {
                            TreeFiles()
                        } else {
                            FoldersTabletSplit(
                                panelOnStart = panelOnStart,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                rail = {
                                    LazyColumn(
                                        modifier = Modifier
                                            .width(Adaptive.sideWidth(widthDp))
                                            .fillMaxHeight()
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentPadding = PaddingValues(bottom = BELOW_FAB)
                                    ) {
                                        items(items = folders, key = { it.path }) { spot ->
                                            SpotRow(
                                                spot = spot,
                                                marked = when (selection.mode) {
                                                    FolderMode.INCLUDED ->
                                                        listedIn(selection.included, spot.path)
                                                    FolderMode.EXCLUDED ->
                                                        selection.hidden(spot.path)
                                                },
                                                authorized = selection.mode == FolderMode.INCLUDED,
                                                selected = false,
                                                onHold = { folderActing = spot.path },
                                                onClick = { onPath(spot.path) }
                                            )
                                        }
                                    }
                                },
                                detail = { TreeFiles() }
                            )
                        }
                    } else {
                        Spots(
                            spots = all,
                            scroll = scroll,
                            selection = selection,
                            chosen = chosen,
                            picking = picking,
                            onPath = onPath,
                            onOpen = onOpen,
                            onToggle = { uri -> chosen = chosen.toggleUri(uri) },
                            onHoldMedia = { uri ->
                                chosen = if (picking) chosen.toggleUri(uri) else setOf(uri)
                            },
                            onHoldFolder = { folderActing = it }
                        )
                    }
                }
            }
        }

        /* La stessa scheda della selezione della griglia, con le stesse azioni. */
        val mediaHere = remember(spots) {
            spots.orEmpty().filter { it.media }.map { Uri.fromFile(it.file) }
        }
        PickSheet(
            visible = picking,
            actions = treePickActions(
                chosen = chosen,
                binOn = binOn,
                mediaInFolder = mediaHere,
                onChosen = { chosen = it },
                onJob = { job = it }
            )
        )
    }
    FileJobDialogs(job = job, fields = factFields, onClose = { job = null }, onRun = perform)
}

/** Le memorie da cui si può partire, quando ce n'è più di una. */
@Composable
private fun Roots(roots: List<Tree.Root>, onPath: (String?) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = BELOW_FAB)) {
        items(items = roots, key = { it.file.absolutePath }) { root ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPath(root.file.absolutePath) }
                    .padding(vertical = ROW_PAD, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SdStorage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(GLYPH)
                )
                Text(text = root.name, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/**
 * Dove si è, e il tasto per salire.
 *
 * Il nome della cartella grande e il percorso piccolo sotto: in una riga sola il nome finirebbe
 * in coda, dove l'ellissi lo taglia. In cima il tasto per salire non c'è, invece di essere spento.
 */
@Composable
private fun PathBar(dir: File, up: File?, roots: List<Tree.Root>, onPath: (String?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Salire dalla radice riporta all'elenco delle memorie, solo quando quell'elenco esiste.
        val above: (() -> Unit)? = when {
            up != null -> ({ onPath(up.absolutePath) })
            roots.size > 1 -> ({ onPath(null) })
            else -> null
        }
        /*
         * La freccia è a destra e dentro un tondo (richiesta dell'utente): il testo comincia
         * sempre allo stesso punto, e il tondo pieno la fa leggere come un comando.
         */
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                text = dir.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dir.absolutePath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (above != null) {
            FilledTonalIconButton(onClick = above) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = stringResource(R.string.tree_up)
                )
            }
        }
    }
}

/**
 * Le righe di una cartella.
 *
 * ⚠️ **La serie da sfogliare si costruisce dall'elenco già letto**: così si sfoglia nell'ordine
 * appena visto, cioè per nome ([Tree.list]), anche se fra il disegno e il tocco il disco cambia.
 */
@Composable
private fun Spots(
    spots: List<Tree.Spot>,
    /**
     * Lo scorrimento dell'elenco. Arriva da fuori perché i due tasti del salto vivono nel
     * contenitore della schermata.
     */
    scroll: LazyListState,
    selection: FolderSelection,
    chosen: Set<Uri>,
    picking: Boolean,
    onPath: (String?) -> Unit,
    onOpen: (List<Uri>, Int) -> Unit,
    onToggle: (Uri) -> Unit,
    onHoldMedia: (Uri) -> Unit,
    onHoldFolder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Si ricava una volta per elenco: la posizione di un file fra i soli media non è la sua
    // posizione fra le righe, che comprendono anche le cartelle.
    val reels = remember(spots) { spots.filter { it.media } }
    val addresses = remember(reels) { reels.map { Uri.fromFile(it.file) } }
    /*
     * ⚠️ L'indirizzo di ogni riga, una volta per elenco, e la posizione da `itemsIndexed`
     * (rilievo D32): una ricerca nell'elenco per ogni riga disegnata costerebbe quanto la
     * cartella.
     */
    val rowUris = remember(spots) { spots.map { if (it.media) Uri.fromFile(it.file) else null } }

    LazyColumn(
        modifier = modifier,
        state = scroll,
        contentPadding = PaddingValues(bottom = BELOW_FAB)
    ) {
        itemsIndexed(items = spots, key = { _, it -> it.path }) { index, spot ->
            val uri = rowUris.getOrNull(index)
            val selected = uri != null && uri in chosen
            fun mediaSelected(at: Int): Boolean = rowUris.getOrNull(at)?.let { it in chosen } == true
            // Angoli arrotondati solo al primo e all'ultimo del blocco (voce `3.27-01`): la
            // selezione consecutiva si legge come un unico rettangolo.
            val roundTop = selected && !mediaSelected(index - 1)
            val roundBottom = selected && !mediaSelected(index + 1)
            SpotRow(
                spot = spot,
                /*
                 * Nella modalità delle escluse conta anche una cartella coperta dal padre (voce
                 * `3.13-08`), o la sottocartella nascosta col ramo non avrebbe la dicitura. Nella
                 * modalità delle incluse conta l'autorizzazione esatta della voce.
                 */
                marked = spot.folder && when (selection.mode) {
                    FolderMode.INCLUDED -> listedIn(selection.included, spot.path)
                    FolderMode.EXCLUDED -> selection.hidden(spot.path)
                },
                authorized = selection.mode == FolderMode.INCLUDED,
                selected = selected,
                roundTop = roundTop,
                roundBottom = roundBottom,
                onHold = when {
                    spot.folder -> ({ onHoldFolder(spot.path) })
                    spot.media && uri != null -> ({ onHoldMedia(uri) })
                    else -> null
                },
                onClick = {
                    when {
                        spot.folder -> onPath(spot.path)
                        picking && uri != null -> onToggle(uri)
                        spot.media -> {
                            val at = reels.indexOfFirst { it.path == spot.path }
                            if (at >= 0) onOpen(addresses, at)
                        }
                        else -> openWithSystem(context, spot.file)
                    }
                }
            )
        }
    }
}

@Composable
private fun SpotRow(
    spot: Tree.Spot,
    marked: Boolean,
    authorized: Boolean,
    selected: Boolean,
    /** Angoli della selezione: solo primo e ultimo del blocco consecutivo. */
    roundTop: Boolean = true,
    roundBottom: Boolean = true,
    onHold: (() -> Unit)?,
    onClick: () -> Unit
) {
    // Il tocco lungo può mancare (i file che non sono media non ne hanno), quindi la vibrazione
    // si compone qui: vedi la nota di [withHaptics].
    val haptics = LocalHapticFeedback.current
    val hold = remember(onHold, haptics) {
        onHold?.let { premuto ->
            {
                haptics.performHapticFeedback(HOLD_BUZZ)
                premuto()
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) Modifier.background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    RoundedCornerShape(
                        topStart = if (roundTop) 8.dp else 0.dp,
                        topEnd = if (roundTop) 8.dp else 0.dp,
                        bottomStart = if (roundBottom) 8.dp else 0.dp,
                        bottomEnd = if (roundBottom) 8.dp else 0.dp
                    )
                ) else Modifier
            )
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = hold,
                hapticFeedbackEnabled = false
            )
            .padding(vertical = ROW_PAD, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SpotGlyph(spot)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = spot.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Una cartella non ha la seconda riga: dire quanto contiene costerebbe una lettura
            // di directory per riga (vedi [Tree.Spot]).
            if (!spot.folder) {
                Text(
                    text = "${formatBytes(spot.size)}  ${moment(spot.stamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        // Questa vista mostra anche le cartelle nascoste, perché descrive il disco: il segno
        // dice a chi l'ha nascosta perché nella griglia non compare.
        if (marked) {
            Text(
                text = stringResource(if (authorized) R.string.folder_authorized_mark else R.string.tree_hidden),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Le azioni della selezione multipla in 'Cartelle di sistema': la stessa scheda e lo stesso
 * ordine della griglia ([PickSheet]), su indirizzi `file://`.
 */
@Composable
private fun treePickActions(
    chosen: Set<Uri>,
    binOn: Boolean,
    mediaInFolder: List<Uri>,
    onChosen: (Set<Uri>) -> Unit,
    onJob: (FileJob) -> Unit
): List<PadAction> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val res = LocalResources.current
    val list = remember(chosen) { chosen.toList() }
    return listOf(
        PadAction(
            key = PadKey.COPY,
            icon = Glyphs.FolderPair,
            label = R.string.menu_copy_here,
            onHold = { onJob(FileJob.Duplicate(list)) },
            holdLabel = R.string.pick_duplicate
        ) { onJob(FileJob.Transfer(list, move = false)) },
        PadAction(PadKey.MOVE, Glyphs.FolderPairDashed, R.string.pick_move) {
            onJob(FileJob.Transfer(list, move = true))
        },
        PadAction(PadKey.SHARE, Icons.Default.Share, R.string.menu_share) {
            val now = list
            scope.launch { ImageActions.shareMany(context, now) }
        },
        PadAction(PadKey.RENAME, Glyphs.TextCursor, R.string.pick_rename) {
            onJob(FileJob.Rename(list))
        },
        PadAction(
            key = PadKey.DELETE,
            icon = Glyphs.PickDelete,
            label = R.string.pick_delete,
            onHold = if (!binOn) null else {
                { onJob(FileJob.Delete(list, forGood = true)) }
            },
            holdLabel = if (!binOn) null else R.string.pick_forever
        ) {
            onJob(FileJob.Delete(list, forGood = !binOn))
        },
        PadAction(PadKey.INFO, Icons.Outlined.Info, R.string.pick_info) {
            onJob(FileJob.Facts(list))
        },
        PadAction(PadKey.LIST, Icons.AutoMirrored.Outlined.FormatListBulleted, R.string.pick_list) {
            val now = list
            scope.launch {
                ImageActions.copyNames(context, now, null)
                Notices.say(res.getString(R.string.pick_list_done))
            }
        },
        PadAction(
            key = PadKey.ALL,
            icon = Glyphs.PickAll,
            label = R.string.pick_all_short,
            onHold = { onChosen(emptySet()) },
            holdLabel = R.string.pick_none
        ) {
            onChosen(mediaInFolder.toSet())
        },
        PadAction(PadKey.NONE, Glyphs.PickNone, R.string.pick_none) {
            onChosen(emptySet())
        },
        PadAction(PadKey.INVERT, Glyphs.PickInvert, R.string.pick_invert) {
            onChosen(mediaInFolder.toSet() - chosen)
        }
    ).inOrder(LocalPadLook.current.pick)
}

/** Aggiunge o toglie un indirizzo dalla selezione, come nella griglia. */
private fun Set<Uri>.toggleUri(uri: Uri): Set<Uri> =
    if (uri in this) this - uri else this + uri

/**
 * Il quadratino di sinistra: la miniatura se è un'immagine o un video, il simbolo se no. La
 * miniatura passa da `Thumbs`, che per un `file://` usa `ThumbnailUtils`.
 */
@Composable
private fun SpotGlyph(spot: Tree.Spot) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(GLYPH)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            spot.media -> {
                val model = remember(spot.path, context) {
                    Thumbs.request(context, Uri.fromFile(spot.file))
                }
                Image(
                    painter = rememberAsyncImagePainter(model = model),
                    contentDescription = null,
                    // `Crop` come nella griglia: adattata, l'immagine lascerebbe due bande vuote.
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(GLYPH).clip(RoundedCornerShape(4.dp))
                )
                if (spot.clip) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.grid_item_clip),
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(GLYPH * 0.6f)
                    )
                }
            }
            spot.folder -> Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(GLYPH * 0.7f)
            )
            else -> Icon(
                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(GLYPH * 0.7f)
            )
        }
    }
}

/**
 * Consegna al sistema un file che non è né un'immagine né un video.
 *
 * ⚠️ **Passa dal MediaStore e non dal nostro provider, per sicurezza**: un altro programma non
 * può leggere un nostro `file://` (da Android 7), e il FileProvider serve apposta una cartella
 * sola, quella delle condivisioni (`ImageActions.shareMany`). Il MediaStore indicizza anche i
 * documenti, quindi per quasi ogni file un indirizzo `content://` esiste già.
 * Quando l'indirizzo non c'è (un file appena copiato e non ancora indicizzato) un avviso lo dice.
 * Il tipo si chiede all'estensione (`MimeTypeMap`): il MediaStore lo dichiara solo per i media,
 * e senza tipo il sistema non sa a chi proporre il file.
 */
private fun openWithSystem(context: Context, file: File) {
    val uri = Tree.contentUri(context, file)
    if (uri == null) {
        Notices.say(context.getString(R.string.tree_unopenable))
        return
    }
    val kind = MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(file.extension.lowercase())
        ?: runCatching { context.contentResolver.getType(uri) }.getOrNull()
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, kind)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val ok = runCatching { context.startActivity(intent); true }
        .getOrElse { if (it is ActivityNotFoundException) false else throw it }
    if (!ok) Notices.say(context.getString(R.string.tree_unopenable))
}

/** Il lato del quadratino di sinistra: come la miniatura della lista, ma più piccolo. */
private val GLYPH = 44.dp

/** Il respiro sopra e sotto una riga. */
private val ROW_PAD = 8.dp

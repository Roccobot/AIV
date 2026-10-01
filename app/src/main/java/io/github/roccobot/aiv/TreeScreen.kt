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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
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
 * La vista **'Cartelle di sistema'**, la terza dopo la griglia e la lista.
 *
 * ⚠️⚠️ **NASCE DALLA `0.84`, ed è la richiesta dell'utente**: *navigare liberamente la
 * memoria come un gestore di file, col fuoco che resta sulle immagini*. Le fotografie e i
 * filmati si aprono nel visualizzatore, gli altri file si vedono e si aprono col sistema.
 * ⚠️⚠️ **E SERVE ANCHE A GIUSTIFICARE IL PERMESSO PESANTE davanti al Play Store**: l'accesso
 * a tutti i file non è ammesso per un visualizzatore di immagini, lo è per un gestore di
 * file. Chi togliesse questa vista dovrebbe togliere anche il permesso, e con lui metà di
 * quello che l'app sa fare.
 *
 * ⚠️ **NON è una terza resa dello stesso elenco**, al contrario di griglia e lista, che
 * mostrano le stesse cartelle del MediaStore in due modi: qui l'elenco è **un altro**, viene
 * dal disco (vedi [Tree]), e si naviga invece di scorrerlo soltanto. Sta nello stesso posto
 * delle altre due perché la domanda dell'utente era *dove sono le mie cose*, e le tre
 * risposte sono sorelle; ma chi ci lavora sopra sappia che sotto non condividono niente.
 *
 * ⚠️⚠️ **Gli indirizzi che escono di qui sono `file://` e non `content://`**, ed è
 * deliberato: questa vista **è** il disco. Ricavare l'indirizzo del MediaStore per ogni riga
 * vorrebbe dire una query per file, cioè trecento query per aprire una cartella. Il costo
 * dichiarato è che i `file://` sono il secondo genere di indirizzo dell'app, quello del
 * ripiego di `Folder.fromDisk`: le miniature passano da `ThumbnailUtils` invece che dal
 * provider, e la consegna a un altro programma non è possibile (vedi [openWithSystem]).
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
     * ⚠️ **Serve a RILEGGERE senza cambiare cartella**: dopo un'eliminazione o una rinomina
     * l'elenco che si ha in mano descrive un disco che non c'è più. Un numero che cresce è la
     * chiave più piccola che fa ripartire la lettura.
     */
    var tick by remember { mutableIntStateOf(0) }

    /**
     * Selezione multipla come in griglia/lista (giro 3.24, campo libero): il tocco lungo su un
     * media entra in selezione invece di aprire [ActionPad] su un solo file.
     * ⚠️ **Chiave `here`**: cambiando cartella la selezione si azzera, come uscendo da una
     * cartella in griglia. Senza, resterebbero URI di file che non si vedono più.
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
    // ⚠️ Salvabile dalla `1.81`, come nelle altre due schermate che chiamano `FileJobDialogs`:
    // ruotando, la finestra aperta si chiudeva e con lei quello che si stava scrivendo. Che cosa
    // si salva e che cosa no sta su `FileJobSaver`.
    var job by rememberSaveable(stateSaver = FileJobSaver) { mutableStateOf<FileJob?>(null) }

    /**
     * ⚠️ **Svuota la selezione PRIMA di lanciare**, come nella griglia: il lavoro vive
     * nell'ambito della schermata e sopravvive alla scheda che si chiude, mentre una selezione
     * lasciata aperta sopra un'operazione in corso invita a toccarla due volte.
     */
    val perform: (FileKind, suspend () -> FileTree.Outcome) -> Unit = { kind, work ->
        chosen = emptySet()
        scope.launch {
            val out = work()
            // ⚠️ **Il cestino tace, e chi decide è [FileKind.speaks]**: la sua notifica
            // dice la stessa cosa e in più offre di disfare, e due messaggi in fondo
            // allo schermo si coprirebbero a vicenda.
            if (kind.speaks(out)) {
                Notices.say(outcomeText(res, out, kind), NOTICE_LONG_MS)
            }
            tick++
        }
    }

    /*
     * ⚠️⚠️ **CON UNA MEMORIA SOLA NON SI MOSTRA L'ELENCO DELLE MEMORIE**, ed è quello che
     * fanno i gestori di file: una schermata con una voce sola da toccare per forza è un
     * passo che non decide niente. Con una scheda SD inserita le radici sono due, e allora
     * la scelta esiste davvero.
     */
    val here = path ?: roots.singleOrNull()?.file?.absolutePath

    /*
     * ⚠️⚠️ **LO SCORRIMENTO DELL'ELENCO VIVE QUI, ED È LA SUA RISPOSTA `tutto`**
     * (`d-salti-dove` del giro della `1.95`): fino alla `1.95` questa vista teneva il proprio
     * scorrimento dentro [Spots], quindi i due tasti del salto non avevano niente da muovere, e
     * questa era una delle due schermate scoperte.
     * ⚠️ **Uno solo, e per l'elenco dell'albero**: l'altra lista è quella delle memorie, che
     * sono due, e in una lista che ci sta tutta nello schermo i tasti non compaiono comunque.
     */
    val scroll = rememberLazyListState()

    /**
     * Elenco della cartella corrente. Vive qui (non dentro la colonna) perché [PickSheet]
     * e 'Tutti' devono vederlo anche fuori dal ramo che disegna le righe.
     */
    var spots by remember(here) { mutableStateOf<List<Tree.Spot>?>(null) }
    LaunchedEffect(here, tick, showHidden, onlyPictures) {
        spots = if (here == null) null else Tree.list(File(here), showHidden, onlyPictures)
    }

    // ⚠️ **A tutta ALTEZZA e non solo a tutta larghezza**, dal 2026-08-31: serve al `weight`
    // del riquadro che centra 'la cartella è vuota' (vedi più sotto). Le due liste non
    // cambiano di una virgola, perché una `LazyColumn` senza peso prendeva già tutto lo
    // spazio che il genitore le concedeva.
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
                 * ⚠️⚠️ **LA FRASE STA AL CENTRO DEL VUOTO, e non appesa sotto il percorso**
                 * (richiesta dell'utente, 2026-08-31). Il vuoto di una cartella è tutto lo
                 * spazio che resta sotto la barra del percorso: una riga di testo posata in
                 * cima a quello spazio si legge come l'inizio di un elenco che non arriva mai,
                 * mentre in mezzo si legge per quello che è, cioè che qui non c'è niente.
                 * ⚠️ **Il `weight` funziona solo perché la colonna qui sopra è a tutta altezza**:
                 * in una colonna che si adatta al contenuto non c'è spazio residuo da
                 * distribuire, e questo riquadro verrebbe alto zero. Le due cose si tengono, e
                 * chi togliesse `fillMaxSize` rimetterebbe la frase in cima senza capire perché.
                 */
                spots!!.isEmpty() -> Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        // ⚠️ La stessa chiave della griglia, dalla 1.09: erano due stringhe con
                        // lo stesso significato in 28 lingue, e due frasi per un'idea sola
                        // divergono al primo ritocco di una delle due (era già successo: qui
                        // 'La cartella è vuota', là 'Questa cartella non contiene più niente').
                        text = stringResource(R.string.folder_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        // ⚠️ Il margine dal basso è l'altezza del FAB: senza, su una
                        // cartella vuota la frase finirebbe centrata **sotto** di lui.
                        modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = BELOW_FAB)
                    )
                }
                else -> Spots(
                    spots = spots!!,
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

        /*
         * ⚠️⚠️ **STESSA SCHEDA DELLA GRIGLIA**, non più [ActionPad] al centro su un file solo
         * (giro 3.24, campo libero sulle cartelle di sistema): selezione multipla e azioni
         * uguali a griglia/lista.
         */
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
 * ⚠️ **Il nome della cartella grande e il percorso piccolo sotto**, e non il percorso solo:
 * dentro `/storage/emulated/0/DCIM/Camera` quello che si guarda è `Camera`, e in una riga
 * sola quella parola finirebbe in coda, cioè nel pezzo che l'ellissi mangia.
 * ⚠️ **Il tasto per salire manca in cima**, invece di esserci spento: un tasto grigio che non
 * si può premere occupa lo stesso spazio di uno che funziona e non dice niente di più della
 * sua assenza.
 */
@Composable
private fun PathBar(dir: File, up: File?, roots: List<Tree.Root>, onPath: (String?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ⚠️ Salire dalla radice riporta all'elenco delle memorie, e solo quando quell'elenco
        // esiste: con una memoria sola la radice è il capolinea.
        val above: (() -> Unit)? = when {
            up != null -> ({ onPath(up.absolutePath) })
            roots.size > 1 -> ({ onPath(null) })
            else -> null
        }
        /*
         * ⚠️⚠️ **LA FRECCIA STA A DESTRA E DENTRO UN TONDO, dalla 1.03** (richiesta
         * dell'utente): a sinistra spingeva il nome della cartella verso il centro e ne
         * mangiava una fetta proprio sui percorsi lunghi, che sono quelli in cui il nome
         * serve di più. A destra il testo comincia sempre allo stesso punto, e il tondo pieno
         * la fa leggere come un comando invece che come una decorazione del titolo.
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
 * ⚠️⚠️ **LA SERIE DA SFOGLIARE SI COSTRUISCE QUI, dall'elenco GIÀ LETTO**, e non rileggendo
 * la cartella: così l'ordine che si sfoglia è esattamente quello che si è appena visto, per
 * costruzione e non per accordo fra due funzioni. Una seconda lettura avrebbe anche potuto
 * dare un elenco diverso, perché fra il disegno e il tocco il disco può cambiare.
 * ⚠️ **E l'ordine NON è quello della galleria**: qui si ordina per nome, come in un gestore
 * di file (vedi [Tree.list]). Chi apre una fotografia da qui sfoglia in ordine di nome, ed è
 * la cosa giusta: è l'elenco che ha davanti agli occhi.
 */
@Composable
private fun Spots(
    spots: List<Tree.Spot>,
    /**
     * Lo scorrimento dell'elenco.
     *
     * ⚠️ **Arriva da fuori e non nasce qui, dalla `2.00`**: i due tasti del salto vivono nel
     * contenitore della schermata, e uno stato ricordato qui dentro non lo raggiungerebbe.
     */
    scroll: LazyListState,
    selection: FolderSelection,
    chosen: Set<Uri>,
    picking: Boolean,
    onPath: (String?) -> Unit,
    onOpen: (List<Uri>, Int) -> Unit,
    onToggle: (Uri) -> Unit,
    onHoldMedia: (Uri) -> Unit,
    onHoldFolder: (String) -> Unit
) {
    val context = LocalContext.current
    // ⚠️ Si ricava una volta per elenco e non a ogni tocco: la posizione di un file dentro i
    // soli media non è la sua posizione fra le righe, che comprendono anche le cartelle.
    val reels = remember(spots) { spots.filter { it.media } }
    val addresses = remember(reels) { reels.map { Uri.fromFile(it.file) } }

    LazyColumn(state = scroll, contentPadding = PaddingValues(bottom = BELOW_FAB)) {
        items(items = spots, key = { it.path }) { spot ->
            val uri = if (spot.media) Uri.fromFile(spot.file) else null
            SpotRow(
                spot = spot,
                /*
                 * ⚠️ **In modalità escluse conta anche una cartella solo coperta dal padre**
                 * (giro 3.24, `3.13-08`): senza, la sottocartella nascosta col ramo non
                 * mostrava la dicitura a destra. In modalità incluse resta l'autorizzazione
                 * esatta sulla voce.
                 */
                marked = spot.folder && when (selection.mode) {
                    FolderMode.INCLUDED -> listedIn(selection.included, spot.path)
                    FolderMode.EXCLUDED -> selection.hidden(spot.path)
                },
                authorized = selection.mode == FolderMode.INCLUDED,
                selected = uri != null && uri in chosen,
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
    onHold: (() -> Unit)?,
    onClick: () -> Unit
) {
    // ⚠️ Il tocco lungo qui può non esserci (le cartelle senza foto non ne hanno uno),
    // quindi la vibrazione si compone sul posto: vedi la nota di [withHaptics].
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
                    RoundedCornerShape(8.dp)
                ) else Modifier
            )
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = hold)
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
            // ⚠️ Sotto una cartella non c'è nessuna seconda riga, e non è una dimenticanza:
            // l'unica cosa che si potrebbe scrivere è quanto contiene, e saperlo costa una
            // lettura di directory per riga (vedi [Tree.Spot]).
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
        // ⚠️ Il segno delle cartelle nascoste esiste perché questa vista le MOSTRA, al
        // contrario di tutte le altre: un gestore di file che non fa vedere una cartella che
        // sul disco c'è dice una bugia sul disco. Ma chi l'ha nascosta deve poterlo sapere,
        // o si chiederà perché quella cartella non compare nella griglia.
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
 * Azioni della selezione multipla in Cartelle di sistema: stessa scheda e stesso ordine
 * della griglia ([PickSheet]), su una lista di URI `file://`.
 *
 * ⚠️⚠️ **Sostituisce il riquadro centrato su UN file** (giro 3.24, campo libero): il tocco
 * lungo entra in selezione come in griglia/lista, e le azioni lavorano su tutti i scelti.
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
        }
        PadAction(PadKey.INVERT, Glyphs.PickInvert, R.string.pick_invert) {
            onChosen(mediaInFolder.toSet() - chosen)
        }
    ).inOrder(LocalPadLook.current.pick)
}

/** Aggiunge o toglie un URI dalla selezione, come in griglia. */
private fun Set<Uri>.toggleUri(uri: Uri): Set<Uri> =
    if (uri in this) this - uri else this + uri

/**
 * Il quadratino di sinistra: la miniatura se è un'immagine o un filmato, il simbolo se no.
 *
 * ⚠️ **La miniatura passa dal caricatore di sempre** (`Thumbs`), quindi un `file://` finisce
 * su `ThumbnailUtils`, che per i video usa la funzione giusta dalla `0.83`. Niente di nuovo
 * da scrivere qui.
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
                    // ⚠️ `Crop` come in griglia: in un quadratino da 44dp una fotografia
                    // adattata lascerebbe due bande vuote invece di riempirlo.
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
 * Consegna al sistema un file che non è né una fotografia né un filmato.
 *
 * ⚠️⚠️ **PASSA DAL MEDIASTORE E NON DAL NOSTRO PROVIDER, ed è una scelta di sicurezza.** Un
 * altro programma non può leggere un nostro `file://` (da Android 7 il sistema lo rifiuta), e la
 * via comoda sarebbe allargare il FileProvider a tutta la memoria. Non si fa: quel provider
 * serve **una cartella sola**, quella delle condivisioni, apposta (vedi
 * `ImageActions.shareMany`), e allargarlo per la comodità di aprire un PDF vorrebbe dire
 * pagare in sicurezza una cosa che il sistema sa già fare da sé. Il MediaStore indicizza
 * anche i documenti, quindi per la stragrande maggioranza dei file un indirizzo `content://`
 * esiste già, ed è **suo**, non nostro.
 * ⚠️ **Quando quell'indirizzo non c'è si dice, invece di provarci e fallire in silenzio**: un
 * file appena copiato che l'indicizzazione non ha ancora visto non si apre, e chi tocca deve
 * sapere perché.
 * ⚠️ **Il tipo si chiede all'estensione** (`MimeTypeMap`), perché il MediaStore lo dichiara
 * solo per quello che ha indicizzato come media: senza tipo il sistema non sa a chi
 * proporlo, e il dialogo esce vuoto.
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

/*
 * ⚠️ **L'arrotondamento non vive più qui, dalla 1.28**: diceva 'lo stesso del menu del
 * visualizzatore' e lo teneva copiando il numero, che è il modo in cui due valori uguali
 * diventano diversi. Adesso è `MENU_ROUND` in `Menus.kt`, e non si può più copiare.
 */

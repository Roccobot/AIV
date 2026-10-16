package io.github.roccobot.aiv

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Dove mettere i file scelti: prima le cartelle dell'app, e a un tocco si va.
 *
 * ⚠️ **Un dialogo a tutto schermo e non una schermata, per la selezione**: quello che si sta per
 * copiare vive in `GridScreen` e se ne va con lei (uscire da una cartella vuol dire 'lascia
 * stare'). Navigando verso un'altra schermata la selezione sparirebbe mentre la si usa; il dialogo
 * è sopra la griglia, che resta viva sotto.
 *
 * Si entra dalla vista normale dell'app, e l'albero è dietro 'Sfoglia...' (riscontro dell'utente,
 * giro della `1.80`). ⚠️ L'albero resta tutto quello che si può scegliere, e le cartelle della
 * galleria sono la scorciatoia ai posti più probabili: da sole sarebbero l'insieme sbagliato (vedi
 * la testa di `FileTree`), perché lasciano fuori le cartelle vuote, quelle escluse e quelle senza
 * immagini. La scorciatoia non chiede conferma, come chiesto; nell'albero la conferma resta, perché
 * là il tocco su una riga entra nella cartella.
 *
 * [action] dice che cosa succederà: copiare e spostare chiedono la stessa cartella e fanno due cose
 * diverse, e il tasto che le avvia è l'ultimo posto in cui distinguerle.
 */
@Composable
fun DestinationDialog(
    @StringRes action: Int,
    onDismiss: () -> Unit,
    onPick: (File) -> Unit,
    /** Percorsi già in elenco (inclusi o esclusi): l'indicatore, e niente nuova aggiunta. */
    alreadyListed: Set<String> = emptySet(),
    /**
     * Le cartelle da cui vengono i file, nella forma di [portablePath]: la destinazione deve essere
     * diversa dall'origine, e si impedisce invece di segnalare un errore (nota C dell'utente,
     * 4.45). Nell'elenco sono grigie e non rispondono al tocco; nell'albero ci si può entrare,
     * perché le sue sottocartelle sono destinazioni valide, ed è spento solo il tasto che la
     * sceglierebbe.
     * ⚠️ Senza valore di serie: chi copia file deve dire da dove vengono.
     */
    from: Set<String>
) {
    val context = LocalContext.current
    val look = LocalDestLook.current

    /*
     * Chi ha la vista ad albero, in modalità esclusione, entra nell'albero: la scorciatoia mostra
     * la vista già attiva, e per lui quella è l'albero. In modalità inclusione si parte dalle
     * destinazioni incluse anche con la vista ad albero, e sfogliare tutte le cartelle resta una
     * scelta esplicita, con 'Sfoglia...'. Salvabile, così girando il telefono a metà scelta non si
     * torna in cima.
     */
    var sfoglia by rememberSaveable { mutableStateOf(look.view == FolderView.TREE && look.folderMode == FolderMode.EXCLUDED) }
    if (!sfoglia) {
        FolderShortcut(
            action = action,
            look = look,
            onDismiss = onDismiss,
            onPick = onPick,
            onBrowse = { sfoglia = true },
            from = from
        )
        return
    }
    // Le radici si leggono su `Dispatchers.IO` (rilievo E29): chiederle al sistema tocca il disco e
    // può creare cartelle, e `null` è l'elenco ancora in lettura.
    val roots by produceState<List<File>?>(null) {
        value = withContext(Dispatchers.IO) { FileTree.roots(context) }
    }
    val scope = rememberCoroutineScope()
    // Le frasi degli avvisi da `LocalResources`, come nella griglia: il perché è là.
    val res = LocalResources.current

    /**
     * Dove si sta guardando, e `null` all'elenco delle memorie: sui telefoni con la scheda le
     * radici sono due, e serve un livello sopra per passare dall'una all'altra. Con una memoria
     * sola si entra dritti dentro, appena le radici sono lette.
     */
    // Salvata per percorso, come la scelta di essere nell'albero (`sfoglia`): girando il telefono
    // si resta nella cartella in cui si è, invece di tornare alla radice.
    var here by rememberSaveable(stateSaver = FILE_BY_PATH) { mutableStateOf<File?>(null) }
    LaunchedEffect(roots) {
        if (here == null) roots?.singleOrNull()?.let { here = it }
    }
    var children by remember { mutableStateOf<List<File>?>(null) }
    var naming by remember { mutableStateOf(false) }

    /*
     * ⚠️ **Il cestino non è una destinazione** (richiesta dell'utente): copiarci un file vorrebbe
     * dire metterlo in un posto che si svuota, e spostarcelo sarebbe eliminarlo senza conferma e
     * senza che l'archivio delle provenienze lo sappia. Quindi sparisce dall'elenco, e il tasto in
     * fondo controlla di nuovo, perché alla cartella dell'app si arriva anche da un altro ramo.
     */
    LaunchedEffect(here) {
        children = here?.let { dir -> FileTree.children(dir).filterNot { Bin.holds(context, it) } }
    }

    /** Risale di un livello, e torna `false` quando non c'è più niente sopra. */
    fun up(): Boolean {
        val at = here ?: return false
        val radici = roots.orEmpty()
        // Il confronto è sul percorso: due `File` costruiti in modi diversi non sono uguali anche
        // quando indicano la stessa cartella.
        if (radici.any { it.absolutePath == at.absolutePath }) {
            // Con una memoria sola non esiste un elenco a cui tornare: si esce.
            if (radici.size <= 1) return false
            here = null
            return true
        }
        here = at.parentFile ?: return false
        return true
    }

    // Sul tablet, da 600 dp, il foglio non supera [Adaptive.destinationMaxWidth] ed è centrato,
    // come nel mockup di Copia o sposta; sul telefono è a tutto schermo. I dialoghi corti usano
    // [Centred] con [Adaptive.dialogMaxWidth].
    val destMaxW = Adaptive.destinationMaxWidth(windowWidthDp())
    Dialog(
        onDismissRequest = onDismiss,
        // [fullWindow]: qui c'è un elenco da scorrere e un `safeDrawingPadding()`, e il perché
        // delle due righe è là.
        properties = fullWindow()
    ) {
        BackHandler { if (!up()) onDismiss() }

        Box(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (destMaxW != null) Modifier.widthIn(max = destMaxW) else Modifier)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (!up()) onDismiss() }) {
                        Icon(
                            imageVector = Glyphs.Material.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = here?.name ?: stringResource(R.string.dest_storages),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.heading()
                        )
                        // Il percorso intero sotto il nome, non al suo posto: due cartelle possono
                        // chiamarsi uguale, ma un percorso lungo come titolo non si legge.
                        here?.let {
                            Text(
                                text = it.absolutePath,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.MiddleEllipsis
                            )
                        }
                    }
                    // AIV Play non può creare una cartella condivisa (`Store.files`).
                    if (here != null && Store.files) {
                        IconButton(onClick = { naming = true }) {
                            Icon(
                                imageVector = Glyphs.Material.CreateNewFolder,
                                contentDescription = stringResource(R.string.dest_new)
                            )
                        }
                    }
                }

                HorizontalDivider()

                val listed = if (here == null) roots else children
                // Il peso è sul contenitore e non sull'elenco: così il tasto resta in fondo anche
                // quando la cartella è vuota.
                Box(modifier = Modifier.weight(1f)) {
                when {
                    listed == null -> Unit
                    listed.isEmpty() -> Text(
                        text = stringResource(R.string.dest_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(items = listed, key = { it.absolutePath }) { dir ->
                            val inList = portablePath(dir.absolutePath) in alreadyListed
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { here = dir }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Glyphs.Material.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = camelBreak(dir.name),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (inList) {
                                    Text(
                                        text = stringResource(R.string.folder_already_listed),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                }

                // ⚠️ **Si sceglie la cartella in cui si è, non una toccata nell'elenco**: il tocco
                // su una riga entra, o non si arriverebbe mai in fondo a un ramo. Il tasto dice
                // quale cartella prende.
                here?.let { dir ->
                    val inList = portablePath(dir.absolutePath) in alreadyListed
                    Button(
                        onClick = { onPick(dir) },
                        enabled = !Bin.holds(context, dir) && !inList && portablePath(dir.absolutePath) !in from,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            if (inList) stringResource(R.string.folder_already_listed)
                            else stringResource(action, dir.name)
                        )
                    }
                }
            }
            }
        }

        if (naming) {
            NewFolderDialog(
                onDismiss = { naming = false },
                onCreate = { name ->
                    naming = false
                    val parent = here ?: return@NewFolderDialog
                    // Un nome già preso entra nella cartella che c'è, un fallimento si dice: il
                    // perché è su `FileTree.makeFolder`.
                    scope.launch {
                        val made = FileTree.makeFolder(parent, name)
                        if (made != null) here = made
                        else Notices.say(res.getString(R.string.folder_create_fail), NOTICE_LONG_MS)
                    }
                }
            )
        }
    }
}

/**
 * Come l'utente guarda le cartelle nell'app, cioè quello che serve per rifarne la vista.
 *
 * Arriva da chi chiama e non si legge qui: questo dialogo non ha le impostazioni in mano, e le tre
 * schermate che lo aprono sì (la stessa scelta dei campi delle informazioni in `FileJobDialogs`).
 * Contiene solo i valori che cambiano il disegno della finestra, e non l'oggetto intero delle
 * impostazioni, così una preferenza in più non ricompone niente qui.
 */
data class DestLook(
    val view: FolderView = FolderView.GRID,
    /** Le colonne della griglia. Vedi `Settings.folderColumns`. */
    val columns: Int = 2,
    /** Se sotto la copertina si legge il conto. Vedi `Settings.folderCount`. */
    val counted: Boolean = true,
    /** Se nella lista si legge il conto. Vedi `Settings.listCount`. */
    val listCount: Boolean = true,
    /** Il corpo del testo della lista. Vedi `Settings.listText`. */
    val listText: TextSize = TextSize.NORMAL,
    /** La modalità inclusione vuole la scorciatoia filtrata finché non si sceglie 'Sfoglia...'. */
    val folderMode: FolderMode = FolderMode.EXCLUDED
)

/**
 * Come l'utente guarda le cartelle, per la finestra che le usa come destinazioni.
 *
 * Un `CompositionLocal`, come [LocalAivDepth] e `LocalPadLook`, perché a chiederlo è una finestra,
 * che le impostazioni non le riceve: la catena attraverserebbe griglia, albero e visualizzatore per
 * un dato che non cambia durante un gesto. I valori di serie sono quelli di fabbrica, così una
 * finestra nata fuori dall'albero della composizione si comporta come l'app appena installata.
 */
val LocalDestLook = staticCompositionLocalOf { DestLook() }

/**
 * Se il minuto di 'Mostra nascoste' è acceso mentre si sceglie una destinazione: il prestito vale
 * anche per le destinazioni (voce `dest-nascoste` del giro della `2.02`).
 *
 * Un `CompositionLocal` per la ragione di [LocalDestLook], ma ⚠️ **non `static`**: questo valore
 * cambia due volte per prestito, e uno `staticCompositionLocalOf` ricomporrebbe l'app intera a ogni
 * cambiamento. Con quello normale si ricompone solo chi lo legge.
 */
val LocalPeek = compositionLocalOf { false }

/**
 * Quello che la finestra delle destinazioni carica da sé, in una lettura sola.
 *
 * ⚠️ **Non passa da [DestLook]**, che è uno `staticCompositionLocalOf`: un colore scelto su una
 * cartella ricomporrebbe l'app intera. Questi dati nascono e finiscono con la finestra. Insieme e
 * non in quattro attese, così l'elenco compare già coi suoi colori e le sue copertine.
 */
private data class DestData(
    val folders: List<Folder.Bucket>,
    val covers: Map<Long, Uri>,
    val tints: Map<Long, Int>,
    val colour: FolderColour,
    /**
     * Le cartelle in elenco in prestito: le nascoste, e solo mentre dura il minuto. Si ricava come
     * nella schermata iniziale, e serve al segno del vuoto e all'inchiostro ridotto, che dicono
     * perché quella cartella è in elenco.
     */
    val peeked: Set<String>
)

/**
 * Le cartelle dell'app come destinazioni: un tocco e l'operazione parte.
 *
 * Riusa le due viste della schermata iniziale, `Covers` e `Rows`, quindi copertine, conti, corpi e
 * colonne sono quelli scelti dall'utente, e le viste non divergono. Il tocco lungo non nasconde
 * niente: in un selettore di destinazione un gesto che cambia le impostazioni farebbe danni per
 * sbaglio.
 */
@Composable
private fun FolderShortcut(
    @StringRes action: Int,
    look: DestLook,
    onDismiss: () -> Unit,
    onPick: (File) -> Unit,
    onBrowse: () -> Unit,
    /** Vedi `from` su [DestinationDialog]. */
    from: Set<String>
) {
    val context = LocalContext.current
    val peekNow = LocalPeek.current
    /*
     * Le cartelle si chiedono al MediaStore come fa la schermata iniziale: il visualizzatore e
     * l'albero non ne hanno un elenco in mano. Nella stessa lettura arrivano le copertine scelte a
     * mano (le due viste le vogliono come parametro obbligatorio, così non si dimenticano), le
     * tinte e il colore, così l'elenco compare già completo.
     * ⚠️ **Il prestito si fotografa all'apertura**: il minuto scade da sé, e un elenco che lo
     * seguisse farebbe sparire delle righe da sotto il dito. Quello che cambia dopo l'apertura si
     * vede alla prossima.
     */
    val prestito = remember { peekNow }
    val dati by produceState<DestData?>(null, context) {
        // Una lettura sola e non un flusso: l'elenco è una fotografia presa all'apertura.
        val preferenze = SettingsStore.flow(context).first()
        val buckets = Folder.buckets(context)
        value = DestData(
            folders = destinations(
                buckets = buckets,
                bin = Bin.dir(context).absolutePath,
                hidden = preferenze.hiddenFolders,
                peeking = prestito,
                selection = preferenze.folderSelection
            ),
            covers = FolderCovers.all(context),
            tints = FolderTints.all(context),
            colour = preferenze.folderColour,
            peeked = if (prestito && preferenze.folderMode == FolderMode.EXCLUDED)
                buckets.filter { it.path?.let(preferenze.folderSelection::hidden) == true }
                    .mapNotNull { it.path?.let(::portablePath) }.toSet() else emptySet()
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        // Le stesse della vista ad albero, per le stesse ragioni: vedi [fullWindow].
        properties = fullWindow()
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Glyphs.Material.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back)
                        )
                    }
                    Text(
                        text = stringResource(
                            if (action == R.string.dest_move_here) R.string.dest_pick_move
                            else R.string.dest_pick_copy
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider()

                // Il peso è sul contenitore, come nell'albero: 'Sfoglia...' resta in fondo anche
                // mentre l'elenco si carica.
                Box(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    val elenco = dati?.folders
                    when {
                        elenco == null -> Unit
                        elenco.isEmpty() -> Text(
                            text = stringResource(R.string.dest_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                        /*
                         * Le cartelle si tingono come in casa (risposta `tinta` a `d-dest-tinta`:
                         * una cartella si riconosce allo stesso modo dappertutto), e il colore
                         * arriva dal `produceState` qui sopra, senza toccare un `CompositionLocal`.
                         * Lo stile è quello scelto nelle impostazioni: chi ha scelto 'Nessuno' non
                         * vede niente.
                         */
                        look.view == FolderView.GRID -> Covers(
                            folders = elenco,
                            columns = look.columns,
                            peeked = dati?.peeked.orEmpty(),
                            counted = look.counted,
                            nameStyle = folderNameStyle(look.columns),
                            colour = dati?.colour ?: FolderColour.NONE,
                            tints = dati?.tints.orEmpty(),
                            covers = dati?.covers.orEmpty(),
                            onPick = { bucket -> bucket.path?.let { onPick(File(it)) } },
                            off = from,
                            onHide = { }
                        )
                        else -> Rows(
                            folders = elenco,
                            peeked = dati?.peeked.orEmpty(),
                            counted = look.listCount,
                            size = look.listText,
                            colour = dati?.colour ?: FolderColour.NONE,
                            tints = dati?.tints.orEmpty(),
                            covers = dati?.covers.orEmpty(),
                            onPick = { bucket -> bucket.path?.let { onPick(File(it)) } },
                            off = from,
                            onHide = { }
                        )
                    }
                }

                /*
                 * 'Sfoglia...' apre l'albero esattamente com'era, con la sua conferma, come
                 * chiesto. In basso, perché è la via secondaria e l'utente l'ha immaginato là.
                 */
                TextButton(
                    onClick = onBrowse,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text(stringResource(R.string.dest_browse))
                }
            }
        }
    }
}

/**
 * Le cartelle che si possono usare come destinazione, fra quelle che l'app elenca.
 *
 * - Senza un percorso non c'è dove copiare: la copia scrive su disco, e la colonna del percorso
 *   di una cartella del MediaStore può mancare (vedi `Folder.Bucket.path`).
 * - Il cestino non è una destinazione, come nell'albero. Qui basta un controllo, perché il gesto
 *   per scegliere è uno solo.
 * - Il conto delle cartelle visibili è quello della schermata iniziale, `FolderSelection.visible`:
 *   in modalità inclusione restano le sole incluse; in esclusione le nascoste restano fuori
 *   (richiesta dell'utente), salvo durante il minuto di 'Mostra nascoste' ([peeking]), che si
 *   accende apposta per entrare in una cartella nascosta (voce `dest-nascoste`).
 *
 * Funzione a sé, col percorso del cestino come argomento e senza `Context`, perché il banco la
 * misura senza un telefono: è il punto in cui un errore manda dei file nel posto sbagliato.
 */
internal fun destinations(
    buckets: List<Folder.Bucket>,
    bin: String,
    hidden: Set<String>,
    peeking: Boolean,
    selection: FolderSelection = FolderSelection(hidden = hidden)
): List<Folder.Bucket> =
    buckets.filter { bucket ->
        val path = bucket.path
        path != null && path != bin && !path.startsWith("$bin/") &&
            selection.visible(path, peeking)
    }

/**
 * Il nome di una cartella nuova. La barra, l'unico carattere che Android non ammette in un nome di
 * file, si toglie: una incollata per sbaglio trasformerebbe una cartella in due.
 */
@Composable
internal fun NewFolderDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val clean = text.replace('/', ' ').trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(null),
        properties = loweredWindow(null),
        title = { Text(stringResource(R.string.dest_new)) },
        text = {
            // Lo scorrimento è quello dei due dialoghi gemelli: il perché è nel pannellino
            // dell'estensione, in `RenameDialog.kt`.
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    // La stondatura condivisa [BOX_SHAPE], come gli altri riquadri della finestra.
                    shape = BOX_SHAPE,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            // `.` e `..` non sono nomi ma questa cartella e quella sopra (rilievo D10).
            TextButton(
                onClick = { onCreate(clean) },
                enabled = clean.isNotEmpty() && clean != "." && clean != ".."
            ) { Text(stringResource(R.string.dest_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Una cartella salvata col suo percorso: un `File` il `Bundle` non lo sa scrivere, una stringa sì.
 * Interno per `RotazioneTest`.
 */
internal val FILE_BY_PATH = Saver<File?, String>(
    save = { it?.absolutePath },
    restore = { File(it) }
)

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * Dove mettere le fotografie scelte: prima le cartelle **dell'app**, e a un tocco si va.
 *
 * ⚠️⚠️ **È UN DIALOGO A TUTTO SCHERMO E NON UNA SCHERMATA DEL MODELLO, e la ragione è la
 * SELEZIONE**: quello che si sta per copiare vive in `GridScreen` e se ne va con lei
 * (deliberatamente: uscire da una cartella vuol dire 'lascia stare'). Navigando davvero
 * verso un'altra schermata la selezione sparirebbe proprio mentre la si sta usando, e per
 * evitarlo bisognerebbe spostarla nel modello, cioè farle sopravvivere anche quando non
 * serve. Un dialogo sta **sopra** la griglia, che resta viva sotto.
 *
 * ⚠️⚠️ **DALLA `1.81` SI ENTRA DALLA VISTA NORMALE DELL'APP, E L'ALBERO È DIETRO
 * 'Sfoglia...'** (riscontro del giro della `1.80`, punto F: *'Copia' e 'Sposta' usano sempre
 * la vista ad albero di sistema per decidere la cartella destinazione. Voglio usare la
 * normale vista già attiva per l'uso normale di AIV, e voglio che la copia o lo spostamento
 * avvengano direttamente al tocco della cartella destinazione ... Da qualche parte, comunque
 * ... dovrebbe esserci un tasto 'Sfoglia...' per attivare ESATTAMENTE la modalità attuale*).
 * ⚠️⚠️ **NON ROVESCIA LA SUA RICHIESTA DEL 2026-08-29** (*una vista del filesystem, NON degli
 * album di foto*), e conviene saperlo per non 'correggere' una delle due: l'albero è ancora
 * **tutto** quello che si può scegliere, e le cartelle della galleria sono la **scorciatoia**
 * ai posti più probabili. Il perché l'elenco della galleria da solo sarebbe l'insieme
 * sbagliato sta in testa a `FileTree`, e vale ancora: lascia fuori le cartelle vuote, quelle
 * escluse e quelle che di immagini non ne hanno mai viste. Per quelle c'è 'Sfoglia...'.
 * ⚠️ **La scorciatoia non chiede conferma, ed è la richiesta alla lettera**: *dovrebbe bastare
 * un tap*. Nell'albero la conferma resta, perché là il tocco su una riga **entra** e serve un
 * gesto diverso per dire 'questa'.
 *
 * ⚠️ **[action] dice che cosa succederà, e non è un dettaglio di parole**: copiare e
 * spostare chiedono la stessa cartella e fanno due cose diverse, una innocua e una no.
 * L'ultimo posto in cui si può ancora distinguerle è il tasto che le avvia.
 */
@Composable
fun DestinationDialog(
    @StringRes action: Int,
    onDismiss: () -> Unit,
    onPick: (File) -> Unit
) {
    val context = LocalContext.current
    val look = LocalDestLook.current

    /*
     * ⚠️⚠️ **CHI HA LA VISTA AD ALBERO ENTRA NELL'ALBERO, e non è un caso particolare da
     * ricordare**: la scorciatoia mostra 'la normale vista già attiva', e per chi naviga le
     * cartelle di sistema quella **è** l'albero. Così la richiesta si applica a tutte e tre le
     * viste con una riga sola.
     * ⚠️ **Salvabile**: girando il telefono a metà scelta si tornava in cima, che è il difetto
     * del punto I visto da un'altra parte.
     */
    var sfoglia by rememberSaveable { mutableStateOf(look.view == FolderView.TREE) }
    if (!sfoglia) {
        FolderShortcut(
            action = action,
            look = look,
            onDismiss = onDismiss,
            onPick = onPick,
            onBrowse = { sfoglia = true }
        )
        return
    }
    val roots = remember { FileTree.roots(context) }

    /**
     * Dove si sta guardando, e `null` finché si è all'elenco delle memorie.
     *
     * ⚠️ **La radice non è una cartella qualunque**: sui telefoni con la scheda ce ne sono
     * due, e senza un livello sopra non ci sarebbe modo di passare dall'una all'altra. Su
     * un telefono senza scheda ne ha una sola, e allora si entra dritti dentro.
     */
    var here by remember { mutableStateOf(roots.singleOrNull()) }
    var children by remember { mutableStateOf<List<File>?>(null) }
    var naming by remember { mutableStateOf(false) }

    /*
     * ⚠️⚠️ **IL CESTINO NON È UNA DESTINAZIONE, ed è una richiesta esplicita**: copiarci
     * dentro vorrebbe dire mettere una fotografia in un posto che si svuota, e spostarcela
     * sarebbe eliminarla passando dalla porta di servizio, senza la conferma e senza che
     * l'archivio delle provenienze ne sappia niente. Quindi sparisce dall'elenco.
     * ⚠️ **Il tasto in fondo controlla di nuovo**, e non è una ripetizione inutile: ci si
     * arriva anche entrando nella cartella dell'app da un altro ramo, e in quel caso
     * l'elenco non c'entra. Due controlli per due strade diverse.
     */
    LaunchedEffect(here) {
        children = here?.let { dir -> FileTree.children(dir).filterNot { Bin.holds(context, it) } }
    }

    /** Risale di un livello, e torna `false` quando non c'è più niente sopra. */
    fun up(): Boolean {
        val at = here ?: return false
        // ⚠️ Il confronto è sul PERCORSO e non sull'oggetto: due `File` costruiti in due
        // modi diversi non sono uguali fra loro nemmeno quando indicano la stessa cosa.
        if (roots.any { it.absolutePath == at.absolutePath }) {
            // Con una memoria sola non esiste un elenco a cui tornare: si esce.
            if (roots.size <= 1) return false
            here = null
            return true
        }
        here = at.parentFile ?: return false
        return true
    }

    Dialog(
        onDismissRequest = onDismiss,
        // ⚠️⚠️ **LE DUE RIGHE VIVONO IN [fullWindow], DALLA `1.81`**: qui dentro c'è un elenco da
        // scorrere (senza, il dialogo resta una fessura in mezzo allo schermo) **e** un
        // `safeDrawingPadding()`, che fino alla `1.80` lavorava su rientri che il decoro aveva
        // già consumato, cioè aggiungeva un margine due volte. Il perché per esteso è là.
        properties = fullWindow()
    ) {
        BackHandler { if (!up()) onDismiss() }

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (!up()) onDismiss() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                        // ⚠️ Il percorso intero sotto il nome, e non al suo posto: due
                        // cartelle possono chiamarsi uguale, ma un percorso da settanta
                        // caratteri come titolo non si legge.
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
                    if (here != null) {
                        IconButton(onClick = { naming = true }) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = stringResource(R.string.dest_new)
                            )
                        }
                    }
                }

                HorizontalDivider()

                val listed = if (here == null) roots else children
                // ⚠️ Il peso sta sul contenitore e non sull'elenco: così il tasto in fondo
                // resta in fondo anche quando la cartella è vuota, invece di saltare a
                // metà schermo.
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { here = dir }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(text = dir.name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                }

                // ⚠️⚠️ **SI SCEGLIE LA CARTELLA IN CUI SI È, non una toccata nell'elenco**:
                // il tocco su una riga ENTRA, e deve farlo, o non si potrebbe mai arrivare
                // in fondo a un ramo. Le due cose sono gesti diversi apposta, e il tasto
                // dice quale cartella prenderebbe.
                here?.let { dir ->
                    Button(
                        onClick = { onPick(dir) },
                        enabled = !Bin.holds(context, dir),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(stringResource(action, dir.name))
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
                    val made = File(parent, name)
                    if (runCatching { made.mkdirs() }.getOrDefault(false)) here = made
                }
            )
        }
    }
}

/**
 * Come l'utente guarda le cartelle nell'app, cioè quello che serve per rifarne la vista.
 *
 * ⚠️⚠️ **ARRIVA DA CHI CHIAMA E NON SI LEGGE QUI**: questo dialogo non ha le impostazioni in
 * mano, e le tre schermate che lo aprono sì. È la stessa scelta dei campi delle informazioni
 * in `FileJobDialogs`, e per la stessa ragione: un file che legge da sé un'impostazione
 * diventa il secondo posto in cui quella scelta vive.
 * ⚠️ **Porta i cinque valori delle due viste e non l'oggetto intero delle impostazioni**: così
 * si vede da fuori che cosa cambia il disegno di questa finestra, e una preferenza in più non
 * ricompone niente qui.
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
    val listText: TextSize = TextSize.NORMAL
)

/**
 * Come l'utente guarda le cartelle, per la finestra che le usa come destinazioni.
 *
 * ⚠️ **Un `CompositionLocal` per la stessa ragione di [LocalAivDepth] e di `LocalPadLook`**:
 * a chiederlo è una **finestra**, e le finestre le impostazioni non le ricevono. La catena per
 * portarci cinque valori attraverserebbe la griglia, l'albero e il visualizzatore, cioè tre
 * schermate per un dato che non cambia mai durante un gesto.
 * ⚠️ **I valori di serie sono quelli di fabbrica delle impostazioni**, quindi una finestra che
 * nascesse fuori dall'albero della composizione si comporta come l'app appena installata invece
 * di mostrare un elenco vuoto.
 */
val LocalDestLook = staticCompositionLocalOf { DestLook() }

/**
 * Se il minuto di **'Mostra nascoste'** è acceso mentre si sceglie una destinazione.
 *
 * ⚠️⚠️ **DALLA `2.03`, ED È IL SUO RISCONTRO** (giro della `2.02`, voce `dest-nascoste`
 * accettabile: *deve valere anche per le destinazioni*). La `2.02` aveva letto la sua parentesi
 * al contrario, e la voce di collaudo gli chiedeva proprio questo: fino a lei una cartella in
 * prestito compariva in casa e non fra le destinazioni, cioè il prestito valeva a metà.
 * ⚠️ **Un `CompositionLocal` per la ragione di [LocalDestLook]**: a chiederlo è una finestra, e
 * il prestito vive nel modello della schermata iniziale. Passarlo come argomento vorrebbe dire
 * quattro livelli (la griglia, i dialoghi dei file, questa finestra, la scorciatoia) per un dato
 * che nessuno di loro guarda.
 * ⚠️⚠️ **MA NON È `staticCompositionLocalOf` come lui, ed è la differenza che conta**: questo
 * valore **cambia** due volte per prestito, e uno static local ricompone l'app intera a ogni
 * cambiamento. Con quello normale si ricompone chi lo legge, che è questa finestra e nessun
 * altro.
 */
val LocalPeek = compositionLocalOf { false }

/**
 * Quello che la finestra delle destinazioni carica da sé, in una lettura sola.
 *
 * ⚠️⚠️ **NON PASSA DA [DestLook], ED È LA DIFFERENZA CHE CONTA**: quello è uno
 * `staticCompositionLocalOf`, quindi ogni suo cambiamento ricompone l'app intera, e per un
 * colore scelto su una cartella sarebbe un prezzo assurdo. Questi dati invece nascono e muoiono
 * con la finestra, che è l'unica a guardarli.
 * ⚠️ **Insieme e non in quattro attese**: l'elenco compare già coi suoi colori e le sue
 * copertine, mentre con letture separate ci sarebbe un tratto in cui si vedono le cartelle
 * vestite da un'altra parte.
 */
private data class DestData(
    val folders: List<Folder.Bucket>,
    val covers: Map<Long, Uri>,
    val tints: Map<Long, Int>,
    val colour: FolderColour,
    /**
     * Le cartelle in elenco **in prestito**: le nascoste, e solo mentre dura il minuto.
     *
     * ⚠️ **Si ricava come nella schermata iniziale e non si tiene a parte**: è l'insieme delle
     * nascoste col prestito acceso e niente col prestito spento, cioè lo stesso conto che
     * `FolderScreen` fa su `prestate`. Serve al segno del vuoto e all'inchiostro ridotto, che
     * dicono perché quella cartella è in elenco.
     */
    val peeked: Set<String>
)

/**
 * Le cartelle dell'app come **destinazioni**: un tocco e l'operazione parte.
 *
 * ⚠️⚠️ **RIUSA LE DUE VISTE DELLA SCHERMATA INIZIALE, non ne disegna una terza**: `Covers` e
 * `Rows` sono le stesse funzioni che quella schermata chiama, quindi le copertine, i conti, i
 * corpi e il numero di colonne sono quelli che l'utente ha scelto. Una copia qui dentro
 * avrebbe fatto due viste che divergono al primo ritocco, che è la trappola scritta in
 * `rules/Roccobot.md` § '🪶 Come si mantiene un file di regole'.
 * ⚠️ **Il tocco lungo non nasconde niente**: nella schermata iniziale quel gesto esclude una
 * cartella dall'elenco, e qui si sta scegliendo dove mettere dei file. Un gesto che in un
 * selettore cambia le impostazioni è un modo di fare danni per sbaglio.
 */
@Composable
private fun FolderShortcut(
    @StringRes action: Int,
    look: DestLook,
    onDismiss: () -> Unit,
    onPick: (File) -> Unit,
    onBrowse: () -> Unit
) {
    val context = LocalContext.current
    val peekNow = LocalPeek.current
    /*
     * ⚠️ **Le cartelle si chiedono al MediaStore come fa la schermata iniziale**, e non si
     * ricevono da chi chiama: il visualizzatore e l'albero non ne hanno un elenco in mano, e
     * farglielo caricare per passarlo qui vorrebbe dire la stessa query in tre posti.
     */
    /*
     * ⚠️⚠️ **LE COPERTINE SCELTE A MANO SI CARICANO QUI, dalla `2.01`, E PRIMA NON ARRIVAVANO**
     * (sua segnalazione, 2026-09-09: *quando copio o sposto e devo selezionare la destinazione,
     * le cartelle appaiono con la loro copertina originale, non con la personalizzata*). Le due
     * viste hanno il parametro da sempre, ma con un valore di serie vuoto: questa finestra lo
     * ereditava in silenzio, e `coverIn` cadeva sempre sulla copertina predefinita. Adesso quel
     * parametro è obbligatorio, quindi il difetto non si può più rifare per omissione.
     * ⚠️ **Si caricano nello STESSO `produceState` delle cartelle**, e non in un secondo: così
     * l'elenco compare già con le copertine giuste, mentre con due attese ci sarebbe un tratto
     * in cui si vedono quelle predefinite.
     */
    /*
     * ⚠️⚠️ **IL PRESTITO SI FOTOGRAFA ALL'APERTURA, e il `remember` è la riga che lo dice**: il
     * minuto scade da sé, e un elenco che lo seguisse farebbe sparire delle righe da sotto il
     * dito mentre si sceglie dove mettere un file. È la stessa scelta dell'elenco qui sotto, che
     * pure è una fotografia: quello che cambia dopo l'apertura si guarda alla prossima.
     */
    val prestito = remember { peekNow }
    val dati by produceState<DestData?>(null, context) {
        // ⚠️ **Una lettura sola e non un flusso in ascolto**, come per le cartelle: qui
        // l'elenco è una fotografia presa all'apertura, e né le esclusioni né il colore
        // cambiano mentre si sceglie dove mettere un file.
        val preferenze = SettingsStore.flow(context).first()
        value = DestData(
            folders = destinations(
                buckets = Folder.buckets(context),
                bin = Bin.dir(context).absolutePath,
                hidden = preferenze.hiddenFolders,
                peeking = prestito
            ),
            covers = FolderCovers.all(context),
            tints = FolderTints.all(context),
            colour = preferenze.folderColour,
            peeked = if (prestito) preferenze.hiddenFolders else emptySet()
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        // ⚠️ Le stesse della vista ad albero, e per le stesse due ragioni: vedi [fullWindow].
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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

                // ⚠️ Il peso sta sul contenitore, come nell'albero: così 'Sfoglia...' resta in
                // fondo anche mentre l'elenco si carica, invece di saltare a metà schermo.
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
                         * ⚠️⚠️ **QUI LE CARTELLE SI TINGONO, DALLA `2.02`, E FINO ALLA `2.01`
                         * NO** (risposta `tinta` a `d-dest-tinta`, giro della `2.01`: *una
                         * cartella si riconosce allo stesso modo dappertutto*). La scelta di
                         * prima era dichiarata e aveva due ragioni: questa finestra non è
                         * l'elenco di casa, e le tinte sarebbero passate da [DestLook], cioè uno
                         * `staticCompositionLocalOf`, ricomponendo l'app intera a ogni colore
                         * scelto.
                         * ⚠️ **La seconda ragione è caduta col lavoro della `2.01`**: i dati di
                         * una cartella si caricano nel `produceState` qui sopra, quindi il
                         * colore arriva senza toccare nessun `CompositionLocal`. La prima l'ha
                         * decisa lui, ed è la risposta.
                         * ⚠️ **Lo STILE è quello che ha scelto nelle impostazioni**, non uno
                         * fissato qui: se in casa vede i nomi colorati, li vede colorati anche
                         * qui, e chi ha scelto 'Nessuno' non vede niente. Un valore scritto a
                         * mano avrebbe fatto due impostazioni per la stessa domanda.
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
                            onHide = { }
                        )
                    }
                }

                /*
                 * ⚠️⚠️ **'Sfoglia...' PORTA ALL'ALBERO ESATTAMENTE COM'ERA, ed è la sua
                 * richiesta alla lettera**: *per attivare ESATTAMENTE la modalità attuale
                 * (albero delle cartelle così com'è, con la conferma così com'è)*. Quindi non è
                 * una vista nuova: è la stessa funzione di prima, che da qui in avanti si apre
                 * su richiesta.
                 * ⚠️ **In basso perché lo ha immaginato là** (*ho immaginato un in basso*), e
                 * perché è la via secondaria: la prima cosa che si vede sono le cartelle.
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
 * ⚠️⚠️ **SENZA UN PERCORSO NON C'È NIENTE DOVE COPIARE, e per questo il filtro esiste**: una
 * copia scrive su **disco**, e la colonna che dà il percorso di una cartella del MediaStore
 * può mancare (vedi `Folder.Bucket.path`). Una riga toccabile che non porta da nessuna parte è
 * peggio di una riga che non c'è.
 * ⚠️⚠️ **E IL CESTINO NON È UNA DESTINAZIONE**, come nell'albero e per la stessa ragione:
 * copiarci dentro vuol dire mettere un file in un posto che si svuota, e spostarcelo è
 * eliminarlo passando dalla porta di servizio. Là il controllo è doppio (l'elenco e il tasto),
 * qui basta una volta, perché di gesti per scegliere ce n'è uno solo.
 * ⚠️⚠️ **E NEMMENO UNA CARTELLA NASCOSTA, DALLA `2.02`** (sua richiesta, 2026-09-09: *le
 * cartelle nascoste devono rimanere nascoste anche quando si copiano/spostano file (se serve le
 * rendo visibili di volta in volta)*). Fino alla `2.01` l'esclusione valeva per la schermata
 * iniziale e non per questa finestra, quindi una cartella tolta dall'elenco di casa ricompariva
 * appena si toccava 'Copia'.
 * ⚠️ **Il filtro vive QUI e non nella finestra**, cioè accanto a quello del cestino: sono la
 * stessa domanda (*questa cartella può essere una destinazione?*), e una condizione scritta nel
 * corpo della finestra sarebbe fuori da quello che il banco misura.
 * ⚠️⚠️ **MA IL MINUTO DI 'Mostra nascoste' APRE UN'ECCEZIONE, DALLA `2.03`, ED È IL SUO
 * RISCONTRO** (giro della `2.02`, voce `dest-nascoste` accettabile: *deve valere anche per le
 * destinazioni*). La `2.02` aveva letto la sua parentesi al contrario, e la nota di allora
 * diceva che un elenco legato a un conto alla rovescia acceso altrove sarebbe imprevedibile:
 * quello che quel ragionamento non guardava è che il prestito **si accende apposta per entrare
 * in una cartella nascosta**, quindi copiarci dentro è la cosa che si vuole fare mentre dura.
 * ⚠️ **Il prestito è un argomento e non un secondo insieme**: chi chiama passa le nascoste come
 * prima e dice se il minuto è acceso, che è lo stesso conto della schermata iniziale
 * (`filterNot { !peeking && ... }`). Sottrarre le prestate da [hidden] avrebbe dato lo stesso
 * elenco e avrebbe reso il prestito invisibile a chi legge questa firma.
 * ⚠️ **Funzione a sé perché il banco la misura**: è la sola parte di questa finestra che si può
 * provare senza uno schermo, ed è quella in cui un errore manda dei file in un posto sbagliato.
 * ⚠️ **Il percorso del cestino arriva come argomento e non si chiede a `Bin`**: quello vuole un
 * `Context`, e con lui questa funzione smetterebbe di essere misurabile senza un telefono.
 */
internal fun destinations(
    buckets: List<Folder.Bucket>,
    bin: String,
    hidden: Set<String>,
    peeking: Boolean
): List<Folder.Bucket> =
    buckets.filter { bucket ->
        val path = bucket.path
        path != null && path != bin && !path.startsWith("$bin/") &&
            (peeking || !bucket.isHidden(hidden))
    }

/**
 * Il nome di una cartella nuova.
 *
 * ⚠️ **I nomi si ripuliscono invece di essere rifiutati**: la barra è l'unico carattere
 * che su Android non può stare in un nome di file, e uno incollato per sbaglio
 * trasformerebbe una cartella in due. Toglierlo è più utile di un messaggio d'errore.
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
            // ⚠️ Lo scorrimento serve al tetto della `1.62`, come nei due dialoghi gemelli:
            // il perché per esteso vive nel pannellino dell'estensione, in `RenameDialog.kt`.
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    // ⚠️ **La stondatura condivisa, dalla `1.81`**: era l'unico dei cinque
                    // campi della superficie a portare quella di Material (`extraSmall`, cioè
                    // 4) invece degli 8 di [BOX_SHAPE], e la costante è nata proprio perché i
                    // riquadri di una stessa finestra divergevano.
                    shape = BOX_SHAPE,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(clean) },
                enabled = clean.isNotEmpty()
            ) { Text(stringResource(R.string.dest_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

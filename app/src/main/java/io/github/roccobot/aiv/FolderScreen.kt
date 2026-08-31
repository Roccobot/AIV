package io.github.roccobot.aiv

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Le cartelle di immagini del telefono: **la schermata iniziale dell'app**, dalla `0.41`.
 *
 * ⚠️⚠️ **PRIMA C'ERA UNA SCHERMATA CHE CHIEDEVA DA DOVE COMINCIARE, con cinque tasti, ed
 * è sparita** (decisione dell'utente, 2026-08-29: *non ha più senso la scelta multipla
 * all'avvio, la risolviamo in modo smart*). Chi apre un visualizzatore di immagini vuole
 * vedere immagini: la domanda 'da dove?' la si risponde da soli, e le vie che non sono la
 * risposta di quasi sempre stanno dietro un tastino.
 *
 * ⚠️ **Serve a DUE cose e la seconda non è un doppione**: da qui si apre una cartella
 * adesso, e da qui si sceglie quella da aprire all'avvio. È la stessa domanda ('quale
 * cartella?'), quindi è la stessa schermata: due elenchi identici che divergono al primo
 * ritocco sono il modo classico di far invecchiare una funzione. ⚠️ Nella seconda veste
 * il tastino **non c'è** e il gesto Indietro **sì**, che è l'esatto contrario della
 * prima: si sta rispondendo a una domanda, non girando per l'app.
 *
 * ⚠️ Le copertine e l'elenco sono **due viste della stessa cosa** e non una migliore
 * dell'altra: vedi `FolderView`.
 */
@Composable
fun FolderScreen(
    view: FolderView,
    /**
     * Quante colonne mostrano le copertine. Vedi `FOLDER_COLUMNS` in `Settings.kt`.
     *
     * ⚠️ **Dalla `0.60` decide anche quante RIGHE si vedono**, perché sono tante quante le
     * colonne: il numero governa il quadrato di copertine, non solo la loro larghezza. Vedi
     * [coverHeader].
     */
    columns: Int,
    /**
     * Se sotto la copertina si vede il conto delle immagini. Vedi `Settings.folderCount`.
     *
     * ⚠️ **Entra nel conto dell'altezza di una riga**: spegnendolo la riga si accorcia e il
     * frontespizio cresce di altrettanto, quindi il numero di righe visibili resta quello
     * della tabella. Vedi [coverHeader].
     */
    counted: Boolean,
    /** I percorsi da non mostrare. Vedi `Settings.hiddenFolders`. */
    hidden: Set<String>,
    onHide: (Folder.Bucket) -> Unit,
    recents: List<RecentImage>,
    onPick: (Folder.Bucket) -> Unit,
    onOpen: (Uri) -> Unit,
    onView: (FolderView) -> Unit,
    onForget: () -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    onBin: () -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(Folder.granted(context)) }
    var folders by remember { mutableStateOf<List<Folder.Bucket>?>(null) }

    /**
     * La cartella che si sta per nascondere, e `null` quando non se ne sta nascondendo
     * nessuna.
     *
     * ⚠️⚠️ **IL TOCCO LUNGO CHIEDE, NON FA**, e non è pignoleria: su questa schermata il
     * tocco lungo non esisteva, quindi il primo che capita è quasi sempre un tocco
     * normale venuto male. Far sparire una cartella per sbaglio, senza dire dove è
     * finita, è il modo di far credere che l'app abbia perso delle foto.
     */
    var hiding by remember { mutableStateOf<Folder.Bucket?>(null) }

    // ⚠️ Al ritorno dalla pagina di sistema non arriva nessun esito, perché non è un
    // dialogo: si RICHIEDE allo stato delle cose, come fa il viewer.
    val fromSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { granted = Folder.granted(context) }

    // ⚠️ Il filtro sta QUI e non dentro `Folder.buckets`, che resta un elenco puro: le
    // impostazioni sono una faccenda della schermata, e una funzione che legge il
    // MediaStore non deve sapere che cosa l'utente ha deciso di non guardare.
    // ⚠️ La chiave comprende `hidden`, o nascondere una cartella non si vedrebbe finché
    // non si esce e si rientra.
    LaunchedEffect(granted, hidden) {
        folders = if (granted) Folder.buckets(context).filterNot { it.isHidden(hidden) }
        else emptyList()
    }

    // La veste 'casa' e quella 'scegli la cartella d'avvio' si distinguono da qui in giù:
    // la prima porta il frontespizio e il tastino, la seconda la freccia Indietro.
    val home = onBack == null

    BoxWithConstraints(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        val density = LocalDensity.current

        // ⚠️⚠️ **Le due righe sotto la copertina si MISURANO, non si stimano**: entrano nel
        // conto dell'altezza di una riga di griglia (vedi [coverHeader]), e un'altezza
        // presunta si porta dietro il corpo del carattere di sistema, che l'utente cambia
        // dalle impostazioni di Android. Il misuratore usa i font veri e la densità vera,
        // che è la sola misura che questo repo accetta.
        val measurer = rememberTextMeasurer()
        val nameStyle = folderNameStyle(columns)
        val countStyle = MaterialTheme.typography.bodySmall
        /*
         * ⚠️⚠️ **IL CONTO SI SOMMA SOLO SE SI VEDE**, e senza questo `if` spegnere la
         * dicitura lascerebbe un buco alto una riga di testo sotto ogni copertina.
         * ⚠️⚠️ **E LA RIGA DEL CONTO È ALTA quanto il TESTO o quanto l'ICONA, il maggiore**:
         * l'icona misura [COUNT_ICON] in dp, il testo `bodySmall` è alto 16sp, e i due si
         * scavalcano perché **sp e dp non scalano insieme**. Con il corpo di sistema al
         * minimo 16sp scende sotto i 15dp e a decidere l'altezza diventa l'icona: darla per
         * vinta al testo vorrebbe dire sbagliare il conto delle righe proprio sui telefoni
         * di chi rimpicciolisce i caratteri.
         */
        val captionPx = remember(measurer, nameStyle, countStyle, counted, density) {
            /*
             * ⚠️⚠️ **IL NOME SI MISURA AL CASO PEGGIORE, cioè a [NAME_LINES] righe, dalla
             * 0.77**: da quando è ammesso un a capo, una riga di griglia è alta due righe di
             * testo **se un nome va a capo**, e quale nome lo faccia dipende dalla cartella,
             * dallo schermo e dal corpo di sistema. Misurando una riga sola, il primo nome
             * lungo farebbe sforare la griglia e in fondo alla schermata ricomparirebbe il
             * pezzo di riga che questo conto esiste per tenere fuori (il difetto della
             * `0.68`).
             * ⚠️ **Il prezzo è dichiarato**: quando nessun nome va a capo, la griglia finisce
             * qualche decina di dp più in alto del previsto. Quello spazio cade **sotto**
             * l'ultima riga, cioè dove ci sono il tastino e la sua sfumatura, e là non
             * disturba nessuno.
             */
            val sample = List(NAME_LINES) { CAPTION_SAMPLE }.joinToString("\n")
            val name = measurer.measure(sample, nameStyle, maxLines = NAME_LINES).size.height
            val count = measurer.measure(CAPTION_SAMPLE, countStyle, maxLines = 1).size.height
            val icon = with(density) { COUNT_ICON.toPx() }
            name + if (counted) maxOf(count, icon.toInt()) else 0
        }

        val headerMax = when {
            !home -> 0.dp
            view == FolderView.GRID ->
                coverHeader(columns, maxWidth, maxHeight, captionPx, counted, density)
            // ⚠️ L'elenco non ha colonne, quindi non ha niente da far quadrare: là resta la
            // frazione fissa, che è la regola di prima. Vedi [HEADER_SHARE].
            else -> maxHeight * HEADER_SHARE
        }
        val headerPx = with(density) { headerMax.toPx() }

        /**
         * Quanti pixel del frontespizio sono già stati chiusi, da 0 a tutto.
         *
         * ⚠️ La chiave è la misura: ruotando il telefono l'altezza cambia, e un valore
         * di chiusura vecchio non vorrebbe più dire niente. Riaprirlo alla rotazione è
         * anche la cosa giusta da vedere.
         */
        var shut by remember(headerPx) { mutableFloatStateOf(0f) }

        /**
         * ⚠️⚠️ **IL FRONTESPIZIO SI CHIUDE PRIMA CHE L'ELENCO SCORRA, ed è per questo che
         * funziona anche con DUE cartelle**: il trascinamento verso l'alto viene
         * intercettato **prima** (`onPreScroll`) e speso tutto qui, quindi l'elenco non ha
         * bisogno di avere niente da scorrere. Verificato sul sorgente di Compose e non
         * supposto: `ScrollingLogic.performScroll` chiama `dispatchPreScroll` prima di
         * consumare, e l'avvio del trascinamento dipende dal **tipo di puntatore**
         * (`canDrag`) e non dal fatto che ci sia spazio da scorrere. Senza questo fatto
         * avrei dovuto gonfiare l'elenco con spazio finto in fondo.
         * ⚠️ E si riapre dall'altra parte con `onPostScroll`: quello arriva solo quando
         * l'elenco è già in cima e ha avanzato del movimento, che è esattamente la
         * condizione in cui il frontespizio deve tornare.
         */
        val paging = remember(headerPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y >= 0f) return Offset.Zero
                    val take = (-available.y).coerceAtMost(headerPx - shut)
                    shut += take
                    return Offset(0f, -take)
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (available.y <= 0f) return Offset.Zero
                    val give = available.y.coerceAtMost(shut)
                    shut -= give
                    return Offset(0f, give)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(paging)
                // ⚠️ Il margine laterale è 12 e non 20 perché è quello che permette **due**
                // colonne di copertine su uno schermo da 360dp. Il conto sta in `FOLDER_CELL`.
                // ⚠️ Da qui in poi è una costante e non un numero scritto due volte:
                // [coverHeader] deve togliere **esattamente** questo margine, o il conto
                // delle righe visibili sbaglia di 24dp.
                .padding(SCREEN_PAD)
        ) {
            if (home) {
                // ⚠️ L'icona si stringe se il frontespizio è basso, e i casi bassi sono
                // due: l'ORIZZONTALE, dove non resta niente, e le QUATTRO COLONNE, dove
                // il quadrato di copertine lascia poco più di 180dp, cioè meno di quanto
                // occupano icona e righe. Senza questo la tela verrebbe tagliata sopra e
                // sotto invece di stare dentro.
                Header(headerPx, minOf(HEADER_ICON, headerMax * 0.5f)) { shut }
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
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
            Spacer(Modifier.height(HEADER_GAP))

            when {
                !granted -> Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.folders_permission),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    FilledTonalButton(onClick = {
                        // Il ripiego sull'elenco generale non è un lusso: la pagina mirata
                        // all'app manca su qualche sistema.
                        Folder.settingsIntents(context).any {
                            runCatching { fromSettings.launch(it) }.isSuccess
                        }
                    }) { Text(stringResource(R.string.folders_grant)) }
                }

                folders == null -> CircularProgressIndicator(
                    Modifier.padding(top = 24.dp).size(28.dp).align(Alignment.CenterHorizontally)
                )

                folders!!.isEmpty() -> Text(
                    text = stringResource(R.string.folders_none),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 24.dp)
                )

                view == FolderView.GRID ->
                    Covers(folders!!, columns, counted, nameStyle, onPick) { hiding = it }
                else -> Rows(folders!!, onPick) { hiding = it }
            }
        }

        // ⚠️ Il tastino manca quando si sta scegliendo la cartella dell'avvio: là dentro
        // ci sono le impostazioni, da cui si è arrivati, e un giro chiuso non serve a
        // nessuno.
        if (home) {
            /*
             * ⚠️⚠️ **LA SFUMATURA CHE INGHIOTTE QUELLO CHE STA SOTTO, dalla 0.77** (richiesta
             * dell'utente: *dalla coordinata Y in cui comincia il tastino, una piccola
             * sfumatura verso il colore di fondo del tema, che inghiotte ciò che sta giù
             * abbastanza velocemente, in modo che dia poco fastidio, e che allo stesso tempo
             * suggerisce che la griglia si scorre*). Al riposo non copre niente, perché
             * [coverHeader] tiene le cartelle sopra di lei; serve quando si scorre, dove
             * l'alternativa era una riga tagliata a metà dal bordo dello schermo.
             * ⚠️⚠️ **STA PRIMA DEL TASTINO E NON DOPO**: in un `Box` l'ultimo figlio sta
             * sopra, quindi scritta dopo dipingerebbe **sul** tastino invece che sotto.
             * ⚠️⚠️ **NON RUBA I TOCCHI, e non è una speranza**: Compose fa la prova del tocco
             * solo sui nodi che hanno un modificatore di puntatore, e questo ne ha uno solo di
             * disegno. Senza questo fatto servirebbe un `pointerInput` che lascia passare, che
             * è il rimedio a un problema che non c'è.
             * ⚠️ Il colore è `background` e non `surface`: è quello che la `Surface` del tema
             * mette dietro a tutta l'app (vedi `AivTheme`), quindi la sfumatura arriva
             * **esattamente** al fondo su cui sta.
             */
            val ground = MaterialTheme.colorScheme.background
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(FAB_REACH)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            SWALLOW to ground,
                            1f to ground
                        )
                    )
            )
            Hub(
                view = view,
                granted = granted,
                recents = recents,
                onOpen = onOpen,
                onView = onView,
                onForget = onForget,
                onSettings = onSettings,
                onSearch = onSearch,
                onBin = onBin,
                // ⚠️ Costante e non numero: [FAB_REACH] la somma per sapere da dove parte la
                // sfumatura, e [coverHeader] per tenere le cartelle sopra il tastino.
                modifier = Modifier.align(Alignment.BottomEnd).padding(HUB_PAD)
            )
        }
    }

    hiding?.let { bucket ->
        AlertDialog(
            onDismissRequest = { hiding = null },
            title = { Text(stringResource(R.string.hide_folder_title, bucket.name)) },
            // ⚠️ Il testo dice DOVE va a finire, e dirlo qui è metà della funzione: una
            // cartella che sparisce senza che si sappia come riaverla è indistinguibile
            // da una cartella persa.
            text = { Text(stringResource(R.string.hide_folder_desc)) },
            confirmButton = {
                TextButton(onClick = { onHide(bucket); hiding = null }) {
                    Text(stringResource(R.string.hide_folder_do))
                }
            },
            dismissButton = {
                TextButton(onClick = { hiding = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * Se questa cartella è fra quelle che l'utente ha escluso.
 *
 * ⚠️⚠️ **IL CONFRONTO È SUL SEPARATORE, e senza di lui la funzione nasconderebbe cose che
 * nessuno ha escluso**: con un `startsWith` nudo, escludere `.../Foo` toglierebbe anche
 * `.../Foo2` e `.../Foobar`, che sono cartelle diverse con un nome che comincia uguale.
 * Chiedendo la barra dopo, si nasconde `Foo` e tutto quello che sta **dentro** `Foo`, che
 * è quello che vuol dire escludere un percorso.
 * ⚠️ **Una cartella senza percorso non si nasconde mai**: il provider può non servire la
 * colonna, e allora non c'è niente da confrontare. Meglio mostrarne una di troppo che
 * nasconderne una a caso.
 */
private fun Folder.Bucket.isHidden(hidden: Set<String>): Boolean {
    val own = path ?: return false
    return hidden.any { own == it || own.startsWith("$it/") }
}

/**
 * Quanto spazio resta al frontespizio quando la griglia si è presa le sue [startRows]
 * righe.
 *
 * ⚠️⚠️ **LA FORMA NON È PIÙ IL QUADRATO, dalla 0.68**: vedi [startRows], dove sta la
 * ragione. Fino alla 0.67 questo conto usava le colonne anche come numero di righe.
 *
 * ⚠️ **Storia, perché non si torni indietro di due passi**: fino alla `0.59` la griglia si
 * prendeva un **60% fisso** dello schermo, che rispondeva bene a due colonne e male alle
 * altre, perché con le copertine più strette le righe si accorciano e nel 60% ne entravano
 * quattro e mezza invece di tre. La `0.60` l'ha sostituito col quadrato, la `0.68` col
 * conto per colonne.
 *
 * ⚠️⚠️ **IL CONTO USA LE STESSE COSTANTI DEL DISEGNO, e non sono ricopiate**: [SCREEN_PAD]
 * per il margine, [FOLDER_GAP] per il distacco fra le copertine, [CARD_GAP] per quello fra
 * copertina e testo, [HEADER_GAP] per lo stacco sotto il frontespizio. Un numero scritto
 * due volte qui non si vedrebbe subito: la griglia mostrerebbe 3,8 righe invece di 4, che
 * sembra una scelta e non un errore.
 *
 * ⚠️ **Una riga di griglia è: copertina quadrata, distacco, nome, distacco, conto.** La
 * copertina è quadrata perché la scheda la disegna con `aspectRatio(1f)`, quindi la sua
 * altezza **è** la larghezza della cella, e l'altezza delle due righe di testo arriva
 * misurata da chi chiama (`captionPx`).
 *
 * ⚠️ **Si arrotonda la cella per ECCESSO**, e non è pignoleria da un pixel:
 * `LazyVerticalGrid` divide lo spazio fra le colonne e distribuisce i pixel di resto sulle
 * **prime**, quindi in ogni riga c'è una copertina più larga delle altre, ed è lei a
 * decidere l'altezza della riga. Per difetto, a quattro colonne, l'ultima riga resterebbe
 * tagliata di qualche pixel.
 *
 * ⚠️ **Se non ci sta, il frontespizio va a zero e la griglia scorre**: succede in
 * orizzontale, dove le copertine sono larghissime e due righe non entrerebbero comunque.
 * Restituire zero è l'esito onesto, perché lo spazio che manca non si inventa; il
 * frontespizio in quel caso si misura ad altezza nulla e sparisce da sé, e chi scorre non
 * perde niente perché là non c'era niente da chiudere.
 *
 * ⚠️⚠️ **QUI C'ERA UN TETTO AL 50% DELLO SCHERMO, ED ERA IL DIFETTO DELLA 0.68**
 * (riscontro dell'utente: *a volte restano visibili sul bordo inferiore parti di cartelle
 * che dovrebbero essere fuori dalla vista*). Il tetto rompeva l'identità su cui poggia tutto
 * questo conto: il frontespizio deve prendere **esattamente** quello che la tabella gli
 * lascia, o lo spazio che gli viene negato lo riceve la griglia, che lo riempie con un
 * pezzo di riga in più. Misurato prima di toccare il codice, su uno schermo da 360dp: a 3
 * colonne il tetto lasciava in vista **52dp** di una riga su 800dp d'altezza, **77** su 850
 * e **112** su 920, e a 4 colonne **35** su 920. A 2 colonne, che è il valore di fabbrica,
 * il conto tornava: è la ragione per cui il difetto si vedeva *a volte*.
 * ⚠️ **La nota vecchia diceva che sbagliava 'dalla parte giusta'**, cioè mostrando più righe
 * del previsto invece che meno. Era vero e non era il punto: l'utente non ha chiesto almeno
 * N righe, ha chiesto N righe.
 * ⚠️ Resta il solo `coerceAtLeast(0f)`, che serve **in orizzontale**: là il quadrato di
 * copertine chiede più dello schermo, il frontespizio va a zero e la griglia scorre. Lì i
 * pezzi di riga sono corretti, perché si sta scorrendo.
 *
 * ⚠️⚠️ **IL TASTINO ENTRA NEL CONTO DALLA 0.77, e prima non c'era** (riscontro dell'utente:
 * *la vista iniziale va ripensata perché il tastino copre*). Il conto teneva le righe dentro
 * lo schermo ma non sopra il tastino, quindi a due colonne la quarta cartella finiva sotto di
 * lui: le righe ci stavano, e una era coperta. Al posto del margine di sotto si sottrae
 * [BELOW_FAB], che è l'ingombro del tastino più un po' d'aria, ed è la stessa costante che
 * tiene l'ultima cartella scoperta quando si è scorso fino in fondo.
 */
private fun coverHeader(
    columns: Int,
    width: Dp,
    height: Dp,
    captionPx: Int,
    counted: Boolean,
    density: Density
): Dp = with(density) {
    val slots = columns.coerceAtLeast(1)
    val lines = startRows(slots)
    val gapPx = FOLDER_GAP.toPx()
    val roomPx = (width - SCREEN_PAD * 2).toPx() - gapPx * (slots - 1)
    val cellPx = ceil(roomPx / slots).coerceAtLeast(0f)
    // ⚠️ I distacchi della scheda sono UNO IN MENO dei suoi figli: con il conto sono tre
    // (copertina, nome, conto) e i distacchi due, senza il conto sono due e il distacco uno.
    val rowPx = cellPx + CARD_GAP.toPx() * (if (counted) 2 else 1) + captionPx
    val gridPx = rowPx * lines + gapPx * (lines - 1)
    val freePx = (height - SCREEN_PAD - BELOW_FAB - HEADER_GAP).toPx() - gridPx
    freePx.coerceAtLeast(0f).toDp()
}

/**
 * Quante righe di cartelle si vedono all'avvio, per numero di colonne: **2 righe fino a
 * tre colonne, 3 righe da quattro**. Cioè 4 cartelle a 2 colonne, 6 a 3, 12 a 4.
 *
 * ⚠️⚠️ **HA SOSTITUITO IL QUADRATO NELLA 0.68, e la ragione è che una RIGA NON È ALTA
 * QUANTO UNA COPERTINA** (revisione dell'utente sulla `0.60`: *non avevo considerato i
 * testi sotto le cartelle*). Sotto ogni copertina quadrata stanno il nome e il conto, cioè
 * due righe di testo più due distacchi: una riga di griglia è **più alta che larga**, e un
 * quadrato di righe per colonne è quindi più alto che largo di quel tanto moltiplicato per
 * il numero di righe. A quattro colonne il 4x4 chiedeva più dello schermo intero, e il
 * frontespizio andava a zero da sé senza che nessuno lo avesse deciso.
 *
 * ⚠️ **Non è una formula ma una tabella, e lo è apposta**: le colonne ammesse sono tre
 * (`FOLDER_COLUMNS`), e una formula su tre valori è un modo di nascondere una scelta
 * dietro un'aria di generalità. Se un giorno le colonne diventassero cinque, qui si
 * aggiunge una riga e si guarda uno schermo, che è l'unico modo onesto di decidere.
 */
private fun startRows(columns: Int): Int = if (columns <= 3) 2 else 3

/**
 * Il corpo del nome di una cartella, che dipende da quante colonne ci sono.
 *
 * ⚠️⚠️ **A QUATTRO COLONNE SCENDE** (richiesta dell'utente): là la cella è larga meno di un
 * quarto di schermo, cioè 78dp su 360, e a `titleSmall` due parole ci stanno a stento.
 * `labelMedium` toglie due punti e **tiene il peso medio**, che è quello che distingue il nome
 * dal conto sotto: passare a `bodySmall` avrebbe reso i due indistinguibili.
 * ⚠️ **Una funzione e non due posti**: la misura del sottotitolo (`captionPx`, che decide
 * quanto è alta una riga di griglia) e il disegno della scheda devono usare lo **stesso**
 * corpo, o il conto delle righe visibili sbaglia di qualche pixel per riga e in fondo alla
 * schermata ricompare mezza cartella.
 */
@Composable
private fun folderNameStyle(columns: Int): TextStyle =
    if (columns >= NARROW_COLUMNS) MaterialTheme.typography.labelMedium
    else MaterialTheme.typography.titleSmall

/**
 * Il frontespizio dell'app, che si chiude scorrendo.
 *
 * ⚠️⚠️ **NON È SPAZIO DECORATIVO: è spazio messo lì apposta perché ogni cartella sia
 * raggiungibile col pollice** tenendo il telefono a una mano (richiesta dell'utente). Poi
 * si scorre e la vista arriva al 100%, che è la seconda metà della stessa richiesta.
 * ⚠️ **Quanto sia alto NON lo decide lui**: in griglia lo decide [coverHeader], che gli
 * lascia quel che resta dopo il quadrato di copertine, e nell'elenco la frazione fissa
 * [HEADER_SHARE]. Fino alla `0.59` era il 40% in tutti i casi.
 *
 * ⚠️⚠️ **IL FIGLIO SI MISURA SEMPRE ALL'ALTEZZA PIENA e si RITAGLIA, non si schiaccia.**
 * Misurandolo con l'altezza che resta, l'icona verrebbe compressa mentre il frontespizio
 * si chiude, cioè un disegno che si deforma invece di uscire di scena. Qui si misura
 * intero, si dichiara alta quel che resta, e lo si colloca **centrato in quel che
 * resta**: il contenuto sale da sé mentre lo spazio si stringe, ed è la parallasse, non
 * un secondo movimento aggiunto sopra.
 *
 * ⚠️ **Sparisce prima che lo spazio finisca** (l'opacità va col quadrato della frazione
 * aperta): a metà corsa è già al 25%, quindi l'ultimo tratto della chiusura è spazio
 * vuoto che si richiude invece di un titolo che si accartoccia sul bordo.
 *
 * ⚠️ Lo stato si legge dentro `layout` e `graphicsLayer`, cioè in fase di misura e di
 * disegno: il trascinamento non fa ricomporre **niente**, e questa schermata contiene
 * una griglia che non deve rifarsi sessanta volte al secondo.
 */
@Composable
private fun Header(fullPx: Float, icon: Dp, shut: () -> Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .layout { measurable, constraints ->
                val full = fullPx.roundToInt().coerceAtLeast(0)
                val left = (fullPx - shut()).roundToInt().coerceIn(0, full)
                val placeable = measurable.measure(
                    constraints.copy(minHeight = full, maxHeight = full)
                )
                layout(placeable.width, left) { placeable.place(0, -(full - left) / 2) }
            },
        contentAlignment = Alignment.Center
    ) {
        Identity(
            iconSize = icon,
            link = false,
            modifier = Modifier.graphicsLayer {
                val open = if (fullPx > 0f) (1f - shut() / fullPx).coerceIn(0f, 1f) else 0f
                alpha = open * open
            }
        )
    }
}

/**
 * Il tastino quadrato in basso a destra, e tutto quello che c'è dietro.
 *
 * ⚠️⚠️ **È IL MENU DELL'APP, non una scorciatoia**: con la schermata iniziale sparita,
 * impostazioni, indirizzo manuale, selettore di sistema e scelta della vista non hanno
 * più nessun altro posto dove stare. Sta **in basso a destra** perché è l'unico angolo
 * che un pollice raggiunge senza cambiare presa, ed è **piccolo** perché non deve
 * competere con le fotografie: la richiesta dell'utente era *un FAB quadrato piccolo*.
 *
 * ⚠️⚠️ **E il selettore di sistema è QUI dentro anche per una ragione che non è di
 * comodo**: è l'unica via per aprire un'immagine **senza il permesso** sui file. Chi
 * quel permesso non lo concede vede una schermata delle cartelle vuota, e senza questa
 * voce l'app non gli servirebbe più a niente.
 * ⚠️⚠️ **Dalla `0.74` quella voce compare SE E SOLO SE il permesso manca** (decisione
 * dell'utente), e il bivio scioglie una tensione vera invece di scegliere un lato: col
 * permesso concesso il selettore era una seconda strada per una cosa che le cartelle già
 * fanno, cioè rumore; senza permesso è l'unica strada. La stessa voce, quindi, era
 * ridondante e indispensabile a seconda dello stato dell'app, e il menu ora lo riflette.
 */
@Composable
private fun Hub(
    view: FolderView,
    /**
     * Se l'app ha il permesso sui file, cioè se le cartelle si possono leggere.
     *
     * ⚠️ **Arriva da fuori e non si rilegge qui**: chi chiama ce l'ha già, e lo tiene
     * aggiornato al ritorno dalla pagina di sistema. Un secondo `Folder.granted` in questo
     * composabile darebbe la risposta giusta al primo disegno e poi resterebbe fermo,
     * perché niente lo farebbe ricomporre.
     */
    granted: Boolean,
    recents: List<RecentImage>,
    onOpen: (Uri) -> Unit,
    onView: (FolderView) -> Unit,
    onForget: () -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    onBin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    var asking by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { picked -> picked?.let(onOpen) }

    Box(modifier = modifier) {
        SmallFloatingActionButton(
            onClick = { open = true },
            // Quadrato con gli angoli appena smussati, come chiesto: il tondo pieno
            // griderebbe 'azione principale', e qui l'azione principale sono le cartelle.
            shape = RoundedCornerShape(FAB_CORNER)
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = stringResource(R.string.hub_open)
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            // ⚠️⚠️ **LA VOCE NOMINA LA VISTA CHE SI OTTIENE, non quella in cui si è**, ed
            // è la cosa da non rovesciare quando si riscrive l'etichetta: una riga di menu
            // è una richiesta, non un indicatore di stato, quindi in griglia si legge
            // 'Visualizzazione lista'. Chi la leggesse come stato la invertirebbe, e da
            // quel momento il menu direbbe il contrario di quello che fa.
            // ⚠️ Le due etichette vanno cambiate INSIEME e nella stessa forma: si vedono
            // una per volta, quindi due registri diversi non si notano subito e restano.
            // Erano 'Vedile come elenco' e 'Vedile come copertine' fino alla 0.46,
            // quando l'utente le ha volute al nominale.
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (view == FolderView.GRID) R.string.hub_view_list
                            else R.string.hub_view_grid
                        )
                    )
                },
                onClick = {
                    open = false
                    onView(if (view == FolderView.GRID) FolderView.LIST else FolderView.GRID)
                }
            )

            HorizontalDivider()

            // ⚠️ Sta in cima al gruppo delle azioni, prima delle tre vie che aprono
            // qualcosa: cercare è la domanda che si fa più spesso quando non si sa già
            // dove andare, ed è il caso in cui una persona apre questo menu.
            DropdownMenuItem(
                text = { Text(stringResource(R.string.hub_search)) },
                onClick = { open = false; onSearch() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.hub_url)) },
                onClick = { open = false; asking = true }
            )
            /*
             * ⚠️⚠️ **COMPARE SE E SOLO SE IL PERMESSO MANCA, dalla 0.74** (decisione
             * dell'utente, 2026-08-31: *facciamo un bivio, lo mostriamo se e solo se l'app
             * non ha il permesso di accedere alla memoria*). La richiesta di partenza era di
             * toglierla del tutto, *a meno che non mi sia perso qualcosa*: quel qualcosa era
             * che questa voce è l'**unica via per aprire un'immagine senza quel permesso**,
             * ed è scritto sopra questo composabile.
             * ⚠️ **Il bivio la rende utile invece di ridondante**, che è la ragione per cui
             * dava fastidio: col permesso concesso le cartelle ci sono, e allora il selettore
             * di sistema è una seconda strada per la stessa cosa; senza permesso è l'unica
             * strada che c'è.
             * ⚠️ **Sparisce da sé quando il permesso arriva**, senza uscire e rientrare: lo
             * stato di `granted` si rinfresca al ritorno dalla pagina di sistema (vedi
             * `fromSettings` in chi chiama), quindi il menu si ricompone.
             */
            if (!granted) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.hub_pick)) },
                    onClick = {
                        open = false
                        picker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                )
            }
            // ⚠️⚠️ **IL CESTINO SI RAGGIUNGE SOLO DA QUI, ed è per costruzione**: le sue
            // fotografie stanno nella cartella dell'app, dove il MediaStore non guarda,
            // quindi non compaiono nell'elenco delle cartelle e non possono comparirci.
            // Senza questa voce sarebbero irraggiungibili, cioè cancellate.
            // ⚠️ Sta in fondo al gruppo di quelle che aprono qualcosa: è un posto dove si
            // va, come una cartella, ma è il meno frequentato dei quattro.
            DropdownMenuItem(
                text = { Text(stringResource(R.string.bin_title)) },
                onClick = { open = false; onBin() }
            )

            HorizontalDivider()

            DropdownMenuItem(
                text = { Text(stringResource(R.string.hub_settings)) },
                onClick = { open = false; onSettings() }
            )
        }
    }

    if (asking) {
        AddressDialog(
            recents = recents,
            onOpen = onOpen,
            onForget = onForget,
            onDismiss = { asking = false }
        )
    }
}

/** Le cartelle come copertine. */
@Composable
private fun Covers(
    folders: List<Folder.Bucket>,
    columns: Int,
    counted: Boolean,
    /** Il corpo del nome, già scelto e già misurato da chi chiama. Vedi [FolderCard]. */
    nameStyle: TextStyle,
    onPick: (Folder.Bucket) -> Unit,
    onHide: (Folder.Bucket) -> Unit
) {
    LazyVerticalGrid(
        // ⚠️⚠️ **FISSO E NON PIÙ ADATTIVO dalla 0.45**, perché il numero adesso lo sceglie
        // l'utente (richiesta del 2026-08-29, dopo aver confermato che le copertine
        // bastano): un minimo in dp e un numero scelto sono due modi opposti di
        // rispondere alla stessa domanda, e tenerli tutti e due vorrebbe dire che la
        // scelta vale solo sugli schermi abbastanza larghi. La misura minima resta
        // scritta in `FOLDER_CELL`, che dice a quante colonne una copertina smette di
        // servire.
        columns = GridCells.Fixed(columns),
        horizontalArrangement = Arrangement.spacedBy(FOLDER_GAP),
        verticalArrangement = Arrangement.spacedBy(FOLDER_GAP),
        // ⚠️ Lo spazio in fondo tiene l'ultima cartella fuori da sotto il tastino, che
        // le si siederebbe sopra proprio quando si è scorso fino in fondo.
        contentPadding = PaddingValues(bottom = BELOW_FAB),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(
            items = folders,
            key = { it.id },
            // Una piastrella sola per tutte, così Compose riusa la composizione di quelle
            // che escono invece di ricostruirla mentre si scorre.
            contentType = { FOLDER_KIND }
        ) { bucket ->
            FolderCard(
                bucket = bucket,
                counted = counted,
                nameStyle = nameStyle,
                onClick = { onPick(bucket) },
                onLongClick = { onHide(bucket) }
            )
        }
    }
}

/**
 * Le cartelle come elenco.
 *
 * ⚠️ **Porta la copertina anche qui, piccola**: l'elenco della `0.29` mostrava la stessa
 * icona di cartella davanti a ogni riga, ed è esattamente il difetto che le copertine
 * hanno corretto. Questa vista serve a leggere **più nomi insieme**, non a rinunciare a
 * vedere che cosa c'è dentro.
 */
@Composable
private fun Rows(
    folders: List<Folder.Bucket>,
    onPick: (Folder.Bucket) -> Unit,
    onHide: (Folder.Bucket) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = BELOW_FAB),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(items = folders, key = { it.id }, contentType = { ROW_KIND }) { bucket ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onPick(bucket) },
                        onLongClick = { onHide(bucket) }
                    )
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(ROW_COVER)
                        .clip(RoundedCornerShape(FOLDER_CORNER))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Cover(bucket.cover)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bucket.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.folders_count,
                            bucket.count,
                            bucket.count
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider()
        }
    }
}

/**
 * Una cartella: la sua foto più recente, il nome e quante ne contiene.
 *
 * ⚠️ **La miniatura è la STESSA richiesta della griglia e del visualizzatore**
 * (`Thumbs.request`, 512 px): la misura è parte della chiave di cache, quindi una
 * copertina già vista altrove è gratis, e chiederla più piccola perché il riquadro è più
 * piccolo costerebbe una seconda generazione della stessa immagine. Vale anche per la
 * copertina minuscola dell'elenco, che è la stessa identica richiesta.
 * ⚠️ **La copertina è decorativa e non porta descrizione**: sotto ci sono già il nome e
 * il conto, e `clickable` fonde le semantiche dei figli, quindi TalkBack legge una voce
 * sola. Descrivere anche l'immagine la farebbe leggere due volte.
 * ⚠️ Il simbolo di ripiego non è un riquadro vuoto: una cartella senza copertina resta
 * toccabile e va detto che è una cartella, o sembra una piastrella rotta.
 */
@Composable
private fun FolderCard(
    bucket: Folder.Bucket,
    counted: Boolean,
    /**
     * Il corpo del nome, che dipende dalle colonne.
     *
     * ⚠️ **Arriva da fuori e non si ricava qui**: chi chiama lo ha già usato per misurare
     * l'altezza di una riga di griglia, e un secondo [folderNameStyle] qui darebbe la stessa
     * risposta oggi e sarebbe il posto da cui i due potrebbero divergere domani.
     */
    nameStyle: TextStyle,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val shape = RoundedCornerShape(FOLDER_CORNER)
    Column(
        // ⚠️ Il tocco lungo nasconde, ed è lo stesso gesto in tutte e due le viste: chi
        // impara a nascondere dalle copertine non deve reimpararlo nell'elenco.
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        // ⚠️ Costante e non numero: [coverHeader] somma questo distacco due volte per
        // sapere quanto è alta una riga di griglia.
        verticalArrangement = Arrangement.spacedBy(CARD_GAP)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Cover(bucket.cover)
        }
        /*
         * ⚠️⚠️ **UN A CAPO E NON PIÙ DI UNO, dalla 0.77** (richiesta dell'utente: *con
         * qualunque numero di colonne è ammesso un a capo, non più di uno*). Fino alla `0.76`
         * era una riga sola, e a quattro colonne quasi ogni nome finiva con i tre puntini
         * dopo la prima parola.
         * ⚠️ **L'ellissi resta**, perché due righe non bastano sempre: un nome che sfora
         * anche la seconda si taglia là, e senza `Ellipsis` si taglierebbe a metà lettera
         * senza dire che manca qualcosa.
         * ⚠️⚠️ **E [NAME_LINES] È LA STESSA COSTANTE CHE MISURA L'ALTEZZA DELLA RIGA**, cioè
         * chi la cambia qui cambia anche il conto del frontespizio, che è quello che vuole.
         * Un numero scritto due volte qui avrebbe rifatto il difetto della `0.68`: mezza
         * cartella in vista in fondo alla schermata.
         */
        Text(
            text = bucket.name,
            style = nameStyle,
            maxLines = NAME_LINES,
            overflow = TextOverflow.Ellipsis
        )
        /*
         * ⚠️⚠️ **PRIMA L'ICONA, POI IL NUMERO, ed è la proposta B del mockup** (scelta
         * dell'utente): l'icona è larga sempre uguale, quindi resta **incolonnata** sotto il
         * nome in tutte le cartelle, e il numero cresce verso destra dove lo spazio è vuoto.
         * Con l'ordine opposto sarebbe l'icona a spostarsi, di tante posizioni quante sono
         * le cifre.
         * ⚠️ **La parola 'immagini' non c'è più** (richiesta dell'utente): su quattro colonne
         * si tagliava, e a schermo pieno erano dodici volte la stessa parola. Il plurale
         * `folders_count` resta e serve alla **descrizione per TalkBack**, che di parole ha
         * bisogno: là dentro 'x immagini' è l'informazione, non rumore.
         * ⚠️⚠️ **`maxLines = 1` NON è pignoleria**: senza, una dicitura che va a capo rende
         * la riga più alta di quanto [coverHeader] ha misurato, e in fondo alla schermata si
         * vede il pezzo di riga che quel conto doveva tenere fuori. Era la seconda causa del
         * difetto della `0.68`, insieme al tetto del frontespizio.
         */
        if (counted) {
            val spoken =
                pluralStringResource(R.plurals.folders_count, bucket.count, bucket.count)
            Row(
                // ⚠️⚠️ **`clearAndSetSemantics` E NON `semantics`**: il tocco della scheda
                // fonde le semantiche dei figli, quindi aggiungere una descrizione
                // lascerebbe **anche** il numero nudo, e TalkBack leggerebbe '1284 immagini
                // 1284'. Qui si buttano le semantiche dei figli e si mette la frase.
                modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(COUNT_GAP)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(COUNT_ICON)
                )
                Text(
                    text = bucket.count.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

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
 * Il lato a cui una copertina è stata disegnata per stare, e il metro con cui si giudica
 * il numero di colonne che l'utente sceglie.
 *
 * ⚠️⚠️ **FINO ALLA 0.45 DECIDEVA LUI QUANTE COLONNE, e il conto resta utile**:
 * `GridCells.Adaptive` teneva `(spazio + distacco) / (minimo + distacco)` colonne. Su uno
 * schermo da 360dp, tolti i 12 di margine per lato, restano 336: `(336 + 12) / (150 + 12)`
 * fa 2.1, quindi **due**, ed è il valore di fabbrica dell'impostazione che l'ha
 * sostituito. Lo stesso conto dice quanto si stringe scegliendone di più: a **tre**
 * colonne una copertina misura 104dp, a **quattro** 78dp.
 * ⚠️ **Due e non tre come le foto**: qui sotto la copertina ci sono un nome e un conto, e
 * su tre colonne un nome vero (`WhatsApp Images`) resta una sigla. Le miniature di una
 * cartella non hanno didascalia, e infatti stanno a 108dp. È il costo che chi sceglie
 * quattro colonne accetta, ed è la ragione per cui il valore di fabbrica non cambia.
 */
private val FOLDER_CELL = 150.dp

/** Il distacco fra le copertine: più largo di quello delle foto, perché qui separa schede. */
private val FOLDER_GAP = 12.dp

/**
 * Il margine della schermata, uguale sui quattro lati.
 *
 * ⚠️ **Il valore laterale non è estetico**: è quello che permette **due** colonne di
 * copertine su uno schermo da 360dp, e il conto sta in [FOLDER_CELL]. Chi lo allarga
 * stringe le copertine.
 */
private val SCREEN_PAD = 12.dp

/** Lo stacco fra il frontespizio (o il titolo) e quello che viene sotto. */
private val HEADER_GAP = 8.dp

/** Il distacco fra la copertina e le sue due righe di testo, dentro una scheda. */
private val CARD_GAP = 6.dp

/**
 * Lo smusso della copertina.
 *
 * ⚠️ Più tondo dei 4dp delle miniature apposta: una piastrella molto smussata si legge
 * come un **contenitore**, una quasi quadrata come una fotografia. Qui la differenza fra
 * le due cose è tutta quella che l'occhio ha per capire dove si trova.
 */
private val FOLDER_CORNER = 12.dp

/** La copertina dell'elenco: grande quanto due righe di testo, che è l'altezza della riga. */
private val ROW_COVER = 48.dp

/**
 * Quanta parte dello schermo tiene il frontespizio da aperto **nella vista a elenco**.
 *
 * ⚠️ Il numero viene dalla richiesta dell'utente letta al contrario: le cartelle nel
 * **60% basso**, quindi qui il 40% alto. Non è una proporzione estetica ma una misura di
 * portata del pollice, e ritoccarla verso il basso rimette le cartelle fuori tiro.
 * ⚠️⚠️ **Dalla `0.60` non vale più per le copertine**, che si fanno il conto da sé in
 * [coverHeader]: là la frazione fissa dava il numero di righe sbagliato appena si
 * cambiavano le colonne. Qui resta perché un elenco non ha colonne, quindi non ha un
 * quadrato da far quadrare.
 */
private const val HEADER_SHARE = 0.4f

// ⚠️ QUI VIVEVA `HEADER_CAP`, il tetto del frontespizio al 50% dello schermo, uscito nella
// 0.69 perché ERA il difetto: vedi [coverHeader], dove sta la misura. Non si rimetta senza
// aver letto quella nota, e senza rispondere alla domanda che il tetto lasciava aperta, cioè
// chi si prende lo spazio che al frontespizio viene negato.

/**
 * Quanto è larga l'icona accanto al conto delle immagini.
 *
 * ⚠️ **15dp e non 16**: accanto a un corpo di 12sp (`bodySmall`) una da 16 pesa più del
 * numero e diventa lei la voce principale, mentre qui il dato è il numero.
 * ⚠️ Entra nel conto dell'altezza di una riga di griglia, perché a corpo di sistema piccolo
 * è **lei** e non il testo a decidere quanto è alta la riga del conto. Vedi `captionPx`.
 */
private val COUNT_ICON = 15.dp

/** Quanto sta l'icona dal numero: abbastanza da non toccarlo, non tanto da separarli. */
private val COUNT_GAP = 4.dp

/**
 * Il testo con cui si misura l'altezza di una riga di didascalia.
 *
 * ⚠️ **Una lettera alta e una bassa**, perché su una riga sola l'altezza la decide lo
 * stile e non i glifi, ma un campione con ascendente e discendente è quello che si
 * riconosce come misura in caso di dubbio. Non finisce sullo schermo: serve solo al
 * misuratore.
 */
private const val CAPTION_SAMPLE = "Ag"

/**
 * Quante righe può prendere il nome di una cartella: **due**, cioè un a capo e non più di uno
 * (richiesta dell'utente, dalla `0.77`).
 *
 * ⚠️⚠️ **Serve in DUE posti che devono restare d'accordo**, e sono l'unica ragione per cui è
 * una costante e non un numero: il `maxLines` della scheda e la misura dell'altezza di una
 * riga di griglia (`captionPx`). Alzata in uno solo, la griglia sfora e in fondo alla
 * schermata compare mezza cartella.
 */
private const val NAME_LINES = 2

/**
 * Da quante colonne il nome della cartella passa a un corpo più piccolo. Vedi
 * [folderNameStyle].
 *
 * ⚠️ Quattro è anche il massimo che le impostazioni offrono, quindi oggi la condizione vale
 * per un solo valore: è scritta come soglia perché il giorno che le colonne diventassero
 * cinque la risposta giusta sarebbe la stessa, e non una riga in più da ricordare.
 */
private const val NARROW_COLUMNS = 4

/**
 * A che punto della sua altezza la sfumatura sopra il tastino ha inghiottito tutto.
 *
 * ⚠️ 0,55 cioè **abbastanza velocemente**, come chiesto: sotto la metà della fascia il fondo
 * è pieno, e quello che scorre là sotto sparisce prima di arrivare al bordo dello schermo
 * invece di essere tagliato di netto. Più alto (0,9) si vedrebbe una riga di cartelle
 * mezza sbiadita, che è il difetto che la sfumatura deve togliere.
 */
private const val SWALLOW = 0.55f

/** L'icona del frontespizio: più grande di quella delle impostazioni, perché qui accoglie. */
private val HEADER_ICON = 96.dp

/** Tutte le piastrelle sono la stessa cosa, e dirlo permette a Compose di riusarle. */
private const val FOLDER_KIND = "folder"

/** Come sopra, per le righe dell'elenco. */
private const val ROW_KIND = "folder-row"

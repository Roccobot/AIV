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
        val nameStyle = MaterialTheme.typography.titleSmall
        val countStyle = MaterialTheme.typography.bodySmall
        val captionPx = remember(measurer, nameStyle, countStyle) {
            measurer.measure(CAPTION_SAMPLE, nameStyle, maxLines = 1).size.height +
                measurer.measure(CAPTION_SAMPLE, countStyle, maxLines = 1).size.height
        }

        val headerMax = when {
            !home -> 0.dp
            view == FolderView.GRID ->
                coverHeader(columns, maxWidth, maxHeight, captionPx, density)
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

                view == FolderView.GRID -> Covers(folders!!, columns, onPick) { hiding = it }
                else -> Rows(folders!!, onPick) { hiding = it }
            }
        }

        // ⚠️ Il tastino manca quando si sta scegliendo la cartella dell'avvio: là dentro
        // ci sono le impostazioni, da cui si è arrivati, e un giro chiuso non serve a
        // nessuno.
        if (home) {
            Hub(
                view = view,
                recents = recents,
                onOpen = onOpen,
                onView = onView,
                onForget = onForget,
                onSettings = onSettings,
                onSearch = onSearch,
                onBin = onBin,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
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
 * Quanto spazio resta al frontespizio quando la griglia si è presa **tante righe quante
 * sono le colonne**.
 *
 * ⚠️⚠️ **È LA REGOLA DELLA 0.60, e ha sostituito il 60% fisso** (richiesta dell'utente:
 * *invece di un generico 60% di spazio per la griglia, facciamo che sono sempre
 * visualizzate 4 cartelle a 2 colonne, 9 a 3 colonne, 16 a 4 colonne*). Il quadrato è la
 * forma della richiesta: righe pari alle colonne, quindi 2x2, 3x3, 4x4. La frazione fissa
 * rispondeva bene a **due** colonne e male alle altre, perché con le copertine più
 * strette le righe si accorciano e nel 60% ne entravano quattro e mezza invece di tre.
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
 * ⚠️ **[HEADER_CAP] non è la regola, è il paletto**: su un telefono altissimo e strettissimo
 * il quadrato di copertine potrebbe lasciare libera più di metà schermo, e un frontespizio
 * più alto della metà rimetterebbe le cartelle fuori dalla portata del pollice, che è
 * esattamente il motivo per cui quello spazio esiste. Quando scatta si vedono **più** righe
 * del quadrato, mai meno: sbaglia dalla parte giusta.
 */
private fun coverHeader(
    columns: Int,
    width: Dp,
    height: Dp,
    captionPx: Int,
    density: Density
): Dp = with(density) {
    val slots = columns.coerceAtLeast(1)
    val gapPx = FOLDER_GAP.toPx()
    val roomPx = (width - SCREEN_PAD * 2).toPx() - gapPx * (slots - 1)
    val cellPx = ceil(roomPx / slots).coerceAtLeast(0f)
    val rowPx = cellPx + CARD_GAP.toPx() * 2 + captionPx
    val gridPx = rowPx * slots + gapPx * (slots - 1)
    val freePx = (height - SCREEN_PAD * 2 - HEADER_GAP).toPx() - gridPx
    freePx.coerceIn(0f, height.toPx() * HEADER_CAP).toDp()
}

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
 */
@Composable
private fun Hub(
    view: FolderView,
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
            DropdownMenuItem(
                text = { Text(stringResource(R.string.hub_pick)) },
                onClick = {
                    open = false
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
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
        Text(
            text = bucket.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = pluralStringResource(R.plurals.folders_count, bucket.count, bucket.count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

/**
 * Il tetto del frontespizio in griglia, come frazione dello schermo. Vedi [coverHeader],
 * che spiega perché è un paletto e non la regola.
 */
private const val HEADER_CAP = 0.5f

/**
 * Il testo con cui si misura l'altezza di una riga di didascalia.
 *
 * ⚠️ **Una lettera alta e una bassa**, perché su una riga sola l'altezza la decide lo
 * stile e non i glifi, ma un campione con ascendente e discendente è quello che si
 * riconosce come misura in caso di dubbio. Non finisce sullo schermo: serve solo al
 * misuratore.
 */
private const val CAPTION_SAMPLE = "Ag"

/** L'icona del frontespizio: più grande di quella delle impostazioni, perché qui accoglie. */
private val HEADER_ICON = 96.dp

/** Tutte le piastrelle sono la stessa cosa, e dirlo permette a Compose di riusarle. */
private const val FOLDER_KIND = "folder"

/** Come sopra, per le righe dell'elenco. */
private const val ROW_KIND = "folder-row"

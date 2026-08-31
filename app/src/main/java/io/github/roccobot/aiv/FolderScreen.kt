package io.github.roccobot.aiv

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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
     * ⚠️ **Dalla `0.93` decide SOLO la larghezza delle copertine**, e quante righe si
     * vedano è una conseguenza: il frontespizio si prende [HEADER_SHARE] dello schermo e la
     * griglia riempie il resto. Fra la `0.60` e la `0.92` il numero governava anche le
     * righe, che venivano riservate una per una.
     */
    columns: Int,
    /**
     * Se sotto la copertina si vede il conto delle immagini. Vedi `Settings.folderCount`.
     *
     * ⚠️ **Spegnendolo la riga si accorcia**, quindi nello stesso spazio ne entra qualcuna
     * in più: dalla `0.93` il frontespizio non si adatta, ed è la griglia a riempire quello
     * che le tocca.
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
    /**
     * Cambia il numero di colonne, dalla `0.78`: lo scrive la scorciatoia del tocco lungo.
     *
     * ⚠️ **La stessa impostazione delle preferenze e non una seconda**: la scorciatoia è una
     * via più corta per la stessa manopola, quindi il numero resta globale e la si ritrova
     * cambiata anche là. Era la richiesta (*che resta globale per tutte le cartelle*).
     */
    onColumns: (Int) -> Unit,
    /**
     * Dove sta la vista 'Cartelle di sistema', e `null` vuol dire in cima.
     *
     * ⚠️⚠️ **VIVE NEL MODELLO e non qui dentro**, ed è la ragione per cui arriva come
     * parametro invece di essere un `remember`: si esce da questa schermata ogni volta che si
     * apre una fotografia, e al ritorno la navigazione deve ritrovarsi dov'era. Un ricordo
     * locale morirebbe a ogni andata e ritorno, riportando in cima.
     */
    treePath: String?,
    /** Se eliminare vuol dire mandare nel cestino: serve alle azioni della vista ad albero. */
    binOn: Boolean,
    /** I campi delle informazioni, per la voce 'Info' della vista ad albero. */
    factFields: List<FactField>,
    onTreePath: (String?) -> Unit,
    /** Una fotografia toccata nella vista delle cartelle di sistema: la serie e la posizione. */
    onTreeOpen: (List<Uri>, Int) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

    /** Se si sta scegliendo la dimensione della griglia col tocco lungo sul tastino. */
    var sizing by remember { mutableStateOf(false) }

    /**
     * Se il velo che insegna la scorciatoia delle colonne si è già visto.
     *
     * ⚠️ **Parte da 'già visto', e come nella griglia delle foto è la scelta prudente**: il
     * valore vero arriva dall'archivio un attimo DOPO la prima composizione, quindi partendo
     * da `false` il velo comparirebbe per un fotogramma anche a chi l'ha già chiuso.
     */
    val columnsSeen by produceState(initialValue = true, context) {
        Hint.COLUMNS.flow(context).collect { value = it }
    }

    /**
     * ⚠️ La bandierina locale esiste perché l'archivio risponde con un giro di ritardo:
     * scrivere in DataStore e aspettare che il flusso riemetta vuol dire un fotogramma o due
     * col velo ancora steso, e nel caso peggiore col dialogo che si apre **sotto** di lui.
     */
    var columnsOff by remember { mutableStateOf(false) }

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

        val nameStyle = folderNameStyle(columns)

        /*
         * ⚠️⚠️ **IL FRONTESPIZIO È UNA FRAZIONE FISSA, dalla 0.93, e prima si CALCOLAVA**
         * (istruzione dell'utente, 2026-08-31: *preferisco semplificare e tornare alla
         * logica precedente, che mi piaceva*). Qui viveva `coverHeader`, che riservava alla
         * griglia un numero esatto di righe e dava al frontespizio quello che avanzava,
         * misurando i due testi sotto ogni copertina per sapere quanto è alta una riga.
         * Funzionava, e non era quello che serviva: l'utente vuole l'intestazione come
         * spazio **deliberato**, per tenere le cartelle in basso a portata di pollice, e un
         * avanzo non è uno spazio deliberato. Il suo riscontro è la misura di quel divario:
         * *c'era una parte troppo grande della schermata dedicata alla griglia, e troppo
         * poco spazio per l'intestazione*.
         * ⚠️ **Quante righe si vedono adesso NON è più una scelta ma una conseguenza**, e va
         * saputo prima di rimettere mano qui: con la frazione fissa il numero di righe cade
         * dove cade, e a volte l'ultima è mezza. Non è il difetto della `0.68`: là i pezzi
         * di riga contraddicevano una richiesta di N righe esatte, qui sono il modo in cui
         * si vede che sotto c'è dell'altro.
         * ⚠️ **Con `coverHeader` se ne sono andate `startRows` e la misura dei testi**, che
         * servivano solo a lui. Il ragionamento che portavano vive nel corpo della PR e in
         * questa nota, non in codice che nessuno chiama.
         */
        val headerMax = if (home) maxHeight * HEADER_SHARE else 0.dp
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

                // ⚠️ Prima dei rami che guardano `folders`, e non dopo: la vista delle
                // cartelle di sistema legge il disco, non il MediaStore, quindi non ha
                // niente da aspettare e non le importa se l'elenco delle cartelle è vuoto.
                // ⚠️⚠️ **`home &&`, e senza quello la scelta della cartella d'avvio si
                // romperebbe**: là la domanda è *quale cartella apro all'avvio*, e la
                // risposta dev'essere una cartella **del MediaStore**, perché l'avvio ne apre
                // la griglia. Navigando il disco non si produce quel genere di risposta, e
                // toccare una cartella non sceglierebbe niente. Nella veste 'scegli la
                // cartella' si scivola quindi sul ramo in fondo, cioè l'elenco.
                view == FolderView.TREE && home ->
                    TreeList(treePath, hidden, binOn, factFields, onTreePath, onTreeOpen)

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
             * lo spazio riservato ([BELOW_FAB]) tiene l'ultima cartella sopra di lei; serve
             * quando si scorre, dove l'alternativa era una riga tagliata di netto dal bordo
             * dello schermo.
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
            /*
             * ⚠️⚠️ **UNA CURVA E NON QUATTRO FERMATE, dalla 0.92** (richiesta dell'utente,
             * due volte: *più alta e graduale, in modo che il FAB ricada sempre in un'area
             * neutra*, e poi *ancora più sfumata e graduale*). Due fermate sole dànno una
             * rampa **dritta**, e l'occhio la legge come un bordo sfocato invece che come
             * una dissolvenza: il difetto sta nei due spigoli, dove la salita comincia e
             * dove finisce. `smoothstep` li toglie tutti e due, perché parte con pendenza
             * zero e ci arriva con pendenza zero.
             * ⚠️ **Si calcola invece di essere scritta**: le fermate a mano sarebbero
             * dodici numeri da riscrivere ogni volta che si cambia l'altezza della fascia,
             * e nessuno lo farebbe. Così [GRADIENT_TIMES] è l'unica manopola.
             * ⚠️ Il colore è `background` e non `surface`: è quello che la `Surface` del
             * tema mette dietro a tutta l'app (vedi `AivTheme`), quindi la sfumatura arriva
             * **esattamente** al fondo su cui sta.
             */
            val ramp = remember(ground) {
                Array(GRADIENT_STOPS + 1) { step ->
                    val at = step / GRADIENT_STOPS.toFloat()
                    val t = (at / SWALLOW).coerceAtMost(1f)
                    at to ground.copy(alpha = t * t * (3f - 2f * t))
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(GRADIENT_REACH)
                    .background(Brush.verticalGradient(colorStops = ramp))
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
                onSize = { sizing = true },
                // ⚠️ Costante e non numero: [FAB_REACH] dice quanto è alta la fascia
                // dipinta, e [BELOW_FAB] quanto spazio si lascia sotto l'ultima cartella.
                modifier = Modifier.align(Alignment.BottomEnd).padding(HUB_PAD)
            )
        }

        /*
         * ⚠️⚠️ **IL VELO CHE INSEGNA LA SCORCIATOIA DELLE COLONNE, dalla 0.78** (richiesta
         * dell'utente, con la sua frase): è il terzo della famiglia, e gli altri due stanno
         * nella griglia delle foto. La macchina è la stessa, [HintVeil], e qui cambiano la
         * frase e il tastino.
         * ⚠️⚠️ **COMPARE SOLO QUANDO C'È UNA GRIGLIA DA DIMENSIONARE**, e ognuna delle
         * condizioni serve: fuori dalla casa il tastino non c'è, senza il permesso non ci
         * sono cartelle, senza cartelle non c'è niente da disporre, e nell'elenco le colonne
         * non esistono. Al primo avvio, che è anche il primo posto in cui l'app si mostra,
         * insegnare a dimensionare una griglia vuota sarebbe rumore sopra una schermata che
         * chiede un permesso.
         * ⚠️ **Il tocco sulla copia NON apre il menu**, al contrario di quella della
         * selezione: là il tocco breve del tastino è l'azione principale (le operazioni),
         * qui è il menu dell'app, che si scopre da sé perché è l'unica cosa che quel tastino
         * fa da sempre. Quello che va insegnato è il tocco **lungo**, ed è quello che la
         * copia fa.
         */
        val hint = home && granted && view == FolderView.GRID &&
            !folders.isNullOrEmpty() && !columnsSeen && !columnsOff

        /** Il velo si archivia appena l'utente fa la cosa che insegnava, o appena la salta. */
        val hintDone: () -> Unit = {
            columnsOff = true
            scope.launch { Hint.COLUMNS.remember(context) }
        }

        if (hint) {
            HintVeil(
                text = stringResource(R.string.columns_hint),
                // ⚠️ Due rientri e non tre come nella griglia delle foto: qui il tastino vive
                // dentro il rientro di sistema più il suo margine, e basta. Il perché sta in
                // [HintVeil], sul parametro.
                inset = Modifier.safeDrawingPadding().padding(HUB_PAD),
                onDone = hintDone
            ) {
                TapHoldFab(
                    icon = Icons.Default.MoreHoriz,
                    label = stringResource(R.string.hub_open),
                    container = HINT_MARK,
                    ink = HINT_INK,
                    // ⚠️ Nessuna ombra: sopra un velo non c'è niente da cui staccarsi.
                    lift = 0.dp,
                    holdLabel = stringResource(R.string.columns_title),
                    onTap = hintDone,
                    onHold = { hintDone(); sizing = true }
                )
            }
        }
    }

    if (sizing) {
        SizeDialog(
            current = columns,
            onPick = onColumns,
            onDismiss = { sizing = false }
        )
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
 * Il corpo del nome di una cartella, che dipende da quante colonne ci sono.
 *
 * ⚠️⚠️ **A QUATTRO COLONNE SCENDE** (richiesta dell'utente): là la cella è larga meno di un
 * quarto di schermo, cioè 78dp su 360, e a `titleSmall` due parole ci stanno a stento.
 * `labelMedium` toglie due punti e **tiene il peso medio**, che è quello che distingue il nome
 * dal conto sotto: passare a `bodySmall` avrebbe reso i due indistinguibili.
 * ⚠️ **Una funzione e non un corpo scritto due volte**: la scheda lo usa per disegnare, e
 * fino alla `0.92` lo usava anche il conto delle righe. Quel conto non c'è più, ma la
 * funzione resta la sola fonte del corpo del nome.
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
 * ⚠️ **Quanto sia alto NON lo decide lui**: lo decide [HEADER_SHARE], in tutte e due le
 * viste. Fra la `0.60` e la `0.92` la griglia faceva eccezione e si calcolava l'altezza da
 * sé, e l'utente ha chiesto di tornare alla frazione fissa.
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
/** Come si chiama una vista nel menu. Vedi la nota dentro [Hub]. */
@StringRes
private fun FolderView.label(): Int = when (this) {
    FolderView.GRID -> R.string.hub_view_grid
    FolderView.LIST -> R.string.hub_view_list
    FolderView.TREE -> R.string.hub_view_tree
}

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
    /**
     * Il tocco lungo: la scorciatoia della dimensione della griglia, dalla `0.78`.
     *
     * ⚠️ **Il dialogo lo apre chi chiama e non questo composabile**, come per il velo: sono
     * cose della schermata, e un tastino che si apre un dialogo da sé diventa il posto in cui
     * cercare quel dialogo, che non è dove sta.
     */
    onSize: () -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    var asking by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { picked -> picked?.let(onOpen) }

    Box(modifier = modifier) {
        /*
         * ⚠️⚠️ **NON È PIÙ `SmallFloatingActionButton`, dalla 0.78**, e la ragione è la
         * stessa del tastino della selezione: quel composabile prende un `onClick` solo, e un
         * `combinedClickable` messo sul suo modificatore non vedrebbe mai il tocco lungo. La
         * resa non cambia: [TapHoldFab] è la stessa `Surface` da 40dp, quadrata con gli
         * angoli appena smussati come chiesto, perché il tondo pieno griderebbe 'azione
         * principale' e qui l'azione principale sono le cartelle.
         */
        TapHoldFab(
            icon = Icons.Default.MoreHoriz,
            label = stringResource(R.string.hub_open),
            container = MaterialTheme.colorScheme.primaryContainer,
            ink = MaterialTheme.colorScheme.onPrimaryContainer,
            lift = FAB_LIFT,
            holdLabel = stringResource(R.string.columns_title),
            onTap = { open = true },
            onHold = onSize
        )
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
            // ⚠️⚠️ **DALLA `0.84` LE VISTE SONO TRE, e il menu mostra le DUE che non sono
            // quella corrente**: con due bastava una riga sola che nominava l'altra, e la
            // regola qui sopra resta intatta, perché una riga che nomina la vista in cui si
            // è già non sarebbe una richiesta ma un indicatore di stato spento.
            // ⚠️ **'Cartelle di sistema' non segue la forma delle altre due** ('Visualizzazione
            // griglia', 'Visualizzazione lista'), e non è una dimenticanza: quello è il NOME
            // che l'utente ha dato alla vista, non una descrizione, e piegarlo allo schema
            // vorrebbe dire ribattezzare una cosa che ha già un nome.
            FolderView.entries.filter { it != view }.forEach { other ->
                DropdownMenuItem(
                    text = { Text(stringResource(other.label())) },
                    onClick = { open = false; onView(other) }
                )
            }

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

/**
 * La dimensione della griglia, chiesta col tocco lungo sul tastino.
 *
 * ⚠️⚠️ **È LA STESSA IMPOSTAZIONE DELLE PREFERENZE, non una seconda** (richiesta
 * dell'utente: *che resta globale per tutte le cartelle*): questa è una via più corta per
 * arrivarci, e chi la usa la ritrova cambiata anche là dentro. Un numero di colonne 'della
 * sessione' sarebbe una terza cosa da capire.
 * ⚠️ **Le pastiglie sono quelle delle impostazioni**, `FilterChip` con i numeri nudi: chi ha
 * già visto quella riga riconosce questa senza leggerla. Una copia della riga intera (con
 * titolo e spiegazione) non serviva: qui il titolo del dialogo dice già di che cosa si parla.
 * ⚠️⚠️ **SCEGLIERE NON CHIUDE, e non è una dimenticanza**: un dialogo di Material lascia
 * vedere quello che c'è dietro, quindi la griglia si riordina **sotto gli occhi** a ogni
 * pastiglia toccata, e si può provare 2, 3 e 4 senza riaprire niente. Chiudendo a ogni scelta,
 * confrontarle vorrebbe dire tre tocchi lunghi. Il tasto in basso quindi non conferma niente:
 * dice che si è finito.
 */
@Composable
private fun SizeDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.columns_title)) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FOLDER_COLUMNS.forEach { n ->
                    FilterChip(
                        selected = n == current,
                        onClick = { onPick(n) },
                        label = { Text(n.toString()) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pick_close)) }
        }
    )
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
         * ⚠️ **`maxLines = 1` resta**, anche se dalla `0.93` non c'è più un conto delle
         * righe da far quadrare: una dicitura che va a capo fa ballare l'altezza di una
         * cella rispetto alle sue vicine, e la griglia diventa irregolare.
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
 * Quanta parte dello schermo tiene il frontespizio da aperto: **un terzo scarso**.
 *
 * ⚠️⚠️ **NON È UNA PROPORZIONE ESTETICA ma una misura di portata del pollice**: l'utente
 * usa l'intestazione come scusa per tenere le cartelle in basso, in stile OneUI (sue
 * parole, 2026-08-31), quindi ritoccarla verso il basso rimette le cartelle fuori tiro e
 * verso l'alto toglie righe che si vorrebbero vedere.
 * ⚠️⚠️ **VALE PER TUTTE E DUE LE VISTE, dalla `0.93`**: fra la `0.60` e la `0.92` la
 * griglia faceva eccezione e si riservava un numero esatto di righe, e l'utente ha chiesto
 * di tornare alla frazione fissa perché quel calcolo dava alla griglia più di quanto lui
 * volesse. Un numero solo per la stessa regola: prima, cambiarla voleva dire ricordarsi
 * che esisteva anche altrove.
 * ⚠️ **34 e non 40, ed è una misura**: con il 40% l'area della griglia scende a 484dp
 * sullo schermo dell'utente, e la fascia sfumata (216dp) arriverebbe a coprire il conto
 * sotto la SECONDA riga di cartelle, cioè velerebbe una riga vera invece di quella che
 * fa capolino. Con il 34% la griglia sale a 533dp e la sfumatura comincia esattamente
 * dove comincia la terza riga. L'utente ha autorizzato il cambio proprio per questo
 * (*se pensi che sia troppo sacrificata possiamo passare a 66% alla griglia e 34%
 * all'intestazione*).
 */
private const val HEADER_SHARE = 0.34f

/**
 * Quanto è larga l'icona accanto al conto delle immagini.
 *
 * ⚠️ **15dp e non 16**: accanto a un corpo di 12sp (`bodySmall`) una da 16 pesa più del
 * numero e diventa lei la voce principale, mentre qui il dato è il numero.
 * ⚠️ A corpo di sistema piccolo è **lei** e non il testo a decidere quanto è alta la riga
 * del conto, perché sp e dp non scalano insieme.
 */
private val COUNT_ICON = 15.dp

/** Quanto sta l'icona dal numero: abbastanza da non toccarlo, non tanto da separarli. */
private val COUNT_GAP = 4.dp

/**
 * Quante righe può prendere il nome di una cartella: **due**, cioè un a capo e non più di uno
 * (richiesta dell'utente, dalla `0.77`).
 *
 * ⚠️ Fino alla `0.92` serviva in due posti che dovevano restare d'accordo, il `maxLines`
 * della scheda e la misura dell'altezza di una riga; adesso il secondo non c'è più, e
 * resta una costante perché è una scelta dell'utente e non un dettaglio del disegno.
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
 * Quante volte il tastino è alta la fascia dipinta.
 *
 * ⚠️⚠️ **DIPINGERE PIÙ IN ALTO NON COSTA ALTEZZA ALLA GRIGLIA, ed è tutto il senso di
 * questa costante**: lo spazio **riservato** resta [BELOW_FAB]; questa dice soltanto fin
 * dove arriva il colore. Tenerle separate è la ragione per cui la sfumatura si può alzare
 * senza che la schermata perda una riga di cartelle.
 * ⚠️⚠️ **DUE E NON TRE, ed è una misura e non un gusto**: sullo schermo dell'utente la
 * griglia ha 533dp e le sue righe sono alte 184, quindi la terza comincia a 392, cioè
 * 141dp sopra il fondo. Con 2 la fascia è alta 144 e la dissolvenza comincia **esattamente
 * lì**, sulla riga che fa capolino; con 3 sarebbe 216 e arriverebbe a velare il conto
 * sotto la SECONDA riga, che è una riga vera. Chi la alza ancora tolga prima quel conto.
 */
private const val GRADIENT_TIMES = 2f

/**
 * In quanti gradini si disegna la curva della sfumatura.
 *
 * ⚠️ Dodici, cioè abbastanza perché la curva non si veda a scalini e pochi perché la lista
 * resti una cosa che si legge. Sotto i sei, su una fascia alta come questa, i gradini si
 * distinguono a occhio nudo sui fondi chiari.
 */
private const val GRADIENT_STOPS = 12

/** Quanto è alta la fascia dipinta sopra il tastino. */
private val GRADIENT_REACH = FAB_REACH * GRADIENT_TIMES

/**
 * A che punto della sua altezza la sfumatura sopra il tastino ha inghiottito tutto.
 *
 * ⚠️⚠️ **NON È PIÙ UN NUMERO SCELTO A OCCHIO (era 0,55): si RICAVA**, ed è così che la
 * promessa 'il tastino cade sempre su un fondo neutro' diventa vera per costruzione invece
 * che per fortuna. Il bordo superiore del tastino sta a [FAB_REACH] dal fondo, cioè a
 * questa frazione della fascia dipinta: da lì in giù il colore è pieno, quindi sotto il
 * tastino non passa mai una fotografia. Cambiando [GRADIENT_TIMES] il conto si rifà da sé.
 * ⚠️ Il rovescio da conoscere: alzando ancora la fascia, la parte piena resta la stessa e
 * cresce solo la dissolvenza sopra, che è esattamente ciò che 'più graduale' vuol dire.
 * ⚠️⚠️ **L'utente ha concesso di NON arrivare al pieno** (*può andare anche il 20%: si
 * intuisce comunque bene che è una cosa che va scomparendo*), e la concessione **non è
 * stata usata**: con la fascia a tre volte il tastino la rampa ha spazio per arrivare al
 * pieno restando dolce, e il pieno serve a tenere la promessa dell'altra richiesta, cioè
 * che sotto il tastino non passi mai una fotografia. Sta scritto perché è una scelta e non
 * una dimenticanza: chi un giorno volesse una fascia più corta sa che può spendere quel
 * permesso invece di irripidire la curva.
 */
private const val SWALLOW = 1f / GRADIENT_TIMES

/** L'icona del frontespizio: più grande di quella delle impostazioni, perché qui accoglie. */
private val HEADER_ICON = 96.dp

/** Tutte le piastrelle sono la stessa cosa, e dirlo permette a Compose di riusarle. */
private const val FOLDER_KIND = "folder"

/** Come sopra, per le righe dell'elenco. */
private const val ROW_KIND = "folder-row"

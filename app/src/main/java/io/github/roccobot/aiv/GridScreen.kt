package io.github.roccobot.aiv

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.annotation.StringRes
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.material.icons.filled.Folder
import androidx.compose.ui.text.font.FontFamily

/**
 * Una cartella intera in miniature, ed è il primo passo della galleria.
 *
 * ⚠️ **L'ordine è quello di lettura**: le miniature sono nella stessa sequenza in cui la
 * strisciata le sfoglierà, quindi 'la prossima' è la stessa cosa qui e là. Col verso
 * predefinito è la più recente per prima; accendendo l'impostazione si girano tutte e due
 * insieme, perché vengono dalla stessa lista (vedi `Folder.Series.reversed`). Una griglia
 * 'sempre dalla più recente' farebbe aprire alla terza miniatura la terzultima immagine.
 *
 * La lista arriva già pronta dal modello: è la serie che il visualizzatore sfoglierà, quindi
 * aprire un'immagine dalla griglia non costa query e non può dare due ordini diversi.
 * Le miniature le chiede `Thumbs`, e là è scritto perché.
 *
 * ⚠️ **[onChanged] si chiama dopo ogni operazione**: copia, sposta, rinomina ed elimina
 * cambiano i file sul disco, e senza una rilettura resterebbero miniature di file che non
 * esistono più.
 */
@Composable
fun GridScreen(
    title: String,
    items: List<Uri>?,
    highlight: Int?,
    onOpen: (Int) -> Unit,
    onBack: () -> Unit,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Il testo cercato, e `null` quando questa non è una ricerca.
     *
     * Due parametri e non una schermata nuova: fra la griglia di una cartella e quella dei
     * risultati cambia solo la testata, e tutto il resto è lo stesso codice. Una schermata a
     * parte ne vorrebbe una seconda copia, che divergerebbe.
     */
    query: String? = null,
    onQuery: (String) -> Unit = {},
    /**
     * Se l'intestazione ha il gradiente: `Settings.frontWash`.
     *
     * I parametri dell'intestazione arrivano da fuori uno per uno, come ogni impostazione che
     * questa schermata legge: la griglia non conosce `SettingsStore`, e i valori di riserva sono
     * quelli di fabbrica (vedi la nota su [columns]).
     */
    frontWash: Boolean = Settings().frontWash,
    /** Se il nome nell'intestazione è grande e graziato: `Settings.frontSerif`. */
    frontSerif: Boolean = Settings().frontSerif,
    /** Se l'intestazione ha le pastiglie del peso e dei video: `Settings.frontFacts`. */
    frontFacts: Boolean = Settings().frontFacts,
    /** Se l'intestazione ha la pastiglia 'Seleziona tutto': `Settings.frontPickAll`. */
    frontPickAll: Boolean = Settings().frontPickAll,
    /**
     * Il colore scelto per questa cartella, come indice in [FRONT_TINTS], oppure `null` per
     * quello dell'app.
     *
     * È un dato della cartella e non un'impostazione (il perché è su `ViewerViewModel.tint`):
     * arriva già risolto.
     */
    frontTint: Int? = null,
    /** Che cosa fare quando l'utente sceglie un colore, o lo toglie. */
    onFrontTint: (Int?) -> Unit = {},
    /**
     * Quanto pesa la cartella, quante immagini e quanti video ha, e dov'è: per le pastiglie e
     * per la rinomina.
     *
     * Arriva contato da `Folder.weigh`, una volta per cartella: la griglia ha gli indirizzi e
     * non le righe del MediaStore.
     */
    facts: Folder.Facts = Folder.Facts(),
    /**
     * Il nome della cartella dentro cui si sta cercando, e `null` per la ricerca in tutta la
     * galleria.
     *
     * È al posto dell'invito del campo: senza, niente direbbe che i risultati si fermano a una
     * cartella. Il nome è un dato, quindi non costa una stringa da tradurre.
     */
    searchIn: String? = null,
    /**
     * Quante colonne di miniature, sul lato corto dello schermo: `Settings.folderColumns`.
     *
     * La stessa voce che governa l'elenco iniziale (richiesta dell'utente, 1.66: un'impostazione
     * globale). La chiave e il valore di fabbrica non si toccano, o chi aggiorna perde la sua
     * scelta.
     * ⚠️ **Il valore di riserva è quello di fabbrica**, come per tutti i parametri che ne hanno
     * uno: un valore che coincide per caso smetterebbe di coincidere senza che nessuno se ne
     * accorga.
     */
    columns: Int = Settings().folderColumns,
    /**
     * I campi delle informazioni sul file, nell'ordine scelto: `Settings.factRows`.
     *
     * Arriva un elenco e non le impostazioni intere: passarle tutte farebbe ricomporre la griglia
     * a ogni ritocco di una voce che qui non c'entra.
     * I valori di serie servono al banco: `IntestazioneTest` monta questa schermata vera coi soli
     * argomenti che misura. Sono i valori di fabbrica dell'impostazione.
     */
    factFields: List<FactField> = Settings().factRows,
    /**
     * Se il cestino è acceso. Vedi `Settings.binOn`.
     *
     * Decide due cose insieme: se 'elimina' sposta nel cestino o cancella, e se prima compare
     * una conferma (il perché è su [FileJob.Delete]). Il valore di serie è quello di fabbrica
     * (vedi [factFields]).
     */
    binOn: Boolean = Settings().binOn,
    /** Che cosa il filtro volatile lascia vedere. Vedi `ViewerViewModel.gridFilter`. */
    filter: MediaKind = MediaKind.ALL,
    onFilter: (MediaKind) -> Unit = {},
    /**
     * Apre la ricerca dei file per nome: il tocco lungo sul filtro.
     *
     * È la stessa della voce 'Cerca' dell'elenco iniziale, quindi cerca in tutta la galleria.
     *
     * ⚠️ **Senza valore di riserva**: con un `{}` di serie, un chiamante dimenticato avrebbe un
     * gesto che vibra e non fa niente, senza che niente lo segnali. Così chi monta questa griglia
     * deve dire dove porta il tocco lungo, o non compila.
     */
    onSearch: () -> Unit,
    /** Se 'Copia lista' mette anche il percorso in testa. Vedi `Settings.listPath`. */
    listPath: Boolean = Settings().listPath,
    /** Se in testa alla selezione si legge il peso. Vedi `Settings.pickWeight`. */
    pickWeight: Boolean = Settings().pickWeight,
    /**
     * Se sotto ogni miniatura si legge il nome del file. Vedi `Settings.gridNames`.
     *
     * Il valore di serie è quello di fabbrica, spento (vedi [factFields]).
     */
    gridNames: Boolean = Settings().gridNames,
    /**
     * Con che segno si riconosce l'ultimo media visualizzato. Vedi `Settings.lastMark`.
     *
     * Il valore di serie è quello di fabbrica, la cornice (vedi [factFields]).
     */
    lastMark: LastMark = Settings().lastMark,
    /**
     * Se questa griglia è il **cestino**.
     *
     * Cambia tre cose e non l'aspetto: 'elimina' diventa definitiva, 'rinomina' diventa
     * 'ripristina' (richiesta dell'utente), e il FAB compare anche senza selezione, per
     * 'svuota il cestino'. Il resto è la griglia di sempre: il cestino si naviga come una
     * cartella qualunque.
     */
    bin: Boolean = false,
    /**
     * Apre la cronologia dei ripristini. Vale **solo** quando [bin] è vero.
     *
     * Il valore di serie non fa niente: fuori dal cestino la voce che lo chiama non esiste.
     */
    onHistory: () -> Unit = {},
    /**
     * Dove manda 'Cestino' nel menu del FAB, e `null` quando di qui non ci si va.
     *
     * Nullo di serie: la ricerca e la cartella d'avvio montano questa griglia per mostrare un
     * elenco, non per navigare l'app. Con questo e [onSettings] nulli, in una cartella il FAB
     * non compare.
     */
    onBin: (() -> Unit)? = null,
    /** Dove manda 'Impostazioni' nel menu del FAB. Vedi [onBin]. */
    onSettings: (() -> Unit)? = null,
    /**
     * Apre la ricerca **dentro questa cartella**, dal menu del FAB. Vedi [onBin].
     *
     * Non è [onSearch], che cerca in tutta la galleria: qui il confine è la cartella aperta
     * (risposta a `d-fab-voci`). Il bucket lo conosce il chiamante, quindi arriva un gesto e non
     * un numero.
     */
    onSearchHere: (() -> Unit)? = null,
    /**
     * Comincia a scegliere la copertina di questa cartella: è il **tocco sull'icona**
     * dell'intestazione. Nullo quando di qui non si sceglie niente, come [onBin].
     *
     * Il gesto l'ha scelto l'utente (risposta a `d-copertina-come`). Comincia e basta:
     * l'immagine si sceglie dopo, da qualsiasi cartella, quindi parte una modalità e non una
     * finestra (vedi `ViewerViewModel.covering`).
     */
    onCoverPick: (() -> Unit)? = null,
    /**
     * Lascia la cartella per l'elenco iniziale dopo che la scelta di una copertina è partita
     * (richiesta dell'utente; il caso in cui non si esce è su `ViewerViewModel.coverAway`).
     * La prima volta si esce quando il mini onboarding si chiude, perché indica l'icona di questa
     * cartella; le altre volte col tocco stesso.
     */
    onCoverAway: () -> Unit = {},
    /** Toglie la copertina scelta: la voce del menu del FAB. Vedi [coverSet]. */
    onCoverClear: (() -> Unit)? = null,
    /**
     * Se questa cartella ha già una copertina scelta a mano.
     *
     * Governa la sola voce che la toglie, che senza copertina non si offre (come 'Mostra
     * nascoste' nell'elenco iniziale).
     */
    coverSet: Boolean = false,
    /**
     * Avvisa che la griglia ha una selezione viva, cioè che una rilettura le farebbe danno.
     *
     * ⚠️ **Serve all'aggiornamento automatico**: quando arriva un file da fuori il modello
     * rilegge la cartella, e una lista nuova azzera la selezione (`remember(items)`, più sotto).
     * Il resto è su `ViewerViewModel.gridBusy`. Il valore di serie non fa niente: nel banco non
     * c'è un modello da avvisare.
     */
    onBusy: (Boolean) -> Unit = {},
    /**
     * Se la scelta di una copertina è in corso **per questa cartella**.
     *
     * Serve solo al mini onboarding della prima scelta: la modalità vive nel modello, perché fra
     * l'inizio e la scelta si cambia schermata. Per questa cartella e non per una qualunque: in
     * un'altra il mini onboarding indicherebbe l'icona sbagliata.
     */
    coverHere: Boolean = false,
    /**
     * Rinomina la cartella aperta: il tocco lungo sul nome dell'intestazione.
     *
     * `null` dove non c'è una cartella da rinominare: nel cestino, nella ricerca e nel banco.
     * La rinomina la fa il modello, che tocca il disco, cambia l'identificatore della cartella
     * (e con lui copertina e colore) e riapre la griglia: da qui esce il solo nome nuovo.
     */
    onFolderRename: ((String) -> Unit)? = null,
    /**
     * La forma dello schermo (vedi [Adaptive.shape]): decide dove sono l'intestazione e i
     * comandi del FAB.
     *
     * La decide chi compone le colonne, che sa se accanto c'è l'elenco delle cartelle. Il valore
     * di serie è il telefono, la griglia che il banco monta.
     */
    shape: Adaptive.Shape = Adaptive.Shape.PHONE,
    /**
     * La colonna delle cartelle sullo schermo largo, che questa schermata compone da sé, o `null`.
     *
     * La compone la griglia: in cima alla colonna ci sono le pastiglie dell'intestazione
     * (richiesta B2), e 'Seleziona tutto' agisce sulla selezione, che è uno stato di questa
     * schermata. Composta fuori, la colonna dovrebbe ricevere e restituire la selezione.
     */
    rail: WideRail? = null
) {
    val wide = shape == Adaptive.Shape.WIDE
    val tall = shape == Adaptive.Shape.TALL
    val state = rememberLazyGridState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Le risorse da `LocalResources` e non da `context.resources`, che non segue i cambi di
    // configurazione (lingua, rotazione). Si leggono mentre si compone e si usano nelle
    // coroutine.
    val res = LocalResources.current

    /**
     * Gli indirizzi scelti, non le posizioni.
     *
     * ⚠️ **Non le posizioni**: la lista si ricarica, e un indice diventerebbe un'altra immagine
     * senza che niente lo dica; un indirizzo che non c'è più sparisce dalla selezione da sé.
     * Vive nella schermata e non nel modello: uscire da una cartella vuol dire 'lascia stare'.
     * Ma sopravvive alla rotazione (riscontro dell'utente), che ricrea l'attività.
     * Il salvatore passa da una lista: il `Bundle` non scrive un `Set`, ma scrive gli `Uri`, che
     * sono `Parcelable`.
     */
    var chosen by rememberSaveable(items, stateSaver = UriSetSaver) {
        mutableStateOf<Set<Uri>>(emptySet())
    }
    val menu = rememberMenuState()

    /*
     * ⚠️ **Il pannello non ha un interruttore proprio**: si vede esattamente quando c'è una
     * selezione (richiesta dell'utente). Un interruttore separato ammetterebbe una selezione viva
     * e senza pannello, in cui non si capisce di essere.
     */

    /** Quanto è alto il pannello, in pixel, per lasciargli il posto sotto la griglia. */
    var sheetTall by remember { mutableIntStateOf(0) }

    /**
     * La selezione che il gesto Indietro ha appena azzerato, e `null` quando non c'è niente da
     * rimettere. È lei a far comparire la notifica con 'Annulla'.
     *
     * Indietro azzera senza chiedere e offre di disfare per tre secondi (istruzione
     * dell'utente): un dialogo si paga sempre, una notifica solo se serve.
     * Tiene la selezione e non un booleano: 'Annulla' deve rimettere quelle immagini.
     * ⚠️ **La chiave è il titolo**, che cambia solo uscendo dalla cartella (l'offerta vale finché
     * non si cambia cartella, come per [worked]): con `items` si azzererebbe a ogni ricarica.
     */
    var cleared by remember(title) { mutableStateOf<Set<Uri>?>(null) }

    /*
     * La riga va al canale unico degli avvisi (risposta `casa` a `d-avvisi`), dove sono la durata
     * e il conto alla rovescia (`Notice.kt`); qui resta quale selezione rimettere. Con la chiave
     * [cleared], un altro Indietro rimette la notifica per tre secondi suoi.
     * La riga vive nel processo, quindi 'finché non si cambia cartella' si ottiene togliendola
     * col congedo qui sotto.
     */
    val azzerata = stringResource(R.string.pick_cleared)
    val annullaAzzerata = stringResource(R.string.pick_undo)
    var rigaAzzerata by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(cleared) {
        val quali = cleared ?: return@LaunchedEffect
        rigaAzzerata = Notices.offer(
            text = azzerata,
            action = annullaAzzerata,
            onGone = { cleared = null }
        ) {
            // ⚠️ La selezione catturata quando la riga è stata creata, non `cleared` riletto
            // adesso: il congedo l'ha già azzerato, e si rimetterebbe una selezione vuota.
            chosen = quali
        }
    }
    /*
     * Il congedo ha per chiave il titolo, come [cleared]. Toglie la sua riga e non quella in
     * scena: nei tre secondi può esserne arrivata un'altra.
     */
    DisposableEffect(title) {
        onDispose { rigaAzzerata?.let { Notices.dismiss(it) } }
    }

    /**
     * Se in questa visita si è già eseguita un'operazione sui file.
     *
     * ⚠️ **La chiave è il titolo e non `items`**: dopo un'operazione la lista si ricarica, e una
     * bandierina legata a lei si azzererebbe proprio quando serve leggerla.
     */
    var worked by remember(title) { mutableStateOf(false) }

    /**
     * Il dialogo di un'operazione, e `null` quando non ce n'è aperto nessuno.
     *
     * Uno stato solo per tutti i dialoghi, che così si escludono per costruzione; contiene le
     * immagini su cui lavorare (vedi [FileJob]).
     * Sopravvive alla rotazione (riscontro dell'utente): che cosa si salva è su [FileJobSaver].
     */
    var job by rememberSaveable(stateSaver = FileJobSaver) { mutableStateOf<FileJob?>(null) }

    /** Se si sta chiedendo di svuotare il cestino. Vale solo quando [bin] è vero. */
    var emptying by rememberSaveable { mutableStateOf(false) }
    /** Se la conferma di 'Ripristina tutto' è in scena. Vedi la nota su quella voce. */
    var restoringAll by rememberSaveable { mutableStateOf(false) }
    /*
     * In AIV Play la conferma è la finestra di Android, che chiede già prima di ripristinare o
     * eliminare per sempre; in AIV GitHub il cestino è dell'app, e chiede AIV.
     */
    val askRestoreAll = { if (Store.files) restoringAll = true else job = FileJob.Restore(items.orEmpty()) }
    val askEmpty = { if (Store.files) emptying = true else job = FileJob.Delete(items.orEmpty(), forGood = true) }
    /**
     * Se il selettore del colore è in scena: lo apre il tocco lungo sull'icona
     * dell'intestazione.
     *
     * È qui e non accanto all'intestazione: la finestra è col resto dei dialoghi, e chi la apre è
     * in un nodo che si rimisura a ogni pixel di scorrimento.
     */
    var tinge by rememberSaveable { mutableStateOf(false) }
    /**
     * Se la finestra che rinomina la cartella è in scena: la apre il tocco lungo sul nome
     * dell'intestazione.
     *
     * È qui accanto a [tinge] per la stessa ragione.
     */
    var rinomina by rememberSaveable { mutableStateOf(false) }
    val picking = chosen.isNotEmpty()

    // In un effetto: avvisare il modello è un cambio di stato fuori da qui, e durante la
    // composizione si scriverebbe e leggerebbe lo stesso dato nello stesso giro. La chiave è il
    // 'se' e non l'insieme.
    LaunchedEffect(picking) { onBusy(picking) }
    /*
     * ⚠️ **Una selezione nuova toglie l'offerta di disfare**: 'Annulla' non saprebbe se
     * sostituirla o sommarsi, e in tutti e due i casi toglierebbe qualcosa appena scelto.
     * È un effetto su `picking` e non una riga nei tre punti in cui una selezione inizia.
     */
    LaunchedEffect(picking) { if (picking) cleared = null }
    // ⚠️ Uscendo dalla griglia il modello va liberato: senza, crederebbe a una selezione viva e
    // non rileggerebbe più da sé.
    DisposableEffect(Unit) { onDispose { onBusy(false) } }

    /**
     * Se c'è qualcosa qui dentro.
     *
     * Serve alle voci del cestino che agiscono su tutto (svuota e ripristina tutto), che su un
     * cestino vuoto non si offrono, e alla scorciatoia del tocco lungo, che allora vibra e basta.
     */
    val filled = !items.isNullOrEmpty()

    /*
     * ⚠️ **Indietro esce dalla selezione prima che dalla cartella**: azzera subito e offre di
     * disfare (il perché è su [cleared]). Una riga nuova sul canale degli avvisi prende il posto
     * di quella in scena (`Notices.posa` chiama il suo congedo).
     */
    BackHandler(enabled = picking) {
        cleared = chosen
        chosen = emptySet()
    }

    /**
     * Dov'è il dito mentre trascina una selezione, e `null` quando non trascina.
     *
     * Serve anche allo scorrimento ai bordi, che legge la posizione fuori dai richiami del gesto
     * mentre il dito è fermo in fondo allo schermo.
     */
    var dragAt by remember { mutableStateOf<Offset?>(null) }

    /** Da dove è partita la selezione da/a. Null quando non si sta trascinando. */
    var dragFrom by remember { mutableStateOf<Int?>(null) }

    /**
     * La selezione com'era **prima** che questo trascinamento cominciasse.
     *
     * La selezione si ricostruisce a ogni fotogramma come 'quella di prima più l'intervallo di
     * adesso': accumulando, tornare indietro col dito non toglierebbe niente.
     */
    var dragBase by remember { mutableStateOf<Set<Uri>>(emptySet()) }

    /**
     * Il VERSO del trascinamento: `true` toglie, `false` aggiunge.
     *
     * Lo decide l'immagine su cui il gesto comincia (richiesta dell'utente): da una già scelta
     * il trascinamento toglie l'intervallo, da una libera lo aggiunge, come in ogni galleria.
     * È fuori dai richiami del gesto perché l'intervallo lo ricostruisce l'effetto qui sotto.
     */
    var dragOff by remember { mutableStateOf(false) }

    // Quale vibrazione, e perché, è su [HOLD_BUZZ].
    val haptics = LocalHapticFeedback.current

    /**
     * Se il mini onboarding del tocco lungo sul FAB si è già visto.
     *
     * ⚠️ **Il valore di partenza è `true`, 'già visto'**: il valore vero arriva dall'archivio
     * dopo la prima composizione, e partendo da `false` il mini onboarding comparirebbe per un
     * fotogramma anche a chi l'ha già chiuso. Così, al peggio compare un fotogramma più tardi.
     */
    val binSeen by produceState(initialValue = true, context) {
        Hint.BIN_EMPTY.flow(context).collect { value = it }
    }

    /**
     * Se il mini onboarding della copertina si è già visto. Stessa ragione del valore di partenza
     * del suo gemello qui sopra.
     */
    val coverSeen by produceState(initialValue = true, context) {
        Hint.COVER.flow(context).collect { value = it }
    }

    /**
     * Dov'è l'icona dell'intestazione, in coordinate della radice.
     *
     * Si misura, perché il mini onboarding che la evidenzia deve cadere sopra di lei: la
     * posizione dipende dai rientri di sistema, dalla testata, da quanto l'intestazione è aperta
     * e da quanto l'icona si stringe scorrendo (vedi [HintSpot]). `null` finché non è disegnata,
     * e allora il mini onboarding non compare.
     */
    var iconaSpot by remember { mutableStateOf<Rect?>(null) }

    /**
     * 'Tutte': seleziona ogni elemento della griglia.
     *
     * Una variabile perché lo chiamano tre posti (il tasto 'Tutti' del pannello, il tocco lungo
     * sul FAB e la sua copia nel mini onboarding), con la stessa vibrazione.
     */
    val takeAll: () -> Unit = {
        haptics.performHapticFeedback(HOLD_BUZZ)
        chosen = items?.toSet() ?: emptySet()
    }

    /**
     * Se tutto quello che c'è in questa griglia è già scelto. Vuota, non conta come 'tutto'.
     *
     * ⚠️ **Il tocco lungo sul FAB non deseleziona**: con una selezione in corso il FAB non è in
     * scena (`!picking` in `visible` di [FabPop]), e un ramo così sarebbe codice morto. Il
     * rovescio è il tocco lungo sul tasto 'Tutti' del pannello e la pastiglia dell'intestazione
     * (domanda `d-fab-tutto`).
     * È qui perché lo leggono in due: chi disegna la pastiglia e chi decide che cosa annuncia il
     * tocco lungo.
     */
    val allTaken = !items.isNullOrEmpty() && chosen.containsAll(items.orEmpty())

    /**
     * Il tocco lungo è la scorciatoia della voce principale del tocco breve (richiesta
     * dell'utente): nel cestino senza selezione è 'Svuota il cestino', altrimenti è 'Tutte'.
     * ⚠️ **Il cestino non si svuota mai con una selezione in corso**: chi ha scelto tre immagini
     * da ripristinare si aspetta che il gesto agisca su quelle.
     * Lo svuotamento passa da `askEmpty`: in AIV GitHub il dialogo (`emptying`), in AIV Play la
     * finestra di Android.
     */
    val shortcut: () -> Unit = if (bin && !picking) {
        {
            haptics.performHapticFeedback(HOLD_BUZZ)
            if (filled) askEmpty()
        }
    } else takeAll

    /**
     * Come TalkBack chiama il tocco lungo, che deve dire la stessa cosa che [shortcut] fa.
     *
     * ⚠️ Senza, il lettore di schermo annuncerebbe 'seleziona tutte' su un gesto che svuota il
     * cestino. Non c'è un caso per 'tutto già scelto': vedi [allTaken].
     */
    val shortcutLabel = if (bin && !picking) R.string.bin_empty else R.string.pick_all

    /**
     * La bandierina locale toglie subito il mini onboarding: l'archivio risponde con un giro di
     * ritardo, e per un fotogramma o due il menu si aprirebbe sotto di lui. La scrittura serve
     * alle sessioni dopo.
     */
    var binOff by remember { mutableStateOf(false) }

    /**
     * Quale mini onboarding è in scena adesso, se ce n'è uno. Un `when`, così uno nuovo si
     * aggiunge come ramo.
     */
    val pillLook = LocalPillLook.current
    val hint: Hint? = when {
        // Insegna il tocco lungo del FAB: sugli schermi larghi e alti il FAB non c'è, e la
        // pillola estesa non ha il tocco lungo (risposta B1).
        bin && !binSeen && !binOff && shape == Adaptive.Shape.PHONE &&
            pillLook.mode != PhonePill.EXTENDED -> Hint.BIN_EMPTY
        else -> null
    }

    /**
     * Il mini onboarding si archivia appena l'utente fa la cosa che insegnava, o la salta.
     *
     * Il ramo `null` non fa niente: questa funzione la chiama anche il FAB vero, e un tocco lungo
     * ordinario non deve archiviare un mini onboarding mai mostrato.
     */
    val hintDone: () -> Unit = {
        when (hint) {
            Hint.BIN_EMPTY -> binOff = true
            // Gli altri mini onboarding vivono in altre schermate (o, quello della copertina,
            // si archiviano da sé): i rami ci sono perché il `when` sull'enum è completo.
            Hint.COLUMNS, Hint.ZOOM_TAP, Hint.EXT_WARN, Hint.COVER, Hint.MODULES,
            Hint.EDITOR_TOOLS, Hint.DRAW, Hint.FEED -> Unit
            null -> Unit
        }
        hint?.let { seen -> scope.launch { seen.remember(context) } }
    }

    /**
     * Le dieci operazioni sulla selezione (richiesta dell'utente, con le sue etichette brevi):
     * anche quelle che non toccano file ('Tutti', 'Nessuno', 'Inverti') sono qui, perché si
     * fanno sulla selezione. L'ordine di fabbrica è sbilanciato a destra apposta, per il pollice.
     * Nessuna voce chiude il pannello, che è la selezione: il dialogo dell'operazione gli si
     * disegna sopra, e alla fine `perform` svuota la selezione.
     *
     * ⚠️ **La lista non è ricordata, ed è una scelta**: le chiusure catturano quattro parametri
     * più contesto, risorse e ambito, e un `remember` vorrebbe sette chiavi da tenere allineate
     * a mano; una chiave dimenticata congelerebbe un valore senza nessun errore. Il costo evitato
     * è per ricomposizione della schermata, non per fotogramma, perché `chosen` ha l'uguaglianza
     * strutturale.
     */
    val pickActions = listOf(
        // Nel cestino al posto della rinomina c'è il ripristino (richiesta dell'utente), allo
        // stesso posto, così le icone non si spostano.
        if (bin) {
            PadAction(PadKey.RENAME, Glyphs.BinRestore, R.string.bin_restore) {
                job = FileJob.Restore(chosen.toList())
            }
        } else {
            PadAction(PadKey.RENAME, Glyphs.TextCursor, R.string.pick_rename) {
                job = FileJob.Rename(chosen.toList())
            }
        },
        PadAction(PadKey.INFO, Icons.Outlined.Info, R.string.pick_info) {
            job = FileJob.Facts(chosen.toList())
        },
        PadAction(PadKey.MOVE, Glyphs.FolderPairDashed, R.string.pick_move) {
            job = FileJob.Transfer(chosen.toList(), move = true)
        },
        // Il tocco lungo su 'Copia' duplica nella cartella (richiesta dell'utente). Nel cestino
        // no: un duplicato non avrebbe una riga d'archivio, e non si potrebbe ripristinare.
        PadAction(
            key = PadKey.COPY,
            icon = Glyphs.FolderPair,
            label = R.string.menu_copy_here,
            onHold = if (bin) null else {
                {
                    job = FileJob.Duplicate(chosen.toList())
                }
            },
            holdLabel = if (bin) null else R.string.pick_duplicate
        ) {
            job = FileJob.Transfer(chosen.toList(), move = false)
        },
        PadAction(PadKey.SHARE, Icons.Default.Share, R.string.menu_share) {
            // La lista si prende adesso: dentro la coroutine `chosen` potrebbe essere cambiata.
            val list = chosen.toList()
            scope.launch { ImageActions.shareMany(context, list) }
        },
        /*
         * ⚠️ **Il tocco lungo salta il cestino** (richiesta dell'utente): è la sola scorciatoia
         * irreversibile, quindi la conferma resta. Col cestino spento il tocco breve fa già la
         * stessa cosa, e la scorciatoia non c'è.
         */
        PadAction(
            key = PadKey.DELETE,
            icon = Glyphs.PickDelete,
            label = R.string.pick_delete,
            onHold = if (bin || !binOn) null else {
                {
                    job = FileJob.Delete(chosen.toList(), forGood = true)
                }
            },
            holdLabel = if (bin || !binOn) null else R.string.pick_forever
        ) {
            // Definitiva nel cestino o col cestino spento: `forGood` chiede anche la conferma.
            job = FileJob.Delete(chosen.toList(), forGood = bin || !binOn)
        },
        // 'Lista' dà un avviso, con la frase dettata dall'utente: l'avviso di sistema dice solo
        // che qualcosa è negli appunti.
        PadAction(PadKey.LIST, Icons.AutoMirrored.Outlined.FormatListBulleted, R.string.pick_list) {
            val list = chosen.toList()
            scope.launch {
                // Il percorso si chiede per un elemento solo: sono tutti nella stessa cartella.
                val head = if (listPath) factsOf(context, list.take(1)).one?.folder else null
                ImageActions.copyNames(context, list, head)
                Notices.say(res.getString(R.string.pick_list_done))
            }
        },
        // Il tocco lungo su 'Tutti' fa il contrario, come chiesto dall'utente.
        PadAction(
            key = PadKey.ALL,
            icon = Glyphs.PickAll,
            label = R.string.pick_all_short,
            onHold = { chosen = emptySet() },
            holdLabel = R.string.pick_none
        ) {
            takeAll()
        },
        PadAction(PadKey.NONE, Glyphs.PickNone, R.string.pick_none) {
            chosen = emptySet()
        }
    ,
        // L'inversione lavora sull'elenco che si ha davanti: con una ricerca o un filtro,
        // `items` è già quello filtrato.
        PadAction(PadKey.INVERT, Glyphs.PickInvert, R.string.pick_invert) {
            chosen = items.orEmpty().toSet() - chosen
        })
        // L'ordine è quello scelto dall'utente (di fabbrica: vedi `PICK_KEYS`), diverso da
        // quello del menu del tocco lungo, che ha altre voci.
        // In AIV Play copia e sposta non ci sono (`Store.allows`); 'Elimina' e 'Rinomina' ci
        // sono da Android 11, perché passano dal MediaStore.
        .filter { Store.allows(it.key) }
        .inOrder(LocalPadLook.current.pick)

    // Le due misure dello scorrimento ai bordi, in pixel: servono dentro un effetto, che
    // non ha una densità sotto mano.
    val density = LocalDensity.current
    val edgePx = with(density) { EDGE_BAND.toPx() }
    val speedPx = with(density) { EDGE_SPEED.toPx() }

    /*
     * ⚠️ **Quale immagine è già stata servita, non un sì o no**: lo stato della schermata
     * sopravvive (il `SaveableStateProvider` di `AivApp`), e una bandierina non tornerebbe mai a
     * zero. Con l'indice, alla rotazione non si salta, e tornando dal visualizzatore su un'altra
     * immagine sì. Meno uno e non `null`: un `Int?` vorrebbe un `Saver`.
     */
    var servito by rememberSaveable { mutableStateOf(-1) }

    /*
     * Tornando dal visualizzatore, la griglia si porta sull'immagine che si stava guardando se
     * non è in vista (richiesta dell'utente). La griglia riparte da dov'era da sé: qui si
     * corregge il caso in cui nel visualizzatore si è sfogliato fino a un'altra immagine.
     * ⚠️ **Si aspetta la prima misura**: al primo giro nessun riquadro è disposto, e 'non è in
     * vista' sarebbe vero anche per un'immagine nella prima schermata.
     * In vista vuol dire il riquadro intero, non un pezzo tagliato dal bordo.
     */
    LaunchedEffect(items, highlight) {
        if (items == null || highlight == null || highlight == servito) return@LaunchedEffect
        if (highlight !in items.indices) return@LaunchedEffect
        servito = highlight
        snapshotFlow { state.layoutInfo.totalItemsCount }.first { it > 0 }
        val info = state.layoutInfo
        val seen = info.visibleItemsInfo.firstOrNull { it.index == highlight }
        val whole = seen != null &&
            seen.offset.y >= 0 &&
            seen.offset.y + seen.size.height <= info.viewportSize.height
        if (!whole) state.scrollToItem(highlight)
    }

    /**
     * Quanto è larga una cella, in pixel, e zero finché la griglia non ha misurato.
     *
     * Si chiede alla griglia e non si ricalcola: la larghezza la ricava Compose dividendo lo
     * spazio, e una seconda formula potrebbe divergere dal suo arrotondamento.
     * ⚠️ **`derivedStateOf`**: `layoutInfo` cambia a ogni fotogramma di scorrimento, la
     * larghezza di una cella no, e senza il filtro ogni miniatura si ricomporrebbe a ogni pixel.
     * Serve solo al nome sotto la miniatura, che `fitName` accorcia a misura.
     */
    val cellPx by remember(state) {
        derivedStateOf { state.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.width ?: 0 }
    }

    /**
     * Il giro di ogni operazione sui file: si parte, si dice com'è andata, si rilegge la
     * cartella.
     *
     * ⚠️ **La selezione si svuota subito**: le spunte accese inviterebbero a ripetere la voce
     * mentre la prima è in corso. Chi chiama ha già preso la sua lista, che [work] riceve da
     * fuori. Il testo dell'esito lo compone `outcomeText`, condiviso col visualizzatore.
     */
    val perform: (FileKind, suspend () -> FileTree.Outcome) -> Unit = { kind, work ->
        chosen = emptySet()
        // In [FileWork] e non nell'ambito della schermata: vedi la nota là.
        FileWork.scope.launch {
            val out = work()
            // Il cestino tace ([FileKind.speaks]): la sua notifica dice la stessa cosa e offre
            // di disfare.
            if (kind.speaks(out)) {
                Notices.say(outcomeText(res, out, kind), NOTICE_LONG_MS)
            }
            worked = true
            onChanged()
        }
    }

    /*
     * Se la cartella si svuota operando, si risale (richiesta dell'utente).
     * Solo dopo un'operazione fatta qui ([worked]): una cartella già vuota all'ingresso si apre
     * e si guarda. Non nel cestino, dove svuotare è lo scopo e il vuoto ha la sua frase, e non
     * nella ricerca, dove il vuoto vuol dire 'nessun risultato'.
     */
    LaunchedEffect(worked, items) {
        if (worked && !bin && query == null && items?.isEmpty() == true) onBack()
    }

    /*
     * ⚠️ **La radice è un `Box` senza margini propri** perché il velo dell'onboarding deve
     * coprire tutto lo schermo, testata e margini di sistema compresi: dentro la `Column` i
     * rientri sono già applicati. Un margine spostato sul `Box` rimette il difetto.
     * Misura anche l'altezza per l'intestazione ([HEADER_SHARE]): i rientri di sistema si
     * sottraggono a mano, perché la frazione è dell'area utile ma il riquadro deve arrivare al
     * vetro.
     */
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    // ⚠️ La larghezza si cattura qui: dentro la colonna il receiver di [BoxWithConstraints] è
    // coperto da quello della colonna, e `maxWidth` non si raggiunge.
    val larghezza = maxWidth

    /*
     * L'intestazione di una cartella: l'icona semitrasparente al posto di quella della schermata
     * iniziale e il titolo della cartella sotto, poi la griglia. Fascia e sfumatura vivono in
     * `Front.kt`, condivise con la schermata iniziale; qui si decide dove comincia e quando è
     * chiusa.
     * Né nel cestino, dove il FAB c'è sempre e la sfumatura che lo tiene su un fondo neutro non
     * potrebbe andarsene scorrendo, né nella ricerca, dove la testata ha un campo di testo e non
     * un titolo.
     */
    val density = LocalDensity.current
    val bordi = steadyDrawing()
    /*
     * Sullo schermo largo la fascia non c'è, dalla `3.70` (richiesta B1): la griglia prende tutto
     * lo spazio, e quello che la fascia mostrava, tranne il titolo, è nella testa della colonna
     * delle cartelle ([headed]).
     */
    val front = !bin && query == null && !wide
    val headed = !bin && query == null
    val headerMax = if (front) {
        (maxHeight - with(density) { (bordi.getTop(this) + bordi.getBottom(this)).toDp() }) *
            HEADER_SHARE
    } else {
        0.dp
    }
    val headerPx = with(density) { headerMax.toPx() }

    /**
     * Quanti pixel di intestazione sono già stati chiusi, da 0 a tutto.
     *
     * ⚠️ La chiave è la misura: ruotando il telefono l'altezza cambia, e un valore di chiusura
     * vecchio non vorrebbe più dire niente.
     */
    var shut by remember(headerPx) { mutableFloatStateOf(0f) }
    val aperto = remember(headerPx) { { frontOpen(headerPx, shut) } }
    val paging = remember(headerPx) {
        frontScroll(
            quanto = headerPx,
            chiuso = { shut },
            chiudi = { shut = it },
            inCima = {
                state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0
            }
        )
    }

    /*
     * ⚠️ **La selezione non chiude l'intestazione, dalla `1.78`** (richiesta dell'utente):
     * chiudendola, la griglia scorreva sotto il dito appoggiato e il gesto da/a della selezione
     * prendeva le miniature che passavano. Il conto dei selezionati è sotto il titolo, quindi
     * la testata non va liberata, e la fascia si chiude in un modo solo, scorrendo.
     */

    /*
     * ⚠️ **Con la griglia scorsa l'intestazione si chiude**, e questo copre il ritorno dal
     * visualizzatore. Col dito è già vero, perché [frontScroll] spende il trascinamento prima
     * che l'elenco si muova; il salto programmato all'immagine da cui si torna invece non passa
     * dallo scorrimento annidato. Di colpo e senza animazione: la schermata sta arrivando.
     */
    LaunchedEffect(headerPx) {
        snapshotFlow {
            state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0
        }.collect { scorsa -> if (scorsa) shut = headerPx }
    }

    /**
     * Il conto sotto il nome, in testata e nella fascia: i selezionati durante una selezione,
     * gli elementi della cartella fuori da lei.
     *
     * ⚠️ Si calcola una volta e si legge in due posti, perché le due copie si dissolvono l'una
     * nell'altra e devono dire lo stesso numero a metà corsa.
     */
    val conto = if (picking) {
        pluralStringResource(R.plurals.pick_count, chosen.size, chosen.size)
    } else {
        items?.let { pluralStringResource(R.plurals.items_count, it.size, it.size) }
    }

    /*
     * I gesti dell'intestazione: il tocco sul nome lo copia e il tocco lungo rinomina la
     * cartella; il tocco sull'icona sceglie la copertina e il tocco lungo il colore della
     * cartella.
     * ⚠️ Le stringhe si risolvono qui: `stringResource` è una funzione di composizione, e in una
     * lambda che parte da un tocco non si può chiamare.
     */
    val nomeCopiato = stringResource(R.string.front_name_copied)
    val rinominaEtichetta = stringResource(R.string.pick_rename)
    val tintaEtichetta = stringResource(R.string.front_tint)
    val copertinaEtichetta = stringResource(R.string.folder_cover)

    /*
     * ⚠️ Il tema dell'app si legge qui perché serve in fase di disegno: l'inchiostro dell'icona
     * lo sceglie una `graphicsLayer`, che gira fuori dalla composizione. È il tema di AIV e non
     * quello di sistema (`Rules.md`, § '🌗 Il tema scelto DENTRO l'app non è quello di sistema').
     */
    val chiaro = LocalAivLight.current
    /*
     * ⚠️ Il gesto si legge aggiornato: il riconoscitore dei tocchi nasce una volta sola
     * (`pointerInput(Unit)`), quindi una lambda catturata sarebbe quella del primo giro.
     */
    val scegliCopertina by rememberUpdatedState(onCoverPick)
    val viaPerCopertina by rememberUpdatedState(onCoverAway)
    val copiaNome = {
        ImageActions.copyName(context, title)
        Notices.say(nomeCopiato)
    }
    // Il tocco lungo sul nome rinomina la cartella, dalla `1.95` (richiesta dell'utente).
    val viaLungo = { if (onFolderRename != null) rinomina = true }

    /*
     * ⚠️ **Il rientro di sotto non è sul contenitore, dalla `1.90`** (richiesta dell'utente: la
     * griglia arriva al bordo inferiore, sotto la barra gestuale). Un rientro sul contenitore
     * toglie lo spazio prima che la griglia disegni; nel `contentPadding` della lista le
     * miniature scorrono sotto la barra e l'ultima riga resta raggiungibile. Anche il margine
     * verticale della schermata si scompone allo stesso modo: rientro in cima, spazio di
     * scorrimento in fondo. Il FAB vive in una finestra sua e i rientri se li mette da sé.
     */
    /*
     * Il glifo del FAB diventa il chevron scorrendo, dalla `2.07`: il gesto lo raccoglie il
     * contenitore e il disegno lo fa il FAB, quindi lo stato vive qui. Il meccanismo è in
     * `Jump.kt`.
     * ⚠️ La fascia chiusa entra nella stima del 'su': chiudendola la griglia non si muove, e da
     * sola direbbe di essere già in cima proprio quando il salto ha qualcosa da fare.
     */
    /*
     * Le voci della pillola sono quelle del menu del FAB, nello stesso ordine e con le stesse
     * etichette (dalla `3.70`): in una cartella Cerca, Cestino e Impostazioni; nel cestino
     * Cronologia, Ripristina tutto e Svuota il cestino, che si spengono a cestino vuoto. In
     * selezione la pillola non c'è, come il FAB. Dalla `4.00` c'è anche sul telefono in
     * verticale, se l'impostazione la chiede.
     */
    val cerca = stringResource(R.string.hub_search)
    val cestino = stringResource(R.string.bin_title)
    val impostazioni = stringResource(R.string.hub_settings)
    val pillEntries: List<PillEntry> = when {
        picking || (shape == Adaptive.Shape.PHONE && pillLook.mode == PhonePill.OFF) -> emptyList()
        // ⚠️ La Cronologia è di AIV GitHub: il cestino di sistema non registra da dove vengono i file.
        bin -> listOfNotNull(
            PillEntry(Glyphs.BinHistory, stringResource(R.string.bin_history), short = stringResource(R.string.bin_history)) { onHistory() }
                .takeIf { Store.files },
            PillEntry(Glyphs.BinRestore, stringResource(R.string.bin_restore_all), enabled = filled, short = stringResource(R.string.start_restore)) {
                askRestoreAll()
            },
            PillEntry(Icons.Default.DeleteForever, stringResource(R.string.bin_empty), enabled = filled, short = stringResource(R.string.start_empty)) {
                askEmpty()
            }
        )
        else -> listOfNotNull(
            onSearchHere?.let { PillEntry(Icons.Default.Search, cerca, short = cerca, onTap = it) },
            onBin?.let { PillEntry(Glyphs.Bin, cestino, short = cestino, onTap = it) },
            onSettings?.let { PillEntry(Icons.Default.Settings, impostazioni, short = impostazioni, onTap = it) }
        )
    }
    val pillShown = pillEntries.isNotEmpty()
    /** Quello che la pillola di vetro sfoca, cioè la griglia, o `null` fuori dal vetro. */
    val backdrop = rememberBackdrop()

    /*
     * ⚠️ L'icona e le pastiglie dell'intestazione sono scritte una volta sola e le chiamano due
     * posti: la fascia sul telefono e la testa della colonna delle cartelle sullo schermo largo
     * (dalla `3.70`, richiesta B2). Cambiano solo la misura e la visibilità, passate come
     * parametri: gesti, colori ed etichette restano gli stessi.
     */
    /*
     * Il pieno dell'icona segue il colore: la sagoma in negativo vuole tutto l'inchiostro, il
     * bianco sovrapposto il 40% scelto dall'utente. Dipende dalla sfumatura sotto l'icona, e le
     * sfumature sono due: la fascia del telefono segue l'interruttore, la testa della colonna lo
     * segue salvo il foro della fotocamera ([lavaTesta]).
     */
    fun pienoPer(lavata: Boolean) = when {
        !lavata -> FRONT_INK
        chiaro -> FRONT_NEG_INK
        else -> FRONT_DARK_INK
    }
    val pienoIcona = pienoPer(frontWash)
    val iconaCartella: @Composable (Modifier, Boolean, GraphicsLayerScope.() -> Float) -> Unit =
        { misura, lavata, inchiostro ->
            Icon(
                imageVector = Glyphs.FolderAiv,
                // La descrizione è quella del tocco; il tocco lungo si dichiara a parte.
                contentDescription = if (scegliCopertina != null) {
                    copertinaEtichetta
                } else {
                    tintaEtichetta
                },
                /*
                 * Nel tema scuro l'icona è bianca e sovrapposta, dalla `1.95` (richiesta
                 * dell'utente): il negativo è la sagoma della superficie, che nel tema scuro è
                 * quasi nera e non staccherebbe sulla tinta. Il tema è quello dell'app
                 * ([LocalAivLight]), non quello di sistema.
                 */
                tint = when {
                    !lavata -> LocalContentColor.current
                    chiaro -> MaterialTheme.colorScheme.surface
                    else -> Color.White
                },
                modifier = Modifier
                    .semantics {
                        onLongClick(label = tintaEtichetta) { tinge = true; true }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                haptics.performHapticFeedback(HOLD_BUZZ)
                                tinge = true
                            },
                            onTap = {
                                scegliCopertina?.invoke()
                                // ⚠️ La prima volta si resta, per il mini onboarding:
                                // vedi `onCoverAway`.
                                if (coverSeen) viaPerCopertina()
                            }
                        )
                    }
                    .then(misura)
                    // ⚠️ Il riquadro serve al velo del mini onboarding, che ci cade sopra:
                    // perché si misura è scritto su [iconaSpot].
                    .onGloballyPositioned { iconaSpot = it.boundsInRoot() }
                    .graphicsLayer { alpha = inchiostro() }
            )
        }
    // [inColumn] è la testa della colonna sullo schermo largo (vedi la nota sulla `FlowRow`).
    val pastiglie: @Composable (Boolean, () -> Float) -> Unit = { inColumn, quanto ->
        val pesa = frontFacts && facts.bytes > 0L
        val conta = frontFacts && facts.clips > 0
        val scatta = frontFacts && facts.shots > 0
        val tutti = items.orEmpty()
        // ⚠️ Lo stesso conto del tocco lungo sul FAB: due comandi che dicono la stessa
        // cosa non possono avere due idee di che cosa sia 'tutto'.
        val presi = allTaken
        val foto = remember(tutti) { tutti.filterNot { Videos.isVideo(it) }.toSet() }
        val clip = remember(tutti) { tutti.filter { Videos.isVideo(it) }.toSet() }
        if (pesa || conta || scatta || frontPickAll) {
            Spacer(Modifier.height(FRONT_CHIP_GAP))
            /*
             * La fila si allinea al lato del FAB, dalla `1.89` (richiesta dell'utente): quando
             * le pastiglie vanno a capo, la seconda riga va dalla parte del pollice. `FlowRow`
             * ha un allineamento solo per tutte le righe, quindi è della fila intera. Conta solo
             * quando si va a capo: la fila si dimensiona sul contenuto, e su una riga sola non
             * ha spazio da distribuire (misurato dal banco).
             * Nella testa della colonna, dalla `3.71`, la fila è centrata con 8 punti ai lati:
             * la colonna è stretta, i 24 punti del telefono la mandavano a capo, e lì il FAB non
             * c'è.
             */
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(
                    6.dp,
                    if (inColumn) Alignment.CenterHorizontally else fabEdge()
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .padding(horizontal = if (inColumn) 8.dp else 24.dp)
                    .graphicsLayer { alpha = quanto() }
            ) {
                if (pesa) {
                    FrontChip(
                        // Il formato dei pesi è uno solo: vedi [formatBytes].
                        text = formatBytes(facts.bytes),
                        tapLabel = stringResource(R.string.pick_all),
                        onTap = { chosen = tutti.toSet() },
                        holdLabel = stringResource(R.string.front_unpick),
                        onHold = { chosen = emptySet() }
                    )
                }
                if (scatta) {
                    FrontChip(
                        text = pluralStringResource(
                            R.plurals.folders_count,
                            facts.shots,
                            facts.shots
                        ),
                        tapLabel = stringResource(R.string.front_pick_images),
                        onTap = { chosen = chosen + foto },
                        holdLabel = stringResource(R.string.front_unpick_images),
                        onHold = { chosen = chosen - foto }
                    )
                }
                if (conta) {
                    FrontChip(
                        text = pluralStringResource(
                            R.plurals.folders_clips,
                            facts.clips,
                            facts.clips
                        ),
                        tapLabel = stringResource(R.string.pick_clips),
                        onTap = { chosen = chosen + clip },
                        holdLabel = stringResource(R.string.front_unpick_clips),
                        onHold = { chosen = chosen - clip }
                    )
                }
                if (frontPickAll) {
                    FrontPick(
                        picked = presi,
                        onTap = {
                            chosen = if (presi) emptySet() else tutti.toSet()
                        },
                        onHold = { chosen = emptySet() }
                    )
                }
            }
        }
    }
    val arm = rememberJumpArm(
        state = state,
        up = { state.jumpUpPixels() + shut },
        down = { state.jumpDownPixels() }
    )
    /*
     * La testa della colonna sullo schermo largo, dalla `3.70` (richiesta B2): l'icona e le
     * pastiglie della fascia, tutto tranne il titolo, sulla stessa sfumatura.
     * ⚠️ L'icona si misura sull'altezza della testa, fino alla misura piena della fascia: la
     * testa va dal 30 al 60% della colonna secondo quante cartelle ci sono.
     * Nella ricerca la testa è vuota, come la fascia.
     */
    /*
     * Il foro della fotocamera dal lato della colonna spegne la sfumatura, dalla `3.71` (nota
     * dell'utente): la colonna comincia dopo la fascia del foro, e la tinta accanto la faceva
     * sembrare un bordo.
     */
    val dir = LocalLayoutDirection.current
    val forata = rail?.let { r ->
        val ritaglio = WindowInsets.displayCutout
        val lato = if (r.onStart) ritaglio.getLeft(LocalDensity.current, dir) else ritaglio.getRight(LocalDensity.current, dir)
        lato > 0
    } ?: false
    val lavaTesta = frontWash && !forata
    val testa: @Composable BoxScope.() -> Unit = {
        if (headed) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
            ) {
                /*
                 * La sfumatura ha un tetto, [HEAD_WASH_MAX], dalla `3.71` (nota dell'utente sul
                 * banding del tablet): sulla testa alta mezza colonna la stessa rampa di colore
                 * faceva gradini larghi; col tetto è quella della fascia del telefono.
                 */
                if (lavaTesta) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(minOf(maxHeight, HEAD_WASH_MAX))
                            .frontWash(
                                tint = frontTintOf(frontTint) ?: MaterialTheme.colorScheme.primary,
                                air = 0.dp,
                                up = 0.dp,
                                bar = 0.dp,
                                ink = { 1f }
                            )
                    )
                }
                val lato = minOf(HEADER_ICON, maxHeight * HEAD_ICON_SHARE)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth()
                ) {
                    iconaCartella(Modifier.size(lato), lavaTesta) { pienoPer(lavaTesta) }
                    pastiglie(true) { 1f }
                }
            }
        }
    }
    /*
     * ⚠️ L'ombra sopra la scheda della selezione (nota E dell'utente sul giro della `4.45`): si
     * legge una volta qui e si passa alla colonna e a ogni cella, così le due metà di
     * `PickShade.kt` partono dallo stesso bordo.
     */
    val ombra = if (picking) sheetTop() else null
    RailFrame(rail = rail.takeIf { wide }, head = testa) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                /*
                 * ⚠️ **L'ombra dietro le celle è qui, prima dei margini, dalla `4.48`** (nota
                 * dell'utente su `4.46-04`, un rettangolo d'ombra che non doveva esserci): sulla
                 * griglia, dentro [GRID_PAD_X] e sopra la scheda, si fermava prima dei bordi.
                 * Questa colonna arriva al vetro su tre lati, e l'ombra continua piena sotto la
                 * scheda.
                 */
                .pickShade(ombra, behind = true)
                .windowInsetsPadding(
                    steadyDrawing().only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                )
                /*
                 * ⚠️⚠️ **L'ORDINE È MISURATO: il gesto si guarda prima di `paging`.** Il
                 * `nestedScroll` scritto per primo riceve per primo il delta della lista, e
                 * `frontScroll` ne consuma la parte con cui chiude l'intestazione: scritto dopo,
                 * al glifo arrivava zero finché la fascia aveva spazio da chiudere.
                 */
                .nestedScroll(arm.watch)
                .nestedScroll(paging)
                .padding(horizontal = GRID_PAD_X)
                .padding(top = GRID_PAD_Y)
        ) {
            /*
             * ⚠️⚠️ **TESTATA E FASCIA VIVONO IN UN BLOCCO SOLO, E LA TINTA SI DIPINGE DIETRO DI
             * LUI**, dalla `1.85`: su un nodo disegnato dopo la testata, il rettangolo che sconfina
             * verso l'alto copriva il titolo che stava comparendo. Il perché e la trappola sono su
             * [Modifier.frontWash].
             * Il blocco si accorcia da sé quando la fascia si chiude, e la tinta segue. Sconfina di
             * [GRID_PAD_Y] verso l'alto: fino al bordo dell'area sicura c'è solo il rientro
             * verticale della schermata, un numero noto.
             */
            Column(
                modifier = if (front && frontWash) {
                    Modifier.frontWash(
                        tint = frontTintOf(frontTint) ?: MaterialTheme.colorScheme.primary,
                        air = GRID_PAD_X,
                        up = GRID_PAD_Y,
                        // ⚠️ La fascia arriva fin sotto la barra di sistema (dalla `1.87`): il
                        // numero è il rientro che questa colonna prende con `steadyDrawing`,
                        // quindi il colore chiude esattamente il buco che quel rientro apre.
                        bar = with(density) { bordi.getTop(this).toDp() },
                        ink = aperto
                    )
                } else {
                    Modifier
                }
            ) {
            // ⚠️ La barra della selezione prende il posto del titolo: due barre insieme
            // toglierebbero un quarto di schermo alle miniature.
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (picking) chosen = emptySet() else onBack() }) {
                    Icon(
                        imageVector = if (picking) Icons.Default.Close
                        else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(
                            if (picking) R.string.pick_leave else R.string.settings_back
                        )
                    )
                }
                /*
                 * ⚠️ Fuori dal ramo del campo (dalla `6.27`, rilievo D6): una selezione toglie il
                 * campo dalla scena e la sua fine lo rimette, e un ricordo tenuto là dentro
                 * ripartiva da capo e ridava il fuoco. La chiave è l'esserci della ricerca: da una
                 * cartella a una ricerca nuova la visita è nuova, e il fuoco si ridà.
                 */
                var focusGiven by rememberSaveable(query != null) { mutableStateOf(false) }
                Column(modifier = Modifier.weight(1f)) {
                    if (query != null && !picking) {
                        /*
                         * Il campo prende il fuoco da sé: chi tocca 'Cerca' ha già in mente la
                         * parola. ⚠️ Una volta sola per visita ([focusGiven]), anche dopo una
                         * selezione o una rotazione: rimettere il fuoco riaprirebbe la tastiera
                         * chiusa per guardare i risultati.
                         */
                        val focus = remember { FocusRequester() }
                        LaunchedEffect(Unit) {
                            if (!focusGiven) {
                                focus.requestFocus()
                                focusGiven = true
                            }
                        }
                        TextField(
                            value = query,
                            onValueChange = onQuery,
                            placeholder = {
                                Text(searchIn ?: stringResource(R.string.search_hint))
                            },
                            singleLine = true,
                            // Senza contorno e senza fondo: è al posto di un titolo, e un
                            // campo squadrato in testata sembrerebbe un modulo da compilare.
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            trailingIcon = if (query.isEmpty()) null else ({
                                IconButton(onClick = { onQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.search_clear)
                                    )
                                }
                            }),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus)
                        )
                    } else {
                        /*
                         * Il titolo si dissolve con l'intestazione: a fascia aperta il nome si
                         * legge là, grande e al centro, e chiudendola lascia il posto a questo.
                         * ⚠️ La traslazione la fa la parallasse della fascia ([FrontBand]): una
                         * seconda sarebbe un doppio movimento sullo stesso oggetto.
                         * Le due opacità sommano uno a ogni istante, quindi il nome non si legge mai
                         * meno che agli estremi. Senza intestazione `aperto` vale zero e l'opacità
                         * è piena. Anche in selezione il titolo resta il nome della cartella.
                         */
                        val titolo: @Composable (Modifier) -> Unit = { m ->
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 1,
                                /*
                                 * ⚠️ L'ellissi in coda, come la copia nella fascia. Per i nomi di
                                 * file è vietata, perché toglierebbe l'estensione, e si usa quella
                                 * in mezzo (`Names.kt`); questo è il nome di una raccolta del
                                 * `MediaStore`, che estensione non ha.
                                 */
                                overflow = TextOverflow.Ellipsis,
                                modifier = m.heading().graphicsLayer { alpha = 1f - aperto() }
                            )
                        }
                        /*
                         * Il conto sotto il titolo, più piccolo e meno opaco (specifica
                         * dell'utente, giro della `1.77`): gli elementi in tutto, o i selezionati
                         * in selezione. Dice 'elementi' e non 'immagini', perché conta anche i
                         * video.
                         * ⚠️⚠️ **TRE CHIAVI DIVERSE, E NESSUNA SI RIUSA PER SOMIGLIANZA**:
                         * `folders_count` conta le sole immagini nella schermata iniziale, accanto a
                         * `folders_clips`; `items_count` dice il totale e `pick_count` *N elementi
                         * selezionati* (dalla `1.80`). Riscriverne una per questo posto la
                         * cambierebbe anche nell'altro.
                         */
                        val numero: @Composable () -> Unit = {
                            conto?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.graphicsLayer { alpha = 1f - aperto() }
                                )
                            }
                        }
                        /*
                         * Sullo schermo largo nome e numero sono su una riga, dalla `3.70`
                         * (richiesta B1): il nome si accorcia per primo, e il numero resta
                         * leggibile.
                         */
                        if (wide) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                titolo(Modifier.weight(1f, fill = false))
                                numero()
                            }
                        } else {
                            titolo(Modifier)
                            numero()
                        }
                    }
                }
                // ⚠️ Il peso è fuori dalla colonna del conto, allineato al bordo destro della
                // barra (richiesta dell'utente): dentro la colonna seguirebbe la larghezza del
                // testo.
                // ⚠️ Spento il peso, in selezione qui non va niente, nemmeno il filtro: in mezzo
                // a una selezione cambierebbe l'elenco sotto le spunte già date.
                when {
                    picking && pickWeight -> PickWeight(chosen)
                    picking -> Unit
                    else -> FilterKey(filter, onFilter, onSearch)
                }

                /*
                 * ⚠️ Qui non c'è un pulsante 'Tutte' (scelta dell'utente): selezionare tutto è il
                 * tocco lungo sul FAB, che si annuncia a TalkBack e ha il suo onboarding. Un
                 * pulsante qui sarebbe l'unica azione che fa apparire qualcosa in questa barra.
                 */
            }
            Spacer(Modifier.height(8.dp))

            /*
             * La fascia dell'intestazione: l'icona della cartella e sotto il nome, come nella
             * schermata iniziale. Misura, ritaglio e parallasse sono in [FrontBand]; qui c'è quello
             * che si vede dentro. L'icona si stringe se la fascia è bassa, con lo stesso conto della
             * schermata iniziale, o sarebbe tagliata sopra e sotto.
             * ⚠️ **Il nome è due volte nell'albero semantico**, qui e in testata, e nessuna delle due
             * copie si può togliere, perché l'una si dissolve nell'altra. L'opacità non lo
             * nasconde a TalkBack: per chiudere il buco serve un `alpha` semantico, non un testo in
             * meno.
             */
            if (front) {
                FrontBand(fullPx = headerPx, shut = { shut }) { quanto ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        /*
                         * L'icona prima si rimpicciolisce, poi si dissolve (specifica dell'utente,
                         * dalla `1.78`), e dalla `1.80` le due fasi cominciano insieme. Le due metà
                         * sono in `Front.kt`: [frontIconMeasure] per la misura, [frontIconInk] per
                         * la dissolvenza, con la soglia risolta da [frontIconFade] dagli stessi
                         * ingressi. La misura vince sulla scala perché il titolo sotto prende lo
                         * spazio che l'icona cede. `toPx()` si chiama qui perché
                         * `GraphicsLayerScope` è una `Density`.
                         * Con la sfumatura accesa l'icona va in negativo: colore della superficie e
                         * inchiostro pieno, con la stessa curva di dissolvenza.
                         */
                        /*
                         * Il tocco sull'icona sceglie la copertina (dalla `1.94`), il tocco lungo il
                         * colore. L'icona dichiara a TalkBack il gesto che fa: muta, sarebbe una
                         * funzione per chi la conosce già.
                         * ⚠️ Chi ascolta viene prima di chi misura (`Rules.md`, § '👆 Che cosa fa il
                         * tocco FUORI da una finestra'): un nodo di tocco non finisce mai dietro un
                         * confine di layout.
                         */
                        iconaCartella(
                            Modifier.frontIconMeasure(fullPx = headerPx, shut = { shut }, max = HEADER_ICON),
                            frontWash
                        ) {
                            frontIconInk(
                                aperto = quanto(),
                                soglia = frontIconFade(headerPx, HEADER_ICON.toPx()),
                                pieno = pienoIcona
                            )
                        }
                        Spacer(Modifier.height(FRONT_GAP))
                        /*
                         * Il titolo è in `titleLarge` (scelta dell'utente, dalla `1.85`), sotto
                         * l'`headlineSmall` della testata: alla stessa misura la traslazione non
                         * avrebbe niente da mostrare. Lo spazio per un nome lungo lo fa
                         * [FRONT_TITLE_LINES], l'andata a capo.
                         * 'Titolo graziato' cambia solo il carattere (dalla `1.85`), ed è quello di
                         * sistema ([FontFamily.Serif]): la richiesta è la forma delle grazie, non
                         * un font in più nell'APK.
                         * Il tocco sul nome lo copia, il tocco lungo rinomina la cartella.
                         */
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.let {
                                if (frontSerif) it.copy(fontFamily = FontFamily.Serif) else it
                            },
                            textAlign = TextAlign.Center,
                            maxLines = FRONT_TITLE_LINES,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .heading()
                                .semantics { onLongClick(label = rinominaEtichetta) { viaLungo(); true } }
                                .pointerInput(title, facts.path) {
                                    detectTapGestures(
                                        onTap = { copiaNome() },
                                        onLongPress = {
                                            haptics.performHapticFeedback(HOLD_BUZZ)
                                            viaLungo()
                                        }
                                    )
                                }
                                .padding(horizontal = 24.dp)
                                .graphicsLayer { alpha = quanto() }
                        )
                        /*
                         * Il conto è anche qui, dentro la fascia col nome: la traslazione verso la
                         * testata (richiesta dell'utente) la fa la parallasse di [FrontBand].
                         * L'opacità è complementare a quella della testata, come per il nome, e la
                         * stringa è la stessa ([conto]).
                         */
                        conto?.let {
                            Spacer(Modifier.height(FRONT_COUNT_GAP))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.graphicsLayer { alpha = quanto() }
                            )
                        }
                        /*
                         * Le pastiglie: peso, immagini e video (tre dati dalla stessa query) e
                         * 'Seleziona tutto', tutte dello stesso colore neutro (scelta dell'utente,
                         * dalla `1.85`). Una pastiglia senza numero non compare, e in selezione la
                         * fila resta.
                         * ⚠️ **I gesti sono coppie, dettate dall'utente nella `1.86`**: il tocco
                         * aggiunge alla selezione quello che la pastiglia nomina, il tocco lungo lo
                         * toglie.
                         * - Peso: tocco tutto, tocco lungo niente.
                         * - Immagini: tocco le immagini, tocco lungo via le immagini.
                         * - Video: tocco i video, tocco lungo via i video.
                         * - Comando: tocco tutto (o niente, se è già tutto), tocco lungo niente.
                         * Aggiunge e non sostituisce, così i dati si compongono: immagini più video
                         * fa tutto.
                         */
                        pastiglie(false, quanto)
                    }
                }
            }
            }

            /*
             * ⚠️ **Il riquadro avvolge tutti e tre i casi, non il solo elenco pieno** (dalla `1.06`):
             * il FAB deve esserci anche a cestino vuoto, perché la Cronologia è nel suo menu.
             * Il `weight` tiene il FAB in basso anche con tre sole immagini.
             */
            /*
             * Sullo schermo largo, a destra della griglia, la pillola con i comandi del FAB (dalla
             * `3.70`, richiesta B2): la colonna comincia sotto il filtro, e la griglia si stringe per
             * lasciarle il posto. È agganciata in basso, staccata dal bordo di [PILL_AIR].
             */
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when {
                items == null -> CircularProgressIndicator(
                    // ⚠️ `TopCenter` e non `CenterHorizontally`: qui il genitore è un `Box`, e
                    // l'allineamento di colonna non esiste più.
                    Modifier.padding(top = 24.dp).size(28.dp).align(Alignment.TopCenter)
                )

                /*
                 * Vuoti diversi, frasi diverse: il cestino vuoto, la ricerca senza esito, il filtro
                 * senza esito (manca quel genere, non è vuota la cartella) e la cartella vuota. A
                 * ricerca ancora da scrivere non si dice niente.
                 * ⚠️ Il filtro si guarda prima della cartella, o con le sole immagini accese in una
                 * cartella di soli video si direbbe il falso; la ricerca vince sul filtro.
                 */
                items.isEmpty() -> {
                    val nulla = when {
                        bin -> stringResource(R.string.bin_none)
                        query != null && query.isNotBlank() ->
                            stringResource(R.string.search_none, query)
                        query != null -> null
                        filter == MediaKind.IMAGES -> stringResource(R.string.folder_no_images)
                        filter == MediaKind.VIDEOS -> stringResource(R.string.folder_no_videos)
                        else -> stringResource(R.string.folder_empty)
                    }
                    // Al centro dello spazio vuoto (richiesta dell'utente): in alto a sinistra
                    // sembrerebbe l'inizio di un elenco che non arriva.
                    if (nulla != null) {
                        Text(
                            text = nulla,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 24.dp)
                                .padding(bottom = BELOW_FAB)
                        )
                    }
                }

                else -> {
                /*
                 * La selezione da/a: il gesto è sulla griglia, perché comincia su una miniatura e
                 * finisce su un'altra.
                 * ⚠️⚠️ **LA CHIAVE DEL `pointerInput` NON COMPRENDE LA SELEZIONE**: cambiare una
                 * chiave annulla il gesto in corso, e il trascinamento si interromperebbe alla
                 * prima immagine aggiunta (il difetto della `0.32`). La selezione si legge dentro
                 * il gesto.
                 * Il gesto dice solo dove si trova il dito; la selezione la estende l'effetto qui
                 * sotto, così il conto è lo stesso se si muove il dito o la griglia.
                 */
                val grab = Modifier.pointerInput(items) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { at ->
                            val hit = state.itemIndexAt(at)
                            if (hit != null) {
                                dragFrom = hit
                                dragBase = chosen
                                // ⚠️ Il verso si legge prima di toccare la selezione, o la
                                // riga dopo lo avrebbe già falsato.
                                dragOff = items[hit] in chosen
                                // La vibrazione è una sola, quella piena: vedi [HOLD_BUZZ].
                                haptics.performHapticFeedback(HOLD_BUZZ)
                                chosen =
                                    if (dragOff) chosen - items[hit] else chosen + items[hit]
                                dragAt = at
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (dragFrom != null) dragAt = change.position
                        },
                        onDragEnd = { dragFrom = null; dragAt = null },
                        onDragCancel = { dragFrom = null; dragAt = null }
                    )
                }

                /*
                 * Lo scorrimento ai bordi: col dito appoggiato in fondo la griglia scorre e la
                 * selezione lo segue, o una selezione da/a si fermerebbe al bordo dello schermo.
                 * ⚠️⚠️ **SI AGGIORNA A OGNI FOTOGRAMMA, NON A OGNI EVENTO DEL DITO**: mentre la
                 * griglia scorre sotto un dito fermo non arriva nessun evento di puntatore, quindi
                 * dentro `onDrag` la selezione resterebbe ferma.
                 * La spinta cresce avvicinandosi al bordo: a velocità unica o non basta, o si arriva
                 * in fondo alla cartella prima di accorgersene.
                 */
                LaunchedEffect(dragAt != null, items) {
                    /*
                     * ⚠️ La casella dell'ultimo giro (dalla `6.28`, rilievo D7): senza, la selezione
                     * si ricostruiva a ogni fotogramma anche col dito fermo. Partenza, selezione di
                     * partenza e verso restano gli stessi per tutto il trascinamento.
                     */
                    var last = -1
                    while (dragAt != null) {
                        withFrameNanos { }
                        val at = dragAt ?: break
                        val from = dragFrom ?: break
                        val height = state.layoutInfo.viewportSize.height.toFloat()
                        val push = when {
                            height <= 0f -> 0f
                            at.y < edgePx -> -(edgePx - at.y) / edgePx
                            at.y > height - edgePx -> (at.y - (height - edgePx)) / edgePx
                            else -> 0f
                        }
                        if (push != 0f) state.scrollBy(push.coerceIn(-1f, 1f) * speedPx)
                        val hit = state.itemIndexAt(at) ?: continue
                        if (hit == last) continue
                        last = hit
                        // ⚠️ L'intervallo si somma o si sottrae alla selezione di partenza
                        // secondo [dragOff]: il conto è sempre 'quella di prima più (o meno)
                        // l'intervallo di adesso', quindi tornare indietro col dito disfa.
                        val span = items.subList(minOf(from, hit), maxOf(from, hit) + 1)
                        chosen = if (dragOff) dragBase - span.toSet() else dragBase + span
                    }
                }

                LazyVerticalGrid(
                        columns = GridCells.Fixed(spread(columns, LocalWindowInfo.current)),
                        state = state,
                        horizontalArrangement = Arrangement.spacedBy(gridGap()),
                        verticalArrangement = Arrangement.spacedBy(gridGap()),
                        /*
                         * ⚠️ **Lo spazio in fondo è quello di ciò che copre la griglia**, o
                         * l'ultima riga di miniature resterebbe sotto senza modo di tirarla
                         * fuori: in selezione la scheda delle azioni, misurata ([sheetTall]);
                         * fuori, la pillola o il menu inferiore, il FAB del cestino
                         * ([BELOW_FAB]) o un margine. Sempre col margine della schermata e il
                         * rientro di sotto, che il contenitore non si mette (dalla `1.90`).
                         */
                        contentPadding = PaddingValues(
                            bottom = GRID_PAD_Y + bottomInset() + if (picking) {
                                with(LocalDensity.current) { sheetTall.toDp() }
                            } else if ((tall || shape == Adaptive.Shape.PHONE) && pillShown) {
                                // La pillola in basso del tablet in verticale o del telefono, o il
                                // menu inferiore: l'ultima riga deve poter salire sopra.
                                if (tall) PILL_KEY + PILL_AIR * 2 else controlRoom()
                            } else if (bin) BELOW_FAB else 16.dp
                        ),
                        modifier = Modifier.fillMaxWidth().backdropSource(backdrop).then(grab)
                    ) {
                        itemsIndexed(
                            items = items,
                            // ⚠️ La chiave è l'indirizzo e non la posizione: ruotando il
                            // telefono le miniature non si rimescolano fra i riquadri.
                            key = { _, uri -> uri.toString() },
                            // Un tipo solo per tutti i riquadri: Compose riusa la composizione
                            // di quelli che escono per quelli che entrano.
                            contentType = { _, _ -> THUMB_KIND }
                        ) { index, uri ->
                            Thumbnail(
                                uri = uri,
                                position = index + 1,
                                total = items.size,
                                // ⚠️ AIV Play non segna l'ultimo elemento visto (`Store.marksLast`),
                                // ma il salto a quell'elemento, che usa lo stesso indice, resta.
                                marked = Store.marksLast && index == highlight,
                                chosen = uri in chosen,
                                named = gridNames,
                                mark = lastMark,
                                room = cellPx,
                                shade = ombra,
                                // In selezione il tocco sceglie invece di aprire, come in ogni
                                // galleria.
                                /*
                                 * ⚠️⚠️ **IL PRIMO RAMO SERVE AL TOCCO LUNGO SU UNA SOLA
                                 * IMMAGINE.** `Modifier.clickable` senza `onLongClick` scatta al
                                 * rilascio qualunque sia la durata, e la piastrella vede gli
                                 * eventi prima della griglia: il tocco lungo selezionava
                                 * l'immagine, e al rilascio questo richiamo, con `picking` già
                                 * vero, la toglieva. `dragFrom` è impostato solo fra
                                 * `onDragStart` e la fine del gesto, quindi un tocco che lo trova
                                 * è la coda di un tocco lungo. Col trascinamento il difetto non
                                 * si vede, perché `onDrag` consuma gli eventi.
                                 * ⚠️ Un `onLongClick` sulla piastrella non va: `combinedClickable`
                                 * consumerebbe tutto fino al rilascio e annullerebbe la selezione
                                 * da/a.
                                 */
                                onClick = {
                                    when {
                                        dragFrom != null -> Unit
                                        picking -> {
                                            haptics.performHapticFeedback(HOLD_BUZZ)
                                            chosen = chosen.toggle(uri)
                                        }
                                        else -> onOpen(index)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            }
            if (wide && pillEntries.isNotEmpty()) {
                ActionPill(
                    entries = pillEntries,
                    vertical = true,
                    backdrop = backdrop,
                    modifier = Modifier
                        .align(Alignment.Bottom)
                        .windowInsetsPadding(steadyDrawing().only(WindowInsetsSides.Bottom))
                        .padding(start = PILL_AIR, bottom = PILL_AIR)
                )
            }
            }
        }

            /*
             * La sfumatura in fondo, dalla `4.33` una sola fascia ampia (specifica dell'utente,
             * voce `4.30-07`): in cima al pieno di [FOLDER_PEAK], a intestazione chiusa al 60% di
             * quel pieno ([SCROLLED_FADE]), insieme al titolo e con lo stesso `aperto`; negli ultimi [TAIL_REACH] di
             * griglia scende a zero seguendo il dito, così a fine corsa l'ultima riga si vede
             * intera. La curva è in [GroundFade], condivisa con la schermata iniziale.
             * ⚠️ È scritta prima di FAB, scheda e veli: in un `Box` l'ultimo figlio è sopra, e
             * nessuno di loro va sbiadito.
             */
            if (front) {
                val coda = with(LocalDensity.current) { TAIL_REACH.toPx() }
                val resto by remember(coda) { derivedStateOf { tailLeft(state, coda) } }
                GroundFade(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    alpha = { (SCROLLED_FADE + (1f - SCROLLED_FADE) * aperto()) * resto },
                    footAlpha = null,
                    peak = FOLDER_PEAK
                )
            }

            /*
             * ⚠️ **Il FAB è scritto dopo la sfumatura** (dalla `1.83`): in un `Box` l'ultimo figlio
             * è sopra, e dentro la colonna la sfumatura lo velava scorrendo. Come nella schermata
             * iniziale.
             * Il FAB c'è nel cestino e in ogni cartella (dalla `1.82`), mai in selezione, dove le
             * azioni sono nella scheda in basso. Nel cestino il menu ha le voci del cestino intero,
             * in una cartella Cerca, Cestino e Impostazioni ([PickMenu]); senza nessun richiamo non
             * compare, perché aprirebbe un menu vuoto. Sul telefono c'è solo se la pillola è spenta.
             * A fine selezione rientra subito, e a scansarsi è la notifica ([Modifier.aboveFoot]).
             */
            FabPop(
                visible = (bin || onSettings != null || onBin != null || onSearchHere != null) &&
                    !picking && shape == Adaptive.Shape.PHONE && pillLook.mode == PhonePill.OFF,
                // Il lato è quello scelto nelle impostazioni: vedi `PadLook.hand`.
                // ⚠️ Il centro è quello del tondo, come nella schermata iniziale ([pillCorner]),
                // e lo stesso angolo lo riceve [HintVeil].
                modifier = Modifier
                    .align(fabSide())
                    .pillCorner(FAB_SIZE)
            ) {
                Box {
                    /*
                     * ⚠️⚠️ **IL MENU È SCRITTO PRIMA DEL FAB, E L'ORDINE È LA FUNZIONE**: il FAB
                     * si stacca in una finestra sua per restare sopra il velo (`lifted` in
                     * [TapHoldFab]), e fra finestre dello stesso tipo comanda l'ordine di
                     * composizione. Scritto dopo, il menu coprirebbe il FAB. Il posizionatore
                     * legge il bordo di questo riquadro, che non dipende dall'ordine dei figli.
                     */
                    PickMenu(menu = menu, columns = columns) {
                        /*
                         * In una cartella il menu è un altro (dalla `1.82`): le voci del cestino
                         * intero qui non vogliono dire niente. Stesso ordine e stesso [MenuRow]
                         * della schermata iniziale, così chi ha imparato dov'è una voce la
                         * ritrova.
                         */
                        if (!bin) {
                            /*
                             * 'Cerca' è in cima, come nella schermata iniziale (dalla `1.83`): è la
                             * sola voce che resta dentro questa cartella.
                             */
                            onSearchHere?.let { cerca ->
                                MenuRow(
                                    text = stringResource(R.string.hub_search),
                                    icon = Icons.Default.Search,
                                    onTap = { menu.close(); cerca() }
                                )
                            }
                            /*
                             * ⚠️ **La voce che toglie la copertina oggi non si vede**:
                             * [COVER_MENU_ROW] è spenta (scelta dell'utente, dalla `1.95`), e il
                             * perché è su quella costante. Accesa, compare solo se una copertina
                             * scelta esiste, fra 'Cerca' (quello che si fa qui) e 'Cestino' (quello
                             * che porta altrove). A sceglierla è il tocco sull'icona
                             * dell'intestazione.
                             */
                            val togliCopertina = onCoverClear.takeIf { coverSet && COVER_MENU_ROW }
                            togliCopertina?.let { togli ->
                                if (onSearchHere != null) HorizontalDivider()
                                MenuRow(
                                    text = stringResource(R.string.folder_cover_auto),
                                    icon = Icons.Outlined.HideImage,
                                    onTap = { menu.close(); togli() }
                                )
                            }
                            onBin?.let { vaiAlCestino ->
                                if (onSearchHere != null || togliCopertina != null) HorizontalDivider()
                                MenuRow(
                                    text = stringResource(R.string.bin_title),
                                    icon = Glyphs.Bin,
                                    onTap = { menu.close(); vaiAlCestino() }
                                )
                            }
                            onSettings?.let { vaiAlleImpostazioni ->
                                if (onBin != null) HorizontalDivider()
                                MenuRow(
                                    text = stringResource(R.string.hub_settings),
                                    icon = Icons.Default.Settings,
                                    onTap = { menu.close(); vaiAlleImpostazioni() }
                                )
                            }
                            return@PickMenu
                        }
                        /*
                         * Il menu del cestino: Cronologia in cima (richiesta dell'utente, dalla
                         * `1.53`), poi Ripristina tutto, ultima la voce che cancella per sempre,
                         * così un tocco al buio sulla prima voce non svuota il cestino.
                         * ⚠️ Le due azioni si spengono a cestino vuoto, la Cronologia no: ha
                         * senso proprio quando si è ripristinato tutto, ed è la sola sempre
                         * toccabile.
                         * Sono `MenuRow` come negli altri menu, col margine sopra e sotto messo
                         * dalla superficie.
                         */
                        if (Store.files) MenuRow(
                            text = stringResource(R.string.bin_history),
                            // Un disegno dell'utente (dalla `1.55`): il cassone e la freccia
                            // che torna indietro.
                            icon = Glyphs.BinHistory,
                            onTap = { menu.close(); onHistory() }
                        )
                        MenuRow(
                            text = stringResource(R.string.bin_restore_all),
                            // Lo stesso glifo del ripristino singolo: vedi [Glyphs.BinRestore].
                            icon = Glyphs.BinRestore,
                            enabled = filled,
                            /*
                             * ⚠️ Chiede conferma (richiesta dell'utente, dalla `1.53`): i file
                             * tornano ognuno nella sua cartella d'origine, che possono essere
                             * molte, e disfare a mano vorrebbe dire ritrovarli uno per uno. Il
                             * testo della conferma nomina la Cronologia, dove le destinazioni
                             * sono scritte.
                             */
                            onTap = { menu.close(); askRestoreAll() }
                        )
                        MenuRow(
                            text = stringResource(R.string.bin_empty),
                            icon = Icons.Default.DeleteForever,
                            enabled = filled,
                            danger = true,
                            onTap = { menu.close(); askEmpty() }
                        )
                    }
                    val altroTema = aivLauncher(!LocalAivLight.current)
                    PickFab(
                        // I colori dell'icona dell'app nell'altro tema, come il FAB della
                        // schermata iniziale: li dà `aivLauncher`, col tema dell'app e non con
                        // quello di sistema, e il perché è là.
                        container = altroTema.first,
                        ink = altroTema.second,
                        holdLabel = shortcutLabel,
                        backdrop = backdrop,
                        // In una cartella il glifo dell'app, nel cestino i tre puntini: vedi
                        // [PickFab].
                        mark = !bin,
                        /*
                         * A tasto armato il tocco fa il salto e non apre il menu (dalla `2.07`):
                         * il tratto dura finché il chevron si vede, e finisce da sé un secondo
                         * dopo l'ultimo pixel scorso.
                         */
                        arm = arm,
                        // ⚠️ `visible` e non `wanted`: il FAB resta staccato per tutta l'uscita,
                        // o rientrerebbe nella finestra dell'app sotto il velo che se ne va.
                        lifted = menu.visible,
                        // ⚠️ `wanted` e non `visible`: il perché è sul parametro `pressed` di
                        // [TapHoldFab].
                        pressed = menu.wanted,
                        // ⚠️ Apre e basta: a menu aperto il tocco lo consuma `MenuGuard` (in
                        // `Menus.kt`, messo in scena da `AivTheme`), e un'alternanza qui
                        // riaprirebbe il menu appena chiuso. La guardia lo raggiunge anche se il
                        // FAB è in una finestra più alta, perché quella finestra è trasparente al
                        // tocco (`untouchable` in `ActionPad`).
                        onTap = { if (arm.armed) scope.launch { arm.leap(paging) } else menu.open() },
                        onHold = { shortcut(); hintDone() }
                    )
                }
            }

            /*
             * Sul telefono, dalla `4.00`, la pillola può prendere il posto del FAB (richiesta
             * dell'utente): nello stesso punto, con la stessa comparsa e le voci del menu del FAB.
             * Il tasto tondo tiene il salto e la scorciatoia del tocco lungo (risposta C1).
             */
            FabPop(
                visible = pillShown && shape == Adaptive.Shape.PHONE,
                // ⚠️ Il menu inferiore si posa in fondo a tutta larghezza, e tiene l'angolo per
                // la sola pillola in cui si ripiega (`corner`).
                modifier = Modifier.align(if (pillLook.bar) Alignment.BottomCenter else fabSide())
            ) {
                PhonePillBar(
                    // Lo stesso angolo della schermata iniziale: vedi [pillCorner].
                    corner = Modifier.pillCorner(),
                    mark = { d ->
                        if (!bin) Marchio(d)
                        else Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = d)
                    },
                    restLabel = stringResource(R.string.pick_actions),
                    entries = pillEntries,
                    arm = arm,
                    nested = paging,
                    fabLabel = jumpLabel(arm, stringResource(R.string.pick_actions)),
                    holdLabel = stringResource(shortcutLabel),
                    onHold = { shortcut(); hintDone() },
                    backdrop = backdrop,
                    fabGlyph = { d ->
                        JumpGlyph(arm) {
                            if (!bin) Marchio(d)
                            else Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = d)
                        }
                    }
                )
            }

            /*
             * Sul tablet in verticale la pillola è in basso a destra, compatta e staccata dal bordo
             * (dalla `3.71`, voce `3.70-07`), con le stesse voci dello schermo largo.
             */
            if (tall && pillShown) {
                ActionPill(
                    entries = pillEntries,
                    vertical = false,
                    backdrop = backdrop,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(steadyDrawing())
                        .padding(PILL_AIR)
                )
            }

            /*
             * ⚠️ **La scheda della selezione è nel `Box` di radice** (dalla `1.40`): dentro la
             * colonna il rientro di sistema è già consumato, e sotto la scheda restava una striscia
             * di un altro colore. Qui arriva al bordo e il rientro se lo mette sul contenuto
             * ([PickSheet]). È scritta prima dei veli, che devono restarle sopra.
             */
            PickSheet(
                visible = picking,
                // Senza specchiatura per la mano sinistra (scelta dell'utente, dalla `1.57`):
                // l'ordine dei tasti si trascina, e il lato del FAB sposta il resto.
                actions = pickActions,
                onHeight = { sheetTall = it }
            )

            /*
             * La notifica dell'azzeramento della selezione passa dal canale delle notifiche, la cui
             * superficie è in `AivApp`: qui non si disegna. Non entra in [sheetTall], perché dura
             * tre secondi e non copre niente da raggiungere.
             * Il velo che chiude i menu (`MenuGuard` in `Menus.kt`, in `AivTheme`) resta sopra la
             * notifica, o un tocco fuori dal menu del FAB finirebbe su 'Annulla': da Android 12 la
             * finestra di un popup non è modale al tocco, e `dismissOnClickOutside` da solo non basta.
             */

            /*
             * Il mini onboarding del tocco lungo: colore, contrasto e geometria sono in [HintVeil],
             * qui restano la frase e il FAB. È l'unico modo di insegnare la scorciatoia, ed è uno
             * solo in questa schermata, quello del cestino, alla prima apertura.
             */
            if (hint != null) {
                HintVeil(
                    text = stringResource(
                        when (hint) {
                            Hint.BIN_EMPTY -> R.string.bin_empty_hint
                            /*
                             * ⚠️ Da qui in giù i rami non si vedono mai (`hint` li esclude): ci
                             * sono perché il `when` su [Hint] deve essere completo. Ognuno di quei
                             * veli vive altrove e con un'altra forma: le colonne nella schermata
                             * delle cartelle, il doppio tocco nel visualizzatore (`HintCentre`),
                             * le estensioni nella rinomina (`HintNotice`), la copertina qui sotto
                             * (`HintSpot`), i moduli e i tasti dell'editor completo (`HintStrip`,
                             * `HintSpots`), il Disegno (`HintSpots`) e il Feed nella schermata
                             * iniziale (`HintFeed`).
                             */
                            Hint.COLUMNS -> R.string.columns_hint
                            Hint.ZOOM_TAP -> R.string.hint_zoom_tap
                            Hint.EXT_WARN -> R.string.hint_ext_warn
                            Hint.COVER -> R.string.hint_cover
                            Hint.MODULES -> R.string.hint_modules
                            Hint.EDITOR_TOOLS -> R.string.hint_tools
                            Hint.DRAW -> R.string.hint_draw
                            Hint.FEED -> R.string.feed_order_desc
                        }
                    ),
                    // ⚠️ L'angolo del comando vero, tondo o FAB ([pillCorner]): il perché è sul
                    // parametro di [HintVeil].
                    inset = Modifier.pillCorner(if (pillLook.mode != PhonePill.OFF) PILL_KEY else FAB_SIZE),
                    onDone = hintDone
                ) {
                    PickFab(
                        container = HINT_MARK,
                        ink = HINT_INK,
                        holdLabel = shortcutLabel,
                        // ⚠️ Lo stesso valore del FAB vero, o il velo indicherebbe un disegno
                        // diverso. Oggi il velo compare solo nel cestino, ma scritta uguale la
                        // condizione resta giusta anche in una cartella.
                        mark = !bin,
                        // Qui il salto non c'è: il velo insegna il tocco lungo, e un chevron
                        // indicherebbe un altro comando.
                        arm = null,
                        round = pillLook.mode == PhonePill.SLIDE,
                        // ⚠️ Col tasto tondo il tocco breve apre la pillola e non il menu: il
                        // velo lo chiude e basta, e la pillola si apre col tasto vero.
                        onTap = { hintDone(); if (pillLook.mode == PhonePill.OFF) menu.open() },
                        onHold = { shortcut(); hintDone() }
                    )
                }
            }

            /*
             * Il mini onboarding della copertina (richiesta dell'utente, dalla `1.95`): la prima
             * volta che si tocca l'icona dell'intestazione per scegliere la copertina, l'icona
             * evidenziata e il testo di `hint_cover`.
             * ⚠️ Non passa da [hint], il velo del FAB, che indica un tasto in fondo: questo indica
             * un'icona in cima. La chiave è nello stesso enum, perché 'Ripristina gli avvisi' li
             * rimette tutti. Aspetta il riquadro dell'icona ([iconaSpot]), che in pratica c'è
             * sempre, perché il velo nasce da un tocco su di lei.
             */
            val dovIcona = iconaSpot
            if (coverHere && !coverSeen && dovIcona != null) {
                HintSpot(
                    text = stringResource(R.string.hint_cover),
                    spot = dovIcona,
                    glyph = Glyphs.FolderAiv,
                    // ⚠️ Chiuso il velo, la scelta continua dall'elenco iniziale: vedi `onCoverAway`.
                    onDone = {
                        scope.launch { Hint.COVER.remember(context) }
                        onCoverAway()
                    }
                )
            }
    
    }
}

    /*
     * Azzerare la selezione non chiede conferma (scelta dell'utente): c'è la notifica con
     * 'Annulla', e il perché è su [cleared].
     */

    /*
     * La rinomina della cartella usa la finestra del file singolo (istruzione dell'utente), senza
     * percorso né estensione: le differenze le dice il parametro `folder` di [RenameDialog]. Il
     * campo parte dal titolo della schermata.
     */
    val rinominaCartella = onFolderRename
    if (rinomina && rinominaCartella != null) {
        RenameDialog(
            uris = emptyList(),
            folder = title,
            onDismiss = { rinomina = false },
            onRename = { nome, _, _ ->
                rinomina = false
                rinominaCartella(nome)
            }
        )
    }

    /*
     * ⚠️ Il selettore del colore della cartella, aperto dal tocco lungo sull'icona
     * dell'intestazione. È qui con gli altri dialoghi e non dentro la fascia, che si misura e si
     * ritaglia a ogni fotogramma: là la finestra nascerebbe e sparirebbe con lei.
     */
    if (tinge) {
        TintDialog(
            current = frontTint,
            onPick = onFrontTint,
            onDismiss = { tinge = false }
        )
    }

    /*
     * ⚠️ La conferma di 'Ripristina tutto' non ha il tasto del colore dell'errore, perché qui
     * non si perde niente: serve a dire dove finiscono i file (il perché è sulla voce del menu).
     */
    if (restoringAll) {
        AlertDialog(
            onDismissRequest = { restoringAll = false },
            modifier = Modifier.lowered { restoringAll = false },
            properties = loweredWindow { restoringAll = false },
            title = { Text(stringResource(R.string.bin_restore_all_ask)) },
            text = { Text(stringResource(R.string.bin_restore_all_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        restoringAll = false
                        job = FileJob.Restore(items.orEmpty())
                    }
                ) { Text(stringResource(R.string.bin_restore_all)) }
            },
            dismissButton = {
                TextButton(onClick = { restoringAll = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    /*
     * ⚠️ **Questo dialogo è qui e non in `FileOps.kt`**: non riceve un elenco di file scelti ma
     * svuota una cartella intera, quindi non entra in `FileJob`. Si chiede solo dal FAB del
     * cestino. L'esito usa l'avviso dell'eliminazione, perché i file vanno via davvero.
     */
    if (emptying) {
        AlertDialog(
            onDismissRequest = { emptying = false },
            modifier = Modifier.lowered { emptying = false },
            properties = loweredWindow { emptying = false },
            title = { Text(stringResource(R.string.bin_empty_ask)) },
            text = { Text(stringResource(R.string.bin_empty_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        emptying = false
                        perform(FileKind.DELETE) { Bin.empty(context) }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(stringResource(R.string.bin_empty)) }
            },
            dismissButton = {
                TextButton(onClick = { emptying = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // ⚠️ I dialoghi delle operazioni sono in `FileOps.kt` perché li chiede anche il
    // visualizzatore: qui resta che a operazione finita la cartella si rilegge.
    FileJobDialogs(
        job = job,
        fields = factFields,
        onClose = { job = null },
        onRun = perform
    )
}

/** Toglie o mette, che è quello che fa un tocco su una cosa selezionabile. */
private fun Set<Uri>.toggle(uri: Uri): Set<Uri> = if (uri in this) this - uri else this + uri

/**
 * Quale riquadro si trova sotto un punto, o `null` se là non c'è niente.
 *
 * ⚠️ Si guardano i soli riquadri in vista: di un'immagine fuori schermo la griglia pigra non
 * conosce la posizione. Un punto nei distacchi fra le piastrelle torna `null` e non il vicino
 * più prossimo: durante un trascinamento l'intervallo resta fermo per un istante, invece di
 * saltare.
 */
private fun LazyGridState.itemIndexAt(at: Offset): Int? =
    layoutInfo.visibleItemsInfo.firstOrNull {
        at.x >= it.offset.x && at.x < it.offset.x + it.size.width &&
            at.y >= it.offset.y && at.y < it.offset.y + it.size.height
    }?.index

/**
 * Il FAB della selezione: quello vero, e la sua copia illuminata sopra il velo del
 * suggerimento.
 *
 * ⚠️ **Esiste perché il velo deve illuminare lo stesso disegno del FAB vero**: scritti due
 * volte, cambiando il glifo di uno il velo avrebbe indicato il tasto sbagliato. Qui vivono i
 * valori condivisi (etichetta, etichetta e gesto del tocco lungo, glifo); i chiamanti passano
 * quello che deve differire, cioè i colori e il tocco breve.
 * Il glifo è il marchio dell'app in una cartella (dalla `1.83`), come nella schermata iniziale,
 * e i tre puntini di Material nel cestino (dalla `1.55`): lo sceglie un parametro, così la
 * copia non può divergere.
 *
 * @param holdLabel la stringa del gesto lungo, che dipende dalla scorciatoia in vigore.
 * @param mark se il glifo è il marchio dell'app invece dei tre puntini, cioè se questa griglia
 *   non è il cestino.
 */
@Composable
private fun PickFab(
    container: Color,
    ink: Color,
    @StringRes holdLabel: Int,
    mark: Boolean,
    /**
     * Lo stato del glifo del salto, o `null` dove quel comando non c'è.
     *
     * ⚠️ Non ha un valore di serie, come il parametro di `Modifier.lowered`: chi disegna un FAB
     * dichiara se ha anche il salto. L'unico `null` di oggi è la copia illuminata
     * dell'onboarding.
     */
    arm: JumpArm?,
    onTap: () -> Unit,
    onHold: () -> Unit,
    lifted: Boolean = false,
    pressed: Boolean = false,
    /** La copia del tasto tondo della pillola, per il velo d'aiuto. Vedi `round` in [TapHoldFab]. */
    round: Boolean = false,
    /** Quello che il vetro sfoca. Vedi `backdrop` in [TapHoldFab]. */
    backdrop: Backdrop? = null
) {
    val home = @Composable { d: String? ->
        if (mark) Marchio(d)
        else Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = d)
    }
    TapHoldFab(
        label = arm?.let { jumpLabel(it, stringResource(R.string.pick_actions)) }
            ?: stringResource(R.string.pick_actions),
        container = container,
        ink = ink,
        holdLabel = stringResource(holdLabel),
        lifted = lifted,
        pressed = pressed,
        round = round,
        backdrop = backdrop,
        onTap = onTap,
        onHold = onHold,
        glyph = { d -> if (arm == null) home(d) else JumpGlyph(arm) { home(d) } }
    )
}

/**
 * Un riquadro della griglia.
 *
 * ⚠️⚠️ **I SEGNI SONO RIQUADRI FRATELLI SOPRA L'IMMAGINE, NON BORDI NELLA CATENA DEI
 * MODIFICATORI**: un `Modifier.border` dipende dal suo posto nella catena e da come il nodo
 * dell'immagine disegna (nella `0.34` l'anello non si vedeva), mentre due fratelli in un `Box`
 * si dipingono nell'ordine in cui sono scritti. I riquadri di sopra non intercettano il tocco,
 * perché non hanno modificatori di puntatore: il tocco arriva alla colonna.
 */
@Composable
internal fun Thumbnail(
    uri: Uri,
    position: Int,
    total: Int,
    marked: Boolean,
    chosen: Boolean,
    /** Se sotto la miniatura va il nome del file. Vedi `Settings.gridNames`. */
    named: Boolean,
    /** Con che segno si disegna [marked]: la cornice o il nastro nell'angolo. */
    mark: LastMark,
    /** Quanto è larga la cella, in pixel: serve solo al nome. Vedi `cellPx`. */
    room: Int,
    /** Dove si trova il bordo della scheda della selezione nella finestra, o `null`: vedi `PickShade.kt`. */
    shade: Float?,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(CORNER)
    // La richiesta si costruisce una volta per indirizzo: ricreata a ogni ricomposizione,
    // Coil avrebbe un oggetto nuovo da confrontare a ogni fotogramma di scorrimento.
    val context = LocalContext.current
    val model = remember(uri, context) { Thumbs.request(context, uri) }

    /*
     * La colonna c'è anche senza il nome: un ramo che la mette solo quando serve scriverebbe due
     * volte il riquadro. Il quadrato è in cima, così le miniature di una riga restano allineate
     * anche con nomi di altezza diversa.
     */
    Column(
        verticalArrangement = Arrangement.spacedBy(NAME_GAP),
        /*
         * ⚠️ **Il tocco è sulla colonna** (dalla `1.81`), così toccare il nome apre come toccare
         * l'immagine, e TalkBack legge un nodo solo: `clickable` unisce posizione e nome. Il
         * tocco lungo resta sulla griglia, perché continua col trascinamento su più piastrelle.
         */
        modifier = Modifier
            .clickable(onClick = onClick, role = Role.Button)
            /*
             * La scelta si dichiara a TalkBack, perché in selezione la piastrella fa da
             * interruttore. `selected` e non una `stateDescription`: lo stato lo dice il
             * lettore di schermo nella lingua del telefono, senza una stringa da tradurre.
             */
            .semantics { selected = chosen }
    ) {
    Box(modifier = Modifier.aspectRatio(1f)) {
        AsyncImage(
            // ⚠️ La richiesta viene da `Thumbs`: la misura è parte della chiave di cache, e
            // deve essere la stessa dovunque (vedi `Thumbs.PX`).
            model = model,
            // ⚠️ La chiave si registra anche qui, per la prima immagine che si apre dalla
            // griglia: senza, entrando nel visualizzatore resterebbe un fotogramma vuoto.
            // Il perché è accanto a `Thumbs.note`.
            onState = { st ->
                if (st is AsyncImagePainter.State.Success) Thumbs.note(uri, st.result.memoryCacheKey)
            },
            // Ogni riquadro è toccabile: TalkBack dice la posizione nella cartella, e se è
            // quello da cui si è tornati.
            contentDescription = stringResource(
                if (marked) R.string.grid_item_last else R.string.grid_item,
                position,
                total
            ),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                // Il fondo si vede finché la miniatura non è pronta: senza, la griglia
                // lampeggerebbe del colore della pagina.
                .background(MaterialTheme.colorScheme.surfaceVariant)
                // ⚠️ Nessuno dei due tocchi è qui: il lungo è sulla griglia, il breve sulla
                // colonna. Un gesto nuovo va dentro uno dei due, non accanto.
        )
        /*
         * ⚠️ **Il velo della scelta va prima del segno dell'ultimo elemento visto**: in un `Box`
         * si dipinge nell'ordine di scrittura, e il segno scritto dopo resta pieno anche su
         * un'immagine scelta. Una tinta sull'intero riquadro si vede anche da lontano.
         * Schiarisce e non scurisce (richiesta dell'utente): scegliere un'immagine è tirarla
         * avanti, non metterla da parte.
         */
        if (chosen) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(Color.White.copy(alpha = PICKED_VEIL))
            )
        }
        if (marked && mark == LastMark.FRAME) {
            /*
             * La cornice, dalla `2.11` (scelta dell'utente), accanto al nastro e al tondo
             * ([LastMark]). Lo spessore è su [lastFrame]; il colore arriva da qui, perché è del
             * tema e un modificatore che non è un composable non lo legge.
             */
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .lastFrame(shape, MaterialTheme.colorScheme.primary)
            )
        }
        if (marked && mark == LastMark.CORNER) {
            /*
             * Il nastro nell'angolo in basso dalla parte dell'inizio della riga (scelta
             * dell'utente, dalla `0.58`): una cornice attorno a una miniatura si legge come
             * selezione, un triangolo no.
             * ⚠️ **L'angolo è diagonalmente opposto alla spunta**, così sulla piastrella vista e
             * scelta i due segni non si toccano: spostarlo 'per simmetria' li rimetterebbe
             * insieme. Il disegno, col ritaglio all'angolo tondo e lo specchio da destra a
             * sinistra, è su [lastCorner].
             */
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .lastCorner(shape, MaterialTheme.colorScheme.primary)
            )
        }
        if (marked && mark == LastMark.DOT) {
            /*
             * Il tondo, dalla `2.92` (richiesta dell'utente), nell'angolo del nastro e per la
             * stessa ragione. Misura, posto e colore sono su [lastDot].
             */
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .lastDot(MaterialTheme.colorScheme.primary)
            )
        }
        /*
         * ⚠️ **L'ombra della selezione va qui, sotto la spunta e la durata** (nota E
         * dell'utente sul giro della `4.45`): la spunta resta sopra e non si nasconde, e la
         * durata è un segno dello stesso tipo. Ritagliata sulla sagoma dell'immagine, o gli
         * angoli tondi sarebbero ombreggiati due volte, qui e sulla griglia.
         */
        Box(modifier = Modifier.matchParentSize().clip(shape).pickShade(shade, behind = false))
        if (Videos.isVideo(uri)) ClipBadge(uri, Modifier.align(Alignment.BottomEnd))
        /*
         * ⚠️ La spunta è su un disco pieno dell'accento, col glifo in `onPrimary`: il contrasto
         * se lo porta da sé, su qualunque immagine. Un `Check` nudo e non un `CheckCircle`, che
         * sarebbe un cerchio dentro un disco. In un angolo, per non coprire l'immagine che si
         * sta guardando per decidere.
         */
        if (chosen) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(TICK)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(TICK * 0.72f)
                )
            }
        }
    }
        if (named) GridName(uri, room)
    }
}

/**
 * La durata di un filmato, nell'angolo della sua miniatura.
 *
 * ⚠️ **È il solo segno che dice 'questo è un video'**, quindi compare anche quando la durata
 * non si sa, col solo triangolo: un fotogramma senza segni sembrerebbe un'immagine.
 * L'angolo è quello in basso dalla parte della fine della riga, l'unico libero: la spunta è in
 * alto dalla stessa parte, il segno dell'ultimo elemento visto in basso dall'altra.
 * Bianco su nero e non i colori del tema: la targhetta è sopra un'immagine qualunque, e il
 * contrasto se lo porta da sé.
 * ⚠️ La durata si chiede una volta per indirizzo, e la prima risposta è quella già in memoria:
 * così una miniatura che rientra in vista non la richiede al MediaStore. Come [GridName].
 */
@Composable
private fun ClipBadge(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val length by produceState(Videos.cachedLength(uri), uri, context) {
        if (value == null) value = Videos.length(context, uri)
    }
    ClipLabel(length, modifier)
}

/**
 * La targhetta vera e propria, per una durata già nota: `null` lascia il solo triangolo.
 *
 * ⚠️ **La durata resta su una riga, e se non entra rimpicciolisce** fino a [BADGE_MIN_TEXT];
 * solo sotto quella misura si taglia (nota A dell'utente sul giro della `5.70`: in orizzontale
 * `12:00` andava a capo). Lo prova `DurataStrettaTest`.
 */
@Composable
internal fun ClipLabel(length: Long?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(BADGE_EDGE)
            .clip(RoundedCornerShape(BADGE_CORNER))
            .background(BADGE_INK)
            .padding(horizontal = BADGE_PAD, vertical = BADGE_LIP),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BADGE_LIP)
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = stringResource(R.string.grid_item_clip),
            tint = Color.White,
            modifier = Modifier.size(BADGE_GLYPH)
        )
        length?.let {
            val style = MaterialTheme.typography.labelSmall
            BasicText(
                text = Videos.stamp(it, floor = true),
                style = style.copy(color = Color.White),
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = BADGE_MIN_TEXT,
                    maxFontSize = style.fontSize
                )
            )
        }
    }
}

/**
 * La superficie di un tasto a icona di Material, cioè il cerchio dell'increspatura.
 *
 * ⚠️ Quaranta e non quarantotto: 48 è il bersaglio del dito, che arriva da
 * `minimumInteractiveComponentSize`. Vedi la nota dentro [FilterKey].
 */
private val FILTER_KEY = 40.dp

/**
 * Il tasto del filtro volatile, in testata a destra quando non si sta scegliendo.
 *
 * Le scelte sono dell'utente:
 * - il tasto mostra l'icona del genere mostrato (spento le tre righe, acceso l'immagine o la
 *   pellicola), così dice che cosa si vede e non solo che si filtra; il tondo dietro dice che è
 *   acceso, perché dentro la stessa cartella il filtro resta;
 * - il popup è una fila di soli simboli: pellicola, immagine e la croce che toglie il filtro.
 * ⚠️ **`MediaKind.ALL` ha due disegni, ed è voluto**: sul tasto le tre righe ('non sto
 * filtrando'), nel popup la croce ('smetti di filtrare'). La croce è un'azione e non un genere,
 * quindi non si accende mai.
 * Le due icone sono quelle dei contatori sotto le copertine (`Outlined.Image` e
 * `Outlined.Movie`). Lo stato lo annuncia `stateDescription`: il tasto fa sempre la stessa cosa,
 * e il genere nella descrizione direbbe a TalkBack che serve a mostrare i video.
 */
@Composable
private fun FilterKey(filter: MediaKind, onFilter: (MediaKind) -> Unit, onSearch: () -> Unit) {
    val menu = rememberMenuState()
    val res = LocalResources.current
    Box {
        /*
         * ⚠️ **Non è un `IconButton` perché quello non espone il tocco lungo.** Le due misure
         * sono quelle del suo sorgente: 40dp di superficie ([FILTER_KEY]) e il bersaglio a 48
         * da `minimumInteractiveComponentSize`, o l'increspatura sarebbe diversa dagli altri
         * tasti della testata.
         * Il tocco lungo apre la ricerca per nome (richiesta dell'utente, dalla `1.50`): la
         * stessa della voce 'Cerca' della schermata iniziale, in tutta la galleria, e con la
         * stessa etichetta.
         */
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(FILTER_KEY)
                .clip(CircleShape)
                .combinedClickable(
                    onClick = { menu.open() },
                    onLongClick = withHaptics(onSearch),
                    hapticFeedbackEnabled = false,
                    onLongClickLabel = stringResource(R.string.hub_search),
                    role = Role.Button
                )
                .semantics { stateDescription = res.getString(filter.label()) },
            contentAlignment = Alignment.Center
        ) {
            FilterMark(lit = filter != MediaKind.ALL) {
                Icon(
                    imageVector = filter.onKey(),
                    contentDescription = stringResource(R.string.filter_title)
                )
            }
        }
        /*
         * Passa da [MenuShell], la superficie di tutti i menu, così ha anche il velo. Il
         * contenuto resta una fila di tre tasti: una disposizione diversa, non un secondo modo
         * di fare un menu.
         */
        MenuShell(
            state = menu,
            position = rememberMenuAtAnchor()
        ) {
            Row(modifier = Modifier.padding(horizontal = FILTER_PAD)) {
                // ⚠️ Pellicola, immagine e croce: l'ordine chiesto, non quello dell'enum. La
                // croce è in fondo perché non sceglie niente.
                for (kind in listOf(MediaKind.VIDEOS, MediaKind.IMAGES, MediaKind.ALL)) {
                    IconButton(onClick = { menu.close(); onFilter(kind) }) {
                        FilterMark(lit = kind != MediaKind.ALL && kind == filter) {
                            Icon(
                                imageVector = kind.inMenu(),
                                contentDescription = stringResource(kind.label())
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Il tondo dietro all'icona del filtro quando il filtro è in vigore.
 *
 * ⚠️ Più piccolo del bersaglio da 48dp: il tondo dipinto è [FILTER_MARK], e resta un segno
 * dentro la testata invece di sembrare un secondo tasto.
 */
@Composable
private fun FilterMark(lit: Boolean, glyph: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(FILTER_MARK)
            .clip(CircleShape)
            .background(
                if (lit) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides
                if (lit) MaterialTheme.colorScheme.onSecondaryContainer
                else LocalContentColor.current
        ) { glyph() }
    }
}

/** Il disegno che questo filtro porta **sul tasto**: spento sono le tre righe. */
private fun MediaKind.onKey(): ImageVector = when (this) {
    MediaKind.ALL -> Icons.Outlined.FilterList
    MediaKind.IMAGES -> Icons.Outlined.Image
    MediaKind.VIDEOS -> Icons.Outlined.Movie
}

/** Il disegno che questo filtro porta **nel popup**: 'tutto' là è la croce che lo toglie. */
private fun MediaKind.inMenu(): ImageVector = when (this) {
    MediaKind.ALL -> Icons.Default.Close
    else -> onKey()
}

/** Come si chiama questo filtro, per chi legge lo schermo. */
private fun MediaKind.label(): Int = when (this) {
    MediaKind.ALL -> R.string.filter_all
    MediaKind.IMAGES -> R.string.filter_images
    MediaKind.VIDEOS -> R.string.filter_videos
}

/**
 * Il peso totale di quello che si è scelto, in testata a destra.
 *
 * ⚠️ Il peso si chiede al MediaStore ([weightOf]): non è nell'indirizzo, quindi ogni cambio di
 * selezione è un'interrogazione.
 * ⚠️⚠️ **L'ATTESA PRIMA DI CONTARE ([WEIGH_WAIT]) RENDE LA FUNZIONE POSSIBILE**: col
 * trascinamento la selezione cambia decine di volte al secondo, e `produceState` annulla
 * l'attesa precedente a ogni cambio, quindi il conto parte quando il dito si ferma.
 * Vuoto e non zero mentre conta: uno zero è un peso, e sarebbe falso.
 */
@Composable
private fun PickWeight(chosen: Set<Uri>) {
    val context = LocalContext.current
    val weight by produceState<Long?>(null, chosen, context) {
        value = null
        delay(WEIGH_WAIT)
        // ⚠️ La sola somma e non tutti i dati: il perché è su [weightOf].
        value = weightOf(context, chosen.toList())
    }
    Text(
        text = weight?.let { formatBytes(it) }.orEmpty(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

/**
 * Il nome del file sotto una miniatura, su due righe al massimo.
 *
 * Le regole sono dell'utente (dalla `0.82`): al massimo due righe, estensione mai spezzata,
 * ellissi nel nome se serve. Sono quelle della pastiglia di 'Info dettagliate sul file', e le
 * rispetta la stessa [fitName], con due righe invece di tre.
 * ⚠️ Il nome non è nella lista della griglia, che ha soli indirizzi: lo dà [Names], che lo
 * ricorda, e il primo valore è quello già in memoria, così una miniatura che rientra in vista
 * non lampeggia senza nome. `labelSmall`, perché sotto un'immagine il nome è un'etichetta.
 */
@Composable
private fun GridName(uri: Uri, room: Int) {
    val context = LocalContext.current
    val name by produceState(Names.cached(uri), uri, context) {
        if (value == null) value = Names.of(context, uri)
    }
    val style = MaterialTheme.typography.labelSmall
    val measurer = rememberTextMeasurer()
    // ⚠️ Il margine si toglie dalla misura: arriva la larghezza della cella, e il testo ha
    // [NAME_PAD] in meno per lato. Sulla cella intera `fitName` conterebbe spazio che il
    // layout non gli dà, e l'ultima lettera sarebbe tagliata. Il margine c'è perché fra le
    // celle ci sono pochi dp, e due nomi lunghi vicini quasi si toccherebbero.
    val bordo = with(LocalDensity.current) { NAME_PAD.roundToPx() }
    val utile = room - bordo * 2
    val shown = remember(name, utile, style, measurer) {
        name?.let { fitName(it, utile, NAME_LINES, style, measurer) }
    } ?: return
    Text(
        text = shown.text,
        // ⚠️ Lo stile è quello con cui il nome è stato misurato: `fitName` può stringere corpo
        // o spaziatura, e alla misura piena il nome sforerebbe di nuovo.
        style = shown.style,
        maxLines = NAME_LINES,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = NAME_PAD)
    )
}

/**
 * Quanto si aspetta prima di contare il peso della selezione.
 *
 * ⚠️ Un terzo di secondo: abbastanza perché un trascinamento non faccia partire niente, poco
 * perché un tocco singolo non sembri lento.
 */
private const val WEIGH_WAIT = 300L

/**
 * Quante colonne entrano davvero: [scelte] sul lato corto, di più se lo schermo è largo.
 *
 * La scelta dell'utente dice quante colonne entrano sul lato corto; ruotando o su uno schermo
 * più largo ne entrano altre della stessa misura, col rapporto fra i due lati e senza una
 * larghezza di riferimento scritta a mano.
 * ⚠️ Non scende mai sotto la scelta, e con una finestra ancora da misurare (larghezza zero al
 * primo fotogramma) restituisce la scelta, invece di una griglia a una colonna che poi salta.
 * ⚠️ **È `internal` perché la usano tutte e due le griglie**, immagini e copertine: la voce
 * `Settings.folderColumns` è una, e deve comportarsi allo stesso modo in tutte e due.
 */
internal fun spread(scelte: Int, finestra: WindowInfo): Int {
    val misura = finestra.containerSize
    val corto = minOf(misura.width, misura.height)
    if (corto <= 0) return scelte
    return (scelte.toFloat() * misura.width / corto).roundToInt().coerceAtLeast(scelte)
}

/** Il distacco fra le miniature: c'è, ma non deve leggersi come una cornice. */
private val GAP = 3.dp

/** Aria fra le miniature: più larga sul tablet (giro 3.37, `3.31-01` C). */
@Composable
internal fun gridGap(): Dp {
    val w = LocalWindowInfo.current.containerSize.width
    val dp = with(LocalDensity.current) { w.toDp() }
    return if (dp.value >= Adaptive.PHONE_MAX) 8.dp else GAP
}

/**
 * Quante righe può prendere il nome sotto una miniatura: **due**, come chiesto.
 *
 * ⚠️ Due e non tre come nella pastiglia di 'Info': là il nome è il soggetto del dialogo, qui
 * è una didascalia sotto un'immagine.
 */
private const val NAME_LINES = 2

/** Quanto stacca il nome dalla sua miniatura: poco, perché sono la stessa cosa. */
private val NAME_GAP = 2.dp

/** Il margine laterale del nome, che [GridName] toglie anche dalla misura. */
private val NAME_PAD = 2.dp

// ── La targhetta della durata ───────────────────────────────────────────────
/** Quanto la targhetta si stacca dall'angolo della miniatura. */
private val BADGE_EDGE = 4.dp

/** L'arrotondamento della targhetta: poco, perché è una targhetta e non una pastiglia. */
private val BADGE_CORNER = 4.dp

/** Il nero della targhetta, semitrasparente perché l'immagine si intraveda sotto. */
private val BADGE_INK = Color(0x99000000)

/** Il respiro ai lati del testo dentro la targhetta. */
private val BADGE_PAD = 4.dp

/** Il corpo più piccolo della durata, quando la miniatura è troppo stretta per il suo. */
private val BADGE_MIN_TEXT = 8.sp

/** Il respiro sopra e sotto, e il distacco fra triangolo e cifre: sono la stessa misura. */
private val BADGE_LIP = 2.dp

/** Il triangolo: della misura del testo accanto, non di più. */
private val BADGE_GLYPH = 12.dp

/**
 * Quanto è lungo il cateto del nastro dell'ultimo elemento visto, in frazione del lato.
 *
 * ⚠️ È una frazione e non una misura in dp perché la piastrella non ha una misura fissa: le
 * colonne le conta [spread] e la griglia le dà a `GridCells.Fixed`, quindi su un tablet o in
 * orizzontale la miniatura cambia. `0,36` dalla `2.03`, più piccolo del mockup, su richiesta
 * dell'utente dopo averlo visto sul telefono.
 */
private const val MARK_LEG = 0.36f

/**
 * Quanto è opaco il nastro dell'ultimo elemento visto: l'85%, scelta dell'utente.
 *
 * ⚠️ Un filo di trasparenza lascia intravedere l'angolo dell'immagine, che a opacità piena il
 * nastro coprirebbe. Non è [PICKED_VEIL], che schiarisce l'intera miniatura.
 */
private const val MARK_ALPHA = 0.85f

/**
 * Quanto è spessa la cornice dell'ultimo elemento visto, in frazione del lato: il 5%, numero
 * dell'utente dopo averla vista (giro della `2.11`).
 *
 * ⚠️ È una frazione come [MARK_LEG], e non una misura in dp come il bordo dei pannelli: fra due e
 * cinque colonne il lato della cella quasi si triplica, e lo stesso numero in punti peserebbe il
 * triplo da una parte e un terzo dall'altra.
 */
private const val MARK_EDGE = 0.05f

/**
 * Quanto è opaca la cornice: l'80%, numero dell'utente (giro della `2.11`).
 *
 * Non è [MARK_ALPHA]: qui la trasparenza ammorbidisce il tratto sul bordo, là lascia
 * intravedere l'angolo coperto.
 * ⚠️ È `internal` perché `CorniceTest` ne calcola il colore atteso: un numero copiato sarebbe
 * una seconda fonte.
 */
internal const val MARK_FRAME_ALPHA = 0.8f

/**
 * Il tratto che segna l'ultimo elemento visto, dipinto in [color] dentro la sagoma [shape].
 *
 * Il colore è l'accento dell'app, dalla `2.13` (richiesta dell'utente): nastro, cornice e tondo
 * non si vedono mai insieme, sono le risposte dello stesso interruttore, e l'arancione
 * [HINT_MARK] resta ai soli mini onboarding. Arriva da fuori perché questo non è un composable
 * e non legge il tema.
 * ⚠️⚠️ **IL TRATTO SI DISEGNA DOPPIO E POI SI RITAGLIA**: è centrato sul contorno, quindi metà
 * cadrebbe fuori dalla miniatura; a `2 * spessore` dentro il ritaglio della sagoma resta
 * esattamente lo spessore voluto. Un `border` non può leggere uno spessore in frazione della
 * misura.
 * Vive fuori dal composable perché `CorniceTest` lo monta da solo: in Robolectric il MediaStore
 * è vuoto, e una miniatura vera non si può comporre.
 */
internal fun Modifier.lastFrame(shape: Shape, color: Color): Modifier = this
    .clip(shape)
    .drawBehind {
        drawOutline(
            outline = shape.createOutline(size, layoutDirection, this),
            color = color.copy(alpha = MARK_FRAME_ALPHA),
            style = Stroke(width = size.minDimension * MARK_EDGE * 2f)
        )
    }

/**
 * Il nastro triangolare che segna l'ultimo elemento visto, nell'angolo in basso dalla parte
 * dell'inizio della riga.
 *
 * ⚠️ **Segue il verso della lingua**, come la spunta (`Alignment.TopEnd`) e la durata
 * (`Alignment.BottomEnd`): da destra a sinistra va in basso a destra. In coordinate assolute, in
 * arabo, persiano e urdu finiva nell'angolo della durata e dal lato della spunta. Lo misura
 * `SegniTest` nei due versi. Vive fuori dal composable per la ragione di [lastFrame].
 */
internal fun Modifier.lastCorner(shape: Shape, color: Color): Modifier = this
    .clip(shape)
    .drawBehind {
        val leg = size.minDimension * MARK_LEG
        val start = if (layoutDirection == LayoutDirection.Ltr) 0f else size.width
        val toward = if (layoutDirection == LayoutDirection.Ltr) leg else -leg
        drawPath(
            path = Path().apply {
                moveTo(start, size.height)
                lineTo(start + toward, size.height)
                lineTo(start, size.height - leg)
                close()
            },
            color = color.copy(alpha = MARK_ALPHA)
        )
    }

/**
 * Il tondo che segna l'ultimo elemento visto: un disco del colore [color] a [MARK_DOT_ALPHA],
 * nell'angolo in basso dalla parte dell'inizio della riga. Il nome interno resta `DOT`, come la
 * chiave nell'archivio, anche se la voce dice 'Tondo'.
 *
 * ⚠️ Il diametro è una frazione del lato ([MARK_DOT]), come il nastro e la cornice, e l'aria
 * dai due bordi vale il raggio: il segno cresce tutto insieme e resta lo stesso tondo a
 * qualunque misura. Si specchia da destra a sinistra come [lastCorner]. Non ritaglia, perché
 * il disco è lontano dall'angolo stondato.
 */
internal fun Modifier.lastDot(color: Color): Modifier = this
    .drawBehind {
        val radius = size.minDimension * MARK_DOT / 2f
        val across = 2f * radius
        drawCircle(
            color = color.copy(alpha = MARK_DOT_ALPHA),
            radius = radius,
            center = Offset(
                x = if (layoutDirection == LayoutDirection.Ltr) across else size.width - across,
                y = size.height - across
            )
        )
    }

/**
 * Quanto è largo il tondo, in frazione del lato della miniatura: il 14%, scelto dall'utente
 * guardando le anteprime (domanda `d-pallino-misura`).
 */
private const val MARK_DOT = 0.14f

/**
 * Quanto è opaco il tondo: il 60%, scelto dall'utente sulla stessa domanda (dalla `2.94`).
 *
 * ⚠️ È `internal` per la ragione di [MARK_FRAME_ALPHA]: la prova ne calcola il colore atteso.
 */
internal const val MARK_DOT_ALPHA = 0.6f

/**
 * Quanto si schiarisce una miniatura scelta.
 *
 * ⚠️ Accompagna la spunta e non segnala da solo: su un'immagine già chiara un velo chiaro non
 * si nota, e il segno vero è il disco.
 */
private const val PICKED_VEIL = 0.34f

/** Il lato del disco della spunta. */
private val TICK = 28.dp

/** Il raggio degli angoli di una piastrella, in un posto solo perché lo usano in due. */
private val CORNER = 4.dp

/**
 * Quanto è alta la fascia, in cima e in fondo, dentro la quale un dito che trascina fa
 * scorrere la griglia da solo.
 *
 * ⚠️ Larga quanto mezza piastrella: più stretta la si manca, più larga si comincia a scorrere
 * mentre si sta ancora scegliendo. Questa e [EDGE_SPEED] sono `internal` perché le usa anche il
 * riordino a trascinamento (`Reorder.kt`), con lo stesso schema.
 */
internal val EDGE_BAND = 56.dp

/** Quanti pixel al fotogramma, al massimo, cioè col dito sul bordo estremo. */
internal val EDGE_SPEED = 14.dp


/** Tutti i riquadri sono la stessa cosa, e dirlo permette a Compose di riusarli. */
internal const val THUMB_KIND = "thumb"

/**
 * Il rientro orizzontale della schermata dai bordi dell'area sicura; quello verticale è
 * [GRID_PAD_Y].
 *
 * ⚠️ Un valore solo per tutti quelli che lo usano: la colonna, la sfumatura dell'intestazione, che
 * ne esce di esattamente quel tanto per arrivare ai bordi, e da fuori [MENU_INSET] e la larghezza
 * minima di un menu ancorato (`menuFloor`). Per questo è `internal`.
 */
internal val GRID_PAD_X = 8.dp

/** L'opacità massima della fascia in fondo a una cartella: il 65%, dalla `4.33` (voce `4.30-07`). */
private const val FOLDER_PEAK = 0.65f

/** Quanto della fascia resta a intestazione chiusa: il 60%, cioè il 40% in meno (voce `4.30-07`). */
private const val SCROLLED_FADE = 0.6f

/**
 * In quanto spazio di griglia, prima della fine, la fascia di una cartella scende a zero: due
 * centimetri ('gli ultimi centimetri' della voce `4.30-07`, lettura dichiarata nel DF).
 */
private val TAIL_REACH = 76.dp

/**
 * Quanto resta della fascia in fondo a una cartella, da 1 a 0 negli ultimi [coda] pixel di griglia.
 *
 * ⚠️ Una griglia che entra nello schermo resta a 1: non ha un fondo da raggiungere, e una
 * cartella corta altrimenti non avrebbe mai la fascia.
 */
private fun tailLeft(state: LazyGridState, coda: Float): Float {
    if (!state.canScrollBackward && !state.canScrollForward) return 1f
    val info = state.layoutInfo
    val last = info.visibleItemsInfo.lastOrNull() ?: return 1f
    if (last.index < info.totalItemsCount - 1) return 1f
    val fine = last.offset.y + last.size.height + info.afterContentPadding - info.viewportEndOffset
    return (fine / coda).coerceIn(0f, 1f)
}
private val GRID_PAD_Y = 12.dp

/**
 * Una pastiglia dell'intestazione: il peso della cartella, quante immagini, quanti video, o il
 * comando che seleziona tutto.
 *
 * ⚠️ Una sola per tutte e quattro, dello stesso colore neutro (scelta dell'utente, dalla
 * `1.85`): contorno, fondo della superficie, inchiostro smorzato.
 * Ogni pastiglia ha i due gesti, e il verso è uno solo: il tocco aggiunge quello che la
 * pastiglia nomina, il tocco lungo lo toglie. La vibrazione è sul solo tocco lungo.
 * ⚠️ I due gesti sono anche semantici (`onClick` e `onLongClick` con l'etichetta): il solo
 * `pointerInput` non compare nell'albero semantico, e TalkBack non li vedrebbe.
 *
 * @param modifier quello che il chiamante aggiunge: serve alla larghezza riservata di [FrontPick].
 */
@Composable
private fun FrontChip(
    text: String,
    holdLabel: String? = null,
    onHold: (() -> Unit)? = null,
    tapLabel: String? = null,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    /*
     * ⚠️ Le due azioni si leggono da uno stato aggiornato: `pointerInput(Unit)` cattura le
     * lambda quando parte, e il testo e l'azione di [FrontPick] cambiano con la selezione.
     */
    val tocca by rememberUpdatedState(onTap)
    val tieni by rememberUpdatedState(
        onHold?.let {
            {
                haptics.performHapticFeedback(HOLD_BUZZ)
                it()
            }
        }
    )
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .semantics {
                if (tapLabel != null) {
                    onClick(label = tapLabel) { tocca?.invoke(); true }
                }
                if (holdLabel != null) {
                    onLongClick(label = holdLabel) { tieni?.invoke(); true }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { tocca?.invoke() },
                    onLongPress = { tieni?.invoke() }
                )
            }
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .then(modifier)
    )
}

/**
 * La pastiglia comando dell'intestazione, che dice 'Seleziona tutto' oppure 'Deseleziona'.
 *
 * Le regole sono dell'utente: con tutto selezionato il testo diventa 'Deseleziona' e il tocco
 * scarta tutta la selezione di quel momento; il tocco lungo azzera sempre, e serve con una
 * selezione parziale.
 * ⚠️ **Il tocco resta un interruttore**, anche se la richiesta della `1.86` si può leggere 'al
 * tocco seleziona tutto' sempre: con tutto selezionato la pastiglia dice 'Deseleziona' (voce
 * `int-scarta` approvata), e fa quello che c'è scritto.
 * ⚠️ La pastiglia è larga quanto la più lunga delle due parole, misurata, così non si muove al
 * cambio: una seconda `Text` trasparente metterebbe una voce in più nell'albero semantico.
 */
@Composable
private fun FrontPick(picked: Boolean, onTap: () -> Unit, onHold: () -> Unit) {
    val misura = rememberTextMeasurer()
    val stile = MaterialTheme.typography.labelSmall
    val prendi = stringResource(R.string.pick_all)
    val scarta = stringResource(R.string.front_unpick)
    val largo = remember(prendi, scarta, stile, misura) {
        maxOf(
            misura.measure(prendi, stile).size.width,
            misura.measure(scarta, stile).size.width
        )
    }
    FrontChip(
        text = if (picked) scarta else prendi,
        tapLabel = if (picked) scarta else prendi,
        onTap = onTap,
        holdLabel = scarta,
        onHold = onHold,
        modifier = Modifier.width(with(LocalDensity.current) { largo.toDp() })
    )
}

/**
 * La comparsa e l'uscita animate di un comando in fondo alla griglia. I chiamanti sono due: il
 * FAB (nel cestino e nelle cartelle, fuori dalla selezione) e la pillola del telefono che ne
 * prende il posto.
 *
 * Entra con un'animazione (richiesta dell'utente, dalla `0.67`): il movimento dice che adesso
 * c'è qualcosa da fare. Cresce dal centro con una molla appena elastica ed esce secco, perché
 * un rimbalzo in uscita tratterrebbe lo sguardo su un angolo che si svuota.
 * ⚠️ È una funzione a sé perché dentro una `Column` `AnimatedVisibility` si risolverebbe
 * sull'overload di `ColumnScope`: qui si risolve sempre quella generale.
 */
@Composable
private fun FabPop(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            initialScale = FAB_SMALL
        ) + fadeIn(animationSpec = tween(FAB_IN)),
        exit = scaleOut(
            animationSpec = tween(FAB_OUT),
            targetScale = FAB_SMALL
        ) + fadeOut(animationSpec = tween(FAB_OUT))
    ) {
        content()
    }
}

/**
 * Il menu del FAB, nel cestino e in una cartella: sul lato del FAB, sopra di lui.
 *
 * ⚠️ **Si apre dove si apre quello della schermata iniziale** (richiesta dell'utente, dalla
 * `1.53`): i due chiedono lo stesso posizionatore, quindi non possono comportarsi in due modi.
 * Superficie e posizionatore sono in [MenuShell] e `MenuSpot`, condivisi da ogni menu dell'app:
 * un `DropdownMenu` si posiziona accanto al genitore e non accetta un posizionatore.
 * ⚠️ A chiuderlo con un tocco fuori è `MenuGuard` (in `Menus.kt`), e il FAB si limita ad aprire:
 * nessuno dei due riapre quello che l'altro ha appena chiuso.
 */
@Composable
private fun PickMenu(menu: MenuState, columns: Int, content: @Composable () -> Unit) {
    MenuShell(
        // ⚠️ La stessa posizione della schermata iniziale: allineato al FAB in orizzontale, e
        // sopra di lui perché sotto non entra.
        state = menu,
        position = rememberMenuAtAnchor(),
        /*
         * ⚠️ Il distacco è quello di questa griglia, più stretto di quello delle cartelle: la
         * misura si prende dalla griglia che il menu copre.
         */
        minWidth = menuFloor(spread(columns, LocalWindowInfo.current), GAP),
        content = content
    )
}

/**
 * Il tondo che segna il filtro in vigore, dietro alla sua icona.
 *
 * ⚠️ 36 e non 48: il tasto resta il bersaglio da 48 che Material chiede, ma il tondo dipinto è
 * più stretto. A 48 toccherebbe i bordi del bersaglio e la testata sembrerebbe avere un tasto in
 * più.
 */
private val FILTER_MARK = 36.dp

/**
 * Il respiro ai lati della fila di simboli del filtro.
 *
 * ⚠️ Serve perché quel popup non ha voci di menu, e senza voci non ha nemmeno il loro
 * rientro: i tre tasti finirebbero appiccicati al bordo stondato.
 */
private val FILTER_PAD = 4.dp

/**
 * Da quanto piccolo entra il FAB, e a quanto piccolo torna uscendo.
 *
 * ⚠️ 0,62 e non 0: da zero, con una molla elastica, il FAB sbucherebbe da un punto con un
 * rimbalzo; da due terzi si legge come 'era lì e si è fatto avanti'.
 */
private const val FAB_SMALL = 0.62f

/** La dissolvenza in entrata: più corta della molla, così il colore c'è già mentre cresce. */
private const val FAB_IN = 90

/** L'uscita, in millisecondi: secca, e più breve dell'entrata. */
private const val FAB_OUT = 110

/**
 * Quello che serve alla griglia per comporre la colonna delle cartelle sullo schermo largo.
 *
 * @property width la larghezza della colonna, la stessa del layout a due colonne.
 * @property onStart se la colonna è a sinistra: segue la mano, come nel visualizzatore.
 * @property list l'elenco delle cartelle ([FolderRailList]) col modificatore che gli dà l'altezza.
 */
class WideRail(
    val width: Dp,
    val onStart: Boolean,
    val list: @Composable (Modifier) -> Unit
)

/**
 * La griglia con accanto la colonna delle cartelle, o la griglia da sola.
 *
 * ⚠️ Senza colonna è un `Box` a tutto spazio: i figli che si allineano agli angoli trovano lo
 * stesso riquadro della griglia da sola.
 */
@Composable
private fun RailFrame(
    rail: WideRail?,
    head: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    if (rail == null) {
        Box(modifier = Modifier.fillMaxSize(), content = content)
        return
    }
    Row(modifier = Modifier.fillMaxSize()) {
        if (rail.onStart) FolderRailWide(width = rail.width, head = head, list = rail.list)
        Box(modifier = Modifier.weight(1f).fillMaxHeight(), content = content)
        if (!rail.onStart) FolderRailWide(width = rail.width, head = head, list = rail.list)
    }
}

/**
 * Quanto dell'altezza della testa può prendere l'icona, prima del tetto della fascia piena.
 *
 * È una scelta: poco meno della metà lascia sotto il posto per le pastiglie anche con la testa al
 * minimo, cioè con l'elenco delle cartelle al 70% della colonna.
 */
private const val HEAD_ICON_SHARE = 0.4f

/**
 * Fin dove arriva la sfumatura nella testa della colonna (dalla `3.71`, nota sul banding del giro
 * della `3.70`).
 *
 * È una scelta: circa l'altezza della fascia del telefono aperta, dove la sfumatura non ha dato
 * bande segnalate.
 */
private val HEAD_WASH_MAX = 200.dp

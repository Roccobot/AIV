package io.github.roccobot.aiv

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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.text.format.Formatter
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.layout.positionInParent

/**
 * Una cartella intera in miniature, ed è il primo passo della galleria.
 *
 * ⚠️⚠️ **L'ORDINE È QUELLO DI LETTURA, non uno suo**, ed è la decisione che tiene
 * insieme le due viste: le miniature stanno nella stessa sequenza in cui la
 * strisciata le sfoglierà, quindi 'la prossima' è la stessa cosa qui e là. Col verso
 * predefinito (`Cambia verso` spenta) è la più recente per prima, cioè l'ordine di
 * una galleria; accendendo l'impostazione si girano tutte e due insieme, perché
 * vengono dalla stessa lista girata una volta sola (vedi `Folder.Series.reversed`).
 * ⚠️ Chi un domani volesse la griglia 'sempre dalla più recente' rompe questa
 * corrispondenza: il tocco sulla terza miniatura aprirebbe la terzultima foto.
 *
 * ⚠️ **La lista arriva GIÀ PRONTA dal modello e non si interroga il MediaStore qui**:
 * è la stessa serie che il visualizzatore userà per sfogliare, quindi aprire una foto
 * dalla griglia non costa nessuna query e non può dare due ordini diversi.
 *
 * ⚠️ **Le miniature NON passano dal decodificatore normale**: le chiede al sistema
 * `Thumbs`, e là sta scritto perché.
 *
 * ⚠️⚠️ **[onChanged] SI CHIAMA DOPO OGNI OPERAZIONE, e senza di lui la griglia
 * mentirebbe**: copia, sposta, rinomina ed elimina cambiano i file sul disco, quindi la
 * lista che questa schermata ha in mano diventa vecchia nell'istante in cui l'operazione
 * finisce. Senza una rilettura resterebbero le miniature di fotografie che non esistono
 * più, e toccarle aprirebbe il vuoto.
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
     * ⚠️⚠️ **DUE PARAMETRI INVECE DI UNA SCHERMATA NUOVA, ed è la scelta che regge la
     * ricerca**: fra la griglia di una cartella e quella dei risultati cambia **solo la
     * testata**, e tutto il resto (miniature, selezione multipla, copia, sposta, rinomina,
     * elimina, apertura, anello) è lo stesso identico codice. Una schermata a parte
     * avrebbe voluto una seconda copia di tutto quello, cioè il posto dove le due si
     * sarebbero messe a divergere.
     */
    query: String? = null,
    onQuery: (String) -> Unit = {},
    /**
     * Se l'intestazione porta la **sfumatura** dell'accento: `Settings.frontWash`.
     *
     * ⚠️ **I quattro parametri dell'intestazione arrivano da fuori uno per uno**, come ogni altra
     * impostazione che questa schermata legge: la griglia non conosce `SettingsStore`, e i valori
     * di riserva dicono quello che dice il valore di fabbrica (vedi la nota su [columns]).
     */
    frontWash: Boolean = Settings().frontWash,
    /** Se il nome nell'intestazione è grande e graziato: `Settings.frontSerif`. */
    frontSerif: Boolean = Settings().frontSerif,
    /** Se l'intestazione porta le pastiglie del peso e dei video: `Settings.frontFacts`. */
    frontFacts: Boolean = Settings().frontFacts,
    /** Se l'intestazione porta la pastiglia 'Seleziona tutto': `Settings.frontPickAll`. */
    frontPickAll: Boolean = Settings().frontPickAll,
    /**
     * La tinta scelta per questa cartella, come indice in [FRONT_TINTS], oppure `null` per quella
     * dell'app.
     *
     * ⚠️ **Non è un'impostazione ma un dato della cartella**, e il perché vive su
     * `ViewerViewModel.tint`: qui arriva come tutti gli altri, cioè già risolto.
     */
    frontTint: Int? = null,
    /** Che cosa fare quando lui sceglie una tinta, o la toglie. */
    onFrontTint: (Int?) -> Unit = {},
    /**
     * Quanto pesa la cartella e quanti video ha, per le due pastiglie.
     *
     * ⚠️ **Arriva contato**: la griglia ha gli indirizzi e non le righe del MediaStore, quindi il
     * peso non se lo può ricavare senza una lettura per file. Lo conta `Folder.weigh`, una volta
     * per cartella, e il modello lo tiene.
     */
    facts: Folder.Facts = Folder.Facts(),
    /**
     * Il nome della cartella dentro cui si sta cercando, e `null` per la ricerca di tutta la
     * galleria.
     *
     * ⚠️⚠️ **PRENDE IL POSTO DELL'INVITO DEL CAMPO, DALLA `1.83`**: una ricerca ristretta e una
     * globale hanno la stessa testata, quindi senza questa riga niente direbbe che i risultati si
     * fermano a una cartella, e un elenco corto si leggerebbe come una galleria povera. Il nome è
     * un **dato** e non un testo da tradurre, che è la ragione per cui non costa una stringa.
     */
    searchIn: String? = null,
    /**
     * Quante colonne di miniature, sul lato corto dello schermo: `Settings.folderColumns`.
     *
     * ⚠️⚠️ **LA STESSA VOCE CHE GOVERNA LA SCHERMATA INIZIALE, DALLA `1.66`, ED È SUA
     * RICHIESTA** (*perché è solo per la home? Lo voglio anche nelle cartelle: dev'essere
     * un'impostazione globale*). Fino alla `1.65` qui le colonne le decideva una misura fissa e
     * là il suo numero, quindi la voce diceva 'griglia delle cartelle' e mentiva a metà.
     * ⚠️ **La chiave e il valore di fabbrica non si toccano**: la voce è spostata di dominio, non
     * sostituita, e chi aggiorna non deve perdere la scelta che aveva fatto.
     * ⚠️⚠️ **IL VALORE DI RISERVA SI RICAVA DAL VALORE DI FABBRICA, dalla `1.81`**: fino alla
     * `1.80` era `FOLDER_COLUMNS.first()`, che dava lo stesso numero **per caso** (censimento
     * della UI del 2026-09-05). Il giorno che l'elenco delle scelte cominciasse da 1, la
     * griglia montata senza questo parametro ne mostrerebbe una sola, e nessuno avrebbe
     * toccato il valore di fabbrica. Vale per i cinque parametri che ne hanno uno: la riserva
     * dice quello che l'impostazione dice, o non è una riserva.
     */
    columns: Int = Settings().folderColumns,
    /**
     * I campi delle informazioni sul file, nell'ordine scelto: `Settings.factRows`.
     *
     * ⚠️ **Arriva un elenco e non le impostazioni intere**: questa schermata non ne usa
     * nient'altro, e passarle tutte vorrebbe dire ricomporre la griglia a ogni ritocco di
     * una voce che qui non c'entra niente.
     * ⚠️⚠️ **IL VALORE DI SERIE TIENE IN PIEDI IL BANCO DI PROVA, e fino alla `1.80` qui era
     * scritto 'le anteprime'**, che nel progetto non esistono (censimento della UI del
     * 2026-09-05): nessuna `@Preview` e un solo insieme di sorgenti. La ragione vera è nata
     * dopo, con la `1.74`, e vale: `IntestazioneTest` monta questa schermata **vera** con i
     * soli argomenti che la prova misura, e chiedergli anche i nove che non c'entrano niente
     * vorrebbe dire scrivere in una prova dei dati che non guarda nessuno.
     * ⚠️ **Ed è il valore di fabbrica dell'impostazione**, non un valore comodo.
     */
    factFields: List<FactField> = Settings().factRows,
    /**
     * Se il cestino è acceso. Vedi `Settings.binOn`.
     *
     * ⚠️ **Decide due cose insieme**: se 'elimina' sposta nel cestino o cancella, e se prima
     * compaia una conferma. Il perché siano la stessa cosa sta in [FileJob.Delete].
     * ⚠️ Il valore di serie è quello di fabbrica dell'impostazione, letto da lei: chi lo
     * tiene in piedi è il banco di prova, e il perché è su [factFields].
     */
    binOn: Boolean = Settings().binOn,
    /** Che cosa il filtro volatile lascia vedere. Vedi `ViewerViewModel.gridFilter`. */
    filter: MediaKind = MediaKind.ALL,
    onFilter: (MediaKind) -> Unit = {},
    /**
     * Apre la ricerca dei file per nome: il tocco lungo sul filtro.
     *
     * ⚠️ **È la stessa che apre la voce 'Cerca' della schermata iniziale**, quindi cerca in
     * tutta la galleria: chi la chiama passa lo stesso `openSearch` di là.
     *
     * ⚠️⚠️ **SENZA VALORE DI RISERVA, dalla 1.53, e la ragione è un difetto vero**: nella
     * `1.50` questo parametro era arrivato al solo ramo della ricerca, e i due rami che
     * contano (la cartella e il cestino) prendevano il `{}` di riserva. Il gesto vibrava e
     * chiamava una funzione che non fa niente, cioè si presentava come una funzione rotta
     * senza che niente lo segnalasse: né il compilatore, né una lettura del codice, dove un
     * parametro assente si legge come una scelta.
     * ⚠️ **Adesso quel difetto non si può più scrivere**: chi aggiunge una schermata che usa
     * questa griglia deve dire dove porta il tocco lungo, o non compila. È il rimedio giusto
     * per un difetto che si vedeva solo provando l'app, che è la cosa che qui non si può
     * fare.
     */
    onSearch: () -> Unit,
    /*
     * ⚠️ **QUI C'ERA UN KDOC ORFANO FINO ALLA `1.78`**, e documentava un parametro della **mano**
     * che questa firma non porta più: la specchiatura delle file per la mano sinistra è uscita
     * del tutto nella `1.57`. Cominciava con una riga vuota e stava appaiato al blocco del
     * parametro successivo, quindi si leggeva come se parlasse di quello.
     */
    /** Se 'Copia lista' mette anche il percorso in testa. Vedi `Settings.listPath`. */
    listPath: Boolean = Settings().listPath,
    /** Se in testa alla selezione si legge il peso. Vedi `Settings.pickWeight`. */
    pickWeight: Boolean = Settings().pickWeight,
    /**
     * Se sotto ogni miniatura si legge il nome del file. Vedi `Settings.gridNames`.
     *
     * ⚠️ Il valore di serie è quello di fabbrica dell'impostazione, cioè **spento**: la griglia
     * montata dal banco di prova è quella di sempre (vedi [factFields]).
     */
    gridNames: Boolean = Settings().gridNames,
    /**
     * Con che segno si riconosce l'ultimo media visualizzato. Vedi `Settings.lastMark`.
     *
     * ⚠️ Il valore di serie è quello di fabbrica dell'impostazione, cioè la **cornice**: la
     * griglia montata dal banco di prova è quella che trova chi installa l'app adesso.
     */
    lastMark: LastMark = Settings().lastMark,
    /**
     * Se questa griglia è il **cestino**.
     *
     * ⚠️⚠️ **CAMBIA TRE COSE E NON L'ASPETTO**: 'elimina' diventa definitiva (là dentro non
     * c'è un secondo cestino), 'rinomina' diventa 'ripristina' (un file nel cestino non si
     * rinomina, richiesta dell'utente), e il FAB compare **anche senza selezione**, per
     * offrire 'svuota il cestino'. Tutto il resto, miniature comprese, è la griglia di
     * sempre: era la richiesta, cioè che il cestino si navighi come una cartella qualunque.
     */
    bin: Boolean = false,
    /**
     * Apre la cronologia dei ripristini. Vale **solo** quando [bin] è vero.
     *
     * ⚠️ Il valore di serie non fa niente, e va bene: fuori dal cestino la voce che lo
     * chiama non esiste, e un parametro obbligatorio costringerebbe le altre due griglie
     * (cartella e ricerca) a passare una funzione che non useranno mai.
     */
    onHistory: () -> Unit = {},
    /**
     * Dove manda 'Cestino' nel menu del FAB, e `null` quando di qui non ci si va.
     *
     * ⚠️ **Nulli di serie perché non ogni veste di questa griglia ha dove mandare**: la ricerca e
     * la cartella d'avvio la montano per mostrare un elenco, non per navigare l'app. Con tutti e
     * due nulli, in una cartella il FAB non compare affatto.
     */
    onBin: (() -> Unit)? = null,
    /** Dove manda 'Impostazioni' nel menu del FAB. Vedi [onBin]. */
    onSettings: (() -> Unit)? = null,
    /**
     * Apre la ricerca **dentro questa cartella**, dal menu del FAB. Vedi [onBin].
     *
     * ⚠️ **Non è [onSearch]**, che apre quella di tutta la galleria e vive nel tocco lungo sul
     * filtro: qui il confine è la cartella aperta, ed è la risposta di `d-fab-voci` (*in quel
     * caso, 'Cerca' è limitato alla cartella corrente*). A conoscere il bucket è il chiamante,
     * quindi questa griglia riceve un gesto e non un numero.
     */
    onSearchHere: (() -> Unit)? = null,
    /**
     * Comincia a scegliere la copertina di questa cartella: è il **tocco sull'icona**
     * dell'intestazione. Nullo quando di qui non si sceglie niente, come [onBin].
     *
     * ⚠️⚠️ **IL GESTO È QUELLO CHE HA SCELTO LUI** (risposta a `d-copertina-come`, giro della
     * `1.92`: *solo con il tocco singolo sull'icona dell'intestazione di una cartella*), ed è il
     * gesto che la `1.86` aveva lasciato libero aspettando *un'azione alternativa realmente
     * utile*.
     * ⚠️ **Comincia e basta: l'immagine si sceglie dopo, e altrove.** La seconda metà della sua
     * risposta dice *può essere scelta dalla normale vista di AIV da qualsiasi cartella*, quindi
     * quello che parte di qui è una modalità e non una finestra: il perché per esteso vive su
     * `ViewerViewModel.covering`.
     */
    onCoverPick: (() -> Unit)? = null,
    /** Toglie la copertina scelta: la voce del menu del FAB. Vedi [coverSet]. */
    onCoverClear: (() -> Unit)? = null,
    /**
     * Se questa cartella ha già una copertina scelta a mano.
     *
     * ⚠️ **Governa una voce sola, quella che la toglie**: senza, il menu offrirebbe di togliere
     * una cosa che non c'è. È lo stesso criterio di 'Mostra nascoste' nella schermata iniziale.
     */
    coverSet: Boolean = false,
    /**
     * Avvisa che la griglia ha una selezione viva, cioè che una rilettura le farebbe danno.
     *
     * ⚠️⚠️ **SERVE ALL'AGGIORNAMENTO AUTOMATICO e a nient'altro**: quando arriva un file da
     * fuori il modello rilegge la cartella da sé, e una lista nuova azzera la selezione (è
     * `remember(items)`, poco più sotto). Senza questo avviso, trenta foto spuntate
     * sparirebbero perché qualcuno ha mandato una fotografia da un altro dispositivo. Il
     * perché la rilettura non si limiti ad aspettare sta su `ViewerViewModel.gridBusy`.
     * ⚠️ Il valore di serie non fa niente, e per una funzione va bene: chi monta questa
     * schermata nel banco di prova non ha un modello dietro da avvisare.
     */
    onBusy: (Boolean) -> Unit = {},
    /**
     * Se la scelta di una copertina è in corso **per questa cartella**.
     *
     * ⚠️ **Serve solo al mini onboarding**, che compare la prima volta che il gesto avvia la
     * scelta: la modalità vive nel modello, perché fra l'inizio e la scelta si cambia schermata,
     * quindi da qui si vede solo come un `Boolean`.
     * ⚠️ **Per QUESTA cartella e non 'una qualunque'**: scegliendo la copertina di un'altra si
     * naviga, e un velo che comparisse là indicherebbe l'icona sbagliata.
     */
    coverHere: Boolean = false,
    /**
     * Rinomina la cartella aperta: il tocco lungo sul nome dell'intestazione.
     *
     * ⚠️ **`null` dove non c'è una cartella da rinominare**, cioè nel cestino, nella ricerca e nel
     * banco di prova: là l'intestazione non c'è, e con lei il gesto.
     * ⚠️ **La rinomina vera la fa il modello e non questa schermata**: tocca il disco, cambia
     * l'identificatore della cartella e con lui la copertina e la tinta, e alla fine riapre la
     * griglia. Da qui esce il solo nome nuovo.
     */
    onFolderRename: ((String) -> Unit)? = null
) {
    val state = rememberLazyGridState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ⚠️ Le risorse si prendono da `LocalResources` e non da `context.resources`, e non è
    // pignoleria di lint: quest'ultimo non segue i cambi di configurazione, quindi dopo un
    // cambio di lingua o una rotazione servirebbe la versione vecchia. Si legge QUI,
    // mentre si compone, e si usa dentro le coroutine.
    val res = LocalResources.current

    /**
     * Gli INDIRIZZI scelti, non le posizioni.
     *
     * ⚠️⚠️ **Le posizioni sarebbero un difetto in attesa**: la lista si ricarica quando la
     * cartella cambia, e un indice che era la terza foto diventa la terza **di un'altra
     * lista** senza che niente lo dica. Un indirizzo o c'è ancora o non c'è, e nel secondo
     * caso sparisce dalla selezione da sé.
     * ⚠️ Vive nella SCHERMATA e non nel modello, perché se ne va con lei: uscire da una
     * cartella è il modo naturale di dire 'lascia stare'.
     * ⚠️⚠️ **MA SOPRAVVIVE ALLA ROTAZIONE, DALLA `1.81`** (riscontro dell'utente: *ruotando la
     * selezione della griglia si scioglie*). Girare il telefono non è uscire da una cartella:
     * l'attività si ricrea e la composizione si rifà da zero, quindi un `remember` perdeva
     * trenta spunte per un gesto che non dice niente sulla selezione.
     * ⚠️ **Il salvatore passa da una LISTA e non salva l'insieme**: un `Set` il `Bundle` non lo
     * scrive, un `Uri` sì perché è `Parcelable`. L'ordine che la lista impone non conta, perché
     * a rientrare è un insieme.
     */
    var chosen by rememberSaveable(items, stateSaver = UriSetSaver) {
        mutableStateOf<Set<Uri>>(emptySet())
    }
    val menu = rememberMenuState()

    /*
     * ⚠️⚠️ **IL PANNELLO NON HA PIÙ UN INTERRUTTORE PROPRIO, dalla 1.06** (riscontro
     * dell'utente sul collaudo: *selezione e bottomsheet devono sempre convivere e
     * apparire/sparire insieme*). Fino alla `1.05` c'era un `sheetOpen` separato da
     * `picking`, e le due cose potevano stare in tre stati invece che in due: il terzo era
     * una selezione **viva e invisibile**, senza il pannello che dice che cosa farci. Ci si
     * finiva col gesto Indietro, che chiudeva prima il pannello e lasciava le spunte accese.
     * ⚠️ Adesso il pannello si vede **esattamente** quando c'è una selezione, e non esiste
     * più nessuno stato da tenere d'accordo con lei. Chi volesse rimettere un modo di
     * nascondere il pannello tenendo la selezione rimetterebbe quel terzo stato.
     */

    /** Quanto è alto il pannello, in pixel, per lasciargli il posto sotto la griglia. */
    var sheetTall by remember { mutableIntStateOf(0) }

    /**
     * La selezione che il gesto Indietro ha appena azzerato, e `null` quando non c'è niente da
     * rimettere. È lei a far comparire la notifica con 'Annulla'.
     *
     * ⚠️⚠️ **DALLA 1.44 INDIETRO AZZERA SENZA CHIEDERE, e la conferma della 1.06 è uscita**
     * (istruzione dell'utente, 2026-09-03: *'indietro' di sistema dalla selezione la deve
     * cancellare e far sparire la bottomsheet, senza conferma. Ma allo stesso tempo deve
     * apparire per 3 secondi una notifica in basso*). Il difetto che la conferma copriva è lo
     * stesso (trenta tocchi persi per sbaglio), ma la cura è cambiata: **prima** si chiedeva e
     * si aspettava, **adesso** si fa e si offre di disfare. Un dialogo si paga sempre, una
     * notifica solo se serve.
     * ⚠️ **Tiene la selezione e non un booleano**: 'annulla' deve rimettere *quelle*
     * fotografie, e un flag saprebbe solo che qualcosa è stato azzerato.
     * ⚠️⚠️ **LA CHIAVE È IL TITOLO, ed è quello che la fa sparire cambiando cartella** (*o
     * finché non si cambia cartella*): il titolo è la sola cosa che cambia quando si esce da
     * questa cartella, ed è già la chiave che [worked] usa per la stessa ragione. Con `items`
     * si azzererebbe a ogni ricarica della lista, cioè anche restando qui.
     */
    var cleared by remember(title) { mutableStateOf<Set<Uri>?>(null) }

    /*
     * ⚠️ **Tre secondi, come li ha chiesti**, e il conto riparte da zero se si azzera una
     * seconda selezione: la chiave dell'effetto è [cleared], quindi un'altra pressione di
     * Indietro rimette la notifica in scena per tre secondi suoi.
     * ⚠️⚠️ **DALLA `1.84` LA RIGA LA MANDA AL CANALE, e l'attesa non è più qui** (sua risposta
     * `casa` a `d-avvisi`): la superficie con cui l'app parla è una sola, quindi la durata e il
     * conto alla rovescia vivono con lei, in `Notice.kt`. Qui resta la sola cosa che è di questa
     * schermata, cioè **quale** selezione rimettere.
     * ⚠️⚠️ **E LA CLAUSOLA 'O FINCHÉ NON SI CAMBIA CARTELLA' ADESSO SI OTTIENE TOGLIENDOLA**: con
     * uno stato locale bastava la chiave del titolo, perché la notifica moriva con la schermata;
     * una riga che vive nel processo no, e senza il congedo qui sotto uscirebbe dalla cartella
     * insieme a chi la legge.
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
            // ⚠️ La selezione da rimettere è quella catturata quando la riga è nata, non
            // `cleared` riletto adesso: fra il tocco e questa riga lo stato è già stato azzerato
            // dal congedo, e rileggerlo rimetterebbe una selezione vuota.
            chosen = quali
        }
    }
    /*
     * ⚠️ **Il congedo ha per chiave il TITOLO**, che è la sola cosa che cambia uscendo da questa
     * cartella, ed è già la chiave di [cleared] per la stessa ragione. Con `items` girerebbe a
     * ogni ricarica della lista, cioè anche restando qui.
     * ⚠️ **Toglie la SUA riga e non quella in scena**: nei tre secondi può esserne arrivata
     * un'altra, e un congedo cieco porterebbe via un messaggio che non è suo.
     */
    DisposableEffect(title) {
        onDispose { rigaAzzerata?.let { Notices.dismiss(it) } }
    }

    /**
     * Se in questa visita si è già eseguita un'operazione sui file.
     *
     * ⚠️⚠️ **LA CHIAVE È IL TITOLO E NON `items`, e con `items` NON FUNZIONEREBBE**: dopo
     * un'operazione la lista si ricarica, quindi una bandierina legata a lei si
     * riazzererebbe **proprio nell'istante** in cui serve leggerla, e la risalita non
     * scatterebbe mai. Il titolo cambia quando si cambia cartella, che è la sola cosa che
     * deve dimenticare l'operazione fatta.
     */
    var worked by remember(title) { mutableStateOf(false) }

    /**
     * Il dialogo di un'operazione, e `null` quando non ce n'è aperto nessuno.
     *
     * ⚠️ **Uno stato solo per quattro dialoghi**, dalla `0.62`: erano quattro variabili, e
     * quattro booleani indipendenti descrivono sedici combinazioni di cui quindici
     * impossibili. Qui i dialoghi si escludono per costruzione. ⚠️ Porta con sé le immagini
     * su cui lavorare, e il perché sta in [FileJob].
     * ⚠️⚠️ **SOPRAVVIVE ALLA ROTAZIONE DALLA `1.81`, e prima no** (riscontro dell'utente:
     * *ruotando si chiude la finestra aperta e con lei quello che stavi scrivendo*). Che cosa si
     * salva e che cosa no, e perché i lavori che partono da sé non si ripristinano, sta su
     * [FileJobSaver].
     */
    var job by rememberSaveable(stateSaver = FileJobSaver) { mutableStateOf<FileJob?>(null) }

    /** Se si sta chiedendo di svuotare il cestino. Vale solo quando [bin] è vero. */
    var emptying by rememberSaveable { mutableStateOf(false) }
    /** Se la conferma di 'Ripristina tutto' è in scena. Vedi la nota su quella voce. */
    var restoringAll by rememberSaveable { mutableStateOf(false) }
    /**
     * Se il selettore della tinta è in scena: lo apre il tocco lungo sull'icona
     * dell'intestazione.
     *
     * ⚠️ **Vive qui e non accanto alla fascia** perché la finestra sta col resto dei dialoghi, e
     * quello che la apre è dentro un nodo che si rimisura a ogni pixel di scorrimento.
     */
    var tinge by rememberSaveable { mutableStateOf(false) }
    /**
     * Se la finestra che rinomina la cartella è in scena: la apre il tocco lungo sul nome
     * dell'intestazione.
     *
     * ⚠️ **Vive qui accanto a [tinge] per la stessa ragione**: chi la apre è dentro la fascia,
     * che si rimisura a ogni pixel di scorrimento, e la finestra sta col resto dei dialoghi.
     */
    var rinomina by rememberSaveable { mutableStateOf(false) }
    val picking = chosen.isNotEmpty()

    // ⚠️ In un effetto e non a filo della composizione: avvisare il modello è un cambiamento
    // di stato fuori da qui, e farlo mentre si disegna vorrebbe dire scrivere e leggere lo
    // stesso dato nello stesso giro. La chiave è il **se** e non l'insieme: aggiungere la
    // trentunesima foto alla selezione non è una notizia nuova.
    LaunchedEffect(picking) { onBusy(picking) }
    /*
     * ⚠️⚠️ **UNA SELEZIONE NUOVA SPENDE IL 'DISFA', ed è la ragione per cui questo effetto
     * esiste**: 'Annulla' rimette *quelle* immagini, quindi con una selezione nuova in corso
     * non saprebbe se sostituirla o sommarsi, e in tutti e due i casi porterebbe via qualcosa
     * che l'utente ha appena scelto. La notifica se ne va, e il gesto Indietro ricomincia il
     * giro da capo con la selezione di adesso.
     * ⚠️ **Sta in un effetto sulla chiave `picking` e non nei punti in cui una selezione
     * nasce**, che sono tre (il trascinamento dopo il tocco lungo, `takeAll` e l'inversione),
     * e il primo di loro scrive la selezione a ogni fotogramma: là sarebbe una riga da
     * ricordare in tre posti, qui è la regola scritta una volta.
     */
    LaunchedEffect(picking) { if (picking) cleared = null }
    // ⚠️ E uscendo dalla griglia la selezione se ne va con la schermata, quindi il modello va
    // liberato: senza resterebbe convinto che c'è una selezione viva, e non rileggerebbe mai
    // più da sé.
    DisposableEffect(Unit) { onDispose { onBusy(false) } }

    /**
     * Se c'è qualcosa qui dentro.
     *
     * ⚠️ Serve alle due voci del cestino che agiscono su **tutto** (svuota e ripristina
     * tutto): su un cestino vuoto non hanno niente da fare, e offrirle vorrebbe dire una
     * conferma o un avviso che dice '0 fatti'. ⚠️ **Vale anche per la scorciatoia del tocco
     * lungo**, o il menu direbbe di no e il tocco lungo di sì. Con la lista vuota resta la
     * vibrazione e non succede niente, che è quello che 'tutte' fa già su una cartella vuota.
     */
    val filled = !items.isNullOrEmpty()

    /*
     * ⚠️ Indietro esce dalla SELEZIONE prima di uscire dalla cartella: chi ha scelto
     * trenta foto e tocca Indietro per sbaglio non deve ritrovarsi due schermate indietro
     * con la selezione persa.
     * ⚠️⚠️ **AZZERA SUBITO E OFFRE DI DISFARE, dalla 1.44: la conferma della 1.06 è uscita.**
     * Il perché del cambio sta su [cleared]. ⚠️ **Il terzo giro di una stessa questione**, e
     * conviene conoscerli tutti e tre per non tornare al primo: dalla `0.94` alla `1.05`
     * Indietro chiudeva prima il **pannello** e solo al secondo tocco la selezione, che
     * lasciava una selezione viva senza pannello, cioè uno stato in cui non si capisce di
     * esserci dentro; dalla `1.06` chiedeva conferma con un dialogo; da adesso fa e offre di
     * disfare. Le tre coprono lo stesso sbaglio, e questa è la sola che non costa niente a chi
     * il gesto lo aveva fatto per davvero.
     */
    BackHandler(enabled = picking) {
        cleared = chosen
        chosen = emptySet()
        /*
         * ⚠️⚠️ **LE DUE NOTIFICHE NON SI SOVRAPPONGONO PIÙ PER COSTRUZIONE, DALLA `1.84`**, e
         * questa riga se ne va con la ragione che la teneva in piedi. Il difetto era del
         * censimento della UI del 2026-09-05: le due superfici avevano lo stesso allineamento in
         * fondo allo schermo, quindi non si affiancavano ma si coprivano, e lo spegnimento
         * incrociato andava in un verso solo. Adesso il canale è **uno**, quindi una riga nuova
         * prende il posto di quella in scena e l'offerta di disfare si chiude da sé
         * (`Notices.posa` chiama il suo congedo).
         * ⚠️ **L'ordine resta quello giusto**: l'azzeramento è la conseguenza del gesto appena
         * fatto, quindi è la notizia di adesso; l'offerta di disfare stava scadendo da sé, e chi
         * voleva usarla l'avrebbe già toccata.
         */
    }

    /**
     * Dove sta il dito mentre trascina una selezione, e `null` quando non trascina.
     *
     * ⚠️ Esiste anche per lo SCORRIMENTO AI BORDI: senza un posto in cui leggere la
     * posizione fuori dai richiami del gesto, la griglia non potrebbe scorrere da sola
     * mentre il dito sta fermo appoggiato in fondo allo schermo.
     */
    var dragAt by remember { mutableStateOf<Offset?>(null) }

    /** Da dove è partita la selezione da/a. Null quando non si sta trascinando. */
    var dragFrom by remember { mutableStateOf<Int?>(null) }

    /**
     * La selezione com'era **prima** che questo trascinamento cominciasse.
     *
     * ⚠️ Senza, tornare indietro col dito non toglierebbe niente: la selezione va
     * **ricostruita** a ogni fotogramma come 'quella di prima più l'intervallo di adesso',
     * non accumulata. Accumulando, un intervallo attraversato per sbaglio resterebbe scelto
     * anche dopo essere tornati sui propri passi.
     */
    var dragBase by remember { mutableStateOf<Set<Uri>>(emptySet()) }

    /**
     * Il VERSO del trascinamento: `true` toglie, `false` aggiunge.
     *
     * ⚠️⚠️ **Lo decide la foto su cui il gesto COMINCIA, e non un modo da accendere**
     * (richiesta dell'utente: *tenendo premuto e trascinando quando una selezione c'è già
     * si possa deselezionare*): partendo da una foto già scelta il trascinamento toglie
     * l'intervallo, partendo da una libera lo aggiunge. È la convenzione di ogni galleria,
     * e soprattutto è l'unica che non ha bisogno di un interruttore da trovare: il gesto
     * dice da sé che cosa vuole.
     * ⚠️ Serve **fuori** dai richiami del gesto perché l'intervallo lo ricostruisce
     * l'effetto qui sotto a ogni fotogramma, e deve sapere in che verso.
     */
    var dragOff by remember { mutableStateOf(false) }

    // ⚠️ Quale vibrazione, e perché quella, sta su [HOLD_BUZZ]: qui c'era la nota, e la
    // scelta si è spostata là quando è diventata una sola per tutta l'app. Due copie della
    // stessa spiegazione divergono alla prima modifica.
    val haptics = LocalHapticFeedback.current

    /**
     * Se il mini onboarding del tocco lungo sul FAB si è già visto.
     *
     * ⚠️ **Era 'i due' fino alla `1.78`, e ne resta uno**: quello della selezione è uscito con
     * la sua chiave nella `0.94` (lo dichiara `Settings.kt`), e la nota non l'aveva seguito.
     *
     * ⚠️⚠️ **Il valore di partenza è `true`, cioè 'già visto', e al contrario di quanto
     * sembra è la scelta prudente**: il valore vero arriva dall'archivio un attimo DOPO la
     * prima composizione, quindi partendo da `false` il velo comparirebbe per un
     * fotogramma anche a chi l'ha già chiuso, che è il difetto peggiore dei due. Partendo
     * da `true` il caso peggiore è che compaia un fotogramma più tardi, e nessuno se ne
     * accorge.
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
     * Dove sta l'icona dell'intestazione, in coordinate della radice.
     *
     * ⚠️⚠️ **SI MISURA INVECE DI RICALCOLARLA**, perché il velo che la evidenzia deve caderci
     * sopra: la sua posizione dipende dai rientri di sistema, dalla testata, da quanto la fascia è
     * aperta e dalla misura che l'icona cede scorrendo, cioè da quattro cose che cambiano. Il
     * perché per esteso vive su [HintSpot].
     * ⚠️ **`null` finché non è stata disegnata**, ed è la condizione che tiene il velo fuori
     * scena: senza il riquadro non c'è niente da indicare.
     */
    var iconaSpot by remember { mutableStateOf<Rect?>(null) }

    /**
     * 'Tutte', che è il gesto che vale trecento tocchi.
     *
     * ⚠️ Vive in una variabile perché lo chiamano in **tre** posti: il tasto 'Tutti' del
     * pannello, il tocco lungo sul FAB e la sua copia arancione nel velo. Scriverlo tre volte
     * vorrebbe dire tre occasioni di dimenticare la vibrazione in uno dei tre.
     */
    val takeAll: () -> Unit = {
        haptics.performHapticFeedback(HOLD_BUZZ)
        chosen = items?.toSet() ?: emptySet()
    }

    /**
     * Se tutto quello che c'è in questa griglia è già scelto. Vuota, non conta come 'tutto'.
     *
     * ⚠️⚠️ **NON È IL VERSO DEL TOCCO LUNGO SUL FAB, E LA `1.87` HA MISURATO PERCHÉ NON PUÒ
     * ESSERLO** (sua richiesta: *la pressione lunga sul FAB in una cartella deve
     * selezionare/deselezionare tutto*): il FAB **non è in scena** quando una selezione è in
     * corso, perché al suo posto si apre la scheda dei comandi (vedi `visible` di [FabPop], che
     * porta `!picking`). Quindi un'alternanza scritta sulla sua scorciatoia sarebbe un ramo che
     * nessun dito può raggiungere, cioè codice morto che sembra una funzione.
     * ⚠️ **Il rovescio esiste, e sono due**: il tasto 'Tutti' del pannello col proprio tocco
     * lungo, e la pastiglia dell'intestazione dalla `1.85`. La domanda `d-fab-tutto` chiede a lui
     * se gli basta o se il FAB deve restare in scena durante la selezione.
     * ⚠️ **Il conto vive qui e non nell'intestazione** perché lo leggono in due: chi disegna la
     * pastiglia e chi decide che cosa annuncia il tocco lungo.
     */
    val allTaken = !items.isNullOrEmpty() && chosen.containsAll(items.orEmpty())

    /**
     * ⚠️⚠️ **IL TOCCO LUNGO È LA SCORCIATOIA DI QUELLO CHE IL TOCCO BREVE OFFRE**, ed è la
     * regola che decide questo `if` (richiesta dell'utente, 2026-08-31: *solo nel cestino,
     * il tocco lungo lo svuota*). Nel cestino **senza niente di scelto** il tocco breve apre
     * il menu del cestino intero, la cui voce grossa è 'Svuota il cestino': la scorciatoia è
     * quella. Appena c'è una selezione, in cestino o in cartella, il tocco breve apre le sei
     * operazioni e la scorciatoia torna a essere 'tutte'.
     * ⚠️ **La scorciatoia NON è cambiata quando il menu è passato a tre voci, dalla 0.76**, e
     * la ragione è la regola qui sopra: le altre due ('Ripristina tutto' e 'Cronologia') sono
     * raggiungibili in un tocco e non hanno bisogno di una scorciatoia, mentre svuotare resta
     * la cosa che un cestino fa.
     * ⚠️ **Il cestino non si svuota MAI con una selezione in corso**, e non è timidezza: chi
     * ha scelto tre foto da ripristinare si aspetta che il gesto agisca su quelle tre, e
     * cancellare invece tutto il cestino sarebbe la sorpresa peggiore che l'app possa fare.
     * La conferma lo fermerebbe comunque, ma un dialogo che chiede una cosa che non hai
     * chiesto è già un difetto.
     * ⚠️ Lo svuotamento **non** si esegue qui: accende il dialogo, che è l'unico posto in
     * cui quel comando esiste (vedi `emptying`).
     */
    val shortcut: () -> Unit = if (bin && !picking) {
        {
            haptics.performHapticFeedback(HOLD_BUZZ)
            if (filled) emptying = true
        }
    } else takeAll

    /**
     * Come TalkBack chiama il tocco lungo, che deve dire la stessa cosa che [shortcut] fa.
     *
     * ⚠️ Senza questa, chi usa il lettore di schermo si sentirebbe annunciare 'seleziona
     * tutte' su un gesto che svuota il cestino: la scorciatoia esisterebbe solo per chi vede
     * il velo, e per gli altri sarebbe una trappola.
     * ⚠️ **Non ha un terzo caso per 'tutto già scelto', e la `1.87` ha misurato perché**: con
     * una selezione in corso questo FAB non è in scena affatto. Il perché per esteso, e le due
     * vie che il rovescio ha davvero, vivono su [allTaken].
     */
    val shortcutLabel = if (bin && !picking) R.string.bin_empty else R.string.pick_all

    /**
     * ⚠️ La bandierina locale esiste perché l'archivio risponde con un giro di ritardo:
     * scrivere in DataStore e aspettare che il flusso riemetta vuol dire un fotogramma o
     * due col velo ancora steso, e nel caso peggiore col menu che si apre **sotto** di
     * lui. Questa lo toglie sull'istante; la scrittura serve alle sessioni dopo.
     */
    var binOff by remember { mutableStateOf(false) }

    /**
     * Se il velo è steso adesso, e quale.
     *
     * ⚠️⚠️ **IL RAMO È UNO, E FINO ALLA `1.78` QUESTA NOTA SPIEGAVA UNA PRECEDENZA FRA DUE**:
     * il velo della selezione è uscito nella `0.94` insieme alla sua chiave, quindi non c'è
     * niente da ordinare. Resta un `when` invece di un `if` perché un velo nuovo si aggiunge
     * come ramo, e allora la precedenza si scrive quando esiste.
     */
    val hint: Hint? = when {
        bin && !binSeen && !binOff -> Hint.BIN_EMPTY
        else -> null
    }

    /*
     * ⚠️⚠️ **IL PANNELLO C'È PERCHÉ C'È LA SELEZIONE, e non perché un effetto l'ha aperto**
     * (vedi la nota su `sheetOpen`, tolto nella `1.06`). Dalla `0.75` alla `1.05` qui stava
     * un `LaunchedEffect` che lo apriva da sé alla prima foto scelta, e serviva perché il
     * pannello aveva una vita propria: adesso non ce l'ha più, quindi non c'è niente da
     * aprire e la richiesta di allora (*scelta la prima foto, l'azione è la cosa che si
     * vuole fare*) è vera per costruzione.
     * ⚠️ **Sotto il velo dell'onboarding il pannello si vede, ed è innocuo**: quel velo
     * copre lo schermo intero (`1.03`), quindi quello che gli sta sotto non si vede
     * comunque. Prima l'effetto doveva aspettarlo perché un menu **a comparsa** sarebbe
     * spuntato *sopra* il velo, che è un'altra cosa.
     */

    /**
     * Il velo si archivia appena l'utente fa la cosa che insegnava, o appena la salta.
     *
     * ⚠️ Col ramo `null` che non fa niente, e non è ridondanza: questa funzione la chiama
     * anche il FAB **vero**, dove un velo non c'è, e senza quel ramo un tocco lungo
     * ordinario archivierebbe un promemoria mai mostrato.
     */
    val hintDone: () -> Unit = {
        when (hint) {
            Hint.BIN_EMPTY -> binOff = true
            // ⚠️ Le colonne non si insegnano qui, ma il ramo c'è perché l'enum le porta:
            // il velo che le riguarda vive nella schermata delle cartelle. Dalla 1.25 vale
            // lo stesso per il doppio tocco, che vive nel visualizzatore.
            // ⚠️ Dalla 1.36 c'è anche l'avviso sul cambio di estensione, che vive dentro
            // la finestra di rinomina: stessa storia, ramo obbligato dall'enum.
            // ⚠️ Dalla 1.95 c'è anche quello della copertina, che vive in questa schermata ma
            // non passa da qui: indica l'icona dell'intestazione, quindi ha un velo suo e si
            // archivia per conto proprio.
            Hint.COLUMNS, Hint.ZOOM_TAP, Hint.EXT_WARN, Hint.COVER -> Unit
            null -> Unit
        }
        hint?.let { seen -> scope.launch { seen.remember(context) } }
    }

    /**
     * Le dieci operazioni sulla selezione, nell'ordine in cui l'utente le ha chieste.
     *
     * ⚠️⚠️ **DIECI E NON SEI, dalla 0.94** (richiesta dell'utente, con le sue etichette
     * brevi): alle sei di prima si aggiungono 'Lista', 'Inverti', 'Tutti' e 'Nessuno'. Le
     * ultime tre non toccano nessun file, e stare accanto a quelle che li toccano è
     * deliberato: sono tutte cose che si fanno **sulla selezione**, e cercarle in due posti
     * diversi era il passaggio a vuoto che questa versione toglie.
     * ⚠️⚠️ **LA FILA È SBILANCIATA A DESTRA APPOSTA** (sue parole: *in modo che le funzioni
     * usate più di frequente siano comodamente raggiungibili con il pollice*), quindi
     * l'ordine non si 'sistema': 'Copia' in fondo alla prima fila e 'Nessuno' in fondo alla
     * seconda sono la posizione più comoda, non l'ultimo posto rimasto.
     * ⚠️⚠️ **NESSUNA VOCE CHIUDE PIÙ IL PANNELLO, dalla 1.06**: fino alla `1.05` le
     * operazioni sui file lo chiudevano prima di aprire il proprio dialogo, ed era corretto
     * finché il pannello era una cosa a sé. Adesso il pannello **è** la selezione (vedi la
     * nota su `sheetOpen`), quindi chiuderlo vorrebbe dire scioglierla: il dialogo di
     * un'operazione gli si disegna sopra, e alla fine dell'operazione la selezione si svuota
     * da sé (`perform`) portandosi via il pannello.
     *
     * ⚠️⚠️ **QUESTA LISTA NON È RICORDATA, ED È UNA SCELTA MISURATA E NON UNA DIMENTICANZA**
     * (censimento della UI del 2026-09-05, dove il rilievo è confermato e il rimedio proposto è
     * un `remember`). Le dieci chiusure catturano quattro parametri (`bin`, `binOn`,
     * `listPath`, `items`) più il contesto, le risorse e l'ambito, quindi un `remember` vorrebbe
     * **sette chiavi** da tenere allineate a mano: chi aggiungesse un'azione che cattura un
     * parametro nuovo senza aggiungere la sua chiave otterrebbe una lambda che legge un valore
     * **congelato**, e quel difetto non dà nessun errore né al build né a schermo. Il costo che
     * si evita è dieci allocazioni per ricomposizione **della schermata**, non per fotogramma:
     * `chosen` ha l'uguaglianza strutturale di serie, quindi un fotogramma di trascinamento che
     * ricalcola lo stesso insieme non ricompone niente.
     * ⚠️ **Quello che si legge vivo si legge vivo comunque**: `chosen` e `job` sono stati, e una
     * lambda ricordata li leggerebbe al momento della chiamata. Il rischio è tutto sui
     * parametri, che stati non sono.
     */
    val pickActions = listOf(
        // ⚠️ Nel cestino al posto della rinomina c'è il ripristino: un file là dentro non si
        // rinomina (richiesta dell'utente), e il posto nel pannello è lo stesso, così le
        // dieci icone non ballano.
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
        // ⚠️⚠️ **IL TOCCO LUNGO SU 'COPIA' DUPLICA DOVE SEI, dalla 0.79** (richiesta
        // dell'utente): copiare chiede dove, duplicare no.
        // ⚠️ **Nel cestino no**: un duplicato là dentro nascerebbe senza riga d'archivio,
        // quindi non si potrebbe ripristinare, e sarebbe un file che il cestino non sa da
        // dove viene.
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
            // ⚠️ La lista si prende ADESSO: la condivisione gira in una coroutine, e
            // leggere `chosen` da dentro leggerebbe una selezione che nel frattempo può
            // essere cambiata.
            val list = chosen.toList()
            scope.launch { ImageActions.shareMany(context, list) }
        },
        /*
         * ⚠️⚠️ **IL TOCCO LUNGO SALTA IL CESTINO** (richiesta dell'utente, 2026-08-31), ed
         * è la sola scorciatoia irreversibile dell'app: per questo la conferma resta. Con
         * il cestino spento il tocco breve fa già la stessa cosa, quindi là la scorciatoia
         * non aggiunge niente e non si mette.
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
            // ⚠️ Definitiva nel cestino **o** col cestino spento: con `forGood` viaggia la
            // conferma.
            job = FileJob.Delete(chosen.toList(), forGood = bin || !binOn)
        },
        /*
         * ⚠️⚠️ **'Lista' LO DICE, dalla 1.06, e la nota di prima diceva il contrario**
         * (riscontro dell'utente sul collaudo: *serve una notifica toast 'Lista file copiata
         * negli appunti'*). Il ragionamento vecchio era che su Android 13 e oltre è il
         * **sistema** ad annunciare ogni copia negli appunti, quindi un nostro avviso
         * sarebbe la stessa cosa detta due volte: l'errore era prendere quella conferma di
         * sistema per una risposta alla domanda che si fa qui. Quella dice 'qualcosa è
         * finito negli appunti' e mostra l'inizio del testo; qui la domanda è **quanti nomi
         * sono partiti**, e la risposta non c'è in nessuno dei due posti.
         * ⚠️ **La frase è quella dettata dall'utente**, senza il conto dei nomi che sarebbe
         * stato facile aggiungere: quello che serve sapere è che la lista è partita, e un
         * numero in più su un avviso che dura due secondi è una cosa da leggere invece che
         * da vedere.
         */
        PadAction(PadKey.LIST, Icons.AutoMirrored.Outlined.FormatListBulleted, R.string.pick_list) {
            val list = chosen.toList()
            scope.launch {
                // ⚠️ Il percorso si chiede per UNA sola, non per tutte: gli elementi scelti
                // stanno nella stessa cartella (la griglia è una cartella), quindi una
                // interrogazione basta e le altre sarebbero la stessa risposta N volte.
                val head = if (listPath) factsOf(context, list.take(1)).one?.folder else null
                ImageActions.copyNames(context, list, head)
                Notices.say(res.getString(R.string.pick_list_done))
            }
        },
        // ⚠️ Il tocco lungo su 'Tutti' fa il contrario, come chiesto: le due stanno
        // accanto, e chi sbaglia mira ha la correzione sotto lo stesso dito.
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
        // ⚠️ L'inversione lavora sull'elenco che si ha DAVANTI, non su tutta la cartella:
        // con una ricerca in corso o un filtro acceso, `items` è già quello filtrato, ed è
        // l'unica lettura che non sorprende.
        PadAction(PadKey.INVERT, Glyphs.PickInvert, R.string.pick_invert) {
            chosen = items.orEmpty().toSet() - chosen
        })
        // ⚠️ L'ordine è quello scelto dall'utente, e di fabbrica quello dettato per la
        // `1.54`: vedi `PICK_KEYS`. È un ordine SUO e non quello del menu del tocco lungo,
        // perché i due riquadri portano insiemi diversi in ordini diversi.
        .inOrder(LocalPadLook.current.pick)

    // Le due misure dello scorrimento ai bordi, in pixel: servono dentro un effetto, che
    // non ha una densità sotto mano.
    val density = LocalDensity.current
    val edgePx = with(density) { EDGE_BAND.toPx() }
    val speedPx = with(density) { EDGE_SPEED.toPx() }

    /*
     * ⚠️⚠️ **QUALE FOTO È GIÀ STATA SERVITA, E DALLA `1.91` NON È PIÙ UN SÌ O NO.** Fino alla
     * `1.90` qui viveva una bandierina: il salto si faceva una volta per visita, e a rimetterla
     * a zero ci pensava il **cambio di schermata**, che portava via il composable e con lui la
     * bandierina. Adesso lo scorrimento di una schermata sopravvive (il `SaveableStateProvider`
     * di `AivApp`), quindi anche la bandierina tornerebbe indietro a `true` e il salto non si
     * farebbe **mai** più.
     * ⚠️ **Ricordare l'indice invece del sì o no risolve tutti e due i casi con un dato solo**:
     * alla rotazione l'indice è lo stesso e non si salta, tornando dal visualizzatore su
     * un'altra foto è diverso e si salta.
     * ⚠️ **Meno uno e non `null`**, perché `rememberSaveable` di un `Int?` costringerebbe a
     * scrivere un `Saver`: nessuna posizione vale meno uno.
     */
    var servito by rememberSaveable { mutableStateOf(-1) }

    /*
     * Tornando dal visualizzatore la griglia si porta SULLA foto che si stava guardando, se
     * quella foto non è già in vista: dopo dieci strisciate, ritrovarsi in cima è perdere il
     * posto.
     *
     * ⚠️⚠️ **DALLA `1.91` QUESTO È IL SECONDO PASSO E NON PIÙ IL PRIMO**: la griglia riparte da
     * dov'era per conto suo, quindi qui si corregge soltanto il caso in cui **nel visualizzatore
     * si è sfogliato** fino a un'altra immagine. Sono la stessa richiesta letta fino in fondo:
     * *voglio ritrovarmi nello stesso punto dove mi trovavo prima del tocco sull'elemento*, e
     * quel punto non c'è più se intanto si è arrivati a un'immagine che di là non si vedeva.
     * ⚠️⚠️ **SI ASPETTA LA PRIMA MISURA PRIMA DI DECIDERE**, e senza quell'attesa la
     * griglia si muoverebbe SEMPRE: al primo giro di composizione nessun riquadro è
     * ancora stato disposto, quindi 'non è in vista' sarebbe vero anche per una foto
     * che sta benissimo nella prima schermata, e la si vedrebbe saltare in cima per
     * niente. L'utente ha chiesto lo scorrimento **solo** se la foto è fuori dalla
     * vista iniziale.
     * ⚠️ Ci si porta l'intero riquadro dentro lo schermo, non un pezzo: una miniatura
     * mezza tagliata dal bordo è 'in vista' per il codice e non per chi guarda.
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
     * ⚠️⚠️ **SI CHIEDE ALLA GRIGLIA, non si ricalcola**: le colonne le conta [spread] e la
     * larghezza di una cella la ricava Compose dividendo lo spazio, e rifare quel conto qui
     * vorrebbe dire una seconda formula da tenere d'accordo con lui, che sbaglierebbe in
     * silenzio il giorno che l'arrotondamento cambia. Qui il numero è quello **misurato**.
     * ⚠️ **`derivedStateOf` e non una lettura nuda**: `layoutInfo` cambia a ogni fotogramma
     * di scorrimento, la larghezza di una cella no, e senza il filtro ogni miniatura si
     * ricomporrebbe a ogni pixel scorso.
     * ⚠️ Serve **solo** al nome sotto la miniatura, che va accorciato a misura: senza quel
     * numero `fitName` non saprebbe rispetto a cosa accorciare.
     */
    val cellPx by remember(state) {
        derivedStateOf { state.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.width ?: 0 }
    }

    /**
     * Il giro che fanno tutte e quattro le operazioni: si parte, si dice com'è andata, si
     * rilegge la cartella.
     *
     * ⚠️⚠️ **LA SELEZIONE SI SVUOTA SUBITO, prima che il lavoro finisca**: è partito, e
     * lasciare le spunte accese inviterebbe a toccare la stessa voce una seconda volta
     * mentre la prima è ancora in corso. Chi chiama deve quindi essersi già preso la sua
     * lista, ed è il motivo per cui [work] la riceve dall'esterno invece di leggerla qui.
     * ⚠️ **Il testo dell'esito lo compone `outcomeText`**, condiviso col visualizzatore.
     */
    val perform: (FileKind, suspend () -> FileTree.Outcome) -> Unit = { kind, work ->
        chosen = emptySet()
        scope.launch {
            val out = work()
            // ⚠️ **Il cestino tace, e chi decide è [FileKind.speaks]**: la sua notifica
            // dice la stessa cosa e in più offre di disfare, e due messaggi in fondo
            // allo schermo si coprirebbero a vicenda.
            if (kind.speaks(out)) {
                Notices.say(outcomeText(res, out, kind.done), NOTICE_LONG_MS)
            }
            worked = true
            onChanged()
        }
    }

    /*
     * ⚠️⚠️ **SE LA CARTELLA SI SVUOTA OPERANDO, SI RISALE** (richiesta dell'utente,
     * 2026-08-31: *se sposto in una nuova cartella TUTTE le immagini di una cartella, la
     * mia vista si deve ri-spostare sul livello superiore*): restare in una cartella vuota
     * appena svuotata da noi è una schermata che non ha più niente da dire, e il tasto
     * Indietro sarebbe l'unica cosa da fare.
     * ⚠️ **Solo dopo un'operazione NOSTRA**, e la bandierina esiste per questo: una cartella
     * che era già vuota all'ingresso si apre e si guarda (ci si può arrivare da un
     * collegamento o da una ricerca), e buttare fuori chi ci entra sarebbe una schermata
     * che si rifiuta di esistere.
     * ⚠️ **Il cestino NO**, ed è la sua natura: svuotarlo è la cosa che si va a fare là
     * dentro, e ritrovarsi fuori dopo averlo fatto vorrebbe dire non vedere mai il
     * risultato. Là il vuoto ha già la sua frase ('Il cestino è vuoto').
     * ⚠️ **La ricerca nemmeno**: là il vuoto vuol dire 'nessun risultato', che è una
     * risposta e non una cartella finita.
     */
    LaunchedEffect(worked, items) {
        if (worked && !bin && query == null && items?.isEmpty() == true) onBack()
    }

    /*
     * ⚠️⚠️ **LA RADICE È UN `Box` E NON LA `Column`, e serve SOLO al velo** (difetto
     * segnalato dall'utente il 2026-08-31: *l'intero schermo deve offuscarsi, non solo un
     * riquadro interno*). Il velo dell'onboarding deve coprire **tutto**, testata e margini
     * di sistema compresi, e da dentro la `Column` non poteva: là il rientro di sistema e i
     * margini della schermata sono già stati applicati, quindi qualunque cosa nasca là
     * dentro comincia sotto la testata.
     * ⚠️ **Il `Box` non ha margini propri**, ed è quello che gli permette di arrivare fino
     * al bordo dello schermo: i margini restano sulla `Column`, cioè sul contenuto. Chi ne
     * spostasse uno sul `Box` rimetterebbe il difetto.
     * ⚠️⚠️ **DALLA `1.76` MISURA, e serve all'intestazione**: quella fascia si prende
     * [HEADER_SHARE] dell'altezza, quindi qualcuno deve saperla. ⚠️ **I rientri di sistema si
     * sottraggono a mano** e non si spostano qui col resto: la frazione dev'essere quella
     * dell'area utile, come nella schermata iniziale, ma il velo dell'onboarding ha bisogno che
     * questo riquadro arrivi al vetro.
     */
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    // ⚠️ La larghezza si cattura qui e non si legge più giù: dentro la colonna il receiver di
    // [BoxWithConstraints] è coperto da quello della colonna, e `maxWidth` non si raggiunge.
    val larghezza = maxWidth

    /*
     * ⚠️⚠️ **IL INTESTAZIONE DI UNA CARTELLA, dalla `1.76`, ed è una richiesta sua del giro
     * della `1.67`**: *l'icona va posizionata esattamente come quella oggi presente sulla
     * schermata home, ma semitrasparente (~50%), e sotto, al posto del nome dell'app, il titolo
     * della cartella scritto un po' più piccolo per lasciare spazio anche a nomi lunghi. Poi,
     * più in basso, con un posizionamento analogo alla home, inizia la griglia delle immagini*.
     * Il meccanismo, la fascia e la sfumatura vivono in `Front.kt`, condivisi con la schermata
     * iniziale: quello che sta qui è dove comincia e quando è chiuso.
     * ⚠️⚠️ **NÉ NEL CESTINO NÉ NELLA RICERCA, e nessuno dei due è una dimenticanza.** Nel
     * cestino il FAB c'è sempre, quindi la sfumatura che lo tiene su un fondo neutro non
     * potrebbe andarsene scorrendo, che è metà di quello che ha chiesto; e nella ricerca la
     * testata non porta un titolo ma un campo di testo, cioè non c'è niente che possa traslare
     * là dentro.
     */
    val density = LocalDensity.current
    val bordi = WindowInsets.safeDrawing
    val front = !bin && query == null
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
     * ⚠️ La chiave è la misura, come nella schermata iniziale: ruotando il telefono l'altezza
     * cambia, e un valore di chiusura vecchio non vorrebbe più dire niente.
     */
    var shut by remember(headerPx) { mutableFloatStateOf(0f) }
    val aperto = remember(headerPx) { { frontOpen(headerPx, shut) } }
    val paging = remember(headerPx) {
        frontScroll(quanto = headerPx, chiuso = { shut }, chiudi = { shut = it })
    }

    /*
     * ⚠️⚠️ **LA SELEZIONE NON CHIUDE IL INTESTAZIONE, E FINO ALLA `1.77` LO CHIUDEVA: È UN
     * ROVESCIAMENTO SUO, con una ragione che nessuna delle due parti aveva previsto** (riscontro
     * del giro della `1.77`, voce `front-misure` non approvata): *appena si tocca a lungo per
     * iniziare a selezionare, lo spostamento delle miniature in alto fa già selezionare più
     * elementi a causa dello spostamento repentino mentre si tiene premuto*. Cioè la chiusura
     * automatica non era solo spazio guadagnato: era una griglia che scorreva **sotto un dito
     * appoggiato**, e il gesto da/a della selezione prendeva tutte le miniature che le passavano
     * sotto.
     * ⚠️⚠️ **E LA RAGIONE PER CUI ESISTEVA È DECADUTA CON IL CONTATORE**: si chiudeva perché in
     * selezione la testata diventava il conto dei selezionati, e con la fascia aperta quel conto
     * non aveva posto. Dalla `1.78` il conto vive **sotto il titolo** (sua specifica, vedi la
     * testata qui sotto), quindi c'è in tutti e due i posti e non serve più liberare la testata.
     * Le sue parole: *visto che il contatore deve stare inizialmente sotto il titolo della
     * cartella, il passaggio a tutto schermo deve avvenire durante la selezione esattamente come
     * senza selezione. È obbligatorio*.
     * ⚠️ **Quindi la fascia si chiude in un modo solo, scorrendo**, e la costante che dava la
     * durata di quella chiusura animata (`FRONT_SHUT_MS`) è uscita da `Front.kt`: serviva a
     * questo caso e a nessun altro, e una costante senza chiamanti è codice morto.
     */

    /*
     * ⚠️⚠️ **CON LA GRIGLIA SCORSA IL INTESTAZIONE NON PUÒ STARE APERTO, e questo copre il
     * ritorno dal visualizzatore** (sua specifica: *resta come era, chiuso se la griglia non è in
     * cima*). Con le dita la cosa è già vera per costruzione, perché [frontScroll] spende il
     * trascinamento qui prima che l'elenco si muova; quello che sfugge è lo scorrimento
     * **programmato**, cioè il salto all'immagine da cui si è tornati, che non passa dallo
     * scorrimento annidato. Senza questa riga si tornava con la fascia aperta sopra una griglia
     * già a metà.
     * ⚠️ **Snap e non animazione**: si è nell'istante in cui la schermata arriva, quindi non c'è
     * niente da raccontare e un'animazione sarebbe un movimento che nessuno ha chiesto.
     */
    LaunchedEffect(headerPx) {
        snapshotFlow {
            state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0
        }.collect { scorsa -> if (scorsa) shut = headerPx }
    }

    /**
     * Il conto che compare **sotto il nome**, in testata e nella fascia: i selezionati durante
     * una selezione, gli elementi della cartella fuori da lei.
     *
     * ⚠️ **Si calcola una volta e si legge in due posti**, perché le due copie si dissolvono
     * l'una nell'altra: scritto due volte, il giorno che una delle due cambia stringa il nome e
     * il numero direbbero due cose diverse a metà corsa. Il perché di ogni pezzo è sulla riga
     * della testata.
     */
    val conto = if (picking) {
        pluralStringResource(R.plurals.pick_count, chosen.size, chosen.size)
    } else {
        items?.let { pluralStringResource(R.plurals.items_count, it.size, it.size) }
    }

    /*
     * ⚠️⚠️ **I GESTI DELL'INTESTAZIONE, DALLA `1.85`**: il nome si copia toccandolo e il tocco
     * lungo ci aggiunge il percorso; l'icona apre il gestore file di sistema, e il tocco lungo
     * il colore di quella cartella. Sono quattro richieste sue del giro della `1.83`.
     * ⚠️ **Le stringhe si risolvono QUI e non dentro i gesti**: `stringResource` è una funzione
     * di composizione, e in una lambda che parte da un tocco non si può chiamare.
     */
    val nomeCopiato = stringResource(R.string.front_name_copied)
    val rinominaEtichetta = stringResource(R.string.pick_rename)
    val tintaEtichetta = stringResource(R.string.front_tint)
    val copertinaEtichetta = stringResource(R.string.folder_cover)

    /*
     * ⚠️ **Il tema dell'app, letto QUI perché serve in fase di disegno**: l'inchiostro dell'icona
     * dell'intestazione lo sceglie una `graphicsLayer`, che gira fuori dalla composizione e un
     * `CompositionLocal` non lo può leggere. È il tema di AIV e non quello di sistema, che è la
     * distinzione di `AIV/CLAUDE.md`, § '🌗 Il tema scelto DENTRO l'app non è quello di sistema'.
     */
    val chiaro = LocalAivLight.current
    /*
     * ⚠️ **Il gesto si legge VIVO e non catturato**: il riconoscitore dei tocchi nasce una volta
     * sola (`pointerInput(Unit)`, o si riavvierebbe a ogni ricomposizione), quindi la lambda che
     * cattura sarebbe quella del primo giro. Con questo, il tocco chiama sempre quella di adesso.
     */
    val scegliCopertina by rememberUpdatedState(onCoverPick)
    val copiaNome = {
        ImageActions.copyName(context, title)
        Notices.say(nomeCopiato)
    }
    /*
     * ⚠️⚠️ **IL TOCCO LUNGO SUL NOME RINOMINA LA CARTELLA, DALLA `1.95`, E FINO ALLA `1.94`
     * COPIAVA IL PERCORSO** (sua richiesta, 2026-09-08: *non più 'copia percorso' -> passa a
     * 'Rinomina': per rinominare facilmente la cartella*). Con lui escono di scena le due stringhe
     * di allora, `front_copy_path` e `front_path_copied`, che non avevano più nessun chiamante.
     * ⚠️ **Il tocco breve non cambia**: copia il nome, come dalla `1.85`.
     */
    val viaLungo = { if (onFolderRename != null) rinomina = true }

    /*
     * ⚠️⚠️ **IL RIENTRO DI SOTTO NON STA PIÙ QUI, DALLA `1.90`** (sua richiesta: *non si può
     * estendere la vista della griglia fino al margine inferiore dello schermo? Quella (la linea
     * della navigazione gestuale) può restare in sovrapposizione*). Un `safeDrawingPadding()`
     * sul contenitore toglie lo spazio **prima** che la griglia cominci a disegnare, quindi là
     * sotto non ci può arrivare niente; passandolo al `contentPadding` della lista le miniature
     * scorrono sotto la barra gestuale e l'ultima riga resta raggiungibile lo stesso.
     * ⚠️ **Anche il margine verticale della schermata si scompone**, per la stessa ragione: in
     * cima resta un rientro, in fondo diventa spazio di scorrimento. Lasciandolo qui la griglia
     * si sarebbe fermata dodici punti sopra il vetro, cioè avrebbe risolto il problema a metà.
     * ⚠️ **Il FAB non si muove**: vive in una finestra sua e i rientri se li mette da sé, che è
     * il motivo per cui questa modifica non lo tocca.
     */
    /*
     * ⚠️⚠️ **IL GLIFO DEL FAB DIVENTA IL CHEVRON SCORRENDO, DALLA `2.07`**: il gesto lo raccoglie
     * il contenitore e il disegno lo fa il FAB, quindi lo stato vive qui, dove li si vede tutti
     * e due. Il perché del meccanismo, e i cinque numeri del mockup, vivono in `Jump.kt`.
     * ⚠️ **La fascia chiusa entra nella stima del 'su'**: chiudendola la griglia non si muove di
     * un pixel, quindi da sola direbbe di essere già in cima proprio nel caso in cui il salto ha
     * qualcosa da fare.
     */
    val arm = rememberJumpArm(
        state = state,
        up = { state.jumpUpPixels() + shut },
        down = { state.jumpDownPixels() }
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
            )
            /*
             * ⚠️⚠️ **IL GESTO SI GUARDA PRIMA DI `paging`, E L'ORDINE È MISURATO**: in una catena
             * di modificatori il `nestedScroll` scritto **per primo** è quello che riceve per
             * primo il delta della lista, e `frontScroll` ne consuma la parte con cui chiude
             * l'intestazione. Scritto dopo, al motore del glifo arrivava **zero** finché la
             * fascia aveva spazio da chiudere: misurato sul banco, un colpo solo con somma 0.
             */
            .nestedScroll(arm.watch)
            .nestedScroll(paging)
            .padding(horizontal = GRID_PAD_X)
            .padding(top = GRID_PAD_Y)
    ) {
        /*
         * ⚠️⚠️ **LA TESTATA E LA FASCIA VIVONO IN UN BLOCCO SOLO, DALLA `1.85`, E LA TINTA SI
         * DIPINGE DIETRO DI LUI**: è la correzione del difetto che ha fatto bocciare la `1.83`
         * (voce `front-dieci`: *il nome della cartella e gli elementi non passano più in testa
         * allo scorrimento (lo spazio rimane vuoto)*). Fino alla `1.84` la tinta viveva su un
         * nodo che la colonna disegnava **dopo** la testata, quindi il rettangolo che sconfina
         * verso l'alto le finiva sopra e si mangiava il titolo che stava comparendo. Il perché
         * per esteso, e la trappola che resta per chi lo rifà, vivono su [Modifier.frontWash].
         * ⚠️ **Il blocco si accorcia da sé**: quando la fascia si chiude qui dentro resta la sola
         * testata, quindi l'area della tinta segue senza che nessuno la animi.
         * ⚠️ **Sconfina di [GRID_PAD_Y] verso l'alto** e non più dell'altezza della testata: da
         * qui al bordo dell'area sicura c'è solo il rientro verticale della schermata, che è un
         * numero noto. Con lui è sparita anche la misura della testata, che era un
         * `onGloballyPositioned` con la sua ricomposizione.
         */
        Column(
            modifier = if (front && frontWash) {
                Modifier.frontWash(
                    tint = frontTintOf(frontTint) ?: MaterialTheme.colorScheme.primary,
                    air = GRID_PAD_X,
                    up = GRID_PAD_Y,
                    // ⚠️ **La fascia in cima arriva fin sotto la barra di sistema, dalla
                    // `1.87`**, ed è la sua richiesta con la schermata alla mano: il numero è
                    // quello che questa colonna le ha appena lasciato con `safeDrawingPadding`,
                    // quindi il colore chiude esattamente il buco che quel rientro apre.
                    bar = with(density) { bordi.getTop(this).toDp() },
                    ink = aperto
                )
            } else {
                Modifier
            }
        ) {
        // ⚠️⚠️ **LA BARRA DELLA SELEZIONE PRENDE IL POSTO DEL TITOLO invece di aggiungersi
        // sopra**: due barre insieme mangerebbero un quarto di schermo alle miniature, che
        // sono la cosa per cui si è entrati. Ed è anche il modo di dire che si è in un
        // modo diverso, senza scriverlo.
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
            Column(modifier = Modifier.weight(1f)) {
                if (query != null && !picking) {
                    /*
                     * ⚠️⚠️ **IL CAMPO PRENDE IL FUOCO DA SÉ, e senza questo la ricerca si
                     * apre su una schermata che non fa niente**: chi tocca 'Cerca' ha già
                     * in mente la parola, e trovarsi davanti un campo spento con la
                     * tastiera chiusa vuol dire un tocco in più prima di poter scrivere.
                     * ⚠️ Una volta sola per visita: rimettere il fuoco a ogni
                     * ricomposizione riaprirebbe la tastiera dopo che la si è chiusa per
                     * guardare i risultati, che è precisamente quando la si vuole via.
                     */
                    val focus = remember { FocusRequester() }
                    LaunchedEffect(Unit) { focus.requestFocus() }
                    TextField(
                        value = query,
                        onValueChange = onQuery,
                        placeholder = {
                            Text(searchIn ?: stringResource(R.string.search_hint))
                        },
                        singleLine = true,
                        // ⚠️ Senza contorno e senza fondo: qui sta al posto di un titolo,
                        // e un campo squadrato in testata sembrerebbe un modulo da
                        // compilare invece della riga che dice dove si è.
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
                     * ⚠️⚠️ **IL TITOLO SI DISSOLVE COL INTESTAZIONE, dalla `1.76`**: mentre la
                     * fascia è aperta il nome della cartella si legge là dentro, grande e al
                     * centro, e chiudendola sale verso qui e lascia il posto a questo, che è
                     * *come già appare adesso in posizione finale* (parole sue). ⚠️ **La
                     * traslazione non è scritta da nessuna parte**: la fa la parallasse della
                     * fascia, che alza il proprio contenuto mentre lo spazio si stringe (vedi
                     * [FrontBand]). Aggiungerne una seconda vorrebbe dire due movimenti sullo
                     * stesso oggetto.
                     * ⚠️ **Le due opacità sono complementari e non due curve**: sommano uno a
                     * ogni istante, quindi non esiste un punto della corsa in cui il nome della
                     * cartella si legga meno che agli estremi.
                     * ⚠️ **Senza intestazione l'opacità è piena**, perché lì `aperto` vale zero:
                     * il titolo del cestino e quello della ricerca non hanno niente da cui
                     * arrivare.
                     * ⚠️⚠️ **E IL TITOLO RESTA IL NOME DELLA CARTELLA ANCHE IN SELEZIONE, dalla
                     * `1.78`**: fino alla `1.77` diventava il conto dei selezionati, ed era la
                     * ragione per cui la fascia doveva chiudersi (vedi la nota là sopra). Col
                     * conto spostato sotto, il titolo dice sempre **dove si è**, che è la sola
                     * cosa che una testata deve dire.
                     */
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        /*
                         * ⚠️⚠️ **L'ELLISSI IN CODA, DALLA `1.81`: fino alla `1.80` un nome
                         * lungo si tagliava a metà glifo** (censimento della UI del
                         * 2026-09-05). ⚠️ **Non è il caso dei nomi di FILE**, dove l'ellissi
                         * in coda è vietata perché mangerebbe l'estensione e si usa quella in
                         * mezzo (`Names.kt`, `RenameDialog.kt`, `FileOps.kt`): questo è il
                         * nome di una raccolta del `MediaStore`, che estensione non ha.
                         * ⚠️ **E la copia nella fascia la portava già**, quindi le due copie
                         * dello stesso nome si troncavano in due modi diversi.
                         */
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.heading().graphicsLayer { alpha = 1f - aperto() }
                    )
                    /*
                     * ⚠️⚠️ **IL CONTO DEGLI ELEMENTI STA SOTTO IL TITOLO, ED È LA SUA SPECIFICA
                     * ALLA LETTERA** (riscontro del giro della `1.77`): *il numero di elementi
                     * (non immagini) totali / selezionati dev'essere indicato sotto il titolo,
                     * centrato, con un carattere leggermente più piccolo e meno opaco*. Il corpo
                     * più piccolo è `bodySmall` e il meno opaco è `onSurfaceVariant`, cioè i due
                     * che questa riga aveva già: quello che cambia è **che cosa conta** e che
                     * c'è anche in selezione.
                     * ⚠️⚠️ **DICE 'ELEMENTI' E NON 'IMMAGINI', punto (b) del suo campo libero**:
                     * *in alto sullo schermo, all'interno di una cartella c'è il contatore del
                     * numero di 'immagini', ma non va più bene da quando ci sono anche i video*.
                     * Qui si contava `items.size`, cioè tutto, con la stringa che dice
                     * *immagini*: era falso da quando i video sono entrati nella griglia.
                     * ⚠️⚠️ **E `folders_count` NON SI È RISCRITTA, perché ha un secondo
                     * chiamante con un altro significato**: nella schermata iniziale conta le
                     * **sole immagini** di una cartella, accanto a `folders_clips` che conta i
                     * video, e là *immagini* è la parola giusta. Cambiarla avrebbe corretto qui
                     * e mentito là, che è la trappola di ogni stringa riusata per somiglianza.
                     * ⚠️⚠️ **E LA DIVERGENZA PER CUI LE DUE CHIAVI ESISTONO È ARRIVATA AL GIRO
                     * DOPO**: dalla `1.80` `pick_count` dice *N elementi selezionati* (sua nota
                     * sulla voce `front-selezione`: *quando elenchi solo il numero di elementi
                     * s'intende il totale. Invece se c'è una selezione scrivi 'elementi
                     * selezionati'*), mentre `items_count` resta il totale. Fino alla `1.79` le
                     * due dicevano lo stesso testo, e riusarne una avrebbe cambiato l'altra in
                     * silenzio: è esattamente quello che sarebbe successo qui.
                     * ⚠️ **In selezione il conto è quello dei selezionati** (*totali /
                     * selezionati*, parole sue), e il posto non cambia: il numero da guardare è
                     * sempre sotto il nome.
                     */
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
            }
            // ⚠️ Il peso sta FUORI dalla colonna del conto, non sotto: la richiesta dice
            // *in linea ma a destra, allineato al bordo destro*, e dentro la colonna
            // seguirebbe la larghezza del testo invece del bordo della barra.
            // ⚠️ **Spento il peso, in selezione qui non va NIENTE**, e non il filtro: quello
            // sceglie che cosa mostrare nella cartella, e in mezzo a una selezione
            // cambierebbe l'elenco sotto le spunte già date.
            when {
                picking && pickWeight -> PickWeight(chosen)
                picking -> Unit
                else -> FilterKey(filter, onFilter, onSearch)
            }

            /*
             * ⚠️⚠️ **QUI NON C'È PIÙ NIENTE, e la ragione per cui c'era è stata SOSTITUITA
             * invece che dimenticata.** Fino alla `0.72` accanto al conto stava un FAB
             * 'Tutte', messo lì perché su una cartella da trecento foto il gesto
             * alternativo è trecento tocchi. Quel bisogno adesso lo copre il **tocco lungo
             * sul FAB**, che fa la stessa cosa, si annuncia a TalkBack e ha
             * un onboarding che lo insegna una volta.
             * ⚠️ Togliendolo si guadagna la coerenza, che è la ragione dell'utente
             * (2026-08-31): *è un unicum e nessun'altra azione fa apparire qualcosa lì*.
             * In questa barra non compariva nient'altro, mai, in nessun altro modo.
             * ⚠️ Chi volesse rimetterlo tenga presente che ne servirebbe **anche** uno per
             * 'nessuna', o la barra torna a essere un posto dove una sola azione su due ha
             * un FAB.
             */
        }
        Spacer(Modifier.height(8.dp))

        /*
         * ⚠️⚠️ **LA FASCIA DEL INTESTAZIONE, dalla `1.76`**: l'icona della cartella a mezza
         * tinta e sotto il nome, *con un posizionamento analogo alla home*. Il meccanismo di
         * misura, il ritaglio e la parallasse stanno in [FrontBand]; qui c'è solo quello che si
         * vede dentro.
         * ⚠️ **L'icona si stringe se la fascia è bassa**, con lo stesso conto della schermata
         * iniziale: in orizzontale non resta niente, e senza questa stretta la tela verrebbe
         * tagliata sopra e sotto invece di stare dentro.
         * ⚠️ **L'icona non parla** (`contentDescription` nullo): a dire dove si è c'è il nome
         * della cartella, e un lettore di schermo che annuncia 'cartella' prima di leggerlo
         * darebbe due voci per una cosa sola.
         * ⚠️⚠️ **IL NOME È SCRITTO DUE VOLTE NELL'ALBERO SEMANTICO, e va saputo**: qui e in
         * testata. Nessuna delle due copie si può togliere, perché l'una si dissolve nell'altra
         * e un titolo che compare a metà corsa sarebbe un salto; a non farne due voci ci pensa
         * l'opacità, perché un nodo trasparente resta comunque leggibile da TalkBack. ⚠️ Chi
         * volesse chiudere anche quel buco lo faccia con `alpha` **semantico**, non togliendo
         * uno dei due testi.
         */
        if (front) {
            FrontBand(fullPx = headerPx, shut = { shut }) { quanto ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    /*
                     * ⚠️⚠️ **L'ICONA SI RIMPICCIOLISCE PRIMA DI SPARIRE, dalla `1.78`, ED È LA
                     * SUA SPECIFICA** (riscontro del giro della `1.77`): *man mano che si
                     * scorre, deve prima rimpicciolirsi e adattarsi ad ogni fotogramma allo
                     * spazio disponibile in verticale, poi sparire con una dissolvenza come fa
                     * adesso*. Fino alla `1.77` la misura era **fissa** (metà della fascia
                     * piena) e l'opacità andava col quadrato dell'apertura: il disegno usciva di
                     * scena sbiadendo e facendosi tagliare, senza mai stringersi.
                     * ⚠️ **Le due metà stanno in `Front.kt`**, [frontIconMeasure] per la misura
                     * e [frontIconInk] per la dissolvenza, perché sono un movimento solo in due
                     * fasi e i loro numeri vivono accanto agli altri della fascia.
                     * ⚠️ **La misura vince sulla scala**, e la differenza è che il titolo sotto
                     * prende lo spazio che l'icona cede: il perché per esteso è sul
                     * modificatore.
                     * ⚠️⚠️ **DALLA `1.80` LE DUE FASI COMINCIANO INSIEME** (riscontro del giro
                     * della `1.79`: *deve iniziare la sua dissolvenza appena inizia a ridursi di
                     * dimensione*): la soglia non è più un numero scritto a mano ma la risolve
                     * [frontIconFade] dagli stessi ingressi della misura. ⚠️ **`toPx()` si può
                     * chiamare qui** perché `GraphicsLayerScope` è una `Density`, e leggerla nel
                     * disegno costa niente: la soglia è la stessa a ogni fotogramma.
                     */
                    /*
                     * ⚠️⚠️ **CON LA TINTA ACCESA L'ICONA VA IN NEGATIVO, DALLA `1.83`** (variante
                     * 10: *l'icona centrata in negativo all'80% invece del 20%*). Cambiano due
                     * cose insieme e vanno insieme: il **colore**, che diventa quello della
                     * superficie perché sopra la tinta l'inchiostro del contenuto sparirebbe, e
                     * l'**inchiostro**, che passa da un accenno a una sagoma.
                     * ⚠️ **La dissolvenza resta la stessa**: quello che cambia è il valore da cui
                     * parte, non la curva, quindi la coreografia dello scorrimento non si tocca.
                     */
                    /*
                     * ⚠️⚠️ **IL TOCCO SULL'ICONA SCEGLIE LA COPERTINA, DALLA `1.94`, ED È LA
                     * PROPOSTA CHE ASPETTAVA** (risposta a `d-copertina-come` del giro della
                     * `1.92`: *solo con il tocco singolo sull'icona dell'intestazione di una
                     * cartella*). Dalla `1.86` quel gesto era spento su sua istruzione (*per il
                     * momento disattiva questo tocco, e proponimi un'azione alternativa realmente
                     * utile. In assenza di funzionalità utili, per il momento resta senza*): la
                     * riga qui sotto è quella funzione utile arrivata. Il tocco lungo, che sceglie
                     * il colore, resta com'era.
                     * ⚠️⚠️ **E CON IL GESTO ESCE IL CODICE CHE LO SERVIVA**, cioè
                     * `Folder.openInFiles` e le sue due stringhe: un ramo senza chiamanti tenuto
                     * in caldo per una funzione che forse torna è codice morto, e la storia git lo
                     * riporta indietro in un comando il giorno che lui sceglie una delle proposte.
                     * ⚠️ **La sua domanda tecnica ha una risposta, e vive nel documento del giro**
                     * (*non si potrebbe far scegliere all'utente con quale app aprire la cartella,
                     * se apparentemente nessuna app è disponibile?*): un selettore di app mostra
                     * quelle che rispondono all'intento, quindi dove non risponde nessuno non
                     * mostra niente. Quello che risponde sempre è un'altra strada, ed è una delle
                     * proposte.
                     * ⚠️⚠️ **L'ICONA RESTA PARLANTE, e adesso dice il gesto che le è rimasto**:
                     * con la descrizione a `null` sarebbe un disegno muto con un'azione sopra,
                     * cioè una funzione per chi la sa e non per chi la cerca.
                     * ⚠️ **Chi ascolta viene PRIMA di chi misura** (regola in `AIV/CLAUDE.md`,
                     * § '👆 Che cosa fa il tocco FUORI da una finestra'): qui i due riquadri
                     * coincidono, ma l'ordine è quello per cui un nodo di tocco non finisce mai
                     * dietro un confine di layout.
                     */
                    Icon(
                        imageVector = Glyphs.FolderAiv,
                        /*
                         * ⚠️ **La descrizione è quella del TOCCO, e il tocco lungo se la dichiara
                         * a parte**: chi ascolta sente prima che cosa fa il gesto normale, che è
                         * quello che farà.
                         */
                        contentDescription = if (scegliCopertina != null) {
                            copertinaEtichetta
                        } else {
                            tintaEtichetta
                        },
                        /*
                         * ⚠️⚠️ **NEL TEMA SCURO L'ICONA È BIANCA E SOVRAPPOSTA, DALLA `1.95`, E
                         * NON PIÙ IN NEGATIVO** (sua richiesta: *l'icona dell'intestazione deve
                         * ritornare positiva (sovrapposta) per il tema scuro: bianco, opacità
                         * 40%*). Il negativo è la sagoma della **superficie**, che nel tema chiaro
                         * è quasi bianca e stacca sulla tinta, mentre nel tema scuro è quasi nera:
                         * là 'in negativo' voleva dire uno scuro sopra un altro scuro.
                         * ⚠️ **Il tema è quello dell'APP e non quello di sistema** ([LocalAivLight]),
                         * per la stessa ragione dell'icona in testata: l'app ha una voce sua in
                         * 'Aspetto', e una risorsa letta dalla configurazione direbbe il contrario.
                         */
                        tint = when {
                            !frontWash -> LocalContentColor.current
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
                                    onTap = { scegliCopertina?.invoke() }
                                )
                            }
                            .frontIconMeasure(
                                fullPx = headerPx,
                                shut = { shut },
                                max = HEADER_ICON
                            )
                            // ⚠️ Il riquadro serve al velo del mini onboarding, che ci cade sopra:
                            // il perché si misura invece di ricalcolarlo vive su [iconaSpot].
                            .onGloballyPositioned { iconaSpot = it.boundsInRoot() }
                            .graphicsLayer {
                                alpha = frontIconInk(
                                    aperto = quanto(),
                                    soglia = frontIconFade(headerPx, HEADER_ICON.toPx()),
                                    // ⚠️ Il pieno segue il colore scelto qui sopra: la sagoma in
                                    // negativo vuole tutto l'inchiostro, il bianco sovrapposto ne
                                    // vuole il 40%, che è il numero che ha dettato lui.
                                    pieno = when {
                                        !frontWash -> FRONT_INK
                                        chiaro -> FRONT_NEG_INK
                                        else -> FRONT_DARK_INK
                                    }
                                )
                            }
                    )
                    Spacer(Modifier.height(FRONT_GAP))
                    /*
                     * ⚠️⚠️ **UN GRADINO SU DALLA `1.77`, ED È IL SUO SECONDO NUMERO**: la
                     * `1.76` scriveva questo titolo in `titleSmall`, un gradino sotto il
                     * `titleMedium` con cui la schermata iniziale scrive il nome dell'app,
                     * perché la sua specifica diceva *un po' più piccolo per lasciare spazio
                     * anche a nomi lunghi*. Col telefono in mano l'ha voluto più grande
                     * (riscontro del giro della `1.76`: *il testo del titolo dev'essere un po'
                     * più grande*).
                     * ⚠️ **La sua ragione di allora non cade, perché non era il corpo a
                     * portarla**: lo spazio per un nome lungo lo fa [FRONT_TITLE_LINES], cioè
                     * l'andata a capo, e quella non è cambiata. Il corpo più piccolo era un
                     * secondo modo di dire la stessa cosa, e questo giro dice che di quei due
                     * ne serviva uno.
                     * ⚠️ **E resta più piccolo di quello della testata**, che è `headlineSmall`:
                     * la fascia non scrive il titolo alla misura in cui lo troverà in cima, o la
                     * traslazione non avrebbe niente da raccontare.
                     */
                    /*
                     * ⚠️⚠️ **'Titolo graziato' CAMBIA SOLO IL CARATTERE, DALLA `1.85`, ED È UNA
                     * SUA CORREZIONE** (riscontro del giro della `1.83`: *l'opzione 'Testo
                     * graziato' cambia solo il carattere, niente iconcina color accento
                     * aggiuntiva*). Nella `1.83` quel chip faceva tre cose insieme, perché
                     * traduceva la variante 7 del mockup: il carattere, un corpo più grande, e
                     * una cartella piccola color accento sopra il nome. Adesso ne fa una, e le
                     * altre due sono uscite: la cartellina con lei, e il corpo perché il titolo
                     * è più grande **sempre**.
                     * ⚠️ **Il carattere graziato è quello di SISTEMA** ([FontFamily.Serif]) e non
                     * un font portato nell'APK: un carattere in più pesa e va scelto, e qui la
                     * richiesta è la **forma** delle grazie, non una tipografia nuova.
                     * ⚠️⚠️ **UN GRADINO SU ANCORA, DALLA `1.85`, ED È IL SUO TERZO NUMERO**
                     * (*il testo dev'essere un po' più grande di default*): `titleSmall` nella
                     * `1.76`, `titleMedium` nella `1.77`, `titleLarge` adesso. ⚠️ **E resta
                     * sotto la testata**, che è `headlineSmall`: se la fascia scrivesse il nome
                     * alla misura in cui lo troverà in cima, la traslazione non avrebbe niente
                     * da raccontare.
                     *
                     * ⚠️⚠️ **I DUE GESTI SUL NOME SONO SUOI** (stesso riscontro, come *bonus*):
                     * *un tap sul nome della cartella copia il suo nome (con notifica toast); un
                     * tap lungo copia il nome della cartella con il percorso*.
                     * ⚠️ **Senza percorso il tocco lungo copia il solo nome** invece di non fare
                     * niente: la cartella di una ricerca o una appena aperta può non averlo
                     * ancora, e un gesto che a volte tace si legge come rotto.
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
                     * ⚠️⚠️ **IL CONTO STA ANCHE QUI, e la traslazione in testata viene per
                     * costruzione**: sua richiesta del giro della `1.77`, *sia il nome che il
                     * numero di elementi totali/selezionati devono traslare e adattarsi alla
                     * loro nuova posizione in testata con un'animazione fluida e moderna*. Il
                     * movimento è la parallasse che [FrontBand] fa da sempre, quindi basta che
                     * il numero viva **dentro** la fascia insieme al nome: aggiungerne uno
                     * scritto a mano darebbe due movimenti sullo stesso oggetto.
                     * ⚠️ **L'opacità è complementare a quella della testata**, come per il
                     * nome: sommano uno a ogni istante, quindi non c'è un punto della corsa in
                     * cui il numero si legga meno che agli estremi.
                     * ⚠️ **La stringa è la stessa dei due posti**, calcolata una volta sola
                     * sopra: il perché è sul suo KDoc.
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
                     * ⚠️⚠️ **LA FILA MISTA DELLA VARIANTE 10: DUE DATI E UN COMANDO** (scelta
                     * sua), e i due vestiti sono diversi apposta: il mockup lo dichiara (*un dato
                     * e un comando che si somigliano sono la trappola vera di una fila mista*).
                     * ⚠️⚠️ **I DUE TOCCHI LUNGHI SONO IL PUNTO A DEL SUO CAMPO LIBERO** (giro
                     * della `1.82`: *tap lungo sulla pastiglia dello spazio occupato → seleziona
                     * tutto; tap lungo sulla pastiglia del numero dei video → seleziona tutti i
                     * video*). Sono scorciatoie su un dato, quindi il tocco breve non fa niente:
                     * un dato che al primo tocco seleziona duecento file sarebbe una sorpresa.
                     * ⚠️ **Una pastiglia senza numero non compare**: una cartella appena aperta
                     * non è ancora pesata e una cartella di sole immagini non ha video, e in tutti
                     * e due i casi uno zero non direbbe niente a nessuno.
                     * ⚠️ **In selezione la fila resta**, perché 'Seleziona tutto' serve proprio
                     * là: è la stessa scorciatoia del riquadro, a portata di pollice.
                     */
                    /*
                     * ⚠️⚠️ **TUTTE DELLO STESSO COLORE DALLA `1.85`, ED È UNA SUA CORREZIONE**
                     * (riscontro del giro della `1.83`: *le pastiglie restano (se abilitate), ma
                     * tutte dello stesso colore neutro, senza distinzione tra info e selezione*).
                     * La `1.83` dava al comando un vestito pieno e scuro, perché il mockup
                     * dichiarava che *un dato e un comando che si somigliano sono la trappola vera
                     * di una fila mista*: col telefono in mano ha deciso il contrario, e adesso il
                     * pezzo che le disegna è **uno**.
                     * ⚠️⚠️ **E IL TERZO DATO È LA RISPOSTA A `d-front-altro`**: `immagini`,
                     * cioè *aggiungi il numero di immagini, accanto ai video, così la somma torna
                     * col conto sotto il titolo*. Viene dalla stessa query delle altre due.
                     *
                     * ⚠️⚠️ **I GESTI SONO QUATTRO COPPIE, E LI HA RIDETTATI NELLA `1.86`**
                     * (riscontro del giro della `1.85`, voce `int-chip`: *benissimo il colore; le
                     * funzionalità però devono essere le seguenti (c'ho pensato meglio)*). Ogni
                     * pastiglia adesso ha **tutti e due** i gesti, e la coppia è sempre la stessa:
                     * il tocco **aggiunge** alla selezione quello che la pastiglia nomina, il
                     * tocco lungo lo **toglie**. Fino alla `1.85` i tre dati avevano il solo tocco
                     * lungo e il comando il solo tocco, cioè quattro pastiglie con tre regole.
                     * - **Peso**: tocco tutto, tocco lungo niente.
                     * - **Immagini**: tocco le immagini, tocco lungo via le immagini.
                     * - **Video**: tocco i video, tocco lungo via i video.
                     * - **Comando**: tocco tutto, tocco lungo niente.
                     * ⚠️⚠️ **AGGIUNGE INVECE DI SOSTITUIRE, e la sua chiosa lo richiede**: *(come
                     * secondo comando dopo il tocco normale equivale a un 'azzera la selezione')*.
                     * Quella frase torna solo se il tocco somma e il tocco lungo sottrae; con una
                     * sostituzione la parola **solo** di *deseleziona solo le immagini* non
                     * vorrebbe dire niente, perché non ci sarebbe mai altro da lasciare in piedi.
                     * Il guadagno è che i due dati si compongono: immagini più video fa tutto.
                     */
                    val pesa = frontFacts && facts.bytes > 0L
                    val conta = frontFacts && facts.clips > 0
                    val scatta = frontFacts && facts.shots > 0
                    val tutti = items.orEmpty()
                    // ⚠️ Lo stesso conto del tocco lungo sul FAB, dalla `1.87`: due comandi che
                    // dicono la stessa cosa non possono avere due idee di che cosa sia 'tutto'.
                    val presi = allTaken
                    val foto = remember(tutti) { tutti.filterNot { Videos.isVideo(it) }.toSet() }
                    val clip = remember(tutti) { tutti.filter { Videos.isVideo(it) }.toSet() }
                    if (pesa || conta || scatta || frontPickAll) {
                        Spacer(Modifier.height(FRONT_CHIP_GAP))
                        /*
                         * ⚠️⚠️ **LA FILA SI ALLINEA AL LATO DEL FAB, DALLA `1.89`** (sua
                         * richiesta, con schermata: *quando le pastiglie vanno a capo, voglio
                         * che quella nella seconda (che è sempre 'Seleziona tutto', essendo in
                         * ultima posizione) sia centrata a destra o a sinistra a seconda del
                         * lato in cui si trova il FAB*). Fino alla `1.87` la seconda riga
                         * restava all'inizio, cioè dalla parte opposta al pollice quando il FAB
                         * è a destra.
                         * ⚠️ **L'allineamento è della FILA e non dell'ultima pastiglia**, ed è
                         * il solo modo che `FlowRow` offre: le sue righe hanno un allineamento
                         * solo, e non c'è un modificatore che ne sposti una.
                         * ⚠️⚠️ **E TOCCA SOLO IL CASO CHE HA CHIESTO, cioè quando si va a capo:
                         * misurato dal banco, non ragionato.** Il blocco dell'intestazione è
                         * centrato, quindi questa fila si dimensiona sul **contenuto** e non
                         * sulla larghezza della schermata: con le pastiglie tutte su una riga
                         * non le avanza un pixel da distribuire, e l'allineamento non ha niente
                         * da spostare. Quando si va a capo la fila è larga quanto la riga più
                         * lunga, ed è dentro quella larghezza che la seconda si sposta.
                         */
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp, fabEdge()),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .graphicsLayer { alpha = quanto() }
                        ) {
                            if (pesa) {
                                FrontChip(
                                    text = Formatter.formatShortFileSize(context, facts.bytes),
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
            }
        }
        }

        /*
         * ⚠️⚠️ **IL RIQUADRO AVVOLGE TUTTI E TRE I CASI, dalla 1.06, e non il solo elenco
         * pieno** (riscontro dell'utente sul collaudo: *il FAB deve apparire anche a cestino
         * vuoto, altrimenti è irraggiungibile*). Fino alla `1.05` il FAB nasceva dentro
         * il ramo dell'elenco pieno, quindi in un cestino vuoto non esisteva: e siccome la
         * **Cronologia** vive nel suo menu, un cestino appena svuotato si portava via l'unica
         * via per sapere che cosa c'era dentro. Il ramo che lo nascondeva era proprio quello
         * in cui serve di più.
         * ⚠️ Il `weight` serve: senza, con tre sole fotografie il riquadro sarebbe alto
         * quanto loro e il FAB finirebbe a mezza schermata invece che in basso.
         */
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        when {
            items == null -> CircularProgressIndicator(
                // ⚠️ `TopCenter` e non `CenterHorizontally`: qui il genitore è un `Box`, e
                // l'allineamento di colonna non esiste più.
                Modifier.padding(top = 24.dp).size(28.dp).align(Alignment.TopCenter)
            )

            /*
             * ⚠️⚠️ **CINQUE VUOTI DIVERSI, CINQUE FRASI, e dirli con la stessa sarebbe un
             * piccolo inganno**: un cestino vuoto è una buona notizia, una cartella vuota
             * dice che non c'è niente, un **filtro** senza esito dice che manca **quel
             * genere** e non che la cartella è vuota, e una ricerca senza esito dice che
             * nessun nome combacia. A ricerca ancora da scrivere non significa niente, e
             * allora non si dice nulla.
             * ⚠️⚠️ **IL FILTRO SI GUARDA PRIMA DELLA CARTELLA, dalla 1.09** (riscontro
             * dell'utente): con 'solo foto' acceso in una cartella di soli filmati, dire 'La
             * cartella è vuota' è **falso**, perché là dentro ci sono dei file. La frase deve dire
             * che cosa manca, non lamentare un vuoto che non c'è.
             * ⚠️ **La ricerca vince sul filtro**: se si sta cercando, quello che si vuole
             * sapere è se il nome combacia, e il filtro è una condizione in più che l'utente
             * ha in testa.
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
                // ⚠️ **Al centro dello spazio vuoto** (richiesta dell'utente), come nella
                // vista ad albero dalla `1.04`: una frase appesa in alto a sinistra sembra
                // l'inizio di un elenco che non arriva mai.
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
             * ⚠️⚠️ **IL GESTO STA SULLA GRIGLIA E NON SULLE PIASTRELLE, perché comincia su
             * una e finisce su un'altra** (richiesta dell'utente: *se striscio da una foto
             * all'altra deve avvenire una selezione da/a*). Una piastrella vede solo sé
             * stessa; la griglia le vede tutte e sa dove sono.
             * ⚠️⚠️ **LA CHIAVE DEL `pointerInput` NON COMPRENDE LA SELEZIONE, e sarebbe il
             * difetto che è già costato una versione** (la `0.32`): cambiare una chiave
             * **annulla il gesto in corso**, quindi con `chosen` fra le chiavi il
             * trascinamento si interromperebbe alla prima foto aggiunta, cioè subito. La selezione
             * si legge **dentro** il gesto, che è lettura e non chiave.
             * ⚠️ Il gesto si limita a dire **dove** sta il dito: chi estende la selezione è
             * l'effetto qui sotto, e averne uno solo vuol dire che il conto è identico sia
             * che si muova il dito sia che si muova la griglia sotto a un dito fermo.
             */
            val grab = Modifier.pointerInput(items) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { at ->
                        val hit = state.itemIndexAt(at)
                        if (hit != null) {
                            dragFrom = hit
                            dragBase = chosen
                            // ⚠️ Il verso si legge PRIMA di toccare la selezione, o la
                            // riga dopo lo avrebbe già falsato.
                            dragOff = items[hit] in chosen
                            // ⚠️ **Il colpetto è UNO SOLO dalla 1.21**, e qui stava il
                            // condizionale che dava quello forte all'ingresso nel modo
                            // selezione e quello leggero ai gesti dentro. Adesso sono la
                            // stessa cosa perché la vibrazione l'utente la vuole discreta
                            // dappertutto: il perché sta su [HOLD_BUZZ].
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
             * ⚠️⚠️ **LO SCORRIMENTO AI BORDI È QUELLO CHE RENDE LA FUNZIONE UTILE, non un
             * ornamento**: senza, una selezione da/a arriva al massimo fino al bordo dello
             * schermo, cioè a una quindicina di foto, e chi ne vuole cinquanta torna a
             * toccarle una per una. Col dito appoggiato in fondo la griglia scorre e la
             * selezione lo segue.
             * ⚠️⚠️ **Si aggiorna a ogni FOTOGRAMMA e non a ogni evento del dito**, ed è la
             * ragione per cui questo lavoro non sta dentro `onDrag`: mentre la griglia
             * scorre sotto un dito **fermo** non arriva nessun evento di puntatore, e la
             * selezione resterebbe ferma insieme a lui.
             * ⚠️ La spinta cresce **avvicinandosi al bordo** invece di essere un
             * interruttore: a velocità unica o si striscia piano e non basta, o si arriva
             * in fondo alla cartella prima di accorgersene.
             */
            LaunchedEffect(dragAt != null, items) {
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
                    // ⚠️ L'intervallo si SOMMA o si SOTTRAE alla selezione di partenza
                    // secondo il verso deciso da [dragOff]: nei due casi il conto resta
                    // 'quella di prima più (o meno) l'intervallo di adesso', quindi
                    // tornare indietro col dito disfa in tutti e due i versi.
                    val span = items.subList(minOf(from, hit), maxOf(from, hit) + 1)
                    chosen = if (dragOff) dragBase - span.toSet() else dragBase + span
                }
            }

            LazyVerticalGrid(
                    columns = GridCells.Fixed(spread(columns, LocalWindowInfo.current)),
                    state = state,
                    horizontalArrangement = Arrangement.spacedBy(GAP),
                    verticalArrangement = Arrangement.spacedBy(GAP),
                    // ⚠️ Il fondo cresce **con la selezione**, cioè quando il FAB
                    // compare: senza, la fotografia in basso a destra resterebbe coperta
                    // proprio mentre la si deve poter toccare. Fuori dalla selezione il
                    // FAB non c'è e quello spazio sarebbe un buco.
                    /*
                     * ⚠️⚠️ **SOTTO LA GRIGLIA CI VA IL PANNELLO MISURATO, dalla 0.94**: prima
                     * bastava lo spazio del FAB, che è alto quanto un dito; il pannello
                     * è due file di icone, e con [BELOW_FAB] l'ultima riga di fotografie
                     * sarebbe rimasta sotto di lui senza modo di tirarla fuori.
                     * ⚠️ Fuori dalla selezione il pannello non c'è, e resta [BELOW_FAB] per
                     * il solo FAB del cestino.
                     */
                    /*
                     * ⚠️⚠️ **E DALLA `1.90` CI SI SOMMANO IL MARGINE DELLA SCHERMATA E IL
                     * RIENTRO DI SOTTO**, che il contenitore ha smesso di mettersi: senza,
                     * l'ultima riga di miniature finirebbe sotto la barra gestuale senza modo
                     * di tirarla fuori, che è il prezzo di far arrivare la griglia al vetro.
                     */
                    contentPadding = PaddingValues(
                        bottom = GRID_PAD_Y + bottomInset() + if (picking) {
                            with(LocalDensity.current) { sheetTall.toDp() }
                        } else if (bin) BELOW_FAB else 16.dp
                    ),
                    modifier = Modifier.fillMaxWidth().then(grab)
                ) {
                    itemsIndexed(
                        items = items,
                        // ⚠️ La chiave è l'indirizzo e non la posizione: senza, ruotando
                        // il telefono le miniature già decodificate si rimescolerebbero
                        // fra i riquadri.
                        key = { _, uri -> uri.toString() },
                        // Un tipo solo per tutti i riquadri: così Compose riusa la
                        // composizione di quelli che escono per quelli che entrano,
                        // invece di ricostruirla a ogni riga che scorre.
                        contentType = { _, _ -> THUMB_KIND }
                    ) { index, uri ->
                        Thumbnail(
                            uri = uri,
                            position = index + 1,
                            total = items.size,
                            marked = index == highlight,
                            chosen = uri in chosen,
                            named = gridNames,
                            mark = lastMark,
                            room = cellPx,
                            // ⚠️ In selezione il tocco NORMALE sceglie invece di aprire,
                            // ed è la convenzione di ogni galleria: chi ne ha scelte
                            // cinque e tocca la sesta ne vuole sei, non vuole uscire e
                            // perderle.
                            /*
                             * ⚠️⚠️ **IL PRIMO RAMO RIPARA IL TOCCO LUNGO SU UNA SOLA FOTO,
                             * che dalla 0.53 non avviava più la selezione** (riscontro
                             * dell'utente sulla 0.65). Il gesto era sano: il difetto stava
                             * qui. `Modifier.clickable` **senza** `onLongClick` fa scattare
                             * il tocco al rilascio **qualunque sia stata la durata** della
                             * pressione, e nella passata `Main` gli eventi vanno dal figlio
                             * al genitore, quindi la piastrella li vede prima della griglia:
                             * il tocco lungo selezionava la foto, il dito si alzava, questo
                             * richiamo partiva con `picking` già vero e la **toglieva**.
                             * Effetto netto, niente. La spia è `dragFrom` e non un flag
                             * nuovo perché vale esattamente fra `onDragStart` e la fine del
                             * gesto: un tocco che la trova impostata è la **coda** di un
                             * tocco lungo, e un tocco normale non la trova mai, perché
                             * senza tocco lungo `onDragStart` non parte.
                             * ⚠️ **Col trascinamento non si vedeva**, ed è la ragione per
                             * cui la prova della 0.53 non l'aveva scoperto: `onDrag` consuma
                             * gli eventi, e un tocco i cui eventi sono consumati si annulla
                             * da sé. Il difetto viveva nel solo caso del dito fermo, cioè
                             * nel gesto che si fa per selezionarne una.
                             * ⚠️ **Non si ripara dando un `onLongClick` alla piastrella**,
                             * che è la strada ovvia: al tocco lungo `combinedClickable`
                             * consuma tutto fino al rilascio, e il gesto della griglia
                             * verrebbe annullato. Si perderebbe la selezione da/a per
                             * riparare quella singola.
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
    }

        /*
         * ⚠️⚠️ **A SINISTRA SI ROVESCIANO LE FILE, NON L'ELENCO**: girando la lista
         * intera, 'Copia' finirebbe nella seconda fila e 'Lista' nella prima, cioè
         * cambierebbe il raggruppamento invece della mano. Rovesciando ogni fila per
         * conto suo, le stesse cinque restano insieme e cambia solo da che parte
         * cominciano.
         */
        /*
         * ⚠️⚠️ **STA NEL `Box` DI RADICE DALLA 1.40, e prima viveva dentro la colonna**
         * (richiesta dell'utente, 2026-09-03: *fa' in modo che la barra multi-attività in
         * basso assuma lo stesso colore dello sfondo*). Dentro la colonna il rientro di
         * sistema è già applicato e **consumato**, quindi la scheda si fermava sopra la barra
         * e là sotto restava la pagina, di un altro colore. Qui arriva al bordo dello schermo
         * e il rientro se lo mette da sé, sul contenuto (vedi [PickSheet]).
         * ⚠️ **Misurato sullo screenshot**: la striscia della barra era
         * `252,251,247` contro i `242,241,237` della scheda.
         * ⚠️ **Sta PRIMA del velo del menu e di quello dell'onboarding**, come stava prima:
         * l'ordine dei figli di un `Box` è l'ordine di sovrapposizione, e i due veli devono
         * restare sopra di lei.
         */
        /*
         * ⚠️⚠️ **LE DUE SFUMATURE IN FONDO, dalla `1.76`, E QUI SE NE VANNO SCORRENDO**
         * (richiesta sua, giro della `1.67`: *esattamente insieme, sincronizzato con
         * l'animazione del titolo, le due sfumature in basso devono progressivamente sparire e
         * lasciare campo libero alla griglia piena su tutto lo schermo; anche in questo caso:
         * l'opposto se si torna in cima*). Nella schermata iniziale restano sempre, perché là il
         * FAB c'è sempre e vuole un fondo neutro sotto di sé; qui il FAB non c'è, quindi appena
         * l'intestazione è chiusa non hanno più niente da fare.
         * ⚠️ **La stessa curva, gli stessi numeri**: vivono in [GroundFade], che la schermata
         * iniziale legge dalla stessa riga. 'Sincronizzato' è alla lettera, perché è lo stesso
         * `aperto` che muove il titolo.
         * ⚠️⚠️ **STA PRIMA DELLA SCHEDA, DELLA NOTIFICA E DEI VELI**: in un `Box` l'ultimo
         * figlio sta sopra, e nessuno dei tre va sbiadito da lei.
         * ⚠️⚠️ **E QUI DI STRATI NE RESTA UNO, DALLA `1.85`** (riscontro del giro della `1.83`,
         * voce `fab-sopra`: *togli la seconda sfumatura sovrapposta, quella corta. SOLO DALLE
         * CARTELLE, resta in home*). La coda serviva a chiudere in pieno l'ultima striscia di
         * schermo, e qui sotto quella striscia adesso passa il FAB.
         */
        if (front) {
            GroundFade(
                modifier = Modifier.align(Alignment.BottomCenter),
                alpha = aperto,
                foot = false
            )
        }

        /*
         * ⚠️⚠️ **IL TASTINO STA DOPO LA SFUMATURA, DALLA `1.83`, E FINO ALLA `1.82` VIVEVA NELLA
         * COLONNA** (riscontro del giro della `1.82`, voce `fab-cartella` approvata con una
         * riserva: *deve stare SOPRA le sfumature*). In un `Box` l'ultimo figlio sta sopra,
         * quindi dentro la colonna il FAB finiva **sotto** le due sfumature, che sono figlie
         * della radice: al riposo non si vedeva, perché con l'intestazione aperta sono
         * trasparenti, e scorrendo il FAB si velava insieme alle miniature.
         * ⚠️ **La schermata iniziale ha sempre avuto quest'ordine**, e la sua nota lo dice da
         * cinque versioni (*sta prima del FAB e non dopo*): questa era l'unica delle due a
         * non seguirla, perché il suo FAB è nato nel cestino, dove la sfumatura non c'è.
         * ⚠️ **La posizione sullo schermo non cambia di un pixel**: i tre rientri che la colonna
         * gli dava adesso sono scritti sul suo modificatore, e sono gli stessi tre che
         * [HintVeil] usa per illuminarlo.
         */
        /*
         * ⚠️⚠️ **IL TASTINO RESTA SOLO NEL CESTINO SENZA SELEZIONE, dalla 0.94.**
         * Con una selezione in corso le operazioni stanno nella bottomsheet qui
         * sotto, e il FAB è sparito perché non aveva più niente da fare (vedi
         * [PickSheet]). Qui invece porta le tre voci che riguardano il cestino
         * **intero**, che non sono operazioni su una selezione e non hanno un altro
         * posto dove stare.
         */
        /*
         * ⚠️⚠️ **E DALLA 1.44 SI FA DA PARTE ANCHE PER LA NOTIFICA**: il gesto Indietro
         * azzera la selezione, quindi in quell'istante `picking` diventa falso e il
         * FAB tornerebbe **proprio dove** compare la notifica, che è larga tutto lo
         * schermo. Coprirebbe il tasto 'Annulla', cioè la sola cosa che quella notifica
         * ha da offrire.
         * ⚠️ **Riguarda il solo cestino**, come tutto questo FAB: in una cartella
         * normale non c'è e la notifica ha il fondo tutto per sé.
         */
        /*
         * ⚠️⚠️ **DALLA `1.82` IL TASTINO C'È ANCHE IN UNA CARTELLA NORMALE** (riscontro del
         * giro della `1.81`, campo libero punto B: *il FAB deve vedersi in tutte le cartelle,
         * non solo nella schermata home*). Fino alla `1.81` viveva nel solo cestino, e da
         * dentro una cartella il cestino e le impostazioni si raggiungevano tornando indietro.
         * ⚠️ **Le voci non sono le stesse**: nel cestino porta le tre che riguardano il
         * cestino intero, in una cartella le due destinazioni che di qui non si raggiungono.
         * A dirlo è [PickMenu], che riceve un blocco diverso.
         * ⚠️ **Senza i due richiami non compare**, ed è il caso della griglia montata in una
         * veste che non ha dove mandare (vedi i due parametri): un FAB che apre un menu
         * vuoto è peggio di un FAB che non c'è.
         */
        FabPop(
            visible = (bin || onSettings != null || onBin != null || onSearchHere != null) &&
                !picking && cleared == null,
            // ⚠️ Il lato è quello scelto nelle impostazioni: vedi `PadLook.hand`.
            // ⚠️ I tre rientri sono quelli che gli dava la colonna, e adesso se li mette da
            // sé: quello di sistema, il margine della schermata e gli 8dp del FAB.
            // Sono gli stessi che [HintVeil] riceve per illuminarlo, e restano scritti una
            // volta sola per ognuno dei due.
            modifier = Modifier
                .align(fabSide())
                .safeDrawingPadding()
                .padding(horizontal = GRID_PAD_X, vertical = GRID_PAD_Y)
                .padding(8.dp)
        ) {
            Box {
                /*
                 * ⚠️⚠️ **IL MENU È SCRITTO PRIMA DEL TASTINO, e quest'ordine è la
                 * funzione** (1.39): il FAB si stacca in una finestra sua per restare
                 * sopra il velo (vedi `lifted` in [TapHoldFab]), e fra finestre dello
                 * stesso tipo comanda l'ordine in cui sono state aggiunte, che è quello
                 * della composizione. Scritto dopo, il menu coprirebbe il FAB invece
                 * del contrario.
                 * ⚠️ **Il menu non si sposta di un pixel**: il posizionatore legge il
                 * bordo di sopra di questo riquadro, che è lo stesso qualunque sia
                 * l'ordine dei figli.
                 */
                PickMenu(menu = menu, columns = columns) {
                    /*
                     * ⚠️⚠️ **IN UNA CARTELLA IL MENU È UN ALTRO, DALLA `1.82`**: le tre
                     * voci qui sotto riguardano il cestino intero e in una cartella non
                     * vogliono dire niente. Quelle di una cartella sono le due destinazioni
                     * che di qui non si raggiungono, cioè quello per cui lui ha chiesto il
                     * FAB: *il FAB deve vedersi in tutte le cartelle*.
                     * ⚠️ **Nello stesso ordine della schermata iniziale**: prima il cestino,
                     * poi il filetto, poi le impostazioni. Chi ha imparato dove sta una voce
                     * la ritrova, che è la ragione per cui questo menu passa dallo stesso
                     * [MenuRow] e non da un elenco scritto a parte.
                     */
                    if (!bin) {
                        /*
                         * ⚠️⚠️ **'Cerca' STA IN CIMA, DALLA `1.83`, COME NELLA SCHERMATA
                         * INIZIALE** (risposta a `d-fab-voci` del giro della `1.82`): là la sua
                         * nota dice che *cercare è la domanda che si fa più spesso quando non si
                         * sa già dove andare*, e in una cartella vale ancora di più, perché le
                         * altre due voci portano **fuori** di qui mentre questa resta dentro.
                         */
                        onSearchHere?.let { cerca ->
                            MenuRow(
                                text = stringResource(R.string.hub_search),
                                icon = Icons.Default.Search,
                                onTap = { menu.close(); cerca() }
                            )
                        }
                        /*
                         * ⚠️⚠️ **QUI C'È LA SOLA VOCE CHE TOGLIE, e non quella che sceglie**: a
                         * scegliere è il tocco sull'icona dell'intestazione, che è la sua
                         * specifica alla lettera (risposta a `d-copertina-come`: *solo con il
                         * tocco singolo sull'icona dell'intestazione di una cartella*). Una
                         * seconda porta per la stessa cosa sarebbe un secondo modo da imparare
                         * per un comando che si dà una volta per cartella.
                         * ⚠️ **C'è se e solo se una copertina scelta esiste**, come 'Mostra
                         * nascoste' nella schermata iniziale: offrire di togliere quello che non
                         * c'è è una riga che non fa niente.
                         * ⚠️ **Vive fra 'Cerca' e 'Cestino' perché l'ordine dice una cosa**: sopra
                         * quello che si fa dentro questa cartella, sotto quello che porta
                         * altrove.
                         * ⚠️⚠️ **DALLA `1.95` NON SI VEDE, PERCHÉ [COVER_MENU_ROW] È SPENTA**: la
                         * voce l'ha bocciata lui, e a togliere la copertina adesso è il gesto
                         * ricorsivo sull'icona dell'intestazione. Il perché, e come si riaccende,
                         * vivono su quella costante.
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
                     * ⚠️⚠️ **L'ORDINE NON È CASUALE**: prima quella che rimette a
                     * posto, poi quella che racconta, ultima quella che cancella per
                     * sempre. Chi tocca al buio la prima voce di un menu non deve
                     * poterci svuotare il cestino, e 'Ripristina tutto' come prima
                     * voce è la richiesta dell'utente.
                     * ⚠️ **Le due azioni si spengono sul cestino vuoto**, la
                     * cronologia no: quelle non avrebbero niente su cui agire e
                     * direbbero '0 fatti', mentre la cronologia ha senso proprio
                     * quando il cestino è vuoto perché si è ripristinato tutto.
                     */
                    /*
                     * ⚠️⚠️ **TRE `MenuRow` E NON PIÙ TRE `DropdownMenuItem`, dalla
                     * `1.46`**: erano l'ultima fila di voci scritta con un componente
                     * diverso da quello degli altri menu, e il prezzo del cambio è
                     * dichiarato: il rientro di sinistra passa da 12 a 15dp, cioè le tre
                     * voci si spostano di tre punti a destra. Quei tre punti esistono per
                     * il glifo che sporge nel menu del visualizzatore, e portarli qui è
                     * esattamente allineare i due menu fra loro.
                     * ⚠️ **Il margine sopra e sotto lo mette la superficie**, quindi
                     * `PICK_EDGE` non c'è più: era il terzo posto in cui viveva lo stesso
                     * otto.
                     */
                    /*
                     * ⚠️⚠️ **LA CRONOLOGIA STA IN CIMA, dalla 1.53, per sua richiesta**
                     * (riscontro del giro della `1.51`, voce `icone-cestino`: *cambia
                     * l'ordine delle voci portando 'Cronologia' in cima*). Le altre due
                     * agiscono su tutto il contenuto, questa lo racconta: chi apre questo
                     * menu senza sapere che cosa c'è dentro incontra prima la voce che
                     * glielo dice, e le due che muovono i file dopo.
                     * ⚠️ **È anche la sola sempre toccabile**: le altre due si spengono a
                     * cestino vuoto, quindi in cima ci sarebbero due righe grigie e la sola
                     * viva in fondo.
                     */
                    MenuRow(
                        text = stringResource(R.string.bin_history),
                        /*
                         * ⚠️⚠️ **UN DISEGNO SUO, dalla `1.55`, e la sua terza scelta su
                         * questa riga.** Nella `1.51` aveva preso il simbolo del riciclo
                         * fra due proposte, perché `Icons.Default.History` diceva 'il
                         * tempo' mentre qui conta quello che è **passato di qui**; poi lo
                         * ha guardato in mano e lo ha ridisegnato (*pensavo che fosse un
                         * miglioramento, ma non mi piaceva*). Il suo mette insieme le due
                         * cose: il cassone e la freccia che torna indietro.
                         */
                        icon = Glyphs.BinHistory,
                        onTap = { menu.close(); onHistory() }
                    )
                    MenuRow(
                        text = stringResource(R.string.bin_restore_all),
                        // ⚠️ **Lo stesso glifo del ripristino singolo, dalla `1.56`**, per
                        // sua istruzione: il perché sta su [Glyphs.BinRestore].
                        icon = Glyphs.BinRestore,
                        enabled = filled,
                        /*
                         * ⚠️⚠️ **ADESSO CHIEDE, dalla 1.53, e la nota di prima diceva il
                         * contrario** (richiesta dell'utente, giro della `1.51`:
                         * *'Ripristina tutto' deve funzionare previa conferma*). Quella
                         * nota diceva che il ripristino non chiede perché è reversibile,
                         * e l'argomento resta vero per **una** immagine: rimette una cosa
                         * dov'era, e la si rielimina con un tocco. Su **tutto** il cestino
                         * no, e la differenza non è la reversibilità ma il **sapere dove
                         * vanno**: i file tornano ognuno nella sua cartella d'origine, che
                         * possono essere molte e non tutte in mente, quindi disfare a mano
                         * vorrebbe dire ritrovarli uno per uno.
                         * ⚠️ **Per questo il testo della conferma nomina la Cronologia**,
                         * che è il posto in cui quelle destinazioni sono scritte: le parole
                         * sono sue.
                         */
                        onTap = { menu.close(); restoringAll = true }
                    )
                    MenuRow(
                        text = stringResource(R.string.bin_empty),
                        icon = Icons.Default.DeleteForever,
                        enabled = filled,
                        danger = true,
                        onTap = { menu.close(); emptying = true }
                    )
                }
                val altroTema = aivLauncher(!LocalAivLight.current)
                PickFab(
                    // ⚠️ I colori dell'icona dell'app, dalla `1.36`, come il FAB della
                    // schermata iniziale: il perché per esteso è là, e i due FAB sono
                    // lo stesso oggetto in due schermate. ⚠️ Dalla `1.86` sono quelli
                    // dell'**altro** tema e li dà `aivLauncher`, che legge le risorse col tema
                    // dell'app invece che con quello di sistema: le due ragioni vivono là.
                    container = altroTema.first,
                    ink = altroTema.second,
                    holdLabel = shortcutLabel,
                    // ⚠️ E dalla `1.83` anche lo stesso glifo, in una cartella: nel cestino
                    // restano i tre puntini. Il perché è su [PickFab].
                    mark = !bin,
                    /*
                     * ⚠️⚠️ **A TASTO ARMATO IL TOCCO FA IL SALTO E NON APRE IL MENU, DALLA
                     * `2.07`**: è la conseguenza della sua scelta, cioè che il comando viva
                     * **sul** FAB invece che accanto. Il tratto in cui il menu non si apre è
                     * quello in cui il chevron si vede, e finisce da sé un secondo dopo
                     * l'ultimo pixel scorso.
                     */
                    arm = arm,
                    // ⚠️ **`visible` e non `wanted`**: il FAB deve restare staccato per tutta
                    // l'uscita, o rientrerebbe nella finestra dell'app sotto il velo che se ne
                    // sta andando. ⚠️ Dalla `1.67` `visible` copre anche quello: era `veiling`
                    // finché la patina durava più del pannello.
                    lifted = menu.visible,
                    // ⚠️ **`wanted` e non `visible`**: il perché sta sul parametro
                    // `pressed` di [TapHoldFab], ed è il riscontro del giro della `1.59`.
                    pressed = menu.wanted,
                    // ⚠️ **Apre e basta, dalla 1.06**: a menu aperto il tocco non
                    // arriva più qui, perché lo consuma `MenuGuard` (in `Menus.kt`,
                    // messo in scena da `AivTheme`). Un'alternanza qui riaprirebbe
                    // il menu che quella guardia ha appena chiuso.
                    // ⚠️⚠️ **IL RIMANDO ERA SBAGLIATO DUE VOLTE FINO ALLA `1.78`**:
                    // nominava un `menuOpen` che non è mai esistito, e diceva 'in
                    // fondo alla schermata', mentre dalla `1.70` quella guardia non
                    // vive più qui dentro.
                    // ⚠️ **E lo raggiunge ancora benché il FAB stia in una finestra
                    // più alta**: quella finestra è trasparente al tocco apposta
                    // (vedi `untouchable` in `ActionPad`).
                    onTap = { if (arm.armed) scope.launch { arm.leap(paging) } else menu.open() },
                    onHold = { shortcut(); hintDone() }
                )
            }
        }

        PickSheet(
            visible = picking,
            /*
             * ⚠️⚠️ **LA SPECCHIATURA È USCITA DEL TUTTO NELLA `1.57`** (decisione dell'utente,
             * giro della `1.55`: *la specchiatura se ne va del tutto, l'altra funzionalità la
             * sostituirà*). Rovesciava le due file per la mano sinistra, e adesso quel
             * mestiere lo fanno meglio due cose insieme: l'**ordine** che si trascina, che
             * mette ogni tasto dove uno lo vuole, e il **lato del FAB**, che sposta tutto
             * il resto. La chiave che diceva la mano adesso dice il lato, quindi chi aveva
             * scelto la sinistra non perde niente.
             */
            actions = pickActions,
            onHeight = { sheetTall = it }
        )

        /*
         * ⚠️⚠️ **LA NOTIFICA DELL'AZZERAMENTO, dalla 1.44**, che è la seconda metà della
         * richiesta con cui la conferma è uscita (istruzione dell'utente, 2026-09-03: *deve
         * apparire per 3 secondi (o finché non si cambia cartella) una notifica in basso che
         * a sinistra dice 'Selezione azzerata' e a destra un pulsante 'Annulla' che la
         * ripristina e fa riapparire la bottomsheet*).
         * ⚠️⚠️ **E DALLA `1.84` QUI NON SI DISEGNA PIÙ NIENTE**: la notifica dell'azzeramento
         * passa dal canale, e la superficie con cui l'app parla è una sola, in `AivApp`. Quello
         * che resta di questa nota è la ragione per cui il velo dei menu deve restare **sopra**
         * la notifica, e quella non è cambiata: senza, un tocco fuori dal menu del FAB
         * finirebbe sul tasto 'Annulla'.
         * ⚠️ **Non serve dire alla griglia che c'è**: [sheetTall] esiste perché la scheda delle
         * azioni copre l'ultima fila di immagini per tutto il tempo della selezione, mentre
         * questa passa in tre secondi e non porta niente da raggiungere sotto di lei.
         */

        /*
         * ⚠️⚠️ **IL VELO CHE CHIUDEVA IL MENU DEL TASTINO STAVA QUI FINO ALLA `1.69`, E ADESSO
         * VIVE IN `AivTheme`** (vedi `MenuGuard` in `Menus.kt`). Il fatto che lo aveva fatto
         * nascere nella `1.06` non è cambiato ed è questo: da Android 12 la finestra di un popup
         * **non è modale al tocco**, quindi un dito fuori dal pannello arriva a tutte e due le
         * finestre, e il solo `dismissOnClickOutside` non basta.
         * ⚠️⚠️ **A spostarlo è stato il censimento della UI del 2026-09-05**: questo velo esisteva
         * in **una** schermata su cinque, e le altre quattro avevano il difetto intero (nella
         * vista ad albero il tocco che chiudeva il menu apriva la riga sotto il dito). Ripeterlo
         * qui altre quattro volte avrebbe lasciato in piedi il quinto modo di dimenticarsene: uno
         * solo, sopra tutto quello che l'app disegna, vale per ogni menu che nascerà.
         * ⚠️ **Non è una perdita di comportamento**: quello copriva lo schermo intero e consumava
         * il tocco, e il velo nuovo fa la stessa cosa una passata prima, quindi questo non
         * arriverebbe mai a vederlo.
         */

        /*
         * ⚠️⚠️ **IL MINI ONBOARDING DEL TOCCO LUNGO**, che dalla `0.78` è un velo condiviso:
         * il colore, il contrasto misurato e la geometria stanno in [HintVeil], qui restano la
         * frase e il FAB.
         * ⚠️⚠️ **E dalla `0.73` è l'UNICA via a insegnare la scorciatoia**, perché il FAB
         * 'Tutte' in testata non c'è più (vedi la nota là dove stava): finché c'era, questo
         * velo era un aiuto e la barra la rete di sicurezza.
         * ⚠️⚠️ **IL VELO DI QUESTA SCHERMATA È UNO, quello del cestino**, e compare alla prima
         * apertura del cestino.
         * ⚠️⚠️ **FINO ALLA `1.78` QUESTA NOTA NE CONTAVA DUE, e il secondo non esiste dalla
         * `0.94`**: era quello della selezione, uscito con la sua chiave (`Settings.kt` lo
         * dichiara). Il difetto era della specie peggiore, perché una nota che conta descrive
         * anche quello che non c'è: chi cercava il velo della selezione lo cercava nel codice.
         */
        if (hint != null) {
            HintVeil(
                text = stringResource(
                    when (hint) {
                        Hint.BIN_EMPTY -> R.string.bin_empty_hint
                        // ⚠️ Le colonne non si insegnano qui: quel velo vive nella schermata
                        // delle cartelle, dove sta il FAB che le cambia. Il ramo c'è
                        // perché [Hint] è un enum e il `when` deve essere completo, e questa
                        // frase non si vedrà mai (vedi `hint`, che la esclude).
                        Hint.COLUMNS -> R.string.columns_hint
                        // ⚠️ Idem per il doppio tocco, che vive nel visualizzatore e non ha
                        // nemmeno un FAB da evidenziare: là il velo è `HintCentre`.
                        Hint.ZOOM_TAP -> R.string.hint_zoom_tap
                        // ⚠️ E idem per l'avviso sulle estensioni, che non è nemmeno un velo
                        // di questa forma: è un `HintNotice`, cioè una finestra sua, aperta
                        // dalla finestra di rinomina.
                        Hint.EXT_WARN -> R.string.hint_ext_warn
                        // ⚠️ E idem per la copertina: quel velo indica l'icona dell'intestazione,
                        // quindi è un `HintSpot` e la sua frase la sceglie lui.
                        Hint.COVER -> R.string.hint_cover
                    }
                ),
                // ⚠️ Tre rientri: quello di sistema, il margine della schermata e gli 8dp
                // del FAB. Il perché sta in [HintVeil], sul parametro.
                inset = Modifier
                    .safeDrawingPadding()
                    .padding(horizontal = GRID_PAD_X, vertical = GRID_PAD_Y)
                    .padding(8.dp),
                onDone = hintDone
            ) {
                PickFab(
                    container = HINT_MARK,
                    ink = HINT_INK,
                    holdLabel = shortcutLabel,
                    // ⚠️ **Lo stesso valore del FAB vero**, che è il mestiere di questa
                    // copia: un velo che illuminasse un disegno diverso indicherebbe il tasto
                    // sbagliato. Oggi questo velo compare solo nel cestino, quindi la condizione
                    // è sempre falsa: scritta uguale, resta vera anche il giorno che un
                    // onboarding nuovo comparisse in una cartella.
                    mark = !bin,
                    // ⚠️ Qui il salto non c'è: questa copia vive dentro un velo che insegna il
                    // tocco lungo, e un chevron sopra di lei indicherebbe un altro comando.
                    arm = null,
                    onTap = { hintDone(); menu.open() },
                    onHold = { shortcut(); hintDone() }
                )
            }
        }

        /*
         * ⚠️⚠️ **IL MINI ONBOARDING DELLA COPERTINA, DALLA `1.95`, ED È SUA RICHIESTA ALLA
         * LETTERA** (riscontro del giro della `1.94`: *la prima volta che si tocca la copertina e
         * si avvia la selezione di un'immagine personalizzata, deve esserci un mini-onboarding con
         * l'icona dell'intestazione evidenziata nell'arancione onboarding, più il seguente testo
         * sotto, centrato*). Il testo è il suo, e vive in `hint_cover`.
         * ⚠️ **Non passa da [hint]**, che è il velo del FAB: quello indica un tasto in fondo allo
         * schermo e questo un'icona in cima, quindi sono due veli diversi e non due frasi dello
         * stesso. La chiave però è nello stesso enum, perché 'Ripristina gli avvisi' li deve
         * rimettere tutti.
         * ⚠️ **Vuole il riquadro dell'icona**, quindi non compare prima che la fascia sia stata
         * disegnata: è la condizione su [iconaSpot], e in pratica non si vede mai, perché il velo
         * nasce da un tocco **su** quell'icona.
         */
        val dovIcona = iconaSpot
        if (coverHere && !coverSeen && dovIcona != null) {
            HintSpot(
                text = stringResource(R.string.hint_cover),
                spot = dovIcona,
                glyph = Glyphs.FolderAiv,
                onDone = { scope.launch { Hint.COVER.remember(context) } }
            )
        }
    }

    /*
     * ⚠️⚠️ **QUESTO DIALOGO STA QUI E NON IN `FileOps.kt`, e la ragione è che non parla di
     * file scelti**: le altre operazioni ricevono un elenco, questa svuota una cartella
     * intera, quindi non entra in `FileJob`, che è fatto di elenchi. Sta nel solo posto da
     * cui si può chiedere, cioè il FAB del cestino.
     * ⚠️ L'esito usa l'avviso dell'eliminazione, che è quello che succede: i file vanno via
     * per davvero.
     */
    /*
     * ⚠️⚠️ **QUI VIVEVA LA CONFERMA DI BUTTARE VIA LA SELEZIONE, dalla 1.06 alla 1.43**, con
     * la domanda e i due verbi 'Mantieni' e 'Scarta' (che erano verbi e non 'Annulla' e 'OK'
     * per una ragione ancora buona: su una domanda 'Annulla' non dice **che cosa** annulla).
     * L'ha tolta l'utente, e al suo posto c'è la notifica con 'Annulla' in fondo alla
     * schermata: il perché sta su [cleared]. ⚠️ Chi la rimettesse avrebbe due cure per lo
     * stesso sbaglio, una che chiede prima e una che disfa dopo.
     */

    /*
     * ⚠️⚠️ **LA RINOMINA DELLA CARTELLA È LA STESSA FINESTRA DEL FILE SINGOLO, ED È SUA
     * ISTRUZIONE** (2026-09-08: *usa esattamente la stessa finestra di rinomina del file singolo,
     * ovviamente senza percorso né estensione*). Quello che cambia lo dice il parametro `folder`
     * di [RenameDialog], e il perché di ogni differenza vive là.
     * ⚠️ **Il nome che arriva è il titolo della schermata**, cioè quello che si vede
     * nell'intestazione: la cartella è questa, e il campo parte da com'è scritta adesso.
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
     * ⚠️ **Il selettore della tinta di questa cartella**, che apre il tocco lungo sull'icona
     * dell'intestazione. Vive qui con gli altri dialoghi della schermata e non dentro la fascia:
     * una finestra dentro un nodo che si misura e si ritaglia a ogni fotogramma di scorrimento
     * sarebbe una finestra che nasce e muore con lui.
     */
    if (tinge) {
        TintDialog(
            current = frontTint,
            onPick = onFrontTint,
            onDismiss = { tinge = false }
        )
    }

    /*
     * ⚠️⚠️ **LA CONFERMA DI 'RIPRISTINA TUTTO', dalla 1.53, e NON è pericolosa**: il tasto
     * che conferma non porta il colore dell'errore come quello di 'Svuota il cestino', perché
     * qui non si perde niente. La conferma non serve a fermare un danno, serve a dire **dove
     * finiscono** i file, che è la sola cosa che chi tocca quella voce non può sapere: il
     * perché per esteso sta sulla voce del menu.
     */
    if (restoringAll) {
        AlertDialog(
            onDismissRequest = { restoringAll = false },
            modifier = Modifier.lowered { restoringAll = false },
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

    if (emptying) {
        AlertDialog(
            onDismissRequest = { emptying = false },
            modifier = Modifier.lowered { emptying = false },
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

    // ⚠️ I cinque dialoghi stanno in `FileOps.kt` perché li chiede anche il
    // visualizzatore: qui resta la sola cosa che è di questa schermata, cioè che a
    // operazione finita la cartella si rilegge.
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
 * Quale riquadro sta sotto un punto, o `null` se là non c'è niente.
 *
 * ⚠️ Si guardano i soli riquadri **in vista**, che è tutto quello che serve e tutto quello
 * che si può sapere: di una fotografia fuori schermo la griglia pigra non conosce nemmeno
 * la posizione. ⚠️ Un punto nei distacchi fra le piastrelle non appartiene a nessuna, e
 * torna `null` invece del vicino più prossimo: durante un trascinamento significa che
 * l'intervallo non cambia per un istante, che è meglio di un intervallo che salta.
 */
private fun LazyGridState.itemIndexAt(at: Offset): Int? =
    layoutInfo.visibleItemsInfo.firstOrNull {
        at.x >= it.offset.x && at.x < it.offset.x + it.size.width &&
            at.y >= it.offset.y && at.y < it.offset.y + it.size.height
    }?.index

/**
 * Un riquadro della griglia.
 *
 * ⚠️⚠️ **IL SEGNO È UN RIQUADRO SOPRA, NON UN BORDO NELLA CATENA DEI MODIFICATORI**, e
 * la differenza è la lezione della `0.34`, dove l'anello non si vedeva: un `Modifier.border`
 * dipende da dove sta nella catena e da come il nodo che disegna l'immagine si comporta col
 * `drawContent`, cioè da due cose che stanno in due librerie diverse. Due fratelli dentro un
 * `Box` invece si dipingono nell'ordine in cui sono scritti, e su questo non c'è niente da
 * sapere: il secondo sta sopra il primo, sempre.
 * ⚠️ Il velo colorato non è decorazione in più: un filo di 3dp su una miniatura piena di
 * dettagli si perde, mentre una tinta sull'intero riquadro si vede dall'altra parte della
 * stanza, che è quello che serve a ritrovare il proprio posto.
 * ⚠️ Il riquadro di sopra **non intercetta il tocco**: in Compose partecipa al colpo solo
 * chi porta un modificatore di puntatore, e qui non ce n'è. Il tocco arriva all'immagine
 * sotto, che è quella che apre.
 */
/**
 * Il FAB della selezione: quello vero, e la sua copia illuminata sopra il velo del
 * suggerimento.
 *
 * ⚠️⚠️ **NASCE PERCHÉ ERA SCRITTO DUE VOLTE, E L'INVARIANTE ERA AFFIDATA A UN COMMENTO**
 * (censimento della UI del 2026-09-05): la nota accanto alla copia diceva *lo STESSO glifo del
 * FAB vero ... un velo che evidenzia un disegno diverso da quello che sta sotto indica il
 * tasto sbagliato*, e niente lo teneva fermo. Cambiando il glifo del FAB vero, il velo
 * avrebbe continuato a illuminare quello di prima senza che nessuno lo segnalasse.
 * - **I quattro valori condivisi vivono qui**: l'etichetta, l'etichetta del tocco lungo, il
 *   gesto lungo e il glifo. Quello che i due chiamanti passano è ciò che deve differire, cioè i
 *   colori e che cosa fa il tocco breve.
 * - ⚠️⚠️ **I TRE PUNTINI ARRIVANO DALLA SCHERMATA INIZIALE, dalla `1.55`** (richiesta
 *   dell'utente, giro della `1.54`: *i tre puntini, renderizzati in modo identico, vanno a
 *   finire sul FAB del cestino, dove c'era un'icona ancora più generica*). Là hanno lasciato il
 *   posto al marchio dell'app, e qui prendono il posto del disco singolo della `1.37`.
 *   ⚠️ **'Renderizzati in modo identico' è alla lettera**: stesso glifo di Material e stessa
 *   misura, senza scale né ritocchi, o sarebbero due disegni che si somigliano invece dello
 *   stesso disegno. Adesso lo garantisce il fatto che il disegno è uno.
 *
 * - ⚠️⚠️ **E IN UNA CARTELLA IL GLIFO È QUELLO DELL'APP, DALLA `1.83`** (riscontro del giro della
 *   `1.82`, voce `fab-cartella`: *anche dentro le cartelle deve esserci il glifo dell'app:
 *   l'altra icona sta solo nel cestino*). I tre puntini restano dove sono nati con la `1.55`,
 *   cioè nel cestino, e in una cartella il FAB è lo stesso oggetto della schermata iniziale:
 *   stessi colori dalla `1.36`, e adesso anche lo stesso marchio.
 *   ⚠️ **A sceglierlo è un parametro e non il chiamante**, per la ragione che questa funzione
 *   esiste: la copia illuminata sotto il velo deve portare **lo stesso** disegno del FAB
 *   vero, e passandolo da fuori sarebbero di nuovo due decisioni che nessuno tiene insieme.
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
     * ⚠️ **Non ha un valore di serie di proposito**, come il parametro di `Modifier.lowered`:
     * chi disegna un FAB deve **dichiarare** se quel FAB porta anche il salto, e non può farlo
     * per omissione. L'unico `null` di oggi è la copia illuminata di un onboarding, dove il
     * chevron non c'entra e il velo indica un gesto diverso.
     */
    arm: JumpArm?,
    onTap: () -> Unit,
    onHold: () -> Unit,
    lifted: Boolean = false,
    pressed: Boolean = false
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
        onTap = onTap,
        onHold = onHold,
        glyph = { d -> if (arm == null) home(d) else JumpGlyph(arm) { home(d) } }
    )
}

@Composable
private fun Thumbnail(
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
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(CORNER)
    // La richiesta si costruisce una volta per indirizzo: ricrearla a ogni
    // ricomposizione darebbe a Coil un oggetto nuovo da confrontare per ogni fotogramma
    // di scorrimento, e questo è il posto in cui i fotogrammi contano.
    val context = LocalContext.current
    val model = remember(uri, context) { Thumbs.request(context, uri) }

    /*
     * ⚠️⚠️ **LA COLONNA C'È ANCHE QUANDO IL NOME NON C'È, ed è la scelta più economica**:
     * un ramo che avvolge la miniatura solo quando serve vorrebbe dire scrivere due volte
     * tutto quello che sta nel riquadro, e un nodo di layout in più per cella non si misura.
     * ⚠️ **Il quadrato sta in cima**, quindi le miniature di una riga restano allineate
     * anche quando i nomi sotto sono di due righe e di una: quello che varia è l'altezza
     * della cella, e la griglia dà a tutta la riga l'altezza della più alta.
     */
    Column(
        verticalArrangement = Arrangement.spacedBy(NAME_GAP),
        /*
         * ⚠️⚠️ **IL TOCCO VIVE SULLA COLONNA E NON SUL RIQUADRO, DALLA `1.81`: così il nome
         * FA PARTE della piastrella** (censimento della UI del 2026-09-05). Fino alla `1.80`
         * il `clickable` viveva sull'immagine, cioè dentro il quadrato, e il nome sotto era
         * un fratello del quadrato: toccarlo non apriva niente, e le due viste gemelle
         * rispondevano in modo diverso allo stesso gesto, perché la griglia delle **cartelle**
         * fa il contrario e lo dichiara.
         * ⚠️⚠️ **E COSÌ IL LETTORE DI SCHERMO LEGGE UN NODO SOLO**: `clickable` unisce le
         * semantiche dei discendenti, quindi la posizione nella cartella e il nome del file
         * arrivano insieme invece di essere due voci da attraversare.
         * ⚠️ **Il tocco LUNGO resta dov'è**, cioè sulla griglia: quel gesto continua col
         * trascinamento e attraversa più piastrelle, quindi non può vivere dentro una (la
         * nota per esteso è più sotto, sull'immagine).
         */
        modifier = Modifier
            .clickable(onClick = onClick, role = Role.Button)
            /*
             * ⚠️⚠️ **LA SCELTA SI DICHIARA, e fino alla `1.80` non lo faceva** (censimento
             * della UI del 2026-09-05): la descrizione porta la posizione nella cartella, la
             * spunta è dichiarata decorativa, e in selezione il tocco breve alterna la scelta,
             * quindi la piastrella si comportava da interruttore senza dirlo. Chi legge con
             * TalkBack non aveva **nessun** modo di sapere quali immagini aveva scelto.
             * ⚠️ **`selected` e non una stringa di stato**: il nome dello stato lo dice il
             * lettore di schermo nella lingua del telefono, mentre una `stateDescription`
             * sarebbe stata una stringa nuova in ventotto lingue per dire la stessa cosa.
             */
            .semantics { selected = chosen }
    ) {
    Box(modifier = Modifier.aspectRatio(1f)) {
        AsyncImage(
            // ⚠️ La richiesta viene da `Thumbs` e non è costruita qui: la misura è parte
            // della chiave di cache, quindi deve essere la stessa dovunque (vedi `Thumbs.PX`).
            model = model,
            // ⚠️⚠️ **La chiave si registra anche QUI, e non è una ripetizione della
            // `Preview` del visualizzatore**: questo copre la PRIMA fotografia che si
            // apre, quella toccata nella griglia, per la quale nessuna vicina ha ancora
            // caricato niente. Senza, all'ingresso nel visualizzatore resterebbe il
            // fotogramma vuoto che tutto il resto serve a togliere. Il perché sta accanto
            // a `Thumbs.note`.
            onState = { st ->
                if (st is AsyncImagePainter.State.Success) Thumbs.note(uri, st.result.memoryCacheKey)
            },
            // Ogni riquadro è toccabile, quindi non è decorativo: chi legge con TalkBack
            // deve sapere dove si trova nella cartella, e se è quello da cui è tornato.
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
                // ⚠️⚠️ **NESSUNO DEI DUE TOCCHI VIVE QUI, ed è la lezione già pagata dalla
                // `0.22`**: dalla `0.53` il tocco lungo apre una selezione **da/a** che
                // continua col trascinamento, e un gesto che comincia su una piastrella e
                // finisce su un'altra non può vivere dentro la piastrella, quindi vive sulla
                // griglia, che è l'unica che le vede tutte; il tocco breve dalla `1.81` sta
                // sulla **colonna**, così comprende anche il nome (vedi la nota là sopra).
                // ⚠️ Chi volesse aggiungere un gesto lo aggiunga **dentro** uno dei due, non
                // accanto.
        )
        /*
         * ⚠️⚠️ **IL VELO DELLA SCELTA VA PRIMA DEL NASTRO, e l'ordine è una decisione**:
         * dentro un `Box` si dipinge nell'ordine in cui si scrive, quindi il nastro
         * disegnato dopo resta **pieno** anche su una foto scelta. Al contrario, il velo
         * sopra lo schiarirebbe insieme alla fotografia, e i due segni che devono
         * distinguersi comincerebbero a somigliarsi proprio sulla piastrella dove
         * convivono, che è il caso peggiore.
         * ⚠️ **SCHIARISCE, non scurisce** (richiesta dell'utente, 2026-08-29): scurire
         * faceva sembrare la foto scelta più lontana, come se fosse stata messa da parte,
         * mentre sceglierla è tirarla avanti.
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
             * ⚠️⚠️ **LA CORNICE È DELLA `2.11`, E RIMETTE IN SCENA QUELLO CHE LA `0.58` AVEVA
             * SCARTATO** (punto D del campo libero del giro accorpato: *voglio che il nuovo
             * indicatore sia una semplice cornice come quella nel mockup allegato, colore
             * #4FD9BE*). L'argomento con cui allora era stata esclusa è qui sotto e **regge
             * ancora**: quello che è cambiato è la sua preferenza, e la scelta resta doppia
             * proprio per questo.
             * ⚠️ **Lo spessore vive su [lastFrame]**, insieme alle ragioni per cui è cambiato con
             * la `2.12`; il **colore** arriva da qui perché è un colore del tema, e un modificatore
             * che non è un composable non lo può leggere da sé.
             * ⚠️ Resta un riquadro fratello, come il nastro: due fratelli si dipingono
             * nell'ordine in cui sono scritti, e su questo non c'è niente da sapere.
             */
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .lastFrame(shape, MaterialTheme.colorScheme.primary)
            )
        }
        if (marked && mark == LastMark.CORNER) {
            /*
             * ⚠️⚠️ **UN NASTRO NELL'ANGOLO IN BASSO A SINISTRA dalla 0.58** (scelta
             * dell'utente fra cinque proposte, 2026-08-30). Prima era una cornice
             * tratteggiata, e il difetto non era il contrasto ma il **linguaggio**: una
             * cornice attorno a una miniatura è il gesto universale della **selezione**,
             * quindi da lontano quel segno diceva la cosa sbagliata. Un triangolo in un
             * angolo non somiglia a niente di tutto ciò.
             * ⚠️⚠️ **DALLA `2.11` È UNA DELLE DUE RISPOSTE E NON PIÙ L'UNICO SEGNO**, ed è
             * quella che trova chi aggiorna: il perché vive su [LastMark] e su `MarkMigration`.
             * ⚠️⚠️ **L'angolo è quello DIAGONALMENTE OPPOSTO alla spunta, ed è la ragione
             * per cui è in basso a sinistra e non altrove**: sulla piastrella che è insieme
             * vista e scelta i due segni stanno alla massima distanza possibile e non si
             * toccano mai. Chi lo spostasse 'per simmetria' rimetterebbe due segni nello
             * stesso angolo.
             * ⚠️ **Il ritaglio agli angoli arrotondati serve**: il triangolo tocca l'angolo
             * in basso a sinistra, che è tondo di `CORNER`, e senza `clip` la punta
             * sborderebbe oltre la sagoma della miniatura.
             * ⚠️ **Il costo, dichiarato**: un angolo di fotografia sparisce sotto il nastro,
             * e la forma triangolare di per sé non dice nulla, va imparata. Era il baratto
             * scritto accanto alla proposta, e l'utente l'ha scelta sapendolo.
             * ⚠️ Resta un riquadro fratello e non un `Modifier.border` nella catena, che è
             * la lezione della `0.34`: due fratelli si dipingono nell'ordine in cui sono
             * scritti, e su questo non c'è niente da sapere.
             */
            val tint = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .drawBehind {
                        val leg = size.minDimension * MARK_LEG
                        drawPath(
                            path = Path().apply {
                                moveTo(0f, size.height)
                                lineTo(leg, size.height)
                                lineTo(0f, size.height - leg)
                                close()
                            },
                            color = tint.copy(alpha = MARK_ALPHA)
                        )
                    }
            )
        }
        /*
         * ⚠️⚠️ **LA SPUNTA STA SU UN DISCO PIENO, e senza quello non si vedeva**: una
         * icona colorata appoggiata a una fotografia qualunque sparisce contro un
         * fondo dello stesso colore, ed è quello che l'utente ha segnalato. Il disco
         * dell'accento con il glifo del suo `onPrimary` porta con sé il proprio
         * contrasto, quindi si legge su qualunque cosa ci sia sotto.
         * ⚠️ Il glifo è un `Check` nudo e non un `CheckCircle`: il cerchio del secondo
         * sarebbe un contorno dentro un disco pieno, cioè due cerchi.
         * ⚠️ Sta sopra a tutto e in un angolo, non al centro: al centro coprirebbe
         * proprio la parte della fotografia che si sta guardando per decidere se
         * sceglierla.
         */
        // ⚠️ Dopo il velo e il nastro, così resta leggibile su una piastrella scelta: dentro
        // un `Box` si dipinge nell'ordine in cui si scrive, e il velo che schiarisce la
        // fotografia schiarirebbe anche la durata.
        if (Videos.isVideo(uri)) ClipBadge(uri, Modifier.align(Alignment.BottomEnd))
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
 * ⚠️⚠️ **È IL SOLO SEGNO CHE DICE 'QUESTO È UN VIDEO' dalla `0.83`**, e per questo compare
 * anche quando la durata non si sa: là resta il solo triangolo. Un fotogramma senza nessun
 * segno sopra è indistinguibile da una fotografia, e chi lo tocca si aspetta una foto.
 * ⚠️ **L'angolo è quello in basso a destra**, l'unico dei quattro rimasto libero: la spunta
 * della scelta sta in alto a destra, il nastro dell'ultima vista in basso a sinistra, e i tre
 * segni non si toccano mai nemmeno sulla piastrella che li porta tutti.
 * ⚠️ **Bianco su nero e non i colori del tema**, come il velo degli avvisi: questa targhetta
 * sta sopra un'immagine qualunque, non sopra una superficie del tema, quindi il contrasto
 * se lo deve portare da sé.
 * ⚠️ **La durata si chiede una volta per indirizzo** e la prima risposta è quella già in
 * memoria: senza, ogni miniatura che rientra in vista rifarebbe la domanda al MediaStore, e
 * la targhetta comparirebbe con un fotogramma di ritardo ogni volta. Stessa forma di
 * [GridName], stessa ragione.
 */
@Composable
private fun ClipBadge(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val length by produceState(Videos.cachedLength(uri), uri, context) {
        if (value == null) value = Videos.length(context, uri)
    }
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
            Text(
                text = Videos.stamp(it, floor = true),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

/**
 * Il nome del file sotto una miniatura, su **due righe al massimo**.
 *
 * ⚠️⚠️ **NASCE DALLA 0.82** (richiesta dell'utente, con le sue tre regole: *massimo 2 righe,
 * estensione mai spezzata, ellissi nel nome se serve*). Sono le stesse tre della pastiglia di
 * 'Info dettagliate sul file', e infatti a rispettarle è la stessa [fitName]: qui le righe
 * ammesse sono due invece di tre, ed è l'unica differenza.
 * ⚠️ **Il nome NON è nella lista che la griglia riceve**, che è fatta di soli indirizzi: si
 * chiede a [Names], che lo legge una volta sola e se lo ricorda. Il primo valore è quello già
 * in memoria, così una miniatura che rientra in vista non lampeggia senza nome.
 * ⚠️ **`labelSmall` e non `bodySmall`**: sotto una fotografia il nome è un'etichetta, e a
 * 11sp due righe stanno sotto una miniatura da 108dp senza rubarle spazio.
 */
/**
 * La superficie di un tasto a icona di Material, cioè il cerchio dell'increspatura.
 *
 * ⚠️ **Quaranta e non quarantotto**: 48 è il bersaglio del dito, che ci arriva da
 * `minimumInteractiveComponentSize`. Vedi la nota dentro [FilterKey], che è il solo posto in
 * cui questo tasto è scritto a mano invece di essere un `IconButton`.
 */
private val FILTER_KEY = 40.dp

/**
 * Il tasto del filtro volatile, in testata a destra quando non si sta scegliendo.
 *
 * ⚠️⚠️ **STA DOVE STAVA 'SELEZIONA TUTTO'** (richiesta dell'utente, 2026-08-31), cioè in un
 * posto che era rimasto vuoto nella `0.72`: l'angolo in alto a destra di una schermata di
 * contenuti è dove ci si aspetta di trovare un modo di restringere quello che si vede.
 * ⚠️⚠️ **IL TASTO PORTA L'ICONA DEL GENERE MOSTRATO, non un'icona di filtro 'accesa'**
 * (richiesta dell'utente, 2026-08-31): spento sono le tre righe che si accorciano, acceso è
 * la fotografia o la pellicola. Così il segno non dice soltanto *che* si sta filtrando, dice
 * *che cosa* si vede, che è l'informazione che serve a chi guarda una cartella dimezzata.
 * ⚠️ **Il tondo dietro è la seconda metà del segno e non un ornamento**: il glifo cambia
 * disegno, quindi da solo si potrebbe leggere come un tasto diverso; il tondo dice
 * 'quel tasto, adesso attivo' ed è la convenzione di Material per uno stato acceso. Il
 * filtro si azzera da sé cambiando cartella, ma dentro la stessa cartella resta, e senza un
 * segno una cartella con metà delle cose nascoste sembra una cartella che le ha perse.
 * ⚠️ **Il popup è di soli SIMBOLI, in fila** (stessa richiesta): pellicola, fotografia e la
 * croce che toglie il filtro. Tre voci con l'etichetta scritta sarebbero un menu, e questa è
 * una levetta che si tocca al volo mentre si guardano le miniature.
 * ⚠️⚠️ **`MediaKind.ALL` HA DUE DISEGNI, ed è voluto**: sul tasto è il filtro spento, quindi
 * le tre righe; nel popup è il comando che lo toglie, quindi una croce. Stessa scelta sotto,
 * due frasi diverse: 'non sto filtrando' e 'smetti di filtrare'. Un solo glifo per tutti e
 * due avrebbe detto la cosa sbagliata da una delle due parti.
 * ⚠️ **La croce non è un terzo genere ma un'AZIONE**, ed è la ragione per cui non si accende
 * mai: gli altri due portano il tondo quando sono quello in vigore, lei no.
 * ⚠️ **Le due icone sono quelle dei contatori sotto le copertine** (`Outlined.Image` e
 * `Outlined.Movie`): in questa app quei due glifi vogliono già dire 'fotografie' e
 * 'filmati', e un terzo disegno per la stessa cosa sarebbe una parola nuova per un concetto
 * vecchio.
 * ⚠️ **Lo stato lo annuncia `stateDescription` e non la descrizione dell'icona**: il tasto
 * *fa* sempre la stessa cosa (apre il filtro), e a cambiare è come sta. Mettere il genere
 * nella descrizione direbbe a chi legge con TalkBack che il tasto serve a mostrare i video.
 */
@Composable
private fun FilterKey(filter: MediaKind, onFilter: (MediaKind) -> Unit, onSearch: () -> Unit) {
    val menu = rememberMenuState()
    val res = LocalResources.current
    Box {
        /*
         * ⚠️⚠️ **NON È UN `IconButton`, ED È L'UNICA RAGIONE PER CUI È SCRITTO A MANO**: quello
         * di Material non espone il tocco lungo, e da qui ne parte uno (vedi sotto). Le due
         * misure sono le sue, prese dal suo sorgente: **40dp** di superficie, cioè il cerchio
         * dell'increspatura, e il bersaglio portato a 48 da `minimumInteractiveComponentSize`.
         * Scriverne una sola farebbe di questo l'unico tasto della testata con
         * un'increspatura di un'altra misura.
         *
         * ⚠️⚠️ **IL TOCCO LUNGO APRE LA RICERCA PER NOME, dalla 1.50** (richiesta dell'utente,
         * 2026-09-04: *voglio che un tocco lungo sull'icona del filtro (in alto a destra) apra
         * la ricerca dei file per nome (come FAB della home -> Cerca)*). È la **stessa**
         * ricerca, non una sua parente: chiama quello che chiama la voce 'Cerca' del menu
         * della schermata iniziale, quindi cerca in tutta la galleria e non nella sola
         * cartella aperta.
         * ⚠️ **L'etichetta del gesto è quella della voce del menu** e non una stringa nuova:
         * nomina la stessa azione, e un secondo testo per la stessa cosa sarebbe due testi da
         * tenere d'accordo in ventotto lingue.
         */
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(FILTER_KEY)
                .clip(CircleShape)
                .combinedClickable(
                    onClick = { menu.open() },
                    onLongClick = withHaptics(onSearch),
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
         * ⚠️⚠️ **PASSA DALLA SUPERFICIE UNICA DALLA `1.46`, e prima era un `DropdownMenu`**:
         * con lui questa era l'unica superficie dell'app senza velo, perché il velo se lo
         * deve chiedere e qui nessuno lo chiedeva. La `1.46` aveva prima aggiunto la riga a
         * mano; adesso il velo arriva perché la superficie è la stessa di tutti, e non c'è
         * più nessun elenco di chiamanti da tenere vero.
         * ⚠️ **Il difetto era nascosto da una frase falsa**, ed è la parte che vale: un
         * commento dava un altro menu per 'l'unico menu dell'app che non passa da
         * `MenuShell`', quindi chi cercava i chiamanti si fermava e li credeva tutti. Non si
         * vedeva perché il velo è spento di fabbrica dalla `1.39`.
         * ⚠️ **Il contenuto resta una fila di tre tasti**, e non diventa un elenco di voci:
         * una disposizione diversa non è un secondo modo di fare un menu.
         */
        MenuShell(
            state = menu,
            position = rememberMenuAtAnchor()
        ) {
            Row(modifier = Modifier.padding(horizontal = FILTER_PAD)) {
                // ⚠️ Pellicola, fotografia e croce, in quest'ordine: è quello chiesto, e non
                // l'ordine dell'enum, che comincia da 'tutto'. La croce sta in fondo perché
                // è l'unica che non sceglie niente.
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
 * Il tondo che sta dietro all'icona del filtro quando il filtro è in vigore.
 *
 * ⚠️ **Più piccolo del bersaglio del tocco**: il tasto resta 48dp perché un bersaglio più
 * stretto si manca, ma il tondo dipinto è [FILTER_MARK], così sta dentro la testata invece
 * di sembrare un secondo tasto attaccato agli altri.
 * ⚠️ **36dp e non 32, dalla 1.06** (riscontro dell'utente sul collaudo: *tondo più grande ma
 * proprio di un filo, poco di più*). Quattro punti sono il massimo che si può prendere
 * restando dentro il bersaglio da 48: da lì in su il tondo comincia a toccarne i bordi, e
 * torna a sembrare un tasto invece di un segno.
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
 * ⚠️⚠️ **SI CHIEDE AL MEDIASTORE E NON SI SOMMA A OCCHIO**: il peso di un file non sta
 * nell'indirizzo, quindi ogni cambio di selezione è una interrogazione. È la stessa
 * `factsOf` del dialogo delle informazioni, che quel conto lo sa già fare.
 * ⚠️⚠️ **L'ATTESA PRIMA DI CONTARE È LA COSA CHE RENDE LA FUNZIONE POSSIBILE**: scegliendo
 * col trascinamento la selezione cambia decine di volte al secondo, e senza questa pausa
 * partirebbe una interrogazione per ogni fotografia sfiorata. `produceState` annulla la
 * precedente a ogni cambio, quindi durante il trascinamento non ne parte nessuna e il conto
 * si fa quando il dito si ferma.
 * ⚠️ **Vuoto e non uno zero mentre conta**: uno zero è un peso, e per un istante direbbe
 * una cosa falsa. Lo spazio vuoto si legge come 'sto arrivando'.
 */
@Composable
private fun PickWeight(chosen: Set<Uri>) {
    val context = LocalContext.current
    val weight by produceState<Long?>(null, chosen, context) {
        value = null
        delay(WEIGH_WAIT)
        // ⚠️ **La sola somma e non tutti i dati, dalla `1.81`**: il perché è su [weightOf].
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

@Composable
private fun GridName(uri: Uri, room: Int) {
    val context = LocalContext.current
    val name by produceState(Names.cached(uri), uri, context) {
        if (value == null) value = Names.of(context, uri)
    }
    val style = MaterialTheme.typography.labelSmall
    val measurer = rememberTextMeasurer()
    // ⚠️⚠️ **IL MARGINE SI TOGLIE DALLA MISURA, e senza questo il nome sforerebbe**: il
    // numero che arriva è la larghezza della **cella**, e il testo ne ha due dp in meno per
    // lato. Misurando sulla cella intera, `fitName` crederebbe di avere quattro dp che il
    // layout poi non gli dà, e l'ultima lettera finirebbe tagliata.
    // ⚠️ Il margine c'è perché il distacco fra le celle è 3dp: due nomi lunghi in due celle
    // vicine si toccherebbero quasi.
    val bordo = with(LocalDensity.current) { NAME_PAD.roundToPx() }
    val utile = room - bordo * 2
    val shown = remember(name, utile, style, measurer) {
        name?.let { fitName(it, utile, NAME_LINES, style, measurer) }
    } ?: return
    Text(
        text = shown.text,
        // ⚠️ Lo stile è quello a cui il nome è stato MISURATO, dalla 1.62: `fitName` può
        // stringere il corpo o la spaziatura per far stare il nome intero, e scriverlo alla
        // misura piena rimetterebbe lo sforo che quella stretta ha appena tolto.
        style = shown.style,
        maxLines = NAME_LINES,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = NAME_PAD)
    )
}

/**
 * Quanto si aspetta prima di contare il peso della selezione.
 *
 * ⚠️ Un terzo di secondo: abbastanza perché un trascinamento non faccia partire niente,
 * poco perché un tocco singolo non sembri lento. Da rivedere col dito, come ogni numero
 * che dipende da un gesto.
 */
private const val WEIGH_WAIT = 300L

/**
 * Quante colonne stanno davvero in scena: [scelte] sul lato corto, di più se lo schermo è largo.
 *
 * ⚠️⚠️ **SOSTITUISCE `GridCells.Adaptive` DALLA `1.66`, E NE CONSERVA IL PREGIO.** Quella dava
 * tre colonne su un telefono e sei ruotandolo, senza un ramo per ogni forma di schermo, ma
 * decideva **lei** quanto sono grandi le miniature, e dalla `1.66` quel numero è una scelta
 * dell'utente. Qui la scelta dice quante colonne stanno sul **lato corto**, e ruotando o su uno
 * schermo più largo ne entrano altrettante della stessa misura: il rapporto fra i due lati fa il
 * conto, quindi non c'è nessuna larghezza di riferimento scritta a mano da rifare il giorno che
 * esce un telefono di un'altra forma.
 * ⚠️ **Non scende mai sotto la scelta**: in verticale il rapporto vale 1 e il conto la restituisce
 * intera, e su una finestra più stretta dell'alta il tetto la protegge lo stesso.
 * ⚠️ **Con una finestra ancora da misurare vale la scelta**: al primo fotogramma la misura può
 * essere zero, e una divisione per zero darebbe una griglia a una colonna che poi salta.
 *
 * ⚠️⚠️ **NON È PIÙ PRIVATA DALLA `1.81`, PERCHÉ LA VOCE È UNA E DEVE VALERE IN TUTTE E DUE LE
 * GRIGLIE** (censimento della UI del 2026-09-05). `Settings.folderColumns` governa la griglia
 * delle immagini **e** quella delle copertine dalla `1.66`, ma questo conto lo faceva solo la
 * prima: ruotando il telefono le miniature restavano della loro misura e le copertine si
 * allargavano fino a diventare enormi, cioè la stessa scelta dava due comportamenti. È la
 * regola che l'utente ha dettato sul tocco lungo del filtro: *due controlli identici devono
 * comportarsi allo stesso modo*.
 */
internal fun spread(scelte: Int, finestra: WindowInfo): Int {
    val misura = finestra.containerSize
    val corto = minOf(misura.width, misura.height)
    if (corto <= 0) return scelte
    return (scelte.toFloat() * misura.width / corto).roundToInt().coerceAtLeast(scelte)
}

/*
 * ⚠️ **`THUMB` NON C'È PIÙ, DALLA `1.78`, e con lei la sua KDoc**: era il lato minimo che
 * `GridCells.Adaptive` riceveva, e dalla `1.66` le colonne le conta [spread] da una scelta
 * dell'utente. La costante era rimasta senza lettori e la sua KDoc era finita orfana sopra
 * quella di [spread], cioè documentava la funzione sbagliata.
 */

/** Il distacco fra le miniature: c'è, ma non deve leggersi come una cornice. */
private val GAP = 3.dp

/**
 * Quante righe può prendere il nome sotto una miniatura: **due**, come chiesto.
 *
 * ⚠️ Due e non tre come nella pastiglia di 'Info': là il nome è il soggetto del dialogo, qui
 * è una didascalia sotto una fotografia, e una terza riga la farebbe diventare il soggetto
 * della cella.
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

/** Il nero della targhetta, semitrasparente perché la fotografia si intraveda sotto. */
private val BADGE_INK = Color(0x99000000)

/** Il respiro ai lati del testo dentro la targhetta. */
private val BADGE_PAD = 4.dp

/** Il respiro sopra e sotto, e il distacco fra triangolo e cifre: sono la stessa misura. */
private val BADGE_LIP = 2.dp

/** Il triangolo: della misura del testo che gli sta accanto, non di più. */
private val BADGE_GLYPH = 12.dp

/**
 * Quanto è lungo il cateto del nastro dell'ultima foto vista, in frazione del lato.
 *
 * ⚠️ È una **frazione** e non una misura in dp, al contrario di tutto il resto qui sotto,
 * e la ragione è che la piastrella non ha una misura fissa: le colonne sono `Adaptive`,
 * quindi su un tablet o in orizzontale la miniatura cresce. Un cateto in dp resterebbe
 * quello di un telefono e su uno schermo grande diventerebbe un francobollo nell'angolo.
 * ⚠️ Il valore veniva dal mockup su cui l'utente ha scelto: `0,44`, cioè a 108dp poco meno
 * di 48dp di cateto.
 * ⚠️⚠️ **DALLA `2.03` È `0,36`, E LO HA CHIESTO LUI DOPO AVERLO VISTO SUL TELEFONO**
 * (*rimpicciolisci un po' l'angolo colorato che indica l'ultimo elemento visualizzato*). Il
 * cateto scende da 48dp a 39dp sulla stessa piastrella, cioè il triangolo copre un terzo di
 * fotografia in meno: è il baratto scritto sul nastro, e adesso pende dall'altra parte.
 * ⚠️ **Il mockup non decide più**, quindi chi lo riaprisse non ci trova questo numero: là il
 * segno era disegnato e non provato, e questo viene dall'app in mano.
 */
private const val MARK_LEG = 0.36f

/**
 * Quanto è OPACO il nastro dell'ultima foto vista.
 *
 * ⚠️ 85%, scelta dell'utente (2026-08-31). Il nastro è pieno del colore d'accento e sta
 * sopra la fotografia: a opacità piena la copre, e un segno che copre quello che segnala
 * lavora contro sé stesso. Un filo di trasparenza lascia intravedere l'angolo della foto e
 * dice 'questa' senza cancellarne un pezzo.
 * ⚠️ **Non è la stessa cosa di [PICKED_VEIL]**, che agisce sull'intera miniatura e la
 * schiarisce: questo è l'opacità di un singolo triangolo dipinto sopra.
 */
private const val MARK_ALPHA = 0.85f

/**
 * Quanto è spessa la **cornice** dell'ultimo media visualizzato, in frazione del lato.
 *
 * ⚠️⚠️ **IL NUMERO È SUO E ARRIVA DOPO AVERLA VISTA** (riscontro del giro della `2.11`, voce
 * `ind-ultimo` non approvata: *se lo spessore viene dal mio mockup, ho sbagliato io: serve più
 * spesso (5%*). Il `2,7%` di prima era misurato sul suo mockup, cioè sul disegno e non sull'app in
 * mano: quello che il mockup non diceva è quanto di quel tratto si perde su una fotografia.
 * ⚠️⚠️ **E CAMBIA L'UNITÀ, NON SOLO IL NUMERO: ADESSO È UNA FRAZIONE COME [MARK_LEG].** La nota di
 * prima diceva che un tratto ha lo stesso spessore ovunque, come il filetto sotto una copertina e
 * il bordo dei pannelli: vale per il bordo di una **superficie dell'app**, che è sempre la stessa,
 * e non per un segno posato su una piastrella le cui colonne le sceglie lui. Fra due e cinque
 * colonne il lato della cella quasi si triplica, quindi lo stesso numero in punti darebbe un segno
 * che pesa il triplo da una parte e un terzo dall'altra. In frazione pesa uguale dappertutto, che
 * è il modo in cui lui ha scritto la richiesta.
 */
private const val MARK_EDGE = 0.05f

/**
 * Quanto è OPACA la cornice: l'80%, ed è suo (riscontro del giro della `2.11`).
 *
 * ⚠️ Non è la stessa cosa di [MARK_ALPHA], che vale per il nastro: là il triangolo copre un angolo
 * di immagine e la trasparenza serve a lasciarlo intravedere, qui il tratto corre sul bordo e la
 * trasparenza lo ammorbidisce contro quello che ha sotto.
 * ⚠️ **Resta invariata con la `2.13`**, che ha cambiato il solo colore: i due numeri del tratto
 * sono quelli che ha dettato lui guardando l'app, e il colore era la terza cosa della stessa riga.
 * ⚠️ **È `internal` perché il banco calcola da lei il colore atteso** invece di riscriverlo:
 * un numero copiato in `CorniceTest` sarebbe una seconda fonte, e le due divergerebbero al primo
 * ritocco dell'opacità.
 */
internal const val MARK_FRAME_ALPHA = 0.8f

/**
 * Il tratto che segna l'ultimo media visualizzato, dipinto in [color] **dentro** la sagoma [shape].
 *
 * ⚠️⚠️ **DALLA `2.13` IL COLORE È L'ACCENTO DELL'APP, ED È SUA RICHIESTA** (riscontro del giro
 * della `2.12`, voce `ind-cornice` approvata con una nota: *forse con questo spessore sarebbe
 * visibile anche nel colore d'accento. Proviamo*). La `2.12` lo aveva portato sull'arancione degli
 * onboarding perché a `2,7%` il verde acqua *non era abbastanza vivido*, e il 5% ha tolto proprio
 * quella causa: un tratto spesso ha l'area per farsi vedere anche in un colore di casa.
 * ⚠️⚠️ **QUINDI CADE LA NOTA CHE LO ESCLUDEVA** (*non è `colorScheme.primary`, che resta il colore
 * del nastro: i due segni devono distinguersi a colpo d'occhio*), e cade perché guardava dalla
 * parte sbagliata: i due segni **non si vedono mai insieme**, sono le due risposte dello stesso
 * interruttore. Che siano dello stesso colore dice il vero, cioè che sono due forme di una cosa
 * sola.
 * ⚠️ **Con lei l'arancione [HINT_MARK] torna a dire una cosa sola**, l'evidenziatore dei mini
 * onboarding, che è l'unico posto in cui la tavolozza dell'app si rompe apposta.
 * ⚠️ **Il colore arriva da fuori perché questo non è un composable**, quindi non può leggere il
 * tema; il chiamante passa `MaterialTheme.colorScheme.primary`, com'è per il nastro. È anche la
 * cosa che il banco può misurare (`CorniceTest`): che il tratto prenda il colore ricevuto e la
 * sua opacità, invece di un numero scritto qui dentro.
 * ⚠️⚠️ **LO STROKE SI DISEGNA DOPPIO E POI SI RITAGLIA, e non è un trucco di comodo**: un tratto
 * è centrato sul contorno, quindi metà cadrebbe **fuori** dalla miniatura; disegnandolo di
 * `2 * spessore` dentro un ritaglio della sagoma, la metà di fuori sparisce e quella di dentro
 * vale esattamente lo spessore voluto, col bordo esterno che coincide col bordo della miniatura.
 * È quello che faceva `border`, e serviva rifarlo a mano perché lo spessore adesso è una frazione
 * della misura, che un modificatore di bordo non può leggere.
 * ⚠️ **Vive fuori dal composable perché il banco lo monta da solo**: una miniatura vuole un
 * MediaStore con dentro delle immagini, e in Robolectric è vuoto, quindi la sola cosa misurabile
 * è il meccanismo su una scena minima (`CorniceTest`).
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
 * Quanto si SCHIARISCE una miniatura scelta.
 *
 * ⚠️ Serve ad accompagnare la spunta, non a segnalare da solo: su una fotografia già
 * chiara un velo chiaro non si nota, ed è la ragione per cui il segno vero è il disco.
 * ⚠️ **Schiarisce e non scurisce dalla 0.53**, per scelta dell'utente: una foto scelta
 * deve venire avanti, non mettersi da parte.
 */
private const val PICKED_VEIL = 0.34f

/** Il lato del disco della spunta. Cresciuto nella 0.53, perché non si vedeva abbastanza. */
private val TICK = 28.dp

/** Il raggio degli angoli di una piastrella, in un posto solo perché lo usano in due. */
private val CORNER = 4.dp

/**
 * Quanto è alta la fascia, in cima e in fondo, dentro la quale un dito che trascina fa
 * scorrere la griglia da solo.
 *
 * ⚠️ Larga quanto **mezza piastrella**: più stretta e la si manca, più larga e si comincia
 * a scorrere mentre si sta ancora scegliendo in mezzo allo schermo.
 */
/*
 * ⚠️ **Non sono più private dalla `1.81`**: le legge anche il riordino a trascinamento
 * (`Reorder.kt`), che ha lo stesso problema e lo risolve con lo stesso schema. Due numeri
 * scritti due volte per la stessa banda darebbero due velocità diverse al primo ritocco.
 */
internal val EDGE_BAND = 56.dp

/** Quanti pixel al fotogramma, al massimo, cioè col dito sul bordo estremo. */
internal val EDGE_SPEED = 14.dp


/** Tutti i riquadri sono la stessa cosa, e dirlo permette a Compose di riusarli. */
private const val THUMB_KIND = "thumb"

/**
 * I rientri della schermata: quanto sta il contenuto dai bordi dell'area sicura.
 *
 * ⚠️⚠️ **ERANO SCRITTI IN TRE POSTI E DALLA `1.83` SONO DUE COSTANTI**: la colonna della
 * schermata, il rientro con cui il velo dell'onboarding illumina il FAB, e il modificatore
 * del FAB stesso. Adesso ne serve un quarto, la tinta dell'intestazione, che deve **uscire**
 * di esattamente quel tanto per arrivare ai bordi dello schermo: con i numeri copiati, il giorno
 * che uno cambia la tinta lascerebbe una striscia chiara sui fianchi.
 */
// ⚠️ **Non è privato dalla `1.91`**: da lui si ricavano [MENU_INSET] e la larghezza minima di un
// menu ancorato (`menuFloor`), cioè due misure che parlano della griglia da fuori. Ricopiare l'8
// là dentro sarebbe la coincidenza che si rompe al primo ritocco di questo margine.
internal val GRID_PAD_X = 8.dp
private val GRID_PAD_Y = 12.dp

/**
 * Una pastiglia dell'intestazione: il peso della cartella, quante immagini, quanti video, o il
 * comando che seleziona tutto.
 *
 * ⚠️⚠️ **UNA SOLA DALLA `1.85`, ED È UNA SUA CORREZIONE** (riscontro del giro della `1.83`:
 * *tutte dello stesso colore neutro, senza distinzione tra info e selezione*). Fino alla `1.84`
 * ce n'erano due, e la seconda aveva il fondo pieno e scuro perché il mockup della variante 10
 * dichiarava che *un dato e un comando che si somigliano sono la trappola vera di una fila
 * mista*: col telefono in mano ha deciso il contrario. Con l'unificazione il vestito non può più
 * divergere fra le quattro.
 * ⚠️ **Il vestito è quello della variante 4 del mockup**: contorno, fondo della superficie,
 * inchiostro smorzato.
 *
 * ⚠️⚠️ **DALLA `1.86` OGNI PASTIGLIA HA TUTTI E DUE I GESTI, e la `1.82` diceva il contrario**
 * (*il tocco lungo è una scorciatoia e sul dato il tocco breve non fa niente*, punto A del campo
 * libero di quel giro): la ragione di allora era che *un dato che al primo tocco seleziona
 * duecento file sarebbe una sorpresa*, e a spenderla è lui col telefono in mano, che nel giro
 * della `1.85` ha ridettato le quattro coppie. Il verso resta leggibile perché è **uno solo**: il
 * tocco aggiunge quello che la pastiglia nomina, il tocco lungo lo toglie.
 * ⚠️ **La vibrazione resta sul solo tocco lungo**, ed è il segno che distingue i due gesti: un
 * tocco che seleziona si vede da sé, uno che toglie arriva dopo mezzo secondo di attesa.
 * ⚠️⚠️ **E TUTTI E DUE I GESTI ESISTONO PER CHI NON VEDE LA PASTIGLIA**: `onClick` e
 * `onLongClick` semantici portano la loro etichetta, quindi un lettore di schermo li annuncia e
 * li può eseguire. Senza il primo, il tocco arriverebbe dal solo `pointerInput`, che nell'albero
 * semantico non compare: sarebbe una funzione riservata a chi la vede.
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
     * ⚠️⚠️ **LE DUE AZIONI SI LEGGONO DA UNO STATO AGGIORNATO, e senza questo il comando farebbe
     * la cosa di prima**: `pointerInput` cattura le sue lambda quando parte, e con le lambda come
     * chiavi ripartirebbe a ogni ricomposizione. Qui il testo e l'azione di [FrontPick] cambiano
     * insieme alla selezione, quindi un gestore catturato al primo giro scarterebbe la selezione
     * anche dopo che la pastiglia ha ricominciato a dire 'Seleziona tutto'.
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
 * ⚠️⚠️ **IL TESTO CAMBIA COL SECONDO TOCCO, DALLA `1.85`, ED È SUO** (riscontro del giro della
 * `1.83`: *dopo il tocco su 'Seleziona tutto' il tasto deve cambiare testo in 'Deseleziona', con
 * le logiche anti-jitter; un secondo tocco scarta la selezione (anche se nel frattempo è
 * cambiata)*).
 * ⚠️ **'Anche se nel frattempo è cambiata' viene da sé**: il comando scarta **tutto** quello che
 * è selezionato in quel momento, e la parola che porta dipende dallo stato di adesso, non da che
 * cosa ha fatto il tocco di prima.
 * ⚠️⚠️ **L'ANTI-JITTER SI MISURA, invece di riservare lo spazio con una copia nascosta**: le due
 * parole sono per forza di lunghezza diversa (qui la causa non si può togliere, come si era
 * potuto nel documento di feedback), quindi la pastiglia si tiene larga quanto la più lunga delle
 * due e non si muove al cambio. Una seconda `Text` trasparente sotto avrebbe messo lo stesso
 * testo in due posti e una voce in più nell'albero semantico.
 *
 * ⚠️⚠️ **IL TOCCO LUNGO AZZERA SEMPRE, DALLA `1.86`** (*'Seleziona tutto' al tocco = seleziona
 * tutto; tocco prolungato = azzera la selezione*), e serve con una selezione **parziale**: là il
 * tocco la completa, e senza il secondo gesto per svuotarla bisognerebbe prima riempirla.
 * ⚠️⚠️ **MA IL TOCCO RESTA UN INTERRUTTORE, e la scelta va dichiarata perché la sua frase si
 * può leggere anche alla lettera** (*al tocco = seleziona tutto*, sempre e comunque): con tutto
 * già selezionato la pastiglia **dice** 'Deseleziona', e un comando che dice una parola e ne fa
 * un'altra è peggio di un gesto in meno. La voce `int-scarta` di quel cambio di testo è
 * approvata, quindi qui il testo comanda: la pastiglia fa quello che c'è scritto sopra.
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
 * La comparsa del FAB del cestino.
 *
 * ⚠️⚠️ **IL FAB ENTRA CON UN'ANIMAZIONE dalla 0.67** (richiesta dell'utente: *voglio che quel
 * FAB appaia con un'animazione*). Prima compariva di scatto, e su un tasto che segnala un
 * **cambio di modo** è l'occasione sprecata: il movimento è la cosa che dice 'adesso c'è
 * qualcosa da fare', e senza di lui sembra essere sempre stato lì.
 * ⚠️⚠️ **FINO ALLA `1.78` QUESTA NOTA PARLAVA DEL FAB DELLA SELEZIONE, che non esiste dalla
 * `0.94`**: con una selezione in corso le operazioni stanno nella scheda in fondo, e il suo
 * unico chiamante lo dichiara escludendo la selezione. La frase gemella nell'altro file l'ha
 * già corretta l'utente in persona, e quella correzione racconta che la stessa frase falsa era
 * finita anche in una voce di collaudo: questa era la seconda copia.
 * ⚠️ **Cresce dal proprio centro con una molla appena elastica, ma esce secco**: una cosa
 * che arriva può permettersi di farsi notare, una che se ne va no, e un rimbalzo in uscita
 * trattiene lo sguardo su un angolo che si sta svuotando.
 * ⚠️ **Non serve al FAB della schermata iniziale, che è sempre in scena** e dalla `1.75` non ha
 * più nessuna entrata da animare (riscontro suo, giro della `1.74`: *animazione all'ingresso: se
 * ne va. Preferisco semplificare*). Quindi questo è l'unico meccanismo del genere in casa.
 * ⚠️⚠️ **Sta in una funzione a sé, e non è per eleganza**: chiamata sul posto,
 * `AnimatedVisibility` finisce sull'overload di `ColumnScope`, perché quel `Box` vive
 * dentro la `Column` della schermata, e il compilatore la rifiuta. Qui dentro di
 * `ColumnScope` non c'è traccia, quindi si risolve quella giusta. È anche il posto in cui
 * la specifica dell'animazione ha un nome invece di essere venti righe in mezzo al
 * riquadro.
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
 * Il menu del cestino: **sul lato del FAB**, sopra di lui.
 *
 * ⚠️⚠️ **STAVA AL CENTRO FINO ALLA `1.53`, E ADESSO STA DOVE STA QUELLO DELLA SCHERMATA
 * INIZIALE** (riscontro dell'utente, giro della `1.53`, voce `sfocatura-segue`: *i due pannelli
 * che si aprono alla pressione sul FAB hanno due funzionamenti diversi e non capisco perché. Il
 * pannello della schermata home a questo punto è eccellente, e mi va bene che stia sul lato del
 * FAB. Ma perché quella del cestino non fa altrettanto?*). Adesso i due chiedono lo **stesso**
 * posizionatore, con gli stessi due lati e senza distacco, quindi non possono più comportarsi
 * in due modi.
 * ⚠️⚠️ **E TOGLIE LA RAGIONE PER CUI IL DIFETTO DELLA SFOCATURA SI VEDEVA PROPRIO LÌ**: un
 * pannello centrato ha lo sfondo dai **due** lati, sopra le miniature, mentre appoggiato al
 * bordo ne ha uno solo. Non è la cura di quel difetto, ma è la ragione per cui il cestino era
 * il posto in cui si notava di più.
 * ⚠️ **Il centro veniva dalla `0.75`** (richiesta di allora: *al centro in basso, con angoli un
 * po' più stondati*), quando questo menu serviva alla **selezione** e non al cestino: la
 * selezione da tempo ha la sua scheda in fondo, e quella richiesta è stata sostituita da questa.
 *
 * ⚠️⚠️ **NON È UN `DropdownMenu`, dalla 0.75**. Un `DropdownMenu` si posiziona **accanto al suo
 * genitore** e non accetta un posizionatore: attaccato a un FAB in basso a destra,
 * usciva da quell'angolo. La superficie e il posizionatore stanno in [MenuShell] e `MenuSpot`,
 * condivisi dalla `1.46` con **ogni** menu dell'app.
 * ⚠️⚠️ **DALLA 1.06 SI CHIUDE TOCCANDO FUORI**, che fino alla `1.05` era spento apposta
 * perché il FAB lo **alternava** e le due cose si pestavano (il perché vive in
 * [MenuShell], dove fino alla `1.46` era un parametro). Adesso il FAB si limita ad
 * aprire, e a chiudere ci pensa
 * il velo trasparente della schermata: nessuno dei due può più riaprire quello che l'altro
 * ha appena chiuso. Questo resta acceso per il caso che il velo non copre, cioè un tocco
 * fuori dalla finestra dell'app.
 */
@Composable
private fun PickMenu(menu: MenuState, columns: Int, content: @Composable () -> Unit) {
    MenuShell(
        // ⚠️ La stessa coppia della schermata iniziale, e non una che le somiglia: allineato al
        // FAB in orizzontale, e sopra di lui perché sotto non ci sta. Scriverla uguale è
        // quello che rende impossibile che i due menu si comportino in modo diverso.
        state = menu,
        position = rememberMenuAtAnchor(),
        /*
         * ⚠️ **Il gap è quello di QUESTA griglia**, che è più stretto di quello delle cartelle:
         * la misura si prende dalla griglia che il menu copre, non da una qualunque. È il caso
         * del suo mockup, dove le colonne sono tre e il tetto non interviene.
         */
        minWidth = menuFloor(spread(columns, LocalWindowInfo.current), GAP),
        content = content
    )
}

/*
 * ⚠️ **Lo stondamento non vive più qui, dalla 1.28**: era 16 mentre gli altri due menu
 * erano a 8, e adesso è `MENU_ROUND` in `Menus.kt`, uno per tutti. La nota vecchia
 * spiegava perché questo ne volesse più degli altri (quasi quadrato contro lista larga):
 * l'argomento era buono e la conclusione sbagliata, perché uno stondamento dice che cosa
 * è una superficie, non quanto è larga.
 */

/**
 * Il tondo che segna il filtro in vigore, dietro alla sua icona.
 *
 * ⚠️ **32 e non 48**: il tasto resta il bersaglio da 48 che Material chiede, ma il tondo
 * dipinto è più stretto dell'icona più il suo respiro. A 48 toccherebbe i vicini e la
 * testata sembrerebbe avere un tasto in più.
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
 * ⚠️ 0,62 e non 0: partendo da zero il FAB sembra **sbucare** da un punto, e con una
 * molla elastica diventa un rimbalzo da cartone animato. Partendo da due terzi il gesto si
 * legge come 'era lì e si è fatto avanti'.
 */
private const val FAB_SMALL = 0.62f

/** La dissolvenza in entrata: più corta della molla, così il colore c'è già mentre cresce. */
private const val FAB_IN = 90

/** L'uscita, in millisecondi: secca, e più breve dell'entrata. */
private const val FAB_OUT = 110


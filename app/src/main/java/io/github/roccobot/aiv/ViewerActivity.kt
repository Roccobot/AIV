package io.github.roccobot.aiv

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import android.app.Application
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.setSingletonImageLoaderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface ViewerState {
    /**
     * Si sta aprendo qualcosa, e [progress] dice a che punto è il trasferimento, quando lo si
     * può sapere.
     *
     * ⚠️ **Il numero vive nello stato**: un campo a parte del modello andrebbe azzerato a mano,
     * e un'apertura nuova potrebbe mostrare l'avanzamento di quella prima.
     * `null` vuol dire 'non lo so', non zero (un file locale, un server che non dichiara la
     * lunghezza): chi legge mostra il giro indeterminato.
     */
    data class Loading(val progress: Float? = null) : ViewerState
    data class Ready(val image: LoadedImage) : ViewerState

    /**
     * Un video, che non si decodifica come un'immagine.
     *
     * ⚠️ **È uno stato a sé e non un caso di [Ready]**: `Ready` contiene una `LoadedImage`
     * (pixel, proporzioni, formato, profondità di colore), e un video passerebbe per zoom,
     * tavolozza e informazioni che gli chiedono cose che non ha.
     * Ha il proprio indirizzo, anche se il modello ne ha uno in `source`: chi disegna lo stato
     * chiede miniatura e durata senza incrociare due campi di due momenti diversi.
     */
    data class Clip(val uri: Uri, val from: Arrival) : ViewerState

    data class Error(@param:StringRes val messageRes: Int, val detail: String?) : ViewerState
}

/**
 * Come si è arrivati su quello che si sta guardando.
 *
 * ⚠️ **Vive nello stato**: fra il dito e il lettore ci sono funzioni che ricevono solo un
 * indirizzo (`showAt`, `startLoad`, `ClipStage`), e in fondo alla catena l'informazione non
 * arriverebbe.
 * Tre valori, tre provenienze. È un fatto e non una decisione: la regola è solo in [plays].
 */
enum class Arrival {
    /**
     * Qualcuno ha toccato questa cosa: una miniatura della griglia, dei risultati o del
     * cestino, o una riga di 'Cartelle di sistema'.
     */
    TAPPED,

    /**
     * È arrivato da sé: la strisciata, o la rilettura della cartella dopo uno spostamento, una
     * rinomina o un'eliminazione. Nessuno ha chiesto questo file.
     */
    LEAFED,

    /**
     * Un indirizzo consegnato da fuori: un collegamento, una condivisione, gli appunti, il
     * selettore di sistema, l'indirizzo digitato.
     *
     * ⚠️ **Oggi un video non arriva mai da qui**: il manifesto dichiara solo `image`, il
     * selettore chiede `PickVisualMedia.ImageOnly` e gli appunti ammettono solo `http(s)`. Se si
     * aprisse ai video, la risposta di [plays] va decisa.
     */
    HANDED;

    /**
     * Se arrivando così un video parte da sé, con `Settings.clipAutoplay` a quel valore.
     *
     * ⚠️ **La regola è solo qui** (richiesta dell'utente): al tocco su una miniatura il video
     * parte, sfogliando resta col tasto 'Play' in sovrimpressione anche con l'interruttore
     * acceso. [HANDED] resta fermo finché non lo si decide.
     */
    fun plays(autoplay: Boolean): Boolean = autoplay && this == TAPPED
}

/** Quale schermata è davanti. */
sealed interface Screen {
    /** Le impostazioni. */
    data object Settings : Screen

    data object Viewer : Screen

    /**
     * L'elenco delle cartelle, la casa dell'app (decisione dell'utente). Le vie che apriva la
     * vecchia schermata iniziale sono nel FAB di `FolderScreen`.
     * [forStart] distingue le due domande a cui la schermata risponde: quale cartella aprire
     * adesso e quale all'avvio. È qui e non nel modello perché se ne va con la schermata.
     */
    data class Folders(val forStart: Boolean) : Screen

    /**
     * Una cartella in miniature. Contiene anche il nome, che l'elenco di partenza conosce già
     * (ricavarlo rileggerebbe il MediaStore), e la cartella è la destinazione di Indietro dal
     * visualizzatore.
     */
    data class Grid(val bucket: Long, val name: String) : Screen

    /**
     * I risultati di una ricerca, una griglia come le altre.
     *
     * ⚠️ **Non contiene il testo cercato**: cambia a ogni lettera, e la schermata si ricostruirebbe
     * a ogni tasto. Il testo vive nel modello.
     * Il confine ([bucket]) invece fa parte dell'identità: 'Cerca' dal menu del FAB è limitato
     * alla cartella corrente (risposta dell'utente a `d-fab-voci`), e cambiarlo è un'altra
     * ricerca. `null` è la ricerca in tutta la galleria. Il nome serve solo a scriverlo nel
     * campo, senza una stringa nuova.
     */
    data class Search(val bucket: Long? = null, val name: String? = null) : Screen

    /**
     * Il cestino.
     *
     * ⚠️ **Non è una `Grid`**: i file del cestino vivono nella cartella dell'app, dove il
     * MediaStore non guarda, e non hanno un album. Si elencano leggendo il disco (`Bin.list`).
     * Non contiene dati, come [Search]: il contenuto lo dice il disco.
     */
    data object Bin : Screen

    /** La cronologia dei ripristini: si apre solo dal cestino e ci torna. */
    data object History : Screen

    /**
     * L'editor semplice: si ritaglia e si gira. Contiene il nome del file come [Grid], che serve
     * in testata e al salvataggio. Si apre solo dal menu del tocco lungo, quindi Indietro torna
     * sempre al visualizzatore.
     */
    data class Editor(val uri: Uri, val name: String) : Screen

    /**
     * L'editor completo, quello che cambia i pixel: una schermata a sé, per scelta dell'utente,
     * con comandi, salvataggio e perimetro suoi. Il nome serve al salvataggio.
     */
    data class FullEditor(val uri: Uri, val name: String) : Screen
}

/**
 * La chiave con cui questa schermata ritrova quello che aveva da parte: vedi il
 * `SaveableStateProvider` in `AivApp`.
 *
 * ⚠️ **Una stringa e non la schermata**: la chiave finisce in un `Bundle`, che non accetta un
 * `Uri` o un oggetto nostro.
 * ⚠️ **Contiene solo quello che fa identità**: una cartella rinominata resta la stessa, e col nome
 * nella chiave si ritroverebbe in cima. `forStart` invece fa identità.
 */
private fun Screen.saveKey(): String = when (this) {
    is Screen.Folders -> "folders:$forStart"
    is Screen.Grid -> "grid:$bucket"
    is Screen.Search -> "search:${bucket ?: 0L}"
    Screen.Bin -> "bin"
    Screen.History -> "history"
    Screen.Settings -> "settings"
    Screen.Viewer -> "viewer"
    is Screen.Editor -> "editor"
    is Screen.FullEditor -> "editor-pieno"
}

/**
 * Se questa schermata è una griglia di miniature, cioè una delle tre che `reloadGrid`
 * sa rileggere.
 *
 * La cartella, i risultati di una ricerca e il cestino hanno lo stesso aspetto e la stessa
 * selezione: la domanda si fa in più punti, e scritta a mano ne dimenticherebbe una.
 */
private fun Screen.isGrid(): Boolean =
    this is Screen.Grid || this is Screen.Search || this == Screen.Bin

/**
 * Se in questa schermata il FAB può esserci: la casa e le tre griglie.
 *
 * Serve a farlo uscire rimpicciolendosi verso una schermata senza FAB (vedi [LocalSenzaFab]).
 * Dice 'può', non 'ce l'ha': un falso positivo non costa niente.
 */
private fun Screen.hasFab(): Boolean = this is Screen.Folders || isGrid()

/**
 * Se questa schermata, andando verso [verso], è quella che se ne va e là il FAB non c'è.
 *
 * ⚠️ **È una funzione sua perché il banco misuri questa**, come [cambioSchermata]: un `!` di
 * troppo darebbe un FAB che sparisce senza nessun errore.
 * ⚠️ **Il primo termine dice chi sono io**: nella transizione la funzione gira per il contenuto
 * che esce e per quello che entra, e solo il primo ha un FAB da congedare.
 */
internal fun Screen.senzaFabVerso(verso: Screen): Boolean = this != verso && !verso.hasFab()

/** La casa dell'app: un nome solo per tutti i posti che ci tornano. */
private val HOME = Screen.Folders(forStart = false)

/**
 * Quanto si aspetta, dopo l'ultimo tasto, prima di interrogare il MediaStore: sotto i due decimi
 * la pausa non raggruppa le lettere, sopra il mezzo secondo si sente il ritardo.
 */
private const val SEARCH_PAUSE_MS = 280L

/**
 * Quanto si aspetta, dopo l'ultima notizia del MediaStore, prima di rileggere la cartella: più
 * della ricerca, perché si aspetta che finisca un trasferimento, e sotto il mezzo secondo si
 * rileggerebbe a metà copia.
 */
private const val OUTSIDE_PAUSE_MS = 700L

/**
 * Quanto restano visibili le cartelle nascoste: un minuto, come chiesto dall'utente. 'Annulla'
 * sulla notifica ne concede un altro.
 */
private const val PEEK_MS = 60_000L

/**
 * Riempie [into] fin dove arriva il flusso, e risponde quanti byte ci ha messo.
 *
 * ⚠️ **Un `read` può restituire meno di quanto chiesto**: un confronto su un `read` nudo
 * vedrebbe diversi due file identici letti a pezzi di misura diversa.
 */
private fun java.io.InputStream.fill(into: ByteArray): Int {
    var done = 0
    while (done < into.size) {
        val step = read(into, done, into.size - done)
        if (step < 0) break
        done += step
    }
    return done
}

/**
 * L'immagine decodificata vive nel ViewModel e non nella composizione: una rotazione non deve
 * rimandare il telefono in rete o a decodificare, che su un file grande è una pausa visibile.
 */
class ViewerViewModel(application: Application) : AndroidViewModel(application) {

    var state: ViewerState by mutableStateOf(ViewerState.Loading())
        private set

    /**
     * Il video in scena è partito: da qui in poi la provenienza non chiede più niente.
     *
     * ⚠️ **Serve alla rotazione**: la composizione si rifà, il modello no, e l'effetto che prepara
     * il lettore rileggerebbe [Arrival.TAPPED] e farebbe ripartire il video dall'inizio. È nel
     * modello perché [state] ha il setter privato: chi disegna non riscrive lo stato.
     */
    fun clipStarted() {
        val here = state
        if (here is ViewerState.Clip && here.from != Arrival.LEAFED) {
            state = here.copy(from = Arrival.LEAFED)
        }
    }

    var screen: Screen by mutableStateOf(HOME)
        private set

    /**
     * `null` finché le impostazioni salvate non sono state lette una volta.
     *
     * ⚠️ **Le schermate le aspettano**: la scala a riposo di un'immagine dipende da due
     * impostazioni, e coi valori di fabbrica l'immagine comparirebbe a una misura e poi
     * scatterebbe a un'altra.
     */
    var settings: Settings? by mutableStateOf(null)
        private set

    /**
     * Se una filigrana è pronta da scrivere: l'interruttore è acceso e un file è stato scelto.
     *
     * ⚠️ **Serve al tasto 'Salva'**: chi apre l'editor solo per firmare un'immagine non tocca
     * niente, e senza questo valore il tasto resterebbe spento. Si aggiorna quando cambiano le
     * impostazioni, anche tornando dalla pagina della filigrana.
     */
    var markReady: Boolean by mutableStateOf(false)
        private set

    /**
     * Se un logo è stato scelto, cioè se l'app ha un file da scrivere: un'altra domanda da
     * [markReady]. Il tasto 'Filigrana' in testata all'editor c'è solo in questo caso, come
     * 'Mostra nascoste' compare solo se una cartella è nascosta.
     */
    var markHas: Boolean by mutableStateOf(false)
        private set

    /**
     * Quello che il salvataggio deve sapere della filigrana, o `null` se non se ne scrive
     * nessuna.
     */
    private fun markPlan(): Watermark.Plan? {
        val now = settings ?: return null
        return if (!now.markOn) null else Watermark.Plan(
            spot = now.markSpot,
            size = now.markSize,
            air = now.markAir,
            alpha = now.markAlpha
        )
    }

    /**
     * Se la pagina della filigrana è aperta sopra l'editor, dal tocco lungo su quel tasto: una
     * scheda e non un cambio di schermata (voce `mark-imposta`; il perché è su `MarkSheet`).
     * Vive nel modello come [editorAsk]: la superficie si disegna sopra il `when` delle schermate.
     */
    var markSetup: Boolean by mutableStateOf(false)
        private set

    /** Il tocco lungo sul tasto 'Filigrana': la sua pagina si apre qui sopra. */
    fun openMarkSetup() {
        markSetup = true
    }

    /** La scheda della filigrana si chiude. */
    fun closeMarkSetup() {
        markSetup = false
    }

    /**
     * Il ridimensionamento predefinito, quello scritto nelle preferenze. Lo scrive solo 'Rendi
     * predefinito' (voce `resize-finestra`), non 'Applica'.
     */
    fun resizeSaved(): Resize.Plan {
        val now = settings
        return if (now == null) Resize.NONE
        else Resize.Plan(now.sizeMode, now.sizeValue)
    }

    /**
     * Il ridimensionamento che il salvataggio userà. Esiste anche a interruttore spento: l'editor
     * lo mostra nella sua finestra.
     *
     * ⚠️ È quello applicato, se c'è, altrimenti il predefinito: 'Applica' vale finché l'app è
     * aperta, 'Rendi predefinito' vale per sempre (`2.81`). Il piano applicato vive nel modello
     * perché chi rimpicciolisce lo fa su più immagini di fila, e non deve riscrivere la misura a
     * ogni apertura dell'editor.
     */
    fun resizePlan(): Resize.Plan = resizeNow ?: resizeSaved()

    /** Il piano applicato in questa sessione, o `null` finché vale il predefinito. */
    private var resizeNow: Resize.Plan? by mutableStateOf(null)

    /** Se quel piano si applica al salvataggio. */
    fun resizeOn(): Boolean = settings?.sizeOn ?: false

    /**
     * Lo scrive e lo accende, o lo spegne con `null`.
     *
     * ⚠️ Scriverlo vuol dire accenderlo (scelta `editor` a `d-resize-dove`). Spegnere non toglie
     * il piano: riaccendere non chiede di riscriverlo. Scrive nel modello; il predefinito nelle
     * preferenze lo scrive [setResizeDefault] (`2.81`).
     */
    fun setResize(plan: Resize.Plan?) {
        val now = settings ?: return
        if (plan != null) resizeNow = plan
        updateSettings(now.copy(sizeOn = plan != null))
    }

    /**
     * Il piano diventa quello predefinito, e vale anche per il salvataggio di adesso.
     *
     * ⚠️ Accende l'interruttore come 'Applica': un predefinito scritto a interruttore spento non
     * rimpicciolirebbe niente.
     */
    fun setResizeDefault(plan: Resize.Plan) {
        val now = settings ?: return
        resizeNow = plan
        updateSettings(now.copy(sizeOn = true, sizeMode = plan.mode, sizeValue = plan.value))
    }

    var recents: List<RecentImage> by mutableStateOf(emptyList())
        private set

    /**
     * Se le cartelle nascoste sono visibili adesso, per un minuto ('Mostra nascoste', scelta
     * dell'utente nel giro della `1.91`).
     *
     * ⚠️ Vive nel modello perché le nascoste si mostrano per entrarci: uno stato dentro
     * `FolderScreen` si perderebbe con la composizione, entrando nella cartella. Non si salva:
     * riaprendo l'app il minuto è già passato.
     */
    var peeking: Boolean by mutableStateOf(false)
        private set

    /**
     * La vista che la schermata iniziale mostra per questa volta sola, o `null` per quella salvata:
     * il Feed dell'azione 'Feed' del launcher (`6.10`, scelta A1) e 'Cartelle di sistema' per una
     * cartella aggiunta alla schermata Home (`6.20`).
     *
     * ⚠️ Non si salva: l'app aperta dalla sua icona torna alla vista delle impostazioni
     * ([handleIntent] la azzera), e così una vista scelta a mano ([chooseView]).
     */
    var viewOnce: FolderView? by mutableStateOf(null)
        private set

    /** La vista in vigore nella schermata iniziale: [viewOnce] finché vale, altrimenti la salvata. */
    fun shownView(saved: FolderView): FolderView = viewOnce ?: saved

    /** Una vista scelta nella schermata iniziale: si scrive, e la vista di una scorciatoia finisce. */
    fun chooseView(view: FolderView) {
        viewOnce = null
        settings?.let { updateSettings(it.copy(folderView = view)) }
    }

    /**
     * Se la finestra dell'indirizzo è aperta sopra l'app, chiesta dall'azione 'Apri un indirizzo'
     * del launcher (`6.10`). I menu della schermata iniziale aprono la stessa finestra da sé.
     */
    var addressAsked: Boolean by mutableStateOf(false)

    /*
     * ⚠️ Sollevamento e scorrimento del rail del tablet (`3.39-03`): le tre schermate a due
     * colonne ricreano ciascuna il rail, e senza uno stato nel modello la posizione della maniglia
     * e dell'elenco tornerebbe a zero a ogni tocco su una cartella.
     */
    var folderRailLift by mutableFloatStateOf(0f)
    var folderRailIndex by mutableIntStateOf(0)
    var folderRailOffset by mutableIntStateOf(0)

    /** Il conto alla rovescia del minuto: si annulla e si rifà a ogni proroga. */
    private var peekJob: Job? = null

    /**
     * Accende o spegne la vista temporanea delle cartelle nascoste.
     *
     * ⚠️ Alla scadenza compare un avviso (`Notices`), e il suo 'Annulla' proroga di un minuto
     * (scelta dell'utente). Spegnere a mano non mostra niente: l'avviso spiega una sparizione non
     * chiesta, e chi tocca 'Nascondi cartelle' l'ha chiesta.
     */
    fun peek(on: Boolean) {
        peekJob?.cancel()
        peeking = on
        if (!on) return
        val context = getApplication<Application>()
        peekJob = viewModelScope.launch {
            delay(PEEK_MS)
            peeking = false
            Notices.offer(
                text = context.getString(R.string.peek_over),
                action = context.getString(R.string.pick_undo),
                onAction = { peek(true) },
                millis = NOTICE_LONG_MS
            )
        }
    }

    /** Da dove viene l'immagine sullo schermo. Serve al menu; il caricatore l'ha già usata. */
    var source: Uri? by mutableStateOf(null)
        private set

    /**
     * Dove torna il gesto Indietro dal visualizzatore, e `null` quando deve uscire dall'app.
     *
     * ⚠️ È la destinazione e non un booleano: le provenienze sono tre (schermata iniziale,
     * collegamento da fuori, una griglia, che è una cartella precisa).
     */
    var viewerBack: Screen? by mutableStateOf(null)
        private set

    /**
     * L'esito della ricerca della cartella nell'ordine di base, dall'immagine più recente alla
     * più vecchia. `null` finché la ricerca non è finita.
     *
     * ⚠️ Contiene la ragione e non solo la serie, perché una serie mancante non dice perché
     * (vedi `Folder.Lookup`). È privato perché non è l'ordine in cui si sfoglia: quello lo
     * decide l'impostazione, in [folder].
     */
    private var listed: Folder.Lookup? by mutableStateOf(null)

    /**
     * L'esito nell'ordine in cui si sfoglia: è questo che guardano tutti gli altri.
     *
     * ⚠️ Si gira al momento dell'uso: la ricerca della cartella e la lettura delle impostazioni
     * sono due coroutine indipendenti, e non si sa quale arrivi prima. Così spostare
     * l'interruttore rigira la serie senza rileggere il database. `derivedStateOf` evita di
     * ricopiare la lista, anche di centinaia di voci, a ogni lettura durante la composizione.
     */
    val folder: Folder.Lookup? by derivedStateOf { listed.oriented().sifted() }

    /** Scorciatoia per chi della cartella vuole solo la serie, quando c'è. */
    val series: Folder.Series? get() = folder?.seriesOrNull

    /**
     * L'esito nel verso scelto dall'impostazione.
     *
     * ⚠️ Serve in tutti e due i versi, perché girare una serie è la sua stessa inversa: mostra
     * l'ordine di lettura e riscrive in [listed] quello grezzo (vedi [step]).
     */
    private fun Folder.Lookup?.oriented(): Folder.Lookup? {
        if (settings?.reverseSequence != true) return this
        /*
         * ⚠️ Il verso non tocca il web: l'impostazione parla di ordine di data. Una serie remota
         * è ordinata per numero, e girarla farebbe andare la strisciata da `foto_013` a
         * `foto_012`.
         */
        val first = this?.seriesOrNull?.items?.firstOrNull()
        if (first != null && WebSeries.isWeb(first)) return this
        return this?.reversed()
    }

    /**
     * Lo stesso esito posizionato sull'inizio della sequenza scelta.
     *
     * ⚠️ A verso invertito la prima immagine della sequenza è l'ultima riga dell'ordine grezzo:
     * dimenticarlo apre la cartella con la strisciata in avanti che non ha dove andare. Lo usano
     * l'avvio e la griglia.
     */
    private fun Folder.Lookup.atSequenceStart(): Folder.Lookup {
        val whole = seriesOrNull ?: return this
        val first = if (settings?.reverseSequence == true) whole.items.lastIndex else 0
        return Folder.Lookup.Found(whole.copy(index = first))
    }

    /**
     * Se in questa visita alla griglia si è già aperta un'immagine.
     *
     * ⚠️ È un sì o no e non l'indice: la posizione la griglia la legge da `series.index`, che la
     * strisciata tiene aggiornato. Un indice copiato al ritorno poteva restare vuoto, e l'anello
     * spariva. Senza questa bandierina, entrando in una cartella si evidenzierebbe la prima
     * miniatura, che nessuno ha guardato.
     */
    var gridVisited: Boolean by mutableStateOf(false)
        private set

    /** Se il permesso sulle cartelle è già stato chiesto una volta. */
    var folderAsked: Boolean by mutableStateOf(true)
        private set

    init {
        val context = getApplication<Application>()
        viewModelScope.launch {
            SettingsStore.flow(context).collect { fresh ->
                applySettings(fresh)
                // Il disco si legge fuori dal thread principale, anche per un solo `isFile`.
                val logo = withContext(Dispatchers.IO) { Watermark.file(context) != null }
                markHas = logo
                markReady = fresh.markOn && logo
                // ⚠️ Gli appunti prima della cartella d'avvio, o la seconda coprirebbe
                // l'immagine che i primi hanno appena aperto.
                readClipboard()
                openStartFolder(fresh)
            }
        }
        viewModelScope.launch { Recents.flow(context).collect { recents = it } }
        viewModelScope.launch { FolderAsk.flow(context).collect { folderAsked = it } }
        viewModelScope.launch { sweepClipLeftovers(context) }
        // ⚠️ `drop(1)` salta il valore iniziale del contatore: le importazioni precedenti il
        // modello le ha già lette all'avvio.
        viewModelScope.launch { Backups.imported.drop(1).collect { reloadStored() } }
        viewModelScope.launch { Backup.sweep(context) }
    }

    /**
     * Rilegge quello che un'importazione cambia senza passare dal flusso delle preferenze: il
     * logo della filigrana, le tinte delle cartelle e le loro copertine.
     *
     * ⚠️ Senza di lei un'importazione si vedrebbe a metà fino al riavvio: le tre cose si leggono
     * una volta e poi le aggiorna il gesto che le cambia ([tintFolder], [applyCover], la pagina
     * della filigrana), da cui l'importazione non passa. Un'importazione del solo logo non cambia
     * nessuna preferenza, quindi il flusso delle preferenze non emette niente.
     */
    private suspend fun reloadStored() {
        val context = getApplication<Application>()
        val logo = withContext(Dispatchers.IO) { Watermark.file(context) != null }
        markHas = logo
        markReady = settings?.markOn == true && logo
        folderTints = FolderTints.all(context)
        folderCovers = FolderCovers.all(context)
    }

    /**
     * Cancella i modelli della ricerca per contenuto, funzione tolta nella `1.12`.
     *
     * ⚠️ Chi aggiorna da una `1.11` o precedente ha in `filesDir/clip` 86 MB che nessun tasto
     * cancella più. Costa un `stat` su una cartella che quasi sempre non c'è, quindi non si tiene
     * una preferenza per ricordare di averlo fatto. Si potrà togliere quando nessuno aggiornerà
     * più da quelle versioni.
     */
    private suspend fun sweepClipLeftovers(context: Context) {
        withContext(Dispatchers.IO) {
            runCatching { File(context.filesDir, "clip").deleteRecursively() }
        }
    }

    /**
     * Se l'intento di partenza è già stato letto da **questa** istanza del modello.
     *
     * ⚠️ È la guardia dell'avvio, e vive nel modello perché il modello resiste a una rotazione e
     * non alla chiusura del processo: la risposta a 'l'ho già letto?' è giusta nei due casi. Il
     * perché per esteso è in `onCreate`.
     */
    var intentRead = false
        private set

    /**
     * Se questa esecuzione serve a scegliere un'immagine per un'altra app (`1.89`, richiesta
     * dell'utente).
     *
     * Con `ACTION_GET_CONTENT` o `ACTION_PICK` l'app si apre come sempre, e cambia solo il tocco
     * su una miniatura: consegna il file a chi lo ha chiesto e chiude l'app. È uno stato del
     * modello perché la scelta deve resistere alla rotazione e alla navigazione fra cartelle.
     * ⚠️ `ACTION_PICK` c'è dalla `1.91` (voce `selettore-app`): col solo `GET_CONTENT` l'app non
     * compariva nell'elenco. Il perché è nel manifesto, accanto ai due filtri.
     */
    var picking: Boolean by mutableStateOf(false)
        private set

    /**
     * Legge l'intento. La si chiama da `onCreate` e da `onNewIntent`, mai dalla composizione, che
     * lo rileggerebbe a ogni ricomposizione.
     */
    fun handleIntent(intent: Intent?, fresh: Boolean) {
        intentRead = true
        /*
         * Si legge prima dell'indirizzo: un `GET_CONTENT` non ne ha, quindi finisce nel ramo
         * dell'avvio dall'icona (si parte dalle cartelle), e lo distingue solo questa bandierina.
         */
        val wants = intent?.action == Intent.ACTION_GET_CONTENT ||
            intent?.action == Intent.ACTION_PICK
        /*
         * ⚠️ L'indirizzo di un `ACTION_PICK` è la sorgente in cui scegliere (di solito
         * `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`), non un'immagine da aprire: passato a
         * `imageUri`, aprirebbe il visualizzatore sull'elenco intero.
         */
        val uri = if (wants) null else intent.imageUri()
        /*
         * ⚠️ Le azioni del launcher (`Shortcuts`, `6.10`) dicono che cosa aprire, quindi agiscono
         * anche ad app già aperta, prima della guardia qui sotto. Gli appunti non si leggono e la
         * cartella d'avvio non si apre: la persona ha già scelto dove andare.
         */
        val shortcut = intent?.action?.takeIf { it in Shortcuts.actions }
        if (shortcut != null) {
            picking = false
            fromIcon = false
            clipboardTried = true
            atStart = false
            viewOnce = null
            addressAsked = false
            screen = HOME
            when (shortcut) {
                Shortcuts.FEED -> viewOnce = FolderView.FEED
                Shortcuts.SEARCH -> openSearch()
                Shortcuts.ADDRESS -> addressAsked = true
                /*
                 * ⚠️ Le due azioni controllano che cosa si chiede (`6.29`, rilievi C29 ed E22):
                 * l'activity è esportata, quindi qualunque app installata potrebbe aprire una
                 * cartella nascosta (e metterne il nome nella schermata Home come quarta azione) o
                 * un percorso qualunque in 'Cartelle di sistema'. Le nostre scorciatoie non lo
                 * chiedono mai, quindi rifiutarlo non costa niente.
                 */
                Shortcuts.FOLDER -> {
                    val name = intent.getStringExtra(Shortcuts.NAME)
                    val bucket = intent.getLongExtra(Shortcuts.BUCKET, 0L)
                    if (name != null) viewModelScope.launch {
                        val selection = snapshotFlow { settings }.filterNotNull().first().folderSelection
                        val known = Folder.buckets(getApplication()).firstOrNull { it.id == bucket }
                        if (known != null && selection.visible(known.path)) openGrid(bucket, name)
                    }
                }
                // 'Aggiungi alla schermata Home' (`6.20`): la cartella si apre per questa volta in
                // 'Cartelle di sistema', da cui è stata aggiunta; AIV Play non ha quella vista.
                // Solo un percorso dentro una memoria del telefono (`6.29`, vedi sopra).
                Shortcuts.TREE -> {
                    val path = intent.getStringExtra(Shortcuts.PATH)
                    if (path != null && FolderView.TREE in Store.views && insideRoots(path)) {
                        viewOnce = FolderView.TREE
                        treeTo(path)
                    }
                }
            }
            return
        }
        // L'app aperta dalla sua icona torna alla vista salvata (`viewOnce`), anche quando era
        // già aperta e non cambia altro.
        if (intent?.action == Intent.ACTION_MAIN) viewOnce = null
        /*
         * ⚠️ Un intento senza indirizzo che arriva ad app già aperta non azzera niente (`2.78`):
         * l'app è `singleTop`, e toccare la sua icona consegna un `onNewIntent` del launcher.
         * Senza questa guardia tornerebbe a [HOME] e chiuderebbe l'editor con le modifiche in
         * corso. Viene prima di [picking], che un intento che non chiede niente non deve
         * riscrivere.
         * Resta fuori il processo chiuso dal sistema in secondo piano: il lavoro dell'editor vive
         * nella composizione, e riparte comunque.
         */
        if (!fresh && uri == null && !wants) return
        picking = wants
        if (uri == null) {
            // Aperta dalla propria icona: si va alle cartelle.
            screen = HOME
            atStart = true
            fromIcon = true
            settings?.let(::openStartFolder)
        } else {
            // Un collegamento da fuori non guarda negli appunti: la persona ha già detto che
            // cosa vuole aprire.
            fromIcon = false
            clipboardTried = true
            open(uri, backToApp = false)
        }
    }

    /** Se questa esecuzione è partita dall'icona, cioè senza che nessuno dica che aprire. */
    private var fromIcon = false

    /** Gli appunti si guardano una volta per esecuzione, non a ogni ritorno del fuoco. */
    private var clipboardTried = false

    /**
     * Se la finestra ha il fuoco adesso.
     *
     * ⚠️ Il fuoco è un evento di sistema e le impostazioni arrivano da una coroutine, in ordine
     * ignoto: segnato il primo dei due, il secondo può leggere gli appunti.
     */
    private var focused = false

    /** L'ha chiamata l'activity: la finestra ha preso o perso il fuoco. */
    fun windowFocus(hasFocus: Boolean) {
        focused = hasFocus
        if (hasFocus) readClipboard()
        if (hasFocus) backFromOutside()
    }

    /**
     * Gli appunti, guardati una volta sola e solo all'avvio dall'icona.
     *
     * ⚠️ Si chiama quando la finestra prende il fuoco: da Android 10 un'app legge gli appunti
     * solo mentre ha il fuoco. Da Android 12 il sistema mostra un avviso a ogni lettura, che è il
     * costo della funzione. Nessuna richiesta di rete: un indirizzo d'immagine si riconosce
     * dall'estensione, e se non lo è lo dice l'errore del visualizzatore, con 'Riprova'.
     */
    private fun readClipboard() {
        if (clipboardTried || !fromIcon || !focused) return
        // ⚠️ Spenta finché non la si accende, e il controllo viene prima della lettura: è la
        // lettura a far comparire l'avviso di sistema.
        val fresh = settings ?: return
        if (!fresh.clipboardStart) {
            // Detto di no: non si guarda, e non si riproverà più in questa esecuzione.
            clipboardTried = true
            return
        }
        clipboardTried = true
        val context = getApplication<Application>()
        val clip = ImageActions.clipInClipboard(context) ?: return
        val uri = clip.address
        if (!ImageActions.looksLikeImage(uri)) return
        /*
         * ⚠️ Lo stesso indirizzo si apre una volta sola (richiesta dell'utente): negli appunti
         * resta per giorni, e l'app lo riaprirebbe a ogni avvio. Riapre un indirizzo nuovo, o lo
         * stesso copiato di nuovo, riconosciuto dall'istante di scrittura degli appunti (`1.45`).
         * Le due prove sono in OR: un sistema che non dà l'istante (zero) non deve bloccare un
         * indirizzo nuovo.
         */
        val address = uri.toString()
        val again = clip.at > 0L && clip.at > fresh.clipboardWhen
        if (address == fresh.clipboardDone && !again) return
        // ⚠️ La scrittura è a parte e non blocca: l'apertura qui sotto deve restare sincrona
        // per non perdere la corsa con la cartella d'avvio.
        viewModelScope.launch { SettingsStore.clipboardOpened(context, address, clip.at) }
        // ⚠️ Spegne la cartella d'avvio: se il fuoco arriva prima delle impostazioni,
        // `openStartFolder` scatterebbe dopo e coprirebbe l'immagine degli appunti, che per
        // scelta dell'utente vengono prima.
        atStart = false
        open(uri)
    }

    /**
     * Se questo è l'avvio dell'app e non un ritorno.
     *
     * ⚠️ La cartella d'avvio si apre solo a impostazioni lette: la bandierina tiene la scelta in
     * attesa fino ad allora.
     */
    private var atStart = false

    /**
     * La cartella dell'avvio, aperta una volta per processo e non a ogni tocco dell'icona: chi è
     * tornato alla schermata iniziale ci è tornato apposta.
     */
    private fun openStartFolder(fresh: Settings) {
        if (!atStart) return
        atStart = false
        val bucket = fresh.startFolder ?: return
        if (!fresh.openAtStart) return
        // Apre la griglia e non un'immagine (`0.48`, scelta dell'utente). Il nome arriva dalle
        // impostazioni (`Settings.startFolderName`): chiederlo al MediaStore vorrebbe dire
        // un'altra interrogazione all'avvio.
        openGrid(bucket, fresh.startFolderName)
    }

    /**
     * L'apertura in corso, per poterla annullare: chi apre un indirizzo sbagliato e torna
     * indietro non deve continuare a scaricare megabyte sulla sua connessione dati.
     */
    private var loadJob: Job? = null

    /**
     * A quale apertura appartiene il progresso che arriva.
     *
     * ⚠️ Il download gira in un ciclo bloccante, fuori da una coroutine: dopo l'annullamento può
     * arrivare ancora un pezzo, e senza questo contatore la percentuale dell'immagine
     * abbandonata comparirebbe per un istante sopra quella nuova.
     */
    private var loadToken = 0

    /**
     * L'unica strada per cui un indirizzo diventa un'immagine sullo schermo.
     *
     * Chi chiama ha già sistemato schermata, provenienza e serie: qui si fa solo il caricamento,
     * condiviso dai tre punti che aprono, perché una seconda copia dimenticherebbe l'annullamento.
     */
    private fun startLoad(uri: Uri, from: Arrival, remember: Boolean = false) {
        val context = getApplication<Application>()
        loadJob?.cancel()
        val token = ++loadToken
        // ⚠️ Un video non passa dal decodificatore (`0.83`) e non ha niente da annullare. Il
        // contatore si è già mosso, quindi la percentuale di un'immagine che si stava aprendo
        // non compare sopra il video.
        if (Videos.isVideo(uri)) {
            loadJob = null
            state = ViewerState.Clip(uri, from)
            return
        }
        state = ViewerState.Loading()
        loadJob = viewModelScope.launch {
            val next = load(context, uri) { fraction ->
                if (token == loadToken) state = ViewerState.Loading(fraction)
            }
            // Si ricorda solo se si è aperto davvero: un elenco di indirizzi falliti sarebbe un
            // elenco di trappole.
            if (remember && next is ViewerState.Ready) {
                Recents.remember(context, uri.toString(), ImageActions.fileName(next.image, uri))
            }
            state = next
        }
    }

    /** Ferma quello che si stava aprendo: si sta andando altrove. */
    private fun stopLoad() {
        loadJob?.cancel()
        loadJob = null
        loadToken++
        state = ViewerState.Loading()
    }

    /**
     * Riprova quello che non si è aperto.
     *
     * Esiste per la rete: un `403` di passaggio, una galleria senza segnale, un server lento.
     * ⚠️ Si ripassa dalla cache, quindi un errore dopo un download riuscito (memoria finita in
     * decodifica) si ritenta senza rete; una copia scaduta si rivalida col server
     * (`RemoteCache`, `6.27`).
     */
    fun retry() {
        val uri = source ?: return
        // La provenienza si conserva: riprovare non è un modo nuovo di arrivare.
        startLoad(uri, (state as? ViewerState.Clip)?.from ?: Arrival.HANDED)
    }

    fun open(uri: Uri, backToApp: Boolean = true) {
        // ⚠️ L'indirizzo si riscrive in `https` prima di ogni altra cosa, o `source`, la
        // condivisione, la cache e le vicine parlerebbero di un indirizzo diverso da quello
        // aperto. Il perché è in [WebSeries.secured].
        val target = WebSeries.secured(uri)
        upgraded = target != uri
        webRule = null
        source = target
        // `null` vuol dire 'esci dall'app': chi arriva da un collegamento non ha un posto
        // dell'app in cui tornare.
        viewerBack = if (backToApp) HOME else null
        screen = Screen.Viewer
        putList(null)
        startLoad(target, Arrival.HANDED, remember = true)
        // ⚠️ La cartella si cerca in una coroutine sua: è un'interrogazione del database che
        // l'immagine non aspetta, e legarla al caricamento ritarderebbe quello che si guarda
        // per preparare un gesto che forse non arriverà.
        val context = getApplication<Application>()
        relist { if (WebSeries.isWeb(target)) webWindow(target) else Folder.seriesAround(context, target) }
    }

    /**
     * Apre le immagini di una pagina come serie, dalla prima. Si arriva dalla finestra 'Apri un
     * indirizzo'; il perché è su `WebSeries.Rule.PAGE_LINKS`.
     *
     * ⚠️ Non passa da [open], che cerca la serie con la cascata (`webWindow`): qui la serie è già
     * intera, e passare di là farebbe una richiesta di rete inutile e mostrerebbe per un istante
     * una finestra di tre indirizzi. Il criterio si segna prima di [startLoad], o il primo passo
     * della strisciata rifarebbe la finestra (vedi [showAt]).
     */
    fun openPage(page: Folder.Series) {
        val first = page.at(page.index) ?: return
        upgraded = false
        webRule = WebSeries.Rule.PAGE_LINKS
        source = first
        viewerBack = HOME
        screen = Screen.Viewer
        putList(Folder.Lookup.Found(page))
        startLoad(first, Arrival.HANDED, remember = true)
    }

    /**
     * Il criterio che ha funzionato per l'indirizzo remoto aperto, quando ce n'è uno.
     *
     * Vive nel modello perché è lo stato di questa apertura: si azzera aprendo un altro
     * indirizzo, e [WebSeries], che non ha stato, non saprebbe quando.
     */
    private var webRule: WebSeries.Rule? = null

    /** Se l'indirizzo aperto è stato riscritto da `http` a `https`. */
    private var upgraded = false

    /**
     * Il filtro temporaneo della griglia: immagini, video o tutto.
     *
     * ⚠️ Si azzera a ogni ingresso in una griglia (richiesta dell'utente): un filtro rimasto dalla
     * cartella di prima farebbe sembrare la nuova mezza vuota. I tre punti d'ingresso lo
     * rimettono a [MediaKind.ALL]. Filtra la serie, quindi vale anche per la strisciata nel
     * visualizzatore, e griglia e visualizzatore leggono la stessa lista con gli stessi indici.
     */
    var gridFilter: MediaKind by mutableStateOf(MediaKind.ALL)
        private set

    fun sift(kind: MediaKind) {
        gridFilter = kind
    }

    /**
     * L'esito senza le cose che il filtro esclude.
     *
     * ⚠️ L'indice segue l'immagine e non il numero: si cerca dove è finita quella corrente, e se
     * il filtro l'ha esclusa si riparte da capo.
     */
    private fun Folder.Lookup?.sifted(): Folder.Lookup? {
        val kind = gridFilter
        if (kind == MediaKind.ALL) return this
        val whole = this?.seriesOrNull ?: return this
        val here = whole.items.getOrNull(whole.index)
        val kept = whole.items.filter { kind.keeps(it) }
        val at = kept.indexOf(here).coerceAtLeast(0)
        return Folder.Lookup.Found(Folder.Series(kept, at))
    }

    /**
     * La finestra attorno a un indirizzo remoto, ricordandosi il criterio che ha vinto.
     *
     * ⚠️ Il criterio resta anche se questa volta non ne ha vinto nessuno: alla fine della serie
     * non c'è nessuna vicina, e cancellarlo costringerebbe la strisciata indietro a rifare tutta
     * la cascata.
     */
    private suspend fun webWindow(uri: Uri): Folder.Lookup {
        val window = WebSeries.around(uri, webRule)
        webRule = window.rule ?: webRule
        return window.lookup
    }

    // ⚠️ Per aprire una cartella sulla sua immagine più recente la strada è quella di
    // `openGrid`, `Folder.newestIn` più `atSequenceStart`, non `seriesAround`: la cartella è
    // già nota, e ripartire dall'immagine rifà la domanda al MediaStore passando dalle chiavi
    // del selettore, dove questo pezzo ha già sbagliato.

    /** L'unico posto in cui un indirizzo diventa uno stato: i tre che aprono passano di qui. */
    private suspend fun load(
        context: android.content.Context,
        uri: Uri,
        onProgress: (Float) -> Unit
    ): ViewerState =
        when (val result = ImageSource.load(context, uri, onProgress)) {
            is LoadResult.Ok -> ViewerState.Ready(result.image)
            is LoadResult.Failed -> ViewerState.Error(reasonFor(result.reason), result.detail)
        }

    /** L'elenco delle cartelle nella veste 'scegli quella dell'avvio'. */
    fun chooseStartFolder() {
        screen = Screen.Folders(forStart = true)
    }

    /**
     * Fuori dalla scelta della cartella d'avvio: si torna alle impostazioni.
     *
     * Vale solo per quella veste: nell'altra l'elenco è la schermata iniziale, e là il gesto
     * Indietro chiude l'app.
     */
    fun leaveStartFolderChoice() {
        screen = Screen.Settings
    }

    /**
     * Una cartella scelta: o se ne apre la griglia, o diventa quella dell'avvio.
     *
     * Sceglierla accende anche l'avvio automatico: si arriva qui dall'interruttore o dal tasto
     * sotto, quindi l'intenzione è quella. Spegnerlo lascia scritta la scelta.
     */
    fun folderPicked(bucket: Folder.Bucket, forStart: Boolean) {
        if (!forStart) {
            openGrid(bucket.id, bucket.name)
            return
        }
        val current = settings ?: return
        updateSettings(
            current.copy(
                startFolder = bucket.id,
                startFolderName = bucket.name,
                openAtStart = true
            )
        )
        screen = Screen.Settings
    }

    /**
     * Il peso della cartella aperta e quanti video ha, per le due pastiglie dell'intestazione.
     *
     * Si legge una volta per cartella con `Folder.weigh`: la griglia conosce gli indirizzi e non
     * le righe, quindi riceve i numeri pronti. ⚠️ Si azzera aprendo, o entrando in una cartella
     * comparirebbe per un istante il peso di quella di prima.
     */
    var facts: Folder.Facts by mutableStateOf(Folder.Facts())
        private set

    /**
     * La tinta scelta per la cartella aperta, come indice in [FRONT_TINTS], oppure `null`.
     *
     * È un dato della cartella e non un'impostazione: si sceglie col tocco lungo sull'icona
     * dell'intestazione (richiesta dell'utente, giro della `1.83`). Si legge una volta per
     * cartella come [facts]: cambia solo col gesto, che la scrive qui e nell'archivio insieme.
     */
    var tint: Int? by mutableStateOf(null)
        private set

    /**
     * Segna (o toglie) la tinta della cartella aperta.
     *
     * Scrive due volte: lo stato in memoria, perché la fascia cambi colore nello stesso
     * fotogramma, e l'archivio, perché il colore resti.
     */
    fun tintFolder(index: Int?) {
        val dove = (screen as? Screen.Grid)?.bucket ?: return
        tint = index
        // Anche la mappa della schermata iniziale: con la guardia di [readBuckets] una rilettura
        // non arriverebbe, e la cartella mostrerebbe il colore vecchio.
        folderTints = if (index == null) folderTints - dove else folderTints + (dove to index)
        val context = getApplication<Application>()
        viewModelScope.launch { FolderTints.set(context, dove, index) }
    }

    /**
     * La copertina scelta a mano per la cartella aperta, o `null` se ha quella predefinita.
     *
     * È un dato della cartella, come [tint]. Serve al menu del FAB, che offre di toglierla solo
     * se c'è.
     */
    var cover: Uri? by mutableStateOf(null)
        private set

    /**
     * Per quale cartella si sta scegliendo una copertina, e come si chiama.
     *
     * ⚠️ È una modalità e non una finestra (scelta a `d-copertina-come`, giro della `1.92`): si
     * sfoglia l'app com'è, da qualunque cartella, e il tocco su una miniatura vale come scelta.
     * Vive nel modello, come [peek], perché fra l'inizio e la scelta si cambia cartella. Contiene
     * anche il nome, che la fascia mostra e che dal solo identificatore non si ricaverebbe.
     */
    var covering: Covering? by mutableStateOf(null)
        private set

    /** La cartella per cui si sta scegliendo una copertina. Vedi [covering]. */
    data class Covering(val bucket: Long, val name: String)

    /**
     * Il tocco sull'icona dell'intestazione: comincia a scegliere la copertina, oppure riporta
     * quella predefinita se la scelta era già partita da questa stessa cartella.
     *
     * ⚠️ Il secondo tocco sulla stessa cartella è il solo modo di tornare alla predefinita
     * (`1.95`, richiesta dell'utente): servono due tocchi, con la fascia in mezzo, così un tocco
     * involontario non cancella una copertina scelta. Su un'altra cartella il gesto ricomincia
     * (scelta dell'utente): il confronto è sull'identificatore. Il ritorno all'elenco iniziale lo
     * fa [coverAway].
     */
    fun startCover() {
        val quale = screen as? Screen.Grid ?: return
        if (covering?.bucket == quale.bucket) {
            covering = null
            applyCover(quale.bucket, null)
        } else {
            covering = Covering(quale.bucket, quale.name)
        }
    }

    /**
     * Porta all'elenco iniziale delle cartelle chi ha appena cominciato a scegliere una copertina.
     *
     * Richiesta dell'utente (`2.96`): la scelta parte dall'elenco iniziale, da cui si raggiunge
     * qualunque cartella.
     * ⚠️ Si esce solo se una scelta è partita: se lo stesso tocco ha rimesso la predefinita
     * ([startCover]) si resta, e [covering] dice quale dei due casi è successo. Lo chiama la
     * griglia e non [startCover]: la prima volta la griglia mostra il mini-onboarding, e si esce
     * quando la persona lo chiude.
     */
    fun coverAway() {
        if (covering != null && screen is Screen.Grid) leaveGrid()
    }

    /** Lascia perdere la scelta in corso. */
    fun stopCover() {
        covering = null
    }

    /** L'immagine toccata diventa la copertina della cartella da cui la scelta è partita. */
    fun coverPicked(source: Uri) {
        val quale = covering ?: return
        covering = null
        applyCover(quale.bucket, source)
    }

    /** Toglie la copertina scelta della cartella aperta: la voce del menu del FAB. */
    fun clearCover() {
        (screen as? Screen.Grid)?.bucket?.let { applyCover(it, null) }
    }

    /**
     * Scrive (o toglie) la copertina di [dove], e dice com'è andata.
     *
     * ⚠️ Lo stato si scrive dopo, al contrario di [tintFolder], perché la copia può non riuscire:
     * decodifica l'immagine e la scrive sul disco. L'esito lo dice un avviso, perché la copertina
     * si vede nella schermata iniziale e non in quella da cui si è scelto. [cover] si aggiorna
     * solo se la cartella aperta è quella della copertina.
     */
    private fun applyCover(dove: Long, source: Uri?) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val scelta = if (source == null) {
                FolderCovers.clear(context, dove)
                null
            } else {
                FolderCovers.set(context, dove, source) ?: run {
                    Notices.say(context.getString(R.string.folder_cover_fail))
                    return@launch
                }
            }
            if ((screen as? Screen.Grid)?.bucket == dove) cover = scelta
            folderCovers = if (scelta == null) {
                folderCovers - dove
            } else {
                folderCovers + (dove to scelta)
            }
            // Due frasi: chi ripete il tocco deve sapere se ha messo una copertina nuova o è
            // tornato alla predefinita.
            Notices.say(
                context.getString(
                    if (scelta == null) R.string.folder_cover_reset else R.string.folder_cover_done
                )
            )
        }
    }

    /**
     * Rinomina la cartella aperta, e ci resta dentro.
     *
     * ⚠️ Il `BUCKET_ID` viene dal percorso, quindi cambia: la griglia si riapre sul nuovo, o
     * mostrerebbe una cartella vuota senza errori. Prima si spostano copertina e colore, che sono
     * legati all'identificatore; una rinomina fatta da fuori li perde, e li pota
     * [FolderCovers.sweep]. Il percorso arriva da [facts]; se una cartella appena aperta non lo
     * ha ancora, si mostra l'errore.
     */
    fun renameFolder(name: String) {
        val quale = screen as? Screen.Grid ?: return
        val context = getApplication<Application>()
        val dove = facts.path
        if (dove.isNullOrBlank()) {
            Notices.say(context.getString(R.string.folder_rename_fail))
            return
        }
        viewModelScope.launch {
            val esito = Folder.rename(context, dove, name)
            when (esito.esito) {
                Folder.Renamed.Esito.DONE -> {
                    val nato = esito.bucket
                    if (nato != null) {
                        FolderCovers.move(context, quale.bucket, nato)
                        FolderTints.move(context, quale.bucket, nato)
                        folderCovers = FolderCovers.all(context)
                        folderTints = FolderTints.all(context)
                        openGrid(nato, name.trim())
                    } else {
                        // Senza identificatore nuovo (la cartella non ha media indicizzati)
                        // non c'è una griglia da mostrare: si torna alla schermata iniziale.
                        screen = HOME
                    }
                    // La schermata iniziale rilegge le cartelle quando [outsideStamp] cambia.
                    outsideStamp++
                    Notices.say(context.getString(R.string.folder_renamed))
                }
                Folder.Renamed.Esito.TAKEN ->
                    Notices.say(context.getString(R.string.folder_rename_taken))
                Folder.Renamed.Esito.BAD_NAME, Folder.Renamed.Esito.FAILED ->
                    Notices.say(context.getString(R.string.folder_rename_fail))
            }
        }
    }

    /**
     * Una cartella in miniature.
     *
     * ⚠️ La serie che carica finisce in [listed], ed è la stessa che sfoglia il visualizzatore:
     * griglia e strisciata non possono dare due ordini diversi, e aprire un'immagine dalla
     * griglia non chiede una seconda interrogazione del MediaStore.
     */
    fun openGrid(bucket: Long, name: String) {
        gridFilter = MediaKind.ALL
        screen = Screen.Grid(bucket, name)
        putList(null)
        source = null
        // Cartella nuova, nessuna immagine ancora guardata.
        gridVisited = false
        // L'immagine di prima non serve più, e il suo bitmap occuperebbe memoria.
        stopLoad()
        val context = getApplication<Application>()
        /*
         * ⚠️ La cartella diventa la quarta azione del launcher (`6.10`, scelta B1), ma non mentre
         * le nascoste sono visibili, né mentre l'app sceglie un'immagine per un'altra app: un
         * nome nascosto finirebbe nella schermata Home, e una scelta per altri non è una visita.
         */
        if (!peeking && !picking) Shortcuts.lastFolder(context, bucket, name)
        // ⚠️ Numeri, tinta e copertina si azzerano, o entrando in una cartella si vedrebbero
        // per un istante quelli di prima (e la voce che toglie la copertina comparirebbe a
        // vuoto). L'inizio è quello della sequenza scelta: la prima miniatura è l'immagine da
        // cui parte l'avvio.
        facts = Folder.Facts()
        tint = null
        cover = null
        relist { Folder.newestIn(context, bucket).atSequenceStart() }
        // ⚠️ In un lancio a sé: i numeri servono alla fascia in cima, che si vede subito, e non
        // devono aspettare la lista. Dentro [folderJob], come [listJob], perché aprendo due
        // cartelle di seguito i numeri della prima non arrivino nella seconda.
        folderJob?.cancel()
        folderJob = viewModelScope.launch {
            launch { facts = Folder.weigh(context, bucket) }
            launch { tint = FolderTints.of(context, bucket) }
            launch { cover = FolderCovers.of(context, bucket) }
        }
    }

    /** Le letture dei dati della cartella aperta (numeri, tinta, copertina), annullate insieme. */
    private var folderJob: Job? = null

    /**
     * Il testo che si sta cercando. Vuoto quando la ricerca si apre.
     *
     * Vive qui e non in [Screen.Search] perché cambia a ogni lettera (vedi la nota su quella
     * schermata).
     */
    var query: String by mutableStateOf("")
        private set

    /**
     * La lettura di [listed] in corso, per poterla annullare quando la lista cambia di nuovo.
     *
     * ⚠️ Senza, la risposta più lenta vincerebbe sulla più recente: digitando `mare` partono
     * quattro ricerche, e il MediaStore non risponde nell'ordine di partenza. Vale per tutte le
     * letture della lista (`6.27`, rilievo C30): una cascata del web lasciata a metà arriverebbe
     * nella schermata nuova. Chi scrive [listed] passa da [relist] o da [putList], che annullano
     * la lettura di prima.
     */
    private var listJob: Job? = null

    /**
     * Legge la lista nuova fuori dal thread principale, e annulla la lettura di prima.
     *
     * ⚠️ `ensureActive` prima di scrivere: una lettura annullata mentre finisce una query già
     * partita non deve scrivere. `withContext` lo garantisce al ritorno; la riga copre anche una
     * [read] che non lo usa.
     */
    private fun relist(read: suspend () -> Folder.Lookup?) {
        listJob?.cancel()
        listJob = viewModelScope.launch {
            val fresh = read()
            ensureActive()
            listed = fresh
        }
    }

    /** Scrive la lista subito, e annulla la lettura che stava per scriverne un'altra. */
    private fun putList(value: Folder.Lookup?) {
        listJob?.cancel()
        listed = value
    }

    /**
     * Apre il cestino.
     *
     * La lista si svuota prima, o per un istante comparirebbero le miniature della schermata di
     * prima. Nessun anello dell'ultima vista: si entra da un menu, non da un'immagine.
     */
    fun openBin() {
        gridFilter = MediaKind.ALL
        screen = Screen.Bin
        putList(null)
        source = null
        gridVisited = false
        stopLoad()
        val context = getApplication<Application>()
        relist { binLookup(context) }
    }

    /**
     * Il cestino come serie, pronto per la griglia.
     *
     * ⚠️ Il cestino vuoto è una serie vuota e non un errore: con `Lookup.Unreadable` la griglia
     * direbbe che la cartella non si legge, e per il cestino vuoto ha una frase sua. Solo la serie
     * piena passa da `atSequenceStart`, che su una lista vuota a verso invertito darebbe `-1`.
     */
    private suspend fun binLookup(context: Context): Folder.Lookup {
        // ⚠️ Il cestino di AIV Play è quello di sistema, da Android 11 (`5.40`): `Store.hasBin`.
        val items = when {
            Store.files -> Bin.list(context)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> MediaDelete.trashed(context)
            else -> emptyList()
        }
        val whole = Folder.Lookup.Found(Folder.Series(items, 0))
        return if (items.isEmpty()) whole else whole.atSequenceStart()
    }

    /**
     * Apre la cronologia dei ripristini, e ci si torna dal cestino.
     *
     * ⚠️ Non azzera niente, al contrario di [openBin]: si va e si torna, e la griglia del cestino
     * si ritrova pronta senza rileggere il disco, come con `openSettings`.
     */
    fun openHistory() {
        screen = Screen.History
    }

    /** Indietro dalla cronologia: il cestino, che è il solo posto da cui si apre. */
    fun leaveHistory() {
        screen = Screen.Bin
    }

    // ── Modifica di un'immagine ──

    /**
     * Se il selettore dell'app di modifica è aperto.
     *
     * Vive nel modello perché lo aprono in due: il menu del tocco lungo al primo uso, e le
     * impostazioni per cambiare idea.
     */
    var editorAsk: Boolean by mutableStateOf(false)
        private set

    /**
     * L'immagine che aspetta un editor, e `null` quando la domanda arriva dalle impostazioni.
     *
     * Distingue i due usi dello stesso selettore: con un indirizzo, scelta l'app si apre
     * l'immagine; senza, si è solo cambiata un'impostazione. Nessuno lo disegna, quindi non è
     * `mutableStateOf`.
     */
    private var editorFor: Uri? = null

    /** Se il salvataggio dell'editor semplice è in corso: il tasto deve smettere di rispondere. */
    var editorBusy: Boolean by mutableStateOf(false)
        private set

    /**
     * Una frase da mostrare una volta, e `null` quando non ce n'è nessuna.
     *
     * ⚠️ Vive nel modello perché l'esito arriva dopo la chiusura dell'editor che l'ha chiesto.
     * Chi lo mostra lo azzera con [noticeShown], o tornerebbe a ogni ricomposizione.
     */
    var notice: Int? by mutableStateOf(null)
        private set

    /** L'avviso è stato mostrato: si toglie, o riapparirebbe al primo ridisegno. */
    fun noticeShown() {
        notice = null
    }

    /**
     * Il menu chiede di modificare un'immagine: tre vie, e la prima volta si domanda.
     *
     * ⚠️ Il valore di fabbrica è la stringa vuota, che vuol dire 'mai chiesto' ed è diversa da
     * 'nessuna': è il solo caso in cui il selettore si apre da sé (vedi `Settings.editorApp`).
     */
    fun edit(uri: Uri) {
        val chosen = settings?.editorApp.orEmpty()
        when {
            // ⚠️ AIV Play ha solo i suoi editor (`5.10`): a un'app esterna chiederebbe di
            // scrivere su un file su cui AIV Play non può farla scrivere.
            !Store.files -> openInternal(uri)
            chosen.isBlank() -> {
                editorFor = uri
                editorAsk = true
            }
            Editors.isInternal(chosen) -> openInternal(uri)
            else -> openOutside(uri, chosen)
        }
    }

    /**
     * Il tocco lungo su 'Modifica': si sceglie l'app adesso, per questa immagine.
     *
     * Al contrario di [edit], che chiede solo la prima volta, apre sempre il selettore: è il
     * gesto per cambiare idea senza passare dalle impostazioni. La scelta resta memorizzata
     * (richiesta dell'utente), e la scrive [editorChosen].
     */
    fun editWith(uri: Uri) {
        editorFor = uri
        editorAsk = true
    }

    /** Le impostazioni vogliono cambiare l'app: stesso selettore, senza nessuna immagine. */
    fun chooseEditor() {
        editorFor = null
        editorAsk = true
    }

    /**
     * L'app è scelta: si ricorda, e se c'era un'immagine in attesa si prosegue.
     *
     * ⚠️ Si prosegue con `id`: la scrittura in DataStore è asincrona, `settings.editorApp`
     * contiene ancora il valore di prima, e ripassare da [edit] riaprirebbe il selettore.
     */
    fun editorChosen(id: String) {
        editorAsk = false
        val waiting = editorFor
        editorFor = null
        settings?.let { updateSettings(it.copy(editorApp = id)) }
        if (waiting == null) return
        if (Editors.isInternal(id)) openInternal(waiting) else openOutside(waiting, id)
    }

    /**
     * L'editor interno: il completo dove il suo conto può girare, il semplice sotto Android 13.
     *
     * ⚠️ Lo decide il telefono e non la scelta salvata (`5.01`, nota su `5.00-02`): la voce
     * 'Editor interno' è una sola (vedi `Editors.INTERNAL`), e da Android 13 in su l'editor
     * semplice non si apre.
     */
    private fun openInternal(uri: Uri) {
        if (advancedEditorAvailable()) openFullEditor(uri) else openEditor(uri)
    }

    /** Selettore chiuso senza scegliere: non si ricorda niente e non si apre niente. */
    fun editorSkip() {
        editorAsk = false
        editorFor = null
    }

    /** L'editor semplice: il nome serve all'intestazione e alla finestra del salvataggio. */
    private fun openEditor(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) { FileTree.displayName(context, uri) }
            if (name == null) {
                notice = R.string.edit_no_file
                return@launch
            }
            screen = Screen.Editor(uri, name)
        }
    }

    /**
     * L'editor completo: come [openEditor]. La guardia di versione è in [openInternal], la sola
     * via per arrivarci.
     */
    private fun openFullEditor(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) { FileTree.displayName(context, uri) }
            if (name == null) {
                notice = R.string.edit_no_file
                return@launch
            }
            screen = Screen.FullEditor(uri, name)
        }
    }

    /**
     * Un'app esterna.
     *
     * ⚠️ Se non parte (di solito perché è disinstallata) si riapre il selettore: la scelta
     * memorizzata punta a un'app che non c'è, e un avviso da solo lascerebbe 'Modifica' guasta.
     *
     * ⚠️ La copia di sicurezza vale anche qui (`1.13`, domanda dell'utente): un'app esterna
     * riscrive il file come l'editor semplice. Si copia prima di lanciare, perché l'editor può
     * salvare subito dopo l'apertura. Una copia che non riesce ferma tutto, come nell'editor
     * semplice; se poi l'editor non parte, la copia si toglie dal cestino. Un indirizzo che non è
     * un file su disco (web, allegato di un'altra app) non ha un originale da perdere, quindi non
     * blocca niente.
     */
    private fun openOutside(uri: Uri, id: String) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            var kept: File? = null
            val file = withContext(Dispatchers.IO) { FileTree.fileOf(context, uri) }
            if (settings?.editorBackup != false && file != null) {
                kept = Bin.keep(context, file)
                if (kept == null) {
                    notice = R.string.edit_no_backup
                    return@launch
                }
            }
            if (Editors.open(context, uri, id)) {
                editedOutside = uri
                keptOutside = kept
                return@launch
            }
            kept?.let { Bin.drop(context, it) }
            notice = R.string.edit_app_gone
            settings?.let { updateSettings(it.copy(editorApp = "")) }
            editorFor = uri
            editorAsk = true
        }
    }

    /** Indietro dall'editor semplice: il visualizzatore, che è il solo posto da cui si apre. */
    fun leaveEditor() {
        screen = Screen.Viewer
    }

    /**
     * L'immagine mandata a un'app esterna, finché non si torna.
     *
     * Un editor esterno si apre con `startActivity` e non restituisce un esito: si sa che ha
     * finito quando la finestra di AIV riprende il fuoco.
     */
    private var editedOutside: Uri? = null

    /** La copia messa nel cestino prima di uscire, finché non si sa se è servita. */
    private var keptOutside: File? = null

    /**
     * Si torna da un'app esterna: quello che ha modificato va riletto.
     *
     * ⚠️ L'indirizzo non cambia, quindi senza questa rilettura miniatura e immagine decodificata
     * resterebbero quelle di prima, e sembrerebbe che l'editor non abbia salvato. Si rilegge
     * sempre: la data del file sbaglierebbe con un editor che riscrive senza cambiarla.
     *
     * ⚠️ Qui si decide se la copia resta, confrontando il contenuto del file: un editor esterno
     * non dice se ha salvato, e la data di modifica sbaglia nei due versi (cambia per gli stessi
     * byte, e certi editor la conservano). Chi esce senza toccare niente non trova un doppione
     * nel cestino, e un avviso glielo dice (`1.48`, richiesta dell'utente). È il solo caso:
     * l'editor semplice rifiuta un salvataggio identico (`edit_nothing`).
     */
    private fun backFromOutside() {
        val uri = editedOutside ?: return
        editedOutside = null
        val kept = keptOutside
        keptOutside = null
        val context = getApplication<Application>()
        if (kept != null) {
            viewModelScope.launch {
                val same = withContext(Dispatchers.IO) {
                    FileTree.fileOf(context, uri)?.let { sameBytes(kept, it) } == true
                }
                if (same) {
                    Bin.drop(context, kept)
                    notice = R.string.edit_backup_same
                }
            }
        }
        Thumbs.forget(context, uri)
        if (source == uri) retry()
        afterFileChanged()
    }

    /**
     * Se due file contengono gli stessi byte.
     *
     * La lunghezza si guarda per prima e decide quasi tutti i casi. Poi si legge a blocchi: un
     * file da centinaia di megabyte è raro, e caricarne due in memoria chiuderebbe l'app. In caso
     * di errore risponde 'diversi', che lascia la copia nel cestino.
     */
    private fun sameBytes(one: File, two: File): Boolean {
        if (one.length() != two.length()) return false
        val step = 64 * 1024
        return runCatching {
            one.inputStream().use { a ->
                two.inputStream().use { b ->
                    val left = ByteArray(step)
                    val right = ByteArray(step)
                    var same = true
                    var going = true
                    while (going && same) {
                        val read = a.fill(left)
                        if (b.fill(right) != read) {
                            same = false
                        } else {
                            for (i in 0 until read) {
                                if (left[i] != right[i]) {
                                    same = false
                                    break
                                }
                            }
                        }
                        going = read == step
                    }
                    same
                }
            }
        }.getOrDefault(false)
    }

    /**
     * Salva quello che l'editor semplice ha in mano.
     *
     * ⚠️ Gira nell'ambito del modello: un salvataggio riuscito chiude l'editor, e un lavoro legato
     * alla sua composizione si interromperebbe a metà scrittura, sul file vero. La miniatura si
     * cancella solo dopo un esito buono.
     */
    fun editSave(turns: Int, mirror: Boolean, crop: ImageEdit.Crop) {
        if (editorBusy) return
        val here = screen as? Screen.Editor ?: return
        val context = getApplication<Application>()
        /*
         * ⚠️ Si sovrascrive senza chiedere (`1.08`, richiesta dell'utente): l'originale va nel
         * cestino prima di essere riscritto. Per i formati che si leggono e non si sanno
         * riscrivere (HEIC, AVIF) l'uscita è un JPEG, che non può finire in un file `.heic`:
         * esce una copia accanto, e l'avviso lo dice. Il formato lo guarda il modello, perché
         * il nome del file non riguarda la schermata.
         */
        val way = if (ImageEdit.canOverwrite(here.name)) ImageEdit.Way.OVERWRITE
        else ImageEdit.Way.COPY
        editorBusy = true
        viewModelScope.launch {
            val esito = ImageEdit.save(
                context, here.uri, turns, mirror, crop, way,
                backup = settings?.editorBackup ?: true,
                mark = markPlan(),
                resize = if (resizeOn()) resizePlan() else null
            )
            editorBusy = false
            when (esito) {
                is ImageEdit.Result.Failed -> notice = esito.why
                is ImageEdit.Result.Done -> {
                    notice = when {
                        esito.downloads -> R.string.editor_done_downloads
                        way == ImageEdit.Way.COPY -> R.string.editor_done_copy
                        esito.lossless -> R.string.editor_done_lossless
                        else -> R.string.editor_done
                    }
                    if (way == ImageEdit.Way.OVERWRITE) {
                        Thumbs.forget(context, here.uri)
                        retry()
                    }
                    screen = Screen.Viewer
                    afterFileChanged()
                }
            }
        }
    }

    /**
     * Salva quello che l'editor completo ha in mano.
     *
     * ⚠️ Gira nell'ambito del modello, come [editSave], e qui pesa di più: applicare il conto a
     * venti megapixel dura secondi. Il formato di uscita lo decide la qualità in
     * `ImageEdit.saveLook`: 'Senza perdita' scrive un PNG accanto, le altre due riscrivono il
     * file dov'è.
     *
     * @param beside il tocco lungo su 'Salva' (`2.58`): un file nuovo accanto all'originale. La
     * frase dell'avviso la decide il confronto qui sotto, se il file scritto è quello di partenza.
     */
    fun lookSave(look: Look, beside: Boolean = false) {
        if (editorBusy) return
        val here = screen as? Screen.FullEditor ?: return
        val context = getApplication<Application>()
        editorBusy = true
        viewModelScope.launch {
            val esito = ImageEdit.saveLook(
                context, here.uri, look,
                quality = settings?.editorQuality ?: Quality.DEFAULT,
                backup = settings?.editorBackup ?: true,
                mark = markPlan(),
                resize = if (resizeOn()) resizePlan() else null,
                beside = beside
            )
            editorBusy = false
            when (esito) {
                is ImageEdit.Result.Failed -> notice = esito.why
                is ImageEdit.Result.Done -> {
                    // La miniatura si cancella solo se si è riscritto il file di partenza.
                    val sopra = esito.file.absolutePath ==
                        FileTree.fileOf(context, here.uri)?.absolutePath
                    notice = when {
                        esito.downloads -> R.string.editor_done_downloads
                        sopra -> R.string.editor_done
                        else -> R.string.editor_done_copy
                    }
                    if (sopra) {
                        Thumbs.forget(context, here.uri)
                        retry()
                    }
                    screen = Screen.Viewer
                    afterFileChanged()
                }
            }
        }
    }

    /**
     * Si entra nella ricerca col campo vuoto.
     *
     * I due argomenti restringono la ricerca a una cartella, e li passa solo il menu del FAB di
     * una cartella; senza, si cerca in tutta la galleria.
     */
    fun openSearch(bucket: Long? = null, name: String? = null) {
        gridFilter = MediaKind.ALL
        screen = Screen.Search(bucket, name)
        query = ""
        putList(Folder.Lookup.Found(Folder.Series(emptyList(), 0)))
        source = null
        gridVisited = false
        stopLoad()
    }

    /**
     * Il testo cambia, e i risultati lo seguono dopo una pausa.
     *
     * ⚠️ Senza la pausa ogni lettera lancerebbe una query su tutta la galleria (otto per
     * `panorama`), e la tastiera rallenterebbe. Il testo invece si aggiorna subito, o il campo
     * sembrerebbe guasto.
     */
    fun search(text: String) {
        query = text
        val context = getApplication<Application>()
        val hidden = settings?.hiddenFolders.orEmpty()
        // Il confine si legge dalla schermata a ogni ricerca (vedi [Screen.Search]): una seconda
        // copia sbaglierebbe passando da una ricerca ristretta a una globale.
        val entro = (screen as? Screen.Search)?.bucket
        // Una ricerca nuova è un elenco nuovo: l'anello dell'ultima immagine vista indicherebbe
        // una posizione della lista di prima.
        gridVisited = false
        relist {
            delay(SEARCH_PAUSE_MS)
            Folder.byName(context, text, hidden, entro, settings?.folderSelection ?: FolderSelection()).atSequenceStart()
        }
    }

    /**
     * Rilegge la griglia aperta, dopo che i suoi file sono cambiati sul disco.
     *
     * ⚠️ Non passa da [openGrid], che riporta la griglia in cima: dopo una copia o una rinomina la
     * persona perderebbe il punto in cui era. La lista non si svuota prima, o le miniature
     * lampeggerebbero; l'anello dell'ultima vista invece si spegne, perché le righe del
     * MediaStore cambiano posizione quando i file si spostano o si rinominano.
     */
    fun reloadGrid() {
        // Le griglie sono tre: la cartella, i risultati di una ricerca (che dopo una rinomina
        // cambiano) e il cestino.
        gridVisited = false
        val context = getApplication<Application>()
        when (val here = screen) {
            is Screen.Grid -> relist { Folder.newestIn(context, here.bucket).atSequenceStart() }
            is Screen.Search -> {
                val hidden = settings?.hiddenFolders.orEmpty()
                val text = query
                val entro = here.bucket
                relist {
                    Folder.byName(context, text, hidden, entro, settings?.folderSelection ?: FolderSelection()).atSequenceStart()
                }
            }
            // Il cestino si rilegge con [binLookup], non dalla query delle cartelle.
            Screen.Bin -> relist { binLookup(context) }
            else -> Unit
        }
    }

    /**
     * Le cartelle di immagini del telefono, e `null` finché la prima lettura non è finita.
     *
     * ⚠️ Vive nel modello, come [treePath] (`1.46`): dalla schermata iniziale si esce a ogni
     * immagine aperta, e un `remember` della schermata rifarebbe la query a ogni ritorno. Non è
     * filtrato: le nascoste le toglie la schermata, così nasconderne una si vede subito senza
     * interrogare il MediaStore.
     */
    var buckets: List<Folder.Bucket>? by mutableStateOf(null)
        private set

    /**
     * Per quale stato delle cose l'elenco qui sopra è stato letto: permesso e [outsideStamp].
     *
     * ⚠️ È questo confronto a rendere la lettura rara: l'effetto di avvio della schermata riparte
     * a ogni ritorno. Il permesso dell'ultima volta serve a [outsideChange], che arriva quando la
     * schermata iniziale è già visibile e nessuno lo può chiedere.
     */
    private var bucketsFor: Pair<Boolean, Int>? = null

    private var bucketsJob: Job? = null

    /**
     * Il colore scelto per ogni cartella che ne ha uno, per la schermata iniziale.
     *
     * ⚠️ Si legge con l'elenco e si aggiorna a mano (`1.87`): [readBuckets] ha una guardia e non
     * riparte al ritorno da una cartella, quindi [tintFolder] scrive anche qui. Si legge anche
     * con [FolderColour.NONE]: è una riga sola dell'archivio, e legarla all'impostazione vorrebbe
     * dire ricaricare l'elenco quando cambia.
     */
    var folderTints: Map<Long, Int> by mutableStateOf(emptyMap())
        private set

    /**
     * La copertina scelta a mano per ogni cartella che ne ha una, per la schermata iniziale.
     *
     * Come [folderTints]: stessa guardia, e la aggiorna a mano [applyCover].
     */
    var folderCovers: Map<Long, Uri> by mutableStateOf(emptyMap())
        private set

    /**
     * Rilegge le cartelle, ma solo se è cambiato qualcosa che le riguarda.
     *
     * Le chiavi sono il permesso e [outsideStamp], la notizia del MediaStore. Le cartelle
     * nascoste non lo sono: non cambiano quello che c'è sul disco.
     */
    fun readBuckets(granted: Boolean) {
        val key = granted to outsideStamp
        if (bucketsFor == key) return
        bucketsFor = key
        val context = getApplication<Application>()
        // ⚠️ La lettura precedente si annulla: con due query in corso, l'ultima a rispondere
        // può non essere l'ultima partita.
        bucketsJob?.cancel()
        bucketsJob = viewModelScope.launch {
            val letti = if (granted) Folder.buckets(context) else emptyList()
            if (granted && storedPreferences(context)[INCLUDED_INITIALIZED] != true) {
                val defaults = withContext(Dispatchers.IO) {
                    defaultIncludedFolders(Tree.roots(context).map { it.file }, letti)
                }
                SettingsStore.initializeIncluded(context, defaults)
            }
            buckets = letti
            /*
             * ⚠️ Qui si potano le copertine orfane (richiesta dell'utente): il `BUCKET_ID` viene
             * dal percorso, e una rinomina fatta da un'altra app lascia una copertina legata a una
             * cartella che non esiste più. Serve l'elenco vero dei bucket, appena letto. Non è un
             * lavoro programmato (regola `d-cestino-chiusa`: niente fuori dall'app in primo
             * piano); il periodo di grazia è su [FolderCovers.sweep].
             */
            FolderCovers.sweep(context, letti.map { it.id }.toSet())
        }
        // ⚠️ Tinte e copertine in lanci a sé: arrivano prima dell'elenco, che passa dal
        // MediaStore, e in coda una cartella colorata si vedrebbe grigia per il tempo della
        // query.
        viewModelScope.launch { folderTints = FolderTints.all(context) }
        viewModelScope.launch { folderCovers = FolderCovers.all(context) }
    }

    /**
     * Il Feed della schermata iniziale, e `null` finché la prima lettura non è finita. Vedi
     * [Feed].
     *
     * Vive nel modello per la ragione di [buckets]: tornando da un'immagine il flusso deve essere
     * dov'era.
     */
    var feed: Feed.Page? by mutableStateOf(null)
        private set

    /** Per che cosa è stato letto [feed]: permesso, [outsideStamp], cartelle, data e [peeking]. */
    private var feedFor: List<Any>? = null

    /**
     * Gli ultimi argomenti di [readFeed], per [outsideChange], che non li può chiedere alla
     * schermata; `null` mentre il Feed non è visibile ([leaveFeed]).
     */
    private var feedArgs: Triple<Boolean, FolderSelection, FeedDate>? = null

    /**
     * La schermata iniziale ha lasciato il Feed (un'altra vista, o il Menu 'Start' davanti).
     *
     * ⚠️ `6.28`, rilievo D16: senza azzerare [feedArgs], ogni notizia del disco rileggerebbe
     * tutte le immagini del telefono anche con un'altra vista (presidiato da
     * `FeedFuoriPlayTest`). Tornando al Feed la schermata richiede la lettura, e la chiave di
     * [readFeed], che contiene [outsideStamp], la rifà se nel frattempo è cambiato qualcosa.
     */
    fun leaveFeed() {
        feedArgs = null
    }

    private var feedJob: Job? = null

    /**
     * Rilegge il Feed senza guardare la chiave, dopo che un'operazione sulla sua selezione ha
     * cambiato i file (vedi [readFeed]).
     */
    fun rereadFeed() {
        feedFor = null
        feedArgs?.let { (granted, selection, date) -> readFeed(granted, selection, date) }
    }

    /**
     * Rilegge il Feed, ma solo quando è cambiato qualcosa da cui dipende.
     *
     * ⚠️ Al contrario di [readBuckets], cartelle e data sono chiavi: cambiano quali immagini il
     * flusso contiene e in che ordine. Il flusso vecchio resta visibile finché arriva il nuovo.
     * [peeking] si legge qui, così nessuno può passarne uno vecchio; la schermata richiama quando
     * cambia, perché è una chiave anche della sua lettura.
     */
    fun readFeed(granted: Boolean, selection: FolderSelection, date: FeedDate) {
        feedArgs = Triple(granted, selection, date)
        val peek = peeking
        val key = listOf(granted, outsideStamp, selection, date, peek)
        if (feedFor == key) return
        feedFor = key
        val context = getApplication<Application>()
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            feed = Folder.feed(context, selection, date, peek) ?: Feed.Page(emptyList(), emptyList())
        }
    }

    /**
     * Dov'è la navigazione della vista 'Cartelle di sistema', e `null` vuol dire in cima.
     *
     * ⚠️ Vive nel modello: si esce dalla schermata iniziale a ogni immagine aperta, e uno stato
     * del composabile riporterebbe in cima a ogni ritorno. Non si salva: riaprendo l'app si
     * riparte dalla radice.
     */
    var treePath: String? by mutableStateOf(null)
        private set

    /**
     * Se dalla cartella corrente si può risalire, cioè se il gesto Indietro ha un posto dove
     * portare.
     *
     * ⚠️ Si calcola quando si naviga: richiede `canonicalPath`, che legge il disco, e una
     * proprietà calcolata lo farebbe a ogni ricomposizione sul thread principale.
     */
    var treeClimbing: Boolean by mutableStateOf(false)
        private set

    /** Entra in una cartella, o torna all'elenco delle memorie con `null`. */
    fun treeTo(path: String?) {
        treePath = path
        treeClimbing = path != null &&
            (treeRoots.size > 1 || Tree.parent(File(path), treeRoots) != null)
    }

    /** Risale di una cartella: dalla radice torna all'elenco delle memorie, se ce n'è uno. */
    fun treeUp() {
        val here = treePath ?: return
        treeTo(Tree.parent(File(here), treeRoots)?.absolutePath)
    }

    /**
     * Le memorie, lette una volta sola e solo quando servono: chi non apre mai quella vista non
     * paga la domanda al sistema all'avvio.
     */
    private val treeRoots: List<Tree.Root> by lazy { Tree.roots(getApplication()) }

    /**
     * Se [path] è una delle memorie del telefono o una cartella al suo interno (`6.29`).
     *
     * ⚠️ Si confrontano i percorsi canonici, senza `..` e senza collegamenti:
     * `/storage/emulated/0/../../data` comincia con la memoria ma conduce altrove.
     */
    internal fun insideRoots(path: String): Boolean {
        val asked = runCatching { File(path).canonicalPath }.getOrNull() ?: return false
        return treeRoots.any { root ->
            val base = runCatching { root.file.canonicalPath }.getOrNull() ?: return@any false
            asked == base || asked.startsWith("$base/")
        }
    }

    /**
     * Un'immagine toccata in 'Cartelle di sistema' o nel Feed (`6.02`).
     *
     * ⚠️ La serie arriva già fatta da chi ha disegnato l'elenco, e non si rilegge: l'ordine che si
     * sfoglia è quello che si aveva davanti. Il ritorno va alla schermata iniziale, che ritrova
     * la cartella perché [treePath] vive nel modello. Nessun anello dell'ultima vista
     * (`gridVisited`): qui la griglia non c'è.
     */
    fun openFromTree(items: List<Uri>, index: Int) {
        val whole = Folder.Series(items, index)
        if (whole.at(index) == null) return
        viewerBack = HOME
        screen = Screen.Viewer
        showAt(whole, index, Arrival.TAPPED)
    }

    /**
     * L'immagine toccata nella griglia.
     *
     * [index] è la posizione nell'ordine di lettura, lo stesso che la griglia mostra: nessuna
     * conversione.
     */
    fun openFromGrid(index: Int) {
        // Vale per le tre griglie (cartella, ricerca, cestino): il ritorno va alla schermata da
        // cui si è partiti.
        val grid = screen.takeIf { it is Screen.Grid || it is Screen.Search || it is Screen.Bin }
            ?: return
        val current = series ?: return
        // Prima di cambiare schermata, o un indice vuoto lascerebbe una schermata vuota.
        if (current.at(index) == null) return
        viewerBack = grid
        // ⚠️ Si scrive al tocco, il solo momento in cui è certo che un'immagine della griglia si
        // sta aprendo.
        gridVisited = true
        screen = Screen.Viewer
        showAt(current, index, Arrival.TAPPED)
    }

    /**
     * Fuori dalla griglia: si torna all'elenco delle cartelle, da dove ci si è arrivati.
     *
     * Passa da [HOME], come gli altri ritorni (vedi il suo KDoc).
     */
    fun leaveGrid() {
        screen = HOME
        putList(null)
    }

    /**
     * Indietro dal visualizzatore, dove [viewerBack] dice.
     *
     * ⚠️ Tornando a una griglia la serie resta, quindi non si passa da [goHome]: la griglia la
     * ritrova pronta, con l'indice dell'immagine da cui si esce, dove si riposiziona. Vale anche
     * per la ricerca, che ritrova risultati e testo.
     */
    fun backFromViewer() {
        when (val dest = viewerBack) {
            null -> Unit
            is Screen.Grid, is Screen.Search, Screen.Bin -> {
                screen = dest
                source = null
                stopLoad()
                // ⚠️ Dopo l'assegnazione di `screen`: [reloadGrid] guarda lì per sapere quale
                // griglia rileggere, e prima troverebbe `Screen.Viewer`. Vedi [afterFileAdded].
                if (gridStale) {
                    gridStale = false
                    reloadGrid()
                }
            }
            else -> goHome()
        }
    }

    /**
     * Mentre si guardava un'immagine è comparso un file nuovo nella cartella.
     *
     * ⚠️ Non si rilegge subito: la griglia non è visibile, e la rilettura si fa al ritorno. Non si
     * passa da [afterFileChanged], che conserva la posizione nella cartella: un file nuovo entra in
     * cima all'ordine per data, e la stessa posizione mostrerebbe un'altra immagine.
     * La chiamano l'esportazione di un fotogramma (`AnimatedBar`) e 'Converti/Esporta'
     * (`ConvertDialog`).
     */
    fun afterFileAdded() {
        gridStale = true
    }

    /** Se la griglia dietro al visualizzatore è da rileggere. Vedi [afterFileAdded]. */
    private var gridStale = false

    /*
     * ══════════════ Quando i file cambiano per mano di qualcun altro ══════════════
     *
     * Niente tasto 'Aggiorna': chiederebbe a chi guarda di sapere che l'elenco è vecchio. Il
     * MediaStore avvisa quando qualcuno scrive, e ascoltarlo costa un osservatore.
     */

    /**
     * Quante volte il disco è cambiato da fuori, da quando l'app è aperta.
     *
     * Un contatore e non un booleano: le schermate che leggono da sé lo usano come chiave di un
     * `LaunchedEffect`, che deve cambiare a ogni notizia.
     */
    var outsideStamp: Int by mutableStateOf(0)
        private set

    /**
     * Se la griglia sta facendo qualcosa che una rilettura rovinerebbe: oggi, una selezione
     * viva.
     *
     * ⚠️ La selezione della griglia è `remember(items)`, quindi una lista nuova la azzera: un file
     * arrivato da fuori cancellerebbe trenta immagini spuntate. La rilettura si rimanda, e si fa
     * qui sotto quando la selezione si scioglie.
     */
    var gridBusy: Boolean = false
        set(value) {
            field = value
            if (!value && gridStale && screen.isGrid()) {
                gridStale = false
                reloadGrid()
            }
        }

    /**
     * Se nel Feed c'è una selezione in corso (`6.27`, rilievo D15).
     *
     * ⚠️ È [gridBusy] per il Feed: una pagina nuova è una chiave nuova del gesto di selezione, e
     * interromperebbe un trascinamento; le immagini arrivate in cima sposterebbero le altre. La
     * rilettura rimandata si fa quando la selezione si scioglie.
     */
    var feedBusy: Boolean = false
        set(value) {
            field = value
            if (!value && feedStale) {
                feedStale = false
                if (screen is Screen.Folders) feedArgs?.let { (granted, selection, date) -> readFeed(granted, selection, date) }
            }
        }

    /** Il Feed è cambiato fuori mentre c'era una selezione: si rilegge quando finisce. */
    private var feedStale = false

    /**
     * L'osservatore del MediaStore, attivo per tutta la vita del modello.
     *
     * ⚠️ `notifyForDescendants` acceso: si registra la collezione, e le notizie arrivano sulla
     * singola riga. `Files` e non `Images`, perché arrivano anche video. Si registra qui e non
     * in `init`, che è più in alto: le proprietà si inizializzano in ordine, e da là
     * l'osservatore non esisterebbe ancora (errore di compilazione). Attivo per tutta la vita
     * del modello, perché acceso e spento a ogni schermata perderebbe le notizie dei passaggi.
     */
    private val outsideWatch = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) = outsideChange()
    }.also {
        getApplication<Application>().contentResolver
            .registerContentObserver(Folder.TABLE, true, it)
    }

    private var outsideJob: Job? = null

    /**
     * Qualcuno ha scritto sul disco: si rilegge quello che è visibile.
     *
     * ⚠️ Con una pausa che riparte a ogni notizia: una copia di venti file dà venti notizie in due
     * secondi, e si legge una volta sola. Passano di qui anche le scritture dell'app: la
     * rilettura che segue trova le stesse righe, e non si vede.
     */
    private fun outsideChange() {
        outsideJob?.cancel()
        outsideJob = viewModelScope.launch {
            delay(OUTSIDE_PAUSE_MS)
            outsideStamp++
            when {
                // Il cestino non si rilegge da qui: in AIV GitHub vive nella cartella privata
                // dell'app, che il MediaStore non guarda.
                screen == Screen.Bin -> Unit
                screen.isGrid() -> if (gridBusy) gridStale = true else reloadGrid()
                // Mentre si guarda un'immagine si segna soltanto, come in [afterFileAdded].
                screen == Screen.Viewer || screen is Screen.Editor -> gridStale = true
                // ⚠️ La schermata iniziale mostra cartelle, e un file arrivato da fuori può averne
                // creata una. Si rilegge col permesso dell'ultima volta, che sa solo la schermata;
                // se non ha ancora letto, lo farà entrando.
                screen is Screen.Folders -> {
                    bucketsFor?.let { readBuckets(it.first) }
                    // E il Feed, con gli ultimi argomenti: un'immagine arrivata da fuori va in
                    // cima al flusso. Non durante una selezione ([feedBusy]).
                    if (feedBusy) feedStale = true
                    else feedArgs?.let { (granted, selection, date) -> readFeed(granted, selection, date) }
                }
                else -> Unit
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().contentResolver.unregisterContentObserver(outsideWatch)
    }

    /**
     * L'immagine che si sta guardando non è più a quell'indirizzo: si rilegge la cartella e si
     * mostra quella che ha preso il suo posto.
     *
     * ⚠️ Una funzione sola per sposta, rinomina ed elimina, che conserva la posizione: dopo
     * un'eliminazione o uno spostamento c'è l'immagine seguente, dopo una rinomina la stessa con
     * l'indirizzo nuovo (l'ordine è per data). Se la cartella si è svuotata, o non c'è un elenco
     * da rileggere (un'immagine aperta da fuori), si torna indietro, o comparirebbe l'errore di
     * caricamento. `gridVisited` resta acceso, al contrario di [reloadGrid]: la posizione si
     * riscrive subito con quella giusta.
     */
    fun afterFileChanged() {
        val context = getApplication<Application>()
        val place = series?.index ?: 0
        val from = viewerBack
        // ⚠️ Nel lavoro della lista ([listJob]), perché scrive la lista: una strisciata o
        // un'uscita durante la rilettura la annullano. Il [putList] di [showAt] annulla questo
        // stesso lavoro mentre finisce, senza effetti.
        listJob?.cancel()
        listJob = viewModelScope.launch {
            val fresh = when (from) {
                is Screen.Grid -> Folder.newestIn(context, from.bucket)
                is Screen.Search ->
                    Folder.byName(context, query, settings?.hiddenFolders.orEmpty(), from.bucket, settings?.folderSelection ?: FolderSelection())
                Screen.Bin -> binLookup(context)
                else -> null
            }
            val reading = fresh.oriented()?.seriesOrNull
            if (reading == null || reading.items.isEmpty()) {
                backFromViewer()
                return@launch
            }
            // `listed` lo riscrive [showAt], nell'ordine grezzo.
            showAt(reading, place.coerceIn(0, reading.items.lastIndex), Arrival.LEAFED)
        }
    }

    /**
     * L'immagine seguente o precedente della serie, se c'è.
     *
     * ⚠️ La serie si porta avanti e non si ricerca: ricostruirla interrogherebbe il database a
     * ogni strisciata, e una cartella cambiata nel frattempo si rinumererebbe mentre la si
     * sfoglia. Non ricomincia dall'inizio: all'ultima immagine la strisciata non fa niente.
     * [delta] si conta nell'ordine di lettura di [folder]: l'impostazione gira la serie, non il
     * gesto.
     *
     * Con 'Sfoglia solo le immagini' (`0.86`) cambia solo di quanto avanza il dito: una serie
     * filtrata cambierebbe anche il contatore e la griglia, che i video li mostrano. Il tocco
     * sulla griglia non passa di qui ([openFromGrid]), e arriva con [Arrival.TAPPED]: se il video
     * parte lo decide `Settings.clipAutoplay`. Il conto del salto è in `Folder.Series.stepping`,
     * dove lo vede anche la schermata per disegnare le vicine.
     */
    fun step(delta: Int) {
        val current = series ?: return
        showAt(
            current,
            current.index + current.stepping(delta, settings?.imagesOnly == true),
            Arrival.LEAFED
        )
    }

    /**
     * Mostra l'immagine in quella posizione della serie di lettura.
     *
     * Condivisa fra la strisciata ([step]) e il tocco sulla griglia ([openFromGrid]): due copie
     * divergerebbero su come si riscrive [listed]. Fuori dalla serie non fa niente.
     */
    private fun showAt(current: Folder.Series, index: Int, from: Arrival) {
        val uri = current.at(index) ?: return
        source = uri
        // La vicina di un indirizzo riscritto è già in `https`: un errore non viene più dal
        // traffico in chiaro.
        upgraded = false
        // `current` è nell'ordine di lettura, `listed` in quello grezzo: si rigira con la
        // stessa funzione, che è la sua stessa inversa.
        putList(Folder.Lookup.Found(current.copy(index = index)).oriented())
        startLoad(uri, from)
        /*
         * ⚠️ La finestra del web si rifà a ogni passo: una serie remota è larga tre e non ha fine
         * nota. L'indice è già spostato, quindi la strisciata risponde subito e la rete arriva
         * dopo. Non si rifà quando la serie è già intera (`WebSeries.WHOLE`), o si riscaricherebbe
         * la stessa pagina a ogni immagine.
         */
        if (WebSeries.isWeb(uri) && webRule !in WebSeries.WHOLE) relist { webWindow(uri) }
    }

    /** Chiamata quando la finestra del permesso ha avuto risposta, qualunque sia. */
    fun folderAnswered(allowed: Boolean) {
        val context = getApplication<Application>()
        viewModelScope.launch { FolderAsk.remember(context) }
        // Permesso dato con un'immagine già aperta: la cartella si cerca adesso, senza far
        // riaprire la stessa immagine.
        val uri = source
        if (allowed && uri != null) relist { Folder.seriesAround(context, uri) }
    }

    fun goHome() {
        screen = HOME
        source = null
        putList(null)
        stopLoad()
    }

    /** Le impostazioni, dall'inizio. */
    fun openSettings() {
        screen = Screen.Settings
    }

    /**
     * Accende o spegne la filigrana al salvataggio: è lo stesso interruttore delle sue
     * impostazioni, con la stessa chiave (scelta dell'utente, giro della `2.70`; `Rules.md`
     * § '⚙️ Dove va un'impostazione, e chi la deve trovare').
     */
    fun setMark(on: Boolean) {
        val now = settings ?: return
        updateSettings(now.copy(markOn = on))
    }

    /** Fuori dalle impostazioni: al visualizzatore se un'immagine è aperta, altrimenti a [HOME]. */
    fun leaveSettings() {
        screen = if (state is ViewerState.Ready) Screen.Viewer else HOME
    }

    private fun applySettings(next: Settings) {
        if (next.folderMode == FolderMode.INCLUDED && peeking) peek(false)
        settings = next
    }

    fun updateSettings(next: Settings) {
        val before = settings
        // Una vista cambiata nelle impostazioni chiude il Feed della scorciatoia (`6.10`).
        if (before != null && before.folderView != next.folderView) viewOnce = null
        // ⚠️ Cartelle nascoste cambiate: l'azione cartella del launcher potrebbe nominarne una
        // (`Shortcuts`).
        if (before != null && (
                before.folderMode != next.folderMode ||
                    before.hiddenFolders != next.hiddenFolders ||
                    before.hiddenExactFolders != next.hiddenExactFolders ||
                    before.includedFolders != next.includedFolders
                )
        ) {
            Shortcuts.dropFolder(getApplication())
        }
        applySettings(next)
        viewModelScope.launch { SettingsStore.save(getApplication(), next) }
    }

    fun forgetRecents() {
        viewModelScope.launch { Recents.clear(getApplication()) }
    }

    /**
     * Fa tornare tutto quello che si mostra una volta sola, come alla prima installazione.
     *
     * Vive nel modello, come `forgetRecents`, perché scrive nell'archivio. Gira su tutte le voci
     * di [Hint], così copre anche quelle nuove. Azzera anche [FolderAsk] (`1.16`, richiesta
     * dell'utente): il tasto promette 'i suggerimenti e gli avvisi', e un 'no' alla cartella
     * altrimenti resterebbe per sempre. Non ci sono un avviso alla prima eliminazione (la
     * conferma compare ogni volta) né uno al primo salvataggio dell'editor.
     */
    fun resetHints() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            Hint.entries.forEach { it.forget(context) }
            FolderAsk.forget(context)
        }
    }

    /**
     * Il messaggio d'errore, con un caso in più che senza [upgraded] sarebbe muto.
     *
     * ⚠️ Un indirizzo in chiaro Android lo blocca, e l'app lo ha già riscritto in `https`: se
     * anche quello non risponde, il messaggio lo dice, o la persona cercherebbe il difetto nella
     * propria rete.
     */
    private fun reasonFor(reason: LoadResult.Reason): Int =
        if (upgraded && reason == LoadResult.Reason.OPEN_FAILED) R.string.open_cleartext
        else reason.messageRes()

    private fun LoadResult.Reason.messageRes(): Int = when (this) {
        LoadResult.Reason.NO_IMAGE -> R.string.no_image
        LoadResult.Reason.UNSUPPORTED -> R.string.unsupported
        LoadResult.Reason.TOO_LARGE -> R.string.too_large
        LoadResult.Reason.OPEN_FAILED -> R.string.open_failed
    }
}

class ViewerActivity : ComponentActivity() {

    private val model: ViewerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        /*
         * ⚠️ La guardia è il modello e non `savedInstanceState` (`1.81`): la domanda è se
         * l'intento è già stato letto. Col processo chiuso in secondo piano e ricreato il
         * `Bundle` c'è ma il modello è nuovo, e un collegamento aperto da fuori finiva a [HOME].
         * Alla rotazione il modello resta e ha già l'immagine, e rileggere l'intento la
         * caricherebbe una seconda volta.
         */
        if (!model.intentRead) model.handleIntent(intent, fresh = true)
        // Le azioni del launcher si riscrivono a ogni creazione, così le etichette seguono la
        // lingua in vigore (`6.10`; il perché è su `Shortcuts`).
        Shortcuts.publish(this)
        // ⚠️ Il tema si legge qui, fuori da `AivApp`, perché deve avvolgerlo. Finché le
        // impostazioni non sono arrivate vale il sistema, che è anche il valore di fabbrica.
        setContent {
            val chosen = model.settings?.uiTheme ?: UiTheme.SYSTEM
            val scuro = chosen.isDark()
            /*
             * ⚠️ Le barre di sistema seguono il tema dell'app (`1.80`). `SystemBarStyle.auto`, che
             * `enableEdgeToEdge()` usa di serie, legge il buio dalla configurazione del sistema:
             * con 'Chiaro' o 'Scuro' scelti contro il sistema le icone di stato e di navigazione
             * finirebbero chiare su fondo chiaro. Per questo `dark` e `light`, con fondo
             * trasparente. Si rifà quando il tema risolto cambia, perché si cambia senza ricreare
             * l'activity.
             * La linea dei gesti è bianca sopra il menu inferiore (`4.15`, nota D del giro della
             * `4.04`): sull'accento quella scura del tema chiaro quasi sparisce, e il sistema la
             * colora solo intera. Che cosa c'è sotto la linea lo dice [BarStage].
             */
            val sottoBarra = BarStage.under
            LaunchedEffect(scuro, sottoBarra) {
                val stile = if (scuro) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(
                    statusBarStyle = stile,
                    navigationBarStyle = if (sottoBarra) SystemBarStyle.dark(Color.TRANSPARENT) else stile
                )
            }
            /*
             * ⚠️ A tutto schermo (`3.70`, scelte B1, B2 e A4; `3.71`, voce `3.70-01`): le barre di
             * sistema si nascondono, e uno scorrimento dal bordo le richiama per qualche secondo.
             * La regola (sul telefono in orizzontale tutta l'app, in verticale il solo
             * visualizzatore) è in [Adaptive.immersive]; qui si dice quale schermata è visibile.
             * Con le barre nascoste `safeDrawing` non le conta più, e lo spazio torna da sé.
             */
            val conf = LocalConfiguration.current
            val immersivo = Adaptive.immersive(
                conf.screenWidthDp, conf.screenHeightDp, conf.smallestScreenWidthDp,
                viewer = model.screen == Screen.Viewer
            )
            LaunchedEffect(immersivo) {
                val barre = WindowCompat.getInsetsController(window, window.decorView)
                if (immersivo) {
                    barre.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    barre.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    barre.show(WindowInsetsCompat.Type.systemBars())
                }
            }
            AivTheme(darkTheme = scuro) {
                // ⚠️ Accanto al tema si forniscono i valori che chiedono finestre a cui le
                // impostazioni non arrivano: 'Sfocatura dietro i pannelli' ([LocalAivDepth]), le
                // etichette sotto le icone e gli ordini dei tasti ([LocalPadLook]). Finché le
                // impostazioni non sono arrivate valgono i valori di fabbrica.
                val look = model.settings?.let {
                    PadLook(
                        it.padLabels, it.menuOrder, it.pickOrder, it.turnOrder, it.stepOrder,
                        it.modOrder, it.hand
                    )
                } ?: PadLook()
                // La vista delle cartelle, per la finestra della destinazione (vedi
                // [LocalDestLook]).
                val dove = model.settings?.let {
                    // Col Feed la finestra mostra le cartelle come copertine: sceglie una
                    // cartella, e il Feed non ne mostra (`6.02`).
                    DestLook(
                        if (it.folderView == FolderView.FEED) FolderView.GRID else it.folderView,
                        it.folderColumns, it.folderCount, it.listCount, it.listText, it.folderMode
                    )
                } ?: DestLook()
                // Il minuto di 'Mostra nascoste', che serve alla finestra delle destinazioni
                // (vedi [LocalPeek]). E la pillola, coi valori in vigore e non quelli scritti:
                // sul tablet niente pillola al posto del FAB, e sotto Android 12 niente vetro
                // (vedi [PillLook]).
                val pillola = model.settings?.let {
                    PillLook(
                        mode = pillMode(it.mainControl, it.phonePill, LocalConfiguration.current.smallestScreenWidthDp),
                        fill = pillFillIn(it.pillFill),
                        bar = barIn(it.mainControl, LocalConfiguration.current.smallestScreenWidthDp),
                        glass = it.glass,
                        corner = cornerIn(it.mainControl, LocalConfiguration.current.smallestScreenWidthDp),
                        cornerRound = it.cornerRound
                    )
                } ?: PillLook()
                CompositionLocalProvider(
                    LocalAivDepth provides (model.settings?.panelDepth ?: PanelDepth.NONE),
                    LocalPillLook provides pillola,
                    LocalPadLook provides look,
                    LocalDestLook provides dove,
                    LocalPeek provides model.peeking
                ) {
                    AivApp(model, onPicked = ::deliver)
                }
            }
        }
    }

    /**
     * ⚠️ Gli appunti si guardano qui e non in `onCreate`: da Android 10 un'app li legge solo
     * mentre ha il fuoco, che alla creazione non ha ancora. Il fuoco va e viene a ogni finestra
     * di sistema, e il modello ignora le chiamate successive.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        model.windowFocus(hasFocus)
    }

    /**
     * Un intento nuovo mentre l'app è già aperta (l'activity è `singleTop`): una condivisione da
     * un'altra app, oppure il ritorno dall'icona del launcher.
     *
     * ⚠️ `fresh = false`: l'app sta già facendo qualcosa, e un intento senza indirizzo non deve
     * riportare alla schermata iniziale. Il perché è su [ViewerViewModel.handleIntent].
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        model.handleIntent(intent, fresh = false)
    }

    /**
     * Consegna l'immagine scelta a chi ha aperto AIV come selettore, e chiude (`1.89`).
     *
     * ⚠️ L'indirizzo si prepara con [ImageActions.readableOutside], la strada della
     * condivisione: un `file://` che esce dal processo fa scattare `FileUriExposedException` da
     * Android 7, quindi diventa una copia servita dal FileProvider; un `content://` passa com'è.
     * Il permesso viaggia con l'intento (`FLAG_GRANT_READ_URI_PERMISSION`), o chi riceve non può
     * aprire l'indirizzo. Se la preparazione non riesce, l'app resta aperta con un avviso.
     * `EXTRA_ALLOW_MULTIPLE` non è gestito: si consegna un file solo, risposta comunque valida.
     */
    private fun deliver(uri: Uri) {
        lifecycleScope.launch {
            val fuori = ImageActions.readableOutside(this@ViewerActivity, listOf(uri)).firstOrNull()
            if (fuori == null) {
                Notices.say(resources.getQuantityString(R.plurals.op_failed, 1, 1), NOTICE_LONG_MS)
                return@launch
            }
            setResult(
                RESULT_OK,
                Intent().setData(fuori).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
            finish()
        }
    }
}

@Composable
private fun AivApp(model: ViewerViewModel, onPicked: (Uri) -> Unit = {}) {
    // ⚠️ Prima di tutto il resto: la fabbrica va dichiarata prima che un'immagine chieda il
    // caricatore, o si prende quello predefinito e le miniature passano dalla decodifica
    // normale. Il perché di un caricatore nostro è in `Thumbs`.
    setSingletonImageLoaderFactory { context -> Thumbs.loader(context) }
    val settings = model.settings
    if (settings == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        return
    }
    /*
     * ⚠️ Qui l'impostazione arriva a Coil: un `Fetcher` non vede le preferenze, e questo è il
     * solo posto in cui ci sono le preferenze e il caricatore è già dichiarato (perché una
     * variabile globale: vedi [Thumbs.inGpu]). `SideEffect` è il blocco fatto per scrivere fuori
     * dalla composizione.
     */
    SideEffect { Thumbs.inGpu = settings.gpuThumbs }
    /*
     * ⚠️ Il controllo degli aggiornamenti gira all'apertura (scelta D1): niente lavoro in
     * sottofondo e niente notifiche, come lo svuotamento del cestino qui sotto. La chiave è
     * l'interruttore, quindi riaccenderlo controlla subito; il limite di una volta al giorno lo
     * tiene [Updater.check].
     */
    val contesto = LocalContext.current
    LaunchedEffect(settings.autoUpdate) {
        if (settings.autoUpdate) Updater.check(contesto, asked = false)
    }
    /*
     * ⚠️ Lo svuotamento automatico del cestino gira qui e solo qui (scelta `aperta` a
     * `d-cestino-chiusa`: niente fuori dall'app aperta in primo piano), senza lavori programmati.
     * La chiave è la durata scelta: passa all'avvio e a ogni cambio, che ha effetto subito. Con
     * 'Mai' [Bin.sweep] non legge il disco. Il conto dei file tolti non si mostra: sarebbe un
     * avviso a ogni avvio per una scelta già fatta.
     */
    val perPulizia = LocalContext.current
    LaunchedEffect(settings.binKeep) { Bin.sweep(perPulizia, settings.binKeep.days) }
    /*
     * ⚠️ Il selettore e l'avviso vengono prima della scelta della schermata: il selettore lo
     * aprono due schermate, e l'esito di un salvataggio arriva a editor già chiuso. Dentro una
     * schermata si perderebbero al cambio, proprio quando devono comparire.
     */
    if (model.editorAsk) {
        EditorPicker(
            chosen = settings.editorApp,
            onPick = { model.editorChosen(it) },
            onDismiss = { model.editorSkip() }
        )
    }
    /*
     * La pagina della filigrana si apre qui, per la ragione del selettore (`2.75`): la apre il
     * tocco lungo su un tasto dell'editor, che deve restare aperto. Vedi [MarkSheet].
     */
    if (model.markSetup) {
        MarkSheet(
            settings = settings,
            onChange = { model.updateSettings(it) },
            onDismiss = { model.closeMarkSetup() }
        )
    }
    // La finestra dell'indirizzo dell'azione del launcher (`6.10`), qui come le due sopra: si
    // apre sopra la schermata iniziale e non dipende da quale schermata c'è sotto.
    if (model.addressAsked) {
        AddressDialog(
            recents = model.recents,
            onOpen = { model.open(it) },
            onOpenPage = { model.openPage(it) },
            onForget = { model.forgetRecents() },
            onDismiss = { model.addressAsked = false }
        )
    }
    val context = LocalContext.current
    val said = model.notice
    if (said != null) {
        // ⚠️ L'avviso si azzera dopo averlo mostrato, o non si potrebbe mostrare due volte di
        // fila la stessa frase (due copie salvate). Passa da [Notices] (scelta `casa` a
        // `d-avvisi`), che prende testo e non risorse: si risolve qui, nella composizione.
        val frase = stringResource(said)
        LaunchedEffect(said) {
            Notices.say(frase, NOTICE_LONG_MS)
            model.noticeShown()
        }
    }
    /*
     * ⚠️ L'offerta di annullare un'eliminazione è qui, sopra la transizione fra schermate:
     * resta disponibile anche cambiando cartella (richiesta dell'utente, giro della `1.59`).
     * Che cosa la tiene in vita è in testa a [Undo]. Il conto alla rovescia riparte a ogni offerta
     * nuova: la chiave è l'elenco, e i nomi nel cestino sono unici (`FileTree.freeName`).
     */
    val disfare = Undo.offerta
    val scope = rememberCoroutineScope()
    /*
     * ⚠️ L'offerta è una riga di [Notices], che ne governa la durata: qui si compone la frase e
     * si dice che cosa fa il tasto. `onGone` chiude l'offerta alla scadenza o al tocco, così c'è
     * una sola attesa. Il tasto si prende l'offerta prima che sparisca: fra il tocco e il lavoro
     * passa un fotogramma. Il valore di riserva della frase non si vede mai, perché l'effetto
     * gira solo con l'offerta presente.
     */
    val annulla = stringResource(R.string.pick_undo)
    val frase = disfare?.let { pluralStringResource(it.kind.done, it.count, it.count) }
    LaunchedEffect(disfare) {
        val quale = disfare ?: return@LaunchedEffect
        Notices.offer(
            text = frase ?: "",
            action = annulla,
            millis = UNDO_MS,
            onGone = { Undo.clear() }
        ) {
            scope.launch {
                when (quale) {
                    // Il ripristino dal cestino lo fa `Bin`, che sa da dove veniva ogni file;
                    // le altre operazioni si annullano sui percorsi.
                    is Undo.Offer.Trashed -> Bin.restore(context, quale.landed)
                    is Undo.Offer.Files -> FileTree.revert(context, quale.steps)
                }
                model.reloadGrid()
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
    /*
     * ⚠️ Ogni cambio di schermata è una dissolvenza (`1.56`, richiesta dell'utente: il passaggio
     * dal menu alle impostazioni era brusco). La dissolvenza tiene in vita la schermata che se ne
     * va, quindi il menu fa in tempo a chiudersi: una voce chiama `menu.close()` e subito la
     * navigazione, e senza dissolvenza il `Popup` sparirebbe di colpo, con la sfocatura. Il
     * visualizzatore, il passaggio più frequente, ha la dissolvenza corta ([CUT_FADE_MS]).
     * Le barre di sistema non cambiano fra le schermate (vedi `immersivo` in `onCreate`).
     */
        /*
         * ⚠️ Lo scorrimento di una schermata resta quando la si lascia e ci si torna (`1.91`,
         * richiesta dell'utente). Lo tiene `SaveableStateHolder`, che conserva quello che una
         * schermata ha in `rememberSaveable` (come `rememberLazyGridState`) e glielo restituisce
         * quando rientra. La chiave distingue le cartelle (vedi [saveKey]), o la seconda
         * erediterebbe la posizione della prima. Cresce di una voce per schermata visitata e non
         * si pota: ogni voce sono due numeri.
         */
        val stanze = rememberSaveableStateHolder()
        AnimatedContent(
            targetState = model.screen,
            transitionSpec = { cambioSchermata() },
            label = "schermata"
        ) { schermo ->
            /*
             * ⚠️ Questo è il solo punto che sa dove si sta andando, e serve al FAB che esce.
             * Durante la dissolvenza `schermo` è la schermata di prima mentre
             * [ViewerViewModel.screen] è già la nuova: il confronto dice che questa se ne va e se
             * dove si va il FAB non c'è. Il modello si legge qui dentro, così il contenuto uscente
             * si ricompone quando la destinazione cambia.
             */
            CompositionLocalProvider(LocalSenzaFab provides schermo.senzaFabVerso(model.screen)) {
                stanze.SaveableStateProvider(schermo.saveKey()) {
                    Stage(schermo, model, settings, onPicked)
                }
            }
        }
        /*
         * ⚠️ L'unica superficie con cui l'app dice com'è andata (scelta `casa` a `d-avvisi`): è
         * qui, sopra la transizione, perché una notizia deve restare oltre la schermata che l'ha
         * prodotta. La frase la contiene la riga. Sale sopra la scheda della selezione (`2.11`,
         * il perché è su [Modifier.aboveFoot]); accanto al FAB invece si stringe (`2.25`, scelta
         * `stringe` alla voce `avviso-fab`), e il lato lo dichiara il FAB in [FootStage].
         */
        AppNotice(
            Notices.line,
            modifier = Modifier.align(Alignment.BottomCenter).aboveFoot()
        )
        // La proposta di aggiornamento è qui per la ragione della notifica: la apre il controllo
        // all'apertura oppure 'Controlla ora', e deve restare oltre la schermata.
        UpdateDialog()
        /*
         * ⚠️ La fascia della copertina è qui per la ragione della notifica: la scelta comincia in
         * una cartella e può finire in un'altra. Non è una notifica: dice che cosa sta
         * succedendo, e resta finché la modalità è attiva.
         */
        model.covering?.let { quale ->
            CoverInvite(
                name = quale.name,
                onCancel = { model.stopCover() },
                // Sale anche lei, come la notifica: è appoggiata allo stesso bordo.
                modifier = Modifier.align(Alignment.BottomCenter).aboveFoot()
            )
        }
    }
}

/**
 * Quanto dura la dissolvenza fra due schermate.
 *
 * ⚠️ Più lunga dell'uscita di un menu (`MENU_OUT_MS` in `Menus.kt`, 75 ms): la schermata di
 * partenza deve restare in vita finché il menu ha finito di chiudersi. Il margine è di 105 ms.
 */
private const val SCHERMO_MS = 180

/**
 * La dissolvenza con cui una schermata prende il posto di un'altra.
 *
 * ⚠️ `null` e non una `SizeTransform`: quella anima la misura del contenitore, e le due
 * schermate riempiono la finestra, quindi non avrebbe niente da animare (misurato nella `1.74`:
 * il centro del FAB era identico a ogni fotogramma con e senza). `null` dice che il contenitore
 * prende subito la misura della schermata che entra, come nella `AnimatedContent` del
 * visualizzatore (`ViewerScreen.kt`). Questa nota esiste perché la `SizeTransform` non torni.
 *
 * È una funzione a sé perché il banco di prova misuri questa e non una copia.
 */
internal fun AnimatedContentTransitionScope<Screen>.cambioSchermata(): ContentTransform =
    if (initialState is Screen.Viewer || targetState is Screen.Viewer) {
        fadeIn(tween(CUT_FADE_MS)) togetherWith fadeOut(tween(CUT_FADE_MS))
    } else {
        fadeIn(tween(SCHERMO_MS)) togetherWith fadeOut(tween(SCHERMO_MS))
    } using null

/**
 * La dissolvenza breve al posto di ogni taglio netto fra due schermate (nota A del giro della
 * `4.35`: il passaggio dalla griglia alla scacchiera sembrava un lampo a ogni tocco).
 *
 * Solo opacità, con la durata scelta dall'utente; le schermate che avevano già la dissolvenza
 * lunga ([SCHERMO_MS]) la tengono. ⚠️ Un valore solo per ogni taglio: il visualizzatore qui, e le
 * pagine delle impostazioni (`SettingsScreen.kt`). Il video è una `TextureView`
 * (`ViewerScreen.kt`), che sfuma con il resto: una `SurfaceView` comparirebbe di colpo.
 */
internal const val CUT_FADE_MS = 100

/**
 * La schermata visibile adesso.
 *
 * ⚠️ Una funzione a sé perché durante la dissolvenza ce ne sono due, e il `when` deve leggere la
 * schermata che gli passa la transizione: `model.screen` è già la nuova per tutte e due, e il
 * ramo in uscita disegnerebbe quella in arrivo. Le due leggono lo stesso modello, e quella che
 * se ne va si ricompone sui dati nuovi per due decimi di secondo.
 *
 * ⚠️ Nessuna intercettazione di 'Indietro' ha l'anteprima del gesto (`PredictiveBackHandler`),
 * e resta così finché non lo chiede l'utente: sarebbe un'animazione nuova in quattordici punti,
 * e il movimento lo decide lui. Con `targetSdk` 36 l'anteprima di sistema è accesa, quindi
 * trascinando dal bordo si vede la schermata di sotto. Non manca nessuna dipendenza né flag del
 * manifesto.
 */
@Composable
private fun Stage(
    screen: Screen,
    model: ViewerViewModel,
    settings: Settings,
    /** Che cosa fare del file scelto quando l'app è un selettore. Vedi [ViewerViewModel.picking]. */
    onPicked: (Uri) -> Unit
) {
    /**
     * Consegna la miniatura toccata invece di aprirla, quando l'app è un selettore o si sceglie
     * una copertina.
     *
     * Vale per tutte e tre le griglie: anche ricerca e cestino mostrano immagini. Restituisce
     * `false` quando non c'è niente da consegnare (fuori dalle due modalità, o su un indice che
     * l'elenco non ha più), e allora chi chiama apre come sempre.
     */
    fun consegna(items: List<Uri>?, quale: Int): Boolean {
        if (!model.picking && model.covering == null) return false
        val scelto = items?.getOrNull(quale) ?: return false
        /*
         * La copertina viene prima: le due modalità non si sovrappongono, ma se capitasse, quella
         * accesa dentro l'app è l'ultima intenzione.
         */
        if (model.covering != null) {
            model.coverPicked(scelto)
            return true
        }
        onPicked(scelto)
        return true
    }

    when (screen) {
        Screen.Settings -> {
            BackHandler { model.leaveSettings() }
            SettingsScreen(
                settings = settings,
                onChange = { model.updateSettings(it) },
                onStartFolder = { model.chooseStartFolder() },
                onResetHints = { model.resetHints() },
                onChooseEditor = { model.chooseEditor() },
                onBack = { model.leaveSettings() }
            )
        }

        is Screen.Folders -> {
            // Il gesto Indietro si intercetta solo nella veste 'scegli la cartella d'avvio':
            // nell'altra questa è la schermata iniziale, e Indietro chiude l'app.
            if (screen.forStart) BackHandler { model.leaveStartFolderChoice() }
            /*
             * ⚠️ In 'Cartelle di sistema' il gesto Indietro risale di una cartella (`0.84`), o
             * l'app si chiuderebbe da tre cartelle di profondità. È attivo solo quando c'è dove
             * risalire: in cima Indietro chiude l'app.
             * La vista in vigore può essere il Feed della scorciatoia o 'Cartelle di sistema' di
             * una cartella aggiunta alla schermata Home, per una volta sola
             * (`ViewerViewModel.viewOnce`).
             */
            val view = model.shownView(settings.folderView)
            val climbing = view == FolderView.TREE && model.treeClimbing
            BackHandler(enabled = climbing) { model.treeUp() }
            val widthDp = LocalConfiguration.current.screenWidthDp
            /*
             * Due colonne da 600 dp (`3.32`, mockup `folders` e `grid`): elenco a lato e
             * contenuto accanto. Non nella scelta d'avvio, non in 'Cartelle di sistema', che ha
             * una navigazione sua (mockup `system`), e non nel Feed (`6.02`), che mostra immagini
             * senza cartelle.
             */
            val dualFolders = Adaptive.sideAvailable(widthDp) &&
                !screen.forStart &&
                view != FolderView.TREE &&
                view != FolderView.FEED
            val shape = screenShape()
            if (dualFolders && shape == Adaptive.Shape.WIDE) {
                /*
                 * Sullo schermo largo (`3.70`, richiesta B2, scelta B5): la colonna ha in testa
                 * l'intestazione della schermata iniziale e in basso l'elenco delle cartelle, fra
                 * il 40 e il 70%; accanto all'invito, la pillola con tutte le voci del FAB.
                 */
                val railWide: @Composable () -> Unit = {
                    FolderRailWide(
                        width = Adaptive.sideWidth(widthDp),
                        head = {
                            RailIdentity(
                                phone = LocalConfiguration.current.smallestScreenWidthDp < Adaptive.PHONE_MAX
                            )
                        },
                        list = railListFor(model, settings, null)
                    )
                }
                Row(modifier = Modifier.fillMaxSize()) {
                    if (settings.hand == Hand.RIGHT) railWide()
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        FoldersTabletHint(
                            identity = false,
                            pillRoom = PaddingValues(end = PILL_KEY + PILL_AIR * 2)
                        )
                        HomePill(
                            view = view,
                            // Letto qui come nella schermata delle cartelle: decide se compare
                            // 'Scegli un'immagine', l'unica via senza il permesso sui file.
                            granted = Folder.granted(LocalContext.current),
                            hiddenCount = settings.folderSelection.let {
                                if (it.mode == FolderMode.EXCLUDED) it.excluded.size else 0
                            },
                            peeking = model.peeking,
                            onPeek = { model.peek(it) },
                            recents = model.recents,
                            onOpen = { model.open(it) },
                            onOpenPage = { model.openPage(it) },
                            onForget = { model.forgetRecents() },
                            onView = { model.chooseView(it) },
                            onSearch = { model.openSearch() },
                            onBin = { model.openBin() },
                            onSettings = { model.openSettings() },
                            // In basso, col suo margine (voce `3.70-05`).
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .safeDrawingPadding()
                                .padding(PILL_AIR)
                        )
                    }
                    if (settings.hand != Hand.RIGHT) railWide()
                }
            } else if (dualFolders) {
                FoldersTabletSplit(
                    panelOnStart = settings.hand == Hand.RIGHT,
                    rail = {
                        FolderRail(
                            buckets = model.buckets,
                            selected = null,
                            selectedName = null,
                            selection = settings.folderSelection,
                            peeking = model.peeking,
                            colour = settings.folderColour,
                            tints = model.folderTints,
                            onPick = { model.folderPicked(it, false) },
                            onHide = { bucket ->
                                bucket.path?.let {
                                    model.updateSettings(
                                        settings.copy(
                                            hiddenFolders = settings.hiddenFolders + portablePath(it)
                                        )
                                    )
                                }
                            },
                            onUnhide = { voci ->
                                model.updateSettings(
                                    settings.withFolders(settings.folderSelection.remove(voci))
                                )
                            },
                            onSelectionChange = {
                                model.updateSettings(settings.withFolders(it))
                            },
                            onRead = { model.readBuckets(it) },
                            onSearch = { model.openSearch() },
                            onBin = { model.openBin() },
                            onSettings = { model.openSettings() },
                            width = Adaptive.sideWidth(widthDp),
                            liftPx = model.folderRailLift,
                            onLift = { model.folderRailLift = it },
                            listIndex = model.folderRailIndex,
                            listOffset = model.folderRailOffset,
                            onListScroll = { i, o ->
                                model.folderRailIndex = i
                                model.folderRailOffset = o
                            },
                            chrome = shape != Adaptive.Shape.TALL,
                        )
                    },
                    detail = {
                        Box(modifier = Modifier.fillMaxSize()) {
                            FoldersTabletHint(
                                pillRoom = if (shape == Adaptive.Shape.TALL) {
                                    PaddingValues(bottom = PILL_KEY + PILL_AIR * 2)
                                } else {
                                    PaddingValues()
                                }
                            )
                            /*
                             * Sul tablet in verticale Cerca, Cestino e Impostazioni sono nella
                             * pillola in basso a destra, con le sole tre icone e nell'ordine
                             * dello schermo largo (`3.70`, richiesta B3, mockup `Tablet_V`;
                             * voce `3.70-07`).
                             */
                            if (shape == Adaptive.Shape.TALL) {
                                ActionPill(
                                    entries = listOfNotNull(
                                        PillEntry(Icons.Default.Search, stringResource(R.string.hub_search)) {
                                            model.openSearch()
                                        },
                                        // ⚠️ AIV Play ha il cestino solo da Android 11 (`Store.hasBin`).
                                        PillEntry(Glyphs.Bin, stringResource(R.string.bin_title)) {
                                            model.openBin()
                                        }.takeIf { Store.hasBin },
                                        PillEntry(
                                            Icons.Default.Settings,
                                            stringResource(R.string.hub_settings)
                                        ) { model.openSettings() }
                                    ),
                                    vertical = false,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .safeDrawingPadding()
                                        .padding(PILL_AIR)
                                )
                            }
                        }
                    }
                )
            } else {
                FolderScreen(
                    view = view,
                    columns = settings.folderColumns,
                    counted = settings.folderCount,
                    colour = settings.folderColour,
                    tints = model.folderTints,
                    covers = model.folderCovers,
                    hidden = settings.hiddenFolders,
                    selection = settings.folderSelection,
                    onSelectionChange = { model.updateSettings(settings.withFolders(it)) },
                    // ⚠️ Una cartella senza percorso non si può nascondere: si scarta in
                    // silenzio, perché una chiave vuota nasconderebbe la radice. La voce si
                    // scrive senza la radice del volume (`portablePath`).
                    onHide = { bucket ->
                        bucket.path?.let {
                            model.updateSettings(
                                settings.copy(hiddenFolders = settings.hiddenFolders + portablePath(it))
                            )
                        }
                    },
                    // La vista temporanea delle nascoste vive nel modello (vedi `peeking`).
                    peeking = model.peeking,
                    onPeek = { model.peek(it) },
                    // Mostrare per sempre scrive la stessa preferenza della pagina 'Cartelle
                    // nascoste'. Le voci si tolgono in una scrittura sola (il perché è sul
                    // parametro, in `FolderScreen`).
                    onUnhide = { voci ->
                        model.updateSettings(
                            settings.withFolders(settings.folderSelection.remove(voci))
                        )
                    },
                    recents = model.recents,
                    onPick = { model.folderPicked(it, screen.forStart) },
                    // Anche i recenti consegnano, al selettore o come copertina, come ogni
                    // griglia: spesso si vuole proprio l'ultima immagine aperta.
                    onOpen = { quale ->
                        when {
                            model.covering != null -> model.coverPicked(quale)
                            model.picking -> onPicked(quale)
                            else -> model.open(quale)
                        }
                    },
                    onOpenPage = { model.openPage(it) },
                    onView = { model.chooseView(it) },
                    onForget = { model.forgetRecents() },
                    onSettings = { model.openSettings() },
                    onSearch = { model.openSearch() },
                    onBin = { model.openBin() },
                    feed = model.feed,
                    feedDate = settings.feedDate,
                    onFeedDate = { model.updateSettings(settings.copy(feedDate = it)) },
                    onFeedRead = { model.readFeed(it, settings.folderSelection, settings.feedDate) },
                    onFeedLeft = model::leaveFeed,
                    // Il Feed consegna come 'Cartelle di sistema': copertina o risposta all'app che
                    // ha chiesto un'immagine, altrimenti il visualizzatore.
                    onFeedOpen = { items, at -> if (!consegna(items, at)) model.openFromTree(items, at) },
                    onFeedChanged = model::rereadFeed,
                    onFeedBusy = { model.feedBusy = it },
                    // La scorciatoia scrive la stessa impostazione delle preferenze, valida per
                    // tutte le cartelle (richiesta dell'utente).
                    onColumns = { model.updateSettings(settings.copy(folderColumns = it)) },
                    // Le opzioni delle altre due viste, dallo stesso popup, allo stesso modo.
                    listCount = settings.listCount,
                    listText = settings.listText,
                    treeHidden = settings.treeHidden,
                    treePictures = settings.treePictures,
                    onListCount = { model.updateSettings(settings.copy(listCount = it)) },
                    onListText = { model.updateSettings(settings.copy(listText = it)) },
                    onTreeHidden = { model.updateSettings(settings.copy(treeHidden = it)) },
                    onTreePictures = { model.updateSettings(settings.copy(treePictures = it)) },
                    treePath = model.treePath,
                    binOn = settings.binOn,
                    factFields = settings.factRows,
                    onTreePath = { model.treeTo(it) },
                    // ⚠️ Anche 'Cartelle di sistema' consegna (`2.96`): la scelta della copertina
                    // comincia da questa schermata (vedi `coverAway`), e deve valere in tutte le
                    // sue viste.
                    onTreeOpen = { items, at -> if (!consegna(items, at)) model.openFromTree(items, at) },
                    forStart = screen.forStart,
                    onBack = { model.leaveStartFolderChoice() },
                    buckets = model.buckets,
                    onRead = { model.readBuckets(it) }
                )
            }
        }

        is Screen.Grid -> {
            BackHandler { model.leaveGrid() }
            // ⚠️ `null` mentre la lettura è in corso, elenco vuoto quando la cartella non ha
            // niente da mostrare: l'indicatore di attesa e il messaggio sono due cose diverse,
            // e confonderle darebbe un'attesa senza fine.
            val lookup = model.folder
            val widthDp = LocalConfiguration.current.screenWidthDp
            val shape = screenShape()
            val dualGrid = Adaptive.sideAvailable(widthDp) &&
                settings.folderView != FolderView.TREE
            /*
             * Una chiamata sola per le tre vesti (colonna larga, colonna normale, telefono), così
             * un parametro nuovo si scrive in un posto solo.
             */
            @Composable
            fun FolderGrid(forma: Adaptive.Shape, colonna: WideRail?) {
            GridScreen(
                title = screen.name,
                items = lookup?.let { it.seriesOrNull?.items ?: emptyList() },
                // L'indice si legge dalla serie, che la strisciata tiene aggiornata; la
                // bandierina dice solo se un'immagine è stata aperta.
                highlight = if (model.gridVisited) model.series?.index else null,
                /*
                 * Cestino e impostazioni dal FAB della cartella (`1.82`, richiesta dell'utente).
                 * Solo la griglia di una cartella riceve queste voci, 'Cerca' ristretto (`1.83`,
                 * scelta a `d-fab-voci`; vedi [Screen.Search]), la copertina (vedi
                 * [ViewerViewModel.covering]), la rinomina e i valori dell'intestazione: la
                 * ricerca è un elenco di risultati con un campo di testo, il cestino ha il suo menu
                 * e nessun bucket, e l'intestazione esiste solo qui.
                 */
                onBin = { model.openBin() }.takeIf { Store.hasBin },
                onSettings = { model.openSettings() },
                onSearchHere = { model.openSearch(screen.bucket, screen.name) },
                onCoverPick = { model.startCover() },
                onCoverAway = { model.coverAway() },
                onCoverClear = { model.clearCover() },
                coverSet = model.cover != null,
                // Serve al mini-onboarding, e solo nella cartella da cui parte la scelta: in
                // un'altra indicherebbe l'icona sbagliata.
                coverHere = model.covering?.bucket == screen.bucket,
                // ⚠️ Non in AIV Play, che non può rinominare una cartella condivisa
                // (`Store.files`).
                onFolderRename = { name: String -> model.renameFolder(name) }.takeIf { Store.files },
                frontWash = settings.frontWash,
                frontSerif = settings.frontSerif,
                frontFacts = settings.frontFacts,
                frontPickAll = settings.frontPickAll,
                // La tinta è un dato della cartella (vedi [ViewerViewModel.tint]).
                frontTint = model.tint,
                onFrontTint = { model.tintFolder(it) },
                facts = model.facts,
                onOpen = { quale ->
                    // Al selettore o come copertina consegna, altrimenti apre come sempre.
                    if (!consegna(lookup?.seriesOrNull?.items, quale)) model.openFromGrid(quale)
                },
                onBack = { model.leaveGrid() },
                onChanged = { model.reloadGrid() },
                columns = settings.folderColumns,
                factFields = settings.factRows,
                binOn = settings.binOn,
                listPath = settings.listPath,
                pickWeight = settings.pickWeight,
                filter = model.gridFilter,
                /*
                 * ⚠️ Va passato a tutti e tre i rami: il parametro ha un valore di riserva vuoto
                 * (vedi `onSearch` in `GridScreen`), e senza il tocco lungo sul tasto del filtro
                 * vibra e non fa niente. Vale anche nel cestino: due controlli identici si
                 * comportano allo stesso modo.
                 */
                onSearch = { model.openSearch() },
                onFilter = { model.sift(it) },
                gridNames = settings.gridNames,
                lastMark = settings.lastMark,
                onBusy = { model.gridBusy = it },
                shape = forma,
                rail = colonna
            )
            }
            /*
             * Sullo schermo largo la colonna la compone la griglia (`3.70`): in cima ci sono le
             * pastiglie dell'intestazione, che agiscono sulla selezione della griglia. Il perché è
             * sul parametro `rail`.
             */
            when {
                dualGrid && shape == Adaptive.Shape.WIDE -> FolderGrid(
                    shape,
                    WideRail(
                        width = Adaptive.sideWidth(widthDp),
                        onStart = settings.hand == Hand.RIGHT,
                        list = railListFor(model, settings, screen.bucket)
                    )
                )
                dualGrid -> FoldersTabletSplit(
                    panelOnStart = settings.hand == Hand.RIGHT,
                    rail = {
                        FolderRail(
                            buckets = model.buckets,
                            selected = screen.bucket,
                            selectedName = screen.name,
                            selection = settings.folderSelection,
                            peeking = model.peeking,
                            colour = settings.folderColour,
                            tints = model.folderTints,
                            onPick = { model.folderPicked(it, false) },
                            onHide = { bucket ->
                                bucket.path?.let {
                                    model.updateSettings(
                                        settings.copy(
                                            hiddenFolders = settings.hiddenFolders + portablePath(it)
                                        )
                                    )
                                }
                            },
                            onUnhide = { voci ->
                                model.updateSettings(
                                    settings.withFolders(settings.folderSelection.remove(voci))
                                )
                            },
                            onSelectionChange = {
                                model.updateSettings(settings.withFolders(it))
                            },
                            onRead = { model.readBuckets(it) },
                            onSearch = {
                                model.openSearch(screen.bucket, screen.name)
                            },
                            onBin = { model.openBin() },
                            onSettings = { model.openSettings() },
                            width = Adaptive.sideWidth(widthDp),
                            liftPx = model.folderRailLift,
                            onLift = { model.folderRailLift = it },
                            listIndex = model.folderRailIndex,
                            listOffset = model.folderRailOffset,
                            onListScroll = { i, o ->
                                model.folderRailIndex = i
                                model.folderRailOffset = o
                            },
                            chrome = shape != Adaptive.Shape.TALL,
                        )
                    },
                    detail = { FolderGrid(shape, null) }
                )
                else -> FolderGrid(shape, null)
            }
        }

        is Screen.Search -> {
            BackHandler { model.leaveGrid() }
            // La stessa `GridScreen` della cartella, con due parametri in più (perché non è una
            // schermata a sé: vedi la nota su `query` là dentro).
            val lookup = model.folder
            val widthDp = LocalConfiguration.current.screenWidthDp
            /*
             * Due colonne da 600 dp (`3.34`, mockup `search`): elenco delle cartelle a lato e
             * risultati accanto, come per la griglia. Non in 'Cartelle di sistema'.
             */
            val dualSearch = Adaptive.sideAvailable(widthDp) &&
                settings.folderView != FolderView.TREE
            val shape = screenShape()
            @Composable
            fun SearchGrid(forma: Adaptive.Shape, colonna: WideRail?) {
                GridScreen(
                    title = "",
                    // Il nome della cartella in cui si cerca, `null` per tutta la galleria: il
                    // campo lo mostra al posto del suo invito, senza una stringa nuova in
                    // ventotto lingue.
                    searchIn = screen.name,
                    items = lookup?.let { it.seriesOrNull?.items ?: emptyList() },
                    highlight = if (model.gridVisited) model.series?.index else null,
                    onOpen = { quale ->
                        // Al selettore o come copertina consegna, altrimenti apre come sempre.
                        if (!consegna(lookup?.seriesOrNull?.items, quale)) model.openFromGrid(quale)
                    },
                    onBack = { model.leaveGrid() },
                    onChanged = { model.reloadGrid() },
                    columns = settings.folderColumns,
                    query = model.query,
                    onQuery = { model.search(it) },
                    onSearch = { model.openSearch() },
                    factFields = settings.factRows,
                    binOn = settings.binOn,
                    listPath = settings.listPath,
                    pickWeight = settings.pickWeight,
                    filter = model.gridFilter,
                    onFilter = { model.sift(it) },
                    gridNames = settings.gridNames,
                    lastMark = settings.lastMark,
                    onBusy = { model.gridBusy = it },
                    /*
                     * ⚠️ Cestino e impostazioni arrivano qui fuori dal telefono (`3.70`, scelte B4
                     * e B7): sul tablet vivono nella pillola, e senza non si raggiungerebbero dalla
                     * ricerca. Sul telefono il FAB della ricerca non c'è.
                     */
                    onBin = { model.openBin() }.takeIf { forma != Adaptive.Shape.PHONE && Store.hasBin },
                    onSettings = { model.openSettings() }.takeIf { forma != Adaptive.Shape.PHONE },
                    shape = forma,
                    rail = colonna
                )
            }
            if (dualSearch && shape == Adaptive.Shape.WIDE) {
                SearchGrid(
                    shape,
                    WideRail(
                        width = Adaptive.sideWidth(widthDp),
                        onStart = settings.hand == Hand.RIGHT,
                        list = railListFor(model, settings, screen.bucket)
                    )
                )
            } else if (dualSearch) {
                FoldersTabletSplit(
                    panelOnStart = settings.hand == Hand.RIGHT,
                    rail = {
                        FolderRail(
                            buckets = model.buckets,
                            selected = screen.bucket,
                            selectedName = screen.name,
                            selection = settings.folderSelection,
                            peeking = model.peeking,
                            colour = settings.folderColour,
                            tints = model.folderTints,
                            onPick = { model.folderPicked(it, false) },
                            onHide = { bucket ->
                                bucket.path?.let {
                                    model.updateSettings(
                                        settings.copy(
                                            hiddenFolders = settings.hiddenFolders + portablePath(it)
                                        )
                                    )
                                }
                            },
                            onUnhide = { voci ->
                                model.updateSettings(
                                    settings.withFolders(settings.folderSelection.remove(voci))
                                )
                            },
                            onSelectionChange = {
                                model.updateSettings(settings.withFolders(it))
                            },
                            onRead = { model.readBuckets(it) },
                            onSearch = {
                                model.openSearch(screen.bucket, screen.name)
                            },
                            onBin = { model.openBin() },
                            onSettings = { model.openSettings() },
                            width = Adaptive.sideWidth(widthDp),
                            liftPx = model.folderRailLift,
                            onLift = { model.folderRailLift = it },
                            listIndex = model.folderRailIndex,
                            listOffset = model.folderRailOffset,
                            onListScroll = { i, o ->
                                model.folderRailIndex = i
                                model.folderRailOffset = o
                            },
                            chrome = shape != Adaptive.Shape.TALL,
                        )
                    },
                    detail = { SearchGrid(shape, null) }
                )
            } else {
                SearchGrid(shape, null)
            }
        }

        Screen.Bin -> {
            BackHandler { model.leaveGrid() }
            // La stessa `GridScreen` di una cartella, con `bin` acceso: quello che cambia è là
            // dentro (eliminazione definitiva, ripristino al posto della rinomina, il FAB anche
            // senza selezione). Il cestino si sfoglia come una cartella (richiesta dell'utente).
            val lookup = model.folder
            val widthDp = LocalConfiguration.current.screenWidthDp
            val listed = lookup?.seriesOrNull?.items
            val highlight = if (model.gridVisited) model.series?.index else null
            val detailUri = listed?.let { list ->
                highlight?.takeIf { it in list.indices }?.let { list[it] }
            }
            @Composable
            fun BinGrid() {
                GridScreen(
                    title = stringResource(R.string.bin_title),
                    items = lookup?.let { it.seriesOrNull?.items ?: emptyList() },
                    highlight = highlight,
                    onOpen = { quale ->
                        // Al selettore o come copertina consegna, altrimenti apre come sempre.
                        if (!consegna(listed, quale)) model.openFromGrid(quale)
                    },
                    onBack = { model.leaveGrid() },
                    onChanged = { model.reloadGrid() },
                    columns = settings.folderColumns,
                    factFields = settings.factRows,
                    binOn = settings.binOn,
                    listPath = settings.listPath,
                    pickWeight = settings.pickWeight,
                    filter = model.gridFilter,
                    // ⚠️ Passato a tutti e tre i rami: vedi la nota nel ramo della cartella.
                    onSearch = { model.openSearch() },
                    onFilter = { model.sift(it) },
                    gridNames = settings.gridNames,
                    lastMark = settings.lastMark,
                    bin = true,
                    onHistory = { model.openHistory() },
                    onBusy = { model.gridBusy = it },
                    // Fuori dal telefono le voci del FAB del cestino vanno nella pillola (`3.70`,
                    // richiesta B2).
                    shape = screenShape()
                )
            }
            // La griglia del cestino occupa tutta la larghezza, senza dettagli a lato (voce
            // `3.35-01`).
            BinGrid()
        }

        Screen.History -> {
            BackHandler { model.leaveHistory() }
            HistoryScreen(onBack = { model.leaveHistory() })
        }

        is Screen.Editor -> {
            BackHandler { model.leaveEditor() }
            EditorScreen(
                uri = screen.uri,
                busy = model.editorBusy,
                marked = model.markReady,
                marking = settings.markOn,
                hasMark = model.markHas,
                onMark = { model.setMark(it) },
                onMarkSetup = { model.openMarkSetup() },
                resize = model.resizePlan(),
                saved = model.resizeSaved(),
                resizing = model.resizeOn(),
                onResize = { model.setResize(it) },
                onResizeDefault = { model.setResizeDefault(it) },
                onSave = { turns, mirror, crop -> model.editSave(turns, mirror, crop) },
                onBack = { model.leaveEditor() }
            )
        }

        // Indietro va al visualizzatore, come per l'editor semplice: tutti e due si aprono solo
        // da lì, quindi la destinazione non viaggia nella schermata.
        is Screen.FullEditor -> {
            BackHandler { model.leaveEditor() }
            AdvancedEditorScreen(
                uri = screen.uri,
                busy = model.editorBusy,
                marked = model.markReady,
                marking = settings.markOn,
                hasMark = model.markHas,
                onMark = { model.setMark(it) },
                onMarkSetup = { model.openMarkSetup() },
                resize = model.resizePlan(),
                saved = model.resizeSaved(),
                resizing = model.resizeOn(),
                onResize = { model.setResize(it) },
                onResizeDefault = { model.setResizeDefault(it) },
                onSave = { look, beside -> model.lookSave(look, beside) },
                onBack = { model.leaveEditor() }
            )
        }

        Screen.Viewer -> {
            // Indietro torna da dove si è aperto il visualizzatore, se quel posto c'è; aperto
            // da un collegamento, Indietro esce dall'app.
            // ⚠️ Il doppio tocco non è una via d'uscita: alterna adattata e 100%, e per
            // l'utente è fondamentale.
            if (model.viewerBack != null) BackHandler { model.backFromViewer() }
            FolderPermission(model)
            ViewerScreen(
                state = model.state,
                settings = settings,
                source = model.source,
                folder = model.folder,
                onStep = { model.step(it) },
                onSettings = { model.openSettings() },
                onRetry = { model.retry() },
                onClipStarted = { model.clipStarted() },
                onFileChanged = { model.afterFileChanged() },
                onFileAdded = { model.afterFileAdded() },
                onEdit = { model.edit(it) },
                onEditWith = { model.editWith(it) },
                // Scrive le stesse due chiavi delle impostazioni: la scelta fatta col tocco
                // lungo su 'Info' resta (richiesta dell'utente).
                onInfoBar = { visible, where ->
                    model.updateSettings(
                        settings.copy(infoVisible = visible, infoPosition = where)
                    )
                },
                // Se si sta guardando il cestino lo dice la provenienza: l'immagine non lo sa.
                inBin = model.viewerBack is Screen.Bin
            )
        }
    }
}

/**
 * Chiede il permesso sulle cartelle una volta, quando servirebbe la prima volta.
 *
 * ⚠️ Solo per un'immagine di questo telefono (`content://`), solo se il permesso manca e solo
 * se non è mai stato chiesto: un'immagine condivisa da una chat non ha una cartella.
 * Una volta sola anche se la risposta è stata no: riproporre la pagina di sistema a ogni
 * immagine insegna a rifiutare. Chi cambia idea accende l'interruttore nelle impostazioni del
 * telefono, e `Folder.granted` lo legge dal sistema alla prossima immagine.
 */
@Composable
private fun FolderPermission(model: ViewerViewModel) {
    val context = LocalContext.current
    // La strada (pagina di sistema o finestra) la sceglie [Store], che conosce la variante;
    // qui resta 'se e quando'. La spiegazione compare prima della pagina.
    val ask = Store.rememberAccessRequest(explain = true) { allowed -> model.folderAnswered(allowed) }
    val source = model.source
    val local = source?.scheme?.lowercase() == "content"
    LaunchedEffect(source, model.folderAsked) {
        if (!local || model.folderAsked || Folder.granted(context)) return@LaunchedEffect
        ask()
    }
}

/**
 * Tre vie d'ingresso: un tocco su un collegamento (`VIEW` con l'indirizzo), un file aperto da
 * un'altra app (`VIEW` con un indirizzo `content://`) e una condivisione (`SEND`, con
 * l'indirizzo in un extra).
 */
private fun Intent?.imageUri(): Uri? {
    if (this == null) return null
    return when (action) {
        Intent.ACTION_SEND -> getParcelableExtraCompat(Intent.EXTRA_STREAM)
        else -> data
    }
}

@Suppress("DEPRECATION")
private fun Intent.getParcelableExtraCompat(name: String): Uri? =
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, Uri::class.java)
    } else {
        getParcelableExtra(name)
    }

/**
 * L'elenco delle cartelle per la colonna dello schermo largo, con le stesse scritture della
 * colonna normale: nascondere, mostrare di nuovo, la modalità delle incluse, la posizione dello
 * scorrimento.
 *
 * Una copia sola per la griglia della cartella, la ricerca e la schermata iniziale.
 */
private fun railListFor(
    model: ViewerViewModel,
    settings: Settings,
    selected: Long?
): @Composable (Modifier) -> Unit = { modifier ->
    FolderRailList(
        buckets = model.buckets,
        selected = selected,
        selection = settings.folderSelection,
        peeking = model.peeking,
        colour = settings.folderColour,
        tints = model.folderTints,
        onPick = { model.folderPicked(it, false) },
        onHide = { bucket ->
            bucket.path?.let {
                model.updateSettings(
                    settings.copy(hiddenFolders = settings.hiddenFolders + portablePath(it))
                )
            }
        },
        onUnhide = { voci ->
            model.updateSettings(settings.withFolders(settings.folderSelection.remove(voci)))
        },
        onSelectionChange = { model.updateSettings(settings.withFolders(it)) },
        onRead = { model.readBuckets(it) },
        listIndex = model.folderRailIndex,
        listOffset = model.folderRailOffset,
        onListScroll = { i, o ->
            model.folderRailIndex = i
            model.folderRailOffset = o
        },
        modifier = modifier
    )
}

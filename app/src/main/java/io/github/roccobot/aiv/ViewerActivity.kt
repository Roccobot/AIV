package io.github.roccobot.aiv

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.setSingletonImageLoaderFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface ViewerState {
    /**
     * Si sta aprendo qualcosa, e [progress] dice a che punto è il **trasferimento**
     * quando lo si può sapere.
     *
     * ⚠️⚠️ **Il numero vive QUI e non in un campo a parte del modello**, ed è la
     * lezione dell'anello della `0.34`: un dato tenuto in un secondo posto va
     * azzerato a mano, e prima o poi qualcuno se ne dimentica lasciando l'80% di
     * ieri sopra l'immagine di adesso. Attaccato allo stato, ogni apertura nuova ne
     * costruisce uno nuovo, e dimenticarsene non è possibile.
     * ⚠️ Null vuol dire **non lo so**, non zero: un file locale non ha nessuna attesa
     * da raccontare, e un server che non dichiara la lunghezza non permette di
     * contare. Chi legge mostra il giro indeterminato, che è la risposta onesta.
     */
    data class Loading(val progress: Float? = null) : ViewerState
    data class Ready(val image: LoadedImage) : ViewerState
    data class Error(@param:StringRes val messageRes: Int, val detail: String?) : ViewerState
}

/** Quale schermata è davanti. */
sealed interface Screen {
    data object Settings : Screen
    data object Viewer : Screen

    /**
     * L'elenco delle cartelle, e **la casa dell'app** dalla 0.41.
     *
     * ⚠️⚠️ **QUI C'ERA `Home`, la schermata che chiedeva da dove cominciare, ed è
     * sparita** (decisione dell'utente, 2026-08-29). Non è stata sostituita da un'altra
     * schermata ma da una **risposta**, che è questa. Chi cerca dove siano finite le sue
     * cinque vie le trova dentro il tastino di `FolderScreen`.
     * ⚠️ Porta [forStart] perché la stessa schermata risponde a due domande: 'quale
     * cartella apro adesso' e 'quale cartella apro all'avvio'. È l'unico dato che
     * distingue i due usi, e sta qui e non nel modello perché muore con la schermata.
     */
    data class Folders(val forStart: Boolean) : Screen

    /**
     * Una cartella in miniature, dalla 0.33: il primo passo della galleria.
     *
     * ⚠️ Porta il **nome** oltre all'identificativo, e non è ridondanza: il nome lo sa
     * l'elenco da cui si arriva, e ricavarlo di nuovo vorrebbe dire rileggere il
     * MediaStore per scrivere un titolo. ⚠️ Porta anche la cartella perché è la
     * destinazione del gesto Indietro dal visualizzatore, che deve poter tornare
     * ESATTAMENTE a questa griglia.
     */
    data class Grid(val bucket: Long, val name: String) : Screen

    /**
     * I risultati di una ricerca, dalla 0.59.
     *
     * ⚠️⚠️ **NON PORTA IL TESTO CERCATO, e sarebbe l'errore facile**: il testo cambia a
     * ogni lettera digitata, quindi metterlo qui vorrebbe dire ricostruire la schermata a
     * ogni tasto, e con lei lo stato della griglia. Il testo vive nel modello, dove può
     * cambiare senza che la schermata cambi identità.
     * ⚠️ Per tutto il resto è una **griglia come le altre**: i risultati sono una serie, e
     * da qui si apre il visualizzatore, si sfoglia e si seleziona esattamente come in una
     * cartella.
     */
    data object Search : Screen

    /**
     * Il cestino, dalla 0.64.
     *
     * ⚠️⚠️ **NON È UNA `Grid` con una cartella particolare, e il motivo è tecnico**: le
     * fotografie del cestino vivono nella cartella dell'app, dove il MediaStore non guarda,
     * quindi non hanno un identificativo di album da mettere in [Grid]. Si elencano leggendo
     * il disco (vedi `Bin.list`), e per il resto dell'app sono una serie come le altre.
     * ⚠️ Come [Search], non porta dati: quello che c'è dentro lo dice il disco, e tenerne una
     * copia qui vorrebbe dire ricostruire la schermata a ogni eliminazione.
     */
    data object Bin : Screen
}

/**
 * La casa dell'app.
 *
 * ⚠️ Un nome solo perché i posti che ci tornano sono sei (l'avvio, l'uscita dal
 * visualizzatore, le impostazioni chiuse senza una foto aperta, e le tre vie che aprono):
 * scritta a mano in ognuno, il giorno che la casa cambia se ne aggiornerebbero cinque.
 */
private val HOME = Screen.Folders(forStart = false)

/**
 * Quanto si aspetta, dopo l'ultimo tasto, prima di interrogare il MediaStore.
 *
 * ⚠️ Tarato su come si scrive e non su un numero tondo: sotto i due decimi la pausa non
 * raggruppa niente perché fra una lettera e l'altra passa di più, sopra il mezzo secondo si
 * sente il ritardo fra il testo e i risultati. Da rivedere solo con una misura sul telefono,
 * non a naso.
 */
private const val SEARCH_PAUSE_MS = 280L

/**
 * The decoded picture lives in the ViewModel and not in the composition: a
 * rotation must not send the phone back to the network, and on a big file that
 * would be a visible pause rather than a purist's detail.
 */
class ViewerViewModel(application: Application) : AndroidViewModel(application) {

    var state: ViewerState by mutableStateOf(ViewerState.Loading())
        private set

    var screen: Screen by mutableStateOf(HOME)
        private set

    /**
     * Null until the stored settings have been read once.
     *
     * ⚠️ The screens wait for it instead of starting on the defaults, and the
     * reason is visible rather than theoretical: the resting scale of a picture
     * depends on two settings, so drawing with the defaults and then receiving the
     * real ones would show the image at one size and snap it to another.
     */
    var settings: Settings? by mutableStateOf(null)
        private set

    var recents: List<RecentImage> by mutableStateOf(emptyList())
        private set

    /** Where the picture on screen came from. The menu needs it, the loader has already used it. */
    var source: Uri? by mutableStateOf(null)
        private set

    /**
     * Dove torna il gesto Indietro dal visualizzatore, e **null quando deve uscire
     * dall'app**.
     *
     * ⚠️ Era un booleano (`cameFromHome`) finché le provenienze erano due, la schermata
     * iniziale e un collegamento da fuori. Dalla 0.33 sono tre, perché si arriva anche
     * da una griglia, e quella griglia è una cartella precisa: un secondo booleano
     * avrebbe descritto quattro stati di cui due impossibili, mentre la destinazione è
     * esattamente il dato che serve.
     */
    var viewerBack: Screen? by mutableStateOf(null)
        private set

    /**
     * L'esito della ricerca della cartella nell'ordine di BASE, cioè dalla foto più
     * recente alla più vecchia, che dalla 0.30 è anche il verso predefinito di lettura.
     *
     * ⚠️ Porta la ragione e non solo la serie perché la serie mancante è muta: vedi
     * `Folder.Lookup`, e le due versioni che sono servite a scoprirlo.
     * ⚠️ Privato perché **non è l'ordine in cui si sfoglia**: quello lo decide
     * l'impostazione, e vive in [folder]. Null soltanto finché la ricerca non è finita.
     */
    private var listed: Folder.Lookup? by mutableStateOf(null)

    /**
     * L'esito nell'ordine in cui si sfoglia: è questo che guardano tutti gli altri.
     *
     * ⚠️⚠️ **Girato al momento dell'uso e non a quello della lettura, e la differenza
     * è una corsa evitata**: la ricerca della cartella e la lettura delle impostazioni
     * sono due coroutine indipendenti, e quale delle due arrivi prima non è deciso.
     * Orientando qui l'ordine è giusto comunque, e spostare l'interruttore rigira la
     * serie che si ha in mano senza rileggere niente dal database.
     * ⚠️ `derivedStateOf` e non un getter nudo: girare copia una lista che può avere
     * qualche centinaio di voci, e senza cache la copia si rifarebbe a ogni lettura
     * durante la composizione.
     */
    val folder: Folder.Lookup? by derivedStateOf { listed.oriented() }

    /** Scorciatoia per chi della cartella vuole solo la serie, quando c'è. */
    val series: Folder.Series? get() = folder?.seriesOrNull

    /**
     * L'esito nel verso scelto dall'impostazione.
     *
     * ⚠️ Serve **in entrambi i versi** e una funzione sola basta, perché girare una
     * serie è la sua stessa inversa: di qui si passa sia per mostrare l'ordine di
     * lettura sia per riscrivere in [listed] quello grezzo (vedi [step]).
     */
    private fun Folder.Lookup?.oriented(): Folder.Lookup? =
        if (settings?.reverseSequence == true) this?.reversed() else this

    /**
     * Lo stesso esito posizionato sull'INIZIO della sequenza scelta.
     *
     * ⚠️⚠️ **Nell'ordine grezzo la prima foto della sequenza è l'ULTIMA riga quando il
     * verso è invertito**, e chi lo dimentica apre la cartella su un vicolo cieco, con
     * la strisciata in avanti che non ha dove andare: è il difetto della 0.29, che si
     * vedeva come un gesto che non fa niente.
     * ⚠️ Vive in una funzione sola perché dalla 0.33 i posti che aprono una cartella
     * sono **due**, l'avvio e la griglia, e la seconda copia di questo calcolo sarebbe
     * quella che un domani resta indietro.
     */
    private fun Folder.Lookup.atSequenceStart(): Folder.Lookup {
        val whole = seriesOrNull ?: return this
        val first = if (settings?.reverseSequence == true) whole.items.lastIndex else 0
        return Folder.Lookup.Found(whole.copy(index = first))
    }

    /**
     * Se in questa visita alla griglia si è già aperta una foto.
     *
     * ⚠️⚠️ **È un SÌ o NO e non l'indice, ed è la correzione della `0.34`**, che l'anello
     * non lo mostrava: là il segno era un indice **copiato** al momento del ritorno, cioè
     * un dato in più da tenere d'accordo con la serie, scritto in un punto solo e in un
     * istante solo. Bastava che quella scrittura non avvenisse, o avvenisse quando la
     * serie non era quella attesa, perché il segno restasse vuoto e l'anello sparisse
     * senza lasciare traccia.
     * ⚠️ Adesso la posizione **non si copia**: la griglia legge `series.index`, cioè
     * l'unico posto in cui la foto corrente è già scritta e che la strisciata tiene
     * aggiornato da sé. Questa bandierina risponde alla sola domanda che quel numero non
     * sa rispondere: se una foto sia mai stata aperta in questa visita. Senza, entrando in
     * una cartella si evidenzierebbe la prima miniatura, che nessuno ha guardato.
     */
    var gridVisited: Boolean by mutableStateOf(false)
        private set

    /** Whether the folder permission has already been asked for once. */
    var folderAsked: Boolean by mutableStateOf(true)
        private set

    init {
        val context = getApplication<Application>()
        viewModelScope.launch {
            SettingsStore.flow(context).collect { fresh ->
                settings = fresh
                // ⚠️ Gli appunti PRIMA della cartella d'avvio, o la seconda coprirebbe
                // la fotografia che i primi hanno appena aperto.
                readClipboard()
                openStartFolder(fresh)
            }
        }
        viewModelScope.launch { Recents.flow(context).collect { recents = it } }
        viewModelScope.launch { FolderAsk.flow(context).collect { folderAsked = it } }
    }

    /**
     * Called from onCreate and onNewIntent, and NOT from the composition: reading
     * the intent while composing meant re-reading it on every recomposition, and
     * the only thing that kept it from re-loading was a guard on the address.
     */
    fun handleIntent(intent: Intent?) {
        val uri = intent.imageUri()
        if (uri == null) {
            // Partita dalla propria icona: si va dove sta la roba, cioè alle cartelle.
            screen = HOME
            atStart = true
            fromIcon = true
            settings?.let(::openStartFolder)
        } else {
            // ⚠️ Un collegamento da fuori NON deve far guardare negli appunti: la
            // persona ha già detto che cosa vuole aprire.
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
     * Gli appunti, guardati **una volta sola** e solo all'avvio dall'icona.
     *
     * ⚠️⚠️ **VA CHIAMATA QUANDO LA FINESTRA PRENDE IL FUOCO, e non da `onCreate`**: da
     * Android 10 un'app può leggere gli appunti **solo mentre ha il fuoco**, quindi un
     * controllo fatto alla creazione tornerebbe a mani vuote e la funzione sembrerebbe
     * guasta invece che impossibile.
     * ⚠️⚠️ **E DA ANDROID 12 IL SISTEMA LO ANNUNCIA** col suo avvisino: leggerli a ogni
     * avvio vuol dire quell'avviso a ogni avvio. È il costo visibile della funzione che
     * l'utente ha chiesto, non un difetto da nascondere; l'unico modo di ridurlo sarebbe
     * leggere di meno, cioè non fare quello che serve.
     * ⚠️ **Nessuna richiesta di rete qui**: 'indirizzo diretto a un'immagine' si decide
     * dall'estensione nel percorso e non con una `HEAD`, o l'apertura dell'app
     * aspetterebbe un server. Se poi l'indirizzo non è un'immagine lo dice il
     * visualizzatore, che dalla 0.38 ha il suo errore e il suo Riprova.
     */
    /**
     * Se la finestra ha il fuoco adesso.
     *
     * ⚠️⚠️ **SERVE PERCHÉ LE DUE CONDIZIONI ARRIVANO DA DUE PARTI E IN ORDINE IGNOTO**: il
     * fuoco è un evento di sistema, le impostazioni sono una coroutine. Guardando negli
     * appunti solo al fuoco, chi avesse le impostazioni ancora in volo non li guarderebbe
     * mai; guardandoli solo all'arrivo delle impostazioni, chi le ha già lette prima del
     * fuoco non li guarderebbe mai. Segnata la prima, la seconda può chiamare.
     */
    private var focused = false

    /** L'ha chiamata l'activity: la finestra ha preso o perso il fuoco. */
    fun windowFocus(hasFocus: Boolean) {
        focused = hasFocus
        if (hasFocus) readClipboard()
    }

    private fun readClipboard() {
        if (clipboardTried || !fromIcon || !focused) return
        // ⚠️⚠️ **SPENTA FINCHÉ NON LA SI ACCENDE, e il controllo sta PRIMA della
        // lettura**: è l'unico punto in cui metterlo che eviti davvero l'avviso di
        // sistema, perché quell'avviso lo fa scattare la lettura e non l'uso di quello
        // che si è letto.
        val fresh = settings ?: return
        if (!fresh.clipboardStart) {
            // Detto di no: non si guarda, e non si riproverà più in questa esecuzione.
            clipboardTried = true
            return
        }
        clipboardTried = true
        val context = getApplication<Application>()
        val uri = ImageActions.urlInClipboard(context) ?: return
        if (!ImageActions.looksLikeImage(uri)) return
        // ⚠️⚠️ **SPEGNE LA CARTELLA D'AVVIO, e senza questa riga l'ordine deciderebbe il
        // vincitore**: le impostazioni arrivano da una coroutine e il fuoco da un evento
        // di sistema, quindi quale dei due sia primo non è stabilito. Se il fuoco arriva
        // prima, `openStartFolder` scatterebbe **dopo** e coprirebbe la fotografia degli
        // appunti con la cartella. L'utente ha detto che gli appunti vengono per primi,
        // quindi qui la corsa si chiude invece di sperare che vada bene.
        atStart = false
        open(uri)
    }

    /**
     * Se questo è l'avvio dell'app e non un ritorno.
     *
     * ⚠️ Serve perché la scelta di aprire una cartella all'avvio si può prendere solo
     * quando le impostazioni sono state lette, e quella lettura arriva quando arriva:
     * la bandierina fa aspettare senza far succedere niente nel frattempo.
     */
    private var atStart = false

    /**
     * La cartella dell'avvio, aperta una volta sola.
     *
     * ⚠️⚠️ **Una volta per PROCESSO, e non a ogni tocco dell'icona**: chi è tornato alla
     * schermata iniziale ci è tornato apposta, e ritrovarsi la foto in faccia al tocco
     * successivo sarebbe l'app che non ascolta. Per questo la bandierina si spegne qui
     * e non si riaccende.
     */
    private fun openStartFolder(fresh: Settings) {
        if (!atStart) return
        atStart = false
        val bucket = fresh.startFolder ?: return
        if (!fresh.openAtStart) return
        // ⚠️⚠️ **PORTA ALLA GRIGLIA E NON PIÙ A UNA FOTOGRAFIA, dalla 0.48** (decisione
        // dell'utente, che aveva chiesto se questa scorciatoia servisse ancora). Fino alla
        // `0.47` apriva il visualizzatore sulla foto più recente della cartella: aveva
        // senso quando l'app si apriva su una schermata di scelte, molto meno da
        // quando la casa è **l'elenco delle cartelle** e sta nel 60% basso. La cosa che si
        // fa davvero è **scegliere fra le proprie foto**, e atterrare su una sola
        // costringeva a tornare indietro per vedere le altre.
        // ⚠️ Il nome arriva dalle impostazioni e non dal MediaStore: è scritto lì apposta
        // (vedi `Settings.startFolderName`), e chiederlo qui vorrebbe dire un'altra
        // interrogazione col permesso, all'avvio, per scrivere un titolo.
        openGrid(bucket, fresh.startFolderName)
    }

    /**
     * L'apertura in corso, per poterla annullare.
     *
     * ⚠️⚠️ **Serve perché un'immagine del web si SCARICA, e prima di questo nessuno
     * fermava quel download**: chi apriva un indirizzo sbagliato e tornava indietro
     * continuava a tirare giù decine di megabyte per una cosa che non avrebbe più
     * guardato, sulla sua connessione dati. Per un file locale il guadagno è piccolo,
     * per un indirizzo remoto è la differenza fra un'app che rispetta il traffico e una
     * che no.
     */
    private var loadJob: Job? = null

    /**
     * A quale apertura appartiene il progresso che arriva.
     *
     * ⚠️ Il download vive in un ciclo bloccante, quindi fra l'annullamento e l'ultimo
     * pezzo già in volo ci sta una frazione di secondo: senza questo numero, la
     * percentuale dell'immagine abbandonata comparirebbe per un istante sopra quella
     * appena aperta. Un contatore risolve quello che un `Job` non può, perché chi
     * riferisce il progresso non è dentro una coroutine.
     */
    private var loadToken = 0

    /**
     * L'unica strada per cui un indirizzo diventa una fotografia sullo schermo.
     *
     * ⚠️ Chi chiama ha già sistemato schermata, provenienza e serie: qui si fa **solo**
     * il caricamento, e i tre posti che aprono lo condividono invece di ripeterlo. Una
     * seconda copia divergerebbe sull'annullamento, che è la cosa che si dimentica.
     */
    private fun startLoad(uri: Uri, remember: Boolean = false) {
        val context = getApplication<Application>()
        loadJob?.cancel()
        val token = ++loadToken
        state = ViewerState.Loading()
        loadJob = viewModelScope.launch {
            val next = load(context, uri) { fraction ->
                if (token == loadToken) state = ViewerState.Loading(fraction)
            }
            // Remembered only once it has actually opened: a list of addresses that
            // failed would be a list of traps.
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
     * ⚠️ Esiste per la rete: un `403` di passaggio, il tunnel della metropolitana, un
     * server lento. Senza, l'unica via era tornare indietro e rifare tutto il giro
     * dell'indirizzo, e con un URL incollato dagli appunti vuol dire ritrovarlo.
     * ⚠️ Riprovando **si ripassa dalla cache**, quindi un errore avvenuto dopo un
     * download riuscito (la memoria finita in decodifica) si ritenta senza rete.
     */
    fun retry() {
        val uri = source ?: return
        startLoad(uri)
    }

    fun open(uri: Uri, backToApp: Boolean = true) {
        source = uri
        // ⚠️ Null vuol dire **esci dall'app**: chi è arrivato da un collegamento non ha
        // nessun posto dell'app in cui tornare, e trattenerlo in una schermata che non ha
        // chiesto sarebbe peggio che chiudersi.
        viewerBack = if (backToApp) HOME else null
        screen = Screen.Viewer
        listed = null
        startLoad(uri, remember = true)
        // ⚠️ The folder is looked for in its OWN coroutine, and not inside the one
        // above: it is a database query that the picture does not wait for, and
        // hanging it off the load would delay what the person is looking at in
        // order to prepare a gesture they may never make.
        val context = getApplication<Application>()
        viewModelScope.launch { listed = Folder.seriesAround(context, uri) }
    }

    // ⚠️⚠️ **QUI VIVEVA `openFolder`, che apriva una cartella dritta sulla sua foto più
    // recente, ed è uscita nella 0.48**: dalla `0.33` la chiamava solo l'avvio
    // automatico, e dalla `0.48` nemmeno quello, perché l'utente ha deciso che quella
    // scorciatoia deve portare alla **griglia**. Restava una funzione senza chiamanti.
    // ⚠️ Chi la volesse rimettere non riparta da `seriesAround`, che era la strada corta
    // e sbagliata: la cartella è già nota, quindi ripartire dall'immagine per chiedere al
    // MediaStore in quale cartella stia significa fare due volte la stessa domanda, la
    // seconda passando dalle chiavi del selettore, che sono il posto in cui questo pezzo
    // ha già sbagliato per cinque versioni. La strada buona è quella di `openGrid`:
    // `Folder.newestIn` più `atSequenceStart`.

    /** L'unico posto in cui un indirizzo diventa uno stato: i tre che aprono passano di qui. */
    private suspend fun load(
        context: android.content.Context,
        uri: Uri,
        onProgress: (Float) -> Unit
    ): ViewerState =
        when (val result = ImageSource.load(context, uri, onProgress)) {
            is LoadResult.Ok -> ViewerState.Ready(result.image)
            is LoadResult.Failed -> ViewerState.Error(result.reason.messageRes(), result.detail)
        }

    /**
     * L'elenco delle cartelle nella veste 'scegli quella dell'avvio'.
     *
     * ⚠️ Un tempo questa funzione portava un parametro, perché la stessa schermata la
     * apriva anche la voce 'Apri una cartella'. Dalla 0.41 quella voce non esiste più,
     * visto che l'elenco delle cartelle **è** la casa: restava un parametro che valeva
     * sempre `true`, cioè una scelta che non si poteva più fare.
     */
    fun chooseStartFolder() {
        screen = Screen.Folders(forStart = true)
    }

    /**
     * Fuori dalla scelta della cartella d'avvio: si torna alle impostazioni.
     *
     * ⚠️ Vale **solo** per quella veste: l'elenco delle cartelle nell'altra è la casa, e
     * da casa non si torna da nessuna parte. Là il gesto Indietro chiude l'app, che è
     * quello che ci si aspetta da una schermata iniziale.
     */
    fun leaveStartFolderChoice() {
        screen = Screen.Settings
    }

    /**
     * Una cartella scelta: o se ne apre la griglia, o diventa quella dell'avvio.
     *
     * ⚠️ Sceglierla **accende** anche l'avvio automatico, e non è un'iniziativa: si
     * arriva qui dall'interruttore o dal tasto sotto di lui, quindi l'intenzione è
     * quella. Spegnerlo resta un tocco, e la scelta rimane scritta.
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
     * Una cartella in miniature.
     *
     * ⚠️⚠️ **La serie che carica è la STESSA che poi sfoglia il visualizzatore** (finisce
     * in [listed], come quella trovata intorno a una foto): la griglia e la strisciata
     * leggono un solo elenco, quindi non possono dare due ordini diversi e aprire una
     * foto dalla griglia non costa nessuna seconda interrogazione del MediaStore.
     */
    fun openGrid(bucket: Long, name: String) {
        screen = Screen.Grid(bucket, name)
        listed = null
        source = null
        // Cartella nuova, nessuna foto ancora guardata.
        gridVisited = false
        // La fotografia di prima non serve più: tenerla vorrebbe dire tenere in memoria
        // un bitmap grande mentre si guarda tutt'altro.
        stopLoad()
        val context = getApplication<Application>()
        // ⚠️ Anche qui l'inizio è quello della sequenza SCELTA: la griglia si apre in
        // cima, e il tocco sulla prima miniatura dà la stessa foto da cui parte l'avvio.
        viewModelScope.launch { listed = Folder.newestIn(context, bucket).atSequenceStart() }
    }

    /**
     * Il testo che si sta cercando. Vuoto quando la ricerca si apre.
     *
     * ⚠️ Vive qui e non in [Screen.Search] perché cambia a ogni lettera: vedi la nota su
     * quella schermata.
     */
    var query: String by mutableStateOf("")
        private set

    /**
     * La ricerca in corso, per poterla **annullare** quando ne parte un'altra.
     *
     * ⚠️⚠️ **Senza questo la risposta più lenta vincerebbe sulla più recente**: digitando
     * `mare` partono quattro ricerche, e l'ordine in cui il MediaStore risponde non è quello
     * in cui sono partite. Chi non annulla si ritrova i risultati di `mar` sopra quelli di
     * `mare`, e il difetto si vede solo quando il telefono è occupato, cioè quasi mai
     * durante una prova.
     */
    private var searching: Job? = null

    /**
     * Apre il cestino.
     *
     * ⚠️ **La lista si spegne prima**: leggere una cartella dal disco costa poco ma non
     * niente, e mostrare per un istante le miniature della schermata di prima sarebbe un
     * lampeggio con dentro le foto sbagliate.
     * ⚠️ **Nessun anello dell'ultima vista**: si entra da un menu, non si sta tornando da
     * nessuna fotografia.
     */
    fun openBin() {
        screen = Screen.Bin
        listed = null
        source = null
        gridVisited = false
        stopLoad()
        val context = getApplication<Application>()
        viewModelScope.launch { listed = binLookup(context) }
    }

    /**
     * Il cestino come serie, pronto per la griglia.
     *
     * ⚠️ **Il cestino vuoto è una serie VUOTA e non un errore**: `Lookup.Unreadable`
     * farebbe scrivere alla griglia che la cartella non si legge, mentre un cestino vuoto è
     * la cosa più normale del mondo, e la griglia ha una frase apposta.
     * ⚠️ **Solo la serie piena passa da `atSequenceStart`**: su una lista vuota quel conto
     * darebbe indice `-1` col verso cronologico acceso, cioè un numero che non indica
     * niente e che poi gira per il resto del modello.
     */
    private suspend fun binLookup(context: Context): Folder.Lookup {
        val items = Bin.list(context)
        val whole = Folder.Lookup.Found(Folder.Series(items, 0))
        return if (items.isEmpty()) whole else whole.atSequenceStart()
    }

    /** Si entra nella ricerca a mani vuote: il testo di ieri non serve a nessuno. */
    fun openSearch() {
        screen = Screen.Search
        query = ""
        listed = Folder.Lookup.Found(Folder.Series(emptyList(), 0))
        source = null
        gridVisited = false
        stopLoad()
    }

    /**
     * Il testo cambia, e i risultati lo seguono dopo una pausa.
     *
     * ⚠️⚠️ **La PAUSA non è cortesia verso il database: è quello che rende la ricerca
     * scrivibile.** Senza, ogni lettera lancerebbe una query su tutta la galleria, e su
     * `panorama` sarebbero otto interrogazioni di cui sette buttate, con la tastiera che
     * rallenta mentre si scrive. Con la pausa parte solo l'ultima.
     * ⚠️ **Il testo si aggiorna SUBITO e i risultati dopo**, che è l'unico ordine
     * accettabile: un campo di testo che aspetta il database per mostrare la lettera
     * appena battuta sembra rotto.
     */
    fun search(text: String) {
        query = text
        searching?.cancel()
        val context = getApplication<Application>()
        val hidden = settings?.hiddenFolders.orEmpty()
        searching = viewModelScope.launch {
            delay(SEARCH_PAUSE_MS)
            listed = Folder.byName(context, text, hidden).atSequenceStart()
            // Una ricerca nuova è un elenco nuovo: l'anello dell'ultima foto vista
            // indicherebbe una posizione della lista di prima.
            gridVisited = false
        }
    }

    /**
     * Rilegge la cartella aperta, dopo che le sue foto sono cambiate sul disco.
     *
     * ⚠️⚠️ **NON PASSA DA [openGrid], ed è la differenza che conta**: quella riparte da
     * zero, cioè spegne la lista, ferma il caricamento e riporta la griglia in cima.
     * Dopo una copia o una rinomina la persona sta guardando un punto preciso della
     * cartella, e farla saltare all'inizio sarebbe farle perdere il posto per una
     * rilettura che dura un istante.
     * ⚠️ **La lista NON si azzera prima**: mostrare la rotellina al posto delle miniature
     * per il tempo di una query darebbe un lampeggio, e le foto di prima sono sbagliate
     * per pochi decimi di secondo, non inguardabili.
     * ⚠️ **L'anello dell'ultima guardata invece si spegne**: le righe del MediaStore
     * cambiano numero quando i file si spostano o si rinominano, quindi quell'indice
     * indicherebbe una fotografia a caso.
     */
    fun reloadGrid() {
        // ⚠️ Dalla 0.59 le griglie sono DUE, la cartella e i risultati di una ricerca, e
        // rileggere vuol dire due cose diverse. Rifare la ricerca dopo aver rinominato o
        // spostato è anzi il caso in cui serve di più: i nomi appena cambiati decidono chi
        // resta nell'elenco.
        gridVisited = false
        val context = getApplication<Application>()
        when (val here = screen) {
            is Screen.Grid ->
                viewModelScope.launch {
                    listed = Folder.newestIn(context, here.bucket).atSequenceStart()
                }
            Screen.Search -> {
                val hidden = settings?.hiddenFolders.orEmpty()
                val text = query
                searching?.cancel()
                searching = viewModelScope.launch {
                    listed = Folder.byName(context, text, hidden).atSequenceStart()
                }
            }
            // ⚠️ Il cestino si rilegge dal disco e non dal MediaStore, che là non guarda:
            // dopo un ripristino o uno svuotamento è l'unica cosa che dice la verità.
            Screen.Bin -> viewModelScope.launch { listed = binLookup(context) }
            else -> Unit
        }
    }

    /**
     * La foto toccata nella griglia.
     *
     * ⚠️ [index] è la posizione nell'ordine di **lettura**, cioè quello che la griglia
     * mostra: è la stessa lista, quindi la terza miniatura apre la terza foto della
     * sequenza, e non c'è nessuna conversione da fare.
     */
    fun openFromGrid(index: Int) {
        // ⚠️ Vale per tutt'e due le griglie, la cartella e i risultati: il gesto è lo
        // stesso, e la destinazione del ritorno è la schermata da cui si è partiti.
        val grid = screen.takeIf { it is Screen.Grid || it is Screen.Search || it is Screen.Bin }
            ?: return
        val current = series ?: return
        // Prima di cambiare schermata: entrare nel visualizzatore e poi accorgersi che
        // non c'è niente da mostrare lascerebbe una schermata vuota al posto della
        // griglia che c'era.
        if (current.at(index) == null) return
        viewerBack = grid
        // ⚠️ Si scrive QUI, nel momento del tocco, e non al ritorno: questo è il punto in
        // cui è certo che una foto della griglia si sta aprendo. La `0.34` lo scriveva
        // all'indietro, ed è il motivo per cui l'anello non compariva.
        gridVisited = true
        screen = Screen.Viewer
        showAt(current, index)
    }

    /** Fuori dalla griglia: si torna all'elenco delle cartelle, da dove ci si è arrivati. */
    fun leaveGrid() {
        screen = Screen.Folders(forStart = false)
        listed = null
    }

    /**
     * Indietro dal visualizzatore, dove [viewerBack] dice.
     *
     * ⚠️⚠️ **Tornando a una griglia la serie NON si butta**, ed è la ragione per cui
     * questa funzione non passa da [goHome]: è la stessa cartella, quindi la griglia
     * deve ritrovarla pronta invece di rileggerla, e con lei l'indice della foto da cui
     * si esce, che è dove la griglia si riposiziona.
     */
    fun backFromViewer() {
        when (val dest = viewerBack) {
            null -> Unit
            // ⚠️ La ricerca sta qui accanto alla cartella per la stessa ragione: i suoi
            // risultati sono costati una query e vanno ritrovati pronti, col testo ancora
            // nel campo. Passando da `goHome` si tornerebbe a un elenco vuoto.
            is Screen.Grid, Screen.Search, Screen.Bin -> {
                screen = dest
                source = null
                stopLoad()
            }
            else -> goHome()
        }
    }

    /**
     * La fotografia che si sta guardando non è più a quell'indirizzo: si rilegge la
     * cartella e si mostra quella che ha preso il suo posto.
     *
     * ⚠️⚠️ **UNA SOLA FUNZIONE PER SPOSTA, RINOMINA ED ELIMINA, e non è una
     * semplificazione**: in tutti e tre i casi la domanda è la stessa, 'che cosa c'è ora
     * in quella posizione della cartella'. Dopo un'eliminazione o uno spostamento c'è la
     * foto **seguente**, perché la lista si è accorciata; dopo una rinomina c'è la
     * **stessa** foto, con l'indirizzo nuovo, perché l'ordine è per data e la data non è
     * cambiata. La posizione è quello che si conserva, non l'indirizzo.
     * ⚠️ **Ci si aggancia alla POSIZIONE e non alla foto vicina**: cercare 'la prossima'
     * per indirizzo vorrebbe dire cercare un indirizzo in una lista che è appena cambiata.
     * ⚠️ **Se la cartella si è svuotata, o non c'è niente da rileggere, si torna
     * indietro**: restare su una fotografia che non esiste più mostrerebbe l'errore di
     * caricamento al posto di una galleria.
     * ⚠️ **Il caso senza cartella da rileggere è quello di una foto aperta da fuori**
     * (una chat, il web), e là dopo una rinomina si perde di vista il file: non c'è nessun
     * elenco in cui ritrovarlo, e inventarne uno vorrebbe dire interrogare il MediaStore
     * su un percorso che non si conosce più. Costa un ritorno alle cartelle, ed è raro.
     * ⚠️ **`gridVisited` NON si spegne**, al contrario di [reloadGrid]: là gli indici
     * cambiano sotto una griglia che li sta mostrando, qui la posizione la si riscrive
     * subito con quella giusta, quindi l'anello dell'ultima vista continua a dire il vero.
     */
    fun afterFileChanged() {
        val context = getApplication<Application>()
        val place = series?.index ?: 0
        val from = viewerBack
        viewModelScope.launch {
            val fresh = when (from) {
                is Screen.Grid -> Folder.newestIn(context, from.bucket)
                Screen.Search -> Folder.byName(context, query, settings?.hiddenFolders.orEmpty())
                Screen.Bin -> binLookup(context)
                else -> null
            }
            val reading = fresh.oriented()?.seriesOrNull
            if (reading == null || reading.items.isEmpty()) {
                backFromViewer()
                return@launch
            }
            // ⚠️ `listed` lo riscrive [showAt], nell'ordine grezzo: scriverlo anche qui
            // vorrebbe dire scriverlo due volte con due significati diversi.
            showAt(reading, place.coerceIn(0, reading.items.lastIndex))
        }
    }

    /**
     * The next or previous picture in the folder, if there is one.
     *
     * ⚠️⚠️ **The series is carried over rather than looked up again**, and it has to
     * be: rebuilding it from the new picture would query the database on every
     * swipe, and worse, a folder whose contents changed underneath would renumber
     * itself while it is being leafed through. One list, one order, until the
     * viewer is left.
     * ⚠️ It does NOT wrap around: at the last picture a swipe does nothing. Coming
     * back to the first one after the last is the kind of surprise that makes
     * people lose their place.
     * ⚠️ [delta] si conta nell'**ordine di lettura**, quello di [folder]: `+1` è la foto
     * dopo come la si sfoglia, cioè la più **vecchia** di default e la più **recente** col
     * verso cronologico acceso. Chi cerca il verso della strisciata non trova nessun segno
     * qui, ed è voluto: l'impostazione gira la serie, non il gesto.
     */
    fun step(delta: Int) {
        val current = series ?: return
        showAt(current, current.index + delta)
    }

    /**
     * Mostra la foto che sta in quella posizione della serie di lettura.
     *
     * ⚠️ Condivisa fra la strisciata ([step]) e il tocco sulla griglia
     * ([openFromGrid]), che sono la stessa cosa con due modi di dire quale foto: un
     * passo relativo e una posizione. Due copie divergerebbero sul punto peggiore, cioè
     * su come si riscrive [listed].
     * ⚠️ Fuori dalla serie non fa **niente**, e da qui viene il fatto che la sequenza
     * non gira su sé stessa: all'ultima foto la strisciata in avanti resta senza esito.
     */
    private fun showAt(current: Folder.Series, index: Int) {
        val uri = current.at(index) ?: return
        source = uri
        // ⚠️ `current` sta nell'ordine di lettura, `listed` in quello grezzo: si
        // rigira per riscriverlo, e la stessa funzione basta perché girare una serie
        // è la sua stessa inversa.
        listed = Folder.Lookup.Found(current.copy(index = index)).oriented()
        startLoad(uri)
    }

    /** Called once the permission dialog has been answered, whatever the answer. */
    fun folderAnswered(allowed: Boolean) {
        val context = getApplication<Application>()
        viewModelScope.launch { FolderAsk.remember(context) }
        // Granted while a picture is already open: the folder is looked for now,
        // instead of making the person open the same picture a second time.
        val uri = source
        if (allowed && uri != null) {
            viewModelScope.launch { listed = Folder.seriesAround(context, uri) }
        }
    }

    fun goHome() {
        screen = HOME
        source = null
        listed = null
        stopLoad()
    }

    fun openSettings() {
        screen = Screen.Settings
    }

    /** Out of the settings, back to whichever screen was showing the picture, or home. */
    fun leaveSettings() {
        screen = if (state is ViewerState.Ready) Screen.Viewer else HOME
    }

    fun updateSettings(next: Settings) {
        settings = next
        viewModelScope.launch { SettingsStore.save(getApplication(), next) }
    }

    fun forgetRecents() {
        viewModelScope.launch { Recents.clear(getApplication()) }
    }

    /**
     * Rimette in piedi tutti gli onboarding, come alla prima installazione.
     *
     * ⚠️ **Sta nel modello e non nella schermata**, come `forgetRecents`: quella scrive
     * nell'archivio, e una schermata che scrive nell'archivio da sé smette di essere una
     * schermata. ⚠️ Gira su **tutte** le voci di [Hint] e non su un elenco scritto a mano:
     * il giorno che ne nasce una quarta, questa funzione la copre già.
     */
    fun resetHints() {
        viewModelScope.launch { Hint.entries.forEach { it.forget(getApplication()) } }
    }

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
        // Only on a fresh start: on a rotation the ViewModel already holds the
        // picture, and re-reading the intent would load it a second time.
        if (savedInstanceState == null) model.handleIntent(intent)
        // ⚠️ Il tema si legge QUI, fuori da `AivApp`, perché deve avvolgerlo: dentro,
        // avrebbe già ereditato la tavolozza sbagliata. Finché le impostazioni non sono
        // arrivate vale il sistema, che è anche il valore di fabbrica della scelta.
        setContent {
            val chosen = model.settings?.uiTheme ?: UiTheme.SYSTEM
            AivTheme(darkTheme = chosen.isDark()) { AivApp(model) }
        }
    }

    /**
     * ⚠️⚠️ **GLI APPUNTI SI GUARDANO QUI E NON IN `onCreate`, e non è una preferenza di
     * ordine**: da Android 10 un'app può leggerli **solo mentre ha il fuoco**, e alla
     * creazione la finestra non ce l'ha ancora. Un controllo fatto là tornerebbe sempre a
     * mani vuote, e la funzione sembrerebbe scritta male invece che chiamata troppo
     * presto. Il modello si difende da sé dalle chiamate successive: il fuoco va e viene
     * a ogni dialogo di sistema.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        model.windowFocus(hasFocus)
    }

    /**
     * The activity is singleTop, so a second link arrives here instead of starting
     * a new copy of the app. Without this the screen would keep showing the
     * previous picture, which looks exactly like a bug.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        model.handleIntent(intent)
    }
}

@Composable
private fun AivApp(model: ViewerViewModel) {
    // ⚠️ PRIMA DI TUTTO IL RESTO, e non è una preferenza di ordine: la fabbrica va
    // dichiarata prima che una qualunque immagine chieda il caricatore, o quella
    // richiesta si prende quello predefinito e le miniature tornano a passare dalla
    // decodifica normale. Il perché di un caricatore nostro sta in `Thumbs`.
    setSingletonImageLoaderFactory { context -> Thumbs.loader(context) }
    val settings = model.settings
    if (settings == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        return
    }
    when (val screen = model.screen) {
        Screen.Settings -> {
            BackHandler { model.leaveSettings() }
            SettingsScreen(
                settings = settings,
                onChange = { model.updateSettings(it) },
                onStartFolder = { model.chooseStartFolder() },
                onResetHints = { model.resetHints() },
                onBack = { model.leaveSettings() }
            )
        }

        is Screen.Folders -> {
            // ⚠️ Il gesto Indietro si intercetta SOLO nella veste 'scegli la cartella
            // d'avvio': nell'altra questa è la casa, e da casa Indietro chiude l'app.
            if (screen.forStart) BackHandler { model.leaveStartFolderChoice() }
            FolderScreen(
                view = settings.folderView,
                columns = settings.folderColumns,
                counted = settings.folderCount,
                hidden = settings.hiddenFolders,
                // ⚠️ Una cartella senza percorso non si può nascondere, e allora non si
                // finge: il dialogo l'ha già chiesto, quindi qui si scarta in silenzio
                // invece di scrivere una chiave vuota che nasconderebbe la radice.
                onHide = { bucket ->
                    bucket.path?.let {
                        model.updateSettings(
                            settings.copy(hiddenFolders = settings.hiddenFolders + it)
                        )
                    }
                },
                recents = model.recents,
                onPick = { model.folderPicked(it, screen.forStart) },
                onOpen = { model.open(it) },
                onView = { model.updateSettings(settings.copy(folderView = it)) },
                onForget = { model.forgetRecents() },
                onSettings = { model.openSettings() },
                onSearch = { model.openSearch() },
                onBin = { model.openBin() },
                onBack = if (screen.forStart) ({ model.leaveStartFolderChoice() }) else null
            )
        }

        is Screen.Grid -> {
            BackHandler { model.leaveGrid() }
            // ⚠️ `null` mentre la ricerca è in corso, elenco VUOTO quando la cartella
            // non ha (più) niente da mostrare: sono due schermate diverse, il giro che
            // aspetta e il messaggio che spiega, e collassarle darebbe una rotellina
            // che non finisce mai.
            val lookup = model.folder
            GridScreen(
                title = screen.name,
                items = lookup?.let { it.seriesOrNull?.items ?: emptyList() },
                // ⚠️ L'indice si legge dalla serie VIVA e non da una copia: è quello
                // della foto mostrata per ultima nel visualizzatore, che la strisciata
                // tiene aggiornato. La bandierina dice solo se qualcosa è stato aperto.
                highlight = if (model.gridVisited) model.series?.index else null,
                onOpen = { model.openFromGrid(it) },
                onBack = { model.leaveGrid() },
                onChanged = { model.reloadGrid() },
                factFields = settings.factRows
            )
        }

        Screen.Search -> {
            BackHandler { model.leaveGrid() }
            // ⚠️ La stessa `GridScreen` della cartella, con due parametri in più: vedi la
            // nota su `query` là dentro per il perché non è una schermata a sé.
            val lookup = model.folder
            GridScreen(
                title = "",
                items = lookup?.let { it.seriesOrNull?.items ?: emptyList() },
                highlight = if (model.gridVisited) model.series?.index else null,
                onOpen = { model.openFromGrid(it) },
                onBack = { model.leaveGrid() },
                onChanged = { model.reloadGrid() },
                query = model.query,
                onQuery = { model.search(it) },
                factFields = settings.factRows
            )
        }

        Screen.Bin -> {
            BackHandler { model.leaveGrid() }
            // ⚠️ La stessa `GridScreen` di una cartella, con `bin` acceso: quello che cambia
            // sta là dentro (elimina definitiva, ripristina al posto di rinomina, e il
            // tastino anche senza selezione). Il cestino si naviga come una cartella
            // qualunque, che era la richiesta.
            val lookup = model.folder
            GridScreen(
                title = stringResource(R.string.bin_title),
                items = lookup?.let { it.seriesOrNull?.items ?: emptyList() },
                highlight = if (model.gridVisited) model.series?.index else null,
                onOpen = { model.openFromGrid(it) },
                onBack = { model.leaveGrid() },
                onChanged = { model.reloadGrid() },
                factFields = settings.factRows,
                bin = true
            )
        }

        Screen.Viewer -> {
            // Back returns to where the viewer was opened from, and only if there is
            // such a place. Opened from a link, Back has to leave the app: swallowing
            // it would trap the reader in a viewer they never chose to enter.
            // ⚠️ Il DOPPIO TOCCO non è una seconda via per uscire, ed è una scelta:
            // alterna adattata e 100%, e l'utente l'ha dichiarato *fondamentale*
            // (2026-08-27). Dargli anche il ritorno alla griglia significherebbe
            // toglierlo a una delle due direzioni dello zoom.
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
                onFileChanged = { model.afterFileChanged() },
                // ⚠️ Da dove si è entrati dice se si sta guardando il cestino: la
                // fotografia in sé non lo sa, ed è la ragione per cui questo dato non vive
                // nell'immagine.
                inBin = model.viewerBack is Screen.Bin
            )
        }
    }
}

/**
 * Asks for the folder permission, once, at the moment it would first be useful.
 *
 * ⚠️⚠️ **The condition is narrow on purpose, and every clause earns its place.** It
 * asks only for a picture that came off this phone (`content://`), only when the
 * permission is missing, and only if it has never been asked before. A viewer that
 * puts up a system dialog while somebody is looking at a photograph from a chat is
 * asking for something it cannot even use: a shared picture has no folder.
 * ⚠️⚠️ **Una volta sola vale ANCHE se la risposta è stata no**, ed è voluto: qui si
 * apre una pagina di sistema, e un'app che ce la rimanda davanti a ogni foto è
 * quella che insegna a rifiutare per riflesso. Chi cambia idea accende
 * l'interruttore dalle impostazioni del telefono, e l'app se ne accorge da sola
 * alla prossima immagine, perché `Folder.granted` chiede al sistema e non a un
 * valore che si era annotato.
 */
@Composable
private fun FolderPermission(model: ViewerViewModel) {
    val context = LocalContext.current
    // Due strade, perché i due permessi si concedono in due modi diversi: quello
    // ampio con un interruttore in una pagina di sistema, quello vecchio col
    // dialogo. La prima non restituisce un esito, quindi al ritorno si RICHIEDE
    // allo stato delle cose invece di credere a quello che l'intent dice.
    val fromSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { model.folderAnswered(Folder.granted(context)) }
    val fromDialog = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { allowed -> model.folderAnswered(allowed) }

    val source = model.source
    val local = source?.scheme?.lowercase() == "content"
    LaunchedEffect(source, model.folderAsked) {
        if (!local || model.folderAsked || Folder.granted(context)) return@LaunchedEffect
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            fromDialog.launch(Folder.legacyPermission)
            return@LaunchedEffect
        }
        // ⚠️ Una pagina di impostazioni che si apre da sola, senza una parola, è
        // il genere di cosa che fa chiudere l'app: il perché arriva prima.
        Toast.makeText(context, R.string.folder_why, Toast.LENGTH_LONG).show()
        // ⚠️ Il ripiego sulla pagina generale non è un lusso: quella mirata
        // all'app manca su qualche sistema, e senza il secondo tentativo la
        // richiesta morirebbe con un'eccezione invece di portare da qualche parte.
        val opened = Folder.settingsIntents(context).any { runCatching { fromSettings.launch(it) }.isSuccess }
        if (!opened) model.folderAnswered(false)
    }
}

/**
 * Three ways in, and they are all real: a tap on a link (VIEW with the address),
 * a file opened from another app (VIEW with a content:// address) and a share
 * (SEND with the address in an extra).
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

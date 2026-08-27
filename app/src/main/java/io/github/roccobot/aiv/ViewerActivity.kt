package io.github.roccobot.aiv

import android.app.Application
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed interface ViewerState {
    data object Loading : ViewerState
    data class Ready(val image: LoadedImage) : ViewerState
    data class Error(@param:StringRes val messageRes: Int, val detail: String?) : ViewerState
}

/** Quale schermata è davanti. */
sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data object Viewer : Screen

    /**
     * L'elenco delle cartelle.
     *
     * ⚠️ Porta [forStart] perché la stessa schermata risponde a due domande: 'quale
     * cartella apro adesso' e 'quale cartella apro all'avvio'. È l'unico dato che
     * distingue i due usi, e sta qui e non nel modello perché muore con la schermata.
     */
    data class Folders(val forStart: Boolean) : Screen
}

/**
 * The decoded picture lives in the ViewModel and not in the composition: a
 * rotation must not send the phone back to the network, and on a big file that
 * would be a visible pause rather than a purist's detail.
 */
class ViewerViewModel(application: Application) : AndroidViewModel(application) {

    var state: ViewerState by mutableStateOf(ViewerState.Loading)
        private set

    var screen: Screen by mutableStateOf(Screen.Home)
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

    /** Whether the viewer was reached from the opening screen, which decides where Back goes. */
    var cameFromHome: Boolean by mutableStateOf(false)
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

    /** Whether the folder permission has already been asked for once. */
    var folderAsked: Boolean by mutableStateOf(true)
        private set

    init {
        val context = getApplication<Application>()
        viewModelScope.launch {
            SettingsStore.flow(context).collect { fresh ->
                settings = fresh
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
            // Started from its own icon: the opening screen, not an error. Until
            // this existed the app showed a spinner and then 'no image to show',
            // which reads as 'this app does nothing'.
            screen = Screen.Home
            atStart = true
            settings?.let(::openStartFolder)
        } else {
            open(uri, fromHome = false)
        }
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
        if (fresh.openAtStart) openFolder(bucket)
    }

    fun open(uri: Uri, fromHome: Boolean = true) {
        source = uri
        cameFromHome = fromHome
        screen = Screen.Viewer
        state = ViewerState.Loading
        listed = null
        val context = getApplication<Application>()
        viewModelScope.launch {
            val next = load(context, uri)
            // Remembered only once it has actually opened: a list of addresses that
            // failed would be a list of traps.
            if (next is ViewerState.Ready) {
                Recents.remember(context, uri.toString(), ImageActions.fileName(next.image, uri))
            }
            state = next
        }
        // ⚠️ The folder is looked for in its OWN coroutine, and not inside the one
        // above: it is a database query that the picture does not wait for, and
        // hanging it off the load would delay what the person is looking at in
        // order to prepare a gesture they may never make.
        viewModelScope.launch { listed = Folder.seriesAround(context, uri) }
    }

    /**
     * Una cartella intera, aperta dalla sua foto più recente.
     *
     * ⚠️⚠️ **La serie NON si ricostruisce con `seriesAround`**, e sarebbe stata la strada
     * corta: qui la cartella la sappiamo già, quindi ripartire dall'immagine per
     * chiedere al MediaStore in quale cartella stia sarebbe fare due volte la stessa
     * domanda, la seconda per via indiretta e passando dalle chiavi del selettore, che
     * sono il posto in cui questa funzione ha già sbagliato per cinque versioni.
     */
    fun openFolder(bucket: Long, fromHome: Boolean = true) {
        cameFromHome = fromHome
        screen = Screen.Viewer
        state = ViewerState.Loading
        listed = null
        source = null
        val context = getApplication<Application>()
        viewModelScope.launch {
            val lookup = Folder.newestIn(context, bucket)
            // ⚠️⚠️ **SI PARTE DALL'INIZIO DELLA SEQUENZA SCELTA, non dalla foto più
            // recente in assoluto**: col verso cronologico acceso la più recente è
            // l'ULTIMA, e la cartella si aprirebbe su un vicolo cieco, con la strisciata
            // in avanti che non ha dove andare. È il difetto della 0.29, e si vedeva come
            // un gesto che non fa niente, cioè il sintomo che qui è già costato cinque
            // versioni.
            val whole = lookup.seriesOrNull
            val start = if (whole == null) lookup else {
                val first = if (settings?.reverseSequence == true) whole.items.lastIndex else 0
                Folder.Lookup.Found(whole.copy(index = first))
            }
            listed = start
            val series = start.seriesOrNull
            val uri = series?.at(series.index)
            if (uri == null) {
                // ⚠️ Si resta nel viewer con l'errore invece di tornare indietro in
                // silenzio: un tocco che riporta da dove si era partiti sembra un tocco
                // andato a vuoto, e non dice che la cartella si è svuotata.
                state = ViewerState.Error(R.string.folder_empty, null)
                return@launch
            }
            source = uri
            state = load(context, uri)
        }
    }

    /** L'unico posto in cui un indirizzo diventa uno stato: i tre che aprono passano di qui. */
    private suspend fun load(context: android.content.Context, uri: Uri): ViewerState =
        when (val result = ImageSource.load(context, uri)) {
            is LoadResult.Ok -> ViewerState.Ready(result.image)
            is LoadResult.Failed -> ViewerState.Error(result.reason.messageRes(), result.detail)
        }

    /** L'elenco delle cartelle, per aprirne una adesso o per sceglierla per l'avvio. */
    fun chooseFolder(forStart: Boolean) {
        screen = Screen.Folders(forStart)
    }

    /** Fuori dall'elenco: si torna da dove ci si è arrivati. */
    fun leaveFolders(forStart: Boolean) {
        screen = if (forStart) Screen.Settings else Screen.Home
    }

    /**
     * Una cartella scelta: o si apre, o diventa quella dell'avvio.
     *
     * ⚠️ Sceglierla **accende** anche l'avvio automatico, e non è un'iniziativa: si
     * arriva qui dall'interruttore o dal tasto sotto di lui, quindi l'intenzione è
     * quella. Spegnerlo resta un tocco, e la scelta rimane scritta.
     */
    fun folderPicked(bucket: Folder.Bucket, forStart: Boolean) {
        if (!forStart) {
            openFolder(bucket.id)
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
        val next = current.index + delta
        val uri = current.at(next) ?: return
        source = uri
        state = ViewerState.Loading
        // ⚠️ `current` sta nell'ordine di lettura, `listed` in quello grezzo: si
        // rigira per riscriverlo, e la stessa funzione basta perché girare una serie
        // è la sua stessa inversa.
        listed = Folder.Lookup.Found(current.copy(index = next)).oriented()
        val context = getApplication<Application>()
        viewModelScope.launch { state = load(context, uri) }
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
        screen = Screen.Home
        state = ViewerState.Loading
        source = null
        listed = null
    }

    fun openSettings() {
        screen = Screen.Settings
    }

    /** Out of the settings, back to whichever screen was showing the picture, or home. */
    fun leaveSettings() {
        screen = if (state is ViewerState.Ready) Screen.Viewer else Screen.Home
    }

    fun updateSettings(next: Settings) {
        settings = next
        viewModelScope.launch { SettingsStore.save(getApplication(), next) }
    }

    fun forgetRecents() {
        viewModelScope.launch { Recents.clear(getApplication()) }
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
        setContent { AivTheme { AivApp(model) } }
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
    val settings = model.settings
    if (settings == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        return
    }
    when (val screen = model.screen) {
        Screen.Home -> HomeScreen(
            recents = model.recents,
            onOpen = { model.open(it) },
            onFolders = { model.chooseFolder(forStart = false) },
            onSettings = { model.openSettings() },
            onForget = { model.forgetRecents() }
        )

        Screen.Settings -> {
            BackHandler { model.leaveSettings() }
            SettingsScreen(
                settings = settings,
                onChange = { model.updateSettings(it) },
                onStartFolder = { model.chooseFolder(forStart = true) },
                onBack = { model.leaveSettings() }
            )
        }

        is Screen.Folders -> {
            BackHandler { model.leaveFolders(screen.forStart) }
            FolderScreen(
                onPick = { model.folderPicked(it, screen.forStart) },
                onBack = { model.leaveFolders(screen.forStart) }
            )
        }

        Screen.Viewer -> {
            // Back returns to the opening screen only when that is where we came
            // from. Opened from a link, Back has to leave the app: swallowing it
            // would trap the reader in a viewer they never chose to enter.
            if (model.cameFromHome) BackHandler { model.goHome() }
            FolderPermission(model)
            ViewerScreen(
                state = model.state,
                settings = settings,
                source = model.source,
                folder = model.folder,
                onStep = { model.step(it) },
                onSettings = { model.openSettings() }
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

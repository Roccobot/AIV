package io.github.roccobot.aiv

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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.setSingletonImageLoaderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    /**
     * Un FILMATO, che non si decodifica e non si sfoglia come una fotografia.
     *
     * ⚠️⚠️ **È il quarto stato dalla `0.83`, e sta accanto agli altri tre invece di essere
     * un caso di [Ready]**: `Ready` porta una `LoadedImage`, cioè pixel, proporzioni, formato
     * e profondità di colore, e un video non ha niente di tutto questo da dare. Fingere un
     * `Ready` vuoto avrebbe fatto passare il filmato per tutta la catena dello zoom, della
     * tavolozza e delle informazioni, che gli chiedono cose che non ha.
     * ⚠️ **Porta il proprio indirizzo** anche se il modello ne ha già uno in `source`: chi
     * disegna lo stato deve poter chiedere la miniatura e la durata senza incrociare due
     * campi che potrebbero raccontare due momenti diversi.
     */
    data class Clip(val uri: Uri, val from: Arrival) : ViewerState

    data class Error(@param:StringRes val messageRes: Int, val detail: String?) : ViewerState
}

/**
 * Come si è arrivati su quello che si sta guardando.
 *
 * ⚠️⚠️ **VIVE NELLO STATO PERCHÉ È L'UNICA COSA CHE SOPRAVVIVE AL VIAGGIO**: fra il dito e la
 * superficie del lettore ci sono più funzioni che ricevono un indirizzo e nient'altro
 * (`showAt`, `startLoad`, `ClipStage`). Chiedere in fondo alla catena 'sono arrivato con un
 * tocco' non si può: là quell'informazione non c'è mai stata, e leggere l'impostazione da là
 * risponderebbe a un'altra domanda, cioè 'è acceso l'interruttore' invece di 'me l'hanno
 * chiesto'.
 * ⚠️ **Tre valori e non un booleano**: non sono 'sì, no e forse', sono tre provenienze con
 * tre risposte, e la terza è una porta che esiste anche se oggi un filmato non ci passa.
 * ⚠️⚠️ **È UN FATTO E NON UNA DECISIONE**: dice come si è arrivati, non che cosa fare. La
 * regola sta in un posto solo, [plays], così chi disegna lo stato non la rifà a modo suo.
 */
enum class Arrival {
    /**
     * Qualcuno ha toccato **questa** cosa: una miniatura della griglia, dei risultati o del
     * cestino, oppure una riga delle cartelle di sistema.
     */
    TAPPED,

    /**
     * È venuto su da sé: la strisciata, e la rilettura della cartella dopo uno spostamento,
     * una rinomina o un'eliminazione.
     *
     * ⚠️ I due casi stanno insieme perché la domanda è la stessa: nessuno ha chiesto
     * **questo** file, quindi niente parte da sé.
     */
    LEAFED,

    /**
     * Un indirizzo consegnato all'app da fuori: un collegamento, una condivisione, gli
     * appunti, il selettore di sistema, l'indirizzo digitato.
     *
     * ⚠️⚠️ **OGGI UN FILMATO NON ARRIVA MAI DA QUI, ed è misurato**: il manifesto dichiara il
     * solo tipo `image` col jolly, il selettore chiede `PickVisualMedia.ImageOnly` e gli
     * appunti passano da `ImageActions.looksLikeImage`, che ammette i soli `http(s)`. Il
     * valore esiste perché la porta esiste, e perché il giorno che si aprisse ai video la
     * risposta va **decisa** invece di ereditata da uno degli altri due valori.
     */
    HANDED;

    /**
     * Se arrivando così un filmato parte da sé, con `Settings.clipAutoplay` a quel valore.
     *
     * ⚠️⚠️ **LA REGOLA STA QUI E IN NESSUN ALTRO POSTO** (richiesta dell'utente, 2026-09-03:
     * *al tocco sulla miniatura di un video parte subito la riproduzione. Quando si sfoglia,
     * invece, i video sono sempre in modalità con 'Play' in sovrimpressione*).
     * ⚠️ **[LEAFED] non parte nemmeno con l'interruttore acceso**, e non è una dimenticanza:
     * è la seconda metà della richiesta. Là la persona non ha chiesto niente, e un audio che
     * esplode a metà di una strisciata è una sorpresa sgradevole.
     * ⚠️ **[HANDED] resta silenzioso finché non lo si decide**: oggi non è raggiungibile da un
     * filmato, quindi non c'è nessun comportamento da conservare, e ereditare la risposta di
     * un altro valore vorrebbe dire prendere una decisione senza accorgersene.
     */
    fun plays(autoplay: Boolean): Boolean = autoplay && this == TAPPED
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
     * cinque vie le trova dentro il FAB di `FolderScreen`.
     * ⚠️ Porta [forStart] perché la stessa schermata risponde a due domande: 'quale
     * cartella apro adesso' e 'quale cartella apro all'avvio'. È l'unico dato che
     * distingue i due usi, e sta qui e non nel modello perché se ne va con la schermata.
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
     *
     * ⚠️⚠️ **DALLA `1.83` PORTA UN CONFINE, E PER QUESTO NON È PIÙ UN OGGETTO** (risposta a
     * `d-fab-voci` del giro della `1.82`: la voce nuova del menu del FAB è *`cerca`*, con la sua
     * nota: *in quel caso, 'Cerca' è limitato alla cartella corrente*). Il bucket non è come il
     * testo: non cambia mentre si scrive, quindi appartiene all'identità della schermata e non al
     * modello. Cambiandolo si è in un'altra ricerca, ed è giusto che la griglia riparta.
     * ⚠️ **`null` è la ricerca di sempre**, quella di tutta la galleria che si apre dalla
     * schermata iniziale: il valore di serie fa sì che i suoi chiamanti non cambino di una riga.
     * ⚠️ **Il nome serve solo a scriverlo nel campo**, che è il modo di dire dove si sta cercando
     * senza una stringa nuova in ventotto lingue: il bucket da solo è un numero, e la testata non
     * ha nessun altro posto da cui ricavare il nome della cartella.
     */
    data class Search(val bucket: Long? = null, val name: String? = null) : Screen

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

    /**
     * La cronologia dei ripristini, dalla 0.76.
     *
     * ⚠️ **Si apre SOLO dal cestino e ci torna**, quindi non ha bisogno di sapere da dove
     * viene: la destinazione di Indietro è una sola, e scriverla qui sarebbe un dato che può
     * solo assumere un valore.
     */
    data object History : Screen

    /**
     * L'editor di casa, dalla 1.03: si ritaglia e si gira, e basta.
     *
     * ⚠️ **Porta il nome del file oltre all'indirizzo**, per la stessa ragione di [Grid]: il
     * nome serve in testata e serve al dialogo del salvataggio (per dire se il formato si può
     * riscrivere), e chi apre l'editor lo ha già letto. Ricavarlo di nuovo vorrebbe dire una
     * query al MediaStore per scrivere un titolo.
     * ⚠️ **Si apre SOLO dal menu del tocco lungo**, quindi la destinazione di Indietro è una
     * sola, il visualizzatore, e non ha bisogno di viaggiare qui dentro.
     */
    data class Editor(val uri: Uri, val name: String) : Screen

    /**
     * L'editor **completo**, dalla `2.14`: quello che cambia i pixel.
     *
     * ⚠️⚠️ **È UNA SCHERMATA A SÉ E NON UNA MODALITÀ DELL'ALTRA**, ed è la sua scelta (*due
     * editor separati*): i due hanno comandi diversi, un salvataggio diverso e un perimetro
     * diverso, e un `if` dentro una schermata sola avrebbe dovuto reggere tutte e due le
     * forme. ⚠️ **Porta il nome come l'altra**, e per la stessa ragione: serve al salvataggio
     * per sapere che formato ha in mano.
     */
    data class FullEditor(val uri: Uri, val name: String) : Screen
}

/**
 * La chiave con cui questa schermata ritrova quello che aveva da parte: vedi il
 * `SaveableStateProvider` in `AivApp`.
 *
 * ⚠️⚠️ **UNA STRINGA E NON LA SCHERMATA STESSA**, benché siano tutte `data class` e quindi
 * confrontabili: la chiave finisce in un `Bundle` quando il sistema mette via l'activity, e là
 * dentro ci vanno i tipi che un `Bundle` conosce. Un `Uri` o un oggetto nostro non ci starebbero.
 * ⚠️⚠️ **PORTA SOLO QUELLO CHE FA IDENTITÀ, e il nome della cartella non lo fa**: una cartella
 * rinominata resta la stessa cartella, e con il nome nella chiave si ritroverebbe in cima. Il
 * criterio è lo stesso con cui [Screen.Search] non porta il testo cercato.
 * ⚠️ **`forStart` invece fa identità**: la stessa schermata risponde a due domande diverse, e
 * chi sceglie la cartella d'avvio non sta scorrendo l'elenco di casa.
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
 * ⚠️ Le tre sono la cartella, i risultati di una ricerca e il cestino: si guardano allo
 * stesso modo e hanno la stessa selezione, quindi la domanda 'sono in una griglia' si fa in
 * più punti e scritta a mano ne dimenticherebbe una.
 */
private fun Screen.isGrid(): Boolean =
    this is Screen.Grid || this is Screen.Search || this == Screen.Bin

/**
 * Se in questa schermata il FAB può esserci.
 *
 * ⚠️⚠️ **SERVE A FARLO USCIRE RIMPICCIOLENDOSI, DALLA `2.11`** (punto A del campo libero del giro
 * accorpato: *quando dal menu del FAB approdo ad una schermata senza FAB (esempio → Impostazioni),
 * il pulsante deve sparire rimpicciolendosi fino a sparire*), e la domanda la fa la schermata che
 * **se ne va**, guardando dove si sta andando: vedi [LocalSenzaFab].
 * ⚠️ **Dice 'può', non 'ce l'ha'**: in una griglia il FAB compare solo se c'è dove mandare, e da
 * qui quel dato non si vede. Un falso positivo costa niente, perché un FAB che non c'è non ha
 * nessuna uscita da animare.
 * ⚠️ **Le quattro sono la casa e le tre griglie**, cioè [isGrid] più l'elenco delle cartelle: nel
 * visualizzatore, nell'editor, nelle impostazioni e nella cronologia il FAB non è mai esistito.
 */
private fun Screen.hasFab(): Boolean = this is Screen.Folders || isGrid()

/**
 * Se questa schermata, mentre si va verso [verso], è quella che se ne va **e** là il FAB non c'è.
 *
 * ⚠️⚠️ **VIVE IN UNA FUNZIONE SUA PER LA STESSA RAGIONE DI [cambioSchermata]: perché il banco
 * possa misurare questa e non una copia.** Sono due termini e una negazione, cioè la forma in cui
 * un `!` di troppo dà un FAB che sparisce quando dovrebbe restare, senza che niente vada in
 * errore.
 * ⚠️ **Il primo termine dice CHI SONO IO**: dentro la transizione la stessa funzione gira due
 * volte, una per il contenuto che esce e una per quello che entra, e solo il primo ha un FAB da
 * congedare. Senza quel termine sparirebbe anche il FAB della schermata che arriva.
 */
internal fun Screen.senzaFabVerso(verso: Screen): Boolean = this != verso && !verso.hasFab()

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
 * Quanto si aspetta, dopo l'ultima notizia del MediaStore, prima di rileggere la cartella.
 *
 * ⚠️ Molto più lunga di quella della ricerca, e per il motivo opposto: là si insegue un dito
 * che scrive e il ritardo si sente, qui si aspetta che un trasferimento finisca, e chi manda
 * venti file da un altro dispositivo li manda in qualche secondo. Sotto il mezzo secondo si
 * rileggerebbe a metà copia, e l'elenco crescerebbe a scatti sotto gli occhi.
 */
private const val OUTSIDE_PAUSE_MS = 700L

/**
 * Quanto restano in scena le cartelle nascoste: **un minuto**, e il numero è suo.
 *
 * ⚠️ **Non è una manopola**: la richiesta dice *temporaneamente (per un minuto)*, e un minuto è
 * quello che serve a entrare in una cartella nascosta, guardarla e uscirne. Chi ne volesse di più
 * ha 'Annulla' sulla notifica, che ne concede un altro.
 */
private const val PEEK_MS = 60_000L

/**
 * Fills [into] as far as the stream goes, and answers how many bytes it put there.
 *
 * ⚠️⚠️ **A SINGLE `read` IS ALLOWED TO RETURN LESS THAN IT WAS ASKED FOR, and that is the
 * whole reason this exists**: a comparison written on a bare `read` would see two identical
 * files answer with different chunk sizes and call them different. The loop is what makes a
 * short read a non-event.
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
 * The decoded picture lives in the ViewModel and not in the composition: a
 * rotation must not send the phone back to the network, and on a big file that
 * would be a visible pause rather than a purist's detail.
 */
class ViewerViewModel(application: Application) : AndroidViewModel(application) {

    var state: ViewerState by mutableStateOf(ViewerState.Loading())
        private set

    /**
     * Il filmato in scena è partito: da qui in poi la provenienza non chiede più niente.
     *
     * ⚠️⚠️ **SERVE ALLA ROTAZIONE, e senza di lei un filmato ripartirebbe dall'inizio a ogni
     * giro del telefono**: la composizione si rifà, il modello no, quindi l'effetto che
     * prepara il lettore leggerebbe di nuovo [Arrival.TAPPED] e rifarebbe partire da capo
     * quello che era già in corso. Consumato il fatto, la seconda lettura non trova più
     * niente da fare.
     * ⚠️ **Sta nel modello e non nella schermata perché [state] ha il setter privato**, ed è
     * giusto che l'abbia: chi disegna non riscrive lo stato, lo riferisce.
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

    /**
     * Se le cartelle nascoste sono in scena **adesso**, per il minuto che lui ha chiesto.
     *
     * ⚠️⚠️ **NASCE DALLA `1.92`, ED È SUA SPECIFICA ALLA LETTERA** (campo libero del giro della
     * `1.91`: *'Mostra nascoste', che rende visibili le cartelle nascoste temporaneamente (per un
     * minuto). Alla fine del minuto, le cartelle tornano ad essere nascoste con un avviso
     * temporizzato in basso*).
     * ⚠️⚠️ **VIVE NEL MODELLO E NON NELLA SCHERMATA, e la ragione è il caso d'uso**: le nascoste
     * si mostrano **per entrarci**, quindi fra l'accensione e la scadenza c'è un giro in una
     * cartella e il ritorno. Uno stato dentro `FolderScreen` se ne andrebbe con la composizione,
     * cioè si spegnerebbe proprio nel momento in cui serve.
     * ⚠️ **Non si salva**, e non è una dimenticanza: quello che si spegne da sé dopo un minuto
     * non ha senso ritrovarlo riaprendo l'app, dove il minuto sarebbe già passato da un pezzo.
     */
    var peeking: Boolean by mutableStateOf(false)
        private set

    /** Il conto alla rovescia del minuto: si annulla e si rifà a ogni proroga. */
    private var peekJob: Job? = null

    /**
     * Accende o spegne la vista temporanea delle cartelle nascoste.
     *
     * ⚠️⚠️ **LA SCADENZA PARLA, e il suo 'Annulla' PROROGA invece di disfare** (sue parole: *un
     * tasto 'Annulla' che prolunga di un minuto*). È lo stesso canale di ogni altro esito
     * (`Notices`, dalla `1.84`), quindi la voce dell'app resta una: quello che cambia è che qui
     * l'azione guarda avanti invece che indietro.
     * ⚠️ **Spegnere a mano non dice niente**: la notifica serve a spiegare una sparizione che
     * l'utente non ha chiesto, e chi tocca 'Nascondi cartelle' l'ha chiesta.
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
    val folder: Folder.Lookup? by derivedStateOf { listed.oriented().sifted() }

    /** Scorciatoia per chi della cartella vuole solo la serie, quando c'è. */
    val series: Folder.Series? get() = folder?.seriesOrNull

    /**
     * L'esito nel verso scelto dall'impostazione.
     *
     * ⚠️ Serve **in entrambi i versi** e una funzione sola basta, perché girare una
     * serie è la sua stessa inversa: di qui si passa sia per mostrare l'ordine di
     * lettura sia per riscrivere in [listed] quello grezzo (vedi [step]).
     */
    private fun Folder.Lookup?.oriented(): Folder.Lookup? {
        if (settings?.reverseSequence != true) return this
        /*
         * ⚠️⚠️ **IL VERSO NON TOCCA IL WEB, e l'impostazione lo dice da sé**: si chiama
         * 'cambia verso della sequenza immagini (in ordine di data)', ed è nata per una
         * galleria, dove sfogliare vuol dire andare avanti o indietro nel tempo. Una serie
         * remota è ordinata per **numero**, e girarla farebbe andare la strisciata da
         * `foto_013` a `foto_012`, cioè al contrario di quello che chiunque si aspetta
         * guardando un indirizzo numerato.
         */
        val first = this?.seriesOrNull?.items?.firstOrNull()
        if (first != null && WebSeries.isWeb(first)) return this
        return this?.reversed()
    }

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
        viewModelScope.launch { sweepClipLeftovers(context) }
    }

    /**
     * Butta via i modelli della ricerca per contenuto, che la `1.12` ha tolto.
     *
     * ⚠️⚠️ **SERVE PERCHÉ SUL TELEFONO DI CHI AGGIORNA RESTANO 86 MB CHE NESSUNO PUÒ PIÙ
     * TOGLIERE**: quei file li scaricava l'app in `filesDir/clip`, e il tasto che li
     * cancellava se n'è andato con la funzione. Senza questa riga sopravvivrebbero fino alla
     * disinstallazione o a un 'cancella dati', che porterebbe via anche il cestino.
     * ⚠️ **Costa un `stat` su una cartella che quasi sempre non c'è**, quindi non si tiene
     * una preferenza per ricordarsi di averlo fatto: quella sarebbe una chiave in archivio
     * per sempre, per risparmiare una chiamata al filesystem a ogni avvio.
     * ⚠️ **Si potrà togliere fra qualche versione**, quando nessuno aggiornerà più da una
     * `1.11` o precedente. Non prima, e non 'tanto ormai': chi salta dieci versioni esiste.
     */
    private suspend fun sweepClipLeftovers(context: Context) {
        withContext(Dispatchers.IO) {
            runCatching { File(context.filesDir, "clip").deleteRecursively() }
        }
    }

    /**
     * Se l'intento di partenza è già stato letto da **questa** istanza del modello.
     *
     * ⚠️⚠️ **È LA GUARDIA DELL'AVVIO, e vive qui perché il modello è la cosa che distingue una
     * rotazione dalla morte del processo**: sopravvive alla prima e non alla seconda, quindi la
     * risposta a 'l'ho già letto?' è giusta in tutti e due i casi. Il perché per esteso, col
     * difetto che chiude, è in `onCreate`.
     */
    var intentRead = false
        private set

    /**
     * La si chiama da `onCreate` e da `onNewIntent`, e **non** dalla composizione: leggere
     * l'intento mentre si compone vorrebbe dire rileggerlo a ogni ricomposizione, e a tenerlo
     * fermo era soltanto una guardia sull'indirizzo.
     */
    /**
     * Se questa esecuzione serve a **scegliere** un'immagine per un'altra app.
     *
     * ⚠️⚠️ **NASCE NELLA `1.89` DA UNA SUA RICHIESTA** (*vorrei che AIV comparisse anche quando
     * scelgo 'Altre app' su WhatsApp*): con `ACTION_GET_CONTENT` l'app si apre come sempre, e a
     * cambiare è una cosa sola, cioè che cosa fa il tocco su una miniatura. Là dove aprirebbe il
     * visualizzatore, consegna il file a chi lo ha chiesto e si chiude.
     * ⚠️ **È uno stato del modello e non un parametro di schermata**: l'intento si legge una
     * volta in `onCreate`, e la scelta deve sopravvivere alla rotazione e alla navigazione fra
     * cartelle, che sono esattamente le cose che il modello tiene in piedi.
     *
     * ⚠️⚠️ **E DALLA `1.91` LE AZIONI SONO DUE, PERCHÉ LA PRIMA NON BASTAVA** (riscontro del giro
     * della `1.89`, voce `selettore-app` non approvata: *non appare in elenco: né tra le app
     * galleria, né tra le app per allegare file*). Il filtro c'era davvero nell'APK, misurato sul
     * manifesto binario del file servito: a non bastare è la strada. `ACTION_PICK` è quella che
     * usa chi chiede *un'immagine dalla galleria*, ed è l'unica delle tre che un'app di terze
     * parti può ancora servire; il perché le altre due non si possano raggiungere vive nel
     * manifesto, accanto ai due filtri.
     */
    var picking: Boolean by mutableStateOf(false)
        private set

    fun handleIntent(intent: Intent?) {
        intentRead = true
        /*
         * ⚠️ **Si legge PRIMA di guardare l'indirizzo**: un `GET_CONTENT` non ne porta nessuno,
         * quindi finisce nel ramo di sotto insieme all'avvio dall'icona, che è giusto (si parte
         * dalle cartelle). Quello che lo distingue è solo questa bandierina.
         */
        picking = intent?.action == Intent.ACTION_GET_CONTENT ||
            intent?.action == Intent.ACTION_PICK
        /*
         * ⚠️⚠️ **UN `ACTION_PICK` PORTA UN INDIRIZZO, E NON È UN'IMMAGINE DA APRIRE**: chi lo
         * lancia ci mette la **sorgente** in cui scegliere, cioè di solito
         * `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`. Passandolo a `imageUri` l'app si
         * aprirebbe sul visualizzatore con in mano l'elenco intero invece di lasciar scegliere,
         * che è il difetto che questa riga evita. `GET_CONTENT` invece non porta niente, quindi
         * questa guardia non cambia il caso della `1.89`.
         */
        val uri = if (picking) null else intent.imageUri()
        if (uri == null) {
            // Partita dalla propria icona: si va dove stanno le immagini, cioè alle cartelle.
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
        if (hasFocus) backFromOutside()
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
        val clip = ImageActions.clipInClipboard(context) ?: return
        val uri = clip.address
        if (!ImageActions.looksLikeImage(uri)) return
        /*
         * ⚠️⚠️ **LO STESSO INDIRIZZO SI APRE UNA VOLTA SOLA** (richiesta dell'utente,
         * 2026-08-31): negli appunti una cosa ci resta per giorni, quindi senza questa
         * memoria l'app riaprirebbe la stessa fotografia a ogni avvio, e chi ha acceso
         * l'impostazione si troverebbe l'app che non ascolta più il tocco sull'icona.
         * ⚠️ Si confronta l'INDIRIZZO e non un 'già fatto' generico: copiando un indirizzo
         * nuovo la funzione deve tornare a rispondere, che è la ragione per cui esiste.
         * ⚠️⚠️ **MA DALLA 1.45 ANCHE RICOPIARE LO STESSO INDIRIZZO RIAPRE, e prima no**
         * (segnalazione dell'utente, 2026-09-03: *se l'URL è negli appunti, non si apre
         * all'avvio*). La nota della `0.94` diceva la cosa giusta a metà: un indirizzo
         * **nuovo** tornava a rispondere, ma lo stesso indirizzo copiato di nuovo è una
         * richiesta altrettanto esplicita, e restava indistinguibile da un link fermo là da
         * tre giorni. Adesso si guarda **anche l'istante** in cui gli appunti sono stati
         * scritti: più recente di quello che avevamo segnato, è una copia nuova.
         * ⚠️ **Le due prove sono in OR e non in AND**: basta che una delle due dica 'è
         * un'altra richiesta'. In AND, un indirizzo nuovo copiato da un sistema che non dà
         * l'istante (zero) non aprirebbe più niente.
         */
        val address = uri.toString()
        val again = clip.at > 0L && clip.at > fresh.clipboardWhen
        if (address == fresh.clipboardDone && !again) return
        // ⚠️ La scrittura è a parte e non blocca: quello che decide è il valore appena
        // confrontato, e l'apertura qui sotto deve restare **sincrona** per non perdere la
        // corsa con la cartella d'avvio.
        viewModelScope.launch { SettingsStore.clipboardOpened(context, address, clip.at) }
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
     * pezzo già in volo passa una frazione di secondo: senza questo numero, la
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
    private fun startLoad(uri: Uri, from: Arrival, remember: Boolean = false) {
        val context = getApplication<Application>()
        loadJob?.cancel()
        val token = ++loadToken
        // ⚠️⚠️ **UN FILMATO NON ENTRA NEL DECODIFICATORE, e si ferma qui** (`0.83`): non c'è
        // niente da aprire e niente da scaricare, quindi non c'è nemmeno un lavoro da
        // annullare. Il contatore però si è già mosso: se una fotografia si stava aprendo,
        // la sua percentuale in volo non deve comparire sopra il filmato.
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
        // ⚠️ La provenienza si CONSERVA e non si reinventa: riprovare non è un modo nuovo di
        // arrivare, e un ripiego a caso deciderebbe qui una cosa che si decide altrove.
        startLoad(uri, (state as? ViewerState.Clip)?.from ?: Arrival.HANDED)
    }

    fun open(uri: Uri, backToApp: Boolean = true) {
        // ⚠️⚠️ **L'indirizzo si riscrive in sicuro PRIMA di ogni altra cosa**, e da qui in
        // poi quello in chiaro non esiste più: se restasse, `source`, la condivisione, la
        // cache e le vicine dello sfogliatore parlerebbero di un indirizzo diverso da
        // quello che si è aperto davvero. Il perché sta in [WebSeries.secured].
        val target = WebSeries.secured(uri)
        upgraded = target != uri
        webRule = null
        source = target
        // ⚠️ Null vuol dire **esci dall'app**: chi è arrivato da un collegamento non ha
        // nessun posto dell'app in cui tornare, e trattenerlo in una schermata che non ha
        // chiesto sarebbe peggio che chiudersi.
        viewerBack = if (backToApp) HOME else null
        screen = Screen.Viewer
        listed = null
        startLoad(target, Arrival.HANDED, remember = true)
        // ⚠️ The folder is looked for in its OWN coroutine, and not inside the one
        // above: it is a database query that the picture does not wait for, and
        // hanging it off the load would delay what the person is looking at in
        // order to prepare a gesture they may never make.
        val context = getApplication<Application>()
        viewModelScope.launch {
            listed = if (WebSeries.isWeb(target)) webWindow(target) else Folder.seriesAround(context, target)
        }
    }

    /**
     * Apre le immagini di una **pagina** come serie, dalla prima.
     *
     * ⚠️⚠️ **È IL SESTO GRADINO DELLO SFOGLIATORE WEB, e la sua porta è il dialogo 'Apri un
     * indirizzo'** (istruzione dell'utente, 2026-09-03: *inizia a lavorarci*). Il perché
     * quella porta e non una condivisione nuova sta su `WebSeries.Rule.PAGE_LINKS`.
     * ⚠️ **Non passa da [open], e la differenza è una riga sola**: là la serie si cerca con
     * la cascata (`webWindow`), qui si ha già in mano ed è intera. Chiamare `open` e poi
     * sovrascrivere `listed` vorrebbe dire una richiesta di rete per niente, e una finestra
     * di tre indirizzi che compare per un istante al posto della serie vera.
     * ⚠️ **Il criterio si segna PRIMA di [startLoad]**: da lui dipende che la strisciata
     * non rifaccia la finestra a ogni passo (vedi [showAt]), e segnarlo dopo lascerebbe il
     * primo passo a rifarla.
     */
    fun openPage(page: Folder.Series) {
        val first = page.at(page.index) ?: return
        upgraded = false
        webRule = WebSeries.Rule.PAGE_LINKS
        source = first
        viewerBack = HOME
        screen = Screen.Viewer
        listed = Folder.Lookup.Found(page)
        startLoad(first, Arrival.HANDED, remember = true)
    }

    /**
     * Il criterio che ha funzionato per l'indirizzo remoto aperto, quando ce n'è uno.
     *
     * ⚠️ Vive nel modello e non dentro [WebSeries] perché è **stato di questa apertura**:
     * si azzera aprendo un altro indirizzo, e un oggetto senza stato non saprebbe quando.
     */
    private var webRule: WebSeries.Rule? = null

    /** Se l'indirizzo aperto è stato riscritto da `http` a `https`. */
    private var upgraded = false

    /**
     * Il filtro volatile della griglia: immagini, filmati o tutto.
     *
     * ⚠️⚠️ **SI AZZERA A OGNI INGRESSO IN UNA GRIGLIA, e non a mano** (richiesta
     * dell'utente): la sua ragione di esistere è portare il fuoco su un tipo *al volo*, e
     * un filtro che sopravvive al cambio di cartella diventa una cartella che sembra aver
     * perso metà delle sue cose. Le tre porte d'ingresso lo rimettono a [MediaKind.ALL], e
     * non c'è nessun altro posto in cui una griglia si apra.
     * ⚠️ **Filtra la SERIE e non solo quello che si vede**, cioè vale anche per la
     * strisciata nel visualizzatore: chi ha isolato i filmati e ne apre uno si aspetta di
     * sfogliare i filmati, non di ritrovarsi in mezzo alle fotografie che aveva appena
     * tolto di mezzo. È anche l'unica lettura che tiene gli indici d'accordo: la griglia e
     * il visualizzatore leggono la **stessa** lista.
     */
    var gridFilter: MediaKind by mutableStateOf(MediaKind.ALL)
        private set

    fun sift(kind: MediaKind) {
        gridFilter = kind
    }

    /**
     * L'esito senza le cose che il filtro esclude.
     *
     * ⚠️ **L'indice segue la fotografia e non il numero**: filtrando, la posizione corrente
     * cambia valore, e conservare il numero vecchio vorrebbe dire ritrovarsi l'anello su
     * una fotografia diversa. Si cerca dove è finita quella di prima, e se il filtro l'ha
     * appena esclusa si riparte da capo.
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
     * ⚠️ Il criterio **non si dimentica** se questa volta non ne ha vinto nessuno: alla fine
     * della serie nessuna vicina esiste, e cancellarlo là costringerebbe la strisciata
     * indietro a ripercorrere tutta la cascata.
     */
    private suspend fun webWindow(uri: Uri): Folder.Lookup {
        val window = WebSeries.around(uri, webRule)
        webRule = window.rule ?: webRule
        return window.lookup
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
            is LoadResult.Failed -> ViewerState.Error(reasonFor(result.reason), result.detail)
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
    /**
     * Il peso della cartella aperta e quanti video ha, per le due pastiglie dell'intestazione.
     *
     * ⚠️⚠️ **SI LEGGE UNA VOLTA PER CARTELLA E VIVE QUI**, come la cartella stessa: la griglia
     * riceve due numeri già pronti, e non ha modo di chiederli da sé perché conosce gli
     * indirizzi e non le righe. Il conto lo fa `Folder.weigh`, con una query sola.
     * ⚠️ **Si azzera aprendo**, o entrando in una cartella si vedrebbe per un istante il peso di
     * quella di prima, che è il genere di bugia che non si nota e resta.
     */
    var facts: Folder.Facts by mutableStateOf(Folder.Facts())
        private set

    /**
     * La tinta scelta per la cartella aperta, come indice in [FRONT_TINTS], oppure `null`.
     *
     * ⚠️⚠️ **VIVE QUI E NON FRA LE IMPOSTAZIONI, ED È UN DATO DELLA CARTELLA**: la scelta si fa
     * col tocco lungo sull'icona dell'intestazione (richiesta sua, giro della `1.83`), quindi non
     * è una preferenza dell'app ma una proprietà di quella cartella. Nel pannello sarebbe una
     * riga che chiede *di quale?*.
     * ⚠️ **Si legge una volta per cartella come [facts]**, e non da un flusso: cambia solo quando
     * lo cambia lui, e in quell'istante lo scriviamo qui e nell'archivio insieme.
     */
    var tint: Int? by mutableStateOf(null)
        private set

    /**
     * Segna (o toglie) la tinta della cartella aperta.
     *
     * ⚠️ **Scrive due volte di proposito**: lo stato in memoria, perché la fascia cambi colore
     * nello stesso fotogramma, e l'archivio, perché ci sia ancora domani. Aspettare la scrittura
     * per ridisegnare vorrebbe dire un colore che arriva dopo il tocco.
     */
    fun tintFolder(index: Int?) {
        val dove = (screen as? Screen.Grid)?.bucket ?: return
        tint = index
        // ⚠️ E dalla `1.87` anche la mappa della casa, per la stessa ragione della riga sopra:
        // uscendo di qui la cartella deve portare il colore nuovo senza aspettare una
        // rilettura, che con la guardia di [readBuckets] non arriverebbe affatto.
        folderTints = if (index == null) folderTints - dove else folderTints + (dove to index)
        val context = getApplication<Application>()
        viewModelScope.launch { FolderTints.set(context, dove, index) }
    }

    /**
     * La copertina scelta a mano per la cartella aperta, o `null` se ha quella automatica.
     *
     * ⚠️ **Vive qui accanto a [tint] e per la stessa ragione**: è un dato della cartella e non
     * una preferenza dell'app. Serve al menu del FAB, che offre di toglierla **se e solo se**
     * c'è, come la voce 'Mostra nascoste' della schermata iniziale.
     */
    var cover: Uri? by mutableStateOf(null)
        private set

    /**
     * Per quale cartella si sta scegliendo una copertina, e come si chiama.
     *
     * ⚠️⚠️ **È UNA MODALITÀ E NON UNA FINESTRA, PERCHÉ LO DICE LA SUA RISPOSTA** (risposta a
     * `d-copertina-come`, giro della `1.92`: *può essere scelta dalla normale vista di AIV da
     * qualsiasi cartella, non necessariamente quella di cui si sta impostando la copertina*).
     * Una finestra che elenca immagini sarebbe una seconda galleria da scrivere e da tenere
     * allineata a quella vera; così invece si sfoglia l'app com'è, e il tocco su una miniatura
     * vale come scelta.
     * ⚠️ **Vive nel modello e non in una schermata, come [peek]**: fra l'inizio e la scelta si
     * cambia cartella, quindi uno stato dentro `GridScreen` se ne andrebbe con la composizione,
     * cioè proprio nel momento in cui serve.
     * ⚠️ **Porta anche il nome**: la fascia che dice che cosa si sta facendo lo nomina, e alla
     * cartella di partenza non si torna, quindi da un identificatore non lo si ricaverebbe più.
     */
    var covering: Covering? by mutableStateOf(null)
        private set

    /** La cartella per cui si sta scegliendo una copertina. Vedi [covering]. */
    data class Covering(val bucket: Long, val name: String)

    /**
     * Il tocco sull'icona dell'intestazione: comincia a scegliere la copertina, **oppure** riporta
     * quella predefinita se la scelta era già partita da questa stessa cartella.
     *
     * ⚠️ **NON si esce dalla cartella**, ed è il caso comune: la copertina che si vuole è quasi
     * sempre una delle immagini che si hanno davanti. Per prenderne una di un'altra cartella
     * basta uscire e navigare, che è la seconda metà della sua richiesta.
     *
     * ⚠️⚠️ **IL SECONDO TOCCO SULLA STESSA CARTELLA RIPORTA LA PREDEFINITA, DALLA `1.95`, ED È IL
     * SOLO MODO DI TORNARCI** (sua richiesta, giro della `1.94`: *aggiungiamo un gesto ricorsivo:
     * se in modalità 'scegli copertina' tocco di nuovo l'icona dell'intestazione DELLA STESSA
     * CARTELLA, la copertina torna quella predefinita*). Con lui la voce del menu che toglieva la
     * copertina si spegne, e la ragione è sua: *un tocco involontario ripristina troppo facilmente
     * la copertina PREDEFINITA, e se non ricordi più qual era la personalizzata (o non ce l'hai
     * più) non puoi selezionarla di nuovo*. Qui il tocco involontario non basta: ce ne vogliono
     * due, e in mezzo c'è una fascia che dice che cosa sta succedendo.
     * ⚠️⚠️ **SU UN'ALTRA CARTELLA IL GESTO RICOMINCIA INVECE DI AZZERARE, e lo ha chiesto lui
     * dirimendo il caso** (*se tocco una cartella di intestazione, poi vado in un'altra cartella e
     * tocco la cartella dell'intestazione, si attiva la scelta della copertina per quella*). Cioè
     * il confronto è sull'identificatore, non sul fatto che una scelta sia in corso.
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
     * ⚠️⚠️ **QUI LO STATO SI SCRIVE DOPO E NON PRIMA, al contrario di [tintFolder]**, e la
     * differenza è che questa operazione **può non riuscire**: la copia decodifica l'immagine e
     * la scrive sul disco, quindi scrivere lo stato in anticipo vorrebbe dire una copertina che
     * la schermata mostra e che sul disco non c'è. Una tinta invece è un numero, e là non c'è
     * niente che possa andare storto.
     * ⚠️⚠️ **L'ESITO LO DICE UNA NOTIFICA, E NON È UN DI PIÙ**: la copertina si vede nella
     * schermata iniziale, cioè quasi mai in quella da cui si è scelto, quindi senza una riga che
     * parli il comando sembrerebbe non aver fatto niente. È lo stesso caso di 'Copia lista'.
     * ⚠️ **[cover] si aggiorna solo se la cartella aperta è quella**: scegliendo da un'altra
     * cartella, quella che si ha davanti non c'entra niente con la copertina appena scritta.
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
            /*
             * ⚠️ **Due frasi e non una, dalla `1.95`**: 'aggiornata' dice che c'è una copertina
             * nuova, e tornando alla predefinita non ce n'è nessuna. Con una frase sola il gesto
             * ricorsivo direbbe la stessa cosa dei due versi opposti, e quello che serve a chi lo
             * fa è sapere **quale** dei due è successo.
             */
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
     * ⚠️⚠️ **IL `BUCKET_ID` CAMBIA, QUINDI LA GRIGLIA SI RIAPRE SU UN'ALTRA CARTELLA**: per il
     * MediaStore l'identificatore viene dal percorso, e quello di prima non esiste più. Restando
     * sul vecchio, la stessa schermata mostrerebbe una cartella vuota senza dare nessun errore.
     * ⚠️⚠️ **E PRIMA DI RIAPRIRE SI TRAVASANO COPERTINA E COLORE**, che è la risposta alla sua
     * domanda (*cosa succede all'immagine memorizzata come copertina se rinomino la cartella da
     * AIV?*): sono appesi all'identificatore, quindi senza questo passaggio una rinomina li
     * perderebbe tutte le volte. Da **fuori** non si può, e là interviene la potatura di
     * [FolderCovers.sweep].
     * ⚠️ **Il percorso arriva da [facts], che la griglia ha già letto**: chiederlo di nuovo qui
     * vorrebbe dire una query in più per un dato che è già in mano.
     * ⚠️ **Senza percorso non si fa niente e si dice**: una cartella appena aperta può non averlo
     * ancora, e una rinomina che tace si legge come un'app che non risponde.
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
                        /*
                         * ⚠️ **Senza identificatore nuovo si torna alla casa invece di restare**:
                         * succede quando la cartella non ha media indicizzati, e là non c'è nessuna
                         * griglia da mostrare. Restare vorrebbe dire una schermata vuota che sembra
                         * un guasto.
                         */
                        screen = HOME
                    }
                    // ⚠️ La casa rilegge le cartelle da sé, perché il MediaStore è cambiato:
                    // legge [outsideStamp] come chiave, quindi basta muoverlo.
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

    fun openGrid(bucket: Long, name: String) {
        gridFilter = MediaKind.ALL
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
        facts = Folder.Facts()
        // ⚠️ **Anche la tinta si azzera**, per la stessa ragione dei numeri: entrando in una
        // cartella si vedrebbe per un istante il colore di quella di prima.
        tint = null
        // ⚠️ E la copertina scelta con lei, per la stessa ragione: la voce del menu che la
        // toglie compare solo se c'è, e col ricordo della cartella di prima comparirebbe dove
        // non c'è niente da togliere.
        cover = null
        viewModelScope.launch { listed = Folder.newestIn(context, bucket).atSequenceStart() }
        // ⚠️ In un lancio a sé e non in coda all'altro: i due numeri servono alla fascia in
        // cima, che si vede subito, mentre le miniature arrivano quando arrivano. Uno dopo
        // l'altro, la cartella si peserebbe solo dopo aver letto tutta la lista.
        viewModelScope.launch { facts = Folder.weigh(context, bucket) }
        viewModelScope.launch { tint = FolderTints.of(context, bucket) }
        viewModelScope.launch { cover = FolderCovers.of(context, bucket) }
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
        gridFilter = MediaKind.ALL
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

    /**
     * Apre la cronologia dei ripristini, e ci si torna dal cestino.
     *
     * ⚠️⚠️ **NON AZZERA NIENTE, al contrario di [openBin]**, ed è la differenza che conta: si
     * va e si torna, quindi la griglia del cestino deve ritrovarsi **pronta** invece di
     * essere riletta dal disco. È la stessa scelta di `openSettings`, e la ragione per cui
     * `leaveHistory` rimette in scena una schermata che non ha mai smesso di avere i suoi
     * dati.
     */
    fun openHistory() {
        screen = Screen.History
    }

    /** Indietro dalla cronologia: il cestino, che è il solo posto da cui si apre. */
    fun leaveHistory() {
        screen = Screen.Bin
    }

    // ── Modifica di una fotografia (1.03) ──

    /**
     * Se il selettore dell'app di modifica è davanti.
     *
     * ⚠️ **Vive nel modello e non nella schermata, perché lo aprono in DUE**: il menu del
     * tocco lungo al primo uso, e le impostazioni quando si vuole cambiare idea. Tenuto in
     * una delle due schermate, l'altra avrebbe dovuto averne una copia propria, e le due
     * copie sarebbero divergite al primo ritocco.
     */
    var editorAsk: Boolean by mutableStateOf(false)
        private set

    /**
     * La fotografia che aspetta un editor, e `null` quando la domanda arriva dalle
     * impostazioni.
     *
     * ⚠️ È ciò che distingue i due usi del **medesimo** selettore: scelta l'app, con un
     * indirizzo si prosegue e si apre la fotografia, senza si è solo cambiata un'impostazione.
     * Non è uno stato di composizione (nessuno lo disegna), quindi non è `mutableStateOf`.
     */
    private var editorFor: Uri? = null

    /** Se il salvataggio dell'editor di casa è in corso: il tasto deve smettere di rispondere. */
    var editorBusy: Boolean by mutableStateOf(false)
        private set

    /**
     * Una frase da mostrare e buttare, e `null` quando non ce n'è nessuna.
     *
     * ⚠️ **Sta nel modello perché l'esito arriva DOPO la schermata che l'ha chiesto**: il
     * salvataggio chiude l'editor, quindi un avviso appeso a quella composizione se ne andrebbe
     * prima di comparire. Chi lo mostra lo azzera con [noticeShown], e senza quell'azzeramento
     * tornerebbe a ogni ricomposizione.
     */
    var notice: Int? by mutableStateOf(null)
        private set

    /** L'avviso è stato mostrato: si toglie, o riapparirebbe al primo ridisegno. */
    fun noticeShown() {
        notice = null
    }

    /**
     * Il menu chiede di modificare una fotografia: tre vie, e la prima volta si domanda.
     *
     * ⚠️ **L'impostazione vuota e 'nessuna' sono cose diverse**, ed è la ragione per cui il
     * valore di fabbrica è la stringa vuota: vuota vuol dire *non ho mai chiesto*, ed è
     * l'unico caso in cui si apre il selettore da sé. Vedi `Settings.editorApp`.
     */
    fun edit(uri: Uri) {
        val chosen = settings?.editorApp.orEmpty()
        when {
            chosen.isBlank() -> {
                editorFor = uri
                editorAsk = true
            }
            chosen == Editors.INTERNAL -> openEditor(uri)
            chosen == Editors.FULL -> openFullEditor(uri)
            else -> openOutside(uri, chosen)
        }
    }

    /**
     * Il tocco lungo su 'Modifica': si sceglie l'app **adesso**, per questa fotografia.
     *
     * ⚠️⚠️ **LA DIFFERENZA CON [edit] È UNA SOLA, e sta tutta qui**: [edit] apre il selettore
     * **solo** la prima volta e poi non lo chiede più, che è quello che deve fare una scelta
     * memorizzata; questo lo apre **sempre**, perché è il gesto con cui si cambia idea senza
     * passare dalle impostazioni.
     * ⚠️ **La scelta resta memorizzata** (richiesta dell'utente): non è un'apertura 'con
     * un'altra app solo stavolta', è la stessa scelta di sempre fatta da un posto più comodo.
     * Ci pensa [editorChosen], che è lo stesso di prima e non sa da dove gli arriva l'utente.
     */
    fun editWith(uri: Uri) {
        editorFor = uri
        editorAsk = true
    }

    /** Le impostazioni vogliono cambiare l'app: stesso selettore, senza nessuna fotografia. */
    fun chooseEditor() {
        editorFor = null
        editorAsk = true
    }

    /**
     * L'utente ha scelto: si ricorda, e se c'era una fotografia in attesa si prosegue.
     *
     * ⚠️⚠️ **SI PROSEGUE CON `id` E NON RILEGGENDO LE IMPOSTAZIONI**: la scrittura è
     * asincrona (DataStore), quindi un istante dopo `settings.editorApp` porta ancora il
     * valore di prima, e ripassare da [edit] riaprirebbe il selettore appena chiuso.
     */
    fun editorChosen(id: String) {
        editorAsk = false
        val waiting = editorFor
        editorFor = null
        settings?.let { updateSettings(it.copy(editorApp = id)) }
        if (waiting == null) return
        when (id) {
            Editors.INTERNAL -> openEditor(waiting)
            Editors.FULL -> openFullEditor(waiting)
            else -> openOutside(waiting, id)
        }
    }

    /** Selettore chiuso senza scegliere: non si ricorda niente e non si apre niente. */
    fun editorSkip() {
        editorAsk = false
        editorFor = null
    }

    /** L'editor di casa: il nome serve alla testata e al dialogo del salvataggio. */
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
     * L'editor completo: come [openEditor], e il nome serve per la stessa ragione.
     *
     * ⚠️ **Nessuna guardia di versione qui**: la voce non si offre affatto dove il conto non
     * puo girare (vedi `EditorPicker`), e una scelta salvata su un telefono che non la regge
     * torna a dire 'nessuno scelto' (vedi `Editors.labelOf`). Ripetere il controllo in un terzo
     * posto vorrebbe dire tre risposte da tenere d'accordo.
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
     * Un'app di fuori.
     *
     * ⚠️ **Se non parte si torna a CHIEDERE invece di dire soltanto che è andata male**: il
     * caso normale è l'app disinstallata, e un avviso senza via d'uscita lascerebbe la voce
     * 'Modifica' rotta per sempre, perché la scelta memorizzata punta a qualcosa che non
     * c'è. Riaprire il selettore è la sola risposta che rimette in piedi la funzione.
     *
     * ⚠️⚠️ **LA COPIA DI SICUREZZA VALE ANCHE QUI, dalla 1.13** (domanda dell'utente: *vale
     * solo per l'editor interno o per tutti quelli che supportano 'Modifica'?*). L'interruttore
     * promette che una modifica non fa perdere l'originale, e chi lo accende non sta pensando
     * a **quale** editor riscriverà il file: un'app di fuori lo riscrive esattamente come
     * quello di casa, e prima della 1.13 era la sola via da cui l'originale se ne andava per
     * sempre.
     * ⚠️⚠️ **SI COPIA PRIMA DI LANCIARE, e l'ordine è tutto**: l'editor esterno può salvare
     * un istante dopo essersi aperto, quindi una copia fatta 'intanto' arriverebbe a
     * fotografare il file già riscritto, cioè salverebbe il nulla credendo di aver salvato
     * qualcosa.
     * ⚠️ **Una copia che non riesce ferma il giro**, come nell'editor di casa e per la stessa
     * ragione: aprire lo stesso vorrebbe dire dare a chi si stava proteggendo proprio il
     * rischio da cui si proteggeva, in silenzio.
     * ⚠️ **Se poi l'editor non parte, la copia si ritira**: quel file non è stato toccato da
     * nessuno, e lasciare nel cestino una copia di un'immagine ancora intatta è spazzatura
     * che l'utente dovrebbe capire e buttare da sé.
     * ⚠️⚠️ **DOVE NON C'È UN FILE NOSTRO NON SI BLOCCA NIENTE, e senza questa riga la
     * modifica si sarebbe rotta su tutte le immagini prese dal web**: un indirizzo che non
     * corrisponde a un file su disco (una pagina remota, un allegato di un'altra app) non ha
     * un originale che noi possiamo perdere, quindi la promessa dell'interruttore è già
     * mantenuta senza fare niente. Trattare quel caso come una copia fallita avrebbe negato
     * l'editor proprio dove non c'era nulla da proteggere.
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

    /** Indietro dall'editor di casa: il visualizzatore, che è il solo posto da cui si apre. */
    fun leaveEditor() {
        screen = Screen.Viewer
    }

    /**
     * La fotografia mandata a un'app di fuori, finché non si torna.
     *
     * ⚠️ Serve perché un editor esterno **non risponde**: si apre con `startActivity` e non
     * con una richiesta di esito, quindi l'unico momento in cui si può sapere che il giro è
     * finito è quando la finestra di AIV riprende il fuoco.
     */
    private var editedOutside: Uri? = null

    /** La copia messa nel cestino prima di uscire, finché non si sa se è servita. */
    private var keptOutside: File? = null

    /**
     * Si torna da un'app di fuori: quello che ha modificato va riletto.
     *
     * ⚠️⚠️ **SENZA QUESTO SI TORNEREBBE ALLA FOTOGRAFIA DI PRIMA, e sembrerebbe che l'editor
     * non abbia salvato**: l'indirizzo non cambia, quindi né la miniatura in memoria né
     * l'immagine già decodificata hanno un motivo per rifarsi. È il difetto classico di chi
     * apre un'app esterna e non ricarica al ritorno.
     * ⚠️ **Si rilegge anche quando l'editor non ha salvato niente**, e va bene: costa una
     * decodifica in un momento in cui non si sta facendo altro, mentre la via furba
     * (guardare la data del file) sbaglierebbe su un editor che riscrive senza cambiarla.
     *
     * ⚠️⚠️ **QUI SI DECIDE SE LA COPIA RESTA, e la domanda non è 'l'editor ha salvato?' ma
     * 'il file è cambiato?'**: un editor esterno non risponde niente, quindi la sola prova
     * che esista è il file stesso. Chi apre 'Modifica' e poi esce senza toccare niente non
     * deve trovarsi un doppione nel cestino a ogni ripensamento.
     * ⚠️⚠️ **SI CONFRONTA IL CONTENUTO E NON LA DATA, ed è già scritto qui sopra perché
     * questa funzione ci era cascata una volta**: la data di modifica la cambia anche chi
     * riscrive gli stessi identici byte, e non la cambia affatto qualche editor che la
     * conserva apposta. Sbaglierebbe in tutte e due le direzioni, e quella grave è buttare
     * la copia di un file che invece è stato riscritto.
     * ⚠️⚠️ **E DALLA 1.48 LA COPIA BUTTATA SI DICE** (istruzione dell'utente, ripetuta nel
     * giro della `1.47`: *farlo sapere all'utente con una notifica toast*). Cancellarla in
     * silenzio era giusto e insieme indistinguibile da una promessa non mantenuta: chi ha
     * acceso l'interruttore va a cercare la copia nel cestino, non la trova, e non ha modo di
     * sapere se manca perché non serviva o perché la protezione non ha funzionato.
     * ⚠️ **L'avviso esce SOLO qui**, ed è l'unico posto in cui il caso esiste: l'editor di
     * casa rifiuta un salvataggio che non cambia niente (`edit_nothing`), e la pulizia di un
     * SVG si ferma prima di fare la copia quando non c'è niente da togliere.
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
     * Se due file portano gli stessi identici byte.
     *
     * ⚠️ **La lunghezza si guarda per prima e chiude quasi tutti i casi**: un editor che
     * salva cambia quasi sempre il peso del file, e leggerne due da capo a fondo per
     * scoprirlo sarebbe lavoro buttato. Restano da leggere davvero i soli file che pesano
     * uguale, che sono il caso raro.
     * ⚠️ **A blocchi e non tutto in memoria**: qui non c'è il tetto che [Animations] si
     * impone, perché una fotografia da centinaia di megabyte è rara ma non impossibile, e
     * leggerne due intere per confrontarle la renderebbe una chiusura dell'app.
     * ⚠️ **In caso di errore risponde 'diversi'**, che è la risposta prudente: la copia
     * resta nel cestino, e il peggio che succede è un file di troppo da buttare a mano.
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
     * Salva quello che l'editor di casa ha in mano.
     *
     * ⚠️⚠️ **GIRA NELL'AMBITO DEL MODELLO E NON DELLA SCHERMATA, ed è il punto**: il primo
     * atto di un salvataggio riuscito è chiudere l'editor, cioè smontare la composizione che
     * l'ha chiesto. Un lavoro appeso a quella si interromperebbe a metà scrittura, sul file vero.
     * ⚠️ La miniatura si butta **solo dopo un esito buono**: il ritaglio fallito lascia il
     * file com'era, e cancellarla costringerebbe a rigenerarla per niente.
     */
    fun editSave(turns: Int, mirror: Boolean, crop: ImageEdit.Crop) {
        if (editorBusy) return
        val here = screen as? Screen.Editor ?: return
        val context = getApplication<Application>()
        /*
         * ⚠️⚠️ **SI SOVRASCRIVE, E NON LO SI CHIEDE PIÙ, dalla 1.08** (richiesta dell'utente):
         * la domanda proteggeva dalla sola cosa irreversibile dell'editor, e dalla `1.03`
         * quella cosa non è più irreversibile, perché l'originale finisce nel cestino prima di
         * essere riscritto.
         * ⚠️⚠️ **LA COPIA RESTA PER I FORMATI CHE NON SI SANNO RISCRIVERE, e non è una
         * scappatoia**: di un HEIC o di un AVIF si leggono i pixel e non si sanno rimettere
         * dentro, quindi l'unica uscita è un JPEG, e un JPEG dentro un file che si chiama
         * `.heic` mentirebbe per sempre sul proprio contenuto. Là esce una copia accanto,
         * l'avviso finale lo dice, e non c'è niente da scegliere: lo decide il formato.
         * ⚠️ **Il formato lo guarda il MODELLO e non la schermata**: quella sceglie che cosa
         * tagliare e di quanto girare, che è il suo mestiere; come si chiama il file che ne
         * esce è una faccenda di file.
         */
        val way = if (ImageEdit.canOverwrite(here.name)) ImageEdit.Way.OVERWRITE
        else ImageEdit.Way.COPY
        editorBusy = true
        viewModelScope.launch {
            val esito = ImageEdit.save(
                context, here.uri, turns, mirror, crop, way,
                backup = settings?.editorBackup ?: true
            )
            editorBusy = false
            when (esito) {
                is ImageEdit.Result.Failed -> notice = esito.why
                is ImageEdit.Result.Done -> {
                    notice = when {
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
     * ⚠️⚠️ **GIRA NELL'AMBITO DEL MODELLO, come l'altro e per la stessa ragione**: il primo atto
     * di un salvataggio riuscito è chiudere l'editor, cioè smontare la composizione che l'ha
     * chiesto, e un lavoro appeso a quella si fermerebbe a metà scrittura sul file vero. Qui pesa
     * di piu: applicare il conto a venti megapixel dura secondi, non decimi.
     * ⚠️ **Il formato di uscita lo decide la QUALITÀ e non questa funzione**: 'Senza perdita'
     * scrive un PNG accanto, le altre due riscrivono il file dov'è. Il conto vive in
     * `ImageEdit.saveLook`, che è il posto in cui vivono tutte le domande sui file.
     */
    fun lookSave(look: Look) {
        if (editorBusy) return
        val here = screen as? Screen.FullEditor ?: return
        val context = getApplication<Application>()
        editorBusy = true
        viewModelScope.launch {
            val esito = ImageEdit.saveLook(
                context, here.uri, look,
                quality = settings?.editorQuality ?: Quality.DEFAULT,
                backup = settings?.editorBackup ?: true
            )
            editorBusy = false
            when (esito) {
                is ImageEdit.Result.Failed -> notice = esito.why
                is ImageEdit.Result.Done -> {
                    /*
                     * ⚠️ **La miniatura si butta solo quando si è riscritto SOPRA**: con 'Senza
                     * perdita' esce un file nuovo, e quella di partenza è ancora giusta.
                     */
                    val sopra = esito.file.absolutePath ==
                        FileTree.fileOf(context, here.uri)?.absolutePath
                    notice = if (sopra) R.string.editor_done else R.string.editor_done_copy
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
     * Si entra nella ricerca a mani vuote: il testo di ieri non serve a nessuno.
     *
     * ⚠️ **I due argomenti sono il confine**, e senza di loro si cerca in tutta la galleria come
     * si è sempre fatto: li passa il menu del FAB di una cartella, che è l'unico posto da cui si
     * chiede una ricerca ristretta.
     */
    fun openSearch(bucket: Long? = null, name: String? = null) {
        gridFilter = MediaKind.ALL
        screen = Screen.Search(bucket, name)
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
        // ⚠️ Il confine si legge dalla schermata a ogni ricerca e non si tiene in un campo: è un
        // dato della schermata (vedi [Screen.Search]), e una seconda copia direbbe il falso il
        // giorno che si passa da una ricerca ristretta a una globale.
        val entro = (screen as? Screen.Search)?.bucket
        searching = viewModelScope.launch {
            delay(SEARCH_PAUSE_MS)
            listed = Folder.byName(context, text, hidden, entro).atSequenceStart()
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
            is Screen.Search -> {
                val hidden = settings?.hiddenFolders.orEmpty()
                val text = query
                val entro = here.bucket
                searching?.cancel()
                searching = viewModelScope.launch {
                    listed = Folder.byName(context, text, hidden, entro).atSequenceStart()
                }
            }
            // ⚠️ Il cestino si rilegge dal disco e non dal MediaStore, che là non guarda:
            // dopo un ripristino o uno svuotamento è l'unica cosa che dice la verità.
            Screen.Bin -> viewModelScope.launch { listed = binLookup(context) }
            else -> Unit
        }
    }

    /**
     * Le cartelle di immagini del telefono, e `null` finché la prima lettura non è finita.
     *
     * ⚠️⚠️ **VIVE QUI E NON NELLA SCHERMATA, dalla `1.46`, per la stessa ragione di
     * [treePath]**: dalla casa si esce ogni volta che si apre un'immagine, e un ricordo dentro
     * il composabile muore con lui. Prima l'elenco era un `remember` della schermata, quindi
     * ogni ritorno dal visualizzatore rifaceva la query sul MediaStore e rimetteva la rotella
     * al posto delle copertine, per riottenere l'elenco di un attimo prima.
     * ⚠️ **Non filtrato**: le cartelle nascoste le toglie la schermata, perché sono una
     * preferenza e non un fatto del disco. Così nasconderne una si vede all'istante, senza
     * chiedere niente al MediaStore.
     */
    var buckets: List<Folder.Bucket>? by mutableStateOf(null)
        private set

    /**
     * Per quale stato delle cose l'elenco qui sopra è stato letto: permesso e [outsideStamp].
     *
     * ⚠️⚠️ **È QUESTO A RENDERE LA LETTURA RARA, non il fatto che il valore viva nel
     * modello**: la schermata rinasce a ogni ritorno, quindi il suo effetto di avvio riparte
     * comunque, e senza un confronto rifarebbe la query esattamente come prima.
     * ⚠️ **Porta anche il permesso dell'ultima volta**, e serve a [outsideChange]: quando una
     * notizia arriva mentre la casa è in scena, nessuno gliela può chiedere.
     */
    private var bucketsFor: Pair<Boolean, Int>? = null

    private var bucketsJob: Job? = null

    /**
     * Il colore scelto per ogni cartella che ne ha uno, per la schermata iniziale.
     *
     * ⚠️⚠️ **SI LEGGE COL SUO ELENCO E SI AGGIORNA A MANO, dalla `1.87`**: la lettura vive in
     * [readBuckets], che però ha una guardia e non riparte al ritorno da una cartella; quindi un
     * colore appena scelto arriverebbe alla casa solo dopo un riavvio. A chiudere il buco è
     * [tintFolder], che scrive anche qui, com'è già per [tint].
     * ⚠️ **Vuota finché [FolderColour] è [FolderColour.NONE]** non sarebbe un risparmio da
     * cercare: la lettura è una riga sola dell'archivio, e legarla all'impostazione vorrebbe
     * dire ricaricare l'elenco quando la si cambia.
     */
    var folderTints: Map<Long, Int> by mutableStateOf(emptyMap())
        private set

    /**
     * La copertina scelta a mano per ogni cartella che ne ha una, per la schermata iniziale.
     *
     * ⚠️ **Vale parola per parola la nota di [folderTints]**, letta con la sua stessa guardia e
     * aggiornata a mano da [coverFolder]: senza quella scrittura, una copertina appena scelta
     * arriverebbe alla casa solo dopo un riavvio.
     */
    var folderCovers: Map<Long, Uri> by mutableStateOf(emptyMap())
        private set

    /**
     * Rilegge le cartelle, ma solo se è cambiato qualcosa che le riguarda.
     *
     * ⚠️ **Le chiavi sono due e sono tutte quelle che contano**: il permesso, senza il quale
     * non c'è niente da leggere, e [outsideStamp], cioè la notizia del MediaStore che l'app
     * ascolta già. Le cartelle nascoste **non** sono fra le chiavi, perché non cambiano quello
     * che c'è sul disco.
     */
    fun readBuckets(granted: Boolean) {
        val key = granted to outsideStamp
        if (bucketsFor == key) return
        bucketsFor = key
        val context = getApplication<Application>()
        // ⚠️ La lettura precedente si annulla: due notizie ravvicinate darebbero due query in
        // volo, e l'ultima a rispondere non è detto sia l'ultima partita.
        bucketsJob?.cancel()
        bucketsJob = viewModelScope.launch {
            val letti = if (granted) Folder.buckets(context) else emptyList()
            buckets = letti
            /*
             * ⚠️⚠️ **QUI SI POTANO LE COPERTINE ORFANE, E QUESTO È L'UNICO MOMENTO IN CUI SI PUÒ**
             * (sua domanda, 2026-09-08: *forse dopotutto serve un sistema che una volta alla
             * settimana verifichi che non siano memorizzate copertine per cartelle che non
             * esistono più*): il `BUCKET_ID` viene dal percorso, quindi una rinomina fatta da
             * un'altra app lascia in casa un file appeso a una cartella che non esiste più. Serve
             * l'elenco **vero** dei bucket, ed è quello che si è appena letto.
             * ⚠️ **Non è un lavoro programmato**, che è la sua regola sul cestino (`d-cestino-chiusa`:
             * *nulla deve avvenire al di fuori dell'app aperta in primo piano*). Il periodo di
             * grazia, che è quello che rende sicura una potatura fatta a ogni lettura, vive su
             * [FolderCovers.sweep].
             * ⚠️ **In coda alla lettura e non in un lancio suo**: dipende dal suo risultato, e con
             * un elenco vuoto non fa niente.
             */
            FolderCovers.sweep(context, letti.map { it.id }.toSet())
        }
        // ⚠️ In un lancio a sé e non in coda all'altro, come per i due numeri di una cartella:
        // le tinte sono una riga di preferenze e arrivano subito, mentre l'elenco passa da una
        // query al MediaStore. In coda, una cartella colorata si vedrebbe grigia per il tempo
        // di quella query.
        viewModelScope.launch { folderTints = FolderTints.all(context) }
        // ⚠️ In un lancio suo, per la stessa ragione delle tinte: qui si legge una cartella sul
        // disco, che è un'altra attesa da quella della query al MediaStore.
        viewModelScope.launch { folderCovers = FolderCovers.all(context) }
    }

    /**
     * Dove sta la navigazione della vista 'Cartelle di sistema', e `null` vuol dire in cima.
     *
     * ⚠️⚠️ **VIVE QUI E NON NELLA SCHERMATA, dalla `0.84`**: si esce dalla casa ogni volta che
     * si apre una fotografia, e al ritorno la navigazione deve ritrovarsi dov'era. Un ricordo
     * dentro il composabile si azzererebbe a ogni andata e ritorno, riportando in cima dopo ogni
     * foto guardata, che è il difetto che rende inutilizzabile un gestore di file.
     * ⚠️ **Non si salva nelle impostazioni**, ed è voluto: è lo stato di una sessione, non una
     * preferenza. Riaprendo l'app si riparte dalla radice, che è dove si sa di essere.
     */
    var treePath: String? by mutableStateOf(null)
        private set

    /**
     * Se dalla cartella corrente si può risalire, cioè se il gesto Indietro ha un posto dove
     * portare.
     *
     * ⚠️⚠️ **SI CALCOLA QUANDO SI NAVIGA e non a ogni lettura**: dirlo richiede di risolvere i
     * collegamenti dei percorsi (`canonicalPath`), che è I/O sul disco, e una proprietà
     * calcolata lo farebbe a ogni ricomposizione, cioè sul filo principale mentre si scorre.
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
     * Le memorie, lette una volta sola.
     *
     * ⚠️ Pigra e non nel costruttore: l'elenco delle memorie si chiede al sistema, e chi non
     * apre mai quella vista non deve pagarlo all'avvio dell'app.
     */
    private val treeRoots: List<Tree.Root> by lazy { Tree.roots(getApplication()) }

    /**
     * Una fotografia toccata nella vista delle cartelle di sistema.
     *
     * ⚠️⚠️ **LA SERIE ARRIVA GIÀ FATTA da chi ha disegnato l'elenco, e non si rilegge**: così
     * l'ordine che si sfoglia è **esattamente** quello che si aveva davanti agli occhi, per
     * costruzione. Rileggere la cartella qui avrebbe voluto dire due letture e la possibilità
     * che la seconda dia un elenco diverso dalla prima.
     * ⚠️ **Non tocca `viewerBack`**: la destinazione del ritorno resta la casa, e la casa si
     * ritrova nella sua terza vista, alla cartella giusta, perché [treePath] vive nel modello.
     * ⚠️ **Nessun anello dell'ultima vista** (`gridVisited`): quello è il segno della griglia
     * di una cartella, e qui la griglia non c'è.
     */
    fun openFromTree(items: List<Uri>, index: Int) {
        val whole = Folder.Series(items, index)
        if (whole.at(index) == null) return
        viewerBack = HOME
        screen = Screen.Viewer
        showAt(whole, index, Arrival.TAPPED)
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
        showAt(current, index, Arrival.TAPPED)
    }

    /**
     * Fuori dalla griglia: si torna all'elenco delle cartelle, da dove ci si è arrivati.
     *
     * ⚠️ **Passa da [HOME] come gli altri sette ritorni**, e fino alla `1.80` era il solo a
     * riscrivere quel valore a mano: il KDoc della costante è scritto apposta per questo caso, e
     * questo gli sfuggiva. Il difetto era **latente**, cioè oggi non si vedeva niente, che è
     * esattamente la forma che quella costante esiste per evitare.
     */
    fun leaveGrid() {
        screen = HOME
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
            is Screen.Grid, is Screen.Search, Screen.Bin -> {
                screen = dest
                source = null
                stopLoad()
                // ⚠️ **Dopo** l'assegnazione di `screen`, e non prima: [reloadGrid] guarda
                // lì per sapere quale delle tre griglie rileggere, e chiamata un attimo
                // prima leggerebbe ancora `Screen.Viewer`, cioè il suo ramo che non fa
                // niente. Vedi [afterFileAdded].
                if (gridStale) {
                    gridStale = false
                    reloadGrid()
                }
            }
            else -> goHome()
        }
    }

    /**
     * Mentre si guardava una fotografia è comparso un file nuovo nella cartella.
     *
     * ⚠️⚠️ **NON SI RILEGGE SUBITO, e non è pigrizia**: la griglia in questo momento non è in
     * scena, e rileggerla vorrebbe dire una query per una schermata che nessuno sta
     * guardando, mentre si sta guardando altro. Si segna soltanto che è vecchia, e la
     * rilettura si paga al ritorno, che è quando serve.
     * ⚠️⚠️ **E soprattutto NON si passa da [afterFileChanged], che sposterebbe la fotografia
     * sotto gli occhi**: quella conserva la **posizione** nella cartella, e un file nuovo
     * entra in cima all'ordine per data, quindi la stessa posizione dopo l'aggiunta è la foto
     * di prima. Chi esporta un fotogramma si ritroverebbe davanti un'altra immagine, senza
     * aver toccato niente.
     * ⚠️ Chi la chiama: l'esportazione di un fotogramma (`AnimatedBar`) e 'Converti/Esporta'
     * (`ConvertDialog`). Difetto riscontrato dall'utente il 2026-09-01: il fotogramma salvato
     * non compariva finché non si usciva dalla cartella e ci si rientrava.
     */
    fun afterFileAdded() {
        gridStale = true
    }

    /** Se la griglia dietro al visualizzatore è da rileggere. Vedi [afterFileAdded]. */
    private var gridStale = false

    /*
     * ══════════════ Quando i file cambiano per mano di qualcun altro ══════════════
     *
     * ⚠️⚠️ **NASCE DA UN RISCONTRO PRECISO** (utente, 2026-09-01: *i file che arrivano in
     * Download da NearDrop o da Blip hanno ancora bisogno di uscire e rientrare nella
     * cartella. Serve un tasto Aggiorna?*). La risposta è **no, non serve**, e vale dire
     * perché: un tasto Aggiorna chiede a chi guarda di sapere che l'elenco è vecchio, cioè
     * proprio la cosa che non può sapere. Il MediaStore invece **avvisa** quando qualcuno
     * scrive, e ascoltarlo costa un osservatore.
     */

    /**
     * Quante volte il disco è cambiato da fuori, da quando l'app è aperta.
     *
     * ⚠️ **Un contatore e non un booleano**: le schermate che leggono da sé (la casa e
     * l'albero delle cartelle) lo usano come chiave di un `LaunchedEffect`, e una chiave
     * deve **cambiare** a ogni notizia. Un `true` che resta `true` non farebbe ripartire
     * niente dalla seconda volta in poi.
     */
    var outsideStamp: Int by mutableStateOf(0)
        private set

    /**
     * Se la griglia sta facendo qualcosa che una rilettura rovinerebbe: oggi, una selezione
     * viva.
     *
     * ⚠️⚠️ **SENZA QUESTO L'AGGIORNAMENTO AUTOMATICO SAREBBE UN DANNO**: la selezione della
     * griglia è `remember(items)`, quindi una lista nuova la azzera. Chi ha spuntato trenta
     * foto e riceve un file da fuori se le vedrebbe sparire tutte, senza aver toccato
     * niente, e non capirebbe mai perché. Meglio un elenco vecchio di qualche secondo.
     * ⚠️ Il debito non si perde: appena la selezione si scioglie, la rilettura rimandata si
     * paga qui sotto.
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
     * L'orecchio sul MediaStore, acceso finché il modello vive.
     *
     * ⚠️ **`notifyForDescendants` acceso**: l'indirizzo registrato è quello della collezione,
     * e le notizie arrivano su quello della singola riga. Senza, non si sentirebbe niente.
     * ⚠️ **`Files` e non `Images`**: l'app sfoglia anche i video, e una copia in arrivo può
     * essere l'uno o l'altro.
     * ⚠️⚠️ **SI REGISTRA QUI E NON NEL BLOCCO `init`, e non è una preferenza di stile**: le
     * proprietà si inizializzano nell'ordine in cui sono scritte, e `init` sta più in su,
     * quindi da là questo osservatore non esiste ancora ('Variable must be initialized', che
     * è un errore di compilazione e non un difetto in agguato). Registrandosi da sé, il
     * momento giusto è garantito da dove sta scritto.
     * ⚠️ **Acceso per tutta la vita del modello e non a schermata**: il modello sopravvive
     * alla rotazione e ai passaggi fra le schermate, mentre un osservatore acceso e spento a
     * ogni passaggio perderebbe proprio le notizie che arrivano mentre si cambia pagina. Chi
     * le riceve decide da sé se gli servono.
     */
    private val outsideWatch = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) = outsideChange()
    }.also {
        getApplication<Application>().contentResolver
            .registerContentObserver(Folder.TABLE, true, it)
    }

    private var outsideJob: Job? = null

    /**
     * Qualcuno ha scritto sul disco: si rilegge quello che è in scena.
     *
     * ⚠️⚠️ **CON UNA PAUSA, e non a ogni notizia**: una copia di venti file arriva come venti
     * notizie in due secondi, e senza la pausa sarebbero venti query di fila mentre si
     * guarda. Il timer riparte a ogni notizia, quindi si legge una volta sola, quando il
     * traffico si ferma.
     * ⚠️ **Anche le nostre scritture passano di qui**, e va bene così: la rilettura che segue
     * una conversione o un'esportazione è già chiesta da chi la fa, e questa arriva dopo,
     * trova le stesse righe, e non si vede.
     */
    private fun outsideChange() {
        outsideJob?.cancel()
        outsideJob = viewModelScope.launch {
            delay(OUTSIDE_PAUSE_MS)
            outsideStamp++
            when {
                // ⚠️ Il cestino NON si rilegge da qui: le sue righe stanno nella cartella
                // privata dell'app, dove il MediaStore non guarda, quindi una notizia che lo
                // riguarda non arriva mai e una che arriva non parla di lui.
                screen == Screen.Bin -> Unit
                screen.isGrid() -> if (gridBusy) gridStale = true else reloadGrid()
                // ⚠️ Mentre si guarda una fotografia si segna e basta, per la stessa ragione
                // di [afterFileAdded]: la griglia non è in scena, e la si paga al ritorno.
                screen == Screen.Viewer || screen is Screen.Editor -> gridStale = true
                // ⚠️ La casa non guarda le righe ma le CARTELLE, e un file arrivato da fuori
                // può averne fatta nascere una: qui la rilettura serve. Si rifà col permesso
                // dell'ultima volta, perché a saperlo è la schermata e in questo istante non
                // le si può chiedere niente; se non ha ancora letto, non c'è niente da
                // rifare e ci penserà lei entrando.
                screen is Screen.Folders -> bucketsFor?.let { readBuckets(it.first) }
                else -> Unit
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().contentResolver.unregisterContentObserver(outsideWatch)
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
                is Screen.Search ->
                    Folder.byName(context, query, settings?.hiddenFolders.orEmpty(), from.bucket)
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
            showAt(reading, place.coerceIn(0, reading.items.lastIndex), Arrival.LEAFED)
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
    /*
     * ⚠️⚠️ **È IL PEZZO 3 DEI VIDEO** (`0.86`, impostazione 'Sfoglia solo le immagini',
     * spenta di fabbrica): l'alternativa era una seconda serie, filtrata, da tenere
     * d'accordo con la prima; ma una serie filtrata avrebbe cambiato anche il **contatore**
     * e la griglia, che dei video li mostrano. Così cambia solo di quanto avanza il dito,
     * che è esattamente ciò che l'impostazione dice.
     * ⚠️⚠️ **IL TOCCO SULLA GRIGLIA NON PASSA DI QUI** ([openFromGrid] chiama [showAt] con una
     * posizione), e questa impostazione spegne il **gesto** e non il tocco. ⚠️ **Fino alla
     * `1.45` qui c'era scritto che toccando un video dalla griglia parte la riproduzione, e
     * non era vero**: in tutta l'app non esisteva nessun `play`, quindi quella riga dava per
     * fatta una cosa mai scritta. Adesso la risposta esiste e sta in un altro posto: chi tocca
     * arriva con [Arrival.TAPPED], e se il filmato parta lo decide `Settings.clipAutoplay`,
     * spenta di fabbrica.
     * ⚠️⚠️ **IL CONTO STA SULLA SERIE, dalla 1.06** (`Folder.Series.stepping`), e non più
     * qui dentro: là lo vede anche la schermata, che delle vicine da disegnare ha bisogno
     * dello stesso identico salto. Il perché e il difetto che l'ha fatto spostare stanno
     * scritti là.
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
     * Mostra la foto che sta in quella posizione della serie di lettura.
     *
     * ⚠️ Condivisa fra la strisciata ([step]) e il tocco sulla griglia
     * ([openFromGrid]), che sono la stessa cosa con due modi di dire quale foto: un
     * passo relativo e una posizione. Due copie divergerebbero sul punto peggiore, cioè
     * su come si riscrive [listed].
     * ⚠️ Fuori dalla serie non fa **niente**, e da qui viene il fatto che la sequenza
     * non gira su sé stessa: all'ultima foto la strisciata in avanti resta senza esito.
     */
    private fun showAt(current: Folder.Series, index: Int, from: Arrival) {
        val uri = current.at(index) ?: return
        source = uri
        // ⚠️ La vicina di un indirizzo riscritto è già in sicuro: il motivo dell'errore,
        // se arriva, non è più il traffico in chiaro, e dirlo sarebbe una bugia.
        upgraded = false
        // ⚠️ `current` sta nell'ordine di lettura, `listed` in quello grezzo: si
        // rigira per riscriverlo, e la stessa funzione basta perché girare una serie
        // è la sua stessa inversa.
        listed = Folder.Lookup.Found(current.copy(index = index)).oriented()
        startLoad(uri, from)
        /*
         * ⚠️⚠️ **LA FINESTRA DEL WEB SI RIFÀ A OGNI PASSO, e quella di una cartella mai.**
         * Una serie remota è larga tre e non ha fine nota: appena il dito la attraversa, la
         * vicina di là va ancora indovinata e verificata. La riga sopra intanto ha già
         * spostato l'indice, quindi la strisciata risponde subito e la rete arriva dopo.
         * ⚠️ **Tranne quando la serie è già INTERA** (`WebSeries.WHOLE`: l'indice della
         * cartella e le immagini di una pagina), che è ordinata come quella di una cartella
         * vera: rifarla vorrebbe dire riscaricare la stessa pagina a ogni fotografia.
         */
        if (WebSeries.isWeb(uri) && webRule !in WebSeries.WHOLE) {
            viewModelScope.launch { listed = webWindow(uri) }
        }
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
     * Rimette in piedi tutto quello che si mostra **una volta sola**, come alla prima
     * installazione.
     *
     * ⚠️ **Sta nel modello e non nella schermata**, come `forgetRecents`: quella scrive
     * nell'archivio, e una schermata che scrive nell'archivio da sé smette di essere una
     * schermata. ⚠️ Gira su **tutte** le voci di [Hint] e non su un elenco scritto a mano:
     * il giorno che ne nasce una quarta, questa funzione la copre già.
     *
     * ⚠️⚠️ **DALLA 1.16 AZZERA ANCHE [FolderAsk], e non è un di più**: il testo del tasto
     * promette 'i suggerimenti **e gli avvisi**', e la richiesta della cartella iniziale è
     * l'unica cosa una-tantum che restava fuori. Era anche la più costosa da subire: gli
     * onboarding tornano da soli cancellando i dati dell'app, mentre un 'no' alla cartella
     * chiudeva la porta per sempre, ed è proprio il caso in cui serve tornare indietro
     * (richiesta dell'utente, 2026-09-01).
     * ⚠️ **Quello che NON copre, perché non esiste**: un avviso alla prima eliminazione (la
     * conferma esce **ogni** volta, ed è una conferma, non un onboarding) e uno al primo
     * salvataggio dell'editor (c'era fino alla `1.07`, ed è uscito con la domanda
     * 'sovrascrivi o copia'). Sta scritto qui perché sono le due cose che si va a cercare.
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
     * ⚠️⚠️ **Un indirizzo in chiaro non fallisce come gli altri**: Android lo blocca prima
     * che parta, l'app lo ha già riscritto in `https`, e se anche quello non risponde la
     * persona merita di sapere che cosa è successo. Con il solo 'non si è potuta aprire'
     * cercherebbe il difetto nella propria rete, che è l'unico posto in cui non c'è.
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
         * ⚠️⚠️ **LA GUARDIA È IL MODELLO E NON `savedInstanceState`, DALLA `1.81`: COSÌ UN
         * COLLEGAMENTO APERTO DA FUORI NON SI PERDE PIÙ QUANDO IL SISTEMA UCCIDE IL PROCESSO**
         * (censimento della UI del 2026-09-05). La domanda giusta non è 'questa è la prima
         * creazione?' ma 'l'intento di partenza è già stato letto?', e le due divergono in un
         * caso: con il processo ucciso in fondo e ricreato, il `Bundle` c'è (quindi la vecchia
         * guardia saltava la lettura) ma il modello è **nuovo**, quindi la schermata nasceva a
         * [HOME]. Cioè si tornava dalla galleria di un'altra app e al posto dell'immagine si
         * trovava la casa.
         * ⚠️ **Alla rotazione non cambia niente**, che è la ragione per cui la guardia esiste: là
         * il modello sopravvive e ha già l'immagine in mano, quindi rileggere l'intento la
         * caricherebbe una seconda volta.
         */
        if (!model.intentRead) model.handleIntent(intent)
        // ⚠️ Il tema si legge QUI, fuori da `AivApp`, perché deve avvolgerlo: dentro,
        // avrebbe già ereditato la tavolozza sbagliata. Finché le impostazioni non sono
        // arrivate vale il sistema, che è anche il valore di fabbrica della scelta.
        setContent {
            val chosen = model.settings?.uiTheme ?: UiTheme.SYSTEM
            val scuro = chosen.isDark()
            /*
             * ⚠️⚠️ **LE BARRE DI SISTEMA SEGUONO IL TEMA DELL'APP, e fino alla `1.80` seguivano
             * quello del SISTEMA** (censimento della UI del 2026-09-05, l'unico rilievo di
             * gravità alta). `enableEdgeToEdge()` senza argomenti costruisce due
             * `SystemBarStyle.auto`, il cui riconoscimento del buio legge la configurazione
             * delle risorse, e da quel valore scrive l'aspetto chiaro o scuro delle **icone**
             * di stato e di navigazione. Il tema dell'app invece si risolve qui, nella
             * composizione, da `model.settings?.uiTheme`: le due fonti divergono ogni volta che
             * si sceglie 'Chiaro' o 'Scuro' contro il sistema, e nel caso peggiore le icone
             * finiscono chiare su fondo chiaro, cioè illeggibili.
             * ⚠️ **Si rifà quando il tema cambia**, e non solo in `onCreate`: la scelta si può
             * cambiare dalle impostazioni senza ricreare l'attività, e una chiamata sola
             * lascerebbe le icone del tema di prima. La chiave è il tema risolto, quindi
             * seguire il sistema mentre il sistema cambia funziona da sé.
             * ⚠️ **`SystemBarStyle.dark` e non `auto`**: `auto` fa la stessa lettura sbagliata.
             * Il colore di fondo resta trasparente in tutti e due i casi, che è quello che
             * l'app dipinge da sé.
             */
            LaunchedEffect(scuro) {
                val stile = if (scuro) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = stile, navigationBarStyle = stile)
            }
            AivTheme(darkTheme = scuro) {
                // ⚠️ Anche la scelta di che cosa c'è dietro un pannello si mette in scena QUI,
                // accanto al tema e per la stessa ragione: la chiedono finestre che le
                // impostazioni non le ricevono. Il perché per esteso sta su [LocalAivDepth].
                // ⚠️ E accanto a lui i riquadri: le parole sotto le icone e i quattro
                // ordini dei tasti. Stessa ragione del velo, scritta su [LocalPadLook]: li
                // chiedono finestre che le impostazioni non ricevono. Finché le impostazioni
                // non sono arrivate vale quello che l'app ha sempre fatto.
                val look = model.settings?.let {
                    PadLook(it.padLabels, it.menuOrder, it.pickOrder, it.turnOrder, it.stepOrder, it.hand)
                } ?: PadLook()
                // ⚠️ E la vista delle cartelle, che serve alla finestra della destinazione:
                // dalla `1.81` quella entra dalla vista normale dell'app, e il perché per
                // esteso sta su [LocalDestLook].
                val dove = model.settings?.let {
                    DestLook(it.folderView, it.folderColumns, it.folderCount, it.listCount, it.listText)
                } ?: DestLook()
                // ⚠️ E accanto a loro il minuto di 'Mostra nascoste', dalla `2.03`: la finestra
                // delle destinazioni deve sapere se è acceso, e il perché per esteso, con la
                // ragione per cui questo non è uno static local, vive su [LocalPeek].
                CompositionLocalProvider(
                    LocalAivDepth provides (model.settings?.panelDepth ?: PanelDepth.NONE),
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

    /**
     * Consegna l'immagine scelta a chi ha aperto AIV come **selettore**, e chiude.
     *
     * ⚠️⚠️ **NASCE NELLA `1.89`** (*vorrei che AIV comparisse anche quando scelgo 'Altre app' su
     * WhatsApp*): il selettore di sistema elenca, oltre agli archivi, le app che rispondono a
     * `ACTION_GET_CONTENT`, e per esserci bisogna sia dichiararlo nel manifesto sia saper
     * **restituire** un file.
     * ⚠️⚠️ **L'INDIRIZZO SI PREPARA E NON SI PASSA COM'È, o l'app può cadere**: un `file://` che
     * esce dal processo fa scattare `FileUriExposedException` da Android 7, e le cartelle lette
     * dal disco portano proprio quello. [ImageActions.readableOutside] è la stessa strada della
     * condivisione, quindi un `content://` passa senza copiare niente e un `file://` diventa una
     * copia servita dal FileProvider.
     * ⚠️ **Il permesso viaggia con l'intento**: senza `FLAG_GRANT_READ_URI_PERMISSION` chi riceve
     * si vede un indirizzo che non può aprire, che è il modo peggiore di fallire.
     * ⚠️ **Se la preparazione non riesce non si consegna e non si chiude**: meglio restare
     * nell'app con un avviso che tornare a chi ha chiesto con le mani vuote e senza spiegazione.
     * ⚠️ **Un'immagine per volta**: `EXTRA_ALLOW_MULTIPLE` non è gestito, e un chiamante che lo
     * chiede riceve comunque un file solo, che è una risposta legittima.
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
    /*
     * ⚠️⚠️ **QUI L'IMPOSTAZIONE ESCE DALL'INTERFACCIA E ARRIVA A COIL**: un `Fetcher` non vede
     * le preferenze, quindi qualcuno deve portargliele, e questo è l'unico posto dell'app in
     * cui le preferenze ci sono e il caricatore è già dichiarato. Il perché di una variabile
     * globale invece di un parametro sta su [Thumbs.inGpu].
     * ⚠️ **`SideEffect` e non una riga nuda**: scrivere fuori dalla composizione durante la
     * composizione è esattamente ciò per cui quel blocco esiste, e senza di lui la scrittura
     * si rifarebbe a ogni ricomposizione della radice, cioè molte volte per niente.
     */
    SideEffect { Thumbs.inGpu = settings.gpuThumbs }
    /*
     * ⚠️⚠️ **LO SVUOTAMENTO AUTOMATICO DEL CESTINO GIRA QUI, E SOLO QUI, PERCHÉ È QUELLO CHE
     * HA CHIESTO LUI** (giro della 1.67, `d-cestino-chiusa`: **`aperta`**, ribadito il
     * 2026-09-06: *nulla deve avvenire al di fuori dell'app aperta in primo piano*). Quindi
     * niente operazione programmata di sistema e nessuna libreria in più: la pulizia è un
     * effetto dell'app che è in scena.
     * ⚠️ **La chiave è la durata scelta**, quindi passa una volta all'avvio e un'altra ogni
     * volta che lui la cambia, che è esattamente quando serve: cambiarla da 'Mai' a 'Un mese'
     * deve avere un effetto visibile subito, o sembra che non abbia fatto niente.
     * ⚠️ **Con 'Mai' non tocca il disco**: [Bin.sweep] esce prima di leggere l'archivio, quindi
     * questo effetto costa niente nel caso di fabbrica.
     * ⚠️ **Il conto dei file tolti non si mostra**: una pulizia che si annuncia da sola
     * sarebbe un avviso a ogni avvio per un lavoro che l'utente ha già deciso una volta, e i
     * file spariti sono esattamente quelli che aveva accettato di perdere.
     */
    val perPulizia = LocalContext.current
    LaunchedEffect(settings.binKeep) { Bin.sweep(perPulizia, settings.binKeep.days) }
    /*
     * ⚠️⚠️ **IL SELETTORE E L'AVVISO STANNO PRIMA DEL `when`, e non dentro un ramo**: la
     * scelta dell'editor si chiede da due schermate diverse (il menu del tocco lungo e le
     * impostazioni), e l'esito di un salvataggio arriva quando l'editor si è **già** chiuso.
     * Scritti dentro un ramo si perderebbero al cambio di schermata, cioè nell'istante esatto in
     * cui devono comparire.
     */
    if (model.editorAsk) {
        EditorPicker(
            chosen = settings.editorApp,
            onPick = { model.editorChosen(it) },
            onDismiss = { model.editorSkip() }
        )
    }
    val context = LocalContext.current
    val said = model.notice
    if (said != null) {
        // ⚠️ L'avviso si azzera subito dopo averlo mostrato: senza, la chiave resterebbe la
        // stessa e la frase tornerebbe identica al primo ridisegno, ma soprattutto non si
        // potrebbe più mostrare **due volte** la stessa (due copie salvate di fila).
        // ⚠️⚠️ **DALLA `1.84` PASSA DAL CANALE E NON DALL'AVVISO DI SISTEMA** (sua risposta
        // `casa` a `d-avvisi`): la frase è la stessa, cambia la superficie. Il testo si risolve
        // qui, dove la composizione c'è, perché [Notices] prende parole e non risorse.
        val frase = stringResource(said)
        LaunchedEffect(said) {
            Notices.say(frase, NOTICE_LONG_MS)
            model.noticeShown()
        }
    }
    /*
     * ⚠️⚠️ **L'OFFERTA DI DISFARE UN'ELIMINAZIONE STA QUI, SOPRA LA TRANSIZIONE FRA SCHERMATE**
     * (richiesta dell'utente, giro della `1.59`: *resta disponibile anche se si cambia
     * cartella*). È la stessa ragione del selettore e dell'avviso qui sopra, scritta un'altra
     * volta perché qui la clausola è **esplicita**: dentro un ramo del `when` la notifica se ne
     * andrebbe proprio nell'istante in cui deve restare. Che cosa la tiene in vita, e perché lo
     * stato non è un parametro, sta in testa a [Undo].
     * ⚠️ **Il conto alla rovescia riparte a ogni offerta nuova**: la chiave dell'effetto è
     * l'elenco, e due eliminazioni di fila non ne producono mai uno uguale, perché i nomi nel
     * cestino sono unici (`FileTree.freeName`).
     * ⚠️ **Il tasto si prende l'elenco PRIMA di azzerarlo**: fra il tocco e il lavoro passa un
     * fotogramma, e leggere l'offerta dentro l'ambito la troverebbe già vuota.
     */
    val disfare = Undo.offerta
    val scope = rememberCoroutineScope()
    /*
     * ⚠️⚠️ **DALLA `1.84` L'OFFERTA DIVENTA UNA RIGA DEL CANALE, e l'attesa non è più qui**: la
     * vita della notifica la governa chi la disegna, quindi il tempo è scritto in un posto solo.
     * Quello che resta a questo effetto è comporre la frase e dire che cosa fa il tasto.
     * ⚠️ **`onGone` chiude l'offerta**, per scadenza o perché il tasto è stato toccato: con due
     * attese parallele i due istanti sarebbero due sorgenti della stessa verità, e il giorno che
     * una delle due cambia resterebbe in scena un 'Annulla' che non fa più niente.
     * ⚠️ **Il tasto si prende l'offerta PRIMA che sparisca**: fra il tocco e il lavoro passa un
     * fotogramma, e rileggere `Undo.offerta` dentro l'ambito la troverebbe già vuota.
     * ⚠️ **Il valore di riserva della frase non si vede mai**: l'effetto gira soltanto quando
     * l'offerta c'è, e a quel punto il plurale è già stato risolto.
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
                    // ⚠️ Il ripristino dal cestino lo fa `Bin`, che sa da dove veniva ogni file:
                    // le altre due si disfano sui percorsi, e non passano di là.
                    is Undo.Offer.Trashed -> Bin.restore(context, quale.landed)
                    is Undo.Offer.Files -> FileTree.revert(context, quale.steps)
                }
                model.reloadGrid()
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
    /*
     * ⚠️⚠️ **OGNI CAMBIO DI SCHERMATA È UNA DISSOLVENZA, dalla `1.56`** (riscontro dell'utente,
     * giro della `1.55`: *la transizione da FAB -> menu -> Impostazioni è molto brusca, quasi
     * glitchata, soprattutto con la sfocatura attiva*). Fino a qui le schermate si
     * **sostituivano** in un fotogramma: non c'era nessuna transizione da nessuna parte, e
     * quello che cambiava era solo quanto le due schermate si somigliassero. Fra il cestino e la
     * cronologia, che hanno la stessa testata, il taglio non si notava; fra la schermata
     * iniziale, che ha il FAB e la fascia sfumata in fondo, e le impostazioni, che non hanno
     * nessuno dei due, si notava tutto.
     * ⚠️⚠️ **E LA DISSOLVENZA FA UNA SECONDA COSA, che è metà della correzione: tiene in vita
     * la schermata che se ne va, quindi il MENU FA IN TEMPO A CHIUDERSI.** Una voce di menu
     * chiama `menu.close()` e subito dopo la navigazione; finora la schermata spariva nello
     * stesso fotogramma, si portava via il `Popup` e l'uscita del menu non veniva disegnata
     * affatto. Con la sfocatura accesa spariva anche quella di colpo, ed è la ragione per cui
     * lui la vede peggio proprio là.
     * ⚠️ **Il visualizzatore resta fuori, e si dichiara**: è il passaggio più frequente
     * dell'app, quello che si fa a ogni immagine aperta, e una dissolvenza mostrerebbe per due
     * decimi di secondo la griglia e l'immagine insieme.
     * ⚠️⚠️ **LA RAGIONE SCRITTA QUI FINO ALLA `1.78` ERA FALSA, e la correzione dice il metodo
     * invece del solo esito**: diceva che entrando o uscendo *le barre di sistema cambiano e lo
     * schermo passa a immersivo*, e in questa app non c'è niente che nasconda le barre. Misurato:
     * nessuna delle API che rendono immersiva una schermata compare in `app/src`, e la barra
     * delle info del visualizzatore si scansa dalle barre di sistema con `safeDrawingPadding()`,
     * cioè da barre che esistono per tutto il tempo. Una frase falsa in un commento ferma chi
     * verifica, ed è un costo già pagato due volte in questo progetto.
     */
        /*
         * ⚠️⚠️ **LO SCORRIMENTO DI UNA SCHERMATA SOPRAVVIVE ALLA SCHERMATA, DALLA `1.91`, ED È
         * UNA SUA RICHIESTA** (campo libero del giro della `1.89`, punto A: *se scorro la
         * schermata home, entro in una cartella e poi torno alla home, voglio che sia nello
         * stesso punto dello scorrimento in cui si trovava al mio tocco sulla cartella*). Fino
         * alla `1.90` la posizione viveva **dentro** la schermata, e una schermata che cambia
         * esce dalla composizione portandosela via: tornando, la griglia ripartiva da capo.
         * ⚠️⚠️ **NON SERVE UN ARCHIVIO SCRITTO A MANO: Compose ne ha uno fatto per questo.** Un
         * `SaveableStateHolder` tiene da parte tutto quello che una schermata ha in
         * `rememberSaveable` e glielo restituisce quando rientra, e `rememberLazyGridState` è
         * proprio un `rememberSaveable`. Una mappa di posizioni scritta da noi avrebbe coperto la
         * sola griglia, e ogni schermata nuova avrebbe dovuto ricordarsi di usarla.
         * ⚠️ **La chiave distingue le cartelle fra loro** (vedi [saveKey]): due cartelle sono due
         * posizioni, e con una chiave sola la seconda erediterebbe quella della prima.
         * ⚠️ **Cresce di una voce per schermata visitata**, e non si pota: quello che tiene sono
         * un indice e uno scarto, cioè due numeri, mentre un limite col suo sfratto sarebbe più
         * codice di quanto ne risparmi.
         */
        val stanze = rememberSaveableStateHolder()
        AnimatedContent(
            targetState = model.screen,
            transitionSpec = { cambioSchermata() },
            label = "schermata"
        ) { schermo ->
            /*
             * ⚠️⚠️ **QUESTA È LA SOLA COSA CHE SA DOVE SI STA ANDANDO, ED È PER IL FAB CHE ESCE**
             * (punto A del campo libero del giro accorpato). Il contenuto uscente vive ancora per
             * i 180 ms della dissolvenza, e in quel tratto `schermo` è la schermata di prima
             * mentre [ViewerViewModel.screen] è già quella nuova: il confronto fra i due dice
             * insieme *io sono quella che se ne va* e *dove si sta andando il FAB non c'è*.
             * ⚠️ **Si legge il modello QUI DENTRO apposta**: così il contenuto uscente si
             * ricompone quando il bersaglio cambia, che è l'istante in cui l'uscita deve partire.
             * ⚠️ **Non è il ritorno di `LocalArrivo`**, che la `1.75` ha tolto: quello diceva
             * quando una schermata aveva finito di **entrare**, e serviva a far aspettare
             * un'animazione; questo dice dove si va, e serve a farne partire una.
             */
            CompositionLocalProvider(LocalSenzaFab provides schermo.senzaFabVerso(model.screen)) {
                stanze.SaveableStateProvider(schermo.saveKey()) {
                    Stage(schermo, model, settings, onPicked)
                }
            }
        }
        /*
         * ⚠️⚠️ **QUESTA È L'UNICA SUPERFICIE CON CUI L'APP DICE COM'È ANDATA, DALLA `1.84`** (sua
         * risposta `casa` a `d-avvisi`): vive qui, sopra la transizione fra schermate, perché una
         * notizia deve poter sopravvivere alla schermata che l'ha prodotta. Dentro un ramo del
         * `when` se ne andrebbe proprio nell'istante in cui deve restare, che è la clausola
         * esplicita dell'offerta di disfare (*resta disponibile anche se si cambia cartella*).
         * ⚠️ **La frase la porta la riga**: qui non si sa che cosa dica, e non serve saperlo.
         * ⚠️⚠️ **E DALLA `2.11` SALE SOPRA LA SCHEDA DELLA SELEZIONE** (punto C del campo libero
         * del giro accorpato: *in alcune circostanze (es. si inizia una selezione dopo un 'copia',
         * 'sposta' o 'elimina'), la bottomsheet della selezione va a finire sotto la notifica in
         * basso*). Le due superfici sono appoggiate allo stesso bordo e nascono da due gesti che
         * si susseguono, quindi prima o poi si incontrano: il perché a muoversi sia questa, e non
         * la scheda, vive su [Modifier.aboveFoot].
         * ⚠️⚠️ **E DALLA `2.24` SALE ANCHE SOPRA IL FAB, PER LA STESSA RAGIONE** (punto A1 del campo
         * libero del giro della `2.23`: *la notifica inferiore con 'Annulla' (es. per 'Sposta') a
         * volte va sopra il FAB (su qualunque lato sia)*). Quel tasto vive in un angolo e questa
         * riga è larga quasi tutto lo schermo, quindi si incontrano **sempre**; e a disegnarla è
         * questa radice, cioè dopo la schermata, quindi gli finiva sopra. Il meccanismo non è
         * nuovo: è lo stesso della scheda, con un secondo chiedente in [FootStage].
         */
        AppNotice(
            Notices.line,
            modifier = Modifier.align(Alignment.BottomCenter).aboveFoot()
        )
        /*
         * ⚠️⚠️ **LA FASCIA DELLA COPERTINA VIVE QUI PER LA STESSA RAGIONE DELLA NOTIFICA**: la
         * scelta comincia in una cartella e può finire in un'altra, quindi l'unica cosa che dice
         * *stai scegliendo una copertina* deve stare sopra la transizione fra schermate. Dentro
         * una schermata se ne andrebbe al primo passo della navigazione, cioè subito.
         * ⚠️ **Non è una notifica**: una notifica dice che cosa è successo e se ne va da sé,
         * questa dice che cosa sta succedendo e resta finché la modalità è viva.
         */
        model.covering?.let { quale ->
            CoverInvite(
                name = quale.name,
                onCancel = { model.stopCover() },
                // ⚠️ **Sale anche lei**, per la stessa ragione della notifica: è appoggiata allo
                // stesso bordo, e una riga in meno di così vorrebbe dire scommettere che le due
                // cose non si incontrino mai.
                modifier = Modifier.align(Alignment.BottomCenter).aboveFoot()
            )
        }
    }
}

/**
 * Quanto dura la dissolvenza fra due schermate.
 *
 * ⚠️ **Più lunga dell'uscita di un menu**, che sono 75ms (`MENU_OUT_MS` in `Menus.kt`): la
 * schermata di partenza deve restare in vita finché il menu da cui si è usciti ha finito di
 * chiudersi, o si torna al taglio di prima.
 * ⚠️⚠️ **IL MARGINE NON È 'POCO', E FINO ALLA `1.78` LA NOTA NOMINAVA LA COSTANTE SBAGLIATA**:
 * citava `MENU_IN`, cioè l'**entrata** di un menu, mentre il conto che serve riguarda l'uscita.
 * Con l'uscita a 75 ms il margine è di 105 ms su 180, cioè largo: chi ritoccasse questa durata
 * fidandosi di 'poco' lavorerebbe su un numero che non è quello.
 */
private const val SCHERMO_MS = 180

/**
 * La dissolvenza con cui una schermata prende il posto di un'altra.
 *
 * ⚠️⚠️ **`null` E NON UNA `SizeTransform`, MA NON PER LA RAGIONE CHE QUI ERA SCRITTA FINO ALLA
 * `1.73`.** La `1.72` l'aveva tolta per correggere un difetto del FAB (riscontro dell'utente,
 * 2026-09-05: *se torno in home da una cartella arriva di nuovo da in basso a destra ...
 * l'ingrandimento dev'essere dal centro del FAB, quindi ingrandimento sì, movimento no*), e la
 * `1.74` ha **misurato** quella correzione sul banco di prova: con e senza `SizeTransform` il
 * centro del FAB sta nello stesso punto a **ogni fotogramma**, cifra per cifra, nella
 * schermata vera. Quindi quel rimedio era un **niente**, e la voce di collaudo che ne
 * annunciava l'effetto ha detto all'utente una cosa falsa.
 * - ⚠️⚠️ **PERCHÉ UNA `SizeTransform` QUI NON FA NIENTE**: anima la misura del **contenitore**,
 *   e le due schermate riempiono tutte e due la finestra, quindi quella misura non cambia e
 *   l'animazione parte e arriva sullo stesso numero. Il ragionamento della `1.72` (un angolo
 *   segue il bordo che si muove) è giusto in generale e non si applica qui, perché nessun bordo
 *   si muove.
 * - ⚠️ **`null` resta lo stesso**, ed è la forma che la `AnimatedContent` del visualizzatore usa
 *   già (`ViewerScreen.kt`): dice che il contenitore prende subito la misura della schermata che
 *   entra, invece di dichiarare un'animazione che non ha niente da animare. Con lei se ne va il
 *   `clip = false`, che riguardava soltanto il ritaglio durante quella misura.
 * - ⚠️⚠️ **E IL MOVIMENTO CHE LUI VEDEVA NASCEVA DALL'ENTRATA DEL FAB, che dalla `1.75` non
 *   esiste più**: la `1.74` gliela aveva fatta aspettare, lui ha guardato il risultato e ha
 *   deciso di toglierla (*se ne va. Preferisco semplificare*). Quindi non c'è più niente da
 *   cercare da questa parte, e questa nota resta perché la `SizeTransform` non torni.
 *
 * ⚠️⚠️ **STA IN UNA FUNZIONE SUA, DALLA `1.74`, PERCHÉ IL BANCO DI PROVA DEVE MISURARE QUESTA
 * E NON UNA COPIA**: scritta dentro la `AnimatedContent`, una prova poteva soltanto riscriverla
 * accanto, e da quel momento avrebbe continuato a passare qualunque cosa fosse successa qui.
 * Una prova che non guarda il codice vero è una prova che mente in verde.
 */
internal fun AnimatedContentTransitionScope<Screen>.cambioSchermata(): ContentTransform =
    if (initialState is Screen.Viewer || targetState is Screen.Viewer) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        fadeIn(tween(SCHERMO_MS)) togetherWith fadeOut(tween(SCHERMO_MS))
    } using null

/**
 * Quello che sta in scena adesso.
 *
 * ⚠️⚠️ **NESSUNA DELLE INTERCETTAZIONI DI 'INDIETRO' HA L'ANTEPRIMA DEL GESTO, E RESTA COSÌ
 * FINCHÉ NON LO CHIEDE LUI** (censimento della UI del 2026-09-05: il rilievo è confermato e non
 * è un difetto da chiudere in un lotto di igiene). Il fatto è esatto: `PredictiveBackHandler`
 * non compare mai, la libreria che lo offre è già in progetto, e con `targetSdk` 36 l'anteprima
 * di sistema è accesa di serie, quindi trascinando dal bordo si vede la schermata **di sotto**
 * invece della nostra che si scosta.
 * ⚠️⚠️ **PERCHÉ NON ENTRA QUI: NON È IGIENE, È UN'ANIMAZIONE NUOVA IN QUATTORDICI PUNTI.** Ogni
 * intercettazione dovrebbe raccontare il proprio scostamento mentre il dito trascina, cioè una
 * decisione di movimento per ciascuna, e il movimento in questa app lo detta lui giro per giro
 * (i dieci giri della sfocatura sono la misura di quanto conti). Farlo da sé vorrebbe dire
 * quattordici animazioni mai chieste, e il banco di prova non le vede nemmeno.
 * ⚠️ **Quello che si può dire misurato**: non manca nessuna dipendenza, e non serve nessun flag
 * nel manifesto (`android:enableOnBackInvokedCallback` da `targetSdk` 33 il sistema lo ignora).
 * Il lavoro è tutto nel decidere che cosa si vede mentre il dito tira.
 *
 * ⚠️⚠️ **STA IN UNA FUNZIONE SUA perché durante la dissolvenza ne vivono DUE**, una per
 * schermata, e il `when` deve leggere quella che gli passa la transizione invece di
 * `model.screen`, che è già quella nuova per tutti e due. Scritto dentro [AivApp] il ramo in
 * uscita disegnerebbe la schermata in arrivo, cioè la dissolvenza non si vedrebbe.
 * ⚠️ **I due rami leggono lo stesso modello**, e va bene: per due decimi di secondo la
 * schermata che se ne va si ricompone sui dati nuovi, che è quello che farebbe comunque se
 * restasse.
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
     * Consegna la miniatura toccata invece di aprirla, quando l'app è un selettore.
     *
     * ⚠️⚠️ **VALE PER TUTTE E TRE LE GRIGLIE, e non per la sola cartella**: la ricerca e il
     * cestino mostrano immagini come le altre, e chi arriva da un'altra app può volere proprio
     * quelle. Una modalità che funziona in una schermata su tre è una modalità che sembra rotta.
     * ⚠️ **Torna `false` quando non c'è niente da consegnare**, e allora chi chiama apre come
     * sempre: fuori dalla modalità scelta, e su un indice che l'elenco non ha più.
     */
    fun consegna(items: List<Uri>?, quale: Int): Boolean {
        if (!model.picking && model.covering == null) return false
        val scelto = items?.getOrNull(quale) ?: return false
        /*
         * ⚠️⚠️ **LA COPERTINA VIENE PRIMA, e i due casi non si sovrappongono mai**: la modalità
         * copertina si accende da dentro l'app, e quella del selettore da un'altra app che ha
         * chiesto un'immagine. Ma se un giorno si sovrapponessero, quella che l'utente ha
         * appena acceso col dito è la sua ultima intenzione.
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
            // ⚠️ Il gesto Indietro si intercetta SOLO nella veste 'scegli la cartella
            // d'avvio': nell'altra questa è la casa, e da casa Indietro chiude l'app.
            if (screen.forStart) BackHandler { model.leaveStartFolderChoice() }
            /*
             * ⚠️⚠️ **NELLA VISTA DELLE CARTELLE DI SISTEMA IL GESTO INDIETRO RISALE, e senza
             * questo l'app si chiuderebbe da tre cartelle di profondità** (`0.84`). È l'unico
             * posto in cui la casa ha uno stato di navigazione, e il gesto Indietro deve
             * significare quello che significa dappertutto: torna al passo di prima.
             * ⚠️ **Si arma solo quando c'è dove risalire**: in cima resta il comportamento di
             * sempre, cioè Indietro chiude l'app. Un gestore aperto che ingoia il gesto senza
             * fare niente è peggio di non averlo.
             */
            val climbing = settings.folderView == FolderView.TREE && model.treeClimbing
            BackHandler(enabled = climbing) { model.treeUp() }
            FolderScreen(
                view = settings.folderView,
                columns = settings.folderColumns,
                counted = settings.folderCount,
                colour = settings.folderColour,
                tints = model.folderTints,
                covers = model.folderCovers,
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
                // ⚠️ La vista temporanea delle nascoste vive nel modello e non qui: il perché
                // (ci si entra dentro, e la schermata nel frattempo se ne va) vive su `peeking`.
                peeking = model.peeking,
                onPeek = { model.peek(it) },
                // ⚠️ Rimostrare per sempre scrive la stessa preferenza della pagina 'Cartelle
                // nascoste' nelle impostazioni: una cosa, una chiave, come la coppia
                // casa/scorciatoia di ogni altra voce.
                onUnhide = { path ->
                    model.updateSettings(
                        settings.copy(hiddenFolders = settings.hiddenFolders - path)
                    )
                },
                recents = model.recents,
                onPick = { model.folderPicked(it, screen.forStart) },
                // ⚠️ Anche i recenti consegnano, in modalità scelta: sono immagini come quelle
                // della griglia, e chi apre un selettore spesso vuole proprio l'ultima aperta.
                // ⚠️ E in modalità copertina l'immagine toccata diventa la copertina, come in
                // ogni altra griglia: dai recenti si sceglie l'ultima aperta, che è spesso
                // proprio quella che si vuole mettere davanti a una cartella.
                onOpen = { quale ->
                    when {
                        model.covering != null -> model.coverPicked(quale)
                        model.picking -> onPicked(quale)
                        else -> model.open(quale)
                    }
                },
                onOpenPage = { model.openPage(it) },
                onView = { model.updateSettings(settings.copy(folderView = it)) },
                onForget = { model.forgetRecents() },
                onSettings = { model.openSettings() },
                onSearch = { model.openSearch() },
                onBin = { model.openBin() },
                // ⚠️ La scorciatoia scrive la STESSA impostazione della riga di pastiglie
                // nelle preferenze, e non una sua: era la richiesta (*che resta globale per
                // tutte le cartelle*), ed è la ragione per cui passa da `updateSettings`
                // come ogni altra voce.
                onColumns = { model.updateSettings(settings.copy(folderColumns = it)) },
                // ⚠️ Come le colonne: la scorciatoia scrive la stessa preferenza del pannello,
                // che è la prima clausola della coppia casa/scorciatoia.
                onColour = { model.updateSettings(settings.copy(folderColour = it)) },
                // Le opzioni delle altre due viste, dallo stesso popup: passano da
                // `updateSettings` come le colonne, per la stessa ragione.
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
                onTreeOpen = { items, at -> model.openFromTree(items, at) },
                forStart = screen.forStart,
                onBack = { model.leaveStartFolderChoice() },
                buckets = model.buckets,
                onRead = { model.readBuckets(it) }
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
                /*
                 * ⚠️⚠️ **I DUE RICHIAMI DEL TASTINO ARRIVANO SOLO QUI, DALLA `1.82`** (campo
                 * libero del giro della `1.81`, punto B: *il FAB deve vedersi in tutte le
                 * cartelle*): questa è la griglia di una cartella vera, cioè il posto da cui
                 * lui vuole raggiungere il cestino e le impostazioni senza tornare indietro.
                 * ⚠️ **La ricerca e il cestino non li ricevono**: la prima è un elenco di
                 * risultati e non una cartella, il secondo porta già il suo menu.
                 */
                onBin = { model.openBin() },
                onSettings = { model.openSettings() },
                /*
                 * ⚠️⚠️ **'Cerca' DENTRO LA CARTELLA, DALLA `1.83`** (risposta a `d-fab-voci` del
                 * giro della `1.82`: *cerca*, con la nota *in quel caso, 'Cerca' è limitato alla
                 * cartella corrente*). Il bucket e il nome viaggiano insieme perché la ricerca
                 * ristretta è un'altra schermata da quella globale: vedi [Screen.Search].
                 * ⚠️ **Arriva al solo ramo della cartella**, come i due richiami qui sopra: nella
                 * ricerca sarebbe una ricerca dentro una ricerca, e nel cestino non c'è nessun
                 * bucket da cui partire.
                 */
                onSearchHere = { model.openSearch(screen.bucket, screen.name) },
                /*
                 * ⚠️⚠️ **ANCHE QUESTI DUE ARRIVANO AL SOLO RAMO DELLA CARTELLA**: l'intestazione
                 * con la sua icona esiste qui e non nella ricerca né nel cestino, e una
                 * copertina è di una cartella. Il perché per esteso vive su
                 * [ViewerViewModel.covering].
                 */
                onCoverPick = { model.startCover() },
                onCoverClear = { model.clearCover() },
                coverSet = model.cover != null,
                // ⚠️ Serve al solo mini onboarding, e vuole **questa** cartella: scegliendo la
                // copertina di un'altra si naviga, e un velo che comparisse là indicherebbe
                // l'icona sbagliata.
                coverHere = model.covering?.bucket == screen.bucket,
                // ⚠️ Anche la rinomina è del solo ramo della cartella, e per la stessa ragione:
                // il gesto vive sul nome dell'intestazione, che qui c'è e altrove no.
                onFolderRename = { model.renameFolder(it) },
                /*
                 * ⚠️⚠️ **I QUATTRO CHIP E I DUE NUMERI ARRIVANO SOLO QUI, DALLA `1.83`**:
                 * l'intestazione esiste nella griglia di una **cartella** e non nelle altre due
                 * (nel cestino il FAB c'è sempre, nella ricerca la testata porta un campo di
                 * testo), quindi passarli anche là sarebbe dare valori a una fascia che non si
                 * disegna.
                 */
                frontWash = settings.frontWash,
                frontSerif = settings.frontSerif,
                frontFacts = settings.frontFacts,
                frontPickAll = settings.frontPickAll,
                /*
                 * ⚠️ **La tinta di questa cartella non viene dalle impostazioni**: è un dato
                 * della cartella, e il perché vive su [ViewerViewModel.tint].
                 */
                frontTint = model.tint,
                onFrontTint = { model.tintFolder(it) },
                facts = model.facts,
                onOpen = { quale ->
                    // ⚠️ In modalità scelta consegna e chiude, altrimenti apre come sempre.
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
                 * ⚠️⚠️ **VA PASSATO A TUTTI E TRE I RAMI, e nella `1.50` era arrivato al solo
                 * ramo della ricerca**, cioè al posto in cui il gesto non serve a niente:
                 * il tasto del filtro c'è in tutte e tre le schermate, e il tocco lungo lo
                 * si fa in una cartella. Il difetto era invisibile alla lettura, perché il
                 * parametro ha un valore di riserva vuoto (vedi `onSearch` in `GridScreen`):
                 * il gesto vibrava e chiamava una funzione che non fa niente, che è
                 * esattamente il riscontro (*c'è una vibrazione, ma non appare niente*).
                 * ⚠️ **E vale anche nel cestino, di proposito**: due controlli identici
                 * devono comportarsi allo stesso modo, che è la regola che lui ha dettato
                 * nello stesso giro parlando dei pannelli.
                 */
                onSearch = { model.openSearch() },
                onFilter = { model.sift(it) },
                gridNames = settings.gridNames,
                lastMark = settings.lastMark,
                onBusy = { model.gridBusy = it }
            )
        }

        is Screen.Search -> {
            BackHandler { model.leaveGrid() }
            // ⚠️ La stessa `GridScreen` della cartella, con due parametri in più: vedi la
            // nota su `query` là dentro per il perché non è una schermata a sé.
            val lookup = model.folder
            GridScreen(
                title = "",
                // ⚠️ Il nome della cartella in cui si cerca, e `null` per la ricerca di tutta la
                // galleria: lo scrive il campo al posto del suo invito, che è il modo di dire
                // dove si sta cercando senza una stringa nuova in ventotto lingue.
                searchIn = screen.name,
                items = lookup?.let { it.seriesOrNull?.items ?: emptyList() },
                highlight = if (model.gridVisited) model.series?.index else null,
                onOpen = { quale ->
                    // ⚠️ In modalità scelta consegna e chiude, altrimenti apre come sempre.
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
                onBusy = { model.gridBusy = it }
            )
        }

        Screen.Bin -> {
            BackHandler { model.leaveGrid() }
            // ⚠️ La stessa `GridScreen` di una cartella, con `bin` acceso: quello che cambia
            // sta là dentro (elimina definitiva, ripristina al posto di rinomina, e il
            // FAB anche senza selezione). Il cestino si naviga come una cartella
            // qualunque, che era la richiesta.
            val lookup = model.folder
            GridScreen(
                title = stringResource(R.string.bin_title),
                items = lookup?.let { it.seriesOrNull?.items ?: emptyList() },
                highlight = if (model.gridVisited) model.series?.index else null,
                onOpen = { quale ->
                    // ⚠️ In modalità scelta consegna e chiude, altrimenti apre come sempre.
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
                 * ⚠️⚠️ **VA PASSATO A TUTTI E TRE I RAMI, e nella `1.50` era arrivato al solo
                 * ramo della ricerca**, cioè al posto in cui il gesto non serve a niente:
                 * il tasto del filtro c'è in tutte e tre le schermate, e il tocco lungo lo
                 * si fa in una cartella. Il difetto era invisibile alla lettura, perché il
                 * parametro ha un valore di riserva vuoto (vedi `onSearch` in `GridScreen`):
                 * il gesto vibrava e chiamava una funzione che non fa niente, che è
                 * esattamente il riscontro (*c'è una vibrazione, ma non appare niente*).
                 * ⚠️ **E vale anche nel cestino, di proposito**: due controlli identici
                 * devono comportarsi allo stesso modo, che è la regola che lui ha dettato
                 * nello stesso giro parlando dei pannelli.
                 */
                onSearch = { model.openSearch() },
                onFilter = { model.sift(it) },
                gridNames = settings.gridNames,
                lastMark = settings.lastMark,
                bin = true,
                onHistory = { model.openHistory() },
                onBusy = { model.gridBusy = it }
            )
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
                onSave = { turns, mirror, crop -> model.editSave(turns, mirror, crop) },
                onBack = { model.leaveEditor() }
            )
        }

        /*
         * ⚠️ **Indietro va dove va l'altro editor**, cioè al visualizzatore: tutti e due si
         * aprono dal menu del tocco lungo e da nessun altro posto, quindi la destinazione è
         * una sola e non ha bisogno di viaggiare nella schermata.
         */
        is Screen.FullEditor -> {
            BackHandler { model.leaveEditor() }
            AdvancedEditorScreen(
                uri = screen.uri,
                busy = model.editorBusy,
                onSave = { look -> model.lookSave(look) },
                onBack = { model.leaveEditor() }
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
                onClipStarted = { model.clipStarted() },
                onFileChanged = { model.afterFileChanged() },
                onFileAdded = { model.afterFileAdded() },
                onEdit = { model.edit(it) },
                onEditWith = { model.editWith(it) },
                // ⚠️ Scrive le STESSE due chiavi che scrive la schermata delle impostazioni,
                // e non una copia volatile: la scorciatoia del tocco lungo su 'Info' deve
                // restare, ed è quello che l'utente ha chiesto ('e si memorizza').
                onInfoBar = { visible, where ->
                    model.updateSettings(
                        settings.copy(infoVisible = visible, infoPosition = where)
                    )
                },
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
        // ⚠️⚠️ **QUESTO È L'UNICO AVVISO DI SISTEMA CHE RESTA, DALLA `1.84`, E NON È UNA
        // DIMENTICANZA**: la riga dopo porta l'app in **secondo piano**, quindi una notifica di
        // casa sparirebbe insieme alla schermata che la disegna, cioè non si vedrebbe affatto.
        // Dire una cosa mentre si esce è esattamente il caso per cui l'avviso di sistema esiste.
        Toast.makeText(context, R.string.folder_why, Toast.LENGTH_LONG).show()
        // ⚠️ Il ripiego sulla pagina generale non è un lusso: quella mirata
        // all'app manca su qualche sistema, e senza il secondo tentativo la
        // richiesta fallirebbe con un'eccezione invece di portare da qualche parte.
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

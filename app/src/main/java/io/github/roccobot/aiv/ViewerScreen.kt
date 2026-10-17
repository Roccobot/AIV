package io.github.roccobot.aiv

import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.Dp
import android.content.Context
import android.content.Intent
import android.graphics.RectF
import android.graphics.Rect as PixelRect
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.splineBasedDecay
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.SingletonImageLoader
import coil3.compose.AsyncImagePainter
import coil3.compose.asPainter
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import coil3.compose.rememberAsyncImagePainter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Sotto questa scala un'immagine è un puntino: è il minimo della pizzicata, non dell'adattamento. */
private const val MIN_SCALE = 0.02f

/**
 * La velocità dello zoom a una mano: la scala si moltiplica per `exp(dy * questo)` a ogni
 * fotogramma, quindi 200 dp di corsa sono poco più di un raddoppio.
 *
 * ⚠️ **Verso il basso si ingrandisce** (prova dell'utente sul telefono). L'analogia con la
 * rotella dello userscript (`dv-wheel-up-in`) porterebbe al verso opposto, ed è sbagliata: la
 * rotella allontana l'immagine, il pollice la trascina, e trascinarla in giù è tirarla verso di
 * sé.
 */
private const val DRAG_ZOOM_SENSITIVITY = 0.005f

/** Il lato di un quadretto della scacchiera, in dp. */
private val CHECKER = 12.dp

/**
 * Lo stacco fra un'immagine e la sua vicina mentre si sfoglia: senza, due immagini chiare di
 * seguito sembrerebbero una sola che scivola. Per questo l'arrivo va a `larghezza + questo`, e la
 * vicina atterra al centro.
 */
private val PAGE_GAP = 16.dp

/**
 * Quanto deve essere rapido un colpo di pollice perché la pagina giri lo stesso.
 *
 * La soglia di spazio da sola non basta: un colpo veloce su un decimo di schermo vuol dire 'vai
 * avanti'. 400 dp al secondo è sopra il minimo di sistema (50, che scatterebbe a ogni sfioramento)
 * e sotto il colpo secco.
 */
private val FLING_VELOCITY = 400.dp

/**
 * Sotto questa velocità l'inerzia non parte, in dp al secondo (poi tradotti in pixel).
 *
 * 120 e non i 50 della soglia di lancio di Android, che decide se un elenco deve scorrere: qui
 * l'immagine si è già mossa col dito, e un dito che si alza da fermo lascia qualche decina di dp
 * al secondo, che darebbe un sussulto.
 */
private val GLIDE_FLOOR = 120.dp

/**
 * Quanto dura l'arrivo a destinazione, o il ritorno al posto.
 *
 * Corta: parte a dito alzato, quando non si comanda più niente. Sotto i 150 ms circa sembra uno
 * scatto, sopra i 300 sembra che l'app esiti.
 */
private const val SNAP_MS = 220

/**
 * Quanto la pagina segue il dito quando di là non c'è niente.
 *
 * Né zero né uno: bloccata sembrerebbe un gesto non ricevuto, libera prometterebbe un'immagine
 * che non c'è. Un terzo si legge come 'sei in fondo'.
 */
private const val END_RESISTANCE = 0.33f

/**
 * Quanto si aspetta, fermi, prima di leggere il pezzo nitido.
 *
 * Separa 'ho finito di muovermi' da 'sto ancora inquadrando': corto da non sembrare un ritardo,
 * lungo abbastanza che una pizzicata non faccia partire una lettura per ogni posizione.
 */
private const val TILE_DELAY_MS = 200L

/**
 * Quanto l'anello del caricamento aspetta prima di farsi vedere.
 *
 * Mezzo secondo: sotto, un indicatore che compare e sparisce sembra un lampeggio; sopra, chi
 * aspetta si chiede se l'app risponde. Le immagini del telefono si decodificano prima.
 */
private const val PROGRESS_GRACE_MS = 500L

@Composable
fun ViewerScreen(
    state: ViewerState,
    settings: Settings,
    source: Uri?,
    folder: Folder.Lookup?,
    onStep: (Int) -> Unit,
    onSettings: () -> Unit,
    onRetry: () -> Unit,
    /**
     * Il filmato in scena è partito da sé: la provenienza si consuma.
     *
     * Sale al modello perché `ViewerState` ha il setter privato, e serve alla rotazione, che rifà
     * la composizione e non il modello (`ViewerViewModel.clipStarted`).
     */
    onClipStarted: () -> Unit,
    /**
     * L'immagine in scena non è più raggiungibile a quell'indirizzo: spostata, rinominata o
     * eliminata.
     *
     * Vale anche per la rinomina ([FileKind]): il file c'è ancora, la sua riga nel MediaStore no.
     * Si chiama solo se qualcosa è riuscito, o la cartella si ricaricherebbe per niente.
     */
    onFileChanged: () -> Unit,
    /**
     * Un file nuovo è comparso nella cartella: un fotogramma esportato, una conversione.
     *
     * ⚠️ **Non è [onFileChanged]**, che rilegge la cartella e mostra quello che c'è in quella
     * posizione: dopo un'aggiunta in cima sarebbe l'immagine precedente. Qui si segna solo che la
     * griglia dietro è vecchia.
     */
    onFileAdded: () -> Unit,
    /**
     * 'Modifica' dal menu del tocco lungo: l'immagine da aprire in un editor.
     *
     * Sale al modello: la risposta dipende da un'impostazione e può cambiare schermata
     * (`ViewerViewModel.edit`).
     */
    onEdit: (Uri) -> Unit,
    onEditWith: (Uri) -> Unit,
    /**
     * La barra delle info cambiata dal tocco lungo su 'Info': accesa, e in che punto.
     *
     * Due valori e non le impostazioni intere, come `binOn` e `factFields`: la schermata non ne
     * cambia altre.
     */
    onInfoBar: (Boolean, InfoPosition) -> Unit,
    /**
     * Se l'immagine in scena viene dal cestino.
     *
     * Cambia due voci: 'Rinomina' diventa 'Ripristina' e 'Elimina' diventa definitiva.
     * Senza valore di fabbrica, perché `modifier` deve restare il primo parametro con un valore
     * predefinito (lint e convenzione di Compose).
     */
    inBin: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Le risorse da `LocalResources`, che seguono i cambi di configurazione: l'esito si compone
    // dentro una coroutine (come in `GridScreen`).
    val res = LocalResources.current

    // ⚠️ Parte da 'già visto', come nella griglia: l'archivio si legge in una coroutine, e col
    // valore opposto il velo d'aiuto lampeggerebbe per un fotogramma.
    val zoomSeen by produceState(true) { Hint.ZOOM_TAP.flow(context).collect { value = it } }
    val zoomHint = !zoomSeen

    /**
     * Il dialogo di un'operazione sui file, e `null` quando non ce n'è aperto nessuno.
     *
     * Salvabile, come nelle altre schermate che usano [FileJobDialogs]: ruotando, la finestra
     * aperta resta con quello che si stava scrivendo ([FileJobSaver]).
     */
    var job by rememberSaveable(stateSaver = FileJobSaver) { mutableStateOf<FileJob?>(null) }

    /*
     * Da 600 dp il pannello info può stare a lato del media; sotto i 1.024 parte chiuso, da 1.024
     * aperto, come nel mockup.
     * `rememberSaveable(sideDefaultOpen)` riparte al cambio di fascia: ruotando in verticale il
     * pannello torna chiuso.
     */
    val widthDp = windowWidthDp()
    val sideAvailable = Adaptive.sideAvailable(widthDp)
    val sideDefaultOpen = Adaptive.sideDefaultOpen(widthDp)
    var sideOpen by rememberSaveable(sideDefaultOpen) { mutableStateOf(sideDefaultOpen) }
    val sideAvailableNow = rememberUpdatedState(sideAvailable)

    /**
     * Il giro di un'operazione: si parte, si dice com'è andata, e se l'immagine non c'è più si
     * avverte chi sa che cosa mostrare al suo posto.
     *
     * ⚠️ **Gira in [FileWork]**, non nello scope del menu, della tela o della schermata: si
     * chiudono proprio quando un'eliminazione riesce, o a una rotazione, e si perderebbero
     * l'avviso e la rilettura (il lavoro sui file lo protegge `FileTree`).
     */
    val perform: (FileKind, suspend () -> FileTree.Outcome) -> Unit = { kind, work ->
        FileWork.scope.launch {
            val out = work()
            // Il cestino tace ([FileKind.speaks]): il suo avviso dice lo stesso e offre di
            // disfare, e due messaggi si coprirebbero.
            if (kind.speaks(out)) {
                Notices.say(outcomeText(res, out, kind), NOTICE_LONG_MS)
            }
            if (kind.gone && out.done > 0) onFileChanged()
        }
    }

    /**
     * Lo stato della riga dei dettagli, che vive più a lungo di ogni immagine.
     *
     * ⚠️ **È il rimedio al lampeggio, che era di struttura e non di animazione** (segnalazione
     * dell'utente): dentro `ImageCanvas`, che esiste solo nello stato `Ready`, la riga si
     * ricostruirebbe a ogni immagine col caricamento in mezzo. Qui è un elemento solo che cambia
     * testo: fra un'immagine e l'altra tiene i dati di quella lasciata.
     * Nasce con l'impostazione (voce `4.30-05`, presidiata da `BarraInfoTest`): nata visibile, la
     * prima misura farebbe posto a una barra che non c'è.
     */
    val info = remember { InfoBar().also { it.visible = settings.infoVisible } }

    /**
     * L'ultima immagine vista, tenuta anche mentre la prossima si carica: la riga non resta mai
     * vuota, al prezzo di mostrare per quel tempo i dati di prima.
     *
     * ⚠️ **Senza i pixel** (rilievo C35): intera, l'immagine lasciata resterebbe in memoria per
     * tutto il caricamento della successiva (fino a 190 MB a 48 megapixel), mentre la riga legge
     * solo tipo, peso, misure e nome. Al posto dei pixel c'è [NO_PIXELS]; chi ne ha bisogno prende
     * [ready], l'immagine in scena.
     */
    var shown by remember { mutableStateOf<LoadedImage?>(null) }
    LaunchedEffect(state) {
        (state as? ViewerState.Ready)?.let { shown = it.image.copy(bitmap = NO_PIXELS) }
    }
    val ready = (state as? ViewerState.Ready)?.image

    // ⚠️ Il dialogo della conversione è qui e non nel menu: il menu si chiude quando si tocca la
    // voce, e quello che gli è appeso si chiude con lui.
    var converting by remember { mutableStateOf(false) }

    /*
     * L'animazione si apre qui e non nel ramo `Ready`: la usano la fila dei comandi e
     * 'Converti/Esporta', che è quassù fuori dal menu. Su un'immagine ferma `rememberAnimation`
     * guarda i primi byte e rinuncia.
     * Il secondo argomento serve alle immagini di rete, i cui byte arrivano dalla cache dello
     * scaricamento ([rememberAnimation]).
     */
    val animation = rememberAnimation(source, state is ViewerState.Ready)

    /*
     * ⚠️ **Il fotogramma si prende quando si tocca la voce**, non quando si preme 'Esporta': in
     * mezzo passano secondi. `null` vuol dire 'converti il file intero', il caso di ogni immagine
     * ferma.
     */
    var shot by remember(source) { mutableStateOf<android.graphics.Bitmap?>(null) }

    /** L'animazione com'è adesso, per i richiami dentro un `remember`. */
    val liveAnimation = rememberUpdatedState(animation)

    // Qui e non nel menu, come il dialogo della conversione.
    var barOpen by remember { mutableStateOf(false) }
    if (barOpen) {
        InfoBarDialog(
            settings = settings,
            onChange = onInfoBar,
            onDismiss = { barOpen = false }
        )
    }

    // Il `takeIf` è sull'immagine: così non si scrive uno stato durante la composizione. Senza
    // immagine il dialogo non c'è.
    ready?.takeIf { converting }?.let { picture ->
        ConvertDialog(
            image = picture,
            source = source,
            frame = shot,
            shownFrame = animation?.shown ?: 0,
            onSaved = onFileAdded,
            onDismiss = {
                converting = false
                shot = null
            }
        )
    }

    /*
     * Il selettore di sistema per scaricare l'immagine, usato su Android 9: là scrivere dal
     * MediaStore vorrebbe `WRITE_EXTERNAL_STORAGE`, il selettore nessun permesso.
     * ⚠️ **È qui e non nel menu**: un richiamo registrato nel menu si cancella quando il menu si
     * chiude, e l'esito arriva dopo (ragionato sul codice, non misurato).
     * Il contratto si ricostruisce solo quando cambia il tipo, non a ogni ricomposizione.
     */
    val saveMime = shown?.mimeType ?: "image/*"
    val saveContract = remember(saveMime) { ActivityResultContracts.CreateDocument(saveMime) }
    val saver = rememberLauncherForActivityResult(saveContract) { target ->
        val from = source
        if (target != null && from != null) {
            scope.launch {
                val ok = context.contentResolver.openOutputStream(target)?.use { out ->
                    ImageActions.copyOriginalTo(context, from, out)
                } ?: false
                Notices.say(res.getString(
                    if (ok) R.string.toast_saved else R.string.toast_save_failed
                ))
            }
        }
    }

    /*
     * ⚠️ **Il selettore di cartella sceglie una destinazione e non scrive** (voce
     * `scarica-percorso` dell'utente): il nome e 'Salva' restano della finestra dell'app.
     * ⚠️ **Il permesso si prende subito e persistente**, o alla riapertura dell'app la cartella
     * si rilegge ma la scrittura va in `SecurityException`: un difetto che non si vede provando
     * nello stesso minuto.
     * Non chiude la finestra del nome: al ritorno c'è ancora quello che si era scritto.
     */
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { tree ->
        if (tree != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    tree,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Senza il permesso persistente la cartella vale per questa sessione sola, e
                // memorizzarla darebbe una destinazione che domani non si può scrivere.
                return@rememberLauncherForActivityResult
            }
            scope.launch { DownloadFolder.set(context, tree.toString()) }
        }
    }

    // La cartella scelta, o `null`: allora si scrive in Download.
    val cartella by produceState<String?>(null) {
        DownloadFolder.flow(context).collect { value = it }
    }

    /**
     * Il salvataggio vero, senza nessuna domanda: quello che succede quando si è deciso.
     *
     * Le strade sono tre, in quest'ordine: la cartella scelta dall'utente, se c'è, perché è una
     * scelta esplicita contro un valore di serie; Download, scritto direttamente (istruzione
     * dell'utente: *niente scelta della cartella, sempre Downloads*); e il selettore di sistema
     * sul solo Android 9, dove Download non si può scrivere (il perché è su
     * [ImageActions.saveToDownloads]). Il selettore riceve il nome battuto nella finestra, così
     * chi l'ha scritto lo ritrova.
     *
     * È separato da [scarica] per la notifica dei doppioni: 'Scarica di nuovo' deve saltare il
     * controllo che ha appena fermato il gesto, e con una funzione sola la notifica tornerebbe.
     */
    val salva: (LoadedImage, String?, String?) -> Unit =
        remember(source, saver, scope, context, cartella) {
            { picture, name, suffix ->
                val from = source
                val chosen = cartella
                if (from != null) {
                    if (chosen != null) {
                        scope.launch {
                            val ok = ImageActions.saveToFolder(
                                context, picture, from, chosen.toUri(), name, suffix
                            )
                            Notices.say(res.getString(
                                if (ok) R.string.toast_saved else R.string.toast_save_failed
                            ))
                            if (ok) noteDownload(context, picture, from, suffix)
                        }
                    } else if (ImageActions.downloadsWritable) {
                        scope.launch {
                            val ok =
                                ImageActions.saveToDownloads(context, picture, from, name, suffix)
                            Notices.say(res.getString(
                                if (ok) R.string.toast_saved else R.string.toast_save_failed
                            ))
                            if (ok) noteDownload(context, picture, from, suffix)
                        }
                    } else {
                        val whole = ImageActions.fileName(picture, from)
                        val coda = suffix ?: ImageActions.splitName(whole).second
                        saver.launch(if (name == null) whole else name + coda)
                    }
                }
            }
        }

    /*
     * Il doppione si intercetta prima di scrivere (richiesta dell'utente, giro della `1.81`,
     * punto E: stessa estensione e stesso numero di byte di un file scaricato di recente).
     * Si tiene il gesto da rifare e non un vero o un falso: la notifica offre di scaricare
     * quella immagine con quel nome, e così gli argomenti non vanno tenuti altrove.
     */
    var doppione by remember { mutableStateOf<(() -> Unit)?>(null) }

    val scarica: (LoadedImage, String?, String?) -> Unit =
        remember(source, scope, context, salva) {
            { picture, name, suffix ->
                val from = source
                if (from == null) {
                    salva(picture, name, suffix)
                } else {
                    scope.launch {
                        val mark = markOf(context, picture, from, suffix)
                        if (mark != null && DownloadLog.seen(context, mark)) {
                            doppione = { salva(picture, name, suffix) }
                        } else {
                            salva(picture, name, suffix)
                        }
                    }
                }
            }
        }

    /*
     * La notifica sparisce dopo [SAVE_SEEN_MS], lo stesso numero passato alla barra che si
     * consuma: una barra a zero sopra un tasto ancora attivo direbbe una scadenza falsa. I
     * cinque secondi li ha chiesti l'utente.
     */
    LaunchedEffect(doppione) {
        if (doppione == null) return@LaunchedEffect
        delay(SAVE_SEEN_MS)
        doppione = null
    }

    /*
     * L'immagine di cui si sta battendo il nome, e come la finestra è stata aperta: nullo vuol
     * dire che la finestra è chiusa. Il gesto serve perché il tocco lungo accende i due comandi
     * della riga del titolo a impostazioni spente (giro della `1.79`, punto D); tenuto in uno
     * stato a parte si potrebbe dimenticare di azzerarlo, qui i due valori si creano e si
     * azzerano insieme.
     */
    var naming by remember { mutableStateOf<Naming?>(null) }

    /*
     * La finestra del nome si riapre dopo una rotazione (riscontro dell'utente sulla `1.81`).
     * Nel `Bundle` passano il nome del file e il gesto, non [Naming], che contiene una bitmap
     * decodificata: dopo la rotazione l'immagine la ricarica il modello, e la finestra si
     * riattacca a lei.
     * ⚠️ Si riapre solo se l'immagine in scena è la stessa: se il sistema ha riaperto l'app su
     * un'altra immagine, il nome scritto per la prima finirebbe sulla seconda. Il testo battuto
     * lo conserva un `rememberSaveable` dentro la finestra (`SaveName.kt`).
     */
    var riapriNome by rememberSaveable { mutableStateOf<String?>(null) }
    var riapriHold by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(naming) {
        naming?.let {
            riapriNome = ImageActions.fileName(it.picture, source)
            riapriHold = it.hold
        }
    }
    LaunchedEffect(ready, riapriNome) {
        val atteso = riapriNome ?: return@LaunchedEffect
        if (naming != null) return@LaunchedEffect
        val viva = ready ?: return@LaunchedEffect
        if (ImageActions.fileName(viva, source) == atteso) naming = Naming(viva, riapriHold)
        else riapriNome = null
    }

    naming?.let { ask ->
        SaveNameDialog(
            full = ImageActions.fileName(ask.picture, source),
            hold = ask.hold,
            // La riga della cartella compare solo se ne è stata scelta una: 'Download' è il
            // comportamento di serie e non serve dirlo.
            folder = cartella?.let { DownloadFolder.label(it) },
            /*
             * 'Destinazione' vuole l'impostazione accesa oppure il tocco lungo, con la stessa
             * regola di 'Estensione' (voce `save-quando` del giro della `1.82`). La scelta si fa
             * qui perché è questa schermata ad avere le impostazioni: la finestra riceve un gesto
             * o un `null` e non sa niente di `SettingsStore`.
             * Il gesto apre una cartella e non salva niente (voce `scarica-percorso`): la
             * finestra resta aperta, e il salvataggio avviene su 'Salva'.
             */
            onPickFolder = if (ask.hold || settings.downloadPath) {
                { folderPicker.launch(null) }
            } else {
                null
            },
            /*
             * Il ritorno a Download c'è ogni volta che una cartella è stata scelta, e non dipende
             * dall'impostazione (voce `save-percorso`, giro della `1.81`): legato a
             * `downloadPath`, spegnendo l'opzione non si tornerebbe più alla cartella di serie.
             */
            onUseDownloads = cartella?.let {
                { scope.launch { DownloadFolder.forget(context) } }
            },
            // Chiudendo si azzera anche `riapriNome`, o la finestra si riaprirebbe dopo una
            // rotazione anche se l'utente l'ha chiusa.
            onDismiss = {
                naming = null
                riapriNome = null
            },
            onSave = { name, suffix ->
                naming = null
                riapriNome = null
                scarica(ask.picture, name, suffix)
            }
        )
    }

    val ops = remember(source, saver, onEdit, onEditWith, settings.saveRename) {
        MenuOps(
            job = { asked ->
                // Sul tablet le informazioni vanno nel pannello laterale (mockup `viewer`): sotto
                // i 600 dp resta [FileJob.Facts] con [FactsDialog], da 600 in su si apre
                // [ViewerFactsSide], che non copre l'immagine.
                if (asked is FileJob.Facts && sideAvailableNow.value) {
                    sideOpen = true
                } else {
                    job = asked
                }
            },
            share = { picture ->
                scope.launch {
                    if (!ImageActions.share(context, picture, source)) {
                        Notices.say(res.getString(R.string.toast_copy_failed))
                    }
                }
            },
            copy = { picture ->
                scope.launch {
                    Notices.say(
                        res.getString(
                            if (ImageActions.copyImage(context, picture)) R.string.toast_image_copied
                            else R.string.toast_copy_failed
                        )
                    )
                }
            },
            /*
             * ⚠️ `settings.saveRename` è fra le chiavi del `remember` qui sopra: il blocco
             * cattura i valori del momento in cui è creato, e senza quella chiave un interruttore
             * appena spostato arriverebbe qui solo al prossimo cambio di sorgente. È la stessa
             * trappola dell'animazione, più sotto.
             */
            save = { picture ->
                if (settings.saveRename) naming = Naming(picture, hold = false)
                else scarica(picture, null, null)
            },
            saveAs = { picture -> naming = Naming(picture, hold = true) },
            // Senza indirizzo non si fa niente: la voce che chiama questa funzione compare solo
            // con un file del telefono davanti, e un avviso sarebbe codice irraggiungibile.
            edit = { source?.let(onEdit) },
            editWith = { source?.let(onEditWith) },
            /*
             * ⚠️ L'animazione si legge da uno stato aggiornato e non si cattura qui: il blocco
             * vive in un `remember`, e quando è creato `rememberAnimation` non ha ancora aperto
             * il file. Catturata direttamente sarebbe `null` per sempre, e l'esportazione del
             * fotogramma convertirebbe in silenzio il file intero.
             */
            convert = {
                scope.launch {
                    shot = liveAnimation.value?.snapshot()
                    converting = true
                }
            },
            bar = { barOpen = true }
        )
    }

    // Ricordato senza chiavi, e non per immagine: parte spento una volta sola, quindi solo la
    // prima riga entra in dissolvenza, e poi non si ricrea più.
    val barState = remember { MutableTransitionState(false) }
    // ⚠️ Su un filmato la riga si nasconde: `shown` è l'ultima immagine vista e resta apposta,
    // contro il lampeggio fra un'immagine e l'altra, ma su un video nessuna immagine nuova la
    // sostituisce, e la riga direbbe pixel, formato e peso di un altro file.
    barState.targetState = info.visible && shown != null && state !is ViewerState.Clip

    /*
     * Lo spazio che l'immagine lascia alla barra (mockup `Viewer_NOPE`): a riposo l'immagine si
     * adatta allo spazio libero, e quando la barra si nasconde col tocco torna a tutto schermo
     * con una breve animazione (scelta A2). Il conto vive in `restPlace` (`Fit.kt`).
     * La riserva segue la barra voluta e non quella visibile, così la dissolvenza della barra e
     * lo spostamento dell'immagine partono insieme; e non segue `barState`, che aspetta la prima
     * immagine pronta e rimetterebbe la riserva a zero durante il primo caricamento.
     * ⚠️ All'apertura lo spazio si applica senza animazione (voce `3.60-02`): una copia invisibile
     * della barra ([probe]) prende subito la misura, la prima riserva si applica di colpo, e
     * finché la misura non c'è l'immagine non si disegna ([spaceReady]).
     */
    val reserveTarget = if (info.visible && state !is ViewerState.Clip) info.height else 0f
    val reserveMotion = remember { Animatable(0f) }
    var reserveSettled by remember { mutableStateOf(false) }
    val probe = info.visible && info.height == 0f
    /*
     * In orizzontale serve anche sapere dove la barra ha del testo, per la tolleranza del 5%: lo
     * dice la barra vera, un fotogramma dopo la prima immagine pronta. In verticale la tolleranza
     * non c'è, e non si aspetta.
     * ⚠️ L'attesa vale per l'anteprima e per l'immagine, non per un filmato né per un errore: là
     * la barra non compare, e aspettarla lascerebbe la schermata vuota per sempre.
     */
    val wideView = windowWidthDp() > windowHeightDp()
    val spaceReady = !info.visible ||
        (info.height > 0f && (!wideView || info.pieces.isNotEmpty()))
    LaunchedEffect(reserveTarget, spaceReady) {
        if (!reserveSettled) {
            reserveMotion.snapTo(reserveTarget)
            if (spaceReady) reserveSettled = true
        } else {
            reserveMotion.animateTo(reserveTarget, tween(BAR_ROOM_MS))
        }
    }
    val reserve = reserveMotion.value
    /*
     * La parte della riserva che appartiene al sistema (la barra di stato sopra una barra in
     * cima, quella di navigazione sotto una in fondo): la tolleranza non vale lì, e il perché è
     * su `restPlace`.
     */
    val density = LocalDensity.current
    val systemSide = if (settings.infoPosition == InfoPosition.TOP) {
        steadyDrawing(viewer = true).getTop(density)
    } else {
        steadyDrawing(viewer = true).getBottom(density)
    }
    val barSpace = if (reserve > 0f) {
        BarSpace(
            edge = if (settings.infoPosition == InfoPosition.TOP) BarEdge.TOP else BarEdge.BOTTOM,
            size = reserve,
            spans = info.spans(),
            system = systemSide.toFloat()
        )
    } else {
        null
    }

    /*
     * ⚠️ Lo sfondo si dipinge qui e non dentro `ImageCanvas`: là la scacchiera esisteva solo
     * nello stato `Ready`, e durante il caricamento il fondo diventava il colore liscio del tema,
     * cioè un lampeggio a tutto schermo a ogni cambio di immagine, visibile anche attraverso il
     * fondo semitrasparente della riga dei dettagli. Dipinta qui resta identica in tutti e tre
     * gli stati, e un colore di fondo sul `Box` non basterebbe, perché è una scacchiera.
     *
     * Si ridipinge intera a ogni fotogramma di un velo che sale o scende, ed è voluto
     * (censimento della UI del 2026-09-05): la dose del velo è letta nella lambda di disegno della
     * radice, quindi il migliaio di rettangoli si rifà mentre il velo si muove. Non si cambia
     * perché:
     * - si paga solo con 'Sfocatura dietro i pannelli' accesa; a interruttore spento nessuno
     *   scrive la mappa delle richieste;
     * - separare i due disegni non basta, perché l'invalidazione vale per il nodo di disegno, e
     *   velo e scacchiera sono nello stesso; isolare la scacchiera vorrebbe un `graphicsLayer` da
     *   mille rettangoli in memoria grafica, su una schermata che apre immagini da trenta
     *   megapixel.
     * Un velo dura poco più di un decimo di secondo: chi ci torna misuri prima il caso vero.
     */
    val checkerPx = with(density) { CHECKER.toPx() }
    val lightGreys = when (settings.bgTheme) {
        BgTheme.LIGHT -> true
        BgTheme.DARK -> false
        BgTheme.AUTO -> MaterialTheme.colorScheme.background.isLight()
    }

    /*
     * Da 600 dp l'immagine e il pannello delle informazioni sono in riga, come nel mockup. Sotto
     * i 600 la riga ha un solo figlio a peso pieno, e la disposizione del telefono non cambia.
     * Il lato segue [Settings.hand]: per i destri il pannello va a sinistra, per i mancini a
     * destra.
     */
    val panelOnStart = settings.hand == Hand.RIGHT

    // Il pannello è uno solo per i due lati: un argomento nuovo si aggiunge in un posto.
    @Composable
    fun Side() {
        if (!sideAvailable || !sideOpen) return
        AivPalette(light = lightGreys) {
            ViewerFactsSide(
                uri = source,
                fields = settings.factRows,
                width = Adaptive.sideWidth(widthDp),
                onClose = { sideOpen = false },
                onRename = { renamed -> job = FileJob.Rename(listOf(renamed)) }
            )
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        if (panelOnStart) Side()
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .drawBehind {
                drawBackground(size, checkerPx, lightGreys, settings.bgType)
                /*
                 * La scacchiera si copre di grigio mentre arriva la sfocatura (richiesta del giro
                 * della `1.67`): sfocata diventa una griglia di macchie in movimento, e sotto una
                 * superficie aperta non c'è trasparenza da mostrare. Il grigio è quello di
                 * `BgType.SOLID`, chiaro o scuro col tema, per non avere un secondo colore da
                 * tenere allineato.
                 * Segue l'avanzamento del velo e non la sua opacità ([veilProgress]), così la
                 * copertura arriva e se ne va nello stesso intervallo della sfocatura. La lettura
                 * avviene nel disegno, quindi costa un ridisegno e non una ricomposizione; a
                 * sfocatura spenta la dose è zero e non si disegna niente.
                 */
                val coperto = veilProgress()
                if (coperto > 0f) {
                    drawRect(color = piatto(lightGreys), alpha = coperto, size = size)
                }
            }
    ) {
        when (state) {
            is ViewerState.Loading -> {
                if (reserveSettled) PreviewThumb(source, settings, barSpace)
                /*
                 * L'anello aspetta [PROGRESS_GRACE_MS]: un anello che compare e sparisce in un
                 * decimo di secondo è un lampeggio, e le immagini del telefono si decodificano
                 * prima. Così compare solo dove serve, con la rete e le immagini enormi.
                 * L'attesa vale anche per la percentuale.
                 */
                var late by remember(source) { mutableStateOf(false) }
                LaunchedEffect(source) {
                    delay(PROGRESS_GRACE_MS)
                    late = true
                }
                if (late) Progress(state.progress, Modifier.align(Alignment.Center))
            }
            is ViewerState.Error -> ErrorMessage(
                state = state,
                // Riprovare si offre solo se c'è un indirizzo: un errore senza sorgente (la
                // cartella che si è svuotata) non ha niente da rifare.
                onRetry = onRetry.takeIf { source != null },
                modifier = Modifier.align(Alignment.Center)
            )
            is ViewerState.Ready -> {
                /*
                 * ⚠️ L'animazione non è uno stato a parte: una GIF resta una `Ready` con la sua
                 * `LoadedImage`, e l'animazione le passa solo i pixel del fotogramma corrente.
                 * Con un ramo suo, come `Clip` per i filmati, si perderebbero ingrandimento,
                 * trascinamento, tasselli e riquadro di riposo. Si apre più in su perché la usa
                 * anche la finestra di 'Converti/Esporta'.
                 */
                if (reserveSettled) ImageCanvas(
                    state.image, settings, source, folder, info, onStep, ops, inBin,
                    animation?.frame,
                    onSingleTap = { animation?.toggle() },
                    /*
                     * Il tocco lungo ferma la riproduzione (richiesta dell'utente): senza,
                     * 'Converti/Esporta' agirebbe su un fotogramma già cambiato, e il menu
                     * coprirebbe un'immagine in movimento. Ferma e non alterna, così
                     * un'animazione già in pausa non riparte.
                     */
                    onHold = { animation?.pause() },
                    bar = barSpace
                )
                if (animation != null) {
                    AnimatedBar(
                        animation = animation,
                        counter = settings.animCounter,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            // Con la riga dei dettagli in basso i comandi le vanno sopra, alla
                            // sua altezza misurata (la stessa che l'immagine le lascia).
                            .padding(
                                bottom = ANIM_LIP + if (barSpace?.edge == BarEdge.BOTTOM) {
                                    with(density) { barSpace.size.toDp() }
                                } else {
                                    0.dp
                                }
                            )
                    )
                }
            }
            is ViewerState.Clip -> ClipStage(
                uri = state.uri,
                // La strisciata porta avanti e indietro solo se c'è una serie, come per
                // un'immagine sola. Arriva la serie intera perché il filmato fa entrare dal bordo
                // le vicine, le stesse che [ImageCanvas] chiede a `toward`.
                series = folder?.seriesOrNull,
                settings = settings,
                onStep = onStep,
                // La regola è in `Arrival.plays`, che incrocia la provenienza con l'impostazione.
                autoStart = state.from.plays(settings.clipAutoplay),
                onStarted = onClipStarted
            )
        }

        /*
         * La copia invisibile della barra misura l'altezza prima che compaia la barra vera, così
         * la prima immagine si disegna già nello spazio giusto (voce `3.60-02`). Ha la forma della
         * barra vera con dati finti, perché l'altezza dipende dalla larghezza e dai caratteri e
         * non dall'immagine (`oneRowFits`). Sparisce appena c'è la misura, e i lettori di schermo
         * la ignorano.
         */
        if (probe) {
            Surface(
                modifier = Modifier
                    .align(
                        if (settings.infoPosition == InfoPosition.TOP) Alignment.TopCenter
                        else Alignment.BottomCenter
                    )
                    .fillMaxWidth()
                    .alpha(0f)
                    .clearAndSetSemantics { }
                    .onGloballyPositioned { info.height = it.size.height.toFloat() }
            ) {
                DetailsPanel(
                    image = PROBE_IMAGE,
                    percent = 1f,
                    folder = null,
                    atTop = settings.infoPosition == InfoPosition.TOP
                )
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visibleState = barState,
            // Entra ed esce con la sola opacità: le transizioni predefinite cambiano anche la
            // misura, e una riga che si apre a soffietto sposta il contatore, che è quello che
            // si guarda mentre si sfoglia.
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(
                if (settings.infoPosition == InfoPosition.TOP) Alignment.TopCenter
                else Alignment.BottomCenter
            )
        ) {
            // L'ultima immagine vista resta anche durante l'uscita, o la riga si svuoterebbe
            // mentre sfuma.
            shown?.let { picture ->
                /*
                 * ⚠️ Lo sfondo è fuori dalla dissolvenza: durante la transizione
                 * `AnimatedContent` tiene due pannelli sovrapposti, e due veli semitrasparenti
                 * a metà strada coprono `1 - 0.57 x 0.57 = 0.675` invece di `0.86`, cioè lo
                 * sfondo si schiarisce e lampeggia. Quello che deve restare fermo si dipinge
                 * fuori da ciò che si anima: rimettere il velo dentro `DetailsPanel` rimetterebbe
                 * il lampeggio. La larghezza piena tiene ferma la forma dello sfondo quando il
                 * testo cambia.
                 *
                 * Fra una riga e l'altra c'è una dissolvenza incrociata (richiesta dell'utente),
                 * senza trasformazione di misura (`using null`): quella predefinita animerebbe la
                 * larghezza e sposterebbe il contatore. La chiave è l'immagine e non la
                 * percentuale, che cambia a ogni fotogramma di una pinza.
                 */
                // Le info prendono il tema dello sfondo e non quello dell'app (`4.03`): con
                // 'Tema dello sfondo' su `Scuro` il pannello è scuro, come il pannello laterale.
                AivPalette(light = lightGreys) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = PANEL_VEIL),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        // La misura che l'immagine le lascia: vedi `reserve` più sopra.
                        .onGloballyPositioned {
                            info.height = it.size.height.toFloat()
                            info.left = it.boundsInRoot().left
                        }
                ) {
                    AnimatedContent(
                        targetState = picture,
                        transitionSpec = { fadeIn() togetherWith fadeOut() using null },
                        label = "dettagli"
                    ) { shownNow ->
                        DetailsPanel(
                            image = shownNow,
                            percent = info.percent,
                            // ⚠️ Senza serie la strisciata non fa niente, e un gesto che non
                            // fa niente sembra guasto: la riga stampa l'esito col suo motivo.
                            // Solo per un'immagine di questo telefono: per una del web o di
                            // una chat 'non è nella galleria' è la normalità.
                            folder = folder.takeIf { source?.scheme?.lowercase() == "content" },
                            // Solo la riga che entra dice dove disegna: vedi [BarReport].
                            report = if (shownNow === picture) BarReport(info) else null,
                            atTop = settings.infoPosition == InfoPosition.TOP
                        )
                    }
                }
                }
            }
        }

        /*
         * Il velo d'aiuto del doppio tocco, alla prima immagine dopo l'installazione (richiesta
         * dell'utente): le voci dello zoom non sono nel menu, e senza il velo il gesto non lo
         * scoprirebbe nessuno. La condizione su `Ready` lo tiene fuori da un filmato, dove il
         * doppio tocco non fa niente, e dal caricamento.
         * È in fondo al `Box`, dopo la barra delle info, per non farsi coprire; e
         * `matchParentSize` esiste solo dentro un `Box`, per questo `HintCentre` è
         * un'estensione di `BoxScope`.
         */
        if (zoomHint && state is ViewerState.Ready) {
            HintCentre(
                text = stringResource(R.string.hint_zoom_tap),
                onDone = { scope.launch { Hint.ZOOM_TAP.remember(context) } }
            )
        }

        /*
         * La notifica del doppione (giro della `1.81`, punto E): è l'avviso dell'eliminazione
         * annullata, con 'Scarica di nuovo' come azione e una durata di [SAVE_SEEN_MS], come ha
         * chiesto l'utente. Passa da [Notices]: la superficie è una sola, in `AivApp`, sopra
         * tutte le schermate, e qui si manda soltanto la riga.
         */
        val doppioneFrase = stringResource(R.string.save_seen_notice)
        val doppioneAzione = stringResource(R.string.save_seen_again)
        LaunchedEffect(doppione) {
            val ancora = doppione ?: return@LaunchedEffect
            Notices.offer(
                text = doppioneFrase,
                action = doppioneAzione,
                millis = SAVE_SEEN_MS,
                onGone = { doppione = null }
            ) { ancora() }
        }
    }
        if (!panelOnStart) Side()
    }

    // Le finestre sono fuori dal riquadro dell'immagine e dal menu: devono sopravvivere
    // all'immagine su cui agiscono. Vedi `perform`.
    FileJobDialogs(
        job = job,
        // Qui le impostazioni ci sono, e i campi si leggono da loro; la griglia, che non le
        // ha, se li fa passare.
        fields = settings.factRows,
        onClose = { job = null },
        onRun = perform
    )
}

/**
 * La finestra del nome che si sta compilando: su quale immagine, e da quale gesto.
 *
 * I due valori viaggiano insieme: il tocco lungo su 'Scarica' accende i comandi che le
 * impostazioni terrebbero spenti (giro della `1.79`, punto D). Tenuto in uno stato a parte, il
 * booleano si potrebbe dimenticare di azzerare, e la finestra successiva si aprirebbe col
 * permesso della precedente.
 */
private class Naming(val picture: LoadedImage, val hold: Boolean)

/**
 * Il segnaposto dei pixel nella copia che la riga delle info tiene dell'immagine lasciata: un
 * pixel solo, condiviso. Vedi `shown` in [ViewerScreen].
 *
 * ⚠️ Pigro: le costanti di questo file si creano appena si usa una sua funzione qualunque
 * (`formatBytes` la chiamano griglia e impostazioni), e una tela creata là si pagherebbe in ogni
 * schermata; nel banco senza grafica nativa andava in errore.
 */
private val NO_PIXELS by lazy { ImageBitmap(1, 1) }

/**
 * Le richieste che il menu del tocco lungo non esegue da sé, e che passa a chi lo contiene.
 *
 * ⚠️ Il menu si chiude prima che finiscano: toccare una voce chiama `onDismiss`, il menu esce
 * dalla composizione, e con lui il suo ambito di coroutine e i suoi richiami registrati. Una
 * condivisione si annullerebbe alla prima sospensione, l'esito del selettore di sistema non
 * tornerebbe a nessuno, e una finestra aperta da là sparirebbe con l'immagine.
 * Quello che comincia e finisce dentro il tocco (lo zoom, la riga dei dettagli) resta nel menu;
 * qui arriva ciò che dura più del gesto, compreso 'Copia immagine'.
 */
private class MenuOps(
    val job: (FileJob) -> Unit,
    val share: (LoadedImage) -> Unit,
    /** 'Copia immagine': la PNG negli appunti, scritta fuori dal filo dell'interfaccia. */
    val copy: (LoadedImage) -> Unit,
    val save: (LoadedImage) -> Unit,
    /**
     * Tocco lungo su 'Scarica': chiede il nome adesso, per questa volta sola.
     *
     * È una seconda funzione e non un parametro di [save], come `edit` e `editWith`: il menu
     * riferisce quale gesto è arrivato, e chi ha le impostazioni decide che cosa vuol dire. Con
     * l'impostazione accesa le due strade portano allo stesso punto, e il menu non lo sa.
     */
    val saveAs: (LoadedImage) -> Unit,
    /**
     * 'Modifica': apre l'editor scelto, o chiede quale usare la prima volta.
     *
     * Non prende l'immagine: si modifica il file, cioè `source`, e l'editor riparte da lì;
     * l'immagine decodificata inviterebbe a modificare i pixel in memoria. Cambia schermata, e
     * deciso dal menu quel cambio lo smonterebbe a metà chiamata.
     */
    val edit: () -> Unit,
    /**
     * Tocco lungo su 'Modifica': fa scegliere l'app adesso, e la scelta resta.
     *
     * Seconda funzione e non parametro di [edit], come [saveAs]. Vedi `ViewerViewModel.editWith`.
     */
    val editWith: () -> Unit,
    /**
     * 'Converti/Esporta': apre la finestra che salva l'immagine in un altro formato.
     *
     * Non prende niente: la finestra ha già l'immagine e il suo indirizzo.
     */
    val convert: () -> Unit,
    /**
     * Tocco lungo su 'Info': apre il riquadrino della barra delle info.
     *
     * Non prende e non decide niente, come [convert]: il riquadro vive accanto alle impostazioni.
     */
    val bar: () -> Unit
)

/**
 * Il riquadrino della barra delle info, aperto dal tocco lungo su 'Info'.
 *
 * L'interruttore è sulla riga del titolo, allineato a destra, e sotto ci sono i due gettoni del
 * lato (riscontro `chip-colonna`); il perché della riga di gettoni unica è in testa a
 * `InfoBar.kt`. Il titolo è l'etichetta dell'interruttore, come nelle impostazioni, e le stringhe
 * sono le stesse delle impostazioni (`settings_info_visible`, `settings_info_position`,
 * `settings_top`, `settings_bottom`), perché la stessa cosa non abbia due nomi.
 * Si applica e si salva subito, senza conferma (richiesta dell'utente); 'Chiudi' chiude e basta.
 * È un `AlertDialog` di Material e non un guscio nostro (riscontro `striscia-sotto`).
 */
@Composable
private fun InfoBarDialog(
    settings: Settings,
    onChange: (Boolean, InfoPosition) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(onDismiss),
        properties = loweredWindow(onDismiss),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_info_visible),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = settings.infoVisible,
                    // Spegnendo, il lato resta quello che c'è: riaccesa, la barra si ritrova
                    // dov'era, come nelle impostazioni.
                    onCheckedChange = { onChange(it, settings.infoPosition) }
                )
            }
        },
        text = {
            InfoSideRow(
                selected = settings.infoPosition,
                enabled = settings.infoVisible,
                onSelect = { onChange(true, it) }
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pick_close)) }
        }
    )
}

/**
 * Quello che la riga dei dettagli deve sapere e che non cambia con l'immagine: la visibilità,
 * la percentuale di zoom, lo stato dei tasselli e le misure della barra. Il resto la riga lo
 * prende dall'immagine che le viene passata.
 */
@Stable
private class InfoBar {
    var visible by mutableStateOf(true)
    var percent by mutableFloatStateOf(1f)

    /**
     * Che cosa stanno facendo i tasselli a piena risoluzione, e `null` quando non
     * c'entrano (immagine non campionata, o lettura non ancora tentata).
     *
     * È qui e non dentro `ImageCanvas` perché la riga dei dettagli vive più a lungo
     * dell'immagine: tenuto di là, il dato sparirebbe a ogni cambio di pagina.
     */
    var tiles by mutableStateOf<String?>(null)

    /**
     * L'altezza della barra in pixel, margini di sistema compresi, e zero finché non è stata
     * misurata.
     *
     * Resta anche quando la barra si nasconde: è lo spazio che l'immagine le lascia quando
     * riappare, e azzerata la barra comparirebbe sopra l'immagine e la sposterebbe un fotogramma
     * dopo.
     */
    var height by mutableFloatStateOf(0f)

    /**
     * Dove la barra disegna, in pixel della radice: un rettangolo per pezzo (nome, dati,
     * contatore, marchio). Lo scrive solo la riga che sta entrando, non quella che sfuma via.
     */
    val pieces = mutableStateMapOf<String, Rect>()

    /** Il bordo sinistro della barra nella radice, per riportare [pieces] nella vista. */
    var left by mutableFloatStateOf(0f)

    /** I tratti orizzontali con del testo, in pixel della vista. Vedi [BarSpace.spans]. */
    fun spans(): List<ClosedFloatingPointRange<Float>> =
        pieces.values.map { (it.left - left)..(it.right - left) }
}

/**
 * Un filmato nel visualizzatore: il suo fotogramma, la durata e la riproduzione.
 *
 * Qui la riproduzione si accende e si spegne: il lettore lo costruisce e lo rilascia questa
 * funzione, che disegna anche la superficie video; se partire lo decide chi chiama, con
 * `autoStart`.
 * La strisciata è quella delle immagini: il filmato si trascina col dito, la vicina entra dal
 * bordo e la pagina si assesta prima del passo (voce `video-scorre`). Senza il gesto, in una
 * cartella mista si resterebbe bloccati sul filmato. La superficie è una texture perché una
 * `SurfaceView` la compone il sistema fuori dall'albero e non si lascia traslare.
 * I numeri sono quelli di `ImageCanvas`: soglia a un quinto della larghezza, [PAGE_GAP],
 * [SNAP_MS] e [END_RESISTANCE]; due tarature diverse sembrerebbero due gesti diversi. Lo zoom
 * no: pinza, panoramica e tasselli lavorano su un'immagine decodificata, che un filmato non ha.
 */
@Composable
@androidx.annotation.OptIn(UnstableApi::class)
// `internal` e non `private` per il banco di prova, che la monta: il passo si chiede alla fine
// dell'assestamento, un legame che il compilatore non vede. Nessun altro modulo la chiama.
internal fun ClipStage(
    uri: Uri,
    /** La serie da sfogliare, o `null` fuori da una cartella. */
    series: Folder.Series?,
    /** Servono alle vicine, che sono miniature come quelle di `ImageCanvas`. */
    settings: Settings,
    onStep: (Int) -> Unit,
    /** Se questo filmato deve partire da sé. Vedi `Arrival.plays`. */
    autoStart: Boolean,
    /** Da chiamare appena è partito, così una rotazione non lo rifà ricominciare. */
    onStarted: () -> Unit
) {
    val context = LocalContext.current
    val model = remember(uri, context) { Thumbs.request(context, uri) }
    val poster = rememberAsyncImagePainter(
        model = model,
        // La chiave si registra qui come in [Preview], dove Coil la dice: senza, un filmato
        // aperto dalla griglia non lascerebbe niente al sostituto qui sotto, e il primo
        // fotogramma della volta dopo sarebbe vuoto.
        onState = { st ->
            if (st is AsyncImagePainter.State.Success) Thumbs.note(uri, st.result.memoryCacheKey)
        }
    )
    /*
     * ⚠️ Il sostituto letto in composizione: `AsyncImagePainter` parte da una coroutine e al
     * primo fotogramma non disegna niente, anche con l'immagine in memoria (misurato sul bytecode
     * di Coil, vedi `Thumbs.note`). Senza, alla fine della strisciata compariva un fotogramma
     * nero prima del filmato. È lo stesso rimedio di [Preview]: la miniatura che entra dal bordo
     * e quella al centro sono lo stesso disegno. Va sotto il pittore vero, che poi disegna la
     * stessa immagine nello stesso riquadro, e il passaggio non si vede.
     */
    val standIn = remember(uri, context) { Thumbs.cached(context, uri)?.asPainter(context) }

    /*
     * ⚠️ Un lettore solo per tutta la schermata, non uno per filmato: uno per indirizzo
     * ricreerebbe la superficie a ogni passo, con un lampo nero a ogni strisciata, e un lettore
     * non rilasciato tiene un decodificatore hardware, che su un telefono sono pochi.
     * Il fuoco audio si chiede (`handleAudioFocus`): senza, un filmato suonerebbe sopra la
     * musica di un'altra app, che poi non ripartirebbe. `setHandleAudioBecomingNoisy` mette in
     * pausa quando si sfilano le cuffie.
     */
    val player = remember(context) {
        ExoPlayer.Builder(context)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    /*
     * Parte solo se è stato chiesto: a un filmato si arriva toccandolo o strisciando da
     * un'immagine, e nel secondo caso un audio che parte da solo è una sorpresa sgradevole. Per
     * questo i due percorsi si distinguono e l'interruttore è spento di fabbrica.
     * ⚠️ Il ramo che mette in pausa serve: il lettore è uno solo, e `playWhenReady` appartiene al
     * lettore e non al filmato. In una cartella di soli filmati il ramo resta `Clip`, questo
     * effetto rifà `setMediaItem` e `prepare` sullo stesso lettore, e senza la pausa ogni
     * strisciata farebbe ripartire l'audio. Dopo la pausa il tasto grande ricompare da sé, con
     * l'effetto di `ClipKeys`.
     * La provenienza si consuma appena il filmato è partito (`onStarted`), o una rotazione lo
     * farebbe ricominciare.
     */
    LaunchedEffect(uri, player) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        if (autoStart) {
            player.play()
            onStarted()
        } else {
            player.pause()
        }
    }

    /*
     * Si ferma quando l'app va in secondo piano, o l'audio continuerebbe da un'app che non si
     * vede. `ON_STOP` e non `ON_PAUSE`, così una finestra di sistema sopra il video non lo
     * interrompe. Il rilascio è nello stesso effetto.
     */
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, player) {
        val watcher = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        owner.lifecycle.addObserver(watcher)
        onDispose {
            owner.lifecycle.removeObserver(watcher)
            player.release()
        }
    }

    val play = rememberPlayPauseButtonState(player)
    val shown = rememberPresentationState(player)
    val scope = rememberCoroutineScope()
    val progress = rememberProgressStateWithTickInterval(player, TICK_MS, scope)

    val activity = remember(context) { Knobs.activityOf(context) }
    /*
     * La luminosità torna com'era quando si esce dal filmato (istruzione dell'utente). Il
     * ripristino è qui e non nelle vie d'uscita: uscire dal filmato è la fine di questa
     * composizione, qualunque gesto l'abbia causata, e una via d'uscita nuova non va ricordata.
     * È la stessa scelta della potatura in `Bin.list`.
     * Fra due filmati adiacenti non scatta, ed è voluto: `startLoad` va da `Clip` a `Clip` e la
     * composizione resta, quindi chi ha abbassato lo schermo non se lo vede risalire. Passando da
     * un'immagine invece la composizione si smonta. Anche l'app in secondo piano non è
     * un'uscita.
     */
    DisposableEffect(activity) {
        onDispose { Knobs.clearBrightness(activity) }
    }
    var knob by remember { mutableStateOf<Knob?>(null) }
    // La chiave è il valore: ogni movimento del dito rifà l'attesa, e l'indicatore sparisce
    // un attimo dopo l'ultimo ritocco e non a metà del gesto.
    LaunchedEffect(knob) {
        if (knob != null) {
            delay(KNOB_LINGER)
            knob = null
        }
    }

    /*
     * ⚠️ I due stati della strisciata hanno `uri` per chiave: [ImageCanvas] si smonta a ogni
     * passo, questa funzione fra due filmati adiacenti no (vedi la nota sulla luminosità). Con
     * la chiave la pagina nuova parte al centro e ferma, da qualunque strada arrivi.
     */
    var travel by remember(uri) { mutableFloatStateOf(0f) }

    /** Se una pagina sta andando a destinazione, per non far cominciare una seconda corsa. */
    var settling by remember(uri) { mutableStateOf(false) }

    /*
     * La larghezza serve anche in composizione: il rilevatore ha la sua `size`, ma le vicine
     * fuori dallo schermo le posa un `graphicsLayer`, che quella misura non ce l'ha.
     */
    var viewWidth by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val pageGap = with(density) { PAGE_GAP.toPx() }

    /*
     * `derivedStateOf` e non `travel` letto qui: `travel` cambia a ogni fotogramma del
     * trascinamento, e la composizione si rifarebbe sessanta volte al secondo. Così si ricompone
     * due volte per gesto, e il movimento passa dal `graphicsLayer`.
     */
    val dragging by remember(uri) { derivedStateOf { travel != 0f } }

    /*
     * Le vicine si chiedono a `toward` come in [ImageCanvas], con la stessa regola di salto:
     * quello che entra dal bordo e quello che `onStep` aprirà devono essere lo stesso elemento.
     */
    val skipVideos = settings.imagesOnly
    val nextUri = series?.toward(1, skipVideos)
    val prevUri = series?.toward(-1, skipVideos)
    val stepping = series != null

    /*
     * Il passo si chiede dopo l'animazione, come in [ImageCanvas]: chiesto prima, l'elemento
     * nuovo arriverebbe mentre il filmato scivola ancora via.
     * ⚠️ Dopo un passo nessuno spegne `settling`, ed è voluto: o la composizione si smonta o
     * cambia `uri`, e le chiavi rifanno i due stati. Il passo si chiede solo dove `toward` ha
     * risposto, quindi la pagina non resta fuori schermo.
     */
    fun settle(step: Int) {
        if (step == 0 && travel == 0f) return
        settling = true
        scope.launch {
            val target = when {
                step > 0 -> -(viewWidth + pageGap)
                step < 0 -> viewWidth + pageGap
                else -> 0f
            }
            Animatable(travel).animateTo(target, tween(SNAP_MS)) { travel = value }
            if (step == 0) settling = false else onStep(step)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            /*
             * Nero puro sempre, sopra la scacchiera, con qualunque tema e tinta (richiesta
             * dell'utente): le bande sopra e sotto un filmato fanno parte di quello che si
             * guarda, e un'altra tinta le renderebbe una cornice. La scacchiera mostra la
             * trasparenza, e un filmato non è mai trasparente.
             */
            .background(Color.Black)
            .onSizeChanged { viewWidth = it.width.toFloat() }
            .pointerInput(uri, stepping, skipVideos) {
                val threshold = size.width / 5f
                val reach = size.height * KNOB_SPAN
                val middle = size.width / 2f
                var axis = Axis.UNDECIDED
                var travelled = 0f
                var onLeft = false
                var started = 0f
                detectDragGestures(
                    onDragStart = { at ->
                        axis = Axis.UNDECIDED
                        travelled = 0f
                        // Il lato si decide da dove il dito si posa: chi comincia a
                        // sinistra e sconfina a destra sta ancora regolando la luminosità.
                        onLeft = at.x < middle
                    },
                    // Il passo si decide alla fine del gesto e non appena la soglia è superata:
                    // il dito può tornare indietro, come nella strisciata delle immagini. Poi la
                    // pagina va a destinazione o torna al suo posto, e solo là si chiede il passo.
                    onDragEnd = {
                        if (axis != Axis.SIDEWAYS || !stepping) return@detectDragGestures
                        val step = when {
                            travelled <= -threshold -> 1
                            travelled >= threshold -> -1
                            else -> 0
                        }
                        val reachable = when {
                            step > 0 -> nextUri != null
                            step < 0 -> prevUri != null
                            else -> false
                        }
                        settle(if (reachable) step else 0)
                    },
                    // Un gesto annullato (un secondo dito, una finestra che si apre) riporta la
                    // pagina al suo posto.
                    onDragCancel = {
                        if (axis == Axis.SIDEWAYS) settle(0)
                    },
                    onDrag = { _, delta ->
                        /*
                         * ⚠️ L'asse si decide al primo movimento e poi non cambia: è quello
                         * che tiene separati i tre gesti. Senza, una strisciata orizzontale un
                         * po' storta cambierebbe pagina e alzerebbe il volume.
                         */
                        if (axis == Axis.UNDECIDED) {
                            axis = if (abs(delta.x) >= abs(delta.y)) Axis.SIDEWAYS else Axis.UPRIGHT
                            if (axis == Axis.UPRIGHT) {
                                started = if (onLeft) Knobs.brightness(activity) else Knobs.volume(context)
                                travelled = 0f
                            }
                        }
                        if (axis == Axis.SIDEWAYS) {
                            travelled += delta.x
                            /*
                             * La resistenza al capolinea è quella di [ImageCanvas]: dove non c'è
                             * niente da aprire la pagina si lascia tirare a [END_RESISTANCE].
                             * ⚠️ Il gesto muove il filmato solo se c'è una serie: senza, `settle`
                             * non sarebbe mai chiamato e la pagina resterebbe spostata.
                             */
                            if (stepping) {
                                val reachable =
                                    if (travelled < 0f) nextUri != null else prevUri != null
                                travel = if (reachable) travelled else travelled * END_RESISTANCE
                            }
                            return@detectDragGestures
                        }
                        travelled += delta.y
                        // Il segno è rovesciato: verso l'alto alza, mentre le coordinate dello
                        // schermo crescono verso il basso.
                        val moved = (started - travelled / reach).coerceIn(0f, 1f)
                        if (onLeft) Knobs.setBrightness(activity, moved) else Knobs.setVolume(context, moved)
                        knob = Knob(onLeft, moved)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        /*
         * ⚠️ La superficie non ascolta nessun tocco, ed è per questo che la strisciata qui sopra
         * funziona: `PlayerSurface` non ha ascoltatori, mentre `PlayerView` di `media3-ui`, coi
         * suoi comandi, si prenderebbe il tocco.
         *
         * Le proporzioni le tiene `resizeWithContentScale`, che legge la misura del filmato da
         * [rememberPresentationState]: da sola la superficie prende lo spazio che le si dà, e il
         * filmato risultava stirato. Lo stesso riquadro va al fotogramma di copertura, o
         * miniatura e video avrebbero due riquadri diversi.
         * ⚠️ Finché la misura non si sa, il riquadro prende tutto lo schermo: con la misura nulla
         * `resizeWithContentScale` centra il figlio alla sua misura naturale, che per una
         * superficie vuota è piccola, e il filmato appariva piccolo e poi saltava a tutto
         * schermo (letto nel bytecode). La copertura, disegnata con `ContentScale.Fit`, non si
         * deforma; la superficie sì, quindi in quel tratto è trasparente e davanti c'è la
         * copertura (voce `video-sfoglia`).
         * La copertura resta anche dopo il primo fotogramma, finché la misura non arriva:
         * `coverSurface` si spegne appena il lettore ha qualcosa da mostrare. Se la misura non
         * arriva mai è perché non arriva nessun fotogramma, e allora `coverSurface` resta acceso.
         */
        val misurato = shown.videoSizeDp != null
        val fitted = Modifier.resizeWithContentScale(ContentScale.Fit, shown.videoSizeDp)
        val scaled = if (misurato) fitted else Modifier.fillMaxSize()
        /*
         * Si trascinano la superficie e la copertura, non i comandi, che appartengono al lettore
         * e non alla pagina. Un livello solo per le due: mosse ognuna per conto suo, un
         * fotogramma di ritardo farebbe vedere i bordi staccarsi.
         */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = travel },
            contentAlignment = Alignment.Center
        ) {
        /*
         * ⚠️ La superficie è a texture e non nativa: una `SurfaceView` la compone il sistema in
         * una finestra sua, fuori dall'albero, e durante lo scorrimento resta ferma e poi sparisce
         * di colpo. Una `TextureView` vive nell'albero, e scorre, sfuma e si ritaglia. Costa più
         * memoria grafica e un fotogramma in più all'avvio.
         */
            PlayerSurface(
                player = player,
                surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                // Trasparente e non assente: la superficie deve restare nell'albero per ricevere
                // il filmato.
                modifier = if (misurato) scaled else scaled.alpha(0f)
            )

            /*
             * Il fotogramma di copertura resta finché il video non ha niente da mostrare
             * (`coverSurface`) o finché la misura non si sa (vedi sopra): è la miniatura della
             * griglia, già in memoria, e al posto di un lampo nero si vede il filmato fermo.
             * Il sostituto va sotto il pittore di Coil, come in [Preview]: per un fotogramma Coil
             * non disegna niente, e durante la strisciata quel fotogramma cade quando la pagina
             * arriva al centro.
             */
            if (shown.coverSurface || !misurato) {
                standIn?.let {
                    Image(
                        painter = it,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = scaled
                    )
                }
                Image(
                    painter = poster,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = scaled
                )
            }
        }

        // Le vicine esistono solo mentre si trascina, come in [ImageCanvas]: sono le miniature
        // della griglia, già in memoria, e compaiono nell'istante del gesto.
        if (dragging) {
            Neighbour(nextUri, settings, null) { travel + viewWidth + pageGap }
            Neighbour(prevUri, settings, null) { travel - viewWidth - pageGap }
        }

        // L'indicatore della manopola: la sola risposta visibile a un gesto senza tasti.
        knob?.let { KnobBadge(it) }

        // Il tasto al centro sparisce mentre si guarda: vedi [ClipKeys].
        ClipKeys(
            playing = !play.showPlay,
            onPlayPause = { play.onClick() },
            position = progress.currentPositionMs,
            duration = progress.durationMs,
            onSeek = { player.seekTo(it) }
        )
    }
}

/**
 * Su quale asse si è mosso il dito sopra un filmato, deciso una volta sola per gesto.
 *
 * Tre valori e non un booleano: fra il dito che si posa e il primo movimento l'asse non si sa.
 */
private enum class Axis { UNDECIDED, SIDEWAYS, UPRIGHT }

/** Quale manopola si sta girando e a che punto sta, da 0 a 1. */
private data class Knob(val brightness: Boolean, val value: Float)

/**
 * L'indicatore che dice che cosa sta cambiando mentre il dito scorre in verticale.
 *
 * Il volume di un filmato muto non si sente, e senza indicatore non si saprebbe quale delle due
 * manopole si è presa. Un'icona e un numero, che spariscono da soli. L'icona non ha descrizione:
 * il valore accanto è già testo, e il lettore di schermo lo annuncerebbe due volte.
 */
@Composable
private fun BoxScope.KnobBadge(knob: Knob) {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .clip(RoundedCornerShape(KNOB_ROUND))
            .background(CLIP_INK)
            .padding(horizontal = CLIP_GAP, vertical = CLIP_LIP),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (knob.brightness) Glyphs.Material.LightModeOutlined else Glyphs.Material.VolumeUpOutlined,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(CLIP_GLYPH)
        )
        Text(
            text = "${(knob.value * 100).roundToInt()}%",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * I comandi del filmato: il tasto grande in mezzo e la barra in fondo.
 *
 * ⚠️ Scritti qui e non presi da `PlayerView`, che è una `View` e si prende i tocchi: con lei la
 * strisciata per cambiare pagina non arriverebbe mai. Spariscono mentre il filmato va, e
 * tornano toccando; in pausa restano. La barra si trascina ([ValueSlider]) per saltare avanti,
 * e accanto c'è il tempo. Interno per `SemanticheTest`.
 */
@Composable
internal fun BoxScope.ClipKeys(
    playing: Boolean,
    onPlayPause: () -> Unit,
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    var visible by remember { mutableStateOf(true) }
    // La chiave è `playing`: ripartendo i comandi si nascondono dopo l'attesa, mettendo in
    // pausa ricompaiono subito.
    LaunchedEffect(playing) {
        if (playing) {
            delay(CLIP_KEYS_LINGER)
            visible = false
        } else {
            visible = true
        }
    }
    // Il tocco che richiama i comandi è un riquadro trasparente sopra il video, che esiste solo
    // quando i comandi sono nascosti: quando ci sono, non ruba il tocco al tasto.
    if (!visible) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { visible = true }
        )
    }

    androidx.compose.animation.AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .size(CLIP_KEY)
                .clip(CircleShape)
                .background(CLIP_INK)
                // Il ruolo, o un lettore di schermo dice 'Riproduci' senza dire che è un tasto.
                .clickable(role = Role.Button, onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (playing) Glyphs.Material.Pause else Glyphs.Material.PlayArrow,
                contentDescription = stringResource(
                    if (playing) R.string.clip_pause else R.string.clip_play
                ),
                tint = Color.White,
                modifier = Modifier.size(CLIP_GLYPH)
            )
        }
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CLIP_INK)
                .padding(horizontal = CLIP_GAP, vertical = CLIP_LIP),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CLIP_LIP)
        ) {
            Text(
                text = Videos.stamp(position),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
            // ⚠️ Prima che il lettore la legga, la durata vale `C.TIME_UNSET`, un numero
            // negativo enorme: passata a [ValueSlider] darebbe un intervallo rovesciato, quindi
            // finché non si sa la barra non c'è.
            if (duration > 0) {
                ValueSlider(
                    value = position.coerceIn(0, duration).toFloat(),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = Videos.stamp(duration, floor = true),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Il tasto che apre il filmato: grande, perché è la sola cosa da toccare in tutta la vista. */
private val CLIP_KEY = 72.dp

/** Il triangolo dentro il tasto. */
private val CLIP_GLYPH = 40.dp

/** Il nero del tasto: lo stesso mestiere della targhetta in griglia, sopra un'immagine. */
private val CLIP_INK = Color(0x99000000)

/** Quanto i comandi si staccano dal bordo. */
private val CLIP_GAP = 12.dp

/** Il respiro sopra e sotto la barra, e il distacco fra i suoi pezzi. */
private val CLIP_LIP = 8.dp

/**
 * Quanto restano i comandi dopo che il filmato è partito.
 *
 * Due secondi e mezzo: abbastanza per vedere dov'è la barra, poco perché non dia fastidio.
 */
private const val CLIP_KEYS_LINGER = 2500L

/**
 * Quanto resta l'indicatore della manopola dopo l'ultimo ritocco del dito.
 *
 * Molto meno dei comandi: risponde a un gesto in corso, e più a lungo sembrerebbe un pannello.
 */
private const val KNOB_LINGER = 700L

/**
 * Quanta parte dell'altezza dello schermo copre l'intera corsa di una manopola.
 *
 * Il 70% e non tutto: il dito non parte dal bordo e non arriva all'altro, e con lo schermo
 * intero il massimo e il minimo vorrebbero due gesti.
 */
private const val KNOB_SPAN = 0.7f

/** L'arrotondamento dell'indicatore della manopola: la stessa aria dei comandi. */
private val KNOB_ROUND = 16.dp

/**
 * Ogni quanto si aggiorna il tempo scritto accanto alla barra.
 *
 * Mezzo secondo e non ogni fotogramma: il numero si legge in secondi.
 */
private const val TICK_MS = 500L

/**
 * La miniatura mentre l'immagine vera si decodifica.
 *
 * Aprire un'immagine da 30 megapixel richiede un tempo che si sente, a ogni passo della
 * strisciata; questa è la miniatura che la griglia ha già decodificato, quindi di solito è in
 * memoria e compare subito. Quando l'immagine è pronta la sostituisce.
 * Solo per gli indirizzi locali: per un URL remoto il telefono non ha miniature, e questo
 * caricatore non usa la rete.
 */
@Composable
private fun PreviewThumb(source: Uri?, settings: Settings, bar: BarSpace?) {
    val local = source?.scheme?.lowercase() == "content" || source?.scheme?.lowercase() == "file"
    if (source == null || !local) return
    Preview(source, settings, Modifier.fillMaxSize(), bar)
}

/**
 * Una miniatura disegnata grande quanto sarà l'immagine che la sostituisce.
 *
 * ⚠️ 'Ingrandisci le immagini piccole' è una regola sull'immagine, non sul suo sostituto:
 * applicata alla miniatura, una da 512 px restava 512 px e all'arrivo dell'immagine vera
 * raddoppiava di colpo.
 * La misura finale si calcola: [Pixels] dà il lato lungo del file, le proporzioni della
 * miniatura danno le due dimensioni, e `plannedSize` ne ricava la misura a riposo con la stessa
 * `restPlace` (`Fit.kt`) di `ImageCanvas`, così le due non divergono. Dedurla dalla sola
 * miniatura sbagliava per le immagini fra 512 px e la larghezza dello schermo.
 * Finché l'intestazione non è letta, e per sempre su ciò che non si riesce ad aprire, vale la
 * stima dalla miniatura. Si vede solo alla prima apertura: durante la strisciata la vicina ha
 * già letto la misura mentre entrava dal bordo.
 */
@Composable
private fun Preview(uri: Uri, settings: Settings, modifier: Modifier, bar: BarSpace? = null) {
    val context = LocalContext.current

    /*
     * ⚠️ Il sostituto letto in composizione toglie il lampeggio dell'immagine:
     * `AsyncImagePainter` parte da una coroutine e al primo fotogramma non disegna niente,
     * anche con l'immagine in memoria (verificato sul bytecode di Coil; il resto è accanto a
     * `Thumbs.note`). Va sotto il pittore vero, che poi disegna la stessa immagine alla stessa
     * scala: è una sovrapposizione, e non serve nessuna animazione.
     */
    val standIn = remember(uri, context) { Thumbs.cached(context, uri)?.asPainter(context) }

    val painter = rememberAsyncImagePainter(
        // ⚠️ La stessa richiesta della griglia, misura compresa, così l'immagine è già in
        // memoria: una misura diversa sarebbe un'altra chiave, e un'altra generazione.
        model = remember(uri, context) { Thumbs.request(context, uri) },
        // La chiave si registra qui, dove Coil la dà in `SuccessResult`: ricostruita a mano
        // si sbaglierebbe in silenzio il giorno in cui Coil cambia come la compone.
        onState = { st ->
            if (st is AsyncImagePainter.State.Success) Thumbs.note(uri, st.result.memoryCacheKey)
        }
    )

    // La misura si prende dal pittore vero se ce l'ha, se no dal sostituto: per un
    // fotogramma solo uno dei due la conosce, e dal solo pittore vero tornerebbe il salto.
    val seen = painter.intrinsicSize.takeIf { it.isSpecified }
        ?: standIn?.intrinsicSize
        ?: Size.Unspecified

    /*
     * Il lato lungo si legge dalla cache in composizione, e dal file solo se manca: durante la
     * strisciata la vicina l'ha già chiesto entrando dal bordo. L'effetto serve alla prima
     * apertura, dove costa la lettura di un'intestazione. Zero vuol dire 'non lo so', come
     * `Size.Unspecified`: un lato lungo di zero non è una misura possibile.
     */
    var longSide by remember(uri) { mutableIntStateOf(Pixels.known(uri) ?: 0) }
    /*
     * L'anteprima aspetta la risposta sulla misura, riuscita o no: senza, il ripiego qui sotto
     * per una miniatura piccola vale `Inside`, e l'immagine crescerebbe all'arrivo della misura.
     * L'attesa dura la lettura dell'intestazione, e con la misura in memoria non c'è.
     * ⚠️ Non era la causa dello zoom con le info spente: quella era lo stato della barra, che
     * partiva visibile (vedi `info` in `ViewerScreen`).
     */
    var asked by remember(uri) { mutableStateOf(longSide > 0) }
    LaunchedEffect(uri) {
        if (longSide == 0) longSide = Pixels.longSideOf(context, uri) ?: 0
        asked = true
    }

    val density = LocalDensity.current

    /*
     * La stessa vista di `ImageCanvas`, che misura allo stesso modo nello stesso contenitore:
     * così le due scale coincidono.
     */
    BoxWithConstraints(modifier = modifier) {
        val viewWidth = constraints.maxWidth.toFloat()
        val viewHeight = constraints.maxHeight.toFloat()
        val planned = plannedSize(
            seen = seen,
            longSide = longSide,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            settings = settings,
            density = density.density,
            bar = bar
        )

        // Il ripiego di quando la misura vera non si sa ancora: vedi la nota sulla funzione.
        val fromBig = seen.isSpecified &&
            (seen.width >= Thumbs.PX - 1f || seen.height >= Thumbs.PX - 1f)

        val scale = if (planned != null || settings.fitGrow || fromBig) {
            ContentScale.Fit
        } else {
            ContentScale.Inside
        }
        // Con la misura nota il riquadro ha già le proporzioni giuste, e `Fit` serve solo a non
        // deformare se l'arrotondamento in dp lo sposta di mezzo pixel.
        /*
         * Il riquadro è nella fascia di riposo e non al centro della vista: è dove `ImageCanvas`
         * metterà l'immagine, e un'anteprima altrove salterebbe all'arrivo. Senza misura nota la
         * fascia si ricava dalle proporzioni della miniatura.
         */
        val frame = when {
            planned != null -> Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, (planned.centre - planned.height / 2f).roundToInt()) }
                .size(
                    with(density) { planned.width.toDp() },
                    with(density) { planned.height.toDp() }
                )
            seen.isSpecified && bar != null -> {
                val room = restPlace(viewWidth, viewHeight, seen.width, seen.height, null, bar)
                Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, room.top.roundToInt()) }
                    .fillMaxWidth()
                    .height(with(density) { room.height.toDp() })
            }
            else -> Modifier.fillMaxSize()
        }

        if (asked) {
            standIn?.let {
                Image(
                    painter = it,
                    contentDescription = null,
                    contentScale = scale,
                    modifier = frame
                )
            }
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = scale,
                modifier = frame
            )
        }
    }
}

/**
 * Quanto sarà grande l'immagine quando arriverà, o `null` se non lo si può ancora dire.
 *
 * È la formula di `ImageCanvas` sui dati del file invece che su quelli del bitmap, e coincide
 * per algebra: là la misura vale `larghezzaBitmap * min(adattamento, campionamento * k)`, e
 * `larghezzaBitmap * campionamento` è la larghezza del file. Il campionamento sparisce, quindi
 * la risposta arriva prima che l'immagine sia aperta.
 * Il lato lungo viene dal file, che la miniatura non sa; le proporzioni dalla miniatura, che
 * l'intestazione sbaglia sulle immagini ruotate. Il perché è in [Pixels].
 */
private fun plannedSize(
    seen: Size,
    longSide: Int,
    viewWidth: Float,
    viewHeight: Float,
    settings: Settings,
    density: Float,
    bar: BarSpace?
): Planned? {
    if (longSide <= 0 || viewWidth <= 0f || viewHeight <= 0f) return null
    if (!seen.isSpecified || seen.width <= 0f || seen.height <= 0f) return null
    val ratio = seen.width / seen.height
    val wide = ratio >= 1f
    val pixelWidth = if (wide) longSide.toFloat() else longSide * ratio
    val pixelHeight = if (wide) longSide / ratio else longSide.toFloat()
    // Senza il campionamento, che qui non serve: vedi la nota sulla funzione.
    val oneToOne = if (settings.scaleMode == ScaleMode.PHYSICAL) 1f else density
    // La stessa `restPlace` di `ImageCanvas`, barra compresa: vedi `Fit.kt`.
    val place = restPlace(
        viewWidth, viewHeight, pixelWidth, pixelHeight,
        cap = if (settings.fitGrow) null else oneToOne,
        bar = bar
    )
    return Planned(pixelWidth * place.scale, pixelHeight * place.scale, place.centre)
}

/** La misura di [plannedSize] e il centro verticale in cui l'immagine riposerà. */
private data class Planned(val width: Float, val height: Float, val centre: Float)

/** Un pezzo nitido e il rettangolo, in coordinate viste, che occupa nell'immagine. */
private data class SharpTile(val bitmap: ImageBitmap, val area: PixelRect)

/** Il lettore di regioni una volta che si è provato ad aprirlo. Vedi il suo uso. */
private data class RegionHolder(val reader: RegionSource?)

/**
 * Che pezzo leggere adesso, con la geometria del visualizzatore: dove l'immagine finisce sullo
 * schermo lo dicono le stesse righe del `graphicsLayer` che la disegna e del `Canvas` che ci
 * posa sopra il pezzo.
 *
 * Il conto che sceglie il pezzo è [sharpAsk], in `Regions.kt`, perché lo usa anche l'editor
 * completo: due copie della stessa aritmetica divergerebbero al primo ritocco.
 */
private fun asked(
    reader: RegionSource,
    basePixels: Int,
    baseWidth: Float,
    baseHeight: Float,
    viewWidth: Float,
    viewHeight: Float,
    scale: Float,
    offset: Offset
): Sharpening {
    val midX = viewWidth / 2f + offset.x
    val midY = viewHeight / 2f + offset.y
    val drawn = RectF(
        midX - baseWidth / 2f * scale,
        midY - baseHeight / 2f * scale,
        midX + baseWidth / 2f * scale,
        midY + baseHeight / 2f * scale
    )
    return sharpAsk(reader.width, reader.height, basePixels, drawn, viewWidth, viewHeight)
}

/**
 * L'immagine vicina, durante la strisciata.
 *
 * Lo spostamento arriva come funzione e si legge dentro `graphicsLayer`: come valore, ogni
 * pixel di trascinamento ricomporrebbe questa funzione.
 * ⚠️ È la stessa [Preview] dell'anteprima: la vicina che scivola dentro e l'anteprima che
 * resta al centro sono lo stesso disegno in due istanti consecutivi, e scalate in modo diverso
 * farebbero saltare il cambio di pagina.
 */
@Composable
private fun Neighbour(uri: Uri?, settings: Settings, bar: BarSpace?, dx: () -> Float) {
    if (uri == null) return
    Preview(
        uri = uri,
        settings = settings,
        bar = bar,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationX = dx() }
    )
}

/**
 * A che punto è l'apertura.
 *
 * Determinato solo mentre si scarica, e a cento torna a girare: un'immagine dalla rete ha due
 * attese, il trasferimento, che si può contare, e la decodifica, che no. Un anello pieno fermo
 * durante la decodifica sembrerebbe bloccato.
 * Per un file locale non arriva nessun numero, e l'anello gira e basta.
 */
@Composable
private fun Progress(fraction: Float?, modifier: Modifier = Modifier) {
    if (fraction == null || fraction >= 1f) {
        CircularProgressIndicator(modifier)
        return
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(progress = { fraction })
        Text(
            text = "${(fraction * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * Quello che non si è aperto, e come ritentare.
 *
 * Il tasto serve per la rete (un `403` di passaggio, una galleria, un server lento): senza,
 * bisognerebbe tornare indietro e ritrovare l'indirizzo.
 */
@Composable
private fun ErrorMessage(
    state: ViewerState.Error,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(state.messageRes),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        state.detail?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        onRetry?.let {
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = it) { Text(stringResource(R.string.error_retry)) }
        }
    }
}

@Composable
private fun ImageCanvas(
    image: LoadedImage,
    settings: Settings,
    source: Uri?,
    folder: Folder.Lookup?,
    info: InfoBar,
    onStep: (Int) -> Unit,
    /** Le richieste del menu che devono sopravvivere al menu. Vedi [MenuOps]. */
    ops: MenuOps,
    /** Vedi il parametro omonimo di `ViewerScreen`. */
    inBin: Boolean,
    /**
     * Il fotogramma da disegnare al posto dell'immagine ferma, quando è animata; `null` per
     * un'immagine ferma.
     *
     * ⚠️ Sostituisce solo i pixel, non la geometria: zoom, panoramica, riquadro di riposo e
     * tasselli si calcolano su `image`, e i fotogrammi hanno la misura della sua tela.
     * Un'immagine di misura diversa sbaglierebbe quei conti in silenzio.
     */
    frame: ImageBitmap? = null,
    /**
     * Che cosa fa un tocco solo in mezzo allo schermo; di serie niente.
     *
     * Lo decide il chiamante: sopra un'immagine animata mette in pausa. Il ritardo che
     * comporta è spiegato su `onSingleTap` in [detectViewerGestures].
     */
    onSingleTap: () -> Unit = {},
    /**
     * Che cosa fare prima che il tocco lungo apra il menu; di serie niente.
     *
     * Serve a fermare un'animazione, che la tela non conosce: lo decide il chiamante, come per
     * [onSingleTap].
     */
    onHold: () -> Unit = {},
    /** La barra delle info sopra o sotto la tela, o `null` quando non c'è. Vedi [BarSpace]. */
    bar: BarSpace? = null
) {
    val density = LocalDensity.current
    // Vedi [withHaptics]: qui il gesto non passa da `combinedClickable`, quindi la
    // vibrazione del tocco lungo si chiama a mano.
    val haptics = LocalHapticFeedback.current

    // Lo sfondo si dipinge in `ViewerScreen`, contro il lampeggio: vedi la nota là.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val viewWidth = constraints.maxWidth.toFloat()
        val viewHeight = constraints.maxHeight.toFloat()
        val imageWidth = image.bitmap.width.toFloat()
        val imageHeight = image.bitmap.height.toFloat()

        /**
         * Di quanto il bitmap è più piccolo del file, cioè il campionamento all'apertura: 1 per
         * quasi tutte le immagini, 2 o 4 per quelle che non entravano intere in memoria.
         */
        val sampleFactor = (image.pixelWidth / imageWidth).coerceAtLeast(1f)

        /**
         * Che cosa vuol dire '100%' su questo schermo. Compose dispone l'immagine in pixel del
         * dispositivo, quindi la scala 1 è già un pixel del bitmap per pixel dello schermo:
         * [ScaleMode.PHYSICAL]. Un pixel per pixel di layout vuol dire moltiplicare per la
         * densità dello schermo, che su un telefono vale due o tre.
         *
         * ⚠️ Il tutto è moltiplicato per il campionamento: su un'immagine da 30 megapixel
         * campionata a metà, la scala 1 mostrava un quarto dei pixel veri e la riga dei dettagli
         * scriveva '100%'. I tasselli recuperano quei pixel, e il 100% deve mostrarli.
         */
        val oneToOne =
            sampleFactor * if (settings.scaleMode == ScaleMode.PHYSICAL) 1f else density.density

        /*
         * 'Adatta' vuol dire mostrata intera. Ingrandire un'immagine piccola fino a riempire lo
         * schermo è un'impostazione a parte e non il comportamento di serie: un'icona da 64 px
         * ingrandita non serve a nessuno.
         * Il riposo si calcola sullo spazio che la barra lascia libero (mockup `Viewer_NOPE`):
         * la regola, con la tolleranza del 5% in orizzontale, vive in `restPlace` (`Fit.kt`),
         * perché la usano anche l'anteprima e la vicina che entra dal bordo.
         */
        val place = restPlace(
            viewWidth, viewHeight, imageWidth, imageHeight,
            cap = if (settings.fitGrow) null else oneToOne,
            bar = bar
        )
        val restScale = place.scale

        // In verticale la fascia di riposo è quella di [place], non la vista intera: vedi
        // `clampAxis`, che senza barra usa la vista intera.
        fun clampOffset(candidate: Offset, atScale: Float): Offset = Offset(
            clampAxis(candidate.x, imageWidth * atScale, viewWidth, 0f, viewWidth),
            clampAxis(candidate.y, imageHeight * atScale, viewHeight, place.top, place.bottom)
        )

        /*
         * ⚠️ La chiave è la scala di riposo e non le impostazioni (riscontro dell'utente sulla
         * `1.81`): con `settings` intero, qualunque preferenza toccata azzerava lo zoom.
         * [restScale] riassume le sole cose che contano ('Ingrandisci le immagini piccole', il
         * metro del 100% e la misura della vista).
         * Al cambio di riposo, per esempio in una rotazione, si conserva l'ingrandimento rispetto
         * al riposo (voce `zoom-rotazione`): il rapporto e il punto inquadrato li tiene
         * [ZoomKeep], che sopravvive alla ricreazione dell'activity, e il valore di partenza è la
         * scala di riposo per l'ingrandimento di prima.
         * Il limite `zoomMax` si applica al ripristino e non alla nota presa, perché è
         * un'impostazione che può cambiare.
         */
        val kept = rememberZoomKeep(remember(image, source) { ImageActions.fileName(image, source) })
        var scale by remember(image, restScale) {
            mutableFloatStateOf(kept.scaleFor(restScale).coerceIn(MIN_SCALE, settings.zoomMax))
        }
        // Anche la fascia è una chiave: su un'immagine limitata dalla larghezza la scala non
        // cambia quando la barra si nasconde, ma il centro di riposo sì.
        var offset by remember(image, restScale, place.top, place.bottom) {
            mutableStateOf(clampOffset(kept.offsetFor(scale, imageWidth, imageHeight), scale))
        }
        /*
         * La nota si prende con un flusso e non con un `LaunchedEffect` per valore: lo
         * scostamento cambia a ogni fotogramma di una panoramica, e come chiave rifarebbe la
         * coroutine sessanta volte al secondo. Come per la percentuale, più sotto.
         */
        LaunchedEffect(kept, restScale, imageWidth, imageHeight) {
            snapshotFlow { scale to offset }.collect { (atScale, atOffset) ->
                kept.record(atScale, atOffset, restScale, imageWidth, imageHeight)
            }
        }

        // La riga torna visibile, secondo l'impostazione, a ogni immagine nuova: nasconderla è
        // una decisione su questa immagine.
        // ⚠️ L'esito dei tasselli si azzera con lei: la riga sopravvive al cambio di immagine,
        // e mostrerebbe l'esito della precedente mentre il lettore si riapre.
        // La chiave è `settings.infoVisible` e non `settings` intero, o la barra riapparirebbe a
        // ogni preferenza toccata.
        LaunchedEffect(image, settings.infoVisible) {
            info.visible = settings.infoVisible
            info.tiles = null
        }

        /*
         * La percentuale esce con un flusso e non con una scrittura in composizione: `scale`
         * cambia a ogni fotogramma di una pinza, e scriverne il derivato mentre si compone
         * rifarebbe la composizione da capo. Così si aggiorna solo il testo che la mostra.
         */
        LaunchedEffect(oneToOne) {
            snapshotFlow { scale }.collect { info.percent = it / oneToOne }
        }
        val menu = rememberMenuState(image)
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        /**
         * Di quanto l'immagine è stata trascinata di lato, in pixel: la figura lo segue e la
         * vicina entra dal bordo.
         *
         * La chiave è la sola `image`: si azzera quando arriva l'immagine nuova, e nessuna
         * impostazione deve azzerare un gesto in corso.
         */
        var travel by remember(image) { mutableFloatStateOf(0f) }

        /**
         * Se una pagina sta andando a destinazione.
         *
         * ⚠️ Impedisce una seconda strisciata mentre la prima arriva: i due movimenti
         * scriverebbero lo stesso numero, e un gesto solo salterebbe due immagini. Si legge
         * quando il dito scende, come le altre condizioni della strisciata.
         */
        var settling by remember(image) { mutableStateOf(false) }

        val pageGap = with(density) { PAGE_GAP.toPx() }
        val series = folder?.seriesOrNull
        /*
         * ⚠️ Nell'ordine di lettura, lo stesso di `onStep`: la successiva è quella che arriva
         * strisciando verso sinistra, e che `onStep(1)` aprirà. Con 'Sfoglia solo le immagini'
         * le vicine saltano i filmati come il passo: la regola di salto è una sola, su
         * `Folder.Series`, o entrerebbe un filmato e ne comparirebbe un altro.
         * Con soli filmati da quella parte `toward` risponde `null`, e il gesto frena come
         * all'ultima immagine.
         */
        val skipVideos = settings.imagesOnly
        val nextUri = series?.toward(1, skipVideos)
        val prevUri = series?.toward(-1, skipVideos)

        // ⚠️ Le due vicine si chiedono subito: una miniatura generata nell'istante del gesto
        // arriverebbe dopo, e si vedrebbe entrare un rettangolo vuoto. In `Ready` l'immagine
        // grande è già decodificata, quindi non si toglie niente a quella; le miniature servono
        // alla vicina che scorre e alla `PreviewThumb` della pagina successiva.
        LaunchedEffect(nextUri, prevUri) {
            val loader = SingletonImageLoader.get(context)
            listOfNotNull(nextUri, prevUri).forEach { loader.execute(Thumbs.request(context, it)) }
        }

        /**
         * Se una pagina si sta muovendo, per accendere le vicine solo mentre servono.
         *
         * ⚠️ `derivedStateOf` e non `travel` letto direttamente: `travel` cambia a ogni
         * fotogramma, e la composizione si rifarebbe sessanta volte al secondo; così si
         * ricompone due volte per gesto, e il movimento passa dai `graphicsLayer`.
         * Fra le chiavi c'è `image`, come per `travel`: senza, il calcolo resterebbe legato
         * allo stato dell'immagine precedente, fermo al suo ultimo valore, e le vicine non si
         * spegnerebbero più.
         */
        val dragging by remember(image) { derivedStateOf { travel != 0f } }

        /**
         * La lettura a pezzi dell'immagine, quando serve e si può.
         *
         * Quasi sempre il lettore è `null`: `open` rinuncia se l'immagine non era campionata,
         * cioè se è entrata intera in memoria e non c'è dettaglio da recuperare.
         * ⚠️ È in un involucro perché `null` deve dire 'sto ancora aprendo', mentre l'apertura
         * fallita restituisce anch'essa `null`: senza la distinzione la riga dei dettagli
         * accuserebbe il formato mentre il file si apre.
         */
        val regions by produceState<RegionHolder?>(null, image, source) {
            val opened = RegionSource.open(context, source, image)
            value = RegionHolder(opened)
            awaitDispose { opened?.close() }
        }

        /**
         * Il pezzo a piena risoluzione disegnato sopra la figura, quando c'è.
         *
         * La chiave è la sola `image`, come per [travel]: con `settings` un'impostazione scritta
         * mentre si guarda toglieva il pezzo nitido finché non ci si muoveva.
         */
        var sharp by remember(image) { mutableStateOf<SharpTile?>(null) }

        /*
         * Il pezzo nitido si legge quando il dito si ferma: a ogni fotogramma di una pinza si
         * leggerebbe dal file sessanta volte al secondo. Finché ci si muove si vede il bitmap
         * di base ingrandito, e [TILE_DELAY_MS] dopo l'ultimo movimento arriva la nitidezza.
         * `collectLatest` più `delay` è l'attesa: ogni movimento annulla il conto alla rovescia
         * precedente e la decodifica che stava partendo.
         */
        LaunchedEffect(regions, viewWidth, viewHeight) {
            // Ancora in apertura: non si dice niente, perché non si sa ancora niente.
            val reader = (regions ?: return@LaunchedEffect).reader
            if (reader == null) {
                sharp = null
                // Il formato o l'orientamento: `RegionSource.open` rinuncia quando non sa
                // rileggere il file e quando le misure grezze non concordano col tag EXIF. Da
                // qui i due casi non si distinguono.
                info.tiles = if (image.sampled) "no tile: format" else null
                return@LaunchedEffect
            }
            snapshotFlow { scale to offset }.collectLatest { (atScale, atOffset) ->
                delay(TILE_DELAY_MS)
                // I pixel del bitmap di base dicono quanto dettaglio c'è già, la sua misura su
                // schermo dove è finita l'immagine: coincidono solo al 100%.
                when (
                    val ask = asked(
                        reader, image.bitmap.width, imageWidth, imageHeight,
                        viewWidth, viewHeight, atScale, atOffset
                    )
                ) {
                    is Sharpening.Read -> {
                        val tile = reader.tile(ask.area, ask.sample)
                        sharp = tile?.let { SharpTile(it.asImageBitmap(), ask.area) }
                        info.tiles = if (tile != null) "sharp" else "no tile: read"
                    }
                    is Sharpening.None -> {
                        sharp = null
                        info.tiles = ask.why
                    }
                }
            }
        }

        /**
         * Porta la pagina a destinazione, o la riporta al suo posto.
         *
         * Senza spostamento si esce subito: un trascinamento verticale passa di qui con lo
         * spostamento a zero, e bloccherebbe la strisciata per i [SNAP_MS] dell'animazione.
         * ⚠️ `settling` si spegne da sé solo nel ritorno al posto; negli altri casi `onStep`
         * ricarica il visualizzatore e scarta questo stato. La pagina non resta fuori schermo
         * perché `reachable` legge la stessa serie che `onStep` conta, e quella serie sotto un
         * `ImageCanvas` attivo non cambia: chi la riscrive passa da `state = Loading`, che
         * smonta questa schermata.
         */
        fun settle(step: Int) {
            if (step == 0 && travel == 0f) return
            settling = true
            scope.launch {
                val target = when {
                    step > 0 -> -(viewWidth + pageGap)
                    step < 0 -> viewWidth + pageGap
                    else -> 0f
                }
                Animatable(travel).animateTo(target, tween(SNAP_MS)) { travel = value }
                // ⚠️ Il passo si chiede dopo l'animazione: prima, l'immagine nuova arriverebbe
                // mentre la vecchia scivola ancora via, e se ne vedrebbero due sovrapposte.
                if (step == 0) settling = false else onStep(step)
            }
        }

        /**
         * Lo scivolamento in corso, e `null` quando la figura è ferma.
         *
         * ⚠️ Si annulla appena un dito torna giù: chi appoggia il dito su un'immagine che
         * scivola vuole fermarla, e due scrittori sullo stesso `offset` darebbero uno strappo.
         * L'annullamento è in [zoomAround] e [animateTo], i due punti da cui `offset` cambia.
         * La chiave è la sola `image`: con `settings` un'impostazione scritta mentre la figura
         * scivola ne perdeva il riferimento, e [stopGlide] non poteva più fermarla.
         */
        var gliding by remember(image) { mutableStateOf<Job?>(null) }

        fun stopGlide() {
            gliding?.cancel()
            gliding = null
        }

        // La curva e la soglia si preparano una volta: la prima dipende dalla densità, la
        // seconda è una velocità in dp al secondo da tradurre in pixel.
        val decay = remember(density) { splineBasedDecay<Offset>(density) }
        val glideFloor = with(density) { GLIDE_FLOOR.toPx() }

        /** Cambia scala intorno a un punto dello schermo, tenendo fermo quello che c'è sotto. */
        fun zoomAround(anchor: Offset, next: Float, pan: Offset = Offset.Zero) {
            stopGlide()
            val clamped = next.coerceIn(MIN_SCALE, settings.zoomMax)
            val fromCentre = anchor - Offset(viewWidth / 2f, viewHeight / 2f)
            val corrected = fromCentre - (fromCentre - offset) * (clamped / scale)
            scale = clamped
            offset = clampOffset(corrected + pan, clamped)
        }

        /*
         * La specifica dell'animazione non si scrive, ed è voluto (censimento della UI del
         * 2026-09-05): la molla di serie di `animateTo` si interrompe e riprende a metà, come
         * serve a uno zoom in cui un secondo doppio tocco arriva durante il primo. La regola di
         * `ActionPad.kt` vieta di scrivere due volte lo stesso movimento, non di usare un valore
         * di serie, e copiare qui i numeri della molla creerebbe proprio una copia.
         * Si dichiarano i movimenti chiesti dall'utente: la strisciata ha [SNAP_MS], e lo
         * scivolamento dopo un trascinamento ha il suo decadimento.
         */
        fun animateTo(target: Float) {
            stopGlide()
            scope.launch {
                val from = scale
                val animation = Animatable(from)
                // Intorno al centro della fascia libera: vedi `zoomOffset`.
                val pivot = place.centre - viewHeight / 2f
                val start = offset
                animation.animateTo(target.coerceIn(MIN_SCALE, settings.zoomMax)) {
                    scale = value
                    offset = clampOffset(
                        Offset(start.x * (value / from), zoomOffset(start.y, pivot, value / from)),
                        value
                    )
                }
            }
        }

        /**
         * L'inerzia della panoramica: al distacco la figura continua e frena da sé (richiesta
         * dell'utente).
         *
         * La curva di frenata è quella del sistema (`splineBasedDecay`), la stessa dello
         * scorrimento di ogni elenco di Android, e non ha numeri da tarare.
         * Si ferma appena il bordo ferma la figura: `clampOffset` la tiene dentro, e
         * l'animazione girerebbe per il suo secondo intero senza cambiare niente.
         * Sotto [GLIDE_FLOOR] non parte: un dito che si alza da fermo lascia una velocità
         * piccola, e uno scivolamento di due pixel sembrerebbe un sussulto.
         */
        fun glideBy(velocity: Velocity) {
            val speed = Offset(velocity.x, velocity.y)
            if (speed.getDistance() < glideFloor) return
            stopGlide()
            gliding = scope.launch {
                // `AnimationState` e non `Animatable`, per potersi fermare al bordo: il blocco
                // di `AnimationState` ha per ricevitore un `AnimationScope`, che ha
                // `cancelAnimation`.
                AnimationState(Offset.VectorConverter, offset, speed).animateDecay(decay) {
                    val next = clampOffset(value, scale)
                    if (next == offset) cancelAnimation() else offset = next
                }
            }
        }

        // ⚠️ `requiredSize` e non `size`: `size` negozia col genitore (`SizeNode.measure`
        // applica i vincoli in ingresso quando `enforceIncoming` è vero, verificato nel bytecode
        // di `foundation-layout`), quindi un'immagine più grande della vista era ricondotta alla
        // misura della vista e poi `graphicsLayer` applicava la scala una seconda volta. Le
        // immagini grandi si aprivano piccole del fattore di adattamento, e nemmeno il 100%
        // mostrava un pixel per pixel.
        // Il ritaglio resta: il `BoxWithConstraints` qui sopra ha `clipToBounds`.
        Image(
            bitmap = frame ?: image.bitmap,
            contentDescription = image.displayName,
            modifier = Modifier
                .align(Alignment.Center)
                .requiredSize(with(density) { imageWidth.toDp() }, with(density) { imageHeight.toDp() })
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    // Lo spostamento della strisciata si somma a quello della panoramica: la
                    // strisciata esiste solo a riposo, dove `offset` è zero, ma se i due stati
                    // si sovrapponessero la figura non salterebbe.
                    translationX = offset.x + travel
                    translationY = offset.y
                }
        )

        /*
         * Il tassello si disegna sopra la figura, non al suo posto: sotto c'è sempre l'immagine
         * intera, e un tassello fuori posto sarebbe un rettangolo spostato, non un'immagine
         * mancante. La posizione si ricalcola nel disegno, con la formula del `graphicsLayer`
         * qui sopra, `travel` compreso: il pezzo segue la figura senza ricomporre niente.
         */
        sharp?.let { tile ->
            Canvas(modifier = Modifier.matchParentSize()) {
                val perPixel = imageWidth / image.pixelWidth.toFloat()
                val midX = viewWidth / 2f + offset.x + travel
                val midY = viewHeight / 2f + offset.y
                val left = midX + (tile.area.left * perPixel - imageWidth / 2f) * scale
                val top = midY + (tile.area.top * perPixel - imageHeight / 2f) * scale
                val right = midX + (tile.area.right * perPixel - imageWidth / 2f) * scale
                val bottom = midY + (tile.area.bottom * perPixel - imageHeight / 2f) * scale
                drawImage(
                    image = tile.bitmap,
                    dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                    dstSize = IntSize((right - left).roundToInt(), (bottom - top).roundToInt())
                )
            }
        }

        // Le vicine esistono solo mentre si trascina: a riposo costerebbero due immagini per
        // ogni immagine guardata. Sono le miniature della griglia e della `PreviewThumb`, già in
        // memoria, e l'immagine vera arriva quando la pagina è girata.
        if (dragging) {
            Neighbour(nextUri, settings, bar) { travel + viewWidth + pageGap }
            Neighbour(prevUri, settings, bar) { travel - viewWidth - pageGap }
        }

        // A riposo la figura non ha gioco da trascinare, quindi una strisciata orizzontale può
        // cambiare immagine; ingrandita, serve a spostarsi dentro l'immagine.
        val atRest = abs(scale - restScale) < 0.01f

        /**
         * Se la strisciata è ammessa, letto quando il dito scende.
         *
         * ⚠️ Uno stato che cambia durante un gesto non può essere una chiave di `pointerInput`:
         * `SuspendingPointerInputModifierNodeImpl.update$ui` confronta le chiavi e, se
         * cambiano, chiama `resetPointerInputHandler()`, cioè annulla il gesto in corso
         * (verificato sul bytecode di `compose.ui`). Con `atRest` come chiave ogni pinza si
         * interrompeva attraversando la banda dell'adattamento; con `folder`, quando la ricerca
         * della cartella rispondeva subito dopo l'apertura.
         * Letto una volta per gesto, così una panoramica che entra nella banda dell'adattamento
         * resta una panoramica.
         */
        val swipeAllowed by rememberUpdatedState(atRest && folder?.seriesOrNull != null)

        /*
         * Le chiavi dei due rilevatori: i valori della composizione che le loro funzioni leggono
         * (la misura della vista, il riposo, il 100%, il tetto dello zoom e le due vicine), e non
         * `settings` intero, per la ragione di [travel]: un'impostazione scritta mentre si guarda
         * (`clipboardOpened`, scritta subito dopo un'apertura dagli appunti) azzerava il gesto in
         * corso, e una pinza si interrompeva a metà.
         */
        val gestureKeys = arrayOf(
            image, viewWidth, viewHeight, place, oneToOne, settings.zoomMax, nextUri, prevUri
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(*gestureKeys) {
                    // ⚠️ Un rilevatore solo per panoramica, pinza e strisciata: nascono tutte
                    // da un dito che si muove, e su due modificatori si contendono il gesto.
                    // Vedi [detectPanZoomOrSwipe].
                    detectPanZoomOrSwipe(
                        // La strisciata esiste solo a riposo e con una serie.
                        // ⚠️ `settling` si legge qui e non in `swipeAllowed`, che è fissato
                        // dalla composizione: fra la partenza della pagina e la composizione
                        // successiva un dito può scendere.
                        swipeEnabled = { swipeAllowed && !settling },
                        // La soglia è una frazione della larghezza: un valore fisso in dp
                        // sarebbe mezza schermata su un telefono stretto e niente su un
                        // tablet. Presa da `size` del rilevatore, perché `viewWidth` sarebbe
                        // catturata e dopo una rotazione resterebbe vecchia.
                        swipeThreshold = { size.width / 5f },
                        onTransform = { centroid, pan, zoom ->
                            zoomAround(centroid, scale * zoom, pan)
                        },
                        onPanEnd = { glideBy(it) },
                        // La resistenza al capolinea: alla prima o all'ultima immagine la
                        // pagina si lascia tirare a [END_RESISTANCE] e poi torna al suo posto.
                        onSwipeDrag = { raw ->
                            val reachable = if (raw < 0f) nextUri != null else prevUri != null
                            travel = if (reachable) raw else raw * END_RESISTANCE
                        },
                        onSwipeEnd = { step ->
                            val reachable = when {
                                step > 0 -> nextUri != null
                                step < 0 -> prevUri != null
                                else -> false
                            }
                            settle(if (reachable) step else 0)
                        }
                    )
                }
                .pointerInput(*gestureKeys) {
                    detectViewerGestures(
                        // Anche questo vibra (vedi [withHaptics]): il rilevatore è scritto qui
                        // e non è `combinedClickable`, quindi la vibrazione si chiama a mano.
                        onLongPress = {
                            haptics.performHapticFeedback(HOLD_BUZZ)
                            onHold()
                            menu.open()
                        },
                        /*
                         * Su un'immagine animata un tocco in mezzo allo schermo mette in pausa o
                         * fa ripartire (richiesta dell'utente); l'icona della fila legge lo
                         * stesso `playing`. Su un'immagine ferma non fa niente, perché
                         * `animation` è `null`.
                         */
                        onSingleTap = { onSingleTap() },
                        onDoubleTap = {
                            // Due stati soli, come nel visualizzatore da desktop: intera, o un
                            // pixel del file per pixel dello schermo.
                            animateTo(if (abs(scale - restScale) < 0.01f) oneToOne else restScale)
                        },
                        onZoomDrag = { anchor, dy ->
                            zoomAround(anchor, scale * exp(dy * DRAG_ZOOM_SENSITIVITY))
                        }
                    )
                }
        )

        /*
         * Il menu compare sempre al centro dello schermo (richiesta dell'utente) e si posiziona
         * da sé sulla finestra: non serve un'ancora, e la posizione del tocco non si conserva.
         * La condizione è `visible` e non `wanted`: qui dentro c'è l'animazione dell'uscita, e
         * il menu deve restare finché il pannello sbiadisce. Vedi `MenuShell`.
         */
        if (menu.visible) {
            ImageMenu(
                image = image,
                source = source,
                menu = menu,
                onZoom = { animateTo(it) },
                oneToOne = oneToOne,
                restScale = restScale,
                ops = ops,
                inBin = inBin,
                zoomRows = settings.zoomInMenu,
                binOn = settings.binOn
            )
        }

    }
}

/**
 * Panoramica, pinza e strisciata orizzontale, decise dallo stesso rilevatore.
 *
 * ⚠️ Su due modificatori la strisciata non funziona: i due gesti nascono dallo stesso dito,
 * e chi supera per primo la soglia di movimento consuma le variazioni. A perdere è sempre la
 * strisciata, perché `detectTransformGestures` guarda il modulo dello spostamento e
 * `detectHorizontalDragGestures` la sola X: a 10 gradi il primo scatta a 24 px, il secondo ne
 * vuole 24,37. `awaitPointerSlopOrCancellation` risponde `null` a una variazione consumata, e
 * controlla anche nel passaggio Final, quindi nemmeno scambiare l'ordine dei modificatori
 * basterebbe (verificato sul sorgente di Compose).
 *
 * Il corpo è `detectTransformGestures` con un ramo in più: con un dito solo e la strisciata
 * ammessa, lo spostamento orizzontale muove la pagina intera, e al distacco si decide se girarla
 * o riportarla al suo posto.
 * - Lo spostamento esce a ogni evento (`onSwipeDrag`), e la figura segue il dito; con un solo
 *   esito finale il dito trascinerebbe il vuoto e l'immagine cambierebbe di scatto.
 * - ⚠️ L'esito esce sempre quando si è trascinato, anche con il gesto annullato o sotto la
 *   soglia: senza, la pagina resterebbe a metà schermo.
 * - Un secondo dito annulla la strisciata, velocità compresa: chi appoggia il pollice per
 *   pizzicare non chiede l'immagine successiva.
 */
private suspend fun PointerInputScope.detectPanZoomOrSwipe(
    swipeEnabled: () -> Boolean,
    swipeThreshold: () -> Float,
    onTransform: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    /**
     * Il dito si è alzato dopo una panoramica, e con quale velocità in pixel al secondo: serve
     * all'inerzia.
     *
     * Solo per un dito solo: dopo una pinza nessuno si aspetta che l'immagine continui, e il
     * centroide che passa da due dita a una salta, inventando una velocità.
     */
    onPanEnd: (Velocity) -> Unit,
    onSwipeDrag: (Float) -> Unit,
    onSwipeEnd: (Int) -> Unit
) {
    awaitEachGesture {
        val slop = viewConfiguration.touchSlop
        val fling = FLING_VELOCITY.toPx()
        var zoomAcc = 1f
        var panAcc = Offset.Zero
        var past = false
        var multi = false
        var travel = 0f
        var dragged = false
        // La velocità serve al colpo di pollice corto e rapido, che non arriverebbe alla
        // soglia per distanza. Si misura sul dito che ha cominciato.
        val speed = VelocityTracker()
        // Un secondo misuratore per la panoramica dentro la figura: una strisciata annullata
        // dal secondo dito azzera il primo, e la panoramica successiva non deve ereditarne la
        // velocità.
        val slide = VelocityTracker()
        var panned = false

        val down = awaitFirstDown(requireUnconsumed = false)
        // ⚠️ Le due condizioni si leggono qui, a dito appena sceso: fuori sarebbero valori
        // catturati alla creazione del rilevatore. Vedi `swipeAllowed`.
        val canSwipe = swipeEnabled()
        val threshold = swipeThreshold()
        var canceled: Boolean
        do {
            val event = awaitPointerEvent()
            // Una variazione già consumata è di qualcun altro: qui c'è solo il
            // rilevatore dei tocchi, che consuma nella sua trascinata di zoom.
            canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                if (event.changes.count { it.pressed } > 1 && !multi) {
                    multi = true
                    travel = 0f
                    // Col secondo dito la pagina torna al suo posto. ⚠️ `dragged` torna falso:
                    // se restasse vero, al distacco la velocità di prima della pinza
                    // girerebbe la pagina.
                    if (dragged) onSwipeDrag(0f)
                    dragged = false
                }
                val swiping = canSwipe && !multi
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()

                if (!past) {
                    zoomAcc *= zoomChange
                    panAcc += panChange
                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    if (abs(1 - zoomAcc) * centroidSize > slop || panAcc.getDistance() > slop) {
                        past = true
                    }
                }

                if (past) {
                    if (swiping) {
                        travel += panChange.x
                        dragged = true
                        event.changes.firstOrNull { it.id == down.id }?.let {
                            speed.addPosition(it.uptimeMillis, it.position)
                        }
                        onSwipeDrag(travel)
                    } else if (zoomChange != 1f || panChange != Offset.Zero) {
                        onTransform(event.calculateCentroid(useCurrent = false), panChange, zoomChange)
                        // La posizione si misura solo con un dito: con due il centroide salta
                        // quando uno si alza, e sembrerebbe un colpo veloce. Con due dita si
                        // azzera, così una pinza non lascia inerzia.
                        val alone = event.changes.singleOrNull { it.pressed }
                        if (alone != null) {
                            slide.addPosition(alone.uptimeMillis, alone.position)
                            panned = true
                        } else {
                            slide.resetTracking()
                            panned = false
                        }
                    }
                    // Si consuma in tutti e due i casi: è quello che dice al
                    // rilevatore dei tocchi che questo dito sta trascinando e non
                    // chiedendo il menu.
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })

        if (!dragged) {
            // Senza strisciata il gesto finisce qui: dopo una panoramica l'inerzia parte al
            // distacco, e un gesto annullato da un altro rilevatore non ne lascia.
            if (panned && !canceled) onPanEnd(slide.calculateVelocity())
            return@awaitEachGesture
        }
        val vx = speed.calculateVelocity().x
        val step = when {
            canceled -> 0
            travel <= -threshold || vx <= -fling -> 1
            travel >= threshold || vx >= fling -> -1
            else -> 0
        }
        onSwipeEnd(step)
    }
}

/**
 * Tocco lungo, doppio tocco e zoom a una mano, in un rilevatore solo.
 *
 * Il tocco singolo non mostra né nasconde i dettagli (richiesta dell'utente): su un
 * visualizzatore ogni tocco è anche un modo di toccare l'immagine, e un pannello che compare
 * sotto il pollice sembrerebbe un difetto. Lo usa solo [onSingleTap].
 *
 * Scritto a mano perché Compose non ha un rilevatore di doppio tocco seguito da trascinamento:
 * `detectTapGestures` segnala il doppio tocco quando il secondo dito si alza, troppo tardi per
 * cominciare un trascinamento.
 * ⚠️ Un secondo dito interrompe il riconoscimento: senza, una pinza lenta tenuta mezzo secondo
 * aprirebbe il menu. E il tocco lungo vuole un dito fermo: oltre la soglia di tocco il dito sta
 * facendo una panoramica (vedi la nota della prima fase).
 */
private suspend fun PointerInputScope.detectViewerGestures(
    onLongPress: (Offset) -> Unit,
    /**
     * Un tocco solo, che non è diventato né un doppio tocco né altro.
     *
     * ⚠️ Arriva dopo la finestra del doppio tocco (`doubleTapTimeoutMillis`, qualche decimo di
     * secondo): finché è aperta, un tocco solo e il primo di due sono indistinguibili, e
     * chiamarlo subito metterebbe in pausa a ogni doppio tocco.
     */
    onSingleTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit,
    onZoomDrag: (anchor: Offset, dy: Float) -> Unit
) {
    awaitEachGesture {
        val first = awaitFirstDown(requireUnconsumed = false)

        // Prima fase: il primo dito si alza prima della scadenza del tocco lungo?
        //
        // ⚠️ Un dito che si muove non è un tocco lungo, e la distanza si controlla qui. La riga
        // su `isConsumed` qui sotto non basta: sul telefono il menu si apriva in mezzo a una
        // panoramica (segnalazione dell'utente), perché la panoramica è su un altro
        // modificatore e se le sue variazioni arrivino già consumate dipende dall'ordine e dai
        // tempi. Il controllo sulla distanza non dipende da niente di questo.
        var abandoned = false
        val lifted = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var up = false
            while (!up && !abandoned) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == first.id }
                when {
                    event.changes.count { it.pressed } > 1 -> abandoned = true
                    change == null || change.isConsumed -> abandoned = true
                    // La distanza si misura dal punto in cui il dito si è posato, non
                    // fotogramma per fotogramma: anche una deriva lenta è un trascinamento.
                    (change.position - first.position).getDistance() >
                        viewConfiguration.touchSlop -> abandoned = true
                    !change.pressed -> up = true
                }
            }
            up
        }
        if (abandoned) return@awaitEachGesture
        if (lifted == null) {
            onLongPress(first.position)
            return@awaitEachGesture
        }

        // Seconda fase: un secondo dito entro la finestra del doppio tocco, o niente.
        val second = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
            awaitFirstDown(requireUnconsumed = false)
        } ?: run {
            onSingleTap(first.position)
            return@awaitEachGesture
        }

        // Terza fase: è un doppio tocco finché il dito non si muove, poi uno zoom. Il punto
        // fermo resta dove è sceso il secondo tocco per tutto il trascinamento, così l'immagine
        // cresce intorno al punto scelto e non dove il pollice si è spostato.
        var dragging = false
        var travelled = 0f
        while (true) {
            val event = awaitPointerEvent()
            if (event.changes.count { it.pressed } > 1) break
            val change = event.changes.firstOrNull { it.id == second.id } ?: break
            if (!change.pressed) {
                if (!dragging) onDoubleTap(second.position)
                break
            }
            val step = change.position.y - change.previousPosition.y
            travelled += step
            if (!dragging && abs(travelled) > viewConfiguration.touchSlop) dragging = true
            if (dragging) {
                onZoomDrag(second.position, step)
                change.consume()
            }
        }
    }
}

/**
 * Il menu che apre il tocco lungo.
 *
 * L'ordine è quello scelto dall'utente dopo aver usato la prima versione: prima quello che si fa
 * con l'immagine, poi quello che si fa alla vista.
 * In fondo ci sono le sei operazioni sui file, come icone su due righe di tre (richiesta
 * dell'utente): sono lo stesso [ActionPad] della selezione nella griglia, quindi icone e ordine
 * si decidono in un posto solo. 'Condividi' è nel riquadro e non fra le voci di testo, per non
 * averla due volte. 'Copia immagine' è un'altra cosa da 'Copia': la prima mette l'immagine negli
 * appunti, la seconda copia il file in un'altra cartella.
 */
@Composable
private fun ImageMenu(
    image: LoadedImage,
    source: Uri?,
    /** Lo stato del menu: da qui si chiude, e da qui la superficie sa quando uscire. */
    menu: MenuState,
    onZoom: (Float) -> Unit,
    oneToOne: Float,
    restScale: Float,
    ops: MenuOps,
    inBin: Boolean,
    /** Se il menu ha anche 'Adatta alla vista' e '100%'. Vedi `Settings.zoomInMenu`. */
    zoomRows: Boolean,
    /**
     * Se il cestino è acceso: decide se 'elimina' sposta o cancella, e con lui se ci sia una
     * conferma.
     *
     * Arriva come booleano e non con le impostazioni intere, o il menu si ricomporrebbe a ogni
     * ritocco di una voce che non lo riguarda. È la stessa scelta di `factFields` nella griglia.
     */
    binOn: Boolean
) {
    val context = LocalContext.current

    /*
     * La superficie è condivisa col menu della selezione: riquadro, ombra, animazione e margine
     * sopra e sotto le voci vivono in [MenuShell], e qui restano posizione e voci.
     */
    MenuShell(state = menu, position = MenuInWindow) {
        Column(modifier = Modifier.width(PAD_MENU_WIDTH)) {
            /*
             * Ogni voce ha la sua icona (scelta dell'utente sul mockup), e così i testi
             * cominciano tutti nello stesso punto senza un rientro scritto a mano.
             * 'Copia immagine' e 'Copia' si distinguono dal disegno (riscontro dell'utente):
             * questa ha due fogli con montagne e sole ([Glyphs.PhotoPair]), l'altra due cartelle.
             * Gli appunti passano da [MenuOps] (rilievi C40 ed E31): la PNG di un'immagine grande
             * richiede secondi, e fatta nel tocco bloccava l'app.
             */
            MenuRow(
                text = stringResource(R.string.menu_copy_image),
                icon = Glyphs.PhotoPair,
                onTap = {
                    menu.close()
                    ops.copy(image)
                }
            )
            /*
             * Lo schema dell'indirizzo decide due voci, quindi si legge una volta qui: dice se
             * l'immagine è un file del telefono (`content://` o `file://`) o viene dal web.
             */
            val scheme = source?.scheme?.lowercase()
            /*
             * 'Modifica' compare solo su un file del telefono, al contrario di 'Scarica' qui
             * sotto: modificare vuol dire riscrivere un file, e un'immagine arrivata dal web non
             * ne ha uno. L'icona, una matita su un'immagine, l'ha disegnata l'utente.
             * Il tocco lungo fa scegliere l'app, e la scelta resta memorizzata (richiesta
             * dell'utente): è lo stesso gesto del tocco lungo su 'Copia', che duplica. Tutte le
             * voci sono [MenuRow], e si allineano per costruzione.
             */
            if (scheme == "content" || scheme == "file") {
                MenuRow(
                    text = stringResource(R.string.menu_edit),
                    icon = Glyphs.ImageEdit,
                    onTap = { menu.close(); ops.edit() },
                    // AIV Play ha solo i suoi editor: non c'è un'app da scegliere (`Store.files`).
                    holdLabel = if (Store.files) stringResource(R.string.menu_edit_hold) else null,
                    onHold = if (Store.files) { { menu.close(); ops.editWith() } } else null
                )
            }
            // Accanto a 'Modifica': sono le due voci che producono un file dall'immagine in
            // vista, una riscrivendolo e l'altra affiancandolo.
            MenuRow(
                text = stringResource(R.string.menu_convert),
                icon = Glyphs.ImageConvert,
                onTap = { menu.close(); ops.convert() }
            )

            /*
             * 'Scarica' non compare su un file locale (richiesta dell'utente): farebbe un
             * doppione di un'immagine che è già nella galleria. Il controllo è sullo schema:
             * senza indirizzo (`null`) la voce resta, perché l'immagine non è un file del
             * telefono.
             */
            if (scheme != "content" && scheme != "file") {
                MenuRow(
                    text = stringResource(R.string.menu_save),
                    // Il perché di questa icona è su [Glyphs.Download].
                    icon = Glyphs.Download,
                    onTap = { menu.close(); ops.save(image) },
                    // Il tocco lungo chiede il nome per questa volta sola, come da specifica:
                    // la stessa coppia di 'Modifica', col tocco breve che fa la cosa di serie.
                    holdLabel = stringResource(R.string.menu_save_hold),
                    onHold = { menu.close(); ops.saveAs(image) }
                )
            }

            /*
             * '100%' e 'Adatta alla vista' sono spente di fabbrica e tornano con l'opzione
             * (richiesta dell'utente): fanno quello che fa il doppio tocco. Il filetto sopra di
             * loro sparisce con loro. Restano dietro un interruttore perché il gesto è nascosto;
             * a farlo scoprire è il velo del doppio tocco (`Hint.ZOOM_TAP`).
             */
            if (zoomRows) {
                HorizontalDivider()

                MenuRow(
                    text = stringResource(R.string.fit_label),
                    icon = Glyphs.Material.FitScreenOutlined,
                    onTap = { menu.close(); onZoom(restScale) }
                )
                MenuRow(
                    text = "100%",
                    icon = Glyphs.Material.PhotoSizeSelectActualOutlined,
                    onTap = { menu.close(); onZoom(oneToOne) }
                )
            }

            // La barra delle info non ha una voce in questo menu (istruzione dell'utente): si
            // regola dalle impostazioni, o col tocco lungo su 'Info' nel riquadro.

            HorizontalDivider()

            /*
             * Senza indirizzo il riquadro non compare: le sei operazioni agiscono su un file, e
             * un'immagine arrivata da una chat o dal web non ne ha uno.
             * La condivisione è nel riquadro ma non passa dai file: chiama
             * `ImageActions.share` sull'immagine caricata.
             */
            source?.let { uri ->
                val one = listOf(uri)
                /*
                 * I sei tasti hanno un nome perché due cambiano nel cestino: 'Rinomina' diventa
                 * 'Ripristina', e 'Copia' perde il tocco lungo che duplica.
                 * ⚠️ Nel cestino 'Copia' e 'Sposta' non si scambiano: l'ordine lo sceglie
                 * l'utente trascinando, e uno scambio automatico rovescerebbe la sua scelta.
                 */
                val copia =
                        // Il tocco lungo duplica, come nella griglia: il riquadro è lo stesso.
                        PadAction(
                            key = PadKey.COPY,
                            icon = Glyphs.FolderPair,
                            label = R.string.menu_copy_here,
                            onHold = if (inBin) null else {
                                {
                                    menu.close()
                                    ops.job(FileJob.Duplicate(one))
                                }
                            },
                            holdLabel = if (inBin) null else R.string.pick_duplicate
                        ) {
                            menu.close()
                            ops.job(FileJob.Transfer(one, move = false))
                        }
                val sposta =
                        PadAction(PadKey.MOVE, Glyphs.FolderPairDashed, R.string.pick_move) {
                            menu.close()
                            ops.job(FileJob.Transfer(one, move = true))
                        }
                val elimina =
                        PadAction(PadKey.DELETE, Glyphs.PickDelete, R.string.pick_delete) {
                            menu.close()
                            // Definitiva nel cestino o col cestino spento, come nella griglia:
                            // con lei viaggia la conferma.
                            ops.job(FileJob.Delete(one, forGood = inBin || !binOn))
                        }
                // Stesso posto nel riquadro per due azioni che si escludono: nel cestino si
                // ripristina, fuori si rinomina.
                val rinomina =
                        if (inBin) {
                            PadAction(PadKey.RENAME, Glyphs.BinRestore, R.string.bin_restore) {
                                menu.close()
                                ops.job(FileJob.Restore(one))
                            }
                        } else {
                            /*
                             * Spento su un'immagine della rete (giro della `1.82`, punto E): non
                             * c'è un file da rinominare, e l'operazione non riusciva. Solo questo
                             * dei sei: gli altri si comportano già come devono, e la
                             * condivisione non passa dai file.
                             */
                            PadAction(
                                key = PadKey.RENAME,
                                icon = Glyphs.TextCursor,
                                label = R.string.pick_rename,
                                enabled = !ImageActions.isRemote(uri)
                            ) {
                                menu.close()
                                ops.job(FileJob.Rename(one))
                            }
                        }
                val condividi =
                        PadAction(PadKey.SHARE, Glyphs.Material.Share, R.string.menu_share) {
                            menu.close()
                            ops.share(image)
                        }
                /*
                 * Il tocco lungo su 'Info' regola la barra delle info (richiesta dell'utente):
                 * il tocco breve apre le informazioni sul file, il lungo un riquadrino con
                 * acceso o spento e sopra o sotto, che si applica e si salva subito. È una
                 * scorciatoia dietro un gesto e non occupa spazio nel menu, come il tocco lungo
                 * su 'Modifica' e su 'Copia'.
                 */
                val info =
                        PadAction(
                            key = PadKey.INFO,
                            icon = Glyphs.Material.InfoOutlined,
                            label = R.string.pick_info,
                            onHold = { menu.close(); ops.bar() },
                            holdLabel = R.string.settings_info_visible
                        ) {
                            menu.close()
                            ops.job(FileJob.Facts(one))
                        }
                // L'ordine è quello scelto dall'utente nelle impostazioni; di fabbrica è quello
                // che ha dettato per la `1.54`: vedi `MENU_KEYS`.
                ActionPad(
                    actions = listOf(copia, sposta, condividi, rinomina, elimina, info)
                        // In AIV Play 'Rinomina' c'è da Android 11 (`Store.allows`).
                        .filter { Store.allows(it.key) }
                        .inOrder(LocalPadLook.current.menu)
                )
            }
            // Le impostazioni non sono in questo menu (istruzione dell'utente): si aprono dal
            // FAB della schermata delle cartelle. Nemmeno la ricerca per immagine: l'utente l'ha
            // tolta dopo averla provata; il codice è nella storia git, al tag `v0.17`.
        }
    }
}



/**
 * Il segno dell'immagine ridotta, al posto della parola.
 *
 * Un simbolo per lo spazio (voce `barra-ridotta`): `(ridotta)` mandava a capo la riga dei dati
 * su un telefono. La parola tradotta `bar_reduced` resta, in 28 lingue, per il lettore di
 * schermo: vedi il modificatore in [DetailsPanel].
 * Scritto per codepoint: `U+25F1`, WHITE SQUARE WITH LOWER LEFT QUADRANT, il carattere indicato
 * dall'utente.
 */
private const val REDUCED_MARK = "\u25f1"

/**
 * Quanto è più grande del testo il segno dell'immagine ridotta.
 *
 * È una scelta e non una misura: il rapporto fra l'inchiostro del carattere e l'altezza di una
 * maiuscola non è verificato col carattere vero. Il numero viene dal giudizio dell'utente sul
 * telefono (voce `segno-formato`: un 4-5% più del valore precedente, 1,2).
 */
private const val MARK_GROW = 1.25f

/**
 * L'aria sopra e sotto la riga dei dati.
 *
 * Due e non quattro: la coda diagnostica che chiedeva spazio è diventata un segno, e la barra si
 * può stringere (riscontro dell'utente).
 */
private val BAR_AIR = 2.dp

/**
 * La riga dei dettagli, col contatore della cartella fisso al suo estremo destro.
 *
 * Quell'angolo è del contatore, che si guarda mentre si sfoglia (istruzione dell'utente): la
 * rotella delle impostazioni che c'era prima non va rimessa, e le impostazioni si aprono dal FAB
 * della schermata delle cartelle.
 * Il contatore è fisso a destra e non in coda al testo: in coda si sposterebbe a ogni immagine,
 * perché nome, peso e percentuale cambiano la lunghezza della riga.
 */
@Composable
private fun DetailsPanel(
    image: LoadedImage,
    percent: Float,
    folder: Folder.Lookup?,
    /** Chi raccoglie dove la riga disegna, o `null` per la riga che sta sfumando via. */
    report: BarReport? = null,
    /** Se la barra è in cima allo schermo: decide da quale lato prende lo spazio di sistema. */
    atTop: Boolean
) {
    // Lette fuori dal `buildString`, che non è un contesto composable.
    val reduced = stringResource(R.string.bar_reduced)
    // `null` mentre la ricerca è in corso e sulle immagini che non hanno una cartella.
    val folderNote = when (folder) {
        null, is Folder.Lookup.Found -> null
        Folder.Lookup.NoPermission -> stringResource(R.string.folder_no_access)
        Folder.Lookup.Unreadable -> stringResource(R.string.folder_unreadable)
        is Folder.Lookup.NotInGallery ->
            stringResource(R.string.folder_not_in_gallery, folder.detail)
        Folder.Lookup.Alone -> stringResource(R.string.folder_alone)
        Folder.Lookup.Lost -> stringResource(R.string.folder_lost)
    }
    // ⚠️ Il velo di fondo non si dipinge qui: dentro la dissolvenza sarebbe disegnato due
    // volte. Lo dipinge `ViewerScreen` fuori dalla `AnimatedContent` (il perché è là), e il
    // colore del testo arriva da quella `Surface` tramite `LocalContentColor`.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            /*
             * ⚠️ Lo spazio di sistema si prende solo dal lato del bordo (voce `3.70-01`): coi
             * quattro lati di `safeDrawingPadding()` una barra in cima riceveva anche lo spazio
             * della barra di navigazione, una fascia vuota sotto il testo. I lati restano, per il
             * foro della fotocamera e la barra di navigazione laterale.
             */
            .windowInsetsPadding(
                steadyDrawing(viewer = true).only(
                    WindowInsetsSides.Horizontal +
                        if (atTop) WindowInsetsSides.Top else WindowInsetsSides.Bottom
                )
            )
            // Sopra e sotto [BAR_AIR]; ai lati 16, il margine di ogni cosa in questa schermata.
            .padding(start = 16.dp, end = 16.dp, top = BAR_AIR, bottom = BAR_AIR)
    ) {
        val name = image.displayName?.takeIf { it.isNotBlank() }
        // Il nome del formato lo calcola [kindOf], lo stesso del pannello delle informazioni.
        val formato = kindOf(image.mimeType) ?: "?"
        val resto = buildString {
                append("  ")
                append(image.pixelWidth).append(" x ").append(image.pixelHeight)
                image.byteSize?.let { append("  ").append(formatBytes(it)) }
                append("  ").append((percent * 100).roundToInt()).append('%')
                /*
                 * La riga non mostra la diagnostica dei tasselli (domanda dell'utente sul suo
                 * AVIF): che l'immagine sia ridotta lo dice il segno dopo il formato, mentre il
                 * motivo per cui i tasselli non si applicano serviva solo a chi scrive il codice.
                 * Un AVIF grande l'avrebbe sempre, perché `RegionSource` non lo sa rileggere
                 * (non c'è un `BitmapRegionDecoder` per quel formato). Il motivo resta in
                 * `info.tiles`, pronto se serve di nuovo.
                 */
                // Il perché di una cartella che non c'è resta qui, col resto del testo: è una
                // frase, e nell'angolo del contatore lo farebbe crescere e spostare.
                folderNote?.let { append("  ").append(it) }
        }
        /*
         * Il segno è subito dopo il formato (riscontro dell'utente): in coda alla riga si
         * spostava a ogni decina della percentuale, mentre quello che ha davanti non cambia
         * lunghezza. È più grande del testo di [MARK_GROW], perché `U+25F1` è disegnato
         * all'altezza della x e deve arrivare a quella di una maiuscola; il fattore è relativo
         * al corpo della riga, quindi segue la dimensione dei caratteri di sistema.
         */
        val corpo = MaterialTheme.typography.labelLarge
        val dati = buildAnnotatedString {
            append(formato)
            if (image.sampled) {
                append(' ')
                withStyle(SpanStyle(fontSize = corpo.fontSize * MARK_GROW)) {
                    append(REDUCED_MARK)
                }
            }
            append(resto)
        }
        /*
         * Il simbolo si vede e la parola si sente: su `U+25F1` un lettore di schermo non dice
         * niente di utile, e TalkBack legge la stringa tradotta `bar_reduced`. La descrizione
         * c'è solo su un'immagine ridotta, perché altrimenti copierebbe il testo, e si compone
         * dai pezzi perché il testo è un `AnnotatedString`.
         */
        val spoken = if (!image.sampled) Modifier
            else Modifier.semantics { contentDescription = "$formato $reduced$resto" }
        val series = folder?.seriesOrNull

        /*
         * Su una riga sola quando tutto entra nella larghezza (mockup `Viewer_H`, scelta A1).
         * ⚠️ La scelta dipende dalla sola larghezza: i dati si misurano su un modello coi numeri
         * più larghi possibili ([oneRowFits]), e al nome si chiede solo [ONE_ROW_NAME]. Se
         * dipendesse dal nome, la barra cambierebbe altezza da un'immagine all'altra, e con lei
         * la misura dell'immagine; se dipendesse dalla percentuale, a metà di una pinza.
         */
        val markWidth = with(LocalDensity.current) {
            (MaterialTheme.typography.labelMedium.fontSize.toPx() * MARK_TALL * MARK_RATIO).toDp()
        }
        val measurer = rememberTextMeasurer()
        val dataRoom = with(LocalDensity.current) {
            measurer.measure(dataModel(corpo), corpo).size.width.toDp()
        }
        /*
         * ⚠️ L'ingombro dei dati si misura con la percentuale più larga e non con quella in scena:
         * la tolleranza dell'immagine legge dove la barra ha del testo, e se quel tratto cambiasse
         * con la percentuale la misura di riposo cambierebbe a metà di una pinza, azzerando il
         * gesto.
         */
        val dataInk = remember(dati, corpo, measurer) {
            val widest = buildAnnotatedString {
                append(dati.subSequence(0, dati.length - resto.length))
                append(resto.replace(Regex("\\d+%"), "8888%"))
            }
            measurer.measure(widest, corpo).size.width.toFloat()
        }
        val counterRoom = with(LocalDensity.current) {
            series?.let { measurer.measure(counterModel(it.size), corpo).size.width.toDp() } ?: 0.dp
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val rowWidth = maxWidth
            val oneRow = name == null || oneRowFits(rowWidth, dataRoom, counterRoom, markWidth)
            if (report != null) {
                SideEffect {
                    if (name == null) { report.drop("name"); report.drop("mark") }
                    if (series == null) report.drop("counter")
                }
            }
            if (oneRow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (name != null) {
                        NameText(name, Modifier.weight(1f), report)
                        Spacer(Modifier.width(ONE_ROW_GAP))
                    }
                    /*
                     * ⚠️ I dati sono allineati a destra, accanto al contatore, così il centro
                     * della barra resta senza testo e un'immagine stretta può passarci sotto (la
                     * tolleranza in orizzontale). Allineati a sinistra coprivano il centro su ogni
                     * telefono: lo presidia `BarraInfoTest`. La scatola ha la larghezza dei dati
                     * più larghi ([dataModel]), e il nome non si riaccorcia mentre la percentuale
                     * cresce.
                     */
                    Text(
                        text = dati,
                        style = corpo.snug(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = (if (name == null) Modifier.weight(1f)
                            else Modifier.widthIn(min = dataRoom, max = rowWidth * ONE_ROW_DATA_MAX))
                            .then(spoken)
                            .then(report?.atEnd("data", dataInk) ?: Modifier)
                    )
                    series?.let { Counter(it, report) }
                    if (name != null) {
                        Spacer(Modifier.width(MARK_GAP))
                        AppMark(report)
                    }
                }
            } else {
                Column {
                    /*
                     * Quando la riga unica non entra, il nome va sopra i dati, piccolo, su una
                     * riga con l'ellissi in mezzo (richiesta dell'utente): nella riga dei dati
                     * toglierebbe spazio al formato, alla misura e alla percentuale, e sposterebbe
                     * il contatore. È più piccolo dei dati (`labelMedium` contro `labelLarge`),
                     * perché la riga esiste per i dati.
                     */
                    NameLine(name, report)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dati,
                            style = corpo.snug(),
                            modifier = Modifier
                                .weight(1f)
                                .then(spoken)
                                .then(report?.at("data") ?: Modifier)
                        )
                        series?.let { Counter(it, report) }
                    }
                }
            }
        }
    }
}

/** Il contatore `n/totale`, fisso a destra: vedi la nota su [DetailsPanel]. */
@Composable
private fun Counter(series: Folder.Series, report: BarReport?) {
    Text(
        text = "${series.index + 1}/${series.size}",
        style = MaterialTheme.typography.labelLarge.snug(),
        modifier = Modifier
            .padding(start = 12.dp)
            .then(report?.at("counter") ?: Modifier)
    )
}

/**
 * Chi raccoglie dove la barra disegna, per la tolleranza dell'immagine (`restPlace`).
 *
 * Si passa alla sola riga che sta entrando: durante la dissolvenza incrociata le righe sono due,
 * e quella che sfuma descrive l'immagine precedente.
 */
private class BarReport(private val info: InfoBar) {
    fun at(key: String): Modifier =
        Modifier.onGloballyPositioned { info.pieces[key] = it.boundsInRoot() }

    /**
     * Come [at], per un testo allineato a destra: del riquadro conta solo l'ultimo tratto, largo
     * [ink] pixel, che è dove il testo è davvero.
     */
    fun atEnd(key: String, ink: Float): Modifier =
        Modifier.onGloballyPositioned {
            val box = it.boundsInRoot()
            info.pieces[key] = Rect(max(box.left, box.right - ink), box.top, box.right, box.bottom)
        }

    fun drop(key: String) {
        info.pieces.remove(key)
    }
}

/**
 * Se la riga unica entra in [width]: i dati al loro massimo, il contatore, il marchio e almeno
 * [ONE_ROW_NAME] per il nome.
 */
internal fun oneRowFits(width: Dp, data: Dp, counter: Dp, mark: Dp): Boolean =
    width - data - counter - mark - (ONE_ROW_GAP + 12.dp + MARK_GAP) >= ONE_ROW_NAME

/**
 * I dati coi numeri più larghi che possono comparire: il formato più lungo, il segno della
 * riduzione, cinque cifre per lato, il peso più largo che [formatBytes] scrive e una percentuale
 * a quattro cifre.
 *
 * Le cifre sono `8`, e in Roboto le cifre hanno tutte la stessa larghezza: il modello misura lo
 * spazio, non un valore.
 */
private fun dataModel(corpo: TextStyle) = buildAnnotatedString {
    append("WEBP ")
    withStyle(SpanStyle(fontSize = corpo.fontSize * MARK_GROW)) { append(REDUCED_MARK) }
    append("  88888 x 88888  8888.88 MB  8888%")
}

/** Il contatore più largo per una serie di [size] elementi. */
private fun counterModel(size: Int): String {
    val digits = "8".repeat(size.toString().length)
    return "$digits/$digits"
}

/**
 * Il nome del file sopra la riga dei dati: una riga sola, con l'ellissi in mezzo.
 *
 * ⚠️ È [fitName], lo stesso della griglia e della pastiglia: `TextOverflow.Ellipsis` mette i tre
 * punti alla fine e toglie l'estensione, che l'utente ha chiesto di non perdere mai, e
 * `TextOverflow.MiddleEllipsis` non basta (il perché è misurato in [fitName]). Una riga sola,
 * perché qui il nome è l'intestazione di una riga di dati. L'estensione è in grassetto come
 * altrove: distingue i tre punti dell'ellissi dal punto dell'estensione. `BoxWithConstraints`
 * dà la larghezza utile di ogni schermo.
 *
 * ⚠️ Allineato a sinistra (correzione dell'utente): un nome accorciato arriva comunque al margine
 * destro, dove finisce il contatore, e allineare a destra spostava anche i nomi corti.
 * Meno acceso dei dati ([NAME_FADE], richiesta dell'utente), partendo dal colore della `Surface`
 * dietro (`LocalContentColor`); staccato dai dati di [NAME_GAP], perché attaccato si leggeva come
 * la prima parola della riga di sotto.
 */
@Composable
private fun NameLine(name: String?, report: BarReport?) {
    if (name == null) return
    /*
     * Il marchio dell'app è in fondo a questa riga, ancorato a destra, a scapito di un pezzo di
     * nome (richiesta dell'utente, per identità). Nella `Row` il nome prende lo spazio che resta
     * (`weight`) e il glifo la sua misura; in un `Box` il glifo coprirebbe le ultime lettere.
     * ⚠️ Il nome riempie lo spazio anche quando è corto (riscontro dell'utente): con
     * `fill = false` la scatola si stringeva sul testo e il glifo restava in mezzo alla riga.
     * Riempire non cambia l'accorciamento: la `Row` passa a un figlio con peso la stessa
     * larghezza massima, e `fill` cambia solo la minima.
     */
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = NAME_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NameText(name, Modifier.weight(1f), report)
        Spacer(Modifier.width(MARK_GAP))
        AppMark(report)
    }
}

/**
 * Il nome accorciato al centro con [fitName], nello spazio che [modifier] gli dà.
 *
 * È a sé perché lo usano due righe: quella del nome sopra i dati e la riga unica, dove il
 * marchio è in fondo, dopo il contatore.
 */
@Composable
private fun NameText(name: String, modifier: Modifier, report: BarReport?) {
    val style = MaterialTheme.typography.labelMedium.snug()
    BoxWithConstraints(modifier = modifier) {
        val measurer = rememberTextMeasurer()
        val room = with(LocalDensity.current) { maxWidth.roundToPx() }
        val shown = remember(name, room, style, measurer) {
            fitName(name, room, 1, style, measurer)
        }
        Text(
            text = shown.text,
            // Lo stile con cui il nome è stato misurato e non quello pieno: vedi `fitName`.
            style = shown.style,
            maxLines = 1,
            color = LocalContentColor.current.copy(alpha = NAME_FADE),
            modifier = report?.at("name") ?: Modifier
        )
    }
}

/** Il marchio dell'app in fondo alla riga del nome: le ragioni sono su [NameLine]. */
@Composable
private fun AppMark(report: BarReport?) {
    val style = MaterialTheme.typography.labelMedium
    /*
     * L'altezza si ricava dal corpo della riga e la larghezza segue: il glifo è 70 x 60 (vedi
     * `ic_aiv_mark.xml`). Relativa e non in dp, come il segno dell'immagine ridotta, quindi
     * segue la dimensione dei caratteri di sistema.
     * [MARK_TALL] è una scelta e non una misura: 0,85 del corpo lo porta un po' sopra le
     * maiuscole del nome, e a 0,71, l'altezza di una maiuscola, spariva. Il rapporto col
     * carattere vero non è verificato.
     */
    val tall = with(LocalDensity.current) { (style.fontSize.toPx() * MARK_TALL).toDp() }
    Icon(
        imageVector = Glyphs.AivMark,
        // Nessuna descrizione: il marchio non dice niente dell'immagine, e un lettore di
        // schermo direbbe 'AIV' dopo ogni nome di file.
        contentDescription = null,
        /*
         * Il colore pieno dell'accento, senza trasparenza (riscontro dell'utente): con l'alfa
         * del nome la firma era più spenta del testo. Il contrasto è quello dell'accento, 2,43
         * sul fondo chiaro (misurato in `Theme.kt`), sotto la soglia delle grafiche non testuali
         * e accettato là: qui non c'è niente da leggere.
         */
        tint = MaterialTheme.colorScheme.primary,
        // Perché lo scostamento è un `offset` e non un margine: vedi [MARK_NUDGE].
        modifier = Modifier
            .height(tall)
            .offset(x = MARK_NUDGE, y = -MARK_NUDGE)
            .then(report?.at("mark") ?: Modifier)
    )
}

/**
 * Quanto è alto il marchio dell'app in coda al nome, in frazione del corpo della riga.
 *
 * È una scelta e non una misura (il perché è accanto all'uso): a 0,71, l'altezza di una
 * maiuscola, il glifo spariva accanto al nome, e 0,85 lo portava appena sopra. L'utente l'ha
 * poi voluto un 15% più grande (riscontro della `1.45`), e resta scritto il prodotto, così si
 * legge da dove si parte e di quanto si è cresciuti.
 */
private const val MARK_TALL = 0.85f * 1.15f

/** Larghezza su altezza del marchio: il glifo è 70 x 60 (`ic_aiv_mark.xml`). */
private const val MARK_RATIO = 70f / 60f

/**
 * Lo spazio minimo che il nome deve avere perché la barra vada su una riga sola.
 *
 * È una scelta: 160 dp tengono una ventina di caratteri, un nome accorciato al centro che si
 * riconosce ancora. Su un telefono in verticale non ci sono, e la barra resta su due righe; su un
 * telefono in orizzontale e su un tablet sì.
 */
private val ONE_ROW_NAME = 160.dp

/** Lo stacco fra il nome e i dati nella riga unica. */
private val ONE_ROW_GAP = 16.dp

/**
 * Quanto della riga unica i dati possono prendere quando portano una frase (la cartella che non
 * c'è): oltre, la frase si accorcia coi tre punti e il nome conserva il suo spazio.
 */
private const val ONE_ROW_DATA_MAX = 0.6f

/**
 * L'aria fra la fine del nome e il marchio.
 *
 * Il nome accorciato può finire con i tre punti dell'ellissi, che attaccati al glifo si
 * leggerebbero come parte di lui.
 */
private val MARK_GAP = 6.dp

/**
 * Di quanto il marchio esce verso il margine e verso l'alto, per il suo equilibrio ottico.
 *
 * ⚠️ È un `offset` e non un margine, perché non deve occupare spazio (riscontro della `1.45`:
 * senza muovere il resto). Un margine sposterebbe il nome e alzerebbe la riga, e servirebbe un
 * secondo margine di segno opposto, una compensazione vietata; un `offset` sposta solo dove il
 * glifo è posato. Lo spazio c'è: il glifo è più basso della riga di testo, e a destra c'è il
 * margine del pannello.
 * `offset` e non `absoluteOffset`: dove si scrive da destra il glifo è dall'altra parte della
 * riga, e deve uscire verso quel margine.
 */
private val MARK_NUDGE = 2.dp

/**
 * Quanto è spento il nome del file in testa alla barra.
 *
 * Il valore viene dalle richieste dell'utente, l'ultima un 20% in meno di 0,65.
 * ⚠️ Il limite sotto è 0,38, l'opacità che Material dà ai comandi spenti: un testo che la
 * raggiunge sembra disattivato e non secondario.
 */
private const val NAME_FADE = 0.52f

/**
 * Quanto il nome si stacca dai dati sotto di lui.
 *
 * Tre punti e non due: la richiesta diceva 2 o 3 pixel apparenti, e due punti su uno schermo a
 * densità tre sono sei pixel fisici, al limite del percepibile.
 */
private val NAME_GAP = 3.dp

/**
 * Quanto copre il velo dietro la riga dei dettagli: lo dipinge `ViewerScreen`, e la nota sul
 * lampeggio ne usa il valore nel conto.
 */
private const val PANEL_VEIL = 0.86f

/**
 * La tinta piatta del fondo, cioè quella di `BgType.SOLID`.
 *
 * La usano in due: il fondo pieno e la copertura della scacchiera quando arriva la sfocatura
 * (vedi il `drawBehind` del palco).
 * I grigi sono quelli dello userscript: `#DDD`/`#EEE` sul chiaro e `#333`/`#222` sullo scuro, e
 * la tinta piatta è `#EEE` sul chiaro e `#222` sullo scuro. La scacchiera rende visibile la
 * trasparenza; la tinta piatta serve a guardare un'immagine senza un motivo sotto.
 */
private fun piatto(light: Boolean): Color =
    if (light) Color(0xFFEEEEEE) else Color(0xFF222222)

private fun DrawScope.drawBackground(size: Size, square: Float, light: Boolean, type: BgType) {
    val a = if (light) Color(0xFFDDDDDD) else Color(0xFF333333)
    val b = piatto(light)
    if (type == BgType.SOLID) {
        // `b` in tutti e due i temi: `#EEE` nella coppia chiara e `#222` in quella scura.
        drawRect(color = b, size = size)
        return
    }
    drawRect(color = b, size = size)
    var y = 0f
    var row = 0
    while (y < size.height) {
        var x = if (row % 2 == 0) 0f else square
        while (x < size.width) {
            drawRect(
                color = a,
                topLeft = Offset(x, y),
                size = Size(min(square, size.width - x), min(square, size.height - y))
            )
            x += square * 2
        }
        y += square
        row++
    }
}


/**
 * Il peso di un file, in multipli da 1024. Tutti i pesi dell'app passano di qui (risposta
 * `punto` dell'utente a `d-pesi-scrittura`): con due formattazioni lo stesso file si leggeva
 * '12.40 MB' in un posto e '13 MB' in un altro.
 *
 * ⚠️ La lingua è `US` e non quella del telefono: il separatore resta il punto in tutte le
 * lingue, come nello userscript. Implicita, darebbe la virgola su un telefono italiano e il
 * punto su uno inglese, e lint la segnala.
 */
internal fun formatBytes(value: Long): String = when {
    value < 1024 -> "$value B"
    value < 1024 * 1024 -> String.format(Locale.US, "%.1f kB", value / 1024f)
    value < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.2f MB", value / (1024f * 1024f))
    // I GB servono alla selezione multipla: un'immagine sola non ci arriva, trecento insieme
    // sì, e senza questo ramo si leggerebbe '4231.77 MB'.
    else -> String.format(Locale.US, "%.2f GB", value / (1024f * 1024f * 1024f))
}

/** Quanto la fila dei comandi si stacca dal fondo, o dalla riga dei dettagli quando è sotto. */
private val ANIM_LIP = 24.dp

/**
 * Quanto dura lo spostamento dell'immagine quando la barra delle info compare o si nasconde.
 *
 * Breve, come dice la scelta A2: è un cambio di spazio, non un evento da guardare.
 */
private const val BAR_ROOM_MS = 220

/**
 * Quanto resta la notifica che dice 'questa l'hai già scaricata'.
 *
 * Cinque secondi, chiesti dall'utente (giro della `1.81`, punto E): più dei tre dell'avviso
 * dell'eliminazione annullata, perché qui si decide se rifare un gesto.
 */
private const val SAVE_SEEN_MS = 5000L

/**
 * La firma di quello che si sta per scaricare: suffisso e byte, oppure `null` se i byte non si
 * sanno.
 *
 * ⚠️ Senza i byte non c'è firma, e il salvataggio va avanti: un sorgente di cui non si legge la
 * lunghezza darebbe `0`, e tutte le immagini senza misura avrebbero la stessa firma, cioè l'app
 * direbbe 'già scaricata' a immagini mai viste.
 */
private suspend fun markOf(
    context: Context,
    image: LoadedImage,
    uri: Uri,
    suffix: String?
): String? = withContext(Dispatchers.IO) {
    val bytes = sizeOf(context, uri)
    if (bytes <= 0L) return@withContext null
    val coda = suffix ?: ImageActions.splitName(ImageActions.fileName(image, uri)).second
    DownloadLog.mark(coda, bytes)
}

/** Prende nota di un salvataggio riuscito, se la firma si può fare. */
private suspend fun noteDownload(
    context: Context,
    image: LoadedImage,
    uri: Uri,
    suffix: String?
) {
    val mark = markOf(context, image, uri, suffix) ?: return
    DownloadLog.note(context, mark, System.currentTimeMillis())
}

/**
 * L'immagine finta con cui la copia invisibile della barra misura la propria altezza.
 *
 * Ha un nome, perché sulle due righe la riga del nome cambia l'altezza della barra; per
 * un'immagine senza nome, che è rara, la barra vera corregge la misura.
 * ⚠️ È pigra: una bitmap creata col caricamento del file fa fallire chi lo carica senza una
 * grafica vera, come il banco di prova.
 */
private val PROBE_IMAGE by lazy {
    LoadedImage(
        bitmap = ImageBitmap(1, 1),
        mimeType = "image/jpeg",
        byteSize = 1L,
        pixelWidth = 1,
        pixelHeight = 1,
        sampled = false,
        displayName = "probe.jpg"
    )
}

/**
 * Lo stesso stile senza l'interlinea in eccesso sopra e sotto la riga.
 *
 * Assottiglia la barra delle info, su una riga e su due (voci `3.60-01` e `3.70-01`):
 * un'etichetta di Material ha una riga più alta del suo carattere (20 sp per 14 nel corpo dei
 * dati), e tagliata la riga è alta quanto il carattere. Fra il nome e i dati resta il solo
 * [NAME_GAP].
 */
private fun TextStyle.snug(): TextStyle = copy(
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

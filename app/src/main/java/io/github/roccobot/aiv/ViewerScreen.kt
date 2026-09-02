package io.github.roccobot.aiv

import android.graphics.Rect as PixelRect
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoSizeSelectActual
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImagePainter
import coil3.compose.asPainter
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import coil3.compose.rememberAsyncImagePainter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Below this, a picture is a speck: it is the floor of the pinch, not of the fit. */
private const val MIN_SCALE = 0.02f

/**
 * How fast the one handed zoom moves: the scale is multiplied by `exp(dy * this)`
 * every frame, so 200dp of travel is a little more than a doubling.
 *
 * ⚠️⚠️ DOWN zooms IN, and it is the opposite of what this file used to do. The old
 * direction was argued from the userscript's `dv-wheel-up-in`, so that two viewers
 * of the same family would agree on which way is closer. That argument LOST against
 * a thumb: the user tried it on a phone and it went the wrong way (2026-08-26). Keep
 * this note, because the reasoning that produced the old direction still sounds
 * good on paper and someone will make it again.
 * - The reason the analogy fails is that a wheel and a thumb are not the same
 *   thing: the wheel pushes the picture away from you, while the thumb DRAGS the
 *   picture, and dragging it down is pulling it towards you.
 */
private const val DRAG_ZOOM_SENSITIVITY = 0.005f

/** Side of one checkerboard square, in dp. */
private val CHECKER = 12.dp

/**
 * Lo stacco fra una foto e la sua vicina mentre si sfoglia.
 *
 * ⚠️ Serve a far leggere DUE fogli invece di un'immagine che si trasforma: senza, due
 * fotografie chiare di seguito sembrano una sola che scivola. È anche la ragione per cui
 * l'animazione di arrivo va a `larghezza + questo` e non a `larghezza`: così la vicina
 * atterra esattamente al centro.
 */
private val PAGE_GAP = 16.dp

/**
 * Quanto deve essere rapido un colpo di pollice perché la pagina giri lo stesso.
 *
 * ⚠️ **La soglia di spazio da sola non basta a sembrare fluidi**: un buffetto veloce che
 * copre un decimo di schermo è chiaramente 'vai avanti', e senza questa via tornerebbe
 * indietro lasciando l'impressione che il gesto non sia stato capito. 400dp al secondo è
 * il punto di partenza: è sopra il minimo di sistema (50dp/s, che scatterebbe su ogni
 * sfioramento) e sotto il colpo secco.
 */
private val FLING_VELOCITY = 400.dp

/**
 * Sotto questa velocità l'inerzia non parte, in dp al secondo (poi tradotti in pixel).
 *
 * ⚠️ 120 e non 50 come la soglia di lancio di Android: quella decide se un elenco **deve**
 * scorrere, qui la figura si è già mossa col dito e la domanda è un'altra, cioè se
 * continuare. Un dito che si alza da fermo lascia qualche decina di dp al secondo, e sotto
 * questa soglia lo scivolamento si leggerebbe come un sussulto.
 */
private val GLIDE_FLOOR = 120.dp

/**
 * Quanto dura l'arrivo a destinazione, o il ritorno al posto.
 *
 * ⚠️ Corta apposta: questa animazione parte quando il dito si è già alzato, quindi è
 * tempo in cui non si comanda più niente. Sotto i ~150ms sembra uno scatto, sopra i ~300
 * sembra che l'app ci pensi su.
 */
private const val SNAP_MS = 220

/**
 * Quanto la pagina segue il dito quando di là non c'è niente.
 *
 * ⚠️ Non zero e non uno: bloccarla del tutto farebbe sembrare il gesto non ricevuto,
 * lasciarla libera prometterebbe una foto che non esiste. Un terzo è la frenata che si
 * legge come 'sei al capolinea'.
 */
private const val END_RESISTANCE = 0.33f

/**
 * Quanto si aspetta, fermi, prima di leggere il pezzo nitido.
 *
 * ⚠️ È il tempo che separa 'ho finito di muovermi' da 'sto ancora inquadrando'. Corto
 * abbastanza da non farsi notare come un ritardo, lungo abbastanza che una pinza non
 * faccia partire una lettura per ogni posizione attraversata.
 */
private const val TILE_DELAY_MS = 200L

/**
 * Quanto l'anello del caricamento aspetta prima di farsi vedere.
 *
 * ⚠️ **Mezzo secondo, e la soglia è quella del fastidio e non della prestazione**: sotto,
 * un indicatore comparso e sparito si legge come un lampeggio, non come un'informazione.
 * Sopra, chi sta aspettando davvero comincia a chiedersi se l'app è viva. Le foto del
 * telefono si decodificano molto prima, quindi sfogliando non lo si vede mai.
 */
private const val PROGRESS_GRACE_MS = 500L

/**
 * Il tetto in pixel di un pezzo letto a piena risoluzione, **contato in schermate**.
 *
 * ⚠️⚠️ **ERA UN NUMERO FISSO, QUATTRO MILIONI, ED È IL DIFETTO CHE HA RESO INUTILE LA
 * `0.39` SUI TELEFONI DI OGGI** (segnalazione dell'utente, 2026-08-29: *con l'immagine
 * grande non mi accorgo di nulla*, con la riga dei dettagli che diceva `sampled`, quindi
 * i tasselli dovevano accendersi). La nota vecchia diceva 'circa uno schermo e mezzo', e
 * il conto **non torna più**: su uno schermo da 1440x3120 una schermata sola è
 * **4.49 milioni** di pixel, cioè già sopra il tetto. Quel numero è nato quando 1080x2400
 * (2.59 milioni) era il normale.
 * ⚠️⚠️ **E la conseguenza era esattamente 'non succede niente', nel punto in cui la
 * funzione serve di più**: al **100%** il pezzo da leggere è grande **esattamente una
 * schermata**, quindi il tetto lo respingeva, il campionamento veniva alzato per
 * rientrare, e alzato arrivava al livello del bitmap di base, dove leggere non guadagna
 * niente e si rinuncia. Il codice faceva la cosa giusta con un dato sbagliato.
 * ⚠️ **Due schermate e non quattro**, e le due soglie vogliono dire cose diverse: il caso
 * peggiore teorico è **quattro** schermate (il campionamento arrotonda per difetto, quindi
 * fino al doppio per lato), ma capita solo **sotto** il 100%, dove il bitmap di base è
 * ingrandito appena e il guadagno è marginale mentre il costo sarebbe di 69 MB. A due
 * schermate il 100% e tutti gli ingrandimenti sopra di lui passano sempre, perché da lì
 * in su il pezzo **si restringe** man mano che si ingrandisce.
 * ⚠️ Il minimo esiste per gli schermi piccoli, dove due schermate sarebbero un pezzo
 * troppo modesto per valere la lettura.
 */
private const val TILE_SCREENFULS = 2L
private const val MIN_TILE_PIXELS = 4_000_000L

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
     * La fotografia che si sta guardando non è più raggiungibile a quell'indirizzo: è stata
     * spostata, rinominata o eliminata.
     *
     * ⚠️ **Vale anche per la rinomina**, e il perché sta in [FileKind]: il file c'è ancora,
     * la sua riga nel MediaStore no. ⚠️ Si chiama **solo se qualcosa è andato a buon fine**:
     * un'operazione fallita lascia tutto dov'era, e far ricaricare la cartella per niente
     * farebbe perdere il posto senza motivo.
     */
    onFileChanged: () -> Unit,
    /**
     * Un file **nuovo** è comparso nella cartella: un fotogramma esportato, una conversione.
     *
     * ⚠️⚠️ **NON È [onFileChanged], e confonderli sposterebbe la fotografia sotto gli occhi
     * di chi guarda**: quella rilegge la cartella e mostra *quello che sta in quella
     * posizione*, che dopo un'aggiunta è la foto precedente, perché l'ordine è per data e il
     * file nuovo entra in cima. Qui invece non cambia niente di quello che si sta guardando:
     * si segna soltanto che la **griglia** dietro è vecchia, e si rilegge quando ci si torna.
     */
    onFileAdded: () -> Unit,
    /**
     * 'Modifica' dal menu del tocco lungo: la fotografia da aprire in un editor.
     *
     * ⚠️ **Sale al modello e non si risolve qui**, perché la risposta dipende da
     * un'impostazione (quale app) e può **cambiare schermata** (l'editor di casa). Vedi
     * `ViewerViewModel.edit`.
     */
    onEdit: (Uri) -> Unit,
    onEditWith: (Uri) -> Unit,
    /**
     * La barra delle info cambiata al volo dal tocco lungo su 'Info': accesa e dove sta.
     *
     * ⚠️ **Due booleani e non le impostazioni intere**, come `binOn` e `factFields`: questa
     * schermata non ne cambia altre, e passare il mutatore completo inviterebbe a farlo.
     */
    onInfoBar: (Boolean, InfoPosition) -> Unit,
    /**
     * Se la fotografia che si sta guardando viene dal **cestino**.
     *
     * ⚠️ Cambia due voci del riquadro e niente altro: 'rinomina' diventa 'ripristina' (là
     * dentro non si rinomina) e 'elimina' diventa definitiva, perché un file del cestino
     * non ha un secondo cestino in cui andare.
     * ⚠️ **Senza valore di serie**, e non per pignoleria: `modifier` deve restare il primo
     * parametro con un valore predefinito (lo pretende lint, ed è la convenzione di
     * Compose), quindi o questo sta prima di lui senza serie o starebbe dopo di lui, dove
     * una schermata non si aspetta di trovarlo.
     */
    inBin: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ⚠️ Le risorse da `LocalResources` e non da `context.resources`: quelle seguono i
    // cambi di configurazione, e l'esito si compone dentro una coroutine. Vedi `GridScreen`.
    val res = LocalResources.current

    /*
     * ⚠️⚠️ **PARTE DA 'GIÀ VISTO' e non da 'mai visto'**, come nella griglia: l'archivio si
     * legge in una coroutine, quindi per un fotogramma non si sa ancora niente, e col valore
     * opposto il velo lampeggerebbe addosso a chi l'aveva già archiviato mesi fa.
     */
    val zoomSeen by produceState(true) { Hint.ZOOM_TAP.flow(context).collect { value = it } }
    val zoomHint = !zoomSeen

    /** Il dialogo di un'operazione sui file, e `null` quando non ce n'è aperto nessuno. */
    var job by remember { mutableStateOf<FileJob?>(null) }

    /**
     * Il giro di un'operazione: si parte, si dice com'è andata, e se la fotografia non c'è
     * più si avverte chi sa che cosa mostrare al suo posto.
     *
     * ⚠️⚠️ **GIRA NELL'AMBITO DI QUESTA SCHERMATA, e non in quello del menu o della tela**:
     * quelli se ne vanno, il primo quando il menu si chiude e la seconda al cambio di stato,
     * cioè **esattamente** nell'istante in cui un'eliminazione riesce. Una coroutine
     * lasciata là dentro verrebbe annullata alla prima sospensione, e si perderebbero
     * l'avviso e la rilettura, non il lavoro sui file, che `FileTree` protegge da sé.
     */
    val perform: (FileKind, suspend () -> FileTree.Outcome) -> Unit = { kind, work ->
        scope.launch {
            val out = work()
            Toast.makeText(context, outcomeText(res, out, kind.done), Toast.LENGTH_LONG).show()
            if (kind.gone && out.done > 0) onFileChanged()
        }
    }

    /**
     * Lo stato della riga dei dettagli, che **vive più a lungo di ogni fotografia**.
     *
     * ⚠️⚠️ **È QUI IL RIMEDIO AL LAMPEGGIO, e il difetto era di STRUTTURA e non di
     * animazione** (segnalazione dell'utente, 2026-08-29: *all'apparizione le info
     * lampeggiano ancora*, dopo che la `0.40` aveva già provato con la dissolvenza). La
     * riga stava dentro `ImageCanvas`, che esiste solo nello stato `Ready`: a ogni
     * cambio di fotografia veniva **distrutta e ricostruita**, con in mezzo il tempo del
     * caricamento. Qualunque animazione si metta su una cosa che nasce dal nulla è
     * comunque una cosa che nasce dal nulla, ed è per questo che sfumare l'entrata non
     * era bastato.
     * ⚠️ Adesso la riga è **un solo elemento** per tutta la vita del visualizzatore: non
     * entra e non esce, cambia **testo**. Fra una fotografia e l'altra tiene i dati di
     * quella che si è appena lasciata, così non c'è mai un istante vuoto, e quando la
     * nuova è pronta il testo si dissolve nell'altro.
     */
    val info = remember { InfoBar() }

    /**
     * L'ultima fotografia che si è vista, tenuta **anche mentre la prossima si carica**.
     *
     * ⚠️ È la ragione per cui la riga non resta mai senza niente da dire. Il prezzo,
     * dichiarato: per il tempo del caricamento quei dati sono quelli di **prima**. Su una
     * strisciata fra foto del telefono sono decimi di secondo; su un'immagine grossa presa
     * dalla rete si vedono più a lungo, ed è il baratto che toglie il lampeggio.
     */
    var shown by remember { mutableStateOf<LoadedImage?>(null) }
    LaunchedEffect(state) { (state as? ViewerState.Ready)?.let { shown = it.image } }

    /*
     * ⚠️ **Il dialogo della conversione vive QUI e non dentro il menu**, per la stessa ragione
     * del selettore qui sotto: il menu si chiude nell'istante in cui si tocca la voce, e
     * quello che gli è appeso si chiude con lui. Un dialogo aperto da una voce di menu deve
     * per forza vivere fuori dal menu.
     */
    var converting by remember { mutableStateOf(false) }

    /*
     * ⚠️⚠️ **L'ANIMAZIONE SI APRE QUI E NON PIÙ DENTRO IL RAMO `Ready`, dalla 1.21**, e la
     * ragione è che adesso la guardano in due: la fila dei comandi, che sta là sotto, e
     * 'Converti/Esporta', che vive quassù perché il menu che lo apre si chiude nell'istante
     * in cui lo si tocca. Aperta in un ramo, il dialogo non l'avrebbe mai vista.
     * ⚠️ **Costa quanto prima su una fotografia ferma**: `rememberAnimation` guarda i primi
     * byte del file e se ne va. Su una animata comincia un attimo prima, mentre l'immagine si
     * sta ancora caricando, e non si vede: i fotogrammi li disegna la tela, che non c'è
     * ancora.
     */
    val animation = rememberAnimation(source)

    /*
     * ⚠️⚠️ **IL FOTOGRAMMA SI PRENDE QUANDO SI TOCCA LA VOCE, non quando si preme 'Esporta'
     * nel dialogo**, ed è la stessa ragione per cui lo faceva la fila dei comandi: fra il
     * tocco e la scelta della cartella passano secondi. Qui in più la riproduzione è già
     * ferma (la apre il tocco lungo, vedi `onHold`), ma prendere l'istante giusto non deve
     * dipendere da quella garanzia.
     * ⚠️ **`null` vuol dire 'converti il file intero'**, che è il caso di ogni fotografia
     * ferma: il dialogo lo legge così.
     */
    var shot by remember(source) { mutableStateOf<android.graphics.Bitmap?>(null) }

    /** L'animazione com'è **adesso**, per i richiami che vivono dentro un `remember`. */
    val liveAnimation = rememberUpdatedState(animation)

    /*
     * ⚠️ **Vive qui e non nel menu**, per la stessa ragione del dialogo qui sopra: il menu si
     * chiude nell'istante in cui si tocca la voce, e quello che gli è appeso si chiude con
     * lui.
     */
    var barOpen by remember { mutableStateOf(false) }
    if (barOpen) {
        InfoBarPopup(
            settings = settings,
            onChange = onInfoBar,
            onDismiss = { barOpen = false }
        )
    }

    // ⚠️ Il `takeIf` sta sull'immagine e non in un `if` esterno: così non si scrive mai su
    // uno stato **durante** la composizione, che è il modo classico di far ricomporre in
    // tondo. Senza immagine il dialogo semplicemente non c'è, e il caso non capita perché la
    // voce che lo apre vive dentro un menu che senza immagine non si apre.
    shown?.takeIf { converting }?.let { picture ->
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
     * Il selettore di sistema con cui si scarica la fotografia: così salvare non chiede
     * nessun permesso e il posto lo scegli tu. Su Android 9, che questa app ancora
     * sostiene, scrivere nella galleria dal MediaStore avrebbe voluto
     * `WRITE_EXTERNAL_STORAGE`.
     *
     * ⚠️⚠️ **STA QUI E NON NEL MENU dalla 0.62, ed è una correzione**: un richiamo
     * registrato dentro il menu viene **cancellato quando il menu si chiude**, cioè nel
     * momento esatto in cui si tocca la voce, e l'esito del selettore arriva sempre dopo.
     * Il difetto è stato ragionato sul codice e **non misurato sul telefono**, quindi la
     * verifica resta da fare: quello che è certo è che qui il richiamo vive quanto la
     * schermata, che è la sola durata sensata.
     * ⚠️ Il tipo si chiede all'immagine mostrata e il contratto si ricostruisce solo
     * quando cambia: un contratto nuovo a ogni ricomposizione rifarebbe la registrazione
     * sessanta volte al secondo.
     */
    val saveMime = shown?.mimeType ?: "image/*"
    val saveAs = remember(saveMime) { ActivityResultContracts.CreateDocument(saveMime) }
    val saver = rememberLauncherForActivityResult(saveAs) { target ->
        val from = source
        if (target != null && from != null) {
            scope.launch {
                val ok = context.contentResolver.openOutputStream(target)?.use { out ->
                    ImageActions.copyOriginalTo(context, from, out)
                } ?: false
                val said = if (ok) R.string.toast_saved else R.string.toast_save_failed
                Toast.makeText(context, said, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val ops = remember(source, saver, onEdit, onEditWith) {
        MenuOps(
            job = { job = it },
            share = { picture ->
                scope.launch {
                    if (!ImageActions.share(context, picture, source)) {
                        Toast.makeText(context, R.string.toast_copy_failed, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            },
            save = { picture -> saver.launch(ImageActions.fileName(picture, source)) },
            // ⚠️ Senza indirizzo non si fa niente e non si dice niente: la voce che chiama
            // questa funzione compare **solo** con un file del telefono davanti, quindi qui
            // il caso non capita, e un avviso sarebbe codice che nessuno può far girare.
            edit = { source?.let(onEdit) },
            editWith = { source?.let(onEditWith) },
            /*
             * ⚠️⚠️ **L'ANIMAZIONE SI LEGGE DA UNO STATO AGGIORNATO e non si cattura qui**:
             * questo blocco vive dentro un `remember`, quindi quello che cattura è il valore
             * di **quel** momento, e in quel momento `rememberAnimation` non ha ancora
             * finito di aprire il file: catturarla direttamente vorrebbe dire un `null` per
             * sempre, cioè l'esportazione del fotogramma che non funziona mai su nessuna
             * animazione. Il difetto non darebbe nessun errore: convertirebbe il file
             * intero, che è un esito plausibile.
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

    // ⚠️ Ricordato SENZA chiavi, e non per fotografia: nasce spento una volta sola,
    // quindi la primissima riga entra in dissolvenza e da lì in poi non si ricrea mai.
    val barState = remember { MutableTransitionState(false) }
    // ⚠️⚠️ **SU UN FILMATO LA RIGA SE NE VA, e non è una scelta di gusto: senza questa
    // condizione racconterebbe il FALSO.** `shown` è l'ultima fotografia vista e resta
    // apposta, perché è ciò che toglie il lampeggio fra una foto e l'altra; ma su un video
    // non arriva nessuna fotografia nuova a sostituirla, quindi la riga resterebbe lì a
    // dichiarare pixel, formato e peso di **un altro file**.
    barState.targetState = info.visible && shown != null && state !is ViewerState.Clip

    /*
     * ⚠️⚠️ **LO SFONDO SI DIPINGE QUI, e prima viveva dentro `ImageCanvas`: era LUI il
     * lampeggio che restava** (segnalazione dell'utente, 2026-08-29: *non ancora risolto,
     * anzi adesso lampeggia anche l'immagine*). La scacchiera esisteva solo nello stato
     * `Ready`, quindi per tutto il tempo del caricamento il fondo diventava il colore
     * liscio del tema e poi tornava indietro: un cambio di colore a tutto schermo a ogni
     * cambio di fotografia.
     * ⚠️⚠️ **E spiega perché la `0.42` sembrava aver peggiorato le cose**: da quando la
     * riga dei dettagli non sparisce più, il suo fondo semitrasparente **mostra** quel
     * cambio invece di andarsene prima che accada. Un difetto solo, che si vedeva in due
     * posti e sembrava due difetti.
     * ⚠️ La scacchiera è anche il motivo per cui non basta un colore di fondo sul `Box`:
     * dipinta qui, resta identica in tutti e tre gli stati, che è l'unica definizione
     * utile di 'non lampeggia'.
     */
    val density = LocalDensity.current
    val checkerPx = with(density) { CHECKER.toPx() }
    val lightGreys = when (settings.bgTheme) {
        BgTheme.LIGHT -> true
        BgTheme.DARK -> false
        BgTheme.AUTO -> MaterialTheme.colorScheme.background.luminanceIsLight()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawBackground(size, checkerPx, lightGreys, settings.bgType) }
    ) {
        when (state) {
            is ViewerState.Loading -> {
                PreviewThumb(source, settings)
                /*
                 * ⚠️⚠️ **L'ANELLO ASPETTA, e non è cortesia: comparire e sparire in un
                 * decimo di secondo È un lampeggio.** Fra una foto e l'altra del telefono
                 * la decodifica dura meno del tempo che serve a leggere l'anello, quindi
                 * fino alla `0.44` a ogni cambio di pagina compariva un cerchietto che se
                 * ne andava subito. Aspettando, sulle foto veloci non si vede mai, e resta
                 * dov'era davvero utile: la rete e le fotografie enormi.
                 * ⚠️ L'attesa vale anche per la percentuale, e va bene: un trasferimento
                 * che finisce in meno di mezzo secondo non aveva niente da raccontare.
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
                // ⚠️ Si offre di riprovare **solo se c'è un indirizzo da riprovare**: un
                // errore senza sorgente (la cartella che si è svuotata) non ha niente da
                // rifare, e un tasto che non può funzionare è peggio di nessun tasto.
                onRetry = onRetry.takeIf { source != null },
                modifier = Modifier.align(Alignment.Center)
            )
            is ViewerState.Ready -> {
                /*
                 * ⚠️⚠️ **L'ANIMAZIONE NON È UNO STATO A PARTE, ed è la scelta che salva zoom e
                 * panoramica**: una GIF resta una `Ready` con la sua `LoadedImage`, e
                 * l'animazione le passa **solo i pixel** del fotogramma corrente. Se fosse un
                 * ramo suo, come lo è `Clip` per i filmati, si perderebbero ingrandimento,
                 * trascinamento, tasselli e riquadro di riposo, cioè tutto quello che
                 * distingue un visualizzatore da un riproduttore.
                 * ⚠️ **Si apre più in su, dalla 1.21**, perché la guarda anche il dialogo di
                 * 'Converti/Esporta': la ragione sta là.
                 */
                ImageCanvas(
                    state.image, settings, source, folder, info, onStep, ops, inBin,
                    animation?.frame,
                    onSingleTap = { animation?.toggle() },
                    /*
                     * ⚠️⚠️ **IL TOCCO LUNGO FERMA LA RIPRODUZIONE, dalla 1.21** (richiesta
                     * dell'utente, 2026-09-01: *condizione: la pressione lunga e la comparsa
                     * del menu fermano la riproduzione*). Senza, 'Converti/Esporta' agirebbe
                     * su un fotogramma che nel frattempo è già cambiato, e il menu aperto
                     * coprirebbe un'immagine che continua a muoversi sotto.
                     * ⚠️ **Ferma e basta, non alterna**: chi apre il menu su un'animazione
                     * già in pausa non se la vede ripartire in faccia.
                     */
                    onHold = { animation?.pause() }
                )
                if (animation != null) {
                    AnimatedBar(
                        animation = animation,
                        counter = settings.animCounter,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            // ⚠️ **PRIMA TARATURA, DA GUARDARE SUL TELEFONO**: la riga dei
                            // dettagli può stare in basso, e allora i comandi le vanno
                            // sopra. L'altezza di quella riga non è una costante, quindi
                            // questo numero è una stima e non una misura.
                            .padding(
                                bottom = if (settings.infoPosition == InfoPosition.BOTTOM) {
                                    ANIM_OVER_INFO
                                } else {
                                    ANIM_LIP
                                }
                            )
                    )
                }
            }
            is ViewerState.Clip -> ClipStage(
                uri = state.uri,
                // La strisciata porta avanti e indietro solo se c'è una serie: fuori da una
                // cartella un filmato è un vicolo cieco, come una fotografia sola.
                stepping = folder?.seriesOrNull != null,
                onStep = onStep
            )
        }

        AnimatedVisibility(
            visibleState = barState,
            // Entra ed esce sfumando e basta: le due predefinite cambiano anche la
            // misura, e una riga che si apre a soffietto in fondo allo schermo sposta il
            // contatore, che è la cosa che si guarda mentre si sfoglia.
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(
                if (settings.infoPosition == InfoPosition.TOP) Alignment.TopCenter
                else Alignment.BottomCenter
            )
        ) {
            // ⚠️ L'ultima fotografia vista resta anche durante l'uscita: senza, la riga
            // si svuoterebbe mentre sta ancora sfumando via.
            shown?.let { picture ->
                /*
                 * ⚠️⚠️ **LO SFONDO STA FUORI DALLA DISSOLVENZA, ed è il lampeggio segnalato
                 * il 2026-08-30** (*mentre la dissolvenza del testo è perfetta, adesso lo
                 * sfondo delle info fa un piccolo flash*). Dentro, la `AnimatedContent`
                 * tiene **due** pannelli sovrapposti per tutta la transizione, e due veli
                 * semitrasparenti sovrapposti non fanno il velo di prima: a metà strada
                 * ciascuno vale `0.86 x 0.5 = 0.43`, e insieme coprono
                 * `1 - 0.57 x 0.57 = 0.675` invece di `0.86`. Lo sfondo si schiarisce di un
                 * quinto e torna, cioè lampeggia, mentre il testo dissolve benissimo.
                 * ⚠️ **È lo stesso rimedio della `0.44`**, quando lo sfondo della fotografia
                 * era dentro `ImageCanvas` e lampeggiava a ogni cambio di stato: quello che
                 * deve restare fermo si dipinge **fuori** da ciò che si anima. Chi
                 * rimettesse il velo dentro `DetailsPanel` per 'tenere il pannello in un
                 * pezzo solo' rimetterebbe il lampeggio.
                 * ⚠️ La larghezza è piena, quindi lo sfondo non cambia forma quando il testo
                 * cambia: senza `fillMaxWidth` seguirebbe la misura del contenuto, e con
                 * `using null` quella misura salta invece di animare.
                 *
                 * ⚠️ **La dissolvenza incrociata fra una riga e l'altra**, chiesta
                 * dall'utente il 2026-08-29 dopo aver confermato che il lampeggio non
                 * c'era più: il testo non cambia di scatto, il vecchio sfuma nel nuovo.
                 * ⚠️⚠️ **Senza trasformazione di misura (`using null`), e non è un
                 * dettaglio**: quella predefinita animerebbe anche la larghezza del
                 * riquadro, e una riga che si allarga mentre sfuma **sposta il
                 * contatore**, che è la cosa che si guarda proprio mentre si sfoglia. È
                 * la stessa ragione per cui l'entrata e l'uscita della riga intera sono
                 * di sola opacità.
                 * ⚠️ La chiave è la **fotografia** e non la percentuale: quella cambia a
                 * ogni fotogramma di una pinza, e incrociarla vorrebbe dire una
                 * dissolvenza al secondo mentre si ingrandisce.
                 */
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = PANEL_VEIL),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = picture,
                        transitionSpec = { fadeIn() togetherWith fadeOut() using null },
                        label = "dettagli"
                    ) { shownNow ->
                        DetailsPanel(
                            image = shownNow,
                            percent = info.percent,
                            // ⚠️⚠️ **Il silenzio non basta a dire perché**, ed è il difetto
                            // che ha fatto perdere DUE versioni sulla strisciata: senza
                            // serie il gesto non fa niente, e 'non fa niente' è identico a
                            // un gesto guasto. Qui l'esito arriva col suo motivo e la riga
                            // lo stampa.
                            // ⚠️ Solo per una foto di questo telefono: su un'immagine del
                            // web o di una chat 'non è nella galleria' è la normalità, non
                            // una notizia.
                            folder = folder.takeIf { source?.scheme?.lowercase() == "content" }
                        )
                    }
                }
            }
        }

        /*
         * ⚠️⚠️ **IL VELO DEL DOPPIO TOCCO, dalla 1.25** (richiesta dell'utente, 2026-09-02:
         * *alla visualizzazione della prima immagine dopo l'installazione*). È la contropartita
         * delle due voci dello zoom uscite dal menu: senza, il gesto resterebbe un segreto.
         * ⚠️ **Alla prima FOTOGRAFIA e non all'avvio**: su un filmato il doppio tocco non fa
         * niente, e su una schermata di caricamento si parlerebbe di un'immagine che non si
         * vede ancora. Da qui la condizione su `Ready`.
         * ⚠️ **Sta in fondo al `Box` della schermata**, dopo la barra delle info: un velo che
         * si lascia scavalcare da qualcosa non è un velo. E `matchParentSize` esiste solo qui
         * dentro, che è la ragione per cui `HintCentre` è un'estensione di `BoxScope`.
         */
        if (zoomHint && state is ViewerState.Ready) {
            HintCentre(
                text = stringResource(R.string.hint_zoom_tap),
                onDone = { scope.launch { Hint.ZOOM_TAP.remember(context) } }
            )
        }
    }

    // ⚠️ I dialoghi stanno FUORI dal riquadro dell'immagine e fuori dal menu, che è la
    // ragione per cui esistono in questo file e non là: sono finestre a sé, e devono
    // sopravvivere alla fotografia su cui agiscono. Vedi `perform`.
    FileJobDialogs(
        job = job,
        // ⚠️ Qui le impostazioni ci sono già, quindi i campi si leggono da loro: la griglia
        // invece se li fa passare, perché non le ha.
        fields = settings.factRows,
        onClose = { job = null },
        onRun = perform
    )
}

/**
 * Le richieste che il menu del tocco lungo **non esegue da sé**, e che passa a chi lo
 * contiene.
 *
 * ⚠️⚠️ **LA RAGIONE È UNA SOLA: il menu si chiude prima che finiscano.** Toccare una voce
 * chiama `onDismiss`, quindi il menu esce dalla composizione, e con lui se ne vanno il suo
 * ambito di coroutine e i suoi richiami registrati. Una condivisione si annullerebbe alla
 * prima sospensione, l'esito del selettore di sistema non tornerebbe a nessuno, e un
 * dialogo aperto da là sparirebbe insieme alla fotografia su cui agisce.
 * ⚠️ **Non ci finisce tutto, e la riga di confine è chiara**: quello che comincia e
 * finisce dentro il tocco (gli appunti, lo zoom, la riga dei dettagli) resta nel menu.
 * Qui sale ciò che **dura più del gesto**.
 */
private class MenuOps(
    val job: (FileJob) -> Unit,
    val share: (LoadedImage) -> Unit,
    val save: (LoadedImage) -> Unit,
    /**
     * 'Modifica': apre l'editor scelto, o chiede quale usare la prima volta.
     *
     * ⚠️ **Non prende la fotografia**, al contrario delle altre due: quello che si modifica
     * è il **file**, cioè `source`, che è già chiuso dentro questo contratto. Passare
     * l'immagine decodificata inviterebbe a modificare i pixel in memoria, che non è quello
     * che succede: l'editor riparte dal file.
     * ⚠️ **Sale qui per la ragione dichiarata sopra e per una in più**: cambia SCHERMATA, e
     * un cambio di schermata deciso dal menu smonterebbe il menu a metà chiamata.
     */
    val edit: () -> Unit,
    /**
     * Tocco lungo su 'Modifica': fa scegliere l'app adesso, e la scelta resta.
     *
     * ⚠️ **È una seconda funzione e non un parametro di [edit]**, perché il menu non deve
     * sapere che cosa distingue i due casi: lui riferisce **quale gesto** è arrivato, e chi
     * ha le impostazioni in mano decide. Vedi `ViewerViewModel.editWith`.
     */
    val editWith: () -> Unit,
    /**
     * 'Converti/Esporta': apre il dialogo che salva l'immagine in un altro formato.
     *
     * ⚠️ **Non prende niente**: il dialogo ha già in mano l'immagine e il suo indirizzo,
     * perché vive accanto a chi li tiene. Questa è solo la richiesta di aprirlo.
     */
    val convert: () -> Unit,
    /**
     * Tocco lungo su 'Info': apre il riquadrino della barra delle info.
     *
     * ⚠️ **Non prende niente e non decide niente**, come [convert]: il riquadro vive accanto
     * a chi ha le impostazioni in mano, e questa è solo la richiesta di aprirlo.
     */
    val bar: () -> Unit
)

/**
 * Il riquadrino della barra delle info, aperto dal tocco lungo su 'Info'.
 *
 * ⚠️⚠️ **LE STESSE DUE SCELTE DELLE IMPOSTAZIONI, e le stesse parole**: le stringhe sono
 * quelle di là (`settings_info_visible`, `settings_top`, `settings_bottom`), quindi le due
 * schermate non possono chiamare la stessa cosa in due modi. È il difetto che la 1.20 aveva
 * appena tolto, e riscriverlo qui a mano lo avrebbe rimesso in scena dalla porta di dietro.
 * ⚠️ **Si applica subito e si salva subito**, senza un tasto di conferma: era la richiesta
 * (*da qui si fa al volo, e si memorizza*), e un riquadro da due comandi con un 'Applica'
 * sarebbe un tocco in più per niente. 'Chiudi' chiude e basta.
 * ⚠️ **Le pastiglie restano toccabili con la barra spenta**, e non è una svista: chi la
 * riaccende trova la posizione che voleva invece di doverla ridire. Spegnerle vorrebbe dire
 * un comando che si accende e si spegne mentre lo si guarda.
 */
@Composable
private fun InfoBarPopup(
    settings: Settings,
    onChange: (Boolean, InfoPosition) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(),
        title = { Text(stringResource(R.string.settings_info_visible)) },
        /*
         * ⚠️⚠️ **TRE GETTONI E NON UN INTERRUTTORE PIÙ DUE, dalla 1.29** (richiesta
         * dell'utente, 2026-09-02: *per coerenza, applica lo stesso trattamento grafico a
         * tre gettoni anche per l'azione a pressione lunga su Info*). Sono esattamente le
         * tre scelte che la `1.27` ha messo nelle impostazioni: Disattivata, In alto, In
         * basso.
         * ⚠️ **La coerenza qui vale doppio**, perché questa scorciatoia e quella riga delle
         * impostazioni comandano **la stessa cosa**: due vesti diverse per un comando solo
         * fanno dubitare che siano due comandi.
         * ⚠️ Sotto restano due valori (acceso e lato), come nelle impostazioni: spegnendo la
         * barra e riaccendendola si ritrova il lato scelto. Vedi `InfoChoice` là.
         */
        text = {
            /*
             * ⚠️⚠️ **`FlowRow` E NON `Row`, e fino alla 1.33 era un `Row`: è il difetto che
             * SPEZZAVA 'In basso' DENTRO IL GETTONE.** Su un dialogo largo 280dp i tre
             * gettoni non ci stanno in fila, e un `Row` non manda nessuno a capo: stringe il
             * terzo e la sua parola si rompe a metà, *In / bass / o* (segnalato dall'utente
             * con una schermata, 2026-09-02). Con `FlowRow` il gettone che non ci sta scende
             * sulla riga sotto, intero.
             * ⚠️ **La stessa scelta era già in casa e questo era l'unico fuori**: la riga
             * omologa delle impostazioni usa `FlowRow` da quando è nata, e per la stessa
             * ragione (vedi `Choices` in `SettingsScreen.kt`). Le due schermate comandano la
             * stessa cosa con le stesse parole: dovevano anche disporle allo stesso modo.
             */
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val acceso = settings.infoVisible
                BarPick.entries.forEach { scelta ->
                    FilterChip(
                        selected = scelta.matches(acceso, settings.infoPosition),
                        onClick = { scelta.apply(settings, onChange) },
                        label = { Text(stringResource(scelta.label)) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pick_close)) }
        }
    )
}

/**
 * Le tre scelte della barra delle info nel pannellino del tocco lungo su 'Info'.
 *
 * ⚠️ **Non è `InfoChoice` di `SettingsScreen.kt`, e non si fondono**: quella lavora su un
 * `Settings` intero e ne restituisce uno nuovo, perché là si scrive tutto insieme; questa
 * chiama `onChange(acceso, lato)`, che è la firma di questo pannellino. Fondendole servirebbe
 * una firma comune a due schermate che non hanno lo stesso modo di salvare, e la parte
 * condivisa sarebbero tre righe.
 */
private enum class BarPick(val label: Int) {
    OFF(R.string.settings_off),
    TOP(R.string.settings_top),
    BOTTOM(R.string.settings_bottom);

    fun matches(on: Boolean, where: InfoPosition): Boolean = when (this) {
        OFF -> !on
        TOP -> on && where == InfoPosition.TOP
        BOTTOM -> on && where == InfoPosition.BOTTOM
    }

    fun apply(settings: Settings, onChange: (Boolean, InfoPosition) -> Unit) = when (this) {
        OFF -> onChange(false, settings.infoPosition)
        TOP -> onChange(true, InfoPosition.TOP)
        BOTTOM -> onChange(true, InfoPosition.BOTTOM)
    }
}

/**
 * Quello che la riga dei dettagli deve sapere, e che **non se ne va con la fotografia**.
 *
 * ⚠️ Due soli campi, e nessuno dei due è un dato della fotografia: sono la sua
 * **visibilità**, che l'utente comanda dal menu, e la **percentuale di zoom**, che cambia
 * sotto le dita. Tutto il resto la riga lo prende dall'immagine che le viene passata.
 */
@Stable
private class InfoBar {
    var visible by mutableStateOf(true)
    var percent by mutableFloatStateOf(1f)

    /**
     * Che cosa stanno facendo i tasselli a piena risoluzione, e `null` quando non
     * c'entrano (immagine non campionata, o lettura non ancora tentata).
     *
     * ⚠️ Sta QUI e non dentro `ImageCanvas` per la stessa ragione degli altri due: la
     * riga dei dettagli vive più a lungo della fotografia, e un dato tenuto di là
     * sparirebbe a ogni cambio di pagina proprio mentre lo si sta leggendo.
     */
    var tiles by mutableStateOf<String?>(null)
}

/**
 * Un FILMATO nel visualizzatore: il suo fotogramma, la durata e il tasto che lo apre.
 *
 * ⚠️⚠️ **PEZZO 1 DEI TRE, e qui non si riproduce niente**: si mostra quello che il sistema
 * ha già (la miniatura, cioè un fotogramma) e si consegna il file al lettore del telefono.
 * La riproduzione in casa è il pezzo 2, e prenderà **questo** posto: la sostituzione
 * riguarda il centro di questa funzione, non l'impalcatura attorno.
 * ⚠️⚠️ **LA STRISCIATA C'È, ed è la ragione per cui questa funzione non è solo un'immagine
 * e un tasto**: sfogliando una cartella mista si arriva su un filmato, e senza il gesto ci
 * si troverebbe **bloccati**, costretti a tornare alla griglia per riprendere da dopo. È il
 * difetto peggiore che questo pezzo poteva avere.
 * ⚠️ **Ma è una strisciata SEMPLICE, e la differenza si vede**: qui il dito non trascina la
 * pagina, non c'è la vicina che entra dal bordo e non c'è la resistenza al capolinea. Tutta
 * quella macchina vive in `ImageCanvas` e poggia su una fotografia decodificata, che qui non
 * c'è. Il baratto è dichiarato: sul filmato il passaggio è secco.
 * ⚠️ **La soglia è la stessa frazione di larghezza dell'altra strisciata** (un quinto), così
 * il gesto ha la stessa taratura in tutti e due i posti: due numeri diversi si sentirebbero
 * come due gesti diversi.
 */
@Composable
@androidx.annotation.OptIn(UnstableApi::class)
private fun ClipStage(uri: Uri, stepping: Boolean, onStep: (Int) -> Unit) {
    val context = LocalContext.current
    val model = remember(uri, context) { Thumbs.request(context, uri) }
    val poster = rememberAsyncImagePainter(model = model)

    /*
     * ⚠️⚠️ **UN LETTORE SOLO PER TUTTA LA VITA DELLA SCHERMATA, e non uno per filmato**:
     * costruirne uno a ogni indirizzo vorrebbe dire buttare via la superficie video e
     * ricrearla a ogni passo, cioè un lampo nero a ogni strisciata; e un lettore non
     * rilasciato tiene un decodificatore hardware, che su un telefono sono pochi.
     * ⚠️⚠️ **IL FUOCO AUDIO SI CHIEDE, e non è cortesia**: senza, un filmato aperto per
     * sbaglio suonerebbe **sopra** la musica di un'altra app invece di metterla in pausa, e
     * al ritorno la musica non ripartirebbe. `handleAudioFocus` fa la cosa giusta in tutti e
     * due i versi. ⚠️ E `setHandleAudioBecomingNoisy` mette in pausa quando si sfilano le
     * cuffie, che è quello che ogni lettore fa e che nessuno nota finché manca.
     */
    val player = remember(context) {
        ExoPlayer.Builder(context)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    /*
     * ⚠️⚠️ **NON PARTE DA SÉ, ed è una scelta**: su questo stesso posto si arriva in due
     * modi, toccando un filmato (dove partire sarebbe giusto) e **strisciando** da una
     * fotografia (dove un audio che esplode è una sorpresa sgradevole). Fra i due, il caso
     * da non rovinare è il secondo, perché è quello in cui la persona non ha chiesto niente.
     * Chi tocca paga un tocco in più, e il tasto è grande in mezzo allo schermo.
     */
    LaunchedEffect(uri, player) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    /*
     * ⚠️⚠️ **SI FERMA QUANDO L'APP VA IN FONDO, e senza questo l'audio continuerebbe** a
     * suonare da un'app che non si vede più: è il difetto che fa disinstallare un
     * visualizzatore. `ON_STOP` e non `ON_PAUSE`, così un dialogo di sistema sopra il video
     * non lo interrompe.
     * ⚠️ **Il rilascio sta nello stesso posto**: chi crea rilascia, e un `DisposableEffect`
     * separato sarebbe una seconda cosa da ricordare.
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
    var knob by remember { mutableStateOf<Knob?>(null) }
    // ⚠️ La chiave è il valore: ogni movimento del dito rifà l'attesa, quindi l'indicatore
    // sparisce un attimo dopo l'ULTIMO ritocco e non a metà del gesto.
    LaunchedEffect(knob) {
        if (knob != null) {
            delay(KNOB_LINGER)
            knob = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            /*
             * ⚠️⚠️ **NERO PURO SEMPRE, e copre la scacchiera di chi sta sotto** (richiesta
             * dell'utente, 2026-08-31). Non è una preferenza di gusto: le bande sopra e
             * sotto un filmato fanno parte di quello che si guarda, e qualunque altra tinta
             * le trasforma in una cornice che sfarfalla intorno all'immagine. È anche il
             * fondo di ogni lettore video esistente, quindi è la cosa che l'occhio si
             * aspetta.
             * ⚠️ **Vale a prescindere dal tema e dalla tinta scelta per le fotografie**: là
             * la scacchiera serve a **mostrare la trasparenza**, che è un'informazione; un
             * filmato non è mai trasparente, quindi quel fondo non racconterebbe niente e
             * disturberebbe soltanto.
             */
            .background(Color.Black)
            .pointerInput(uri, stepping) {
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
                        // ⚠️ Il lato si decide da DOVE IL DITO SI POSA, non da dove
                        // arriva: chi comincia a sinistra e sconfina a destra sta ancora
                        // regolando la luminosità, e cambiare manopola a metà gesto
                        // sarebbe una sorpresa.
                        onLeft = at.x < middle
                    },
                    // ⚠️ Il passo si dà alla FINE del gesto e non appena la soglia è
                    // superata: durante il trascinamento il dito può tornare indietro, e
                    // cambiare pagina a metà strada toglierebbe a chi striscia la
                    // possibilità di ripensarci, che l'altra strisciata concede.
                    onDragEnd = {
                        if (axis != Axis.SIDEWAYS || !stepping) return@detectDragGestures
                        if (travelled <= -threshold) onStep(1)
                        else if (travelled >= threshold) onStep(-1)
                    },
                    onDrag = { _, delta ->
                        /*
                         * ⚠️⚠️ **L'ASSE SI DECIDE AL PRIMO MOVIMENTO E POI NON CAMBIA**, ed
                         * è la sola cosa che tiene separati i tre gesti: senza il blocco,
                         * una strisciata orizzontale un po' storta cambierebbe fotografia
                         * **e** alzerebbe il volume, e una verticale un po' storta
                         * salterebbe al filmato dopo.
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
                            return@detectDragGestures
                        }
                        travelled += delta.y
                        // ⚠️ Il segno è rovesciato apposta: in su alza, come su ogni
                        // lettore, mentre le coordinate dello schermo crescono in giù.
                        val moved = (started - travelled / reach).coerceIn(0f, 1f)
                        if (onLeft) Knobs.setBrightness(activity, moved) else Knobs.setVolume(context, moved)
                        knob = Knob(onLeft, moved)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        /*
         * ⚠️⚠️ **LA SUPERFICIE NON ASCOLTA NESSUN TOCCO, ed è la ragione per cui la
         * strisciata qui sopra funziona**: `PlayerSurface` è una `SurfaceView` dentro un
         * `AndroidView`, senza ascoltatori. Con `PlayerView` di `media3-ui`, che i comandi
         * ce li ha già, il tocco sarebbe finito a lui e il gesto per cambiare fotografia
         * non sarebbe mai arrivato qui.
         */
        /*
         * ⚠️⚠️ **LE PROPORZIONI LE TIENE QUESTO MODIFICATORE, e senza di lui il filmato era
         * STIRATO** (riscontro dell'utente sulla `0.86`: in verticale un video orizzontale
         * riempiva tutto lo schermo deformandosi). `PlayerSurface` da sola prende lo spazio
         * che le si dà, e `fillMaxSize` gliene dava di ogni forma: la superficie non sa
         * nulla del video che ci finisce sopra. `resizeWithContentScale` legge la misura
         * vera del filmato da [rememberPresentationState] e ritaglia il riquadro.
         * ⚠️ Lo stesso modificatore va **anche al fotogramma di copertura** qui sotto, o
         * all'apertura si vedrebbe la miniatura in un riquadro e il video in un altro.
         */
        val scaled = Modifier.resizeWithContentScale(ContentScale.Fit, shown.videoSizeDp)
        PlayerSurface(player = player, modifier = scaled)

        /*
         * ⚠️⚠️ **IL FOTOGRAMMA COPRE LA SUPERFICIE FINCHÉ IL VIDEO NON HA DA MOSTRARE
         * NIENTE** (`coverSurface`), ed è la continuità con la `0.83`: è la stessa miniatura
         * che la griglia ha già in memoria, quindi al posto del rettangolo nero
         * dell'apertura si vede il filmato fermo. Senza, ogni apertura comincerebbe con un
         * lampo nero, che è lo stesso difetto che le anteprime delle fotografie esistono per
         * togliere.
         */
        if (shown.coverSurface) {
            Image(
                painter = poster,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = scaled
            )
        }

        // ⚠️ L'indicatore sta sopra i comandi e sotto niente: è l'unica risposta visibile a
        // un gesto che non tocca nessun tasto, e senza di lui la manopola sarebbe al buio.
        knob?.let { KnobBadge(it) }

        // ⚠️ Il tasto sta al centro e sparisce mentre si guarda: vedi [ClipKeys].
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
 * Su quale asse si è mosso il dito sopra un filmato, deciso **una volta sola** per gesto.
 *
 * ⚠️ Tre valori e non un booleano: fra il dito che si posa e il primo movimento c'è un
 * momento in cui l'asse non si sa ancora, e un booleano avrebbe dovuto fingere di saperlo.
 */
private enum class Axis { UNDECIDED, SIDEWAYS, UPRIGHT }

/** Quale manopola si sta girando e a che punto sta, da 0 a 1. */
private data class Knob(val brightness: Boolean, val value: Float)

/**
 * L'indicatore che dice che cosa sta cambiando mentre il dito scorre in verticale.
 *
 * ⚠️⚠️ **Senza di lui il gesto sarebbe al buio**: la luminosità si vede da sé, ma il volume
 * di un filmato muto no, e in entrambi i casi non si saprebbe **quale** delle due manopole si
 * è presa finché non succede qualcosa. Un numero e un'icona bastano, e spariscono da soli.
 * ⚠️ Niente descrizione per l'icona: il valore accanto è già testo, e un lettore di schermo
 * annuncerebbe due volte la stessa cosa mentre il dito è ancora sul vetro.
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
            imageVector = if (knob.brightness) Icons.Outlined.LightMode else Icons.AutoMirrored.Outlined.VolumeUp,
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
 * ⚠️⚠️ **SCRITTI IN CASA E NON PRESI DA `PlayerView`**, e la ragione sta nella nota della
 * dipendenza: quelli sono una `View` che si prende i tocchi, e con loro la strisciata per
 * cambiare fotografia non arriverebbe mai. Sono un tasto e una barra: il prezzo di scriverli è basso,
 * quello di perdere il gesto no.
 * ⚠️⚠️ **SPARISCONO MENTRE IL FILMATO VA, e tornano toccando**: un tasto grande in mezzo
 * allo schermo sopra un video in corso copre proprio quello che si sta guardando. In pausa
 * restano, perché in pausa la cosa da fare è ripartire.
 * ⚠️ **La barra si trascina** (`Slider`), perché saltare avanti è la sola cosa che si chiede
 * a un lettore oltre a partire e fermarsi; e il tempo si legge accanto, perché una barra
 * senza numeri non dice a che minuto si è.
 */
@Composable
private fun BoxScope.ClipKeys(
    playing: Boolean,
    onPlayPause: () -> Unit,
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    var visible by remember { mutableStateOf(true) }
    // ⚠️ La chiave è `playing`: ripartendo si nasconde dopo la pausa, e mettendo in pausa
    // ricompare subito. Un timer solo, senza chiave, avrebbe continuato a contare.
    LaunchedEffect(playing) {
        if (playing) {
            delay(CLIP_KEYS_LINGER)
            visible = false
        } else {
            visible = true
        }
    }
    // ⚠️ Il tocco che RICHIAMA i comandi sta qui e non sulla superficie: è un riquadro
    // trasparente sopra il video, e vive solo quando i comandi sono nascosti, così quando
    // ci sono non ruba il tocco al tasto.
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

    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .size(CLIP_KEY)
                .clip(CircleShape)
                .background(CLIP_INK)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(
                    if (playing) R.string.clip_pause else R.string.clip_play
                ),
                tint = Color.White,
                modifier = Modifier.size(CLIP_GLYPH)
            )
        }
    }

    AnimatedVisibility(
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
            // ⚠️ **Durata sconosciuta**: prima che il lettore l'abbia letta vale
            // `C.TIME_UNSET`, che è un numero negativo enorme. Passarlo a `Slider` darebbe
            // un intervallo rovesciato, quindi finché non si sa la barra non c'è.
            if (duration > 0) {
                Slider(
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

/** Il nero del tasto: lo stesso mestiere della targhetta in griglia, sopra una fotografia. */
private val CLIP_INK = Color(0x99000000)

/** Quanto i comandi si staccano dal bordo. */
private val CLIP_GAP = 12.dp

/** Il respiro sopra e sotto la barra, e il distacco fra i suoi pezzi. */
private val CLIP_LIP = 8.dp

/**
 * Quanto restano i comandi dopo che il filmato è partito.
 *
 * ⚠️ Due secondi e mezzo: abbastanza per vedere dov'è la barra, poco perché non dia
 * fastidio. Da rivedere sul telefono, non a naso.
 */
private const val CLIP_KEYS_LINGER = 2500L

/**
 * Quanto resta l'indicatore della manopola dopo l'ultimo ritocco del dito.
 *
 * ⚠️ Molto meno dei comandi: quello risponde a un gesto **in corso**, e appena il dito si
 * stacca la sua ragione di esistere è finita. Restare più a lungo lo farebbe leggere come un
 * pannello invece che come un riscontro.
 */
private const val KNOB_LINGER = 700L

/**
 * Quanta parte dell'altezza dello schermo copre l'intera corsa di una manopola.
 *
 * ⚠️ **Il 70% e non tutto**, perché il dito non parte mai dal bordo e non arriva mai
 * all'altro: chiedendo lo schermo intero, il massimo e il minimo sarebbero irraggiungibili
 * senza due gesti. ⚠️ Da rivedere col pollice, come ogni numero di gesto.
 */
private const val KNOB_SPAN = 0.7f

/** L'arrotondamento dell'indicatore della manopola: la stessa aria dei comandi. */
private val KNOB_ROUND = 16.dp

/**
 * Ogni quanto si aggiorna il tempo scritto accanto alla barra.
 *
 * ⚠️ Mezzo secondo e non ogni fotogramma: il numero si legge in secondi, quindi chiedere di
 * più vorrebbe dire ricomporre due volte per ogni cifra che cambia.
 */
private const val TICK_MS = 500L

/**
 * La MINIATURA mentre la fotografia vera si decodifica.
 *
 * ⚠️⚠️ **NON è un compromesso sulla qualità: è quello che si vede PRIMA di averla.**
 * Aprire una foto da 30 megapixel costa un tempo che si sente, e la strisciata da una
 * all'altra lo paga a ogni passo; questa è la stessa miniatura che la griglia ha già
 * decodificato, quindi nella maggior parte dei casi è già in memoria e compare
 * nell'istante del gesto. Quando la fotografia è pronta la sostituisce, intera.
 * ⚠️ Solo per gli indirizzi LOCALI: per un URL remoto non c'è nessuna miniatura da
 * chiedere al telefono, e questo caricatore non parla con la rete apposta.
 */
@Composable
private fun PreviewThumb(source: Uri?, settings: Settings) {
    val local = source?.scheme?.lowercase() == "content" || source?.scheme?.lowercase() == "file"
    if (source == null || !local) return
    Preview(source, settings, Modifier.fillMaxSize())
}

/**
 * Una miniatura disegnata **grande quanto sarà la fotografia che la sostituisce**.
 *
 * ⚠️⚠️ **QUESTO ERA IL SALTO DELL'IMMAGINE, e la regola vecchia lo garantiva invece di
 * evitarlo.** La miniatura seguiva l'impostazione *ingrandisci le immagini piccole*: a
 * interruttore spento `ContentScale.Inside` non ingrandisce, quindi una miniatura da 512
 * px restava 512 px in mezzo a uno schermo da 1080, e all'arrivo della fotografia vera
 * (disegnata adattata allo schermo) l'immagine **raddoppiava di colpo**. Succedeva a ogni
 * cambio di pagina.
 * ⚠️ Il ragionamento vecchio non era sbagliato, era applicato alla cosa sbagliata:
 * 'non ingrandire' è una regola sulla **fotografia**, e applicarla al suo sostituto
 * produce esattamente lo scarto che voleva evitare.
 *
 * ⚠️⚠️ **DALLA `0.56` LA MISURA SI CALCOLA INVECE DI ESSERE INDOVINATA, e la stima di
 * prima aveva un buco largo mezzo schermo.** Fino alla `0.55` si deduceva dalla miniatura
 * se l'originale fosse 'grande': arrivata a 512 px, l'originale era più grande di 512, e si
 * adattava alla vista. È un'informazione da **un bit**, e sbagliava per tutta la fascia fra
 * i 512 px e la larghezza dello schermo: con l'ingrandimento spento, un'immagine da 900 px
 * su uno schermo da 1440 si vedeva prima adattata a 1440 e poi rimpiccioliva a 900
 * all'arrivo della fotografia. L'utente l'ha segnalato il 2026-08-30, e su uno schermo
 * QHD+ quella fascia comprende quasi tutte le immagini piccole, cioè proprio quelle per cui
 * l'interruttore si spegne.
 * ⚠️ **Adesso la misura finale si calcola con la stessa formula di `ImageCanvas`**, e
 * l'unico dato che mancava lo dà [Pixels]: il lato lungo del file. Da lì e dalle
 * proporzioni della miniatura escono le due dimensioni vere, e da quelle la misura a
 * riposo. ⚠️ Chi tocca `restScale` di là tocchi anche `plannedSize` di qua: sono la stessa
 * regola scritta due volte, e l'unico modo di accorgersi che divergono è vedere di nuovo
 * l'immagine saltare.
 * ⚠️ **La stima vecchia resta come RIPIEGO**, e non è codice morto: per il tempo che serve
 * a leggere l'intestazione, e per sempre su ciò che non si riesce ad aprire, è ancora la
 * cosa migliore che si sappia. Il ripiego si vede solo alla **prima** apertura di una
 * fotografia; sfogliando, la vicina ha già scaldato la misura mentre entrava dal bordo.
 */
@Composable
private fun Preview(uri: Uri, settings: Settings, modifier: Modifier) {
    val context = LocalContext.current

    /*
     * ⚠️⚠️ **IL SOSTITUTO LETTO SUBITO, ED È QUESTO CHE TOGLIE IL LAMPEGGIO RIMASTO**
     * (segnalazione dell'utente, 2026-08-29: *l'immagine lampeggia ancora*, dopo che la
     * `0.44` aveva già tolto quello del fondo). `AsyncImagePainter` parte da una coroutine
     * e non disegna niente al primo fotogramma, anche quando l'immagine è già in memoria:
     * verificato sul bytecode di Coil, non supposto. Qui la si legge **in composizione**,
     * quindi c'è già quando il fotogramma si disegna. Il perché la cosa nasca a ogni
     * cambio di pagina sta accanto a `Thumbs.note`.
     * ⚠️ Sotto e non al posto: quando il pittore vero arriva disegna **la stessa
     * immagine, alla stessa scala**, quindi il passaggio non si vede. Non è una
     * dissolvenza, è una sovrapposizione, ed è il motivo per cui non serve nessuna
     * animazione.
     */
    val standIn = remember(uri, context) { Thumbs.cached(context, uri)?.asPainter(context) }

    val painter = rememberAsyncImagePainter(
        // ⚠️ La STESSA richiesta della griglia, misura compresa: è così che questa
        // immagine è già in memoria invece di essere chiesta di nuovo. Una misura
        // diversa sarebbe una chiave diversa, cioè un'altra generazione.
        model = remember(uri, context) { Thumbs.request(context, uri) },
        // ⚠️ La chiave si registra QUI, cioè nel posto in cui Coil la dice: `SuccessResult`
        // la porta con sé, e ricavarla altrove vorrebbe dire ricostruirla a mano, che è
        // esattamente il modo di sbagliarla in silenzio la prima volta che Coil cambia
        // come la compone.
        onState = { st ->
            if (st is AsyncImagePainter.State.Success) Thumbs.note(uri, st.result.memoryCacheKey)
        }
    )

    // ⚠️ La misura si prende dal pittore vero se ce l'ha, se no dal sostituto: sono la
    // stessa immagine, ma per un fotogramma solo uno dei due la conosce, e da lì dipende
    // la scala. Prenderla dal solo pittore vero rimetterebbe il salto che questa
    // funzione esiste per togliere.
    val seen = painter.intrinsicSize.takeIf { it.isSpecified }
        ?: standIn?.intrinsicSize
        ?: Size.Unspecified

    /*
     * ⚠️ **Si legge dalla cache MENTRE SI COMPONE, e solo se manca si va a leggerla**: nel
     * caso che conta, cioè lo sfoglio, la vicina l'ha già chiesta entrando dal bordo, e a
     * pagina girata è qui. L'effetto serve al primo ingresso, dove costa un'intestazione.
     * ⚠️ Zero e non `null` per la stessa ragione per cui `Size.Unspecified` esiste: è
     * 'non lo so', e un lato lungo di zero non è una misura possibile.
     */
    var longSide by remember(uri) { mutableIntStateOf(Pixels.known(uri) ?: 0) }
    LaunchedEffect(uri) {
        if (longSide == 0) longSide = Pixels.longSideOf(context, uri) ?: 0
    }

    val density = LocalDensity.current

    /*
     * ⚠️ **La stessa vista di `ImageCanvas`**, che misura allo stesso modo dentro lo stesso
     * contenitore: è questo a garantire che le due scale coincidano invece di somigliarsi.
     */
    BoxWithConstraints(modifier = modifier) {
        val planned = plannedSize(
            seen = seen,
            longSide = longSide,
            viewWidth = constraints.maxWidth.toFloat(),
            viewHeight = constraints.maxHeight.toFloat(),
            settings = settings,
            density = density.density
        )

        // Il ripiego di quando la misura vera non si sa ancora: vedi la nota sulla
        // funzione. Un bit di informazione, e sbaglia in una fascia sola.
        val fromBig = seen.isSpecified &&
            (seen.width >= Thumbs.PX - 1f || seen.height >= Thumbs.PX - 1f)

        val scale = if (planned != null || settings.fitGrow || fromBig) {
            ContentScale.Fit
        } else {
            ContentScale.Inside
        }
        // ⚠️ Con la misura nota il riquadro ha **già** le proporzioni giuste, quindi
        // `Fit` qui dentro non ridimensiona niente: serve solo a non deformare se
        // l'arrotondamento in dp sposta il riquadro di mezzo pixel.
        val frame = if (planned == null) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .align(Alignment.Center)
                .size(
                    with(density) { planned.width.toDp() },
                    with(density) { planned.height.toDp() }
                )
        }

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

/**
 * Quanto sarà grande la fotografia quando arriverà, o `null` se non lo si può ancora dire.
 *
 * ⚠️⚠️ **È LA FORMULA DI `ImageCanvas`, riscritta sui dati del file invece che su quelli
 * del bitmap**, e le due coincidono per algebra e non per somiglianza: là la misura vale
 * `larghezzaBitmap * min(adattamento, campionamento * k)`, e siccome
 * `larghezzaBitmap * campionamento` **è** la larghezza del file, il campionamento sparisce.
 * Resta `min(adattato alla vista, pixel del file * k)`, che è quello che si calcola qui.
 * Quindi questa funzione non ha bisogno di sapere se la fotografia verrà ridotta per
 * entrare in memoria, ed è la ragione per cui può rispondere prima che venga aperta.
 *
 * ⚠️ **Le due dimensioni vengono da DUE fonti diverse apposta**: il lato lungo dal file
 * (l'unica cosa che la miniatura non sa) e le proporzioni dalla miniatura (l'unica cosa che
 * l'intestazione non sa dire senza sbagliare sulle foto ruotate). Il perché sta in [Pixels].
 */
private fun plannedSize(
    seen: Size,
    longSide: Int,
    viewWidth: Float,
    viewHeight: Float,
    settings: Settings,
    density: Float
): Size? {
    if (longSide <= 0 || viewWidth <= 0f || viewHeight <= 0f) return null
    if (!seen.isSpecified || seen.width <= 0f || seen.height <= 0f) return null
    val ratio = seen.width / seen.height
    val wide = ratio >= 1f
    val pixelWidth = if (wide) longSide.toFloat() else longSide * ratio
    val pixelHeight = if (wide) longSide / ratio else longSide.toFloat()
    val fit = min(viewWidth / pixelWidth, viewHeight / pixelHeight)
    // ⚠️ Senza il campionamento, che qui non serve: vedi la nota sulla funzione.
    val oneToOne = if (settings.scaleMode == ScaleMode.PHYSICAL) 1f else density
    val rest = if (settings.fitGrow) fit else min(fit, oneToOne)
    return Size(pixelWidth * rest, pixelHeight * rest)
}

/** Un pezzo nitido e il rettangolo, in coordinate viste, che occupa nella fotografia. */
private data class SharpTile(val bitmap: ImageBitmap, val area: PixelRect)

/** Il lettore di regioni una volta che si è **provato** ad aprirlo. Vedi il suo uso. */
private data class RegionHolder(val reader: RegionSource?)

/**
 * Quale pezzo leggere adesso, e con quanto campionamento.
 *
 * ⚠️⚠️ **SOPRA LA SCALA 1 E NON PRIMA**, ed è il confine esatto oltre il quale il bitmap
 * di base viene **ingrandito**: sotto, un pixel del bitmap copre meno di un pixel di
 * schermo e il dettaglio che manca non si vedrebbe comunque. È anche la ragione per cui
 * il campionamento chiesto qui risulta sempre più fine di quello del bitmap di base:
 * sono la stessa disuguaglianza scritta in due modi.
 *
 * ⚠️ **Il campionamento si arrotonda per DIFETTO** (la potenza di due immediatamente
 * sotto): per eccesso si leggerebbe meno dettaglio di quello che lo schermo può mostrare,
 * cioè si farebbe tutto questo lavoro per restare sfocati. Il prezzo è un pezzo che può
 * venire fino al doppio del necessario per lato, e per quello c'è il tetto.
 *
 * ⚠️ Il tetto sui pixel non è prudenza generica: senza, un ingrandimento appena sopra la
 * soglia su una fotografia enorme chiederebbe un pezzo grande quanto tutto lo schermo
 * moltiplicato per quattro, cioè una decina di volte la memoria del bitmap che sta già
 * mostrando. Quando alzare il campionamento per rientrare lo porta al livello del bitmap
 * di base, tanto vale non leggere niente.
 */
private suspend fun sharpen(
    reader: RegionSource,
    baseWidth: Float,
    baseHeight: Float,
    viewWidth: Float,
    viewHeight: Float,
    scale: Float,
    offset: Offset
): Sharpening {
    if (scale <= 1f) return Sharpening.None("no tile: zoom")
    val perPixel = baseWidth / reader.width
    if (perPixel <= 0f) return Sharpening.None("no tile: base")

    // Da schermo a coordinate viste: l'inversa della formula del `graphicsLayer`.
    fun seenX(screen: Float) = ((screen - viewWidth / 2f - offset.x) / scale + baseWidth / 2f) / perPixel
    fun seenY(screen: Float) = ((screen - viewHeight / 2f - offset.y) / scale + baseHeight / 2f) / perPixel

    val area = PixelRect(
        floor(seenX(0f)).toInt().coerceIn(0, reader.width),
        floor(seenY(0f)).toInt().coerceIn(0, reader.height),
        ceil(seenX(viewWidth)).toInt().coerceIn(0, reader.width),
        ceil(seenY(viewHeight)).toInt().coerceIn(0, reader.height)
    )
    if (area.width() <= 0 || area.height() <= 0) return Sharpening.None("no tile: area")

    val baseSample = (reader.width / baseWidth).roundToInt().coerceAtLeast(1)
    // ⚠️ Il tetto si calcola sulla VISTA e non è più un numero scritto a mano: il perché,
    // e il conto che il numero fisso sbagliava, stanno accanto a `TILE_SCREENFULS`.
    val cap = maxOf(MIN_TILE_PIXELS, viewWidth.toLong() * viewHeight.toLong() * TILE_SCREENFULS)
    // Il campionamento che si vorrebbe, prima che il tetto lo alzi: i due si distinguono
    // perché dicono due cose diverse a chi legge la diagnostica, e distinguerli è l'unico
    // modo di sapere se un giorno il tetto tornasse a essere il problema.
    val ideal = powerOfTwoAtMost(1f / (scale * perPixel))
    var sample = ideal
    while ((area.width().toLong() / sample) * (area.height().toLong() / sample) > cap) {
        sample *= 2
    }
    if (sample >= baseSample) {
        return Sharpening.None(if (ideal >= baseSample) "no tile: gain" else "no tile: cap")
    }

    val tile = reader.tile(area, sample) ?: return Sharpening.None("no tile: read")
    return Sharpening.Done(SharpTile(tile.asImageBitmap(), area))
}

/**
 * L'esito di una lettura a piena risoluzione, col **motivo** quando non se ne fa niente.
 *
 * ⚠️⚠️ **IL MOTIVO ESISTE PERCHÉ IL SILENZIO NON DICE PERCHÉ, ed è la lezione già pagata
 * dallo sfoglio**: 'non succede niente' è identico fra una funzione rotta, una funzione
 * che ha deciso di non fare niente e un formato che non si sa rileggere. La riga dei
 * dettagli lo stampa, come stampa l'esito della ricerca della cartella, ed è così che la
 * `0.49` ha potuto nominare il difetto invece di indovinarlo.
 */
private sealed interface Sharpening {
    data class Done(val tile: SharpTile) : Sharpening
    data class None(val why: String) : Sharpening
}

/** La potenza di due immediatamente sotto, e mai meno di uno. */
private fun powerOfTwoAtMost(value: Float): Int =
    if (value < 2f) 1 else Integer.highestOneBit(value.toInt())

/**
 * La fotografia vicina, mentre si sfoglia col dito.
 *
 * ⚠️ Lo spostamento arriva come **funzione** e si legge dentro `graphicsLayer`: preso
 * come valore, ogni pixel di trascinamento sarebbe una ricomposizione di questo
 * composable, cioè il contrario della fluidità che deve dare.
 * ⚠️ È la stessa `Preview` dell'anteprima, e deve esserlo: la vicina che scivola dentro
 * e l'anteprima che resta al centro subito dopo sono lo stesso disegno in due istanti
 * consecutivi, e se si scalassero in modo diverso il cambio di pagina avrebbe un salto
 * proprio nel punto in cui deve essere invisibile.
 */
@Composable
private fun Neighbour(uri: Uri?, settings: Settings, dx: () -> Float) {
    if (uri == null) return
    Preview(
        uri = uri,
        settings = settings,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationX = dx() }
    )
}

/**
 * A che punto è l'apertura.
 *
 * ⚠️⚠️ **DETERMINATO SOLO MENTRE SI SCARICA, e a cento torna a girare.** Un'immagine
 * presa dalla rete ha due attese diverse: il trasferimento, che si può contare, e la
 * decodifica, che no. Lasciando l'anello pieno al 100% durante la seconda si vedrebbe
 * una barra ferma a fine corsa, che è il modo classico di sembrare bloccati; il giro
 * indeterminato dice la verità, cioè 'sta lavorando e non so quanto manca'.
 * ⚠️ Per un file locale non arriva nessun numero, quindi qui si vede quello che si
 * vedeva prima: non c'è nessuna attesa da raccontare, e una barra che salta da zero a
 * cento in un fotogramma sarebbe rumore.
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
 * ⚠️ **Il tasto esiste per la rete**: un `403` di passaggio, una galleria, un server
 * lento. Senza, l'unica via era il gesto Indietro e tutto il giro dell'indirizzo da
 * capo, che con un URL preso dagli appunti vuol dire ritrovarlo.
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
     * Il fotogramma da disegnare al posto dell'immagine ferma, quando è animata.
     *
     * ⚠️⚠️ **SOSTITUISCE SOLO I PIXEL, NON LA GEOMETRIA, ed è il motivo per cui basta un
     * parametro**: zoom, panoramica, riquadro di riposo e tasselli si calcolano tutti su
     * `image`, e i fotogrammi di un'animazione hanno per forza la stessa misura della sua
     * tela. Passare di qui un'immagine di misura diversa spezzerebbe quei conti in silenzio.
     * ⚠️ **`null` è il caso normale**: una fotografia ferma non sa nemmeno che questo
     * parametro esiste.
     */
    frame: ImageBitmap? = null,
    /**
     * Che cosa fa un tocco solo in mezzo allo schermo, e di serie **niente**.
     *
     * ⚠️ Arriva da fuori perché la cosa da fare la sa il chiamante e non questa tela: sopra
     * un'immagine animata mette in pausa, e sopra una ferma non c'è niente da fare. Vedi
     * `onSingleTap` in [detectViewerGestures] per il ritardo che comporta.
     */
    onSingleTap: () -> Unit = {},
    /**
     * Che cosa fare **prima** che il tocco lungo apra il menu, e di serie niente.
     *
     * ⚠️ Serve a fermare un'animazione: la tela non sa che l'immagine si muove, quindi la
     * cosa da fare la sa il chiamante, come per [onSingleTap].
     */
    onHold: () -> Unit = {}
) {
    val density = LocalDensity.current
    // Vedi [withHaptics]: qui il gesto non passa da `combinedClickable`, quindi la
    // vibrazione del tocco lungo si chiama a mano.
    val haptics = LocalHapticFeedback.current

    // ⚠️ Lo sfondo NON si dipinge più qui: vedi `ViewerScreen`, e la ragione per cui
    // spostarlo era l'unico modo di togliere il lampeggio.
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
         * Di quanto il bitmap è più piccolo del FILE, cioè il campionamento subito
         * all'apertura: **1** per la stragrande maggioranza delle immagini, 2 o 4 per
         * quelle che non entravano intere in memoria.
         */
        val sampleFactor = (image.pixelWidth / imageWidth).coerceAtLeast(1f)

        /**
         * What '100%' is on THIS screen. Compose lays the picture out in device
         * pixels, so a scale of 1 already means one pixel of the bitmap per pixel of
         * the screen: that is [ScaleMode.PHYSICAL]. Asking instead for one pixel
         * per LAYOUT pixel means scaling by the screen's density, which on a phone
         * is two or three.
         *
         * ⚠️⚠️ **E MOLTIPLICATO PER IL CAMPIONAMENTO, che è la correzione della 0.39.**
         * Un pixel del bitmap non è un pixel del file quando il file è stato ridotto per
         * entrare in memoria: su una foto da 30 megapixel campionata a metà, la scala 1
         * mostrava **un quarto** dei pixel veri e la riga dei dettagli scriveva '100%'.
         * Non era un difetto visibile finché quei pixel non c'erano comunque; adesso i
         * tasselli li recuperano, quindi il 100% deve portare dove si vedono davvero,
         * e sarebbe stato assurdo leggere a piena risoluzione a una scala chiamata 200%.
         * ⚠️ Per un'immagine non campionata il fattore vale 1 e qui non cambia niente,
         * che è il caso di quasi tutte.
         */
        val oneToOne =
            sampleFactor * if (settings.scaleMode == ScaleMode.PHYSICAL) 1f else density.density

        val fitScale = min(viewWidth / imageWidth, viewHeight / imageHeight)
        // 'Fit' means shown whole. Growing a small picture to fill the screen is a
        // separate wish, and it is a setting rather than the default for the same
        // reason it is in the userscript: blowing up a 64px icon helps nobody.
        val restScale = if (settings.fitGrow) fitScale else min(fitScale, oneToOne)

        var scale by remember(image, settings) { mutableFloatStateOf(restScale) }
        var offset by remember(image, settings) { mutableStateOf(Offset.Zero) }

        // ⚠️ La riga torna a mostrarsi a ogni fotografia nuova, come faceva quando viveva
        // qui dentro: spegnerla è una decisione su **questa** immagine, non una
        // preferenza, che è quello che dice l'impostazione.
        // ⚠️ E l'esito dei tasselli si AZZERA con lei: la riga sopravvive al cambio di
        // fotografia (è il rimedio al lampeggio della `0.42`), quindi senza questa riga
        // porterebbe l'esito di quella di prima sopra quella di adesso, per il tempo che
        // serve a riaprire il lettore. Un dato vecchio in una diagnostica è peggio di
        // nessun dato.
        LaunchedEffect(image, settings) {
            info.visible = settings.infoVisible
            info.tiles = null
        }

        /*
         * ⚠️ La percentuale esce di qui con un flusso e non con una scrittura durante la
         * composizione: `scale` cambia a ogni fotogramma di una pinza, e scriverne il
         * derivato mentre si compone è il modo di farsi rifare la composizione da capo
         * dentro sé stessa. Così invece si aggiorna solo il testo che la mostra.
         */
        LaunchedEffect(oneToOne) {
            snapshotFlow { scale }.collect { info.percent = it / oneToOne }
        }
        var menuOpen by remember(image) { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        /**
         * Di quanto la fotografia è stata trascinata di lato, in pixel.
         *
         * ⚠️⚠️ **È QUESTO CHE RENDE FLUIDA LA SFOGLIATURA, e fino alla 0.35 non
         * esisteva**: il rilevatore sommava lo spostamento in una variabile sua e sullo
         * schermo non si muoveva niente, quindi il dito trascinava il vuoto e la
         * fotografia cambiava di scatto al distacco. Ora il numero vive qui, la figura lo
         * segue e la vicina entra dal bordo.
         * ⚠️ Si azzera col cambio di fotografia perché la chiave del `remember` è
         * `image`: quando la nuova arriva, la pagina è già al suo posto.
         */
        var travel by remember(image, settings) { mutableFloatStateOf(0f) }

        /**
         * Se una pagina sta andando a destinazione.
         *
         * ⚠️ Serve a non far cominciare una seconda strisciata mentre la prima atterra:
         * i due movimenti scriverebbero lo stesso numero, e due arrivi vorrebbero dire
         * due fotografie saltate con un gesto solo. Si legge **al momento in cui il dito
         * scende**, come le altre due condizioni della strisciata.
         */
        var settling by remember(image, settings) { mutableStateOf(false) }

        val pageGap = with(density) { PAGE_GAP.toPx() }
        val series = folder?.seriesOrNull
        /*
         * ⚠️ Nell'ordine di LETTURA, lo stesso che conta `onStep`: la 'dopo' è quella che
         * arriva strisciando verso sinistra, cioè quella che `onStep(1)` aprirà. Se le due
         * divergessero, si vedrebbe entrare una foto e comparirne un'altra.
         * ⚠️⚠️ **E DALLA 1.06 SALTANO I FILMATI COME LI SALTA IL PASSO**: prima erano un
         * `index ± 1` crudo, e con 'Sfoglia solo le immagini' acceso divergevano proprio nel
         * modo che la nota qui sopra prometteva di evitare. Il filmato scivolava dentro dal
         * bordo, il dito si alzava, e il passo saltava a quello dopo: si vedeva un filmato
         * *per un istante*, che è il difetto segnalato dall'utente. Il salto adesso è uno
         * solo, e vive su `Folder.Series`.
         * ⚠️ **Da qui viene anche la resistenza al capolinea**: con soli filmati di là,
         * `toward` risponde `null`, quindi il gesto frena come all'ultima fotografia. Prima
         * la pagina si lasciava trascinare fino in fondo per poi non andare da nessuna
         * parte.
         */
        val skipVideos = settings.imagesOnly
        val nextUri = series?.toward(1, skipVideos)
        val prevUri = series?.toward(-1, skipVideos)

        // ⚠️⚠️ LE DUE VICINE SI CHIEDONO SUBITO, e non quando il dito le tira dentro:
        // una miniatura che comincia a generarsi nell'istante del gesto arriva **dopo**
        // il gesto, e si vedrebbe entrare un rettangolo vuoto. Qui la fotografia grande è
        // già decodificata (siamo in `Ready`), quindi queste due richieste non rubano
        // niente a quello che si sta guardando, e servono due volte: alla vicina che
        // scorre e alla `PreviewThumb` che comparirà appena la pagina gira.
        LaunchedEffect(nextUri, prevUri) {
            val loader = SingletonImageLoader.get(context)
            listOfNotNull(nextUri, prevUri).forEach { loader.execute(Thumbs.request(context, it)) }
        }

        /**
         * Se una pagina si sta muovendo, per accendere le vicine solo mentre servono.
         *
         * ⚠️⚠️ **`derivedStateOf` e non `travel != 0f` letto direttamente**, ed è la
         * differenza fra un gesto fluido e uno a scatti: `travel` cambia a ogni
         * fotogramma, e leggerlo qui rifarebbe la composizione sessanta volte al secondo.
         * Così invece si ricompone **due volte per gesto**, quando il booleano si accende
         * e quando si spegne; il movimento vero passa dai blocchi `graphicsLayer`, che
         * leggono lo stato senza ricomporre.
         * ⚠️ Le chiavi sono le stesse di `travel`, e non è pignoleria: un `remember`
         * senza chiavi terrebbe il calcolo agganciato allo stato della fotografia
         * **precedente**, che resta fermo al suo ultimo valore, e le vicine non si
         * spegnerebbero più.
         */
        val dragging by remember(image, settings) { derivedStateOf { travel != 0f } }

        /**
         * La lettura a pezzi della fotografia, quando serve e si può.
         *
         * ⚠️ Nella stragrande maggioranza dei casi è **null**, ed è voluto: `open`
         * rinuncia subito se l'immagine non era campionata, cioè se è già entrata
         * intera in memoria e non c'è nessun dettaglio da recuperare. Là questa strada
         * non costa niente e il visualizzatore è quello di sempre.
         */
        /*
         * ⚠️ **Un involucro invece del lettore nudo, e non è cerimonia**: `null` deve poter
         * dire *sto ancora aprendo* e non *non si può aprire*, e un lettore nudo confonde
         * le due cose, perché l'apertura fallita restituisce anch'essa `null`. Senza la
         * distinzione la riga dei dettagli accuserebbe il formato per la frazione di
         * secondo che serve ad aprirlo.
         */
        val regions by produceState<RegionHolder?>(null, image, source) {
            val opened = RegionSource.open(context, source, image)
            value = RegionHolder(opened)
            awaitDispose { opened?.close() }
        }

        /** Il pezzo a piena risoluzione disegnato sopra la figura, quando c'è. */
        var sharp by remember(image, settings) { mutableStateOf<SharpTile?>(null) }

        /*
         * ⚠️⚠️ **SI LEGGE QUANDO IL DITO SI FERMA, e la pausa è la funzione, non un
         * ripiego**: decodificare a ogni fotogramma di una pinza vorrebbe dire leggere
         * dal file sessanta volte al secondo per pezzi che nessuno ha ancora guardato.
         * Finché ci si muove si vede il bitmap di base ingrandito, cioè esattamente
         * quello che si vedeva prima; un quinto di secondo dopo che ci si ferma arriva
         * la nitidezza.
         * ⚠️ `collectLatest` più `delay` **è** l'attesa: ogni movimento annulla il conto
         * alla rovescia precedente e insieme la decodifica che stava partendo.
         */
        LaunchedEffect(regions, viewWidth, viewHeight) {
            // Ancora in apertura: non si dice niente, perché non si sa ancora niente.
            val reader = (regions ?: return@LaunchedEffect).reader
            if (reader == null) {
                sharp = null
                // ⚠️ Il formato o l'orientamento: `RegionSource.open` rinuncia quando non
                // sa rileggere il file, e quando le misure grezze non concordano col tag
                // EXIF. Sono due cose, ma da qui si vedono uguali, e dirne una sola
                // sarebbe peggio che dirle insieme.
                info.tiles = if (image.sampled) "no tile: format" else null
                return@LaunchedEffect
            }
            snapshotFlow { scale to offset }.collectLatest { (atScale, atOffset) ->
                delay(TILE_DELAY_MS)
                when (
                    val done =
                        sharpen(reader, imageWidth, imageHeight, viewWidth, viewHeight, atScale, atOffset)
                ) {
                    is Sharpening.Done -> {
                        sharp = done.tile
                        info.tiles = "sharp"
                    }
                    is Sharpening.None -> {
                        sharp = null
                        info.tiles = done.why
                    }
                }
            }
        }

        /**
         * Porta la pagina a destinazione, o la riporta al suo posto.
         *
         * ⚠️ **Chi non si è mosso non ha niente da riportare**, e senza questa uscita una
         * trascinata verticale (che passa di qui con lo spostamento a zero) bloccherebbe
         * la strisciata per i 220ms dell'animazione, cioè proprio quando il dito sta per
         * ricominciare.
         * ⚠️ **Perché la pagina non può restare fuori schermo**: `settling` si spegne da
         * sé solo nel ritorno al posto, e negli altri casi conta su `onStep`, che ricarica
         * il visualizzatore e butta via questo stato insieme al resto. Regge perché
         * `reachable` legge la stessa serie che `onStep` conta, e quella serie sotto un
         * `ImageCanvas` vivo non cambia: ogni altra via che la riscrive passa da
         * `state = Loading`, che questa schermata non le sopravvive.
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
                // ⚠️ Il passo si chiede DOPO l'animazione, non prima: chiedendolo prima,
                // la fotografia nuova arriverebbe mentre la vecchia sta ancora scivolando
                // via, e si vedrebbero due immagini sovrapposte. Chiesto qui, il
                // visualizzatore si rifà da capo con la pagina già al centro.
                if (step == 0) settling = false else onStep(step)
            }
        }

        /**
         * Lo scivolamento in corso, e `null` quando la figura è ferma.
         *
         * ⚠️⚠️ **SI ANNULLA APPENA UN DITO TORNA GIÙ**, e senza questo l'inerzia diventerebbe
         * un difetto invece di una comodità: chi appoggia il dito su una fotografia che
         * scivola vuole fermarla, e due scrittori sullo stesso `offset` (l'animazione e il
         * gesto) darebbero uno strappo. L'annullamento sta dentro [zoomAround] e [animateTo],
         * cioè nei due soli punti da cui `offset` cambia per volontà di qualcuno.
         */
        var gliding by remember(image, settings) { mutableStateOf<Job?>(null) }

        fun stopGlide() {
            gliding?.cancel()
            gliding = null
        }

        // ⚠️ La curva e la soglia si preparano una volta: la prima dipende dalla densità, la
        // seconda è una velocità in dp al secondo che va tradotta in pixel.
        val decay = remember(density) { splineBasedDecay<Offset>(density) }
        val glideFloor = with(density) { GLIDE_FLOOR.toPx() }

        fun clampOffset(candidate: Offset, atScale: Float): Offset {
            val slackX = max(0f, (imageWidth * atScale - viewWidth) / 2f)
            val slackY = max(0f, (imageHeight * atScale - viewHeight) / 2f)
            return Offset(
                candidate.x.coerceIn(-slackX, slackX),
                candidate.y.coerceIn(-slackY, slackY)
            )
        }

        /** Rescales around a point on screen, keeping what is under that point still. */
        fun zoomAround(anchor: Offset, next: Float, pan: Offset = Offset.Zero) {
            stopGlide()
            val clamped = next.coerceIn(MIN_SCALE, settings.zoomMax)
            val fromCentre = anchor - Offset(viewWidth / 2f, viewHeight / 2f)
            val corrected = fromCentre - (fromCentre - offset) * (clamped / scale)
            scale = clamped
            offset = clampOffset(corrected + pan, clamped)
        }

        fun animateTo(target: Float) {
            stopGlide()
            scope.launch {
                val from = scale
                val animation = Animatable(from)
                animation.animateTo(target.coerceIn(MIN_SCALE, settings.zoomMax)) {
                    scale = value
                    offset = clampOffset(offset * (value / from), value)
                }
            }
        }

        /**
         * L'inerzia della panoramica: al distacco la figura continua e frena da sé.
         *
         * ⚠️⚠️ **NASCE DALLA 0.81** (richiesta dell'utente: *inerzia del trascinamento*):
         * prima la figura si fermava nell'istante in cui il dito si stacca, che su una
         * fotografia ingrandita è il gesto più frequente di tutti e l'unico che sembrava di
         * un'app di dieci anni fa.
         * ⚠️⚠️ **LA CURVA DI FRENATA È QUELLA DEL SISTEMA** (`splineBasedDecay`), non
         * un'esponenziale con un attrito scelto da me: è la stessa che Android usa per lo
         * scorrimento di ogni elenco, quindi il gesto qui dentro finisce **come finisce
         * altrove**, e non c'è nessun numero da tarare a naso.
         * ⚠️ **Si ferma appena il bordo ferma la figura**, e senza questo controllo
         * l'animazione continuerebbe a girare per il suo secondo intero mentre l'immagine è
         * già ferma contro il limite: `clampOffset` la tiene dentro, quindi il valore
         * calcolato smette di cambiare qualcosa e tanto vale smettere di calcolarlo.
         * ⚠️ **Sotto [GLIDE_FLOOR] non parte niente**: un dito che si alza da fermo lascia
         * una velocità piccola ma non nulla, e uno scivolamento di due pixel si legge come un
         * sussulto invece che come inerzia.
         */
        fun glideBy(velocity: Velocity) {
            val speed = Offset(velocity.x, velocity.y)
            if (speed.getDistance() < glideFloor) return
            stopGlide()
            gliding = scope.launch {
                // ⚠️ `AnimationState` e non `Animatable`, e la differenza è l'unica cosa che
                // permette di fermarsi al bordo: il blocco di `Animatable` ha per ricevitore
                // l'animabile stesso, quello di `AnimationState` un `AnimationScope`, che è
                // dove vive `cancelAnimation`.
                AnimationState(Offset.VectorConverter, offset, speed).animateDecay(decay) {
                    val next = clampOffset(value, scale)
                    if (next == offset) cancelAnimation() else offset = next
                }
            }
        }

        // ⚠️⚠️ **`requiredSize` E NON `size`, ED È IL DIFETTO PER CUI LE IMMAGINI GRANDI SI
        // APRIVANO PICCOLE INVECE CHE ADATTATE** (segnalato dall'utente, corretto nella 0.31).
        // `size` **negozia** col genitore: `SizeNode.measure` chiama `constrain(vincoli in
        // ingresso, misura chiesta)` quando `enforceIncoming` è vero, e per `size` è vero
        // (verificato nel bytecode di `foundation-layout` nella 0.28, quando la stessa
        // trappola aveva mangiato l'ingrandimento dell'icona). Qui il genitore è il
        // `BoxWithConstraints` della vista, quindi un'immagine **più grande della vista**
        // veniva ricondotta alla misura della vista, la bitmap ci finiva dentro adattata, e
        // poi `graphicsLayer` applicava la scala UNA SECONDA VOLTA.
        // - **Effetto**: larghezza disegnata = vista x scala, invece di bitmap x scala. Cioè
        //   l'immagine veniva piccola **esattamente del fattore di adattamento**.
        // - **Perché solo le grandi**: un'immagine più piccola della vista non viene
        //   ricondotta a niente, e infatti si è sempre vista giusta. I WebP dell'utente sono
        //   i suoi file più grossi, ed è per questo che il difetto sembrava del formato.
        // - **Misura che lo prova**: su uno screenshot di un 2736 x 4096 la figura occupava il
        //   43.6% della larghezza, cioè **esattamente la scala di riposo**, che è la firma di
        //   questo difetto: se il nodo fosse la bitmap, quella frazione sarebbe 1.
        // - ⚠️ **E spiega anche i 'passi forzati' dello zoom**: con il nodo ricondotto, ogni
        //   scala era moltiplicata per il fattore di adattamento, quindi nemmeno il 100%
        //   mostrava un pixel per pixel. Un difetto solo, due sintomi.
        // ⚠️ Il ritaglio non si perde: il `BoxWithConstraints` qui sopra ha `clipToBounds`.
        Image(
            bitmap = frame ?: image.bitmap,
            contentDescription = image.displayName,
            modifier = Modifier
                .align(Alignment.Center)
                .requiredSize(with(density) { imageWidth.toDp() }, with(density) { imageHeight.toDp() })
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    // ⚠️ Lo spostamento della sfogliatura si SOMMA a quello della
                    // panoramica invece di sostituirlo: la strisciata vive solo a riposo,
                    // dove `offset` è zero, ma se un domani i due stati si
                    // sovrapponessero, sostituirlo farebbe saltare la figura al primo
                    // pixel di trascinamento.
                    translationX = offset.x + travel
                    translationY = offset.y
                }
        )

        /*
         * ⚠️⚠️ **IL TASSELLO SI DISEGNA SOPRA LA FIGURA, NON AL SUO POSTO**, ed è quello
         * che rende innocua tutta questa strada: sotto c'è sempre la fotografia intera
         * com'è sempre stata, e questo è un rattoppo nitido sulla parte che si guarda.
         * Se un giorno finisse fuori posto si vedrebbe un rettangolo spostato, non
         * un'immagine mancante.
         * ⚠️ La posizione si ricalcola **al disegno**, leggendo scala e spostamento
         * dentro il `Canvas`: così il pezzo resta incollato alla figura mentre il dito
         * la muove, senza che nessuno ricomponga niente. È la stessa formula del
         * `graphicsLayer` qui sopra, `travel` compreso.
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

        // ⚠️ Le vicine esistono SOLO mentre si trascina: a riposo non c'è niente da
        // disegnare, e tenerle in scena costerebbe due immagini per ogni fotografia
        // guardata. ⚠️ Sono MINIATURE, le stesse della griglia e della `PreviewThumb`:
        // entrano nell'istante del gesto perché sono già in memoria, e la fotografia
        // vera arriva quando la pagina è girata.
        if (dragging) {
            Neighbour(nextUri, settings) { travel + viewWidth + pageGap }
            Neighbour(prevUri, settings) { travel - viewWidth - pageGap }
        }

        // A riposo la figura non ha gioco da trascinare, quindi una strisciata
        // orizzontale non serve a niente e può cambiare immagine. Ingrandita
        // serve a spostarsi dentro la foto, e lì il rilevatore non c'è proprio.
        val atRest = abs(scale - restScale) < 0.01f

        /**
         * Se la strisciata è ammessa, letto **al momento in cui il dito scende**.
         *
         * ⚠️⚠️ **QUESTO È IL RIMEDIO ALLA PINZA CHE SI INCOLLAVA ALL'ADATTAMENTO, ed è la
         * ragione per cui `atRest` NON è più una chiave di `pointerInput`.** Verificato sul
         * bytecode di `compose.ui`: `SuspendingPointerInputModifierNodeImpl.update$ui`
         * confronta le chiavi con `Arrays.equals` e chiama **`resetPointerInputHandler()`**
         * quando cambiano, cioè **annulla il gesto in corso**. Siccome `atRest` si rovescia
         * proprio quando la scala attraversa la banda dell'adattamento, ogni pinza moriva
         * là: il dito era ancora giù e il gesto non c'era più. L'utente lo ha misurato come
         * 'si blocca al 13%', con un pelo di asimmetria nei due versi, che è la banda di
         * 0.01 vista da sopra e da sotto.
         * ⚠️ **Regola che vale oltre questo caso**: uno stato che cambia DURANTE un gesto non
         * può essere una chiave di `pointerInput`, perché quel gesto è la prima cosa che il
         * cambiamento distrugge. Vale anche per `folder`, che cambia quando la ricerca della
         * cartella risponde e annullava le pinze fatte subito dopo l'apertura.
         * ⚠️ Letto **una volta per gesto** e non a ogni evento: così una panoramica che entra
         * nella banda dell'adattamento resta una panoramica invece di diventare a metà strada
         * una strisciata che cambia immagine.
         */
        val swipeAllowed by rememberUpdatedState(atRest && folder?.seriesOrNull != null)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(image, settings) {
                    // ⚠️⚠️ UN RILEVATORE SOLO PER LE DUE COSE, e questa è la
                    // correzione della `0.22`: panoramica, pinza e strisciata
                    // nascono tutte da un dito che si muove, quindi finché stanno su
                    // due modificatori diversi si contendono lo stesso gesto e uno
                    // dei due perde sempre. Chi decide dev'essere uno.
                    detectPanZoomOrSwipe(
                        // La strisciata vive solo A RIPOSO: ingrandita, il dito
                        // serve a spostarsi dentro la foto. E senza una serie da
                        // sfogliare non ha dove portare.
                        // ⚠️ `settling` si legge QUI dentro e non dentro `swipeAllowed`:
                        // là sarebbe un valore fissato dalla composizione, e fra il
                        // momento in cui la pagina parte e quello in cui la composizione
                        // se ne accorge ci sta un dito che scende.
                        swipeEnabled = { swipeAllowed && !settling },
                        // ⚠️ La soglia è una FRAZIONE della larghezza e non un
                        // numero di dp: su uno schermo stretto un valore fisso
                        // sarebbe mezza schermata, su un tablet un nulla.
                        // ⚠️ Presa da `size` del rilevatore e non dalla `viewWidth` della
                        // composizione: quella sarebbe un valore catturato, e dopo una
                        // rotazione il rilevatore in vita ne userebbe uno vecchio.
                        swipeThreshold = { size.width / 5f },
                        onTransform = { centroid, pan, zoom ->
                            zoomAround(centroid, scale * zoom, pan)
                        },
                        onPanEnd = { glideBy(it) },
                        // ⚠️ La resistenza al capolinea NON è decorazione: senza, alla
                        // prima o all'ultima foto il dito trascinerebbe la pagina su un
                        // vuoto che non porta da nessuna parte. Frenata a un terzo, la
                        // pagina dice 'di qua non c'è niente' e poi torna al suo posto.
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
                .pointerInput(image, settings) {
                    detectViewerGestures(
                        // ⚠️ Anche questo vibra, ed è quello che l'utente ha nominato
                        // per primo: vedi [withHaptics]. Qui non passa da
                        // `combinedClickable` ma dal rilevatore scritto in casa, quindi la
                        // vibrazione si chiama a mano.
                        onLongPress = {
                            haptics.performHapticFeedback(HOLD_BUZZ)
                            onHold()
                            menuOpen = true
                        },
                        /*
                         * ⚠️⚠️ **UN TOCCO IN MEZZO ALLO SCHERMO METTE IN PAUSA, dalla 1.18**
                         * (richiesta dell'utente, 2026-09-01: *visto che non sono previste
                         * azioni al singolo tocco istantaneo, con le immagini animate un
                         * semplice tocco in mezzo allo schermo deve essere equivalente a
                         * pausa/play*). L'icona della fila cambia da sé, perché legge lo
                         * stesso `playing`.
                         * ⚠️ **Su una fotografia ferma non fa niente**, e non è una
                         * dimenticanza: `animation` è `null` fuori dalle immagini animate.
                         */
                        onSingleTap = { onSingleTap() },
                        onDoubleTap = {
                            // Two states only, as on the desktop viewer: whole, or
                            // one pixel of the file per pixel of the screen.
                            animateTo(if (abs(scale - restScale) < 0.01f) oneToOne else restScale)
                        },
                        onZoomDrag = { anchor, dy ->
                            zoomAround(anchor, scale * exp(dy * DRAG_ZOOM_SENSITIVITY))
                        }
                    )
                }
        )

        /*
         * ⚠️⚠️ **NON C'È PIÙ NESSUN ANCORAGGIO, dalla 0.69** (richiesta dell'utente: *fai
         * apparire il menu sempre al centro dello schermo*). Prima qui stava un `Box` vuoto
         * spostato sul punto premuto, che faceva da ancora a un `DropdownMenu`; adesso il
         * menu si posiziona da sé sulla finestra, quindi non serve un'ancora e **non serve
         * sapere dove era il dito**: è la ragione per cui la posizione del tocco non si
         * conserva più.
         */
        if (menuOpen) {
            ImageMenu(
                image = image,
                source = source,
                onDismiss = { menuOpen = false },
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
 * Panoramica, pinza e strisciata orizzontale, decise dallo STESSO rilevatore.
 *
 * ⚠️⚠️ **Perché stanno insieme, che è la correzione della `0.22`.** Fino alla `0.21`
 * la strisciata viveva su un `Modifier.pointerInput` suo, prima di
 * `detectTransformGestures`, con un commento che dava l'ordine per garanzia. Non lo
 * è, e la strisciata **non ha mai funzionato**: le due cose nascono dallo stesso
 * dito che si muove, quindi si contendono il gesto, e chi supera per primo la soglia
 * di movimento consuma le variazioni e lascia l'altro senza niente.
 *
 * ⚠️⚠️ **A perdere era sempre la strisciata, e non per un pelo.** Le due soglie non
 * misurano la stessa cosa: `detectTransformGestures` guarda il **modulo** dello
 * spostamento, `detectHorizontalDragGestures` la sola **X**. Su una riga
 * perfettamente orizzontale pareggiano, e per ogni altro angolo il modulo arriva
 * prima: a 10 gradi il transform scatta a 24px dove l'orizzontale ne vuole 24,37. Un
 * pollice non disegna mai una riga perfetta, quindi la strisciata perdeva sempre.
 * Verificato sul sorgente vero di Compose e non a memoria: `detectTransformGestures`
 * consuma ogni variazione appena passata la soglia, e
 * `awaitPointerSlopOrCancellation` risponde `return null` a una variazione consumata,
 * cioè annulla la trascinata.
 *
 * ⚠️ E l'ordine dei modificatori non avrebbe salvato niente, il che è la ragione per
 * cui il rimedio non è scambiarli: quell'elica controlla la variazione **anche nel
 * passaggio Final**, che gira dopo il Main di tutti i nodi, quindi un consumo
 * altrui arriva comunque prima della fine dell'evento.
 *
 * Il corpo qui sotto è `detectTransformGestures` con un ramo in più: finché il dito
 * è uno solo e la strisciata è ammessa, lo spostamento orizzontale non muove la figura
 * **dentro** la vista ma sposta la pagina intera, e al distacco decide se girarla o
 * riportarla al suo posto.
 *
 * ⚠️⚠️ **Dalla 0.36 lo spostamento esce a OGNI evento (`onSwipeDrag`) e non solo alla
 * fine, ed è tutta lì la fluidità che l'utente ha chiesto**: prima il numero si
 * accumulava qui dentro senza che niente si muovesse, quindi il dito trascinava il vuoto
 * e la fotografia cambiava di scatto al distacco. Chi tornasse a un solo `onSwipe`
 * finale rifarebbe esattamente quel difetto.
 *
 * ⚠️ **L'esito finale esce SEMPRE quando si è trascinato**, anche quando il gesto è
 * stato annullato o non è arrivato alla soglia: senza, la pagina resterebbe ferma a metà
 * schermo, che è il peggiore dei difetti possibili qui.
 *
 * ⚠️ Un secondo dito **annulla** la strisciata in corso (`travel` torna a zero e il
 * gesto è come non fosse cominciato, velocità compresa): chi appoggia il pollice per
 * pizzicare non sta chiedendo l'immagine dopo, e senza questo una pinza cominciata
 * storta la cambierebbe.
 */
private suspend fun PointerInputScope.detectPanZoomOrSwipe(
    swipeEnabled: () -> Boolean,
    swipeThreshold: () -> Float,
    onTransform: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    /**
     * Il dito si è alzato dopo una panoramica, e con quale velocità in pixel al secondo.
     *
     * ⚠️⚠️ **NASCE DALLA 0.81** (richiesta dell'utente: *inerzia del trascinamento*): senza,
     * la figura si fermava nell'istante in cui il dito si stacca, che su una fotografia
     * ingrandita è il gesto che si fa più spesso. ⚠️ **Esce solo per il dito SOLO**: dopo una
     * pinza nessuno si aspetta che l'immagine continui, e il centroide che passa da due dita
     * a una salta di netto, cioè inventerebbe una velocità che nessuno ha impresso.
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
        // ⚠️ La velocità serve al colpo di pollice corto e rapido, che per spazio non
        // arriverebbe alla soglia: senza, quel gesto tornerebbe indietro e sembrerebbe
        // non capito. Si misura sul dito che ha cominciato e non sul centroide, che con
        // un dito solo è lo stesso punto e con due non è più una strisciata.
        val speed = VelocityTracker()
        // ⚠️ Un secondo misuratore e non lo stesso: quello sopra segue la STRISCIATA (il
        // dito che ha cominciato, in orizzontale), questo la PANORAMICA dentro la figura.
        // Sono due gesti che si escludono, ma i loro numeri no: una strisciata annullata dal
        // secondo dito azzera il primo, e la panoramica che ne segue non deve ereditarne la
        // velocità.
        val slide = VelocityTracker()
        var panned = false

        val down = awaitFirstDown(requireUnconsumed = false)
        // ⚠️ Le due condizioni si leggono QUI, a dito appena sceso, e non fuori: fuori
        // sarebbero valori catturati alla nascita del rilevatore, ed è esattamente ciò che
        // costringeva a rifarlo a ogni cambio di stato. Vedi `swipeAllowed`.
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
                    // Chi appoggia il secondo dito non sta più sfogliando: la pagina
                    // torna al suo posto e la strisciata è come non fosse cominciata.
                    // ⚠️ `dragged` torna falso apposta: se restasse vero, al distacco si
                    // leggerebbe la velocità di prima del pizzico e la pagina girerebbe.
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
                        // ⚠️ Si misura la posizione del dito **solo quando è uno**: con due
                        // il centroide è un punto che non appartiene a nessun dito, e nel
                        // momento in cui uno si alza fa un salto che il misuratore leggerebbe
                        // come una frustata. Con due dita si azzera, così una pinza non lascia
                        // dietro di sé un'inerzia.
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
            // ⚠️ Non si è strisciato, quindi se si è spostata la figura il gesto finisce
            // qui: l'inerzia parte al distacco, e un gesto annullato da un altro rilevatore
            // non ne lascia.
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
 * Long press, double tap, and the one handed zoom, in one detector.
 *
 * ⚠️⚠️ **A single tap does nothing on purpose.** It used to toggle the details, and
 * the user asked for it gone: on a viewer every tap is also a way of just touching
 * the picture, so a panel that appears and disappears under the thumb reads as a
 * glitch rather than a control. The details now come from the menu, which is asked
 * for rather than stumbled into.
 *
 * ⚠️ Written by hand because Compose has no double-tap-then-drag detector, and the
 * two things it does have cannot be combined: `detectTapGestures` reports a double
 * tap only when the second finger LIFTS, which is exactly too late to start a drag
 * from it.
 *
 * ⚠️ The bail-outs on a second finger are what keep a slow PINCH from being read as
 * a long press: the pinch belongs to the transform detector on the other modifier,
 * and without these a two-fingered zoom held for half a second would open the menu.
 *
 * ⚠️ And the long press wants a finger that STAYS PUT: a single finger that travels
 * further than the touch slop is panning the picture, not asking for the menu. See
 * the note in phase one for why that is checked here instead of being left to the
 * detector that consumes the pan.
 */
private suspend fun PointerInputScope.detectViewerGestures(
    onLongPress: (Offset) -> Unit,
    /**
     * Un tocco solo, che non è diventato né un doppio tocco né altro.
     *
     * ⚠️⚠️ **ARRIVA DOPO LA FINESTRA DEL DOPPIO TOCCO, e non si può fare altrimenti**: finché
     * quella finestra è aperta, un tocco solo e il primo di due sono indistinguibili. Il
     * ritardo è quello di sistema (`doubleTapTimeoutMillis`), qualche decimo di secondo, e su
     * un comando che mette in pausa si sopporta. Chiamarlo subito vorrebbe dire mettere in
     * pausa a ogni doppio tocco, cioè rompere l'ingrandimento per aggiungere una scorciatoia.
     * ⚠️ **Sulle immagini ferme non fa niente**, ed è la ragione per cui esiste: là un tocco
     * solo non era previsto (l'utente, 2026-09-01), quindi il posto era libero.
     */
    onSingleTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit,
    onZoomDrag: (anchor: Offset, dy: Float) -> Unit
) {
    awaitEachGesture {
        val first = awaitFirstDown(requireUnconsumed = false)

        // Phase one: does this first finger lift before the long press timeout?
        //
        // ⚠️⚠️ A FINGER THAT MOVES IS NOT A LONG PRESS, and the distance is checked
        // HERE rather than left to whoever consumes the drag. Leaving it to the
        // consumer is what the `isConsumed` line below already tries, and on a real
        // phone it was not enough: the menu opened in the middle of a pan (user,
        // 2026-08-26). The pan lives on another modifier, so whether its changes
        // are consumed before this detector sees them is a matter of ordering and
        // timing, which is exactly the kind of thing that holds in one build and
        // stops holding in the next. The travel test does not depend on any of it.
        var abandoned = false
        val lifted = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var up = false
            while (!up && !abandoned) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == first.id }
                when {
                    event.changes.count { it.pressed } > 1 -> abandoned = true
                    change == null || change.isConsumed -> abandoned = true
                    // The distance is measured from where the finger LANDED, not
                    // frame by frame: a slow drift adds up to a drag just as much
                    // as a quick flick does, and summing steps would let a wander
                    // that comes back cancel itself out.
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

        // Phase two: a second finger within the double tap window, or nothing.
        val second = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
            awaitFirstDown(requireUnconsumed = false)
        } ?: run {
            onSingleTap(first.position)
            return@awaitEachGesture
        }

        // Phase three: it is a double tap until it moves, and a zoom once it does.
        // The anchor stays where the second tap landed for the whole drag, so the
        // picture grows around the point that was chosen and not around wherever
        // the thumb has wandered to.
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
 * The menu a long press opens.
 *
 * ⚠️ The order is the user's, given after using the first version, and it is not
 * the userscript's: what you do WITH the picture comes first, what you do TO the
 * view second, and the two that lead somewhere else last. 'Copy address' and the
 * 200/400% steps fell out of that list rather than being dropped for a reason of
 * mine.
 *
 * ⚠️⚠️ **IN FONDO CI SONO LE SEI OPERAZIONI SUI FILE, dalla `0.62`** (richiesta
 * dell'utente: *in fondo, come icone, due righe di tre*). Sono lo stesso [ActionPad] della
 * selezione nella griglia, quindi le icone e il loro ordine si decidono in un posto solo:
 * chi impara dove sta 'sposta' lo impara una volta.
 * ⚠️⚠️ **E 'Condividi' è uscita dalle voci di testo perché il riquadro la porta**, con la
 * stessa chiamata di prima: due voci identiche nello stesso menu sono un ingombro, e questo
 * menu è tenuto corto apposta.
 * ⚠️ **'Copia immagine' invece resta, e non è un doppione di 'copia'**: quella mette la
 * fotografia negli **appunti**, questa copia il **file** in un'altra cartella. Sono due
 * cose diverse, ed è la ragione per cui l'etichetta della prima adesso dice 'negli
 * appunti': accanto al riquadro, 'Copia immagine' e 'Copia' si sarebbero lette come la
 * stessa voce scritta due volte.
 */
@Composable
private fun ImageMenu(
    image: LoadedImage,
    source: Uri?,
    onDismiss: () -> Unit,
    onZoom: (Float) -> Unit,
    oneToOne: Float,
    restScale: Float,
    ops: MenuOps,
    inBin: Boolean,
    /** Se il menu porta anche 'Adatta alla vista' e '100%'. Vedi `Settings.zoomInMenu`. */
    zoomRows: Boolean,
    /**
     * Se il cestino è acceso: decide se 'elimina' sposta o cancella, e con lui se ci sia una
     * conferma.
     *
     * ⚠️ **Arriva come booleano e non come impostazioni intere**: questo menu non usa
     * nient'altro, e passarle tutte vorrebbe dire ricomporlo a ogni ritocco di una voce che
     * qui non c'entra niente. È la stessa scelta di `factFields` nella griglia.
     */
    binOn: Boolean
) {
    val context = LocalContext.current

    /*
     * ⚠️⚠️ **LA SUPERFICIE È CONDIVISA COL MENU DELLA SELEZIONE dalla 0.75**: il
     * riquadro, l'ombra e l'animazione che lo fa crescere vivono in [MenuShell], e qui
     * restano le sole cose di questo menu, cioè dove sta, quanto è stondato e le sue voci.
     * ⚠️ **Si chiude anche toccando fuori**, e a differenza di quello della selezione può:
     * qui nessun tastino lo alterna, quindi non c'è nessun tocco da contendersi.
     */
    MenuShell(
        position = MenuCenter,
        dismissOnOutside = true,
        onDismiss = onDismiss
    ) {
        Column(modifier = Modifier.width(MENU_WIDTH).padding(vertical = MENU_EDGE)) {
            /*
             * ⚠️⚠️ **OGNI VOCE HA LA SUA ICONA, dalla 0.69** (scelta dell'utente sul
             * mockup), e la conseguenza che vale più dell'aspetto: **l'allineamento dei
             * testi si risolve da sé**. Con icone su alcune voci e non su altre, i testi
             * cominciavano in due posti diversi e allinearli voleva dire un rientro
             * scritto a mano, cioè un numero da tenere d'accordo con Material.
             * ⚠️⚠️ **Due collisioni sono state sciolte, e la prima è tornata a farsi
             * viva nella 0.72**: 'Copia' negli appunti e 'Copia' in una cartella
             * avrebbero avuto la stessa icona, e così 'Barra dei dettagli' e 'Info'. La
             * barra prende `Subtitles`, che è una striscia di testo su un fotogramma,
             * cioè quello che la barra è.
             * ⚠️⚠️ **La prima l'avevo sciolta con le PAROLE e non bastava** (riscontro
             * dell'utente, 2026-08-31: *non si capisce che il 'Copia negli appunti' è
             * diverso dal 'Copia' sotto*). Adesso la scioglie il **disegno**: questa
             * voce ha due fogli con montagne e sole ([Glyphs.PhotoPair]), l'altra due
             * cartelle, e l'etichetta torna a dire la cosa ('Copia immagine') invece
             * della destinazione. Una distinzione che un'etichetta non riesce a fare non
             * la si fa allungando l'etichetta.
             * ⚠️ Gli appunti sono l'unica cosa che il menu fa da sé, e può: è una
             * chiamata che non sospende e finisce prima che il menu si chiuda. Tutto il
             * resto passa da [MenuOps], e là sta scritto perché.
             */
            MenuRow(
                text = stringResource(R.string.menu_copy_image),
                icon = Glyphs.PhotoPair,
                onTap = {
                    onDismiss()
                    Toast.makeText(
                        context,
                        if (ImageActions.copyImage(context, image)) R.string.toast_image_copied
                        else R.string.toast_copy_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
            /*
             * ⚠️ **Lo schema dell'indirizzo decide DUE voci**, e per questo si legge una
             * volta sola qui invece che dentro ognuna: dice se quello che si sta guardando
             * è un file di questo telefono (`content://` o `file://`) o roba del web.
             */
            val scheme = source?.scheme?.lowercase()
            /*
             * ⚠️⚠️ **'Modifica' COMPARE SOLO SU UN FILE DEL TELEFONO, ed è l'esatto
             * rovescio della condizione di 'Scarica' qui sotto**: modificare vuol dire
             * riscrivere un file, e di una fotografia arrivata dal web non c'è nessun file
             * da riscrivere. La voce ci sarebbe, si toccherebbe, e l'unica risposta
             * possibile sarebbe un errore.
             * ⚠️⚠️ **L'ICONA È QUELLA CHE L'UTENTE HA DISEGNATO, dalla 1.29**, e la
             * richiesta originale era proprio quella: *matita su immagine*. Fino alla `1.28`
             * era la matita nuda di Material, perché nel suo repertorio una matita posata su
             * un'immagine non c'è e comporla a mano voleva dire un disegno nuovo. Quel
             * giorno è arrivato, il disegno è suo, e qui è cambiata una riga: è esattamente
             * quello che la nota vecchia prevedeva.
             */
            /*
             * ⚠️⚠️ **IL TOCCO LUNGO SU 'Modifica' FA SCEGLIERE L'APP, dalla 1.12**
             * (richiesta dell'utente): il tocco breve apre quella scelta, il tocco lungo
             * apre il selettore e la nuova scelta **resta memorizzata**. Prima, per
             * cambiare editor, bisognava andare nelle impostazioni.
             * ⚠️ **È l'idioma già in casa e non un gesto nuovo da imparare**: 'Copia' nel
             * riquadro della selezione duplica col tocco lungo esattamente così, dalla
             * `0.79`. Un secondo modo di fare la stessa cosa sarebbe stato il costo vero.
             * ⚠️⚠️ **DALLA 1.28 TUTTE LE VOCI DI QUESTO MENU SONO [MenuRow], e prima no**:
             * questa era scritta a mano (un `DropdownMenuItem` non può portare due gesti) e
             * le altre erano di Material, cioè due disposizioni diverse della stessa fila.
             * L'utente ha visto il disallineamento e lo ha disegnato. Adesso si allineano
             * per costruzione, e il perché sta in [MenuRow].
             */
            if (scheme == "content" || scheme == "file") {
                MenuRow(
                    text = stringResource(R.string.menu_edit),
                    icon = Glyphs.ImageEdit,
                    onTap = { onDismiss(); ops.edit() },
                    holdLabel = stringResource(R.string.menu_edit_hold),
                    onHold = { onDismiss(); ops.editWith() }
                )
            }
            /*
             * ⚠️ **Sta accanto a 'Modifica' perché è la stessa famiglia**: le due sole voci
             * che producono un file **nuovo** partendo da quello che si sta guardando. La
             * differenza è che una lo riscrive e l'altra lo affianca.
             */
            MenuRow(
                text = stringResource(R.string.menu_convert),
                icon = Glyphs.ImageConvert,
                onTap = { onDismiss(); ops.convert() }
            )

            /*
             * ⚠️⚠️ **'Scarica' NON COMPARE SU UN FILE LOCALE** (richiesta dell'utente):
             * là scaricherebbe nella galleria una fotografia che nella galleria c'è già,
             * cioè farebbe un doppione senza dirlo.
             * ⚠️ **Il test è lo SCHEMA dell'indirizzo** e non l'assenza di indirizzo: un
             * `content://` o un `file://` vengono dal telefono, tutto il resto dal web.
             * Senza indirizzo (`null`) la voce resta, perché quello che si sta guardando
             * non è un file di questo telefono e salvarlo ha senso.
             */
            if (scheme != "content" && scheme != "file") {
                MenuRow(
                    text = stringResource(R.string.menu_save),
                    icon = Icons.Outlined.Download,
                    onTap = { onDismiss(); ops.save(image) }
                )
            }

            /*
             * ⚠️⚠️ **LE DUE VOCI DELLO ZOOM SONO SPENTE DI FABBRICA, dalla 1.25** (richiesta
             * dell'utente, 2026-09-02: *togli '100%' e 'Adatta alla vista' dal menu a
             * pressione lunga; ricompaiono se si accende quell'opzione*). Fanno quello che il
             * **doppio tocco** fa già, e stavano in mezzo a comandi che agiscono sul file: chi
             * apre questo menu cerca quasi sempre un'altra cosa.
             * ⚠️ **Sparisce anche il filetto sopra di loro**, e non è un dettaglio: un divisore
             * che separa il nulla dal nulla lascia in scena il posto vuoto delle voci tolte.
             * ⚠️ **Non sono state cancellate ma messe dietro un interruttore**, perché il gesto
             * che le sostituisce è nascosto: chi non lo conosce resterebbe senza. Il velo del
             * doppio tocco (`Hint.ZOOM_TAP`) è la vera contropartita.
             */
            if (zoomRows) {
                HorizontalDivider()

                MenuRow(
                    text = stringResource(R.string.fit_label),
                    icon = Icons.Outlined.FitScreen,
                    onTap = { onDismiss(); onZoom(restScale) }
                )
                MenuRow(
                    text = "100%",
                    icon = Icons.Outlined.PhotoSizeSelectActual,
                    onTap = { onDismiss(); onZoom(oneToOne) }
                )
            }

            /*
             * ⚠️⚠️ **LA BARRA DELLE INFO NON STA PIÙ IN QUESTO MENU, dalla 1.20**
             * (istruzione dell'utente, 2026-09-01: *togli del tutto i riferimenti alla
             * barra delle info dal menu a pressione lunga; si decide tutto dalle
             * impostazioni*). Qui era arrivata come interruttore per non far scommettere
             * sul suo stato, ma restava la sola voce del menu che non fa niente
             * all'immagine: cambia una preferenza, e le preferenze hanno una schermata.
             * ⚠️ **E là adesso ce ne sono DUE, l'acceso e la posizione**, che in un menu
             * sarebbero state due voci per una cosa sola.
             */

            HorizontalDivider()

            /*
             * ⚠️⚠️ **SENZA INDIRIZZO NON C'È NIENTE DA FARE, e il riquadro non compare**:
             * le sei operazioni agiscono su un **file**, e una fotografia arrivata da una
             * chat o dal web non ne ha uno che questa app possa spostare. Mostrare sei
             * tasti che risponderebbero 'non riuscito' sarebbe peggio di non mostrarli.
             * ⚠️ **La condivisione sta nel riquadro ma NON passa dai file**: chiama la
             * stessa `ImageActions.share` della voce di testo che ha sostituito, cioè
             * condivide l'immagine caricata. È l'unica delle sei che funziona anche
             * quando il file non si può toccare, e il baratto è dichiarato: la scelta è
             * tenerlo semplice.
             */
            source?.let { uri ->
                val one = listOf(uri)
                ActionPad(
                    actions = listOf(
                        // ⚠️ Il tocco lungo duplica dove sei, come nella griglia: il
                        // riquadro è condiviso, e una scorciatoia che c'è in un posto e non
                        // nell'altro si impara e poi non funziona.
                        PadAction(
                            icon = Glyphs.FolderPair,
                            label = R.string.menu_copy_here,
                            onHold = if (inBin) null else {
                                {
                                    onDismiss()
                                    ops.job(FileJob.Duplicate(one))
                                }
                            },
                            holdLabel = if (inBin) null else R.string.pick_duplicate
                        ) {
                            onDismiss()
                            ops.job(FileJob.Transfer(one, move = false))
                        },
                        PadAction(Glyphs.FolderPairDashed, R.string.pick_move) {
                            onDismiss()
                            ops.job(FileJob.Transfer(one, move = true))
                        },
                        PadAction(Icons.Default.Delete, R.string.pick_delete, danger = true) {
                            onDismiss()
                            // ⚠️ Definitiva nel cestino **o** col cestino spento, ed è la
                            // stessa condizione della griglia: con lei viaggia la conferma.
                            ops.job(FileJob.Delete(one, forGood = inBin || !binOn))
                        },
                        // ⚠️ Stesso posto nel riquadro per due azioni che si escludono:
                        // nel cestino si ripristina, fuori si rinomina. Le sei icone non
                        // ballano.
                        if (inBin) {
                            PadAction(Icons.Default.SettingsBackupRestore, R.string.bin_restore) {
                                onDismiss()
                                ops.job(FileJob.Restore(one))
                            }
                        } else {
                            PadAction(Glyphs.TextCursor, R.string.pick_rename) {
                                onDismiss()
                                ops.job(FileJob.Rename(one))
                            }
                        },
                        PadAction(Icons.Default.Share, R.string.menu_share) {
                            onDismiss()
                            ops.share(image)
                        },
                        /*
                         * ⚠️⚠️ **IL TOCCO LUNGO SU 'Info' GOVERNA LA BARRA DELLE INFO, dalla
                         * 1.21** (richiesta dell'utente, 2026-09-01, che la chiama *un trick
                         * / easter egg*): il tocco breve apre le informazioni sul file, il
                         * tocco lungo apre un riquadrino con acceso/spento e sopra/sotto,
                         * che si applicano subito e **restano memorizzati**.
                         * ⚠️ **Non è la voce che la 1.20 aveva tolto da questo menu**, ed è
                         * la differenza che rende le due cose compatibili: quella era una
                         * riga sempre in scena, che si leggeva come un comando sull'immagine
                         * e non lo era. Questa è una scorciatoia nascosta dietro un gesto, e
                         * non occupa nessuno spazio: chi non la conosce vede il menu di
                         * prima.
                         * ⚠️ **L'idioma è quello di casa**: 'Modifica' qui sopra e 'Copia'
                         * nel riquadro della selezione fanno esattamente così dal 2026-08-31.
                         */
                        PadAction(
                            icon = Icons.Outlined.Info,
                            label = R.string.pick_info,
                            onHold = { onDismiss(); ops.bar() },
                            holdLabel = R.string.settings_info_visible
                        ) {
                            onDismiss()
                            ops.job(FileJob.Facts(one))
                        }
                    )
                )
            }
            // ⚠️⚠️ **QUI SOTTO C'ERANO LE IMPOSTAZIONI, e sono uscite nella 0.44**
            // (istruzione dell'utente): erano arrivate nella 0.30 perché la loro rotella
            // occupava un posto che serviva al contatore della cartella, e restavano
            // l'unica via per raggiungerle da dentro una fotografia. Dalla 0.41 le porta
            // il tastino della schermata delle cartelle, quindi questa voce era diventata
            // la seconda porta di una stanza sola.
            // ⚠️ LA RICERCA IMMAGINE NON C'È PIÙ, dalla 0.18, e non è una dimenticanza:
            // l'utente l'ha spenta dopo averla provata sul telefono, perché non
            // funzionava e faceva solo rumore in un menu tenuto corto apposta. Il codice
            // sta nella storia git, al tag `v0.17`.
        }
    }
}

/** Quanto è larga la tendina del tocco lungo: la misura del riquadro delle sei icone. */
private val MENU_WIDTH = 252.dp

/**
 * L'aria sopra e sotto le voci della tendina.
 *
 * ⚠️ **Il raggio non sta più qui, dalla 1.28**: era 8 mentre il menu della selezione era 16 e
 * quello del navigatore 8, cioè tre numeri per la stessa cosa, e l'utente li ha visti diversi
 * prima di noi. Adesso è uno solo, in `Menus.kt`, e non passa più come parametro.
 */
private val MENU_EDGE = 8.dp

/**
 * Il segno dell'immagine ridotta, al posto della parola.
 *
 * ⚠️⚠️ **DALLA `1.34` È UN SIMBOLO E NON UNA PAROLA, e la ragione è lo SPAZIO** (riscontro
 * dell'utente sulla `1.31`, voce `barra-ridotta`: *riservare una riga esclusivamente a quella
 * dicitura mi pare troppo; aggiungerei un simbolo tipo questo, che non ha nemmeno bisogno di
 * traduzione, sulla stessa riga di testo che contiene le info base*). Il difetto vero era che
 * `(ridotta)` allungava la riga dei dati abbastanza da mandarla **a capo** su un telefono, e
 * da fuori quel secondo rigo si legge come una riga dedicata a lei.
 * ⚠️ **La parola tradotta NON è stata cancellata**: `bar_reduced` resta in 28 lingue ed è
 * quello che legge un lettore di schermo, perché su questo carattere non direbbe niente di
 * utile. Vedi il modificatore in [DetailsPanel].
 * ⚠️ **Scritto per codepoint**: `U+25F1`, WHITE SQUARE WITH LOWER LEFT QUADRANT, cioè un
 * quadrato pieno per un quarto. È il carattere che l'utente ha indicato.
 */
private const val REDUCED_MARK = "\u25f1"

/**
 * La riga dei dettagli, col contatore della cartella fisso al suo estremo destro.
 *
 * ⚠️⚠️ **LÀ C'ERA LA ROTELLA DELLE IMPOSTAZIONI, ed è uscita nella 0.30** (istruzione
 * dell'utente): quel posto vale più al contatore, che è un dato che si guarda mentre
 * si sfoglia, mentre le impostazioni si aprono una volta ogni tanto e adesso stanno in
 * fondo al menu del tocco lungo. ⚠️ Chi la rimettesse toglierebbe di nuovo il posto al
 * contatore: sono due cose che si contendono lo stesso angolo.
 *
 * ⚠️ **Il contatore è FISSO a destra e non in coda al testo**, e la differenza si vede
 * sfogliando: in coda si sposta a ogni immagine, perché la riga davanti cambia
 * lunghezza col nome, col peso e con la percentuale. Un numero che si guarda spesso
 * deve stare sempre nello stesso punto.
 */
@Composable
private fun DetailsPanel(
    image: LoadedImage,
    percent: Float,
    folder: Folder.Lookup?
) {
    /*
     * ⚠️ Letta fuori dal `buildString`, che non è un contesto composable: è la stessa ragione
     * per cui `folderNote` sta qui sotto.
     */
    val reduced = stringResource(R.string.bar_reduced)
    // Letta fuori dal `buildString`, che non è un contesto composable. Null mentre
    // la ricerca è in corso e sulle immagini che una cartella non ce l'hanno.
    val folderNote = when (folder) {
        null, is Folder.Lookup.Found -> null
        Folder.Lookup.NoPermission -> stringResource(R.string.folder_no_access)
        Folder.Lookup.Unreadable -> stringResource(R.string.folder_unreadable)
        is Folder.Lookup.NotInGallery ->
            stringResource(R.string.folder_not_in_gallery, folder.detail)
        Folder.Lookup.Alone -> stringResource(R.string.folder_alone)
        Folder.Lookup.Lost -> stringResource(R.string.folder_lost)
    }
    // ⚠️⚠️ **IL VELO DI FONDO NON STA PIÙ QUI, ed è la correzione della `0.57`**: dipinto
    // dentro il contenuto della dissolvenza veniva disegnato **due volte** per tutta la
    // transizione, e due veli sovrapposti non fanno il velo di prima. Adesso lo dipinge
    // `ViewerScreen`, una volta sola, fuori dalla `AnimatedContent`: il perché per esteso
    // sta là. ⚠️ Il colore del testo arriva da quella `Surface` tramite `LocalContentColor`,
    // quindi qui non si dichiara: dichiararlo di nuovo vorrebbe dire due fonti per la
    // stessa scelta.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
    ) {
        /*
         * ⚠️⚠️ **IL NOME STA SOPRA I DATI E NON IN MEZZO A LORO** (richiesta dell'utente,
         * 2026-09-02: *sopra i dati già esistenti, in piccolo, in una sola riga con ellissi
         * centrale*). Infilato nella riga di sotto avrebbe rubato lo spazio al formato, alla
         * misura e alla percentuale, che sono corti e stanno tutti insieme proprio perché si
         * leggono in un colpo d'occhio; e allungandosi col nome avrebbe rimesso in movimento il
         * contatore, che tre note qui intorno difendono dall'essere spostato.
         * ⚠️ **Più piccolo dei dati e non più grande**: è il titolo di quello che si guarda, ma
         * la riga esiste per i dati, e un nome in grande sopra una riga di numeri li farebbe
         * sembrare una didascalia. `labelMedium` contro il `labelLarge` di sotto.
         */
        image.displayName?.takeIf { it.isNotBlank() }?.let { NameLine(it) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dati = buildString {
                    append(image.mimeType?.substringAfter('/')?.uppercase() ?: "?")
                    append("  ")
                    append(image.pixelWidth).append(" x ").append(image.pixelHeight)
                    image.byteSize?.let { append("  ").append(formatBytes(it)) }
                    append("  ").append((percent * 100).roundToInt()).append('%')
                    /*
                     * ⚠️⚠️ **DALLA 1.31 DICE UNA PAROLA TRADOTTA E NON UNA DIAGNOSTICA**
                     * (domanda dell'utente, 2026-09-02, che sul suo AVIF leggeva
                     * `(sampled, no tile: format)`: *leggere quella cosa mi serve davvero, a
                     * parte motivi di debug? Pensavo di sbarazzarmene o di riscriverla in
                     * modo più amichevole*). La risposta è che **metà serviva e metà no**:
                     * 'ridotta' è un fatto sull'immagine che si sta guardando, cioè che non
                     * è a piena risoluzione; il motivo per cui i tasselli non si applicano
                     * era per me, ed era in inglese in mezzo a un'interfaccia tradotta.
                     * ⚠️⚠️ **E SU UN AVIF QUELLA CODA C'È SEMPRE**, che è la ragione per cui
                     * il rumore si notava adesso: `RegionSource` non sa rileggere un AVIF
                     * (nessun `BitmapRegionDecoder` per quel formato), quindi ogni AVIF
                     * grande portava `no tile: format` per sempre.
                     * ⚠️ **Il motivo dei tasselli resta nel codice** (`info.tiles`, che
                     * `sharpen` continua a riempire): il giorno che serve di nuovo si
                     * rimette in scena una riga, e il difetto della `0.49` si è potuto
                     * nominare grazie a lui.
                     * ⚠️ Solo per le immagini **ridotte**: su tutte le altre la parola non
                     * dice niente, e sarebbe rumore su ogni foto.
                     */
                    if (image.sampled) append("  ").append(REDUCED_MARK)
                    // ⚠️ Il perché di una cartella che non c'è resta QUI, col resto del testo,
                    // e non va nell'angolo del contatore: è una frase, non un numero, e in
                    // quello spazio starebbe stretta o lo farebbe crescere rimettendo in
                    // movimento il contatore che si è appena fissato.
                    folderNote?.let { append("  ").append(it) }
            }
            Text(
                text = dati,
                style = MaterialTheme.typography.labelLarge,
                /*
                 * ⚠️⚠️ **IL SIMBOLO SI VEDE E LA PAROLA SI SENTE, dalla `1.34`.** Il segno
                 * non ha bisogno di traduzione (era la ragione dell'utente), ma un lettore
                 * di schermo su `◱` non dice niente di utile: quindi la stringa tradotta
                 * `bar_reduced` **resta** e diventa quello che TalkBack legge. Se un domani
                 * il simbolo cambia, la parola non si tocca.
                 * ⚠️ **Si dichiara solo quando serve**: su un'immagine intera la
                 * descrizione sarebbe una copia del testo, cioè lavoro per niente e un
                 * posto in più dove le due versioni possono divergere.
                 */
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (!image.sampled) Modifier
                        else Modifier.semantics {
                            contentDescription = dati.replace(REDUCED_MARK, reduced)
                        }
                    )
            )
            folder?.seriesOrNull?.let {
                Text(
                    text = "${it.index + 1}/${it.size}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}

/**
 * Il nome del file sopra la riga dei dati: una riga sola, con l'ellissi in mezzo.
 *
 * ⚠️⚠️ **È [fitName], lo stesso di GRIGLIA E PASTIGLIA, e non un accorciamento nuovo**: qui
 * bastava un `TextOverflow.Ellipsis`, ma quello mette i tre punti alla **fine**, cioè mangia
 * l'estensione, che è la parte che dice che cosa si sta guardando ed è la sola che l'utente ha
 * chiesto di non perdere mai. Un'ellissi in mezzo Compose non ce l'ha: è misurata là dentro, e
 * questo posto è il terzo che la usa.
 * ⚠️ **Una riga sola** (`lines = 1`), che è la differenza con gli altri due: la pastiglia del
 * dialogo ne concede tre e la griglia due, perché là il nome è il contenuto; qui è la testatina
 * di una riga di dati, e una seconda riga la farebbe crescere sopra la fotografia.
 * ⚠️ **L'estensione in grassetto anche qui**, e non per decorazione: è l'accorgimento che
 * distingue i tre punti dell'ellissi dal punto dell'estensione, e cambiarlo in un posto solo
 * vorrebbe dire che lo stesso nome si legge in due modi in due schermate.
 * ⚠️ **`BoxWithConstraints` e non una misura in dp**: la larghezza utile è quella dello schermo
 * meno i margini, e scritta a mano sarebbe giusta su un telefono solo.
 *
 * ⚠️⚠️ **ALLINEATO A SINISTRA, e la 1.24 lo aveva messo a DESTRA per un mio errore di
 * lettura.** La richiesta del 2026-09-02 diceva *fa' in modo che a destra finisca esattamente
 * dove finisce l'indice numerico dell'immagine visualizzata*, e l'avevo letta come 'allinea a
 * destra'. La correzione dell'utente dice che cosa intendeva: *il nome del file dev'essere
 * sempre allineato a sinistra; volevo dire che un nome lungo con l'ellissi in mezzo deve
 * finire allineato a destra con l'indice, ma forse era già così*. Era già così: la scatola del
 * nome è larga quanto il contenuto, quindi un nome accorciato arriva al margine destro, che è
 * dove finisce il contatore. ⚠️ **Non c'era niente da aggiustare, e allineare a destra ha
 * spostato anche i nomi CORTI**, che è il difetto che la 1.25 toglie.
 *
 * ⚠️ **Meno acceso dei dati, e la 1.25 lo abbassa ancora** (*abbassa ancora leggermente
 * l'opacità*). Il colore di partenza è quello della `Surface` che sta dietro, preso da
 * `LocalContentColor`: scriverne uno nuovo qui vorrebbe dire un secondo colore da tenere
 * d'accordo col tema.
 * ⚠️ **E si stacca dai dati di [NAME_GAP]** (*distanzia il nome di altri 2/3 pixel apparenti
 * dalle altre info*): attaccato com'era, il nome si leggeva come la prima parola della riga di
 * sotto invece che come la sua testatina.
 */
@Composable
private fun NameLine(name: String) {
    val style = MaterialTheme.typography.labelMedium
    val grassetto = SpanStyle(fontWeight = FontWeight.Bold)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(bottom = NAME_GAP)) {
        val measurer = rememberTextMeasurer()
        val room = with(LocalDensity.current) { maxWidth.roundToPx() }
        val shown = remember(name, room, style, grassetto, measurer) {
            fitName(name, room, 1, style, grassetto, measurer)
        }
        Text(
            text = shown,
            style = style,
            maxLines = 1,
            color = LocalContentColor.current.copy(alpha = NAME_FADE)
        )
    }
}

/**
 * Quanto il nome del file è meno acceso dei dati sotto di lui, e quanto se ne stacca.
 *
 * ⚠️ **0.65 dalla 1.25, e prima era 0.74**, cioè il gradino che Material chiama 'emphasis
 * media'. La richiesta era di abbassare *ancora leggermente*, e questo è un passo solo: sotto
 * si va verso il valore che Material riserva ai comandi **disattivati** (0.38), e un nome che
 * sembra spento sopra una fotografia chiara non si legge più.
 * ⚠️ **Lo stacco è di tre punti e non di due**: la richiesta diceva 'altri 2/3 pixel
 * apparenti', e fra i due estremi si prende quello che si vede, perché due punti su uno
 * schermo a tre volte sono sei pixel fisici, cioè al limite del percepibile.
 */
/**
 * Quanto è spento il nome del file in testa alla barra.
 *
 * ⚠️ **Sceso tre volte in tre versioni**, e la scala è quella: 0.74 nella `1.23`, **0.65**
 * nella `1.25` e **0.52** nella `1.26` (riscontro dell'utente, 2026-09-02: *forse il nome mi
 * piacerà di più con ancora meno opacità, diminuisci di un altro 20%*). Il numero è quello
 * di prima meno un quinto, cioè la richiesta presa alla lettera e non a occhio.
 * ⚠️ **Sotto di qui c'è un limite vero**: 0.38 è il velo che Material mette sui comandi
 * spenti, e un testo che lo raggiunge smette di dire 'sono secondario' e dice 'sono
 * disattivato'. Da qui a là restano due gradini come questo.
 */
private const val NAME_FADE = 0.52f
private val NAME_GAP = 3.dp

/**
 * Quanto copre il velo dietro la riga dei dettagli.
 *
 * ⚠️ Sta in un posto solo perché a dipingerlo è `ViewerScreen` e a spiegarlo è
 * `DetailsPanel`: due numeri uguali in due file sono un numero che prima o poi diverge.
 */
private const val PANEL_VEIL = 0.86f

/**
 * The checkerboard is what makes transparency visible, and on a viewer that is
 * information rather than decoration. The flat colour is there for looking at a
 * photograph without a pattern under it, where the checkerboard says nothing
 * because there is nothing to see through.
 *
 * ⚠️ The four greys are the userscript's, not new ones: #DDD/#EEE light and
 * #333/#222 dark, and the flat colour takes the light of one pair and the dark of
 * the other. AIV had drifted to #2A2A2A for the dark pair, which is the kind of
 * difference nobody notices and nobody can justify later.
 */
private fun DrawScope.drawBackground(size: Size, square: Float, light: Boolean, type: BgType) {
    val a = if (light) Color(0xFFDDDDDD) else Color(0xFF333333)
    val b = if (light) Color(0xFFEEEEEE) else Color(0xFF222222)
    if (type == BgType.SOLID) {
        // `b` in both themes, and that is the point: it is #EEE in the light pair
        // and #222 in the dark one, which is exactly 'the light of one and the dark
        // of the other'.
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

private fun Color.luminanceIsLight(): Boolean = (0.2126f * red + 0.7152f * green + 0.0722f * blue) > 0.5f

/**
 * ⚠️ The locale is named, and it is `US` on purpose rather than the phone's: the
 * separator stays a point in both languages, which is the convention the
 * userscript settled on after trying the other way. Leaving it implicit is also
 * what lint flags here, and 'implicit' would have meant a comma on an Italian
 * phone and a point on an English one, for the same file.
 * ⚠️ `internal` e non `private` perché la usano DUE schermate: la riga dei dettagli e il
 * riquadro che dice quanto pesa una selezione. Due copie della stessa formattazione
 * divergerebbero al primo ritocco, e il ritocco è già arrivato una volta (i GB).
 */
internal fun formatBytes(value: Long): String = when {
    value < 1024 -> "$value B"
    value < 1024 * 1024 -> String.format(Locale.US, "%.1f kB", value / 1024f)
    value < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.2f MB", value / (1024f * 1024f))
    // ⚠️ Il gradino dei GB è arrivato con la SELEZIONE MULTIPLA: una fotografia sola non
    // ci arriva mai, ma trecento insieme sì, e senza questo ramo si leggerebbe
    // '4231.77 MB', che è un numero da contare con le dita.
    else -> String.format(Locale.US, "%.2f GB", value / (1024f * 1024f * 1024f))
}

/** Quanto la fila dei comandi si stacca dal fondo, e quanto in più se sotto c'è la riga dei
 * dettagli. Vedi la nota alla chiamata: sono tarature, non misure. */
private val ANIM_LIP = 24.dp
private val ANIM_OVER_INFO = 96.dp

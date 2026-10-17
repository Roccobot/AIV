package io.github.roccobot.aiv

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.Shader.TileMode
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.nativePaint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * L'editor **completo**: quello che cambia i pixel.
 *
 * ⚠️⚠️ **Non sostituisce l'editor semplice** (scelta dell'utente: due editor separati): uno
 * mette un'immagine in posa e la ritaglia senza toccare un pixel, l'altro la sviluppa, e un editor
 * solo dovrebbe ricomprimere anche per girare un'immagine. Chi apre 'Modifica' sceglie fra i due
 * la prima volta, e la scelta si ricorda.
 *
 * ⚠️⚠️ **La storia è di valori e non di gesti, e un passo nasce quando il dito lascia il
 * cursore**: dentro un trascinamento un cursore passa per cento valori, e 'Annulla' disfa un gesto
 * compiuto.
 *
 * I confronti col prima sono due (richiesta dell'utente): il tocco lungo sull'immagine mostra
 * l'originale intero, il tocco lungo sul nome di un cursore l'immagine senza quel solo cursore.
 * Il palco non fa la sottrazione: il valore da mostrare lo costruisce la scheda, che ha sia il
 * valore vivo sia il modo di azzerare quel campo.
 */
@Composable
fun AdvancedEditorScreen(
    uri: Uri,
    /** Se una scrittura è in corso: i comandi si spengono, o si salverebbe due volte. */
    busy: Boolean,
    /**
     * Se una filigrana è pronta da scrivere: come nell'editor semplice, rende 'Salva' toccabile su
     * un'immagine che nessun cursore ha ancora toccato.
     */
    marked: Boolean,
    /** Se l'interruttore della filigrana è acceso: il tasto in testata prende l'accento. */
    marking: Boolean,
    /** Se un logo è stato scelto: senza, il tasto in testata non c'è. */
    hasMark: Boolean,
    /** Accende o spegne la filigrana, che è la stessa chiave delle sue impostazioni. */
    onMark: (Boolean) -> Unit,
    /** Apre le impostazioni della filigrana, dal tocco lungo su quel tasto. */
    onMarkSetup: () -> Unit,
    /**
     * Il ridimensionamento configurato, che esiste anche quando non si applica: spegnere
     * l'interruttore non fa perdere quello che si era scelto.
     */
    resize: Resize.Plan,
    /**
     * Il ridimensionamento predefinito: la finestra lo confronta col piano che ha, o 'Rendi
     * predefinito' resterebbe acceso dopo averlo toccato.
     */
    saved: Resize.Plan,
    /** Se quel piano si applica al salvataggio (l'interruttore del ridimensionamento). */
    resizing: Boolean,
    /** `null` spegne; un piano lo scrive e accende, come 'Applica' della sua finestra. */
    onResize: (Resize.Plan?) -> Unit,
    /** Il piano diventa il predefinito: il comando 'Rendi predefinito'. */
    onResizeDefault: (Resize.Plan) -> Unit,
    /**
     * Che cosa applicare al file vero. Il lavoro lo fa chi chiama, come per l'editor semplice.
     *
     * Il secondo argomento è il tocco lungo: `true` chiede un file nuovo accanto all'originale.
     * Vedi [SaveButton].
     */
    onSave: (Look, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // ⚠️ I quattro stili del carattere del testo si caricano una volta, fuori dal thread
    // principale: otto file di qualche megabyte tratterrebbero il primo fotogramma dell'editor.
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { Faces.load(context.applicationContext) } }
    /*
     * ⚠️ **La rotazione è bloccata in verticale, come nell'editor semplice** (risposta
     * `d-rotazione`): i cursori vivono in una colonna in fondo, e in orizzontale prenderebbero
     * metà schermo.
     * ⚠️⚠️ **Tranne con gli strumenti a lato** ([Adaptive.editorBeside]: da 1.024 dp, e in
     * orizzontale su uno schermo grande): là l'orizzontale è previsto, e forzare `PORTRAIT` farebbe
     * sfarfallare il layout.
     */
    val activity = remember(context) { Knobs.activityOf(context) }
    val beside = editorBeside()
    val lockPortrait = !beside
    DisposableEffect(activity, lockPortrait) {
        if (!lockPortrait) return@DisposableEffect onDispose { }
        val prima = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            if (activity != null && prima != null) activity.requestedOrientation = prima
        }
    }

    var origin by remember(uri) { mutableStateOf<Bitmap?>(null) }

    /*
     * La storia dei valori, dal primo all'ultimo, e dove si è dentro di lei.
     *
     * ⚠️⚠️ **Una lista e un indice, non due pile**: con due pile ogni passo nuovo dovrebbe
     * svuotare la seconda, o 'Ripristina' riporterebbe a una strada abbandonata. Con l'indice un
     * passo nuovo taglia tutto quello che veniva dopo, in una riga sola.
     * Il primo elemento è sempre il niente: 'Annulla' fino in fondo torna all'originale, e
     * 'Originale' porta l'indice a zero.
     * ⚠️ **Vivono in [EditorKeep], un `ViewModel`**, perché sopravvivano alla ricreazione
     * dell'attività (vedi là); si svuota quando l'editor si chiude davvero.
     */
    val keep: EditorKeep = viewModel()
    keep.open(uri)
    DisposableEffect(keep) {
        onDispose { if (activity?.isChangingConfigurations != true) keep.close() }
    }
    var history by keep.history
    var at by keep.at

    /** Quello che si vede adesso, compreso il movimento di cursore ancora in corso. */
    var look by keep.look

    /** Se il dito è premuto sull'immagine: si guarda l'originale intero. */
    var comparing by remember(uri) { mutableStateOf(false) }

    /**
     * Il confronto di un cursore solo: come togliere quel cursore, o `null`.
     *
     * ⚠️⚠️ **Un cambiamento e non un'immagine già fatta**: un [Look] fissato al momento del tocco
     * congelerebbe quello che si vede se il confronto restasse acceso, e gli altri cursori
     * sembrerebbero azzerati. Applicato qui, a quello che si vede adesso, l'immagine segue i
     * cursori comunque.
     */
    var peek by remember(uri) { mutableStateOf<((Look) -> Look)?>(null) }

    /** Dove si ha lo sguardo nella scheda, e se il colore mirato è armato: vedi [Gaze]. */
    val gaze = rememberSaveable(uri, saver = Gaze.Saver) { Gaze() }
    val busy = busy || gaze.healingBusy

    // L'avviso 'area troppo grande' sparisce dopo 10 s, o al tocco altrove (voce `3.25-02`).
    LaunchedEffect(gaze.healingFailed) {
        if (!gaze.healingFailed) return@LaunchedEffect
        kotlinx.coroutines.delay(10_000)
        gaze.healingFailed = false
    }

    /*
     * ⚠️⚠️ **Con del lavoro di sviluppo in corso, Indietro chiede prima di uscire** (richiesta
     * dell'utente): 'Vuoi scartare le modifiche?'. Le due porte, la freccia in testata e il gesto
     * di sistema, passano da una funzione sola, [leave].
     * La domanda la fa la tabella dei moduli ([developed]), la stessa che accende il punto
     * d'accento dei gettoni. Il Ritaglio resta fuori per scelta dell'utente, e per la stessa
     * ragione l'editor semplice non chiede mai.
     * Durante un salvataggio non chiede: non si scarta niente, e il salvataggio finisce da sé nel
     * modello. Con l'avviso in scena, Indietro lo chiude e si resta.
     */
    var leaving by remember(uri) { mutableStateOf(false) }
    fun leave() {
        if (gaze.healingBusy) return
        if (!busy && (developed(look) || !gaze.selection.idle)) leaving = true else onBack()
    }
    BackHandler { leave() }
    if (leaving) {
        DiscardDialog(
            onDismiss = { leaving = false },
            onDiscard = {
                leaving = false
                onBack()
            }
        )
    }

    LaunchedEffect(uri) {
        val letta = withContext(Dispatchers.IO) { preview(context, uri) }
        /*
         * ⚠️ **Il verso del ritaglio lo decide l'immagine** (regola dell'utente, scritta su
         * [startLay]). Si decide prima di consegnare l'anteprima, così non esiste un fotogramma
         * col verso sbagliato, e una volta sola, perché dopo il primo tocco il verso è una scelta.
         */
        if (letta != null && gaze.lay == LAY_UNSET) gaze.lay = startLay(letta).ordinal
        origin = letta
    }

    /**
     * La lettura a pezzi del file vero, per quando si ingrandisce: `null` finché non si sa, e
     * `null` per sempre se quel file non si sa rileggere a pezzi.
     *
     * ⚠️⚠️ **Serve perché questo editor lavora su un'anteprima** (risposta `pieno` a
     * `d-dett-vedere`): l'immagine che si sviluppa è ridotta a 1600 pixel di lato, quindi
     * ingrandendo si guarderebbero i suoi pixel, dove la grana che il Dettaglio toglie è già
     * mediata. Si rilegge dal file la sola finestra inquadrata; l'immagine intera a venti
     * megapixel sarebbero ottanta megabyte fermi.
     */
    var full by remember(uri) { mutableStateOf<RegionSource?>(null) }
    LaunchedEffect(uri, origin) {
        val base = origin ?: return@LaunchedEffect
        if (full == null) full = RegionSource.open(context, uri, base)
    }
    /*
     * ⚠️ **Il decodificatore tiene i byte del file in memoria nativa**, quindi si chiude quando la
     * schermata se ne va. La variabile locale serve: `onDispose` deve chiudere quello che
     * l'effetto aveva, e leggendo lo stato delegato prenderebbe quello di adesso.
     */
    val opened = full
    DisposableEffect(opened) {
        onDispose { opened?.close() }
    }

    /**
     * Un passo compiuto entra nella storia, e taglia quello che veniva dopo.
     *
     * ⚠️⚠️ **Entra [look], quello che si vede, e non un valore del cursore**: un cursore non sa
     * che cosa fanno gli altri. [look] si legge qui e non si cattura, perché è delegato a uno
     * stato e va letto nel momento in cui la funzione gira (vedi `onSettled` di [LookKnob]).
     */
    fun push() {
        if (look == history[at]) return
        history = history.take(at + 1) + look
        at = history.size - 1
    }

    /**
     * Sceglie l'elemento [index] del modulo Disegno, o toglie la scelta con `null`: il modulo ne
     * carica i parametri, così campioni e cursori li mostrano e li cambiano ([Gaze.load]).
     */
    fun pick(index: Int?) {
        val segno = index?.let { look.drawing.marks.getOrNull(it) }
        gaze.picked = if (segno != null) index else null
        segno?.let { gaze.load(it) }
    }

    /**
     * Un gesto sul palco del modulo Disegno è finito: la storia prende il disegno, e il modulo
     * ricarica i parametri dell'elemento scelto.
     * ⚠️⚠️ **Il ricaricamento serve** (nota dell'utente sulla voce `4.91-03`): una maniglia cambia
     * la misura di un testo sull'elemento e non nel modulo, e il cambio di campione successivo gli
     * rimetterebbe la misura vecchia del modulo ([Gaze.restyle]).
     */
    fun drawn() {
        push()
        gaze.picked?.let { look.drawing.marks.getOrNull(it) }?.let { gaze.load(it) }
    }

    /**
     * Il colore mirato: che cosa vuol dire aver toccato il pixel [pixel].
     *
     * ⚠️ **Il mirato è del solo HSL** (risposta `via` a `d-mirato-curve`): nelle Curve si lavora
     * sul grafico. Quello che si indica è sempre quale colore, una scelta fra otto fasce. Il
     * mirino si trascina per scegliere un altro colore senza alzare il dito (richiesta
     * dell'utente).
     */
    fun aimStart(pixel: Int) {
        Mix.bandOf(pixel).takeIf { it >= 0 }?.let { gaze.band = it }
    }

    val scope = rememberCoroutineScope()
    fun applyHealing() {
        if (busy || gaze.healingBusy || gaze.selection.idle) return
        val originalLook = look
        val selection = gaze.selection
        val retainedBytes = history.flatMap { it.healing.patches }.distinct().sumOf { it.bytes.toLong() }
        gaze.healingBusy = true
        gaze.healingFailed = false
        scope.launch {
            try {
                val patch = Healing.prepare(context, uri, full, originalLook.healing, selection)
                ensureActive()
                if (patch != null && retainedBytes + patch.bytes <= Healing.MAX_HISTORY_BYTES) {
                    look = originalLook.copy(healing = Healing.Plan(originalLook.healing.patches + patch))
                    push()
                    gaze.selection = Healing.Selection.NONE
                    gaze.healingFailed = false
                } else gaze.healingFailed = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                gaze.healingFailed = true
            } finally {
                gaze.healingBusy = false
            }
        }
    }

    /*
     * Il mini onboarding della fila dei moduli (richiesta dell'utente): la fila continua fuori
     * dallo schermo, e uno scorrimento non si dichiara da sé.
     * ⚠️ **Il riquadro da illuminare arriva da una misura** ([strip]), non da un conto: rifare qui
     * la catena dei rientri della scheda sarebbe una seconda geometria (vedi [HintSpot]).
     * Parte da 'già visto', come gli altri veli: il valore vero arriva dal disco un fotogramma
     * dopo, e partendo dal contrario il velo lampeggerebbe a ogni apertura.
     */
    val hinted by produceState(true) { Hint.MODULES.flow(context).collect { value = it } }
    var strip by remember { mutableStateOf(Rect.Zero) }

    /*
     * La seconda slide, che evidenzia 'Filigrana', 'Ridimensiona' e 'Salva' (richiesta
     * dell'utente): arriva dopo la prima, e il suo testo comincia con 'Oltre ai moduli', quindi
     * vive solo qui e non nell'editor semplice.
     * ⚠️ **La chiave è sua** ([Hint.EDITOR_TOOLS]): chi ha già archiviato [Hint.MODULES] deve
     * vedere lo stesso questa.
     * I riquadri si misurano, perché i tasti non sono larghi uguali e 'Filigrana' c'è solo con un
     * logo scelto: un riquadro vuoto vuol dire che quel tasto non c'è.
     */
    val toolsHinted by produceState(true) {
        Hint.EDITOR_TOOLS.flow(context).collect { value = it }
    }
    val drawHinted by produceState(true) { Hint.DRAW.flow(context).collect { value = it } }
    var markSpot by remember { mutableStateOf(Rect.Zero) }
    var resizeSpot by remember { mutableStateOf(Rect.Zero) }
    var saveSpot by remember { mutableStateOf(Rect.Zero) }

    /*
     * ⚠️⚠️ **Quanti pixel il salvataggio avrà davanti, non quanti ne ha il file**: il
     * ridimensionamento si applica dopo la posa e il ritaglio. Il conto vive in
     * [Resize.frameSize]. Senza la misura il ridimensionamento non conta come lavoro da salvare,
     * perché non si sa se rimpicciolisce.
     */
    val longSide = rememberLongSide(uri)
    val frame = origin?.let { base ->
        longSide?.let {
            Resize.frameSize(it, base.width, base.height, look.spin.turns, look.crop)
        }
    }
    val shrinks = resizing && frame?.let { (w, h) -> resize.sizeFor(w, h) } != null
    var asking by remember { mutableStateOf(false) }
    if (asking) {
        ResizeDialog(
            initial = resize,
            saved = saved,
            size = frame,
            onDismiss = { asking = false },
            onApply = {
                asking = false
                onResize(it)
            },
            onDefault = onResizeDefault
        )
    }

    /*
     * Il menu dell'elemento scelto del modulo Disegno: tenere premuto un elemento lo sceglie e apre
     * questo menu ([LookStage]). Tiene l'indice e il disegno del momento in cui si è aperto, così i
     * suoi tasti non cambiano sotto il dito mentre se ne va.
     */
    val drawMenu = rememberMenuState()
    var drawFor by remember(uri) { mutableStateOf<Pair<Int, Drawing>?>(null) }
    fun holdDraw() {
        gaze.picked?.let { drawFor = it to look.drawing }
        drawMenu.open()
    }

    /*
     * ⚠️ **Le parole di un testo si scrivono in una finestra loro**, modale come ogni finestra
     * che raccoglie un input scritto. Un testo nuovo si posa centrato dove il dito ha toccato e
     * nasce scelto, così i tasti del modulo lo cambiano subito; quello scelto tiene posto e stile,
     * e prende le parole nuove. Un testo vuoto non è un testo: il tasto di conferma aspetta una
     * lettera. Una pastiglia o un pannello possono non avere parole (nota C dell'utente sul giro
     * della 4.91), ma un tocco senza parole non posa niente, perché li misura sulle parole.
     */
    fun askText(at: Offset) {
        gaze.textAsk = TextAsk(at, null, "")
    }
    // ⚠️ Una pastiglia o un pannello appena disegnati nascono scelti, e le parole si chiedono subito.
    fun askPill(i: Int) {
        pick(i)
        gaze.textAsk = TextAsk(null, i, "")
    }
    // ⚠️ Il colore sotto il testo scelto, o sotto l'immagine intera, per la fila dei fondi.
    LaunchedEffect(origin, gaze.picked, look.drawing) {
        val base = origin ?: return@LaunchedEffect
        val segno = gaze.picked?.let { look.drawing.marks.getOrNull(it) }?.takeIf { it.pen == Pen.TEXT }
        gaze.under = withContext(Dispatchers.Default) { Draw.under(base, segno) }
    }
    gaze.textAsk?.let { ask ->
        val per = ask.index?.let { look.drawing.marks.getOrNull(it)?.pen } ?: gaze.pen
        TextDialog(
            initial = ask.text,
            blank = per.framed,
            onDismiss = { gaze.textAsk = null },
            onDone = { parole ->
                gaze.textAsk = null
                val dove = ask.at
                if (ask.index == null && dove != null) {
                    // ⚠️ Un tocco posa pastiglia o pannello intorno alle parole: senza, niente.
                    if (parole.isBlank()) return@TextDialog
                    val w = origin?.width?.toFloat() ?: 1f
                    val h = origin?.height?.toFloat() ?: 1f
                    // ⚠️ Un testo nuovo nasce con la riga più larga a metà della larghezza
                    // dell'immagine (nota sulla voce `4.91-03`), anche dentro pastiglia o pannello.
                    val nuovo = if (gaze.pen.framed) {
                        val pillola = gaze.pillWords(parole)
                        gaze.penMark().copy(
                            points = Draw.pillAround(pillola, Draw.fitSize(pillola, w, h), dove, w, h, gaze.pen), words = pillola
                        )
                    } else gaze.words(parole).let { testo ->
                        gaze.penMark().copy(points = listOf(dove), width = Draw.fitSize(testo, w, h), words = testo)
                    }
                    look = look.copy(drawing = look.drawing.with(nuovo))
                    push()
                    pick(look.drawing.marks.size - 1)
                } else ask.index?.let { i ->
                    look.drawing.marks.getOrNull(i)?.takeIf { it.pen.written }?.let { segno ->
                        look = look.copy(drawing = look.drawing.replacing(i, segno.copy(words = segno.words?.copy(text = parole))))
                        push()
                    }
                }
            }
        )
    }
    origin?.let { base ->
        drawFor?.let { held ->
            DrawMenu(
                state = drawMenu,
                gaze = gaze,
                held = held,
                w = base.width,
                h = base.height,
                onEdit = { cambia ->
                    look = look.copy(drawing = cambia(look.drawing))
                    push()
                },
                onPick = { pick(it) }
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                        )
                    )
                    // ⚠️ I due lati hanno rientri diversi, come in `EditorScreen`: da una parte
                    // c'è un'icona, dall'altra una parola.
                    .padding(start = 4.dp, end = STAGE_SIDE - TEXT_BUTTON_PAD),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ⚠️ La stessa porta del gesto di sistema, cioè [leave]: vedi la nota su di lei.
                IconButton(onClick = { leave() }) {
                    Icon(Glyphs.Material.ArrowBack, stringResource(R.string.settings_back))
                }
                /*
                 * ⚠️ **In testa c'è quello che si sta facendo, non quale editor è** (istruzione
                 * dell'utente): 'Modifica immagine' in tutte e due le schermate. I nomi dei due
                 * editor restano dove la scelta si fa: il selettore e l'elenco delle app.
                 */
                Text(
                    text = stringResource(R.string.editor_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).heading()
                )
                /*
                 * 'Filigrana' e 'Ridimensiona' sono nella barra in basso, alla portata del
                 * pollice (richiesta dell'utente; vedi [EditorToolBar]): in testata resta il solo
                 * 'Salva', il comando che chiude il lavoro.
                 * Il riquadro lo misura la seconda slide del mini onboarding.
                 */
                Box(modifier = Modifier.onGloballyPositioned { saveSpot = it.boundsInRoot() }) {
                    SaveButton(
                        // ⚠️ Filigrana e ridimensionamento sono lavoro da salvare, come
                        // nell'editor semplice; il ridimensionamento solo se su questa immagine
                        // toglie davvero dei pixel.
                        enabled = origin != null && !busy && gaze.selection.idle && (marked || shrinks || !look.idle),
                        onSave = { onSave(look, false) },
                        onBeside = { onSave(look, true) }
                    )
                }
            }

            /*
             * Con gli strumenti a lato ([beside], già letto per il blocco dell'orientamento, dal
             * mockup dell'editor) la scheda è in una colonna; altrimenti resta in basso, come sul
             * telefono.
             */
            /*
             * ⚠️ **I comandi seguono la mano** (voce `3.39-01`): con la mano destra sono a destra e
             * il palco a sinistra; con la sinistra i lati si scambiano.
             */
            val panelOnStart = LocalPadLook.current.hand == Hand.LEFT
            val toolsWidth = Adaptive.editorToolsWidth(beside)

            @Composable
            fun Sheet() {
            LookSheet(
                beside = beside,
                panelOnStart = panelOnStart,
                toolsWidth = toolsWidth,
                onHealApply = { applyHealing() },
                look = look,
                busy = busy,
                ready = origin != null,
                canUndo = at > 0,
                canRedo = at < history.size - 1,
                origin = origin,
                gaze = gaze,
                onStrip = { strip = it },
                /*
                 * ⚠️ **I due comandi del salvataggio arrivano alla scheda come uno spazio da
                 * riempire**: vivono nella barra in basso, dentro la scheda, ma lo stato che
                 * governano vive qui, e passarlo uno per uno darebbe alla scheda otto argomenti
                 * che non guarda. Il lato lo decide la scheda, che lo conosce già, e torna come
                 * argomento dello spazio.
                 */
                toolCount = if (hasMark) 2 else 1,
                tools = { mirror ->
                    EditorToolBar(
                        mirror = mirror,
                        hasMark = hasMark,
                        marking = marking,
                        resizing = resizing,
                        enabled = !busy,
                        onMark = { onMark(!marking) },
                        onMarkSetup = onMarkSetup,
                        onResize = { onResize(if (resizing) null else resize) },
                        onResizeSetup = { asking = true },
                        onMarkSpot = { markSpot = it },
                        onResizeSpot = { resizeSpot = it }
                    )
                },
                /*
                 * ⚠️⚠️ **Qui si legge lo stato vivo**: arriva un cambiamento da applicare, quindi
                 * il punto di partenza è [look] letto adesso, come in `push`. La ragione per esteso
                 * vive su [Dial.set].
                 */
                onLive = { cambia -> look = cambia(look) },
                onSettled = { push() },
                onPeek = { senza -> peek = senza },
                // ⚠️ I tre tolgono la scelta dell'elemento del Disegno: dopo, il suo indice può
                // essere di un altro.
                // ⚠️⚠️ **E prima mettono nella storia il cambio che aspetta**: un cambio di stile
                // entra 400 ms dopo l'ultimo ritocco, e toccati in quella finestra 'Annulla'
                // tornerebbe a due passi fa perdendo il cambio.
                onUndo = {
                    push()
                    if (at > 0) {
                        at -= 1
                        look = history[at]
                        gaze.picked = null
                    }
                },
                onRedo = {
                    push()
                    if (at < history.size - 1) {
                        at += 1
                        look = history[at]
                        gaze.picked = null
                    }
                },
                onOriginal = {
                    push()
                    gaze.picked = null
                    gaze.selection = Healing.Selection.NONE
                    gaze.healingFailed = false
                    look = Look.NONE
                    push()
                }
            )
            }

            /*
             * Il palco è uno solo per le due disposizioni: cambia soltanto la misura, che il
             * chiamante dà col suo `weight`, alto nella riga degli strumenti a lato e largo nella
             * colonna del telefono. Due copie divergevano senza che nessuna prova lo vedesse.
             */
            @Composable
            fun Stage(size: Modifier) {
                Box(
                    modifier = size
                        .background(stageBack())
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        .padding(horizontal = STAGE_SIDE, vertical = STAGE_PAD),
                    contentAlignment = Alignment.Center
                ) {
                    val picture = origin
                    if (picture == null) {
                        CircularProgressIndicator()
                    } else {
                        LookStage(
                            picture = picture,
                            full = full,
                            /*
                             * ⚠️⚠️ **Il confronto toglie un gruppo di campi e non tutti** (richiesta
                             * dell'utente): nei due moduli che dicono dove va un pixel mostra
                             * l'originale intero; negli altri tiene posa, taglio, geometria e vista, e
                             * confronta il colore dentro l'inquadratura di adesso (vedi [Look.place]).
                             * ⚠️ Nel Ritaglio il dito serve alle squadrette e il confronto non parte:
                             * quel ramo oggi si vede solo nella Geometria, a strumento 'Angoli' spento.
                             */
                            look = when {
                                !comparing -> peek?.invoke(look) ?: look
                                MODULES[gaze.module].extra.places -> Look.NONE
                                else -> look.place
                            },
                            onCompare = { comparing = it },
                            /*
                             * ⚠️⚠️ **Il mirato vale solo nel modulo che lo sa usare, e si guarda qui**:
                             * passando a un altro modulo resterebbe armato, e il palco smetterebbe di
                             * rispondere a pinza e doppio tocco senza che si veda il tasto per spegnerlo.
                             */
                            aiming = { gaze.aiming && MODULES[gaze.module].extra == Extra.BANDS },
                            onAimStart = { aimStart(it) },
                            onAimEnd = { push() },
                            /*
                             * ⚠️ **Il ritaglio si accende dalla stessa tabella**, per la stessa ragione:
                             * le squadrette sono sul palco, e in un altro modulo prenderebbero il dito.
                             */
                            cutting = look.crop.takeIf { MODULES[gaze.module].extra == Extra.CROP },
                            // ⚠️ Dentro la porzione applicata: vedi [frameAspect].
                            keep = cropShape(gaze).value(
                                cropLay(gaze), frameAspect(look, posedAspect(origin, look.spin))
                            ),
                            onCut = { look = look.copy(crop = it) },
                            onCutEnd = { push() },
                            /*
                             * ⚠️ **Lo strumento 'Angoli' si arma come il colore mirato**: stesso tasto e
                             * stesso stato, e quale gesto sia lo dice la tabella dei moduli. In un altro
                             * modulo il valore torna nullo, e le maniglie se ne vanno col tasto.
                             */
                            corners = look.geo.corners.takeIf {
                                gaze.aiming && MODULES[gaze.module].extra == Extra.CORNERS
                            },
                            onCorners = { look = look.copy(geo = look.geo.copy(corners = it)) },
                            onCornersEnd = { push() },
                            liquifying = {
                                gaze.liquifying && MODULES[gaze.module].extra == Extra.CORNERS
                            },
                            rebuild = { gaze.rebuilding },
                            brushRadius = { gaze.brushRadius },
                            brushSizing = { gaze.brushSizing && gaze.liquifying },
                            brushStrength = { gaze.brushStrength },
                            onLiquify = { look = look.copy(geo = look.geo.copy(liquify = it)) },
                            onLiquifyEnd = { push() },
                            healing = { gaze.healing && !busy && MODULES[gaze.module].extra == Extra.HEALING },
                            selection = gaze.selection.takeIf { MODULES[gaze.module].extra == Extra.HEALING },
                            healingRadius = { gaze.healingRadius },
                            healingSizing = { gaze.healingSizing },
                            leveling = { gaze.leveling && MODULES[gaze.module].extra == Extra.CORNERS },
                            drawPen = {
                                gaze.penMark().takeIf { !busy && MODULES[gaze.module].extra == Extra.DRAW }
                            },
                            onDraw = { look = look.copy(drawing = it) },
                            onDrawEnd = { drawn() },
                            inkSizing = { gaze.inkSizing && MODULES[gaze.module].extra == Extra.DRAW },
                            onViewLong = { if (gaze.viewLong != it) gaze.viewLong = it },
                            drawPicked = { gaze.picked },
                            onDrawPick = { i -> pick(i) },
                            drawTurning = { gaze.turning },
                            onDrawTurn = { gaze.turning = !gaze.turning },
                            onDrawText = { at -> askText(at) },
                            onDrawWords = { i -> gaze.askWords(look.drawing, i) },
                            onDrawPill = { i -> askPill(i) },
                            onDrawHold = { holdDraw() },
                            onHealPaint = { polygon ->
                                if (!gaze.healingBusy) {
                                    if (gaze.selection.polygons.size < Healing.MAX_POLYGONS) {
                                        gaze.selection = gaze.selection.add(polygon)
                                        gaze.healingFailed = false
                                    } else gaze.healingFailed = true
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    // Il promemoria della selezione è sopra il palco, così dipingere non ne cambia le misure.
                    if (!gaze.selection.idle) {
                        Text(
                            stringResource(R.string.look_heal_pending),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .clickable {
                                    // ⚠️ Toccandolo si va a 'Correggi/Rimuovi' (voce `3.24-05`).
                                    val i = MODULES.indexOfFirst { it.extra == Extra.HEALING }
                                    if (i >= 0) {
                                        gaze.module = i
                                        gaze.healing = true
                                    }
                                }
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainer,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    /*
                     * ⚠️ **L'avviso 'area troppo grande' è sopra la scheda, non dentro** (nota
                     * dell'utente sul giro della 3.24): stesso disegno del promemoria della
                     * selezione, appoggiato al bordo basso del palco.
                     */
                    if (gaze.healingFailed) {
                        Text(
                            stringResource(R.string.look_heal_failed),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainer,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { gaze.healingFailed = false }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (beside) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (panelOnStart) {
                        Sheet()
                    }
                    Stage(Modifier.weight(1f).fillMaxHeight())
                    if (!panelOnStart) {
                        Sheet()
                    }
                }
            } else {
                Stage(Modifier.weight(1f).fillMaxWidth())
                Sheet()
            }
        }

        if (!hinted && strip != Rect.Zero) {
            HintStrip(
                text = stringResource(R.string.hint_modules),
                spot = strip,
                icons = LocalPadLook.current.mods.map { modGlyph(it) },
                onDone = { scope.launch { Hint.MODULES.remember(context) } }
            )
        }

        /*
         * ⚠️ **Viene dopo la prima, e la condizione lo dice**: due veli sovrapposti sarebbero un
         * fondo doppio con due frasi. Toccando il primo, la seconda slide arriva al fotogramma dopo.
         * I gruppi sono due, 'Salva' in testata e i tasti nella barra in basso, ognuno con il suo
         * paragrafo (richiesta dell'utente): da che parte cada la frase lo decide [HintSpots]. I
         * testi sono quelli dell'utente, divisi in `hint_save` e `hint_tools`.
         * Il disegno lo passa il chiamante, perché sono due glifi e una parola; i glifi sono gli
         * stessi dei tasti veri ([Glyphs.Watermark] e [Glyphs.Resize]).
         * La condizione guarda il riquadro di 'Ridimensiona', il tasto che c'è sempre: prima della
         * misura quel gruppo sarebbe vuoto.
         */
        if (hinted && !toolsHinted && !saveSpot.isEmpty && !resizeSpot.isEmpty) {
            val sopra = HintGroup(
                // ⚠️ AIV Play non sovrascrive mai l'originale, quindi la sua frase dice dove va il
                // file nuovo (`ImageEdit.outDir`).
                text = stringResource(if (Store.files) R.string.hint_save else R.string.hint_save_play),
                spots = listOf(
                    saveSpot to {
                        Text(
                            text = stringResource(R.string.editor_save),
                            style = MaterialTheme.typography.labelLarge,
                            color = HINT_MARK
                        )
                    }
                )
            )
            val sotto = HintGroup(
                text = stringResource(R.string.hint_tools),
                spots = buildList {
                    if (!markSpot.isEmpty) {
                        add(markSpot to { Icon(Glyphs.Watermark, null, tint = HINT_MARK) })
                    }
                    add(resizeSpot to { Icon(Glyphs.Resize, null, tint = HINT_MARK) })
                }
            )
            HintSpots(
                groups = listOf(sopra, sotto),
                onDone = { scope.launch { Hint.EDITOR_TOOLS.remember(context) } }
            )
        }

        /*
         * Il velo del modulo Disegno (nota B dell'utente sul giro della 4.60): la prima volta che il
         * modulo è in scena, dopo le due slide dell'editor. La copia è il campione rosso disegnato
         * come quello scelto, con l'anello in [HINT_MARK]; il campione è nella metà di sotto, quindi
         * [HintSpots] posa il testo sopra.
         */
        if (hinted && toolsHinted && !drawHinted && MODULES[gaze.module].extra == Extra.DRAW && !gaze.redSpot.isEmpty) {
            HintSpots(
                groups = listOf(
                    HintGroup(
                        text = stringResource(R.string.hint_draw),
                        spots = listOf(
                            gaze.redSpot to {
                                Canvas(Modifier.fillMaxSize()) {
                                    val r = size.minDimension / 2f
                                    drawCircle(HINT_MARK, radius = r)
                                    drawCircle(Color(Draw.INKS.first()), radius = r - SWATCH_RING.toPx())
                                }
                            }
                        )
                    )
                ),
                onDone = { scope.launch { Hint.DRAW.remember(context) } }
            )
        }
    }
}

/**
 * Il comando che scrive, in testata: il tocco riscrive il file, il tocco lungo ne fa uno nuovo.
 *
 * Il tocco lungo è una richiesta dell'utente: tiene anche l'originale senza uscire a duplicarlo.
 *
 * ⚠️⚠️ **Non è un `TextButton`**, come i gettoni dei moduli: quel pezzo di Material non offre un
 * secondo gesto, e un `pointerInput` nel modificatore sarebbe un secondo nodo che consuma il tocco
 * prima del tasto. Con `combinedClickable` i gesti sono due e il bersaglio uno.
 * L'etichetta del gesto lungo si dichiara, perché il lettore di schermo la annunci (come
 * [PadAction.holdLabel]). Rientro e corpo sono quelli di un `TextButton`, come il 'Salva'
 * dell'editor semplice.
 */
@Composable
private fun SaveButton(
    enabled: Boolean,
    onSave: () -> Unit,
    onBeside: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Text(
        text = stringResource(R.string.editor_save),
        style = MaterialTheme.typography.labelLarge,
        color = if (enabled) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface.copy(alpha = OFF_INK),
        modifier = Modifier
            .clip(CircleShape)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onLongClickLabel = stringResource(R.string.editor_save_beside),
                onLongClick = {
                    haptics.performHapticFeedback(HOLD_BUZZ)
                    onBeside()
                },
                hapticFeedbackEnabled = false,
                onClick = onSave
            )
            .padding(horizontal = TEXT_BUTTON_PAD, vertical = TEXT_BUTTON_PAD / 2)
    )
}

/**
 * La domanda prima di uscire con del lavoro in corso: vedi la nota su `leave`.
 *
 * La domanda è dell'utente alla lettera e non ha un corpo, che ripeterebbe il titolo. Il tasto
 * che scarta ha il colore dell'errore, come 'Elimina': è quello che non si disfa.
 * ⚠️ **Non è una modale vera** (`Rules.md` § '👆 Che cosa fa il tocco FUORI da una finestra'):
 * non si scrive niente, e il tocco fuori vale 'Annulla', cioè si resta col lavoro intatto.
 */
@Composable
private fun DiscardDialog(
    onDismiss: () -> Unit,
    onDiscard: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(onDismiss),
        properties = loweredWindow(onDismiss),
        title = { Text(stringResource(R.string.editor_discard_ask)) },
        confirmButton = {
            TextButton(
                onClick = onDiscard,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text(stringResource(R.string.editor_discard)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Il palco: l'immagine con il conto applicato sopra, e lo zoom per guardarne i dettagli.
 *
 * ⚠️⚠️ **Il conto si applica col pennello e non con un secondo bitmap**: uno shader costa un
 * passaggio sulla scheda grafica a ogni fotogramma, mentre rigenerare una mappa di pixel a ogni
 * movimento del cursore vorrebbe decine di millisecondi, cioè un cursore che scatta. A riposo lo
 * shader non si mette: è più veloce, e il confronto col prima mostra davvero l'immagine di
 * partenza.
 *
 * Lo zoom è una richiesta dell'utente. ⚠️ **Ingrandisce l'anteprima e non il file**: oltre un
 * certo ingrandimento si vedrebbero i suoi pixel, e il tetto è [ZOOM_MAX] (il pezzo a piena
 * risoluzione arriva da [full]). Si ingrandisce anche a una mano: il secondo tocco di un doppio
 * tocco resta giù e trascina (voce `zoom-corsa`); il conto vive su [pulled], e il punto toccato
 * resta fermo.
 *
 * ⚠️⚠️ **Tutti i gesti vivono in un rilevatore solo**: il tocco lungo del confronto e la pinza
 * nascono dallo stesso dito, e in due `pointerInput` si contenderebbero l'evento. Il caso peggiore
 * è il confronto acceso durante una pinza, perché `waitForUpOrCancellation` risponde `null` sia
 * allo scadere del tempo sia a un evento consumato da altri.
 */
@Composable
private fun LookStage(
    picture: Bitmap,
    /**
     * Da dove rileggere il file a piena risoluzione quando [picture] viene ingrandita oltre i
     * propri pixel, o `null` se quel file non si sa rileggere a pezzi.
     */
    full: RegionSource?,
    look: Look,
    onCompare: (Boolean) -> Unit,
    /**
     * Se il colore mirato è acceso, cioè se il dito lavora sull'immagine invece che sui comandi.
     *
     * ⚠️⚠️ **Una funzione e non un valore**: la legge il corpo di un `pointerInput`, che si
     * ricostruisce solo quando cambiano le sue chiavi, e un valore catturato invecchierebbe. La
     * chiave non si tocca: metterci il mirato annullerebbe il gesto in corso a ogni armamento.
     */
    aiming: () -> Boolean,
    /** Il pixel toccato, al principio di un gesto mirato: vedi [AdvancedEditorScreen]. */
    onAimStart: (Int) -> Unit,
    /** Il gesto mirato è finito: quello che si è fatto diventa un passo della storia. */
    onAimEnd: () -> Unit,
    /**
     * Il rettangolo da tenere mentre il modulo Ritaglio è in scena, e `null` quando non lo è.
     *
     * ⚠️ **Il Ritaglio mette i suoi comandi sul palco**, le quattro squadrette: finché è in scena
     * il palco fa solo quello, e pinza, panoramica, doppio tocco e confronto restano fermi.
     * Nullo e non un booleano accanto al valore: così non esiste 'ritaglia ma senza rettangolo'.
     */
    cutting: ImageEdit.Crop?,
    /**
     * Il rapporto che il rettangolo deve tenere mentre lo si tira, e `null` per la forma libera.
     *
     * ⚠️ **Larghezza diviso altezza in pixel**, l'unità del rettangolo sullo schermo: le frazioni
     * del ritaglio non sono la proporzione, perché i lati dell'immagine sono diversi.
     */
    keep: Float?,
    onCut: (ImageEdit.Crop) -> Unit,
    /** Il gesto del ritaglio è finito: quello che si è fatto diventa un passo della storia. */
    onCutEnd: () -> Unit,
    /**
     * I quattro angoli mentre lo strumento 'Angoli' è armato, e `null` quando non lo è.
     *
     * Armato, il palco fa solo questo, come col colore mirato e col Ritaglio.
     * ⚠️⚠️ **L'immagine si vede intera e rimpicciolita**: con la scala di copertura un angolo
     * tirato in fuori finirebbe oltre il bordo dello schermo, proprio la maniglia che si tira. Il
     * riquadro che resterà è disegnato sopra (vedi [ARMED_FIT]).
     */
    corners: Corners?,
    onCorners: (Corners) -> Unit,
    /** Il gesto su un angolo è finito: quello che si è fatto diventa un passo della storia. */
    onCornersEnd: () -> Unit,
    liquifying: () -> Boolean,
    rebuild: () -> Boolean,
    brushRadius: () -> Float,
    brushSizing: () -> Boolean,
    brushStrength: () -> Float,
    onLiquify: (Liquify) -> Unit,
    onLiquifyEnd: () -> Unit,
    healing: () -> Boolean,
    selection: Healing.Selection?,
    healingRadius: () -> Float,
    healingSizing: () -> Boolean,
    onHealPaint: (List<Offset>) -> Unit,
    /** Se le linee dei terzi sono accese: vedi [Gaze.leveling]. */
    leveling: () -> Boolean,
    /**
     * Lo strumento del modulo Disegno come elemento vuoto, o `null` quando il modulo non è in
     * scena: con uno strumento il palco disegna con un dito e ingrandisce con due, come Fluidifica.
     */
    drawPen: () -> Mark?,
    /** Il disegno mentre il dito si muove: lo scrive il palco, e la schermata lo tiene nel look. */
    onDraw: (Drawing) -> Unit,
    /** Il dito si è alzato: quello che si è disegnato diventa un passo della storia. */
    onDrawEnd: () -> Unit,
    /** Se un dito tiene la manopola 'Spessore' del modulo Disegno: vedi [Gaze.inkSizing]. */
    inkSizing: () -> Boolean,
    /** Il lato lungo dell'immagine sul palco, in pixel, col modulo Disegno in scena: vedi [Gaze.viewLong]. */
    onViewLong: (Float) -> Unit,
    /** L'indice dell'elemento scelto del modulo Disegno, o `null`: vedi [Gaze.picked]. */
    drawPicked: () -> Int?,
    /** Un tocco ha scelto l'elemento a questo indice, o tolto la scelta con `null`. */
    onDrawPick: (Int?) -> Unit,
    /** Se le maniglie dell'elemento scelto lo ruotano invece di deformarlo: vedi [Gaze.turning]. */
    drawTurning: () -> Boolean,
    /** Un tocco sull'elemento scelto: 'Trasforma' e 'Ruota' si scambiano, vedi [Gaze.turning]. */
    onDrawTurn: () -> Unit,
    /** Un tocco sul vuoto con lo strumento Testo, in questo punto dell'immagine originale: si chiedono le parole. */
    onDrawText: (Offset) -> Unit,
    /** Un doppio tocco sull'elemento a questo indice, testo, pastiglia o pannello: si chiedono le sue parole. */
    onDrawWords: (Int) -> Unit,
    /** Una pastiglia appena disegnata, a questo indice: nasce scelta e se ne chiedono le parole. */
    onDrawPill: (Int) -> Unit,
    /** Un dito fermo su un elemento, che il palco ha appena scelto: si apre il suo menu. */
    onDrawHold: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val hold = stringResource(R.string.look_compare)
    /**
     * La geometria di adesso, per chi la legge dentro un gesto.
     *
     * ⚠️⚠️ **La stessa prudenza di [aiming]**: un `look` catturato dal corpo del `pointerInput`
     * avrebbe la geometria di quando il nodo è nato. La chiave non si tocca: metterci il `look`
     * annullerebbe il gesto in corso a ogni cursore mosso.
     */
    val geoNow by rememberUpdatedState(look.geo)
    /** Il rettangolo di adesso e dove scriverlo, per chi li legge dentro un gesto: vedi [geoNow]. */
    val cutNow by rememberUpdatedState(cutting)
    val cutTo by rememberUpdatedState(onCut)
    /** Il rapporto da tenere, letto dentro il gesto e non catturato: vedi [geoNow]. */
    val keepNow by rememberUpdatedState(keep)
    /** Gli angoli di adesso e dove scriverli, per chi li legge dentro un gesto: vedi [geoNow]. */
    val cornerNow by rememberUpdatedState(corners)
    val cornerTo by rememberUpdatedState(onCorners)
    val liquifyNow by rememberUpdatedState(look.geo.liquify)
    val liquifyTo by rememberUpdatedState(onLiquify)
    val spinNow by rememberUpdatedState(look.spin)
    val healTo by rememberUpdatedState(onHealPaint)
    val healingNow by rememberUpdatedState(healing)
    val drawingNow by rememberUpdatedState(look.drawing)
    val drawTo by rememberUpdatedState(onDraw)
    val drawEnd by rememberUpdatedState(onDrawEnd)
    val penNow by rememberUpdatedState(drawPen)
    val pickedNow by rememberUpdatedState(drawPicked)
    val pickNow by rememberUpdatedState(onDrawPick)
    val turningNow by rememberUpdatedState(drawTurning)
    val turnNow by rememberUpdatedState(onDrawTurn)
    val textNow by rememberUpdatedState(onDrawText)
    val wordsNow by rememberUpdatedState(onDrawWords)
    /** L'ultimo tocco su un elemento del modulo Disegno, per leggere il successivo come doppio tocco. */
    val lastTap = remember { MarkTap() }
    val pillNow by rememberUpdatedState(onDrawPill)
    val holdNow by rememberUpdatedState(onDrawHold)
    val airPx = with(LocalDensity.current) { CROP_AIR.toPx() }
    val metrics = LocalContext.current.resources.displayMetrics
    val brushCmPx = (metrics.xdpi.takeIf { it > 0f } ?: metrics.densityDpi.toFloat()) / 2.54f
    val brushAccent = MaterialTheme.colorScheme.primary

    /**
     * L'aria da lasciare intorno all'immagine: quanta ne chiedono le squadrette del ritaglio.
     *
     * ⚠️⚠️ **Vale in tutti i moduli, anche in quelli senza squadrette** (punto C dell'utente sul
     * giro della 2.36): l'immagine non deve rimpicciolirsi passando al Ritaglio, quindi le maniglie
     * sono fuori dallo spazio dell'anteprima. Il prezzo è qualche punto per lato negli altri
     * moduli, meno dell'uno per cento: lo stesso compromesso di [SteadyBody].
     * È una funzione per la ragione di [geoNow]: la leggono il disegno e i gesti.
     */
    fun air(): Float = airPx
    /*
     * L'anteprima messa in posa, cioè quello che si guarda e si tocca.
     *
     * ⚠️⚠️ **Si gira la mappa di pixel e non il disegno**: così viewport, shader, maglia della
     * geometria, lente e ritaglio lavorano su un'immagine normale, senza un caso in più per la
     * posa. Il conto è `spunBy`, lo stesso del salvataggio: un quarto di giro è una permutazione
     * di pixel. Costa una copia a ogni posa nuova; la vecchia non si ricicla a mano, perché può
     * essere ancora dentro un disegno in corso.
     * ⚠️ **Il vetro di pastiglie e pannelli del Disegno cambia l'immagine stessa**, prima dello
     * sviluppo, come le toppe di Correggi ([Draw.blurAreas]): sono chiave dell'anteprima in posa,
     * gli altri elementi no, così una linea non risfoca l'immagine. Ci sono tutti gli elementi col
     * vetro, pastiglie comprese, o il palco le mostrerebbe senza vetro e il file salvato con.
     */
    val sfocature = remember(look.drawing) { Drawing(look.drawing.marks.filter { it.glass }) }
    val posed = remember(picture, look.spin, look.healing, sfocature) {
        val curato = Healing.render(picture, look.healing)
        Draw.blurAreas(curato, sfocature, mine = curato !== picture).spunBy(look.spin.turns, look.spin.mirror)
    }
    /*
     * Il disegno del modulo Disegno, nella cornice in posa dell'anteprima (vedi `Draw.overlay`).
     * ⚠️ Un bitmap solo, tenuto fra i fotogrammi e ridisegnato sul posto: il dito cambia il
     * disegno a ogni movimento, e un bitmap nuovo ogni volta sarebbe megabyte di memoria da
     * liberare al secondo.
     */
    val drawingHolder = remember(picture, look.spin) { arrayOfNulls<Bitmap>(1) }
    val drawingMask = remember(picture, look.drawing, look.spin) {
        Draw.overlay(look.drawing, picture.width, picture.height, look.spin, drawingHolder[0])
            ?.also { drawingHolder[0] = it }
    }
    val selectionMask = remember(picture, selection, look.spin, brushAccent) {
        selection?.let { Healing.overlay(it, picture.width, picture.height, brushAccent.toArgb()) }
            ?.spunBy(look.spin.turns, look.spin.mirror)
    }
    var scale by remember(picture) { mutableFloatStateOf(1f) }
    var shift by remember(picture) { mutableStateOf(Offset.Zero) }
    // Il colore mirato non ha una lente (risposta `via` a `d-mirino-resta`): il dito sceglie e basta.
    /** La corsa del doppio tocco, tenuta per poterla fermare appena un dito scende. */
    var ride by remember(picture) { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val wide = posed.width.toFloat() / posed.height

    /**
     * Il taglio che il palco inquadra, o `null` se si vede l'immagine intera: l''Applica' del
     * ritaglio (richiesta dell'utente), che vale anche dentro il Ritaglio, dove le squadrette
     * ripartono ai bordi della porzione (vedi [Framing]).
     * ⚠️⚠️ **Con lo strumento 'Angoli' armato non vale**: le maniglie sono gli angoli
     * dell'immagine intera, e inquadrando la porzione cadrebbero fuori dallo schermo appena il
     * taglio è piccolo. Il taglio resta, disegnato sopra (vedi [cornerOverlay]).
     */
    val framed = look.framing.shown.takeIf { corners == null }

    /**
     * Quanto è larga rispetto all'alta la porzione che si vede: l'immagine intera, o il taglio.
     *
     * ⚠️ **È questo numero a entrare nella stanza**, non l'aspetto dell'immagine: con un taglio
     * confermato è lui che deve entrare nel palco, e dove finisca l'immagine intera lo ricava
     * [spread].
     */
    val shown =
        if (framed == null) wide
        else wide * (framed.right - framed.left) / (framed.bottom - framed.top)

    /*
     * ⚠️⚠️ **Il gesto legge la posa e il taglio di adesso**, con la prudenza di [geoNow]: il corpo
     * del `pointerInput` parte al primo tocco sul palco e non riparte, quindi [shown], [framed] e
     * [posed] letti là dentro resterebbero quelli di quel tocco. Dopo 'Applica' il dito
     * cercherebbe la cornice nell'immagine intera, e dopo un quarto di giro in quella coricata. Il
     * difetto compare solo col palco già toccato prima del cambiamento.
     */
    val shownNow by rememberUpdatedState(shown)
    val framedNow by rememberUpdatedState(framed)
    val posedNow by rememberUpdatedState(posed)

    /** Il pezzo di file letto a risoluzione piena, quando c'è: vedi [SharpPiece]. */
    var sharp by remember(picture) { mutableStateOf<SharpPiece?>(null) }
    /**
     * Quale presa del Ritaglio tiene il dito, per la lente (la stessa dell'editor semplice,
     * richiesta dell'utente): lo scrive il gesto e lo legge il disegno, come `held` di
     * `CropStage`, e la lente c'è solo mentre si tira un angolo.
     * ⚠️ **Torna a [Grab.NONE] in un `finally`**: un gesto annullato non torna alla riga dopo, e
     * la lente resterebbe in scena senza un dito (vedi `heldOrTwice`).
     */
    var held by remember { mutableStateOf(Grab.NONE) }
    var brushAt by remember(picture) { mutableStateOf<Offset?>(null) }
    var brushTouching by remember(picture) { mutableStateOf(false) }
    /**
     * Dove disegna il dito, finché la forma è ancora piccola: là il palco mostra la lente (nota
     * dell'utente sulla voce `4.40-01`, R2), o `null`.
     */
    var penLoupe by remember(picture) { mutableStateOf<Offset?>(null) }
    /**
     * Dove comincia una linea o una freccia e l'asse su cui è posata, mentre il dito la disegna:
     * il palco mostra una guida lungo quell'asse, che sparisce quando il dito si alza (nota A
     * dell'utente sul giro della 4.45), o `null`.
     */
    var penGuide by remember(picture) { mutableStateOf<Pair<Offset, SnapAxis>?>(null) }
    /**
     * I bordi dell'immagine visibile a cui si appoggia l'elemento sotto il dito, mentre lo disegna
     * o lo sposta: il palco mostra una guida lungo ciascuno, fino a quando il dito si alza
     * (richiesta dell'utente).
     */
    var penEdges by remember(picture) { mutableStateOf(emptySet<ImageEdge>()) }
    /**
     * Le guide fra l'elemento sotto il dito e gli altri elementi con cui si allinea, come
     * segmenti sul palco ([Draw.lines]), fino a quando il dito si alza (richiesta dell'utente).
     */
    var penLines by remember(picture) { mutableStateOf(emptyList<Pair<Offset, Offset>>()) }
    var brushHide by remember(picture) { mutableStateOf<Job?>(null) }
    /**
     * Quanto è grande il palco, misurato dal layout.
     *
     * Serve solo alla richiesta del pezzo: il disegno la misura da sé (`size`, dentro il
     * `Canvas`), ma una richiesta vive in una coroutine, dove quel numero non si legge.
     */
    var stage by remember(picture) { mutableStateOf(Size.Zero) }

    /*
     * Il lato lungo dell'immagine sul palco, per il tasto 'Spessore' del modulo Disegno: lo stesso
     * conto del disegno ([viewport]), fatto fuori dal disegno.
     * ⚠️ **Scritto dentro il disegno, uno stato fa ridisegnare un altro disegno**: durante una
     * pinza ogni fotogramma del palco ne invalidava uno del tasto, che seguiva con un fotogramma
     * di ritardo. Il flusso legge gli stessi stati e scrive solo quando il numero cambia.
     */
    val viewLongTo by rememberUpdatedState(onViewLong)
    val drawPenNow by rememberUpdatedState(drawPen)
    val airNow by rememberUpdatedState(airPx)
    LaunchedEffect(picture) {
        snapshotFlow {
            if (drawPenNow() == null || stage.width <= 0f || stage.height <= 0f) {
                null
            } else {
                val vista = viewport(stage, shownNow, scale, shift, airNow, framedNow)
                max(vista.width(), vista.height())
            }
        }.collect { lungo -> if (lungo != null) viewLongTo(lungo) }
    }

    /**
     * Sale di uno ogni volta che la vista si ferma, ed è il via alla richiesta del pezzo.
     *
     * ⚠️⚠️ **Si chiede a gesto finito**: durante una pinza la vista passa per cento posizioni, e
     * leggere dal file a ognuna vorrebbe dire decodificare sessanta volte al secondo. Finché ci si
     * muove si vede l'anteprima ingrandita.
     * ⚠️⚠️ **Un contatore e non `scale` e `shift`**: quei due si leggono nel disegno e non in
     * composizione, ed è quello che tiene un gesto a costo zero; fra le chiavi di un effetto
     * ricomporrebbero il palco a ogni fotogramma di panoramica.
     */
    var resting by remember(picture) { mutableIntStateOf(0) }

    /*
     * ⚠️⚠️ **Col Ritaglio in scena l'immagine torna intera**: le squadrette si tirano ai bordi di
     * quello che si vede, e con l'immagine ingrandita metà di quei bordi sarebbe fuori dallo
     * schermo. Come nell'editor semplice, dove il palco del ritaglio non ingrandisce.
     */
    LaunchedEffect(cutting != null) {
        if (cutting != null) {
            scale = 1f
            shift = Offset.Zero
        }
    }

    /*
     * ⚠️ **Con lo strumento 'Angoli' armato per la stessa ragione**: le maniglie sono agli angoli
     * dell'immagine, e ingrandita sarebbero tutte fuori dallo schermo.
     */
    LaunchedEffect(corners != null) {
        if (corners != null) {
            scale = 1f
            shift = Offset.Zero
        }
    }

    /*
     * ⚠️ **Qui dentro [scale] e [shift] si leggono senza costo**: il blocco di un `LaunchedEffect`
     * gira fuori dalla composizione, e le sue letture non diventano dipendenze. Per questo le
     * chiavi sono [resting] e [stage].
     */
    LaunchedEffect(picture, full, stage, resting, look.geo.idle, look.square, look.healing, sfocature) {
        val source = full
        if (source == null || stage.width <= 0f || stage.height <= 0f) {
            sharp = null
            return@LaunchedEffect
        }
        /*
         * ⚠️⚠️ **Con la Geometria mossa il pezzo nitido non si legge**: `sharpAsk` ricava la
         * porzione dal rettangolo dell'immagine intera, e con la deformazione quel rettangolo non
         * dice più dove finisce un pixel, quindi il pezzo cadrebbe al posto sbagliato. Si perde la
         * risoluzione piena mentre si raddrizza; per riaverla il pezzo dovrebbe passare dalla maglia.
         */
        if (!look.geo.idle || !look.square) {
            sharp = null
            return@LaunchedEffect
        }
        val view = viewport(stage, shown, scale, shift, air(), framed)
        val ask = sharpAsk(
            source.width, source.height, picture.width, view, stage.width, stage.height
        )
        if (ask !is Sharpening.Read) {
            sharp = null
            return@LaunchedEffect
        }
        /*
         * Lo stesso pezzo che si ha già: un gesto può finire dove era cominciato, e rileggerlo
         * costerebbe una decodifica per niente. Le correzioni e i vetri fanno parte del confronto,
         * perché cambiano a vista ferma.
         */
        val healing = look.healing
        if (sharp?.serves(ask, healing, sfocature) == true) return@LaunchedEffect
        val tile = source.tile(ask.area, ask.sample) ?: return@LaunchedEffect
        val pezzo = RectF(
            ask.area.left.toFloat() / source.width, ask.area.top.toFloat() / source.height,
            ask.area.right.toFloat() / source.width, ask.area.bottom.toFloat() / source.height
        )
        /*
         * ⚠️ La sfocatura va anche sul pezzo, o ingrandendo si vedrebbe nitida la parte nascosta.
         * ⚠️ **Fuori dal thread principale**: questo effetto gira sul dispatcher della
         * composizione, e copia e sfocatura di un pezzo grande quanto lo schermo bloccherebbero
         * l'interfaccia.
         */
        val pixels = withContext(Dispatchers.Default) {
            Draw.blurAreas(Healing.render(tile, healing, pezzo), sfocature, mine = true, at = pezzo)
        }
        sharp = SharpPiece(pixels = pixels, ask = ask, at = pezzo, healing = healing, blurs = sfocature)
    }

    /**
     * Il pennello che disegna [mappa] **col conto già applicato** dentro il rettangolo
     * [dove], col filtro lineare o con quello a pixel interi. [dentro] dice dove finisce
     * l'immagine **intera** sullo schermo, e quanto misura.
     *
     * ⚠️ **Una funzione perché i rettangoli sono più di uno**: il palco, il pezzo letto a
     * risoluzione piena e la lente del ritaglio. Copie diverse svilupperebbero l'immagine in modi
     * diversi.
     */
    fun pennello(mappa: Bitmap, dove: RectF, nitido: Boolean, dentro: Framed): Paint {
        val image = BitmapShader(mappa, TileMode.CLAMP, TileMode.CLAMP).apply {
            setLocalMatrix(
                Matrix().apply {
                    setRectToRect(
                        RectF(0f, 0f, mappa.width.toFloat(), mappa.height.toFloat()),
                        dove,
                        Matrix.ScaleToFit.FILL
                    )
                }
            )
        }
        /*
         * ⚠️⚠️ **Il filtro lineare serve al Dettaglio**, che legge i vicini a distanze che non
         * cadono su un pixel intero: senza, ogni campione si arrotonderebbe al pixel più vicino.
         * Non basta `isFilterBitmap` del pennello, che governa il disegno e non i campioni che uno
         * shader chiede a un altro.
         * ⚠️ **Nella lente si vogliono i pixel interi**: là si guarda fra quali pixel passa il
         * bordo del taglio, e il filtro lineare li mescolerebbe.
         * ⚠️ `setFilterMode` e le sue costanti sono dell'API 33: l'editor completo si apre solo da
         * lì ([advancedEditorAvailable]), e il controllo lo dice a chi usasse il palco altrove.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            image.setFilterMode(
                if (nitido) BitmapShader.FILTER_MODE_NEAREST else BitmapShader.FILTER_MODE_LINEAR
            )
        }
        /*
         * ⚠️⚠️ **Si consegna il riquadro dell'immagine intera come è disegnata**, non quello della
         * mappa né del pezzo: il Dettaglio e i primi cursori degli Effetti ragionano in frazioni
         * del lato, vignettatura e grana chiedono anche dove cade il punto. Così ingrandire
         * ingrandisce il risultato del filtro senza cambiarlo, e un pezzo disegnato da solo ha lo
         * stesso raggio e lo stesso centro del tutto; con la misura del pezzo, il filtro
         * cambierebbe forza con la panoramica e la vignettatura seguirebbe il dito.
         * Con la geometria mossa `p` arriva comunque di qui: `drawVertices` campiona lo shader alle
         * coordinate di texture, e si spostano i soli vertici, come nel salvataggio.
         */
        val shader = if (look.plain) null else lookShader(image, look, dentro)
        return Paint().apply {
            nativePaint.isFilterBitmap = !nitido
            nativePaint.shader = shader ?: image
        }
    }

    /*
     * ⚠️ **La tela è una sola**, anche con la lente del ritaglio: mentre si tira un angolo la tela
     * del palco si ridisegna comunque a ogni fotogramma, perché le squadrette si muovono. Portando
     * squadrette e lente in una tela a parte, il palco smetterebbe di ridisegnarsi durante il
     * ritaglio.
     */
    Box(
        modifier = modifier
            // ⚠️ Ingrandita, l'immagine esce dal suo riquadro: senza, finirebbe sopra la testata e
            // la scheda dei cursori.
            .clipToBounds()
            .onSizeChanged { stage = Size(it.width.toFloat(), it.height.toFloat()) }
    ) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = hold }
            .pointerInput(picture) {
                val room = Size(size.width.toFloat(), size.height.toFloat())
                val middle = Offset(size.width / 2f, size.height / 2f)
                val span = ZOOM_PULL.toPx()

                /*
                 * La pinza, scritta una volta sola perché la raggiungono due strade: le dita che si
                 * muovono subito, e il compagno che arriva mentre il secondo tocco è ancora giù.
                 */
                fun pinch(centroid: Offset, pan: Offset, zoom: Float) {
                    val next = (scale * zoom).coerceIn(1f, ZOOM_MAX)
                    // Il punto sotto le dita resta fermo: lo spostamento si riscrive intorno al
                    // centroide.
                    val grown = next / scale
                    val from = centroid - middle
                    scale = next
                    shift = reined(from + (shift - from) * grown + pan, next, room, shownNow, air())
                }

                /**
                 * Il colore del pixel dell'immagine sotto il punto [at], o `null` se là c'è il
                 * fondo del palco invece dell'immagine.
                 *
                 * ⚠️⚠️ **Il colore è quello del file e non quello che si vede**: il risultato del
                 * conto vive sulla scheda grafica e non si rilegge. Con un'esposizione già alzata
                 * di molto, il colore preso è più scuro di quello che il dito vede.
                 * Il rettangolo è quello del disegno, dalla stessa funzione.
                 * ⚠️ **Si prende dal pezzo nitido quando c'è** (risposta `pieno` a
                 * `d-lente-pieno`): un pixel dell'anteprima è la media di due o tre pixel veri, e
                 * su un dettaglio fine cadrebbe in un'altra fascia.
                 */
                fun colourAt(at: Offset): Int? {
                    val view = viewport(room, shownNow, scale, shift, air(), framedNow)
                    val l = view.left
                    val t = view.top
                    val r = view.right
                    val b = view.bottom
                    if (at.x < l || at.x > r || at.y < t || at.y > b) return null
                    if (r - l <= 0f || b - t <= 0f) return null
                    /*
                     * ⚠️⚠️ **Il dito tocca l'immagine deformata, e il colore vive prima della
                     * deformazione**: quello che si vede sotto il dito viene da un altro punto del
                     * file. La mappatura inversa è `Warp.back`, l'inversa esatta di quella del
                     * disegno: una stima darebbe un colore vicino e sbagliato.
                     */
                    val geo = geoNow
                    val where = if (geo.idle) at else {
                        val plan = Warp.plan(
                            geo, view.centerX(), view.centerY(), view.width(), view.height()
                        )
                        val p = Warp.back(
                            plan, RectF(view.left, view.top, view.right, view.bottom),
                            geo.liquify, at.x, at.y
                        )
                        Offset(p[0], p[1])
                    }
                    val u = ((where.x - l) / (r - l)).coerceIn(0f, 1f)
                    val v = ((where.y - t) / (b - t)).coerceIn(0f, 1f)
                    sharp?.pixel(u, v)?.let { return it }
                    // ⚠️ L'anteprima di adesso, già in posa: vedi `posedNow`.
                    val mappa = posedNow
                    return mappa.getPixel(
                        (u * (mappa.width - 1)).roundToInt(),
                        (v * (mappa.height - 1)).roundToInt()
                    )
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    /*
                     * ⚠️ Un dito che scende ferma la corsa dove è arrivata: chi tocca mentre
                     * l'immagine si ingrandisce vuole prendere il comando.
                     */
                    ride?.cancel()
                    /*
                     * ⚠️⚠️ **Col colore mirato armato, o col Ritaglio in scena, il palco fa solo
                     * quello**: pinza, panoramica, doppio tocco e confronto restano fermi. In
                     * questo rilevatore ogni gesto nasce dallo stesso dito, e un gesto in più
                     * sarebbe una strada in più da distinguere in mezzo secondo.
                     * Nel Ritaglio un dito lontano da una presa non fa niente: l'alternativa
                     * farebbe saltare il rettangolo sotto il dito.
                     */
                    val taglio = cutNow
                    if (taglio != null) {
                        // ⚠️ Letta una volta per gesto: il riquadro e il rettangolo scritto alla
                        // fine devono parlare della stessa porzione.
                        val porzione = framedNow
                        val vista = viewport(room, shownNow, scale, shift, air(), porzione)
                        /*
                         * ⚠️⚠️ **Le squadrette si tirano dentro la porzione applicata**: il
                         * riquadro del gesto è il taglio confermato, e il rettangolo che si muove
                         * è in frazioni sue. Senza le due conversioni il rettangolo scapperebbe
                         * da sotto le dita. Senza niente applicato le due funzioni sono l'identità.
                         */
                        val dentro = cutout(vista, porzione)
                        val frame = Rect(dentro.left, dentro.top, dentro.right, dentro.bottom)
                        var going = cropBox(relativeTo(porzione, taglio), frame)
                        val presa = grabbed(down.position, going, GRIP.toPx(), keepNow == null)
                        if (presa != Grab.NONE) {
                            down.consume()
                            held = presa
                            try {
                                drag(down.id) { change ->
                                    /*
                                     * ⚠️⚠️ **Il delta si legge prima di consumare**:
                                     * `positionChange()` risponde zero su un evento già
                                     * consumato, e il rettangolo non si muoverebbe. Nessun
                                     * compilatore lo vede; il banco sì.
                                     */
                                    val passo = change.positionChange()
                                    change.consume()
                                    going = dragged(
                                        going,
                                        presa,
                                        passo,
                                        frame,
                                        /*
                                         * Il rapporto forzato arriva dai gettoni dei formati, ed è
                                         * `null` con 'Libero': lo stesso parametro dell'editor
                                         * semplice.
                                         */
                                        keepNow,
                                        LEAST_SIDE.toPx()
                                    )
                                    cutTo(absolute(porzione, cropFractions(going, frame)))
                                }
                            } finally {
                                held = Grab.NONE
                            }
                            onCutEnd()
                        }
                        return@awaitEachGesture
                    }
                    if (healingNow()) {
                        brushHide?.cancel()
                        brushAt = down.position
                        brushTouching = true
                        try {
                            val outcome = settled(down, viewConfiguration.touchSlop) { brushAt = it }
                            if (outcome == Settled.MULTI) {
                                brushAt = null
                                transformed(::pinch)
                                resting++
                                return@awaitEachGesture
                            }
                            var previous = down.position
                            fun paintAt(point: Offset) {
                                val view = viewport(room, shownNow, scale, shift, air(), framedNow)
                                val visible = cutout(view, framedNow)
                                if (!visible.contains(point.x, point.y) || view.width() <= 0 || view.height() <= 0) return
                                val radius = healingRadius() * max(view.width(), view.height())
                                val plan = Warp.plan(geoNow, view.centerX(), view.centerY(), view.width(), view.height())
                                val polygon = (0 until 16).map { n ->
                                    val angle = n * 2.0 * kotlin.math.PI / 16
                                    val x = point.x + radius * kotlin.math.cos(angle).toFloat()
                                    val y = point.y + radius * kotlin.math.sin(angle).toFloat()
                                    val mapped = if (geoNow.idle) floatArrayOf(x,y)
                                        else Warp.back(plan, view, geoNow.liquify, x,y)
                                    Healing.unpose(Offset((mapped[0]-view.left)/view.width(),
                                        (mapped[1]-view.top)/view.height()), spinNow)
                                }
                                healTo(polygon)
                            }
                            fun strokeTo(point: Offset) {
                                val view = viewport(room, shownNow, scale, shift, air(), framedNow)
                                val step = (healingRadius()*max(view.width(),view.height())/3f).coerceAtLeast(.25f)
                                val count = kotlin.math.ceil((point-previous).getDistance()/step).toInt().coerceIn(1,Healing.MAX_POLYGONS)
                                for (n in 1..count) paintAt(lerp(previous,point,n.toFloat()/count))
                                previous = point
                                brushAt = point
                            }
                            paintAt(down.position)
                            strokeTo(brushAt ?: down.position)
                            if (outcome == Settled.MOVED) drag(down.id) { change ->
                                strokeTo(change.position)
                                change.consume()
                            }
                        } finally {
                            brushTouching = false
                            brushHide = scope.launch { delay(1_000); brushAt = null }
                        }
                        return@awaitEachGesture
                    }
                    /*
                     * ⚠️⚠️ **Col modulo Disegno in scena un dito disegna e due ingrandiscono**, come in
                     * Fluidifica: il dito torna indietro attraverso la geometria (`Warp.back`) e la
                     * posa (`Healing.unpose`) alla cornice originale, dove vive il disegno, così una
                     * linea su un'immagine raddrizzata cade dove il dito l'ha vista.
                     * ⚠️ Un tocco disegna un punto a mano libera, e niente con gli altri strumenti:
                     * una linea o un rettangolo senza lunghezza non sono una forma.
                     */
                    val pen = penNow()
                    if (pen != null) {
                        // ⚠️ L'evento che supera la soglia lo consuma `settled`: la sua posizione si
                        // tiene qui e diventa il secondo punto, o una linea aspetterebbe un altro
                        // movimento per averne uno.
                        var oltre = down.position
                        fun toImage(at: Offset): Offset? {
                            val view = viewport(room, shownNow, scale, shift, air(), framedNow)
                            if (view.width() <= 0f || view.height() <= 0f) return null
                            val p = if (geoNow.idle) floatArrayOf(at.x, at.y) else Warp.back(
                                Warp.plan(geoNow, view.centerX(), view.centerY(), view.width(), view.height()),
                                view, geoNow.liquify, at.x, at.y
                            )
                            return Healing.unpose(
                                Offset((p[0] - view.left) / view.width(), (p[1] - view.top) / view.height()),
                                spinNow
                            )
                        }
                        // La strada inversa, dall'immagine al palco: vedi [onStage].
                        fun toStage(): (Offset) -> Offset {
                            val view = viewport(room, shownNow, scale, shift, air(), framedNow)
                            val plan = if (geoNow.idle) null
                            else Warp.plan(geoNow, view.centerX(), view.centerY(), view.width(), view.height())
                            return onStage(picture.width, picture.height, spinNow, view, plan, geoNow.liquify)
                        }
                        /*
                         * ⚠️⚠️ **Un dito fermo su un elemento lo sceglie e apre il suo menu** (nota E
                         * dell'utente sul giro della 4.60, forma dalla risposta sulla voce
                         * `4.64-03`). L'attesa è il tocco lungo di sistema, come in tutta l'app;
                         * fermo sul vuoto, il gesto continua, quindi un dito che aspetta e poi
                         * disegna disegna, e uno che si alza tocca.
                         */
                        var esito = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            settled(down, viewConfiguration.touchSlop) { oltre = it }
                        }
                        if (esito == null) {
                            val vista = viewport(room, shownNow, scale, shift, air(), framedNow)
                            val tenuto = toImage(down.position)
                                ?.takeIf { cutout(vista, framedNow).contains(down.position.x, down.position.y) }
                                ?.let { at ->
                                    Draw.hit(
                                        drawingNow, at, picture.width, picture.height,
                                        PICK_REACH.toPx() / max(vista.width(), vista.height()).coerceAtLeast(1f)
                                    )
                                }
                            if (tenuto != null) {
                                pickNow(tenuto)
                                haptics.performHapticFeedback(HOLD_BUZZ)
                                holdNow()
                                // ⚠️ L'alzata del dito appartiene a questo gesto: lasciata andare,
                                // arriverebbe al palco come un tocco e toglierebbe la scelta su cui
                                // il menu agisce.
                                do {
                                    val evento = awaitPointerEvent()
                                    evento.changes.forEach { it.consume() }
                                } while (evento.changes.any { it.pressed })
                                return@awaitEachGesture
                            }
                            esito = settled(down, viewConfiguration.touchSlop) { oltre = it }
                        }
                        if (esito == Settled.MULTI) {
                            transformed(::pinch)
                            resting++
                            return@awaitEachGesture
                        }
                        val visible = cutout(
                            viewport(room, shownNow, scale, shift, air(), framedNow), framedNow
                        )
                        if (!visible.contains(down.position.x, down.position.y)) return@awaitEachGesture
                        val start = toImage(down.position) ?: return@awaitEachGesture
                        val base = drawingNow
                        /*
                         * ⚠️⚠️ **Un tocco sceglie un elemento, e un trascinamento da quello scelto lo
                         * sposta** (specifica dell'utente). Un tocco sul vuoto toglie la scelta, e con
                         * una scelta non lascia un punto: un tocco che voleva dire 'basta'
                         * disegnerebbe. Un trascinamento che parte altrove toglie la scelta e
                         * disegna.
                         * ⚠️ La portata si misura sullo schermo, [PICK_REACH] oltre la metà della
                         * linea, e si converte nel lato lungo dell'immagine: una linea sottile è
                         * difficile da prendere.
                         */
                        val scelto = pickedNow()
                        val vista = viewport(room, shownNow, scale, shift, air(), framedNow)
                        val reach = PICK_REACH.toPx() / max(vista.width(), vista.height()).coerceAtLeast(1f)
                        /*
                         * ⚠️⚠️ **Un elemento vicino a un bordo dell'immagine visibile vi si appoggia**
                         * (richiesta dell'utente: uno scatto calamitato per tenere una linea a filo
                         * del bordo): il contorno, tratto e punta compresi, va sul bordo entro
                         * [EDGE_REACH], da dentro o da fuori, e il palco mostra la guida. Più lontano
                         * supera il bordo, e se ne vede solo la parte dentro. Si misura sullo schermo,
                         * dove è il bordo che si vede ([Draw.rest]).
                         */
                        val cornice = Rect(visible.left, visible.top, visible.right, visible.bottom)
                        val lungo = max(vista.width(), vista.height())
                        val portata = EDGE_REACH.toPx()
                        /*
                         * ⚠️⚠️ **Un trascinamento da una maniglia dell'elemento scelto lo deforma o lo
                         * ruota** (risposta dell'utente sulla voce `4.64-04`). La maniglia più vicina
                         * al dito entro [HANDLE_REACH] vince sul corpo dell'elemento, che lo sposta.
                         * - 'Trasforma': la maniglia segue il dito alla distanza da cui è stata presa,
                         *   senza saltare sotto il dito. L'estremo di una linea o di una freccia si
                         *   aggancia all'orizzontale e alla verticale rispetto all'altro, come quando è
                         *   stata disegnata, con la guida.
                         * - 'Ruota': l'elemento gira intorno al centro dell'angolo che il dito
                         *   descrive, misurato sull'immagine originale ([Mark.turned]), così anche
                         *   un'immagine specchiata gira nel verso del dito.
                         * ⚠️ Ogni fotogramma riparte dall'elemento del momento in cui il dito è sceso.
                         */
                        val tenendo = scelto?.let { base.marks.getOrNull(it) }
                        if (tenendo != null && esito == Settled.MOVED) {
                            val w = picture.width
                            val h = picture.height
                            val sulPalco = toStage()
                            val maniglie = tenendo.handles(w, h).map(sulPalco)
                            val presa = maniglie.indices
                                .minByOrNull { (maniglie[it] - down.position).getDistance() }
                                ?.takeIf { (maniglie[it] - down.position).getDistance() <= HANDLE_REACH.toPx() }
                            if (presa != null) {
                                val giro = turningNow()
                                val scarto = maniglie[presa] - down.position
                                /*
                                 * ⚠️⚠️ **In 'Trasforma' il lato tirato si appoggia ai bordi e agli
                                 * altri elementi** (richiesta dell'utente), come quando l'elemento si
                                 * disegna o si sposta. Quali lati tiri una maniglia si legge sullo
                                 * schermo, da dove è nel riquadro dell'elemento, così un'immagine
                                 * specchiata o girata tira i lati che mostra. Un rettangolo o
                                 * un'ellisse ruotati non si appoggiano: il riquadro non segue la
                                 * maniglia.
                                 */
                                val altri = base.marks.filterIndexed { i, _ -> i != scelto }
                                    .map { stageBox(it, sulPalco, lungo, w, h) }
                                val prima = stageBox(tenendo, sulPalco, lungo, w, h)
                                val tirati = buildSet {
                                    val m = maniglie[presa]
                                    if (abs(m.x - prima.center.x) > prima.width / 4f) {
                                        add(if (m.x < prima.center.x) ImageEdge.LEFT else ImageEdge.RIGHT)
                                    }
                                    if (abs(m.y - prima.center.y) > prima.height / 4f) {
                                        add(if (m.y < prima.center.y) ImageEdge.TOP else ImageEdge.BOTTOM)
                                    }
                                }
                                // ⚠️ Un testo cresce intorno al centro, quindi nessun suo lato segue
                                // la maniglia da solo.
                                val appoggia = !(tenendo.pen.boxed && tenendo.angle != 0f) && tenendo.pen != Pen.TEXT
                                val perno = tenendo.centre(w, h)
                                fun verso(p: Offset): Float = Math.toDegrees(
                                    kotlin.math.atan2(((p.y - perno.y) * h).toDouble(), ((p.x - perno.x) * w).toDouble())
                                ).toFloat()
                                fun tira(at: Offset) {
                                    val nuovo = if (giro) {
                                        val dove = toImage(at) ?: return
                                        tenendo.turned(verso(dove) - verso(start), w, h)
                                    } else {
                                        var qui = at + scarto
                                        if (tenendo.pen == Pen.LINE || tenendo.pen == Pen.ARROW) {
                                            val altro = maniglie[1 - presa]
                                            val (d, asse) = Draw.snap(altro, qui)
                                            penGuide = asse?.let { altro to it }
                                            val tratto = tenendo.width * lungo
                                            val (fine, _) = Draw.restEnd(
                                                tenendo.pen, altro, d, tratto, lungo, cornice, portata,
                                                asse?.let { setOf(it) } ?: SnapAxis.entries.toSet(), altri
                                            )
                                            qui = fine
                                            val box = Draw.extent(tenendo.pen, altro, fine, tratto, lungo)
                                            penEdges = Draw.on(box, cornice)
                                            penLines = Draw.lines(box, altri)
                                        } else if (appoggia) {
                                            val prova = tenendo.reshaped(presa, toImage(qui) ?: return, w, h)
                                            val box = stageBox(prova, sulPalco, lungo, w, h)
                                            val (d, _) = Draw.rest(box, cornice, portata, tirati, altri)
                                            qui += d
                                            penEdges = Draw.on(box.translate(d), cornice)
                                            penLines = Draw.lines(box.translate(d), altri)
                                        }
                                        tenendo.reshaped(presa, toImage(qui) ?: return, w, h)
                                    }
                                    drawTo(base.replacing(scelto, nuovo))
                                }
                                try {
                                    tira(oltre)
                                    drag(down.id) { change ->
                                        tira(change.position)
                                        change.consume()
                                    }
                                } finally {
                                    penGuide = null
                                    penEdges = emptySet()
                                    penLines = emptyList()
                                }
                                drawEnd()
                                return@awaitEachGesture
                            }
                        }
                        if (esito == Settled.UP) {
                            val preso = Draw.hit(base, start, picture.width, picture.height, reach)
                            /*
                             * ⚠️⚠️ **Un doppio tocco su testo, pastiglia o pannello chiede le parole**,
                             * come il tasto 'Testo' (nota dell'utente sul giro della 4.92). Il primo
                             * tocco fa subito quello che fa un tocco (sceglie l'elemento, o ne cambia
                             * la modalità); il secondo, sullo stesso elemento entro il tempo del
                             * doppio tocco di sistema, annulla il cambio e apre la finestra. Così un
                             * tocco non è mai più lento, e un trascinamento subito dopo resta un
                             * trascinamento: si ricorda solo l'ultimo tocco ([MarkTap]).
                             */
                            val su = currentEvent.changes.firstOrNull()?.uptimeMillis ?: down.uptimeMillis
                            if (preso != null && preso == scelto && base.marks[preso].pen.written &&
                                lastTap.index == preso && down.uptimeMillis - lastTap.up <= viewConfiguration.doubleTapTimeoutMillis
                            ) {
                                if (lastTap.turned) turnNow()
                                lastTap.index = null
                                wordsNow(preso)
                                return@awaitEachGesture
                            }
                            /*
                             * ⚠️ **Un tocco sull'elemento scelto scambia 'Trasforma' e 'Ruota'** (nota
                             * dell'utente sulla voce `4.70-04`). Il tasto nel menu resta, e dice la
                             * modalità a cui porta il tocco.
                             */
                            if (preso != null && preso == scelto) {
                                turnNow()
                                lastTap.set(preso, su, turned = true)
                                return@awaitEachGesture
                            }
                            if (preso != null || scelto != null) {
                                pickNow(preso)
                                lastTap.set(preso, su, turned = false)
                                return@awaitEachGesture
                            }
                        } else if (scelto != null) {
                            val segno = base.marks.getOrNull(scelto)
                            if (segno != null && Draw.hit(Drawing(listOf(segno)), start, picture.width, picture.height, reach) == 0) {
                                // ⚠️ L'elemento che si sposta si appoggia anche agli altri.
                                val sulPalco = toStage()
                                val altri = base.marks.filterIndexed { i, _ -> i != scelto }
                                    .map { stageBox(it, sulPalco, lungo, picture.width, picture.height) }
                                fun sposta(at: Offset) {
                                    val dove = toImage(at) ?: return
                                    val prova = segno.moved(dove - start)
                                    val box = stageBox(prova, toStage(), lungo, picture.width, picture.height)
                                    // ⚠️ Il suo centro si appoggia anche al centro dell'immagine (nota A).
                                    val (d, _) = Draw.rest(box, cornice, portata, others = altri, centred = true)
                                    val finale = if (d == Offset.Zero) prova
                                    else toImage(at + d)?.let { segno.moved(it - start) } ?: prova
                                    penEdges = Draw.on(box.translate(d), cornice)
                                    penLines = Draw.lines(box.translate(d), altri, cornice)
                                    drawTo(base.replacing(scelto, finale))
                                }
                                try {
                                    sposta(oltre)
                                    drag(down.id) { change ->
                                        sposta(change.position)
                                        change.consume()
                                    }
                                } finally {
                                    penEdges = emptySet()
                                    penLines = emptyList()
                                }
                                drawEnd()
                                return@awaitEachGesture
                            }
                            pickNow(null)
                        }
                        /*
                         * ⚠️⚠️ **Lo strumento Testo scrive dove il dito tocca**: un tocco sul vuoto
                         * chiede le parole, che nascono centrate su quel punto. Un trascinamento non
                         * fa niente: un testo non si disegna, si scrive.
                         * ⚠️ Pastiglia e Pannello fanno tutte e due le cose: un tocco chiede le parole
                         * e posa l'elemento intorno, un trascinamento lo disegna come un rettangolo e
                         * poi chiede.
                         */
                        if (pen.pen.written && esito == Settled.UP) {
                            textNow(start)
                            return@awaitEachGesture
                        }
                        if (pen.pen == Pen.TEXT) return@awaitEachGesture
                        // ⚠️ Gli strumenti a due punti posano anche l'inizio su un bordo vicino; la
                        // mano libera segue il dito.
                        val tratto = pen.width * lungo
                        // ⚠️ Inizio e fine si appoggiano anche agli altri elementi.
                        val altri = toStage().let { sulPalco ->
                            base.marks.map { stageBox(it, sulPalco, lungo, picture.width, picture.height) }
                        }
                        val inizio = if (pen.pen == Pen.FREE) down.position else down.position + Draw.rest(
                            Draw.extent(pen.pen, down.position, down.position, tratto, lungo), cornice, portata,
                            others = altri
                        ).first
                        var mark = pen.copy(points = listOf(toImage(inizio) ?: start))
                        val draws = pen.pen == Pen.FREE || esito == Settled.MOVED
                        /*
                         * ⚠️ Una linea o una freccia vicine all'orizzontale o alla verticale vi si
                         * posano, sullo schermo, e la guida dice quale (nota A dell'utente sul giro
                         * della 4.45). La lente segue il dito, non l'estremo agganciato.
                         * ⚠️ Poi l'estremo si appoggia ai bordi vicini lungo gli assi su cui è
                         * libero: una linea posata sull'orizzontale tiene la sua altezza.
                         */
                        fun aimed(at: Offset): Offset {
                            if (pen.pen == Pen.FREE) return at
                            var dove = at
                            var assi = SnapAxis.entries.toSet()
                            if (pen.pen == Pen.LINE || pen.pen == Pen.ARROW) {
                                val (d, asse) = Draw.snap(inizio, at)
                                penGuide = asse?.let { inizio to it }
                                dove = d
                                if (asse != null) assi = setOf(asse)
                            }
                            val (fine, _) = Draw.restEnd(pen.pen, inizio, dove, tratto, lungo, cornice, portata, assi, altri)
                            val box = Draw.extent(pen.pen, inizio, fine, tratto, lungo)
                            penEdges = Draw.on(box, cornice)
                            penLines = Draw.lines(box, altri)
                            return fine
                        }
                        if (esito == Settled.MOVED) toImage(aimed(oltre))?.let { mark = mark.reaching(it) }
                        if (draws) drawTo(base.with(mark))
                        /*
                         * ⚠️ La lente segue il dito finché la forma è piccola, misurata sullo
                         * schermo: il riquadro del tratto a mano libera fin qui, o la distanza da A
                         * al dito per gli altri strumenti. Una forma che supera la soglia perde la
                         * lente, e una a due punti che torna piccola la riprende.
                         */
                        var box = Rect(down.position, oltre)
                        fun loupeAt(at: Offset) {
                            box = if (pen.pen == Pen.FREE) Rect(
                                min(box.left, at.x), min(box.top, at.y),
                                max(box.right, at.x), max(box.bottom, at.y)
                            ) else Rect(down.position, at)
                            val span = max(abs(box.width), abs(box.height))
                            penLoupe = at.takeIf { span < brushCmPx * PEN_LOUPE_CM }
                        }
                        try {
                            if (esito == Settled.MOVED) {
                                loupeAt(oltre)
                                drag(down.id) { change ->
                                    loupeAt(change.position)
                                    toImage(aimed(change.position))?.let { at ->
                                        if (mark.pen != Pen.FREE || Draw.far(mark.points.last(), at)) {
                                            mark = mark.reaching(at)
                                            drawTo(base.with(mark))
                                        }
                                    }
                                    change.consume()
                                }
                            }
                        } finally {
                            penLoupe = null
                            penGuide = null
                            penEdges = emptySet()
                            penLines = emptyList()
                        }
                        if (draws) drawEnd()
                        if (draws && pen.pen.framed) pillNow(base.marks.size)
                        return@awaitEachGesture
                    }
                    if (liquifying()) {
                        brushHide?.cancel()
                        brushAt = down.position
                        brushTouching = true
                        try {
                            val esito = settled(down, viewConfiguration.touchSlop) { brushAt = it }
                            if (esito == Settled.MULTI) {
                                brushAt = null
                                transformed(::pinch)
                                resting++
                                return@awaitEachGesture
                            }
                            if (esito == Settled.MOVED) {
                                var mesh = liquifyNow
                                var prima = down.position
                                drag(down.id) { change ->
                                    val vista = viewport(room, shownNow, scale, shift, air(), framedNow)
                                    val ora = change.position
                                    val passo = ora - prima
                                    prima = ora
                                    brushAt = ora
                                    if (vista.width() > 0f && vista.height() > 0f) {
                                        mesh = mesh.stroke(
                                            x = (ora.x - vista.left) / vista.width(),
                                            y = (ora.y - vista.top) / vista.height(),
                                            moveX = passo.x / vista.width(),
                                            moveY = passo.y / vista.height(),
                                            radius = brushRadius(),
                                            strength = brushStrength(),
                                            rebuild = rebuild(),
                                            aspect = vista.width() / vista.height(),
                                            geometry = geoNow
                                        )
                                        liquifyTo(mesh)
                                    }
                                    change.consume()
                                }
                                onLiquifyEnd()
                            }
                        } finally {
                            brushTouching = false
                            brushHide = scope.launch {
                                delay(1_000)
                                brushAt = null
                            }
                        }
                        return@awaitEachGesture
                    }
                    /*
                     * ⚠️ **Lo strumento 'Angoli' tira un vertice per volta** (richiesta
                     * dell'utente): il gesto delle squadrette del ritaglio, ma si muove un vertice
                     * dell'immagine, e la presa è un tondo. Un dito lontano da una maniglia non fa
                     * niente, come nel Ritaglio, o l'angolo salterebbe sotto il dito.
                     */
                    val angoli = cornerNow
                    if (angoli != null) {
                        val vista = viewport(room, shownNow, scale, shift, air(), framedNow)
                        val piano = Warp.plan(
                            geoNow, vista.centerX(), vista.centerY(),
                            vista.width(), vista.height(), hold = ARMED_FIT
                        )
                        val presa = nearestCorner(
                            down.position, cornerSpots(piano, vista), GRIP.toPx()
                        )
                        /*
                         * Quanti pixel vale un'unità isotropa: la mappa posa un punto a
                         * `centro + n * cover * half`, quindi quel prodotto converte il passo del
                         * dito nello scarto dell'angolo. Con la scala di lavoro ferma (vedi
                         * [ARMED_FIT]) la maniglia va dove va il dito.
                         */
                        val unita = piano.half * piano.cover
                        if (presa >= 0 && unita > 0f) {
                            down.consume()
                            var ora: Corners = angoli
                            drag(down.id) { change ->
                                val passo = change.positionChange()
                                change.consume()
                                val dx = (ora.dx(presa) + passo.x / unita)
                                    .coerceIn(-Warp.PULL, Warp.PULL)
                                val dy = (ora.dy(presa) + passo.y / unita)
                                    .coerceIn(-Warp.PULL, Warp.PULL)
                                val prova = ora.with(presa, dx, dy)
                                /*
                                 * ⚠️⚠️ **L'angolo si ferma dove il quadrilatero si rovescerebbe**: il
                                 * valore non si scrive, o scartandolo dopo l'angolo scatterebbe
                                 * indietro. Il perché del confine vive su [Warp.convex].
                                 */
                                if (Warp.convex(prova, piano.ax, piano.ay)) {
                                    ora = prova
                                    cornerTo(prova)
                                }
                            }
                            onCornersEnd()
                        }
                        return@awaitEachGesture
                    }
                    if (aiming()) {
                        /*
                         * ⚠️ **Il dito si trascina e la scelta arriva quando si alza** (istruzione
                         * dell'utente, voce `mirato-lente`): con il dito il punto giusto si prende
                         * di rado al primo colpo. Conta dove il dito ha finito. Il gesto non disegna
                         * niente (risposta `via` a `d-mirino-resta`).
                         */
                        var preso = colourAt(down.position)
                        var dove = down.position
                        while (true) {
                            val punto = awaitPointerEvent().changes
                                .firstOrNull { it.id == down.id } ?: break
                            if (!punto.pressed) break
                            val ora = punto.position
                            if ((ora - dove).getDistance() > viewConfiguration.touchSlop) {
                                dove = ora
                                preso = colourAt(ora)
                                punto.consume()
                            }
                        }
                        preso?.let { onAimStart(it) }
                        onAimEnd()
                        return@awaitEachGesture
                    }
                    /*
                     * Fase 1: chi vince fra il tempo, il movimento e il secondo dito. Il tempo si
                     * misura qui, perché un dito fermo non genera eventi: il tocco lungo lo vede
                     * solo un timeout.
                     */
                    val esito = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        settled(down, viewConfiguration.touchSlop)
                    }
                    when (esito) {
                        null -> {
                            onCompare(true)
                            // Lo spegnimento in un `finally` (vedi `heldOrTwice`): un'attesa
                            // annullata non torna alla riga dopo, e resterebbe l'originale.
                            try {
                                waitForUpOrCancellation()
                            } finally {
                                onCompare(false)
                            }
                        }
                        Settled.UP -> {
                            // Un tocco secco: forse è il primo di due. Il secondo alterna fra
                            // l'immagine adattata e quella ingrandita sul punto toccato.
                            val again = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
                                awaitFirstDown(requireUnconsumed = false)
                            }
                            if (again != null) {
                                again.consume()
                                val fromScale = scale
                                val fromShift = shift
                                val anchor = again.position - middle
                                /*
                                 * ⚠️⚠️ **Il secondo tocco è due gesti** (voce `zoom-corsa`): quale
                                 * dei due lo dice quello che il dito fa dopo, se si alza o
                                 * trascina, e per saperlo si aspetta. Quindi la corsa del doppio
                                 * tocco parte quando il dito si alza, il solo prezzo del gesto.
                                 */
                                when (settled(again, viewConfiguration.touchSlop)) {
                                    Settled.MOVED -> hauled(again) { dy ->
                                        /*
                                         * ⚠️ **Il conto riparte sempre dallo stato d'inizio del
                                         * gesto**: i limiti della panoramica troncano, e uno
                                         * spostamento accumulato sul valore troncato deriverebbe
                                         * mentre il dito va avanti e indietro.
                                         */
                                        val next = pulled(fromScale, dy, span)
                                        scale = next
                                        shift = reined(
                                            anchor + (fromShift - anchor) * (next / fromScale),
                                            next, room, shownNow, air()
                                        )
                                    }
                                    // Un compagno arrivato mentre il secondo tocco è ancora giù:
                                    // chi apre due dita vuole la pinza, non un doppio tocco.
                                    Settled.MULTI -> transformed(::pinch)
                                    Settled.UP -> {
                                        // Un doppio tocco secco: alterna fra l'immagine adattata e
                                        // quella ingrandita sul punto toccato.
                                        val big = fromScale <= 1f
                                        val toScale = if (big) ZOOM_TAP else 1f
                                        val toShift =
                                            if (!big) Offset.Zero
                                            else reined(anchor * (1f - ZOOM_TAP), ZOOM_TAP, room, shownNow, air())
                                        /*
                                         * Ci si arriva con un'animazione (nota dell'utente sulla
                                         * voce `luce-zoom`). ⚠️ **Si muove un progresso solo**, da
                                         * cui si ricavano ingrandimento e spostamento: due corse
                                         * separate potrebbero finire in tempi diversi e far
                                         * scivolare l'immagine a ingrandimento fermo.
                                         */
                                        ride = scope.launch {
                                            animate(
                                                initialValue = 0f,
                                                targetValue = 1f,
                                                animationSpec = tween(ZOOM_RIDE, easing = FastOutSlowInEasing)
                                            ) { t, _ ->
                                                scale = fromScale + (toScale - fromScale) * t
                                                shift = lerp(fromShift, toShift, t)
                                            }
                                            // ⚠️ La corsa finisce dopo il gesto, quindi il via al
                                            // pezzo nitido lo dà lei; annullata, lo dà il gesto
                                            // che la ferma.
                                            resting++
                                        }
                                    }
                                }
                            }
                        }
                        else -> transformed(::pinch)
                    }
                    // ⚠️ Il via al pezzo nitido è qui e non nei rami: a gesto finito la vista è
                    // ferma, e un gesto che non l'ha mossa costa una richiesta già soddisfatta
                    // (`SharpPiece.serves`). Con la corsa del doppio tocco partita, il via lo dà lei.
                    if (ride?.isActive != true) resting++
                }
            }
    ) {
        val room = size
        if (room.width <= 0f || room.height <= 0f) return@Canvas
        // Il rettangolo da disegnare: quello adattato, ingrandito attorno al centro del palco e
        // poi spostato. ⚠️ Si scala il rettangolo e non la tela: il pennello ha uno shader con la
        // sua matrice, e una tela scalata ingrandirebbe il conto invece dell'immagine.
        val view = viewport(room, shown, scale, shift, air(), framed)

        /** Dove finisce l'immagine intera sullo schermo adesso, e quanto misura. */
        val dentro = Framed.shown(view.left, view.top, view.width(), view.height())

        /*
         * ⚠️⚠️ **Con la geometria mossa si disegna una maglia**, con lo stesso pennello: cambia dove
         * finisce ogni pixel. La maglia la costruisce `Warp`, lo stesso conto del salvataggio. A
         * geometria ferma resta il rettangolo, senza le cuciture possibili fra mille triangoli.
         * Con lo strumento 'Angoli' armato si disegna la vista di lavoro, l'immagine intera
         * rimpicciolita (vedi `hold` di [Warp.plan]), e il riquadro che resterà si disegna sopra.
         */
        val armato = corners != null
        /*
         * L'immagine, il disegno e la selezione passano tutti di qui, così la scelta fra rettangolo
         * e maglia, e la vista di 'Angoli', sono una sola per i tre.
         */
        fun maglia(paint: android.graphics.Paint) {
            if (look.geo.idle && !armato) {
                drawIntoCanvas { tela -> tela.nativeCanvas.drawRect(view, paint) }
                return
            }
            /*
             * ⚠️⚠️ **La maglia si taglia al riquadro dell'immagine** (voce `geo-dritto`): ingrandita
             * per coprire, uscirebbe sul fondo del palco, mentre il salvataggio la disegna in un
             * bitmap grande quanto l'originale. `clipToBounds` ferma il disegno al palco, che è
             * più grande, e da solo non basta.
             */
            val piano = Warp.plan(
                look.geo, view.centerX(), view.centerY(), view.width(), view.height(),
                hold = if (armato) ARMED_FIT else null
            )
            clipRect(view.left, view.top, view.right, view.bottom) {
                drawIntoCanvas { tela ->
                    Warp.draw(tela.nativeCanvas, view, piano, paint, look.geo.liquify)
                }
            }
        }
        fun stendi(mappa: Bitmap, nitido: Boolean) {
            maglia(pennello(mappa, view, nitido, dentro).nativePaint)
        }

        /*
         * ⚠️⚠️ **Col taglio confermato si disegna l'immagine intera e se ne vede una porzione**:
         * [view] resta il riquadro dell'immagine intera, su cui si reggono gli altri conti del
         * palco, e il taglio lo fa il confine. Una copia ritagliata sarebbe un secondo bitmap a ogni
         * fotogramma. Senza taglio `cutout` con `null` risponde quello che riceve.
         */
        val visto = cutout(view, framed)
        clipRect(visto.left, visto.top, visto.right, visto.bottom) {
            stendi(posed, false)
        }

        /*
         * ⚠️⚠️ **Il pezzo nitido si disegna sopra l'anteprima, non al suo posto**: sotto c'è sempre
         * l'immagine intera, e finché il pezzo non arriva non manca niente.
         * Il pezzo è ancorato all'immagine e non allo schermo: si tengono le frazioni che copre, e
         * il rettangolo si ricalcola a ogni fotogramma, così resta incollato mentre il dito muove.
         */
        val fine = sharp
        if (fine != null) {
            val dove = fine.place(view)
            drawIntoCanvas { tela ->
                tela.drawRect(
                    dove.left, dove.top, dove.right, dove.bottom,
                    pennello(fine.pixels, dove, false, dentro)
                )
            }
        }

        /*
         * ⚠️⚠️ **Il disegno va sopra l'immagine sviluppata e sotto la selezione, con la stessa
         * maglia**: nel file salvato si posa dopo lo sviluppo e prima della geometria, quindi qui si
         * deforma come l'immagine (con la vista di 'Angoli' quando è armato) e si taglia alla
         * stessa cornice visibile.
         */
        fun DrawScope.paintDrawing() {
            val disegno = drawingMask ?: return
            val shader = BitmapShader(disegno, TileMode.CLAMP, TileMode.CLAMP)
            shader.setLocalMatrix(Matrix().apply {
                setScale(view.width() / disegno.width, view.height() / disegno.height)
                postTranslate(view.left, view.top)
            })
            val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
                this.shader = shader
            }
            clipRect(visto.left, visto.top, visto.right, visto.bottom) { maglia(paint) }
        }
        paintDrawing()

        fun DrawScope.paintSelection() {
            val mask = selectionMask ?: return
            val shader = BitmapShader(mask, TileMode.CLAMP, TileMode.CLAMP)
            shader.setLocalMatrix(Matrix().apply {
                setScale(view.width()/mask.width, view.height()/mask.height)
                postTranslate(view.left,view.top)
            })
            val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
                this.shader = shader; alpha = 128
            }
            clipRect(visto.left, visto.top, visto.right, visto.bottom) { maglia(paint) }
        }
        paintSelection()

        fun DrawScope.pictureForLens() {
            clipRect(visto.left, visto.top, visto.right, visto.bottom) {
                stendi(posed, true)
            }
            if (fine != null) {
                val dove = fine.place(view)
                drawIntoCanvas { tela ->
                    tela.drawRect(
                        dove.left, dove.top, dove.right, dove.bottom,
                        pennello(fine.pixels, dove, true, dentro)
                    )
                }
            }
            // ⚠️ L'inchiostro è anche nella lente, che si apre mentre si disegna (R2 del giro della
            // 4.40): senza, mostrerebbe tutto tranne il tratto sotto il dito.
            paintDrawing()
            paintSelection()
        }

        /*
         * ⚠️⚠️ **Le squadrette del ritaglio sono quelle dell'editor semplice**: velo, terzi, angoli
         * e misure vivono in `EditorScreen.kt` (`cropOverlay`), e questo palco le chiama.
         * Il riquadro è quello dell'immagine, non del palco: sul fondo intorno non c'è niente da
         * ritagliare.
         */
        val taglio = cutting
        if (taglio != null) {
            /*
             * ⚠️ **Il riquadro è quello della porzione applicata**, lo stesso del gesto: con due
             * conti diversi le squadrette si vedrebbero dove il dito non le prende.
             */
            val frame = Rect(visto.left, visto.top, visto.right, visto.bottom)
            val r = cropBox(relativeTo(framed, taglio), frame)
            cropOverlay(
                frame,
                r,
                HANDLE_ARM.toPx(),
                HANDLE_THICK.toPx(),
                GRIP_HALO.toPx(),
                keep == null
            )
            /*
             * ⚠️⚠️ **La lente del ritaglio è quella dell'editor semplice** ([lens], in
             * `EditorScreen.kt`): qui cambia soltanto che cosa si vede dentro, l'immagine
             * sviluppata col suo pennello, o mostrerebbe un'altra immagine da quella che si taglia.
             * ⚠️ **Il pennello è a pixel interi**: il bordo del taglio deve cadere visibilmente fra
             * due quadretti. Il prezzo: Dettaglio e Foschia leggono i vicini, e nella lente si
             * vedono appena diversi dal palco. Vive nella tela del palco (vedi la nota sulla tela).
             */
            eyeOf(held, r)?.let { eye ->
                lens(
                    eye, r, LOUPE_SIDE.toPx(), LOUPE_EDGE.toPx(), HANDLE_THICK.toPx(),
                    LENS_EDGE.toPx(), Color.White
                ) {
                    pictureForLens()
                }
            }
        }

        if (leveling()) levelThirds(visto, room, brushAccent)

        if (liquifying() || healing()) {
            val radius = (if (healing()) healingRadius() else brushRadius()) * max(view.width(), view.height())
            val touching = brushAt
            val visibleLeft = max(0f, visto.left)
            val visibleTop = max(0f, visto.top)
            val visibleRight = min(room.width, visto.right)
            val visibleBottom = min(room.height, visto.bottom)
            val preview = if ((if (healing()) healingSizing() else brushSizing()) && visibleRight > visibleLeft && visibleBottom > visibleTop) {
                val margin = 12.dp.toPx()
                Offset(
                    if (visibleRight - visibleLeft >= 2f * (radius + margin)) {
                        visibleRight - radius - margin
                    } else (visibleLeft + visibleRight) / 2f,
                    if (visibleBottom - visibleTop >= 2f * (radius + margin)) {
                        visibleBottom - radius - margin
                    } else (visibleTop + visibleBottom) / 2f
                )
            } else null
            (preview ?: touching)?.let { centre ->
                clipRect(visto.left, visto.top, visto.right, visto.bottom) {
                    drawCircle(
                        if (preview != null) brushAccent else Color.Black.copy(alpha = 0.6f),
                        radius, centre,
                        // Mentre si regola la misura il tratto è d'accento; al contatto resta sottile, in pixel fisici.
                        style = Stroke(width = if (preview != null) 6.dp.toPx() else 2f)
                    )
                }
            }
            // ⚠️ La lente c'è solo per un pennello piccolo (voci `3.26-06` e `3.37-08`).
if (brushTouching && touching != null && radius * 2f < brushCmPx * 0.58f) {
                val loupeSide = LOUPE_SIDE.toPx()
                /*
                 * ⚠️ **Lo zoom scende se il pennello ingrandito uscirebbe dalla lente** (voce
                 * `3.24-02`): il diametro in lente, `2 * radius * zoom`, non supera quello della
                 * lente.
                 */
                val fitZoom = (loupeSide / (2f * radius.coerceAtLeast(1f))).coerceAtMost(LOUPE_ZOOM)
                lens(
                    touching, null, loupeSide, LOUPE_EDGE.toPx(),
                    HANDLE_THICK.toPx(), LENS_EDGE.toPx(), brushAccent,
                    farthestCorner = true,
                    zoom = fitZoom.coerceAtLeast(1f)
                ) {
                    pictureForLens()
                }
            }
        }

        /*
         * I due aiuti del modulo Disegno (note dell'utente sulla voce `4.40-01`).
         * ⚠️ R1: mentre un dito tiene 'Spessore', una lineetta curva alla larghezza vera
         * nell'inchiostro del contorno, in basso a destra dell'immagine visibile, nello stesso
         * angolo del pennello di Correggi e Fluidifica (nota D dell'utente: una lineetta rende
         * meglio l'idea di un punto). La larghezza vera è la frazione del lato lungo applicata al
         * lato lungo dell'immagine sullo schermo, lo stesso numero del tasto Spessore ([onViewLong]).
         * ⚠️ R2: la lente di Correggi mentre il dito disegna una forma piccola, col suo zoom.
         */
        val penura = drawPen()
        if (penura != null) {
            if (inkSizing() && visto.width() > 0f && visto.height() > 0f) {
                val tratto = (penura.width * max(view.width(), view.height())).coerceAtLeast(1f)
                val largo = max(SAMPLE_LONG.toPx(), tratto * 4f)
                val alto = largo / 4f
                val margin = 12.dp.toPx() + tratto / 2f
                val left = max(0f, visto.left)
                val top = max(0f, visto.top)
                val right = min(room.width, visto.right)
                val bottom = min(room.height, visto.bottom)
                val x1 = if (right - left >= largo + 2f * margin) right - margin else (left + right + largo) / 2f
                val y1 = if (bottom - top >= alto + 2f * margin) bottom - margin else (top + bottom + alto) / 2f
                val x0 = x1 - largo
                val y0 = y1 - alto
                // ⚠️ Una S leggera dal basso a sinistra all'alto a destra: si legge come un tratto a mano.
                val lineetta = Path().apply {
                    moveTo(x0, y1)
                    cubicTo(x0 + largo * 0.45f, y1, x0 + largo * 0.55f, y0, x1, y0)
                }
                clipRect(visto.left, visto.top, visto.right, visto.bottom) {
                    drawPath(lineetta, Color(penura.ink), style = Stroke(tratto, cap = StrokeCap.Round))
                }
            }
            // ⚠️ La guida attraversa tutta l'immagine visibile, sottile e d'accento, sotto la
            // lente: dice dove è andata la linea, non dov'è il dito.
            penGuide?.let { (da, asse) ->
                clipRect(visto.left, visto.top, visto.right, visto.bottom) {
                    if (asse == SnapAxis.HORIZONTAL) drawLine(
                        brushAccent, Offset(visto.left, da.y), Offset(visto.right, da.y), GUIDE_LINE.toPx()
                    ) else drawLine(
                        brushAccent, Offset(da.x, visto.top), Offset(da.x, visto.bottom), GUIDE_LINE.toPx()
                    )
                }
            }
            // ⚠️ I bordi a cui l'elemento si appoggia: la stessa guida, lungo ogni bordo e
            // rientrata di metà spessore, così si vede intera.
            if (penEdges.isNotEmpty()) {
                val g = GUIDE_LINE.toPx()
                for (bordo in penEdges) {
                    val (da, a) = when (bordo) {
                        ImageEdge.LEFT -> Offset(visto.left + g / 2f, visto.top) to Offset(visto.left + g / 2f, visto.bottom)
                        ImageEdge.RIGHT -> Offset(visto.right - g / 2f, visto.top) to Offset(visto.right - g / 2f, visto.bottom)
                        ImageEdge.TOP -> Offset(visto.left, visto.top + g / 2f) to Offset(visto.right, visto.top + g / 2f)
                        ImageEdge.BOTTOM -> Offset(visto.left, visto.bottom - g / 2f) to Offset(visto.right, visto.bottom - g / 2f)
                    }
                    drawLine(brushAccent, da, a, g)
                }
            }
            // ⚠️ Le guide verso gli altri elementi: la stessa linea, da un elemento all'altro lungo
            // il lato o il centro che condividono.
            if (penLines.isNotEmpty()) {
                clipRect(visto.left, visto.top, visto.right, visto.bottom) {
                    for ((da, a) in penLines) drawLine(brushAccent, da, a, GUIDE_LINE.toPx())
                }
            }
            /*
             * ⚠️⚠️ **I punti dell'elemento scelto**: tondi d'accento di misura fissa sullo schermo,
             * dove la maglia posa gli angoli o gli estremi dell'elemento (specifica dell'utente),
             * più i punti a metà dei lati, le maniglie che lo deformano ([Mark.handles]). Sono del
             * palco e non del disegno, quindi non arrivano al file salvato.
             * ⚠️ La stessa strada del disegno: cornice originale, posa, poi la geometria (con la
             * vista di 'Angoli' quando è armato) attraverso [Warp.to].
             */
            drawPicked()?.let { look.drawing.marks.getOrNull(it) }?.let { segno ->
                val piano = if (look.geo.idle && !armato) null else Warp.plan(
                    look.geo, view.centerX(), view.centerY(), view.width(), view.height(),
                    hold = if (armato) ARMED_FIT else null
                )
                val sulPalco = onStage(picture.width, picture.height, look.spin, view, piano, look.geo.liquify)
                val r = HANDLE_DOT.toPx()
                val anello = HANDLE_RING.toPx()
                // ⚠️ In 'Ruota' le maniglie sono vuote: la modalità si vede dove va il dito.
                val vuote = drawTurning()
                val cavo = HANDLE_HOLLOW.toPx()
                clipRect(visto.left, visto.top, visto.right, visto.bottom) {
                    for (punto in segno.handles(picture.width, picture.height)) {
                        val centro = sulPalco(punto)
                        drawCircle(Color.White, r + anello, centro)
                        if (vuote) drawCircle(brushAccent, r - cavo / 2f, centro, style = Stroke(cavo))
                        else drawCircle(brushAccent, r, centro)
                    }
                }
            }
            penLoupe?.let { at ->
                lens(
                    at, null, LOUPE_SIDE.toPx(), LOUPE_EDGE.toPx(),
                    HANDLE_THICK.toPx(), LENS_EDGE.toPx(), brushAccent,
                    farthestCorner = true
                ) {
                    pictureForLens()
                }
            }
        }

        /*
         * ⚠️⚠️ **Le quattro maniglie degli angoli e il riquadro che resterà**: nella vista di
         * lavoro il velo dice quello che il ritaglio automatico toglierà.
         * Il riquadro tenuto si ricava dalle due scale: è il rettangolo che la copertura riempie,
         * scalato del rapporto fra le due. Con un conto suo, il velo e il file direbbero cose
         * diverse.
         */
        if (armato) {
            val lavoro = Warp.plan(
                look.geo, view.centerX(), view.centerY(), view.width(), view.height(),
                hold = ARMED_FIT
            )
            val finale = Warp.plan(
                look.geo, view.centerX(), view.centerY(), view.width(), view.height()
            )
            val k = if (finale.cover > 0f) lavoro.cover / finale.cover else 1f
            val tenuto = Rect(
                view.centerX() - view.width() / 2f * k,
                view.centerY() - view.height() / 2f * k,
                view.centerX() + view.width() / 2f * k,
                view.centerY() + view.height() / 2f * k
            )
            cornerOverlay(
                frame = Rect(0f, 0f, room.width, room.height),
                /*
                 * ⚠️⚠️ **Dentro il riquadro tenuto c'è il taglio**: armando lo strumento si vede
                 * l'immagine intera anche con un taglio applicato (vedi [framed]), e un velo fermo
                 * alla copertura direbbe che resterà più di quanto conterrà il file. Il taglio è
                 * [Look.crop], quello che il salvataggio applica, in frazioni di questo riquadro.
                 */
                keep = cropBox(look.crop, tenuto),
                spots = cornerSpots(lavoro, view),
                grip = CORNER_GRIP.toPx(),
                halo = GRIP_HALO.toPx()
            )
        }

    }

    }
}

/**
 * Dove cadono sullo schermo i quattro angoli dell'immagine, in senso orario da in alto a sinistra.
 *
 * ⚠️ **Lo stesso ordine di [Corners]**: l'indice di [nearestCorner] finisce dritto in
 * `Corners.with`, e due ordini diversi muoverebbero l'angolo sbagliato senza errori.
 */
private fun cornerSpots(plan: WarpPlan, view: RectF): List<Offset> = listOf(
    plan.map(view.left, view.top),
    plan.map(view.right, view.top),
    plan.map(view.right, view.bottom),
    plan.map(view.left, view.bottom)
).map { Offset(it[0], it[1]) }

/**
 * Quale dei quattro angoli il dito ha preso, o `-1` se nessuno è abbastanza vicino.
 *
 * ⚠️ **Il più vicino e non il primo che rientra**: con un'immagine molto deformata due maniglie
 * possono avvicinarsi, e il primo che rientra dipenderebbe dall'ordine dell'elenco.
 */
private fun nearestCorner(at: Offset, spots: List<Offset>, reach: Float): Int {
    var best = -1
    var near = reach
    spots.forEachIndexed { i, p ->
        val d = (p - at).getDistance()
        if (d <= near) {
            near = d
            best = i
        }
    }
    return best
}

/**
 * Le linee dei terzi sopra l'immagine mentre 'Raddrizza' si muove (nota E dell'utente sul giro
 * della 4.36: due verticali e due orizzontali, sempre visibili, via quando si lascia la manopola).
 *
 * ⚠️ **Sono i terzi dell'immagine [image], disegnati solo dove è sullo schermo** ([room]):
 * un'immagine ingrandita tiene i suoi terzi dove sono.
 * Hanno l'aspetto delle guide del Disegno (nota dell'utente sul giro della 4.63): [ink],
 * l'accento, larghe [GUIDE_LINE], senza alone.
 */
private fun DrawScope.levelThirds(image: RectF, room: Size, ink: Color) {
    val left = max(0f, image.left)
    val top = max(0f, image.top)
    val right = min(room.width, image.right)
    val bottom = min(room.height, image.bottom)
    if (right <= left || bottom <= top) return
    val g = GUIDE_LINE.toPx()
    for (i in 1..2) {
        val x = image.left + image.width() * i / 3f
        val y = image.top + image.height() * i / 3f
        if (x in left..right) drawLine(ink, Offset(x, top), Offset(x, bottom), g)
        if (y in top..bottom) drawLine(ink, Offset(left, y), Offset(right, y), g)
    }
}

/**
 * Il velo, il riquadro che resterà e le quattro maniglie dello strumento 'Angoli'.
 *
 * Le maniglie si disegnano per ultime, così restano in vista anche quando l'angolo cade fuori dal
 * riquadro tenuto. Il contorno dell'immagine deformata è più tenue del riquadro, che è quello che
 * resterà nel file.
 */
private fun DrawScope.cornerOverlay(
    frame: Rect,
    keep: Rect,
    spots: List<Offset>,
    grip: Float,
    halo: Float
) {
    val dim = Color.Black.copy(alpha = VEIL)
    val line = Color.White
    drawRect(dim, topLeft = frame.topLeft, size = Size(frame.width, keep.top - frame.top))
    drawRect(
        dim, topLeft = Offset(frame.left, keep.bottom),
        size = Size(frame.width, frame.bottom - keep.bottom)
    )
    drawRect(
        dim, topLeft = Offset(frame.left, keep.top),
        size = Size(keep.left - frame.left, keep.height)
    )
    drawRect(
        dim, topLeft = Offset(keep.right, keep.top),
        size = Size(frame.right - keep.right, keep.height)
    )

    if (spots.size == 4) {
        val contorno = Path().apply {
            moveTo(spots[0].x, spots[0].y)
            lineTo(spots[1].x, spots[1].y)
            lineTo(spots[2].x, spots[2].y)
            lineTo(spots[3].x, spots[3].y)
            close()
        }
        drawPath(contorno, line.copy(alpha = 0.45f), style = Stroke(width = EDGE_PX))
    }
    drawRect(
        color = line.copy(alpha = 0.9f),
        topLeft = keep.topLeft,
        size = keep.size,
        style = Stroke(width = EDGE_PX)
    )
    for (p in spots) {
        drawCircle(Color.Black.copy(alpha = 0.35f), grip + halo, p)
        drawCircle(line, grip, p)
    }
}

/**
 * Quanto si rimpicciolisce l'immagine mentre lo strumento 'Angoli' è armato.
 *
 * ⚠️⚠️ **Il numero viene dal limite di [Warp.PULL]**: un angolo arriva al massimo a `1 + PULL`
 * semilati dal centro, quindi perché la maniglia resti nel palco la vista deve stare sotto
 * `1 / (1 + PULL)`, cioè 0,74 con `PULL` a 0,35.
 * ⚠️ **Non si misura sul contorno deformato** (vedi `hold` di [Warp.plan]): una scala che
 * seguisse gli angoli terrebbe la maniglia incollata al bordo.
 */
private const val ARMED_FIT = 0.7f

/** Il raggio del tondo di una maniglia d'angolo: un tondo dice 'portami dove vuoi'. */
private val CORNER_GRIP = 7.dp

/** Come è andata a finire l'attesa di [settled]. */
private enum class Settled { UP, MOVED, MULTI }

/**
 * Attende che il dito faccia qualcosa: si alzi, si muova oltre la soglia, o porti un compagno.
 *
 * ⚠️ **Non risponde se il dito resta fermo**: chi la chiama la avvolge in un timeout, e il
 * timeout scaduto è il tocco lungo.
 */
private suspend fun AwaitPointerEventScope.settled(
    down: PointerInputChange,
    slop: Float,
    onPosition: (Offset) -> Unit = {}
): Settled {
    var travel = Offset.Zero
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.count { it.pressed } > 1) return Settled.MULTI
        val mine = event.changes.firstOrNull { it.id == down.id } ?: return Settled.UP
        onPosition(mine.position)
        if (!mine.pressed) return Settled.UP
        travel += mine.positionChange()
        if (travel.getDistance() > slop) return Settled.MOVED
    }
}

/**
 * Pinza e panoramica, fino a quando l'ultimo dito si alza.
 *
 * ⚠️ **Consuma quello che usa**: un evento non consumato risale ai genitori, e sopra questo palco
 * c'è lo scorrimento della schermata.
 */
private suspend fun AwaitPointerEventScope.transformed(
    onMove: (centroid: Offset, pan: Offset, zoom: Float) -> Unit
) {
    var alive = true
    while (alive) {
        val event = awaitPointerEvent()
        val zoom = event.calculateZoom()
        val pan = event.calculatePan()
        if (zoom != 1f || pan != Offset.Zero) {
            onMove(event.calculateCentroid(useCurrent = false), pan, zoom)
            event.changes.forEach { if (it.positionChanged()) it.consume() }
        }
        alive = event.changes.any { it.pressed }
    }
}

/**
 * Il dito che, dopo un doppio tocco, resta giù e trascina: l'ingrandimento a una mano.
 *
 * ⚠️ **Riferisce la distanza dal punto di partenza e non l'ultimo passo**: il conto non accumula,
 * e il dito che torna indietro riporta l'immagine dov'era. Il primo evento oltre la soglia l'ha
 * già letto `settled`, e basta quello dopo, con la posizione corrente.
 * Consuma quello che usa, come la pinza.
 */
private suspend fun AwaitPointerEventScope.hauled(
    down: PointerInputChange,
    onPull: (Float) -> Unit
) {
    var alive = true
    while (alive) {
        val event = awaitPointerEvent()
        val mine = event.changes.firstOrNull { it.id == down.id }
        if (mine != null && mine.positionChanged()) {
            onPull(mine.position.y - down.position.y)
            mine.consume()
        }
        alive = event.changes.any { it.pressed }
    }
}

/**
 * Dove arriva l'ingrandimento a una mano: da [from], trascinando di [dy] pixel.
 *
 * Si raddoppia a ogni [span] e non cresce di un tanto al pixel: l'ingrandimento si percepisce in
 * rapporti, quindi un centimetro di dito vale sempre lo stesso raddoppio.
 *
 * ⚠️ **Giù ingrandisce**, come ha chiesto l'utente: il puntatore conta positivo verso il basso, e
 * un segno di troppo darebbe un gesto rovesciato che nessun compilatore vede. Questa riga ha una
 * prova. Il tetto è quello della pinza, [ZOOM_MAX].
 */
internal fun pulled(from: Float, dy: Float, span: Float): Float =
    (from * 2f.pow(dy / span)).coerceIn(1f, ZOOM_MAX)

/**
 * Lo spostamento [want] riportato dentro i bordi di un'immagine ingrandita di [zoom].
 *
 * A ingrandimento uno la risposta è zero: l'immagine resta centrata.
 *
 * La leggono il gesto, per non accumulare uno spostamento da riavvolgere, e il disegno, per il
 * tratto in cui l'ingrandimento si muove da sé. È pura e idempotente.
 */
private fun reined(want: Offset, zoom: Float, room: Size, wide: Float, air: Float = 0f): Offset {
    val box = fitted(room, wide, air)
    val slackX = ((box.width() * zoom) - room.width).coerceAtLeast(0f) / 2f
    val slackY = ((box.height() * zoom) - room.height).coerceAtLeast(0f) / 2f
    return Offset(want.x.coerceIn(-slackX, slackX), want.y.coerceIn(-slackY, slackY))
}

/**
 * Il rettangolo in cui un'immagine larga [wide] entra dentro [room] senza deformarsi, lasciando
 * [air] di aria su ogni lato.
 *
 * La leggono il disegno e il gesto: i limiti della panoramica si contano sull'immagine adattata, e
 * due conti in due posti darebbero una panoramica che si ferma dove l'immagine non finisce.
 *
 * ⚠️ **L'aria serve alle squadrette del Ritaglio**, che si disegnano fuori dal rettangolo
 * (`bracket`, in `EditorScreen.kt`) mentre il palco ritaglia al proprio riquadro: senza, quelle dei
 * due lati che l'immagine tocca sarebbero tagliate a metà. Si rimpicciolisce l'immagine e non il
 * palco, che altrimenti tornerebbe a tagliarle. Vale in tutti i moduli: il perché vive su `air()`.
 */
private fun fitted(room: Size, wide: Float, air: Float = 0f): RectF {
    val dentro = Size(
        (room.width - 2f * air).coerceAtLeast(1f),
        (room.height - 2f * air).coerceAtLeast(1f)
    )
    val box = if (dentro.width / dentro.height > wide) {
        val h = dentro.height
        val w = h * wide
        RectF((dentro.width - w) / 2f, 0f, (dentro.width + w) / 2f, h)
    } else {
        val w = dentro.width
        val h = w / wide
        RectF(0f, (dentro.height - h) / 2f, w, (dentro.height + h) / 2f)
    }
    box.offset(air, air)
    return box
}

/**
 * Dove l'immagine intera finisce sullo schermo: adattata al palco, ingrandita attorno al suo
 * centro e poi spostata.
 *
 * La leggono il disegno, il gesto che deve sapere quale pixel c'è sotto il dito, e la richiesta
 * del pezzo a risoluzione piena: due conti diversi darebbero un colore preso da un punto e una
 * lente che ne mostra un altro. Lo spostamento si riporta nei bordi qui, per tutti i chiamanti.
 */
private fun viewport(
    room: Size,
    wide: Float,
    scale: Float,
    shift: Offset,
    air: Float = 0f,
    /**
     * Il taglio confermato con 'Applica', quando il palco inquadra lui invece dell'immagine
     * intera: vedi [spread].
     */
    cut: ImageEdit.Crop? = null
): RectF {
    val box = fitted(room, wide, air)
    val safe = reined(shift, scale, room, wide, air)
    val midX = room.width / 2f
    val midY = room.height / 2f
    val seen = RectF(
        midX + (box.left - midX) * scale + safe.x,
        midY + (box.top - midY) * scale + safe.y,
        midX + (box.right - midX) * scale + safe.x,
        midY + (box.bottom - midY) * scale + safe.y
    )
    return if (cut == null) seen else spread(seen, cut)
}

/**
 * Dove finisce l'immagine **intera**, dato dove deve finire il suo taglio [cut].
 *
 * Fa vedere il ritaglio senza toccare un altro conto: tutto il palco (il pezzo a risoluzione
 * piena, la lente, il colore mirato, la maglia della geometria) ragiona su dove finisce l'immagine
 * intera, e quel riquadro qui si ricava da dove deve cadere la porzione scelta.
 * L'inversa è [cutout], che serve al disegno come confine oltre il quale non si vede niente.
 */
private fun spread(seen: RectF, cut: ImageEdit.Crop): RectF {
    val w = seen.width() / (cut.right - cut.left).coerceAtLeast(0.001f)
    val h = seen.height() / (cut.bottom - cut.top).coerceAtLeast(0.001f)
    val left = seen.left - cut.left * w
    val top = seen.top - cut.top * h
    return RectF(left, top, left + w, top + h)
}

/**
 * Il taglio [cut], che è in frazioni dell'immagine intera, riscritto in frazioni della porzione
 * [outer] che il palco inquadra.
 *
 * La coppia di [absolute]: col taglio applicato le squadrette si tirano dentro la porzione, quindi
 * il rettangolo che il dito muove è in frazioni di lei, mentre [Look.crop] resta il rettangolo
 * totale che il salvataggio applica. Con `null` (la porzione è tutta l'immagine) sono l'identità.
 */
private fun relativeTo(outer: ImageEdit.Crop?, cut: ImageEdit.Crop): ImageEdit.Crop {
    if (outer == null) return cut
    val w = (outer.right - outer.left).coerceAtLeast(0.001f)
    val h = (outer.bottom - outer.top).coerceAtLeast(0.001f)
    return ImageEdit.Crop(
        ((cut.left - outer.left) / w).coerceIn(0f, 1f),
        ((cut.top - outer.top) / h).coerceIn(0f, 1f),
        ((cut.right - outer.left) / w).coerceIn(0f, 1f),
        ((cut.bottom - outer.top) / h).coerceIn(0f, 1f)
    )
}

/** Il taglio [part], che è in frazioni della porzione [outer], riportato all'immagine intera. */
private fun absolute(outer: ImageEdit.Crop?, part: ImageEdit.Crop): ImageEdit.Crop {
    if (outer == null) return part
    val w = outer.right - outer.left
    val h = outer.bottom - outer.top
    return ImageEdit.Crop(
        outer.left + part.left * w,
        outer.top + part.top * h,
        outer.left + part.right * w,
        outer.top + part.bottom * h
    )
}

/** Dove cade il taglio [cut] dentro il riquadro [view] dell'immagine intera: vedi [spread]. */
private fun cutout(view: RectF, cut: ImageEdit.Crop?): RectF =
    if (cut == null) view else RectF(
        view.left + cut.left * view.width(),
        view.top + cut.top * view.height(),
        view.left + cut.right * view.width(),
        view.top + cut.bottom * view.height()
    )

/**
 * Un cursore del modulo: il suo nome, come si legge il suo valore e come si riscrive.
 *
 * I cursori sono una tabella: ognuno ha tre gesti (il trascinamento, il doppio tocco che azzera,
 * il tocco lungo che confronta), che così si scrivono una volta e un cursore nuovo li prende per
 * costruzione.
 */
private class Dial(
    @param:StringRes val name: Int,
    val read: (Look) -> Float,
    val write: (Look, Float) -> Look,
    val span: Float = 1f,
    val stops: Boolean = false,
    /**
     * Se la corsa parte da zero invece che da `-span`: per i cursori in cui zero è il fondo
     * naturale, come la nitidezza (negativa sarebbe una sfocatura) e la riduzione del rumore.
     */
    val unipolar: Boolean = false,
    /**
     * Quando questo cursore non governa niente, e l'interfaccia lo spegne: per esempio i colori
     * col bianco e nero, o la maschera di contrasto senza nitidezza.
     */
    val off: (Look) -> Boolean = { false }
) {
    /**
     * Il cambiamento che porta questo cursore a [v], da applicare a quello che si vede **adesso**.
     *
     * È una trasformazione e non un [Look] già fatto: il punto di partenza lo legge chi la
     * applica, cioè lo stato vivo, e non un `Look` catturato da un `pointerInput` vecchio. È una
     * difesa, non la cura di una causa misurata (`docs/Editor.md`, 'Due difetti chiusi senza
     * causa accertata').
     */
    fun set(v: Float): (Look) -> Look = { write(it, v) }
}

/**
 * Un modulo: il suo nome, i suoi cursori, e come si rimette a zero tutto insieme.
 *
 * [clear] è il 'Reset modulo' chiesto dall'utente (giro della `2.14`, punto 2), sul tocco lungo
 * del gettone.
 */
private class Module(
    /**
     * Il gettone con cui questo modulo si salva: la fila si riordina (richiesta dell'utente), e
     * un ordine salvato per indice cambierebbe significato il giorno che si aggiunge un modulo.
     * È un [PadKey] per usare il riordino, la replica nelle impostazioni e le azioni parlate dei
     * riquadri di casa.
     */
    val key: PadKey,
    @param:StringRes val name: Int,
    /**
     * I cursori da mostrare, data la fascia scelta.
     *
     * È una funzione perché l'HSL ha un insieme per ognuna delle otto fasce; gli altri moduli
     * ignorano il parametro.
     */
    val rows: (Int) -> List<Dial>,
    val clear: (Look) -> Look,
    val spent: (Look) -> Boolean,
    /**
     * L'icona del gettone, al posto del nome, per richiesta dell'utente (giro della `2.29`): i
     * nomi non entrano in nessuna larghezza. Il nome resta il `contentDescription` e l'etichetta
     * del 'Reset modulo'.
     * È una lambda perché alcuni glifi vivono in `res/` e si leggono con `vectorResource`, che è
     * `@Composable`, mentre questa tabella è una costante di file.
     */
    val icon: @Composable () -> ImageVector,
    /** Che cosa questo modulo ha in più dei suoi cursori: vedi [Extra]. */
    val extra: Extra = Extra.NONE,
    /**
     * Se il tasto 'Auto' ha senso mentre si guarda questo modulo: solo la Luce e il Colore, per
     * richiesta dell'utente (giro della `2.40`, punto 5), perché quel comando scrive nei quattro
     * cursori della Luce e nei due del bilanciamento del bianco.
     */
    val auto: Boolean = false
)

/**
 * Sotto questa misura sullo schermo, in centimetri, una forma che si disegna ha la lente (R2 del
 * giro della `4.40`): circa un polpastrello e mezzo, dove il dito copre quasi tutto quello che
 * disegna. È una scelta, dichiarata nella voce di collaudo, non una misura.
 */
private const val PEN_LOUPE_CM = 1.5f

/** La lunghezza minima del campione curvo nell'anteprima dello Spessore. */
private val SAMPLE_LONG = 56.dp

/**
 * Quanto lontano dalla linea di un elemento, oltre metà del suo spessore, un tocco lo sceglie
 * ancora (G2, `4.60`): circa mezzo polpastrello, scelta dichiarata nella voce di collaudo.
 */
private val PICK_REACH = 24.dp

/** I punti dell'elemento scelto: un tondo d'accento con un anello che lo tiene visibile su ogni immagine. */
private val HANDLE_DOT = 5.dp
private val HANDLE_RING = 1.5.dp

/**
 * Lo spessore dell'anello d'accento di una maniglia in 'Ruota' (`4.70`), dove le maniglie sono
 * vuote perché la modalità si veda sull'elemento stesso. Scelta dichiarata nella voce di collaudo.
 */
private val HANDLE_HOLLOW = 2.dp

/**
 * Quanto vicino a una maniglia dell'elemento scelto il dito la prende (`4.70`). ⚠️ È minore di
 * [PICK_REACH] perché le maniglie coprono l'elemento: su uno piccolo una presa più larga non
 * lascerebbe spazio per spostarlo. Scelta dichiarata nella voce di collaudo.
 */
private val HANDLE_REACH = 16.dp

/**
 * Dove cadono sul palco i punti del Disegno: un punto in frazioni dell'immagine originale [w] x
 * [h], attraverso la posa [spin], nel riquadro [view] dell'immagine posata, e attraverso la
 * geometria [plan] quando c'è. La via di ritorno è `toImage` del gesto.
 * Una funzione sola per i punti dell'elemento scelto e per l'appoggio ai bordi, così cadono dove
 * cade il disegno; la posa si costruisce una volta e la mappa gira per ogni punto.
 */
private fun onStage(w: Int, h: Int, spin: Spin, view: RectF, plan: WarpPlan?, liquify: Liquify): (Offset) -> Offset {
    val posa = Draw.posed(w, h, spin)
    val odd = spin.turns.mod(2) == 1
    val pw = if (odd) h else w
    val ph = if (odd) w else h
    val q = FloatArray(2)
    return { p ->
        q[0] = p.x * w
        q[1] = p.y * h
        posa.mapPoints(q)
        val x = view.left + q[0] / pw * view.width()
        val y = view.top + q[1] / ph * view.height()
        val d = plan?.let { Warp.to(it, view, liquify, x, y) }
        if (d == null) Offset(x, y) else Offset(d[0], d[1])
    }
}

/** Il riquadro sul palco coperto dal contorno di [mark], coi punti portati da [toStage]. */
private fun stageBox(mark: Mark, toStage: (Offset) -> Offset, long: Float, w: Int, h: Int): Rect {
    // ⚠️ Un testo e una pastiglia sono il loro riquadro, senza linea attorno: lo spessore di un
    // testo è il suo corpo, e i tratti di una pastiglia sono dentro il riquadro.
    if (mark.pen.written) return Draw.extent(Draw.outline(mark, w, h).map(toStage), 0f)
    val stroke = mark.width * long
    // ⚠️ Un rettangolo o un'ellisse ruotati si misurano sul contorno: i due angoli non dicono dove
    // sono finiti i lati.
    if (mark.pen.closed && mark.angle != 0f) return Draw.extent(Draw.outline(mark, w, h).map(toStage), stroke)
    val pts = mark.points.map(toStage)
    return if (mark.pen == Pen.FREE) Draw.extent(pts, stroke)
    else Draw.extent(mark.pen, pts.first(), pts.last(), stroke, long)
}

/** Lo spessore sul palco della guida di una linea agganciata. */
private val GUIDE_LINE = 1.dp

/**
 * Quanto vicino a un bordo dell'immagine visibile il contorno di un elemento vi si appoggia
 * (`4.62`): circa 2 mm, un piccolo strappo per staccarlo, come i 5 gradi dell'orizzontale e della
 * verticale. Scelta dichiarata nella voce di collaudo.
 */
private val EDGE_REACH = 12.dp

/**
 * Che cosa un modulo mette in scena oltre ai propri cursori.
 *
 * È un valore solo e non una bandierina per ogni caso: due booleani si potrebbero accendere insieme, e
 * un modulo con le fasce e il grafico sarebbe una riga che compila e non vuol dire niente.
 */
private enum class Extra {
    /** Niente: la scheda mostra i soli cursori, come la Luce e il Colore. */
    NONE,

    /** La fila delle otto fasce di colore, cioè l'HSL. */
    BANDS,

    /**
     * La fila dei quattro canali e il grafico della curva, cioè le Curve. Quel modulo non ha
     * cursori: la sua lista è vuota, quindi il ciclo dei cursori non disegna niente.
     */
    CURVES,

    /**
     * I tre comandi di posa e le maniglie sul palco, cioè il Ritaglio: prende il dito
     * sull'immagine appena è in scena, e il palco lo chiede alla tabella dei moduli.
     */
    CROP,

    /**
     * Lo strumento Angoli e le sue quattro maniglie sul palco, nella Geometria: prende il dito solo
     * quando è armato, come il colore mirato, perché i cursori si tarano guardando l'immagine da
     * vicino, con la pinza e la panoramica.
     */
    CORNERS,

    /**
     * L'elenco degli stili: l'unico corpo che riceve l'altezza invece di dettarla (vedi
     * [SteadyBody]), perché cresce con quello che l'utente salva; contarlo farebbe alzare la scheda
     * a ogni stile salvato.
     */
    PRESETS,

    /** Il pennello correttivo e la selezione sul palco, nel Dettaglio. */
    HEALING,

    /**
     * Gli strumenti del Disegno e il dito sull'immagine. Lo prende sempre, come il Ritaglio:
     * armare lo strumento costerebbe un tocco in più a ogni elemento. Pinza e panoramica restano a
     * due dita. Non è [places]: il confronto col prima lo raggiunge solo un dito, e qui ogni dito
     * disegna.
     */
    DRAW;

    /**
     * Se questo modulo dice dove va un pixel invece di che colore è, per il confronto col prima
     * (vedi [Look.place]). Lo dichiara la tabella: un `if` accanto al palco darebbe a un modulo
     * nuovo il confronto sbagliato senza nessun errore.
     */
    val places: Boolean get() = this == CROP || this == CORNERS
}

/**
 * I cursori che il **bianco e nero** spegne: là non c'è più niente da saturare, e un cursore che
 * si muove senza cambiare l'immagine si legge come un guasto.
 */
private val MONO: (Look) -> Boolean = { it.chroma.mono }

/**
 * Il rovescio di [MONO]: la riga che governa qualcosa solo col bianco e nero, cioè il 'Filtro BN'.
 * A colori la riga resta in scena spenta, sotto l'interruttore, come ha chiesto l'utente (punto A
 * del giro della `2.36`).
 */
private val NOT_MONO: (Look) -> Boolean = { !it.chroma.mono }

/**
 * I cursori della maschera di contrasto (il raggio e la mascheratura), che dicono come lavora la
 * nitidezza: con la nitidezza a zero non governano niente.
 */
private val UNSHARP: (Look) -> Boolean = { it.detail.flat }

/**
 * Le righe che governano la grana, cioè 'Dimensione' e 'Luci', spente finché la grana è a zero:
 * il criterio di [UNSHARP].
 */
private val NO_GRAIN: (Look) -> Boolean = { it.effects.noGrain }

/** Come [NO_GRAIN], per la 'Sfumatura' della vignettatura. */
private val NO_VIGNETTE: (Look) -> Boolean = { it.effects.noVignette }

/**
 * I nomi delle otto fasce, nell'ordine dei centri di [Mix.CENTRES].
 *
 * ⚠️ I due elenchi si leggono per indice: la pastiglia prende il colore dal centro e il nome da
 * qui, quindi una coppia sbagliata darebbe una pastiglia verde che si chiama 'Blu'.
 */
private val BAND_NAMES = listOf(
    R.string.look_band_red,
    R.string.look_band_orange,
    R.string.look_band_yellow,
    R.string.look_band_green,
    R.string.look_band_aqua,
    R.string.look_band_blue,
    R.string.look_band_purple,
    R.string.look_band_magenta
)

/**
 * I tre cursori dell'HSL, uno per ognuna delle otto fasce: ventiquattro oggetti, costruiti una
 * volta sola.
 *
 * ⚠️ **Sono precostruiti** perché la scheda usa ogni [Dial] come `key` della sua riga: una chiave
 * che cambia a ogni ricomposizione rifarebbe i nodi dei cursori e annullerebbe ogni gesto in corso.
 * Due fasce non hanno chiavi in comune, quindi cambiando fascia i nodi si rifanno.
 *
 * La saturazione riusa la stringa del modulo Colore: è la stessa parola, e una chiave nuova
 * sarebbe una seconda traduzione da tenere allineata.
 */
private val MIX_ROWS: List<List<Dial>> = List(Mix.COUNT) { b ->
    listOf(
        Dial(
            R.string.look_hue,
            { it.mix.bands[b].hue },
            { k, v -> k.copy(mix = k.mix.swap(b) { it.copy(hue = v) }) },
            off = MONO
        ),
        Dial(
            R.string.look_saturation,
            { it.mix.bands[b].sat },
            { k, v -> k.copy(mix = k.mix.swap(b) { it.copy(sat = v) }) },
            off = MONO
        ),
        /*
         * ⚠️ Questa riga è anche la miscela del bianco e nero (risposta `hsl` dell'utente a
         * `d-bn-pesi`): col bianco e nero acceso le altre due si spengono, e questa dice quanto
         * quel colore pesa nel grigio. Per questo non ha `off`: il bianco e nero viene dopo nella
         * catena e ne raccoglie il risultato.
         */
        Dial(
            R.string.look_lum,
            { it.mix.bands[b].lum },
            { k, v -> k.copy(mix = k.mix.swap(b) { it.copy(lum = v) }) }
        )
    )
}

/** I sei cursori del modulo Luce. */
private val LIGHT_ROWS = listOf(
    Dial(
        R.string.look_exposure,
        { it.light.exposure },
        { k, v -> k.copy(light = k.light.copy(exposure = v)) },
        span = Light.EXPOSURE_RANGE, stops = true
    ),
    Dial(
        R.string.look_contrast,
        { it.light.contrast },
        { k, v -> k.copy(light = k.light.copy(contrast = v)) }
    ),
    Dial(
        R.string.look_highlights,
        { it.light.highlights },
        { k, v -> k.copy(light = k.light.copy(highlights = v)) }
    ),
    Dial(
        R.string.look_shadows,
        { it.light.shadows },
        { k, v -> k.copy(light = k.light.copy(shadows = v)) }
    ),
    Dial(
        R.string.look_whites,
        { it.light.whites },
        { k, v -> k.copy(light = k.light.copy(whites = v)) }
    ),
    Dial(
        R.string.look_blacks,
        { it.light.blacks },
        { k, v -> k.copy(light = k.light.copy(blacks = v)) }
    )
)

/**
 * Il cursore del filtro del bianco e nero, davanti al quale il corpo posa l'interruttore del
 * bianco e nero. Vive a parte perché il corpo lo riconosce per identità (`knob === FILTER_ROW`):
 * un indice sbaglierebbe in silenzio il giorno che si aggiunge un cursore a [COLOUR_ROWS].
 */
private val FILTER_ROW = Dial(
    R.string.look_filter,
    { it.chroma.filter },
    { k, v -> k.copy(chroma = k.chroma.copy(filter = v)) },
    off = NOT_MONO
)

/**
 * I cursori del modulo Colore, con l'interruttore del bianco e nero fra i quattro del colore e il
 * 'Filtro BN'.
 *
 * Il quinto, il 'Filtro BN', lavora solo col bianco e nero acceso (richiesta dell'utente; che cosa
 * fa vive su [Chroma.grey]) e resta sempre in scena sotto l'interruttore (punto A del giro della
 * `2.36`): prima i cursori del colore, poi la riga che accende, poi il comando che ne dipende.
 */
private val COLOUR_ROWS = listOf(
    Dial(
        R.string.look_temp,
        { it.chroma.temp },
        { k, v -> k.copy(chroma = k.chroma.copy(temp = v)) }
    ),
    Dial(
        R.string.look_tint,
        { it.chroma.tint },
        { k, v -> k.copy(chroma = k.chroma.copy(tint = v)) }
    ),
    Dial(
        R.string.look_saturation,
        { it.chroma.saturation },
        { k, v -> k.copy(chroma = k.chroma.copy(saturation = v)) },
        off = MONO
    ),
    Dial(
        R.string.look_vibrance,
        { it.chroma.vibrance },
        { k, v -> k.copy(chroma = k.chroma.copy(vibrance = v)) },
        off = MONO
    ),
    FILTER_ROW
)


/**
 * I cinque cursori del modulo Dettaglio, nell'ordine del pannello di Lightroom: prima la maschera
 * di contrasto coi suoi due comandi, poi le due riduzioni del rumore.
 *
 * Il raggio è l'unico bipolare, col suo zero sul raggio di serie: un raggio zero non esiste (le
 * misure vivono su [Detail.sharpReach]).
 *
 * 'Rumore' e 'Rumore colore' e non 'Luminanza' e 'Colore', come in Lightroom: qui 'Luminanza' è
 * una riga dell'HSL e 'Colore' è il nome di un modulo.
 */
private val DETAIL_ROWS = listOf(
    Dial(
        R.string.look_sharpen,
        { it.detail.sharpen },
        { k, v -> k.copy(detail = k.detail.copy(sharpen = v)) },
        unipolar = true
    ),
    Dial(
        R.string.look_radius,
        { it.detail.radius },
        { k, v -> k.copy(detail = k.detail.copy(radius = v)) },
        off = UNSHARP
    ),
    Dial(
        R.string.look_masking,
        { it.detail.masking },
        { k, v -> k.copy(detail = k.detail.copy(masking = v)) },
        unipolar = true,
        off = UNSHARP
    ),
    Dial(
        R.string.look_noise,
        { it.detail.noise },
        { k, v -> k.copy(detail = k.detail.copy(noise = v)) },
        unipolar = true
    ),
    Dial(
        R.string.look_noise_color,
        { it.detail.noiseColor },
        { k, v -> k.copy(detail = k.detail.copy(noiseColor = v)) },
        unipolar = true
    )
)

/**
 * I sei cursori degli **Effetti**: foschia, grana coi suoi due comandi, vignettatura col suo.
 *
 * L'elenco e l'ordine sono dell'utente (`d-dopo-editor`); 'Chiarezza' e 'Texture' sono tolte con
 * la sua risposta `via` a `d-eff-restano`, e il perché vive in testa a [Effects].
 *
 * I cursori secondari sono sua richiesta: 'Dimensione' e 'Luci' per la grana, 'Sfumatura' per la
 * vignettatura, cioè l'elenco che ha scritto, che vince sul suo 'uno per effetto' (la voce di
 * collaudo glielo dice). Ognuno vive sotto il cursore che lo governa e si spegne quando quello è a
 * zero, come [UNSHARP].
 *
 * Quattro sono bipolari: verso il basso la foschia si aggiunge, la vignettatura apre l'angolo, la
 * 'Sfumatura' porta l'alone verso il bordo, e 'Dimensione' si dimezza attorno al valore di serie.
 * La grana non ha un verso negativo sensato, e le 'Luci' partono dallo zero di fabbrica chiesto
 * dall'utente.
 */
private val EFFECT_ROWS = listOf(
    Dial(
        R.string.look_haze,
        { it.effects.haze },
        { k, v -> k.copy(effects = k.effects.copy(haze = v)) }
    ),
    Dial(
        R.string.look_grain,
        { it.effects.grain },
        { k, v -> k.copy(effects = k.effects.copy(grain = v)) },
        unipolar = true
    ),
    Dial(
        R.string.look_grain_size,
        { it.effects.grainSize },
        { k, v -> k.copy(effects = k.effects.copy(grainSize = v)) },
        off = NO_GRAIN
    ),
    Dial(
        R.string.look_grain_lift,
        { it.effects.grainLift },
        { k, v -> k.copy(effects = k.effects.copy(grainLift = v)) },
        unipolar = true,
        off = NO_GRAIN
    ),
    Dial(
        R.string.look_vignette,
        { it.effects.vignette },
        { k, v -> k.copy(effects = k.effects.copy(vignette = v)) }
    ),
    Dial(
        R.string.look_vignette_feather,
        { it.effects.vignetteFeather },
        { k, v -> k.copy(effects = k.effects.copy(vignetteFeather = v)) },
        off = NO_VIGNETTE
    )
)

/**
 * Il cursore 'Raddrizza', con un nome perché mentre si muove il palco disegna la griglia dei terzi
 * (vedi [Gaze.leveling]). Decide l'identità della riga, come per [FILTER_ROW]: un indice
 * cambierebbe il giorno che si aggiunge un cursore davanti.
 */
private val STRAIGHTEN_ROW = Dial(
    R.string.look_straighten,
    { it.geo.straighten },
    { k, v -> k.copy(geo = k.geo.copy(straighten = v)) }
)

/**
 * I cinque cursori del **Geometria**, nell'ordine del pannello di Lightroom: raddrizzamento,
 * proporzioni, orizzontale, verticale, distorsione.
 *
 * Le due correzioni di prospettiva si chiamano col proprio asse, 'Verticale' e 'Orizzontale', come
 * in Lightroom: chiamarne una 'Prospettiva' direbbe che l'altra è un'altra cosa.
 *
 * Sono tutti bipolari, perché ognuno ha due versi con due correzioni diverse e lo zero è
 * l'immagine come la fotocamera l'ha presa. Il raddrizzamento si mostra da -100 a +100 come gli
 * altri e non in gradi; quanto vale il fondo corsa lo dice [Warp.TILT].
 */
private val GEO_ROWS = listOf(
    STRAIGHTEN_ROW,
    Dial(
        R.string.look_aspect,
        { it.geo.aspect },
        { k, v -> k.copy(geo = k.geo.copy(aspect = v)) }
    ),
    Dial(
        R.string.look_horizontal,
        { it.geo.horizontal },
        { k, v -> k.copy(geo = k.geo.copy(horizontal = v)) }
    ),
    Dial(
        R.string.look_vertical,
        { it.geo.vertical },
        { k, v -> k.copy(geo = k.geo.copy(vertical = v)) }
    ),
    Dial(
        R.string.look_distortion,
        { it.geo.distortion },
        { k, v -> k.copy(geo = k.geo.copy(distortion = v)) }
    )
)

/**
 * I moduli, nell'ordine in cui la fila li disegna.
 *
 * L'ordine dei cursori è quello del pannello di Lightroom, il riferimento dell'utente (giro della
 * `2.15`): chi apre questo editor ha in mente quello.
 */
private val MODULES = listOf(
    /*
     * L'ordine è dell'utente (riscontro del giro della `2.54`): Dettagli, Effetti, Geometria,
     * Ritaglio, Luce, Colore, HSL, Curve, Stili; il Disegno, nato dopo, è prima degli Stili.
     * ⚠️ Il posto nella fila non è il posto nella catena: la fila è l'ordine in cui si lavora, la
     * catena quello in cui il conto gira.
     * ⚠️ **Questo elenco e `MOD_KEYS` si riordinano insieme**: là vive il valore di fabbrica
     * dell'archivio, qui il nome e il glifo di ognuno.
     */
    /*
     * Il Dettaglio è il primo anche nella catena, per coincidenza: parla del file (il rumore del
     * sensore, il disegno fine), e va letto prima che il contrasto moltiplichi la grana.
     */
    Module(
        PadKey.MOD_DETAIL,
        R.string.look_detail,
        rows = { DETAIL_ROWS },
        clear = { it.copy(detail = Detail.NONE, healing = Healing.Plan.NONE) },
        spent = { !it.detail.idle || !it.healing.idle },
        icon = { Glyphs.ModDetail },
        extra = Extra.HEALING
    ),
    /*
     * Gli Effetti vivono accanto all'altro modulo che guarda i pixel vicini, col quale dividono il
     * bordo delle tessere del salvataggio (vedi `AdjustRender.bleedFor`).
     * Il glifo è il disegno dell'utente in `res/` (voce `eff-glifo`: quello di prima era uguale al
     * Dettaglio); le sue misure vivono in testa a `ic_mod_effects.xml`.
     */
    Module(
        PadKey.MOD_EFFECTS,
        R.string.look_effects,
        rows = { EFFECT_ROWS },
        clear = { it.copy(effects = Effects.NONE) },
        spent = { !it.effects.idle },
        icon = { Glyphs.ModEffects }
    ),
    /*
     * La Geometria non cambia il colore di un pixel, dice dove va a finire: perché il suo conto si
     * fa dopo lo shader vive in testa a `Geometry.kt`. Il tasto che arma il dito è lo stesso del
     * colore mirato, e qui arma lo strumento 'Angoli' (vedi [Extra.CORNERS]).
     */
    Module(
        PadKey.MOD_GEOMETRY,
        R.string.look_geometry,
        rows = { GEO_ROWS },
        clear = { it.copy(geo = Geometry.NONE) },
        spent = { !it.geo.idle },
        icon = { Glyphs.ModGeometry },
        extra = Extra.CORNERS
    ),
    /*
     * Il Ritaglio è aperto di fabbrica, per istruzione dell'utente; l'indice si ricava dalla
     * tabella (vedi [LOOK_FIRST]).
     */
    Module(
        PadKey.MOD_CROP,
        R.string.look_crop,
        rows = { emptyList() },
        // ⚠️ Il 'Reset modulo' toglie anche i tagli applicati, o il palco inquadrerebbe un taglio
        // che non c'è più. Azzera anche la posa, al contrario del comando 'Azzera' della fila, che
        // guarda il solo ritaglio.
        clear = { it.copy(spin = Spin.STILL, crop = ImageEdit.Crop.WHOLE, framing = Framing.NONE) },
        spent = { !it.square },
        icon = { Glyphs.ModCrop },
        extra = Extra.CROP
    ),
    /*
     * I due moduli che 'Auto' tocca: vedi [Module.auto].
     */
    Module(
        PadKey.MOD_LIGHT,
        R.string.look_light,
        rows = { LIGHT_ROWS },
        clear = { it.copy(light = Light.NONE) },
        spent = { !it.light.idle },
        icon = { Glyphs.ModLight },
        auto = true
    ),
    Module(
        PadKey.MOD_COLOUR,
        R.string.look_color,
        rows = { COLOUR_ROWS },
        clear = { it.copy(chroma = Chroma.NONE) },
        spent = { !it.chroma.idle },
        icon = { Glyphs.Material.Palette },
        auto = true
    ),
    Module(
        PadKey.MOD_MIX,
        R.string.look_mix,
        rows = { MIX_ROWS[it] },
        clear = { it.copy(mix = Mix.NONE) },
        spent = { !it.mix.idle },
        icon = { Glyphs.ModMix },
        extra = Extra.BANDS
    ),
    /*
     * Le Curve non hanno cursori: un cursore dice 'quanto', una curva 'quanto per ogni tono'. Il
     * comando è il grafico.
     */
    Module(
        PadKey.MOD_TONE,
        R.string.look_tone,
        rows = { emptyList() },
        clear = { it.copy(tone = Tone.NONE) },
        spent = { !it.tone.idle },
        icon = { Glyphs.Material.Timeline },
        extra = Extra.CURVES
    ),
    /*
     * Il Disegno (nota D del giro della `4.34`, risposte D1a-D4a) vive solo qui, quindi sotto
     * Android 13 non c'è. Nella catena del salvataggio è dopo lo sviluppo e prima della geometria
     * (vedi `Drawing.kt`).
     */
    Module(
        PadKey.MOD_DRAW,
        R.string.look_draw,
        rows = { emptyList() },
        clear = { it.copy(drawing = Drawing.NONE) },
        spent = { !it.drawing.idle },
        icon = { Glyphs.ModDraw },
        extra = Extra.DRAW
    ),
    /*
     * Gli Stili sono un modulo a parte per istruzione dell'utente (giro della `2.40`, punto 1), e
     * sono sempre l'ultimo a destra, anche dove la fila è riordinata (nota E sul giro della
     * `4.49`; vedi `stylesLast`).
     * Il 'Reset modulo' azzera i sei moduli che uno stile governa (Luce, Colore, HSL, Dettaglio,
     * Effetti, Curve): uno stile lascia i cursori dove li ha messi, quindi il tocco lungo vuol dire
     * 'togli l'aspetto'. La posa, il ritaglio e la geometria restano, perché dipendono dallo scatto.
     * Il punto d'accento non si accende, o direbbe che questo modulo ha toccato l'immagine per
     * quello che hanno fatto gli altri. Il glifo è di Material, provvisorio.
     */
    Module(
        PadKey.MOD_PRESET,
        R.string.look_presets,
        rows = { emptyList() },
        clear = {
            it.copy(
                light = Light.NONE, chroma = Chroma.NONE, mix = Mix.NONE,
                detail = Detail.NONE, effects = Effects.NONE, tone = Tone.NONE
            )
        },
        spent = { false },
        icon = { Glyphs.Material.Style },
        extra = Extra.PRESETS
    )
)

/**
 * Il modulo che porta questa chiave.
 *
 * ⚠️ Lancia se la chiave non è di un modulo, e non risponde `null`: i chiamanti sono i due `when`
 * di `ActionPad.kt`, che ci arrivano dai soli rami delle chiavi dei moduli, e la fila, che riceve
 * un ordine già ripulito da `padOrderOf`. Che i due elenchi si coprano lo misura il banco.
 */
private fun moduleOf(key: PadKey): Module = MODULES.first { it.key == key }

/**
 * Se uscendo si perderebbe del lavoro di sviluppo: un modulo diverso dal Ritaglio ha toccato
 * l'immagine. È la condizione dell'avviso all'uscita (vedi la nota su `leave`).
 *
 * La risposta la dà la tabella, con la domanda del punto d'accento ([Module.spent]): un modulo
 * nuovo entra qui da sé. Gli Stili non rispondono mai di sì: uno stile lascia i suoi valori nei
 * moduli che governa, ed è là che l'avviso li trova.
 */
internal fun developed(look: Look): Boolean =
    MODULES.any { it.extra != Extra.CROP && it.spent(look) }

/**
 * Come si chiama un modulo, per chi lo deve nominare fuori dall'editor.
 *
 * La fonte è la tabella: la pagina che riordina i riquadri, la ricerca delle impostazioni e la
 * fila vera dicono tutti questa parola.
 */
@StringRes
internal fun modName(key: PadKey): Int = moduleOf(key).name

/** Il glifo di un modulo, dalla stessa fonte del nome: vedi [modName]. */
@Composable
internal fun modGlyph(key: PadKey): ImageVector = moduleOf(key).icon()

/**
 * Il posto di un modulo nella tabella, cioè quello che [Gaze.module] tiene.
 *
 * L'ordine scelto cambia solo come i gettoni si vedono: con un indice legato alla fila, spostare
 * un gettone cambierebbe il modulo aperto.
 */
private fun modIndex(key: PadKey): Int = MODULES.indexOfFirst { it.key == key }

/**
 * Quante colonne ha la fila della posa, cioè quanti sono i suoi tasti.
 *
 * È un numero e non un conto sull'elenco, che si compone dentro la scheda coi suoi chiamanti.
 */
private const val POSE_KEYS = 5

/**
 * Quanti gettoni di modulo entrano in uno schermo prima che la fila debba scorrere.
 *
 * Oltre questo numero la fila scorre invece di stringere i gettoni, come ha chiesto l'utente (giro
 * della `2.40`). Con meno moduli in scena la cella si divide la larghezza.
 */
private const val MOD_FIT = 7

/**
 * L'aria fra due gettoni della fila dei moduli.
 *
 * La legge anche il velo del mini onboarding, che ricopia quella fila: vedi [modCell].
 */
internal val MOD_GAP = 4.dp

/**
 * Quanto è larga la cella di un gettone, data la larghezza [width] e quanti moduli sono in scena.
 *
 * La leggono la fila vera e il mini onboarding che la ricopia sopra il velo: con due conti
 * divergerebbe per primo quello del velo, che nessuno rimisura.
 */
internal fun modCell(width: Dp, count: Int): Dp {
    val quanti = minOf(count, MOD_FIT).coerceAtLeast(1)
    return (width - MOD_GAP * (quanti - 1)) / quanti
}

/**
 * Quanto è alta la riga dei quattro comandi del ritaglio: 'Indietro', 'Avanti', 'Applica' e
 * 'Azzera'.
 *
 * ⚠️ **È la misura di riga della scheda e non quella di un tasto, ed è misurata**: il corpo del
 * Ritaglio aveva 30 punti liberi prima di superare le Curve (Ritaglio 208 contro Curve 238; oggi
 * il più alto è il Disegno, vedi [DIAL_ROW]). Una cella di [ActionPad] ne chiede 64 e un
 * `IconButton` 48: tutti e due avrebbero alzato la scheda in tutti i moduli. Il bersaglio resta largo, perché le
 * quattro celle si dividono la larghezza; l'altezza è sotto i 48 come la riga di un cursore
 * ([DIAL_ROW]).
 * Ai tre comandi chiesti dall'utente si aggiunge 'Applica': applicare e disfare sono lo stesso
 * gesto in due versi, e vanno vicini.
 */
private val CROP_CMD_ROW = 32.dp

/*
 * ⚠️ Niente aria sotto i quattro comandi del Ritaglio (voce `crop-alti` non approvata): l'avanzo
 * che [Breathe] le cedeva mancava agli altri vani, e il corpo si vedeva ammucchiato. Il banco non
 * vede questa classe di difetti (`Rules.md` § '🧪 Quando si scrive una prova, e quando no').
 */

/** Quanto è grande il glifo di un comando del ritaglio: la misura dei glifi di comando dell'app. */
private val CROP_CMD_ICON = 24.dp

/** Lo stondamento della cella di un comando del ritaglio, cioè quello dei chip di questa scheda. */
private val CROP_CMD_ROUND = 8.dp

/**
 * Quanto distano due comandi del ritaglio (punto `crop-giu` dell'utente). Non costa altezza: si
 * stringono le quattro celle, che restano più larghe di un `IconButton`.
 */
private val CROP_CMD_GAP = 12.dp

/** L'aria sopra la fila delle forme, lontano dai gettoni dei moduli (punto `crop-giu`). */
private val CROP_TOP_AIR = 8.dp

/** L'aria del separatore sfumato del Ritaglio, sopra e sotto: vedi `CropRule`. */
private val CROP_RULE_AIR = 10.dp

/** La forma scelta nel Ritaglio, cioè l'indice di [Gaze.shape] riportato al suo valore. */
private fun cropShape(gaze: Gaze): Shape =
    Shape.entries.getOrElse(gaze.shape) { Shape.FREE }

/**
 * Il verso della forma scelta: vedi [cropShape]. Col verso da decidere risponde 'Verticale', che
 * nessuno vede: succede solo finché l'anteprima non è arrivata, coi gettoni del Ritaglio spenti.
 */
private fun cropLay(gaze: Gaze): Lay = Lay.entries.getOrElse(gaze.lay) { Lay.TALL }

/** Il verso del ritaglio quando l'anteprima non è ancora arrivata: vedi [Gaze.lay]. */
private const val LAY_UNSET = -1

/**
 * Quante volte l'immagine **già posata** è più larga che alta.
 *
 * ⚠️ **Si conta sulla posa e non sul file**: un quarto di giro scambia i lati, e il 16:9 sarebbe
 * calcolato sull'aspetto di prima. È la correzione che l'editor semplice fa dentro `onTurn`.
 * La leggono la scheda e il palco, che tiene la forma mentre il dito trascina una squadretta.
 */
private fun posedAspect(src: Bitmap?, spin: Spin): Float {
    val b = src ?: return 1f
    val dritto = b.width.toFloat() / b.height
    return if (spin.turns % 2 == 0) dritto else 1f / dritto
}

/**
 * La posa [gesto] applicata a [look], **col rettangolo che la segue**.
 *
 * ⚠️ **Il ritaglio è in frazioni dell'immagine già posata, quindi girando va riscritto**, o
 * taglierebbe un'altra porzione di immagine senza nessun errore. Lo riscrive [spunRect], la stessa
 * funzione dell'editor semplice.
 * Questa funzione non rifà la cornice: la scheda passa da [posedLook], che con una proporzione la
 * rifà nel verso scelto, come nell'editor semplice.
 */
internal fun spunLook(look: Look, gesto: Spin): Look = look.copy(
    spin = look.spin.then(gesto),
    crop = spunRect(look.crop, gesto),
    // ⚠️ Anche i tagli applicati, che vivono nello stesso spazio di [Look.crop]: senza, il palco
    // inquadrerebbe un'altra porzione di immagine (vedi [Framing.spun]).
    framing = look.framing.spun(gesto)
)

/**
 * La posa [gesto] applicata a [look], con la cornice che la segue: vedi [spunLook].
 *
 * Con una proporzione un quarto di giro rifà la cornice nel verso scelto, grande e centrata dentro
 * la porzione inquadrata (risposta `casa` dell'utente a `d-giro-forme`): gettone e cornice dicono
 * sempre la stessa cosa, come nell'editor semplice dentro `onTurn`. Girando con l'immagine, un
 * 16:9 diventava 9:16 mentre il gettone diceva ancora 16:9. 'Libero', 'Originale' e uno specchio
 * non rifanno niente.
 *
 * [wide] è quante volte l'immagine intera è più larga che alta **dopo** il gesto.
 */
internal fun posedLook(look: Look, gesto: Spin, shape: Shape, lay: Lay, wide: Float): Look {
    val girato = spunLook(look, gesto)
    if (!shape.numeric || gesto.turns % 2 == 0) return girato
    return girato.copy(crop = framedCrop(girato, wide) { _, quanto -> shape.fit(quanto, lay) })
}

/**
 * Quante volte la porzione che il Ritaglio inquadra è più larga che alta, sapendo quante volte lo è
 * l'immagine intera già posata ([wide]).
 *
 * ⚠️ **I comandi del Ritaglio lavorano dentro la porzione**, come le squadrette: sull'immagine
 * intera 'Centra in orizzontale' portava la cornice fuori dalla porzione (misurato sul banco), e
 * il file salvato avrebbe contenuto una striscia che il palco non mostrava. Così 'Originale' dentro
 * una porzione è la porzione, come nell'editor semplice dopo 'Applica'. Senza niente applicato è
 * l'aspetto dell'immagine.
 */
internal fun frameAspect(look: Look, wide: Float): Float {
    val porzione = look.framing.shown ?: return wide
    val w = porzione.right - porzione.left
    val h = porzione.bottom - porzione.top
    return if (w > 0f && h > 0f) wide * w / h else wide
}

/**
 * Il rettangolo di [look] dopo [op], che lo riceve **in frazioni della porzione** inquadrata
 * insieme al rapporto di lei, e lo restituisce nello stesso spazio: vedi [frameAspect].
 *
 * Le due conversioni sono quelle del gesto ([relativeTo] e [absolute]): il dito e i comandi della
 * scheda parlano della stessa porzione.
 */
internal fun framedCrop(
    look: Look,
    wide: Float,
    op: (ImageEdit.Crop, Float) -> ImageEdit.Crop
): ImageEdit.Crop {
    val porzione = look.framing.shown
    return absolute(porzione, op(relativeTo(porzione, look.crop), frameAspect(look, wide)))
}

/**
 * Il modulo aperto di fabbrica, cioè il Ritaglio, per istruzione dell'utente. Si ricava
 * dall'elenco: chi sposta un modulo ritrova l'apertura giusta senza toccare altro.
 */
private val LOOK_FIRST = MODULES.indexOfFirst { it.name == R.string.look_crop }.coerceAtLeast(0)

/**
 * I nomi dei quattro canali delle curve, nell'ordine degli indici di [Tone].
 *
 * I tre colori riusano le stringhe delle fasce dell'HSL, che dicono le stesse parole.
 */
private val TONE_NAMES = listOf(
    R.string.look_tone_rgb,
    R.string.look_band_red,
    R.string.look_band_green,
    R.string.look_band_blue
)

/**
 * Dove si ha lo **sguardo** nella scheda: il modulo, la fascia dell'HSL, il canale delle curve, e
 * se il colore mirato è armato.
 *
 * Non è una proprietà dell'immagine, quindi non entra nella storia dei passi: 'Annulla' non
 * riporta un modulo o una fascia.
 *
 * Vive fuori dalla scheda perché lo legge anche il palco: il colore mirato deve sapere quale
 * modulo si guarda e, per le curve, quale canale. Sono stati di Compose, quindi un gesto che li
 * legge quando scrive vede il valore di adesso.
 */
private class Gaze(
    module: Int = LOOK_FIRST,
    band: Int = 0,
    channel: Int = Tone.WHOLE,
    shape: Int = 0,
    lay: Int = LAY_UNSET
) {
    var module by mutableIntStateOf(module)
    var band by mutableIntStateOf(band)
    var channel by mutableIntStateOf(channel)

    /**
     * La forma scelta nel Ritaglio e il suo verso, cioè i due gettoni dell'editor semplice.
     *
     * Vivono qui e non nel [Look] perché sono un comando: sull'immagine si salva il rettangolo, e
     * 'Annulla' non deve riportare indietro la scelta dei gettoni. Sono indici perché l'oggetto si
     * salva alla rotazione come lista di interi ([cropShape] e [cropLay] ricostruiscono i valori).
     * Il verso nasce [LAY_UNSET]: lo sa solo l'anteprima, che arriva dopo, e il caricamento lo
     * scrive qui (vedi [AdvancedEditorScreen]).
     */
    var shape by mutableIntStateOf(shape)
    var lay by mutableIntStateOf(lay)

    /**
     * Se il colore mirato è armato. Non si salva alla rotazione: è una modalità accesa per un
     * gesto, e ritrovarla accesa darebbe un palco che non risponde ai gesti di sempre.
     */
    var aiming by mutableStateOf(false)
    var liquifying by mutableStateOf(false)
    var rebuilding by mutableStateOf(false)
    var healing by mutableStateOf(false)
    var healingRadius by mutableFloatStateOf(Healing.START_RADIUS)
    var healingSizing by mutableStateOf(false)
    var selection by mutableStateOf(Healing.Selection.NONE)
    var healingBusy by mutableStateOf(false)
    var healingFailed by mutableStateOf(false)

    var brushRadius by mutableFloatStateOf(0.10f)
    var brushSizing by mutableStateOf(false)
    var brushStrength by mutableFloatStateOf(0.50f)

    /**
     * Se un dito è sul cursore 'Raddrizza': il palco disegna la griglia dei terzi, che sparisce
     * quando il dito lascia (nota E del giro della `4.36`). Come [brushSizing] è un gesto, e non
     * si salva alla rotazione.
     */
    var leveling by mutableStateOf(false)

    /*
     * Lo strumento del Disegno: forma, colore, spessore, tratteggio e riempimento. Come il pennello
     * di Fluidifica sono strumenti e non l'immagine: restano fuori dalla storia e non si salvano
     * alla rotazione. Di fabbrica, il rettangolo arrotondato tratteggiato (nota A dell'utente sul
     * giro della `4.49`).
     */
    var pen by mutableStateOf(Pen.RECT)
    var ink by mutableIntStateOf(Draw.INKS.first())
    var inkWidth by mutableFloatStateOf(Draw.WIDTH)
    var dashed by mutableStateOf(true)

    /**
     * Il colore del riempimento, senza alfa, o `null` per nessuno. Di fabbrica il colore e
     * l'opacità di [Draw.FILL], valori dell'utente: le forme nascono riempite.
     */
    var fillInk by mutableStateOf<Int?>(Draw.FILL or 0xFF000000.toInt())
    var fillAlpha by mutableFloatStateOf((Draw.FILL ushr 24) / 255f)

    /**
     * L'opacità del contorno, da 0,1 a 1 ('Traccia', risposta `S1` dell'utente); di fabbrica
     * [Draw.INK_ALPHA].
     */
    var inkAlpha by mutableFloatStateOf(Draw.INK_ALPHA)

    /**
     * Quale dei tre tasti 'Traccia', 'Riempimento' e 'Spessore' è scelto, cioè che cosa regola il
     * cursore (risposta `S1`): l'opacità del contorno, quella del riempimento, o lo spessore.
     */
    var target by mutableStateOf(DrawTarget.STROKE)

    /**
     * La luminosità dei colori del contorno e del riempimento, da -1 (più scuro) a 1 (più
     * chiaro), regolata tenendo premuto un tondo ([Draw.lit]; nota dell'utente su `4.45-02`,
     * risposte `L1a`, `L2a`, `L3a`).
     * ⚠️ Il tondo resta il colore scelto e lo scarto vive accanto: un tocco sul tondo ridà il suo
     * colore di base.
     */
    var inkLight by mutableFloatStateOf(Draw.INK_LIGHT)
    var fillLight by mutableFloatStateOf(0f)

    /**
     * La lunghezza in pixel del lato lungo dell'immagine sul palco: la scrive il palco, e il tasto
     * 'Spessore' la legge per mostrare lo spessore come sarà il tratto (nota su `4.49-01`). Zero
     * finché il palco non ha disegnato.
     */
    var viewLong by mutableFloatStateOf(0f)

    /**
     * Dove si trova il tondo rosso del Disegno, in coordinate della radice: il velo del modulo ne
     * disegna lì la copia (nota B sul giro della `4.60`). Vuoto finché il modulo non è disposto.
     */
    var redSpot by mutableStateOf(Rect.Zero)

    /** Il colore del contorno come si disegna, senza alfa: il tondo con la sua [inkLight]. */
    val litInk: Int get() = Draw.lit(ink, inkLight)

    /** Il colore del riempimento come si disegna, senza alfa: il tondo con la sua [fillLight]. */
    val litFill: Int? get() = fillInk?.let { Draw.lit(it, fillLight) }

    /**
     * Se un dito tiene il cursore 'Spessore': il palco mostra la punta alla sua misura vera (R1 del
     * giro della `4.40`). Come [brushSizing] è un gesto.
     */
    var inkSizing by mutableStateOf(false)

    /**
     * Quanto un pannello sfoca l'immagine sotto di sé (nota su `4.81-01`): come gli altri parametri
     * regola il pannello scelto, o il prossimo.
     */
    var blurAmount by mutableFloatStateOf(Draw.BLUR)

    /**
     * Il colore del pannello, opaco, uno di [Draw.PILL_INKS], o `null` per nessuno (nota su
     * `4.81-01`: la tavolozza della pastiglia al 20% di opacità, con 'nessuno'). Nessuno di fabbrica
     * è una lettura della sessione, dichiarata nella voce di collaudo: un pannello è prima di tutto
     * una sfocatura.
     */
    var panelInk by mutableStateOf<Int?>(null)

    /** Il testo che si sta scrivendo, o `null`: finché c'è, la schermata mostra la sua finestra. */
    var textAsk by mutableStateOf<TextAsk?>(null)

    /**
     * Richiede le parole dell'elemento [i] di [drawing], se ne ha: il tasto 'Testo' e il doppio
     * tocco su un testo, una pastiglia o un pannello.
     */
    fun askWords(drawing: Drawing, i: Int) {
        val segno = drawing.marks.getOrNull(i)?.takeIf { it.pen.written } ?: return
        textAsk = TextAsk(null, i, segno.words?.text.orEmpty())
    }

    /**
     * Il colore dell'immagine sotto il testo scelto, o dell'immagine intera: lo misura la
     * schermata ([Draw.under]), e gli sfondi che non se ne staccano sono spenti.
     */
    var under by mutableIntStateOf(0xFF808080.toInt())

    /**
     * I parametri propri del testo (G3): colore e luminosità, corpo, carattere, i tre stili e lo
     * sfondo. Sono separati da quelli della linea, così da un rettangolo rosso a un testo non si
     * scrive in rosso, e viceversa (di fabbrica il testo è bianco: lettura `A4`).
     * ⚠️ Il testo si disegna opaco: l'opacità della linea non è la sua.
     */
    var textInk by mutableIntStateOf(Draw.TEXT_INK)
    var textLight by mutableFloatStateOf(0f)
    var textSize by mutableFloatStateOf(Draw.TEXT)
    var face by mutableStateOf(Face.ROBOTO)
    var bold by mutableStateOf(false)
    var italic by mutableStateOf(false)
    var strike by mutableStateOf(false)
    var label by mutableStateOf(false)
    var align by mutableStateOf(Align.CENTER)
    var labelInk by mutableIntStateOf(Draw.LABEL_INK)

    /** Il colore della pastiglia, opaco, uno di [Draw.PILL_INKS]: la pastiglia lo posa col suo alfa. */
    var pillInk by mutableIntStateOf(Draw.PILL_INKS.first())

    /** Le parole di un testo nuovo, o di quello scelto, con gli stili del modulo. */
    fun words(text: String): Words = Words(text, face, bold, italic, strike, label, labelInk, align)

    /**
     * Le parole di una pastiglia nuova, o di quella scelta: carattere, i tre stili e l'allineamento,
     * senza sfondo.
     */
    fun pillWords(text: String): Words = Words(text, face, bold, italic, strike, align = align)

    /**
     * L'interruttore 'Sfondo': la striscia dell'etichetta accesa o spenta. Accendendola, le parole
     * prendono un colore che si legge sopra ([Draw.wordsOn]).
     */
    fun switchLabel() {
        label = !label
        if (!label) return
        val parole = Draw.lit(textInk, textLight) or 0xFF000000.toInt()
        val nuove = Draw.wordsOn(labelInk, parole)
        if (nuove != parole) {
            textInk = nuove
            textLight = 0f
        }
    }

    /**
     * Lo strumento come elemento vuoto, pronto per il primo punto del dito.
     *
     * ⚠️ Gli strumenti che non chiudono una forma non hanno riempimento, così un elemento non
     * contiene mai un valore che niente disegna (nota dell'utente: la freccia ignora il
     * riempimento). Un testo ha colore e corpo suoi; le parole arrivano dalla finestra.
     */
    fun penMark(): Mark = if (pen == Pen.TEXT) Mark(
        pen, emptyList(), Draw.lit(textInk, textLight) or 0xFF000000.toInt(), textSize, false, null,
        tint = Tint(textInk, textLight, 1f, null, 0f, 1f), words = words("")
    ) else if (pen == Pen.PILL) Mark(
        // ⚠️ Una pastiglia ha un aspetto suo: niente spessore, parole senza sfondo, e il colore è il
        // riempimento, scelto fra i suoi.
        pen, emptyList(), Draw.PILL_WORDS, 0f, false, Draw.pillFill(pillInk), words = pillWords("")
    ) else if (pen == Pen.PANEL) Mark(
        // ⚠️ Un pannello: le parole della pastiglia, il suo colore al 20% o nessuno, e la sfocatura.
        pen, emptyList(), Draw.PILL_WORDS, 0f, false, panelInk?.let { Draw.panelFill(it) },
        blur = blurAmount, words = pillWords("")
    ) else Mark(
        pen, emptyList(), Draw.withAlpha(litInk, inkAlpha), inkWidth, dashed,
        fill = litFill?.takeIf { pen.closed }?.let { Draw.withAlpha(it, fillAlpha) },
        tint = tint()
    )

    /** Come sono scelti i colori adesso, che un elemento conserva per ricaricarli ([Tint]). */
    fun tint(): Tint = Tint(ink, inkLight, inkAlpha, fillInk, fillLight, fillAlpha)

    /**
     * L'indice dell'elemento scelto, o `null` (G2). È uno strumento e non l'immagine: resta fuori
     * dalla storia, e 'Annulla', 'Ripeti' e 'Originale' lo tolgono, perché dopo di loro l'indice
     * può puntare a un altro elemento.
     */
    var picked: Int?
        get() = pickedNow
        set(value) {
            // ⚠️ La modalità appartiene all'elemento scelto: una scelta nuova, o nessuna, riparte da
            // 'Trasforma', così le maniglie non ruotano mai un elemento che non lo chiedeva.
            if (value != pickedNow) turning = false
            pickedNow = value
        }
    private var pickedNow by mutableStateOf<Int?>(null)

    /**
     * Se le maniglie dell'elemento scelto lo ruotano ('Ruota') o lo deformano ('Trasforma'),
     * risposta dell'utente su `4.64-04`; trascinando l'elemento lo si sposta in tutte e due.
     * Torna a 'Trasforma' quando la scelta cambia ([picked]), e un tocco sull'elemento scelto
     * scambia le due modalità (nota su `4.70-04`).
     */
    var turning by mutableStateOf(false)

    /**
     * Lo stile copiato con 'Copia', come l'elemento da cui viene, o `null` (risposta su
     * `4.64-03`: 'Incolla' prende il posto di 'Copia', e il tocco lungo svuota la memoria). Se ne
     * legge solo lo stile ([Mark.styledLike]); resta finché l'editor è aperto, così uno stile va su
     * più elementi. È uno strumento, fuori dalla storia.
     */
    var styleClip by mutableStateOf<Mark?>(null)

    /**
     * Carica nel modulo i parametri di [mark], così i tondi, i cursori e i tasti li mostrano e li
     * modificano (G2). Restano anche quando la scelta si toglie: l'elemento successivo nasce come
     * l'ultimo scelto (scelta della sessione, dichiarata nella voce di collaudo).
     * ⚠️ Un elemento senza [Tint] (fatto da una prova) carica i colori come tondi senza scarto di
     * luminosità, e il riempimento solo per una forma chiusa, l'unica in cui vuol dire qualcosa.
     */
    fun load(mark: Mark) {
        pen = mark.pen
        if (mark.pen.framed) {
            // ⚠️ Una pastiglia carica carattere, stili e colore: il resto del suo aspetto è fisso.
            // Un pannello carica il colore, o nessuno, e la sfocatura.
            if (mark.pen == Pen.PILL) pillInk = mark.fill?.let { it or 0xFF000000.toInt() } ?: Draw.PILL_INKS.first()
            else {
                panelInk = mark.fill?.let { it or 0xFF000000.toInt() }
                blurAmount = mark.blur ?: Draw.BLUR
            }
            mark.words?.let { w ->
                face = w.face
                bold = w.bold
                italic = w.italic
                strike = w.strike
                align = w.align
            }
            return
        }
        if (mark.pen == Pen.TEXT) {
            // ⚠️ Un testo carica i parametri suoi, e lascia quelli della linea come erano.
            textSize = mark.width
            textInk = mark.tint?.ink ?: (mark.ink or 0xFF000000.toInt())
            textLight = mark.tint?.inkLight ?: 0f
            mark.words?.let { w ->
                face = w.face
                bold = w.bold
                italic = w.italic
                strike = w.strike
                align = w.align
                label = w.label
                if (w.label) labelInk = w.ground
            }
            return
        }
        inkWidth = mark.width
        dashed = mark.dashed
        val t = mark.tint ?: Tint(
            mark.ink or 0xFF000000.toInt(), 0f, (mark.ink ushr 24) / 255f,
            mark.fill?.let { it or 0xFF000000.toInt() }, 0f, mark.fill?.let { (it ushr 24) / 255f } ?: fillAlpha
        )
        ink = t.ink
        inkLight = t.inkLight
        inkAlpha = t.inkAlpha
        if (mark.pen.closed) {
            fillInk = t.fill
            fillLight = t.fillLight
            fillAlpha = t.fillAlpha
        }
    }

    /** [mark] coi parametri del modulo, tenendo il suo strumento e i suoi punti (G2). */
    fun restyle(mark: Mark): Mark = if (mark.pen == Pen.PILL) mark.copy(
        fill = Draw.pillFill(pillInk),
        words = pillWords(mark.words?.text.orEmpty())
    ) else if (mark.pen == Pen.PANEL) mark.copy(
        fill = panelInk?.let { Draw.panelFill(it) },
        blur = blurAmount,
        words = pillWords(mark.words?.text.orEmpty())
    ) else if (mark.pen == Pen.TEXT) mark.copy(
        ink = Draw.lit(textInk, textLight) or 0xFF000000.toInt(),
        width = textSize,
        tint = Tint(textInk, textLight, 1f, null, 0f, 1f),
        // ⚠️ La larghezza a cui le righe vanno a capo è dell'elemento, come le parole (`B3`).
        words = words(mark.words?.text.orEmpty()).copy(wrap = mark.words?.wrap ?: 0f)
    ) else mark.copy(
        ink = Draw.withAlpha(litInk, inkAlpha),
        width = inkWidth,
        dashed = dashed,
        fill = if (mark.pen.closed) litFill?.let { Draw.withAlpha(it, fillAlpha) } else null,
        tint = tint()
    )

    companion object {
        /**
         * Salva gli interi, con cui Compose porta lo stato oltre la chiusura del processo.
         * ⚠️ **Un valore nuovo si aggiunge in coda**: in mezzo cambierebbe il significato delle
         * posizioni già salvate, e dopo una rotazione si ritroverebbe un modulo al posto di una
         * fascia.
         */
        val Saver = listSaver<Gaze, Int>(
            save = { listOf(it.module, it.band, it.channel, it.shape, it.lay) },
            restore = { Gaze(it[0], it[1], it[2], it[3], it[4]) }
        )
    }
}

/**
 * Di che colore si disegna la curva del canale [channel].
 *
 * I tre colori sono scritti qui e non presi dal tema: sono il nome del canale, e dicono 'rosso',
 * 'verde' e 'blu' in tutti e due i temi, più chiari del colore puro perché un blu pieno su fondo
 * scuro non si distingue. Il composito, la curva di tutti i toni, ha l'accento dell'app.
 */
@Composable
private fun toneInk(channel: Int): Color = when (channel) {
    Tone.RED -> Color(0xFFEF5350)
    Tone.GREEN -> Color(0xFF66BB6A)
    Tone.BLUE -> Color(0xFF64B5F6)
    else -> MaterialTheme.colorScheme.primary
}

/**
 * La scheda in fondo: la fila dei moduli, i cursori di quello scelto e i tre comandi della storia.
 *
 * Un gettone dice anche se il suo modulo ha toccato l'immagine, col punto d'accento: i cursori di
 * un modulo chiuso non si vedono, e senza quel segno 'Originale' sembrerebbe l'unico modo per
 * tornare indietro.
 */
@Composable
private fun LookSheet(
    /** Se la scheda è a lato del canvas. Vedi [Adaptive.editorBeside]. */
    beside: Boolean = false,
    /** Se a lato, scheda a inizio riga (destri) o in fondo (mancini). */
    panelOnStart: Boolean = true,
    toolsWidth: Dp = 320.dp,
    onHealApply: () -> Unit,
    look: Look,
    busy: Boolean,
    ready: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    /**
     * L'anteprima da misurare quando si tocca 'Auto', o `null` finché non è pronta: il conto di
     * [Auto] è puro e vuole dei pixel, che ha la schermata.
     */
    origin: Bitmap?,
    /** Dove si ha lo sguardo: il modulo, la fascia, il canale, e se il mirato è armato. */
    gaze: Gaze,
    /**
     * Dov'è la fila dei gettoni, in coordinate della radice: serve al velo del mini onboarding,
     * che copre lo schermo e nasce fuori dalla scheda, e riceve il riquadro misurato.
     */
    onStrip: (Rect) -> Unit,
    /**
     * I comandi del salvataggio, nella barra in basso. È uno spazio da riempire: quei tasti
     * governano lo stato della schermata, che la scheda non guarda. Riceve il lato, che sa la
     * scheda.
     */
    toolCount: Int,
    tools: @Composable (mirror: Boolean) -> Unit,
    onLive: ((Look) -> Look) -> Unit,
    onSettled: () -> Unit,
    onPeek: (((Look) -> Look)?) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOriginal: () -> Unit
) {
    val module = gaze.module
    val chosen = MODULES[module]
    /*
     * Gli stili salvati vivono qui: li crea il comando sulla barra e li mostra il corpo, quindi lo
     * stato vive sopra tutti e due. Si leggono una volta, perché rileggere il file a ogni
     * ricomposizione vorrebbe dire aprirlo a ogni fotogramma di un'animazione.
     */
    val context = LocalContext.current
    var mine by remember { mutableStateOf(Presets.mine(context)) }
    /*
     * La superficie è quella dell'editor semplice, con le stesse ragioni misurate su
     * `EditorSheet`; a lato è una colonna ([editorSheetChrome]).
     */
    val chrome = editorSheetChrome(beside, panelOnStart, toolsWidth)
    Surface(
        modifier = chrome.modifier,
        shape = chrome.shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (beside) Modifier.fillMaxHeight().verticalScroll(rememberScrollState()) else Modifier)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        if (beside) {
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Top +
                                WindowInsetsSides.Bottom
                        } else {
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                        }
                    )
                )
                .padding(start = 16.dp, end = 16.dp, top = SHEET_TOP, bottom = 10.dp)
        ) {
            /*
             * Il tocco lungo su un gettone è il 'Reset modulo' chiesto dall'utente, anche sul
             * modulo che non si sta guardando: disfa quello che si è fatto altrove senza andarci.
             * L'ordine dei gettoni è quello scelto nelle impostazioni (sua richiesta), da
             * `LocalPadLook`; il modulo aperto resta un indice della tabella (vedi [modIndex]).
             * La cella si misura su [MOD_FIT] e non su quanti sono: oltre, la fila scorre invece di
             * stringere i gettoni, per istruzione dell'utente, e il mini onboarding lo dice.
             */
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                val cella = modCell(maxWidth, LocalPadLook.current.mods.size)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .onGloballyPositioned { onStrip(it.boundsInRoot()) },
                    horizontalArrangement = Arrangement.spacedBy(MOD_GAP)
                ) {
                    for (key in LocalPadLook.current.mods) {
                        val i = modIndex(key)
                        val mod = MODULES[i]
                        ModuleChip(
                            name = stringResource(mod.name),
                            icon = mod.icon(),
                            chosen = i == module,
                            spent = mod.spent(look),
                            enabled = ready && !busy,
                            onTap = { gaze.module = i },
                            onHold = {
                                if (mod.extra == Extra.HEALING) {
                                    gaze.selection = Healing.Selection.NONE
                                    gaze.healingFailed = false
                                }
                                // ⚠️ Il disegno si svuota, quindi nessun elemento resta scelto, come
                                // con 'Elimina tutto': un indice rimasto farebbe nascere scelto il
                                // prossimo elemento disegnato.
                                if (mod.extra == Extra.DRAW) gaze.picked = null
                                onLive(mod.clear)
                                onSettled()
                            },
                            modifier = Modifier.width(cella)
                        )
                    }
                }
            }

            /*
             * Il corpo è alto quanto il modulo più alto, per richiesta dell'utente: la scheda non
             * cambia altezza da un modulo all'altro, e con lei l'immagine sul palco.
             * La colonna dei nomi si misura qui, una volta per scheda: guarda i nomi di tutti i
             * moduli, e [SteadyBody] compone ogni corpo per misurarlo.
             */
            val nameWidth = knobNameWidth()
            SteadyBody(
                slots = MODULES.size,
                chosen = module,
                modifier = Modifier.fillMaxWidth(),
                // ⚠️ L'elenco degli stili resta fuori dalla misura e la riceve: vedi [Extra.PRESETS].
                measured = { MODULES[it].extra != Extra.PRESETS }
            ) { indice, comune ->
                ModuleBody(
                    onHealApply = onHealApply,
                    module = indice,
                    look = look,
                    gaze = gaze,
                    ready = ready,
                    busy = busy,
                    origin = origin,
                    nameWidth = nameWidth,
                    height = comune,
                    mine = mine,
                    onLive = onLive,
                    onSettled = onSettled,
                    onPeek = onPeek
                )
            }

            /*
             * La fila è di icone, con l'ordine dell'utente (giro della `2.31`) e i glifi che l'editor
             * semplice usa per gli stessi comandi.
             * Si specchia col lato del FAB ([fabEdge]), con l'eccezione dell'utente: 'Annulla' resta
             * sempre a sinistra di 'Ripristina', perché quei due sono un verso del tempo (vedi
             * [barOrder]). 'Salva stile' è dalla parte opposta e si specchia con loro, lettura
             * confermata dall'utente.
             */
            val mirror = fabEdge() == Alignment.Start
            val salva: @Composable () -> Unit = {
                /*
                 * C'è nel solo modulo che ne parla, e la condizione si legge dalla tabella dei
                 * moduli, come per 'Auto' e 'Mirato'.
                 */
                if (chosen.extra == Extra.PRESETS) {
                    PresetSaveButton(
                        look = look,
                        enabled = ready && !busy,
                        onSaved = { mine = it }
                    )
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                // La fila completa di Geometria si riduce solo quando non entra. Il conteggio
                // comprende soltanto i tasti presenti, così senza logo resta alla misura normale.
                val width = if (chosen.extra == Extra.CORNERS) {
                    ((maxWidth - TOOL_EDGE) / (toolCount + chosen.barKeys().size))
                        .coerceAtMost(TOOL_TOUCH)
                } else TOOL_TOUCH
                CompositionLocalProvider(
                    LocalEditorToolWidth provides width,
                    LocalMinimumInteractiveComponentSize provides
                        if (width < TOOL_TOUCH) 0.dp else LocalMinimumInteractiveComponentSize.current
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        /*
                         * Lo spazio elastico in mezzo fa tutto l'allineamento. I tasti del
                         * salvataggio sono al bordo e 'Salva stile' verso il centro: c'è nel solo
                         * modulo Stili, e messo per primo sposterebbe gli altri a ogni cambio di
                         * modulo.
                         */
                        if (!mirror) tools(false)
                        if (mirror) Comandi(
                            look = look,
                            chosen = chosen,
                            gaze = gaze,
                            origin = origin,
                            ready = ready,
                            busy = busy,
                            canUndo = canUndo,
                            canRedo = canRedo,
                            mirror = true,
                            onLive = onLive,
                            onSettled = onSettled,
                            onUndo = onUndo,
                            onRedo = onRedo,
                            onOriginal = onOriginal
                        ) else salva()
                        Spacer(modifier = Modifier.weight(1f))
                        if (mirror) salva() else Comandi(
                            look = look,
                            chosen = chosen,
                            gaze = gaze,
                            origin = origin,
                            ready = ready,
                            busy = busy,
                            canUndo = canUndo,
                            canRedo = canRedo,
                            mirror = false,
                            onLive = onLive,
                            onSettled = onSettled,
                            onUndo = onUndo,
                            onRedo = onRedo,
                            onOriginal = onOriginal
                        )
                        if (mirror) tools(true)
                    }
                }
            }
        }
    }

}

/** Le icone della barra bassa dell'editor completo, nell'ordine in cui si disegnano col FAB a destra. */
internal enum class Bar { AIM, LIQUIFY, HEAL, AUTO, UNDO, REDO, ORIGINAL }

/**
 * L'ordine in cui la barra bassa disegna le sue icone, dato il lato del FAB.
 *
 * ⚠️ **Lo specchio si ferma davanti alla coppia del tempo**, per eccezione dell'utente: 'Annulla'
 * resta sempre a sinistra di 'Ripristina', o il dito che cerca l'uno troverebbe l'altro. Le altre
 * icone si specchiano tutte. È una funzione pura perché il banco la prova chiamandola.
 */
internal fun barOrder(keys: List<Bar>, mirror: Boolean): List<Bar> {
    if (!mirror) return keys
    val fila = keys.reversed().toMutableList()
    val undo = fila.indexOf(Bar.UNDO)
    val redo = fila.indexOf(Bar.REDO)
    if (undo >= 0 && redo >= 0 && redo < undo) {
        fila[redo] = Bar.UNDO
        fila[undo] = Bar.REDO
    }
    return fila
}

/** Un solo elenco governa sia l'ordine sia lo spazio necessario alla barra. */
private fun Module.barKeys(): List<Bar> = buildList {
    if (extra == Extra.BANDS || extra == Extra.CORNERS) add(Bar.AIM)
    if (extra == Extra.CORNERS) add(Bar.LIQUIFY)
    if (extra == Extra.HEALING) add(Bar.HEAL)
    if (auto) add(Bar.AUTO)
    add(Bar.UNDO)
    add(Bar.REDO)
    add(Bar.ORIGINAL)
}

/**
 * Le icone dei comandi della barra bassa, nell'ordine che [barOrder] detta.
 *
 * Sono un pezzo a sé perché la fila si compone in due posti, uno per lato: due copie
 * divergerebbero al primo comando nuovo.
 */
@Composable
private fun Comandi(
    look: Look,
    chosen: Module,
    gaze: Gaze,
    origin: Bitmap?,
    ready: Boolean,
    busy: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    mirror: Boolean,
    onLive: ((Look) -> Look) -> Unit,
    onSettled: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOriginal: () -> Unit
) {
    val chiavi = chosen.barKeys()
    val width = LocalEditorToolWidth.current
    val button = if (width < TOOL_TOUCH) Modifier.width(width) else Modifier
    val glyph = Modifier.size(width / 2)
    val sorgente = origin
    for (voce in barOrder(chiavi, mirror)) when (voce) {
        /*
         * Il tasto arma il dito sull'immagine: nell'HSL sceglie la fascia del colore toccato, nella
         * Geometria tira uno dei quattro angoli. Il gesto lo decide la tabella dei moduli, e il
         * glifo e il nome cambiano con lui. Il nome resta la descrizione, che il lettore di schermo
         * annuncia e il banco cerca; un `IconButton` non ha uno stato scelto, quindi da armato il
         * glifo prende l'accento.
         */
        Bar.AIM -> IconButton(
            modifier = button,
            onClick = {
                gaze.liquifying = false
                gaze.aiming = !gaze.aiming
            },
            enabled = ready && !busy
        ) {
            Icon(
                modifier = glyph,
                imageVector = if (chosen.extra == Extra.CORNERS) {
                    Glyphs.Material.Transform
                } else {
                    Glyphs.Aim
                },
                contentDescription = stringResource(
                    if (chosen.extra == Extra.CORNERS) {
                        R.string.look_corners
                    } else {
                        R.string.look_target
                    }
                ),
                tint = if (gaze.aiming) {
                    MaterialTheme.colorScheme.primary
                } else {
                    LocalContentColor.current
                }
            )
        }
        Bar.HEAL -> IconButton(
            modifier = button,
            onClick = { gaze.healing = !gaze.healing },
            enabled = ready && !busy
        ) {
            Icon(Glyphs.Heal, stringResource(R.string.look_heal), modifier = glyph,
                tint = if (gaze.healing) MaterialTheme.colorScheme.primary else LocalContentColor.current)
        }
        Bar.LIQUIFY -> IconButton(
            modifier = button,
            onClick = {
                gaze.aiming = false
                gaze.liquifying = !gaze.liquifying
            },
            // Nessun controllo di versione: l'editor completo esiste solo da Android 13.
            enabled = ready && !busy
        ) {
            Icon(
                Glyphs.Liquify,
                stringResource(R.string.look_liquify),
                modifier = glyph,
                tint = if (gaze.liquifying) MaterialTheme.colorScheme.primary
                else LocalContentColor.current
            )
        }
        /*
         * 'Auto' scrive solo nei cursori, quindi è annullabile come chiede l'utente: il risultato è
         * un [Look] che entra nella storia. Il conto vive in [Auto]. Legge l'anteprima e non il
         * file: percentili e medie valgono uguali su una riduzione. Compare solo dove tocca
         * qualcosa ([Module.auto]).
         */
        Bar.AUTO -> IconButton(
            modifier = button,
            onClick = {
                if (sorgente != null) {
                    onLive { Auto.tuned(it, Auto.probe(sorgente)) }
                    onSettled()
                }
            },
            enabled = ready && !busy && sorgente != null
        ) {
            Icon(Glyphs.Auto, stringResource(R.string.look_auto), modifier = glyph)
        }
        Bar.UNDO -> IconButton(modifier = button, onClick = onUndo, enabled = canUndo && !busy) {
            Icon(Glyphs.EditUndo, stringResource(R.string.editor_undo), modifier = glyph)
        }
        Bar.REDO -> IconButton(modifier = button, onClick = onRedo, enabled = canRedo && !busy) {
            Icon(Glyphs.EditRedo, stringResource(R.string.editor_redo), modifier = glyph)
        }
        Bar.ORIGINAL -> IconButton(modifier = button, onClick = onOriginal, enabled = !look.idle && !busy) {
            Icon(Glyphs.EditReset, stringResource(R.string.editor_original), modifier = glyph)
        }
    }
}

/**
 * Il menu di un elemento del Disegno, che si apre tenendo fermo l'elemento (nota E sul giro della
 * `4.60` e risposta su `4.64-03`: lo stile del menu sulla foto nel visualizzatore, sei icone in
 * due righe e tre colonne).
 *
 * - L'ordine è quello della nota letto per righe: 'Sposta sopra', 'Copia', 'Duplica' sopra,
 *   'Sposta sotto', 'Ruota', 'Elimina' sotto (lettura dichiarata nella voce di collaudo).
 * - 'Copia' e 'Incolla' sono un posto solo: 'Copia' tiene lo stile ([Gaze.styleClip]), poi il
 *   posto dice 'Incolla' e lo posa sull'elemento scelto ([Mark.styledLike]) finché il tocco
 *   lungo svuota la memoria, con una notifica.
 * - 'Ruota' e 'Trasforma' sono un posto solo (risposta su `4.64-04`): che cosa fanno le maniglie.
 * - 'Sposta sopra' e 'Sposta sotto' scambiano l'elemento col vicino, l'ordine dei livelli
 *   semplificato chiesto dall'utente; in cima o in fondo il tasto è spento. Scelta e modalità
 *   seguono l'elemento.
 * - 'Duplica' posa una copia spostata di un passo, sopra l'originale, e la sceglie
 *   ([Draw.duplicateShift]).
 *
 * Il riquadro e la sua larghezza sono quelli del menu del visualizzatore ([ActionPad],
 * [PAD_MENU_WIDTH]). Ogni tasto chiude il menu prima di agire.
 */
@Composable
private fun DrawMenu(
    state: MenuState,
    gaze: Gaze,
    /** L'indice dell'elemento e il disegno di quando il menu si è aperto. */
    held: Pair<Int, Drawing>,
    w: Int,
    h: Int,
    /** Una modifica del disegno, che diventa un passo della storia. */
    onEdit: ((Drawing) -> Drawing) -> Unit,
    /** Sceglie l'elemento a questo indice caricandone i parametri, o toglie la scelta. */
    onPick: (Int?) -> Unit
) {
    val context = LocalContext.current
    // Le frasi degli avvisi da `LocalResources`, come nella griglia: il perché è là.
    val res = LocalResources.current
    val (i, prima) = held
    val segno = prima.marks.getOrNull(i) ?: return
    /** Sposta la scelta su [j] tenendo la modalità, perché l'elemento è lo stesso. */
    fun segui(j: Int) {
        val giro = gaze.turning
        onPick(j)
        gaze.turning = giro
    }
    MenuShell(state = state, position = MenuInWindow) {
        val sopra = PadAction(
            PadKey.DRAW_RAISE, Glyphs.Material.FlipToFront, R.string.draw_raise, enabled = i < prima.marks.size - 1
        ) {
            state.close()
            onEdit { it.swapping(i, i + 1) }
            segui(i + 1)
        }
        val copiato = gaze.styleClip
        val stile = if (copiato == null) PadAction(PadKey.DRAW_STYLE, Glyphs.Material.FormatPaint, R.string.draw_copy) {
            state.close()
            gaze.styleClip = segno
        } else PadAction(
            key = PadKey.DRAW_STYLE,
            icon = Glyphs.Material.ContentPaste,
            label = R.string.draw_paste,
            onHold = {
                state.close()
                gaze.styleClip = null
                Notices.say(res.getString(R.string.draw_style_cleared))
            },
            holdLabel = R.string.draw_paste_clear
        ) {
            state.close()
            /*
             * ⚠️ Lo stile passa dal modulo, come ogni modifica dell'elemento scelto: caricato, il
             * modulo lo posa e ne fa un passo della storia ([DrawBody]). Posato direttamente sul
             * disegno, il modulo poserebbe poi la sua lettura dello stesso stile, un secondo passo
             * che 'Annulla' dovrebbe disfare per niente.
             */
            gaze.load(segno.styledLike(copiato))
        }
        val doppio = PadAction(PadKey.DRAW_DUPLICATE, Glyphs.Material.ControlPointDuplicate, R.string.pick_duplicate) {
            state.close()
            onEdit { d -> d.marks.getOrNull(i)?.let { d.inserting(i + 1, it.moved(Draw.duplicateShift(w, h))) } ?: d }
            onPick(i + 1)
        }
        val sotto = PadAction(PadKey.DRAW_LOWER, Glyphs.Material.FlipToBack, R.string.draw_lower, enabled = i > 0) {
            state.close()
            onEdit { it.swapping(i, i - 1) }
            segui(i - 1)
        }
        val gira = if (gaze.turning) PadAction(PadKey.DRAW_TURN, Glyphs.Material.Transform, R.string.draw_transform) {
            state.close()
            gaze.turning = false
        } else PadAction(PadKey.DRAW_TURN, Glyphs.TurnRight, R.string.draw_rotate) {
            state.close()
            gaze.turning = true
        }
        val elimina = PadAction(PadKey.DELETE, Glyphs.PickDelete, R.string.pick_delete) {
            state.close()
            onPick(null)
            onEdit { it.without(i) }
        }
        Column(modifier = Modifier.width(PAD_MENU_WIDTH)) {
            ActionPad(actions = listOf(sopra, stile, doppio, sotto, gira, elimina))
        }
    }
}

/**
 * Il corpo del Disegno: gli strumenti, il bersaglio (contorno o riempimento), i colori, lo
 * spessore o l'opacità, il tratteggio ed 'Elimina tutto' (nome scelto dall'utente).
 *
 * Scrive nel [Gaze] e non nel [Look], tranne 'Elimina tutto': è lo strumento, fuori dalla storia.
 * Un elemento nuovo lo prende quando il dito si posa (`Gaze.penMark`), quindi cambiarlo non tocca
 * gli elementi già fatti, salvo quello scelto.
 * Il riempimento ha colore e opacità suoi (precisazione dell'utente a G1): colori e cursore sono
 * una fila sola, e i tasti in cima dicono a che cosa si applicano. Per il riempimento il primo
 * colore è 'Nessuno', e quello di fabbrica è [Draw.FILL]. 'Riempimento' si spegne per gli
 * strumenti che non chiudono una forma, come il 'Filtro BN'.
 */
@Composable
private fun DrawBody(
    look: Look,
    gaze: Gaze,
    live: Boolean,
    onLive: ((Look) -> Look) -> Unit,
    onSettled: () -> Unit
) {
    val chiusa = gaze.pen.closed
    // ⚠️ 'Riempimento' resta scelto quando lo strumento cambia, e per quelli che non chiudono una
    // forma vale come 'Traccia': un tasto che non cambia niente si legge come un guasto.
    val bersaglio = if (gaze.target == DrawTarget.FILL && !chiusa) DrawTarget.STROKE else gaze.target
    val riempimento = bersaglio == DrawTarget.FILL
    // ⚠️ Con lo strumento Testo la tavolozza e il cursore regolano il colore e il corpo del testo.
    val scrive = gaze.pen == Pen.TEXT
    // ⚠️ Una pastiglia ha un aspetto suo (nota A sul giro della `4.43`): il cursore è spento, e i
    // tondi mostrano i colori della pastiglia (nota su `4.90-05`).
    val pillola = gaze.pen == Pen.PILL
    /*
     * ⚠️ Il pannello (risposta su `4.81-01`: la sfocatura diventa uno strumento, una pastiglia con
     * un altro stile): i tasti del testo, i colori della pastiglia al 20% con 'Nessuno' per primo,
     * e il cursore della sfocatura.
     */
    val pannello = gaze.pen == Pen.PANEL
    val incornicia = pillola || pannello
    val tratto = live && !incornicia
    val tondi = live
    /*
     * Gli strumenti sono tasti come quelli sotto, col glifo più grande (nota B sul giro della
     * `4.90`): i chip di Material tengono 8dp per lato, e il glifo restava piccolo. Lo strumento
     * scelto ha il bordo dei tasti scelti, [KEY_RIM].
     */
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (pen in Pen.entries) {
            TextKey(
                stringResource(penName(pen)), selected = gaze.pen == pen, enabled = live, toggle = false, choice = true,
                // ⚠️ Lo strumento non è un parametro dell'elemento (G2): sceglierlo toglie la scelta.
                onClick = {
                    gaze.picked = null
                    gaze.pen = pen
                },
                modifier = Modifier.weight(1f)
            ) { tinta -> Icon(Glyphs.pen(pen), contentDescription = null, tint = tinta, modifier = Modifier.size(PEN_GLYPH)) }
        }
    }
    // ⚠️ Le colonne degli strumenti sopra, così i tasti si allineano (bozza dell'utente su
    // `4.43-01`). La fila finisce con 'Elimina' ed è ancorata a destra (risposta `B1`), quindi le
    // prime tre colonne sono vuote. 'Traccia', 'Spessore' e 'Riempimento' sono una scelta sola:
    // che cosa regola il cursore (risposta `S1`).
    val elimina: @Composable (Modifier) -> Unit = { m -> DeleteKey(look, gaze, live, onLive, onSettled, m) }
    if (scrive || incornicia) TextKeys(look, gaze, live, pill = incornicia, delete = elimina) else Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { Spacer(Modifier.weight(1f)) }
        ArtKey(
            KeyKind.DASH, gaze, selected = gaze.dashed, enabled = tratto, name = R.string.draw_dashed,
            toggle = true, onClick = { gaze.dashed = !gaze.dashed }, modifier = Modifier.weight(1f)
        )
        ArtKey(
            KeyKind.STROKE, gaze, selected = bersaglio == DrawTarget.STROKE, enabled = tratto,
            name = R.string.draw_outline, toggle = false,
            onClick = { gaze.target = DrawTarget.STROKE }, modifier = Modifier.weight(1f)
        )
        ArtKey(
            KeyKind.WIDTH, gaze, selected = bersaglio == DrawTarget.WIDTH, enabled = tratto,
            name = R.string.draw_width, toggle = false,
            onClick = { gaze.target = DrawTarget.WIDTH }, modifier = Modifier.weight(1f)
        )
        ArtKey(
            KeyKind.FILL, gaze, selected = riempimento, enabled = tratto && chiusa, name = R.string.draw_filled,
            toggle = false, onClick = { gaze.target = DrawTarget.FILL }, modifier = Modifier.weight(1f)
        )
        elimina(Modifier.weight(1f))
    }
    /*
     * ⚠️ **Con un elemento scelto, i parametri del modulo cambiano lui** (G2, specifica
     * dell'utente): ogni modifica si posa subito sull'elemento, ed entra nella storia quando il
     * modulo è fermo da [RESTYLE_SETTLE_MS], così un cursore trascinato è un passo solo.
     * Il primo valore è quello che [Gaze.load] ha appena scritto, e si salta. Una modifica arrivata
     * quando la scelta è passata a un altro elemento non è di questo: la scelta carica i valori nello
     * stesso passo, e questo raccoglitore li può vedere prima di essere annullato.
     */
    val scelto = gaze.picked
    LaunchedEffect(scelto) {
        if (scelto == null) return@LaunchedEffect
        snapshotFlow {
            listOf(
                gaze.ink, gaze.inkLight, gaze.inkAlpha, gaze.fillInk, gaze.fillLight, gaze.fillAlpha,
                gaze.inkWidth, gaze.dashed, gaze.blurAmount, gaze.panelInk,
                gaze.textInk, gaze.textLight, gaze.textSize, gaze.face, gaze.bold, gaze.italic,
                gaze.strike, gaze.label, gaze.labelInk, gaze.align, gaze.pillInk
            )
        }.drop(1).collectLatest {
            if (gaze.picked != scelto) return@collectLatest
            onLive { l ->
                l.drawing.marks.getOrNull(scelto)?.let { l.copy(drawing = l.drawing.replacing(scelto, gaze.restyle(it))) } ?: l
            }
            delay(RESTYLE_SETTLE_MS)
            onSettled()
        }
    }
    /*
     * ⚠️ La tavolozza regola il riempimento con 'Riempimento' scelto, e il contorno negli altri
     * casi, anche con 'Spessore', perché lo spessore è della linea (lettura della sessione,
     * dichiarata nella voce di collaudo).
     */
    fun luce(): Float = if (scrive) gaze.textLight else if (riempimento) gaze.fillLight else gaze.inkLight
    fun posa(luce: Float) {
        if (scrive) gaze.textLight = luce else if (riempimento) gaze.fillLight = luce else gaze.inkLight = luce
    }
    fun scegli(ink: Int?) {
        if (pannello) {
            gaze.panelInk = ink
        } else if (pillola) {
            if (ink != null) gaze.pillInk = ink
        } else if (scrive) {
            if (ink != null) {
                gaze.textInk = ink
                gaze.textLight = 0f
            }
        } else if (riempimento) {
            gaze.fillInk = ink
            gaze.fillLight = 0f
        } else if (ink != null) {
            gaze.ink = ink
            gaze.inkLight = 0f
        }
    }
    val chiaro = rememberMenuState()
    val nomeLuce = stringResource(R.string.draw_light)
    val nomeSfocatura = stringResource(R.string.draw_blur)
    /** La pista del cursore 'Luminosità', dove si legge il dito che tiene un tondo (nota C). */
    val pista = remember { Placed() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    /*
     * ⚠️ **La luminosità è il punto del dito sulla pista** (nota C sul giro della `4.49`: il
     * cursore seguiva il dito da un lato solo): sommando il movimento alla luminosità di partenza,
     * da un tondo vicino a un capo il dito non aveva spazio per l'altro verso. Il cursore copre i
     * tondi e il pomello va sotto il dito, il gesto descritto per `4.49-02`.
     * I due punti sono sullo schermo perché il cursore vive nella finestra del menu. Material legge
     * un tocco da mezzo pomello dentro ogni capo, 2dp: l'unico scarto fra dito e pomello.
     */
    fun lucePer(x: Float): Float? {
        val binario = pista.at?.takeIf { it.isAttached } ?: return null
        val largo = binario.size.width.toFloat().takeIf { it > 0f } ?: return null
        val t = ((x - binario.localToScreen(Offset.Zero).x) / largo).coerceIn(0f, 1f)
        return (if (rtl) 1f - t else t) * 2f - 1f
    }
    // ⚠️ Dieci posti da 32dp fanno 320dp, più di quanto un telefono stretto dà al pannello: il
    // tondo si stringe per entrare e non supera mai [SWATCH].
    val voci: List<Int?> = listOf(null) + if (incornicia) Draw.PILL_INKS else Draw.INKS
    val sfondo = stringResource(R.string.draw_ground)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val lato = minOf(SWATCH, maxWidth / voci.size - SWATCH_GAP)
        Row(
            /*
             * ⚠️ I tondi sfumano mentre una superficie si apre sopra l'editor (nota F sul giro della
             * `4.49`): sotto la sfocatura i loro colori pieni si allargano oltre il bordo della
             * finestra. Seguono l'avanzamento del velo, come la scacchiera del visualizzatore, e lo
             * leggono mentre disegnano.
             */
            modifier = Modifier.fillMaxWidth().graphicsLayer {
                alpha = 1f - veilProgress()
            },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bordo = MaterialTheme.colorScheme.onSurface
            val filo = MaterialTheme.colorScheme.outline
            // ⚠️ La fila del riempimento comincia con 'Nessuno'; quella del contorno lascia lì un
            // posto vuoto, così i colori non si spostano sotto il dito quando cambia il bersaglio.
            val nessuno = stringResource(R.string.settings_colour_none)
            voci.forEachIndexed { i, ink ->
                val scelto = when {
                    pannello -> gaze.panelInk == ink
                    pillola -> ink != null && gaze.pillInk == ink
                    scrive -> ink != null && gaze.textInk == ink
                    riempimento -> gaze.fillInk == ink
                    else -> ink != null && gaze.ink == ink
                }
                // ⚠️ Anche la fila del pannello comincia con 'Nessuno', quella della pastiglia no.
                if (ink == null && !riempimento && !pannello) {
                    Spacer(Modifier.size(lato))
                    return@forEachIndexed
                }
                // ⚠️ I colori della pastiglia non hanno nomi: sono numerati, come le strisce dello sfondo.
                val nome = if (ink == null) nessuno else if (incornicia) "$sfondo $i" else stringResource(INK_NAMES[i - 1])
                /*
                 * ⚠️ **Tenere premuto un tondo apre 'Luminosità'** (nota su `4.45-02`, risposte
                 * `L1a`, `L2a`, `L3a`): dopo il tocco lungo di sistema il dito che scorre regola la
                 * luminosità e sollevandosi chiude il cursore; il dito che si solleva senza
                 * scorrere lascia il cursore in scena, e un tocco fuori lo chiude.
                 * Il dito che scorre posa la luminosità subito (nota su `4.61-04`): l'elemento
                 * scelto cambia colore sotto il dito.
                 * Il cursore è un menu, da [MenuShell]: un tocco fuori lo chiude senza arrivare al
                 * palco, dove disegnerebbe un punto. Tenere un tondo non scelto prima lo sceglie,
                 * dal suo colore; tenere quello scelto parte dalla luminosità che ha.
                 */
                fun tieni() {
                    if (ink == null || incornicia) return
                    if (!scelto) scegli(ink)
                    chiaro.open()
                }
                val qui = remember { Placed() }
                Box(
                    modifier = Modifier
                        .size(lato)
                        .onGloballyPositioned {
                            qui.at = it
                            if (!incornicia && ink == Draw.INKS.first()) gaze.redSpot = it.boundsInRoot()
                        }
                        .clip(CircleShape)
                        .pointerInput(ink, riempimento, tondi, incornicia) {
                            if (!tondi) return@pointerInput
                            awaitEachGesture {
                                val giu = awaitFirstDown()
                                val lungo = awaitLongPressOrCancellation(giu.id)
                                if (lungo == null) {
                                    val su = currentEvent.changes.firstOrNull { it.id == giu.id }
                                    if (su != null && !su.pressed && !su.isConsumed) {
                                        su.consume()
                                        scegli(ink)
                                    }
                                    return@awaitEachGesture
                                }
                                // ⚠️ I colori della pastiglia non hanno luminosità: tenerli non fa niente.
                                if (ink == null || incornicia) return@awaitEachGesture
                                tieni()
                                // ⚠️ Un dito che si solleva soltanto lascia la luminosità com'era
                                // (`L3a`): la pista comanda oltre la soglia di movimento.
                                var mosso = false
                                while (true) {
                                    val evento = awaitPointerEvent()
                                    val dito = evento.changes.firstOrNull { it.id == giu.id } ?: break
                                    if (!dito.pressed) {
                                        dito.consume()
                                        break
                                    }
                                    if (!mosso) mosso = abs(dito.position.x - lungo.position.x) > viewConfiguration.touchSlop
                                    if (mosso) qui.at?.takeIf { it.isAttached }?.localToScreen(dito.position)
                                        ?.let { lucePer(it.x) }?.let { posa(it) }
                                    dito.consume()
                                }
                                if (mosso) chiaro.close()
                            }
                        }
                        .semantics {
                            contentDescription = nome
                            role = Role.RadioButton
                            selected = scelto
                            if (!tondi) disabled()
                            onClick { scegli(ink); true }
                            if (ink != null && !incornicia) onLongClick(nomeLuce) { tieni(); true }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // ⚠️ Il filo sottile c'è sempre, o il bianco sul tema chiaro e il nero sul tema
                    // scuro non si vedrebbero; il cerchio pieno dice qual è scelto. 'Nessuno' è il
                    // cerchio vuoto con la diagonale.
                    Canvas(Modifier.size(lato)) {
                        val r = size.minDimension / 2f
                        val dentro = if (scelto) r - SWATCH_RING.toPx() else r - 4.dp.toPx()
                        if (scelto) drawCircle(bordo, radius = r)
                        if (ink != null) {
                            drawCircle(Color(ink), radius = dentro)
                        } else {
                            drawCircle(Color.White, radius = dentro)
                            val d = dentro * 0.7071f
                            drawLine(
                                Color(Draw.INKS.first()), center + Offset(-d, d), center + Offset(d, -d),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                        drawCircle(filo, radius = dentro, style = Stroke(1.dp.toPx()))
                    }
                }
            }
        }
        /*
         * ⚠️ **Il cursore copre la fila dei tondi** (nota su `4.49-02`: sopra i tondi come un
         * livello sovrapposto, così il dito è davvero sul cursore), largo quanto la fila e
         * centrato. Ha un [MenuSpot] suo e non [rememberMenuSpot], che sposta verso l'interno un
         * pannello vicino ai bordi: questo deve restare esattamente sulla fila.
         */
        val sopra = remember { MenuSpot(MenuSide.ANCHOR_CENTRE, MenuSide.ANCHOR_CENTRE) }
        /*
         * ⚠️ **Dietro questo cursore niente velo né sfocatura, ma l'ombra** (nota D sul giro della
         * `4.60`): l'elemento scelto e i tasti cambiano colore mentre il dito si muove, e devono
         * vedersi. È la scelta 'Ombra' delle impostazioni ([MenuShell], `Edge.kt`), data a questo
         * solo menu.
         */
        CompositionLocalProvider(LocalAivDepth provides PanelDepth.SHADOW) {
            MenuShell(state = chiaro, position = sopra, minWidth = maxWidth) {
                val base = (if (scrive) gaze.textInk else if (riempimento) gaze.fillInk else gaze.ink) ?: Draw.INK
                val valore = luce()
                Row(
                    modifier = Modifier.width(maxWidth).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ⚠️ Il colore come sarà disegnato, che il tondo non mostra: il tondo tiene il
                    // colore di base, per richiesta dell'utente.
                    Canvas(Modifier.size(SWATCH)) { drawCircle(Color(Draw.lit(base, valore))) }
                    ValueSlider(
                        value = valore,
                        onValueChange = { posa(it) },
                        valueRange = -1f..1f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                            .onGloballyPositioned { pista.at = it }
                            .semantics { contentDescription = nomeLuce }
                    )
                }
            }
        }
    }
    // ⚠️ Il nome del cursore e il cursore sono una riga sola del modulo: niente aria fra loro.
    // 'Elimina' ed 'Elimina tutto' sono un tasto solo, l'ultimo della fila sopra ([DeleteKey]).
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(
                when {
                    pannello -> R.string.draw_blur
                    scrive || pillola -> R.string.draw_size
                    bersaglio == DrawTarget.WIDTH -> R.string.draw_width
                    else -> R.string.settings_mark_alpha
                }
            ),
            style = MaterialTheme.typography.labelMedium
        )
        if (pannello) ValueSlider(
            value = gaze.blurAmount,
            onValueChange = { gaze.blurAmount = it },
            valueRange = Draw.BLUR_MIN..Draw.BLUR_MAX,
            enabled = live,
            modifier = Modifier.semantics { contentDescription = nomeSfocatura }
        ) else if (scrive || pillola) {
            val nomeDimensione = stringResource(R.string.draw_size)
            // ⚠️ Le parole di una pastiglia prendono il corpo più grande che il riquadro contiene,
            // quindi il cursore è spento. La pista è quella di un rapporto ([Draw.textTrack]): i
            // corpi arrivano a 1,5. Un testo nuovo nasce al suo corpo ([Draw.fitSize]), quindi il
            // cursore regola solo il testo scelto, ed è spento senza.
            ValueSlider(
                value = Draw.textTrack(gaze.textSize),
                onValueChange = { gaze.textSize = Draw.textSize(it) },
                enabled = live && scrive && gaze.picked != null,
                modifier = Modifier.semantics { contentDescription = nomeDimensione }
            )
        } else when (bersaglio) {
            DrawTarget.STROKE -> ValueSlider(
                value = gaze.inkAlpha,
                onValueChange = { gaze.inkAlpha = it },
                valueRange = 0.1f..1f,
                enabled = live
            )
            DrawTarget.FILL -> ValueSlider(
                value = gaze.fillAlpha,
                onValueChange = { gaze.fillAlpha = it },
                valueRange = 0.1f..1f,
                enabled = live && gaze.fillInk != null
            )
            DrawTarget.WIDTH -> {
                DisposableEffect(gaze) { onDispose { gaze.inkSizing = false } }
                ValueSlider(
                    value = gaze.inkWidth,
                    onValueChange = { gaze.inkSizing = true; gaze.inkWidth = it },
                    onValueChangeFinished = { gaze.inkSizing = false },
                    valueRange = Draw.WIDTH_MIN..Draw.WIDTH_MAX,
                    enabled = live
                )
            }
        }
    }
    /*
     * ⚠️ **La fila delle strisce ha il suo posto con ogni strumento** (nota E sul giro della
     * `4.91`): la scheda è alta quanto il modulo più alto ([SteadyBody]), e la fila comparsa con
     * 'Sfondo' toglieva l'aria fra le file di tasti. Il posto c'è sempre, vuoto senza strisce,
     * così il cursore non si sposta da uno strumento all'altro.
     */
    Box(Modifier.fillMaxWidth().height(STRIP), contentAlignment = Alignment.Center) {
        if (scrive && gaze.label) GroundRow(gaze, live)
    }
}

/** Il nome di uno strumento di disegno, che il lettore di schermo annuncia al posto del suo glifo. */
private fun penName(pen: Pen): Int = when (pen) {
    Pen.FREE -> R.string.draw_free
    Pen.LINE -> R.string.draw_line
    Pen.ARROW -> R.string.draw_arrow
    Pen.RECT -> R.string.draw_rect
    Pen.ELLIPSE -> R.string.draw_ellipse
    Pen.TEXT -> R.string.draw_text
    Pen.PILL -> R.string.draw_pill
    Pen.PANEL -> R.string.draw_panel
}

/**
 * Dove un nodo è stato disposto l'ultima volta, letto quando un gesto lo chiede: il cursore
 * 'Luminosità' trasforma il dito su un tondo in un punto della sua pista, che vive nella finestra
 * del menu.
 * ⚠️ Non è uno stato: le coordinate si leggono all'evento, quindi scriverle non ricompone niente.
 */
private class Placed {
    var at: LayoutCoordinates? = null
}

/** I quattro tasti sopra la tavolozza, disegnati invece che scritti (`ArtKey`). */
internal enum class KeyKind { STROKE, FILL, DASH, WIDTH }

/** Quanto colore tengono le strisce che non si staccherebbero: quanto un tasto spento. */
private const val OFF_SWATCHES = OFF_INK

/** Quanto il modulo aspetta fermo prima che una modifica all'elemento scelto entri nella storia. */
private const val RESTYLE_SETTLE_MS = 400L

/** Che cosa regola il cursore del Disegno, scelto con 'Traccia', 'Riempimento' o 'Spessore'. */
internal enum class DrawTarget { STROKE, FILL, WIDTH }

/**
 * L'opacità che il tasto 'Riempimento' mostra per un riempimento di opacità [alpha]: quella vera
 * alzata in proporzione, mai sotto [KEY_ALPHA_MIN], e un riempimento pieno resta pieno (voce
 * `4.42-01`). Il 20% di fabbrica si vede al 52%, il minimo del cursore (10%) al 46%.
 */
internal fun keyAlpha(alpha: Float): Float = KEY_ALPHA_MIN + (1f - KEY_ALPHA_MIN) * alpha.coerceIn(0f, 1f)

/** L'opacità minima che il tasto 'Riempimento' mostra, valore dell'utente. */
internal const val KEY_ALPHA_MIN = 0.4f

/** Il grigio del tasto 'Tratteggio': il suo 'grigio scuro', uguale nei due temi. */
private val DASH_GREY = Color(0xFF616161)

/**
 * Quanto di [DASH_GREY] il tasto 'Tratteggio' mostra da spento (risposta `B2` dell'utente alla
 * nota B sul giro della `4.49`: spento, il tratteggio grigio sbiadito).
 */
private const val DASH_OFF = 0.35f

/**
 * I tasti 'Tratteggio', 'Traccia' e 'Riempimento', disegnati invece che scritti (note
 * dell'utente su `4.43-01` e sua bozza).
 * - 'Traccia' (`draw_outline`): una banda piena del colore corrente, di uno spessore, da bordo a
 *   bordo del tasto.
 * - 'Tratteggio': la stessa banda, tratteggiata; il tasto si accende e si spegne. Acceso, il
 *   tratteggio ha il colore del contorno; spento, il grigio scuro sbiadito a [DASH_OFF] (risposta
 *   `B2`).
 * - 'Riempimento': un rettangolo stondato della forma del tasto, rientrato di [KEY_GAP], su una
 *   scacchiera, nel colore del riempimento a [keyAlpha]. Con 'Nessuno' è la scacchiera vuota con
 *   la diagonale rossa del tondo 'Nessuno'.
 *
 * ⚠️ **Un tasto suo e non un `FilterChip`**: un chip tiene l'etichetta lontana dai bordi, e il
 * disegno dell'utente arriva fino a loro. Il tasto è [TextKey], con il disegno al posto della
 * parola, così la fila si legge come gli strumenti sopra. La parola resta la descrizione per il
 * lettore di schermo.
 */
@Composable
private fun ArtKey(
    kind: KeyKind,
    gaze: Gaze,
    selected: Boolean,
    enabled: Boolean,
    name: Int,
    toggle: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    TextKey(
        name = stringResource(name), selected = selected, enabled = enabled, toggle = toggle,
        onClick = onClick, modifier = modifier, choice = true
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val banda = KEY_BAND.toPx()
            val y = size.height / 2f
            when (kind) {
                KeyKind.STROKE -> drawRect(
                    Color(gaze.litInk).copy(alpha = keyAlpha(gaze.inkAlpha)),
                    Offset(0f, y - banda / 2f), Size(size.width, banda)
                )
                KeyKind.DASH -> {
                    val (tratto, vuoto) = keyDashes(size.width, KEY_DASH.toPx(), KEY_SPACE.toPx())
                    drawLine(
                        if (selected) Color(gaze.litInk) else DASH_GREY.copy(alpha = DASH_OFF),
                        Offset(0f, y), Offset(size.width, y), strokeWidth = banda,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(tratto, vuoto))
                    )
                }
                KeyKind.WIDTH -> {
                    /*
                     * ⚠️ **La linea è spessa quanto il tratto sullo schermo** (nota su `4.49-01`):
                     * lo spessore sul lato lungo dell'immagine come il palco la mostra
                     * ([Gaze.viewLong]), quindi segue l'ingrandimento. Mai sotto [KEY_WIDTH_MIN],
                     * che l'utente ammette per gli spessori minimi, né oltre il tasto, con [KEY_GAP]
                     * sopra e sotto.
                     * Le estremità tonde finiscono a [KEY_GAP] dai lati del tasto a ogni spessore
                     * (nota sul giro della `4.96`: vicino al bordo, senza toccarlo), lo stesso
                     * filo d'aria del rettangolo di 'Riempimento'.
                     */
                    val spesso = (gaze.inkWidth * gaze.viewLong)
                        .coerceIn(KEY_WIDTH_MIN.toPx(), size.height - 2f * KEY_GAP.toPx())
                    val margine = KEY_GAP.toPx() + spesso / 2f
                    drawLine(
                        Color(gaze.litInk), Offset(margine, y), Offset(size.width - margine, y),
                        strokeWidth = spesso, cap = StrokeCap.Round
                    )
                }
                KeyKind.FILL -> {
                    val g = KEY_GAP.toPx()
                    val angolo = (KEY_CORNER - KEY_GAP).toPx()
                    val dentro = RoundRect(
                        Rect(Offset(g, g), Size(size.width - 2 * g, size.height - 2 * g)), CornerRadius(angolo)
                    )
                    clipPath(Path().apply { addRoundRect(dentro) }) {
                        val lato = 4.dp.toPx()
                        drawRect(Color.White)
                        var r = 0
                        while (r * lato < size.height) {
                            var c = r % 2
                            while (c * lato < size.width) {
                                drawRect(CHECKER, Offset(c * lato, r * lato), Size(lato, lato))
                                c += 2
                            }
                            r++
                        }
                        val ink = gaze.litFill
                        if (ink != null) {
                            drawRect(Color(ink).copy(alpha = keyAlpha(gaze.fillAlpha)))
                        } else {
                            drawLine(
                                Color(Draw.INKS.first()), Offset(0f, size.height), Offset(size.width, 0f),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Il tratto e lo spazio del tasto 'Tratteggio', stirati perché la banda cominci e finisca con un
 * tratto (nota C sul giro della `4.60`). Il numero di tratti è il più vicino a [dash] e [space]
 * alla loro misura, e tutti e due crescono o calano dello stesso fattore, così il ritmo della
 * bozza dell'utente regge a ogni larghezza.
 */
internal fun keyDashes(width: Float, dash: Float, space: Float): Pair<Float, Float> {
    val n = ((width + space) / (dash + space)).roundToInt().coerceAtLeast(1)
    val k = width / (n * dash + (n - 1) * space)
    return dash * k to space * k
}

/** Lo spessore del bordo di un tasto scelto del Disegno, strumenti e tasti (nota A). */
private val KEY_RIM = 2.dp

/** La linea più sottile che il tasto 'Spessore' disegna, anche se il tratto sullo schermo è più sottile. */
private val KEY_WIDTH_MIN = 2.dp

/** Lo spessore delle bande di 'Traccia' e 'Tratteggio', uguale per tutte e due (nota 3 su `4.43-01`). */
private val KEY_BAND = 6.dp

/** Il tratteggio del tasto 'Tratteggio': tratto e spazio, misurati sulla bozza dell'utente. */
private val KEY_DASH = 10.dp
private val KEY_SPACE = 6.dp

/** Il tondo più grande della fila della tavolozza, e l'aria minima fra due tondi. */
private val SWATCH = 32.dp
private val SWATCH_GAP = 2.dp

/** L'anello attorno al tondo scelto, che il velo del Disegno disegna arancione attorno al rosso. */
private val SWATCH_RING = 3.dp

/** Il filo d'aria fra il bordo del tasto 'Riempimento' e il suo rettangolo (nota 4). */
private val KEY_GAP = 3.dp

/** L'angolo di un chip, che il rettangolo di 'Riempimento' segue dentro il filo d'aria. */
private val KEY_CORNER = 8.dp

/** I quadretti più scuri della scacchiera del tasto 'Riempimento'. */
private val CHECKER = Color(0xFFC8C8C8)

/** I nomi dei colori, nell'ordine di [Draw.INKS]. */
private val INK_NAMES = listOf(
    R.string.ink_red, R.string.ink_amber, R.string.ink_green, R.string.ink_blue,
    R.string.ink_violet, R.string.ink_pink, R.string.ink_white, R.string.ink_grey, R.string.ink_black
)

/**
 * Quello che la scheda mostra fra la fila dei gettoni e quella dei comandi: i cursori del modulo
 * [module], e quello che quel modulo ha in più (le forme e la posa del Ritaglio, le otto fasce
 * dell'HSL, il grafico delle Curve, l'interruttore del bianco e nero del Colore).
 *
 * ⚠️ **Prende l'indice del modulo invece di leggere [Gaze.module]**: [SteadyBody] compone questo
 * corpo una volta per modulo per trovare il più alto, e un corpo che leggesse il modulo in scena
 * risponderebbe sempre la stessa altezza. Le lambda di scrittura invece leggono lo sguardo vivo
 * (`dialAt`): le copie misurate non sono posizionate, e nessun dito le raggiunge.
 * Ogni modulo ha un'altezza sola (vedi [NOT_MONO]).
 */
@Composable
private fun ModuleBody(
    onHealApply: () -> Unit,
    module: Int,
    look: Look,
    gaze: Gaze,
    ready: Boolean,
    busy: Boolean,
    origin: Bitmap?,
    /** La colonna dei nomi, misurata una volta per tutti i moduli: vedi [knobNameWidth]. */
    nameWidth: Dp,
    /**
     * L'altezza comune della scheda, per il solo corpo che la riceve invece di dettarla: l'elenco
     * degli stili, che ci scorre dentro (vedi [Extra.PRESETS]). Gli altri moduli la ignorano.
     */
    height: Dp,
    /**
     * Gli stili salvati, per il solo corpo che li elenca. Arrivano dalla scheda perché a crearli è
     * un comando sulla barra (vedi [PresetBody]).
     */
    mine: List<Preset>,
    onLive: ((Look) -> Look) -> Unit,
    onSettled: () -> Unit,
    onPeek: (((Look) -> Look)?) -> Unit
) {
    val mod = MODULES[module]
    val band = gaze.band

    /*
     * L'elenco degli stili riceve l'altezza invece di dettarla, e i suoi gesti scrivono un [Look]
     * intero. Applicare è un passo della storia (`onLive` più `onSettled`, come 'Auto'), che
     * 'Annulla' disfa.
     */
    if (mod.extra == Extra.PRESETS) {
        PresetBody(
            height = height,
            mine = mine,
            onPick = { scelto, add ->
                onLive { if (add) scelto.addTo(it) else scelto.applyTo(it) }
                onSettled()
            }
        )
    }

    /**
     * Il cursore che sta alla riga [riga] del modulo e della fascia che si stanno guardando
     * **adesso**, o `null` se là non c'è niente (i moduli non hanno tutti lo stesso numero di
     * cursori).
     *
     * ⚠️ **Legge [Gaze.module] e [Gaze.band] al momento della chiamata e non alla composizione**:
     * chiamata dal rilevatore di un gesto, risponde con quello che il dito ha davvero sotto. Senza
     * la fascia, un cursore dell'HSL scriverebbe nella fascia da cui il suo nodo è nato.
     */
    fun dialAt(riga: Int): Dial? = MODULES[gaze.module].rows(gaze.band).getOrNull(riga)

    /** Quante volte l'immagine **già posata** è più larga che alta: vedi [posedAspect]. */
    fun cropAspect(k: Look): Float = posedAspect(origin, k.spin)

    if (mod.extra == Extra.HEALING && (gaze.healing || !gaze.selection.idle)) {
        DisposableEffect(gaze) { onDispose { gaze.healingSizing = false } }
        Text(stringResource(R.string.look_heal), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(
                if (gaze.healingBusy) R.string.look_heal_working else R.string.look_heal_hint
            ),
            style = MaterialTheme.typography.bodySmall
        )
        Text(stringResource(R.string.look_brush_size), style = MaterialTheme.typography.labelMedium)
        ValueSlider(value = gaze.healingRadius, onValueChange = {
            gaze.healingSizing = true; gaze.healingRadius = it
        }, onValueChangeFinished = { gaze.healingSizing = false },
            valueRange = Healing.MIN_RADIUS..Healing.MAX_RADIUS, enabled = ready && !busy)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = {
                gaze.selection = Healing.Selection.NONE; gaze.healingFailed = false
            }, enabled = !busy && !gaze.selection.idle) { Text(stringResource(R.string.look_heal_clear)) }
            TextButton(onClick = onHealApply, enabled = ready && !busy && !gaze.selection.idle) {
                Text(stringResource(R.string.editor_apply))
            }
        }
        return
    }
    if (mod.extra == Extra.CORNERS && gaze.liquifying) {
        DisposableEffect(gaze) {
            onDispose { gaze.brushSizing = false }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !gaze.rebuilding,
                onClick = { gaze.rebuilding = false },
                label = {
                    Text(
                        stringResource(R.string.look_deform),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                modifier = Modifier.weight(1.1f)
            )
            FilterChip(
                selected = gaze.rebuilding,
                onClick = { gaze.rebuilding = true },
                label = {
                    Text(
                        stringResource(R.string.look_rebuild),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                modifier = Modifier.weight(1.45f)
            )
            TextButton(
                onClick = {
                    onLive { it.copy(geo = it.geo.copy(liquify = Liquify.NONE)) }
                    onSettled()
                },
                enabled = ready && !busy && !look.geo.liquify.idle,
                modifier = Modifier.weight(0.65f),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) { Text(stringResource(R.string.editor_original)) }
        }
        Text(stringResource(R.string.look_brush_size), style = MaterialTheme.typography.labelMedium)
        ValueSlider(
            value = gaze.brushRadius,
            onValueChange = {
                gaze.brushSizing = true
                gaze.brushRadius = it
            },
            onValueChangeFinished = { gaze.brushSizing = false },
            valueRange = Liquify.MIN_RADIUS..Liquify.MAX_RADIUS,
            enabled = ready && !busy
        )
        Text(stringResource(R.string.look_brush_strength), style = MaterialTheme.typography.labelMedium)
        ValueSlider(
            value = gaze.brushStrength,
            onValueChange = { gaze.brushStrength = it },
            valueRange = 0.05f..1f,
            enabled = ready && !busy
        )
        return
    }
    if (mod.extra == Extra.DRAW) {
        /*
         * Le file del Disegno distano [DRAW_AIR] (nota sul giro della `4.93`, con bozza): con il
         * solo margine di ogni fila i tasti quasi si toccavano. Il modulo è il più alto, quindi la
         * scheda cresce con lui.
         * ⚠️ Il margine in alto e i 4dp della fila dei moduli ([MOD_GAP]) fanno un [DRAW_AIR]; in
         * basso [DRAW_FOOT] tiene le strisce lontane dalle icone della barra come nella bozza.
         */
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = DRAW_AIR - MOD_GAP, bottom = DRAW_FOOT),
            verticalArrangement = Arrangement.spacedBy(DRAW_AIR)
        ) {
            DrawBody(look, gaze, live = ready && !busy, onLive = onLive, onSettled = onSettled)
        }
        return
    }
    /*
     * La fila della posa è del solo Ritaglio, coi cinque tasti dell'editor semplice: stesso pezzo
     * (`ActionPad`), stessi glifi, stesse etichette e stessi tocchi lunghi. Le due centrature con le
     * forme sopra sono la risposta `formati` dell'utente a `d-crop-formati`.
     * ⚠️ L'ordine salvato dal riordino non si legge: è quello della fila della selezione.
     */
    if (mod.extra == Extra.CROP) {
        val live = ready && !busy
        val lay = cropLay(gaze)
        fun pose(gesto: Spin) {
            // ⚠️ Con una proporzione un quarto di giro rifà la cornice: vedi [posedLook].
            onLive { k ->
                posedLook(k, gesto, cropShape(gaze), lay, posedAspect(origin, k.spin.then(gesto)))
            }
            onSettled()
        }
        /** Un comando della scheda che lavora dentro la porzione inquadrata: vedi [framedCrop]. */
        fun inFrame(op: (ImageEdit.Crop, Float) -> ImageEdit.Crop) {
            onLive { k -> k.copy(crop = framedCrop(k, cropAspect(k), op)) }
            onSettled()
        }
        /*
         * Le forme le disegna `ShapeRow`, lo stesso pezzo dell'editor semplice: qui resta solo
         * dove si scrive la scelta.
         */
        ShapeRow(
            shape = cropShape(gaze),
            lay = lay,
            enabled = live,
            /*
             * Qui le forme vanno a capo (voce `crop-fila`: devono occupare più spazio); le due
             * disposizioni vivono su [ShapeRow].
             */
            rows = ShapeRows.FULL,
            onShape = { one ->
                /*
                 * Ritoccare la forma già scelta rimette il rettangolo intero, come nell'editor
                 * semplice: è il modo rapido di tornare grandi dopo una riduzione per sbaglio.
                 */
                val prima = cropShape(gaze)
                gaze.shape = one.ordinal
                inFrame { dentro, quanto ->
                    if (one == prima) {
                        one.fit(quanto, lay)
                    } else {
                        reshaped(dentro, quanto, one.value(lay, quanto))
                    }
                }
            },
            /*
             * I due versi sono due icone dentro la fila delle forme, a destra di 'Originale'
             * (punto `crop-giu` dell'utente): una riga in più per due stati costava [CHIP_TALL].
             */
            onLay = { one ->
                if (one != lay) {
                    gaze.lay = one.ordinal
                    inFrame { dentro, quanto -> flipped(dentro, quanto) }
                }
            },
            /*
             * Niente distacco scritto a mano fra i blocchi: l'aria fra le righe di un modulo la
             * distribuisce [Breathe], e una seconda si sommerebbe alla sua.
             */
            modifier = Modifier.padding(top = CROP_TOP_AIR)
        )
        ActionPad(
            columns = POSE_KEYS,
            stretch = true,
            actions = listOf(
                PadAction(
                    PadKey.CENTRE_ACROSS, Glyphs.AlignAcross, R.string.editor_center_across,
                    enabled = live,
                    onHold = { inFrame { dentro, _ -> centredDown(centredAcross(dentro)) } },
                    holdLabel = R.string.editor_center_both
                ) { inFrame { dentro, _ -> centredAcross(dentro) } },
                PadAction(
                    PadKey.CENTRE_DOWN, Glyphs.AlignDown, R.string.editor_center_down,
                    enabled = live,
                    onHold = { inFrame { dentro, _ -> centredDown(centredAcross(dentro)) } },
                    holdLabel = R.string.editor_center_both
                ) { inFrame { dentro, _ -> centredDown(dentro) } },
                /*
                 * Il tocco lungo ha sempre la sua etichetta, come vuole [PadAction.onHold]: un
                 * gesto che il lettore di schermo non annuncia lo scopre solo chi ci arriva per
                 * caso.
                 */
                PadAction(
                    PadKey.FLIP, Glyphs.Material.Flip, R.string.editor_flip,
                    enabled = live,
                    onHold = { pose(Spin.DOWN) },
                    holdLabel = R.string.editor_flip_down
                ) { pose(Spin.ACROSS) },
                PadAction(
                    PadKey.TURN_LEFT, Glyphs.TurnLeft, R.string.editor_left,
                    enabled = live
                ) { pose(Spin(3, false)) },
                PadAction(
                    PadKey.TURN_RIGHT, Glyphs.TurnRight, R.string.editor_right,
                    enabled = live
                ) { pose(Spin(1, false)) }
            ),
            modifier = Modifier
        )
        /*
         * I quattro comandi del Ritaglio ('Indietro', 'Avanti', 'Applica', 'Azzera'), chiesti
         * dall'utente per il solo Ritaglio, vivono insieme dentro il modulo: due storie con gli
         * stessi gesti nella fila di fondo si scambierebbero.
         * ⚠️ **Non è un [ActionPad], per una misura**: una sua cella è alta 64 punti, e il Ritaglio
         * arrivava a 278, oltre il modulo più alto: la scheda cresceva in tutti i moduli e sul
         * banco il palco andava a zero (lo hanno preso le prove sui pixel).
         * La riga è alta [CROP_CMD_ROW] (vedi lì il perché).
         */
        val portata = relativeTo(look.framing.shown, look.crop)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CROP_CMD_GAP)
        ) {
            CropCmd(
                icon = Glyphs.Material.ArrowBack,
                label = R.string.editor_crop_back,
                enabled = live && look.framing.undoable
            ) {
                onLive { k ->
                    val prima = k.framing.back()
                    k.copy(framing = prima, crop = prima.shown ?: ImageEdit.Crop.WHOLE)
                }
                onSettled()
            }
            CropCmd(
                icon = Glyphs.Material.ArrowForward,
                label = R.string.editor_crop_on,
                enabled = live && look.framing.redoable
            ) {
                onLive { k ->
                    val dopo = k.framing.on()
                    k.copy(framing = dopo, crop = dopo.shown ?: ImageEdit.Crop.WHOLE)
                }
                onSettled()
            }
            /*
             * Si accende quando il rettangolo è più piccolo della porzione che si vede: altrimenti
             * applicarlo non cambierebbe niente.
             */
            CropCmd(
                icon = Glyphs.EditApply,
                label = R.string.editor_apply,
                enabled = live && !portata.whole
            ) {
                onLive { k -> k.copy(framing = k.framing.applied(k.crop)) }
                onSettled()
            }
            /*
             * 'Azzera' guarda il solo ritaglio e non la posa, al contrario del tocco lungo sul
             * gettone, che rimette a nuovo il modulo intero.
             */
            CropCmd(
                icon = Glyphs.Material.CropFree,
                label = R.string.editor_crop_clear,
                enabled = live && (!look.crop.whole || look.framing.undoable)
            ) {
                onLive { k ->
                    k.copy(framing = Framing.NONE, crop = ImageEdit.Crop.WHOLE)
                }
                onSettled()
            }
        }
        CropRule()
    }

    /*
     * La fila delle otto fasce dell'HSL: gettoni di colore, che dicono che cosa si sceglie senza
     * leggere. I gesti sono quelli del gettone di un modulo, tocco per scegliere e tocco lungo per
     * azzerare, qui la fascia.
     */
    if (mod.extra == Extra.BANDS) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BAND_NAMES.forEachIndexed { i, nome ->
                BandChip(
                    name = stringResource(nome),
                    hue = Mix.CENTRES[i],
                    chosen = i == band,
                    spent = !look.mix.bands[i].idle,
                    enabled = ready && !busy,
                    onTap = { gaze.band = i },
                    onHold = {
                        onLive { it.copy(mix = it.mix.swap(i) { Band.NONE }) }
                        onSettled()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    /*
     * La fila dei quattro canali e il grafico delle Curve. Il gettone è quello della fila dei
     * moduli, con gli stessi gesti: il tocco lungo azzera la curva di quel canale.
     */
    if (mod.extra == Extra.CURVES) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TONE_NAMES.forEachIndexed { i, nome ->
                ModuleChip(
                    name = stringResource(nome),
                    chosen = i == gaze.channel,
                    spent = !look.tone.curve(i).idle,
                    enabled = ready && !busy,
                    onTap = { gaze.channel = i },
                    onHold = {
                        onLive { k -> k.copy(tone = k.tone.swap(i) { Curve.NONE }) }
                        onSettled()
                    }
                )
            }
        }
        CurveBoard(
            curve = look.tone.curve(gaze.channel),
            ink = toneInk(gaze.channel),
            enabled = ready && !busy,
            /*
             * ⚠️ Arriva un cambiamento e il canale si risolve qui: il gesto del grafico non tiene
             * né la curva né il canale, che invecchierebbero, e legge [Gaze.channel] al momento
             * della scrittura.
             */
            onEdit = { cambia ->
                onLive { k -> k.copy(tone = k.tone.swap(gaze.channel, cambia)) }
            },
            onSettled = onSettled,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        )
    }

    /*
     * ⚠️ **Un gesto scrive la riga che tocca nel modulo che si vede** (giro della `2.19`: il tocco
     * su un cursore ne spostava uno di un altro modulo): risolvendo la riga al momento della
     * scrittura, il cursore N scrive sempre il cursore N di quello che è in scena. La causa non è
     * accertata (il difetto non si riproduce sul banco): questo è il meccanismo che lo rende
     * impossibile, dichiarato come tale.
     * Il `key` è la seconda metà: senza, Compose riusa i nodi di una lista per posizione, e
     * cambiando modulo passerebbero di mano. La chiave è il [Dial], stabile per tutta la vita del
     * processo, e due moduli non ne hanno in comune.
     */
    mod.rows(band).forEachIndexed { riga, knob ->
        /*
         * L'interruttore del bianco e nero si disegna davanti al filtro (punto A del giro della
         * `2.36`), riconosciuto per identità: vedi [FILTER_ROW].
         */
        if (knob === FILTER_ROW) {
            MonoSwitch(
                look = look,
                ready = ready,
                busy = busy,
                onLive = onLive,
                onSettled = onSettled
            )
        }
        if (knob === STRAIGHTEN_ROW) {
            DisposableEffect(gaze) { onDispose { gaze.leveling = false } }
        }
        key(knob) {
            LookKnob(
                name = stringResource(knob.name),
                value = knob.read(look),
                span = knob.span,
                stops = knob.stops,
                unipolar = knob.unipolar,
                /*
                 * Un cursore che non governa niente si spegne (vedi [Dial.off]): uno che si
                 * muove senza cambiare l'immagine si legge come un guasto.
                 */
                enabled = ready && !busy && !knob.off(look),
                nameWidth = nameWidth,
                onLive = { v ->
                    if (knob === STRAIGHTEN_ROW) gaze.leveling = true
                    dialAt(riga)?.let { onLive(it.set(v)) }
                },
                onSettled = {
                    gaze.leveling = false
                    onSettled()
                },
                /*
                 * Il confronto usa lo stesso `set` con cui il cursore scrive: l'immagine di
                 * adesso con questo solo campo a zero. Parte da quello che si vede adesso e non
                 * da [look], perché un valore catturato nel gesto invecchia (vedi [Dial.set]).
                 */
                onPeek = { on -> onPeek(if (on) dialAt(riga)?.set(0f) else null) }
            )
        }
    }
}

/**
 * L'interruttore del **bianco e nero**, che nel modulo Colore vive fra i quattro cursori del colore
 * e il 'Filtro BN'.
 *
 * È un interruttore e non un cursore a -100: è una scelta e non una quantità, e come fondo corsa
 * della saturazione resterebbe esposto a chi muove quel cursore. Sotto di lui c'è il 'Filtro BN',
 * che ne dipende.
 * La riga è un bersaglio solo, con `Role.Switch` sulla riga e niente sull'interruttore, come
 * nelle impostazioni.
 */
@Composable
private fun MonoSwitch(
    look: Look,
    ready: Boolean,
    busy: Boolean,
    onLive: ((Look) -> Look) -> Unit,
    onSettled: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = look.chroma.mono,
                enabled = ready && !busy,
                role = Role.Switch,
                onValueChange = { on ->
                    onLive { it.copy(chroma = it.chroma.copy(mono = on)) }
                    onSettled()
                }
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        /*
         * Lo stesso corpo dei nomi dei cursori, in mezzo ai quali vive questa riga.
         */
        Text(
            text = stringResource(R.string.look_bw),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = look.chroma.mono, onCheckedChange = null, enabled = ready && !busy)
    }
}

/**
 * Uno dei quattro comandi del ritaglio: un glifo in una cella che si divide la riga con le altre.
 *
 * Il nome vive nella descrizione parlata del glifo, come nei gettoni dei moduli: nella riga alta
 * [CROP_CMD_ROW] una parola non entrerebbe, ma il lettore di schermo la annuncia e il banco la
 * cerca. Il tocco vive sulla cella, quindi il bersaglio è largo un quarto di riga.
 */
@Composable
private fun RowScope.CropCmd(
    icon: ImageVector,
    label: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(CROP_CMD_ROW)
            .clip(RoundedCornerShape(CROP_CMD_ROUND))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(label),
            modifier = Modifier.size(CROP_CMD_ICON),
            tint = LocalContentColor.current.copy(alpha = if (enabled) 1f else OFF_INK)
        )
    }
}

/**
 * Il separatore sfumato fra i quattro comandi del ritaglio e la barra in basso (punto D
 * dell'utente): i comandi salgono perché qualcosa li separa. Non rifà il difetto della voce
 * `crop-alti`: l'aria che spende viene dalla riga dei versi, entrata nella fila delle forme, e non
 * dall'avanzo di [Breathe]. Il filo è `FadedRule`, in `Theme.kt` (nota su `d-filo-uno`); qui resta
 * l'aria, che è del Ritaglio.
 */
@Composable
private fun CropRule() {
    FadedRule(Modifier.padding(vertical = CROP_RULE_AIR))
}

/**
 * Il corpo della scheda, alto quanto il più alto dei [slots] moduli: così la scheda non cambia
 * misura passando da un modulo all'altro, e il palco sopra di lei nemmeno.
 *
 * ⚠️ **L'altezza si misura e non si scrive**: dipende dal corpo del carattere, dalla sua scala e
 * dalla lingua, quindi un numero in `dp` sarebbe giusto su un telefono e sbagliato sul prossimo.
 * Ogni corpo si compone una volta, si misura, e si tiene il massimo.
 *
 * ⚠️ **Le copie vivono un giro solo**: tenute in scena con un `SubcomposeLayout`, non si
 * disegnano e non prendono il dito, ma il lettore di schermo annuncia i cursori di tutti i moduli
 * e il banco li conta (la prima stesura ha fatto rosse sette prove). La misura si scrive in uno
 * stato, e la ricomposizione che segue le toglie. Nel fotogramma in cui ci sono non hanno
 * semantica (`clearAndSetSemantics`).
 *
 * La misura si fa una volta per schermata aperta: le altezze non dipendono dai valori dei
 * cursori, e rifarla a ogni fotogramma di trascinamento comporrebbe tutti i moduli sessanta volte
 * al secondo. La larghezza non cambia: la schermata inibisce la rotazione, e un cambio di
 * configurazione rifà anche questo `remember`. Vale perché ogni modulo ha un'altezza sola (vedi
 * [NOT_MONO]).
 */
@Composable
internal fun SteadyBody(
    slots: Int,
    chosen: Int,
    modifier: Modifier = Modifier,
    /**
     * Se lo slot [it] entra nella misura dell'altezza comune: l'elenco degli stili ne resta
     * fuori, la riceve e ci scorre dentro (vedi [Extra.PRESETS]). Almeno uno deve entrarci, o non
     * ci sarebbe nessuna altezza da ricevere.
     */
    measured: (Int) -> Boolean = { true },
    body: @Composable (Int, Dp) -> Unit
) {
    var tallest by remember { mutableStateOf<Int?>(null) }
    val comune = with(LocalDensity.current) { (tallest ?: 0).toDp() }
    Box(modifier) {
        if (tallest == null) {
            Layout(
                content = {
                    for (i in 0 until slots) {
                        if (measured(i)) Column(modifier = Modifier.fillMaxWidth()) { body(i, 0.dp) }
                    }
                },
                modifier = Modifier.clearAndSetSemantics { }
            ) { corpi, constraints ->
                val libero = constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                val massimo = corpi.maxOfOrNull { it.measure(libero).height } ?: 0
                /*
                 * ⚠️ **Nessun `place`**: un nodo non posato non si disegna e non riceve i
                 * tocchi, ed è questo che rende innocue le copie.
                 * La misura si scrive nel piazzamento, la fase in cui Compose ammette una
                 * scrittura di stato: avvia la ricomposizione che toglie le copie, una volta
                 * sola, perché poi il valore non è più nullo.
                 */
                layout(0, 0) { tallest = massimo }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = comune),
            verticalArrangement = Breathe
        ) {
            body(chosen, comune)
        }
    }
}

/**
 * Come si dispone un corpo **più corto** dell'altezza comune: un po' di aria fra le righe, e il
 * resto diviso sopra e sotto.
 *
 * Richiesta dell'utente: i moduli corti devono respirare, invece di restare appiccicati in cima a
 * un vuoto.
 *
 * ⚠️ **L'aria ha un tetto**: nel Ritaglio avanzano un centinaio di punti su tre blocchi, e divisi
 * per due vani darebbero tre isole invece di un pannello. Col tetto ogni vano prende il suo
 * respiro, e il resto centra il blocco.
 *
 * È un `Arrangement` perché il conto viva in un posto solo per ogni modulo, e il banco lo prova
 * chiamandolo.
 */
internal object Breathe : Arrangement.Vertical {
    override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
        val usato = sizes.sum()
        val avanzo = (totalSize - usato).coerceAtLeast(0)
        val vani = sizes.size - 1
        val aria = if (vani > 0) min(avanzo / vani, BREATH_MAX.roundToPx()) else 0
        var y = (avanzo - aria * vani) / 2
        for (i in sizes.indices) {
            outPositions[i] = y
            y += sizes[i] + aria
        }
    }
}

/**
 * Quanta aria al massimo [Breathe] mette fra due righe di un modulo corto.
 *
 * È un tetto e non una spaziatura: dove non avanza niente vale zero, e il modulo più alto non
 * cambia.
 */
private val BREATH_MAX = 12.dp

/**
 * Un gettone della fila dei moduli: il nome, il punto di 'toccato', e i due gesti.
 *
 * ⚠️ **Non è un `FilterChip`, per il tocco lungo**: quel pezzo non offre un secondo gesto, e il
 * 'Reset modulo' andrebbe in un secondo nodo che consuma il tocco prima del chip. Con
 * `combinedClickable` i gesti sono due e il bersaglio resta uno.
 */
@Composable
private fun ModuleChip(
    name: String,
    /**
     * Il glifo da scrivere al posto del nome, o `null` per il gettone scritto: i nomi dei moduli
     * non entrano nella larghezza (`docs/Editor.md` § '✂️ Il modulo Ritaglio, e la fila che è
     * diventata di icone'), quelli dei canali delle Curve sono corti e dicono più di un'icona.
     * Col glifo il nome resta il `contentDescription`.
     */
    icon: ImageVector? = null,
    chosen: Boolean,
    spent: Boolean,
    enabled: Boolean,
    onTap: () -> Unit,
    onHold: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wipe = stringResource(R.string.look_reset_one, name)
    val face = if (chosen) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val ink = if (chosen) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(face)
            .combinedClickable(
                enabled = enabled,
                role = Role.Tab,
                onClick = onTap,
                // ⚠️ La vibrazione è [HOLD_BUZZ] e quella di Compose è spenta: vedi [withHaptics].
                onLongClick = withHaptics(onHold),
                hapticFeedbackEnabled = false,
                onLongClickLabel = wipe
            )
            .padding(horizontal = if (icon == null) MODULE_SIDE else 0.dp, vertical = MODULE_PAD)
            .alpha(if (enabled) 1f else OFF_INK),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon == null) {
            Text(text = name, style = MaterialTheme.typography.labelLarge, color = ink)
        } else {
            Icon(imageVector = icon, contentDescription = name, tint = ink)
        }
        // Il segno di 'questo modulo ha toccato l'immagine': senza, i cursori di un modulo che
        // non si sta guardando non hanno niente che li dichiari.
        if (spent) {
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(MODULE_MARK)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * Una pastiglia della fila delle fasce: il tondo del suo colore, l'anello di 'scelta' e il punto
 * di 'toccata'.
 *
 * Il colore arriva dal centro della fascia, lo stesso numero con cui il conto assegna un pixel a
 * una fascia: la pastiglia non può dire un colore che il cursore non tocca.
 *
 * L'anello e il punto sono in due posti diversi, perché si vedono spesso insieme e sovrapposti
 * si confonderebbero.
 */
@Composable
private fun BandChip(
    name: String,
    hue: Float,
    chosen: Boolean,
    spent: Boolean,
    enabled: Boolean,
    onTap: () -> Unit,
    onHold: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wipe = stringResource(R.string.look_reset_one, name)
    val tint = bandTint(hue)
    val ring = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
            .height(BAND_ROW)
            .semantics {
                contentDescription = name
                role = Role.Tab
                selected = chosen
            }
            .combinedClickable(
                enabled = enabled,
                onClick = onTap,
                onLongClick = withHaptics(onHold),
                hapticFeedbackEnabled = false,
                onLongClickLabel = wipe
            )
            .alpha(if (enabled) 1f else OFF_INK)
    ) {
        val dot = BAND_DOT.toPx() / 2f
        val gap = BAND_GAP.toPx()
        val mark = MODULE_MARK.toPx() / 2f
        val middle = Offset(size.width / 2f, dot + gap)
        drawCircle(tint, dot, middle)
        if (chosen) {
            drawCircle(ring, dot + gap, middle, style = Stroke(BAND_RING.toPx()))
        }
        if (spent) {
            drawCircle(ring, mark, Offset(middle.x, middle.y + dot + gap * 2f + mark))
        }
    }
}

/**
 * Il grafico di una curva tonale: il fondo con la griglia, la diagonale, la curva e i suoi punti.
 *
 * I gesti: il trascinamento prende il punto sotto il dito, o ne fa uno nuovo, e lo sposta; il
 * tocco lungo su un punto lo toglie, come si azzera un modulo o una fascia. Un tocco secco fa
 * nascere un punto sulla curva senza spostarla.
 *
 * ⚠️ **Il riquadro non è quadrato, per compromesso dichiarato**: un quadrato largo quanto lo
 * schermo toglierebbe metà del palco. La forma della curva resta leggibile.
 *
 * Un grafico a punti non si governa col lettore di schermo: qui l'accessibilità si ferma alla
 * descrizione, e i cursori della Luce fanno lo stesso lavoro con comandi che si annunciano.
 */
@Composable
private fun CurveBoard(
    curve: Curve,
    ink: Color,
    enabled: Boolean,
    onEdit: ((Curve) -> Curve) -> Unit,
    onSettled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val board = stringResource(R.string.look_tone_board)
    val face = MaterialTheme.colorScheme.surfaceContainerHighest
    val grid = MaterialTheme.colorScheme.onSurfaceVariant
    val round = with(LocalDensity.current) { BOARD_ROUND.toPx() }
    val dot = with(LocalDensity.current) { BOARD_DOT.toPx() }
    val line = with(LocalDensity.current) { BOARD_LINE.toPx() }
    val pad = with(LocalDensity.current) { BOARD_PAD.toPx() }

    /*
     * ⚠️ **La curva si legge viva, e non entra nella chiave del rilevatore**: con il numero di
     * punti nella chiave, il punto appena nato cambiava la chiave e Compose annullava il gesto in
     * corso, quindi il dito non lo muoveva (richiesta dell'utente: toccare e trascinare deve
     * aggiungere il punto e spostarlo).
     */
    val viva by rememberUpdatedState(curve)

    Canvas(
        modifier = modifier
            .height(BOARD_H)
            .semantics { contentDescription = board }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                val wide = (size.width - 2 * pad).coerceAtLeast(1f)
                val tall = (size.height - 2 * pad).coerceAtLeast(1f)

                /** Dove cade, in scala della curva, il punto [at] dello schermo. */
                fun atOf(at: Offset): Float = ((at.x - pad) / wide).coerceIn(0f, 1f)
                fun toOf(at: Offset): Float = (1f - (at.y - pad) / tall).coerceIn(0f, 1f)

                awaitEachGesture {
                    val down = awaitFirstDown()
                    val x = atOf(down.position)
                    /*
                     * ⚠️ **La curva si fotografa qui, una volta per tutto il gesto**: fra `onEdit`
                     * e la riga dopo non c'è ricomposizione, quindi [rememberUpdatedState]
                     * risponde ancora la curva di prima. Rileggendola, il punto appena nato cadeva
                     * sull'ultimo indice, il gesto si credeva su un estremo e portava il bordo al
                     * livello del dito (segnalazione dell'utente: una retta orizzontale fino al
                     * margine). Quello che il gesto scrive lo scrive per indice.
                     */
                    val partenza = viva
                    val near = partenza.nearest(x)
                    /*
                     * Il tempo si misura qui: un dito fermo non genera eventi, quindi il tocco
                     * lungo lo vede solo un timeout (vedi [settled]).
                     */
                    val esito = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        settled(down, viewConfiguration.touchSlop)
                    }
                    if (esito == null) {
                        // Il tocco lungo: se sotto il dito c'era un punto, se ne va.
                        if (near > 0) {
                            onEdit { it.drop(near) }
                            onSettled()
                        }
                        waitForUpOrCancellation()
                        return@awaitEachGesture
                    }
                    /*
                     * L'indice si ricava prima di scrivere: `grow` risponde anche dove è finito il
                     * punto, e il gesto muove quello senza cercarlo a ogni fotogramma.
                     */
                    val i = if (near >= 0) near else partenza.grow(x).second
                    if (near < 0) {
                        onEdit { it.grow(x).first }
                    }
                    if (esito == Settled.MOVED) {
                        /*
                         * Un estremo trascinato lascia un punto gemello al bordo, allo stesso
                         * livello (richiesta dell'utente). Nasce solo quando il gesto è già un
                         * trascinamento: un tocco secco su un estremo non lascia niente.
                         * L'indice del punto mosso scala di uno se il gemello entra in testa. Al
                         * tetto dei punti [Curve.pin] risponde la curva com'è, e l'estremo resta
                         * tale.
                         * ⚠️ Solo un punto che c'era già può essere un estremo: uno appena nato è
                         * per definizione fra due punti, e `near < 0` chiude il caso anche al
                         * tetto, dove [Curve.grow] risponde un indice qualunque.
                         */
                        val bordo = near >= 0 &&
                            (near == 0 || near == partenza.knots.lastIndex) &&
                            partenza.knots.size < Curve.MAX_KNOTS
                        val testa = bordo && near == 0
                        if (bordo) onEdit { it.pin(testa) }
                        val quale = if (testa) 1 else i
                        val gemello = if (!bordo) -1 else if (testa) 0 else i + 1
                        drag(down.id) { change ->
                            onEdit {
                                it.move(
                                    quale,
                                    atOf(change.position),
                                    toOf(change.position),
                                    gemello
                                )
                            }
                            change.consume()
                        }
                        // ⚠️ Il gemello rimasto a filo si toglie: chi porta l'estremo dentro e
                        // poi indietro non si ritrova due punti sovrapposti.
                        if (bordo) onEdit { it.tidy() }
                    }
                    onSettled()
                }
            }
    ) {
        val wide = (size.width - 2 * pad).coerceAtLeast(1f)
        val tall = (size.height - 2 * pad).coerceAtLeast(1f)
        fun px(x: Float) = pad + x * wide
        fun py(y: Float) = pad + (1f - y) * tall

        drawRoundRect(color = face, cornerRadius = CornerRadius(round, round))
        // La griglia in terzi e la diagonale dicono dov'è il dito senza numeri. La diagonale, la
        // curva che non fa niente, è tratteggiata: piena si confonderebbe con una curva a riposo.
        for (k in 1..2) {
            val t = k / 3f
            drawLine(grid.copy(alpha = 0.18f), Offset(px(t), py(0f)), Offset(px(t), py(1f)), line)
            drawLine(grid.copy(alpha = 0.18f), Offset(px(0f), py(t)), Offset(px(1f), py(t)), line)
        }
        drawLine(
            color = grid.copy(alpha = 0.35f),
            start = Offset(px(0f), py(0f)),
            end = Offset(px(1f), py(1f)),
            strokeWidth = line,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
        )

        // La curva, letta dalla stessa tabella che va alla scheda grafica: quello che si vede è
        // quello che l'immagine riceve.
        val table = curve.table()
        val path = Path().apply {
            moveTo(px(0f), py(table[0]))
            for (k in 1 until Curve.SIZE) {
                lineTo(px(k.toFloat() / (Curve.SIZE - 1)), py(table[k]))
            }
        }
        drawPath(path, color = ink, style = Stroke(width = line * 2f))

        curve.knots.forEach { k ->
            drawCircle(color = ink, radius = dot, center = Offset(px(k.at), py(k.to)))
            drawCircle(color = face, radius = dot - line, center = Offset(px(k.at), py(k.to)))
        }
    }
}

/**
 * Una riga di cursore: il nome, la barra e il numero.
 *
 * Il numero è il tasto che azzera, come il doppio tocco sul nome, sul tondo e sulla barra
 * (richiesta dell'utente): resta perché è l'unica superficie che si vede da sola come comando.
 *
 * Il tocco lungo sul nome mostra l'immagine senza questo cursore (stessa richiesta). Vive sul nome
 * e non sulla barra, dove scatterebbe ogni volta che il dito si ferma mentre trascina.
 *
 * Due chiamate: quella continua muove quello che si vede, quella finale scrive un passo nella
 * storia (vedi la nota sulla pila in [AdvancedEditorScreen]).
 * ⚠️ **Quella finale non contiene nessun valore**: Compose chiama `onValueChange` e
 * `onValueChangeFinished` senza per forza ricomporre in mezzo, quindi un valore catturato poteva
 * essere quello di prima, e 'Annulla' restava spento. Il passo legge lo stato vivo. Lo presidia
 * `LuceTest`.
 */
@Composable
private fun LookKnob(
    name: String,
    value: Float,
    enabled: Boolean,
    /** La colonna del nome, misurata una volta per tutti i cursori: vedi [knobNameWidth]. */
    nameWidth: Dp,
    onLive: (Float) -> Unit,
    onSettled: () -> Unit,
    onPeek: (Boolean) -> Unit,
    span: Float = 1f,
    stops: Boolean = false,
    unipolar: Boolean = false
) {
    val zero = stringResource(R.string.look_reset_one, name)
    val against = stringResource(R.string.look_peek_one, name)
    /*
     * Le tre lambda si leggono vive: il corpo di un `pointerInput` si ricostruisce solo quando
     * cambiano le sue chiavi, e in linea di principio chiamerebbe le lambda di quando è nato. È una
     * difesa dichiarata, non la cura di un difetto misurato; è la forma di `live` in [LookDial].
     */
    val write by rememberUpdatedState(onLive)
    val settle by rememberUpdatedState(onSettled)
    val peek by rememberUpdatedState(onPeek)
    val reset = { write(0f); settle() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KNOB_GAP)
    ) {
        /*
         * Il nome è un gradino più piccolo, per richiesta dell'utente (moduli più compatti): la
         * riga è alta [DIAL_ROW] comunque, e i punti di larghezza che avanzano vanno alla barra.
         * La colonna la decide la misura ([knobNameWidth]).
         * ⚠️ **Il nome può andare a capo** (istruzione dell'utente, purché la barra resti alla
         * stessa distanza da quella sopra): due righe di `bodySmall` sono 32 punti contro i 36 di
         * [DIAL_ROW], quindi la riga non cresce. `maxLines` esclude la terza.
         * Resta fuori, dichiarato: oltre il 150% della scala dei caratteri di sistema due righe
         * superano i 36 punti e quella riga si allunga; un'altezza fissa farebbe sbordare il testo
         * sulla riga vicina, che è peggio.
         */
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .width(nameWidth)
                .semantics { contentDescription = against }
                .heldOrTwice(enabled = enabled, onTwice = reset, onHold = { peek(it) })
        )
        LookDial(
            value = value,
            span = span,
            unipolar = unipolar,
            enabled = enabled,
            onLive = { write(it) },
            onSettled = { settle() },
            modifier = Modifier.weight(1f)
        )
        Text(
            /*
             * Gli stop si scrivono con due decimali e il resto come intero: un diaframma è +0,33
             * e una percentuale +33. Un cursore monopolare non ha il segno, che direbbe un verso
             * che non c'è.
             */
            text = when {
                stops -> "%+.2f".format(value)
                unipolar -> "%d".format((value * 100).roundToInt())
                else -> "%+d".format((value * 100).roundToInt())
            },
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.End,
            color = if (abs(value) < DIAL_REST) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .width(KNOB_VALUE)
                .clickable(enabled = enabled) { reset() }
                .semantics { contentDescription = zero }
        )
    }
}

/**
 * La barra di un cursore, disegnata in casa.
 *
 * ⚠️ **Non è uno `Slider` di Material**, per il tondo grosso chiesto dall'utente (voce
 * `luce-taratura`) e soprattutto per il doppio tocco che azzera: uno `Slider` salta al punto al
 * primo tocco, e scriverebbe un passo che nessuno ha chiesto. Qui il salto si scrive dopo aver
 * escluso il doppio tocco. Il trascinamento non aspetta niente.
 *
 * ⚠️ **L'azione semantica serve**: senza `setProgress` il cursore sarebbe muto per il lettore di
 * schermo e invisibile al banco, che i cursori li muove da lì (`LuceTest`).
 */
@Composable
private fun LookDial(
    value: Float,
    span: Float,
    enabled: Boolean,
    onLive: (Float) -> Unit,
    onSettled: () -> Unit,
    modifier: Modifier = Modifier,
    unipolar: Boolean = false
) {
    /**
     * Il fondo corsa: lo zero per i cursori con un verso solo, `-span` per gli altri. Lo leggono
     * la semantica, l'azione, il gesto e il disegno, o il tondo si disegnerebbe dove il dito non
     * lo trova.
     */
    val low = if (unipolar) 0f else -span
    /** L'ampiezza della corsa, cioè quanto vale il tratto da un estremo all'altro. */
    val sweep = span - low
    val ink = MaterialTheme.colorScheme.primary
    val rail = MaterialTheme.colorScheme.surfaceVariant
    val mark = MaterialTheme.colorScheme.outline
    val face = MaterialTheme.colorScheme.surfaceContainerHigh
    val faded = MaterialTheme.colorScheme.onSurface.copy(alpha = OFF_INK)
    /*
     * ⚠️ **Il valore si legge vivo**: il corpo di un `pointerInput` si ricostruisce solo quando
     * cambiano le sue chiavi. Serve a sapere dov'è il tondo quando il dito scende: con un valore
     * vecchio, un tocco sul tondo sembrerebbe lontano e il cursore salterebbe.
     */
    val live by rememberUpdatedState(value)

    Canvas(
        modifier = modifier
            .height(DIAL_ROW)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value, low..span)
                /*
                 * ⚠️ **L'azione c'è anche da spento, e risponde `false`**: dichiarata solo da
                 * acceso, finché l'anteprima si decodificava il banco non trovava i cursori, e su
                 * una macchina più lenta la prova falliva in CI e passava qui. Il lettore di
                 * schermo lo annuncia spento per il `disabled()`.
                 */
                if (!enabled) disabled()
                setProgress { target ->
                    if (!enabled) return@setProgress false
                    onLive(target.coerceIn(low, span))
                    onSettled()
                    true
                }
            }
            .pointerInput(enabled, span, low) {
                if (!enabled) return@pointerInput
                val knob = DIAL_KNOB.toPx()
                /** Il valore che corrisponde a una posizione del dito. */
                fun valueAt(x: Float): Float {
                    val run = (size.width - 2f * knob).coerceAtLeast(1f)
                    val part = ((x - knob) / run).coerceIn(0f, 1f)
                    return low + part * sweep
                }

                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    // Il tondo sta dove dice il valore: toccando lontano da lui si salta subito,
                    // toccandolo si trascina da dove è.
                    val run = (size.width - 2f * knob).coerceAtLeast(1f)
                    val here = knob + ((live - low) / sweep) * run
                    var dragging = abs(down.position.x - here) > knob
                    if (dragging) onLive(valueAt(down.position.x))

                    var alive = true
                    var moved = false
                    while (alive) {
                        val event = awaitPointerEvent()
                        val mine = event.changes.firstOrNull { it.id == down.id }
                        if (mine == null || !mine.pressed) {
                            alive = false
                        } else if (mine.positionChanged()) {
                            if (!dragging &&
                                abs(mine.position.x - down.position.x) > viewConfiguration.touchSlop
                            ) {
                                dragging = true
                            }
                            if (dragging) {
                                moved = true
                                onLive(valueAt(mine.position.x))
                            }
                            mine.consume()
                        }
                    }

                    if (moved) {
                        onSettled()
                    } else {
                        /*
                         * ⚠️ Il passo aspetta di sapere se erano due tocchi: scritto subito, un
                         * doppio tocco lascerebbe nella storia il salto del primo.
                         */
                        val again = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
                            awaitFirstDown()
                        }
                        if (again == null) {
                            onSettled()
                        } else {
                            again.consume()
                            onLive(0f)
                            onSettled()
                            waitForUpOrCancellation()
                        }
                    }
                }
            }
    ) {
        val knob = DIAL_KNOB.toPx()
        val thick = DIAL_RAIL.toPx()
        val middle = size.height / 2f
        val run = (size.width - 2f * knob).coerceAtLeast(1f)
        val part = (value - low) / sweep
        val at = knob + part * run
        val zero = knob + ((0f - low) / sweep) * run
        val hot = if (enabled) ink else faded
        val dead = if (enabled) rail else rail.copy(alpha = 0.5f)

        drawLine(
            dead, Offset(knob, middle), Offset(size.width - knob, middle), thick,
            cap = StrokeCap.Round
        )
        // Il tratto acceso parte dallo zero: il centro su un cursore bipolare, il fondo corsa su
        // uno monopolare.
        drawLine(hot, Offset(zero, middle), Offset(at, middle), thick, cap = StrokeCap.Round)
        // La tacca dello zero, che il tondo copre quando è al centro. Su un cursore monopolare non
        // si disegna: segnerebbe il fondo corsa, che si vede da sé.
        if (!unipolar) {
            drawLine(
                if (enabled) mark else faded,
                Offset(zero, middle - thick),
                Offset(zero, middle + thick),
                DIAL_ZERO.toPx()
            )
        }
        // Un alone del colore del pannello sotto il tondo, così il tondo stacca dalla barra senza
        // bisogno di un'ombra.
        drawCircle(face, knob, Offset(at, middle))
        drawCircle(hot, knob - DIAL_RING.toPx(), Offset(at, middle))
    }
}

/**
 * Il doppio tocco e il dito premuto, su una superficie che non è un tasto.
 *
 * Non usa `detectTapGestures`, che annuncia il tocco lungo e non il rilascio: qui il confronto
 * dura finché il dito si alza, e un secondo gesto per il rilascio si contenderebbe l'evento.
 */
internal fun Modifier.heldOrTwice(
    enabled: Boolean,
    onTwice: () -> Unit,
    onHold: (Boolean) -> Unit
): Modifier = this.pointerInput(enabled) {
    if (!enabled) return@pointerInput
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        val esito = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            settled(down, viewConfiguration.touchSlop)
        }
        if (esito == null) {
            onHold(true)
            /*
             * ⚠️ **Lo spegnimento vive in un `finally`**: il rilevatore è annullato quando il
             * suo `pointerInput` cambia chiave (basta un salvataggio che spegne i cursori), e
             * un'attesa annullata non torna alla riga dopo. Senza, il confronto resterebbe acceso.
             */
            try {
                waitForUpOrCancellation()
            } finally {
                onHold(false)
            }
            return@awaitEachGesture
        }
        if (esito != Settled.UP) return@awaitEachGesture
        val again = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) { awaitFirstDown() }
        if (again != null) {
            again.consume()
            onTwice()
            waitForUpOrCancellation()
        }
    }
}

/**
 * Il minimo della colonna dei nomi dei cursori (la misura vive in [knobNameWidth]): in una
 * lingua dai nomi corti la colonna non si stringe, e la barra parte sempre dallo stesso punto.
 */
private val KNOB_NAME_MIN = 84.dp

/**
 * Il tetto della colonna dei nomi: oltre, un nome va a capo come faceva prima.
 *
 * La misura cresce col testo, e senza tetto la colonna si prenderebbe mezza riga alla scala
 * massima dei caratteri: un nome su due righe si legge, un cursore di un centimetro non si usa.
 * Il numero copre il nome più largo di oggi, lo swahili 'Kichujio cha B/W', una novantina di punti
 * in Roboto.
 */
private val KNOB_NAME_MAX = 132.dp

/**
 * Quanto è larga la colonna dei nomi dei cursori: quanto il più largo di tutti, col carattere e la
 * scala di **questo** telefono.
 *
 * ⚠️ **Si misura e non si scrive** (punto B del giro della `2.36`: 'Mascheratura' per esteso,
 * senza andare a capo). 'Quanto basta' non è un numero: in Roboto 'Mascheratura' chiede 79 punti su
 * 84 e va a capo appena il testo cresce di un decimo, e su ventotto lingue quattordici nomi
 * superano gli 84 punti a scala uno (il francese 'Hautes lumières' ne chiede 93). Accorciare una
 * parola cura una lingua sola, allargare di un numero fisso una scala sola.
 * La misura la fa il telefono: `rememberTextMeasurer` usa il carattere e la scala in vigore.
 *
 * Si misurano i nomi di tutti i moduli, o la colonna cambierebbe larghezza a ogni gettone. Le
 * otto fasce dell'HSL hanno gli stessi tre nomi, quindi basta la prima.
 */
@Composable
private fun knobNameWidth(): Dp {
    val measurer = rememberTextMeasurer()
    val stile = MaterialTheme.typography.bodySmall
    val nomi = MODULES.flatMap { it.rows(0) }.map { stringResource(it.name) }
    val density = LocalDensity.current
    return remember(measurer, stile, nomi, density) {
        val largo = nomi.maxOfOrNull { measurer.measure(it, stile).size.width } ?: 0
        with(density) { largo.toDp() }.coerceIn(KNOB_NAME_MIN, KNOB_NAME_MAX)
    }
}

/** Il punto che dice 'questo modulo ha toccato l'immagine', nel gettone della fila. */
private val MODULE_MARK = 6.dp

/**
 * L'aria ai fianchi di un gettone **scritto**, cioè dei quattro canali delle Curve.
 *
 * Il gettone a icona non ne ha: là la larghezza la divide la fila, e un rientro toglierebbe solo
 * area di tocco.
 */
private val MODULE_SIDE = 14.dp

/**
 * L'aria sopra e sotto il glifo di un gettone dei moduli.
 *
 * Non è privata perché la legge anche la replica che riordina la fila nelle impostazioni, dove
 * l'altezza di una cella è questo numero più il glifo.
 */
internal val MODULE_PAD = 8.dp

/**
 * L'altezza di una pastiglia della fila delle fasce.
 *
 * È l'area di tocco e non il disegno, che contiene il tondo, l'anello e il punto.
 */
private val BAND_ROW = 44.dp

/** Il diametro del tondo di una fascia. */
private val BAND_DOT = 22.dp

/** Quanto l'anello di 'scelta' sta fuori dal tondo, e il punto sotto di lui. */
private val BAND_GAP = 3.dp

/** Lo spessore dell'anello di 'scelta'. */
private val BAND_RING = 2.dp

/**
 * Quanto sono saturi e chiari i tondi della fila.
 *
 * Non è il colore che il conto vedrà: la pastiglia dice quale colore si sceglie, quindi spinge
 * la tonalità alla massima riconoscibilità.
 */
private const val BAND_SAT = 0.85f
private const val BAND_VAL = 0.95f

/**
 * Il colore con cui si disegna la fascia della tonalità [hue], in giri.
 *
 * La tonalità viene dai centri di `Mix`: con due elenchi, una pastiglia verde potrebbe chiamarsi
 * 'Blu'.
 */
private fun bandTint(hue: Float): Color = Color.hsv(hue * 360f, BAND_SAT, BAND_VAL)

/**
 * Quanto è alto il grafico della curva.
 *
 * Non è quadrato (vedi [CurveBoard]): questo numero lascia al palco più di metà schermo con le
 * Curve aperte.
 */
private val BOARD_H = 190.dp

/** Lo stondamento del riquadro del grafico: quello delle altre superfici di questa scheda. */
private val BOARD_ROUND = 10.dp

/** Il raggio del tondo di un punto della curva. */
private val BOARD_DOT = 6.dp

/** Lo spessore della griglia del grafico; la curva ne vale il doppio. */
private val BOARD_LINE = 1.5.dp

/**
 * L'aria fra il bordo del riquadro e il disegno.
 *
 * Vale più di [BOARD_DOT]: gli estremi della curva cadono sui bordi, e il loro tondo verrebbe
 * tagliato a metà.
 */
private val BOARD_PAD = 8.dp

/** Quanto è larga la colonna del numero: ci deve stare `-100` col segno. */
private val KNOB_VALUE = 44.dp

/**
 * L'aria fra le tre colonne di una riga di cursore: il nome, la barra e il numero.
 *
 * Segnalazione dell'utente: tutto troppo attaccato. La colonna dei nomi è larga quanto il nome
 * più largo, che arrivava a filo del tondo. Lo spazio lo paga la barra, l'unica parte della riga
 * che può cedere senza troncare niente.
 */
private val KNOB_GAP = 12.dp

/**
 * L'altezza di una riga di cursore.
 *
 * È l'area di tocco e non l'altezza del disegno: il tondo è alto la metà, e il resto serve perché
 * il dito prenda la barra senza centrarla.
 *
 * ⚠️ **Il modulo più alto è il Disegno** (misurato col banco: 244 punti, poi le Curve a 238 e la
 * Luce a 216, uguali a 360, 390 e 600 punti di larghezza): un punto tolto qui accorcia la scheda
 * solo se accorcia il Disegno. Sotto questo numero non si scende: è già sotto i 48 punti di
 * Material.
 */
private val DIAL_ROW = 36.dp

/** Il raggio del tondo, la misura che l'utente ha chiesto di far crescere. */
private val DIAL_KNOB = 11.dp

/** Lo spessore della barra. */
private val DIAL_RAIL = 4.dp

/** Quanto il tondo si stacca dal pannello: è l'alone che sostituisce un'ombra. */
private val DIAL_RING = 2.dp

/** Lo spessore della tacca dello zero. */
private val DIAL_ZERO = 1.5.dp

/**
 * Dove arriva il doppio tocco sull'immagine.
 *
 * Due volte: il doppio tocco serve a guardare un dettaglio, poi si continua con le dita; un salto
 * più lungo porterebbe fuori dal punto voluto.
 */
private const val ZOOM_TAP = 2f

/**
 * Il tetto dell'ingrandimento.
 *
 * Lo decide l'anteprima: si vede la riduzione con cui l'editor lavora in fretta, e oltre questo
 * ingrandimento si guarderebbero i pixel di quella e non dell'immagine.
 */
private const val ZOOM_MAX = 6f

/**
 * Quanto dito serve per raddoppiare l'ingrandimento a una mano.
 *
 * Da uno a [ZOOM_MAX] ci sono due raddoppi e mezzo, quindi l'intera corsa entra in poco più di un
 * terzo di schermo, un trascinamento che il pollice fa senza staccarsi.
 */
private val ZOOM_PULL = 96.dp

/**
 * Quanto dura la corsa del doppio tocco, in millisecondi.
 *
 * Si valuta sul telefono: il banco misura che la corsa esista e dove finisca, non come si
 * percepisce. È un filo più lunga della dissolvenza fra due schermate (180 ms), perché il
 * movimento è più grande.
 */
private const val ZOOM_RIDE = 220

/**
 * L'aria intorno all'immagine, che serve alle squadrette del Ritaglio e vale in tutti i moduli
 * (vedi `air()`, nel palco, e [fitted]).
 *
 * ⚠️ **Si ricava dalla squadretta**: il tracciato di `bracket` corre a mezzo spessore fuori dal
 * rettangolo, con l'alone intorno, quindi l'inchiostro arriva a `HANDLE_THICK + GRIP_HALO` oltre
 * il bordo. Un numero scritto a mano taglierebbe le maniglie al primo ritocco.
 */
private val CROP_AIR = HANDLE_THICK + GRIP_HALO

/**
 * Un testo che si sta scrivendo: [at] è il punto dell'immagine originale dove va uno nuovo,
 * [index] il posto di quello scelto che si modifica, [text] le parole fin qui.
 */
internal data class TextAsk(val at: Offset?, val index: Int?, val text: String)

/**
 * L'ultimo tocco su un elemento del Disegno: quale elemento, quando il dito si è alzato, e se il
 * tocco ha scambiato 'Trasforma' e 'Ruota', così un doppio tocco lo può disfare.
 * ⚠️ Non è uno stato: il palco lo legge al tocco dopo, quindi scriverlo non ricompone niente.
 */
private class MarkTap {
    var index: Int? = null
    var up: Long = 0L
    var turned: Boolean = false

    fun set(index: Int?, up: Long, turned: Boolean) {
        this.index = index
        this.up = up
        this.turned = turned
    }
}

/**
 * La finestra in cui si scrive un testo (G3): un campo su più righe, perché un testo va a capo
 * solo dove lo mette Invio (lettura `A1`), e i tasti di [NewFolderDialog], con 'Applica' che
 * aspetta una lettera.
 * ⚠️ Per una pastiglia e un pannello 'Applica' accetta anche il campo vuoto ([blank]; nota C sul
 * giro della `4.91`): il riquadro resta, senza parole. Un testo è le sue parole, quindi per un
 * testo aspetta una lettera.
 * ⚠️ È modale, con le due righe di ogni finestra che raccoglie un input scritto (`Rules.md`,
 * § '👆 Che cosa fa il tocco FUORI da una finestra'), e la tastiera sale con lei.
 */
@Composable
private fun TextDialog(initial: String, blank: Boolean, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var text by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    val fuoco = remember { FocusRequester() }
    LaunchedEffect(Unit) { fuoco.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(null),
        properties = loweredWindow(null),
        title = { Text(stringResource(R.string.draw_text)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = false,
                    shape = BOX_SHAPE,
                    modifier = Modifier.fillMaxWidth().focusRequester(fuoco)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onDone(if (text.text.isBlank()) "" else text.text) },
                enabled = blank || text.text.isNotBlank()
            ) { Text(stringResource(R.string.editor_apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * I tasti degli strumenti Testo, Pastiglia e Pannello (G3), sotto gli strumenti: 'Carattere',
 * 'Grassetto', 'Corsivo', 'Barrato', 'Sfondo', 'Allineamento', 'Testo' ed 'Elimina' ([delete]).
 * Regolano il prossimo testo o pastiglia, o quello scelto, come i tasti degli altri strumenti. Con
 * la pastiglia e il pannello ([pill]) 'Sfondo' è spento: sono il loro sfondo.
 * - 'Carattere' passa al successivo dei quattro caratteri a ogni tocco, e mostra il suo 'Aa'
 *   (lettura della sessione, dichiarata nella voce di collaudo).
 * - 'Grassetto', 'Corsivo', 'Barrato' e 'Sfondo' sono interruttori e si combinano (lettura `A3`);
 *   'Sfondo' posa la striscia dell'etichetta.
 * - 'Allineamento' va dal centro a sinistra a destra (`B2` e nota A sul giro della `4.91`), e
 *   mostra come sono allineate le righe; vale anche per la pastiglia e il pannello.
 * - 'Testo' apre la finestra delle parole del testo scelto, ed è l'ultima icona prima di
 *   'Elimina' (nota E sul giro della `4.90`).
 */
@Composable
private fun TextKeys(look: Look, gaze: Gaze, live: Boolean, pill: Boolean, delete: @Composable (Modifier) -> Unit) {
    val segno = gaze.picked?.let { look.drawing.marks.getOrNull(it) }?.takeIf { it.pen.written }
    val faccia = gaze.face.label
    val carattere = stringResource(R.string.draw_face)
    val famiglia = remember(gaze.face, Faces.loads) { faceFamily(gaze.face) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TextKey("$carattere: $faccia", selected = false, enabled = live, toggle = false,
            onClick = { gaze.face = Face.entries[(gaze.face.ordinal + 1) % Face.entries.size] },
            modifier = Modifier.weight(1f)) { tinta ->
            Text("Aa", color = tinta, fontFamily = famiglia, fontSize = KEY_GLYPH)
        }
        TextKey(stringResource(R.string.draw_bold), selected = gaze.bold, enabled = live, toggle = true,
            onClick = { gaze.bold = !gaze.bold }, modifier = Modifier.weight(1f)) { tinta ->
            Text("B", color = tinta, fontWeight = FontWeight.Bold, fontSize = KEY_GLYPH)
        }
        TextKey(stringResource(R.string.draw_italic), selected = gaze.italic, enabled = live, toggle = true,
            onClick = { gaze.italic = !gaze.italic }, modifier = Modifier.weight(1f)) { tinta ->
            Text("I", color = tinta, fontStyle = FontStyle.Italic, fontFamily = FontFamily.Serif, fontSize = KEY_GLYPH)
        }
        TextKey(stringResource(R.string.draw_strike), selected = gaze.strike, enabled = live, toggle = true,
            onClick = { gaze.strike = !gaze.strike }, modifier = Modifier.weight(1f)) { tinta ->
            Text("S", color = tinta, textDecoration = TextDecoration.LineThrough, fontSize = KEY_GLYPH)
        }
        val sfondo = gaze.label && !pill
        TextKey(stringResource(R.string.draw_ground), selected = sfondo, enabled = live && !pill, toggle = true,
            onClick = { gaze.switchLabel() },
            modifier = Modifier.weight(1f)) { tinta ->
            GroundGlyph(if (sfondo) Color(gaze.labelInk) else tinta)
        }
        val allineamento = stringResource(R.string.draw_align)
        val dove = when (gaze.align) {
            Align.LEFT -> stringResource(R.string.settings_left)
            Align.CENTER -> stringResource(R.string.draw_center)
            Align.RIGHT -> stringResource(R.string.settings_right)
        }
        TextKey("$allineamento: $dove", selected = false, enabled = live, toggle = false,
            onClick = { gaze.align = gaze.align.next },
            modifier = Modifier.weight(1f)) { tinta ->
            Icon(Glyphs.align(gaze.align), contentDescription = null, tint = tinta)
        }
        TextKey(stringResource(R.string.draw_text_edit), selected = false, enabled = live && segno != null, toggle = false,
            onClick = { gaze.picked?.let { gaze.askWords(look.drawing, it) } },
            modifier = Modifier.weight(1f)) { tinta ->
            Icon(Glyphs.Material.Edit, contentDescription = null, tint = tinta)
        }
        delete(Modifier.weight(1f))
    }
}

/**
 * Il tasto 'Elimina', l'ultimo a destra di ogni fila di tasti (nota B sul giro della `4.94`, che
 * toccava per errore i tasti scritti, e risposta `B1`). Un tocco elimina l'elemento scelto, il
 * tocco lungo li elimina tutti, come il gettone del modulo.
 * ⚠️ È acceso finché il disegno ha un elemento, così il tocco lungo funziona anche senza scelta; un
 * tocco senza scelta non fa niente (lettura della sessione, dichiarata nella voce di collaudo).
 */
@Composable
private fun DeleteKey(
    look: Look,
    gaze: Gaze,
    live: Boolean,
    onLive: ((Look) -> Look) -> Unit,
    onSettled: () -> Unit,
    modifier: Modifier
) {
    TextKey(
        stringResource(R.string.pick_delete), selected = false, enabled = live && !look.drawing.idle, toggle = false,
        onClick = {
            val i = gaze.picked ?: return@TextKey
            gaze.picked = null
            onLive { l -> l.copy(drawing = l.drawing.without(i)) }
            onSettled()
        },
        onLongClick = {
            gaze.picked = null
            onLive { it.copy(drawing = Drawing.NONE) }
            onSettled()
        },
        longName = stringResource(R.string.draw_clear),
        modifier = modifier
    ) { tinta -> Icon(Glyphs.PickDelete, contentDescription = null, tint = tinta) }
}

/**
 * Un tasto del Disegno (strumenti, tasti del testo, 'Elimina', e i tasti disegnati di [ArtKey]):
 * forma, colori e altezza sono del `FilterChip`, e vivono solo qui, così tutte le file si leggono
 * uguali; la parola resta la descrizione per il lettore di schermo, e [content] disegna il tasto
 * nel colore che riceve.
 */
@Composable
private fun TextKey(
    name: String,
    selected: Boolean,
    enabled: Boolean,
    toggle: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    choice: Boolean = false,
    /** Un secondo gesto, il tocco lungo, e come lo chiama il lettore di schermo ('Elimina tutto'). */
    onLongClick: (() -> Unit)? = null,
    longName: String? = null,
    content: @Composable (Color) -> Unit
) {
    val forma = FilterChipDefaults.shape
    val schema = MaterialTheme.colorScheme
    // ⚠️ Un interruttore, una scelta fra molte (gli strumenti), un tasto, o un tasto col tocco lungo.
    val scelta = Modifier
        .then(
            when {
                toggle -> Modifier.toggleable(selected, enabled = enabled, role = Role.Switch) { onClick() }
                choice -> Modifier.selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
                onLongClick != null -> Modifier.combinedClickable(
                    enabled = enabled, role = Role.Button, onLongClickLabel = longName,
                    onLongClick = withHaptics(onLongClick), hapticFeedbackEnabled = false,
                    onClick = onClick
                )
                else -> Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            }
        )
        .semantics { contentDescription = name }
    Box(
        modifier
            .height(FilterChipDefaults.Height)
            .alpha(if (enabled) 1f else OFF_INK)
            .clip(forma)
            .background(if (selected) schema.secondaryContainer else Color.Transparent)
            .then(if (selected) Modifier else Modifier.border(1.dp, schema.outlineVariant, forma))
            .then(scelta),
        contentAlignment = Alignment.Center
    ) {
        content(if (selected) schema.primary else schema.onSurfaceVariant)
        /*
         * Un bordo pieno attorno al tasto scelto (nota A sul giro della `4.60`): i disegni arrivano
         * ai bordi dei tasti, e la sola tinta si leggeva male.
         * ⚠️ Sopra il contenuto e non sotto: le bande di 'Traccia' e 'Tratteggio' vanno da bordo a
         * bordo, e sotto di loro il bordo si interromperebbe ai due lati.
         */
        if (selected) Box(Modifier.matchParentSize().border(KEY_RIM, schema.primary, forma))
    }
}

/**
 * Le strisce di sfondo offerte per il testo (nota dell'utente: da 4 a 8 colori che contrastano a
 * sufficienza): le sei di [Draw.LABEL_INKS]. Quelle che non si staccano dall'immagine sotto il
 * testo, o non lasciano leggibili le parole ([Draw.readable], con [Gaze.under]), sono spente:
 * restano visibili sbiadite, così la fila non cambia lunghezza sotto il dito.
 * Sono rettangoli affiancati, senza spazio e bassi (nota E sul giro della `4.91`). Quella scelta
 * ha dentro l'anello del tondo scelto.
 */
@Composable
private fun GroundRow(gaze: Gaze, live: Boolean) {
    val voci = Draw.LABEL_INKS
    val parole = Draw.lit(gaze.textInk, gaze.textLight)
    val fondo = stringResource(R.string.draw_ground)
    val bordo = MaterialTheme.colorScheme.onSurface
    val dentro = MaterialTheme.colorScheme.surface
    Row(modifier = Modifier.fillMaxWidth().height(STRIP).clip(RoundedCornerShape(STRIP_ROUND))) {
        voci.forEachIndexed { i, ink ->
            val scelto = gaze.labelInk == ink
            val leggibile = scelto || Draw.readable(ink, gaze.under, parole)
            val attivo = live && leggibile
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .alpha(if (leggibile) 1f else OFF_SWATCHES)
                    .selectable(scelto, enabled = attivo, role = Role.RadioButton) {
                        gaze.labelInk = ink
                    }
                    .semantics { contentDescription = "$fondo ${i + 1}" }
                    .drawBehind {
                        if (!scelto) {
                            drawRect(Color(ink))
                            return@drawBehind
                        }
                        /*
                         * Tre rettangoli uno dentro l'altro (nota sul giro della `4.93`, con
                         * disegno): l'anello scuro riempie la striscia, il filo chiaro e il colore
                         * sono stondati, concentrici con l'angolo della fila ([STRIP_ROUND]).
                         */
                        val anello = SWATCH_RING.toPx()
                        val filo = anello / 2f
                        val raggio = (STRIP_ROUND.toPx() - anello).coerceAtLeast(filo)
                        drawRect(bordo)
                        drawRoundRect(dentro, topLeft = Offset(anello, anello),
                            size = Size(size.width - 2 * anello, size.height - 2 * anello), cornerRadius = CornerRadius(raggio))
                        drawRoundRect(Color(ink), topLeft = Offset(anello + filo, anello + filo),
                            size = Size(size.width - 2 * (anello + filo), size.height - 2 * (anello + filo)),
                            cornerRadius = CornerRadius(raggio - filo))
                    }
            )
        }
    }
}

/**
 * Il glifo del tasto 'Sfondo' (nota F sul giro della `4.91`: un rettangolino stondato col testo
 * in negativo): un rettangolo stondato di [colour], quello della striscia quando il tasto è
 * acceso, con le lettere ritagliate, da cui si vede il fondo del tasto.
 * ⚠️ Il ritaglio vuole un livello suo ([CompositingStrategy.Offscreen]): sul tasto, le lettere
 * taglierebbero anche il suo fondo.
 */
@Composable
private fun GroundGlyph(colour: Color) {
    val misura = rememberTextMeasurer()
    val stile = TextStyle(fontSize = GROUND_GLYPH_TEXT, fontWeight = FontWeight.Bold)
    Canvas(
        Modifier
            .size(GROUND_GLYPH_W, GROUND_GLYPH_H)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        drawRoundRect(colour, cornerRadius = CornerRadius(GROUND_GLYPH_ROUND.toPx()))
        val lettere = misura.measure("Aa", stile)
        drawText(
            lettere, color = Color.Black,
            topLeft = Offset((size.width - lettere.size.width) / 2f, (size.height - lettere.size.height) / 2f),
            blendMode = BlendMode.DstOut
        )
    }
}

/** Le misure del glifo di 'Sfondo': scelta della sessione, dichiarata nella voce di collaudo. */
private val GROUND_GLYPH_W = 30.dp
private val GROUND_GLYPH_H = 20.dp
private val GROUND_GLYPH_ROUND = 5.dp
private val GROUND_GLYPH_TEXT = 13.sp

/**
 * L'altezza delle strisce e lo stondamento della loro fila (nota E sul giro della `4.91`): più
 * basse dei tondi della tavolozza ([SWATCH]), così il cursore sale poco. Scelte della sessione,
 * dichiarate nella voce di collaudo.
 */
private val STRIP = 20.dp
private val STRIP_ROUND = 6.dp

/**
 * L'aria fra due file del Disegno (bozza dell'utente sul giro della `4.93`, dove gli spazi misurano
 * da 11 a 15dp). Scelta della sessione, dichiarata nella voce di collaudo.
 */
private val DRAW_AIR = 12.dp

/** Quanto le strisce restano lontane dalla barra sotto il modulo, oltre [DRAW_AIR] (bozza dell'utente). */
private val DRAW_FOOT = 8.dp

/**
 * Il carattere dell''Aa' sul tasto 'Carattere': quello con cui dipinge il disegno, regolare e
 * diritto, avvolto per Compose.
 * ⚠️ **Non un carattere di risorsa di Compose col peso come variazione**: sul banco, appena si
 * misurava un altro testo (il campo della finestra delle parole), l'editor non si fermava più e la
 * finestra non compariva. Un solo caricatore per i quattro caratteri, [Faces].
 */
private fun faceFamily(face: Face): FontFamily = FontFamily(Typeface(Faces.of(face, italic = false, bold = false)))

/** Il corpo delle lettere sui tasti dello strumento Testo. */
private val KEY_GLYPH = 18.sp

/**
 * La misura dei glifi degli strumenti (nota B): 28dp in un tasto alto 32, contro i 24dp di
 * Material, che i chip stringevano ancora. Scelta della sessione, dichiarata nella voce di collaudo.
 */
private val PEN_GLYPH = 28.dp

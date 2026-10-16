package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * L'editor semplice: si gira di novanta gradi e si ritaglia, e basta.
 *
 * Il perimetro è dichiarato (richiesta dell'utente): niente filtri, luminosità o pennelli, che
 * sono dell'editor completo o di un'app scelta dalla stessa schermata. Qui c'è il minimo per
 * raddrizzare un'immagine e togliere il bordo di troppo.
 *
 * ⚠️ **L'anteprima è rimpicciolita, il salvataggio no**: si lavora su una copia campionata, e il
 * rettangolo si tiene in frazioni del lato e non in pixel; al salvataggio si applica al file vero
 * (`ImageEdit`).
 */
@Composable
fun EditorScreen(
    uri: Uri,
    /** Se una scrittura è in corso: i comandi si spengono, o si salverebbe due volte. */
    busy: Boolean,
    /**
     * Se una filigrana è pronta da scrivere, cioè se il salvataggio ha qualcosa da fare anche su
     * un'immagine intatta (`ViewerViewModel.markReady`).
     */
    marked: Boolean,
    /** Se l'interruttore della filigrana è acceso: il tasto 'Filigrana' prende l'accento. */
    marking: Boolean,
    /** Se un logo è stato scelto: senza, il tasto 'Filigrana' non c'è. */
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
     * Il ridimensionamento predefinito: alla finestra serve per disattivare 'Rendi predefinito'
     * quando il piano è già quello.
     */
    saved: Resize.Plan,
    /** Se quel piano si applica al salvataggio, cioè l'interruttore di 'Ridimensiona'. */
    resizing: Boolean,
    /** `null` spegne; un piano lo scrive e accende, come 'Applica' della sua finestra. */
    onResize: (Resize.Plan?) -> Unit,
    /** Il piano diventa il predefinito: il comando 'Rendi predefinito'. */
    onResizeDefault: (Resize.Plan) -> Unit,
    /**
     * Che cosa salvare.
     *
     * ⚠️ **Il lavoro lo fa chi chiama**: una scrittura da venti megapixel dura secondi, e appesa
     * alla composizione si interromperebbe chiudendo la schermata. Nello scope del modello arriva
     * in fondo.
     * Sovrascrivere o copiare lo decide il modello dal formato del file, non questa schermata.
     */
    onSave: (turns: Int, mirror: Boolean, crop: ImageEdit.Crop) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    /*
     * ⚠️ **L'editor è in verticale finché si è qui** (risposta dell'utente a `d-rotazione`,
     * 1.69): un editor in orizzontale sul telefono è inusabile. Verticale e non
     * `SCREEN_ORIENTATION_LOCKED`, che congelerebbe l'orientamento corrente, anche orizzontale.
     * Uscendo si rimette il valore letto all'ingresso, senza scrivere `UNSPECIFIED` alla cieca.
     * L'attività si trova risalendo i contesti ([Knobs.activityOf]).
     */
    val activity = remember(context) { Knobs.activityOf(context) }
    /*
     * ⚠️ **Da 1.024 dp il verticale non si impone** (voce `3.38-01`): con gli strumenti a lato
     * ([Adaptive.editorBeside]) l'orizzontale è il layout previsto, e forzare `PORTRAIT` su un
     * tablet orizzontale farebbe ruotare la finestra e oscillare la larghezza intorno alla soglia,
     * con un layout che sfarfalla.
     */
    val widthDp = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width.toDp()
    }.value.toInt()
    val lockPortrait = !Adaptive.editorBeside(widthDp)
    DisposableEffect(activity, lockPortrait) {
        if (!lockPortrait) return@DisposableEffect onDispose { }
        val prima = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            if (activity != null && prima != null) activity.requestedOrientation = prima
        }
    }
    var origin by remember(uri) { mutableStateOf<Bitmap?>(null) }

    /**
     * I passi già confermati con 'Applica', dal primo all'ultimo.
     *
     * ⚠️⚠️ **Ogni passo contiene la trasformazione composta dall'originale, non la propria**: al
     * salvataggio serve una posa e un rettangolo, che è quello che `ImageEdit` sa applicare al
     * file vero. Il conto è esatto ([after]).
     * Contiene anche la sua anteprima: 'Annulla' la ritrova senza una nuova decodifica.
     */
    var steps by remember(uri) { mutableStateOf<List<Step>>(emptyList()) }

    /**
     * I passi disfatti con 'Annulla', pronti a tornare con 'Ripristina'.
     *
     * ⚠️ **'Ripristina' vuol dire un passo avanti, non 'com'era all'inizio'** (chiarimento
     * dell'utente): il ritorno all'originale è il tasto 'Originale', con l'icona del riavvio.
     * Un 'Applica' nuovo svuota l'elenco, come in ogni editor.
     */
    var undone by remember(uri) { mutableStateOf<List<Step>>(emptyList()) }

    /** L'immagine su cui si sta lavorando adesso: l'ultimo passo, o l'originale. */
    val base = steps.lastOrNull()?.preview ?: origin

    /*
     * ⚠️ **Uno stato solo per la posa**: rotazione e riflessione non sono indipendenti, perché
     * uno specchio davanti a una rotazione la rovescia. Il conto lo fa `Spin.then`, una volta.
     */
    var spin by remember(base) { mutableStateOf(Spin.STILL) }
    var shape by remember(base) { mutableStateOf(Shape.FREE) }
    var crop by remember(base) { mutableStateOf(ImageEdit.Crop.WHOLE) }

    BackHandler { onBack() }

    LaunchedEffect(uri) {
        origin = withContext(Dispatchers.IO) { preview(context, uri) }
    }


    // L'anteprima girata si ricalcola solo quando cambia la posa, non a ogni movimento del dito
    // sul rettangolo.
    val shown: ImageBitmap? = remember(base, spin) {
        // Passa da [spunBy], condivisa coi due salvataggi, che tiene fuori dalla composizione un
        // errore di memoria su un'immagine grossa.
        base?.spunBy(spin.turns, spin.mirror)?.asImageBitmap()
    }

    /*
     * ⚠️ **L'orientamento della selezione lo decide l'immagine, e il quadrato conta come
     * verticale** (richiesta dell'utente). La chiave è [base] e non [uri]: prima dell'anteprima
     * non si sa che forma abbia.
     * Girare l'immagine non lo cambia: dopo il primo tocco l'orientamento è una decisione
     * dell'utente.
     */
    var lay by remember(base) { mutableStateOf(startLay(base)) }

    /**
     * Quante volte l'immagine mostrata è più larga che alta.
     *
     * Si ricava da [shown] e non dal riquadro disegnato, che dà lo stesso numero: i tasti fanno i
     * loro conti senza sapere dov'è l'immagine sullo schermo.
     */
    val aspect = shown?.let { it.width.toFloat() / it.height } ?: 1f

    /** Se c'è qualcosa di non ancora confermato con 'Applica'. */
    val pending = spin != Spin.STILL || !crop.whole

    /** Tutto quello che si è fatto finora, composto in una posa e un rettangolo soli. */
    val total = after(steps.lastOrNull()?.done ?: Done.NOTHING, spin, crop)

    /*
     * ⚠️ **Le proporzioni vengono da [origin] e non da [base]**: il salvataggio applica il totale
     * dei passi all'immagine di partenza, e posa e ritaglio sono in [total]. Perché lato lungo e
     * forma vengano da due fonti è detto su [Resize.frameSize].
     */
    val longSide = rememberLongSide(uri)
    val frame = origin?.let { partenza ->
        longSide?.let {
            Resize.frameSize(it, partenza.width, partenza.height, total.spin.turns, total.crop)
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
     * ⚠️ **Il rientro di sistema non è sulla colonna intera** (voce `sotto-barra` dell'utente):
     * toglierebbe spazio alla scheda, che si fermerebbe sopra la barra di sistema. Ogni pezzo
     * prende i suoi lati: la testata quello in alto, la scheda quello in fondo, e i fianchi tutti
     * e tre (in orizzontale il ritaglio del display è di lato). Così la scheda arriva al bordo col
     * suo colore.
     * `safeDrawing` e non `systemBars`: comprende anche il ritaglio del display e la tastiera, e
     * questa schermata dichiara `shortEdges`.
     */
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                    )
                )
                /*
                 * ⚠️ **I due lati hanno rientri diversi: uno ha un'icona, l'altro una parola.**
                 * Il testo di 'Salva' si allinea a [STAGE_SIDE], dove cominciano palco e file di
                 * gettoni (riscontro dell'utente), togliendo i 12 dp che il tasto ha dentro
                 * ([TEXT_BUTTON_PAD]).
                 * A sinistra 4 dp, quindi la freccia cade a 16 dp dal bordo: è l'allineamento
                 * ottico che Material dà a un'icona di navigazione.
                 */
                .padding(start = 4.dp, end = STAGE_SIDE - TEXT_BUTTON_PAD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back))
            }
            /*
             * ⚠️ **Il titolo è 'Modifica immagine', la stessa stringa dell'editor completo**
             * (istruzione dell'utente, 2.20): la differenza fra i due editor conta solo quando si
             * sceglie, non mentre si lavora.
             * Il corpo è `headlineSmall`, come in tutte le altre schermate: misurato, il titolo
             * entra in tutte le lingue anche accanto a 'Salva' (caso peggiore, telugu su 320 dp,
             * con 45 dp di avanzo).
             */
            Text(
                text = stringResource(R.string.editor_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f).heading()
            )
            /*
             * ⚠️ **'Salva' sovrascrive senza chiedere** (richiesta dell'utente, 1.08): la copia di
             * sicurezza nel cestino rende la sovrascrittura reversibile. Dove i pixel non si
             * riscrivono (un HEIC) il modello salva un JPEG accanto e l'avviso lo dice
             * (`ImageEdit.canOverwrite`).
             * ⚠️ **Salva il totale dei passi, non l'ultimo ritocco**: `turns` e `crop` da soli
             * perderebbero tutto quello che è stato confermato con 'Applica'.
             */
            TextButton(
                onClick = { onSave(total.spin.turns, total.spin.mirror, total.crop) },
                // ⚠️ Una filigrana, o un ridimensionamento che rimpicciolisce davvero, è lavoro da
                // salvare anche senza posa né ritaglio: senza questa condizione il tasto
                // resterebbe spento.
                enabled = shown != null && !busy &&
                    (marked || shrinks || !(total.spin == Spin.STILL && total.crop.whole))
            ) {
                Text(stringResource(R.string.editor_save))
            }
        }

        // Da 1.024 dp gli strumenti sono a lato (mockup editor/simple); sotto, la scheda resta in
        // basso. La larghezza è quella della finestra, letta sopra per l'orientamento.
        val beside = Adaptive.editorBeside(widthDp)
        // Con la mano destra (FAB a destra) i comandi sono a destra e il palco a sinistra; con la
        // sinistra si scambiano (voce `3.39-01`).
            val panelOnStart = LocalPadLook.current.hand == Hand.LEFT
        val toolsWidth = Adaptive.editorToolsWidth(widthDp)

        @Composable
        fun Sheet() {
        EditorSheet(
            beside = beside,
            panelOnStart = panelOnStart,
            toolsWidth = toolsWidth,
            shape = shape,
            lay = lay,
            busy = busy,
            ready = shown != null,
            pending = pending,
            applied = steps.isNotEmpty(),
            undone = undone.isNotEmpty(),
            // I due comandi del salvataggio arrivano alla scheda come uno spazio da riempire: sono
            // nella sua barra, ma quello che governano (logo, interruttori, piano, finestra) è qui.
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
                    onResizeSetup = { asking = true }
                )
            },
            onShape = { one ->
                /*
                 * ⚠️ **La selezione tiene il posto anche al cambio di proporzione** (richiesta
                 * dell'utente, 1.17), come al cambio di orientamento.
                 * Ritoccare la proporzione già scelta la rimette intera: è la via rapida per una
                 * selezione ridotta per sbaglio, senza azzerare i passi confermati.
                 */
                crop = if (one == shape) {
                    one.fit(aspect, lay)
                } else {
                    reshaped(crop, aspect, one.value(lay, aspect))
                }
                shape = one
            },
            onLay = { one ->
                if (one != lay) {
                    crop = flipped(crop, aspect)
                    lay = one
                }
            },
            onTurn = { way ->
                spin = spin.then(Spin(way.mod(4), false))
                // ⚠️ La proporzione si rifà sull'aspetto nuovo, il reciproco di quello di adesso:
                // con quello vecchio il rettangolo sarebbe storto per un fotogramma.
                crop = shape.fit(1f / aspect, lay)
            },
            /*
             * ⚠️ **Riflettere non rifà il ritaglio, girare sì** (richiesta dell'utente): uno
             * specchio lascia i lati come sono, quindi il rettangolo si ribalta e resta sulla
             * stessa porzione di immagine. Lo ribalta [spunRect], la funzione che compone i passi,
             * così rettangolo e immagine si muovono insieme per costruzione.
             */
            onFlip = { down ->
                val gesto = if (down) Spin.DOWN else Spin.ACROSS
                spin = spin.then(gesto)
                crop = spunRect(crop, gesto)
            },
            onCentreAcross = { crop = centredAcross(crop) },
            onCentreDown = { crop = centredDown(crop) },
            onCentreBoth = { crop = centredDown(centredAcross(crop)) },
            onApply = {
                val picture = base ?: return@EditorSheet
                steps = steps + applied(picture, steps.lastOrNull()?.done ?: Done.NOTHING, spin, crop)
                undone = emptyList()
            },
            onUndo = {
                // ⚠️ Un passo indietro solo, ed è quello che si vede: il ritocco non confermato, se
                // c'è, altrimenti l'ultimo 'Applica'.
                if (pending) {
                    spin = Spin.STILL
                    crop = ImageEdit.Crop.WHOLE
                    shape = Shape.FREE
                } else {
                    steps.lastOrNull()?.let { undone = undone + it }
                    steps = steps.dropLast(1)
                }
            },
            onRedo = {
                undone.lastOrNull()?.let { steps = steps + it }
                undone = undone.dropLast(1)
            },
            onOriginal = {
                steps = emptyList()
                undone = emptyList()
            }
        )
        }

        if (beside) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (panelOnStart) {
                    Sheet()
                }
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                /*
                 * ⚠️ **Il grigio va prima dei due rientri**: dipinge la fascia intera fra testata
                 * e scheda, mentre l'immagine resta dentro i rientri. Dopo, resterebbe una cornice
                 * del colore della pagina. Il colore è spiegato su [stageBack].
                 */
                .background(stageBack())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = STAGE_SIDE, vertical = STAGE_PAD),
            contentAlignment = Alignment.Center
        ) {
            val picture = shown
            if (picture == null) {
                CircularProgressIndicator()
            } else {
                val room = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
                val frame = remember(picture, room) { fitted(picture, room) }
                val density = LocalDensity.current
                CropStage(
                    picture = picture,
                    frame = frame,
                    crop = crop,
                    grip = with(density) { GRIP.toPx() },
                    least = with(density) { LEAST_SIDE.toPx() },
                    arm = with(density) { HANDLE_ARM.toPx() },
                    thick = with(density) { HANDLE_THICK.toPx() },
                    halo = with(density) { GRIP_HALO.toPx() },
                    lensEdge = with(density) { LENS_EDGE.toPx() },
                    loupe = with(density) { LOUPE_SIDE.toPx() },
                    edge = with(density) { LOUPE_EDGE.toPx() },
                    onCrop = { crop = it },
                    keep = shape.value(lay, aspect)
                )
            }
        }

                if (!panelOnStart) {
                    Sheet()
                }
            }
        } else {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                // Il grigio va prima dei due rientri, come nel ramo affiancato qui sopra.
                .background(stageBack())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = STAGE_SIDE, vertical = STAGE_PAD),
            contentAlignment = Alignment.Center
        ) {
            val picture = shown
            if (picture == null) {
                CircularProgressIndicator()
            } else {
                val room = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
                val frame = remember(picture, room) { fitted(picture, room) }
                val density = LocalDensity.current
                CropStage(
                    picture = picture,
                    frame = frame,
                    crop = crop,
                    grip = with(density) { GRIP.toPx() },
                    least = with(density) { LEAST_SIDE.toPx() },
                    arm = with(density) { HANDLE_ARM.toPx() },
                    thick = with(density) { HANDLE_THICK.toPx() },
                    halo = with(density) { GRIP_HALO.toPx() },
                    lensEdge = with(density) { LENS_EDGE.toPx() },
                    loupe = with(density) { LOUPE_SIDE.toPx() },
                    edge = with(density) { LOUPE_EDGE.toPx() },
                    onCrop = { crop = it },
                    keep = shape.value(lay, aspect)
                )
            }
        }

            Sheet()
        }
    }
}

/**
 * Un passo confermato con 'Applica'.
 *
 * [done] è composto dall'originale e non dal passo prima (la nota su `steps` in
 * [EditorScreen]). [preview] è l'immagine che ne risulta, per ritrovarla disfacendo.
 * ⚠️ **I bitmap dei passi disfatti non si riciclano**: uno di loro può essere ancora dentro un
 * [ImageBitmap] che Compose sta disegnando, e il disegno andrebbe in errore. Sono grandi al
 * massimo quanto un'anteprima, e li libera il raccoglitore.
 */
private class Step(val done: Done, val preview: Bitmap)


/** Una posa e un rettangolo: quello che si sa applicare al file vero. */
private data class Done(val spin: Spin, val crop: ImageEdit.Crop) {
    companion object {
        val NOTHING = Done(Spin.STILL, ImageEdit.Crop.WHOLE)
    }
}

/**
 * Quello che si ottiene facendo [spin] e [crop] **dopo** [done].
 *
 * ⚠️ **La composizione è esatta**: la catena è sempre 'metti in posa, poi ritaglia'
 * (`ImageEdit.redraw`), e mettere in posa un ritaglio equivale a ritagliare l'immagine in posa
 * col rettangolo messo in posa nel quadrato unitario ([spunRect]). Restano due ritagli uno dentro
 * l'altro, che si compongono in uno ([insideOf]): n passi diventano una posa e un rettangolo.
 */
private fun after(done: Done, spin: Spin, crop: ImageEdit.Crop): Done = Done(
    done.spin.then(spin),
    insideOf(spunRect(done.crop, spin), crop)
)

/** Il passo nuovo: la composizione, e l'anteprima che ne esce. */
private fun applied(base: Bitmap, done: Done, spin: Spin, crop: ImageEdit.Crop): Step {
    val spun = base.spunBy(spin.turns, spin.mirror)
    val cut = spun.cutTo(crop)
    if (spun !== base && spun !== cut) spun.recycle()
    return Step(after(done, spin, crop), cut)
}

/**
 * Lo stesso rettangolo dopo [spin], in frazioni.
 *
 * ⚠️ Girando di un quarto in senso orario il punto `(x, y)` va in `(1 - y, x)`, quindi il lato
 * sinistro nuovo viene dal fondo vecchio. Il segno sbagliato dà un ritaglio speculare, che su
 * un'immagine simmetrica non si vede.
 * ⚠️ **Lo specchio viene prima delle rotazioni**, come nella posa: sul quadrato unitario
 * ribalta la x.
 */
internal fun spunRect(crop: ImageEdit.Crop, spin: Spin): ImageEdit.Crop {
    var out = if (spin.mirror) {
        ImageEdit.Crop(1f - crop.right, crop.top, 1f - crop.left, crop.bottom)
    } else {
        crop
    }
    repeat(spin.turns.mod(4)) {
        out = ImageEdit.Crop(1f - out.bottom, out.left, 1f - out.top, out.right)
    }
    return out
}

/** Il rettangolo [inner], che è in frazioni di [outer], scritto in frazioni dell'intero. */
private fun insideOf(outer: ImageEdit.Crop, inner: ImageEdit.Crop): ImageEdit.Crop {
    val w = outer.right - outer.left
    val h = outer.bottom - outer.top
    return ImageEdit.Crop(
        outer.left + inner.left * w,
        outer.top + inner.top * h,
        outer.left + inner.right * w,
        outer.top + inner.bottom * h
    )
}


/** La selezione portata a metà larghezza, senza cambiare misura. */
internal fun centredAcross(crop: ImageEdit.Crop): ImageEdit.Crop {
    val w = crop.right - crop.left
    return ImageEdit.Crop((1f - w) / 2f, crop.top, (1f + w) / 2f, crop.bottom)
}

/** La selezione portata a metà altezza, senza cambiare misura. */
internal fun centredDown(crop: ImageEdit.Crop): ImageEdit.Crop {
    val h = crop.bottom - crop.top
    return ImageEdit.Crop(crop.left, (1f - h) / 2f, crop.right, (1f + h) / 2f)
}

/**
 * La stessa selezione con una proporzione nuova: stesso centro, stessa **area**.
 *
 * ⚠️ **Si conserva l'area e non un lato**: tenendo la larghezza, da 16:9 a 9:16 il rettangolo
 * uscirebbe dall'immagine. L'area non privilegia un verso, e si legge come 'la stessa selezione,
 * di un'altra forma'.
 * ⚠️ **Le frazioni non sono la proporzione**: il rapporto fra le frazioni è quello dei pixel
 * diviso [frame], come in [flipped].
 * Se non entra si rimpicciolisce, e solo dopo si sposta: la forma è quello che si è chiesto.
 */
internal fun reshaped(crop: ImageEdit.Crop, frame: Float, want: Float?): ImageEdit.Crop {
    if (want == null || frame <= 0f) return crop
    val area = (crop.right - crop.left) * (crop.bottom - crop.top)
    if (area <= 0f) return crop
    val ratio = want / frame
    var w = sqrt(area * ratio)
    var h = sqrt(area / ratio)
    if (w <= 0f || h <= 0f) return crop
    val room = min(1f, min(1f / w, 1f / h))
    if (room < 1f) {
        w *= room
        h *= room
    }
    val x = ((crop.left + crop.right) / 2f).coerceIn(w / 2f, 1f - w / 2f)
    val y = ((crop.top + crop.bottom) / 2f).coerceIn(h / 2f, 1f - h / 2f)
    return ImageEdit.Crop(x - w / 2f, y - h / 2f, x + w / 2f, y + h / 2f)
}

/** Il modificatore e la forma della scheda dell'editor: vedi [editorSheetChrome]. */
internal data class EditorSheetChrome(val modifier: Modifier, val shape: RoundedCornerShape)

/**
 * Come si veste la scheda dell'editor: in basso sul telefono, a lato da 1.024 dp
 * ([Adaptive.editorBeside]), come nel mockup.
 *
 * Il lato lo decide [panelOnStart]: a lato, la scheda è all'inizio della riga per i mancini e alla
 * fine per i destri (`LocalPadLook.current.hand == Hand.LEFT` in [EditorScreen]).
 */
@Composable
internal fun editorSheetChrome(
    beside: Boolean,
    panelOnStart: Boolean,
    toolsWidth: Dp
): EditorSheetChrome {
    if (!beside) {
        return EditorSheetChrome(
            modifier = Modifier
                .fillMaxWidth()
                .background(stageBack())
                .edgedTop(PANEL_ROUND),
            shape = RoundedCornerShape(topStart = PANEL_ROUND, topEnd = PANEL_ROUND)
        )
    }
    val shape = if (panelOnStart) {
        RoundedCornerShape(topEnd = PANEL_ROUND, bottomEnd = PANEL_ROUND)
    } else {
        RoundedCornerShape(topStart = PANEL_ROUND, bottomStart = PANEL_ROUND)
    }
    return EditorSheetChrome(
        modifier = Modifier
            .width(toolsWidth)
            .fillMaxHeight()
            .background(stageBack())
            .edged(PANEL_ROUND),
        shape = shape
    )
}

/**
 * I comandi dell'editor, in una scheda appoggiata in fondo alla schermata, o a lato da 1.024 dp.
 *
 * Una scheda perché i tasti sono sette (richiesta dell'utente). ⚠️ **Non una
 * `ModalBottomSheet`**, come la scheda della selezione ([PickSheet]): quella mette un velo e si
 * prende i tocchi, e qui sotto c'è il rettangolo che si trascina col dito.
 * Le due file di tasti sono due `ActionPad` e non uno da sette: sopra si trasforma, sotto si
 * conferma o si torna indietro, e il filetto in mezzo segna la differenza.
 */
@Composable
private fun EditorSheet(
    /** Se la scheda è a lato del palco (tablet largo) e non sotto ([Adaptive.editorBeside]). */
    beside: Boolean = false,
    /** Se a lato, la scheda è all'inizio della riga (mancini) o alla fine (destri). */
    panelOnStart: Boolean = true,
    /** Larghezza della colonna quando [beside] è acceso; ignorata altrimenti. */
    toolsWidth: Dp = 320.dp,
    shape: Shape,
    lay: Lay,
    busy: Boolean,
    /** Se l'anteprima è arrivata: prima non c'è niente su cui agire. */
    ready: Boolean,
    /** Se c'è un ritocco non ancora confermato. */
    pending: Boolean,
    /** Se c'è almeno un 'Applica' alle spalle. */
    applied: Boolean,
    /** Se c'è almeno un passo disfatto che 'Ripristina' può rimettere. */
    undone: Boolean,
    /**
     * I due comandi del salvataggio, nella barra in basso.
     *
     * Riceve il lato come argomento perché lo sa la scheda: il blocco cambia lato col FAB.
     */
    tools: @Composable (mirror: Boolean) -> Unit,
    onShape: (Shape) -> Unit,
    onLay: (Lay) -> Unit,
    /** Un quarto di giro: `1` in senso orario, `3` antiorario. */
    onTurn: (Int) -> Unit,
    /** Uno specchio: `false` sull'asse verticale (destra e sinistra), `true` sull'orizzontale. */
    onFlip: (Boolean) -> Unit,
    onCentreAcross: () -> Unit,
    onCentreDown: () -> Unit,
    /** Il tocco lungo su uno qualunque dei due tasti di centratura: vedi là. */
    onCentreBoth: () -> Unit,
    onApply: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOriginal: () -> Unit
) {
    val live = ready && !busy
    val chrome = editorSheetChrome(beside, panelOnStart, toolsWidth)
    Surface(
        /*
         * ⚠️ **Il grigio del palco passa sotto la scheda** (riscontro dell'utente, giro della
         * 1.51): negli spicchi scavati dagli angoli stondati si vedrebbe il fondo della pagina.
         * Il fondo è nel modificatore di [editorSheetChrome] e non in `color`: `Surface` accoda il
         * proprio `background` e il proprio `clip`, quindi quel fondo resta un rettangolo non
         * ritagliato e la scheda gli si stampa sopra con i suoi angoli.
         * ⚠️ **Il bordo d'accento va dopo il fondo del palco** (decisione dell'utente, giro della
         * 1.54): il nodo che lo disegna riceve la misura della superficie, e invertendo l'ordine la
         * riga finirebbe sotto il fondo. La riga corre fuori dalla scheda: si vedono la cima e i
         * due archi.
         */
        modifier = chrome.modifier,
        shape = chrome.shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        /*
         * Niente ombra, come le altre schede appoggiate in basso (richiesta dell'utente): il
         * perché misurato è su `PickSheet`. Niente rilievo tonale: vale solo su una superficie
         * `surface`, e questa è `surfaceContainerHigh`.
         */
    ) {
        Column(
            // Il rientro di sistema è qui: il fondo della scheda passa sotto la barra, il
            // contenuto no (come in `PickSheet`).
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
                /*
                 * ⚠️ **Il respiro in cima** (riscontro dell'utente): la prima fila è di gettoni
                 * alti 32 dp senza rientro proprio, e toccherebbe il bordo dentro la curva degli
                 * angoli ([PANEL_ROUND]). [SHEET_TOP] e non i 12 dp della scheda delle
                 * informazioni, che ha anche il bersaglio della crocetta: misurato, a 16 dp la
                 * curva è rientrata di 5 dp, meno del rientro laterale della fila.
                 */
                .padding(top = SHEET_TOP),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            /*
             * I due versi sono nella fila delle forme, accanto a 'Originale' ([ShapeRow]).
             * Cambiarli ribalta la selezione sul posto (richiesta dell'utente): da 16:9 a 9:16, col
             * centro fermo, perché si sceglie che forma dare al ritaglio e non dove metterlo.
             * Gli otto punti sotto sono l'aria prima delle due file di tasti.
             */
            ShapeRow(
                shape = shape,
                lay = lay,
                enabled = live,
                onShape = onShape,
                onLay = onLay,
                rows = ShapeRows.SIMPLE,
                modifier = Modifier.padding(start = STAGE_SIDE, end = STAGE_SIDE, bottom = 8.dp)
            )

            /*
             * Le frecce circolari senza forme geometriche, disegnate dall'utente (sua richiesta).
             * ⚠️ **Il verso di un giro è fisico e non si specchia** nelle lingue che si leggono da
             * destra: una freccia antioraria specchiata diventerebbe oraria. I disegni di
             * [Glyphs] non si specchiano da sé.
             */
            val ccw = Glyphs.TurnLeft
            val cw = Glyphs.TurnRight

            val turnLeft = PadAction(PadKey.TURN_LEFT, ccw, R.string.editor_left, enabled = live) { onTurn(3) }
            val turnRight = PadAction(PadKey.TURN_RIGHT, cw, R.string.editor_right, enabled = live) { onTurn(1) }
            /*
             * Le icone di centratura sono disegnate dall'utente (voce `ed-sheet`, [Glyphs.AlignAcross]).
             * ⚠️ **Il tocco lungo su uno dei due centra su tutti e due gli assi** (punto F
             * dell'utente, 2.10): chi tiene premuto non deve chiedersi quale tasto sia. Il conto è
             * la composizione delle due funzioni, indipendenti fra loro. Il gesto ha la sua
             * etichetta, come vuole [PadAction.onHold].
             */
            val acrossKey = PadAction(
                PadKey.CENTRE_ACROSS, Glyphs.AlignAcross, R.string.editor_center_across,
                enabled = live,
                onHold = { onCentreBoth() },
                holdLabel = R.string.editor_center_both
            ) { onCentreAcross() }
            val downKey = PadAction(
                PadKey.CENTRE_DOWN, Glyphs.AlignDown, R.string.editor_center_down,
                enabled = live,
                onHold = { onCentreBoth() },
                holdLabel = R.string.editor_center_both
            ) { onCentreDown() }

            /*
             * ⚠️ **L'etichetta dice 'Rifletti' e basta**: con cinque celle 'Rifletti in
             * orizzontale' non entra sotto il glifo nemmeno su due righe (misurato). Il verso lo
             * dicono il glifo (destra-sinistra) e il tocco lungo, che si annuncia con la sua
             * etichetta (richiesta dell'utente).
             * Il glifo è di Material e resta (risposta `resta` dell'utente a `d-flip-glifo`): non
             * è provvisorio.
             */
            val flipKey = PadAction(
                PadKey.FLIP, Icons.Filled.Flip, R.string.editor_flip,
                enabled = live,
                onHold = { onFlip(true) },
                holdLabel = R.string.editor_flip_down
            ) { onFlip(false) }

            val applyKey = PadAction(
                PadKey.APPLY, Glyphs.EditApply, R.string.editor_apply, enabled = live && pending
            ) { onApply() }
            val undoKey = PadAction(
                PadKey.UNDO, Glyphs.EditUndo, R.string.editor_undo,
                enabled = live && (pending || applied)
            ) { onUndo() }
            val redoKey = PadAction(
                PadKey.REDO, Glyphs.EditRedo, R.string.editor_redo,
                // ⚠️ Disattivato con un ritocco in sospeso: il rettangolo è in frazioni
                // dell'immagine di adesso, e sotto un passo rimesso selezionerebbe un'altra cosa.
                // 'Annulla' toglie il ritocco e lo riattiva.
                enabled = live && undone && !pending
            ) { onRedo() }
            val originalKey = PadAction(
                PadKey.ORIGINAL, Glyphs.EditReset, R.string.editor_original,
                enabled = live && (pending || applied || undone)
            ) { onOriginal() }

            /*
             * ⚠️ **Le due file non si specchiano: l'ordine lo sceglie l'utente trascinando**
             * (decisione del giro della 1.54). Rovesciare una lista sbaglierebbe i tasti che hanno
             * il verso delle frecce e non quello della lettura (i due giri, 'Annulla' e
             * 'Ripristina').
             * Restano due file con due ordini salvati: girare e centrare è un mestiere, la
             * cronologia un altro, e il filetto in mezzo lo dice.
             * La prima fila ha cinque colonne ([TURN_KEYS]) e la seconda quattro ([SHEET_KEYS]):
             * 'Rifletti' è al centro, dove l'ha chiesto l'utente, e le file restano allineate agli
             * estremi. Una quinta colonna vuota in basso sarebbe un buco.
             */
            ActionPad(
                columns = TURN_KEYS.size,
                stretch = true,
                actions = listOf(downKey, acrossKey, flipKey, turnLeft, turnRight)
                    .inOrder(LocalPadLook.current.turn)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = STAGE_SIDE))

            ActionPad(
                columns = SHEET_KEYS,
                stretch = true,
                actions = listOf(originalKey, undoKey, redoKey, applyKey)
                    .inOrder(LocalPadLook.current.step)
            )

            /*
             * La barra dei due comandi del salvataggio, in fondo come nell'editor completo, dove
             * arriva il pollice. Il lato lo dà [fabEdge], la stessa preferenza del FAB.
             */
            Row(modifier = Modifier.fillMaxWidth()) {
                val mirror = fabEdge() == Alignment.Start
                if (mirror) Spacer(modifier = Modifier.weight(1f))
                tools(mirror)
                if (!mirror) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Un gettone del pannello, scritto in casa.
 *
 * ⚠️ **Non è il `FilterChip` di Material, per una misura**: i gettoni delle proporzioni sono
 * larghi uguali e da bordo a bordo (richiesta dell'utente), e il `FilterChip` ha 16 dp di
 * rientro fissi per lato, senza `contentPadding`: su 360 dp resterebbero 28 dp per la parola, e
 * non entrerebbe nemmeno '9:16'. Con [CHIP_PAD] ne restano quasi 50.
 * Per il resto la resa è la sua: altezza 32 dp, smusso, `secondaryContainer` da scelto, filetto
 * da non scelto. L'ellissi resta come rete.
 *
 * ⚠️ **Disattivato si vede disattivato** (voce `originale-completo` dell'utente): `Surface` con
 * `enabled = false` non risponde al tocco ma non cambia colore. I valori sono quelli dello stato
 * disattivato del `FilterChip` (`FilterChipTokens` di material3): testo e icona a [CHIP_OFF_INK],
 * fondo del gettone scelto e filetto degli altri a [CHIP_OFF_FILL]. Vale anche durante un
 * salvataggio e prima che l'anteprima arrivi.
 */
@Composable
internal fun SheetChip(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Il corpo della parola. Lo passa la sola fila delle forme, che nell'editor semplice scende di
     * un gradino ([ShapeRow]).
     */
    style: TextStyle = MaterialTheme.typography.labelLarge,
    /**
     * Un disegno al posto della parola (i due versi del ritaglio nell'editor completo,
     * [ShapeRow]).
     *
     * ⚠️ **Con lui [text] diventa la descrizione parlata**, dove la trovano il lettore di schermo
     * e il banco, come nei gettoni dei moduli.
     */
    icon: ImageVector? = null
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        /*
         * ⚠️ **La scelta si dichiara anche al lettore di schermo** (`picked`), come fa il
         * `FilterChip`: col solo colore non si sentirebbe. La mette il componente e non i
         * chiamanti, che sono sette.
         */
        modifier = modifier.picked(selected).height(CHIP_TALL),
        shape = RoundedCornerShape(CHIP_ROUND),
        color = when {
            !selected -> Color.Transparent
            enabled -> scheme.secondaryContainer
            else -> scheme.onSurface.copy(alpha = CHIP_OFF_FILL)
        },
        contentColor = when {
            !enabled -> scheme.onSurface.copy(alpha = CHIP_OFF_INK)
            selected -> scheme.onSecondaryContainer
            else -> scheme.onSurfaceVariant
        },
        border = when {
            selected -> null
            enabled -> BorderStroke(CHIP_EDGE, scheme.outlineVariant)
            else -> BorderStroke(CHIP_EDGE, scheme.onSurface.copy(alpha = CHIP_OFF_FILL))
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = CHIP_PAD),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    modifier = Modifier.size(CHIP_ICON)
                )
            } else {
                Text(
                    text = text,
                    style = style,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Le forme del ritaglio e i due versi, in **due righe** in tutti e due gli editor.
 *
 * ⚠️ **Un pezzo solo perché i due editor mostrano la stessa fila**: il modulo Ritaglio
 * dell'editor completo usa lo stesso ritaglio e gli stessi formati dell'editor semplice.
 * La fila prende tutta la larghezza (richiesta dell'utente).
 *
 * **Editor semplice** (richiesta dell'utente, giro della 2.87): riga 1 'Libero' e i numeri, riga 2
 * 'Originale' e i due versi.
 * - 'Originale' è accanto ai due versi che disattiva ([Shape.fromImage]).
 * - I versi sono scritti: tre celle uguali su 360 dp lasciano 88 dp per la parola, contro i 76
 *   della più larga delle lingue (il tedesco *Hochformat*).
 * - Le celle della prima riga si misurano: a 'Libero' la larghezza vera della sua parola, a ogni
 *   numero quella del più largo, e l'avanzo in proporzione, come in `ModeRow`.
 * - ⚠️ **Se la riga non entra si stringe la parola e non i numeri** ([shapeCell]): '16:9' troncato
 *   sembrerebbe un'altra proporzione.
 * - Misure, Roboto Medium su una fila di 312 dp (360 meno i due [STAGE_SIDE]) che coi distacchi
 *   ne lascia 282: italiano 237, russo 260; col corpo pieno il russo ne chiederebbe 278, per
 *   questo il corpo è un gradino sotto. Il tamil ne chiede 283 col Noto Sans Tamil, e la parola
 *   prende l'ellissi; il valore vero non è misurato, perché Android usa la variante UI di quel
 *   carattere.
 *
 * **Editor completo** (punto `crop-giu` dell'utente): riga 1 le parole e i due versi come icone
 * larghe [LAY_CELL], riga 2 i numeri.
 * L'ordine dei versi è quello di [Lay] in tutti e due gli editor.
 */
@Composable
internal fun ShapeRow(
    shape: Shape,
    lay: Lay,
    enabled: Boolean,
    onShape: (Shape) -> Unit,
    /** Che cosa fa il tocco su uno dei due versi. */
    onLay: (Lay) -> Unit,
    /**
     * In quale editor è la fila, cioè quale delle due disposizioni.
     *
     * Senza valore di fabbrica: un chiamante nuovo che lo omettesse mostrerebbe la fila
     * dell'altro editor.
     */
    rows: ShapeRows,
    modifier: Modifier = Modifier
) {
    val full = rows == ShapeRows.FULL
    val style = if (full) {
        MaterialTheme.typography.labelLarge
    } else {
        MaterialTheme.typography.labelMedium
    }

    /** Che cosa si legge su un gettone: la sua parola, o il suo numero nel verso acceso. */
    @Composable
    fun scritto(one: Shape): String = one.word?.let { stringResource(it) } ?: one.text(lay).orEmpty()

    @Composable
    fun chip(one: Shape, cella: Modifier) {
        SheetChip(
            text = scritto(one),
            selected = one == shape,
            enabled = enabled,
            onClick = { onShape(one) },
            modifier = cella,
            style = style
        )
    }

    @Composable
    fun verso(one: Lay, cella: Modifier) {
        SheetChip(
            text = stringResource(one.label),
            icon = when {
                !full -> null
                one == Lay.TALL -> Icons.Filled.CropPortrait
                else -> Icons.Filled.CropLandscape
            },
            selected = one == lay,
            // Disattivati con 'Originale' ([Shape.fromImage]).
            enabled = enabled && !shape.fromImage,
            onClick = { onLay(one) },
            modifier = cella,
            style = style
        )
    }

    Column(
        modifier = modifier.fillMaxWidth().oneOf(),
        verticalArrangement = Arrangement.spacedBy(SHAPE_GAP)
    ) {
        if (full) {
            Row(horizontalArrangement = Arrangement.spacedBy(SHAPE_GAP)) {
                for (one in Shape.entries.filter { it.word != null }) {
                    chip(one, Modifier.weight(1f))
                }
                for (one in Lay.entries) verso(one, Modifier.width(LAY_CELL))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(SHAPE_GAP)) {
                for (one in Shape.entries.filter { it.numeric }) {
                    chip(one, Modifier.weight(1f))
                }
            }
        } else {
            /*
             * Le celle si misurano (regola su [shapeCell]): la parola prende quello che i numeri
             * lasciano, e la riga finisce sul bordo. I numeri sono larghi uguali, quanto il più
             * largo: '1:1' più stretto di '16:9' romperebbe la griglia.
             */
            val righello = rememberTextMeasurer()
            val density = LocalDensity.current
            val numeri = Shape.entries.filter { it.numeric }
            fun serve(testo: String): Dp = with(density) {
                righello.measure(AnnotatedString(testo), style, maxLines = 1).size.width.toDp()
            } + CHIP_PAD * 2
            val parola = serve(scritto(Shape.FREE))
            val numero = numeri.maxOf { serve(scritto(it)) }
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cella = shapeCell(parola, numero, numeri.size, maxWidth - SHAPE_GAP * numeri.size)
                Row(horizontalArrangement = Arrangement.spacedBy(SHAPE_GAP)) {
                    chip(Shape.FREE, Modifier.weight(1f))
                    for (one in numeri) chip(one, Modifier.width(cella))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(SHAPE_GAP)) {
                chip(Shape.ORIGINAL, Modifier.weight(1f))
                for (one in Lay.entries) verso(one, Modifier.weight(1f))
            }
        }
    }
}

/** Le due disposizioni di [ShapeRow], una per editor. */
internal enum class ShapeRows {
    /** L'editor semplice: 'Libero' e i numeri sopra, 'Originale' e i versi sotto. */
    SIMPLE,

    /** L'editor completo: le due parole e i versi a icona sopra, i numeri sotto. */
    FULL
}

/** Il distacco fra due forme, uguale fra due celle vicine e fra le due righe. */
private val SHAPE_GAP = 6.dp

/**
 * Quanto è larga la cella di un verso del ritaglio nell'editor completo ([ShapeRow]).
 *
 * Il bersaglio di Material meno l'aria: un'icona sola non ha niente da allargare, e il resto va
 * alle due parole accanto.
 */
private val LAY_CELL = 44.dp

/** Quanto è grande un'icona dentro un gettone: la misura di Material per un'icona in una riga. */
private val CHIP_ICON = 18.dp

/**
 * Quanto è larga la cella di un numero nella prima riga dell'editor semplice ([ShapeRow]). La
 * cella di 'Libero' prende quello che resta.
 *
 * [word] e [number] sono quanto chiedono le due celle, testo più rientri; [room] è la riga meno i
 * distacchi.
 * - Se la riga entra, l'avanzo si divide in proporzione a quello che ognuno chiede.
 * - ⚠️ **Se non entra, i numeri tengono la loro misura e si stringe la parola**.
 * - Se non entrano nemmeno i numeri, parti uguali: una riga così stretta non esiste su un telefono.
 *
 * È una funzione a sé per il banco, dove i testi misurano meno che sul telefono: la regola si
 * prova con le misure vere.
 */
internal fun shapeCell(word: Dp, number: Dp, numbers: Int, room: Dp): Dp {
    val need = word + number * numbers
    return when {
        need <= room -> number * (room / need)
        number * numbers < room -> number
        else -> room / (numbers + 1)
    }
}

/** Le misure del gettone scritto in casa: quelle di Material, tranne il rientro. */
private val CHIP_TALL = 32.dp
private val CHIP_ROUND = 8.dp
private val CHIP_EDGE = 1.dp
private val CHIP_PAD = 6.dp

/**
 * Le due opacità di un gettone disattivato ([SheetChip]): quelle di Material
 * (`FilterChipTokens.DisabledLabelTextOpacity` e `FlatDisabledSelectedContainerOpacity`), come
 * per tutti gli altri comandi disattivati dell'app.
 */
private const val CHIP_OFF_INK = 0.38f
private const val CHIP_OFF_FILL = 0.12f

/**
 * Quante colonne ha la seconda fila di tasti del pannello (originale e cronologia): quattro.
 *
 * La prima fila ne ha cinque ([TURN_KEYS]): le due file sono allineate agli estremi, non per
 * colonne (la nota sulle due file in [EditorSheet]).
 */
internal const val SHEET_KEYS = 4


/**
 * Il respiro fra il bordo di sopra della scheda e la prima fila di gettoni.
 *
 * Condiviso con l'editor completo (`AdvancedEditorScreen.kt`), che ha la stessa testata, lo
 * stesso palco e la stessa scheda.
 */
internal val SHEET_TOP = 16.dp

/**
 * Il rientro che un `TextButton` di Material ha dentro, per lato (`ButtonDefaults`,
 * `TextButtonHorizontalPadding`, letto nel bytecode di material3): fa cadere il testo di 'Salva'
 * sulla linea del resto della schermata (la nota sulla testata in [EditorScreen]).
 */
internal val TEXT_BUTTON_PAD = 12.dp

/** Come sta la selezione: in piedi o coricata. Vedi i due tasti in [EditorScreen]. */
internal enum class Lay(@StringRes val label: Int) {
    TALL(R.string.editor_tall),
    WIDE(R.string.editor_wide)
}

/**
 * Con che forma si ritaglia, senza dire in quale verso.
 *
 * ⚠️ **Una voce per proporzione e non una per verso**: '2:3' e '3:2' sono la stessa forma nei due
 * versi. Il verso lo dà [Lay], e la forma resta scelta mentre si gira.
 * Il valore è larghezza diviso altezza in verticale, e in orizzontale il suo reciproco; `null` è
 * 'libero', così il caso speciale è uno solo. Le etichette numeriche non sono risorse: '16:9' si
 * scrive uguale in tutte le lingue. Le due forme a parole hanno la loro in [word].
 *
 * 'Originale' (richiesta dell'utente, 2.32) prende il rapporto dall'immagine, quindi il rapporto
 * è una funzione del `frame` passato a [value] e a [fit], per tutte le forme.
 * ⚠️ **'Originale' è l'immagine intera in qualunque verso** (risposta `intera` dell'utente a
 * `d-originale-verso`): ignora i due gettoni del verso ([fromImage]), che mentre è scelta si
 * disattivano. Il verso vale per le proporzioni scritte qui ([numeric]).
 *
 * Le proporzioni sono in ordine crescente, nei due editor (istruzioni dell'utente, 2.88 e 2.89).
 * ⚠️ **L'ordine di dichiarazione è l'ordine dei gettoni**, e nessun archivio lo legge: l'editor
 * completo tiene la forma scelta come indice nel solo stato salvato della schermata (`Gaze`). Una
 * voce nuova in mezzo cambierebbe al più il gettone acceso in un editor aperto durante un
 * aggiornamento.
 */
internal enum class Shape(
    private val tall: (Float) -> Float?,
    @StringRes val word: Int?,
    private val up: String?,
    private val flat: String?,
    /**
     * Se il rapporto viene dall'immagine com'è, e i due gettoni del verso non c'entrano: vale solo
     * per 'Originale'.
     */
    val fromImage: Boolean = false
) {
    FREE({ null }, R.string.editor_free, null, null),
    ORIGINAL({ it.takeIf { f -> f > 0f } }, R.string.editor_shape_original, null, null, fromImage = true),
    ONE({ 1f }, null, "1:1", "1:1"),
    FOUR_FIVE({ 4f / 5f }, null, "4:5", "5:4"),
    THREE_FOUR({ 3f / 4f }, null, "3:4", "4:3"),
    TWO_THREE({ 2f / 3f }, null, "2:3", "3:2"),
    NINE_SIXTEEN({ 9f / 16f }, null, "9:16", "16:9");

    /**
     * Se il rapporto è un numero scritto qui, cioè una delle proporzioni: 'Libero' non ne ha, e
     * 'Originale' lo prende dall'immagine.
     *
     * Dice le forme che una rotazione deve rifare nel verso scelto: le altre due non cambiano
     * (`posedLook`, nell'editor completo).
     */
    val numeric: Boolean get() = this != FREE && !fromImage

    /**
     * Larghezza diviso altezza in questo verso dentro un'immagine larga [frame] volte la sua
     * altezza, e `null` se la forma è libera.
     */
    fun value(lay: Lay, frame: Float): Float? =
        tall(frame)?.let { if (fromImage || lay == Lay.TALL) it else 1f / it }

    /** Come si scrive in questo verso, e `null` per le due che portano una parola in [word]. */
    fun text(lay: Lay): String? = if (lay == Lay.TALL) up else flat

    /**
     * Il rettangolo più grande con questa forma dentro un'immagine larga [frame] volte la sua
     * altezza, centrato, in frazioni.
     */
    fun fit(frame: Float, lay: Lay): ImageEdit.Crop {
        val want = value(lay, frame) ?: return ImageEdit.Crop.WHOLE
        return if (want >= frame) {
            // È largo quanto l'immagine, e avanza sopra e sotto.
            val h = (frame / want).coerceAtMost(1f)
            val gap = (1f - h) / 2f
            ImageEdit.Crop(0f, gap, 1f, 1f - gap)
        } else {
            val w = (want / frame).coerceAtMost(1f)
            val gap = (1f - w) / 2f
            ImageEdit.Crop(gap, 0f, 1f - gap, 1f)
        }
    }
}

/**
 * Il verso di un'immagine; il quadrato conta come verticale (richiesta dell'utente). La usano
 * tutti e due gli editor.
 */
internal fun startLay(base: Bitmap?): Lay =
    if (base != null && base.width > base.height) Lay.WIDE else Lay.TALL

/**
 * La stessa selezione girata di quarto: larghezza e altezza si scambiano, il centro resta.
 *
 * ⚠️ **Lo scambio è in pixel e non in frazioni**: le frazioni sono relative ai due lati, che
 * sono diversi, e scambiarle non farebbe di 16:9 un 9:16. Il passaggio per i pixel è la
 * moltiplicazione e la divisione per [frame].
 * Se il rettangolo girato non entra si rimpicciolisce senza deformarsi, e solo dopo si sposta.
 */
internal fun flipped(crop: ImageEdit.Crop, frame: Float): ImageEdit.Crop {
    if (frame <= 0f) return crop
    val cx = (crop.left + crop.right) / 2f
    val cy = (crop.top + crop.bottom) / 2f
    var w = (crop.bottom - crop.top) / frame
    var h = (crop.right - crop.left) * frame
    if (w <= 0f || h <= 0f) return crop
    val room = min(1f, min(1f / w, 1f / h))
    if (room < 1f) {
        w *= room
        h *= room
    }
    val x = cx.coerceIn(w / 2f, 1f - w / 2f)
    val y = cy.coerceIn(h / 2f, 1f - h / 2f)
    return ImageEdit.Crop(x - w / 2f, y - h / 2f, x + w / 2f, y + h / 2f)
}

/**
 * L'immagine con sopra il rettangolo del ritaglio, e le dita che lo muovono.
 *
 * Il rettangolo si disegna e si tocca in pixel di schermo, e si conserva in frazioni: si converte
 * una volta all'entrata e una all'uscita del gesto.
 * La presa dell'angolo è più grande dell'angolo disegnato, che altrimenti coprirebbe l'immagine.
 *
 * ⚠️⚠️ **Il rettangolo in corso vive dentro il gesto e non si rilegge da [crop] a ogni
 * movimento**: il blocco di `pointerInput` è una coroutine che dura quanto le sue chiavi, e il
 * `crop` che vede è quello catturato alla partenza. Calcolando `iniziale + delta` il rettangolo
 * tremerebbe intorno al punto di partenza senza rispondere. Con `crop` fra le chiavi il gesto si
 * riavvierebbe a ogni movimento. Si accumula qui dentro.
 */
@Composable
private fun CropStage(
    picture: ImageBitmap,
    frame: Rect,
    crop: ImageEdit.Crop,
    grip: Float,
    least: Float,
    arm: Float,
    thick: Float,
    /** Il filo che contorna le maniglie, e che è quello che le fa vedere. Vedi [bracket]. */
    halo: Float,
    /** Il bordo del ritaglio dentro la lente, che è la mira. Vedi [lens]. */
    lensEdge: Float,
    /** Il diametro della lente e la sua distanza dai bordi. Vedi [lens]. */
    loupe: Float,
    edge: Float,
    keep: Float?,
    onCrop: (ImageEdit.Crop) -> Unit
) {
    val dim = Color.Black.copy(alpha = VEIL)
    val line = Color.White
    var held by remember { mutableStateOf(Grab.NONE) }
    // Il valore più recente senza rifare il gesto: `rememberUpdatedState` serve a questo, dentro
    // una coroutine di lunga vita.
    val now by rememberUpdatedState(crop)
    val report by rememberUpdatedState(onCrop)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(frame, keep, least) {
                var going = Rect.Zero
                detectDragGestures(
                    onDragStart = { at ->
                        going = cropBox(now, frame)
                        // Le maniglie di lato ci sono con la sola forma libera ([grabbed]).
                        held = grabbed(at, going, grip, keep == null)
                    },
                    onDragEnd = { held = Grab.NONE },
                    onDragCancel = { held = Grab.NONE }
                ) { change, delta ->
                    change.consume()
                    if (held == Grab.NONE) return@detectDragGestures
                    going = dragged(going, held, delta, frame, keep, least)
                    report(cropFractions(going, frame))
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawImage(
                image = picture,
                dstOffset = IntOffset(frame.left.roundToInt(), frame.top.roundToInt()),
                dstSize = IntSize(frame.width.roundToInt(), frame.height.roundToInt())
            )
            val r = cropBox(crop, frame)
            cropOverlay(frame, r, arm, thick, halo, keep == null)

            // ── La lente ──
            eyeOf(held, r)?.let { eye ->
                lens(eye, r, loupe, edge, thick, lensEdge, line) {
                    drawImage(
                        image = picture,
                        dstOffset = IntOffset(frame.left.roundToInt(), frame.top.roundToInt()),
                        dstSize = IntSize(frame.width.roundToInt(), frame.height.roundToInt()),
                        filterQuality = FilterQuality.None
                    )
                }
            }
        }
    }
}

/**
 * Il velo intorno al rettangolo tenuto, i terzi e le quattro squadrette: quello che si vede
 * addosso a un'immagine mentre la si ritaglia.
 *
 * La usano i due editor: il palco di questo e il modulo Ritaglio dell'editor completo, così il
 * ritaglio si vede allo stesso modo. Disegna soltanto: il gesto resta di chi la chiama.
 *
 * Quattro squadrette (richiesta dell'utente): una squadretta appoggiata ai due lati dice quali lati
 * quel punto muove. Il braccio si accorcia sui ritagli piccoli, o le squadrette opposte si
 * toccherebbero. I terzi si disegnano sempre: servono a decidere, prima del tocco.
 */
internal fun DrawScope.cropOverlay(
    frame: Rect,
    r: Rect,
    arm: Float,
    thick: Float,
    halo: Float,
    /** Se si disegnano anche le quattro maniglie di lato: vedi [grabbed]. */
    sides: Boolean = true
) {
    val dim = Color.Black.copy(alpha = VEIL)
    val line = Color.White
    // Il velo, in quattro pezzi intorno al rettangolo tenuto.
    drawRect(dim, topLeft = frame.topLeft, size = Size(frame.width, r.top - frame.top))
    drawRect(dim, topLeft = Offset(frame.left, r.bottom),
        size = Size(frame.width, frame.bottom - r.bottom))
    drawRect(dim, topLeft = Offset(frame.left, r.top), size = Size(r.left - frame.left, r.height))
    drawRect(dim, topLeft = Offset(r.right, r.top), size = Size(frame.right - r.right, r.height))

    drawRect(
        color = line.copy(alpha = 0.9f),
        topLeft = r.topLeft,
        size = r.size,
        style = Stroke(width = EDGE_PX)
    )
    for (k in 1..2) {
        val x = r.left + r.width * k / 3f
        val y = r.top + r.height * k / 3f
        drawLine(line.copy(alpha = 0.35f), Offset(x, r.top), Offset(x, r.bottom), THIRD_PX)
        drawLine(line.copy(alpha = 0.35f), Offset(r.left, y), Offset(r.right, y), THIRD_PX)
    }
    val reach = min(arm, min(r.width, r.height) / 2.5f)
    bracket(Offset(r.left, r.top), 1, 1, reach, thick, CROP_GRIP, halo, line)
    bracket(Offset(r.right, r.top), -1, 1, reach, thick, CROP_GRIP, halo, line)
    bracket(Offset(r.left, r.bottom), 1, -1, reach, thick, CROP_GRIP, halo, line)
    bracket(Offset(r.right, r.bottom), -1, -1, reach, thick, CROP_GRIP, halo, line)
    /*
     * Le quattro barrette di lato dicono che quel bordo si muove da solo (richiesta dell'utente).
     * Il segno è diverso da quello degli angoli: un tratto dritto muove un lato, una squadretta
     * due. Si accorciano come i bracci, o toccherebbero le squadrette del loro lato.
     */
    if (!sides) return
    val half = min(arm, min(r.width, r.height) / 4f)
    bar(Offset(r.left, r.center.y), false, -1, half, thick, CROP_GRIP, halo, line)
    bar(Offset(r.right, r.center.y), false, 1, half, thick, CROP_GRIP, halo, line)
    bar(Offset(r.center.x, r.top), true, -1, half, thick, CROP_GRIP, halo, line)
    bar(Offset(r.center.x, r.bottom), true, 1, half, thick, CROP_GRIP, halo, line)
}

/**
 * Una maniglia di lato: un tratto dritto appoggiato fuori dal rettangolo, centrato su [at].
 *
 * Fuori come le squadrette, per la stessa ragione misurata ([bracket]): sul velo l'accento e il suo
 * filo si vedono sempre. L'alone è lo stesso tratto più grosso disegnato prima.
 *
 * [horizontal] dice se il tratto corre in orizzontale, cioè se è la maniglia del lato di sopra o di
 * sotto; [out] vale `1` se da quel bordo si esce verso destra o verso il basso, `-1` se no.
 */
private fun DrawScope.bar(
    at: Offset,
    horizontal: Boolean,
    out: Int,
    half: Float,
    thick: Float,
    ink: Color,
    halo: Float,
    haloInk: Color
) {
    val c = if (horizontal) {
        Offset(at.x, at.y + out * thick / 2f)
    } else {
        Offset(at.x + out * thick / 2f, at.y)
    }
    val da = if (horizontal) Offset(c.x - half, c.y) else Offset(c.x, c.y - half)
    val a = if (horizontal) Offset(c.x + half, c.y) else Offset(c.x, c.y + half)

    fun traccia(spessore: Float, colore: Color) = drawLine(
        color = colore,
        start = da,
        end = a,
        strokeWidth = spessore,
        cap = StrokeCap.Round
    )

    traccia(thick + 2f * halo, haloInk)
    traccia(thick, ink)
}

/**
 * L'angolo che la lente deve guardare, e `null` quando non c'è niente da guardare.
 *
 * Solo gli angoli: la domanda dell'utente è su quale pixel si rilascia la selezione, e quel pixel
 * esiste solo tirando un angolo. Spostando il rettangolo intero i punti sono quattro.
 */
internal fun eyeOf(held: Grab, r: Rect): Offset? = when (held) {
    Grab.TOP_LEFT -> r.topLeft
    Grab.TOP_RIGHT -> r.topRight
    Grab.BOTTOM_LEFT -> r.bottomLeft
    Grab.BOTTOM_RIGHT -> r.bottomRight
    // Una maniglia di lato muove una retta, non un punto: nessun pixel da ingrandire.
    Grab.LEFT, Grab.TOP, Grab.RIGHT, Grab.BOTTOM -> null
    Grab.INSIDE, Grab.NONE -> null
}

/**
 * La lente: un cerchio in alto con dentro [eye] ingrandito [LOUPE_ZOOM] volte.
 *
 * La usano i due editor (richiesta dell'utente, 2.88): quello che si vede dentro arriva da
 * [picture], che disegna nello spazio del palco (l'anteprima qui, l'immagine sviluppata
 * nell'editor completo). Cerchio, posto, ingrandimento, mira e anello sono scritti una volta.
 *
 * ⚠️ **È in alto, dalla parte opposta al dito, e non attaccata al dito**: il rettangolo arriva al
 * bordo dell'immagine, e una lente attaccata all'angolo uscirebbe dal riquadro proprio quando
 * serve. Il lato lo decide la metà dello schermo in cui è il dito, a ogni movimento: la lente
 * salta quando l'angolo attraversa la metà, nel mezzo di uno spostamento lungo, e non finisce mai
 * sotto il dito a fine corsa.
 *
 * ⚠️ **Dentro c'è anche il bordo del ritaglio**: i soli pixel direbbero che cosa c'è sotto il dito,
 * non dove passa il taglio. Lo spessore è diviso per l'ingrandimento.
 *
 * Senza interpolazione ([FilterQuality.None]): i pixel diventano quadretti netti e il bordo cade
 * visibilmente fra due. Lo sceglie chi disegna dentro ([picture]).
 * I pixel sono quelli dell'anteprima ([PREVIEW] sul lato lungo), non del file: il ritaglio si
 * conserva in frazioni e si applica al file intero, quindi la mira è più fine del quadretto.
 *
 * Il fondo nero sotto tutto: con un angolo a filo dell'immagine metà lente è fuori, e si vedrebbe
 * l'immagine non ingrandita.
 * L'anello è spesso quanto le squadrette ([thick]) e non quanto il bordo del ritaglio
 * ([EDGE_PX], due pixel fisici), che su un attrezzo sopra l'immagine sarebbe invisibile.
 */
internal fun DrawScope.lens(
    eye: Offset,
    r: Rect?,
    side: Float,
    edge: Float,
    thick: Float,
    lensEdge: Float,
    line: Color,
    farthestCorner: Boolean = false,
    /**
     * Quante volte si ingrandisce. Di fabbrica [LOUPE_ZOOM]; Correggi e Fluidifica lo abbassano
     * perché il pennello ingrandito resti dentro la lente (voce `3.24-02`).
     */
    zoom: Float = LOUPE_ZOOM,
    /** Quello che si vede dentro, disegnato nello spazio del palco: la lente lo ingrandisce. */
    picture: DrawScope.() -> Unit
) {
    val radius = side / 2f
    val centre = Offset(
        x = if (eye.x < size.width / 2f) size.width - edge - radius else edge + radius,
        y = if (farthestCorner && eye.y < size.height / 2f) {
            size.height - edge - radius
        } else edge + radius
    )
    val glass = Path().apply { addOval(Rect(center = centre, radius = radius)) }
    clipPath(glass) {
        drawRect(
            color = Color.Black,
            topLeft = Offset(centre.x - radius, centre.y - radius),
            size = Size(side, side)
        )
        withTransform({
            // Il punto mirato finisce al centro della lente: p -> centre + (p - eye) * zoom.
            translate(left = centre.x - eye.x * zoom, top = centre.y - eye.y * zoom)
            scale(scaleX = zoom, scaleY = zoom, pivot = Offset.Zero)
        }) {
            picture()
            /*
             * ⚠️ **La mira dentro la lente si misura in punti** ([LENS_EDGE], riscontro
             * dell'utente): [EDGE_PX], due pixel fisici, si vedrebbe come un capello. Fuori dalla
             * lente il bordo resta [EDGE_PX], per coprire il meno possibile.
             * La divisione per [zoom] serve: il disegno è dentro la trasformazione che ingrandisce.
             */
            if (r != null) {
                drawRect(
                    color = line.copy(alpha = 0.9f),
                    topLeft = r.topLeft,
                    size = r.size,
                    style = Stroke(width = lensEdge / zoom)
                )
            }
        }
    }
    drawCircle(color = line, radius = radius, center = centre, style = Stroke(width = thick))
}

/**
 * Quale presa ha preso il dito.
 *
 * Un angolo muove due bordi, un lato ne muove uno (maniglie di lato, richiesta dell'utente,
 * 2.40).
 */
internal enum class Grab { NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, LEFT, TOP, RIGHT, BOTTOM, INSIDE }

/**
 * Una squadretta d'angolo: due bracci arrotondati appoggiati fuori dal rettangolo.
 *
 * ⚠️ **Fuori e non dentro** (riscontro dell'utente): dentro appoggerebbe sull'immagine non
 * velata, e su una schermata bianca il contrasto sarebbe 1,00; fuori appoggia sul velo, dove lo
 * stesso bianco scende a `#737373` e il contrasto sale a 4,74.
 *
 * ⚠️ **Due colori: [ink] è l'accento, [halo] il filo che lo fa vedere.** Da solo nessun accento del
 * tema basta su ogni fondo (sul velo di un'immagine bianca `#43B59E` è a 1,88, `#4FD9BE` a 2,71).
 * Col filo bianco il caso peggiore diventa 2,85 sul grigio del palco e 4,74 sul velo bianco: dove
 * sparisce l'accento si vede il filo, e viceversa.
 *
 * [dx] e [dy] valgono `1` se da quell'angolo si va verso destra o verso il basso, `-1` se no.
 */
internal fun DrawScope.bracket(
    at: Offset,
    dx: Int,
    dy: Int,
    arm: Float,
    thick: Float,
    ink: Color,
    halo: Float,
    haloInk: Color
) {
    /*
     * ⚠️ **Un tracciato solo, una L con la piega** (riscontro dell'utente, giro della 1.51): due
     * tratti separati coi capi tondi si leggerebbero come due segmenti e non come una piega.
     * `StrokeJoin.Round` fa la piega, `StrokeCap.Round` i capi liberi.
     * Il tracciato corre a mezzo spessore fuori dal riquadro: l'inchiostro è tutto fuori e il bordo
     * interno della L tocca l'angolo dell'immagine.
     * L'alone è lo stesso tracciato più grosso disegnato prima: contorno uniforme, senza cuciture.
     */
    val c = Offset(at.x - dx * thick / 2f, at.y - dy * thick / 2f)
    val elle = Path().apply {
        moveTo(c.x + dx * arm, c.y)
        lineTo(c.x, c.y)
        lineTo(c.x, c.y + dy * arm)
    }

    fun traccia(spessore: Float, colore: Color) = drawPath(
        path = elle,
        color = colore,
        style = Stroke(
            width = spessore,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    traccia(thick + 2f * halo, haloInk)
    traccia(thick, ink)
}

/**
 * Quale maniglia prende il dito che scende in [at], dato il rettangolo [r].
 *
 * ⚠️ **I quattro lati ci sono solo con la forma libera**: la richiesta dell'utente è muovere un
 * lato senza toccare l'altra dimensione, e con un rapporto fisso non si può. [sides] lo dichiara
 * il chiamante, che sa se una forma è scelta.
 */
internal fun grabbed(at: Offset, r: Rect, grip: Float, sides: Boolean = true): Grab {
    val angoli = listOf(
        Grab.TOP_LEFT to Offset(r.left, r.top),
        Grab.TOP_RIGHT to Offset(r.right, r.top),
        Grab.BOTTOM_LEFT to Offset(r.left, r.bottom),
        Grab.BOTTOM_RIGHT to Offset(r.right, r.bottom)
    )
    // I lati entrano nello stesso confronto degli angoli: su un rettangolo piccolo cadono tutti
    // nella presa, e vince il punto più vicino al dito.
    val lati = if (!sides) emptyList() else listOf(
        Grab.LEFT to Offset(r.left, r.center.y),
        Grab.RIGHT to Offset(r.right, r.center.y),
        Grab.TOP to Offset(r.center.x, r.top),
        Grab.BOTTOM to Offset(r.center.x, r.bottom)
    )
    val near = (angoli + lati).minByOrNull { (_, corner) -> (corner - at).getDistance() }
    if (near != null && (near.second - at).getDistance() <= grip) {
        // ⚠️ Ma non quando il dito è più vicino al centro: su un ritaglio piccolo tutti gli
        // angoli cadono nella presa, e il rettangolo non si potrebbe più spostare.
        if ((near.second - at).getDistance() <= (r.center - at).getDistance()) return near.first
    }
    return if (r.contains(at)) Grab.INSIDE else Grab.NONE
}

/**
 * Come `coerceIn`, ma con un intervallo che non può essere vuoto: se il massimo cade sotto il
 * minimo vince il minimo.
 *
 * ⚠️ `coerceIn` con un intervallo rovesciato va in errore, e i limiti del ritaglio si rovesciano
 * quando il riquadro è più piccolo del lato minimo o la squadretta va oltre il bordo opposto
 * ([dragged]).
 */
private fun within(v: Float, lo: Float, hi: Float): Float = v.coerceIn(lo, max(lo, hi))

/**
 * Il rettangolo dopo lo spostamento del dito.
 *
 * ⚠️ **Con una proporzione fissa si muove un lato e l'altro segue**, con l'angolo opposto fermo:
 * così un 16:9 resta 16:9 anche con un movimento obliquo.
 */
internal fun dragged(
    r: Rect,
    held: Grab,
    delta: Offset,
    frame: Rect,
    keep: Float?,
    /** Il lato più corto che un ritaglio può avere. Non è la presa, che è più larga. */
    least: Float
): Rect {
    if (held == Grab.INSIDE) {
        val dx = delta.x.coerceIn(frame.left - r.left, frame.right - r.right)
        val dy = delta.y.coerceIn(frame.top - r.top, frame.bottom - r.bottom)
        return r.translate(dx, dy)
    }
    /*
     * ⚠️⚠️ **Il lato minimo non può superare il riquadro, o il gesto manda in crash l'app**: con
     * un'immagine molto allungata o un palco corto `frame.top .. bottom - small` sarebbe vuoto,
     * cioè un `IllegalArgumentException` (preso dal banco su un palco di 118 pixel con un lato
     * minimo di 122).
     */
    val small = least.coerceAtMost(min(frame.width, frame.height))
    var left = r.left
    var top = r.top
    var right = r.right
    var bottom = r.bottom
    when (held) {
        Grab.TOP_LEFT -> { left += delta.x; top += delta.y }
        Grab.TOP_RIGHT -> { right += delta.x; top += delta.y }
        Grab.BOTTOM_LEFT -> { left += delta.x; bottom += delta.y }
        Grab.BOTTOM_RIGHT -> { right += delta.x; bottom += delta.y }
        // Un lato muove solo il suo bordo.
        Grab.LEFT -> left += delta.x
        Grab.RIGHT -> right += delta.x
        Grab.TOP -> top += delta.y
        Grab.BOTTOM -> bottom += delta.y
        else -> Unit
    }
    // I quattro limiti passano da [within]: una squadretta tirata oltre il bordo opposto
    // rovescerebbe comunque l'intervallo.
    left = within(left, frame.left, right - small)
    right = within(right, left + small, frame.right)
    top = within(top, frame.top, bottom - small)
    bottom = within(bottom, top + small, frame.bottom)

    // Con una presa di lato il rapporto non si tiene; i lati esistono solo con la forma libera
    // ([grabbed]), ma la guardia protegge un chiamante nuovo che lo dimenticasse.
    if (keep == null || held == Grab.LEFT || held == Grab.RIGHT ||
        held == Grab.TOP || held == Grab.BOTTOM
    ) {
        return Rect(left, top, right, bottom)
    }

    // L'angolo che sta fermo è quello opposto a quello preso.
    val anchorX = if (held == Grab.TOP_LEFT || held == Grab.BOTTOM_LEFT) right else left
    val anchorY = if (held == Grab.TOP_LEFT || held == Grab.TOP_RIGHT) bottom else top
    var w = abs(right - left)
    var h = abs(bottom - top)
    // Si tiene il lato che si è mosso di più, e si ricava l'altro.
    if (w / h > keep) w = h * keep else h = w / keep
    val toLeft = held == Grab.TOP_LEFT || held == Grab.BOTTOM_LEFT
    val toTop = held == Grab.TOP_LEFT || held == Grab.TOP_RIGHT
    var x0 = if (toLeft) anchorX - w else anchorX
    var y0 = if (toTop) anchorY - h else anchorY
    // Se esce dall'immagine si rimpicciolisce, invece di deformarsi.
    val over = max(
        max((frame.left - x0) / w, (x0 + w - frame.right) / w),
        max((frame.top - y0) / h, (y0 + h - frame.bottom) / h)
    )
    if (over > 0f) {
        w *= (1f - over)
        h = w / keep
        x0 = if (toLeft) anchorX - w else anchorX
        y0 = if (toTop) anchorY - h else anchorY
    }
    return Rect(x0, y0, x0 + w, y0 + h)
}

/** Dov'è l'immagine dentro lo spazio disponibile, a filo e centrata. */
private fun fitted(picture: ImageBitmap, room: Size): Rect {
    if (room.width <= 0f || room.height <= 0f) return Rect(Offset.Zero, Size(1f, 1f))
    val k = min(room.width / picture.width, room.height / picture.height)
    val w = picture.width * k
    val h = picture.height * k
    val x = (room.width - w) / 2f
    val y = (room.height - h) / 2f
    return Rect(x, y, x + w, y + h)
}

internal fun cropBox(crop: ImageEdit.Crop, frame: Rect) = Rect(
    frame.left + crop.left * frame.width,
    frame.top + crop.top * frame.height,
    frame.left + crop.right * frame.width,
    frame.top + crop.bottom * frame.height
)

internal fun cropFractions(r: Rect, frame: Rect) = ImageEdit.Crop(
    ((r.left - frame.left) / frame.width).coerceIn(0f, 1f),
    ((r.top - frame.top) / frame.height).coerceIn(0f, 1f),
    ((r.right - frame.left) / frame.width).coerceIn(0f, 1f),
    ((r.bottom - frame.top) / frame.height).coerceIn(0f, 1f)
)

/**
 * L'anteprima su cui si lavora.
 *
 * Campionata a [PREVIEW] sul lato lungo: il ritaglio si sceglie a occhio, e l'originale intero
 * terrebbe centinaia di megabyte fermi mentre la schermata è aperta.
 *
 * ⚠️ **AVIF e SVG, che `ImageDecoder` non apre, passano dal ripiego di [ImageSource.pixels]**
 * ([ImageSource.rescue]). Se anche quello risponde `null`, la schermata resta nello stato di
 * attesa in cui parte.
 */
internal fun preview(context: Context, uri: Uri): Bitmap? =
    ImageSource.pixels(context, uri, PREVIEW)

/** Il lato lungo dell'anteprima. */
private const val PREVIEW = 1600

/** Quanto scurisce quello che il ritaglio butta via. */
internal const val VEIL = 0.55f

/** Il bordo del rettangolo, in pixel: sottile, perché copre l'immagine. */
internal const val EDGE_PX = 2f

/** Le righe dei terzi, più leggere del bordo. */
internal const val THIRD_PX = 1f

/**
 * L'ingrandimento della lente, il suo diametro e la sua distanza dai bordi del riquadro.
 *
 * Quattro volte, per il dito: un polpastrello copre una quarantina di punti, e in una lente da 112
 * a quattro ingrandimenti se ne vedono ventotto. Di più si perderebbe il contesto, di meno la lente
 * non servirebbe. Lo spessore dell'anello è quello delle squadrette ([lens]).
 */
internal const val LOUPE_ZOOM = 4f
internal val LOUPE_SIDE = 112.dp
internal val LOUPE_EDGE = 8.dp

/** Il braccio della squadretta d'angolo, e il suo spessore. */
internal val HANDLE_ARM = 24.dp
internal val HANDLE_THICK = 4.dp

/**
 * Il filo che contorna una maniglia.
 *
 * Un punto: basta a separare l'accento dal fondo, e su un braccio da 4 un filo da 2 per lato
 * farebbe una maniglia quasi tutta contorno. Il perché del filo è su [bracket].
 */
internal val GRIP_HALO = 1.dp

/** Il bordo del ritaglio dentro la lente, la mira: due punti, chiesti dall'utente ([lens]). */
internal val LENS_EDGE = 2.dp

/**
 * Quanto lontano dall'angolo il dito lo prende ancora.
 *
 * Più largo della squadretta disegnata, perché il dito copre quello che tocca: 40 dp è il
 * bersaglio comodo di Material.
 */
internal val GRIP = 40.dp

/**
 * Il lato più corto che un ritaglio può avere: distinto dalla presa ([GRIP]), così allargare il
 * bersaglio non allarga il ritaglio minimo.
 */
internal val LEAST_SIDE = 32.dp

/**
 * Il respiro intorno all'immagine nell'editor, e la sua distanza dai bordi laterali.
 *
 * ⚠️ **Di lato è più largo, per il dito** (richiesta dell'utente): il ritaglio non esce
 * dall'immagine, quindi per portarlo a filo il dito deve raggiungere il bordo dell'immagine, e con
 * una cover sporgente a dodici punti dal vetro non ci arriva. Sopra e sotto il riquadro confina
 * con testata e comandi, non col bordo, e lo spazio verticale è scarso.
 */
internal val STAGE_PAD = 12.dp
internal val STAGE_SIDE = 24.dp

/**
 * Chi modifica le immagini: si sceglie la prima volta e si cambia dalle impostazioni.
 *
 * Una finestra sola per le due strade, menu e impostazioni (richiesta dell'utente).
 * L'editor interno è in cima e non in ordine alfabetico: c'è sempre.
 * ⚠️ **Chiudere senza scegliere non ricorda niente**: la prossima volta la domanda si rifà
 * (in `Settings.editorApp` la stringa vuota vuol dire 'non ancora deciso').
 */
@Composable
fun EditorPicker(
    chosen: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // Chiesto una volta, fuori dal filo principale (rilievo B13): l'elenco è una scansione delle
    // app installate. L'editor interno è in cima da subito, gli altri arrivano dopo.
    val others by produceState(emptyList<Editors.Choice>()) {
        value = withContext(Dispatchers.IO) { Editors.installed(context) }
    }
    /*
     * ⚠️ **Due famiglie separate**: le prime riscrivono l'immagine dov'è, le seconde ne ricevono
     * una copia per condivisione e salvano dove decidono loro. Mescolate, la modifica si
     * cercherebbe nella cartella sbagliata. La seconda viene da un'euristica (`Editors`), e la
     * separazione lo dice.
     */
    val (edit, send) = remember(others) { others.partition { !it.shared } }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(onDismiss),
        properties = loweredWindow(onDismiss),
        title = { Text(stringResource(R.string.editor_pick)) },
        text = {
            // [Modifier.lowered] limita la larghezza; il tetto sull'altezza fa scorrere l'elenco
            // senza spingere i tasti fuori schermo (mockup `help`).
            val pickMaxH = (LocalConfiguration.current.screenHeightDp * 0.75f).dp
            Column(
                modifier = Modifier
                    .heightIn(max = pickMaxH)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // I due editor dell'app sono la sola voce 'Editor interno' (nota dell'utente su
                // `5.00-02`): quale si apre lo decide `Editors.INTERNAL`.
                PickRow(
                    label = stringResource(R.string.editor_internal),
                    icon = null,
                    here = Editors.isInternal(chosen),
                    onClick = { onPick(Editors.INTERNAL) }
                )
                edit.forEach { one ->
                    PickRow(
                        label = one.label,
                        icon = one.icon,
                        here = chosen == one.id,
                        onClick = { onPick(one.id) }
                    )
                }
                if (send.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.editor_pick_send),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 12.dp, start = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.editor_pick_send_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                    )
                    send.forEach { one ->
                        PickRow(
                            label = one.label,
                            icon = one.icon,
                            here = chosen == one.id,
                            onClick = { onPick(one.id) }
                        )
                    }
                }
                // La frase compare solo a elenco vuoto: con una voce sola la finestra sembrerebbe
                // ancora in caricamento.
                if (others.isEmpty()) {
                    Text(
                        text = stringResource(R.string.editor_pick_none),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
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
 * Una voce del selettore: icona, nome, e il fondo acceso su quella in vigore.
 *
 * Il tocco lo prende la riga intera, da un lato all'altro della finestra.
 */
@Composable
private fun PickRow(label: String, icon: Drawable?, here: Boolean, onClick: () -> Unit) {
    val side = with(LocalDensity.current) { PICK_ICON.roundToPx() }
    // ⚠️ Un `Drawable` va rasterizzato, con una misura data da noi: un'icona adattiva ha
    // `intrinsicWidth` -1, e quella misura darebbe un `IllegalArgumentException`.
    val shot = remember(icon, side) { icon?.let { asBitmap(it, side) } }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(
                if (here) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
            )
            .clickable(onClick = onClick)
            // La voce in vigore lo dichiara anche al lettore di schermo, oltre al fondo.
            .picked(here)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(PICK_ICON), contentAlignment = Alignment.Center) {
            // L'editor interno non è un'app e non ha un'icona di sistema: prende quella della voce
            // di menu, disegnata dall'utente, perché sia riconoscibile come la stessa cosa.
            if (shot != null) {
                Image(bitmap = shot, contentDescription = null, modifier = Modifier.size(PICK_ICON))
            } else {
                Icon(Glyphs.ImageEdit, contentDescription = null)
            }
        }
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** L'icona di un'app come immagine da disegnare, e `null` se non si lascia rasterizzare. */
private fun asBitmap(icon: Drawable, side: Int): ImageBitmap? = runCatching {
    icon.toBitmap(width = side, height = side).asImageBitmap()
}.getOrNull()

/** Il lato dell'icona nel selettore: quello di un'icona di lancio in un elenco. */
private val PICK_ICON = 32.dp

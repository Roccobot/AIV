package io.github.roccobot.aiv

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
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

@Composable
fun ViewerScreen(
    state: ViewerState,
    settings: Settings,
    source: Uri?,
    folder: Folder.Lookup?,
    onStep: (Int) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (state) {
            is ViewerState.Loading -> {
                PreviewThumb(source, settings)
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            is ViewerState.Error -> ErrorMessage(state, Modifier.align(Alignment.Center))
            is ViewerState.Ready -> ImageCanvas(state.image, settings, source, folder, onStep, onSettings)
        }
    }
}

/**
 * La MINIATURA mentre la fotografia vera si decodifica.
 *
 * ⚠️⚠️ **NON è un compromesso sulla qualità: è quello che si vede PRIMA di averla.**
 * Aprire una foto da 30 megapixel costa un tempo che si sente, e la strisciata da una
 * all'altra lo paga a ogni passo; questa è la stessa miniatura che la griglia ha già
 * decodificato, quindi nella maggior parte dei casi è già in memoria e compare
 * nell'istante del gesto. Quando la fotografia è pronta la sostituisce, intera.
 * ⚠️ La scala segue l'impostazione *ingrandisci le immagini piccole*, e non è un
 * dettaglio: con quella spenta una figura più piccola dello schermo resta alla sua
 * misura, quindi una miniatura sparata a pieno schermo salterebbe di posizione nel
 * momento in cui la vera arriva. `Inside` è esattamente 'adatta ma non ingrandire'.
 * ⚠️ Solo per gli indirizzi LOCALI: per un URL remoto non c'è nessuna miniatura da
 * chiedere al telefono, e questo caricatore non parla con la rete apposta.
 */
@Composable
private fun PreviewThumb(source: Uri?, settings: Settings) {
    val local = source?.scheme?.lowercase() == "content" || source?.scheme?.lowercase() == "file"
    if (source == null || !local) return
    val context = LocalContext.current
    val model = remember(source, context) { Thumbs.request(context, source) }
    AsyncImage(
        // ⚠️ La STESSA richiesta della griglia, misura compresa: è così che questa
        // immagine è già in memoria invece di essere chiesta di nuovo. Una misura
        // diversa sarebbe una chiave diversa, cioè un'altra generazione.
        model = model,
        contentDescription = null,
        contentScale = if (settings.fitGrow) ContentScale.Fit else ContentScale.Inside,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ErrorMessage(state: ViewerState.Error, modifier: Modifier = Modifier) {
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
    }
}

@Composable
private fun ImageCanvas(
    image: LoadedImage,
    settings: Settings,
    source: Uri?,
    folder: Folder.Lookup?,
    onStep: (Int) -> Unit,
    onSettings: () -> Unit
) {
    val density = LocalDensity.current
    val checkerPx = with(density) { CHECKER.toPx() }
    val lightGreys = when (settings.bgTheme) {
        BgTheme.LIGHT -> true
        BgTheme.DARK -> false
        BgTheme.AUTO -> MaterialTheme.colorScheme.background.luminanceIsLight()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .drawBehind { drawBackground(size, checkerPx, lightGreys, settings.bgType) }
    ) {
        val viewWidth = constraints.maxWidth.toFloat()
        val viewHeight = constraints.maxHeight.toFloat()
        val imageWidth = image.bitmap.width.toFloat()
        val imageHeight = image.bitmap.height.toFloat()

        /**
         * What '100%' is on THIS screen. Compose lays the picture out in device
         * pixels, so a scale of 1 already means one pixel of the file per pixel of
         * the screen: that is [ScaleMode.PHYSICAL]. Asking instead for one pixel
         * per LAYOUT pixel means scaling by the screen's density, which on a phone
         * is two or three.
         */
        val oneToOne = if (settings.scaleMode == ScaleMode.PHYSICAL) 1f else density.density

        val fitScale = min(viewWidth / imageWidth, viewHeight / imageHeight)
        // 'Fit' means shown whole. Growing a small picture to fill the screen is a
        // separate wish, and it is a setting rather than the default for the same
        // reason it is in the userscript: blowing up a 64px icon helps nobody.
        val restScale = if (settings.fitGrow) fitScale else min(fitScale, oneToOne)

        var scale by remember(image, settings) { mutableFloatStateOf(restScale) }
        var offset by remember(image, settings) { mutableStateOf(Offset.Zero) }
        var panelVisible by remember(image, settings) { mutableStateOf(settings.infoVisible) }
        var menuAt by remember(image) { mutableStateOf<Offset?>(null) }
        val scope = rememberCoroutineScope()

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
            val clamped = next.coerceIn(MIN_SCALE, settings.zoomMax)
            val fromCentre = anchor - Offset(viewWidth / 2f, viewHeight / 2f)
            val corrected = fromCentre - (fromCentre - offset) * (clamped / scale)
            scale = clamped
            offset = clampOffset(corrected + pan, clamped)
        }

        fun animateTo(target: Float) {
            scope.launch {
                val from = scale
                val animation = Animatable(from)
                animation.animateTo(target.coerceIn(MIN_SCALE, settings.zoomMax)) {
                    scale = value
                    offset = clampOffset(offset * (value / from), value)
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
            bitmap = image.bitmap,
            contentDescription = image.displayName,
            modifier = Modifier
                .align(Alignment.Center)
                .requiredSize(with(density) { imageWidth.toDp() }, with(density) { imageHeight.toDp() })
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )

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
         * cartella risponde e uccideva le pinze fatte subito dopo l'apertura.
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
                        swipeEnabled = { swipeAllowed },
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
                        onSwipe = onStep
                    )
                }
                .pointerInput(image, settings) {
                    detectViewerGestures(
                        onLongPress = { menuAt = it },
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

        menuAt?.let { at ->
            // The menu is anchored where the finger was, like the right click menu
            // it comes from. An empty Box at that point is the anchor: a
            // DropdownMenu positions itself against its parent, and it flips on its
            // own when there is no room below.
            Box(modifier = Modifier.offset { IntOffset(at.x.roundToInt(), at.y.roundToInt()) }) {
                ImageMenu(
                    image = image,
                    source = source,
                    onDismiss = { menuAt = null },
                    onZoom = { animateTo(it) },
                    oneToOne = oneToOne,
                    restScale = restScale,
                    onToggleDetails = { panelVisible = !panelVisible },
                    onSettings = onSettings
                )
            }
        }

        AnimatedVisibility(
            visible = panelVisible,
            modifier = Modifier.align(
                if (settings.infoPosition == InfoPosition.TOP) Alignment.TopCenter
                else Alignment.BottomCenter
            )
        ) {
            DetailsPanel(
                image = image,
                percent = scale / oneToOne,
                // ⚠️⚠️ **Il silenzio non basta a dire perché**, ed è il difetto che
                // ha fatto perdere DUE versioni sulla strisciata: senza serie il
                // gesto non fa niente, e 'non fa niente' è identico a un gesto
                // guasto. Qui l'esito arriva col suo motivo e la riga lo stampa.
                // ⚠️ Solo per una foto di questo telefono: su un'immagine del web o
                // di una chat 'non è nella galleria' è la normalità, non una notizia.
                folder = folder.takeIf { source?.scheme?.lowercase() == "content" }
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
 * di movimento consuma le variazioni e fa morire l'altro.
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
 * è uno solo e la strisciata è ammessa, lo spostamento orizzontale si **somma**
 * invece di muovere la figura, e al distacco decide se cambiare immagine.
 *
 * ⚠️ Un secondo dito **annulla** la strisciata in corso (`travel` torna a zero): chi
 * appoggia il pollice per pizzicare non sta chiedendo l'immagine dopo, e senza
 * questo una pinza cominciata storta la cambierebbe.
 */
private suspend fun PointerInputScope.detectPanZoomOrSwipe(
    swipeEnabled: () -> Boolean,
    swipeThreshold: () -> Float,
    onTransform: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    onSwipe: (Int) -> Unit
) {
    awaitEachGesture {
        val slop = viewConfiguration.touchSlop
        var zoomAcc = 1f
        var panAcc = Offset.Zero
        var past = false
        var multi = false
        var travel = 0f

        awaitFirstDown(requireUnconsumed = false)
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
                    } else if (zoomChange != 1f || panChange != Offset.Zero) {
                        onTransform(event.calculateCentroid(useCurrent = false), panChange, zoomChange)
                    }
                    // Si consuma in tutti e due i casi: è quello che dice al
                    // rilevatore dei tocchi che questo dito sta trascinando e non
                    // chiedendo il menu.
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })

        if (!canceled && travel <= -threshold) onSwipe(1)
        else if (!canceled && travel >= threshold) onSwipe(-1)
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
        } ?: return@awaitEachGesture

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
 */
@Composable
private fun ImageMenu(
    image: LoadedImage,
    source: Uri?,
    onDismiss: () -> Unit,
    onZoom: (Float) -> Unit,
    oneToOne: Float,
    restScale: Float,
    onToggleDetails: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun say(res: Int) = Toast.makeText(context, res, Toast.LENGTH_SHORT).show()

    // The system's own file picker, so saving needs no permission at all and the
    // person chooses where it lands. On Android 9, which this app still supports,
    // writing into the gallery through MediaStore would have wanted
    // WRITE_EXTERNAL_STORAGE.
    val saver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(image.mimeType ?: "image/*")
    ) { target ->
        if (target == null || source == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = context.contentResolver.openOutputStream(target)?.use { out ->
                ImageActions.copyOriginalTo(context, source, out)
            } ?: false
            say(if (ok) R.string.toast_saved else R.string.toast_save_failed)
        }
    }

    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_copy_image)) },
            onClick = {
                onDismiss()
                say(
                    if (ImageActions.copyImage(context, image)) R.string.toast_image_copied
                    else R.string.toast_copy_failed
                )
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_share)) },
            onClick = {
                onDismiss()
                scope.launch {
                    if (!ImageActions.share(context, image, source)) say(R.string.toast_copy_failed)
                }
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_save)) },
            onClick = {
                onDismiss()
                saver.launch(ImageActions.fileName(image, source))
            }
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text(stringResource(R.string.fit_label)) },
            onClick = { onDismiss(); onZoom(restScale) }
        )
        DropdownMenuItem(
            text = { Text("100%") },
            onClick = { onDismiss(); onZoom(oneToOne) }
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_details)) },
            onClick = { onDismiss(); onToggleDetails() }
        )
        // ⚠️ Le impostazioni sono ARRIVATE QUI nella 0.30, e da qui non se ne vanno: la
        // loro rotella stava in fondo alla riga dei dettagli, e quel posto serviva al
        // contatore della cartella. Sta per ultima perché è quella che porta più
        // lontano, che è il criterio dell'ordine di questo menu.
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_settings)) },
            onClick = { onDismiss(); onSettings() }
        )
        // ⚠️ LA RICERCA IMMAGINE NON C'È PIÙ, dalla 0.18, e non è una dimenticanza:
        // l'utente l'ha spenta dopo averla provata sul telefono, perché non
        // funzionava e faceva solo rumore in un menu tenuto corto apposta. Con
        // lei sono usciti il suo motore fra le impostazioni, le tre risposte
        // all'esito e la dichiarazione `queries` del manifest, che serviva solo a
        // lei. Il codice sta nella storia git, al tag `v0.17`, e ci si torna se
        // l'utente riporta il feedback che ha detto di voler raccogliere.
    }
}

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
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildString {
                    append(image.mimeType?.substringAfter('/')?.uppercase() ?: "?")
                    append("  ")
                    append(image.pixelWidth).append(" x ").append(image.pixelHeight)
                    image.byteSize?.let { append("  ").append(formatBytes(it)) }
                    append("  ").append((percent * 100).roundToInt()).append('%')
                    if (image.sampled) append("  (sampled)")
                    // ⚠️ Il perché di una cartella che non c'è resta QUI, col resto del
                    // testo, e non va nell'angolo del contatore: è una frase, non un
                    // numero, e in quello spazio starebbe stretta o lo farebbe crescere
                    // rimettendo in movimento il contatore che si è appena fissato.
                    folderNote?.let { append("  ").append(it) }
                },
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
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
 */
private fun formatBytes(value: Long): String = when {
    value < 1024 -> "$value B"
    value < 1024 * 1024 -> String.format(Locale.US, "%.1f kB", value / 1024f)
    else -> String.format(Locale.US, "%.2f MB", value / (1024f * 1024f))
}

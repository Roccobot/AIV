package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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

/**
 * L'editor di casa: si gira di novanta gradi e si ritaglia, e basta.
 *
 * ⚠️⚠️ **IL PERIMETRO È DICHIARATO E NON È UNA MANCANZA** (richiesta dell'utente: *AIV non ha
 * come obiettivo di soppiantare un editor completo*): niente filtri, niente luminosità, niente
 * pennelli. Chi vuole quelli sceglie un'app vera dalla stessa schermata, ed è la ragione per
 * cui la scelta dell'editor esiste. Qui c'è il minimo che serve a raddrizzare una foto storta
 * e a togliere il bordo di troppo, cioè le due cose che si vogliono fare mentre si guarda.
 *
 * ⚠️⚠️ **L'ANTEPRIMA È RIMPICCIOLITA, IL SALVATAGGIO NO**: qui dentro si lavora su una copia
 * campionata, perché il ritaglio si sceglie col dito su uno schermo e venti megapixel non
 * servirebbero a niente se non a far scattare la memoria. Il rettangolo si tiene in
 * **frazioni** del lato e non in pixel, e al salvataggio viene applicato al file vero (vedi
 * `ImageEdit`).
 */
@Composable
fun EditorScreen(
    uri: Uri,
    /** Il nome del file: serve a sapere se si può sovrascrivere. */
    name: String,
    /** Se una scrittura è in corso: i comandi si spengono, o si salverebbe due volte. */
    busy: Boolean,
    /**
     * Che cosa salvare e come.
     *
     * ⚠️⚠️ **IL LAVORO LO FA CHI CHIAMA, e non questa schermata**: una scrittura da venti
     * megapixel dura secondi, e appesa alla composizione morirebbe nel momento in cui la
     * schermata si chiude, cioè proprio quando l'utente ha finito. Nell'ambito del modello
     * invece arriva in fondo.
     */
    onSave: (turns: Int, crop: ImageEdit.Crop, way: ImageEdit.Way) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var base by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var turns by remember(uri) { mutableIntStateOf(0) }
    var shape by remember(uri) { mutableStateOf(Shape.FREE) }
    var crop by remember(uri) { mutableStateOf(ImageEdit.Crop.WHOLE) }
    var asking by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    LaunchedEffect(uri) {
        base = withContext(Dispatchers.IO) { preview(context, uri) }
    }

    // ⚠️ L'anteprima girata si ricalcola SOLO quando cambia il quarto di giro: girare una
    // mappa di pixel da due megapixel a ogni ridisegno vorrebbe dire farlo a ogni dito che
    // si muove sul rettangolo.
    val shown: ImageBitmap? = remember(base, turns) {
        val bitmap = base ?: return@remember null
        if (turns == 0) bitmap.asImageBitmap()
        else Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height,
            Matrix().apply { postRotate(90f * turns) },
            true
        ).asImageBitmap()
    }

    /*
     * ⚠️⚠️ **L'ORIENTAMENTO DELLA SELEZIONE LO DECIDE LA FOTOGRAFIA, e il quadrato conta come
     * verticale** (richiesta dell'utente, 2026-08-31). La chiave è [base] e non [uri]: prima
     * che l'anteprima arrivi non si sa che forma abbia, e un valore scelto a scatola chiusa
     * sarebbe sbagliato la metà delle volte.
     * ⚠️ **Girare la fotografia NON lo cambia**, ed è una scelta: dopo il primo tocco
     * l'orientamento è una decisione dell'utente, e una rotazione che gliela ribalta sotto le
     * dita gli toglie il comando. Il valore di partenza si decide una volta.
     */
    var lay by remember(base) { mutableStateOf(startLay(base)) }

    /**
     * Quante volte l'immagine mostrata è più larga che alta.
     *
     * ⚠️ Si ricava da [shown] e non dal riquadro disegnato, che è lo stesso numero: così i
     * tasti qui sotto possono fare i loro conti senza sapere niente di dove sta l'immagine
     * sullo schermo.
     */
    val aspect = shown?.let { it.width.toFloat() / it.height } ?: 1f

    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back))
            }
            Text(
                text = stringResource(R.string.menu_edit),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = { asking = true },
                enabled = shown != null && !busy && !(turns == 0 && crop.whole)
            ) {
                Text(stringResource(R.string.editor_save))
            }
        }

        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(STAGE_PAD),
            contentAlignment = Alignment.Center
        ) {
            val picture = shown
            if (picture == null) {
                CircularProgressIndicator()
            } else {
                val room = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
                val frame = remember(picture, room) { fitted(picture, room) }
                // ⚠️ Il rettangolo si rifà a ogni GIRO, e non a ogni cambio di forma: quelli
                // li rifà il tasto che li sceglie, perché deve poterlo fare anche quando la
                // forma è già quella (ritoccato il ritaglio, si ritocca il tasto per
                // rimetterlo intero). Dopo una rotazione invece un rettangolo tenuto
                // taglierebbe un pezzo diverso da quello che si vedeva.
                LaunchedEffect(turns) { crop = shape.fit(aspect, lay) }
                val density = LocalDensity.current
                CropStage(
                    picture = picture,
                    frame = frame,
                    crop = crop,
                    grip = with(density) { GRIP.toPx() },
                    least = with(density) { LEAST_SIDE.toPx() },
                    arm = with(density) { HANDLE_ARM.toPx() },
                    thick = with(density) { HANDLE_THICK.toPx() },
                    onCrop = { crop = it },
                    keep = shape.value(lay)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            /*
             * ⚠️ **Le frecce circolari e nient'altro** (richiesta dell'utente, 2026-08-31: le
             * icone di rotazione devono essere le due frecce *senza forme geometriche*). Le
             * `Rotate90Degrees*` di Material portano un quadrato che qui non vuol dire niente:
             * non si gira un quadrato, si gira la fotografia.
             * ⚠️⚠️ **NON si passa alle versioni `AutoMirrored`, e la deprecazione si zittisce
             * apposta**: quelle si specchiano nelle lingue che si leggono da destra, e una
             * freccia antioraria specchiata **diventa oraria**. Il verso di un giro è fisico,
             * non dipende da come si legge: il tasto direbbe il falso in arabo, in persiano e
             * in urdu, cioè in tre delle ventotto lingue dell'app. Il rispecchiamento serve
             * alle frecce che indicano 'indietro' e 'avanti', dove il verso È quello della
             * lettura.
             */
            @Suppress("DEPRECATION") val ccw = Icons.Default.RotateLeft
            @Suppress("DEPRECATION") val cw = Icons.Default.RotateRight
            IconButton(onClick = { turns = (turns + 3).mod(4) }, enabled = !busy) {
                Icon(ccw, stringResource(R.string.editor_left))
            }
            IconButton(onClick = { turns = (turns + 1).mod(4) }, enabled = !busy) {
                Icon(cw, stringResource(R.string.editor_right))
            }
        }

        /*
         * ⚠️⚠️ **DUE TASTI CHE SI ESCLUDONO, e cambiarli RIBALTA la selezione sul posto**
         * (richiesta dell'utente, 2026-08-31): da 16:9 si passa a 9:16, e una selezione libera
         * si inverte allo stesso modo. ⚠️ **Il centro non si muove**, ed è la parte che rende
         * il gesto utile invece che spaesante: si sta scegliendo *che forma* dare al ritaglio,
         * non *dove* metterlo. Il conto sta in [flipped], che sposta il centro solo quando il
         * rettangolo ribaltato uscirebbe dall'immagine.
         */
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = STAGE_PAD),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (one in Lay.entries) {
                FilterChip(
                    selected = one == lay,
                    onClick = {
                        if (one != lay) {
                            crop = flipped(crop, aspect)
                            lay = one
                        }
                    },
                    enabled = !busy,
                    label = { Text(stringResource(one.label)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ⚠️ Le proporzioni sono CINQUE e non otto, perché le stesse quattro forme lette
        // nell'altro verso sono le altre quattro: '2:3' e '3:2' non sono due scelte, sono la
        // stessa scelta con l'orientamento girato. Vedi [Shape].
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = STAGE_PAD, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (one in Shape.entries) {
                FilterChip(
                    selected = one == shape,
                    onClick = { shape = one; crop = one.fit(aspect, lay) },
                    enabled = !busy,
                    label = { Text(one.text(lay) ?: stringResource(R.string.editor_free)) }
                )
            }
        }
    }

    if (asking) {
        SaveDialog(
            canOverwrite = ImageEdit.canOverwrite(name),
            onPick = { way ->
                asking = false
                onSave(turns, crop, way)
            },
            onDismiss = { asking = false }
        )
    }
}

/** Come sta la selezione: in piedi o coricata. Vedi i due tasti in [EditorScreen]. */
private enum class Lay(@StringRes val label: Int) {
    TALL(R.string.editor_tall),
    WIDE(R.string.editor_wide)
}

/**
 * Con che forma si ritaglia, **senza** dire da che parte sta.
 *
 * ⚠️⚠️ **QUATTRO FORME E NON OTTO, ed è la struttura che la richiesta dell'utente impone**:
 * '2:3' e '3:2' non sono due scelte diverse, sono la stessa forma letta nei due versi, e chi
 * le tiene separate deve poi tenere d'accordo due elenchi ogni volta che l'orientamento
 * cambia. Qui il verso lo dà [Lay], e la forma resta selezionata mentre gli si gira intorno.
 * ⚠️ **Il valore è larghezza diviso altezza in verticale**, e in orizzontale è il suo
 * reciproco: un numero solo per forma, e l'inversione è una divisione.
 * ⚠️ **`null` è 'libero'**: con un numero anche per quello servirebbe un caso speciale in
 * ogni conto, mentre così il caso speciale è uno solo e sta qui.
 * ⚠️ **Le etichette NON sono risorse, tranne 'Libero'**: '16:9' si scrive uguale in tutte le
 * lingue, e metterlo in ventotto file vorrebbe dire ventotto occasioni di scriverlo storto
 * per zero traduzioni.
 */
private enum class Shape(val tall: Float?, private val up: String?, private val flat: String?) {
    FREE(null, null, null),
    ONE(1f, "1:1", "1:1"),
    TWO_THREE(2f / 3f, "2:3", "3:2"),
    THREE_FOUR(3f / 4f, "3:4", "4:3"),
    NINE_SIXTEEN(9f / 16f, "9:16", "16:9");

    /** Larghezza diviso altezza in questo verso, e `null` se la forma è libera. */
    fun value(lay: Lay): Float? = tall?.let { if (lay == Lay.TALL) it else 1f / it }

    /** Come si scrive in questo verso, e `null` per 'libero', che è una risorsa. */
    fun text(lay: Lay): String? = if (lay == Lay.TALL) up else flat

    /**
     * Il rettangolo più grande con questa forma dentro un'immagine larga [frame] volte la sua
     * altezza, centrato, in frazioni.
     */
    fun fit(frame: Float, lay: Lay): ImageEdit.Crop {
        val want = value(lay) ?: return ImageEdit.Crop.WHOLE
        return if (want >= frame) {
            // Sta largo quanto l'immagine, e avanza sopra e sotto.
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

/** Da che parte sta una fotografia. ⚠️ Il quadrato conta come verticale: lo chiede l'utente. */
private fun startLay(base: Bitmap?): Lay =
    if (base != null && base.width > base.height) Lay.WIDE else Lay.TALL

/**
 * La stessa selezione girata di quarto: larghezza e altezza si scambiano, il centro resta.
 *
 * ⚠️⚠️ **LO SCAMBIO È IN PIXEL E NON IN FRAZIONI, ed è l'unico modo perché 16:9 diventi
 * 9:16**: le frazioni sono relative ai due lati dell'immagine, che sono diversi, quindi
 * scambiarle darebbe un rettangolo con una proporzione che non è né l'una né l'altra. Il
 * passaggio per i pixel è la moltiplicazione e la divisione per [frame] qui sotto.
 * ⚠️ **Se il rettangolo girato non ci sta, si RIMPICCIOLISCE invece di deformarsi**, e solo
 * dopo, se serve, si sposta il centro quel tanto che basta a rientrare: la forma è la cosa
 * che si è chiesta, la posizione è quella che si può cedere.
 */
private fun flipped(crop: ImageEdit.Crop, frame: Float): ImageEdit.Crop {
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
 * ⚠️⚠️ **IL RETTANGOLO SI DISEGNA E SI TOCCA IN PIXEL DI SCHERMO, e si conserva in
 * frazioni**: fare i conti in frazioni dentro il gesto vorrebbe dire dividere e moltiplicare
 * a ogni movimento del dito, e un errore di un pixel diventerebbe un errore diverso su ogni
 * schermo. Qui si converte una volta all'entrata e una all'uscita.
 * ⚠️ **La presa dell'angolo è più grande dell'angolo disegnato**: un bersaglio da 12dp non si
 * prende, e allargare il disegno per allargare il bersaglio coprirebbe la fotografia.
 *
 * ⚠️⚠️ **IL RETTANGOLO IN CORSO VIVE DENTRO IL GESTO, e NON si rilegge da [crop] a ogni
 * spostamento**: è la correzione del 2026-08-31, e il difetto che toglieva era totale
 * (segnalazione dell'utente: *la selezione non può essere ridimensionata né spostata,
 * traballa senza rispondere*). La ragione è che il blocco di `pointerInput` è una **coroutine
 * che dura quanto le sue chiavi**: il `crop` che vede è quello catturato quando è partita, e
 * resta quello per tutto il trascinamento. Ogni movimento del dito calcolava quindi
 * `iniziale + delta` invece di `corrente + delta`, cioè il rettangolo tornava indietro a ogni
 * fotogramma e restava a tremare intorno al punto di partenza.
 * ⚠️ **Non basta togliere le chiavi né aggiungerne**: con `crop` fra le chiavi il gesto
 * verrebbe **riavviato** a ogni movimento, cioè si perderebbe il trascinamento invece di
 * sbagliarlo. La cosa giusta è accumulare qui dentro, che è anche dove il conto ha senso.
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
    keep: Float?,
    onCrop: (ImageEdit.Crop) -> Unit
) {
    val dim = Color.Black.copy(alpha = VEIL)
    val line = Color.White
    var held by remember { mutableStateOf(Grab.NONE) }
    // ⚠️ Il valore più fresco senza rifare il gesto: `rememberUpdatedState` è fatto apposta
    // per quello che sta dentro una coroutine di lunga vita.
    val now by rememberUpdatedState(crop)
    val report by rememberUpdatedState(onCrop)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(frame, keep, least) {
                var going = Rect.Zero
                detectDragGestures(
                    onDragStart = { at ->
                        going = box(now, frame)
                        held = grabbed(at, going, grip)
                    },
                    onDragEnd = { held = Grab.NONE },
                    onDragCancel = { held = Grab.NONE }
                ) { change, delta ->
                    change.consume()
                    if (held == Grab.NONE) return@detectDragGestures
                    going = dragged(going, held, delta, frame, keep, least)
                    report(fractions(going, frame))
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawImage(
                image = picture,
                dstOffset = IntOffset(frame.left.roundToInt(), frame.top.roundToInt()),
                dstSize = IntSize(frame.width.roundToInt(), frame.height.roundToInt())
            )
            val r = box(crop, frame)
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
            // ⚠️ I terzi si disegnano sempre e non solo mentre si trascina: sono la ragione
            // per cui un ritaglio viene dritto, e comparendo solo al tocco arriverebbero dopo
            // che la decisione è presa.
            for (k in 1..2) {
                val x = r.left + r.width * k / 3f
                val y = r.top + r.height * k / 3f
                drawLine(line.copy(alpha = 0.35f), Offset(x, r.top), Offset(x, r.bottom), THIRD_PX)
                drawLine(line.copy(alpha = 0.35f), Offset(r.left, y), Offset(r.right, y), THIRD_PX)
            }
            /*
             * ⚠️⚠️ **QUATTRO SQUADRETTE E NON QUATTRO QUADRATINI** (richiesta dell'utente,
             * 2026-08-31: *manopole angolari più grandi e visibili*). Un quadratino centrato
             * sull'angolo dice 'qui c'è un punto'; una squadretta appoggiata ai due lati dice
             * **quali due lati** quel punto muove, che è l'informazione che serve mentre si
             * tira. È anche la forma che ogni ritaglio moderno usa, quindi non va imparata.
             * ⚠️ **Il braccio si accorcia sui ritagli piccoli**: a lato pieno resterebbe più
             * lungo di metà rettangolo, e le due squadrette opposte si toccherebbero.
             */
            val reach = min(arm, min(r.width, r.height) / 2.5f)
            bracket(Offset(r.left, r.top), 1, 1, reach, thick, line)
            bracket(Offset(r.right, r.top), -1, 1, reach, thick, line)
            bracket(Offset(r.left, r.bottom), 1, -1, reach, thick, line)
            bracket(Offset(r.right, r.bottom), -1, -1, reach, thick, line)
        }
    }
}

/** Quale presa ha preso il dito. */
private enum class Grab { NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, INSIDE }

/**
 * Una squadretta d'angolo: due bracci arrotondati appoggiati DENTRO al rettangolo.
 *
 * ⚠️ **Dentro e non a cavallo del bordo**: a cavallo coprirebbe un pezzo di fotografia fuori
 * dal ritaglio, e quello che si sta guardando mentre si tira è proprio il bordo.
 * [dx] e [dy] valgono `1` se da quell'angolo si va verso destra o verso il basso, `-1` se no.
 */
private fun DrawScope.bracket(
    at: Offset,
    dx: Int,
    dy: Int,
    arm: Float,
    thick: Float,
    color: Color
) {
    val round = CornerRadius(thick / 2f, thick / 2f)
    drawRoundRect(
        color = color,
        topLeft = Offset(if (dx > 0) at.x else at.x - arm, if (dy > 0) at.y else at.y - thick),
        size = Size(arm, thick),
        cornerRadius = round
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(if (dx > 0) at.x else at.x - thick, if (dy > 0) at.y else at.y - arm),
        size = Size(thick, arm),
        cornerRadius = round
    )
}

private fun grabbed(at: Offset, r: Rect, grip: Float): Grab {
    val near = listOf(
        Grab.TOP_LEFT to Offset(r.left, r.top),
        Grab.TOP_RIGHT to Offset(r.right, r.top),
        Grab.BOTTOM_LEFT to Offset(r.left, r.bottom),
        Grab.BOTTOM_RIGHT to Offset(r.right, r.bottom)
    ).minByOrNull { (_, corner) -> (corner - at).getDistance() }
    if (near != null && (near.second - at).getDistance() <= grip) {
        // ⚠️⚠️ **MA NON QUANDO IL DITO È PIÙ VICINO AL CENTRO CHE ALL'ANGOLO**: su un ritaglio
        // piccolo tutti e quattro gli angoli cadono dentro la presa, e senza questo confronto
        // il rettangolo si potrebbe solo ridimensionare, mai più spostare. Il centro è
        // l'unico riferimento che non dipende da quanto è grande la presa.
        if ((near.second - at).getDistance() <= (r.center - at).getDistance()) return near.first
    }
    return if (r.contains(at)) Grab.INSIDE else Grab.NONE
}

/**
 * Il rettangolo dopo lo spostamento del dito.
 *
 * ⚠️⚠️ **CON UNA PROPORZIONE BLOCCATA SI MUOVE UN LATO E L'ALTRO SEGUE**, e l'angolo opposto
 * sta fermo: è l'unico modo in cui un ritaglio 16:9 resta 16:9 mentre lo si tira. Lasciando
 * liberi tutti e due i lati la proporzione si perderebbe al primo movimento obliquo.
 */
private fun dragged(
    r: Rect,
    held: Grab,
    delta: Offset,
    frame: Rect,
    keep: Float?,
    /** Il lato più corto che un ritaglio può avere. ⚠️ Non è la presa: quella è più larga. */
    least: Float
): Rect {
    if (held == Grab.INSIDE) {
        val dx = delta.x.coerceIn(frame.left - r.left, frame.right - r.right)
        val dy = delta.y.coerceIn(frame.top - r.top, frame.bottom - r.bottom)
        return r.translate(dx, dy)
    }
    val small = least
    var left = r.left
    var top = r.top
    var right = r.right
    var bottom = r.bottom
    when (held) {
        Grab.TOP_LEFT -> { left += delta.x; top += delta.y }
        Grab.TOP_RIGHT -> { right += delta.x; top += delta.y }
        Grab.BOTTOM_LEFT -> { left += delta.x; bottom += delta.y }
        Grab.BOTTOM_RIGHT -> { right += delta.x; bottom += delta.y }
        else -> Unit
    }
    left = left.coerceIn(frame.left, right - small)
    right = right.coerceIn(left + small, frame.right)
    top = top.coerceIn(frame.top, bottom - small)
    bottom = bottom.coerceIn(top + small, frame.bottom)

    if (keep == null) return Rect(left, top, right, bottom)

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

/** Dove sta l'immagine dentro lo spazio disponibile, a filo e centrata. */
private fun fitted(picture: ImageBitmap, room: Size): Rect {
    if (room.width <= 0f || room.height <= 0f) return Rect(Offset.Zero, Size(1f, 1f))
    val k = min(room.width / picture.width, room.height / picture.height)
    val w = picture.width * k
    val h = picture.height * k
    val x = (room.width - w) / 2f
    val y = (room.height - h) / 2f
    return Rect(x, y, x + w, y + h)
}

private fun box(crop: ImageEdit.Crop, frame: Rect) = Rect(
    frame.left + crop.left * frame.width,
    frame.top + crop.top * frame.height,
    frame.left + crop.right * frame.width,
    frame.top + crop.bottom * frame.height
)

private fun fractions(r: Rect, frame: Rect) = ImageEdit.Crop(
    ((r.left - frame.left) / frame.width).coerceIn(0f, 1f),
    ((r.top - frame.top) / frame.height).coerceIn(0f, 1f),
    ((r.right - frame.left) / frame.width).coerceIn(0f, 1f),
    ((r.bottom - frame.top) / frame.height).coerceIn(0f, 1f)
)

/**
 * Il dialogo del salvataggio.
 *
 * ⚠️⚠️ **CHIEDE SEMPRE, ed è una richiesta dell'utente**: sovrascrivere è l'unica cosa
 * irreversibile che questa schermata sa fare, e un editor che salva sopra senza domandare
 * distrugge un originale al primo tocco sbagliato.
 * ⚠️ **Con un formato che non si sa riscrivere resta la sola copia**, e il dialogo lo dice
 * invece di mostrare un tasto spento: un tasto che non si può premere non spiega perché.
 */
@Composable
private fun SaveDialog(
    canOverwrite: Boolean,
    onPick: (ImageEdit.Way) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_how)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!canOverwrite) {
                    Text(
                        text = stringResource(R.string.edit_no_overwrite),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canOverwrite) {
                    TextButton(
                        onClick = { onPick(ImageEdit.Way.OVERWRITE) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.editor_overwrite)) }
                }
                TextButton(
                    onClick = { onPick(ImageEdit.Way.COPY) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.editor_copy)) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pick_close)) }
        }
    )
}

/**
 * L'anteprima su cui si lavora.
 *
 * ⚠️ **Campionata a [PREVIEW] sul lato lungo**: il ritaglio si sceglie a occhio su uno
 * schermo da mille punti, e tenere in memoria l'originale intero per tutto il tempo in cui la
 * schermata è aperta vorrebbe dire duecento megabyte fermi mentre si decide.
 */
private fun preview(context: Context, uri: Uri): Bitmap? = runCatching {
    ImageDecoder.decodeBitmap(
        ImageDecoder.createSource(context.contentResolver, uri)
    ) { decoder, info, _ ->
        val long = max(info.size.width, info.size.height)
        if (long > PREVIEW) decoder.setTargetSampleSize(sample(long))
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.isMutableRequired = false
    }
}.getOrNull()

/** Il campionamento: potenza di due, come `ImageDecoder` vuole. */
private fun sample(long: Int): Int {
    var step = 1
    while (long / (step * 2) >= PREVIEW) step *= 2
    return step
}

/** Il lato lungo dell'anteprima. */
private const val PREVIEW = 1600

/** Quanto scurisce quello che il ritaglio butta via. */
private const val VEIL = 0.55f

/** Il bordo del rettangolo, in pixel: sottile, perché copre la fotografia. */
private const val EDGE_PX = 2f

/** Le righe dei terzi, più leggere del bordo. */
private const val THIRD_PX = 1f

/** Il braccio della squadretta d'angolo, e il suo spessore. */
private val HANDLE_ARM = 24.dp
private val HANDLE_THICK = 4.dp

/**
 * Quanto lontano dall'angolo il dito lo prende ancora.
 *
 * ⚠️ **Più largo della squadretta disegnata, e non per generosità**: il dito copre quello che
 * tocca, quindi una presa grande esattamente quanto il disegno si prende solo guardando. 40dp
 * è la misura che Material dà a un bersaglio comodo.
 */
private val GRIP = 40.dp

/**
 * Il lato più corto che un ritaglio può avere.
 *
 * ⚠️ **Separato dalla PRESA, e prima era lo stesso numero**: con un solo valore, allargare il
 * bersaglio del dito allargava anche il ritaglio minimo, cioè si perdeva la possibilità di
 * ritagliare in piccolo per guadagnare comodità. Sono due cose diverse e adesso lo sono anche
 * nel codice.
 */
private val LEAST_SIDE = 32.dp

/** Il respiro intorno alla fotografia nell'editor. */
private val STAGE_PAD = 12.dp

/**
 * Chi modifica le fotografie: si sceglie la prima volta e si cambia dalle impostazioni.
 *
 * ⚠️⚠️ **UNA FINESTRA SOLA PER DUE PORTE** (richiesta dell'utente: il tasto delle
 * impostazioni dev'essere *lo stesso che si presenta al primo utilizzo dal menu*). Due
 * finestre gemelle sarebbero divergite alla prima voce aggiunta, e la promessa 'lo stesso'
 * sarebbe diventata falsa senza che nessuno se ne accorgesse.
 * ⚠️⚠️ **L'editor di casa sta in CIMA e non in ordine alfabetico fra gli altri**: è l'unico
 * che c'è sempre, e su un telefono senza nessun editor installato sarebbe l'unica voce
 * dell'elenco. Metterlo in fila lo farebbe cercare.
 * ⚠️ **Chiudere senza scegliere NON ricorda niente**, ed è la differenza fra 'non ho ancora
 * deciso' e 'ho deciso nessuna': la prossima volta la domanda si rifà. Vedi
 * `Settings.editorApp`, dove la stringa vuota è proprio quel 'non ho ancora deciso'.
 */
@Composable
fun EditorPicker(
    chosen: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // ⚠️ Chiesto una volta sola: l'elenco viene dal `PackageManager`, cioè da una scansione
    // delle app installate, e questa finestra si ridisegna a ogni tocco.
    val others = remember { Editors.installed(context) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_pick)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                PickRow(
                    label = stringResource(R.string.editor_internal),
                    icon = null,
                    here = chosen == Editors.INTERNAL,
                    onClick = { onPick(Editors.INTERNAL) }
                )
                others.forEach { one ->
                    PickRow(
                        label = one.label,
                        icon = one.icon,
                        here = chosen == one.id,
                        onClick = { onPick(one.id) }
                    )
                }
                // ⚠️ La frase compare solo a elenco vuoto, e serve: senza, la finestra
                // mostrerebbe una voce sola e sembrerebbe non aver finito di caricare.
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
 * ⚠️ **Il tocco lo prende la RIGA intera** e non l'etichetta: è un bersaglio da un lato
 * all'altro della finestra, cioè quello che un elenco di scelte deve essere.
 */
@Composable
private fun PickRow(label: String, icon: Drawable?, here: Boolean, onClick: () -> Unit) {
    val side = with(LocalDensity.current) { PICK_ICON.roundToPx() }
    // ⚠️⚠️ **UN `Drawable` NON SI DISEGNA IN COMPOSE, va rasterizzato**, e la misura la si
    // deve dare noi: l'icona di un'app è spesso adattiva, cioè non ha una misura sua e
    // `intrinsicWidth` torna -1. Chiedendola a quel numero si otterrebbe un `IllegalArgument`
    // proprio sulle icone più comuni.
    val shot = remember(icon, side) { icon?.let { asBitmap(it, side) } }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(
                if (here) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(PICK_ICON), contentAlignment = Alignment.Center) {
            // ⚠️ L'editor di casa non ha un'icona di sistema perché non è un'app: prende la
            // stessa matita della voce di menu, che è il modo per dire che è la stessa cosa.
            if (shot != null) {
                Image(bitmap = shot, contentDescription = null, modifier = Modifier.size(PICK_ICON))
            } else {
                Icon(Icons.Outlined.Edit, contentDescription = null)
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

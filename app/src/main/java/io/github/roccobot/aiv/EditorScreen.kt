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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.RestartAlt
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    /** Se una scrittura è in corso: i comandi si spengono, o si salverebbe due volte. */
    busy: Boolean,
    /**
     * Che cosa salvare.
     *
     * ⚠️⚠️ **IL LAVORO LO FA CHI CHIAMA, e non questa schermata**: una scrittura da venti
     * megapixel dura secondi, e appesa alla composizione si interromperebbe nel momento in cui la
     * schermata si chiude, cioè proprio quando l'utente ha finito. Nell'ambito del modello
     * invece arriva in fondo.
     * ⚠️⚠️ **E DALLA 1.08 NON DICE PIÙ 'COME', ed è la richiesta dell'utente**: si sovrascrive
     * e basta. Il perché sta sul tasto Salva. Anche la scelta fra sovrascrivere e copiare non
     * è più di questa schermata: la decide il formato del file, e il formato lo conosce il
     * modello.
     */
    onSave: (turns: Int, crop: ImageEdit.Crop) -> Unit,
    onBack: () -> Unit,
    /**
     * Se i comandi vanno disposti per la mano **sinistra**.
     *
     * ⚠️ Non cambia che cosa fanno, cambia **dove stanno**: vedi [EditorSheet], dove le due
     * file sono scritte per esteso nei due versi. È la stessa impostazione che rovescia le
     * file della bottomsheet della selezione.
     */
    leftHand: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var origin by remember(uri) { mutableStateOf<Bitmap?>(null) }

    /**
     * I passi già confermati con 'Applica', dal primo all'ultimo.
     *
     * ⚠️⚠️ **OGNI PASSO PORTA LA TRASFORMAZIONE COMPOSTA DALL'ORIGINALE, non la propria**, e
     * questa è la scelta che tiene in piedi tutto il resto: al salvataggio serve **una**
     * rotazione e **un** rettangolo, perché è quello che `ImageEdit` sa applicare al file
     * vero. Tenendo la catena dei passi bisognerebbe comporla là, cioè in un posto che di
     * ritagli non sa niente. Vedi [after] per il conto, che è esatto e non approssima.
     * ⚠️ **E porta anche l'anteprima che ne esce**, perché disfare vuol dire ritrovarla:
     * ricalcolarla dall'originale a ogni 'Annulla' costerebbe una decodifica per un tasto che
     * si preme di fretta.
     */
    var steps by remember(uri) { mutableStateOf<List<Step>>(emptyList()) }

    /**
     * I passi disfatti con 'Annulla', pronti a tornare con 'Ripristina'.
     *
     * ⚠️⚠️ **'RIPRISTINA' QUI VUOL DIRE 'UN PASSO AVANTI', non 'com'era all'inizio'**
     * (chiarito dall'utente, 2026-09-01, che l'icona sbagliata aveva fatto leggere al
     * contrario): il ritorno all'originale è un tasto a sé, **'Originale'**, e la sua icona è
     * quella del riavvio. Chi scambia i due scambia un passo con tutta la storia.
     * ⚠️ **Un 'Applica' nuovo la svuota**, come in ogni editor: da lì in poi la strada è
     * un'altra, e i passi disfatti appartenevano a quella vecchia.
     */
    var undone by remember(uri) { mutableStateOf<List<Step>>(emptyList()) }

    /** L'immagine su cui si sta lavorando adesso: l'ultimo passo, o l'originale. */
    val base = steps.lastOrNull()?.preview ?: origin

    var turns by remember(base) { mutableIntStateOf(0) }
    var shape by remember(base) { mutableStateOf(Shape.FREE) }
    var crop by remember(base) { mutableStateOf(ImageEdit.Crop.WHOLE) }

    BackHandler { onBack() }

    LaunchedEffect(uri) {
        origin = withContext(Dispatchers.IO) { preview(context, uri) }
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

    /** Se c'è qualcosa di non ancora confermato con 'Applica'. */
    val pending = turns != 0 || !crop.whole

    /** Tutto quello che si è fatto finora, composto in una rotazione e un rettangolo soli. */
    val total = after(steps.lastOrNull()?.done ?: Done.NOTHING, turns, crop)

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
            /*
             * ⚠️⚠️ **SALVA SOVRASCRIVE, E NON CHIEDE PIÙ, dalla 1.08** (richiesta dell'utente:
             * *dato che ora c'è la rete di sicurezza dell'immagine nel cestino, rendi
             * predefinita e non modificabile la sovrascrittura*). Fino alla `1.07` qui si
             * apriva un dialogo con 'Sovrascrivi' e 'Salva una copia', e la ragione scritta
             * allora era giusta: sovrascrivere era l'unica cosa irreversibile che questa
             * schermata sapeva fare. Non lo è più, perché la `1.03` ha portato la copia di
             * sicurezza nel cestino: la domanda proteggeva da un rischio che nel frattempo
             * qualcun altro ha coperto, e una domanda che non protegge più da niente è solo
             * un tocco in più a ogni salvataggio.
             * ⚠️ **Il caso 'non si può sovrascrivere' non sparisce, cambia posto**: di un HEIC
             * i pixel si leggono e non si riscrivono, quindi là esce per forza un JPEG
             * accanto. Adesso lo decide il modello guardando il formato, e l'avviso finale lo
             * dice; prima lo si spiegava dentro il dialogo. Vedi `ImageEdit.canOverwrite`.
             *
             * ⚠️⚠️ **DALLA 1.17 SALVA IL TOTALE, non l'ultimo ritocco**: con 'Applica' i passi
             * possono essere parecchi, e quello che si scrive sul file è la loro composizione.
             * Chi passasse `turns` e `crop` da soli butterebbe via tutto quello che è stato
             * confermato prima, cioè quasi tutto il lavoro.
             */
            TextButton(
                onClick = { onSave(total.turns, total.crop) },
                enabled = shown != null && !busy && !(total.turns == 0 && total.crop.whole)
            ) {
                Text(stringResource(R.string.editor_save))
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
                    loupe = with(density) { LOUPE_SIDE.toPx() },
                    edge = with(density) { LOUPE_EDGE.toPx() },
                    onCrop = { crop = it },
                    keep = shape.value(lay)
                )
            }
        }

        EditorSheet(
            shape = shape,
            lay = lay,
            busy = busy,
            ready = shown != null,
            leftHand = leftHand,
            pending = pending,
            applied = steps.isNotEmpty(),
            undone = undone.isNotEmpty(),
            onShape = { one ->
                /*
                 * ⚠️⚠️ **LA SELEZIONE TIENE IL POSTO ANCHE AL CAMBIO DI PROPORZIONE, dalla
                 * 1.17** (richiesta dell'utente: *se la selezione mantiene la sua posizione al
                 * cambio di orientamento, deve mantenerlo anche al cambio di proporzione*).
                 * Prima ogni tocco su una proporzione rimetteva il rettangolo grande al
                 * massimo e in mezzo, cioè buttava via la mira appena presa.
                 * ⚠️ **Ritoccare la proporzione GIÀ scelta invece lo rimette intero**, ed è la
                 * via di fuga che c'era prima e che qui si conserva apposta: senza, una
                 * selezione ridotta per sbaglio non avrebbe più un modo rapido di tornare
                 * grande, e 'Ripristina' azzererebbe anche i passi confermati.
                 */
                crop = if (one == shape) one.fit(aspect, lay) else reshaped(crop, aspect, one.value(lay))
                shape = one
            },
            onLay = { one ->
                if (one != lay) {
                    crop = flipped(crop, aspect)
                    lay = one
                }
            },
            onTurn = { way ->
                turns = (turns + way).mod(4)
                // ⚠️ La proporzione si rifà sull'aspetto NUOVO, che è il reciproco di quello
                // di adesso: dopo un quarto di giro i due lati si scambiano, e il conto fatto
                // con l'aspetto vecchio darebbe un rettangolo storto per un fotogramma.
                crop = shape.fit(1f / aspect, lay)
            },
            onCentreAcross = { crop = centredAcross(crop) },
            onCentreDown = { crop = centredDown(crop) },
            onApply = {
                val picture = base ?: return@EditorSheet
                steps = steps + applied(picture, steps.lastOrNull()?.done ?: Done.NOTHING, turns, crop)
                undone = emptyList()
            },
            onUndo = {
                // ⚠️ **Un passo indietro solo, e il passo è quello che si vede**: se c'è un
                // ritocco non confermato è lui il passo, altrimenti è l'ultimo 'Applica'. È la
                // regola di ogni annullamento, e l'alternativa (disfare sempre un 'Applica'
                // lasciando in piedi il ritocco in corso) farebbe sparire un pezzo di immagine
                // mentre il rettangolo resta dov'è.
                if (pending) {
                    turns = 0
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
}

/**
 * Un passo confermato con 'Applica'.
 *
 * ⚠️ **[done] è composto dall'originale e non dal passo prima**: vedi la nota su `steps` in
 * [EditorScreen]. [preview] è l'immagine che ne esce, tenuta per poterla ritrovare disfacendo.
 * ⚠️ **I bitmap dei passi disfatti NON si riciclano**, ed è voluto: sono grandi quanto
 * un'anteprima e sempre più piccoli, mentre uno di loro può essere ancora dentro un
 * [ImageBitmap] che Compose sta disegnando. Riciclare quello manderebbe in errore il disegno,
 * e lasciarli al raccoglitore costa qualche decina di millisecondi di memoria in più.
 */
private class Step(val done: Done, val preview: Bitmap)

/** Una rotazione e un rettangolo: quello che si sa applicare al file vero. */
private data class Done(val turns: Int, val crop: ImageEdit.Crop) {
    companion object {
        val NOTHING = Done(0, ImageEdit.Crop.WHOLE)
    }
}

/**
 * Quello che si ottiene facendo [turns] e [crop] **dopo** [done].
 *
 * ⚠️⚠️ **LA COMPOSIZIONE È ESATTA, non un'approssimazione, e vale la pena sapere perché**: la
 * catena è sempre 'gira, poi ritaglia' (lo è in `ImageEdit.redraw`), e girare un ritaglio è la
 * stessa cosa che ritagliare l'immagine girata, col rettangolo girato dentro il quadrato
 * unitario ([turnedRect]). Portata fuori la rotazione, restano due ritagli uno dentro l'altro,
 * e due ritagli si compongono in uno ([insideOf]). Quindi n passi qualunque diventano una
 * rotazione e un rettangolo, sempre, senza perdere niente.
 */
private fun after(done: Done, turns: Int, crop: ImageEdit.Crop): Done = Done(
    (done.turns + turns).mod(4),
    insideOf(turnedRect(done.crop, turns), crop)
)

/** Il passo nuovo: la composizione, e l'anteprima che ne esce. */
private fun applied(base: Bitmap, done: Done, turns: Int, crop: ImageEdit.Crop): Step {
    val spun = turnedBitmap(base, turns)
    val cut = cutBitmap(spun, crop)
    if (spun !== base && spun !== cut) spun.recycle()
    return Step(after(done, turns, crop), cut)
}

/**
 * Lo stesso rettangolo dopo [turns] quarti di giro **in senso orario**, in frazioni.
 *
 * ⚠️ Girando di un quarto in senso orario il punto `(x, y)` va in `(1 - y, x)`, quindi il lato
 * sinistro nuovo viene dal fondo vecchio. Chi la ritocca la riderivi da lì e non a occhio: il
 * segno sbagliato dà un ritaglio speculare, che su una fotografia simmetrica non si vede.
 */
private fun turnedRect(crop: ImageEdit.Crop, turns: Int): ImageEdit.Crop {
    var out = crop
    repeat(turns.mod(4)) {
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

/** L'anteprima girata di [turns] quarti, o quella di prima se non si gira. */
private fun turnedBitmap(source: Bitmap, turns: Int): Bitmap =
    if (turns.mod(4) == 0) source
    else runCatching {
        Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            Matrix().apply { postRotate(90f * turns) },
            true
        )
    }.getOrDefault(source)

/**
 * L'anteprima ritagliata.
 *
 * ⚠️ **Gli arrotondamenti sono gli STESSI di `ImageEdit.redraw`**, e non per caso: se qui si
 * troncasse e là si arrotondasse, l'anteprima e il file salvato mostrerebbero due ritagli
 * diversi di un pixel, e la differenza si vedrebbe solo sul risultato finale.
 */
private fun cutBitmap(source: Bitmap, crop: ImageEdit.Crop): Bitmap {
    if (crop.whole) return source
    return runCatching {
        val x = (crop.left * source.width).toInt().coerceIn(0, source.width - 1)
        val y = (crop.top * source.height).toInt().coerceIn(0, source.height - 1)
        val w = ((crop.right - crop.left) * source.width).toInt().coerceIn(1, source.width - x)
        val h = ((crop.bottom - crop.top) * source.height).toInt().coerceIn(1, source.height - y)
        Bitmap.createBitmap(source, x, y, w, h)
    }.getOrDefault(source)
}

/** La selezione portata a metà larghezza, senza cambiare misura. */
private fun centredAcross(crop: ImageEdit.Crop): ImageEdit.Crop {
    val w = crop.right - crop.left
    return ImageEdit.Crop((1f - w) / 2f, crop.top, (1f + w) / 2f, crop.bottom)
}

/** La selezione portata a metà altezza, senza cambiare misura. */
private fun centredDown(crop: ImageEdit.Crop): ImageEdit.Crop {
    val h = crop.bottom - crop.top
    return ImageEdit.Crop(crop.left, (1f - h) / 2f, crop.right, (1f + h) / 2f)
}

/**
 * La stessa selezione con una proporzione nuova: stesso centro, stessa **area**.
 *
 * ⚠️⚠️ **SI CONSERVA L'AREA E NON UN LATO, ed è quello che rende il cambio prevedibile**:
 * tenendo la larghezza, passare da 16:9 a 9:16 farebbe un rettangolo altissimo che esce
 * dall'immagine; tenendo l'altezza, il contrario. L'area è l'unica misura che non privilegia
 * un verso, e a occhio si legge come 'la stessa selezione, di un'altra forma'.
 * ⚠️ **Le frazioni non sono la proporzione**: i due lati dell'immagine sono diversi, quindi
 * il rapporto fra le frazioni è quello dei pixel diviso [frame]. È la stessa correzione che
 * fa [flipped], e saltarla dà forme sbagliate su ogni immagine non quadrata.
 * ⚠️ **Se non ci sta si rimpicciolisce, e solo dopo si sposta**: la forma è quello che si è
 * chiesto, la posizione è quello che si può cedere. Come in [flipped].
 */
private fun reshaped(crop: ImageEdit.Crop, frame: Float, want: Float?): ImageEdit.Crop {
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

/**
 * I comandi dell'editor, come pannello appoggiato in fondo alla schermata.
 *
 * ⚠️⚠️ **È UNA BOTTOMSHEET PERCHÉ I TASTI SONO DIVENTATI SETTE** (richiesta dell'utente,
 * 2026-09-01: *visto che il numero di tasti totali è salito, per coerenza con un'altra parte
 * importante di UI, mettiamo il tutto in una bella bottomsheet ordinata*). Prima erano tre
 * file sciolte appese al fondo della colonna, che con sette tasti sarebbero diventate quattro
 * e avrebbero mangiato l'immagine senza nemmeno sembrare un gruppo.
 * ⚠️⚠️ **E NON è una `ModalBottomSheet`, per la stessa ragione della selezione** (vedi
 * [PickSheet]): quella mette un velo davanti a tutto e si prende i tocchi, e qui sotto c'è il
 * rettangolo che si sta trascinando col dito. Un pannello che copre la cosa su cui agisce non
 * è un pannello, è una porta chiusa.
 * ⚠️ **Le due file di tasti sono DUE `ActionPad` e non uno da sette**: uno solo le
 * spezzerebbe a quattro più tre lasciando le celle dell'ultima fila più larghe delle altre, e
 * soprattutto direbbe che sono sette cose dello stesso genere. Sopra si **trasforma**, sotto
 * si **conferma o si torna indietro**: il filetto in mezzo è quella differenza.
 */
@Composable
private fun EditorSheet(
    shape: Shape,
    lay: Lay,
    busy: Boolean,
    /** Se l'anteprima è arrivata: prima non c'è niente su cui agire. */
    ready: Boolean,
    /** Se le due file di tasti vanno nell'ordine della mano sinistra. */
    leftHand: Boolean,
    /** Se c'è un ritocco non ancora confermato. */
    pending: Boolean,
    /** Se c'è almeno un 'Applica' alle spalle. */
    applied: Boolean,
    /** Se c'è almeno un passo disfatto che 'Ripristina' può rimettere. */
    undone: Boolean,
    onShape: (Shape) -> Unit,
    onLay: (Lay) -> Unit,
    /** Un quarto di giro: `1` in senso orario, `3` antiorario. */
    onTurn: (Int) -> Unit,
    onCentreAcross: () -> Unit,
    onCentreDown: () -> Unit,
    onApply: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOriginal: () -> Unit
) {
    val live = ready && !busy
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = SHEET_ROUND, topEnd = SHEET_ROUND),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        /*
         * ⚠️⚠️ **NIENTE OMBRA DALLA 1.40, come le altre due schede appoggiate in basso**
         * (richiesta dell'utente, 2026-09-03: *evita le ombreggiature in basso*). Su una
         * superficie ancorata al bordo l'ombra si vede solo di sotto, dove va a finire
         * sull'angolo stondato del vetro: il perché per esteso sta su `PickSheet`, dov'è
         * misurato.
         * ⚠️ **Questa scheda però si ferma ancora SOPRA la barra di sistema**, a differenza
         * delle altre due: vive dentro la colonna che porta il rientro, e portarla al bordo
         * vorrebbe dire rifare l'impaginazione di questa schermata. Chi ci mette mano lo
         * faccia insieme, non a metà.
         */
        tonalElevation = SHEET_RISE
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ⚠️ La maniglia è un segno e non un comando, come nella bottomsheet della
            // selezione: dice 'questo è un pannello', e non si trascina.
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(width = SHEET_GRIP_WIDE, height = SHEET_GRIP_TALL)
                    .clip(RoundedCornerShape(SHEET_GRIP_TALL))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            /*
             * ⚠️ Le proporzioni sono CINQUE e non otto, perché le stesse quattro forme lette
             * nell'altro verso sono le altre quattro: '2:3' e '3:2' non sono due scelte, sono
             * la stessa scelta con l'orientamento girato. Vedi [Shape].
             *
             * ⚠️⚠️ **PRENDONO TUTTA LA LARGHEZZA, ed è una richiesta** (utente, 2026-09-01:
             * *fa' in modo che le 5 proporzioni occupino tutto lo spazio orizzontale della
             * bottomsheet: è più elegante e ordinato*). Prima la fila scorreva di lato e
             * finiva dove finivano le parole, lasciando un vuoto a destra.
             * ⚠️⚠️ **MA NON a celle uguali, e la ragione è una misura**: su uno schermo da
             * 360dp, tolti i due margini da 24 e i quattro distacchi da 8, a ogni quinto
             * restano una sessantina di dp, e un chip di Material se ne mangia 32 di rientri.
             * Nei 28 che avanzano non ci sta nemmeno '9:16', figurarsi 'Свободно', che è il
             * 'Libero' russo. Celle uguali vorrebbe dire etichette tagliate in mezza Europa.
             * ⚠️ **Perciò [FlowRow] con [Arrangement.SpaceBetween]**: la fila arriva ai due
             * bordi, i distacchi sono tutti uguali, ogni chip resta largo quanto la sua
             * parola, e nella lingua in cui non ci stanno **va a capo** invece di uscire dallo
             * schermo. Una `Row` semplice, senza scorrimento, là sborderebbe in silenzio.
             */
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = STAGE_SIDE),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                /*
                 * ⚠️⚠️ **'Libero' È PIÙ LARGO DEGLI ALTRI QUATTRO, e le celle uguali sono
                 * durate una versione** (correzione dell'utente, 2026-09-01, voce `ed-chip`
                 * del collaudo: *la richiesta non era che fossero tutte uguali, ma che
                 * occupassero tutto lo spazio*). Le quattro proporzioni portano numeri che
                 * **non si traducono**, quindi la loro larghezza è nota e non cresce mai;
                 * 'Libero' è l'unica parola vera della fila, ed è quella che in tedesco, in
                 * russo o in tamil può aver bisogno di posto. Dare a tutti la stessa cella
                 * vuol dire tararla sul caso peggiore di uno solo, e sprecarla per quattro.
                 */
                for (one in Shape.entries) {
                    SheetChip(
                        text = one.text(lay) ?: stringResource(R.string.editor_free),
                        selected = one == shape,
                        enabled = live,
                        onClick = { onShape(one) },
                        modifier = Modifier.weight(if (one == Shape.FREE) FREE_ROOM else 1f)
                    )
                }
            }

            /*
             * ⚠️⚠️ **DUE TASTI CHE SI ESCLUDONO, e cambiarli RIBALTA la selezione sul posto**
             * (richiesta dell'utente, 2026-08-31): da 16:9 si passa a 9:16, e una selezione
             * libera si inverte allo stesso modo. ⚠️ **Il centro non si muove**, ed è la parte
             * che rende il gesto utile invece che spaesante: si sta scegliendo *che forma*
             * dare al ritaglio, non *dove* metterlo.
             */
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = STAGE_SIDE, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (one in Lay.entries) {
                    SheetChip(
                        text = stringResource(one.label),
                        selected = one == lay,
                        enabled = live,
                        onClick = { onLay(one) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            /*
             * ⚠️ **Le frecce circolari e nient'altro** (richiesta dell'utente, 2026-08-31: le
             * icone di rotazione devono essere le due frecce *senza forme geometriche*). Le
             * `Rotate90Degrees*` di Material portano un quadrato che qui non vuol dire niente:
             * non si gira un quadrato, si gira la fotografia.
             * ⚠️⚠️ **NON si passa alle versioni `AutoMirrored`, e la deprecazione si zittisce
             * apposta**: quelle si specchiano nelle lingue che si leggono da destra, e una
             * freccia antioraria specchiata **diventa oraria**. Il verso di un giro è fisico,
             * non dipende da come si legge: il tasto direbbe il falso in arabo, in persiano e
             * in urdu, cioè in tre delle ventotto lingue dell'app. ⚠️ L'annullamento qui sotto
             * invece È `AutoMirrored`, e va bene: là 'indietro' segue davvero la lettura.
             */
            @Suppress("DEPRECATION") val ccw = Icons.Default.RotateLeft
            @Suppress("DEPRECATION") val cw = Icons.Default.RotateRight

            val turnLeft = PadAction(ccw, R.string.editor_left, enabled = live) { onTurn(3) }
            val turnRight = PadAction(cw, R.string.editor_right, enabled = live) { onTurn(1) }
            /*
              * ⚠️ **Le due icone sono DISEGNATE DALL'UTENTE** (2026-09-01, voce `ed-sheet`
              * del collaudo: *usa le mie icone che ti ho già passato*): quelle di Material
              * non gli dicevano abbastanza. Vedi [Glyphs.AlignAcross].
              */
            val acrossKey = PadAction(
                Glyphs.AlignAcross, R.string.editor_center_across,
                enabled = live
            ) { onCentreAcross() }
            val downKey = PadAction(
                Glyphs.AlignDown, R.string.editor_center_down,
                enabled = live
            ) { onCentreDown() }

            val applyKey = PadAction(
                Icons.Outlined.Check, R.string.editor_apply, enabled = live && pending
            ) { onApply() }
            val undoKey = PadAction(
                Icons.AutoMirrored.Outlined.Undo, R.string.editor_undo,
                enabled = live && (pending || applied)
            ) { onUndo() }
            val redoKey = PadAction(
                Icons.AutoMirrored.Outlined.Redo, R.string.editor_redo,
                // ⚠️⚠️ **SPENTO FINCHÉ C'È UN RITOCCO IN SOSPESO, e non è pignoleria**: il
                // rettangolo in corso è in frazioni dell'immagine di **adesso**, e rimettere
                // un passo sotto di lui gli farebbe selezionare un'altra cosa senza che
                // nessuno l'abbia mosso. 'Annulla' toglie il ritocco e lo riaccende.
                enabled = live && undone && !pending
            ) { onRedo() }
            val originalKey = PadAction(
                Icons.Outlined.RestartAlt, R.string.editor_original,
                enabled = live && (pending || applied || undone)
            ) { onOriginal() }

            /*
             * ⚠️⚠️ **LE DUE FILE SONO SCRITTE PER ESTESO NEI DUE VERSI, e NON si ricavano
             * rovesciando una lista** (ordine dettato dall'utente, 2026-09-01). Rovesciarla
             * darebbe l'ordine sbagliato in due punti su otto, ed è il genere di errore che
             * si vede solo provando: 'Ruota a sinistra' e 'Ruota a destra' restano in
             * quest'ordine anche per la mano sinistra, perché il loro verso è quello delle
             * frecce e non quello della lettura, e lo stesso vale per 'Annulla' e
             * 'Ripristina', che sono le due direzioni della stessa cronologia. A scambiarsi
             * sono i **gruppi**, e le due coppie interne di centratura e di conferma.
             * ⚠️ **Il criterio, che è quello che regge la scelta**: il tasto che si usa di
             * più finisce sotto il pollice, cioè al bordo della mano che tiene il telefono.
             */
            ActionPad(
                columns = SHEET_KEYS,
                stretch = true,
                actions =
                    if (leftHand) listOf(turnLeft, turnRight, acrossKey, downKey)
                    else listOf(downKey, acrossKey, turnLeft, turnRight)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = STAGE_SIDE))

            ActionPad(
                columns = SHEET_KEYS,
                stretch = true,
                actions =
                    if (leftHand) listOf(applyKey, undoKey, redoKey, originalKey)
                    else listOf(originalKey, undoKey, redoKey, applyKey)
            )
        }
    }
}

/**
 * Un chip del pannello, scritto in casa.
 *
 * ⚠️⚠️ **NON È IL `FilterChip` DI MATERIAL, e la ragione è una misura**: l'utente vuole i
 * cinque chip delle proporzioni **larghi uguali e da bordo a bordo** (2026-09-01, con lo
 * schizzo). Quello di Material tiene 16dp di rientro **fissi** per lato, che non si possono
 * cambiare perché non espone nessun `contentPadding`: su uno schermo da 360dp, a un quinto
 * della larghezza, dei 60dp di cella ne resterebbero 28 per la parola, e là non ci sta
 * nemmeno '9:16'. Con 6dp di rientro ne restano quasi 50, e ci sta anche il 'Libero' russo.
 * ⚠️ **La resa è la sua, non un'altra cosa**: stessa altezza di 32dp, stesso smusso, stesso
 * `secondaryContainer` da scelto e stesso filetto da non scelto. Quello che cambia è solo il
 * rientro, che è la cosa per cui è stato riscritto.
 * ⚠️ **L'ellissi resta come rete**: in una lingua che dovesse sforare comunque, la parola si
 * accorcia invece di sbordare fuori dal chip.
 */
@Composable
private fun SheetChip(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(CHIP_TALL),
        shape = RoundedCornerShape(CHIP_ROUND),
        color = if (selected) scheme.secondaryContainer else Color.Transparent,
        contentColor = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
        border = if (selected) null else BorderStroke(CHIP_EDGE, scheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = CHIP_PAD),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Quanto è più larga la cella di 'Libero' rispetto a una delle quattro proporzioni.
 *
 * ⚠️ Con 1,6 la fila resta piena da bordo a bordo e la parola prende quasi il 29% invece
 * del 20: su uno schermo da 360dp sono una settantina di dp netti, che bastano al 'Libero'
 * di tutte e ventotto le lingue. Vedi la nota sulla fila dei chip.
 */
private const val FREE_ROOM = 1.6f

/** Le misure del chip di casa: quelle di Material, tranne il rientro. */
private val CHIP_TALL = 32.dp
private val CHIP_ROUND = 8.dp
private val CHIP_EDGE = 1.dp
private val CHIP_PAD = 6.dp

/**
 * Quante colonne hanno le due file di tasti del pannello: **quattro tutte e due**.
 *
 * ⚠️ Il numero è uno solo apposta: due file con un numero diverso di celle stanno su due
 * griglie diverse, e l'ultima icona di sotto finirebbe spostata rispetto a quella di sopra.
 */
private const val SHEET_KEYS = 4

/** Lo smusso dei due angoli alti, e quanto il pannello si stacca: come la bottomsheet della
 * selezione, perché è la stessa cosa in un'altra schermata. */
private val SHEET_ROUND = 28.dp
private val SHEET_RISE = 6.dp
private val SHEET_GRIP_WIDE = 32.dp
private val SHEET_GRIP_TALL = 4.dp

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
    /** Il diametro della lente e quanto sta lontana dai bordi. Vedi [lens]. */
    loupe: Float,
    edge: Float,
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

            // ── La lente ──
            eyeOf(held, r)?.let { eye ->
                lens(eye, picture, frame, r, loupe, edge, thick, line)
            }
        }
    }
}

/**
 * L'angolo che la lente deve guardare, e `null` quando non c'è niente da guardare.
 *
 * ⚠️⚠️ **SOLO GLI ANGOLI, e lo spostamento del rettangolo intero NON ne ha uno**: muovendolo
 * da dentro i punti da mirare sono **quattro**, e sceglierne uno vorrebbe dire ingrandire un
 * angolo qualunque mentre si guarda l'inquadratura tutta insieme. La domanda dell'utente era
 * *su quale pixel sto rilasciando il rettangolo di selezione*, e quel pixel esiste solo
 * mentre si tira un angolo.
 */
private fun eyeOf(held: Grab, r: Rect): Offset? = when (held) {
    Grab.TOP_LEFT -> r.topLeft
    Grab.TOP_RIGHT -> r.topRight
    Grab.BOTTOM_LEFT -> r.bottomLeft
    Grab.BOTTOM_RIGHT -> r.bottomRight
    Grab.INSIDE, Grab.NONE -> null
}

/**
 * La lente: un cerchio in alto con dentro [eye] ingrandito [LOUPE_ZOOM] volte.
 *
 * ⚠️⚠️ **STA IN ALTO E DALLA PARTE OPPOSTA AL DITO, non attaccata al dito**: una lente che
 * segue il dito è quella che tutti conoscono dalla selezione del testo, ma là sopra il dito c'è
 * sempre spazio perché la riga di testo sta in mezzo alla pagina; qui il rettangolo arriva fino
 * al bordo dell'immagine, e una lente attaccata all'angolo alto uscirebbe dal riquadro proprio
 * nel caso in cui serve di più. Fissata all'angolo alto opposto non esce mai e non finisce mai
 * sotto la mano: il dito che tira l'angolo sinistro la trova a destra e viceversa, e tirando un
 * angolo basso il posto in alto è libero comunque.
 * ⚠️ **La scelta guarda la METÀ dello schermo in cui sta il dito**, non quale dei quattro
 * angoli è: è la stessa regola per gli angoli alti e per quelli bassi, e un rettangolo tirato
 * tutto a sinistra manda la lente a destra qualunque angolo si tenga.
 * ⚠️ **E si ridecide a ogni movimento, quindi la lente SALTA da una parte all'altra quando
 * l'angolo attraversa la metà dello schermo**: è voluto, e l'alternativa è peggio. Decidendo
 * il lato una volta sola alla presa, un angolo trascinato da un capo all'altro si porterebbe
 * la lente sotto il dito proprio alla fine della corsa, cioè la spegnerebbe nel momento in cui
 * si mira. Il salto invece capita in mezzo a uno spostamento lungo, dove non si sta mirando
 * niente.
 *
 * ⚠️⚠️ **DENTRO CI VA ANCHE IL BORDO DEL RITAGLIO, e senza quello la lente non servirebbe a
 * niente**: ingrandire i soli pixel direbbe *che cosa* c'è sotto il dito ma non *dove* passa il
 * taglio, che è la domanda. Le due righe bianche che si incrociano al centro della lente sono
 * il bordo vero, disegnato nello stesso spazio ingrandito e con lo spessore diviso per
 * l'ingrandimento, così a schermo resta il filo sottile di sempre invece di diventare una fascia
 * larga quanto quello che dovrebbe mostrare.
 *
 * ⚠️⚠️ **SENZA INTERPOLAZIONE ([FilterQuality.None]), ed è la differenza fra una lente e un
 * ingrandimento sfocato**: col filtro predefinito i pixel ingranditi sfumano l'uno nell'altro e
 * il bordo del taglio torna a essere indeciso, cioè si ripaga il difetto che la lente doveva
 * togliere. Così invece i pixel diventano quadretti netti e il bordo cade visibilmente fra due.
 * ⚠️ **Ma i pixel sono quelli dell'ANTEPRIMA, non quelli del file**: qui si lavora su una copia
 * campionata a [PREVIEW] sul lato lungo (vedi `preview`), quindi il quadretto che si vede può
 * valere più di un pixel dell'originale. Non è un difetto di questa lente ed è la ragione per
 * cui vale dirlo: il ritaglio si conserva in **frazioni** e si applica al file intero alla
 * massima risoluzione, quindi la mira è più fine del quadretto che la mostra.
 *
 * ⚠️ **Il fondo nero sotto tutto**: tirando un angolo a filo dell'immagine metà lente cade
 * fuori dalla fotografia, e senza un fondo pieno là si vedrebbe per trasparenza quello che sta
 * dietro, cioè la fotografia **non** ingrandita. Due scale della stessa immagine dentro lo
 * stesso cerchio si leggono come un difetto di disegno.
 *
 * ⚠️ **L'anello è spesso quanto le squadrette d'angolo ([thick]) e non quanto il bordo del
 * ritaglio ([EDGE_PX])**, che è un filo di due pixel **fisici**: su uno schermo a tre volte
 * quel filo vale due terzi di punto, giusto per un bordo che deve coprire il meno possibile e
 * invisibile per un attrezzo che galleggia sopra la fotografia. Con la misura delle squadrette
 * gli arnesi del ritaglio hanno tutti lo stesso peso, che è anche il modo di dire che sono la
 * stessa cosa.
 */
private fun DrawScope.lens(
    eye: Offset,
    picture: ImageBitmap,
    frame: Rect,
    r: Rect,
    side: Float,
    edge: Float,
    thick: Float,
    line: Color
) {
    val radius = side / 2f
    val centre = Offset(
        x = if (eye.x < size.width / 2f) size.width - edge - radius else edge + radius,
        y = edge + radius
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
            translate(left = centre.x - eye.x * LOUPE_ZOOM, top = centre.y - eye.y * LOUPE_ZOOM)
            scale(scaleX = LOUPE_ZOOM, scaleY = LOUPE_ZOOM, pivot = Offset.Zero)
        }) {
            drawImage(
                image = picture,
                dstOffset = IntOffset(frame.left.roundToInt(), frame.top.roundToInt()),
                dstSize = IntSize(frame.width.roundToInt(), frame.height.roundToInt()),
                filterQuality = FilterQuality.None
            )
            drawRect(
                color = line.copy(alpha = 0.9f),
                topLeft = r.topLeft,
                size = r.size,
                style = Stroke(width = EDGE_PX / LOUPE_ZOOM)
            )
        }
    }
    drawCircle(color = line, radius = radius, center = centre, style = Stroke(width = thick))
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
 * L'anteprima su cui si lavora.
 *
 * ⚠️ **Campionata a [PREVIEW] sul lato lungo**: il ritaglio si sceglie a occhio su uno
 * schermo da mille punti, e tenere in memoria l'originale intero per tutto il tempo in cui la
 * schermata è aperta vorrebbe dire duecento megabyte fermi mentre si decide.
 *
 * ⚠️⚠️ **IL `?:` IN FONDO TOGLIE UN'ATTESA INFINITA, e c'era dalla `1.26`**: su un AVIF (e
 * ora su un SVG) `ImageDecoder` fallisce, questa funzione tornava `null`, e la schermata
 * restava a girare senza mai dire niente, perché il suo stato di partenza è 'sto ancora
 * leggendo'. Il perché la catena dei ripieghi non arrivasse fin qui sta su
 * [ImageSource.rescue].
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
}.getOrNull() ?: ImageSource.rescue(context, uri, PREVIEW)

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

/**
 * L'ingrandimento della lente, il suo diametro e quanto sta lontana dai bordi del riquadro.
 *
 * ⚠️ **Quattro volte, e il conto è quello del dito**: un polpastrello copre una quarantina di
 * punti, e a quattro ingrandimenti in una lente da 112 se ne vedono ventotto, cioè poco meno di
 * quello che il dito nasconde. Ingrandendo di più si vedrebbe un francobollo di fotografia e si
 * perderebbe il contesto che serve a capire dove si è; ingrandendo di meno la lente mostrerebbe
 * quasi le stesse dimensioni dello schermo, cioè non servirebbe.
 * ⚠️ Lo spessore dell'anello **non** è qui: è quello delle squadrette d'angolo, e il perché sta
 * in [lens].
 */
private const val LOUPE_ZOOM = 4f
private val LOUPE_SIDE = 112.dp
private val LOUPE_EDGE = 8.dp

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

/**
 * Il respiro intorno alla fotografia nell'editor, e quanto sta lontana dai bordi laterali.
 *
 * ⚠️⚠️ **I DUE NUMERI SONO DIVERSI, dalla 1.08, e la ragione è FISICA e non estetica**
 * (richiesta dell'utente: *spostare la selezione fino al bordo estremo è difficile se per
 * qualche motivo, es. una cover con bordo sporgente, non si riesce ad arrivare col dito
 * esattamente sul bordo*). Il rettangolo del ritaglio non può uscire dalla fotografia, quindi
 * per portarlo a filo il dito deve **raggiungere** il bordo dell'immagine: se quel bordo sta a
 * dodici punti dal vetro, con una cover sporgente il dito non ci arriva e l'ultima striscia di
 * fotografia diventa impossibile da tenere.
 * ⚠️ **Solo di lato, e non sopra e sotto**: là il riquadro confina con la testata e con la
 * fila delle rotazioni, non col bordo dello schermo, quindi il problema non esiste e lo spazio
 * verticale è quello scarso (il riquadro se lo divide con quattro file di comandi).
 * ⚠️ **Il prezzo è dichiarato**: l'anteprima si stringe di ventiquattro punti in tutto, cioè
 * meno di un decimo di uno schermo da telefono, e in cambio l'ultimo pixel dell'immagine è
 * raggiungibile.
 */
private val STAGE_PAD = 12.dp
private val STAGE_SIDE = 24.dp

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
    /*
     * ⚠️⚠️ **DUE FAMIGLIE SEPARATE E NON UN ELENCO SOLO, dalla 1.11**: le prime si dichiarano
     * editor ad Android e riscrivono la fotografia dov'è; le seconde ricevono una copia per
     * condivisione e salvano dove decidono loro. Mescolarle vorrebbe dire promettere la stessa
     * cosa a due comportamenti diversi, e chi sceglie se ne accorgerebbe solo dopo aver perso
     * una modifica cercandola nella cartella sbagliata.
     * ⚠️ La seconda famiglia esce da un'euristica dichiarata (vedi `Editors`), quindi può
     * portare dentro qualcosa che non c'entra: separarla è anche il modo di dirlo senza una
     * frase in più.
     */
    val (edit, send) = remember(others) { others.partition { !it.shared } }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(),
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
            // **stessa** icona della voce di menu, che è il modo per dire che è la stessa
            // cosa. Dalla 1.29 è quella disegnata dall'utente, e cambiarla qui non era
            // opzionale: due disegni diversi per lo stesso editor lo farebbero sembrare due.
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

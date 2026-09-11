package io.github.roccobot.aiv

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.Shader.TileMode
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * L'editor **completo**: quello che cambia i pixel.
 *
 * ⚠️⚠️ **NON SOSTITUISCE L'EDITOR DI CASA, ED È LA SUA SCELTA** (*due editor separati*): uno
 * mette un'immagine in posa e la ritaglia **senza toccare un pixel**, l'altro la sviluppa. Sono
 * due mestieri, e un editor solo che li facesse tutti e due dovrebbe ricomprimere anche quando
 * gira una fotografia, cioè perdere qualità per un gesto che oggi non ne fa perdere. Chi apre
 * 'Modifica' sceglie fra i due la prima volta, e la scelta si ricorda.
 *
 * ⚠️⚠️ **LA PILA È DI VALORI E NON DI GESTI, e un passo nasce quando il dito LASCIA il cursore**:
 * dentro un trascinamento un cursore passa per cento valori, e una pila che li prendesse tutti
 * renderebbe 'Annulla' inutilizzabile. Quello che si disfa è un **gesto compiuto**, che è la cosa
 * che l'utente ricorda di aver fatto.
 *
 * ⚠️⚠️ **IL CONFRONTO COL PRIMA È UN TOCCO LUNGO SULL'IMMAGINE**: finché il dito resta giù si
 * vede l'originale, e al rilascio torna il lavoro. È il gesto di ogni editor fotografico, e vale
 * la pena scriverlo perché l'alternativa (un tasto che alterna) lascia in dubbio su quale delle
 * due si stia guardando.
 */
@Composable
fun AdvancedEditorScreen(
    uri: Uri,
    /** Se una scrittura è in corso: i comandi si spengono, o si salverebbe due volte. */
    busy: Boolean,
    /** Che cosa applicare al file vero. Il lavoro lo fa chi chiama, come per l'editor di casa. */
    onSave: (Look) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    /*
     * ⚠️ **La rotazione è inibita qui come nell'editor di casa, e per la sua stessa ragione**
     * (giro della `1.67`, domanda `d-rotazione`: *usare editor di immagini in orizzontale è
     * impensabile*). Qui vale di più: i cursori vivono in una colonna in fondo, e coricati
     * prenderebbero metà schermo.
     */
    val activity = remember(context) { Knobs.activityOf(context) }
    DisposableEffect(activity) {
        val prima = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            if (activity != null && prima != null) activity.requestedOrientation = prima
        }
    }

    var origin by remember(uri) { mutableStateOf<Bitmap?>(null) }

    /**
     * La storia dei valori, dal primo all'ultimo, e dove si è dentro di lei.
     *
     * ⚠️⚠️ **UNA LISTA E UN INDICE, E NON DUE PILE**: con due pile ('fatti' e 'disfatti')
     * ogni passo nuovo deve ricordarsi di svuotare la seconda, e chi se ne dimentica lascia un
     * 'Ripristina' che riporta a una strada abbandonata. Con l'indice quel caso non esiste: un
     * passo nuovo **taglia** tutto quello che veniva dopo, in una riga sola.
     * ⚠️ **Il primo elemento è sempre il niente**, quindi 'Annulla' fino in fondo torna
     * all'originale per costruzione, e 'Originale' non è altro che portare l'indice a zero.
     */
    var history by remember(uri) { mutableStateOf(listOf(Look.NONE)) }
    var at by remember(uri) { mutableIntStateOf(0) }

    /** Quello che si vede adesso, compreso il movimento di cursore ancora in corso. */
    var look by remember(uri) { mutableStateOf(Look.NONE) }

    /** Se il dito è premuto sull'immagine: si guarda l'originale. */
    var comparing by remember(uri) { mutableStateOf(false) }

    BackHandler { onBack() }

    LaunchedEffect(uri) {
        origin = withContext(Dispatchers.IO) { preview(context, uri) }
    }

    /**
     * Un passo compiuto entra nella storia, e taglia quello che veniva dopo.
     *
     * ⚠️⚠️ **QUELLO CHE ENTRA È [look], CIOÈ QUELLO CHE SI VEDE, E NON UN VALORE CHE ARRIVA
     * DAL CURSORE**: un cursore non sa che cosa fanno gli altri quattro, quindi un passo
     * costruito dal suo solo valore perderebbe tutto il resto. ⚠️ **E [look] si legge qui e
     * non si cattura**: è una proprietà delegata a uno stato, quindi la lettura è sempre
     * quella del momento in cui questa funzione gira. Il perché non sia un dettaglio vive
     * sul parametro `onSettled` di [LookKnob], ed è un difetto che il banco ha preso alla
     * prima corsa.
     */
    fun push() {
        if (look == history[at]) return
        history = history.take(at + 1) + look
        at = history.size - 1
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                    )
                )
                // ⚠️ I due lati non hanno lo stesso rientro, e la ragione è quella scritta in
                // `EditorScreen`: da una parte c'è un'icona, dall'altra una parola.
                .padding(start = 4.dp, end = STAGE_SIDE - TEXT_BUTTON_PAD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back))
            }
            Text(
                text = stringResource(R.string.editor_full),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f).heading()
            )
            TextButton(
                onClick = { onSave(look) },
                enabled = origin != null && !busy && !look.idle
            ) {
                Text(stringResource(R.string.editor_save))
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
                    look = if (comparing) Look.NONE else look,
                    onCompare = { comparing = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        LookSheet(
            look = look,
            busy = busy,
            ready = origin != null,
            canUndo = at > 0,
            canRedo = at < history.size - 1,
            onLive = { look = it },
            onSettled = { push() },
            onUndo = {
                if (at > 0) {
                    at -= 1
                    look = history[at]
                }
            },
            onRedo = {
                if (at < history.size - 1) {
                    at += 1
                    look = history[at]
                }
            },
            onOriginal = {
                look = Look.NONE
                push()
            }
        )
    }
}

/**
 * Il palco: l'immagine con il conto applicato sopra.
 *
 * ⚠️⚠️ **IL CONTO SI APPLICA COL PENNELLO E NON CON UN SECONDO BITMAP**: disegnare l'anteprima
 * dentro uno shader costa un solo passaggio sulla scheda grafica a ogni fotogramma, mentre
 * rigenerare una mappa di pixel a ogni movimento del cursore vorrebbe dire decine di
 * millisecondi per dito mosso, cioè un cursore che scatta.
 *
 * ⚠️ **A riposo lo shader non si mette affatto**: senza valori da applicare il programma
 * restituirebbe esattamente quello che riceve, e saltarlo è insieme più veloce e la prova che il
 * confronto col prima mostra davvero l'immagine di partenza.
 */
@Composable
private fun LookStage(
    picture: Bitmap,
    look: Look,
    onCompare: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val hold = stringResource(R.string.look_compare)
    Canvas(
        modifier = modifier
            .semantics { contentDescription = hold }
            /*
             * ⚠️⚠️ **IL TOCCO LUNGO SI SCRIVE A MANO E NON CON `detectTapGestures`**: quello
             * annuncia il tocco lungo **una volta**, mentre qui serve sapere anche **quando il
             * dito si alza**, cioè per quanto tempo il confronto resta acceso. Con l'altra via
             * servirebbe un secondo gesto per il rilascio, e i due si contenderebbero l'evento.
             */
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val alzato = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        waitForUpOrCancellation()
                    }
                    if (alzato == null) {
                        onCompare(true)
                        waitForUpOrCancellation()
                        onCompare(false)
                    }
                }
            }
    ) {
        val room = size
        if (room.width <= 0f || room.height <= 0f) return@Canvas
        val wide = picture.width.toFloat() / picture.height
        val box = if (room.width / room.height > wide) {
            val h = room.height
            val w = h * wide
            RectF((room.width - w) / 2f, 0f, (room.width + w) / 2f, h)
        } else {
            val w = room.width
            val h = w / wide
            RectF(0f, (room.height - h) / 2f, w, (room.height + h) / 2f)
        }

        val image = BitmapShader(picture, TileMode.CLAMP, TileMode.CLAMP).apply {
            setLocalMatrix(
                Matrix().apply {
                    setRectToRect(
                        RectF(0f, 0f, picture.width.toFloat(), picture.height.toFloat()),
                        box,
                        Matrix.ScaleToFit.FILL
                    )
                }
            )
        }
        val shader = if (look.idle) null else lookShader(image, look)
        val paint = Paint().apply {
            asFrameworkPaint().isFilterBitmap = true
            asFrameworkPaint().shader = shader ?: image
        }
        drawIntoCanvas { tela -> tela.drawRect(box.left, box.top, box.right, box.bottom, paint) }
    }
}

/**
 * La scheda in fondo: i cursori del modulo e i tre comandi della storia.
 *
 * ⚠️⚠️ **I MODULI SONO UNO SOLO E LA FILA CHE LI SCEGLIE NON SI DISEGNA ANCORA**, ed è dichiarato
 * invece di essere una dimenticanza: con un modulo solo, una fila di un gettone direbbe soltanto
 * dove si è, che è l'unico posto possibile. Entra col Colore, cioè al giro dopo, e la struttura
 * che la regge (un elenco di moduli, ognuno coi suoi cursori) c'è già.
 */
@Composable
private fun LookSheet(
    look: Look,
    busy: Boolean,
    ready: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onLive: (Look) -> Unit,
    onSettled: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOriginal: () -> Unit
) {
    val light = look.light
    /*
     * ⚠️⚠️ **LA SUPERFICIE È QUELLA DELL'EDITOR DI CASA, riga per riga**: il fondo del palco che
     * passa sotto gli angoli stondati, il bordo d'accento che corre di fuori, il colore, e il
     * rientro di sistema dentro invece che sopra. Le ragioni di ognuna di quelle righe sono
     * misurate e vivono su `EditorSheet`: qui si ripetono perché le due schede sono la stessa
     * superficie, e una scritta a modo suo si vedrebbe al primo passaggio fra i due editor.
     */
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(stageBack())
            .edgedTop(PANEL_ROUND),
        shape = RoundedCornerShape(topStart = PANEL_ROUND, topEnd = PANEL_ROUND),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
                .padding(start = 16.dp, end = 16.dp, top = SHEET_TOP, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.look_light),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp).heading()
            )

            LookKnob(
                name = stringResource(R.string.look_exposure),
                value = light.exposure,
                span = Light.EXPOSURE_RANGE,
                stops = true,
                enabled = ready && !busy,
                onLive = { onLive(look.copy(light = light.copy(exposure = it))) },
                onSettled = onSettled
            )
            LookKnob(
                name = stringResource(R.string.look_brightness),
                value = light.brightness,
                enabled = ready && !busy,
                onLive = { onLive(look.copy(light = light.copy(brightness = it))) },
                onSettled = onSettled
            )
            LookKnob(
                name = stringResource(R.string.look_contrast),
                value = light.contrast,
                enabled = ready && !busy,
                onLive = { onLive(look.copy(light = light.copy(contrast = it))) },
                onSettled = onSettled
            )
            LookKnob(
                name = stringResource(R.string.look_shadows),
                value = light.shadows,
                enabled = ready && !busy,
                onLive = { onLive(look.copy(light = light.copy(shadows = it))) },
                onSettled = onSettled
            )
            LookKnob(
                name = stringResource(R.string.look_highlights),
                value = light.highlights,
                enabled = ready && !busy,
                onLive = { onLive(look.copy(light = light.copy(highlights = it))) },
                onSettled = onSettled
            )

            /*
             * ⚠️⚠️ **TRE ICONE E NON TRE SCRITTE, DALLA `2.15`, ED È IL SUO RISCONTRO** (giro
             * della `2.14`, voce `luce-storia`: *'Annulla' e 'Ripristina' devono essere icone, non
             * testo*).
             * ⚠️⚠️ **E I GLIFI SONO QUELLI CHE L'EDITOR DI CASA USA GIÀ PER GLI STESSI TRE
             * COMANDI**, cioè i suoi: disegnarne altri vorrebbe dire due segni per lo stesso gesto
             * a un tocco di distanza, visto che dalla stessa immagine si entra nell'uno o
             * nell'altro editor.
             * ⚠️ **Anche il terzo, che lui non ha nominato**: due icone accanto a una scritta
             * sarebbero una fila che si legge in due modi, e 'Originale' il suo glifo ce l'ha già.
             */
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onUndo, enabled = canUndo && !busy) {
                    Icon(Glyphs.EditUndo, stringResource(R.string.editor_undo))
                }
                IconButton(onClick = onRedo, enabled = canRedo && !busy) {
                    Icon(Glyphs.EditRedo, stringResource(R.string.editor_redo))
                }
                IconButton(onClick = onOriginal, enabled = !look.idle && !busy) {
                    Icon(Glyphs.EditReset, stringResource(R.string.editor_original))
                }
            }
        }
    }
}

/**
 * Una riga di cursore: il nome, la barra e il numero.
 *
 * ⚠️⚠️ **IL NUMERO È IL TASTO CHE AZZERA, e non c'è un secondo comando**: in un editor a cursori
 * il gesto che si fa più spesso è 'rimetti questo a zero', e senza una via rapida lo si insegue
 * col dito senza mai centrarlo. Il numero c'è già, è largo abbastanza da toccarlo, e quando il
 * valore è zero non fa niente, quindi non serve nemmeno spegnerlo.
 *
 * ⚠️ **Due chiamate diverse mentre si trascina e alla fine**: quella continua muove quello che si
 * vede, quella finale scrive un passo nella storia. Vedi la nota sulla pila in
 * [AdvancedEditorScreen].
 *
 * ⚠️⚠️ **E QUELLA FINALE NON PORTA NESSUN VALORE, CHE È LA CORREZIONE PIÙ IMPORTANTE DI QUESTO
 * FILE**: la prima stesura scriveva `onValueChangeFinished = { onSettled(value) }`, cioè
 * consegnava il **parametro** di questa funzione, che è un valore catturato alla composizione.
 * Compose chiama `onValueChange` e `onValueChangeFinished` **senza per forza ricomporre in
 * mezzo**, quindi il passo poteva portare il valore di prima: l'immagine cambiava e 'Annulla'
 * restava spento. Adesso il passo se lo va a prendere da solo dallo stato vivo, e non c'è niente
 * da tenere allineato. ⚠️ **Il difetto l'ha preso `LuceTest` alla prima corsa**, ed è la ragione
 * per cui quella prova è nata con la funzione invece che dopo una segnalazione.
 */
@Composable
private fun LookKnob(
    name: String,
    value: Float,
    enabled: Boolean,
    onLive: (Float) -> Unit,
    onSettled: () -> Unit,
    span: Float = 1f,
    stops: Boolean = false
) {
    val zero = stringResource(R.string.look_reset_one, name)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(KNOB_NAME)
        )
        Slider(
            value = value,
            onValueChange = onLive,
            onValueChangeFinished = onSettled,
            valueRange = -span..span,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
        Text(
            /*
             * ⚠️ **Gli stop si scrivono con due decimali e il resto come numero intero**: un
             * diaframma è +0,33 e una percentuale è +33, e scriverli allo stesso modo
             * farebbe leggere un valore per l'altro.
             */
            text = if (stops) "%+.2f".format(value) else "%+d".format((value * 100).roundToInt()),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.End,
            color = if (abs(value) < 0.0005f) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .width(KNOB_VALUE)
                .clickable(enabled = enabled) { onLive(0f); onSettled() }
                .semantics { contentDescription = zero }
        )
    }
}

/** Quanto è larga la colonna dei nomi dei cursori. */
private val KNOB_NAME = 96.dp

/** Quanto è larga la colonna del numero: ci deve stare `-100` col segno. */
private val KNOB_VALUE = 48.dp

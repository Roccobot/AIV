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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
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
     * Se una **filigrana** è pronta da scrivere, cioè se il salvataggio ha qualcosa da fare
     * anche su un'immagine intonsa: vedi `ViewerViewModel.markReady`.
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
     * Il **ridimensionamento** configurato, che esiste sempre anche quando non si applica:
     * spegnere l'interruttore non deve far perdere quello che si era scelto.
     */
    resize: Resize.Plan,
    /**
     * Il ridimensionamento **predefinito**, che serve alla finestra per sapere se il piano che
     * ha in mano è già quello: senza, 'Rendi predefinito' resterebbe acceso dopo averlo toccato.
     */
    saved: Resize.Plan,
    /** Se quel piano si applica al salvataggio, cioè l'interruttore della `2.70`. */
    resizing: Boolean,
    /** `null` spegne; un piano lo scrive **e** accende, come 'Applica' della sua finestra. */
    onResize: (Resize.Plan?) -> Unit,
    /** Il piano diventa il predefinito, cioè il comando 'Rendi predefinito' della `2.81`. */
    onResizeDefault: (Resize.Plan) -> Unit,
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
    onSave: (turns: Int, mirror: Boolean, crop: ImageEdit.Crop) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    /*
     * ⚠️⚠️ **LA ROTAZIONE È INIBITA FINCHÉ SI STA QUI, DALLA `1.69`, ED È UNA SUA DECISIONE**
     * (giro della `1.67`, domanda `d-rotazione`: *l'unica scelta possibile è inibire la
     * rotazione finché si è nell'editor: usare editor di immagini in orizzontale è
     * impensabile, la UX è terribile*). L'alternativa era conservare ritaglio e passi
     * attraverso la rotazione, e lui l'ha scartata: il problema non era perdere il lavoro, era
     * ritrovarsi in una schermata inusabile.
     * ⚠️⚠️ **VERTICALE E NON 'BLOCCATO COM'È', e la differenza conta**: `SCREEN_ORIENTATION_LOCKED`
     * congela l'orientamento **corrente**, quindi chi entra nell'editor da orizzontale ci
     * resterebbe, che è esattamente il caso che lui chiama impensabile. Il verticale è la sola
     * lettura che soddisfi la ragione che ha dato, non solo il meccanismo che ha nominato.
     * ⚠️ **Si rimette a posto uscendo**, e il valore da rimettere si legge invece di essere
     * indovinato: l'attività potrebbe averne uno suo, e scrivere `UNSPECIFIED` alla cieca
     * cambierebbe il comportamento di tutto il resto dell'app.
     * ⚠️ **L'attività si trova risalendo i contesti** ([Knobs.activityOf]): il contesto di una
     * composizione è quasi sempre un `ContextWrapper`, e un cast diretto risponderebbe nullo
     * proprio dove l'attività c'è.
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

    /*
     * ⚠️⚠️ **UNO STATO SOLO PER LA POSA, DALLA `2.02`, E PRIMA ERA UN INTERO**: da quando c'è
     * anche la riflessione, 'girato di due quarti' e 'specchiato' non sono due variabili
     * indipendenti, perché uno specchio davanti a una rotazione la rovescia. Tenendone due,
     * ogni chiamante avrebbe dovuto ricordarsi quel conto: qui lo fa `Spin.then`, una volta.
     */
    var spin by remember(base) { mutableStateOf(Spin.STILL) }
    var shape by remember(base) { mutableStateOf(Shape.FREE) }
    var crop by remember(base) { mutableStateOf(ImageEdit.Crop.WHOLE) }

    BackHandler { onBack() }

    LaunchedEffect(uri) {
        origin = withContext(Dispatchers.IO) { preview(context, uri) }
    }


    // ⚠️ L'anteprima girata si ricalcola SOLO quando cambia il quarto di giro: girare una
    // mappa di pixel da due megapixel a ogni ridisegno vorrebbe dire farlo a ogni dito che
    // si muove sul rettangolo.
    val shown: ImageBitmap? = remember(base, spin) {
        // ⚠️ Passa da [spunBy], che è la funzione condivisa coi due salvataggi: fino alla
        // `1.80` questo punto era il solo dei tre senza rete, quindi un errore di memoria su
        // un'immagine grossa arrivava dentro la composizione.
        base?.spunBy(spin.turns, spin.mirror)?.asImageBitmap()
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
     * tasti qui sotto possono fare i loro conti senza sapere niente di dov'è l'immagine
     * sullo schermo.
     */
    val aspect = shown?.let { it.width.toFloat() / it.height } ?: 1f

    /** Se c'è qualcosa di non ancora confermato con 'Applica'. */
    val pending = spin != Spin.STILL || !crop.whole

    /** Tutto quello che si è fatto finora, composto in una posa e un rettangolo soli. */
    val total = after(steps.lastOrNull()?.done ?: Done.NOTHING, spin, crop)

    /*
     * ⚠️ **Le proporzioni vengono da [origin] e non da [base]**: il salvataggio applica il
     * totale dei passi all'immagine di partenza, quindi la forma da cui partire è quella, e la
     * posa e il ritaglio li porta [total]. Il perché il lato lungo e la forma si prendano da due
     * fonti diverse vive su [Resize.frameSize].
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
     * ⚠️⚠️ **IL RIENTRO DI SISTEMA NON STA PIÙ QUI, dalla 1.42, ed è quello che porta la scheda
     * al bordo di sotto** (riscontro `sotto-barra`: *fallo dappertutto, incluso l'editor*).
     * Applicato alla colonna intera, il rientro toglieva spazio anche alla scheda in fondo, che
     * quindi si fermava sopra la barra di sistema: là sotto restava la pagina, di un colore
     * diverso dal pannello. Adesso ogni pezzo prende i lati che lo riguardano, e la scheda
     * arriva al vetro col suo colore.
     * ⚠️ **Sono tre pezzi e non due**: la testata vuole il rientro in alto, il palco solo quelli
     * ai fianchi, la scheda quello in fondo, e i fianchi li vogliono tutti e tre (in orizzontale
     * il ritaglio del display è di lato).
     * ⚠️⚠️ **`safeDrawing` E NON `systemBars`**, come prima: comprende anche il ritaglio del
     * display e la tastiera, e questa schermata dichiara `shortEdges`.
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
                 * ⚠️⚠️ **I DUE LATI NON HANNO LO STESSO RIENTRO, e non è una svista: uno porta
                 * un'icona e l'altro una parola** (riscontro dell'utente, 2026-09-04: *'Salva'
                 * è troppo a destra, dovrebbe allinearsi alla linea immaginaria cui si allinea
                 * tutto il resto su quel lato*). Quella linea è [STAGE_SIDE], cioè dove
                 * cominciano il palco e le file di chip, e il testo di 'Salva' ci arriva
                 * togliendo dal rientro i 12dp che il tasto si porta dentro
                 * (`TextButtonHorizontalPadding`, letto sul bytecode di material3
                 * 1.5.0-alpha26).
                 * ⚠️ **A sinistra resta 4dp**, quindi la freccia cade a 16dp dal bordo: è
                 * l'allineamento **ottico** che Material dà a un'icona di navigazione, e un
                 * glifo tondeggiante appoggiato sulla linea del testo si legge come rientrato.
                 * Chi volesse i due lati uguali cambi questo `start`, non l'altro.
                 */
                .padding(start = 4.dp, end = STAGE_SIDE - TEXT_BUTTON_PAD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back))
            }
            /*
             * ⚠️⚠️ **IN TESTA C'È 'MODIFICA IMMAGINE', DALLA `2.20`, E LA STRINGA È LA STESSA
             * DELL'EDITOR COMPLETO** (sua istruzione, 2026-09-12: *l'utente deve pensare alla
             * differenza tra i due (e alla loro stessa esistenza) solo quando fa la scelta*).
             * Quindi la chiave resta questa, che è sempre stata 'il titolo della schermata mentre
             * si modifica', e a cambiare è il suo testo.
             * ⚠️⚠️ **DECADE LA NOTA DELLA `1.49`, e conviene saperlo per non rimetterla**: diceva
             * *questa schermata si chiama 'Editor' e non 'Modifica'* (sua istruzione, 2026-09-04:
             * *'Modifica' è la funzione, che mette a disposizione anche altre app: una volta
             * scelto l'editor interno, è quello il suo nome*). Allora l'editor in casa era uno; da
             * quando sono due, il nome del singolo è esattamente la cosa che non serve sapere
             * mentre si lavora, e i due nomi restano dove la scelta si fa.
             * ⚠️ **Il corpo è `headlineSmall`, come TUTTE le altre schermate**, dalla 1.49
             * versione: era `titleMedium`, cioè l'unico titolo dell'app scritto più piccolo,
             * e nessuna nota diceva perché. La ragione per cui poteva essere voluto era che
             * questa testata porta anche un comando, e la misura l'ha smentita: a scala
             * normale del carattere il titolo grande ci sta in tutte e ventisette le lingue,
             * col caso peggiore (telugu su 320dp) che avanza ancora 45dp.
             */
            Text(
                text = stringResource(R.string.editor_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f).heading()
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
            /*
             * ⚠️⚠️ **'Filigrana' E 'Ridimensiona' SONO SCESI NELLA BARRA IN BASSO, DALLA `2.79`,
             * ED È SUA RICHIESTA** (punto C del campo libero del giro dalla `2.75` alla `2.77`:
             * *sono troppo lontani e poco raggiungibili dal pollice*). Qui come nell'editor
             * completo, perché i due editor si aprono dalla stessa immagine e un comando che
             * cambia posto fra l'uno e l'altro è un comando da ricercare.
             */
            TextButton(
                onClick = { onSave(total.spin.turns, total.spin.mirror, total.crop) },
                // ⚠️⚠️ **UNA FILIGRANA È LAVORO DA SALVARE, DALLA `2.69`**: chi apre l'editor per
                // firmare un'immagine e basta non tocca la posa e non taglia niente, e senza
                // questa condizione il tasto resterebbe spento, cioè la firma da sola non si
                // potrebbe applicare mai. ⚠️ **E dalla `2.70` anche un ridimensionamento che
                // rimpicciolisce davvero**, per la stessa ragione e con la stessa forma.
                enabled = shown != null && !busy &&
                    (marked || shrinks || !(total.spin == Spin.STILL && total.crop.whole))
            ) {
                Text(stringResource(R.string.editor_save))
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                /*
                 * ⚠️⚠️ **IL GRIGIO STA QUI, PRIMA DEI DUE RIENTRI, e l'ordine è la cosa che
                 * conta**: dipinge la fascia **intera** fra la testata e la scheda, mentre
                 * l'immagine resta dentro il rientro di sistema e i suoi margini. Messo dopo,
                 * il grigio si fermerebbe dove finisce l'immagine e resterebbe una cornice del
                 * colore della pagina, cioè si vedrebbero tre fondi invece di uno.
                 * Il perché di questo colore, e le misure, stanno su [stageBack].
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

        EditorSheet(
            shape = shape,
            lay = lay,
            busy = busy,
            ready = shown != null,
            pending = pending,
            applied = steps.isNotEmpty(),
            undone = undone.isNotEmpty(),
            /*
             * ⚠️ **I due comandi del salvataggio arrivano alla scheda come uno spazio da
             * riempire**, per la stessa ragione dell'editor completo: vivono nella barra in
             * basso, che è dentro la scheda, ma quello che governano (il logo scelto, i due
             * interruttori, il piano, la finestra che si apre) vive qui.
             */
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
                // ⚠️ La proporzione si rifà sull'aspetto NUOVO, che è il reciproco di quello
                // di adesso: dopo un quarto di giro i due lati si scambiano, e il conto fatto
                // con l'aspetto vecchio darebbe un rettangolo storto per un fotogramma.
                crop = shape.fit(1f / aspect, lay)
            },
            /*
             * ⚠️⚠️ **RIFLETTERE NON RIFÀ IL RETTAGLIO, E GIRARE SÌ: la differenza è l'aspetto**
             * (richiesta dell'utente, 2026-09-09). Un quarto di giro scambia i due lati, quindi
             * la selezione va rifatta sulla proporzione nuova; uno specchio lascia i lati come
             * sono, quindi il rettangolo si può **ribaltare** e resta esattamente sulla stessa
             * porzione di immagine. Rifarlo anche qui sarebbe buttare via una selezione in corso
             * per niente.
             * ⚠️ **Lo ribalta [spunRect]**, cioè la stessa funzione che compone i passi: il
             * rettangolo e l'immagine si muovono insieme per costruzione, e non perché due conti
             * scritti a parte dicono la stessa cosa.
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
                // ⚠️ **Un passo indietro solo, e il passo è quello che si vede**: se c'è un
                // ritocco non confermato è lui il passo, altrimenti è l'ultimo 'Applica'. È la
                // regola di ogni annullamento, e l'alternativa (disfare sempre un 'Applica'
                // lasciando in piedi il ritocco in corso) farebbe sparire un pezzo di immagine
                // mentre il rettangolo resta dov'è.
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


/** Una posa e un rettangolo: quello che si sa applicare al file vero. */
private data class Done(val spin: Spin, val crop: ImageEdit.Crop) {
    companion object {
        val NOTHING = Done(Spin.STILL, ImageEdit.Crop.WHOLE)
    }
}

/**
 * Quello che si ottiene facendo [spin] e [crop] **dopo** [done].
 *
 * ⚠️⚠️ **LA COMPOSIZIONE È ESATTA, non un'approssimazione, e vale la pena sapere perché**: la
 * catena è sempre 'metti in posa, poi ritaglia' (lo è in `ImageEdit.redraw`), e mettere in posa
 * un ritaglio è la stessa cosa che ritagliare l'immagine in posa, col rettangolo messo in posa
 * dentro il quadrato unitario ([spunRect]). Portata fuori la posa, restano due ritagli uno
 * dentro l'altro, e due ritagli si compongono in uno ([insideOf]). Quindi n passi qualunque
 * diventano una posa e un rettangolo, sempre, senza perdere niente.
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
 * sinistro nuovo viene dal fondo vecchio. Chi la ritocca la riderivi da lì: il
 * segno sbagliato dà un ritaglio speculare, che su una fotografia simmetrica non si vede.
 * ⚠️ **Lo specchio viene PRIMA delle rotazioni**, come nella posa che descrive: sul quadrato
 * unitario ribalta la x, quindi il lato sinistro nuovo viene dal destro vecchio.
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
    /** Se c'è un ritocco non ancora confermato. */
    pending: Boolean,
    /** Se c'è almeno un 'Applica' alle spalle. */
    applied: Boolean,
    /** Se c'è almeno un passo disfatto che 'Ripristina' può rimettere. */
    undone: Boolean,
    /**
     * I due comandi del salvataggio, che dalla `2.79` vivono nella barra in basso.
     *
     * ⚠️ **Riceve il lato come argomento** perché è la scheda a saperlo: il blocco cambia lato
     * col FAB, come ogni altra fila di comandi di questa app.
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
    Surface(
        /*
         * ⚠️⚠️ **IL GRIGIO DEL PALCO PASSA SOTTO QUESTA SCHEDA, dalla 1.53** (riscontro
         * dell'utente, giro della `1.51`: *il colore va bene, ma il grigio non va 'sotto' gli
         * angoli arrotondati della parte superiore della bottomsheet*). Gli angoli in cima
         * sono stondati, quindi nei due spicchi che scavano si vede quello che sta **dietro**
         * la scheda: senza questa riga era il fondo della pagina, cioè un terzo colore che
         * compariva in due gnocchetti proprio sul bordo fra le due superfici.
         * ⚠️ **Il fondo sta nel modificatore del CHIAMANTE e non in `color`, e l'ordine è
         * tutto**: `Surface` accoda al modificatore ricevuto il proprio `background(color,
         * shape)` e il proprio `clip(shape)`, quindi quello che si dipinge qui resta un
         * rettangolo **non ritagliato**, e la scheda gli si stampa sopra tenendo i suoi
         * angoli. Passarlo come `color` lo ritaglierebbe insieme a lei, cioè non cambierebbe
         * niente.
         * ⚠️ **Non serve nessun rientro negativo e nessun riquadro in più**: il palco
         * finisce dove comincia la scheda, e a coprire i due spicchi basta che la scheda
         * porti il proprio fondo con sé.
         */
        /*
         * ⚠️⚠️ **E DALLA `1.55` PORTA IL BORDO D'ACCENTO, su tre lati** (decisione dell'utente,
         * giro della `1.54`: *voglio la riga anche lì: in realtà dappertutto ... per coerenza
         * deve avere il tratto intorno come tutti gli altri elementi simili*). Come la scheda
         * della selezione, questa non ha il velo e non lo avrà: il palco dietro deve restare
         * visibile e toccabile. Il bordo non dipende da quello.
         * ⚠️ **Va DOPO il fondo del palco e prima della forma**: il bordo lo disegna un nodo che
         * riceve la misura della superficie, e il fondo qui sotto è il rettangolo non ritagliato
         * che copre i due spicchi degli angoli. Invertirli metterebbe la riga sotto il fondo.
         * ⚠️⚠️ **E DALLA `1.56` LA RIGA CORRE DI FUORI** (sua prova, giro della `1.55`): si
         * vedono la cima e i due archi, i fianchi finiscono oltre il bordo dello schermo. ⚠️ Il
         * fondo del palco qui sopra **non** la copre, ed è l'ordine a garantirlo: quel
         * rettangolo è grande quanto la scheda, mentre la riga sta un filo più su.
         */
        modifier = Modifier
            .fillMaxWidth()
            .background(stageBack())
            .edgedTop(PANEL_ROUND),
        shape = RoundedCornerShape(topStart = PANEL_ROUND, topEnd = PANEL_ROUND),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        /*
         * ⚠️⚠️ **NIENTE OMBRA DALLA 1.40, come le altre due schede appoggiate in basso**
         * (richiesta dell'utente, 2026-09-03: *evita le ombreggiature in basso*). Su una
         * superficie ancorata al bordo l'ombra si vede solo di sotto, dove va a finire
         * sull'angolo stondato del vetro: il perché per esteso sta su `PickSheet`, dov'è
         * misurato.
         * ⚠️ **E dalla 1.42 arriva sotto la barra di sistema come le altre due**: il rientro
         * non è più sulla colonna della schermata ma sui suoi tre pezzi, e il perché sta là.
         * ⚠️⚠️ **E NEMMENO IL RILIEVO TONALE, DALLA `1.78`: NON FACEVA NIENTE.** Il perché
         * misurato sta sulla `Surface` di `PickSheet`, che portava lo stesso parametro spento:
         * il rilievo tonale vale solo su una superficie di colore `surface`, e questa è
         * `surfaceContainerHigh`.
         */
    ) {
        Column(
            /*
             * ⚠️⚠️ **IL RIENTRO DI SISTEMA STA QUI, e la scheda arriva al bordo**: il fondo
             * della scheda passa **sotto** la barra, il contenuto no, quindi le file di tasti
             * restano dove sono e a cambiare è la sola striscia in fondo, che prende il colore
             * della scheda invece di quello della pagina. È la stessa cura di `PickSheet`.
             */
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
                /*
                 * ⚠️⚠️ **IL RESPIRO IN CIMA, dalla 1.49, e prima era ZERO** (riscontro
                 * dell'utente, 2026-09-04: *la bottomsheet dei comandi dell'editor continua a
                 * non avere abbastanza spazio: arriva a pelo delle chip delle proporzioni*).
                 * La prima fila è di chip alti 32dp senza rientro proprio, quindi appoggiava
                 * direttamente sul bordo della scheda, dentro la curva dei suoi angoli da
                 * [PANEL_ROUND].
                 * ⚠️ **Perché [SHEET_TOP] e non i 12dp della scheda delle informazioni**: là
                 * il respiro lo dà anche il bersaglio da 48dp della crocetta di chiusura, che
                 * qui non c'è. Misurato: a 16dp dal bordo la curva dell'angolo è già rientrata
                 * di 5dp, cioè meno del rientro laterale della fila, e i chip la scansano.
                 */
                .padding(top = SHEET_TOP),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            /*
             * ⚠️⚠️ **I DUE VERSI VIVONO DENTRO LA FILA DELLE FORME, DALLA `2.88`**, accanto a
             * 'Originale' (§ [ShapeRow]): fino alla `2.87` erano una riga di chip tutta loro qui
             * sotto. **Cambiarli RIBALTA la selezione sul posto** (richiesta dell'utente,
             * 2026-08-31): da 16:9 si passa a 9:16, e una selezione libera si inverte allo stesso
             * modo. ⚠️ **Il centro non si muove**, ed è la parte che rende il gesto utile invece che
             * spaesante: si sta scegliendo *che forma* dare al ritaglio, non *dove* metterlo.
             * ⚠️ **Gli otto punti sotto sono quelli che la riga dei versi aveva di suo**, cioè
             * l'aria prima delle due file di tasti.
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
            // ⚠️ **Dalla 1.63 sono le sue**, e la clausola di deprecazione è caduta con
            // loro: un disegno nostro non si specchia da sé, quindi non c'è nessuna variante
            // AutoMirrored da scartare. Il verso resta quello fisico, che è il punto della
            // nota qui sopra.
            val ccw = Glyphs.TurnLeft
            val cw = Glyphs.TurnRight

            val turnLeft = PadAction(PadKey.TURN_LEFT, ccw, R.string.editor_left, enabled = live) { onTurn(3) }
            val turnRight = PadAction(PadKey.TURN_RIGHT, cw, R.string.editor_right, enabled = live) { onTurn(1) }
            /*
              * ⚠️ **Le due icone sono DISEGNATE DALL'UTENTE** (2026-09-01, voce `ed-sheet`
              * del collaudo: *usa le mie icone che ti ho già passato*): quelle di Material
              * non gli dicevano abbastanza. Vedi [Glyphs.AlignAcross].
              */
            /*
             * ⚠️⚠️ **IL TOCCO LUNGO CENTRA SU TUTTI E DUE GLI ASSI, DALLA `2.10`, ED È SUO**
             * (punto F del campo libero del giro accorpato: *pressione lunga sui due tasti di
             * centratura: centra su tutti e due gli assi*). Lo stesso gesto sui due tasti fa la
             * stessa cosa, e non è una svista: quello che si vuole è *al centro*, e chi tiene
             * premuto non deve chiedersi quale dei due tasti sia quello giusto.
             * ⚠️ **Il conto è la composizione delle due funzioni, e l'ordine non conta**: una
             * tocca i due lati verticali e l'altra i due orizzontali, quindi sono indipendenti.
             * Scrivere un terzo conto darebbe una terza definizione di 'al centro'.
             * ⚠️ **L'etichetta del gesto c'è**, come vuole [PadAction.onHold]: un gesto che il
             * lettore di schermo non annuncia esiste solo per chi lo scopre per caso.
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
             * ⚠️⚠️ **L'ETICHETTA DICE 'Rifletti' E BASTA, E IL VERSO LO DICONO IL GLIFO E IL
             * GESTO** (sua richiesta, 2026-09-09: *un 'Rifletti in orizzontale' (a pressione
             * lunga diventa 'Rifletti in verticale')*, col suo dubbio: *forse non ci sta
             * l'etichetta di testo*). Il dubbio era fondato e la causa è misurata: con cinque
             * celle la fila si stringe, e 'Rifletti in orizzontale' non entra sotto un glifo da
             * 24 punti nemmeno su due righe. Quello che entra è la parola sola, e i due versi
             * restano dove si leggono davvero: il glifo mostra lo specchio destra-sinistra, e
             * il tocco lungo si annuncia con la sua etichetta.
             * ⚠️ **Il tocco lungo ha SEMPRE la sua etichetta**, come vuole [PadAction.onHold]:
             * un gesto che il lettore di schermo non annuncia esiste solo per chi lo scopre per
             * caso.
             * ⚠️⚠️ **IL GLIFO È DI MATERIAL E CI RESTA, DALLA `2.03`, ED È SUA RISPOSTA**
             * (`d-flip-glifo` del giro della `2.02`: **`resta`**, cioè *va bene quello di
             * Material*). Gli altri sette di questa scheda li ha disegnati lui, quindi questo si
             * vede che viene da un'altra mano, e la domanda esisteva per quello: con
             * `FolderDownload` ed `Extension`, nella stessa situazione, aveva risposto mandando i
             * suoi. Qui ha scelto il contrario, quindi **non è più provvisorio** e non si
             * richiede.
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
                // ⚠️⚠️ **SPENTO FINCHÉ C'È UN RITOCCO IN SOSPESO, e non è pignoleria**: il
                // rettangolo in corso è in frazioni dell'immagine di **adesso**, e rimettere
                // un passo sotto di lui gli farebbe selezionare un'altra cosa senza che
                // nessuno l'abbia mosso. 'Annulla' toglie il ritocco e lo riaccende.
                enabled = live && undone && !pending
            ) { onRedo() }
            val originalKey = PadAction(
                PadKey.ORIGINAL, Glyphs.EditReset, R.string.editor_original,
                enabled = live && (pending || applied || undone)
            ) { onOriginal() }

            /*
             * ⚠️⚠️ **LE DUE FILE NON SI SPECCHIANO PIÙ, dalla `1.56`, e l'ordine lo sceglie
             * lui trascinando** (decisione dell'utente, giro della `1.54`: *toglili dalla
             * specchiatura e rendili riordinabili*). Fino alla `1.55` erano scritte per esteso
             * nei due versi, perché rovesciare una lista dava l'ordine sbagliato in due punti
             * su otto: 'Ruota a sinistra' e 'Ruota a destra' hanno il verso delle frecce e non
             * quello della lettura, e lo stesso vale per 'Annulla' e 'Ripristina'.
             * ⚠️ **Quel difetto non torna, e la ragione è che adesso non c'è nessun
             * rovesciamento**: un ordine salvato è una lista, e chi tiene la sinistra la mette
             * come vuole invece di riceverne una specchiata da un'altra impostazione.
             * ⚠️ **Restano DUE file e non una**: il filetto in mezzo dice che girare e centrare
             * è un mestiere e la cronologia un altro, quindi un tasto non passa di fila. Sono
             * due ordini salvati, non uno da otto.
             */
            /*
             * ⚠️⚠️ **LA PRIMA FILA HA CINQUE COLONNE E LA SECONDA QUATTRO, DALLA `2.02`, E LE
             * DUE NON SI ALLINEANO PIÙ**: è il prezzo del tasto nuovo, e va saputo per non
             * leggerlo come un difetto. Il posto è quello che ha chiesto lui (*al centro fra
             * 'Centra in orizzontale' e 'Ruota a sinistra'*), che in una fila di cinque è
             * esattamente la terza cella. ⚠️ **Le due file restano allineate agli estremi**,
             * perché lo spazio si distribuisce fra le celle: a divergere sono le colonne in
             * mezzo, che fra un mestiere e l'altro il filetto già separa.
             * ⚠️ **La seconda fila NON passa a cinque per simmetria**: là i tasti sono quattro,
             * e una colonna vuota in fondo sarebbe un buco invece di un allineamento.
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
             * ⚠️⚠️ **LA BARRA DEI DUE COMANDI DEL SALVATAGGIO, DALLA `2.79`**: là dove l'editor
             * completo ha la sua fila di icone, qui c'è una riga che porta solo loro, perché la
             * cronologia di questa scheda vive nella fila qui sopra. Il posto è lo stesso nei
             * due editor, che è quello che la sua richiesta chiede: il pollice li trova in
             * fondo, e non in cima.
             * ⚠️ **Il lato lo dà [fabEdge]**, cioè la stessa preferenza che governa il FAB e la
             * barra dell'editor completo: una seconda lettura direbbe la stessa cosa fino al
             * giorno che una delle due cambia.
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
 *
 * ⚠️⚠️ **E DALLA `2.88` SPENTO SI VEDE SPENTO, E FINO ALLA `2.87` NO** (voce `originale-completo`
 * del giro della `2.87`, accettabile: *anche se disattivate le icone orizzontale/verticale NON si
 * spengono e sembrano attive*). Il riscritto aveva perso lo stato spento del disegno: `Surface`
 * con `enabled = false` smette di rispondere al tocco ma non cambia un colore, quindi il chip
 * scelto restava pieno e l'altro restava col suo filetto. Con 'Originale' i due versi si spengono
 * dalla `2.87`, ed è il primo caso in cui un chip spento resta in scena da solo.
 * ⚠️ **I numeri sono quelli di Material, letti nel bytecode di material3 1.5.0-alpha26**
 * (`FilterChipTokens`): il testo e l'icona a [CHIP_OFF_INK] del colore del testo, il fondo del
 * chip scelto e il filetto degli altri a [CHIP_OFF_FILL]. Sono lo stato spento del `FilterChip`,
 * cioè quello che il componente di partenza avrebbe fatto da sé.
 * ⚠️ **Vale per tutti i chip del pannello**, e non è un effetto collaterale: si spengono anche
 * mentre un salvataggio è in corso e prima che l'anteprima arrivi, e in quei momenti erano spenti
 * davvero senza dirlo.
 */
@Composable
internal fun SheetChip(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Il corpo della parola, dalla `2.34`.
     *
     * ⚠️ **Ha il valore di serie di sempre**, quindi i chip che non lo nominano non cambiano: lo
     * passa la sola fila delle forme, che nell'editor semplice scende di un gradino (il conto vive
     * su [ShapeRow]).
     */
    style: TextStyle = MaterialTheme.typography.labelLarge,
    /**
     * Un disegno al posto della parola, dalla `2.80`.
     *
     * ⚠️⚠️ **CON LUI [text] DIVENTA LA DESCRIZIONE PARLATA, E NON SPARISCE**: è il criterio dei
     * sette gettoni dei moduli e dei comandi del ritaglio, cioè il nome vive dove un lettore di
     * schermo lo trova e dove il banco lo cerca. Lo chiedono i due versi del ritaglio, che dalla
     * `2.80` sono due icone accanto alle due parole (§ [ShapeRow]).
     */
    icon: ImageVector? = null
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        /*
         * ⚠️⚠️ **LA SCELTA NON ERA DETTA A PAROLE, e fino alla `1.80` la segnava solo il
         * COLORE** (censimento della UI del 2026-09-05). La nota qui sopra dichiara che di
         * Material *quello che cambia è solo il rientro*, e non era vero: il riscritto aveva
         * perso anche la semantica, che l'originale porta con sé. A due tocchi di distanza,
         * nella conversione, la stessa scelta esclusiva è fatta col `FilterChip`, che lo stato
         * lo dichiara.
         * ⚠️ **La riga la mette il COMPONENTE e non il chiamante**, come il tocco della riga di
         * un interruttore: le scelte esclusive dell'editor sono sette, e sette chiamanti che
         * se la ricordano sono sette modi di dimenticarsene.
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
 * ⚠️⚠️ **È UN PEZZO SOLO PERCHÉ I DUE EDITOR MOSTRANO LA STESSA FILA**: dalla `2.31` il modulo
 * Ritaglio dell'editor completo chiama lo stesso ritaglio dell'editor semplice, e dalla `2.32` anche
 * gli stessi formati. Scritta due volte, la fila divergerebbe al primo ritocco, e a vederlo sarebbe
 * lui, che i due editor li apre dalla stessa immagine.
 *
 * ⚠️⚠️ **PRENDONO TUTTA LA LARGHEZZA, ed è una richiesta** (utente, 2026-09-01: *fa' in modo
 * che le 5 proporzioni occupino tutto lo spazio orizzontale della bottomsheet: è più elegante e
 * ordinato*). Prima la fila scorreva di lato e finiva dove finivano le parole, lasciando un
 * vuoto a destra.
 *
 * ⚠️⚠️ **DALLA `2.88` LE RIGHE DELL'EDITOR SEMPLICE SONO 'LIBERO E I NUMERI' E 'ORIGINALE E I
 * VERSI', ED È SUA RICHIESTA** (campo libero del giro della `2.87`, con una schermata: *Riga 1:
 * Libero, 1:1, 4:3, 3:2, 16:9. Riga 2: Originale, Verticale, Orizzontale*). Fino alla `2.87` là
 * c'era una fila sola da sei celle, dove 'Originale' si troncava già in italiano, e sotto una riga
 * con i soli due versi: le righe erano già due, e a cambiare è che cosa porta ognuna.
 * - ⚠️ **'Originale' finisce accanto ai due versi che spegne** (§ [Shape.fromImage]): quando è
 *   scelta, la riga di sotto dice da sé che il verso non c'entra.
 * - ⚠️ **I versi restano scritti, come li ha elencati lui**: nell'editor completo sono icone dalla
 *   `2.80`, perché là servivano a togliere una terza riga; qui lo spazio c'è, e tre celle uguali
 *   su 360dp ne lasciano 88 per la parola, contro i 76 che chiede la più larga delle ventotto
 *   lingue (il tedesco *Hochformat*, Roboto Medium col corpo pieno).
 * - ⚠️ **L'altezza della scheda non cresce**: due punti in meno di prima, perché fra le due righe
 *   c'è [SHAPE_GAP] invece degli otto punti della riga dei versi.
 *
 * ⚠️⚠️ **E DALLA `2.89` LE CELLE DELLA PRIMA RIGA SI MISURANO, E FINO ALLA `2.88` 'LIBERO' AVEVA
 * UN PESO FISSO** (1,35 volte un numero). Col 5:4 le celle sono sei (sua istruzione, § [Shape]), e
 * con quel peso alla parola sarebbero rimasti 48 punti di testo su uno schermo da 360dp, contro i
 * 59,6 che chiede il 'Libero' russo. Adesso a 'Libero' spetta la larghezza vera della sua parola
 * nella lingua del telefono, a ogni numero quella del più largo di loro, e l'avanzo si divide in
 * proporzione: è la resa della fila dei modi del ridimensionamento (`ModeRow`).
 * - ⚠️⚠️ **SE LA RIGA NON CI STA, SI STRINGE LA PAROLA E NON I NUMERI** ([shapeCell]): '16:9'
 *   troncato si legge come un'altra proporzione, mentre una parola con l'ellissi si legge ancora.
 *   Col solo peso misurato, in una riga troppo corta ogni cella perderebbe la sua parte.
 * - **I numeri misurati**, Roboto Medium con la spaziatura delle lettere di Material, su una fila
 *   larga 312 punti (360 meno i due [STAGE_SIDE]) di cui cinque distacchi lasciano 282: in italiano
 *   la riga ne chiede 237, in russo 260. Col corpo pieno il russo ne chiederebbe 278, cioè quattro
 *   di margine: per questo il corpo resta un gradino sotto.
 * - ⚠️ **Il tamil ne chiede 283, e va dichiarato**: il suo 'Libero' vale 83 punti col Noto Sans
 *   Tamil, quindi là la parola perde un punto e prende l'ellissi. Fino alla `2.88` gliene
 *   mancavano ventidue. ⚠️ **Il numero vero non è misurato**: Android usa la variante UI di quel
 *   carattere, che in sessione non c'è.
 * - ⚠️ **Fino alla `2.88` qui c'era scritto che la cella di 'Libero' lasciava 64,7 punti, ed era
 *   sbagliato**: era il conto della fila dell'editor completo, larga 328 punti, e su questa ne
 *   lasciava 60,7. Il russo entrava lo stesso, con un punto di margine invece di cinque.
 *
 * ⚠️ **Chi legge una nota vecchia sappia che la fila unica non c'è più**: dalla `2.34` alla `2.87`
 * l'editor semplice portava le sei forme in una fila sola col corpo ridotto (sua risposta `una` a
 * `d-crop-righe`, *preferisco lo spazio per l'immagine*), e dalla `2.35` l'editor completo le
 * mandava a capo perché là lo spazio avanzava.
 *
 * ⚠️⚠️ **E NELL'EDITOR COMPLETO LE RIGHE SONO 'LE PAROLE' E 'I NUMERI', DALLA `2.80`, ED È IL SUO
 * PUNTO `crop-giu`** (riscontro del giro dalla `2.75` alla `2.77`, col mockup: *le proporzioni
 * numeriche tutte in una riga* e *'Orizzontale' e 'Verticale' diventano icone a destra di
 * 'Originale'*). Le due parole prendono quello che avanza accanto alle due celle dei versi, che
 * sono larghe [LAY_CELL] perché un'icona non ha bisogno di più.
 * - ⚠️ **L'ordine dei versi è quello di [Lay]** in tutti e due gli editor: girarlo sarebbe un
 *   cambiamento non chiesto su una scelta che lui ha davanti da venti versioni.
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
     * In quale editor vive la fila, cioè quale delle due disposizioni: vedi la nota in testa.
     *
     * ⚠️ **Non ha un valore di serie**: le due disposizioni hanno corpi e celle diversi, e un
     * chiamante nuovo che la ereditasse per omissione mostrerebbe la fila dell'altro editor.
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
            // ⚠️ Spenti con 'Originale', dalla `2.87`: vedi [Shape.fromImage].
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
             * ⚠️ **Le celle si misurano, dalla `2.89`**: il perché vive nella nota in testa, la
             * regola su [shapeCell]. La parola prende quello che i numeri lasciano, quindi la riga
             * finisce esattamente sul bordo senza che nessuno sommi niente.
             * ⚠️ **I numeri sono larghi uguali**, cioè quanto il più largo di loro: sono gettoni
             * dello stesso genere, e '1:1' più stretto di '16:9' romperebbe la griglia della riga.
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
    /** L'editor semplice, dalla `2.88`: 'Libero' e i numeri sopra, 'Originale' e i versi sotto. */
    SIMPLE,

    /** L'editor completo, dalla `2.80`: le due parole e i versi a icona sopra, i numeri sotto. */
    FULL
}

/** Il distacco fra due forme, uguale fra due celle vicine e fra le due righe. */
private val SHAPE_GAP = 6.dp

/**
 * Quanto è larga la cella di un verso del ritaglio, dalla `2.80`: vedi [ShapeRow].
 *
 * ⚠️ **È il bersaglio di Material meno l'aria**: una cella che porta un'icona sola non ha niente
 * da allargare, e i punti che non prende vanno alle due parole che le vivono accanto.
 */
private val LAY_CELL = 44.dp

/** Quanto è grande un'icona dentro un chip: la misura di Material per un'icona in una riga. */
private val CHIP_ICON = 18.dp

/**
 * Quanto è larga la cella di un numero nella prima riga dell'editor semplice, dalla `2.89`: vedi
 * [ShapeRow]. La cella di 'Libero' prende quello che resta.
 *
 * [word] e [number] sono quanto chiedono le due celle, testo più rientri, e [room] è la riga meno
 * i distacchi.
 * - **Se la riga ci sta**, l'avanzo si divide in proporzione a quello che ognuno chiede, come nella
 *   fila dei modi del ridimensionamento: chi ha una parola lunga riceve di più.
 * - ⚠️⚠️ **Se non ci sta, i numeri tengono la loro misura e a stringersi è la parola**: '16:9'
 *   troncato si legge come un'altra proporzione, mentre una parola con l'ellissi si legge ancora.
 * - ⚠️ **Se non ci stanno nemmeno i numeri**, le celle si dividono la riga in parti uguali: è una
 *   riga più stretta di quella di qualunque telefono, e là nessuna ripartizione salva il testo.
 *
 * ⚠️ **È una funzione a sé per il banco**: là i testi misurano meno che sul telefono, quindi la
 * regola si prova chiamandola con le misure vere invece di montare la fila.
 */
internal fun shapeCell(word: Dp, number: Dp, numbers: Int, room: Dp): Dp {
    val need = word + number * numbers
    return when {
        need <= room -> number * (room / need)
        number * numbers < room -> number
        else -> room / (numbers + 1)
    }
}

/** Le misure del chip scritto in casa: quelle di Material, tranne il rientro. */
private val CHIP_TALL = 32.dp
private val CHIP_ROUND = 8.dp
private val CHIP_EDGE = 1.dp
private val CHIP_PAD = 6.dp

/**
 * Le due opacità di un chip spento, dalla `2.88`: vedi [SheetChip].
 *
 * ⚠️ **Sono quelle di Material** (`FilterChipTokens.DisabledLabelTextOpacity` e
 * `FlatDisabledSelectedContainerOpacity`, lette nel bytecode di material3 1.5.0-alpha26): due
 * numeri scelti qui darebbero un chip spento diverso da tutti gli altri comandi spenti dell'app,
 * che quei numeri li prendono da Material.
 */
private const val CHIP_OFF_INK = 0.38f
private const val CHIP_OFF_FILL = 0.12f

/**
 * Quante colonne hanno le due file di tasti del pannello: **quattro tutte e due**.
 *
 * ⚠️ Il numero è uno solo apposta: due file con un numero diverso di celle stanno su due
 * griglie diverse, e l'ultima icona di sotto finirebbe spostata rispetto a quella di sopra.
 */
internal const val SHEET_KEYS = 4


/**
 * Il respiro fra il bordo di sopra della scheda e la prima fila di chip. Vedi la sua nota.
 *
 * ⚠️ **Condiviso con l'editor completo, come le altre tre misure di questa schermata**: le due
 * schermate hanno la stessa testata, lo stesso palco e la stessa scheda in fondo, e un secondo
 * numero scritto là sarebbe la prima cosa a divergere. Vedi `AdvancedEditorScreen.kt`.
 */
internal val SHEET_TOP = 16.dp

/**
 * Il rientro che un `TextButton` di Material si porta dentro, per lato.
 *
 * ⚠️ **Letto sul bytecode di material3 1.5.0-alpha26** (`ButtonDefaults`,
 * `TextButtonHorizontalPadding`): serve a far cadere il testo di 'Salva'
 * sulla linea del resto della schermata, e un numero sbagliato lo sposterebbe in silenzio.
 * Vedi la nota sulla testata, in [EditorScreen].
 */
internal val TEXT_BUTTON_PAD = 12.dp

/** Come sta la selezione: in piedi o coricata. Vedi i due tasti in [EditorScreen]. */
internal enum class Lay(@StringRes val label: Int) {
    TALL(R.string.editor_tall),
    WIDE(R.string.editor_wide)
}

/**
 * Con che forma si ritaglia, **senza** dire da che parte sta.
 *
 * ⚠️⚠️ **UNA VOCE PER PROPORZIONE E NON UNA PER VERSO, ed è la struttura che la richiesta
 * dell'utente impone**: '2:3' e '3:2' non sono due scelte diverse, sono la stessa forma letta nei due
 * versi, e chi le tiene separate deve poi tenere d'accordo due elenchi ogni volta che
 * l'orientamento cambia. Qui il verso lo dà [Lay], e la forma resta selezionata mentre gli si
 * gira intorno.
 * ⚠️ **Il valore è larghezza diviso altezza in verticale**, e in orizzontale è il suo
 * reciproco: un numero solo per forma, e l'inversione è una divisione. L'unica eccezione è una
 * forma [fromImage], il cui valore è il rapporto dell'immagine com'è, e il verso non lo tocca.
 * ⚠️ **`null` è 'libero'**: con un numero anche per quello servirebbe un caso speciale in
 * ogni conto, mentre così il caso speciale è uno solo e sta qui.
 * ⚠️ **Le etichette NON sono risorse quando sono numeri**: '16:9' si scrive uguale in tutte le
 * lingue, e metterlo in ventotto file vorrebbe dire ventotto occasioni di scriverlo storto
 * per zero traduzioni. Le due forme che si dicono a parole portano invece la loro, in [word].
 *
 * ⚠️⚠️ **'ORIGINALE' È ARRIVATA CON LA `2.32`, ED È SUA RICHIESTA** (2026-09-13: *tra i
 * vincoli di proporzione dev'esserci anche 'Originale', ma scelta di default resta 'Libera'*).
 * È la sola forma il cui rapporto **non è un numero scritto qui**: lo porta l'immagine, quindi
 * il valore arriva dal `frame` che si passa a [value] e a [fit].
 * ⚠️ **Quindi il rapporto è una funzione e non una costante, per tutte le forme**: un campo
 * `Float?` più un booleano 'questa lo prende dall'immagine' direbbe la stessa cosa in due
 * pezzi, e il conto finirebbe in chi legge invece che qui.
 * ⚠️ **Il booleano che c'è dalla `2.87` ([fromImage]) dice un'altra cosa**: non da dove viene il
 * rapporto, ma se il verso lo gira. E ha due lettori, [value] e i due gettoni del verso, che dal
 * conto non potrebbero ricavarlo.
 *
 * ⚠️⚠️ **E DALLA `2.87` 'ORIGINALE' È L'IMMAGINE INTERA IN QUALUNQUE VERSO, ED È LA SUA RISPOSTA
 * `intera` A `d-originale-verso`** (giro della `2.85` e della `2.86`, e in chat: *'Originale' che
 * si adatta anche al verso della rotazione attuale*). Fino alla `2.86` seguiva il verso acceso come
 * le quattro proporzioni, quindi su un'immagine larga la cornice veniva alta in due casi: toccando
 * 'Verticale', e girando l'immagine di un quarto, perché il verso resta quello di partenza.
 * ⚠️ **Adesso ignora i due gettoni del verso** ([fromImage]): prende l'immagine com'è in quel
 * momento, cioè già posata, e [fit] con un rapporto uguale al frame non trova niente da togliere.
 * Il verso resta per le proporzioni scritte qui ([numeric]).
 * ⚠️ **E mentre è scelta i due gettoni si spengono**, perché con lei non governano niente: un
 * tocco che non cambia la cornice si leggerebbe come un comando rotto.
 *
 * ⚠️⚠️ **LE PROPORZIONI SONO IN ORDINE CRESCENTE, DALLA `2.88`, ED È SUA ISTRUZIONE** (campo
 * libero del giro della `2.87`: *Riga 1: Libero, 1:1, 4:3, 3:2, 16:9*, e in chat, alla domanda se
 * valesse per tutti e due gli editor: *Sì, in tutti e due*). Fino alla `2.87` il 3:2 veniva prima
 * del 4:3, cioè l'ordine non era né crescente né decrescente.
 * - ⚠️⚠️ **E DALLA `2.89` C'È ANCHE IL 5:4, FRA 1:1 E 4:3, ED È SUA ISTRUZIONE** (2026-09-26:
 *   *Metti questi pulsanti proporzione, ordine: 1:1, 5:4, 4:3, 3:2, 16:9*). L'ordine resta
 *   crescente, perché 1,25 cade fra 1 e 1,33, e vale nei due editor come quello di prima.
 * - ⚠️ **L'ordine di dichiarazione è l'ordine dei gettoni**, nei due editor, e nessun archivio lo
 *   legge: l'editor completo tiene la forma scelta come indice nel solo stato salvato della
 *   schermata (vedi `Gaze`), che vale per la rotazione e per la morte del processo.
 *   ⚠️ **Una voce nuova in mezzo sposta l'indice di quelle che vengono dopo**, e il solo caso in
 *   cui si vede è un editor aperto mentre l'app si aggiorna: là il lavoro riparte comunque da
 *   capo, perché non è salvato, e a cambiare è al più il gettone acceso.
 */
internal enum class Shape(
    private val tall: (Float) -> Float?,
    @StringRes val word: Int?,
    private val up: String?,
    private val flat: String?,
    /**
     * Se il rapporto lo porta l'immagine com'è, e i due gettoni del verso non c'entrano: vale per
     * 'Originale' soltanto, dalla `2.87`. Vedi la nota in testa.
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
     * ⚠️ **Dice le forme che una rotazione deve rifare nel verso scelto**, dalla `2.88`: le altre
     * due una rotazione le porta con sé senza cambiare niente di quello che dicono (vedi
     * `posedLook`, nell'editor completo).
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

/**
 * Da che parte sta una fotografia. ⚠️ Il quadrato conta come verticale: lo chiede l'utente.
 *
 * ⚠️ **La legge anche l'editor completo, dalla `2.85`**: fino alla `2.84` là il verso partiva
 * sempre da 'Verticale', cioè la regola valeva in uno solo dei due editor.
 */
internal fun startLay(base: Bitmap?): Lay =
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
    /** Il filo che contorna le maniglie, e che è quello che le fa vedere. Vedi [bracket]. */
    halo: Float,
    /** Il bordo del ritaglio dentro la lente, che è la mira. Vedi [lens]. */
    lensEdge: Float,
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
                        going = cropBox(now, frame)
                        // ⚠️ **Le maniglie di lato ci sono con la sola forma libera**, dalla
                        // `2.40`: il perché vive su [grabbed], e il disegno dice la stessa cosa.
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
 * ⚠️⚠️ **VIVE QUI E LA LEGGONO IN DUE, DALLA `2.31`**: il palco di questo editor e quello
 * dell'editor completo, che dal modulo Ritaglio disegna lo stesso corredo sopra la sua anteprima
 * sviluppata. Scritta due volte, la seconda copia sarebbe quella che diverge al primo ritocco, e
 * il ritaglio si vedrebbe in due modi a un tocco di distanza.
 * ⚠️ **Disegna e basta**: il gesto che muove le maniglie resta di chi la chiama, perché i due
 * palchi lo ricevono in due modi (là un rilevatore suo, qui dentro il gesto unico del palco).
 *
 * ⚠️⚠️ **QUATTRO SQUADRETTE E NON QUATTRO QUADRATINI** (richiesta dell'utente, 2026-08-31:
 * *manopole angolari più grandi e visibili*). Un quadratino centrato sull'angolo dice 'qui c'è un
 * punto'; una squadretta appoggiata ai due lati dice **quali due lati** quel punto muove, che è
 * l'informazione che serve mentre si tira.
 * ⚠️ **Il braccio si accorcia sui ritagli piccoli**: a lato pieno resterebbe più lungo di metà
 * rettangolo, e le due squadrette opposte si toccherebbero.
 * ⚠️ **I terzi si disegnano sempre e non solo mentre si trascina**: sono la ragione per cui un
 * ritaglio viene dritto, e comparendo solo al tocco arriverebbero dopo che la decisione è presa.
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
     * ⚠️⚠️ **LE QUATTRO BARRETTE DI LATO, DALLA `2.40`**: dicono che quel bordo si muove da solo,
     * ed è il gesto che lui ha chiesto. ⚠️ **Il segno è diverso da quello degli angoli di
     * proposito**: una squadretta appoggiata a due lati dice *muovo tutti e due*, un tratto dritto
     * dice *muovo questo*, e due segni uguali per due gesti diversi sarebbero una promessa falsa.
     * ⚠️ **Si accorciano come i bracci**: su un rettangolo stretto un tratto lungo quanto il
     * braccio toccherebbe le due squadrette del suo lato.
     */
    if (!sides) return
    val half = min(arm, min(r.width, r.height) / 4f)
    bar(Offset(r.left, r.center.y), false, -1, half, thick, CROP_GRIP, halo, line)
    bar(Offset(r.right, r.center.y), false, 1, half, thick, CROP_GRIP, halo, line)
    bar(Offset(r.center.x, r.top), true, -1, half, thick, CROP_GRIP, halo, line)
    bar(Offset(r.center.x, r.bottom), true, 1, half, thick, CROP_GRIP, halo, line)
}

/**
 * Una maniglia di lato: un tratto dritto appoggiato FUORI dal rettangolo, centrato su [at].
 *
 * ⚠️⚠️ **FUORI COME LE SQUADRETTE, E PER LA STESSA RAGIONE MISURATA** (vedi [bracket]): dentro
 * appoggerebbe sull'immagine non velata, dove su una schermata bianca il bianco su bianco sparisce;
 * fuori appoggia sul velo, che è il fondo su cui l'accento e il suo filo si vedono sempre.
 * ⚠️ **L'alone è lo stesso tratto più grosso disegnato prima**, come nella squadretta: un contorno
 * di spessore uniforme, capi compresi, senza nessuna cucitura da nascondere.
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
 * ⚠️⚠️ **SOLO GLI ANGOLI, e lo spostamento del rettangolo intero NON ne ha uno**: muovendolo
 * da dentro i punti da mirare sono **quattro**, e sceglierne uno vorrebbe dire ingrandire un
 * angolo qualunque mentre si guarda l'inquadratura tutta insieme. La domanda dell'utente era
 * *su quale pixel sto rilasciando il rettangolo di selezione*, e quel pixel esiste solo
 * mentre si tira un angolo.
 */
internal fun eyeOf(held: Grab, r: Rect): Offset? = when (held) {
    Grab.TOP_LEFT -> r.topLeft
    Grab.TOP_RIGHT -> r.topRight
    Grab.BOTTOM_LEFT -> r.bottomLeft
    Grab.BOTTOM_RIGHT -> r.bottomRight
    /*
     * ⚠️ **Nemmeno le quattro maniglie di lato, dalla `2.40`**: là quello che si muove è una
     * **retta** e non un punto, quindi non c'è un pixel da ingrandire. È la stessa ragione per
     * cui lo spostamento del rettangolo intero non ha una lente.
     */
    Grab.LEFT, Grab.TOP, Grab.RIGHT, Grab.BOTTOM -> null
    Grab.INSIDE, Grab.NONE -> null
}

/**
 * La lente: un cerchio in alto con dentro [eye] ingrandito [LOUPE_ZOOM] volte.
 *
 * ⚠️⚠️ **DALLA `2.88` LA USANO I DUE EDITOR, ED È SUA RICHIESTA** (campo libero del giro della
 * `2.87`: *voglio nell'editor avanzato la stessa lente d'ingrandimento per il ritaglio che è già
 * presente nell'editor semplice*). Per questo quello che si vede dentro arriva da [picture], che
 * disegna nello spazio del palco: qui è l'anteprima, nell'editor completo è l'immagine sviluppata
 * col suo pennello. Il cerchio, il posto, l'ingrandimento, la mira e l'anello restano scritti una
 * volta sola, e una seconda lente sarebbe la prima cosa a divergere.
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
 * ⚠️ **La scelta la fa chi disegna dentro**, cioè [picture]: l'editor completo la fa col filtro a
 * pixel interi del suo pennello, che è lo stesso ragionamento scritto per un'immagine sviluppata.
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
internal fun DrawScope.lens(
    eye: Offset,
    r: Rect,
    side: Float,
    edge: Float,
    thick: Float,
    lensEdge: Float,
    line: Color,
    /** Quello che si vede dentro, disegnato nello spazio del palco: la lente lo ingrandisce. */
    picture: DrawScope.() -> Unit
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
            picture()
            /*
             * ⚠️⚠️ **LA MIRA DENTRO LA LENTE SI MISURA IN PUNTI E NON IN PIXEL, dalla 1.49**
             * (riscontro dell'utente, 2026-09-04: *il crocino di riferimento nel tondo
             * ingrandito funziona benissimo, ma è troppo sottile, credo sia un singolo pixel:
             * falla spessa 2px*). Prima era [EDGE_PX], cioè due pixel **fisici**: su uno
             * schermo a tre volte la densità sono due terzi di punto, ed è la ragione per cui
             * si vedeva come un capello. Adesso è [LENS_EDGE], che vale due punti su
             * qualunque telefono.
             * ⚠️ **Il bordo del ritaglio fuori dalla lente NON cambia**, e resta [EDGE_PX]:
             * là il filo copre l'immagine che si sta guardando e deve coprirne il meno
             * possibile, mentre qui galleggia su un ingrandimento dove lo spazio c'è.
             * ⚠️ **La divisione per [LOUPE_ZOOM] resta**, e non è di troppo: questo disegno
             * sta dentro la trasformazione che ingrandisce, quindi uno spessore scritto per
             * intero verrebbe reso quattro volte più grosso di quello chiesto.
             */
            drawRect(
                color = line.copy(alpha = 0.9f),
                topLeft = r.topLeft,
                size = r.size,
                style = Stroke(width = lensEdge / LOUPE_ZOOM)
            )
        }
    }
    drawCircle(color = line, radius = radius, center = centre, style = Stroke(width = thick))
}

/**
 * Quale presa ha preso il dito.
 *
 * ⚠️⚠️ **I QUATTRO LATI SONO NATI CON LA `2.40`, ED È SUA RICHIESTA** (2026-09-14: *Il 'Ritaglio'
 * dovrebbe avere anche delle maniglie a metà dei lati, non solo negli angoli: servirebbero a
 * trascinare solo il lato, senza modificare l'altra dimensione*). Un angolo muove due bordi, un
 * lato ne muove uno: sono due gesti diversi, e fino alla `2.39` il secondo si poteva solo imitare
 * tirando un angolo e rimettendo a posto l'altro bordo.
 */
internal enum class Grab { NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, LEFT, TOP, RIGHT, BOTTOM, INSIDE }

/**
 * Una squadretta d'angolo: due bracci arrotondati appoggiati FUORI dal rettangolo.
 *
 * ⚠️⚠️ **FUORI E NON DENTRO, dalla 1.49, e il difetto che si toglie era totale** (riscontro
 * dell'utente, 2026-09-04: *le maniglie di trascinamento spesso non si vedono, renderizzale
 * ESTERNAMENTE al riquadro*). Dentro, la squadretta appoggia sull'immagine **non** velata:
 * su una schermata bianca il bianco su bianco misura **1,00**, cioè spariva del tutto. Fuori
 * appoggia sul velo, dove la stessa immagine bianca scende a `#737373` e il contrasto sale a
 * 4,74. ⚠️ **La nota di prima diceva il contrario e aveva il suo perché**, che qui va
 * archiviato: 'dentro' serviva a non coprire un pezzo di immagine fuori dal ritaglio, ma
 * quel pezzo è già coperto dal velo, mentre il bordo che si sta guardando restava senza
 * maniglia visibile. Coprire due punti di velo costa meno che perdere la presa.
 *
 * ⚠️⚠️ **DUE COLORI E NON UNO: [ink] è l'accento, [halo] è il filo che lo salva.** Nessuna
 * variante di accento del tema, da sola, basta su un fondo qualunque: misurato sul velo di
 * un'immagine bianca, l'accento `#43B59E` sta a **1,88** e il mint chiaro `#4FD9BE` a
 * **2,71**, cioè sotto il 3:1 che si chiede a una grafica. Col filo bianco intorno il peggio
 * dei casi diventa **2,85** sul grigio chiaro del palco e **4,74** sul velo bianco, perché a
 * separare è sempre uno dei due: dove sparisce l'accento si vede il filo, e viceversa.
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
     * ⚠️⚠️ **UN TRACCIATO SOLO E NON DUE RETTANGOLI, dalla 1.53, e il difetto era proprio
     * quello** (riscontro dell'utente, giro della `1.51`: *adesso le maniglie sono due tratti
     * colorati separati, brutto, devi disegnare delle manopole angolari a forma di L
     * simmetrica*, col mockup allegato). Fino alla `1.52` la L era fatta di due
     * `drawRoundRect`, ognuno coi capi **tondi** per tutta la sua lunghezza: al giunto si
     * incontravano due semicerchi, quindi l'angolo si leggeva come la fine di un tratto e
     * l'inizio di un altro invece che come una piega. Non era un difetto di misura, e nessun
     * numero lo avrebbe corretto: era la forma sbagliata.
     * ⚠️ **La piega la fa `StrokeJoin.Round` e i capi liberi `StrokeCap.Round`**, che è
     * esattamente la distinzione che serviva: il tondo va **ai due capi** della L, e
     * all'angolo va una piega tonda, che è un'altra cosa. Con due rettangoli quella
     * distinzione non si può nemmeno esprimere.
     * ⚠️ **Il tracciato corre a MEZZO SPESSORE fuori dal riquadro**, così l'inchiostro sta
     * tutto fuori e il bordo interno della L tocca esattamente l'angolo dell'immagine: è la
     * richiesta della `1.49` (*renderizzale ESTERNAMENTE al riquadro*) e regge invariata.
     * ⚠️ **L'alone è lo STESSO tracciato più grosso, disegnato prima**: così contorna la L
     * intera di uno spessore uniforme, capi e piega compresi, e non ha nessuna cucitura da
     * nascondere. Prima erano due aloni gonfiati che si sovrapponevano.
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
 * ⚠️⚠️ **I QUATTRO LATI CI SONO SOLO CON LA FORMA LIBERA, DALLA `2.40`, ED È LA SUA FRASE LETTA
 * ALLA LETTERA** (*servirebbero a trascinare solo il lato, senza modificare l'altra dimensione*):
 * con un rapporto forzato quella promessa non si può mantenere, perché muovere un lato cambia
 * l'altro per definizione. Quindi là i lati non si prendono, e il disegno lo dice non mostrandoli.
 * [sides] lo dichiara il chiamante, che è quello che sa se una forma è scelta.
 */
internal fun grabbed(at: Offset, r: Rect, grip: Float, sides: Boolean = true): Grab {
    val angoli = listOf(
        Grab.TOP_LEFT to Offset(r.left, r.top),
        Grab.TOP_RIGHT to Offset(r.right, r.top),
        Grab.BOTTOM_LEFT to Offset(r.left, r.bottom),
        Grab.BOTTOM_RIGHT to Offset(r.right, r.bottom)
    )
    /*
     * ⚠️ **I lati entrano nello stesso confronto degli angoli e non in un secondo giro**: su un
     * rettangolo piccolo un angolo e il mezzo del lato accanto cadono tutti e due nella presa, e
     * con due passate vincerebbe sempre quella scritta per prima invece del punto più vicino al
     * dito.
     */
    val lati = if (!sides) emptyList() else listOf(
        Grab.LEFT to Offset(r.left, r.center.y),
        Grab.RIGHT to Offset(r.right, r.center.y),
        Grab.TOP to Offset(r.center.x, r.top),
        Grab.BOTTOM to Offset(r.center.x, r.bottom)
    )
    val near = (angoli + lati).minByOrNull { (_, corner) -> (corner - at).getDistance() }
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
 * Come `coerceIn`, ma con un intervallo che non può essere vuoto: se il massimo cade sotto il
 * minimo vince il minimo.
 *
 * ⚠️ **Esiste perché `coerceIn` con un intervallo rovesciato LANCIA**, e i limiti di un rettangolo
 * di ritaglio si rovesciano da soli quando il riquadro è più piccolo del lato minimo, o quando il
 * dito porta una squadretta oltre il bordo opposto. Vedi la nota su [dragged].
 */
private fun within(v: Float, lo: Float, hi: Float): Float = v.coerceIn(lo, max(lo, hi))

/**
 * Il rettangolo dopo lo spostamento del dito.
 *
 * ⚠️⚠️ **CON UNA PROPORZIONE BLOCCATA SI MUOVE UN LATO E L'ALTRO SEGUE**, e l'angolo opposto
 * sta fermo: è l'unico modo in cui un ritaglio 16:9 resta 16:9 mentre lo si tira. Lasciando
 * liberi tutti e due i lati la proporzione si perderebbe al primo movimento obliquo.
 */
internal fun dragged(
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
    /*
     * ⚠️⚠️ **IL LATO MINIMO NON PUÒ SUPERARE IL RIQUADRO, O IL GESTO FA CADERE L'APP**: con
     * un'immagine molto allungata, o su un palco corto, il riquadro disegnato è più basso del lato
     * minimo, e allora `frame.top .. bottom - small` è un intervallo **vuoto**, cioè un
     * `IllegalArgumentException` dentro il dito che sta tirando una squadretta. Non è un caso di
     * scuola: il banco lo ha preso su un palco alto 118 pixel con un lato minimo di 122.
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
        // ⚠️ **Un lato muove il suo bordo e basta**, dalla `2.40`: è la differenza fra le due
        // famiglie di maniglie, ed è quello che ha chiesto (*senza modificare l'altra dimensione*).
        Grab.LEFT -> left += delta.x
        Grab.RIGHT -> right += delta.x
        Grab.TOP -> top += delta.y
        Grab.BOTTOM -> bottom += delta.y
        else -> Unit
    }
    /*
     * ⚠️ **E i quattro limiti passano da [within]**, che un intervallo vuoto non lo può avere: il
     * lato limitato al riquadro chiude il caso comune, ma un dito che tira una squadretta molto
     * oltre il bordo opposto porta comunque l'estremo dalla parte sbagliata.
     */
    left = within(left, frame.left, right - small)
    right = within(right, left + small, frame.right)
    top = within(top, frame.top, bottom - small)
    bottom = within(bottom, top + small, frame.bottom)

    /*
     * ⚠️ **Con una presa di lato il rapporto non si tiene, e il caso non arriva qui**: i lati
     * esistono solo con la forma libera (vedi [grabbed]), e la guardia è scritta lo stesso perché
     * un chiamante nuovo che se ne dimenticasse otterrebbe un rettangolo tirato da un'ancora che
     * non è la sua, cioè un movimento che nessuno ha chiesto.
     */
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
internal fun preview(context: Context, uri: Uri): Bitmap? =
    ImageSource.pixels(context, uri, PREVIEW)

/** Il lato lungo dell'anteprima. */
private const val PREVIEW = 1600

/** Quanto scurisce quello che il ritaglio butta via. */
internal const val VEIL = 0.55f

/** Il bordo del rettangolo, in pixel: sottile, perché copre la fotografia. */
internal const val EDGE_PX = 2f

/** Le righe dei terzi, più leggere del bordo. */
internal const val THIRD_PX = 1f

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
internal val LOUPE_SIDE = 112.dp
internal val LOUPE_EDGE = 8.dp

/** Il braccio della squadretta d'angolo, e il suo spessore. */
internal val HANDLE_ARM = 24.dp
internal val HANDLE_THICK = 4.dp

/**
 * Il filo che contorna una maniglia.
 *
 * ⚠️ **Un punto e non due**: è quello che serve a separare l'accento dal fondo, e su un
 * braccio da 4 punti un filo da 2 per lato ne farebbe una maniglia da 8 che è quasi tutta
 * contorno. Il perché il filo esista, con le misure, sta su [bracket].
 */
internal val GRIP_HALO = 1.dp

/**
 * Il bordo del ritaglio **dentro la lente**, che è la mira su cui si tira.
 *
 * ⚠️ **Due punti, chiesti dall'utente**, e in punti e non in pixel: il perché sta dentro
 * [lens], accanto al disegno.
 */
internal val LENS_EDGE = 2.dp

/**
 * Quanto lontano dall'angolo il dito lo prende ancora.
 *
 * ⚠️ **Più largo della squadretta disegnata, e non per generosità**: il dito copre quello che
 * tocca, quindi una presa grande esattamente quanto il disegno si prende solo guardando. 40dp
 * è la misura che Material dà a un bersaglio comodo.
 */
internal val GRIP = 40.dp

/**
 * Il lato più corto che un ritaglio può avere.
 *
 * ⚠️ **Separato dalla PRESA, e prima era lo stesso numero**: con un solo valore, allargare il
 * bersaglio del dito allargava anche il ritaglio minimo, cioè si perdeva la possibilità di
 * ritagliare in piccolo per guadagnare comodità. Sono due cose diverse e adesso lo sono anche
 * nel codice.
 */
internal val LEAST_SIDE = 32.dp

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
internal val STAGE_PAD = 12.dp
internal val STAGE_SIDE = 24.dp

/**
 * Chi modifica le fotografie: si sceglie la prima volta e si cambia dalle impostazioni.
 *
 * ⚠️⚠️ **UNA FINESTRA SOLA PER DUE PORTE** (richiesta dell'utente: il tasto delle
 * impostazioni dev'essere *lo stesso che si presenta al primo utilizzo dal menu*). Due
 * finestre gemelle sarebbero divergite alla prima voce aggiunta, e la promessa 'lo stesso'
 * sarebbe diventata falsa senza che nessuno se ne accorgesse.
 * ⚠️⚠️ **L'editor semplice è in CIMA e non in ordine alfabetico fra gli altri**: è l'unico
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
        modifier = Modifier.lowered(onDismiss),
        properties = loweredWindow(onDismiss),
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
                /*
                 * ⚠️⚠️ **L'EDITOR COMPLETO STA SUBITO SOTTO QUELLO SEMPLICE, e compare solo
                 * dove puo funzionare**: il conto che applica gira sulla scheda grafica con un
                 * programma scritto a mano, che nasce con Android 13. Sotto quella versione la
                 * voce **non si offre affatto**, invece di offrirla e poi dire di no: è
                 * l'istruzione dell'utente (*sotto la 13 resta l'editor di oggi*), e una voce
                 * che si puo toccare e non fa niente sarebbe peggio della sua assenza.
                 */
                if (advancedEditorAvailable()) {
                    PickRow(
                        label = stringResource(R.string.editor_full),
                        icon = null,
                        here = chosen == Editors.FULL,
                        onClick = { onPick(Editors.FULL) }
                    )
                }
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
            // ⚠️ **La voce in vigore lo dichiara**, e prima la diceva solo il fondo: in un
            // elenco di scelte esclusive il colore è il segno per chi guarda, e senza `picked`
            // non ce n'era nessuno per chi ascolta.
            .picked(here)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(PICK_ICON), contentAlignment = Alignment.Center) {
            // ⚠️ L'editor semplice non ha un'icona di sistema perché non è un'app: prende la
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

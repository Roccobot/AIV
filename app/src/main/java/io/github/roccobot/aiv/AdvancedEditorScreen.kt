package io.github.roccobot.aiv

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.Shader.TileMode
import android.graphics.Rect as PixelRect
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Deblur
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
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
 * ⚠️⚠️ **I CONFRONTI COL PRIMA SONO DUE, E SONO SUOI** (richiesta del 2026-09-11): il tocco lungo
 * **sull'immagine** mostra l'originale intero, il tocco lungo **sul nome di un cursore** mostra
 * l'immagine senza quel solo cursore. Il secondo è quello che serve mentre si lavora, perché
 * risponde alla domanda che ci si fa muovendo una manopola: *questa, da sola, che cosa sta
 * facendo?*
 * - ⚠️ **Il palco non sa fare la sottrazione, e non deve**: a costruire il valore da mostrare è la
 *   scheda, che ha in mano sia il valore vivo sia il modo di azzerare quel campo. Qui arriva un
 *   [Look] già fatto, e il palco disegna quello che riceve.
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

    /** Se il dito è premuto sull'immagine: si guarda l'originale intero. */
    var comparing by remember(uri) { mutableStateOf(false) }

    /**
     * Il confronto di un cursore solo: **come** togliere quel cursore, o `null`.
     *
     * ⚠️⚠️ **UN CAMBIAMENTO E NON UN'IMMAGINE GIÀ FATTA, DALLA `2.17`**: fotografando il [Look] al
     * momento del tocco, un confronto che per qualunque ragione restasse acceso **congelerebbe**
     * quello che si vede, e da lì in poi muovere un cursore non cambierebbe più niente, cioè gli
     * altri sembrerebbero azzerati. Applicandolo qui, a quello che si vede adesso, l'immagine
     * segue i cursori comunque, e un confronto rimasto acceso costa al massimo un cursore che non
     * si vede applicato.
     */
    var peek by remember(uri) { mutableStateOf<((Look) -> Look)?>(null) }

    /** Dove si ha lo sguardo nella scheda, e se il colore mirato è armato: vedi [Gaze]. */
    val gaze = rememberSaveable(uri, saver = Gaze.Saver) { Gaze() }

    BackHandler { onBack() }

    LaunchedEffect(uri) {
        origin = withContext(Dispatchers.IO) { preview(context, uri) }
    }

    /**
     * La lettura a pezzi del file vero, per quando si ingrandisce: `null` finché non si sa, e
     * `null` per sempre se quel file non si sa rileggere a pezzi.
     *
     * ⚠️⚠️ **SERVE PERCHÉ QUESTO EDITOR LAVORA SU UN'ANTEPRIMA, ED È LA SUA RISPOSTA `pieno` A
     * `d-dett-vedere`** (giro della `2.22`): l'immagine che si sviluppa è ridotta a 1600 pixel di
     * lato per essere immediata, quindi ingrandendo si guardano **i suoi** pixel e non quelli
     * della fotografia, e la grana che il modulo Dettaglio esiste per togliere là è già stata
     * mediata. Rileggere dal file la sola finestra inquadrata è il solo modo di vedere quello su
     * cui si sta lavorando davvero.
     * ⚠️ **Non si ridecodifica l'immagine intera**, che è esattamente quello che l'anteprima esiste
     * per evitare: a venti megapixel sarebbero ottanta megabyte fermi per tutto il tempo in cui la
     * schermata è aperta.
     */
    var full by remember(uri) { mutableStateOf<RegionSource?>(null) }
    LaunchedEffect(uri, origin) {
        val base = origin ?: return@LaunchedEffect
        if (full == null) full = RegionSource.open(context, uri, base)
    }
    /*
     * ⚠️ **Il decodificatore tiene i byte del file in memoria nativa**, quindi va chiuso quando la
     * schermata se ne va: senza, quella memoria resta presa fino al primo giro del raccoglitore.
     * La cattura in una variabile locale non è cerimonia: il lambda di `onDispose` deve chiudere
     * **quello** che l'effetto aveva in mano, e leggendo lo stato delegato prenderebbe quello di
     * adesso.
     */
    val opened = full
    DisposableEffect(opened) {
        onDispose { opened?.close() }
    }

    /**
     * Un passo compiuto entra nella storia, e taglia quello che veniva dopo.
     *
     * ⚠️⚠️ **QUELLO CHE ENTRA È [look], CIOÈ QUELLO CHE SI VEDE, E NON UN VALORE CHE ARRIVA
     * DAL CURSORE**: un cursore non sa che cosa fanno gli altri, quindi un passo costruito dal
     * suo solo valore perderebbe tutto il resto. ⚠️ **E [look] si legge qui e non si cattura**:
     * è una proprietà delegata a uno stato, quindi la lettura è sempre quella del momento in cui
     * questa funzione gira. Il perché non sia un dettaglio vive sul parametro `onSettled` di
     * [LookKnob], ed è un difetto che il banco ha preso alla prima corsa.
     */
    fun push() {
        if (look == history[at]) return
        history = history.take(at + 1) + look
        at = history.size - 1
    }

    /**
     * Il colore mirato: che cosa vuol dire aver toccato il pixel [pixel].
     *
     * ⚠️⚠️ **DALLA `2.32` IL MIRATO È DELL'HSL E BASTA, ED È LA SUA RISPOSTA `via` A
     * `d-mirato-curve`** (giro della `2.31`: *il tasto 'Mirato' resta nel solo HSL, e nelle Curve
     * si lavora sul grafico*, con la nota *si torna subito a zoomare/spostare l'immagine
     * toccandola*). Quindi quello che si indica è sempre **quale colore**, cioè una scelta fra
     * otto, e il gesto porta il dito su quella fascia.
     *
     * ⚠️⚠️ **E CON LE CURVE SE NE VA IL TRASCINAMENTO, con tutto quello che gli serviva**:
     * l'attesa di un secondo e due decimi, la vibrazione che la chiudeva, e il punto della curva
     * da muovere. Restavano in piedi per un modulo solo, e senza quel modulo erano un ramo che
     * nessun dito può più raggiungere. Chi li rivolesse li ritrova nella storia git, misure
     * comprese.
     *
     * ⚠️ **Il trascinamento del MIRINO invece resta**: serve a scegliere un altro colore senza
     * alzare il dito, ed è la richiesta della `2.25`.
     */
    fun aimStart(pixel: Int) {
        Mix.bandOf(pixel).takeIf { it >= 0 }?.let { gaze.band = it }
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
            /*
             * ⚠️⚠️ **IN TESTA C'È QUELLO CHE SI STA FACENDO, E NON QUALE DEI DUE EDITOR È, DALLA
             * `2.20`** (sua istruzione, 2026-09-12: *mentre modifico le immagini deve apparire
             * 'Modifica immagine', che è quello che sto facendo, sia che usi l'editor semplice,
             * sia che usi quello completo. L'utente deve pensare alla differenza tra i due (e alla
             * loro stessa esistenza) solo quando fa la scelta*). Quindi la stringa è la stessa
             * delle due schermate, e i nomi dei due editor restano dove la scelta si fa: il
             * selettore e l'elenco delle app.
             * ⚠️ **Con lei decade la nota della `1.49`**, che diceva *questa schermata si chiama
             * 'Editor' e non 'Modifica'*: là il ragionamento partiva dal fatto che 'Modifica' è la
             * funzione che apre anche un'app di fuori, e con due editor in casa il nome del
             * singolo è diventato la cosa che non serve sapere mentre si lavora.
             */
            Text(
                text = stringResource(R.string.editor_title),
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
                    full = full,
                    look = if (comparing) Look.NONE else peek?.invoke(look) ?: look,
                    onCompare = { comparing = it },
                    /*
                     * ⚠️⚠️ **IL MIRATO VALE SOLO NEL MODULO CHE LO SA USARE, E SI GUARDA QUI**: il
                     * tasto che lo arma compare in quello solo, ma passando a un altro modulo
                     * resterebbe armato e il palco smetterebbe di rispondere a pinza e doppio
                     * tocco senza che nessuno veda più il tasto per spegnerlo. Chiedendolo alla
                     * tabella dei moduli, quel caso non esiste.
                     * ⚠️⚠️ **ED È UNA DOMANDA SOLA DALLA `2.32`, PERCHÉ I MODI SONO TORNATI UNO**:
                     * fino alla `2.31` la stessa riga diceva anche **che gesto** fosse, perché le
                     * Curve trascinavano e l'HSL sceglieva e basta. Con le Curve fuori (sua
                     * risposta `via` a `d-mirato-curve`) resta un comportamento solo, e un enum a
                     * tre stati dichiarerebbe una possibilità che non esiste più.
                     */
                    aiming = { gaze.aiming && MODULES[gaze.module].extra == Extra.BANDS },
                    onAimStart = { aimStart(it) },
                    onAimEnd = { push() },
                    /*
                     * ⚠️ **Il ritaglio si accende dalla stessa tabella del mirato**, e per la
                     * stessa ragione: quelle squadrette vivono sul palco, quindi passando a un
                     * altro modulo resterebbero in scena a prendere il dito senza che niente in
                     * fondo allo schermo lo dica.
                     */
                    cutting = look.crop.takeIf { MODULES[gaze.module].extra == Extra.CROP },
                    keep = cropShape(gaze).value(cropLay(gaze), posedAspect(origin, look.spin)),
                    onCut = { look = look.copy(crop = it) },
                    onCutEnd = { push() },
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
            origin = origin,
            gaze = gaze,
            /*
             * ⚠️⚠️ **QUI SI LEGGE LO STATO VIVO, ED È IL PUNTO IN CUI LA CORREZIONE DELLA `2.17`
             * FUNZIONA**: quello che arriva è un cambiamento da applicare, non un'immagine già
             * fatta, quindi il punto di partenza è [look] letto **adesso**. È lo stesso motivo per
             * cui `push` legge [look] invece di riceverlo, e la ragione per esteso vive su
             * [Dial.set].
             */
            onLive = { cambia -> look = cambia(look) },
            onSettled = { push() },
            onPeek = { senza -> peek = senza },
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
 * Il palco: l'immagine con il conto applicato sopra, e lo zoom per guardarne i dettagli.
 *
 * ⚠️⚠️ **IL CONTO SI APPLICA COL PENNELLO E NON CON UN SECONDO BITMAP**: disegnare l'anteprima
 * dentro uno shader costa un solo passaggio sulla scheda grafica a ogni fotogramma, mentre
 * rigenerare una mappa di pixel a ogni movimento del cursore vorrebbe dire decine di
 * millisecondi per dito mosso, cioè un cursore che scatta.
 *
 * ⚠️ **A riposo lo shader non si mette affatto**: senza valori da applicare il programma
 * restituirebbe esattamente quello che riceve, e saltarlo è insieme più veloce e la prova che il
 * confronto col prima mostra davvero l'immagine di partenza.
 *
 * ⚠️⚠️ **LO ZOOM È DELLA `2.16` ED È UNA SUA RICHIESTA** (campo libero del giro della `2.15`:
 * *qui capita di lavorare sui dettagli, perciò credo sia necessario che si possa zoomare
 * nell'immagine che si sta editando*). ⚠️ **Ingrandisce l'ANTEPRIMA e non il file**: quello che
 * si vede è la riduzione che l'editor decodifica per lavorare in fretta, quindi oltre un certo
 * ingrandimento si vedono i suoi pixel e non quelli della fotografia. Il tetto è [ZOOM_MAX], e
 * serve proprio a fermarsi prima che l'immagine diventi un mosaico.
 *
 * ⚠️⚠️ **E DALLA `2.18` SI INGRANDISCE ANCHE A UNA MANO** (nota sulla voce `zoom-corsa` del giro
 * della `2.17`: *mi piacerebbe anche il gesto di ingrandimento a una mano: doppio tocco con
 * trascinamento al secondo (giù per ingrandire)*): il secondo tocco di un doppio tocco, invece di
 * alzarsi, resta giù e trascina. Il conto vive su [pulled] e il punto fermo è quello toccato,
 * quindi il gesto ingrandisce senza spostare: chi vuole spostare ha la panoramica.
 *
 * ⚠️⚠️ **TUTTI I GESTI VIVONO IN UN RILEVATORE SOLO, E NON È UNA SCELTA DI STILE**: il tocco
 * lungo del confronto e la pinza nascono dallo stesso dito che scende, quindi scritti in due
 * `pointerInput` si contenderebbero l'evento. Il caso peggiore non è che un gesto non parta: è
 * che il confronto si accenda **durante una pinza**, perché `waitForUpOrCancellation` risponde
 * `null` sia allo scadere del tempo sia a un evento consumato da qualcun altro, e quel `null`
 * qui vale 'il dito è fermo da mezzo secondo'. Il precedente in casa è la strisciata del
 * visualizzatore, che per la stessa ragione non ha mai funzionato fino alla `0.22`.
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
     * ⚠️⚠️ **È UNA FUNZIONE E NON UN VALORE, ED È LA STESSA PRUDENZA DELLA `2.17`**: questa riga la
     * legge il corpo di un `pointerInput`, che si ricostruisce solo quando cambiano le sue chiavi;
     * un valore catturato invecchierebbe, mentre una funzione che legge uno stato risponde sempre
     * con quello di adesso. ⚠️ **E la chiave non si tocca**: metterci il mirato annullerebbe il
     * gesto in corso ogni volta che lo si arma.
     */
    aiming: () -> Boolean,
    /** Il pixel toccato, al principio di un gesto mirato: vedi [AdvancedEditorScreen]. */
    onAimStart: (Int) -> Unit,
    /** Il gesto mirato è finito: quello che si è fatto diventa un passo della storia. */
    onAimEnd: () -> Unit,
    /**
     * Il rettangolo da tenere mentre il modulo **Ritaglio** è in scena, e `null` quando non lo è.
     *
     * ⚠️⚠️ **È IL PRIMO MODULO CHE PRENDE IL DITO SULL'IMMAGINE, DALLA `2.31`**: gli altri sei
     * mettono i loro comandi nella scheda, questo li mette **sul palco**, cioè le quattro
     * squadrette da tirare. Finché è in scena il palco fa solo quello, come col colore mirato
     * armato: pinza, panoramica, doppio tocco e confronto restano fermi.
     * ⚠️ **Nullo invece di un booleano accanto al valore**: così non esiste lo stato 'sta
     * ritagliando ma non c'è un rettangolo', che è il genere di caso che compila e non vuol dire
     * niente.
     */
    cutting: ImageEdit.Crop?,
    /**
     * Il rapporto che il rettangolo deve tenere mentre lo si tira, e `null` per la forma libera.
     *
     * ⚠️ **È larghezza diviso altezza in PIXEL**, cioè la stessa unità del rettangolo sullo
     * schermo: le frazioni del ritaglio non sono la proporzione, perché i due lati dell'immagine
     * sono diversi.
     */
    keep: Float?,
    onCut: (ImageEdit.Crop) -> Unit,
    /** Il gesto del ritaglio è finito: quello che si è fatto diventa un passo della storia. */
    onCutEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hold = stringResource(R.string.look_compare)
    /**
     * La geometria di **adesso**, per chi la legge dentro un gesto.
     *
     * ⚠️⚠️ **È LA STESSA PRUDENZA DI [aiming], DALLA `2.29`**: il corpo di un `pointerInput` si
     * ricostruisce solo quando cambiano le sue chiavi, quindi un `look` catturato porterebbe la
     * geometria di quando quel nodo è nato, e il colore mirato leggerebbe il pixel di una
     * deformazione vecchia. ⚠️ **E la chiave non si tocca**: metterci il `look` annullerebbe il
     * gesto in corso a ogni cursore mosso.
     */
    val geoNow by rememberUpdatedState(look.geo)
    /** Il rettangolo di adesso e dove scriverlo, per chi li legge dentro un gesto: vedi [geoNow]. */
    val cutNow by rememberUpdatedState(cutting)
    val cutTo by rememberUpdatedState(onCut)
    /** Il rapporto da tenere, letto dentro il gesto e non catturato: vedi [geoNow]. */
    val keepNow by rememberUpdatedState(keep)
    val airPx = with(LocalDensity.current) { CROP_AIR.toPx() }

    /**
     * L'aria da lasciare intorno all'immagine: quanta ne chiedono le squadrette, e zero quando non
     * ci sono.
     *
     * ⚠️ **È una funzione e non un valore**, per la stessa ragione di [geoNow]: la leggono il
     * disegno e i gesti, che vivono fuori dalla composizione e vogliono il valore di adesso.
     */
    fun air(): Float = if (cutNow != null) airPx else 0f
    /**
     * L'anteprima **messa in posa**, cioè quello che si guarda e si tocca, dalla `2.31`.
     *
     * ⚠️⚠️ **SI GIRA LA MAPPA DI PIXEL INVECE DI GIRARE IL DISEGNO, ED È LA SCELTA CHE TIENE IN
     * PIEDI TUTTO IL RESTO**: così il viewport, lo shader, la maglia della geometria, la lente e
     * il ritaglio continuano a lavorare su un'immagine normale, e nessuno dei loro conti deve
     * sapere che esiste una posa. Con una matrice sul pennello ognuno di quei pezzi avrebbe avuto
     * un caso in più da trattare.
     * ⚠️ **Il conto è quello di casa** (`spunBy`), lo stesso del salvataggio: una rotazione di un
     * quarto di giro è una permutazione di pixel, quindi l'anteprima non perde niente.
     * ⚠️ **Costa una copia dell'anteprima a ogni posa nuova**, che è il prezzo dichiarato: la
     * vecchia non si ricicla a mano, perché può essere ancora dentro un disegno in corso (è la
     * stessa ragione scritta sui passi dell'editor di casa).
     */
    val posed = remember(picture, look.spin) {
        picture.spunBy(look.spin.turns, look.spin.mirror)
    }
    var scale by remember(picture) { mutableFloatStateOf(1f) }
    var shift by remember(picture) { mutableStateOf(Offset.Zero) }
    /**
     * Dove il dito è sceso nel gesto mirato in corso, cioè dove va la lente: `null` a riposo.
     *
     * ⚠️⚠️ **NASCE DALLA SUA RISPOSTA A `d-mirato-hsl`, DALLA `2.24`** (giro della `2.23`: *serve un
     * selettore con zoom e anteprima dei pixel campionati. Anche per le curve, forse*): il colore
     * mirato legge il pixel **sotto il dito**, cioè sotto la cosa che lo copre, e senza un
     * ingrandimento non c'è modo di sapere quale si sta prendendo.
     * ⚠️ **Si legge nel DISEGNO e non in composizione**: un `Canvas` rilegge lo stato nella fase di
     * disegno, quindi muoverlo costa un ridisegno e nessuna ricomposizione, che è quello che serve
     * a un gesto.
     */
    var lens by remember(picture) { mutableStateOf<Offset?>(null) }
    /**
     * Il colore della fascia a cui appartiene il pixel sotto il dito, e `null` per un grigio.
     *
     * ⚠️⚠️ **DALLA `2.25`, ED È SUA RICHIESTA** (giro della `2.24`, voce `mirato-lente` non
     * approvata: *l'anello del 'mirino' deve essere più spessa e deve variare dinamicamente il
     * colore per corrispondere a uno degli 8 colori standard, in modo che si capisca all'istante su
     * cosa si agirà se ci si ferma lì*). Col mirino trascinabile la domanda *che cosa prendo* si
     * fa a ogni pixel, e la risposta deve stare sul dito invece che nella fila delle pastiglie.
     * ⚠️ **Il colore è quello del CENTRO della fascia, come le pastiglie**, e passa dalla stessa
     * funzione: due conti darebbero due verdi diversi per la stessa fascia.
     */
    var aimTint by remember(picture) { mutableStateOf<Color?>(null) }
    val lensInk = MaterialTheme.colorScheme.primary
    val lensBack = MaterialTheme.colorScheme.surface
    /** La corsa del doppio tocco, tenuta per poterla fermare appena un dito scende. */
    var ride by remember(picture) { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val wide = posed.width.toFloat() / posed.height

    /** Il pezzo di file letto a risoluzione piena, quando c'è: vedi [SharpPiece]. */
    var sharp by remember(picture) { mutableStateOf<SharpPiece?>(null) }
    /**
     * Quanto è grande il palco, misurato dal layout.
     *
     * ⚠️ **Serve solo alla richiesta del pezzo**: il disegno la misura da sé a ogni fotogramma
     * (`size`, dentro il `Canvas`), ma una richiesta vive in una coroutine, dove quel numero non
     * si può leggere. Costa una ricomposizione alla prima misura e a ogni rotazione.
     */
    var stage by remember(picture) { mutableStateOf(Size.Zero) }
    /**
     * Sale di uno ogni volta che la vista si **ferma**, ed è il via alla richiesta del pezzo.
     *
     * ⚠️⚠️ **SI CHIEDE A GESTO FINITO, E NON È UN RIPIEGO DEL DEBOUNCE**: durante una pinza la
     * vista passa per cento posizioni, e leggere dal file a ognuna vorrebbe dire decodificare
     * sessanta volte al secondo pezzi che nessuno ha ancora guardato. Finché ci si muove si vede
     * l'anteprima ingrandita, cioè quello che si vedeva prima di questa funzione.
     * ⚠️⚠️ **E DEVE ESSERE UN CONTATORE INVECE DI `scale` E `shift`**: quei due si leggono nel
     * **disegno** e non in composizione, ed è quello che tiene un gesto a costo zero (la nota vive
     * su [lens]). Metterli fra le chiavi di un effetto li porterebbe in composizione, cioè
     * ricomporrebbe il palco a ogni fotogramma di panoramica.
     */
    var resting by remember(picture) { mutableIntStateOf(0) }

    /*
     * ⚠️⚠️ **COL RITAGLIO IN SCENA L'IMMAGINE TORNA INTERA**: le quattro squadrette si tirano ai
     * bordi di quello che si vede, e con l'immagine ingrandita metà di quei bordi starebbe fuori
     * dallo schermo. È la stessa scelta dell'editor di casa, dove il palco del ritaglio non
     * ingrandisce affatto.
     */
    LaunchedEffect(cutting != null) {
        if (cutting != null) {
            scale = 1f
            shift = Offset.Zero
        }
    }

    /*
     * ⚠️⚠️ **QUI DENTRO [scale] E [shift] SI LEGGONO SENZA COSTO**: il blocco di un
     * `LaunchedEffect` gira in una coroutine, fuori dalla passata di composizione, quindi le sue
     * letture di stato non diventano dipendenze di nessuno. È la ragione per cui le chiavi sono
     * [resting] e [stage] e non i due valori che al conto servono davvero.
     */
    LaunchedEffect(picture, full, stage, resting, look.geo.idle, look.square) {
        val source = full
        if (source == null || stage.width <= 0f || stage.height <= 0f) {
            sharp = null
            return@LaunchedEffect
        }
        /*
         * ⚠️⚠️ **COL MODULO GEOMETRIA MOSSO IL PEZZO NITIDO NON SI LEGGE, E SI DICHIARA INVECE DI
         * LASCIARLO SBAGLIARE**: `sharpAsk` ricava la porzione inquadrata dal rettangolo in cui
         * l'immagine **intera** è disegnata, e con la deformazione quel rettangolo non dice più
         * dove finisce un pixel. Il pezzo si dipingerebbe al posto sbagliato, cioè un rattoppo
         * spostato sopra l'anteprima.
         * ⚠️ **Quello che si perde è l'anteprima a risoluzione piena mentre si raddrizza**, e non
         * il contrario: sotto c'è sempre l'immagine intera, quindi qui non manca niente. La strada
         * per riaverlo è far passare anche il pezzo dalla maglia, che è un lavoro a sé.
         */
        if (!look.geo.idle || !look.square) {
            sharp = null
            return@LaunchedEffect
        }
        val view = viewport(stage, wide, scale, shift, air())
        val ask = sharpAsk(
            source.width, source.height, picture.width, view, stage.width, stage.height
        )
        if (ask !is Sharpening.Read) {
            sharp = null
            return@LaunchedEffect
        }
        // Lo stesso pezzo che si ha già in mano: un gesto può finire dove era cominciato, e
        // rileggerlo costerebbe una decodifica per niente.
        if (sharp?.area == ask.area) return@LaunchedEffect
        val pixels = source.tile(ask.area, ask.sample) ?: return@LaunchedEffect
        sharp = SharpPiece(
            pixels = pixels,
            area = ask.area,
            at = RectF(
                ask.area.left.toFloat() / source.width,
                ask.area.top.toFloat() / source.height,
                ask.area.right.toFloat() / source.width,
                ask.area.bottom.toFloat() / source.height
            )
        )
    }

    /**
     * Il pennello che disegna [mappa] **col conto già applicato** dentro il rettangolo
     * [dove], col filtro lineare o con quello a pixel interi. [lato] è il lato lungo
     * dell'immagine **intera** come è disegnata adesso.
     *
     * ⚠️⚠️ **È UNA FUNZIONE DALLA `2.24` PERCHÉ I RETTANGOLI SONO PIÙ DI UNO**: il palco, la
     * lente del colore mirato, e dalla `2.27` il pezzo letto a risoluzione piena. Scritta più
     * volte, la seconda copia mostrerebbe un'immagine sviluppata in un altro modo il giorno
     * che una delle due cambia, ed è esattamente il genere di divergenza che l'editor completo
     * esiste per non avere.
     */
    fun pennello(mappa: Bitmap, dove: RectF, nitido: Boolean, lato: Float): Paint {
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
         * ⚠️⚠️ **IL FILTRO LINEARE SERVE AL DETTAGLIO, DALLA `2.22`**: quel modulo legge i
         * vicini a distanze che non cadono su un pixel intero, e senza filtro ogni campione
         * verrebbe arrotondato al pixel più vicino, cioè il vicinato si accartoccerebbe su
         * meno punti di quanti ne chiede. ⚠️ **Non basta `isFilterBitmap` del pennello**, che
         * governa il disegno e non i campioni che uno shader chiede a un altro.
         * ⚠️⚠️ **NELLA LENTE INVECE SI VOGLIONO I PIXEL INTERI, ED È IL SUO SCOPO**: là si
         * guarda **quale** pixel si sta prendendo, e il filtro lineare mescola i vicini
         * proprio nel punto in cui bisogna distinguerli.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            image.setFilterMode(
                if (nitido) BitmapShader.FILTER_MODE_NEAREST else BitmapShader.FILTER_MODE_LINEAR
            )
        }
        /*
         * ⚠️⚠️ **LA MISURA CHE SI CONSEGNA È IL LATO LUNGO DELL'IMMAGINE INTERA COME È
         * DISEGNATA, e non quello della mappa di pixel né quello del pezzo**: il Dettaglio
         * ragiona in frazioni del lato, e qui il conto gira nello spazio dello schermo. Così
         * l'ingrandimento ingrandisce anche il risultato del filtro invece di cambiarlo, e un
         * pezzo disegnato da solo riceve lo stesso raggio del tutto: consegnando la misura del
         * pezzo, il filtro cambierebbe forza mentre si sposta la panoramica.
         */
        val shader = if (look.plain) null else lookShader(image, look, lato)
        return Paint().apply {
            asFrameworkPaint().isFilterBitmap = !nitido
            asFrameworkPaint().shader = shader ?: image
        }
    }

    /*
     * ⚠️⚠️ **LE TELE SONO DUE DALLA `2.32`, E LA SECONDA PORTA LA SOLA LENTE: È QUELLO CHE LA
     * RENDE VELOCE** (riscontro del giro della `2.31`, voce `geo-lente` non approvata: *continua ad
     * essere lento*). Un `Canvas` si ridisegna quando cambia uno stato che il suo **disegno** legge,
     * e fino alla `2.31` il tondo viveva dentro il disegno del palco: ogni pixel di dito invalidava
     * la tela dell'immagine, cioè faceva rigirare tutto il conto dello sviluppo su tutta
     * l'anteprima (col Dettaglio acceso sono diciotto campioni per pixel), sessanta volte al
     * secondo. Adesso quel movimento invalida la **sola** tela di sopra, e il conto gira sull'area
     * del tondo.
     * ⚠️ **La seconda tela non prende i tocchi**: non porta nessun `pointerInput`, quindi non entra
     * nella hit-test e non può rubare niente al palco, che è la trappola scritta in questo
     * repository su `MenuGuard`.
     * ⚠️ **Il conto della vista è lo stesso in tutte e due**, cioè `viewport` con gli stessi
     * argomenti: una seconda catena di misure darebbe un tondo che inquadra un altro pezzo di
     * immagine appena l'ingrandimento o la panoramica cambiano.
     */
    Box(
        modifier = modifier
            // ⚠️ Ingrandita, l'immagine esce dal proprio riquadro: senza questa riga andrebbe a
            // finire sopra la testata e sopra la scheda dei cursori.
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
                 * La pinza, scritta una volta sola perché la raggiungono due strade: le dita che
                 * si muovono subito, e il compagno che arriva mentre il secondo tocco è ancora
                 * giù. Due copie di questo conto divergerebbero al primo ritocco.
                 */
                fun pinch(centroid: Offset, pan: Offset, zoom: Float) {
                    val next = (scale * zoom).coerceIn(1f, ZOOM_MAX)
                    // Il punto sotto le dita resta fermo: si riscrive lo spostamento intorno al
                    // centroide, invece di scalare e poi ricentrare.
                    val grown = next / scale
                    val from = centroid - middle
                    scale = next
                    shift = reined(from + (shift - from) * grown + pan, next, room, wide, air())
                }

                /**
                 * Il colore del pixel dell'immagine sotto il punto [at], o `null` se là c'è il
                 * fondo del palco invece dell'immagine.
                 *
                 * ⚠️⚠️ **IL COLORE È QUELLO DEL FILE E NON QUELLO CHE SI VEDE, e va detto**: quello
                 * che si vede è il risultato del conto, che vive sulla scheda grafica e non si può
                 * rileggere. Quindi il tono che il colore mirato prende è quello di partenza: con
                 * un'esposizione già alzata di molto, il punto nasce un po' più in basso di dove
                 * il dito lo vede.
                 * ⚠️ **Il rettangolo è quello del disegno**, e passa dalla stessa funzione: sono
                 * lo stesso conto, e il disegno lo fa già a ogni fotogramma.
                 * ⚠️⚠️ **E DALLA `2.28` SI PRENDE DAL PEZZO NITIDO QUANDO C'È**, che è la sua
                 * risposta `pieno` a `d-lente-pieno`: un pixel dell'anteprima è la media di due o
                 * tre pixel veri, quindi puntando un dettaglio fine il colore preso poteva cadere
                 * in una fascia che con quel pixel non c'entrava. ⚠️ **Va insieme al disegno della
                 * lente**, che dalla stessa versione mostra lo stesso pezzo: se uno dei due
                 * cambiasse senza l'altro, si tornerebbe a vedere un pixel e a prenderne un altro.
                 */
                fun colourAt(at: Offset): Int? {
                    val view = viewport(room, wide, scale, shift, air())
                    val l = view.left
                    val t = view.top
                    val r = view.right
                    val b = view.bottom
                    if (at.x < l || at.x > r || at.y < t || at.y > b) return null
                    if (r - l <= 0f || b - t <= 0f) return null
                    /*
                     * ⚠️⚠️ **COL MODULO GEOMETRIA IL DITO TOCCA L'IMMAGINE DEFORMATA, DALLA `2.29`,
                     * E IL COLORE VIVE PRIMA DELLA DEFORMAZIONE**: quello che si vede sotto il dito
                     * è arrivato là da un altro punto del file, quindi senza la mappatura inversa
                     * questo tasto prenderebbe il pixel di un altro posto. La funzione è
                     * `WarpPlan.back`, cioè l'inversa esatta di quella che il disegno applica: una
                     * seconda stima darebbe un colore vicino e sbagliato.
                     */
                    val geo = geoNow
                    val where = if (geo.idle) at else {
                        val p = Warp
                            .plan(geo, view.centerX(), view.centerY(), view.width(), view.height())
                            .back(at.x, at.y)
                        Offset(p[0], p[1])
                    }
                    val u = ((where.x - l) / (r - l)).coerceIn(0f, 1f)
                    val v = ((where.y - t) / (b - t)).coerceIn(0f, 1f)
                    sharp?.pixel(u, v)?.let { return it }
                    return posed.getPixel(
                        (u * (posed.width - 1)).roundToInt(),
                        (v * (posed.height - 1)).roundToInt()
                    )
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    /*
                     * ⚠️ **Un dito che scende ferma la corsa dove è arrivata**, e non c'è nessun
                     * altro posto in cui annullarla: chi tocca mentre l'immagine si sta
                     * ingrandendo vuole prendere il comando, non aspettare il suo turno.
                     */
                    ride?.cancel()
                    /*
                     * ⚠️⚠️ **COL COLORE MIRATO ARMATO IL PALCO FA SOLO QUELLO, ED È UNA MODALITÀ
                     * DICHIARATA**: pinza, panoramica, doppio tocco e confronto restano fermi
                     * finché il tasto è acceso. La via alternativa era infilare il mirato accanto
                     * agli altri quattro, e in questo rilevatore ognuno nasce dallo stesso dito
                     * che scende: un quinto gesto vorrebbe dire cinque strade da distinguere in
                     * mezzo secondo, e la prima a sbagliare sarebbe quella che si usa di più.
                     */
                    /*
                     * ⚠️⚠️ **COL MODULO RITAGLIO IN SCENA IL PALCO TIRA LE MANIGLIE E BASTA**: è
                     * la stessa modalità dichiarata del colore mirato, e per la stessa ragione,
                     * cioè che in questo rilevatore ogni gesto nasce dallo stesso dito che scende.
                     * ⚠️ **Un dito che scende lontano da una presa non fa niente**, e non è una
                     * dimenticanza: l'alternativa sarebbe spostare il rettangolo dal punto
                     * toccato, cioè farlo saltare sotto il dito.
                     */
                    val taglio = cutNow
                    if (taglio != null) {
                        val vista = viewport(room, wide, scale, shift, air())
                        val frame = Rect(vista.left, vista.top, vista.right, vista.bottom)
                        var going = cropBox(taglio, frame)
                        val presa = grabbed(down.position, going, GRIP.toPx())
                        if (presa != Grab.NONE) {
                            down.consume()
                            drag(down.id) { change ->
                                /*
                                 * ⚠️⚠️ **IL DELTA SI LEGGE PRIMA DI CONSUMARE, E IL BANCO LO HA
                                 * TROVATO ALLA PRIMA CORSA**: `positionChange()` risponde **zero**
                                 * su un evento già consumato, quindi consumando per primo il
                                 * rettangolo non si muoveva di un pixel. Il codice era valido, il
                                 * gesto partiva, la presa scattava: nessun compilatore poteva
                                 * vederlo.
                                 */
                                val passo = change.positionChange()
                                change.consume()
                                going = dragged(
                                    going,
                                    presa,
                                    passo,
                                    frame,
                                    /*
                                     * ⚠️ **Il rapporto forzato arriva dai gettoni dei formati**,
                                     * dalla `2.32`, ed è `null` quando la forma è 'Libero': è lo
                                     * stesso parametro dell'editor di casa, e la forma la sceglie
                                     * la scheda.
                                     */
                                    keepNow,
                                    LEAST_SIDE.toPx()
                                )
                                cutTo(cropFractions(going, frame))
                            }
                            onCutEnd()
                        }
                        return@awaitEachGesture
                    }
                    if (aiming()) {
                        /*
                         * ⚠️⚠️ **LA LENTE SEGUE IL DITO, DALLA `2.25`, ED È SUA ISTRUZIONE** (giro
                         * della `2.24`, voce `mirato-lente` non approvata: *dev'essere possibile
                         * trascinare il 'mirino', perché difficilmente con il dito si azzecca il
                         * punto giusto al primo colpo*). ⚠️ **Rovescia la nota della `2.24`**, che
                         * diceva il contrario (*si ancora al punto in cui il dito è sceso e non lo
                         * segue*, perché una lente che insegue mostrerebbe un colore diverso da
                         * quello preso): quell'argomento reggeva finché il pixel si prendeva
                         * all'istante del tocco, e adesso il pixel è quello sotto il dito **ora**,
                         * quindi la lente e la scelta dicono la stessa cosa a ogni fotogramma.
                         * ⚠️⚠️ **E DALLA `2.32` LA SCELTA ARRIVA SEMPRE QUANDO IL DITO SI ALZA**: fino
                         * alla `2.31` un'attesa di un secondo e due decimi armava il trascinamento
                         * per le Curve, e con loro fuori dal mirato (sua risposta `via` a
                         * `d-mirato-curve`) quel ramo non aveva più nessun dito che lo potesse
                         * raggiungere. Con lui se ne sono andati l'attesa, la vibrazione che la
                         * chiudeva e il punto della curva da muovere.
                         */
                        var preso = colourAt(down.position)
                        var dove = down.position
                        lens = dove
                        aimTint = preso?.let { tintOfPixel(it) }
                        try {
                            while (true) {
                                val punto = awaitPointerEvent().changes
                                    .firstOrNull { it.id == down.id } ?: break
                                if (!punto.pressed) break
                                val ora = punto.position
                                if ((ora - dove).getDistance() > viewConfiguration.touchSlop) {
                                    dove = ora
                                    lens = ora
                                    preso = colourAt(ora)
                                    aimTint = preso?.let { tintOfPixel(it) }
                                    punto.consume()
                                }
                            }
                            /*
                             * ⚠️⚠️ **LA FASCIA SI SCEGLIE QUANDO IL DITO SI ALZA, E NON QUANDO
                             * SCENDE**: il mirino si trascina (`2.25`), quindi quello che conta è
                             * dove il dito **ha finito**, non dove aveva cominciato.
                             */
                            preso?.let { onAimStart(it) }
                        } finally {
                            /*
                             * ⚠️⚠️ **NEL `finally`, PER LA STESSA RAGIONE DEL CONFRONTO DELLA
                             * `2.17`**: un rilevatore di gesti viene annullato quando il suo
                             * `pointerInput` cambia chiave, e un'attesa annullata non torna alla
                             * riga dopo. Senza, la lente resterebbe in scena senza un dito.
                             */
                            lens = null
                            aimTint = null
                        }
                        onAimEnd()
                        return@awaitEachGesture
                    }
                    /*
                     * Fase 1: chi vince fra il tempo, il movimento e il secondo dito. Il tempo si
                     * misura qui e non dentro il ciclo degli eventi, perché un dito **fermo** non
                     * genera nessun evento: il tocco lungo lo può vedere solo un timeout.
                     */
                    val esito = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        settled(down, viewConfiguration.touchSlop)
                    }
                    when (esito) {
                        null -> {
                            onCompare(true)
                            // Lo spegnimento in un `finally`, per la ragione misurata su
                            // `heldOrTwice`: un'attesa annullata non torna alla riga dopo, e
                            // l'immagine resterebbe l'originale senza che niente lo dica.
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
                                 * ⚠️⚠️ **IL SECONDO TOCCO È DUE GESTI, DALLA `2.18`, ED È UNA SUA
                                 * RICHIESTA** (nota sulla voce `zoom-corsa` del giro della `2.17`:
                                 * *mi piacerebbe anche il gesto di ingrandimento a una mano:
                                 * doppio tocco con trascinamento al secondo (giù per
                                 * ingrandire)*). A dire quale dei due è non c'è nessun indizio al
                                 * momento in cui il dito scende: lo dice quello che fa dopo, cioè
                                 * se si alza o se trascina, e per saperlo si aspetta.
                                 * ⚠️ **Quindi la corsa del doppio tocco parte quando il dito si
                                 * ALZA e non quando scende**, che è il solo prezzo di questo
                                 * gesto: dura quanto un tocco, e chi tocca due volte non sta
                                 * ancora guardando l'immagine.
                                 */
                                when (settled(again, viewConfiguration.touchSlop)) {
                                    Settled.MOVED -> hauled(again) { dy ->
                                        /*
                                         * ⚠️ **Il conto riparte sempre dallo stato in cui il gesto
                                         * è cominciato**, invece di comporsi un pezzo per volta:
                                         * i limiti della panoramica troncano, e uno spostamento
                                         * accumulato sul valore troncato deriverebbe mentre il
                                         * dito va avanti e indietro.
                                         */
                                        val next = pulled(fromScale, dy, span)
                                        scale = next
                                        shift = reined(
                                            anchor + (fromShift - anchor) * (next / fromScale),
                                            next, room, wide, air()
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
                                            else reined(anchor * (1f - ZOOM_TAP), ZOOM_TAP, room, wide, air())
                                        /*
                                         * ⚠️⚠️ **CI SI ARRIVA CON UN'ANIMAZIONE, DALLA `2.17`, ED È
                                         * IL SUO RISCONTRO** (giro della `2.16`, voce `luce-zoom`
                                         * approvata con una nota: *mi piacerebbe di più se al
                                         * doppio tocco l'immagine passasse da uno zoom all'altro
                                         * con un'animazione anziché con uno stacco netto*).
                                         * ⚠️ **A muoversi è un progresso solo**, e da lui si
                                         * ricavano ingrandimento e spostamento: animarli separati
                                         * vorrebbe dire due corse da tenere allineate, e una che
                                         * finisse prima dell'altra farebbe scivolare l'immagine a
                                         * ingrandimento fermo.
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
                                            // ⚠️ **La corsa finisce dopo il gesto**, quindi il
                                            // via al pezzo nitido lo dà lei: annullata da un dito
                                            // che scende, lo darà il gesto che la ferma.
                                            resting++
                                        }
                                    }
                                }
                            }
                        }
                        else -> transformed(::pinch)
                    }
                    // ⚠️ **Il via al pezzo nitido è qui e non dentro i rami**: la vista è ferma
                    // quando il gesto è finito, qualunque dei quattro fosse, e un gesto che non
                    // l'ha mossa affatto costa soltanto una richiesta che si riconosce già
                    // soddisfatta.
                    resting++
                }
            }
    ) {
        val room = size
        if (room.width <= 0f || room.height <= 0f) return@Canvas
        // Il rettangolo da disegnare: quello adattato, ingrandito attorno al centro del palco e
        // poi spostato. ⚠️ **Si scala il RETTANGOLO e non la tela**: il pennello porta uno
        // shader con la sua matrice, e una tela scalata scalerebbe anche quella, cioè
        // ingrandirebbe il conto invece dell'immagine.
        val view = viewport(room, wide, scale, shift, air())

        /** Il lato lungo dell'immagine intera come è disegnata adesso. */
        val lato = max(view.width(), view.height())

        /*
         * ⚠️⚠️ **CON LA GEOMETRIA MOSSA NON SI DISEGNA UN RETTANGOLO MA UNA MAGLIA, DALLA `2.29`**,
         * e il pennello è lo stesso: quello che cambia è **dove** finisce ogni pixel, non di che
         * colore è. La maglia la costruisce `Warp`, cioè lo stesso conto che il salvataggio applica
         * al file pieno: una fonte, due lettori.
         * ⚠️ **A geometria ferma resta il rettangolo di sempre**, e non è un'ottimizzazione: mille
         * triangoli per disegnare un rettangolo sarebbero mille occasioni di una cucitura che a
         * rettangolo non esiste.
         */
        fun stendi(mappa: Bitmap, dove: RectF, nitido: Boolean) {
            val paint = pennello(mappa, dove, nitido, lato)
            if (look.geo.idle) {
                drawIntoCanvas { tela ->
                    tela.drawRect(dove.left, dove.top, dove.right, dove.bottom, paint)
                }
                return
            }
            /*
             * ⚠️⚠️ **IL RITAGLIO SI VEDE MENTRE SI MUOVE UN CURSORE, DALLA `2.30`, ED È IL SUO
             * RISCONTRO** (giro della `2.29`, voce `geo-dritto`: *anche il ritaglio per non
             * lasciare angoli vuoti dovrebbe vedersi in tempo reale*). La scala di copertura c'era
             * già e faceva il suo lavoro, ma qui la maglia si disegnava **senza confini**: ingrandita
             * per coprire, usciva dal riquadro dell'immagine e finiva sul fondo del palco. Quindi si
             * vedeva la deformazione intera, mentre il salvataggio disegna dentro un bitmap grande
             * quanto l'originale, cioè taglia. Due immagini diverse per lo stesso conto.
             * ⚠️ **Il riquadro è quello dell'immagine e non quello del palco**: `clipToBounds` più
             * sopra ferma il disegno al palco, che è più grande, quindi da solo non bastava.
             */
            val piano = Warp.plan(
                look.geo, view.centerX(), view.centerY(), view.width(), view.height()
            )
            clipRect(dove.left, dove.top, dove.right, dove.bottom) {
                drawIntoCanvas { tela ->
                    Warp.draw(tela.nativeCanvas, dove, piano, paint.asFrameworkPaint())
                }
            }
        }

        stendi(posed, view, false)

        /*
         * ⚠️⚠️ **IL PEZZO NITIDO SI DISEGNA SOPRA L'ANTEPRIMA, NON AL SUO POSTO, DALLA `2.27`**, ed
         * è quello che rende innocua tutta questa strada: sotto c'è sempre l'immagine intera com'è
         * sempre stata, e questo è un rattoppo nitido sulla parte che si guarda. Se un giorno
         * finisse fuori posto si vedrebbe un rettangolo spostato, non un'immagine mancante, e
         * finché non arriva non manca niente.
         * ⚠️ **Il pezzo è ancorato all'IMMAGINE e non allo schermo**: quello che si tiene sono le
         * frazioni che copre, quindi il rettangolo si ricalcola qui a ogni fotogramma e il
         * rattoppo resta incollato alla fotografia mentre il dito la muove.
         */
        val fine = sharp
        if (fine != null) {
            val dove = fine.place(view)
            drawIntoCanvas { tela ->
                tela.drawRect(
                    dove.left, dove.top, dove.right, dove.bottom,
                    pennello(fine.pixels, dove, false, lato)
                )
            }
        }

        /*
         * ⚠️⚠️ **LE SQUADRETTE DEL RITAGLIO SONO QUELLE DELL'EDITOR DI CASA, DALLA `2.31`**: il
         * velo in quattro pezzi, i terzi, i quattro angoli e le loro misure vivono in
         * `EditorScreen.kt` (`cropOverlay`), e questo palco le chiama invece di ridisegnarle.
         * Due disegni dello stesso comando divergerebbero al primo ritocco, e chi lo vedrebbe per
         * primo è lui, che i due editor li apre dalla stessa immagine.
         * ⚠️ **Il riquadro è quello dell'immagine e non quello del palco**: le squadrette si
         * tirano ai bordi della fotografia, e sul fondo intorno non c'è niente da ritagliare.
         */
        val taglio = cutting
        if (taglio != null) {
            val frame = Rect(view.left, view.top, view.right, view.bottom)
            cropOverlay(
                frame,
                cropBox(taglio, frame),
                HANDLE_ARM.toPx(),
                HANDLE_THICK.toPx(),
                GRIP_HALO.toPx()
            )
        }

    }

    /*
     * ⚠️⚠️ **QUI DENTRO SI LEGGE [lens], ED È TUTTO IL PUNTO DELLA SECONDA TELA**: quel valore
     * cambia a ogni pixel di dito, quindi il disegno che si invalida è questo e non quello
     * dell'immagine. Il perché per esteso vive sul [Box] qui sopra.
     * ⚠️ **La vista si ricalcola invece di farsela passare**: i due disegni leggono [scale] e
     * [shift] nella propria passata, e un valore catturato porterebbe la geometria di un
     * fotogramma prima.
     */
    Canvas(modifier = Modifier.fillMaxSize()) {
        val room = size
        if (room.width <= 0f || room.height <= 0f) return@Canvas
        val view = viewport(room, wide, scale, shift, air())
        val lato = max(view.width(), view.height())
        val fine = sharp
        /*
         * ⚠️⚠️ **LA LENTE DEL COLORE MIRATO, DALLA `2.24`**: l'ingrandimento si costruisce scalando
         * il **rettangolo già calcolato** attorno al punto toccato e posandolo sul centro della
         * lente, che è lo stesso conto del palco con un fattore in più. Una seconda catena di
         * misure darebbe una lente che mostra un altro pezzo di immagine appena l'ingrandimento o
         * la panoramica cambiano.
         */
        val dito = lens
        if (dito != null) {
            val raggio = LENS_SIDE.toPx() / 2f
            val aria = LENS_AIR.toPx()
            /*
             * ⚠️ **Sopra il dito, e sotto solo se sopra non ci sta**: una lente disegnata dove il
             * dito è appoggiato sarebbe coperta dal dito, che è il difetto che deve togliere.
             * ⚠️ **Il centro si tiene dentro il palco in orizzontale**, o toccando vicino a un
             * fianco metà lente finirebbe fuori.
             */
            val alta = dito.y - aria - raggio * 2f >= 0f
            val centro = Offset(
                dito.x.coerceIn(raggio, (room.width - raggio).coerceAtLeast(raggio)),
                if (alta) dito.y - aria - raggio else dito.y + aria + raggio
            )
            val k = LENS_ZOOM
            /*
             * ⚠️⚠️ **LA LENTE INQUADRA IL PUNTO TOCCATO E MOSTRA L'IMMAGINE DEFORMATA, DALLA
             * `2.30`, ED È IL SUO RISCONTRO** (giro della `2.29`, voce `geo-mirato` non approvata:
             * *Il punto non è quello giusto, si vede l'immagine prima della distorsione*). La
             * `2.29` aveva risolto il problema dalla parte sbagliata: dentro il tondo disegnava
             * l'immagine **senza** la maglia e allora, per non mostrare un pixel e prenderne un
             * altro, spostava l'inquadratura sul punto sorgente. Il conto tornava e quello che si
             * vedeva era un'altra immagine, cioè la fotografia com'era prima della geometria.
             * ⚠️⚠️ **ADESSO LA MAGLIA C'È ANCHE QUI, E IL CONTO È LO STESSO SCALATO**: `Warp.plan`
             * normalizza sul rettangolo che riceve, quindi costruito su [vista] (che è il riquadro
             * dell'immagine ingrandito di [k] attorno al dito) dà **la stessa** deformazione, e il
             * punto toccato cade esattamente al centro del tondo per costruzione. Il colore
             * continua a venire da `WarpPlan.back`, e i due dicono la stessa cosa: il pixel che si
             * vede nel punto deformato è il pixel sorgente.
             */
            val vista = RectF(
                centro.x + (view.left - dito.x) * k,
                centro.y + (view.top - dito.y) * k,
                centro.x + (view.right - dito.x) * k,
                centro.y + (view.bottom - dito.y) * k
            )
            val tondo = Path().apply { addOval(Rect(centro, raggio)) }
            // ⚠️ **Il fondo si dipinge prima**: toccando vicino a un bordo dell'immagine, dentro
            // la lente resterebbe scoperto il palco, cioè l'immagine a scala uno, che si legge
            // come un secondo disegno invece che come il fuori.
            drawCircle(color = lensBack, radius = raggio, center = centro)
            /*
             * ⚠️⚠️ **DALLA `2.28` LA LENTE MOSTRA IL PEZZO LETTO DAL FILE, QUANDO C'È, ED È LA SUA
             * RISPOSTA `pieno` A `d-lente-pieno`**: quello che si vede nel mirino è esattamente il
             * pixel che si prende. ⚠️ **Le due cose vanno insieme e non si possono separare**:
             * `colourAt` legge dallo stesso pezzo con lo stesso conto (`SharpPiece.pixel`), quindi
             * disegnare qui il file e campionare di là l'anteprima, o il contrario, farebbe vedere
             * un pixel e prenderne un altro. Chi tocca una delle due guardi l'altra.
             * ⚠️ **Il pezzo si dipinge SOPRA l'anteprima anche qui**, come sul palco: copre la sola
             * finestra inquadrata, e dove non arriva resta l'immagine intera invece di un buco.
             * ⚠️ **Il filtro resta a pixel interi in tutti e due i rettangoli**: qui si guarda
             * **quale** pixel si sta prendendo, e una fusione coi vicini lo nasconderebbe proprio
             * dove va distinto.
             */
            val vetro = max(vista.width(), vista.height())
            /*
             * ⚠️ **La maglia si costruisce sul rettangolo della lente**, e il ritaglio è il suo
             * gemello di quello del palco: quello che qui si vede è quello che il salvataggio
             * scriverà, ingrandito, bordo tagliato compreso.
             * ⚠️ **Il pezzo nitido qui non c'è mai con la geometria mossa**, perché in quel caso
             * non si legge affatto (vedi la nota sul `LaunchedEffect` che lo chiede): resta un
             * rettangolo, e non serve deformarlo.
             */
            val lente = if (look.geo.idle) null else Warp.plan(
                look.geo, vista.centerX(), vista.centerY(), vista.width(), vista.height()
            )
            clipPath(tondo) {
                clipRect(vista.left, vista.top, vista.right, vista.bottom) {
                    drawIntoCanvas { tela ->
                        val paint = pennello(posed, vista, true, vetro)
                        if (lente == null) {
                            tela.drawRect(vista.left, vista.top, vista.right, vista.bottom, paint)
                        } else {
                            Warp.draw(tela.nativeCanvas, vista, lente, paint.asFrameworkPaint())
                        }
                    }
                    if (fine != null) {
                        val sotto = fine.place(vista)
                        drawIntoCanvas { tela ->
                            tela.drawRect(
                                sotto.left, sotto.top, sotto.right, sotto.bottom,
                                pennello(fine.pixels, sotto, true, vetro)
                            )
                        }
                    }
                }
            }
            // ⚠️ **Il bordo è quello di casa**: 2dp d'accento, come ogni superficie dell'app
            // (vedi `Edge.kt`), perché anche questa è una superficie che si apre sopra un'altra.
            drawCircle(
                color = lensInk,
                radius = raggio,
                center = centro,
                style = Stroke(width = LENS_EDGE.toPx())
            )
            /*
             * ⚠️⚠️ **IL MIRINO È DI DUE COLORI, E NON È UNA DECORAZIONE**: dice quale pixel si sta
             * prendendo, e deve vedersi sopra qualunque immagine. Un anello chiaro dentro uno nero
             * si distingue tanto su un cielo quanto su un'ombra, che un colore solo non fa.
             * ⚠️⚠️ **E DALLA `2.25` QUELLO DI DENTRO PORTA IL COLORE DELLA FASCIA** (sua richiesta:
             * *deve variare dinamicamente il colore per corrispondere a uno degli 8 colori
             * standard, in modo che si capisca all'istante su cosa si agirà se ci si ferma lì*).
             * Su un grigio, che non appartiene a nessuna fascia, resta bianco: dire 'rosso' di un
             * pixel senza colore sarebbe la stessa bugia che `Mix.bandOf` evita rispondendo `-1`.
             * ⚠️ **Il tratto è raddoppiato**, come ha chiesto: a un pixel di spessore il colore
             * della fascia non si distingueva da quello che c'è sotto.
             */
            val occhio = LENS_PIP.toPx()
            val tratto = LENS_PIP_LINE.toPx()
            drawCircle(
                color = Color.Black,
                radius = occhio + tratto,
                center = centro,
                style = Stroke(width = tratto)
            )
            drawCircle(
                color = aimTint ?: Color.White,
                radius = occhio,
                center = centro,
                style = Stroke(width = tratto)
            )
        }
    }
    }
}

/** Come è andata a finire l'attesa di [settled]. */
private enum class Settled { UP, MOVED, MULTI }

/**
 * Attende che il dito faccia qualcosa: si alzi, si muova oltre la soglia, o porti un compagno.
 *
 * ⚠️ **Non risponde mai se il dito resta fermo**, ed è il suo mestiere: chi la chiama la avvolge
 * in un timeout, e il timeout scaduto **è** il tocco lungo.
 */
private suspend fun AwaitPointerEventScope.settled(
    down: PointerInputChange,
    slop: Float
): Settled {
    var travel = Offset.Zero
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.count { it.pressed } > 1) return Settled.MULTI
        val mine = event.changes.firstOrNull { it.id == down.id } ?: return Settled.UP
        if (!mine.pressed) return Settled.UP
        travel += mine.positionChange()
        if (travel.getDistance() > slop) return Settled.MOVED
    }
}

/**
 * Pinza e panoramica, fino a quando l'ultimo dito si alza.
 *
 * ⚠️ **Consuma quello che usa**: qui non c'è nessun altro rilevatore da disturbare, ma un evento
 * non consumato risale ai genitori, e sopra questo palco vive lo scorrimento della schermata.
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
 * ⚠️ **Riferisce sempre la distanza dal punto di partenza e non l'ultimo passo**, così il conto
 * non accumula e il dito che torna indietro riporta l'immagine dov'era. ⚠️ **Il primo evento che
 * ha superato la soglia l'ha già letto `settled`**, e non si perde niente: quello dopo porta la
 * posizione corrente, che è tutto quello che serve.
 *
 * ⚠️ **Consuma quello che usa**, come la pinza: un evento non consumato risale ai genitori, e
 * sopra questo palco vive lo scorrimento della schermata.
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
 * ⚠️⚠️ **SI RADDOPPIA A OGNI [span], E NON CRESCE DI UN TANTO AL PIXEL**: l'ingrandimento si
 * percepisce in rapporti e non in differenze, quindi con una crescita lineare lo stesso
 * trascinamento varrebbe moltissimo vicino a uno e quasi niente vicino al tetto. Con la potenza,
 * un centimetro di dito vale sempre lo stesso raddoppio.
 *
 * ⚠️ **Giù ingrandisce**, come lo ha chiesto lui, e il verso è tutto qui: il puntatore conta
 * positivo verso il basso. Un segno di troppo darebbe un gesto rovesciato che nessun compilatore
 * vede, ed è la ragione per cui questa riga ha una prova.
 *
 * ⚠️ **Il tetto è lo stesso della pinza**: lo decide l'anteprima (vedi [ZOOM_MAX]), non il gesto
 * da cui si arriva.
 */
internal fun pulled(from: Float, dy: Float, span: Float): Float =
    (from * 2f.pow(dy / span)).coerceIn(1f, ZOOM_MAX)

/**
 * Lo spostamento [want] riportato dentro i bordi di un'immagine ingrandita di [zoom].
 *
 * ⚠️ **Un margine non esiste**: a ingrandimento uno la risposta è zero, cioè l'immagine resta
 * centrata e nessuna panoramica la può staccare dal centro.
 *
 * ⚠️⚠️ **LA LEGGONO IL GESTO E IL DISEGNO, E DALLA `2.17` SERVONO TUTTI E DUE**: il gesto la usa
 * per non accumulare uno spostamento che poi andrebbe riavvolto, il disegno per il tratto in cui
 * l'ingrandimento si muove da sé. È **pura e idempotente**, quindi applicarla due volte non
 * cambia niente.
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
 * ⚠️ **La leggono in due, il disegno e il gesto**, ed è la ragione per cui è una funzione: i
 * limiti della panoramica si contano sull'immagine adattata, quindi due conti scritti in due
 * posti darebbero una panoramica che si ferma dove l'immagine non finisce.
 *
 * ⚠️⚠️ **L'ARIA NASCE CON LA `2.32`, E SENZA DI LEI LE SQUADRETTE DEL RITAGLIO SI VEDEVANO A
 * METÀ** (riscontro del giro della `2.31`, voce `crop-modulo` non approvata, con schermata:
 * *all'avvio del modulo gli angoli di ritaglio non sono del tutto visibili*). La causa è
 * geometrica: una squadretta si disegna **fuori** dal rettangolo (`bracket`, in `EditorScreen.kt`,
 * dove il perché è misurato), e questo palco **ritaglia** al proprio riquadro; l'immagine adattata
 * tocca due bordi del palco per costruzione, quindi le maniglie di quei due lati finivano fuori e
 * il ritaglio le tagliava.
 * ⚠️ **Si rimpicciolisce l'IMMAGINE e non il palco**: dando il rientro al `Canvas`, l'immagine
 * tornerebbe a toccarne i bordi e il taglio si ripeterebbe un pixel più in là.
 * ⚠️ **Vale solo col Ritaglio in scena**: negli altri moduli non c'è niente da tirare, e l'aria
 * sarebbe spazio tolto all'immagine per niente.
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
 * ⚠️⚠️ **LA LEGGONO IN TRE, E FINO ALLA `2.27` ERANO TRE COPIE DELLE STESSE QUATTRO RIGHE**: il
 * disegno, il gesto che deve sapere che pixel c'è sotto il dito, e adesso la richiesta del pezzo
 * a risoluzione piena. Sono lo stesso conto, e un rettangolo ricostruito in un modo diverso
 * dall'altro darebbe un colore preso da un punto e una lente che ne mostra un altro.
 * ⚠️ **Lo spostamento si riporta nei bordi QUI**, cioè in tutti e tre i chiamanti insieme: la
 * funzione è pura e applicarla due volte dà lo stesso risultato, quindi chi la chiama non ha una
 * seconda riga da ricordare.
 */
private fun viewport(room: Size, wide: Float, scale: Float, shift: Offset, air: Float = 0f): RectF {
    val box = fitted(room, wide, air)
    val safe = reined(shift, scale, room, wide, air)
    val midX = room.width / 2f
    val midY = room.height / 2f
    return RectF(
        midX + (box.left - midX) * scale + safe.x,
        midY + (box.top - midY) * scale + safe.y,
        midX + (box.right - midX) * scale + safe.x,
        midY + (box.bottom - midY) * scale + safe.y
    )
}

/**
 * Un cursore del modulo: il suo nome, come si legge il suo valore e come si riscrive.
 *
 * ⚠️⚠️ **I CURSORI SONO UNA TABELLA E NON SEI BLOCCHI COPIATI, DALLA `2.16`**: ognuno porta ora
 * **tre** gesti (il trascinamento, il doppio tocco che azzera, il tocco lungo che confronta), e
 * scritti riga per riga sarebbero diciotto occasioni di sbagliarne uno. Con la tabella il
 * comportamento si scrive **una** volta e un cursore nuovo lo prende per costruzione, che è lo
 * stesso criterio per cui `Modifier.lowered()` si porta dietro il velo.
 */
private class Dial(
    @param:StringRes val name: Int,
    val read: (Look) -> Float,
    val write: (Look, Float) -> Look,
    val span: Float = 1f,
    val stops: Boolean = false,
    /**
     * Se la corsa parte da zero invece che da `-span`.
     *
     * ⚠️⚠️ **NASCE COL DETTAGLIO, DALLA `2.22`, E NON È UNA VARIANTE GRAFICA**: fino alla `2.21`
     * ogni cursore era bipolare, perché ognuno aveva un verso in su e uno in giù. Là invece
     * 'nessuna nitidezza' e 'nessuna riduzione del rumore' sono il fondo naturale: una nitidezza
     * negativa sarebbe una sfocatura, e una riduzione negativa non vuol dire niente.
     */
    val unipolar: Boolean = false,
    /**
     * Quando questo cursore non governa niente, e l'interfaccia lo spegne.
     *
     * ⚠️ **Dalla `2.22` è una domanda e non più un interruttore del bianco e nero**: i casi sono
     * due (i colori spenti dal bianco e nero, e la maschera di contrasto senza nitidezza) e
     * scriverne uno per ognuno moltiplicherebbe i campi di questa tabella.
     */
    val off: (Look) -> Boolean = { false }
) {
    /**
     * Il cambiamento che porta questo cursore a [v], da applicare a quello che si vede **adesso**.
     *
     * ⚠️⚠️ **È UNA TRASFORMAZIONE E NON UN [Look] GIÀ FATTO, ED È LA CORREZIONE DELLA `2.17`**
     * (riscontro del giro della `2.16`, voce `luce-sei`: *quasi sempre se modifico il contrasto la
     * luminosità si azzera; se faccio un doppio tocco su un nome di slider se ne resetta anche un
     * altro*). Costruendo qui il `Look` di arrivo servirebbe quello di partenza, e chi chiama lo
     * avrebbe **catturato**: il corpo di un `pointerInput` si ricostruisce solo quando cambiano le
     * sue chiavi, quindi ogni gesto scriveva a partire dall'immagine di quando il suo nodo era
     * nato, e tutto quello che gli altri cursori avevano fatto nel frattempo tornava indietro.
     * Con una trasformazione il punto di partenza lo legge **chi la applica**, che è lo stato
     * vivo, e l'età della lambda non conta più.
     */
    fun set(v: Float): (Look) -> Look = { write(it, v) }
}

/**
 * Un modulo: il suo nome, i suoi cursori, e come si rimette a zero tutto insieme.
 *
 * ⚠️⚠️ **NASCE COL SECONDO MODULO, DALLA `2.19`, E FINO ALLA `2.18` NON SERVIVA**: con la sola
 * Luce l'elenco dei cursori era la scheda intera, e una fila di gettoni con un gettone solo
 * avrebbe detto dove si è, che era l'unico posto possibile. Adesso i moduli sono due e la fila
 * li sceglie.
 *
 * ⚠️⚠️ **[clear] È IL 'RESET MODULO' CHE LUI HA CHIESTO** (campo libero del giro della `2.14`,
 * punto 2: *per ciascun modulo ci dev'essere anche un 'Reset modulo'... potrebbe essere il tocco
 * lungo sul nome del modulo*), e arriva adesso per la stessa ragione: con un modulo solo avrebbe
 * fatto esattamente quello che fa 'Originale', che è lì accanto.
 */
private class Module(
    @param:StringRes val name: Int,
    /**
     * I cursori da mostrare, data la fascia scelta.
     *
     * ⚠️ **È una funzione e non una lista perché l'HSL ne ha otto insiemi**, uno per fascia, e
     * quale si vede lo decide la fila delle pastiglie. Gli altri due moduli rispondono sempre la
     * stessa lista, e il parametro lo ignorano.
     */
    val rows: (Int) -> List<Dial>,
    val clear: (Look) -> Look,
    val spent: (Look) -> Boolean,
    /**
     * Il segno che il gettone porta, dalla `2.31`, e il nome resta quello che si annuncia.
     *
     * ⚠️⚠️ **È SUA RICHIESTA** (campo libero del giro della `2.29`: *al posto dei nomi dei moduli
     * (che resterebbero per gli screen reader) dovremmo usare delle icone, che sono molto più
     * brevi e sarebbero tutte visibili senza scorrere in orizzontale*). Coi sette moduli i nomi
     * non entrano in nessuna larghezza, e una fila che scorre nasconde metà dei moduli a chi non
     * sa che si scorre.
     * ⚠️ **Il nome non se ne va, cambia posto**: resta il `contentDescription` del gettone, cioè
     * quello che un lettore di schermo annuncia, e l'etichetta del 'Reset modulo'.
     */
    val icon: ImageVector,
    /** Che cosa questo modulo ha in più dei suoi cursori: vedi [Extra]. */
    val extra: Extra = Extra.NONE
)

/**
 * Che cosa un modulo mette in scena oltre ai propri cursori.
 *
 * ⚠️⚠️ **È UN VALORE SOLO E NON DUE BANDIERINE, DALLA `2.23`**: fino alla `2.22` c'era `banded`, e
 * col modulo Curve sarebbe servita una seconda bandierina accanto a lei. Due booleani indipendenti
 * si possono accendere insieme, e un modulo che dichiarasse le fasce **e** il grafico sarebbe una
 * riga che compila e non vuol dire niente. Con un valore solo quel caso non esiste.
 */
private enum class Extra {
    /** Niente: la scheda mostra i soli cursori, come la Luce e il Colore. */
    NONE,

    /** La fila delle otto fasce di colore, cioè l'HSL. */
    BANDS,

    /**
     * La fila dei quattro canali e il grafico della curva, cioè le Curve.
     *
     * ⚠️ **Quel modulo non ha cursori affatto**, ed è il primo: il suo comando è il grafico, e la
     * sua lista di righe è vuota, quindi il ciclo dei cursori non disegna niente per costruzione.
     */
    CURVES,

    /**
     * I tre comandi di posa e le maniglie sul palco, cioè il **Ritaglio**, dalla `2.31`.
     *
     * ⚠️⚠️ **È IL PRIMO CHE CAMBIA QUELLO CHE IL PALCO FA COL DITO**: gli altri sei mettono in
     * scena dei comandi dentro la scheda, questo prende il dito sull'immagine, come il colore
     * mirato quando è armato. Il palco lo chiede con lo stesso criterio, cioè alla tabella dei
     * moduli, invece di tenere una seconda bandierina che qualcuno deve ricordarsi di spegnere.
     */
    CROP
}

/**
 * I cursori che il **bianco e nero** spegne: là non c'è più niente da saturare, e un cursore che
 * si muove senza cambiare l'immagine si legge come un guasto.
 */
private val MONO: (Look) -> Boolean = { it.chroma.mono }

/**
 * I cursori che governano la **maschera di contrasto**, e che senza di lei non governano niente.
 *
 * ⚠️ **Sono due dei cinque del Dettaglio**, il raggio e la mascheratura: non sono quantità, sono
 * come la nitidezza lavora. Con la nitidezza a zero non c'è nessuna maschera da governare.
 */
private val UNSHARP: (Look) -> Boolean = { it.detail.flat }

/**
 * I nomi delle otto fasce, nell'ordine dei centri di [Mix.CENTRES].
 *
 * ⚠️ **I due elenchi si leggono per indice e non si possono disallineare senza che si veda**: la
 * pastiglia prende il colore dal centro e il nome da qui, quindi una coppia sbagliata darebbe una
 * pastiglia verde che si chiama 'Blu'.
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
 * ⚠️⚠️ **SONO PRECOSTRUITI E NON GENERATI A OGNI RICOMPOSIZIONE, ED È QUELLO CHE TIENE IN PIEDI IL
 * `key`**: la scheda dà a ogni riga come chiave il proprio [Dial], e una chiave che cambia a ogni
 * giro butterebbe e rifarebbe i nodi dei cursori in continuazione, cioè annullerebbe ogni gesto in
 * corso. Così invece le chiavi sono stabili per tutta la vita del processo, e due fasce non ne
 * hanno nessuna in comune: cambiando fascia i nodi si buttano invece di passare di mano.
 *
 * ⚠️⚠️ **LA SATURAZIONE RIUSA LA STRINGA DEL MODULO COLORE**, perché è esattamente la stessa
 * parola: una chiave nuova che dice lo stesso testo sarebbe una seconda traduzione da tenere
 * allineata in ventotto lingue.
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
         * ⚠️⚠️ **QUESTA RIGA È ANCHE LA MISCELA DEL BIANCO E NERO, ED È LA SUA RISPOSTA `hsl` A
         * `d-bn-pesi`**: col bianco e nero acceso le altre due non hanno più niente da fare e si
         * spengono, mentre questa diventa **quanto quel colore pesa nel grigio**. Non porta
         * `dims`, ed è tutta la differenza: il conto è lo stesso, e il bianco e nero viene dopo
         * di lui proprio per raccoglierne il risultato.
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

/** I quattro cursori del modulo Colore: l'interruttore del bianco e nero è il quinto comando. */
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
    )
)

/**
 * I cinque cursori del modulo Dettaglio, nell'ordine del pannello di Lightroom: prima la maschera
 * di contrasto coi suoi due comandi, poi le due riduzioni del rumore.
 *
 * ⚠️⚠️ **IL RAGGIO È L'UNICO BIPOLARE DEI CINQUE, E IL SUO ZERO È IL RAGGIO DI SERIE**: gli altri
 * quattro partono da zero perché 'niente nitidezza' e 'niente riduzione' sono il loro fondo, mentre
 * un raggio zero non esiste. Il perché, con le misure, vive su [Detail.sharpReach].
 *
 * ⚠️ **'Rumore' e 'Rumore colore' e non 'Luminanza' e 'Colore'**: sono i nomi che Lightroom dà a
 * quei due cursori, ma qui 'Luminanza' è già la terza riga dell'HSL e 'Colore' è il nome di un
 * modulo, quindi due parole direbbero due cose a mezzo centimetro di distanza.
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
 * I cinque cursori del **Geometria**, nell'ordine del pannello di Lightroom: raddrizzamento,
 * proporzioni, orizzontale, verticale, distorsione.
 *
 * ⚠️⚠️ **I DUE KEYSTONE SI CHIAMANO COL PROPRIO ASSE E NON 'PROSPETTIVA', ED È IL NOME CHE HA IN
 * MANO**: sono tutte e due una correzione di prospettiva, quindi chiamarne uno 'Prospettiva'
 * direbbe che l'altro è un'altra cosa. Lightroom li chiama 'Verticale' e 'Orizzontale', che è il
 * pannello che lui conosce, e nel codice il campo porta la stessa parola: qui non c'è nessuna
 * ragione per cui il nome interno debba divergere da quello che si legge nel telefono.
 *
 * ⚠️⚠️ **SONO TUTTI E CINQUE BIPOLARI, E QUI NON È UNA SCELTA MA LA NATURA DEL MODULO**: ognuno ha
 * due versi opposti che vogliono dire due correzioni diverse (si ruota da una parte o dall'altra,
 * si stira in larghezza o in altezza, si guarda dal basso o dall'alto), e lo zero è l'immagine come
 * la fotocamera l'ha presa.
 *
 * ⚠️ **Il raddrizzamento si mostra da -100 a +100 come gli altri e non in gradi**, che è quello che
 * farebbe Lightroom: un secondo modo di scrivere un numero vorrebbe dire un secondo formato nella
 * riga del cursore, e quanto valga il fondo corsa lo dice [Warp.TILT], che è il posto in cui quel
 * grado vive.
 */
private val GEO_ROWS = listOf(
    Dial(
        R.string.look_straighten,
        { it.geo.straighten },
        { k, v -> k.copy(geo = k.geo.copy(straighten = v)) }
    ),
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
 * ⚠️⚠️ **L'ORDINE DEI CURSORI È QUELLO DEL PANNELLO DI LIGHTROOM, ED È IL SUO RIFERIMENTO** (giro
 * della `2.15`: *Lightroom ha solo 'Esposizione'*): chi apre questo editor ha in mente quello,
 * quindi un ordine nostro costringerebbe a cercare ogni volta il cursore che si sa già di voler
 * muovere. Vale per la Luce come per il Colore, dove temperatura e tinta vengono prima di quanto
 * i colori sono accesi, e per l'HSL, dove tonalità, saturazione e luminanza sono le tre righe di
 * quel pannello nel suo ordine.
 */
private val MODULES = listOf(
    /*
     * ⚠️⚠️ **IL PRIMO È IL RITAGLIO, DALLA `2.31`, ED È IL SUO ORDINE ALLA LETTERA** (campo libero
     * del giro della `2.29`: *`Geometria` dev'essere il secondo modulo; il primo dev'essere
     * `Ritaglio` (più o meno ciò che fa già l'editor semplice). Il terzo (ma attivo di default)
     * 'Luce', e gli altri di seguito nell'ordine attuale*). I due che non parlano di colore
     * vengono per primi perché sono le domande che si fanno per prime davanti a una fotografia:
     * che cosa ci sta dentro, e se sta dritta.
     * ⚠️ **Aperto di fabbrica resta il terzo**, cioè la Luce, e non il primo: l'indice di partenza
     * vive in [Gaze], che lo dichiara.
     */
    Module(
        R.string.look_crop,
        rows = { emptyList() },
        clear = { it.copy(spin = Spin.STILL, crop = ImageEdit.Crop.WHOLE) },
        spent = { !it.square },
        icon = Icons.Filled.Crop,
        extra = Extra.CROP
    ),
    /*
     * ⚠️⚠️ **IL SECONDO NON CAMBIA IL COLORE DI UN PIXEL, E NELLA CATENA RESTA PER ULTIMO**: gli
     * altri cinque dicono di che colore è un pixel, questo dice dove va a finire, e la ragione per
     * cui il conto si fa in fondo vive in testa a `Geometry.kt` (il Dettaglio deve leggere i pixel
     * del file e non quelli già interpolati). ⚠️ **Il posto nella fila e il posto nella catena sono
     * due cose diverse**, e dalla `2.31` non coincidono più: la fila è l'ordine in cui si lavora,
     * la catena è l'ordine in cui il conto gira.
     * ⚠️ **Non offre il colore mirato**, e la condizione se lo dice da sé: `Extra.NONE` vuol dire
     * che la scheda mostra i soli cursori, e mirare un colore in un modulo che i colori non li
     * tocca non vorrebbe dire niente.
     */
    Module(
        R.string.look_geometry,
        rows = { GEO_ROWS },
        clear = { it.copy(geo = Geometry.NONE) },
        spent = { !it.geo.idle },
        icon = Icons.Filled.Transform
    ),
    Module(
        R.string.look_light,
        rows = { LIGHT_ROWS },
        clear = { it.copy(light = Light.NONE) },
        spent = { !it.light.idle },
        icon = Icons.Filled.WbSunny
    ),
    Module(
        R.string.look_color,
        rows = { COLOUR_ROWS },
        clear = { it.copy(chroma = Chroma.NONE) },
        spent = { !it.chroma.idle },
        icon = Icons.Filled.Palette
    ),
    Module(
        R.string.look_mix,
        rows = { MIX_ROWS[it] },
        clear = { it.copy(mix = Mix.NONE) },
        spent = { !it.mix.idle },
        icon = Icons.Filled.Colorize,
        extra = Extra.BANDS
    ),
    /*
     * ⚠️⚠️ **QUESTO NON HA CURSORI, ED È STATO IL PRIMO COSÌ**: quello che un cursore sa dire è
     * 'quanto', e una curva dice 'quanto per ogni tono', cioè una cosa che nessuna manopola può
     * esprimere. Il suo comando è il grafico, e la sua lista di righe è vuota.
     */
    Module(
        R.string.look_tone,
        rows = { emptyList() },
        clear = { it.copy(tone = Tone.NONE) },
        spent = { !it.tone.idle },
        icon = Icons.Filled.Timeline,
        extra = Extra.CURVES
    ),
    /*
     * ⚠️⚠️ **L'ULTIMO È IL DETTAGLIO, DALLA `2.32`, ED È SUA ISTRUZIONE** (riscontro del giro
     * della `2.31`, voce `moduli-ordine`: *fa' un ulteriore spostamento: modulo 'Dettagli' ultimo
     * in fondo*). Fino alla `2.31` veniva prima delle Curve, cioè nell'ordine in cui i moduli
     * erano usciti.
     * ⚠️ **Nella catena invece resta il PRIMO**, prima del bilanciamento del bianco, e le due cose
     * non si toccano: questo modulo parla del file (quanto rumore ha il sensore) e va letto prima
     * che il contrasto moltiplichi la grana. Il posto nella fila è l'ordine in cui si lavora, il
     * posto nella catena è l'ordine in cui il conto gira.
     */
    Module(
        R.string.look_detail,
        rows = { DETAIL_ROWS },
        clear = { it.copy(detail = Detail.NONE) },
        spent = { !it.detail.idle },
        icon = Icons.Filled.Deblur
    )
)

/**
 * Quante colonne ha la fila della posa, cioè quanti sono i suoi tasti.
 *
 * ⚠️ **È il numero e non un conto sull'elenco**, perché quell'elenco si compone dentro la scheda,
 * dove servono i chiamanti dei tre tasti: qui c'è la forma della fila, e là che cosa fa ognuno.
 */
private const val POSE_KEYS = 5

/** La forma scelta nel Ritaglio, cioè l'indice di [Gaze.shape] riportato al suo valore. */
private fun cropShape(gaze: Gaze): Shape =
    Shape.entries.getOrElse(gaze.shape) { Shape.FREE }

/** Da che parte sta la forma scelta: vedi [cropShape]. */
private fun cropLay(gaze: Gaze): Lay = Lay.entries.getOrElse(gaze.lay) { Lay.TALL }

/**
 * Quante volte l'immagine **già posata** è più larga che alta.
 *
 * ⚠️⚠️ **SI CONTA SULLA POSA E NON SUL FILE, e senza quella riga le forme verrebbero storte**: un
 * quarto di giro scambia i due lati, quindi dopo una rotazione il 16:9 chiesto dai gettoni sarebbe
 * calcolato sull'aspetto di prima. È la stessa correzione che l'editor di casa fa dentro `onTurn`.
 * ⚠️ **Vive qui e non dentro la scheda, dalla `2.32`**: da quando 'Originale' ricava il proprio
 * rapporto dall'immagine, quel numero serve anche al palco (che tiene la forma mentre il dito
 * trascina una squadretta), e un secondo conto scritto là sarebbe il primo a divergere.
 */
private fun posedAspect(src: Bitmap?, spin: Spin): Float {
    val b = src ?: return 1f
    val dritto = b.width.toFloat() / b.height
    return if (spin.turns % 2 == 0) dritto else 1f / dritto
}

/**
 * La posa [gesto] applicata a [look], **col rettangolo che la segue**.
 *
 * ⚠️⚠️ **IL RITAGLIO È IN FRAZIONI DELL'IMMAGINE GIÀ POSATA, QUINDI GIRANDO VA RISCRITTO**: senza
 * questa riga il rettangolo resterebbe dov'è sullo schermo e si porterebbe via un'altra porzione di
 * fotografia, cioè un difetto che non dà nessun errore e che si vede solo con un ritaglio già
 * fatto.
 * ⚠️ **Lo riscrive [spunRect]**, cioè la stessa funzione dell'editor di casa: il rettangolo e
 * l'immagine si muovono insieme per costruzione, e non perché due conti scritti a parte dicono la
 * stessa cosa.
 * ⚠️ **Qui la rotazione NON rifà il rettangolo**, al contrario dell'editor di casa, e la differenza
 * è che là esiste una forma scelta (i gettoni dei formati) da rifare sull'aspetto nuovo. Qui il
 * rettangolo è libero, quindi girarlo lo lascia esattamente sulla stessa porzione di immagine.
 */
internal fun spunLook(look: Look, gesto: Spin): Look =
    look.copy(spin = look.spin.then(gesto), crop = spunRect(look.crop, gesto))

/**
 * Il modulo aperto di fabbrica, cioè la **Luce**.
 *
 * ⚠️⚠️ **NON È IL PRIMO DELLA FILA, ED È SUA ISTRUZIONE** (campo libero del giro della `2.29`: *Il
 * terzo (ma attivo di default) 'Luce'*): la fila è l'ordine in cui si lavora, l'apertura è dove si
 * lavora quasi sempre. ⚠️ **Si ricava dall'elenco e non è un numero scritto a mano**: chi sposta un
 * modulo si ritrova l'apertura giusta senza toccare altro, che è lo stesso criterio dei raggi delle
 * fasce dell'HSL.
 */
private val LOOK_FIRST = MODULES.indexOfFirst { it.name == R.string.look_light }.coerceAtLeast(0)

/**
 * I nomi dei quattro canali delle curve, nell'ordine degli indici di [Tone].
 *
 * ⚠️ **I tre colori riusano le stringhe delle fasce dell'HSL**, che dicono esattamente quelle tre
 * parole: chiavi nuove che dicono lo stesso testo sarebbero tre traduzioni in più da tenere
 * allineate in ventotto lingue.
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
 * ⚠️⚠️ **NON È UNA PROPRIETÀ DELL'IMMAGINE, E PER QUESTO NON ENTRA NELLA STORIA DEI PASSI**: è
 * dove si sta guardando, quindi 'Annulla' non deve riportarci un modulo o una fascia.
 *
 * ⚠️⚠️ **MA DALLA `2.23` NON PUÒ PIÙ VIVERE DENTRO LA SCHEDA, ED È IL COLORE MIRATO A COSTRINGERE
 * AL TRASLOCO**: quel gesto vive sul **palco**, e per sapere che cosa fare di un pixel toccato
 * deve sapere quale modulo si sta guardando (una curva o una fascia) e, per le curve, quale
 * canale. Con questi valori dentro la scheda il palco non li potrebbe leggere, e passarglieli uno
 * per uno vorrebbe dire quattro parametri che si tengono allineati a mano.
 *
 * ⚠️ **Le letture restano osservabili**: sono stati di Compose, quindi chi li legge si ricompone,
 * e un gesto che li legge al momento in cui scrive vede il valore di adesso. È la stessa
 * condizione su cui poggia la correzione della `2.20`.
 */
private class Gaze(
    module: Int = LOOK_FIRST,
    band: Int = 0,
    channel: Int = Tone.WHOLE,
    shape: Int = 0,
    lay: Int = 0
) {
    var module by mutableIntStateOf(module)
    var band by mutableIntStateOf(band)
    var channel by mutableIntStateOf(channel)

    /**
     * La forma scelta nel modulo Ritaglio e da che parte sta, cioè i due gettoni dell'editor di
     * casa, dalla `2.32`.
     *
     * ⚠️⚠️ **VIVONO QUI E NON NEL [Look], ED È LA STESSA RAGIONE DEL MODULO APERTO**: quello che si
     * salva sull'immagine è il **rettangolo**, cioè `crop`; la forma con cui lo si sta tirando è un
     * comando, e un comando non entra nella storia dei passi. Messa nel modello, 'Annulla'
     * riporterebbe indietro anche la scelta dei gettoni.
     * ⚠️ **Sono indici e non i due enum**, perché questo oggetto si salva alla rotazione come una
     * lista di interi: il valore vero lo ricostruiscono [cropShape] e [cropLay].
     */
    var shape by mutableIntStateOf(shape)
    var lay by mutableIntStateOf(lay)

    /**
     * Se il colore mirato è armato.
     *
     * ⚠️ **Non si salva alla rotazione, al contrario degli altri tre**: è una **modalità** accesa
     * per un gesto, non una scelta, e ritrovarla accesa dopo essere tornati alla schermata
     * vorrebbe dire un palco che non risponde più ai gesti di sempre senza che nessuno lo abbia
     * chiesto.
     */
    var aiming by mutableStateOf(false)

    companion object {
        /**
         * ⚠️ **Salva gli interi e non l'oggetto**: un `Saver` scritto per elenco è il modo con cui
         * Compose porta uno stato attraverso la morte del processo, e questi valori sono
         * esattamente quello che `rememberSaveable` teneva prima della `2.23`, quando vivevano
         * dentro la scheda.
         * ⚠️ **Chi ne aggiunge uno lo aggiunge in CODA**: un valore infilato in mezzo cambierebbe
         * il significato delle posizioni già salvate, cioè farebbe ritrovare un modulo al posto di
         * una fascia dopo una rotazione. I due del Ritaglio sono entrati così con la `2.32`.
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
 * ⚠️⚠️ **I TRE COLORI SONO SCRITTI QUI E NON PRESI DAL TEMA, ED È UNA SCELTA**: sono il **nome**
 * del canale, non un accento dell'app, quindi devono dire 'rosso', 'verde' e 'blu' in tutti e due i
 * temi. Sono presi più chiari del colore puro per la stessa ragione per cui le sedici tinte delle
 * cartelle sono una coppia: un blu pieno su fondo scuro non si distingue dal fondo.
 * - ⚠️ **Il composito invece è l'accento dell'app**, perché non è un canale: è la curva di tutti i
 *   toni, e il colore che le tocca è quello della superficie che la disegna.
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
 * ⚠️⚠️ **LA FILA DEI MODULI ENTRA CON LA `2.19`, cioè col secondo modulo**: fino alla `2.18` i
 * cursori erano la scheda intera e un gettone solo avrebbe detto dove si è, che era l'unico posto
 * possibile.
 *
 * ⚠️⚠️ **UN GETTONE DICE ANCHE SE IL SUO MODULO È STATO TOCCATO**, col punto d'accento accanto al
 * nome: i cursori di un modulo che non si sta guardando non si vedono, quindi senza quel segno
 * un'immagine cambiata da un modulo chiuso non avrebbe niente che lo dica, e 'Originale' sembrerebbe
 * l'unico modo per tornare indietro.
 */
@Composable
private fun LookSheet(
    look: Look,
    busy: Boolean,
    ready: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    /**
     * L'anteprima da misurare quando si tocca 'Auto', o `null` finché non è pronta.
     *
     * ⚠️ **Arriva qui e non nel modello**: il conto di [Auto] è puro e vuole dei pixel, e i pixel
     * li ha la schermata. Farlo chiedere al modello vorrebbe dire tenere una seconda strada per
     * un'immagine che è già in scena.
     */
    origin: Bitmap?,
    /** Dove si ha lo sguardo: il modulo, la fascia, il canale, e se il mirato è armato. */
    gaze: Gaze,
    onLive: ((Look) -> Look) -> Unit,
    onSettled: () -> Unit,
    onPeek: (((Look) -> Look)?) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOriginal: () -> Unit
) {
    val module = gaze.module
    val band = gaze.band
    val chosen = MODULES[module]

    /**
     * Il cursore che sta alla riga [riga] del modulo e della fascia che si stanno guardando
     * **adesso**, o `null` se là non c'è niente (i moduli non hanno tutti lo stesso numero di
     * cursori).
     *
     * ⚠️ **Legge [module] e [band] al momento della chiamata e non alla composizione**, ed è tutto
     * il suo valore: chiamata da dentro il rilevatore di un gesto, risponde con quello che il dito
     * ha davvero sotto. ⚠️ **La fascia è entrata nel conto con la `2.21` e non è un secondo
     * meccanismo**: è la stessa correzione della `2.20` su una dimensione in più, e senza di lei
     * un cursore dell'HSL scriverebbe nella fascia da cui il suo nodo è nato.
     */
    fun dialAt(riga: Int): Dial? = MODULES[gaze.module].rows(gaze.band).getOrNull(riga)

    /** Quante volte l'immagine **già posata** è più larga che alta: vedi [posedAspect]. */
    fun cropAspect(k: Look): Float = posedAspect(origin, k.spin)
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
                .padding(start = 16.dp, end = 16.dp, top = SHEET_TOP, bottom = 10.dp)
        ) {
            /*
             * ⚠️⚠️ **IL TOCCO LUNGO SU UN GETTONE AZZERA QUEL MODULO, ED È IL SUO 'RESET
             * MODULO'**: vive sul nome, come lo aveva descritto, e sul gettone invece che su un
             * titolo perché dalla `2.19` il nome del modulo **è** il gettone.
             * ⚠️ **Il gesto c'è anche sul modulo che non si sta guardando**, e non è un caso
             * limite da chiudere: è il modo di disfare quello che si è fatto altrove senza
             * andarci.
             */
            /*
             * ⚠️⚠️ **LA FILA NON SCORRE PIÙ, DALLA `2.31`, E LA RAGIONE È LA SUA RICHIESTA**: coi
             * sette gettoni a icona ci stanno tutti, quindi si dividono la larghezza come le otto
             * fasce dell'HSL. ⚠️ **Lo scorrimento della `2.23` non era una scelta ma un rimedio**:
             * coi nomi scritti la fila cresceva in altezza e il palco si riduceva a zero pixel,
             * e scorrere teneva metà dei moduli fuori dallo schermo per chi non sa che si scorre.
             */
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MODULES.forEachIndexed { i, mod ->
                    ModuleChip(
                        name = stringResource(mod.name),
                        icon = mod.icon,
                        chosen = i == module,
                        spent = mod.spent(look),
                        enabled = ready && !busy,
                        onTap = { gaze.module = i },
                        onHold = {
                            onLive(mod.clear)
                            onSettled()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            /*
             * ⚠️⚠️ **LA FILA DELLA POSA È DEL SOLO MODULO RITAGLIO, DALLA `2.31`, E I TRE TASTI
             * SONO QUELLI DELL'EDITOR DI CASA**: stesso pezzo (`ActionPad`), stessi glifi, stesse
             * etichette e stesso tocco lungo sul terzo. Disegnarne di nuovi vorrebbe dire due segni
             * per lo stesso gesto a un tocco di distanza, visto che dalla stessa immagine si entra
             * nell'uno o nell'altro editor.
             * ⚠️⚠️ **E DALLA `2.32` SONO CINQUE, CON I FORMATI SOPRA, ED È LA SUA RISPOSTA
             * `formati` A `d-crop-formati`** (giro della `2.31`: *portali, con le centrature*).
             * Fino alla `2.31` il rettangolo era libero e le due centrature non avrebbero avuto
             * niente da centrare: con una forma scelta ce l'hanno, ed è la stessa fila dell'editor
             * di casa.
             * ⚠️ **L'ordine salvato dal riordino non si legge**: quello è l'ordine della fila della
             * selezione, e infilarci dentro questi tasti darebbe una fila che si riordina in un
             * modo che nessuno ha chiesto.
             */
            if (chosen.extra == Extra.CROP) {
                val live = ready && !busy
                val lay = cropLay(gaze)
                fun pose(gesto: Spin) {
                    onLive { spunLook(it, gesto) }
                    onSettled()
                }
                /*
                 * ⚠️⚠️ **LE FILE SONO QUELLE DELL'EDITOR DI CASA, E DALLA `2.32` SONO LO STESSO
                 * PEZZO**: le forme le disegna `ShapeRows`, che vive di là, e qui resta la sola
                 * cosa che cambia fra i due editor, cioè dove si scrive la scelta. Fino alla
                 * `2.31` questa fila era ricopiata riga per riga, e con 'Originale' sarebbero
                 * state due copie da tenere d'accordo invece di una.
                 */
                ShapeRows(
                    shape = cropShape(gaze),
                    lay = lay,
                    enabled = live,
                    onShape = { one ->
                        /*
                         * ⚠️ **Ritoccare la forma GIÀ scelta rimette il rettangolo intero**, ed è
                         * la via di fuga dell'editor di casa: senza, una selezione ridotta per
                         * sbaglio non avrebbe un modo rapido di tornare grande.
                         */
                        onLive { k ->
                            val quanto = cropAspect(k)
                            k.copy(
                                crop = if (one == cropShape(gaze)) {
                                    one.fit(quanto, lay)
                                } else {
                                    reshaped(k.crop, quanto, one.value(lay, quanto))
                                }
                            )
                        }
                        gaze.shape = one.ordinal
                        onSettled()
                    },
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (one in Lay.entries) {
                        SheetChip(
                            text = stringResource(one.label),
                            selected = one == lay,
                            enabled = live,
                            onClick = {
                                if (one != lay) {
                                    onLive { k -> k.copy(crop = flipped(k.crop, cropAspect(k))) }
                                    gaze.lay = one.ordinal
                                    onSettled()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                ActionPad(
                    columns = POSE_KEYS,
                    stretch = true,
                    actions = listOf(
                        PadAction(
                            PadKey.CENTRE_ACROSS, Glyphs.AlignAcross, R.string.editor_center_across,
                            enabled = live,
                            onHold = {
                                onLive { it.copy(crop = centredDown(centredAcross(it.crop))) }
                                onSettled()
                            },
                            holdLabel = R.string.editor_center_both
                        ) {
                            onLive { it.copy(crop = centredAcross(it.crop)) }
                            onSettled()
                        },
                        PadAction(
                            PadKey.CENTRE_DOWN, Glyphs.AlignDown, R.string.editor_center_down,
                            enabled = live,
                            onHold = {
                                onLive { it.copy(crop = centredDown(centredAcross(it.crop))) }
                                onSettled()
                            },
                            holdLabel = R.string.editor_center_both
                        ) {
                            onLive { it.copy(crop = centredDown(it.crop)) }
                            onSettled()
                        },
                        /*
                         * ⚠️ **Il tocco lungo ha SEMPRE la sua etichetta**, come vuole
                         * [PadAction.onHold]: un gesto che il lettore di schermo non annuncia
                         * esiste solo per chi lo scopre per caso.
                         */
                        PadAction(
                            PadKey.FLIP, Icons.Filled.Flip, R.string.editor_flip,
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
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            /*
             * ⚠️⚠️ **LA FILA DELLE OTTO FASCE, DALLA `2.21`**: compare solo per i moduli che
             * lavorano su un colore per volta, cioè oggi l'HSL soltanto. È la seconda fila di
             * gettoni della scheda, e la sua forma è di proposito **diversa** dalla prima: là ci
             * sono parole, qui ci sono colori, e un colore dice che cosa si sta scegliendo senza
             * che nessuno lo debba leggere.
             * ⚠️ **I due gesti sono gli stessi del gettone di un modulo**, tocco per scegliere e
             * tocco lungo per azzerare, un gradino più in basso: là si azzera il modulo, qui la
             * fascia. Chi impara il gesto sopra lo ritrova qui.
             */
            if (chosen.extra == Extra.BANDS) {
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
             * ⚠️⚠️ **LA FILA DEI QUATTRO CANALI E IL GRAFICO, DALLA `2.23`**: è la terza forma che
             * questa scheda può prendere, e la sola in cui non ci sono cursori. I gesti dei
             * gettoni sono quelli di sempre, tocco per scegliere e tocco lungo per azzerare, un
             * gradino più in basso: là si azzera il modulo, qui la curva di quel canale.
             * ⚠️ **Il gettone è lo stesso della fila dei moduli e non un pezzo nuovo**: quello che
             * serve qui è esattamente quello che fa già, cioè un nome, il segno di 'toccato' e i
             * due gesti.
             */
            if (chosen.extra == Extra.CURVES) {
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
                     * ⚠️⚠️ **QUELLO CHE ARRIVA È UN CAMBIAMENTO E IL CANALE SI RISOLVE QUI, cioè la
                     * regola della `2.20` applicata a un modulo che non ha cursori**: il gesto del
                     * grafico non si porta dentro né la curva né il canale, che invecchierebbero
                     * tutti e due; legge [Gaze.channel] al momento della scrittura, e la curva di
                     * partenza è quella viva.
                     */
                    onEdit = { cambia ->
                        onLive { k -> k.copy(tone = k.tone.swap(gaze.channel, cambia)) }
                    },
                    onSettled = onSettled,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )
            }

            /*
             * ⚠️⚠️ **UN GESTO SCRIVE LA RIGA CHE TOCCA NEL MODULO CHE SI VEDE, E NON UN CURSORE
             * CHE SI PORTA DENTRO, DALLA `2.20`** (campo libero del giro della `2.19`: *il mio
             * tocco, mentre provo a spostare la tinta o la saturazione, sposta invece il contrasto
             * che è nell'altro modulo; lo stesso succede altrove, c'è qualcosa di mescolato*). Una
             * lambda che si porta dentro il proprio [Dial] dice il vero finché il nodo che la
             * tiene è quello per cui è nata; risolvendo la **riga** al momento della scrittura, il
             * cursore numero N scrive sempre il cursore numero N di quello che è in scena adesso,
             * cioè quello che il dito sta toccando.
             * ⚠️⚠️ **LA CAUSA NON È ACCERTATA, E SI SCRIVE COSÌ INVECE DI INVENTARLA**: quel
             * difetto **non si riproduce sul banco**, né col tocco secco né con un trascinamento
             * vero, e una spia messa dentro il gesto risponde col cursore giusto in tutti e due i
             * casi. Quindi questa riga non è la cura misurata di quel difetto: è il meccanismo che
             * lo rende impossibile qualunque sia la sua causa, ed è dichiarato come tale.
             * ⚠️ **Il `key` è la seconda metà**: senza, Compose riusa i composable di una lista
             * **per posizione**, quindi cambiando modulo i nodi dei cursori passano di mano
             * portandosi dietro tutto quello che un nodo tiene. La chiave è il [Dial], che è un
             * oggetto di [MODULES] e quindi stabile per tutta la vita del processo: due moduli non
             * ne hanno nessuno in comune, e i nodi si buttano invece di passare di mano.
             */
            chosen.rows(band).forEachIndexed { riga, knob ->
                key(knob) {
                    LookKnob(
                        name = stringResource(knob.name),
                        value = knob.read(look),
                        span = knob.span,
                        stops = knob.stops,
                        unipolar = knob.unipolar,
                        /*
                         * ⚠️ **Un cursore che non governa niente si spegne**, e i casi sono due: i
                         * colori col bianco e nero acceso, e la maschera di contrasto senza
                         * nitidezza. Un cursore che si muove senza cambiare l'immagine si legge
                         * come un guasto.
                         */
                        enabled = ready && !busy && !knob.off(look),
                        onLive = { v -> dialAt(riga)?.let { onLive(it.set(v)) } },
                        onSettled = onSettled,
                        /*
                         * ⚠️ **Il confronto si costruisce QUI**, con lo stesso `set` con cui il
                         * cursore scrive: è l'immagine di adesso con questo solo campo a zero,
                         * cioè la risposta alla domanda 'questo cursore, da solo, che cosa fa?'.
                         * ⚠️⚠️ **E PARTE DA QUELLO CHE SI VEDE ADESSO E NON DA [look]**, per la
                         * ragione misurata su [Dial.set]: questa riga vive dentro il rilevatore
                         * di un gesto, e il valore che ci si cattura invecchia.
                         */
                        onPeek = { on -> onPeek(if (on) dialAt(riga)?.set(0f) else null) }
                    )
                }
            }

            /*
             * ⚠️⚠️ **IL BIANCO E NERO È UN INTERRUTTORE E NON UN CURSORE A -100**: è una scelta
             * (questa immagine è a colori, o non lo è) e non una quantità, e scritto come fondo
             * corsa della saturazione resterebbe esposto a chiunque muova quel cursore. Nel conto
             * viene infatti dopo, e sulla stessa riga dei cursori perché è il quinto comando di
             * questo modulo.
             * ⚠️ **La riga è un bersaglio solo**, con `Role.Switch` sulla riga e niente
             * sull'interruttore: è la stessa regola delle righe del pannello delle impostazioni.
             */
            if (chosen.name == R.string.look_color) {
                val bw = stringResource(R.string.look_bw)
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
                    Text(
                        text = bw,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = look.chroma.mono, onCheckedChange = null, enabled = ready && !busy)
                }
            }

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
            /*
             * ⚠️⚠️ **DALLA `2.32` LA FILA È TUTTA DI ICONE, E L'ORDINE È IL SUO** (campo libero del
             * giro della `2.31`: *aggiungi un tasto 'Auto' ... può stare a sinistra di 'Annulla',
             * solo icona ... a sinistra di 'Auto', sposta 'Mirato' in forma di icona (un mirino) ed
             * elimina il suo pulsante testuale a sinistra*). Quindi da sinistra: **Mirato**,
             * **Auto**, **Annulla**, **Ripristina**, **Originale**, tutti allineati a destra.
             * ⚠️ **Con la pastiglia sparisce anche il suo posto a sinistra**, che teneva la fila
             * divisa in due: adesso i cinque comandi sono una cosa sola, e lo spazio a sinistra non
             * resta vuoto per niente.
             */
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                /*
                 * ⚠️⚠️ **IL COLORE MIRATO C'È DOVE IL MODULO HA UN BERSAGLIO DA SCEGLIERE, e non è
                 * una coincidenza**: quel gesto serve a dire *questo colore qui*, e ha senso solo
                 * dove esiste qualcosa da puntare, cioè una fascia dell'HSL. Negli altri moduli un
                 * cursore vale per tutta l'immagine, quindi non c'è niente da mirare.
                 * ⚠️⚠️ **ED È UN'ICONA E NON PIÙ UNA PASTIGLIA SCRITTA, DALLA `2.32`**: il nome
                 * resta come descrizione, cioè quello che un lettore di schermo annuncia e quello
                 * che il banco cerca, ed è lo stesso criterio dei sette gettoni dei moduli.
                 * ⚠️ **A dire che è acceso è il colore**: un `IconButton` non ha uno stato scelto,
                 * quindi il glifo passa all'accento quando la modalità è armata.
                 */
                if (chosen.extra == Extra.BANDS) {
                    IconButton(
                        onClick = { gaze.aiming = !gaze.aiming },
                        enabled = ready && !busy
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GpsFixed,
                            contentDescription = stringResource(R.string.look_target),
                            tint = if (gaze.aiming) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                LocalContentColor.current
                            }
                        )
                    }
                }
                /*
                 * ⚠️⚠️ **'AUTO' SCRIVE NEI CURSORI E BASTA, ED È QUELLO CHE LO RENDE ANNULLABILE**
                 * (sua richiesta: *dev'essere annullabile*): quello che ne esce è un [Look] come un
                 * altro, quindi entra nella storia dei passi e 'Annulla' lo disfa. Il conto, e
                 * perché imita 'Colore automatico' di Photoshop con quattro cursori, vivono in
                 * [Auto].
                 * ⚠️ **Legge l'anteprima e non il file**: quello che si misura sono percentili e
                 * medie, e su una riduzione valgono quanto sull'originale. Leggere il file pieno
                 * costerebbe una pausa per una cifra che non si muove di un livello.
                 * ⚠️ **Il glifo è di Material e nasce provvisorio**, come i due della `1.80`: se non
                 * dice abbastanza, il giro di collaudo lo chiede e lui manda il suo.
                 */
                val sorgente = origin
                IconButton(
                    onClick = {
                        if (sorgente != null) {
                            onLive { Auto.tuned(it, Auto.probe(sorgente)) }
                            onSettled()
                        }
                    },
                    enabled = ready && !busy && sorgente != null
                ) {
                    Icon(Icons.Filled.AutoFixHigh, stringResource(R.string.look_auto))
                }
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
 * Un gettone della fila dei moduli: il nome, il segno di 'toccato', e i due gesti.
 *
 * ⚠️⚠️ **È SCRITTO IN CASA E NON È UN `FilterChip`, E LA RAGIONE È IL TOCCO LUNGO**: quel pezzo
 * di Material prende il suo `onClick` e non offre un secondo gesto, quindi il 'Reset modulo'
 * andrebbe messo con un `pointerInput` nel modificatore, cioè in un **secondo nodo** che consuma
 * il tocco prima che il chip lo veda. Con `combinedClickable` i gesti sono due e il bersaglio
 * resta uno, che è la regola di ogni riga di questa app.
 */
@Composable
private fun ModuleChip(
    name: String,
    /**
     * Il glifo da scrivere al posto del nome, o `null` per il gettone scritto.
     *
     * ⚠️⚠️ **I DUE ASPETTI SONO DUE FILE DIVERSE E NON UNA PREFERENZA**: i moduli sono sette e coi
     * nomi non entrano nella larghezza (§ '🎚️ L'editor completo'), i canali delle Curve sono
     * quattro e i loro nomi sono una lettera o poco più, quindi là un'icona direbbe meno della
     * parola. ⚠️ **Il nome non si perde nemmeno col glifo**: resta il `contentDescription`, cioè
     * quello che un lettore di schermo legge e quello che il banco cerca.
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
                onLongClick = onHold,
                onLongClickLabel = wipe
            )
            .padding(horizontal = if (icon == null) MODULE_SIDE else 0.dp, vertical = 8.dp)
            .alpha(if (enabled) 1f else OFF_INK),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        /*
         * ⚠️⚠️ **IL SEGNO È UN'ICONA E IL NOME SI ANNUNCIA, DALLA `2.31`**: coi sette moduli le
         * parole non entrano in nessuna larghezza, e la fila che scorreva ne teneva metà fuori
         * dallo schermo. ⚠️ **Il nome non si perde**: è il `contentDescription`, cioè quello che un
         * lettore di schermo legge, ed è anche quello che il banco cerca.
         */
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
 * ⚠️⚠️ **IL COLORE ARRIVA DAL CENTRO DELLA FASCIA E NON DA UNA TAVOLOZZA SCRITTA A MANO**: è lo
 * stesso numero che il conto usa per sapere a quale fascia appartiene un pixel, quindi la
 * pastiglia non può dire un colore che il cursore non tocca. Con due elenchi, il primo a divergere
 * sarebbe quello che nessuno guarda.
 *
 * ⚠️ **I due segni sono in due posti diversi**, l'anello intorno e il punto sotto: si possono
 * vedere insieme (la fascia scelta è spesso anche quella toccata), e sovrapposti si
 * confonderebbero. È la stessa coppia di domande del gettone di un modulo, dove il punto sta
 * accanto al nome perché là il posto c'è.
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
                onLongClick = onHold,
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
 * ⚠️⚠️ **I GESTI SONO DUE E SONO QUELLI DI CASA**: il **trascinamento** prende il punto sotto il
 * dito, o ne fa uno nuovo, e lo porta dove si vuole; il **tocco lungo** su un punto esistente lo
 * toglie. Il secondo è lo stesso gesto con cui si azzera un modulo e una fascia, un gradino più in
 * basso: là si azzera un insieme di valori, qui si toglie un punto.
 * - ⚠️ **Un tocco secco fa nascere un punto SULLA curva**, senza spostarla: chi tocca il grafico
 *   vuole prendere quella curva in quel punto, e farla saltare al dito nell'istante in cui la si
 *   prende sarebbe un movimento che nessuno ha chiesto.
 *
 * ⚠️⚠️ **IL RIQUADRO NON È QUADRATO, ED È UN COMPROMESSO DICHIARATO**: un grafico tonale si disegna
 * quadrato, perché così la diagonale è davvero a quarantacinque gradi e la pendenza si legge a
 * occhio. Qui la scheda porta già due file di gettoni e i tre comandi della storia, e un quadrato
 * largo quanto lo schermo si prenderebbe metà del palco, cioè l'immagine su cui si sta lavorando.
 * La forma della curva resta leggibile, e a dire i valori ci sono i punti.
 *
 * ⚠️ **Che cosa resta fuori, e si dichiara**: un grafico a punti non si governa con un lettore di
 * schermo, quindi qui l'accessibilità si ferma alla descrizione di che cosa è. Chi lavora così ha
 * i sei cursori della Luce, che coprono lo stesso mestiere con dei comandi che si annunciano.
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

    Canvas(
        modifier = modifier
            .height(BOARD_H)
            .semantics { contentDescription = board }
            .pointerInput(enabled, curve.knots.size) {
                if (!enabled) return@pointerInput
                val wide = (size.width - 2 * pad).coerceAtLeast(1f)
                val tall = (size.height - 2 * pad).coerceAtLeast(1f)

                /** Dove cade, in scala della curva, il punto [at] dello schermo. */
                fun atOf(at: Offset): Float = ((at.x - pad) / wide).coerceIn(0f, 1f)
                fun toOf(at: Offset): Float = (1f - (at.y - pad) / tall).coerceIn(0f, 1f)

                awaitEachGesture {
                    val down = awaitFirstDown()
                    val x = atOf(down.position)
                    val near = curve.nearest(x)
                    /*
                     * ⚠️ **Il tempo si misura qui e non dentro il ciclo degli eventi**, per la
                     * ragione scritta su [settled]: un dito fermo non genera nessun evento, quindi
                     * il tocco lungo lo può vedere solo un timeout.
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
                     * ⚠️ **L'indice si ricava PRIMA di scrivere**: `grow` risponde anche dove è
                     * finito il punto, e da lì in poi il gesto muove quello. Ricavarlo dopo, dalla
                     * curva viva, vorrebbe dire cercarlo a ogni fotogramma mentre si sposta.
                     */
                    val i = if (near >= 0) near else curve.grow(x).second
                    if (near < 0) {
                        onEdit { it.grow(x).first }
                    }
                    if (esito == Settled.MOVED) {
                        drag(down.id) { change ->
                            onEdit { it.move(i, atOf(change.position), toOf(change.position)) }
                            change.consume()
                        }
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
        // La griglia in terzi e la diagonale: due riferimenti che dicono dov'è il dito senza
        // nessun numero scritto. ⚠️ La diagonale è **la curva che non fa niente**, quindi si
        // disegna tratteggiata: piena si confonderebbe con una curva a riposo.
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

        // La curva, letta dalla stessa tabella che va alla scheda grafica: quello che si vede qui
        // è esattamente quello che l'immagine riceve, e non un secondo disegno che le somiglia.
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
 * ⚠️⚠️ **IL NUMERO È IL TASTO CHE AZZERA, e non è l'unico dalla `2.16`**: in un editor a cursori
 * il gesto che si fa più spesso è 'rimetti questo a zero', e adesso lo fanno anche il **doppio
 * tocco** sul nome, sul tondo e sulla barra (sua richiesta del 2026-09-11). Il numero resta
 * perché è l'unica delle quattro superfici che si vede da sola, cioè che dice 'sono io il
 * comando'.
 *
 * ⚠️⚠️ **IL TOCCO LUNGO SUL NOME MOSTRA L'IMMAGINE SENZA QUESTO CURSORE** (stessa richiesta:
 * *ma solo relativo alla modifica dello slider stesso rispetto all'originale*), e vive sul **nome**
 * e non sulla barra perché la barra ha già il dito sopra mentre si trascina: un tocco lungo là
 * dentro scatterebbe ogni volta che ci si ferma un istante a guardare.
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
    onPeek: (Boolean) -> Unit,
    span: Float = 1f,
    stops: Boolean = false,
    unipolar: Boolean = false
) {
    val zero = stringResource(R.string.look_reset_one, name)
    val against = stringResource(R.string.look_peek_one, name)
    /*
     * ⚠️⚠️ **LE TRE LAMBDA SI LEGGONO VIVE, DALLA `2.20`, ED È UNA DIFESA DICHIARATA E NON UNA
     * CURA MISURATA**: il corpo di un `pointerInput` si ricostruisce solo quando cambiano le sue
     * chiavi, quindi in linea di principio un rilevatore può chiamare le lambda della composizione
     * in cui è nato. ⚠️ **Che qui succeda non è provato**: una spia messa dentro il gesto dice che
     * a rispondere è sempre la lambda di adesso, e l'ipotesi era già caduta nella `2.17`, misurata
     * in due modi. Con uno stato aggiornato in mezzo la domanda non si pone più, e costa tre
     * righe.
     * ⚠️ **È la stessa forma di `live` in [LookDial]**, che là vale per il valore.
     */
    val write by rememberUpdatedState(onLive)
    val settle by rememberUpdatedState(onSettled)
    val peek by rememberUpdatedState(onPeek)
    val reset = { write(0f); settle() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .width(KNOB_NAME)
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
             * ⚠️ **Gli stop si scrivono con due decimali e il resto come numero intero**: un
             * diaframma è +0,33 e una percentuale è +33, e scriverli allo stesso modo
             * farebbe leggere un valore per l'altro.
             * ⚠️ **E un cursore monopolare non porta il segno**, dalla `2.22`: là il numero non
             * può essere negativo, e un `+` davanti a una corsa che parte da zero dice che
             * esiste un verso che non c'è.
             */
            text = when {
                stops -> "%+.2f".format(value)
                unipolar -> "%d".format((value * 100).roundToInt())
                else -> "%+d".format((value * 100).roundToInt())
            },
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.End,
            color = if (abs(value) < 0.0005f) MaterialTheme.colorScheme.onSurfaceVariant
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
 * ⚠️⚠️ **NON È UNO `Slider` DI MATERIAL, E LE RAGIONI SONO DUE, TUTTE E DUE SUE.** La prima è
 * l'aspetto (giro della `2.15`, voce `luce-taratura`: *credo che mi piacerebbero di più dei bei
 * tondi grossi al posto delle barrette verticali Material*), e da sola non basterebbe, perché il
 * pezzo di Material accetta un tondo scritto da noi. La seconda è il **doppio tocco che azzera**:
 * uno `Slider` risponde al primo tocco saltando al punto, quindi il primo dei due toccherebbe il
 * cursore e scriverebbe un passo nella storia che nessuno ha chiesto. Qui il salto si scrive dopo
 * che il doppio tocco è stato escluso, e chi tocca due volte ottiene **un** passo solo.
 *
 * ⚠️ **Il trascinamento invece non aspetta niente**: appena il dito supera la soglia si muove, e
 * il doppio tocco non è più possibile. Il ritardo esiste solo per il tocco secco, dove si misura
 * in una frazione di secondo e non si vede, perché il valore si è già mosso.
 *
 * ⚠️⚠️ **L'AZIONE SEMANTICA NON È UN DI PIÙ**: senza `setProgress` questo cursore sarebbe muto per
 * un lettore di schermo e invisibile al banco di prova, che i cursori li muove **da lì**. Chi
 * riscrive questo pezzo la tenga: costa tre righe e senza di lei `LuceTest` non misura niente.
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
     * Il fondo corsa: lo zero per i cursori che un verso solo ce l'hanno, `-span` per gli altri.
     *
     * ⚠️ **Si legge da qui in tutti e quattro i posti** (la semantica, l'azione, il conto del
     * gesto e il disegno): scritto quattro volte, il primo a divergere sarebbe quello che
     * nessuno guarda, e un tondo disegnato dove il dito non lo trova è un difetto che non dà
     * nessun errore.
     */
    val low = if (unipolar) 0f else -span
    /** L'ampiezza della corsa, cioè quanto vale il tratto da un estremo all'altro. */
    val sweep = span - low
    val ink = MaterialTheme.colorScheme.primary
    val rail = MaterialTheme.colorScheme.surfaceVariant
    val mark = MaterialTheme.colorScheme.outline
    val face = MaterialTheme.colorScheme.surfaceContainerHigh
    val faded = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    /*
     * ⚠️⚠️ **IL VALORE SI LEGGE VIVO E NON SI CATTURA, ED È LO STESSO DIFETTO DEL PASSO DELLA
     * `2.14`**: il corpo di un `pointerInput` si ricostruisce solo quando cambiano le sue chiavi,
     * quindi il parametro di questa funzione resterebbe quello della prima composizione. Serve a
     * sapere **dov'è il tondo** quando il dito scende: con un valore stantio, un tocco sul
     * tondo verrebbe letto come un tocco lontano, e il cursore salterebbe dove il dito non ha
     * chiesto di andare.
     */
    val live by rememberUpdatedState(value)

    Canvas(
        modifier = modifier
            .height(DIAL_ROW)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value, low..span)
                /*
                 * ⚠️⚠️ **L'AZIONE C'È ANCHE DA SPENTO, E RISPONDE `false`: dichiararla solo da
                 * acceso ha fatto una prova ROSSA IN CI E VERDE QUI.** Finché l'anteprima si
                 * decodifica i cursori sono spenti, quindi il nodo non portava nessuna azione
                 * e il banco, che i cursori li muove da lì, non li trovava affatto: su una
                 * macchina più lenta la scena arriva un attimo dopo, e la prova falliva mentre
                 * qui passava. ⚠️ Un lettore di schermo lo annuncia spento per il `disabled()`,
                 * che è il modo giusto di dire 'c'è ma adesso non si può'.
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
                         * ⚠️ **Il passo aspetta di sapere se erano due tocchi**: scritto subito,
                         * un doppio tocco lascerebbe nella storia il salto del primo, cioè un
                         * valore che nessuno voleva e che 'Annulla' riporterebbe indietro.
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
        // Il tratto acceso parte dallo zero: su un cursore bipolare quello è il centro, e un
        // pieno che partisse da sinistra direbbe che il valore neutro è già mezzo acceso; su uno
        // monopolare lo zero **è** il fondo corsa, quindi il tratto parte da sinistra da sé.
        drawLine(hot, Offset(zero, middle), Offset(at, middle), thick, cap = StrokeCap.Round)
        // La tacca dello zero, che il tondo copre quando è al centro: senza, il valore neutro si
        // trova solo guardando il numero. ⚠️ **Su un cursore monopolare non si disegna**: là
        // cadrebbe sotto il tondo a riposo, cioè segnerebbe il fondo corsa, che si vede da sé.
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
 * ⚠️⚠️ **NON SI OTTIENE CON `detectTapGestures`, e la ragione è il RILASCIO**: quel rilevatore
 * annuncia il tocco lungo una volta e non dice più niente, mentre qui il confronto deve durare
 * *fino a che il dito si alza*. Con l'altra strada servirebbe un secondo gesto per il rilascio, e
 * i due si contenderebbero l'evento.
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
             * ⚠️⚠️ **LO SPEGNIMENTO VIVE IN UN `finally`, DALLA `2.17`, E NON È PRUDENZA
             * GENERICA**:
             * un rilevatore di gesti viene **annullato** quando il suo `pointerInput` cambia chiave
             * (qui basta un salvataggio che parte, che spegne i cursori), e un'attesa annullata non
             * torna alla riga dopo. Senza, il confronto resterebbe acceso per sempre, e l'immagine
             * mostrerebbe un cursore in meno senza che nessun numero lo dica.
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

/** Quanto è larga la colonna dei nomi dei cursori. */
private val KNOB_NAME = 96.dp

/** Il punto che dice 'questo modulo ha toccato l'immagine', nel gettone della fila. */
private val MODULE_MARK = 6.dp

/**
 * L'aria ai fianchi di un gettone **scritto**, cioè dei quattro canali delle Curve.
 *
 * ⚠️ **Il gettone a icona non ne ha**: là la larghezza la divide la fila (`weight`), quindi un
 * rientro ai fianchi toglierebbe area di tocco senza spostare niente.
 */
private val MODULE_SIDE = 14.dp

/**
 * L'altezza di una pastiglia della fila delle fasce.
 *
 * ⚠️ **È l'area di tocco e non il disegno**: dentro vivono il tondo, l'anello di chi è scelto e il
 * punto di chi è stato toccato, e il resto serve al dito.
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
 * ⚠️ **Non è il colore che il conto vedrà**, ed è giusto così: la pastiglia deve dire *quale
 * colore* si sta scegliendo, quindi porta la tonalità della fascia alla sua massima riconoscibilità
 * invece che al colore medio di una fotografia, che sarebbe un grigio sporco diverso ogni volta.
 */
private const val BAND_SAT = 0.85f
private const val BAND_VAL = 0.95f

/**
 * Il colore con cui si disegna la fascia della tonalità [hue], in giri.
 *
 * ⚠️⚠️ **UNA FUNZIONE SOLA PER LE PASTIGLIE E PER IL MIRINO, DALLA `2.25`**: dalla richiesta di far
 * portare al mirino *uno degli 8 colori standard* nasce un secondo posto che disegna una fascia, e
 * due conti darebbero due verdi diversi per lo stesso colore. È la stessa ragione per cui la fila
 * delle pastiglie prende la tonalità dai centri di `Mix` invece di avere un elenco suo.
 */
private fun bandTint(hue: Float): Color = Color.hsv(hue * 360f, BAND_SAT, BAND_VAL)

/**
 * Il colore della fascia a cui appartiene [pixel], e `null` se quel pixel è un grigio.
 *
 * ⚠️ **Il `null` non è un caso limite da chiudere con un colore qualunque**: un grigio non
 * appartiene a nessuna fascia, e dargli il rosso vorrebbe dire promettere al dito una fascia su cui
 * quel pixel non ha nessun peso. Vedi `Mix.bandOf`, che risponde `-1` per la stessa ragione.
 */
internal fun tintOfPixel(pixel: Int): Color? =
    Mix.bandOf(pixel).takeIf { it >= 0 }?.let { bandTint(Mix.CENTRES[it]) }

/**
 * Quanto è alto il grafico della curva.
 *
 * ⚠️ **Non è quadrato, ed è un compromesso dichiarato**: il perché vive sul KDoc di [CurveBoard].
 * Questo numero è quello che lascia al palco più di metà schermo con la scheda delle curve aperta,
 * che è la condizione da cui si guarda quello che la curva sta facendo.
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
 * ⚠️ **Vale più di [BOARD_DOT] di proposito**: i due estremi della curva cadono sui bordi, e con
 * meno aria il loro tondo verrebbe tagliato a metà dal riquadro.
 */
private val BOARD_PAD = 8.dp

/** Quanto è larga la colonna del numero: ci deve stare `-100` col segno. */
private val KNOB_VALUE = 48.dp

/**
 * L'altezza di una riga di cursore.
 *
 * ⚠️ **È l'area di tocco e non l'altezza del disegno**: il tondo è alto la metà, e il resto serve
 * perché il dito prenda la barra senza centrarla.
 */
private val DIAL_ROW = 40.dp

/** Il raggio del tondo, che è la misura che lui ha chiesto di far crescere. */
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
 * ⚠️ **Due volte e non di più**: il doppio tocco serve a guardare un dettaglio in un colpo, e da
 * lì si continua con le dita. Un salto più lungo porterebbe quasi sempre fuori dal punto voluto.
 */
private const val ZOOM_TAP = 2f

/**
 * Il tetto dell'ingrandimento.
 *
 * ⚠️ **Lo decide l'anteprima e non il gusto**: quello che si vede è la riduzione con cui l'editor
 * lavora in fretta, quindi oltre questo ingrandimento si guarderebbero i pixel di quella e non
 * quelli della fotografia.
 */
private const val ZOOM_MAX = 6f

/**
 * Quanto dito serve per raddoppiare l'ingrandimento a una mano.
 *
 * ⚠️ **Il conto che lo regge**: da uno a [ZOOM_MAX] ci sono due raddoppi e mezzo, quindi con
 * questa misura l'intera corsa entra in poco più di un terzo di schermo, cioè in un trascinamento
 * che il pollice fa senza staccarsi. Più corto, il tetto arriverebbe prima di aver guardato
 * l'immagine; più lungo, si finirebbe il vetro a metà strada.
 */
private val ZOOM_PULL = 96.dp

/**
 * Quanto dura la corsa del doppio tocco, in millisecondi.
 *
 * ⚠️ **È il numero della prima stesura e si guarda sul telefono**: il banco misura che la corsa
 * esista e dove finisca, non come si percepisce. Il riferimento in casa è la dissolvenza fra due
 * schermate (180 ms): qui è un filo più lunga perché il movimento è più grande, e la curva parte
 * decisa e si posa, che è il modo in cui una lente si ferma.
 */
private const val ZOOM_RIDE = 220

/**
 * Il lato della lente del colore mirato.
 *
 * ⚠️ **Poco più di un polpastrello**: deve stare sopra il dito senza coprire la fotografia su cui
 * si sta scegliendo, e mostrare abbastanza intorno da capire dove si è. Il riferimento in casa è
 * la lente dell'angolo del ritaglio, nell'editor di casa.
 */
private val LENS_SIDE = 112.dp

/** Quanto la lente sta staccata dal dito: abbastanza da non finire sotto il polpastrello. */
private val LENS_AIR = 20.dp

/**
 * L'aria intorno all'immagine mentre il modulo **Ritaglio** è in scena.
 *
 * ⚠️⚠️ **SI RICAVA DALLA SQUADRETTA E NON È UN NUMERO SCELTO**: il tracciato di `bracket` corre a
 * mezzo spessore **fuori** dal rettangolo, e l'alone gli sta intorno, quindi l'inchiostro arriva
 * esattamente a `HANDLE_THICK + GRIP_HALO` oltre il bordo dell'immagine. Chi cambiasse una di
 * quelle due misure si ritroverebbe l'aria giusta senza toccare niente; un numero scritto a mano
 * tornerebbe a tagliare le maniglie al primo ritocco.
 * ⚠️ **Il perché esista vive su [fitted]**, insieme al difetto che ha corretto.
 */
private val CROP_AIR = HANDLE_THICK + GRIP_HALO

/**
 * Di quanto la lente ingrandisce.
 *
 * ⚠️ **Si moltiplica all'ingrandimento del palco invece di sostituirlo**: chi ha già ingrandito
 * l'immagine sta guardando da vicino, e una lente a scala fissa gliela mostrerebbe **più piccola**
 * di quello che ha davanti.
 * ⚠️ **Sei e non dieci**: con questa misura la lente mostra una ventina di pixel dell'anteprima
 * per lato, cioè il pixel scelto e il suo intorno. Più su, si vedrebbe un colore solo e non si
 * capirebbe più dove si è.
 */
private const val LENS_ZOOM = 6f

/** Il bordo della lente: lo stesso delle altre superfici dell'app, e per la stessa ragione. */
private val LENS_EDGE = 2.dp

/**
 * Il raggio dell'anello del mirino, e lo spessore dei suoi due tratti.
 *
 * ⚠️ **Il tratto è raddoppiato dalla `2.25`, su sua richiesta** (*l'anello del 'mirino' deve essere
 * più spessa*): a un punto di spessore il colore della fascia che l'anello adesso porta si
 * confondeva con l'immagine sotto.
 */
private val LENS_PIP = 5.dp
private val LENS_PIP_LINE = 2.dp


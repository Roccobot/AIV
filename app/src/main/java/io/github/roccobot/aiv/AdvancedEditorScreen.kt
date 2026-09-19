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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    /**
     * Se una **filigrana** è pronta da scrivere: come nell'editor di casa, è quello che rende
     * 'Salva' toccabile su un'immagine che nessun cursore ha ancora toccato.
     */
    marked: Boolean,
    /**
     * Che cosa applicare al file vero. Il lavoro lo fa chi chiama, come per l'editor di casa.
     *
     * ⚠️ Il secondo argomento è il **tocco lungo**: `true` chiede un file nuovo accanto
     * all'originale invece di riscriverlo. Vedi [SaveButton].
     */
    onSave: (Look, Boolean) -> Unit,
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

    /*
     * ⚠️⚠️ **IL MINI-ONBOARDING DELLA FILA DEI MODULI, DALLA `2.50`, ED È SUA RICHIESTA** (campo
     * libero del giro della `2.40`: *aggiungiamo un mini-onboarding al primo avvio dell'editor*).
     * Serve perché dalla `2.50` i moduli sono otto e la fila **continua fuori dallo schermo**: uno
     * scorrimento non si dichiara da sé, e senza il velo l'ottavo modulo lo troverebbe solo chi
     * prova a trascinare per caso.
     * ⚠️ **Il riquadro da illuminare arriva da una misura e non da un conto**: la fila vive dentro
     * la scheda, che ha i suoi rientri e la sua altezza, quindi rifare quella catena qui vorrebbe
     * dire una seconda geometria che cade sul vuoto al primo ritocco. È lo stesso criterio del
     * velo della copertina.
     * ⚠️ **Parte da 'già visto'**, come gli altri: il valore vero arriva dal disco un fotogramma
     * dopo, e partendo dal contrario il velo lampeggerebbe a ogni apertura.
     */
    val scope = rememberCoroutineScope()
    val hinted by produceState(true) { Hint.MODULES.flow(context).collect { value = it } }
    var strip by remember { mutableStateOf(Rect.Zero) }

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
                SaveButton(
                    // ⚠️ La filigrana è lavoro da salvare, come nell'editor di casa: senza questa
                    // condizione una firma da sola non si potrebbe applicare.
                    enabled = origin != null && !busy && (marked || !look.idle),
                    onSave = { onSave(look, false) },
                    onBeside = { onSave(look, true) }
                )
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
                        /*
                         * ⚠️⚠️ **IL CONFRONTO TOGLIE UN GRUPPO DI CAMPI E NON TUTTI, DALLA `2.58`,
                         * ED È SUA RICHIESTA** (campo libero del giro della `2.55`): nei due moduli
                         * che dicono **dove** va un pixel mostra l'originale intero, perché è
                         * proprio quello che si sta tarando; in tutti gli altri tiene posa, taglio,
                         * geometria e vista, cioè confronta il **colore** dentro l'inquadratura di
                         * adesso. Il perché, e che cosa succede a chi aggiunge un campo a [Look],
                         * vivono su [Look.place].
                         * ⚠️⚠️ **NEL RITAGLIO QUEL RAMO NON LO RAGGIUNGE NESSUN DITO, E SI DICHIARA
                         * INVECE DI LASCIARLO CREDERE VIVO**: là il palco fa solo quello, cioè il
                         * dito serve alle squadrette e il confronto non parte, ed è così da quando
                         * quel modulo esiste. La riga lo nomina lo stesso perché la sua richiesta
                         * nomina i due moduli insieme: il giorno che quel dito si liberasse, il
                         * comportamento è già quello giusto. Oggi si vede nella **Geometria**, dove
                         * il palco risponde finché lo strumento 'Angoli' è spento.
                         */
                        look = when {
                            !comparing -> peek?.invoke(look) ?: look
                            MODULES[gaze.module].extra.places -> Look.NONE
                            else -> look.place
                        },
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
                        /*
                         * ⚠️⚠️ **LO STRUMENTO 'ANGOLI' SI ARMA COME IL COLORE MIRATO, DALLA `2.50`**:
                         * è lo stesso tasto e lo stesso stato, e a dire quale dei due gesti sia è la
                         * tabella dei moduli. Passando a un altro modulo il valore torna nullo, cioè
                         * le maniglie se ne vanno insieme al tasto che le spegne.
                         */
                        corners = look.geo.corners.takeIf {
                            gaze.aiming && MODULES[gaze.module].extra == Extra.CORNERS
                        },
                        onCorners = { look = look.copy(geo = look.geo.copy(corners = it)) },
                        onCornersEnd = { push() },
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
                onStrip = { strip = it },
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

        if (!hinted && strip != Rect.Zero) {
            HintStrip(
                text = stringResource(R.string.hint_modules),
                spot = strip,
                icons = LocalPadLook.current.mods.map { modGlyph(it) },
                onDone = { scope.launch { Hint.MODULES.remember(context) } }
            )
        }
    }
}

/**
 * Il comando che scrive, in testata: il tocco riscrive il file, il tocco lungo ne fa uno nuovo.
 *
 * ⚠️⚠️ **IL SECONDO GESTO È SUO, DALLA `2.58`** (campo libero del giro della `2.55`: *tocco lungo
 * su 'Salva' (in alto a destra) nell'editor: salva un nuovo file accanto all'originale*). È il
 * rovescio della strada di sempre: l'editor riscrive l'immagine dov'è, e chi vuole tenere anche il
 * prima oggi deve uscire, duplicare il file e rientrare.
 *
 * ⚠️⚠️ **NON È UN `TextButton`, E LA RAGIONE È LA STESSA DEI GETTONI DEI MODULI**: quel pezzo di
 * Material prende il suo `onClick` e non offre un secondo gesto, quindi il tocco lungo andrebbe
 * messo con un `pointerInput` nel modificatore, cioè in un **secondo nodo** che consuma il tocco
 * prima che il tasto lo veda. Con `combinedClickable` i gesti sono due e il bersaglio resta uno.
 *
 * ⚠️ **L'etichetta del gesto lungo si DICHIARA**, o resta una scorciatoia che esiste solo per chi
 * l'ha letta qui: è quello che un lettore di schermo annuncia fra le azioni disponibili, ed è la
 * stessa regola di [PadAction.holdLabel].
 *
 * ⚠️ **Il rientro e il corpo sono quelli di un `TextButton`**, perché il tasto è lo stesso di
 * prima e quello dell'editor di casa non è cambiato: due parole 'Salva' di misura diversa a una
 * schermata di distanza si vedrebbero.
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
                onClick = onSave
            )
            .padding(horizontal = TEXT_BUTTON_PAD, vertical = TEXT_BUTTON_PAD / 2)
    )
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
    /**
     * I quattro angoli mentre lo strumento **Angoli** è armato, e `null` quando non lo è.
     *
     * ⚠️⚠️ **ARMATO, IL PALCO FA SOLO QUESTO**: pinza, panoramica, doppio tocco e confronto restano
     * fermi, come col colore mirato e col Ritaglio, e per la stessa ragione, cioè che in questo
     * rilevatore ogni gesto nasce dallo stesso dito che scende.
     * ⚠️⚠️ **E L'IMMAGINE SI VEDE INTERA E RIMPICCIOLITA**: con la scala di copertura un angolo
     * tirato in fuori finisce oltre il bordo dello schermo, cioè proprio la maniglia che si sta
     * tirando esce dall'inquadratura. Il riquadro che resterà si vede lo stesso, disegnato sopra
     * (vedi [ARMED_FIT]).
     */
    corners: Corners?,
    onCorners: (Corners) -> Unit,
    /** Il gesto su un angolo è finito: quello che si è fatto diventa un passo della storia. */
    onCornersEnd: () -> Unit,
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
    /** Gli angoli di adesso e dove scriverli, per chi li legge dentro un gesto: vedi [geoNow]. */
    val cornerNow by rememberUpdatedState(corners)
    val cornerTo by rememberUpdatedState(onCorners)
    val airPx = with(LocalDensity.current) { CROP_AIR.toPx() }

    /**
     * L'aria da lasciare intorno all'immagine: quanta ne chiedono le squadrette del ritaglio.
     *
     * ⚠️⚠️ **VALE SEMPRE DALLA `2.37`, ANCHE NEI SEI MODULI CHE SQUADRETTE NON NE HANNO, ED È IL
     * PUNTO C DEL SUO CAMPO LIBERO** (giro della `2.36`: *Consideralo un anti-jitter tra moduli: al
     * cambio da un altro modulo al ritaglio, l'immagine NON deve rimpicciolirsi, il che significa
     * che le maniglie dell'area di ritaglio devono essere ESTERNE allo spazio dedicato
     * all'anteprima immagine*). Fino alla `2.36` valeva zero fuori dal Ritaglio, e quello che
     * sembrava un risparmio era un **salto**: entrando in quel modulo l'immagine perdeva cinque
     * punti per lato e si rimpiccioliva sotto gli occhi, cioè in orizzontale lo stesso ballo che la
     * `2.33` aveva tolto in verticale.
     * ⚠️ **Quello che costa è dichiarato**: negli altri sei moduli l'immagine è più piccola di
     * cinque punti per lato di quanto sarebbe, cioè meno dell'uno per cento su uno schermo da
     * telefono. È il prezzo di una misura che non cambia mai, ed è lo stesso baratto di
     * [SteadyBody].
     * ⚠️ **È una funzione e non un valore**, per la stessa ragione di [geoNow]: la leggono il
     * disegno e i gesti, che vivono fuori dalla composizione e vogliono il valore di adesso.
     */
    fun air(): Float = airPx
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
    /*
     * ⚠️⚠️ **QUI VIVEVA LA LENTE DEL COLORE MIRATO, E DALLA `2.35` NON C'È PIÙ: È LA SUA RISPOSTA
     * `via` A `d-mirino-resta`** (giro della `2.34`, con la sua ragione scritta nella scelta: *Il
     * colore mirato resta, ma senza il tondo ingrandito: il dito sceglie e basta*, e nel giro
     * prima *continuo a non essere sicuro che funzioni come mi aspetto*). Era nata nella `2.24` per
     * mostrare il pixel che il dito copre, e in quattro versioni aveva preso il trascinamento,
     * l'anello colorato, il pezzo a piena risoluzione e una tela sua.
     * ⚠️ **Il colore mirato non perde niente**: sceglie la fascia del pixel sotto il dito come
     * prima, e continua a leggerlo dal pezzo a risoluzione piena quando c'è (la nota della `2.28`
     * su `colourAt`). Quello che se ne va è il **disegno**, cioè il tondo, il suo mirino e la
     * seconda tela che esisteva per ridisegnarlo senza rifare il conto dello sviluppo.
     * ⚠️ **Chi la volesse rimettere la ritrova nella storia git**, misure comprese: sono
     * `lens`, `aimTint` e le costanti `LENS_*`.
     */
    /** La corsa del doppio tocco, tenuta per poterla fermare appena un dito scende. */
    var ride by remember(picture) { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val wide = posed.width.toFloat() / posed.height

    /**
     * Il taglio che il palco **inquadra**, o `null` se si vede l'immagine intera.
     *
     * ⚠️⚠️ **DALLA `2.33`, ED È IL SUO 'APPLICA'** (2026-09-13: *manca 'Applica' per il
     * ritaglio*). Fino alla `2.32` il rettangolo si tirava e non si vedeva mai applicato: il
     * taglio compariva solo nel file salvato.
     * ⚠️⚠️ **E DALLA `2.40` VALE ANCHE DENTRO IL RITAGLIO, ED È LA SUA RICHIESTA** (2026-09-14:
     * *Potrebbe avere senso se fosse applicato effettivamente anche nel modulo Ritaglio (resta
     * solo la parte ritagliata)*). Fino alla `2.39` là l'immagine tornava intera, quindi il tasto
     * non faceva niente che si vedesse proprio nel modulo in cui lo si tocca; adesso le squadrette
     * ripartono ai bordi della porzione e si ritaglia dentro quella, e a tornare indietro sono i
     * tre comandi suoi (vedi [Framing]).
     */
    val framed = look.framing.shown

    /**
     * Quanto è larga rispetto all'alta la porzione che si vede: l'immagine intera, o il taglio.
     *
     * ⚠️ **È questo numero a entrare nella stanza**, non l'aspetto dell'immagine: con un taglio
     * confermato quello che deve stare dentro il palco è lui, e dove finisca l'immagine intera lo
     * ricava [spread].
     */
    val shown =
        if (framed == null) wide
        else wide * (framed.right - framed.left) / (framed.bottom - framed.top)

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
     * **disegno** e non in composizione, ed è quello che tiene un gesto a costo zero. Metterli fra le
     * chiavi di un effetto li porterebbe in composizione, cioè
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
     * ⚠️ **E con lo strumento 'Angoli' armato per la stessa ragione**, dalla `2.50`: le quattro
     * maniglie vivono agli angoli dell'immagine, e con l'immagine ingrandita starebbero fuori dallo
     * schermo tutte e quattro.
     */
    LaunchedEffect(corners != null) {
        if (corners != null) {
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
        val view = viewport(stage, shown, scale, shift, air(), framed)
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
     * [dove], col filtro lineare o con quello a pixel interi. [dentro] dice dove finisce
     * l'immagine **intera** sullo schermo, e quanto misura.
     *
     * ⚠️⚠️ **È UNA FUNZIONE DALLA `2.24` PERCHÉ I RETTANGOLI SONO PIÙ DI UNO**: il palco, la
     * lente del colore mirato, e dalla `2.27` il pezzo letto a risoluzione piena. Scritta più
     * volte, la seconda copia mostrerebbe un'immagine sviluppata in un altro modo il giorno
     * che una delle due cambia, ed è esattamente il genere di divergenza che l'editor completo
     * esiste per non avere.
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
         * ⚠️⚠️ **QUELLO CHE SI CONSEGNA È IL RIQUADRO DELL'IMMAGINE INTERA COME È DISEGNATA, e
         * non quello della mappa di pixel né quello del pezzo**: il Dettaglio e i primi tre
         * cursori degli Effetti ragionano in frazioni del lato, la vignettatura e la grana
         * chiedono anche **dove** cade il punto, e qui il conto gira nello spazio dello schermo.
         * Così l'ingrandimento ingrandisce anche il risultato del filtro invece di cambiarlo, e
         * un pezzo disegnato da solo riceve lo stesso raggio e lo stesso centro del tutto:
         * consegnando la misura del pezzo, il filtro cambierebbe forza mentre si sposta la
         * panoramica e la vignettatura seguirebbe il dito.
         * ⚠️ **Con la geometria mossa `p` arriva comunque di qui**: `drawVertices` campiona lo
         * shader alle coordinate di **texture**, cioè su questo riquadro, e a spostarsi sono i
         * soli vertici. Quindi questi due cursori lavorano prima della deformazione sul palco
         * come nel salvataggio, dove la maglia si disegna dopo lo shader.
         */
        val shader = if (look.plain) null else lookShader(image, look, dentro)
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
                    shift = reined(from + (shift - from) * grown + pan, next, room, shown, air())
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
                    val view = viewport(room, shown, scale, shift, air(), framed)
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
                        val vista = viewport(room, shown, scale, shift, air(), framed)
                        /*
                         * ⚠️⚠️ **LE SQUADRETTE SI TIRANO DENTRO LA PORZIONE APPLICATA, DALLA
                         * `2.40`**: quello che si vede è il taglio confermato, quindi il riquadro
                         * del gesto è il suo e il rettangolo che si muove è in frazioni **di lui**.
                         * Senza le due conversioni il dito tirerebbe un rettangolo dell'immagine
                         * intera dentro un riquadro che ne mostra un pezzo, cioè il rettangolo
                         * scapperebbe da sotto le dita.
                         * ⚠️ **Senza niente applicato è un ramo solo**: le due funzioni sono
                         * l'identità, e il riquadro resta quello dell'immagine.
                         */
                        val dentro = cutout(vista, framed)
                        val frame = Rect(dentro.left, dentro.top, dentro.right, dentro.bottom)
                        var going = cropBox(relativeTo(framed, taglio), frame)
                        val presa = grabbed(down.position, going, GRIP.toPx(), keepNow == null)
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
                                cutTo(absolute(framed, cropFractions(going, frame)))
                            }
                            onCutEnd()
                        }
                        return@awaitEachGesture
                    }
                    /*
                     * ⚠️⚠️ **LO STRUMENTO 'ANGOLI' TIRA UN VERTICE PER VOLTA, DALLA `2.50`, ED È SUA
                     * RICHIESTA** (campo libero del giro della `2.40`, punto 5: *deformare
                     * l'immagine trascinando un angolo per volta*). Il gesto è quello delle
                     * squadrette del ritaglio, con due differenze: quello che si muove è un vertice
                     * dell'immagine invece di un lato del rettangolo, e la presa è un tondo invece
                     * di una squadretta.
                     * ⚠️ **Un dito che scende lontano da una maniglia non fa niente**, come nel
                     * Ritaglio: l'alternativa sarebbe deformare dal punto toccato, cioè un angolo
                     * che salta sotto il dito.
                     */
                    val angoli = cornerNow
                    if (angoli != null) {
                        val vista = viewport(room, shown, scale, shift, air(), framed)
                        val piano = Warp.plan(
                            geoNow, vista.centerX(), vista.centerY(),
                            vista.width(), vista.height(), hold = ARMED_FIT
                        )
                        val presa = nearestCorner(
                            down.position, cornerSpots(piano, vista), GRIP.toPx()
                        )
                        /*
                         * ⚠️ **Quanti pixel vale un'unità isotropa**: la mappa finale posa un punto
                         * a `centro + n * cover * half`, quindi è quel prodotto a convertire il
                         * passo del dito nello scarto dell'angolo. Con la scala di lavoro ferma
                         * durante il gesto (vedi [ARMED_FIT]) il fattore non cambia, e la maniglia
                         * va dove va il dito.
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
                                 * ⚠️⚠️ **IL DITO SI FERMA DOVE IL QUADRILATERO SI ROVESCEREBBE**, e
                                 * non è il valore che si rifiuta: scrivendolo e poi scartandolo,
                                 * l'angolo scatterebbe indietro appena passa il confine. Il perché
                                 * quel confine esista vive su [Warp.convex].
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
                         * ⚠️⚠️ **IL DITO SI TRASCINA E LA SCELTA ARRIVA QUANDO SI ALZA, DALLA
                         * `2.25`, ED È SUA ISTRUZIONE** (giro della `2.24`, voce `mirato-lente` non
                         * approvata: *dev'essere possibile trascinare il 'mirino', perché
                         * difficilmente con il dito si azzecca il punto giusto al primo colpo*).
                         * Quello che conta è dove il dito **ha finito**, non dove aveva cominciato.
                         * ⚠️⚠️ **E DALLA `2.32` NON C'È NESSUNA ATTESA DA ARMARE**: fino alla `2.31`
                         * un secondo e due decimi armavano il trascinamento per le Curve, e con loro
                         * fuori dal mirato (sua risposta `via` a `d-mirato-curve`) quel ramo non
                         * aveva più nessun dito che lo potesse raggiungere.
                         * ⚠️⚠️ **DALLA `2.35` QUESTO GESTO NON DISEGNA PIÙ NIENTE**, ed è la sua
                         * risposta `via` a `d-mirino-resta`: il pixel si prende come prima, e il
                         * tondo che lo mostrava se n'è andato (vedi la nota dov'era il suo stato).
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
                                            next, room, shown, air()
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
                                            else reined(anchor * (1f - ZOOM_TAP), ZOOM_TAP, room, shown, air())
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
        val view = viewport(room, shown, scale, shift, air(), framed)

        /** Dove finisce l'immagine intera sullo schermo adesso, e quanto misura. */
        val dentro = Framed.shown(view.left, view.top, view.width(), view.height())

        /*
         * ⚠️⚠️ **CON LA GEOMETRIA MOSSA NON SI DISEGNA UN RETTANGOLO MA UNA MAGLIA, DALLA `2.29`**,
         * e il pennello è lo stesso: quello che cambia è **dove** finisce ogni pixel, non di che
         * colore è. La maglia la costruisce `Warp`, cioè lo stesso conto che il salvataggio applica
         * al file pieno: una fonte, due lettori.
         * ⚠️ **A geometria ferma resta il rettangolo di sempre**, e non è un'ottimizzazione: mille
         * triangoli per disegnare un rettangolo sarebbero mille occasioni di una cucitura che a
         * rettangolo non esiste.
         */
        /*
         * ⚠️⚠️ **CON LO STRUMENTO 'ANGOLI' ARMATO SI DISEGNA LA VISTA DI LAVORO, DALLA `2.50`**:
         * l'immagine intera rimpicciolita invece della sola parte che resta, o la maniglia che si
         * tira uscirebbe dallo schermo. Il perché per esteso vive sul parametro `hold` di
         * [Warp.plan], e il riquadro che resterà si disegna sopra.
         */
        val armato = corners != null
        fun stendi(mappa: Bitmap, dove: RectF, nitido: Boolean) {
            val paint = pennello(mappa, dove, nitido, dentro)
            if (look.geo.idle && !armato) {
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
                look.geo, view.centerX(), view.centerY(), view.width(), view.height(),
                hold = if (armato) ARMED_FIT else null
            )
            clipRect(dove.left, dove.top, dove.right, dove.bottom) {
                drawIntoCanvas { tela ->
                    Warp.draw(tela.nativeCanvas, dove, piano, paint.asFrameworkPaint())
                }
            }
        }

        /*
         * ⚠️⚠️ **COL TAGLIO CONFERMATO SI DISEGNA L'IMMAGINE INTERA E SE NE VEDE UNA PORZIONE,
         * DALLA `2.33`**: [view] resta il riquadro dell'immagine tutta quanta, che è quello su cui
         * tutti gli altri conti del palco si reggono, e a fare il taglio è il confine. Disegnarne
         * una copia già ritagliata vorrebbe dire un secondo bitmap a ogni fotogramma, e con lui
         * due geometrie da tenere d'accordo.
         * ⚠️ **A taglio non confermato il confine è il riquadro stesso**, quindi qui non cambia
         * niente: `cutout` con `null` risponde quello che riceve.
         */
        val visto = cutout(view, framed)
        clipRect(visto.left, visto.top, visto.right, visto.bottom) {
            stendi(posed, view, false)
        }

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
                    pennello(fine.pixels, dove, false, dentro)
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
            /*
             * ⚠️ **Il riquadro è quello della porzione applicata**, cioè lo stesso che riceve il
             * gesto: il disegno e il dito devono dire la stessa cosa, e con due conti diversi le
             * squadrette si vedrebbero dove il dito non le prende.
             */
            val frame = Rect(visto.left, visto.top, visto.right, visto.bottom)
            cropOverlay(
                frame,
                cropBox(relativeTo(framed, taglio), frame),
                HANDLE_ARM.toPx(),
                HANDLE_THICK.toPx(),
                GRIP_HALO.toPx(),
                keep == null
            )
        }

        /*
         * ⚠️⚠️ **LE QUATTRO MANIGLIE DEGLI ANGOLI, DALLA `2.50`, E IL RIQUADRO CHE RESTERÀ**: qui
         * si vede l'immagine intera rimpicciolita (la vista di lavoro), quindi il velo dice quello
         * che l'auto-ritaglio porterà via. Senza quel riquadro lo strumento chiederebbe di tirare
         * un angolo senza dire che cosa si perde, che è metà della sua richiesta.
         * ⚠️ **Il riquadro tenuto si RICAVA dalle due scale e non è un secondo conto**: quello che
         * resta è il rettangolo che la copertura riempie, quindi nella vista di lavoro è lo stesso
         * rettangolo scalato del rapporto fra le due. Con un conto suo, il velo direbbe una cosa e
         * il file ne porterebbe un'altra.
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
                keep = tenuto,
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
 * ⚠️ **È lo stesso ordine di [Corners]**, e non è una coincidenza da mantenere a memoria: l'indice
 * che esce da [nearestCorner] finisce dritto in `Corners.with`, quindi due ordini diversi
 * muoverebbero l'angolo sbagliato senza dare nessun errore.
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
 * Il velo, il riquadro che resterà e le quattro maniglie dello strumento 'Angoli'.
 *
 * ⚠️ **Le maniglie si disegnano per ultime**, quindi restano in vista anche quando l'angolo cade
 * fuori dal riquadro tenuto, cioè proprio nel caso in cui si sta tirando.
 * ⚠️ **Il contorno dell'immagine deformata è più tenue del riquadro**: sono due cose diverse, e il
 * secondo è quello che conta, cioè quello che resterà nel file.
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
 * ⚠️⚠️ **IL NUMERO È IL GUINZAGLIO LETTO AL ROVESCIO**: un angolo può allontanarsi di
 * [Warp.PULL] unità isotrope, cioè arrivare a `1 + PULL` semilati dal centro, quindi perché la
 * maniglia resti dentro il palco la vista deve stare sotto `1 / (1 + PULL)`. Con `PULL` a 0,35
 * quel confine vale 0,74, e questo numero ci sta sotto con un filo di margine.
 * ⚠️ **Non si misura sul contorno deformato**, e la ragione è sul parametro `hold` di [Warp.plan]:
 * una scala che seguisse gli angoli farebbe stringere l'immagine di quanto la maniglia avanza,
 * cioè la terrebbe incollata al bordo.
 */
private const val ARMED_FIT = 0.7f

/** Il raggio del tondo di una maniglia d'angolo: un tondo dice 'portami dove vuoi'. */
private val CORNER_GRIP = 7.dp

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
 * ⚠️⚠️ **E DALLA `2.37` VALE IN TUTTI E SETTE I MODULI, ANCHE DOVE SQUADRETTE NON CE NE SONO**: la
 * nota di allora diceva che fuori dal Ritaglio sarebbe stata spazio tolto per niente, e guardava un
 * modulo per volta invece del passaggio da uno all'altro. Il perché per esteso vive su `air()`,
 * dentro il palco.
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
 * ⚠️⚠️ **È IL PEZZO CHE FA VEDERE IL RITAGLIO SENZA TOCCARE UN ALTRO CONTO, DALLA `2.33`**: tutto
 * il palco (il pezzo a risoluzione piena, la lente, il colore mirato, la maglia della geometria)
 * ragiona su *dove finisce l'immagine intera sullo schermo*, e quel riquadro qui continua a
 * esistere: quello che cambia è che adesso si ricava da dove deve cadere la **porzione** scelta,
 * invece che dalla stanza. Cambiando il significato di quel rettangolo si sarebbero dovuti
 * riscrivere tutti gli altri.
 * ⚠️ **L'inversa è [cutout]**, che dal riquadro dell'immagine intera dice dove cade il taglio: è
 * quella che serve al disegno, perché è il confine oltre il quale non si deve vedere niente.
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
 * ⚠️⚠️ **È LA COPPIA DI [absolute], E LE DUE NASCONO COL RITAGLIO CHE TAGLIA DAVVERO** (`2.40`):
 * col taglio applicato le squadrette si tirano dentro la porzione, quindi il rettangolo che il
 * dito muove va letto e scritto **in frazioni di lei**, mentre [Look.crop] resta quello che il
 * salvataggio applica, cioè il rettangolo totale.
 * ⚠️ **Senza niente applicato sono l'identità**, e questo è quello che tiene un ramo solo sul
 * palco: `null` vuol dire 'la porzione è tutta l'immagine'.
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
    /**
     * Il gettone con cui questo modulo si salva, dalla `2.34`.
     *
     * ⚠️⚠️ **ESISTE PERCHÉ LA FILA SI RIORDINA** (sua richiesta, 2026-09-13: *voglio poter
     * ordinare anche i pulsanti dei moduli*), e l'ordine di un riquadro nell'archivio è un
     * elenco di gettoni: senza una chiave, un modulo si potrebbe salvare solo per indice, cioè
     * con un numero che cambia significato il giorno che se ne aggiunge uno.
     * ⚠️ **È un [PadKey] e non un enum suo**: il riordino, la replica nelle impostazioni e le
     * azioni parlate sono quelli dei quattro riquadri di casa, e un secondo tipo vorrebbe dire
     * un secondo meccanismo identico da tenere allineato.
     */
    val key: PadKey,
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
     * ⚠️⚠️ **È UNA LAMBDA E NON UN `ImageVector`, DALLA `2.32`, PERCHÉ CINQUE GLIFI SU SETTE
     * VIVONO IN `res/`**: là il disegno si legge con `vectorResource`, che è `@Composable`,
     * mentre questa tabella è una costante di file. La lambda si valuta dove il gettone si
     * compone, cioè dove quella lettura ha il suo `remember`.
     */
    val icon: @Composable () -> ImageVector,
    /** Che cosa questo modulo ha in più dei suoi cursori: vedi [Extra]. */
    val extra: Extra = Extra.NONE,
    /**
     * Se il tasto 'Auto' ha senso mentre si guarda questo modulo, dalla `2.50`.
     *
     * ⚠️⚠️ **È SUA RICHIESTA, E IL CRITERIO È CHE COSA QUEL TASTO TOCCA** (campo libero del giro
     * della `2.40`, punto 5: *l'icona di 'Auto' deve apparire solo se è attivo 'Luce' o
     * 'Colore'*): quel comando scrive in sei cursori, i quattro della Luce e i due del
     * bilanciamento del bianco, quindi in un altro modulo cambia dei numeri che non si vedono.
     * ⚠️ **Vive nella tabella e non in un `when` della scheda**, come la condizione del colore
     * mirato: la domanda 'questo modulo offre quel comando?' si fa in un posto solo.
     */
    val auto: Boolean = false
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
    CROP,

    /**
     * Lo strumento **Angoli** e le sue quattro maniglie sul palco, cioè la Geometria, dalla `2.50`.
     *
     * ⚠️⚠️ **È IL SECONDO CHE CAMBIA QUELLO CHE IL PALCO FA COL DITO, MA A COMANDO**: il Ritaglio
     * prende il dito appena è in scena, questo lo prende quando lo strumento è **armato**, come il
     * colore mirato. La ragione è che qui i cursori sono cinque e si tarano guardando l'immagine da
     * vicino: togliere pinza e panoramica a chi raddrizza un orizzonte sarebbe un peggioramento.
     */
    CORNERS,

    /**
     * L'elenco degli **stili**, cioè il modulo dei preset, dalla `2.50`.
     *
     * ⚠️⚠️ **È IL PRIMO CORPO CHE RICEVE L'ALTEZZA INVECE DI DETTARLA**: gli altri sette sono
     * alti quanto il loro contenuto e il più alto detta la misura di tutti (vedi [SteadyBody]),
     * mentre questo è un elenco che cresce con quello che l'utente salva. Scorre dentro l'altezza
     * comune, e per questo resta fuori dalla misura: contarlo vorrebbe dire una scheda che si
     * alza a ogni stile salvato, anche per chi gli stili non li usa.
     */
    PRESETS;

    /**
     * Se questo modulo dice **dove** va un pixel invece di che colore è.
     *
     * ⚠️⚠️ **LO DICHIARA LA TABELLA DEI MODULI E NON UN ELENCO DI NOMI, dalla `2.58`**: è lo
     * stesso criterio del colore mirato e delle squadrette del ritaglio, e serve al confronto col
     * prima (vedi [Look.place]). Scritto come un `if` accanto al palco, un modulo nuovo che
     * spostasse i pixel si ritroverebbe il confronto sbagliato senza che niente dia errore.
     * ⚠️ **Sono i due che hanno il dito sull'immagine**, e non è una coincidenza: un modulo che
     * dice dove va un pixel si tara guardando i bordi, quindi i suoi comandi vivono sul palco.
     */
    val places: Boolean get() = this == CROP || this == CORNERS
}

/**
 * I cursori che il **bianco e nero** spegne: là non c'è più niente da saturare, e un cursore che
 * si muove senza cambiare l'immagine si legge come un guasto.
 */
private val MONO: (Look) -> Boolean = { it.chroma.mono }

/**
 * Il rovescio di [MONO]: la riga che governa qualcosa **solo** col bianco e nero, cioè il
 * 'Filtro BN'.
 *
 * ⚠️⚠️ **SPEGNE, E DALLA `2.36` ALLA `2.37` NASCONDEVA**: la `2.36` toglieva quella riga dalla
 * scena a colori (*'Filtro' deve apparire solo quando l'interruttore 'Bianco e nero' è acceso*), e
 * il punto A del campo libero del giro dopo la rimette in scena dicendo dove deve stare: *'Filtro'
 * diventa 'Filtro BN' ... e va posizionato (non attivo) DOPO l'interruttore 'Bianco e nero'*. Cioè
 * quello che chiedeva non era una riga che sparisce, era una riga che si legge come la conseguenza
 * del comando che la governa.
 * ⚠️ **Con lei esce il meccanismo che la nascondeva** (`Dial.hide`, e la misura col corpo pieno di
 * [SteadyBody] che esisteva per pagarlo): non aveva più nessun altro chiamante, e un meccanismo
 * senza chiamanti è codice morto.
 */
private val NOT_MONO: (Look) -> Boolean = { !it.chroma.mono }

/**
 * I cursori che governano la **maschera di contrasto**, e che senza di lei non governano niente.
 *
 * ⚠️ **Sono due dei cinque del Dettaglio**, il raggio e la mascheratura: non sono quantità, sono
 * come la nitidezza lavora. Con la nitidezza a zero non c'è nessuna maschera da governare.
 */
private val UNSHARP: (Look) -> Boolean = { it.detail.flat }

/**
 * Le righe che governano la **grana**, cioè 'Dimensione' e 'Luci', e non hanno niente da fare
 * finché quel cursore è a zero.
 *
 * ⚠️ **È il criterio di [UNSHARP] applicato agli Effetti**, dalla `2.66`: un cursore che dice
 * *come* lavora un altro si spegne quando l'altro non lavora.
 */
private val NO_GRAIN: (Look) -> Boolean = { it.effects.noGrain }

/** Come [NO_GRAIN], per la 'Sfumatura' della vignettatura. */
private val NO_VIGNETTE: (Look) -> Boolean = { it.effects.noVignette }

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

/**
 * Il cursore del **filtro del bianco e nero**, che è l'unica riga di un modulo a **non** venire
 * dopo tutte le altre nel disegno: davanti a lei si posa l'interruttore del bianco e nero.
 *
 * ⚠️⚠️ **VIVE A PARTE PERCHÉ IL CORPO LA RICONOSCE PER IDENTITÀ E NON PER INDICE** (`knob ===
 * FILTER_ROW`): un numero di riga scritto là dentro direbbe il vero finché nessuno tocca l'ordine
 * di [COLOUR_ROWS], e il giorno che qualcuno ci infila un cursore l'interruttore comparirebbe in
 * mezzo a due manopole senza che niente dia errore.
 * ⚠️ **È la riga che il punto A del giro della `2.36` ha spostato**: il perché per esteso vive su
 * [NOT_MONO] e su [COLOUR_ROWS].
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
 * ⚠️⚠️ **IL QUINTO È IL 'FILTRO' E LAVORA SOLO COL BIANCO E NERO ACCESO, DALLA `2.35`** (sua
 * richiesta, 2026-09-13: *se attivo 'bianco e nero', voglio che appaia uno slider 'Filtro' che
 * definisca la resa del bianco e nero in base a come sono mappati i colori nell'output*). Che cosa
 * fa, e perché è un asse e non una ruota, vive su [Chroma.grey].
 * ⚠️⚠️ **DALLA `2.37` SI CHIAMA 'FILTRO BN', C'È SEMPRE E VIVE SOTTO L'INTERRUTTORE**, ed è il punto
 * A del campo libero del giro della `2.36`: *'Filtro' diventa 'Filtro BN' ... e va posizionato (non
 * attivo) DOPO l'interruttore 'Bianco e nero'. Si attiva solo con l'interruttore ON*. La `2.36` la
 * nascondeva a colori, e questo la rimette in scena spenta: il perché vive su [NOT_MONO], e come
 * l'interruttore le finisce davanti su [FILTER_ROW].
 * ⚠️ **Gli altri quattro restano sopra l'interruttore**, che è l'ordine dei comandi di questo
 * modulo da sempre: prima i cursori del colore, poi la riga che accende, e sotto di lei il solo
 * comando che da quella riga dipende.
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
 * I sei cursori degli **Effetti**: foschia, grana coi suoi due comandi, vignettatura col suo.
 *
 * ⚠️⚠️ **L'ELENCO E IL SUO ORDINE SONO SUOI** (`d-dopo-editor`, giro della `2.50`: *'Effetti', con
 * 'Chiarezza', 'Texture', 'Foschia', `Grana` e `Vignettatura`*): la `2.53` ha portato i primi due,
 * la `2.54` il terzo e la `2.57` gli ultimi due, che è il *procediamo un po' alla volta* della sua
 * istruzione dello stesso giorno.
 *
 * ⚠️⚠️ **E DALLA `2.64` I PRIMI DUE NON CI SONO PIÙ, ED È LA SUA RISPOSTA `via` A
 * `d-eff-restano`** (giro della `2.63`: *Toglili tutti e due*). Il perché, e il difetto misurato
 * che li ha tolti, vivono in testa a [Effects].
 *
 * ⚠️⚠️ **E DALLA `2.66` I TRE CHE RESTANO PORTANO TRE CURSORI SECONDARI, ED È SUA RICHIESTA**
 * (campo libero del giro chiuso il 2026-09-19: *Prima di chiudere il modulo Effetti voglio fare
 * una cosa che ti avevo anticipato, ovvero raffinarli con parametri aggiuntivi. Rimane spazio
 * verticale per tre slider secondari, uno per effetto, che si dovrà attivare solo se l'effetto
 * relativo sta modificando l'immagine*).
 * - ⚠️⚠️ **'UNO PER EFFETTO' E IL SUO ELENCO NON DÀNNO LO STESSO CONTO, E VINCE L'ELENCO**: la
 *   frase dice uno per effetto, e le tre voci che scrive sotto sono **due** per la grana
 *   ('Dimensione' e 'Luci') e **una** per la vignettatura ('Sfumatura'), quindi alla foschia non
 *   ne tocca nessuna. Il numero torna, la ripartizione no, e l'elenco è la parte dettagliata:
 *   ognuna delle tre porta la sua ragione scritta, mentre 'uno per effetto' è il conto dello
 *   spazio verticale. La voce di collaudo gli dice questa lettura in chiare lettere.
 * - ⚠️ **Ognuno vive SOTTO il cursore che lo governa**, cioè si legge come la sua conseguenza: è
 *   il criterio con cui il 'Filtro BN' è finito sotto l'interruttore del bianco e nero nella
 *   `2.37`, e non una scelta nuova.
 * - ⚠️ **Si spengono quando il loro principale è a zero**, che è la seconda metà della sua
 *   richiesta, e il meccanismo è quello della maschera di contrasto senza nitidezza ([UNSHARP]).
 *
 * ⚠️⚠️ **TRE SONO BIPOLARI E DUE NO, E NON È UNA DIMENTICANZA**: verso il basso la foschia si
 * **aggiunge** invece di essere tolta, la vignettatura **apre** l'angolo invece di chiuderlo (che
 * è quello che si fa su una fotografia già vignettata dall'obiettivo) e la sua 'Sfumatura' porta
 * l'alone verso il bordo invece che verso il centro. La grana invece non ha un verso negativo che
 * voglia dire qualcosa (un grano tolto non esiste, e quello che spiana la grana è la riduzione del
 * rumore del Dettaglio), e le sue 'Luci' partono da zero perché lo zero è il valore di fabbrica
 * che lui ha chiesto. ⚠️ **'Dimensione' è bipolare come il raggio del Dettaglio**, perché una
 * misura di quel genere si dimezza e si raddoppia attorno al valore di serie.
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
     * ⚠️⚠️ **L'ORDINE È IL SUO, DALLA `2.35`, E LO HA DETTATO PER ESTESO** (2026-09-13: *voglio
     * cambiare anche l'ordine dei moduli: per impostazione predefinita, da sinistra a destra,
     * dev'essere: dettagli, curve, geometria, ritaglio (nuovo default attivo all'avvio), luce,
     * contrasto, HSL*). Fino alla `2.34` la fila cominciava dal Ritaglio e finiva col Dettaglio,
     * cioè l'ordine che aveva dettato per la `2.31`.
     * ⚠️⚠️ **'CONTRASTO' È IL MODULO COLORE, E LA LETTURA È PER ESCLUSIONE**: i moduli sono sette
     * e lui ne nomina sette, sei col loro nome; quello che resta è il Colore, e nessun altro può
     * stare in quel posto. La voce di collaudo glielo chiede in chiare lettere.
     * ⚠️ **Il posto nella fila non è il posto nella catena**, e dalla `2.31` non coincidono: la
     * fila è l'ordine in cui si lavora, la catena l'ordine in cui il conto gira (il Dettaglio è
     * primo là perché legge i pixel del file, il Ritaglio ultimo perché taglia quello che il
     * resto ha prodotto).
     * ⚠️⚠️ **E DALLA `2.55` LE CURVE E GLI EFFETTI SI SONO SCAMBIATI LA CASELLA, SU SUA
     * ISTRUZIONE** (riscontro del giro della `2.54`: *il nuovo ordine dei moduli dev'essere:
     * Dettagli, Effetti, Geometria, Ritaglio, Luce, Colore, HLS, Curve, Stili*). Quindi la frase
     * del 2026-09-13 qui sopra resta la fonte di tutto il resto, e di suo cade il solo posto
     * delle Curve.
     * ⚠️ **Questo elenco e `MOD_KEYS` si riordinano insieme**, o le note che dicono 'nell'ordine
     * in cui la fila li disegna' diventerebbero false: là vive il valore di fabbrica
     * dell'archivio, qui il nome e il glifo di ognuno.
     */
    /*
     * ⚠️⚠️ **QUESTO NELLA CATENA È IL PRIMO E NELLA FILA È DIVENTATO IL PRIMO ANCHE LUI**, ed è
     * una coincidenza e non una regola: quel modulo parla del **file** (quanto rumore ha il
     * sensore, quanto il disegno fine va accentuato), e va letto prima che il contrasto
     * moltiplichi la grana.
     */
    Module(
        PadKey.MOD_DETAIL,
        R.string.look_detail,
        rows = { DETAIL_ROWS },
        clear = { it.copy(detail = Detail.NONE) },
        spent = { !it.detail.idle },
        icon = { Glyphs.ModDetail }
    ),
    /*
     * ⚠️⚠️ **IL NONO VIVE QUI DALLA `2.55`, SUBITO DOPO IL DETTAGLIO, ED È SUA ISTRUZIONE**
     * (riscontro del giro della `2.54`: *il nuovo ordine dei moduli dev'essere: Dettagli, Effetti,
     * Geometria, Ritaglio, Luce, Colore, HLS, Curve, Stili*). Nella `2.53` e nella `2.54` stava
     * fra l'HSL e gli Stili.
     * ⚠️ **La ragione di allora è caduta e se ne ricava una migliore**: diceva che parlava dei
     * colori come i tre che lo precedevano, e adesso vive accanto all'unico altro modulo che
     * **guarda i pixel vicini**, cioè quello con cui divide il bordo delle tessere del
     * salvataggio (vedi `AdjustRender.bleedFor`).
     * ⚠️⚠️ **E IL GLIFO NON È PIÙ `Deblur`, DALLA `2.55`, PERCHÉ ERA QUELLO DEL DETTAGLIO**
     * (stesso riscontro, voce `eff-glifo`: *non va bene, perché è lo stesso di 'Dettagli'. Uno dei
     * due deve cambiare*): i due disegni sono la stessa nuvola di punti, e nella fila si toccano.
     * Con la `2.55` è diventato `Vignette`, che in quella fila non somiglia a nessuno.
     * ⚠️⚠️ **E DALLA `2.62` QUELLA VIGNETTATURA È LA SUA, CIOÈ UN DISEGNO DI `res/`**: gliel'ho
     * mandata come SVG alla fine del giro della `2.55` e lui l'ha rimodellata (2026-09-18), quindi
     * la nota che lo dava per un glifo di Material è superata. Le misure, lo scarto del trasporto
     * e la regola di riempimento vivono in testa a `ic_mod_effects.xml`.
     * ⚠️ **L'argomento di allora non cade, si avvera**: diceva che quel disegno è tutto di curve,
     * quindi l'arrotondamento a 0,4 lo lascia a **zero pixel** di scarto, e infatti il suo non ha
     * una punta da raccordare (`icon-round.py` risponde 0). A cambiare non è il trattamento, è chi
     * ha disegnato, che è il criterio di `AIV/CLAUDE.md` § '🖌️ Come entra un disegno'.
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
     * ⚠️⚠️ **QUESTO NON CAMBIA IL COLORE DI UN PIXEL**: gli altri sei dicono di che colore è un
     * pixel, questo dice dove va a finire, e la ragione per cui il suo conto si fa dopo lo shader
     * vive in testa a `Geometry.kt`.
     * ⚠️ **Non offre il colore mirato**, e la condizione se lo dice da sé: quel tasto vive nel solo
     * modulo che ha un colore da puntare, e mirare in un modulo che i colori non li tocca non
     * vorrebbe dire niente.
     * ⚠️⚠️ **MA DALLA `2.50` OFFRE LO STRUMENTO 'ANGOLI', CHE ARMA IL DITO SULL'IMMAGINE ALLO
     * STESSO MODO** (vedi [Extra.CORNERS]): il tasto è lo stesso della fila in fondo, e a dire
     * quale dei due gesti sia è questa tabella.
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
     * ⚠️⚠️ **QUESTO È APERTO DI FABBRICA, DALLA `2.35`, ED È SUA ISTRUZIONE** (*ritaglio (nuovo
     * default attivo all'avvio)*): fino alla `2.34` si apriva la Luce, che era la sua scelta per
     * la `2.31`. L'indice si **ricava** dalla tabella e non è un numero scritto a mano: vedi
     * [LOOK_FIRST].
     */
    Module(
        PadKey.MOD_CROP,
        R.string.look_crop,
        rows = { emptyList() },
        // ⚠️ Il 'Reset modulo' porta via anche i tagli applicati: senza, il palco resterebbe a
        // inquadrare un taglio che non c'è più, cioè l'immagine intera dentro un riquadro.
        // ⚠️ **Azzera anche la posa, al contrario del comando 'Azzera' della fila**: questo è il
        // gesto che rimette a nuovo il modulo, quello là guarda il solo ritaglio.
        clear = { it.copy(spin = Spin.STILL, crop = ImageEdit.Crop.WHOLE, framing = Framing.NONE) },
        spent = { !it.square },
        icon = { Glyphs.ModCrop },
        extra = Extra.CROP
    ),
    /*
     * ⚠️ **I DUE MODULI CHE 'Auto' TOCCA**, dalla `2.50`: quel comando scrive nei quattro cursori
     * della Luce e nei due del bilanciamento del bianco, quindi la sua icona compare mentre si
     * guarda uno di questi due e non altrove (vedi [Module.auto]).
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
        icon = { Icons.Filled.Palette },
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
     * ⚠️⚠️ **QUESTO NON HA CURSORI, ED È STATO IL PRIMO COSÌ**: quello che un cursore sa dire è
     * 'quanto', e una curva dice 'quanto per ogni tono', cioè una cosa che nessuna manopola può
     * esprimere. Il suo comando è il grafico, e la sua lista di righe è vuota.
     * ⚠️⚠️ **DALLA `2.55` È PENULTIMO E NON PIÙ SECONDO**, ed è la stessa istruzione che ha
     * portato gli Effetti al suo posto: i due si sono scambiati la casella. Chi legge una nota
     * che lo dà per secondo sappia che valeva dalla `2.35` alla `2.54`.
     */
    Module(
        PadKey.MOD_TONE,
        R.string.look_tone,
        rows = { emptyList() },
        clear = { it.copy(tone = Tone.NONE) },
        spent = { !it.tone.idle },
        icon = { Icons.Filled.Timeline },
        extra = Extra.CURVES
    ),
    /*
     * ⚠️⚠️ **L'OTTAVO È L'ELENCO DEGLI STILI, DALLA `2.50`, ED È SUA ISTRUZIONE** (campo libero
     * del giro della `2.40`, punto 1: *inserisci i modelli in un modulo a parte*). Fino alla
     * `2.40` gli stili si aprivano da un'icona in fondo alla scheda, cioè da una superficie in
     * più davanti all'immagine su cui si lavora.
     * ⚠️⚠️ **IL 'RESET MODULO' AZZERA I CINQUE MODULI DI COLORE, che è quello che uno stile
     * governa**: un preset non lascia un valore suo da rimettere a zero, lascia i cursori dove li
     * ha portati, quindi il tocco lungo su questo gettone vuol dire 'togli l'aspetto'. ⚠️ **La
     * posa, il ritaglio e la geometria restano**, per la stessa ragione per cui un preset non li
     * porta: dipendono da come è stata scattata quell'immagine.
     * ⚠️ **Il punto d'accento non si accende**, al contrario degli altri sette: si accenderebbe
     * per quello che hanno fatto loro, cioè direbbe 'questo modulo ha toccato l'immagine' anche a
     * chi non ha mai aperto l'elenco.
     * ⚠️ **Il glifo è di Material e nasce provvisorio**, come quello di 'Auto' nella `2.32`: se
     * non dice abbastanza, il giro di collaudo lo chiede e lui manda il suo.
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
        icon = { Icons.Filled.Style },
        extra = Extra.PRESETS
    )
)

/**
 * Il modulo che porta questa chiave.
 *
 * ⚠️⚠️ **LANCIA SE LA CHIAVE NON È DI UN MODULO, e non risponde `null`**: i chiamanti sono i
 * due `when` di `ActionPad.kt`, che ci arrivano dai soli sette rami delle chiavi dei moduli, e
 * la fila che la legge riceve un ordine già ripulito da `padOrderOf`. Un `null` da gestire
 * vorrebbe dire un ramo che nessuna strada può raggiungere, cioè codice morto; e che i due
 * elenchi si coprano lo misura il banco.
 */
private fun moduleOf(key: PadKey): Module = MODULES.first { it.key == key }

/**
 * Come si chiama un modulo, per chi lo deve nominare fuori dall'editor.
 *
 * ⚠️ **La fonte è la tabella e non un secondo elenco**: la pagina che riordina i riquadri, i
 * testi che la ricerca delle impostazioni confronta e la fila vera dicono tutti questa parola.
 */
@StringRes
internal fun modName(key: PadKey): Int = moduleOf(key).name

/** Il segno di un modulo, alla stessa fonte del nome: vedi [modName]. */
@Composable
internal fun modGlyph(key: PadKey): ImageVector = moduleOf(key).icon()

/**
 * Il posto di un modulo nella tabella, cioè quello che [Gaze.module] tiene.
 *
 * ⚠️⚠️ **L'ORDINE SCELTO È UNA PERMUTAZIONE DEL DISEGNO, E NON CAMBIA L'IDENTITÀ DI NIENTE**:
 * quello che si riordina è come i gettoni si vedono, mentre l'indice che lo sguardo porta resta
 * il posto nella tabella. Con un indice legato alla fila, spostare un gettone cambierebbe il
 * modulo aperto, e [LOOK_FIRST] direbbe un'altra cosa a ogni trascinamento.
 */
private fun modIndex(key: PadKey): Int = MODULES.indexOfFirst { it.key == key }

/**
 * Quante colonne ha la fila della posa, cioè quanti sono i suoi tasti.
 *
 * ⚠️ **È il numero e non un conto sull'elenco**, perché quell'elenco si compone dentro la scheda,
 * dove servono i chiamanti dei tre tasti: qui c'è la forma della fila, e là che cosa fa ognuno.
 */
private const val POSE_KEYS = 5

/**
 * Quanti gettoni di modulo entrano in uno schermo prima che la fila debba scorrere.
 *
 * ⚠️⚠️ **È LA MISURA DI PRIMA, NON UN TETTO, ED È QUELLO CHE HA CHIESTO LUI** (campo libero del
 * giro della `2.40`: *nel mio caso, con il mio schermo, sarà l'unico a richiedere uno scorrimento
 * a destra, ma va benissimo così*). Dividendo la larghezza per **otto** nessun gettone
 * sporgerebbe, e tutti si stringerebbero di un ottavo: la fila cambierebbe aspetto per fare posto
 * a un modulo, invece di continuare fuori dallo schermo.
 * ⚠️ **Con sette o meno moduli in scena non cambia niente**, perché là la cella si divide la
 * larghezza come ha sempre fatto: questo numero entra in funzione dall'ottavo in poi.
 */
private const val MOD_FIT = 7

/**
 * L'aria fra due gettoni della fila dei moduli.
 *
 * ⚠️ **La legge anche il velo del mini-onboarding**, che quella fila la ricopia sopra di sé: è la
 * stessa ragione per cui [modCell] è una funzione sola.
 */
internal val MOD_GAP = 4.dp

/**
 * Quanto è larga la cella di un gettone, data la larghezza [width] e quanti moduli sono in scena.
 *
 * ⚠️ **La leggono in due**, la fila vera e il mini-onboarding che la ricopia sopra il velo: con
 * due conti, il primo a divergere sarebbe quello del velo, cioè quello che si vede una volta sola
 * e che nessuno rimisura.
 */
internal fun modCell(width: Dp, count: Int): Dp {
    val quanti = minOf(count, MOD_FIT).coerceAtLeast(1)
    return (width - MOD_GAP * (quanti - 1)) / quanti
}

/**
 * Quanto è alta la riga dei quattro comandi del ritaglio: 'Indietro', 'Avanti', 'Applica' e
 * 'Azzera'.
 *
 * ⚠️⚠️ **È LA MISURA DI RIGA DI QUESTA SCHEDA E NON QUELLA DI UN TASTO, ED È UNA MISURA A
 * IMPORLA**: il corpo del Ritaglio aveva **30** punti liberi prima di superare le Curve, che dalla
 * `2.33` sono il modulo che detta l'altezza della scheda (misurato: Ritaglio 208 contro Curve 238).
 * Una cella di [ActionPad] ne chiede 64 anche senza etichetta e un `IconButton` di Material 48:
 * tutti e due avrebbero alzato la scheda **in tutti e sette i moduli**, che è il ballo che la
 * `2.33` esiste per togliere.
 * ⚠️ **Il bersaglio non è quello che si perde**: le quattro celle si dividono tutta la larghezza,
 * quindi ognuna è larga una settantina di punti, cioè più di un `IconButton`; l'altezza è sotto
 * i suoi 48 come già la riga di un cursore ([DIAL_ROW]), che porta la stessa nota.
 * ⚠️ **Sono quattro e non tre**, cioè i tre che ha chiesto lui più 'Applica': quello che si applica
 * e quello che si disfa sono lo stesso gesto in due versi, e tenerli separati vorrebbe dire cercare
 * il secondo in un'altra parte della scheda.
 */
private val CROP_CMD_ROW = 32.dp

/** Quanto è grande il glifo di un comando del ritaglio: la misura dei glifi di comando dell'app. */
private val CROP_CMD_ICON = 24.dp

/** Lo stondamento della cella di un comando del ritaglio, cioè quello dei chip di questa scheda. */
private val CROP_CMD_ROUND = 8.dp

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
internal fun spunLook(look: Look, gesto: Spin): Look = look.copy(
    spin = look.spin.then(gesto),
    crop = spunRect(look.crop, gesto),
    // ⚠️ **Anche i tagli applicati**, dalla `2.40`: sono rettangoli come [Look.crop] e vivono
    // nello stesso spazio, quindi una posa che riscrivesse solo lui lascerebbe il palco a
    // inquadrare un'altra porzione di fotografia (vedi [Framing.spun]).
    framing = look.framing.spun(gesto)
)

/**
 * Il modulo aperto di fabbrica, cioè il **Ritaglio** dalla `2.35`.
 *
 * ⚠️⚠️ **ERA LA LUCE FINO ALLA `2.34`, ED È SUA ISTRUZIONE** (2026-09-13: *ritaglio (nuovo default
 * attivo all'avvio)*), che rovescia quella del giro della `2.29` (*Il terzo (ma attivo di default)
 * 'Luce'*). La fila è l'ordine in cui si lavora, l'apertura è dove si lavora quasi sempre, e adesso
 * le due cose coincidono in un modulo solo: quello che dice che cosa ci sta dentro la fotografia.
 * ⚠️ **Si ricava dall'elenco e non è un numero scritto a mano**: chi sposta un modulo si ritrova
 * l'apertura giusta senza toccare altro, che è lo stesso criterio dei raggi delle fasce dell'HSL.
 */
private val LOOK_FIRST = MODULES.indexOfFirst { it.name == R.string.look_crop }.coerceAtLeast(0)

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
    /**
     * Dov'è la fila dei gettoni, in coordinate della radice, dalla `2.50`.
     *
     * ⚠️ **Serve al mini-onboarding, che vive nella schermata e non qui**: quel velo copre tutto
     * lo schermo, quindi nasce fuori da questa scheda, e il riquadro da illuminare lo sa solo chi
     * la fila la disegna. È lo stesso criterio del velo della copertina, che riceve il riquadro
     * misurato invece di ricalcolare la catena dei rientri.
     */
    onStrip: (Rect) -> Unit,
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
     * ⚠️⚠️ **GLI STILI SALVATI VIVONO QUI E NON NEL CORPO DEL MODULO, DALLA `2.52`**: il comando
     * che li crea vive sulla barra e l'elenco che li mostra vive nel corpo, cioè due pezzi
     * diversi, quindi lo stato vive sopra tutti e due. Tenuto di là, un salvataggio non si vedrebbe fino a
     * quando l'editor non si riapre.
     * ⚠️ **Si legge una volta**: a scriverlo sono il tasto qui accanto e la pagina delle
     * impostazioni, che è un'altra schermata, e rileggere il file a ogni ricomposizione vorrebbe
     * dire aprirlo per ogni fotogramma di un'animazione.
     */
    val context = LocalContext.current
    var mine by remember { mutableStateOf(Presets.mine(context)) }
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
            /*
             * ⚠️⚠️ **L'ORDINE DEI GETTONI È QUELLO SCELTO NELLE IMPOSTAZIONI, DALLA `2.34`, ED È
             * SUA RICHIESTA** (2026-09-13: *voglio poter ordinare anche i pulsanti dei moduli*).
             * ⚠️ **Si scorre l'ordine e non la tabella**: quello che cambia è come i sette si
             * vedono, mentre il modulo aperto resta un indice della tabella (vedi [modIndex]).
             * ⚠️ **Arriva da `LocalPadLook` come gli altri quattro ordini**, perché questa scheda
             * le impostazioni non le riceve.
             */
            /*
             * ⚠️⚠️ **E DALLA `2.50` SCORRE DI NUOVO, PERCHÉ I MODULI SONO OTTO, ED È SUA
             * ISTRUZIONE** (campo libero del giro della `2.40`: *nel mio caso, con il mio schermo,
             * sarà l'unico a richiedere uno scorrimento a destra, ma va benissimo così*). ⚠️ **Non
             * è il ritorno del rimedio della `2.23`, e la differenza è misurabile**: là la fila
             * scorreva perché i **nomi scritti** andavano a capo e il palco si riduceva a zero
             * pixel, cioè scorrere copriva un difetto; qui i gettoni sono icone, la cella resta
             * quella di sempre e a sporgere è il solo ottavo.
             * ⚠️⚠️ **LA CELLA SI MISURA SU [MOD_FIT] E NON SU QUANTI SONO**, ed è quello che tiene
             * la fila com'era: dividendola per otto, tutti i gettoni si stringerebbero e nessuno
             * sporgerebbe, cioè si perderebbe la misura che lui guarda da dieci versioni.
             * ⚠️ **Chi scorre lo scopre dal mini-onboarding**, che è la seconda metà della sua
             * richiesta: un elenco che continua fuori dallo schermo non lo dichiara da sé.
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
                                onLive(mod.clear)
                                onSettled()
                            },
                            modifier = Modifier.width(cella)
                        )
                    }
                }
            }

            /*
             * ⚠️⚠️ **IL CORPO È ALTO QUANTO IL MODULO PIÙ ALTO, DALLA `2.33`, ED È SUA
             * RICHIESTA** (2026-09-13: *voglio che la bottomsheet dell'editor completo sia sempre
             * alta uguale: non deve ballare da un modulo all'altro*). Fino alla `2.32` la scheda si
             * dimensionava sul proprio contenuto, quindi passare dalla Luce (sei cursori) alle
             * Curve (un grafico) le cambiava l'altezza, e con lei quella del palco: l'immagine su
             * cui si lavora cambiava misura a ogni gettone toccato.
             */
            /*
             * ⚠️ **La colonna dei nomi si misura QUI e non dentro il corpo**, cioè una volta per
             * scheda invece di una per modulo: la misura è la stessa per tutti e sette, perché
             * guarda i nomi di tutti, e [SteadyBody] compone ogni corpo una volta per misurarlo.
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
            /*
             * ⚠️⚠️ **DALLA `2.52` QUESTA FILA SI SPECCHIA COL LATO DEL FAB, CON UN'ECCEZIONE SUA**
             * (sua richiesta del 2026-09-14, con schermata: *l'ordine delle icone della barra bassa
             * deve essere speculare quando il FAB è a sinistra, con la sola eccezione di
             * 'Annulla'/'Ripristina', che devono essere sempre il primo a sinistra del secondo*).
             * Il lato lo dà [fabEdge], cioè la stessa preferenza che governa il FAB, le pastiglie
             * dell'intestazione e il rientro della notifica.
             * ⚠️⚠️ **L'ECCEZIONE NON È UN CAPRICCIO: QUEI DUE COMANDI SONO UN VERSO DEL TEMPO**, e
             * uno specchio lo rovescerebbe, cioè metterebbe 'Ripristina' sotto il dito che cerca
             * 'Annulla'. Le altre icone un ordine che voglia dire qualcosa non ce l'hanno.
             * ⚠️ **'Salva stile' sta dalla parte opposta e si specchia con loro**: col FAB a destra
             * è a sinistra, col FAB a sinistra passa dall'altro lato. ⚠️ **È una lettura, non una
             * sua parola**, e gliel'ho chiesta: la sua risposta del 2026-09-18 la conferma.
             */
            val mirror = fabEdge() == Alignment.Start
            val salva: @Composable () -> Unit = {
                /*
                 * ⚠️ **C'è nel solo modulo che ne parla**, come 'Auto' nei suoi due e 'Mirato' nei
                 * suoi: la condizione si legge dalla tabella dei moduli e non da un elenco di nomi
                 * scritto accanto al tasto.
                 */
                if (chosen.extra == Extra.PRESETS) {
                    PresetSaveButton(
                        look = look,
                        enabled = ready && !busy,
                        onSaved = { mine = it }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                /*
                 * ⚠️ **Lo spazio elastico in mezzo fa TUTTO il lavoro dell'allineamento**: senza il
                 * comando che salva resta lui solo, quindi la fila dei comandi si trova comunque
                 * appoggiata al lato giusto, e non serve una seconda condizione che scelga come
                 * disporla.
                 */
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
            }
        }
    }

}

/** Le icone della barra bassa dell'editor completo, nell'ordine in cui si disegnano col FAB a destra. */
internal enum class Bar { AIM, AUTO, UNDO, REDO, ORIGINAL }

/**
 * L'ordine in cui la barra bassa disegna le sue icone, dato il lato del FAB.
 *
 * ⚠️⚠️ **LO SPECCHIO SI FERMA DAVANTI ALLA COPPIA DEL TEMPO, ED È LA SUA ECCEZIONE ALLA LETTERA**
 * (*con la sola eccezione di 'Annulla'/'Ripristina', che devono essere sempre il primo a sinistra
 * del secondo*): rovesciata, quella coppia metterebbe 'Ripristina' dove il dito cerca 'Annulla'.
 * Le altre icone si specchiano tutte, perché un ordine che voglia dire qualcosa non ce l'hanno.
 * ⚠️ **È una funzione pura e non una riga dentro la Row**, quindi il banco la misura chiamandola:
 * uno specchio scritto a mano nel corpo si proverebbe solo montando la schermata e contando i
 * pixel di cinque icone.
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

/**
 * Le icone dei comandi della barra bassa, nell'ordine che [barOrder] detta.
 *
 * ⚠️ **Sono un pezzo a sé perché la fila si compone due volte**, una per lato: scritte dentro la
 * Row con un `if` intorno, le cinque chiamate sarebbero due copie che divergono al primo comando
 * nuovo.
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
    val armabile = chosen.extra == Extra.BANDS || chosen.extra == Extra.CORNERS
    val chiavi = buildList {
        if (armabile) add(Bar.AIM)
        if (chosen.auto) add(Bar.AUTO)
        add(Bar.UNDO)
        add(Bar.REDO)
        add(Bar.ORIGINAL)
    }
    val sorgente = origin
    for (voce in barOrder(chiavi, mirror)) when (voce) {
        /*
         * ⚠️⚠️ **IL COLORE MIRATO C'È DOVE IL MODULO HA UN BERSAGLIO DA SCEGLIERE, e non è una
         * coincidenza**: quel gesto serve a dire *questo colore qui*, e ha senso solo dove esiste
         * qualcosa da puntare, cioè una fascia dell'HSL. Negli altri moduli un cursore vale per
         * tutta l'immagine, quindi non c'è niente da mirare.
         * ⚠️⚠️ **ED È UN'ICONA E NON PIÙ UNA PASTIGLIA SCRITTA, DALLA `2.32`**: il nome resta come
         * descrizione, cioè quello che un lettore di schermo annuncia e quello che il banco cerca,
         * ed è lo stesso criterio dei sette gettoni dei moduli.
         * ⚠️ **A dire che è acceso è il colore**: un `IconButton` non ha uno stato scelto, quindi
         * il glifo passa all'accento quando la modalità è armata.
         * ⚠️⚠️ **E DALLA `2.50` QUESTO TASTO SERVE A DUE MODULI, CIOÈ ARMA IL DITO SENZA DIRE CHE
         * COSA FARÀ**: nell'HSL sceglie la fascia del colore toccato, nella Geometria tira uno dei
         * quattro angoli. Il gesto lo decide la tabella dei moduli, che è dove vive già la domanda
         * 'questo modulo prende il dito sull'immagine?'.
         * ⚠️ **Il glifo e il nome cambiano con lui**, perché sono la cosa che dice che cosa si sta
         * per armare: un mirino su un modulo che di colori non parla direbbe il falso.
         */
        Bar.AIM -> IconButton(
            onClick = { gaze.aiming = !gaze.aiming },
            enabled = ready && !busy
        ) {
            Icon(
                imageVector = if (chosen.extra == Extra.CORNERS) {
                    Icons.Filled.Transform
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
        /*
         * ⚠️⚠️ **'AUTO' SCRIVE NEI CURSORI E BASTA, ED È QUELLO CHE LO RENDE ANNULLABILE** (sua
         * richiesta: *dev'essere annullabile*): quello che ne esce è un [Look] come un altro,
         * quindi entra nella storia dei passi e 'Annulla' lo disfa. Il conto, e perché imita
         * 'Colore automatico' di Photoshop con quattro cursori, vivono in [Auto].
         * ⚠️ **Legge l'anteprima e non il file**: quello che si misura sono percentili e medie, e
         * su una riduzione valgono quanto sull'originale. Leggere il file pieno costerebbe una
         * pausa per una cifra che non si muove di un livello.
         * ⚠️ **Il glifo è di Material e nasce provvisorio**, come i due della `1.80`: se non dice
         * abbastanza, il giro di collaudo lo chiede e lui manda il suo.
         * ⚠️⚠️ **E COMPARE SOLO DOVE TOCCA QUALCOSA, DALLA `2.50`, ED È SUA RICHIESTA** (campo
         * libero del giro della `2.40`, punto 5: *l'icona di 'Auto' deve apparire solo se è attivo
         * 'Luce' o 'Colore'*): quel comando scrive nei quattro cursori della Luce e nei due del
         * bilanciamento del bianco, quindi altrove cambiava dei numeri che non si vedono. La
         * condizione la porta la tabella dei moduli ([Module.auto]), come quella del colore
         * mirato.
         */
        Bar.AUTO -> IconButton(
            onClick = {
                if (sorgente != null) {
                    onLive { Auto.tuned(it, Auto.probe(sorgente)) }
                    onSettled()
                }
            },
            enabled = ready && !busy && sorgente != null
        ) {
            Icon(Glyphs.Auto, stringResource(R.string.look_auto))
        }
        /*
         * ⚠️⚠️ **GLI STILI NON SONO PIÙ UN'ICONA DI QUESTA FILA, DALLA `2.50`: SONO L'OTTAVO
         * MODULO** (campo libero del giro della `2.40`, punto 1: *inserisci i modelli in un modulo
         * a parte*). ⚠️ **Ma dalla `2.52` qui torna il comando che SALVA**, che è un'altra cosa: il
         * modulo è dove si scelgono, questo è dove se ne fa uno nuovo, e vive fuori dall'elenco che
         * scorre perché un comando che se ne va con le righe si ritrova risalendo.
         */
        Bar.UNDO -> IconButton(onClick = onUndo, enabled = canUndo && !busy) {
            Icon(Glyphs.EditUndo, stringResource(R.string.editor_undo))
        }
        Bar.REDO -> IconButton(onClick = onRedo, enabled = canRedo && !busy) {
            Icon(Glyphs.EditRedo, stringResource(R.string.editor_redo))
        }
        Bar.ORIGINAL -> IconButton(onClick = onOriginal, enabled = !look.idle && !busy) {
            Icon(Glyphs.EditReset, stringResource(R.string.editor_original))
        }
    }
}

/**
 * Quello che la scheda mostra fra la fila dei gettoni e quella dei comandi: i cursori del modulo
 * [module], e quello che quel modulo ha in più (le forme e la posa del Ritaglio, le otto fasce
 * dell'HSL, il grafico delle Curve, l'interruttore del bianco e nero del Colore).
 *
 * ⚠️⚠️ **PRENDE L'INDICE DEL MODULO INVECE DI LEGGERE [Gaze.module], ED È QUELLO CHE PERMETTE DI
 * MISURARLI TUTTI**: [SteadyBody] compone questo corpo una volta per modulo per sapere quale sia
 * il più alto, e un corpo che si chiedesse da sé quale modulo è in scena risponderebbe sette volte
 * la stessa altezza.
 * ⚠️ **Le lambda di scrittura invece leggono lo sguardo VIVO** (`dialAt`), che è la correzione
 * della `2.20` e non cambia: le copie che si misurano non vengono posizionate, quindi nessun dito
 * le raggiunge, e quella in scena scrive nel modulo che si sta guardando.
 *
 * ⚠️⚠️ **UN MODULO HA UN'ALTEZZA SOLA, E FRA LA `2.36` E LA `2.37` NON ERA COSÌ**: là il 'Filtro'
 * del Colore compariva col bianco e nero, quindi quel corpo era alto due misure diverse e questa
 * funzione aveva un secondo argomento che chiedeva *dammi il più alto che puoi venire*. Con la riga
 * tornata sempre in scena (vedi [NOT_MONO]) quel parametro non aveva più niente da dire.
 */
@Composable
private fun ModuleBody(
    module: Int,
    look: Look,
    gaze: Gaze,
    ready: Boolean,
    busy: Boolean,
    origin: Bitmap?,
    /** La colonna dei nomi, misurata una volta per tutti i moduli: vedi [knobNameWidth]. */
    nameWidth: Dp,
    /**
     * L'altezza comune della scheda, per il solo corpo che la riceve invece di dettarla.
     *
     * ⚠️ **Gli altri sette la ignorano**, ed è giusto così: sono alti quanto il loro contenuto, e
     * il più alto di loro è quello che questo numero misura. L'unico che la legge è l'elenco degli
     * stili, che ci scorre dentro (vedi [Extra.PRESETS]).
     */
    height: Dp,
    /**
     * Gli stili salvati, per il solo corpo che li elenca.
     *
     * ⚠️ **Arrivano dalla schermata e non si leggono qui**, perché dalla `2.52` a crearli è un
     * comando che vive sulla barra: il perché per esteso è su [PresetBody].
     */
    mine: List<Preset>,
    onLive: ((Look) -> Look) -> Unit,
    onSettled: () -> Unit,
    onPeek: (((Look) -> Look)?) -> Unit
) {
    val mod = MODULES[module]
    val band = gaze.band

    /*
     * ⚠️⚠️ **L'ELENCO DEGLI STILI È UN CORPO COME GLI ALTRI, DALLA `2.50`**: quello che cambia è
     * che riceve l'altezza invece di dettarla, e che i suoi due gesti scrivono un [Look] intero
     * invece di un cursore. Applicare resta un passo della storia, cioè `onLive` più `onSettled`,
     * la stessa coppia con cui scrive 'Auto': 'Annulla' disfa uno stile senza una strada sua.
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
     * ⚠️ **Legge [Gaze.module] e [Gaze.band] al momento della chiamata e non alla composizione**,
     * ed è tutto il suo valore: chiamata da dentro il rilevatore di un gesto, risponde con quello
     * che il dito ha davvero sotto. ⚠️ **La fascia è entrata nel conto con la `2.21` e non è un
     * secondo meccanismo**: è la stessa correzione della `2.20` su una dimensione in più, e senza
     * di lei un cursore dell'HSL scriverebbe nella fascia da cui il suo nodo è nato.
     */
    fun dialAt(riga: Int): Dial? = MODULES[gaze.module].rows(gaze.band).getOrNull(riga)

    /** Quante volte l'immagine **già posata** è più larga che alta: vedi [posedAspect]. */
    fun cropAspect(k: Look): Float = posedAspect(origin, k.spin)

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
    if (mod.extra == Extra.CROP) {
        val live = ready && !busy
        val lay = cropLay(gaze)
        fun pose(gesto: Spin) {
            onLive { spunLook(it, gesto) }
            onSettled()
        }
        /*
         * ⚠️⚠️ **LE FILE SONO QUELLE DELL'EDITOR DI CASA, E DALLA `2.32` SONO LO STESSO
         * PEZZO**: le forme le disegna `ShapeRow`, che vive di là, e qui resta la sola
         * cosa che cambia fra i due editor, cioè dove si scrive la scelta. Fino alla
         * `2.31` questa fila era ricopiata riga per riga, e con 'Originale' sarebbero
         * state due copie da tenere d'accordo invece di una.
         */
        ShapeRow(
            shape = cropShape(gaze),
            lay = lay,
            enabled = live,
            /*
             * ⚠️⚠️ **QUI LE FORME VANNO A CAPO, DALLA `2.35`, ED È IL SUO RISCONTRO** (giro
             * della `2.34`, voce `crop-fila` non approvata: *devono occupare più spazio*).
             * Vale **solo** in questo editor, e la ragione è la scheda ad altezza fissa
             * della `2.33`: qui lo spazio che il Ritaglio non usa resterebbe vuoto, di là
             * una seconda riga scenderebbe sull'immagine. Il conto vive su [ShapeRow].
             */
            wrap = true,
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
            /*
             * ⚠️⚠️ **I TRE BLOCCHI NON PORTANO PIÙ UN DISTACCO SCRITTO A MANO, DALLA `2.40`, E
             * NON È UNA SPREMITURA**: l'aria fra le righe di un modulo la dà [Breathe], che dalla
             * `2.35` la distribuisce dove avanza; scritta anche qui si sommava alla sua, e nel
             * Ritaglio, che è il modulo più fitto, era l'unica a esserci. Toglierla lascia posto
             * ai quattro comandi e mette lo spazio dove il pannello lo mette dappertutto.
             */
            modifier = Modifier
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier
        )
        /*
         * ⚠️⚠️ **I QUATTRO COMANDI DEL RITAGLIO VIVONO QUI, DALLA `2.40`, ED È LA SUA
         * RICHIESTA** (2026-09-14: *apparissero dei tasti 'Annulla'/'Ripristina'/'Azzera' SOLO
         * PER IL RITAGLIO (magari posizionati altrove e chiamati 'Indietro'/'Avanti'/'Azzera')*).
         * Il *magari altrove* era una possibilità e la scelta è mia: stanno **tutti e quattro
         * insieme** dentro il modulo, e la fila di fondo torna a portare i soli comandi
         * dell'immagine. Due storie con gli stessi gesti a mezzo centimetro di distanza sono due
         * cose che si scambiano, e 'Applica' in fondo era proprio il caso che lui ha notato.
         * ⚠️⚠️ **NON È UN [ActionPad], E LA RAGIONE È UNA MISURA, NON UNO STILE**: una cella di
         * quel pezzo è alta 64 punti anche senza etichetta, e il corpo del Ritaglio ne aveva
         * **30** liberi prima di superare le Curve, che dalla `2.33` è il modulo che detta
         * l'altezza della scheda (misurato: Ritaglio 208, Curve 238). Con quella fila il Ritaglio
         * arrivava a 278, cioè la scheda cresceva di 40 punti **in tutti e sette i moduli** e sul
         * banco il palco andava a **zero**: è il difetto della `2.23` rifatto, e l'hanno preso le
         * prove sui pixel.
         * ⚠️⚠️ **QUINDI LA RIGA È ALTA [CROP_CMD_ROW], CHE È LA MISURA DI RIGA DI QUESTA
         * SCHEDA**: quanto un chip delle forme, e quattro punti sotto [DIAL_ROW]. ⚠️ **Il
         * bersaglio non è quello che si perde**: le quattro celle si dividono tutta la larghezza,
         * quindi ognuna è larga una settantina di punti, cioè più di un `IconButton` di Material;
         * l'altezza è sotto i suoi 48 come già la riga di un cursore, che porta la stessa nota.
         * ⚠️ **Il nome vive nella descrizione parlata**, come nei sette gettoni dei moduli: è
         * quello che un lettore di schermo annuncia e quello che il banco cerca.
         */
        val portata = relativeTo(look.framing.shown, look.crop)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CropCmd(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
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
                icon = Icons.AutoMirrored.Filled.ArrowForward,
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
             * ⚠️ **Si accende quando c'è qualcosa da applicare**, cioè quando il rettangolo vivo è
             * più piccolo della porzione che si vede: applicare un rettangolo che la copre tutta
             * non cambierebbe niente, e un tasto acceso che non fa niente è peggio di uno spento.
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
             * ⚠️ **'Azzera' guarda il solo ritaglio e non la posa**, al contrario del tocco lungo
             * sul gettone del modulo: quello rimette a nuovo il modulo intero, questo risponde
             * alla domanda *rivoglio l'immagine tutta quanta*.
             */
            CropCmd(
                icon = Icons.Filled.CropFree,
                label = R.string.editor_crop_clear,
                enabled = live && (!look.crop.whole || look.framing.undoable)
            ) {
                onLive { k ->
                    k.copy(framing = Framing.NONE, crop = ImageEdit.Crop.WHOLE)
                }
                onSettled()
            }
        }
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
     * ⚠️⚠️ **LA FILA DEI QUATTRO CANALI E IL GRAFICO, DALLA `2.23`**: è la terza forma che
     * questa scheda può prendere, e la sola in cui non ci sono cursori. I gesti dei
     * gettoni sono quelli di sempre, tocco per scegliere e tocco lungo per azzerare, un
     * gradino più in basso: là si azzera il modulo, qui la curva di quel canale.
     * ⚠️ **Il gettone è lo stesso della fila dei moduli e non un pezzo nuovo**: quello che
     * serve qui è esattamente quello che fa già, cioè un nome, il segno di 'toccato' e i
     * due gesti.
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
    mod.rows(band).forEachIndexed { riga, knob ->
        /*
         * ⚠️⚠️ **L'INTERRUTTORE DEL BIANCO E NERO SI DISEGNA DAVANTI AL FILTRO, DALLA `2.37`**
         * (punto A del campo libero del giro della `2.36`: *'Filtro BN' ... va posizionato (non
         * attivo) DOPO l'interruttore 'Bianco e nero'*). A dirlo è l'identità della riga e non
         * un indice: il perché vive su [FILTER_ROW].
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
                nameWidth = nameWidth,
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
}

/**
 * L'interruttore del **bianco e nero**, che nel modulo Colore vive fra i quattro cursori del colore
 * e il 'Filtro BN'.
 *
 * ⚠️⚠️ **È UN INTERRUTTORE E NON UN CURSORE A -100**: è una scelta (questa immagine è a colori, o
 * non lo è) e non una quantità, e scritto come fondo corsa della saturazione resterebbe esposto a
 * chiunque muova quel cursore. Nel conto viene infatti dopo, e qui vive sulla stessa colonna dei
 * cursori perché è uno dei comandi di questo modulo.
 * ⚠️⚠️ **E DALLA `2.37` NON È PIÙ L'ULTIMA RIGA**, ed è il suo riscontro (punto A del campo libero
 * del giro della `2.36`): sotto di lui c'è il 'Filtro BN', che è il solo comando del modulo a
 * dipendere da questo, quindi si legge come la sua conseguenza invece che come un quinto cursore.
 * ⚠️ **La riga è un bersaglio solo**, con `Role.Switch` sulla riga e niente sull'interruttore: è la
 * stessa regola delle righe del pannello delle impostazioni.
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
         * ⚠️ **Lo stesso corpo dei nomi dei cursori**, che dalla `2.35` è un gradino sotto: questa
         * riga vive in mezzo a loro, e due corpi diversi nella stessa colonna si vedrebbero prima
         * di qualunque altra cosa.
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
 * Il corpo della scheda, alto quanto il più alto dei [slots] moduli: così la scheda non cambia
 * misura passando da un modulo all'altro, e il palco sopra di lei nemmeno.
 *
 * ⚠️⚠️ **L'ALTEZZA SI MISURA E NON SI SCRIVE**: i moduli portano cursori, gettoni e un grafico, e
 * quanto sia alta una di quelle righe dipende dal corpo del carattere, dalla sua scala e dalla
 * lingua. Un numero in `dp` scritto qui sarebbe giusto su un telefono e sbagliato sul prossimo, e
 * per accorgersene servirebbe che qualcuno guardasse; quello che regge è comporre ogni corpo una
 * volta, misurarlo, e tenere il massimo.
 *
 * ⚠️⚠️ **LE COPIE VIVONO UN GIRO SOLO, E NON È UN'OTTIMIZZAZIONE: LA PRIMA STESURA LE TENEVA IN
 * SCENA E HA FATTO ROSSE SETTE PROVE.** Misurandole con un `SubcomposeLayout` a ogni passata,
 * quei corpi restano nell'albero: non si disegnano e non prendono il dito (nessuno li posiziona),
 * ma **ci sono**, quindi un lettore di schermo annuncia i cursori di sette moduli e il banco li
 * conta. Qui la misura si scrive in uno stato, e la ricomposizione che ne segue le porta via:
 * dal secondo giro nell'albero c'è il solo modulo che si sta guardando.
 *
 * ⚠️ **E mentre ci sono non hanno semantica** (`clearAndSetSemantics`), che è la seconda porta
 * chiusa: fra la misura e la ricomposizione passa un fotogramma, e in quel fotogramma là dentro
 * non c'è niente da annunciare.
 *
 * ⚠️ **Il giro costa una volta per schermata aperta**: le altezze non dipendono dai valori dei
 * cursori (un cursore è alto uguale a +100 come a zero), quindi rifare quella misura a ogni
 * fotogramma di trascinamento vorrebbe dire comporre sette moduli sessanta volte al secondo, che
 * è il difetto misurato nella `2.26`. ⚠️ **Che la larghezza non cambi è vero per costruzione**:
 * questa schermata inibisce la rotazione, e un cambio di configurazione rifà l'activity, cioè
 * anche questo `remember`.
 *
 * ⚠️⚠️ **E UN MODULO HA UN'ALTEZZA SOLA, CHE È QUELLO CHE RENDE VERA QUESTA MISURA**: nessuna riga
 * compare e sparisce col valore di un cursore, quindi il corpo che si misura è lo stesso che si
 * vedrà. Fra la `2.36` e la `2.37` non era così: il 'Filtro' del Colore compariva col bianco e
 * nero, e [body] aveva un secondo argomento che chiedeva *dammi il più alto che puoi venire*. Il
 * perché quella riga sia tornata sempre in scena vive su [NOT_MONO].
 */
/**
 * Uno dei quattro comandi del ritaglio: un glifo in una cella che si divide la riga con le altre.
 *
 * ⚠️ **Il nome vive nella descrizione parlata del glifo**, come nei sette gettoni dei moduli: la
 * riga è alta [CROP_CMD_ROW] e una parola là sotto non ci starebbe, ma un lettore di schermo la
 * annuncia lo stesso, ed è anche quello che il banco cerca.
 * ⚠️ **Il tocco vive sulla cella e non sul glifo**, quindi il bersaglio è largo un quarto di riga:
 * è il modo in cui questa fila compra in larghezza l'altezza che non ha.
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

@Composable
internal fun SteadyBody(
    slots: Int,
    chosen: Int,
    modifier: Modifier = Modifier,
    /**
     * Se lo slot [it] entra nella misura dell'altezza comune.
     *
     * ⚠️⚠️ **NASCE COL MODULO DEGLI STILI, DALLA `2.50`, E SENZA DI LEI QUEL MODULO DETTEREBBE
     * L'ALTEZZA DI TUTTI**: il suo corpo è un elenco che cresce con quello che l'utente salva,
     * quindi misurarlo vorrebbe dire una scheda che si alza a ogni stile nuovo, anche per chi gli
     * stili non li usa. Chi resta fuori dalla misura la **riceve** e ci scorre dentro.
     * ⚠️ **Almeno uno deve entrarci**, o non ci sarebbe nessuna altezza da ricevere: qui ce ne
     * sono sette su otto, e la domanda la fa la tabella dei moduli.
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
                 * ⚠️⚠️ **NESSUN `place`, ED È QUELLO CHE RENDE INNOCUE LE COPIE**: un nodo che
                 * non viene posato non si disegna e non entra nella hit-test dei tocchi, che è la
                 * stessa proprietà su cui si regge la seconda tela della lente.
                 * ⚠️ **La misura si scrive nel piazzamento**, che è la fase in cui Compose
                 * ammette una scrittura di stato: quella scrittura è il via alla ricomposizione
                 * che porta via queste copie, e succede una volta perché il valore poi non è più
                 * nullo.
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
 * ⚠️⚠️ **NASCE DALLA SECONDA METÀ DELLA SUA RICHIESTA, DALLA `2.35`** (2026-09-13: *fa' respirare
 * di più quelli ristretti inutilmente*). Dalla `2.33` la scheda è alta quanto il modulo più alto,
 * quindi in un modulo corto avanza dello spazio; fino alla `2.34` quello spazio restava tutto **in
 * fondo**, cioè il contenuto stava appiccicato in cima a un vuoto.
 *
 * ⚠️⚠️ **L'ARIA HA UN TETTO, E SENZA DI LUI LA DISTRIBUZIONE SAREBBE PEGGIO DEL VUOTO**: nel
 * Ritaglio avanzano un centinaio di punti su tre blocchi, e divisi per due vani darebbero mezzo
 * centimetro fra una fila di tasti e l'altra, cioè tre isole invece di un pannello. Col tetto
 * ognuno prende il suo respiro e quello che resta **centra** il blocco, che è il modo in cui una
 * cosa corta vive dentro una cosa alta.
 *
 * ⚠️ **È un `Arrangement` e non un `padding` scritto nei chiamanti**: il conto vive in un posto
 * solo e lo prende qualunque modulo, compresi quelli che verranno; e siccome è Kotlin puro, il
 * banco lo può misurare chiamandolo, senza montare niente.
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
 * ⚠️ **È un tetto e non una spaziatura**: dove non avanza niente vale zero, quindi il modulo più
 * alto resta esattamente come prima e nessun altro si allunga per colpa di questo numero.
 */
private val BREATH_MAX = 12.dp

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
            .padding(horizontal = if (icon == null) MODULE_SIDE else 0.dp, vertical = MODULE_PAD)
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

    /*
     * ⚠️⚠️ **LA CURVA SI LEGGE VIVA, E LA CHIAVE NON LA PORTA PIÙ: È LA CORREZIONE DELLA `2.40`**
     * (sua richiesta, 2026-09-14: *toccare un punto libero e trascinarlo dovrebbe sia aggiungere un
     * nuovo punto che spostarlo creando la curva*). Fino alla `2.39` la chiave del rilevatore
     * portava il **numero di punti**, quindi l'istante in cui il gesto ne faceva nascere uno
     * cambiava la chiave e Compose **annullava il rilevatore in corso**: il punto compariva e il
     * dito non lo muoveva più. Il codice era giusto, il gesto partiva, e la metà che lui chiedeva
     * non arrivava mai.
     * ⚠️ **La chiave portava il conto perché il corpo catturava la curva**, che è il difetto vero:
     * con uno stato aggiornato quel bisogno non c'è, e il gesto legge sempre l'ultima.
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
                     * ⚠️⚠️ **LA CURVA SI FOTOGRAFA QUI, E RILEGGERLA PIÙ SOTTO ERA IL DIFETTO DELLA
                     * `2.50`** (sua segnalazione, 2026-09-14: *se tocco e trascino la curva
                     * direttamente, si crea una retta orizzontale che arriva fino al margine
                     * sinistro o destro, distruggendo l'immagine*). Fra `onEdit` e la riga dopo non
                     * c'è nessuna ricomposizione, quindi [rememberUpdatedState] risponde ancora
                     * **quella di prima**: il conto degli estremi guardava la curva senza il punto
                     * appena nato, e l'indice di quel punto cadeva esattamente sul suo ultimo
                     * indice. Da lì il gesto si credeva su un estremo, faceva nascere il gemello e
                     * portava il bordo al livello del dito.
                     * ⚠️ **Una fotografia sola per tutto il gesto**: il dito lavora sulla curva che
                     * aveva sotto quando è sceso, e quello che il gesto scrive lo scrive per indice.
                     */
                    val partenza = viva
                    val near = partenza.nearest(x)
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
                    val i = if (near >= 0) near else partenza.grow(x).second
                    if (near < 0) {
                        onEdit { it.grow(x).first }
                    }
                    if (esito == Settled.MOVED) {
                        /*
                         * ⚠️⚠️ **UN ESTREMO TRASCINATO LASCIA UN PUNTO AL BORDO, DALLA `2.40`, ED È
                         * SUA RICHIESTA** (2026-09-14: *se trascino il punto iniziale a destra o il
                         * finale a sinistra, dovrebbero muoversi lasciando la loro vecchia
                         * posizione ad un nuovo punto allo stesso livello*). Il gemello nasce
                         * **qui**, cioè quando il gesto è già un trascinamento: un tocco secco su
                         * un estremo non deve lasciare niente dietro di sé.
                         * ⚠️ **L'indice del punto mosso scala di uno** quando il gemello entra in
                         * testa, e resta dov'è quando entra in coda: da quel momento il dito muove
                         * un punto come gli altri, che si sposta anche in orizzontale.
                         * ⚠️ **Al tetto dei punti non si fa niente**, e l'estremo resta un estremo:
                         * [Curve.pin] risponde la curva com'è, quindi l'indice non va toccato.
                         * ⚠️⚠️ **E SOLO UN PUNTO CHE C'ERA GIÀ PUÒ ESSERE UN ESTREMO**, che è la
                         * seconda metà della correzione della `2.50`: un punto appena nato sta per
                         * definizione **fra** due punti, quindi `near < 0` chiude il caso senza
                         * bisogno di guardare gli indici. Con la sola fotografia della curva un
                         * tocco al tetto dei punti, dove [Curve.grow] risponde un indice qualunque,
                         * potrebbe ancora cadere su zero.
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
                        // ⚠️ **Il gemello rimasto a filo se ne va**: chi porta l'estremo dentro e
                        // poi lo riporta indietro non si ritrova due punti l'uno sull'altro.
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KNOB_GAP)
    ) {
        /*
         * ⚠️⚠️ **UN GRADINO PIÙ PICCOLO DALLA `2.35`, ED È SUA RICHIESTA** (2026-09-13: *riduci
         * dimensioni del testo, padding, ecc. per i moduli che occupano più spazio verticale*).
         * ⚠️ **Il corpo del nome non decide l'altezza della riga**, che è [DIAL_ROW]: quello che
         * compra è **larghezza**, perché la stessa parola entra in una colonna più stretta e i
         * punti che avanzano vanno alla barra. ⚠️ **E toglie un rischio**: un nome lungo che va a
         * capo fa crescere la sua riga oltre [DIAL_ROW], e a corpo più piccolo quel caso arriva
         * più tardi in tutte e ventotto le lingue.
         * ⚠️⚠️ **E DALLA `2.37` LA COLONNA LA DECIDE LA MISURA**, che è quello che toglie del tutto
         * quel rischio invece di allontanarlo: vedi [knobNameWidth].
         * ⚠️⚠️ **IL NOME PUÒ ANDARE A CAPO, E DALLA `2.38` LA SUA CONDIZIONE È UN NUMERO** (sua
         * istruzione, 2026-09-13: *'colore' può anche andare a capo, a patto che lo slider rimanga
         * alla stessa distanza da quello sopra*). Due righe di `bodySmall` valgono **32 punti**
         * contro i 36 di [DIAL_ROW], quindi la riga non cresce e la barra resta dov'è: la
         * condizione la tiene una misura e non una speranza.
         * ⚠️ **La terza riga non c'è**, e `maxLines` la esclude: là l'altezza sfonderebbe
         * [DIAL_ROW] e il passo fra due cursori cambierebbe.
         * ⚠️⚠️ **QUELLO CHE RESTA FUORI SI DICHIARA**: con la scala dei caratteri di sistema oltre
         * il 150% due righe superano i 36 punti, e **quella** riga si allunga. Non si chiude con
         * un'altezza fissa: là il testo sborderebbe sulla riga vicina invece di essere tagliato,
         * che è peggio del passo diverso.
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

/**
 * Il minimo della colonna dei nomi dei cursori, cioè quanto era larga fino alla `2.36`.
 *
 * ⚠️⚠️ **SCESA DALLA `2.35` INSIEME AL CORPO DEL NOME, ED È SUA RICHIESTA** (2026-09-13: *cerca di
 * mantenere tutto più compatto: riduci dimensioni del testo, padding, ecc. per i moduli che
 * occupano più spazio verticale*). Il nome si scrive un gradino più piccolo, quindi la stessa
 * parola chiede meno larghezza, e i dodici punti che avanzano vanno alla **barra**, cioè alla sola
 * parte di quella riga con cui si lavora.
 * ⚠️ **Dalla `2.37` è un minimo e non la misura**: vedi [knobNameWidth]. Resta perché in una lingua
 * dai nomi corti la colonna si stringerebbe, e la barra partirebbe da un punto diverso da quello
 * che lui ha davanti da venti versioni.
 */
private val KNOB_NAME_MIN = 84.dp

/**
 * Il tetto della colonna dei nomi: oltre, un nome va a capo come faceva prima.
 *
 * ⚠️⚠️ **ESISTE PERCHÉ LA MISURA CRESCE COL TESTO, E SENZA DI LUI SI MANGEREBBE LA BARRA**: con la
 * scala dei caratteri di sistema al massimo, o in una lingua dai nomi lunghissimi, la colonna
 * arriverebbe a prendersi mezza riga e il cursore diventerebbe un tratto di un centimetro. Sopra il
 * tetto si torna al comportamento di prima, che è il male minore fra i due: un nome su due righe si
 * legge, un cursore che non si può muovere no.
 * ⚠️ **Il numero copre quello che si misura oggi**: il nome più largo delle ventotto lingue è lo
 * swahili 'Kichujio cha B/W', e in Roboto a corpo pieno chiede una novantina di punti.
 */
private val KNOB_NAME_MAX = 132.dp

/**
 * Quanto è larga la colonna dei nomi dei cursori: quanto il più largo di tutti, col carattere e la
 * scala di **questo** telefono.
 *
 * ⚠️⚠️ **SI MISURA E NON SI SCRIVE, DALLA `2.37`, ED È IL PUNTO B DEL SUO CAMPO LIBERO** (giro
 * della `2.36`: *'Mascheratura' deve stare per esteso nel modulo Dettagli, senza andare a capo:
 * aumenta la larghezza quanto basta, oppure fallo diventare 'Maschera'*). Le due vie che ha dato
 * hanno lo stesso difetto, ed è il conto a dirlo: **'quanto basta' non è un numero**. In Roboto a
 * corpo pieno 'Mascheratura' chiede 79 punti su 84, cioè entra con un margine del 6%, e va a capo
 * appena il testo cresce di un decimo, che è quello che succede alzando la dimensione dei
 * caratteri di sistema; e non è nemmeno il caso peggiore, perché su ventotto lingue **quattordici**
 * nomi superano quegli 84 punti a scala uno: il più largo dei latini è il francese 'Hautes
 * lumières', che ne chiede 93, e il russo 'orizzontale' arriva a 95. Accorciare una parola cura una
 * lingua sola; allargare di un numero fisso cura una scala sola.
 *
 * ⚠️⚠️ **E QUELLO CHE LO RENDE VERO È CHE LA MISURA LA FA IL TELEFONO**: `rememberTextMeasurer`
 * usa il carattere di sistema e la scala in vigore, quindi la colonna cresce insieme al testo. Un
 * conto fatto qui con le metriche di Roboto sarebbe di nuovo un numero, giusto su un telefono e
 * sbagliato sul prossimo.
 *
 * ⚠️ **Si misurano i nomi di TUTTI i moduli e non quelli in scena**: una colonna che si
 * dimensionasse sul modulo aperto cambierebbe larghezza a ogni gettone toccato, cioè rifarebbe in
 * orizzontale il ballo che la `2.33` ha tolto in verticale.
 * ⚠️ **Le otto fasce dell'HSL portano gli stessi tre nomi**, quindi basta chiedere la prima.
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
 * ⚠️ **Il gettone a icona non ne ha**: là la larghezza la divide la fila (`weight`), quindi un
 * rientro ai fianchi toglierebbe area di tocco senza spostare niente.
 */
private val MODULE_SIDE = 14.dp

/**
 * L'aria sopra e sotto il glifo di un gettone dei moduli.
 *
 * ⚠️ **Non è privata perché la legge anche la REPLICA**, cioè il riquadro che riordina la fila
 * nelle impostazioni: là l'altezza di una cella si ricava da questo numero più il glifo, e
 * scriverne un altro vorrebbe dire una replica che si scosta dal modello al primo ritocco.
 */
internal val MODULE_PAD = 8.dp

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
 * ⚠️ **La tonalità viene dai centri di `Mix` e non da un elenco suo**: con due elenchi, il primo a
 * divergere sarebbe quello che nessuno guarda, e una pastiglia verde si chiamerebbe 'Blu'.
 * ⚠️⚠️ **DALLA `2.25` ALLA `2.34` LA LEGGEVA ANCHE IL MIRINO DELLA LENTE**, che portava il colore
 * della fascia del pixel sotto il dito; con la lente se n'è andata anche quella funzione
 * (`tintOfPixel`), perché non aveva più nessun chiamante. Vedi la nota sulla `2.35` dov'era lo
 * stato della lente.
 */
private fun bandTint(hue: Float): Color = Color.hsv(hue * 360f, BAND_SAT, BAND_VAL)

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
private val KNOB_VALUE = 44.dp

/**
 * L'aria fra le tre colonne di una riga di cursore: il nome, la barra e il numero.
 *
 * ⚠️⚠️ **NASCE DALLA `2.38`, ED È SUA SEGNALAZIONE CON SCHERMATA** (2026-09-13: *lascia più spazio
 * per i testi ... più un po' di aria, perché al momento è tutto troppo attaccato*). Fino alla
 * `2.37` le tre colonne si toccavano: la colonna dei nomi è larga quanto il **più largo** di tutti
 * i moduli, quindi proprio quel nome arrivava a filo del tondo, che a riposo ha il centro sul bordo
 * della barra. Il testo non era tagliato, ma si leggeva come incollato al comando.
 * ⚠️ **Lo spazio lo paga la barra e non il nome**: la colonna del nome e quella del numero hanno
 * una larghezza dichiarata, quindi i ventiquattro punti dei due distacchi escono dal `weight` della
 * barra, cioè dalla sola parte della riga che può cedere senza che niente si tronchi.
 */
private val KNOB_GAP = 12.dp

/**
 * L'altezza di una riga di cursore.
 *
 * ⚠️ **È l'area di tocco e non l'altezza del disegno**: il tondo è alto la metà, e il resto serve
 * perché il dito prenda la barra senza centrarla.
 *
 * ⚠️⚠️ **SCESA DI QUATTRO PUNTI CON LA `2.35`, ED È IL NUMERO CHE PAGA PIÙ DI TUTTI** (sua
 * richiesta, 2026-09-13: *cerca di mantenere tutto più compatto*): la scheda è alta quanto il
 * modulo più alto, che è la Luce coi suoi sei cursori, quindi ogni punto tolto qui vale **sei
 * volte** e lo guadagnano tutti e sette i moduli. Il conto: sei righe passano da 240 a 216 punti,
 * cioè il palco cresce di 24 su una scheda che ne vale circa 360.
 * ⚠️ **Sotto questo numero non si scende**: il bersaglio resta largo tutta la riga, ma l'altezza
 * è già sotto i 48 punti di Material, e il tondo ne vale 22.
 */
private val DIAL_ROW = 36.dp

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
 * decisa e si posa, che è il modo in cui un movimento si chiude.
 */
private const val ZOOM_RIDE = 220

/**
 * L'aria intorno all'immagine, che è quella di cui le squadrette del **Ritaglio** hanno bisogno.
 *
 * ⚠️ **Dalla `2.37` vale in tutti e sette i moduli**, perché l'immagine non cambi misura entrando
 * nel Ritaglio: il perché per esteso vive su `air()`, dentro il palco.
 *
 * ⚠️⚠️ **SI RICAVA DALLA SQUADRETTA E NON È UN NUMERO SCELTO**: il tracciato di `bracket` corre a
 * mezzo spessore **fuori** dal rettangolo, e l'alone gli sta intorno, quindi l'inchiostro arriva
 * esattamente a `HANDLE_THICK + GRIP_HALO` oltre il bordo dell'immagine. Chi cambiasse una di
 * quelle due misure si ritroverebbe l'aria giusta senza toccare niente; un numero scritto a mano
 * tornerebbe a tagliare le maniglie al primo ritocco.
 * ⚠️ **Il perché esista vive su [fitted]**, insieme al difetto che ha corretto.
 */
private val CROP_AIR = HANDLE_THICK + GRIP_HALO


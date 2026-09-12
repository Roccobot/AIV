package io.github.roccobot.aiv

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.Shader.TileMode
import android.net.Uri
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
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

    BackHandler { onBack() }

    LaunchedEffect(uri) {
        origin = withContext(Dispatchers.IO) { preview(context, uri) }
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
                    look = if (comparing) Look.NONE else peek?.invoke(look) ?: look,
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
    look: Look,
    onCompare: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val hold = stringResource(R.string.look_compare)
    var scale by remember(picture) { mutableFloatStateOf(1f) }
    var shift by remember(picture) { mutableStateOf(Offset.Zero) }
    /** La corsa del doppio tocco, tenuta per poterla fermare appena un dito scende. */
    var ride by remember(picture) { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val wide = picture.width.toFloat() / picture.height

    Canvas(
        modifier = modifier
            // ⚠️ Ingrandita, l'immagine esce dal proprio riquadro: senza questa riga andrebbe a
            // finire sopra la testata e sopra la scheda dei cursori.
            .clipToBounds()
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
                    shift = reined(from + (shift - from) * grown + pan, next, room, wide)
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
                                            next, room, wide
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
                                            else reined(anchor * (1f - ZOOM_TAP), ZOOM_TAP, room, wide)
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
                                        }
                                    }
                                }
                            }
                        }
                        else -> transformed(::pinch)
                    }
                }
            }
    ) {
        val room = size
        if (room.width <= 0f || room.height <= 0f) return@Canvas
        val box = fitted(room, wide)
        val middle = Offset(room.width / 2f, room.height / 2f)
        /*
         * ⚠️⚠️ **LO SPOSTAMENTO SI RIPORTA NEI BORDI QUI E NON SOLO NEL GESTO, DALLA `2.17`**: il
         * limite dipende dall'ingrandimento, quindi durante la corsa del doppio tocco un valore
         * buono per l'arrivo è **troppo** per un ingrandimento intermedio, e per qualche fotogramma
         * si vedrebbe una striscia di fondo da un lato. Applicarlo dove si disegna lo chiude per
         * ogni combinazione, e non toglie niente al gesto: la funzione è pura e applicarla due
         * volte dà lo stesso risultato.
         */
        val safe = reined(shift, scale, room, wide)
        // Il rettangolo da disegnare: quello adattato, ingrandito attorno al centro del palco e
        // poi spostato. ⚠️ **Si scala il RETTANGOLO e non la tela**: il pennello porta uno
        // shader con la sua matrice, e una tela scalata scalerebbe anche quella, cioè
        // ingrandirebbe il conto invece dell'immagine.
        val view = RectF(
            middle.x + (box.left - middle.x) * scale + safe.x,
            middle.y + (box.top - middle.y) * scale + safe.y,
            middle.x + (box.right - middle.x) * scale + safe.x,
            middle.y + (box.bottom - middle.y) * scale + safe.y
        )

        val image = BitmapShader(picture, TileMode.CLAMP, TileMode.CLAMP).apply {
            setLocalMatrix(
                Matrix().apply {
                    setRectToRect(
                        RectF(0f, 0f, picture.width.toFloat(), picture.height.toFloat()),
                        view,
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
        drawIntoCanvas { tela -> tela.drawRect(view.left, view.top, view.right, view.bottom, paint) }
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
private fun reined(want: Offset, zoom: Float, room: Size, wide: Float): Offset {
    val box = fitted(room, wide)
    val slackX = ((box.width() * zoom) - room.width).coerceAtLeast(0f) / 2f
    val slackY = ((box.height() * zoom) - room.height).coerceAtLeast(0f) / 2f
    return Offset(want.x.coerceIn(-slackX, slackX), want.y.coerceIn(-slackY, slackY))
}

/**
 * Il rettangolo in cui un'immagine larga [wide] entra dentro [room] senza deformarsi.
 *
 * ⚠️ **La leggono in due, il disegno e il gesto**, ed è la ragione per cui è una funzione: i
 * limiti della panoramica si contano sull'immagine adattata, quindi due conti scritti in due
 * posti darebbero una panoramica che si ferma dove l'immagine non finisce.
 */
private fun fitted(room: Size, wide: Float): RectF =
    if (room.width / room.height > wide) {
        val h = room.height
        val w = h * wide
        RectF((room.width - w) / 2f, 0f, (room.width + w) / 2f, h)
    } else {
        val w = room.width
        val h = w / wide
        RectF(0f, (room.height - h) / 2f, w, (room.height + h) / 2f)
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
    /** Se il bianco e nero lo spegne: vedi la nota sul suo `enabled`, nella scheda. */
    val dims: Boolean = false
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
    val dials: List<Dial>,
    val clear: (Look) -> Look,
    val spent: (Look) -> Boolean
)

/**
 * I moduli, nell'ordine in cui la fila li disegna.
 *
 * ⚠️⚠️ **L'ORDINE DEI CURSORI È QUELLO DEL PANNELLO DI LIGHTROOM, ED È IL SUO RIFERIMENTO** (giro
 * della `2.15`: *Lightroom ha solo 'Esposizione'*): chi apre questo editor ha in mente quello,
 * quindi un ordine nostro costringerebbe a cercare ogni volta il cursore che si sa già di voler
 * muovere. Vale per la Luce come per il Colore, dove temperatura e tinta vengono prima di quanto
 * i colori sono accesi.
 */
private val MODULES = listOf(
    Module(
        R.string.look_light,
        listOf(
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
        ),
        clear = { it.copy(light = Light.NONE) },
        spent = { !it.light.idle }
    ),
    Module(
        R.string.look_color,
        listOf(
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
                dims = true
            ),
            Dial(
                R.string.look_vibrance,
                { it.chroma.vibrance },
                { k, v -> k.copy(chroma = k.chroma.copy(vibrance = v)) },
                dims = true
            )
        ),
        clear = { it.copy(chroma = Chroma.NONE) },
        spent = { !it.chroma.idle }
    )
)

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
    onLive: ((Look) -> Look) -> Unit,
    onSettled: () -> Unit,
    onPeek: (((Look) -> Look)?) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOriginal: () -> Unit
) {
    /**
     * Quale modulo si sta guardando.
     *
     * ⚠️ **Vive nella scheda e non nel modello**: è dove si ha lo sguardo, non una proprietà
     * dell'immagine, quindi non entra nella storia dei passi e 'Annulla' non deve riportarcelo.
     */
    var module by rememberSaveable { mutableIntStateOf(0) }
    val chosen = MODULES[module]

    /**
     * Il cursore che sta alla riga [riga] del modulo che si sta guardando **adesso**, o `null` se
     * là non c'è niente (i moduli non hanno tutti lo stesso numero di cursori).
     *
     * ⚠️ **Legge [module] al momento della chiamata e non alla composizione**, ed è tutto il suo
     * valore: chiamata da dentro il rilevatore di un gesto, risponde con quello che il dito ha
     * davvero sotto il dito.
     */
    fun dialAt(riga: Int): Dial? = MODULES[module].dials.getOrNull(riga)
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MODULES.forEachIndexed { i, mod ->
                    ModuleChip(
                        name = stringResource(mod.name),
                        chosen = i == module,
                        spent = mod.spent(look),
                        enabled = ready && !busy,
                        onTap = { module = i },
                        onHold = {
                            onLive(mod.clear)
                            onSettled()
                        }
                    )
                }
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
            chosen.dials.forEachIndexed { riga, knob ->
                key(knob) {
                    LookKnob(
                        name = stringResource(knob.name),
                        value = knob.read(look),
                        span = knob.span,
                        stops = knob.stops,
                        /*
                         * ⚠️ **Col bianco e nero acceso i due cursori dei colori si spengono**:
                         * là non c'è più niente da saturare, e un cursore che si muove senza
                         * cambiare l'immagine si legge come un guasto.
                         */
                        enabled = ready && !busy && !(look.chroma.mono && knob.dims),
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
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
    chosen: Boolean,
    spent: Boolean,
    enabled: Boolean,
    onTap: () -> Unit,
    onHold: () -> Unit
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
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(face)
            .combinedClickable(
                enabled = enabled,
                role = Role.Tab,
                onClick = onTap,
                onLongClick = onHold,
                onLongClickLabel = wipe
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .alpha(if (enabled) 1f else OFF_INK),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, style = MaterialTheme.typography.labelLarge, color = ink)
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
    stops: Boolean = false
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
             */
            text = if (stops) "%+.2f".format(value) else "%+d".format((value * 100).roundToInt()),
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
    modifier: Modifier = Modifier
) {
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
                progressBarRangeInfo = ProgressBarRangeInfo(value, -span..span)
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
                    onLive(target.coerceIn(-span, span))
                    onSettled()
                    true
                }
            }
            .pointerInput(enabled, span) {
                if (!enabled) return@pointerInput
                val knob = DIAL_KNOB.toPx()
                /** Il valore che corrisponde a una posizione del dito. */
                fun valueAt(x: Float): Float {
                    val run = (size.width - 2f * knob).coerceAtLeast(1f)
                    val part = ((x - knob) / run).coerceIn(0f, 1f)
                    return -span + part * 2f * span
                }

                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    // Il tondo sta dove dice il valore: toccando lontano da lui si salta subito,
                    // toccandolo si trascina da dove è.
                    val run = (size.width - 2f * knob).coerceAtLeast(1f)
                    val here = knob + ((live + span) / (2f * span)) * run
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
        val part = (value + span) / (2f * span)
        val at = knob + part * run
        val zero = knob + 0.5f * run
        val hot = if (enabled) ink else faded
        val dead = if (enabled) rail else rail.copy(alpha = 0.5f)

        drawLine(
            dead, Offset(knob, middle), Offset(size.width - knob, middle), thick,
            cap = StrokeCap.Round
        )
        // Il tratto acceso parte dallo zero, perché questi cursori sono bipolari: un pieno che
        // partisse da sinistra direbbe che il valore neutro è già mezzo acceso.
        drawLine(hot, Offset(zero, middle), Offset(at, middle), thick, cap = StrokeCap.Round)
        // La tacca dello zero, che il tondo copre quando è al centro: senza, il valore neutro si
        // trova solo guardando il numero.
        drawLine(
            if (enabled) mark else faded,
            Offset(zero, middle - thick),
            Offset(zero, middle + thick),
            DIAL_ZERO.toPx()
        )
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

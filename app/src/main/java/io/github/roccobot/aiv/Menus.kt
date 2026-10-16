package io.github.roccobot.aiv

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

/**
 * La superficie di **ogni** menu dell'app: un riquadro che cresce, dove dice il posizionatore.
 *
 * ⚠️ **Una superficie sola per tutti i menu** (richiesta dell'utente, 1.46: un sistema
 * affidabile, dove una decisione su 'tutti i menu' si prende una volta). Con due meccanismi, ogni
 * cambio andava scritto due volte e uno dei due veniva dimenticato.
 *
 * `Popup` e non `DropdownMenu`, perché permette di scegliere il posto: un `DropdownMenu` si
 * posiziona contro il proprio genitore e non accetta un posizionatore, quindi non si mette 'al
 * centro della finestra' né 'sopra il FAB'. Le finestre sono le stesse: un `DropdownMenu` di
 * Material è un `Popup`. Non è un guscio da togliere: posiziona un menu dove Material non sa
 * metterlo.
 *
 * Il contenuto è libero: voci ([MenuRow]), un riquadro di tasti (`ActionPad`) o una fila di
 * tasti, come nel filtro della testata.
 *
 * I numeri dell'animazione li ha scelti l'utente su un mockup, e valgono per tutti i menu. Il
 * raggio non è un parametro ([MENU_ROUND], richiesta dell'utente, 1.28): uno stondamento dice
 * che cosa è una superficie, non quanto è larga.
 */
@Composable
fun MenuShell(
    state: MenuState,
    /** Dove va il menu: [MenuInWindow] oppure un [rememberMenuSpot]. */
    position: PopupPositionProvider,
    /**
     * Quanto deve essere largo almeno il pannello. Vedi [menuFloor], l'unico che lo passa.
     *
     * Non specificato vuol dire 'quanto il contenuto', come vogliono i menu non ancorati a una
     * griglia.
     */
    minWidth: Dp = Dp.Unspecified,
    content: @Composable () -> Unit
) {
    /*
     * Un avanzamento solo per pannello, sfocatura e patina: l'uscita è una dissolvenza veloce di
     * [MENU_OUT_MS] (riscontro dell'utente, giro della `1.66`), e una sfocatura che cala vuole in
     * scena qualcosa che se ne va con lei (il fatto per esteso è in fondo a `Veil.kt`).
     * Riaprendo a metà uscita non c'è salto: `animateTo` riparte dal valore corrente.
     *
     * ⚠️ **La prima composizione non anima**: un `LaunchedEffect` parte anche alla prima
     * composizione, dove `wanted` è falso e il valore è già zero, e `animateTo` terrebbe
     * l'animazione in corso per [MENU_OUT_MS]. In quel tempo [MenuState.visible] direbbe 'menu in
     * scena', con una finestra di popup vuota, il cancello dei tocchi di [MenuGuard] e il distacco
     * del FAB. È la stessa guardia di `TapHoldFab`. L'uguaglianza esatta regge: `animateTo` chiude
     * assegnando il valore d'arrivo.
     */
    LaunchedEffect(state.wanted) {
        val meta = if (state.wanted) 1f else 0f
        if (state.show.value == meta) return@LaunchedEffect
        state.show.animateTo(
            targetValue = meta,
            animationSpec = tween(
                durationMillis = if (state.wanted) MENU_IN else MENU_OUT_MS,
                easing = if (state.wanted) MENU_EASE else MENU_OUT
            )
        )
    }

    /*
     * ⚠️ **Fuori dal `Popup`**: la patina la dipinge l'app, e chiesta da qui la spegne la
     * composizione della schermata, che è quella che se ne va quando si naviga altrove. Dentro,
     * un menu chiuso navigando lascerebbe sull'app il suo scuro.
     */
    AppPatina { state.show.value }

    /*
     * ⚠️ **Il menu si dichiara in scena, così il tocco fuori non arriva anche sotto** (misurato
     * sul bytecode di Compose): `createFlags` di `AndroidPopup_androidKt` parte da
     * `FLAG_WATCH_OUTSIDE_TOUCH` e non mette mai `FLAG_NOT_TOUCH_MODAL`, quindi da Android 12 un
     * tocco fuori dal menu arriva a tutte e due le finestre: il popup si chiude, e l'app apre
     * la riga o la cartella sotto il dito.
     * Lo ripara un cancello solo, in `AivTheme` ([MenuGuard]), per ogni menu, perché [MenuShell]
     * è la sola via per aprirne uno.
     * È prima del cancello qui sotto, quindi vale anche durante l'uscita di [MENU_OUT_MS].
     */
    val inScena = state.visible
    DisposableEffect(state, inScena) {
        if (inScena) MenuScene.enter(state)
        onDispose { MenuScene.leave(state) }
    }

    /*
     * ⚠️ **A menu chiuso non deve esistere nessun `Popup`**: il FAB che si stacca nella sua
     * finestra non sarebbe più l'ultima aggiunta e finirebbe sotto, e un popup trasparente
     * sempre presente intercetterebbe i tocchi.
     */
    if (!state.visible) return

    Popup(
        /*
         * La finestra segue il pannello che cresce (voce `sfocatura-segue`, vedi [Growing]):
         * senza, resterebbe grande quanto il pannello a riposo mentre il disegno è al 96%, e la
         * sfocatura di finestra coprirebbe quella fascia, che si vede come una cornice.
         */
        popupPositionProvider = remember(position, state) { Growing(position) { grown(state) } },
        onDismissRequest = state::close,
        properties = PopupProperties(
            /*
             * ⚠️ **Costante: non deve cambiare durante un'animazione**. Un `PopupProperties` che
             * cambia fa riassegnare i flag della finestra, e per un fotogramma spariscono la
             * sfocatura e il velo (misurato sul bytecode, nota in fondo a `Veil.kt`). La finestra
             * si chiude col pannello, quindi non tiene mai il focus senza niente in scena.
             */
            focusable = true,
            dismissOnBackPress = true,
            // Sempre acceso: tutti i menu si chiudono toccando fuori.
            dismissOnClickOutside = true
        )
    ) {
        /*
         * ⚠️ **Dentro il `Popup`**: la sfocatura si applica alla finestra che ospita chi la
         * chiede, e fuori sarebbe quella della schermata (vedi [WindowVeil]).
         * Riceve [MenuState.show], lo stesso numero che muove opacità e scala: la sfocatura
         * cresce e cala col pannello, e a zero si spegne. Costa un `updateViewLayout` per
         * fotogramma durante l'animazione.
         * Qui c'è la sola sfocatura, attributo di finestra; il velo scuro lo chiede [AppPatina]
         * più su, e lo dipinge l'app (il perché è su `VeilStage`, in `Veil.kt`).
         */
        WindowVeil { state.show.value }
        /*
         * ⚠️ **Con l'ombra il menu si dà un'aria intorno**: la finestra di un `Popup` è grande
         * quanto quello che vi si misura, e l'ombra, che esce dal pannello, sarebbe tagliata di
         * netto sul suo rettangolo (il 'quadrato sfocato' bocciato dall'utente). Il conto è su
         * [LIFT_ROOM].
         * Solo con l'ombra: con la sfocatura la finestra deve restare grande quanto il pannello,
         * o sfoca una fascia intorno a lui. Le due vie vogliono finestre opposte, e per questo
         * sono alternative.
         */
        val alzato = LocalAivDepth.current == PanelDepth.SHADOW
        val ariaPx = with(LocalDensity.current) { if (alzato) LIFT_ROOM.toPx() else 0f }
        Surface(
            /*
             * Cresce da 0,96 in [MENU_IN], dal centro del riquadro (scelta dell'utente sul
             * mockup, che ha scartato l'apertura dal punto premuto): il movimento si sente e non
             * si guarda. Per questo il riquadro si mette in mezzo alla misura che dichiara (la
             * riga `place` qui sotto).
             *
             * La misura dichiarata è quella scalata: il `layout` misura il pannello intero e ne
             * riporta la frazione, così la finestra del `Popup` è grande quanto il pannello
             * disegnato (vedi [Growing]). Il contenuto è misurato coi vincoli interi, quindi
             * nessun testo va a capo diversamente mentre il menu entra.
             */
            modifier = Modifier
                /*
                 * ⚠️ **Il tocco sull'aria vale come il tocco fuori**: l'aria è della finestra
                 * del menu, quindi `dismissOnClickOutside` non la vede, come l'aria dei dialoghi
                 * (il perché è su `airTop` in `Centred.kt`).
                 * ⚠️ **È prima del `layout` di proposito** (misurato): la prova del tocco si
                 * ferma sul primo nodo di layout della catena, e così il riquadro di chi ascolta
                 * è quello dichiarato là sotto, l'unico che comprende l'aria.
                 * Non consuma niente: il tocco è già dentro la finestra del popup.
                 * ⚠️ **Senza ombra il nodo non esiste**: un nodo di tocco sempre in scena può
                 * bloccare tutti i tocchi dell'app (vedi [MenuGuard]).
                 */
                .then(
                    if (!alzato) Modifier else Modifier.pointerInput(state, ariaPx) {
                        awaitPointerEventScope {
                            while (true) {
                                val evento = awaitPointerEvent(PointerEventPass.Initial)
                                if (evento.type != PointerEventType.Press) continue
                                val dove = evento.changes.firstOrNull()?.position ?: continue
                                val aria = dove.x < ariaPx || dove.y < ariaPx ||
                                    dove.x > size.width - ariaPx ||
                                    dove.y > size.height - ariaPx
                                if (aria) state.close()
                            }
                        }
                    }
                )
                .layout { measurable, constraints ->
                    val pannello = measurable.measure(constraints)
                    val k = grown(state)
                    val w = (pannello.width * k).roundToInt()
                    val h = (pannello.height * k).roundToInt()
                    layout(w, h) {
                        pannello.place((w - pannello.width) / 2, (h - pannello.height) / 2)
                    }
                }
                .graphicsLayer {
                    alpha = state.show.value
                    val k = grown(state)
                    scaleX = k
                    scaleY = k
                }
                /*
                 * ⚠️ **L'aria dell'ombra è dentro l'opacità**: un'opacità minore di uno fa
                 * disegnare il sottoalbero in un buffer grande quanto questo nodo, e l'ombra
                 * sarebbe tagliata dal bordo del buffer, proprio agli angoli.
                 */
                .then(if (!alzato) Modifier else Modifier.padding(LIFT_ROOM))
                // L'ombra, quando è lei la scelta (vedi `Edge.kt`).
                .lifted(MENU_ROUND),
            shape = RoundedCornerShape(MENU_ROUND),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                /*
                 * L'ordine è quello di `DropdownMenuContent` (letto in `MenuKt`): il margine è
                 * fuori dallo scorrimento, quindi non scorre via, e la larghezza intrinseca è in
                 * mezzo.
                 * ⚠️ **Lo scorrimento serve**: in orizzontale, su una finestra alta 360dp,
                 * restano 264dp, e il menu del visualizzatore è alto circa 474dp.
                 * ⚠️ **Senza la larghezza intrinseca una voce sola si allarga a tutta la
                 * finestra**: un `DropdownMenuItem` chiede `fillMaxWidth`, e dentro un `Popup` i
                 * vincoli sono la finestra. Con un contenuto di larghezza fissa non cambia niente.
                 */
                modifier = Modifier
                    .padding(vertical = MENU_PAD)
                    // ⚠️ Il minimo va prima della larghezza intrinseca: i modificatori di misura
                    // si applicano da fuori a dentro, e scritto dopo la larghezza sarebbe già
                    // fissata.
                    .widthIn(min = minWidth)
                    .width(IntrinsicSize.Max)
                    .verticalScroll(rememberScrollState())
                    /*
                     * ⚠️ **Mentre esce, il menu non si tocca più**: un tocco su una voce che
                     * sbiadisce ne eseguirebbe l'azione. Si consuma nella passata iniziale,
                     * senza toccare i flag della finestra, che usa la sfocatura.
                     * E anche per il lettore di schermo: `deaf()` consuma eventi di puntatore,
                     * mentre un comando di accessibilità esegue l'azione dalla semantica (come
                     * sul FAB, in `ActionPad.kt`).
                     */
                    .then(if (state.wanted) Modifier else Modifier.deaf())
                    .then(if (state.wanted) Modifier else Modifier.clearAndSetSemantics { })
            ) { content() }
        }
    }
}

/**
 * Non lascia passare nessun tocco, consumandolo prima che arrivi ai figli.
 *
 * Nella passata iniziale, prima che i figli lo vedano: nella passata `Main` il nodo più interno
 * riceve per primo, e una voce lo consumerebbe lei.
 */
private fun Modifier.deaf(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    }
}

/** L'aria sopra e sotto il contenuto di ogni menu. */
private val MENU_PAD = 8.dp

/**
 * Da quanto piccolo cresce il menu, e in quanti millisecondi.
 *
 * La scala è scelta dell'utente. La durata, 120ms, è quella con cui Material fa entrare un
 * menu (richiesta dell'utente: animazioni più veloci): sotto, un movimento smette di leggersi
 * come tale.
 * ⚠️ Nessuna durata nasconde l'angolo sfocato che sporge dal pannello, che è fermo finché il
 * menu è aperto: che cosa lo toglierebbe è scritto in fondo a `Veil.kt`.
 */
private const val MENU_SMALL = 0.96f
private const val MENU_IN = 120

/** L'accelerazione di Material per una cosa che entra: parte decisa e si posa piano. */
private val MENU_EASE = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * L'uscita di un menu: una dissolvenza corta, e senza nessuna curva.
 *
 * 75 ms, la durata con cui Material fa sparire i propri menu (`OutTransitionDuration`), su
 * richiesta dell'utente (riscontro del giro della `1.66`: una dissolvenza veloce).
 * ⚠️ **Conta il tempo passato in scena**: le voci che navigano fanno comporre e animare una
 * schermata nuova mentre il menu esce, e un'uscita lunga lascerebbe il pannello a mezza opacità
 * sopra la schermata d'arrivo, a scatti. A 75 ms il pannello è già andato quando la schermata
 * arriva.
 * La curva è lineare: a quattro fotogrammi una curva non si vede, e una che parte piano sotto
 * carico si legge come un pannello che si inceppa.
 */
private const val MENU_OUT_MS = 75
private val MENU_OUT: Easing = LinearEasing

/**
 * Quanto è grande adesso il pannello, in frazione della sua misura a riposo.
 *
 * ⚠️ **In uscita vale uno**: questo numero lo leggono il `layout` del pannello, che rimisura
 * tutto il contenuto, e [Growing], che rimisura e riposiziona la finestra. Fermo a uno,
 * dell'uscita resta la sola opacità, letta nella fase di disegno, mentre una voce che naviga fa
 * comporre una schermata nuova.
 * Una chiusura a menu aperto trova già uno, quindi nessun salto; salta solo chiudere mentre il
 * menu sta ancora entrando, di quattro centesimi in un fotogramma.
 */
private fun grown(state: MenuState): Float =
    if (!state.wanted) 1f else MENU_SMALL + (1f - MENU_SMALL) * state.show.value

/**
 * Il posizionatore che mette la finestra rimpicciolita al posto di quella intera.
 *
 * ⚠️ **Toglie la cornice sfumata** (voce `sfocatura-segue`): la sfocatura copre il rettangolo
 * della finestra, e con la finestra grande quanto il pannello a riposo, mentre il disegno è al
 * 96%, resterebbe una fascia sfocata con un bordo netto. La finestra si stringe col disegno, e
 * la fascia non esiste in nessun fotogramma.
 *
 * Il posto si sceglie sulla misura intera, o un candidato che a riposo non entra potrebbe
 * entrare al 96% e il menu salterebbe mentre entra. La finestra rimpicciolita si centra dentro
 * quel rettangolo: il pannello cresce dal proprio centro, come ha scelto l'utente sul mockup.
 * La misura intera si ricava dividendo, con un errore di un pixel; a riposo la divisione è per
 * uno.
 * Costa un riposizionamento della finestra a ogni fotogramma dell'entrata (vedi la nota in fondo
 * a `Veil.kt`).
 */
private class Growing(
    private val dove: PopupPositionProvider,
    private val quanto: () -> Float
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val k = quanto().coerceIn(MENU_SMALL, 1f)
        val intero = IntSize(
            (popupContentSize.width / k).roundToInt(),
            (popupContentSize.height / k).roundToInt()
        )
        val at = dove.calculatePosition(anchorBounds, windowSize, layoutDirection, intero)
        return IntOffset(
            x = at.x + (intero.width - popupContentSize.width) / 2,
            y = at.y + (intero.height - popupContentSize.height) / 2
        )
    }
}

/**
 * Su un asse, da dove si apre il menu.
 *
 * ⚠️ **'Comincia' e 'finisce' seguono il verso di scrittura**: in arabo, persiano e urdu
 * cominciano a destra, e il manifesto dichiara `supportsRtl`.
 */
enum class MenuSide {
    /** Comincia dove comincia l'ancora, cioè allineato al suo bordo iniziale. */
    AT_ANCHOR,

    /** Dopo l'ancora, cioè sotto di lei o dal suo lato finale. */
    AFTER_ANCHOR,

    /**
     * Centrato sull'ancora: il cursore della luminosità del Disegno, che copre la fila dei tondi
     * (nota dell'utente su `4.49-02`).
     */
    ANCHOR_CENTRE,

    /** In mezzo alla finestra, senza guardare l'ancora. */
    IN_WINDOW,

    /** In mezzo alla finestra e il 15% più in basso, che in AIV vuol dire 'centrato'. */
    LOWERED_IN_WINDOW
}

/**
 * Il posizionatore di ogni menu: una politica per asse e una sola regola di bordo.
 *
 * ⚠️ **La collisione col bordo si scrive una volta**: è la regola di
 * `DropdownMenuPositionProvider`, trascritta dal bytecode di `material3` in uso, coi suoi
 * candidati, i suoi margini e il suo ripiego. Senza, un menu più alto dello schermo uscirebbe
 * dal bordo e non lo si potrebbe raggiungere.
 *
 * @param across la politica sull'asse orizzontale.
 * @param along la politica sull'asse verticale.
 * @param edge il margine dal bordo di finestra, in pixel.
 * @param air lo stesso margine in frazione della finestra: qui il `Density` non c'è, e un
 *   numero fisso sarebbe otto volte più grande su un telefono vecchio.
 */
class MenuSpot(
    private val across: MenuSide,
    private val along: MenuSide,
    private val edge: Int = 0,
    private val air: Float = 0f,
    /**
     * Entro quanti pixel dal bordo di finestra la distanza del pannello si **uniforma** a
     * [flushTo].
     *
     * ⚠️ **Il fianco del menu non deve essere vicino al bordo della colonna** (riscontro
     * dell'utente: i due bordi vicini, con la sfocatura, si leggono come una linea sfocata).
     * Il menu si ancora al FAB, quindi il suo fianco cade al margine del FAB: questa soglia
     * riconosce quel caso, e [flushTo] dice dove portarlo.
     * ⚠️ **È una soglia e non un margine** (misurato dal banco): un margine è un minimo, e non
     * muoverebbe il candidato naturale di un menu ancorato, che lo rispetta già.
     * Vale sui due lati e nei due versi di scrittura: il difetto è speculare. Solo in
     * orizzontale: in verticale il menu si stacca dal bordo di [edge], e uniformare lo farebbe
     * finire sotto la barra di sistema.
     */
    private val flush: Int = 0,
    /**
     * Dove si ferma la **finestra** quando [flush] scatta, in pixel dal bordo.
     *
     * Vale [MENU_INSET] meno l'aria, uguale con sfocatura, ombra o niente (voce `menu-bordo`):
     * il pannello è tirato dentro oltre il margine della griglia, lontano dal bordo della
     * colonna.
     * Lo calcola il chiamante, perché dipende dall'aria dell'ombra e dal `Density`, che qui non
     * ci sono. Vedi [rememberMenuSpot].
     */
    private val flushTo: Int = 0
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = IntOffset(
        x = place(
            spots(
                across, anchorBounds.left, anchorBounds.right,
                windowSize.width, popupContentSize.width, layoutDirection
            ),
            size = popupContentSize.width,
            space = windowSize.width,
            // Zero, come in `DropdownMenu`: in orizzontale un menu si appoggia al bordo. Lo tira
            // dentro dove serve [flush], che è una soglia e non un minimo.
            margin = 0
        ).let { appoggia(it, popupContentSize.width, windowSize.width, flush, flushTo) },
        y = place(
            spots(
                along, anchorBounds.top, anchorBounds.bottom,
                windowSize.height, popupContentSize.height, LayoutDirection.Ltr
            ),
            size = popupContentSize.height,
            space = windowSize.height,
            margin = edge + (windowSize.height * air).toInt()
        )
    )
}

/**
 * I posti possibili del menu su un asse, dal preferito all'ultima risorsa.
 *
 * L'ordine di [MenuSide.AT_ANCHOR] e [MenuSide.AFTER_ANCHOR] è quello che Material usa per un
 * menu che si apre sotto la sua ancora, letto nel bytecode.
 */
private fun spots(
    side: MenuSide,
    from: Int,
    to: Int,
    space: Int,
    size: Int,
    dir: LayoutDirection
): IntArray = when (side) {
    MenuSide.AT_ANCHOR -> intArrayOf(
        if (dir == LayoutDirection.Ltr) from else to - size,
        if (dir == LayoutDirection.Ltr) to - size else from,
        // Ultima risorsa: il bordo di finestra più vicino all'ancora.
        if ((from + to) / 2 < space / 2) 0 else space - size
    )
    MenuSide.AFTER_ANCHOR -> intArrayOf(to, from - size, from - size / 2)
    MenuSide.ANCHOR_CENTRE -> intArrayOf((from + to - size) / 2)
    MenuSide.IN_WINDOW -> intArrayOf((space - size) / 2)
    // Il centro più il 15%: 'centrato' in AIV vuol dire questo, e il numero è [LOWER_BY].
    MenuSide.LOWERED_IN_WINDOW -> intArrayOf((space - size) / 2 + (space * LOWER_BY).toInt())
}

/**
 * Porta il pannello a [fino] dal bordo di finestra quando gli manca meno di [entro] per
 * arrivarci.
 *
 * Il perché è su `MenuSpot.flush` e `MenuSpot.flushTo`; qui c'è il conto, una soglia sui due
 * lati. A [entro] zero non fa niente: `1..0` è un intervallo vuoto.
 * ⚠️ **La distanza si uniforma**: anche un pannello più vicino di [fino] è portato a [fino],
 * e oggi [fino] è più grande della distanza naturale, quindi il pannello è tirato dentro.
 */
private fun appoggia(at: Int, size: Int, space: Int, entro: Int, fino: Int): Int = when {
    at in 1..entro -> fino
    space - (at + size) in 1..entro -> space - size - fino
    else -> at
}

/**
 * Il posto sull'asse: il primo candidato che entra, e se nessuno entra l'ultimo riportato
 * dentro.
 */
private fun place(candidates: IntArray, size: Int, space: Int, margin: Int): Int {
    for (i in candidates.indices) {
        val at = candidates[i]
        if (at >= margin && at + size <= space - margin) return at
        if (i == candidates.lastIndex) {
            // Un menu più grande dello spazio fra i margini si centra e sfora dai due lati
            // uguali, come nel ripiego di Material.
            return if (size >= space - 2 * margin) (space - size) / 2
            else at.coerceIn(margin, space - margin - size)
        }
    }
    return 0
}

/**
 * Il menu al centro della finestra: il visualizzatore e il tocco lungo su una cartella.
 *
 * Il margine è una frazione e non pixel: vedi [MENU_AIR].
 */
val MenuInWindow = MenuSpot(MenuSide.IN_WINDOW, MenuSide.LOWERED_IN_WINDOW, air = MENU_AIR)

/**
 * Il menu ancorato al FAB che lo apre: la coppia di lati che i menu d'angolo dell'app chiedono
 * tutti uguale, scritta una volta sola come [MenuInWindow] per il caso centrato.
 */
@Composable
fun rememberMenuAtAnchor(): MenuSpot =
    rememberMenuSpot(MenuSide.AT_ANCHOR, MenuSide.AFTER_ANCHOR)

/**
 * Un menu ancorato a un angolo, con la coppia di lati scelta dal chiamante.
 *
 * Ricordato: costruirne uno nuovo a ogni ricomposizione farebbe rimisurare la finestra al
 * `Popup` senza ragione. I menu d'angolo dell'app passano da [rememberMenuAtAnchor]; questa è la
 * forma generale.
 *
 * Fra il pannello e il vetro restano [MENU_INSET], con sfocatura, ombra o niente: il perché è
 * su `MenuSpot.flush` e `MenuSpot.flushTo`.
 * ⚠️ **L'aria dell'ombra va sottratta, perché è dentro la finestra**: con l'ombra il menu si dà
 * `LIFT_ROOM` per lato (vedi [MenuShell]), e il pannello disegnato è `LIFT_ROOM` dentro il bordo
 * della finestra.
 */
@Composable
fun rememberMenuSpot(across: MenuSide, along: MenuSide): MenuSpot {
    val density = LocalDensity.current
    val depth = LocalAivDepth.current
    return remember(density, across, along, depth) {
        with(density) {
            val aria = if (depth == PanelDepth.SHADOW) LIFT_ROOM.roundToPx() else 0
            MenuSpot(
                across, along, MENU_KEEP_OUT.roundToPx(),
                // La soglia è il margine del FAB, dove cade il fianco di un menu ancorato (vedi
                // `MenuSpot.flush`); il FAB ha il centro sul tondo ([pillCorner]).
                flush = cornerSide(FAB_SIZE).roundToPx(),
                flushTo = (MENU_INSET.roundToPx() - aria).coerceAtLeast(0)
            )
        }
    }
}

/**
 * Quanto resta fra il fianco di un menu ancorato e il bordo dello schermo.
 *
 * Tre volte il margine della griglia, misurato sul mockup dell'utente (voce `menu-bordo`): il
 * pannello si ferma 24 dentro il bordo destro della griglia, che è a [GRID_PAD_X] dal vetro
 * (celle a 12-181, 185-354, 358-528 su 539 di larghezza, pannello da 137 a 504).
 * Legato a [GRID_PAD_X], resta giusto se il margine della griglia cambia. Vale con sfocatura,
 * ombra o niente (istruzione dell'utente).
 */
internal val MENU_INSET = GRID_PAD_X * 3

/**
 * Frazione massima della larghezza finestra per i menu del FAB sul tablet (≥600 dp).
 *
 * 0,35 (voce `3.38-02`): 0,45 era troppo largo, e senza tetto in verticale il menu arrivava al
 * 70% circa.
 */
internal const val MENU_TABLET_WIDTH = 0.35f

/**
 * La larghezza minima del pannello di un menu ancorato, sopra una griglia di [columns] colonne.
 *
 * Due celle più lo spazio che le separa, più due margini di griglia (voce `menu-bordo`: con
 * l'ombra il pannello più largo di due miniature e del loro spazio; i due margini sono la misura
 * del mockup).
 * ⚠️ **Il tetto serve all'elenco iniziale**: là le colonne sono due, e il conto darebbe un
 * pannello largo quanto la finestra. Limitato, il menu si ferma a [MENU_INSET] dai due lati,
 * comunque fuori squadra rispetto ai bordi delle colonne.
 * La finestra e non lo schermo, meno i rientri di lato: con un ritaglio o in orizzontale la
 * griglia comincia più dentro.
 */
@Composable
fun menuFloor(columns: Int, gap: Dp): Dp {
    val density = LocalDensity.current
    val window = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val sides = WindowInsets.safeDrawing.asPaddingValues()
    val dir = LocalLayoutDirection.current
    val usable = window - sides.calculateStartPadding(dir) - sides.calculateEndPadding(dir) -
        GRID_PAD_X * 2
    val cell = (usable - gap * (columns - 1)) / columns
    val floor = cell * 2 + gap + GRID_PAD_X * 2
    val insetCap = window - MENU_INSET * 2
    // Sul tablet al più [MENU_TABLET_WIDTH], in orizzontale e in verticale.
    val cap =
        if (window.value >= Adaptive.PHONE_MAX) {
            minOf(insetCap, window * MENU_TABLET_WIDTH)
        } else {
            insetCap
        }
    return minOf(floor, cap)
}

/**
 * Quanta aria resta comunque sotto un menu centrato, in frazione dell'altezza della finestra.
 *
 * Una frazione e non pixel: `calculatePosition` non riceve un `Density`, e un numero fisso in
 * pixel varierebbe molto fra un telefono vecchio e uno di oggi. Per questo non è `LOWER_AIR`,
 * che è in dp perché là il `Density` c'è.
 */
private const val MENU_AIR = 0.02f

/** Il margine dal bordo di finestra di un menu ancorato: quello di Material, letto in `MenuKt`. */
private val MENU_KEEP_OUT = 48.dp

/**
 * Lo stato di un menu: se deve essere in scena, e se lo è ancora perché si sta chiudendo.
 *
 * ⚠️ **Non è un booleano del chiamante**: con un `if` di chi lo apre l'uscita non potrebbe
 * esistere, perché la finestra sparirebbe dalla composizione al primo fotogramma. La presenza
 * la decide [visible], vero per tutta l'uscita, come fa Material col proprio popup. Una voce che
 * chiudesse il menu assegnando `false` a una sua variabile non compila.
 */
class MenuState internal constructor() {
    var wanted by mutableStateOf(false)
        private set

    internal val show = Animatable(0f)

    /**
     * Se il pannello si vede adesso, chiusura compresa. Coincide con la vita della finestra,
     * della sfocatura e del velo scuro: lo leggono anche il FAB, che resta staccato sopra il
     * velo, e il cancello del menu del visualizzatore.
     */
    val visible: Boolean get() = visto

    /*
     * ⚠️ **Derivato**: la condizione legge il valore dell'animazione, e letta in composizione
     * invaliderebbe chi la legge a ogni fotogramma; `derivedStateOf` invalida solo quando cambia
     * l'esito.
     * ⚠️ **Niente `show.isRunning`**: direbbe 'in scena' anche con un'animazione che corre a
     * valore zero, come alla prima composizione (vedi la guardia in [MenuShell]). Presidiato da
     * `CambioSchermataTest`; il contesto è in `Rules.md`, § '🎬 Le animazioni dentro una
     * schermata che arriva'.
     */
    private val visto by derivedStateOf { wanted || show.value > 0f }

    fun open() {
        wanted = true
    }

    fun close() {
        wanted = false
    }
}

/**
 * @param keys quello che, cambiando, crea uno stato nuovo (e quindi un menu chiuso): nel
 *   visualizzatore è l'immagine in scena.
 */
@Composable
fun rememberMenuState(vararg keys: Any?): MenuState = remember(*keys) { MenuState() }


/**
 * Una voce di menu a tutta larghezza: icona, testo, e un tocco lungo se serve.
 *
 * ⚠️ **Tutte le voci a tutta larghezza dei menu passano di qui**, quindi si allineano per
 * costruzione (mockup dell'utente, 1.28): con due implementazioni della stessa fila i testi
 * cominciavano in due posti diversi.
 *
 * ⚠️ **Non può essere un `DropdownMenuItem`**: il suo `clickable` è in fondo alla sua catena, e
 * un `combinedClickable` passato nel `modifier` non vedrebbe mai la pressione, perché nella
 * passata `Main` il nodo più interno riceve per primo. Come per [TapHoldFab]: per due gesti su
 * un comando, il nodo che ascolta dev'essere uno.
 *
 * Le misure sono lette nel bytecode di `MenuKt` (`material3` `1.5.0-alpha26`): altezza minima
 * 48dp, spazio fra icona e testo 8dp (`DropdownMenuIconTextPadding`, 12 solo col mouse), corpo
 * `labelLarge`; i rientri sono su [MENU_ITEM_SIDE] e [MENU_ITEM_LEFT]. I colori si chiedono a
 * Material (`MenuDefaults.itemColors()`), perché cambiano con la tavolozza.
 *
 * @param holdLabel che cosa fa il tocco lungo, per il lettore di schermo: senza, il gesto lo
 *   scopre solo chi ci arriva per caso. Va insieme a [onHold]: o tutti e due o nessuno.
 */
@Composable
fun MenuRow(
    text: String,
    /**
     * Il glifo davanti al testo, e `null` per un menu che non ne ha.
     *
     * ⚠️ **La regola è per menu e non per voce**: dentro un menu le voci si allineano, quindi
     * l'icona o ce l'hanno tutte o nessuna. Oggi nessun menu la passa nulla, ma il nullo resta
     * per un menu le cui voci non hanno un disegno.
     */
    icon: ImageVector?,
    onTap: () -> Unit,
    holdLabel: String? = null,
    onHold: (() -> Unit)? = null,
    /** Se la voce si può toccare adesso. Spenta resta in scena, in grigio. */
    enabled: Boolean = true,
    /**
     * Se la voce fa una cosa da cui non si torna indietro, e va scritta in colore d'errore.
     *
     * Da spenta non cambia niente: il colore dello spento è un'altra chiave della tavolozza, e
     * un rosso smorto si leggerebbe come un errore in corso.
     */
    danger: Boolean = false
) {
    // Anche i colori dello spento si chiedono a Material (`disabledTextColor`,
    // `disabledLeadingIconColor`): un'opacità copiata a mano si scollerebbe dalla libreria.
    val colors = MenuDefaults.itemColors(
        textColor = if (danger) MaterialTheme.colorScheme.error else Color.Unspecified,
        leadingIconColor = if (danger) MaterialTheme.colorScheme.error else Color.Unspecified
    )
    /*
     * Una catena sola, con o senza tocco lungo: `onLongClick` nullo non installa nessun gesto
     * lungo, e il nodo che ascolta è sempre uno. Presidiato da `MenuRowTest`, che verifica il
     * tocco breve e quello lungo, con e senza il secondo gesto.
     */
    val gestures = Modifier.combinedClickable(
        enabled = enabled,
        onLongClickLabel = holdLabel,
        onLongClick = onHold?.let { withHaptics(it) },
        hapticFeedbackEnabled = false,
        onClick = onTap
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Le due larghezze sono di Material, lette in `MenuKt`: senza il tetto una voce si
            // allargherebbe fino alla finestra nelle lingue che scrivono lungo.
            .sizeIn(
                minWidth = MENU_ITEM_MIN,
                maxWidth = MENU_ITEM_MAX,
                minHeight = MENU_ITEM_HEIGHT
            )
            .then(gestures)
            /*
             * Il rientro di sinistra è più largo, per il glifo che sporge (voce `icona-copia`, vedi
             * [MENU_ITEM_LEFT]): si sposta il contenuto di tutte le voci, come ha chiesto
             * l'utente, così restano allineate fra loro. Senza icona il rientro in più non serve.
             */
            .padding(
                start = if (icon != null) MENU_ITEM_LEFT else MENU_ITEM_SIDE,
                end = MENU_ITEM_SIDE
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MENU_ITEM_GAP)
    ) {
        // L'icona non ha descrizione e il testo sì: il modificatore di gesto fonde le semantiche
        // dei figli, e TalkBack legge una voce sola. Vedi `PadButton`.
        /*
         * ⚠️ **Lo slot è sempre 24dp e il disegno può essere più grande**: `Glyphs.PhotoPair` è
         * 25dp (il perché è su `Glyphs.COPY_IMAGE`), e quel dp in più deve sporgere, o il testo
         * di quella voce comincerebbe più a destra delle altre.
         * `requiredSize` e non `size`: `size` negozia col genitore, e lo slot da 24 schiaccerebbe
         * il disegno da 25 (misura in `Identity.kt`, su `LAUNCHER_ZOOM`).
         * In basso a destra, così quello che sporge va a sinistra e in alto, da dove esce quel
         * glifo; per i glifi da 24 l'allineamento non cambia niente.
         */
        if (icon != null) {
            Box(
                modifier = Modifier.size(MENU_ITEM_ICON),
                contentAlignment = Alignment.BottomEnd
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) {
                        colors.leadingIconColor
                    } else {
                        colors.disabledLeadingIconColor
                    },
                    modifier = Modifier.requiredSize(icon.defaultWidth, icon.defaultHeight)
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) colors.textColor else colors.disabledTextColor
        )
    }
}

/** Le misure di una voce di menu, lette in `MenuKt`. Vedi [MenuRow]. */
private val MENU_ITEM_HEIGHT = 48.dp
private val MENU_ITEM_MIN = 112.dp
private val MENU_ITEM_MAX = 280.dp
/**
 * Il rientro dei due fianchi di una voce: 16 e non i 12 di Material, perché le parole non
 * arrivino a filo del bordo (riscontro dell'utente, giro della `1.56`).
 */
private val MENU_ITEM_SIDE = 16.dp
private val MENU_ITEM_GAP = 8.dp

/**
 * Il rientro di sinistra di una voce: [MENU_ITEM_SIDE] più tre punti, per il glifo che sporge
 * di 1dp oltre lo slot (l'utente ha chiesto 2 o 3 pixel apparenti: con 3 quel glifo non tocca il
 * margine). A destra resta [MENU_ITEM_SIDE]: si sposta il contenuto, la scheda non si allarga
 * in modo simmetrico.
 */
private val MENU_ITEM_LEFT = MENU_ITEM_SIDE + 3.dp

/**
 * Lo slot dell'icona di una voce, e vale per ogni glifo qualunque sia il suo disegno.
 *
 * La misura di serie di `Icon`, scritta perché un glifo dichiara 25dp: senza, lo slot lo
 * deciderebbe il disegno più grande. Vedi [MenuRow].
 */
private val MENU_ITEM_ICON = 24.dp

/**
 * Dove cade il centro dell'icona di una voce, misurato dal fianco del pannello.
 *
 * ⚠️ **Il riquadro a icone si allinea qui** (richiesta dell'utente, giro della `1.58`): le icone
 * delle voci e la prima colonna della griglia di icone devono cadere sulla stessa verticale. Un
 * rientro calcolato a parte, in frazione della larghezza del pannello, si scosterebbe nelle
 * lingue con parole lunghe.
 */
val MENU_ICON_MID = MENU_ITEM_LEFT + MENU_ITEM_ICON / 2

/**
 * Lo stondamento di ogni menu, uno solo per tutti e non un parametro (richiesta dell'utente,
 * 1.28): un parametro inviterebbe a ridiventare tre numeri diversi.
 * 20 e non 28: le schede in fondo hanno 28 (`PANEL_ROUND`), e un menu con lo stesso raggio non si
 * distinguerebbe da loro.
 */
val MENU_ROUND = 20.dp

/**
 * Quali menu sono in scena: serve a dire all'app di ignorare i tocchi ([MenuGuard]).
 *
 * ⚠️ **Non si può riusare `VeilStage`**: quella mappa è vuota quando la Sfocatura dietro i
 * pannelli è spenta, e il tocco che non deve passare dipenderebbe da un'impostazione. Qui un
 * menu entra sempre.
 * Una mappa di richiedenti e non un numero, come `VeilStage`: due menu possono sovrapporsi
 * durante un'uscita, e con un contatore il primo che esce toglierebbe anche il secondo.
 */
internal object MenuScene {
    private val aperti = mutableStateMapOf<Any, Unit>()

    /** Se c'è almeno un menu in scena. Si legge in composizione, da [MenuGuard]. */
    val open: Boolean get() = aperti.isNotEmpty()

    fun enter(chi: Any) {
        aperti[chi] = Unit
    }

    fun leave(chi: Any) {
        aperti.remove(chi)
    }

    /** La rete contro il menu fantasma, gemella di `VeilStage.clear`. */
    fun clear() {
        aperti.clear()
    }
}

/**
 * Il cancello che, mentre un menu è aperto, impedisce al tocco di arrivare anche all'app.
 *
 * ⚠️ **La finestra di un popup non è modale al tocco** (misurato sul bytecode di Compose):
 * `createFlags` di `AndroidPopup_androidKt` parte da `FLAG_WATCH_OUTSIDE_TOUCH` e non mette mai
 * `FLAG_NOT_TOUCH_MODAL`, quindi da Android 12 un dito fuori dal pannello arriva a tutte e due
 * le finestre: il popup si chiude, e l'app apre quello che c'è sotto il dito.
 * Uno solo, in `AivTheme`, sopra tutto quello che l'app disegna: vale per ogni menu, presente e
 * futuro. Consuma nella passata `Initial`, prima di chiunque altro; al tocco risponde il popup,
 * che è in un'altra finestra.
 *
 * ⚠️ **A menu chiuso non esiste, e non basterebbe che non consumasse**: un nodo con
 * `pointerInput` grande quanto lo schermo ferma la prova del tocco, che fra fratelli sovrapposti
 * si ferma sul primo ramo colpito, e l'app intera non risponderebbe più. Il rimedio è l'assenza:
 * costa una ricomposizione della radice a ogni apertura e chiusura di un menu.
 *
 * I dialoghi non ne hanno bisogno: la finestra di un `Dialog` è modale al tocco. Il loro caso
 * gemello è in `Centred.kt`, sull'aria dichiarata sopra il pannello.
 */
@Composable
fun MenuGuard(modifier: Modifier = Modifier) {
    DisposableEffect(Unit) { onDispose { MenuScene.clear() } }
    if (!MenuScene.open) return
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                }
            }
    )
}

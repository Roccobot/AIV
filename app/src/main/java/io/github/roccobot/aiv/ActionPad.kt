package io.github.roccobot.aiv

import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Le operazioni sui file, come riquadro di icone.
 *
 * ⚠️ **Un pezzo solo per i due posti che lo usano**, la selezione nella griglia e il tocco lungo
 * nel visualizzatore: l'ordine dei tasti non deve cambiare fra una schermata e l'altra.
 * L'ordine di fabbrica è quello dell'utente, e non si cambia 'per sicurezza'.
 * ⚠️ **Il colore dell'errore su 'Elimina' non è decorativo**: la voce è accanto a 'Sposta', e il
 * colore è l'unica cosa che distingue la voce irreversibile dalle vicine.
 * Icona più parola: 'Copia' e 'Sposta' hanno glifi simili, e la parola evita di sbagliare tasto.
 */
@Composable
fun ActionPad(
    actions: List<PadAction>,
    modifier: Modifier = Modifier,
    columns: Int = PAD_COLUMNS,
    /**
     * Se le celle si dividono tutta la larghezza invece di misurare [PAD_CELL].
     *
     * Serve alla scheda della selezione, larga quanto lo schermo. Nel menu del tocco lungo la
     * larghezza fissa è quella giusta: là il riquadro si adatta al contenuto.
     */
    stretch: Boolean = false,
    /**
     * Se sotto ogni icona si legge la parola ('Etichette sotto le icone').
     *
     * Arriva da [LocalPadLook]: questi riquadri vivono in finestre che le impostazioni non
     * ricevono. Chi ha una ragione per ignorare l'interruttore lo passa a mano.
     */
    labels: Boolean = LocalPadLook.current.labels
) {
    /*
     * Il riquadro disteso si misura prima di disegnarsi, per sapere se le colonne entrano (vedi
     * `cella` in [PadBody]).
     * ⚠️ **Il ramo a larghezza fissa non passa di qui**: `BoxWithConstraints` è un
     * `SubcomposeLayout`, e dentro una misura intrinseca, quella dei menu, non si può fare.
     */
    if (stretch) {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            PadBody(
                actions = actions,
                columns = columns,
                stretch = true,
                labels = labels,
                // Al netto dei due fianchi, che il corpo mette come padding.
                room = maxWidth - PAD_EDGE * 2
            )
        }
    } else {
        PadBody(
            actions = actions,
            modifier = modifier,
            columns = columns,
            stretch = false,
            labels = labels,
            room = Dp.Unspecified
        )
    }
}

/**
 * Il corpo del riquadro: le righe di celle, con la larghezza già decisa da chi lo chiama.
 *
 * @param room quanto spazio hanno le celle in tutto, o [Dp.Unspecified] dove non si misura.
 */
@Composable
private fun PadBody(
    actions: List<PadAction>,
    columns: Int,
    stretch: Boolean,
    labels: Boolean,
    room: Dp,
    modifier: Modifier = Modifier
) {
    /*
     * ⚠️ **Nei menu il fianco sinistro è derivato dalla colonna delle icone della lista**
     * ([MENU_ICON_MID]), perché le icone delle voci sopra cadano sulla stessa verticale della
     * prima colonna (richiesta dell'utente): il bordo va mezza cella prima del centro voluto.
     * Col riquadro a etichette il conto darebbe un rientro negativo, e il pavimento riporta a
     * [PAD_EDGE]. Nelle schede (`stretch`) sopra non c'è nessuna lista, e il fianco è [PAD_EDGE].
     * La distinzione è [stretch] e non [labels]: in un menu senza parole l'allineamento serve.
     */
    val piena = if (labels) PAD_CELL else PAD_CELL_BARE
    /*
     * ⚠️ **La cella si stringe se le colonne non entrano**, con lo stesso conto di [PadArrange]:
     * cinque celle da 76dp più i distacchi fanno 412dp, più di uno schermo da 360. Il tetto resta
     * [PAD_CELL]. Vale solo con [stretch]: dove la larghezza è intrinseca (i menu) il vincolo può
     * essere illimitato, e il conto darebbe una cella infinita.
     */
    val cella = if (stretch && room != Dp.Unspecified) {
        ((room - PAD_GAP * (columns - 1)) / columns).coerceAtMost(piena)
    } else {
        piena
    }
    val avvio =
        if (stretch) PAD_EDGE else (MENU_ICON_MID - cella / 2).coerceAtLeast(PAD_EDGE)
    Column(
        modifier = modifier.padding(start = avvio, end = PAD_EDGE, top = PAD_GAP, bottom = PAD_GAP),
        verticalArrangement = Arrangement.spacedBy(PAD_GAP)
    ) {
        /*
         * Senza le parole la cella è [PAD_CELL_BARE]: [PAD_CELL] è larga quanto una parola, e
         * con le sole icone lascerebbe troppa aria.
         * ⚠️ **Senza etichette, e nelle schede, lo spazio fra le celle si distribuisce sulla
         * larghezza** (riscontro dell'utente): la larghezza del menu la fanno le voci di testo
         * sopra, e le celle resterebbero ammucchiate a sinistra. Si distribuisce lo spazio e non
         * la cella, così il centro della prima dipende solo dal fianco ([avvio]) e non dalla
         * lingua. I vuoti sono tutti uguali, quindi le colonne restano allineate fra le righe.
         */
        val disteso = stretch || !labels
        // ⚠️ Il minimo è la misura naturale del riquadro: in una colonna a larghezza intrinseca
        // un figlio che chiede tutta la larghezza non ne dichiara nessuna.
        val minimo = cella * columns + PAD_GAP * (columns - 1)
        // Le righe si ricavano a gruppi: il numero delle azioni cambia (nel cestino, per
        // esempio), e l'ultima riga può essere corta.
        for (row in actions.chunked(columns)) {
            Row(
                modifier = if (disteso) Modifier.fillMaxWidth().widthIn(min = minimo) else Modifier,
                horizontalArrangement =
                    if (disteso) Arrangement.SpaceBetween else Arrangement.spacedBy(PAD_GAP)
            ) {
                for (action in row) {
                    PadButton(action = action, modifier = Modifier.width(cella), labels = labels)
                }
                /*
                 * ⚠️ L'ultima riga corta si riempie di posti vuoti larghi come una cella, o le
                 * sue celle si spargerebbero e non cadrebbero sulle colonne di sopra. Non
                 * elastici: si prenderebbero tutto lo spazio avanzato.
                 */
                if (disteso) {
                    repeat(columns - row.size) { Spacer(Modifier.width(cella)) }
                }
            }
        }
    }
}

/**
 * Lo stesso riquadro, ma per cambiare l'ordine: la replica nelle impostazioni.
 *
 * ⚠️ **Si riordina un riquadro a griglia con lo stesso riquadro** (riscontro dell'utente): un
 * elenco di righe chiederebbe di tradurre a mente 'terzo dall'alto' in 'seconda colonna'.
 * Le celle sono tutte uguali, quindi il posto d'arrivo è la posizione del dito divisa per il
 * passo e arrotondata. La larghezza si stringe se non entra, come il riquadro vero con
 * `stretch`.
 * Mentre si trascina, il riquadro si ricompone a ogni pixel col tasto preso già al posto
 * d'arrivo: quello che si vede è il risultato, andata a capo compresa (richiesta dell'utente).
 * Niente maniglia, su sua indicazione: il gesto è il tocco lungo, e la riga di istruzioni sopra
 * lo scrive.
 *
 * Le parole ci sono anche con 'Etichette sotto le icone' spenta: qui un tasto si nomina per
 * spostarlo, la cella ha un'altezza fissa ([ARRANGE_HIGH]) che serve al passo, e la somiglianza
 * chiesta riguarda la disposizione. Un riquadro che nel modello non ha parole (la fila dei moduli
 * dell'editor completo) non le ha neanche qui: il nome resta nel `contentDescription` dell'icona,
 * e le due azioni parlate restano.
 *
 * @param columns quante colonne ha il riquadro vero: la replica va a capo dove va a capo lui.
 * @param labels se sotto il glifo si scrive la parola, cioè se il riquadro vero ce l'ha.
 */
@Composable
fun PadArrange(
    order: List<PadKey>,
    columns: Int,
    onOrder: (List<PadKey>) -> Unit,
    modifier: Modifier = Modifier,
    labels: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    val prima = stringResource(R.string.settings_buttons_before)
    val dopo = stringResource(R.string.settings_buttons_after)
    val righe = (order.size + columns - 1) / columns
    /*
     * Senza la parola la cella vale il glifo più l'aria che il tasto vero gli lascia
     * ([MODULE_PAD]), non [ARRANGE_HIGH].
     */
    val alta = if (labels) ARRANGE_HIGH else PAD_ICON + MODULE_PAD * 2
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val larga = ((maxWidth - PAD_GAP * (columns - 1)) / columns).coerceAtMost(PAD_CELL)
        val passoX = with(LocalDensity.current) { (larga + PAD_GAP).toPx() }
        val passoY = with(LocalDensity.current) { (alta + PAD_GAP).toPx() }

        var preso by remember { mutableStateOf<PadKey?>(null) }
        var scarto by remember { mutableStateOf(Offset.Zero) }

        /** Dov'è la cella numero [i], in pixel dal vertice del riquadro. */
        fun posto(i: Int) = Offset((i % columns) * passoX, (i / columns) * passoY)

        /*
         * ⚠️ **Si legge dallo stato vivo**: la chiamano la composizione e la fine del
         * trascinamento, dentro un `pointerInput` ricordato. Un indice calcolato una volta
         * resterebbe quello del primo fotogramma.
         */
        fun bersaglio(da: Int): Int {
            if (da < 0) return -1
            val p = posto(da) + scarto
            val col = (p.x / passoX).roundToInt().coerceIn(0, columns - 1)
            val rig = (p.y / passoY).roundToInt().coerceIn(0, righe - 1)
            return (rig * columns + col).coerceIn(0, order.lastIndex)
        }

        val da = order.indexOf(preso)
        val a = bersaglio(da)
        val visto = if (da < 0 || a < 0) order else order.moved(da, a)

        Box(modifier = Modifier.height(alta * righe + PAD_GAP * (righe - 1))) {
            for (chiave in order) {
                val suo = chiave == preso
                val meta = if (suo) posto(da) + scarto else posto(visto.indexOf(chiave))
                /*
                 * ⚠️ Un `Animatable`: mentre il dito è giù si fa `snapTo`, e al rilascio
                 * l'animazione parte da dove il dito l'ha lasciato. Un `animate*AsState`
                 * inseguirebbe sempre, e il tasto salterebbe.
                 */
                val moto = remember(chiave) { Animatable(posto(order.indexOf(chiave)), Offset.VectorConverter) }
                LaunchedEffect(meta, suo) {
                    if (suo) moto.snapTo(meta)
                    else moto.animateTo(meta, spring(stiffness = Spring.StiffnessMediumLow))
                }
                val tinta = MaterialTheme.colorScheme.primary
                /*
                 * Il tasto irreversibile ha il colore dell'errore anche qui, come nel riquadro
                 * vero (vedi [ActionPad]). Il rosso si legge: il fondo della cella è l'accento
                 * al 10% ([ARRANGE_BED]).
                 */
                val inchiostro =
                    if (chiave.danger()) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                Box(
                    modifier = Modifier
                        .offset { IntOffset(moto.value.x.roundToInt(), moto.value.y.roundToInt()) }
                        // Il tasto preso è sopra gli altri, o passerebbe sotto il vicino che
                        // scavalca.
                        .zIndex(if (suo) 1f else 0f)
                        .size(width = larga, height = alta)
                        .clip(RoundedCornerShape(PAD_CORNER))
                        .background(tinta.copy(alpha = if (suo) ARRANGE_HELD else ARRANGE_BED))
                        .pointerInput(chiave, order, columns, passoX, passoY) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    preso = chiave
                                    scarto = Offset.Zero
                                    haptics.performHapticFeedback(HOLD_BUZZ)
                                },
                                onDrag = { evento, delta ->
                                    evento.consume()
                                    scarto += delta
                                },
                                onDragEnd = {
                                    val partenza = order.indexOf(chiave)
                                    val fine = bersaglio(partenza)
                                    if (fine >= 0 && fine != partenza) {
                                        onOrder(order.moved(partenza, fine))
                                    }
                                    preso = null
                                    scarto = Offset.Zero
                                },
                                onDragCancel = {
                                    preso = null
                                    scarto = Offset.Zero
                                }
                            )
                        }
                        .semantics {
                            // ⚠️ Con un lettore di schermo non si trascina: queste due azioni
                            // sono l'unica via al riordino, come nell'elenco dei campi della
                            // scheda Info.
                            customActions = listOf(
                                CustomAccessibilityAction(prima) {
                                    val i = order.indexOf(chiave)
                                    onOrder(order.moved(i, i - 1)); true
                                },
                                CustomAccessibilityAction(dopo) {
                                    val i = order.indexOf(chiave)
                                    onOrder(order.moved(i, i + 1)); true
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val nome = stringResource(chiave.label())
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(PAD_LABEL_GAP, Alignment.CenterVertically)
                    ) {
                        Icon(
                            imageVector = chiave.glyph(),
                            // Senza la parola il nome va qui; col testo sotto sarebbe letto due
                            // volte a chi ascolta.
                            contentDescription = if (labels) null else nome,
                            tint = inchiostro,
                            modifier = Modifier.size(PAD_ICON)
                        )
                        if (labels) {
                            Text(
                                text = nome,
                                style = padLabel(),
                                color = inchiostro,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Le stesse operazioni, come pannello che entra dal basso: la selezione multipla.
 *
 * ⚠️ **Non è una `ModalBottomSheet`** (richiesta dell'utente): quella si prende i tocchi di tutto
 * il resto, e con lei aperta non si potrebbe più cambiare la selezione. Qui è una `Surface` in
 * fondo al `Box` della schermata. Il FAB della selezione non serve più: il pannello si apre da sé.
 * ⚠️ **Niente maniglia**: un segno di trascinamento promette un gesto che il pannello non ha, e un
 * trascinamento qui competerebbe con lo scorrimento della griglia sotto (riscontro
 * `niente-ombre`). Si chiude col tasto Indietro e si riapre quando la selezione riparte.
 */
@Composable
fun BoxScope.PickSheet(visible: Boolean, actions: List<PadAction>, onHeight: (Int) -> Unit = {}) {
    val finestra = LocalWindowInfo.current.containerSize.height
    AnimatedVisibility(
        visible = visible,
        /*
         * Entrata e uscita sono quelle di tutte le schede che arrivano dal basso (istruzione
         * dell'utente: l'uscita è l'entrata speculare); la forma del gesto vive in `Sheet.kt`.
         */
        enter = arrivaDalBasso(),
        exit = vaGiu(),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        /*
         * ⚠️ **Quanto la scheda copre dal fondo si dichiara a ogni fotogramma** in [FootStage], e
         * non è [onHeight]: quello è l'altezza del contenuto, che serve alla griglia; questo è lo
         * spazio occupato adesso, che cambia mentre la scheda sale e scende.
         * A salire è la notifica e non la scheda: muovere la scheda sposterebbe i comandi mentre
         * il dito sta per toccarli. Si misura la posizione senza una seconda animazione, quindi
         * chi la legge segue le curve della scheda per costruzione. Si misura sul contenitore,
         * che arriva al bordo dello schermo.
         */
        val quota = remember { Any() }
        DisposableEffect(quota) { onDispose { FootStage.off(quota) } }
        Surface(
            /*
             * Il bordo d'accento ce l'ha anche lei, per decisione dell'utente, anche se non ha la
             * Sfocatura dietro i pannelli: il bordo dice 'sono una superficie di questa app', la
             * sfocatura 'mi apro sopra qualcosa'. Solo la cima con i due archi: i fianchi corrono
             * fuori dallo schermo (vedi `edgedTop`).
             */
            modifier = Modifier
                .edgedTop(PANEL_ROUND)
                .onGloballyPositioned {
                    FootStage.cover(quota, finestra - it.positionInWindow().y.roundToInt())
                },
            shape = RoundedCornerShape(topStart = PANEL_ROUND, topEnd = PANEL_ROUND),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            /*
             * ⚠️ **Niente ombra né rilievo tonale** (richiesta dell'utente): appoggiata al bordo,
             * dell'ombra resterebbe solo la striscia di sotto, sull'angolo del vetro; il rilievo
             * tonale cambia il colore solo quando il colore è `surface`, e qui non lo è. A
             * staccare la scheda sono il colore e il bordo d'accento.
             */
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    /*
                     * ⚠️ **Il rientro di sistema è qui**: il fondo della scheda passa sotto la
                     * barra di navigazione, che prende il suo colore (richiesta dell'utente), e il
                     * contenuto no. Per questo la scheda vive nel `Box` di radice della
                     * schermata: dentro la colonna il rientro è già consumato.
                     */
                    .navigationBarsPadding()
                    /*
                     * ⚠️ **L'altezza si misura, dopo il rientro**: serve alla griglia per non
                     * lasciare l'ultima fila sotto il pannello, anche quando un'etichetta va a
                     * capo. La griglia vive in uno spazio già senza la barra, e un'altezza che la
                     * comprendesse lascerebbe un buco.
                     */
                    .onSizeChanged { onHeight(it.height) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ActionPad(actions = actions, columns = SHEET_COLUMNS, stretch = true)
            }
        }
    }
}

/**
 * Quanta parte del fondo dello schermo è occupata, in questo fotogramma.
 *
 * ⚠️ **Chi deve saperlo non vive dentro la schermata**: la notifica di casa e la fascia della
 * copertina sono sopra la transizione fra schermate (`ViewerActivity`), in un ramo che non
 * discende da [PickSheet]. Quindi, come [Notices] e [VeilStage], è un oggetto di processo che una
 * superficie scrive e le altre leggono. Scrivono la scheda della selezione e il FAB (voce
 * `avviso-fab`); il FAB della griglia, staccato in una finestra sua a menu aperto, lascia un
 * segnaposto della stessa misura ([TapHoldFab]), ed è quello che si misura.
 * ⚠️ **Si legge in composizione**: letto dentro `Modifier.offset { }` il valore è stato valutato
 * una volta sola sul banco, con lo zero di partenza, e la causa non è nota. Il costo è una
 * ricomposizione della notifica a ogni fotogramma mentre la scheda sale o scende.
 */
internal object FootStage {
    /*
     * Una mappa per chiedente, perché chi se ne va non tolga l'ingombro dell'altro (come
     * [VeilStage]). Vale il massimo e non la somma: le superfici sono appoggiate allo stesso bordo.
     */
    private val coprono = mutableStateMapOf<Any, Int>()

    /*
     * Un comando in un angolo occupa un fianco invece di una fascia: accanto a lui la notifica si
     * stringe invece di salire (risposta `stringe` dell'utente a `d-avviso-forma`). Due mappe e
     * non un numero col segno: il lato può cambiare (`fabSide`), e chi scrive si toglie
     * dall'altra.
     */
    private val aDestra = mutableStateMapOf<Any, Int>()
    private val aSinistra = mutableStateMapOf<Any, Int>()

    /*
     * Fin dove sale chi occupa un fianco: decide se una notifica che scende dietro la scheda
     * incontra il FAB (segnalazione dell'utente, nel cestino). Quel numero lo sa solo il FAB.
     */
    private val alti = mutableStateMapOf<Any, Int>()

    /*
     * Dov'è il centro di chi occupa un fianco: una notifica accanto a un tasto si centra sul tasto
     * (nota D dell'utente sul giro della 4.45), una che sale sopra una scheda no.
     */
    private val mezzi = mutableStateMapOf<Any, Int>()

    /** Quanti pixel di schermo copre, contati dal bordo di sotto: `0` quando non c'è nessuno. */
    val covers: Int get() = coprono.values.maxOrNull() ?: 0

    /** Quanti pixel occupa in fondo a destra, contati dal bordo destro della finestra. */
    val right: Int get() = aDestra.values.maxOrNull() ?: 0

    /** Quanti pixel occupa in fondo a sinistra, contati dal bordo sinistro della finestra. */
    val left: Int get() = aSinistra.values.maxOrNull() ?: 0

    /** Fin dove arriva in altezza chi occupa un fianco, contato dal bordo di sotto della finestra. */
    val tall: Int get() = alti.values.maxOrNull() ?: 0

    /**
     * Dov'è il centro di chi occupa un fianco, contato dal bordo di sotto della finestra: `0` se
     * nessuno. I tasti in fondo hanno un centro comune, quindi il più alto vince solo durante un
     * cambio.
     */
    val mid: Int get() = mezzi.values.maxOrNull() ?: 0

    fun cover(chi: Any, px: Int) {
        if (px <= 0) coprono.remove(chi) else coprono[chi] = px
    }

    /**
     * Dice che [chi] occupa [px] pixel del fondo dello schermo dal lato dichiarato, invece di una
     * fascia larga quanto la finestra, e che arriva fino a [tall] pixel dal bordo di sotto.
     */
    fun beside(chi: Any, px: Int, right: Boolean, tall: Int, mid: Int) {
        val qui = if (right) aDestra else aSinistra
        val altrove = if (right) aSinistra else aDestra
        altrove.remove(chi)
        if (px <= 0 || tall <= 0) {
            qui.remove(chi)
            alti.remove(chi)
            mezzi.remove(chi)
        } else {
            qui[chi] = px
            alti[chi] = tall
            mezzi[chi] = mid
        }
    }

    fun off(chi: Any) {
        coprono.remove(chi)
        aDestra.remove(chi)
        aSinistra.remove(chi)
        alti.remove(chi)
        mezzi.remove(chi)
    }
}

/**
 * Tiene una superficie appoggiata in fondo sopra quello che il fondo dello schermo già ha.
 *
 * Lo chiamano la notifica di casa e la fascia della copertina, le sole superfici che possono
 * finire sopra qualcosa: un modo solo per tutte e due.
 * ⚠️ **A scansarsi è sempre questa, non i comandi sotto** (la scheda della selezione, il FAB):
 * spostarli vorrebbe dire muoverli mentre il dito sta per toccarli.
 * ⚠️ **Si sottrae la barra di navigazione**: chi chiama se n'è già scansato, e l'ingombro dichiarato
 * conta dal bordo della finestra. La salita è uno spostamento e non un `padding`, che rimisurerebbe
 * la superficie a ogni fotogramma.
 *
 * Sopra chi occupa una fascia si sale; accanto a chi occupa un fianco ci si stringe (risposta
 * `stringe` dell'utente). Il rientro qui va bene, perché la larghezza di un comando non cambia
 * mentre lo si guarda. Il lato lo dichiara chi occupa lo spazio: leggere `fabSide` qui sarebbe un
 * secondo posto che decide dov'è il FAB.
 * Chi sale sopra il FAB non si stringe (risposta `sempre` a `d-avviso-forma-2`): durante il
 * cambio fra FAB e scheda le due dichiarazioni convivono per qualche fotogramma.
 * ⚠️ **'Sopra' vuol dire sopra l'altezza del FAB** ([FootStage.tall]), non sopra lo zero: una
 * notifica che scende con la scheda passerebbe sopra il FAB già rientrato (segnalazione
 * dell'utente, nel cestino). La barra si toglie da tutte e due le parti. Si stringe qualche pixel
 * prima del necessario, nel verso che non copre niente, e il cambio di larghezza è uno scatto.
 */
@Composable
internal fun Modifier.aboveFoot(): Modifier {
    val density = LocalDensity.current
    val barra = WindowInsets.navigationBars.getBottom(density)
    val su = (FootStage.covers - barra).coerceAtLeast(0)
    val stretta = su == 0 || su < FootStage.tall - barra
    val sinistra = with(density) { (if (stretta) FootStage.left else 0).toDp() }
    val destra = with(density) { (if (stretta) FootStage.right else 0).toDp() }
    val posata = padding(start = sinistra, end = destra).offset { IntOffset(0, -su) }
    /*
     * ⚠️ **Accanto a un tasto la notifica si centra sul tasto** (nota D dell'utente sul giro della
     * 4.45), solo a terra: sopra una scheda non c'è un tasto accanto.
     * Il centro è quello della notifica senza la barra di sistema sotto: chi chiama aggiunge la
     * barra solo in basso, quindi il centro della superficie è mezza barra sopra quello del box.
     */
    val mezzo = FootStage.mid
    if (su != 0 || mezzo <= 0 || (FootStage.left == 0 && FootStage.right == 0)) return posata
    return posata.layout { misurabile, vincoli ->
        val p = misurabile.measure(vincoli)
        layout(p.width, p.height) {
            val centro = barra + (p.height - barra) / 2
            p.place(0, centro - mezzo)
        }
    }
}

/**
 * I tasti che possono comparire in un riquadro, con il gettone con cui si salvano.
 *
 * ⚠️ **Un elenco solo per tutti i riquadri**, perché il gettone sia unico nell'archivio. Quali
 * tasti ha un riquadro lo dice il suo ordine di fabbrica in `Settings`.
 * [RENAME] nel cestino diventa 'Ripristina': cambia icona, etichetta e azione, ma è lo stesso
 * posto nel riquadro.
 */
enum class PadKey(override val token: String) : Choice {
    // Le sei azioni sui file: il riquadro del visualizzatore e dell'albero, e le prime della
    // scheda della selezione.
    COPY("copy"),
    MOVE("move"),
    SHARE("share"),
    RENAME("rename"),
    DELETE("delete"),
    INFO("info"),

    // Le quattro che vivono nella sola scheda della selezione.
    LIST("list"),
    ALL("all"),
    NONE("none"),
    INVERT("invert"),

    // La prima fila dell'editor: girare, riflettere e centrare.
    TURN_LEFT("turn-left"),
    TURN_RIGHT("turn-right"),
    CENTRE_ACROSS("centre-across"),
    CENTRE_DOWN("centre-down"),
    FLIP("flip"),

    // La seconda fila dell'editor: la cronologia e la conferma.
    ORIGINAL("original"),
    UNDO("undo"),
    REDO("redo"),
    APPLY("apply"),

    /*
     * I moduli dell'editor completo, tasti come gli altri (richiesta dell'utente): la loro fila si
     * riordina con [PadArrange].
     * ⚠️ **Nome e glifo dei moduli non sono qui**: vivono nella tabella `MODULES` di
     * `AdvancedEditorScreen.kt`, la loro fonte unica, e [label] e [glyph] non li copiano.
     */
    MOD_CROP("mod-crop"),
    MOD_GEOMETRY("mod-geometry"),
    MOD_LIGHT("mod-light"),
    MOD_COLOUR("mod-colour"),
    MOD_MIX("mod-mix"),
    MOD_TONE("mod-tone"),
    MOD_DETAIL("mod-detail"),

    /**
     * Il modulo Effetti. Una chiave nuova non si tocca più: chi ha già riordinato la fila la
     * ritrova in coda.
     */
    MOD_EFFECTS("mod-effects"),

    /** Il modulo Stili, un tasto come gli altri moduli. */
    MOD_PRESET("mod-preset"),

    /** Il modulo Disegno, ultimo nell'ordine di fabbrica (nota D dell'utente sul giro della 4.36). */
    MOD_DRAW("mod-draw"),

    /*
     * Il menu di un elemento del modulo Disegno (nota E dell'utente sul giro della 4.60, risposte
     * su `4.64-03` e `4.64-04`). Il suo ordine è dell'utente e non si salva, quindi questi tasti
     * non si riordinano: sono qui perché l'azione di un riquadro è fatta di un tasto. 'Elimina' è
     * [DELETE]; [DRAW_STYLE] è 'Copia' o 'Incolla', [DRAW_TURN] è 'Ruota' o 'Trasforma', un posto
     * solo ciascuno come [RENAME] nel cestino.
     */
    DRAW_RAISE("draw-raise"),
    DRAW_STYLE("draw-style"),
    DRAW_DUPLICATE("draw-duplicate"),
    DRAW_LOWER("draw-lower"),
    DRAW_TURN("draw-turn")
}

/**
 * Se il tasto è quello irreversibile, cioè quello col colore dell'errore.
 *
 * Serve in due posti, come [label] e [glyph]: il riquadro vero (valore di serie di
 * `PadAction.danger`) e la replica che si riordina, che lavora sulle sole chiavi.
 * ⚠️ **Irreversibile, non 'importante'**: vale per l'eliminazione e per niente che si possa
 * annullare. Una voce nuova da cui non si torna si aggiunge qui.
 */
fun PadKey.danger(): Boolean = this == PadKey.DELETE

/**
 * Come si chiama un tasto, quando lo si nomina fuori dal suo riquadro.
 *
 * È qui e non in un `when` delle impostazioni, come per `FactField`: serve in due posti (la
 * pagina che riordina e i testi che la ricerca confronta). Sono le stringhe dei tasti, quindi
 * nessuna traduzione nuova. Per [PadKey.RENAME] il nome è 'Rinomina', quello che si vede quasi
 * sempre, anche se nel cestino il tasto diventa 'Ripristina'.
 */
@StringRes
fun PadKey.label(): Int = when (this) {
    PadKey.COPY -> R.string.menu_copy_here
    PadKey.MOVE -> R.string.pick_move
    PadKey.SHARE -> R.string.menu_share
    PadKey.RENAME -> R.string.pick_rename
    PadKey.DELETE -> R.string.pick_delete
    PadKey.INFO -> R.string.pick_info
    PadKey.LIST -> R.string.pick_list
    PadKey.ALL -> R.string.pick_all_short
    PadKey.NONE -> R.string.pick_none
    PadKey.INVERT -> R.string.pick_invert
    PadKey.TURN_LEFT -> R.string.editor_left
    PadKey.TURN_RIGHT -> R.string.editor_right
    PadKey.CENTRE_ACROSS -> R.string.editor_center_across
    PadKey.CENTRE_DOWN -> R.string.editor_center_down
    PadKey.FLIP -> R.string.editor_flip
    PadKey.ORIGINAL -> R.string.editor_original
    PadKey.UNDO -> R.string.editor_undo
    PadKey.REDO -> R.string.editor_redo
    PadKey.APPLY -> R.string.editor_apply
    // I moduli dell'editor completo leggono il nome dalla tabella dei moduli, la loro fonte
    // unica.
    PadKey.MOD_CROP, PadKey.MOD_GEOMETRY, PadKey.MOD_LIGHT, PadKey.MOD_COLOUR,
    PadKey.MOD_MIX, PadKey.MOD_TONE, PadKey.MOD_DETAIL, PadKey.MOD_EFFECTS,
    PadKey.MOD_PRESET, PadKey.MOD_DRAW -> modName(this)
    PadKey.DRAW_RAISE -> R.string.draw_raise
    PadKey.DRAW_STYLE -> R.string.draw_copy
    PadKey.DRAW_DUPLICATE -> R.string.pick_duplicate
    PadKey.DRAW_LOWER -> R.string.draw_lower
    PadKey.DRAW_TURN -> R.string.draw_rotate
}

/**
 * Il disegno di un tasto, per la riga che lo fa riordinare.
 *
 * È composabile perché molti di questi glifi vivono in `res/`, e una risorsa si legge solo
 * dentro una composizione. I glifi sono gli stessi dei tasti veri, o sembrerebbero tasti diversi.
 */
@Composable
fun PadKey.glyph(): ImageVector = when (this) {
    PadKey.COPY -> Glyphs.FolderPair
    PadKey.MOVE -> Glyphs.FolderPairDashed
    PadKey.SHARE -> Glyphs.Material.Share
    PadKey.RENAME -> Glyphs.TextCursor
    PadKey.DELETE -> Glyphs.PickDelete
    PadKey.INFO -> Glyphs.Material.InfoOutlined
    PadKey.LIST -> Glyphs.Material.FormatListBulletedOutlined
    PadKey.ALL -> Glyphs.PickAll
    PadKey.NONE -> Glyphs.PickNone
    PadKey.INVERT -> Glyphs.PickInvert
    // Le due frecce disegnate dall'utente, come nell'editor: il verso di un giro è fisico, e
    // non si specchiano (vedi `Glyphs`).
    PadKey.TURN_LEFT -> Glyphs.TurnLeft
    PadKey.TURN_RIGHT -> Glyphs.TurnRight
    PadKey.CENTRE_ACROSS -> Glyphs.AlignAcross
    PadKey.CENTRE_DOWN -> Glyphs.AlignDown
    PadKey.FLIP -> Glyphs.Material.Flip
    PadKey.ORIGINAL -> Glyphs.EditReset
    PadKey.UNDO -> Glyphs.EditUndo
    PadKey.REDO -> Glyphs.EditRedo
    PadKey.APPLY -> Glyphs.EditApply
    // Come il nome: il glifo di un modulo vive nella sua tabella.
    PadKey.MOD_CROP, PadKey.MOD_GEOMETRY, PadKey.MOD_LIGHT, PadKey.MOD_COLOUR,
    PadKey.MOD_MIX, PadKey.MOD_TONE, PadKey.MOD_DETAIL, PadKey.MOD_EFFECTS,
    PadKey.MOD_PRESET, PadKey.MOD_DRAW -> modGlyph(this)
    PadKey.DRAW_RAISE -> Glyphs.Material.FlipToFront
    PadKey.DRAW_STYLE -> Glyphs.Material.FormatPaint
    PadKey.DRAW_DUPLICATE -> Glyphs.Material.ControlPointDuplicate
    PadKey.DRAW_LOWER -> Glyphs.Material.FlipToBack
    PadKey.DRAW_TURN -> Glyphs.TurnRight
}

/**
 * Rimette una lista di azioni nell'ordine che l'utente ha scelto.
 *
 * ⚠️ **Un tasto che l'ordine non nomina finisce in coda, non sparisce.** È una rete: l'ordine
 * arriva da [padOrderOf], che aggiunge i gettoni mancanti al loro posto di fabbrica. Un tasto
 * senza posto qui è un riquadro con un tasto non suo, e la coda lo rende visibile.
 * Si ordinano le azioni vere, con le loro chiusure, e non i gettoni.
 */
fun List<PadAction>.inOrder(order: List<PadKey>): List<PadAction> {
    if (order.isEmpty()) return this
    val posto = order.withIndex().associate { (i, k) -> k to i }
    // `sortedBy` è stabile: fra due tasti senza posto resta l'ordine di partenza.
    return sortedBy { posto[it.key] ?: Int.MAX_VALUE }
}

/**
 * Un'azione del riquadro: l'icona, la parola, e se è quella da cui non si torna.
 *
 * [onHold] è `null` per quasi tutte: il tocco lungo è una scorciatoia in più, e una su ogni
 * tasto sarebbe un gesto nascosto da imparare per ciascuno.
 */
class PadAction(
    /**
     * Chi è questo tasto, per l'archivio: l'ordine salvato deve sapere quale tasto è in quale
     * posto, anche quando nel cestino 'Rinomina' diventa 'Ripristina'. Il gettone non è il nome
     * della costante, come per ogni [Choice].
     */
    val key: PadKey,
    val icon: ImageVector,
    @StringRes val label: Int,
    /**
     * Se è quella da cui non si torna. Si ricava dalla chiave ([PadKey.danger]), quindi un
     * riquadro nuovo ha il colore per costruzione; un chiamante con una ragione per spegnerlo lo
     * passa, e scrive la ragione accanto.
     */
    val danger: Boolean = key.danger(),
    /**
     * Se il tasto si può premere adesso.
     *
     * ⚠️ **Spento e non nascosto**: una fila che perde e riacquista tasti li sposta sotto le dita.
     * Spento, il posto resta suo.
     */
    val enabled: Boolean = true,
    /**
     * Che cosa fa il tocco lungo, e `null` quando non fa niente. Va insieme a [holdLabel], che il
     * lettore di schermo annuncia.
     */
    val onHold: (() -> Unit)? = null,
    @StringRes val holdLabel: Int? = null,
    val onClick: () -> Unit
)

/**
 * Un tasto del riquadro.
 *
 * ⚠️ **Con la parola, l'icona non ha descrizione**: `clickable` fonde le semantiche dei figli, e
 * TalkBack la leggerebbe due volte. Senza la parola la descrizione passa all'icona, o il tasto
 * sarebbe muto.
 * Il tocco è sull'intera colonna: un bersaglio di 24dp si manca.
 * ⚠️ **L'altezza minima si dichiara** ([PAD_TAP]): senza la parola la cella sarebbe alta 40dp,
 * sotto i 48dp che Material chiede a un bersaglio.
 */
@Composable
private fun PadButton(
    action: PadAction,
    modifier: Modifier = Modifier,
    labels: Boolean = LocalPadLook.current.labels
) {
    val haptics = LocalHapticFeedback.current
    val hold = remember(action.onHold, haptics) {
        action.onHold?.let { premuto ->
            {
                haptics.performHapticFeedback(HOLD_BUZZ)
                premuto()
            }
        }
    }
    val full =
        if (action.danger) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurface
    // Lo spento è il colore di sempre a [OFF_INK], il valore di Material per un comando
    // inattivo: un grigio scritto a mano andrebbe bene in un tema solo.
    val tint = if (action.enabled) full else full.copy(alpha = OFF_INK)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(PAD_CORNER))
            // `combinedClickable` sempre: con `onLongClick` a null si comporta come `clickable`.
            .combinedClickable(
                enabled = action.enabled,
                // Il ruolo si dichiara, o un lettore di schermo leggerebbe un contenitore con un
                // testo dentro.
                role = Role.Button,
                onLongClickLabel = action.holdLabel?.let { stringResource(it) },
                // Qui il gesto può mancare, quindi la vibrazione si compone a mano (vedi
                // [withHaptics]).
                onLongClick = hold,
                hapticFeedbackEnabled = false,
                onClick = action.onClick
            )
            .heightIn(min = PAD_TAP)
            .padding(vertical = PAD_GAP),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PAD_LABEL_GAP, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = if (labels) null else stringResource(action.label),
            tint = tint,
            modifier = Modifier.size(PAD_ICON)
        )
        if (!labels) return@Column
        Text(
            text = stringResource(action.label),
            style = padLabel(),
            color = tint,
            textAlign = TextAlign.Center,
            // Due righe: in molte lingue la parola è più lunga che in italiano. Una parola che va
            // a capo allunga la sua colonna senza spostare le icone.
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Il vestito dell'etichetta di un tasto, per il modello e per la sua replica.
 *
 * ⚠️ **10sp, un gradino sotto `labelSmall`, per una misura** (richiesta dell'utente): nella fila
 * da cinque dell'editor la cella è 76dp ([PAD_CELL]), e 'orizzontale' a 11sp ne chiede 77 sul suo
 * telefono. A 10,5 il margine sarebbe di tre punti, a 10 è di sei.
 * ⚠️ **Il banco non lo vede**: con Robolectric il carattere è più stretto, e la stessa parola entra
 * in 64dp (`Rules.md` § '🧪 Quando si scrive una prova, e quando no').
 * Vale per tutte le file, perché il tasto è uno solo ([PadButton]).
 * L'interlinea è [PAD_LABEL_LINE], 12sp: la 16sp di `labelSmall` lascerebbe troppa aria fra le
 * due righe di un'etichetta che va a capo. La spaziatura va a zero, che su 'orizzontale' recupera
 * 5,5 punti senza rimpicciolire il corpo (punto D dell'utente sul giro della 2.77).
 */
@Composable
private fun padLabel(): TextStyle =
    MaterialTheme.typography.labelSmall.copy(
        fontSize = PAD_LABEL_SIZE,
        lineHeight = PAD_LABEL_LINE,
        letterSpacing = 0.sp
    )

/**
 * Lo stesso gesto, con la vibrazione del tocco lungo davanti.
 *
 * Ogni pressione lunga vibra, per richiesta dell'utente.
 * ⚠️ **Ogni `combinedClickable` spegne la propria vibrazione** (`hapticFeedbackEnabled = false`):
 * Compose Foundation vibra da sé con `LongPress`, e con questa funzione sarebbero due colpi per
 * un gesto. Una vibrazione sola, sempre da [HOLD_BUZZ]: lo presidia `VibrazioneTest`, che legge
 * anche i sorgenti.
 * Una funzione sola, così un tocco lungo nuovo la prende passando di qui. Prende un gesto che
 * c'è: dove il tocco lungo è opzionale ([PadButton]) la vibrazione si scrive sul posto.
 */
@Composable
fun withHaptics(action: () -> Unit): () -> Unit {
    val haptics = LocalHapticFeedback.current
    return remember(action, haptics) {
        {
            haptics.performHapticFeedback(HOLD_BUZZ)
            action()
        }
    }
}

/**
 * La vibrazione di **ogni** pressione lunga dell'app, scritta in un posto solo.
 *
 * È `LongPress`, la vibrazione piena di Android, ovunque (scelta B1 dell'utente, 5.92).
 * ⚠️ **Mai `SegmentTick` o `ToggleOn`**: esistono da Android 14, e Compose passa il numero grezzo
 * a `performHapticFeedback` senza ripiego; sotto Android 14 il telefono non vibrerebbe, e il
 * `minSdk` è 28.
 * Entrare nella selezione e muoversi dentro vibrano uguale.
 */
val HOLD_BUZZ = HapticFeedbackType.LongPress


/**
 * Quante colonne ha il riquadro: tre, come le ha chieste l'utente. Non è privata perché la legge
 * anche la pagina che riordina i tasti, come [SHEET_COLUMNS].
 */
internal const val PAD_COLUMNS = 3

/**
 * Quanto è largo un menu fatto di un riquadro di sei azioni: il menu del tocco lungo nel
 * visualizzatore e quello di un elemento del modulo Disegno (risposta dell'utente su `4.64-03`).
 */
internal val PAD_MENU_WIDTH = 252.dp

/**
 * Quanto è alta una cella della replica che si riordina.
 *
 * Fissa, a differenza del riquadro vero: il passo di riga deve essere un numero per il conto del
 * posto d'arrivo. 72dp contengono un glifo da 24 e due righe di parola.
 */
private val ARRANGE_HIGH = 72.dp

/** Quanto si vede il fondo tondo sotto un tasto fermo. */
private const val ARRANGE_BED = 0.10f

/** Quanto si vede quando il tasto è in mano: la stessa cosa, più evidente. */
private const val ARRANGE_HELD = 0.24f


/**
 * La larghezza di una cella.
 *
 * Tre celle più i distacchi fanno poco meno di 250dp, che entrano in uno schermo da 360dp col
 * margine del menu: è questo vincolo a decidere il numero.
 */
private val PAD_CELL = 76.dp

/** Il lato dell'icona: quella di un tasto, non quella di una barra. */
private val PAD_ICON = 24.dp

/** Il distacco fra le celle, e il respiro dentro ognuna. */
private val PAD_GAP = 8.dp

/** Quanto stacca la parola dalla sua icona: poco, perché sono la stessa cosa. */
private val PAD_LABEL_GAP = 2.dp

/** Il corpo dell'etichetta di un tasto. Il perché di questo numero vive su [padLabel]. */
private val PAD_LABEL_SIZE = 10.sp

/** L'interlinea di un'etichetta che va a capo, poco più del corpo: il perché è su [padLabel]. */
private val PAD_LABEL_LINE = 12.sp

/**
 * Il margine laterale del riquadro dentro il menu che lo contiene.
 *
 * Nei menu è il pavimento del fianco sinistro, che decide la colonna delle icone della lista
 * (vedi [PadBody]); a destra e nelle schede è il margine.
 */
private val PAD_EDGE = 4.dp

/**
 * Il rimbalzo del FAB al tocco: di quanto si stringe, e in quanto tempo scende e risale.
 *
 * Il FAB non ha ombra, sfocatura né tinte che si spostano (istruzione dell'utente): un rientro
 * di pochi punti su una piastrella isolata non si vede, perché non c'è un riferimento; un
 * movimento, un simbolo e un colore che cambiano si vedono da soli.
 * Il 30% e i due tempi sono dell'utente, con ease-in/ease-out. È una scala sul tasto intero,
 * glifo compreso: costa un livello grafico e nessuna rimisurazione.
 */
private const val RIMBALZO = 0.30f
private const val RIMBALZO_GIU_MS = 30
private const val RIMBALZO_SU_MS = 60

/**
 * In quanto tempo il glifo dell'app diventa una ×, e in quanto il fondo cambia accento.
 *
 * I 90 ms del simbolo sono la somma dei due tempi del rimbalzo (richiesta dell'utente): il
 * simbolo finisce di cambiare quando il tasto torna alla sua misura. Il colore è più corto,
 * quasi istantaneo, anche questo per sua richiesta: si legge come lo stato nuovo del tasto. I
 * due numeri sono indipendenti, come li ha dati lui.
 */
private const val MORPH_MS = RIMBALZO_GIU_MS + RIMBALZO_SU_MS
private const val TINTA_MS = 55

/**
 * In quanto tempo il FAB si rimpicciolisce fino a sparire, andando dove non c'è.
 *
 * ⚠️ **Il numero viene dalla dissolvenza di schermata**: nei suoi 180 ms l'opacità della
 * schermata che se ne va vale 0,54 a 60 ms, 0,30 a 80 e 0,16 a 100, quindi dopo un decimo di
 * secondo non si vede quasi niente. Con 100 ms e una curva che parte veloce, a 40 ms il tasto è
 * già sotto la metà con la schermata ancora al 72%. Allungarlo non servirebbe.
 */
private const val VIA_MS = 100

/**
 * Il lato della × che prende il posto del glifo, e quanto zoom fanno i due simboli.
 *
 * La × è più piccola del glifo dell'app, come ha chiesto l'utente, un gradino sotto [PAD_ICON].
 * È la croce di Material, [Glyphs.Material.Close] (`Rules.md` § '🖌️ Come entra un disegno').
 * Lo zoom è una frazione e non una misura: il glifo dell'app può non essere quadrato (il
 * marchio), e una misura lo schiaccerebbe. Chi entra parte piccolo e chi esce finisce piccolo,
 * quindi a metà strada i due simboli hanno la stessa misura.
 */
private val CHIUDI_LATO = 20.dp
private const val GLIFO_ZOOM = 0.35f

/** Lo smusso dell'alone del tocco su una cella. */
private val PAD_CORNER = 10.dp

/**
 * La larghezza di una cella quando le parole sono spente.
 *
 * Viene dal bersaglio e non dal glifo: [PAD_TAP] è il pavimento in altezza, e una cella più
 * stretta sarebbe difficile da centrare. È il rapporto di un `IconButton` di Material.
 */
private val PAD_CELL_BARE = 48.dp

/**
 * L'altezza minima di una cella, con o senza parola.
 *
 * 48dp è il bersaglio minimo di Material: senza la parola la cella sarebbe alta 40dp. Il
 * riquadro compatto lo è di disegno, non di area toccabile.
 */
private val PAD_TAP = 48.dp

/**
 * Come si presentano i riquadri di questa app: le parole, gli ordini dei tasti e il lato del FAB.
 *
 * ⚠️ **È in un `CompositionLocal`, come `LocalAivDepth`**: lo chiedono superfici che vivono in
 * finestre, che le impostazioni non ricevono, e la catena di parametri attraverserebbe cinque
 * schermate. Il valore di fabbrica vale per una finestra nata fuori dall'albero della
 * composizione.
 */
class PadLook(
    val labels: Boolean = true,
    /** Il riquadro delle sei azioni: visualizzatore e albero. */
    val menu: List<PadKey> = MENU_KEYS,
    /** La scheda della selezione, dieci azioni. */
    val pick: List<PadKey> = PICK_KEYS,
    /** La prima fila dell'editor: girare e centrare. */
    val turn: List<PadKey> = TURN_KEYS,
    /** La seconda fila dell'editor: la cronologia e la conferma. */
    val step: List<PadKey> = STEP_KEYS,
    /** La fila dei moduli dell'editor completo, che vive in una scheda come le file sopra. */
    val mods: List<PadKey> = MOD_KEYS,
    /**
     * Da che parte dello schermo è il FAB. È qui per la stessa ragione degli ordini, e insieme
     * all'aspetto dei riquadri perché il FAB apre il riquadro.
     */
    val hand: Hand = Hand.RIGHT
)

/** Quello che i riquadri leggono, messo in scena accanto al tema. */
val LocalPadLook = compositionLocalOf { PadLook() }

/**
 * In quale angolo in basso è il FAB, secondo l'impostazione; con lui si sposta tutto quello che
 * gli gira intorno. La chiave nell'archivio è quella della vecchia scelta della mano.
 * ⚠️ **`End` e `Start`, non destra e sinistra**: nelle lingue da destra a sinistra l'interfaccia
 * si specchia, e il FAB la segue.
 */
@Composable
fun fabSide(): Alignment =
    if (LocalPadLook.current.hand == Hand.RIGHT) Alignment.BottomEnd else Alignment.BottomStart

/**
 * Lo stesso lato, per chi allinea una fila invece di posare un tasto in un angolo.
 *
 * Serve alle pastiglie dell'intestazione (richiesta dell'utente): la riga che va a capo si
 * allinea dalla parte del FAB, sotto il pollice. Legge lo stesso valore di [fabSide].
 */
@Composable
fun fabEdge(): Alignment.Horizontal =
    if (LocalPadLook.current.hand == Hand.RIGHT) Alignment.End else Alignment.Start

/**
 * Quante colonne ha la scheda della selezione: cinque, come chieste. Le azioni sono dieci, e a
 * tre colonne il pannello coprirebbe mezzo schermo. Non è privata perché la legge anche la pagina
 * che riordina i tasti (`SettingsScreen`).
 */
internal const val SHEET_COLUMNS = 5


/**
 * Lo smusso del FAB quadrato, uguale in tutte le schermate.
 *
 * Quadrato ma non tagliente: il tondo pieno direbbe 'azione principale', e qui l'azione
 * principale sono le immagini. È qui perché la usano tutti i FAB.
 */
val FAB_CORNER = 12.dp

/**
 * La misura di `SmallFloatingActionButton`, che [TapHoldFab] rifà a mano.
 *
 * È anche l'altezza che [FAB_REACH] somma al margine.
 */
val FAB_SIZE = 40.dp


/**
 * Il margine del FAB quadrato dalle due sponde della schermata delle cartelle.
 *
 * È qui perché lo somma [FAB_REACH].
 */
val HUB_PAD = 16.dp

/** Il glifo del FAB col vetro, nel tema chiaro: il `#ecfff7` dell'utente. */
internal val FAB_GLASS_INK_LIGHT = Color(0xFFECFFF7)

/** Il glifo del FAB col vetro, nel tema scuro: il `#004247` dell'utente. */
internal val FAB_GLASS_INK_DARK = Color(0xFF004247)

/**
 * Quanto arriva in su il FAB quadrato delle cartelle, misurato dal fondo dello schermo:
 * il suo margine ([HUB_PAD]) più la sua altezza.
 *
 * ⚠️ **È l'altezza da cui parte la sfumatura in fondo allo schermo** (richiesta dell'utente):
 * comincia dove comincia il FAB, non a [BELOW_FAB], che ha una ventina di dp in più d'aria.
 * L'altezza è [FAB_SIZE], la misura che Material dà a un FAB piccolo.
 */
val FAB_REACH = HUB_PAD + FAB_SIZE

/**
 * Quanto spazio resta sotto l'ultimo elemento di una griglia, perché il FAB non lo copra: [FAB_REACH]
 * più aria.
 */
val BELOW_FAB = FAB_REACH + 20.dp

/**
 * Se questa schermata sta uscendo verso una senza FAB: il FAB si rimpicciolisce fino a sparire
 * (richiesta dell'utente).
 *
 * Lo fornisce `AivApp`, dentro la transizione fra schermate, l'unico posto che sa dove si va. Lo
 * legge [TapHoldFab], così un FAB nuovo prende l'uscita per costruzione. Fuori dalla transizione
 * vale `false`.
 */
internal val LocalSenzaFab = compositionLocalOf { false }

/**
 * Un FAB con **due** gesti: tocco breve e tocco lungo.
 *
 * ⚠️ **Non è `SmallFloatingActionButton`**: quello prende un `onClick` solo, e il `modifier`
 * finisce fuori dal suo `clickable`; un `combinedClickable` là non vedrebbe mai il tocco lungo,
 * perché il figlio consuma il down per primo. Per due gesti il nodo che ascolta dev'essere uno.
 * La misura è quella di Material ([FAB_SIZE]); senza ombra (vedi [RIMBALZO]), e i colori li
 * passano i chiamanti.
 * Il gesto è dentro la `Surface`, così l'increspatura prende il colore del contenuto ([ink]).
 * È condiviso da tutti i FAB col tocco lungo: cambiano solo il glifo e quello che fanno i gesti.
 */
@Composable
fun TapHoldFab(
    /** Che cos'è il FAB, per il lettore di schermo: la sua azione breve. */
    label: String,
    container: Color,
    ink: Color,
    /**
     * Che cosa fa il tocco lungo, per il lettore di schermo, che lo annuncia fra le azioni del
     * FAB. Arriva da fuori perché il gesto cambia con la schermata.
     */
    holdLabel: String,
    /**
     * Il FAB si stacca in una finestra sua, per restare sopra la Sfocatura dietro i pannelli del
     * suo menu (richiesta dell'utente).
     *
     * La sfocatura di finestra (`WindowVeil`) è dietro la finestra che la chiede, e tutto quello
     * che è più in basso ci finisce sotto: l'unica via è una finestra più in alto. Vale solo per il
     * menu che il FAB apre: un dialogo di Material è sempre sopra i menu, e con lui il FAB resta
     * sfocato, come si conviene a un modale.
     * ⚠️ **Il menu va composto prima del FAB**: fra finestre dello stesso tipo l'ordine è quello
     * in cui sono aggiunte.
     * ⚠️ **Staccato, il FAB è solo da guardare**: la sua finestra lascia passare le dita (vedi
     * [untouchable]), e così il tocco fuori chiude il menu.
     */
    lifted: Boolean = false,
    /**
     * Se il FAB si mostra premuto: rimbalzo fatto, × al posto del glifo e accento dell'altro tema.
     *
     * ⚠️ **È un segnale a sé e non [lifted]**: [lifted] resta vero per tutta la discesa del menu
     * (`MenuState.visible`), e il FAB tornerebbe solo a menu sparito, in un secondo tempo
     * (riscontro dell'utente). Chi apre un menu passa `wanted`, che cade quando si chiede la
     * chiusura. Il valore di serie è [lifted], che per un FAB senza menu coincide.
     */
    pressed: Boolean = lifted,
    /**
     * Se il FAB è il tasto tondo della pillola ([PhonePill.SLIDE]) invece del quadrato. Lo chiede
     * solo la copia che un velo d'aiuto illumina, che deve avere la forma del tasto vero.
     */
    round: Boolean = false,
    /**
     * Quello che il vetro sfoca, o `null` dove non c'è niente da sfocare. Vedi [Backdrop].
     * Staccato, il FAB è sopra la sfocatura del suo menu, e resta la tinta.
     */
    backdrop: Backdrop? = null,
    onTap: () -> Unit,
    onHold: () -> Unit,
    /**
     * Che cosa si vede sul FAB, con la descrizione per il lettore di schermo. È una fessura e non
     * un `ImageVector` perché il FAB della schermata iniziale ha il marchio dell'app, più largo
     * che alto (vedi `Marchio` in `FolderScreen.kt`).
     */
    glyph: @Composable (descrizione: String?) -> Unit
) {
    /*
     * Al tocco il FAB rimbalza, il glifo diventa una × (che dice che ora il tasto chiude il menu)
     * e il fondo prende l'accento dell'altro tema (istruzione dell'utente; vedi [RIMBALZO]).
     * Rimbalzo e simboli sono in `graphicsLayer`, un ridisegno per fotogramma; il colore va a
     * `Surface` in composizione, per [TINTA_MS], per avere l'increspatura e il `contentColor`.
     * ⚠️ **Il primo giro non anima**: `LaunchedEffect` parte anche alla prima composizione, e il
     * FAB rimbalzerebbe a ogni ingresso della schermata.
     */
    val morph = remember { Animatable(if (pressed) 1f else 0f) }
    val tinta = remember { Animatable(if (pressed) 1f else 0f) }
    val rimbalzo = remember { Animatable(1f) }
    var primo by remember { mutableStateOf(true) }
    /*
     * L'uscita verso una schermata senza FAB rimpicciolisce tutto, glifo compreso (la prima delle
     * due varianti descritte dall'utente, che ha lasciato la scelta): una maschera taglierebbe il
     * glifo. Si moltiplica al rimbalzo, che può essere ancora in corso. È più corta della
     * dissolvenza di schermata (vedi [VIA_MS]).
     * Non c'è un ritorno da animare: la schermata che rientra compone un FAB nuovo. Lo `snapTo`
     * copre una navigazione annullata.
     */
    val via = remember { Animatable(1f) }
    val senzaFab = LocalSenzaFab.current
    LaunchedEffect(senzaFab) {
        if (senzaFab) via.animateTo(0f, tween(VIA_MS, easing = FastOutLinearInEasing))
        else via.snapTo(1f)
    }
    LaunchedEffect(pressed) {
        val a = if (pressed) 1f else 0f
        if (primo) {
            primo = false
            morph.snapTo(a)
            tinta.snapTo(a)
            return@LaunchedEffect
        }
        launch { morph.animateTo(a, tween(MORPH_MS, easing = FastOutSlowInEasing)) }
        launch { tinta.animateTo(a, tween(TINTA_MS, easing = FastOutSlowInEasing)) }
        rimbalzo.animateTo(1f - RIMBALZO, tween(RIMBALZO_GIU_MS, easing = FastOutSlowInEasing))
        rimbalzo.animateTo(1f, tween(RIMBALZO_SU_MS, easing = FastOutSlowInEasing))
    }
    /*
     * A riposo il FAB ha l'accento dell'altro tema, e da premuto torna a quello del tema in
     * vigore (riscontro dell'utente: più stacco). Il colore a riposo lo passa il chiamante
     * ([container]), perché le copie di un velo d'aiuto sono arancioni; qui si decide il secondo
     * estremo.
     * ⚠️ **Cambia anche l'inchiostro** ([aivOnAccent]): quello del tema chiaro sull'accento scuro
     * misura 2,05.
     */
    val light = LocalAivLight.current
    val fondo = lerp(container, aivAccent(light), tinta.value)
    /*
     * Col vetro il glifo ha un colore fisso, anche da premuto (nota F dell'utente sul giro della
     * 4.25): sul vetro l'inchiostro dell'accento non regge il contrasto. Solo per il FAB vero,
     * quello con uno sfondo da sfocare ([backdrop]).
     */
    val glassInk = backdrop != null && LocalPillLook.current.fill == PillFill.GLASS
    // Un colore del vetro scelto dall'utente sceglie il proprio inchiostro (risposta A1): vedi
    // [ownGlassInk].
    val segno = if (glassInk) ownGlassInk() ?: (if (light) FAB_GLASS_INK_LIGHT else FAB_GLASS_INK_DARK)
    else lerp(ink, aivOnAccent(light), tinta.value)
    /*
     * I due simboli sono uno sopra l'altro in una scatola sola, misurata sul più grande: il tasto
     * non si rimisura durante la dissolvenza.
     */
    val simboli = @Composable { descrizione: String? ->
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.graphicsLayer {
                    val m = morph.value
                    alpha = 1f - m
                    val s = 1f - GLIFO_ZOOM * m
                    scaleX = s
                    scaleY = s
                }
            ) { CompositionLocalProvider(LocalRoundKey provides round) { glyph(descrizione) } }
            Box(
                modifier = Modifier.graphicsLayer {
                    val m = morph.value
                    alpha = m
                    val s = 1f - GLIFO_ZOOM * (1f - m)
                    scaleX = s
                    scaleY = s
                }
            ) {
                Icon(
                    imageVector = Glyphs.Material.Close,
                    // Muta: a parlare è il glifo dell'app.
                    contentDescription = null,
                    modifier = Modifier.size(CHIUDI_LATO)
                )
            }
        }
    }
    /* Il rimbalzo è sul nodo che contiene fondo e simboli: rimbalza il tasto intero. */
    val lato = if (round) PILL_KEY else FAB_SIZE
    val forma = if (round) CircleShape else RoundedCornerShape(FAB_CORNER)
    val tasto = @Composable { muto: Boolean ->
        Surface(
            modifier = Modifier
                .size(lato)
                .graphicsLayer {
                    val k = rimbalzo.value * via.value
                    scaleX = k
                    scaleY = k
                }
                /*
                 * Il fondo lo dipinge `buttonFill`: 'Aspetto dei pulsanti principali' vale anche
                 * per il FAB (nota E dell'utente sul giro della 4.04). La `Surface` resta per il
                 * colore del contenuto e la forma dell'increspatura, con fondo trasparente.
                 */
                .buttonFill(if (lifted) null else backdrop, fondo, forma)
                // Staccato, il FAB non prende i tocchi ([untouchable]): il lettore di schermo non
                // deve annunciarlo.
                .then(if (muto) Modifier.clearAndSetSemantics { } else Modifier),
            shape = forma,
            color = Color.Transparent,
            contentColor = segno,
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier.combinedClickable(
                    role = Role.Button,
                    onLongClickLabel = holdLabel,
                    onLongClick = withHaptics(onHold),
                    hapticFeedbackEnabled = false,
                    onClick = onTap
                ),
                contentAlignment = Alignment.Center
            ) {
                simboli(label)
            }
        }
    }

    /*
     * ⚠️ **Il FAB dichiara quanto occupa del fondo dello schermo** ([FootStage]), così la notifica
     * non gli finisce sopra (vedi [Modifier.aboveFoot]): un fianco, la sua altezza e il suo centro.
     * Il lato si misura dalla posizione nella finestra, non da `fabSide`, per non avere due posti
     * che decidono dov'è il FAB. È qui, così ogni FAB lo dichiara per costruzione, con una chiave
     * per FAB: durante la dissolvenza fra due schermate sono due.
     */
    val misura = Modifier.declaresFoot()

    if (!lifted) {
        Box(modifier = misura) { tasto(false) }
        return
    }
    /*
     * ⚠️ **Il posto resta occupato da una scatola della stessa misura**: una finestra non occupa
     * spazio nel genitore, e senza la scatola il menu, ancorato al FAB, salterebbe altrove.
     * `Alignment.TopStart` mette la finestra dove il FAB sarebbe stato.
     *
     * ⚠️ **Il sosia c'è solo finché la finestra non disegna**: a menu aperto deve esistere solo il
     * FAB sopra la sfocatura (istruzione dell'utente). Ma non si può togliere del tutto: togliere
     * il nodo e aggiungere la finestra non avviene nello stesso fotogramma, e il FAB lampeggerebbe.
     * Si aspetta un fotogramma con `withFrameNanos`: un effetto dentro il `Popup` scatta prima
     * che la finestra abbia disegnato.
     * ⚠️ **Il sosia non è una `Surface`**, che si prende i tocchi anche senza `onClick`: qui i
     * tocchi devono passare alla schermata, che chiude il menu (come [untouchable] dall'altra
     * parte).
     * ⚠️ **Il sosia si centra con `align`, non col `contentAlignment` della scatola**: il `Popup`
     * si ancora a dove atterra il suo nodo di misura zero, e un `contentAlignment` lo sposterebbe.
     */
    var suo by remember { mutableStateOf(false) }
    Box(modifier = Modifier.size(lato)) {
        if (!suo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(lato)
                    .graphicsLayer {
                        val k = rimbalzo.value
                        scaleX = k
                        scaleY = k
                    }
                    .background(fondo, forma),
                contentAlignment = Alignment.Center
            ) {
                /*
                 * La tinta si dà col `CompositionLocal`, come fa la `Surface` del FAB vero: la
                 * fessura disegna un `Icon` che il colore lo legge da lì.
                 */
                CompositionLocalProvider(LocalContentColor provides segno) { simboli(null) }
            }
        }
        Popup(alignment = Alignment.TopStart) {
            untouchable()
            LaunchedEffect(Unit) {
                withFrameNanos { }
                suo = true
            }
            DisposableEffect(Unit) { onDispose { suo = false } }
            tasto(true)
        }
    }
}

/**
 * Dichiara a [FootStage] quanto del fondo dello schermo occupa il nodo: il fianco e l'altezza.
 *
 * La usano tutti i tasti in fondo, il FAB e quelli della pillola: la notifica li scansa con lo
 * stesso conto. Il perché è nella nota dentro [TapHoldFab].
 */
@Composable
fun Modifier.declaresFoot(): Modifier {
    val quota = remember { Any() }
    val finestra = LocalWindowInfo.current.containerSize
    DisposableEffect(quota) { onDispose { FootStage.off(quota) } }
    return this.onGloballyPositioned {
        val dove = it.positionInWindow()
        val da = dove.x.roundToInt()
        val fino = da + it.size.width
        val destra = da + fino > finestra.width
        FootStage.beside(
            quota,
            if (destra) finestra.width - da else fino,
            destra,
            tall = finestra.height - dove.y.roundToInt(),
            // ⚠️ Dal fondo della radice della composizione, non della finestra: la notifica è
            // posata in fondo alla radice, e le due differiscono dove la radice non riempie la
            // finestra (sul banco di 8 dp, misurato).
            mid = it.findRootCoordinates().size.height -
                (it.positionInRoot().y + it.size.height / 2f).roundToInt()
        )
    }
}

/**
 * Rende la finestra che ospita questa vista trasparente al tocco: le dita arrivano a quello che
 * è sotto.
 *
 * ⚠️ **Senza, il FAB staccato impedirebbe la chiusura del menu**: un tocco fuori dal menu, FAB
 * compreso, deve chiuderlo (richiesta dell'utente), e lo raccoglie la schermata nella finestra
 * dell'app. Un FAB in una finestra più alta lo prenderebbe per primo, e il suo `onTap` riaprirebbe
 * il menu. Staccato, il FAB è solo da guardare.
 * Si passa dai `LayoutParams` della radice: un `Popup` non ha un `Window` suo (vedi `Veil.kt`).
 * All'uscita non c'è niente da rimettere: la finestra si chiude col popup.
 */
@Composable
private fun untouchable() {
    val view = LocalView.current
    DisposableEffect(view) {
        val root = view.rootView
        val params = root.layoutParams as? WindowManager.LayoutParams
        val manager = view.context.getSystemService(WindowManager::class.java)
        if (params != null && manager != null) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            runCatching { manager.updateViewLayout(root, params) }
        }
        onDispose { }
    }
}

/**
 * Il marchio dell'app sul FAB, centrato otticamente (richiesta dell'utente).
 *
 * In orizzontale è la A a essere al centro (nota C dell'utente sul giro della 4.25), e la A occupa
 * tutta la larghezza della tela. In verticale vale il baricentro dell'inchiostro, misurato
 * rendendo il disegno a 700 x 600: cade al 9,4% dell'altezza sotto il centro, perché la A ha la
 * massa in basso ([MARK_DY]). Se `ic_aiv_mark.xml` cambia, la misura si rifà.
 * Si dà la larghezza e l'altezza segue: il glifo è 70 x 60.
 */
@Composable
internal fun Marchio(descrizione: String?) {
    val tondo = LocalRoundKey.current
    val largo = if (tondo) MARK_WIDE_ROUND else MARK_WIDE
    val alto = largo * 60f / 70f
    Icon(
        imageVector = Glyphs.AivMark,
        contentDescription = descrizione,
        modifier = Modifier
            .offset(
                // In orizzontale non si sposta: la A, che è al centro, occupa tutta la tela.
                x = 0.dp,
                y = if (tondo) alto * MARK_DY_ROUND - MARK_LIFT_ROUND else alto * MARK_DY
            )
            .size(width = largo, height = alto)
            // Il banco trova il marchio da qui, ovunque sia disegnato (`PillolaTest`).
            .testTag(MARK_TAG)
    )
}

/** Il tag di prova di [Marchio]. */
internal const val MARK_TAG = "aiv-mark"

/**
 * Se il glifo è disegnato nel tasto tondo della pillola a scomparsa ([PhonePill.SLIDE]): lo
 * forniscono i due posti che disegnano quel tondo, [TapHoldFab] (la copia dell'onboarding) e
 * `SlidePill` (il tasto vero).
 */
internal val LocalRoundKey = compositionLocalOf { false }

/** Quanto è largo il marchio nel tasto tondo: 22dp, l'8% in meno (richiesta dell'utente). */
private val MARK_WIDE_ROUND = 22.dp

/**
 * Lo spostamento del marchio nel tasto tondo, dove è centrata la A sui due assi (richiesta
 * dell'utente). Il riquadro della A in `ic_aiv_mark.xml` va da 6 a 60 in altezza: il suo centro
 * è il 5% sotto quello della tela.
 */
private const val MARK_DY_ROUND = -0.050f

/**
 * Quanto il marchio sale ancora nel tondo: una correzione ottica dell'utente, perché la A ha la
 * massa in basso.
 */
private val MARK_LIFT_ROUND = 1.5.dp

/**
 * Quanto è largo il marchio sul FAB: 24dp, la misura standard di un glifo di Material su un FAB
 * da 40. Altezza e spostamento lo seguono.
 */
private val MARK_WIDE = 24.dp

/** Lo spostamento verticale del marchio sul FAB quadrato: vedi [Marchio]. */
private const val MARK_DY = -0.094f

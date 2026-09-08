package io.github.roccobot.aiv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

/*
 * ⚠️⚠️ **I DUE TASTI SONO UNA RICHIESTA SUA, E IL MODELLO È DICHIARATO: 'I Grandi di
 * Terramare'** (2026-09-08: *mi servono dei tasti 'scorri in cima' e 'scorri in fondo' che
 * appaiono in sovrimpressione sul lato dello schermo ... la logica è la stessa di quelli di
 * `Earthsea Top` (mobile), e l'aspetto simile ... devono avere ESATTAMENTE la velocità, la
 * decelerazione e la logica di `Earthsea Top`*). Quindi i numeri di questo file non sono
 * scelte: sono **misure prese** su `earthsea/top/index.html`, e chi li ritocca li stacca da
 * quella sorgente.
 */

/** La parte fissa della durata, in millisecondi: `280 + |dist| * 0.16`, con tetto a 800. */
private const val JUMP_BASE_MS = 280f

/** Quanto dura un pixel di corsa. La corsa lunga satura sul tetto, come nel sito. */
private const val JUMP_PER_PX = 0.16f

/** Il tetto della durata: oltre, una corsa lunghissima diventerebbe un'attesa. */
private const val JUMP_MAX_MS = 800f

/**
 * L'easing quintico in uscita: parte rapido e decelera, che è l'inerzia che ha chiesto.
 *
 * ⚠️ **Scritto come `Easing` e non approssimato con una curva di Bézier**: `1 - (1-x)^5` è la
 * riga esatta del sito, e una Bézier che le somiglia sarebbe la stessa cosa detta da un'altra
 * parte, cioè un numero che diverge al primo ritocco.
 */
private val QUINT_OUT = Easing { x -> 1f - (1f - x).pow(5) }

/**
 * Quanto restano in scena dopo che lo scorrimento si è fermato: **due secondi**, ed è suo
 * (*sono visibili solo per 2 secondi*).
 *
 * ⚠️ Sul sito il valore è un altro (0,8 s su mobile, 3 s su desktop): la logica è quella, il
 * numero l'ha dettato lui per questa app.
 */
private const val JUMP_HOLD_MS = 2_000L

/**
 * La quiete che dichiara finito lo scorrimento, prima di far partire il conto alla rovescia.
 *
 * ⚠️⚠️ **È LO 'STADIO 1' DEL SITO, E SENZA DI LUI I DUE SECONDI PARTIREBBERO PRESTO**: un
 * lancio inerziale è fatto di tanti eventi ravvicinati, e ognuno rimette il conto a zero. Chi
 * togliesse questa attesa vedrebbe i tasti sparire mentre la lista sta ancora correndo.
 */
private const val JUMP_SETTLE_MS = 150L

/** L'entrata e l'uscita in dissolvenza, come la transizione di 0,25 s del sito. */
internal const val JUMP_FADE_MS = 250

/**
 * Quanto si vede il tasto: **quattro decimi**, ed è suo (*opacità 40%*).
 *
 * ⚠️ Vale sul **fondo** e non sul nodo intero: sbiadendo tutto insieme il glifo scenderebbe
 * sotto il contrasto minimo proprio sopra le miniature, che è il fondo peggiore che ci sia.
 */
private const val JUMP_INK = 0.4f

/**
 * Il lato del glifo: **più piccolo del FAB**, ed è suo (*dimensione: più piccoli del FAB*).
 *
 * ⚠️⚠️ **DALLA `2.00` QUESTO È IL GLIFO E NON PIÙ IL TASTO INTERO**, ed è la correzione della
 * voce `salti-tasti` (*rendi i glifi più grandi ed elimina i tondi di sfondo*): finché il tondo
 * c'era, il disegno dentro ne misurava sei decimi, cioè meno di 17dp. Adesso il tondo non esiste
 * e questi 28dp sono quello che si vede.
 */
private val JUMP_SIZE = 28.dp

/**
 * Il lato dell'**area di tocco**, che è quella del FAB.
 *
 * ⚠️⚠️ **DALLA `2.00` LA DECIDE QUESTA RIGA E NON UN COMPONENTE**: fino alla `1.95` il tasto era
 * un `IconButton`, che porta con sé `minimumInteractiveComponentSize`, un `size` proprio, un
 * `clip` e un ripple, cioè quattro decisioni che qui non ha preso nessuno. Misurato dal banco, il
 * bersaglio veniva 28dp, sotto i 40 del FAB accanto.
 * ⚠️ **Uguale al FAB** perché la colonna è larga così: il bersaglio riempie la colonna e i due
 * centri coincidono per costruzione invece che per un numero scritto due volte. Presidiata da
 * `SaltiTest`.
 */
internal val JUMP_TAP = FAB_SIZE

/** L'aria fra i due tasti. */
private val JUMP_GAP = 8.dp

/** L'aria fra il tasto di sotto e il FAB. */
private val JUMP_FROM_FAB = 12.dp

/**
 * Ferma la corsa quando la lista non prende più niente.
 *
 * ⚠️ **Un'eccezione e non una bandierina**: dentro `scroll {}` la lista è bloccata per chiunque
 * altro, quindi girare a vuoto fino alla fine della durata vorrebbe dire un'app che non
 * risponde al dito per mezzo secondo dopo essere arrivata in fondo.
 */
private class Arrivato : CancellationException("bordo")

/**
 * I due tasti che portano in cima e in fondo, sopra il FAB e sul suo stesso lato.
 *
 * ⚠️⚠️ **LA COLONNA È LARGA QUANTO IL FAB, ED È COSÌ CHE I CENTRI COINCIDONO** (sua
 * precisazione: *devono apparire sopra il FAB (a destra o sinistra) ed essere perfettamente
 * allineati orizzontalmente con il centro del FAB stesso*): i glifi sono più piccoli, quindi
 * centrarli dentro una colonna larga [FAB_SIZE] è la sola forma che tiene l'allineamento
 * qualunque sia la loro misura. Con un allineamento al bordo, cambiando [JUMP_SIZE] i due
 * centri si scollerebbero senza che nessuno se ne accorga.
 * ⚠️ **Lo spazio del FAB se lo mette lei**, perché il chiamante le passa lo **stesso**
 * modificatore di posizione del FAB: così i rientri (quelli di sistema, il margine della
 * schermata) restano scritti una volta sola per schermata.
 * ⚠️⚠️ **A TASTI NASCOSTI QUI NON C'È NIENTE CHE POSSA RUBARE UN TOCCO**: la colonna non porta
 * nessun modificatore di puntatore, e il solo nodo che ne ha uno esce dall'albero con la
 * dissolvenza. È la trappola della `1.70`, e la difesa è la stessa: l'assenza.
 *
 * @param state la lista da scorrere.
 * @param up quanti pixel mancano per arrivare in cima, stimati al momento del tocco.
 * @param down quanti ne mancano per arrivare in fondo.
 * @param nested lo scorrimento annidato della schermata, se c'è: è quello che chiude e riapre
 *   l'intestazione, e passando di qui il tasto 'su' arriva **fino alla fascia aperta**, che è
 *   la sua richiesta. Senza, i tasti muovono la sola lista.
 * @param more se c'è ancora qualcosa sopra oltre alla lista: la fascia chiusa. Serve perché
 *   chiudendo l'intestazione la lista **non** si muove, quindi `canScrollBackward` risponde di
 *   no proprio nel caso in cui il tasto 'su' ha più da fare.
 * @param aboveFab se sotto la colonna c'è il FAB, e quindi va lasciato il suo posto. Spento
 *   nelle **impostazioni**, che sono la sola schermata coi tasti e senza FAB: là quello spazio
 *   sarebbe aria in fondo allo schermo.
 */
@Composable
fun JumpFabs(
    state: ScrollableState,
    up: () -> Float,
    down: () -> Float,
    modifier: Modifier = Modifier,
    nested: NestedScrollConnection? = null,
    more: () -> Boolean = { false },
    aboveFab: Boolean = true
) {
    var awake by remember { mutableStateOf(false) }
    /*
     * ⚠️ **Il conto alla rovescia si annulla da sé a ogni ripartenza** (`collectLatest`), che
     * è il modo in cui il sito rimette a zero il suo timer: senza, un lancio inerziale
     * lascerebbe in coda un congedo già programmato.
     */
    LaunchedRestart(state) { corre ->
        if (corre) {
            awake = true
        } else if (awake) {
            delay(JUMP_SETTLE_MS + JUMP_HOLD_MS)
            awake = false
        }
    }
    /*
     * ⚠️ **Le tre lambda si leggono VIVE**: `derivedStateOf` nasce una volta sola, quindi
     * quella catturata sarebbe la lambda del primo giro. Chi passa una condizione che dipende
     * da un valore ricalcolato a ogni ricomposizione se la troverebbe congelata, e non darebbe
     * nessun errore.
     */
    val extra by rememberUpdatedState(more)
    val goUp by remember(state) { derivedStateOf { state.canScrollBackward || extra() } }
    val goDown by remember(state) { derivedStateOf { state.canScrollForward } }
    val scope = rememberCoroutineScope()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(JUMP_GAP),
        modifier = modifier
            .width(FAB_SIZE)
            .padding(bottom = if (aboveFab) FAB_SIZE + JUMP_FROM_FAB else JUMP_FROM_FAB)
    ) {
        JumpFab(
            visible = awake && goUp,
            glyph = Glyphs.BrowseTop,
            label = stringResource(R.string.jump_top)
        ) { scope.launch { glide(state, nested, -up()) } }
        JumpFab(
            visible = awake && goDown,
            glyph = Glyphs.BrowseBottom,
            label = stringResource(R.string.jump_bottom)
        ) { scope.launch { glide(state, nested, down()) } }
    }
}

/**
 * Un tasto solo: **il glifo e basta**, del colore del tema opposto.
 *
 * ⚠️⚠️ **DALLA `2.00` NON C'È NESSUN TONDO DIETRO, ED È SUA ISTRUZIONE** (riscontro del giro
 * della `1.95`, voce `salti-tasti` non approvata: *i tondi in cui si trovano (che comunque non
 * avevo chiesto) appaiono come rettangoli ad ogni tocco, e flashano pieni di glitch. Rendi i
 * glifi più grandi ed elimina i tondi di sfondo*). Con il tondo se ne va la causa di tutte e due
 * le cose che ha visto: la forma che compariva premendo era lo **stato premuto** del componente
 * di Material, e il fondo semitrasparente era quello che la faceva vedere.
 * ⚠️⚠️ **QUINDI NIENTE `IconButton` E NIENTE INDICAZIONE DI STATO**: `indication = null` toglie
 * l'unica cosa che questo tasto disegnava oltre al proprio glifo. Un riscontro al tocco qui non
 * serve, perché il tocco fa partire una corsa che si vede.
 * ⚠️ **Il colore passa al glifo**, e non è un cambiamento della sua specifica ma la sua
 * conseguenza: il 40% del fondo opposto (*colore dello sfondo scuro su tema chiaro e dello
 * sfondo chiaro su tema scuro, opacità 40%*) era del tondo, e senza il tondo un glifo del colore
 * del fondo in vigore sparirebbe sul fondo in vigore.
 * ⚠️ **Un bersaglio solo e un'etichetta sola**, come la riga di un interruttore: l'etichetta sta
 * sul nodo che si tocca, e il glifo dentro non ne porta una sua.
 */
@Composable
private fun JumpFab(
    visible: Boolean,
    glyph: ImageVector,
    label: String,
    onTap: () -> Unit
) {
    val light = LocalAivLight.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(JUMP_FADE_MS)),
        exit = fadeOut(tween(JUMP_FADE_MS))
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(JUMP_TAP)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Button,
                    onClick = onTap
                )
                .semantics { contentDescription = label }
        ) {
            Icon(
                imageVector = glyph,
                contentDescription = null,
                tint = aivGround(!light).copy(alpha = JUMP_INK),
                modifier = Modifier.size(JUMP_SIZE)
            )
        }
    }
}

/**
 * Guarda se la lista sta correndo, e chiama il blocco a ogni cambio.
 *
 * ⚠️ Vive in una funzione sua perché il `collectLatest` che annulla il conto alla rovescia si
 * legge male in mezzo a una colonna di tasti, e qui ha un nome che dice che cosa fa.
 */
@Composable
private fun LaunchedRestart(state: ScrollableState, block: suspend (Boolean) -> Unit) {
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.collectLatest(block)
    }
}

/**
 * La corsa, con la durata e la decelerazione del sito.
 *
 * ⚠️⚠️ **IL DELTA PASSA DALLO SCORRIMENTO ANNIDATO, ESATTAMENTE COME UN DITO**: prima
 * `onPreScroll` (che chiude l'intestazione scendendo), poi la lista, poi `onPostScroll` (che la
 * riapre salendo). Muovendo la sola lista, il tasto 'su' si sarebbe fermato con la fascia
 * ancora chiusa, e lui ha chiesto il contrario (*fa scorrere in cima fino alla visualizzazione
 * piena dell'intestazione*). Scritto così quel comportamento non è una riga in più: è quello
 * che già succede col dito.
 *
 * @param quanti pixel di lista, positivi verso il fondo. La stima serve solo alla **durata**:
 *   la corsa finisce quando nessuno prende più niente, quindi una stima lunga non porta oltre
 *   il bordo e una corta arriva lo stesso, con meno decelerazione.
 */
internal suspend fun glide(
    state: ScrollableState,
    nested: NestedScrollConnection?,
    quanti: Float
) {
    if (abs(quanti) < 1f) return
    val ms = min(JUMP_MAX_MS, JUMP_BASE_MS + abs(quanti) * JUMP_PER_PX).toInt()
    var done = 0f
    var still = 0
    try {
        state.scroll {
            animate(0f, quanti, animationSpec = tween(ms, easing = QUINT_OUT)) { value, _ ->
                val delta = value - done
                done = value
                if (delta != 0f) {
                    if (abs(spend(nested, delta)) < 0.5f) {
                        if (++still >= 2) throw Arrivato()
                    } else {
                        still = 0
                    }
                }
            }
        }
    } catch (_: Arrivato) {
        // Il bordo: la corsa è finita prima della durata, e non è un errore.
    }
}

/**
 * Spende un pezzo di corsa fra l'intestazione e la lista, e dice quanti pixel sono andati.
 *
 * ⚠️ **I segni sono due mondi**: `scrollBy` conta positivo verso il fondo, il puntatore conta
 * positivo verso il basso, cioè verso l'inizio. Il `-` davanti a ogni passaggio è la
 * conversione, e toglierne uno solo fa un tasto che va dalla parte sbagliata.
 */
private fun ScrollScope.spend(nested: NestedScrollConnection?, delta: Float): Float {
    val hand = Offset(0f, -delta)
    val pre = nested?.onPreScroll(hand, NestedScrollSource.UserInput) ?: Offset.Zero
    val rest = delta + pre.y
    val got = scrollBy(rest)
    val left = rest - got
    val post = nested?.onPostScroll(
        consumed = Offset(0f, -got),
        available = Offset(0f, -left),
        source = NestedScrollSource.UserInput
    ) ?: Offset.Zero
    return -pre.y + got + -post.y
}

/**
 * Quanti pixel mancano per arrivare in cima a una griglia.
 *
 * ⚠️⚠️ **È UNA STIMA, E NON PUÒ ESSERE ALTRO**: una lista pigra non sa quanto è alto quello che
 * non ha ancora composto, quindi l'altezza di una riga si ricava da quelle in scena. Serve alla
 * sola **durata** della corsa, che è un numero con un tetto: sbagliarla di un fattore due
 * cambia i millisecondi, non dove si arriva.
 */
fun LazyGridState.jumpUpPixels(): Float {
    val seen = layoutInfo.visibleItemsInfo
    if (seen.isEmpty()) return 0f
    return seen.first().row * rowHeight(seen) + firstVisibleItemScrollOffset
}

/** Quanti pixel mancano per arrivare in fondo a una griglia. Vedi [jumpUpPixels]. */
fun LazyGridState.jumpDownPixels(): Float {
    val info = layoutInfo
    val seen = info.visibleItemsInfo
    if (seen.isEmpty()) return 0f
    val columns = seen.maxOf { it.column } + 1
    val rows = (info.totalItemsCount + columns - 1) / columns
    val below = rows - 1 - seen.maxOf { it.row }
    val tail = seen.maxOf { it.offset.y + it.size.height } -
        (info.viewportEndOffset - info.afterContentPadding)
    return below * rowHeight(seen) + tail.coerceAtLeast(0)
}

/** L'altezza di una riga, spazio compreso, misurata su quelle in scena. */
private fun rowHeight(seen: List<LazyGridItemInfo>): Float {
    val rows = seen.distinctBy { it.row }.size.coerceAtLeast(1)
    val span = seen.maxOf { it.offset.y + it.size.height } - seen.minOf { it.offset.y }
    return span.toFloat() / rows
}

/**
 * Quanti pixel mancano per arrivare in cima a una **pagina che scorre tutta intera**.
 *
 * ⚠️⚠️ **QUESTA NON È UNA STIMA, ed è l'unica delle sei**: una colonna con `verticalScroll`
 * misura tutto il proprio contenuto, quindi la posizione e il fondo sono due numeri esatti.
 * Serve alle **impostazioni**, che sono la pagina più lunga dell'app, e la risposta `tutto` a
 * `d-salti-dove` le ha portate dentro.
 */
fun ScrollState.jumpUpPixels(): Float = value.toFloat()

/** Quanti pixel mancano per arrivare in fondo a una pagina che scorre. Vedi [jumpUpPixels]. */
fun ScrollState.jumpDownPixels(): Float = (maxValue - value).toFloat()

/** Quanti pixel mancano per arrivare in cima a un elenco. Vedi [jumpUpPixels]. */
fun LazyListState.jumpUpPixels(): Float {
    val seen = layoutInfo.visibleItemsInfo
    if (seen.isEmpty()) return 0f
    val high = seen.sumOf { it.size }.toFloat() / seen.size
    return firstVisibleItemIndex * high + firstVisibleItemScrollOffset
}

/** Quanti pixel mancano per arrivare in fondo a un elenco. Vedi [jumpUpPixels]. */
fun LazyListState.jumpDownPixels(): Float {
    val info = layoutInfo
    val seen = info.visibleItemsInfo
    if (seen.isEmpty()) return 0f
    val high = seen.sumOf { it.size }.toFloat() / seen.size
    val below = info.totalItemsCount - 1 - seen.last().index
    val tail = seen.last().let { it.offset + it.size } -
        (info.viewportEndOffset - info.afterContentPadding)
    return below * high + tail.coerceAtLeast(0)
}

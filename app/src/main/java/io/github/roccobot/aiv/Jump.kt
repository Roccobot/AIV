package io.github.roccobot.aiv

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

/*
 * Il salto in cima e in fondo è il FAB stesso: scorrendo, il suo glifo diventa un chevron, con lo
 * stesso incrocio di zoom e dissolvenza con cui diventa la `×` a menu aperto (scelta dell'utente
 * su un mockup animato, 2.07).
 * Il verso dello scorrimento decide il tasto: verso il fondo 'Vai alla fine', verso l'alto 'Vai
 * all'inizio'. I due versi non sono disponibili insieme; chi vuole l'altro scorre un momento
 * nell'altro senso.
 *
 * ⚠️ **[HAUL], [SWERVE], [QUIET_MS], [BACK_WAIT_MS] e [BACK_MS] sono del mockup approvato**
 * dall'utente (l'artefatto 'Animazione definitiva tasti su/giù'), non scelte di questo file.
 *
 * Dove il FAB non c'è, il comando non c'è: nelle impostazioni per risposta dell'utente (si cerca
 * con la ricerca e con le sezioni), e in 'Cartelle di sistema'.
 */

/** La parte fissa della durata, in millisecondi: `280 + |dist| * 0.16`, con tetto a 800. */
private const val JUMP_BASE_MS = 280f

/** Quanto dura un pixel di corsa. La corsa lunga satura sul tetto, come nel sito. */
private const val JUMP_PER_PX = 0.16f

/** Il tetto della durata: oltre, una corsa lunghissima diventerebbe un'attesa. */
private const val JUMP_MAX_MS = 800f

/**
 * L'easing quintico in uscita: parte rapido e decelera, l'inerzia chiesta dall'utente.
 *
 * `1 - (1-x)^5` è la riga esatta del sito da cui la corsa è copiata: una Bézier che le somiglia
 * potrebbe divergere al primo ritocco.
 */
private val QUINT_OUT = Easing { x -> 1f - (1f - x).pow(5) }

/**
 * La curva del rientro: accelera in mezzo e frena in fondo.
 *
 * Non è la curva di serie di `tween`, come nel mockup: una curva che accelera fino all'ultimo
 * istante fa arrivare il glifo di schianto, e questo tratto dura un quarto di secondo.
 */
private val SMOOTHER = Easing { x -> x * x * x * (x * (x * 6f - 15f) + 10f) }

/**
 * I pixel di scorrimento che trasformano il glifo dell'app nel chevron.
 *
 * È la corsa piena, non una soglia: ogni pixel ne sposta una frazione, e il disegno segue il dito.
 */
private const val HAUL = 44f

/**
 * Quanto di [HAUL] può coprire un evento di scorrimento: un sesto, così il cambio dura almeno sei
 * fotogrammi.
 *
 * ⚠️ **Il salto si arma solo da un lancio** ([JUMP_SPEED]), e il primo fotogramma di un lancio
 * da solo supera [HAUL]: senza tetto il glifo diventerebbe chevron in un fotogramma. Presidiato
 * da `ScattoGlifoTest`.
 */
private const val HAUL_STEP = 1f / 6f

/**
 * I pixel nel verso opposto che fanno cambiare idea al tasto.
 *
 * ⚠️ **Senza questa soglia il glifo sfarfalla** (misurato nel mockup): un dito non va mai in un
 * verso solo, e un rimbalzo di pochi pixel girerebbe il disegno a ogni gesto.
 * Cambiare verso non fa ricominciare: il chevron si gira sul posto e la corsa fatta resta.
 */
private const val SWERVE = 8f

/**
 * Il silenzio che dichiara fermo il dito quando la corsa è incompleta.
 *
 * Distingue una pausa da un lancio inerziale: un lancio è fatto di tanti eventi ravvicinati, e
 * ognuno rimette il conto a zero.
 */
private const val QUIET_MS = 150L

/** L'attesa prima che il glifo dell'app rientri a tasto armato. */
private const val BACK_WAIT_MS = 1_000L

/** Quanto dura il rientro del glifo dell'app. */
private const val BACK_MS = 250

/**
 * L'esponente delle due opacità incrociate.
 *
 * ⚠️ **Sotto uno, ed è misurato**: con due opacità lineari incrociate il tasto a metà corsa
 * appare quasi vuoto; a 0,8 la somma non scende mai sotto il pieno. Un `Crossfade` di serie
 * riporterebbe quel buco.
 */
internal const val JUMP_FULL = 0.8f

/**
 * Quanto si rimpicciolisce il disegno che se ne va, e da quanto arriva quello che entra.
 *
 * A distinguere i due glifi è la scala: si incrociano per tutta la corsa, e senza un movimento
 * che li separi si vedrebbe una macchia sola.
 */
internal const val JUMP_ZOOM = 0.45f

/**
 * Come sbiadiscono il marchio dell'app e i suoi sostituti sul FAB e sul tondo: l'opacità va a
 * ogni disegno, senza un buffer fuori schermo.
 *
 * ⚠️ **Il marchio è disegnato qualche dp sopra il suo riquadro** (`Marchio`, il rialzo ottico), e
 * con la strategia di serie un layer non opaco disegna in un buffer grande quanto il riquadro: la
 * cima del disco resterebbe tagliata per tutta la dissolvenza e tornerebbe con uno scatto (nota C
 * dell'utente sul giro della 4.32). Presidiato da `ScattoGlifoTest`.
 * Il buffer serve quando le forme si sovrappongono; qui un glifo è un vettore solo.
 */
internal val MARK_FADE = CompositingStrategy.ModulateAlpha

/**
 * Ferma la corsa quando la lista non prende più niente.
 *
 * Un'eccezione che interrompe l'animazione: dentro `scroll {}` la lista è bloccata per chiunque
 * altro, e girare a vuoto fino alla fine della durata lascerebbe il dito senza risposta per
 * mezzo secondo.
 */
private class Arrivato : CancellationException("bordo")

/**
 * Quanto veloce deve partire un lancio perché il salto si armi, in dp al secondo.
 *
 * Solo uno scorrimento abbastanza veloce arma il salto (nota C dell'utente sul giro della 4.33):
 * con pochi elementi si scorre poco e piano. La misura è la velocità iniziale del lancio, che il
 * sistema calcola dagli ultimi campioni del dito: chi scorre molti elementi lancia, e un
 * trascinamento che si ferma prima di alzare il dito non lancia. Una volta armato, il glifo segue
 * lo scorrimento.
 * ⚠️ **È una prima stima, da tarare sul suo telefono**: Android avvia un lancio da 50dp/s, e uno
 * deciso arriva alle migliaia.
 */
internal const val JUMP_SPEED = 1_000f

/** Le quattro fasi del glifo, che sono quelle del mockup. */
private enum class Phase { IDLE, PULL, READY, BACK }

/**
 * Lo stato del glifo del FAB: da 0 (il disegno dell'app) a 1 (il chevron).
 *
 * ⚠️ **La corsa del tasto non può girare il chevron, quindi non serve una guardia** (il mockup ne
 * aveva una): [glide] muove la lista dentro `state.scroll {}`, e quel movimento non arriva al
 * nodo che guarda il gesto; e anche passando di lì, una corsa verso l'inizio va nel verso che il
 * chevron già indica. Una guardia sarebbe codice morto, e una prova che la presidiasse sarebbe
 * verde con e senza di lei.
 */
@Stable
class JumpArm internal constructor(
    private val state: ScrollableState,
    private val up: () -> Float,
    private val down: () -> Float,
    /** La velocità di lancio che arma il salto, in pixel al secondo: [JUMP_SPEED] a questa densità. */
    private val fast: Float
) {
    /** Quanto il chevron ha preso il posto del disegno dell'app: da 0 a 1. */
    var shown by mutableFloatStateOf(0f)
        private set

    /** Dove porterebbe il tocco adesso: `-1` verso l'inizio, `+1` verso la fine, `0` nessuno. */
    var toward by mutableIntStateOf(0)
        private set

    private var phase by mutableStateOf(Phase.IDLE)

    /** Vero da un lancio abbastanza veloce ([fast]) al rientro del glifo: solo allora un pixel conta. */
    private var flung = false
    private var run = 0f
    private var against = 0f

    /** Cresce a ogni pixel raccolto: è quello che fa ripartire l'attesa del congedo. */
    internal var tick by mutableIntStateOf(0)
        private set

    /** Vero quando il tocco fa il salto invece di aprire il menu. */
    val armed: Boolean get() = phase == Phase.READY

    /**
     * Lo scorrimento annidato da montare sul contenitore della schermata.
     *
     * Non consuma niente: guarda il delta del dito prima che qualcuno lo spenda, l'unico punto in
     * cui il gesto si legge intero. L'intestazione ne consuma una parte per chiudersi, e anche
     * quel tratto arma il tasto, come nel mockup.
     */
    internal val watch = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Un trascinamento lento non arma il salto: vedi [JUMP_SPEED].
            if (flung || phase != Phase.IDLE) pull(available.y)
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (abs(available.y) >= fast) flung = true
            return Velocity.Zero
        }
    }

    /**
     * Il dito che scorre: ogni pixel avvicina il glifo al chevron.
     *
     * ⚠️ **I segni sono opposti**: il puntatore conta positivo verso il basso, cioè verso l'inizio
     * della lista, `scrollBy` verso il fondo. Il verso del chevron è quello della lista, quindi un
     * dito che sale (`y` negativa) arma 'Vai alla fine'.
     */
    private fun pull(dy: Float) {
        val px = abs(dy)
        if (px < 0.5f) return
        val sign = if (dy < 0f) 1 else -1
        if (toward == 0) {
            toward = sign
        } else if (sign != toward) {
            against += px
            if (against < SWERVE) return
            against = 0f
            toward = sign
        } else {
            against = 0f
        }
        phase = Phase.PULL
        // Al più [HAUL_STEP] della corsa per evento: il perché è là.
        run = min(HAUL, run + min(px, HAUL * HAUL_STEP))
        shown = run / HAUL
        if (shown >= 1f) phase = Phase.READY
        tick++
    }

    /**
     * Il rientro del disegno dell'app.
     *
     * La corsa accumulata si azzera solo alla fine: un pixel che arriva mentre il glifo rientra
     * riprende da dov'era, come nel mockup.
     * ⚠️ **Un rientro annullato a metà riparte da dov'è** ([shown]): lo annulla ogni `tick`, anche
     * quello di una corsa della pillola, che non passa da [pull]. Lasciato in `BACK`, il glifo
     * restava mezzo chevron, e il primo trascinamento anche lento armava il salto.
     */
    private suspend fun retreat() {
        if (phase == Phase.IDLE) return
        phase = Phase.BACK
        animate(shown, 0f, animationSpec = tween(BACK_MS, easing = SMOOTHER)) { v, _ ->
            shown = v
        }
        shown = 0f
        run = 0f
        against = 0f
        toward = 0
        flung = false
        phase = Phase.IDLE
    }

    /**
     * Il congedo, con i suoi due tempi: uno a corsa incompleta, l'altro a tasto armato. Un rientro
     * interrotto si completa subito: vedi [retreat].
     */
    internal suspend fun idle() {
        when (phase) {
            Phase.PULL -> delay(QUIET_MS)
            Phase.READY -> delay(BACK_WAIT_MS)
            Phase.BACK -> {}
            Phase.IDLE -> return
        }
        retreat()
    }

    /**
     * La corsa verso un capo scelto: `-1` l'inizio, `+1` la fine.
     *
     * Serve alla pillola estesa, che ha due tasti e non un chevron solo (risposta C2 dell'utente,
     * 4.00): il capo lo sceglie il tasto, non il verso dell'ultimo scorrimento.
     */
    internal suspend fun leapToward(capo: Int, nested: NestedScrollConnection?) {
        val quanti = if (capo > 0) down() else -up()
        try {
            glide(state, nested, quanti)
        } finally {
            tick++
        }
    }

    /** La corsa verso il capo che il chevron indica. */
    internal suspend fun leap(nested: NestedScrollConnection?) {
        val quanti = if (toward > 0) down() else -up()
        try {
            glide(state, nested, quanti)
        } finally {
            // Finita la corsa ricomincia l'attesa: il glifo rientra un secondo dopo.
            tick++
        }
    }
}

/**
 * Tiene lo stato del glifo del FAB per una schermata.
 *
 * @param state la lista da scorrere.
 * @param up quanti pixel mancano per arrivare in cima, stimati al momento del tocco.
 * @param down quanti ne mancano per arrivare in fondo.
 */
@Composable
fun rememberJumpArm(
    state: ScrollableState,
    up: () -> Float,
    down: () -> Float
): JumpArm {
    /*
     * ⚠️ **Le due lambda si leggono vive**: l'oggetto si crea una volta sola, e una lambda
     * catturata resterebbe quella del primo giro, congelata senza nessun errore.
     */
    val alto by rememberUpdatedState(up)
    val basso by rememberUpdatedState(down)
    val fast = with(LocalDensity.current) { JUMP_SPEED.dp.toPx() }
    val arm = remember(state, fast) { JumpArm(state, { alto() }, { basso() }, fast) }
    /*
     * `collectLatest` rimette a zero il congedo: ogni pixel alza `tick`, l'attesa precedente è
     * annullata e ne parte una nuova. Senza, un lancio lascerebbe in coda un rientro già
     * programmato.
     */
    LaunchedEffect(arm) {
        snapshotFlow { arm.tick }.collectLatest { arm.idle() }
    }
    return arm
}

/**
 * Il glifo del FAB: il disegno dell'app e il chevron, incrociati per tutta la corsa.
 *
 * I due disegni non si danno il cambio a metà: sfasati, il FAB resterebbe quasi vuoto fra l'uno e
 * l'altro. Li distingue la scala ([JUMP_ZOOM]), come nella transizione verso la `×`.
 * Una descrizione sola, quella del tasto: la passa il chiamante a `TapHoldFab` ([jumpLabel]),
 * perché a tasto armato il FAB fa un'altra cosa.
 *
 * @param home il disegno del FAB a riposo.
 */
@Composable
fun JumpGlyph(arm: JumpArm, home: @Composable () -> Unit) {
    val q = arm.shown
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.graphicsLayer {
                alpha = (1f - q).pow(JUMP_FULL)
                // Senza buffer: vedi [MARK_FADE].
                compositingStrategy = MARK_FADE
                val s = 1f - JUMP_ZOOM * q
                scaleX = s
                scaleY = s
            }
        ) { home() }
        // A riposo il chevron non è nell'albero: un nodo trasparente riceve comunque i tocchi e
        // conta per il lettore di schermo.
        if (q > 0f) {
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = q.pow(JUMP_FULL)
                    val s = 1f - JUMP_ZOOM * (1f - q)
                    scaleX = s
                    scaleY = s
                }
            ) {
                Icon(
                    imageVector = if (arm.toward > 0) Glyphs.BrowseBottom else Glyphs.BrowseTop,
                    contentDescription = null
                )
            }
        }
    }
}

/** Che cosa fa il FAB adesso, per il lettore di schermo: il salto, o quello che faceva. */
@Composable
fun jumpLabel(arm: JumpArm, home: String): String = when {
    !arm.armed -> home
    arm.toward > 0 -> stringResource(R.string.jump_bottom)
    else -> stringResource(R.string.jump_top)
}

/**
 * La corsa, con la durata e la decelerazione del sito.
 *
 * ⚠️ **Il delta passa dallo scorrimento annidato, come un dito**: prima `onPreScroll` (che chiude
 * l'intestazione scendendo), poi la lista, poi `onPostScroll` (che la riapre salendo). Così 'Vai
 * all'inizio' arriva fino all'intestazione piena, come ha chiesto l'utente.
 *
 * @param quanti pixel di lista, positivi verso il fondo. La stima serve solo alla durata: la
 *   corsa finisce quando nessuno prende più niente, quindi una stima lunga non va oltre il bordo
 *   e una corta arriva lo stesso, con meno decelerazione.
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
 * ⚠️ **I segni sono opposti** (vedi `pull`): il `-` davanti a ogni passaggio è la conversione, e
 * toglierne uno solo manda il tasto dalla parte sbagliata.
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
 * È una stima: una lista pigra non sa quanto è alto quello che non ha ancora composto, e
 * l'altezza di una riga si ricava da quelle in scena. Serve alla sola durata della corsa:
 * sbagliarla cambia i millisecondi, non dove si arriva.
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

/** Quanti pixel mancano per arrivare in cima a una pagina che scorre tutta intera. */
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

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

/*
 * ⚠️⚠️ **DALLA `2.07` I DUE TASTI NON CI SONO PIÙ: A PORTARE IN CIMA E IN FONDO È IL FAB, E IL
 * SUO GLIFO DIVENTA UN CHEVRON** (sua scelta del 2026-09-09, dopo aver guardato due mockup
 * animati: *questo è molto più pulito e fluido ... ho già scelto, appena possibile implementiamo
 * questo*). Il pezzo nuovo non esiste: niente colonna che compare, niente seconda finestra,
 * niente tasto in più da mettere da qualche parte, e il FAB non sparisce mai dallo schermo.
 * Quello che cambia è il **disegno** dentro un tasto che c'era già, con lo stesso incrocio di
 * zoom e dissolvenza con cui oggi diventa la `×` a menu aperto.
 *
 * ⚠️⚠️ **E IL TASTO È UNO SOLO, DECISO DAL VERSO DELLO SCORRIMENTO** (*se scorro per vedere
 * altre immagini in basso, appare solo il tasto 'giù'*): scorrendo verso il fondo il glifo
 * diventa 'Vai alla fine', scorrendo verso l'alto 'Vai all'inizio'. Le due domande di prima
 * (dove voglio andare, e quale dei due tasti tocco) diventano una sola.
 * ⚠️ **Quello che si perde è dichiarato**: i due versi non sono più disponibili insieme, e chi
 * vuole l'altro scorre un momento nell'altro senso.
 *
 * ⚠️⚠️ **I CINQUE NUMERI SONO DEL MOCKUP CHE HA APPROVATO GUARDANDOLO**, e non sono scelte di
 * questo file: [HAUL], [SWERVE], [QUIET_MS], [BACK_WAIT_MS] e [BACK_MS]. Chi li ritocca li
 * stacca da quella sorgente, che è l'artefatto 'Animazione definitiva tasti su/giù'.
 *
 * ⚠️⚠️ **CHE COSA DECADE CON LORO, e va saputo per non cercarlo**: i due tempi tarati nella
 * `2.05` (0,8 secondi pieni più 1,6 di dissolvenza) non esistono più, perché non c'è più una
 * colonna che compare e sbiadisce; e la durata della corsa, la sua curva e i due stadi del
 * congedo restano, perché sono del salto e non del tasto.
 *
 * ⚠️⚠️ **DOVE IL FAB NON C'È, IL COMANDO NON C'È PIÙ.** Nelle **impostazioni** è la sua
 * risposta alla lettera (2026-09-09: *lì non serve nessun tasto, in realtà ... le impostazioni
 * che cerco le trovo o con la ricerca o con le sezioni e le sotto-pagine, non scorrendo una
 * lista finché non vedo quello che cerco*); in **'Cartelle di sistema'** è la conseguenza di
 * un tasto che vive sul FAB, e là il FAB non esiste.
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
 * riga esatta del sito da cui la corsa è copiata, e una Bézier che le somiglia sarebbe la
 * stessa cosa detta da un'altra parte, cioè un numero che diverge al primo ritocco.
 */
private val QUINT_OUT = Easing { x -> 1f - (1f - x).pow(5) }

/**
 * La curva del rientro: accelera in mezzo e frena in fondo.
 *
 * ⚠️ **Non è la curva di serie di `tween`**, ed è la stessa scelta del mockup: una curva che
 * accelera fino all'ultimo istante fa arrivare il glifo di schianto, e questo tratto lo si
 * guarda per un quarto di secondo intero.
 */
private val SMOOTHER = Easing { x -> x * x * x * (x * (x * 6f - 15f) + 10f) }

/**
 * I pixel di scorrimento che portano il glifo dell'app al chevron: **quarantaquattro**.
 *
 * ⚠️ È la corsa piena, non una soglia: ogni pixel ne sposta una frazione, quindi il disegno
 * segue il dito invece di scattare a un certo punto.
 */
private const val HAUL = 44f

/**
 * I pixel nel verso opposto che fanno cambiare idea al tasto: **otto**.
 *
 * ⚠️⚠️ **SENZA QUESTA SOGLIA IL GLIFO SFARFALLA**, ed è misurato nel mockup: un dito che scorre
 * non va mai in un verso solo, e un rimbalzo di pochi pixel girerebbe il disegno a ogni gesto.
 * ⚠️ **Cambiare verso non fa ricominciare da capo**: il chevron si gira sul posto e la corsa
 * fatta finora resta, perché il tasto offre sempre la corsa che ha senso adesso.
 */
private const val SWERVE = 8f

/**
 * Il silenzio che dichiara fermo il dito quando la corsa è **incompleta**: 150 millisecondi.
 *
 * ⚠️ È lo stesso numero con cui i due tasti di prima distinguevano una pausa da un lancio
 * inerziale, e viene di là: un lancio è fatto di tanti eventi ravvicinati, e ognuno rimette il
 * conto a zero.
 */
private const val QUIET_MS = 150L

/** L'attesa prima che il glifo dell'app rientri a tasto **armato**: un secondo. */
private const val BACK_WAIT_MS = 1_000L

/** Quanto dura il rientro del glifo dell'app. */
private const val BACK_MS = 250

/**
 * L'esponente delle due opacità incrociate.
 *
 * ⚠️⚠️ **SOTTO UNO, E IL PERCHÉ È MISURATO**: con due opacità **lineari** incrociate, a metà
 * corsa i due glifi sono tutti e due al 9% nello stesso fotogramma, cioè il tasto resta vuoto.
 * A 0,8 la somma non scende mai sotto il pieno. Chi lo scrive con un `Crossfade` di serie si
 * riprende quel buco.
 */
private const val FULL = 0.8f

/**
 * Quanto si rimpicciolisce il disegno che se ne va, e da quanto arriva quello che entra.
 *
 * ⚠️ **A distinguere i due glifi è la SCALA e non il turno**: si incrociano per tutta la corsa,
 * quindi senza un movimento che li separi si vedrebbe una macchia sola.
 */
private const val JUMP_ZOOM = 0.45f

/**
 * Ferma la corsa quando la lista non prende più niente.
 *
 * ⚠️ **Un'eccezione e non una bandierina**: dentro `scroll {}` la lista è bloccata per chiunque
 * altro, quindi girare a vuoto fino alla fine della durata vorrebbe dire un'app che non
 * risponde al dito per mezzo secondo dopo essere arrivata in fondo.
 */
private class Arrivato : CancellationException("bordo")

/** Le quattro fasi del glifo, che sono quelle del mockup. */
private enum class Phase { IDLE, PULL, READY, BACK }

/**
 * Lo stato del glifo del FAB: da 0 (il disegno dell'app) a 1 (il chevron).
 *
 * ⚠️⚠️ **LA CORSA DEL TASTO NON PUÒ GIRARE IL CHEVRON, E LA RAGIONE NON È UNA GUARDIA: È IL
 * SEGNO.** Nel mockup c'era una riga che escludeva la corsa dal conto del gesto, con la sua
 * spiegazione (*toccando 'vai all'inizio' la lista sale, cioè scorre nel verso opposto a quello
 * che ha armato il tasto*), e la prima stesura di questo file l'aveva copiata. **Due
 * controprove l'hanno smentita.**
 * ⚠️⚠️ **PRIMA MISURA**: togliendo la guardia, le prove del banco restavano verdi, perché
 * [glide] muove la lista **dentro** `state.scroll {}` e quel movimento non risale la catena dei
 * modificatori: al nodo che guarda il gesto non arriva niente.
 * ⚠️⚠️ **SECONDA MISURA, ed è quella che chiude la questione**: passando alla corsa proprio quel
 * nodo, cioè facendole attraversare il motore, il chevron **non si gira lo stesso**. Una corsa
 * verso l'inizio muove la lista verso l'inizio, che è il verso che il chevron già indica, quindi
 * il segno che ne esce è quello già armato. Il rischio descritto nel mockup non esiste in
 * nessuna delle due strade.
 * ⚠️ **Per questo qui non c'è nessuna bandierina**, ed è scritto perché non la rimetta nessuno:
 * una riga che non ha un caso è codice morto, e una prova che la presidiasse sarebbe verde con
 * e senza di lei.
 */
@Stable
class JumpArm internal constructor(
    private val state: ScrollableState,
    private val up: () -> Float,
    private val down: () -> Float
) {
    /** Quanto il chevron ha preso il posto del disegno dell'app: da 0 a 1. */
    var shown by mutableFloatStateOf(0f)
        private set

    /** Dove porterebbe il tocco adesso: `-1` verso l'inizio, `+1` verso la fine, `0` nessuno. */
    var toward by mutableIntStateOf(0)
        private set

    private var phase by mutableStateOf(Phase.IDLE)
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
     * ⚠️ **Non consuma niente e non è un secondo `frontScroll`**: guarda il delta del dito
     * **prima** che qualcuno lo spenda, che è l'unico punto in cui il gesto si legge intero. La
     * fascia dell'intestazione ne consuma una parte per chiudersi, e quel tratto è gesto come
     * tutto il resto: nel mockup la fascia si chiude sullo stesso scorrimento che arma il tasto.
     */
    internal val watch = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            pull(available.y)
            return Offset.Zero
        }
    }

    /**
     * Il dito che scorre: ogni pixel porta il glifo un po' più verso il chevron.
     *
     * ⚠️ **I segni sono due mondi**: il puntatore conta positivo verso il basso, cioè verso
     * l'inizio della lista, mentre `scrollBy` conta positivo verso il fondo. Il verso del
     * chevron è quello della **lista**, quindi un dito che sale (`y` negativa) arma 'Vai alla
     * fine'.
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
        run = min(HAUL, run + px)
        shown = run / HAUL
        if (shown >= 1f) phase = Phase.READY
        tick++
    }

    /**
     * Il rientro del disegno dell'app.
     *
     * ⚠️ **La corsa accumulata non si azzera prima della fine**: un pixel che arriva mentre il
     * glifo rientra riprende da dov'era invece di ricominciare, ed è il comportamento del
     * mockup.
     */
    private suspend fun retreat() {
        if (phase == Phase.IDLE || phase == Phase.BACK) return
        phase = Phase.BACK
        animate(shown, 0f, animationSpec = tween(BACK_MS, easing = SMOOTHER)) { v, _ ->
            shown = v
        }
        shown = 0f
        run = 0f
        against = 0f
        toward = 0
        phase = Phase.IDLE
    }

    /** Il congedo, con i suoi due tempi: uno a corsa incompleta, l'altro a tasto armato. */
    internal suspend fun idle() {
        when (phase) {
            Phase.PULL -> delay(QUIET_MS)
            Phase.READY -> delay(BACK_WAIT_MS)
            else -> return
        }
        retreat()
    }

    /** La corsa verso il capo che il chevron indica. */
    internal suspend fun leap(nested: NestedScrollConnection?) {
        val quanti = if (toward > 0) down() else -up()
        try {
            glide(state, nested, quanti)
        } finally {
            // ⚠️ Finita la corsa ricomincia l'attesa: il glifo rientra un secondo dopo.
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
     * ⚠️ **Le due lambda si leggono VIVE**: l'oggetto nasce una volta sola, quindi quella
     * catturata sarebbe la lambda del primo giro. Chi passa una stima che dipende da un valore
     * ricalcolato a ogni ricomposizione se la troverebbe congelata, e non darebbe nessun errore.
     */
    val alto by rememberUpdatedState(up)
    val basso by rememberUpdatedState(down)
    val arm = remember(state) { JumpArm(state, { alto() }, { basso() }) }
    /*
     * ⚠️ **`collectLatest` è il modo in cui il congedo si rimette a zero**: ogni pixel alza
     * `tick`, quindi l'attesa precedente viene annullata e ne parte una nuova. Senza, un lancio
     * inerziale lascerebbe in coda un rientro già programmato.
     */
    LaunchedEffect(arm) {
        snapshotFlow { arm.tick }.collectLatest { arm.idle() }
    }
    return arm
}

/**
 * Il glifo del FAB: il disegno dell'app e il chevron, incrociati per tutta la corsa.
 *
 * ⚠️⚠️ **I DUE DISEGNI NON SI DÀNNO IL CAMBIO A METÀ**: sfasandoli il FAB resta quasi vuoto
 * nell'istante in cui uno è finito e l'altro non è ancora arrivato. A distinguerli ci pensa la
 * **scala**, non il turno, ed è la stessa forma della transizione verso la `×`.
 * ⚠️ **Una descrizione sola**: a parlare è il tasto, e due descrizioni sullo stesso nodo dànno
 * due voci per un comando solo. Quella giusta la passa il chiamante a `TapHoldFab`, perché a
 * tasto armato il FAB fa un'altra cosa.
 *
 * @param home il disegno di sempre, cioè quello che il FAB porta a riposo.
 */
@Composable
fun JumpGlyph(arm: JumpArm, home: @Composable () -> Unit) {
    val q = arm.shown
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.graphicsLayer {
                alpha = (1f - q).pow(FULL)
                val s = 1f - JUMP_ZOOM * q
                scaleX = s
                scaleY = s
            }
        ) { home() }
        /*
         * ⚠️ **A riposo il chevron non è nell'albero**, e non è un risparmio: un nodo
         * trasparente riceve comunque i tocchi e conta per il lettore di schermo, e questo ne
         * porterebbe uno dentro il FAB che ne ha già uno suo.
         */
        if (q > 0f) {
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = q.pow(FULL)
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

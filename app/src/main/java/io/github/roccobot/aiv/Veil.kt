package io.github.roccobot.aiv

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * La **Sfocatura dietro i pannelli**: il velo e la sfocatura dietro tutto quello che si apre
 * sopra la schermata (richiesta dell'utente, giro della 1.37). Stacca la superficie che si apre
 * da quello che resta sotto.
 *
 * Le schede della selezione (`PickSheet`) e del ritaglio ne sono esenti per costruzione: non
 * sono finestre ma `Surface` dentro la schermata, perché con loro aperte si deve poter toccare
 * quello che c'è sotto. Trasformate in `ModalBottomSheet`, avrebbero anche il velo.
 *
 * ⚠️ **Il velo lo dipinge l'app, la sfocatura resta di finestra**: due finestre non possono
 * cambiare il proprio velo nello stesso fotogramma, e da lì veniva il lampo (il perché è su
 * [VeilStage]). La sfocatura non si può dipingere senza il rifacimento descritto in fondo a
 * questo file. Il velo dipinto è un rettangolo dell'app, quindi può avere un colore, al
 * contrario di `FLAG_DIM_BEHIND`. Quantità, colore e tempo sono in [VEIL_DOSE], [veilInk] e
 * [AppVeil].
 *
 * ⚠️ **La sfocatura si applica alla finestra che ospita chi la chiede, e sbagliare finestra è
 * silenzioso**: chiesta dal posto sbagliato va sulla finestra dell'attività, senza effetto e
 * senza errore. Per questo le vie sono due:
 * - [WindowVeil] per chi è dentro la finestra da velare: il contenuto di un `Popup` (i menu) o
 *   di un `Dialog` scritto in casa (le schede in fondo);
 * - [Modifier.veiled] per i dialoghi di Material, le cui fessure (`title`, `text`) sono lambda:
 *   un nodo di modificatore si aggancia dove il modificatore è attaccato, cioè dentro il
 *   dialogo. Ci arriva da [Modifier.lowered], che quei dialoghi hanno già.
 *
 * Il velo dipinto è uno solo e vale quanto la richiesta più forte, quindi mentre un menu esce e
 * un dialogo entra non cambia (vedi [VeilStage]).
 *
 * È dietro un'impostazione a tre scelte, sfocatura, ombra o niente, mai insieme (richiesta
 * dell'utente, 1.81), letta da [LocalAivDepth]: questo file lavora solo in [PanelDepth.BLUR], e
 * negli altri casi non aggiunge niente. L'ombra è in `Edge.kt`, perché è il contorno di una
 * superficie.
 * ⚠️ **L'unica eccezione è la scheda in fondo**, che si chiede un velo da sé perché la sua
 * finestra non ne ha uno di serie (il parametro [bare], e `SHEET_DIM` in `Sheet.kt`).
 */
@Composable
fun WindowVeil(
    bare: Float = 0f,
    /**
     * L'avanzamento del velo dipinto di questa superficie, la quota che chiede a [VeilStage]: la
     * finestra che esce lo confronta con la scena (vedi [blurHeld]). La scheda passa la sua
     * patina, che esce con una curva diversa dalla sfocatura; un menu le muove insieme.
     */
    patina: (() -> Float)? = null,
    quanto: () -> Float = { PIENO }
) {
    val view = LocalView.current
    val on = LocalAivDepth.current == PanelDepth.BLUR
    val radius = with(LocalDensity.current) { BLUR.roundToPx() }
    val misura by rememberUpdatedState(quanto)
    val quota by rememberUpdatedState(patina ?: quanto)
    val velo = remember(view, on, bare, radius) { veilFor(view, on, bare, radius) }
    /*
     * La sfocatura si dosa a ogni fotogramma (richiesta dell'utente, 1.50): `snapshotFlow` legge
     * l'avanzamento dell'animazione di chi chiama; chi non ne ha una passa [PIENO]. Su un menu
     * costa un `updateViewLayout` per fotogramma, e la via che lo eviterebbe è il rifacimento
     * descritto in fondo a questo file.
     * Il dosaggio vale anche a funzione spenta, per il velo di finestra della scheda in fondo
     * ([veilFor], ramo `bare`): la scheda passa la sua patina, che entra ed esce con lei.
     * Con la funzione spenta e nessun velo di finestra, [veilFor] risponde `null` e il flusso
     * non parte.
     */
    /*
     * ⚠️ **Una superficie che si apre mentre un'altra è in scena eredita la sfocatura piena**
     * (nota dell'utente su `4.44-02`): la sfocatura del menu se ne va con la sua uscita di 75 ms,
     * e quella della scheda crescerebbe da zero, quindi per qualche fotogramma lo sfondo
     * tornerebbe nitido. La finestra nuova sfoca subito al pieno, e segue il proprio avanzamento
     * solo quando comincia a uscire. Il velo dipinto ha la stessa regola in [AppVeil].
     * Il banco non la misura: la sfocatura la applica il gestore delle finestre, che su una
     * macchina senza schermo non risponde. Si guarda sul telefono.
     */
    val eredita = remember { VeilStage.dose > 0f }
    /*
     * ⚠️ **E una finestra che esce mentre un'altra superficie è in scena tiene la sfocatura
     * piena** (nota dell'utente su `4.45-01`): la finestra nuova si aggiunge qualche fotogramma
     * dopo la sua composizione, e in quei fotogrammi lo sfondo tornerebbe nitido. La finestra che
     * esce tiene la sfocatura finché la scena chiede più velo della sua quota, cosa che può fare
     * solo un'altra superficie. Neanche questo è misurato sul banco; la decisione sì, in
     * [blurHeld].
     */
    if (velo != null) {
        LaunchedEffect(velo) {
            var piena = eredita
            var prima = 0f
            snapshotFlow { Triple(misura().coerceIn(0f, PIENO), quota().coerceIn(0f, PIENO), VeilStage.dose) }
                .collect { (it, mia, scena) ->
                    if (it < prima) piena = false
                    prima = it
                    velo.at(if (piena || blurHeld(mia, scena)) PIENO else it)
                }
        }
    }
    DisposableEffect(velo) { onDispose { velo?.off() } }
}

/**
 * Se una finestra col proprio avanzamento a [own] tiene la sfocatura piena perché la scena, a
 * [stage], chiede più velo della sua quota: un'altra superficie è in scena, e la sfocatura non
 * deve calare prima che la finestra di quella superficie sia disegnata. Vedi la nota in
 * [WindowVeil].
 *
 * Il margine impedisce a una finestra che si chiude da sola di restare per un arrotondamento.
 */
internal fun blurHeld(own: Float, stage: Float): Boolean = own < PIENO && stage > own * VEIL_DOSE + HOLD_MARGIN

/** Un millesimo di velo: l'arrotondamento di due float, non una quantità visibile. */
private const val HOLD_MARGIN = 0.001f

/**
 * Il **livello sovrapposto**: quanta patina scura chiede alla scena chi lo scrive.
 *
 * ⚠️ **È fuori dalla finestra che lo chiede, ed è la differenza con [WindowVeil]**: quella
 * parla alla finestra che la ospita e si chiude con lei; questo parla a [VeilStage], che vive
 * nella finestra dell'attività e resta quando un menu si chiude. Così la patina si scioglie
 * dopo che il pannello è sparito. Vedi la nota in fondo a questo file.
 *
 * ⚠️ **Il chiamante deve scriverlo fuori dal proprio `Popup` o `Dialog`**, o si chiude con la
 * finestra e la coda si taglia. In `MenuShell` è prima del cancello che apre il `Popup`.
 */
@Composable
fun AppPatina(quanto: () -> Float) {
    val on = LocalAivDepth.current == PanelDepth.BLUR
    val dark = !LocalAivLight.current
    val misura by rememberUpdatedState(quanto)
    val chiave = remember { Any() }
    val dim = if (on) VEIL_DOSE else 0f
    val colore = veilInk(dark)
    LaunchedEffect(chiave, dim, colore) {
        // A funzione spenta la richiesta si toglie: l'interruttore può spegnersi mentre questo è
        // in scena, e la richiesta di prima resterebbe nella mappa, con lo schermo velato.
        if (dim <= 0f) return@LaunchedEffect VeilStage.off(chiave)
        snapshotFlow { misura().coerceIn(0f, PIENO) }.collect { VeilStage.at(chiave, dim * it, dim, colore) }
    }
    DisposableEffect(chiave) { onDispose { VeilStage.off(chiave) } }
}

/**
 * Che cosa c'è dietro una superficie che si apre: la sfocatura, l'ombra oppure niente.
 *
 * Un `CompositionLocal` e non un parametro: lo chiedono i dialoghi, i menu e le schede, che non
 * ricevono le impostazioni. Lo mette in scena l'attività, che le ha già lette per il tema.
 * Il nodo lo legge quando si attacca, cioè quando la finestra si apre; l'impostazione è in una
 * schermata, quindi non cambia mentre un dialogo è aperto.
 *
 * Il valore di serie è il più prudente, [PanelDepth.NONE]: finché le impostazioni non sono
 * arrivate non si aggiunge niente. Il valore di fabbrica dell'impostazione è in `Settings`.
 * Al contrario di [LocalView], dentro e fuori da un dialogo il valore è lo stesso, perché viene
 * dall'attività.
 */
val LocalAivDepth = staticCompositionLocalOf { PanelDepth.NONE }

/**
 * Lo stesso velo, agganciato dove il modificatore è attaccato. Vedi la nota in testa.
 *
 * ⚠️ **Non è un `@Composable` che ritorna un `Modifier`**: quello sarebbe eseguito dove la riga
 * è scritta, fuori dal dialogo, e leggerebbe la finestra dell'attività. Un nodo legge i suoi
 * `CompositionLocal` dalla posizione in cui è attaccato, dentro il dialogo.
 */
fun Modifier.veiled(): Modifier = this then VeilElement

private object VeilElement : ModifierNodeElement<VeilNode>() {
    override fun create() = VeilNode()
    override fun update(node: VeilNode) = Unit
    override fun hashCode() = "veiled".hashCode()
    override fun equals(other: Any?) = other === this
    override fun InspectorInfo.inspectableProperties() {
        name = "veiled"
    }
}

private class VeilNode : Modifier.Node(), CompositionLocalConsumerModifierNode {
    private var velo: Veil? = null

    /*
     * Qui il velo è pieno e basta: i dialoghi di Material compaiono di colpo, senza un
     * avanzamento da seguire. Chi ne ha uno passa da [WindowVeil].
     * ⚠️ **Deve essere sincrono, qui e non in una coroutine**: questo metodo gira mentre il
     * dialogo si compone, prima del suo primo fotogramma, quindi il velo di sistema (0,6) non si
     * vede mai; rimandato anche di un giro, si vedrebbe. Quel velo si azzera, perché il nostro lo
     * dipinge l'app, e due veli sovrapposti scurirebbero lo sfondo oltre il valore approvato.
     * Misura scartata: far salire il velo del dialogo con un'animazione partiva da sotto il velo
     * di sistema, e schiariva lo sfondo prima di scurirlo.
     */
    override fun onAttach() {
        if (currentValueOf(LocalAivDepth) != PanelDepth.BLUR) return
        val view = currentValueOf(LocalView)
        val dark = !currentValueOf(LocalAivLight)
        val radius = with(currentValueOf(LocalDensity)) { BLUR.roundToPx() }
        velo = veilFor(view, on = true, bare = 0f, radius = radius)?.also { it.at(PIENO) }
        // La patina il nodo la chiede da sé a [VeilStage]: [Veil] parla alla sola finestra.
        VeilStage.at(this, VEIL_DOSE, VEIL_DOSE, veilInk(dark))
    }

    override fun onDetach() {
        velo?.off()
        velo = null
        VeilStage.off(this)
    }
}

/**
 * Quanto velo e quanta sfocatura vuole questa finestra, e chi glieli stende.
 *
 * Il calcolo è qui e la stesura in [Veil], perché il velo semplice (la scheda a funzione spenta)
 * vuole la stesura senza il calcolo: non chiede la sfocatura, e la quantità la dice chi chiama.
 */
private fun veilFor(
    view: View,
    on: Boolean,
    bare: Float,
    radius: Int
): Veil? =
    when {
        on -> Veil(
            view = view,
            dim = VEIL_DOSE,
            radius = if (blurs(view)) radius else null,
            dipinto = true
        )
        /*
         * L'unico velo che resta di finestra, a funzione spenta: la scheda in fondo se lo chiede
         * perché la sua finestra non ne ha uno di serie (`SHEET_DIM` in `Sheet.kt`). Là non ci
         * sono due finestre in staffetta, quindi nessun lampo, e dipingerlo accenderebbe il
         * rettangolo dell'app a funzione spenta.
         */
        bare > 0f -> Veil(view, dim = bare, radius = null, dipinto = false)
        else -> null
    }

/**
 * Se questo telefono, adesso, la sfocatura la fa.
 *
 * ⚠️ **Si chiede al sistema**: da Android 12 il telefono può dire di no in qualunque momento
 * (risparmio energetico, hardware che non la fa), e `isCrossWindowBlurEnabled` è la domanda
 * giusta.
 */
private fun blurs(view: View): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        view.context.getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled == true

/**
 * Di che colore è il velo: nero sul tema chiaro, bianco su quello scuro (nota B dell'utente sul
 * giro della `4.43`; 'tema chiaro' scritto due volte è letto come scuro per il secondo, lettura
 * dichiarata nella voce di collaudo). Sul tema scuro i pannelli sono scuri, e uno sfondo
 * schiarito li stacca.
 * Misura scartata: il velo nero sui due temi, che col vetro della pillola diventava l'accento al
 * 10%, e i pannelli nudi non staccavano.
 */
private fun veilInk(dark: Boolean): Color = if (dark) Color.White else VEIL_INK

/**
 * Quanto copre il velo pieno: il 30% sui due temi, con o senza sfocatura (richiesta
 * dell'utente, 4.44).
 *
 * Misure scartate: 0,20 sul chiaro e 0,45 sullo scuro, più 0,12 dove il telefono non sfoca, e
 * 0,10 d'accento col vetro della pillola. Il velo di serie di Material sui dialoghi è 0,6.
 */
internal const val VEIL_DOSE = 0.30f

/**
 * Il velo **dipinto dall'app**: quanto ne vuole chi, fra le superfici in scena, ne chiede di più.
 *
 * ⚠️ **Il velo non è un attributo di finestra perché due finestre non si aggiornano nello stesso
 * fotogramma** (voce `flash-menu-dialogo`): fra il `setDimAmount` del dialogo e
 * l'`updateViewLayout` del menu passa almeno un fotogramma, in cui i veli sono due (più scuro) o
 * nessuno (più chiaro), e nessuna sequenza di chiamate lo rende atomico. Misure scartate:
 * spegnere il velo del menu prima del dialogo (lampo chiaro), farlo calare mentre l'altro sale
 * (i due si sommano, lampo scuro), spegnerlo nella stessa chiamata (il fotogramma resta).
 *
 * Quindi il velo è un rettangolo che l'app dipinge ([AppVeil], messo in scena da `AivTheme`):
 * uno solo, nella finestra dell'attività, che attraversa la transizione cambiando solo opacità.
 * Vale il massimo delle richieste, non la somma: due superfici in scena danno lo stesso velo di
 * una.
 *
 * La sfocatura resta di finestra (vedi il fondo di questo file): nel passaggio da menu a
 * dialogo le due finestre la chiedono insieme per il tempo dell'uscita del menu
 * (`MENU_OUT_MS`), e lo sfondo diventa un filo più sfocato e torna. È un cambio di nitidezza, non
 * di luminosità, e l'occhio non lo legge come un lampo.
 *
 * A funzione spenta la mappa è vuota: menu e dialoghi non creano veli ([veilFor]), e la scheda in
 * fondo chiede il suo velo semplice alla propria finestra.
 */
internal object VeilStage {
    /*
     * Una mappa di richiedenti e non un numero: le superfici se ne vanno in un ordine qualunque,
     * e con un numero solo la seconda che esce toglierebbe anche il velo della prima.
     */
    private val chiedono = mutableStateMapOf<Any, Richiesta>()

    /**
     * Una richiesta: quanto velo, quanto ne vorrebbe a pieno, e di che colore.
     *
     * Il pieno viaggia con la dose: oggi è [VEIL_DOSE] per tutti, e [veilProgress] lo usa per dire
     * a che punto è la transizione senza conoscere il numero.
     */
    data class Richiesta(val dose: Float, val pieno: Float, val colore: Color, val sale: Boolean)

    private val piu: Richiesta? get() = chiedono.values.maxByOrNull { it.dose }

    /** Quanto velo si dipinge adesso. Si legge nella fase di disegno, non in composizione. */
    val dose: Float get() = piu?.dose ?: 0f

    /** Di che colore è il velo che si dipinge adesso: quello della richiesta più forte. */
    val colore: Color get() = piu?.colore ?: VEIL_INK

    /** Il pieno della richiesta più forte: dove il velo arriva quando [AppVeil] ha finito di salire. */
    val pieno: Float get() = piu?.pieno ?: 0f

    /** A che punto è la transizione del velo, da 0 a 1. Vedi [veilProgress]. */
    val avanzamento: Float
        get() = chiedono.values.maxOfOrNull { if (it.pieno > 0f) it.dose / it.pieno else 0f } ?: 0f

    /**
     * Se c'è una superficie che sta entrando, cioè la cui dose è salita all'ultima richiesta.
     *
     * Serve ad [AppVeil] per non scendere mentre un'altra superficie esce (vedi la nota là). Una
     * richiesta ferma al pieno resta 'in salita' finché non comincia a calare: fino ad allora la
     * superficie è in scena.
     */
    val sale: Boolean get() = chiedono.values.any { it.sale }

    fun at(chi: Any, quanto: Float, pieno: Float, colore: Color) {
        if (quanto <= 0f) {
            chiedono.remove(chi)
            return
        }
        val prima = chiedono[chi]
        val sale = prima == null || quanto > prima.dose || (quanto == prima.dose && prima.sale)
        chiedono[chi] = Richiesta(quanto, pieno, colore, sale)
    }

    fun off(chi: Any) {
        chiedono.remove(chi)
    }

    /**
     * La rete contro il velo fantasma: se la composizione dell'app se ne va con una superficie
     * aperta, il suo `off` non arriva più, e lo schermo resterebbe velato. Lo chiama [AppVeil]
     * quando esce di scena.
     */
    fun clear() {
        chiedono.clear()
    }
}

/**
 * Il velo, dipinto sopra il contenuto dell'app. Lo mette in scena `AivTheme`, una volta sola.
 *
 * Non intercetta i tocchi: Compose fa la prova del tocco solo sui nodi con un modificatore di
 * puntatore, e questo ha solo un modificatore di disegno (come la fascia sfumata della schermata
 * iniziale).
 * La dose si legge dentro il disegno: un velo che sale o scende costa un ridisegno, non una
 * ricomposizione.
 * È sopra tutto quello che l'app disegna, FAB compreso: sopra deve restare solo la superficie
 * aperta, che vive in un'altra finestra.
 */
@Composable
fun AppVeil(modifier: Modifier = Modifier) {
    DisposableEffect(Unit) { onDispose { VeilStage.clear() } }
    /*
     * Il velo sale per conto suo in [VEIL_IN_MS] (note B e C dell'utente sul giro della `4.43`):
     * pannello e sfocatura compaiono col loro tempo, e il velo parte con loro e arriva al pieno
     * della richiesta più forte con [VEIL_IN_EASING].
     * Sale verso il pieno e non verso la dose del momento, che cresce a ogni fotogramma
     * dell'entrata del pannello: inseguirla farebbe ripartire la salita ogni volta.
     * ⚠️ **Scende subito**: appena la dose richiesta è sotto quella dipinta, il velo la segue
     * fotogramma per fotogramma. Un velo che resta dopo il pannello si legge come uno schermo
     * bloccato.
     * ⚠️ **Ma non scende mentre un'altra superficie sta entrando** ([VeilStage.sale]): dal menu
     * di una miniatura a 'Info' la dose più forte cala per qualche fotogramma, e il velo
     * scenderebbe quasi a zero per poi risalire (nota dell'utente su `4.44-02`). Presidiato da
     * `VeloTest`.
     */
    val dipinto = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        var salita: Job? = null
        var meta = -1f
        snapshotFlow { Triple(VeilStage.dose, VeilStage.pieno, VeilStage.sale) }.collect { (dose, pieno, sale) ->
            if (dose <= 0f || (dose < dipinto.value && !sale)) {
                salita?.cancel()
                meta = -1f
                dipinto.snapTo(dose)
            } else if (pieno != meta) {
                meta = pieno
                salita?.cancel()
                salita = launch { dipinto.animateTo(pieno, tween(VEIL_IN_MS, easing = VEIL_IN_EASING)) }
            }
        }
    }
    Box(
        modifier = modifier.drawBehind {
            val quanto = dipinto.value
            if (quanto > 0f) drawRect(color = VeilStage.colore, alpha = quanto)
        }
    )
}

/** Quanto ci mette il velo a comparire: il numero dell'utente. */
internal const val VEIL_IN_MS = 800

/**
 * La curva con cui il velo compare: parte veloce e arriva sfumando (nota C dell'utente). Misurata
 * sulla curva: 29% dopo un decimo del tempo, 61% dopo un quarto, 88% a metà, 98% a tre quarti,
 * quindi la salita si vede per quasi tutto il tempo.
 * Curva scartata: l'esponenziale `(0,16; 1; 0,3; 1)`, già al 97% a metà tempo, che fa sembrare
 * il velo arrivato in 200 ms.
 */
private val VEIL_IN_EASING: Easing = CubicBezierEasing(0.2f, 0.6f, 0.35f, 1f)

/**
 * Di che colore è il velo.
 *
 * Nero sul tema chiaro, come quello di Android; sul tema scuro è bianco ([veilInk]).
 */
private val VEIL_INK = Color.Black

/**
 * Il velo steso sulla finestra di [view]: si dosa con [at] e si toglie con [off].
 *
 * ⚠️ **Dosabile**: [dim] e [radius] sono il pieno, e alla finestra va la loro frazione. Così la
 * sfocatura cresce e cala col pannello: tutta subito coprirebbe al primo fotogramma il
 * rettangolo pieno della finestra mentre il pannello è ancora piccolo e trasparente, e quel
 * rettangolo coi bordi sfumati si vedrebbe come una cornice (riscontro dell'utente).
 *
 * Si aggancia alla prima chiamata e non alla costruzione: durante la composizione i
 * `LayoutParams` della radice di un `Popup` possono non essere ancora quelli del gestore, e
 * letti troppo presto darebbero un velo che non si applica, in silenzio.
 */
private class Veil(
    private val view: View,
    private val dim: Float,
    private val radius: Int?,
    /**
     * Se il velo lo dipinge l'app ([VeilStage]) invece della finestra.
     *
     * Quando è vero, alla finestra si chiede la sola sfocatura, e il suo velo va a zero: su un
     * dialogo quello di serie è 0,6, e sommato al nostro scurirebbe lo sfondo oltre il valore
     * approvato. Azzerarlo qui è sincrono, quindi non si vede.
     */
    private val dipinto: Boolean
) {
    private var window: Window? = null
    private var manager: WindowManager? = null
    private var params: WindowManager.LayoutParams? = null
    private var dimPrima = 0f
    private var flagsPrima = 0
    private var acceso = false

    /** Aggancia la finestra e ricorda com'era. Falso se qui non c'è niente da velare. */
    private fun grab(): Boolean {
        if (acceso) return true
        manager = view.context.getSystemService(WindowManager::class.java)
        window = view.dialogWindow()
        val w = window
        if (w != null) {
            dimPrima = w.attributes.dimAmount
        } else {
            /*
             * ⚠️ **Un `Popup` è una finestra, ma non ha un `Window`**: Compose lo aggiunge al
             * `WindowManager` come vista, quindi i suoi parametri sono i `LayoutParams` della
             * radice di questo albero, e si applicano con `updateViewLayout`. È la sola via per
             * velare un menu.
             */
            val p = view.rootView.layoutParams as? WindowManager.LayoutParams ?: return false
            if (manager == null) return false
            params = p
            flagsPrima = p.flags
            dimPrima = p.dimAmount
        }
        acceso = true
        return true
    }

    /**
     * Il dosaggio chiesto alla finestra. La patina non passa di qui: la chiede [AppPatina] a
     * [VeilStage], da fuori dalla finestra.
     */
    fun at(q: Float) {
        if (!grab()) return
        stendi(q)
    }

    private fun stendi(q: Float) {
        // A velo dipinto alla finestra si chiede la sola sfocatura (vedi [dipinto]).
        val velo = if (dipinto) 0f else dim * q
        val raggio = radius?.let { (it * q).roundToInt() } ?: 0
        val w = window
        if (w != null) {
            // Su un dialogo si passa dalla finestra e non dai suoi parametri: è lei che li
            // riapplica, e `setDimAmount` accende già il flag che serve.
            w.setDimAmount(velo)
            if (radius != null) {
                w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                w.attributes = w.attributes.apply { blurBehindRadius = raggio }
            }
            return
        }
        // Un menu a velo dipinto e senza sfocatura non chiede niente alla sua finestra: sui
        // telefoni che non sfocano risparmia un `updateViewLayout` a ogni fotogramma.
        if (dipinto && radius == null) return
        val p = params ?: return
        val m = manager ?: return
        p.flags = p.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        p.dimAmount = velo
        if (radius != null) {
            p.flags = p.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            p.blurBehindRadius = raggio
        }
        runCatching { m.updateViewLayout(view.rootView, p) }
    }

    fun off() {
        if (!acceso) return
        acceso = false
        val w = window
        if (w != null) {
            w.setDimAmount(dimPrima)
            if (radius != null) {
                w.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                // Anche il raggio torna a zero: con la bandierina spenta non si vede, ma
                // chi la riaccendesse ritroverebbe una sfocatura che nessuno ha chiesto.
                w.attributes = w.attributes.apply { blurBehindRadius = 0 }
            }
            return
        }
        val p = params ?: return
        val m = manager ?: return
        p.flags = flagsPrima
        p.dimAmount = dimPrima
        if (radius != null) p.blurBehindRadius = 0
        // La vista può essere già staccata, e allora il gestore va in errore: su una finestra
        // che non c'è più non c'è niente da rimettere a posto.
        runCatching { m.updateViewLayout(view.rootView, p) }
    }
}

/**
 * La finestra del dialogo che ospita questa vista, se è un dialogo.
 *
 * Si risale tutta la catena: la scorciatoia comune (`view.parent as DialogWindowProvider`) vale
 * finché Compose tiene quell'esatto annidamento, e un livello in più la farebbe fallire in
 * silenzio, senza velo e senza errore.
 */
fun View.dialogWindow(): Window? {
    var nodo = parent
    while (nodo != null) {
        (nodo as? DialogWindowProvider)?.let { return it.window }
        nodo = (nodo as? View)?.parent
    }
    return null
}

/**
 * Quanto è sfocato lo sfondo.
 *
 * 'Leggera', come ha chiesto l'utente: si legge che c'è qualcosa dietro senza cancellarlo. Il
 * doppio trasformerebbe lo sfondo in una macchia di colore.
 * In dp, anche se l'API vuole pixel: un raggio fisso in pixel sfocherebbe diversamente su ogni
 * densità di schermo.
 */
private val BLUR = 12.dp

/**
 * Quanto è avanzato il velo, da 0 a 1, indipendentemente da quanto scuro sia.
 *
 * Serve a chi deve cambiare disegno insieme alla sfocatura, come la scacchiera della
 * trasparenza, che con la sfocatura fa un brutto effetto ottico (riscontro dell'utente). Il velo
 * dice quanto buio c'è; questo dice a che punto è la transizione.
 * Si divide per la dose piena di ogni richiesta ([VeilStage.Richiesta]), così l'avanzamento non
 * dipende dal numero del velo.
 * ⚠️ **Si legge nella fase di disegno**, come [AppVeil]: in composizione farebbe ricomporre chi
 * lo guarda a ogni fotogramma della transizione.
 */
internal fun veilProgress(): Float =
    VeilStage.avanzamento.coerceIn(0f, 1f)


/**
 * Il velo al massimo: quello che chiede chi non ha un'animazione da seguire.
 *
 * I dialoghi di Material compaiono di colpo e il loro velo vale sempre questo; chi ha
 * un'animazione passa il suo avanzamento.
 */
private const val PIENO = 1f

/*
 * ── La coda: dove si scioglie, dove non serve, e che cosa non può ────────────
 *
 * ⚠️ **Ridurre un raggio di sfocatura si legge come una messa a fuoco** (riscontro dell'utente),
 * se cala con lo schermo vuoto. Una sfocatura che cala vuole in scena qualcosa che se ne sta
 * andando: sulla scheda in fondo, che resta opaca per due terzi della sua uscita, la stessa
 * curva è stata giudicata perfetta.
 *
 * Quindi l'uscita di un menu è una dissolvenza sola di `MENU_OUT_MS`, in cui pannello,
 * sfocatura e patina se ne vanno insieme (riscontro dell'utente sul giro della `1.66`). La coda
 * ([VEIL_FADE_MS]) resta solo sulla scheda in fondo, dove l'uscita dura più di lei.
 * ⚠️ Una coda sui menu cadrebbe sulla composizione della schermata nuova quando una voce
 * naviga, col pannello a mezza opacità sopra la schermata d'arrivo: non si vede aprendo e
 * chiudendo un menu sul posto.
 *
 * La finestra di un menu non sopravvive al proprio pannello.
 * ⚠️ **Un `PopupProperties` che cambia durante un'animazione fa sfarfallare la sfocatura**
 * (misurato sul bytecode di Compose): `PopupLayout.updatePopupProperties` assegna `params.flags`
 * dalle sole `PopupProperties` (`flagsWithSecureFlagInherited`), quindi per un fotogramma la
 * finestra perde `FLAG_BLUR_BEHIND` e `FLAG_DIM_BEHIND`, finché `stendi` li rimette. Nessun errore
 * lo segnala.
 *
 * All'entrata patina e sfocatura crescono col pannello, con la sua durata e la sua curva: così
 * nessun fotogramma sfoca più di quanto il pannello sia in scena.
 */

/**
 * Quanto ci mette la patina a sciogliersi, quando la superficie se ne è andata.
 *
 * Lo usa solo la scheda in fondo, dove l'uscita dura già più di lui (`USCITA_MS` in `Sheet.kt`);
 * sui menu non c'è coda, per la ragione scritta nel blocco qui sopra.
 */
internal const val VEIL_FADE_MS = 280

/**
 * La curva con cui si scioglie: arriva con pendenza **nulla**, cioè decelerando.
 *
 * Il secondo punto `(0.25, 1)` dà la pendenza nulla all'arrivo, il finale lento chiesto
 * dall'utente. Su un'opacità la curva si percepisce quasi come si scrive; su un raggio no (fra 33
 * e 25 pixel non si vede differenza, fra 4 e 0 c'è tutto il passaggio), ed è uno dei motivi per
 * cui il raggio non si anima in uscita.
 */
internal val VEIL_FADE: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

/*
 * ── Il limite che resta, e il rifacimento che lo toglierebbe ──────────────────
 *
 * ⚠️ **Una sfocatura di finestra non può seguire un riquadro stondato** (voce
 * `sfocatura-segue`). La finestra è grande quanto il pannello (vedi `Growing` in `Menus.kt`),
 * quindi lungo i lati non resta niente, ma agli angoli il pannello è stondato di `MENU_ROUND` e
 * la finestra è quadrata: in ogni angolo sporge un triangolino di sfondo sfocato largo al massimo
 * `r * (1 - 1 / radice(2))`, meno di sei dp. `WindowManager.LayoutParams` dichiara un raggio di
 * sfocatura, non una forma. È un difetto fermo, che si vede guardando un angolo del pannello
 * sopra una miniatura molto contrastata.
 *
 * La via che lo toglierebbe è un rifacimento: dipingere la sfocatura dentro il pannello con un
 * `RenderEffect` ritagliato sulla sua forma. Toglierebbe gli angoli e gli `updateViewLayout`
 * per fotogramma, ma vuole che l'app si disegni in un `GraphicsLayer` da cui campionare quello
 * che c'è sotto, e dalla finestra di un menu i pixel dell'app non si leggono. I menu dovrebbero
 * smettere di essere finestre, perdendo l'ordine sopra il FAB e la chiusura con Indietro e col
 * tocco fuori, che oggi `Popup` dà da sé.
 */

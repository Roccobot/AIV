package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/*
 * L'intestazione: la fascia in cima che si chiude scorrendo, e la sfumatura in fondo che le fa da
 * controparte. È in un file suo perché la usano due schermate, la schermata iniziale e la griglia
 * di una cartella: la frazione, l'altezza della fascia, la curva della sfumatura e la sua coda sono
 * una decisione sola per l'app, e copiarne i numeri farebbe due tavolozze che divergono.
 */

/**
 * Quanta parte dello schermo tiene l'intestazione da aperta: un terzo scarso.
 *
 * ⚠️ **È una misura di portata del pollice, non una proporzione estetica**: l'intestazione tiene le
 * cartelle in basso, in stile OneUI, quindi abbassarla rimette le cartelle fuori tiro e alzarla
 * toglie righe. Vale per le due viste della schermata iniziale e per la griglia di una cartella,
 * che la leggono da qui.
 * ⚠️ **34 e non 40, misurato**: col 40% l'area della griglia scende a 484 dp sullo schermo
 * dell'utente, e la fascia sfumata in fondo (216 dp) velerebbe il conto della seconda riga di
 * cartelle. Col 34% la griglia sale a 533 dp e la sfumatura comincia dove comincia la terza riga.
 * Il cambio l'ha autorizzato l'utente.
 */
const val HEADER_SHARE = 0.34f

/**
 * Quanto è larga l'icona dell'intestazione: più grande di quella delle impostazioni, perché qui
 * accoglie.
 */
val HEADER_ICON = 96.dp

/**
 * Quanto è aperta l'intestazione, da 0 (chiusa) a 1.
 *
 * La formula è in un posto solo perché la leggono in tre: la fascia, per sbiadire il contenuto; la
 * testata di una cartella, per far comparire il titolo; e la sfumatura in fondo. È una funzione
 * pura perché si legge in fase di disegno, dentro un `graphicsLayer`, dove un valore nuovo costa un
 * ridisegno e non una ricomposizione.
 */
fun frontOpen(fullPx: Float, chiuso: Float): Float =
    if (fullPx > 0f) (1f - chiuso / fullPx).coerceIn(0f, 1f) else 0f

/**
 * La fascia dell'intestazione: alta [fullPx] da aperta, e alta quel che resta mentre si chiude.
 *
 * ⚠️ **Il figlio si misura sempre all'altezza piena e si ritaglia, non si schiaccia**: misurato con
 * l'altezza che resta, l'icona si deformerebbe. Si misura intero, si dichiara alto quel che resta e
 * si colloca centrato in quel che resta: il contenuto sale da sé mentre lo spazio si stringe, ed è
 * la parallasse. È anche la traslazione del titolo di una cartella verso la testata chiesta
 * dall'utente: una seconda traslazione aggiunta sopra darebbe due movimenti sullo stesso oggetto.
 *
 * Lo stato si legge dentro `layout` e nel `graphicsLayer` del contenuto, cioè in misura e in
 * disegno: il trascinamento non fa ricomporre niente, e sotto c'è una griglia.
 *
 * @param aperto quanto è aperta la fascia, da 0 (chiusa) a 1: si legge nel `graphicsLayer` del
 *   contenuto, dove l'opacità non costa una ricomposizione.
 */
@Composable
fun FrontBand(
    fullPx: Float,
    shut: () -> Float,
    content: @Composable (aperto: () -> Float) -> Unit
) {
    val aperto = remember(fullPx) { { frontOpen(fullPx, shut()) } }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .layout { measurable, constraints ->
                val full = fullPx.roundToInt().coerceAtLeast(0)
                val left = (fullPx - shut()).roundToInt().coerceIn(0, full)
                val placeable = measurable.measure(
                    constraints.copy(minHeight = full, maxHeight = full)
                )
                layout(placeable.width, left) { placeable.place(0, -(full - left) / 2) }
            },
        contentAlignment = Alignment.Center
    ) {
        content(aperto)
    }
}

/**
 * Quanto rientra il bordo di sotto dello schermo: la barra gestuale, o zero dove non c'è.
 *
 * Le griglie arrivano al vetro (richiesta dell'utente): le due schermate non mettono il rientro di
 * sotto sul contenitore, che impedirebbe di disegnare là sotto, ma nel `contentPadding` della loro
 * lista. Così le miniature scorrono sotto la barra gestuale e l'ultima riga resta raggiungibile. Il
 * rientro è quello di [steadyDrawing], fermo mentre le barre compaiono: comprende anche il ritaglio
 * del display e la tastiera.
 */
@Composable
fun bottomInset(): Dp = steadyDrawing().asPaddingValues().calculateBottomPadding()

/**
 * Le due sfumature in fondo allo schermo, che coprono quello che gli scorre sotto.
 *
 * La fascia grande parte dal FAB (richiesta dell'utente): suggerisce che la griglia scorre e
 * sostituisce una riga tagliata di netto dal bordo. Nella schermata iniziale a riposo non copre
 * niente, perché lo spazio riservato ([BELOW_FAB]) tiene l'ultima cartella sopra di lei.
 * ⚠️ **Una curva e non quattro fermate**: una rampa dritta si legge come un bordo sfocato, per i
 * due spigoli dove la salita comincia e finisce; [smoothstep] parte e arriva con pendenza zero. Le
 * fermate si calcolano, e le sole manopole sono [GRADIENT_TIMES] e [GRADIENT_PEAK].
 *
 * Il secondo strato, la coda, chiude l'ultima striscia, che la fascia grande lascia a sei decimi:
 * là passano il bordo stondato del vetro e la barra di sistema. È uno strato e non una fermata in
 * più, perché la curva della fascia grande tiene il suo massimo nell'ultimo terzo, e per finire
 * piena sul bordo dovrebbe avere due massimi; due strati si sommano da soli.
 *
 * Il colore è `background`, quello che la `Surface` del tema mette dietro a tutta l'app (vedi
 * `AivTheme`). Non prende i tocchi: Compose fa la prova del tocco solo sui nodi con un modificatore
 * di puntatore, e qui non ce n'è.
 *
 * I due strati hanno un'opacità ciascuno, e i massimi arrivano da chi chiama (voce `4.30-07`): in
 * casa la fascia grande al 60% e la coda all'85%; in una cartella la sola fascia grande, un po' più
 * opaca, che se ne va quando la griglia tocca il fondo. Curve e altezze sono una sola, qui.
 *
 * @param alpha quanto si vede la fascia grande, da 0 a 1; il valore di serie è la schermata
 *   iniziale.
 * @param footAlpha quanto si vede la coda, o `null` dove la coda non c'è (le cartelle).
 * @param peak l'opacità massima della fascia grande: [GRADIENT_PEAK] in casa, di più in una
 *   cartella.
 * @param footPeak l'opacità massima della coda.
 */
@Composable
fun GroundFade(
    modifier: Modifier = Modifier,
    alpha: () -> Float = { 1f },
    footAlpha: (() -> Float)? = { 1f },
    peak: Float = GRADIENT_PEAK,
    footPeak: Float = HOME_FOOT_PEAK
) {
    val ground = MaterialTheme.colorScheme.background
    /*
     * Le due rampe si posano col rumore: sopra un'immagine i livelli che una rampa attraversa sono
     * quelli fra il fondo e l'immagine, e un gradino viene alto una dozzina di pixel. Il conto è in
     * testa a `Dither.kt`.
     */
    val ramp = remember(peak) {
        GrainedRamp(List(GRADIENT_STOPS + 1) { step -> (step / GRADIENT_STOPS.toFloat()).let { it to swallow(it) } }, peak)
    }
    val piede = remember(footPeak) {
        GrainedRamp(List(FOOT_STOPS + 1) { step -> (step / FOOT_STOPS.toFloat()).let { it to foot(it) } }, footPeak)
    }
    /*
     * Un'opacità per strato, perché se ne vanno in momenti diversi: quando sono tutte e due a metà
     * la loro somma non segue un numero solo, ed è il prezzo dichiarato.
     */
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(GRADIENT_REACH)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(GRADIENT_REACH)
                .graphicsLayer { this.alpha = alpha() }
                .drawBehind { ramp.paint(this, ground, 0f, size.height) }
        )
        // La coda viene dopo la fascia grande, quindi è sopra: in un `Box` l'ultimo figlio si
        // disegna per ultimo, e la coda riporta al pieno quello che la fascia lascia a sei decimi.
        if (footAlpha != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(FOOT_REACH)
                    .graphicsLayer { this.alpha = footAlpha() }
                    .drawBehind { piede.paint(this, ground, 0f, size.height) }
            )
        }
    }
}

/**
 * Quante volte il FAB è alta la fascia dipinta: due e mezzo, tarato dall'utente a occhio.
 *
 * ⚠️ **Dipingere più in alto non costa altezza alla griglia**: lo spazio riservato resta
 * [BELOW_FAB], e questa dice soltanto fin dove arriva il colore.
 */
private const val GRADIENT_TIMES = 2.5f

/**
 * L'opacità massima della fascia grande, quella che tiene dal FAB in giù: sei decimi, per scelta
 * dell'utente. Il prezzo è dichiarato: un'immagine molto contrastata si intravede sotto il FAB.
 * L'ultima striscia la riporta al pieno la coda ([FOOT_REACH]).
 */
private const val GRADIENT_PEAK = 0.60f

/** L'opacità massima della coda in casa: l'85% (voce `4.30-07`). */
private const val HOME_FOOT_PEAK = 0.85f

/**
 * In quanti gradini si disegna la curva della sfumatura: con dodici, su una fascia così lunga, ogni
 * gradino sarebbe alto una quindicina di dp e si distinguerebbe su un fondo chiaro.
 */
private const val GRADIENT_STOPS = 20

/**
 * Quanto è alta la fascia dipinta sopra il FAB. Non è privata perché la legge il banco di prova,
 * che deve sapere se un tocco cade dentro la sfumatura senza ricopiare il numero.
 */
internal val GRADIENT_REACH = FAB_REACH * GRADIENT_TIMES

/**
 * Quanto è alta la coda che chiude in pieno l'ultima striscia di schermo: il numero è dell'utente.
 *
 * ⚠️ **Quello che si vede è la lunghezza del tratto in cui la copertura cresce**, non il pieno sul
 * bordo: una coda di 8 dp era giusta sul profilo misurato e invisibile a occhio. Sotto una certa
 * lunghezza una dissolvenza non si legge come tale. Ha la cima più in alto da quando la griglia
 * arriva al vetro, perché là sotto passano le miniature e non più il fondo dell'app.
 */
private val FOOT_REACH = 55.dp

/**
 * Quanto dura il pieno in fondo alla coda, dal bordo dello schermo in su: il numero è dell'utente.
 *
 * È la forma di [swallow], che sale e poi tiene il suo massimo: un massimo toccato in una riga di
 * pixel non si legge come un massimo. Il pieno occupa più di metà della coda, e la salita, un
 * raccordo di 23 dp, si ricava per differenza da [FOOT_REACH]; le due misure vanno cambiate
 * insieme.
 */
private val FOOT_SOLID = 32.dp

/**
 * In quanti gradini si disegna la coda: venti, come la fascia grande. I gradini sono i vertici
 * della spezzata che approssima la curva (Compose interpola fra due fermate), e con pochi si
 * vedrebbero gli spigoli.
 */
private const val FOOT_STOPS = 20

/**
 * A che punto della sua altezza la fascia sopra il FAB arriva al suo massimo.
 *
 * Si ricava: il bordo superiore del FAB è a [FAB_REACH] dal fondo, cioè a questa frazione della
 * fascia, e da lì in giù il colore non cresce, quindi il FAB è su un fondo di un colore solo.
 * Alzando la fascia, il tratto a colore fermo resta lo stesso e cresce solo la dissolvenza sopra.
 * Quanto vale quel colore lo dice [GRADIENT_PEAK].
 */
private const val SWALLOW = 1f / GRADIENT_TIMES

/**
 * Quanto colore c'è a una data altezza della fascia, con `0` in cima e `1` sul fondo, come frazione
 * del suo massimo (il `peak` di [GroundFade]). Si sale da niente al massimo fino al bordo del FAB
 * ([SWALLOW]), e da lì in giù il colore è fermo.
 */
private fun swallow(at: Float): Float {
    val fermo = 1f - SWALLOW
    if (at >= fermo) return 1f
    return smoothstep(at / fermo)
}

/**
 * Quanto colore c'è a una data altezza della coda, con `0` in cima e `1` sul bordo di sotto. Ha un
 * pianoro come [swallow]: si sale fino al pieno, e negli ultimi [FOOT_SOLID] il colore è fermo. Il
 * tratto in salita si ricava dalle due misure.
 */
private fun foot(at: Float): Float {
    val sale = 1f - FOOT_SOLID / FOOT_REACH
    if (at >= sale) return 1f
    return smoothstep(at / sale)
}

/** La curva che parte e arriva con pendenza zero, cioè quella che non fa spigoli. */
private fun smoothstep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

/**
 * Lo scorrimento che chiude l'intestazione prima che l'elenco scorra, e la riapre in cima.
 *
 * ⚠️ **Funziona anche con due elementi**, verificato sul sorgente di Compose: il trascinamento
 * verso l'alto è intercettato prima (`onPreScroll`) e speso tutto qui.
 * `ScrollingLogic.performScroll` chiama `dispatchPreScroll` prima di consumare, e l'avvio del
 * trascinamento dipende dal tipo di puntatore (`canDrag`), non dallo spazio da scorrere. La
 * riapertura verso il basso è in `onPreScroll` quando [inCima] è vero (voce `3.13-02`), perché con
 * poche cartelle la griglia non dà un `onPostScroll` utile; `onPostScroll` resta come ripiego a
 * metà gesto.
 *
 * @param quanto quanti pixel di intestazione ci sono in tutto.
 * @param chiuso quanti ne sono già chiusi.
 * @param chiudi dove scrivere il numero nuovo.
 */
fun frontScroll(
    quanto: Float,
    chiuso: () -> Float,
    chiudi: (Float) -> Unit,
    /**
     * Se l'elenco è in cima: serve a riaprire l'intestazione in `onPreScroll` anche quando il
     * contenuto non riempie lo spazio (modalità inclusione, poche cartelle).
     */
    inCima: () -> Boolean = { true }
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y < 0f) {
            val take = (-available.y).coerceAtMost(quanto - chiuso())
            chiudi(chiuso() + take)
            return Offset(0f, -take)
        }
        // Si riapre prima che l'elenco consumi, ma solo in cima: un elenco a metà riaprirebbe la
        // fascia mentre si risale.
        if (available.y > 0f && inCima() && chiuso() > 0f) {
            val give = available.y.coerceAtMost(chiuso())
            chiudi(chiuso() - give)
            return Offset(0f, give)
        }
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        // Ripiego: se `onPreScroll` non ha visto il gesto (elenco arrivato in cima a metà gesto),
        // recupera il residuo.
        if (available.y <= 0f || !inCima()) return Offset.Zero
        val give = available.y.coerceAtMost(chiuso())
        chiudi(chiuso() - give)
        return Offset(0f, give)
    }
}

/**
 * Quanto è lontana l'icona dell'intestazione dal titolo sotto di lei: lo stesso numero della
 * schermata iniziale, perché le due fasce si somigliano.
 */
val FRONT_GAP: Dp = 10.dp

/**
 * Quanto è lontano il conto degli elementi dal nome della cartella, nell'intestazione: meno di
 * [FRONT_GAP], perché nome e numero si leggono insieme, mentre l'icona sopra è un fondale.
 */
val FRONT_COUNT_GAP: Dp = 2.dp

/**
 * Quanto si vede l'icona nell'intestazione di una cartella: diciotto centesimi, scelti dall'utente
 * col telefono in mano. Là l'icona è un fondale dietro il nome della cartella, che è la sola cosa
 * da leggere. Non si moltiplica per l'apertura: l'icona prima si rimpicciolisce (vedi
 * [frontIconMeasure]) e la dissolvenza è in [frontIconInk].
 */
const val FRONT_INK = 0.18f

/**
 * Quanta parte dello spazio verticale libero può prendere l'icona dell'intestazione. È qui perché
 * la leggono il chiamante e [frontIconMeasure].
 */
const val FRONT_ICON_SHARE = 0.5f

/**
 * Da che apertura l'icona dell'intestazione comincia a rimpicciolirsi, e con lei a sbiadire.
 *
 * ⚠️ **Si ricava dalla misura** (voce `front-selezione`: dissolvenza e rimpicciolimento cominciano
 * e finiscono insieme). [frontIconMeasure] tiene il lato pieno finché `libero * FRONT_ICON_SHARE`
 * supera il lato massimo, e questo è quel punto, risolto per l'apertura. Una soglia scritta a mano
 * faceva rimpicciolire l'icona a inchiostro pieno per un tratto. A fascia chiusa lato e frazione
 * sono zero insieme, per costruzione.
 *
 * @param fullPx quanto è alta la fascia da aperta, in pixel.
 * @param maxPx il lato massimo dell'icona, in pixel.
 */
fun frontIconFade(fullPx: Float, maxPx: Float): Float =
    if (fullPx <= 0f) 1f else (maxPx / (fullPx * FRONT_ICON_SHARE)).coerceIn(0f, 1f)

/**
 * Quanto inchiostro ha l'icona dell'intestazione con la fascia aperta di [aperto].
 *
 * La rampa è lineare: una [smoothstep] parte con pendenza zero, e il primo tratto di dissolvenza
 * non si vedrebbe, mentre deve cominciare insieme al rimpicciolimento. Il valore pieno arriva da
 * fuori, perché con il gradiente acceso l'icona cambia inchiostro e la rampa resta la stessa; così
 * la funzione non conosce le impostazioni.
 *
 * @param soglia il punto da cui l'icona si stringe, cioè [frontIconFade].
 * @param pieno l'inchiostro a fascia aperta: [FRONT_INK] di solito, [FRONT_NEG_INK] o
 *   [FRONT_DARK_INK] col gradiente acceso.
 */
fun frontIconInk(aperto: Float, soglia: Float, pieno: Float = FRONT_INK): Float =
    pieno * if (soglia <= 0f) 1f else (aperto / soglia).coerceIn(0f, 1f)

/**
 * L'icona dell'intestazione si misura sullo spazio che la fascia le lascia, a ogni fotogramma.
 *
 * ⚠️ **Una misura e non una scala**: una scala in `graphicsLayer` rimpicciolisce il disegno e
 * lascia il posto occupato, quindi il titolo sotto non salirebbe; misurando, l'icona cede lo spazio
 * che libera, e il nome della cartella lo prende. Si legge in fase di misura, come in [FrontBand]:
 * `shut()` in composizione farebbe ricomporre la schermata a ogni fotogramma. Il lato è quadrato
 * (`Constraints.fixed`), o l'icona si deformerebbe.
 *
 * @param fullPx quanto è alta la fascia da aperta, in pixel.
 * @param shut quanti pixel di fascia sono già chiusi.
 * @param max il lato massimo, cioè quello che l'icona prende a fascia aperta.
 */
fun Modifier.frontIconMeasure(fullPx: Float, shut: () -> Float, max: Dp): Modifier =
    layout { measurable, constraints ->
        val libero = (fullPx - shut()).coerceIn(0f, fullPx)
        val lato = minOf(max.toPx(), libero * FRONT_ICON_SHARE)
            .roundToInt()
            .coerceIn(0, constraints.maxWidth)
        val placeable = measurable.measure(Constraints.fixed(lato, lato))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

/**
 * Il gradiente dell'intestazione: pieno in cima, spento prima della griglia.
 *
 * È la variante 10 del mockup, scelta dall'utente (`d-frontespizio`). Si dipinge dietro il blocco
 * 'testata più fascia', che si accorcia mentre la fascia si chiude, quindi la coda segue senza
 * animazione.
 * ⚠️ **Dietro il blocco intero e non dietro la sola fascia**: `drawBehind` disegna dietro il
 * contenuto del proprio nodo, non dietro i fratelli già disegnati, e un rettangolo che sconfina
 * verso l'alto da un nodo successivo coprirebbe il titolo della testata. Il compilatore non lo
 * segnala. Sconfina apposta, perché `drawBehind` non ritaglia: esce dai fianchi di [air] e sale di
 * [up] fino al bordo dell'area sicura, e una fascia piena prende la barra di sistema col colore di
 * partenza (richiesta dell'utente). Una fascia e non un rettangolo più alto: allungando il
 * gradiente il suo massimo salirebbe sopra la barra, e sotto la testata la tinta sarebbe più chiara
 * di [WASH_PEAK]. La fascia segue [ink], e le icone della barra restano leggibili perché la tinta
 * arriva al solo [WASH_PEAK] sopra il fondo: chi alza quel numero guardi anche loro. Il colore di
 * serie è `primary`, la tinta della variante su cui il mockup ha misurato i contrasti; il chiamante
 * può passare il colore scelto per quella cartella. Dieci tappe, quelle del mockup, perché una
 * rampa dritta si legge come un bordo sfocato.
 *
 * @param tint la tinta piena, di solito `colorScheme.primary`.
 * @param air quanto sconfinare per lato, cioè il rientro orizzontale della schermata.
 * @param up quanto salire sopra il blocco, cioè il rientro verticale della schermata.
 * @param bar quanto è alta la barra di sistema sopra il blocco: la fascia piena arriva fin là.
 * @param ink quanto si vede la tinta, da 0 a 1: si legge in fase di disegno, perché segue lo
 *   scorrimento.
 */
fun Modifier.frontWash(
    tint: Color,
    air: Dp,
    up: Dp,
    bar: Dp,
    ink: () -> Float
): Modifier = composed {
    /*
     * ⚠️ **La rampa è fuori dalla cache del disegno** (rilievo D34): la cache si rifà a ogni cambio
     * di misura, che mentre la fascia si chiude cambia a ogni fotogramma, e dentro ogni fotogramma
     * compilerebbe di nuovo il programma del rumore (presidiato da `RampeTest`). Il gradiente si
     * rifà, e deve: segue l'altezza.
     */
    val rampa = remember { GrainedRamp(WASH_STOPS, WASH_PEAK) }
    drawWithCache {
        val ariaPx = air.toPx()
        val suPx = up.toPx()
        val barraPx = bar.toPx()
        val alto = size.height + suPx
        val pieno = tint.copy(alpha = WASH_PEAK)
        /*
         * La rampa si posa col rumore di [GrainedRamp], che toglie le bande (richiesta dell'utente;
         * il conto è in testa a `Dither.kt`). L'opacità che cambia scorrendo entra come `alpha`,
         * che non rifà niente. La fascia piena sopra la testata è tinta unita, e non ha bande.
         */
        val largo = size.width + ariaPx * 2
        onDrawBehind {
            val visto = ink()
            if (visto <= 0f || alto <= 0f) return@onDrawBehind
            if (barraPx > 0f) drawRect(
                color = pieno,
                topLeft = Offset(-ariaPx, -suPx - barraPx),
                size = Size(largo, barraPx),
                alpha = visto
            )
            rampa.paint(
                this, tint, from = -suPx, to = alto - suPx, alpha = visto,
                left = -ariaPx, top = -suPx, right = largo - ariaPx, bottom = alto - suPx
            )
        }
    }
}

/**
 * Quanto copre la tinta nel suo punto più forte: un quarto, tarato dall'utente col telefono in
 * mano. Si moltiplica per le tappe del mockup, che restano la forma della dissolvenza, e governa
 * anche la fascia piena sopra la testata, così la barra di sistema prende sempre il colore da cui
 * la sfumatura parte.
 */
private const val WASH_PEAK = 0.25f

/**
 * Le tappe del gradiente dell'intestazione: dove, e con quanto colore. Sono quelle del mockup, su
 * cui l'utente ha guardato la variante e misurato i contrasti.
 */
private val WASH_STOPS = listOf(
    0f to 1f, 0.12f to 0.97f, 0.24f to 0.90f, 0.36f to 0.78f, 0.48f to 0.62f,
    0.60f to 0.44f, 0.72f to 0.27f, 0.84f to 0.13f, 0.93f to 0.04f, 1f to 0f
)

/**
 * Quanto si vede l'icona della cartella col gradiente acceso nel tema chiaro: tutta, in negativo
 * (numero dell'utente). Con [FRONT_INK] è un fondale che si intravede, qui una sagoma: i due
 * estremi della stessa rampa (vedi [frontIconInk]). In negativo vuol dire il colore della
 * superficie, l'unico che è sempre dall'altra parte del contrasto.
 */
const val FRONT_NEG_INK = 1f

/**
 * Quanto si vede l'icona della cartella col gradiente acceso nel tema scuro: un quinto, in bianco
 * (numero dell'utente). Nel tema scuro la superficie è quasi nera, e il negativo sarebbe una sagoma
 * scura su una tinta scura. Bianco e non il colore del contenuto, che nel tema scuro è un bianco
 * sporco.
 */
const val FRONT_DARK_INK = 0.2f

/**
 * Quanto è lontana la fila delle pastiglie dal conto degli elementi: come [FRONT_GAP], perché le
 * pastiglie sono un'altra riga, e con l'aria stretta sembrerebbero la coda del conto.
 */
val FRONT_CHIP_GAP: Dp = 10.dp

/**
 * Quante righe può prendere il nome della cartella nell'intestazione: due, perché un nome lungo
 * entri (richiesta dell'utente, col titolo più piccolo). In testata resta a riga sola.
 */
const val FRONT_TITLE_LINES = 2



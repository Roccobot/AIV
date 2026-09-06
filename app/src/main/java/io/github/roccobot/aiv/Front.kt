package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Il **frontespizio**: la fascia in cima che si chiude scorrendo, e la sfumatura in fondo che
 * gli fa da controparte.
 *
 * ⚠️⚠️ **STA IN UN FILE SUO DALLA `1.76`, PERCHÉ DA QUELLA VERSIONE LE SCHERMATE CHE LO
 * PORTANO SONO DUE**: la schermata iniziale, dove è nato nella `0.60`, e la griglia di una
 * cartella, che lo ha chiesto lui (giro della `1.67`: *l'icona va posizionata esattamente come
 * quella oggi presente sulla schermata home, ma semitrasparente (~50%), e sotto, al posto del
 * nome dell'app, il titolo della cartella*). Fino a lì viveva in `FolderScreen.kt` come parte
 * privata di quella schermata.
 * ⚠️ **Non è un trasloco di comodo, è la regola sui numeri scritti due volte**: la frazione,
 * l'altezza della fascia dipinta, la curva della sfumatura e la sua coda sono **una** decisione
 * per l'app, presa dall'utente giro per giro, e copiarne i numeri nella seconda schermata
 * avrebbe fatto due tavolozze che divergono al primo ritocco.
 */

/**
 * Quanta parte dello schermo tiene il frontespizio da aperto: **un terzo scarso**.
 *
 * ⚠️⚠️ **NON È UNA PROPORZIONE ESTETICA ma una misura di portata del pollice**: l'utente
 * usa l'intestazione come scusa per tenere le cartelle in basso, in stile OneUI (sue
 * parole, 2026-08-31), quindi ritoccarla verso il basso rimette le cartelle fuori tiro e
 * verso l'alto toglie righe che si vorrebbero vedere.
 * ⚠️⚠️ **VALE PER TUTTE E DUE LE VISTE, dalla `0.93`**: fra la `0.60` e la `0.92` la
 * griglia faceva eccezione e si riservava un numero esatto di righe, e l'utente ha chiesto
 * di tornare alla frazione fissa perché quel calcolo dava alla griglia più di quanto lui
 * volesse. Un numero solo per la stessa regola: prima, cambiarla voleva dire ricordarsi
 * che esisteva anche altrove.
 * ⚠️ **E dalla `1.76` vale anche per la griglia di una cartella**, che è la terza superficie a
 * leggerlo: 'analogo alla home' sono parole sue, e l'unico modo di essere analoghi per sempre è
 * leggere lo stesso numero.
 * ⚠️ **34 e non 40, ed è una misura**: con il 40% l'area della griglia scende a 484dp
 * sullo schermo dell'utente, e la fascia sfumata (216dp) arriverebbe a coprire il conto
 * sotto la SECONDA riga di cartelle, cioè velerebbe una riga vera invece di quella che
 * fa capolino. Con il 34% la griglia sale a 533dp e la sfumatura comincia esattamente
 * dove comincia la terza riga. L'utente ha autorizzato il cambio proprio per questo
 * (*se pensi che sia troppo sacrificata possiamo passare a 66% alla griglia e 34%
 * all'intestazione*).
 */
const val HEADER_SHARE = 0.34f

/**
 * Quanto è larga l'icona del frontespizio: più grande di quella delle impostazioni, perché qui
 * accoglie.
 */
val HEADER_ICON = 96.dp

/**
 * Quanto è aperto il frontespizio, da 0 (chiuso) a 1.
 *
 * ⚠️ **La formula sta in un posto solo perché la leggono in tre**: la fascia, per sbiadire il suo
 * contenuto; la testata di una cartella, per far comparire il titolo quando la fascia lo lascia
 * andare; e la sfumatura in fondo, che se ne va con lei. Scritta tre volte, il giorno che una
 * curva cambia ne cambierebbe una sola.
 * ⚠️ **Si legge in fase di DISEGNO e non in composizione**: è il motivo per cui è una funzione
 * pura invece di uno stato derivato. Chi la chiama lo fa dentro un `graphicsLayer`, dove un
 * valore nuovo costa un ridisegno e non una ricomposizione.
 */
fun frontOpen(fullPx: Float, chiuso: Float): Float =
    if (fullPx > 0f) (1f - chiuso / fullPx).coerceIn(0f, 1f) else 0f

/**
 * La fascia del frontespizio: alta [fullPx] da aperta, e alta quel che resta mentre si chiude.
 *
 * ⚠️⚠️ **IL FIGLIO SI MISURA SEMPRE ALL'ALTEZZA PIENA e si RITAGLIA, non si schiaccia.**
 * Misurandolo con l'altezza che resta, l'icona verrebbe compressa mentre il frontespizio
 * si chiude, cioè un disegno che si deforma invece di uscire di scena. Qui si misura
 * intero, si dichiara alta quel che resta, e lo si colloca **centrato in quel che
 * resta**: il contenuto sale da sé mentre lo spazio si stringe, ed è la parallasse, non
 * un secondo movimento aggiunto sopra.
 *
 * ⚠️⚠️ **LA PARALLASSE È ANCHE LA TRASLAZIONE CHE LUI HA CHIESTO PER IL TITOLO DI UNA
 * CARTELLA** (*il nome in alto deve traslare con un'animazione fluida nella testata*): il
 * contenuto sale verso la testata mentre lo spazio si chiude, quindi la traslazione non è un
 * movimento in più da scrivere, è quella che questa fascia fa da sempre. Chi ne aggiungesse
 * una seconda sopra avrebbe due movimenti sullo stesso oggetto.
 *
 * ⚠️ Lo stato si legge dentro `layout` e nel `graphicsLayer` del contenuto, cioè in fase di
 * misura e di disegno: il trascinamento non fa ricomporre **niente**, e queste schermate
 * contengono una griglia che non deve rifarsi sessanta volte al secondo.
 *
 * @param aperto quanto è aperta la fascia, da 0 (chiusa) a 1: si legge nel `graphicsLayer` del
 *   contenuto, che è il posto in cui l'opacità non costa una ricomposizione.
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
 * Le due sfumature in fondo allo schermo, che inghiottono quello che gli scorre sotto.
 *
 * ⚠️⚠️ **LA FASCIA GRANDE, dalla `0.77`** (richiesta dell'utente: *dalla coordinata Y in cui
 * comincia il tastino, una piccola sfumatura verso il colore di fondo del tema, che inghiotte
 * ciò che sta giù abbastanza velocemente, in modo che dia poco fastidio, e che allo stesso
 * tempo suggerisce che la griglia si scorre*). Nella schermata iniziale al riposo non copre
 * niente, perché lo spazio riservato ([BELOW_FAB]) tiene l'ultima cartella sopra di lei; serve
 * quando si scorre, dove l'alternativa era una riga tagliata di netto dal bordo dello schermo.
 *
 * ⚠️⚠️ **UNA CURVA E NON QUATTRO FERMATE, dalla `0.92`** (richiesta dell'utente, due volte:
 * *più alta e graduale, in modo che il FAB ricada sempre in un'area neutra*, e poi *ancora più
 * sfumata e graduale*). Due fermate sole dànno una rampa **dritta**, e l'occhio la legge come un
 * bordo sfocato invece che come una dissolvenza: il difetto sta nei due spigoli, dove la salita
 * comincia e dove finisce. [smoothstep] li toglie tutti e due, perché parte con pendenza zero e
 * ci arriva con pendenza zero.
 * ⚠️ **Si calcola invece di essere scritta**: le fermate a mano sarebbero venti numeri da
 * riscrivere ogni volta che si cambia l'altezza della fascia, e nessuno lo farebbe. Così
 * [GRADIENT_TIMES] e [GRADIENT_PEAK] sono le sole manopole.
 *
 * ⚠️⚠️ **IL SECONDO STRATO, dalla `1.56`, ED È UNO STRATO E NON UNA FERMATA IN PIÙ**
 * (richiesta dell'utente, giro della `1.55`: *in più vorrei un ulteriore livello sopra la
 * sfumatura attuale, stesso colore, 100% di opacità sul bordo inferiore e 0% a 7/8 dp dal bordo
 * inferiore*). Serve a chiudere l'ultima striscia di schermo, che la fascia grande lascia a sei
 * decimi: là sotto passa il bordo stondato del vetro e la barra di sistema, e un'immagine che si
 * intravede proprio lì si legge come un difetto di disegno.
 * ⚠️⚠️ **PERCHÉ NON BASTAVA ALLUNGARE LA CURVA DELL'ALTRA**: quella arriva al suo massimo e ci
 * **resta** per l'ultimo terzo, quindi per finire in pieno sul bordo dovrebbe risalire, cioè
 * avere due massimi. Due strati invece si sommano da soli.
 *
 * ⚠️ Il colore è `background` e non `surface`: è quello che la `Surface` del tema mette dietro a
 * tutta l'app (vedi `AivTheme`), quindi la sfumatura arriva **esattamente** al fondo su cui sta.
 * ⚠️⚠️ **NON RUBA I TOCCHI, e non è una speranza**: Compose fa la prova del tocco solo sui nodi
 * che hanno un modificatore di puntatore, e qui non ce n'è nessuno. Senza questo fatto servirebbe
 * un `pointerInput` che lascia passare, che è il rimedio a un problema che non c'è.
 *
 * @param alpha quanto si vedono, da 0 a 1. ⚠️ **Il valore di serie è il pieno**, che è il caso
 *   della schermata iniziale: là il tastino c'è sempre, quindi la fascia che lo tiene su un fondo
 *   neutro non ha ragione di andarsene. Nella griglia di una cartella invece se ne va scorrendo,
 *   ed è una richiesta sua (*le due sfumature in basso devono progressivamente sparire e lasciare
 *   campo libero alla griglia piena su tutto lo schermo*).
 */
@Composable
fun GroundFade(modifier: Modifier = Modifier, alpha: () -> Float = { 1f }) {
    val ground = MaterialTheme.colorScheme.background
    val ramp = remember(ground) {
        Array(GRADIENT_STOPS + 1) { step ->
            val at = step / GRADIENT_STOPS.toFloat()
            at to ground.copy(alpha = swallow(at))
        }
    }
    val piede = remember(ground) {
        Array(FOOT_STOPS + 1) { step ->
            val at = step / FOOT_STOPS.toFloat()
            at to ground.copy(alpha = foot(at))
        }
    }
    /*
     * ⚠️⚠️ **UN'OPACITÀ SOLA PER TUTTE E DUE, e non una per strato**: i due strati si
     * **sovrappongono**, quindi sbiadendoli separatamente la loro somma non seguirebbe il
     * numero che arriva (due strati al 50% coprono più della metà di quanto coprano al 100%).
     * Con un livello solo intorno, quello che sbiadisce è il risultato già composto.
     */
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(GRADIENT_REACH)
            .graphicsLayer { this.alpha = alpha() }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(GRADIENT_REACH)
                .background(Brush.verticalGradient(colorStops = ramp))
        )
        // ⚠️ **Sta DOPO la fascia grande**: in un `Box` l'ultimo figlio sta sopra, e questa coda
        // esiste per riportare al pieno quello che la fascia lascia a sei decimi.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(FOOT_REACH)
                .background(Brush.verticalGradient(colorStops = piede))
        )
    }
}

/**
 * Quante volte il tastino è alta la fascia dipinta.
 *
 * ⚠️⚠️ **DUE E MEZZO DALLA `1.56`, ED È IL TERZO CAMBIO IN TRE VERSIONI**: quattro nella `1.54`
 * per fare posto a una coda in cima, tre nella `1.55` quando quella coda è sparita, e adesso due
 * e mezzo perché lui ha chiesto ancora la stessa cosa (*il punto di 0% si abbassa ancora
 * leggermente*). Ogni giro ha guardato la fascia in mano e l'ha voluta un po' più corta: qui non
 * c'è un numero giusto da calcolare, c'è il suo occhio.
 * ⚠️⚠️ **DIPINGERE PIÙ IN ALTO NON COSTA ALTEZZA ALLA GRIGLIA, ed è tutto il senso di questa
 * costante**: lo spazio **riservato** resta [BELOW_FAB]; questa dice soltanto fin dove arriva il
 * colore. Tenerle separate è la ragione per cui la fascia si alza e si abbassa senza che la
 * schermata guadagni o perda una riga di cartelle.
 * ⚠️ **La misura vecchia, che resta vera**: sullo schermo dell'utente la griglia ha 533dp e le
 * sue righe sono alte 184, quindi la terza comincia a 141dp dal fondo.
 */
private const val GRADIENT_TIMES = 2.5f

/**
 * L'opacità massima della sfumatura, quella che tiene dal tastino in giù.
 *
 * ⚠️⚠️ **ERA IL PIENO FINO ALLA `1.54`, E ADESSO NON LO È PIÙ** (richiesta dell'utente, giro
 * della `1.54`: *il colore non parte più da 100%, bensì da 70%*, e poi giro della `1.55`:
 * *l'opacità massima sul bordo inferiore scende al 60%*). ⚠️ **Il prezzo è dichiarato**: la
 * promessa vecchia era che sotto il tastino non passasse mai un'immagine, e con sei decimi di
 * colore un'immagine molto contrastata si intravede. È una scelta sua, non una svista, ed è in
 * linea con la concessione che aveva già fatto sulla stessa fascia (*può andare anche il 20%: si
 * intuisce comunque bene che è una cosa che va scomparendo*).
 * ⚠️ **Ma non vale più fino al bordo dello schermo**, dalla `1.56`: là sotto arriva il secondo
 * strato ([FOOT_REACH]), che riporta al pieno l'ultima striscia.
 */
private const val GRADIENT_PEAK = 0.60f

/**
 * In quanti gradini si disegna la curva della sfumatura.
 *
 * ⚠️ **Venti dalla `1.54`**: con dodici, su una fascia lunga come questa, ogni gradino sarebbe
 * alto una quindicina di dp e su un fondo chiaro si distinguerebbero a occhio nudo.
 */
private const val GRADIENT_STOPS = 20

/**
 * Quanto è alta la fascia dipinta sopra il tastino.
 *
 * ⚠️ **Non è privata perché la legge il banco di prova**: per sapere se un tocco cade **dentro**
 * la sfumatura serve sapere fin dove arriva, e ricopiare il numero là darebbe una prova che
 * misura la propria copia.
 */
internal val GRADIENT_REACH = FAB_REACH * GRADIENT_TIMES

/**
 * Quanto è alta la coda che chiude in pieno l'ultima striscia di schermo.
 *
 * ⚠️⚠️ **GLI 8 CHE AVEVA CHIESTO NON ERANO UN DIFETTO: ERANO INVISIBILI** (riscontro
 * dell'utente, giro della `1.56`: *non vedo la sovrapposizione piccola in fondo*). La coda
 * c'era e faceva quello che deve: misurata sul profilo composto, a 8dp dal bordo la copertura
 * vale 0,60 e sul bordo vale 1,00, cioè il pieno arriva davvero. Solo che quaranta punti di
 * copertura distribuiti su otto dp, su un fondo già coperto per sei decimi, sono un filo che
 * l'occhio non separa dalla fascia sopra.
 * ⚠️⚠️ **E NEMMENO VENTI BASTAVANO: il numero è suo, ed è 35** (riscontro del giro della
 * `1.57`: *era semplicemente troppo sottile: falla di 35 dp*). La `1.57` aveva alzato la coda
 * da 8 a 20 e lui l'ha bocciata di nuovo, il che dice una cosa che il profilo da solo non
 * diceva: quello che si vede non è il **pieno** sul bordo, che a 8dp c'era già, ma la
 * **lunghezza del tratto** in cui la copertura cresce. Sotto una certa lunghezza una
 * dissolvenza non si legge come tale, per quanto sia giusta la curva.
 * ⚠️⚠️ **E DALLA `1.60` SONO 40, MA IL NUMERO CHE CONTA È L'ALTRO** (riscontro del giro della
 * `1.59`: *40 dp di altezza, il pieno (opacità 100%) inizia 10 dp più in alto del bordo*).
 * Cinque dp in più sull'altezza non spostano niente; quello che cambia la forma è
 * [FOOT_SOLID], perché fino alla `1.59` il pieno esisteva **in un punto solo**, il bordo
 * dello schermo, e un massimo raggiunto in una riga di pixel non si vede come un massimo.
 * ⚠️ **La curva, il colore e il modo di sommarsi restano quelli che ha dettato lui**: quello
 * che si aggiunge è un pianoro in fondo, non una curva nuova.
 */
private val FOOT_REACH = 40.dp

/**
 * Quanto dura il pieno in fondo alla coda, misurato dal bordo dello schermo in su.
 *
 * ⚠️⚠️ **È LA STESSA FORMA DELLA FASCIA GRANDE, e non un'invenzione**: anche [swallow] sale
 * fino al bordo del tastino e poi tiene il suo massimo, e la ragione è la stessa in tutti e due
 * i posti. Una dissolvenza che tocca il massimo e subito finisce non ha un massimo da leggere:
 * si vede la salita, e quello che sta in cima lo si deduce.
 * ⚠️⚠️ **VENTIDUE DALLA `1.62`, ED È IL SUO SECONDO NUMERO SU QUESTO PIANORO** (riscontro del
 * giro della `1.60`: *ci siamo quasi: sposta il 100% della sfumatura piccola più in alto di
 * altri 12 dp*). Dieci era il primo, e la correzione dice una cosa sulla proporzione: adesso il
 * pieno occupa **più della metà** della coda e la salita ne ha diciotto, quindi quello che si
 * legge non è più una dissolvenza con un pianoro in fondo ma una striscia piena con un
 * raccordo sopra.
 * ⚠️ **Chi trovasse scritto 'dieci su quaranta' altrove sappia che è superato**, e il numero da
 * guardare è questo: la salita si ricava per differenza, non si scrive due volte.
 */
private val FOOT_SOLID = 22.dp

/**
 * In quanti gradini si disegna la coda.
 *
 * ⚠️⚠️ **VENTI DALLA `1.60`, ED ERANO DODICI PER UNA CODA CINQUE VOLTE PIÙ CORTA**: quel numero
 * era nato con gli 8dp della `1.56` e non l'ha più toccato nessuno mentre la coda cresceva, il
 * che è il difetto tipico di una costante che dipende da un'altra senza dirlo. Su 40dp, dodici
 * gradini sono più di tre dp l'uno.
 * ⚠️ **I gradini non sono bande, sono i vertici di una spezzata**: Compose interpola fra due
 * fermate, quindi quello che si vedrebbe non è banding ma gli spigoli con cui la spezzata
 * approssima la curva. È la stessa ragione per cui la fascia grande ne vuole venti.
 */
private const val FOOT_STOPS = 20

/**
 * A che punto della sua altezza la sfumatura sopra il tastino ha inghiottito tutto.
 *
 * ⚠️⚠️ **NON È UN NUMERO SCELTO A OCCHIO (era 0,55): si RICAVA.** Il bordo superiore del
 * tastino sta a [FAB_REACH] dal fondo, cioè a questa frazione della fascia dipinta: da lì in
 * giù il colore non cresce più, quindi il tastino sta tutto su un fondo di un colore solo.
 * Cambiando [GRADIENT_TIMES] il conto si rifà da sé.
 * ⚠️ Il rovescio da conoscere: alzando la fascia, il tratto a colore fermo resta lo stesso e
 * cresce solo la dissolvenza sopra, che è esattamente ciò che 'più graduale' vuol dire.
 * ⚠️ **Quel colore fermo non è più il pieno dalla `1.55`**: quanto vale lo dice
 * [GRADIENT_PEAK], e la promessa che ne cade è scritta là.
 */
private const val SWALLOW = 1f / GRADIENT_TIMES

/**
 * Quanto colore c'è a una data altezza della fascia, con `0` in cima e `1` sul fondo.
 *
 * ⚠️⚠️ **UNA SALITA SOLA, DI NUOVO, DALLA `1.55`**: la `1.54` ne aveva due perché la sfumatura
 * doveva **arrivare** a un terzo invece di sparire, e senza una coda in cima quel terzo sarebbe
 * comparso di colpo in una riga di pixel. Adesso in cima si arriva a zero, quindi lo scalino non
 * esiste e la seconda salita non ha più niente da nascondere.
 * ⚠️ **Il tratto si ricava da [SWALLOW]** e non è scritto a mano: si sale da niente a
 * [GRADIENT_PEAK] fino al bordo del tastino, e da lì in giù il colore sta fermo. Cambiando
 * [GRADIENT_TIMES] i due tratti si ridistribuiscono da soli.
 */
private fun swallow(at: Float): Float {
    val fermo = 1f - SWALLOW
    if (at >= fermo) return GRADIENT_PEAK
    return GRADIENT_PEAK * smoothstep(at / fermo)
}

/**
 * Quanto colore c'è a una data altezza della coda, con `0` in cima e `1` sul bordo di sotto.
 *
 * ⚠️⚠️ **DALLA `1.60` HA UN PIANORO, ed è la stessa forma di [swallow]**: si sale da niente al
 * pieno sui primi tratti, e negli ultimi [FOOT_SOLID] il colore sta fermo al massimo. Fino alla
 * `1.59` era la sola [smoothstep], quindi il pieno cadeva **esattamente** sul bordo dello
 * schermo, cioè in una riga di pixel, ed è quello che lui non vedeva.
 * ⚠️ **Il tratto in salita si ricava dalle due misure** e non è scritto a mano: cambiando
 * [FOOT_REACH] o [FOOT_SOLID] il pianoro si ridistribuisce da sé, come là.
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
 * Lo scorrimento che **chiude il frontespizio prima che l'elenco scorra**, e lo riapre in cima.
 *
 * ⚠️⚠️ **FUNZIONA ANCHE CON DUE ELEMENTI, e il fatto è verificato sul sorgente di Compose e non
 * supposto**: il trascinamento verso l'alto viene intercettato **prima** (`onPreScroll`) e speso
 * tutto qui, quindi l'elenco non ha bisogno di avere niente da scorrere.
 * `ScrollingLogic.performScroll` chiama `dispatchPreScroll` prima di consumare, e l'avvio del
 * trascinamento dipende dal **tipo di puntatore** (`canDrag`) e non dal fatto che ci sia spazio
 * da scorrere. Senza questo fatto avrei dovuto gonfiare l'elenco con spazio finto in fondo.
 * ⚠️ E si riapre dall'altra parte con `onPostScroll`: quello arriva solo quando l'elenco è già in
 * cima e ha avanzato del movimento, che è esattamente la condizione in cui il frontespizio deve
 * tornare.
 *
 * @param quanto quanti pixel di frontespizio ci sono in tutto.
 * @param chiuso quanti ne sono già stati chiusi.
 * @param chiudi dove scrivere il numero nuovo.
 */
fun frontScroll(
    quanto: Float,
    chiuso: () -> Float,
    chiudi: (Float) -> Unit
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y >= 0f) return Offset.Zero
        val take = (-available.y).coerceAtMost(quanto - chiuso())
        chiudi(chiuso() + take)
        return Offset(0f, -take)
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (available.y <= 0f) return Offset.Zero
        val give = available.y.coerceAtMost(chiuso())
        chiudi(chiuso() - give)
        return Offset(0f, give)
    }
}

/**
 * Quanto sta l'icona del frontespizio dal titolo sotto di lei.
 *
 * ⚠️ **Lo stesso numero della schermata iniziale** (dove viveva dentro `Identity`), perché le due
 * fasce devono somigliarsi: 'analogo alla home' sono parole sue.
 */
val FRONT_GAP: Dp = 10.dp

/**
 * Quanto si vede l'icona nel frontespizio di una cartella: **tre decimi**.
 *
 * ⚠️ **Il numero è suo, e questo è il secondo**: la `1.76` era uscita col ~50% della sua
 * specifica (*ma semitrasparente (~50%)*), e col telefono in mano l'ha voluta meno visibile
 * (riscontro del giro della `1.76`: *l'icona può essere meno visibile (proviamo con opacità
 * 30%)*). La ragione della prima vale ancora più adesso: là dentro l'icona non è il marchio
 * dell'app che si presenta, è un fondale dietro il nome della cartella, che è l'unica cosa da
 * leggere.
 * ⚠️ **Si moltiplica per l'apertura della fascia**, non la sostituisce: chiudendosi il
 * frontespizio sbiadisce come nella schermata iniziale, e questo dice soltanto da dove parte.
 */
const val FRONT_INK = 0.3f

/**
 * Quante righe può prendere il nome della cartella nel frontespizio: **due**.
 *
 * ⚠️ **È la metà della ragione per cui quel titolo è più piccolo di quello della testata**: la
 * richiesta dice *scritto un po' più piccolo per lasciare spazio anche a nomi lunghi*, e un nome
 * lungo entra solo se può andare a capo. In testata resta a riga sola, come è sempre stato.
 */
const val FRONT_TITLE_LINES = 2

/**
 * Quanto dura la chiusura del frontespizio quando non è un dito a chiuderlo.
 *
 * ⚠️ **Serve a un caso solo, la selezione che comincia** (sua specifica per la griglia di una
 * cartella: durante una selezione il frontespizio sta chiuso). Là non c'è nessun trascinamento
 * che porti il movimento, quindi senza un'animazione la fascia sparirebbe in un fotogramma.
 * ⚠️ **Lo stesso numero della dissolvenza fra due schermate** (`SCHERMO_MS`): cominciare una
 * selezione cambia la testata, i comandi e il senso di ogni tocco, cioè è un cambio di modo, e
 * due durate diverse nello stesso istante si leggono come un inceppamento.
 */
const val FRONT_SHUT_MS = 180

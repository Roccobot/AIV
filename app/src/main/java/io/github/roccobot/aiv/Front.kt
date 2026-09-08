package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Il **intestazione**: la fascia in cima che si chiude scorrendo, e la sfumatura in fondo che
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
 * Quanta parte dello schermo tiene l'intestazione da aperto: **un terzo scarso**.
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
 * Quanto è larga l'icona dell'intestazione: più grande di quella delle impostazioni, perché qui
 * accoglie.
 */
val HEADER_ICON = 96.dp

/**
 * Quanto è aperto l'intestazione, da 0 (chiuso) a 1.
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
 * La fascia dell'intestazione: alta [fullPx] da aperta, e alta quel che resta mentre si chiude.
 *
 * ⚠️⚠️ **IL FIGLIO SI MISURA SEMPRE ALL'ALTEZZA PIENA e si RITAGLIA, non si schiaccia.**
 * Misurandolo con l'altezza che resta, l'icona verrebbe compressa mentre l'intestazione
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
 * comincia il FAB, una piccola sfumatura verso il colore di fondo del tema, che inghiotte
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
 * ⚠️⚠️ **E DALLA `1.85` NELLE CARTELLE DI STRATO CE N'È UNO SOLO** (riscontro del giro della
 * `1.83`, voce `fab-sopra` approvata con una prova: *togli la seconda sfumatura sovrapposta,
 * quella corta. SOLO DALLE CARTELLE, resta in home*). Il secondo strato è nato per chiudere in
 * pieno l'ultima striscia di schermo, e là dentro quella striscia adesso la attraversa il FAB,
 * che dalla `1.83` passa **sopra** le sfumature: la coda gli finiva addosso.
 *
 * @param alpha quanto si vedono, da 0 a 1. ⚠️ **Il valore di serie è il pieno**, che è il caso
 *   della schermata iniziale: là il FAB c'è sempre, quindi la fascia che lo tiene su un fondo
 *   neutro non ha ragione di andarsene. Nella griglia di una cartella invece se ne va scorrendo,
 *   ed è una richiesta sua (*le due sfumature in basso devono progressivamente sparire e lasciare
 *   campo libero alla griglia piena su tutto lo schermo*).
 * @param foot se disegnare anche la coda che chiude in pieno l'ultima striscia. ⚠️ **Il valore di
 *   serie è di averla**, perché la schermata iniziale non ha cambiato idea: quello che cambia è
 *   la cartella, e un valore di serie rovesciato avrebbe tolto la coda anche a lei.
 */
/**
 * Quanto rientra il bordo di SOTTO dello schermo: la barra gestuale, o zero dove non c'è.
 *
 * ⚠️⚠️ **NASCE NELLA `1.90` PERCHÉ LE GRIGLIE ARRIVANO AL VETRO** (sua richiesta: *non si può
 * estendere la vista della griglia fino al margine inferiore dello schermo? Quella può restare
 * in sovrapposizione*). Le due schermate hanno smesso di mettersi il rientro di sotto sul
 * **contenitore**, che è quello che impediva di disegnare là sotto, e lo passano al
 * `contentPadding` della loro lista: così le miniature scorrono sotto la barra gestuale e
 * l'ultima riga resta comunque raggiungibile, perché lo scorrimento ha quello spazio in più.
 * ⚠️ **`safeDrawing` e non `navigationBars`**: comprende anche il ritaglio del display e la
 * tastiera, cioè tutto quello che sul bordo di sotto può mangiarsi il contenuto. È lo stesso
 * insieme che le schermate usavano prima con `safeDrawingPadding`, quindi il conto non cambia:
 * cambia solo chi se lo mette.
 */
@Composable
fun bottomInset(): Dp = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()

@Composable
fun GroundFade(
    modifier: Modifier = Modifier,
    alpha: () -> Float = { 1f },
    foot: Boolean = true
) {
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
        if (foot) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(FOOT_REACH)
                    .background(Brush.verticalGradient(colorStops = piede))
            )
        }
    }
}

/**
 * Quante volte il FAB è alta la fascia dipinta.
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
 * L'opacità massima della sfumatura, quella che tiene dal FAB in giù.
 *
 * ⚠️⚠️ **ERA IL PIENO FINO ALLA `1.54`, E ADESSO NON LO È PIÙ** (richiesta dell'utente, giro
 * della `1.54`: *il colore non parte più da 100%, bensì da 70%*, e poi giro della `1.55`:
 * *l'opacità massima sul bordo inferiore scende al 60%*). ⚠️ **Il prezzo è dichiarato**: la
 * promessa vecchia era che sotto il FAB non passasse mai un'immagine, e con sei decimi di
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
 * Quanto è alta la fascia dipinta sopra il FAB.
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
 * ⚠️⚠️ **CINQUANTACINQUE DALLA `1.91`, E LA RAGIONE È LA `1.90`** (sua richiesta: *adesso che la
 * griglia arriva fino alla fine del vetro anche in basso, la seconda sfumatura deve ... finire il
 * gradiente 15dp più in alto*). Finché la griglia si fermava sopra la barra gestuale, sotto la
 * coda c'era il fondo dell'app; adesso là sotto passano le miniature, quindi la stessa coda ha
 * più da coprire e la sua cima deve cominciare prima.
 */
private val FOOT_REACH = 55.dp

/**
 * Quanto dura il pieno in fondo alla coda, misurato dal bordo dello schermo in su.
 *
 * ⚠️⚠️ **È LA STESSA FORMA DELLA FASCIA GRANDE, e non un'invenzione**: anche [swallow] sale
 * fino al bordo del FAB e poi tiene il suo massimo, e la ragione è la stessa in tutti e due
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
 * ⚠️⚠️ **TRENTADUE DALLA `1.91`, ED È IL SUO TERZO NUMERO** (sua richiesta dopo la `1.90`: *la
 * seconda sfumatura deve essere a 100% 10dp più in alto*). Il pieno sale insieme alla cima della
 * coda ([FOOT_REACH], 15 in più), quindi la salita resta un raccordo di 23 dp: le due misure sono
 * arrivate insieme perché insieme tengono la proporzione che aveva tarato nella `1.62`.
 */
private val FOOT_SOLID = 32.dp

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
 * A che punto della sua altezza la sfumatura sopra il FAB ha inghiottito tutto.
 *
 * ⚠️⚠️ **NON È UN NUMERO SCELTO A OCCHIO (era 0,55): si RICAVA.** Il bordo superiore del
 * FAB sta a [FAB_REACH] dal fondo, cioè a questa frazione della fascia dipinta: da lì in
 * giù il colore non cresce più, quindi il FAB sta tutto su un fondo di un colore solo.
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
 * [GRADIENT_PEAK] fino al bordo del FAB, e da lì in giù il colore sta fermo. Cambiando
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
 * Lo scorrimento che **chiude l'intestazione prima che l'elenco scorra**, e lo riapre in cima.
 *
 * ⚠️⚠️ **FUNZIONA ANCHE CON DUE ELEMENTI, e il fatto è verificato sul sorgente di Compose e non
 * supposto**: il trascinamento verso l'alto viene intercettato **prima** (`onPreScroll`) e speso
 * tutto qui, quindi l'elenco non ha bisogno di avere niente da scorrere.
 * `ScrollingLogic.performScroll` chiama `dispatchPreScroll` prima di consumare, e l'avvio del
 * trascinamento dipende dal **tipo di puntatore** (`canDrag`) e non dal fatto che ci sia spazio
 * da scorrere. Senza questo fatto avrei dovuto gonfiare l'elenco con spazio finto in fondo.
 * ⚠️ E si riapre dall'altra parte con `onPostScroll`: quello arriva solo quando l'elenco è già in
 * cima e ha avanzato del movimento, che è esattamente la condizione in cui l'intestazione deve
 * tornare.
 *
 * @param quanto quanti pixel di intestazione ci sono in tutto.
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
 * Quanto sta l'icona dell'intestazione dal titolo sotto di lei.
 *
 * ⚠️ **Lo stesso numero della schermata iniziale** (dove viveva dentro `Identity`), perché le due
 * fasce devono somigliarsi: 'analogo alla home' sono parole sue.
 */
val FRONT_GAP: Dp = 10.dp

/**
 * Quanto sta il conto degli elementi dal nome della cartella, nell'intestazione.
 *
 * ⚠️ **Più corto di [FRONT_GAP], e non è un numero a caso**: il nome e il numero sono **una**
 * cosa da leggere insieme, mentre l'icona sopra è un fondale. Un'aria uguale a quella dell'icona
 * li farebbe leggere come due righe indipendenti.
 */
val FRONT_COUNT_GAP: Dp = 2.dp

/**
 * Quanto si vede l'icona nell'intestazione di una cartella: **diciotto centesimi**.
 *
 * ⚠️ **Il numero è suo, e questo è il TERZO in tre versioni**: la `1.76` era uscita col ~50%
 * della sua specifica (*ma semitrasparente (~50%)*), la `1.77` è scesa a 0,3 col telefono in
 * mano (*l'icona può essere meno visibile (proviamo con opacità 30%)*), e la `1.78` a **0,18**
 * (riscontro del giro della `1.77`: *l'icona della cartella (che dev'essere ancora meno opaca,
 * facciamo 18%)*). La ragione della prima vale ancora più adesso: là dentro l'icona non è il
 * marchio dell'app che si presenta, è un fondale dietro il nome della cartella, che è l'unica
 * cosa da leggere.
 * ⚠️⚠️ **E NON SI MOLTIPLICA PIÙ PER L'APERTURA, dalla `1.78`**: la dissolvenza è diventata
 * tardiva e vive in [frontIconInk], perché adesso l'icona si **rimpicciolisce** prima di
 * sparire (vedi [frontIconMeasure]) e sbiadire da subito avrebbe reso invisibile proprio il
 * movimento che lui ha chiesto.
 */
const val FRONT_INK = 0.18f

/**
 * Quanta parte dello spazio verticale libero può prendere l'icona dell'intestazione.
 *
 * ⚠️ **Era un `0.5f` scritto a mano nel chiamante**, e questo è il posto in cui viveva già lo
 * stesso conto della schermata iniziale: appare qui perché dalla `1.78` lo legge anche
 * [frontIconMeasure], e due copie dello stesso numero divergono al primo ritocco.
 */
const val FRONT_ICON_SHARE = 0.5f

/**
 * Da che apertura l'icona dell'intestazione comincia a rimpicciolirsi, e con lei a sbiadire.
 *
 * ⚠️⚠️ **SI RICAVA DALLA MISURA E NON È PIÙ UN NUMERO SCRITTO A MANO, dalla `1.80`** (riscontro
 * del giro della `1.79`, voce `front-selezione` approvata con una nota: *l'icona cartella deve
 * iniziare la sua dissolvenza appena inizia a ridursi di dimensione, e arrivare alla dimensione
 * minima e a opacità 0 contemporaneamente*). Le due cose devono cominciare e finire insieme, e
 * un `0.35f` scritto accanto non lo garantisce: [frontIconMeasure] tiene il lato pieno finché lo
 * spazio libero glielo permette, quindi il rimpicciolimento comincia esattamente quando
 * `libero * FRONT_ICON_SHARE` scende sotto il lato massimo. Questo è quel punto, risolto per
 * l'apertura.
 * ⚠️ **La `1.78` e la `1.79` avevano un `0.35f`**, cioè una dissolvenza che partiva a un terzo di
 * corsa mentre l'icona cominciava a stringersi molto prima (col telefono dell'utente, intorno a
 * sei decimi): fra i due punti l'icona si rimpiccioliva a inchiostro pieno, e quello è quanto
 * lui ha visto.
 * ⚠️ **Il minimo è zero e ci arrivano insieme per costruzione**: a fascia chiusa `libero` è zero,
 * quindi il lato è zero e questa frazione è zero.
 *
 * @param fullPx quanto è alta la fascia da aperta, in pixel.
 * @param maxPx il lato massimo dell'icona, in pixel.
 */
fun frontIconFade(fullPx: Float, maxPx: Float): Float =
    if (fullPx <= 0f) 1f else (maxPx / (fullPx * FRONT_ICON_SHARE)).coerceIn(0f, 1f)

/**
 * Quanto inchiostro ha l'icona dell'intestazione con la fascia aperta di [aperto].
 *
 * ⚠️ **Sta accanto ai suoi numeri e non nel chiamante**: la curva e la soglia sono una cosa sola,
 * e separarle vorrebbe dire cambiare una soglia senza cambiare la curva.
 * ⚠️⚠️ **LA RAMPA È LINEARE E NON PIÙ UNA [smoothstep], dalla `1.80`**: quella parte con pendenza
 * zero, quindi il primo tratto di dissolvenza non si vede, e la richiesta è che la dissolvenza
 * **cominci** insieme al rimpicciolimento. Con la rampa dritta l'inchiostro segue la misura in
 * proporzione, che è il modo di far leggere le due cose come un movimento solo.
 *
 * ⚠️⚠️ **IL VALORE PIENO ARRIVA DA FUORI DALLA `1.83`, e prima era [FRONT_INK]**: con la tinta
 * accesa l'icona va in negativo all'80% (variante 10), cioè cambia il numero da cui la rampa
 * parte e **non** la rampa. Scritto qui dentro con un `if`, questa funzione avrebbe dovuto
 * conoscere un'impostazione, che è esattamente quello che una funzione pura non deve fare.
 *
 * @param soglia il punto da cui l'icona si stringe, cioè [frontIconFade].
 * @param pieno l'inchiostro a fascia aperta: [FRONT_INK] di solito, [FRONT_NEG_INK] in negativo.
 */
fun frontIconInk(aperto: Float, soglia: Float, pieno: Float = FRONT_INK): Float =
    pieno * if (soglia <= 0f) 1f else (aperto / soglia).coerceIn(0f, 1f)

/**
 * L'icona dell'intestazione si misura sullo spazio che la fascia le lascia, a ogni fotogramma.
 *
 * ⚠️⚠️ **È UNA MISURA E NON UNA SCALA, ed è la differenza fra le due parole della sua
 * richiesta**: *rimpicciolirsi* e *adattarsi allo spazio disponibile in verticale*. Una
 * `graphicsLayer` che scala rimpicciolisce il disegno e lascia il posto occupato, quindi il
 * titolo sotto non sale di un pixel; misurando, l'icona **cede** lo spazio che libera, e il
 * nome della cartella lo prende.
 * ⚠️⚠️ **SI LEGGE IN FASE DI MISURA E NON IN COMPOSIZIONE**, che è lo stesso mestiere che fa
 * [FrontBand] con la propria altezza: `shut()` letto in composizione farebbe ricomporre questa
 * schermata sessanta volte al secondo, e sotto c'è una griglia. Il costo che resta è una
 * **rimisura** della fascia per fotogramma, e c'era già: la fascia dichiara un'altezza diversa
 * a ogni pixel di scorrimento.
 * ⚠️ **Il quadrato è d'obbligo**: un'icona misurata più stretta che alta si deformerebbe, e
 * `Constraints.fixed` con lo stesso lato è il modo di dirlo una volta.
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
 * La **tinta** dell'intestazione: piena in cima, spenta una riga di miniature sotto la fascia.
 *
 * ⚠️⚠️ **È LA VARIANTE 10 DEL MOCKUP, SCELTA DA LUI** (risposta a `d-frontespizio` del giro della
 * `1.81`), e le tre cose che la distinguono dalla 8 sono sue: la tinta **non muore col
 * intestazione** ma si spegne una riga più in basso, l'icona passa in negativo all'80%, e sotto
 * il conto arriva una fila di pastiglie. Qui c'è la prima.
 *
 * ⚠️⚠️ **SI DIPINGE DIETRO IL BLOCCO 'TESTATA PIÙ FASCIA', ED È QUELLO CHE LA FA ACCORCIARE DA
 * SÉ**: l'altezza dell'area di disegno **è** quella del blocco, che si stringe mentre la fascia
 * si chiude, quindi la coda segue senza che nessuno la animi. La richiesta lo dice: *la coda è
 * attaccata alla fascia e si accorcia con lei*, e l'alternativa (una coda ferma sotto cui scorre
 * la griglia) tingerebbe una fila diversa a ogni fotogramma.
 * ⚠️⚠️ **SCONFINA DI PROPOSITO, e in Compose si può**: `drawBehind` non ritaglia, quindi il
 * rettangolo esce dai fianchi per [air] e sale di [up] fino al bordo dell'area sicura, dove la
 * tinta deve cominciare (*parte sotto la barra di sistema e prende anche la testata*: partendo
 * sotto la testata ci sarebbe un gradino netto fra il pieno e il fondo chiaro). I due numeri
 * sono i rientri della schermata, e chi li cambia passa di qui.
 * ⚠️ **Il colore è `primary` e non l'accento della sfumatura in fondo**: è la tinta della carta
 * 8, quella che lui ha dettato, e le due misure di contrasto del mockup (titolo a 8,5, icona in
 * negativo a 1,9) valgono per lei. ⚠️ **Dalla `1.85` può arrivare anche il colore scelto per
 * quella cartella**, e chi sceglie è il chiamante: qui la tinta è un ingresso.
 * ⚠️⚠️ **DIECI TAPPE E NON DUE, e il perché è lo stesso della sfumatura in fondo**: una rampa
 * dritta si legge come un bordo sfocato, perché l'occhio vede i due spigoli in cui la salita
 * comincia e finisce. Le tappe qui sono quelle del mockup, cioè quelle che lui ha guardato.
 *
 * ⚠️⚠️ **DALLA `1.85` LA TINTA VA DIETRO IL BLOCCO INTERO 'TESTATA PIÙ FASCIA', E NON DIETRO LA
 * SOLA FASCIA: È LA CORREZIONE DEL DIFETTO CHE HA FATTO BOCCIARE LA `1.83`** (riscontro della
 * voce `front-dieci`: *il nome della cartella e gli elementi non passano più in testa allo
 * scorrimento (lo spazio rimane vuoto)*). `drawBehind` disegna dietro il contenuto **del proprio
 * nodo**, non dietro i fratelli che la colonna ha già disegnato: con la tinta su un nodo che
 * veniva dopo la testata, il rettangolo che sconfina verso l'alto le finiva **sopra**, e il
 * titolo che stava comparendo ci spariva dentro. Chi la sposta di nuovo su un fratello successivo
 * rifà lo stesso difetto, e il compilatore non dirà niente.
 * ⚠️ **Con lei sparisce anche la misura della testata**: l'altezza da cui la tinta parte adesso è
 * quella del nodo, quindi non c'è più niente da misurare con un `onGloballyPositioned` e da
 * ricomporre quando cambia.
 *
 * ⚠️⚠️ **E NON SCENDE PIÙ SOTTO LA FASCIA** (stesso riscontro: *inizia da 70% e sfuma verso lo 0
 * prima di raggiungere la griglia*): fino alla `1.84` la coda arrivava una riga di miniature più
 * in basso, cioè tingeva la prima fila di immagini.
 *
 * ⚠️⚠️ **E DALLA `1.87` SALE FIN SOTTO LA BARRA DI SISTEMA, CON UNA FASCIA PIENA** (sua
 * richiesta, con schermata: *puoi colorare la barra di sistema di Android dello stesso colore
 * dell'inizio della sfumatura?*). Fino alla `1.86` il rettangolo si fermava al bordo dell'area
 * sicura, quindi sopra restava una striscia del fondo dell'app e la tinta cominciava con un
 * gradino netto proprio dove l'occhio la incontra per prima.
 * - ⚠️⚠️ **UNA FASCIA PIENA E NON UN RETTANGOLO PIÙ ALTO, e la differenza non è di comodo**:
 *   allungando il gradiente verso l'alto il suo massimo si sposterebbe sopra la barra, e sotto
 *   la testata la tinta verrebbe più chiara di [WASH_PEAK], cioè cambierebbe la rampa che lui ha
 *   tarato al giro prima. Con la fascia, la barra prende **esattamente** il colore di partenza,
 *   che è quello che ha chiesto, e la sfumatura resta quella approvata.
 * - ⚠️ **Segue [ink] come il resto**: scorrendo la fascia si spegne insieme alla sfumatura, o
 *   resterebbe una striscia colorata in cima a una griglia che non ha più niente di colorato.
 * - ⚠️ **Le icone della barra non si toccano**: la tinta arriva a [WASH_PEAK] sopra il fondo
 *   dell'app, quindi il contrasto con cui il sistema le disegna resta quello di prima. Chi
 *   alzasse quel numero guardi anche quelle.
 *
 * @param tint la tinta piena, di solito `colorScheme.primary`.
 * @param air quanto sconfinare per lato, cioè il rientro orizzontale della schermata.
 * @param up quanto salire sopra il blocco, cioè il rientro verticale della schermata.
 * @param bar quanto è alta la barra di sistema sopra di lui, cioè il rientro che la schermata le
 *   ha già lasciato: la fascia piena arriva fin là.
 * @param ink quanto si vede la tinta, da 0 a 1: si legge in fase di **disegno**, perché segue lo
 *   scorrimento e leggerla in composizione farebbe rifare la griglia a ogni pixel.
 */
fun Modifier.frontWash(
    tint: Color,
    air: Dp,
    up: Dp,
    bar: Dp,
    ink: () -> Float
): Modifier = drawWithCache {
    val ariaPx = air.toPx()
    val suPx = up.toPx()
    val barraPx = bar.toPx()
    val alto = size.height + suPx
    val pieno = tint.copy(alpha = WASH_PEAK)
    /*
     * ⚠️ **Il pennello si costruisce UNA VOLTA per misura e non a ogni fotogramma**: l'opacità
     * che cambia mentre si scorre entra dal parametro `alpha` di `drawRect`, che moltiplica il
     * colore già composto. Rifacendo le dieci tappe a ogni disegno si allocherebbe un pennello
     * per fotogramma per ottenere lo stesso risultato.
     */
    val pennello = Brush.verticalGradient(
        colorStops = WASH_STOPS.map { (at, quanto) -> at to tint.copy(alpha = quanto * WASH_PEAK) }
            .toTypedArray(),
        startY = -suPx,
        endY = alto - suPx
    )
    /*
     * ⚠️⚠️ **IL PENNELLO SI POSA CON UN PAINT CHE DITHERA, DALLA `1.95`, E SENZA QUESTA RIGA LA
     * SFUMATURA HA LE BANDE** (sua segnalazione: *noto un banding fastidioso nel gradiente
     * dell'intestazione: riducilo al massimo*). Il conto che spiega il difetto: fra il picco della
     * tinta e il fondo dell'app ci sono una manciata di livelli su 255, e quei livelli sono
     * distribuiti su tutta l'altezza della fascia, quindi ogni gradino della quantizzazione a 8
     * bit è alto decine di pixel, cioè una striscia che si vede.
     * ⚠️⚠️ **A 8 BIT L'UNICO RIMEDIO È IL DITHERING**, che spezza il gradino con mezzo livello di
     * rumore ordinato: non si ottiene con più tappe (l'interpolazione è già continua, a quantizzare
     * è la destinazione) e non si ottiene schiarendo la rampa.
     * ⚠️⚠️ **E COMPOSE NON LO ACCENDE DA SÉ, che è la ragione per cui il difetto si vede QUI**:
     * [DrawScope.drawRect] costruisce un paint suo, dove il dither resta spento;
     * `GradientDrawable`, cioè la stessa sfumatura scritta in XML per una `View`, lo accende di
     * serie. Da qui il paint di casa, con `isDither` acceso e riusato per ogni fotogramma.
     * ⚠️ **La fascia piena sopra la testata non passa di qui**: è tinta unita, e una tinta unita
     * non ha nessuna rampa da quantizzare.
     */
    val pittura = Paint().apply { asFrameworkPaint().isDither = true }
    onDrawBehind {
        val visto = ink()
        if (visto <= 0f || alto <= 0f) return@onDrawBehind
        if (barraPx > 0f) drawRect(
            color = pieno,
            topLeft = Offset(-ariaPx, -suPx - barraPx),
            size = Size(size.width + ariaPx * 2, barraPx),
            alpha = visto
        )
        val largo = size.width + ariaPx * 2
        pennello.applyTo(Size(largo, alto), pittura, visto)
        drawIntoCanvas { tela ->
            tela.drawRect(-ariaPx, -suPx, largo - ariaPx, alto - suPx, pittura)
        }
    }
}

/**
 * Quanto arriva a coprire la tinta nel suo punto più forte: **un quarto**.
 *
 * ⚠️⚠️ **QUATTRO VALORI IN QUATTRO VERSIONI, E LI HA DETTATI TUTTI COL TELEFONO IN MANO**: il
 * pieno fino alla `1.84`, il 70% con la `1.85` (*sfumatura molto meno visibile: inizia il
 * gradiente già dal bordo superiore, e anzi inizia da 70%*), il 40% con la `1.86` (riscontro del
 * giro della `1.85`, voce `int-gradiente` approvata con una nota: *ancora troppo invadente:
 * proviamo dal 40% anziché 70%*), e il **25%** con la `1.89` (*allora porta il valore a 25%*).
 * Le dieci tappe restano quelle del mockup: quello che cambia è il numero da cui partono, quindi
 * la **forma** della dissolvenza è ancora quella che lui ha guardato.
 * ⚠️ **Si moltiplica invece di riscrivere le tappe**, ed è la ragione per cui quattro giri di
 * ritocchi sono costati quattro caratteri: scritte col numero già dentro, ogni cambio vorrebbe
 * dire rifare dieci moltiplicazioni a mano.
 * ⚠️⚠️ **E DALLA `1.87` GOVERNA ANCHE LA BARRA DI SISTEMA, che è la ragione per cui il ritocco
 * della `1.89` è di un carattere solo** (sua domanda: *se decido di passare da 40% a 25% come
 * opacità d'inizio per la sfumatura, adatterai automaticamente il colore della barra di
 * sistema?*). La fascia piena sopra la testata legge questa costante e non una sua copia, quindi
 * la barra prende sempre esattamente il colore da cui la sfumatura parte.
 */
private const val WASH_PEAK = 0.25f

/**
 * Le tappe della tinta dell'intestazione: dove, e con quanto colore.
 *
 * ⚠️ **Sono quelle del mockup**, cioè quelle su cui l'utente ha guardato la variante e ha
 * misurato i contrasti: cambiarle vorrebbe dire mostrargli una cosa e dargliene un'altra.
 */
private val WASH_STOPS = listOf(
    0f to 1f, 0.12f to 0.97f, 0.24f to 0.90f, 0.36f to 0.78f, 0.48f to 0.62f,
    0.60f to 0.44f, 0.72f to 0.27f, 0.84f to 0.13f, 0.93f to 0.04f, 1f to 0f
)

/**
 * Quanto si vede l'icona della cartella quando la tinta è accesa: **tutto**, in negativo.
 *
 * ⚠️ **Il numero è suo, ed è il secondo**: la `1.83` era uscita all'80% con la sua prima
 * specifica (*l'icona centrata in negativo all'80% invece del 20%*), e il giro dopo lo ha portato
 * al pieno con la sfumatura sotto molto più chiara (*l'icona resta in negativo, 100%, vediamo che
 * effetto fa*). Le due cose vanno insieme: su una tinta al 70% una sagoma all'80% si spegne.
 * ⚠️ **Resta un numero e non sparisce**: con [FRONT_INK] è un fondale che si intravede, qui è una
 * sagoma, e sono i due estremi della stessa rampa (vedi [frontIconInk]).
 * ⚠️ **In negativo vuol dire il colore della SUPERFICIE**, non un grigio: sopra la tinta il
 * colore del contenuto sparirebbe, e quello della superficie è l'unico che sta sempre dall'altra
 * parte del contrasto, in tutti e due i temi.
 */
const val FRONT_NEG_INK = 1f

/**
 * Quanto si vede l'icona della cartella col gradiente acceso **nel tema scuro**: poco più di un
 * terzo, in bianco.
 *
 * ⚠️⚠️ **DALLA `1.95`, ED È SUA RICHIESTA** (*l'icona dell'intestazione deve ritornare positiva
 * (sovrapposta) per il tema scuro: bianco, opacità 40%*). Fino alla `1.94` il negativo valeva per
 * tutti e due i temi, e il fatto che nel tema scuro non funzionasse è geometrico e non di gusto:
 * lì la superficie dell'app è quasi nera, quindi 'in negativo' vuol dire una sagoma nera su una
 * tinta al 25% di un fondo già scuro, cioè due scuri uno sopra l'altro.
 * ⚠️ **Bianco e non [FRONT_INK] del contenuto**: il colore del contenuto nel tema scuro è un
 * bianco sporco di superficie, e la sua parola è *bianco*.
 */
const val FRONT_DARK_INK = 0.4f

/**
 * Quanto sta la fila delle pastiglie dal conto degli elementi.
 *
 * ⚠️ **Come [FRONT_GAP] e non come [FRONT_COUNT_GAP]**: il nome e il numero sono una cosa sola da
 * leggere, le pastiglie sono un'altra riga, e con l'aria stretta si leggerebbero come la coda del
 * conto.
 */
val FRONT_CHIP_GAP: Dp = 10.dp

/**
 * Quante righe può prendere il nome della cartella nell'intestazione: **due**.
 *
 * ⚠️ **È la metà della ragione per cui quel titolo è più piccolo di quello della testata**: la
 * richiesta dice *scritto un po' più piccolo per lasciare spazio anche a nomi lunghi*, e un nome
 * lungo entra solo se può andare a capo. In testata resta a riga sola, come è sempre stato.
 */
const val FRONT_TITLE_LINES = 2



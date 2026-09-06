package io.github.roccobot.aiv

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * L'entrata del tastino della schermata iniziale, dalla 1.58.
 *
 * ⚠️⚠️ **RICHIESTA DELL'UTENTE, giro della `1.56`** (*all'avvio dell'app e ogni volta che si
 * torna sulla home, il tastino FAB principale deve entrare con un'animazione, che è già in
 * corso all'apertura, e non dev'essere troppo appariscente. Un leggero zoom-in con rimbalzo e
 * dissolvenza*), e la variante l'ha scelta lui su un confronto animato di tre: quello di
 * adesso, questa e una più marcata.
 *
 * ⚠️⚠️ **'GIÀ IN CORSO' È LA PARTE CHE DECIDE TUTTO, e si legge nei due numeri di partenza**:
 * un'animazione che comincia da zero ha un fotogramma in cui il tastino **non c'è**, e
 * quell'assenza l'occhio la legge come un'app lenta invece che come un'entrata. Partendo da
 * [ENTRA_DA] della misura e da [ENTRA_ALFA] dell'opacità, al primo fotogramma il tastino c'è
 * già e sta soltanto finendo di posarsi.
 *
 * ⚠️⚠️ **TRE COSE INSIEME E UN OGGETTO SOLO, dalla `1.60`**: la misura, l'opacità e l'**ombra**
 * nascono e finiscono nella stessa entrata, quindi vivono in uno stato solo. Fino alla `1.59`
 * questo file esportava un modificatore e basta, e l'ombra non poteva entrarci: `shadowElevation`
 * è un parametro di [TapHoldFab] e non una proprietà del suo genitore.
 * ⚠️ **E metterla sul genitore sarebbe stato peggio che scomodo**: la pressione del tastino lo
 * rimpicciolisce del sei per cento, e un'ombra disegnata dalla scatola di fuori resterebbe della
 * misura piena mentre il tastino si stringe, sbordando.
 *
 * ⚠️⚠️ **LA SCALA VA A MOLLA E LE ALTRE DUE NO, e non è una svista**: una molla supera il
 * bersaglio e poi torna, che su una misura è il rimbalzo chiesto, ma su un'opacità vorrebbe
 * dire passare oltre l'uno, cioè un lampo. La `tween` non può farlo per costruzione.
 *
 * ⚠️⚠️ **I TEMPI SCRITTI QUI FINO ALLA `1.59` ERANO IL DOPPIO DEL VERO, e la cifra sbagliata è
 * finita anche nel documento di feedback**: dicevano 234 ms per la `1.58` e 292 per la `1.59`,
 * misurati con una soglia di 0,001 e guardando il solo valore. La soglia di un
 * `Animatable(Float)` costruito senza dirne una è **0,01**
 * (`Spring.DefaultDisplacementThreshold`), e `SpringSimulation` chiede **due** condizioni, non
 * una: `|x| < soglia` **e** `|v| < soglia * 62,5`. Su una molla che rimbalza è la velocità a
 * legare, perché nell'istante in cui il valore attraversa il bersaglio la velocità è massima.
 * Misurate come si deve, la `1.58` assestava in **131 ms** e la `1.59` in **147**.
 *
 * ⚠️ **Si rigioca a ogni ritorno sulla home**, che è quello che è stato chiesto, e non c'è
 * niente da scrivere perché lo fa la composizione: dalla `1.56` il cambio di schermata passa
 * da un `AnimatedContent`, quindi rientrando nella schermata iniziale nasce una composizione
 * nuova e con lei il [remember] di questo stato.
 */
@Stable
class Entrata internal constructor(
    private val misura: Animatable<Float, AnimationVector1D>,
    private val velo: Animatable<Float, AnimationVector1D>
) {
    internal val scala: Float get() = misura.value
    internal val opacita: Float get() = velo.value

    /*
     * ⚠️⚠️ **L'OMBRA CHE SALIVA QUI NON C'È PIÙ, DALLA `1.68`** (istruzione dell'utente, giro
     * della `1.67`: *togli del tutto l'ombra*). Era una salita lenta e ritardata, allungata due
     * volte su sua richiesta, e con lei se ne vanno il terzo `Animatable` di questa classe, la
     * funzione con cui il FAB la leggeva nel disegno, e le sue due costanti. Chi cerca quel
     * meccanismo in una nota vecchia sappia che il tastino adesso non ha nessuna ombra da
     * alzare: quello che gli resta dell'entrata è la misura che si posa e l'opacità che sale.
     */
    internal suspend fun posa() = coroutineScope {
        launch { velo.animateTo(1f, tween(ENTRA_ALFA_MS, easing = LinearOutSlowInEasing)) }
        misura.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = ENTRA_MOLLA)
        )
    }
}

/**
 * Se la schermata che ospita questo composabile **sta ancora arrivando**, cioè se la dissolvenza
 * del cambio di schermata è in corso.
 *
 * ⚠️⚠️ **LA FORNISCE CHI FA IL CAMBIO DI SCHERMATA** (`ViewerActivity`, dentro la
 * `AnimatedContent`), perché è l'unico posto che ha la transizione in mano. Il valore di serie è
 * **falso**, che è la verità per chiunque non stia dentro un cambio di schermata: l'avvio
 * dell'app, un'anteprima, una prova.
 *
 * ⚠️ **È uno stato e non un numero di millisecondi**, e la differenza conta: una durata scritta
 * qui sarebbe la seconda copia di `SCHERMO_MS`, e si scollerebbe il giorno che quella cambia.
 * Così invece l'attesa finisce **quando la dissolvenza finisce davvero**, qualunque cosa lei
 * faccia, compreso non animare affatto (entrando o uscendo dal visualizzatore la transizione è
 * `None`, e allora questo stato è falso dal primo fotogramma).
 */
val LocalArrivo: ProvidableCompositionLocal<State<Boolean>> =
    staticCompositionLocalOf { mutableStateOf(false) }

/**
 * Lo stato dell'entrata, che parte appena la schermata ha finito di arrivare.
 *
 * ⚠️⚠️ **ASPETTA LA DISSOLVENZA, DALLA `1.74`, E IL PERCHÉ È MISURATO** (riscontro dell'utente,
 * giro della `1.73`: *è l'animazione con cui appare all'avvio o all'uscita da una cartella che è
 * sbagliata*, e poi la sua scelta fra tre: *entrata dopo la dissolvenza*). Fino alla `1.73`
 * l'entrata partiva insieme al cambio di schermata, quindi la sua opacità si **moltiplicava**
 * per quella della schermata in arrivo, e lo stesso codice dava due animazioni diverse:
 * - **all'avvio**, dove non c'è nessuna dissolvenza, il FAB parte visibile al 65% e si vede
 *   tutta la crescita dal 75%;
 * - **tornando da una cartella**, a metà opacità (67 ms) la misura era **già al 90%**, quindi di
 *   tutta la crescita restavano l'ultimo decimo e il rimbalzo: un'apparizione seguita da
 *   un'oscillazione, che è un'altra cosa da uno zoom-in.
 *
 * ⚠️ **Aspettando, i due casi tornano a essere la stessa animazione**: durante la dissolvenza il
 * FAB sta fermo alla sua misura di partenza e arriva con la schermata, e l'entrata si gioca
 * intera dopo, a opacità piena.
 *
 * ⚠️⚠️ **FRA LA FINE DELLA DISSOLVENZA E IL PRIMO FOTOGRAMMA DELLA MOLLA PASSANO 4 FOTOGRAMMI,
 * ED È DELLA TRANSIZIONE E NON DI QUESTA ATTESA** (misurato sul banco, sulla schermata vera: la
 * dissolvenza dura 180 ms e la misura comincia a muoversi a 256). Un `Transition` porta
 * `currentState` su `targetState` un paio di fotogrammi dopo l'ultimo valore animato, e uno
 * ancora ne serve perché la molla produca un numero diverso dal suo inizio.
 * - ⚠️ **Provata anche la via a tempo** (`delay` della durata della dissolvenza): comincia 32 ms
 *   prima, cioè due fotogrammi, e in cambio vuole quel numero scritto in un secondo posto e non
 *   sa distinguere una transizione che anima da una che non anima. Con l'attesa così com'è,
 *   `EnterTransition.None` non fa aspettare niente, perché lo stato è già arrivato.
 * - ⚠️ **Non si compensa con un'attesa più corta**: quei fotogrammi non sono un errore da
 *   sottrarre, sono il momento in cui la schermata ha davvero finito di arrivare.
 */
@Composable
fun rememberEntrata(): Entrata {
    val arrivo = LocalArrivo.current
    val entrata = remember {
        Entrata(Animatable(ENTRA_DA), Animatable(ENTRA_ALFA))
    }
    /*
     * ⚠️ **La chiave dell'effetto è lo stato, non `Unit`**: così l'entrata parte nella stessa
     * ricomposizione in cui la schermata dichiara di essere arrivata, senza un giro in più per
     * osservarlo. E se un domani quello stato tornasse indietro, l'entrata ricomincerebbe, che è
     * il comportamento giusto e non un effetto collaterale da evitare.
     */
    val arrivata = !arrivo.value
    LaunchedEffect(arrivata) { if (arrivata) entrata.posa() }
    return entrata
}

/** La misura e l'opacità dell'entrata, applicate a chi le porta. */
fun Modifier.entering(entrata: Entrata): Modifier = graphicsLayer {
    scaleX = entrata.scala
    scaleY = entrata.scala
    alpha = entrata.opacita
}

/**
 * Da quanto parte la misura del tastino.
 *
 * ⚠️ **75 dalla `1.59`, ed è un numero suo** (riscontro del giro della `1.58`: *parti dal 75%
 * di dimensione senza cambiare durata*). Prima era 85, che con l'app in mano gli è sembrato
 * poco: la molla c'era ma il tratto che percorreva era corto.
 * ⚠️⚠️ **IL RIMBALZO NON È UN NUMERO A SÉ: è questo punto di partenza per una frazione fissa.**
 * La molla supera sempre il bersaglio del 16,3% dello scarto che deve colmare, e quella
 * frazione dipende dal **solo** smorzamento: partire da più in basso allunga il rimbalzo nella
 * stessa proporzione. Dalla `1.58` alla `1.59` questa costante è scesa di dieci punti e il
 * rimbalzo è passato da 0,98 a 1,63dp senza che nessuno lo chiedesse: chi la tocca sappia che
 * ne muove due.
 */
private const val ENTRA_DA = 0.75f

/**
 * Quanto è rigida la molla della misura.
 *
 * ⚠️⚠️ **UN NUMERO A MANO, E NON UNA DELLE COSTANTI DI COMPOSE, PERCHÉ LA RICHIESTA È UNA
 * DURATA** (riscontro del giro della `1.59`: *l'animazione deve durare 100ms in più, e avere
 * una netta decelerazione sul finale*). Con `StiffnessMedium` l'assestamento è di **147 ms**,
 * quindi il bersaglio è **247**, e nessuna delle rigidità che Compose ha battezzato ci cade
 * vicino: la sotto (`StiffnessMediumLow`, 400) dà 271, cioè 24 ms oltre la richiesta.
 * ⚠️ **Lo smorzamento invece resta `DampingRatioMediumBouncy`**, ed è la ragione per cui il
 * rimbalzo non cambia di un centesimo: dipende solo da lui.
 * ⚠️ **La decelerazione finale viene di conseguenza e non è un secondo intervento**: una molla
 * più morbida percorre lo stesso tratto in più tempo, quindi l'ultimo pezzo lo fa più adagio.
 * Misurato: il colmo del rimbalzo passa da 94 a 166 ms, e resta lo stesso 4,08% oltre la misura
 * piena.
 */
private const val ENTRA_MOLLA = 480f

/**
 * Da quanta opacità parte.
 *
 * ⚠️ **Più bassa della scala di proposito**: l'opacità è la parte che si nota di meno, quindi
 * può permettersi un tratto più lungo senza farsi vedere, e serve a dire 'sta arrivando'
 * mentre la misura dice soltanto 'si sta posando'.
 */
private const val ENTRA_ALFA = 0.65f

/**
 * Quanto dura la dissolvenza.
 *
 * ⚠️ **180 dalla `1.59`, ed è un numero suo** (riscontro del giro della `1.58`: *fa' durare 180
 * ms la dissolvenza da 65% a 100% di opacità*). Prima era 140.
 * ⚠️⚠️ **DEVE FINIRE PRIMA DELLA MOLLA, e fino alla `1.59` NON ci finiva**: il rimbalzo si vede
 * solo se il tastino è già pieno quando arriva, o si legge come uno sfarfallio invece che come
 * un movimento. Con la misura giusta dell'assestamento (147 ms) questi 180 arrivavano **dopo**,
 * e il margine che questa nota dichiarava non esisteva; con i 247 della `1.60` c'è davvero.
 */
private const val ENTRA_ALFA_MS = 180


package io.github.roccobot.aiv

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
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
    private val velo: Animatable<Float, AnimationVector1D>,
    private val alza: Animatable<Float, AnimationVector1D>
) {
    internal val scala: Float get() = misura.value
    internal val opacita: Float get() = velo.value

    /**
     * L'ombra del tastino in questo istante, che arriva a [piena] quando il movimento finisce.
     *
     * ⚠️ **Si legge in composizione e non a tempo di disegno**, al contrario delle altre due:
     * `shadowElevation` è un parametro di `Surface`, quindi il valore va saputo mentre si
     * compone. Il prezzo è una ricomposizione per fotogramma del solo tastino, per il quarto di
     * secondo dell'entrata.
     */
    fun lift(piena: Dp): Dp = piena * alza.value

    internal suspend fun posa() = coroutineScope {
        launch { velo.animateTo(1f, tween(ENTRA_ALFA_MS, easing = LinearOutSlowInEasing)) }
        launch { alza.animateTo(1f, tween(ENTRA_OMBRA_MS, easing = LinearEasing)) }
        misura.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = ENTRA_MOLLA)
        )
    }
}

/** Lo stato dell'entrata, che parte da sé appena entra in composizione. */
@Composable
fun rememberEntrata(): Entrata {
    val entrata = remember {
        Entrata(Animatable(ENTRA_DA), Animatable(ENTRA_ALFA), Animatable(0f))
    }
    LaunchedEffect(Unit) { entrata.posa() }
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

/**
 * Quanto ci mette l'ombra a comparire.
 *
 * ⚠️⚠️ **RICHIESTA DELL'UTENTE, giro della `1.59`** (*l'ombra arriva alla fine, con un fade in
 * molto graduale*): il tastino entra **piatto** e si stacca dal fondo mentre si posa, che è il
 * verso giusto di una cosa che sta arrivando.
 * ⚠️ **Tanto quanto la molla, e la rampa è lineare**: 'arriva alla fine' dice dove deve
 * finire, 'molto graduale' dice che non deve avere un momento in cui compare. Una curva
 * accelerata la terrebbe invisibile per due terzi e poi la farebbe apparire, che è il
 * contrario.
 * ⚠️⚠️ **VA RIMISURATO SE SI TOCCA [ENTRA_MOLLA] O [ENTRA_DA]**: è l'assestamento di quella
 * molla, non un numero indipendente, e Compose non lo espone prima di far girare
 * l'animazione. Oggi vale 247 ms.
 */
private const val ENTRA_OMBRA_MS = 247

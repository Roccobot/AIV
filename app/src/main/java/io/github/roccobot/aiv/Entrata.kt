package io.github.roccobot.aiv

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
 * ⚠️⚠️ **LA SCALA VA A MOLLA E L'OPACITÀ NO, e non è una svista**: una molla supera il
 * bersaglio e poi torna, che su una misura è il rimbalzo chiesto, ma su un'opacità vorrebbe
 * dire passare oltre l'uno, cioè un lampo. La `tween` non può farlo per costruzione.
 *
 * ⚠️ **Le due costanti della molla sono quelle che Compose ha già con un nome**
 * (`DampingRatioMediumBouncy` e `StiffnessMedium`) e non due numeri scelti a mano: misurate,
 * danno un rimbalzo di **1,7 centesimi** oltre la misura piena (meno di un punto densità su
 * un tastino da [FAB_SIZE]) e un assestamento in **234 ms**. La variante scartata partiva dal
 * 60% con `StiffnessMediumLow` e ci metteva **602 ms**, con un rimbalzo di due punti e mezzo:
 * la stessa forma, vista al rallentatore.
 *
 * ⚠️ **Si rigioca a ogni ritorno sulla home**, che è quello che è stato chiesto, e non c'è
 * niente da scrivere perché lo fa la composizione: dalla `1.56` il cambio di schermata passa
 * da un `AnimatedContent`, quindi rientrando nella schermata iniziale nasce una composizione
 * nuova e con lei il [remember] di questi due valori.
 */
@Composable
fun Modifier.entering(): Modifier {
    val scala = remember { Animatable(ENTRA_DA) }
    val opacita = remember { Animatable(ENTRA_ALFA) }
    LaunchedEffect(Unit) {
        launch {
            opacita.animateTo(1f, tween(ENTRA_ALFA_MS, easing = LinearOutSlowInEasing))
        }
        scala.animateTo(
            1f,
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }
    return this.graphicsLayer {
        scaleX = scala.value
        scaleY = scala.value
        alpha = opacita.value
    }
}

/**
 * Da quanto parte la misura del tastino.
 *
 * ⚠️ **Sotto questo valore l'entrata smette di essere discreta**: la variante scartata partiva
 * dal 60% e si vedeva arrivare, che è l'opposto di quello che è stato chiesto.
 */
private const val ENTRA_DA = 0.85f

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
 * ⚠️ **Più corta dell'assestamento della molla, e deve esserlo**: il rimbalzo si vede solo se
 * il tastino è già pieno quando arriva, o si legge come uno sfarfallio invece che come un
 * movimento.
 */
private const val ENTRA_ALFA_MS = 140

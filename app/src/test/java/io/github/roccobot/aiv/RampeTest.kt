package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Il lavoro delle rampe durante uno scorrimento, nella `6.28` (rilievi D26 e D34 della revisione
 * della 6.25, voce C2 del rapporto): si conta con [DrawWork], come vuole la sua scelta B1, e si
 * toglie solo il lavoro rifatto identico.
 *
 * ⚠️ **Dieci fotogrammi, dieci posizioni o altezze**: è lo scorrimento visto da chi disegna, senza
 * il gesto, che qui non aggiungerebbe niente.
 * ⚠️ **Il banco disegna solo quando si legge l'immagine** (`captureToImage`): senza, i conti
 * restano a zero col difetto dentro, ed è successo alla prima stesura. Il primo disegno si fa prima
 * di azzerare il conto, perché il primo gradiente e il primo programma servono comunque.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RampeTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **L'intestazione che si accorcia non ricompila il programma del rumore a ogni fotogramma.**
     *
     * ⚠️ Il gradiente invece si rifà, e deve: con l'altezza cambia la rampa.
     * ⚠️ **Controprovata** col programma costruito dentro `drawWithCache`: dieci altezze, undici
     * programmi.
     */
    @Test
    fun `l'intestazione che si chiude compila il programma una volta`() {
        var alto by mutableIntStateOf(200)
        banco.setContent {
            Box(Modifier.fillMaxWidth().height(alto.dp).frontWash(Color.Red, 0.dp, 0.dp, 0.dp) { 1f })
        }
        banco.waitForIdle()
        banco.onRoot().captureToImage()
        DrawWork.programs = 0
        repeat(10) {
            alto -= 10
            banco.waitForIdle()
            banco.onRoot().captureToImage()
        }
        assertEquals("Il programma si è ricompilato mentre l'intestazione si chiudeva", 0, DrawWork.programs)
    }

    /**
     * **Una cella con l'ombra della selezione che scorre non rifà il gradiente a ogni fotogramma.**
     *
     * ⚠️ La rampa è la stessa in coordinate della finestra, e la cella si sposta sotto: cambia solo
     * dove la si legge.
     * ⚠️ **Controprovata** con la rampa calcolata nelle coordinate della cella: dieci posizioni,
     * dieci gradienti.
     */
    @Test
    fun `l'ombra della selezione che scorre non rifà il gradiente`() {
        var giu by mutableIntStateOf(0)
        banco.setContent {
            Column {
                Box(Modifier.offset(y = giu.dp).size(80.dp).pickShade(top = 600f, behind = false))
            }
        }
        banco.waitForIdle()
        banco.onRoot().captureToImage()
        DrawWork.gradients = 0
        repeat(10) {
            giu += 7
            banco.waitForIdle()
            banco.onRoot().captureToImage()
        }
        assertEquals("Il gradiente si è rifatto a ogni posizione", 0, DrawWork.gradients)
    }
}

package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
 * Il vetro della pillola durante uno scorrimento, nella `6.28` (rilievo F38 della revisione della
 * 6.25, voce C3 del rapporto): si conta con [DrawWork], come vuole la sua scelta B1, e il lavoro
 * rifatto identico si toglie.
 *
 * ⚠️ **Il vetro è il riempimento di fabbrica delle impostazioni** (`Settings.pillFill`), ma non di
 * `PillLook()`, che parte pieno: la scena lo fornisce esplicitamente. Ridisegna a ogni fotogramma in cui
 * il contenuto sotto si sposta: quella registrazione è inevitabile, ed è dichiarata. L'effetto
 * invece dipende solo dai due cursori.
 * ⚠️ **Il banco disegna solo quando si legge l'immagine**, come in `RampeTest`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VetroTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **Il contenuto che scorre sotto il vetro non fa ricreare l'effetto a ogni fotogramma.**
     *
     * ⚠️ **Controprovata** con l'effetto creato dentro il disegno: dieci posizioni, dieci effetti.
     */
    @Test
    fun `il vetro non ricrea l'effetto mentre si scorre`() {
        var giu by mutableIntStateOf(0)
        banco.setContent {
            AivTheme(darkTheme = false) {
              CompositionLocalProvider(LocalPillLook provides PillLook(fill = PillFill.GLASS)) {
                val dietro = rememberBackdrop()
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.offset(y = giu.dp).size(300.dp).backdropSource(dietro)
                            .background(Color.Blue)
                    )
                    Box(Modifier.align(Alignment.BottomCenter).size(120.dp, 56.dp).pillFill(dietro))
                }
              }
            }
        }
        banco.waitForIdle()
        banco.onRoot().captureToImage()
        DrawWork.glass = 0
        repeat(10) {
            giu += 7
            banco.waitForIdle()
            banco.onRoot().captureToImage()
        }
        assertEquals("Il vetro ha ricreato l'effetto a ogni posizione", 0, DrawWork.glass)
    }
}

package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Il FAB che **si rimpicciolisce fino a sparire** andando dove non c'è, dalla `2.11`.
 *
 * ⚠️⚠️ **È IL PUNTO A DEL CAMPO LIBERO** (*quando dal menu del FAB approdo ad una schermata senza
 * FAB (esempio → Impostazioni), il pulsante deve sparire rimpicciolendosi fino a sparire*), e le
 * due cose che possono rompersi sono di specie diversa: **quando** l'uscita parte, che è una
 * condizione con due termini e una negazione, e **che cosa** fa, che è una scala dentro un
 * `graphicsLayer`.
 *
 * ⚠️⚠️ **LA SCALA NON SI VEDE NELL'ALBERO SEMANTICO, E PER QUESTO QUI SI GUARDANO I PIXEL**: un
 * `graphicsLayer` cambia il disegno e non il layout, quindi il riquadro del FAB resta quello di
 * prima a ogni fotogramma. Una prova che misurasse `boundsInRoot` passerebbe con e senza la
 * correzione, cioè misurerebbe niente.
 *
 * ⚠️ **Che cosa NON vede**: se il gesto si **legga** come un rimpicciolimento, che è percezione e
 * si guarda sul telefono, e come si compone con la dissolvenza della schermata, che dipende dalla
 * resa vera. Vede che il tasto si stringe e che alla fine non c'è più: due fatti.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UscitaFabTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **Il tasto si stringe mentre esce, e alla fine non copre più un pixel.**
     *
     * ⚠️ **Le tre misure sono una corsa**: pieno prima, meno di prima a metà, zero alla fine.
     * Quella di mezzo è la sola che dice *rimpicciolisce* invece di *sparisce*, e senza di lei la
     * prova resterebbe verde anche con una dissolvenza secca al posto della scala.
     * ⚠️ **Controprovata** togliendo il fattore [via] dalla scala del tasto: il conto dei pixel
     * resta quello di partenza a ogni fotogramma, e cadono la seconda e la terza misura.
     */
    @Test
    fun `il FAB si rimpicciolisce fino a sparire`() {
        var senza by mutableStateOf(false)
        banco.setContent {
            AivTheme(darkTheme = false) {
                CompositionLocalProvider(LocalSenzaFab provides senza) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TapHoldFab(
                            label = "apri",
                            container = TINTA,
                            ink = Color.White,
                            holdLabel = "tieni premuto",
                            onTap = {},
                            onHold = {},
                            glyph = { Box(Modifier.size(8.dp)) }
                        )
                    }
                }
            }
        }
        banco.waitForIdle()
        val pieno = quantiPixel()
        assertTrue("il FAB non si vede affatto: la prova non guarda niente", pieno > 0)

        banco.mainClock.autoAdvance = false
        senza = true
        /*
         * ⚠️⚠️ **PRIMA LA RICOMPOSIZIONE, POI IL TEMPO, E L'ORDINE È MISURATO**: col clock fermo
         * `advanceTimeByFrame` da solo non fa ripartire il `LaunchedEffect` che accende l'uscita,
         * quindi la scala resta a uno e la prova fallisce **col codice giusto** (1426 pixel a
         * metà corsa, cioè quelli di partenza). Con `waitForIdle` prima, che ricompone senza
         * muovere l'orologio, l'animazione parte e a metà ne restano 994.
         */
        banco.waitForIdle()
        banco.mainClock.advanceTimeBy(META)
        val meta = quantiPixel()
        assertTrue(
            "a metà corsa il tasto copre $meta pixel invece di stringersi (partiva da $pieno)",
            meta in 1..<pieno
        )

        banco.mainClock.advanceTimeBy(META * 2)
        assertEquals("a corsa finita il tasto deve essere sparito", 0, quantiPixel())
    }

    /**
     * **Quando l'uscita parte: solo per chi se ne va, e solo verso una schermata senza FAB.**
     *
     * ⚠️ **I quattro casi sono le quattro combinazioni che contano**, e tre di loro devono dire di
     * no: la schermata che **arriva** non si congeda mai (è il termine che un `!` di troppo
     * toglierebbe), e andare da una griglia a un'altra lascia il FAB dov'è.
     */
    @Test
    fun `l'uscita parte solo andando dove il FAB non c'è`() {
        val casa = Screen.Folders(forStart = false)
        val cartella = Screen.Grid(bucket = 1L, name = "prova")

        assertTrue("da casa alle impostazioni il FAB se ne va", casa.senzaFabVerso(Screen.Settings))
        assertTrue(
            "da una cartella al visualizzatore il FAB se ne va",
            cartella.senzaFabVerso(Screen.Viewer)
        )
        assertFalse(
            "chi arriva non si congeda: è la schermata di destinazione",
            Screen.Settings.senzaFabVerso(Screen.Settings)
        )
        assertFalse("da casa a una cartella il FAB resta", casa.senzaFabVerso(cartella))
    }

    /** Quanti pixel dello schermo porta il colore del FAB, cioè quanto è grande quello che si vede. */
    private fun quantiPixel(): Int {
        val pixel = banco.onRoot().captureToImage().toPixelMap()
        var quanti = 0
        for (y in 0 until pixel.height) {
            for (x in 0 until pixel.width) {
                if (pixel[x, y] == TINTA) quanti++
            }
        }
        return quanti
    }
}

/**
 * Il colore del FAB in questa prova: uno che non compare da nessun'altra parte nella scena.
 *
 * ⚠️ **Non è l'accento dell'app di proposito**: il fondo della schermata e il tasto porterebbero
 * colori vicini, e il conto dei pixel misurerebbe anche quello che non è il tasto.
 */
private val TINTA = Color(0xFFFF00FF)

/** Metà della corsa dell'uscita, in millisecondi: vedi `VIA_MS` in `ActionPad.kt`. */
private const val META = 50L

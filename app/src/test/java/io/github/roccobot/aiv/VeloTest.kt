package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * **Il velo della `4.44`**: il 30%, nero sul tema chiaro e bianco sullo scuro, che compare in
 * [VEIL_IN_MS] e se ne va subito (sua richiesta sul giro della `4.43`, note B e C).
 *
 * ⚠️ **La grafica vera serve alla seconda prova**, che legge i pixel che [AppVeil] dipinge.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VeloTest {

    @get:Rule
    val banco = createComposeRule()

    /** ⚠️ La mappa del velo è un oggetto di processo: una prova che la lascia sporca fa cadere la dopo. */
    @After
    fun pulisci() {
        VeilStage.clear()
    }

    /**
     * **Il velo pieno è il 30% sui due temi, nero sul chiaro e bianco sullo scuro.**
     * ⚠️⚠️ **CONTROPROVATA** rimettendo il velo nero su tutti e due i temi: sullo scuro il colore
     * torna nero.
     */
    @Test
    fun `il velo e il 30 per cento, nero sul chiaro e bianco sullo scuro`() {
        val scuro = mutableStateOf(false)
        banco.setContent {
            CompositionLocalProvider(LocalAivDepth provides PanelDepth.BLUR) {
                AivTheme(darkTheme = scuro.value) { AppPatina { 1f } }
            }
        }
        banco.waitForIdle()
        assertEquals("sul tema chiaro il velo pieno", 0.30f, VeilStage.dose, 1e-4f)
        assertEquals("sul tema chiaro il velo è nero", Color.Black, VeilStage.colore)

        scuro.value = true
        banco.waitForIdle()
        assertEquals("sul tema scuro il velo pieno", 0.30f, VeilStage.dose, 1e-4f)
        assertEquals("sul tema scuro il velo è bianco", Color.White, VeilStage.colore)
    }

    /**
     * **Il velo compare in 800 ms, e se ne va subito.**
     *
     * ⚠️ Si legge l'opacità dipinta su un fondo bianco: col nero sopra, il rosso che resta dice
     * quanto velo c'è. A 100 ms la curva è al 35% circa del pieno, cioè un velo intorno al 10%.
     * ⚠️⚠️ **CONTROPROVATA** dipingendo la dose richiesta invece del velo che sale (com'era fino
     * alla `4.43`): a 100 ms il velo è già pieno.
     */
    @Test
    fun `il velo sale in 800 ms e scende subito`() {
        banco.mainClock.autoAdvance = false
        banco.setContent {
            Box(Modifier.size(40.dp).background(Color.White).testTag("fondo")) {
                AppVeil(Modifier.size(40.dp))
            }
        }
        banco.mainClock.advanceTimeByFrame()
        val chi = Any()
        // ⚠️ With the clock stopped the change to the map is only applied when someone says so,
        // and without it the veil's flow never hears of it.
        banco.runOnIdle {
            VeilStage.at(chi, 0.30f, 0.30f, Color.Black)
            Snapshot.sendApplyNotifications()
        }
        banco.mainClock.advanceTimeByFrame()

        banco.mainClock.advanceTimeBy(100)
        val presto = velo()
        assertTrue("a 100 ms il velo doveva essere ancora in salita: $presto", presto in 0.03f..0.2f)

        banco.mainClock.advanceTimeBy(VEIL_IN_MS.toLong())
        assertEquals("dopo 800 ms il velo doveva essere pieno", 0.30f, velo(), 0.02f)

        banco.runOnIdle {
            VeilStage.off(chi)
            Snapshot.sendApplyNotifications()
        }
        banco.mainClock.advanceTimeByFrame()
        banco.mainClock.advanceTimeByFrame()
        assertEquals("tolta la richiesta, il velo doveva sparire subito", 0f, velo(), 0.01f)
    }

    /** Quanto velo nero c'è sul fondo bianco, al centro. */
    private fun velo(): Float {
        val mappa = banco.onNodeWithTag("fondo").captureToImage().toPixelMap()
        return 1f - mappa[mappa.width / 2, mappa.height / 2].red
    }
}

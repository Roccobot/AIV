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
import org.junit.Assert.assertFalse
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

    /**
     * **Fra un menu che esce e una scheda che entra il velo resta pieno** (sua nota su `4.44-02`:
     * dal menu di una miniatura a 'Info' *c'è un lampeggio, mi pare che la sfocatura se ne vada e
     * ritorni*).
     * ⚠️ Il passaggio si riproduce sulla mappa: il menu cala in [MENU_STEPS] fotogrammi (la sua
     * uscita di 75 ms), e nello stesso tempo la scheda sale da zero col suo avanzamento.
     * ⚠️⚠️ **CONTROPROVATA** sul velo della `4.44`, che scendeva con la dose del menu e risaliva in
     * 800 ms per la scheda: a metà passaggio il velo era sotto il 10%.
     */
    @Test
    fun `fra un menu che esce e una scheda che entra il velo resta pieno`() {
        banco.mainClock.autoAdvance = false
        banco.setContent {
            Box(Modifier.size(40.dp).background(Color.White).testTag("fondo")) {
                AppVeil(Modifier.size(40.dp))
            }
        }
        banco.mainClock.advanceTimeByFrame()
        val menu = Any()
        val scheda = Any()
        banco.runOnIdle {
            VeilStage.at(menu, 0.30f, 0.30f, Color.Black)
            Snapshot.sendApplyNotifications()
        }
        banco.mainClock.advanceTimeBy(VEIL_IN_MS.toLong() + 100)
        assertEquals("il velo del menu doveva essere pieno", 0.30f, velo(), 0.02f)

        var minimo = 1f
        for (i in 1..MENU_STEPS * 3) {
            val esce = (1f - i / MENU_STEPS.toFloat()).coerceAtLeast(0f)
            val entra = (i / (MENU_STEPS * 3f)).coerceAtMost(1f)
            banco.runOnIdle {
                VeilStage.at(menu, 0.30f * esce, 0.30f, Color.Black)
                VeilStage.at(scheda, 0.30f * entra, 0.30f, Color.Black)
                Snapshot.sendApplyNotifications()
            }
            banco.mainClock.advanceTimeByFrame()
            minimo = minOf(minimo, velo())
        }
        assertTrue("fra il menu e la scheda il velo è sceso a $minimo", minimo > 0.27f)
    }

    /**
     * **A window that leaves while another surface is up keeps its blur full** (his note on
     * `4.45-01`: *lampeggio 'attenuato' ma ancora visibile*, from a thumbnail's menu to 'Info').
     * ⚠️ The window blur itself the bench cannot see (the window manager answers it): this checks
     * the decision [WindowVeil] takes at every frame of a window's exit.
     * ⚠️⚠️ **COUNTER-PROVED** with the decision left as it was until 4.45 (the leaving window
     * follows its own progress): the first case comes out `false`.
     */
    @Test
    fun `la finestra che esce tiene la sfocatura se un'altra superficie e in scena`() {
        assertTrue("menu a metà uscita, scheda già piena", blurHeld(own = 0.5f, stage = VEIL_DOSE))
        assertFalse("menu a metà uscita e da solo", blurHeld(own = 0.5f, stage = VEIL_DOSE * 0.5f))
        assertFalse("una superficie piena non ha niente da tenere", blurHeld(own = 1f, stage = VEIL_DOSE))
        assertFalse("menu uscito e scena vuota", blurHeld(own = 0f, stage = 0f))
    }

    /** Quanto velo nero c'è sul fondo bianco, al centro. */
    private fun velo(): Float {
        val mappa = banco.onNodeWithTag("fondo").captureToImage().toPixelMap()
        return 1f - mappa[mappa.width / 2, mappa.height / 2].red
    }
}

/** The frames of a menu's exit, 75 ms at 60 frames per second. */
private const val MENU_STEPS = 5

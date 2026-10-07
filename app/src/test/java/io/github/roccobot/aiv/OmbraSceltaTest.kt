package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import kotlin.math.pow

/**
 * **The shade over the grid above the selection sheet** (`4.45`, his note E, with his mockup).
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️ `@GraphicsMode(NATIVE)` in a class of its own, because it reads pixels.
 * ⚠️ The scene is a white grid with one cell and a tick on it, drawn the way `GridScreen` does: the
 * shade behind the grid's content, then the cell, then the shade inside the cell, then the tick.
 * ⚠️⚠️ **COUNTER-PROVED** on the grid's half, drawing it in front instead of behind (that is, not
 * at all on the grid): the gap beside the cell stays white while the cell is shaded.
 * ⚠️ **What it does not prove**: that `Thumbnail` puts the shade before the tick. The order lives in
 * `GridScreen`, which this scene copies; the tick's line here checks the shade's own drawing.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OmbraSceltaTest {

    @get:Rule
    val banco = createComposeRule()

    @Test
    fun `l'ombra scende fino al bordo della scheda e la spunta resta sopra`() {
        banco.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalAivLight provides true) {
                Box(
                    Modifier.size(LATO.dp, ALTO.dp).background(Color.White).testTag("scena")
                        .pickShade(BORDO, behind = true)
                ) {
                    Box(Modifier.offset(x = CELLA_X.dp).size(CELLA.dp, ALTO.dp).background(Color.White)) {
                        Box(Modifier.size(CELLA.dp, ALTO.dp).pickShade(BORDO, behind = false))
                        Box(
                            Modifier.align(Alignment.BottomCenter).offset(y = (-SPUNTA_SU).dp)
                                .size(SPUNTA.dp).background(VERDE)
                        )
                    }
                }
            }
        }
        banco.waitForIdle()
        val mappa = banco.onNodeWithTag("scena").captureToImage().toPixelMap()
        fun buio(x: Int, y: Int) = 1f - mappa[x, y].red

        val alto = SHADE_TALL.value
        assertEquals("sopra l'ombra il bianco è intatto", 0f, buio(CELLA_X + 5, (BORDO - alto - 5).toInt()), 0.01f)
        assertEquals("sul bordo della scheda l'ombra è al massimo", SHADE_MAX, buio(CELLA_X + 5, (BORDO - 1).toInt()), 0.03f)
        assertEquals(
            "a metà altezza l'ombra segue la curva del mockup, a metà della sua forza dalla 4.48",
            SHADE_MAX * 0.5f.pow(1.7f), buio(CELLA_X + 5, (BORDO - alto / 2).toInt()), 0.03f
        )
        val y = (BORDO - 10).toInt()
        assertEquals(
            "la cella e lo spazio accanto devono avere la stessa ombra",
            buio(CELLA_X + 5, y), buio(CELLA_X - 5, y), 0.01f
        )
        val spunta = mappa[CELLA_X + CELLA / 2, ALTO - SPUNTA_SU - SPUNTA / 2]
        assertTrue("la spunta doveva restare piena sopra l'ombra: $spunta", abs(spunta.green - VERDE.green) < 0.02f)
    }

    private fun abs(f: Float) = kotlin.math.abs(f)
}

private const val LATO = 200
private const val ALTO = 400
private const val CELLA = 80
private const val CELLA_X = 60
private const val SPUNTA = 20
private const val SPUNTA_SU = 20

/** The sheet's top edge in the window: the scene starts at the window's top at density 1. */
private const val BORDO = 390f
private val VERDE = Color(0xFF2E9E6B)

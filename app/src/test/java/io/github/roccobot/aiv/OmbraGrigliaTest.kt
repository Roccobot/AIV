package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

/**
 * **The selection's shade on the real grid screen: from glass to glass, down to the sheet**
 * (`4.48`, his note on `4.46-04`: *invece di essere mostrata sotto la bottomsheet, l'ombra appare
 * in un rettangolo che non dovrebbe esserci*).
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️⚠️ **It mounts `GridScreen` itself**: `OmbraSceltaTest` copies the scene, and the defect lived
 * in WHERE the screen put the shade (on the grid, inside its margins and above the sheet), which a
 * copy cannot see.
 * ⚠️ Few pictures, so under the last row the grid is empty: the shade is measured there, in the
 * left margin and in the middle, a few pixels above the sheet's edge.
 * ⚠️⚠️ **COUNTER-PROVED** twice: putting the shade back on the grid, the margin stays as white as
 * the page; putting back the 4.46 strength (0.49), the last check fails.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(shadows = [OmbraArchivio::class], qualifiers = "w393dp-h873dp")
class OmbraGrigliaTest {

    @get:Rule
    val banco = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun onboardingGiaVisto() {
        runBlocking {
            Hint.COLUMNS.remember(app)
            Hint.BIN_EMPTY.remember(app)
        }
    }

    @Test
    fun `l'ombra della selezione arriva ai bordi e fino alla scheda`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    GridScreen(
                        title = "Cartella di prova",
                        items = (1..3).map { Uri.parse("file:///finta/$it.jpg") },
                        highlight = null,
                        onOpen = {},
                        onBack = {},
                        onChanged = {},
                        onSearch = {},
                        onSearchHere = {}
                    )
                }
            }
        }
        banco.waitForIdle()
        banco.onNodeWithContentDescription(app.getString(R.string.pick_actions)).performTouchInput { longClick() }
        banco.mainClock.advanceTimeBy(2_000)
        banco.waitForIdle()

        val mappa = banco.onRoot().captureToImage().toPixelMap()
        // The sheet's top edge: up from its 'Sposta' key, the last row of the sheet's own colour.
        val tasto = banco.onNodeWithText(app.getString(R.string.pick_move)).fetchSemanticsNode().boundsInRoot
        val x = mappa.width / 2
        val foglio = mappa[x, tasto.top.roundToInt() - 2]
        var bordo = tasto.top.roundToInt() - 2
        while (bordo > 0 && simile(mappa, x, bordo - 1, foglio)) bordo--
        val sopra = bordo - 3
        val pagina = mappa[2, 2 * mappa.height / 5]
        fun buio(px: Int) = (pagina.red - mappa[px, sopra].red).coerceAtLeast(0f)

        assertTrue("al centro, sopra la scheda, l'ombra doveva esserci: ${buio(x)}", buio(x) > SHADE_MAX * 0.6f)
        assertTrue("nel margine di sinistra l'ombra doveva esserci: ${buio(2)}", buio(2) > SHADE_MAX * 0.6f)
        assertEquals("ai bordi e al centro l'ombra doveva essere la stessa", buio(x), buio(2), 0.02f)
        assertTrue("l'ombra non doveva superare il suo massimo: ${buio(x)}", buio(x) < SHADE_MAX + 0.03f)
        // ⚠️ His figure, not the constant: half of the 49% he saw in 4.46.
        assertTrue("l'ombra doveva essere la metà di quella della 4.46: ${buio(x)}", buio(x) < 0.49f * 0.6f)
    }

    private fun simile(m: PixelMap, x: Int, y: Int, c: androidx.compose.ui.graphics.Color) =
        kotlin.math.abs(m[x, y].red - c.red) < 0.01f && kotlin.math.abs(m[x, y].green - c.green) < 0.01f &&
            kotlin.math.abs(m[x, y].blue - c.blue) < 0.01f
}

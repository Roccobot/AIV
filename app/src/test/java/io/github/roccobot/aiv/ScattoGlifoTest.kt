package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * The app's mark coming back on the round key after a scroll, read from the pixels (note C on the
 * 4.32 round: *il simbolo quasi del tutto disegnato si ferma per un attimo (visibile) con un pezzo
 * mancante (parte superiore del cerchio) che poi appare con uno scatto unico*).
 *
 * ⚠️ The cause: the mark is drawn a few dp above its own box (the optical lift), and while its
 * opacity is below one the layer that fades it was drawn offscreen, in a buffer as large as the box,
 * which cut the disc's top. At full opacity the buffer went, and the piece came back in one frame.
 * ⚠️ `@GraphicsMode(NATIVE)` in a class of its own, because it reads pixels (`Rules.md`, § 'Quando
 * si scrive una prova, e quando no').
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScattoGlifoTest {

    @get:Rule
    val banco = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<android.app.Application>()

    /**
     * **While the mark comes back, its top moves frame by frame, without a final jump.**
     *
     * ⚠️ The clock is stopped and every frame is captured: at the end the mark was whole even with
     * the defect, which lived in the frames of the fade.
     */
    @Test
    fun `il marchio torna sul tondo senza scatto finale`() {
        banco.mainClock.autoAdvance = false
        banco.setContent { Griglia() }
        banco.mainClock.advanceTimeBy(NASCITA)
        // ⚠️ At rest the mark is where it will be at the end: its box and the key's fill are read
        // here, and every frame keeps only the row where its ink begins, or a full run of the bench
        // runs out of memory.
        val marchio = banco.onNodeWithTag(MARK_TAG, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val dp = app.resources.displayMetrics.density
        val fondo = banco.onRoot().captureToImage().toPixelMap()[(marchio.left - 3 * dp).toInt(), marchio.center.y.toInt()]
        fun cima(m: PixelMap): Int? {
            for (y in (marchio.top - 6 * dp).toInt() until marchio.bottom.toInt()) {
                for (x in marchio.left.toInt() until marchio.right.toInt()) {
                    val c = m[x, y]
                    if (abs(c.red - fondo.red) + abs(c.green - fondo.green) + abs(c.blue - fondo.blue) > 0.15f) return y
                }
            }
            return null
        }
        val scena = banco.onRoot().fetchSemanticsNode().size
        banco.onRoot().performTouchInput {
            down(Offset(scena.width * LATO, scena.height * DA))
            moveTo(Offset(scena.width * LATO, scena.height * A))
            advanceEventTime(FERMO)
            up()
        }
        val cime = mutableListOf<Int>()
        repeat(240) {
            banco.mainClock.advanceTimeByFrame()
            cima(banco.onRoot().captureToImage().toPixelMap())?.let { cime += it }
        }
        assertTrue("Il marchio non è mai tornato", cime.isNotEmpty())
        var salto = 0
        for (i in 1 until cime.size) salto = maxOf(salto, abs(cime[i] - cime[i - 1]))
        assertTrue("La cima del marchio salta di $salto px fra due fotogrammi: $cime", salto <= 1.2f * dp)
    }

    /** A folder's grid with the sliding pill, whose round key carries the mark. */
    @Composable
    private fun Griglia() {
        AivTheme(darkTheme = false) {
            CompositionLocalProvider(LocalPillLook provides PillLook(PhonePill.SLIDE)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    GridScreen(
                        title = "Cartella di prova",
                        items = (1..60).map { Uri.parse("file:///finta/$it.jpg") },
                        highlight = null,
                        onOpen = {},
                        onBack = {},
                        onChanged = {},
                        onSearch = {},
                        onSearchHere = {},
                        onBin = {},
                        onSettings = {}
                    )
                }
            }
        }
    }
}

/** As in `SaltiTest`: the first composition and its effects. */
private const val NASCITA = 1_000L

/** As in `SaltiTest`. */
private const val FERMO = 300L

/** As in `SaltiTest`. */
private const val DA = 0.8f

/** As in `SaltiTest`. */
private const val A = 0.2f

/** As in `SaltiTest`. */
private const val LATO = 0.25f

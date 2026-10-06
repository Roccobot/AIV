package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * **The accent edge is off: panels are bare** (his request of 2026-10-06, variant A of the
 * mockup), and it comes back by flipping [ACCENT_EDGE].
 *
 * ⚠️ **The panel is drawn with the real modifiers**, [edged] and [edgedTop], on a surface of a
 * known colour: every pixel of the panel must be that colour, the outer ring included, where
 * the edge used to fall (inside on a panel, outside on a bottom sheet, which is why the sheet
 * sits inside a larger box).
 * ⚠️ **Counter-tested** with `ACCENT_EDGE = true`: the ring turns accent and both cases fail.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class BordoTest {

    @get:Rule
    val banco = createComposeRule()

    private val superficie = Color(0xFF808080)

    @Test
    fun `un pannello non ha il bordo d'accento`() {
        banco.setContent {
            CompositionLocalProvider(LocalAivLight provides true) {
                Box(
                    Modifier.testTag("pannello").size(120.dp)
                        .background(superficie, RoundedCornerShape(20.dp)).edged(20.dp)
                )
            }
        }
        val px = banco.onNodeWithTag("pannello").captureToImage().toPixelMap()
        val mezzo = px.height / 2
        for (x in 0 until 6) {
            assertEquals("pixel $x sul fianco sinistro", superficie.toArgb(), px[x, mezzo].toArgb())
        }
    }

    @Test
    fun `una scheda in fondo non ha il bordo d'accento sopra`() {
        banco.setContent {
            CompositionLocalProvider(LocalAivLight provides false) {
                Box(Modifier.testTag("fondo").size(160.dp).background(Color.Black)) {
                    Box(
                        Modifier.size(160.dp).padding(top = 20.dp)
                            .background(superficie, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                            .edgedTop(28.dp)
                    )
                }
            }
        }
        val px = banco.onNodeWithTag("fondo").captureToImage().toPixelMap()
        val mezzo = px.width / 2
        val alto = (0 until px.height).first { px[mezzo, it].toArgb() != Color.Black.toArgb() }
        assertEquals("sopra la scheda c'è solo la superficie", superficie.toArgb(), px[mezzo, alto].toArgb())
    }

}

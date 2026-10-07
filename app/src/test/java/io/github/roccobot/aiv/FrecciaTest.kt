package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * **A dashed arrow has no gap against its head, and its first dash clears the head** (`4.45`, his
 * note B, with a drawing of the wrong and the right arrow).
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️ `@GraphicsMode(NATIVE)` in a class of its own, because it reads the pixels [Draw] paints.
 * ⚠️ The arrow runs left to right along the middle of a 400 x 200 white image, 4 px thick, with
 * its head at x = 360. Its length is chosen so that the old pattern, counted from the tail,
 * reached the tip inside a gap.
 * ⚠️⚠️ **COUNTER-PROVED** with the shaft drawn as before 4.46 (one dashed line from the tail): the
 * pixels just behind the tip are white.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FrecciaTest {

    @Test
    fun `la freccia tratteggiata e piena contro la punta e poi si interrompe`() {
        val tela = Bitmap.createBitmap(LARGA, ALTA, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val freccia = Mark(
            Pen.ARROW,
            listOf(Offset(CODA / LARGA, 0.5f), Offset(PUNTA / LARGA, 0.5f)),
            Color.BLACK,
            SPESSO / LARGA,
            dashed = true,
            fill = null
        )
        Draw.onto(tela, Drawing(listOf(freccia)), Spin.STILL)
        val y = ALTA / 2
        fun nero(x: Int) = Color.red(tela.getPixel(x, y)) < 128

        // The head's arms reach 20 * cos(pi/7) = 18 px back along the shaft, plus a stroke for
        // their caps: 22 px of solid shaft behind the tip.
        for (x in (PUNTA - 21).toInt()..(PUNTA - 1).toInt()) {
            assertTrue("l'asta doveva essere piena a $x, contro la punta", nero(x))
        }
        // Then a gap: 2,2 strokes long, minus half a stroke of cap at each side.
        assertEquals(
            "dopo il tratto pieno doveva cominciare un vuoto",
            false, nero((PUNTA - 22 - 2.2f * SPESSO / 2).toInt())
        )
    }
}

private const val LARGA = 400
private const val ALTA = 200
private const val SPESSO = 4f
private const val PUNTA = 360f

/** A length of 19 periods of 16,8 px plus 12: the old pattern ended in a gap at the tip. */
private const val CODA = PUNTA - (19 * 16.8f + 12f)

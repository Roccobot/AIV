package io.github.roccobot.aiv

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.util.Base64

/**
 * Le tele del lettore WebP, nella `6.28` (rilievo C14 della revisione della 6.25, voce C8 del
 * rapporto): si contano con [DrawWork], come vuole la sua scelta B1.
 *
 * ⚠️ **La WebP è scritta qui in base64**, sei fotogrammi senza perdita da 16 x 12, rosso, verde e
 * blu a turno: costruita con Pillow, e abbastanza piccola da leggerla.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TeleWebpTest {

    private val webp = Base64.getDecoder().decode(
        "UklGRkQBAABXRUJQVlA4WAoAAAACAAAADwAACwAAQU5JTQYAAAAAAAAAAABBTk1GKAAAAAAAAAAAAA8AAAsAADIA" +
            "AAJWUDhMDwAAAC8PwAIABxD9j/4HIqL/AQBBTk1GKAAAAAAAAAAAAA8AAAsAADIAAABWUDhMDwAAAC8PwAIAB9D/" +
            "iP4HIqL/AQBBTk1GKAAAAAAAAAAAAA8AAAsAADIAAABWUDhMDwAAAC8PwAIABxDR//4HIqL/AQBBTk1GKAAAAAAA" +
            "AAAAAA8AAAsAADIAAABWUDhMDwAAAC8PwAIABxD9j/4HIqL/AQBBTk1GKAAAAAAAAAAAAA8AAAsAADIAAABWUDhM" +
            "DwAAAC8PwAIAB9D/iP4HIqL/AQBBTk1GKAAAAAAAAAAAAA8AAAsAADIAAABWUDhMDwAAAC8PwAIABxDR//4HIqL/AQA="
    )

    private fun atteso(i: Int): Int = when (i % 3) {
        0 -> Color.RED
        1 -> Color.GREEN
        else -> Color.BLUE
    }

    /**
     * **Riprodotta per due giri, la WebP lavora su tele fisse, e quella consegnata non è mai quella
     * in mostra.**
     *
     * ⚠️ **Controprovata** col lettore di prima: una tela sola, riscritta mentre si disegna (due
     * consegne di fila erano lo stesso oggetto), e una tela nuova per ogni toppa decodificata.
     */
    @Test
    fun `tele fisse che si alternano`() {
        val lettore = AnimatedWebp.open(webp)!!
        assertEquals(6, lettore.frameCount)
        var prima = lettore.current()!!
        assertEquals(atteso(0), prima.getPixel(0, 0))
        lettore.advance()
        prima = lettore.current()!!
        lettore.advance()
        DrawWork.canvases = 0
        for (i in 2 until 12) {
            val ora = lettore.current()!!
            assertNotSame("La tela in mostra è stata riscritta", prima, ora)
            assertEquals("Fotogramma $i", atteso(i % 6), ora.getPixel(0, 0))
            prima = ora
            lettore.advance()
        }
        assertEquals("Una tela nuova a ogni fotogramma", 0, DrawWork.canvases)
        lettore.close()
    }
}

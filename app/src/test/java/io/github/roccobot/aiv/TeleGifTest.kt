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
 * Le tele del lettore GIF, nella `6.28` (rilievo C13 della revisione della 6.25, voce C8 del
 * rapporto): si contano con [DrawWork], come vuole la sua scelta B1.
 *
 * ⚠️ **La GIF è scritta qui in base64**, sei fotogrammi da 16 x 12 a strisce rosse, verdi e blu che
 * scorrono di un pixel per fotogramma: costruita con Pillow, e abbastanza piccola da leggerla.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TeleGifTest {

    private val gif = Base64.getDecoder().decode(
        "R0lGODlhEAAMAIEAAP8AAAD/AAAA/wAAACH/C05FVFNDQVBFMi4wAwEAAAAh+QQEBQAAACwAAAAAEAAMAAAILQAB" +
            "BBAgkODAgggRHlxokKDChg8BRGSYkKJFhxcjToR4cKPGjBQ9hgQJUSTEgAAh+QQFBQADACwAAAAAEAAMAIH/AAAA" +
            "/wAAAP8AAAAILQADCAAgkODAgggRHlxokKDChg8DRGSYkKJFhxcjToR4cKPGjBQ9hgQJUSTEgAAh+QQFBQADACwA" +
            "AAAAEAAMAIH/AAAA/wAAAP8AAAAILQAFAAggkODAgggRHlxokKDChg8FRGSYkKJFhxcjToR4cKPGjBQ9hgQJUSTE" +
            "gAAh+QQFBQADACwAAAAAEAAMAIH/AAAA/wAAAP8AAAAILQABBBAgkODAgggRHlxokKDChg8BRGSYkKJFhxcjToR4" +
            "cKPGjBQ9hgQJUSTEgAAh+QQFBQADACwAAAAAEAAMAIH/AAAA/wAAAP8AAAAILQADCAAgkODAgggRHlxokKDChg8D" +
            "RGSYkKJFhxcjToR4cKPGjBQ9hgQJUSTEgAAh+QQFBQADACwAAAAAEAAMAIH/AAAA/wAAAP8AAAAILQAFAAggkODA" +
            "gggRHlxokKDChg8FRGSYkKJFhxcjToR4cKPGjBQ9hgQJUSTEgAA7"
    )

    /** Il colore del primo pixel del fotogramma [i]: le strisce scorrono di uno a ogni fotogramma. */
    private fun atteso(i: Int): Int = when (i % 3) {
        0 -> Color.RED
        1 -> Color.GREEN
        else -> Color.BLUE
    }

    /**
     * **Riprodotta per due giri, la GIF lavora su due tele e non ne crea altre.**
     *
     * ⚠️ **Controprovata** col serbatoio di prima: il decodificatore non restituisce mai il
     * fotogramma consegnato, quindi ogni fotogramma creava una tela (dieci fotogrammi, dieci tele).
     * ⚠️ **E la tela consegnata non è mai quella in mostra**: due di fila sono oggetti diversi, e il
     * decodificatore non scrive dentro quella che il disegno sta leggendo.
     */
    @Test
    fun `due tele che si alternano`() {
        val lettore = AnimatedGif.open(gif)!!
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

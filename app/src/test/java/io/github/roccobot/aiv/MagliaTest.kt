package io.github.roccobot.aiv

import android.graphics.RectF
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * La maglia di Fluidifica nell'editor completo, nella `6.28` (rilievi A25 e A26 della revisione
 * della 6.25, voce C1 del rapporto): si conta con [DrawWork], come vuole la sua scelta B1, e il
 * lavoro rifatto identico si toglie.
 *
 * ⚠️ **Correggi e il Disegno riportano al file un punto per volta**, sedici per ogni poligono di
 * una pennellata, con lo stesso piano, lo stesso riquadro e la stessa Fluidifica: ognuno costruiva
 * da capo i 16.641 vertici della maglia.
 */
@RunWith(AndroidJUnit4::class)
class MagliaTest {

    /**
     * **Sedici punti riportati con la stessa maglia la costruiscono una volta sola.**
     *
     * ⚠️ **Controprovata** con la maglia costruita a ogni chiamata: sedici punti, sedici maglie.
     */
    @Test
    fun `sedici punti con la stessa maglia la costruiscono una volta`() {
        val piano = Warp.plan(Geometry.NONE, cx = 200f, cy = 150f, w = 400f, h = 300f)
        val riquadro = RectF(0f, 0f, 400f, 300f)
        val fluida = Liquify.NONE.stroke(0.5f, 0.5f, 0.1f, -0.1f, 0.2f, 1f, false)
        Warp.back(piano, riquadro, fluida, 10f, 10f)
        DrawWork.meshes = 0
        repeat(16) { Warp.back(piano, riquadro, fluida, 100f + it * 10f, 120f) }
        assertEquals("La maglia si è ricostruita a ogni punto", 0, DrawWork.meshes)
    }

    /** **Una Fluidifica diversa costruisce la sua maglia**, e i punti la seguono. */
    @Test
    fun `una fluidifica nuova costruisce una maglia nuova`() {
        val piano = Warp.plan(Geometry.NONE, cx = 200f, cy = 150f, w = 400f, h = 300f)
        val riquadro = RectF(0f, 0f, 400f, 300f)
        val prima = Liquify.NONE.stroke(0.5f, 0.5f, 0.1f, -0.1f, 0.2f, 1f, false)
        val dopo = prima.stroke(0.5f, 0.5f, 0.1f, -0.1f, 0.2f, 1f, false)
        val a = Warp.back(piano, riquadro, prima, 200f, 150f)
        DrawWork.meshes = 0
        val b = Warp.back(piano, riquadro, dopo, 200f, 150f)
        assertEquals(1, DrawWork.meshes)
        assert(a[0] != b[0] || a[1] != b[1]) { "La seconda pennellata non ha spostato il punto" }
    }
}

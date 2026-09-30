package io.github.roccobot.aiv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Le invarianti della maglia di 'Fluidifica', indipendenti dalla resa del dispositivo. */
class LiquifyTest {

    @Test
    fun `il diametro minimo e un trentesimo del lato lungo`() {
        assertEquals(1f / 30f, 2f * Liquify.MIN_RADIUS, 1e-7f)
    }

    @Test
    fun `la maglia ha centoventotto celle e il bordo resta fermo`() {
        assertEquals(128, Liquify.CELLS)
        val moved = Liquify.NONE.stroke(0.5f, 0.5f, 0.1f, -0.1f, 0.2f, 1f, false)
        assertFalse(moved.idle)
        for (i in 0..Liquify.CELLS) {
            assertEquals(0f, moved.dx(i, 0), 0f)
            assertEquals(0f, moved.dy(i, Liquify.CELLS), 0f)
            assertEquals(0f, moved.dx(0, i), 0f)
            assertEquals(0f, moved.dy(Liquify.CELLS, i), 0f)
        }
    }

    @Test
    fun `deforma solo dentro il pennello e non cambia il passo precedente`() {
        val before = Liquify.NONE
        val after = before.stroke(0.5f, 0.5f, 0.08f, 0.04f, 0.1f, 1f, false)
        val middle = Liquify.CELLS / 2
        assertTrue(after.dx(middle, middle) > 0.07f)
        assertEquals(0f, after.dx(2, 2), 0f)
        assertTrue(before.idle)
        assertNotEquals(before, after)
    }

    @Test
    fun `il pennello resta circolare su un'immagine verticale`() {
        val after = Liquify.NONE.stroke(
            0.5f, 0.5f, 0.08f, 0f, 0.1f, 1f, false, aspect = 0.5f
        )
        assertTrue("il punto a 0,075 lati lunghi dal centro è dentro", after.dx(83, 64) > 0f)
        assertEquals("il punto a 0,15 lati lunghi dal centro è fuori", 0f, after.dx(64, 83), 0f)
    }

    @Test
    fun `il pennello resta circolare su un'immagine orizzontale`() {
        val after = Liquify.NONE.stroke(
            0.5f, 0.5f, 0.08f, 0f, 0.1f, 1f, false, aspect = 2f
        )
        assertTrue("il punto a 0,075 lati lunghi dal centro è dentro", after.dx(64, 83) > 0f)
        assertEquals("il punto a 0,15 lati lunghi dal centro è fuori", 0f, after.dx(83, 64), 0f)
    }

    @Test
    fun `ricostruisci riporta la maglia verso la partenza`() {
        val middle = Liquify.CELLS / 2
        val moved = Liquify.NONE.stroke(0.5f, 0.5f, 0.1f, 0f, 0.1f, 1f, false)
        // Rebuild the visible position, which changes again after the first reconstruction.
        val rebuilt = moved.stroke(0.6f, 0.5f, 0f, 0f, 0.1f, 0.5f, true)
        assertTrue(rebuilt.dx(middle, middle) < moved.dx(middle, middle))
        val cleared = rebuilt.stroke(0.55f, 0.5f, 0f, 0f, 0.1f, 1f, true)
        assertEquals(0f, cleared.dx(middle, middle), 1e-6f)
    }

    @Test
    fun `una seconda pennellata segue il vertice gia spostato`() {
        val first = Liquify.NONE.stroke(0.5f, 0.5f, 0.08f, 0f, 0.1f, 1f, false)
        val second = first.stroke(0.58f, 0.5f, 0.02f, 0f, 0.05f, 1f, false)
        assertTrue("the vertex now under the finger must move again", second.dx(64, 64) > first.dx(64, 64))
        val rebuilt = first.stroke(0.58f, 0.5f, 0f, 0f, 0.05f, 0.5f, true)
        assertTrue("rebuild must reach the displaced vertex too", rebuilt.dx(64, 64) < first.dx(64, 64))
    }

    @Test
    fun `il pennello colpisce solo i vertici visibili dentro il cerchio dopo Geometria`() {
        val geo = Geometry(straighten = 0.7f, aspect = 0.4f, horizontal = 0.3f)
        val plan = Warp.plan(geo, 400f, 250f, 800f, 500f)
        val stroke = Liquify.NONE.stroke(0.45f, 0.6f, 0.02f, 0f, 0.1f, 1f, false, aspect = 1.6f, geometry = geo)
        var inside = 0
        for (j in 1 until Liquify.CELLS) {
            for (i in 1 until Liquify.CELLS) {
                val point = plan.map(800f * i / Liquify.CELLS, 500f * j / Liquify.CELLS)
                val distance = kotlin.math.hypot(point[0] - 360f, point[1] - 300f)
                if (distance >= 80f) {
                    assertEquals("a vertex outside the displayed brush must stay still", 0f, stroke.dx(i, j), 0f)
                } else {
                    inside++
                    assertTrue("a visible vertex inside must move", stroke.dx(i, j) > 0f)
                }
            }
        }
        assertTrue("the brush must contain vertices", inside > 0)
    }

    @Test
    fun `due maglie con gli stessi tratti sono lo stesso valore`() {
        val first = Liquify.NONE.stroke(0.4f, 0.6f, 0.02f, -0.03f, 0.08f, 0.7f, false)
        val second = Liquify.NONE.stroke(0.4f, 0.6f, 0.02f, -0.03f, 0.08f, 0.7f, false)
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertSame(Liquify.NONE, Geometry.NONE.liquify)
    }

}

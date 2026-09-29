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
    fun `ricostruisci riporta la maglia verso la partenza`() {
        val middle = Liquify.CELLS / 2
        val moved = Liquify.NONE.stroke(0.5f, 0.5f, 0.1f, 0f, 0.1f, 1f, false)
        val rebuilt = moved.stroke(0.5f, 0.5f, 0f, 0f, 0.1f, 0.5f, true)
        assertTrue(rebuilt.dx(middle, middle) in 0f..moved.dx(middle, middle))
        val cleared = rebuilt.stroke(0.5f, 0.5f, 0f, 0f, 0.1f, 1f, true)
        assertEquals(0f, cleared.dx(middle, middle), 1e-6f)
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

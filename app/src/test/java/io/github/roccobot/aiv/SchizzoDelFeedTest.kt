package io.github.roccobot.aiv

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The geometry of the Feed's veil (6.20, `feedSketch`): where the two full cells fall, on a phone, on
 * a tablet and on a phone on its side.
 *
 * ⚠️ **A pure function, so a plain test**: what it measures is his drawing, *il rettangolo in alto a
 * sinistra e quello più in basso a destra tra quelli visibili*. How the veil is painted is looked at on
 * the phone and in the catalogue of the veils.
 */
class SchizzoDelFeedTest {

    private fun uguali(atteso: Dp, misurato: Dp, cosa: String) =
        assertEquals(cosa, atteso.value, misurato.value, 0.01f)

    /**
     * **His two cells**: the newest is the first at the top left, the oldest is the last on the right
     * of the lowest row that shows whole, and the row under it is cut by the edge.
     *
     * ⚠️ Counter-tested with `cells.last()` as the oldest: it is the cut row, past the bottom.
     */
    private fun leDueCelle(larghezza: Dp, altezza: Dp) {
        val schizzo = feedSketch(larghezza, altezza)
        val celle = schizzo.cells
        uguali(celle.minOf { it.x }, schizzo.newest.x, "la più recente a sinistra")
        uguali(celle.minOf { it.y }, schizzo.newest.y, "la più recente in alto")
        uguali(celle.maxOf { it.x }, schizzo.oldest.x, "la meno recente a destra")
        assertTrue("la meno recente si vede intera", schizzo.oldest.y + schizzo.oldest.size <= altezza)
        val sotto = celle.filter { it.y > schizzo.oldest.y }
        assertTrue("sotto ci sono solo righe tagliate", sotto.all { it.y + it.size > altezza })
        assertTrue("la riga tagliata c'è", sotto.isNotEmpty())
    }

    @Test
    fun `sul telefono le due celle sono in alto a sinistra e in basso a destra`() =
        leDueCelle(393.dp, 800.dp)

    @Test
    fun `sul tablet le due celle sono in alto a sinistra e in basso a destra`() =
        leDueCelle(1200.dp, 800.dp)

    /**
     * **Three columns at any width, centred, and no giant squares on a tablet**.
     *
     * ⚠️ Counter-tested without the cap at 160: the cell of the tablet is over 370.
     */
    @Test
    fun `tre colonne centrate e celle col tetto sul tablet`() {
        val schizzo = feedSketch(1200.dp, 800.dp)
        assertEquals(3, schizzo.cells.map { it.x }.distinct().size)
        uguali(160.dp, schizzo.newest.size, "la cella al tetto")
        val sinistra = schizzo.cells.minOf { it.x }
        val destra = 1200.dp - schizzo.cells.maxOf { it.x + it.size }
        uguali(sinistra, destra, "i due margini")
    }

    /** **A phone on its side with one whole row**: the two cells are on the same row, at its ends. */
    @Test
    fun `con una riga sola le due celle sono ai suoi estremi`() {
        val schizzo = feedSketch(873.dp, 250.dp)
        uguali(schizzo.newest.y, schizzo.oldest.y, "la stessa riga")
        assertTrue("due celle diverse", schizzo.newest.x < schizzo.oldest.x)
    }
}

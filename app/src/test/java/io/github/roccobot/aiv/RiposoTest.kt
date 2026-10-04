package io.github.roccobot.aiv

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where a picture rests when the info bar is on screen (3.60, mockups `Viewer_NOPE` and
 * `Viewer_H`), and when the bar goes on one row.
 *
 * ⚠️ It tests the pure functions of `Fit.kt`, which the canvas, the preview and the neighbour
 * all call: a wrong number here moves every picture in the viewer, and on screen it reads as a
 * picture slightly cut, which nobody reports as a defect.
 */
class RiposoTest {

    private val eps = 0.01f

    @Test
    fun `senza barra la regola e quella di sempre`() {
        val place = restPlace(1080f, 2400f, 4000f, 3000f, cap = null, bar = null)
        assertEquals(1080f / 4000f, place.scale, 1e-6f)
        assertEquals(0f, place.top, eps)
        assertEquals(2400f, place.bottom, eps)
        // Smaller than the view: centred.
        assertEquals(0f, clampAxis(300f, 810f, 2400f, 0f, 2400f), eps)
        // Larger than the view: it may move by the slack.
        assertEquals(100f, clampAxis(100f, 3000f, 2400f, 0f, 2400f), eps)
        assertEquals(300f, clampAxis(900f, 3000f, 2400f, 0f, 2400f), eps)
    }

    @Test
    fun `in verticale l immagine non va sotto la barra`() {
        // A tall picture on a portrait phone, bar of 150 px at the top.
        val bar = BarSpace(BarEdge.TOP, 150f)
        val place = restPlace(1080f, 2400f, 1000f, 3000f, cap = null, bar = bar)
        assertEquals(2250f / 3000f, place.scale, 1e-6f)
        assertEquals(150f, place.top, eps)
        assertEquals(2400f, place.bottom, eps)
        // At rest the centre is the centre of the free band, 75 px below the view's centre.
        assertEquals(75f, clampAxis(0f, 2250f, 2400f, place.top, place.bottom), eps)
    }

    @Test
    fun `la barra in basso lascia la fascia di sopra`() {
        val bar = BarSpace(BarEdge.BOTTOM, 150f)
        val place = restPlace(1080f, 2400f, 1000f, 3000f, cap = null, bar = bar)
        assertEquals(0f, place.top, eps)
        assertEquals(2250f, place.bottom, eps)
        assertEquals(-75f, clampAxis(0f, 2250f, 2400f, place.top, place.bottom), eps)
    }

    /**
     * **La tolleranza non entra nella barra di sistema** (voce `3.70-01`: *su tablet in
     * orizzontale le immagini alte vanno ancora a finire sotto l'overlay info*).
     */
    @Test
    fun `la tolleranza si ferma alla barra di stato`() {
        // Landscape tablet 2880 x 1800: status bar of 60 px, then the bar's row of 44 px.
        val bar = BarSpace(BarEdge.TOP, 104f, listOf(48f..900f, 1900f..2832f), system = 60f)
        val place = restPlace(2880f, 1800f, 1000f, 3000f, cap = null, bar = bar)
        // 5% of 1800 = 90 px would reach 14 px from the top, inside the status bar: it stops at 60.
        assertEquals(60f, place.top, eps)
        assertEquals((1800f - 60f) / 3000f, place.scale, 1e-6f)
    }

    @Test
    fun `in orizzontale il 5 per cento va sotto la barra dove non c e testo`() {
        // Landscape phone 2400 x 1080, bar of 100 px with text only at the two ends.
        val bar = BarSpace(BarEdge.TOP, 100f, listOf(48f..600f, 1900f..2352f))
        val place = restPlace(2400f, 1080f, 3000f, 4000f, cap = null, bar = bar)
        // 5% of 1080 = 54 px under the bar: the picture rests on 980 + 54 = 1034 px.
        assertEquals(1034f / 4000f, place.scale, 1e-6f)
        assertEquals(46f, place.top, eps)
        assertEquals(1080f, place.bottom, eps)
    }

    @Test
    fun `mai piu della barra e mai sul testo`() {
        // A bar thinner than 5%: the picture may use all of it and no more.
        val thin = BarSpace(BarEdge.TOP, 30f, listOf(0f..100f))
        val thinPlace = restPlace(2400f, 1080f, 3000f, 4000f, cap = null, bar = thin)
        assertEquals(1080f / 4000f, thinPlace.scale, 1e-6f)
        assertEquals(0f, thinPlace.top, eps)

        // Text over the middle of the bar: nothing goes under it.
        val middle = BarSpace(BarEdge.TOP, 100f, listOf(1100f..1300f))
        val blocked = restPlace(2400f, 1080f, 3000f, 4000f, cap = null, bar = middle)
        assertEquals(980f / 4000f, blocked.scale, 1e-6f)
        assertEquals(100f, blocked.top, eps)

        // Text close to the centre: the picture grows only until its edges reach the text.
        val near = BarSpace(BarEdge.TOP, 100f, listOf(1580f..2000f))
        val limited = restPlace(2400f, 1080f, 3000f, 4000f, cap = null, bar = near)
        // Clear width 2 x (1580 - 1200) = 760 px, so the scale is 760 / 3000.
        assertEquals(760f / 3000f, limited.scale, 1e-6f)
        assertEquals(100f - (4000f * 760f / 3000f - 980f), limited.top, eps)
    }

    @Test
    fun `la tolleranza vale solo in orizzontale`() {
        val bar = BarSpace(BarEdge.TOP, 100f)
        val place = restPlace(1080f, 2400f, 1000f, 3000f, cap = null, bar = bar)
        assertEquals(2300f / 3000f, place.scale, 1e-6f)
        assertEquals(100f, place.top, eps)
    }

    @Test
    fun `un immagine limitata dalla larghezza o piccola resta nella fascia libera`() {
        val bar = BarSpace(BarEdge.TOP, 100f)
        // Panorama on a landscape phone: limited by the width, no overlap spent.
        val wide = restPlace(2400f, 1080f, 6000f, 1000f, cap = null, bar = bar)
        assertEquals(0.4f, wide.scale, 1e-6f)
        assertEquals(100f, wide.top, eps)
        // A small icon with 'Enlarge small images' off: shown at one-to-one, centred in the band.
        val small = restPlace(2400f, 1080f, 64f, 64f, cap = 2.75f, bar = bar)
        assertEquals(2.75f, small.scale, 1e-6f)
        assertEquals(50f, clampAxis(0f, 176f, 1080f, small.top, small.bottom), eps)
    }

    @Test
    fun `ingrandita l immagine scorre fino a mostrare quello che e sotto la barra`() {
        val top = 150f
        val bottom = 2400f
        // Between the band and the view: it moves until its top edge clears the bar.
        assertEquals(100f, clampAxis(500f, 2300f, 2400f, top, bottom), eps)
        assertEquals(50f, clampAxis(-500f, 2300f, 2400f, top, bottom), eps)
        // Larger than the view: down until the top edge is just under the bar, up as usual.
        assertEquals(450f, clampAxis(900f, 3000f, 2400f, top, bottom), eps)
        assertEquals(-300f, clampAxis(-900f, 3000f, 2400f, top, bottom), eps)
    }

    @Test
    fun `la larghezza libera dal testo`() {
        assertEquals(Float.POSITIVE_INFINITY, clearWidth(1200f, emptyList()))
        assertEquals(0f, clearWidth(1200f, listOf(1000f..1300f)), eps)
        assertEquals(800f, clearWidth(1200f, listOf(0f..300f, 1600f..2000f)), eps)
    }

    @Test
    fun `la riga unica dipende dalla larghezza e non dal nome`() {
        // Portrait phone: 412 dp minus margins leave too little room for the name.
        assertFalse(oneRowFits(380.dp, 230.dp, 40.dp, 16.dp))
        // Landscape phone and tablet: one row.
        assertTrue(oneRowFits(820.dp, 230.dp, 40.dp, 16.dp))
        // The threshold is exact: 160 dp for the name, after the gaps (16 + 12 + 6).
        assertTrue(oneRowFits((230 + 40 + 16 + 34 + 160).dp, 230.dp, 40.dp, 16.dp))
        assertFalse(oneRowFits((230 + 40 + 16 + 34 + 159).dp, 230.dp, 40.dp, 16.dp))
    }
}

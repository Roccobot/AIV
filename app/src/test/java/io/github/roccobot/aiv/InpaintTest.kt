package io.github.roccobot.aiv

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class InpaintTest {
    private val white = 0xffeeeeee.toInt()
    private val black = 0xff181818.toInt()
    private val defect = 0xffff00ff.toInt()

    @Test fun `uniform background is reconstructed without touching the surroundings`() {
        val w = 64
        val source = IntArray(w * w) { white }
        val mask = BooleanArray(source.size)
        for (y in 27..35) {
            for (x in 27..35) {
                mask[y * w + x] = true
                source[y * w + x] = defect
            }
        }
        val before = source.copyOf()
        val result = Inpaint.repair(source, w, w, mask)!!
        assertArrayEquals(before, source)
        for (i in result.indices) if (mask[i]) assertEquals(white, result[i]) else assertEquals(before[i], result[i])
    }

    @Test fun `a horizontal line continues through an opaque defect`() {
        val w = 80
        val source = IntArray(w * w) { i -> if (i / w in 39..41) black else white }
        val mask = BooleanArray(source.size)
        for (y in 35..45) {
            for (x in 35..43) {
                mask[y * w + x] = true
                source[y * w + x] = defect
            }
        }
        val result = Inpaint.repair(source, w, w, mask)!!
        for (x in 35..43) assertTrue("Broken line at $x: ${result[40 * w + x] and 255}", (result[40 * w + x] and 255) < 80)
        for (x in 35..43) assertTrue("Line spread outside its row", (result[35 * w + x] and 255) > 200)
        for (i in result.indices) if (!mask[i]) assertEquals(source[i], result[i])
    }

    @Test fun `a diagonal line retains its direction through a small defect`() {
        val w = 80
        val source = IntArray(w * w) { i -> if (abs(i % w - i / w) <= 1) black else white }
        val mask = BooleanArray(source.size)
        for (y in 35..43) {
            for (x in 35..43) {
                mask[y * w + x] = true
                source[y * w + x] = defect
            }
        }
        val result = Inpaint.repair(source, w, w, mask)!!
        for (i in 35..43) assertTrue("Diagonal broken at $i", (result[i * w + i] and 255) < 100)
        assertTrue((result[35 * w + 43] and 255) > 200)
    }

    @Test fun `repeated texture is reconstructed rather than replaced with a flat average`() {
        val w = 96
        val expected = IntArray(w * w) { i -> if ((i % w / 4 + i / w / 4) % 2 == 0) black else white }
        val source = expected.copyOf()
        val mask = BooleanArray(source.size)
        for (y in 42..51) {
            for (x in 42..51) {
                mask[y * w + x] = true
                source[y * w + x] = defect
            }
        }
        val result = Inpaint.repair(source, w, w, mask)!!
        val error =
            mask.indices
                .filter { mask[it] }
                .sumOf { abs((result[it] and 255) - (expected[it] and 255)) }
                .toDouble() / 100
        assertTrue("Texture mismatch: $error", error < 35)
    }

    @Test fun `an area without surrounding information is rejected`() {
        val pixels = IntArray(64) { white }
        assertNull(Inpaint.repair(pixels, 8, 8, BooleanArray(64) { true }))
    }

    @Test fun `cancellation leaves the input unchanged`() {
        val pixels = IntArray(64 * 64) { white }
        val mask = BooleanArray(pixels.size)
        for (y in 20..40) {
            for (x in 20..40) {
                mask[y * 64 + x] = true
                pixels[y * 64 + x] = defect
            }
        }
        val before = pixels.copyOf()
        var called = false
        try {
            Inpaint.repair(pixels, 64, 64, mask) {
                called = true
                throw InterruptedException()
            }
            fail("Cancellation ignored")
        } catch (
            _: InterruptedException,
        ) {
        }
        assertTrue(called)
        assertArrayEquals(before, pixels)
    }

    @Test fun `alpha and unselected pixels remain exact`() {
        val pixels = IntArray(64 * 64) { 0x80eeeeee.toInt() }
        val mask = BooleanArray(pixels.size)
        for (y in 28..34) {
            for (x in 28..34) {
                mask[y * 64 + x] = true
                pixels[y * 64 + x] = 0x80ff00ff.toInt()
            }
        }
        val result = Inpaint.repair(pixels, 64, 64, mask)!!
        for (i in result.indices) {
            assertEquals(pixels[i] ushr 24, result[i] ushr 24)
            if (!mask[i]) assertEquals(pixels[i], result[i])
        }
        assertTrue((result[31 * 64 + 31] and 255) > 200)
        assertTrue((result[31 * 64 + 31] ushr 8 and 255) > 200)
    }

    @Test fun `a larger local repair remains bounded and preserves its surroundings`() {
        val width = 320
        val source = IntArray(width * width) { white }
        val mask = BooleanArray(source.size)
        for (y in 115 until 205) {
            for (x in 115 until 205) {
                mask[y * width + x] = true
                source[y * width + x] = defect
            }
        }
        val started = System.nanoTime()
        val result = Inpaint.repair(source, width, width, mask)!!
        val elapsed = (System.nanoTime() - started) / 1_000_000
        println("Local repair: 8100 selected pixels in a 320x320 crop, $elapsed ms on the build host")
        for (i in result.indices) assertEquals(if (mask[i]) white else source[i], result[i])
    }


    @Test fun `a hem-like vertical edge survives removing a spot on it`() {
        val w = 96
        // Soft field + a dark vertical hem at x=48..49; defect is a "button" on the hem.
        val source = IntArray(w * w) { i ->
            val x = i % w
            val y = i / w
            when {
                x in 48..49 -> black
                else -> 0xffd0d0d0.toInt()
            }
        }
        val mask = BooleanArray(source.size)
        for (y in 44..52) {
            for (x in 46..51) {
                mask[y * w + x] = true
                source[y * w + x] = defect
            }
        }
        val result = Inpaint.repair(source, w, w, mask)!!
        // Media sull'orlo vs campo: l'orlo deve restare piu scuro del tessuto intorno.
        val hem = (44..52).map { result[it * w + 48] and 255 }.average()
        val field = (44..52).map { result[it * w + 42] and 255 }.average()
        assertTrue("Hem not darker than field: hem=$hem field=$field", hem < field - 40.0)
        assertTrue("Hem washed out: $hem", hem < 130.0)
        assertTrue("Field darkened: $field", field > 140.0)
        for (i in result.indices) if (!mask[i]) assertEquals(source[i], result[i])
    }

    @Test fun `a defect at the image corner is corrected without changing the edge alpha`() {
        val width = 48
        val source = IntArray(width * width) { white }
        val mask = BooleanArray(source.size)
        for (y in 0..6) {
            for (x in 0..6) {
                mask[y * width + x] = true
                source[y * width + x] = defect
            }
        }
        val result = Inpaint.repair(source, width, width, mask)!!
        assertEquals(white, result[0])
        for (i in result.indices) if (!mask[i]) assertEquals(source[i], result[i])
    }
    // Since `3.52`: a hole several bricks wide is rebuilt with joints that line up. Measured on
    // this wall: the pyramid puts mortar and brick in the right place on every hole pixel, the
    // greedy fill alone on 93% of them (scattered mortar fragments, visible at a glance).
    @Test fun `brick joints continue through a hole several bricks wide`() {
        val w = 320

        fun grain(x: Int, y: Int): Int {
            var h = x * 374761393 + y * 668265263
            h = (h xor (h ushr 13)) * 1274126177
            return ((h xor (h ushr 16)) and 31) - 16
        }
        val expected =
            IntArray(w * w) { i ->
                val x = i % w
                val y = i / w
                val row = y / 18
                val mortar = y % 18 < 3 || (x + if (row % 2 == 0) 0 else 20) % 40 < 3
                val n = grain(x / 2, y / 2)
                if (mortar) {
                    (0xff shl 24) or ((190 + n / 3) shl 16) or ((185 + n / 3) shl 8) or (175 + n / 3)
                } else {
                    (0xff shl 24) or ((140 + n) shl 16) or ((62 + n / 2) shl 8) or (45 + n / 3)
                }
            }
        val source = expected.copyOf()
        val mask = BooleanArray(source.size) { (it % w - 160) * (it % w - 160) + (it / w - 160) * (it / w - 160) <= 45 * 45 }
        for (i in source.indices) if (mask[i]) source[i] = defect
        val result = Inpaint.repair(source, w, w, mask)!!
        // Green tells mortar (above 170) from brick (below 80) whatever the grain.
        val wrong = mask.indices.count { mask[it] && ((result[it] ushr 8 and 255) > 120) != ((expected[it] ushr 8 and 255) > 120) }
        assertTrue("Joints misplaced on $wrong of ${mask.count { it }} pixels", wrong * 100 < mask.count { it })
        for (i in result.indices) if (!mask[i]) assertEquals(source[i], result[i])
    }

    // Since `3.52`: a sharp horizon across the hole stays straight and clean. The old Poisson
    // blend, guided by donors from different places, left dark dots and light dashes on it
    // (measured: off by up to 348 on this scene, against 0 now).
    @Test fun `a sharp horizon stays free of dark dots and light dashes`() {
        val w = 200
        val sky = 0xff9cc4ea.toInt()
        val ground = 0xff5a5040.toInt()
        val source = IntArray(w * w) { if (it / w < 100) sky else ground }
        val mask = BooleanArray(source.size) { (it % w - 100) * (it % w - 100) + (it / w - 100) * (it / w - 100) <= 20 * 20 }
        for (i in source.indices) if (mask[i]) source[i] = defect
        val result = Inpaint.repair(source, w, w, mask)!!
        for (i in result.indices) {
            if (!mask[i]) continue
            val want = if (i / w < 100) sky else ground
            val off = abs((result[i] ushr 16 and 255) - (want ushr 16 and 255)) +
                abs((result[i] ushr 8 and 255) - (want ushr 8 and 255)) + abs((result[i] and 255) - (want and 255))
            assertTrue("Spot at ${i % w},${i / w}: off by $off", off < 12)
        }
    }

    // Since `3.52`: a hole far wider than the old limit is filled with real texture in bounded
    // time; the finest levels only copy along the upsampled field when the search would cost too much.
    @Test fun `a hole wider than the old limit is filled and keeps its surroundings`() {
        val w = 640
        val source = IntArray(w * w) { i -> if ((i % w / 6 + i / w / 6) % 2 == 0) black else white }
        val expected = source.copyOf()
        val mask = BooleanArray(source.size) { (it % w - 320) * (it % w - 320) + (it / w - 320) * (it / w - 320) <= 250 * 250 }
        for (i in source.indices) if (mask[i]) source[i] = defect
        val started = System.nanoTime()
        val result = Inpaint.repair(source, w, w, mask)!!
        println("Large repair: ${mask.count { it }} selected pixels, ${(System.nanoTime() - started) / 1_000_000} ms on the build host")
        for (i in result.indices) if (!mask[i]) assertEquals(source[i], result[i])
        val magenta = mask.indices.count { mask[it] && result[it] == defect }
        assertEquals(0, magenta)
        val error =
            mask.indices
                .filter { mask[it] }
                .sumOf { abs((result[it] and 255) - (expected[it] and 255)) }
                .toDouble() / mask.count { it }
        assertTrue("Checker mismatch: $error", error < 40)
    }
    // Since `3.52`: a hole half as wide as the crop left no donor on the smallest level, and the
    // whole repair gave up (found by HealingTest on the bench). The start moves one scale up.
    @Test fun `a hole half as wide as the crop is still filled`() {
        val w = 400
        val source = IntArray(w * w) { white }
        val mask = BooleanArray(source.size) { it % w in 100 until 300 && it / w in 100 until 300 }
        for (i in source.indices) if (mask[i]) source[i] = defect
        val result = Inpaint.repair(source, w, w, mask)
        assertNotNull(result)
        for (i in result!!.indices) assertEquals(if (mask[i]) white else source[i], result[i])
    }
}

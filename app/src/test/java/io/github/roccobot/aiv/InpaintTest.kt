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
}

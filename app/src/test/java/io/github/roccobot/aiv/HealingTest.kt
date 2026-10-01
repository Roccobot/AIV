package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HealingTest {
    private val app: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun `draft painting does not change pixels and Apply creates a reusable full size patch`() =
        runBlocking {
            val bitmap = Bitmap.createBitmap(128, 96, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.LTGRAY)
            for (y in 43..51) for (x in 59..67) bitmap.setPixel(x, y, Color.MAGENTA)
            val file = File(app.filesDir, "healing-source.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val selection =
                Healing.Selection().add(
                    listOf(
                        Offset(58f / 128, 42f / 96),
                        Offset(69f / 128, 42f / 96),
                        Offset(69f / 128, 53f / 96),
                        Offset(58f / 128, 53f / 96),
                    ),
                )
            val draft = Healing.overlay(selection, 128, 96, Color.GREEN)!!
            assertEquals(Color.MAGENTA, bitmap.getPixel(63, 47))
            assertEquals(Color.GREEN, draft.getPixel(63, 47))
            val patch = Healing.prepare(app, file.toUri(), null, Healing.Plan.NONE, selection)!!
            val plan = Healing.Plan(listOf(patch))
            val result = Healing.render(bitmap, plan)
            assertEquals(Color.LTGRAY, result.getPixel(63, 47))
            for (y in 0 until 96) {
                for (x in 0 until 128) {
                    if (x !in 58..68 || y !in 42..52) {
                        assertEquals("Outside selection at $x,$y", bitmap.getPixel(x, y), result.getPixel(x, y))
                    }
                }
            }
            val preview = Healing.render(Bitmap.createScaledBitmap(bitmap, 64, 48, true), plan)
            assertEquals(Color.LTGRAY, preview.getPixel(31, 23))
            assertSame(bitmap, Healing.render(bitmap, Healing.Plan.NONE))
            val look = Look(healing = plan)
            assertFalse(look.idle)
            assertFalse(look.lossless)
            assertTrue(look.plain)
            assertTrue(look.place.healing.idle)
            val saved = ImageEdit.saveLook(app, file.toUri(), look, Quality.LOSSLESS, false, beside = true)
            assertTrue("Save failed: $saved", saved is ImageEdit.Result.Done)
            val uri = (saved as ImageEdit.Result.Done).file.toUri()
            val read = ImageSource.pixels(app, uri, 0)!!
            assertEquals(128, read.width)
            assertEquals(96, read.height)
            assertEquals(Color.LTGRAY, read.getPixel(63, 47))
            assertEquals(Color.MAGENTA, ImageSource.pixels(app, file.toUri(), 0)!!.getPixel(63, 47))
        }

    @Test fun `patch replacement preserves semi transparency instead of compositing twice`() {
        val pixels = intArrayOf(0x80404040.toInt(), 0x80808080.toInt(), 0x80b0b0b0.toInt(), 0x80e0e0e0.toInt())
        val expected = Bitmap.createBitmap(pixels, 2, 2, Bitmap.Config.ARGB_8888)
        val source = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply { eraseColor(0x80303030.toInt()) }
        val patch = Healing.Patch(0, 0, 2, 2, 2, 2, pixels)
        pixels.fill(Color.RED)
        val result = Healing.render(source, Healing.Plan(listOf(patch)))
        for (y in 0..1) for (x in 0..1) assertEquals(expected.getPixel(x, y), result.getPixel(x, y))
    }

    @Test fun `overlapping brush strokes make one solid mask`() {
        val square = listOf(Offset(.2f, .2f), Offset(.8f, .2f), Offset(.8f, .8f), Offset(.2f, .8f))
        val selection = Healing.Selection().add(square).add(square)
        val overlay = Healing.overlay(selection, 100, 100, Color.GREEN)!!
        assertEquals(255, Color.alpha(overlay.getPixel(50, 50)))
        assertEquals(0, Color.alpha(overlay.getPixel(10, 10)))
    }

    @Test fun `source coordinates survive every rotation and mirror`() {
        val original = Offset(.2f, .7f)
        for (turn in 0..3) {
            for (mirror in listOf(false, true)) {
                val x = if (mirror) 1f - original.x else original.x
                val y = original.y
                val posed =
                    when (turn) {
                        1 -> Offset(1f - y, x)
                        2 -> Offset(1f - x, 1f - y)
                        3 -> Offset(y, 1f - x)
                        else -> Offset(x, y)
                    }
                val back = Healing.unpose(posed, Spin(turn, mirror))
                assertEquals(original.x, back.x, .00001f)
                assertEquals(original.y, back.y, .00001f)
            }
        }
    }

    @Test fun `too large a selection keeps the source untouched`() =
        runBlocking {
            val source = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
            val file = File(app.filesDir, "healing-too-large.png")
            file.outputStream().use { source.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val selection = Healing.Selection().add(listOf(Offset(.1f, .1f), Offset(.9f, .1f), Offset(.9f, .9f), Offset(.1f, .9f)))
            assertNull(Healing.prepare(app, file.toUri(), null, Healing.Plan.NONE, selection))
            assertEquals(Color.RED, ImageSource.pixels(app, file.toUri(), 0)!!.getPixel(150, 150))
        }

    @Test fun `styles exclude image patches and preserve patches already on the target`() {
        val patch = Healing.Patch(0, 0, 2, 2, 4, 4, IntArray(4) { Color.RED })
        val plan = Healing.Plan(listOf(patch))
        val edited = Look(healing = plan, light = Light(exposure = .2f))
        val style = Preset.of("Color", edited)
        assertTrue(style.look.healing.idle)
        assertSame(plan, style.applyTo(edited).healing)
        assertSame(plan, style.addTo(edited).healing)
    }
    @Test fun `a zoom tile reuses the same full resolution correction`() = runBlocking {
        val bitmap = Bitmap.createBitmap(400,300,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.LTGRAY) }
        for (y in 181..187) for (x in 277..283) bitmap.setPixel(x,y,Color.MAGENTA)
        val file = File(app.filesDir,"healing-region.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        val selection = Healing.Selection().add(listOf(Offset(276f/400,180f/300),Offset(285f/400,180f/300),
            Offset(285f/400,189f/300),Offset(276f/400,189f/300)))
        // Robolectric's region decoder cannot open this PNG. Test the actual correction
        // and tile projection with real bitmap pixels; native region decoding needs a device.
        val patch = Healing.prepare(app,file.toUri(),null,Healing.Plan.NONE,selection)!!
        val plan = Healing.Plan(listOf(patch))
        val tile = Bitmap.createBitmap(bitmap,240,150,70,70)
        val corrected = Healing.render(tile,plan,android.graphics.RectF(.6f,.5f,.775f,220f/300))
        assertEquals(Color.LTGRAY,corrected.getPixel(40,34))
        assertEquals(Color.MAGENTA,tile.getPixel(40,34))
        assertEquals(Color.LTGRAY,Healing.render(bitmap,plan).getPixel(280,184))
    }

}

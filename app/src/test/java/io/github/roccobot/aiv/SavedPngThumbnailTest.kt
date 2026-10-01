package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SavedPngThumbnailTest {
    private val app: Context get() = ApplicationProvider.getApplicationContext()

    // A filesystem can retain the same timestamp across a fast rewrite or a restore.
    @Test
    fun `rewriting a PNG invalidates disk pixels even with the same timestamp`() = runBlocking {
        val file = File(app.filesDir, "saved-png-same-time.png")
        writePng(file, Color.RED)
        val uri = file.toUri()
        val stamp = file.lastModified()
        val old = thumbnail(uri)
        assertTrue(Color.red(old.getPixel(4, 4)) > 200)
        assertNotNull(AvifCache.read(app, uri, Thumbs.PX))

        writePng(file, Color.BLUE)
        assertTrue(file.setLastModified(stamp))
        Thumbs.forget(app, uri)
        val fresh = thumbnail(uri)
        val pixel = fresh.getPixel(4, 4)
        assertTrue("Expected newly saved blue pixels, got ${Integer.toHexString(pixel)}",
            Color.blue(pixel) > 200 && Color.red(pixel) < 50)
        val kept = AvifCache.read(app, uri, Thumbs.PX)!!
        assertTrue(Color.blue(kept.getPixel(4, 4)) > 200)
    }

    @Test
    fun `invalidating one PNG preserves another cached thumbnail`() {
        val first = File(app.filesDir, "saved-png-first.png")
        val second = File(app.filesDir, "saved-png-second.png")
        writePng(first, Color.RED)
        writePng(second, Color.BLUE)
        val blue = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        AvifCache.write(app, first.toUri(), Thumbs.PX, blue)
        AvifCache.write(app, second.toUri(), Thumbs.PX, blue)
        Thumbs.forget(app, first.toUri())
        assertNull(AvifCache.read(app, first.toUri(), Thumbs.PX))
        assertNotNull(AvifCache.read(app, second.toUri(), Thumbs.PX))
    }

    private suspend fun thumbnail(uri: android.net.Uri): Bitmap {
        val loader = Thumbs.warmer(app)
        return try {
            val result = loader.execute(Thumbs.request(app, uri).newBuilder().allowHardware(false).build())
            assertTrue("PNG thumbnail failed: $result", result is SuccessResult)
            (result as SuccessResult).image.toBitmap()
        } finally { loader.shutdown() }
    }

    private fun writePng(file: File, color: Int) {
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }
}

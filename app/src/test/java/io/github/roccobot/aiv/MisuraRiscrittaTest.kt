package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * La misura e il peso di un file riscritto nella `6.26` (rilievi B14 e C20 della revisione della
 * 6.25): dopo `Thumbs.forget`, il punto in cui l'app dichiara un file riscritto, si rileggono.
 *
 * ⚠️ **Il file si riscrive allo stesso indirizzo**, come fa un salvataggio sopra l'originale.
 * ⚠️ `@GraphicsMode(NATIVE)`: con la grafica di fabbrica di Robolectric il JPEG non si scrive.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
class MisuraRiscrittaTest {

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private fun scrivi(file: File, w: Int, h: Int) {
        val mappa = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        file.outputStream().use { mappa.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /**
     * **Dopo la riscrittura il lato lungo e il peso sono quelli nuovi.**
     *
     * ⚠️ **Controprovata** togliendo da `Thumbs.forget` le due righe nuove: tornano 1600 e il peso
     * di prima.
     */
    @Test
    fun `un file riscritto si rimisura`() {
        val cartella = File(app.cacheDir, "misura-riscritta").also { it.mkdirs() }
        val file = File(cartella, "foto.png")
        val uri = Uri.fromFile(file)
        scrivi(file, 1600, 900)
        assertEquals(1600, runBlocking { Pixels.longSideOf(app, uri) })
        assertEquals(file.length(), sizeOf(app, uri))

        scrivi(file, 800, 400)
        Thumbs.forget(app, uri)

        assertEquals(800, runBlocking { Pixels.longSideOf(app, uri) })
        assertEquals(file.length(), sizeOf(app, uri))
    }
}

package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * La misura dell'anteprima dell'editor, nella `6.28` (rilievo A4 della revisione della 6.25, voce
 * C5 del rapporto).
 *
 * ⚠️ **La scala è un decimo di quella vera**, perché un file da tremila pixel peserebbe sul banco
 * senza dire niente di più: conta il rapporto fra il lato e il riquadro, che è lo stesso.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AnteprimaTest {
    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * **Un lato lungo meno del doppio del riquadro arriva alla misura del riquadro.**
     *
     * ⚠️ **Controprovata** col campionamento a potenze di due di prima: 300 pixel restavano 300,
     * perché dimezzarli scendeva sotto i 160.
     */
    @Test
    fun `l'anteprima ha il lato lungo del riquadro`() {
        val pixels = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888)
        pixels.eraseColor(Color.GRAY)
        val file = File(app.filesDir, "anteprima-300.png")
        file.outputStream().use { pixels.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val letta = ImageSource.pixels(app, file.toUri(), 160)!!
        assertEquals(160, letta.width)
        assertEquals(107, letta.height)
    }

    /** **Un file già più piccolo del riquadro resta com'è**: l'anteprima non ingrandisce. */
    @Test
    fun `un file piccolo non si ingrandisce`() {
        val pixels = Bitmap.createBitmap(120, 90, Bitmap.Config.ARGB_8888)
        val file = File(app.filesDir, "anteprima-120.png")
        file.outputStream().use { pixels.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val letta = ImageSource.pixels(app, file.toUri(), 160)!!
        assertEquals(120, letta.width)
        assertEquals(90, letta.height)
    }
}

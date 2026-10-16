package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Il ridimensionamento che non rimpicciolisce niente nella `6.26` (rilievo B16 della revisione
 * della 6.25): con 'Ridimensiona' acceso, girare un JPEG che il piano lascia com'è resta senza
 * perdita.
 *
 * ⚠️ **Il JPEG è vero**, 1600 per 900, scritto nella cache dell'app: il salvataggio ne legge
 * l'intestazione come farebbe col file di un telefono.
 * ⚠️ `@GraphicsMode(NATIVE)`: con la grafica di fabbrica di Robolectric il JPEG non si scrive.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
class PianoInerteTest {

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private fun jpeg(nome: String, orientamento: Int? = null): File {
        val cartella = File(app.cacheDir, "piano-inerte").also { it.mkdirs() }
        val file = File(cartella, nome)
        val mappa = Bitmap.createBitmap(1600, 900, Bitmap.Config.ARGB_8888)
        file.outputStream().use { mappa.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        if (orientamento != null) {
            ExifInterface(file).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, orientamento.toString())
                saveAttributes()
            }
        }
        return file
    }

    private val intero = ImageEdit.Crop.WHOLE

    /**
     * **Il piano si misura sulle misure che si scriverebbero**: lato lungo, verso, ritaglio.
     *
     * ⚠️ 'Larghezza 1000' è il caso che distingue il verso: dritta l'immagine è larga 1600 e il
     * piano rimpicciolisce, girata di un quarto è larga 900 e non fa niente.
     */
    @Test
    fun `il piano si misura sul file vero`() {
        val file = jpeg("dritta.jpg")
        val lungo2048 = Resize.Plan(Resize.Mode.LONG, 2048)
        val lungo1000 = Resize.Plan(Resize.Mode.LONG, 1000)
        val largo1000 = Resize.Plan(Resize.Mode.WIDE, 1000)

        assertFalse(ImageEdit.shrinks(file, lungo2048, 1, intero))
        assertTrue(ImageEdit.shrinks(file, lungo1000, 1, intero))
        assertFalse(ImageEdit.shrinks(file, lungo1000, 0, ImageEdit.Crop(0f, 0f, 0.5f, 1f)))
        assertTrue(ImageEdit.shrinks(file, largo1000, 0, intero))
        assertFalse(ImageEdit.shrinks(file, largo1000, 1, intero))
        // ⚠️ Un quarto di giro nell'EXIF e uno nella posa si annullano.
        assertTrue(ImageEdit.shrinks(jpeg("exif.jpg", ExifInterface.ORIENTATION_ROTATE_90), largo1000, 1, intero))
    }

    /**
     * **Girare un JPEG con un piano che non lo rimpicciolisce resta senza perdita.**
     *
     * ⚠️ **Controprovata** passando a `save` il piano senza `shrinks`: il salvataggio ridisegna e
     * `lossless` è falso.
     */
    @Test
    fun `girare con un piano inerte resta senza perdita`() {
        val file = jpeg("girata.jpg")
        val esito = runBlocking {
            ImageEdit.save(
                app, Uri.fromFile(file), turns = 1, mirror = false, crop = intero,
                way = ImageEdit.Way.COPY, backup = false,
                resize = Resize.Plan(Resize.Mode.LONG, 2048)
            )
        }

        assertTrue("$esito", esito is ImageEdit.Result.Done)
        assertEquals(true, (esito as ImageEdit.Result.Done).lossless)
    }
}

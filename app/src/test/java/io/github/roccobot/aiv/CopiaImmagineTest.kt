package io.github.roccobot.aiv

import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 'Copia immagine' nella `6.27` (rilievi C40 ed E31 della revisione della 6.25): diventata una
 * funzione sospesa con la PNG scritta su `Dispatchers.IO`, mette ancora negli appunti l'immagine
 * intera.
 *
 * ⚠️⚠️ **IL BANCO NON VEDE SU QUALE FILO GIRA LA COMPRESSIONE**: una bitmap non si può osservare
 * mentre si comprime, e un confronto di tempi sul banco sarebbe una prova che cade a caso. Questa
 * prova presidia il comportamento, cioè che lo spostamento non abbia perso gli appunti; il filo lo
 * dice il codice, e la voce di collaudo chiede di copiare una foto grande.
 * ⚠️ `@GraphicsMode(NATIVE)`: con la grafica di fabbrica di Robolectric la PNG non si scrive.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
class CopiaImmagineTest {

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * **Gli appunti ricevono l'indirizzo della PNG, e la PNG ha le misure dell'immagine.**
     *
     * ⚠️ **Controprovata** facendo rispondere `copyImage` prima di scrivere gli appunti: la clip
     * resta vuota.
     */
    @Test
    fun `copia immagine mette la png negli appunti`() {
        val immagine = LoadedImage(
            bitmap = ImageBitmap(120, 80), mimeType = "image/jpeg", byteSize = 1,
            pixelWidth = 120, pixelHeight = 80, sampled = false, displayName = "foto.jpg"
        )

        assertTrue(runBlocking { ImageActions.copyImage(app, immagine) })

        val clip = app.getSystemService(ClipboardManager::class.java).primaryClip!!
        assertEquals(1, clip.itemCount)
        val png = File(File(app.cacheDir, "share"), "clipboard.png")
        val misure = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(png.path, misure)
        assertEquals(120, misure.outWidth)
        assertEquals(80, misure.outHeight)
    }
}

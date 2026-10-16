package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Il nome di un'immagine del web nella `6.26` (rilievo E30 della revisione della 6.25): resta
 * sotto il tetto del filesystem e senza caratteri di controllo, così 'Condividi' e i salvataggi
 * non vanno in errore.
 *
 * ⚠️ **Il nome viene dall'ultimo pezzo dell'indirizzo**, come per un'immagine aperta da un link:
 * `displayName` è nullo.
 * ⚠️ `@GraphicsMode(NATIVE)` per la sola `ImageBitmap` di [LoadedImage]: con la grafica di
 * fabbrica di Robolectric la bitmap è nulla e la prova cade prima di arrivare al nome.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
class NomeLungoTest {

    private fun immagine() = LoadedImage(
        bitmap = ImageBitmap(1, 1),
        mimeType = "image/jpeg",
        byteSize = 1,
        pixelWidth = 1,
        pixelHeight = 1,
        sampled = false,
        displayName = null
    )

    /**
     * **Un ultimo pezzo di 300 caratteri dà un nome sotto i 255 byte, col suo suffisso.**
     *
     * ⚠️ **Controprovata** togliendo il tetto da `ImageActions.fileName`: il nome supera i 255
     * byte e il primo confronto cade.
     */
    @Test
    fun `un indirizzo lunghissimo da un nome che il filesystem accetta`() {
        val pezzo = "A".repeat(150) + "è".repeat(150)
        val nome = ImageActions.fileName(immagine(), Uri.parse("https://esempio.it/foto/$pezzo=w1200"))

        assertTrue("${nome.toByteArray().size} byte", nome.toByteArray().size <= 255)
        assertTrue(nome, nome.endsWith(".jpg"))
        assertTrue(nome, nome.startsWith("AAAA"))
    }

    /**
     * **Il carattere nullo e gli altri di controllo non entrano nel nome.**
     *
     * ⚠️ **Controprovata** togliendo i caratteri di controllo dall'espressione dei vietati: il
     * carattere nullo resta.
     */
    @Test
    fun `i caratteri di controllo escono dal nome`() {
        val nome = ImageActions.fileName(immagine(), Uri.parse("https://esempio.it/foto%00due%0Atre.png"))

        assertFalse(nome, nome.any { it.code < 0x20 })
        assertEquals("foto_due_tre.png", nome)
    }
}

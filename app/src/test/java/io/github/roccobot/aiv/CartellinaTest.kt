package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Dove cade la cartella AIV nell'icona di 'Faro', dalla `6.29` (sua richiesta del 2026-10-16,
 * `Cartellina`, e la sua scelta *1,5dp: VAI!* sulle anteprime).
 *
 * ⚠️ **Si misura sui pixel dell'icona vera** (`Shortcuts.folderBitmap`), su un fondo nero: le righe
 * che contengono inchiostro sono quelle della cartella, linguetta compresa.
 * ⚠️ **Il numero che ne viene è un fatto, non una regola scelta qui**: coi 4,575 dp della sua
 * scelta il riquadro della cartella intera cade al centro dell'icona, entro un decimo di dp.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CartellinaTest {

    /**
     * **La cartella, linguetta compresa, è centrata nell'icona in verticale.**
     *
     * ⚠️ **Controprovata** con la posizione della 6.28 (nessuna discesa): il riquadro della
     * cartella stava circa 4,6 dp sopra il centro.
     */
    @Test
    fun `la cartella è centrata nell'icona`() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val icona = Shortcuts.folderBitmap(app, Color.BLACK)
        val righe = (0 until icona.height).filter { y ->
            (0 until icona.width).any { x -> Color.red(icona.getPixel(x, y)) > 64 }
        }
        val centroCartella = (righe.first() + righe.last() + 1) / 2f
        val scartoDp = (centroCartella - icona.height / 2f) / app.resources.displayMetrics.density
        assertEquals("La cartella è a $scartoDp dp dal centro", 0f, scartoDp, 0.5f)
    }
}

package io.github.roccobot.aiv

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Le copie del salvataggio nella `6.26` (rilievo B15 della revisione della 6.25): un errore nel
 * girare, ritagliare, ridimensionare o appiattire arriva al salvataggio, che risponde 'troppo
 * grande' e non scrive un file senza la modifica.
 *
 * ⚠️⚠️ **L'ERRORE SI PROVOCA CON UNA BITMAP GIÀ RICICLATA**: un errore di memoria non si chiede a
 * comando, mentre copiare una bitmap riciclata va in errore nello stesso punto, cioè dentro la
 * copia. Quello che si misura è la rete: con lei l'errore sparisce e torna l'immagine di prima,
 * senza di lei arriva a chi chiama.
 * ⚠️ `@GraphicsMode(NATIVE)`: con la grafica di fabbrica di Robolectric una bitmap riciclata si
 * copia senza errori, e la prova non misurerebbe niente.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
class SalvataggioSenzaReteTest {

    private fun riciclata(alfa: Boolean = false) =
        Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888).apply {
            setHasAlpha(alfa)
            recycle()
        }

    /** Se [blocco] va in errore. */
    private fun fallisce(blocco: () -> Unit): Boolean = runCatching(blocco).isFailure

    /**
     * **Nelle anteprime la rete resta, nei salvataggi no.**
     *
     * ⚠️ **Controprovata** rimettendo la rete in `netted` anche con `strict`: le due copie
     * tornano l'immagine di prima e i due `fallisce` cadono.
     */
    @Test
    fun `girare e ritagliare lasciano passare l'errore solo nei salvataggi`() {
        val giro = riciclata()
        val taglio = ImageEdit.Crop(0.1f, 0.1f, 0.6f, 0.6f)

        assertSame(giro, giro.spunBy(1, false))
        assertSame(giro, giro.cutTo(taglio))
        assertTrue("girare", fallisce { giro.spunBy(1, false, strict = true) })
        assertTrue("ritagliare", fallisce { giro.cutTo(taglio, strict = true) })
    }

    /**
     * **Ridimensionare e appiattire, che servono solo a scrivere, lasciano passare l'errore.**
     *
     * ⚠️ **Controprovata** rimettendo il `runCatching` in `Resize.apply` e in `Convert.flatten`:
     * tornano `null` e l'immagine con la trasparenza.
     */
    @Test
    fun `ridimensionare e appiattire lasciano passare l'errore`() {
        assertTrue("ridimensionare", fallisce {
            Resize.apply(riciclata(), Resize.Plan(Resize.Mode.LONG, 10))
        })
        assertTrue("appiattire", fallisce { Convert.flatten(riciclata(alfa = true)) })
    }
}

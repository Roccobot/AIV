package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * 'Converti/Esporta' sui telefoni che l'app accetta sotto Android 11, dalla `2.99`.
 *
 * ⚠️⚠️ **NASCE CON LA CORREZIONE, ED È UN DIFETTO CHE NESSUNO HA VISTO SU UN TELEFONO**: fino alla
 * `2.98` l'elenco dei formati nominava `WEBP_LOSSY` e `WEBP_LOSSLESS` nei propri campi, cioè due
 * costanti che nascono con Android 11, mentre l'app si installa da Android 9. Leggere la prima voce
 * faceva fallire l'inizializzazione dell'elenco intero, e con lui la finestra.
 * ⚠️⚠️ **LE PROVE GIRANO SULLE PIATTAFORME VERE DI ANDROID 9, 10 E 11** (`@Config(sdk)`), perché
 * è la piattaforma a non avere quelle due costanti: su quella di serie del banco, la 36, il difetto
 * non esiste. Robolectric scarica le tre la prima volta.
 * ⚠️ **Controprovata rimettendo il formato come campo della voce**: su Android 9 e 10 le prove
 * cadono con `NoSuchFieldError`, e su Android 11 no.
 * ⚠️ **I nomi delle prove non portano lettere accentate**, per la ragione scritta in
 * `SalvataggioTest`: con una codifica di sistema stretta il rapporto non si genera.
 */
@RunWith(AndroidJUnit4::class)
class ConvertiTest {

    /** Ogni voce offerta si scrive, e il file che ne esce non è vuoto. */
    private fun scriveTutto() {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        Convert.Target.entries.filter { it.offered }.forEach { voce ->
            val out = java.io.ByteArrayOutputStream()
            assertTrue("${voce.name} non si scrive", bitmap.compress(voce.format, voce.qualityFor(80), out))
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun `su Android 9 l'elenco regge e la WebP senza perdita manca`() {
        assertEquals(
            listOf(Convert.Target.JPEG, Convert.Target.PNG, Convert.Target.WEBP_LOSSY),
            Convert.Target.entries.filter { it.offered }
        )
        scriveTutto()
    }

    /**
     * ⚠️ **Su Android 10 le due WebP sono la stessa costante**, e a distinguerle è la qualità: il
     * senza perdita chiede 100 qualunque cosa la finestra passi.
     */
    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `su Android 10 le due WebP si distinguono per la qualita`() {
        assertTrue(Convert.Target.WEBP_LOSSLESS.offered)
        @Suppress("DEPRECATION")
        assertEquals(Bitmap.CompressFormat.WEBP, Convert.Target.WEBP_LOSSLESS.format)
        assertEquals(100, Convert.Target.WEBP_LOSSLESS.qualityFor(40))
        assertEquals(40, Convert.Target.WEBP_LOSSY.qualityFor(40))
        scriveTutto()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `da Android 11 le due WebP sono le costanti loro`() {
        assertEquals(Bitmap.CompressFormat.WEBP_LOSSY, Convert.Target.WEBP_LOSSY.format)
        assertEquals(Bitmap.CompressFormat.WEBP_LOSSLESS, Convert.Target.WEBP_LOSSLESS.format)
        assertFalse(Convert.Target.entries.any { !it.offered })
        scriveTutto()
    }
}

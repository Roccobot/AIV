package io.github.roccobot.aiv

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * L'indonesiano risponde con tutti e due i codici (6.29, scelta Q1 dell'utente). Le traduzioni
 * vivono in `values-id`, l'unica cartella del sorgente; Android ha usato a lungo il codice vecchio
 * `in`, e le librerie AndroidX tengono le loro stringhe in `values-in`, quindi la build copia la
 * cartella anche con quel nome (`app/build.gradle.kts`, il compito `indonesianIn`).
 */
@RunWith(RobolectricTestRunner::class)
class IndonesianoTest {

    private fun impostazioni() =
        ApplicationProvider.getApplicationContext<Context>().getString(R.string.settings_title)

    @Test
    @Config(qualifiers = "id-rID")
    fun `col codice id l'app parla indonesiano`() {
        assertEquals("Pengaturan", impostazioni())
    }

    /** Il controllo del metodo: con una lingua che ha sempre funzionato la prova legge la traduzione. */
    @Test
    @Config(qualifiers = "it-rIT")
    fun `con l'italiano la prova legge la traduzione`() {
        assertEquals("Impostazioni", impostazioni())
    }

    @Test
    @Config(qualifiers = "in-rID")
    fun `col codice in l'app parla indonesiano`() {
        assertEquals("Pengaturan", impostazioni())
    }
}

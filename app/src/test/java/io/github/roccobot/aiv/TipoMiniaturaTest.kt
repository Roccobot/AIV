package io.github.roccobot.aiv

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

/**
 * Le domande sul tipo prima di chiedere una miniatura al sistema, nella `6.28` (rilievo D22 della
 * revisione della 6.25, voce C10 del rapporto).
 *
 * ⚠️ **Il fornitore è finto e conta**: risponde il tipo che gli si dà e tiene il conto delle
 * domande, che è proprio quello che passa da un processo all'altro sul telefono.
 */
@RunWith(AndroidJUnit4::class)
class TipoMiniaturaTest {

    class Contatore : ContentProvider() {
        override fun onCreate() = true
        override fun getType(uri: Uri): String {
            domande++
            return tipo
        }
        override fun query(u: Uri, p: Array<String>?, s: String?, a: Array<String>?, o: String?): Cursor? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, s: String?, a: Array<String>?) = 0
        override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<String>?) = 0

        companion object {
            var domande = 0
            var tipo = "image/jpeg"
        }
    }

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * **Un JPEG, che il sistema sa fare, costa una domanda sola.**
     *
     * ⚠️ **Controprovata** con le tre prove che chiedevano il tipo ognuna per sé: tre domande.
     */
    @Test
    fun `una domanda sola sul tipo`() {
        Robolectric.setupContentProvider(Contatore::class.java, "prova.tipo")
        Contatore.tipo = "image/jpeg"
        Contatore.domande = 0
        assertFalse(declines(app, Uri.parse("content://prova.tipo/7")))
        assertEquals(1, Contatore.domande)
    }

    /** **BMP e HEIC si fanno in casa**, come prima, col tipo letto una volta. */
    @Test
    fun `bmp e heic restano in casa`() {
        Robolectric.setupContentProvider(Contatore::class.java, "prova.tipo")
        Contatore.tipo = "image/bmp"
        assertTrue(declines(app, Uri.parse("content://prova.tipo/8")))
        Contatore.tipo = "image/heic"
        assertTrue(declines(app, Uri.parse("content://prova.tipo/9")))
        assertTrue(declines(app, Uri.parse("file:///sdcard/foto.heif")))
        assertFalse(declines(app, Uri.parse("file:///sdcard/foto.jpg")))
    }
}

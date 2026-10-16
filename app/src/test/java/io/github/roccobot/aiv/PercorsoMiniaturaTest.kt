package io.github.roccobot.aiv

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import java.io.File

/**
 * Il percorso di una miniatura su disco, nella `6.28` (rilievo C10 della revisione della 6.25, voce
 * C10 del rapporto).
 *
 * ⚠️ **Il fornitore è finto e conta**: risponde il percorso che gli si dà e tiene il conto delle
 * domande, che sul telefono passano da un processo all'altro.
 */
@RunWith(AndroidJUnit4::class)
class PercorsoMiniaturaTest {

    class Contatore : ContentProvider() {
        override fun onCreate() = true
        override fun getType(uri: Uri): String = "image/jpeg"
        @Suppress("DEPRECATION")
        override fun query(u: Uri, p: Array<String>?, s: String?, a: Array<String>?, o: String?): Cursor {
            domande++
            return MatrixCursor(arrayOf(MediaStore.Images.Media.DATA)).apply { addRow(arrayOf(percorso)) }
        }
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, s: String?, a: Array<String>?) = 0
        override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<String>?) = 0

        companion object {
            var domande = 0
            var percorso = ""
        }
    }

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * **Dieci letture della stessa miniatura chiedono il percorso una volta.**
     *
     * ⚠️ **Controprovata** senza il percorso ricordato: dieci letture, dieci domande.
     */
    @Test
    fun `il percorso si chiede una volta`() {
        Robolectric.setupContentProvider(Contatore::class.java, "prova.percorso")
        val foto = File(app.filesDir, "percorso-1.jpg").apply { writeText("x") }
        Contatore.percorso = foto.absolutePath
        Contatore.domande = 0
        val indirizzo = Uri.parse("content://prova.percorso/1")
        repeat(10) { AvifCache.read(app, indirizzo, Thumbs.PX) }
        assertEquals(1, Contatore.domande)
    }

    /**
     * **Un file rinominato si ritrova**: il percorso ricordato non porta più a niente, e si chiede
     * di nuovo; una riscrittura cambia la chiave, perché la data si legge sempre dal file.
     */
    @Test
    fun `un file spostato si richiede e una riscrittura cambia la chiave`() {
        Robolectric.setupContentProvider(Contatore::class.java, "prova.percorso")
        val prima = File(app.filesDir, "percorso-2.jpg").apply { writeText("x") }
        Contatore.percorso = prima.absolutePath
        val indirizzo = Uri.parse("content://prova.percorso/2")
        val chiave = AvifCache.fileFor(app, indirizzo, Thumbs.PX)
        val dopo = File(app.filesDir, "percorso-2-nuovo.jpg")
        prima.renameTo(dopo)
        dopo.setLastModified(prima.lastModified() + 60_000)
        Contatore.percorso = dopo.absolutePath
        Contatore.domande = 0
        val nuova = AvifCache.fileFor(app, indirizzo, Thumbs.PX)
        assertEquals(1, Contatore.domande)
        assertNotEquals(chiave, nuova)
    }
}

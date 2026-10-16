package io.github.roccobot.aiv

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

/**
 * Le cartelle d'origine di una copia, nella `6.28` (rilievo E8 della revisione della 6.25, voce C12
 * del rapporto): la finestra della destinazione le aspetta prima di comparire.
 *
 * ⚠️ **Il fornitore è finto e conta**: risponde il percorso `/sdcard/<cartella>/<numero>.jpg`, una
 * cartella ogni cento numeri, sia alla domanda su un indirizzo sia a quella per blocchi.
 */
@RunWith(AndroidJUnit4::class)
class OriginiTest {

    class Contatore : ContentProvider() {
        override fun onCreate() = true
        override fun getType(uri: Uri): String = "image/jpeg"

        private fun percorso(id: String) = "/sdcard/cartella${id.toLong() / 100}/$id.jpg"

        @Suppress("DEPRECATION")
        override fun query(u: Uri, p: Array<String>?, s: String?, a: Array<String>?, o: String?): Cursor {
            domande++
            val righe = MatrixCursor(arrayOf(MediaStore.Images.Media.DATA))
            val ids = a?.toList() ?: listOfNotNull(u.lastPathSegment)
            ids.forEach { righe.addRow(arrayOf(percorso(it))) }
            return righe
        }
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, s: String?, a: Array<String>?) = 0
        override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<String>?) = 0

        companion object {
            var domande = 0
        }
    }

    /**
     * **Milleduecento file costano tre domande, e le cartelle sono quelle giuste.**
     *
     * ⚠️ **Controprovata** con una domanda per indirizzo: milleduecento domande.
     */
    @Test
    fun `una domanda per blocco`() {
        Robolectric.setupContentProvider(Contatore::class.java, "prova.origini")
        val scelti = (0 until 1200).map { Uri.parse("content://prova.origini/images/$it") }
        Contatore.domande = 0
        val cartelle = FileTree.sourcesOf(ApplicationProvider.getApplicationContext(), scelti)
        assertEquals(3, Contatore.domande)
        assertEquals(12, cartelle.size)
    }
}

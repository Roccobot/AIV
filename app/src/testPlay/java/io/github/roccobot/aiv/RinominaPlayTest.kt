package io.github.roccobot.aiv

import android.app.PendingIntent
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

/*
 * 'Rinomina' in AIV Play (5.50): from Android 11 the files are renamed through the MediaStore,
 * after the system's window.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️⚠️ **What is measured**: the key exists from Android 11 and not below; Android's window is
 * asked once, and a 'no' touches nothing and says nothing; the order is alphabetical, the numbers
 * come from the template, and a series that is renumbered onto itself loses no file; the names
 * kept in memory are forgotten.
 * ⚠️ The provider stands in for the MediaStore: it answers a file's row by its path, and renames
 * the real file on disk when a row gets a new `DISPLAY_NAME`, refusing a name already taken, as
 * the system does. What the phone's MediaStore does with the names is for the test round.
 * ⚠️ Runs only in the `play` variant: see `AccessoPlayTest`.
 */
@RunWith(AndroidJUnit4::class)
class RinominaPlayTest {

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /** The MediaStore as far as the rename goes: rows by path, renames on disk, the window. */
    class Media : ContentProvider() {
        override fun onCreate() = true
        override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
            chieste += method
            val window = PendingIntent.getActivity(context!!, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
            return Bundle().apply { putParcelable("result", window) }
        }
        override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? {
            val at = righe.indexOf(a?.singleOrNull())
            return MatrixCursor(arrayOf(MediaStore.MediaColumns._ID)).apply { if (at >= 0) addRow(arrayOf<Any>(at.toLong())) }
        }
        override fun update(uri: Uri, values: ContentValues?, s: String?, a: Array<out String>?): Int {
            val at = uri.lastPathSegment?.toIntOrNull() ?: return 0
            val name = values?.getAsString(MediaStore.MediaColumns.DISPLAY_NAME) ?: return 0
            val file = File(righe[at])
            val target = File(file.parentFile, name)
            if (target.exists() || !file.renameTo(target)) return 0
            righe[at] = target.absolutePath
            nomi += name
            return 1
        }
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0

        companion object {
            val chieste = mutableListOf<String>()
            val righe = mutableListOf<String>()
            val nomi = mutableListOf<String>()
        }
    }

    private lateinit var cartella: File

    @Before
    fun prepara() {
        Media.chieste.clear()
        Media.righe.clear()
        Media.nomi.clear()
        Robolectric.buildContentProvider(Media::class.java).create(MediaStore.AUTHORITY)
        cartella = File(app.filesDir, "rinomina").apply { deleteRecursively(); mkdirs() }
    }

    /** Real files whose content is their first name, each with its row; returns their addresses. */
    private fun file(vararg nomi: String): List<Uri> = nomi.map { nome ->
        val f = File(cartella, nome).apply { writeText(nome) }
        Media.righe += f.absolutePath
        Uri.fromFile(f)
    }

    /** What is in the folder now: name to first name. */
    private fun cartella(): Map<String, String> =
        cartella.listFiles().orEmpty().associate { it.name to it.readText() }

    /** A [SystemAsk] that answers [yes] to Android's window, and counts the windows. */
    private fun sistema(yes: Boolean, finestre: MutableList<IntentSenderRequest>): SystemAsk {
        lateinit var ask: SystemAsk
        ask = SystemAsk(app, { finestre += it; ask.answer(yes) }, { ask.answer(false) })
        return ask
    }

    private fun rinomina(uris: List<Uri>, template: String, start: Int, yes: Boolean = true): Pair<FileTree.Outcome, Int> {
        val finestre = mutableListOf<IntentSenderRequest>()
        val esito = runBlocking { MediaRename.rename(app, uris, template, start, sistema(yes, finestre)) }
        return esito to finestre.size
    }

    /** **The key is back from Android 11**, in the two file pads. */
    @Test
    fun `da Android 11 c'e Rinomina`() {
        assertTrue(Store.renames)
        assertTrue(Store.allows(PadKey.RENAME))
    }

    /** **Below Android 11 AIV Play does not rename**, and the key is not there. */
    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `su Android 10 Rinomina non c'e`() {
        assertFalse(Store.renames)
        assertFalse(Store.allows(PadKey.RENAME))
    }

    /**
     * **One window of Android for all the files, then the names in alphabetical order**, with the
     * numbers of the template and each file's own extension.
     */
    @Test
    fun `con il si di Android i file prendono i nomi in ordine alfabetico`() {
        val (esito, finestre) = rinomina(file("c.jpg", "a.jpg", "b.png"), "Museo ##", 1)
        assertEquals(FileTree.Outcome(3, 0), esito)
        assertEquals(1, finestre)
        assertEquals(listOf("create_write_request"), Media.chieste)
        assertEquals(mapOf("Museo 01.jpg" to "a.jpg", "Museo 02.png" to "b.png", "Museo 03.jpg" to "c.jpg"), cartella())
    }

    /**
     * **A series renumbered onto itself loses no file**: `s 1` becomes `s 2` while the old `s 2`
     * is still there, and only the two phases let it through.
     */
    @Test
    fun `una serie rinumerata su se stessa non perde file`() {
        val (esito, _) = rinomina(file("s 1.jpg", "s 2.jpg"), "s #", 2)
        assertEquals(FileTree.Outcome(2, 0), esito)
        assertEquals(mapOf("s 2.jpg" to "s 1.jpg", "s 3.jpg" to "s 2.jpg"), cartella())
    }

    /** **A name taken by a file outside the selection gets a free one.** */
    @Test
    fun `un nome preso da un altro file ne prende uno libero`() {
        File(cartella, "Museo 01.jpg").writeText("estraneo")
        val (esito, _) = rinomina(file("a.jpg"), "Museo ##", 1)
        assertEquals(FileTree.Outcome(1, 0), esito)
        assertEquals("a.jpg", cartella()["Museo 01 (2).jpg"])
        assertEquals("estraneo", cartella()["Museo 01.jpg"])
    }

    /** **A no in Android's window touches nothing and says nothing.** */
    @Test
    fun `col no di Android non succede niente`() {
        val (esito, finestre) = rinomina(file("a.jpg"), "Museo ##", 1, yes = false)
        assertEquals(FileTree.Outcome(0, 0), esito)
        assertEquals(1, finestre)
        assertTrue("Si è rinominato: ${Media.nomi}", Media.nomi.isEmpty())
        assertEquals(mapOf("a.jpg" to "a.jpg"), cartella())
        assertFalse("L'esito parla", FileKind.RENAME.speaks(esito))
    }

    /** **The name kept in memory is forgotten**, or the grid would show the old one. */
    @Test
    fun `il nome in memoria si dimentica`() {
        val uri = file("a.jpg").single()
        runBlocking { Names.of(app, uri) }
        assertNotNull(Names.cached(uri))
        rinomina(listOf(uri), "Museo ##", 1)
        assertNull(Names.cached(uri))
    }
}

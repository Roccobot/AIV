package io.github.roccobot.aiv

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.GraphicsMode
import java.io.File

/*
 * AIV Play's editors save into Download, and never beside or over the original (5.10).
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️ `@GraphicsMode(NATIVE)` in a class of its own, because the save decodes and writes real
 * pixels (`Rules.md`, § 'Quando si scrive una prova, e quando no').
 * ⚠️ **Download is a fake provider on the `media` authority** ([FintoMedia]): the bench has no
 * MediaStore, and without it every save would answer 'not saved' for a reason that is not the
 * app's.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SalvaPlayTest {

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private lateinit var download: FintoMedia

    @Before
    fun montaDownload() {
        download = Robolectric.setupContentProvider(FintoMedia::class.java, MediaStore.AUTHORITY)
    }

    /** An image of 40 × 20 in a folder of its own, as a camera would leave it. */
    private fun originale(): File {
        val dir = File(app.filesDir, "DCIM").apply { mkdirs() }
        val bitmap = Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        return File(dir, "prova.png").apply { outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }

    @Test
    fun `il Salva dell'editor semplice scrive in Download e lascia stare l'originale`() = runBlocking {
        val file = originale()
        val prima = file.readBytes()
        val esito = ImageEdit.save(
            app, file.toUri(), turns = 1, mirror = false, crop = ImageEdit.Crop.WHOLE,
            way = ImageEdit.Way.OVERWRITE, backup = true
        )
        assertTrue("Non salvato: $esito", esito is ImageEdit.Result.Done && esito.downloads)
        assertArrayEquals("L'originale è cambiato", prima, file.readBytes())
        assertEquals("Accanto all'originale c'è altro", listOf("prova.png"), file.parentFile!!.list()!!.toList())
        assertEquals("La copia provvisoria è rimasta", 0, File(app.cacheDir, "editor").list()?.size ?: 0)

        val riga = download.rows.single()
        assertEquals("prova.png", riga.getAsString(MediaStore.Downloads.DISPLAY_NAME))
        assertEquals("image/png", riga.getAsString(MediaStore.Downloads.MIME_TYPE))
        assertEquals(0, riga.getAsInteger(MediaStore.Downloads.IS_PENDING))
        // ⚠️ The quarter turn is in the pixels: what arrived is the edit, not a copy.
        val scritta = BitmapFactory.decodeFile(download.files.values.single().path)
        assertEquals(20, scritta.width)
        assertEquals(40, scritta.height)
    }

    @Test
    fun `il Salva dell'editor completo scrive in Download e lascia stare l'originale`() = runBlocking {
        val file = originale()
        val prima = file.readBytes()
        val esito = ImageEdit.saveLook(
            app, file.toUri(), Look(spin = Spin(1, false)), Quality.HIGH, backup = true
        )
        assertTrue("Non salvato: $esito", esito is ImageEdit.Result.Done && esito.downloads)
        assertArrayEquals("L'originale è cambiato", prima, file.readBytes())
        assertEquals("Accanto all'originale c'è altro", listOf("prova.png"), file.parentFile!!.list()!!.toList())
        assertEquals("prova.png", download.rows.single().getAsString(MediaStore.Downloads.DISPLAY_NAME))
    }

    /**
     * **The full editor's own road too**, the one that develops the pixels: with a healing patch
     * the save does not go through [ImageEdit.save], and decides on its own where to write.
     */
    @Test
    fun `il Salva con una correzione vera scrive in Download e lascia stare l'originale`() = runBlocking {
        val file = originale()
        val prima = file.readBytes()
        val zona = Healing.Selection().add(
            listOf(Offset(0.4f, 0.4f), Offset(0.6f, 0.4f), Offset(0.6f, 0.6f), Offset(0.4f, 0.6f))
        )
        val toppa = Healing.prepare(app, file.toUri(), null, Healing.Plan.NONE, zona)!!
        val esito = ImageEdit.saveLook(
            app, file.toUri(), Look(healing = Healing.Plan(listOf(toppa))), Quality.HIGH, backup = true
        )
        assertTrue("Non salvato: $esito", esito is ImageEdit.Result.Done && esito.downloads)
        assertArrayEquals("L'originale è cambiato", prima, file.readBytes())
        assertEquals("Accanto all'originale c'è altro", listOf("prova.png"), file.parentFile!!.list()!!.toList())
        assertEquals("prova.png", download.rows.single().getAsString(MediaStore.Downloads.DISPLAY_NAME))
    }
}

/**
 * Download on the bench: it keeps what was inserted, and gives each row a real file to write.
 */
class FintoMedia : ContentProvider() {
    val rows = mutableListOf<ContentValues>()
    val files = mutableMapOf<Uri, File>()

    override fun onCreate() = true

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        val row = Uri.withAppendedPath(uri, rows.size.toString())
        rows += ContentValues(values)
        files[row] = File.createTempFile("riga", ".bin")
        return row
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor =
        ParcelFileDescriptor.open(files.getValue(uri), ParcelFileDescriptor.parseMode(mode))

    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?): Int {
        rows[uri.lastPathSegment!!.toInt()].putAll(values)
        return 1
    }

    override fun delete(uri: Uri, selection: String?, args: Array<out String>?): Int {
        files.remove(uri)?.delete()
        return 1
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, order: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
}

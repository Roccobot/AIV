package io.github.roccobot.aiv

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/*
 * AIV Play's bin page (5.40, his choice A1): from Android 11 it shows the system's trash, and its
 * three actions go through the system's windows.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️⚠️ **What is measured**: the page exists from Android 11 and not below; the list is the
 * system's trash, newest first and without PSD files; 'Ripristina' is in the bin's pads; 'Ripristina
 * tutto' and 'Svuota il cestino' ask Android's window with no window of AIV's own before it, and
 * ask the right one; 'Cronologia' is not there, since the system does not record where files
 * came from.
 * ⚠️ What a real phone shows (thumbnails and Info of a trashed file, the text of Android's
 * windows) is for the test round.
 * ⚠️ Runs only in the `play` variant: see `AccessoPlayTest`.
 */
@RunWith(AndroidJUnit4::class)
class CestinoPlayTest {

    @get:Rule
    val banco = createAndroidComposeRule<ComponentActivity>()

    private fun testo(id: Int) = banco.activity.getString(id)

    /**
     * The MediaStore's provider, for the trash: it answers the trashed rows of each collection,
     * and a request with a window, keeping which request it was and the values it carried.
     */
    class Media : ContentProvider() {
        override fun onCreate() = true
        override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
            chieste += method
            @Suppress("DEPRECATION")
            valori += extras?.keySet()?.firstNotNullOfOrNull { extras.get(it) as? ContentValues }
            val window = PendingIntent.getActivity(context!!, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
            return Bundle().apply { putParcelable("result", window) }
        }
        override fun query(uri: Uri, p: Array<out String>?, args: Bundle?, signal: CancellationSignal?): Cursor? {
            argomenti += args
            val righe = if (uri.toString().contains("video")) VIDEO else IMMAGINI
            return MatrixCursor(p).apply { righe.forEach { addRow(it) } }
        }
        override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
        override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0

        companion object {
            val chieste = mutableListOf<String>()
            val valori = mutableListOf<ContentValues?>()
            val argomenti = mutableListOf<Bundle?>()

            /** `_ID`, `DATE_EXPIRES`, `DISPLAY_NAME`, `MIME_TYPE`, as `MediaDelete.trashed` asks. */
            val IMMAGINI = listOf(
                arrayOf<Any>(1L, 100L, "vecchia.jpg", "image/jpeg"),
                arrayOf<Any>(2L, 300L, "nuova.jpg", "image/jpeg"),
                arrayOf<Any>(3L, 400L, "livelli.psd", "image/vnd.adobe.photoshop")
            )
            val VIDEO = listOf(arrayOf<Any>(9L, 200L, "clip.mp4", "video/mp4"))
        }
    }

    @Before
    fun prepara() {
        Media.chieste.clear()
        Media.valori.clear()
        Media.argomenti.clear()
        Robolectric.buildContentProvider(Media::class.java).create(MediaStore.AUTHORITY)
        shadowOf(banco.activity.application).grantPermissions(Manifest.permission.READ_MEDIA_IMAGES)
        runBlocking {
            Hint.COLUMNS.remember(banco.activity)
            Hint.BIN_EMPTY.remember(banco.activity)
        }
    }

    private fun immagine(id: Long) = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "$id")

    /** **The page is there from Android 11, where the system has a trash.** */
    @Test
    fun `il cestino c'e da Android 11`() {
        assertTrue(Store.hasBin)
    }

    /** **Below Android 11 the system has no trash, and the page is not there.** */
    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `su Android 10 il cestino non c'e`() {
        assertFalse(Store.hasBin)
    }

    /**
     * **The list is the system's trash, the most recently trashed first**, images and videos
     * together, without the PSD files that the app never shows.
     */
    @Test
    fun `l'elenco e il cestino di sistema dal piu recente`() {
        val elenco = runBlocking { MediaDelete.trashed(banco.activity) }
        val video = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "9")
        assertEquals(listOf(immagine(2), video, immagine(1)), elenco)
        assertTrue(
            "Non si è chiesto il solo cestino: ${Media.argomenti}",
            Media.argomenti.all { it?.getInt(MediaStore.QUERY_ARG_MATCH_TRASHED) == MediaStore.MATCH_ONLY }
        )
    }

    /**
     * **'Ripristina' is in the bin's pads**: in the bin the key that renames restores. Since 5.50
     * that key is in AIV Play's factory lists, so a saved order that does not name it gets it in
     * its factory place (`padOrderOf`), as in AIV GitHub's bin.
     */
    @Test
    fun `nel cestino c'e Ripristina`() {
        assertTrue(Store.allows(PadKey.RENAME))
        assertTrue("Il menu non ha il tasto: $MENU_KEYS", PadKey.RENAME in MENU_KEYS)
        assertTrue("La selezione non ha il tasto: $PICK_KEYS", PadKey.RENAME in PICK_KEYS)
        assertTrue(PadKey.RENAME in padOrderOf(listOf(PadKey.SHARE.token), MENU_KEYS))
    }

    /** Mounts the bin page on two trashed images, with a system that answers yes; returns what was launched. */
    private fun cestino(): MutableList<Any?> {
        val lanciati = mutableListOf<Any?>()
        val registro = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?
            ) {
                lanciati += input
                @Suppress("UNCHECKED_CAST")
                dispatchResult(requestCode, ActivityResult(Activity.RESULT_OK, null) as O)
            }
        }
        val proprietario = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = registro
        }
        banco.setContent {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides proprietario,
                LocalPillLook provides PillLook(PhonePill.EXTENDED)
            ) {
                AivTheme(darkTheme = false) {
                    GridScreen(title = testo(R.string.bin_title), items = listOf(immagine(1), immagine(2)),
                        highlight = null, onOpen = {}, onBack = {}, onChanged = {}, onSearch = {},
                        bin = true, shape = Adaptive.Shape.PHONE)
                }
            }
        }
        banco.waitForIdle()
        return lanciati
    }

    /** Touches [voce] in the pill and waits for Android's window. */
    private fun tocca(voce: Int, lanciati: List<Any?>) {
        banco.onNodeWithContentDescription(testo(voce)).performClick()
        // ⚠️ The rows are read on `Dispatchers.IO`, which `waitForIdle` does not wait for (see `EliminaPlayTest`).
        banco.waitUntil(timeoutMillis = 5_000) { lanciati.isNotEmpty() }
        banco.waitForIdle()
    }

    /** **'Cronologia' is not there**: the system does not record where a trashed file came from. */
    @Test
    fun `il cestino non ha la Cronologia`() {
        cestino()
        assertEquals(0, banco.onAllNodesWithContentDescription(testo(R.string.bin_history)).fetchSemanticsNodes().size)
        assertEquals(1, banco.onAllNodesWithContentDescription(testo(R.string.bin_empty)).fetchSemanticsNodes().size)
    }

    /** **'Svuota il cestino' asks Android's window for deleting for good, and AIV asks nothing before it.** */
    @Test
    fun `Svuota il cestino chiede solo la finestra di Android`() {
        val lanciati = cestino()
        tocca(R.string.bin_empty, lanciati)
        assertEquals(0, banco.onAllNodesWithText(testo(R.string.bin_empty_ask)).fetchSemanticsNodes().size)
        assertEquals(listOf("create_delete_request"), Media.chieste)
        assertEquals(1, lanciati.size)
    }

    /** **'Ripristina tutto' asks Android's window for taking the files out of the trash**, with nothing before it. */
    @Test
    fun `Ripristina tutto chiede solo la finestra di Android`() {
        val lanciati = cestino()
        tocca(R.string.bin_restore_all, lanciati)
        assertEquals(0, banco.onAllNodesWithText(testo(R.string.bin_restore_all_ask)).fetchSemanticsNodes().size)
        assertEquals(listOf("create_trash_request"), Media.chieste)
        assertEquals("La richiesta rimette nel cestino", 0, Media.valori.single()?.getAsInteger(MediaStore.MediaColumns.IS_TRASHED))
        assertEquals(1, lanciati.size)
    }
}

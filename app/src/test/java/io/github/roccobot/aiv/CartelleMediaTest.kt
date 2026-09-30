package io.github.roccobot.aiv

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ArchivioAperto::class])
class CartelleMediaTest {
    private val app = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before fun provider() {
        ShadowContentResolver.registerProviderInternal("media", MediaFoldersProvider())
    }

    @Test fun `ricerca e generazione rispettano la stessa selezione`() = runBlocking {
        val included = FolderSelection(FolderMode.INCLUDED, included = setOf("Camera"))
        assertEquals(listOf("3"), Folder.everything(app, emptySet(), included).map { it.lastPathSegment })
        val search = Folder.byName(app, "image", emptySet(), selection = included) as Folder.Lookup.Found
        assertEquals(listOf("3"), search.series.items.map { it.lastPathSegment })
        val exact = FolderSelection(exact = setOf("Parent"))
        assertEquals(listOf("2", "3"), Folder.everything(app, emptySet(), exact).map { it.lastPathSegment })
        val searchExact = Folder.byName(app, "image", emptySet(), selection = exact) as Folder.Lookup.Found
        assertEquals(listOf("2", "3"), searchExact.series.items.map { it.lastPathSegment })
    }
}

/** Supplies three matching media rows; filtering itself is the real Folder implementation. */
class MediaFoldersProvider : ContentProvider() {
    override fun onCreate() = true
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val columns = projection ?: arrayOf("_id", "_data", "media_type")
        return MatrixCursor(columns).apply {
            listOf("Parent", "Parent/Child", "Camera").forEachIndexed { index, dir ->
                addRow(columns.map { name -> when (name) {
                    "_id" -> index + 1
                    "_data" -> "/sdcard/$dir/image.jpg"
                    "media_type" -> MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    else -> null
                } })
            }
        }
    }
    override fun getType(uri: Uri): String = "image/jpeg"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
}

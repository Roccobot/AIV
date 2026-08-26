package io.github.roccobot.aiv

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The other pictures in the same folder, so that one opened photo can be leafed
 * through instead of being a dead end.
 *
 * ⚠️⚠️ **The picker cannot answer this question, and that is the whole reason this
 * file exists.** What comes back from the system picker is a permit for ONE item:
 * on Android 13 and up a `content://media/picker/...` address that grants access
 * to that picture and to nothing around it. There is no parent to walk up to. The
 * folder lives in the MediaStore, and reading the MediaStore needs a permission,
 * which is what the user agreed to pay.
 */
object Folder {

    /** One folder, in order, and where the picture on screen sits inside it. */
    data class Series(val items: List<Uri>, val index: Int) {
        val size: Int get() = items.size
        fun at(position: Int): Uri? = items.getOrNull(position)
    }

    /** The permission that lets the MediaStore be read, which is not the same on every version. */
    val permission: String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun granted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * ⚠️⚠️ **Partial access counts as NO access here, and that is deliberate.** From
     * Android 14 the permission dialog offers 'Select photos', which grants
     * `READ_MEDIA_VISUAL_USER_SELECTED` and leaves the MediaStore showing only the
     * handful of pictures the person ticked. A folder of four hundred photos would
     * then answer 'three', and leafing through it would quietly skip everything
     * else: an answer that looks right and is wrong is worse than no answer.
     */
    fun partial(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            !granted(context) &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED

    private val COLUMNS = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE
    )

    /**
     * The folder the picture at [uri] belongs to, or null when there is no folder
     * to speak of: no permission, an address that is not a local file (a web page's
     * picture, a chat's attachment), or a picture the MediaStore has never indexed.
     *
     * ⚠️ Null is also the answer when the folder holds only that one picture: a
     * series of one is not a series, and offering to leaf through it would be a
     * gesture that does nothing.
     */
    suspend fun seriesAround(context: Context, uri: Uri): Series? = withContext(Dispatchers.IO) {
        if (!granted(context)) return@withContext null
        val card = identify(context, uri) ?: return@withContext null
        val bucket = locate(context, uri, card) ?: return@withContext null
        list(context, bucket, card)
    }

    /** Name and size of whatever was opened, which every content provider can answer. */
    private data class Card(val name: String, val size: Long)

    private fun identify(context: Context, uri: Uri): Card? =
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null, null, null
            )?.use { c ->
                if (!c.moveToFirst()) return@use null
                val name = c.getString(0) ?: return@use null
                val size = if (c.isNull(1)) -1L else c.getLong(1)
                Card(name, size)
            }
        }.getOrNull()

    /**
     * Which MediaStore row is the picture on screen, as a bucket id.
     *
     * ⚠️⚠️ **The fast path is CHECKED and not trusted**, and without the check it
     * would be a silent trap: a picker address ends in a number too, and parsing it
     * as a MediaStore id gives a perfectly valid row that is somebody else's photo.
     * The row only counts when its name and size are the ones we opened.
     */
    private fun locate(context: Context, uri: Uri, card: Card): Long? {
        val guessed = runCatching { ContentUris.parseId(uri) }.getOrNull()
        if (guessed != null && guessed > 0) {
            byId(context, guessed)?.let { (bucket, found) ->
                if (found.name == card.name && found.size == card.size) return bucket
            }
        }
        // The address said nothing usable, so the picture is looked up by what it
        // IS: a name and a size. Two different photos sharing both is possible and
        // rare, and the cost of guessing wrong is leafing through the wrong folder.
        return byCard(context, card)
    }

    private fun byId(context: Context, id: Long): Pair<Long, Card>? =
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            COLUMNS,
            "${MediaStore.Images.Media._ID} = ?",
            arrayOf(id.toString()),
            null
        )?.use { c ->
            if (!c.moveToFirst()) return@use null
            c.getLong(1) to Card(c.getString(2) ?: "", c.getLong(3))
        }

    private fun byCard(context: Context, card: Card): Long? =
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            COLUMNS,
            "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.SIZE} = ?",
            arrayOf(card.name, card.size.toString()),
            null
        )?.use { c -> if (c.moveToFirst()) c.getLong(1) else null }

    /**
     * Everything in that bucket, in a fixed order, with the opened picture found
     * inside it.
     *
     * ⚠️ **Ordered by date and not by name**, and the reason is that names sort
     * badly: `IMG_10` comes before `IMG_9` in any lexicographic order, so a folder
     * of numbered photos would leaf through in a jumbled sequence. The id breaks
     * ties, so two photos with the same timestamp still have one order rather than
     * whichever the database feels like today.
     */
    private fun list(context: Context, bucket: Long, card: Card): Series? {
        val items = mutableListOf<Uri>()
        var index = -1
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            COLUMNS,
            "${MediaStore.Images.Media.BUCKET_ID} = ?",
            arrayOf(bucket.toString()),
            "${MediaStore.Images.Media.DATE_MODIFIED} ASC, ${MediaStore.Images.Media._ID} ASC"
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                if (index < 0 && c.getString(2) == card.name && c.getLong(3) == card.size) {
                    index = items.size
                }
                items.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id))
            }
        }
        if (items.size < 2 || index < 0) return null
        return Series(items, index)
    }
}

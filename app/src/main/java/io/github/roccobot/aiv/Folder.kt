package io.github.roccobot.aiv

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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

    /**
     * ⚠️⚠️ **Il permesso è quello PESANTE, l'accesso a tutti i file, ed è una scelta
     * dell'utente**: *preferisco chiedere un permesso pesante prima e poi essere a
     * posto per sempre*. Quello leggero (`READ_MEDIA_IMAGES`) sarebbe bastato a
     * leggere il MediaStore, e la differenza che si paga volentieri è questa: da
     * Android 14 quello leggero apre la porta all'accesso **parziale**, dove la
     * persona spunta tre foto e il MediaStore ne mostra tre, e una cartella da
     * quattrocento risponde 'tre' senza che niente segnali l'inganno. L'accesso a
     * tutti i file quella scelta non ce l'ha.
     *
     * ⚠️ **Non è un dialogo ma una PAGINA DI SISTEMA**: `MANAGE_EXTERNAL_STORAGE` è
     * un permesso speciale, si concede con un interruttore nelle impostazioni e non
     * con il solito 'Consenti'. Da qui esce quindi un intent, non una richiesta.
     */
    fun granted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            // Fino ad Android 10 il permesso ampio non esiste, e quello classico
            // sull'archivio fa già vedere tutto: là si chiede quello, col dialogo.
            ContextCompat.checkSelfPermission(context, legacyPermission) ==
                PackageManager.PERMISSION_GRANTED
        }

    /** Quello da chiedere col dialogo, e serve solo sotto Android 11. */
    const val legacyPermission: String = Manifest.permission.READ_EXTERNAL_STORAGE

    /**
     * La pagina delle impostazioni dove si concede l'accesso a tutti i file.
     *
     * ⚠️ Ne esistono DUE, e la seconda non è un lusso: quella mirata all'app manca
     * su qualche sistema, e senza il ripiego sull'elenco generale la richiesta
     * morirebbe con un'eccezione invece di portare da qualche parte.
     */
    fun settingsIntents(context: Context): List<Intent> = listOf(
        Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            "package:${context.packageName}".toUri()
        ),
        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
    )

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

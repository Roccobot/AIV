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
     * L'esito della ricerca, col PERCHÉ quando non c'è una serie.
     *
     * ⚠️⚠️ **Nato dopo due giri a vuoto, e il difetto era di processo prima che di
     * codice**: finché questa funzione rispondeva `null` e basta, una strisciata che
     * non faceva niente era indistinguibile da una strisciata guasta, e da fuori non
     * c'era modo di sapere quale delle due fosse. Due versioni sono andate perse a
     * indovinare. Un esito che dice la ragione costa quattro stringhe e chiude la
     * domanda in un'occhiata.
     * ⚠️ Quindi chi un domani volesse 'semplificare' rimettendo un `Series?` sappia
     * che toglierebbe l'unica cosa che rende diagnosticabile la funzione.
     */
    sealed interface Lookup {
        /** La cartella c'è, e questa è. */
        data class Found(val series: Series) : Lookup

        /** Manca il permesso: la sola causa che la persona può rimuovere. */
        data object NoPermission : Lookup

        /** Il MediaStore non conosce questa immagine: una chat, il web, un file sciolto. */
        data object NotInGallery : Lookup

        /** La cartella esiste e ha questa foto sola: non c'è niente da sfogliare. */
        data object Alone : Lookup

        /**
         * La cartella ha più foto ma l'aperta non è fra loro.
         * ⚠️ Questo NON dovrebbe succedere, ed è scritto apposta: se compare, il
         * difetto è nel modo in cui si risale alla riga, non nei dati.
         */
        data object Lost : Lookup

        /** La serie quando c'è, e null per tutti gli esiti che spiegano perché non c'è. */
        val seriesOrNull: Series? get() = (this as? Found)?.series
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
     * The folder the picture at [uri] belongs to, or the REASON there is none.
     *
     * ⚠️ Una cartella con quella foto sola non è una serie, e vale `Alone`: offrire
     * di sfogliarla sarebbe un gesto che non porta da nessuna parte.
     */
    suspend fun seriesAround(context: Context, uri: Uri): Lookup = withContext(Dispatchers.IO) {
        if (!granted(context)) return@withContext Lookup.NoPermission
        val card = identify(context, uri) ?: return@withContext Lookup.NotInGallery
        val bucket = locate(context, uri, card) ?: return@withContext Lookup.NotInGallery
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
    private fun list(context: Context, bucket: Long, card: Card): Lookup {
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
        if (items.size < 2) return Lookup.Alone
        if (index < 0) return Lookup.Lost
        return Lookup.Found(Series(items, index))
    }
}

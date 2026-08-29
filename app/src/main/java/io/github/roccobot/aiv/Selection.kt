package io.github.roccobot.aiv

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Che cosa si sa di un gruppo di immagini scelte nella griglia.
 *
 * ⚠️ **Il peso è la somma, il resto è solo per UNA**: nome e misure di dieci fotografie
 * diverse non si possono riassumere senza inventare, e una riga che dicesse 'varie' non
 * direbbe niente. Con una sola selezionata invece la domanda ha una risposta esatta, ed è
 * la stessa che dà la riga dei dettagli del visualizzatore.
 */
data class Facts(
    val count: Int,
    val bytes: Long,
    /** Solo quando la selezione è di uno. Vedi la nota sulla classe. */
    val name: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

/**
 * I dati di una selezione, letti dal disco.
 *
 * ⚠️⚠️ **LE MISURE SI LEGGONO DALLE INTESTAZIONI E NON DECODIFICANDO**
 * (`inJustDecodeBounds`), ed è la differenza fra leggere qualche decina di byte e
 * ricostruire trenta megapixel per scrivere '8160 x 6120'. Vale anche per i formati che
 * il MediaStore non descrive, quindi non serve un secondo ramo per loro.
 * ⚠️ **Il peso invece NON si prende dal decodificatore**: quello dice quanto misura
 * l'immagine, non quanto pesa il file. Per un `file://` lo dice il filesystem, per tutto
 * il resto la colonna che ogni provider serve.
 */
suspend fun factsOf(context: Context, uris: List<Uri>): Facts = withContext(Dispatchers.IO) {
    var bytes = 0L
    for (uri in uris) bytes += sizeOf(context, uri)
    if (uris.size != 1) return@withContext Facts(uris.size, bytes)

    val uri = uris.first()
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    }
    Facts(
        count = 1,
        bytes = bytes,
        name = nameOf(context, uri),
        width = bounds.outWidth.takeIf { it > 0 },
        height = bounds.outHeight.takeIf { it > 0 }
    )
}

private fun sizeOf(context: Context, uri: Uri): Long {
    if (uri.scheme?.lowercase() == "file") {
        return uri.path?.let { runCatching { File(it).length() }.getOrNull() } ?: 0L
    }
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { c ->
                val at = c.getColumnIndex(OpenableColumns.SIZE)
                if (at >= 0 && c.moveToFirst() && !c.isNull(at)) c.getLong(at) else null
            }
    }.getOrNull() ?: 0L
}

private fun nameOf(context: Context, uri: Uri): String? {
    if (uri.scheme?.lowercase() == "file") return uri.lastPathSegment
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c ->
                val at = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (at >= 0 && c.moveToFirst()) c.getString(at) else null
            }
    }.getOrNull() ?: uri.lastPathSegment
}

package io.github.roccobot.aiv

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Il lato lungo di un file di immagine, letto senza decodificarlo.
 *
 * Serve alla miniatura che si vede mentre l'immagine si apre: va disegnata alla misura che avrà
 * l'immagine, o al suo arrivo cambia dimensione sotto gli occhi. Una miniatura da 512 px non dice
 * se viene da un'immagine di 600 px o di seimila.
 *
 * ⚠️ **Si tiene il lato lungo e non la coppia, per l'orientamento**: `inJustDecodeBounds` legge le
 * dimensioni del flusso codificato, che per una foto verticale sono orizzontali (la rotazione è
 * nell'EXIF). Il lato lungo non cambia con un quarto di giro, e le proporzioni le dà la miniatura.
 *
 * La lettura costa un'intestazione, come in `factsOf`, ma resta I/O: si chiede prima e si ritrova
 * qui mentre si compone.
 */
object Pixels {

    /**
     * Abbastanza per una sfogliata: la misura si ricava in un istante, e tenerne di più terrebbe
     * indirizzi di cartelle già chiuse. In ordine d'uso (`accessOrder`), così chi sfoglia avanti e
     * indietro ritrova le stesse.
     */
    private const val KEEP = 64

    private val seen = object : LinkedHashMap<String, Int>(KEEP, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Int>): Boolean =
            size > KEEP
    }

    /**
     * Il lato lungo se è già stato letto, altrimenti `null`.
     *
     * Sincrona apposta, come `Thumbs.cached`: si legge mentre si compone, e un dato che arriva un
     * fotogramma dopo arriva col disegno sbagliato già sullo schermo.
     */
    @Synchronized
    fun known(uri: Uri): Int? = seen[uri.toString()]

    @Synchronized
    private fun note(uri: Uri, side: Int) {
        seen[uri.toString()] = side
    }

    /**
     * Dimentica il lato di [uri], il cui file è stato riscritto. La chiama `Thumbs.forget`: dopo un
     * ritaglio salvato sopra l'originale la finestra del ridimensionamento leggerebbe la misura di
     * prima.
     */
    @Synchronized
    fun forget(uri: Uri) {
        seen.remove(uri.toString())
    }

    /**
     * Il lato lungo del file dietro [uri], letto dall'intestazione e poi ricordato. `null` per
     * tutto quello che non si apre (un indirizzo di rete, un file sparito, un formato
     * sconosciuto): chi chiama lo tratta come un caso normale.
     */
    suspend fun longSideOf(context: Context, uri: Uri): Int? {
        known(uri)?.let { return it }
        val side = withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            runCatching {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
            }
            maxOf(bounds.outWidth, bounds.outHeight).takeIf { it > 0 }
        } ?: return null
        note(uri, side)
        return side
    }
}

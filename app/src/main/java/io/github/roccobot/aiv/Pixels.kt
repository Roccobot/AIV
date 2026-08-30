package io.github.roccobot.aiv

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Quanto è grande DAVVERO un file di immagine, senza decodificarlo.
 *
 * ⚠️⚠️ **SERVE ALLA MINIATURA, non alla fotografia**, ed è la sola ragione per cui esiste.
 * Il sostituto che si vede mentre la fotografia si apre deve essere disegnato **alla misura
 * che avrà la fotografia**, o al suo arrivo l'immagine cambia di dimensione sotto gli occhi.
 * Quella misura dipende da quanto è grande il file, che è precisamente l'unica cosa che una
 * miniatura non può dire: una miniatura da 512 px può venire da un'immagine di 600 px o da
 * una di seimila, e sono due misure finali diverse.
 *
 * ⚠️⚠️ **SI TIENE IL LATO LUNGO E NON LA COPPIA, e non è pigrizia: è l'ORIENTAMENTO.**
 * `inJustDecodeBounds` legge le dimensioni del flusso **codificato**, che per una foto
 * scattata in verticale sono quelle orizzontali: la rotazione sta nell'EXIF, e chi decodifica
 * per davvero la applica. Prendere quella coppia per buona darebbe proporzioni girate su
 * tutte le foto verticali. Il **lato lungo** invece è lo stesso prima e dopo una rotazione
 * di un quarto di giro, quindi non ha questo problema; le proporzioni le sa già la miniatura,
 * che dal decodificatore è passata. Due dati veri, presi ognuno da chi lo conosce.
 *
 * ⚠️ **La lettura costa un'intestazione, non un'immagine** (qualche decina di byte contro
 * trenta megapixel), ed è la stessa via di `factsOf`. Resta comunque I/O, quindi non si fa
 * mentre si compone: si chiede prima e si ritrova qui.
 */
object Pixels {

    /**
     * ⚠️ Tante quante ne serve a coprire una sfogliata, non un archivio: la misura si
     * ricava in un istante, quindi tenerne mille non farebbe risparmiare niente di
     * apprezzabile e terrebbe in vita indirizzi di cartelle già chiuse.
     * ⚠️ **In ordine d'uso** (`accessOrder`), non d'inserimento: chi sfoglia avanti e
     * indietro sulle stesse foto le vuole ancora dentro.
     */
    private const val KEEP = 64

    private val seen = object : LinkedHashMap<String, Int>(KEEP, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Int>): Boolean =
            size > KEEP
    }

    /**
     * Il lato lungo se lo si è già letto, e `null` se no.
     *
     * ⚠️ **Sincrona apposta**: si legge **mentre si compone**, che è l'unico momento in cui
     * serve. È la stessa forma di `Thumbs.cached` e per la stessa ragione: un dato che
     * arriva un fotogramma dopo arriva quando il disegno sbagliato è già sullo schermo.
     */
    @Synchronized
    fun known(uri: Uri): Int? = seen[uri.toString()]

    @Synchronized
    private fun note(uri: Uri, side: Int) {
        seen[uri.toString()] = side
    }

    /**
     * Il lato lungo del file dietro [uri], letto dall'intestazione e poi ricordato.
     *
     * ⚠️ Torna `null` per tutto quello che non si riesce ad aprire, e chi chiama deve
     * saperci fare: un indirizzo di rete, un file sparito, un formato che il decodificatore
     * non riconosce. Non sapere la misura non è un guasto, è un caso.
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

package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.annotation.StringRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Salvare l'immagine che si sta guardando in un altro formato.
 *
 * ⚠️⚠️ **QUELLO CHE ANDROID SA SCRIVERE È UN ELENCO CHIUSO DI QUATTRO VOCI, ed è misurato**:
 * `Bitmap.CompressFormat` nell'API 37 offre `JPEG`, `PNG`, `WEBP` (deprecata dalla 30),
 * `WEBP_LOSSLESS` e `WEBP_LOSSY`, e nient'altro. Niente GIF, niente AVIF, niente TIFF, niente
 * BMP **in scrittura**. Chi cerca uno di quelli deve passare da un servizio di fuori, ed è la
 * ragione per cui il dialogo ha due metà invece di una.
 * ⚠️ **`WEBP` senza suffisso non entra fra le scelte**: fa la stessa cosa di una delle altre
 * due a seconda della qualità, e offrire tre voci per due comportamenti vuol dire chiedere
 * all'utente di indovinare quale sta scegliendo.
 *
 * ⚠️⚠️ **SI RIDECODIFICA DAL FILE, e non si riusa il bitmap sullo schermo**: quello che il
 * visualizzatore ha in mano può essere **ridotto** (vedi `LoadedImage.sampled`), perché per
 * mostrarlo non serve di più. Convertire quella copia darebbe un file più piccolo del vero
 * senza che nessuno l'abbia chiesto, ed è il difetto peggiore possibile in una funzione che
 * l'utente usa proprio per **conservare** un'immagine.
 */
object Convert {

    /**
     * I formati che Android sa scrivere, con quello che serve per scriverli.
     *
     * ⚠️ **`quality` è l'unico parametro che l'API accetta**, e sul senza perdita non conta
     * niente: la voce lo dichiara con [lossy] invece di lasciare in scena un cursore che non
     * fa nulla.
     */
    enum class Target(
        val format: Bitmap.CompressFormat,
        val extension: String,
        val mime: String,
        val lossy: Boolean,
        @param:StringRes val label: Int
    ) {
        JPEG(Bitmap.CompressFormat.JPEG, "jpg", "image/jpeg", true, R.string.convert_jpeg),
        PNG(Bitmap.CompressFormat.PNG, "png", "image/png", false, R.string.convert_png),
        WEBP_LOSSY(
            Bitmap.CompressFormat.WEBP_LOSSY, "webp", "image/webp", true, R.string.convert_webp
        ),
        WEBP_LOSSLESS(
            Bitmap.CompressFormat.WEBP_LOSSLESS, "webp", "image/webp", false,
            R.string.convert_webp_lossless
        );

        /**
         * ⚠️⚠️ **IL JPEG NON HA LA TRASPARENZA, e va detto PRIMA e non dopo**: convertendo
         * una PNG o una WebP con le parti trasparenti, quelle diventano **nere**, in silenzio.
         * Chi se ne accorge lo fa guardando il file salvato, cioè quando è tardi.
         */
        val keepsAlpha: Boolean get() = this != JPEG
    }

    /**
     * Di quanto si rimpicciolisce, in centesimi.
     *
     * ⚠️ **Percentuali e non lati massimi**: una percentuale si legge senza sapere quanto
     * misura l'originale, e il dialogo mostra comunque i pixel che ne escono. Un 'lato lungo
     * 1920' costringerebbe a fare il conto a mente per capire se sta riducendo o ingrandendo.
     * ⚠️ **Non si ingrandisce**: inventare pixel che nel file non ci sono non è una
     * conversione, e nessuna delle voci va sopra il 100.
     */
    enum class Size(val percent: Int) {
        FULL(100), THREE_QUARTERS(75), HALF(50), QUARTER(25);

        fun applyTo(side: Int): Int = (side * percent / 100).coerceAtLeast(1)
    }

    /**
     * Il nome proposto per il file convertito.
     *
     * ⚠️ **Cambia l'estensione e lascia il resto**: chi converte vuole ritrovare lo stesso
     * nome, e un suffisso aggiunto ('-convertito') allontanerebbe le due versioni
     * nell'ordinamento della cartella, che è dove le si confronta.
     */
    fun nameFor(original: String?, target: Target): String {
        val base = (original ?: "immagine").substringBeforeLast('.', original ?: "immagine")
        return "$base.${target.extension}"
    }

    /**
     * Scrive l'immagine di [source] in [destination], nel formato chiesto.
     *
     * @param fallback il bitmap già in mano al visualizzatore, usato solo se il file non si
     *   riesce a rileggere per intero.
     *
     * ⚠️ **`NonCancellable` come ogni altra scrittura su file**: a metà, una cancellazione
     * lascerebbe un file troncato che sembra un'immagine e non lo è.
     * ⚠️ **La scala si applica DOPO la decodifica e non con un `inSampleSize`**: quello
     * scende solo per potenze di due, quindi il 75% e il 25% non li saprebbe fare, e per il
     * 50% darebbe un risultato più duro di una riduzione con filtro.
     */
    suspend fun write(
        context: Context,
        source: Uri?,
        fallback: Bitmap?,
        target: Target,
        quality: Int,
        size: Size,
        destination: Uri
    ): Boolean = withContext(Dispatchers.IO + NonCancellable) {
        val whole = source?.let { full(context, it) } ?: fallback ?: return@withContext false
        val scaled = if (size == Size.FULL) whole else runCatching {
            Bitmap.createScaledBitmap(
                whole, size.applyTo(whole.width), size.applyTo(whole.height), true
            )
        }.getOrDefault(whole)
        val done = runCatching {
            context.contentResolver.openOutputStream(destination)?.use { out ->
                scaled.compress(target.format, quality.coerceIn(1, 100), out)
            } ?: false
        }.getOrDefault(false)
        if (scaled !== whole) scaled.recycle()
        if (whole !== fallback) whole.recycle()
        done
    }

    /**
     * L'immagine alla sua risoluzione vera, e `null` se non ci si riesce.
     *
     * ⚠️⚠️ **SOFTWARE E MAI HARDWARE**: un bitmap con configurazione `HARDWARE` non si può
     * comprimere né ridimensionare, quindi il valore di serie di `ImageDecoder` renderebbe
     * questa funzione inutile proprio quando riesce.
     * ⚠️ **Un fallimento qui non è un errore da mostrare**: su un'immagine enorme la memoria
     * può non bastare, e allora si converte quello che il visualizzatore ha già, che è
     * ridotto ma esiste. Il dialogo lo dice quando la sorgente è ridotta.
     */
    private fun full(context: Context, uri: Uri): Bitmap? = runCatching {
        ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(context.contentResolver, uri)
        ) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
        }
    }.getOrNull()

    /**
     * I servizi di fuori, per tutto quello che Android non sa scrivere.
     *
     * ⚠️⚠️ **SI APRE IL SITO E BASTA: non gli si può PASSARE il file, e prometterlo sarebbe
     * una bugia**. Un servizio web riceve un'immagine solo con un caricamento fatto dalla
     * pagina, e Android non ha nessun modo di consegnargliela da fuori: il parametro `url` che
     * alcuni accettano vuole un indirizzo pubblico, che un file sul telefono non ha. Quindi il
     * dialogo apre il sito, e l'immagine la si sceglie di là.
     * ⚠️ **Tre e non venti**: un elenco lungo sposta sull'utente la scelta che questa voce
     * dovrebbe risparmiargli. Questi tre coprono i casi veri, e la ragione di ognuno è scritta
     * accanto.
     */
    enum class Service(val url: String, @param:StringRes val label: Int, @param:StringRes val why: Int) {
        /** Il servizio nominato dall'utente: converte da e verso quasi tutto, GIF comprese. */
        EZGIF("https://ezgif.com/", R.string.convert_ezgif, R.string.convert_ezgif_why),

        /** Di Google, gira **nel browser**: l'immagine non viene caricata da nessuna parte. */
        SQUOOSH("https://squoosh.app/", R.string.convert_squoosh, R.string.convert_squoosh_why),

        /** Quello con l'elenco di formati più lungo, AVIF e TIFF compresi. */
        CLOUDCONVERT(
            "https://cloudconvert.com/image-converter", R.string.convert_cloudconvert,
            R.string.convert_cloudconvert_why
        )
    }
}

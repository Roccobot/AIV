package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.annotation.StringRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Salvare l'immagine che si sta guardando in un altro formato.
 *
 * ⚠️ **Android scrive solo quello che `Bitmap.CompressFormat` elenca** (API 37): `JPEG`, `PNG`,
 * `WEBP` (deprecata dalla 30), `WEBP_LOSSLESS` e `WEBP_LOSSY`. Per GIF, AVIF, TIFF e BMP si passa
 * da un servizio esterno, ed è la ragione delle due metà del dialogo.
 * `WEBP` senza suffisso non è fra le scelte: fa quello che fa una delle altre due, secondo la
 * qualità.
 *
 * ⚠️ **Si ridecodifica dal file**: il bitmap del visualizzatore può essere ridotto
 * (`LoadedImage.sampled`), e convertirlo darebbe un file più piccolo del vero in una funzione
 * che serve a conservare un'immagine.
 */
object Convert {

    /**
     * I formati che Android sa scrivere, con quello che serve per scriverli.
     *
     * `quality` è il solo parametro che l'API accetta, e sul senza perdita non conta: la voce lo
     * dichiara con [lossy], e il cursore non compare.
     *
     * ⚠️ **Il formato non è un campo della voce, è [format]**: un campo si legge quando l'elenco
     * si costruisce, e sotto Android 11 `WEBP_LOSSY` e `WEBP_LOSSLESS` non esistono, quindi
     * leggerli farebbe fallire l'elenco intero, JPEG compreso, e l'app si chiuderebbe aprendo la
     * finestra. [format] guarda la versione prima di nominarli.
     */
    enum class Target(
        val extension: String,
        val mime: String,
        val lossy: Boolean,
        @param:StringRes val label: Int
    ) {
        JPEG("jpg", "image/jpeg", true, R.string.convert_jpeg),
        PNG("png", "image/png", false, R.string.convert_png),
        WEBP_LOSSY("webp", "image/webp", true, R.string.convert_webp),
        WEBP_LOSSLESS("webp", "image/webp", false, R.string.convert_webp_lossless);

        /**
         * Il formato con cui Android scrive questa voce.
         *
         * Sotto Android 11 le due WebP sono la stessa costante, `WEBP`, che sceglie con la
         * qualità: sotto 100 comprime con perdita, a 100 senza ([qualityFor]). Come in
         * `ImageEdit`, `FolderCover` e `AvifCache`.
         */
        val format: Bitmap.CompressFormat
            get() = when (this) {
                JPEG -> Bitmap.CompressFormat.JPEG
                PNG -> Bitmap.CompressFormat.PNG
                WEBP_LOSSY -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
                }
                WEBP_LOSSLESS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
                }
            }

        /**
         * La qualità da passare a `compress`, da quella che la finestra ha in mano.
         *
         * ⚠️ **Il senza perdita vuole 100**: sotto Android 11, con `WEBP`, è la qualità a dire se
         * si perde, e sul senza perdita il valore della finestra non vale (il cursore non c'è).
         */
        fun qualityFor(quality: Int): Int = if (this == WEBP_LOSSLESS) 100 else quality.coerceIn(1, 100)

        /**
         * Se il telefono sa scrivere questa voce.
         *
         * ⚠️ **La WebP senza perdita non c'è su Android 9**: là `WEBP` a qualità 100 comprime ancora
         * con perdita (documentazione di `CompressFormat.WEBP`: senza perdita dalla versione Q), e
         * il file perderebbe quello che dice di conservare. Presidiato da `ConvertiTest`, su
         * Android 9 e 10.
         */
        val offered: Boolean
            get() = this != WEBP_LOSSLESS || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        /**
         * Il JPEG non ha la trasparenza, e il dialogo lo dice prima della conversione: le parti
         * trasparenti diventano bianche ([flatten]).
         */
        val keepsAlpha: Boolean get() = this != JPEG
    }

    /**
     * Di quanto si rimpicciolisce, in centesimi.
     *
     * Percentuali: si leggono senza sapere quanto misura l'originale, e il dialogo mostra
     * comunque i pixel risultanti. Nessuna voce supera 100: una conversione non inventa pixel.
     */
    enum class Size(val percent: Int) {
        FULL(100), THREE_QUARTERS(75), HALF(50), QUARTER(25);

        fun applyTo(side: Int): Int = (side * percent / 100).coerceAtLeast(1)
    }

    /**
     * Il nome proposto per il file convertito.
     *
     * Cambia solo l'estensione: le due versioni restano vicine nell'ordinamento della cartella,
     * dove le si confronta.
     */
    fun nameFor(original: String?, target: Target): String {
        val base = (original ?: "immagine").substringBeforeLast('.', original ?: "immagine")
        return "$base.${target.extension}"
    }

    /**
     * Come [nameFor], ma per un fotogramma solo di un'immagine animata.
     *
     * Il numero ha quattro cifre con gli zeri davanti, così i fotogrammi di una GIF si ordinano
     * da soli nella cartella (`foto-0009` prima di `foto-0010`).
     */
    fun frameName(original: String?, frame: Int, target: Target): String {
        val base = (original ?: "immagine").substringBeforeLast('.', original ?: "immagine")
        return "%s-%04d.%s".format(base, frame, target.extension)
    }

    /**
     * Scrive l'immagine di [source] in [destination], nel formato chiesto.
     *
     * @param fallback il bitmap già in mano al visualizzatore, usato solo se il file non si
     *   riesce a rileggere per intero.
     *
     * `NonCancellable`, come ogni scrittura su file: una cancellazione a metà lascerebbe un file
     * troncato che sembra un'immagine.
     * La scala si applica dopo la decodifica: `inSampleSize` scende solo per potenze di due, e
     * al 50% darebbe un risultato più duro di una riduzione con filtro.
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
        var scaled: Bitmap? = null
        var flat: Bitmap? = null
        // ⚠️ **Senza memoria per ridurre o appiattire, la conversione fallisce** (rilievo B15):
        // un file a misura piena, o con la trasparenza nera, uscirebbe con l'avviso 'salvato'.
        try {
            scaled = if (size == Size.FULL) whole else Bitmap.createScaledBitmap(
                whole, size.applyTo(whole.width), size.applyTo(whole.height), true
            )
            flat = if (target.keepsAlpha) scaled else flatten(scaled)
            runCatching {
                context.contentResolver.openOutputStream(destination)?.use { out ->
                    flat.compress(target.format, target.qualityFor(quality), out)
                } ?: false
            }.getOrDefault(false)
        } catch (_: OutOfMemoryError) {
            false
        } finally {
            if (flat !== scaled) flat?.recycle()
            if (scaled !== whole) scaled?.recycle()
            if (whole !== fallback) whole.recycle()
        }
    }

    /**
     * L'immagine su fondo **bianco**, per i formati che la trasparenza non la sanno tenere.
     *
     * ⚠️ **Esiste perché `compress` in JPEG scarta il canale alfa e basta**: un pixel trasparente
     * tiene i suoi valori di colore, che in un PNG sono quasi sempre zero, cioè nero. Disegnando
     * prima su una tela piena, il colore lo decide l'app.
     * Bianco (scelta dell'utente, 1.16): è il fondo su cui l'immagine sarà quasi sempre guardata,
     * e di un disegno con lo sfondo tolto è la resa più vicina all'originale.
     * Senza canale alfa si restituisce la sorgente, senza una tela in più.
     * La usa anche il salvataggio in JPEG degli editor (`ImageEdit`).
     * ⚠️ **Un errore di memoria passa al chiamante** (rilievo B15): restituire l'immagine con la
     * trasparenza farebbe scrivere al JPEG proprio il nero che questa funzione toglie. La
     * chiamano solo le scritture, che lo raccolgono.
     */
    internal fun flatten(source: Bitmap): Bitmap {
        if (!source.hasAlpha()) return source
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(out).apply {
            drawColor(android.graphics.Color.WHITE)
            drawBitmap(source, 0f, 0f, null)
        }
        return out
    }

    /**
     * L'immagine alla sua risoluzione vera, e `null` se non ci si riesce.
     *
     * Passa da `ImageSource.pixels`, che ha anche i ripieghi per AVIF e SVG: lì `ImageDecoder`
     * fallisce, e senza ripiego si convertirebbe il bitmap ridotto del visualizzatore.
     * Un fallimento qui non è un errore da mostrare: su un'immagine enorme la memoria può non
     * bastare, e allora si converte la copia del visualizzatore, ridotta. Il dialogo lo dice.
     */
    private fun full(context: Context, uri: Uri): Bitmap? =
        ImageSource.pixels(context, uri, 0)

    /**
     * I servizi di fuori, per tutto quello che Android non sa scrivere.
     *
     * ⚠️ **Si apre solo il sito, senza il file**: un servizio web riceve un'immagine solo da un
     * caricamento fatto nella pagina, e il parametro `url` che alcuni accettano vuole un
     * indirizzo pubblico, che un file sul telefono non ha. L'immagine si sceglie di là.
     * Tre servizi, ognuno con la sua ragione: un elenco lungo sposterebbe la scelta sull'utente.
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

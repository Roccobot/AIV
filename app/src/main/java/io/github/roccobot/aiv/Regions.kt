package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Un pezzo di fotografia letto a piena risoluzione, per quando lo zoom lo chiede.
 *
 * ⚠️⚠️ **IL PROBLEMA CHE RISOLVE: LA QUALITÀ CHE CALAVA IN SILENZIO SULLE FOTO PIÙ
 * GRANDI.** `ImageSource` apre l'immagine **intera** in memoria, e quando la memoria non
 * basta la campiona: una foto da 30 megapixel diventa 7 o 8, e al 100% si guarda un
 * ingrandimento invece dei pixel veri. Non c'era nessun avviso oltre alla parola
 * `(sampled)` nella riga dei dettagli. Qui si legge dal file **solo la parte che si sta
 * guardando**, e a quella non serve nessun campionamento.
 *
 * ⚠️ **Serve SOLO alle immagini campionate**, e chi apre questa classe lo verifica prima:
 * se la fotografia è già entrata intera, il tassello non ha niente da aggiungere e
 * accenderlo sarebbe lavoro puro. È il paletto che tiene tutta questa strada fuori dai
 * piedi per la stragrande maggioranza delle immagini.
 *
 * ⚠️⚠️ **E QUI STA LA TRAPPOLA VERA, LETTA SUI SORGENTI AOSP E NON SUPPOSTA: LE DUE
 * DECODIFICHE NON PARLANO LO STESSO SISTEMA DI COORDINATE.**
 * - `ImageDecoder` **applica l'orientamento EXIF**: in `hwui/ImageDecoder.cpp` la sua
 *   `width()` restituisce la misura **scambiata** quando l'origine scambia i lati
 *   (`SkEncodedOriginSwapsWidthHeight`), e il disegno passa per
 *   `SkEncodedOriginToMatrix`. Quindi il bitmap del visualizzatore, e le misure che
 *   porta in `LoadedImage`, sono **già girati**.
 * - `BitmapRegionDecoder` **no**: nel suo JNI non compare nessuna origine, e le sue
 *   misure sono quelle grezze del file.
 *
 * Su una foto scattata in verticale, che il telefono scrive orizzontale con un tag di
 * rotazione, prendere il rettangolo visibile e passarlo così com'è al secondo
 * decodificatore darebbe un pezzo dell'immagine **sbagliato e messo di traverso**. Ecco
 * perché qui dentro c'è una conversione, e perché ha un controllo che la smentisce.
 *
 * ⚠️⚠️ **IL CONTROLLO DI COERENZA NON È PRUDENZA GENERICA: è quello che rende questa
 * classe sicura senza un telefono in mano.** L'orientamento si legge con
 * `ExifInterface`, ma non ci si fida: si verifica che concordi con le **misure**. Se il
 * tag dice 'ruotata di 90' i due lati devono risultare scambiati, e se non lo sono
 * qualcosa non torna (un tag che mente, un formato letto male, una versione di Android
 * che un domani applicasse l'orientamento anche qui). In quel caso [open] restituisce
 * **null** e il visualizzatore resta esattamente com'era prima di questa classe.
 */
class RegionSource private constructor(
    private val decoder: BitmapRegionDecoder,
    private val orientation: Int,
    /** Le misure **come si vedono**, cioè quelle di `LoadedImage`. */
    val width: Int,
    val height: Int
) {

    /**
     * ⚠️ `BitmapRegionDecoder` non è utilizzabile da due parti insieme, e la protezione
     * serve davvero: chi chiede i tasselli annulla la richiesta precedente quando il
     * dito si muove, ma una `decodeRegion` già partita è una chiamata **bloccante** e
     * non si interrompe. Senza questo, due decodifiche si sovrapporrebbero sullo stesso
     * oggetto nativo.
     */
    private val lock = Mutex()

    /**
     * Il pezzo di [area] (in coordinate **viste**) letto con quel campionamento, già
     * girato come lo si guarda.
     */
    suspend fun tile(area: Rect, sample: Int): Bitmap? = lock.withLock {
        withContext(Dispatchers.IO) {
            val raw = toRaw(area) ?: return@withContext null
            val options = BitmapFactory.Options().apply { inSampleSize = maxOf(1, sample) }
            val decoded = runCatching { decoder.decodeRegion(raw, options) }.getOrNull()
            decoded?.let(::turned)
        }
    }

    fun close() {
        runCatching { decoder.recycle() }
    }

    /**
     * Da coordinate viste a coordinate grezze.
     *
     * ⚠️ Le formule sono l'inversa di quello che l'orientamento EXIF **dice di fare** al
     * file per guardarlo: per il 6 il file va girato di 90 in senso orario, quindi il
     * pixel grezzo `(rx, ry)` si vede in `(Hr - 1 - ry, rx)`, e questa è l'inversa di
     * quella. Chi le ritocca le riderivi da lì invece che a occhio.
     */
    private fun toRaw(area: Rect): Rect? {
        val rawWidth = decoder.width
        val rawHeight = decoder.height
        val raw = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_180 -> Rect(
                rawWidth - area.right, rawHeight - area.bottom,
                rawWidth - area.left, rawHeight - area.top
            )
            ExifInterface.ORIENTATION_ROTATE_90 -> Rect(
                area.top, rawHeight - area.right,
                area.bottom, rawHeight - area.left
            )
            ExifInterface.ORIENTATION_ROTATE_270 -> Rect(
                rawWidth - area.bottom, area.left,
                rawWidth - area.top, area.right
            )
            else -> Rect(area)
        }
        // ⚠️ L'ultima rete: un rettangolo fuori dai bordi farebbe sollevare
        // `decodeRegion`, e qui invece non si disegna nessun tassello. Meglio la
        // fotografia com'era che un errore.
        if (raw.left < 0 || raw.top < 0 || raw.right > rawWidth || raw.bottom > rawHeight) return null
        if (raw.width() <= 0 || raw.height() <= 0) return null
        return raw
    }

    /** Il pezzo grezzo girato come si guarda. */
    private fun turned(bitmap: Bitmap): Bitmap {
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        val turned = runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrNull() ?: return bitmap
        // Il pezzo grezzo non è mai uscito di qui, quindi buttarlo è sicuro; e
        // `createBitmap` può restituire lo stesso oggetto, che invece va tenuto.
        if (turned !== bitmap) bitmap.recycle()
        return turned
    }

    companion object {

        /**
         * Apre la lettura a pezzi di [uri], o **null** se non si può o non conviene.
         *
         * ⚠️ I casi in cui torna null sono tutti voluti, e in ognuno il visualizzatore
         * resta quello di prima: l'immagine non era campionata (non c'è niente da
         * guadagnare), il ribaltamento EXIF non è una semplice rotazione (raro, e le
         * formule non lo coprono), le misure non concordano col tag, oppure i byte non
         * sono raggiungibili. ⚠️ Un'immagine remota si legge dalla **cache**: se non c'è
         * non la si riscarica, perché rifare un download per affinare uno zoom è
         * esattamente il contrario di quello che questa strada serve a fare.
         */
        suspend fun open(context: Context, uri: Uri?, image: LoadedImage): RegionSource? {
            if (uri == null || !image.sampled) return null
            return withContext(Dispatchers.IO) {
                val orientation = runCatching {
                    stream(context, uri)?.use { ExifInterface(it).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    ) }
                }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
                val turns = when (orientation) {
                    ExifInterface.ORIENTATION_NORMAL,
                    ExifInterface.ORIENTATION_UNDEFINED,
                    ExifInterface.ORIENTATION_ROTATE_180 -> false
                    ExifInterface.ORIENTATION_ROTATE_90,
                    ExifInterface.ORIENTATION_ROTATE_270 -> true
                    // I ribaltamenti (2, 4, 5, 7) non sono rotazioni e non si trattano:
                    // sono rarissimi, e indovinarli a memoria è il modo di sbagliarli.
                    else -> return@withContext null
                }
                val decoder = stream(context, uri)?.use { runCatching { create(it) }.getOrNull() }
                    ?: return@withContext null
                // ⚠️ IL CONTROLLO CHE RENDE SICURO TUTTO IL RESTO: le misure grezze
                // devono dire la stessa cosa del tag. Se non la dicono, si rinuncia.
                val agrees = if (turns) {
                    decoder.width == image.pixelHeight && decoder.height == image.pixelWidth
                } else {
                    decoder.width == image.pixelWidth && decoder.height == image.pixelHeight
                }
                if (!agrees) {
                    decoder.recycle()
                    return@withContext null
                }
                RegionSource(decoder, orientation, image.pixelWidth, image.pixelHeight)
            }
        }

        /**
         * ⚠️ Il decodificatore **copia** i byte codificati e non tiene il flusso, quindi
         * lo si può chiudere subito: è la ragione per cui questa funzione può stare
         * dentro un `use`. Il costo è la dimensione del file in memoria nativa, che è
         * una frazione di quello che costerebbe tenerne la versione decodificata.
         */
        @Suppress("DEPRECATION")
        private fun create(input: InputStream): BitmapRegionDecoder? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                BitmapRegionDecoder.newInstance(input)
            } else {
                BitmapRegionDecoder.newInstance(input, false)
            }

        /**
         * I byte da cui rileggere, e sono due strade sole.
         *
         * ⚠️ Per un indirizzo remoto **non si va in rete**: si guarda la cache, che dopo
         * la 0.38 contiene quello che si sta guardando. Se non c'è, niente tasselli.
         */
        private fun stream(context: Context, uri: Uri): InputStream? =
            when (uri.scheme?.lowercase()) {
                "http", "https" -> RemoteCache.read(context, uri)?.let(::ByteArrayInputStream)
                else -> runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
            }
    }
}

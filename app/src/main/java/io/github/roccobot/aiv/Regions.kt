package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Un pezzo di immagine letto a piena risoluzione, quando lo zoom lo chiede.
 *
 * `ImageSource` apre l'immagine intera in memoria, e quando la memoria non basta la campiona: una
 * foto da 30 megapixel diventa 7 o 8, e al 100% si guarda un ingrandimento. Qui si legge dal file
 * solo la parte che si sta guardando. Serve solo alle immagini campionate, e chi apre la classe lo
 * verifica prima.
 *
 * ⚠️ **Le due decodifiche non usano lo stesso sistema di coordinate** (letto nei sorgenti AOSP):
 * - `ImageDecoder` applica l'orientamento EXIF (`hwui/ImageDecoder.cpp`: `width()` scambia i lati
 *   con `SkEncodedOriginSwapsWidthHeight`), quindi il bitmap del visualizzatore e le misure di
 *   `LoadedImage` sono già girati;
 * - `BitmapRegionDecoder` no: le sue misure sono quelle grezze del file. Su una foto verticale il
 * rettangolo visibile passato così com'è darebbe un pezzo sbagliato e di traverso, e per questo c'è
 * una conversione.
 *
 * ⚠️ **Il controllo di coerenza rende la classe sicura senza un telefono in mano**: l'orientamento
 * letto con `ExifInterface` deve concordare con le misure (se il tag dice 'ruotata di 90' i lati
 * devono risultare scambiati). Se non concorda, [open] restituisce `null` e il visualizzatore resta
 * com'era.
 */
class RegionSource private constructor(
    private val decoder: BitmapRegionDecoder,
    private val orientation: Int,
    /** Le misure come si vedono, cioè quelle di `LoadedImage`. */
    val width: Int,
    val height: Int
) {

    /**
     * `BitmapRegionDecoder` non regge due chiamate insieme: chi chiede i tasselli annulla la
     * richiesta precedente quando il dito si muove, ma una `decodeRegion` già partita è bloccante e
     * non si interrompe.
     */
    private val lock = Mutex()

    /** Il pezzo di [area] (in coordinate viste) letto con quel campionamento, già girato. */
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
     * Da coordinate viste a coordinate grezze: l'inversa di quello che l'orientamento EXIF dice di
     * fare al file per guardarlo. Per il 6 il file si gira di 90 in senso orario, quindi il pixel
     * grezzo `(rx, ry)` si vede in `(Hr - 1 - ry, rx)`. Chi ritocca le formule le riderivi da lì.
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
        // Un rettangolo fuori dai bordi manderebbe in errore `decodeRegion`: niente tassello, e
        // l'immagine resta com'era.
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
        // Il pezzo grezzo non è mai uscito di qui, quindi si può riciclare; ma `createBitmap` può
        // restituire lo stesso oggetto, che va tenuto.
        if (turned !== bitmap) bitmap.recycle()
        return turned
    }

    companion object {

        /**
         * Apre la lettura a pezzi di [uri] per il visualizzatore, o `null` se non si può o non
         * conviene: l'immagine non era campionata, il tag è un ribaltamento e non una rotazione, le
         * misure non concordano col tag, o i byte non sono raggiungibili. In ogni caso il
         * visualizzatore resta com'era. Un'immagine remota si legge dalla cache, senza
         * riscaricarla.
         */
        suspend fun open(context: Context, uri: Uri?, image: LoadedImage): RegionSource? {
            if (uri == null || !image.sampled) return null
            // Il controllo di coerenza: le misure grezze girate devono essere quelle che
            // `ImageDecoder` ha misurato con l'EXIF già applicato.
            return make(context, uri) { seenWidth, seenHeight ->
                seenWidth == image.pixelWidth && seenHeight == image.pixelHeight
            }
        }

        /**
         * Apre la lettura a pezzi di [uri] per l'editor completo, che lavora su un'anteprima
         * ridotta, o `null` se non si può o non conviene.
         *
         * Qui il bitmap in mano è l'anteprima da 1600 pixel di lato, quindi anche un'immagine
         * entrata intera nel visualizzatore ha dettaglio da recuperare: la domanda è se il file ha
         * più pixel dell'anteprima. Il controllo di coerenza diventa un rapporto, perché
         * l'anteprima è il file diviso per una potenza di due: se il tag dice 'ruotata' e le misure
         * grezze non sono scambiate, le proporzioni non tornano. ⚠️ Su un'immagine quadrata il
         * controllo non distingue niente.
         */
        suspend fun open(context: Context, uri: Uri?, preview: Bitmap): RegionSource? {
            if (uri == null || preview.width <= 0 || preview.height <= 0) return null
            return make(context, uri) { seenWidth, seenHeight ->
                val gains = maxOf(seenWidth, seenHeight) > maxOf(preview.width, preview.height)
                val shape = seenWidth.toFloat() / seenHeight
                val same = preview.width.toFloat() / preview.height
                // Il campionamento arrotonda i lati al pixel, quindi i due rapporti non coincidono
                // mai alla cifra: un punto percentuale li tiene insieme e resta lontano dallo
                // scambio dei lati.
                gains && abs(shape - same) <= same * SHAPE_SLACK
            }
        }

        /**
         * Il lavoro comune alle due vie: legge il tag, apre il decodificatore e chiede a [fits] se
         * le misure viste sono quelle che il chiamante si aspetta. Un `false` è una rinuncia.
         */
        private suspend fun make(
            context: Context,
            uri: Uri,
            fits: (Int, Int) -> Boolean
        ): RegionSource? = withContext(Dispatchers.IO) {
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
                // I ribaltamenti (2, 4, 5, 7) non sono rotazioni e non si trattano: sono rarissimi.
                else -> return@withContext null
            }
            val decoder = stream(context, uri)?.use { runCatching { create(it) }.getOrNull() }
                ?: return@withContext null
            val seenWidth = if (turns) decoder.height else decoder.width
            val seenHeight = if (turns) decoder.width else decoder.height
            if (!fits(seenWidth, seenHeight)) {
                decoder.recycle()
                return@withContext null
            }
            RegionSource(decoder, orientation, seenWidth, seenHeight)
        }

        /**
         * Il decodificatore copia i byte codificati e non tiene il flusso, quindi lo si può
         * chiudere subito dentro un `use`. Il costo è il peso del file in memoria nativa.
         */
        @Suppress("DEPRECATION")
        private fun create(input: InputStream): BitmapRegionDecoder? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                BitmapRegionDecoder.newInstance(input)
            } else {
                BitmapRegionDecoder.newInstance(input, false)
            }

        /**
         * I byte da cui rileggere. Per un indirizzo remoto non si va in rete: si guarda la cache,
         * che contiene quello che si sta guardando; se non c'è, niente tasselli.
         */
        private fun stream(context: Context, uri: Uri): InputStream? =
            when (uri.scheme?.lowercase()) {
                "http", "https" -> RemoteCache.read(context, uri)?.let(::ByteArrayInputStream)
                else -> runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
            }
    }
}

/** Quanto i due rapporti possono distare prima che [RegionSource.open] rinunci. */
private const val SHAPE_SLACK = 0.01f

/**
 * L'esito di [sharpAsk]: che cosa leggere, o il motivo per cui non si legge niente. Il motivo
 * esiste perché 'non succede niente' è identico fra una funzione rotta, una rinuncia voluta e un
 * formato che non si sa rileggere: la riga dei dettagli del visualizzatore lo stampa.
 */
internal sealed interface Sharpening {
    /** Il rettangolo da leggere, in coordinate viste, e con quanto campionamento. */
    data class Read(val area: Rect, val sample: Int) : Sharpening
    data class None(val why: String) : Sharpening
}

/**
 * Quale pezzo leggere adesso, e con quanto campionamento.
 *
 * Il conto è uno per due chiamanti: il visualizzatore, che ingrandisce un'immagine campionata, e
 * l'editor completo, che lavora su un'anteprima da 1600 pixel. Le due geometrie entrano come
 * risultato: [drawn], il rettangolo di schermo in cui l'immagine intera è disegnata adesso.
 *
 * Si legge quando l'immagine di base è ingrandita, non prima: sotto quel confine un suo pixel copre
 * meno di un pixel di schermo. Il confine si ricava da [basePixels].
 *
 * Il campionamento si arrotonda per difetto (la potenza di due sotto): per eccesso si leggerebbe
 * meno dettaglio di quello che lo schermo mostra. Il prezzo è un pezzo fino al doppio per lato, e
 * per quello c'è il tetto ([TILE_SCREENFULS]); quando rientrare nel tetto porta il campionamento al
 * livello del bitmap di base, non si legge niente.
 *
 * @param fullWidth,fullHeight le misure viste del file, cioè quelle di [RegionSource].
 * @param basePixels quanti pixel ha sul lato largo il bitmap che si sta disegnando.
 * @param drawn dove finisce sullo schermo l'immagine intera, adesso.
 * @param viewWidth,viewHeight la finestra che si guarda.
 */
internal fun sharpAsk(
    fullWidth: Int,
    fullHeight: Int,
    basePixels: Int,
    drawn: RectF,
    viewWidth: Float,
    viewHeight: Float
): Sharpening {
    if (drawn.width() <= 0f || drawn.height() <= 0f || basePixels <= 0) {
        return Sharpening.None("no tile: base")
    }
    if (drawn.width() <= basePixels) return Sharpening.None("no tile: zoom")

    // Da schermo a coordinate viste: l'inversa di dove il rettangolo è finito.
    fun seenX(screen: Float) = (screen - drawn.left) / drawn.width() * fullWidth
    fun seenY(screen: Float) = (screen - drawn.top) / drawn.height() * fullHeight

    val area = Rect(
        floor(seenX(0f)).toInt().coerceIn(0, fullWidth),
        floor(seenY(0f)).toInt().coerceIn(0, fullHeight),
        ceil(seenX(viewWidth)).toInt().coerceIn(0, fullWidth),
        ceil(seenY(viewHeight)).toInt().coerceIn(0, fullHeight)
    )
    if (area.width() <= 0 || area.height() <= 0) return Sharpening.None("no tile: area")

    // Quanti pixel del file vale un pixel del bitmap di base: il campionamento che si ha già.
    val baseSample = (fullWidth.toFloat() / basePixels).roundToInt().coerceAtLeast(1)
    // Il tetto si calcola sulla vista: il perché è accanto a [TILE_SCREENFULS].
    val cap = maxOf(MIN_TILE_PIXELS, viewWidth.toLong() * viewHeight.toLong() * TILE_SCREENFULS)
    // Il campionamento che si vorrebbe, prima che il tetto lo alzi: la diagnostica distingue i due
    // motivi di rinuncia, `gain` e `cap`.
    val ideal = powerOfTwoAtMost(fullWidth / drawn.width())
    var sample = ideal
    while ((area.width().toLong() / sample) * (area.height().toLong() / sample) > cap) {
        sample *= 2
    }
    if (sample >= baseSample) {
        return Sharpening.None(if (ideal >= baseSample) "no tile: gain" else "no tile: cap")
    }
    return Sharpening.Read(area, sample)
}

/**
 * Un pezzo del file letto a risoluzione piena, con le frazioni di immagine che copre.
 *
 * Frazioni e non pixel di schermo: il rettangolo dove disegnarlo si ricalcola a ogni fotogramma,
 * quindi il pezzo resta incollato all'immagine anche se la vista si è mossa nel frattempo.
 * ⚠️ **Questo bitmap non si ricicla mai**: quando ne arriva uno nuovo, il vecchio può essere ancora
 * nello shader di un fotogramma in corso, e `recycle` farebbe chiudere l'app. Se ne occupa il
 * raccoglitore. I conti sono qui perché li usano il palco e la lente del colore mirato, e il banco
 * li misura senza un file vero.
 *
 * @property ask la richiesta da cui il pezzo è nato, con [healing] e [blurs] le correzioni e le
 *   sfocature già dipinte: servono a riconoscere il pezzo che si ha già in mano ([serves]).
 */
internal class SharpPiece(
    val pixels: Bitmap,
    val ask: Sharpening.Read,
    val at: RectF,
    val healing: Healing.Plan = Healing.Plan.NONE,
    val blurs: Drawing = Drawing()
) {

    /**
     * Se questo pezzo è già la risposta a [next] con [healing] e [blurs]: il palco chiede il pezzo
     * alla fine di ogni gesto, anche un tocco che non ha mosso la vista, e senza questo confronto
     * rileggerebbe dal file la stessa regione.
     */
    fun serves(next: Sharpening.Read, healing: Healing.Plan, blurs: Drawing): Boolean =
        next == ask && healing == this.healing && blurs == this.blurs

    /**
     * Dove disegnare il pezzo, dato [view], cioè dove l'immagine intera è disegnata adesso: il
     * palco passa la sua vista, la lente quella ingrandita del suo tondo.
     */
    fun place(view: RectF): RectF = RectF(
        view.left + at.left * view.width(),
        view.top + at.top * view.height(),
        view.left + at.right * view.width(),
        view.top + at.bottom * view.height()
    )

    /**
     * Il colore del pixel a [u],[v], frazioni dell'immagine intera, o `null` se quel punto è fuori
     * dal pezzo: è il caso normale fuori dalla finestra inquadrata, e chi chiama torna
     * all'anteprima.
     * ⚠️ Una conversione sbagliata darebbe il colore di un altro punto, senza nessun errore
     * visibile.
     */
    fun pixel(u: Float, v: Float): Int? {
        if (at.width() <= 0f || at.height() <= 0f) return null
        if (u < at.left || u > at.right || v < at.top || v > at.bottom) return null
        val x = ((u - at.left) / at.width() * (pixels.width - 1)).roundToInt()
        val y = ((v - at.top) / at.height() * (pixels.height - 1)).roundToInt()
        return pixels.getPixel(
            x.coerceIn(0, pixels.width - 1),
            y.coerceIn(0, pixels.height - 1)
        )
    }
}

/** La potenza di due immediatamente sotto, e mai meno di uno. */
private fun powerOfTwoAtMost(value: Float): Int =
    if (value < 2f) 1 else Integer.highestOneBit(value.toInt())

/**
 * Il tetto in pixel di un pezzo letto a piena risoluzione, contato in schermate.
 *
 * ⚠️ **Non un numero fisso**: con quattro milioni di pixel, su uno schermo da 1440x3120 (4,49
 * milioni) una schermata sola era già sopra il tetto, e al 100%, dove il pezzo è grande una
 * schermata, il campionamento saliva fino a quello di base e la lettura si annullava. Due schermate
 * e non quattro: il caso peggiore teorico è quattro (il campionamento arrotonda per difetto), ma
 * capita solo sotto il 100%, dove il guadagno è marginale e il costo sarebbe di 69 MB. Dal 100% in
 * su il pezzo si restringe con l'ingrandimento e passa sempre. Il minimo vale per gli schermi
 * piccoli, dove due schermate sarebbero un pezzo troppo modesto.
 */
private const val TILE_SCREENFULS = 2L
private const val MIN_TILE_PIXELS = 4_000_000L

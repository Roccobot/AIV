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
     * quella. Chi le ritocca le riderivi da lì.
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
        // ⚠️ L'ultima rete: un rettangolo fuori dai bordi manderebbe in errore
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
         * Apre la lettura a pezzi di [uri] per il **visualizzatore**, o **null** se non si
         * può o non conviene.
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
            // ⚠️ IL CONTROLLO CHE RENDE SICURO TUTTO IL RESTO: le misure grezze devono
            // dire la stessa cosa del tag. Qui si confrontano con quelle che
            // `ImageDecoder` ha misurato, cioè con l'EXIF già applicato.
            return make(context, uri) { seenWidth, seenHeight ->
                seenWidth == image.pixelWidth && seenHeight == image.pixelHeight
            }
        }

        /**
         * Apre la lettura a pezzi di [uri] per l'**editor completo**, che lavora su
         * un'anteprima ridotta, o **null** se non si può o non conviene.
         *
         * ⚠️⚠️ **È UNA SECONDA VIA PERCHÉ IL PRESUPPOSTO DELL'ALTRA QUI NON VALE**: là si
         * rinuncia quando l'immagine è entrata **intera** in memoria, perché il
         * visualizzatore disegna quella; qui il bitmap in mano è l'anteprima da 1600 pixel
         * di lato, quindi anche una fotografia piccola per il visualizzatore ha dettaglio
         * da recuperare. La domanda che decide non è più *era campionata* ma *il file ha
         * più pixel di quelli che ho*.
         *
         * ⚠️⚠️ **E IL CONTROLLO DI COERENZA DIVENTA UN RAPPORTO, PERCHÉ NON C'È PIÙ UNA
         * MISURA ESATTA DA CONFRONTARE**: l'anteprima è il file diviso per una potenza di
         * due, quindi i suoi lati non dicono quelli del file. Quello che deve tornare è la
         * **proporzione**: se il tag dice 'ruotata' e le misure grezze non risultano
         * scambiate, il presupposto di tutta questa classe (che `BitmapRegionDecoder` non
         * applichi l'orientamento) è caduto, e il rapporto se ne accorge.
         * ⚠️ **Su un'immagine quadrata quel controllo non distingue niente**, e si dichiara
         * invece di lasciarlo credere: là i due rapporti valgono uno in tutti e due i casi.
         */
        suspend fun open(context: Context, uri: Uri?, preview: Bitmap): RegionSource? {
            if (uri == null || preview.width <= 0 || preview.height <= 0) return null
            return make(context, uri) { seenWidth, seenHeight ->
                val gains = maxOf(seenWidth, seenHeight) > maxOf(preview.width, preview.height)
                val shape = seenWidth.toFloat() / seenHeight
                val same = preview.width.toFloat() / preview.height
                // ⚠️ La tolleranza non è prudenza generica: il campionamento arrotonda i
                // lati al pixel, quindi i due rapporti non coincidono mai alla cifra. Un
                // punto percentuale li tiene insieme e resta lontanissimo dallo scambio
                // dei lati, che è la cosa da riconoscere.
                gains && abs(shape - same) <= same * SHAPE_SLACK
            }
        }

        /**
         * Il lavoro comune alle due vie: legge il tag, apre il decodificatore e chiede a
         * [fits] se le misure **viste** che ne escono sono quelle che il chiamante si
         * aspetta. Un `false` è una rinuncia, e il chiamante resta com'era.
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
                // I ribaltamenti (2, 4, 5, 7) non sono rotazioni e non si trattano:
                // sono rarissimi, e indovinarli a memoria è il modo di sbagliarli.
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

/** Quanto i due rapporti possono distare prima che [RegionSource.open] rinunci. */
private const val SHAPE_SLACK = 0.01f

/**
 * L'esito di [sharpAsk]: che cosa leggere, o il **motivo** per cui non se ne fa niente.
 *
 * ⚠️⚠️ **IL MOTIVO ESISTE PERCHÉ IL SILENZIO NON DICE PERCHÉ, ed è la lezione già pagata dallo
 * sfoglio**: 'non succede niente' è identico fra una funzione rotta, una funzione che ha deciso
 * di non fare niente e un formato che non si sa rileggere. La riga dei dettagli del
 * visualizzatore lo stampa, ed è così che la `0.49` ha potuto nominare il difetto.
 */
internal sealed interface Sharpening {
    /** Il rettangolo da leggere, in coordinate **viste**, e con quanto campionamento. */
    data class Read(val area: Rect, val sample: Int) : Sharpening
    data class None(val why: String) : Sharpening
}

/**
 * Quale pezzo leggere adesso, e con quanto campionamento.
 *
 * ⚠️⚠️ **IL CONTO ESISTE IN UNA COPIA SOLA, E DALLA `2.27` LO CHIEDONO IN DUE**: il
 * visualizzatore, che ingrandisce una fotografia campionata, e l'editor completo, che lavora
 * sempre su un'anteprima da 1600 pixel di lato. La domanda è la stessa (*che pezzo del file mi
 * serve per riempire questo schermo*) e le due geometrie sono diverse solo in come ci si arriva,
 * quindi qui entra il risultato di quelle due geometrie: [drawn], il rettangolo di schermo in cui
 * l'immagine **intera** è disegnata adesso.
 *
 * ⚠️⚠️ **SI LEGGE QUANDO L'IMMAGINE DI BASE VIENE INGRANDITA E NON PRIMA**: sotto quel confine un
 * suo pixel copre meno di un pixel di schermo, e il dettaglio che manca non si vedrebbe comunque.
 * ⚠️ **E il confine si RICAVA da [basePixels]**, cioè da quanti pixel ha davvero il bitmap che si
 * sta disegnando: non è una soglia scritta a mano, ed è la stessa disuguaglianza del guadagno qui
 * sotto, presa un passo prima.
 *
 * ⚠️ **Il campionamento si arrotonda per DIFETTO** (la potenza di due immediatamente sotto): per
 * eccesso si leggerebbe meno dettaglio di quello che lo schermo può mostrare, cioè si farebbe
 * tutto questo lavoro per restare sfocati. Il prezzo è un pezzo che può venire fino al doppio del
 * necessario per lato, e per quello c'è il tetto.
 *
 * ⚠️ Il tetto sui pixel non è prudenza generica: senza, un ingrandimento appena sopra la soglia su
 * una fotografia enorme chiederebbe un pezzo grande quanto tutto lo schermo moltiplicato per
 * quattro, cioè una decina di volte la memoria del bitmap che sta già mostrando. Quando alzare il
 * campionamento per rientrare lo porta al livello del bitmap di base, tanto vale non leggere
 * niente.
 *
 * @param fullWidth,fullHeight le misure **viste** del file, cioè quelle di [RegionSource].
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

    // Quanti pixel del file vale un pixel del bitmap di base: è il campionamento che si ha già
    // in mano, e non dipende dall'ingrandimento.
    val baseSample = (fullWidth.toFloat() / basePixels).roundToInt().coerceAtLeast(1)
    // ⚠️ Il tetto si calcola sulla VISTA e non è un numero scritto a mano: il perché, e il conto
    // che il numero fisso sbagliava, vivono accanto a [TILE_SCREENFULS].
    val cap = maxOf(MIN_TILE_PIXELS, viewWidth.toLong() * viewHeight.toLong() * TILE_SCREENFULS)
    // Il campionamento che si vorrebbe, prima che il tetto lo alzi: i due si distinguono perché
    // dicono due cose diverse a chi legge la diagnostica, e distinguerli è l'unico modo di sapere
    // se un giorno il tetto tornasse a essere il problema.
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

/** La potenza di due immediatamente sotto, e mai meno di uno. */
private fun powerOfTwoAtMost(value: Float): Int =
    if (value < 2f) 1 else Integer.highestOneBit(value.toInt())

/**
 * Il tetto in pixel di un pezzo letto a piena risoluzione, **contato in schermate**.
 *
 * ⚠️⚠️ **ERA UN NUMERO FISSO, QUATTRO MILIONI, ED È IL DIFETTO CHE HA RESO INUTILE LA `0.39` SUI
 * TELEFONI DI OGGI** (segnalazione dell'utente, 2026-08-29: *con l'immagine grande non mi accorgo
 * di nulla*, con la riga dei dettagli che diceva `sampled`, quindi i tasselli dovevano
 * accendersi). La nota vecchia diceva 'circa uno schermo e mezzo', e il conto **non torna più**:
 * su uno schermo da 1440x3120 una schermata sola è **4.49 milioni** di pixel, cioè già sopra il
 * tetto. Quel numero è nato quando 1080x2400 (2.59 milioni) era il normale.
 * ⚠️⚠️ **E la conseguenza era esattamente 'non succede niente', nel punto in cui la funzione serve
 * di più**: al **100%** il pezzo da leggere è grande **esattamente una schermata**, quindi il
 * tetto lo respingeva, il campionamento veniva alzato per rientrare, e alzato arrivava al livello
 * del bitmap di base, dove leggere non guadagna niente e si rinuncia. Il codice faceva la cosa
 * giusta con un dato sbagliato.
 * ⚠️ **Due schermate e non quattro**, e le due soglie vogliono dire cose diverse: il caso peggiore
 * teorico è **quattro** schermate (il campionamento arrotonda per difetto, quindi fino al doppio
 * per lato), ma capita solo **sotto** il 100%, dove il bitmap di base è ingrandito appena e il
 * guadagno è marginale mentre il costo sarebbe di 69 MB. A due schermate il 100% e tutti gli
 * ingrandimenti sopra di lui passano sempre, perché da lì in su il pezzo **si restringe** man mano
 * che si ingrandisce.
 * ⚠️ Il minimo esiste per gli schermi piccoli, dove due schermate sarebbero un pezzo troppo
 * modesto per valere la lettura.
 */
private const val TILE_SCREENFULS = 2L
private const val MIN_TILE_PIXELS = 4_000_000L

package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.annotation.StringRes
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Che cosa succede quando l'editor di casa salva: girare, ritagliare, scrivere.
 *
 * ⚠️⚠️ **LA ROTAZIONE SENZA RITAGLIO NON TOCCA UN PIXEL, e su un JPEG è la differenza fra
 * lossless e no**: un JPEG porta in testa il tag EXIF dell'orientamento, e girare una
 * fotografia vuol dire cambiare **quel numero**, non ricomprimere venti megapixel. Una
 * ricompressione a ogni rotazione degrada l'immagine ogni volta, e dopo quattro giri si
 * torna al punto di partenza con una foto peggiore. Qui invece dopo quattro giri il file è
 * **identico** a com'era.
 * ⚠️ **Vale per il solo JPEG**: PNG e WebP il tag non ce l'hanno. Il PNG però si ricomprime
 * senza perdere niente, perché è un formato senza perdita per costruzione; il WebP no.
 *
 * ⚠️⚠️ **I PIXEL SI DECODIFICANO CON `ImageDecoder`, CHE APPLICA GIÀ L'ORIENTAMENTO EXIF**
 * (documentato in `ImageSource`, e la ragione per cui `RegionSource` deve convertire le
 * coordinate). Quindi la mappa di pixel che arriva qui è **già dritta**, il ritaglio e la
 * rotazione si ragionano su quella, e il file che si scrive porta orientamento **normale**:
 * scrivere i pixel girati e lasciare anche il tag vecchio vorrebbe dire una fotografia
 * ruotata due volte.
 *
 * ⚠️⚠️ **L'EXIF SI TRAVASA A MANO QUANDO SI RICOMPRIME**: `Bitmap.compress` scrive i soli
 * pixel, quindi senza questo passo una fotografia ritagliata perderebbe data, fotocamera e
 * posizione. Sono i campi che la pastiglia 'Info' mostra, quindi la perdita si vedrebbe
 * subito e non ci sarebbe modo di tornare indietro.
 */
object ImageEdit {

    /**
     * Il rettangolo tenuto, in frazioni del lato, **dopo** la rotazione.
     *
     * ⚠️ In frazioni e non in pixel: chi lo sceglie lo fa su un'anteprima rimpicciolita, e
     * un rettangolo in pixel dell'anteprima non vorrebbe dire niente sul file vero.
     */
    data class Crop(val left: Float, val top: Float, val right: Float, val bottom: Float) {

        /** Se non taglia niente: la tolleranza è quella di un dito su un'anteprima. */
        val whole: Boolean
            get() = left <= EDGE && top <= EDGE && right >= 1f - EDGE && bottom >= 1f - EDGE

        companion object {
            val WHOLE = Crop(0f, 0f, 1f, 1f)

            /** Sotto mezzo punto percentuale il ritaglio non esiste: è la mano che trema. */
            private const val EDGE = 0.005f
        }
    }

    /** Se il file salvato prende il posto dell'originale o gli si mette accanto. */
    enum class Way { OVERWRITE, COPY }

    /** Com'è andata. */
    sealed interface Result {
        /** Fatto: dove, e se si è riusciti a non toccare i pixel. */
        data class Done(val file: File, val lossless: Boolean) : Result

        /** Non fatto, e perché, in una frase da mostrare. */
        data class Failed(@StringRes val why: Int) : Result
    }

    /**
     * Se si può riscrivere **sopra** l'originale.
     *
     * ⚠️⚠️ **NO QUANDO IL FORMATO CAMBIEREBBE, ed è una tutela e non un limite**: di un HEIC
     * o di un AVIF si sanno leggere i pixel ma non si sanno riscrivere, quindi l'unica uscita
     * è un JPEG. Sovrascrivere vorrebbe dire mettere un JPEG dentro un file che si chiama
     * `.heic`: il sistema lo aprirebbe lo stesso, guardando il contenuto, ma il nome
     * mentirebbe per sempre e nessuno saprebbe più che cosa c'è dentro.
     */
    fun canOverwrite(name: String): Boolean = format(name) != null

    /**
     * Con che nome esce, dato quello di partenza.
     *
     * ⚠️ Il nome cambia **solo** se cambia il formato: chi ritaglia un JPEG si aspetta un
     * JPEG che si chiama come prima, non una copia con l'estensione diversa.
     */
    fun outputName(name: String): String {
        if (format(name) != null) return name
        return name.substringBeforeLast('.', name) + ".jpg"
    }

    /**
     * Applica e scrive.
     *
     * @param turns quarti di giro in senso orario, da 0 a 3.
     * @param crop che cosa tenere, in frazioni, dopo la rotazione.
     * @param backup se, sovrascrivendo, una copia della versione di prima va nel cestino.
     *
     * ⚠️ **`NonCancellable` come le altre operazioni sui file**: a metà scrittura una
     * cancellazione lascerebbe un file troncato dove prima c'era una fotografia.
     */
    suspend fun save(
        context: Context,
        uri: Uri,
        turns: Int,
        crop: Crop,
        way: Way,
        backup: Boolean
    ): Result = withContext(Dispatchers.IO + NonCancellable) {
        val source = FileTree.fileOf(context, uri)
            ?: return@withContext Result.Failed(R.string.edit_no_file)
        val dir = source.parentFile ?: return@withContext Result.Failed(R.string.edit_no_file)
        if (turns == 0 && crop.whole) return@withContext Result.Failed(R.string.edit_nothing)

        /*
         * ⚠️⚠️ **LA COPIA DI SICUREZZA SI FA QUI, PRIMA DI OGNI ALTRA COSA, ed è l'unico
         * punto che le copre tutte e due**: sia la via senza perdita sia il ridisegno
         * riscrivono l'originale quando si sovrascrive, quindi metterla dentro una delle due
         * vorrebbe dire dimenticarsene nell'altra il giorno che se ne aggiunge una terza.
         * ⚠️⚠️ **UNA COPIA CHE NON RIESCE FERMA IL SALVATAGGIO, e non è eccesso di zelo**:
         * chi ha acceso quell'interruttore ha chiesto di non poter perdere l'originale, e
         * sovrascrivere lo stesso gli darebbe esattamente la cosa da cui si stava
         * proteggendo, per giunta in silenzio.
         * ⚠️ Con `Way.COPY` non serve: là l'originale non lo tocca nessuno, e una copia in
         * più sarebbe un file nel cestino che nessuno ha chiesto.
         */
        if (way == Way.OVERWRITE && backup && Bin.keep(context, source) == null) {
            return@withContext Result.Failed(R.string.edit_no_backup)
        }

        val jpeg = source.extension.lowercase() in JPEG_EXT
        // ⚠️ La via senza perdita vale solo se non c'è ritaglio: tagliare vuol dire per forza
        // riscrivere i pixel, e allora tanto vale girarli insieme.
        if (jpeg && crop.whole) return@withContext turnOnly(context, source, dir, turns, way)

        val target = when (way) {
            Way.OVERWRITE ->
                if (canOverwrite(source.name)) source
                else return@withContext Result.Failed(R.string.edit_no_overwrite)
            Way.COPY -> FileTree.freeName(dir, outputName(source.name))
        }
        redraw(context, uri, source, target, turns, crop)
    }

    /**
     * La via senza perdita: si cambia il numero dell'orientamento e basta.
     *
     * ⚠️ **La copia si fa PRIMA di toccare il tag**: cambiando prima il tag sull'originale e
     * copiando dopo si sarebbe modificato un file che l'utente aveva chiesto di lasciare
     * stare.
     */
    private suspend fun turnOnly(
        context: Context,
        source: File,
        dir: File,
        turns: Int,
        way: Way
    ): Result {
        val target = when (way) {
            Way.OVERWRITE -> source
            Way.COPY -> FileTree.freeName(dir, source.name)
        }
        if (target != source) {
            val copied = runCatching {
                source.inputStream().use { input -> target.outputStream().use { input.copyTo(it) } }
                true
            }.getOrDefault(false)
            if (!copied) {
                target.delete()
                return Result.Failed(R.string.edit_failed)
            }
        }
        val ok = runCatching {
            val exif = ExifInterface(target)
            val now = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, turned(now, turns).toString())
            exif.saveAttributes()
            true
        }.getOrDefault(false)
        if (!ok) {
            if (target != source) target.delete()
            return Result.Failed(R.string.edit_failed)
        }
        FileTree.scan(context, listOfNotNull(source.absolutePath, target.absolutePath))
        return Result.Done(target, lossless = true)
    }

    /**
     * La via che ridisegna: decodifica, gira, taglia, ricomprime.
     *
     * ⚠️⚠️ **SI SCRIVE IN UN FILE PROVVISORIO E SI RINOMINA ALLA FINE**, come lo scaricamento
     * dei modelli: sovrascrivendo direttamente, una compressione che fallisce a metà lascia
     * al posto della fotografia un file troncato, e l'originale non c'è più da nessuna parte.
     * Col nome provvisorio, il file buono esiste solo quando è finito.
     * ⚠️ **L'`OutOfMemoryError` si cattura e si racconta**: una fotografia da cinquanta
     * megapixel sono duecento megabyte di mappa, e su un telefono stretto la decodifica può
     * non farcela. Meglio una frase che dice 'troppo grande' di un'app che sparisce.
     */
    private suspend fun redraw(
        context: Context,
        uri: Uri,
        source: File,
        target: File,
        turns: Int,
        crop: Crop
    ): Result {
        val temp = File(target.parentFile, target.name + ".part")
        var full: Bitmap? = null
        var turned: Bitmap? = null
        var cut: Bitmap? = null
        try {
            // ⚠️ **Il secondo tentativo serve ai formati che il sistema non apre**, AVIF e
            // SVG: senza, un ritaglio su quei file rispondeva 'troppo grande', che è un
            // messaggio sbagliato su un file di venti kilobyte. Lo zero vuol dire 'grande
            // quanto viene', perché qui si sta per riscrivere e non si mostra niente.
            full = runCatching {
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.contentResolver, uri)
                ) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false
                }
            }.getOrNull()
                ?: ImageSource.rescue(context, uri, 0)
                ?: return Result.Failed(R.string.edit_too_big)

            turned = if (turns == 0) full else Bitmap.createBitmap(
                full, 0, 0, full.width, full.height,
                Matrix().apply { postRotate(90f * turns) },
                true
            )
            if (turned !== full) {
                full.recycle()
                full = null
            }

            cut = if (crop.whole) turned else {
                val x = (crop.left * turned.width).toInt().coerceIn(0, turned.width - 1)
                val y = (crop.top * turned.height).toInt().coerceIn(0, turned.height - 1)
                val w = ((crop.right - crop.left) * turned.width).toInt()
                    .coerceIn(1, turned.width - x)
                val h = ((crop.bottom - crop.top) * turned.height).toInt()
                    .coerceIn(1, turned.height - y)
                Bitmap.createBitmap(turned, x, y, w, h)
            }

            val kind = format(target.name) ?: Bitmap.CompressFormat.JPEG
            val written = runCatching {
                temp.outputStream().use { cut.compress(kind, QUALITY, it) }
            }.getOrDefault(false)
            if (!written) {
                temp.delete()
                return Result.Failed(R.string.edit_failed)
            }
        } catch (_: OutOfMemoryError) {
            temp.delete()
            return Result.Failed(R.string.edit_too_big)
        } finally {
            if (cut !== turned) cut?.recycle()
            if (turned !== full) turned?.recycle()
            full?.recycle()
        }

        carryExif(source, temp)
        // ⚠️ Il rinomina è l'ultimo passo, e su Android sovrascrive: da qui in poi il file
        // buono c'è, e quello provvisorio non esiste più.
        if (!temp.renameTo(target)) {
            temp.delete()
            return Result.Failed(R.string.edit_failed)
        }
        FileTree.scan(context, listOfNotNull(source.absolutePath, target.absolutePath))
        return Result.Done(target, lossless = false)
    }

    /**
     * Travasa i dati EXIF che vale la pena non perdere.
     *
     * ⚠️⚠️ **L'ORIENTAMENTO NON SI TRAVASA, ed è il punto**: i pixel scritti sono già dritti e
     * già girati come l'utente ha chiesto. Portandosi dietro il tag vecchio, la fotografia
     * verrebbe ruotata una seconda volta a ogni apertura.
     * ⚠️ **Nemmeno le misure**: dopo un ritaglio i lati sono altri, e un tag che dichiara i
     * lati di prima è peggio di un tag assente.
     * ⚠️ **Il travaso può non riuscire e non è un errore da fermare tutto**: `ExifInterface`
     * scrive JPEG, PNG e WebP e non gli altri, e una fotografia ritagliata senza la data resta
     * una fotografia ritagliata. Si prova e si tira avanti.
     */
    private fun carryExif(from: File, to: File) {
        runCatching {
            val old = ExifInterface(from)
            val new = ExifInterface(to)
            var any = false
            for (tag in KEEP) {
                val value = old.getAttribute(tag) ?: continue
                new.setAttribute(tag, value)
                any = true
            }
            if (any) new.saveAttributes()
        }
    }

    /**
     * L'orientamento EXIF dopo [turns] quarti di giro in senso orario.
     *
     * ⚠️⚠️ **LA TABELLA È DERIVATA, non ricordata**: un orientamento EXIF è uno specchio
     * facoltativo seguito da una rotazione (1 e 6 e 3 e 8 senza specchio, 2 e 7 e 4 e 5 con),
     * e girare la vista di 90 gradi aggiunge 90 alla rotazione lasciando lo specchio dov'è.
     * Da lì escono i due cicli qui sotto. Chi la copia da un forum prende quella di 'ruota il
     * file', che è un'altra cosa e sbaglia sulle quattro con lo specchio.
     */
    internal fun turned(now: Int, turns: Int): Int {
        val cycle = when (now) {
            in DIRECT -> DIRECT
            in MIRROR -> MIRROR
            else -> DIRECT
        }
        val at = cycle.indexOf(now).takeIf { it >= 0 } ?: 0
        return cycle[(at + turns).mod(cycle.size)]
    }

    /** Il ciclo dei quarti di giro senza specchio: normale, 90, 180, 270. */
    private val DIRECT = listOf(
        ExifInterface.ORIENTATION_NORMAL,
        ExifInterface.ORIENTATION_ROTATE_90,
        ExifInterface.ORIENTATION_ROTATE_180,
        ExifInterface.ORIENTATION_ROTATE_270
    )

    /** Lo stesso ciclo per le quattro con lo specchio. */
    private val MIRROR = listOf(
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
        ExifInterface.ORIENTATION_TRANSVERSE,
        ExifInterface.ORIENTATION_FLIP_VERTICAL,
        ExifInterface.ORIENTATION_TRANSPOSE
    )

    /** In che formato si riscrive, e `null` quando non si sa riscrivere quello di partenza. */
    private fun format(name: String): Bitmap.CompressFormat? =
        when (name.substringAfterLast('.', "").lowercase()) {
            in JPEG_EXT -> Bitmap.CompressFormat.JPEG
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            else -> null
        }

    /** Le due estensioni del JPEG, che sono l'unico formato con la via senza perdita. */
    private val JPEG_EXT = setOf("jpg", "jpeg")

    /**
     * Quanto si comprime quando si deve ricomprimere.
     *
     * ⚠️ **95 e non 100**: fra i due la differenza a occhio non c'è e il file quasi
     * raddoppia, perché a 100 la quantizzazione JPEG smette di fare il suo mestiere. Sul PNG
     * il numero non ha effetto, ed è giusto così: quel formato non perde niente.
     */
    private const val QUALITY = 95

    /**
     * I campi che seguono la fotografia.
     *
     * ⚠️ Sono quelli che la pastiglia 'Info' mostra (vedi `Facts`), più i satelliti: la prova
     * che il travaso serve è che senza di lui quella pastiglia si svuoterebbe dopo un
     * ritaglio, e l'utente lo vedrebbe subito.
     */
    private val KEEP = listOf(
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_OFFSET_TIME,
        ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
        ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_TIMESTAMP
    )
}

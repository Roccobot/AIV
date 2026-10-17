package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import kotlin.math.roundToInt

/**
 * Uno specchio facoltativo e una rotazione: le otto pose in cui si può mettere un'immagine.
 *
 * ⚠️⚠️ **Sono otto, e per questo i gesti non si rifanno uno per uno**: riflessioni e quarti di
 * giro si compongono sempre in uno specchio più una rotazione, quindi dieci tocchi diventano una
 * trasformazione sola da applicare al file. Sono anche le otto dell'orientamento EXIF.
 * ⚠️ **L'ordine dichiarato è 'specchia, poi gira'**, lo stesso di `ImageEdit.save`: senza,
 * `Spin(1, true)` sarebbe due trasformazioni diverse secondo chi lo legge.
 * Le pose le leggono tutti e due gli editor e il modello dell'editor completo (`Look`); accanto a
 * [ImageEdit.Crop], perché sono i due valori che dicono come è messa un'immagine.
 */
data class Spin(val turns: Int, val mirror: Boolean) {

    /**
     * Questa posa, e **poi** [next]: cioè `next` applicata a quello che si vede adesso.
     *
     * ⚠️⚠️ **Uno specchio davanti a una rotazione la rovescia**: `M ∘ R(k) = R(-k) ∘ M`, quindi
     * con `next.mirror` i quarti di giro accumulati cambiano segno e lo specchio si alterna. Una
     * somma anche in quel ramo girerebbe l'immagine dalla parte sbagliata solo quando c'è già una
     * rotazione, cioè un difetto che passa tutte le prove fatte a immagine dritta.
     */
    fun then(next: Spin): Spin =
        if (next.mirror) Spin((next.turns - turns).mod(4), !mirror)
        else Spin((next.turns + turns).mod(4), mirror)

    companion object {
        val STILL = Spin(0, false)

        /** Il gesto 'rifletti in orizzontale': lo specchio nudo. */
        val ACROSS = Spin(0, true)

        /**
         * Il gesto 'rifletti in verticale'.
         *
         * È lo specchio orizzontale più mezzo giro, non un secondo meccanismo: così la matrice,
         * l'EXIF e il rettangolo hanno un caso in meno da coprire.
         */
        val DOWN = Spin(2, true)
    }
}

/**
 * Che cosa succede quando l'editor semplice salva: girare, ritagliare, scrivere.
 *
 * ⚠️⚠️ **Su un JPEG la rotazione senza ritaglio non tocca un pixel**: cambia il tag EXIF
 * dell'orientamento e non ricomprime, quindi dopo quattro giri il file è identico a com'era. Vale
 * per il solo JPEG: PNG e WebP il tag non lo hanno, e il PNG si ricomprime comunque senza perdita.
 *
 * ⚠️⚠️ **I pixel si decodificano con `ImageDecoder`, che applica già l'orientamento EXIF** (vedi
 * `ImageSource`): la mappa che arriva qui è già dritta, il ritaglio e la rotazione si ragionano su
 * quella, e il file scritto ha orientamento normale. Pixel girati col tag vecchio darebbero
 * un'immagine ruotata due volte.
 *
 * ⚠️⚠️ **L'EXIF si travasa a mano quando si ricomprime**: `Bitmap.compress` scrive i soli pixel,
 * e un'immagine ritagliata perderebbe data, fotocamera e posizione, cioè i campi della pastiglia
 * 'Info'.
 */
object ImageEdit {

    /**
     * Il rettangolo tenuto, in frazioni del lato, **dopo** la rotazione.
     *
     * In frazioni e non in pixel: si sceglie su un'anteprima rimpicciolita, e i suoi pixel non
     * dicono niente sul file vero.
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
        /**
         * Fatto: dove, e se si è riusciti a non toccare i pixel.
         *
         * @param downloads se il file è andato in Download, l'unica uscita di AIV Play: là [file]
         *   è la copia provvisoria, già cancellata.
         */
        data class Done(val file: File, val lossless: Boolean, val downloads: Boolean = false) : Result

        /** Non fatto, e perché, in una frase da mostrare. */
        data class Failed(@StringRes val why: Int) : Result
    }

    /**
     * Se si può riscrivere **sopra** l'originale.
     *
     * ⚠️ **No quando il formato cambierebbe**: un HEIC o un AVIF si leggono ma non si riscrivono,
     * e sovrascrivere metterebbe un JPEG in un file che si chiama `.heic`.
     */
    fun canOverwrite(name: String): Boolean = format(name) != null

    /**
     * Con che nome esce, dato quello di partenza.
     *
     * Il nome cambia solo se cambia il formato: chi ritaglia un JPEG si aspetta un JPEG col
     * nome di prima.
     *
     * ⚠️ **Dai formati con la trasparenza esce un PNG** ([ALPHA_EXT]; richiesta dell'utente, voce
     * `svg-modifica`): un JPEG perderebbe per sempre il fondo tolto. Dai formati fotografici esce
     * un JPEG, perché un PNG da ventiquattro megapixel pesa decine di megabyte; là la trasparenza,
     * se c'è, va su fondo bianco (vedi [redraw]).
     */
    fun outputName(name: String): String {
        if (format(name) != null) return name
        val base = name.substringBeforeLast('.', name)
        val coda = if (name.substringAfterLast('.', "").lowercase() in ALPHA_EXT) ".png"
        else ".jpg"
        return base + coda
    }

    /**
     * Applica e scrive.
     *
     * @param turns quarti di giro in senso orario, da 0 a 3.
     * @param mirror se prima della rotazione l'immagine si specchia sull'asse verticale.
     * @param crop che cosa tenere, in frazioni, dopo la rotazione.
     * @param backup se, sovrascrivendo, una copia della versione di prima va nel cestino.
     *
     * ⚠️⚠️ **L'ordine è 'specchia, gira, taglia'**: così ogni catena di riflessioni e rotazioni
     * si riscrive in uno specchio più una rotazione, e salvare non rifà i gesti uno per uno sui
     * pixel. Il conto che compone i passi vive su [Spin.then], in questo file.
     *
     * ⚠️ **`NonCancellable` come le altre operazioni sui file**: a metà scrittura una
     * cancellazione lascerebbe un file troncato.
     */
    suspend fun save(
        context: Context,
        uri: Uri,
        turns: Int,
        mirror: Boolean,
        crop: Crop,
        way: Way,
        backup: Boolean,
        /**
         * La filigrana da scrivere sull'immagine, o `null` per nessuna.
         *
         * Arriva come argomento, come la copia di sicurezza: quale interruttore sia acceso lo sa
         * chi ha le impostazioni, e così il banco la può passare. ⚠️ **Conta come lavoro da
         * fare**: chi apre l'editor solo per firmare non tocca niente altro.
         */
        mark: Watermark.Plan? = null,
        /**
         * A che misura scrivere, o `null` per lasciare quella dell'immagine. Arriva come argomento
         * e conta come lavoro da fare, come la filigrana.
         */
        resize: Resize.Plan? = null
    ): Result = withContext(Dispatchers.IO + NonCancellable) {
        val source = FileTree.fileOf(context, uri)
            ?: return@withContext Result.Failed(R.string.edit_no_file)
        val dir = outDir(context, source) ?: return@withContext Result.Failed(R.string.edit_no_file)
        // ⚠️ Un piano che su questo file non rimpicciolisce niente non è lavoro: l'interruttore
        // acceso passa il piano ogni volta, e 'Lato lungo 2048' su un JPEG da 1600 toglierebbe il
        // senza perdita per niente. [shrinks] lo misura sulle misure vere che si scriverebbero.
        val resize = resize?.takeIf { shrinks(source, it, turns, crop) }
        if (turns == 0 && !mirror && crop.whole && mark == null && resize == null) {
            return@withContext Result.Failed(R.string.edit_nothing)
        }
        // ⚠️ In AIV Play l'originale non si sovrascrive mai: vedi [outDir].
        val via = if (Store.files) way else Way.COPY

        /*
         * ⚠️⚠️ **La copia di sicurezza si fa qui, prima di tutto**, l'unico punto che copre le due
         * vie, che riscrivono entrambe l'originale quando si sovrascrive.
         * ⚠️⚠️ **Una copia che non riesce ferma il salvataggio**: chi ha acceso l'interruttore ha
         * chiesto di non poter perdere l'originale. Con `Way.COPY` non serve, perché l'originale
         * non si tocca.
         */
        if (via == Way.OVERWRITE && backup && Bin.keep(context, source) == null) {
            return@withContext Result.Failed(R.string.edit_no_backup)
        }

        val jpeg = source.extension.lowercase() in JPEG_EXT
        // ⚠️ La via senza perdita vale finché i pixel non vanno riscritti: un ritaglio, una
        // filigrana e un ridimensionamento la tolgono, perché un tag EXIF dice come guardare i
        // pixel e non quali o quanti sono. Vale anche per uno specchio: l'orientamento EXIF ne ha
        // quattro con lo specchio (2, 4, 5, 7).
        if (jpeg && crop.whole && mark == null && resize == null) {
            return@withContext delivered(context, turnOnly(context, source, dir, turns, mirror, via))
        }

        val target = when (via) {
            Way.OVERWRITE ->
                if (canOverwrite(source.name)) source
                else return@withContext Result.Failed(R.string.edit_no_overwrite)
            Way.COPY -> FileTree.freeName(dir, outputName(source.name))
        }
        delivered(context, redraw(context, uri, source, target, turns, mirror, crop, mark, resize))
    }

    /**
     * Se [plan] rimpicciolisce davvero l'immagine che il salvataggio scriverebbe da [source].
     *
     * Le misure si leggono dall'intestazione (`inJustDecodeBounds`), senza decodificare; si
     * scambiano se l'EXIF o la posa girano di un quarto, perché i modi 'Larghezza' e 'Altezza'
     * guardano un lato preciso, e si riducono col ritaglio, come in [Resize.frameSize].
     * ⚠️ **Senza misure risponde di sì**, cioè ridisegna: un file che non si legge non deve
     * perdere il ridimensionamento chiesto.
     */
    internal fun shrinks(source: File, plan: Resize.Plan, turns: Int, crop: Crop): Boolean {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { BitmapFactory.decodeFile(source.path, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return true
        val exifQuarter = runCatching { ExifInterface(source).rotationDegrees % 180 != 0 }
            .getOrDefault(false)
        val quarter = exifQuarter != (turns.mod(2) == 1)
        val w = if (quarter) bounds.outHeight else bounds.outWidth
        val h = if (quarter) bounds.outWidth else bounds.outHeight
        val cw = maxOf(1, (w * (crop.right - crop.left)).roundToInt())
        val ch = maxOf(1, (h * (crop.bottom - crop.top)).roundToInt())
        return plan.sizeFor(cw, ch) != null
    }

    /**
     * Dove scrive un salvataggio: accanto all'originale, o in AIV Play in una cartella dell'app.
     *
     * ⚠️⚠️ **AIV Play non può scrivere nella cartella dell'originale**: senza il permesso su
     * tutti i file, un file di un'altra app o della fotocamera si legge e non si sostituisce.
     * Là gli editor scrivono sempre un file nuovo, prima qui e poi in Download ([delivered]), e
     * l'originale resta com'è: nessuna copia nel cestino, perché [Way.COPY] non la chiede.
     */
    private fun outDir(context: Context, source: File): File? =
        if (Store.files) source.parentFile
        else File(context.cacheDir, "editor").takeIf { it.isDirectory || it.mkdirs() }

    /**
     * In AIV Play il file scritto in [outDir] va in Download, e quello privato si cancella in ogni
     * caso; in AIV GitHub il risultato torna com'è arrivato.
     */
    private suspend fun delivered(context: Context, result: Result): Result {
        if (Store.files || result !is Result.Done) return result
        val moved = ImageActions.fileToDownloads(context, result.file)
        result.file.delete()
        return if (moved) result.copy(downloads = true) else Result.Failed(R.string.edit_failed)
    }

    /**
     * Applica i valori dell'editor **completo** e scrive.
     *
     * Vive qui perché le due strade (la posa e i valori) condividono la copia di sicurezza, il
     * travaso EXIF, il file provvisorio e la rinomina finale.
     *
     * ⚠️⚠️ **La qualità decide anche il formato**: un JPEG non può essere senza perdita, quindi
     * con 'Senza perdita' esce un PNG, che ha un altro nome e si mette accanto all'originale.
     *
     * `NonCancellable` come tutto il resto.
     */
    suspend fun saveLook(
        context: Context,
        uri: Uri,
        look: Look,
        quality: Quality,
        backup: Boolean,
        /**
         * La filigrana, come in [save]. Non vive in [Look], che è un aspetto che uno stile porta
         * da un'immagine all'altra: vedi la testa di [Watermark].
         */
        mark: Watermark.Plan? = null,
        /**
         * A che misura scrivere, come in [save]. Non vive in [Look], per la ragione scritta in
         * testa a [Resize].
         */
        resize: Resize.Plan? = null,
        /**
         * Se il file nuovo va accanto all'originale: il tocco lungo su 'Salva' (richiesta
         * dell'utente). È la stessa strada di un formato che non si riscrive, cioè [Way.COPY] e
         * [lookTarget] con un nome libero; la copia di sicurezza si salta da sé, perché il
         * bersaglio non è la sorgente.
         */
        beside: Boolean = false
    ): Result = withContext(Dispatchers.IO + NonCancellable) {
        val source = FileTree.fileOf(context, uri)
            ?: return@withContext Result.Failed(R.string.edit_no_file)
        val dir = outDir(context, source) ?: return@withContext Result.Failed(R.string.edit_no_file)
        // ⚠️ In AIV Play il file è sempre nuovo: vedi [outDir].
        val accanto = beside || !Store.files
        // ⚠️ Filigrana e ridimensionamento sono lavoro anche a cursori fermi: la guardia non
        // guarda soltanto il [Look].
        if (look.idle && mark == null && resize == null) {
            return@withContext Result.Failed(R.string.edit_nothing)
        }
        /*
         * ⚠️⚠️ **Col solo modulo Ritaglio si passa da [save]**: posa e taglio sono il suo lavoro,
         * compreso il senza perdita di un JPEG girato con un tag EXIF, che è la clausola
         * dell'utente. La via si sceglie sul nome e non sulla qualità: un formato che non si
         * riscrive esce accanto, come fa [lookTarget].
         */
        if (look.plain && look.geo.idle && look.healing.idle && look.drawing.idle) {
            val way = if (!accanto && canOverwrite(source.name)) Way.OVERWRITE else Way.COPY
            return@withContext save(
                context, uri, look.spin.turns, look.spin.mirror, look.crop, way, backup, mark,
                resize
            )
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return@withContext Result.Failed(R.string.look_failed)
        }

        val kind = lookFormat(source.name, quality)
        val target = lookTarget(source, dir, kind, accanto)
        // ⚠️ La copia di sicurezza si fa solo quando si sovrascrive, e prima di tutto, con la
        // regola di [save]: un fallimento ferma il salvataggio.
        if (target == source && backup && Bin.keep(context, source) == null) {
            return@withContext Result.Failed(R.string.edit_no_backup)
        }

        val temp = File(target.parentFile, target.name + ".part")
        var full: Bitmap? = null
        var posed: Bitmap? = null
        var shaded: Bitmap? = null
        var drawn: Bitmap? = null
        var warped: Bitmap? = null
        var done: Bitmap? = null
        var small: Bitmap? = null
        var signed: Bitmap? = null
        try {
            full = ImageSource.pixels(context, uri, 0)
                ?: return@withContext Result.Failed(R.string.edit_too_big)
            val healed = Healing.render(full, look.healing)
            if (healed !== full) { full.recycle(); full = healed }
            // ⚠️ Gli elementi sfocanti del modulo Disegno cambiano l'immagine prima dello
            // sviluppo, come sul palco: la ragione vive su `Draw.blurAreas`.
            val blurred = Draw.blurAreas(full, look.drawing, mine = true)
            if (blurred !== full) { full.recycle(); full = blurred }
            /*
             * ⚠️⚠️ **La posa per prima e il taglio per ultimo**: la posa è una permutazione di
             * pixel, quindi il Dettaglio legge ancora i pixel del file, e i due keystone lavorano
             * sugli assi che si vedono. Il taglio è l'inquadratura, che si decide guardando il
             * risultato.
             */
            posed = if (look.spin == Spin.STILL) full
            else full.spunBy(look.spin.turns, look.spin.mirror, strict = true)
            /*
             * ⚠️⚠️ **Due passate, prima il colore e poi la geometria**: il colore gira sulla scheda
             * grafica a tessere, la geometria è una maglia di triangoli su una tela normale. Il
             * Dettaglio deve leggere i pixel come sono nel file: deformando prima, misurerebbe una
             * nitidezza ammorbidita dal ricampionamento. Ognuna si salta quando non ha niente da
             * fare.
             */
            shaded = if (look.plain) posed else AdjustRender.apply(posed, look)
                ?: return@withContext Result.Failed(R.string.look_failed)
            /*
             * ⚠️⚠️ **Il disegno viene dopo il colore e prima della geometria** (risposta D2a): i
             * cursori non devono ritingere l'inchiostro, e raddrizzare e tagliare lo devono spostare
             * con l'immagine. Il perché vive in testa a `Drawing.kt`.
             */
            // ⚠️ I caratteri del testo si caricano anche qui: il salvataggio può girare senza un editor aperto.
            Faces.load(context)
            drawn = Draw.onto(shaded, look.drawing, look.spin)
            warped = if (look.geo.idle) drawn else Warp.render(drawn, look.geo)
                ?: return@withContext Result.Failed(R.string.look_failed)
            done = if (look.crop.whole) warped else warped.cutTo(look.crop, strict = true)
            // ⚠️ Il ridimensionamento viene prima della firma, come in [redraw].
            small = resize?.let { Resize.apply(done, it) }
            val ridotta = small ?: done
            // ⚠️ La filigrana è l'ultima cosa che si disegna, sopra sviluppo, geometria e taglio:
            // vedi la testa di [Watermark].
            signed = mark?.let { Watermark.stamp(context, ridotta, it) }
            val finita = signed ?: ridotta
            // ⚠️ La trasparenza va su fondo bianco come in [redraw], con la stessa funzione.
            val piatta =
                if (kind == Bitmap.CompressFormat.JPEG) Convert.flatten(finita) else finita
            val written = runCatching {
                temp.outputStream().use { piatta.compress(kind, lookQuality(quality), it) }
            }.getOrDefault(false)
            if (piatta !== finita) piatta.recycle()
            if (!written) {
                temp.delete()
                return@withContext Result.Failed(R.string.edit_failed)
            }
        } catch (_: OutOfMemoryError) {
            temp.delete()
            return@withContext Result.Failed(R.string.edit_too_big)
        } finally {
            // ⚠️ Le mappe possono essere la stessa: una passata saltata consegna quella che ha
            // ricevuto, e riciclare due volte lo stesso bitmap è un errore che non si vede subito.
            if (signed !== small && signed !== done) signed?.recycle()
            if (small !== done) small?.recycle()
            if (done !== warped) done?.recycle()
            if (warped !== drawn) warped?.recycle()
            if (drawn !== shaded) drawn?.recycle()
            if (shaded !== posed) shaded?.recycle()
            if (posed !== full) posed?.recycle()
            full?.recycle()
        }

        carryExif(source, temp)
        if (!temp.renameTo(target)) {
            temp.delete()
            return@withContext Result.Failed(R.string.edit_failed)
        }
        FileTree.scan(context, listOfNotNull(source.absolutePath, target.absolutePath))
        delivered(context, Result.Done(target, lossless = false))
    }

    /** In che formato esce un salvataggio dell'editor completo, data la qualità scelta. */
    private fun lookFormat(name: String, quality: Quality): Bitmap.CompressFormat =
        if (quality == Quality.LOSSLESS) Bitmap.CompressFormat.PNG
        else format(name) ?: Bitmap.CompressFormat.JPEG

    /**
     * Dove si scrive: sopra l'originale se il formato resta quello, accanto se cambia.
     *
     * ⚠️ **Il confronto è sul formato e non sull'estensione**: `.jpg` e `.jpeg` sono lo stesso
     * formato, e il nome non deve cambiare per la grafia.
     * [beside] chiede un file nuovo anche dove il formato coincide (il tocco lungo su 'Salva').
     * Il nome lo sceglie [FileTree.freeName], la stessa funzione di ogni copia dell'app.
     * È `internal` perché il banco misuri, senza decodificare, che il tocco lungo scriva davvero
     * un file nuovo.
     */
    internal fun lookTarget(
        source: File,
        dir: File,
        kind: Bitmap.CompressFormat,
        beside: Boolean = false
    ): File = if (!beside && format(source.name) == kind) source
    else FileTree.freeName(dir, source.nameWithoutExtension + extensionOf(kind))

    /** Il suffisso di un formato, con il punto. */
    private fun extensionOf(kind: Bitmap.CompressFormat): String =
        if (kind == Bitmap.CompressFormat.PNG) ".png" else ".jpg"

    /**
     * Quanto si comprime, dato quello che l'utente ha scelto.
     *
     * Su PNG il numero non conta, perché il formato non perde niente: 'Massima' è un JPEG
     * spinto al limite, 'Senza perdita' un PNG.
     */
    private fun lookQuality(quality: Quality): Int = when (quality) {
        Quality.HIGH -> QUALITY
        Quality.MAX -> 100
        Quality.LOSSLESS -> 100
    }

    /**
     * La via senza perdita: si cambia il numero dell'orientamento e basta.
     *
     * ⚠️ **La copia si fa prima di toccare il tag**, o si modificherebbe un originale che si è
     * chiesto di lasciare com'è.
     */
    private suspend fun turnOnly(
        context: Context,
        source: File,
        dir: File,
        turns: Int,
        mirror: Boolean,
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
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, spun(now, turns, mirror).toString())
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
     * ⚠️⚠️ **Si scrive in un file provvisorio e si rinomina alla fine**: sovrascrivendo
     * direttamente, una compressione che fallisce a metà lascerebbe un file troncato al posto
     * dell'originale.
     * L'`OutOfMemoryError` si cattura e diventa 'troppo grande': un'immagine da cinquanta
     * megapixel è una mappa da duecento megabyte.
     */
    private suspend fun redraw(
        context: Context,
        uri: Uri,
        source: File,
        target: File,
        turns: Int,
        mirror: Boolean,
        crop: Crop,
        mark: Watermark.Plan?,
        resize: Resize.Plan?
    ): Result {
        val temp = File(target.parentFile, target.name + ".part")
        var full: Bitmap? = null
        var turned: Bitmap? = null
        var cut: Bitmap? = null
        var small: Bitmap? = null
        var signed: Bitmap? = null
        try {
            // ⚠️ `ImageSource.pixels` apre anche i formati che il sistema non apre (AVIF, SVG):
            // senza, un ritaglio su quei file risponderebbe 'troppo grande'. Lo zero vuol dire
            // 'grande quanto viene', perché qui si riscrive e non si mostra niente.
            full = ImageSource.pixels(context, uri, 0)
                ?: return Result.Failed(R.string.edit_too_big)

            turned = full.spunBy(turns, mirror, strict = true)
            if (turned !== full) {
                full.recycle()
                full = null
            }

            cut = turned.cutTo(crop, strict = true)
            /*
             * ⚠️⚠️ **Si ridimensiona prima di firmare**: la filigrana è larga una frazione del
             * lato dell'immagine che si scrive, e firmando prima sarebbe ricampionata, più
             * sgranata dell'anteprima delle impostazioni. Così si disegna una volta sola, alla
             * misura giusta.
             */
            small = resize?.let { Resize.apply(cut, it) }
            val ridotta = small ?: cut
            /*
             * ⚠️ **La filigrana è l'ultima cosa che si disegna**: è una firma sul risultato. Se
             * non c'è niente da scrivere (nessun file scelto, o un file che non si disegna)
             * risponde `null` e si salva l'immagine e basta, senza fallire per un logo.
             */
            signed = mark?.let { Watermark.stamp(context, ridotta, it) }
            val finita = signed ?: ridotta

            val kind = format(target.name) ?: Bitmap.CompressFormat.JPEG
            /*
             * ⚠️⚠️ **La trasparenza va su fondo bianco** (richiesta dell'utente, voce
             * `svg-modifica`): il JPEG scarta il canale alfa, e un pixel trasparente resterebbe
             * coi suoi valori di colore, in un PNG o in un SVG quasi sempre zero, cioè nero.
             * La funzione è quella di [Convert], non una seconda copia. `flatten` torna la stessa
             * immagine se non ha canale alfa, e per PNG e WebP non si chiama.
             */
            val piatta =
                if (kind == Bitmap.CompressFormat.JPEG) Convert.flatten(finita) else finita
            val written = runCatching {
                temp.outputStream().use { piatta.compress(kind, QUALITY, it) }
            }.getOrDefault(false)
            if (piatta !== finita) piatta.recycle()
            if (!written) {
                temp.delete()
                return Result.Failed(R.string.edit_failed)
            }
        } catch (_: OutOfMemoryError) {
            temp.delete()
            return Result.Failed(R.string.edit_too_big)
        } finally {
            // ⚠️ La firma può essere lo stesso bitmap su cui si è disegnato: come in [saveLook],
            // non si ricicla due volte.
            if (signed !== small && signed !== cut) signed?.recycle()
            if (small !== cut) small?.recycle()
            if (cut !== turned) cut?.recycle()
            if (turned !== full) turned?.recycle()
            full?.recycle()
        }

        carryExif(source, temp)
        // ⚠️ La rinomina è l'ultimo passo, e su Android sovrascrive: da qui il file buono c'è, e
        // quello provvisorio non esiste più.
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
     * ⚠️⚠️ **L'orientamento non si travasa**: i pixel scritti sono già girati, e il tag vecchio
     * ruoterebbe l'immagine una seconda volta. Nemmeno le misure: dopo un ritaglio i lati sono
     * altri.
     * Il travaso può non riuscire senza fermare niente: `ExifInterface` scrive solo JPEG, PNG e
     * WebP.
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
     * L'orientamento EXIF dopo uno specchio facoltativo e [turns] quarti di giro in senso
     * orario.
     *
     * ⚠️⚠️ **La tabella è derivata**: un orientamento EXIF è uno specchio facoltativo seguito da
     * una rotazione (1, 6, 3 e 8 senza specchio, 2, 7, 4 e 5 con), e girare la vista di 90 gradi
     * aggiunge 90 alla rotazione lasciando lo specchio. Da lì i due cicli qui sotto; la tabella di
     * 'ruota il file' che circola è un'altra cosa, e sbaglia sulle quattro con lo specchio.
     * ⚠️⚠️ **Lo specchio passa all'altro ciclo rovesciando l'indice**: con l'orientamento scritto
     * `R(i) ∘ M^s`, uno specchio davanti dà `M ∘ R(i) ∘ M^s = R(-i) ∘ M^(1-s)`, cioè l'altro ciclo
     * alla posizione meno i. È lo stesso conto di [Spin.then]; 'stessa posizione, altro ciclo'
     * sbaglierebbe su tutti gli orientamenti tranne i due dritti, senza dare errore.
     */
    internal fun spun(now: Int, turns: Int, mirror: Boolean): Int {
        val cycle = when (now) {
            in DIRECT -> DIRECT
            in MIRROR -> MIRROR
            else -> DIRECT
        }
        val at = cycle.indexOf(now).takeIf { it >= 0 } ?: 0
        if (!mirror) return cycle[(at + turns).mod(cycle.size)]
        val other = if (cycle === MIRROR) DIRECT else MIRROR
        return other[(turns - at).mod(other.size)]
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
     * I formati di partenza da cui esce un PNG invece di un JPEG.
     *
     * ⚠️ **Quelli in cui la trasparenza è normale e il contenuto è grafica**. Il criterio non è
     * 'può avere il canale alfa' (lo hanno anche AVIF e HEIF), ma quanto costa sbagliare: là il
     * fondo tolto è il senso del file, qui sarebbe un PNG enorme al posto di una fotografia.
     * Non è l'elenco di [Folder], che dice che cosa si mostra: unirli farebbe uscire un PNG
     * anche da un HEIC.
     */
    private val ALPHA_EXT = setOf("svg", "svgz", "gif", "bmp")

    /**
     * Quanto si comprime quando si deve ricomprimere.
     *
     * ⚠️ **95 e non 100**: a occhio non c'è differenza, e a 100 il file quasi raddoppia. Sul PNG
     * il numero non ha effetto.
     */
    private const val QUALITY = 95

    /**
     * I campi che seguono l'immagine: quelli che la pastiglia 'Info' mostra (vedi `Facts`), più
     * i satelliti.
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

/**
 * Questa mappa di pixel specchiata e girata di [turns] quarti di giro, oppure lei stessa.
 *
 * La usano l'anteprima mostrata, l'anteprima da salvare e il salvataggio, così fanno lo stesso
 * conto. Se la rotazione fallisce torna l'originale.
 * - ⚠️ **Torna `this` quando non c'è niente da girare**, e chi ricicla la copia confronta
 *   l'identità (`!==`), come il salvataggio: senza, riciclerebbe la mappa ancora in uso.
 * - ⚠️⚠️ **[strict] toglie la rete, e i due salvataggi lo accendono**: con la rete, un errore di
 *   memoria nel salvataggio scriverebbe il file dritto, anche sopra l'originale, e l'avviso
 *   direbbe 'salvato'. Senza, l'errore arriva al `catch` del salvataggio, che risponde 'troppo
 *   grande' e non tocca il file. La rete resta per le anteprime.
 */
internal fun Bitmap.spunBy(turns: Int, mirror: Boolean, strict: Boolean = false): Bitmap =
    if (turns.mod(4) == 0 && !mirror) this
    else netted(strict) {
        Bitmap.createBitmap(
            this, 0, 0, width, height,
            /*
             * ⚠️⚠️ **L'ordine delle due righe è la trasformazione**: `postScale` prima e
             * `postRotate` dopo vuol dire 'specchia, poi gira', l'ordine dichiarato in [save].
             * Al contrario, uno specchio dopo un quarto di giro ribalterebbe l'altro asse.
             * Lo specchio è sull'asse verticale (la x cambia segno); il ribaltamento in verticale
             * si ottiene da lui più mezzo giro, e lo compone chi chiama.
             */
            Matrix().apply {
                if (mirror) postScale(-1f, 1f)
                postRotate(90f * turns)
            },
            true
        )
    } ?: this

/**
 * [make], con la rete di [spunBy] e [cutTo] quando [strict] è spento: un errore torna `null`, e
 * chi chiama ripiega sulla mappa che aveva.
 */
private inline fun netted(strict: Boolean, make: () -> Bitmap): Bitmap? =
    if (strict) make() else runCatching(make).getOrNull()

/**
 * Questa mappa di pixel ritagliata sulle frazioni di [crop], oppure lei stessa.
 *
 * ⚠️ **Una funzione sola per l'anteprima e per il file salvato**: un ritaglio diverso di un pixel
 * fra i due non darebbe nessun errore, e si vedrebbe solo sul risultato.
 * - Si tronca (`toInt`) e si stringe dentro i bordi; il minimo di larghezza e altezza è uno,
 *   perché un ritaglio di zero pixel non è un'immagine, e l'origine arriva al più all'ultimo pixel.
 * - [strict] come in [spunBy]: con la rete, un salvataggio senza memoria scriverebbe l'immagine
 *   intera al posto del ritaglio.
 */
internal fun Bitmap.cutTo(crop: ImageEdit.Crop, strict: Boolean = false): Bitmap {
    if (crop.whole) return this
    return netted(strict) {
        val x = (crop.left * width).toInt().coerceIn(0, width - 1)
        val y = (crop.top * height).toInt().coerceIn(0, height - 1)
        val w = ((crop.right - crop.left) * width).toInt().coerceIn(1, width - x)
        val h = ((crop.bottom - crop.top) * height).toInt().coerceIn(1, height - y)
        Bitmap.createBitmap(this, x, y, w, h)
    } ?: this
}

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
     *
     * ⚠️⚠️ **DAI FORMATI CON LA TRASPARENZA ESCE UN PNG, dalla `1.34`, e prima era sempre un
     * JPEG**: richiesta dell'utente sugli SVG (giro della `1.31`, voce `svg-modifica`: *le
     * modifiche agli SVG devono salvare un PNG, non un JPG*). Ritagliare un disegno col fondo
     * tolto e riceverne un JPEG vuol dire perdere l'unica cosa per cui quel formato era stato
     * scelto, e la perdita è irreversibile.
     * ⚠️ **Ma dai formati FOTOGRAFICI esce ancora un JPEG** ([ALPHA_EXT] non li elenca), e non
     * è una dimenticanza: un PNG di ventiquattro megapixel sono decine di megabyte per una
     * fotografia che non ha niente di trasparente da salvare. Là la trasparenza, se c'è, viene
     * appiattita su fondo **bianco**: vedi la nota dentro [redraw].
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
     * ⚠️⚠️ **L'ORDINE È 'SPECCHIA, GIRA, TAGLIA', E NON È UNA CONVENZIONE FRA TANTE**: con
     * quello, ogni catena di riflessioni e rotazioni si riscrive in **un** solo specchio più
     * **una** sola rotazione (le otto trasformazioni del quadrato, che sono le stesse otto
     * dell'orientamento EXIF). Tenendo invece un elenco di gesti, salvare vorrebbe dire
     * rifarli uno per uno sui pixel, cioè decodificare e ricomprimere più volte.
     * ⚠️ **Il conto che lo rende possibile** è che uno specchio davanti a una rotazione la
     * rovescia: `M ∘ R(k) = R(-k) ∘ M`. Vive su `Spin.then`, in `EditorScreen.kt`, ed è quello
     * che compone i passi prima di arrivare qui.
     *
     * ⚠️ **`NonCancellable` come le altre operazioni sui file**: a metà scrittura una
     * cancellazione lascerebbe un file troncato dove prima c'era una fotografia.
     */
    suspend fun save(
        context: Context,
        uri: Uri,
        turns: Int,
        mirror: Boolean,
        crop: Crop,
        way: Way,
        backup: Boolean
    ): Result = withContext(Dispatchers.IO + NonCancellable) {
        val source = FileTree.fileOf(context, uri)
            ?: return@withContext Result.Failed(R.string.edit_no_file)
        val dir = source.parentFile ?: return@withContext Result.Failed(R.string.edit_no_file)
        if (turns == 0 && !mirror && crop.whole) {
            return@withContext Result.Failed(R.string.edit_nothing)
        }

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
        // ⚠️⚠️ **E VALE ANCHE PER UNO SPECCHIO, DALLA `2.02`**: l'orientamento EXIF ne porta
        // quattro con lo specchio (2, 4, 5, 7), quindi riflettere un JPEG non costa una
        // ricompressione più di quanto ne costi girarlo. Chi credesse il contrario toglierebbe
        // qualità a un gesto che oggi non ne toglie.
        if (jpeg && crop.whole) {
            return@withContext turnOnly(context, source, dir, turns, mirror, way)
        }

        val target = when (way) {
            Way.OVERWRITE ->
                if (canOverwrite(source.name)) source
                else return@withContext Result.Failed(R.string.edit_no_overwrite)
            Way.COPY -> FileTree.freeName(dir, outputName(source.name))
        }
        redraw(context, uri, source, target, turns, mirror, crop)
    }

    /**
     * Applica i valori dell'editor **completo** e scrive.
     *
     * ⚠️⚠️ **VIVE QUI E NON IN UN FILE SUO, ED È UNA SCELTA**: questo oggetto è la casa della
     * domanda *che cosa succede quando l'editor salva*, e le due strade (la posa e i valori)
     * condividono la copia di sicurezza, il travaso EXIF, il file provvisorio e il rinomina
     * finale. Scritte in due posti, la prima a cambiare sarebbe quella che nessuno guarda.
     *
     * ⚠️⚠️ **LA QUALITÀ DECIDE ANCHE IL FORMATO, e chi sceglie 'senza perdita' riceve un file
     * NUOVO**: un JPEG non puo essere senza perdita, quindi là esce un PNG, che ha un altro
     * nome e quindi si mette accanto invece di prendere il posto. Non è un ripiego: è l'unica
     * lettura onesta di quella scelta, e il nome diverso lo dice a chi guarda la cartella.
     *
     * ⚠️ **`NonCancellable` come tutto il resto**: una scrittura interrotta a metà lascerebbe un
     * file troncato al posto di una fotografia.
     */
    suspend fun saveLook(
        context: Context,
        uri: Uri,
        look: Look,
        quality: Quality,
        backup: Boolean
    ): Result = withContext(Dispatchers.IO + NonCancellable) {
        val source = FileTree.fileOf(context, uri)
            ?: return@withContext Result.Failed(R.string.edit_no_file)
        val dir = source.parentFile ?: return@withContext Result.Failed(R.string.edit_no_file)
        if (look.idle) return@withContext Result.Failed(R.string.edit_nothing)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return@withContext Result.Failed(R.string.look_failed)
        }

        val kind = lookFormat(source.name, quality)
        val target = lookTarget(source, dir, kind)
        // ⚠️ La copia di sicurezza si fa **solo** quando si sovrascrive, e prima di tutto: è la
        // stessa regola di [save], e la stessa ragione (chi l'ha accesa ha chiesto di non poter
        // perdere l'originale, quindi un fallimento ferma il salvataggio invece di procedere).
        if (target == source && backup && Bin.keep(context, source) == null) {
            return@withContext Result.Failed(R.string.edit_no_backup)
        }

        val temp = File(target.parentFile, target.name + ".part")
        var full: Bitmap? = null
        var shaded: Bitmap? = null
        var done: Bitmap? = null
        try {
            full = ImageSource.pixels(context, uri, 0)
                ?: return@withContext Result.Failed(R.string.edit_too_big)
            /*
             * ⚠️⚠️ **DUE PASSATE E NON UNA, DALLA `2.29`, E L'ORDINE È LA SPECIFICA**: prima il
             * colore, che gira sulla scheda grafica a tessere, e poi la geometria, che è una maglia
             * di triangoli su una tela normale. Il Dettaglio guarda i pixel vicini, quindi deve
             * leggerli **come sono nel file**: deformando prima, misurerebbe una nitidezza che il
             * ricampionamento ha appena ammorbidito.
             * ⚠️ **Ognuna delle due si salta quando non ha niente da fare**, ed è il caso comune:
             * chi raddrizza soltanto non paga una passata di shader su venti megapixel, e chi
             * sviluppa soltanto non paga il ricampionamento.
             */
            shaded = if (look.plain) full else AdjustRender.apply(full, look)
                ?: return@withContext Result.Failed(R.string.look_failed)
            done = if (look.geo.idle) shaded else Warp.render(shaded, look.geo)
                ?: return@withContext Result.Failed(R.string.look_failed)
            // ⚠️ La trasparenza va su fondo bianco come nell'altra strada, e con la stessa
            // funzione: il JPEG butta via il canale alfa, e i pixel trasparenti resterebbero
            // col loro colore, che quasi sempre è il nero.
            val piatta = if (kind == Bitmap.CompressFormat.JPEG) Convert.flatten(done) else done
            val written = runCatching {
                temp.outputStream().use { piatta.compress(kind, lookQuality(quality), it) }
            }.getOrDefault(false)
            if (piatta !== done) piatta.recycle()
            if (!written) {
                temp.delete()
                return@withContext Result.Failed(R.string.edit_failed)
            }
        } catch (_: OutOfMemoryError) {
            temp.delete()
            return@withContext Result.Failed(R.string.edit_too_big)
        } finally {
            // ⚠️ Le tre mappe possono essere la stessa: una passata saltata consegna quella che ha
            // ricevuto, e riciclare due volte lo stesso bitmap è un errore che non si vede finché
            // qualcuno non lo legge dopo.
            if (done !== shaded) done?.recycle()
            if (shaded !== full) shaded?.recycle()
            full?.recycle()
        }

        carryExif(source, temp)
        if (!temp.renameTo(target)) {
            temp.delete()
            return@withContext Result.Failed(R.string.edit_failed)
        }
        FileTree.scan(context, listOfNotNull(source.absolutePath, target.absolutePath))
        Result.Done(target, lossless = false)
    }

    /** In che formato esce un salvataggio dell'editor completo, data la qualità scelta. */
    private fun lookFormat(name: String, quality: Quality): Bitmap.CompressFormat =
        if (quality == Quality.LOSSLESS) Bitmap.CompressFormat.PNG
        else format(name) ?: Bitmap.CompressFormat.JPEG

    /**
     * Dove si scrive: sopra l'originale se il formato resta quello, accanto se cambia.
     *
     * ⚠️ **Il confronto è sul FORMATO e non sull'estensione**: `.jpg` e `.jpeg` sono lo stesso
     * formato, e un file che si chiama in un modo non deve cambiare nome solo perché l'altra
     * grafia era più comune.
     */
    private fun lookTarget(
        source: File,
        dir: File,
        kind: Bitmap.CompressFormat
    ): File = if (format(source.name) == kind) source
    else FileTree.freeName(dir, source.nameWithoutExtension + extensionOf(kind))

    /** Il suffisso di un formato, con il punto. */
    private fun extensionOf(kind: Bitmap.CompressFormat): String =
        if (kind == Bitmap.CompressFormat.PNG) ".png" else ".jpg"

    /**
     * Quanto si comprime, dato quello che l'utente ha scelto.
     *
     * ⚠️ **Su PNG il numero non conta**, ed è giusto così: quel formato non perde niente,
     * quindi 'Massima' e 'Senza perdita' non sono la stessa cosa detta due volte. La prima è un
     * JPEG spinto al limite, la seconda un file che non butta via un bit.
     */
    private fun lookQuality(quality: Quality): Int = when (quality) {
        Quality.HIGH -> QUALITY
        Quality.MAX -> 100
        Quality.LOSSLESS -> 100
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
        mirror: Boolean,
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
            full = ImageSource.pixels(context, uri, 0)
                ?: return Result.Failed(R.string.edit_too_big)

            turned = full.spunBy(turns, mirror)
            if (turned !== full) {
                full.recycle()
                full = null
            }

            cut = turned.cutTo(crop)

            val kind = format(target.name) ?: Bitmap.CompressFormat.JPEG
            /*
             * ⚠️⚠️ **LA TRASPARENZA VA SU FONDO BIANCO, e fino alla 1.33 QUI diventava
             * NERA**: il JPEG butta via il canale alfa e basta, quindi un pixel trasparente
             * resta coi suoi valori di colore, che in un PNG o in un SVG sono quasi sempre
             * zero, cioè nero. Non lo decideva il JPEG, lo decideva la sorte. 'Converti /
             * Esporta' dipingeva la tela di bianco **dalla `1.16`**, ma questa è un'altra
             * strada e la correzione non l'aveva mai vista: segnalato dall'utente sugli SVG
             * (giro della `1.31`, voce `svg-modifica`: *voglio che sia SEMPRE bianco*), e non
             * riguardava solo loro.
             * ⚠️ **La funzione è quella di [Convert] e non una seconda copia**: un secondo
             * `drawColor` scritto qui sarebbe la stessa scelta in due posti, cioè due posti
             * da cambiare il giorno che il colore diventa un'opzione.
             * ⚠️ **Si salta dove non serve**: `flatten` torna la stessa immagine se non ha
             * canale alfa, e per PNG e WebP non la si chiama nemmeno.
             */
            val piatta = if (kind == Bitmap.CompressFormat.JPEG) Convert.flatten(cut) else cut
            val written = runCatching {
                temp.outputStream().use { piatta.compress(kind, QUALITY, it) }
            }.getOrDefault(false)
            if (piatta !== cut) piatta.recycle()
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
     * L'orientamento EXIF dopo uno specchio facoltativo e [turns] quarti di giro in senso
     * orario.
     *
     * ⚠️⚠️ **LA TABELLA È DERIVATA, non ricordata**: un orientamento EXIF è uno specchio
     * facoltativo seguito da una rotazione (1 e 6 e 3 e 8 senza specchio, 2 e 7 e 4 e 5 con),
     * e girare la vista di 90 gradi aggiunge 90 alla rotazione lasciando lo specchio dov'è.
     * Da lì escono i due cicli qui sotto. Chi la copia da un forum prende quella di 'ruota il
     * file', che è un'altra cosa e sbaglia sulle quattro con lo specchio.
     * ⚠️⚠️ **E LO SPECCHIO PASSA ALL'ALTRO CICLO ROVESCIANDO L'INDICE, dalla `2.02`**: con
     * l'orientamento scritto come `R(i) ∘ M^s`, mettere uno specchio davanti dà
     * `M ∘ R(i) ∘ M^s = R(-i) ∘ M^(1-s)`, cioè l'altro ciclo alla posizione **meno** i. È lo
     * stesso conto di `Spin.then`, e chi lo scrivesse come 'stessa posizione, altro ciclo'
     * sbaglierebbe su tutti gli orientamenti tranne i due dritti, senza che niente dia errore.
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
     * ⚠️ **Sono quelli in cui la trasparenza è normale e il contenuto è grafica**: un SVG, un
     * `.svgz`, una GIF, un BMP. Il criterio non è 'può avere il canale alfa' (ce l'hanno anche
     * l'AVIF e l'HEIF), è **quanto costa sbagliare**: là il fondo tolto è il senso del file,
     * qui sarebbe un PNG enorme al posto di una fotografia.
     * ⚠️ **Non è l'elenco di [Folder]**: quello dice che cosa si mostra in una cartella,
     * questo che cosa si scrive uscendo. Metterli insieme farebbe uscire un PNG anche da un
     * HEIC.
     */
    private val ALPHA_EXT = setOf("svg", "svgz", "gif", "bmp")

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

/**
 * Questa mappa di pixel specchiata e girata di [turns] quarti di giro, oppure lei stessa.
 *
 * ⚠️⚠️ **NASCE PERCHÉ ERA SCRITTA TRE VOLTE** (censimento della UI del 2026-09-05):
 * nell'anteprima mostrata, nell'anteprima da salvare e nel salvataggio vero. E le tre copie
 * **non erano identiche**, che è il danno peggiore di una duplicazione: una proteggeva il conto
 * con un `runCatching` e le altre due no, quindi un errore di memoria su un'immagine grossa
 * arrivava dentro la composizione. La condizione di uscita era scritta in due modi (`turns == 0`
 * e `turns.mod(4) == 0`), oggi equivalenti solo perché lo stato è tenuto fra 0 e 3 da chi lo
 * cambia.
 * - **Quindi la rete c'è per tutti e tre**: se la rotazione fallisce torna l'originale, che è
 *   quello che l'unica copia protetta già faceva.
 * - ⚠️ **Torna `this` quando non c'è niente da girare**, e chi ricicla la copia se ne accorge
 *   confrontando l'identità (`!==`), come fa il salvataggio: senza quel confronto si
 *   riciclerebbe la mappa che si sta ancora usando.
 */
internal fun Bitmap.spunBy(turns: Int, mirror: Boolean): Bitmap =
    if (turns.mod(4) == 0 && !mirror) this
    else runCatching {
        Bitmap.createBitmap(
            this, 0, 0, width, height,
            /*
             * ⚠️⚠️ **L'ORDINE DELLE DUE RIGHE È LA TRASFORMAZIONE, e scambiarle dà un'altra
             * cosa**: `postScale` prima e `postRotate` dopo vuol dire 'specchia, poi gira',
             * che è l'ordine dichiarato in [save] e quello con cui i passi si compongono.
             * Al contrario, uno specchio dopo un quarto di giro ribalta l'altro asse.
             * ⚠️ **Lo specchio è sull'asse VERTICALE** (la x cambia segno), cioè quello che
             * scambia destra e sinistra: il verticale si ottiene da lui più mezzo giro, e a
             * comporlo è chi chiama.
             */
            Matrix().apply {
                if (mirror) postScale(-1f, 1f)
                postRotate(90f * turns)
            },
            true
        )
    }.getOrDefault(this)

/**
 * Questa mappa di pixel ritagliata sulle frazioni di [crop], oppure lei stessa.
 *
 * ⚠️⚠️ **NASCE PERCHÉ ERA SCRITTA DUE VOLTE, e il commento chiedeva di tenerle uguali A MANO**
 * (censimento della UI del 2026-09-05): il KDoc dell'anteprima dichiarava *gli arrotondamenti
 * sono gli STESSI di `ImageEdit.redraw`, e non per caso*, cioè affidava un vincolo a chi legge,
 * che è la definizione del difetto. Il danno sarebbe stato futuro e silenzioso: un ritaglio
 * diverso di un pixel fra anteprima e file salvato non dà nessun errore, e si vede solo sul
 * risultato.
 * - ⚠️ **Il conto è quello di prima, carattere per carattere**: si tronca (`toInt`) e si stringe
 *   dentro i bordi, il minimo di larghezza e altezza è **uno** perché un ritaglio di zero pixel
 *   non è un'immagine, e l'origine può arrivare al più all'ultimo pixel.
 */
internal fun Bitmap.cutTo(crop: ImageEdit.Crop): Bitmap {
    if (crop.whole) return this
    return runCatching {
        val x = (crop.left * width).toInt().coerceIn(0, width - 1)
        val y = (crop.top * height).toInt().coerceIn(0, height - 1)
        val w = ((crop.right - crop.left) * width).toInt().coerceIn(1, width - x)
        val h = ((crop.bottom - crop.top) * height).toInt().coerceIn(1, height - y)
        Bitmap.createBitmap(this, x, y, w, h)
    }.getOrDefault(this)
}

package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Build
import android.os.CancellationSignal
import android.util.Size
import androidx.annotation.RequiresApi
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import android.net.Uri as AndroidUri
import coil3.Uri
import coil3.Extras
import coil3.asImage
import coil3.getExtra
import coil3.decode.DataSource
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.fetch.SourceFetchResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.allowHardware
import coil3.size.pxOrElse
import coil3.toAndroidUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okio.Buffer
import java.io.File

/**
 * Le miniature della griglia, e perché si chiedono al sistema.
 *
 * ⚠️ **Un'immagine da 30 megapixel non si decodifica per mostrarla a 512 pixel**: anche
 * campionando, il decodificatore legge l'immagine intera, e lo farebbe per ogni riquadro che
 * scorre. Il sistema ha già una miniatura, o se la fa con molto meno:
 * - `ContentResolver.loadThumbnail` chiede al provider la misura voluta (`EXTRA_SIZE`),
 *   ridimensiona se serve e applica l'orientamento EXIF per conto suo;
 * - `ThumbnailUtils.createImageThumbnail`, per i `file://`, prova la miniatura incorporata in
 *   HEIF e AVIF, poi quella EXIF, e solo alla fine decodifica il file intero.
 *
 * ⚠️ **Una miniatura sotto la metà del possibile si rifiuta** e passa alla decodifica normale:
 * una miniatura EXIF da 160x120 su un riquadro da 512 si vede ingrandita tre volte (segnalazione
 * dell'utente). Il metro è su `tooSmall`. Il prezzo: su una cartella di fotocamera il primo
 * scorrimento decodifica invece di leggere la miniatura incorporata.
 *
 * Il visualizzatore non usa niente di tutto questo: decodifica con `ImageSource` a piena
 * risoluzione. Le due cose si incontrano solo nella miniatura mostrata mentre l'immagine vera si
 * decodifica.
 */
object Thumbs {

    /**
     * Il caricatore delle miniature.
     *
     * ⚠️ **La cache in memoria resta quella predefinita di Coil**, già proporzionata alla memoria
     * dell'app: è la stessa memoria che `ImageSource.pixelBudget` misura per decidere se
     * campionare l'immagine grande, quindi gonfiarla toglierebbe qualità al visualizzatore.
     * Niente cache su disco: i file sono già sul telefono.
     */
    fun loader(context: PlatformContext): ImageLoader = build(context, warming = false)

    /**
     * Il caricatore con cui 'Genera miniature' prepara la collezione.
     *
     * Generare lascia tutto quello che resta: la miniatura che il sistema salva per ogni riga del
     * MediaStore (nella cartella `.thumbnails` del provider), e in [AvifCache] quella di un AVIF,
     * il fotogramma scelto per un video ([ClipFrames]) e ogni miniatura che passa dalla decodifica
     * normale (risposta `disco` dell'utente a `d-mini-disco`; vedi [KeepingDecoderFactory]).
     * ⚠️ **Niente cache in memoria**: quello che si genera non si guarda, e toglierebbe spazio alle
     * miniature della griglia e all'immagine grande (vedi [loader]). Si costruisce per una
     * generazione e si spegne alla fine (`shutdown`).
     */
    internal fun warmer(context: PlatformContext): ImageLoader = build(context, warming = true)

    /**
     * I due caricatori, che differiscono soltanto per la cache in memoria: una funzione sola,
     * perché i componenti siano gli stessi e nello stesso ordine.
     */
    private fun build(context: PlatformContext, warming: Boolean): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(SystemThumbnailFactory())
            // ⚠️ Subito dopo il sistema il ripiego dei video: quando la miniatura di un video manca
            // o è nera, sceglie un fotogramma più avanti ([ClipFrames]). È prima dell'AVIF, che per
            // riconoscere un file ne legge la testa, mentre questo guarda solo l'indirizzo.
            add(ClipFrameFactory())
            // ⚠️ Poi il disco: quello che la decodifica normale e l'AVIF hanno già fatto una volta.
            // È dopo il sistema perché trovare il file costa una domanda al MediaStore, e la
            // maggior parte delle miniature la dà il sistema.
            add(DiskThumbnailFactory())
            // ⚠️ L'AVIF: su questi file il telefono si rifiuta ([Avif]), e così anche la decodifica
            // normale di Coil. Vale per la griglia e per le copertine. È dopo il sistema, che per
            // un AVIF si tira indietro da sé, o si decodificherebbero in casa anche i formati per
            // cui il telefono ha la miniatura pronta.
            add(AvifThumbnailFactory())
            // Prima di tutti i decodificatori quello che tiene su disco ([KeepingDecoderFactory]).
            add(KeepingDecoderFactory())
            // ⚠️ Quello degli SVG è un `Decoder`: entra quando i byte sono già letti. Coil tiene
            // separati i due elenchi, quindi conta solo che sia prima del decodificatore
            // predefinito, che di un SVG non sa niente.
            add(SvgThumbnailFactory())
        }
        .apply { if (warming) memoryCache(null) }
        .diskCache(null)
        .build()

    /**
     * La misura di ogni miniatura, sempre la stessa.
     *
     * ⚠️ **La chiave della cache in memoria contiene la misura richiesta**: con la misura del
     * riquadro, la stessa immagine avrebbe una chiave nella griglia e un'altra nel visualizzatore,
     * e una nuova a ogni rotazione. E 512 è la misura che il MediaStore usa per le sue
     * (`MINI_KIND` è 512x384), quindi il provider non deve ridimensionare.
     * Su un riquadro più largo di 512 px (un tablet) la miniatura si vede morbida: compromesso
     * accettato dall'utente per la velocità.
     */
    const val PX = 512

    /**
     * Se le miniature devono vivere nella memoria grafica: l'impostazione [Settings.gpuThumbs]
     * resa leggibile a un componente di Coil.
     *
     * ⚠️ **Una variabile globale perché un `Fetcher` di Coil nasce dentro il caricatore**, fuori da
     * ogni composizione, e non vede le preferenze. La scrive solo `AivApp` in `ViewerActivity`;
     * `@Volatile` perché la legge un thread di I/O di Coil.
     * Cambiarla vale per le miniature nuove: quelle già in cache restano come sono.
     */
    @Volatile
    var inGpu: Boolean = false

    /**
     * La richiesta di una miniatura, una sola definizione per la griglia e per l'anteprima del
     * visualizzatore: due costruzioni separate divergerebbero sulla misura, cioè sulla chiave.
     *
     * ⚠️ **`allowHardware` si dichiara sempre, anche spento**: il valore di serie di Coil è acceso,
     * e la decodifica di ripiego darebbe bitmap in memoria grafica mentre quelle del sistema
     * restano software.
     */
    fun request(context: Context, uri: AndroidUri): ImageRequest =
        ImageRequest.Builder(context).data(uri).size(PX).allowHardware(inGpu)
            .apply { extras.set(KEPT, uri) }
            .build()

    /**
     * L'indirizzo di una miniatura, trasmesso dalla richiesta fino a chi la tiene su disco.
     *
     * ⚠️ **Un decodificatore non sa da quale file vengono i suoi byte**, e la cache su disco ha
     * bisogno dell'indirizzo come chiave: gli extra di una richiesta arrivano fino alle [Options].
     * Non entrano nella chiave della cache in memoria.
     * Lo mette solo [request], e fa da cancello: una richiesta senza, cioè che non chiede una
     * miniatura, non legge e non scrive niente su disco.
     */
    internal val KEPT = Extras.Key<AndroidUri?>(null)

    /**
     * Le chiavi di cache delle miniature già viste, per indirizzo.
     *
     * ⚠️ **Toglie il fotogramma vuoto, cioè il lampeggio**: in Coil `AsyncImagePainter.onRemembered`
     * lancia una coroutine, e lo stato di partenza non disegna niente. Ogni composable con una
     * miniatura che entra in scena ha quindi il primo fotogramma vuoto, anche con l'immagine già
     * in memoria, e a ogni cambio di pagina al centro entra un'anteprima nuova.
     * Si tiene la chiave e non l'immagine: i pixel restano nel budget di Coil, e tenere i bitmap
     * sarebbe una seconda cache che toglie memoria all'immagine grande.
     */
    private val keys = LinkedHashMap<String, MemoryCache.Key>(16, 0.75f, true)

    /** Quante chiavi si ricordano. Costano una stringa l'una: il tetto serve a non crescere. */
    private const val KEYS = 64

    /**
     * Gli indirizzi il cui contenuto è **cambiato** da quando l'app ne ha visto la miniatura.
     *
     * ⚠️ **La miniatura del sistema non ha la data nella chiave** (voce `mini-cestino`): il
     * provider tiene le miniature per riga, quindi un'immagine riscritta sopra il proprio
     * indirizzo può farsi servire la miniatura di prima, e [forget] su quella cache non ha presa.
     * [AvifCache] mette la data nella propria chiave; qui la chiave la compone il provider.
     * Quindi per un indirizzo dichiarato riscritto la strada di sistema si salta una volta, e la
     * miniatura la fa la decodifica normale dal file vero. Stesso tetto di [KEYS].
     */
    private val stale = LinkedHashSet<String>()

    /** Registra dove Coil ha messo la miniatura di [uri], appena l'ha messa. */
    @Synchronized
    fun note(uri: AndroidUri, key: MemoryCache.Key?) {
        if (key == null) return
        keys[uri.toString()] = key
        while (keys.size > KEYS) keys.remove(keys.keys.first())
    }

    /**
     * La miniatura di [uri] se è **già** in memoria, letta senza sospendere.
     *
     * È la lettura sincrona che riempie il primo fotogramma. Restituisce `null` la prima volta che
     * si vede un'immagine.
     */
    @Synchronized
    fun cached(context: Context, uri: AndroidUri): coil3.Image? {
        val key = keys[uri.toString()] ?: return null
        return SingletonImageLoader.get(context).memoryCache?.get(key)?.image
    }

    /**
     * Dimentica la miniatura di [uri]: il file dietro quell'indirizzo è cambiato.
     *
     * ⚠️ **La chiave è l'indirizzo, non il contenuto**: sovrascrivendo un'immagine l'indirizzo
     * resta identico, e senza questa chiamata la griglia mostrerebbe la miniatura di prima.
     * La miniatura del sistema non si tocca: l'indirizzo si segna in [stale].
     * Si cercano tutte le chiavi di Coil per quell'indirizzo, non solo quella in [keys], che ha un
     * tetto: la chiave di Coil per un indirizzo è `uri.toString()` (`UriKeyer`), con la misura
     * negli extra, quindi il confronto prende tutte le misure dello stesso file. Si filtra prima e
     * si rimuove dopo, per non modificare l'insieme mentre lo si scorre.
     * ⚠️ **Dimentica anche la misura e il peso del file** ([Pixels], [sizeOf]; rilievi B14 e C20):
     * questo è il punto in cui l'app dichiara un file riscritto, dalla scansione, dal cestino e
     * dai salvataggi del visualizzatore.
     */
    @Synchronized
    fun forget(context: Context, uri: AndroidUri) {
        val at = uri.toString()
        AvifCache.forget(context, uri)
        Pixels.forget(uri)
        forgetSize(uri)
        stale += at
        while (stale.size > KEYS) stale.remove(stale.first())
        val cache = SingletonImageLoader.get(context).memoryCache ?: return
        keys.remove(at)?.let { cache.remove(it) }
        cache.keys.filter { it.key == at }.forEach { cache.remove(it) }
    }

    /**
     * Se l'indirizzo è stato dichiarato **riscritto** da [forget], e in quel caso lo consuma.
     *
     * ⚠️ **Consuma il segno**: se restasse, quell'indirizzo non userebbe mai più la miniatura del
     * sistema. Una nuova riscrittura lo segna di nuovo.
     */
    @Synchronized
    fun rewritten(uri: AndroidUri): Boolean = stale.remove(uri.toString())

    /**
     * Se l'indirizzo è segnato come riscritto, **senza consumare il segno**.
     *
     * Serve alla generazione, che non mostra niente e non deve consumare il segno destinato alla
     * griglia (vedi [stale]).
     */
    @Synchronized
    fun isStale(uri: AndroidUri): Boolean = uri.toString() in stale

    /**
     * Prepara la miniatura di [uri] dove resta, cioè nella cache del sistema o in quella degli AVIF.
     *
     * ⚠️ **La richiesta è quella della griglia** ([request]), quindi la stessa miniatura che la
     * griglia chiederà dopo; cambiano il caricatore ([warmer]) e la memoria grafica, spenta perché
     * il risultato non si disegna.
     * ⚠️ **Un indirizzo appena riscritto si salta** ([isStale]): il fetcher di sistema consumerebbe
     * il segno, e la griglia tornerebbe alla miniatura vecchia.
     * Un file che non si legge non ferma niente: [ImageLoader.execute] risponde con un errore.
     */
    internal suspend fun warm(context: Context, uri: AndroidUri, loader: ImageLoader) {
        if (isStale(uri)) return
        loader.execute(request(context, uri).newBuilder().allowHardware(false).build())
    }

    /**
     * Svuota tutto quello che Coil tiene in memoria, e le chiavi che lo indirizzavano.
     *
     * La cache intera e non le sole miniature: le chiavi delle immagini grandi e delle miniature
     * non si distinguono da fuori. Le chiavi si svuotano con lei, o [cached] le cercherebbe a
     * vuoto.
     */
    @Synchronized
    fun forgetAll(context: Context) {
        keys.clear()
        SingletonImageLoader.get(context).memoryCache?.clear()
    }
}

/**
 * Il lato da chiedere quando la misura del riquadro non è ancora nota.
 *
 * ⚠️ Serve un numero vero: `Dimension.Undefined` capita quando la richiesta parte prima che il
 * riquadro sia misurato, e `Size(0, 0)` farebbe dividere per zero dentro `loadThumbnail`. 384 è
 * il lato corto tipico delle miniature di sistema.
 * È a livello di file perché un membro privato di un `object` non si vede dalle classi del file.
 */
private const val FALLBACK_PX = 384

/**
 * Il bitmap spostato in memoria grafica, se la richiesta lo chiede e il telefono lo concede.
 *
 * ⚠️ **`allowHardware` da solo non basta**: lo legge il decodificatore di Coil, mentre queste
 * miniature arrivano già decodificate da `loadThumbnail`, da libavif o dal disegnatore SVG, e
 * sono software.
 * `copy` può restituire `null` quando la memoria grafica è piena: allora si tiene il bitmap
 * software. L'originale non si ricicla a mano, per non rischiare di disegnare un bitmap già
 * liberato: lo prende il raccoglitore.
 */
private fun Bitmap.inGraphics(options: Options): Bitmap {
    if (!options.allowHardware || config == Bitmap.Config.HARDWARE) return this
    return runCatching { copy(Bitmap.Config.HARDWARE, false) }.getOrNull() ?: this
}

/**
 * Chiede al sistema la miniatura, e si tira indietro quando non ce l'ha.
 *
 * ⚠️ **Restituire `null` da `fetch()` passa la mano al componente successivo** (contratto di
 * `Fetcher`): la richiesta prosegue nel `ContentUriFetcher` di Coil e nella decodifica normale.
 * I componenti dichiarati qui vengono prima di quelli predefiniti (`RealImageLoader` accoda i
 * suoi).
 * L'annotazione è sulla classe, non una soppressione sulla riga: le funzioni di sistema esistono
 * da Android 10, il controllo è nella fabbrica, e così lint verifica che ci sia.
 */
@RequiresApi(Build.VERSION_CODES.Q)
private class SystemThumbnailFetcher(
    private val data: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val wanted = Size(
            options.size.width.pxOrElse { FALLBACK_PX },
            options.size.height.pxOrElse { FALLBACK_PX }
        )
        val uri = data.toAndroidUri()

        /*
         * ⚠️ **Un indirizzo dichiarato riscritto non si chiede al sistema**, che servirebbe la
         * miniatura di prima: tirandosi indietro, la richiesta arriva alla decodifica normale del
         * file vero (vedi [Thumbs.stale]). I video no: la decodifica normale non li sa leggere, e
         * l'editor lavora solo sulle immagini.
         */
        if (!Videos.isVideo(uri) && Thumbs.rewritten(uri)) return@withContext null

        /*
         * Tre classi di file non si chiedono al sistema ([declines], che chiede il tipo al
         * MediaStore una volta sola; rilievo D22, presidiato da `TipoMiniaturaTest`):
         * - ⚠️ **i file con la trasparenza** (voci `esporta-png` e `mini-alfa`): le miniature del
         *   MediaStore sono JPEG, senza canale alfa, e il trasparente arriva nero dentro la
         *   miniatura. Decodificando in casa, sotto si vede il fondo del riquadro come per gli
         *   SVG. Si decide dall'intestazione del file e non dal formato ([seeThrough]), quindi la
         *   maggior parte dei PNG, che l'alfa non l'hanno, resta sulla via veloce;
         * - ⚠️ **i BMP**, che il generatore del telefono restituisce a metà: le righe sono nel file
         *   dal basso verso l'alto, e chi si ferma a metà ottiene la metà inferiore;
         * - ⚠️ **gli HEIC e gli HEIF**: la miniatura del MediaStore spesso ignora il profilo ICC, e
         *   i colori cambierebbero fra la miniatura e l'immagine aperta.
         * La decodifica normale legge il file intero, e [KeepingDecoderFactory] ne tiene il
         * risultato su disco dalla prima volta.
         */
        if (declines(options.context, uri)) return@withContext null

        // ⚠️ L'annullamento è vero: scorrendo in fretta Coil annulla le richieste dei riquadri
        // usciti dallo schermo, e senza questo aggancio il sistema continuerebbe a generare
        // miniature che nessuno guarderà, togliendo I/O a quelle che servono.
        val signal = CancellationSignal()
        val hook = coroutineContext[Job]?.invokeOnCompletion { signal.cancel() }
        try {
            val bitmap = try {
                when (uri.scheme?.lowercase()) {
                    // Vale anche per i video: il MediaStore genera le loro miniature come per
                    // le immagini.
                    "content" -> options.context.contentResolver.loadThumbnail(uri, wanted, signal)
                    // ⚠️ Per i `file://` le due funzioni sono diverse: `createImageThumbnail` su
                    // un video va in errore, e la richiesta finirebbe nel ripiego dei video con
                    // un lavoro in più. Il tipo lo dice l'estensione, come in `Folder.fromDisk`.
                    "file" -> uri.path?.let {
                        val file = File(it)
                        if (Videos.isVideo(uri)) {
                            ThumbnailUtils.createVideoThumbnail(file, wanted, signal)
                        } else {
                            ThumbnailUtils.createImageThumbnail(file, wanted, signal)
                        }
                    }
                    else -> null
                }
            } catch (e: Throwable) {
                // ⚠️ Non un `runCatching`, che ingoierebbe l'annullamento e manderebbe la
                // richiesta alla decodifica di un'immagine che nessuno guarda più: se la
                // coroutine è stata annullata, `ensureActive` lo dice e si esce.
                if (e is CancellationException) throw e
                coroutineContext.ensureActive()
                null
            } ?: return@withContext null

            /*
             * ⚠️ **Una miniatura troppo piccola si rifiuta** (vedi la nota in testa al file):
             * `createImageThumbnail` prova prima la miniatura EXIF, che in un JPEG di fotocamera
             * c'è quasi sempre. `null` passa la mano alla decodifica normale, campionata alla
             * misura chiesta.
             */
            if (!Videos.isVideo(uri) && tooSmall(options.context, uri, bitmap, wanted)) {
                return@withContext null
            }

            /*
             * La miniatura nera di un video si rifiuta: passa al ripiego dei video
             * ([ClipFrameFactory]), che sceglie un fotogramma più avanti (vedi [ClipFrames]). Una
             * miniatura che manca ci arriva già dall'uscita qui sopra.
             */
            if (Videos.isVideo(uri) && ClipFrames.isBlack(bitmap)) return@withContext null

            ImageFetchResult(
                image = bitmap.inGraphics(options).asImage(),
                // Campionata, perché è una riduzione dell'immagine.
                isSampled = true,
                dataSource = DataSource.DISK
            )
        } finally {
            hook?.dispose()
        }
    }

}

/**
 * Se la miniatura che il sistema ha dato è troppo piccola per il riquadro che la chiede.
 *
 * ⚠️ **Il confronto è col possibile, non col voluto**: di un'icona da 96 pixel non si fa una
 * miniatura da 512, e rifiutarla costerebbe due letture per la stessa immagine. Il metro è il
 * minimo fra la misura chiesta e quella del file, letta dall'intestazione e ricordata da
 * [Pixels.longSideOf]. Senza la misura del file si tiene quello che c'è.
 * La soglia è la metà: sotto, l'ingrandimento è di due volte o più. Passano le misure che i
 * provider servono (384 e 512 di lato), cade la miniatura EXIF da 160. Si scrive come prodotto
 * (`x * 2 < y`), senza divisioni né virgola mobile.
 */
private suspend fun tooSmall(
    context: Context,
    uri: AndroidUri,
    bitmap: Bitmap,
    wanted: Size
): Boolean {
    val file = Pixels.longSideOf(context, uri) ?: return false
    val possibile = minOf(maxOf(wanted.width, wanted.height), file)
    return maxOf(bitmap.width, bitmap.height) * 2 < possibile
}

/**
 * Chi decide se il fetcher qui sopra è utilizzabile.
 *
 * ⚠️ **È fuori dalla classe annotata**: l'annotazione vale anche per le classi innestate, e
 * registrare una fabbrica da Android 10 in su sarebbe un errore di lint. Fuori, la fabbrica
 * esiste sempre e si tira indietro dove le funzioni di sistema non ci sono.
 */
private class SystemThumbnailFactory : Fetcher.Factory<Uri> {
    override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
        // Le funzioni di sistema esistono da Android 10, e `minSdk` è 28: sotto vale la
        // decodifica normale di Coil.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return when (data.scheme?.lowercase()) {
            "content", "file" -> SystemThumbnailFetcher(data, options)
            else -> null
        }
    }
}

/**
 * La miniatura di un video scelta da AIV, quando quella del sistema è nera o manca.
 *
 * Arriva qui solo quello che il fetcher di sistema ha rifiutato; quale fotogramma si sceglie lo
 * dice [ClipFrames]. Prima si guarda il disco: scegliere un fotogramma vuol dire decodificarne
 * fino a quattro, e il risultato si tiene in [AvifCache], con la data del file nella chiave.
 * Vale anche sotto Android 10, dove il fetcher di sistema non esiste. Se non trova niente passa
 * la mano, e la richiesta finisce senza miniatura.
 */
private class ClipFrameFetcher(
    private val data: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val uri = data.toAndroidUri()
        val wanted = Size(
            options.size.width.pxOrElse { FALLBACK_PX },
            options.size.height.pxOrElse { FALLBACK_PX }
        )
        val box = maxOf(wanted.width, wanted.height)
        val bitmap = AvifCache.read(options.context, uri, box) ?: run {
            val job = coroutineContext[Job]
            val scelto = ClipFrames.pick(options.context, uri, wanted) { job?.isActive != false }
                ?: return@withContext null
            coroutineContext.ensureActive()
            AvifCache.write(options.context, uri, box, scelto)
            scelto
        }
        ImageFetchResult(
            image = bitmap.inGraphics(options).asImage(),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }
}

/**
 * Chi decide se il ripiego dei video serve: per un video sempre, per il resto mai. Lo dice
 * l'indirizzo ([Videos.isVideo]), quindi `create` non tocca il disco.
 */
private class ClipFrameFactory : Fetcher.Factory<Uri> {
    override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
        val scheme = data.scheme?.lowercase()
        if (scheme != "content" && scheme != "file") return null
        return if (Videos.isVideo(data.toAndroidUri())) ClipFrameFetcher(data, options) else null
    }
}

/**
 * La miniatura già fatta una volta, ritrovata su disco (risposta `disco` dell'utente a
 * `d-mini-disco`): quello che il sistema non tiene lo salva [KeepingDecoderFactory], e lo ritrova
 * questo, insieme alle miniature degli AVIF. Una sola domanda al MediaStore per richiesta.
 * I video no: il loro fotogramma lo cerca [ClipFrameFetcher], che viene prima. Se non trova
 * niente passa la mano.
 */
private class DiskThumbnailFetcher(
    private val data: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val pronta = AvifCache.read(options.context, data.toAndroidUri(), boxOf(options))
            ?: return@withContext null
        ImageFetchResult(
            image = pronta.inGraphics(options).asImage(),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }
}

/**
 * Chi decide se il lettore del disco serve: per una miniatura chiesta da [Thumbs.request] che non
 * sia un video. Lo dice la richiesta, quindi `create` non tocca il disco.
 */
private class DiskThumbnailFactory : Fetcher.Factory<Uri> {
    override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
        if (options.getExtra(Thumbs.KEPT) == null) return null
        val scheme = data.scheme?.lowercase()
        if (scheme != "content" && scheme != "file") return null
        return if (Videos.isVideo(data.toAndroidUri())) null else DiskThumbnailFetcher(data, options)
    }
}

/**
 * Il decodificatore che tiene su disco quello che la decodifica normale ha fatto.
 *
 * ⚠️ **Non decodifica niente da sé**: chiede al registro il decodificatore successivo al suo
 * posto (`newDecoder`), lo fa lavorare e ne salva il risultato in [AvifCache]. Così la cache vale
 * solo per i file che il sistema non serve.
 * È un `Decoder` e non un intercettore perché è la sola strada che dice chi ha fatto la
 * miniatura: da fuori una miniatura del sistema, una di un AVIF e una decodificata sono tutte
 * `DataSource.DISK`.
 * L'indirizzo arriva dagli extra ([Thumbs.KEPT]); senza, si tira indietro.
 * ⚠️ **Se il suo posto nel registro non si trova si tira indietro**: cercando dal primo
 * ritroverebbe se stesso, e si chiamerebbe all'infinito.
 */
private class KeepingDecoderFactory : Decoder.Factory {
    override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? {
        val uri = options.getExtra(Thumbs.KEPT) ?: return null
        val registry = imageLoader.components
        val me = registry.decoderFactories.indexOf(this)
        if (me < 0) return null
        val next = registry.newDecoder(result, options, imageLoader, me + 1)?.first ?: return null
        return KeepingDecoder(next, uri, options)
    }
}

private class KeepingDecoder(
    private val next: Decoder,
    private val uri: AndroidUri,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult? {
        val result = next.decode() ?: return null
        val image = result.image as? coil3.BitmapImage ?: return result
        withContext(Dispatchers.IO) {
            // ⚠️ Una bitmap in memoria grafica si copia prima di comprimerla: i suoi pixel non
            // sono leggibili da qui, e `compress` su di lei dipende dalla versione di Android.
            val bitmap = image.bitmap.let {
                if (it.config == Bitmap.Config.HARDWARE) it.copy(Bitmap.Config.ARGB_8888, false) else it
            } ?: return@withContext
            AvifCache.write(options.context, uri, boxOf(options), bitmap)
        }
        return result
    }
}

/**
 * Il lato con cui una miniatura si ritrova su disco: una funzione sola per chi scrive e per chi
 * legge, perché il numero è nella chiave, e due conti diversi scriverebbero miniature che
 * nessuno ritrova.
 */
private fun boxOf(options: Options): Int = maxOf(
    options.size.width.pxOrElse { FALLBACK_PX },
    options.size.height.pxOrElse { FALLBACK_PX }
)

/**
 * La miniatura di un AVIF, fatta da libavif invece che dal telefono, e tenuta su disco.
 *
 * ⚠️ **La prima volta costa quanto aprire l'immagine intera**: un AVIF non contiene una
 * miniatura pronta, e per 512 pixel si decodificano tutti i megapixel. Si paga una volta per
 * file, perché il risultato finisce in [AvifCache]. I piani decodificati restano nella memoria
 * nativa di libavif: in Java arriva già il riquadro piccolo.
 * È un `Fetcher` e non un `Decoder` perché riceve l'indirizzo, che è la chiave della cache su
 * disco, prima di leggere il file.
 */
@RequiresApi(Build.VERSION_CODES.Q)
private class AvifThumbnailFetcher(
    private val data: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        if (!Avif.ready) return@withContext null
        val uri = data.toAndroidUri()
        val box = boxOf(options)

        // Il disco l'ha già guardato [DiskThumbnailFetcher], che viene prima: qui la miniatura
        // non c'era.
        /*
         * ⚠️ Si legge il file intero, perché libavif vuole tutto il flusso in un buffer diretto,
         * ma solo dopo aver riconosciuto il formato dai primi byte, o in memoria finirebbe ogni
         * JPEG della cartella.
         */
        val bytes = runCatching {
            options.context.contentResolver.openInputStream(uri)?.use { stream ->
                val head = ByteArray(Avif.SNIFF)
                val got = stream.readFully(head, Avif.SNIFF)
                if (!Avif.looksLike(head.copyOf(got))) null else head.copyOf(got) + stream.readBytes()
            }
        }.getOrNull() ?: return@withContext null
        coroutineContext.ensureActive()

        val bitmap = Avif.thumbnail(bytes, box) ?: return@withContext null
        AvifCache.write(options.context, uri, box, bitmap)
        ImageFetchResult(
            image = bitmap.inGraphics(options).asImage(),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }
}

/**
 * Chi decide se il caricatore AVIF serve.
 *
 * Non guarda i byte: `create` non sospende, e aprire un file qui sarebbe un accesso al disco
 * durante la composizione. Il riconoscimento è dentro `fetch`, su IO, che restituisce `null` se il
 * file non è un AVIF. È dopo la fabbrica di sistema nel registro.
 */
private class AvifThumbnailFactory : Fetcher.Factory<Uri> {
    override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return when (data.scheme?.lowercase()) {
            "content", "file" -> AvifThumbnailFetcher(data, options)
            else -> null
        }
    }
}

/**
 * La miniatura di un SVG, disegnata da androidsvg.
 *
 * Costa poco: un documento di qualche kilobyte si disegna direttamente nella misura giusta. Il
 * tetto ai pixel non si passa (vedi [Svg.FREE]): con un lato lungo di 512 non scatterebbe mai.
 */
private class SvgThumbnailDecoder(
    private val source: coil3.decode.ImageSource,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult? = withContext(Dispatchers.IO) {
        val box = maxOf(
            options.size.width.pxOrElse { FALLBACK_PX },
            options.size.height.pxOrElse { FALLBACK_PX }
        )
        coroutineContext.ensureActive()
        // Un flusso e non un array di byte, quindi nessun tetto di lettura: il parser legge il
        // documento mentre scorre.
        val drawn = Svg.render(source.source().inputStream(), box) ?: return@withContext null
        DecodeResult(
            image = drawn.bitmap.inGraphics(options).asImage(),
            // ⚠️ Sempre `true`, non il `sampled` del disegno: a Coil serve sapere se è l'immagine
            // intera, e una miniatura non lo è mai. Dichiarata intera, Coil la riuserebbe per
            // una richiesta più grande, stirata a schermo pieno.
            isSampled = true
        )
    }
}

/**
 * Chi decide se il disegnatore SVG serve.
 *
 * ⚠️ **Si guardano i byte senza consumarli**: `peek()` dà un secondo lettore sullo stesso buffer,
 * e il decodificatore predefinito trova la sorgente intatta quando questo si tira indietro.
 * ⚠️ **Un sì sbagliato costa la miniatura**: il parser chiude la sorgente anche quando fallisce
 * (un PNG col manifesto C2PA contiene un'icona SVG in testa), quindi la domanda può sbagliare
 * per difetto e mai per eccesso (vedi [Svg.looksLike]).
 * Il tipo dichiarato conta come il contenuto, come nel `SvgDecoder.Factory` di Coil: un
 * `content://` può dichiarare `image/svg+xml` con una dichiarazione XML lunga in testa.
 */
private class SvgThumbnailFactory : Decoder.Factory {
    override fun create(
        result: SourceFetchResult,
        options: Options,
        imageLoader: ImageLoader
    ): Decoder? {
        if (!Svg.ready) return null
        if (result.mimeType != Svg.MIME && !looksLike(result)) return null
        return SvgThumbnailDecoder(result.source, options)
    }

    private fun looksLike(result: SourceFetchResult): Boolean = runCatching {
        val head = Buffer()
        result.source.source().peek().read(head, Svg.SNIFF.toLong())
        Svg.looksLike(head.readByteArray())
    }.getOrDefault(false)
}

/**
 * Se il file può avere trasparenza, e quindi la sua miniatura non si chiede al sistema.
 *
 * Il tipo prima e il nome dopo: su un `content://` del MediaStore il percorso è un numero, su un
 * `file://` il tipo non lo dichiara nessuno. Gira su IO, dentro `fetch`, e non nella fabbrica.
 * Gli SVG sono nell'elenco per completezza: li prende il loro decodificatore, più avanti.
 */
private fun seeThrough(context: Context, uri: AndroidUri, mime: String?): Boolean {
    val forma = when {
        mime != null -> SEE_THROUGH_MIME[mime]
        else -> {
            val name = uri.lastPathSegment?.lowercase() ?: return false
            SEE_THROUGH_EXT.entries.firstOrNull { name.endsWith(".${it.key}") }?.value
        }
    } ?: return false
    /*
     * Solo PNG e WebP si interrogano: in un GIF la trasparenza è in un blocco prima di ogni
     * fotogramma, e per saperlo bisognerebbe scandire il file, quindi un GIF va sempre sulla via
     * lenta. Il BMP non è nell'elenco: va sulla via lenta per un'altra ragione ([isBmp]).
     */
    return when (forma) {
        Alpha.SEMPRE -> true
        Alpha.SE_DICHIARATA -> declaresAlpha(context, uri)
    }
}

/**
 * Se il file dichiara un canale alfa nella propria intestazione.
 *
 * ⚠️ **Nel dubbio risponde sì**: un falso sì costa una decodifica in più, un falso no ridà il
 * fondo nero. Ogni intestazione illeggibile, troncata o inattesa vale come 'forse'.
 * Legge [SNIFF] byte: in un PNG i chunk della trasparenza sono prima di `IDAT` per specifica,
 * quindi la finestra li contiene, salvo profili di colore enormi in testa ('forse').
 * Non usa `BitmapFactory` con `inJustDecodeBounds`: su un PNG a palette con `tRNS` il suo
 * `hasAlpha` dipende dalla versione di Android.
 */
private fun declaresAlpha(context: Context, uri: AndroidUri): Boolean {
    val testa = runCatching {
        when (uri.scheme?.lowercase()) {
            "content" -> context.contentResolver.openInputStream(uri)
            "file" -> uri.path?.let { File(it).inputStream() }
            else -> null
        }?.use { flusso ->
            val buffer = ByteArray(SNIFF)
            var letti = 0
            while (letti < SNIFF) {
                val n = flusso.read(buffer, letti, SNIFF - letti)
                if (n <= 0) break
                letti += n
            }
            buffer.copyOf(letti)
        }
    }.getOrNull() ?: return true
    if (testa.size < 32) return true

    // ── PNG: il tipo di colore è al byte 25, e la palette può avere un `tRNS` ──
    if (testa.size >= 26 && PNG_MAGIC.indices.all { testa[it] == PNG_MAGIC[it] }) {
        return when (testa[25].toInt()) {
            // 4 = grigio più alfa, 6 = RGBA: l'alfa è nel formato stesso.
            4, 6 -> true
            // 3 = palette: l'alfa esiste solo se c'è un chunk `tRNS` prima di `IDAT`.
            3 -> fourcc(testa, "tRNS") ?: true
            // 0 = grigio, 2 = RGB: nessun canale alfa possibile.
            0, 2 -> false
            else -> true
        }
    }

    // ── WebP: lo dice il flag del chunk VP8X, e le altre due forme per costruzione ──
    if (String(testa, 0, 4, Charsets.US_ASCII) == "RIFF" &&
        String(testa, 8, 4, Charsets.US_ASCII) == "WEBP"
    ) {
        return when (String(testa, 12, 4, Charsets.US_ASCII)) {
            // Il bit 0x10 del byte dei flag è ALPHA, per specifica.
            "VP8X" -> testa.size > 20 && (testa[20].toInt() and 0x10) != 0
            /*
             * Il senza perdita dichiara l'alfa nel proprio flusso di bit: dopo la firma `0x2F` al
             * byte 20 vengono 14 bit di larghezza, 14 di altezza e `alpha_is_used`, cioè il bit 28
             * dei quattro byte dal 21, letti little-endian. Misurato su file veri.
             */
            "VP8L" -> if (testa.size >= 25) {
                val v = (testa[21].toInt() and 0xFF) or
                    ((testa[22].toInt() and 0xFF) shl 8) or
                    ((testa[23].toInt() and 0xFF) shl 16) or
                    ((testa[24].toInt() and 0xFF) shl 24)
                ((v ushr 28) and 1) == 1
            } else {
                true
            }
            // Il lossy semplice non ha alfa: senza un VP8X davanti, non ce n'è.
            "VP8 " -> false
            else -> true
        }
    }
    return true
}

/**
 * Se la sigla di quattro lettere compare prima di `IDAT`, e `null` se la finestra finisce
 * prima di poterlo dire. Vedi [declaresAlpha]: il `null` è il 'forse'.
 */
private fun fourcc(testa: ByteArray, cerca: String): Boolean? {
    var pos = 8
    while (pos + 8 <= testa.size) {
        val length = ((testa[pos].toInt() and 0xFF) shl 24) or
            ((testa[pos + 1].toInt() and 0xFF) shl 16) or
            ((testa[pos + 2].toInt() and 0xFF) shl 8) or
            (testa[pos + 3].toInt() and 0xFF)
        if (length < 0) return null
        val tipo = String(testa, pos + 4, 4, Charsets.US_ASCII)
        if (tipo == cerca) return true
        if (tipo == "IDAT") return false
        // 4 di lunghezza, 4 di sigla, il corpo, 4 di CRC.
        pos += 12 + length
    }
    return null
}

/**
 * Se questo file è un BMP, che il sistema riduce a metà (vedi [SystemThumbnailFetcher]).
 *
 * Il tipo prima e il nome dopo, come [seeThrough]. Non guarda i byte: la domanda è a chi chiedere
 * la miniatura, e un falso sì costa solo una decodifica in casa.
 */
private fun isBmp(uri: AndroidUri, mime: String?): Boolean {
    if (mime != null) return mime == "image/bmp"
    return uri.lastPathSegment?.lowercase()?.endsWith(".bmp") == true
}

/**
 * Se questo file è un HEIC o un HEIF, per tipo o per estensione come [isBmp]. Il tipo può
 * mancare su `file://` e su certi provider: l'estensione copre quel caso.
 */
private fun isHeic(uri: AndroidUri, mime: String?): Boolean {
    if (mime != null) {
        return mime == "image/heic" || mime == "image/heif" || mime == "image/heic-sequence" ||
            mime == "image/heif-sequence"
    }
    val name = uri.lastPathSegment?.lowercase() ?: return false
    return name.endsWith(".heic") || name.endsWith(".heif")
}

/**
 * Se la miniatura di [uri] va fatta in casa invece di chiederla al sistema: trasparenza vera
 * ([seeThrough]), BMP ([isBmp]) o HEIC ([isHeic]), in quest'ordine.
 *
 * ⚠️ **Il tipo si chiede al provider una volta sola** per le tre prove (rilievo D22, presidiato
 * da `TipoMiniaturaTest`): è una domanda al processo del MediaStore per ogni riquadro che entra
 * in vista. Su un `file://` le tre prove guardano il nome.
 */
internal fun declines(context: Context, uri: AndroidUri): Boolean {
    val mime = if (uri.scheme?.lowercase() == "content") {
        runCatching { context.contentResolver.getType(uri) }.getOrNull()?.lowercase()
    } else {
        null
    }
    return seeThrough(context, uri, mime) || isBmp(uri, mime) || isHeic(uri, mime)
}

/** Quanto si legge in testa a un file per sapere se dichiara l'alfa. Vedi [declaresAlpha]. */
private const val SNIFF = 4096

private val PNG_MAGIC = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
)

/** Come si sa se un formato può avere l'alfa: sempre, o solo se il file lo dichiara. */
private enum class Alpha { SEMPRE, SE_DICHIARATA }

/**
 * I tipi che possono avere un canale alfa, e come lo si accerta. Vedi [seeThrough]. Il BMP non
 * c'è, per scelta dell'utente: lo tratta [isBmp].
 */
private val SEE_THROUGH_MIME = mapOf(
    "image/png" to Alpha.SE_DICHIARATA,
    "image/webp" to Alpha.SE_DICHIARATA,
    "image/gif" to Alpha.SEMPRE,
    "image/svg+xml" to Alpha.SEMPRE
)

/** Le estensioni degli stessi formati, per quando il tipo non lo dichiara nessuno. */
private val SEE_THROUGH_EXT = mapOf(
    "png" to Alpha.SE_DICHIARATA,
    "webp" to Alpha.SE_DICHIARATA,
    "gif" to Alpha.SEMPRE,
    "svg" to Alpha.SEMPRE,
    "svgz" to Alpha.SEMPRE
)

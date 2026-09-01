package io.github.roccobot.aiv

import android.content.Context
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
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.size.pxOrElse
import coil3.toAndroidUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Le miniature della griglia, e perché NON passano dal decodificatore normale.
 *
 * ⚠️⚠️ **IL PUNTO È CHE UNA FOTO DA 30 MEGAPIXEL NON SI APRE PER MOSTRARLA GRANDE
 * 324 PIXEL.** Anche campionando, il decodificatore deve comunque leggere e
 * ricostruire l'immagine intera, e su una cartella di centinaia di scatti quel costo
 * si paga per ogni riquadro che scorre. Il sistema invece **ha già** una miniatura, o
 * sa farsela con molto meno, ed è quella che si chiede qui.
 *
 * **Che cosa fa il sistema, letto dal sorgente AOSP e non ricordato:**
 * - `ContentResolver.loadThumbnail` chiede al provider un `openTypedAssetFile` con
 *   dentro la misura voluta (`EXTRA_SIZE`), *'to help it avoid downloading or
 *   generating heavy resources'*, poi ridimensiona ancora se il provider ha risposto
 *   con qualcosa di grosso, e **applica l'orientamento EXIF** per conto suo, con un
 *   canale laterale, perché le miniature EXIF non portano i flag di rotazione.
 * - `ThumbnailUtils.createImageThumbnail`, che serve il caso `file://`, prova in
 *   ordine: la miniatura **incorporata** in HEIF e AVIF
 *   (`MediaMetadataRetriever.getThumbnailImageAtIndex`), poi la miniatura **EXIF**
 *   (`getThumbnailBytes`), e solo se non ce n'è nessuna decodifica il file intero.
 *
 * ⚠️ **Il compromesso di qualità sta tutto lì, ed è quello che l'utente ha accettato**
 * (*anche a costo di qualche compromesso di qualità*): una miniatura EXIF è spesso
 * 160x120, quindi su un riquadro da 324 px si vede morbida. Sul percorso normale della
 * galleria (`content://`) il provider serve misure sue, più grandi, e la cosa non si
 * nota; il ripiego `file://` esiste solo per le cartelle che il MediaStore non conosce.
 *
 * ⚠️⚠️ **NEL VISUALIZZATORE SINGOLO NON SI USA NIENTE DI TUTTO QUESTO**: là comanda la
 * qualità, e la decodifica resta quella di `ImageSource`, a piena risoluzione e con il
 * campionamento deciso dalla memoria disponibile. L'unico punto in cui le due cose si
 * toccano è la miniatura mostrata **mentre** la fotografia vera si decodifica.
 */
object Thumbs {

    /**
     * Il caricatore delle miniature.
     *
     * ⚠️ **La cache in memoria resta quella PREDEFINITA di Coil, e non è pigrizia**:
     * è già proporzionata alla memoria dell'app e più piccola sui dispositivi a poca
     * RAM. Gonfiarla si paga nel posto sbagliato, perché è **la stessa memoria** che
     * `ImageSource.pixelBudget` misura per decidere se campionare la fotografia
     * grande: più miniature tenute in caldo, meno budget al visualizzatore, cioè
     * qualità tolta proprio dove l'utente la vuole intera.
     * ⚠️ **Niente cache su DISCO**, e qui è una dichiarazione più che un effetto: i
     * file sono già sul telefono, quindi copiarli in una cartella di cache sarebbe
     * spazio speso per niente. Oggi Coil non lo farebbe comunque (scrive su disco solo
     * il caricatore di rete), ma un domani basterebbe un componente nuovo.
     */
    fun loader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components { add(SystemThumbnailFactory()) }
        .diskCache(null)
        .build()

    /**
     * La misura di OGNI miniatura, sempre la stessa, e le due ragioni sono tutte e due
     * pratiche.
     *
     * ⚠️⚠️ **PRIMA: la chiave della cache in memoria contiene la MISURA richiesta.**
     * Lasciando che ogni riquadro chieda la propria (Coil misura il posto in cui
     * l'immagine andrà), la stessa fotografia avrebbe una chiave nella griglia e
     * un'altra nel visualizzatore, e soprattutto **una nuova a ogni rotazione**, perché
     * con le colonne adattive il riquadro cambia larghezza: si rigenererebbe tutto,
     * proprio nel momento in cui l'utente ha già le miniature in mano.
     * ⚠️⚠️ **POI: 512 è la misura che il MediaStore usa per le sue** (`MINI_KIND` è
     * 512x384), quindi chiedere esattamente quella evita al provider un
     * ridimensionamento in più.
     * ⚠️ Il costo dichiarato: su un riquadro più largo di 512 px (un tablet) la
     * miniatura si vede morbida. È il compromesso che l'utente ha accettato per la
     * velocità, e riguarda la sola griglia.
     */
    const val PX = 512

    /**
     * La richiesta di una miniatura, **una sola definizione per i due posti che la
     * chiedono**: la griglia e l'anteprima del visualizzatore. Due costruzioni separate
     * divergerebbero sulla misura, cioè sulla chiave, cioè sull'unica cosa che fa la
     * differenza fra un colpo in cache e una rigenerazione.
     */
    fun request(context: Context, uri: AndroidUri): ImageRequest =
        ImageRequest.Builder(context).data(uri).size(PX).build()

    /**
     * Le chiavi di cache delle miniature già viste, per indirizzo.
     *
     * ⚠️⚠️ **ESISTE PER TOGLIERE IL FOTOGRAMMA VUOTO, che era il lampeggio rimasto**
     * (segnalazione dell'utente, 2026-08-29: *l'immagine lampeggia ancora*, alla terza
     * versione che ci prova). Letto sul bytecode di Coil 3.6.0 e non supposto:
     * `AsyncImagePainter.onRemembered` chiama `launchJob`, che **lancia una coroutine**, e
     * lo stato di partenza non disegna niente. Quindi ogni volta che un composable con una
     * miniatura **nasce**, il suo primo fotogramma è vuoto, anche quando l'immagine è già
     * in memoria. A tutto schermo quel fotogramma è un lampo.
     * ⚠️⚠️ **E nasce a ogni cambio di pagina**: la vicina che scivola dentro vive in
     * `ImageCanvas`, che al passaggio a 'sto caricando' viene smontato, e al centro nasce
     * un'anteprima nuova con la **stessa** immagine. Un elemento esce di scena e un altro rifà il
     * suo lavoro da capo: è la stessa lezione della riga dei dettagli nella `0.42`, questa
     * volta sull'immagine.
     * ⚠️ Si tiene la **chiave** e non l'immagine, e la differenza è tutta: i pixel restano
     * quelli di Coil, che li conta nel suo budget, mentre qui c'è una stringa. Tenere i
     * bitmap sarebbe una seconda cache in concorrenza con la prima, cioè memoria tolta
     * alla fotografia grande.
     */
    private val keys = LinkedHashMap<String, MemoryCache.Key>(16, 0.75f, true)

    /** Quante chiavi si ricordano. Costano una stringa l'una: il tetto serve a non crescere. */
    private const val KEYS = 64

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
     * ⚠️ È la lettura sincrona che riempie il primo fotogramma. Torna `null` la prima volta
     * che si vede un'immagine, ed è giusto così: là non c'è niente da mostrare comunque.
     */
    @Synchronized
    fun cached(context: Context, uri: AndroidUri): coil3.Image? {
        val key = keys[uri.toString()] ?: return null
        return SingletonImageLoader.get(context).memoryCache?.get(key)?.image
    }

    /**
     * Butta la miniatura di [uri]: il file dietro quell'indirizzo è cambiato.
     *
     * ⚠️⚠️ **SERVE PERCHÉ LA CHIAVE È L'INDIRIZZO, non il contenuto**: sovrascrivendo una
     * fotografia dall'editor interno l'indirizzo resta identico, quindi senza questa
     * chiamata la griglia continuerebbe a mostrare la miniatura di **prima** del ritaglio,
     * cioè un'immagine che sul telefono non esiste più.
     * ⚠️ **Non tocca la miniatura del SISTEMA**, che è un'altra cache e non è nostra: quella
     * la rifà il MediaScanner, ed è la ragione per cui [ImageEdit] chiama `FileTree.scan`
     * dopo ogni scrittura. Qui si toglie la sola copia che teniamo noi.
     */
    @Synchronized
    fun forget(context: Context, uri: AndroidUri) {
        val key = keys.remove(uri.toString()) ?: return
        SingletonImageLoader.get(context).memoryCache?.remove(key)
    }
}

/**
 * Il lato da chiedere quando la misura del riquadro non è ancora nota.
 *
 * ⚠️ Serve un numero vero e non zero: `Dimension.Undefined` capita quando la richiesta
 * parte prima che il riquadro sia stato misurato, e `Size(0, 0)` farebbe dividere per
 * zero dentro `loadThumbnail`. 384 è la misura tipica del lato corto delle miniature di
 * sistema, quindi non chiede niente di straordinario.
 * ⚠️ Sta a livello di FILE e non dentro l'oggetto: un membro privato di un `object` non
 * si vede da una classe del file, nemmeno accanto.
 */
private const val FALLBACK_PX = 384

/**
 * Chiede al sistema la miniatura, e si toglie di mezzo quando non ce l'ha.
 *
 * ⚠️⚠️ **RESTITUIRE `null` DA `fetch()` PASSA LA MANO AL COMPONENTE SUCCESSIVO**, ed è
 * scritto nel contratto di `Fetcher`: *'or return null to delegate to the next Factory
 * in the component registry'*. È il motivo per cui questo file non contiene nessun
 * ripiego scritto a mano: se il sistema non sa fare la miniatura, la richiesta prosegue
 * dal `ContentUriFetcher` di Coil e finisce nella decodifica normale.
 * ⚠️ E i componenti dichiarati qui vengono **prima** di quelli predefiniti: il registro
 * di Coil parte da quello dell'utente e ci accoda i suoi (`RealImageLoader`), quindi
 * l'ordine è garantito dalla costruzione e non da una convenzione.
 *
 * ⚠️ L'annotazione sulla CLASSE e non una soppressione sulla riga: le due funzioni di
 * sistema esistono da Android 10, il controllo vive nella fabbrica, e dichiararlo qui è
 * quello che permette a lint di verificare che chi costruisce questa classe l'abbia
 * fatto. Una `@SuppressLint` avrebbe spento il controllo invece di soddisfarlo.
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

        // ⚠️⚠️ L'ANNULLAMENTO È VERO, ed è metà della fluidità: scorrendo in fretta Coil
        // annulla le richieste dei riquadri usciti dallo schermo, e senza questo aggancio
        // il sistema continuerebbe a generare miniature che nessuno guarderà più,
        // rubando I/O a quelle che si stanno per vedere.
        val signal = CancellationSignal()
        val hook = coroutineContext[Job]?.invokeOnCompletion { signal.cancel() }
        try {
            val bitmap = try {
                when (uri.scheme?.lowercase()) {
                    // ⚠️ Vale anche per i FILMATI, e senza una riga in più: `loadThumbnail`
                    // chiede al provider la miniatura di quell'indirizzo, e il MediaStore
                    // le genera per i video come per le fotografie. È la ragione per cui
                    // il pezzo 1 dei video (`0.83`) non ha dovuto scrivere un secondo
                    // caricatore.
                    "content" -> options.context.contentResolver.loadThumbnail(uri, wanted, signal)
                    // ⚠️⚠️ **Qui invece le due funzioni sono DIVERSE**, e sbagliarle non dà
                    // una miniatura brutta: `createImageThumbnail` su un filmato solleva, e
                    // la richiesta finirebbe nella decodifica normale, che di un `mp4` non
                    // sa che farsene. Il ramo `file://` è quello delle cartelle che il
                    // MediaStore non conosce, dove non c'è nessuna tabella a dire il tipo:
                    // lo dice l'estensione, come in `Folder.fromDisk`.
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
                // ⚠️⚠️ UN `runCatching` QUI SAREBBE UN DIFETTO, non una scorciatoia:
                // ingoierebbe anche l'annullamento, e allora la richiesta proseguirebbe
                // verso la decodifica normale di una foto che nessuno sta più guardando.
                // Quando il segnale è scattato perché la coroutine è stata annullata,
                // `ensureActive` lo dice e si esce di qui.
                if (e is CancellationException) throw e
                coroutineContext.ensureActive()
                null
            } ?: return@withContext null

            ImageFetchResult(
                image = bitmap.asImage(),
                // Dichiarata campionata perché lo è: non è la fotografia, è una sua
                // riduzione, e chi legge questo esito deve saperlo.
                isSampled = true,
                dataSource = DataSource.DISK
            )
        } finally {
            hook?.dispose()
        }
    }

}

/**
 * Chi decide se il fetcher qui sopra è utilizzabile.
 *
 * ⚠️ **Sta FUORI dalla classe annotata, e non è una questione di gusto**: annotare una
 * classe annota anche quelle innestate, quindi una fabbrica dentro il fetcher sarebbe
 * lei stessa da Android 10 in su, e costruirla per registrarla diventerebbe un errore
 * di lint su un dispositivo qualunque. Fuori, la fabbrica esiste sempre e si sfila da
 * sé dove le funzioni di sistema non ci sono.
 */
private class SystemThumbnailFactory : Fetcher.Factory<Uri> {
    override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
        // Le due funzioni di sistema esistono da Android 10, e `minSdk` è 28: sotto,
        // la fabbrica si sfila e vale la decodifica normale di Coil.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return when (data.scheme?.lowercase()) {
            "content", "file" -> SystemThumbnailFetcher(data, options)
            else -> null
        }
    }
}

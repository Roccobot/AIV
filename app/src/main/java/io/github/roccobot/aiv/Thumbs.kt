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
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.fetch.SourceFetchResult
import coil3.request.ImageRequest
import coil3.request.Options
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
        .components {
            add(SystemThumbnailFactory())
            // ⚠️⚠️ **IL SECONDO SERVE PERCHÉ IL PRIMO SU UN AVIF NON HA NIENTE DA DARE**:
            // `loadThumbnail` e `createImageThumbnail` chiedono al telefono, e il telefono su
            // questi file si rifiuta (vedi [Avif]). Da lì la richiesta proseguirebbe verso la
            // decodifica normale di Coil, che usa `BitmapFactory` e si rifiuta uguale: era il
            // 'non mostra nemmeno le miniature' del riscontro. Questo la intercetta prima, e
            // vale sia per la griglia sia per le copertine delle cartelle.
            // ⚠️ **L'ORDINE CONTA**: al sistema si chiede per primo, e per un AVIF si tira
            // indietro da sé tornando `null`. Invertirli vorrebbe dire decodificare in casa
            // anche i formati per cui il telefono ha già la miniatura pronta.
            add(AvifThumbnailFactory())
            // ⚠️⚠️ **IL TERZO È UN `Decoder` E NON UN `Fetcher`, e la differenza dice a che
            // punto della catena entra**: i due sopra prendono la richiesta **prima** che il
            // file venga letto, perché la miniatura può arrivare da un'altra parte; questo
            // entra **dopo**, quando i byte ci sono già e resta solo da capirci un'immagine,
            // che è esattamente il caso di un SVG. ⚠️ Fra i due elenchi non c'è ordine da
            // rispettare, perché Coil li tiene separati: conta solo che sia dichiarato qui,
            // e quindi prima del decodificatore predefinito che di un SVG non sa niente.
            add(SvgThumbnailFactory())
        }
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

        /*
         * ⚠️⚠️ **I FORMATI CON LA TRASPARENZA NON PASSANO DA QUI, dalla 1.36, e la
         * ragione è il NERO** (riscontro dell'utente, 2026-09-02, voce `esporta-png`: *la
         * trasparenza è sempre scacchiera nel visualizzatore, ma nelle anteprime è variabile:
         * per gli SVG è mostrata come #EFEEEA, mentre per le PNG come nero. Voglio che ci sia
         * coerenza fra tutte le rappresentazioni di trasparenza nelle miniature*).
         * ⚠⚠ **La causa: le miniature del MediaStore sono JPEG**, e un JPEG non ha canale
         * alfa: dove l'immagine era trasparente restano i byte del colore, che per un PNG
         * appena decodificato sono zero, cioè nero. Il nero non è dipinto da noi e non si può
         * ridipingere dopo: arriva **cotto** dentro la miniatura.
         * ⚠️ **Quindi si sceglie DA CHE PARTE stare**: chiedendo al sistema si ha una
         * miniatura veloce col fondo nero, decodificando in casa si ha l'alfa vera e sotto si
         * vede il fondo del riquadro, che è `surfaceVariant`, cioè lo stesso `#EFEEEA` che lui
         * vede sugli SVG (e la sua controparte scura, senza scriverla qui). Il ripiego di Coil
         * fa la seconda cosa da sé: basta tirarsi indietro.
         * ⚠️ **IL COSTO SI PAGA, e va detto invece di scoprirlo**: la miniatura di un PNG
         * grande adesso passa dal file intero (campionato), non dalla copia già pronta del
         * provider. Su una cartella di PNG da molti megapixel lo scorrimento può farsi sentire
         * la prima volta; la cache in memoria di Coil copre le successive. È lo stesso baratto
         * dell'AVIF, che qui accanto ha perfino una cache su disco.
         * ⚠️ **I filmati e i JPEG non cambiano strada**: quelli non portano trasparenza, e
         * per loro la miniatura di sistema resta la cosa giusta.
         */
        if (seeThrough(options.context, uri)) return@withContext null

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

/**
 * La miniatura di un AVIF, fatta da libavif invece che dal telefono, e tenuta su disco.
 *
 * ⚠️ **Costa quanto aprire l'immagine intera la PRIMA volta**, e va detto invece di lasciarlo
 * scoprire: un AVIF non porta dentro una miniatura già pronta come fa un JPEG, quindi per un
 * riquadro da 512 pixel bisogna decodificare i 24 megapixel e poi ridurli. Dalla `1.31` si
 * paga **una volta sola per file**, perché il risultato finisce in [AvifCache]: prima si
 * pagava a ogni apertura della cartella, ed è la segnalazione che ha fatto nascere quella
 * cache.
 * ⚠️ I piani decodificati stanno nella memoria **nativa** di libavif, non nell'heap dell'app:
 * quello che arriva in Java è già il riquadro piccolo.
 *
 * ⚠️⚠️ **È UN `Fetcher` E NON UN `Decoder`, ed è cambiato nella 1.31**: un decodificatore
 * riceve i **byte** e non sa da quale file vengono, mentre la cache su disco ha bisogno
 * dell'indirizzo come chiave. Un `Fetcher.Factory<Uri>` lo riceve come parametro, quindi la
 * cache si può guardare **prima** di leggere venticinque megabyte, che è il punto di averla.
 */
@RequiresApi(Build.VERSION_CODES.Q)
private class AvifThumbnailFetcher(
    private val data: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        if (!Avif.ready) return@withContext null
        val uri = data.toAndroidUri()
        val box = maxOf(
            options.size.width.pxOrElse { FALLBACK_PX },
            options.size.height.pxOrElse { FALLBACK_PX }
        )

        // ⚠️⚠️ **PRIMA IL DISCO, e questa è tutta la correzione**: sta prima di aprire il
        // file, perché aprirlo vorrebbe dire portare in memoria venticinque megabyte per poi
        // scoprire che la miniatura c'era già.
        AvifCache.read(options.context, uri, box)?.let { pronta ->
            return@withContext ImageFetchResult(
                image = pronta.asImage(),
                isSampled = true,
                dataSource = DataSource.DISK
            )
        }

        /*
         * ⚠️ **Si legge il file INTERO e non a pezzi**, perché libavif vuole tutto il flusso
         * in un buffer diretto: non esiste un modo di decodificare un AVIF leggendone solo la
         * testa. ⚠️ E si legge **solo** dopo aver riconosciuto il formato dai primi byte, o
         * questa riga porterebbe in memoria ogni JPEG della cartella.
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
            image = bitmap.asImage(),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }
}

/**
 * Chi decide se il caricatore AVIF serve.
 *
 * ⚠️ **Non guarda i byte, e non potrebbe**: `create` non sospende, e aprire un file qui
 * vorrebbe dire un accesso al disco durante la composizione. Il riconoscimento sta dentro
 * `fetch`, che gira su IO e torna `null` quando il file non è un AVIF: da lì la richiesta
 * prosegue verso la decodifica normale di Coil, come vuole il contratto di `Fetcher`.
 * ⚠️ **Sta DOPO la fabbrica di sistema** nel registro, e l'ordine è la ragione per cui le due
 * convivono: al sistema si chiede per primo, e per un AVIF si tira indietro da sé.
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
 * ⚠️ **Costa poco, al contrario di quella di un AVIF, e per questo non ha una cache su
 * disco**: là bisogna decodificare ventiquattro megapixel per ricavarne 512, qui si disegna
 * un documento di qualche kilobyte **direttamente** nella misura giusta. Non c'è nessun
 * lavoro grande da buttare via, quindi non c'è niente da conservare.
 * ⚠️ **Il tetto ai pixel non si passa**, e il perché sta su [Svg.FREE]: con un lato lungo di
 * 512 non potrebbe scattare mai.
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
        // ⚠️ **Uno stream e non un array di byte**, ed è la ragione per cui qui non c'è
        // nessun tetto di lettura: il parser legge il documento mentre scorre, quindi un
        // file grosso non passa mai per un array grande quanto lui.
        val drawn = Svg.render(source.source().inputStream(), box) ?: return@withContext null
        DecodeResult(
            image = drawn.bitmap.asImage(),
            // ⚠️ **`true` sempre, e NON il `sampled` del disegno**: quel campo dice se il
            // raster è più piccolo del documento, mentre a Coil serve sapere se questa è
            // l'immagine **intera**, e una miniatura non lo è mai. Dichiarandola intera,
            // Coil la riuserebbe per una richiesta più grande, cioè un'icona da 512 pixel
            // stirata a schermo pieno.
            isSampled = true
        )
    }
}

/**
 * Chi decide se il disegnatore SVG serve.
 *
 * ⚠️⚠️ **SI GUARDANO I BYTE, E SI GUARDANO SENZA CONSUMARLI**: `peek()` dà un secondo
 * lettore sullo stesso buffer, quindi il decodificatore predefinito trova la sorgente intatta
 * quando questo si sfila. Senza, un JPEG resterebbe senza i suoi primi mille byte.
 * ⚠️ **Il tipo dichiarato conta come il contenuto**, ed è la stessa coppia di prove che usa
 * Coil nel suo `SvgDecoder.Factory`: un `content://` può dichiarare `image/svg+xml` senza che
 * i byte comincino col tag, per esempio quando in testa c'è una dichiarazione XML lunga.
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
 * Se il file può portare **trasparenza**, e quindi la sua miniatura non si chiede al sistema.
 *
 * ⚠️ **Il tipo prima e il nome dopo**: su un `content://` del MediaStore il percorso non
 * porta nessuna estensione (è un numero), quindi l'unica via è chiedere il tipo al provider;
 * su un `file://` invece il tipo non lo dichiara nessuno e l'estensione c'è sempre. Nessuna
 * delle due prove basta da sola.
 * ⚠️ **Gira su IO**, dentro `fetch`, e non nella fabbrica: `create` non sospende, e
 * chiedere il tipo a un provider durante la composizione vorrebbe dire un accesso al disco nel
 * mezzo di un disegno.
 * ⚠️ **Gli SVG ci sono per completezza ma non passano da qui**: li prende il loro
 * decodificatore, che sta più avanti nel registro. Toglierli dall'elenco renderebbe la
 * risposta di questa funzione una mezza verità da tenere a mente.
 */
private fun seeThrough(context: Context, uri: AndroidUri): Boolean {
    val mime = if (uri.scheme?.lowercase() == "content") {
        runCatching { context.contentResolver.getType(uri) }.getOrNull()?.lowercase()
    } else {
        null
    }
    if (mime != null) return mime in SEE_THROUGH_MIME
    val name = uri.lastPathSegment?.lowercase() ?: return false
    return SEE_THROUGH_EXT.any { name.endsWith(".$it") }
}

/** I tipi che possono avere un canale alfa. Vedi [seeThrough]. */
private val SEE_THROUGH_MIME = setOf(
    "image/png", "image/webp", "image/gif", "image/bmp", "image/x-ms-bmp", "image/svg+xml"
)

/** Le code degli stessi formati, per quando il tipo non lo dichiara nessuno. */
private val SEE_THROUGH_EXT = setOf("png", "webp", "gif", "bmp", "svg", "svgz")

package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import java.io.File

/**
 * Le miniature degli AVIF, tenute **su disco**, e dalla `2.98` anche i fotogrammi dei video.
 *
 * ⚠️⚠️ **ESISTE PERCHÉ RIENTRARE IN UNA CARTELLA LE RIFACEVA TUTTE** (riscontro dell'utente,
 * 2026-09-02: *con i miei file grossi anche uno Snapdragon molto potente impiega un paio di
 * secondi per miniatura, ma mi andrebbe bene se poi rimanessero. Purtroppo si rigenerano ad
 * ogni apertura della cartella*). La cache in memoria di Coil basta a scorrere, non a uscire
 * e rientrare: quella si svuota quando l'app perde le sue pagine, e allora i 24 megapixel si
 * decodificano di nuovo.
 *
 * ⚠️⚠️ **E VALEVA SOLO PER GLI AVIF, che è la sua richiesta alla lettera** (*eventualmente si
 * può fare una cache 'potenziata' solo per questo formato?*), e non è una restrizione
 * arbitraria: per ogni altro formato la miniatura la fa il **sistema**, che ne ha una già
 * pronta o sa farsela leggendo poche decine di kilobyte. Là copiare su disco sarebbe spazio
 * speso per niente, ed è la ragione per cui `Thumbs` dichiara `diskCache(null)`. Qui invece
 * il costo non è leggere il file, è **decodificarlo**: un AVIF non porta dentro nessuna
 * miniatura, quindi per un riquadro da 512 pixel bisogna ricostruire l'immagine intera.
 *
 * ⚠️⚠️ **E DALLA `2.98` TIENE ANCHE IL FOTOGRAMMA CHE AIV SCEGLIE PER UN VIDEO**, quando la
 * miniatura del sistema è nera o manca (vedi `ClipFrames`), ed è la stessa ragione detta con altre
 * parole: il costo non è leggere il file ma aprire il contenitore e decodificarne fino a quattro
 * fotogrammi. Il nome resta quello di prima, perché qui si tiene quello che AIV fa da sé, e la
 * pagina 'Gestisci le miniature memorizzate' ne misura e ne svuota il contenuto intero: i suoi
 * testi parlano già di immagini e di video.
 * ⚠️ **Un indirizzo di video e uno di immagine non si incontrano**, quindi i due non hanno bisogno
 * di due cartelle: la chiave è l'indirizzo con la data e la misura.
 *
 * ⚠️⚠️ **E DALLA `2.99` TIENE OGNI MINIATURA CHE PASSA DALLA DECODIFICA NORMALE, ED È LA SUA RISPOSTA
 * `disco` A `d-mini-disco`** (giro della `2.98`): i file con la trasparenza, i BMP, gli SVG e quelli
 * la cui miniatura di sistema è troppo piccola. Il sistema per loro non tiene niente, quindi fino
 * alla `2.98` si decodificavano a ogni apertura dell'app, e 'Genera miniature' non poteva
 * prepararli. A scrivere è `KeepingDecoderFactory`, in `Thumbs.kt`.
 * ⚠️ **Il nome della cartella resta quello di prima**, perché le miniature già scritte restano
 * buone e un nome nuovo le butterebbe tutte.
 * ⚠️⚠️ **E CON LORO IL TETTO SALE DA 400 A [KEEP]**, cioè abbastanza per un'intera collezione: la
 * domanda del giro lo diceva, *circa 300 MB ogni 10.000 miniature*, ed è una stima e non una
 * misura. ⚠️ **Il tetto vale per tutto quello che la cartella tiene**: chi ha una collezione più
 * grande vede potate le miniature viste meno di recente, che al giro dopo si rifanno.
 *
 * ⚠️⚠️ **NON È LA CACHE SU DISCO DI COIL, e non lo sarebbe potuta essere**: quella conserva i
 * **byte sorgente**, quindi con lei l'AVIF da 25 MB verrebbe copiato in una cartella di cache
 * e poi decodificato da capo ogni volta. Quello che qui si tiene è il **risultato**, cioè
 * un'immagine da qualche decina di kilobyte.
 */
object AvifCache {

    /**
     * La miniatura già fatta, o `null`.
     *
     * ⚠️ La chiave porta **la data del file**: sovrascrivendo un AVIF dall'editor l'indirizzo
     * resta identico, e senza la data la cartella continuerebbe a servire la miniatura di
     * prima, cioè un'immagine che sul telefono non esiste più.
     */
    fun read(context: Context, uri: Uri, box: Int): Bitmap? {
        val file = fileFor(context, uri, box) ?: return null
        if (!file.exists()) return null
        return runCatching {
            BitmapFactory.decodeFile(file.absolutePath)
        }.getOrNull().also {
            // ⚠️ Un file illeggibile si butta invece di essere riprovato a ogni apertura:
            // altrimenti resterebbe là a costare un tentativo per sempre.
            if (it == null) file.delete()
            // La data di ultimo accesso è quella su cui la potatura decide chi resta.
            else file.setLastModified(System.currentTimeMillis())
        }
    }

    /**
     * Tiene [bitmap] come miniatura di [uri].
     *
     * ⚠️ **WebP con perdita e non PNG**: qui dentro sta un riquadro da 512 pixel che si
     * guarda in una griglia, e un PNG dello stesso riquadro pesa qualche volta tanto. La
     * qualità è quella che si vede in una miniatura, non quella che si conserva.
     * ⚠️ **Se scrivere non riesce, non succede niente**: la miniatura c'è comunque, e la
     * prossima volta si rifà. Una cache che va in errore sarebbe peggio di una cache che manca.
     */
    fun write(context: Context, uri: Uri, box: Int, bitmap: Bitmap) {
        val file = fileFor(context, uri, box) ?: return
        runCatching {
            file.parentFile?.mkdirs()
            file.outputStream().use { bitmap.compress(FORMAT, QUALITY, it) }
            // ⚠️ **La potatura non gira a ogni scrittura**: con [KEEP] file, elencare la cartella
            // costerebbe più della miniatura appena scritta. Gira alla prima scrittura del processo e
            // poi una volta ogni [PRUNE_EVERY], quindi la cartella supera il tetto al più di tanto.
            if (writes.getAndIncrement() % PRUNE_EVERY == 0) prune(file.parentFile)
        }
    }

    /**
     * Quanto occupano le miniature tenute qui, in byte, e zero quando non ce n'è nessuna.
     *
     * ⚠️⚠️ **È LA MISURA CHE LE IMPOSTAZIONI MOSTRANO, ed è quella su DISCO e non in
     * memoria**: 'memorizzate' vuol dire ciò che resta fra un'apertura e l'altra dell'app, e
     * la cache in memoria di Coil non resta. Sommarle in un numero solo direbbe anche il
     * falso, perché là dentro non ci sono solo miniature: c'è l'immagine grande che si sta
     * guardando, che nessuno chiamerebbe una miniatura.
     * ⚠️ **Si chiama su un thread di I/O dalla `2.99`**: la cartella può tenere [KEEP] file, e un
     * `length()` per file sono altrettante `stat`. Il risultato resta in [known].
     */
    fun bytes(context: Context): Long =
        (dirOf(context).listFiles()?.sumOf { it.length() } ?: 0L).also { known = it }

    /**
     * L'ultima misura di [bytes] in questo processo, e `-1` finché non ce n'è stata nessuna.
     *
     * ⚠️ **Serve al primo fotogramma delle impostazioni**, che non può aspettare il thread di I/O:
     * con l'ultima misura la riga dice subito un numero vero, o non dice niente.
     */
    @Volatile
    var known: Long = -1L
        private set

    /**
     * Butta tutte le miniature tenute qui.
     *
     * ⚠️ **Non tocca la cache in MEMORIA**, che è di Coil e si svuota con `Thumbs.forgetAll`:
     * senza quella chiamata la griglia continuerebbe a mostrare quello che ha già in mano, e
     * lo svuotamento si vedrebbe solo dopo aver chiuso l'app.
     * ⚠️ **Non tocca nemmeno le miniature del SISTEMA**, che sono un'altra cache e non sono
     * nostre: quelle le rifà il MediaStore. Qui si butta la sola copia che teniamo noi.
     */
    fun clear(context: Context) {
        dirOf(context).listFiles()?.forEach { it.delete() }
    }

    /**
     * Dov'è la miniatura di [uri] alla misura [box], e `null` se non si può sapere.
     *
     * ⚠️ **La chiave è un hash e non il nome del file**: un nome di file può contenere
     * qualunque cosa, barre comprese, e ricavarne un percorso vorrebbe dire ripulirlo, cioè
     * far collidere due nomi diversi. Un hash non ha questo problema e ha lunghezza fissa.
     */
    private fun fileFor(context: Context, uri: Uri, box: Int): File? {
        val when1 = stamp(context, uri) ?: return null
        val key = "$uri|$when1|$box".hashCode().toString(HEX)
        return File(dirOf(context), "$key.webp")
    }

    /** La cartella in cui vivono le miniature, che esista o no. */
    private fun dirOf(context: Context) = File(context.cacheDir, DIR)

    /** Quando il file è stato scritto l'ultima volta, e `null` se non si sa. */
    private fun stamp(context: Context, uri: Uri): Long? {
        FileTree.fileOf(context, uri)?.let { file ->
            return file.lastModified().takeIf { it > 0L }
        }
        return null
    }

    /**
     * Butta le miniature più vecchie quando la cartella supera il tetto.
     *
     * ⚠️⚠️ **IL TETTO È SUI FILE E NON SUI BYTE, ed è una scelta**: qui dentro entrano solo
     * riquadri della stessa misura, quindi pesano quasi uguale e contarli è quasi lo stesso che
     * pesarli, con una `list()` invece di una `length()` per file. ⚠️ Il numero è quello di una
     * collezione intera, perché 'Genera miniature' serve a prepararla tutta.
     * ⚠️ **Sta in `cacheDir`**, quindi Android può svuotarla quando lo spazio finisce: è
     * esattamente quello che una cache deve permettere, e la miniatura si rifà.
     */
    private fun prune(dir: File?) {
        val files = dir?.listFiles() ?: return
        if (files.size <= KEEP) return
        files.sortedBy { it.lastModified() }
            .take(files.size - KEEP)
            .forEach { it.delete() }
    }

    private const val DIR = "avif-thumbs"
    private const val HEX = 16
    private const val QUALITY = 88
    private const val KEEP = 30_000
    private const val PRUNE_EVERY = 100

    /** Quante miniature si sono scritte in questo processo, per [PRUNE_EVERY]. */
    private val writes = java.util.concurrent.atomic.AtomicInteger()

    /**
     * Il formato in cui si scrive una miniatura.
     *
     * ⚠️⚠️ **`WEBP_LOSSY` NASCE CON ANDROID 11, E FINO ALLA `2.97` QUI ERA SCRITTO SENZA
     * CONDIZIONE**: sotto quella versione la costante non esiste, e leggerla fa fallire
     * l'inizializzazione dell'oggetto intero, cioè ogni sua funzione. Trovato scrivendo il ripiego
     * dei video, che da Android 9 in su passa di qui. Sotto Android 11 il gemello è `WEBP`, che con
     * una qualità sotto 100 comprime con perdita: è la stessa scelta di `ImageEdit` e di
     * `FolderCover`.
     */
    private val FORMAT = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Bitmap.CompressFormat.WEBP_LOSSY
    } else {
        @Suppress("DEPRECATION")
        Bitmap.CompressFormat.WEBP
    }
}

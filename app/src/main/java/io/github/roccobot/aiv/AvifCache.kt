package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.LruCache
import java.io.File

/**
 * Le miniature che AIV fa da sé, tenute **su disco**: gli AVIF, il fotogramma scelto per un
 * video quando la miniatura del sistema è nera o manca (`ClipFrames`), e ogni miniatura che passa
 * dalla decodifica normale (trasparenza, BMP, SVG, miniatura di sistema troppo piccola; risposta
 * `disco` a `d-mini-disco`). A scrivere queste ultime è `KeepingDecoderFactory`, in `Thumbs.kt`.
 *
 * ⚠️ **Esiste perché per questi file il costo è decodificare, non leggere**: un AVIF non contiene
 * una miniatura, quindi per un riquadro da 512 pixel si ricostruisce l'immagine intera, e un
 * video si apre e se ne decodificano fino a quattro fotogrammi. La cache in memoria di Coil basta
 * a scorrere, non a uscire e rientrare in una cartella. Per gli altri formati la miniatura la fa
 * il sistema, ed è la ragione di `diskCache(null)` in `Thumbs`.
 * Un indirizzo di video e uno di immagine non si incontrano, quindi basta una cartella: la chiave
 * è l'indirizzo con la data e la misura. La pagina 'Gestisci le miniature memorizzate' ne misura
 * e ne svuota il contenuto intero.
 * Il nome della cartella ([DIR]) non cambia: un nome nuovo butterebbe le miniature già scritte.
 *
 * Il tetto [KEEP] basta a una collezione intera (stima del giro: circa 300 MB ogni 10.000
 * miniature); una collezione più grande perde le miniature viste meno di recente, che si rifanno.
 *
 * ⚠️ **Non è la cache su disco di Coil**: quella conserva i byte sorgente, quindi l'AVIF da 25 MB
 * sarebbe copiato e poi decodificato da capo ogni volta. Qui si tiene il risultato, un'immagine
 * da qualche decina di kilobyte.
 */
object AvifCache {

    /**
     * La miniatura già fatta, o `null`.
     *
     * ⚠️ La chiave contiene **la data del file**: sovrascrivendo un AVIF dall'editor l'indirizzo
     * resta identico, e senza la data la cartella servirebbe la miniatura di prima.
     */
    fun read(context: Context, uri: Uri, box: Int): Bitmap? {
        val file = fileFor(context, uri, box) ?: return null
        if (!file.exists()) return null
        return runCatching {
            BitmapFactory.decodeFile(file.absolutePath)
        }.getOrNull().also {
            // Un file illeggibile si cancella: riprovato a ogni apertura costerebbe un
            // tentativo per sempre.
            if (it == null) file.delete()
            // La data di ultimo accesso è quella su cui la potatura decide chi resta.
            else file.setLastModified(System.currentTimeMillis())
        }
    }

    /**
     * Tiene [bitmap] come miniatura di [uri].
     *
     * WebP con perdita: è un riquadro da 512 pixel che si guarda in una griglia, e un PNG dello
     * stesso riquadro pesa qualche volta tanto.
     * Se la scrittura fallisce non succede niente: la miniatura c'è comunque, e la prossima volta
     * si rifà. Una cache che va in errore sarebbe peggio di una cache che manca.
     */
    fun write(context: Context, uri: Uri, box: Int, bitmap: Bitmap) {
        val file = fileFor(context, uri, box) ?: return
        runCatching {
            // ⚠️ Con [AtomicWrite] (rilievo C9): una miniatura troncata col nome giusto si
            // leggerebbe come buona.
            AtomicWrite.write(file) { bitmap.compress(FORMAT, QUALITY, it) }
            // La potatura gira alla prima scrittura del processo e poi una volta ogni
            // [PRUNE_EVERY]: con [KEEP] file, elencare la cartella costerebbe più della miniatura.
            // La cartella supera il tetto al più di tanto.
            if (writes.getAndIncrement() % PRUNE_EVERY == 0) prune(file.parentFile)
        }
    }

    /**
     * Cancella la miniatura su disco dopo una riscrittura, anche quando la data non cambia.
     * Tutte le richieste usano [Thumbs.PX], e le altre miniature restano. La chiamano le
     * operazioni sui file, sul thread di I/O, insieme alla cancellazione in memoria.
     */
    fun forget(context: Context, uri: Uri) {
        runCatching { fileFor(context, uri, Thumbs.PX)?.delete() }
    }

    /**
     * Quanto occupano le miniature tenute qui, in byte, e zero quando non ce n'è nessuna.
     *
     * È la misura che le impostazioni mostrano, e conta solo il disco: 'memorizzate' vuol dire
     * quello che resta fra un'apertura e l'altra dell'app, e la cache in memoria di Coil contiene
     * anche l'immagine grande che si sta guardando.
     * ⚠️ **Si chiama su un thread di I/O**: la cartella può tenere [KEEP] file, e un `length()`
     * per file sono altrettante `stat`. Il risultato resta in [known].
     */
    fun bytes(context: Context): Long =
        (dirOf(context).listFiles()?.sumOf { it.length() } ?: 0L).also { known = it }

    /**
     * L'ultima misura di [bytes] in questo processo, e `-1` finché non ce n'è stata nessuna.
     *
     * Serve al primo fotogramma delle impostazioni, che non può aspettare il thread di I/O: la
     * riga mostra subito l'ultima misura vera, o niente.
     */
    @Volatile
    var known: Long = -1L
        private set

    /**
     * Cancella tutte le miniature tenute qui.
     *
     * ⚠️ **La cache in memoria resta**: è di Coil e si svuota con `Thumbs.forgetAll`, senza la
     * quale la griglia continuerebbe a mostrare le miniature che ha già.
     * Le miniature del sistema restano anche loro: sono del MediaStore.
     */
    fun clear(context: Context) {
        dirOf(context).listFiles()?.forEach { it.delete() }
    }

    /**
     * Dov'è la miniatura di [uri] alla misura [box], e `null` se non si può sapere.
     *
     * Il nome è un hash: un nome di file può contenere qualunque cosa, barre comprese, e
     * ripulirlo farebbe collidere due nomi diversi.
     * ⚠️ **SHA-256** (rilievo C8): con un hash a 32 bit e fino a [KEEP] voci due file si
     * scambierebbero la miniatura circa una volta su cento.
     */
    internal fun fileFor(context: Context, uri: Uri, box: Int): File? {
        val when1 = stamp(context, uri) ?: return null
        return File(dirOf(context), "${sha256("$uri|$when1|$box")}.webp")
    }

    /** La cartella in cui vivono le miniature, che esista o no. */
    private fun dirOf(context: Context) = File(context.cacheDir, DIR)

    /**
     * Quando il file è stato scritto l'ultima volta, e `null` se non si sa.
     *
     * ⚠️ **Il percorso si ricorda** (rilievo C10): chiederlo al MediaStore è una domanda a un altro
     * processo per riquadro, anche per i JPEG che su disco non hanno niente (presidiato da
     * `PercorsoMiniaturaTest`). La data si legge sempre dal file, quindi una riscrittura cambia la
     * chiave; un percorso ricordato che non è più un file (rinomina, spostamento) si richiede.
     */
    private fun stamp(context: Context, uri: Uri): Long? {
        val chiave = uri.toString()
        val noto = paths.get(chiave)?.let(::File)?.takeIf { it.isFile }
        val file = noto ?: FileTree.fileOf(context, uri)?.also { paths.put(chiave, it.absolutePath) }
            ?: run {
                paths.remove(chiave)
                return null
            }
        return file.lastModified().takeIf { it > 0L }
    }

    /** I percorsi già chiesti al MediaStore, per indirizzo: vedi [stamp]. */
    private val paths = LruCache<String, String>(PATHS)

    /**
     * Cancella le miniature più vecchie quando la cartella supera il tetto.
     *
     * Il tetto conta i file, non i byte: i riquadri sono tutti della stessa misura, quindi
     * contarli vale quasi come pesarli, con una `list()` sola. Il numero è quello di una
     * collezione intera, perché 'Genera miniature' serve a prepararla tutta.
     * La cartella è in `cacheDir`: Android può svuotarla quando lo spazio finisce, e le miniature
     * si rifanno.
     */
    private fun prune(dir: File?) {
        val files = dir?.listFiles() ?: return
        if (files.size <= KEEP) return
        files.sortedBy { it.lastModified() }
            .take(files.size - KEEP)
            .forEach { it.delete() }
    }

    private const val DIR = "avif-thumbs"
    private const val QUALITY = 88
    private const val KEEP = 30_000
    private const val PRUNE_EVERY = 100

    /** Quanti percorsi si ricordano: qualche griglia piena, poche decine di kilobyte. */
    private const val PATHS = 2048

    /** Quante miniature si sono scritte in questo processo, per [PRUNE_EVERY]. */
    private val writes = java.util.concurrent.atomic.AtomicInteger()

    /**
     * Il formato in cui si scrive una miniatura.
     *
     * ⚠️ **`WEBP_LOSSY` esiste da Android 11**: prima la costante non c'è, e leggerla fa fallire
     * l'inizializzazione dell'oggetto intero, cioè ogni sua funzione (il ripiego dei video passa
     * di qui da Android 9). Sotto Android 11 si usa `WEBP`, che con una qualità sotto 100
     * comprime con perdita, come in `ImageEdit` e in `FolderCover`.
     */
    private val FORMAT = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Bitmap.CompressFormat.WEBP_LOSSY
    } else {
        @Suppress("DEPRECATION")
        Bitmap.CompressFormat.WEBP
    }
}

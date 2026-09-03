package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * Le miniature degli AVIF, tenute **su disco**.
 *
 * ⚠️⚠️ **ESISTE PERCHÉ RIENTRARE IN UNA CARTELLA LE RIFACEVA TUTTE** (riscontro dell'utente,
 * 2026-09-02: *con i miei file grossi anche uno Snapdragon molto potente impiega un paio di
 * secondi per miniatura, ma mi andrebbe bene se poi rimanessero. Purtroppo si rigenerano ad
 * ogni apertura della cartella*). La cache in memoria di Coil basta a scorrere, non a uscire
 * e rientrare: quella si svuota quando l'app perde le sue pagine, e allora i 24 megapixel si
 * decodificano di nuovo.
 *
 * ⚠️⚠️ **E VALE SOLO PER GLI AVIF, che è la sua richiesta alla lettera** (*eventualmente si
 * può fare una cache 'potenziata' solo per questo formato?*), e non è una restrizione
 * arbitraria: per ogni altro formato la miniatura la fa il **sistema**, che ne ha una già
 * pronta o sa farsela leggendo poche decine di kilobyte. Là copiare su disco sarebbe spazio
 * speso per niente, ed è la ragione per cui `Thumbs` dichiara `diskCache(null)`. Qui invece
 * il costo non è leggere il file, è **decodificarlo**: un AVIF non porta dentro nessuna
 * miniatura, quindi per un riquadro da 512 pixel bisogna ricostruire l'immagine intera.
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
            prune(file.parentFile)
        }
    }

    /**
     * Dove sta la miniatura di [uri] alla misura [box], e `null` se non si può sapere.
     *
     * ⚠️ **La chiave è un hash e non il nome del file**: un nome di file può contenere
     * qualunque cosa, barre comprese, e ricavarne un percorso vorrebbe dire ripulirlo, cioè
     * far collidere due nomi diversi. Un hash non ha questo problema e ha lunghezza fissa.
     */
    private fun fileFor(context: Context, uri: Uri, box: Int): File? {
        val when1 = stamp(context, uri) ?: return null
        val key = "$uri|$when1|$box".hashCode().toString(HEX)
        return File(File(context.cacheDir, DIR), "$key.webp")
    }

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
     * riquadri della stessa misura, quindi pesano tutti uguale e contarli è lo stesso che
     * pesarli, con una `list()` invece di una `length()` per file. ⚠️ Il numero è generoso
     * apposta: [KEEP] miniature sono più di quante ne stiano in qualunque cartella che si
     * scorra a mano, quindi la potatura non entra mai in gioco nell'uso normale.
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
    private const val KEEP = 400
    private val FORMAT = Bitmap.CompressFormat.WEBP_LOSSY
}

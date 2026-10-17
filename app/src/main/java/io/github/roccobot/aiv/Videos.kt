package io.github.roccobot.aiv

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Che genere di file si vuole vedere adesso: il filtro volatile della griglia.
 *
 * ⚠️ **Non è un'impostazione e non si ricorda**: si azzera a ogni cambio di cartella, per scelta
 * dell'utente, quindi vive nel modello e non nell'archivio. Un filtro dimenticato acceso farebbe
 * sembrare una cartella senza metà dei suoi file.
 * È in questo file perché la domanda 'è un video' la sa rispondere [Videos], e la risposta deve
 * essere una sola in tutta l'app.
 */
enum class MediaKind {
    ALL, IMAGES, VIDEOS;

    fun keeps(uri: Uri): Boolean = when (this) {
        ALL -> true
        IMAGES -> !Videos.isVideo(uri)
        VIDEOS -> Videos.isVideo(uri)
    }
}

/**
 * I video: come si riconoscono e quanto durano. Si vedono nelle cartelle, nelle griglie e nelle
 * ricerche con la durata sopra la miniatura, si riproducono nell'app (`ClipStage` in
 * `ViewerScreen.kt`) e sfogliando si possono saltare con 'Sfoglia solo le immagini'.
 *
 * ⚠️ **Il tipo viaggia dentro l'indirizzo.** Una serie è solo una lista di indirizzi
 * (`Folder.Series`), e il MediaStore serve i video su una tabella sua
 * (`content://media/external/video/media/<id>`), distinta da quella delle immagini: quindi
 * [isVideo] è una funzione pura, senza query e senza memoria, che la griglia può chiamare per ogni
 * riquadro che scorre. Mettere il tipo nella serie vorrebbe dire cambiare insieme visualizzatore,
 * ricerca, cestino e selezione.
 */
object Videos {

    /**
     * Se quell'indirizzo è un video.
     *
     * ⚠️ **Si guarda il segmento e non la stringa intera**: un `content://` del MediaStore è
     * `<volume>/video/media/<id>`, e il volume non è sempre `external` (una scheda SD ha il suo,
     * come `1234-5678`). Il confronto con `Video.Media.EXTERNAL_CONTENT_URI` intero lascerebbe
     * fuori i file sulla scheda.
     * L'estensione serve ai `file://`, che arrivano da una cartella che il MediaStore non conosce
     * (`Folder.fromDisk`): là il nome è tutto quello che si ha.
     * Un indirizzo remoto non è mai un video: il visualizzatore scarica e decodifica immagini.
     * ⚠️ **Un indirizzo del selettore di sistema non ha questa forma**
     * (`content://media/picker/<utente>/<autorità>/media/<id>`), quindi qui risponderebbe
     * 'immagine'. Oggi il selettore chiede `PickVisualMedia.ImageOnly`; chi lo aprisse ai video
     * (`ImageAndVideo`) deve cambiare prima questa funzione, o un video finirebbe al
     * decodificatore di immagini.
     */
    fun isVideo(uri: Uri): Boolean = when (uri.scheme?.lowercase()) {
        "content" -> uri.pathSegments.getOrNull(1) == VIDEO_SEGMENT
        "file" -> uri.lastPathSegment.orEmpty().substringAfterLast('.', "").lowercase() in EXTENSIONS
        else -> false
    }

    /**
     * Le estensioni che si considerano video leggendo una cartella dal disco.
     *
     * Fa per i video quello che `Folder.EXTENSIONS` fa per le immagini. Non sono tutti i formati
     * che Android sa aprire: sono quelli che il MediaStore indicizza come video sui telefoni veri,
     * più `mkv` e `avi`, che capita di trovare in una cartella scaricata. Un formato che manca qui
     * è un file che non compare nella cartella letta dal disco.
     */
    val EXTENSIONS: Set<String> = setOf(
        "mp4", "m4v", "mkv", "webm", "3gp", "3gpp", "mov", "avi", "mpg", "mpeg", "ts", "wmv"
    )

    /**
     * Quanto dura, in millisecondi, e `null` quando non si riesce a saperlo.
     *
     * Con una memoria per indirizzo: la durata non è nell'indirizzo e va chiesta, e una cartella
     * di trecento video scorsa avanti e indietro la chiederebbe centinaia di volte per lo stesso
     * file. Il ripiego su `MediaMetadataRetriever`, per i soli `file://`, apre il contenitore e
     * ne legge l'intestazione: costa molto più di una riga del MediaStore.
     */
    suspend fun length(context: Context, uri: Uri): Long? {
        known[uri]?.let { return it.takeIf { ms -> ms >= 0 } }
        val read = withContext(Dispatchers.IO) { read(context, uri) }
        // Si ricorda anche il fallimento, come -1, o la durata che non si sa sarebbe richiesta a
        // ogni giro di scorrimento.
        known[uri] = read ?: -1L
        return read
    }

    /** La durata se è già stata letta, per il primo disegno del riquadro. */
    fun cachedLength(uri: Uri): Long? = known[uri]?.takeIf { it >= 0 }

    private fun read(context: Context, uri: Uri): Long? {
        if (uri.scheme?.lowercase() == "content") {
            val column = MediaStore.Video.Media.DURATION
            runCatching {
                context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { c ->
                    val at = c.getColumnIndex(column).takeIf { it >= 0 } ?: return@use null
                    if (c.moveToFirst() && !c.isNull(at)) c.getLong(at) else null
                }
            }.getOrNull()?.let { return it }
        }
        // ⚠️ `release()` a mano: `MediaMetadataRetriever` è chiudibile con `use` solo da Android
        // 10, e `minSdk` è 28.
        val reader = MediaMetadataRetriever()
        return try {
            reader.setDataSource(context, uri)
            reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { reader.release() }
        }
    }

    /**
     * La durata come si scrive sopra una miniatura: `0:07`, `3:41`, `1:02:15`.
     *
     * Serve anche al tempo del lettore: durata e posizione sono ai due capi della barra, e devono
     * avere la stessa forma.
     *
     * I secondi si arrotondano: un video di 7,6 secondi scritto `0:07` contraddirebbe il lettore,
     * che alla fine dice `0:08`.
     * ⚠️ **Il minimo `0:01` vale per una durata e non per una posizione, ed è quello che [floor]
     * distingue**: un video non può durare `0:00`, che si leggerebbe come un file rotto, ma un
     * video fermo all'inizio è a `0:00`.
     * Le cifre seguono la lingua del telefono (`Locale.getDefault()`): in arabo e in bengalese i
     * numeri hanno segni propri.
     */
    fun stamp(ms: Long, floor: Boolean = false): String {
        val whole = ((ms + 500) / 1000).coerceAtLeast(if (floor) 1 else 0)
        val seconds = whole % 60
        val minutes = (whole / 60) % 60
        val hours = whole / 3600
        val locale = Locale.getDefault()
        return if (hours > 0) String.format(locale, "%d:%02d:%02d", hours, minutes, seconds)
        else String.format(locale, "%d:%02d", minutes, seconds)
    }

    /** Il segmento che, in un indirizzo del MediaStore, dice 'questo è un video'. */
    private const val VIDEO_SEGMENT = "video"

    /** Le durate già lette, in millisecondi, e -1 per quelle che non si sono sapute. */
    private val known = ConcurrentHashMap<Uri, Long>()
}

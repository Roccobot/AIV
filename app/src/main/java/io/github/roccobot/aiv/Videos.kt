package io.github.roccobot.aiv

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * I video: come si riconoscono, quanto durano, e chi li fa vedere.
 *
 * ⚠️⚠️ **PRIMO DEI TRE PEZZI del supporto ai video** (`0.83`), quello che l'utente ha
 * chiesto per primo (*quanto è complicato abilitare il supporto ai video? solo cose
 * terra-terra, oltre all'anteprima nelle cartelle*). Qui i video si **vedono**: nelle
 * cartelle, nelle griglie, nelle ricerche, con la durata sopra la miniatura; toccandone
 * uno si vede il suo fotogramma e il tastino che lo passa al lettore del telefono. La
 * riproduzione dentro l'app è il pezzo 2, e il resto dell'app (zoom, ritaglio, tavolozza,
 * ricerca per contenuto) è il pezzo 3.
 *
 * ⚠️⚠️ **IL TIPO VIAGGIA DENTRO L'INDIRIZZO, ed è LA decisione di questo pezzo.** Una
 * serie è una lista di indirizzi e basta (vedi `Folder.Series`), quindi ogni pezzo dell'app
 * che deve comportarsi diversamente davanti a un video ha due sole vie: chiedere al
 * MediaStore, oppure guardare l'indirizzo. Il MediaStore serve i video su una tabella
 * **sua** (`content://media/external/video/media/<id>`), distinta da quella delle immagini,
 * quindi l'indirizzo lo dice già: [isVideo] è una funzione pura, senza query e senza cache,
 * e la griglia può chiamarla per ogni riquadro che scorre.
 * ⚠️ **L'alternativa era far portare il tipo alla serie**, e sarebbe stato l'errore già
 * dichiarato nella `0.82` per i nomi: cambiare la forma di `Folder.Series` vuol dire
 * cambiare il visualizzatore, la ricerca, il cestino e la selezione, tutti insieme.
 */
object Videos {

    /**
     * Se quell'indirizzo è un video.
     *
     * ⚠️ **Si guarda il SEGMENTO e non la stringa intera**: un `content://` del MediaStore
     * è `<volume>/video/media/<id>`, e il volume non è sempre `external` (una scheda SD ha
     * il suo, tipo `1234-5678`). Confrontare con `Video.Media.EXTERNAL_CONTENT_URI` per
     * intero lascerebbe fuori proprio i file sulla scheda.
     * ⚠️ **Il ripiego sull'estensione serve ai `file://`**, che arrivano dalla lettura di
     * una cartella che il MediaStore non conosce (`Folder.fromDisk`): là non c'è nessuna
     * tabella a dire di che si tratta, e il nome è tutto quello che si ha.
     * ⚠️ **Un indirizzo remoto non è mai un video, per ora**: il visualizzatore scarica e
     * decodifica immagini, e un `http` che punta a un filmato resta fuori da questo pezzo.
     * ⚠️⚠️ **Un indirizzo del SELETTORE di sistema non ha questa forma**
     * (`content://media/picker/<utente>/<autorità>/media/<id>`, dove al secondo posto c'è il
     * numero dell'utente), quindi qui risponderebbe 'immagine'. Oggi non è un difetto perché
     * il selettore di questa app chiede `PickVisualMedia.ImageOnly` e un filmato da lì non
     * arriva; chi un domani lo aprisse anche ai video (`ImageAndVideo`) deve passare da qui
     * **per primo**, o si troverebbe un filmato spedito al decodificatore di immagini.
     */
    fun isVideo(uri: Uri): Boolean = when (uri.scheme?.lowercase()) {
        "content" -> uri.pathSegments.getOrNull(1) == VIDEO_SEGMENT
        "file" -> uri.lastPathSegment.orEmpty().substringAfterLast('.', "").lowercase() in EXTENSIONS
        else -> false
    }

    /**
     * Le estensioni che si considerano video leggendo una cartella dal disco.
     *
     * ⚠️ Stesso mestiere di `Folder.EXTENSIONS` per le immagini, e stessa ragione: senza
     * MediaStore non c'è nessuno che sappia distinguere, e una directory contiene di tutto.
     * ⚠️ **Non sono tutti i formati che Android sa aprire**: sono quelli che il MediaStore
     * indicizza come video sui telefoni veri, più i due contenitori (`mkv`, `avi`) che
     * capita di trovare in una cartella scaricata. Un formato in meno qui vuol dire un file
     * che nella cartella dal disco non compare; non vuol dire un file rotto.
     */
    val EXTENSIONS: Set<String> = setOf(
        "mp4", "m4v", "mkv", "webm", "3gp", "3gpp", "mov", "avi", "mpg", "mpeg", "ts", "wmv"
    )

    /**
     * Quanto dura, in millisecondi, e `null` quando non si riesce a saperlo.
     *
     * ⚠️⚠️ **CON LA MEMORIA PER INDIRIZZO, come i nomi della `0.82`, e per lo stesso
     * motivo**: la durata non sta nell'indirizzo, va chiesta, e una cartella da trecento
     * video scorsa avanti e indietro farebbe quella domanda centinaia di volte per lo
     * stesso file. Una voce costa otto byte.
     * ⚠️ **Il ripiego su `MediaMetadataRetriever` è per i soli `file://`**, e costa
     * infinitamente di più di una riga del MediaStore: apre il contenitore e ne legge
     * l'intestazione. Ci si arriva solo nelle cartelle che il MediaStore non conosce, ed è
     * la ragione per cui la memoria qui non è un lusso.
     */
    suspend fun length(context: Context, uri: Uri): Long? {
        known[uri]?.let { return it.takeIf { ms -> ms >= 0 } }
        val read = withContext(Dispatchers.IO) { read(context, uri) }
        // ⚠️ Si ricorda anche il fallimento, come -1: senza, un video di cui la durata non
        // si sa la si richiederebbe a ogni giro di scorrimento, che è il caso peggiore.
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
        // ⚠️ `release()` a mano e non `use`: `MediaMetadataRetriever` è chiudibile solo da
        // Android 10, e `minSdk` è 28. Con `use` il codice compilerebbe e lint segnalerebbe
        // una chiamata che su un telefono vecchio non esiste.
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
     * ⚠️ **I secondi si arrotondano invece di troncare**: un filmato di 7,6 secondi scritto
     * `0:07` contraddice il lettore, che alla fine dirà `0:08`. Arrotondando, i due numeri
     * combaciano.
     * ⚠️ **E il minimo è `0:01`**: un video esiste, quindi non può durare `0:00`. Sotto il
     * mezzo secondo l'arrotondamento darebbe zero, che si legge come 'file rotto'.
     * ⚠️ **Le cifre seguono la lingua del telefono** (`Locale.getDefault()`): in arabo e in
     * bengalese i numeri hanno segni propri, e una durata in cifre occidentali in mezzo a
     * un'interfaccia tradotta è la stessa stonatura di una data all'americana.
     */
    fun stamp(ms: Long): String {
        val whole = ((ms + 500) / 1000).coerceAtLeast(1)
        val seconds = whole % 60
        val minutes = (whole / 60) % 60
        val hours = whole / 3600
        val locale = Locale.getDefault()
        return if (hours > 0) String.format(locale, "%d:%02d:%02d", hours, minutes, seconds)
        else String.format(locale, "%d:%02d", minutes, seconds)
    }

    /**
     * Passa il filmato al lettore del telefono, e dice se qualcuno l'ha preso.
     *
     * ⚠️⚠️ **È IL PEZZO 1, e la riproduzione dentro l'app è il 2**: qui non si decodifica
     * niente, si consegna. Il baratto è dichiarato e si vede: si esce dall'app e ci si torna
     * col tasto Indietro.
     * ⚠️ **Il tipo si CHIEDE invece di scrivere il jolly dei filmati**: dichiararlo farebbe
     * comparire nel dialogo anche i lettori che quel formato non aprono, e il tipo vero è
     * una riga del MediaStore che si legge in un istante.
     * ⚠️⚠️ **I `file://` restano fuori, e non è una dimenticanza**: un altro programma non
     * può leggere un nostro `file://` (da Android 7 il sistema solleva), e il nostro
     * FileProvider serve **una cartella sola**, quella delle condivisioni, apposta (vedi
     * `ImageActions.shareMany`). Allargarlo a tutta la memoria per un caso raro sarebbe
     * pagare in sicurezza una comodità: quelle cartelle sono le sole che il MediaStore non
     * conosce, e il pezzo 2, che riproduce in casa, non ha questo problema.
     */
    fun play(context: Context, uri: Uri): Boolean {
        if (uri.scheme?.lowercase() != "content") return false
        val type = runCatching { context.contentResolver.getType(uri) }.getOrNull() ?: FALLBACK_TYPE
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, type)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    /** Il segmento che, in un indirizzo del MediaStore, dice 'questo è un filmato'. */
    private const val VIDEO_SEGMENT = "video"

    /**
     * Quando il MediaStore non dichiara il tipo: meglio il jolly che niente.
     *
     * ⚠️⚠️ **E IL JOLLY NON SI SCRIVE DENTRO UN COMMENTO A BLOCCO, mai**: in Kotlin i
     * commenti a blocco si **annidano**, quindi una barra seguita da un asterisco ne apre un
     * secondo, e la chiusura che dovrebbe chiudere questo chiude quello. L'errore che ne
     * esce parla di commento non chiuso a fine file, cioè lontanissimo dalla riga colpevole,
     * e per giunta segnala come 'non risolti' tutti i nomi dell'oggetto. È scritto anche in
     * `ImageActions.shareMany`, ed è successo lo stesso qui: la nota là non basta a chi
     * scrive un file nuovo.
     */
    private const val FALLBACK_TYPE = "video/*"

    /** Le durate già lette, in millisecondi, e -1 per quelle che non si sono sapute. */
    private val known = ConcurrentHashMap<Uri, Long>()
}

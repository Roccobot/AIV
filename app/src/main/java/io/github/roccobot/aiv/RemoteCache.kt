package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import java.io.File
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * I byte delle immagini prese dalla rete, tenuti da parte.
 *
 * Esiste perché un indirizzo diretto, senza, ripartirebbe dalla connessione a ogni sguardo, anche
 * tornando sulla stessa immagine di dieci secondi prima.
 *
 * ⚠️ **Una voce ha una scadenza**, come in un browser: un indirizzo il cui contenuto cambia
 * (l'istantanea di una webcam, un'immagine sostituita sul server) mostrerebbe per sempre la prima
 * versione. Finché la voce è fresca ([Entry.fresh]) si serve senza rete, poi si chiede al server se
 * è cambiata (`If-None-Match`, `If-Modified-Since`), che risponde `304` senza corpo se è uguale. Il
 * percorso vive in `ImageSource.loadRemote`.
 *
 * ⚠️ **Non è la cache di Coil, e le due non si uniscono.** Quella serve le miniature dei file
 * locali e ha il disco spento apposta (vedi `Thumbs`); questa tiene i byte di ciò che sul telefono
 * non c'è.
 *
 * Vive in `cacheDir`: il contenuto si rifà scaricandolo, quindi il sistema può cancellarlo quando
 * lo spazio scarseggia, e l'utente lo svuota da 'Cancella cache' senza toccare le impostazioni.
 */
object RemoteCache {

    /**
     * Il tetto di un file solo: un file più grande non si tiene.
     *
     * ⚠️ Senza, un'immagine sola potrebbe riempire la cache e far uscire tutte le altre. Il
     * tetto del trasferimento è quello di `ImageSource`, molto più alto: qui si decide che cosa
     * conservare, non che cosa si può aprire.
     */
    private const val MAX_ENTRY_BYTES = 16L * 1024 * 1024

    /** Quanto occupa in tutto, prima che le più vecchie escano. */
    private const val MAX_TOTAL_BYTES = 96L * 1024 * 1024

    /**
     * Quanto resta fresca una voce di cui il server non dice niente (né `max-age` né
     * `Last-Modified`).
     *
     * ⚠️ È il tempo di un giro avanti e indietro fra le immagini di una pagina, che è il caso per
     * cui la cache esiste. Più lungo, un'immagine cambiata sul server resterebbe vecchia a lungo;
     * zero, ogni ritorno sulla stessa immagine la riscaricherebbe intera, perché senza
     * `Last-Modified` e spesso senza `ETag` non c'è niente con cui chiedere se è cambiata.
     */
    private const val UNKNOWN_FRESH_MS = 10L * 60 * 1000

    /**
     * Il tetto della freschezza ricavata da `Last-Modified`, quando il server non ne dichiara una.
     *
     * ⚠️ La regola del browser è il 10% dell'età del file: un'immagine ferma da un anno varrebbe
     * più di un mese senza chiedere niente. Un giorno basta al caso d'uso, e tiene corto il tempo
     * in cui una sostituzione resta invisibile.
     */
    private const val HEURISTIC_CAP_MS = 24L * 60 * 60 * 1000

    /**
     * Il segno che apre una voce, con la riga delle intestazioni dopo.
     *
     * ⚠️ Una voce senza questo segno è del formato vecchio, solo byte: si legge come scaduta e
     * senza niente con cui chiedere, quindi si riscarica una volta e si riscrive nel formato nuovo.
     */
    private val MAGIC = "AIVC2\n".toByteArray()

    /**
     * Una voce: i byte, fin quando sono freschi, e con che cosa chiedere al server se sono cambiati.
     *
     * [freshUntil] è un istante di [System.currentTimeMillis]; [etag] e [lastModified] sono le
     * intestazioni della risposta, ripetute tali e quali nella domanda.
     */
    class Entry(val bytes: ByteArray, val freshUntil: Long, val etag: String?, val lastModified: String?) {
        val fresh: Boolean get() = System.currentTimeMillis() < freshUntil
    }

    /**
     * I byte tenuti per questo indirizzo, o null, freschi o no.
     *
     * ⚠️ È la lettura di chi guarda l'immagine **già aperta** (le informazioni, le aree, le
     * animazioni): quei byte sono quelli che si vedono, quindi la scadenza qui non conta.
     */
    fun read(context: Context, uri: Uri): ByteArray? = entry(context, uri)?.bytes

    /**
     * La voce di questo indirizzo, o null.
     *
     * ⚠️ La lettura aggiorna la data di modifica del file, che è l'unico ordinamento di questa
     * cache: senza, la sfoltitura toglierebbe l'immagine guardata più spesso solo perché è stata
     * scaricata per prima.
     */
    fun entry(context: Context, uri: Uri): Entry? {
        val file = fileFor(context, uri)
        if (!file.isFile) return null
        return runCatching {
            val raw = file.readBytes()
            file.setLastModified(System.currentTimeMillis())
            parse(raw)
        }.getOrNull()
    }

    /**
     * Tiene da parte i byte di questo indirizzo, se ci stanno e se il server lo permette.
     *
     * [cacheControl], [etag] e [lastModified] sono le intestazioni della risposta, tali e quali,
     * o null quando mancano.
     *
     * ⚠️ Con [AtomicWrite]: un'app chiusa a metà scrittura lascerebbe un file troncato con il
     * nome giusto, cioè una cache che serve immagini rotte.
     * ⚠️ **Con `no-store` la voce di prima si cancella**: il server ha appena detto che quella
     * risposta non va tenuta, e una copia vecchia ancora fresca la smentirebbe.
     */
    fun write(
        context: Context,
        uri: Uri,
        bytes: ByteArray,
        cacheControl: String? = null,
        etag: String? = null,
        lastModified: String? = null
    ) {
        val file = fileFor(context, uri)
        val now = System.currentTimeMillis()
        val freshFor = freshFor(cacheControl, httpDate(lastModified), now)
        if (freshFor == null) {
            file.delete()
            return
        }
        if (bytes.size > MAX_ENTRY_BYTES) return
        val body = MAGIC + header(now + freshFor, etag, lastModified) + bytes
        if (AtomicWrite.bytes(file, body)) runCatching { trim(file.parentFile) }
    }

    /**
     * Il server ha risposto `304`: i byte tenuti sono ancora quelli, e tornano freschi.
     *
     * ⚠️ Le intestazioni nuove vincono su quelle salvate, perché un `304` può aggiornarle; dove
     * mancano restano quelle di prima, che sono le stesse con cui si è chiesto.
     */
    fun renew(context: Context, uri: Uri, old: Entry, cacheControl: String?, etag: String?, lastModified: String?) =
        write(context, uri, old.bytes, cacheControl, etag ?: old.etag, lastModified ?: old.lastModified)

    /** Un'intestazione HTTP di data (`Last-Modified`) in millisecondi, o 0 se manca o non si legge. */
    private fun httpDate(text: String?): Long = text?.let {
        runCatching {
            ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
        }.getOrNull()
    } ?: 0

    /**
     * Per quanti millisecondi una risposta resta fresca, o null se non va tenuta affatto.
     *
     * ⚠️⚠️ **`no-store` vuol dire non tenerla, e `no-cache` tenerla ma chiedere ogni volta**: sono
     * le due parole che un server usa per un'istantanea che cambia, e confonderle vorrebbe dire o
     * riscaricare sempre o mostrare la vecchia.
     * ⚠️ Senza `max-age` si usa il 10% dell'età dichiarata da `Last-Modified`, con un tetto
     * ([HEURISTIC_CAP_MS]), e senza nemmeno quella [UNKNOWN_FRESH_MS].
     */
    internal fun freshFor(cacheControl: String?, lastModified: Long, now: Long): Long? {
        val directives = cacheControl.orEmpty().lowercase().split(',').map { it.trim() }
        if ("no-store" in directives) return null
        if ("no-cache" in directives) return 0
        directives.firstOrNull { it.startsWith("max-age=") }
            ?.substringAfter('=')?.trim('"')?.toLongOrNull()
            ?.let { return (it * 1000).coerceAtLeast(0) }
        if (lastModified in 1 until now) return ((now - lastModified) / 10).coerceAtMost(HEURISTIC_CAP_MS)
        return UNKNOWN_FRESH_MS
    }

    private fun header(freshUntil: Long, etag: String?, lastModified: String?): ByteArray =
        listOf(freshUntil.toString(), clean(etag), clean(lastModified))
            .joinToString("\t", postfix = "\n").toByteArray()

    /** Un'intestazione senza i due caratteri che separano i campi della riga. */
    private fun clean(value: String?): String = value.orEmpty().filter { it != '\t' && it != '\n' }

    private fun parse(raw: ByteArray): Entry {
        val magic = raw.size >= MAGIC.size && (MAGIC.indices).all { raw[it] == MAGIC[it] }
        if (!magic) return Entry(raw, 0, null, null)
        val end = (MAGIC.size until raw.size).first { raw[it] == '\n'.code.toByte() }
        val fields = String(raw, MAGIC.size, end - MAGIC.size).split('\t')
        return Entry(
            bytes = raw.copyOfRange(end + 1, raw.size),
            freshUntil = fields.getOrNull(0)?.toLongOrNull() ?: 0,
            etag = fields.getOrNull(1)?.takeIf { it.isNotEmpty() },
            lastModified = fields.getOrNull(2)?.takeIf { it.isNotEmpty() }
        )
    }

    /**
     * Le più vecchie escono finché il totale non rientra.
     *
     * ⚠️ Si sfoltisce dopo aver scritto: prima vorrebbe dire stimare quanto occuperà la
     * scrittura, e una stima per difetto lascia la cartella sopra il tetto fino al giro dopo.
     */
    private fun trim(directory: File?) {
        val files = directory?.listFiles()?.filter { it.isFile } ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_TOTAL_BYTES) return
        for (file in files.sortedBy { it.lastModified() }) {
            if (total <= MAX_TOTAL_BYTES) return
            val size = file.length()
            if (file.delete()) total -= size
        }
    }

    /**
     * Il nome del file per un indirizzo.
     *
     * ⚠️ **SHA-256 dell'indirizzo intero, query compresa**: il digest evita i caratteri che un
     * filesystem non accetta e i nomi troppo lunghi. La query fa parte della chiave perché su
     * moltissimi siti è lei a dire quale immagine è, e troncarla farebbe servire un'immagine al
     * posto di un'altra.
     */
    private fun fileFor(context: Context, uri: Uri): File =
        File(File(context.cacheDir, "remote"), sha256(uri.toString()))
}

/**
 * Il digest SHA-256 di [text], in esadecimale: il nome di un file di cache.
 *
 * Lo usano le due cache su disco, questa e quella delle miniature (`AvifCache`): un hash di 32 bit
 * fa collidere due miniature su cento a diecimila voci.
 */
internal fun sha256(text: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray())
        .joinToString("") { "%02x".format(it) }

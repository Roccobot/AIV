package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.File
import java.io.FilterInputStream
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Le parti di un backup, cioè le caselle della pagina 'Esporta e importa'.
 *
 * ⚠️⚠️ **SONO MACRO-AREE, ED È LA SUA RIGA ALLA LETTERA** (terza richiesta del campo libero del
 * giro della `2.91`: *per non fare un elenco troppo lungo di checkbox, si selezionerà per
 * macro-aree*). Le prime tre sono le sezioni della schermata delle impostazioni, cioè i posti in
 * cui quelle preferenze si toccano, e portano il loro titolo; le altre sono le cose che l'app
 * ricorda fuori da quella schermata.
 * ⚠️ **Il token è il formato e l'etichetta è la pagina**, e vivono nella stessa riga perché
 * l'elenco delle aree è uno: un'area nuova scritta in due tabelle comparirebbe nel file e non fra
 * le caselle, o il contrario. Il token non si traduce e non cambia mai, perché lo leggono anche i
 * file già salvati.
 * ⚠️ **L'ordine è quello delle caselle**, e il cestino viene per ultimo: è la sola parte che pesa
 * quanto le immagini che contiene, e la sola che all'importazione si aggiunge invece di prendere
 * il posto di quello che c'è.
 */
internal enum class BackupArea(val token: String, @StringRes val label: Int) {
    VIEW("view", R.string.backup_area_view),
    BUTTONS("buttons", R.string.settings_group_input),
    EDITOR("editor", R.string.settings_group_files),
    STYLES("styles", R.string.settings_styles),
    TINTS("tints", R.string.settings_colour),
    COVERS("covers", R.string.backup_area_covers),
    HINTS("hints", R.string.backup_area_hints),
    BIN("bin", R.string.bin_title),
}

/** Il tipo di una preferenza, come `DataStore` la tiene e come il file la scrive. */
internal enum class PrefType(val token: String) {
    BOOLEAN("boolean"), INT("int"), LONG("long"), FLOAT("float"), STRING("string"), SET("set");

    companion object {
        /** Il tipo di un valore letto dall'archivio, o `null` se non è uno dei sei. */
        fun of(value: Any?): PrefType? = when (value) {
            is Boolean -> BOOLEAN
            is Int -> INT
            is Long -> LONG
            is Float -> FLOAT
            is String -> STRING
            is Set<*> -> SET
            else -> null
        }
    }
}

/** Una preferenza che il backup porta: il suo nome nell'archivio, il suo tipo, la sua area. */
internal class PrefKey(val name: String, val type: PrefType, val area: BackupArea)

/**
 * Tutte le preferenze che un backup porta, ognuna con la sua area.
 *
 * ⚠️⚠️ **È UN ELENCO SCRITTO A MANO, E LO TIENE ONESTO UNA PROVA E NON LA MEMORIA**: le chiavi
 * vivono in cinque oggetti diversi di `Settings.kt`, e una chiave nuova che qualcuno dimenticasse
 * qui resterebbe fuori da ogni backup senza che niente dia errore. `BackupTest` scrive l'archivio
 * con tutti i suoi scrittori e pretende che ogni chiave che trova sia qui o in [PREF_OUTSIDE], col
 * tipo giusto; e che ogni chiave di qui, tranne le due che non si scrivono più, venga scritta
 * davvero.
 * ⚠️ **Le due chiavi vecchie ci sono di proposito**, `veil` e `mark-air`: nessuno le scrive, ma
 * l'app le legge ancora come ripiego, quindi su un telefono che non ha mai salvato da quando sono
 * state sostituite sono loro a dire il valore. Un backup senza di loro rimetterebbe quello di
 * fabbrica.
 * ⚠️ **Gli avvisi entrano da [Hint]**, e non per nome: un avviso nuovo arriva nel backup per
 * costruzione.
 * ⚠️⚠️ **L'AREA DI UNA CHIAVE NON CAMBIA MAI, ANCHE SE LA SUA VOCE SI SPOSTA DI PAGINA**: un
 * backup scritto prima e uno scritto dopo la metterebbero in due voci diverse, e una versione
 * vecchia la cercherebbe in quella sbagliata. Il posto nell'interfaccia e l'area nel backup sono
 * due cose indipendenti, come il posto e la chiave nell'archivio.
 */
internal val PREF_KEYS: List<PrefKey> = buildList {
    fun area(area: BackupArea, type: PrefType, vararg names: String) {
        names.forEach { add(PrefKey(it, type, area)) }
    }
    area(
        BackupArea.VIEW, PrefType.BOOLEAN,
        "fit-grow", "info-visible", "veil", "zoom-in-menu", "sequence-reversed", "open-at-start",
        "clipboard-start", "pick-weight", "anim-counter", "list-count", "tree-hidden",
        "tree-pictures", "images-only", "clip-autoplay", "grid-names", "front-wash", "front-serif",
        "front-facts", "front-pick-all", "gpu-thumbs", "folder-count"
    )
    area(
        BackupArea.VIEW, PrefType.STRING,
        "bg-type", "bg-theme", "scale-mode", "info-position", "panel-depth", "start-folder-name",
        "folder-view", "list-text", "ui-theme", "fact-order", "recent"
    )
    area(BackupArea.VIEW, PrefType.FLOAT, "zoom-max")
    area(BackupArea.VIEW, PrefType.LONG, "start-folder")
    area(BackupArea.VIEW, PrefType.INT, "folder-columns")
    area(BackupArea.VIEW, PrefType.SET, "hidden-folders", "fact-off")
    area(BackupArea.BUTTONS, PrefType.BOOLEAN, "list-path", "pad-labels")
    area(
        BackupArea.BUTTONS, PrefType.STRING,
        "hand", "menu-order", "pick-order", "turn-order", "step-order", "mod-order", "last-mark"
    )
    area(
        BackupArea.EDITOR, PrefType.BOOLEAN,
        "editor-backup", "mark-on", "size-on", "ext-edit", "ext-download", "save-rename",
        "download-path", "bin-on"
    )
    area(
        BackupArea.EDITOR, PrefType.STRING,
        "editor-app", "editor-quality", "mark-spot", "size-mode", "bin-keep"
    )
    area(
        BackupArea.EDITOR, PrefType.INT,
        "mark-size-pct", "mark-air-tenths", "mark-air", "mark-alpha", "size-value"
    )
    area(BackupArea.TINTS, PrefType.STRING, "folder-colour")
    area(BackupArea.TINTS, PrefType.SET, "folder-tints")
    Hint.entries.forEach { add(PrefKey(it.token, PrefType.BOOLEAN, BackupArea.HINTS)) }
}

/**
 * Le preferenze che un backup **non** porta, e il perché di ognuna.
 *
 * ⚠️⚠️ **SONO PROMEMORIA DI QUESTO TELEFONO, NON SCELTE DELL'UTENTE**, e portate su un altro
 * direbbero una cosa falsa: `all-files-asked` dice che il permesso a questo telefono è già stato
 * chiesto, `download-tree` è una cartella che vale col permesso persistente di questo telefono e
 * senza di lui non si apre, `clipboard-done` e `clipboard-when` dicono quale indirizzo degli
 * appunti è già stato aperto qui, `download-seen` che cosa c'è nella cartella Download di qui.
 * ⚠️ **All'importazione non si toccano**, per la stessa ragione: sono di questo telefono anche
 * dopo.
 */
internal val PREF_OUTSIDE: Set<String> = setOf(
    "all-files-asked", "download-tree", "clipboard-done", "clipboard-when", "download-seen"
)

/** Le aree fatte di preferenze, cioè quelle che hanno una voce `prefs/<area>.json`. */
internal val PREF_AREAS: Set<BackupArea> = PREF_KEYS.map { it.area }.toSet()

/**
 * Il backup delle impostazioni: un file che solo AIV sa leggere, e che porta quello che l'app
 * ricorda, un'area per volta.
 *
 * ⚠️⚠️ **NASCE NELLA `2.93`, ED È LA TERZA RICHIESTA DEL CAMPO LIBERO DEL GIRO DELLA `2.91`**
 * (*uno o più file di testo ... ed eventuali file accessori, strutturati in modo ottimale e
 * compressi in uno ZIP criptato (leggibile solo da AIV, su richiesta anche protetto da password)*,
 * e *deve contenere letteralmente TUTTE le impostazioni dell'app che possono essere salvate, ma
 * ogni cosa su richiesta con apposita checkbox*). Il formato l'ha lasciato scegliere a me.
 *
 * ⚠️⚠️ **È UNO ZIP DENTRO UN CONTENITORE CIFRATO, E NON UNO ZIP CON LA PASSWORD**: la cifratura
 * classica dello ZIP si rompe in pochi minuti, e quella AES di WinZip in `java.util.zip` non c'è,
 * cioè vorrebbe una libreria. Qui lo ZIP si scrive com'è e passa da [SealingStream], che lo taglia
 * in segmenti da [SEGMENT] byte e li cifra uno per uno con AES-GCM. È la costruzione dei flussi
 * cifrati di Tink e di `age`, con le sole classi della piattaforma.
 * - ⚠️ **A segmenti e non in un colpo solo**: su Android un GCM non restituisce un byte finché il
 *   tag non è verificato, quindi un cestino da un gigabyte andrebbe letto tutto in memoria. Un
 *   segmento si verifica prima che ne esca un byte, e niente di quello che arriva allo ZIP è stato
 *   toccato.
 * - ⚠️⚠️ **L'ULTIMO SEGMENTO SI DICHIARA NEL NONCE, ED È QUELLO CHE FA VEDERE UN FILE TAGLIATO**:
 *   un file troncato proprio fra due segmenti sarebbe fatto di segmenti tutti validi, e senza quel
 *   segno si leggerebbe come completo. Con il segno, un file che finisce prima dell'ultimo o
 *   continua dopo di lui non si apre.
 * - ⚠️ **L'intestazione entra in ogni segmento come dato autenticato**: cambiare il modo, le
 *   iterazioni o il sale rompe la lettura invece di cambiarne il significato.
 *
 * ⚠️⚠️ **SENZA PASSWORD È UN SIGILLO E NON UNA PROTEZIONE, E LA PAGINA LO DICE**: la chiave nasce
 * da una costante che vive nell'APK, quindi un altro programma non lo apre, ma chiunque abbia AIV
 * lo importa. È il *leggibile solo da AIV* della sua richiesta; la protezione vera è la password,
 * e là la chiave nasce da PBKDF2 con [ROUNDS] iterazioni.
 *
 * ⚠️⚠️ **L'IMPORTAZIONE PRIMA LEGGE TUTTO E POI SCRIVE TUTTO**: le voci si mettono da parte in due
 * cartelle d'appoggio, si controllano, e il file si legge fino all'ultimo segmento; solo allora
 * qualcosa dell'app cambia. Un file tagliato o storto si scopre quando non è ancora cambiato
 * niente.
 * ⚠️ **Ogni area prende il posto di quella di adesso, tranne il cestino e la cronologia dei
 * ripristini**, che si aggiungono: sostituirli vorrebbe dire cancellare per sempre dei file, cioè
 * l'unica cosa che il cestino esiste per non fare.
 *
 * ⚠️⚠️ **VALE FRA VERSIONI DIVERSE DI AIV, NEI DUE VERSI, ED È SUA RICHIESTA** (2026-09-26, a
 * lavoro iniziato: *una versione di AIV più recente di quella che ha generato il backup troverà
 * 'vuote' alcune impostazioni nate dopo ... una versione di AIV più datata ... ignorerà alcune voci
 * di importazione non sapendo come trattarle, ma teoricamente potrà importare tutto ciò che è
 * importabile*). Lo tengono in piedi quattro regole.
 * - **Il formato cresce solo aggiungendo**: una voce, un campo o una chiave non cambiano mai forma
 *   né significato, e quello che cambia prende un nome nuovo. Così una versione più vecchia salta
 *   quello che non conosce e prende il resto.
 * - ⚠️⚠️ **UNA PREFERENZA CHE IL FILE NON NOMINA NON SI TOCCA**: per ogni area il file dice tutte le
 *   chiavi che conosceva, col valore oppure come assenti, cioè al valore di fabbrica. All'arrivo una
 *   chiave nata dopo il backup resta com'era sul telefono, e una che il file dichiara assente torna
 *   al valore di fabbrica: senza l'elenco delle assenti le due cose non si distinguerebbero.
 * - **L'unico cancello è la versione del contenitore**, il quinto byte dell'intestazione: sale solo
 *   il giorno che una versione vecchia non potrebbe leggere niente (un'altra cifratura, un'altra
 *   chiave), e allora la risposta giusta è 'aggiorna l'app'. Il resoconto non porta un numero di
 *   versione, perché con la prima regola non ne ha bisogno.
 * - ⚠️ **Quello che non si sa leggere non cancella niente**: un logo o una copertina in un formato
 *   che questa versione non conosce lasciano al suo posto quello che c'era. E quello che si salta
 *   si dice, nella notifica finale.
 * ⚠️ **Gli archivi del cestino e della cronologia non crescono per colonne**: le loro righe si
 *   leggono a numero fisso di campi, e un percorso ci sta in mezzo senza nessuna protezione, quindi
 *   una colonna in più le farebbe scartare a una versione vecchia. Un dato nuovo lì prende una voce
 *   sua.
 */
internal object Backup {

    /** Quanto è lunga l'intestazione: `AIVB`, versione, modo, due riservati, iterazioni, sale, nonce. */
    const val HEAD = 35

    /** I byte in chiaro di un segmento, cioè quanto si cifra e si verifica in un colpo. */
    const val SEGMENT = 64 * 1024

    /** Il tag di GCM, in byte: 128 bit, il massimo, e quello che ogni implementazione accetta. */
    const val TAG = 16

    /**
     * Le iterazioni di PBKDF2 con cui nasce la chiave di una password.
     *
     * ⚠️ **È la cifra che OWASP indica per PBKDF2 con SHA-256**, e sta nell'intestazione invece
     * che nel codice: un backup scritto oggi si apre anche il giorno che questo numero sale.
     * ⚠️ **Quanto costa su un telefono non è misurato**: la finestra mostra l'avanzamento mentre la
     * chiave nasce, perché sono i secondi in cui l'app sembra ferma.
     */
    const val ROUNDS = 600_000

    /**
     * Il tetto delle iterazioni in lettura.
     *
     * ⚠️ **Serve contro un file fatto apposta**: senza tetto, un'intestazione con due miliardi di
     * iterazioni terrebbe il telefono a contare per ore prima di dire che la password è sbagliata.
     */
    const val ROUNDS_MAX = 5_000_000

    const val MODE_SEAL = 0
    const val MODE_PASSWORD = 1

    /** Perché un file non si apre. */
    enum class Reason {
        /** Non è un backup di AIV, o è stato scritto a mano in un modo che AIV non scrive. */
        BAD,

        /**
         * Il contenitore viene da una versione più nuova, che lo scrive in un modo che questa non sa
         * aprire.
         *
         * ⚠️ **Solo il contenitore**: un contenuto più nuovo si legge per quello che si conosce, ed è
         * la seconda regola in testa a questo oggetto.
         */
        NEWER,

        /** La password non è giusta, oppure il primo segmento è rovinato: GCM non li distingue. */
        WRONG,

        /** Il file è tagliato o toccato dopo il primo segmento. */
        BROKEN
    }

    /** Un file che non si apre, col suo perché. */
    class Failure(val reason: Reason) : IOException(reason.name)

    /** L'intestazione di un file: in chiaro, perché dice come aprire il resto. */
    class Head(
        val mode: Int,
        val rounds: Int,
        val salt: ByteArray,
        val prefix: ByteArray,
        val bytes: ByteArray
    ) {
        /** Se per aprirlo serve una password. */
        val locked: Boolean get() = mode == MODE_PASSWORD
    }

    /** Un file di cui si ha la chiave: l'intestazione letta e quello che la password ha dato. */
    class Opened internal constructor(val head: Head, internal val key: SecretKeySpec)

    /**
     * Che cosa dice un backup di sé, prima di aprirlo tutto.
     *
     * @property foreign quante aree del file questa versione non conosce: vengono da una più nuova.
     */
    class Manifest(val app: String, val created: Long, val areas: Set<BackupArea>, val foreign: Int)

    /**
     * Com'è andata un'importazione: le aree da rimettere, quelle che sono entrate davvero, e se il
     * file portava qualcosa che questa versione non sa trattare.
     *
     * ⚠️ **Le prime due si confrontano**, ed è la differenza fra 'importato' e 'importato a metà':
     * una copia che non riesce (lo spazio finito, un logo che non si disegna) toglie la sua area
     * dalla seconda e non dalla prima.
     * ⚠️ **[skipped] non fa un'importazione a metà**: quello che si salta non si poteva importare,
     * e le aree sono entrate con tutto quello che si sapeva leggere. Lo si dice lo stesso, perché
     * aggiornando l'app quelle voci si potrebbero importare.
     */
    class Outcome(val wanted: Set<BackupArea>, val applied: Set<BackupArea>, val skipped: Boolean)

    /** Il tipo del file per il selettore di sistema: un formato suo, che nessun'altra app apre. */
    const val MIME = "application/octet-stream"

    /** Il nome proposto per un backup scritto oggi. */
    fun fileName(now: Long): String =
        "AIV-backup-${SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(Date(now))}.aivbackup"

    // ── Scrittura ────────────────────────────────────────────────────────────

    /**
     * Scrive un backup delle aree scelte su [out], e lo chiude.
     *
     * ⚠️⚠️ **SE QUALCOSA VA STORTO, L'ULTIMO SEGMENTO NON SI SCRIVE**: il file resta com'è arrivato
     * fin lì, e un file senza il suo ultimo segmento non si apre. Chi chiama lo cancella; se non ci
     * riesce, quello che resta è un file che dice di essere rotto invece di un backup a metà che si
     * importa come se fosse intero.
     * ⚠️ **Un file del cestino che sparisce mentre si copia si salta**: il cestino si può svuotare
     * da un'altra schermata nel frattempo, e un backup che fallisse per questo non si potrebbe fare
     * mai con un cestino che cambia.
     *
     * @param password `null` per il solo sigillo. ⚠️ **Non vuota**: una chiave di HMAC vuota non
     *   esiste, e la finestra lo impedisce prima.
     * @param rounds le iterazioni di PBKDF2: di serie [ROUNDS]. ⚠️ **Le prove ne usano meno**, o
     *   ogni caso costerebbe un secondo per niente.
     * @param onBytes quanti byte sono già usciti, per l'avanzamento.
     */
    suspend fun write(
        context: Context,
        out: OutputStream,
        areas: Set<BackupArea>,
        password: String?,
        rounds: Int = ROUNDS,
        onBytes: (Long) -> Unit = {}
    ) = pack(out, password, rounds, onBytes) { zip ->
        putText(zip, MANIFEST, manifestJson(areas).toString())
        val stored = storedPreferences(context).asMap().entries
            .associate { it.key.name to it.value }
        for (area in BackupArea.entries) {
            if (area !in areas) continue
            if (area in PREF_AREAS) putText(zip, prefsName(area), prefsJson(stored, area))
            when (area) {
                BackupArea.EDITOR -> Watermark.file(context)?.let {
                    putFile(zip, MARK_DIR + it.name, it)
                }
                BackupArea.STYLES -> putText(zip, STYLES_ENTRY, Presets.export(context))
                BackupArea.COVERS -> FolderCovers.files(context).forEach {
                    putFile(zip, COVERS_DIR + it.name, it)
                }
                BackupArea.BIN -> {
                    val (records, files) = Bin.snapshot(context)
                    putText(zip, BIN_INDEX, Bin.text(records))
                    putText(zip, BIN_RESTORED, History.text(History.snapshot(context)))
                    files.forEach { putFile(zip, BIN_FILES + it.name, it) }
                }
                else -> Unit
            }
        }
    }

    /**
     * Sigilla quello che [body] scrive in uno ZIP, e chiude [out]: il contenitore, senza il
     * contenuto.
     *
     * ⚠️⚠️ **È UNA FUNZIONE A SÉ PERCHÉ LE PROVE CI METTONO VOCI SCRITTE A MANO**, cioè i file che
     * una versione più nuova potrebbe scrivere (un'area, una chiave o un formato di copertina che
     * questa non conosce). Con un secondo contenitore scritto nelle prove, quelle misurerebbero un
     * file che l'app non fa; con questo, la sola cosa finta è il contenuto.
     * ⚠️ **Se [body] va storto l'ultimo segmento non si scrive**, e il perché è su [write].
     */
    internal suspend fun pack(
        out: OutputStream,
        password: String?,
        rounds: Int = ROUNDS,
        onBytes: (Long) -> Unit = {},
        body: suspend (ZipOutputStream) -> Unit
    ) = withContext(Dispatchers.IO) {
        require(password == null || (password.isNotEmpty() && rounds in 1..ROUNDS_MAX))
        val counted = CountingOut(out, onBytes)
        val mode = if (password == null) MODE_SEAL else MODE_PASSWORD
        val salt = random(SALT)
        val prefix = random(PREFIX)
        val head = headBytes(mode, if (password == null) 0 else rounds, salt, prefix)
        val key = keyOf(mode, if (password == null) 0 else rounds, salt, password)
        val seal = SealingStream(counted, key, head, prefix)
        try {
            counted.write(head)
            val zip = ZipOutputStream(seal, Charsets.UTF_8)
            body(zip)
            zip.close()
        } catch (e: Throwable) {
            seal.abandon()
            throw e
        }
    }

    /**
     * Quanti byte un backup delle aree scelte peserà, più o meno: per l'avanzamento.
     *
     * ⚠️ **Più o meno**: i testi si comprimono e i file no, quindi la stima sta un poco sopra, e la
     * barra arriva in fondo un poco prima di dove la mette.
     */
    suspend fun estimate(context: Context, areas: Set<BackupArea>): Long {
        var total = ESTIMATE_BASE
        for (area in areas) total += weight(context, area)
        if (BackupArea.STYLES in areas) total += withContext(Dispatchers.IO) {
            Presets.export(context).length.toLong()
        }
        return total + total / SEGMENT * TAG
    }

    /**
     * Quanto pesano i file di un'area, cioè la parte di un backup che non è testo.
     *
     * ⚠️ **Serve anche alla pagina**, che lo scrive accanto al cestino: è la sola casella che può
     * valere un gigabyte, e chi la spunta per mandare un backup su Drive lo deve sapere prima.
     */
    suspend fun weight(context: Context, area: BackupArea): Long = withContext(Dispatchers.IO) {
        when (area) {
            BackupArea.EDITOR -> Watermark.file(context)?.length() ?: 0L
            BackupArea.COVERS -> FolderCovers.files(context).sumOf { it.length() }
            BackupArea.BIN -> Bin.snapshot(context).second.sumOf { it.length() }
            else -> 0L
        }
    }

    /**
     * Il resoconto: che cosa è il file, chi l'ha scritto e quando, e quali aree porta.
     *
     * ⚠️ **Nessun numero di versione**, e non è una dimenticanza: il contenuto cresce solo
     * aggiungendo, quindi non c'è niente che un numero debba fermare (vedi la testa di questo
     * oggetto). La versione dell'app c'è, e serve a chi legge la conferma.
     */
    private fun manifestJson(areas: Set<BackupArea>): JSONObject = JSONObject()
        .put("format", FORMAT)
        .put("app", BuildConfig.VERSION_NAME)
        .put("created", System.currentTimeMillis())
        .put("areas", JSONArray(BackupArea.entries.filter { it in areas }.map { it.token }))

    /**
     * Le preferenze di un'area come testo: quelle che hanno un valore, e quelle che non ce l'hanno.
     *
     * ⚠️⚠️ **LE CHIAVI SENZA VALORE SI ELENCANO IN `absent`, ED È QUELLO CHE RENDE IL FILE LEGGIBILE
     * DA UNA VERSIONE PIÙ NUOVA**: una chiave che manca dall'archivio vale il valore di fabbrica, e
     * all'arrivo deve tornare tale; una chiave nata dopo il backup invece non c'è né di qua né di là,
     * e all'arrivo deve restare com'è. Senza l'elenco, per chi importa le due cose sarebbero lo stesso
     * silenzio.
     * ⚠️⚠️ **I NUMERI VIAGGIANO COME STRINGHE**: un numero JSON lo tipizza chi legge (`org.json`
     * ridà lo stesso intero come `Int` o come `Long` secondo la grandezza, e un decimale come
     * `Double`), e altri lettori arrotondano gli interi oltre i cinquantatré bit. Una stringa torna
     * com'è, e il tipo lo dice il campo accanto.
     * ⚠️ **Un valore col tipo sbagliato si scrive come assente**, e non capita: la prova sulla
     * copertura delle chiavi confronta proprio i tipi. Se capitasse, l'app quel valore non lo sa
     * leggere, e all'arrivo il valore di fabbrica è l'unica cosa sensata da rimettere.
     */
    private fun prefsJson(stored: Map<String, Any>, area: BackupArea): String {
        val keys = JSONArray()
        val absent = JSONArray()
        for (k in PREF_KEYS) {
            if (k.area != area) continue
            val value = stored[k.name]
            if (value == null || PrefType.of(value) != k.type) {
                absent.put(k.name)
                continue
            }
            keys.put(
                JSONObject()
                    .put("name", k.name)
                    .put("type", k.type.token)
                    .put("value", encode(k.type, value))
            )
        }
        return JSONObject().put("keys", keys).put("absent", absent).toString()
    }

    private fun encode(type: PrefType, value: Any): Any = when (type) {
        PrefType.BOOLEAN -> value as Boolean
        PrefType.INT, PrefType.LONG, PrefType.FLOAT -> value.toString()
        PrefType.STRING -> value as String
        PrefType.SET -> JSONArray((value as Set<*>).map { it.toString() }.sorted())
    }

    /** Una voce di testo, compressa. */
    private fun putText(zip: ZipOutputStream, name: String, text: String) {
        zip.setLevel(Deflater.DEFAULT_COMPRESSION)
        zip.putNextEntry(ZipEntry(name))
        zip.write(text.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    /**
     * Una voce che copia un file, senza comprimerlo.
     *
     * ⚠️⚠️ **SENZA COMPRESSIONE, E NON PER RISPARMIARE TEMPO**: immagini e video sono già compressi,
     * e un file che esce della misura con cui entra è quello che permette all'importazione di
     * accorgersi di una voce gonfiata apposta (vedi [Meter]).
     * ⚠️ **Il file si apre prima della voce**: uno sparito nel frattempo non lascia nello ZIP una
     * voce vuota con il suo nome.
     */
    private suspend fun putFile(zip: ZipOutputStream, name: String, source: File) {
        val input = runCatching { source.inputStream() }.getOrNull() ?: return
        input.use {
            zip.setLevel(Deflater.NO_COMPRESSION)
            zip.putNextEntry(ZipEntry(name))
            pour(it, zip, null)
            zip.closeEntry()
        }
    }

    // ── Lettura ──────────────────────────────────────────────────────────────

    /**
     * L'intestazione di un file, e il primo giudizio su di lui.
     *
     * ⚠️ **La versione si guarda prima del resto**: un file scritto da una versione più nuova può
     * avere un'intestazione che questa non capisce, e la risposta giusta è 'aggiorna l'app', non
     * 'non è un backup'.
     * ⚠️⚠️ **È L'UNICO CANCELLO DI VERSIONE DEL FORMATO, E SI ALZA DI RADO**: quel numero sale solo
     * quando cambia il modo di aprire il file (la cifratura, la chiave, i segmenti), cioè quando una
     * versione vecchia non potrebbe leggere niente. Una voce nuova, un campo nuovo o un'area nuova
     * non lo toccano.
     */
    fun head(input: InputStream): Head {
        val b = ByteArray(HEAD)
        var got = 0
        while (got < HEAD) {
            val n = input.read(b, got, HEAD - got)
            if (n < 0) throw Failure(Reason.BAD)
            got += n
        }
        if (MAGIC.indices.any { b[it] != MAGIC[it] }) throw Failure(Reason.BAD)
        val version = b[4].toInt() and 0xff
        if (version > VERSION) throw Failure(Reason.NEWER)
        if (version != VERSION) throw Failure(Reason.BAD)
        if (b[6] != ZERO || b[7] != ZERO) throw Failure(Reason.BAD)
        val mode = b[5].toInt() and 0xff
        val rounds = ((b[8].toInt() and 0xff) shl 24) or ((b[9].toInt() and 0xff) shl 16) or
            ((b[10].toInt() and 0xff) shl 8) or (b[11].toInt() and 0xff)
        when (mode) {
            MODE_SEAL -> if (rounds != 0) throw Failure(Reason.BAD)
            MODE_PASSWORD -> if (rounds !in 1..ROUNDS_MAX) throw Failure(Reason.BAD)
            else -> throw Failure(Reason.BAD)
        }
        return Head(
            mode = mode,
            rounds = rounds,
            salt = b.copyOfRange(12, 12 + SALT),
            prefix = b.copyOfRange(12 + SALT, HEAD),
            bytes = b
        )
    }

    /**
     * La chiave di un file, dalla sua intestazione e dalla password.
     *
     * ⚠️ **È il passo lento**, PBKDF2, e per questo è a sé: il resoconto e l'importazione leggono
     * il file due volte, e la chiave nasce una volta sola.
     * ⚠️ **Non dice se la password è giusta**: lo dice il primo segmento, che con la chiave
     * sbagliata non si verifica.
     */
    fun open(head: Head, password: String?): Opened {
        if (head.locked && password.isNullOrEmpty()) throw Failure(Reason.WRONG)
        return Opened(head, keyOf(head.mode, head.rounds, head.salt, password))
    }

    /**
     * Che cosa dice un backup di sé: la prima voce, e nient'altro.
     *
     * ⚠️ **Legge solo l'inizio**, quindi non dice se il file è intero: quello lo scopre [restore],
     * che lo legge tutto prima di toccare qualcosa.
     */
    fun manifest(input: InputStream, opened: Opened): Manifest {
        val plain = unseal(input, opened)
        val zip = ZipInputStream(plain, Charsets.UTF_8)
        val first = nextEntry(zip) ?: throw Failure(Reason.BAD)
        if (first.name != MANIFEST) throw Failure(Reason.BAD)
        return readManifest(readText(zip, MANIFEST_MAX))
    }

    /**
     * Rimette le aree scelte di un backup, che devono essere anche nel suo resoconto.
     *
     * ⚠️⚠️ **IN DUE TEMPI, E IL SECONDO NON SI INTERROMPE**: prima si legge e si controlla tutto
     * (e lì annullare lascia l'app com'era), poi si applica, e quel passo va fino in fondo anche se
     * chi aspetta se ne va, perché un'importazione fermata a metà sarebbe un'app fatta di due
     * backup.
     * ⚠️ **Un'area che il resoconto nomina e il file non porta non si tocca**, e resta fra quelle
     * attese: il riscontro dice 'a metà' invece di tacere. Le copertine sono l'eccezione, perché
     * non hanno una voce che ne dica la presenza: un backup senza copertine dice che non ce n'erano.
     *
     * @param chosen le caselle spuntate. ⚠️ Si importano quelle che sono anche nel resoconto.
     * @param onBytes quanti byte del file sono già stati letti, per l'avanzamento.
     */
    suspend fun restore(
        context: Context,
        input: InputStream,
        opened: Opened,
        chosen: Set<BackupArea>,
        onBytes: (Long) -> Unit = {}
    ): Outcome = withContext(Dispatchers.IO) {
        sweep(context)
        try {
            val found = gather(context, CountingIn(input, onBytes), opened, chosen)
            withContext(NonCancellable) {
                Outcome(found.wanted, apply(context, found), skipped = found.skipped > 0)
            }
        } finally {
            withContext(NonCancellable) { sweep(context) }
        }
    }

    /**
     * Toglie le cartelle d'appoggio di un'importazione.
     *
     * ⚠️ **Si chiama anche all'avvio dell'app**: un'importazione che il sistema ha ucciso a metà
     * lascerebbe là dentro delle copie che nessuno legge più, e il cestino di un altro telefono può
     * pesare quanto le sue immagini.
     */
    suspend fun sweep(context: Context) = withContext(Dispatchers.IO) {
        runCatching { stageHome(context).deleteRecursively() }
        runCatching { stageBin(context).deleteRecursively() }
        Unit
    }

    /**
     * Le preferenze di un'area lette dal file: quelle da scrivere, quelle da riportare al valore di
     * fabbrica, e quante il file ne porta che questa versione non sa trattare.
     *
     * ⚠️ **Le chiavi che non compaiono in nessuno dei due elenchi non si toccano**: sono nate dopo
     * il backup, ed è la metà della sua richiesta sulle versioni (vedi la testa di questo oggetto).
     */
    private class Prefs(
        val values: List<Pair<PrefKey, Any>>,
        val absent: List<PrefKey>,
        val skipped: Int
    )

    /** Quello che un'importazione ha messo da parte, prima di applicarlo. */
    private class Found(val wanted: Set<BackupArea>) {
        val prefs = HashMap<BackupArea, Prefs>()
        var styles: String? = null
        val covers = ArrayList<File>()

        /** Le cartelle di cui il file porta una copertina in un formato che qui non si conosce. */
        val coversKept = HashSet<Long>()
        var logo: File? = null

        /** Se il file porta un logo in un formato che qui non si conosce. */
        var logoForeign = false
        var records: List<Bin.Record>? = null
        val binFiles = ArrayList<File>()
        var restored: List<History.Row> = emptyList()

        /** Quante cose il file porta che questa versione non sa trattare. */
        var skipped = 0
    }

    /**
     * Legge il file intero, e mette da parte le voci delle aree da rimettere.
     *
     * ⚠️⚠️ **QUELLO CHE QUESTA VERSIONE NON CONOSCE SI SALTA, QUELLO CHE NESSUNA VERSIONE SCRIVE
     * NO**: una voce, un'area, una chiave o un tipo sconosciuti vengono da una versione più nuova, che
     * ha il diritto di aggiungere, e si saltano contandoli. Un nome che esce dalla sua cartella, un
     * doppione o una struttura che non si legge invece non li scrive nessuna versione, cioè vogliono
     * dire un file fatto a mano, e allora non si importa niente.
     */
    private suspend fun gather(
        context: Context,
        raw: CountingIn,
        opened: Opened,
        chosen: Set<BackupArea>
    ): Found {
        val plain = unseal(raw, opened)
        val zip = ZipInputStream(plain, Charsets.UTF_8)
        val first = nextEntry(zip) ?: throw Failure(Reason.BAD)
        if (first.name != MANIFEST) throw Failure(Reason.BAD)
        val manifest = readManifest(readText(zip, MANIFEST_MAX))
        val found = Found(chosen intersect manifest.areas)
        found.skipped += manifest.foreign
        val want = found.wanted
        val meter = Meter(raw)
        val seen = HashSet<String>()
        while (true) {
            currentCoroutineContext().ensureActive()
            val entry = nextEntry(zip) ?: break
            val name = entry.name
            if (entry.isDirectory) continue
            if (!seen.add(name)) throw Failure(Reason.BAD)
            val area = PREF_AREAS.firstOrNull { prefsName(it) == name }
            when {
                area != null -> if (area in want) {
                    val letto = readPrefs(readText(zip, PREFS_MAX), area)
                    found.prefs[area] = letto
                    found.skipped += letto.skipped
                }
                name == STYLES_ENTRY -> if (BackupArea.STYLES in want) {
                    val text = readText(zip, STYLES_MAX)
                    if (!Presets.readable(text)) throw Failure(Reason.BAD)
                    found.styles = text
                }
                name.startsWith(COVERS_DIR) -> if (BackupArea.COVERS in want) {
                    val file = name.removePrefix(COVERS_DIR)
                    if (!simple(file)) throw Failure(Reason.BAD)
                    if (FolderCovers.named(file)) {
                        found.covers += stage(zip, File(stageHome(context), name), COVER_MAX, meter)
                    } else {
                        // ⚠️ Un formato di copertina che questa versione non conosce: si salta, e la
                        // copertina che quella cartella ha qui resta dov'è.
                        found.skipped++
                        FolderCovers.ownerOf(file)?.let { found.coversKept += it }
                    }
                }
                name.startsWith(MARK_DIR) -> if (BackupArea.EDITOR in want) {
                    val file = name.removePrefix(MARK_DIR)
                    if (!simple(file)) throw Failure(Reason.BAD)
                    if (file in MARK_NAMES) {
                        if (found.logo != null) throw Failure(Reason.BAD)
                        found.logo = stage(zip, File(stageHome(context), name), Watermark.MAX_BYTES, meter)
                    } else {
                        // ⚠️ Un formato di logo che questa versione non conosce: si salta, e il logo
                        // di qui non si cancella (vedi [apply]).
                        found.skipped++
                        found.logoForeign = true
                    }
                }
                name == BIN_INDEX -> if (BackupArea.BIN in want) {
                    found.records = Bin.records(readText(zip, INDEX_MAX)).onEach {
                        if (!simple(it.name) || !safeOrigin(context, it.origin)) {
                            throw Failure(Reason.BAD)
                        }
                    }
                }
                name == BIN_RESTORED -> if (BackupArea.BIN in want) {
                    found.restored = History.rows(readText(zip, RESTORED_MAX)).onEach {
                        if (!it.path.startsWith('/') || it.path.contains('\u0000')) {
                            throw Failure(Reason.BAD)
                        }
                    }
                }
                name.startsWith(BIN_FILES) -> if (BackupArea.BIN in want) {
                    val file = name.removePrefix(BIN_FILES)
                    if (!simple(file)) throw Failure(Reason.BAD)
                    // ⚠️ Una cartella numerata per file: la casa del cestino vive su un archivio che
                    // può non distinguere le maiuscole, e due nomi che differiscono solo per quelle
                    // finirebbero sullo stesso file d'appoggio.
                    val slot = File(File(stageBin(context), "files"), found.binFiles.size.toString())
                    found.binFiles += stage(zip, File(slot, file), -1L, meter)
                }
                // ⚠️ Una voce che questa versione non conosce viene da una più nuova: si salta, e si
                // conta. ⚠️ Le voci delle aree non scelte invece non contano, perché le ha lasciate
                // fuori chi importa.
                else -> found.skipped++
            }
        }
        // ⚠️⚠️ Il resto del file si legge fino in fondo, ed è qui che un file tagliato si scopre:
        // lo ZIP si ferma al suo indice, e senza questo giro l'ultimo segmento non si guarderebbe.
        val rest = ByteArray(BUFFER)
        while (plain.read(rest) >= 0) currentCoroutineContext().ensureActive()
        if (!plain.ended) throw Failure(Reason.BROKEN)
        return found
    }

    /**
     * Applica quello che [gather] ha messo da parte, e dice quali aree sono entrate.
     *
     * ⚠️ **Prima i file e poi le preferenze**, in una transazione sola: le preferenze sono quello
     * che l'app rilegge da sé appena cambiano, e un'app che si ridisegna con le impostazioni nuove
     * deve trovare già al loro posto le copertine e il logo di cui parlano.
     * ⚠️ **Un'area che non riesce non ferma le altre**, e manca dal risultato.
     */
    private suspend fun apply(context: Context, found: Found): Set<BackupArea> {
        val done = HashSet<BackupArea>()
        found.styles?.let { text ->
            if (runCatching { Presets.load(context, text) }.getOrDefault(false)) {
                done += BackupArea.STYLES
            }
        }
        if (BackupArea.COVERS in found.wanted) {
            val entrate = runCatching {
                FolderCovers.adopt(context, found.covers, keep = found.coversKept)
            }.getOrDefault(false)
            if (entrate) done += BackupArea.COVERS
        }
        // ⚠️ Il logo va con le preferenze dell'editor, e solo se quelle ci sono: rimesse senza di
        // lui direbbero 'firma accesa' su un'app senza firma, o il contrario.
        var logoOk = true
        if (BackupArea.EDITOR in found.prefs) {
            val logo = found.logo
            logoOk = runCatching {
                when {
                    logo != null -> Watermark.adopt(context, Uri.fromFile(logo))
                    // ⚠️ Un logo che questa versione non sa leggere non cancella quello che c'è:
                    // il file un logo ce l'aveva, e toglierlo vorrebbe dire rimettere un'altra cosa.
                    found.logoForeign -> true
                    else -> Watermark.forget(context).let { true }
                }
            }.getOrDefault(false)
        }
        found.records?.let { records ->
            val byName = records.associateBy { it.name }
            val pairs = found.binFiles.map { it to byName[it.name] }
            val left = runCatching { Bin.adopt(context, pairs) }.getOrDefault(pairs.size)
            val rows = runCatching { History.merge(context, found.restored) }.isSuccess
            if (left == 0 && rows) done += BackupArea.BIN
        }
        if (found.prefs.isNotEmpty()) {
            val written = runCatching {
                rewritePreferences(context) { p ->
                    // ⚠️⚠️ Si tolgono le sole chiavi che il file dichiara assenti, e non tutte quelle
                    // dell'area: una chiave che il file non nomina è nata dopo il backup, e resta
                    // com'è (è la sua richiesta sulle versioni, in testa a questo oggetto).
                    for (letto in found.prefs.values) {
                        letto.absent.forEach { remove(p, it) }
                        letto.values.forEach { (key, value) -> put(p, key, value) }
                    }
                    // ⚠️ Un indicatore mancante lo deciderebbe la migrazione al prossimo avvio, e
                    // su un archivio pieno direbbe 'angolo': scriverlo adesso fa dire la stessa cosa
                    // a questo avvio e al prossimo. Un backup di AIV lo porta sempre.
                    if (p[LAST_MARK] == null) p[LAST_MARK] = LastMark.CORNER.token
                }
            }.isSuccess
            if (written) {
                done += found.prefs.keys
                if (!logoOk) done -= BackupArea.EDITOR
            }
        }
        return done
    }

    private fun remove(p: MutablePreferences, key: PrefKey) {
        // ⚠️ Una chiave si riconosce dal nome e non dal tipo: questa toglie il valore qualunque
        // tipo abbia nell'archivio.
        p.remove(booleanPreferencesKey(key.name))
    }

    @Suppress("UNCHECKED_CAST")
    private fun put(p: MutablePreferences, key: PrefKey, value: Any) {
        when (key.type) {
            PrefType.BOOLEAN -> p[booleanPreferencesKey(key.name)] = value as Boolean
            PrefType.INT -> p[intPreferencesKey(key.name)] = value as Int
            PrefType.LONG -> p[longPreferencesKey(key.name)] = value as Long
            PrefType.FLOAT -> p[floatPreferencesKey(key.name)] = value as Float
            PrefType.STRING -> p[stringPreferencesKey(key.name)] = value as String
            PrefType.SET -> p[stringSetPreferencesKey(key.name)] = value as Set<String>
        }
    }

    /**
     * Le preferenze di un'area lette da una voce, già controllate.
     *
     * ⚠️⚠️ **SI SALTA TUTTO QUELLO CHE QUESTA VERSIONE NON SA METTERE AL SUO POSTO, E SI CONTA**:
     * una chiave sconosciuta, una di un'altra area, una col tipo diverso o un valore che non si
     * legge vengono da una versione più nuova, e il resto dell'area entra lo stesso. Il tipo che
     * cambia in questa app prende una chiave nuova, quindi un tipo diverso qui vuol dire un'altra
     * versione e non un file toccato.
     * ⚠️ **Un'assente sconosciuta invece non conta**: è un'impostazione di un'altra versione rimasta
     * al valore di fabbrica, e saltarla non perde niente.
     * ⚠️ **Un nome che compare due volte, nei due elenchi o nello stesso, resta un rifiuto**: nessuna
     * versione lo scrive, e dei due valori non si saprebbe quale vale.
     */
    private fun readPrefs(text: String, area: BackupArea): Prefs {
        val o = json(text)
        val keys = o.optJSONArray("keys") ?: throw Failure(Reason.BAD)
        val values = ArrayList<Pair<PrefKey, Any>>()
        val absent = ArrayList<PrefKey>()
        val names = HashSet<String>()
        var skipped = 0
        for (i in 0 until keys.length()) {
            val k = keys.optJSONObject(i) ?: throw Failure(Reason.BAD)
            val name = k.optString("name")
            if (name.isEmpty() || !names.add(name)) throw Failure(Reason.BAD)
            val known = PREF_KEYS.firstOrNull { it.name == name && it.area == area }
            val value = known
                ?.takeIf { k.optString("type") == it.type.token }
                ?.let { decode(it.type, k.opt("value")) }
            if (known == null || value == null) {
                skipped++
                continue
            }
            values += known to value
        }
        // ⚠️ Un elenco che manca vale vuoto: il file dice soltanto meno cose.
        o.optJSONArray("absent")?.let { list ->
            for (i in 0 until list.length()) {
                val name = list.opt(i) as? String ?: throw Failure(Reason.BAD)
                if (name.isEmpty() || !names.add(name)) throw Failure(Reason.BAD)
                PREF_KEYS.firstOrNull { it.name == name && it.area == area }?.let { absent += it }
            }
        }
        return Prefs(values, absent, skipped)
    }

    private fun decode(type: PrefType, raw: Any?): Any? = when (type) {
        PrefType.BOOLEAN -> raw as? Boolean
        PrefType.INT -> (raw as? String)?.toIntOrNull()
        PrefType.LONG -> (raw as? String)?.toLongOrNull()
        PrefType.FLOAT -> (raw as? String)?.toFloatOrNull()?.takeIf { it.isFinite() }
        PrefType.STRING -> raw as? String
        PrefType.SET -> (raw as? JSONArray)?.let { a ->
            (0 until a.length()).map { a.opt(it) as? String ?: return null }.toSet()
        }
    }

    /**
     * Il resoconto letto.
     *
     * ⚠️⚠️ **NON C'È NESSUN CANCELLO DI VERSIONE, E FINO A QUI C'ERA**: un resoconto scritto da una
     * versione più nuova si legge per quello che si conosce, che è la sua richiesta (una versione
     * più vecchia *potrà importare tutto ciò che è importabile*). Un campo `version`, se c'è, non si
     * guarda.
     * ⚠️ **Un'area sconosciuta viene da una versione più nuova**: non si importa, non ferma le altre,
     * e si conta in [Manifest.foreign].
     */
    private fun readManifest(text: String): Manifest {
        val o = json(text)
        if (o.optString("format") != FORMAT) throw Failure(Reason.BAD)
        val list = o.optJSONArray("areas") ?: throw Failure(Reason.BAD)
        val tokens = (0 until list.length()).map { list.optString(it) }
        val areas = tokens.mapNotNull { t -> BackupArea.entries.firstOrNull { it.token == t } }.toSet()
        val foreign = tokens.count { t -> BackupArea.entries.none { it.token == t } }
        return Manifest(o.optString("app"), o.optLong("created", 0L), areas, foreign)
    }

    private fun json(text: String): JSONObject = try {
        JSONObject(text)
    } catch (e: JSONException) {
        throw Failure(Reason.BAD)
    }

    /**
     * La voce dopo, con gli errori dello ZIP tradotti.
     *
     * ⚠️ **Un errore del contenitore passa com'è**: dice se la password è sbagliata o se il file è
     * tagliato, e tradotto in 'non è un backup' direbbe una cosa falsa. Un errore dello ZIP invece
     * arriva da dentro un segmento già verificato, cioè da un file scritto apposta.
     */
    private fun nextEntry(zip: ZipInputStream): ZipEntry? = try {
        zip.nextEntry
    } catch (e: Failure) {
        throw e
    } catch (e: ZipException) {
        throw Failure(Reason.BAD)
    } catch (e: EOFException) {
        throw Failure(Reason.BAD)
    } catch (e: IllegalArgumentException) {
        throw Failure(Reason.BAD)
    }

    /** Il testo della voce in corso, entro un tetto: un testo più lungo non l'ha scritto AIV. */
    private fun readText(zip: ZipInputStream, cap: Long): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER)
        var total = 0L
        while (true) {
            val n = readZip(zip, buffer)
            if (n < 0) break
            total += n
            if (total > cap) throw Failure(Reason.BAD)
            out.write(buffer, 0, n)
        }
        return out.toString(Charsets.UTF_8.name())
    }

    /**
     * Copia la voce in corso in un file d'appoggio, e lo restituisce.
     *
     * @param cap il tetto in byte, o `-1` per nessun tetto (i file del cestino).
     */
    private suspend fun stage(zip: ZipInputStream, to: File, cap: Long, meter: Meter): File {
        val dir = to.parentFile
        if (dir == null || !(dir.isDirectory || dir.mkdirs())) throw IOException("appoggio")
        to.outputStream().use { out ->
            val buffer = ByteArray(BUFFER)
            var total = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val n = readZip(zip, buffer)
                if (n < 0) break
                total += n
                if (cap >= 0 && total > cap) throw Failure(Reason.BAD)
                meter.pour(n)
                out.write(buffer, 0, n)
            }
        }
        return to
    }

    /**
     * Una lettura dalla voce in corso, con gli errori dello ZIP tradotti come in [nextEntry].
     *
     * ⚠️ **Anche la fine anticipata**: dentro un contenitore intero, uno ZIP che finisce a metà di
     * una voce l'ha scritto qualcuno apposta, e `ZipInputStream` lo dice con un `EOFException`.
     */
    private fun readZip(zip: ZipInputStream, buffer: ByteArray): Int = try {
        zip.read(buffer)
    } catch (e: Failure) {
        throw e
    } catch (e: ZipException) {
        throw Failure(Reason.BAD)
    } catch (e: EOFException) {
        throw Failure(Reason.BAD)
    }

    /**
     * Se un nome si può dare a un file così com'è: niente cartelle, niente salite, niente
     * caratteri che l'archivio del cestino usa come separatori.
     */
    private fun simple(name: String): Boolean =
        name.isNotEmpty() && name != "." && name != ".." &&
            name.none { it == '/' || it == '\\' || it == '\u0000' || it == '\t' || it == '\n' || it == '\r' }

    /**
     * Se un'origine del cestino è un posto in cui 'Ripristina' può rimettere un file.
     *
     * ⚠️⚠️ **LE CARTELLE DELL'APP SONO FUORI**, ed è la ragione per cui questa riga esiste: il
     * ripristino scrive dove l'origine dice, e un file scritto apposta potrebbe mandare un'immagine
     * dentro la casa dell'app, cioè accanto agli stili e alle preferenze. Un backup di AIV non lo
     * fa mai, perché là dentro il MediaStore non guarda e niente ne esce verso il cestino.
     */
    private fun safeOrigin(context: Context, origin: String): Boolean {
        if (!origin.startsWith('/') || origin.contains('\u0000')) return false
        if (origin.split('/').any { it == ".." || it == "." }) return false
        val homes = listOfNotNull(
            context.applicationInfo.dataDir,
            context.filesDir.parentFile?.absolutePath,
            context.getExternalFilesDir(null)?.parentFile?.absolutePath,
            "/data/data/${context.packageName}"
        )
        // ⚠️ La casa esterna ha più di un nome (`/sdcard`, `/storage/emulated/0`), e tutti finiscono
        // con la stessa coda: quella si riconosce qualunque sia il volume.
        if (origin.contains("/Android/data/${context.packageName}/")) return false
        return homes.none { origin == it || origin.startsWith("$it/") }
    }

    private fun stageHome(context: Context): File = File(context.filesDir, STAGE_HOME)

    /**
     * L'appoggio del cestino, **accanto** al cestino: sullo stesso volume lo spostamento finale è
     * una rinomina, mentre da un altro sarebbe una seconda copia di tutto.
     */
    private fun stageBin(context: Context): File = File(Bin.dir(context).parentFile, STAGE_BIN)

    // ── Il contenitore ───────────────────────────────────────────────────────

    private fun unseal(input: InputStream, opened: Opened): OpeningStream {
        val head = head(input)
        // ⚠️ Il file letto adesso deve essere quello di cui si ha la chiave: cambiato fra le due
        // letture, non si apre.
        if (!head.bytes.contentEquals(opened.head.bytes)) throw Failure(Reason.BROKEN)
        return OpeningStream(input, opened.key, head)
    }

    private fun headBytes(mode: Int, rounds: Int, salt: ByteArray, prefix: ByteArray): ByteArray {
        val b = ByteArray(HEAD)
        MAGIC.copyInto(b, 0)
        b[4] = VERSION.toByte()
        b[5] = mode.toByte()
        b[8] = (rounds ushr 24).toByte()
        b[9] = (rounds ushr 16).toByte()
        b[10] = (rounds ushr 8).toByte()
        b[11] = rounds.toByte()
        salt.copyInto(b, 12)
        prefix.copyInto(b, 12 + SALT)
        return b
    }

    private fun keyOf(mode: Int, rounds: Int, salt: ByteArray, password: String?): SecretKeySpec {
        val key = when (mode) {
            MODE_SEAL -> sealKey(salt)
            else -> pbkdf2(passwordBytes(password ?: throw Failure(Reason.WRONG)), salt, rounds)
        }
        return SecretKeySpec(key, "AES")
    }

    /**
     * La password come byte.
     *
     * ⚠️⚠️ **NFC E POI UTF-8, SEMPRE**: la stessa lettera accentata arriva da una tastiera come un
     * carattere solo e da un'altra come lettera più accento, e senza la normalizzazione sarebbero
     * due password diverse per chi le ha scritte uguali.
     */
    internal fun passwordBytes(password: String): ByteArray =
        Normalizer.normalize(password, Normalizer.Form.NFC).toByteArray(Charsets.UTF_8)

    /**
     * PBKDF2 con HMAC-SHA256, per una chiave da 32 byte.
     *
     * ⚠️⚠️ **SCRITTO A MANO E NON PRESO DA `SecretKeyFactory`, E LA RAGIONE È LA CODIFICA**: quella
     * strada riceve la password come caratteri e decide lei come farli diventare byte, e su Android
     * lo ha già cambiato una volta (la 4.4 ha dovuto aggiungere `PBKDF2WithHmacSHA1And8bit` per chi
     * aveva cifrato con gli otto bit bassi). Qui i byte li decide [passwordBytes], e il conto è
     * quello della RFC 8018, che il banco confronta coi vettori pubblicati.
     * ⚠️ **Un blocco solo**: la chiave è lunga quanto un HMAC-SHA256, quindi il contatore del
     * blocco vale sempre uno.
     */
    internal fun pbkdf2(password: ByteArray, salt: ByteArray, rounds: Int): ByteArray {
        val mac = Mac.getInstance(HMAC)
        mac.init(SecretKeySpec(password, HMAC))
        mac.update(salt)
        mac.update(byteArrayOf(0, 0, 0, 1))
        val u = mac.doFinal()
        val t = u.copyOf()
        repeat(rounds - 1) {
            mac.update(u)
            mac.doFinal(u, 0)
            for (i in t.indices) t[i] = (t[i].toInt() xor u[i].toInt()).toByte()
        }
        return t
    }

    /** La chiave del sigillo: dal sale del file e da una costante che vive in AIV. */
    private fun sealKey(salt: ByteArray): ByteArray {
        val base = MessageDigest.getInstance("SHA-256").digest(SEAL_LABEL.toByteArray(Charsets.US_ASCII))
        val mac = Mac.getInstance(HMAC)
        mac.init(SecretKeySpec(base, HMAC))
        return mac.doFinal(salt)
    }

    /**
     * Il nonce di un segmento: il prefisso del file, il contatore, e se è l'ultimo.
     *
     * ⚠️ **Il contatore non gira mai su sé stesso**: quattro byte sono quattro miliardi di segmenti,
     * cioè 256 TiB, e oltre si ferma invece di riusare un nonce.
     */
    private fun nonce(prefix: ByteArray, counter: Long, last: Boolean): ByteArray {
        if (counter > 0xFFFF_FFFFL) throw IOException("segmenti")
        val n = ByteArray(PREFIX + 5)
        prefix.copyInto(n, 0)
        n[PREFIX] = (counter ushr 24).toByte()
        n[PREFIX + 1] = (counter ushr 16).toByte()
        n[PREFIX + 2] = (counter ushr 8).toByte()
        n[PREFIX + 3] = counter.toByte()
        n[PREFIX + 4] = if (last) 1 else 0
        return n
    }

    private fun random(size: Int): ByteArray = ByteArray(size).also { SecureRandom().nextBytes(it) }

    /**
     * Copia un flusso in un altro, e si ferma se chi aspetta se ne è andato.
     */
    private suspend fun pour(input: InputStream, out: OutputStream, meter: Meter?) {
        val buffer = ByteArray(BUFFER)
        while (true) {
            currentCoroutineContext().ensureActive()
            val n = input.read(buffer)
            if (n < 0) break
            meter?.pour(n)
            out.write(buffer, 0, n)
        }
    }

    /**
     * Cifra quello che riceve, a segmenti.
     *
     * ⚠️⚠️ **UN SEGMENTO PIENO SI CHIUDE SOLO QUANDO ARRIVA UN BYTE IN PIÙ**: fino a quel momento
     * non si sa se sarà l'ultimo, e l'ultimo porta il suo segno nel nonce. Così un file che finisce
     * a un multiplo esatto del segmento non ha un segmento vuoto in coda, e un backup vuoto ne ha
     * uno solo, fatto del solo tag.
     * ⚠️ **`flush` non chiude un segmento**: chiuderlo a metà vorrebbe dire un segmento corto nel
     * mezzo, che la lettura prenderebbe per l'ultimo.
     */
    private class SealingStream(
        private val out: OutputStream,
        private val key: SecretKeySpec,
        private val head: ByteArray,
        private val prefix: ByteArray
    ) : OutputStream() {
        private val cipher = Cipher.getInstance(CIPHER)
        private val buffer = ByteArray(SEGMENT)
        private var fill = 0
        private var counter = 0L
        private var closed = false

        override fun write(b: Int) {
            if (closed) throw IOException("chiuso")
            if (fill == SEGMENT) seal(last = false)
            buffer[fill++] = b.toByte()
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            if (closed) throw IOException("chiuso")
            var from = off
            var left = len
            while (left > 0) {
                if (fill == SEGMENT) seal(last = false)
                val n = minOf(left, SEGMENT - fill)
                System.arraycopy(b, from, buffer, fill, n)
                fill += n
                from += n
                left -= n
            }
        }

        override fun flush() = out.flush()

        override fun close() {
            if (closed) return
            closed = true
            try {
                seal(last = true)
            } finally {
                out.close()
            }
        }

        /** Chiude senza l'ultimo segmento: il file che resta non si apre. */
        fun abandon() {
            if (closed) return
            closed = true
            runCatching { out.close() }
        }

        private fun seal(last: Boolean) {
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG * 8, nonce(prefix, counter, last)))
            cipher.updateAAD(head)
            out.write(cipher.doFinal(buffer, 0, fill))
            counter++
            fill = 0
        }
    }

    /**
     * Decifra a segmenti, e restituisce un byte solo dopo che il suo segmento è verificato.
     *
     * ⚠️⚠️ **UN BYTE D'ANTICIPO DICE SE UN SEGMENTO È L'ULTIMO**: il segno vive nel nonce, quindi va
     * saputo prima di decifrare, e l'unico modo di saperlo è guardare se dopo c'è ancora qualcosa.
     * ⚠️ **La fine si dichiara solo dopo l'ultimo segmento verificato** ([ended]): un file che
     * finisce prima non restituisce mai `-1`, restituisce un errore.
     */
    private class OpeningStream(
        private val input: InputStream,
        private val key: SecretKeySpec,
        private val head: Head
    ) : InputStream() {
        private val cipher = Cipher.getInstance(CIPHER)
        private val chunk = ByteArray(SEGMENT + TAG)
        private var plain = ByteArray(0)
        private var pos = 0
        private var counter = 0L
        private var pending = -1

        /** Se l'ultimo segmento è arrivato e si è verificato. */
        var ended = false
            private set

        override fun read(): Int {
            if (!fill()) return -1
            return plain[pos++].toInt() and 0xff
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (len == 0) return 0
            if (!fill()) return -1
            val n = minOf(len, plain.size - pos)
            System.arraycopy(plain, pos, b, off, n)
            pos += n
            return n
        }

        override fun available(): Int = plain.size - pos

        override fun close() = input.close()

        private fun fill(): Boolean {
            while (pos >= plain.size) {
                if (ended) return false
                next()
            }
            return true
        }

        private fun next() {
            var n = 0
            if (pending >= 0) {
                chunk[0] = pending.toByte()
                n = 1
                pending = -1
            }
            while (n < chunk.size) {
                val r = input.read(chunk, n, chunk.size - n)
                if (r < 0) break
                n += r
            }
            val last = if (n < chunk.size) {
                true
            } else {
                val more = input.read()
                if (more >= 0) pending = more
                more < 0
            }
            if (n < TAG) throw Failure(Reason.BROKEN)
            plain = try {
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG * 8, nonce(head.prefix, counter, last)))
                cipher.updateAAD(head.bytes)
                cipher.doFinal(chunk, 0, n)
            } catch (e: GeneralSecurityException) {
                throw Failure(if (counter == 0L && head.locked) Reason.WRONG else Reason.BROKEN)
            }
            pos = 0
            counter++
            if (last) ended = true
        }
    }

    /**
     * Quanti byte escono verso i file d'appoggio, contro quanti ne sono entrati dal file.
     *
     * ⚠️⚠️ **SERVE CONTRO UNA VOCE GONFIATA APPOSTA**: un gigabyte di zeri compresso pesa un
     * megabyte, e un file che lo portasse fra le voci del cestino riempirebbe il telefono prima di
     * essere scoperto. AIV quelle voci le scrive senza compressione, quindi quello che esce non
     * supera quello che è entrato: con una tolleranza per i buffer, uscire di più vuol dire un file
     * che AIV non ha scritto.
     * ⚠️ **I testi non contano qui**, perché si comprimono davvero: hanno il loro tetto a testa.
     */
    private class Meter(private val raw: CountingIn) {
        private var poured = 0L

        fun pour(n: Int) {
            poured += n
            if (poured > raw.total + SLACK) throw Failure(Reason.BAD)
        }
    }

    /** Conta i byte che escono, per l'avanzamento. */
    private class CountingOut(out: OutputStream, private val onBytes: (Long) -> Unit) :
        FilterOutputStream(out) {
        private var total = 0L

        override fun write(b: Int) {
            out.write(b)
            total++
            onBytes(total)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len)
            total += len
            onBytes(total)
        }
    }

    /** Conta i byte che entrano, per l'avanzamento e per [Meter]. */
    private class CountingIn(input: InputStream, private val onBytes: (Long) -> Unit) :
        FilterInputStream(input) {
        var total = 0L
            private set

        override fun read(): Int {
            val b = super.read()
            if (b >= 0) count(1)
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val n = super.read(b, off, len)
            if (n > 0) count(n)
            return n
        }

        private fun count(n: Int) {
            total += n
            onBytes(total)
        }
    }

    private fun prefsName(area: BackupArea) = "$PREFS_DIR${area.token}.json"

    private const val VERSION = 1
    private const val SALT = 16
    private const val PREFIX = 7
    private const val ZERO: Byte = 0
    private val MAGIC = "AIVB".toByteArray(Charsets.US_ASCII)
    private const val FORMAT = "aiv-backup"
    private const val SEAL_LABEL = "AIV backup seal 1"
    private const val CIPHER = "AES/GCM/NoPadding"
    private const val HMAC = "HmacSHA256"
    private const val BUFFER = 64 * 1024

    private const val MANIFEST = "manifest.json"
    private const val PREFS_DIR = "prefs/"
    private const val STYLES_ENTRY = "styles/presets.json"
    private const val COVERS_DIR = "covers/"
    private const val MARK_DIR = "watermark/"
    private val MARK_NAMES = setOf("mark.png", "mark.svg")
    private const val BIN_INDEX = "bin/index.tsv"
    private const val BIN_RESTORED = "bin/restored.tsv"
    private const val BIN_FILES = "bin/files/"

    private const val MANIFEST_MAX = 64L * 1024
    private const val PREFS_MAX = 1024L * 1024
    private const val STYLES_MAX = 4L * 1024 * 1024
    private const val INDEX_MAX = 4L * 1024 * 1024
    private const val RESTORED_MAX = 1024L * 1024
    private const val COVER_MAX = 8L * 1024 * 1024

    /** Vedi [Meter]: quanto i buffer fra il file e lo ZIP possono tenere, e un margine. */
    private const val SLACK = 1024L * 1024

    /** I testi e l'indice dello ZIP, a occhio, per [estimate]. */
    private const val ESTIMATE_BASE = 64L * 1024

    private const val STAGE_HOME = "backup-in"
    private const val STAGE_BIN = "bin-in"
}

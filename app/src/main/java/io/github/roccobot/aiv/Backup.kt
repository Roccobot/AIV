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
 * Le aree del file di impostazioni: le voci del formato.
 *
 * ⚠️⚠️ **La pagina 'Esporta e importa' non mostra le aree ma i gruppi di [BackupGroup]** (risposta
 * A1 dell'utente, 5.80). Le aree restano il formato, coi loro token, così ogni file scritto prima
 * si importa ancora.
 * ⚠️ **Il token non si traduce e non cambia mai**: lo leggono anche i file già salvati.
 * Il cestino viene per ultimo: è la sola area che pesa quanto le sue immagini, e all'importazione
 * si aggiunge a quello che c'è.
 * Le cartelle nascoste hanno un'area loro (`hidden`, richiesta dell'utente, 2.96); un file più
 * vecchio porta il loro elenco dentro `view` ([PREF_RETIRED]).
 */
internal enum class BackupArea(val token: String) {
    VIEW("view"),
    BUTTONS("buttons"),
    EDITOR("editor"),
    STYLES("styles"),
    TINTS("tints"),
    COVERS("covers"),
    HIDDEN("hidden"),
    HINTS("hints"),
    BIN("bin"),
}

/**
 * Le parti della pagina 'Esporta e importa', cioè le sue caselle: sei gruppi che raccolgono le
 * aree del file, nell'ordine della risposta A1 dell'utente (5.80).
 *
 * ⚠️⚠️ **Un gruppo raccoglie aree e non le rinomina mai**: il file continua a scrivere `view`,
 * `buttons`, `hidden` e le altre, così un AIV più vecchio legge un file nuovo e viceversa. La sola
 * chiave che ha cambiato gruppo senza cambiare area è in [PREF_REGROUPED].
 * Il token si scrive nel resoconto ([Backup.Manifest.groups]), così la conferma nomina quello che
 * il file contiene davvero; una versione più vecchia lo ignora, come ogni campo che non conosce.
 * L'etichetta di `EDITOR` non è il titolo della sua sezione (nota dell'utente su
 * `d-backup-aree`): dentro una pagina di esportazione quel titolo sembrava indicare il file stesso.
 */
internal enum class BackupGroup(val token: String, @StringRes val label: Int, val areas: Set<BackupArea>) {
    LOOK("look", R.string.backup_area_view, setOf(BackupArea.VIEW, BackupArea.BUTTONS)),
    // Il gruppo prende il nome della sua sezione delle impostazioni, 'File e cartelle'
    // (richiesta dell'utente, 6.02): un nome in un posto solo.
    FOLDERS(
        "folders", R.string.settings_group_browse,
        setOf(BackupArea.TINTS, BackupArea.COVERS, BackupArea.HIDDEN)
    ),
    EDITOR("editor", R.string.backup_area_editor, setOf(BackupArea.EDITOR)),
    STYLES("styles", R.string.settings_styles, setOf(BackupArea.STYLES)),
    HINTS("hints", R.string.backup_area_hints, setOf(BackupArea.HINTS)),
    BIN("bin", R.string.bin_title, setOf(BackupArea.BIN));

    companion object {
        /** Il gruppo di un'area: ogni area è in uno solo. */
        fun of(area: BackupArea): BackupGroup = entries.first { area in it.areas }

        /**
         * Le aree che servono a un file per contenere [groups]: le loro, più l'area di ogni chiave
         * spostata in uno di loro ([PREF_REGROUPED]).
         */
        fun areasOf(groups: Set<BackupGroup>): Set<BackupArea> =
            groups.flatMap { it.areas }.toSet() + PREF_KEYS.filter { it.group in groups }.map { it.area }

        /**
         * I gruppi di un file più vecchio, che non ha un elenco suo: quelli con un'area nel file, o
         * con una chiave che vive in una di quelle. ⚠️ È esatto, perché nei file di allora ogni
         * `prefs/<area>.json` nominava tutte le chiavi della sua area.
         */
        fun carriedBy(areas: Set<BackupArea>): Set<BackupGroup> =
            entries.filter { g -> g.areas.any { it in areas } || PREF_KEYS.any { it.group == g && it.area in areas } }
                .toSet()
    }
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

/** Una preferenza del file di impostazioni: il suo nome nell'archivio, il suo tipo, la sua area. */
internal class PrefKey(val name: String, val type: PrefType, val area: BackupArea) {
    /** La parte della pagina che la contiene: il gruppo della sua area, salvo [PREF_REGROUPED]. */
    val group: BackupGroup get() = PREF_REGROUPED[name] ?: BackupGroup.of(area)
}

/**
 * Le chiavi che la pagina mette in un gruppo diverso da quello della loro area.
 *
 * ⚠️ **L'area di una chiave non cambia mai ([PREF_KEYS]), quindi una chiave che si sposta nella
 * pagina si sposta qui**: `last-mark`, l'indicatore dell'ultimo media, viaggia ancora in `buttons`,
 * dove ogni versione lo cerca, e segue 'File e cartelle' (richiesta dell'utente).
 */
internal val PREF_REGROUPED: Map<String, BackupGroup> = mapOf("last-mark" to BackupGroup.FOLDERS)

/**
 * Tutte le preferenze del file di impostazioni, ognuna con la sua area.
 *
 * ⚠️⚠️ **È un elenco scritto a mano, presidiato da `BackupTest`**: le chiavi sono in cinque
 * oggetti di `Settings.kt`, e una chiave dimenticata qui resterebbe fuori senza errori. La prova
 * scrive l'archivio con tutti i suoi scrittori e pretende che ogni chiave sia qui o in
 * [PREF_OUTSIDE], col tipo giusto, e che ogni chiave di qui, tranne le due vecchie, sia scritta.
 * ⚠️ **Le due chiavi vecchie, `veil` e `mark-air`, ci sono di proposito**: nessuno le scrive, ma
 * l'app le legge ancora come ripiego, e su un telefono che non ha più salvato sono loro a dire il
 * valore.
 * Gli avvisi entrano da [Hint], e non per nome: un avviso nuovo entra per costruzione.
 * ⚠️⚠️ **L'area di una chiave non cambia mai, anche se la sua voce si sposta di pagina**: una
 * versione vecchia la cercherebbe nella voce sbagliata. Il posto nell'interfaccia e l'area nel
 * file sono indipendenti.
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
        "front-facts", "front-pick-all", "gpu-thumbs", "folder-count", "auto-update"
    )
    area(
        BackupArea.VIEW, PrefType.STRING,
        "bg-type", "bg-theme", "scale-mode", "info-position", "panel-depth", "start-folder-name",
        "folder-view", "list-text", "ui-theme", "fact-order", "recent"
    )
    area(BackupArea.VIEW, PrefType.FLOAT, "zoom-max")
    area(BackupArea.VIEW, PrefType.LONG, "start-folder")
    area(BackupArea.VIEW, PrefType.INT, "folder-columns")
    area(BackupArea.VIEW, PrefType.SET, "fact-off")
    area(BackupArea.BUTTONS, PrefType.BOOLEAN, "list-path", "pad-labels", "corner-round")
    area(
        BackupArea.BUTTONS, PrefType.STRING,
        "hand", "menu-order", "pick-order", "turn-order", "step-order", "mod-order", "last-mark",
        "phone-pill", "pill-fill", "main-control"
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
    area(
        BackupArea.BUTTONS, PrefType.INT,
        "glass-radius", "glass-intensity", "glass-tint", "glass-shift", "glass-colour-light", "glass-colour-dark"
    )
    area(BackupArea.TINTS, PrefType.STRING, "folder-colour")
    area(BackupArea.TINTS, PrefType.SET, "folder-tints")
    area(BackupArea.HIDDEN, PrefType.SET, "hidden-relative", "hidden-only", "included-relative")
    // 'feed-date' va con 'folder-order', la voce accanto nelle impostazioni.
    area(BackupArea.HIDDEN, PrefType.STRING, "folder-mode", "folder-order", "favourite-folders", "feed-date")
    Hint.entries.forEach { add(PrefKey(it.token, PrefType.BOOLEAN, BackupArea.HINTS)) }
}

/**
 * Una preferenza che le versioni di prima scrivevano e questa non scrive più, e come si traduce.
 *
 * @property area dove la scrivono i file di allora. Non cambia mai, come quella di [PrefKey].
 * @property into la chiave di adesso che la prende.
 * @property convert come un valore di allora diventa uno di adesso.
 */
internal class RetiredKey(
    val area: BackupArea,
    val type: PrefType,
    val into: PrefKey,
    val convert: (Any) -> Any
)

/**
 * Le preferenze ritirate, cioè quelle che un file di un'altra versione può ancora contenere.
 *
 * ⚠️ **Senza di loro un file più vecchio sembrerebbe più nuovo**: una chiave sconosciuta si conta
 * fra le saltate, e l'avviso finale direbbe che il file viene da una versione più recente. Con la
 * traduzione il valore entra, e non si conta niente.
 * ⚠️ **L'elenco delle nascoste di un file della 2.95 è nell'area `view`**: quel file lo importa la
 * casella 'Aspetto e navigazione', e la conferma non elenca le cartelle nascoste fra le sue parti.
 */
internal val PREF_RETIRED: Map<String, RetiredKey> = mapOf(
    "hidden-folders" to RetiredKey(
        area = BackupArea.VIEW,
        type = PrefType.SET,
        into = PREF_KEYS.first { it.name == "hidden-relative" },
        convert = { vecchie -> (vecchie as Set<*>).map { portablePath(it.toString()) }.toSet() }
    ),
    // Il vecchio interruttore della pillola: acceso vuol dire la pillola, spento il FAB. Il perché
    // della chiave nuova è su `MainControl`.
    "phone-pill-on" to RetiredKey(
        area = BackupArea.BUTTONS,
        type = PrefType.BOOLEAN,
        into = PREF_KEYS.first { it.name == "main-control" },
        convert = { acceso -> if (acceso == true) MainControl.PILL.token else MainControl.FAB.token }
    ),
    // La vecchia 'Luminosità' col segno: lo 'Scostamento' non ha segno, e scurisce o schiarisce
    // secondo il tema (risposta C2 dell'utente). Il perché è su `GlassTune.shift`.
    "glass-light" to RetiredKey(
        area = BackupArea.BUTTONS,
        type = PrefType.INT,
        into = PREF_KEYS.first { it.name == "glass-shift" },
        convert = { luce -> kotlin.math.abs((luce as Number).toInt()) }
    )
)

/**
 * Le preferenze che il file di impostazioni non contiene, e il perché di ognuna.
 *
 * ⚠️ **Sono promemoria di questo telefono, non scelte dell'utente**, e su un altro direbbero il
 * falso: `all-files-asked` dice che il permesso è già stato chiesto qui, `download-tree` vale col
 * permesso persistente di questo telefono, `clipboard-done` e `clipboard-when` dicono quale
 * indirizzo degli appunti è già stato aperto qui, `download-seen` che cosa c'è nella cartella
 * Download di qui. `included-initialized` dice che questo telefono ha già scritto il suo elenco di
 * cartelle di fabbrica: l'importazione non lo copia, ma lo accende quando arriva un elenco di
 * incluse, così le voci di fabbrica tolte non ricompaiono.
 */
internal val PREF_OUTSIDE: Set<String> = setOf(
    "all-files-asked", "download-tree", "clipboard-done", "clipboard-when", "download-seen", "included-initialized"
)

/** Le aree fatte di preferenze, cioè quelle che hanno una voce `prefs/<area>.json`. */
internal val PREF_AREAS: Set<BackupArea> = PREF_KEYS.map { it.area }.toSet()

/**
 * Le preferenze che all'importazione si fondono con quelle di adesso, e come.
 *
 * ⚠️ **Risposta `fonde` dell'utente a `d-backup-importa`** (2.95): copertine, colori e stili del
 * file si aggiungono, e dove si sovrappongono vince il file. Dei colori si fonde l'elenco delle
 * tinte, cartella per cartella; lo stile (`folder-colour`) è un valore solo e si sostituisce. Gli
 * elenchi delle cartelle nascoste e incluse si sommano.
 * ⚠️ **Una chiave che si fonde non torna mai al valore di fabbrica**: assente nel file vuol dire
 * niente da aggiungere.
 * Sono tutte insiemi di stringhe, e il banco lo controlla.
 */
internal val PREF_MERGED: Map<String, (Set<String>, Set<String>) -> Set<String>> = mapOf(
    "folder-tints" to FolderTints::merged,
    "hidden-relative" to { adesso, file -> adesso + file },
    "hidden-only" to { adesso, file -> adesso + file },
    "included-relative" to { adesso, file -> adesso + file }
)

/**
 * Il file di impostazioni: un file che solo AIV sa leggere, con quello che l'app ricorda, un'area
 * per volta (richiesta dell'utente, giro della 2.91: uno ZIP cifrato, leggibile solo da AIV, a
 * richiesta protetto da password).
 *
 * ⚠️⚠️ **È uno ZIP dentro un contenitore cifrato, non uno ZIP con la password**: la cifratura
 * classica dello ZIP è debole, e quella AES di WinZip in `java.util.zip` non c'è. Lo ZIP passa da
 * [SealingStream], che lo taglia in segmenti da [SEGMENT] byte e li cifra uno per uno con AES-GCM,
 * come i flussi cifrati di Tink e di `age`, con le sole classi della piattaforma.
 * - A segmenti: su Android un GCM non restituisce un byte finché il tag non è verificato, e un
 *   cestino da un gigabyte andrebbe letto tutto in memoria.
 * - ⚠️ **L'ultimo segmento si dichiara nel nonce**: un file troncato fra due segmenti sarebbe
 *   fatto di segmenti validi e sembrerebbe completo.
 * - L'intestazione entra in ogni segmento come dato autenticato: cambiare modo, iterazioni o sale
 *   rompe la lettura.
 *
 * ⚠️ **Senza password è un sigillo, non una protezione, e la pagina lo dice**: la chiave nasce da
 * una costante nell'APK, quindi chiunque abbia AIV lo importa. La protezione vera è la password,
 * con PBKDF2 a [ROUNDS] iterazioni.
 *
 * ⚠️⚠️ **L'importazione prima legge tutto e poi scrive tutto**: le voci si mettono da parte in due
 * cartelle d'appoggio, si controllano, e il file si legge fino all'ultimo segmento; solo allora
 * l'app cambia.
 * Ogni area sostituisce quella di adesso, tranne il cestino e la cronologia dei ripristini, che si
 * aggiungono (sostituirli cancellerebbe dei file), e copertine, tinte e stili, che si fondono
 * ([PREF_MERGED]).
 *
 * ⚠️⚠️ **Vale fra versioni diverse di AIV, nei due versi** (richiesta dell'utente). Quattro regole:
 * - il formato cresce solo aggiungendo: quello che cambia prende un nome nuovo, e una versione
 *   più vecchia salta quello che non conosce;
 * - ⚠️ **una preferenza che il file non nomina non si tocca**: per ogni area il file elenca tutte
 *   le chiavi che conosceva, col valore o come assenti. Una chiave nata dopo resta com'era, una
 *   dichiarata assente torna al valore di fabbrica;
 * - l'unico cancello è la versione del contenitore, il quinto byte dell'intestazione, che sale
 *   solo quando una versione vecchia non potrebbe leggere niente;
 * - quello che non si sa leggere non cancella niente, e quello che si salta si dice nell'avviso.
 * Gli archivi del cestino e della cronologia non crescono per colonne: le loro righe hanno un
 * numero fisso di campi, e una colonna in più le farebbe scartare a una versione vecchia.
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
     * La cifra che OWASP indica per PBKDF2 con SHA-256. È scritta nell'intestazione: un file di
     * oggi si apre anche quando questo numero sale.
     * Il costo su un telefono non è misurato: la pagina mostra l'avanzamento mentre la chiave si
     * calcola.
     */
    const val ROUNDS = 600_000

    /**
     * Il tetto delle iterazioni in lettura.
     *
     * ⚠️ Contro un file fatto apposta: un'intestazione con due miliardi di iterazioni terrebbe il
     * telefono a contare per ore.
     */
    const val ROUNDS_MAX = 5_000_000

    const val MODE_SEAL = 0
    const val MODE_PASSWORD = 1

    /** Perché un file non si apre. */
    enum class Reason {
        /** Non è un file di impostazioni di AIV, o è scritto in un modo che AIV non scrive. */
        BAD,

        /**
         * Il contenitore viene da una versione più nuova, che lo scrive in un modo che questa non sa
         * aprire. Solo il contenitore: un contenuto più nuovo si legge per quello che si conosce.
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
     * Che cosa dice un file di sé, prima di aprirlo tutto.
     *
     * @property foreign quante aree del file questa versione non conosce: vengono da una più nuova.
     * @property groups le parti della pagina che il file contiene: il suo elenco, o per un file più
     *   vecchio quelle ricavate da [areas] ([BackupGroup.carriedBy]).
     */
    class Manifest(
        val app: String,
        val created: Long,
        val areas: Set<BackupArea>,
        val foreign: Int,
        val groups: Set<BackupGroup>
    )

    /**
     * Com'è andata un'importazione: le aree da rimettere, quelle entrate davvero, e se il file
     * conteneva qualcosa che questa versione non sa trattare.
     *
     * Le prime due si confrontano: è la differenza fra 'importato' e 'importato a metà'.
     * [skipped] non fa un'importazione a metà: quello che si salta non si poteva importare, ma lo si
     * dice, perché aggiornando l'app si potrebbe.
     */
    class Outcome(val wanted: Set<BackupArea>, val applied: Set<BackupArea>, val skipped: Boolean)

    /**
     * Il tipo del file per il selettore di sistema: un formato suo, che nessun'altra app apre.
     *
     * ⚠️ **È questo tipo che lascia l'estensione com'è**: un fornitore di documenti ritocca il
     * suffisso che non va d'accordo col tipo dichiarato, e con un tipo vero il file risulterebbe
     * `.aivsettings.json`.
     */
    const val MIME = "application/octet-stream"

    /**
     * Il nome proposto per un file di impostazioni scritto oggi.
     *
     * Nome ed estensione sono dell'utente (estensione `.aivsettings`, giro della 2.94; nome
     * `AIV_<data>`, voce `impostazioni-file`, 2.99). Un file col vecchio suffisso `.aivbackup` si
     * importa lo stesso: che cosa è lo dice l'intestazione, non il nome.
     */
    fun fileName(now: Long): String =
        "AIV_${SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(Date(now))}.aivsettings"

    // ── Scrittura ────────────────────────────────────────────────────────────

    /**
     * Scrive un file di impostazioni con le aree scelte su [out], e lo chiude.
     *
     * ⚠️ **Se qualcosa va storto, l'ultimo segmento non si scrive**: un file senza il suo ultimo
     * segmento non si apre. Chi chiama lo cancella; se non ci riesce, resta un file che si dichiara
     * rotto, non uno a metà che si importa come intero.
     * Un file del cestino che sparisce durante la copia si salta: il cestino può cambiare intanto.
     *
     * @param password `null` per il solo sigillo. Non vuota: una chiave HMAC vuota non esiste, e la
     *   finestra lo impedisce prima.
     * @param rounds le iterazioni di PBKDF2: di fabbrica [ROUNDS]; le prove ne usano meno.
     * @param onBytes quanti byte sono già usciti, per l'avanzamento.
     * @param groups le parti spuntate nella pagina, che scelgono chiave per chiave che cosa entra in
     *   un'area ([PREF_REGROUPED]); `null` scrive [areas] intere, come nelle prove. Coi gruppi,
     *   [areas] è il loro [BackupGroup.areasOf].
     */
    suspend fun write(
        context: Context,
        out: OutputStream,
        areas: Set<BackupArea>,
        password: String?,
        rounds: Int = ROUNDS,
        onBytes: (Long) -> Unit = {},
        groups: Set<BackupGroup>? = null
    ) = pack(out, password, rounds, onBytes) { zip ->
        putText(zip, MANIFEST, manifestJson(areas, groups).toString())
        val stored = storedPreferences(context).asMap().entries
            .associate { it.key.name to it.value }
        for (area in BackupArea.entries) {
            if (area !in areas) continue
            if (area in PREF_AREAS) putText(zip, prefsName(area), prefsJson(stored, area, groups))
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
     * È una funzione a sé perché le prove ci mettono voci scritte a mano, come quelle di una versione
     * più nuova: il contenitore è quello vero, e solo il contenuto è finto.
     * Se [body] va storto l'ultimo segmento non si scrive ([write]).
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
     * Quanti byte peserà il file con le aree scelte, più o meno: per l'avanzamento.
     *
     * I testi si comprimono e i file no, quindi la stima è un poco abbondante.
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
     * Quanto pesano i file di un'area, cioè la parte del file di impostazioni che non è testo.
     *
     * Serve anche alla pagina, che lo scrive accanto al cestino: è la sola casella che può valere un
     * gigabyte.
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
     * Il resoconto: che cosa è il file, chi l'ha scritto e quando, e quali aree contiene.
     *
     * Nessun numero di versione del formato: il contenuto cresce solo aggiungendo. La versione
     * dell'app c'è, per la conferma.
     */
    private fun manifestJson(areas: Set<BackupArea>, groups: Set<BackupGroup>?): JSONObject = JSONObject()
        .put("format", FORMAT)
        .put("app", BuildConfig.VERSION_NAME)
        .put("created", System.currentTimeMillis())
        .put("areas", JSONArray(BackupArea.entries.filter { it in areas }.map { it.token }))
        .apply {
            if (groups != null) put("groups", JSONArray(BackupGroup.entries.filter { it in groups }.map { it.token }))
        }

    /**
     * Le preferenze di un'area come testo: quelle che hanno un valore, e quelle che non ce l'hanno.
     *
     * ⚠️⚠️ **Le chiavi senza valore si elencano in `absent`**: una chiave assente dall'archivio
     * vale il valore di fabbrica e all'arrivo deve tornare tale; una chiave nata dopo invece non è
     * nominata, e all'arrivo resta com'è. Senza l'elenco le due cose non si distinguerebbero.
     * ⚠️ **I numeri viaggiano come stringhe**: un numero JSON lo tipizza chi legge (`org.json`
     * restituisce `Int` o `Long` secondo la grandezza, e `Double` per un decimale), e altri lettori
     * arrotondano gli interi oltre i 53 bit. Il tipo lo dice il campo accanto.
     * Un valore col tipo sbagliato si scrive come assente (la prova sulle chiavi confronta i tipi):
     * l'app non lo saprebbe leggere.
     */
    private fun prefsJson(stored: Map<String, Any>, area: BackupArea, groups: Set<BackupGroup>?): String {
        val keys = JSONArray()
        val absent = JSONArray()
        for (k in PREF_KEYS) {
            // Una chiave di una parte non spuntata non si nomina affatto: all'arrivo resta com'è.
            if (k.area != area || (groups != null && k.group !in groups)) continue
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
     * ⚠️ **Senza compressione**: immagini e video sono già compressi, e una voce che pesa quanto il
     * suo file permette all'importazione di accorgersi di una voce gonfiata apposta ([Meter]).
     * Il file si apre prima della voce: uno sparito intanto non lascia una voce vuota.
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
     * La versione si guarda prima del resto: un file di una versione più nuova può avere
     * un'intestazione che questa non capisce, e la risposta giusta è 'aggiorna l'app'.
     * ⚠️ **È l'unico cancello di versione del formato**: sale solo quando cambia il modo di aprire
     * il file (cifratura, chiave, segmenti). Voci, campi e aree nuove non lo toccano.
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
     * È il passo lento, PBKDF2, e per questo è a sé: resoconto e importazione leggono il file due
     * volte, e la chiave si calcola una volta.
     * Non dice se la password è giusta: lo dice il primo segmento, che con la chiave sbagliata non
     * si verifica.
     */
    fun open(head: Head, password: String?): Opened {
        if (head.locked && password.isNullOrEmpty()) throw Failure(Reason.WRONG)
        return Opened(head, keyOf(head.mode, head.rounds, head.salt, password))
    }

    /**
     * Che cosa dice un file di sé: la prima voce, e nient'altro.
     *
     * Legge solo l'inizio, quindi non dice se il file è intero: lo scopre [restore].
     */
    fun manifest(input: InputStream, opened: Opened): Manifest {
        val plain = unseal(input, opened)
        val zip = ZipInputStream(plain, Charsets.UTF_8)
        val first = nextEntry(zip) ?: throw Failure(Reason.BAD)
        if (first.name != MANIFEST) throw Failure(Reason.BAD)
        return readManifest(readText(zip, MANIFEST_MAX))
    }

    /**
     * Rimette le aree scelte di un file, che devono essere anche nel suo resoconto.
     *
     * ⚠️⚠️ **In due tempi, e il secondo non si interrompe**: prima si legge e si controlla tutto
     * (e lì annullare lascia l'app com'era), poi si applica fino in fondo, perché un'importazione
     * fermata a metà lascerebbe l'app fatta di due file.
     * Un'area che il resoconto nomina e il file non contiene non si tocca, e resta fra quelle
     * attese: l'avviso dice 'a metà'. Le copertine fanno eccezione, perché non hanno una voce che ne
     * dica la presenza: un file senza copertine non ne aveva, e fondere niente lascia quelle che ci
     * sono.
     *
     * @param chosen le caselle spuntate: si importano quelle che sono anche nel resoconto.
     * @param onBytes quanti byte del file sono già stati letti, per l'avanzamento.
     * @param groups le parti spuntate nella pagina, che scelgono chiave per chiave che cosa entra;
     *   `null` prende [chosen] intere. Come in [write].
     */
    suspend fun restore(
        context: Context,
        input: InputStream,
        opened: Opened,
        chosen: Set<BackupArea>,
        onBytes: (Long) -> Unit = {},
        groups: Set<BackupGroup>? = null
    ): Outcome = withContext(Dispatchers.IO) {
        sweep(context)
        try {
            val found = gather(context, CountingIn(input, onBytes), opened, chosen, groups)
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
     * Si chiama anche all'avvio dell'app: un'importazione interrotta dal sistema lascerebbe copie
     * che nessuno legge, e il cestino di un altro telefono può pesare quanto le sue immagini.
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
     * Le chiavi che non compaiono in nessuno dei due elenchi non si toccano: sono nate dopo il file
     * (le regole sulle versioni, in testa a [Backup]).
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
        var logo: File? = null

        /** Se il file contiene un logo in un formato che qui non si conosce. */
        var logoForeign = false
        var records: List<Bin.Record>? = null
        val binFiles = ArrayList<File>()
        var restored: List<History.Row> = emptyList()

        /** Quante cose del file questa versione non sa trattare. */
        var skipped = 0
    }

    /**
     * Legge il file intero, e mette da parte le voci delle aree da rimettere.
     *
     * ⚠️ **Quello che questa versione non conosce si salta, quello che nessuna versione scrive
     * no**: voci, aree, chiavi o tipi sconosciuti vengono da una versione più nuova, e si saltano
     * contandoli. Un nome che esce dalla sua cartella, un doppione o una struttura illeggibile
     * vogliono dire un file fatto a mano, e allora non si importa niente.
     */
    private suspend fun gather(
        context: Context,
        raw: CountingIn,
        opened: Opened,
        chosen: Set<BackupArea>,
        groups: Set<BackupGroup>?
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
                    val letto = readPrefs(readText(zip, PREFS_MAX), area, groups)
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
                        // Un formato di copertina sconosciuto: si salta, e la copertina di quella
                        // cartella resta, perché le copertine si fondono.
                        found.skipped++
                    }
                }
                name.startsWith(MARK_DIR) -> if (BackupArea.EDITOR in want) {
                    val file = name.removePrefix(MARK_DIR)
                    if (!simple(file)) throw Failure(Reason.BAD)
                    if (file in MARK_NAMES) {
                        if (found.logo != null) throw Failure(Reason.BAD)
                        found.logo = stage(zip, File(stageHome(context), name), Watermark.MAX_BYTES, meter)
                    } else {
                        // Un formato di logo sconosciuto: si salta, e il logo di qui non si
                        // cancella ([apply]).
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
                    // ⚠️ Una cartella numerata per file: il cestino può essere su un archivio che
                    // non distingue le maiuscole, e due nomi diversi solo per quelle finirebbero
                    // sullo stesso file d'appoggio.
                    val slot = File(File(stageBin(context), "files"), found.binFiles.size.toString())
                    found.binFiles += stage(zip, File(slot, file), -1L, meter)
                }
                // Una voce sconosciuta viene da una versione più nuova: si salta e si conta. Le voci
                // delle aree non scelte non contano: le ha lasciate fuori chi importa.
                else -> found.skipped++
            }
        }
        // ⚠️ Il resto del file si legge fino in fondo, ed è qui che un file tagliato si scopre: lo
        // ZIP si ferma al suo indice, e senza questo giro l'ultimo segmento non si guarderebbe.
        val rest = ByteArray(BUFFER)
        while (plain.read(rest) >= 0) currentCoroutineContext().ensureActive()
        if (!plain.ended) throw Failure(Reason.BROKEN)
        return found
    }

    /**
     * Applica quello che [gather] ha messo da parte, e dice quali aree sono entrate.
     *
     * ⚠️ **Prima i file e poi le preferenze**, in una transazione sola: l'app rilegge le preferenze
     * appena cambiano, e ridisegnandosi deve trovare già al loro posto copertine e logo.
     * Un'area che non riesce non ferma le altre, e manca dal risultato.
     * Stili, copertine e tinte si fondono (risposta `fonde`): [Presets.merge], [FolderCovers.adopt]
     * e [PREF_MERGED].
     */
    private suspend fun apply(context: Context, found: Found): Set<BackupArea> {
        val done = HashSet<BackupArea>()
        found.styles?.let { text ->
            if (runCatching { Presets.merge(context, text) }.getOrDefault(false)) {
                done += BackupArea.STYLES
            }
        }
        if (BackupArea.COVERS in found.wanted) {
            val entrate = runCatching { FolderCovers.adopt(context, found.covers) }.getOrDefault(false)
            if (entrate) done += BackupArea.COVERS
        }
        // Il logo va con le preferenze dell'editor, e solo se ci sono: senza di lui direbbero
        // 'filigrana accesa' su un'app senza logo, o il contrario.
        var logoOk = true
        if (BackupArea.EDITOR in found.prefs) {
            val logo = found.logo
            logoOk = runCatching {
                when {
                    logo != null -> Watermark.adopt(context, Uri.fromFile(logo))
                    // Un logo che questa versione non sa leggere non cancella quello che c'è: il
                    // file un logo l'aveva.
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
                    // ⚠️ Si tolgono le sole chiavi che il file dichiara assenti, non tutte quelle
                    // dell'area: una chiave non nominata è nata dopo il file, e resta com'è. Una
                    // chiave che si fonde non si toglie e non si sostituisce ([PREF_MERGED]).
                    // Un file con un elenco di nascoste ma senza `folder-mode` viene da prima delle
                    // incluse, e vuol dire la modalità delle escluse.
                    for ((area, letto) in found.prefs) {
                        val legacyFolders = area == BackupArea.HIDDEN ||
                            (area == BackupArea.VIEW && (letto.values.any { it.first.name == "hidden-relative" } ||
                                letto.absent.any { it.name == "hidden-relative" }))
                        if (legacyFolders && letto.values.none { it.first.name == "folder-mode" } &&
                            letto.absent.none { it.name == "folder-mode" }) {
                            p[FOLDER_MODE] = FolderMode.EXCLUDED.token
                        }
                        letto.absent.forEach { if (it.name !in PREF_MERGED) remove(p, it) }
                        letto.values.forEach { (key, value) -> put(p, key, value) }
                        if (area == BackupArea.HIDDEN && (letto.values.any { it.first.name == "included-relative" } ||
                            letto.absent.any { it.name == "included-relative" })) p[INCLUDED_INITIALIZED] = true
                    }
                    // Un indicatore mancante lo deciderebbe la migrazione al prossimo avvio, che su
                    // un archivio pieno direbbe 'angolo': scriverlo adesso fa dire la stessa cosa a
                    // questo avvio e al prossimo.
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
        // Una chiave si riconosce dal nome e non dal tipo: questa toglie il valore qualunque tipo
        // abbia nell'archivio.
        p.remove(booleanPreferencesKey(key.name))
    }

    @Suppress("UNCHECKED_CAST")
    private fun put(p: MutablePreferences, key: PrefKey, value: Any) {
        PREF_MERGED[key.name]?.let { fondi ->
            val k = stringSetPreferencesKey(key.name)
            p[k] = fondi(p[k].orEmpty(), value as Set<String>)
            return
        }
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
     * ⚠️ **Si salta, contandolo, quello che questa versione non sa mettere al suo posto**: chiave
     * sconosciuta, di un'altra area, di un altro tipo o con un valore illeggibile vengono da una
     * versione più nuova (un tipo che cambia prende una chiave nuova), e il resto dell'area entra.
     * Un'assente sconosciuta non si conta: è al valore di fabbrica, e saltarla non perde niente.
     * Un nome ripetuto, nei due elenchi o nello stesso, è un rifiuto: nessuna versione lo scrive.
     * Una chiave ritirata si traduce e non si conta ([PREF_RETIRED]): viene da una versione più
     * vecchia.
     * Una chiave di una parte non spuntata si lascia fuori senza contarla, come le voci di un'area
     * non spuntata.
     */
    private fun readPrefs(text: String, area: BackupArea, groups: Set<BackupGroup>?): Prefs {
        fun wanted(key: PrefKey) = groups == null || key.group in groups
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
                val retired = PREF_RETIRED[name]
                    ?.takeIf { it.area == area && k.optString("type") == it.type.token }
                val old = retired?.let { decode(it.type, k.opt("value")) }
                if (retired != null && old != null) {
                    if (wanted(retired.into)) values += retired.into to retired.convert(old)
                } else if (known == null || wanted(known)) {
                    skipped++
                }
                continue
            }
            if (wanted(known)) values += known to value
        }
        // Un elenco che manca vale vuoto: il file dice soltanto meno cose.
        o.optJSONArray("absent")?.let { list ->
            for (i in 0 until list.length()) {
                val name = list.opt(i) as? String ?: throw Failure(Reason.BAD)
                if (name.isEmpty() || !names.add(name)) throw Failure(Reason.BAD)
                PREF_KEYS.firstOrNull { it.name == name && it.area == area }
                    ?.takeIf { wanted(it) }?.let { absent += it }
                // Anche un vecchio elenco di nascoste vuoto indica la modalità delle escluse.
                PREF_RETIRED[name]?.takeIf { it.area == area && it.into.name == "hidden-relative" }
                    ?.takeIf { wanted(it.into) }?.let { absent += it.into }
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
     * ⚠️ **Nessun cancello di versione qui**: un resoconto di una versione più nuova si legge per
     * quello che si conosce. Un campo `version`, se c'è, non si guarda.
     * Un'area sconosciuta viene da una versione più nuova: non si importa, non ferma le altre, e si
     * conta in [Manifest.foreign].
     */
    private fun readManifest(text: String): Manifest {
        val o = json(text)
        if (o.optString("format") != FORMAT) throw Failure(Reason.BAD)
        val list = o.optJSONArray("areas") ?: throw Failure(Reason.BAD)
        val tokens = (0 until list.length()).map { list.optString(it) }
        val areas = tokens.mapNotNull { t -> BackupArea.entries.firstOrNull { it.token == t } }.toSet()
        val foreign = tokens.count { t -> BackupArea.entries.none { it.token == t } }
        // Un gruppo sconosciuto viene da una versione più nuova: le sue aree sono già contate in
        // `foreign`, quindi si lascia solo fuori.
        val groups = o.optJSONArray("groups")?.let { g ->
            (0 until g.length()).mapNotNull { i -> BackupGroup.entries.firstOrNull { it.token == g.optString(i) } }.toSet()
        } ?: BackupGroup.carriedBy(areas)
        return Manifest(o.optString("app"), o.optLong("created", 0L), areas, foreign, groups)
    }

    private fun json(text: String): JSONObject = try {
        JSONObject(text)
    } catch (e: JSONException) {
        throw Failure(Reason.BAD)
    }

    /**
     * La voce dopo, con gli errori dello ZIP tradotti.
     *
     * Un errore del contenitore passa com'è: dice se la password è sbagliata o se il file è
     * tagliato. Un errore dello ZIP invece arriva da un segmento già verificato, cioè da un file
     * scritto apposta.
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
     * Anche la fine anticipata: in un contenitore intero, uno ZIP che finisce a metà di una voce è
     * scritto apposta, e `ZipInputStream` lo dice con un `EOFException`.
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
     * ⚠️ **Le cartelle dell'app sono escluse**: il ripristino scrive dove l'origine dice, e un file
     * scritto apposta potrebbe mandare un'immagine accanto agli stili e alle preferenze. Un file di
     * AIV non lo fa mai: da quelle cartelle niente finisce nel cestino.
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
        // La cartella esterna dell'app ha più di un nome (`/sdcard`, `/storage/emulated/0`), con la
        // stessa coda: si riconosce quella, qualunque sia il volume.
        if (origin.contains("/Android/data/${context.packageName}/")) return false
        return homes.none { origin == it || origin.startsWith("$it/") }
    }

    private fun stageHome(context: Context): File = File(context.filesDir, STAGE_HOME)

    /**
     * L'appoggio del cestino, accanto al cestino: sullo stesso volume lo spostamento finale è una
     * rinomina, da un altro sarebbe una seconda copia di tutto.
     */
    private fun stageBin(context: Context): File = File(Bin.dir(context).parentFile, STAGE_BIN)

    // ── Il contenitore ───────────────────────────────────────────────────────

    private fun unseal(input: InputStream, opened: Opened): OpeningStream {
        val head = head(input)
        // Il file letto adesso deve essere quello di cui si ha la chiave: se è cambiato fra le due
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
     * ⚠️ **NFC e poi UTF-8, sempre**: la stessa lettera accentata arriva da una tastiera come un
     * carattere e da un'altra come lettera più accento, e senza normalizzazione sarebbero due
     * password diverse.
     */
    internal fun passwordBytes(password: String): ByteArray =
        Normalizer.normalize(password, Normalizer.Form.NFC).toByteArray(Charsets.UTF_8)

    /**
     * PBKDF2 con HMAC-SHA256, per una chiave da 32 byte.
     *
     * ⚠️ **Scritto a mano e non preso da `SecretKeyFactory`, per la codifica**: quella strada
     * riceve la password come caratteri e decide lei come farne byte, e su Android lo ha già
     * cambiato una volta (Android 4.4 ha dovuto aggiungere `PBKDF2WithHmacSHA1And8bit`). Qui i byte
     * li decide [passwordBytes], e il conto è quello della RFC 8018, che il banco confronta coi
     * vettori pubblicati.
     * Un blocco solo: la chiave è lunga quanto un HMAC-SHA256.
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

    /** La chiave del sigillo: dal sale del file e da una costante scritta in AIV. */
    private fun sealKey(salt: ByteArray): ByteArray {
        val base = MessageDigest.getInstance("SHA-256").digest(SEAL_LABEL.toByteArray(Charsets.US_ASCII))
        val mac = Mac.getInstance(HMAC)
        mac.init(SecretKeySpec(base, HMAC))
        return mac.doFinal(salt)
    }

    /**
     * Il nonce di un segmento: il prefisso del file, il contatore, e se è l'ultimo.
     *
     * ⚠️ **Il contatore non ricomincia mai da capo**: quattro byte sono quattro miliardi di
     * segmenti (256 TiB), e oltre si va in errore, senza riusare un nonce.
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

    /** Copia un flusso in un altro, e si ferma se chi aspetta se ne è andato. */
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
     * ⚠️ **Un segmento pieno si chiude solo quando arriva un byte in più**: prima non si sa se sarà
     * l'ultimo, che ha il suo segno nel nonce. Così un file lungo un multiplo esatto del segmento
     * non ha un segmento vuoto in coda, e un file vuoto ne ha uno, fatto del solo tag.
     * ⚠️ **`flush` non chiude un segmento**: un segmento corto nel mezzo sembrerebbe l'ultimo.
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
     * ⚠️ **Un byte d'anticipo dice se un segmento è l'ultimo**: il segno è nel nonce, quindi va
     * saputo prima di decifrare, guardando se dopo c'è ancora qualcosa.
     * ⚠️ **La fine si dichiara solo dopo l'ultimo segmento verificato** ([ended]): un file che
     * finisce prima non restituisce `-1` ma un errore.
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
     * ⚠️ **Contro una voce gonfiata apposta**: un gigabyte di zeri compresso pesa un megabyte, e fra
     * le voci del cestino riempirebbe il telefono. AIV quelle voci le scrive senza compressione,
     * quindi quello che esce non supera quello che è entrato, salvo i buffer ([SLACK]).
     * I testi non contano qui, perché si comprimono davvero: ognuno ha il suo tetto.
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

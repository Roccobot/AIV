package io.github.roccobot.aiv

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/*
 * Le due varianti dell'app, e che cosa può fare ognuna con la memoria condivisa
 * (`Rules.md` § '🏪 Le due varianti, e che cosa manca ad AIV Play').
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️ **È l'unico punto che legge `BuildConfig.FILES`**: un secondo lettore sarebbe una seconda
 * risposta alla stessa domanda. Il perché delle varianti è in `build.gradle.kts`, su
 * `productFlavors`.
 */
object Store {

    /**
     * Se questa variante può scrivere sulla memoria condivisa con `java.io.File`: eliminare, il
     * cestino, rinominare, copiare, spostare, duplicare, le cartelle nuove, la vista 'Cartelle di
     * sistema', il salvataggio sopra l'originale, la rotazione senza perdita, un editor esterno e
     * 'Pulisci'.
     *
     * ⚠️ **`false` in `play`, e ognuna di quelle funzioni resta fuori finché non è riscritta per il
     * MediaStore**: senza l'accesso a tutti i file fallirebbero sui file di un'altra app.
     */
    val files: Boolean = BuildConfig.FILES

    /**
     * I tasti dei due riquadri dei file (il menu del visualizzatore e la scheda della selezione)
     * che scrivono sui file con `java.io.File`: copia, sposta, rinomina (ripristina, nel cestino).
     *
     * 'Elimina' in AIV Play passa dal MediaStore (`MediaDelete`): il cestino di sistema da Android
     * 11, l'eliminazione definitiva sotto (scelte A1 e B1 dell'utente). 'Rinomina' passa da
     * [allows] quando c'è [renames].
     * ⚠️ **Lo leggono solo quei due riquadri**: anche il modulo Disegno ha un 'Elimina', lo stesso
     * [PadKey.DELETE], e là toglie un elemento del disegno, non un file.
     */
    private val writers = setOf(PadKey.COPY, PadKey.MOVE, PadKey.RENAME)

    /**
     * I tasti che AIV Play lascia fuori per scelta dell'utente, anche se funzionerebbero: 'Lista'
     * della scheda della selezione, che copia i nomi dei file scelti.
     */
    private val trimmed = setOf(PadKey.LIST)

    /**
     * Se un tasto dei riquadri dei file esiste in questa variante.
     *
     * ⚠️ **Filtra anche gli elenchi di fabbrica** (`MENU_KEYS`, `PICK_KEYS`), non solo i riquadri:
     * un tasto assente dal riquadro e presente nella pagina che lo riordina si potrebbe spostare
     * senza vederlo mai.
     */
    fun allows(key: PadKey): Boolean =
        files || (key !in trimmed && (key !in writers || (key == PadKey.RENAME && renames)))

    /*
     * Nel cestino il tasto RENAME è 'Ripristina' (`GridScreen`, `ViewerScreen`), e AIV Play
     * ripristina col MediaStore: il cestino esiste solo dove [renames] è vero, quindi lo stesso
     * tasto copre i due casi, al posto che l'ordine di fabbrica gli dà.
     */

    /**
     * Se questa variante rinomina i file: AIV GitHub con `java.io.File`, AIV Play col MediaStore
     * da Android 11 (`MediaRename`), dove `createWriteRequest` chiede alla finestra di sistema il
     * consenso per i file delle altre app.
     *
     * ⚠️ **Sotto Android 11 AIV Play non rinomina**: Android 10 chiede un consenso per file e
     * Android 9 vuole il permesso di scrittura, e nessuno dei due è stato misurato. Le cartelle in
     * AIV Play non si rinominano mai.
     */
    val renames: Boolean = files || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /**
     * Se questa variante ha la pagina del cestino: AIV GitHub il suo, AIV Play quello di sistema da
     * Android 11, dove esiste `MediaStore.createTrashRequest`. Su Android 9 e 10 AIV Play elimina
     * definitivamente (scelta B1 dell'utente), quindi non c'è niente da mostrare.
     */
    val hasBin: Boolean = files || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /**
     * Se questa variante si aggiorna da sé dalle release di GitHub (`Updater`, scelte C1 e D1
     * dell'utente).
     *
     * ⚠️ **Solo AIV GitHub**: AIV Play lo aggiorna il Play Store, e le regole di Google non
     * permettono a un'app dello store di installare pacchetti. Il permesso è nel manifesto della
     * sola variante `github`.
     */
    val updates: Boolean = files

    /**
     * Le viste della casa in questa variante: AIV Play non ha 'Cartelle di sistema', che legge il
     * disco cartella per cartella ed è dove vive 'Nuova cartella'.
     *
     * La vista salvata si legge anche lei attraverso questo elenco (`Settings.kt`), quindi un file
     * di impostazioni esportato da AIV GitHub con quella vista apre AIV Play sulla griglia.
     */
    val views: List<FolderView> = FolderView.entries.filter { files || it != FolderView.TREE }

    /**
     * Il comando principale di fabbrica sul telefono: il Menu 'Start' in AIV GitHub, la pillola che
     * esce dal suo tasto tondo in AIV Play.
     *
     * ⚠️ **Il valore di AIV Play è una scelta dell'utente, provvisoria**: torna indietro solo
     * quando lo dice lui. Come ogni valore di fabbrica conta solo dove l'archivio non dice niente,
     * quindi chi ha scelto un comando lo tiene.
     */
    val mainControl: MainControl = if (files) MainControl.CORNER else MainControl.PILL

    /*
     * Quello che segue è AIV Play semplificata, per scelta dell'utente: meno impostazioni, ognuna
     * tolta con la sua riga e col valore che lascia fissato. AIV GitHub le tiene tutte.
     */

    /**
     * I comandi principali che questa variante offre sul telefono: AIV Play tiene il FAB e la
     * pillola, e toglie 'Menu basso' e il Menu 'Start'.
     */
    val mainControls: List<MainControl> =
        if (files) MainControl.entries else listOf(MainControl.FAB, MainControl.PILL)

    /** Se l'ultimo elemento visto è segnato in una griglia: AIV Play non ha quel segno. */
    val marksLast: Boolean = files

    /**
     * Se le impostazioni che AIV Play fissa hanno la loro riga: quello che c'è dietro i pannelli,
     * il gradiente e il titolo con le grazie dell'intestazione, il colore delle cartelle, il verso
     * della sequenza e il tasto che modifica l'estensione di un file.
     */
    val fullSettings: Boolean = files

    /**
     * Le impostazioni come le usa questa variante: in AIV Play i valori fissati dall'utente
     * prendono il posto di quello che dice l'archivio.
     *
     * ⚠️ **Lavora alla lettura, non al salvataggio** (`SettingsStore.read`): un archivio scritto da
     * una AIV Play precedente, o un file di impostazioni esportato da AIV GitHub, contiene i valori
     * vecchi, e solo una lettura che li corregge ogni volta mostra l'app voluta. Niente si
     * riscrive, quindi tornare su una scelta non chiede nessuna migrazione.
     * Un comando principale che non c'è più diventa la pillola, il valore di fabbrica di AIV Play.
     */
    fun fit(settings: Settings): Settings = if (files) settings else settings.copy(
        mainControl = settings.mainControl.takeIf { it in mainControls } ?: mainControl,
        panelDepth = PanelDepth.SHADOW,
        frontWash = true,
        frontSerif = false,
        folderColour = FolderColour.NAME,
        reverseSequence = false,
        extRename = false,
        extDownload = false
    )

    /** Quanta parte della raccolta di immagini l'app vede. */
    enum class Access { FULL, PARTIAL, NONE }

    /**
     * Che cosa ha concesso il sistema, chiesto ogni volta e mai ricordato: la persona può
     * cambiarlo nelle impostazioni di sistema senza chiudere l'app.
     *
     * In `play` la concessione parziale (Android 14 e successivi) è una risposta a sé: la persona
     * ha scelto alcune immagini, e le cartelle mostrano quelle. Conta come accesso, e
     * [Access.PARTIAL] permette all'app di dirlo.
     */
    fun access(context: Context): Access {
        fun has(permission: String) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (files) {
            val all = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                // Fino ad Android 10 l'accesso a tutti i file non esiste: la coppia classica di
                // permessi mostra tutto e fa scrivere i comandi dei file (vedi [storagePair]).
                storagePair.all(::has)
            }
            return if (all) Access.FULL else Access.NONE
        }
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (has(Manifest.permission.READ_MEDIA_IMAGES) || has(Manifest.permission.READ_MEDIA_VIDEO)) -> Access.FULL
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                has(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> Access.PARTIAL
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                has(Manifest.permission.READ_EXTERNAL_STORAGE) -> Access.FULL
            else -> Access.NONE
        }
    }

    /** Se le cartelle si possono leggere: anche una concessione parziale conta. */
    fun granted(context: Context): Boolean = access(context) != Access.NONE

    /**
     * I permessi che `github` chiede con la finestra di sistema su Android 9 e 10, dove l'accesso
     * a tutti i file non esiste: lettura e scrittura insieme (scelta B1 dell'utente, 6.23).
     *
     * ⚠️ **Senza la scrittura ogni comando che scrive con `java.io.File`** (eliminare, il cestino,
     * rinominare, copiare, spostare) **fallisce**: su Android 9 per il permesso mancante, su
     * Android 10 perché la memoria separata rifiuta i file delle altre app. Su Android 10 la
     * scrittura funziona solo insieme a `requestLegacyExternalStorage`, nel manifesto di `github`.
     * L'accesso conta come completo solo con tutti e due; i due permessi sono nello stesso gruppo,
     * e Android concede il secondo senza mostrare la sua finestra.
     */
    private val storagePair = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    /**
     * I permessi che la finestra di sistema chiede in questa variante: in `github` solo sotto
     * Android 11, perché da lì l'accesso è l'interruttore in una pagina di sistema.
     */
    internal fun dialogPermissions(): Array<String> = if (files) storagePair.copyOf() else mediaPermissions()

    /**
     * I permessi che `play` chiede con la finestra di sistema.
     *
     * Da Android 14 il terzo va nella stessa richiesta, come indica Android: fa offrire alla
     * finestra la scelta di alcune immagini, e distingue una concessione parziale da un rifiuto.
     * ⚠️ **Da Android 10 c'è anche la posizione delle immagini** (scelta R1 dell'utente, 6.27):
     * senza, il sistema toglie le coordinate dall'EXIF che serve, e nella scheda `Info` manca la
     * riga 'Posizione'. Non fa parte di [granted]: rifiutarla lascia la galleria funzionante,
     * senza quella riga.
     */
    private fun mediaPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            Manifest.permission.ACCESS_MEDIA_LOCATION
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.ACCESS_MEDIA_LOCATION
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_MEDIA_LOCATION
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /**
     * Se si può chiedere l'originale di un'immagine del MediaStore, con le sue coordinate: da
     * Android 10 e col permesso della posizione, che dichiara solo `play`. `github` legge il file
     * stesso e non ne ha bisogno. Vedi [mediaPermissions].
     */
    fun seesLocation(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * La richiesta del permesso delle cartelle, uguale per ogni chiamante: restituisce la funzione
     * da chiamare, e [onAnswer] riceve [granted] quando la persona ha risposto.
     *
     * Le strade sono due: in `github` da Android 11 un interruttore in una pagina di sistema, che
     * non restituisce un risultato, quindi al ritorno l'app richiede lo stato al sistema; in
     * `github` sotto Android 11, e in `play`, la finestra di sistema.
     * ⚠️ **La risposta si legge sempre dal sistema**, mai da quello che dice la finestra: la scelta
     * di alcune immagini risponde 'rifiutato' su un permesso e 'concesso' su un altro.
     *
     * @param explain se la pagina della prima strada arriva con la frase che dice perché
     *   (`folder_why`): una pagina che si apre da sola senza una parola fa chiudere l'app.
     */
    @Composable
    fun rememberAccessRequest(explain: Boolean, onAnswer: (Boolean) -> Unit): () -> Unit {
        val context = LocalContext.current
        val fromSettings = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { onAnswer(granted(context)) }
        val fromDialog = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            /*
             * ⚠️ **Dopo il secondo rifiuto Android non mostra più la sua finestra** (da Android
             * 11), e ogni richiesta successiva risponde subito 'no': senza questo ramo il tasto
             * non farebbe più niente. Android lo segnala non chiedendo più la spiegazione per
             * nessun permesso, e allora resta solo la pagina dell'app nelle impostazioni di
             * sistema.
             */
            val activity = generateSequence(context) { (it as? ContextWrapper)?.baseContext }
                .filterIsInstance<Activity>().firstOrNull()
            val shut = !granted(context) && !files && activity != null &&
                mediaPermissions().none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
            if (shut && runCatching { fromSettings.launch(appPage(context)) }.isSuccess) {
                return@rememberLauncherForActivityResult
            }
            onAnswer(granted(context))
        }
        return remember(fromSettings, fromDialog, explain) {
            {
                if (files && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // ⚠️ L'unico toast di sistema dell'app: la riga dopo manda l'app in secondo
                    // piano, e un avviso suo sparirebbe con la schermata che lo disegna.
                    if (explain) Toast.makeText(context, R.string.folder_why, Toast.LENGTH_LONG).show()
                    // Il ripiego sulla pagina generale serve: su alcuni sistemi la pagina
                    // dell'app manca.
                    val opened = Folder.settingsIntents(context).any {
                        runCatching { fromSettings.launch(it) }.isSuccess
                    }
                    if (!opened) onAnswer(false)
                } else {
                    runCatching { fromDialog.launch(dialogPermissions()) }.onFailure { onAnswer(false) }
                }
            }
        }
    }

    /** La pagina dell'app nelle impostazioni di sistema, dove i permessi si concedono uno per uno. */
    private fun appPage(context: Context) = Intent(
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        "package:${context.packageName}".toUri()
    )

    /**
     * Apre la pagina dell'app nelle impostazioni di sistema, e chiama [onBack] al ritorno.
     *
     * ⚠️ **È quello che fa 'Consenti tutte'**: con una concessione parziale già data, richiedere il
     * permesso mostra la scelta delle immagini del sistema, non la concessione completa. Nella
     * pagina dell'app la persona concede 'Foto e video' per intero.
     */
    @Composable
    fun rememberAppPage(onBack: () -> Unit): () -> Unit {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { onBack() }
        return remember(launcher) { { runCatching { launcher.launch(appPage(context)) } } }
    }
}

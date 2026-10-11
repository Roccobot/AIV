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
 * The two variants of the app, and what each one may do with shared storage.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️⚠️ **This is the only place that reads `BuildConfig.FILES`** (since 5.10): a second reader
 * would be a second answer to the same question, and the two would drift at the first edit.
 * The reason for the variants lives in `build.gradle.kts`, on `productFlavors`.
 */
object Store {

    /**
     * Whether this variant may write on shared storage through `java.io.File`: delete, the
     * bin, rename, copy, move, duplicate, new folders, the 'Cartelle di sistema' view, saving
     * over the original, the lossless turn, an outside editor and 'Pulisci'.
     *
     * ⚠️⚠️ **`false` in `play`, and every one of those functions is out of it until it is
     * rewritten for MediaStore**: without the all-files permission they fail on another app's
     * file, and a command that fails every time is worse than no command.
     */
    val files: Boolean = BuildConfig.FILES

    /**
     * The keys of the two file pads (the viewer's menu and the selection sheet) that write on
     * the files through `java.io.File`: copy, move, rename (restore, in the bin).
     *
     * ⚠️ **'Elimina' is back in AIV Play since 5.20**, through the MediaStore (`MediaDelete`): the
     * system's trash from Android 11, a deletion for good below (his choices A1 and B1).
     * ⚠️ **Only those two pads read it**: the Disegno module has a 'Elimina' too, the same
     * [PadKey.DELETE], and there it removes an element of the drawing, not a file.
     */
    private val writers = setOf(PadKey.COPY, PadKey.MOVE, PadKey.RENAME)

    /**
     * The keys AIV Play leaves out by his choice, not because they cannot work: 'Lista' of the
     * selection sheet, which copies the names of the chosen files, since 5.30 (his note in Altro
     * on the 5.20 round: *copia lista file*).
     */
    private val trimmed = setOf(PadKey.LIST)

    /**
     * Whether a key of the file pads exists in this variant.
     *
     * ⚠️⚠️ **The factory lists filter with it too** (`MENU_KEYS`, `PICK_KEYS`), not only the pads:
     * a key missing from the pad and present in the page that reorders it would be a button
     * that can be moved and never seen.
     */
    fun allows(key: PadKey, bin: Boolean = false): Boolean =
        files || (key !in trimmed && (key !in writers || (bin && key == PadKey.RENAME)))

    /*
     * ⚠️ **In the bin the RENAME key is 'Ripristina'** (`GridScreen`, `ViewerScreen`), and since
     * 5.40 AIV Play restores through the MediaStore, so there [allows] lets it through with
     * `bin`. Its place in the pad comes from `withRestore`, in `ActionPad.kt`.
     */

    /**
     * Whether this variant has a bin page: AIV GitHub its own, AIV Play the system's trash from
     * Android 11 (since 5.40), where `MediaStore.createTrashRequest` exists. On Android 9 and 10
     * AIV Play deletes for good (his choice B1), so there is nothing to show.
     */
    val hasBin: Boolean = files || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /**
     * The views of the home screen in this variant: AIV Play has no 'Cartelle di sistema', which
     * reads the disk folder by folder and is where 'Nuova cartella' lives.
     *
     * ⚠️ **The saved view is read through this list too** (`Settings.kt`), so a settings file
     * exported from AIV GitHub with that view opens AIV Play on the grid.
     */
    val views: List<FolderView> = FolderView.entries.filter { files || it != FolderView.TREE }

    /**
     * The phone's main control as it comes from the factory: the corner menu ('Menu Start') in
     * AIV GitHub, the pill that slides out of its round key in AIV Play.
     *
     * ⚠️⚠️ **AIV Play's value is his choice, and a temporary one** (his note in Altro on the 5.11
     * round: *Fino a nuovo ordine, solo per AIV Play, il pulsante predefinito sarà la pillola a
     * scomparsa*): it goes back only when he says so. As every factory value, it counts only
     * where the store says nothing, so whoever picked a control keeps it.
     */
    val mainControl: MainControl = if (files) MainControl.CORNER else MainControl.PILL

    /*
     * ⚠️⚠️ **What follows is AIV Play made simpler, since 5.30, and every line is his** (his
     * note in Altro on the 5.20 round, headed *solo AIV Play*): fewer settings, each one gone
     * with its row, and the value it leaves behind fixed. AIV GitHub keeps them all.
     */

    /**
     * The phone's main controls this variant offers: AIV Play keeps the FAB and the pill, and
     * drops 'Menu basso' and 'Menu Start' (*rimangono pillola e FAB*).
     */
    val mainControls: List<MainControl> =
        if (files) MainControl.entries else listOf(MainControl.FAB, MainControl.PILL)

    /** Whether the last item seen is marked in a grid: AIV Play has no such mark. */
    val marksLast: Boolean = files

    /**
     * Whether the settings that AIV Play fixes have their rows: what is behind the panels, the
     * gradient and the serif title of the folder header, the colour of the folders, the
     * direction of the sequence and the button that edits a file extension.
     */
    val fullSettings: Boolean = files

    /**
     * The settings as this variant uses them: in AIV Play the values he fixed take the place of
     * whatever the store says.
     *
     * ⚠️⚠️ **It runs on reading, not on saving** (`SettingsStore.read`): a store written by an
     * earlier AIV Play, or a settings file exported from AIV GitHub, holds the old values, and
     * only a reading that corrects them every time shows the app he asked for. Nothing is
     * rewritten, so going back on a choice later costs no migration.
     * ⚠️ **A main control that is gone becomes the pill**, the factory value of AIV Play.
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

    /** How much of the photo library the app sees. */
    enum class Access { FULL, PARTIAL, NONE }

    /**
     * What the system granted, asked every time and never remembered: the person can change
     * it in the system settings without closing the app.
     *
     * ⚠️ **In `play` the partial grant is its own answer** (Android 14 and later): the person
     * picked some photos, and the folders show those. It counts as access, because what is
     * there is real; [Access.PARTIAL] is what lets the app say so.
     */
    fun access(context: Context): Access {
        fun has(permission: String) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (files) {
            val all = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                // Up to Android 10 the wide permission does not exist, and the classic one
                // on storage already shows everything.
                has(Manifest.permission.READ_EXTERNAL_STORAGE)
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

    /** Whether the folders can be read at all: a partial grant counts. */
    fun granted(context: Context): Boolean = access(context) != Access.NONE

    /**
     * The permissions `play` asks with the system dialog.
     *
     * ⚠️ **On Android 14 and later the third one goes in the same request** (Android's own
     * guidance): it is what makes the dialog offer 'select photos' and lets the app tell a
     * partial grant from a refusal.
     */
    private fun mediaPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /**
     * The request for the folder permission, the same for every caller: it returns the
     * function to call, and [onAnswer] receives [granted] once the person has answered.
     *
     * ⚠️⚠️ **Three roads, because the permissions are granted in three ways**: in `github`,
     * from Android 11, a switch in a system page, which returns no result, so on the way back
     * the app asks the system again; in `github` below Android 11, and in `play`, the system
     * dialog. ⚠️ The answer is always read from the system, never from what the dialog says,
     * because 'select photos' answers 'refused' on one permission and 'granted' on another.
     *
     * @param explain whether the page of the first road comes with the sentence that says why
     *   (`folder_why`): a page that opens by itself without a word makes people close the app.
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
             * ⚠️⚠️ **After the second refusal Android stops showing its dialog** (since
             * Android 11), and every later request answers 'no' at once: without this branch
             * the button would do nothing, for good. Android says so by no longer asking for a
             * rationale on any of the permissions, and then the app's page in the system
             * settings is the only place left where the person can grant them.
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
                    // ⚠️⚠️ The only system toast left in the app, since 1.84: the next line
                    // sends the app to the background, so a notice of its own would vanish
                    // with the screen that draws it.
                    if (explain) Toast.makeText(context, R.string.folder_why, Toast.LENGTH_LONG).show()
                    // ⚠️ The fallback on the general page is not a luxury: the page aimed at
                    // the app is missing on some systems.
                    val opened = Folder.settingsIntents(context).any {
                        runCatching { fromSettings.launch(it) }.isSuccess
                    }
                    if (!opened) onAnswer(false)
                } else {
                    val asked = if (files) arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE) else mediaPermissions()
                    runCatching { fromDialog.launch(asked) }.onFailure { onAnswer(false) }
                }
            }
        }
    }

    /** The app's own page in the system settings, where its permissions are granted one by one. */
    private fun appPage(context: Context) = Intent(
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        "package:${context.packageName}".toUri()
    )

    /**
     * Opens the app's page in the system settings, and calls [onBack] on the way back.
     *
     * ⚠️⚠️ **It is what 'Consenti tutte' does, since 5.11** (his note on `5.10-03`: *un tocco non
     * rimanda alla vera autorizzazione: torna alla selezione 'limitata'*): with a partial grant
     * already given, asking the permission again shows the system's choice of photos, not the
     * full grant. On the app's page the person grants 'Foto e video' whole.
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

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
     * the files: copy, move, rename (restore, in the bin) and delete.
     *
     * ⚠️ **Only those two pads read it**: the Disegno module has a 'Elimina' too, the same
     * [PadKey.DELETE], and there it removes an element of the drawing, not a file.
     */
    private val writers = setOf(PadKey.COPY, PadKey.MOVE, PadKey.RENAME, PadKey.DELETE)

    /**
     * Whether a key of the file pads exists in this variant.
     *
     * ⚠️⚠️ **The factory lists filter with it too** (`MENU_KEYS`, `PICK_KEYS`), not only the pads:
     * a key missing from the pad and present in the page that reorders it would be a button
     * that can be moved and never seen.
     */
    fun allows(key: PadKey): Boolean = files || key !in writers

    /**
     * The views of the home screen in this variant: AIV Play has no 'Cartelle di sistema', which
     * reads the disk folder by folder and is where 'Nuova cartella' lives.
     *
     * ⚠️ **The saved view is read through this list too** (`Settings.kt`), so a settings file
     * exported from AIV GitHub with that view opens AIV Play on the grid.
     */
    val views: List<FolderView> = FolderView.entries.filter { files || it != FolderView.TREE }

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

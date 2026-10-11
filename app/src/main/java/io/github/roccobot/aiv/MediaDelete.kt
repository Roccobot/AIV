package io.github.roccobot.aiv

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/*
 * 'Elimina' in AIV Play, through the MediaStore (5.20).
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️⚠️ **His choices of 2026-10-11, A1 and B1**: from Android 11 'Elimina' moves the files into the
 * system's trash (`createTrashRequest`), where the system keeps them about 30 days
 * (`MediaColumns.DATE_EXPIRES`: *the default trashed expiration is typically 30 days*); on Android 9
 * and 10, where that trash does not exist, it deletes them for good after AIV's own confirmation.
 * ⚠️ **Android asks every time, and AIV cannot skip it**: only the phone maker's own gallery may
 * trash without the system's window. On Android 10 a file of another app asks once per file
 * (`RecoverableSecurityException`); on Android 9 the write permission is asked once.
 * ⚠️ AIV GitHub never comes here: it writes through `java.io.File`, and its bin is its own folder.
 */
object MediaDelete {

    /**
     * The MediaStore rows of [uris]: a `content://` passes as it is, a `file://` is looked up by
     * its path. A file the MediaStore does not know has no row, and counts as failed.
     */
    fun rows(context: Context, uris: List<Uri>): List<Uri> = uris.mapNotNull { uri ->
        when (uri.scheme?.lowercase()) {
            "content" -> uri
            "file" -> uri.path?.let { Tree.contentUri(context, File(it)) }
            else -> null
        }
    }

    /** The system's window that moves [rows] into its trash. */
    @RequiresApi(Build.VERSION_CODES.R)
    fun trashRequest(context: Context, rows: List<Uri>): IntentSender =
        MediaStore.createTrashRequest(context.contentResolver, rows, true).intentSender

    /**
     * Deletes [uris] for good, below Android 11.
     *
     * ⚠️ **On Android 10 a file of another app answers with a [RecoverableSecurityException]**, whose
     * window [ask] shows; after a yes the same deletion is tried again. On Android 9 the write
     * permission comes first, from [grant], and without it nothing is deleted.
     */
    suspend fun forGood(
        context: Context,
        uris: List<Uri>,
        ask: SystemAsk
    ): FileTree.Outcome {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !ask.grant(WRITE)) {
            return FileTree.Outcome(0, uris.size)
        }
        val rows = withContext(Dispatchers.IO) { rows(context, uris) }
        var done = 0
        for (row in rows) {
            val gone = try {
                remove(context, row)
            } catch (e: SecurityException) {
                val consent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    (e as? RecoverableSecurityException)?.userAction?.actionIntent?.intentSender
                } else {
                    null
                }
                consent != null && ask.ask(consent) && runCatching { remove(context, row) }.getOrDefault(false)
            }
            if (gone) done++
        }
        return FileTree.Outcome(done, uris.size - done)
    }

    private suspend fun remove(context: Context, row: Uri): Boolean =
        withContext(Dispatchers.IO + NonCancellable) { context.contentResolver.delete(row, null, null) > 0 }

    /** The permission Android 9 wants to delete a file of another app, declared up to Android 9. */
    @Suppress("DEPRECATION")
    private const val WRITE = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
}

/**
 * A system window that answers yes or no, awaited by a suspended job: the trash request, the
 * consent of Android 10, the permission of Android 9.
 *
 * ⚠️⚠️ **It lives at the top of `FileJobDialogs`, never in the branch of a job**: the job goes on
 * after its window closes, and a launcher removed from the composition would never deliver the
 * answer.
 * ⚠️ **If the activity is recreated while the system window is open, the answer is lost** and the
 * job waits: the system window is an activity of its own, and the new launcher does not know the
 * old question. It is accepted, and declared.
 */
class SystemAsk internal constructor(
    private val context: Context,
    private val window: (IntentSenderRequest) -> Unit,
    private val permission: (String) -> Unit
) {
    internal var pending: CompletableDeferred<Boolean>? = null

    /** Shows [sender]'s window and answers whether the person said yes. */
    suspend fun ask(sender: IntentSender): Boolean = await {
        window(IntentSenderRequest.Builder(sender).build())
    }

    /** Asks [name] when it is missing, and answers whether it is granted. */
    suspend fun grant(name: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, name) == PackageManager.PERMISSION_GRANTED) return true
        return await { permission(name) }
    }

    /**
     * ⚠️ **Called on the main thread**, where a launcher wants to be: the jobs run in the screens'
     * scopes and in a `LaunchedEffect`, all of them on the main thread. A hop to the main
     * dispatcher here would wait forever under a caller that blocks that thread.
     */
    private suspend fun await(launch: () -> Unit): Boolean {
        val answer = CompletableDeferred<Boolean>()
        pending = answer
        if (runCatching { launch() }.isFailure) {
            pending = null
            return false
        }
        return answer.await()
    }

    internal fun answer(yes: Boolean) {
        pending?.complete(yes)
        pending = null
    }
}

/** The [SystemAsk] of a screen, with its two launchers. */
@Composable
fun rememberSystemAsk(): SystemAsk {
    val context = LocalContext.current
    val holder = remember { arrayOfNulls<SystemAsk>(1) }
    val window = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        holder[0]?.answer(it.resultCode == Activity.RESULT_OK)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        holder[0]?.answer(it)
    }
    return remember(window, permission) {
        SystemAsk(context, { window.launch(it) }, { permission.launch(it) }).also { holder[0] = it }
    }
}

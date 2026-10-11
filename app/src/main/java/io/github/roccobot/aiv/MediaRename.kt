package io.github.roccobot.aiv

import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/*
 * 'Rinomina' in AIV Play, through the MediaStore (5.50).
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️⚠️ **The same rename as AIV GitHub's, with the MediaStore doing the writing**: the order is
 * alphabetical (`FileTree.ordered`, which the dialog's preview reads too), the numbers come from
 * the template (`renderName`), and the extension is the file's own. Only the way a name changes is
 * different: a new `DISPLAY_NAME` on the file's row, which the system turns into a rename on disk.
 * ⚠️⚠️ **Android's window is the confirmation**, as for 'Elimina' (his choice A1 of the 5.20 round):
 * `createWriteRequest` asks once for all the files, those of other apps included, and AIV adds no
 * window of its own after its rename dialog. A 'no' touches nothing and says nothing.
 * ⚠️ **From Android 11 only** (`Store.renames`): below, `createWriteRequest` does not exist.
 * ⚠️ **The extension never changes here**: AIV Play has no 'Estensione' key (`Store.fit`, his
 * choice of the 5.20 round), and the MediaStore would add the type's extension to a name that
 * disagrees with it.
 */
object MediaRename {

    /** The system's window that lets AIV write [rows], files of other apps included. */
    @RequiresApi(Build.VERSION_CODES.R)
    fun writeRequest(context: Context, rows: List<Uri>): IntentSender =
        MediaStore.createWriteRequest(context.contentResolver, rows).intentSender

    /**
     * Renames [uris] with [template], numbered from [start], after Android's window says yes.
     *
     * ⚠️⚠️ **TWO PHASES, as in `FileTree.rename`, and for the same reason**: renumbering a series
     * that is already numbered makes cycles between the names, and renaming one by one would
     * find the wanted name still taken by a file of the same selection. So every file goes to a
     * temporary name first, and takes its real one only when none of them holds a wanted name.
     * ⚠️ **The temporary name does not start with a dot**, unlike `FileTree.rename`'s: a hidden
     * name is a file the MediaStore stops listing, and the second phase needs the row it renames.
     * If the app is stopped between the two phases, the files keep a name like `aiv-<time>-<n>`,
     * and stay in the gallery.
     * ⚠️ **A name taken by a file outside the selection gets a free one** (`FileTree.freeName`),
     * and a file whose second step fails goes back to its old name.
     * ⚠️ **The names kept in memory are forgotten** ([Names.forget]): the row keeps its address
     * through the rename, so the grid would go on showing the old name.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun rename(
        context: Context,
        uris: List<Uri>,
        template: String,
        start: Int,
        ask: SystemAsk
    ): FileTree.Outcome {
        val plan = withContext(Dispatchers.IO) {
            FileTree.ordered(context, uris).mapIndexedNotNull { at, file ->
                Tree.contentUri(context, file)?.let { row ->
                    Step(file, row, at, renderName(template, start + at, file.extension))
                }
            }
        }
        val window = if (plan.isEmpty()) null else runCatching { writeRequest(context, plan.map { it.row }) }.getOrNull()
        if (window == null) return FileTree.Outcome(0, uris.size)
        if (!ask.ask(window)) return FileTree.Outcome(0, 0)

        return withContext(Dispatchers.IO + NonCancellable) {
            val stamp = System.currentTimeMillis()
            var failed = uris.size - plan.size
            var done = 0
            val touched = ArrayList<String>(plan.size * 2)

            // Phase 1: every file under a name that cannot be anybody's.
            val parked = plan.filter { step ->
                val tail = if (step.file.extension.isEmpty()) "" else ".${step.file.extension}"
                named(context, step.row, "aiv-$stamp-${step.at}$tail").also { if (!it) failed++ }
            }

            // Phase 2: the real name. A name taken now belongs to a file outside the selection.
            for (step in parked) {
                touched += step.file.absolutePath
                val target = step.file.parentFile?.let { FileTree.freeName(it, step.wanted) }
                if (target != null && named(context, step.row, target.name)) {
                    done++
                    touched += target.absolutePath
                } else {
                    failed++
                    named(context, step.row, step.file.name)
                }
                Names.forget(step.row)
                Names.forget(Uri.fromFile(step.file))
            }
            uris.forEach(Names::forget)

            FileTree.scan(context, touched)
            FileTree.Outcome(done, failed)
        }
    }

    /** Gives [row] the name [name], and says whether the MediaStore took it. */
    private fun named(context: Context, row: Uri, name: String): Boolean = runCatching {
        val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, name) }
        context.contentResolver.update(row, values, null, null) > 0
    }.getOrDefault(false)

    /** One file of the rename: where it is, its row, its place in the order and the name it wants. */
    private class Step(val file: File, val row: Uri, val at: Int, val wanted: String)
}

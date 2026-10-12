package io.github.roccobot.aiv

import java.io.File

/** The two saved lists remain independent when the display mode changes. */
enum class FolderMode(override val token: String) : Choice {
    INCLUDED("included"), EXCLUDED("excluded")
}

/**
 * How the home grid orders its folders, from the `5.80` (his request of 2026-10-12).
 *
 * ⚠️ **There is no factory value**: until the person chooses, [Settings.folderOrder] is `null`, the
 * grid stays alphabetical and a window asks once, also to those who update (his answer `C1`).
 * ⚠️ **Only the grid follows it**: the list and the tablet column stay alphabetical, which is his
 * request (*solo per la modalità griglia*).
 */
enum class FolderOrder(override val token: String) : Choice {
    /** By name, with the collator of the language in force: what the grid did up to the `5.71`. */
    ALPHA("alpha"),
    /** The folder holding the most recently modified file first ([Folder.Bucket.latest]). */
    RECENT("recent")
}

/**
 * Portable paths; exclusions may cover a branch or only the directory itself.
 *
 * ⚠️ **Order and favourites live here too, from the `5.80`**: every view of the folders already
 * receives this object and writes it back through `onSelectionChange`, so the long press can add
 * a favourite and the views can arrange themselves without a new parameter in each caller.
 */
data class FolderSelection(
    val mode: FolderMode = FolderMode.EXCLUDED,
    val hidden: Set<String> = emptySet(),
    val exact: Set<String> = emptySet(),
    val included: Set<String> = DEFAULT_INCLUDED_FOLDERS,
    /** The favourite folders as portable paths, in the order he gave them. */
    val favourites: List<String> = emptyList(),
    /** The grid order, `null` until it is chosen. See [FolderOrder]. */
    val order: FolderOrder? = null
) {
    val excluded: Set<String> get() = hidden + exact

    fun covering(path: String): List<String> =
        (coveringOf(hidden, path) + exact.filter { it == portablePath(path) }).distinct()

    fun hidden(path: String): Boolean = hiddenIn(hidden, path) || listedIn(exact, path)

    fun visible(path: String?, peeking: Boolean = false): Boolean = when (mode) {
        FolderMode.INCLUDED -> path != null && listedIn(included, path)
        FolderMode.EXCLUDED -> path == null || peeking || !hidden(path)
    }

    fun remove(paths: Collection<String>): FolderSelection =
        copy(hidden = hidden - paths.toSet(), exact = exact - paths.toSet())

    fun exclude(paths: Collection<String>, recursive: Boolean): FolderSelection {
        val portable = paths.map(::portablePath).toSet()
        return if (recursive) copy(hidden = hidden + portable, exact = exact - portable)
        else copy(exact = exact + portable, hidden = hidden - portable)
    }

    /** Whether the folder at [path], a full path, is a favourite. A favourite is the folder itself, never a branch. */
    fun favourite(path: String): Boolean = portablePath(path) in favourites

    /** Adds the folder at [path] at the end of the favourites, or takes it out if it is there. */
    fun flipFavourite(path: String): FolderSelection {
        val own = portablePath(path)
        return copy(favourites = if (own in favourites) favourites - own else favourites + own)
    }

    /**
     * The folders in the order a view shows them: the favourites first, in their order, then the
     * others.
     *
     * ⚠️ **[buckets] arrive alphabetical** (`Folder.buckets`), so the alphabetical order costs
     * nothing, and the recent one is a stable sort on top of it: two folders touched in the same
     * second keep their alphabetical order.
     * @param grid whether the view is the grid, the only one that follows [order].
     */
    fun arrange(buckets: List<Folder.Bucket>, grid: Boolean): List<Folder.Bucket> {
        val base = if (grid && order == FolderOrder.RECENT) buckets.sortedByDescending { it.latest } else buckets
        if (favourites.isEmpty()) return base
        val rank = favourites.withIndex().associate { (at, path) -> path to at }
        val (first, rest) = base.partition { b -> b.path?.let { rank[portablePath(it)] } != null }
        return first.sortedBy { b -> rank.getValue(portablePath(b.path!!)) } + rest
    }
}

// Absent folders produce no media bucket. These defaults also work after restoring to another phone.
internal val DEFAULT_INCLUDED_FOLDERS = setOf("DCIM/Camera", "Pictures/Screenshots", "Movies")

internal val Settings.folderSelection: FolderSelection
    get() = FolderSelection(
        folderMode, hiddenFolders, hiddenExactFolders, includedFolders, favouriteFolders, folderOrder
    )

internal fun Settings.withFolders(selection: FolderSelection): Settings = copy(
    folderMode = selection.mode, hiddenFolders = selection.hidden,
    hiddenExactFolders = selection.exact, includedFolders = selection.included,
    favouriteFolders = selection.favourites, folderOrder = selection.order
)

/** Reads direct directories on disk, including empty ones; storage aliases remain valid. */
internal fun childFolders(parent: File): List<File> =
    runCatching { parent.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name } }
        .getOrNull().orEmpty()

internal fun siblingFolders(path: String, selection: FolderSelection): List<File> {
    val own = File(path)
    val parent = own.parentFile ?: return emptyList()
    val children = childFolders(parent)
    return if (children.any { it.absolutePath != own.absolutePath && !selection.hidden(it.absolutePath) })
        children else emptyList()
}

/** Seeds existing folders once; both common screenshot locations and removable volumes count. */
internal fun defaultIncludedFolders(roots: List<File>, buckets: List<Folder.Bucket>): Set<String> {
    val names = setOf("Camera", "Screenshots", "Movies")
    val paths = buckets.filter { it.name in names }.mapNotNull { it.path }
    val conventional = roots.flatMap { root ->
        listOf("DCIM/Camera", "Pictures/Screenshots", "DCIM/Screenshots", "Movies")
            .map { File(root, it) }.filter { it.isDirectory }.map { it.absolutePath }
    }
    return (paths + conventional).map(::portablePath).toSet()
}

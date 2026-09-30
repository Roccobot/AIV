package io.github.roccobot.aiv

import java.io.File

/** The two saved lists remain independent when the display mode changes. */
enum class FolderMode(override val token: String) : Choice {
    INCLUDED("included"), EXCLUDED("excluded")
}

/** Portable paths; exclusions may cover a branch or only the directory itself. */
data class FolderSelection(
    val mode: FolderMode = FolderMode.EXCLUDED,
    val hidden: Set<String> = emptySet(),
    val exact: Set<String> = emptySet(),
    val included: Set<String> = DEFAULT_INCLUDED_FOLDERS
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
}

// Absent folders produce no media bucket. These defaults also work after restoring to another phone.
internal val DEFAULT_INCLUDED_FOLDERS = setOf("DCIM/Camera", "Pictures/Screenshots", "Movies")

internal val Settings.folderSelection: FolderSelection
    get() = FolderSelection(folderMode, hiddenFolders, hiddenExactFolders, includedFolders)

internal fun Settings.withFolders(selection: FolderSelection): Settings = copy(
    folderMode = selection.mode, hiddenFolders = selection.hidden,
    hiddenExactFolders = selection.exact, includedFolders = selection.included
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

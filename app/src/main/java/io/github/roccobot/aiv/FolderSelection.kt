package io.github.roccobot.aiv

import java.io.File

/** Le due liste salvate restano indipendenti quando cambia la modalità. */
enum class FolderMode(override val token: String) : Choice {
    INCLUDED("included"), EXCLUDED("excluded")
}

/**
 * Come la griglia della casa ordina le cartelle.
 *
 * ⚠️ **Non c'è un valore di fabbrica**: finché la persona non sceglie, [Settings.folderOrder] è
 * `null`, la griglia resta alfabetica e una finestra lo chiede una volta, anche a chi aggiorna
 * (risposta `C1` dell'utente, 5.80).
 * ⚠️ **Lo segue solo la griglia**: la lista e la colonna del tablet restano alfabetiche, per scelta
 * dell'utente.
 */
enum class FolderOrder(override val token: String) : Choice {
    /** Per nome, con l'ordinamento della lingua in vigore. */
    ALPHA("alpha"),
    /** Prima la cartella col file modificato più di recente ([Folder.Bucket.latest]). */
    RECENT("recent")
}

/**
 * Percorsi portabili; un'esclusione può coprire un ramo o la sola cartella.
 *
 * Qui vivono anche l'ordine e le preferite: ogni vista delle cartelle riceve già questo oggetto e
 * lo riscrive con `onSelectionChange`, quindi la pressione lunga aggiunge una preferita e le viste
 * si ordinano senza un parametro nuovo in ogni chiamante.
 */
data class FolderSelection(
    val mode: FolderMode = FolderMode.EXCLUDED,
    val hidden: Set<String> = emptySet(),
    val exact: Set<String> = emptySet(),
    val included: Set<String> = DEFAULT_INCLUDED_FOLDERS,
    /** Le cartelle preferite come percorsi portabili, nell'ordine in cui le ha messe l'utente. */
    val favourites: List<String> = emptyList(),
    /** L'ordine della griglia, `null` finché non è scelto. Vedi [FolderOrder]. */
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

    /** Se la cartella in [path], un percorso completo, è preferita: la cartella stessa, mai un ramo. */
    fun favourite(path: String): Boolean = portablePath(path) in favourites

    /** Mette la cartella in [path] in fondo alle preferite, o la toglie se c'è già. */
    fun flipFavourite(path: String): FolderSelection {
        val own = portablePath(path)
        return copy(favourites = if (own in favourites) favourites - own else favourites + own)
    }

    /**
     * Le cartelle nell'ordine in cui una vista le mostra: prima le preferite, nel loro ordine, poi
     * le altre.
     *
     * ⚠️ **[buckets] arrivano in ordine alfabetico** (`Folder.buckets`), quindi l'ordine alfabetico
     * non costa niente, e quello per data è un ordinamento stabile sopra di lui: due cartelle
     * toccate nello stesso secondo restano in ordine alfabetico.
     * @param grid se la vista è la griglia, l'unica che segue [order].
     */
    fun arrange(buckets: List<Folder.Bucket>, grid: Boolean): List<Folder.Bucket> {
        val base = if (grid && order == FolderOrder.RECENT) buckets.sortedByDescending { it.latest } else buckets
        if (favourites.isEmpty()) return base
        val rank = favourites.withIndex().associate { (at, path) -> path to at }
        val (first, rest) = base.partition { b -> b.path?.let { rank[portablePath(it)] } != null }
        return first.sortedBy { b -> rank.getValue(portablePath(b.path!!)) } + rest
    }
}

// Una cartella assente non produce nessun gruppo; questi valori valgono anche dopo il ripristino su un altro telefono.
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

/** Le sottocartelle dirette sul disco, anche vuote; i percorsi alias della memoria restano validi. */
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

/** Le cartelle incluse di partenza, fra quelle che esistono: contano tutte e due le sedi comuni delle schermate e le memorie rimovibili. */
internal fun defaultIncludedFolders(roots: List<File>, buckets: List<Folder.Bucket>): Set<String> {
    val names = setOf("Camera", "Screenshots", "Movies")
    val paths = buckets.filter { it.name in names }.mapNotNull { it.path }
    val conventional = roots.flatMap { root ->
        listOf("DCIM/Camera", "Pictures/Screenshots", "DCIM/Screenshots", "Movies")
            .map { File(root, it) }.filter { it.isDirectory }.map { it.absolutePath }
    }
    return (paths + conventional).map(::portablePath).toSet()
}

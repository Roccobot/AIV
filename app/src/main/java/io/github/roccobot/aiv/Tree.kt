package io.github.roccobot.aiv

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * La memoria del telefono come si naviga davvero: cartelle dentro cartelle.
 *
 * ⚠️ **È la vista che giustifica l'accesso a tutti i file**: il permesso si chiede per navigare
 * la memoria come un gestore di file, ed è la domanda che il Play Store fa nel modulo dei
 * permessi sensibili.
 *
 * ⚠️ **Non passa dal MediaStore**, che conosce solo le cartelle con qualcosa di già indicizzato:
 * qui si legge il disco, che con quel permesso è leggibile per intero, quindi compaiono anche le
 * cartelle vuote, quelle di documenti e i file non ancora indicizzati. Il tipo di un file si
 * deduce dall'estensione, come in `Folder.fromDisk`: un file senza estensione è un file e basta.
 */
object Tree {

    /**
     * Una memoria da cui si può partire: quella interna, e le schede se ce ne sono.
     *
     * Il nome arriva dal sistema (`getDescription`), già tradotto nella lingua del telefono.
     */
    data class Root(val file: File, val name: String)

    /**
     * Una riga della vista: una cartella o un file, con quello che serve a disegnarla.
     *
     * Il tipo si decide qui, una volta per riga, perché durante lo scorrimento ogni riga si
     * ridisegna molte volte. Non c'è il conto di quello che una cartella contiene: costerebbe una
     * lettura di directory per riga prima di poter disegnare.
     */
    data class Spot(
        val file: File,
        val folder: Boolean,
        /** Immagine o video: quello su cui l'app sa fare qualcosa. */
        val media: Boolean,
        /** Video: serve al segno sulla riga. */
        val clip: Boolean,
        val size: Long,
        val stamp: Long
    ) {
        val name: String get() = file.name
        val path: String get() = file.absolutePath
    }

    /**
     * Da dove si comincia.
     *
     * `StorageVolume.directory`, l'unica via che dà anche le schede SD, esiste da Android 11; sotto,
     * il permesso a tutti i file non esiste e basta la memoria interna. Si tengono solo le memorie
     * leggibili: una scheda smontata compare comunque nell'elenco del sistema.
     */
    fun roots(context: Context): List<Root> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val manager = context.getSystemService(StorageManager::class.java)
            val volumes = runCatching { manager?.storageVolumes }.getOrNull().orEmpty()
            val found = volumes.mapNotNull { volume ->
                val dir = volume.directory ?: return@mapNotNull null
                if (!dir.canRead()) return@mapNotNull null
                Root(dir, volume.getDescription(context) ?: dir.name)
            }
            if (found.isNotEmpty()) return found
        }
        @Suppress("DEPRECATION")
        val fallback = runCatching { Environment.getExternalStorageDirectory() }.getOrNull()
        return listOfNotNull(fallback?.takeIf { it.canRead() }?.let { Root(it, it.name) })
    }

    /**
     * Quello che c'è dentro una cartella, **prima le cartelle e poi i file**, per nome.
     *
     * L'ordine non è quello della galleria: in un gestore di file si cerca un nome, e le cartelle
     * sono in cima perché si naviga più spesso di quanto si apra. Senza distinzione fra maiuscole e
     * minuscole, o `Zip` finirebbe prima di `avi`.
     * I file che cominciano con un punto, nascosti per convenzione (`.thumbnails`, `.trashed`),
     * compaiono solo con [hidden].
     * ⚠️ **Una cartella che non si può leggere torna vuota, senza errore**: succede davvero
     * (`/storage/emulated/0/Android/data` è chiusa anche con l'accesso a tutti i file).
     */
    suspend fun list(
        dir: File,
        hidden: Boolean = false,
        onlyPictures: Boolean = false
    ): List<Spot> = withContext(Dispatchers.IO) {
        val kids = runCatching { dir.listFiles() }.getOrNull().orEmpty()
        kids.asSequence()
            .filter { hidden || !it.name.startsWith('.') }
            .filter { it.isDirectory || !MediaFiles.ignored(it.name) }
            .map { spot(it) }
            .filter { !onlyPictures || !it.folder || leadsToMedia(it.file, hidden, DIG) }
            .sortedWith(
                compareBy<Spot> { !it.folder }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )
            .toList()
    }

    /**
     * Se dentro questa cartella, o poco sotto, c'è almeno un'immagine o un video.
     *
     * ⚠️ **Guarda anche nei piani sotto**: le foto sono in `DCIM/Camera`, e un controllo sui soli
     * figli diretti nasconderebbe `DCIM` e la strada per arrivarci.
     * ⚠️ **Ma si ferma a [DIG] piani**: senza limite scandirebbe tutta la memoria a ogni cartella
     * aperta. Tre piani bastano per i casi veri (`DCIM/Camera`, `Android/media/...`); una cartella
     * con le immagini più in fondo sparisce finché l'opzione è accesa.
     * Si ferma alla prima cosa trovata.
     */
    private fun leadsToMedia(dir: File, hidden: Boolean, left: Int): Boolean {
        if (left <= 0) return false
        val kids = runCatching { dir.listFiles() }.getOrNull().orEmpty()
        val seen = kids.filter { hidden || !it.name.startsWith('.') }
        if (seen.any { !it.isDirectory && isMedia(it) }) return true
        return seen.any { it.isDirectory && leadsToMedia(it, hidden, left - 1) }
    }

    /** Se questo file è un'immagine o un video, guardando la sola estensione. */
    private fun isMedia(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in Videos.EXTENSIONS || Folder.isPicture(ext)
    }

    /** Quanti piani si scendono cercando un'immagine: vedi [leadsToMedia]. */
    private const val DIG = 3

    private fun spot(file: File): Spot {
        val folder = runCatching { file.isDirectory }.getOrDefault(false)
        val ext = file.extension.lowercase()
        val clip = !folder && ext in Videos.EXTENSIONS
        return Spot(
            file = file,
            folder = folder,
            media = !folder && (clip || Folder.isPicture(ext)),
            clip = clip,
            size = if (folder) 0L else runCatching { file.length() }.getOrDefault(0L),
            stamp = runCatching { file.lastModified() }.getOrDefault(0L)
        )
    }

    /**
     * La cartella di sopra, e `null` quando si è già su una radice.
     *
     * ⚠️ **Il confronto è sui percorsi canonici**: due `File` scritti in modo diverso sullo stesso
     * percorso non sono uguali, e `/sdcard` è un collegamento a `/storage/emulated/0`. Senza, la
     * risalita passerebbe oltre la radice, dentro `/storage`, da cui non si torna indietro.
     */
    fun parent(dir: File, roots: List<Root>): File? {
        val here = real(dir)
        if (roots.any { real(it.file) == here }) return null
        val up = dir.parentFile ?: return null
        // Oltre le radici non si sale: la cartella di sopra non appartiene all'utente ed è quasi
        // sempre illeggibile.
        val reachable = roots.any { real(it.file).let { root -> here.startsWith("$root/") } }
        return if (reachable) up else null
    }

    private fun real(file: File): String =
        runCatching { file.canonicalPath }.getOrElse { file.absolutePath }

    /**
     * L'indirizzo `content://` di un file qualunque, e `null` se il MediaStore non lo conosce.
     *
     * Serve dove un file letto dal disco deve passare dal MediaStore: per consegnarlo a un altro
     * programma senza allargare il nostro FileProvider (un `file://` da fuori non è leggibile),
     * e per rinominarlo o cancellarlo (`MediaRename`, `MediaDelete`). Il MediaStore indicizza
     * anche documenti e archivi, quindi per quasi ogni file la riga esiste già.
     * ⚠️ **Qui non c'è il filtro sui tipi di `Folder`**: si cerca qualunque riga, anche di un file
     * che non è né un'immagine né un video.
     * Una query per file toccato: si chiama nel gesto, non mentre si scorre.
     */
    fun contentUri(context: Context, file: File): Uri? = runCatching {
        @Suppress("DEPRECATION")
        val column = MediaStore.Files.FileColumns.DATA
        val table = MediaStore.Files.getContentUri("external")
        context.contentResolver.query(
            table,
            arrayOf(MediaStore.Files.FileColumns._ID),
            "$column = ?",
            arrayOf(file.absolutePath),
            null
        )?.use { c ->
            if (c.moveToFirst()) ContentUris.withAppendedId(table, c.getLong(0)) else null
        }
    }.getOrNull()
}

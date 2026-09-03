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
 * ⚠️⚠️ **QUESTA È LA VISTA CHE GIUSTIFICA IL PERMESSO PESANTE, e non è un modo di dire.**
 * L'accesso a **tutti i file** si chiede per poter navigare la memoria come fa un gestore di
 * file; senza questa vista, un visualizzatore di immagini quel permesso non lo saprebbe
 * spiegare, ed è esattamente la domanda che il Play Store fa nel modulo dei permessi
 * sensibili. Le due cose vanno lette insieme.
 *
 * ⚠️⚠️ **NON PASSA DAL MEDIASTORE, ed è tutto il punto**: il MediaStore conosce le cartelle
 * che contengono già qualcosa di indicizzato, quindi non sa niente di una cartella vuota, di
 * una cartella di documenti, o di un file che l'indicizzazione non ha ancora visto. Qui si
 * legge il **disco**, che con quel permesso è leggibile per intero.
 * ⚠️ Il prezzo, dichiarato: senza MediaStore non c'è nessuno che dica di che tipo è un file,
 * e lo si deduce dall'**estensione**, come già fa il ripiego di `Folder.fromDisk`. Un file
 * senza estensione qui è un file e basta, anche se è una fotografia.
 */
object Tree {

    /**
     * Una memoria da cui si può partire: quella interna, e le schede se ce ne sono.
     *
     * ⚠️ Il **nome** arriva dal sistema (`getDescription`) e non è cosmetica: 'Memoria
     * interna condivisa' e il nome della scheda SD sono già tradotti dal telefono, e
     * scriverli noi vorrebbe dire due stringhe in ventotto lingue per dire quello che il
     * sistema dice meglio.
     */
    data class Root(val file: File, val name: String)

    /**
     * Una riga della vista: una cartella o un file, con quello che serve a disegnarla.
     *
     * ⚠️ **Il tipo si decide QUI, una volta per riga, e non nel composabile**: durante lo
     * scorrimento ogni riga si ridisegna molte volte, e confrontare estensioni a ogni
     * fotogramma è lavoro speso per una risposta che non cambia mai.
     * ⚠️ **Non c'è nessun conteggio di quello che una cartella contiene**, ed è una scelta di
     * costo: saperlo vuol dire aprire ogni sottocartella, cioè una lettura di directory per
     * riga, e una cartella con duecento sottocartelle diventerebbe duecento letture prima di
     * poter disegnare qualsiasi cosa.
     */
    data class Spot(
        val file: File,
        val folder: Boolean,
        /** Immagine o filmato: è quello su cui l'app sa fare qualcosa. */
        val media: Boolean,
        /** Filmato: serve al segno sulla riga, e distingue i due generi di media. */
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
     * ⚠️ **`StorageManager` da Android 11 in su, e il ripiego sotto**: `StorageVolume.directory`
     * esiste da lì, ed è la sola via che dà anche le **schede SD**. Sotto Android 11 il
     * permesso a tutti i file non esiste nemmeno, quindi là basta la memoria interna, che è
     * l'unica che il permesso classico apre.
     * ⚠️ **Si tengono solo le memorie LEGGIBILI**: una scheda smontata compare comunque
     * nell'elenco del sistema, e una radice che risponde 'non posso' sarebbe una voce che si
     * tocca e non fa niente.
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
     * ⚠️⚠️ **L'ORDINE NON È QUELLO DELLA GALLERIA, ed è deliberato**: le fotografie si
     * ordinano per data perché quello che si cerca è 'l'ultima che ho scattato', mentre in un
     * gestore di file quello che si cerca è **un nome**, e un elenco per data costringe a
     * leggerlo tutto. Cartelle in cima per la stessa ragione: si naviga più spesso di quanto
     * si apra.
     * ⚠️ **Senza distinzione fra maiuscole e minuscole**, o `Zip` finirebbe prima di `avi`
     * come nell'ordine dei codici, che è la cosa che fa sembrare rotto un elenco.
     * ⚠️ **I file che cominciano con un punto restano fuori**: sono nascosti per convenzione
     * su ogni sistema, e nelle cartelle di sistema sono la maggioranza silenziosa
     * (`.thumbnails`, `.trashed`). Non c'è un interruttore per mostrarli, e se servirà si
     * aggiungerà: metterlo adesso vorrebbe dire un'impostazione in ventotto lingue per un
     * caso che nessuno ha chiesto.
     * ⚠️ **Una cartella che non si può leggere torna VUOTA e non va in errore**: succede davvero
     * (`/storage/emulated/0/Android/data` è chiusa anche col permesso pesante), e chi entra
     * deve vedere 'niente qui dentro' invece di un errore.
     */
    suspend fun list(
        dir: File,
        hidden: Boolean = false,
        onlyPictures: Boolean = false
    ): List<Spot> = withContext(Dispatchers.IO) {
        val kids = runCatching { dir.listFiles() }.getOrNull().orEmpty()
        kids.asSequence()
            .filter { hidden || !it.name.startsWith('.') }
            .map { spot(it) }
            .filter { !onlyPictures || !it.folder || leadsToMedia(it.file, hidden, DIG) }
            .sortedWith(
                compareBy<Spot> { !it.folder }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )
            .toList()
    }

    /**
     * Se dentro questa cartella, o poco sotto, c'è almeno una fotografia o un filmato.
     *
     * ⚠️⚠️ **GUARDA IN PROFONDITÀ E NON SOLO DENTRO, e senza questo l'opzione sarebbe una
     * TRAPPOLA**: su un telefono le foto stanno in `DCIM/Camera`, cioè un piano sotto, e un
     * controllo che guardasse solo i figli diretti nasconderebbe `DCIM` insieme alla strada
     * per arrivarci. L'opzione si chiama 'mostra solo le cartelle con immagini' e finirebbe
     * per nascondere proprio quelle che le contengono.
     * ⚠️⚠️ **MA CON UN FONDO, e il fondo è la ragione per cui la funzione è usabile**: una
     * ricerca senza limite su una memoria da 128 GB scandirebbe tutto il telefono a ogni
     * cartella aperta. Tre piani bastano per i casi veri (`DCIM/Camera`,
     * `Android/media/...`), e chi ha le foto più in fondo vede la cartella sparire: è il
     * prezzo dichiarato, e si paga spegnendo l'opzione.
     * ⚠️ Si ferma alla **prima** cosa trovata: non conta niente, risponde sì o no.
     */
    private fun leadsToMedia(dir: File, hidden: Boolean, left: Int): Boolean {
        if (left <= 0) return false
        val kids = runCatching { dir.listFiles() }.getOrNull().orEmpty()
        val seen = kids.filter { hidden || !it.name.startsWith('.') }
        if (seen.any { !it.isDirectory && isMedia(it) }) return true
        return seen.any { it.isDirectory && leadsToMedia(it, hidden, left - 1) }
    }

    /** Se questo file è una fotografia o un filmato, guardando la sola estensione. */
    private fun isMedia(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in Videos.EXTENSIONS || Folder.isPicture(ext)
    }

    /**
     * Quanti piani si scendono cercando un'immagine. Tre: vedi [leadsToMedia].
     */
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
     * La cartella che sta sopra, e `null` quando si è già su una radice.
     *
     * ⚠️⚠️ **Il confronto è sui PERCORSI e non sugli oggetti `File`**: due `File` costruiti
     * su percorsi equivalenti ma scritti diversi non sono uguali, e la risalita passerebbe
     * **oltre** la radice, dentro `/storage`, dove non c'è niente da vedere e da cui non si
     * torna indietro.
     * ⚠️ **`canonicalPath` e non `absolutePath`**: `/sdcard` è un collegamento a
     * `/storage/emulated/0`, e senza risolverlo la stessa cartella avrebbe due nomi, uno dei
     * quali non combacia con nessuna radice.
     */
    fun parent(dir: File, roots: List<Root>): File? {
        val here = real(dir)
        if (roots.any { real(it.file) == here }) return null
        val up = dir.parentFile ?: return null
        // Oltre le radici non si sale: la cartella di sopra esiste sul disco ma non è
        // roba dell'utente, ed è quasi sempre illeggibile.
        val reachable = roots.any { real(it.file).let { root -> here.startsWith("$root/") } }
        return if (reachable) up else null
    }

    private fun real(file: File): String =
        runCatching { file.canonicalPath }.getOrElse { file.absolutePath }

    /**
     * L'indirizzo `content://` di un file qualunque, e `null` se il MediaStore non lo conosce.
     *
     * ⚠️⚠️ **SERVE A CONSEGNARE UN FILE A UN ALTRO PROGRAMMA senza allargare il nostro
     * FileProvider**: un `file://` non è leggibile da fuori (da Android 7 il sistema lo rifiuta),
     * e la via comoda sarebbe esporre tutta la memoria attraverso il nostro provider. Il
     * MediaStore indicizza anche i documenti e gli archivi, quindi per la stragrande
     * maggioranza dei file un indirizzo concedibile **esiste già** ed è suo.
     * ⚠️ **Qui NON c'è il filtro sui tipi di `Folder`**, ed è la differenza che rende questa
     * query diversa da tutte quelle: là si cercano fotografie e filmati, qui si cerca
     * qualunque riga, perché il caso d'uso è precisamente il file che non è né l'una né
     * l'altro.
     * ⚠️ **Una query per FILE TOCCATO, non per riga disegnata**: si chiama nel gesto, non
     * mentre si scorre.
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

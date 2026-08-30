package io.github.roccobot.aiv

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Le cartelle del telefono come le vede il **filesystem**, non come le racconta la
 * galleria.
 *
 * ⚠️⚠️ **È UN'ALTRA COSA DA `Folder`, e la differenza è il motivo per cui questo file
 * esiste** (precisazione dell'utente, 2026-08-29: la destinazione di copia e sposta deve
 * essere *una vista del filesystem, NON degli album di foto*). `Folder` interroga il
 * MediaStore, quindi per costruzione conosce **solo le cartelle che contengono già
 * fotografie**: come elenco di destinazioni è esattamente l'insieme sbagliato, perché
 * lascia fuori le cartelle vuote, quelle che l'utente ha escluso e quelle che di immagini
 * non ne hanno mai viste.
 * ⚠️ **Serve il permesso pesante**, che l'app ha già: senza `MANAGE_EXTERNAL_STORAGE` una
 * `listFiles` sulla memoria condivisa torna `null`, e non per un errore ma per progetto.
 */
object FileTree {

    /**
     * Da dove si comincia a navigare: la memoria condivisa, più le schede se ce ne sono.
     *
     * ⚠️⚠️ **Le schede si ricavano dalle cartelle DELL'APP e non da `StorageManager`**, ed
     * è la via che funziona su tutte le versioni: `StorageVolume.getDirectory` esiste solo
     * da Android 11, mentre `getExternalFilesDirs` torna una voce per ogni volume montato
     * da sempre. Il punto in cui tagliare è `/Android/`, che sta in mezzo per definizione:
     * il percorso è `<radice>/Android/data/<pacchetto>/files`.
     * ⚠️ Insieme ordinato e non lista: sui telefoni senza scheda le due strade danno la
     * **stessa** cartella, e senza questo comparirebbe due volte.
     */
    fun roots(context: Context): List<File> {
        val found = LinkedHashSet<File>()
        runCatching { Environment.getExternalStorageDirectory() }.getOrNull()?.let { found += it }
        runCatching { context.getExternalFilesDirs(null) }.getOrNull()
            ?.filterNotNull()
            ?.forEach { dir ->
                val path = dir.absolutePath
                val cut = path.indexOf("/Android/")
                if (cut > 0) found += File(path.substring(0, cut))
            }
        return found.filter { runCatching { it.isDirectory }.getOrDefault(false) }
    }

    /**
     * Le sottocartelle di una cartella, in ordine alfabetico e senza distinguere le
     * maiuscole.
     *
     * ⚠️ **Solo cartelle**: qui si sceglie una destinazione, e un elenco che mostrasse
     * anche i file costringerebbe a scorrere trecento fotografie per trovare la cartella
     * sotto.
     * ⚠️ **Le nascoste (quelle col punto) restano fuori**: sono cartelle di servizio, e in
     * un elenco di destinazioni sono rumore. Chi un domani ne avesse bisogno tolga questo
     * filtro, che è una riga sola e non una struttura.
     * ⚠️ `listFiles` torna `null` su una cartella che non si può leggere, e succede anche
     * col permesso pesante (`/storage/emulated/0/Android/data`, per dirne una): là si
     * mostra una cartella **vuota**, non un errore, perché non poterci entrare non è un
     * guasto dell'app.
     */
    suspend fun children(dir: File): List<File> = withContext(Dispatchers.IO) {
        runCatching { dir.listFiles() }.getOrNull().orEmpty()
            .filter { it.isDirectory && !it.name.startsWith(".") }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Copia le immagini scelte dentro [dest], e dice com'è andata.
     *
     * ⚠️⚠️ **NON SOVRASCRIVE MAI: un nome già preso diventa `nome (2).ext`.** Una copia
     * che sovrascrive è una cancellazione travestita, e questa funzione è nata proprio
     * perché la copia doveva essere l'operazione che non può far danni.
     * ⚠️⚠️ **E il MediaStore va AVVISATO, o la galleria resta indietro**: scrivendo il file
     * a mano il provider non se ne accorge, quindi le copie non comparirebbero fra le foto
     * finché il sistema non rifà una scansione per conto suo, cioè quando vuole lui. Chi
     * aggiungerà sposta ed elimina deve avvisarlo **sui due percorsi**, la partenza e
     * l'arrivo.
     * ⚠️ **Una copia andata storta non ferma le altre**: su cinquanta file, un permesso
     * negato o un disco pieno a metà strada non deve buttare via il lavoro fatto. Il conto
     * dei falliti torna a chi chiama, che lo dice.
     */
    suspend fun copy(context: Context, uris: List<Uri>, dest: File): Copied =
        withContext(Dispatchers.IO) {
            var done = 0
            var failed = 0
            val written = ArrayList<String>(uris.size)
            for (uri in uris) {
                val ok = runCatching {
                    val name = displayName(context, uri) ?: return@runCatching false
                    val target = freeName(dest, name)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { input.copyTo(it) }
                    } ?: return@runCatching false
                    written += target.absolutePath
                    true
                }.getOrDefault(false)
                if (ok) done++ else failed++
            }
            if (written.isNotEmpty()) {
                runCatching {
                    MediaScannerConnection.scanFile(context, written.toTypedArray(), null, null)
                }
            }
            Copied(done, failed)
        }

    /** Quante ne sono arrivate e quante no. */
    data class Copied(val done: Int, val failed: Int)

    /**
     * Un nome libero dentro [dir], partendo da [name].
     *
     * ⚠️ **L'estensione non si tocca e il contatore va prima di lei**: `foto (2).jpg` e non
     * `foto.jpg (2)`, o il file smetterebbe di essere una fotografia per il sistema.
     * ⚠️ Il tetto esiste perché questo è un ciclo su un disco che qualcun altro può
     * scrivere: senza, un caso patologico girerebbe per sempre invece di fallire.
     */
    private fun freeName(dir: File, name: String): File {
        val first = File(dir, name)
        if (!first.exists()) return first
        val stem = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        val tail = if (ext.isEmpty()) "" else ".$ext"
        for (n in 2..999) {
            val candidate = File(dir, "$stem ($n)$tail")
            if (!candidate.exists()) return candidate
        }
        return File(dir, "$stem (${System.currentTimeMillis()})$tail")
    }

    /** Il nome del file dietro un indirizzo, che è quello che la copia deve conservare. */
    private fun displayName(context: Context, uri: Uri): String? {
        if (uri.scheme?.lowercase() == "file") return uri.lastPathSegment
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null, null, null
            )?.use { c ->
                val at = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (at >= 0 && c.moveToFirst()) c.getString(at) else null
            }
        }.getOrNull() ?: uri.lastPathSegment
    }
}

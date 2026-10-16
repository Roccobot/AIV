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
 * 'Rinomina' in AIV Play, attraverso il MediaStore.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * La stessa rinomina di AIV GitHub: l'ordine è alfabetico (`FileTree.ordered`, che legge anche
 * l'anteprima della finestra), i numeri vengono dal modello (`renderName`) e l'estensione è quella
 * del file. Cambia solo il modo: un nuovo `DISPLAY_NAME` sulla riga del file, che il sistema
 * trasforma in una rinomina sul disco.
 * ⚠️ **La conferma è la finestra di Android**, come per 'Elimina' (scelta A1 dell'utente, giro
 * della 5.20): `createWriteRequest` chiede una volta per tutti i file, anche quelli di altre app,
 * e AIV non aggiunge finestre sue. Un 'no' non tocca niente e non dice niente.
 * Solo da Android 11 (`Store.renames`): sotto, `createWriteRequest` non esiste.
 * ⚠️ **L'estensione qui non cambia mai**: AIV Play non ha il tasto 'Estensione' (`Store.fit`,
 * scelta dell'utente, giro della 5.20), e il MediaStore aggiungerebbe l'estensione del tipo a un
 * nome che non le corrisponde.
 */
object MediaRename {

    /** La finestra di sistema che permette ad AIV di scrivere [rows], anche file di altre app. */
    @RequiresApi(Build.VERSION_CODES.R)
    fun writeRequest(context: Context, rows: List<Uri>): IntentSender =
        MediaStore.createWriteRequest(context.contentResolver, rows).intentSender

    /**
     * Rinomina [uris] col modello [template], numerati da [start], dopo il sì della finestra di
     * Android.
     *
     * ⚠️ **Due fasi, come in `FileTree.rename`**: rinumerare una serie già numerata crea cicli
     * fra i nomi, e una rinomina alla volta troverebbe il nome voluto ancora occupato da un file
     * della stessa selezione. Ogni file passa prima da un nome provvisorio.
     * ⚠️ **Il nome provvisorio non comincia col punto**, a differenza di `FileTree.rename`: un
     * nome nascosto è un file che il MediaStore smette di elencare, e la seconda fase ha bisogno
     * della sua riga. Se l'app si ferma fra le due fasi, i file restano col nome
     * `aiv-<ora>-<n>`, visibili in galleria.
     * Un nome occupato da un file fuori dalla selezione ne riceve uno libero
     * (`FileTree.freeName`), e un file la cui seconda fase fallisce torna al nome vecchio.
     * I nomi in memoria si dimenticano ([Names.forget]): la riga conserva l'indirizzo anche dopo
     * la rinomina, e la griglia mostrerebbe il nome vecchio.
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

            // Fase 1: ogni file sotto un nome che non può essere di nessun altro.
            val parked = plan.filter { step ->
                val tail = if (step.file.extension.isEmpty()) "" else ".${step.file.extension}"
                named(context, step.row, "aiv-$stamp-${step.at}$tail").also { if (!it) failed++ }
            }

            // Fase 2: il nome vero. Un nome occupato adesso è di un file fuori dalla selezione.
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

    /** Dà a [row] il nome [name], e dice se il MediaStore l'ha accettato. */
    private fun named(context: Context, row: Uri, name: String): Boolean = runCatching {
        val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, name) }
        context.contentResolver.update(row, values, null, null) > 0
    }.getOrDefault(false)

    /** Un file della rinomina: dov'è, la sua riga, il posto nell'ordine e il nome che vuole. */
    private class Step(val file: File, val row: Uri, val at: Int, val wanted: String)
}

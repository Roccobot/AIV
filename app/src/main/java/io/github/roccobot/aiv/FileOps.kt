package io.github.roccobot.aiv

import android.content.res.Resources
import android.net.Uri
import androidx.annotation.PluralsRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * Le operazioni sui file e i loro dialoghi, in un posto solo.
 *
 * ⚠️⚠️ **NASCE DALLA 0.62, PERCHÉ I POSTI CHE LE CHIEDONO SONO DIVENTATI DUE**: la
 * selezione nella griglia e il tocco lungo nel visualizzatore. Fino alla `0.61` tutto questo
 * viveva dentro `GridScreen`, che era giusto finché era l'unico a chiedere; una seconda copia
 * nel visualizzatore avrebbe voluto dire due dialoghi di eliminazione da tenere d'accordo, e
 * il cestino (che arriva dopo) ne cambia il testo.
 *
 * ⚠️ **Quello che NON sta qui, e non è una dimenticanza**: che cosa fare a operazione
 * finita. La griglia svuota la selezione e si rilegge, il visualizzatore deve capire quale
 * fotografia mostrare al posto di quella che non c'è più. Sono due risposte diverse alla
 * stessa domanda, quindi le decide chi chiama ([onRun]) e non questo file.
 */
@Composable
fun FileJobDialogs(
    job: FileJob?,
    onClose: () -> Unit,
    /**
     * Fa partire l'operazione: riceve **che cosa** si sta facendo e il lavoro da svolgere.
     *
     * ⚠️ Il lavoro arriva come funzione sospesa e non eseguito: chi chiama lo lancia nel
     * **suo** ambito, che è la sola cosa che gli permette di sopravvivere al dialogo che
     * si chiude e alla schermata che cambia.
     */
    onRun: (FileKind, suspend () -> FileTree.Outcome) -> Unit
) {
    val context = LocalContext.current
    when (job) {
        null -> Unit

        is FileJob.Transfer -> DestinationDialog(
            action = if (job.move) R.string.dest_move_here else R.string.dest_here,
            onDismiss = onClose,
            onPick = { dir ->
                onClose()
                if (job.move) onRun(FileKind.MOVE) { FileTree.move(context, job.uris, dir) }
                else onRun(FileKind.COPY) { FileTree.copy(context, job.uris, dir) }
            }
        )

        is FileJob.Rename -> RenameDialog(
            uris = job.uris,
            onDismiss = onClose,
            onRename = { template, start ->
                onClose()
                onRun(FileKind.RENAME) { FileTree.rename(context, job.uris, template, start) }
            }
        )

        is FileJob.Delete -> DeleteDialog(
            count = job.uris.size,
            onDismiss = onClose,
            onConfirm = {
                onClose()
                onRun(FileKind.DELETE) { FileTree.delete(context, job.uris) }
            }
        )

        is FileJob.Facts -> FactsDialog(uris = job.uris, onDismiss = onClose)
    }
}

/**
 * Il dialogo aperto, e su quali immagini.
 *
 * ⚠️⚠️ **LE IMMAGINI SI FOTOGRAFANO QUANDO IL DIALOGO SI APRE, non quando si conferma**:
 * l'elenco viaggia dentro il lavoro invece di essere riletto dalla selezione viva. Il
 * dialogo è modale, quindi le due letture darebbero lo stesso risultato **oggi**, e domani
 * basterebbe un dialogo non modale o una selezione che si aggiorna da sola perché una
 * conferma agisse su file diversi da quelli che sono stati mostrati.
 */
sealed interface FileJob {
    val uris: List<Uri>

    /**
     * Copia o spostamento, che differiscono **solo** per la parola sul tasto e per il
     * fatto che il secondo porta via l'originale: la cartella di arrivo la si chiede allo
     * stesso modo.
     */
    class Transfer(override val uris: List<Uri>, val move: Boolean) : FileJob
    class Rename(override val uris: List<Uri>) : FileJob
    class Delete(override val uris: List<Uri>) : FileJob
    class Facts(override val uris: List<Uri>) : FileJob
}

/**
 * Le quattro operazioni che toccano i file, con quello che di ognuna serve sapere dopo.
 *
 * ⚠️⚠️ **[gone] è la ragione per cui questo elenco esiste** invece di passare in giro la
 * sola stringa dell'esito: dice se l'indirizzo di partenza vale ancora. Per il
 * visualizzatore è la differenza fra restare dov'è e dover trovare un'altra fotografia da
 * mostrare, e scritto qui è una riga, sparso fra le schermate sarebbe una condizione da
 * ricordare in ogni posto.
 * ⚠️ **La rinomina è fra quelle che 'portano via' il file**, e non è un errore: la
 * fotografia c'è ancora, ma la sua riga nel MediaStore cambia numero, quindi l'indirizzo
 * che si aveva in mano non apre più niente.
 */
enum class FileKind(@PluralsRes val done: Int, val gone: Boolean) {
    COPY(R.plurals.copy_done, gone = false),
    MOVE(R.plurals.move_done, gone = true),
    RENAME(R.plurals.rename_done, gone = true),
    DELETE(R.plurals.delete_done, gone = true)
}

/**
 * Che cosa dire quando un'operazione finisce: quante sono passate e, solo se ce ne sono,
 * quante no.
 *
 * ⚠️ Le forme plurali si risolvono con `getQuantityString` e non con
 * `pluralStringResource`, perché il numero si sa solo a lavoro finito e quella funzione si
 * può chiamare soltanto mentre si compone.
 * ⚠️ **Un avviso solo con tutti e due i numeri**: due avvisi di fila si coprono a vicenda,
 * e il secondo si leggerebbe senza il primo.
 */
fun outcomeText(res: Resources, out: FileTree.Outcome, @PluralsRes doneRes: Int): String =
    buildString {
        append(res.getQuantityString(doneRes, out.done, out.done))
        if (out.failed > 0) {
            append(", ")
            append(res.getQuantityString(R.plurals.op_failed, out.failed, out.failed))
        }
    }

/**
 * La conferma dell'eliminazione.
 *
 * ⚠️⚠️ **NON È CORTESIA: qui non c'è un cestino.** Il MediaStore ne ha uno, ma ci si
 * finisce solo passando dal provider con la richiesta apposita, e questa app cancella dal
 * disco perché è l'unica via che copre anche i file che nella galleria non ci sono mai
 * entrati. Quindi il gesto è definitivo, e il testo lo dice invece di lasciarlo intuire.
 * ⚠️ Il conto sta nel TITOLO e non nel corpo: è il dato che fa cambiare idea, e chi tocca
 * in fretta legge solo la prima riga.
 */
@Composable
private fun DeleteDialog(count: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(pluralStringResource(R.plurals.delete_ask, count, count)) },
        text = { Text(stringResource(R.string.delete_desc)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text(stringResource(R.string.pick_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Che cosa si è scelto, in numeri.
 *
 * ⚠️ **I dati si leggono quando il dialogo si apre e non prima**: contare il peso di
 * trecento file vuol dire trecento interrogazioni, e farle a ogni tocco su una miniatura
 * sarebbe pagarle per una domanda che quasi nessuno fa.
 * ⚠️ **Finché non sono pronti si dice che si sta contando**, invece di mostrare uno zero
 * che poi cambia: uno zero che si corregge da solo si legge come un errore.
 */
@Composable
private fun FactsDialog(uris: List<Uri>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val facts by produceState<Facts?>(null, uris) { value = factsOf(context, uris) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_info)) },
        text = {
            val f = facts
            Text(
                text = if (f == null) stringResource(R.string.pick_counting) else buildString {
                    /*
                     * ⚠️ **Conto e peso sulla STESSA RIGA dalla 0.59** (richiesta
                     * dell'utente: *indica come 'X file, Y MB totali'*). Erano due righe,
                     * e due numeri messi uno sotto l'altro si leggono come due fatti
                     * separati invece che come la misura di una cosa sola.
                     * ⚠️ L'unità la sceglie `formatBytes`, che sale fino ai GB: su una
                     * selezione grossa il gradino serve, ed è per quello che c'è.
                     */
                    append(
                        pluralStringResource(
                            R.plurals.pick_facts, f.count, f.count, formatBytes(f.bytes)
                        )
                    )
                    f.name?.let { append('\n').append(it) }
                    if (f.width != null && f.height != null) {
                        append('\n').append(f.width).append(" x ").append(f.height)
                    }
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pick_close)) }
        }
    )
}

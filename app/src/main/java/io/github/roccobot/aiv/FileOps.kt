package io.github.roccobot.aiv

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import androidx.annotation.PluralsRes
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

/**
 * L'ambito dei lavori sui file (copia, sposta, cestino, rinomina, eliminazione), e
 * dell'importazione e dell'esportazione degli stili.
 *
 * ⚠️⚠️ **Vive quanto il processo, non quanto la schermata**: l'ambito di una schermata si chiude
 * con una rotazione o uscendo dalla cartella, e la scrittura sul disco si salverebbe da sé
 * (`NonCancellable`), ma con lei si perderebbero 'Annulla' e l'avviso dell'esito.
 * ⚠️ **Sul thread principale**: le finestre di sistema che un lavoro aspetta ([SystemAsk]) si
 * aprono da lì, e il lavoro sul disco passa da sé su `Dispatchers.IO`.
 * `SupervisorJob`: un lavoro che va in errore non ferma gli altri in corso.
 */
object FileWork {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}

/**
 * Le operazioni sui file e i loro dialoghi, in un posto solo per tutte le schermate che le
 * chiedono (la selezione nella griglia, il tocco lungo nel visualizzatore...).
 *
 * ⚠️ **Che cosa fare a operazione finita lo decide chi chiama** ([onRun]): la griglia svuota la
 * selezione e si rilegge, il visualizzatore cerca l'immagine da mostrare al posto di quella che
 * non c'è più.
 */
@Composable
fun FileJobDialogs(
    job: FileJob?,
    /**
     * I campi delle informazioni, nell'ordine scelto e senza quelli spenti, da
     * `Settings.factRows`: li passa chi chiama, perché questo file non legge le impostazioni.
     */
    fields: List<FactField>,
    onClose: () -> Unit,
    /**
     * Fa partire l'operazione: riceve che cosa si sta facendo e il lavoro da svolgere.
     *
     * ⚠️ Il lavoro arriva come funzione sospesa e non eseguito: chi chiama lo lancia in
     * [FileWork], che lo fa sopravvivere al dialogo che si chiude, alla schermata che cambia e
     * alla rotazione.
     */
    onRun: (FileKind, suspend () -> FileTree.Outcome) -> Unit
) {
    val context = LocalContext.current
    // ⚠️ Qui e non nel ramo di un lavoro: l'eliminazione di AIV Play aspetta una finestra di
    // sistema dopo che la sua si è chiusa (`MediaDelete`, `SystemAsk`).
    val ask = rememberSystemAsk()
    when (job) {
        null -> Unit

        is FileJob.Transfer -> {
            /*
             * ⚠️ Le cartelle da cui vengono i file, lette prima che la finestra si apra (nota C
             * dell'utente): con un insieme vuoto all'inizio, l'origine risponderebbe a un tocco
             * per il tempo della domanda. Una domanda al MediaStore per file, fuori dal thread
             * principale.
             */
            val from by produceState<Set<String>?>(null, job) {
                value = withContext(Dispatchers.IO) { FileTree.sourcesOf(context, job.uris) }
            }
            from?.let { from -> DestinationDialog(
            action = if (job.move) R.string.dest_move_here else R.string.dest_here,
            from = from,
            onDismiss = onClose,
            /*
             * ⚠️ **Copia e spostamento offrono 'Annulla'** (richiesta dell'utente, punto B del
             * giro della 1.82): l'offerta è qui perché è l'unico punto dell'app da cui si copia e
             * si sposta. Si apre dopo l'operazione e con i suoi passi: quello che si può disfare
             * lo sa solo chi ha scritto i file, e non si ricostruisce qui dai nomi che `freeName`
             * ha scelto.
             */
            onPick = { dir ->
                onClose()
                if (job.move) {
                    onRun(FileKind.MOVE) {
                        val out = FileTree.move(context, job.uris, dir)
                        Undo.offer(Undo.Offer.Files(FileKind.MOVE, out.undo))
                        out
                    }
                } else {
                    onRun(FileKind.COPY) {
                        val out = FileTree.copy(context, job.uris, dir)
                        Undo.offer(Undo.Offer.Files(FileKind.COPY, out.undo))
                        out
                    }
                }
            }
        ) }
        }

        is FileJob.Rename -> RenameDialog(
            uris = job.uris,
            onDismiss = onClose,
            onRename = { template, start, extension ->
                onClose()
                onRun(FileKind.RENAME) {
                    renameFiles(context, job.uris, template, start, extension, ask)
                }
            }
        )

        /*
         * ⚠️⚠️ **La conferma c'è quando e solo quando non si torna indietro** (testo
         * dell'utente sull'interruttore 'Attiva il cestino'): chiedere per una cosa reversibile
         * insegna a confermare senza leggere. `forGood` contiene già la decisione (vedi
         * [FileJob.Delete]).
         *
         * ⚠️⚠️ **In AIV Play da Android 11 la conferma è la finestra di sistema** (scelta A1):
         * AIV non ne aggiunge una sua davanti a quella di Android, che non si salta. Un 'no' là
         * non tocca niente e non dice niente. `forGood` elimina definitivamente con
         * `createDeleteRequest`: lo chiedono la pagina del cestino di AIV Play e, fuori, il
         * tocco lungo su 'Elimina'.
         */
        is FileJob.Delete if !Store.files && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
            SystemWindow(job, job.uris, if (job.forGood) FileKind.DELETE else FileKind.TRASH, ask, onRun, onClose) {
                if (job.forGood) MediaDelete.deleteRequest(context, it) else MediaDelete.trashRequest(context, it)
            }

        // ⚠️ Il cestino di AIV Play è quello di sistema: la stessa finestra, girata al contrario.
        is FileJob.Restore if !Store.files && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
            SystemWindow(job, job.uris, FileKind.RESTORE, ask, onRun, onClose) {
                MediaDelete.restoreRequest(context, it)
            }

        // ⚠️ AIV Play su Android 9 e 10 elimina definitivamente (scelta B1), dopo la conferma di AIV.
        is FileJob.Delete if !Store.files ->
            DeleteDialog(
                count = job.uris.size,
                onDismiss = onClose,
                onConfirm = {
                    onClose()
                    onRun(FileKind.DELETE) { MediaDelete.forGood(context, job.uris, ask) }
                }
            )

        is FileJob.Delete ->
            if (job.forGood) {
                DeleteDialog(
                    count = job.uris.size,
                    onDismiss = onClose,
                    onConfirm = {
                        onClose()
                        onRun(FileKind.DELETE) { FileTree.delete(context, job.uris) }
                    }
                )
            } else {
                /*
                 * Parte da sé, come il ripristino (vedi là l'effetto legato al lavoro), e offre
                 * 'Annulla' (richiesta dell'utente): qui perché è l'unico punto dell'app in cui un
                 * file finisce nel cestino per eliminazione. Lo stato vive fuori dalle schermate:
                 * vedi la testa di [Undo].
                 */
                LaunchedEffect(job) {
                    onRun(FileKind.TRASH) {
                        val sent = Bin.send(context, job.uris)
                        Undo.offer(Undo.Offer.Trashed(sent.landed))
                        sent.outcome
                    }
                    onClose()
                }
            }

        /*
         * Parte da sé: il ripristino non ha domande da fare, e passa di qui per lo stesso imbuto
         * delle altre (l'avviso dell'esito, la rilettura, l'ambito che sopravvive).
         * ⚠️ **L'effetto è legato al lavoro e non alla composizione**: gira una volta, poi
         * `onClose` toglie il ramo di scena e con lui l'effetto, ma non il lavoro, che `onRun` ha
         * già lanciato in [FileWork].
         */
        is FileJob.Restore -> LaunchedEffect(job) {
            onRun(FileKind.RESTORE) { Bin.restore(context, job.uris) }
            onClose()
        }

        /*
         * Come il ripristino, parte da sé: la destinazione è la cartella in cui il file è già.
         */
        is FileJob.Duplicate -> LaunchedEffect(job) {
            // ⚠️ Anche il duplicato si disfa: nasce dal tocco lungo su 'Copia', il gesto più
            // facile da dare per sbaglio.
            onRun(FileKind.COPY) {
                val out = FileTree.duplicate(context, job.uris)
                Undo.offer(Undo.Offer.Files(FileKind.COPY, out.undo))
                out
            }
            onClose()
        }

        /*
         * ⚠️ **Questo ramo può diventare una rinomina**: il tocco lungo sulla pastiglia del nome
         * rinomina il file (richiesta dell'utente). La scheda delle informazioni non apre
         * finestre da sé: chiede, e cambia finestra l'imbuto, dove vivono tutte le finestre dei
         * lavori sui file. La rinomina è la stessa di sempre, con gli stessi indirizzi e lo
         * stesso `onRun`.
         * `remember(job)`: legato al lavoro, così una scheda riaperta su un altro file non nasce
         * già in rinomina.
         */
        is FileJob.Facts -> {
            var renaming by remember(job) { mutableStateOf(false) }
            if (renaming) {
                RenameDialog(
                    uris = job.uris,
                    onDismiss = onClose,
                    onRename = { template, start, extension ->
                        onClose()
                        onRun(FileKind.RENAME) {
                            renameFiles(context, job.uris, template, start, extension, ask)
                        }
                    }
                )
            } else {
                FactsDialog(
                    uris = job.uris,
                    fields = fields,
                    onDismiss = onClose,
                    onRename = { renaming = true }
                )
            }
        }
    }
}

/**
 * Il dialogo aperto, e su quali immagini.
 *
 * ⚠️⚠️ **L'elenco delle immagini si fissa quando il dialogo si apre, non quando si conferma**:
 * viaggia dentro il lavoro e non si rilegge dalla selezione viva, così un dialogo non modale o una
 * selezione che si aggiorna da sola non farebbero agire la conferma su file diversi da quelli
 * mostrati.
 */
sealed interface FileJob {
    val uris: List<Uri>

    /**
     * Copia o spostamento, che differiscono solo per la parola sul tasto e per l'originale che
     * il secondo toglie: la cartella di arrivo si chiede allo stesso modo.
     */
    class Transfer(override val uris: List<Uri>, val move: Boolean) : FileJob
    class Rename(override val uris: List<Uri>) : FileJob

    /**
     * L'eliminazione, che vuol dire due cose diverse.
     *
     * ⚠️⚠️ **[forGood] non è un'opzione ma il posto in cui si è, più l'interruttore**: dentro il
     * cestino si cancella sempre, fuori si sposta là finché 'Attiva il cestino' è acceso. Con
     * `false` nel cestino un file finirebbe nel cestino stesso, cioè da nessuna parte.
     * ⚠️⚠️ **Decide anche la conferma**, che si chiede solo quando è `true`: in un dato solo non
     * esiste la combinazione 'cancellare per sempre senza chiedere'.
     */
    class Delete(override val uris: List<Uri>, val forGood: Boolean) : FileJob

    /**
     * Il ripristino, senza dialogo: la destinazione è scritta nell'archivio del cestino, e il
     * gesto è reversibile, quindi una conferma insegnerebbe a confermare senza leggere.
     */
    class Restore(override val uris: List<Uri>) : FileJob

    /**
     * La duplicazione dove il file è già: l'altra voce senza dialogo.
     *
     * ⚠️ **Non è [Transfer] con la cartella corrente**: quella ne prende una per tutti i file, qui
     * ogni file torna nella sua, e in una ricerca possono essere venti diverse (vedi
     * `FileTree.duplicate`). Nessuna conferma: aggiunge un file e non ne tocca nessuno.
     */
    class Duplicate(override val uris: List<Uri>) : FileJob
    class Facts(override val uris: List<Uri>) : FileJob
}

/**
 * Come un lavoro sopravvive a una rotazione, cioè alla ricreazione dell'attività: lo stato passa
 * dal `Bundle` del sistema e la finestra si riapre dov'era, col testo che si stava scrivendo.
 * Gli indirizzi si salvano da sé, perché un `Uri` è `Parcelable`.
 *
 * ⚠️⚠️ **I lavori che partono da sé non si ripristinano, perché ripristinarli li farebbe
 * rifare**: ripristino, duplicazione ed eliminazione col cestino attivo partono da un
 * `LaunchedEffect` sulla chiave del lavoro, quindi rimessi in scena ripartirebbero una seconda
 * volta. Si salvano come 'niente': l'operazione è già in corso in [FileWork], che alla rotazione
 * resta. Si salvano le sole finestre: la destinazione di 'Copia' e 'Sposta', la rinomina, la
 * conferma di un'eliminazione definitiva e la scheda delle informazioni.
 * ⚠️⚠️ **Oltre [SAVED_URIS] indirizzi non si salva niente**: lo stato salvato passa da una
 * transazione del sistema con un tetto di 1 MB, e una selezione di qualche migliaio di file lo
 * supera quando l'app va in secondo piano, che basta a chiuderla. Meglio una finestra che dopo la
 * rotazione non si riapre.
 */
internal val FileJobSaver: Saver<FileJob?, Any> = listSaver(
    save = { job ->
        if (job != null && job.uris.size > SAVED_URIS) return@listSaver emptyList()
        when (job) {
            null -> emptyList()
            is FileJob.Transfer -> listOf(TRANSFER, job.move) + job.uris
            is FileJob.Rename -> listOf(RENAME, false) + job.uris
            // ⚠️ Solo la conferma di AIV è una finestra da riaprire: in AIV Play da Android 11 la
            // finestra è quella di sistema, e riaprire il lavoro chiederebbe una seconda volta.
            is FileJob.Delete ->
                if (if (Store.files) job.forGood else Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    listOf(DELETE, true) + job.uris
                } else {
                    emptyList()
                }
            is FileJob.Facts -> listOf(FACTS, false) + job.uris
            is FileJob.Restore, is FileJob.Duplicate -> emptyList()
        }
    },
    restore = { salvato ->
        val quale = salvato.getOrNull(0) as? String
        val flag = salvato.getOrNull(1) as? Boolean ?: false
        val uris = salvato.drop(2).filterIsInstance<Uri>()
        when (quale) {
            TRANSFER -> FileJob.Transfer(uris, move = flag)
            RENAME -> FileJob.Rename(uris)
            DELETE -> FileJob.Delete(uris, forGood = true)
            FACTS -> FileJob.Facts(uris)
            else -> null
        }
    }
)

/**
 * Come un insieme di indirizzi sopravvive a una rotazione.
 *
 * Vive accanto a [FileJobSaver], perché è la stessa idea. Un `Set` il `Bundle` non lo scrive, una
 * lista di `Uri` sì: si passa da una lista, e l'ordine non conta. Oltre [SAVED_URIS] indirizzi la
 * selezione non si salva, per la ragione scritta su [FileJobSaver].
 */
internal val UriSetSaver: Saver<Set<Uri>, Any> = listSaver(
    save = { if (it.size > SAVED_URIS) emptyList() else it.toList() },
    restore = { it.toSet() }
)

/**
 * Quanti indirizzi al massimo entrano nello stato salvato. Un indirizzo del MediaStore occupa
 * circa 160 byte nel `Parcel`, misurato in `StatoSalvatoTest` (diecimila senza tetto pesano
 * 1.596.004 byte), quindi 500 restano sotto i 100 KB anche con una finestra aperta che li salva
 * una seconda volta.
 */
internal const val SAVED_URIS = 500

/*
 * ⚠️ **Etichette scritte a mano e non il nome della classe**: un nome di classe cambia con una
 * rinomina del codice, e quello che si scrive qui finisce in un `Bundle` che il sistema può
 * restituire dopo un aggiornamento dell'app. Lo stesso criterio dei token delle impostazioni.
 */
private const val TRANSFER = "transfer"
private const val RENAME = "rename"
private const val DELETE = "delete"
private const val FACTS = "facts"

/**
 * Le operazioni che toccano i file, con quello che di ognuna serve sapere dopo.
 *
 * ⚠️⚠️ **[gone] dice se l'indirizzo di partenza vale ancora**: per il visualizzatore è la
 * differenza fra restare dov'è e cercare un'altra immagine da mostrare, e scritto qui è una
 * riga sola.
 * ⚠️ **La rinomina toglie il file**: l'immagine c'è ancora, ma la sua riga nel MediaStore cambia
 * numero, e l'indirizzo che si aveva non apre più niente.
 */
enum class FileKind(@PluralsRes val done: Int, val gone: Boolean) {
    COPY(R.plurals.copy_done, gone = false),
    MOVE(R.plurals.move_done, gone = true),
    RENAME(R.plurals.rename_done, gone = true),

    /**
     * L'eliminazione di tutti i giorni: il file va nel cestino. `gone = true` come le altre,
     * perché per la schermata il file non è più là; che sia recuperabile riguarda il cestino.
     */
    TRASH(R.plurals.trash_done, gone = true),

    /**
     * L'eliminazione definitiva: dentro il cestino, o svuotandolo.
     *
     * ⚠️ **Svuotando, la sua frase dice anche lo spazio liberato**, e a sceglierla è il dato e non
     * un tipo in più: vedi [outcomeText].
     */
    DELETE(R.plurals.delete_done, gone = true),

    /** Il ritorno alla cartella d'origine, da dentro il cestino. */
    RESTORE(R.plurals.restore_done, gone = true)
}

/**
 * La rinomina che chiedono le due finestre ([RenameDialog], diretta o dal nome in [FactsDialog]):
 * AIV Play rinomina col MediaStore da Android 11 ([MediaRename]), ogni altro caso sul disco. Una
 * funzione sola, così le due strade non divergono.
 */
private suspend fun renameFiles(
    context: Context,
    uris: List<Uri>,
    template: String,
    start: Int,
    extension: String?,
    ask: SystemAsk
): FileTree.Outcome =
    if (!Store.files && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        MediaRename.rename(context, uris, template, start, ask)
    } else {
        FileTree.rename(context, uris, template, start, extension)
    }

/**
 * Se l'esito di un'operazione va detto con un avviso di sistema.
 *
 * ⚠️⚠️ **Un'operazione che offre 'Annulla' non parla**: la notizia la dà la notifica con
 * 'Annulla' (vedi [Undo]), e un avviso di sistema le comparirebbe sopra, in fondo allo schermo.
 * La regola guarda se c'è un'offerta (`out.undo`) e non i tipi, così vale anche per
 * un'operazione che diventi reversibile domani. Il cestino di AIV GitHub tace anche senza
 * offerta; quello di AIV Play, che è di sistema e non offre 'Annulla', parla.
 * ⚠️ **Se qualcosa è fallito parla lo stesso**: la notifica dice quante sono passate, non quante
 * no. Niente fatto e niente fallito non dice niente: è un 'no' nella finestra di Android, che la
 * rinomina di AIV Play chiede da dentro il lavoro (`MediaRename`).
 * Una regola sola, qui, e non tre condizioni nelle tre schermate che eseguono.
 */
fun FileKind.speaks(out: FileTree.Outcome): Boolean =
    out.failed > 0 || (out.done > 0 && out.undo.isEmpty() && (this != FileKind.TRASH || !Store.files))

/**
 * Che cosa dire quando un'operazione finisce: quante sono passate e, solo se ce ne sono,
 * quante no.
 *
 * Le forme plurali si risolvono con `getQuantityString`, perché il numero si sa solo a lavoro
 * finito e `pluralStringResource` si chiama soltanto mentre si compone. Un avviso solo con tutti
 * e due i numeri: due di fila si coprirebbero.
 *
 * ⚠️ **Un'eliminazione definitiva che ha misurato lo spazio lo dice** (richiesta dell'utente):
 * oggi lo misura il solo svuotamento del cestino (`Bin.empty`). La frase è una chiave a sé e non
 * un pezzo in coda, perché in molte lingue verbo e accordo cambiano con quello che segue. Lo
 * spazio si scrive con [formatBytes], come il peso di una selezione (`pick_facts`), così chi lo
 * guarda prima di svuotare ritrova lo stesso numero dopo. Si chiede il tipo perché lo spazio
 * liberato ha senso detto di un'eliminazione.
 */
fun outcomeText(res: Resources, out: FileTree.Outcome, kind: FileKind): String =
    buildString {
        append(
            if (kind == FileKind.DELETE && out.freed > 0) {
                res.getQuantityString(R.plurals.delete_freed, out.done, out.done, formatBytes(out.freed))
            } else {
                res.getQuantityString(kind.done, out.done, out.done)
            }
        )
        if (out.failed > 0) {
            append(", ")
            append(res.getQuantityString(R.plurals.op_failed, out.failed, out.failed))
        }
    }

/**
 * Un lavoro di AIV Play che conferma la finestra di Android: apre [request] sulle righe del
 * MediaStore di [uris] e, dopo un sì, conta ogni riga come fatta sotto [kind].
 *
 * ⚠️ **Un 'no' non tocca niente e non dice niente**; i file senza riga, o un sistema che non dà
 * la finestra, contano come non riusciti, e si dice. Lo condividono lo spostamento nel cestino, il
 * ripristino e l'eliminazione definitiva della pagina del cestino, così non divergono.
 */
@Composable
private fun SystemWindow(
    job: FileJob,
    uris: List<Uri>,
    kind: FileKind,
    ask: SystemAsk,
    onRun: (FileKind, suspend () -> FileTree.Outcome) -> Unit,
    onClose: () -> Unit,
    request: (List<Uri>) -> android.content.IntentSender
) {
    val context = LocalContext.current
    LaunchedEffect(job) {
        val rows = withContext(Dispatchers.IO) { MediaDelete.rows(context, uris) }
        val window = if (rows.isEmpty()) null else runCatching { request(rows) }.getOrNull()
        val yes = window != null && ask.ask(window)
        val failed = if (window == null) uris.size else uris.size - rows.size
        if (yes || failed > 0) onRun(kind) { FileTree.Outcome(if (yes) rows.size else 0, failed) }
        onClose()
    }
}

/**
 * La conferma dell'eliminazione definitiva: si vede soltanto quando il file va via davvero, cioè
 * dentro il cestino o col cestino spento. Rimettere la conferma sul cestino andrebbe contro il
 * testo dell'interruttore 'Attiva il cestino'.
 *
 * Il titolo dice il gesto, il corpo quanto e che cosa succede dopo (testo dell'utente): per
 * questo `delete_ask` non è un plurale e `delete_desc` sì, e rimettere il conto nel titolo
 * vorrebbe rifare le stringhe in tutte le lingue.
 */
@Composable
private fun DeleteDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(onDismiss),
        properties = loweredWindow(onDismiss),
        title = { Text(stringResource(R.string.delete_ask)) },
        text = {
            // ⚠️ Lo scorrimento serve al tetto d'altezza dei dialoghi, come nei due gemelli (il
            // perché vive nel pannellino dell'estensione, in `RenameDialog.kt`). Un `Column` con
            // dentro un `Text`: il plurale in qualche lingua è lungo, e lo scorrimento vuole un
            // contenitore che possa essere più alto del suo riquadro.
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(pluralStringResource(R.plurals.delete_desc, count, count))
            }
        },
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
 * Che cosa si è scelto: i numeri di una selezione, o i dati di un file solo.
 *
 * È una scheda appoggiata in basso, larga quanto lo schermo, con la pastiglia del nome ferma in
 * fondo (richiesta dell'utente, giro della 1.37), e arriva dal basso con una mini dissolvenza
 * (istruzione dell'utente, 1.43).
 * ⚠️⚠️ **Sale dal basso anche se 'senza scorrere dal basso' era stato chiesto due volte**: l'utente
 * l'ha visto in mano e ha cambiato idea, perché una scheda appoggiata al bordo di sotto che non
 * viene da lì contraddice il posto in cui è. Chi rilegge solo le richieste di allora rifà il
 * divieto. Il movimento, coi tempi e la curva, vive in `Sheet.kt`.
 *
 * I dati si leggono quando si apre: il peso di trecento file vuol dire trecento domande.
 * ⚠️ **Su un file solo non si apre finché non ha i dati**: aprendosi prima con 'Sto contando...'
 * e poi coi dati, la crescita si vedrebbe; con un file è una domanda sola. Su molti file si apre
 * subito, perché l'attesa è vera e una scheda muta per mezzo secondo sembra un tocco a vuoto.
 */
@Composable
private fun FactsDialog(
    uris: List<Uri>,
    fields: List<FactField>,
    onDismiss: () -> Unit,
    onRename: () -> Unit
) {
    val context = LocalContext.current
    val facts by produceState<Facts?>(null, uris) { value = factsOf(context, uris) }
    if (facts == null && uris.size == 1) return

    Sheet(onDismiss = onDismiss, title = stringResource(R.string.facts_title), foot = {
        // ⚠️ La pastiglia c'è solo con un file: con una selezione non c'è un nome, e un piede
        // vuoto lascerebbe una striscia vuota sopra il bordo. È anche quello che rende sicura la
        // rinomina col tocco lungo: `one` non è nullo solo quando il file è uno.
        facts?.one?.name?.let { NamePill(name = it, onRename = onRename) }
    }) {
        val f = facts
        when {
            f == null -> Text(
                text = stringResource(R.string.pick_counting),
                style = MaterialTheme.typography.bodyMedium
            )
            /*
             * Il peso davanti e il conto dietro ('X MB in Y file', richiesta dell'utente): quante
             * immagini si sono scelte lo dice già la barra sopra. È un plurale perché con un file
             * solo la forma cambia in diverse lingue.
             */
            f.one == null -> Text(
                text = pluralStringResource(
                    R.plurals.pick_facts, f.count, f.count, formatBytes(f.bytes)
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            else -> FileFacts(facts = f, one = f.one, fields = fields)
        }
    }
}

/**
 * I dati di un file, una riga per campo.
 *
 * ⚠️⚠️ **Un campo senza dato non prende nessuna riga** (richiesta dell'utente): il `continue`
 * qui sotto è tutta la regola. Niente righe vuote né 'n.d.', quindi la scheda di un'immagine
 * scaricata dal web è corta e quella di uno scatto col telefono è lunga.
 * Il nome non è una riga: è nella pastiglia in fondo alla scheda ([NamePill], vedi [FactsDialog]).
 * ⚠️⚠️ **Questo blocco non scorre da sé**: lo scorrimento è della scheda che lo contiene (vedi
 * [Sheet]), e due scorrimenti verticali annidati non sono un difetto estetico ma un crash: quello
 * di fuori misura il contenuto con altezza infinita, e `checkScrollableContainerConstraints` va
 * in errore. Un `verticalScroll` qui rimetterebbe il crash.
 */
@Composable
internal fun FileFacts(facts: Facts, one: OneFile, fields: List<FactField>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (field in fields) {
            // ⚠️ Il nome non è una riga di questo elenco: è nella pastiglia in fondo alla scheda.
            if (field == FactField.NAME) continue
            val value = factValue(field, facts, one) ?: continue
            FactRow(label = stringResource(field.label), value = value)
            /*
             * ⚠️ **L'altitudine è una riga a sé ma non un campo a sé** (richiesta dell'utente):
             * un `FactField` suo sarebbe una spunta in più nelle impostazioni, che l'utente ha
             * chiesto di alleggerire. Viene dallo stesso dato GPS delle coordinate, e chi spegne
             * le coordinate spegne anche lei.
             */
            if (field == FactField.PLACE) {
                altitudeText(one)?.let {
                    FactRow(label = stringResource(R.string.facts_altitude_label), value = it)
                }
            }
        }
    }
}

/**
 * Il nome del file in una pastiglia del colore d'accento, larga quanto la scheda e alta quanto
 * serve fino a [NAME_LINES] righe.
 *
 * `primaryContainer` e non `primary`, anche se in questa tavolozza valgono lo stesso: è il ruolo
 * giusto per una superficie colorata.
 *
 * ⚠️⚠️ **Fino a tre righe, poi accorcia con l'ellissi, e l'estensione non si spezza mai** (regole
 * dell'utente): l'ellissi in mezzo e l'estensione salva le fa [fitName], perché
 * `TextOverflow.Ellipsis` toglierebbe proprio l'estensione.
 * L'estensione è in grassetto e il resto del nome al peso di `titleSmall`: il grassetto si vede
 * solo accanto a qualcosa che non lo è. Vale sempre, non solo quando il nome si accorcia.
 *
 * ⚠️⚠️ **Il tocco copia il nome intero, non quello che si vede**: a schermo il nome può essere
 * accorciato, e negli appunti va [name]. Tocco breve copia, tocco lungo rinomina (richieste
 * dell'utente): una pastiglia sembra un tasto, quindi il gesto ovvio è il tocco. La vibrazione,
 * [HOLD_BUZZ], è la stessa per i due gesti, ed è il solo segno immediato che il gesto è passato.
 * La rinomina la apre chi chiama ([onRename]), perché è un lavoro sui file e passa da
 * `FileJobDialogs`.
 * [detectTapGestures] e non `combinedClickable`, che metterebbe l'onda del tocco su una superficie
 * che fa da titolo, e un titolo che si illumina si legge come una voce di elenco.
 */
@Composable
internal fun NamePill(name: String, onRename: () -> Unit, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.titleSmall
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val said = stringResource(R.string.toast_name_copied)
    val copia = {
        haptics.performHapticFeedback(HOLD_BUZZ)
        ImageActions.copyName(context, name)
        Notices.say(said)
    }
    val rinomina = {
        haptics.performHapticFeedback(HOLD_BUZZ)
        onRename()
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(name) {
                // ⚠️ AIV Play rinomina solo da Android 11 (`Store.renames`): sotto, il tocco lungo
                // non c'è, e il tocco copia comunque il nome.
                detectTapGestures(
                    onTap = { copia() },
                    onLongPress = if (Store.renames) { _ -> rinomina() } else null
                )
            },
        shape = RoundedCornerShape(NAME_CORNER),
        color = MaterialTheme.colorScheme.primaryContainer,
        /*
         * ⚠️⚠️ **Sul tema chiaro il testo ha il colore dello sfondo della scheda** (richiesta
         * dell'utente), come ritagliato nella pastiglia. Il prezzo misurato: il contrasto scende
         * da 5,19 a 2,23, sotto il 4,5 di un testo di questa misura. Per avere tutte e due le
         * cose si scurisce l'accento: con `#2C7667` lo stesso testo torna a 4,78.
         * Solo sul chiaro: sullo scuro il bianco resta (5,69), perché la richiesta nomina un tema
         * solo.
         */
        contentColor = if (LocalAivLight.current) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        // ⚠️ `BoxWithConstraints` e non una misura in dp: la larghezza utile dipende dalla
        // scheda, cioè dallo schermo.
        BoxWithConstraints(
            modifier = Modifier.padding(horizontal = NAME_PAD_SIDE, vertical = NAME_PAD_TOP)
        ) {
            val measurer = rememberTextMeasurer()
            val room = with(LocalDensity.current) { maxWidth.roundToPx() }
            val shown = remember(name, room, style, measurer) {
                fitName(name, room, NAME_LINES, style, measurer)
            }
            Text(
                text = shown.text,
                // ⚠️ Lo stile a cui il nome è stato misurato e non quello pieno: `fitName` può
                // stringere corpo o spaziatura, e la misura piena rimetterebbe lo sforo.
                style = shown.style,
                maxLines = NAME_LINES,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Una riga di dati: etichetta a sinistra, valore a destra.
 *
 * ⚠️ **`alignByBaseline` su tutti e due**: etichetta `bodySmall` e valore `bodyMedium` hanno
 * corpi e interlinee diversi, e allineati in alto le lettere sarebbero sfalsate (riscontro
 * dell'utente). Su un valore di due righe si allinea la prima, quella accanto all'etichetta.
 */
@Composable
private fun FactRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(LABEL_SHARE).alignByBaseline()
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f - LABEL_SHARE).alignByBaseline()
        )
    }
}

/** Quanta parte della riga tiene l'etichetta: meno della metà, perché il dato conta più di lei. */
private const val LABEL_SHARE = 0.42f

/** Il raggio della pastiglia del nome: abbastanza da leggersi come una targhetta. */
private val NAME_CORNER = 8.dp

/** Quanto respira il nome dentro la sua pastiglia, ai lati e sopra e sotto. */
private val NAME_PAD_SIDE = 10.dp
private val NAME_PAD_TOP = 4.dp

/**
 * Quante righe può prendere il nome nella pastiglia.
 *
 * Tre, scelta dell'utente: alla larghezza della scheda su un telefono sono circa novanta
 * caratteri, più del doppio del nome più lungo che una fotocamera produce.
 */
private const val NAME_LINES = 3

/**
 * Il valore di un campo, già scritto come si legge, oppure `null` se il file non ce l'ha.
 *
 * ⚠️ **La formattazione vive qui e non nella lettura dei dati**: dentro la composizione si
 * leggono le stringhe tradotte e la lingua del telefono, che cambia date e separatori decimali.
 */
@Composable
private fun factValue(field: FactField, facts: Facts, one: OneFile): String? = when (field) {
    FactField.NAME -> one.name

    // ⚠️ Il nome del formato lo calcola [kindOf], che serve anche alla barra delle informazioni:
    // il perché del taglio del tipo MIME vive là.
    FactField.KIND -> kindOf(one.mime)

    FactField.PIXELS -> pixelsText(one)
    FactField.SIZE -> formatBytes(facts.bytes)
    FactField.TAKEN -> one.taken?.let { moment(it) }
    FactField.MODIFIED -> one.modified?.let { moment(it) }
    FactField.FOLDER -> one.folder
    FactField.ENCODING -> encodingText(one.encoding)
    FactField.COLOURS -> coloursText(one.colours)
    FactField.FRAMES -> one.motion?.frames?.toString()
    FactField.DURATION -> one.motion?.let { durationText(it.durationMs) }
    FactField.CAMERA -> cameraText(one)
    FactField.PLACE -> placeText(one)
}

/**
 * Le misure, coi megapixel quando ce ne sono abbastanza per dire qualcosa.
 *
 * Sotto un decimo di megapixel il numero non si scrive: su un'icona da 48x48 direbbe '0,0 Mpx'.
 */
@Composable
private fun pixelsText(one: OneFile): String? {
    val w = one.width ?: return null
    val h = one.height ?: return null
    val mpx = w.toLong() * h / 1_000_000.0
    return if (mpx >= 0.1) stringResource(R.string.facts_pixels_mpx, w, h, neat(mpx))
    else stringResource(R.string.facts_pixels_value, w, h)
}

/**
 * La codifica, dai byte del file. Vedi [Encoding], che spiega perché non dall'EXIF.
 *
 * La parola dell'ordine di scansione dipende dal formato: un JPEG è 'progressivo', un PNG
 * 'interlacciato', la stessa idea con i due nomi che usa chi conosce il formato.
 */
@Composable
private fun encodingText(encoding: Encoding?): String? {
    if (encoding == null) return null
    val parts = ArrayList<String>(4)
    parts += encoding.codec
    encoding.bits?.let { parts += pluralStringResource(R.plurals.facts_bits, it, it) }
    encoding.chroma?.let { parts += it }
    if (encoding.interlaced) {
        parts += stringResource(
            if (encoding.codec == "PNG") R.string.facts_interlaced
            else R.string.facts_progressive
        )
    }
    return parts.joinToString(", ")
}

/**
 * Il metodo colore, su DUE righe.
 *
 * ⚠️ **Le due righe sono una richiesta dell'utente**, coi suoi esempi (`RGBA 32 bit` a capo
 * `8 bit/canale`): sopra che cosa contiene un pixel, sotto quanto. La trasparenza entra nella
 * seconda riga, perché è una proprietà del metodo colore.
 * ⚠️ **Il nome della tavolozza è 'Colore indicizzato'**, una correzione dichiarata al suo esempio
 * ('Scala di colore'): è il termine italiano di Photoshop, come `Metodo colore` e `Scala di
 * grigio`.
 * Su RGBA la trasparenza non si ripete, perché la `A` del nome la dice già (per il grigio con
 * alfa vedi la nota nel corpo).
 */
@Composable
private fun coloursText(colours: Colours?): String? {
    if (colours == null) return null
    val name = stringResource(
        when (colours.model) {
            Colours.Model.GREY, Colours.Model.GREY_ALPHA -> R.string.facts_colour_grey
            Colours.Model.INDEXED -> R.string.facts_colour_indexed
            Colours.Model.RGB -> R.string.facts_colour_rgb
            Colours.Model.RGBA -> R.string.facts_colour_rgba
        }
    )
    val first = colours.bitsPerPixel?.let { stringResource(R.string.facts_colour_bits, name, it) }
        ?: name
    val second = ArrayList<String>(2)
    if (colours.model == Colours.Model.INDEXED) {
        colours.palette?.let {
            second += pluralStringResource(R.plurals.facts_colour_count, it, it)
        }
    } else {
        colours.bitsPerChannel?.let {
            second += stringResource(R.string.facts_colour_channel, it)
        }
    }
    // ⚠️ Solo RGBA la sottintende: il grigio con alfa si chiama 'scala di grigio' come quello
    // senza, e là la trasparenza va detta, o due file diversi mostrerebbero la stessa riga.
    if (colours.transparent && colours.model != Colours.Model.RGBA) {
        second += stringResource(R.string.facts_colour_alpha)
    }
    if (second.isEmpty()) return first
    val tail = second.joinToString(", ")
    return if (colours.model == Colours.Model.INDEXED) "$first\n($tail)" else "$first\n$tail"
}

/**
 * Quanto dura un'animazione.
 *
 * ⚠️ **Sotto il minuto i secondi con un decimale, e non `0:01`**: una GIF dura quasi sempre un
 * paio di secondi, e la forma dell'orologio perderebbe la cifra che distingue un'animazione
 * svelta da una lenta. Sopra il minuto [Videos.stamp], la forma delle miniature dei video.
 */
@Composable
private fun durationText(ms: Int): String? {
    if (ms <= 0) return null
    if (ms >= 60_000) return Videos.stamp(ms.toLong())
    return stringResource(R.string.facts_duration_s, neat(ms / 1000.0))
}

/**
 * Obiettivo ed esposizione, i tre dati che l'utente ha chiesto insieme.
 *
 * Una voce sola: focale, tempo e ISO sono i parametri di uno scatto, e si guardano insieme. Gli
 * ISO vanno a capo (richiesta dell'utente): focale e tempo dicono la luce che entra, gli ISO
 * quanto la si amplifica dopo.
 * Il tempo sotto il secondo si scrive come una frazione (`1/120 s`), come lo scrivono le
 * fotocamere e chi fotografa.
 */
@Composable
private fun cameraText(one: OneFile): String? {
    val lens = ArrayList<String>(2)
    one.focalMm?.let { lens += stringResource(R.string.facts_focal, neat(it)) }
    one.exposureSec?.let { seconds ->
        lens += if (seconds < 1.0) {
            stringResource(R.string.facts_exposure_fraction, (1.0 / seconds).roundToInt())
        } else {
            stringResource(R.string.facts_exposure_seconds, neat(seconds))
        }
    }
    val iso = one.iso?.let { stringResource(R.string.facts_iso, it) }
    val head = lens.joinToString(", ")
    // ⚠️ L'a capo esiste solo quando ci sono tutte e due le metà, o resterebbe una riga vuota.
    return when {
        head.isEmpty() -> iso
        iso == null -> head
        else -> "$head\n$iso"
    }
}

/**
 * Dove è stata scattata: le sole coordinate (l'altitudine è una riga sua, [altitudeText]).
 *
 * ⚠️⚠️ **Le coordinate si scrivono col punto, sempre, anche in italiano**, l'unica deroga alla
 * lingua del telefono in questa scheda: sono un numero da incollare in una mappa, e la virgola è
 * già il separatore fra i due valori.
 * Sei decimali: circa undici centimetri all'equatore, più della precisione di qualunque GPS di
 * telefono. Con quattro si perderebbe la casa giusta.
 */
@Composable
private fun placeText(one: OneFile): String? {
    val lat = one.latitude ?: return null
    val lon = one.longitude ?: return null
    return String.format(Locale.US, "%.6f, %.6f", lat, lon)
}

/**
 * L'altitudine, nella forma che l'utente ha chiesto: `X m s.l.m.`
 *
 * Si mostra solo insieme alle coordinate, e la condizione è in chi la chiama. Arrotondata al
 * metro: il GPS di un telefono sbaglia l'altitudine di parecchi metri.
 */
@Composable
private fun altitudeText(one: OneFile): String? {
    val high = one.altitudeM ?: return null
    return stringResource(R.string.facts_altitude, high.roundToInt())
}

/**
 * Un numero con un decimale, e senza lo zero inutile.
 *
 * `30,0 s` e `26,0 mm` dichiarerebbero una precisione che non c'è: un decimale zero si toglie.
 */
private fun neat(value: Double): String =
    if (value == floor(value) && abs(value) < 1e9) value.toLong().toString()
    else String.format(Locale.getDefault(), "%.1f", value)

/**
 * Un istante come lo scrive la lingua del telefono.
 *
 * Data media e ora breve: la data per esteso accanto a un'etichetta va a capo, e i secondi non
 * servono. ⚠️ **Nessun formato scritto a mano** (`dd/MM/yyyy`): il giorno prima del mese vale in
 * italiano e non in inglese.
 * È `internal` perché la usa anche la cronologia dei ripristini, per il capo di ogni gruppo: un
 * formato solo per le due schermate.
 */
internal fun moment(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

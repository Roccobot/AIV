package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.text.DateFormat
import java.util.Date

/**
 * Il lavoro di esportazione e importazione del file di impostazioni: quello che sta facendo, e le
 * domande che deve fare.
 *
 * ⚠️ **Vive col processo e non con la pagina**: col cestino il file pesa quanto le sue immagini e il
 * lavoro può durare minuti, e una pagina chiusa o una rotazione non devono fermarlo. Rientrando si
 * ritrova il lavoro dov'è, e l'avviso finale arriva dovunque ci si trovi.
 * Le domande sono qui per la stessa ragione: la chiave ricavata dalla password non può viaggiare in
 * un `Bundle`, che Android scrive su disco.
 *
 * ⚠️⚠️ **La password non va mai su disco**: è in [plan] per il tempo del selettore di sistema, e il
 * campo in cui la si scrive è un `remember`, non un `rememberSaveable`. Ruotando il telefono a
 * finestra aperta, la password va riscritta.
 */
internal object Backups {

    /** Che cosa la pagina deve mostrare oltre a sé stessa. */
    sealed interface Step {
        /** Il file è protetto: serve la sua password. [wrong] dice che quella di prima non andava. */
        class Locked(
            val uri: Uri,
            val head: Backup.Head,
            val groups: Set<BackupGroup>,
            val wrong: Boolean
        ) : Step

        /** Il file è aperto: si conferma che cosa rimettere. */
        class Ready(
            val uri: Uri,
            val opened: Backup.Opened,
            val manifest: Backup.Manifest,
            val groups: Set<BackupGroup>
        ) : Step

        /** Un lavoro in corso, e a che punto è: `null` quando non si può dire. */
        class Busy(val export: Boolean, val done: Float?) : Step
    }

    var step: Step? by mutableStateOf(null)
        private set

    /**
     * Quante importazioni sono andate in porto.
     *
     * Serve al modello, che ricarica quello che non passa dal flusso delle preferenze: il logo, le
     * tinte e le copertine, che un'importazione cambia per un'altra strada.
     */
    val imported = MutableStateFlow(0)

    /** Quello che un'esportazione deve fare, fra la finestra della password e il selettore. */
    private class Plan(val groups: Set<BackupGroup>, val password: String?)

    private var plan: Plan? = null
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Fissa le parti e la password dell'esportazione, prima di aprire il selettore. */
    fun prepare(groups: Set<BackupGroup>, password: String?) {
        plan = Plan(groups, password)
    }

    /** Il selettore non ha dato una destinazione: il piano non serve più. */
    fun drop() {
        plan = null
    }

    /** Chiude la domanda in scena, cioè la password da scrivere o la conferma. */
    fun forget() {
        if (step !is Step.Busy) step = null
    }

    /** Ferma il lavoro in corso. Un'importazione che sta già scrivendo va fino in fondo. */
    fun cancel() {
        job?.cancel()
    }

    /**
     * Scrive il file di impostazioni nel documento che il selettore ha appena creato.
     *
     * ⚠️ **Se non riesce, il documento si cancella**: il selettore l'ha già creato, e un file a metà
     * sembrerebbe valido finché non lo si importa.
     * Un piano che manca vuol dire un processo ripartito a selettore aperto, cioè una password persa:
     * si cancella il documento, senza scrivere un file privo della protezione chiesta.
     * ⚠️ **Si controlla che il file non sia vuoto** (voce `backup-esporta` dell'utente, 2.95): un
     * fornitore può accettare i byte senza scriverli e senza errori. Il controllo è [landed], e un
     * file vuoto si cancella come un'esportazione fallita. Il testo dell'avviso vale anche per una
     * `SecurityException`: la cartella scelta non lascia scrivere.
     */
    fun export(context: Context, uri: Uri?) {
        val piano = plan
        plan = null
        if (uri == null) return
        val app = context.applicationContext
        if (piano == null || job?.isActive == true) {
            scope.launch {
                forgetDocument(app, uri)
                say(app, R.string.toast_save_failed)
            }
            return
        }
        job = scope.launch {
            step = Step.Busy(export = true, done = 0f)
            val esito = runCatching {
                val aree = BackupGroup.areasOf(piano.groups)
                val avanza = progress(export = true, total = Backup.estimate(app, aree))
                val out = app.contentResolver.openOutputStream(uri, "w")
                    ?: throw IOException("destinazione")
                out.use {
                    Backup.write(app, it, aree, piano.password, onBytes = avanza, groups = piano.groups)
                }
                if (!landed(app, uri)) throw Empty()
            }
            withContext(NonCancellable) {
                val errore = esito.exceptionOrNull()
                if (errore != null) forgetDocument(app, uri)
                step = null
                // Un annullamento l'ha chiesto chi guarda: nessun avviso.
                if (errore !is CancellationException) {
                    say(
                        app,
                        when (errore) {
                            null -> R.string.backup_saved
                            is Empty, is SecurityException -> R.string.backup_empty
                            else -> R.string.toast_save_failed
                        }
                    )
                }
            }
        }
    }

    /**
     * Apre il file scelto per importarlo: legge l'intestazione, e chiede la password o conferma.
     *
     * @param groups le caselle spuntate al momento del tocco: si importano quelle, e solo se il file
     *   le contiene.
     */
    fun inspect(context: Context, uri: Uri?, groups: Set<BackupGroup>) {
        if (uri == null || job?.isActive == true) return
        val app = context.applicationContext
        job = scope.launch {
            step = Step.Busy(export = false, done = null)
            val esito = runCatching {
                val head = read(app, uri) { Backup.head(it) }
                if (head.locked) Step.Locked(uri, head, groups, wrong = false)
                else ready(app, uri, Backup.open(head, null), groups)
            }
            land(app, esito)
        }
    }

    /**
     * Prova la password del file in scena.
     *
     * Una password sbagliata riapre la stessa domanda col suo avviso, senza ripartire dal selettore.
     * È il passo lento: il calcolo della chiave non si ferma a metà, ma dopo un annullamento il suo
     * risultato non arriva a nessuno.
     */
    fun unlock(context: Context, password: String) {
        val chiuso = step as? Step.Locked ?: return
        if (job?.isActive == true) return
        val app = context.applicationContext
        job = scope.launch {
            step = Step.Busy(export = false, done = null)
            val esito = runCatching {
                val opened = Backup.open(chiuso.head, password)
                currentCoroutineContext().ensureActive()
                ready(app, chiuso.uri, opened, chiuso.groups)
            }
            val errore = esito.exceptionOrNull()
            if (errore is Backup.Failure && errore.reason == Backup.Reason.WRONG) {
                step = Step.Locked(chiuso.uri, chiuso.head, chiuso.groups, wrong = true)
            } else {
                land(app, esito)
            }
        }
    }

    /** Rimette le parti confermate. */
    fun restore(context: Context) {
        val pronto = step as? Step.Ready ?: return
        if (job?.isActive == true) return
        val app = context.applicationContext
        job = scope.launch {
            step = Step.Busy(export = false, done = 0f)
            val esito = runCatching {
                val avanza = progress(export = false, total = sizeOf(app, pronto.uri))
                read(app, pronto.uri) {
                    Backup.restore(
                        app, it, pronto.opened, BackupGroup.areasOf(pronto.groups),
                        onBytes = avanza, groups = pronto.groups
                    )
                }
            }
            withContext(NonCancellable) {
                step = null
                val errore = esito.exceptionOrNull()
                val out = esito.getOrNull()
                when {
                    out != null -> {
                        imported.value++
                        // 'A metà' vince su 'saltate': una parte che doveva entrare e non è entrata
                        // conta più di una voce che questa versione non sa leggere.
                        say(
                            app,
                            when {
                                !out.applied.containsAll(out.wanted) -> R.string.backup_load_partial
                                out.skipped -> R.string.backup_loaded_skipped
                                else -> R.string.backup_loaded
                            }
                        )
                    }
                    errore is CancellationException -> Unit
                    errore != null -> say(app, failure(errore))
                }
            }
        }
    }

    /**
     * Il file aperto e il suo resoconto, pronti per la conferma, o `null` se non c'è niente da
     * confermare.
     *
     * Senza parti in comune lo si dice subito: una conferma con l'elenco vuoto chiederebbe di
     * importare niente.
     */
    private suspend fun ready(
        context: Context,
        uri: Uri,
        opened: Backup.Opened,
        groups: Set<BackupGroup>
    ): Step? {
        val manifest = read(context, uri) { Backup.manifest(it, opened) }
        if ((groups intersect manifest.groups).isEmpty()) {
            say(context, R.string.backup_none)
            return null
        }
        return Step.Ready(uri, opened, manifest, groups)
    }

    /** Mette in scena la domanda che un passo ha prodotto, o dice perché non si va avanti. */
    private suspend fun land(context: Context, esito: Result<Step?>) = withContext(NonCancellable) {
        val errore = esito.exceptionOrNull()
        step = if (errore == null) esito.getOrNull() else null
        if (errore != null && errore !is CancellationException) say(context, failure(errore))
    }

    /** La frase che dice perché un file non si apre. */
    private fun failure(errore: Throwable): Int = when ((errore as? Backup.Failure)?.reason) {
        Backup.Reason.BAD -> R.string.backup_bad
        Backup.Reason.NEWER -> R.string.backup_newer
        Backup.Reason.WRONG -> R.string.backup_wrong
        Backup.Reason.BROKEN -> R.string.backup_broken
        // Un errore che non è del formato viene dal disco o dal fornitore del file, prima che
        // l'importazione tocchi qualcosa: lo dice la frase.
        null -> R.string.backup_load_failed
    }

    /**
     * L'avanzamento di un lavoro, che aggiorna la barra solo quando cambia il numero.
     *
     * Un punto per cento alla volta e non a ogni blocco: i blocchi sono da 64 kB, e un gigabyte
     * vorrebbe dire sedicimila ricomposizioni della pagina.
     */
    private fun progress(export: Boolean, total: Long?): (Long) -> Unit {
        if (total == null || total <= 0L) return {}
        var ultimo = -1
        return { fatti ->
            val quanto = (fatti * 100 / total).toInt().coerceIn(0, 100)
            if (quanto != ultimo) {
                ultimo = quanto
                step = Step.Busy(export, quanto / 100f)
            }
        }
    }

    private suspend fun <T> read(context: Context, uri: Uri, block: suspend (InputStream) -> T): T {
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("sorgente")
        return input.use { block(it) }
    }

    /** Un'esportazione che è finita senza errori e ha lasciato un file vuoto. */
    private class Empty : IOException("vuoto")

    /**
     * Se il documento appena scritto contiene davvero qualcosa.
     *
     * ⚠️ **Due domande, per non dare un allarme falso**: il peso dichiarato dal fornitore è la
     * risposta rapida, ma non tutti lo dichiarano, e uno che carica in rete (Drive) può dirlo in
     * ritardo. Senza peso, o con peso zero, si prova a leggere il primo byte.
     * È `internal` per il banco, che la prova con un file vuoto e con uno pieno.
     */
    internal fun landed(context: Context, uri: Uri): Boolean {
        if (sizeOf(context, uri) != null) return true
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.read() >= 0 } ?: false
        }.getOrDefault(false)
    }

    /** Quanto pesa il file scelto, se il suo fornitore lo dice: serve alla barra. */
    private fun sizeOf(context: Context, uri: Uri): Long? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
        }
    }.getOrNull()?.takeIf { it > 0L }

    private fun forgetDocument(context: Context, uri: Uri) {
        runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
    }

    /** Sul filo principale: l'avviso è uno stato della composizione. */
    private suspend fun say(context: Context, text: Int) = withContext(Dispatchers.Main) {
        Notices.say(context.getString(text), NOTICE_LONG_MS)
    }
}

/**
 * La pagina 'Esporta e importa': le parti da portare, la password, e i due comandi.
 *
 * Le impostazioni si scelgono per macro-aree con una casella ciascuna (richiesta dell'utente, giro
 * della 2.91). Il formato è in [Backup], il perché delle aree in [BackupArea].
 *
 * Le stesse caselle valgono nei due versi: esportando dicono che cosa entra nel file, importando
 * che cosa esce. Due elenchi separati sarebbero due domande sullo stesso argomento.
 * ⚠️ **Di fabbrica sono accese tutte tranne il cestino** (risposta `senza-cestino` dell'utente a
 * `d-backup-fabbrica`, 2.95): è la sola parte che può pesare molto, e accanto dice quanto pesa. La
 * pagina non ricorda le caselle: rientrando si riparte dal valore di fabbrica.
 *
 * ⚠️ **L'avanzamento è una riga della pagina e non una finestra**: una finestra che si chiude
 * toccando fuori annullerebbe un lavoro lungo con un tocco distratto, e una che non si chiude
 * sarebbe una modale senza input (§ '👆 Che cosa fa il tocco FUORI da una finestra'). Come riga, si
 * esce dalla pagina e il lavoro continua.
 */
@Composable
internal fun ColumnScope.BackupPage() {
    val context = LocalContext.current
    // Le frasi degli avvisi da `LocalResources`, come nella griglia: il perché è là.
    val res = LocalResources.current
    // Gli stili sono dell'editor completo: dove quello non c'è, la loro casella non c'è.
    val offered = remember { BackupGroup.entries.filter { it != BackupGroup.STYLES || advancedEditorAvailable() } }
    var chosen by rememberSaveable(stateSaver = GROUPS) { mutableStateOf(offered.toSet() - BackupGroup.BIN) }
    var lock by rememberSaveable { mutableStateOf(false) }
    var asking by remember { mutableStateOf(false) }
    // Si rimisura a ogni importazione riuscita, che può far crescere il cestino.
    val importati by Backups.imported.collectAsState()
    val binBytes by produceState(0L, importati) { value = Backup.weight(context, BackupArea.BIN) }
    val step = Backups.step
    val busy = step as? Backups.Step.Busy
    val scelte = chosen intersect offered.toSet()

    // ⚠️ I contratti si ricordano, o si registrano di nuovo a ogni ricomposizione (come in
    // `ConvertDialog` e in `StyleSettings`).
    val scrittura = remember { ActivityResultContracts.CreateDocument(Backup.MIME) }
    val esporta = rememberLauncherForActivityResult(scrittura) { dove ->
        Backups.export(context, dove)
    }
    val lettura = remember { ActivityResultContracts.OpenDocument() }
    val importa = rememberLauncherForActivityResult(lettura) { da ->
        Backups.inspect(context, da, scelte)
    }

    fun avvia(password: String?) {
        Backups.prepare(scelte, password)
        val aperto = runCatching { esporta.launch(Backup.fileName(System.currentTimeMillis())) }
        if (aperto.isFailure) {
            Backups.drop()
            Notices.say(res.getString(R.string.toast_save_failed))
        }
    }

    /*
     * ⚠️ **Scorrono le sole caselle delle parti**; 'Proteggi con password' e i due tasti restano
     * fissi in fondo (voce `backup-caselle` dell'utente, 2.99, e sua richiesta, 5.80). Il guscio non
     * scorre (`scrolls = false`) e la colonna delle voci prende quello che resta.
     * Le voci sono più strette ([CheckRow] con `tight`, stessa voce): sul telefono più diffuso la
     * pagina entra intera senza scorrere.
     */
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Detail(stringResource(R.string.backup_intro))
        offered.forEach { group ->
            CheckRow(
                label = stringResource(group.label),
                detail = if (group == BackupGroup.BIN && binBytes > 0L) formatBytes(binBytes) else null,
                checked = group in chosen,
                onChange = { acceso -> chosen = if (acceso) chosen + group else chosen - group },
                tight = true
            )
        }
    }
    SwitchRow(
        label = stringResource(R.string.backup_lock),
        detail = stringResource(R.string.backup_lock_desc),
        checked = lock,
        onChange = { lock = it }
    )

    // 'Importa' a sinistra ed 'Esporta' a destra (voce `backup-caselle`, 2.99).
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = {
                val aperto = runCatching { importa.launch(arrayOf(ANY_FILE)) }
                if (aperto.isFailure) Notices.say(res.getString(R.string.backup_load_failed))
            },
            enabled = busy == null && scelte.isNotEmpty(),
            modifier = Modifier.weight(1f)
        ) { Text(stringResource(R.string.backup_import)) }
        Button(
            onClick = { if (lock) asking = true else avvia(null) },
            enabled = busy == null && scelte.isNotEmpty(),
            modifier = Modifier.weight(1f)
        ) { Text(stringResource(R.string.backup_export)) }
    }

    if (busy != null) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(
                    if (busy.export) R.string.backup_exporting else R.string.backup_importing
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            val quanto = busy.done
            if (quanto == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(progress = { quanto }, modifier = Modifier.fillMaxWidth())
            }
            TextButton(onClick = { Backups.cancel() }, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.cancel))
            }
        }
    }

    if (asking) {
        SetPasswordDialog(
            onDismiss = { asking = false },
            onDone = {
                asking = false
                avvia(it)
            }
        )
    }
    when (step) {
        is Backups.Step.Locked -> UnlockDialog(
            wrong = step.wrong,
            onDismiss = { Backups.forget() },
            onDone = { Backups.unlock(context, it) }
        )
        is Backups.Step.Ready -> ConfirmDialog(
            ready = step,
            onDismiss = { Backups.forget() },
            onDone = { Backups.restore(context) }
        )
        else -> Unit
    }
}

/**
 * La password di un'esportazione, scritta due volte.
 *
 * Due volte perché nessuno la può rileggere: una lettera sbagliata fa un file che non si apre più.
 * È una modale vera, perché raccoglie un input scritto (§ '👆 Che cosa fa il tocco FUORI da una
 * finestra').
 */
@Composable
private fun SetPasswordDialog(onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var one by remember { mutableStateOf("") }
    var two by remember { mutableStateOf("") }
    val differ = two.isNotEmpty() && one != two
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(null),
        properties = loweredWindow(null),
        title = { Text(stringResource(R.string.backup_password_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PasswordField(
                    value = one,
                    onValue = { one = it },
                    label = stringResource(R.string.backup_password),
                    last = false
                )
                PasswordField(
                    value = two,
                    onValue = { two = it },
                    label = stringResource(R.string.backup_password_again),
                    last = true,
                    error = if (differ) stringResource(R.string.backup_password_mismatch) else null
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onDone(one) }, enabled = one.isNotEmpty() && one == two) {
                Text(stringResource(R.string.backup_export))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * La password di un file protetto, all'importazione.
 *
 * L'avviso di una password sbagliata sparisce appena si ricomincia a scrivere: parla del tentativo
 * appena fatto, non di quello nuovo.
 */
@Composable
private fun UnlockDialog(wrong: Boolean, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(null),
        properties = loweredWindow(null),
        title = { Text(stringResource(R.string.backup_password_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.backup_unlock))
                PasswordField(
                    value = text,
                    onValue = { text = it },
                    label = stringResource(R.string.backup_password),
                    last = true,
                    error = if (wrong && text.isEmpty()) stringResource(R.string.backup_wrong) else null
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onDone(text) }, enabled = text.isNotEmpty()) {
                Text(stringResource(R.string.backup_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Che cosa entrerà, e da quale file di impostazioni.
 *
 * L'elenco è l'incrocio fra le caselle e il file: è quello che [Backup.restore] farà, detto prima.
 * Non è una modale vera: non raccoglie niente di scritto, e il tocco fuori vale 'Annulla'.
 * La riga di testa (`backup_import_from`, testo dell'utente, 2.95) riceve la data e l'ora come due
 * argomenti, perché la frase le mette dove le vuole; le scrive la lingua del telefono.
 */
@Composable
private fun ConfirmDialog(ready: Backups.Step.Ready, onDismiss: () -> Unit, onDone: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val (giorno, ora) = remember(ready, locale) {
        val quando = Date(ready.manifest.created)
        DateFormat.getDateInstance(DateFormat.LONG, locale).format(quando) to
            DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(quando)
    }
    val parti = BackupGroup.entries.filter { it in ready.groups && it in ready.manifest.groups }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(onDismiss),
        properties = loweredWindow(onDismiss),
        title = { Text(stringResource(R.string.backup_import)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.backup_import_from, giorno, ora, ready.manifest.app))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    parti.forEach {
                        Text(text = stringResource(it.label), style = MaterialTheme.typography.titleSmall)
                    }
                }
                Text(stringResource(R.string.backup_import_ask))
            }
        },
        confirmButton = {
            TextButton(onClick = onDone) { Text(stringResource(R.string.backup_import)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Un campo in cui si scrive una password.
 *
 * La tastiera è quella delle password, che non suggerisce e non impara.
 */
@Composable
private fun PasswordField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    last: Boolean,
    error: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = if (last) ImeAction.Done else ImeAction.Next
        ),
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        shape = BOX_SHAPE,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Come le caselle spuntate sopravvivono a una rotazione: i token, che sono il formato.
 *
 * I token e non gli ordinali: con una parte nuova in mezzo, un ordinale salvato indicherebbe la
 * vicina.
 */
private val GROUPS = listSaver<Set<BackupGroup>, String>(
    save = { groups -> groups.map { it.token } },
    restore = { saved -> saved.mapNotNull { t -> BackupGroup.entries.firstOrNull { it.token == t } }.toSet() }
)

/**
 * Che file il selettore lascia scegliere all'importazione: tutti.
 *
 * ⚠️ **Tutti, non il tipo del file di impostazioni**: un file passato da Drive, da una chat o da
 * un'altra app arriva col tipo che decide chi lo serve, e un filtro lo nasconderebbe. Se è un file
 * di impostazioni lo dice la sua intestazione.
 */
private const val ANY_FILE = "*/*"

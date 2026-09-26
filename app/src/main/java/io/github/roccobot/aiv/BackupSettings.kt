package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
 * Il lavoro di un backup: quello che sta facendo, e le domande che deve fare.
 *
 * ⚠️⚠️ **VIVE COL PROCESSO E NON CON LA PAGINA, E LA RAGIONE È IL CESTINO**: un backup che lo porta
 * pesa quanto le immagini che contiene, cioè può durare dei minuti, e una pagina che si chiude o un
 * telefono che si gira non devono fermarlo a metà. Rientrando nella pagina si ritrova il lavoro dov'è,
 * e la notifica finale arriva dovunque ci si trovi, perché la superficie che la mostra è di casa.
 * ⚠️ **Le domande vivono qui per la stessa ragione**: fra la lettura dell'intestazione e la conferma
 * passano una finestra e forse una rotazione, e la chiave nata dalla password non può viaggiare in un
 * `Bundle`, che Android scrive su disco.
 *
 * ⚠️⚠️ **LA PASSWORD NON VA MAI SU DISCO**: vive in [plan] per il tempo del selettore di sistema, e il
 * campo in cui la si scrive è un `remember` e non un `rememberSaveable`. Il prezzo è dichiarato: se il
 * telefono si gira mentre la finestra è aperta, la password va riscritta.
 */
internal object Backups {

    /** Che cosa la pagina deve mostrare oltre a sé stessa. */
    sealed interface Step {
        /** Il file è protetto: serve la sua password. [wrong] dice che quella di prima non andava. */
        class Locked(
            val uri: Uri,
            val head: Backup.Head,
            val areas: Set<BackupArea>,
            val wrong: Boolean
        ) : Step

        /** Il file è aperto: si conferma che cosa rimettere. */
        class Ready(
            val uri: Uri,
            val opened: Backup.Opened,
            val manifest: Backup.Manifest,
            val areas: Set<BackupArea>
        ) : Step

        /** Un lavoro in corso, e a che punto è: `null` quando non si può dire. */
        class Busy(val export: Boolean, val done: Float?) : Step
    }

    var step: Step? by mutableStateOf(null)
        private set

    /**
     * Quante importazioni sono andate in porto.
     *
     * ⚠️ **Serve al modello, che ricarica quello che non passa dal flusso delle preferenze**: il logo,
     * le tinte e le copertine si leggono una volta e poi si aggiornano a mano, e un'importazione li
     * cambia senza passare da quelle strade.
     */
    val imported = MutableStateFlow(0)

    /** Quello che un'esportazione deve fare, fra la finestra della password e il selettore. */
    private class Plan(val areas: Set<BackupArea>, val password: String?)

    private var plan: Plan? = null
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Fissa le parti e la password dell'esportazione, prima di aprire il selettore. */
    fun prepare(areas: Set<BackupArea>, password: String?) {
        plan = Plan(areas, password)
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
     * Scrive il backup nel documento che il selettore ha appena creato.
     *
     * ⚠️⚠️ **SE NON RIESCE, IL DOCUMENTO SI CANCELLA**: il selettore l'ha già creato vuoto, e un file che
     * resta a metà si leggerebbe come un backup. Non si apre comunque (manca il suo ultimo segmento), ma
     * trovarlo fra i propri file e scoprirlo solo importandolo è peggio di non trovarlo.
     * ⚠️ **Un piano che manca vuol dire un processo ripartito mentre il selettore era aperto**, cioè
     * una password che non c'è più: si cancella il documento invece di scrivere senza la protezione che
     * era stata chiesta.
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
                val avanza = progress(export = true, total = Backup.estimate(app, piano.areas))
                val out = app.contentResolver.openOutputStream(uri, "w")
                    ?: throw IOException("destinazione")
                out.use { Backup.write(app, it, piano.areas, piano.password, onBytes = avanza) }
            }
            withContext(NonCancellable) {
                val errore = esito.exceptionOrNull()
                if (errore != null) forgetDocument(app, uri)
                step = null
                // ⚠️ Un annullamento l'ha chiesto chi guarda, e non ha bisogno che glielo si dica.
                if (errore !is CancellationException) {
                    say(app, if (errore == null) R.string.backup_saved else R.string.toast_save_failed)
                }
            }
        }
    }

    /**
     * Apre il file scelto per importarlo: legge l'intestazione, e chiede la password o conferma.
     *
     * @param areas le caselle spuntate al momento del tocco: si importano quelle, e solo se il file le
     *   porta.
     */
    fun inspect(context: Context, uri: Uri?, areas: Set<BackupArea>) {
        if (uri == null || job?.isActive == true) return
        val app = context.applicationContext
        job = scope.launch {
            step = Step.Busy(export = false, done = null)
            val esito = runCatching {
                val head = read(app, uri) { Backup.head(it) }
                if (head.locked) Step.Locked(uri, head, areas, wrong = false)
                else ready(app, uri, Backup.open(head, null), areas)
            }
            land(app, esito)
        }
    }

    /**
     * Prova la password del file in scena.
     *
     * ⚠️ **Una password sbagliata riapre la stessa domanda, col suo avviso**: chiuderla vorrebbe dire
     * ricominciare dal selettore per una lettera sbagliata.
     * ⚠️ **È il passo lento**, e l'annullamento si vede solo dopo: il conto della chiave non si ferma a
     * metà, ma il suo risultato non arriva a nessuno.
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
                ready(app, chiuso.uri, opened, chiuso.areas)
            }
            val errore = esito.exceptionOrNull()
            if (errore is Backup.Failure && errore.reason == Backup.Reason.WRONG) {
                step = Step.Locked(chiuso.uri, chiuso.head, chiuso.areas, wrong = true)
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
                    Backup.restore(app, it, pronto.opened, pronto.areas, onBytes = avanza)
                }
            }
            withContext(NonCancellable) {
                step = null
                val errore = esito.exceptionOrNull()
                val out = esito.getOrNull()
                when {
                    out != null -> {
                        imported.value++
                        // ⚠️ 'A metà' vince su 'saltate': una parte che doveva entrare e non è
                        // entrata conta di più di una voce che questa versione non poteva leggere.
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
     * ⚠️ **Nessuna parte in comune si dice subito**: una finestra di conferma con un elenco vuoto
     * chiederebbe di importare niente.
     */
    private suspend fun ready(
        context: Context,
        uri: Uri,
        opened: Backup.Opened,
        areas: Set<BackupArea>
    ): Step? {
        val manifest = read(context, uri) { Backup.manifest(it, opened) }
        if ((areas intersect manifest.areas).isEmpty()) {
            say(context, R.string.backup_none)
            return null
        }
        return Step.Ready(uri, opened, manifest, areas)
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
        // ⚠️ Un errore che non è del formato viene dal disco o dal fornitore del file, e
        // l'importazione non è arrivata a toccare niente: lo dice la frase.
        null -> R.string.backup_load_failed
    }

    /**
     * L'avanzamento di un lavoro, che aggiorna la barra solo quando cambia il numero.
     *
     * ⚠️ **Un punto per cento alla volta, e non a ogni blocco**: i blocchi sono da 64 kB, e un gigabyte
     * ne porta sedicimila, cioè sedicimila ricomposizioni della pagina per una barra che si muove di un
     * pixel ogni cento.
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

    /** Quanto pesa il file scelto, se il suo fornitore lo dice: serve alla barra. */
    private fun sizeOf(context: Context, uri: Uri): Long? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
        }
    }.getOrNull()?.takeIf { it > 0L }

    private fun forgetDocument(context: Context, uri: Uri) {
        runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
    }

    /** ⚠️ Sul thread principale: la notifica di casa è uno stato della composizione. */
    private suspend fun say(context: Context, text: Int) = withContext(Dispatchers.Main) {
        Notices.say(context.getString(text), NOTICE_LONG_MS)
    }
}

/**
 * La pagina 'Esporta e importa': le parti da portare, la password, e i due comandi.
 *
 * ⚠️⚠️ **NASCE NELLA `2.93`, ED È LA TERZA RICHIESTA DEL CAMPO LIBERO DEL GIRO DELLA `2.91`** (*deve
 * contenere letteralmente TUTTE le impostazioni dell'app che possono essere salvate, ma ogni cosa su
 * richiesta con apposita checkbox ... per non fare un elenco troppo lungo di checkbox, si selezionerà
 * per macro-aree*). Il formato vive in [Backup], e il perché delle aree in [BackupArea].
 *
 * ⚠️⚠️ **LE STESSE CASELLE VALGONO NEI DUE VERSI, ED È UNA SCELTA DICHIARATA**: esportando dicono che
 * cosa entra nel file, importando che cosa esce dal file. Due elenchi separati sarebbero due domande
 * sullo stesso argomento, e la seconda si leggerebbe come un doppione della prima.
 * ⚠️ **Di fabbrica sono accese tutte, cestino compreso**: la sua richiesta dice *tutte*, e il cestino è
 * la sola casella che accanto dice quanto pesa, così chi manda un backup su Drive lo sa prima.
 *
 * ⚠️⚠️ **L'AVANZAMENTO È UNA RIGA DELLA PAGINA E NON UNA FINESTRA**: una finestra che si chiude toccando
 * fuori annullerebbe un lavoro lungo con un tocco distratto, e una che non si chiude sarebbe una modale
 * senza niente da scrivere, cioè il caso che la regola di casa esclude (§ '👆 Che cosa fa il tocco FUORI
 * da una finestra'). Come riga, si esce dalla pagina e il lavoro continua.
 */
@Composable
internal fun BackupPage() {
    val context = LocalContext.current
    // ⚠️ Gli stili sono dell'editor completo, e dove quello non c'è la loro casella non avrebbe niente
    // da portare: è lo stesso criterio della loro pagina.
    val offered = remember { BackupArea.entries.filter { it != BackupArea.STYLES || advancedEditorAvailable() } }
    var chosen by rememberSaveable(stateSaver = AREAS) { mutableStateOf(offered.toSet()) }
    var lock by rememberSaveable { mutableStateOf(false) }
    var asking by remember { mutableStateOf(false) }
    // ⚠️ Si rimisura a ogni importazione riuscita: il cestino è la sola parte che un'importazione fa
    // crescere, e senza questa chiave la pagina direbbe il peso di prima finché non la si riapre.
    val importati by Backups.imported.collectAsState()
    val binBytes by produceState(0L, importati) { value = Backup.weight(context, BackupArea.BIN) }
    val step = Backups.step
    val busy = step as? Backups.Step.Busy
    val scelte = chosen intersect offered.toSet()

    /*
     * ⚠️ **I CONTRATTI SI RICORDANO**, o si registrano di nuovo a ogni ricomposizione: è la stessa
     * trappola scritta in `ConvertDialog` e in `StyleSettings`.
     */
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
            Notices.say(context.getString(R.string.toast_save_failed))
        }
    }

    Detail(stringResource(R.string.backup_intro))
    offered.forEach { area ->
        CheckRow(
            label = stringResource(area.label),
            detail = if (area == BackupArea.BIN && binBytes > 0L) formatBytes(binBytes) else null,
            checked = area in chosen,
            onChange = { acceso -> chosen = if (acceso) chosen + area else chosen - area }
        )
    }
    SwitchRow(
        label = stringResource(R.string.backup_lock),
        detail = stringResource(R.string.backup_lock_desc),
        checked = lock,
        onChange = { lock = it }
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = { if (lock) asking = true else avvia(null) },
            enabled = busy == null && scelte.isNotEmpty(),
            modifier = Modifier.weight(1f)
        ) { Text(stringResource(R.string.backup_export)) }
        Button(
            onClick = {
                val aperto = runCatching { importa.launch(arrayOf(ANY_FILE)) }
                if (aperto.isFailure) Notices.say(context.getString(R.string.backup_load_failed))
            },
            enabled = busy == null && scelte.isNotEmpty(),
            modifier = Modifier.weight(1f)
        ) { Text(stringResource(R.string.backup_import)) }
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
 * ⚠️ **Due volte perché nessuno la può rileggere**: una lettera sbagliata qui fa un file che non si
 * apre più, e il testo della voce lo dice (*se la dimentichi, il file non si apre più*).
 * ⚠️ **È una modale vera**, perché raccoglie un input scritto: le due righe di § '👆 Che cosa fa il
 * tocco FUORI da una finestra' vanno insieme.
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
 * ⚠️ **L'avviso di una password sbagliata sparisce appena si ricomincia a scrivere**: resta finché il
 * campo è vuoto, cioè finché dice qualcosa sul tentativo appena fatto e non su quello nuovo.
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
 * Che cosa entrerà, e da quale backup.
 *
 * ⚠️⚠️ **L'ELENCO È L'INCROCIO FRA LE CASELLE E IL FILE**: una parte spuntata che il file non porta non
 * compare, e una che il file porta ma non è spuntata nemmeno. È quello che [Backup.restore] farà, detto
 * prima.
 * ⚠️ **Non è una modale vera**: non raccoglie niente di scritto, e il tocco fuori vale 'Annulla', che è
 * l'esito sicuro.
 */
@Composable
private fun ConfirmDialog(ready: Backups.Step.Ready, onDismiss: () -> Unit, onDone: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val quando = remember(ready, locale) {
        DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, locale)
            .format(Date(ready.manifest.created))
    }
    val parti = BackupArea.entries.filter { it in ready.areas && it in ready.manifest.areas }
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
                Text(stringResource(R.string.backup_import_from, quando, ready.manifest.app))
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
 * ⚠️ **La tastiera è quella delle password**, che non suggerisce e non impara: una password proposta
 * dai suggerimenti è una password che la tastiera ha tenuto.
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
 * ⚠️ **I token e non gli ordinali**: l'ordine delle aree è quello delle caselle, e il giorno che ne
 * nasce una in mezzo un ordinale salvato indicherebbe la vicina.
 */
private val AREAS = listSaver<Set<BackupArea>, String>(
    save = { areas -> areas.map { it.token } },
    restore = { saved -> saved.mapNotNull { t -> BackupArea.entries.firstOrNull { it.token == t } }.toSet() }
)

/**
 * Che file il selettore lascia scegliere all'importazione: tutti.
 *
 * ⚠️ **Tutti e non il tipo del backup**: un file passato da Drive, da una chat o da un'altra app arriva
 * col tipo che ha deciso chi lo serve, e filtrando per tipo il backup giusto non si vedrebbe. A dire se
 * è un backup ci pensa la sua intestazione.
 */
private const val ANY_FILE = "*/*"

package io.github.roccobot.aiv

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * 'Genera miniature': prepara la miniatura di ogni immagine e di ogni video delle cartelle visibili.
 *
 * Una pagina con titolo, numero X/Y, barra e 'Annulla', e un avviso alla fine (specifica
 * dell'utente, 2.97). Che cosa resta su disco è scritto su [Thumbs.warmer].
 *
 * ⚠️ **Vive col processo e non con la pagina, come il backup** ([Backups]): su una collezione
 * grande il lavoro dura dei minuti, e una rotazione non deve ripartire da capo. Rientrando si
 * ritrova la pagina al punto in cui era, perché a disegnarla è [run].
 * Con l'app in secondo piano continua: la risposta `aperta` a `d-cestino-chiusa` riguarda i
 * lavori che partono da soli, come lo svuotamento automatico del cestino, e questo lo fa partire
 * l'utente. Se il sistema chiude il processo, quello che è già fatto resta su disco.
 *
 * Le cartelle visibili sono quelle dell'elenco iniziale ([Folder.everything]), senza le nascoste
 * e senza il minuto di prestito di 'Mostra nascoste', che serve a entrare in una cartella.
 */
internal object Warmup {

    /** A che punto è la generazione: [total] è `null` mentre si contano le immagini. */
    class Run(val done: Int, val total: Int?)

    /**
     * La generazione in scena, o `null`: la pagina c'è se e solo se c'è questo valore.
     *
     * Si scrive da un thread di I/O, come [Backups.step]: lo snapshot globale di Compose lo
     * consente, e le scritture dello stesso fotogramma si leggono come una sola.
     */
    var run: Run? by mutableStateOf(null)
        private set

    /**
     * Quante generazioni sono finite, annullate comprese.
     *
     * È la chiave con cui la pagina delle impostazioni rimisura la riga del riepilogo: una
     * generazione scrive in [AvifCache], e senza la riga direbbe il peso di prima.
     */
    var finished by mutableIntStateOf(0)
        private set

    /**
     * Il numero della generazione in corso.
     *
     * ⚠️ **Impedisce alla pagina di ricomparire dopo 'Annulla'**: la pagina se ne va subito,
     * mentre le miniature già chieste finiscono il loro giro, e un loro aggiornamento scriverebbe
     * di nuovo [run]. Ogni aggiornamento ha il numero della sua generazione, e quello di una
     * generazione annullata non conta più; così una generazione nuova può partire mentre la
     * vecchia sta ancora finendo.
     */
    private var gen = 0
    private val lock = Any()
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Comincia a generare. Con una generazione già in scena non fa niente: il tasto che la chiede è
     * sotto la pagina, quindi è un caso che un dito non raggiunge.
     *
     * @param hidden le cartelle nascoste, come le scrive [Settings.hiddenFolders].
     */
    fun start(context: Context, hidden: Set<String>,
        selection: FolderSelection = FolderSelection(hidden = hidden)) {
        val app = context.applicationContext
        synchronized(lock) {
            if (run != null) return
            val mio = ++gen
            run = Run(0, null)
            job = scope.launch { work(app, hidden, mio, selection) }
        }
    }

    /**
     * Ferma la generazione, e la pagina se ne va subito.
     *
     * Le miniature finite restano su disco: annullare vuol dire smettere, non disfare. Quelle già
     * chieste finiscono in un istante, perché la loro richiesta si annulla con il lavoro.
     */
    fun cancel() {
        synchronized(lock) {
            gen++
            run = null
            job?.cancel()
        }
    }

    private suspend fun work(app: Context, hidden: Set<String>, mio: Int, selection: FolderSelection) {
        val caricatore = Thumbs.warmer(app)
        val esito = runCatching {
            val elenco = Folder.everything(app, hidden, selection)
            post(mio, 0, elenco.size)
            warmAll(elenco, WARM_LANES, work = { Thumbs.warm(app, it, caricatore) }) { fatti ->
                post(mio, fatti, elenco.size)
            }
        }
        withContext(NonCancellable) {
            caricatore.shutdown()
            val mia = synchronized(lock) {
                finished++
                (gen == mio).also { if (it) run = null }
            }
            /*
             * L'avviso si dà solo se la generazione è arrivata in fondo: un annullamento l'ha
             * chiesto chi guarda. Un errore che non è di un file non ha una strada nota (vedi
             * [warmAll]), ma se arrivasse la pagina se ne va senza l'avviso, che sarebbe falso.
             */
            if (mia && esito.isSuccess) {
                withContext(Dispatchers.Main) {
                    Notices.say(app.getString(R.string.settings_thumbs_gen_done), NOTICE_LONG_MS)
                }
            }
        }
    }

    /**
     * Scrive a che punto si è, se la generazione è ancora quella in scena e il numero è andato avanti.
     *
     * Il numero va solo avanti: due corsie finiscono in un ordine diverso da quello in cui hanno
     * preso il conto, e senza la guardia X scenderebbe di uno per un fotogramma.
     * Un aggiornamento per file, perché X/Y deve crescere man mano (specifica dell'utente); le
     * scritture dello stesso fotogramma si ricompongono una volta sola.
     */
    private fun post(mio: Int, done: Int, total: Int) {
        synchronized(lock) {
            if (gen != mio) return
            val adesso = run ?: return
            if (adesso.total != null && done <= adesso.done) return
            run = Run(done, total)
        }
    }
}

/**
 * Passa [items] a [work] su [lanes] corsie, e dice a [onDone] quanti ne sono finiti.
 *
 * ⚠️ **Un file che non si legge conta come fatto**: una collezione vera ha sempre qualche file
 * rotto o sparito nel frattempo, e fermarsi al primo vorrebbe dire non arrivare mai in fondo.
 * L'unica eccezione che si propaga è l'annullamento.
 * Il conto delle voci è uno solo per tutte le corsie (`next`), così nessun file si fa due volte e
 * nessuno resta fuori.
 * È `internal` per il banco, che la chiama con un lavoro finto: la generazione vera vuole un
 * MediaStore con delle immagini, e quello di Robolectric è vuoto.
 */
internal suspend fun <T> warmAll(
    items: List<T>,
    lanes: Int,
    work: suspend (T) -> Unit,
    onDone: (Int) -> Unit
) {
    coroutineScope {
        val next = AtomicInteger(0)
        val fatti = AtomicInteger(0)
        repeat(minOf(lanes, items.size)) {
            launch {
                while (true) {
                    ensureActive()
                    val i = next.getAndIncrement()
                    if (i >= items.size) break
                    try {
                        work(items[i])
                    } catch (e: Throwable) {
                        if (e is CancellationException) throw e
                    }
                    onDone(fatti.incrementAndGet())
                }
            }
        }
    }
}

/**
 * Quante miniature si generano insieme.
 *
 * ⚠️ **Due, per l'AVIF**: la sua miniatura si fa leggendo il file intero in memoria (vedi
 * `AvifThumbnailFetcher`), quindi ogni corsia in più è un file intero in più nello stesso
 * istante. Per i file serviti dal sistema la miniatura la fa il provider nel suo processo, e due
 * richieste insieme bastano a tenerlo occupato. Quanto dura una generazione non è misurato.
 */
private const val WARM_LANES = 2

/**
 * La pagina della generazione: il titolo, il numero X/Y, la barra e 'Annulla'.
 *
 * Una finestra a tutto schermo, al contrario della riga del backup (specifica dell'utente: *una
 * pagina semplice*). Copre le impostazioni, quindi non chiama `WindowVeil()` (l'esenzione è
 * dichiarata su [fullWindow]).
 * ⚠️ **Indietro vale 'Annulla'**: una pagina che si chiudesse lasciando il lavoro in corso
 * nasconderebbe un lavoro che non si può più fermare.
 * Il blocco è al centro esatto, non il 15% più in basso: è una schermata intera, non una
 * finestra che si apre in mezzo allo schermo.
 * Il numero si scrive come ogni altro conteggio dell'app, con `%d` (senza separatore delle
 * migliaia, con le cifre della lingua in corso); le cifre a larghezza fissa lo tengono fermo
 * mentre cresce.
 */
@Composable
internal fun WarmupPage(run: Warmup.Run, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel, properties = fullWindow()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
            ) {
                Text(
                    text = stringResource(R.string.settings_thumbs_gen_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )
                val locale = LocalConfiguration.current.locales[0]
                val total = run.total
                // Mentre si contano le immagini la riga resta vuota: sparendo, la barra e il tasto
                // salterebbero di una riga nell'istante in cui il conto arriva.
                Text(
                    text = if (total == null) "" else String.format(locale, "%d/%d", run.done, total),
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (total == null || total == 0) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(
                        progress = { run.done.toFloat() / total },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}

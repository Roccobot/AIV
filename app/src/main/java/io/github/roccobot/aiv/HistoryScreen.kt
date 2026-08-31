package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.io.File

/**
 * La cronologia dei ripristini: dove sono finiti i file usciti dal cestino.
 *
 * ⚠️⚠️ **NASCE DALLA 0.76** (richiesta dell'utente, con la sua ragione: *talvolta si fa un
 * ripristina senza pensarci troppo e senza ricordare dove è stato ripristinato un file*). Si
 * apre dal menu del cestino, che è il solo posto da cui ha senso cercarla, e il registro che
 * legge lo scrive `Bin.restore`: vedi [History] per il perché sia un secondo archivio e non
 * una lettura di quello del cestino.
 *
 * ⚠️ **Testata FISSA e lista pigra**, al contrario delle pagine delle impostazioni, che
 * scorrono tutte insieme dentro una colonna: là le voci sono venti e si contano, qui un solo
 * 'ripristina tutto' su una cartella grossa può scriverne trecento. ⚠️ È anche la ragione per
 * cui questa schermata non usa il guscio delle impostazioni: quello porta lo scorrimento con
 * sé, e uno scorrimento dentro l'altro non si può.
 */
@Composable
fun HistoryScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    /*
     * ⚠️ **Si legge una volta all'ingresso e non si osserva**: la cronologia cambia solo
     * quando si ripristina qualcosa, e da qui non si ripristina niente. Un flusso che
     * guardasse il file costerebbe un osservatore per un dato che non si muove mentre lo si
     * guarda.
     * ⚠️⚠️ **`null` vuol dire 'sto leggendo' e NON 'niente', e i due casi mostrano cose
     * diverse**: senza la distinzione, per un fotogramma comparirebbe 'non è stato
     * ripristinato niente' anche a chi ha una cronologia piena, che è la bugia peggiore che
     * questa schermata potrebbe raccontare. Mentre legge non mostra nulla invece di un
     * anello: è un file di testo su disco locale, e un anello che compare e sparisce in un
     * decimo di secondo **è** un lampeggio (la stessa ragione dell'attesa dell'anello nel
     * visualizzatore).
     */
    val batches by produceState<List<History.Batch>?>(initialValue = null, context) {
        value = History.batches(context)
    }

    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        // ⚠️ Il rientro è quello delle impostazioni, [EDGE], e vale anche per la freccia: la
        // testata e la lista devono cominciare sulla stessa colonna, o la pagina si legge
        // come due pagine.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = EDGE, vertical = 12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back)
                )
            }
            // ⚠️ Il titolo prende `weight`, come nelle impostazioni: 'Cronologia dei
            // ripristini' in tedesco e in tamil è quasi il doppio, e senza peso una `Row` lo
            // taglia invece di mandarlo a capo.
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
        }

        val groups = batches
        when {
            groups == null -> Unit

            groups.isEmpty() -> Detail(
                text = stringResource(R.string.history_none, History.DAYS),
                modifier = Modifier.padding(horizontal = EDGE)
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(start = EDGE, end = EDGE, bottom = 24.dp)
            ) {
                /*
                 * ⚠️ **La regola dei sette giorni si dice, e sta DENTRO la lista**: chi apre
                 * questa schermata e non trova il ripristino di due settimane fa deve poter
                 * capire perché senza chiederlo a nessuno. Dentro la lista e non sopra perché
                 * è una nota e non una testata: scorre via col resto.
                 * ⚠️ **Il numero arriva da [History.DAYS] e non è scritto nella frase**, così
                 * il testo e la scadenza vera non possono divergere.
                 */
                item(key = "nota") {
                    Detail(
                        text = stringResource(R.string.history_note, History.DAYS),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                /*
                 * ⚠️⚠️ **UN GRUPPO PER OPERAZIONE, col filetto in mezzo** (richiesta
                 * dell'utente: *tutti i file rimessi a posto dalla stessa operazione di
                 * ripristino vanno di seguito, poi un separatore se c'è altro dopo*). Il
                 * filetto sta **fra** i gruppi e non sotto ognuno: sotto l'ultimo sarebbe una
                 * riga che promette qualcosa che non c'è.
                 * ⚠️ **Le chiavi portano l'istante del gruppo**, che è unico per costruzione
                 * (i gruppi nascono raggruppando su quello) e distingue lo stesso percorso
                 * ripristinato due volte.
                 */
                groups.forEachIndexed { i, batch ->
                    item(key = "q${batch.at}") { Moment(batch.at) }
                    items(items = batch.paths, key = { "${batch.at}\t$it" }) { Line(it) }
                    if (i < groups.lastIndex) {
                        item(key = "f${batch.at}") {
                            HorizontalDivider(Modifier.padding(vertical = 14.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quando è avvenuto un ripristino: il capo del suo gruppo.
 *
 * ⚠️ Nel colore primario e in `titleSmall`, come i titoli di gruppo delle impostazioni: chi
 * non distingue quel colore vede comunque un testo di un altro corpo, che è quello che deve
 * dire 'da qui comincia un altro gruppo'.
 */
@Composable
private fun Moment(at: Long) {
    Text(
        text = moment(at),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

/**
 * Un file ripristinato: come si chiama, e dove è finito.
 *
 * ⚠️⚠️ **DUE RIGHE E NON IL PERCORSO INTERO SU UNA**, ed è la richiesta letta alla lettera
 * (*ognuno col proprio percorso*): il nome è quello che si cerca, la cartella è la risposta.
 * Su una riga sola il nome starebbe in fondo, dopo cinquanta caratteri di percorso, cioè
 * fuori dallo schermo esattamente nei casi in cui serve.
 * ⚠️ **Il percorso è quello vero e non abbellito**, `/storage/emulated/0/...` compreso: è la
 * stessa scelta della riga 'Cartella' di 'Info dettagliate sul file', e un percorso accorciato
 * non si potrebbe ricopiare in un gestore di file.
 * ⚠️ **Il nome torna dal percorso** invece di essere una seconda colonna dell'archivio: due
 * dati che dicono la stessa cosa divergono il giorno che uno dei due si scrive male.
 */
@Composable
private fun Line(path: String) {
    val file = File(path)
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(text = file.name, style = MaterialTheme.typography.bodyLarge)
        Detail(text = file.parent ?: path)
    }
}

/** Un testo di servizio, nello stile che le impostazioni usano per le spiegazioni. */
@Composable
private fun Detail(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/** Il margine laterale, quello delle pagine delle impostazioni. */
private val EDGE = 20.dp

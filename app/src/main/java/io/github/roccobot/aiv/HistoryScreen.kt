package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
 * La cronologia dei ripristini: dove sono finiti i file usciti dal cestino. Si apre dal menu del
 * cestino, e il registro che legge lo scrive `Bin.restore` (vedi [History] per il perché di un
 * secondo archivio).
 *
 * ⚠️ **Testata fissa e lista pigra**, e quindi niente guscio delle impostazioni, che scorre tutto
 * in una colonna: un 'ripristina tutto' su una cartella grossa scrive centinaia di righe, e uno
 * scorrimento dentro l'altro non si può.
 */
@Composable
fun HistoryScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    /*
     * Si legge una volta all'ingresso, senza osservare il file: da qui non si ripristina niente.
     * ⚠️ **`null` vuol dire 'sto leggendo', non 'niente'**: senza la distinzione, per un fotogramma
     * comparirebbe 'non è stato ripristinato niente' anche a chi ha una cronologia piena. Mentre
     * legge non mostra niente, nemmeno l'anello, che su un file locale comparirebbe e sparirebbe in
     * un lampo.
     */
    val batches by produceState<List<History.Batch>?>(initialValue = null, context) {
        value = History.batches(context)
    }

    val widthDp = LocalConfiguration.current.screenWidthDp
    val dual = Adaptive.sideAvailable(widthDp)

    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        // Il rientro è [PAGE_SIDE], come nelle impostazioni, anche per la freccia: testata e lista
        // cominciano sulla stessa colonna.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = PAGE_SIDE, vertical = 12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back)
                )
            }
            // Il titolo prende `weight`, come nelle impostazioni: in tedesco e in tamil è quasi il
            // doppio, e senza peso la `Row` lo taglia.
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f).heading()
            )
        }

        val groups = batches
        when {
            groups == null -> Unit

            groups.isEmpty() -> Detail(
                text = stringResource(R.string.history_none, History.DAYS),
                modifier = Modifier.padding(horizontal = PAGE_SIDE)
            )

            /* Da 600 dp due colonne (mockup `history`): gruppi per data a lato, percorsi al centro. */
            dual -> {
                var chosen by remember(groups) { mutableStateOf(groups.first().at) }
                val batch = groups.firstOrNull { it.at == chosen } ?: groups.first()
                FoldersTabletSplit(
                    panelOnStart = LocalPadLook.current.hand == Hand.RIGHT,
                    modifier = Modifier.weight(1f),
                    rail = {
                        HistoryDateRail(
                            groups = groups,
                            selected = chosen,
                            onPick = { chosen = it },
                            width = Adaptive.sideWidth(widthDp),
                            modifier = Modifier.fillMaxHeight()
                        )
                    },
                    detail = {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = PAGE_SIDE, end = PAGE_SIDE, bottom = 24.dp
                            )
                        ) {
                            item(key = "nota") {
                                Detail(
                                    text = stringResource(R.string.history_note, History.DAYS),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }
                            item(key = "q${batch.at}") { Moment(batch.at) }
                            items(items = batch.paths, key = { "${batch.at}\t$it" }) { Line(it) }
                        }
                    }
                )
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(start = PAGE_SIDE, end = PAGE_SIDE, bottom = 24.dp)
            ) {
                /*
                 * La regola dei sette giorni si dice dentro la lista, come nota che scorre col
                 * resto: chi non trova un ripristino vecchio capisce perché. Il numero viene da
                 * [History.DAYS], così testo e scadenza vera non divergono.
                 */
                item(key = "nota") {
                    Detail(
                        text = stringResource(R.string.history_note, History.DAYS),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                /*
                 * Un gruppo per operazione di ripristino, col filetto fra i gruppi e non sotto
                 * l'ultimo. Le chiavi contengono l'istante del gruppo, unico per costruzione, che
                 * distingue lo stesso percorso ripristinato due volte.
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

/** L'indice delle operazioni in colonna, per la cronologia sul tablet. */
@Composable
private fun HistoryDateRail(
    groups: List<History.Batch>,
    selected: Long,
    onPick: (Long) -> Unit,
    width: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(width)
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        for (batch in groups) {
            val chosen = batch.at == selected
            Text(
                text = moment(batch.at),
                style = MaterialTheme.typography.bodyMedium,
                color = if (chosen) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (chosen) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable { onPick(batch.at) }
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            )
        }
    }
}

/**
 * Quando è avvenuto un ripristino: il capo del suo gruppo. In [accentInk] e in `titleSmall`, come i
 * titoli di gruppo delle impostazioni: `primary` come colore di testo non arriva alla soglia di
 * contrasto, e il corpo diverso distingue il gruppo anche a chi non vede la tinta.
 */
@Composable
private fun Moment(at: Long) {
    Text(
        text = moment(at),
        style = MaterialTheme.typography.titleSmall,
        color = accentInk(),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

/**
 * Un file ripristinato su due righe, nome e cartella: su una sola il nome finirebbe dopo il
 * percorso, fuori dallo schermo. Il percorso è quello vero (`/storage/emulated/0/...` compreso),
 * come nella riga 'Cartella' di 'Info dettagliate sul file', così si può ricopiare in un gestore di
 * file. Il nome si ricava dal percorso, per non avere due dati che possono divergere.
 */
@Composable
private fun Line(path: String) {
    val file = File(path)
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(text = file.name, style = MaterialTheme.typography.bodyLarge)
        Detail(text = file.parent ?: path)
    }
}

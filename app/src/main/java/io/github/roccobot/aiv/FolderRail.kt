package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * L'elenco delle cartelle a colonna, per il layout tablet.
 *
 * ⚠️⚠️ **NASCE CON LA `3.32`**: il mockup (`publish/tablet.html`, rotte `folders` / `grid`)
 * tiene l'elenco a lato e il contenuto della cartella nello spazio principale. Sul
 * telefono restano [FolderScreen] a pieno schermo e poi [GridScreen]; da 600 dp in su
 * ([Adaptive.sideAvailable]) le due cose restano insieme.
 * ⚠️ **Non sostituisce la casa sul telefono**: niente intestazione che si chiude, niente
 * griglia di copertine. Qui servono i nomi, la cartella scelta e un passaggio alle
 * impostazioni, come nel mockup.
 * ⚠️ **La vista ad albero resta fuori** (si decide nel chiamante): la sua navigazione è
 * un percorso a sé, e il mockup le dà una rotta propria (`system`).
 */
@Composable
fun FolderRail(
    buckets: List<Folder.Bucket>?,
    selected: Long?,
    selection: FolderSelection,
    peeking: Boolean,
    colour: FolderColour,
    tints: Map<Long, Int>,
    onPick: (Folder.Bucket) -> Unit,
    onRead: (Boolean) -> Unit,
    onSearch: () -> Unit,
    onBin: () -> Unit,
    onSettings: () -> Unit,
    width: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(Folder.granted(context)) }
    LaunchedEffect(granted) { onRead(granted) }

    val folders = buckets?.filter { selection.visible(it.path, peeking) }

    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        /*
         * ⚠️ **Icona multi-cartella al posto del titolo** (giro 3.37, `3.31-01` B):
         * 'Cartelle' andava a capo e cambiava lunghezza per lingua. Più aria fra i tasti.
         */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = stringResource(R.string.folders_title),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 4.dp, end = 8.dp)
                    .size(28.dp)
            )
            Box(Modifier.weight(1f))
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.hub_search),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onBin) {
                Icon(
                    imageVector = Glyphs.Bin,
                    contentDescription = stringResource(R.string.bin_title),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.hub_settings),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        HorizontalDivider()
        when {
            !granted -> Text(
                text = stringResource(R.string.folders_permission),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
            folders == null -> Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(Modifier.size(28.dp))
            }
            folders.isEmpty() -> Text(
                text = stringResource(R.string.folders_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(folders, key = { it.id }) { bucket ->
                    val chosen = bucket.id == selected
                    val tinta = frontTintOf(tints[bucket.id])
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (chosen) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { onPick(bucket) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = when {
                                colour == FolderColour.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
                                tinta != null -> tinta
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = bucket.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (chosen) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Il riquadro destro vuoto: nessuna cartella ancora scelta.
 *
 * ⚠️ Solo sul tablet, quando la casa è a due colonne e non si è ancora toccata una
 * cartella. Sul telefono questa situazione non esiste: si apre la griglia subito.
 */
@Composable
fun FoldersTabletHint(modifier: Modifier = Modifier) {
    // ⚠️ **Stesso senso dell'intestazione mobile** (giro 3.37, `3.31-01` A): non uno
    // spazio vuoto. Titolo + invito a scegliere una cartella, centrati.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = stringResource(R.string.folders_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = stringResource(R.string.folders_tablet_pick),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * Riga a due colonne per Cartelle + contenuto, allineata al mockup.
 *
 * ⚠️ **Il lato segue [Hand]** come nel Visualizzatore: destri -> elenco a sinistra;
 * mancini -> elenco a destra.
 */
@Composable
fun FoldersTabletSplit(
    panelOnStart: Boolean,
    rail: @Composable () -> Unit,
    detail: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize()) {
        if (panelOnStart) {
            rail()
        }
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            detail()
        }
        if (!panelOnStart) {
            rail()
        }
    }
}

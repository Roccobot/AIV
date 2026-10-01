package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Il pannello laterale delle informazioni nel Visualizzatore, sul tablet.
 *
 * ⚠️⚠️ **NASCE CON LA `3.31`**: il mockup (`publish/tablet.html`, rotta `viewer`) mette
 * l'immagine al centro e le informazioni a lato, richiudibili. Sul telefono resta la
 * bottomsheet di [FactsDialog]; qui la stessa lettura di [factsOf] / [FileFacts] vive in
 * colonna, senza coprire il media.
 * ⚠️ **Chiuso di serie sotto i 1.024 dp** ([Adaptive.sideDefaultOpen]): la soglia e il
 * perché vivono là. ⚠️ **Il lato segue [Hand]**: destri tengono il pannello a sinistra
 * (come il mockup senza classe `left`), mancini a destra (classe `left`, order 2).
 * ⚠️ **Lo scorrimento vive QUI e non dentro [FileFacts]**: è la stessa regola della
 * bottomsheet (vedi KDoc di [FileFacts]): due scorrimenti annidati fanno crash.
 */
@Composable
fun ViewerFactsSide(
    uri: Uri?,
    fields: List<FactField>,
    width: Dp,
    onClose: () -> Unit,
    onRename: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uris = uri?.let { listOf(it) }.orEmpty()
    val facts by produceState<Facts?>(null, uris) {
        value = if (uris.isEmpty()) null else factsOf(context, uris)
    }

    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.facts_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.pick_close),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        HorizontalDivider()
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val f = facts
            when {
                uri == null || f == null -> Text(
                    text = stringResource(R.string.pick_counting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                f.one == null -> Text(
                    text = stringResource(R.string.pick_counting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                else -> FileFacts(facts = f, one = f.one, fields = fields)
            }
        }
        facts?.one?.name?.let { name ->
            HorizontalDivider()
            NamePill(
                name = name,
                onRename = { uri?.let(onRename) },
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

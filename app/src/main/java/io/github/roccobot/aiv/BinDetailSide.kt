package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dettagli del file scelto nel cestino, a colonna sul tablet.
 *
 * ⚠️⚠️ **NASCE CON LA `3.35`**: il mockup (`bin`) vuole anteprime e dettagli leggibili
 * insieme. Sul telefono resta la sola griglia; da 600 dp in su ([Adaptive.sideAvailable])
 * questa colonna mostra nome e fatti del file evidenziato.
 */
@Composable
fun BinDetailSide(
    uri: Uri?,
    fields: List<FactField>,
    width: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val facts by produceState<Facts?>(initialValue = null, uri) {
        value = if (uri == null) null
        else withContext(Dispatchers.IO) { factsOf(context, listOf(uri)) }
    }

    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.bin_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        when {
            uri == null -> Text(
                text = stringResource(R.string.bin_tablet_pick),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            facts == null -> CircularProgressIndicator(
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.CenterHorizontally)
            )
            else -> {
                val one = facts!!.one
                if (one != null) {
                    Text(
                        text = one.name ?: (uri.lastPathSegment ?: uri.toString()),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    FileFacts(facts = facts!!, one = one, fields = fields)
                } else {
                    Text(
                        text = uri.lastPathSegment ?: uri.toString(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

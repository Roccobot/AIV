package io.github.roccobot.aiv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * La pagina **'Filigrana'**: il file, l'interruttore, dove cade e quanto è grande.
 *
 * ⚠️⚠️ **È UNA SOTTO-PAGINA PERCHÉ LA FAMIGLIA HA SUPERATO LA SOGLIA** (`CLAUDE.md`, § '⚙️ Dove va
 * un'impostazione, e chi la deve trovare'): la domanda è una sola, *che logo scrivo sulle immagini
 * che salvo*, e le voci sono quattro più l'anteprima, cioè oltre il *2-3* della soglia dell'utente.
 * Vive dentro 'Editor e salvataggio' perché quella pagina risponde a *che cosa succede quando
 * modifico una fotografia*.
 *
 * ⚠️⚠️ **L'ANTEPRIMA NON È UN ORNAMENTO: SENZA DI LEI I DUE NUMERI SI SCEGLIEREBBERO ALLA CIECA.**
 * Posizione e dimensione si vedono sul file salvato, cioè dopo, e provarle vorrebbe dire salvare
 * un'immagine per ogni tentativo. Qui il riquadro usa **gli stessi due numeri** del disegno vero
 * ([Watermark.Size.share] e [Watermark.AIR]), quindi quello che si vede è quello che si avrà.
 */
@Composable
fun MarkPage(settings: Settings, onChange: (Settings) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    /*
     * ⚠️ **Il contatore è quello che fa rileggere il disco**, e non un capriccio: il file scelto
     * vive in `filesDir`, cioè fuori dallo stato di Compose, quindi dopo averlo adottato o tolto
     * niente direbbe a questa pagina di guardare di nuovo. Lo stesso schema degli stili.
     */
    var giro by remember { mutableIntStateOf(0) }
    val kind by produceState<Watermark.Kind?>(null, giro) {
        value = withContext(Dispatchers.IO) { Watermark.file(context)?.let(Watermark::kindOf) }
    }

    val lettura = remember { ActivityResultContracts.OpenDocument() }
    val scegli = rememberLauncherForActivityResult(lettura) { da ->
        if (da == null) return@rememberLauncherForActivityResult
        scope.launch {
            val fatto = Watermark.adopt(context, da)
            giro++
            if (!fatto) Notices.say(context.getString(R.string.settings_mark_bad))
        }
    }

    val label = stringResource(R.string.settings_mark)
    val desc = stringResource(R.string.settings_mark_desc)
    Searchable(label, desc) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
            Detail(desc)
        }
        /*
         * ⚠️⚠️ **I TIPI SI DICHIARANO AL SELETTORE, ED È LA SUA SPECIFICA** (*Input PNG o SVG*):
         * così il navigatore di sistema mostra i soli file che si possono usare, invece di
         * lasciar scegliere un JPEG e rispondere di no dopo. ⚠️ **Il controllo vero resta sui
         * byte** (vedi [Watermark.adopt]): un fornitore di documenti può dichiarare quello che
         * vuole, e chi sceglie 'tutti i file' arriva qui lo stesso.
         */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = kind?.suffix?.uppercase() ?: stringResource(R.string.settings_mark_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (kind != null) {
                TextButton(onClick = {
                    Watermark.forget(context)
                    giro++
                }) { Text(stringResource(R.string.settings_mark_clear)) }
            }
            TextButton(onClick = { scegli.launch(arrayOf(PNG_MIME, SVG_MIME)) }) {
                Text(stringResource(R.string.settings_editor_pick))
            }
        }
    }

    /*
     * ⚠️ **L'anteprima c'è solo quando c'è una filigrana**: un riquadro vuoto non direbbe niente
     * a chi non ne ha ancora scelta una, e il posto in cui si dice che non c'è è la riga qui
     * sopra.
     */
    if (kind != null) MarkPreview(settings)

    SwitchRow(
        label = stringResource(R.string.settings_mark_on),
        detail = stringResource(R.string.settings_mark_on_desc),
        checked = settings.markOn,
        onChange = { onChange(settings.copy(markOn = it)) }
    )

    Choices(
        label = stringResource(R.string.settings_mark_where),
        detail = null,
        options = Watermark.Spot.entries,
        selected = settings.markSpot,
        nameOf = {
            stringResource(
                when (it) {
                    Watermark.Spot.TOP_LEFT -> R.string.mark_spot_tl
                    Watermark.Spot.TOP_RIGHT -> R.string.mark_spot_tr
                    Watermark.Spot.BOTTOM_LEFT -> R.string.mark_spot_bl
                    Watermark.Spot.BOTTOM_RIGHT -> R.string.mark_spot_br
                    Watermark.Spot.CENTRE -> R.string.mark_spot_c
                }
            )
        },
        onSelect = { onChange(settings.copy(markSpot = it)) }
    )

    Choices(
        label = stringResource(R.string.settings_mark_size),
        detail = null,
        options = Watermark.Size.entries,
        selected = settings.markSize,
        nameOf = {
            stringResource(
                when (it) {
                    Watermark.Size.SMALL -> R.string.mark_size_s
                    Watermark.Size.MEDIUM -> R.string.mark_size_m
                    Watermark.Size.LARGE -> R.string.mark_size_l
                    Watermark.Size.HUGE -> R.string.mark_size_xl
                }
            )
        },
        onSelect = { onChange(settings.copy(markSize = it)) }
    )
}

/**
 * Il riquadro che mostra dove la filigrana cadrà, e quanto sarà grande.
 *
 * ⚠️⚠️ **LA MISURA SI RICAVA DAL LATO LUNGO DEL RIQUADRO, ESATTAMENTE COME SULL'IMMAGINE VERA**:
 * il disegno entra in un quadrato di lato `share` per il lato lungo, e il margine vale
 * [Watermark.AIR] dello stesso lato. Sono i due numeri del salvataggio, letti di là invece di
 * essere riscritti qui.
 * ⚠️ **Il rapporto è 3:2**, cioè quello di una fotografia: un riquadro quadrato direbbe una
 * proporzione che quasi nessuna immagine ha.
 */
@Composable
private fun MarkPreview(settings: Settings) {
    val context = LocalContext.current
    val art by produceState<ImageBitmap?>(null, settings.markSize) {
        value = withContext(Dispatchers.IO) {
            Watermark.artwork(context, PREVIEW_ART)?.asImageBitmap()
        }
    }
    val disegno = art ?: return
    val descrizione = stringResource(R.string.settings_mark_preview)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .aspectRatio(3f / 2f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription = descrizione }
    ) {
        val lungo = maxWidth
        Image(
            bitmap = disegno,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(
                    when (settings.markSpot) {
                        Watermark.Spot.TOP_LEFT -> Alignment.TopStart
                        Watermark.Spot.TOP_RIGHT -> Alignment.TopEnd
                        Watermark.Spot.BOTTOM_LEFT -> Alignment.BottomStart
                        Watermark.Spot.BOTTOM_RIGHT -> Alignment.BottomEnd
                        Watermark.Spot.CENTRE -> Alignment.Center
                    }
                )
                // ⚠️ Al centro non c'è nessun bordo da cui stare lontani, come nel disegno vero.
                .padding(
                    if (settings.markSpot == Watermark.Spot.CENTRE) 0.dp
                    else lungo * Watermark.AIR
                )
                .sizeIn(
                    maxWidth = lungo * settings.markSize.share,
                    maxHeight = lungo * settings.markSize.share
                )
        )
    }
}

/** Il lato lungo a cui si disegna la filigrana per l'anteprima. */
private const val PREVIEW_ART = 512

/** I due tipi che il selettore di sistema mostra: vedi [Watermark.Kind]. */
private const val PNG_MIME = "image/png"
private const val SVG_MIME = "image/svg+xml"

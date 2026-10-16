package io.github.roccobot.aiv

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.roundToInt

/**
 * 'Converti/Esporta': salvare l'immagine che si sta guardando in un altro formato.
 *
 * ⚠️ **Due metà, e la divisione la impone Android**: sopra i formati che il sistema sa scrivere
 * (vedi [Convert]), sotto i servizi esterni per tutto il resto. I pixel che escono si leggono prima
 * di esportare, perché chi riduce lo fa per il peso.
 *
 * @param onSaved il file convertito è stato scritto: fa rileggere la cartella aperta dietro al
 *   visualizzatore, che altrimenti resterebbe con l'elenco di prima.
 */
@Composable
fun ConvertDialog(
    image: LoadedImage,
    source: Uri?,
    /**
     * Il fotogramma da esportare al posto del file, `null` per un'immagine ferma: questo dialogo è
     * anche l'esportazione del fotogramma corrente, con formato, qualità e riduzione. Il fotogramma
     * lo prende chi apre il dialogo (vedi `ViewerScreen`), perché fra il tocco e la scelta della
     * cartella passano secondi.
     */
    frame: android.graphics.Bitmap? = null,
    /** Quale fotogramma è, per dirlo nel dialogo. Vale solo insieme a [frame]. */
    shownFrame: Int = 0,
    onSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // Le frasi degli avvisi da `LocalResources`, come nella griglia: il perché è là.
    val res = LocalResources.current
    val scope = rememberCoroutineScope()

    var target by remember { mutableStateOf(Convert.Target.JPEG) }
    var size by remember { mutableStateOf(Convert.Size.FULL) }
    var quality by remember { mutableIntStateOf(92) }

    /*
     * Lo stato della pulizia SVG: l'esito da mostrare e il segnale che spegne il tasto mentre
     * lavora (vedi [SvgClean]). La copia nel cestino si legge dalle impostazioni: è lo stesso
     * interruttore dell'editor ('Copia nel cestino prima di sovrascrivere'), perché la domanda è la
     * stessa.
     */
    var esito by remember { mutableStateOf<SvgClean.Result?>(null) }
    var pulendo by remember { mutableStateOf(false) }
    val backup by produceState(true) {
        SettingsStore.flow(context).collect { value = it.editorBackup }
    }

    /*
     * Un selettore di cartella suo e non quello di 'Scarica', che scrive il file originale col tipo
     * della sorgente: qui si scrive un file nuovo, nel formato scelto.
     * ⚠️ **Il contratto si ricorda per tipo**: `rememberLauncherForActivityResult` tiene il
     * contratto fra le chiavi del suo effetto, e un `CreateDocument(...)` nuovo a ogni
     * ricomposizione registrerebbe il selettore ogni volta.
     */
    val contract = remember(target.mime) { ActivityResultContracts.CreateDocument(target.mime) }
    val exporter = rememberLauncherForActivityResult(contract) { destination ->
        if (destination == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = Convert.write(
                context = context,
                /*
                 * ⚠️ **Con un fotogramma in mano l'indirizzo è `null`**: `Convert.write` preferisce
                 * sempre il file, e rileggerlo esporterebbe il primo fotogramma della GIF. Senza
                 * indirizzo resta il ripiego, cioè il fotogramma.
                 */
                source = if (frame != null) null else source,
                fallback = frame
                    ?: image.bitmap.let { runCatching { it.asAndroidBitmap() }.getOrNull() },
                target = target,
                quality = quality,
                size = size,
                destination = destination
            )
            Notices.say(res.getString(
                if (ok) R.string.toast_saved else R.string.toast_save_failed
            ))
            if (ok) onSaved()
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(onDismiss),
        properties = loweredWindow(onDismiss),
        title = { Text(stringResource(R.string.menu_convert)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                /*
                 * Con un'animazione davanti il dialogo dice che lavora sul fotogramma, o 'Converti'
                 * sembrerebbe riferito al file intero. Su un'immagine ferma la riga non c'è.
                 */
                if (frame != null) {
                    Note(stringResource(R.string.convert_this_frame, shownFrame))
                }
                Heading(stringResource(R.string.convert_format))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.oneOf()
                ) {
                    Convert.Target.entries.filter { it.offered }.forEach { option ->
                        FilterChip(
                            selected = option == target,
                            onClick = { target = option },
                            label = { Text(stringResource(option.label)) },
                            modifier = Modifier.picked(option == target)
                        )
                    }
                }

                // Il cursore compare solo per i formati con perdita: altrove la qualità non ha
                // effetto.
                if (target.lossy) {
                    /*
                     * Titolo e percentuale vengono dalle risorse, così il segno di percentuale e la
                     * sua posizione seguono la lingua, e con `%1$d` `getString` scrive le cifre
                     * nella numerazione della lingua (arabo, bengalese). I tre spazi sono nella
                     * stringa, come ` `, perché l'XML li collassa.
                     */
                    Heading(stringResource(R.string.convert_quality_at, quality))
                    ValueSlider(
                        value = quality.toFloat(),
                        onValueChange = { quality = it.roundToInt() },
                        valueRange = QUALITY_MIN..QUALITY_MAX,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Heading(stringResource(R.string.convert_size))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.oneOf()
                ) {
                    Convert.Size.entries.forEach { option ->
                        FilterChip(
                            selected = option == size,
                            onClick = { size = option },
                            label = { Text(stringResource(R.string.convert_percent, option.percent)) },
                            modifier = Modifier.picked(option == size)
                        )
                    }
                }
                Note(
                    stringResource(
                        R.string.convert_pixels,
                        size.applyTo(image.pixelWidth),
                        size.applyTo(image.pixelHeight)
                    )
                )

                if (!target.keepsAlpha) {
                    Note(stringResource(R.string.convert_no_alpha))
                }

                /*
                 * 'Pulisci' compare solo su un SVG (richiesta dell'utente): toglie il contorno di
                 * un documento vettoriale, che in un'immagine a punti non c'è. È fra l'esportazione
                 * e 'Altri formati', come chiesto. I tre testi sono suoi, numeri compresi (*fino al
                 * 99%*, *intatta al 100%*).
                 * ⚠️ **Non in AIV Play** (`Store.files`): la pulizia riscrive l'SVG stesso, e
                 * quella variante non scrive sui file.
                 */
                if (isVector(image) && Store.files) {
                    HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                    Heading(stringResource(R.string.convert_clean))
                    Note(stringResource(R.string.convert_clean_why))
                    Note(stringResource(R.string.convert_clean_keeps), strong = true)
                    Note(stringResource(R.string.convert_clean_c2pa))
                    /*
                     * L'esito è sopra i tasti e resta scritto: confronta due pesi, che non si
                     * leggono in un avviso che passa, e dice anche quando non c'era niente da
                     * togliere.
                     */
                    esito?.let { fatto ->
                        Note(
                            when (fatto) {
                                is SvgClean.Result.Done -> stringResource(
                                    R.string.clean_done,
                                    formatBytes(fatto.before),
                                    formatBytes(fatto.after)
                                )
                                SvgClean.Result.Nothing -> stringResource(R.string.clean_nothing)
                                is SvgClean.Result.Failed -> stringResource(fatto.why)
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        /*
                         * Un tasto solo: l'utente ha tolto il pulitore Web (`dom-pulitore-web`),
                         * perché il selettore di file di Android non mostra le anteprime. ⚠️ Non
                         * erano due vie per la stessa cosa: CleanSVG con SVGO riscrive anche i
                         * tracciati, la pulizia di casa no. Il tasto si spegne senza un file su cui
                         * lavorare: un SVG arrivato dal web non ha niente da riscrivere.
                         */
                        TextButton(
                            enabled = source != null && !pulendo,
                            onClick = {
                                val uri = source ?: return@TextButton
                                pulendo = true
                                scope.launch {
                                    val fatto = SvgClean.clean(context, uri, backup)
                                    esito = fatto
                                    pulendo = false
                                    if (fatto is SvgClean.Result.Done) onSaved()
                                }
                            }
                        ) { Text(stringResource(R.string.clean_now)) }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                Heading(stringResource(R.string.convert_elsewhere))
                Note(stringResource(R.string.convert_elsewhere_desc))
                Convert.Service.entries.forEach { service ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(service.label),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Note(stringResource(service.why))
                        }
                        TextButton(onClick = { open(context, service.url) }) {
                            Text(stringResource(R.string.convert_open))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                /*
                 * Il nome proposto contiene il numero del fotogramma, o esportandone tre da una GIF
                 * si proporrebbe tre volte lo stesso nome.
                 */
                onClick = {
                    exporter.launch(
                        if (frame != null) Convert.frameName(image.displayName, shownFrame, target)
                        else Convert.nameFor(image.displayName, target)
                    )
                }
            ) {
                Text(stringResource(R.string.convert_export))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun Heading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 10.dp)
    )
}

/**
 * @param strong se la riga va in grassetto e col colore pieno del testo. Serve alla sola riga che
 *   l'utente ha marcato come la promessa del pulitore (riscontro `pulitore-web-via`).
 *
 * ⚠️ **Il grassetto è sulla riga intera e non sulle sole frasi marcate**: `stringResource` scarta
 * la marcatura dentro la stringa, e accenderla a metà frase vorrebbe un sistema a mano in 28
 * lingue. Il resto della riga è l'elenco di quello che le due frasi promettono.
 */
@Composable
private fun Note(text: String, strong: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (strong) FontWeight.SemiBold else null,
        color = if (strong) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Apre un indirizzo nel browser. Un fallimento non si segnala: succede solo su un telefono senza
 * nessun browser.
 */
private fun open(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** Qualità minima e massima del cursore, in centesimi. */
private const val QUALITY_MIN = 10f
private const val QUALITY_MAX = 100f

/**
 * Se quello che si sta guardando è un documento vettoriale.
 *
 * Due prove: il tipo dichiarato, e il nome, perché un `content://` può servire un SVG come
 * `application/octet-stream`. I byte qui non si possono guardare: l'immagine è già decodificata.
 */
private fun isVector(image: LoadedImage): Boolean {
    if (image.mimeType == Svg.MIME) return true
    val name = image.displayName?.lowercase() ?: return false
    return name.endsWith(".svg") || name.endsWith(".svgz")
}

package io.github.roccobot.aiv

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material3.Slider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.roundToInt

/**
 * 'Converti/Esporta': salvare l'immagine che si sta guardando in un altro formato.
 *
 * ⚠️⚠️ **DUE METÀ, E LA DIVISIONE È IMPOSTA DA ANDROID, non da una scelta di disegno**: sopra
 * ci sono i quattro formati che il sistema sa **scrivere** (vedi [Convert]), sotto i servizi
 * di fuori per tutto il resto. Un dialogo unico che elencasse dieci formati e ne facesse
 * quattro sarebbe una promessa rotta a metà elenco.
 * ⚠️ **I pixel che escono si leggono PRIMA di esportare**: la percentuale da sola non dice
 * quanto sarà grande il file, e chi riduce lo fa proprio per quello.
 *
 * @param onSaved il file convertito è stato scritto. ⚠️ Serve a far rileggere la cartella
 *   aperta dietro al visualizzatore, che altrimenti resterebbe con la lista di prima: vedi
 *   `AnimatedBar`, dove il difetto si è visto per la prima volta.
 */
@Composable
fun ConvertDialog(
    image: LoadedImage,
    source: Uri?,
    /**
     * Il fotogramma da esportare al posto del file, e `null` per ogni fotografia ferma.
     *
     * ⚠️⚠️ **DALLA 1.21 QUESTO DIALOGO È ANCHE L'ESPORTAZIONE DEL FOTOGRAMMA** (richiesta
     * dell'utente, 2026-09-01: *si può togliere del tutto quel tasto e usare direttamente la
     * funzionalità 'Esporta' del menu a pressione lunga, che agisce sul fotogramma
     * corrente*). Prima era un tasto a sé nella fila dei comandi, che scriveva PNG e basta;
     * qui si eredita la scelta del formato, la qualità e la riduzione, che quel tasto non
     * offriva.
     * ⚠️ **Il fotogramma lo prende chi apre il dialogo**, non questo dialogo: fra il tocco e
     * la scelta della cartella passano secondi. Vedi `ViewerScreen`.
     */
    frame: android.graphics.Bitmap? = null,
    /** Quale fotogramma è, per dirlo nel dialogo. Vale solo insieme a [frame]. */
    shownFrame: Int = 0,
    onSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var target by remember { mutableStateOf(Convert.Target.JPEG) }
    var size by remember { mutableStateOf(Convert.Size.FULL) }
    var quality by remember { mutableIntStateOf(92) }

    /*
     * ⚠️ Lo stato della pulizia SVG: l'esito da mostrare e la bandierina che spegne il tasto
     * mentre lavora. Vedi [SvgClean], e la nota sui due tasti più sotto.
     * ⚠️ **Il backup si legge dalle impostazioni e non si chiede qui**: è lo stesso
     * interruttore dell'editor ('Copia nel cestino prima di sovrascrivere'), perché la
     * domanda è la stessa e due interruttori per una domanda sola sono due posti in cui
     * dimenticarsene.
     */
    var esito by remember { mutableStateOf<SvgClean.Result?>(null) }
    var pulendo by remember { mutableStateOf(false) }
    val backup by produceState(true) {
        SettingsStore.flow(context).collect { value = it.editorBackup }
    }

    /*
     * ⚠️ **Il selettore di cartella è suo e non quello di 'Scarica'**: quello scrive il file
     * originale byte per byte e dichiara il tipo della sorgente; qui si scrive un file
     * **nuovo**, in un formato che l'utente ha appena scelto. Due contenuti diversi vogliono
     * due contratti diversi.
     */
    /*
     * ⚠️⚠️ **IL CONTRATTO SI RICORDA PER TIPO, e senza `remember` questa riga si
     * ri-registrerebbe a OGNI ricomposizione**: `rememberLauncherForActivityResult` tiene il
     * contratto fra le chiavi del suo effetto, e `CreateDocument(...)` scritto qui dentro è un
     * oggetto nuovo ogni volta. Con la chiave sul tipo, cambia solo quando cambia il formato,
     * che è esattamente quando deve.
     */
    val contract = remember(target.mime) { ActivityResultContracts.CreateDocument(target.mime) }
    val exporter = rememberLauncherForActivityResult(contract) { destination ->
        if (destination == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = Convert.write(
                context = context,
                /*
                 * ⚠️⚠️ **CON UN FOTOGRAMMA IN MANO L'INDIRIZZO SI PASSA `null`, ed è
                 * l'inversione che fa funzionare la cosa**: `Convert.write` preferisce
                 * sempre il file, e rileggerlo qui vorrebbe dire esportare il **primo**
                 * fotogramma della GIF invece di quello che si sta guardando. Lasciandolo
                 * senza indirizzo, gli resta solo il ripiego, che è appunto il fotogramma.
                 */
                source = if (frame != null) null else source,
                fallback = frame
                    ?: image.bitmap.let { runCatching { it.asAndroidBitmap() }.getOrNull() },
                target = target,
                quality = quality,
                size = size,
                destination = destination
            )
            Toast.makeText(
                context,
                if (ok) R.string.toast_saved else R.string.toast_save_failed,
                Toast.LENGTH_SHORT
            ).show()
            if (ok) onSaved()
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(),
        title = { Text(stringResource(R.string.menu_convert)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                /*
                 * ⚠️ **Il dialogo dice su CHE COSA sta lavorando quando non è ovvio**: con
                 * un'animazione davanti, 'Converti' potrebbe significare il file intero, e
                 * chi lo credesse si troverebbe un PNG da un fotogramma senza sapere perché.
                 * Su una fotografia ferma la riga non c'è, perché non c'è ambiguità.
                 */
                if (frame != null) {
                    Note(stringResource(R.string.convert_this_frame, shownFrame))
                }
                Heading(stringResource(R.string.convert_format))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Convert.Target.entries.forEach { option ->
                        FilterChip(
                            selected = option == target,
                            onClick = { target = option },
                            label = { Text(stringResource(option.label)) }
                        )
                    }
                }

                // ⚠️ Il cursore compare solo dove serve: sul senza perdita la qualità non ha
                // nessun effetto, e lasciarlo in scena spento sarebbe un comando che mente.
                if (target.lossy) {
                    Heading(
                        stringResource(R.string.convert_quality) + "   " + quality + "%"
                    )
                    Slider(
                        value = quality.toFloat(),
                        onValueChange = { quality = it.roundToInt() },
                        valueRange = QUALITY_MIN..QUALITY_MAX,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Heading(stringResource(R.string.convert_size))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Convert.Size.entries.forEach { option ->
                        FilterChip(
                            selected = option == size,
                            onClick = { size = option },
                            label = { Text("${option.percent}%") }
                        )
                    }
                }
                Note(
                    "${size.applyTo(image.pixelWidth)} x ${size.applyTo(image.pixelHeight)}"
                )

                if (!target.keepsAlpha) {
                    Note(stringResource(R.string.convert_no_alpha))
                }

                /*
                 * ⚠️⚠️ **'PULISCI' COMPARE SOLO SU UN SVG, ed è una richiesta dell'utente**
                 * (2026-09-02: *se sto visualizzando un SVG, voglio che il menu
                 * 'Esporta/Converti' includa anche un 'Pulisci' prima di 'Altri formati'*). Su
                 * una fotografia non avrebbe senso: CleanSVG toglie i metadati da un
                 * **documento vettoriale**, e su un JPEG non c'è niente da ripulire con lui.
                 * ⚠️ **Prima di 'Altri formati' e dopo l'esportazione**, come chiesto: quello
                 * che si fa più spesso resta in cima, e questa è una via di mezzo fra il
                 * convertire (sopra) e il mandare fuori (sotto).
                 * ⚠️⚠️ **PORTA FUORI E NON MANDA IL FILE, e la differenza va detta**: CleanSVG
                 * è una pagina web che lavora **nel browser**, e per riceverne un file
                 * vorrebbe un intento di condivisione che una pagina non può dichiarare.
                 * Quindi qui si apre l'indirizzo, e il file lo si sceglie là. È lo stesso
                 * contratto dei tre servizi qui sotto.
                 * ⚠️ **Il testo è suo, parola per parola**, numeri compresi (*fino al 99%*):
                 * quella è la misura che ha fatto lui sul suo strumento, non una mia stima.
                 */
                if (isVector(image)) {
                    HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                    Text(
                        text = stringResource(R.string.convert_clean),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                    Note(stringResource(R.string.convert_clean_why))
                    Note(stringResource(R.string.convert_clean_c2pa))
                    /*
                     * ⚠️ **L'esito sta SOPRA i tasti e resta scritto**, invece di essere un
                     * avviso che passa: dice due pesi, e due numeri che si vogliono
                     * confrontare non si leggono in tre secondi. È anche il solo posto in cui
                     * si vede che non c'era niente da togliere, che è un'informazione e non un
                     * errore.
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
                         * ⚠️⚠️ **DUE TASTI E NON UNO, e la differenza è dichiarata nel testo
                         * sopra**: 'Pulisci qui' lavora **in casa e senza rete** e non riscrive
                         * i tracciati; 'Apri' porta a CleanSVG, che con SVGO li riscrive. Sono
                         * due lavori diversi sullo stesso file, non due vie per lo stesso.
                         * ⚠️ **Si spegne senza un file su cui lavorare**: la pulizia riscrive
                         * un file del telefono, quindi su un SVG arrivato dal web non c'è
                         * niente da riscrivere. CleanSVG invece resta acceso: là il file lo
                         * sceglie l'utente.
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
                        TextButton(onClick = { open(context, CLEANSVG) }) {
                            Text(stringResource(R.string.convert_open))
                        }
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
                 * ⚠️ **Il nome proposto porta il numero del fotogramma**, o esportandone
                 * tre da una GIF si proporrebbe tre volte lo stesso nome, e il selettore di
                 * sistema aggiungerebbe '(1)' e '(2)' senza dire di che si tratta.
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

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Apre un indirizzo nel browser.
 *
 * ⚠️ **Un fallimento non si racconta**: succede solo su un telefono senza nessun browser, e
 * un avviso su quel caso sarebbe un testo in 28 lingue che non leggerà nessuno.
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

/** La paginetta dell'utente che ripulisce un SVG, dallo stesso sito che serve l'APK. */
private const val CLEANSVG = "https://roccobot.github.io/CleanSVG/"

/**
 * Se quello che si sta guardando è un documento **vettoriale**.
 *
 * ⚠️ **Due prove e non una**: il tipo dichiarato è quello giusto quando c'è, ma un `content://`
 * può servire un SVG come `application/octet-stream` (succede coi file arrivati da fuori), e
 * allora l'unica traccia è il nome. È la stessa coppia di prove che usa il riconoscimento delle
 * miniature, dove i byte però si possono guardare e qui no: qui l'immagine è già decodificata.
 */
private fun isVector(image: LoadedImage): Boolean {
    if (image.mimeType == Svg.MIME) return true
    val name = image.displayName?.lowercase() ?: return false
    return name.endsWith(".svg") || name.endsWith(".svgz")
}

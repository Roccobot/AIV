package io.github.roccobot.aiv

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * The settings screen.
 *
 * Five of the nine carry a description and the rest do not, which is a choice
 * rather than an omission: an explanation under a setting whose name already says
 * everything is noise, and after three of them nobody reads the fourth. The ones
 * that have one are the ones where the name cannot carry the meaning: what the
 * checkerboard is FOR, what 'enlarge' does when it is off, what 100% means, what the
 * reverse reading order is the reverse OF, and what exactly opens at startup.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onStartFolder: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back)
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Spacer(Modifier.height(8.dp))

        Choices(
            label = stringResource(R.string.settings_background),
            detail = stringResource(R.string.settings_bg_desc),
            options = BgType.entries,
            selected = settings.bgType,
            nameOf = {
                stringResource(
                    when (it) {
                        BgType.CHECKER -> R.string.settings_bg_checker
                        BgType.SOLID -> R.string.settings_bg_solid
                    }
                )
            },
            onSelect = { onChange(settings.copy(bgType = it)) }
        )

        Choices(
            label = stringResource(R.string.settings_bg_theme),
            detail = null,
            options = BgTheme.entries,
            selected = settings.bgTheme,
            nameOf = {
                stringResource(
                    when (it) {
                        BgTheme.AUTO -> R.string.settings_auto
                        BgTheme.LIGHT -> R.string.settings_light
                        BgTheme.DARK -> R.string.settings_dark
                    }
                )
            },
            onSelect = { onChange(settings.copy(bgTheme = it)) }
        )

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        SwitchRow(
            label = stringResource(R.string.settings_fit_grow),
            detail = stringResource(R.string.settings_fit_grow_desc),
            checked = settings.fitGrow,
            onChange = { onChange(settings.copy(fitGrow = it)) }
        )

        Text(
            text = stringResource(R.string.settings_zoom_max) + "   " +
                settings.zoomMax.roundToInt() + "x",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp)
        )
        Slider(
            value = settings.zoomMax,
            onValueChange = { onChange(settings.copy(zoomMax = it.roundToInt().toFloat())) },
            valueRange = SettingsStore.ZOOM_MAX_MIN..SettingsStore.ZOOM_MAX_MAX,
            modifier = Modifier.fillMaxWidth()
        )

        Choices(
            label = stringResource(R.string.settings_scale_mode),
            detail = stringResource(R.string.settings_scale_desc),
            options = ScaleMode.entries,
            selected = settings.scaleMode,
            nameOf = {
                stringResource(
                    when (it) {
                        ScaleMode.PHYSICAL -> R.string.settings_scale_physical
                        ScaleMode.LOGICAL -> R.string.settings_scale_logical
                    }
                )
            },
            onSelect = { onChange(settings.copy(scaleMode = it)) }
        )

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        Choices(
            label = stringResource(R.string.settings_info_position),
            detail = null,
            options = InfoPosition.entries,
            selected = settings.infoPosition,
            nameOf = {
                stringResource(
                    when (it) {
                        InfoPosition.TOP -> R.string.settings_top
                        InfoPosition.BOTTOM -> R.string.settings_bottom
                    }
                )
            },
            onSelect = { onChange(settings.copy(infoPosition = it)) }
        )

        SwitchRow(
            label = stringResource(R.string.settings_info_visible),
            detail = null,
            checked = settings.infoVisible,
            onChange = { onChange(settings.copy(infoVisible = it)) }
        )

        // ⚠️ Con la ricerca immagine è uscito anche il suo motore, che era l'ultima
        // voce di questo elenco, e con lei il separatore che la staccava: un filo
        // sopra il nulla è peggio di nessun filo.

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        SwitchRow(
            label = stringResource(R.string.settings_reverse_order),
            detail = stringResource(R.string.settings_reverse_order_desc),
            checked = settings.reverseSequence,
            onChange = { onChange(settings.copy(reverseSequence = it)) }
        )

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        SwitchRow(
            label = stringResource(R.string.settings_start_folder),
            detail = stringResource(R.string.settings_start_folder_desc),
            checked = settings.openAtStart,
            // ⚠️ Acceso senza una cartella scelta porta ALL'ELENCO invece di accendersi
            // e non fare niente: un interruttore che dipende da un'altra voce e non lo
            // dice è il modo classico di far sembrare rotta un'impostazione.
            onChange = {
                if (it && settings.startFolder == null) onStartFolder()
                else onChange(settings.copy(openAtStart = it))
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = settings.startFolderName.ifBlank {
                    stringResource(R.string.settings_start_folder_none)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onStartFolder) {
                Text(stringResource(R.string.settings_start_folder_pick))
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Identity()

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * A row of chips for one choice. FlowRow and not Row: two of these carry labels a
 * sentence long, and on a narrow screen a fixed row would push them off the edge
 * instead of wrapping.
 */
@Composable
private fun <T : Choice> Choices(
    label: String,
    detail: String?,
    options: List<T>,
    selected: T,
    nameOf: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 12.dp)
    )
    detail?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(nameOf(option)) }
            )
        }
    }
}

/**
 * L'icona, il nome e da dove viene.
 *
 * ⚠️⚠️ **STAVA SULLA SCHERMATA INIZIALE, ed è arrivata qui quando quella è sparita**
 * (`0.41`): un'app senza nessun posto che dica come si chiama e di chi è non è sobria, è
 * anonima. Il fondo delle impostazioni è il posto che ogni app usa per questo, quindi non
 * si è inventato niente.
 * ⚠️ Nessuno di questi testi è una stringa di risorsa, ed è deliberato: sono un nome, una
 * firma e un dominio. Tradurre 'by Roccobot' sarebbe un errore, non una cortesia, e una
 * risorsa inviterebbe a farlo.
 */
@Composable
private fun Identity() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIcon(72.dp)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Astonishing Image Viewer",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "(AIV) by Roccobot 天",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = buildAnnotatedString {
                withLink(LinkAnnotation.Url("https://roccobot.me")) {
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) { append("roccobot.me") }
                }
            },
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Quanto si ingrandisce il livello di primo piano perché questa anteprima mostri la
 * stessa cosa che mostra il launcher.
 *
 * ⚠️⚠️ **I due fattori sono uno per ciascuna cosa che il launcher fa, e non sono
 * intercambiabili.** `108 / 72`, cioè esattamente 1.5, perché di un'icona adattiva si
 * vedono **solo i 72dp centrali** della tela da 108: l'anello esterno esiste per la
 * maschera e la parallasse del launcher, e chi rende la tela intera sta mostrando un
 * margine che sul telefono nessuno vede. Il **1.3** è l'altra metà di una coppia:
 * HyperOS ingrandisce il primo piano di circa un terzo, quindi `ic_launcher_foreground`
 * porta un glifo rimpicciolito di 1.3 per venire giusto sul telefono, e qui si
 * moltiplica per lo stesso 1.3 per rivedere il disegno com'è. Chi ne cambia uno deve
 * cambiare anche l'altro.
 */
private const val LAUNCHER_ZOOM = 1.5f * 1.3f

/**
 * L'icona del launcher, disegnata grande.
 *
 * ⚠️⚠️ **L'ingrandimento si fa al DISEGNO e non alla misura, e fino alla 0.27 questa
 * differenza costava tutto l'ingrandimento.** `Modifier.size` **negozia** col genitore:
 * `SizeNode.measure` chiama `constrain(vincoli in ingresso, misura chiesta)` quando
 * `enforceIncoming` è vero, e per `size` è vero (per `requiredSize` no), verificato nel
 * bytecode di `foundation-layout` e non a memoria. Il `Box` qui sotto passa ai figli i
 * propri vincoli col solo minimo azzerato, quindi il massimo resta la misura della
 * piastrella: l'immagine chiedeva 187.2dp, ne otteneva 96, e l'anteprima mostrava la
 * **tela intera** invece dei 72dp centrali, cioè un glifo 1.95 volte più piccolo di
 * quello del launcher. È esattamente quello che l'utente aveva visto e segnalato.
 * ⚠️ Un `Modifier.scale` è una trasformazione di disegno: nessun genitore la può
 * limitare, e il ritaglio del `Box` continua a valere.
 */
@Composable
private fun AppIcon(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 24))
            .background(colorResource(R.color.launcher_background)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().scale(LAUNCHER_ZOOM)
        )
    }
}

@Composable
private fun SwitchRow(
    label: String,
    detail: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
            detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

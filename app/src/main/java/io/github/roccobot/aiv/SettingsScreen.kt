package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.stringResource
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
            checked = settings.reverseOrder,
            onChange = { onChange(settings.copy(reverseOrder = it)) }
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

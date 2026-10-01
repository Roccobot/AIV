package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * L'indice delle impostazioni a colonna, per il layout tablet.
 *
 * ⚠️⚠️ **NASCE CON LA `3.33`**: il mockup (`publish/tablet.html`, rotta `settings`) tiene
 * l'indice a lato e la pagina scelta nello spazio principale. Sul telefono resta la
 * navigazione a pila a pieno schermo ([SettingsScreen]); da 600 dp in su
 * ([Adaptive.sideAvailable]) indice e pagina restano insieme.
 * ⚠️ **Non riscrive le pagine**: il corpo continua a viverle [SettingsScreen]; qui ci sono
 * solo i titoli per saltare da una all'altra senza tornare alla radice.
 * ⚠️ **Il lato segue [Hand]**, come Cartelle e Visualizzatore.
 */
@Composable
internal fun SettingsIndexRail(
    selected: Page,
    onPick: (Page) -> Unit,
    width: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
        )
        HorizontalDivider()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            for (page in SETTINGS_INDEX) {
                val chosen = page == selected
                Text(
                    text = stringResource(page.titleRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (chosen) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (chosen) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable { onPick(page) }
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                )
            }
        }
    }
}

/**
 * Riga a due colonne per Impostazioni: indice + pagina, allineata al mockup.
 */
@Composable
internal fun SettingsTabletSplit(
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

/**
 * L'ordine dell'indice tablet: radice, poi le famiglie come nel mockup.
 *
 * ⚠️ **ROOT per prima**: è la "Pagina iniziale" del mockup. Le altre seguono il percorso
 * contenitore -> contenuto (cartelle, visualizzatore, info, comandi, modifica, gestione).
 */
internal val SETTINGS_INDEX: List<Page> = listOf(
    Page.ROOT,
    Page.FOLDERS, Page.VIEWS, Page.HIDDEN,
    Page.VIEWER, Page.ZOOM,
    Page.INFO, Page.FACTS,
    Page.CONTROLS, Page.BUTTONS,
    Page.EDITING, Page.STYLES, Page.MARK, Page.SAVING,
    Page.THUMBS, Page.BACKUP
)

internal val Page.titleRes: Int
    get() = when (this) {
        Page.ROOT -> R.string.settings_title
        Page.FOLDERS -> R.string.settings_group_browse
        Page.VIEWER -> R.string.settings_group_viewer
        Page.INFO -> R.string.settings_page_info
        Page.CONTROLS -> R.string.settings_page_controls
        Page.EDITING -> R.string.settings_page_editing
        Page.FACTS -> R.string.settings_facts
        Page.HIDDEN -> R.string.folder_selection
        Page.ZOOM -> R.string.settings_zoom_page
        Page.VIEWS -> R.string.view_options
        Page.THUMBS -> R.string.settings_thumbs
        Page.BUTTONS -> R.string.settings_buttons
        Page.SAVING -> R.string.settings_rename_download
        Page.STYLES -> R.string.settings_styles
        Page.MARK -> R.string.settings_mark
        Page.BACKUP -> R.string.backup_title
    }

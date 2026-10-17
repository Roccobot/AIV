package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * I comandi della **barra delle info**, che vivono in due posti (impostazioni e pannellino) e
 * devono essere identici.
 *
 * ⚠️ **La scelta è disegnata una volta sola, qui, e i due posti la chiamano** (riscontro
 * `chip-colonna`, 1.38): con due disegni della stessa scelta un giro di riscontri ne aggiusta
 * uno e l'altro resta indietro, ed è successo tre volte di fila.
 *
 * L'interruttore resta fuori: nelle impostazioni è su una riga sua, nel pannellino è sulla riga
 * del titolo, allineato a destra, cioè in due contenitori diversi. Un `Switch` è una riga sola e
 * non ha niente da far divergere.
 */

/**
 * Il nome di un lato, letto una volta sola per tutti e due i posti.
 *
 * Esiste perché la ricerca delle impostazioni deve poter filtrare su 'In alto' e 'In basso'
 * senza un secondo `when` delle stesse due voci.
 */
@Composable
fun infoSideName(side: InfoPosition): String = stringResource(
    when (side) {
        InfoPosition.TOP -> R.string.settings_top
        InfoPosition.BOTTOM -> R.string.settings_bottom
    }
)

/**
 * La riga 'Posizione': titolo leggero a sinistra, i due gettoni appoggiati a destra, in linea.
 *
 * La forma è quella chiesta dall'utente (riscontro `chip-colonna`): titolo subordinato alla riga
 * precedente, gettoni in linea a destra, nell'ordine dell'enum ('In alto', 'In basso').
 * I gettoni sono in una `FlowRow`: in una lingua che li scrive lunghi vanno a capo e restano
 * nello schermo. Il titolo prende il peso della riga, quindi si stringe per primo.
 *
 * @param enabled se i due gettoni si possono toccare. Spenti restano visibili e leggibili: una
 *   riga che sparisce col suo interruttore fa credere che l'impostazione non esista. Il valore
 *   `infoPosition` resta scritto a barra spenta, così riaccendendola si ritrova il lato scelto.
 */
@Composable
fun InfoSideRow(
    selected: InfoPosition,
    enabled: Boolean,
    onSelect: (InfoPosition) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_info_position),
            style = MaterialTheme.typography.bodyMedium,
            // Anche il titolo si spegne coi gettoni: a pieno colore accanto a due gettoni
            // spenti si leggerebbe come un guasto.
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = if (enabled) 1f else OFF_INK
            ),
            modifier = Modifier.weight(1f)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            modifier = Modifier.oneOf()
        ) {
            InfoPosition.entries.forEach { side ->
                FilterChip(
                    selected = side == selected,
                    onClick = { onSelect(side) },
                    enabled = enabled,
                    label = { Text(infoSideName(side)) },
                    modifier = Modifier.picked(side == selected)
                )
            }
        }
    }
}


package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
 * I comandi della **barra delle info**, che vivono in due posti e devono essere identici.
 *
 * ⚠️⚠️ **QUESTO FILE NASCE DA UNO SCAMBIO MIO, e la 1.38 lo rimette a posto** (riscontro
 * `chip-colonna`, 2026-09-02: *qui c'è stato un grosso problema o fraintendimento: quando
 * l'ho chiesto per il pannello l'hai fatta nelle impostazioni, e viceversa; di conseguenza ti
 * ho sempre dato il feedback sbagliato*). Le due richieste della `1.26` e della `1.29` sono
 * finite ognuna nel posto dell'altra, quindi per tre versioni i riscontri hanno limato la
 * cosa sbagliata: i tre gettoni impilati sono stati provati e ritoccati **nelle
 * impostazioni**, dove lui voleva un interruttore, e l'interruttore è rimasto **nel
 * pannellino**, dove lui voleva i gettoni.
 * ⚠️⚠️ **LA CURA NON È RIFARE I DUE POSTI CON CURA: è avere UNA riga sola.** Finché la stessa
 * scelta è disegnata due volte, un giro di riscontri ne aggiusta una e l'altra resta indietro,
 * ed è successo tre volte di fila (`chip-impilati` della 1.37 lo dice per esteso: *e la riga
 * omologa delle impostazioni era già così dalla 1.36*). Adesso il disegno è qui, e i due posti
 * lo chiamano.
 *
 * ⚠️ **Che cosa NON sta qui**: l'interruttore. Nelle impostazioni sta su una riga sua, nel
 * pannellino sta **sulla riga del titolo** (parole sue: *in quel caso l'interruttore va in
 * linea con il titolo stesso, e sempre allineato a destra*), cioè in due contenitori diversi
 * che nessun composabile condiviso può abitare. Un `Switch` di Material è una riga sola e non
 * ha niente da far divergere: quello che divergeva era la **disposizione della scelta**.
 */

/**
 * Il nome di un lato, letto una volta sola per tutti e due i posti.
 *
 * ⚠️ Esiste perché la ricerca delle impostazioni deve poter filtrare su 'In alto' e 'In basso'
 * senza riscrivere il `when`: due elenchi delle stesse due voci sono già abbastanza per
 * divergere.
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
 * ⚠️⚠️ **LA FORMA È QUELLA DETTATA** (riscontro `chip-colonna`: *'Posizione' (gerarchicamente
 * subordinata alla riga precedente): in linea e allineati a destra, i chip 'In alto' 'In
 * basso' in questo ordine*), ed è la stessa che la `1.25` aveva già costruito per le
 * impostazioni: titolo senza il peso di un'impostazione a sé, gettoni sulla sua riga, a
 * destra. L'ordine è quello dell'enum, cioè quello chiesto.
 * ⚠️ **I gettoni stanno in una `FlowRow` e non in una `Row`**: in linea ci vanno perché sono
 * due e corti, ma in una lingua che li scrive lunghi devono poter andare a capo invece di
 * uscire dallo schermo. Il titolo prende il peso della riga, quindi si stringe per primo e
 * lascia il posto a loro.
 *
 * @param enabled se i due gettoni si possono toccare. ⚠️⚠️ **Spenti restano visibili e
 *   leggibili** (richiesta: *i due chip della posizione sono selezionabili solo quando
 *   l'interruttore principale è ON*): una riga che sparisce col suo interruttore fa credere
 *   che l'impostazione non esista, mentre esiste e il suo interruttore è lì sopra. ⚠️ **E il
 *   valore sotto non si perde**: `infoPosition` resta scritto a barra spenta, così
 *   riaccendendola si ritrova il lato che si era scelto.
 */
@OptIn(ExperimentalLayoutApi::class)
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
            // ⚠️ Anche il titolo si spegne coi gettoni: un'etichetta a pieno colore sopra due
            // gettoni spenti si legge come un guasto invece che come una riga in attesa.
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = if (enabled) 1f else SIDE_OFF
            ),
            modifier = Modifier.weight(1f)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            InfoPosition.entries.forEach { side ->
                FilterChip(
                    selected = side == selected,
                    onClick = { onSelect(side) },
                    enabled = enabled,
                    label = { Text(infoSideName(side)) }
                )
            }
        }
    }
}

/**
 * Quanto sbiadisce il titolo quando i gettoni sono spenti.
 *
 * ⚠️ **0,38, che è il numero di Material per il contenuto disabilitato**, lo stesso che
 * `FilterChip` applica da sé ai gettoni accanto: scritto qui perché su un `Text` nudo nessuno
 * lo applica, e due sbiadimenti diversi sulla stessa riga si vedrebbero.
 */
private const val SIDE_OFF = 0.38f

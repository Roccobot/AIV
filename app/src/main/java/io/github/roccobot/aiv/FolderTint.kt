package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Il colore dell'**intestazione** di una cartella: le sedici tinte e la finestra che le mostra.
 *
 * ⚠️⚠️ **NASCE NELLA `1.85` DA UNA SUA RICHIESTA** (riscontro del giro della `1.83`: *il tocco
 * lungo sulla cartella apre un selettore di colore che fa impostare il colore della sfumatura
 * dell'intestazione per cartella, piccolo tocco di personalizzazione: la scelta sarà solo tra 16
 * colori preimpostati, presentati come tondi da toccare in una griglia 4x4*).
 * ⚠️ **Vive fuori dalle impostazioni**, perché non è una preferenza dell'app ma un dato di una
 * cartella: si sceglie guardando quella cartella, e nel pannello sarebbe una riga che chiede *di
 * quale?*. È la stessa clausola con cui `folderView` sta solo nella sua scorciatoia.
 */

/**
 * Le sedici tinte, nell'ordine in cui compaiono nella griglia 4x4.
 *
 * ⚠️⚠️ **LE PRIME OTTO SONO SUE, ALLA LETTERA** (*prime due righe: 4E6367, 00727B, 43B59E,
 * 4FD9BE / C0FFE5, 38BFD3, FFB400, BC4A61; gli altri 8 colori sceglili tu*): sono i colori di
 * casa, cioè la famiglia del verde acqua dell'app più i tre accenti caldi che la rompono.
 * ⚠️⚠️ **TRE DELLE SUE SONO CAMBIATE NELLA `1.86`, E TUTTE E TRE SU SUA ISTRUZIONE.** L'ambra
 * `FFB400` diventa `FFA726`, che è `HINT_MARK`, cioè l'accento dei mini-onboarding (*sostituisci
 * l'arancione che ho inserito con quello dell'accento dei mini-onboarding*): così l'unico
 * arancione dell'app è uno solo.
 * ⚠️⚠️ **E DUE ERANO IL COLORE PREDEFINITO, non uno**, che è il fatto misurato dietro la sua
 * nota (*credo di aver inserito tra i colori di prima anche il colore predefinito nella mia
 * lista; se è così, sostituiscilo con una sua variazione di luminosità o saturazione*): una
 * cartella senza tinta prende `MaterialTheme.colorScheme.primary`, che vale `43B59E` sul tema
 * chiaro e `00727B` su quello scuro, e tutti e due erano nella griglia. Quindi in ciascuno dei
 * due temi c'era un tondo che non aggiungeva niente, perché sceglierlo dava esattamente il
 * colore che la cartella aveva già.
 * ⚠️ **Le due variazioni tengono la tinta e spostano luminosità e saturazione**, e i numeri
 * sono scelti misurando: `23927C` sta a 58 dal predefinito chiaro e a 51 dal più vicino degli
 * altri quindici (il verde `2E7D4F`), `0098A4` a 56 dal predefinito scuro e a 83 dal celeste
 * `38BFD3`. Le vie scartate erano più belle e più vicine: `35907E` cadeva a 51 da `2E7D4F`, e
 * la variazione di sola saturazione (`24D4B0`) finiva a 45 dall'acquamarina `4FD9BE`, cioè
 * risolveva un doppione facendone un altro.
 * ⚠️⚠️ **LE ALTRE OTTO COMPLETANO LA RUOTA, ed è il criterio con cui sono scelte**: nelle sue
 * mancano il blu, l'indaco, il viola, il magenta e tutta la metà calda che non sia l'ambra,
 * quindi una cartella di ritratti e una di documenti finirebbero per forza nella stessa famiglia.
 * Restano nella **stessa fascia di luminosità** delle sue (fra il grigio-blu scuro e la menta
 * chiara), o sedici tondi in fila si leggerebbero come due tavolozze diverse.
 * ⚠️ **Sono numeri e non risorse colore**: una tinta scelta dall'utente non cambia col tema, o
 * la cartella che ha segnato di rosso sarebbe di un altro rosso la sera.
 */
val FRONT_TINTS: List<Color> = listOf(
    // Le sue due righe: la famiglia dell'app, poi i tre caldi.
    Color(0xFF4E6367), Color(0xFF0098A4), Color(0xFF23927C), Color(0xFF4FD9BE),
    Color(0xFFC0FFE5), Color(0xFF38BFD3), Color(0xFFFFA726), Color(0xFFBC4A61),
    // Le mie due: i freddi che mancavano, e poi i caldi e i verdi.
    Color(0xFF4E7FD4), Color(0xFF6C5CE0), Color(0xFF9B5FC7), Color(0xFFD46FB0),
    Color(0xFFE2725B), Color(0xFFA8823C), Color(0xFF7A9E3F), Color(0xFF2E7D4F)
)

/**
 * La tinta scelta per una cartella, o `null` per quella dell'app.
 *
 * ⚠️ **L'indice fuori elenco vale come 'nessuna scelta'**: l'elenco può accorciarsi, e un numero
 * vecchio nell'archivio non deve far cadere la schermata.
 */
fun frontTintOf(index: Int?): Color? = index?.let { FRONT_TINTS.getOrNull(it) }

/**
 * La finestra che fa scegliere la tinta di questa cartella.
 *
 * ⚠️ **Non è una modale vera**: non raccoglie nessun input scritto, quindi il tocco fuori la
 * chiude, che è il criterio di `AIV/CLAUDE.md` § '👆 Che cosa fa il tocco FUORI da una finestra'.
 * ⚠️ **Il tocco sceglie e chiude**, senza un tasto di conferma: la scelta è di un colore fra
 * sedici e si vede subito dietro la finestra, quindi una conferma sarebbe un tocco in più per
 * dire una cosa che l'occhio ha già detto.
 * ⚠️⚠️ **OGNI TONDO È UN BERSAGLIO SOLO con `Role.RadioButton`**, e la sua descrizione parlata è
 * il **codice esadecimale**: un colore non ha un nome che esista in ventotto lingue senza
 * inventarlo, e sedici nomi inventati sarebbero sedici stringhe che nessun traduttore può
 * verificare. Il codice è un dato, e chi usa un lettore di schermo lo riconosce fra i sedici.
 *
 * @param current l'indice scelto per questa cartella, o `null`.
 * @param onPick l'indice nuovo, o `null` per tornare alla tinta dell'app.
 */
@Composable
fun TintDialog(current: Int?, onPick: (Int?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(onDismiss),
        properties = loweredWindow(onDismiss),
        title = { Text(stringResource(R.string.front_tint)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TINT_GAP)) {
                FRONT_TINTS.chunked(TINT_COLUMNS).forEachIndexed { riga, tinte ->
                    Row(horizontalArrangement = Arrangement.spacedBy(TINT_GAP)) {
                        tinte.forEachIndexed { colonna, tinta ->
                            val quale = riga * TINT_COLUMNS + colonna
                            TintDot(
                                tint = tinta,
                                picked = quale == current,
                                onPick = { onPick(quale); onDismiss() }
                            )
                        }
                    }
                }
            }
        },
        /*
         * ⚠️ **Il ritorno alla tinta dell'app è un tasto e non un diciassettesimo tondo**: un
         * tondo del colore dell'app starebbe in fila con gli altri sedici e si leggerebbe come
         * una scelta in più, mentre è il **togliere** una scelta.
         */
        confirmButton = {
            TextButton(onClick = { onPick(null); onDismiss() }) {
                Text(stringResource(R.string.front_tint_none))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Un tondo del selettore.
 *
 * ⚠️ **La spunta è dentro il tondo e non un contorno in più**: su una tinta chiara un contorno
 * d'accento si confonde con la tinta stessa, e il segno dovrebbe dire 'questa' senza dipendere
 * dal colore che sta segnando.
 * ⚠️ **L'inchiostro della spunta si sceglie dalla LUMINANZA della tinta**, non dal tema: sedici
 * tondi vanno dal grigio scuro alla menta chiarissima, e un segno bianco sparirebbe sulla metà
 * chiara in tutti e due i temi.
 */
@Composable
private fun TintDot(tint: Color, picked: Boolean, onPick: () -> Unit) {
    val etichetta = "#%06X".format(tint.toArgb() and 0xFFFFFF)
    Box(
        modifier = Modifier
            .size(TINT_DOT)
            .clip(CircleShape)
            .background(tint)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .selectable(selected = picked, role = Role.RadioButton, onClick = onPick)
            .semantics { contentDescription = etichetta },
        contentAlignment = Alignment.Center
    ) {
        if (picked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (tint.luminance() > TINT_DARK_INK) Color.Black else Color.White,
                modifier = Modifier.size(TINT_MARK)
            )
        }
    }
}

/** Quanto è largo un tondo: il bersaglio minimo di un tocco, che è quello che serve qui. */
private val TINT_DOT = 48.dp

/** La spunta dentro il tondo. */
private val TINT_MARK = 24.dp

/** L'aria fra un tondo e l'altro, in tutte e due le direzioni. */
private val TINT_GAP = 8.dp

/** Quante colonne ha la griglia: **quattro**, cioè la 4x4 che ha chiesto. */
private const val TINT_COLUMNS = 4

/**
 * Sopra quanta luminanza la spunta si scrive in nero.
 *
 * ⚠️ **Mezzo e non la soglia di contrasto WCAG**: qui si sceglie fra due inchiostri opposti, e la
 * luminanza relativa di Compose è già percettiva. Con le sedici tinte in casa il taglio cade dove
 * ci si aspetta, cioè fra il ciano e la menta.
 */
private const val TINT_DARK_INK = 0.5f

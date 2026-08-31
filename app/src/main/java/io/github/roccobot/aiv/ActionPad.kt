package io.github.roccobot.aiv

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Le operazioni sui file, come riquadro di icone a tre colonne.
 *
 * ⚠️⚠️ **NASCE PER DUE POSTI INSIEME, e questa è la ragione per cui è un file a sé**: le
 * azioni della selezione nella griglia e quelle del tocco lungo nel visualizzatore sono le
 * **stesse sei**, e l'utente le ha chieste nella stessa forma. Due copie divergerebbero al
 * primo ritocco, e l'ordine dei tasti è precisamente la cosa che non deve cambiare fra una
 * schermata e l'altra: chi impara dove sta 'sposta' lo impara una volta.
 *
 * ⚠️ **L'ordine è quello dell'utente** (richiesta del 2026-08-30: *copia, sposta, elimina /
 * rinomina, condividi, info*) e non uno mio, quindi non si riordina 'per sicurezza': chi
 * volesse spostare 'elimina' lontano da 'sposta' cambierebbe una scelta, non un difetto.
 * ⚠️⚠️ **Ed è per QUELL'ordine che il colore dell'errore non è decorativo**: 'elimina' sta
 * accanto a 'sposta' invece di stare in fondo dopo una riga di separazione, come nel menu
 * che c'era prima, quindi il colore è l'unica cosa che la distingue dalla vicina. Chi lo
 * togliesse lascerebbe l'unica voce irreversibile identica a quelle che si possono disfare.
 *
 * ⚠️ **Icona più parola, e la parola non è un ripensamento**: l'utente ha chiesto icone, e
 * l'icona è quello che si riconosce a colpo d'occhio, ma 'copia' e 'sposta' hanno due glifi
 * che si somigliano, e fra sei tasti la parola minuta è quello che impedisce di sbagliare
 * mirando. Costa una riga di testo e non un tocco.
 */
@Composable
fun ActionPad(actions: List<PadAction>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = PAD_EDGE, vertical = PAD_GAP),
        verticalArrangement = Arrangement.spacedBy(PAD_GAP)
    ) {
        // ⚠️ Le righe si ricavano a gruppi di [PAD_COLUMNS] invece di essere scritte a
        // mano: con sei azioni fanno le due righe di tre che l'utente ha chiesto, e con
        // cinque l'ultima riga ne tiene due invece di lasciare un buco da riempire con un
        // tasto finto. Serve al cestino, dove 'rinomina' diventa 'ripristina' e le voci
        // possono non essere sei.
        for (row in actions.chunked(PAD_COLUMNS)) {
            Row(horizontalArrangement = Arrangement.spacedBy(PAD_GAP)) {
                for (action in row) PadButton(action)
            }
        }
    }
}

/**
 * Un'azione del riquadro: l'icona, la parola, e se è quella da cui non si torna.
 *
 * ⚠️ [danger] non è 'importante': è **irreversibile**. Vale per l'eliminazione, e per
 * niente che si possa disfare con l'operazione contraria.
 */
class PadAction(
    val icon: ImageVector,
    @StringRes val label: Int,
    val danger: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Un tasto del riquadro.
 *
 * ⚠️ **L'icona non porta descrizione e il testo sì**: `clickable` fonde le semantiche dei
 * figli, quindi TalkBack legge una voce sola. Descrivendo anche l'icona la leggerebbe due
 * volte, che è il difetto già evitato nelle copertine delle cartelle.
 * ⚠️ **Il tocco sta sull'intera colonna**, non sull'icona: un bersaglio di 24dp si manca,
 * e qui i tasti sono sei e vicini. La cella è larga [PAD_CELL] e alta quanto icona più
 * parola, che è l'area minima toccabile di Material.
 */
@Composable
private fun PadButton(action: PadAction) {
    val tint =
        if (action.danger) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier
            .width(PAD_CELL)
            .clip(RoundedCornerShape(PAD_CORNER))
            .clickable(onClick = action.onClick)
            .padding(vertical = PAD_GAP),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PAD_LABEL_GAP)
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(PAD_ICON)
        )
        Text(
            text = stringResource(action.label),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            textAlign = TextAlign.Center,
            // ⚠️ Due righe e non una: fra le lingue che stanno per arrivare ce ne sono
            // di più lunghe dell'italiano, e una parola tagliata a metà in un tasto di
            // icone lascia il tasto senza nome. Le celle di una riga si allineano in
            // alto, quindi una parola che va a capo allunga la sua colonna e non
            // scompagina le icone.
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Quante colonne ha il riquadro: tre, come l'utente le ha chieste. */
private const val PAD_COLUMNS = 3

/**
 * La larghezza di una cella.
 *
 * ⚠️ Tre celle più i distacchi fanno poco meno di 250dp, che sta dentro uno schermo da
 * 360dp con il margine del menu: è il vincolo che decide questo numero, non l'estetica.
 */
private val PAD_CELL = 76.dp

/** Il lato dell'icona: quella di un tasto, non quella di una barra. */
private val PAD_ICON = 24.dp

/** Il distacco fra le celle, e il respiro dentro ognuna. */
private val PAD_GAP = 8.dp

/** Quanto stacca la parola dalla sua icona: poco, perché sono la stessa cosa. */
private val PAD_LABEL_GAP = 2.dp

/** Il margine laterale del riquadro dentro il menu che lo contiene. */
private val PAD_EDGE = 4.dp

/** Lo smusso dell'alone del tocco su una cella. */
private val PAD_CORNER = 10.dp

/**
 * Lo smusso del tastino quadrato, uguale in tutte le schermate.
 *
 * ⚠️ Quadrato ma non tagliente: il tondo pieno griderebbe 'azione principale', e in questa
 * app l'azione principale sono sempre le fotografie. ⚠️ **Sta qui e non in una schermata**
 * perché i tastini sono due, quello delle cartelle e quello della selezione, e due numeri
 * uguali scritti in due file sono un numero che prima o poi diverge.
 */
val FAB_CORNER = 12.dp

/**
 * Il margine del tastino quadrato dalle due sponde della schermata delle cartelle.
 *
 * ⚠️ Sta qui e non là perché [FAB_REACH] lo somma: il giorno che il tastino si sposta di un
 * dp, il conto che tiene le cartelle sopra di lui deve muoversi con lui.
 */
val HUB_PAD = 16.dp

/**
 * Quanto arriva in su il tastino quadrato delle cartelle, misurato dal fondo dello schermo:
 * il suo margine ([HUB_PAD]) più la sua altezza.
 *
 * ⚠️⚠️ **È LA Y DA CUI PARTE LA SFUMATURA che inghiotte quello che sta sotto** (richiesta
 * dell'utente, dalla `0.77`). Comincia dove comincia il **tastino**, non dove finisce lo
 * spazio che gli si lascia, che è [BELOW_FAB] e vale una ventina di dp in più: la differenza
 * fra i due numeri è l'aria che al riposo resta fra l'ultima cartella e il tastino, e la
 * sfumatura deve trovarla vuota.
 * ⚠️ **40dp è la misura di `SmallFloatingActionButton`**, che Material non espone come
 * costante pubblica: è un dato suo, non una nostra scelta, e lo stesso numero sta in
 * `GridScreen` per la stessa ragione.
 */
val FAB_REACH = HUB_PAD + 40.dp

/**
 * Quanto spazio resta sotto l'ultimo elemento di una griglia, perché il tastino non gli si
 * sieda sopra.
 *
 * ⚠️ Serve **solo** quando il tastino c'è: nella griglia delle foto compare con la
 * selezione, quindi il fondo cresce da quel momento. Senza, la fotografia in basso a
 * destra resterebbe coperta proprio mentre si sta scegliendo, cioè quando la si deve poter
 * toccare.
 * ⚠️ **Scritto come [FAB_REACH] più aria** dalla `0.77`, e prima era 76dp nudi: i due numeri
 * descrivono la stessa cosa a due altezze diverse, e slegati si sarebbero mossi uno per
 * volta.
 */
val BELOW_FAB = FAB_REACH + 20.dp

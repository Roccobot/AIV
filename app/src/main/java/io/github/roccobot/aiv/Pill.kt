package io.github.roccobot.aiv

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/*
 * La pillola d'accento che prende il posto del FAB sugli schermi larghi e alti.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * Mockup `Tablet_H` e `Tablet_V`: col telefono o il tablet in orizzontale le voci del FAB sono in
 * una pillola verticale in basso a destra della griglia, col tablet in verticale in una pillola
 * orizzontale in basso a destra. Dove c'è la pillola, il FAB non c'è (decisione B4).
 * ⚠️ Le voci sono quelle del FAB, lette dallo stesso posto: quando il FAB offre scelte diverse o
 * in più, la pillola le segue. Per questo una schermata passa una lista alla pillola e non ne
 * scrive una seconda.
 * Il salto in cima o in fondo (la freccia sul FAB) non è qui: una pillola non cambia icona
 * durante lo scorrimento (decisione B4).
 * ⚠️ **Il tocco lungo su un tasto mostra la sua etichetta in un fumetto** (risposte B1 e B2
 * dell'utente, 4.00): un tasto senza etichetta scritta deve poter dire che cos'è. Le eccezioni
 * sono in [PillKey]. Il riempimento segue l'impostazione ([pillFill]).
 */

/**
 * Un comando della pillola.
 *
 * @property label quello che dice il lettore di schermo e la riga del menu del FAB: la stessa
 *   stringa.
 * @property enabled se il comando può agire adesso (le due azioni del cestino a cestino vuoto).
 */
data class PillEntry(
    val icon: ImageVector,
    val label: String,
    val enabled: Boolean = true,
    /**
     * Il gruppo della voce, per il menu del FAB, che traccia un divisore fra i gruppi. La
     * pillola non ne traccia: i suoi tasti sono icone, e una linea fra due icone in una capsula
     * sembra una giuntura.
     */
    val group: Int = 0,
    /**
     * Che cosa fa il tocco lungo, dove la riga ne ha uno ('Mostra nascoste').
     *
     * ⚠️ Un tasto della pillola lo esegue solo quando riceve [holdLabel] ([PillKey]); altrimenti
     * il tocco lungo mostra l'etichetta.
     */
    val onHold: (() -> Unit)? = null,
    /**
     * L'etichetta abbreviata che il Menu 'Start' scrive sotto l'icona (nota F del giro della
     * 4.32): `null` dove la cella tiene la sola icona (la ×).
     */
    val short: String? = null,
    /**
     * Che cosa fa [onHold], dove il Menu 'Start' lo offre: il gesto prende il posto del fumetto
     * (voce `4.34-01` dell'utente, 4.35).
     */
    val holdLabel: String? = null,
    val onTap: () -> Unit
)

/** La distanza della pillola dai bordi dello schermo e da quello che ha sopra. */
val PILL_AIR = 8.dp

/** Il lato di un tasto della pillola, che è anche lo spessore della pillola. */
val PILL_KEY = 44.dp

/** Il lato dell'icona di un tasto. */
internal val PILL_GLYPH = 22.dp

/**
 * La pillola, da sola.
 *
 * @param vertical una colonna in basso a destra della griglia (schermi larghi) o una riga
 *   nell'angolo in basso a destra (schermi alti).
 *
 * I colori sono l'accento e il suo inchiostro, come nel mockup: la pillola dice 'comandi' come il
 * FAB, col colore principale del tema. L'accento può essere all'80% o vetro smerigliato
 * ([pillFill]), e [backdrop] è quello che il vetro sfoca.
 * Senza voci non disegna niente: una capsula vuota è un comando che non fa niente.
 * La pillola orizzontale ha le sole icone (voce `3.70-07` dell'utente, 3.71).
 */
@Composable
fun ActionPill(
    entries: List<PillEntry>,
    vertical: Boolean,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null
) {
    if (entries.isEmpty()) return
    CompositionLocalProvider(LocalContentColor provides pillInk()) {
        if (vertical) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier.pillFill(backdrop)
            ) {
                entries.forEach { PillKey(it) }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier.pillFill(backdrop)
            ) {
                entries.forEach { PillKey(it) }
            }
        }
    }
}

/**
 * Un tasto della pillola: la sola icona, con l'etichetta per il lettore di schermo e in un
 * fumetto.
 *
 * ⚠️ **Il tocco lungo mostra l'etichetta e nient'altro** (risposte B1 e B3 dell'utente, 4.00), in
 * un fumetto sopra il tasto, dove il dito non lo copre. L'eccezione è [holdLabel]: chi lo passa
 * fa eseguire [PillEntry.onHold], come il tasto tondo di [PhonePill.SLIDE] a riposo, che è il FAB
 * e tiene il suo tocco lungo (risposta C1), e il Menu 'Start'.
 *
 * @param size il lato del tasto: più piccolo di [PILL_KEY] solo quando la pillola non ci starebbe.
 * @param glyph quello che il tasto mostra, dove non è solo [PillEntry.icon] (il tasto tondo, e i
 *   due tasti che diventano il salto).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PillKey(
    entry: PillEntry,
    size: Dp = PILL_KEY,
    enabled: Boolean = entry.enabled,
    holdLabel: String? = null,
    modifier: Modifier = Modifier,
    glyph: (@Composable () -> Unit)? = null
) {
    val tip = rememberTooltipState()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val hold = entry.onHold.takeIf { holdLabel != null }
    val press = remember { MutableInteractionSource() }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(entry.label) } },
        state = tip,
        enableUserInput = false,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .combinedClickable(
                    // L'onda del tocco è tonda (nota dell'utente, giro della 4.04): un quadrato
                    // dentro una pillola arrotondata sembra un errore.
                    // ⚠️ È un'onda senza limiti, non un ritaglio: un ritaglio tondo taglierebbe
                    // le etichette del Menu 'Start', più larghe del tasto (voce `4.33-07`).
                    interactionSource = press,
                    indication = ripple(bounded = false, radius = size / 2),
                    enabled = enabled,
                    role = Role.Button,
                    onClick = entry.onTap,
                    onLongClickLabel = holdLabel,
                    onLongClick = {
                        haptics.performHapticFeedback(HOLD_BUZZ)
                        if (hold != null) hold() else scope.launch { tip.show() }
                    },
                    hapticFeedbackEnabled = false
                )
                .semantics { contentDescription = entry.label }
                .alpha(if (entry.enabled) 1f else DISABLED_INK)
        ) {
            if (glyph != null) {
                glyph()
            } else {
                Icon(imageVector = entry.icon, contentDescription = null, modifier = Modifier.size(PILL_GLYPH))
            }
        }
    }
}

/**
 * 'Cerca nelle cartelle', oppure 'Cerca in *X*' col nome della cartella in grassetto.
 *
 * Il modello contiene `%1$s`: si divide su un segnaposto messo al posto del nome, così restano
 * giuste le lingue che mettono il nome altrove.
 */
@Composable
fun searchInvitation(folderName: String?): AnnotatedString {
    if (folderName == null) {
        return buildAnnotatedString { append(stringResource(R.string.folders_rail_search)) }
    }
    val marker = "\u0001"
    val parts = stringResource(R.string.folders_rail_search_in, marker).split(marker, limit = 2)
    return buildAnnotatedString {
        append(parts[0])
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(folderName) }
        if (parts.size > 1) append(parts[1])
    }
}

/** L'opacità di un tasto che non può agire: il valore di Material per i contenuti disattivati. */
private const val DISABLED_INK = 0.38f

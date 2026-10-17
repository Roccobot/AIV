package io.github.roccobot.aiv

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp

/**
 * I due comandi del **salvataggio** dei due editor: 'Filigrana' e 'Ridimensiona'.
 *
 * Sono nella barra in basso, dove arriva il pollice ([EditorToolBar]).
 *
 * ⚠️ **I due gesti** (istruzione dell'utente, giro della 2.70): il tocco accende e spegne, che è
 * la cosa frequente; il tocco lungo apre le impostazioni, che si toccano una volta.
 *
 * Un pezzo solo per tutti e due: sono tasti gemelli e affiancati, e due disegni separati
 * divergerebbero al primo ritocco.
 *
 * L'accento dice se è acceso, e l'inchiostro attenuato se il comando è disattivato.
 */
@Composable
private fun EditorTool(
    icon: ImageVector,
    /** Come si chiama: è la descrizione parlata, e la parola che l'app usa per questa funzione. */
    label: String,
    /** Se si applica al salvataggio. */
    on: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onSetup: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(width = LocalEditorToolWidth.current, height = TOOL_TOUCH)
            .clip(CircleShape)
            .combinedClickable(
                enabled = enabled,
                role = Role.Switch,
                // L'etichetta del tocco lungo è quella della schermata a cui porta:
                // 'Impostazioni' esiste già in tutte le lingue.
                onLongClickLabel = stringResource(R.string.settings_title),
                onLongClick = {
                    haptics.performHapticFeedback(HOLD_BUZZ)
                    onSetup()
                },
                hapticFeedbackEnabled = false,
                onClick = onToggle
            )
            // ⚠️ Senza `toggleableState` `Role.Switch` non annuncia lo stato al lettore di
            // schermo, e `Modifier.toggleable`, che lo metterebbe da sé, non offre il tocco lungo.
            .semantics { toggleableState = ToggleableState(on) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            modifier = Modifier.size(LocalEditorToolWidth.current / 2),
            contentDescription = label,
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = OFF_INK)
                on -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/**
 * Il comando 'Filigrana', che nella fila viene prima di 'Ridimensiona'.
 *
 * ⚠️ **Senza un logo scelto il tasto non c'è**: non ci sarebbe niente da accendere, come 'Mostra
 * nascoste' che compare solo con una cartella nascosta (`docs/Folders.md` § '👁️ Mostra nascoste, e
 * perché dura un minuto'). Il logo si sceglie dalle impostazioni.
 *
 * Il glifo è il disegno di Material specchiato (il rettangolino in basso a sinistra, punto B
 * dell'utente, giro della 2.77): la misura è in testa a `res/drawable/ic_watermark.xml`.
 */
@Composable
fun MarkButton(
    /** Se un logo è stato scelto: senza, il tasto non si disegna affatto. */
    has: Boolean,
    /** Se la filigrana si scrive al salvataggio, cioè l'interruttore delle impostazioni. */
    on: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onSetup: () -> Unit
) {
    if (!has) return
    EditorTool(
        icon = Glyphs.Watermark,
        label = stringResource(R.string.settings_mark),
        on = on,
        enabled = enabled,
        onToggle = onToggle,
        onSetup = onSetup
    )
}

/**
 * Il comando 'Ridimensiona'.
 *
 * Il tasto e l'interruttore sono un bersaglio solo con due gesti (risposta `editor` dell'utente a
 * `d-resize-dove`), come `SaveButton` e i gettoni dei moduli.
 *
 * Il piano esiste sempre, anche spento: accendere non chiede di configurare niente. Se il piano
 * di fabbrica non rimpicciolisce quell'immagine, 'Salva' resta spento e la finestra lo dice.
 *
 * Il glifo è quello di Material ammorbidito: la misura e il perché di ogni raccordo sono in testa
 * a `res/drawable/ic_resize.xml`.
 */
@Composable
fun ResizeButton(
    /** Se il ridimensionamento si applica al salvataggio: l'icona prende l'accento. */
    on: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onSetup: () -> Unit
) {
    EditorTool(
        icon = Glyphs.Resize,
        label = stringResource(R.string.look_resize),
        on = on,
        enabled = enabled,
        onToggle = onToggle,
        onSetup = onSetup
    )
}

/**
 * La coppia di comandi nella barra in basso, dove arriva il pollice (punto C dell'utente, giro
 * della 2.77).
 *
 * Il blocco cambia lato col FAB, e l'ordine dei due si specchia con lui. Che si specchi anche
 * l'ordine interno è una lettura dichiarata: la barra ha l'ordine delle icone speculare, e così
 * 'Filigrana' resta il più vicino al bordo da cui arriva il pollice.
 * Lo spazio dal bordo è [TOOL_EDGE].
 */
@Composable
fun EditorToolBar(
    /** Se il FAB è a sinistra: il blocco passa a destra e i due tasti si scambiano. */
    mirror: Boolean,
    hasMark: Boolean,
    marking: Boolean,
    resizing: Boolean,
    enabled: Boolean,
    onMark: () -> Unit,
    onMarkSetup: () -> Unit,
    onResize: () -> Unit,
    onResizeSetup: () -> Unit,
    /**
     * Dov'è il tasto 'Filigrana', in coordinate della radice, o un riquadro vuoto se il tasto
     * non c'è.
     *
     * Serve al velo d'aiuto, che copre tutto lo schermo e quindi vive nella schermata: il
     * riquadro da illuminare lo sa solo chi disegna il tasto.
     */
    onMarkSpot: (Rect) -> Unit = {},
    onResizeSpot: (Rect) -> Unit = {}
) {
    val filigrana: @Composable () -> Unit = {
        Box(modifier = Modifier.onGloballyPositioned { onMarkSpot(it.boundsInRoot()) }) {
            MarkButton(
                has = hasMark,
                on = marking,
                enabled = enabled,
                onToggle = onMark,
                onSetup = onMarkSetup
            )
        }
    }
    val misura: @Composable () -> Unit = {
        Box(modifier = Modifier.onGloballyPositioned { onResizeSpot(it.boundsInRoot()) }) {
            ResizeButton(
                on = resizing,
                enabled = enabled,
                onToggle = onResize,
                onSetup = onResizeSetup
            )
        }
    }
    Row(
        modifier = Modifier.padding(
            if (mirror) PaddingValues(end = TOOL_EDGE) else PaddingValues(start = TOOL_EDGE)
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (mirror) {
            misura()
            filigrana()
        } else {
            filigrana()
            misura()
        }
    }
}

/** Il bersaglio di un comando dell'editor, come quello di un `IconButton` di Material. */
internal val TOOL_TOUCH = 48.dp

/** Larghezza condivisa dai comandi nella sola barra che richiede la misura compatta. */
internal val LocalEditorToolWidth = staticCompositionLocalOf { TOOL_TOUCH }

/**
 * Quanto la coppia rientra dal bordo esterno della barra.
 *
 * È il 'non del tutto a sinistra' dell'utente, misurato sul mockup: il primo bersaglio comincia
 * a 41 punti dal vetro, e la scheda ne ha già 16. Vale [STAGE_SIDE], l'aria che il palco lascia
 * ai fianchi, e su uno schermo da 360 punti il tasto comincia a 40.
 */
internal val TOOL_EDGE = STAGE_SIDE

package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * La finestra di **'Ridimensiona'**: il modo, il valore, e che misura ne viene fuori.
 *
 * ⚠️⚠️ **'APPLICA' ACCENDE L'INTERRUTTORE, ED È LA SUA SPECIFICA ALLA LETTERA** (risposta `editor`
 * a `d-resize-dove`: *una volta che premo 'OK' l'interruttore è acceso e salva con
 * ridimensionamento se non lo spengo*). Quindi questa finestra non chiede due volte la stessa cosa:
 * chi entra a configurare ha già detto che lo vuole, e l'interruttore serve a **spegnerlo** dopo.
 * ⚠️ **Il tasto porta 'Applica' e non 'OK'**, ed è una lettura dichiarata: dice quello che succede
 * toccandolo, e quella stringa esiste già in tutte e ventotto le lingue (è il comando del Ritaglio).
 *
 * ⚠️⚠️ **LA MISURA CHE NE ESCE SI VEDE MENTRE SI SCRIVE, E SENZA DI LEI SI SCEGLIEREBBE ALLA
 * CIECA**: 'lato lungo 1600' non dice a nessuno quanto verrà l'altra dimensione, e una percentuale
 * non dice niente finché non si sa da dove parte. La riga sotto il campo la calcola con la
 * **stessa** funzione del salvataggio, quindi quello che si legge è quello che verrà scritto.
 * ⚠️ **E quando il piano non rimpicciolisce, lo dice**: chiedere quattromila pixel a un'immagine
 * che ne ha duemila non è un errore da rifiutare, è un piano che su quell'immagine non fa niente.
 *
 * ⚠️ **È una modale vera**, quindi porta tutte e due le righe (`Modifier.lowered(null)` e
 * `properties = loweredWindow(null)`): esiste per raccogliere un input scritto, che è il criterio
 * di `AIV/CLAUDE.md` § '👆 Che cosa fa il tocco FUORI da una finestra'.
 */
@Composable
fun ResizeDialog(
    /** Il piano da cui si parte: quello salvato, anche se l'interruttore è spento. */
    initial: Resize.Plan,
    /**
     * Quanti pixel il salvataggio avrà davanti, o `null` finché non si sa.
     *
     * ⚠️ **Può mancare, e non è un guasto**: la misura vera si legge dal file, e di un indirizzo
     * remoto o di un formato che il decodificatore non riconosce non arriva. Senza di lei la
     * finestra funziona lo stesso e mostra la sola nota: quello che non si può dire è a che
     * misura si arriva.
     */
    size: Pair<Int, Int>?,
    onDismiss: () -> Unit,
    onApply: (Resize.Plan) -> Unit
) {
    var mode by remember { mutableStateOf(initial.mode) }
    var text by remember { mutableStateOf(initial.value.toString()) }

    /*
     * ⚠️⚠️ **IL CAMPO TIENE IL TESTO E NON UN NUMERO, E SENZA QUESTA DISTINZIONE NON SI PUÒ
     * SCRIVERE**: cancellando l'ultima cifra il campo resta vuoto, e un campo legato a un intero
     * ci rimetterebbe uno zero sotto le dita. Quindi qui vive il testo, e il numero si ricava
     * quando serve.
     * ⚠️ **Le cifre si filtrano in scrittura**: la tastiera numerica di Android porta comunque il
     * separatore decimale e il segno, e un carattere che il campo non sa leggere spegnerebbe
     * 'Applica' senza dire perché.
     */
    val range = Resize.range(mode)
    val value = text.toIntOrNull()
    val ok = value != null && value in range
    val plan = if (ok) Resize.Plan(mode, value) else null
    val esito = size?.let { (w, h) -> plan?.sizeFor(w, h) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(null),
        properties = loweredWindow(null),
        title = { Text(stringResource(R.string.look_resize)) },
        text = {
            // ⚠️ Lo scorrimento serve al tetto della `1.62`, come in ogni altra finestra con un
            // campo: a tastiera aperta il pannello si accorcia invece di essere tagliato.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(RESIZE_GAP)
            ) {
                Choices(
                    label = stringResource(R.string.look_resize_mode),
                    detail = null,
                    options = Resize.Mode.entries,
                    selected = mode,
                    nameOf = {
                        stringResource(
                            when (it) {
                                Resize.Mode.LONG -> R.string.resize_long
                                Resize.Mode.WIDE -> R.string.resize_wide
                                Resize.Mode.TALL -> R.string.resize_tall
                                Resize.Mode.SHARE -> R.string.resize_share
                            }
                        )
                    },
                    onSelect = { scelto ->
                        /*
                         * ⚠️⚠️ **IL VALORE SI RIPORTA DENTRO I CONFINI DEL MODO NUOVO, E NON SI
                         * AZZERA**: passando da 'Lato lungo 1600' a 'Percentuale' quel numero non
                         * vuol più dire niente, e lasciarlo darebbe un campo rosso che nessuno ha
                         * scritto. Chi passa alla percentuale trova il valore di fabbrica, chi
                         * torna ai pixel trova il suo.
                         */
                        if (scelto != mode) {
                            val n = text.toIntOrNull()
                            val nuovo = Resize.range(scelto)
                            text = when {
                                n != null && n in nuovo -> n.toString()
                                scelto == Resize.Mode.SHARE -> Resize.DEFAULT_SHARE.toString()
                                else -> Resize.DEFAULT_PX.toString()
                            }
                            mode = scelto
                        }
                    }
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { scritto ->
                        text = scritto.filter { it.isDigit() }.take(RESIZE_DIGITS)
                    },
                    label = {
                        Text(
                            stringResource(
                                if (mode == Resize.Mode.SHARE) R.string.look_resize_pct
                                else R.string.look_resize_px
                            )
                        )
                    },
                    isError = !ok,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    shape = BOX_SHAPE,
                    modifier = Modifier.fillMaxWidth()
                )
                val riga = when {
                    !ok -> stringResource(R.string.look_resize_range, range.first, range.last)
                    size == null -> null
                    esito == null -> stringResource(R.string.look_resize_keep)
                    else -> stringResource(
                        R.string.look_resize_to,
                        size.first, size.second, esito.first, esito.second
                    )
                }
                if (riga != null) {
                    Text(
                        text = riga,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(R.string.look_resize_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { plan?.let(onApply) },
                enabled = plan != null
            ) { Text(stringResource(R.string.editor_apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Il comando **'Ridimensiona'** in testata ai due editor: il tocco apre la finestra, il tocco
 * lungo spegne.
 *
 * ⚠️⚠️ **'UN TASTO PIÙ UN INTERRUTTORE' SU UN BERSAGLIO SOLO, ED È UNA LETTURA DICHIARATA**
 * (sua risposta `editor` a `d-resize-dove`): la sua frase ne descrive due, e in testata non
 * entrano. Il conto, su uno schermo da 360 punti: il tasto Indietro ne prende 48, 'Salva' una
 * settantina, questa icona 48, e uno `Switch` di Material altri 52; al titolo, che è l'unico a
 * cedere, ne resterebbero un centinaio, cioè 'Modifica immagine' a `headlineSmall` andrebbe a
 * capo. Quindi i gesti sono due sullo stesso tasto, che è il modo di questa app (è la regola di
 * `SaveButton` e dei gettoni dei moduli), e l'accento dice se è acceso.
 * ⚠️ **Il tocco lungo c'è solo quando è acceso**: spegnere quello che è già spento non è un
 * gesto, e un'etichetta annunciata che non fa niente è peggio della sua assenza.
 * ⚠️ **Ad accendere è 'Applica' della finestra**, che è la sua specifica alla lettera (*una
 * volta che premo 'OK' l'interruttore è acceso e salva con ridimensionamento se non lo spengo*).
 *
 * ⚠️ **Il glifo è di Material e nasce provvisorio**, come quello di 'Auto' nella `2.32` e di
 * 'Salva stile' nella `2.52`: se non dice abbastanza, il giro di collaudo lo chiede e lui manda
 * il suo.
 */
@Composable
fun ResizeButton(
    /** Se il ridimensionamento si applica al salvataggio: l'icona prende l'accento. */
    on: Boolean,
    enabled: Boolean,
    onOpen: () -> Unit,
    onOff: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(RESIZE_TOUCH)
            .clip(CircleShape)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onLongClickLabel = stringResource(R.string.look_resize_off),
                onLongClick = if (!on) null else {
                    {
                        haptics.performHapticFeedback(HOLD_BUZZ)
                        onOff()
                    }
                },
                onClick = onOpen
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PhotoSizeSelectLarge,
            contentDescription = stringResource(R.string.look_resize),
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = OFF_INK)
                on -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/**
 * Il lato lungo del file aperto, che è la sola misura vera che si legge senza decodificarlo.
 *
 * ⚠️ Vive qui e non nelle due schermate perché lo chiedono tutte e due, e perché è la metà di
 * quello che [Resize.frameSize] vuole: l'altra metà, le proporzioni, la sa l'anteprima.
 */
@Composable
fun rememberLongSide(uri: Uri): Int? {
    val context = LocalContext.current
    return produceState<Int?>(null, uri) {
        value = Pixels.longSideOf(context, uri)
    }.value
}

/** L'aria fra i blocchi della finestra. */
private val RESIZE_GAP = 10.dp

/** Il bersaglio del comando in testata, come quello di un `IconButton` di Material. */
private val RESIZE_TOUCH = 48.dp

/**
 * Quante cifre si accettano nel campo.
 *
 * ⚠️ **Cinque bastano al tetto dei pixel e avanzano alla percentuale**, e servono a non far
 * scrivere un numero così lungo da non entrare in un intero: il campo dice comunque quali valori
 * sono buoni, e il taglio è solo la rete.
 */
private const val RESIZE_DIGITS = 5

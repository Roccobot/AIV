package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.max

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

    /*
     * ⚠️⚠️ **'RIPRISTINA' RIPORTA A 'LATO LUNGO' COL LATO LUNGO DELL'IMMAGINE, ED È SUA
     * RICHIESTA** (2026-09-19, con una schermata: *serve anche un tasto 'Ripristina', che
     * riporti tutto su 'Lato lungo' con il valore letto dall'immagine allo stato corrente*).
     * Quel valore è il piano che **non fa niente**, cioè il punto da cui si riparte: chiedere il
     * lato che l'immagine già ha non la rimpicciolisce (vedi [Resize.Plan.sizeFor]), quindi il
     * comando riporta la finestra a zero senza aprire un caso a parte.
     * ⚠️ **'Allo stato corrente' vuol dire dopo la posa e il ritaglio**, che è esattamente quello
     * che [size] porta: il conto lo fa già [Resize.frameSize] per la riga che dice a che misura
     * si arriva, e prenderne un secondo sarebbe un numero da tenere allineato.
     * ⚠️ **Scrive nel campo e non applica**: la finestra si conferma con 'Applica', come ogni
     * altra cosa che ci si scrive dentro.
     * ⚠️ **Senza la misura il comando non c'è**: là non si sa da dove si parte, e un tasto che
     * riporta a un numero che nessuno conosce non ha niente da scrivere.
     */
    val base = size?.let { (w, h) -> max(w, h).coerceIn(Resize.range(Resize.Mode.LONG)) }
    val reset = stringResource(R.string.look_resize_reset)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(null),
        properties = loweredWindow(null),
        /*
         * ⚠️ **Il comando vive sulla riga del titolo**, che è dove questa app mette un comando
         * di una finestra dalla `1.79` ('Estensione' e 'Percorso' in 'Scarica'): [TitleRow]
         * decide da sé fra la pastiglia scritta e l'icona, misurando se il titolo ci sta
         * accanto, quindi non c'è una seconda regola da scrivere qui.
         */
        title = {
            TitleRow(
                title = stringResource(R.string.look_resize),
                commands = if (base == null) emptyList() else listOf(
                    TitleCommand(
                        text = reset,
                        glyph = Icons.Filled.SettingsBackupRestore,
                        onTap = {
                            mode = Resize.Mode.LONG
                            text = base.toString()
                        }
                    )
                )
            )
        },
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

/**
 * Quante cifre si accettano nel campo.
 *
 * ⚠️ **Cinque bastano al tetto dei pixel e avanzano alla percentuale**, e servono a non far
 * scrivere un numero così lungo da non entrare in un intero: il campo dice comunque quali valori
 * sono buoni, e il taglio è solo la rete.
 */
private const val RESIZE_DIGITS = 5

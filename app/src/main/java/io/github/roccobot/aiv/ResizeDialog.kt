package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * La finestra di **'Ridimensiona'**: le misure di destinazione, la regola che le governa, e che
 * cosa ne viene fuori.
 *
 * ⚠️⚠️ **RIDISEGNATA PER INTERO NELLA `2.77`, ED È LA SUA SPECIFICA ALLA LETTERA** (voce
 * `resize-ripristina` del giro dalla `2.71` alla `2.74`: *la schermata va ridisegnata e migliorata:
 * sotto 'Ridimensiona' va indicata la dimensione di origine ... Di default ci devono essere due
 * campi compilabili con il numero dei pixel di destinazione, e nessun chip deve essere selezionato
 * ... Se poi si tocca un chip, si disattivano i campi che dipendono dal compilabile ... Sotto i
 * campi compilabili, un'anteprima simile a quella di 'Rinomina'*). Fino alla `2.76` c'erano quattro
 * gettoni e **un** campo, quindi 'lato lungo 1600' diceva un numero solo e l'altra misura la si
 * scopriva in una riga di testo.
 *
 * ⚠️⚠️ **I DUE CAMPI SONO LA VERITÀ, E IL GETTONE DICE SOLTANTO QUALE DEI DUE COMANDA**: le
 * proporzioni si mantengono sempre, quindi i due numeri sono uno solo scritto due volte, e il modo
 * sceglie da che parte si scrive. Il valore del piano si **ricava** (vedi [Resize.valueOf]), e per
 * questo cambiare gettone non muove nessuna cifra: è la cosa che nella `2.76` costringeva la
 * finestra a inventarsi un numero a ogni cambio di modo.
 *
 * ⚠️⚠️ **'Pixel' È IL GETTONE DEL LIBERO E NON SI ACCENDE MAI, ED È UNA LETTURA DICHIARATA**: la
 * sua riga dice *nessun chip deve essere selezionato* proprio nello stato in cui i due campi si
 * scrivono tutti e due, e quello stato è il libero. Un gettone acceso direbbe che una regola c'è,
 * mentre là non ce n'è nessuna: il gettone resta perché è la porta per **tornarci** dopo averne
 * toccato un altro, e il riscontro che si vede toccandolo è il campo disattivato che si riaccende.
 *
 * ⚠️ **È una modale vera**, quindi porta tutte e due le righe (`Modifier.lowered(null)` e
 * `properties = loweredWindow(null)`): esiste per raccogliere un input scritto, che è il criterio
 * di `AIV/CLAUDE.md` § '👆 Che cosa fa il tocco FUORI da una finestra'.
 *
 * ⚠️⚠️ **'APPLICA' ACCENDE L'INTERRUTTORE, ED È LA SUA SPECIFICA ALLA LETTERA** (risposta `editor`
 * a `d-resize-dove`: *una volta che premo 'OK' l'interruttore è acceso e salva con
 * ridimensionamento se non lo spengo*). Quindi questa finestra non chiede due volte la stessa cosa:
 * chi entra a configurare ha già detto che lo vuole, e l'interruttore serve a **spegnerlo** dopo.
 * ⚠️ **Il tasto porta 'Applica' e non 'OK'**, ed è una lettura dichiarata: dice quello che succede
 * toccandolo, e quella stringa esiste già in tutte e ventotto le lingue (è il comando del Ritaglio).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResizeDialog(
    /** Il piano da cui si parte: quello salvato, anche se l'interruttore è spento. */
    initial: Resize.Plan,
    /**
     * Quanti pixel il salvataggio avrà davanti, o `null` finché non si sa.
     *
     * ⚠️ **Può mancare, e non è un guasto**: la misura vera si legge dal file, e di un indirizzo
     * remoto o di un formato che il decodificatore non riconosce non arriva. ⚠️⚠️ **E ALLORA
     * RESTA LA SOLA PERCENTUALE**, che è l'unico modo che non ha bisogno di sapere da dove si
     * parte: senza le misure non si possono precompilare i campi né tradurre l'uno nell'altro,
     * cioè manca tutto quello su cui la finestra nuova è costruita.
     * ⚠️ **Può arrivare dopo l'apertura**, perché il lato lungo si legge dal file: per questo i
     * campi li riempie un effetto e non un `remember`.
     */
    size: Pair<Int, Int>?,
    onDismiss: () -> Unit,
    onApply: (Resize.Plan) -> Unit
) {
    val w0 = size?.first ?: 0
    val h0 = size?.second ?: 0
    val known = w0 > 0 && h0 > 0

    var mode by remember { mutableStateOf(initial.mode) }
    var wide by remember { mutableStateOf("") }
    var tall by remember { mutableStateOf("") }
    var share by remember {
        val da = if (initial.mode == Resize.Mode.SHARE) initial.value else Resize.DEFAULT_SHARE
        mutableStateOf(da.toString())
    }

    /*
     * ⚠️ **Il modo che si VEDE, che senza le misure è la percentuale**: lo stato resta quello
     * scelto, perché le misure possono arrivare un istante dopo e quella scelta non va persa.
     */
    val shown = if (known) mode else Resize.Mode.SHARE

    /*
     * ⚠️⚠️ **I CAMPI PARTONO DALLE MISURE DI DESTINAZIONE, E QUANDO IL PIANO NON FA NIENTE SONO
     * QUELLE CORRENTI**: è la sua riga alla lettera (*con le misure correnti precompilate*), ed è
     * una regola sola invece di due, perché il piano di fabbrica è proprio quello che non
     * rimpicciolisce (vedi [Resize.NONE]).
     * ⚠️ **Un effetto e non un `remember`**: la misura la legge il file, quindi può arrivare dopo
     * l'apertura, e i campi devono riempirsi quando arriva.
     */
    LaunchedEffect(size) {
        if (!known) return@LaunchedEffect
        val arrivo = initial.sizeFor(w0, h0) ?: (w0 to h0)
        wide = arrivo.first.toString()
        tall = arrivo.second.toString()
    }

    val side = Resize.edge(shown, w0, h0)
    val wideOn = shown != Resize.Mode.SHARE && side != Resize.Side.TALL
    val tallOn = shown != Resize.Mode.SHARE && side != Resize.Side.WIDE

    /*
     * ⚠️ **Scrivendo in un campo l'altro si aggiorna**, perché le proporzioni si mantengono
     * sempre: due numeri liberi direbbero un rapporto che il salvataggio non rispetterà. Il conto
     * vive in [Resize.mate], cioè fuori dalla finestra, così il banco lo misura chiamandolo.
     */
    val writeWide = { testo: String ->
        val pulito = testo.filter { it.isDigit() }.take(RESIZE_DIGITS)
        wide = pulito
        val n = pulito.toIntOrNull()
        if (n != null && known) tall = Resize.mate(Resize.Side.WIDE, n, w0, h0).toString()
    }
    val writeTall = { testo: String ->
        val pulito = testo.filter { it.isDigit() }.take(RESIZE_DIGITS)
        tall = pulito
        val n = pulito.toIntOrNull()
        if (n != null && known) wide = Resize.mate(Resize.Side.TALL, n, w0, h0).toString()
    }

    /*
     * ⚠️⚠️ **CAMBIARE GETTONE NON MUOVE I DUE CAMPI, MA LA PERCENTUALE SÌ, PERCHÉ È UN'ALTRA
     * UNITÀ**: fra i cinque modi in pixel il ridimensionamento chiesto è lo stesso e cambia solo
     * chi lo governa; passando ai per cento, o tornandone, il numero va tradotto o la finestra
     * mostrerebbe due ridimensionamenti diversi nello stesso istante.
     */
    val pick = { scelto: Resize.Mode ->
        if (scelto != mode && known) {
            if (scelto == Resize.Mode.SHARE) {
                wide.toIntOrNull()?.let {
                    share = Resize.shareOf(it, w0).coerceIn(Resize.range(Resize.Mode.SHARE))
                        .toString()
                }
            } else if (mode == Resize.Mode.SHARE) {
                val s = share.toIntOrNull()
                val arrivo = s?.let { Resize.Plan(Resize.Mode.SHARE, it).sizeFor(w0, h0) }
                    ?: (w0 to h0)
                wide = arrivo.first.toString()
                tall = arrivo.second.toString()
            }
            mode = scelto
        }
    }

    val wv = wide.toIntOrNull()
    val tv = tall.toIntOrNull()
    val sv = share.toIntOrNull()
    val plan: Resize.Plan? = when {
        shown == Resize.Mode.SHARE ->
            sv?.takeIf { it in Resize.range(Resize.Mode.SHARE) }
                ?.let { Resize.Plan(Resize.Mode.SHARE, it) }
        wv == null || tv == null -> null
        else -> Resize.valueOf(shown, wv, tv, sv ?: Resize.DEFAULT_SHARE)
            .takeIf { it in Resize.range(shown) }
            ?.let { Resize.Plan(shown, it) }
    }
    val esito = if (known) plan?.sizeFor(w0, h0) else null
    val range = Resize.range(shown)

    /*
     * ⚠️⚠️ **'RIPRISTINA' RIPORTA ALLE MISURE DELL'IMMAGINE COM'È ADESSO, ED È SUO** (voce
     * `resize-ripristina`: *'Ripristina' deve riportare i valori non all'originale della
     * visualizzazione o dell'immagine in memoria, bensì dell'immagine reale al suo stato
     * corrente*). Quelle misure sono esattamente [size], che [Resize.frameSize] ricava dal file
     * tenendo conto della posa e del ritaglio: un secondo conto sarebbe un numero da tenere
     * allineato.
     * ⚠️ **E toglie il gettone**, perché due campi pieni con le misure correnti sono il libero:
     * lasciarne uno acceso direbbe che una regola governa un ridimensionamento che non c'è.
     * ⚠️ **Scrive nei campi e non applica**: la finestra si conferma con 'Applica', come ogni
     * altra cosa che ci si scrive dentro.
     * ⚠️ **Senza le misure il comando non c'è**: là non si sa da dove si parte.
     */
    val reset = stringResource(R.string.look_resize_reset)
    val wideFocus = remember { FocusRequester() }
    val tallFocus = remember { FocusRequester() }
    val shareFocus = remember { FocusRequester() }

    /*
     * ⚠️ **Il cursore lampeggia nel primo campo disponibile**, che è la sua riga, e lo rifà a ogni
     * gettone toccato: chi sceglie 'Altezza' vuole scrivere là, non cercare il campo col dito.
     * ⚠️ **Dentro un `runCatching` perché un nodo che non c'è ancora va in errore**: i campi
     * cambiano con il modo, e l'effetto parte nello stesso giro in cui il campo nuovo nasce.
     */
    LaunchedEffect(shown, known) {
        val chi = when {
            shown == Resize.Mode.SHARE -> shareFocus
            side == Resize.Side.TALL -> tallFocus
            else -> wideFocus
        }
        runCatching { chi.requestFocus() }
    }

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
                commands = if (!known) emptyList() else listOf(
                    TitleCommand(
                        text = reset,
                        glyph = Icons.Filled.SettingsBackupRestore,
                        onTap = {
                            mode = Resize.Mode.FREE
                            wide = w0.toString()
                            tall = h0.toString()
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
                /*
                 * ⚠️⚠️ **LA MISURA DI PARTENZA SI SCRIVE SUBITO SOTTO IL TITOLO, ED È IL PRIMO
                 * PEZZO DELLA SUA RICHIESTA** (*sotto 'Ridimensiona' va indicata la dimensione di
                 * origine (o dello stato attuale, dopo un ritaglio; il ridimensionamento avviene
                 * per ultimo, DOPO il ritaglio)*). Senza, i due campi direbbero dei numeri senza
                 * dire da dove vengono.
                 */
                if (known) {
                    Text(
                        text = filled(stringResource(R.string.look_resize_now), w0, h0),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ModeRow(shown = shown, onPick = pick)
                if (shown == Resize.Mode.SHARE) {
                    OutlinedTextField(
                        value = share,
                        onValueChange = { scritto ->
                            share = scritto.filter { it.isDigit() }.take(RESIZE_DIGITS)
                        },
                        label = { Text(stringResource(R.string.look_resize_pct)) },
                        isError = plan == null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        shape = BOX_SHAPE,
                        modifier = Modifier.fillMaxWidth().focusRequester(shareFocus)
                    )
                } else {
                    /*
                     * ⚠️ **I due campi su una riga sola, col segno in mezzo e l'unità in coda**,
                     * che è la forma della sua richiesta (`[ ] × [ ] px`): scritti uno sopra
                     * l'altro si leggerebbero come due impostazioni invece che come una misura.
                     */
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = wide,
                            onValueChange = writeWide,
                            enabled = wideOn,
                            label = { Text(stringResource(R.string.resize_wide)) },
                            isError = wideOn && plan == null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            shape = BOX_SHAPE,
                            modifier = Modifier.weight(1f).focusRequester(wideFocus)
                        )
                        Text(text = BY.toString(), style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = tall,
                            onValueChange = writeTall,
                            enabled = tallOn,
                            label = { Text(stringResource(R.string.resize_tall)) },
                            isError = tallOn && plan == null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            shape = BOX_SHAPE,
                            modifier = Modifier.weight(1f).focusRequester(tallFocus)
                        )
                        Text(text = PX, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                when {
                    plan == null -> Hint(
                        stringResource(R.string.look_resize_range, range.first, range.last)
                    )
                    !known -> Unit
                    esito == null -> Hint(stringResource(R.string.look_resize_keep))
                    else -> Outcome(esito.first, esito.second)
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
 * La fila dei sei gettoni.
 *
 * ⚠️ **Scritta qui e non con `Choices`**, che è il pezzo delle impostazioni: là il gettone scelto
 * si disegna sempre acceso, e qui quello del libero non si accende mai (il perché vive in testa a
 * [ResizeDialog]). Il resto è lo stesso, [FlowRow] compreso, perché sei nomi non entrano in una
 * riga in nessuna lingua.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModeRow(shown: Resize.Mode, onPick: (Resize.Mode) -> Unit) {
    Text(
        text = stringResource(R.string.look_resize_mode),
        style = MaterialTheme.typography.titleSmall
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().oneOf()
    ) {
        for (modo in Resize.Mode.entries) {
            val acceso = modo == shown && modo != Resize.Mode.FREE
            FilterChip(
                selected = acceso,
                onClick = { onPick(modo) },
                label = { Text(stringResource(modeName(modo))) },
                modifier = Modifier.picked(acceso)
            )
        }
    }
}

/** Il nome di un modo, cioè quello che si legge sul suo gettone. */
private fun modeName(mode: Resize.Mode): Int = when (mode) {
    Resize.Mode.LONG -> R.string.resize_long
    Resize.Mode.SHORT -> R.string.resize_short
    Resize.Mode.WIDE -> R.string.resize_wide
    Resize.Mode.TALL -> R.string.resize_tall
    Resize.Mode.FREE -> R.string.look_resize_px
    Resize.Mode.SHARE -> R.string.resize_share
}

/** Una riga di servizio: l'intervallo ammesso, o che su quest'immagine il piano non fa niente. */
@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * L'anteprima: a che misura si arriva.
 *
 * ⚠️⚠️ **HA LA FORMA DELL'ANTEPRIMA DI 'Rinomina', ED È SUA RICHIESTA** (*Sotto i campi
 * compilabili, un'anteprima simile a quella di 'Rinomina', che metta in evidenza meglio le
 * dimensioni finali in W × H px*): il titoletto che dice che cosa si sta guardando, e sotto una
 * superficie incavata col risultato. ⚠️ **Riusa la stringa di là** (`rename_preview`), che dice
 * esattamente 'Anteprima' in tutte e ventotto le lingue: una parola nuova sarebbe la stessa cosa
 * scritta due volte.
 */
@Composable
private fun Outcome(w: Int, h: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.rename_preview),
            style = MaterialTheme.typography.labelLarge
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = BOX_SHAPE,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, hairline())
        ) {
            Text(
                text = measure(w, h),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * Una misura scritta come lui l'ha chiesta: i due numeri in **grassetto**, il segno in mezzo e
 * l'unità in coda.
 *
 * ⚠️ **Una funzione sola per le due righe**, quella della misura di partenza e quella
 * dell'anteprima: scritte due volte, la prima a divergere sarebbe quella che nessuno guarda.
 */
private fun measure(w: Int, h: Int): AnnotatedString = buildAnnotatedString {
    val forte = SpanStyle(fontWeight = FontWeight.Bold)
    withStyle(forte) { append(w.toString()) }
    append(" $BY ")
    withStyle(forte) { append(h.toString()) }
    append(" $PX")
}

/**
 * La misura infilata dentro una frase tradotta, al posto del suo segnaposto.
 *
 * ⚠️ **Il segnaposto si cerca invece di concatenare**: in una lingua che scrive la frase al
 * rovescio quel `%1$s` non è in fondo, e una concatenazione darebbe una riga sgrammaticata.
 * ⚠️ **Se manca, la misura va in coda**: una traduzione senza segnaposto è un difetto che
 * `tools/i18n-check.py` prende, e intanto la riga dice comunque quello che deve dire.
 */
internal fun filled(pattern: String, w: Int, h: Int): AnnotatedString {
    val segno = "\u0001"
    val testo = pattern.format(segno)
    val dove = testo.indexOf(segno)
    return buildAnnotatedString {
        if (dove < 0) {
            append(testo)
            append(' ')
            append(measure(w, h))
            return@buildAnnotatedString
        }
        append(testo.substring(0, dove))
        append(measure(w, h))
        append(testo.substring(dove + segno.length))
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

/**
 * Il segno fra le due misure.
 *
 * ⚠️ **È il segno di moltiplicazione e non la lettera `x`**, ed è quello che ha scritto lui
 * (`Dimensioni attuali: **1800**×**1200** px`): scritto come codice invece che a carattere pieno
 * perché una `x` e un `×` si somigliano abbastanza da scambiarli rileggendo.
 */
private const val BY = '×'

/** L'unità dei due campi, che non si traduce in nessuna lingua. */
private const val PX = "px"

/**
 * Quante cifre si accettano in un campo.
 *
 * ⚠️ **Cinque bastano al tetto dei pixel e avanzano alla percentuale**, e servono a non far
 * scrivere un numero così lungo da non entrare in un intero: il campo dice comunque quali valori
 * sono buoni, e il taglio è solo la rete.
 */
private const val RESIZE_DIGITS = 5

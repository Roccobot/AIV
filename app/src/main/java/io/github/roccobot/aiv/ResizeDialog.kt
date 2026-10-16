package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * La finestra di **'Ridimensiona'**: le misure di destinazione, la regola che le governa, e che
 * cosa ne viene fuori.
 *
 * La disposizione è la specifica dell'utente (voci `resize-ripristina`, `resize-finestra` e
 * `resize-finestra-2`, giri dalla 2.74 alla 2.82): la misura di partenza sotto il titolo, due campi
 * con le misure di destinazione, i gettoni dei modi, il risultato sotto i campi.
 *
 * ⚠️ **I due campi sono la verità, il gettone dice solo quale dei due comanda**: le proporzioni
 * si mantengono sempre, quindi i due numeri sono uno solo scritto due volte. Il valore del piano
 * si ricava ([Resize.valueOf]), e cambiare gettone non muove nessuna cifra.
 *
 * ⚠️ **'Pixel' è il gettone del libero e non si accende mai** (lettura dichiarata della specifica:
 * nessun gettone acceso quando si scrivono tutti e due i campi). Un gettone acceso direbbe che c'è
 * una regola; il gettone resta per tornare al libero, e il riscontro è il campo che si riaccende.
 *
 * È una modale vera (`Modifier.lowered(null)` e `properties = loweredWindow(null)`): raccoglie un
 * input scritto (`Rules.md` § '👆 Che cosa fa il tocco FUORI da una finestra').
 *
 * ⚠️ **'Applica' accende l'interruttore** (risposta `editor` dell'utente a `d-resize-dove`): chi
 * entra a configurare ha già detto che lo vuole, e l'interruttore serve a spegnerlo dopo. Il tasto
 * dice 'Applica' e non 'OK' (lettura dichiarata): dice quello che succede, e la stringa esiste già.
 *
 * ⚠️ **'Applica' vale finché l'app è aperta; 'Rendi predefinito' scrive il piano nelle
 * preferenze**, per i salvataggi dopo un riavvio. Il riscontro di 'Rendi predefinito' è il tasto
 * che si disattiva: un avviso lo disegnerebbe la radice dell'app, dietro questa finestra.
 * Disattivato e non assente: un tasto che compare e sparisce farebbe muovere la riga del titolo.
 */
@Composable
fun ResizeDialog(
    /** Il piano da cui si parte: quello salvato, anche se l'interruttore è spento. */
    initial: Resize.Plan,
    /**
     * Il piano predefinito, cioè quello scritto nelle preferenze.
     *
     * Serve solo a disattivare 'Rendi predefinito' quando non c'è niente da scrivere.
     */
    saved: Resize.Plan,
    /**
     * Le misure su cui lavorerà il salvataggio, o `null` finché non si sanno.
     *
     * ⚠️ **Possono mancare**: si leggono dal file, e per un indirizzo remoto o un formato che il
     * decodificatore non riconosce non arrivano. Allora resta la sola percentuale, l'unico modo che
     * non ha bisogno di sapere da dove si parte.
     * Possono arrivare dopo l'apertura: per questo i campi li riempie un effetto.
     */
    size: Pair<Int, Int>?,
    onDismiss: () -> Unit,
    onApply: (Resize.Plan) -> Unit,
    /** Il piano diventa il predefinito, cioè quello con cui si riparte al prossimo avvio. */
    onDefault: (Resize.Plan) -> Unit
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

    // Il modo che si vede, che senza le misure è la percentuale: lo stato resta quello scelto,
    // perché le misure possono arrivare un istante dopo.
    val shown = if (known) mode else Resize.Mode.SHARE

    // I campi partono dalle misure di destinazione, che col piano di fabbrica ([Resize.NONE])
    // sono quelle correnti. Un effetto, perché le misure possono arrivare dopo l'apertura.
    LaunchedEffect(size) {
        if (!known) return@LaunchedEffect
        val arrivo = initial.sizeFor(w0, h0) ?: (w0 to h0)
        wide = arrivo.first.toString()
        tall = arrivo.second.toString()
    }

    val side = Resize.edge(shown, w0, h0)
    val wideOn = shown != Resize.Mode.SHARE && side != Resize.Side.TALL
    val tallOn = shown != Resize.Mode.SHARE && side != Resize.Side.WIDE

    // Scrivendo in un campo l'altro si aggiorna, perché le proporzioni si mantengono. Il conto è
    // in [Resize.mate], fuori dalla finestra, così il banco lo prova chiamandolo.
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

    // ⚠️ Fra i cinque modi in pixel cambiare gettone non muove i campi; passando alla
    // percentuale, o tornandone, il numero va tradotto, perché è un'altra unità.
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
     * ⚠️ **'Ripristina' riporta alle misure dell'immagine com'è adesso** (voce
     * `resize-ripristina`): sono [size], che [Resize.frameSize] ricava dal file con la posa e il
     * ritaglio. Torna al libero, scrive nei campi e non applica. Senza le misure non c'è.
     */
    val reset = stringResource(R.string.look_resize_reset)
    val azzera = {
        mode = Resize.Mode.FREE
        wide = w0.toString()
        tall = h0.toString()
    }
    val wideFocus = remember { FocusRequester() }
    val tallFocus = remember { FocusRequester() }
    val shareFocus = remember { FocusRequester() }

    /*
     * Il cursore va nel primo campo disponibile, e ci torna a ogni gettone toccato: chi sceglie
     * 'Altezza' vuole scrivere là.
     * ⚠️ In un `runCatching` perché un nodo che non c'è ancora va in errore: l'effetto parte
     * nello stesso giro in cui il campo nuovo compare.
     */
    LaunchedEffect(shown, known) {
        val chi = when {
            shown == Resize.Mode.SHARE -> shareFocus
            side == Resize.Side.TALL -> tallFocus
            else -> wideFocus
        }
        runCatching { chi.requestFocus() }
    }

    // Da 1.024 dp parametri e risultato sono affiancati (mockup `resize`), e la finestra si
    // allarga a [Adaptive.dialogBesideMaxWidth]; sotto restano in colonna.
    val widthDp = windowWidthDp()
    val beside = Adaptive.previewBeside(widthDp)
    val dialogW = if (beside) Adaptive.dialogBesideMaxWidth else Adaptive.dialogMaxWidth
    val previewOnStart = LocalPadLook.current.hand == Hand.RIGHT

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(null, maxWidth = dialogW),
        properties = loweredWindow(null),
        // Il comando è sulla riga del titolo, il posto dei comandi di una finestra: [TitleRow]
        // sceglie da sé fra la pastiglia scritta e l'icona, misurando lo spazio.
        title = {
            // Il sottotitolo è la sola misura di partenza, senza dicitura e senza grassetto
            // (voce `resize-finestra`).
            TitleRow(
                title = stringResource(R.string.look_resize),
                subtitle = if (known) AnnotatedString(plain(w0, h0)) else null,
                commands = if (!known) emptyList() else listOf(
                    TitleCommand(
                        text = stringResource(R.string.look_resize_default),
                        glyph = Glyphs.Material.PushPin,
                        onTap = { plan?.let(onDefault) },
                        enabled = plan != null &&
                            Resize.portable(plan.mode) &&
                            plan != saved,
                        lines = 2
                    )
                )
            )
        },
        text = {
            // Lo scorrimento serve a tastiera aperta: il pannello si accorcia e non si taglia.
            @Composable
            fun Fields() {
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
                     * I due campi su una riga, col segno in mezzo e l'unità in coda: si leggono
                     * come una misura e non come due impostazioni.
                     * ⚠️ **Il segno e l'unità si allineano alle cifre, non al campo** (voce
                     * `resize-finestra`): l'etichetta occupa la parte alta del campo, quindi il
                     * centro della riga di testo è a 20 punti dal fondo (8 di margine più metà
                     * dei 24 di `bodyLarge`). Un segno in `titleMedium`, che ha la stessa
                     * altezza di riga, appoggiato al fondo con [FIELD_TEXT_DROP] ha lo stesso
                     * centro.
                     */
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
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
                        Text(
                            text = BY.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = FIELD_TEXT_DROP)
                        )
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
                        Text(
                            text = PX,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = FIELD_TEXT_DROP)
                        )
                    }
                }
            }

            @Composable
            fun Result() {
                when {
                    plan == null -> Hint(
                        stringResource(R.string.look_resize_range, range.first, range.last)
                    )
                    !known -> Unit
                    esito == null -> Hint(stringResource(R.string.look_resize_keep))
                    else -> Outcome(esito.first, esito.second)
                }
            }

            @Composable
            fun Foot() {
                Text(
                    text = stringResource(R.string.look_resize_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                /*
                 * 'Ripristina' è in fondo a destra, dove arriva il pollice (voce
                 * `resize-finestra`): si tocca mentre si prova. È una pastiglia e non un
                 * `TextButton` accanto ad 'Applica', dove sembrerebbe un terzo tasto di conferma.
                 */
                if (known) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        TitlePill(
                            text = reset,
                            onTap = azzera,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }
            }

            if (beside) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    val fieldsMod = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    val resultMod = Modifier.weight(1f)
                    @Composable
                    fun Params() {
                        Column(
                            modifier = fieldsMod,
                            verticalArrangement = Arrangement.spacedBy(RESIZE_GAP)
                        ) {
                            Fields()
                            Foot()
                        }
                    }
                    @Composable
                    fun Side() {
                        Column(
                            modifier = resultMod,
                            verticalArrangement = Arrangement.spacedBy(RESIZE_GAP)
                        ) { Result() }
                    }
                    if (previewOnStart) {
                        Side()
                        Params()
                    } else {
                        Params()
                        Side()
                    }
                }
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(RESIZE_GAP)
                ) {
                    Fields()
                    Result()
                    Foot()
                }
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
 * La fila dei sei gettoni: due righe da tre, nell'ordine dell'utente.
 *
 * Scritta qui e non con `Choices` delle impostazioni, dove il gettone scelto è sempre acceso:
 * qui quello del libero non si accende mai ([ResizeDialog]).
 *
 * ⚠️ **Le righe sono due per costruzione** (voce `resize-finestra-2`: '%' sulla prima riga): con
 * una `FlowRow` decideva lo spazio, e per pochi punti il segno scendeva.
 * - ⚠️ **Le celle hanno un peso misurato sul proprio testo**: con tre colonne uguali una cella
 *   vale 85 punti e a 'Larghezza' ne servono 105. Chi ha una parola lunga riceve di più, e
 *   l'avanzo si distribuisce in proporzione, come nella fila delle forme del Ritaglio.
 * - Il corpo è `labelMedium`, un gradino sotto, perché le lingue dai nomi lunghi si tronchino
 *   più tardi.
 * - Dove la somma non ci sta (in francese, per esempio) si tronca: conta che il segno resti sulla
 *   prima riga e che la finestra non cresca.
 */
@Composable
private fun ModeRow(shown: Resize.Mode, onPick: (Resize.Mode) -> Unit) {
    val righello = rememberTextMeasurer()
    val corpo = MaterialTheme.typography.labelMedium
    val density = LocalDensity.current
    Column(
        modifier = Modifier.fillMaxWidth().oneOf(),
        verticalArrangement = Arrangement.spacedBy(MODE_GAP)
    ) {
        for (riga in Resize.Mode.entries.chunked(MODE_COLS)) {
            Row(horizontalArrangement = Arrangement.spacedBy(MODE_GAP)) {
                for (modo in riga) {
                    val acceso = modo == shown && modo != Resize.Mode.FREE
                    val nome = stringResource(modeName(modo))
                    // La percentuale si scrive col segno (voce `resize-finestra`), e il suo nome
                    // `resize_share` resta nella descrizione per il lettore di schermo.
                    val scritto = if (modo == Resize.Mode.SHARE) PCT else nome
                    val cella = with(density) {
                        righello.measure(
                            AnnotatedString(scritto),
                            corpo,
                            maxLines = 1
                        ).size.width.toDp()
                    } + MODE_PAD * 2
                    FilterChip(
                        selected = acceso,
                        onClick = { onPick(modo) },
                        label = {
                            Text(
                                text = scritto,
                                style = corpo,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier
                            .weight(cella.value)
                            .picked(acceso)
                            .semantics { if (scritto != nome) contentDescription = nome }
                    )
                }
            }
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
 * La forma è quella dell'anteprima di 'Rinomina' (richiesta dell'utente): il titoletto
 * 'Risultato', centrato, e sotto una superficie col risultato (voce `resize-finestra`).
 * I colori sono quelli della seconda pastiglia della rinomina, quella del nome nuovo
 * (`secondaryContainer` e filo d'accento). `NamePill` non si riusa: ha lo stringimento
 * anti-orfano e un corpo piccolo, e qui il testo è più grande per richiesta dell'utente.
 */
@Composable
private fun Outcome(w: Int, h: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.look_resize_result),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = BOX_SHAPE,
            color = MaterialTheme.colorScheme.secondaryContainer,
            border = BorderStroke(BOX_EDGE, MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = measure(w, h),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * Una misura come l'ha chiesta l'utente: i due numeri in grassetto, il segno in mezzo e l'unità
 * in coda.
 *
 * Il formato è scritto solo qui: la riga sotto il titolo ne prende il testo con [plain].
 */
private fun measure(w: Int, h: Int): AnnotatedString = buildAnnotatedString {
    val forte = SpanStyle(fontWeight = FontWeight.Bold)
    withStyle(forte) { append(w.toString()) }
    append(" $BY ")
    withStyle(forte) { append(h.toString()) }
    append(" $PX")
}

/**
 * La stessa misura senza grassetto, per la riga sotto il titolo: il testo di [measure] senza gli
 * stili.
 */
internal fun plain(w: Int, h: Int): String = measure(w, h).text

/**
 * Il lato lungo del file aperto, che è la sola misura vera che si legge senza decodificarlo.
 *
 * È qui perché lo chiedono tutte e due le schermate. È la metà di quello che vuole
 * [Resize.frameSize]: le proporzioni le sa l'anteprima.
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

/** L'aria fra due gettoni, in orizzontale come in verticale. */
private val MODE_GAP = 8.dp

/**
 * Quante colonne ha la fila dei modi.
 *
 * È anche la ripartizione in righe: sei modi, due righe. Un settimo modo farebbe una terza riga
 * da una cella sola.
 */
private const val MODE_COLS = 3

/**
 * Il riempimento orizzontale di un `FilterChip`, che Material non espone come costante pubblica.
 *
 * Serve al peso e non al disegno: una stima sbagliata sposterebbe solo un po' l'avanzo fra le
 * celle.
 */
private val MODE_PAD = 16.dp

/**
 * Il segno fra le due misure.
 *
 * Il segno di moltiplicazione e non la lettera `x`, come l'ha scritto l'utente.
 */
private const val BY = '×'

/** L'unità dei due campi, che non si traduce in nessuna lingua. */
private const val PX = "px"

/** Il gettone della percentuale: il segno e non la parola. */
private const val PCT = "%"

/**
 * Quanto il segno fra i due campi scende, per cadere sulle cifre e non sul campo: la distanza
 * fra il fondo di un campo e il fondo della riga scritta dentro (il conto è sulla `Row` che lo
 * usa).
 */
private val FIELD_TEXT_DROP = 8.dp

/**
 * Quante cifre si accettano in un campo.
 *
 * Cinque bastano al tetto dei pixel e alla percentuale, ed evitano un numero che non entra in un
 * intero: i valori buoni li dice il campo, il taglio è solo la rete.
 */
private const val RESIZE_DIGITS = 5

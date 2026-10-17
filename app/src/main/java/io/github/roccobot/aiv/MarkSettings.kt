package io.github.roccobot.aiv

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.DecimalFormatSymbols
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * La pagina 'Filigrana', dentro 'Editor e salvataggio': il file, dove cade e come è fatta. Se si
 * applica al salvataggio lo decide il tasto 'Filigrana' dell'editor, non questa pagina.
 *
 * È una sotto-pagina perché la famiglia supera la soglia (`Rules.md` § '⚙️ Dove va
 * un'impostazione, e chi la deve trovare'): una domanda sola, che logo scrivere sulle immagini.
 *
 * L'ordine è quello dell'utente (nota A del giro della 4.98, nota su `5.00-01`): il paragrafo che
 * spiega la filigrana, il file, la posizione con l'anteprima, i tre numeri, e in fondo l'avviso
 * sul senza perdita.
 *
 * ⚠️ **L'anteprima evita di scegliere i numeri alla cieca**: il riquadro riceve lo stesso
 * [Watermark.Plan] del salvataggio, con le correzioni di visibilità di [previewLook].
 */
@Composable
fun MarkPage(settings: Settings, onChange: (Settings) -> Unit) {
    val context = LocalContext.current
    // Le frasi degli avvisi da `LocalResources`, come nella griglia: il perché è là.
    val res = LocalResources.current
    val scope = rememberCoroutineScope()
    // ⚠️ Il contatore fa rileggere il disco: il file scelto è in `filesDir`, fuori dallo stato di
    // Compose, e dopo averlo adottato o tolto niente direbbe alla pagina di guardare di nuovo.
    var giro by remember { mutableIntStateOf(0) }
    val kind by produceState<Watermark.Kind?>(null, giro) {
        value = withContext(Dispatchers.IO) { Watermark.file(context)?.let(Watermark::kindOf) }
    }

    val lettura = remember { ActivityResultContracts.OpenDocument() }
    val scegli = rememberLauncherForActivityResult(lettura) { da ->
        if (da == null) return@rememberLauncherForActivityResult
        scope.launch {
            val fatto = Watermark.adopt(context, da)
            giro++
            if (!fatto) {
                Notices.say(res.getString(R.string.settings_mark_bad))
            } else {
                /*
                 * ⚠️ **Scegliere un file accende la filigrana** (voce `filigrana` dell'utente,
                 * punto 2, giro della 2.70), come 'Applica' accende il ridimensionamento
                 * ([ResizeDialog]). La pagina non ha un interruttore, e il tasto dell'editor senza
                 * un logo non compare. Solo a file adottato: un documento rifiutato non cambia
                 * niente.
                 */
                onChange(settings.copy(markOn = true))
            }
        }
    }

    val label = stringResource(R.string.settings_mark)
    val desc = stringResource(R.string.settings_mark_desc)
    /*
     * La pagina si apre col paragrafo e senza interruttore (nota dell'utente su `5.00-01`: qui si
     * impostano i parametri, l'accensione è del tasto dell'editor).
     * 'Filigrana' è fra i testi di ricerca del paragrafo, che in una ricerca compare senza la
     * pagina intorno.
     */
    val spiegazione = stringResource(R.string.settings_mark_on_desc)
    Searchable(spiegazione, label) {
        Detail(spiegazione, Modifier.padding(top = 8.dp, bottom = 4.dp))
    }
    Searchable(label, desc) {
        /*
         * I tipi si dichiarano al selettore (specifica dell'utente: PNG o SVG), che mostra solo i
         * file usabili. ⚠️ Il controllo vero è sui byte ([Watermark.adopt]): un fornitore può
         * dichiarare quello che vuole.
         */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = kind?.suffix?.uppercase() ?: stringResource(R.string.settings_mark_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (kind != null) {
                TextButton(onClick = {
                    Watermark.forget(context)
                    giro++
                }) { Text(stringResource(R.string.settings_mark_clear)) }
            }
            // 'Seleziona' è una stringa della sola pagina: `settings_editor_pick` è il tasto della
            // riga dell'editor, che dice 'Imposta app'.
            TextButton(onClick = { scegli.launch(arrayOf(PNG_MIME, SVG_MIME)) }) {
                Text(stringResource(R.string.settings_mark_pick))
            }
        }
    }

    /*
     * L'anteprima è anche la scelta del posto ([MarkSpot]), quindi c'è anche senza un logo.
     * Da 1.024 dp i parametri sono accanto al riquadro (mockup `resize`), dal lato che segue
     * [Hand]; sotto restano in colonna.
     */
    val widthDp = windowWidthDp()
    val beside = Adaptive.previewBeside(widthDp)
    val previewOnStart = LocalPadLook.current.hand == Hand.RIGHT

    @Composable
    fun Spot() {
        MarkSpot(
            plan = settings.markPlan(),
            giro = giro,
            onSpot = { onChange(settings.copy(markSpot = it)) }
        )
    }

    @Composable
    fun Params() {
        // I tre numeri sono tre chiamate allo stesso pezzo: campo, cursore e la loro sincronia.
        MarkNumber(
            label = stringResource(R.string.settings_mark_size),
            value = settings.markSize,
            range = Watermark.SIZE,
            onChange = { onChange(settings.copy(markSize = it)) }
        )
        // La distanza dal bordo ammette un decimale (richiesta dell'utente, 2.76): il valore resta
        // un intero, in decimi di centesimo ([Watermark.AIR_STEP]).
        MarkNumber(
            label = stringResource(R.string.settings_mark_air),
            value = settings.markAir,
            range = Watermark.AIR,
            step = Watermark.AIR_STEP,
            onChange = { onChange(settings.copy(markAir = it)) }
        )
        MarkNumber(
            label = stringResource(R.string.settings_mark_alpha),
            value = settings.markAlpha,
            range = Watermark.ALPHA,
            onChange = { onChange(settings.copy(markAlpha = it)) }
        )
    }

    if (beside) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (previewOnStart) {
                Column(modifier = Modifier.weight(1f)) { Spot() }
                Column(modifier = Modifier.weight(1f)) { Params() }
            } else {
                Column(modifier = Modifier.weight(1f)) { Params() }
                Column(modifier = Modifier.weight(1f)) { Spot() }
            }
        }
    } else {
        Spot()
        Params()
    }

    // L'avviso sul senza perdita chiude la pagina (nota A del giro della 4.98, testo dell'utente).
    val avviso = stringResource(R.string.settings_mark_lossy)
    Searchable(avviso, label) {
        Text(
            text = avviso,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)
        )
    }
}

/**
 * La stessa pagina, aperta sopra l'editor dal tocco lungo sul tasto 'Filigrana'.
 *
 * ⚠️ **Una scheda e non un cambio di schermata** (voce `mark-imposta` dell'utente, 2.75):
 * Indietro deve tornare all'editor aperto. Il lavoro dell'editor (cursori, storia dei passi,
 * inquadratura) è in un `remember` e non in un `rememberSaveable`, quindi una schermata che esce
 * di scena lo perderebbe; con una scheda l'editor non esce mai. Da qui non si raggiungono le altre
 * impostazioni.
 */
@Composable
fun MarkSheet(settings: Settings, onChange: (Settings) -> Unit, onDismiss: () -> Unit) {
    Sheet(title = stringResource(R.string.settings_mark), onDismiss = onDismiss) {
        MarkPage(settings = settings, onChange = onChange)
    }
}

/**
 * Il piano che queste preferenze descrivono, cioè quello che il salvataggio riceverà.
 *
 * Non guarda l'interruttore: dice com'è la firma, e se scriverla lo decide chi salva
 * (`ViewerViewModel.markPlan`). L'anteprima la disegna anche a filigrana spenta.
 */
private fun Settings.markPlan() =
    Watermark.Plan(spot = markSpot, size = markSize, air = markAir, alpha = markAlpha)

/**
 * Una misura della filigrana: un numero in centesimi, che si scrive a mano o si trascina.
 *
 * Il campo (voce `filigrana` dell'utente, giro della 2.70) ritrova una taratura fatta altrove,
 * per esempio quella di Lightroom; il cursore ne cerca una nuova guardando l'anteprima.
 *
 * ⚠️ **Il cursore scrive la preferenza quando il dito si alza, il campo a ogni numero valido**:
 * una strisciata passa per decine di valori, una cifra digitata è già un gesto compiuto. Quello
 * che si vede segue tutti e due subito, dallo stato locale.
 *
 * Il campo tiene il testo e non un numero: cancellando l'ultima cifra resta vuoto, senza uno zero
 * rimesso sotto le dita.
 *
 * [step] dice se il numero ha un decimale: la distanza dal bordo è in decimi di centesimo
 * ([Watermark.AIR_STEP]), misura e opacità in centesimi (`step` uno). ⚠️ **Il valore resta un
 * intero**: nelle preferenze viaggia il numero di passi, senza l'arrotondamento binario di un
 * `Float`.
 */
@Composable
private fun MarkNumber(
    label: String,
    value: Int,
    range: IntRange,
    step: Int = 1,
    onChange: (Int) -> Unit
) {
    Searchable(label) {
        // I due stati ripartono quando la preferenza cambia da fuori (la chiave è `value`), per
        // esempio col ripristino dei valori di fabbrica.
        var quanto by remember(value) { mutableIntStateOf(value) }
        var testo by remember(value) { mutableStateOf(markText(value, step)) }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            /*
             * ⚠️ **Un `BasicTextField` con la cornice di Material**, per un campo più compatto
             * (nota A del giro della 4.98): `OutlinedTextField` ha un'altezza minima di 56 punti e
             * un rientro di 16 che non espone; la `DecorationBox` prende il rientro come
             * parametro, e il campo scende a [NUM_FIELD_HIGH] con la stessa cornice, il '%' e lo
             * stato d'errore.
             */
            val tocco = remember { MutableInteractionSource() }
            // Il numero fuori corsa si segna e non si rifiuta: chi scrive '1' per arrivare a '14'
            // passa da un valore che la corsa non ammette.
            val fuori = markValue(testo, step) !in range
            BasicTextField(
                value = testo,
                onValueChange = { scritto ->
                    val pulito = markClean(scritto, step)
                    testo = pulito
                    val n = markValue(pulito, step)
                    if (n != null && n in range) {
                        quanto = n
                        if (n != value) onChange(n)
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                interactionSource = tocco,
                modifier = Modifier.width(NUM_FIELD).height(NUM_FIELD_HIGH),
                decorationBox = { campo ->
                    OutlinedTextFieldDefaults.DecorationBox(
                        value = testo,
                        innerTextField = campo,
                        enabled = true,
                        singleLine = true,
                        visualTransformation = VisualTransformation.None,
                        interactionSource = tocco,
                        isError = fuori,
                        suffix = { Text(PERCENT) },
                        contentPadding = OutlinedTextFieldDefaults.contentPadding(
                            top = NUM_FIELD_PAD,
                            bottom = NUM_FIELD_PAD
                        ),
                        container = {
                            OutlinedTextFieldDefaults.Container(
                                enabled = true,
                                isError = fuori,
                                interactionSource = tocco,
                                shape = BOX_SHAPE
                            )
                        }
                    )
                }
            )
        }
        ValueSlider(
            value = quanto.toFloat(),
            onValueChange = {
                quanto = it.roundToInt()
                testo = markText(quanto, step)
            },
            onValueChangeFinished = { if (quanto != value) onChange(quanto) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Quante cifre dopo il separatore consente un passo.
 *
 * Si ricava dal passo, che è una potenza di dieci ([Watermark.AIR_STEP] vale dieci): un secondo
 * parametro potrebbe non essere d'accordo col passo.
 */
private fun markDecimals(step: Int): Int = when {
    step >= 100 -> 2
    step >= 10 -> 1
    else -> 0
}

/**
 * Il separatore decimale della lingua del telefono.
 *
 * Non una virgola fissa: in italiano si scrive `0,5`, in inglese `0.5`. Scrivendo valgono tutti
 * e due ([markClean]).
 */
private fun decimalMark(): Char = DecimalFormatSymbols.getInstance().decimalSeparator

/**
 * Come un valore si scrive nel campo, col decimale che il suo passo consente.
 *
 * Il decimale a zero non si scrive: trenta decimi si leggono `3` e non `3,0`.
 */
internal fun markText(value: Int, step: Int): String {
    if (markDecimals(step) == 0) return value.toString()
    val intero = value / step
    val resto = value % step
    if (resto == 0) return intero.toString()
    return "$intero${decimalMark()}${resto.toString().padStart(markDecimals(step), '0')}"
}

/**
 * Quanto vale, in passi, quello che è scritto nel campo, o `null` se non è un numero.
 *
 * ⚠️ **Il punto e la virgola valgono uguale**: le tastiere numeriche di Android offrono l'uno o
 * l'altra secondo la lingua.
 * La coda si completa con degli zeri: `1,` vale uno.
 */
internal fun markValue(text: String, step: Int): Int? {
    val cifre = markDecimals(step)
    if (cifre == 0) return text.toIntOrNull()
    val pezzi = text.split('.', ',')
    if (pezzi.size > 2) return null
    val intero = if (pezzi[0].isEmpty()) 0 else pezzi[0].toIntOrNull() ?: return null
    if (pezzi.size == 1) return intero * step
    val coda = pezzi[1].padEnd(cifre, '0')
    if (coda.length > cifre) return null
    return intero * step + (coda.toIntOrNull() ?: return null)
}

/**
 * Che cosa resta di quello che è stato scritto: le sole cifre, e un separatore solo.
 *
 * Filtra e non rifiuta: un campo che respinge tutto il testo non dice quale carattere non andava.
 */
private fun markClean(text: String, step: Int): String {
    val cifre = markDecimals(step)
    if (cifre == 0) return text.filter { it.isDigit() }.take(NUM_DIGITS)
    val fuori = StringBuilder()
    var separato = false
    var decimali = 0
    for (c in text) {
        when {
            c.isDigit() && separato && decimali < cifre -> {
                fuori.append(c)
                decimali++
            }
            c.isDigit() && !separato && fuori.length < NUM_DIGITS -> fuori.append(c)
            (c == '.' || c == ',') && !separato -> {
                fuori.append(decimalMark())
                separato = true
            }
        }
    }
    return fuori.toString()
}

/**
 * Il posto della firma: cinque selettori intorno al riquadro che la mostra.
 *
 * La scelta è visuale (richiesta dell'utente, 2.76): selettori d'accento appena fuori dal
 * riquadro, che dicono il posto col posto. Per questo il riquadro c'è anche senza un logo: è il
 * comando.
 *
 * I selettori sono tutti nella stessa fascia esterna, larga [SPOT_RING]: i quattro angoli da
 * fuori, il centro in mezzo alla fascia di sotto ([spotAlign]). Così nessuno copre il riquadro.
 *
 * Il bersaglio è più grande del segno ([SPOT_TAP] contro [SPOT_ARM]): una squadretta larga quanto
 * il dito coprirebbe l'angolo che deve mostrare.
 */
@Composable
private fun MarkSpot(plan: Watermark.Plan, giro: Int, onSpot: (Watermark.Spot) -> Unit) {
    val label = stringResource(R.string.settings_mark_where)
    val names = Watermark.Spot.entries.map { spotName(it) }
    // I nomi dei posti restano fra i testi di ricerca, anche se non si scrivono: chi cerca
    // 'centro' cerca questa voce (come in [Choices]).
    Searchable(label, *names.toTypedArray()) {
        /*
         * Il riquadro è ridotto a [PREVIEW_SCALE], la fascia dei selettori no: le squadrette hanno
         * la misura chiesta dall'utente (2.78).
         * Il blocco è centrato e la nota è sotto, in un posto fisso (nota dell'utente su
         * `5.00-01`); l'aria sopra e sotto viene dalla nota su `5.01-01`.
         */
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val riquadro = (maxWidth - SPOT_RING * 2) * PREVIEW_SCALE + SPOT_RING * 2
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.width(riquadro).oneOf()) {
                    MarkPreview(plan, giro, label, Modifier.padding(SPOT_RING))
                    Watermark.Spot.entries.forEachIndexed { at, spot ->
                        SpotHandle(
                            spot = spot,
                            name = names[at],
                            chosen = spot == plan.spot,
                            onClick = { onSpot(spot) },
                            modifier = Modifier.align(spotAlign(spot))
                        )
                    }
                }
                PreviewNote(
                    stringResource(R.string.settings_mark_preview_note),
                    Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
                )
            }
        }
    }
}

/**
 * La nota che dice che l'anteprima non è la resa vera: piccola, grigia e centrata sotto il riquadro.
 *
 * Esiste perché l'anteprima cambia di proposito fondo, opacità e misura perché la firma si veda
 * ([previewLook]). Il testo è dell'utente.
 * ⚠️ **Il grigio è un terzo di inchiostro sul fondo** ([NOTE_INK]): col tema chiaro dà il
 * `#B1B1B1` del mockup dell'utente (nota su `4.99-01`), e un grigio di pari peso col tema scuro.
 * Il contrasto sotto la soglia è una sua scelta, per non allarmare.
 * In italiano e in inglese entra in una riga (nota su `5.00-01`), presidiato da
 * `FiligranaPaginaTest`; in una lingua più lunga va a capo, centrata.
 */
@Composable
private fun PreviewNote(text: String, modifier: Modifier) {
    val schema = MaterialTheme.colorScheme
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = lerp(schema.background, schema.onSurface, NOTE_INK),
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

/** Come si chiama un posto, nelle ventotto lingue. */
@Composable
private fun spotName(spot: Watermark.Spot): String = stringResource(
    when (spot) {
        Watermark.Spot.TOP_LEFT -> R.string.mark_spot_tl
        Watermark.Spot.TOP_RIGHT -> R.string.mark_spot_tr
        Watermark.Spot.BOTTOM_LEFT -> R.string.mark_spot_bl
        Watermark.Spot.BOTTOM_RIGHT -> R.string.mark_spot_br
        Watermark.Spot.CENTRE -> R.string.mark_spot_c
    }
)

/**
 * In quale punto della fascia esterna è il selettore di un posto.
 *
 * Il centro è in basso, come nel mockup dell'utente di `5.00-01`: in alto c'è 'Posizione della
 * filigrana', e lo stelo sale dal tondo verso il centro del riquadro ([centreStem]).
 */
private fun spotAlign(spot: Watermark.Spot): Alignment = when (spot) {
    Watermark.Spot.TOP_LEFT -> Alignment.TopStart
    Watermark.Spot.TOP_RIGHT -> Alignment.TopEnd
    Watermark.Spot.BOTTOM_LEFT -> Alignment.BottomStart
    Watermark.Spot.BOTTOM_RIGHT -> Alignment.BottomEnd
    Watermark.Spot.CENTRE -> Alignment.BottomCenter
}

/**
 * Un selettore: la squadretta di un angolo, o il tondo del centro.
 *
 * La piega della squadretta si ricava dalla fascia: l'angolo del riquadro cade a [SPOT_RING] dai
 * due lati esterni del bersaglio, e i quattro angoli sono lo stesso conto con due segni.
 *
 * Il tondo del centro è pieno quando è scelto e vuoto quando no, come la squadretta cambia
 * inchiostro: senza un logo il riquadro non dice quale posto è scelto.
 * Lo stelo del tondo (nota dell'utente su `5.00-01`) lo disegna [MarkPreview], sotto il logo
 * ([centreStem]); qui restano il tondo e, se acceso, il tratto fra il suo bordo e il riquadro,
 * che cade nella fascia.
 */
@Composable
private fun SpotHandle(
    spot: Watermark.Spot,
    name: String,
    chosen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val accento = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(SPOT_TAP)
            .selectable(selected = chosen, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tinta = spotInk(accento, chosen)
            val spesso = spotThick(chosen).toPx()
            val anello = SPOT_RING.toPx()
            if (spot == Watermark.Spot.CENTRE) {
                val centro = Offset(size.width / 2f, size.height - anello / 2f)
                val raggio = SPOT_DOT.toPx()
                if (chosen) {
                    drawCircle(tinta, radius = raggio, center = centro)
                    drawLine(
                        color = tinta,
                        start = Offset(centro.x, centro.y - raggio),
                        end = Offset(centro.x, size.height - anello),
                        strokeWidth = spesso
                    )
                } else {
                    drawCircle(tinta, radius = raggio, center = centro, style = Stroke(spesso))
                }
                return@Canvas
            }
            val sinistra = spot == Watermark.Spot.TOP_LEFT || spot == Watermark.Spot.BOTTOM_LEFT
            val sopra = spot == Watermark.Spot.TOP_LEFT || spot == Watermark.Spot.TOP_RIGHT
            val fuori = anello - SPOT_OUT.toPx()
            val ox = if (sinistra) fuori else size.width - fuori
            val oy = if (sopra) fuori else size.height - fuori
            val braccio = SPOT_ARM.toPx()
            val dx = if (sinistra) braccio else -braccio
            val dy = if (sopra) braccio else -braccio
            /*
             * ⚠️ **Un tracciato solo e non due linee** (nota D dell'utente, giro della 4.98): due
             * linee coi capi tondi coprirebbero due volte la piega, che con l'inchiostro a
             * [SPOT_FAINT] risulterebbe più scura. Il giunto tondo tiene la piega arrotondata.
             */
            val squadra = Path().apply {
                moveTo(ox + dx, oy)
                lineTo(ox, oy)
                lineTo(ox, oy + dy)
            }
            drawPath(squadra, tinta, style = Stroke(spesso, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** L'inchiostro di un selettore: l'accento pieno se il posto è scelto, spento se no. */
private fun spotInk(accent: Color, chosen: Boolean): Color =
    if (chosen) accent else accent.copy(alpha = SPOT_FAINT)

/** Lo spessore del segno di un selettore, scelto o no. */
private fun spotThick(chosen: Boolean): Dp = if (chosen) SPOT_THICK_ON else SPOT_THICK_OFF

/**
 * Lo stelo del tondo del centro, disegnato nell'anteprima: dal fondo del riquadro sale fino a
 * [STEM_SHORT] sotto il centro.
 *
 * ⚠️ **È nell'anteprima, sotto il logo, e si ferma prima del centro** (nota dell'utente su
 * `5.01-01`): un logo al centro deve coprirlo, non esserne coperto.
 * Spento, comincia un tratto sopra l'anello del tondo senza toccarlo: a [SPOT_FAINT] due tratti
 * sovrapposti farebbero una macchia più scura (come nella nota D del giro della 4.98). Acceso,
 * parte dal bordo del tondo pieno; il pezzo nella fascia lo disegna [SpotHandle].
 */
private fun DrawScope.centreStem(ink: Color, chosen: Boolean) {
    val spesso = spotThick(chosen).toPx()
    val x = size.width / 2f
    val tondo = size.height + SPOT_RING.toPx() / 2f
    val partenza = tondo - SPOT_DOT.toPx() - if (chosen) 0f else spesso
    drawLine(
        color = ink,
        start = Offset(x, partenza),
        end = Offset(x, size.height / 2f + STEM_SHORT.toPx()),
        strokeWidth = spesso,
        cap = StrokeCap.Round
    )
}

/**
 * Il riquadro che mostra dove la filigrana cadrà, quanto sarà grande e quanto si vedrà.
 *
 * ⚠️ **Legge il piano e non le preferenze**: lo stesso [Watermark.Plan] del salvataggio, quindi
 * posto e distanza dal bordo sono gli stessi. Fondo, opacità e misura invece li sceglie
 * [previewLook] perché la firma si veda (nota A del giro della 4.98), e la nota sotto lo dice.
 * La misura si ricava dal lato lungo del riquadro come sull'immagine vera: il disegno entra in un
 * quadrato di `size` centesimi del lato lungo, il margine vale `air` decimi di centesimo
 * ([Watermark.cornerFor]).
 * Il rapporto è 3:2, quello di una foto, e gli angoli sono quasi vivi ([PREVIEW_ROUND]).
 */
@Composable
private fun MarkPreview(plan: Watermark.Plan, giro: Int, label: String, modifier: Modifier) {
    val context = LocalContext.current
    // ⚠️ La chiave è il contatore della pagina: il disegno è in `filesDir`, fuori dallo stato di
    // Compose, e scegliendo un altro file niente direbbe a `produceState` di rileggerlo.
    // Non si rilegge alla misura: si rende a un lato fisso e lo rimpicciolisce il riquadro.
    val art by produceState<Pair<ImageBitmap, Float?>?>(null, giro) {
        value = withContext(Dispatchers.IO) {
            Watermark.artwork(context, PREVIEW_ART)?.let { it.asImageBitmap() to inkLuminance(it) }
        }
    }
    val descrizione = stringResource(R.string.settings_mark_preview)
    val grigio = MaterialTheme.colorScheme.surfaceVariant
    val accento = MaterialTheme.colorScheme.primary
    val aspetto = previewLook(art?.second, grigio.luminance(), plan)
    val fondo = when (aspetto.ground) {
        Ground.SURFACE -> grigio
        Ground.BLACK -> Color.Black
        Ground.WHITE -> Color.White
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 2f)
            .clip(RoundedCornerShape(PREVIEW_ROUND))
            .background(fondo)
            .semantics { contentDescription = descrizione }
    ) {
        // Prima del logo, perché il logo al centro lo copra ([centreStem]).
        Canvas(modifier = Modifier.matchParentSize()) {
            centreStem(spotInk(accento, plan.spot == Watermark.Spot.CENTRE), plan.spot == Watermark.Spot.CENTRE)
        }
        /*
         * 'Posizione della filigrana' è dentro il riquadro, verso l'alto e non al centro, dove
         * può cadere la firma (nota dell'utente su `4.99-01`). Il colore lo sceglie [labelInk]
         * dal fondo di [previewLook], la quota è [LABEL_BIAS]; corpo `labelMedium` (nota su
         * `5.01-01`).
         */
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelInk(fondo),
            modifier = Modifier.align(BiasAlignment(0f, LABEL_BIAS))
        )
        val disegno = art?.first ?: return@BoxWithConstraints
        val lungo = maxWidth
        val lato = lungo * (aspetto.size / 100f)
        Image(
            bitmap = disegno,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            // Opacità e misura sono quelle dell'anteprima e non del piano ([previewLook]): un logo
            // piccolo e smorzato non si vedrebbe.
            alpha = aspetto.alpha,
            modifier = Modifier
                .align(
                    when (plan.spot) {
                        Watermark.Spot.TOP_LEFT -> Alignment.TopStart
                        Watermark.Spot.TOP_RIGHT -> Alignment.TopEnd
                        Watermark.Spot.BOTTOM_LEFT -> Alignment.BottomStart
                        Watermark.Spot.BOTTOM_RIGHT -> Alignment.BottomEnd
                        Watermark.Spot.CENTRE -> Alignment.Center
                    }
                )
                // Al centro non c'è un bordo da cui tenersi lontani, come nel disegno vero.
                .padding(
                    if (plan.spot == Watermark.Spot.CENTRE) 0.dp
                    else lungo * (plan.air / (100f * Watermark.AIR_STEP))
                )
                .sizeIn(maxWidth = lato, maxHeight = lato)
        )
    }
}

/** Il fondo del riquadro dell'anteprima: il grigio della pagina, o il nero, o il bianco. */
internal enum class Ground { SURFACE, BLACK, WHITE }

/** Come l'anteprima mostra la firma: su quale fondo, con che opacità, a che misura in centesimi. */
internal data class PreviewLook(val ground: Ground, val alpha: Float, val size: Int)

/**
 * Come l'anteprima mostra la firma, dati la luminosità del suo inchiostro e quella del grigio.
 *
 * È l'esempio della nota A dell'utente, giro della 4.98, reso regola:
 * - il fondo resta il grigio finché il contrasto con l'inchiostro arriva a [PREVIEW_CONTRAST];
 *   sotto diventa il nero o il bianco, quello che contrasta di più (rapporto di contrasto del W3C
 *   fra luminanze relative);
 * - l'opacità è sempre piena, perché smorzare la firma la nasconde;
 * - la misura è doppia, con un tetto a [PREVIEW_SIZE_CAP] centesimi; una misura già oltre il
 *   tetto resta com'è.
 * Senza un logo il fondo è il grigio.
 * È una funzione pura e interna perché il banco la chiami senza decodificare un disegno.
 */
internal fun previewLook(ink: Float?, surface: Float, plan: Watermark.Plan): PreviewLook {
    val misura = minOf(plan.size * 2, maxOf(plan.size, PREVIEW_SIZE_CAP))
    if (ink == null || contrast(ink, surface) >= PREVIEW_CONTRAST) {
        return PreviewLook(Ground.SURFACE, 1f, misura)
    }
    val fondo = if (contrast(ink, 0f) >= contrast(ink, 1f)) Ground.BLACK else Ground.WHITE
    return PreviewLook(fondo, 1f, misura)
}

/**
 * L'inchiostro di 'Posizione della filigrana' dentro il riquadro: un grigio a contrasto
 * [LABEL_CONTRAST] col fondo [ground].
 *
 * Segue il fondo che [previewLook] ha scelto per la firma (nota dell'utente su `4.99-01`). Il
 * contrasto è 3 e non il massimo, perché la parola non prevalga sulla firma.
 * Più scuro del fondo quando il fondo lascia posto a un grigio a quel contrasto; sul nero e su un
 * grigio scuro si va verso il chiaro.
 */
internal fun labelInk(ground: Color): Color {
    val fondo = ground.luminance()
    val sotto = (fondo + 0.05f) / LABEL_CONTRAST - 0.05f
    val sopra = (fondo + 0.05f) * LABEL_CONTRAST - 0.05f
    return greyOf(if (sotto >= 0f) sotto else minOf(sopra, 1f))
}

/** Il grigio sRGB che ha la luminanza relativa [luminance]: l'inverso della linearizzazione. */
private fun greyOf(luminance: Float): Color {
    val c = if (luminance <= 0.0031308f) luminance * 12.92f
    else 1.055f * luminance.toDouble().pow(1 / 2.4).toFloat() - 0.055f
    return Color(c, c, c)
}

/** Il rapporto di contrasto del W3C fra due luminanze relative. */
private fun contrast(a: Float, b: Float): Float = (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)

/**
 * La luminanza relativa media dell'inchiostro di un disegno, pesata sull'opacità, o `null` se il
 * disegno è tutto trasparente.
 *
 * Pesata sull'opacità, perché i pixel trasparenti valgono nero e non sono inchiostro: un logo
 * bianco su fondo trasparente risulterebbe scuro.
 * Le componenti si linearizzano prima della media, come vuole la luminanza relativa.
 */
internal fun inkLuminance(bitmap: Bitmap): Float? {
    val w = bitmap.width
    val h = bitmap.height
    val pixel = IntArray(w * h)
    bitmap.getPixels(pixel, 0, w, 0, 0, w, h)
    var somma = 0.0
    var peso = 0.0
    for (p in pixel) {
        val a = (p ushr 24) / 255.0
        if (a == 0.0) continue
        val l = 0.2126 * linear((p shr 16) and 0xFF) + 0.7152 * linear((p shr 8) and 0xFF) +
            0.0722 * linear(p and 0xFF)
        somma += l * a
        peso += a
    }
    return if (peso == 0.0) null else (somma / peso).toFloat()
}

/** Una componente sRGB da 0 a 255, portata in luce lineare. */
private fun linear(c: Int): Double {
    val v = c / 255.0
    return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
}

/**
 * Il contrasto minimo fra l'inchiostro della firma e il fondo del riquadro.
 *
 * Il 3:1 che il W3C chiede agli elementi grafici (WCAG 2.1, criterio 1.4.11), non il 4,5:1 del
 * testo: una firma è un disegno.
 */
private const val PREVIEW_CONTRAST = 3f

/** Il tetto della misura raddoppiata dell'anteprima, in centesimi del lato lungo. */
private const val PREVIEW_SIZE_CAP = 50

/**
 * Quanto è grande il riquadro dell'anteprima rispetto alla larghezza che ha a disposizione.
 *
 * L'80% è la misura del mockup dell'utente (nota su `5.00-01`), dove il blocco occupa l'81%
 * della pagina.
 */
private const val PREVIEW_SCALE = 0.8f

/**
 * Quanto inchiostro ha la nota sotto il riquadro, sul fondo della pagina.
 *
 * Un terzo dà `#B1B1B1` col tema chiaro, il grigio del mockup ([PreviewNote]).
 */
private const val NOTE_INK = 1f / 3f

/**
 * La quota di 'Posizione' dentro il riquadro, da -1 (in cima) a 1 (in fondo).
 *
 * -0,7 mette il centro della parola al 15% dell'altezza, come nel mockup: sopra il centro, dove
 * può cadere la firma, e lontano dagli angoli.
 */
private const val LABEL_BIAS = -0.7f

/**
 * Il contrasto di 'Posizione della filigrana' col fondo del riquadro.
 *
 * 3 è la richiesta dell'utente (nota su `5.01-01`), ed è anche la soglia del W3C per gli
 * elementi grafici, come [PREVIEW_CONTRAST].
 */
private const val LABEL_CONTRAST = 3f

/**
 * Quanto lo stelo del centro si ferma prima del centro del riquadro.
 *
 * 12 è nel mezzo dei 10-15 chiesti dall'utente (nota su `5.01-01`): arrivando al centro, lo
 * stelo coprirebbe la firma posata lì.
 */
private val STEM_SHORT = 12.dp

/** Il lato lungo a cui si disegna la filigrana per l'anteprima. */
private const val PREVIEW_ART = 512

/**
 * Il raggio degli angoli del riquadro dell'anteprima.
 *
 * Due punti (richiesta dell'utente, 2.76): il riquadro rappresenta una foto, che non ha angoli
 * tondi; zero lo farebbe sembrare estraneo a una pagina in cui ogni superficie è stondata.
 */
private val PREVIEW_ROUND = 2.dp

/**
 * Quanto è larga la fascia intorno al riquadro in cui sono i cinque selettori.
 *
 * È il solo numero che lega il riquadro ai selettori: il rientro dell'anteprima e la piega delle
 * squadrette si ricavano tutti e due da lui.
 * Vale 22 perché i selettori sono spessi e visibili (voce `mark-posto` dell'utente, 2.78), e la
 * fascia si prende dal riquadro.
 */
private val SPOT_RING = 22.dp

/** Quanto è grande il bersaglio di un selettore: il dito, non il segno. */
private val SPOT_TAP = 44.dp

/**
 * Quanto sono lunghi i due bracci di una squadretta, lungo i lati del riquadro.
 *
 * Vale quanto la fascia: una squadretta spessa con bracci corti sarebbe tozza.
 */
private val SPOT_ARM = 22.dp

/** Quanto la piega di una squadretta è fuori dall'angolo del riquadro, in diagonale. */
private val SPOT_OUT = 5.dp

/**
 * Il raggio del tondo che sceglie il centro.
 *
 * Il tondo ha circa la misura di una squadretta (voce `mark-posto` dell'utente, 2.78): 18 di
 * diametro contro i 22 di un braccio.
 * ⚠️ **Non arriva a 22**: il tondo è dentro la fascia, e raggio più mezzo tratto (9 più 1,5 col tratto
 * spento) deve restare entro metà di [SPOT_RING], 11, per non entrare nel riquadro.
 */
private val SPOT_DOT = 9.dp

/**
 * Lo spessore del segno di un posto scelto, e di uno che non lo è.
 *
 * Spessi per la voce `mark-posto` dell'utente (2.78); quale è scelto lo dicono lo spessore e
 * l'inchiostro insieme.
 */
private val SPOT_THICK_ON = 5.dp
private val SPOT_THICK_OFF = 3.dp

/**
 * Quanto si spegne il segno di un posto non scelto.
 *
 * Attenuato e non di un altro colore: tutti e cinque sono d'accento, come ha chiesto l'utente.
 * Abbastanza vivo da dire che si può toccare (voce `mark-posto`, 2.78).
 */
private const val SPOT_FAINT = 0.55f

/** I due tipi che il selettore di sistema mostra: vedi [Watermark.Kind]. */
private const val PNG_MIME = "image/png"
private const val SVG_MIME = "image/svg+xml"

/**
 * Quanto è largo il campo dei tre numeri.
 *
 * Fissa e non lasciata al contenuto: tre campi larghi quanto il loro numero sarebbero tre colonne
 * diverse, e il valore cambia mentre si trascina.
 */
private val NUM_FIELD = 104.dp

/**
 * Quanto è alto il campo dei tre numeri, e il suo rientro verticale.
 *
 * 44 e non i 56 di Material (nota A del giro della 4.98): col rientro di 10 per lato resta una
 * riga di `bodyLarge`, alta 24.
 */
private val NUM_FIELD_HIGH = 44.dp
private val NUM_FIELD_PAD = 10.dp

/** Quante cifre si accettano: le corse arrivano a cento, e il taglio è la rete. */
private const val NUM_DIGITS = 3


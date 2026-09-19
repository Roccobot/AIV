package io.github.roccobot.aiv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.DecimalFormatSymbols
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * La pagina **'Filigrana'**: il file, dove cade, come è fatta, e se si applica al salvataggio.
 *
 * ⚠️⚠️ **È UNA SOTTO-PAGINA PERCHÉ LA FAMIGLIA HA SUPERATO LA SOGLIA** (`CLAUDE.md`, § '⚙️ Dove va
 * un'impostazione, e chi la deve trovare'): la domanda è una sola, *che logo scrivo sulle immagini
 * che salvo*, e le voci sono sei più l'anteprima, cioè oltre il *2-3* della soglia dell'utente.
 * ⚠️⚠️ **E DALLA `2.71` VIVE DAVVERO DENTRO 'Editor e salvataggio', CHE PRIMA ERA SOLO SCRITTO**
 * (voce `filigrana` del giro della `2.70`, punto 1): la porta che la apre stava nella pagina
 * **radice**, sotto 'Aspetto', mentre il commento di questo file la dava già dov'è adesso. Cioè
 * il codice e la nota dicevano due cose diverse, e a vedersi era il codice.
 *
 * ⚠️⚠️ **L'ORDINE È IL SUO: PRIMA SI DESCRIVE LA FIRMA, POI SI DICE SE APPLICARLA** (stesso punto:
 * *'Posizione' deve stare sopra 'Applica al salvataggio'*). Quindi l'interruttore chiude la
 * pagina invece di spezzarla in due: le righe sopra rispondono a *com'è fatta*, e lui
 * risponde a *la scrivo?*.
 *
 * ⚠️⚠️ **L'ANTEPRIMA NON È UN ORNAMENTO: SENZA DI LEI I NUMERI SI SCEGLIEREBBERO ALLA CIECA.**
 * Posizione, misura, distanza dal bordo e opacità si vedono sul file salvato, cioè dopo, e
 * provarle vorrebbe dire salvare un'immagine per ogni tentativo. Qui il riquadro riceve **lo
 * stesso** [Watermark.Plan] del salvataggio, quindi quello che si vede è quello che si avrà.
 */
@Composable
fun MarkPage(settings: Settings, onChange: (Settings) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    /*
     * ⚠️ **Il contatore è quello che fa rileggere il disco**, e non un capriccio: il file scelto
     * vive in `filesDir`, cioè fuori dallo stato di Compose, quindi dopo averlo adottato o tolto
     * niente direbbe a questa pagina di guardare di nuovo. Lo stesso schema degli stili.
     */
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
                Notices.say(context.getString(R.string.settings_mark_bad))
            } else {
                /*
                 * ⚠️⚠️ **SCEGLIERE UN FILE ACCENDE L'INTERRUTTORE, DALLA `2.71`, ED È SUA
                 * RICHIESTA** (voce `filigrana`, punto 2). Chi entra qui e sceglie un logo ha
                 * detto che cosa vuole scrivere sulle immagini, e lasciare la firma spenta
                 * vorrebbe dire una seconda riga da toccare perché il gesto appena fatto valga
                 * qualcosa. È la stessa lettura dell''Applica' del ridimensionamento, che
                 * accende il suo (§ [ResizeDialog]).
                 * ⚠️ **Solo quando il file è stato adottato davvero**: un documento rifiutato
                 * lascia le cose com'erano, interruttore compreso.
                 * ⚠️ **Spegnerla resta un gesto**, e non si riaccende da sé: questo scatta sulla
                 * scelta di un file, che si fa una volta.
                 */
                onChange(settings.copy(markOn = true))
            }
        }
    }

    val label = stringResource(R.string.settings_mark)
    val desc = stringResource(R.string.settings_mark_desc)
    Searchable(label, desc) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
            Detail(desc)
        }
        /*
         * ⚠️⚠️ **I TIPI SI DICHIARANO AL SELETTORE, ED È LA SUA SPECIFICA** (*Input PNG o SVG*):
         * così il navigatore di sistema mostra i soli file che si possono usare, invece di
         * lasciar scegliere un JPEG e rispondere di no dopo. ⚠️ **Il controllo vero resta sui
         * byte** (vedi [Watermark.adopt]): un fornitore di documenti può dichiarare quello che
         * vuole, e chi sceglie 'tutti i file' arriva qui lo stesso.
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
            TextButton(onClick = { scegli.launch(arrayOf(PNG_MIME, SVG_MIME)) }) {
                Text(stringResource(R.string.settings_editor_pick))
            }
        }
    }

    /*
     * ⚠️⚠️ **DALLA `2.76` L'ANTEPRIMA E LA SCELTA DEL POSTO SONO LA STESSA COSA, e la nota che
     * diceva 'il riquadro c'è solo quando c'è una filigrana' è decaduta**: senza un logo scelto il
     * riquadro resta vuoto, ma porta comunque i cinque selettori, cioè dice qualcosa. Il perché
     * per esteso vive su [MarkSpot].
     */
    MarkSpot(
        plan = settings.markPlan(),
        giro = giro,
        onSpot = { onChange(settings.copy(markSpot = it)) }
    )

    /*
     * ⚠️⚠️ **I TRE NUMERI SONO TRE CHIAMATE ALLO STESSO PEZZO, E NON TRE BLOCCHI COPIATI**: ognuno
     * porta un campo, un cursore e la loro sincronia, e scritti riga per riga sarebbero nove
     * occasioni di sbagliarne una. È lo stesso criterio per cui i sei cursori della Luce sono una
     * tabella.
     */
    MarkNumber(
        label = stringResource(R.string.settings_mark_size),
        value = settings.markSize,
        range = Watermark.SIZE,
        onChange = { onChange(settings.copy(markSize = it)) }
    )

    /*
     * ⚠️⚠️ **LA DISTANZA DAL BORDO SI SCRIVE COL DECIMALE, DALLA `2.76`, ED È SUA RICHIESTA**
     * (punto 1 del campo libero del giro dalla `2.71` alla `2.74`: *Filigrana / Distanza dal bordo:
     * aggiungi i valori 0,2 e 0,5*). A viaggiare è sempre un intero, perché il valore è in **decimi
     * di centesimo** ([Watermark.AIR_STEP]): quello che cambia è come si legge e come si scrive.
     */
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

    /*
     * ⚠️⚠️ **L'INTERRUTTORE CHIUDE LA PAGINA DALLA `2.71`, E FINO ALLA `2.70` LA APRIVA**: è la
     * sua richiesta (*'Posizione' deve stare sopra 'Applica al salvataggio'*), e la ragione che la
     * regge è che le righe sopra descrivono **com'è fatta** la firma, mentre questa
     * dice se scriverla. Messa in testa, si leggeva come la prima di sei voci pari.
     */
    SwitchRow(
        label = stringResource(R.string.settings_mark_on),
        detail = stringResource(R.string.settings_mark_on_desc),
        checked = settings.markOn,
        onChange = { onChange(settings.copy(markOn = it)) }
    )
}

/**
 * La stessa pagina, aperta **sopra l'editor** dal tocco lungo sul tasto 'Filigrana'.
 *
 * ⚠️⚠️ **DALLA `2.75` QUEL GESTO NON NAVIGA PIÙ, ED È LA SUA RICHIESTA LETTA FINO IN FONDO**
 * (voce `mark-imposta` accettabile: *quando entro nelle impostazioni della filigrana con il tocco
 * lungo poi se torno indietro deve tornare direttamente nell'editor aperto, senza rifare il giro
 * dalle impostazioni alla home e di nuovo all'editor*). Fino alla `2.74` quel gesto **cambiava
 * schermata**, quindi Indietro risaliva la pila delle impostazioni e usciva nel visualizzatore.
 * ⚠️⚠️ **E RIPORTARLO ALL'EDITOR NON SAREBBE BASTATO, CHE È LA RAGIONE DELLA SCHEDA**: il lavoro
 * dell'editor (i cursori, la storia dei passi, l'inquadratura) vive in un `remember` e non in un
 * `rememberSaveable`, quindi una schermata che esce di scena se lo porta via. Chi fosse tornato
 * 'nell'editor' l'avrebbe trovato **vuoto**, cioè peggio del giro che si è lamentato di fare. Con
 * una scheda l'editor non esce mai di scena, e non c'è nessuno stato da conservare.
 * ⚠️ **Così i due tasti diventano gemelli anche nel gesto lungo**: quello del ridimensionamento
 * apre la propria finestra sopra l'editor da sempre, e adesso lo fa anche questo.
 * ⚠️ **Quello che si perde si dichiara**: da qui non si gira nelle altre impostazioni, perché
 * questa scheda è la sola pagina che il gesto apre. La porta di sempre resta il pannello.
 *
 * ⚠️ **Nessuna stringa nuova**: il titolo è quello della voce, cioè 'Filigrana'.
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
 * ⚠️ **Non guarda l'interruttore**: dice **com'è** la firma, e se scriverla lo decide chi salva
 * (`ViewerViewModel.markPlan`). Serve all'anteprima, che deve disegnare quello che si sta
 * tarando anche mentre la firma è spenta.
 */
private fun Settings.markPlan() =
    Watermark.Plan(spot = markSpot, size = markSize, air = markAir, alpha = markAlpha)

/**
 * Una misura della filigrana: un numero in centesimi, che si scrive a mano o si trascina.
 *
 * ⚠️⚠️ **IL CAMPO C'È PERCHÉ LO HA CHIESTO LUI, E IL CURSORE PERCHÉ IL CAMPO NON BASTA** (voce
 * `filigrana` del giro della `2.70`: *dimensione relativa da inserire a mano, che serve a chi come
 * me vuole riprodurre esattamente la firma di Lightroom*). Scrivere il numero è il solo modo di
 * ritrovare una taratura fatta altrove; trascinarlo è il solo modo di cercarne una nuova
 * guardando l'anteprima, e nessuno dei due sostituisce l'altro.
 *
 * ⚠️⚠️ **IL CURSORE SCRIVE LA PREFERENZA QUANDO IL DITO SI ALZA, E IL CAMPO A OGNI NUMERO BUONO**,
 * e i due tempi sono diversi apposta: una strisciata passa per cento valori (il censimento del
 * 2026-09-05 ne aveva contati quasi duecento su un cursore che scriveva a ogni fotogramma), mentre
 * una cifra digitata è già un gesto compiuto. Quello che si **vede** invece segue tutti e due
 * subito, perché viene dallo stato locale.
 *
 * ⚠️ **Il campo tiene il testo e non un numero**, come quello del ridimensionamento: cancellando
 * l'ultima cifra resta vuoto, e un campo legato a un intero ci rimetterebbe uno zero sotto le
 * dita.
 *
 * ⚠️⚠️ **DALLA `2.76` IL NUMERO PUÒ AVERE UN DECIMALE, E LO DICE [step]**: la distanza dal bordo
 * si conta in decimi di centesimo (`step` vale [Watermark.AIR_STEP]), la misura e l'opacità in
 * centesimi interi (`step` vale uno). ⚠️ **Il valore resta un intero in tutti e due i casi**: a
 * viaggiare nelle preferenze è il numero di passi, e il decimale è solo il modo in cui si scrive.
 * Un `Float` nell'archivio avrebbe portato dentro l'arrotondamento binario per un dato che ha
 * duecentocinquantun valori possibili.
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
        /*
         * ⚠️ **I due stati ripartono quando la preferenza cambia da fuori** (la chiave è `value`),
         * così un ripristino dei valori di fabbrica riporta cursore e campo dove devono stare
         * invece di lasciarli dov'erano.
         */
        var quanto by remember(value) { mutableIntStateOf(value) }
        var testo by remember(value) { mutableStateOf(markText(value, step)) }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
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
                // ⚠️ Il numero fuori corsa si segna invece di essere rifiutato: chi scrive '1'
                // per arrivare a '14' passa da un valore che la corsa non ammette, e un campo
                // che glielo cancellasse sotto le dita non si potrebbe usare.
                isError = markValue(testo, step) !in range,
                singleLine = true,
                suffix = { Text(PER_CENT) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                shape = BOX_SHAPE,
                modifier = Modifier.width(NUM_FIELD)
            )
        }
        Slider(
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
 * ⚠️ **Si ricava dal passo invece di essere un parametro in più**: un passo è una potenza di dieci
 * (oggi solo [Watermark.AIR_STEP], che vale dieci), quindi le cifre sono già scritte dentro di lui,
 * e un secondo numero da tenere allineato darebbe un campo che accetta un decimale e un valore che
 * non lo sa leggere.
 */
private fun markDecimals(step: Int): Int = when {
    step >= 100 -> 2
    step >= 10 -> 1
    else -> 0
}

/**
 * Il separatore decimale della lingua del telefono.
 *
 * ⚠️⚠️ **NON È LA VIRGOLA SCRITTA A MANO, e la ragione è che l'app parla ventotto lingue**: in
 * italiano si scrive `0,5` e in inglese `0.5`, e un campo che mostrasse la virgola a chi ha il
 * telefono in inglese scriverebbe un numero che nel suo tastierino non c'è. Chi **scrive** invece
 * è accontentato in tutti e due i modi (vedi [markClean]): una tastiera numerica offre il punto o
 * la virgola a seconda di come è fatta, e rifiutare quello sbagliato sarebbe un campo che non
 * risponde.
 */
private fun decimalMark(): Char = DecimalFormatSymbols.getInstance().decimalSeparator

/**
 * Come un valore si scrive nel campo, col decimale che il suo passo consente.
 *
 * ⚠️ **Il decimale a zero non si scrive**: la distanza di fabbrica vale trenta decimi, e nel campo
 * si legge `3` e non `3,0`, perché il numero tondo è il caso normale e la coda direbbe solo che
 * esiste una cifra in più.
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
 * ⚠️ **Il punto e la virgola valgono uguale**, e non è indulgenza: le due tastiere numeriche di
 * Android offrono l'uno o l'altra a seconda della lingua, quindi accettarne una sola vorrebbe dire
 * un tasto che non scrive niente su metà dei telefoni.
 * ⚠️ **La coda si allunga a destra con degli zeri**: chi scrive `0,` ha già detto zero, e chi
 * scrive `1,` ha detto uno, quindi la cifra che manca vale zero e non fa cadere la lettura.
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
 * ⚠️ **Filtra invece di rifiutare**, come faceva prima con le sole cifre: un campo che respinge un
 * carattere intero non dice quale, e chi scrive non capisce che cosa gli sia stato tolto.
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
 * ⚠️⚠️ **DALLA `2.76` LA SCELTA È VISUALE E NON PIÙ UNA FILA DI PASTIGLIE, ED È SUA RICHIESTA**
 * (punto 3 del campo libero del giro dalla `2.71` alla `2.74`: *la scelta tra i quattro angoli e
 * il centro dovrebbe essere visuale, con dei selettori angolari color accento, poco fuori dal
 * riquadro, per i quattro angoli, più un selettore superiore, sempre fuori dal riquadro, per
 * selezionare il centro senza coprirlo*). Cinque nomi scritti dicono dove andrà la firma con delle
 * parole, mentre il riquadro lo dice **col posto**, e il riquadro c'era già lì sopra.
 *
 * ⚠️⚠️ **QUINDI IL RIQUADRO C'È ANCHE SENZA UN LOGO SCELTO, e la nota della `2.71` è decaduta**:
 * diceva che un riquadro vuoto non dice niente, e valeva finché era la sola anteprima. Adesso
 * porta i cinque selettori, cioè è il comando: toglierlo vorrebbe dire non poter scegliere il
 * posto prima di scegliere il file.
 *
 * ⚠️ **I selettori vivono tutti nella stessa fascia esterna**, larga [SPOT_RING]: i quattro angoli
 * l'abbracciano da fuori e il centro sta in mezzo a quella di sopra. Una fascia diversa per il
 * quinto avrebbe dato due arie da tenere allineate, e il *senza coprirlo* della sua richiesta è
 * proprio quello che la fascia garantisce.
 *
 * ⚠️ **Il bersaglio è più grande del segno**, [SPOT_TAP] contro [SPOT_ARM]: una squadretta larga
 * quanto il dito coprirebbe l'angolo dell'immagine che deve mostrare.
 */
@Composable
private fun MarkSpot(plan: Watermark.Plan, giro: Int, onSpot: (Watermark.Spot) -> Unit) {
    val label = stringResource(R.string.settings_mark_where)
    val names = Watermark.Spot.entries.map { spotName(it) }
    // ⚠️ I nomi restano nella ricerca anche se non si scrivono più da nessuna parte: chi cerca
    // 'centro' cerca questa voce, ed è il criterio di [Choices], che li metteva fra i testi
    // confrontati proprio perché sono la parola con cui si pensa all'impostazione.
    Searchable(label, *names.toTypedArray()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp)
        )
        Box(modifier = Modifier.fillMaxWidth().padding(top = 10.dp).oneOf()) {
            MarkPreview(plan, giro, Modifier.padding(SPOT_RING))
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
    }
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

/** A quale angolo della fascia esterna vive il selettore di un posto. */
private fun spotAlign(spot: Watermark.Spot): Alignment = when (spot) {
    Watermark.Spot.TOP_LEFT -> Alignment.TopStart
    Watermark.Spot.TOP_RIGHT -> Alignment.TopEnd
    Watermark.Spot.BOTTOM_LEFT -> Alignment.BottomStart
    Watermark.Spot.BOTTOM_RIGHT -> Alignment.BottomEnd
    Watermark.Spot.CENTRE -> Alignment.TopCenter
}

/**
 * Un selettore: la squadretta di un angolo, o il tondo del centro.
 *
 * ⚠️⚠️ **LA PIEGA DELLA SQUADRETTA SI RICAVA DALLA FASCIA E NON È UN NUMERO PER OGNI ANGOLO**: il
 * bersaglio è allineato all'angolo della fascia, quindi l'angolo del riquadro cade a [SPOT_RING]
 * dai suoi due lati esterni, e i quattro casi sono lo stesso conto con due segni. Scritti uno per
 * uno sarebbero quattro coppie di coordinate da tenere d'accordo col rientro del riquadro.
 *
 * ⚠️ **Il tondo del centro è pieno quando è scelto e vuoto quando no**, come la squadretta che
 * cambia inchiostro: un segno che restasse uguale direbbe dove si può toccare e non che cosa è
 * scelto, e il riquadro sotto non lo dice, perché senza un logo è vuoto.
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
            val tinta = if (chosen) accento else accento.copy(alpha = SPOT_FAINT)
            val spesso = (if (chosen) SPOT_THICK_ON else SPOT_THICK_OFF).toPx()
            val anello = SPOT_RING.toPx()
            if (spot == Watermark.Spot.CENTRE) {
                val centro = Offset(size.width / 2f, anello / 2f)
                val raggio = SPOT_DOT.toPx()
                if (chosen) drawCircle(tinta, radius = raggio, center = centro)
                else drawCircle(tinta, radius = raggio, center = centro, style = Stroke(spesso))
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
            val piega = Offset(ox, oy)
            drawLine(tinta, piega, Offset(ox + dx, oy), spesso, StrokeCap.Round)
            drawLine(tinta, piega, Offset(ox, oy + dy), spesso, StrokeCap.Round)
        }
    }
}

/**
 * Il riquadro che mostra dove la filigrana cadrà, quanto sarà grande e quanto si vedrà.
 *
 * ⚠️⚠️ **LEGGE IL PIANO E NON LE PREFERENZE, ED È QUELLO CHE LO TIENE ONESTO**: riceve lo stesso
 * [Watermark.Plan] che il salvataggio riceverà, quindi i quattro numeri sono **gli stessi** e non
 * una seconda lettura che il giorno dopo diverge.
 * ⚠️ **La misura si ricava dal lato lungo del riquadro, esattamente come sull'immagine vera**: il
 * disegno entra in un quadrato di lato `size` centesimi del lato lungo, e il margine vale `air`
 * **decimi di centesimo** dello stesso lato, cioè lo stesso conto di [Watermark.cornerFor].
 * ⚠️ **Il rapporto è 3:2**, cioè quello di una fotografia: un riquadro quadrato direbbe una
 * proporzione che quasi nessuna immagine ha.
 * ⚠️⚠️ **DALLA `2.76` GLI ANGOLI SONO QUASI VIVI, ED È SUA RICHIESTA** (punto 2 dello stesso campo
 * libero: *dev'essere molto meno arrotondata (giusto un paio di pixel*). Il riquadro rappresenta
 * una fotografia, e una fotografia gli angoli stondati non ce li ha: i due punti che restano
 * dicono che è una superficie dell'app senza farla sembrare una scheda.
 */
@Composable
private fun MarkPreview(plan: Watermark.Plan, giro: Int, modifier: Modifier) {
    val context = LocalContext.current
    // ⚠️⚠️ **LA CHIAVE È IL CONTATORE DELLA PAGINA, E SENZA DI LEI IL RIQUADRO MENTIVA**: il
    // disegno vive in `filesDir`, cioè fuori dallo stato di Compose, quindi scegliendo un altro
    // file dello stesso tipo niente diceva a `produceState` di rileggerlo. Dalla `2.76` il
    // riquadro c'è anche senza un logo, quindi senza chiave non si sarebbe riempito mai.
    // ⚠️ Non si rilegge alla **misura**: qui si rende a un lato fisso e a rimpicciolirlo è il
    // riquadro, quindi seguire il cursore vorrebbe dire decodificare un SVG sessanta volte al
    // secondo.
    val art by produceState<ImageBitmap?>(null, giro) {
        value = withContext(Dispatchers.IO) {
            Watermark.artwork(context, PREVIEW_ART)?.asImageBitmap()
        }
    }
    val descrizione = stringResource(R.string.settings_mark_preview)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 2f)
            .clip(RoundedCornerShape(PREVIEW_ROUND))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription = descrizione }
    ) {
        val disegno = art ?: return@BoxWithConstraints
        val lungo = maxWidth
        val lato = lungo * (plan.size / 100f)
        Image(
            bitmap = disegno,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            // ⚠️ L'opacità è quella del piano, cioè la stessa che il pennello del salvataggio
            // mette sul suo `Paint`: senza, il riquadro mostrerebbe una firma piena mentre sul
            // file ne arriva una smorzata.
            alpha = plan.alpha / 100f,
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
                // ⚠️ Al centro non c'è nessun bordo da cui stare lontani, come nel disegno vero.
                .padding(
                    if (plan.spot == Watermark.Spot.CENTRE) 0.dp
                    else lungo * (plan.air / (100f * Watermark.AIR_STEP))
                )
                .sizeIn(maxWidth = lato, maxHeight = lato)
        )
    }
}

/** Il lato lungo a cui si disegna la filigrana per l'anteprima. */
private const val PREVIEW_ART = 512

/**
 * Il raggio degli angoli del riquadro dell'anteprima.
 *
 * ⚠️ **Due punti e non i dodici della `2.71`**, ed è sua richiesta: quel riquadro rappresenta una
 * fotografia, e una fotografia gli angoli tondi non ce li ha. Zero avrebbe fatto di lui un
 * rettangolo nudo in una pagina in cui ogni superficie è stondata.
 */
private val PREVIEW_ROUND = 2.dp

/**
 * Quanto è larga la fascia intorno al riquadro in cui vivono i cinque selettori.
 *
 * ⚠️ **È l'unico numero che lega il riquadro ai selettori**: il rientro dell'anteprima e la piega
 * delle squadrette si ricavano tutti e due da lui, quindi non possono scollarsi.
 */
private val SPOT_RING = 16.dp

/** Quanto è grande il bersaglio di un selettore: il dito, non il segno. */
private val SPOT_TAP = 44.dp

/** Quanto sono lunghi i due bracci di una squadretta, lungo i lati del riquadro. */
private val SPOT_ARM = 18.dp

/** Quanto la piega di una squadretta sta fuori dall'angolo del riquadro, in diagonale. */
private val SPOT_OUT = 5.dp

/** Il raggio del tondo che sceglie il centro. */
private val SPOT_DOT = 5.dp

/** Lo spessore del segno di un posto scelto, e di uno che non lo è. */
private val SPOT_THICK_ON = 3.dp
private val SPOT_THICK_OFF = 2.dp

/**
 * Quanto si spegne il segno di un posto non scelto.
 *
 * ⚠️ **Spento e non di un altro colore**: sono tutti e cinque d'accento, che è la parola della sua
 * richiesta, quindi a dire quale è scelto restano l'inchiostro e lo spessore.
 */
private const val SPOT_FAINT = 0.35f

/** I due tipi che il selettore di sistema mostra: vedi [Watermark.Kind]. */
private const val PNG_MIME = "image/png"
private const val SVG_MIME = "image/svg+xml"

/**
 * Quanto è largo il campo dei tre numeri.
 *
 * ⚠️ **Dichiarata e non lasciata al contenuto**: tre campi larghi quanto il loro numero darebbero
 * tre colonne diverse una sotto l'altra, e il valore cambia mentre si trascina.
 */
private val NUM_FIELD = 104.dp

/** Quante cifre si accettano: le corse arrivano a cento, e il taglio è la rete. */
private const val NUM_DIGITS = 3

/**
 * Il segno che accompagna i tre numeri.
 *
 * ⚠️ **Non si traduce**, come '16:9' fra i formati del Ritaglio: è un simbolo, e le ventotto
 * lingue lo scrivono tutte così.
 */
private const val PER_CENT = "%"

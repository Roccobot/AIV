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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
 * La pagina **'Filigrana'**: il file, dove cade, come è fatta, e se si applica al salvataggio.
 *
 * ⚠️⚠️ **È UNA SOTTO-PAGINA PERCHÉ LA FAMIGLIA HA SUPERATO LA SOGLIA** (`Rules.md`, § '⚙️ Dove va
 * un'impostazione, e chi la deve trovare'): la domanda è una sola, *che logo scrivo sulle immagini
 * che salvo*, e le voci sono sei più l'anteprima, cioè oltre il *2-3* della soglia dell'utente.
 * ⚠️⚠️ **E DALLA `2.71` VIVE DAVVERO DENTRO 'Editor e salvataggio', CHE PRIMA ERA SOLO SCRITTO**
 * (voce `filigrana` del giro della `2.70`, punto 1): la porta che la apre stava nella pagina
 * **radice**, sotto 'Aspetto', mentre il commento di questo file la dava già dov'è adesso. Cioè
 * il codice e la nota dicevano due cose diverse, e a vedersi era il codice.
 *
 * ⚠️⚠️ **L'ORDINE È IL SUO, E DALLA `4.99` L'INTERRUTTORE APRE LA PAGINA** (nota A del giro della
 * `4.98`, sul suo mockup): in cima 'Attiva' con la spiegazione di che cos'è la filigrana, poi il
 * file, il posto, i tre numeri, e in fondo l'avviso sul senza perdita. Dalla `2.71` alla `4.98`
 * l'interruttore chiudeva la pagina, perché allora lui voleva 'Posizione' sopra di lui.
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
                 * ⚠️⚠️ **SCEGLIERE UN FILE ACCENDE LA FILIGRANA, DALLA `2.71`, ED È SUA
                 * RICHIESTA** (voce `filigrana`, punto 2). Chi entra qui e sceglie un logo ha
                 * detto che cosa vuole scrivere sulle immagini, e lasciare la firma spenta
                 * vorrebbe dire un secondo gesto perché quello appena fatto valga qualcosa. È la
                 * stessa lettura dell''Applica' del ridimensionamento, che accende il suo
                 * (§ [ResizeDialog]).
                 * ⚠️ **Dalla `5.01` vale ancora di più**: l'interruttore di questa pagina non c'è
                 * più, e la filigrana si accende e si spegne solo dal tasto dell'editor, che
                 * senza un logo non compare.
                 * ⚠️ **Solo quando il file è stato adottato davvero**: un documento rifiutato
                 * lascia le cose com'erano.
                 */
                onChange(settings.copy(markOn = true))
            }
        }
    }

    val label = stringResource(R.string.settings_mark)
    val desc = stringResource(R.string.settings_mark_desc)
    /*
     * ⚠️⚠️ **DALLA `5.01` LA PAGINA SI APRE COL PARAGRAFO E SENZA INTERRUTTORE, ED È LA SUA NOTA SU
     * `5.00-01`** (*dalla schermata si impostano i parametri, non si stabilisce se la filigrana è
     * attiva o no: per quello c'è il tasto dell'editor*). Con l'interruttore se ne è andato anche il
     * titolo 'Attiva', che senza di lui non diceva niente. Fino alla `5.00` la riga apriva la pagina
     * col titolo 'Attiva' e il paragrafo sotto (nota A del giro della `4.98`).
     * ⚠️ **'Filigrana' è fra i testi della ricerca del paragrafo**, perché durante una ricerca il
     * paragrafo compare senza la pagina intorno, e chi cerca 'filigrana' cerca proprio lui.
     */
    val spiegazione = stringResource(R.string.settings_mark_on_desc)
    Searchable(spiegazione, label) {
        Detail(spiegazione, Modifier.padding(top = 8.dp, bottom = 4.dp))
    }
    Searchable(label, desc) {
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
            // ⚠️ 'Seleziona' è una stringa della sola pagina (nota A del giro della `4.98`):
            // `settings_editor_pick` è il tasto della riga dell'editor, che dalla `5.00` dice
            // 'Imposta app'.
            TextButton(onClick = { scegli.launch(arrayOf(PNG_MIME, SVG_MIME)) }) {
                Text(stringResource(R.string.settings_mark_pick))
            }
        }
    }

    /*
     * ⚠️⚠️ **DALLA `2.76` L'ANTEPRIMA E LA SCELTA DEL POSTO SONO LA STESSA COSA, e la nota che
     * diceva 'il riquadro c'è solo quando c'è una filigrana' è decaduta**: senza un logo scelto il
     * riquadro resta vuoto, ma porta comunque i cinque selettori, cioè dice qualcosa. Il perché
     * per esteso vive su [MarkSpot].
     * ⚠️⚠️ **DALLA `3.36` ANTEPRIMA A LATO DA 1.024 dp** (mockup `resize` / Dimensioni e
     * filigrana): sul tablet largo i parametri sono accanto al riquadro; sotto e sul telefono
     * restano in colonna. Hand come altrove.
     */
    val widthDp = LocalConfiguration.current.screenWidthDp
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
        /*
         * ⚠️⚠️ **I TRE NUMERI SONO TRE CHIAMATE ALLO STESSO PEZZO, E NON TRE BLOCCHI COPIATI**:
         * ognuno porta un campo, un cursore e la loro sincronia, e scritti riga per riga
         * sarebbero nove occasioni di sbagliarne una. È lo stesso criterio per cui i sei
         * cursori della Luce sono una tabella.
         */
        MarkNumber(
            label = stringResource(R.string.settings_mark_size),
            value = settings.markSize,
            range = Watermark.SIZE,
            onChange = { onChange(settings.copy(markSize = it)) }
        )
        /*
         * ⚠️⚠️ **LA DISTANZA DAL BORDO SI SCRIVE COL DECIMALE, DALLA `2.76`, ED È SUA RICHIESTA**
         * (punto 1 del campo libero del giro dalla `2.71` alla `2.74`: *Filigrana / Distanza dal
         * bordo: aggiungi i valori 0,2 e 0,5*). A viaggiare è sempre un intero, perché il valore
         * è in **decimi di centesimo** ([Watermark.AIR_STEP]): quello che cambia è come si legge
         * e come si scrive.
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

    /*
     * ⚠️ **L'avviso sul senza perdita chiude la pagina, dalla `4.99`** (nota A del giro della
     * `4.98`, col testo riscritto da lui): fino alla `4.98` era la coda della spiegazione
     * dell'interruttore, dove si leggeva come una parte del 'come si accende'.
     */
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
             * ⚠️⚠️ **DALLA `4.99` IL CAMPO È UN `BasicTextField` CON LA CORNICE DI MATERIAL, PERCHÉ
             * LUI LO VOLEVA PIÙ COMPATTO** (nota A del giro della `4.98`). `OutlinedTextField` ha
             * un'altezza minima di 56 punti e un rientro verticale di 16 che nessuna sua firma
             * espone; la `DecorationBox` prende il rientro come parametro, e il campo scende a
             * [NUM_FIELD_HIGH] con la stessa cornice, lo stesso segno '%' e lo stesso stato
             * d'errore.
             */
            val tocco = remember { MutableInteractionSource() }
            // ⚠️ Il numero fuori corsa si segna invece di essere rifiutato: chi scrive '1' per
            // arrivare a '14' passa da un valore che la corsa non ammette, e un campo che glielo
            // cancellasse sotto le dita non si potrebbe usare.
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
                        suffix = { Text(PER_CENT) },
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
 * ospita i cinque selettori, cioè è il comando: toglierlo vorrebbe dire non poter scegliere il
 * posto prima di scegliere il file.
 *
 * ⚠️ **I selettori vivono tutti nella stessa fascia esterna**, larga [SPOT_RING]: i quattro angoli
 * l'abbracciano da fuori e il centro è in mezzo a quella di sotto (in quella di sopra fino alla
 * `5.00`, vedi [spotAlign]). Una fascia diversa per il
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
        /*
         * ⚠️⚠️ **DALLA `4.99` IL RIQUADRO È RIDOTTO, E 'Posizione' VIVE DENTRO DI LUI DALLA `5.00`**
         * (nota A del giro della `4.98`, e sua nota su `4.99-01`). La riduzione si toglie al
         * riquadro e non alla fascia dei selettori, che resta larga quanto prima perché le
         * squadrette hanno la misura che lui ha chiesto con la `2.78`.
         * ⚠️⚠️ **DALLA `5.01` IL BLOCCO È CENTRATO E LA NOTA È SOTTO, SU UNA RIGA, ED È LA SUA NOTA SU
         * `5.00-01`** (*non mi piace la posizione variabile, risolviamo cambiando approccio*). Fino
         * alla `5.00` il blocco si spostava a sinistra e la nota andava accanto al riquadro, o sotto
         * quando la sua parola più larga non ci entrava, cioè cambiava posto con la lingua e con lo
         * schermo. Senza la nota a lato il riquadro torna un po' più grande ([PREVIEW_SCALE]).
         * ⚠️ **Dalla `5.02` sopra il blocco c'è meno aria e sotto la nota un po' di più** (sua nota
         * su `5.01-01`: *aumenta un pelo la distanza fra la nota e il primo slider; diminuisci
         * invece la distanza fra 'Rimuovi | Seleziona' e il rettangolo*): 6 punti invece di 16
         * sopra, 8 in più sotto.
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
 * ⚠️ **Esiste perché dalla `4.99` l'anteprima cambia di proposito quello che mostra** (vedi
 * [previewLook]): un fondo, un'opacità e una misura diverse da quelle del file, scelte perché la
 * firma si veda. Il testo è suo.
 * ⚠️⚠️ **DALLA `5.00` IL GRIGIO È UN TERZO DI INCHIOSTRO SUL FONDO** (sua nota su `4.99-01`, e il
 * colore del suo mockup, `#B1B1B1`, misurato sul file). Lui aveva scritto `#fcfbf7`, che è il fondo
 * stesso della pagina (`LIGHT_BACK` vale `#FCFBF8`): con quello la nota sparirebbe, quindi vale il
 * mockup. Un terzo di `onSurface` su `background` dà esattamente `#B1B1B1` col tema chiaro, e la
 * stessa regola dà un grigio di pari peso col tema scuro. Il contrasto è sotto la soglia, ed è la
 * sua scelta: *sennò è troppo allarmista*.
 * ⚠️ **Dalla `5.01` entra in una riga in italiano e in inglese** (sua nota su `5.00-01`: *carattere
 * abbastanza piccolo da far stare la frase in una sola riga almeno in ITA e ENG*), e lo misura
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
 * In quale punto della fascia esterna vive il selettore di un posto.
 *
 * ⚠️ **Il centro è in basso dalla `5.01`**, come nel suo mockup di `5.00-01`: in alto c'è
 * 'Posizione della filigrana', e lo stelo sale dal tondo verso il centro del riquadro (vedi
 * [centreStem]).
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
 * ⚠️⚠️ **LA PIEGA DELLA SQUADRETTA SI RICAVA DALLA FASCIA E NON È UN NUMERO PER OGNI ANGOLO**: il
 * bersaglio è allineato all'angolo della fascia, quindi l'angolo del riquadro cade a [SPOT_RING]
 * dai suoi due lati esterni, e i quattro casi sono lo stesso conto con due segni. Scritti uno per
 * uno sarebbero quattro coppie di coordinate da tenere d'accordo col rientro del riquadro.
 *
 * ⚠️ **Il tondo del centro è pieno quando è scelto e vuoto quando no**, come la squadretta che
 * cambia inchiostro: un segno che restasse uguale direbbe dove si può toccare e non che cosa è
 * scelto, e il riquadro sotto non lo dice, perché senza un logo è vuoto.
 * ⚠️⚠️ **DALLA `5.01` IL TONDO HA UNO STELO, ED È LA SUA NOTA SU `5.00-01`** (*aggiungi uno stelo
 * al cerchio di selezione per il centro, è più chiaro*). Dalla `5.02` lo stelo lo disegna
 * [MarkPreview], sotto il logo (vedi [centreStem]); qui resta il tondo, e acceso anche il tratto
 * fra il suo bordo e il riquadro, che cade nella fascia, fuori dall'anteprima.
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
             * ⚠️⚠️ **UN TRACCIATO SOLO E NON DUE LINEE, DALLA `4.99`, ED È LA SUA NOTA D DEL GIRO
             * DELLA `4.98`** (*una forma unica unita, senza sovrapposizioni che sommano la loro
             * opacità*): due linee coi capi tondi coprivano due volte la piega, e con l'inchiostro
             * spento al [SPOT_FAINT] là la squadretta veniva più scura. Un tracciato solo si
             * dipinge una volta, e il giunto tondo tiene la piega com'era.
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
 * ⚠️⚠️ **DALLA `5.02` VIVE NELL'ANTEPRIMA, SOTTO IL LOGO, ED È LA SUA NOTA SU `5.01-01`** (*lo stelo
 * del mio mockup era troppo lungo e va a coprire la filigrana al centro. Accorcialo di 10-15 dp e
 * fa' in modo che un eventuale logo al centro lo copra*). Fino alla `5.01` lo disegnava il
 * selettore, che si disegna sopra a tutto, e arrivava al centro esatto.
 * ⚠️ **Spento, comincia un tratto sopra l'anello del tondo, senza toccarlo**: il segno è al
 * [SPOT_FAINT], e due tratti sovrapposti farebbero una macchia più scura, che è il difetto della
 * nota D del giro della `4.98` sulle squadrette (il capo tondo sporge di mezzo tratto). Acceso, il
 * tondo è pieno e coprente, e lo stelo parte dal suo bordo: il pezzo che cade nella fascia, fuori
 * dall'anteprima, lo disegna [SpotHandle].
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
 * ⚠️⚠️ **LEGGE IL PIANO E NON LE PREFERENZE**: riceve lo stesso [Watermark.Plan] che il
 * salvataggio riceverà, quindi posto e distanza dal bordo sono **gli stessi** e non una seconda
 * lettura che il giorno dopo diverge.
 * ⚠️⚠️ **MA DALLA `4.99` FONDO, OPACITÀ E MISURA SI SCELGONO PERCHÉ LA FIRMA SI VEDA** (nota A del
 * giro della `4.98`: *la trasparenza e la posizione possono devono modificati caso per caso per
 * facilitare la visibilità*), e la nota accanto lo dice. La scelta la fa [previewLook].
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
private fun MarkPreview(plan: Watermark.Plan, giro: Int, label: String, modifier: Modifier) {
    val context = LocalContext.current
    // ⚠️⚠️ **LA CHIAVE È IL CONTATORE DELLA PAGINA, E SENZA DI LEI IL RIQUADRO MENTIVA**: il
    // disegno vive in `filesDir`, cioè fuori dallo stato di Compose, quindi scegliendo un altro
    // file dello stesso tipo niente diceva a `produceState` di rileggerlo. Dalla `2.76` il
    // riquadro c'è anche senza un logo, quindi senza chiave non si sarebbe riempito mai.
    // ⚠️ Non si rilegge alla **misura**: qui si rende a un lato fisso e a rimpicciolirlo è il
    // riquadro, quindi seguire il cursore vorrebbe dire decodificare un SVG sessanta volte al
    // secondo.
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
        // ⚠️ Prima del logo, perché il logo al centro lo copra (vedi [centreStem]).
        Canvas(modifier = Modifier.matchParentSize()) {
            centreStem(spotInk(accento, plan.spot == Watermark.Spot.CENTRE), plan.spot == Watermark.Spot.CENTRE)
        }
        /*
         * ⚠️⚠️ **'Posizione della filigrana' VIVE QUI DENTRO DALLA `5.00`** (sua nota su `4.99-01`: *va
         * direttamente dentro il rettangolo di anteprima, con le stesse regole di
         * contrasto/leggibilità della filigrana: non al centro, perché lì potrebbe apparire la
         * filigrana, ma spostato verso l'alto*). Il colore lo sceglie [labelInk] dal fondo che
         * [previewLook] ha scelto per la firma, e la quota è [LABEL_BIAS], come nel suo mockup.
         * ⚠️ **Dalla `5.02` dice 'Posizione della filigrana', due punti più piccola** (`labelMedium`
         * al posto di `titleSmall`) **e a contrasto 3** (sua nota su `5.01-01`).
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
            // ⚠️⚠️ Dalla `4.99` l'opacità e la misura sono quelle dell'anteprima e non del piano
            // (vedi [previewLook]): fino alla `4.98` il riquadro le copiava dal salvataggio, e un
            // logo piccolo e smorzato non si vedeva.
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
                // ⚠️ Al centro non c'è nessun bordo da cui stare lontani, come nel disegno vero.
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
 * ⚠️⚠️ **È IL SUO ESEMPIO DELLA NOTA A DEL GIRO DELLA `4.98`, MESSO IN REGOLA**: *logo bianco
 * semitrasparente, piccolo, in basso a sinistra: il grigio di default diventa nero, l'opacità
 * della filigrana diventa 100%, la dimensione diventa 200%*, con *gli accorgimenti di contrasto
 * minimo necessari*. Quindi:
 * - **il fondo resta il grigio** finché il contrasto con l'inchiostro arriva a
 *   [PREVIEW_CONTRAST], la soglia minima per un elemento grafico; sotto diventa il nero o il
 *   bianco, quello dei due che contrasta di più. Il conto è il rapporto di contrasto del W3C, fra
 *   luminanze relative;
 * - **l'opacità è sempre piena**, perché smorzare la firma è proprio quello che la nasconde;
 * - **la misura è doppia, con un tetto a [PREVIEW_SIZE_CAP] centesimi**, oltre il quale un logo
 *   grande coprirebbe il riquadro; una misura che è già oltre il tetto resta com'è.
 * ⚠️ **Senza un logo il fondo è il grigio**: non c'è niente da far vedere.
 * ⚠️ **È una funzione pura e interna** perché il banco la chiami: il riquadro vero dipende dal
 * disegno, che il banco non decodifica.
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
 * [LABEL_CONTRAST] col fondo [ground], più scuro del fondo dove ci sta, più chiaro dove no.
 *
 * ⚠️ **Segue il fondo che [previewLook] ha scelto per la firma** (sua nota su `4.99-01`: *con le
 * stesse regole di contrasto/leggibilità della filigrana*).
 * ⚠️⚠️ **DALLA `5.02` IL CONTRASTO È 3 E NON IL MASSIMO, ED È LA SUA NOTA SU `5.01-01`** (*contrasto a
 * 3 anziché ≥4 rispetto al rettangolo*): fino alla `5.01` la parola era bianca sul nero, nera sul
 * bianco e del colore del testo sul grigio, cioè gridava più della firma che deve far vedere.
 * ⚠️ **Più scura quando si può**, cioè quando il fondo è abbastanza chiaro da lasciare posto sotto
 * di sé a un grigio a quel contrasto; sul nero e su un grigio scuro non c'è posto, e si sale.
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
 * ⚠️ **Pesata sull'opacità** perché i pixel trasparenti non sono inchiostro: un logo bianco su un
 * PNG vuoto direbbe 'scuro' se contassero anche loro, dato che un pixel trasparente vale nero.
 * ⚠️ **Le componenti si linearizzano prima della media**, come vuole la luminanza relativa: la
 * media delle componenti codificate darebbe un grigio più scuro del vero.
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
 * ⚠️ **È il 3:1 che il W3C chiede agli elementi grafici** (WCAG 2.1, criterio 1.4.11): il minimo
 * della sua richiesta, e non il 4,5:1 del testo, perché una firma è un disegno.
 */
private const val PREVIEW_CONTRAST = 3f

/** Il tetto della misura raddoppiata dell'anteprima, in centesimi del lato lungo. */
private const val PREVIEW_SIZE_CAP = 50

/**
 * Quanto è grande il riquadro dell'anteprima rispetto alla larghezza che ha a disposizione.
 *
 * ⚠️ **L'80% è la sua richiesta della `5.01`** (sua nota su `5.00-01`: *dato che non c'è più la
 * nota a lato, torna ad ingrandire leggermente l'anteprima*), ed è la misura del suo mockup, dove il
 * blocco occupa l'81% della pagina. Dalla `4.99` alla `5.00` era il 75% (*rimpicciolisci del 25%*,
 * nota A del giro della `4.98`), per far posto alla nota accanto.
 */
private const val PREVIEW_SCALE = 0.8f

/**
 * Quanto inchiostro ha la nota sotto il riquadro, sul fondo della pagina.
 *
 * ⚠️ **Un terzo dà esattamente `#B1B1B1` col tema chiaro**, cioè il grigio del suo mockup: vedi
 * [PreviewNote].
 */
private const val NOTE_INK = 1f / 3f

/**
 * La quota di 'Posizione' dentro il riquadro, da -1 (in cima) a 1 (in fondo).
 *
 * ⚠️ **-0,7 mette il centro della parola al 15% dell'altezza**, come nel suo mockup: sopra il
 * centro, dove può cadere la firma, e lontano dagli angoli, che non tocca perché è centrata.
 */
private const val LABEL_BIAS = -0.7f

/**
 * Il contrasto di 'Posizione della filigrana' col fondo del riquadro.
 *
 * ⚠️ **3 è la sua richiesta della `5.02`** (*contrasto a 3 anziché ≥4 rispetto al rettangolo*, nota
 * su `5.01-01`): è anche la soglia del W3C per gli elementi grafici, la stessa di [PREVIEW_CONTRAST].
 */
private const val LABEL_CONTRAST = 3f

/**
 * Quanto lo stelo del centro si ferma prima del centro del riquadro.
 *
 * ⚠️ **12 è nel mezzo dei suoi 10-15** (nota su `5.01-01`): arrivando al centro esatto, lo stelo
 * copriva la firma posata lì.
 */
private val STEM_SHORT = 12.dp

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
 *
 * ⚠️⚠️ **DALLA `2.78` VALE 22 E NON PIÙ 16, E A PAGARE È IL RIQUADRO** (voce `mark-posto`
 * approvata con una richiesta: *rendi solo i selettori DECISAMENTE più spessi e visibili ... se
 * necessario rimpicciolisci il riquadro di quanto basta a far stare all'esterno dei selettori ben
 * pasciuti*). Segni più grossi vogliono più fascia, e la fascia si prende dal rientro
 * dell'anteprima: sei punti per lato su un riquadro largo quanto la pagina.
 */
private val SPOT_RING = 22.dp

/** Quanto è grande il bersaglio di un selettore: il dito, non il segno. */
private val SPOT_TAP = 44.dp

/**
 * Quanto sono lunghi i due bracci di una squadretta, lungo i lati del riquadro.
 *
 * ⚠️ **Dalla `2.78` vale 22 e non più 18**: cresce con la fascia, o una squadretta più spessa
 * sarebbe anche più tozza.
 */
private val SPOT_ARM = 22.dp

/** Quanto la piega di una squadretta sta fuori dall'angolo del riquadro, in diagonale. */
private val SPOT_OUT = 5.dp

/**
 * Il raggio del tondo che sceglie il centro.
 *
 * ⚠️⚠️ **DALLA `2.78` VALE QUANTO UN BRACCIO DI SQUADRETTA, ED È LA SECONDA METÀ DELLA SUA
 * RICHIESTA** (*il selettore del centro dev'essere un tondo più o meno delle stesse dimensioni dei
 * selettori di angolo, anche se di forma diversa*): il diametro è 18 contro i 22 di un braccio,
 * dove prima erano 10. ⚠️ **Non arriva a 22 tondi**, e il conto dice perché: il tondo vive dentro
 * la fascia, quindi il suo bordo esterno (raggio più mezzo tratto) deve restare sotto
 * [SPOT_RING]; a 9 più 1,5 arriva a 10,5 su un mezzo anello di 11, cioè sfiora il confine senza
 * entrare nel riquadro, che è il *senza coprirlo* della richiesta del giro prima.
 */
private val SPOT_DOT = 9.dp

/**
 * Lo spessore del segno di un posto scelto, e di uno che non lo è.
 *
 * ⚠️ **Dalla `2.78` sono 5 e 3, e prima erano 3 e 2**: è il *decisamente più spessi* della sua
 * richiesta, e il rapporto fra i due resta, perché a dire quale è scelto sono lo spessore **e**
 * l'inchiostro.
 */
private val SPOT_THICK_ON = 5.dp
private val SPOT_THICK_OFF = 3.dp

/**
 * Quanto si spegne il segno di un posto non scelto.
 *
 * ⚠️ **Spento e non di un altro colore**: sono tutti e cinque d'accento, che è la parola della sua
 * richiesta, quindi a dire quale è scelto restano l'inchiostro e lo spessore.
 * ⚠️ **Dalla `2.78` è più vivo (0,55 invece di 0,35)**, che è la metà *visibili* della sua
 * richiesta: un segno che non è scelto deve dire lo stesso che si può toccare.
 */
private const val SPOT_FAINT = 0.55f

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

/**
 * Quanto è alto il campo dei tre numeri, e il suo rientro verticale.
 *
 * ⚠️ **44 e non i 56 di Material**, dalla `4.99` (nota A del giro della `4.98`): con il rientro
 * di 10 per lato resta una riga di `bodyLarge`, che è alta 24.
 */
private val NUM_FIELD_HIGH = 44.dp
private val NUM_FIELD_PAD = 10.dp

/** Quante cifre si accettano: le corse arrivano a cento, e il taglio è la rete. */
private const val NUM_DIGITS = 3

/**
 * Il segno che accompagna i tre numeri.
 *
 * ⚠️ **Non si traduce**, come '16:9' fra i formati del Ritaglio: è un simbolo, e le ventotto
 * lingue lo scrivono tutte così.
 */
private const val PER_CENT = "%"

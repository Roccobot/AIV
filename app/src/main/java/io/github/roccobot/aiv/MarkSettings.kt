package io.github.roccobot.aiv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
     * ⚠️ **L'anteprima c'è solo quando c'è una filigrana**: un riquadro vuoto non direbbe niente
     * a chi non ne ha ancora scelta una, e il posto in cui si dice che non c'è è la riga qui
     * sopra.
     */
    if (kind != null) MarkPreview(settings.markPlan())

    Choices(
        label = stringResource(R.string.settings_mark_where),
        detail = null,
        options = Watermark.Spot.entries,
        selected = settings.markSpot,
        nameOf = {
            stringResource(
                when (it) {
                    Watermark.Spot.TOP_LEFT -> R.string.mark_spot_tl
                    Watermark.Spot.TOP_RIGHT -> R.string.mark_spot_tr
                    Watermark.Spot.BOTTOM_LEFT -> R.string.mark_spot_bl
                    Watermark.Spot.BOTTOM_RIGHT -> R.string.mark_spot_br
                    Watermark.Spot.CENTRE -> R.string.mark_spot_c
                }
            )
        },
        onSelect = { onChange(settings.copy(markSpot = it)) }
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

    MarkNumber(
        label = stringResource(R.string.settings_mark_air),
        value = settings.markAir,
        range = Watermark.AIR,
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
 */
@Composable
private fun MarkNumber(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Searchable(label) {
        /*
         * ⚠️ **I due stati ripartono quando la preferenza cambia da fuori** (la chiave è `value`),
         * così un ripristino dei valori di fabbrica riporta cursore e campo dove devono stare
         * invece di lasciarli dov'erano.
         */
        var quanto by remember(value) { mutableIntStateOf(value) }
        var testo by remember(value) { mutableStateOf(value.toString()) }

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
                    val pulito = scritto.filter { it.isDigit() }.take(NUM_DIGITS)
                    testo = pulito
                    val n = pulito.toIntOrNull()
                    if (n != null && n in range) {
                        quanto = n
                        if (n != value) onChange(n)
                    }
                },
                // ⚠️ Il numero fuori corsa si segna invece di essere rifiutato: chi scrive '1'
                // per arrivare a '14' passa da un valore che la corsa non ammette, e un campo
                // che glielo cancellasse sotto le dita non si potrebbe usare.
                isError = testo.toIntOrNull() !in range,
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
                testo = quanto.toString()
            },
            onValueChangeFinished = { if (quanto != value) onChange(quanto) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )
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
 * centesimi dello stesso lato.
 * ⚠️ **Il rapporto è 3:2**, cioè quello di una fotografia: un riquadro quadrato direbbe una
 * proporzione che quasi nessuna immagine ha.
 */
@Composable
private fun MarkPreview(plan: Watermark.Plan) {
    val context = LocalContext.current
    // ⚠️ Il disegno si ricarica al cambio del **tipo** e non della misura: qui si rende a un
    // lato fisso e a rimpicciolirlo è il riquadro, quindi rileggere il file a ogni pixel di
    // cursore vorrebbe dire decodificare un SVG sessanta volte al secondo.
    val art by produceState<ImageBitmap?>(null) {
        value = withContext(Dispatchers.IO) {
            Watermark.artwork(context, PREVIEW_ART)?.asImageBitmap()
        }
    }
    val disegno = art ?: return
    val descrizione = stringResource(R.string.settings_mark_preview)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .aspectRatio(3f / 2f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription = descrizione }
    ) {
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
                    else lungo * (plan.air / 100f)
                )
                .sizeIn(maxWidth = lato, maxHeight = lato)
        )
    }
}

/** Il lato lungo a cui si disegna la filigrana per l'anteprima. */
private const val PREVIEW_ART = 512

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

package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.PushPin
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
 *
 * ⚠️⚠️ **RIDISPOSTA DALLA `2.81` SUL SUO SECONDO MOCKUP, E IL GUADAGNO È L'ALTEZZA** (voce
 * `resize-finestra`: *la finestra diventa MOLTO più compatta e usabile (e sarà meno coperta dalla
 * tastiera attiva)*). Quello che cambia, tutto suo: la misura di partenza perde la sua dicitura e
 * sale **sotto il titolo**, la percentuale diventa `%` e va in prima fila (quindi i gettoni stanno
 * in due righe), 'Ripristina' scende in basso a destra, e al suo posto in alto arriva **'Rendi
 * predefinito'**.
 *
 * ⚠️⚠️ **'RENDI PREDEFINITO' È UNA FUNZIONE NUOVA, E PER ESISTERE HA DOVUTO TOGLIERNE UNA A
 * 'APPLICA'** (sua riga: *fa in modo che le impostazioni di ridimensionamento restino memorizzate
 * per i salvataggi seguenti (se l'icona 'Ridimensiona' è accesa)*). Fino alla `2.80` 'Applica'
 * scriveva nelle preferenze, quindi ogni configurazione sopravviveva già al riavvio dell'app e
 * questo comando non avrebbe avuto niente da fare: adesso 'Applica' vale finché l'app è viva, e
 * l'archivio lo scrive solo questo. ⚠️ **La distinzione si dichiara a lui**, perché una parte di
 * quello che chiede c'era già.
 * ⚠️ **Il riscontro è il tasto che si spegne**, e non un avviso: la notifica di casa la disegna la
 * radice dell'app, cioè **dietro** questa finestra, quindi non si vedrebbe. Spento vuol dire che
 * il piano in mano è già il predefinito.
 * ⚠️ **Spento e non assente**: un tasto che compare e sparisce cambiando gettone farebbe ballare
 * la riga del titolo, che è il difetto che la `2.33` ha tolto alla scheda.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResizeDialog(
    /** Il piano da cui si parte: quello salvato, anche se l'interruttore è spento. */
    initial: Resize.Plan,
    /**
     * Il piano **predefinito**, cioè quello scritto nelle preferenze.
     *
     * ⚠️ **Serve solo a spegnere 'Rendi predefinito' quando non c'è niente da scrivere**: senza,
     * quel comando resterebbe acceso dopo averlo toccato, cioè non darebbe nessun riscontro.
     */
    saved: Resize.Plan,
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
    val azzera = {
        mode = Resize.Mode.FREE
        wide = w0.toString()
        tall = h0.toString()
    }
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
            /*
             * ⚠️⚠️ **LA MISURA DI PARTENZA È IL SOTTOTITOLO, DALLA `2.81`, E PRIMA ERA UNA RIGA
             * DEL CORPO** (voce `resize-finestra`: *'Dimensioni attuali' non serviva: toglilo, e
             * metti i pixel larghezza × altezza correnti subito sotto il titolo*). Quella
             * dicitura diceva a parole quello che il posto già dice, e il testo `t-resize-now` la
             * toglie: la riga resta il solo numero, quindi `look_resize_now` non ha più niente da
             * tradurre ed esce dalle ventotto lingue.
             * ⚠️ **E i numeri non sono più in grassetto**, che è il suo mockup: là il grassetto
             * distingueva le cifre dalla dicitura intorno, e senza dicitura distinguerebbe una
             * riga da sé stessa.
             */
            TitleRow(
                title = stringResource(R.string.look_resize),
                subtitle = if (known) AnnotatedString(plain(w0, h0)) else null,
                commands = if (!known) emptyList() else listOf(
                    TitleCommand(
                        text = stringResource(R.string.look_resize_default),
                        glyph = Icons.Filled.PushPin,
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
            // ⚠️ Lo scorrimento serve al tetto della `1.62`, come in ogni altra finestra con un
            // campo: a tastiera aperta il pannello si accorcia invece di essere tagliato.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(RESIZE_GAP)
            ) {
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
                    /*
                     * ⚠️⚠️ **IL SEGNO SI ALLINEA ALLE CIFRE E NON AL CAMPO, DALLA `2.81`** (voce
                     * `resize-finestra`: *il segno `×` è allineato meglio in verticale, in modo
                     * che sia centrato in verticale rispetto alle cifre*). Un campo con
                     * l'etichetta in alto è alto 56 punti e il testo che si scrive dentro non
                     * sta al suo centro: la label gli prende la parte di sopra, quindi il centro
                     * della riga di testo cade circa 20 punti sopra il fondo. Centrando sul
                     * campo, il segno restava più in alto delle cifre che deve separare.
                     * ⚠️ **Il conto**: l'ultima riga di un campo di Material ha 8 punti sotto di
                     * sé, e una riga di `bodyLarge` ne vale 24, quindi il suo centro sta a 20 dal
                     * fondo; un segno in `titleMedium` ha la stessa misura di riga, quindi
                     * appoggiato al fondo con [FIELD_TEXT_DROP] i due centri coincidono.
                     * ⚠️ **Vale anche per l'unità in coda**, che nel suo mockup si legge sulla
                     * stessa riga delle cifre.
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
                /*
                 * ⚠️⚠️ **'RIPRISTINA' È SCESO IN FONDO A DESTRA, DALLA `2.81`** (voce
                 * `resize-finestra`: *Tasto 'Ripristina' spostato in basso: si raggiunge meglio
                 * con una mano*). In cima ci stava dalla `2.72`, ed è il posto che questa app dà
                 * a un comando di finestra: quel posto adesso è del comando che si tocca una
                 * volta sola, mentre questo si tocca mentre si prova, cioè col pollice.
                 * ⚠️ **Resta la stessa pastiglia**, e non un `TextButton` accanto ad 'Applica':
                 * là si legge come un terzo tasto di conferma, mentre scrive nei campi e basta.
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
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().oneOf()
    ) {
        for (modo in Resize.Mode.entries) {
            val acceso = modo == shown && modo != Resize.Mode.FREE
            val nome = stringResource(modeName(modo))
            /*
             * ⚠️⚠️ **LA PERCENTUALE SI SCRIVE COL SEGNO, DALLA `2.81`, E IL SUO NOME RESTA NELLA
             * DESCRIZIONE PARLATA** (voce `resize-finestra`: *'Percentuale' diventa '%'*). Il
             * segno non si traduce, quindi non nasce nessuna stringa e `resize_share` non resta
             * orfana: la legge un lettore di schermo, che di un gettone con scritto '%' non
             * saprebbe dire di che cosa parla.
             */
            val scritto = if (modo == Resize.Mode.SHARE) PCT else nome
            FilterChip(
                selected = acceso,
                onClick = { onPick(modo) },
                label = { Text(scritto) },
                modifier = Modifier
                    .picked(acceso)
                    .semantics { if (scritto != nome) contentDescription = nome }
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
 * superficie incavata col risultato.
 *
 * ⚠️⚠️ **DALLA `2.81` SI CHIAMA 'RISULTATO', ED È TUTTO CENTRATO** (voce `resize-finestra`:
 * *'Anteprima' diventa 'Risultato', ed è più piccolo e centrato. Le misure finali sono centrate,
 * e forse per coerenza potrebbero avere la stessa resa grafica e gli stessi colori del risultato
 * della rinomina (ma con il testo più grande)*). Quindi la stringa di 'Rinomina' non si riusa più
 * e ne nasce una: 'Anteprima' dice che cosa si sta guardando, 'Risultato' dice che cosa si
 * otterrà, e sono due parole diverse.
 * ⚠️ **I colori sono quelli della SECONDA pastiglia della rinomina**, cioè quella del nome nuovo:
 * riempimento `secondaryContainer` e filo d'accento. ⚠️ **Il pezzo però non si riusa**, e la
 * ragione è misurata: `NamePill` porta con sé lo stringimento anti-orfano e un corpo piccolo, che
 * qui non hanno niente da fare, e il testo di qui è più grande per sua richiesta.
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
 * La stessa misura senza grassetto, che è la riga sotto il titolo.
 *
 * ⚠️⚠️ **DALLA `2.81` NON PASSA PIÙ DA UNA FRASE TRADOTTA, e con lei se ne sono andate la
 * stringa `look_resize_now` e la funzione che le infilava dentro il numero**: quella frase
 * diceva 'Dimensioni attuali:', e il suo testo `t-resize-now` la toglie, quindi restava un
 * segnaposto da tradurre in ventotto lingue.
 */
internal fun plain(w: Int, h: Int): String = "$w $BY $h $PX"

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

/** Il gettone della percentuale, che dalla `2.81` è il segno e non la parola. */
private const val PCT = "%"

/**
 * Quanto il segno fra i due campi scende, per cadere sulle cifre invece che sul campo.
 *
 * ⚠️ **Il conto vive sul Row che lo usa**, con la misura di un campo di Material e della sua
 * riga di testo: qui basta sapere che è la distanza fra il fondo di un campo e il fondo della
 * riga che si scrive dentro.
 */
private val FIELD_TEXT_DROP = 8.dp

/**
 * Quante cifre si accettano in un campo.
 *
 * ⚠️ **Cinque bastano al tetto dei pixel e avanzano alla percentuale**, e servono a non far
 * scrivere un numero così lungo da non entrare in un intero: il campo dice comunque quali valori
 * sono buoni, e il taglio è solo la rete.
 */
private const val RESIZE_DIGITS = 5

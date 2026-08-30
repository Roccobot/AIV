package io.github.roccobot.aiv

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Una cartella intera in miniature, ed è il primo passo della galleria.
 *
 * ⚠️⚠️ **L'ORDINE È QUELLO DI LETTURA, non uno suo**, ed è la decisione che tiene
 * insieme le due viste: le miniature stanno nella stessa sequenza in cui la
 * strisciata le sfoglierà, quindi 'la prossima' è la stessa cosa qui e là. Col verso
 * predefinito (`Cambia verso` spenta) è la più recente per prima, cioè l'ordine di
 * una galleria; accendendo l'impostazione si girano tutte e due insieme, perché
 * vengono dalla stessa lista girata una volta sola (vedi `Folder.Series.reversed`).
 * ⚠️ Chi un domani volesse la griglia 'sempre dalla più recente' rompe questa
 * corrispondenza: il tocco sulla terza miniatura aprirebbe la terzultima foto.
 *
 * ⚠️ **La lista arriva GIÀ PRONTA dal modello e non si interroga il MediaStore qui**:
 * è la stessa serie che il visualizzatore userà per sfogliare, quindi aprire una foto
 * dalla griglia non costa nessuna query e non può dare due ordini diversi.
 *
 * ⚠️ **Le miniature NON passano dal decodificatore normale**: le chiede al sistema
 * `Thumbs`, e là sta scritto perché.
 *
 * ⚠️⚠️ **[onChanged] SI CHIAMA DOPO OGNI OPERAZIONE, e senza di lui la griglia
 * mentirebbe**: copia, sposta, rinomina ed elimina cambiano i file sul disco, quindi la
 * lista che questa schermata ha in mano diventa vecchia nell'istante in cui l'operazione
 * finisce. Senza una rilettura resterebbero le miniature di fotografie che non esistono
 * più, e toccarle aprirebbe il vuoto.
 */
@Composable
fun GridScreen(
    title: String,
    items: List<Uri>?,
    highlight: Int?,
    onOpen: (Int) -> Unit,
    onBack: () -> Unit,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Il testo cercato, e `null` quando questa non è una ricerca.
     *
     * ⚠️⚠️ **DUE PARAMETRI INVECE DI UNA SCHERMATA NUOVA, ed è la scelta che regge la
     * ricerca**: fra la griglia di una cartella e quella dei risultati cambia **solo la
     * testata**, e tutto il resto (miniature, selezione multipla, copia, sposta, rinomina,
     * elimina, apertura, anello) è lo stesso identico codice. Una schermata a parte
     * avrebbe voluto una seconda copia di tutto quello, cioè il posto dove le due si
     * sarebbero messe a divergere.
     */
    query: String? = null,
    onQuery: (String) -> Unit = {},
    /**
     * I campi delle informazioni sul file, nell'ordine scelto: `Settings.factRows`.
     *
     * ⚠️ **Arriva un elenco e non le impostazioni intere**: questa schermata non ne usa
     * nient'altro, e passarle tutte vorrebbe dire ricomporre la griglia a ogni ritocco di
     * una voce che qui non c'entra niente.
     * ⚠️ Il valore di serie tiene in piedi le anteprime e i richiami che non lo passano.
     */
    factFields: List<FactField> = FactField.entries,
    /**
     * Se questa griglia è il **cestino**.
     *
     * ⚠️⚠️ **CAMBIA TRE COSE E NON L'ASPETTO**: 'elimina' diventa definitiva (là dentro non
     * c'è un secondo cestino), 'rinomina' diventa 'ripristina' (un file nel cestino non si
     * rinomina, richiesta dell'utente), e il tastino compare **anche senza selezione**, per
     * offrire 'svuota il cestino'. Tutto il resto, miniature comprese, è la griglia di
     * sempre: era la richiesta, cioè che il cestino si navighi come una cartella qualunque.
     */
    bin: Boolean = false
) {
    val state = rememberLazyGridState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    /**
     * Gli INDIRIZZI scelti, non le posizioni.
     *
     * ⚠️⚠️ **Le posizioni sarebbero un difetto in attesa**: la lista si ricarica quando la
     * cartella cambia, e un indice che era la terza foto diventa la terza **di un'altra
     * lista** senza che niente lo dica. Un indirizzo o c'è ancora o non c'è, e nel secondo
     * caso sparisce dalla selezione da sé.
     * ⚠️ Vive nella SCHERMATA e non nel modello, perché muore con lei: uscire da una
     * cartella è il modo naturale di dire 'lascia stare'.
     */
    var chosen by remember(items) { mutableStateOf<Set<Uri>>(emptySet()) }
    var menuOpen by remember { mutableStateOf(false) }

    /**
     * Il dialogo di un'operazione, e `null` quando non ce n'è aperto nessuno.
     *
     * ⚠️ **Uno stato solo per quattro dialoghi**, dalla `0.62`: erano quattro variabili, e
     * quattro booleani indipendenti descrivono sedici combinazioni di cui quindici
     * impossibili. Qui i dialoghi si escludono per costruzione. ⚠️ Porta con sé le immagini
     * su cui lavorare, e il perché sta in [FileJob].
     */
    var job by remember { mutableStateOf<FileJob?>(null) }

    /** Se si sta chiedendo di svuotare il cestino. Vale solo quando [bin] è vero. */
    var emptying by remember { mutableStateOf(false) }
    val picking = chosen.isNotEmpty()

    // ⚠️ Indietro esce dalla SELEZIONE prima di uscire dalla cartella: chi ha scelto
    // trenta foto e tocca Indietro per sbaglio non deve ritrovarsi due schermate
    // indietro con la selezione persa.
    BackHandler(enabled = picking) { chosen = emptySet() }

    /**
     * Dove sta il dito mentre trascina una selezione, e `null` quando non trascina.
     *
     * ⚠️ Esiste anche per lo SCORRIMENTO AI BORDI: senza un posto in cui leggere la
     * posizione fuori dai richiami del gesto, la griglia non potrebbe scorrere da sola
     * mentre il dito sta fermo appoggiato in fondo allo schermo.
     */
    var dragAt by remember { mutableStateOf<Offset?>(null) }

    /** Da dove è partita la selezione da/a. Null quando non si sta trascinando. */
    var dragFrom by remember { mutableStateOf<Int?>(null) }

    /**
     * La selezione com'era **prima** che questo trascinamento cominciasse.
     *
     * ⚠️ Senza, tornare indietro col dito non toglierebbe niente: la selezione va
     * **ricostruita** a ogni fotogramma come 'quella di prima più l'intervallo di adesso',
     * non accumulata. Accumulando, un intervallo attraversato per sbaglio resterebbe scelto
     * anche dopo essere tornati sui propri passi.
     */
    var dragBase by remember { mutableStateOf<Set<Uri>>(emptySet()) }

    // Le due misure dello scorrimento ai bordi, in pixel: servono dentro un effetto, che
    // non ha una densità sotto mano.
    val density = LocalDensity.current
    val edgePx = with(density) { EDGE_BAND.toPx() }
    val speedPx = with(density) { EDGE_SPEED.toPx() }

    /*
     * ⚠️⚠️ UNA VOLTA SOLA PER VISITA, e la bandierina non è pignoleria: alla ROTAZIONE
     * `rememberLazyGridState` ripristina da sé il punto in cui si stava scorrendo
     * (dentro è un `rememberSaveable`), e un effetto che riparte lo butterebbe via
     * riportando la griglia sulla foto da cui si era entrati. Anche la bandierina è
     * saveable, per la stessa ragione.
     * ⚠️ Cambiando SCHERMATA invece il composable esce dalla composizione e si porta via
     * la bandierina: rientrando ci si riposiziona, che è esattamente quello che serve.
     */
    var placed by rememberSaveable { mutableStateOf(false) }

    /*
     * Tornando dal visualizzatore la griglia si porta SULLA foto che si stava guardando:
     * dopo dieci strisciate, ritrovarsi in cima è perdere il posto.
     *
     * ⚠️⚠️ **SI ASPETTA LA PRIMA MISURA PRIMA DI DECIDERE**, e senza quell'attesa la
     * griglia si muoverebbe SEMPRE: al primo giro di composizione nessun riquadro è
     * ancora stato disposto, quindi 'non è in vista' sarebbe vero anche per una foto
     * che sta benissimo nella prima schermata, e la si vedrebbe saltare in cima per
     * niente. L'utente ha chiesto lo scorrimento **solo** se la foto è fuori dalla
     * vista iniziale.
     * ⚠️ Ci si porta l'intero riquadro dentro lo schermo, non un pezzo: una miniatura
     * mezza tagliata dal bordo è 'in vista' per il codice e non per chi guarda.
     */
    LaunchedEffect(items, highlight) {
        if (placed || items == null || highlight == null) return@LaunchedEffect
        if (highlight !in items.indices) return@LaunchedEffect
        placed = true
        snapshotFlow { state.layoutInfo.totalItemsCount }.first { it > 0 }
        val info = state.layoutInfo
        val seen = info.visibleItemsInfo.firstOrNull { it.index == highlight }
        val whole = seen != null &&
            seen.offset.y >= 0 &&
            seen.offset.y + seen.size.height <= info.viewportSize.height
        if (!whole) state.scrollToItem(highlight)
    }

    // ⚠️ Le risorse si prendono da `LocalResources` e non da `context.resources`, e non è
    // pignoleria di lint: quest'ultimo non segue i cambi di configurazione, quindi dopo un
    // cambio di lingua o una rotazione servirebbe la versione vecchia. Si legge QUI,
    // mentre si compone, e si usa dentro le coroutine.
    val res = LocalResources.current

    /**
     * Il giro che fanno tutte e quattro le operazioni: si parte, si dice com'è andata, si
     * rilegge la cartella.
     *
     * ⚠️⚠️ **LA SELEZIONE SI SVUOTA SUBITO, prima che il lavoro finisca**: è partito, e
     * lasciare le spunte accese inviterebbe a toccare la stessa voce una seconda volta
     * mentre la prima è ancora in corso. Chi chiama deve quindi essersi già preso la sua
     * lista, ed è il motivo per cui [work] la riceve dall'esterno invece di leggerla qui.
     * ⚠️ **Il testo dell'esito lo compone `outcomeText`**, condiviso col visualizzatore.
     */
    val perform: (FileKind, suspend () -> FileTree.Outcome) -> Unit = { kind, work ->
        chosen = emptySet()
        scope.launch {
            val out = work()
            Toast.makeText(context, outcomeText(res, out, kind.done), Toast.LENGTH_LONG).show()
            onChanged()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        // ⚠️⚠️ **LA BARRA DELLA SELEZIONE PRENDE IL POSTO DEL TITOLO invece di aggiungersi
        // sopra**: due barre insieme mangerebbero un quarto di schermo alle miniature, che
        // sono la cosa per cui si è entrati. Ed è anche il modo di dire che si è in un
        // modo diverso, senza scriverlo.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (picking) chosen = emptySet() else onBack() }) {
                Icon(
                    imageVector = if (picking) Icons.Default.Close
                    else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(
                        if (picking) R.string.pick_leave else R.string.settings_back
                    )
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                if (query != null && !picking) {
                    /*
                     * ⚠️⚠️ **IL CAMPO PRENDE IL FUOCO DA SÉ, e senza questo la ricerca si
                     * apre su una schermata che non fa niente**: chi tocca 'Cerca' ha già
                     * in mente la parola, e trovarsi davanti un campo spento con la
                     * tastiera chiusa vuol dire un tocco in più prima di poter scrivere.
                     * ⚠️ Una volta sola per visita: rimettere il fuoco a ogni
                     * ricomposizione riaprirebbe la tastiera dopo che la si è chiusa per
                     * guardare i risultati, che è precisamente quando la si vuole via.
                     */
                    val focus = remember { FocusRequester() }
                    LaunchedEffect(Unit) { focus.requestFocus() }
                    TextField(
                        value = query,
                        onValueChange = onQuery,
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        singleLine = true,
                        // ⚠️ Senza contorno e senza fondo: qui sta al posto di un titolo,
                        // e un campo squadrato in testata sembrerebbe un modulo da
                        // compilare invece della riga che dice dove si è.
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        trailingIcon = if (query.isEmpty()) null else ({
                            IconButton(onClick = { onQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.search_clear)
                                )
                            }
                        }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus)
                    )
                } else {
                    Text(
                        text = if (picking) pluralStringResource(
                            R.plurals.pick_count, chosen.size, chosen.size
                        ) else title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1
                    )
                    if (!picking) items?.let {
                        Text(
                            text = pluralStringResource(R.plurals.folders_count, it.size, it.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // ⚠️ 'Tutte' sta accanto al conto e non nel riquadro delle azioni, ed è la
            // sola cosa rimasta qui: su una cartella da trecento foto il gesto
            // alternativo è trecento tocchi, e non è un'operazione sui file ma un modo
            // di scegliere. La barra parla della selezione, il tastino di cosa farne.
            if (picking) {
                IconButton(onClick = { chosen = items?.toSet() ?: emptySet() }) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = stringResource(R.string.pick_all)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        when {
            items == null -> CircularProgressIndicator(
                Modifier.padding(top = 24.dp).size(28.dp).align(Alignment.CenterHorizontally)
            )

            // ⚠️ Un elenco vuoto vuol dire due cose diverse, e dirle con la stessa frase
            // sarebbe un piccolo inganno: in una cartella significa che le foto non ci sono
            // più, in una ricerca che nessun nome combacia, e a ricerca ancora da scrivere
            // non significa niente e non si dice nulla.
            items.isEmpty() -> when {
                // ⚠️ Tre frasi per tre vuoti diversi, e dirle con la stessa sarebbe un
                // piccolo inganno: un cestino vuoto è una buona notizia, una cartella
                // vuota vuol dire che le foto non ci sono più, e una ricerca senza esito
                // che nessun nome combacia.
                bin -> Text(
                    text = stringResource(R.string.bin_none),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 24.dp, start = 12.dp)
                )
                query == null -> Text(
                    text = stringResource(R.string.folder_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 24.dp, start = 12.dp)
                )
                query.isNotBlank() -> Text(
                    text = stringResource(R.string.search_none, query),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 24.dp, start = 12.dp)
                )
                else -> Unit
            }

            else -> {
            /*
             * ⚠️⚠️ **IL GESTO STA SULLA GRIGLIA E NON SULLE PIASTRELLE, perché comincia su
             * una e finisce su un'altra** (richiesta dell'utente: *se striscio da una foto
             * all'altra deve avvenire una selezione da/a*). Una piastrella vede solo sé
             * stessa; la griglia le vede tutte e sa dove sono.
             * ⚠️⚠️ **LA CHIAVE DEL `pointerInput` NON COMPRENDE LA SELEZIONE, e sarebbe il
             * difetto che è già costato una versione** (la `0.32`): cambiare una chiave
             * **annulla il gesto in corso**, quindi con `chosen` fra le chiavi il
             * trascinamento morirebbe alla prima foto aggiunta, cioè subito. La selezione
             * si legge **dentro** il gesto, che è lettura e non chiave.
             * ⚠️ Il gesto si limita a dire **dove** sta il dito: chi estende la selezione è
             * l'effetto qui sotto, e averne uno solo vuol dire che il conto è identico sia
             * che si muova il dito sia che si muova la griglia sotto a un dito fermo.
             */
            val grab = Modifier.pointerInput(items) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { at ->
                        val hit = state.itemIndexAt(at)
                        if (hit != null) {
                            dragFrom = hit
                            dragBase = chosen
                            chosen = chosen + items[hit]
                            dragAt = at
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        if (dragFrom != null) dragAt = change.position
                    },
                    onDragEnd = { dragFrom = null; dragAt = null },
                    onDragCancel = { dragFrom = null; dragAt = null }
                )
            }

            /*
             * ⚠️⚠️ **LO SCORRIMENTO AI BORDI È QUELLO CHE RENDE LA FUNZIONE UTILE, non un
             * ornamento**: senza, una selezione da/a arriva al massimo fino al bordo dello
             * schermo, cioè a una quindicina di foto, e chi ne vuole cinquanta torna a
             * toccarle una per una. Col dito appoggiato in fondo la griglia scorre e la
             * selezione lo segue.
             * ⚠️⚠️ **Si aggiorna a ogni FOTOGRAMMA e non a ogni evento del dito**, ed è la
             * ragione per cui questo lavoro non sta dentro `onDrag`: mentre la griglia
             * scorre sotto un dito **fermo** non arriva nessun evento di puntatore, e la
             * selezione resterebbe ferma insieme a lui.
             * ⚠️ La spinta cresce **avvicinandosi al bordo** invece di essere un
             * interruttore: a velocità unica o si striscia piano e non basta, o si arriva
             * in fondo alla cartella prima di accorgersene.
             */
            LaunchedEffect(dragAt != null, items) {
                while (dragAt != null) {
                    withFrameNanos { }
                    val at = dragAt ?: break
                    val from = dragFrom ?: break
                    val height = state.layoutInfo.viewportSize.height.toFloat()
                    val push = when {
                        height <= 0f -> 0f
                        at.y < edgePx -> -(edgePx - at.y) / edgePx
                        at.y > height - edgePx -> (at.y - (height - edgePx)) / edgePx
                        else -> 0f
                    }
                    if (push != 0f) state.scrollBy(push.coerceIn(-1f, 1f) * speedPx)
                    val hit = state.itemIndexAt(at) ?: continue
                    chosen = dragBase + items.subList(minOf(from, hit), maxOf(from, hit) + 1)
                }
            }

            /*
             * ⚠️⚠️ **IL RIQUADRO STA IN UN `Box` INTORNO ALLA SOLA GRIGLIA, e non intorno
             * a tutta la schermata**: è la parte su cui il tastino galleggia, quindi
             * avvolgere il resto avrebbe voluto dire spostare di rientro trecento righe
             * per niente. ⚠️ Il `weight` serve: senza, con tre sole fotografie il `Box`
             * sarebbe alto quanto loro e il tastino finirebbe a mezza schermata invece che
             * in basso.
             */
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                LazyVerticalGrid(
                    // ⚠️ `Adaptive` e non un numero fisso di colonne: la stessa misura
                    // minima dà tre colonne su un telefono e sei su un tablet o in
                    // orizzontale, senza un ramo per ogni forma di schermo.
                    columns = GridCells.Adaptive(minSize = THUMB),
                    state = state,
                    horizontalArrangement = Arrangement.spacedBy(GAP),
                    verticalArrangement = Arrangement.spacedBy(GAP),
                    // ⚠️ Il fondo cresce **con la selezione**, cioè quando il tastino
                    // compare: senza, la fotografia in basso a destra resterebbe coperta
                    // proprio mentre la si deve poter toccare. Fuori dalla selezione il
                    // tastino non c'è e quello spazio sarebbe un buco.
                    contentPadding = PaddingValues(bottom = if (picking) BELOW_FAB else 16.dp),
                    modifier = Modifier.fillMaxWidth().then(grab)
                ) {
                    itemsIndexed(
                        items = items,
                        // ⚠️ La chiave è l'indirizzo e non la posizione: senza, ruotando
                        // il telefono le miniature già decodificate si rimescolerebbero
                        // fra i riquadri.
                        key = { _, uri -> uri.toString() },
                        // Un tipo solo per tutti i riquadri: così Compose riusa la
                        // composizione di quelli che escono per quelli che entrano,
                        // invece di ricostruirla a ogni riga che scorre.
                        contentType = { _, _ -> THUMB_KIND }
                    ) { index, uri ->
                        Thumbnail(
                            uri = uri,
                            position = index + 1,
                            total = items.size,
                            marked = index == highlight,
                            chosen = uri in chosen,
                            // ⚠️ In selezione il tocco NORMALE sceglie invece di aprire,
                            // ed è la convenzione di ogni galleria: chi ne ha scelte
                            // cinque e tocca la sesta ne vuole sei, non vuole uscire e
                            // perderle.
                            /*
                             * ⚠️⚠️ **IL PRIMO RAMO RIPARA IL TOCCO LUNGO SU UNA SOLA FOTO,
                             * che dalla 0.53 non avviava più la selezione** (riscontro
                             * dell'utente sulla 0.65). Il gesto era sano: il difetto stava
                             * qui. `Modifier.clickable` **senza** `onLongClick` fa scattare
                             * il tocco al rilascio **qualunque sia stata la durata** della
                             * pressione, e nella passata `Main` gli eventi vanno dal figlio
                             * al genitore, quindi la piastrella li vede prima della griglia:
                             * il tocco lungo selezionava la foto, il dito si alzava, questo
                             * richiamo partiva con `picking` già vero e la **toglieva**.
                             * Effetto netto, niente. La spia è `dragFrom` e non un flag
                             * nuovo perché vale esattamente fra `onDragStart` e la fine del
                             * gesto: un tocco che la trova impostata è la **coda** di un
                             * tocco lungo, e un tocco normale non la trova mai, perché
                             * senza tocco lungo `onDragStart` non parte.
                             * ⚠️ **Col trascinamento non si vedeva**, ed è la ragione per
                             * cui la prova della 0.53 non l'aveva scoperto: `onDrag` consuma
                             * gli eventi, e un tocco i cui eventi sono consumati si annulla
                             * da sé. Il difetto viveva nel solo caso del dito fermo, cioè
                             * nel gesto che si fa per selezionarne una.
                             * ⚠️ **Non si ripara dando un `onLongClick` alla piastrella**,
                             * che è la strada ovvia: al tocco lungo `combinedClickable`
                             * consuma tutto fino al rilascio, e il gesto della griglia
                             * verrebbe annullato. Si perderebbe la selezione da/a per
                             * riparare quella singola.
                             */
                            onClick = {
                                when {
                                    dragFrom != null -> Unit
                                    picking -> chosen = chosen.toggle(uri)
                                    else -> onOpen(index)
                                }
                            }
                        )
                    }
                }

                /*
                 * ⚠️⚠️ **LE OPERAZIONI STANNO IN UN TASTINO DEDICATO dalla 0.61**
                 * (richiesta dell'utente: *le azioni disponibili devono comparire in un
                 * FAB dedicato, icona diversa, tipo hamburger*). Prima stavano in testata,
                 * un tastino per 'condividi' e un menu a tendina per le altre cinque, e
                 * il conto delle foto scelte si giocava lo spazio con loro.
                 * ⚠️ **Nella griglia delle foto non c'è nessun tastino da sostituire**, e
                 * va detto invece di lasciarlo scoprire: quello quadrato vive nella
                 * schermata delle cartelle. Qui il tastino **compare** con la selezione e
                 * sparisce con lei, che è il modo di dire che si è in un modo diverso.
                 * ⚠️ **Le sei azioni le disegna [ActionPad], che è condiviso col
                 * visualizzatore**: l'ordine e le icone stanno là, una volta sola.
                 * ⚠️ Il margine è 8 e non 16 come quello delle cartelle perché la griglia
                 * sta già dentro il margine della schermata, e i due si sommano.
                 */
                if (picking || bin) {
                    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                        SmallFloatingActionButton(
                            onClick = { menuOpen = true },
                            shape = RoundedCornerShape(FAB_CORNER)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.pick_actions)
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            // ⚠️ Nel cestino senza niente scelto il tastino non porta le sei
                            // operazioni, che non avrebbero su cosa agire, ma la sola voce
                            // che riguarda il cestino intero.
                            if (!picking) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.bin_empty)) },
                                    leadingIcon = { Icon(Icons.Default.DeleteForever, null) },
                                    colors = MenuDefaults.itemColors(
                                        textColor = MaterialTheme.colorScheme.error,
                                        leadingIconColor = MaterialTheme.colorScheme.error
                                    ),
                                    onClick = { menuOpen = false; emptying = true }
                                )
                            } else ActionPad(
                                actions = listOf(
                                    PadAction(Icons.Default.ContentCopy, R.string.menu_copy_here) {
                                        menuOpen = false
                                        job = FileJob.Transfer(chosen.toList(), move = false)
                                    },
                                    PadAction(
                                        Icons.AutoMirrored.Filled.DriveFileMove,
                                        R.string.pick_move
                                    ) {
                                        menuOpen = false
                                        job = FileJob.Transfer(chosen.toList(), move = true)
                                    },
                                    PadAction(
                                        Icons.Default.Delete,
                                        R.string.pick_delete,
                                        danger = true
                                    ) {
                                        menuOpen = false
                                        job = FileJob.Delete(chosen.toList(), forGood = bin)
                                    },
                                    // ⚠️ Nel cestino al posto della rinomina c'è il
                                    // ripristino: un file là dentro non si rinomina
                                    // (richiesta dell'utente), e il posto nel riquadro è
                                    // lo stesso, così le sei icone non ballano.
                                    if (bin) {
                                        PadAction(
                                            Icons.Default.SettingsBackupRestore,
                                            R.string.bin_restore
                                        ) {
                                            menuOpen = false
                                            job = FileJob.Restore(chosen.toList())
                                        }
                                    } else {
                                        PadAction(Icons.Default.Edit, R.string.pick_rename) {
                                            menuOpen = false
                                            job = FileJob.Rename(chosen.toList())
                                        }
                                    },
                                    PadAction(Icons.Default.Share, R.string.menu_share) {
                                        menuOpen = false
                                        // ⚠️ La lista si prende ADESSO: la condivisione
                                        // gira in una coroutine, e leggere `chosen` da
                                        // dentro leggerebbe una selezione che nel
                                        // frattempo può essere cambiata.
                                        val list = chosen.toList()
                                        scope.launch { ImageActions.shareMany(context, list) }
                                    },
                                    PadAction(Icons.Outlined.Info, R.string.pick_info) {
                                        menuOpen = false
                                        job = FileJob.Facts(chosen.toList())
                                    }
                                )
                            )
                        }
                    }
                }
            }
            }
        }
    }

    /*
     * ⚠️⚠️ **QUESTO DIALOGO STA QUI E NON IN `FileOps.kt`, e la ragione è che non parla di
     * file scelti**: le altre operazioni ricevono un elenco, questa svuota una cartella
     * intera, quindi non entra in `FileJob`, che è fatto di elenchi. Sta nel solo posto da
     * cui si può chiedere, cioè il tastino del cestino.
     * ⚠️ L'esito usa l'avviso dell'eliminazione, che è quello che succede: i file vanno via
     * per davvero.
     */
    if (emptying) {
        AlertDialog(
            onDismissRequest = { emptying = false },
            title = { Text(stringResource(R.string.bin_empty_ask)) },
            text = { Text(stringResource(R.string.bin_empty_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        emptying = false
                        perform(FileKind.DELETE) { Bin.empty(context) }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(stringResource(R.string.bin_empty)) }
            },
            dismissButton = {
                TextButton(onClick = { emptying = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // ⚠️ I cinque dialoghi stanno in `FileOps.kt` perché li chiede anche il
    // visualizzatore: qui resta la sola cosa che è di questa schermata, cioè che a
    // operazione finita la cartella si rilegge.
    FileJobDialogs(
        job = job,
        fields = factFields,
        onClose = { job = null },
        onRun = perform
    )
}

/** Toglie o mette, che è quello che fa un tocco su una cosa selezionabile. */
private fun Set<Uri>.toggle(uri: Uri): Set<Uri> = if (uri in this) this - uri else this + uri

/**
 * Quale riquadro sta sotto un punto, o `null` se là non c'è niente.
 *
 * ⚠️ Si guardano i soli riquadri **in vista**, che è tutto quello che serve e tutto quello
 * che si può sapere: di una fotografia fuori schermo la griglia pigra non conosce nemmeno
 * la posizione. ⚠️ Un punto nei distacchi fra le piastrelle non appartiene a nessuna, e
 * torna `null` invece del vicino più prossimo: durante un trascinamento significa che
 * l'intervallo non cambia per un istante, che è meglio di un intervallo che salta.
 */
private fun LazyGridState.itemIndexAt(at: Offset): Int? =
    layoutInfo.visibleItemsInfo.firstOrNull {
        at.x >= it.offset.x && at.x < it.offset.x + it.size.width &&
            at.y >= it.offset.y && at.y < it.offset.y + it.size.height
    }?.index

/**
 * Un riquadro della griglia.
 *
 * ⚠️⚠️ **IL SEGNO È UN RIQUADRO SOPRA, NON UN BORDO NELLA CATENA DEI MODIFICATORI**, e
 * la differenza è la lezione della `0.34`, dove l'anello non si vedeva: un `Modifier.border`
 * dipende da dove sta nella catena e da come il nodo che disegna l'immagine si comporta col
 * `drawContent`, cioè da due cose che stanno in due librerie diverse. Due fratelli dentro un
 * `Box` invece si dipingono nell'ordine in cui sono scritti, e su questo non c'è niente da
 * sapere: il secondo sta sopra il primo, sempre.
 * ⚠️ Il velo colorato non è decorazione in più: un filo di 3dp su una miniatura piena di
 * dettagli si perde, mentre una tinta sull'intero riquadro si vede dall'altra parte della
 * stanza, che è quello che serve a ritrovare il proprio posto.
 * ⚠️ Il riquadro di sopra **non intercetta il tocco**: in Compose partecipa al colpo solo
 * chi porta un modificatore di puntatore, e qui non ce n'è. Il tocco arriva all'immagine
 * sotto, che è quella che apre.
 */
@Composable
private fun Thumbnail(
    uri: Uri,
    position: Int,
    total: Int,
    marked: Boolean,
    chosen: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(CORNER)
    // La richiesta si costruisce una volta per indirizzo: ricrearla a ogni
    // ricomposizione darebbe a Coil un oggetto nuovo da confrontare per ogni fotogramma
    // di scorrimento, e questo è il posto in cui i fotogrammi contano.
    val context = LocalContext.current
    val model = remember(uri, context) { Thumbs.request(context, uri) }

    Box(modifier = Modifier.aspectRatio(1f)) {
        AsyncImage(
            // ⚠️ La richiesta viene da `Thumbs` e non è costruita qui: la misura è parte
            // della chiave di cache, quindi deve essere la stessa dovunque (vedi `Thumbs.PX`).
            model = model,
            // ⚠️⚠️ **La chiave si registra anche QUI, e non è una ripetizione della
            // `Preview` del visualizzatore**: questo copre la PRIMA fotografia che si
            // apre, quella toccata nella griglia, per la quale nessuna vicina ha ancora
            // caricato niente. Senza, all'ingresso nel visualizzatore resterebbe il
            // fotogramma vuoto che tutto il resto serve a togliere. Il perché sta accanto
            // a `Thumbs.note`.
            onState = { st ->
                if (st is AsyncImagePainter.State.Success) Thumbs.note(uri, st.result.memoryCacheKey)
            },
            // Ogni riquadro è toccabile, quindi non è decorativo: chi legge con TalkBack
            // deve sapere dove si trova nella cartella, e se è quello da cui è tornato.
            contentDescription = stringResource(
                if (marked) R.string.grid_item_last else R.string.grid_item,
                position,
                total
            ),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                // Il fondo si vede finché la miniatura non è pronta: senza, la griglia
                // lampeggerebbe del colore della pagina.
                .background(MaterialTheme.colorScheme.surfaceVariant)
                // ⚠️⚠️ **IL TOCCO LUNGO NON STA PIÙ QUI, ed è la lezione già pagata dalla
                // `0.22`**: dalla `0.53` il tocco lungo apre una selezione **da/a** che
                // continua col trascinamento, e un gesto che comincia su una piastrella e
                // finisce su un'altra non può vivere dentro la piastrella. Sta sulla
                // griglia, che è l'unica che le vede tutte. ⚠️ Chi volesse aggiungere un
                // gesto lo aggiunga **dentro** quello, non accanto.
                .clickable(onClick = onClick)
        )
        /*
         * ⚠️⚠️ **IL VELO DELLA SCELTA VA PRIMA DEL NASTRO, e l'ordine è una decisione**:
         * dentro un `Box` si dipinge nell'ordine in cui si scrive, quindi il nastro
         * disegnato dopo resta **pieno** anche su una foto scelta. Al contrario, il velo
         * sopra lo schiarirebbe insieme alla fotografia, e i due segni che devono
         * distinguersi comincerebbero a somigliarsi proprio sulla piastrella dove
         * convivono, che è il caso peggiore.
         * ⚠️ **SCHIARISCE, non scurisce** (richiesta dell'utente, 2026-08-29): scurire
         * faceva sembrare la foto scelta più lontana, come se fosse stata messa da parte,
         * mentre sceglierla è tirarla avanti.
         */
        if (chosen) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(Color.White.copy(alpha = PICKED_VEIL))
            )
        }
        if (marked) {
            /*
             * ⚠️⚠️ **UN NASTRO NELL'ANGOLO IN BASSO A SINISTRA dalla 0.58** (scelta
             * dell'utente fra cinque proposte, 2026-08-30). Prima era una cornice
             * tratteggiata, e il difetto non era il contrasto ma il **linguaggio**: una
             * cornice attorno a una miniatura è il gesto universale della **selezione**,
             * quindi da lontano quel segno diceva la cosa sbagliata. Un triangolo in un
             * angolo non somiglia a niente di tutto ciò.
             * ⚠️⚠️ **L'angolo è quello DIAGONALMENTE OPPOSTO alla spunta, ed è la ragione
             * per cui è in basso a sinistra e non altrove**: sulla piastrella che è insieme
             * vista e scelta i due segni stanno alla massima distanza possibile e non si
             * toccano mai. Chi lo spostasse 'per simmetria' rimetterebbe due segni nello
             * stesso angolo.
             * ⚠️ **Il ritaglio agli angoli arrotondati serve**: il triangolo tocca l'angolo
             * in basso a sinistra, che è tondo di `CORNER`, e senza `clip` la punta
             * sborderebbe oltre la sagoma della miniatura.
             * ⚠️ **Il costo, dichiarato**: un angolo di fotografia sparisce sotto il nastro,
             * e la forma triangolare di per sé non dice nulla, va imparata. Era il baratto
             * scritto accanto alla proposta, e l'utente l'ha scelta sapendolo.
             * ⚠️ Resta un riquadro fratello e non un `Modifier.border` nella catena, che è
             * la lezione della `0.34`: due fratelli si dipingono nell'ordine in cui sono
             * scritti, e su questo non c'è niente da sapere.
             */
            val tint = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .drawBehind {
                        val leg = size.minDimension * MARK_LEG
                        drawPath(
                            path = Path().apply {
                                moveTo(0f, size.height)
                                lineTo(leg, size.height)
                                lineTo(0f, size.height - leg)
                                close()
                            },
                            color = tint
                        )
                    }
            )
        }
        /*
         * ⚠️⚠️ **LA SPUNTA STA SU UN DISCO PIENO, e senza quello non si vedeva**: una
         * icona colorata appoggiata a una fotografia qualunque sparisce contro un
         * fondo dello stesso colore, ed è quello che l'utente ha segnalato. Il disco
         * dell'accento con il glifo del suo `onPrimary` porta con sé il proprio
         * contrasto, quindi si legge su qualunque cosa ci sia sotto.
         * ⚠️ Il glifo è un `Check` nudo e non un `CheckCircle`: il cerchio del secondo
         * sarebbe un contorno dentro un disco pieno, cioè due cerchi.
         * ⚠️ Sta sopra a tutto e in un angolo, non al centro: al centro coprirebbe
         * proprio la parte della fotografia che si sta guardando per decidere se
         * sceglierla.
         */
        if (chosen) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(TICK)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(TICK * 0.72f)
                )
            }
        }
    }
}

/**
 * Il lato minimo di una miniatura.
 *
 * ⚠️ È una misura, non un gusto: a 108dp uno schermo da 360dp di larghezza tiene
 * **tre** colonne con i distacchi, che è la densità delle gallerie di sistema; a 96
 * ne terrebbe quattro, e su un telefono la faccia in una foto di gruppo non si
 * riconosce più.
 */
private val THUMB = 108.dp

/** Il distacco fra le miniature: c'è, ma non deve leggersi come una cornice. */
private val GAP = 3.dp

/**
 * Quanto è lungo il cateto del nastro dell'ultima foto vista, in frazione del lato.
 *
 * ⚠️ È una **frazione** e non una misura in dp, al contrario di tutto il resto qui sotto,
 * e la ragione è che la piastrella non ha una misura fissa: le colonne sono `Adaptive`,
 * quindi su un tablet o in orizzontale la miniatura cresce. Un cateto in dp resterebbe
 * quello di un telefono e su uno schermo grande diventerebbe un francobollo nell'angolo.
 * ⚠️ Il valore viene dal mockup su cui l'utente ha scelto: a 108dp fa poco meno di 48dp
 * di cateto, che è quanto serve perché si legga anche da lontano.
 */
private const val MARK_LEG = 0.44f

/**
 * Quanto si SCHIARISCE una miniatura scelta.
 *
 * ⚠️ Serve ad accompagnare la spunta, non a segnalare da solo: su una fotografia già
 * chiara un velo chiaro non si nota, ed è la ragione per cui il segno vero è il disco.
 * ⚠️ **Schiarisce e non scurisce dalla 0.53**, per scelta dell'utente: una foto scelta
 * deve venire avanti, non mettersi da parte.
 */
private const val PICKED_VEIL = 0.34f

/** Il lato del disco della spunta. Cresciuto nella 0.53, perché non si vedeva abbastanza. */
private val TICK = 28.dp

/** Il raggio degli angoli di una piastrella, in un posto solo perché lo usano in due. */
private val CORNER = 4.dp

/**
 * Quanto è alta la fascia, in cima e in fondo, dentro la quale un dito che trascina fa
 * scorrere la griglia da solo.
 *
 * ⚠️ Larga quanto **mezza piastrella**: più stretta e la si manca, più larga e si comincia
 * a scorrere mentre si sta ancora scegliendo in mezzo allo schermo.
 */
private val EDGE_BAND = 56.dp

/** Quanti pixel al fotogramma, al massimo, cioè col dito sul bordo estremo. */
private val EDGE_SPEED = 14.dp


/** Tutti i riquadri sono la stessa cosa, e dirlo permette a Compose di riusarli. */
private const val THUMB_KIND = "thumb"

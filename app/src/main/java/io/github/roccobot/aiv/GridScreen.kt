package io.github.roccobot.aiv

import android.content.res.Resources
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.PluralsRes
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
    modifier: Modifier = Modifier
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
    var showingFacts by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    /** Quale delle due operazioni sta chiedendo una cartella, e `null` se nessuna. */
    var transfer by remember { mutableStateOf<Transfer?>(null) }

    /**
     * Le immagini che il dialogo di rinomina sta trattando, e `null` quando è chiuso.
     *
     * ⚠️ **Si fotografa la selezione all'apertura invece di leggerla viva**, e non è la
     * stessa cosa: il dialogo carica i nomi in una coroutine legata alla lista che riceve,
     * e una lista ricostruita a ogni ricomposizione la farebbe ripartire da capo.
     */
    var renaming by remember { mutableStateOf<List<Uri>?>(null) }
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
     * ⚠️ **Un avviso solo con tutti e due i numeri**: due avvisi di fila si coprono a
     * vicenda, e il secondo si leggerebbe senza il primo.
     */
    val perform: (Int, suspend () -> FileTree.Outcome) -> Unit = { doneRes, work ->
        chosen = emptySet()
        scope.launch {
            val out = work()
            Toast.makeText(context, outcomeText(res, out, doneRes), Toast.LENGTH_LONG).show()
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
            if (picking) {
                // ⚠️ 'Tutte' sta accanto al conto e non in un menu: su una cartella da
                // trecento foto il gesto alternativo è trecento tocchi.
                IconButton(onClick = { chosen = items?.toSet() ?: emptySet() }) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = stringResource(R.string.pick_all)
                    )
                }
                IconButton(onClick = {
                    val list = chosen.toList()
                    scope.launch { ImageActions.shareMany(context, list) }
                }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.menu_share)
                    )
                }
                /*
                 * ⚠️⚠️ **LE OPERAZIONI STANNO IN UN MENU, e con sette non c'era scelta**:
                 * sette tastini in fila su uno schermo da 360dp lascerebbero al conto
                 * delle foto meno spazio del conto stesso, e il primo a sparire sarebbe
                 * proprio quello che dice quante se ne stanno per cancellare.
                 * ⚠️ **Fuori restano le due che non fanno danni e si usano di più**,
                 * 'tutte' e 'condividi': una selezione la si fa quasi sempre per una di
                 * quelle due, e metterle a due tocchi sarebbe raddoppiare il gesto comune
                 * per abbreviare quello raro.
                 * ⚠️ **Elimina sta in fondo, dopo una riga e nel colore dell'errore**: è
                 * l'unica voce irreversibile del menu, e la distanza dalle altre è quello
                 * che impedisce di toccarla mirando a 'rinomina'.
                 */
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.pick_more)
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_copy_here)) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                            onClick = {
                                menuOpen = false
                                transfer = Transfer.COPY
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pick_move)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null) },
                            onClick = {
                                menuOpen = false
                                transfer = Transfer.MOVE
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pick_rename)) },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = {
                                menuOpen = false
                                renaming = chosen.toList()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pick_info)) },
                            leadingIcon = { Icon(Icons.Outlined.Info, null) },
                            onClick = {
                                menuOpen = false
                                showingFacts = true
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pick_delete)) },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error,
                                leadingIconColor = MaterialTheme.colorScheme.error
                            ),
                            onClick = {
                                menuOpen = false
                                deleting = true
                            }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        when {
            items == null -> CircularProgressIndicator(
                Modifier.padding(top = 24.dp).size(28.dp).align(Alignment.CenterHorizontally)
            )

            items.isEmpty() -> Text(
                text = stringResource(R.string.folder_empty),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp, start = 12.dp)
            )

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

            LazyVerticalGrid(
                // ⚠️ `Adaptive` e non un numero fisso di colonne: la stessa misura
                // minima dà tre colonne su un telefono e sei su un tablet o in
                // orizzontale, senza un ramo per ogni forma di schermo.
                columns = GridCells.Adaptive(minSize = THUMB),
                state = state,
                horizontalArrangement = Arrangement.spacedBy(GAP),
                verticalArrangement = Arrangement.spacedBy(GAP),
                contentPadding = PaddingValues(bottom = 16.dp),
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
                        // ⚠️ In selezione il tocco NORMALE sceglie invece di aprire, ed è
                        // la convenzione di ogni galleria: chi ne ha scelte cinque e tocca
                        // la sesta ne vuole sei, non vuole uscire e perderle.
                        onClick = {
                            if (picking) chosen = chosen.toggle(uri) else onOpen(index)
                        }
                    )
                }
            }
            }
        }
    }

    if (showingFacts) {
        FactsDialog(uris = chosen.toList(), onDismiss = { showingFacts = false })
    }

    transfer?.let { kind ->
        DestinationDialog(
            action = if (kind == Transfer.COPY) R.string.dest_here else R.string.dest_move_here,
            onDismiss = { transfer = null },
            onPick = { dir ->
                val list = chosen.toList()
                transfer = null
                if (kind == Transfer.COPY) {
                    perform(R.plurals.copy_done) { FileTree.copy(context, list, dir) }
                } else {
                    perform(R.plurals.move_done) { FileTree.move(context, list, dir) }
                }
            }
        )
    }

    renaming?.let { list ->
        RenameDialog(
            uris = list,
            onDismiss = { renaming = null },
            onRename = { template, start ->
                renaming = null
                perform(R.plurals.rename_done) {
                    FileTree.rename(context, list, template, start)
                }
            }
        )
    }

    if (deleting) {
        /*
         * ⚠️⚠️ **LA CONFERMA NON È CORTESIA: qui non c'è un cestino.** Il MediaStore ne ha
         * uno, ma ci si finisce solo passando dal provider con la richiesta apposita, e
         * questa app cancella dal disco perché è l'unica via che copre anche i file che
         * nella galleria non ci sono mai entrati. Quindi il gesto è definitivo, e il testo
         * lo dice invece di lasciarlo intuire.
         * ⚠️ Il conto sta nel TITOLO e non nel corpo: è il dato che fa cambiare idea, e
         * chi tocca in fretta legge solo la prima riga.
         */
        val going = chosen.size
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text(pluralStringResource(R.plurals.delete_ask, going, going)) },
            text = { Text(stringResource(R.string.delete_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleting = false
                        val list = chosen.toList()
                        perform(R.plurals.delete_done) { FileTree.delete(context, list) }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(stringResource(R.string.pick_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/** Le due operazioni che chiedono una cartella di arrivo, e che a parte quella differiscono. */
private enum class Transfer { COPY, MOVE }

/**
 * Che cosa dire quando un'operazione finisce: quante sono passate e, solo se ce ne sono,
 * quante no.
 *
 * ⚠️ Le forme plurali si risolvono con `getQuantityString` e non con
 * `pluralStringResource`, perché il numero si sa solo a lavoro finito e quella funzione si
 * può chiamare soltanto mentre si compone.
 */
private fun outcomeText(res: Resources, out: FileTree.Outcome, @PluralsRes doneRes: Int): String =
    buildString {
        append(res.getQuantityString(doneRes, out.done, out.done))
        if (out.failed > 0) {
            append(", ")
            append(res.getQuantityString(R.plurals.op_failed, out.failed, out.failed))
        }
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
        if (marked) {
            /*
             * ⚠️⚠️ **TRATTEGGIATO E SENZA VELO dalla 0.53** (richiesta dell'utente). Prima
             * era un velo colorato su tutto il riquadro più un filo continuo, e il velo
             * serviva a farsi vedere da lontano; il tratteggio fa lo stesso lavoro con una
             * forma invece che con una tinta, e lascia la fotografia com'è.
             * ⚠️ **Il costo, dichiarato**: su una foto molto affollata un filo tratteggiato
             * si legge meno di una tinta su tutto il riquadro. È il baratto che l'utente ha
             * scelto, e chi lo rimette in discussione rimetta prima il velo in discussione
             * con lui.
             * ⚠️ **Rientra di mezzo spessore**, o metà del tratto cadrebbe fuori dai limiti
             * e verrebbe tagliata: un rettangolo si disegna sul suo bordo, metà di qua e
             * metà di là.
             * ⚠️ Resta un riquadro fratello e non un `Modifier.border` nella catena, che è
             * la lezione della `0.34` scritta qui sotto: due fratelli si dipingono
             * nell'ordine in cui sono scritti, e su questo non c'è niente da sapere.
             */
            val tint = MaterialTheme.colorScheme.primary
            val ringPx = with(LocalDensity.current) { RING.toPx() }
            val dashPx = with(LocalDensity.current) { DASH.toPx() }
            val cornerPx = with(LocalDensity.current) { CORNER.toPx() }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        drawRoundRect(
                            color = tint,
                            topLeft = Offset(ringPx / 2f, ringPx / 2f),
                            size = Size(size.width - ringPx, size.height - ringPx),
                            cornerRadius = CornerRadius(cornerPx),
                            style = Stroke(
                                width = ringPx,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(dashPx, dashPx)
                                )
                            )
                        )
                    }
            )
        }
        /*
         * ⚠️⚠️ **IL SEGNO DELLA SCELTA È UN SIMBOLO, NON UN ALTRO VELO, e non è una
         * questione di gusto**: l'anello dell'ultima foto vista è già un velo colorato, e
         * un secondo velo sullo stesso riquadro darebbe due tinte sovrapposte che nessuno
         * saprebbe separare. Una spunta invece si legge da sola, e si legge **insieme**
         * all'anello quando capitano sulla stessa foto.
         * ⚠️ Sta sopra a tutto e in un angolo, non al centro: al centro coprirebbe
         * proprio la parte della fotografia che si sta guardando per decidere se
         * sceglierla.
         */
        if (chosen) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    // ⚠️ **SCHIARISCE, non scurisce** (richiesta dell'utente, 2026-08-29):
                    // scurire faceva sembrare la foto scelta più lontana, come se fosse
                    // stata messa da parte, mentre sceglierla è tirarla avanti.
                    .background(Color.White.copy(alpha = PICKED_VEIL))
            )
            /*
             * ⚠️⚠️ **LA SPUNTA STA SU UN DISCO PIENO, e senza quello non si vedeva**: una
             * icona colorata appoggiata a una fotografia qualunque sparisce contro un
             * fondo dello stesso colore, ed è quello che l'utente ha segnalato. Il disco
             * dell'accento con il glifo del suo `onPrimary` porta con sé il proprio
             * contrasto, quindi si legge su qualunque cosa ci sia sotto.
             * ⚠️ Il glifo è un `Check` nudo e non un `CheckCircle`: il cerchio del secondo
             * sarebbe un contorno dentro un disco pieno, cioè due cerchi.
             */
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

/** L'anello sull'ultima foto guardata: si deve vedere a colpo d'occhio, da lontano. */
private val RING = 4.dp

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

/** La lunghezza di un tratto del bordo tratteggiato, e del vuoto che lo segue. */
private val DASH = 6.dp

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

/**
 * Che cosa si è scelto, in numeri.
 *
 * ⚠️ **I dati si leggono quando il dialogo si apre e non prima**: contare il peso di
 * trecento file vuol dire trecento interrogazioni, e farle a ogni tocco su una miniatura
 * sarebbe pagarle per una domanda che quasi nessuno fa.
 * ⚠️ **Finché non sono pronti si dice che si sta contando**, invece di mostrare uno zero
 * che poi cambia: uno zero che si corregge da solo si legge come un errore.
 */
@Composable
private fun FactsDialog(uris: List<Uri>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val facts by produceState<Facts?>(null, uris) { value = factsOf(context, uris) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_info)) },
        text = {
            val f = facts
            Text(
                text = if (f == null) stringResource(R.string.pick_counting) else buildString {
                    append(pluralStringResource(R.plurals.pick_count, f.count, f.count))
                    append('\n')
                    append(formatBytes(f.bytes))
                    f.name?.let { append('\n').append(it) }
                    if (f.width != null && f.height != null) {
                        append('\n').append(f.width).append(" x ").append(f.height)
                    }
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pick_close)) }
        }
    )
}

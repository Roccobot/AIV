package io.github.roccobot.aiv

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
    bin: Boolean = false,
    /**
     * Apre la cronologia dei ripristini. Vale **solo** quando [bin] è vero.
     *
     * ⚠️ Il valore di serie non fa niente, e va bene: fuori dal cestino la voce che lo
     * chiama non esiste, e un parametro obbligatorio costringerebbe le altre due griglie
     * (cartella e ricerca) a passare una funzione che non useranno mai.
     */
    onHistory: () -> Unit = {}
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

    /**
     * Se c'è qualcosa qui dentro.
     *
     * ⚠️ Serve alle due voci del cestino che agiscono su **tutto** (svuota e ripristina
     * tutto): su un cestino vuoto non hanno niente da fare, e offrirle vorrebbe dire una
     * conferma o un avviso che dice '0 fatti'. ⚠️ **Vale anche per la scorciatoia del tocco
     * lungo**, o il menu direbbe di no e il tocco lungo di sì. Con la lista vuota resta la
     * vibrazione e non succede niente, che è quello che 'tutte' fa già su una cartella vuota.
     */
    val filled = !items.isNullOrEmpty()

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

    /**
     * Il VERSO del trascinamento: `true` toglie, `false` aggiunge.
     *
     * ⚠️⚠️ **Lo decide la foto su cui il gesto COMINCIA, e non un modo da accendere**
     * (richiesta dell'utente: *tenendo premuto e trascinando quando una selezione c'è già
     * si possa deselezionare*): partendo da una foto già scelta il trascinamento toglie
     * l'intervallo, partendo da una libera lo aggiunge. È la convenzione di ogni galleria,
     * e soprattutto è l'unica che non ha bisogno di un interruttore da trovare: il gesto
     * dice da sé che cosa vuole.
     * ⚠️ Serve **fuori** dai richiami del gesto perché l'intervallo lo ricostruisce
     * l'effetto qui sotto a ogni fotogramma, e deve sapere in che verso.
     */
    var dragOff by remember { mutableStateOf(false) }

    /**
     * ⚠️⚠️ **[HapticFeedbackType.TextHandleMove] e non `SegmentTick` o `ToggleOn`**, che
     * sarebbero i tipi giusti per nome: quelle costanti sono arrivate con **Android 14**, e
     * Compose passa il numero grezzo a `performHapticFeedback` **senza nessun ripiego**
     * (verificato sul bytecode di `DefaultHapticFeedback`, che mappa `SegmentTick` a 26 e
     * `ToggleOn` a 21 e li consegna così). Sotto Android 14 il telefono riceve una costante
     * che non conosce e **non vibra affatto**, e il minSdk qui è 28. `TextHandleMove` esiste
     * dall'API 27 ed è il tocco leggero che Android usa per le maniglie del testo, cioè
     * esattamente il colpetto chiesto.
     */
    val haptics = LocalHapticFeedback.current

    /**
     * Se i due mini onboarding del tocco lungo sul tastino si sono già visti.
     *
     * ⚠️⚠️ **Il valore di partenza è `true`, cioè 'già visto', e al contrario di quanto
     * sembra è la scelta prudente**: il valore vero arriva dall'archivio un attimo DOPO la
     * prima composizione, quindi partendo da `false` il velo comparirebbe per un
     * fotogramma anche a chi l'ha già chiuso, che è il difetto peggiore dei due. Partendo
     * da `true` il caso peggiore è che compaia un fotogramma più tardi, e nessuno se ne
     * accorge.
     */
    val pickSeen by produceState(initialValue = true, context) {
        Hint.PICK_ALL.flow(context).collect { value = it }
    }
    val binSeen by produceState(initialValue = true, context) {
        Hint.BIN_EMPTY.flow(context).collect { value = it }
    }

    /**
     * 'Tutte', che è il gesto che vale trecento tocchi.
     *
     * ⚠️ Sta in una variabile perché lo chiamano in **tre** posti: il tastino in testata,
     * il tocco lungo sul tastino galleggiante e la sua copia arancione nel velo. Scriverlo
     * tre volte vorrebbe dire tre occasioni di dimenticare la vibrazione in uno dei tre.
     */
    val takeAll: () -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        chosen = items?.toSet() ?: emptySet()
    }

    /**
     * ⚠️⚠️ **IL TOCCO LUNGO È LA SCORCIATOIA DI QUELLO CHE IL TOCCO BREVE OFFRE**, ed è la
     * regola che decide questo `if` (richiesta dell'utente, 2026-08-31: *solo nel cestino,
     * il tocco lungo lo svuota*). Nel cestino **senza niente di scelto** il tocco breve apre
     * il menu del cestino intero, la cui voce grossa è 'Svuota il cestino': la scorciatoia è
     * quella. Appena c'è una selezione, in cestino o in cartella, il tocco breve apre le sei
     * operazioni e la scorciatoia torna a essere 'tutte'.
     * ⚠️ **La scorciatoia NON è cambiata quando il menu è passato a tre voci, dalla 0.76**, e
     * la ragione è la regola qui sopra: le altre due ('Ripristina tutto' e 'Cronologia') sono
     * raggiungibili in un tocco e non hanno bisogno di una scorciatoia, mentre svuotare resta
     * la cosa che un cestino fa.
     * ⚠️ **Il cestino non si svuota MAI con una selezione in corso**, e non è timidezza: chi
     * ha scelto tre foto da ripristinare si aspetta che il gesto agisca su quelle tre, e
     * cancellare invece tutto il cestino sarebbe la sorpresa peggiore che l'app possa fare.
     * La conferma lo fermerebbe comunque, ma un dialogo che chiede una cosa che non hai
     * chiesto è già un difetto.
     * ⚠️ Lo svuotamento **non** si esegue qui: accende il dialogo, che è l'unico posto in
     * cui quel comando esiste (vedi `emptying`).
     */
    val shortcut: () -> Unit = if (bin && !picking) {
        {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (filled) emptying = true
        }
    } else takeAll

    /**
     * Come TalkBack chiama il tocco lungo, che deve dire la stessa cosa che [shortcut] fa.
     *
     * ⚠️ Senza questa, chi usa il lettore di schermo si sentirebbe annunciare 'seleziona
     * tutte' su un gesto che svuota il cestino: la scorciatoia esisterebbe solo per chi vede
     * il velo, e per gli altri sarebbe una trappola.
     */
    val shortcutLabel = if (bin && !picking) R.string.bin_empty else R.string.pick_all

    /**
     * ⚠️ Le bandierine locali esistono perché l'archivio risponde con un giro di ritardo:
     * scrivere in DataStore e aspettare che il flusso riemetta vuol dire un fotogramma o
     * due col velo ancora steso, e nel caso peggiore col menu che si apre **sotto** di
     * lui. Queste lo tolgono sull'istante; la scrittura serve alle sessioni dopo.
     */
    var pickOff by remember { mutableStateOf(false) }
    var binOff by remember { mutableStateOf(false) }

    /**
     * Quale dei due veli è steso adesso, o nessuno.
     *
     * ⚠️ La selezione viene **prima** apposta: nel cestino con una selezione in corso il
     * gesto utile è 'tutte', quindi è quello che va insegnato, e il velo del cestino ha già
     * avuto la sua occasione all'apertura.
     */
    val hint: Hint? = when {
        picking && !pickSeen && !pickOff -> Hint.PICK_ALL
        bin && !binSeen && !binOff -> Hint.BIN_EMPTY
        else -> null
    }

    /*
     * ⚠️⚠️ **IL MENU SI APRE DA SÉ QUANDO LA SELEZIONE COMINCIA, dalla 0.75** (richiesta
     * dell'utente, *per usabilità*): scelta la prima foto, l'azione è la cosa che si vuole
     * fare, e farla cercare dietro un tocco su un tastino piccolo in un angolo era un
     * passaggio a vuoto.
     * ⚠️⚠️ **MA NON SOPRA UN VELO DI ONBOARDING, e senza questa condizione i due si
     * pestano**: il velo del tocco lungo compare esattamente nello stesso istante, cioè al
     * primo ingresso in selezione, e un menu che si aprisse sotto di lui sarebbe un riquadro
     * dentro un velo. Aspettando che il velo sia archiviato, l'effetto gira di nuovo (`hint`
     * cambia) e il menu si apre appena il velo cade: la sequenza diventa insegna, chiudi,
     * ecco il menu.
     * ⚠️ **Non riapre il menu che l'utente ha chiuso a mano**: l'effetto dipende da `picking`
     * e da `hint`, non da `menuOpen`, quindi chiuderlo col tastino non lo fa tornare.
     * ⚠️ **Nel cestino non si apre da sé**: là `picking` è falso finché non si sceglie
     * qualcosa, e un menu che si aprisse entrando mostrerebbe 'Svuota il cestino' senza che
     * nessuno l'abbia chiesto.
     */
    LaunchedEffect(picking, hint) { if (picking && hint == null) menuOpen = true }

    /**
     * Il velo si archivia appena l'utente fa la cosa che insegnava, o appena la salta.
     *
     * ⚠️ Col ramo `null` che non fa niente, e non è ridondanza: questa funzione la chiama
     * anche il tastino **vero**, dove un velo non c'è, e senza quel ramo un tocco lungo
     * ordinario archivierebbe un promemoria mai mostrato.
     */
    val hintDone: () -> Unit = {
        when (hint) {
            Hint.PICK_ALL -> pickOff = true
            Hint.BIN_EMPTY -> binOff = true
            null -> Unit
        }
        hint?.let { seen -> scope.launch { seen.remember(context) } }
    }

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

    /*
     * ⚠️⚠️ **LA RADICE È UN `Box` E NON LA `Column`, e serve SOLO al velo** (difetto
     * segnalato dall'utente il 2026-08-31: *l'intero schermo deve offuscarsi, non solo un
     * riquadro interno*). Il velo dell'onboarding deve coprire **tutto**, testata e margini
     * di sistema compresi, e da dentro la `Column` non poteva: là il rientro di sistema e i
     * margini della schermata sono già stati applicati, quindi qualunque cosa nasca là
     * dentro comincia sotto la testata.
     * ⚠️ **Il `Box` non ha margini propri**, ed è quello che gli permette di arrivare fino
     * al bordo dello schermo: i margini restano sulla `Column`, cioè sul contenuto. Chi ne
     * spostasse uno sul `Box` rimetterebbe il difetto.
     */
    Box(modifier = modifier.fillMaxSize()) {

    Column(
        modifier = Modifier
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
            /*
             * ⚠️⚠️ **QUI NON C'È PIÙ NIENTE, e la ragione per cui c'era è stata SOSTITUITA
             * invece che dimenticata.** Fino alla `0.72` accanto al conto stava un tastino
             * 'Tutte', messo lì perché su una cartella da trecento foto il gesto
             * alternativo è trecento tocchi. Quel bisogno adesso lo copre il **tocco lungo
             * sul tastino galleggiante**, che fa la stessa cosa, si annuncia a TalkBack e ha
             * un onboarding che lo insegna una volta.
             * ⚠️ Togliendolo si guadagna la coerenza, che è la ragione dell'utente
             * (2026-08-31): *è un unicum e nessun'altra azione fa apparire qualcosa lì*.
             * In questa barra non compariva nient'altro, mai, in nessun altro modo.
             * ⚠️ Chi volesse rimetterlo tenga presente che ne servirebbe **anche** uno per
             * 'nessuna', o la barra torna a essere un posto dove una sola azione su due ha
             * un tastino.
             */
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
                            // ⚠️ Il verso si legge PRIMA di toccare la selezione, o la
                            // riga dopo lo avrebbe già falsato.
                            dragOff = items[hit] in chosen
                            // ⚠️⚠️ **Il colpetto forte lo dà solo l'INGRESSO nel modo
                            // selezione**, che è il passaggio da raccontare; dentro al
                            // modo ogni gesto ne dà uno leggero. Darne uno forte ogni
                            // volta vorrebbe dire annunciare un passaggio che non
                            // avviene, e in una selezione da cinquanta foto sarebbe un
                            // martello.
                            haptics.performHapticFeedback(
                                if (picking) HapticFeedbackType.TextHandleMove
                                else HapticFeedbackType.LongPress
                            )
                            chosen =
                                if (dragOff) chosen - items[hit] else chosen + items[hit]
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
                    // ⚠️ L'intervallo si SOMMA o si SOTTRAE alla selezione di partenza
                    // secondo il verso deciso da [dragOff]: nei due casi il conto resta
                    // 'quella di prima più (o meno) l'intervallo di adesso', quindi
                    // tornare indietro col dito disfa in tutti e due i versi.
                    val span = items.subList(minOf(from, hit), maxOf(from, hit) + 1)
                    chosen = if (dragOff) dragBase - span.toSet() else dragBase + span
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
                                    picking -> {
                                        haptics.performHapticFeedback(
                                            HapticFeedbackType.TextHandleMove
                                        )
                                        chosen = chosen.toggle(uri)
                                    }
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
                FabPop(
                    visible = picking || bin,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                ) {
                    Box {
                        PickFab(
                            container = MaterialTheme.colorScheme.primaryContainer,
                            ink = MaterialTheme.colorScheme.onPrimaryContainer,
                            lift = FAB_LIFT,
                            longLabel = stringResource(shortcutLabel),
                            // ⚠️⚠️ **ALTERNA e non apre, dalla 0.75** (richiesta
                            // dell'utente): il menu si apre da sé all'inizio della
                            // selezione, quindi un tocco che 'apre' non avrebbe niente da
                            // fare, mentre serve **chiuderlo** per vedere le foto sotto e
                            // continuare a scegliere. Un altro tocco lo riporta.
                            onOpen = { menuOpen = !menuOpen },
                            onAll = { shortcut(); hintDone() }
                        )
                        PickMenu(open = menuOpen, onDismiss = { menuOpen = false }) {
                            /*
                             * ⚠️ Nel cestino senza niente scelto il tastino non porta le sei
                             * operazioni, che non avrebbero su cosa agire, ma le tre voci che
                             * riguardano il cestino **intero**.
                             * ⚠️⚠️ **L'ORDINE NON È CASUALE**: prima quella che rimette a
                             * posto, poi quella che racconta, ultima quella che cancella per
                             * sempre. Chi tocca al buio la prima voce di un menu non deve
                             * poterci svuotare il cestino, e 'Ripristina tutto' come prima
                             * voce è la richiesta dell'utente.
                             * ⚠️ **Le due azioni si spengono sul cestino vuoto**, la
                             * cronologia no: quelle non avrebbero niente su cui agire e
                             * direbbero '0 fatti', mentre la cronologia ha senso proprio
                             * quando il cestino è vuoto perché si è ripristinato tutto.
                             */
                            if (!picking) {
                                Column(modifier = Modifier.padding(vertical = PICK_EDGE)) {
                                    DropdownMenuItem(
                                        enabled = filled,
                                        text = { Text(stringResource(R.string.bin_restore_all)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.SettingsBackupRestore, null)
                                        },
                                        // ⚠️ Nessuna conferma, come per il ripristino di una
                                        // foto sola: rimette le cose come stavano, ed è
                                        // reversibile (si rielimina). Vedi [FileJob.Restore].
                                        onClick = {
                                            menuOpen = false
                                            job = FileJob.Restore(items.orEmpty())
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.bin_history)) },
                                        leadingIcon = { Icon(Icons.Default.History, null) },
                                        onClick = { menuOpen = false; onHistory() }
                                    )
                                    DropdownMenuItem(
                                        enabled = filled,
                                        text = { Text(stringResource(R.string.bin_empty)) },
                                        leadingIcon = { Icon(Icons.Default.DeleteForever, null) },
                                        colors = MenuDefaults.itemColors(
                                            textColor = MaterialTheme.colorScheme.error,
                                            leadingIconColor = MaterialTheme.colorScheme.error
                                        ),
                                        onClick = { menuOpen = false; emptying = true }
                                    )
                                }
                            } else ActionPad(
                                actions = listOf(
                                    // ⚠️⚠️ **LE PRIME TRE ICONE SONO DISEGNATE IN CASA,
                                    // dalla 0.73** (vedi [Glyphs]), e le stesse valgono nel
                                    // visualizzatore: il riquadro è condiviso, e chi impara
                                    // dove sta 'sposta' lo impara una volta.
                                    // ⚠️ Copia e Sposta escono dallo **stesso stampo** e
                                    // differiscono solo per il tratteggio, che sta sulla
                                    // cartella di dietro: spostare vuol dire che l'originale
                                    // non resta dov'era. Prima erano `FolderCopy` e
                                    // `CopyAll` di Material, cioè due cartelle disegnate da
                                    // due mani diverse, e l'utente ha detto che la seconda
                                    // non andava bene.
                                    // ⚠️ **Sono provvisorie**: l'utente ha chiesto gli SVG
                                    // per ridisegnarle, e quando arrivano cambiano le
                                    // coordinate in [Glyphs] e nient'altro.
                                    PadAction(Glyphs.FolderPair, R.string.menu_copy_here) {
                                        menuOpen = false
                                        job = FileJob.Transfer(chosen.toList(), move = false)
                                    },
                                    PadAction(
                                        Glyphs.FolderPairDashed,
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
                                        PadAction(Glyphs.TextCursor, R.string.pick_rename) {
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
         * ⚠️⚠️ **IL MINI ONBOARDING DEL TOCCO LUNGO** (richiesta dell'utente: *un
         * mini onboarding grafico, che oscura la schermata ed evidenzia in arancione
         * il FAB*). Serve perché il tocco lungo è una scorciatoia che **non si
         * scopre da sola**: un tastino non dichiara i propri gesti.
         * ⚠️⚠️ **E dalla `0.73` è l'UNICA via a insegnarla**, perché il tastino 'Tutte' in
         * testata non c'è più (vedi la nota là dove stava): finché c'era, questo velo era
         * un aiuto e la barra la rete di sicurezza. Ora la rete è l'etichetta che TalkBack
         * legge sul tocco lungo, e chi tocca il velo per chiuderlo senza leggerlo la
         * scorciatoia non la scoprirà. È il costo della scelta, ed è dichiarato.
         * ⚠️⚠️ **I VELI SONO DUE perché le scorciatoie sono due** (vedi `shortcut`),
         * e ognuno ha il suo promemoria in archivio: quello della selezione compare
         * alla prima selezione, quello del cestino alla prima apertura del cestino.
         * Prima era uno solo, mostrato anche nel cestino, e prometteva di
         * selezionare 'tutte le immagini della cartella' a chi in una cartella non
         * era: il comportamento era giusto, la frase no.
         * ⚠️⚠️ **La copia arancione FUNZIONA, non è un disegno**, ed è la
         * differenza fra insegnare e raccontare: chi tiene premuto sul velo fa la
         * cosa mentre gliela si spiega, invece di doverla richiudere e rifare. È
         * anche il motivo per cui è la stessa [PickFab] del tastino vero, alla
         * stessa misura e nello stesso angolo: cade **sopra** l'originale.
         * ⚠️ **L'arancione è l'unico posto in cui la tavolozza si rompe apposta**:
         * l'accento dell'app è verde acqua, e un velo che evidenzia col colore di
         * casa non evidenzia niente. Misurati: il glifo scuro sull'arancione fa
         * 7.29, e l'arancione sul velo fa 4.35 contro la fotografia più chiara
         * possibile, cioè sopra il 3:1 delle grafiche non testuali anche nel caso
         * peggiore.
         * ⚠️⚠️ **IL VELO COPRE TUTTO LO SCHERMO dalla `0.73`**, testata e margini di
         * sistema compresi, ed è una correzione: fino alla `0.72` copriva la sola griglia,
         * perché nasceva dentro la `Column` che i margini li ha già applicati. Il rimedio
         * non è stato spostare i margini ma **avvolgere la schermata in un `Box`** e far
         * nascere il velo là (vedi la nota sulla radice): i margini restano dove servono,
         * cioè sul contenuto, e il velo nasce fuori da tutti. La nota vecchia diceva che
         * avvolgere la testata voleva dire spostare di rientro tutta la schermata: era
         * vera per la strada che aveva in mente, non per questa.
         */
        if (hint != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(HINT_SCRIM)
                    // ⚠️ Niente increspatura e nessuna descrizione: questo non è
                    // un tasto, è il velo, e un tocco qualunque lo archivia. Un
                    // onboarding che si deve leggere due volte non è un
                    // onboarding.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = hintDone
                    )
            ) {
                Column(
                    // ⚠️⚠️ **I RIENTRI DELLA `Column` SI RIFANNO QUI, e non sono
                    // decorazione**: la copia arancione deve cadere ESATTAMENTE
                    // sopra il tastino vero, e quello vive dentro il rientro di
                    // sistema più i margini della schermata più i suoi 8dp. Il velo
                    // invece nasce fuori da tutti e tre, perché è il suo mestiere.
                    // Chi ne toglie uno vede la copia scivolare in un angolo.
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .safeDrawingPadding()
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(HINT_GAP)
                ) {
                    Text(
                        text = stringResource(
                            when (hint) {
                                Hint.PICK_ALL -> R.string.pick_all_hint
                                Hint.BIN_EMPTY -> R.string.bin_empty_hint
                            }
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.End,
                        modifier = Modifier.widthIn(max = HINT_WIDTH)
                    )
                    PickFab(
                        container = HINT_MARK,
                        ink = HINT_INK,
                        // ⚠️ Nessuna ombra: sopra un velo non c'è niente da cui
                        // staccarsi, e un'ombra su fondo scuro è solo sporco.
                        lift = 0.dp,
                        longLabel = stringResource(shortcutLabel),
                        onOpen = { hintDone(); menuOpen = true },
                        onAll = { shortcut(); hintDone() }
                    )
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
                            color = tint.copy(alpha = MARK_ALPHA)
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
 * Quanto è OPACO il nastro dell'ultima foto vista.
 *
 * ⚠️ 85%, scelta dell'utente (2026-08-31). Il nastro è pieno del colore d'accento e sta
 * sopra la fotografia: a opacità piena la copre, e un segno che copre quello che segnala
 * lavora contro sé stesso. Un filo di trasparenza lascia intravedere l'angolo della foto e
 * dice 'questa' senza cancellarne un pezzo.
 * ⚠️ **Non è la stessa cosa di [PICKED_VEIL]**, che agisce sull'intera miniatura e la
 * schiarisce: questo è l'opacità di un singolo triangolo dipinto sopra.
 */
private const val MARK_ALPHA = 0.85f

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

/**
 * La comparsa del tastino della selezione.
 *
 * ⚠️⚠️ **IL TASTINO ENTRA CON UN'ANIMAZIONE dalla 0.67** (richiesta dell'utente: *voglio
 * che quel FAB appaia con un'animazione*). Prima compariva di scatto, e su un tastino che
 * segnala un **cambio di modo** è l'occasione sprecata: il movimento è la cosa che dice
 * 'adesso sei in selezione', e senza di lui il tastino sembra essere sempre stato lì.
 * ⚠️ **Cresce dal proprio centro con una molla appena elastica, ma esce secco**: una cosa
 * che arriva può permettersi di farsi notare, una che se ne va no, e un rimbalzo in uscita
 * trattiene lo sguardo su un angolo che si sta svuotando.
 * ⚠️⚠️ **Sta in una funzione a sé, e non è per eleganza**: chiamata sul posto,
 * `AnimatedVisibility` finisce sull'overload di `ColumnScope`, perché quel `Box` vive
 * dentro la `Column` della schermata, e il compilatore la rifiuta. Qui dentro di
 * `ColumnScope` non c'è traccia, quindi si risolve quella giusta. È anche il posto in cui
 * la specifica dell'animazione ha un nome invece di essere venti righe in mezzo al
 * riquadro.
 */
@Composable
private fun FabPop(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            initialScale = FAB_SMALL
        ) + fadeIn(animationSpec = tween(FAB_IN)),
        exit = scaleOut(
            animationSpec = tween(FAB_OUT),
            targetScale = FAB_SMALL
        ) + fadeOut(animationSpec = tween(FAB_OUT))
    ) {
        content()
    }
}

/**
 * Il tastino galleggiante della selezione, con i suoi **due** gesti.
 *
 * ⚠️⚠️ **NON È `SmallFloatingActionButton`, e non è un capriccio**: quel composabile
 * prende un `onClick` solo, e il `modifier` che gli si passa finisce **fuori** dal suo
 * `clickable`, cioè come genitore. Un `combinedClickable` messo là non vedrebbe mai il
 * tocco lungo, perché nella passata `Main` il figlio consuma il down per primo: è
 * esattamente il meccanismo che aveva rotto il tocco lungo sulla griglia. Per avere due
 * gesti su un tastino bisogna che di nodo che ascolta ce ne sia **uno**.
 * ⚠️ **La resa non cambia**: `SmallFloatingActionButton` è una `Surface` da 40dp con
 * `primaryContainer`, il suo contrasto e 6dp d'ombra, e questa è quella. L'unica cosa che
 * si perde è l'ombra che cresce al passaggio del **mouse**, che su un telefono non
 * succede: in Material 3 la pressione lascia l'ombra dov'è.
 * ⚠️ Il gesto sta **dentro** la `Surface` e non sul suo modificatore, così l'increspatura
 * prende il colore del contenuto (`ink`) invece di quello che c'era fuori.
 */
@Composable
private fun PickFab(
    container: Color,
    ink: Color,
    lift: Dp,
    longLabel: String,
    onOpen: () -> Unit,
    onAll: () -> Unit
) {
    Surface(
        modifier = Modifier.size(FAB_SIZE),
        shape = RoundedCornerShape(FAB_CORNER),
        color = container,
        contentColor = ink,
        shadowElevation = lift
    ) {
        Box(
            modifier = Modifier.combinedClickable(
                role = Role.Button,
                // ⚠️ Il tocco lungo si DICHIARA a TalkBack, o resta una scorciatoia che
                // esiste solo per chi vede il velo: l'etichetta la legge il lettore di
                // schermo fra le azioni disponibili sul tastino. ⚠️ Arriva da fuori perché
                // il gesto fa due cose diverse (vedi `shortcut`), e un'etichetta fissa ne
                // annuncerebbe una mentre succede l'altra.
                onLongClickLabel = longLabel,
                onLongClick = onAll,
                onClick = onOpen
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = stringResource(R.string.pick_actions)
            )
        }
    }
}

/**
 * Il menu delle operazioni: **al centro in basso**, sopra il tastino.
 *
 * ⚠️⚠️ **NON È PIÙ UN `DropdownMenu`, dalla 0.75** (richiesta dell'utente: *al centro in
 * basso, con angoli un po' più stondati*). Un `DropdownMenu` si posiziona **accanto al suo
 * genitore** e non accetta un posizionatore: attaccato a un tastino in basso a destra,
 * usciva da quell'angolo, che è il posto peggiore per un riquadro su un telefono tenuto in
 * una mano. La superficie e il posto stanno in [MenuShell] e [MenuAbove], condivisi col menu
 * del visualizzatore, che è un `Popup` per la stessa ragione dalla `0.69`.
 * ⚠️ **Non si chiude toccando fuori, e non è una dimenticanza**: qui è il tastino ad
 * alternarlo, e le due cose insieme si pestano. Il perché sta in [MenuShell], sul parametro.
 */
@Composable
private fun PickMenu(open: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    if (!open) return
    val gap = with(LocalDensity.current) { PICK_GAP.roundToPx() }
    MenuShell(
        position = remember(gap) { MenuAbove(gap) },
        corner = PICK_CORNER,
        dismissOnOutside = false,
        onDismiss = onDismiss,
        content = content
    )
}

/**
 * Quanto sono stondati gli angoli del menu, e quanto stacca dal bordo alto del tastino.
 *
 * ⚠️ 16dp, cioè *un po' più stondati* come chiesto: un `DropdownMenu` di Material ne ha 4,
 * e il salto a 16 si vede senza trasformare il riquadro in una bolla. Il menu del
 * visualizzatore ne ha 8 e resta com'è: quello è una lista larga e bassa, questo è quasi
 * quadrato, e su una forma quadrata lo stesso raggio si legge meno.
 */
private val PICK_CORNER = 16.dp
private val PICK_GAP = 12.dp

/**
 * Il margine sopra e sotto una voce sola.
 *
 * ⚠️ Serve **solo** al cestino: il riquadro delle sei azioni porta i suoi, una voce di menu
 * no, e un `DropdownMenu` di Material glielo metteva lui. Senza, il testo toccherebbe
 * l'angolo stondato.
 */
private val PICK_EDGE = 8.dp

/** La misura di `SmallFloatingActionButton`, che [PickFab] rifà a mano. */
private val FAB_SIZE = 40.dp

/** L'ombra di serie di un tastino galleggiante in Material 3. */
private val FAB_LIFT = 6.dp

/**
 * Da quanto piccolo entra il tastino, e a quanto piccolo torna uscendo.
 *
 * ⚠️ 0,62 e non 0: partendo da zero il tastino sembra **sbucare** da un punto, e con una
 * molla elastica diventa un rimbalzo da cartone animato. Partendo da due terzi il gesto si
 * legge come 'era lì e si è fatto avanti'.
 */
private const val FAB_SMALL = 0.62f

/** La dissolvenza in entrata: più corta della molla, così il colore c'è già mentre cresce. */
private const val FAB_IN = 90

/** L'uscita, in millisecondi: secca, e più breve dell'entrata. */
private const val FAB_OUT = 110

/**
 * Il velo del mini onboarding.
 *
 * ⚠️ **Il 70% di nero e non il 50%**: sotto c'è una griglia di fotografie, cioè il fondo
 * più chiassoso che ci sia, e a metà velo le miniature continuano a chiamare l'occhio. Col
 * 70% il bianco del testo misura 8.45 anche sulla fotografia più chiara possibile.
 */
private val HINT_SCRIM = Color(0xB3000000)

/**
 * L'arancione della copia evidenziata, e **l'unico posto in cui la tavolozza si rompe
 * apposta** (richiesta dell'utente).
 *
 * ⚠️ L'accento dell'app è verde acqua: un velo che evidenzia col colore di casa non
 * evidenzia niente, perché quel colore è già dappertutto. Misurato: 4.35 sul velo steso
 * sulla fotografia più chiara possibile, cioè sopra il 3:1 delle grafiche non testuali nel
 * caso peggiore, e 10.81 nel caso normale.
 */
private val HINT_MARK = Color(0xFFFFA726)

/** Il glifo sopra l'arancione: misurato 7.29, cioè leggibile senza discussioni. */
private val HINT_INK = Color(0xFF3E2600)

/** Quanto sta lontano il testo dal tastino che indica: abbastanza da non sembrarne parte. */
private val HINT_GAP = 14.dp

/**
 * Quanto è larga al massimo la frase del velo.
 *
 * ⚠️ Un limite serve perché la frase è lunga e le lingue non sono l'italiano: senza, in
 * tedesco diventerebbe una riga sola da bordo a bordo, e in un telefono stretto si
 * spezzerebbe dove capita invece che dove si legge.
 */
private val HINT_WIDTH = 260.dp

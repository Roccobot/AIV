package io.github.roccobot.aiv

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
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
     * Quante colonne di miniature, sul lato corto dello schermo: `Settings.folderColumns`.
     *
     * ⚠️⚠️ **LA STESSA VOCE CHE GOVERNA LA SCHERMATA INIZIALE, DALLA `1.66`, ED È SUA
     * RICHIESTA** (*perché è solo per la home? Lo voglio anche nelle cartelle: dev'essere
     * un'impostazione globale*). Fino alla `1.65` qui le colonne le decideva una misura fissa e
     * là il suo numero, quindi la voce diceva 'griglia delle cartelle' e mentiva a metà.
     * ⚠️ **La chiave e il valore di fabbrica non si toccano**: la voce è spostata di dominio, non
     * sostituita, e chi aggiorna non deve perdere la scelta che aveva fatto.
     */
    columns: Int = FOLDER_COLUMNS.first(),
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
     * Se il cestino è acceso. Vedi `Settings.binOn`.
     *
     * ⚠️ **Decide due cose insieme**: se 'elimina' sposta nel cestino o cancella, e se prima
     * compaia una conferma. Il perché siano la stessa cosa sta in [FileJob.Delete].
     * ⚠️ Il valore di serie tiene in piedi le anteprime e i richiami che non lo passano, ed è
     * quello di fabbrica dell'impostazione.
     */
    binOn: Boolean = true,
    /** Che cosa il filtro volatile lascia vedere. Vedi `ViewerViewModel.gridFilter`. */
    filter: MediaKind = MediaKind.ALL,
    onFilter: (MediaKind) -> Unit = {},
    /**
     * Apre la ricerca dei file per nome: il tocco lungo sul filtro.
     *
     * ⚠️ **È la stessa che apre la voce 'Cerca' della schermata iniziale**, quindi cerca in
     * tutta la galleria: chi la chiama passa lo stesso `openSearch` di là.
     *
     * ⚠️⚠️ **SENZA VALORE DI RISERVA, dalla 1.53, e la ragione è un difetto vero**: nella
     * `1.50` questo parametro era arrivato al solo ramo della ricerca, e i due rami che
     * contano (la cartella e il cestino) prendevano il `{}` di riserva. Il gesto vibrava e
     * chiamava una funzione che non fa niente, cioè si presentava come una funzione rotta
     * senza che niente lo segnalasse: né il compilatore, né una lettura del codice, dove un
     * parametro assente si legge come una scelta.
     * ⚠️ **Adesso quel difetto non si può più scrivere**: chi aggiunge una schermata che usa
     * questa griglia deve dire dove porta il tocco lungo, o non compila. È il rimedio giusto
     * per un difetto che si vedeva solo provando l'app, che è la cosa che qui non si può
     * fare.
     */
    onSearch: () -> Unit,
    /**
     *
     * ⚠️ Arriva come booleano e non come [Hand]: qui serve una sola domanda ('si rovescia
     * o no'), e passare l'enum vorrebbe dire che questa schermata conosce un tipo delle
     * impostazioni per leggerne un caso.
     */
    /** Se 'Copia lista' mette anche il percorso in testa. Vedi `Settings.listPath`. */
    listPath: Boolean = false,
    /** Se in testa alla selezione si legge il peso. Vedi `Settings.pickWeight`. */
    pickWeight: Boolean = true,
    /**
     * Se sotto ogni miniatura si legge il nome del file. Vedi `Settings.gridNames`.
     *
     * ⚠️ Il valore di serie è quello di fabbrica dell'impostazione, cioè **spento**: le
     * anteprime e i richiami che non lo passano mostrano la griglia com'è di solito.
     */
    gridNames: Boolean = false,
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
    onHistory: () -> Unit = {},
    /**
     * Avvisa che la griglia ha una selezione viva, cioè che una rilettura le farebbe danno.
     *
     * ⚠️⚠️ **SERVE ALL'AGGIORNAMENTO AUTOMATICO e a nient'altro**: quando arriva un file da
     * fuori il modello rilegge la cartella da sé, e una lista nuova azzera la selezione (è
     * `remember(items)`, poco più sotto). Senza questo avviso, trenta foto spuntate
     * sparirebbero perché qualcuno ha mandato una fotografia da un altro dispositivo. Il
     * perché la rilettura non si limiti ad aspettare sta su `ViewerViewModel.gridBusy`.
     * ⚠️ Il valore di serie non fa niente: le anteprime e i richiami che non lo passano non
     * hanno un modello dietro da avvisare.
     */
    onBusy: (Boolean) -> Unit = {}
) {
    val state = rememberLazyGridState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ⚠️ Le risorse si prendono da `LocalResources` e non da `context.resources`, e non è
    // pignoleria di lint: quest'ultimo non segue i cambi di configurazione, quindi dopo un
    // cambio di lingua o una rotazione servirebbe la versione vecchia. Si legge QUI,
    // mentre si compone, e si usa dentro le coroutine.
    val res = LocalResources.current

    /**
     * Gli INDIRIZZI scelti, non le posizioni.
     *
     * ⚠️⚠️ **Le posizioni sarebbero un difetto in attesa**: la lista si ricarica quando la
     * cartella cambia, e un indice che era la terza foto diventa la terza **di un'altra
     * lista** senza che niente lo dica. Un indirizzo o c'è ancora o non c'è, e nel secondo
     * caso sparisce dalla selezione da sé.
     * ⚠️ Vive nella SCHERMATA e non nel modello, perché se ne va con lei: uscire da una
     * cartella è il modo naturale di dire 'lascia stare'.
     */
    var chosen by remember(items) { mutableStateOf<Set<Uri>>(emptySet()) }
    val menu = rememberMenuState()

    /*
     * ⚠️⚠️ **IL PANNELLO NON HA PIÙ UN INTERRUTTORE PROPRIO, dalla 1.06** (riscontro
     * dell'utente sul collaudo: *selezione e bottomsheet devono sempre convivere e
     * apparire/sparire insieme*). Fino alla `1.05` c'era un `sheetOpen` separato da
     * `picking`, e le due cose potevano stare in tre stati invece che in due: il terzo era
     * una selezione **viva e invisibile**, senza il pannello che dice che cosa farci. Ci si
     * finiva col gesto Indietro, che chiudeva prima il pannello e lasciava le spunte accese.
     * ⚠️ Adesso il pannello si vede **esattamente** quando c'è una selezione, e non esiste
     * più nessuno stato da tenere d'accordo con lei. Chi volesse rimettere un modo di
     * nascondere il pannello tenendo la selezione rimetterebbe quel terzo stato.
     */

    /** Quanto è alto il pannello, in pixel, per lasciargli il posto sotto la griglia. */
    var sheetTall by remember { mutableIntStateOf(0) }

    /**
     * La selezione che il gesto Indietro ha appena azzerato, e `null` quando non c'è niente da
     * rimettere. È lei a far comparire la notifica con 'Annulla'.
     *
     * ⚠️⚠️ **DALLA 1.44 INDIETRO AZZERA SENZA CHIEDERE, e la conferma della 1.06 è uscita**
     * (istruzione dell'utente, 2026-09-03: *'indietro' di sistema dalla selezione la deve
     * cancellare e far sparire la bottomsheet, senza conferma. Ma allo stesso tempo deve
     * apparire per 3 secondi una notifica in basso*). Il difetto che la conferma copriva è lo
     * stesso (trenta tocchi persi per sbaglio), ma la cura è cambiata: **prima** si chiedeva e
     * si aspettava, **adesso** si fa e si offre di disfare. Un dialogo si paga sempre, una
     * notifica solo se serve.
     * ⚠️ **Tiene la selezione e non un booleano**: 'annulla' deve rimettere *quelle*
     * fotografie, e un flag saprebbe solo che qualcosa è stato azzerato.
     * ⚠️⚠️ **LA CHIAVE È IL TITOLO, ed è quello che la fa sparire cambiando cartella** (*o
     * finché non si cambia cartella*): il titolo è la sola cosa che cambia quando si esce da
     * questa cartella, ed è già la chiave che [worked] usa per la stessa ragione. Con `items`
     * si azzererebbe a ogni ricarica della lista, cioè anche restando qui.
     */
    var cleared by remember(title) { mutableStateOf<Set<Uri>?>(null) }

    /*
     * ⚠️ **Tre secondi, come li ha chiesti**, e il conto riparte da zero se si azzera una
     * seconda selezione: la chiave dell'effetto è [cleared], quindi un'altra pressione di
     * Indietro rimette la notifica in scena per tre secondi suoi.
     */
    LaunchedEffect(cleared) {
        if (cleared != null) {
            delay(UNDO_MS)
            cleared = null
        }
    }

    /**
     * Se in questa visita si è già eseguita un'operazione sui file.
     *
     * ⚠️⚠️ **LA CHIAVE È IL TITOLO E NON `items`, e con `items` NON FUNZIONEREBBE**: dopo
     * un'operazione la lista si ricarica, quindi una bandierina legata a lei si
     * riazzererebbe **proprio nell'istante** in cui serve leggerla, e la risalita non
     * scatterebbe mai. Il titolo cambia quando si cambia cartella, che è la sola cosa che
     * deve dimenticare l'operazione fatta.
     */
    var worked by remember(title) { mutableStateOf(false) }

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
    /** Se la conferma di 'Ripristina tutto' è in scena. Vedi la nota su quella voce. */
    var restoringAll by remember { mutableStateOf(false) }
    val picking = chosen.isNotEmpty()

    // ⚠️ In un effetto e non a filo della composizione: avvisare il modello è un cambiamento
    // di stato fuori da qui, e farlo mentre si disegna vorrebbe dire scrivere e leggere lo
    // stesso dato nello stesso giro. La chiave è il **se** e non l'insieme: aggiungere la
    // trentunesima foto alla selezione non è una notizia nuova.
    LaunchedEffect(picking) { onBusy(picking) }
    /*
     * ⚠️⚠️ **UNA SELEZIONE NUOVA SPENDE IL 'DISFA', ed è la ragione per cui questo effetto
     * esiste**: 'Annulla' rimette *quelle* immagini, quindi con una selezione nuova in corso
     * non saprebbe se sostituirla o sommarsi, e in tutti e due i casi porterebbe via qualcosa
     * che l'utente ha appena scelto. La notifica se ne va, e il gesto Indietro ricomincia il
     * giro da capo con la selezione di adesso.
     * ⚠️ **Sta in un effetto sulla chiave `picking` e non nei punti in cui una selezione
     * nasce**, che sono tre (il trascinamento dopo il tocco lungo, `takeAll` e l'inversione),
     * e il primo di loro scrive la selezione a ogni fotogramma: là sarebbe una riga da
     * ricordare in tre posti, qui è la regola scritta una volta.
     */
    LaunchedEffect(picking) { if (picking) cleared = null }
    // ⚠️ E uscendo dalla griglia la selezione se ne va con la schermata, quindi il modello va
    // liberato: senza resterebbe convinto che c'è una selezione viva, e non rileggerebbe mai
    // più da sé.
    DisposableEffect(Unit) { onDispose { onBusy(false) } }

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

    /*
     * ⚠️ Indietro esce dalla SELEZIONE prima di uscire dalla cartella: chi ha scelto
     * trenta foto e tocca Indietro per sbaglio non deve ritrovarsi due schermate indietro
     * con la selezione persa.
     * ⚠️⚠️ **AZZERA SUBITO E OFFRE DI DISFARE, dalla 1.44: la conferma della 1.06 è uscita.**
     * Il perché del cambio sta su [cleared]. ⚠️ **Il terzo giro di una stessa questione**, e
     * conviene conoscerli tutti e tre per non tornare al primo: dalla `0.94` alla `1.05`
     * Indietro chiudeva prima il **pannello** e solo al secondo tocco la selezione, che
     * lasciava una selezione viva senza pannello, cioè uno stato in cui non si capisce di
     * esserci dentro; dalla `1.06` chiedeva conferma con un dialogo; da adesso fa e offre di
     * disfare. Le tre coprono lo stesso sbaglio, e questa è la sola che non costa niente a chi
     * il gesto lo aveva fatto per davvero.
     */
    BackHandler(enabled = picking) {
        cleared = chosen
        chosen = emptySet()
    }

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

    // ⚠️ Quale vibrazione, e perché quella, sta su [HOLD_BUZZ]: qui c'era la nota, e la
    // scelta si è spostata là quando è diventata una sola per tutta l'app. Due copie della
    // stessa spiegazione divergono alla prima modifica.
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
        haptics.performHapticFeedback(HOLD_BUZZ)
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
            haptics.performHapticFeedback(HOLD_BUZZ)
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
    var binOff by remember { mutableStateOf(false) }

    /**
     * Quale dei due veli è steso adesso, o nessuno.
     *
     * ⚠️ La selezione viene **prima** apposta: nel cestino con una selezione in corso il
     * gesto utile è 'tutte', quindi è quello che va insegnato, e il velo del cestino ha già
     * avuto la sua occasione all'apertura.
     */
    val hint: Hint? = when {
        bin && !binSeen && !binOff -> Hint.BIN_EMPTY
        else -> null
    }

    /*
     * ⚠️⚠️ **IL PANNELLO C'È PERCHÉ C'È LA SELEZIONE, e non perché un effetto l'ha aperto**
     * (vedi la nota su `sheetOpen`, tolto nella `1.06`). Dalla `0.75` alla `1.05` qui stava
     * un `LaunchedEffect` che lo apriva da sé alla prima foto scelta, e serviva perché il
     * pannello aveva una vita propria: adesso non ce l'ha più, quindi non c'è niente da
     * aprire e la richiesta di allora (*scelta la prima foto, l'azione è la cosa che si
     * vuole fare*) è vera per costruzione.
     * ⚠️ **Sotto il velo dell'onboarding il pannello si vede, ed è innocuo**: quel velo
     * copre lo schermo intero (`1.03`), quindi quello che gli sta sotto non si vede
     * comunque. Prima l'effetto doveva aspettarlo perché un menu **a comparsa** sarebbe
     * spuntato *sopra* il velo, che è un'altra cosa.
     */

    /**
     * Il velo si archivia appena l'utente fa la cosa che insegnava, o appena la salta.
     *
     * ⚠️ Col ramo `null` che non fa niente, e non è ridondanza: questa funzione la chiama
     * anche il tastino **vero**, dove un velo non c'è, e senza quel ramo un tocco lungo
     * ordinario archivierebbe un promemoria mai mostrato.
     */
    val hintDone: () -> Unit = {
        when (hint) {
            Hint.BIN_EMPTY -> binOff = true
            // ⚠️ Le colonne non si insegnano qui, ma il ramo c'è perché l'enum le porta:
            // il velo che le riguarda vive nella schermata delle cartelle. Dalla 1.25 vale
            // lo stesso per il doppio tocco, che vive nel visualizzatore.
            // ⚠️ Dalla 1.36 c'è anche l'avviso sul cambio di estensione, che vive dentro
            // la finestra di rinomina: stessa storia, ramo obbligato dall'enum.
            Hint.COLUMNS, Hint.ZOOM_TAP, Hint.EXT_WARN -> Unit
            null -> Unit
        }
        hint?.let { seen -> scope.launch { seen.remember(context) } }
    }

    /**
     * Le dieci operazioni sulla selezione, nell'ordine in cui l'utente le ha chieste.
     *
     * ⚠️⚠️ **DIECI E NON SEI, dalla 0.94** (richiesta dell'utente, con le sue etichette
     * brevi): alle sei di prima si aggiungono 'Lista', 'Inverti', 'Tutti' e 'Nessuno'. Le
     * ultime tre non toccano nessun file, e stare accanto a quelle che li toccano è
     * deliberato: sono tutte cose che si fanno **sulla selezione**, e cercarle in due posti
     * diversi era il passaggio a vuoto che questa versione toglie.
     * ⚠️⚠️ **LA FILA È SBILANCIATA A DESTRA APPOSTA** (sue parole: *in modo che le funzioni
     * usate più di frequente siano comodamente raggiungibili con il pollice*), quindi
     * l'ordine non si 'sistema': 'Copia' in fondo alla prima fila e 'Nessuno' in fondo alla
     * seconda sono la posizione più comoda, non l'ultimo posto rimasto.
     * ⚠️⚠️ **NESSUNA VOCE CHIUDE PIÙ IL PANNELLO, dalla 1.06**: fino alla `1.05` le
     * operazioni sui file lo chiudevano prima di aprire il proprio dialogo, ed era corretto
     * finché il pannello era una cosa a sé. Adesso il pannello **è** la selezione (vedi la
     * nota su `sheetOpen`), quindi chiuderlo vorrebbe dire scioglierla: il dialogo di
     * un'operazione gli si disegna sopra, e alla fine dell'operazione la selezione si svuota
     * da sé (`perform`) portandosi via il pannello.
     */
    val pickActions = listOf(
        // ⚠️ Nel cestino al posto della rinomina c'è il ripristino: un file là dentro non si
        // rinomina (richiesta dell'utente), e il posto nel pannello è lo stesso, così le
        // dieci icone non ballano.
        if (bin) {
            PadAction(PadKey.RENAME, Glyphs.BinRestore, R.string.bin_restore) {
                job = FileJob.Restore(chosen.toList())
            }
        } else {
            PadAction(PadKey.RENAME, Glyphs.TextCursor, R.string.pick_rename) {
                job = FileJob.Rename(chosen.toList())
            }
        },
        PadAction(PadKey.INFO, Icons.Outlined.Info, R.string.pick_info) {
            job = FileJob.Facts(chosen.toList())
        },
        PadAction(PadKey.MOVE, Glyphs.FolderPairDashed, R.string.pick_move) {
            job = FileJob.Transfer(chosen.toList(), move = true)
        },
        // ⚠️⚠️ **IL TOCCO LUNGO SU 'COPIA' DUPLICA DOVE SEI, dalla 0.79** (richiesta
        // dell'utente): copiare chiede dove, duplicare no.
        // ⚠️ **Nel cestino no**: un duplicato là dentro nascerebbe senza riga d'archivio,
        // quindi non si potrebbe ripristinare, e sarebbe un file che il cestino non sa da
        // dove viene.
        PadAction(
            key = PadKey.COPY,
            icon = Glyphs.FolderPair,
            label = R.string.menu_copy_here,
            onHold = if (bin) null else {
                {
                    job = FileJob.Duplicate(chosen.toList())
                }
            },
            holdLabel = if (bin) null else R.string.pick_duplicate
        ) {
            job = FileJob.Transfer(chosen.toList(), move = false)
        },
        PadAction(PadKey.SHARE, Icons.Default.Share, R.string.menu_share) {
            // ⚠️ La lista si prende ADESSO: la condivisione gira in una coroutine, e
            // leggere `chosen` da dentro leggerebbe una selezione che nel frattempo può
            // essere cambiata.
            val list = chosen.toList()
            scope.launch { ImageActions.shareMany(context, list) }
        },
        /*
         * ⚠️⚠️ **IL TOCCO LUNGO SALTA IL CESTINO** (richiesta dell'utente, 2026-08-31), ed
         * è la sola scorciatoia irreversibile dell'app: per questo la conferma resta. Con
         * il cestino spento il tocco breve fa già la stessa cosa, quindi là la scorciatoia
         * non aggiunge niente e non si mette.
         */
        PadAction(
            key = PadKey.DELETE,
            icon = Glyphs.PickDelete,
            label = R.string.pick_delete,
            danger = true,
            onHold = if (bin || !binOn) null else {
                {
                    job = FileJob.Delete(chosen.toList(), forGood = true)
                }
            },
            holdLabel = if (bin || !binOn) null else R.string.pick_forever
        ) {
            // ⚠️ Definitiva nel cestino **o** col cestino spento: con `forGood` viaggia la
            // conferma.
            job = FileJob.Delete(chosen.toList(), forGood = bin || !binOn)
        },
        /*
         * ⚠️⚠️ **'Lista' LO DICE, dalla 1.06, e la nota di prima diceva il contrario**
         * (riscontro dell'utente sul collaudo: *serve una notifica toast 'Lista file copiata
         * negli appunti'*). Il ragionamento vecchio era che su Android 13 e oltre è il
         * **sistema** ad annunciare ogni copia negli appunti, quindi un nostro avviso
         * sarebbe la stessa cosa detta due volte: l'errore era prendere quella conferma di
         * sistema per una risposta alla domanda che si fa qui. Quella dice 'qualcosa è
         * finito negli appunti' e mostra l'inizio del testo; qui la domanda è **quanti nomi
         * sono partiti**, e la risposta non c'è in nessuno dei due posti.
         * ⚠️ **La frase è quella dettata dall'utente**, senza il conto dei nomi che sarebbe
         * stato facile aggiungere: quello che serve sapere è che la lista è partita, e un
         * numero in più su un avviso che dura due secondi è una cosa da leggere invece che
         * da vedere.
         */
        PadAction(PadKey.LIST, Icons.AutoMirrored.Outlined.FormatListBulleted, R.string.pick_list) {
            val list = chosen.toList()
            scope.launch {
                // ⚠️ Il percorso si chiede per UNA sola, non per tutte: gli elementi scelti
                // stanno nella stessa cartella (la griglia è una cartella), quindi una
                // interrogazione basta e le altre sarebbero la stessa risposta N volte.
                val head = if (listPath) factsOf(context, list.take(1)).one?.folder else null
                ImageActions.copyNames(context, list, head)
                Toast.makeText(
                    context,
                    res.getString(R.string.pick_list_done),
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        // ⚠️ Il tocco lungo su 'Tutti' fa il contrario, come chiesto: le due stanno
        // accanto, e chi sbaglia mira ha la correzione sotto lo stesso dito.
        PadAction(
            key = PadKey.ALL,
            icon = Glyphs.PickAll,
            label = R.string.pick_all_short,
            onHold = { chosen = emptySet() },
            holdLabel = R.string.pick_none
        ) {
            takeAll()
        },
        PadAction(PadKey.NONE, Glyphs.PickNone, R.string.pick_none) {
            chosen = emptySet()
        }
    ,
        // ⚠️ L'inversione lavora sull'elenco che si ha DAVANTI, non su tutta la cartella:
        // con una ricerca in corso o un filtro acceso, `items` è già quello filtrato, ed è
        // l'unica lettura che non sorprende.
        PadAction(PadKey.INVERT, Glyphs.PickInvert, R.string.pick_invert) {
            chosen = items.orEmpty().toSet() - chosen
        })
        // ⚠️ L'ordine è quello scelto dall'utente, e di fabbrica quello dettato per la
        // `1.54`: vedi `PICK_KEYS`. È un ordine SUO e non quello del menu del tocco lungo,
        // perché i due riquadri portano insiemi diversi in ordini diversi.
        .inOrder(LocalPadLook.current.pick)

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

    /**
     * Quanto è larga una cella, in pixel, e zero finché la griglia non ha misurato.
     *
     * ⚠️⚠️ **SI CHIEDE ALLA GRIGLIA, non si ricalcola**: le colonne le decide
     * `GridCells.Adaptive` a partire da [THUMB], e rifare quel conto qui vorrebbe dire una
     * seconda formula da tenere d'accordo con Compose, che sbaglierebbe in silenzio il
     * giorno che l'arrotondamento cambia. Qui il numero è quello **misurato**.
     * ⚠️ **`derivedStateOf` e non una lettura nuda**: `layoutInfo` cambia a ogni fotogramma
     * di scorrimento, la larghezza di una cella no, e senza il filtro ogni miniatura si
     * ricomporrebbe a ogni pixel scorso.
     * ⚠️ Serve **solo** al nome sotto la miniatura, che va accorciato a misura: senza quel
     * numero `fitName` non saprebbe rispetto a cosa accorciare.
     */
    val cellPx by remember(state) {
        derivedStateOf { state.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.width ?: 0 }
    }

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
            // ⚠️ **Il cestino tace, e chi decide è [FileKind.speaks]**: la sua notifica
            // dice la stessa cosa e in più offre di disfare, e due messaggi in fondo
            // allo schermo si coprirebbero a vicenda.
            if (kind.speaks(out)) {
                Toast.makeText(context, outcomeText(res, out, kind.done), Toast.LENGTH_LONG)
                    .show()
            }
            worked = true
            onChanged()
        }
    }

    /*
     * ⚠️⚠️ **SE LA CARTELLA SI SVUOTA OPERANDO, SI RISALE** (richiesta dell'utente,
     * 2026-08-31: *se sposto in una nuova cartella TUTTE le immagini di una cartella, la
     * mia vista si deve ri-spostare sul livello superiore*): restare in una cartella vuota
     * appena svuotata da noi è una schermata che non ha più niente da dire, e il tasto
     * Indietro sarebbe l'unica cosa da fare.
     * ⚠️ **Solo dopo un'operazione NOSTRA**, e la bandierina esiste per questo: una cartella
     * che era già vuota all'ingresso si apre e si guarda (ci si può arrivare da un
     * collegamento o da una ricerca), e buttare fuori chi ci entra sarebbe una schermata
     * che si rifiuta di esistere.
     * ⚠️ **Il cestino NO**, ed è la sua natura: svuotarlo è la cosa che si va a fare là
     * dentro, e ritrovarsi fuori dopo averlo fatto vorrebbe dire non vedere mai il
     * risultato. Là il vuoto ha già la sua frase ('Il cestino è vuoto').
     * ⚠️ **La ricerca nemmeno**: là il vuoto vuol dire 'nessun risultato', che è una
     * risposta e non una cartella finita.
     */
    LaunchedEffect(worked, items) {
        if (worked && !bin && query == null && items?.isEmpty() == true) onBack()
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
            // ⚠️ Il peso sta FUORI dalla colonna del conto, non sotto: la richiesta dice
            // *in linea ma a destra, allineato al bordo destro*, e dentro la colonna
            // seguirebbe la larghezza del testo invece del bordo della barra.
            // ⚠️ **Spento il peso, in selezione qui non va NIENTE**, e non il filtro: quello
            // sceglie che cosa mostrare nella cartella, e in mezzo a una selezione
            // cambierebbe l'elenco sotto le spunte già date.
            when {
                picking && pickWeight -> PickWeight(chosen)
                picking -> Unit
                else -> FilterKey(filter, onFilter, onSearch)
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

        /*
         * ⚠️⚠️ **IL RIQUADRO AVVOLGE TUTTI E TRE I CASI, dalla 1.06, e non il solo elenco
         * pieno** (riscontro dell'utente sul collaudo: *il FAB deve apparire anche a cestino
         * vuoto, altrimenti è irraggiungibile*). Fino alla `1.05` il tastino nasceva dentro
         * il ramo dell'elenco pieno, quindi in un cestino vuoto non esisteva: e siccome la
         * **Cronologia** vive nel suo menu, un cestino appena svuotato si portava via l'unica
         * via per sapere che cosa c'era dentro. Il ramo che lo nascondeva era proprio quello
         * in cui serve di più.
         * ⚠️ Il `weight` serve: senza, con tre sole fotografie il riquadro sarebbe alto
         * quanto loro e il tastino finirebbe a mezza schermata invece che in basso.
         */
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        when {
            items == null -> CircularProgressIndicator(
                // ⚠️ `TopCenter` e non `CenterHorizontally`: qui il genitore è un `Box`, e
                // l'allineamento di colonna non esiste più.
                Modifier.padding(top = 24.dp).size(28.dp).align(Alignment.TopCenter)
            )

            /*
             * ⚠️⚠️ **CINQUE VUOTI DIVERSI, CINQUE FRASI, e dirli con la stessa sarebbe un
             * piccolo inganno**: un cestino vuoto è una buona notizia, una cartella vuota
             * dice che non c'è niente, un **filtro** senza esito dice che manca **quel
             * genere** e non che la cartella è vuota, e una ricerca senza esito dice che
             * nessun nome combacia. A ricerca ancora da scrivere non significa niente, e
             * allora non si dice nulla.
             * ⚠️⚠️ **IL FILTRO SI GUARDA PRIMA DELLA CARTELLA, dalla 1.09** (riscontro
             * dell'utente): con 'solo foto' acceso in una cartella di soli filmati, dire 'La
             * cartella è vuota' è **falso**, perché là dentro ci sono dei file. La frase deve dire
             * che cosa manca, non lamentare un vuoto che non c'è.
             * ⚠️ **La ricerca vince sul filtro**: se si sta cercando, quello che si vuole
             * sapere è se il nome combacia, e il filtro è una condizione in più che l'utente
             * ha in testa.
             */
            items.isEmpty() -> {
                val nulla = when {
                    bin -> stringResource(R.string.bin_none)
                    query != null && query.isNotBlank() ->
                        stringResource(R.string.search_none, query)
                    query != null -> null
                    filter == MediaKind.IMAGES -> stringResource(R.string.folder_no_images)
                    filter == MediaKind.VIDEOS -> stringResource(R.string.folder_no_videos)
                    else -> stringResource(R.string.folder_empty)
                }
                // ⚠️ **Al centro dello spazio vuoto** (richiesta dell'utente), come nella
                // vista ad albero dalla `1.04`: una frase appesa in alto a sinistra sembra
                // l'inizio di un elenco che non arriva mai.
                if (nulla != null) {
                    Text(
                        text = nulla,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp)
                            .padding(bottom = BELOW_FAB)
                    )
                }
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
             * trascinamento si interromperebbe alla prima foto aggiunta, cioè subito. La selezione
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
                            // ⚠️ **Il colpetto è UNO SOLO dalla 1.21**, e qui stava il
                            // condizionale che dava quello forte all'ingresso nel modo
                            // selezione e quello leggero ai gesti dentro. Adesso sono la
                            // stessa cosa perché la vibrazione l'utente la vuole discreta
                            // dappertutto: il perché sta su [HOLD_BUZZ].
                            haptics.performHapticFeedback(HOLD_BUZZ)
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

            LazyVerticalGrid(
                    columns = GridCells.Fixed(spread(columns, LocalWindowInfo.current)),
                    state = state,
                    horizontalArrangement = Arrangement.spacedBy(GAP),
                    verticalArrangement = Arrangement.spacedBy(GAP),
                    // ⚠️ Il fondo cresce **con la selezione**, cioè quando il tastino
                    // compare: senza, la fotografia in basso a destra resterebbe coperta
                    // proprio mentre la si deve poter toccare. Fuori dalla selezione il
                    // tastino non c'è e quello spazio sarebbe un buco.
                    /*
                     * ⚠️⚠️ **SOTTO LA GRIGLIA CI VA IL PANNELLO MISURATO, dalla 0.94**: prima
                     * bastava lo spazio del tastino, che è alto quanto un dito; il pannello
                     * è due file di icone, e con [BELOW_FAB] l'ultima riga di fotografie
                     * sarebbe rimasta sotto di lui senza modo di tirarla fuori.
                     * ⚠️ Fuori dalla selezione il pannello non c'è, e resta [BELOW_FAB] per
                     * il solo tastino del cestino.
                     */
                    contentPadding = PaddingValues(
                        bottom = if (picking) {
                            with(LocalDensity.current) { sheetTall.toDp() }
                        } else if (bin) BELOW_FAB else 16.dp
                    ),
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
                            named = gridNames,
                            room = cellPx,
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
                                        haptics.performHapticFeedback(HOLD_BUZZ)
                                        chosen = chosen.toggle(uri)
                                    }
                                    else -> onOpen(index)
                                }
                            }
                        )
                    }
                }
            }
        }

            /*
             * ⚠️⚠️ **IL TASTINO RESTA SOLO NEL CESTINO SENZA SELEZIONE, dalla 0.94.**
             * Con una selezione in corso le operazioni stanno nella bottomsheet qui
             * sotto, e il tastino è sparito perché non aveva più niente da fare (vedi
             * [PickSheet]). Qui invece porta le tre voci che riguardano il cestino
             * **intero**, che non sono operazioni su una selezione e non hanno un altro
             * posto dove stare.
             */
            /*
             * ⚠️⚠️ **E DALLA 1.44 SI FA DA PARTE ANCHE PER LA NOTIFICA**: il gesto Indietro
             * azzera la selezione, quindi in quell'istante `picking` diventa falso e il
             * tastino tornerebbe **proprio dove** compare la notifica, che è larga tutto lo
             * schermo. Coprirebbe il tasto 'Annulla', cioè la sola cosa che quella notifica
             * ha da offrire.
             * ⚠️ **Riguarda il solo cestino**, come tutto questo tastino: in una cartella
             * normale non c'è e la notifica ha il fondo tutto per sé.
             */
            FabPop(
                visible = bin && !picking && cleared == null,
                // ⚠️ Il lato è quello scelto nelle impostazioni: vedi `PadLook.hand`.
                modifier = Modifier.align(fabSide()).padding(8.dp)
            ) {
                Box {
                    /*
                     * ⚠️⚠️ **IL MENU È SCRITTO PRIMA DEL TASTINO, e quest'ordine è la
                     * funzione** (1.39): il tastino si stacca in una finestra sua per restare
                     * sopra il velo (vedi `lifted` in [TapHoldFab]), e fra finestre dello
                     * stesso tipo comanda l'ordine in cui sono state aggiunte, che è quello
                     * della composizione. Scritto dopo, il menu coprirebbe il tastino invece
                     * del contrario.
                     * ⚠️ **Il menu non si sposta di un pixel**: il posizionatore legge il
                     * bordo di sopra di questo riquadro, che è lo stesso qualunque sia
                     * l'ordine dei figli.
                     */
                    PickMenu(menu = menu) {
                        /*
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
                        /*
                         * ⚠️⚠️ **TRE `MenuRow` E NON PIÙ TRE `DropdownMenuItem`, dalla
                         * `1.46`**: erano l'ultima fila di voci scritta con un componente
                         * diverso da quello degli altri menu, e il prezzo del cambio è
                         * dichiarato: il rientro di sinistra passa da 12 a 15dp, cioè le tre
                         * voci si spostano di tre punti a destra. Quei tre punti esistono per
                         * il glifo che sporge nel menu del visualizzatore, e portarli qui è
                         * esattamente allineare i due menu fra loro.
                         * ⚠️ **Il margine sopra e sotto lo mette la superficie**, quindi
                         * `PICK_EDGE` non c'è più: era il terzo posto in cui viveva lo stesso
                         * otto.
                         */
                        /*
                         * ⚠️⚠️ **LA CRONOLOGIA STA IN CIMA, dalla 1.53, per sua richiesta**
                         * (riscontro del giro della `1.51`, voce `icone-cestino`: *cambia
                         * l'ordine delle voci portando 'Cronologia' in cima*). Le altre due
                         * agiscono su tutto il contenuto, questa lo racconta: chi apre questo
                         * menu senza sapere che cosa c'è dentro incontra prima la voce che
                         * glielo dice, e le due che muovono i file dopo.
                         * ⚠️ **È anche la sola sempre toccabile**: le altre due si spengono a
                         * cestino vuoto, quindi in cima ci sarebbero due righe grigie e la sola
                         * viva in fondo.
                         */
                        MenuRow(
                            text = stringResource(R.string.bin_history),
                            /*
                             * ⚠️⚠️ **UN DISEGNO SUO, dalla `1.55`, e la sua terza scelta su
                             * questa riga.** Nella `1.51` aveva preso il simbolo del riciclo
                             * fra due proposte, perché `Icons.Default.History` diceva 'il
                             * tempo' mentre qui conta quello che è **passato di qui**; poi lo
                             * ha guardato in mano e lo ha ridisegnato (*pensavo che fosse un
                             * miglioramento, ma non mi piaceva*). Il suo mette insieme le due
                             * cose: il cassone e la freccia che torna indietro.
                             */
                            icon = Glyphs.BinHistory,
                            onTap = { menu.close(); onHistory() }
                        )
                        MenuRow(
                            text = stringResource(R.string.bin_restore_all),
                            // ⚠️ **Lo stesso glifo del ripristino singolo, dalla `1.56`**, per
                            // sua istruzione: il perché sta su [Glyphs.BinRestore].
                            icon = Glyphs.BinRestore,
                            enabled = filled,
                            /*
                             * ⚠️⚠️ **ADESSO CHIEDE, dalla 1.53, e la nota di prima diceva il
                             * contrario** (richiesta dell'utente, giro della `1.51`:
                             * *'Ripristina tutto' deve funzionare previa conferma*). Quella
                             * nota diceva che il ripristino non chiede perché è reversibile,
                             * e l'argomento resta vero per **una** immagine: rimette una cosa
                             * dov'era, e la si rielimina con un tocco. Su **tutto** il cestino
                             * no, e la differenza non è la reversibilità ma il **sapere dove
                             * vanno**: i file tornano ognuno nella sua cartella d'origine, che
                             * possono essere molte e non tutte in mente, quindi disfare a mano
                             * vorrebbe dire ritrovarli uno per uno.
                             * ⚠️ **Per questo il testo della conferma nomina la Cronologia**,
                             * che è il posto in cui quelle destinazioni sono scritte: le parole
                             * sono sue.
                             */
                            onTap = { menu.close(); restoringAll = true }
                        )
                        MenuRow(
                            text = stringResource(R.string.bin_empty),
                            icon = Icons.Default.DeleteForever,
                            enabled = filled,
                            danger = true,
                            onTap = { menu.close(); emptying = true }
                        )
                    }
                    TapHoldFab(
                        label = stringResource(R.string.pick_actions),
                        // ⚠️ I colori dell'icona dell'app, dalla `1.36`, come il tastino della
                        // schermata iniziale: il perché per esteso sta là, e i due tastini sono
                        // lo stesso oggetto in due schermate.
                        container = colorResource(R.color.launcher_background),
                        ink = colorResource(R.color.launcher_foreground),
                        holdLabel = stringResource(shortcutLabel),
                        // ⚠️ **`visible` e non `wanted`**: il FAB deve restare staccato per tutta
                        // l'uscita, o rientrerebbe nella finestra dell'app sotto il velo che se ne
                        // sta andando. ⚠️ Dalla `1.67` `visible` copre anche quello: era `veiling`
                        // finché la patina durava più del pannello.
                        lifted = menu.visible,
                        // ⚠️ **`wanted` e non `visible`**: il perché sta sul parametro
                        // `pressed` di [TapHoldFab], ed è il riscontro del giro della `1.59`.
                        pressed = menu.wanted,
                        // ⚠️ **Apre e basta, dalla 1.06**: a menu aperto il tocco non
                        // arriva più qui, perché lo mangia il velo trasparente (vedi
                        // `menuOpen` in fondo alla schermata). Un'alternanza qui
                        // riaprirebbe il menu che quel velo ha appena chiuso.
                        // ⚠️ **E dalla 1.39 quel velo lo raggiunge ancora**, benché il
                        // tastino stia in una finestra più alta: quella finestra è
                        // trasparente al tocco apposta (vedi `untouchable` in `ActionPad`).
                        onTap = { menu.open() },
                        onHold = { shortcut(); hintDone() },
                        /*
                         * ⚠️⚠️ **I TRE PUNTINI ARRIVANO DALLA SCHERMATA INIZIALE, dalla `1.55`**
                         * (richiesta dell'utente, giro della `1.54`: *i tre puntini, renderizzati
                         * in modo identico, vanno a finire sul FAB del cestino, dove c'era
                         * un'icona ancora più generica*). Là hanno lasciato il posto al marchio
                         * dell'app, e qui prendono il posto del disco singolo della `1.37`.
                         * ⚠️ **'Renderizzati in modo identico' è alla lettera**: stesso glifo di
                         * Material e stessa misura, senza scale né ritocchi, o sarebbero due
                         * disegni che si somigliano invece dello stesso disegno.
                         */
                        glyph = { Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = it) }
                    )
                }
            }

        }
    }

        /*
         * ⚠️⚠️ **A SINISTRA SI ROVESCIANO LE FILE, NON L'ELENCO**: girando la lista
         * intera, 'Copia' finirebbe nella seconda fila e 'Lista' nella prima, cioè
         * cambierebbe il raggruppamento invece della mano. Rovesciando ogni fila per
         * conto suo, le stesse cinque restano insieme e cambia solo da che parte
         * cominciano.
         */
        /*
         * ⚠️⚠️ **STA NEL `Box` DI RADICE DALLA 1.40, e prima viveva dentro la colonna**
         * (richiesta dell'utente, 2026-09-03: *fa' in modo che la barra multi-attività in
         * basso assuma lo stesso colore dello sfondo*). Dentro la colonna il rientro di
         * sistema è già applicato e **consumato**, quindi la scheda si fermava sopra la barra
         * e là sotto restava la pagina, di un altro colore. Qui arriva al bordo dello schermo
         * e il rientro se lo mette da sé, sul contenuto (vedi [PickSheet]).
         * ⚠️ **Misurato sullo screenshot**: la striscia della barra era
         * `252,251,247` contro i `242,241,237` della scheda.
         * ⚠️ **Sta PRIMA del velo del menu e di quello dell'onboarding**, come stava prima:
         * l'ordine dei figli di un `Box` è l'ordine di sovrapposizione, e i due veli devono
         * restare sopra di lei.
         */
        PickSheet(
            visible = picking,
            /*
             * ⚠️⚠️ **LA SPECCHIATURA È USCITA DEL TUTTO NELLA `1.57`** (decisione dell'utente,
             * giro della `1.55`: *la specchiatura se ne va del tutto, l'altra funzionalità la
             * sostituirà*). Rovesciava le due file per la mano sinistra, e adesso quel
             * mestiere lo fanno meglio due cose insieme: l'**ordine** che si trascina, che
             * mette ogni tasto dove uno lo vuole, e il **lato del tastino**, che sposta tutto
             * il resto. La chiave che diceva la mano adesso dice il lato, quindi chi aveva
             * scelto la sinistra non perde niente.
             */
            actions = pickActions,
            onHeight = { sheetTall = it }
        )

        /*
         * ⚠️⚠️ **LA NOTIFICA DELL'AZZERAMENTO, dalla 1.44**, che è la seconda metà della
         * richiesta con cui la conferma è uscita (istruzione dell'utente, 2026-09-03: *deve
         * apparire per 3 secondi (o finché non si cambia cartella) una notifica in basso che
         * a sinistra dice 'Selezione azzerata' e a destra un pulsante 'Annulla' che la
         * ripristina e fa riapparire la bottomsheet*).
         * ⚠️⚠️ **STA DOPO LA SCHEDA E PRIMA DEI DUE VELI, e l'ordine è la funzione**: in un
         * `Box` chi è scritto dopo sta sopra, e queste due non si vedono mai insieme (una
         * selezione nuova spegne la notifica, vedi l'effetto su `picking`), quindi fra loro
         * l'ordine non conta; conta invece che i veli restino sopra tutte e due, o un tocco
         * fuori dal menu del tastino finirebbe sul tasto 'Annulla'.
         * ⚠️ **Non serve dire alla griglia che c'è**: [sheetTall] esiste perché la scheda
         * delle azioni copre l'ultima fila di immagini per tutto il tempo della selezione,
         * mentre questa passa in tre secondi e non porta niente da raggiungere sotto di lei.
         */
        UndoNotice(
            visible = cleared != null,
            text = stringResource(R.string.pick_cleared),
            action = stringResource(R.string.pick_undo),
            modifier = Modifier.align(Alignment.BottomCenter),
            // ⚠️ `orEmpty()` e non `!!`: fra il tocco e questa riga il conto dei tre secondi
            // può essere scaduto, e con la notifica già in uscita il tasto non deve far
            // cadere l'app. Rimettere una selezione vuota è quello che c'è adesso.
            onUndo = {
                chosen = cleared.orEmpty()
                cleared = null
            }
        )

        /*
         * ⚠️⚠️ **IL VELO CHE CHIUDEVA IL MENU DEL TASTINO STAVA QUI FINO ALLA `1.69`, E ADESSO
         * VIVE IN `AivTheme`** (vedi `MenuGuard` in `Menus.kt`). Il fatto che lo aveva fatto
         * nascere nella `1.06` non è cambiato ed è questo: da Android 12 la finestra di un popup
         * **non è modale al tocco**, quindi un dito fuori dal pannello arriva a tutte e due le
         * finestre, e il solo `dismissOnClickOutside` non basta.
         * ⚠️⚠️ **A spostarlo è stato il censimento della UI del 2026-09-05**: questo velo esisteva
         * in **una** schermata su cinque, e le altre quattro avevano il difetto intero (nella
         * vista ad albero il tocco che chiudeva il menu apriva la riga sotto il dito). Ripeterlo
         * qui altre quattro volte avrebbe lasciato in piedi il quinto modo di dimenticarsene: uno
         * solo, sopra tutto quello che l'app disegna, vale per ogni menu che nascerà.
         * ⚠️ **Non è una perdita di comportamento**: quello copriva lo schermo intero e consumava
         * il tocco, e il velo nuovo fa la stessa cosa una passata prima, quindi questo non
         * arriverebbe mai a vederlo.
         */

        /*
         * ⚠️⚠️ **IL MINI ONBOARDING DEL TOCCO LUNGO**, che dalla `0.78` è un velo condiviso:
         * il colore, il contrasto misurato e la geometria stanno in [HintVeil], qui restano la
         * frase e il tastino.
         * ⚠️⚠️ **E dalla `0.73` è l'UNICA via a insegnare la scorciatoia**, perché il tastino
         * 'Tutte' in testata non c'è più (vedi la nota là dove stava): finché c'era, questo
         * velo era un aiuto e la barra la rete di sicurezza.
         * ⚠️⚠️ **I VELI DI QUESTA SCHERMATA SONO DUE perché le scorciatoie sono due** (vedi
         * `shortcut`), e ognuno ha il suo promemoria in archivio: quello della selezione
         * compare alla prima selezione, quello del cestino alla prima apertura del cestino.
         * Prima era uno solo, mostrato anche nel cestino, e prometteva di selezionare 'tutte
         * le immagini della cartella' a chi in una cartella non era: il comportamento era
         * giusto, la frase no.
         */
        if (hint != null) {
            HintVeil(
                text = stringResource(
                    when (hint) {
                        Hint.BIN_EMPTY -> R.string.bin_empty_hint
                        // ⚠️ Le colonne non si insegnano qui: quel velo vive nella schermata
                        // delle cartelle, dove sta il tastino che le cambia. Il ramo c'è
                        // perché [Hint] è un enum e il `when` deve essere completo, e questa
                        // frase non si vedrà mai (vedi `hint`, che la esclude).
                        Hint.COLUMNS -> R.string.columns_hint
                        // ⚠️ Idem per il doppio tocco, che vive nel visualizzatore e non ha
                        // nemmeno un tastino da evidenziare: là il velo è `HintCentre`.
                        Hint.ZOOM_TAP -> R.string.hint_zoom_tap
                        // ⚠️ E idem per l'avviso sulle estensioni, che non è nemmeno un velo
                        // di questa forma: è un `HintNotice`, cioè una finestra sua, aperta
                        // dalla finestra di rinomina.
                        Hint.EXT_WARN -> R.string.hint_ext_warn
                    }
                ),
                // ⚠️ Tre rientri: quello di sistema, il margine della schermata e gli 8dp
                // del tastino. Il perché sta in [HintVeil], sul parametro.
                inset = Modifier
                    .safeDrawingPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp)
                    .padding(8.dp),
                onDone = hintDone
            ) {
                TapHoldFab(
                    label = stringResource(R.string.pick_actions),
                    container = HINT_MARK,
                    ink = HINT_INK,
                    // ⚠️ Nessuna ombra: sopra un velo non c'è niente da cui staccarsi, e
                    // un'ombra su fondo scuro è solo sporco.
                    holdLabel = stringResource(shortcutLabel),
                    onTap = { hintDone(); menu.open() },
                    onHold = { shortcut(); hintDone() },
                    // ⚠️ Lo STESSO glifo del tastino vero, che dalla `1.55` sono i tre puntini:
                    // questo è la sua copia illuminata sopra il velo, e un velo che evidenzia un
                    // disegno diverso da quello che sta sotto indica il tasto sbagliato.
                    glyph = { Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = it) }
                )
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
    /*
     * ⚠️⚠️ **QUI VIVEVA LA CONFERMA DI BUTTARE VIA LA SELEZIONE, dalla 1.06 alla 1.43**, con
     * la domanda e i due verbi 'Mantieni' e 'Scarta' (che erano verbi e non 'Annulla' e 'OK'
     * per una ragione ancora buona: su una domanda 'Annulla' non dice **che cosa** annulla).
     * L'ha tolta l'utente, e al suo posto c'è la notifica con 'Annulla' in fondo alla
     * schermata: il perché sta su [cleared]. ⚠️ Chi la rimettesse avrebbe due cure per lo
     * stesso sbaglio, una che chiede prima e una che disfa dopo.
     */

    /*
     * ⚠️⚠️ **LA CONFERMA DI 'RIPRISTINA TUTTO', dalla 1.53, e NON è pericolosa**: il tasto
     * che conferma non porta il colore dell'errore come quello di 'Svuota il cestino', perché
     * qui non si perde niente. La conferma non serve a fermare un danno, serve a dire **dove
     * finiscono** i file, che è la sola cosa che chi tocca quella voce non può sapere: il
     * perché per esteso sta sulla voce del menu.
     */
    if (restoringAll) {
        AlertDialog(
            onDismissRequest = { restoringAll = false },
            modifier = Modifier.lowered { restoringAll = false },
            title = { Text(stringResource(R.string.bin_restore_all_ask)) },
            text = { Text(stringResource(R.string.bin_restore_all_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        restoringAll = false
                        job = FileJob.Restore(items.orEmpty())
                    }
                ) { Text(stringResource(R.string.bin_restore_all)) }
            },
            dismissButton = {
                TextButton(onClick = { restoringAll = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (emptying) {
        AlertDialog(
            onDismissRequest = { emptying = false },
            modifier = Modifier.lowered { emptying = false },
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
    /** Se sotto la miniatura va il nome del file. Vedi `Settings.gridNames`. */
    named: Boolean,
    /** Quanto è larga la cella, in pixel: serve solo al nome. Vedi `cellPx`. */
    room: Int,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(CORNER)
    // La richiesta si costruisce una volta per indirizzo: ricrearla a ogni
    // ricomposizione darebbe a Coil un oggetto nuovo da confrontare per ogni fotogramma
    // di scorrimento, e questo è il posto in cui i fotogrammi contano.
    val context = LocalContext.current
    val model = remember(uri, context) { Thumbs.request(context, uri) }

    /*
     * ⚠️⚠️ **LA COLONNA C'È ANCHE QUANDO IL NOME NON C'È, ed è la scelta più economica**:
     * un ramo che avvolge la miniatura solo quando serve vorrebbe dire scrivere due volte
     * tutto quello che sta nel riquadro, e un nodo di layout in più per cella non si misura.
     * ⚠️ **Il quadrato sta in cima**, quindi le miniature di una riga restano allineate
     * anche quando i nomi sotto sono di due righe e di una: quello che varia è l'altezza
     * della cella, e la griglia dà a tutta la riga l'altezza della più alta.
     */
    Column(verticalArrangement = Arrangement.spacedBy(NAME_GAP)) {
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
        // ⚠️ Dopo il velo e il nastro, così resta leggibile su una piastrella scelta: dentro
        // un `Box` si dipinge nell'ordine in cui si scrive, e il velo che schiarisce la
        // fotografia schiarirebbe anche la durata.
        if (Videos.isVideo(uri)) ClipBadge(uri, Modifier.align(Alignment.BottomEnd))
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
        if (named) GridName(uri, room)
    }
}

/**
 * La durata di un filmato, nell'angolo della sua miniatura.
 *
 * ⚠️⚠️ **È IL SOLO SEGNO CHE DICE 'QUESTO È UN VIDEO' dalla `0.83`**, e per questo compare
 * anche quando la durata non si sa: là resta il solo triangolo. Un fotogramma senza nessun
 * segno sopra è indistinguibile da una fotografia, e chi lo tocca si aspetta una foto.
 * ⚠️ **L'angolo è quello in basso a destra**, l'unico dei quattro rimasto libero: la spunta
 * della scelta sta in alto a destra, il nastro dell'ultima vista in basso a sinistra, e i tre
 * segni non si toccano mai nemmeno sulla piastrella che li porta tutti.
 * ⚠️ **Bianco su nero e non i colori del tema**, come il velo degli avvisi: questa targhetta
 * sta sopra un'immagine qualunque, non sopra una superficie del tema, quindi il contrasto
 * se lo deve portare da sé.
 * ⚠️ **La durata si chiede una volta per indirizzo** e la prima risposta è quella già in
 * memoria: senza, ogni miniatura che rientra in vista rifarebbe la domanda al MediaStore, e
 * la targhetta comparirebbe con un fotogramma di ritardo ogni volta. Stessa forma di
 * [GridName], stessa ragione.
 */
@Composable
private fun ClipBadge(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val length by produceState(Videos.cachedLength(uri), uri, context) {
        if (value == null) value = Videos.length(context, uri)
    }
    Row(
        modifier = modifier
            .padding(BADGE_EDGE)
            .clip(RoundedCornerShape(BADGE_CORNER))
            .background(BADGE_INK)
            .padding(horizontal = BADGE_PAD, vertical = BADGE_LIP),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BADGE_LIP)
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = stringResource(R.string.grid_item_clip),
            tint = Color.White,
            modifier = Modifier.size(BADGE_GLYPH)
        )
        length?.let {
            Text(
                text = Videos.stamp(it, floor = true),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

/**
 * Il nome del file sotto una miniatura, su **due righe al massimo**.
 *
 * ⚠️⚠️ **NASCE DALLA 0.82** (richiesta dell'utente, con le sue tre regole: *massimo 2 righe,
 * estensione mai spezzata, ellissi nel nome se serve*). Sono le stesse tre della pastiglia di
 * 'Info dettagliate sul file', e infatti a rispettarle è la stessa [fitName]: qui le righe
 * ammesse sono due invece di tre, ed è l'unica differenza.
 * ⚠️ **Il nome NON è nella lista che la griglia riceve**, che è fatta di soli indirizzi: si
 * chiede a [Names], che lo legge una volta sola e se lo ricorda. Il primo valore è quello già
 * in memoria, così una miniatura che rientra in vista non lampeggia senza nome.
 * ⚠️ **`labelSmall` e non `bodySmall`**: sotto una fotografia il nome è un'etichetta, e a
 * 11sp due righe stanno sotto una miniatura da 108dp senza rubarle spazio.
 */
/**
 * La superficie di un tasto a icona di Material, cioè il cerchio dell'increspatura.
 *
 * ⚠️ **Quaranta e non quarantotto**: 48 è il bersaglio del dito, che ci arriva da
 * `minimumInteractiveComponentSize`. Vedi la nota dentro [FilterKey], che è il solo posto in
 * cui questo tasto è scritto a mano invece di essere un `IconButton`.
 */
private val FILTER_KEY = 40.dp

/**
 * Il tasto del filtro volatile, in testata a destra quando non si sta scegliendo.
 *
 * ⚠️⚠️ **STA DOVE STAVA 'SELEZIONA TUTTO'** (richiesta dell'utente, 2026-08-31), cioè in un
 * posto che era rimasto vuoto nella `0.72`: l'angolo in alto a destra di una schermata di
 * contenuti è dove ci si aspetta di trovare un modo di restringere quello che si vede.
 * ⚠️⚠️ **IL TASTO PORTA L'ICONA DEL GENERE MOSTRATO, non un'icona di filtro 'accesa'**
 * (richiesta dell'utente, 2026-08-31): spento sono le tre righe che si accorciano, acceso è
 * la fotografia o la pellicola. Così il segno non dice soltanto *che* si sta filtrando, dice
 * *che cosa* si vede, che è l'informazione che serve a chi guarda una cartella dimezzata.
 * ⚠️ **Il tondo dietro è la seconda metà del segno e non un ornamento**: il glifo cambia
 * disegno, quindi da solo si potrebbe leggere come un tasto diverso; il tondo dice
 * 'quel tasto, adesso attivo' ed è la convenzione di Material per uno stato acceso. Il
 * filtro si azzera da sé cambiando cartella, ma dentro la stessa cartella resta, e senza un
 * segno una cartella con metà delle cose nascoste sembra una cartella che le ha perse.
 * ⚠️ **Il popup è di soli SIMBOLI, in fila** (stessa richiesta): pellicola, fotografia e la
 * croce che toglie il filtro. Tre voci con l'etichetta scritta sarebbero un menu, e questa è
 * una levetta che si tocca al volo mentre si guardano le miniature.
 * ⚠️⚠️ **`MediaKind.ALL` HA DUE DISEGNI, ed è voluto**: sul tasto è il filtro spento, quindi
 * le tre righe; nel popup è il comando che lo toglie, quindi una croce. Stessa scelta sotto,
 * due frasi diverse: 'non sto filtrando' e 'smetti di filtrare'. Un solo glifo per tutti e
 * due avrebbe detto la cosa sbagliata da una delle due parti.
 * ⚠️ **La croce non è un terzo genere ma un'AZIONE**, ed è la ragione per cui non si accende
 * mai: gli altri due portano il tondo quando sono quello in vigore, lei no.
 * ⚠️ **Le due icone sono quelle dei contatori sotto le copertine** (`Outlined.Image` e
 * `Outlined.Movie`): in questa app quei due glifi vogliono già dire 'fotografie' e
 * 'filmati', e un terzo disegno per la stessa cosa sarebbe una parola nuova per un concetto
 * vecchio.
 * ⚠️ **Lo stato lo annuncia `stateDescription` e non la descrizione dell'icona**: il tasto
 * *fa* sempre la stessa cosa (apre il filtro), e a cambiare è come sta. Mettere il genere
 * nella descrizione direbbe a chi legge con TalkBack che il tasto serve a mostrare i video.
 */
@Composable
private fun FilterKey(filter: MediaKind, onFilter: (MediaKind) -> Unit, onSearch: () -> Unit) {
    val menu = rememberMenuState()
    val res = LocalResources.current
    Box {
        /*
         * ⚠️⚠️ **NON È UN `IconButton`, ED È L'UNICA RAGIONE PER CUI È SCRITTO A MANO**: quello
         * di Material non espone il tocco lungo, e da qui ne parte uno (vedi sotto). Le due
         * misure sono le sue, prese dal suo sorgente: **40dp** di superficie, cioè il cerchio
         * dell'increspatura, e il bersaglio portato a 48 da `minimumInteractiveComponentSize`.
         * Scriverne una sola farebbe di questo l'unico tasto della testata con
         * un'increspatura di un'altra misura.
         *
         * ⚠️⚠️ **IL TOCCO LUNGO APRE LA RICERCA PER NOME, dalla 1.50** (richiesta dell'utente,
         * 2026-09-04: *voglio che un tocco lungo sull'icona del filtro (in alto a destra) apra
         * la ricerca dei file per nome (come FAB della home -> Cerca)*). È la **stessa**
         * ricerca, non una sua parente: chiama quello che chiama la voce 'Cerca' del menu
         * della schermata iniziale, quindi cerca in tutta la galleria e non nella sola
         * cartella aperta.
         * ⚠️ **L'etichetta del gesto è quella della voce del menu** e non una stringa nuova:
         * nomina la stessa azione, e un secondo testo per la stessa cosa sarebbe due testi da
         * tenere d'accordo in ventotto lingue.
         */
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(FILTER_KEY)
                .clip(CircleShape)
                .combinedClickable(
                    onClick = { menu.open() },
                    onLongClick = withHaptics(onSearch),
                    onLongClickLabel = stringResource(R.string.hub_search),
                    role = Role.Button
                )
                .semantics { stateDescription = res.getString(filter.label()) },
            contentAlignment = Alignment.Center
        ) {
            FilterMark(lit = filter != MediaKind.ALL) {
                Icon(
                    imageVector = filter.onKey(),
                    contentDescription = stringResource(R.string.filter_title)
                )
            }
        }
        /*
         * ⚠️⚠️ **PASSA DALLA SUPERFICIE UNICA DALLA `1.46`, e prima era un `DropdownMenu`**:
         * con lui questa era l'unica superficie dell'app senza velo, perché il velo se lo
         * deve chiedere e qui nessuno lo chiedeva. La `1.46` aveva prima aggiunto la riga a
         * mano; adesso il velo arriva perché la superficie è la stessa di tutti, e non c'è
         * più nessun elenco di chiamanti da tenere vero.
         * ⚠️ **Il difetto era nascosto da una frase falsa**, ed è la parte che vale: un
         * commento dava un altro menu per 'l'unico menu dell'app che non passa da
         * `MenuShell`', quindi chi cercava i chiamanti si fermava e li credeva tutti. Non si
         * vedeva perché il velo è spento di fabbrica dalla `1.39`.
         * ⚠️ **Il contenuto resta una fila di tre tasti**, e non diventa un elenco di voci:
         * una disposizione diversa non è un secondo modo di fare un menu.
         */
        MenuShell(
            state = menu,
            position = rememberMenuSpot(MenuSide.AT_ANCHOR, MenuSide.AFTER_ANCHOR)
        ) {
            Row(modifier = Modifier.padding(horizontal = FILTER_PAD)) {
                // ⚠️ Pellicola, fotografia e croce, in quest'ordine: è quello chiesto, e non
                // l'ordine dell'enum, che comincia da 'tutto'. La croce sta in fondo perché
                // è l'unica che non sceglie niente.
                for (kind in listOf(MediaKind.VIDEOS, MediaKind.IMAGES, MediaKind.ALL)) {
                    IconButton(onClick = { menu.close(); onFilter(kind) }) {
                        FilterMark(lit = kind != MediaKind.ALL && kind == filter) {
                            Icon(
                                imageVector = kind.inMenu(),
                                contentDescription = stringResource(kind.label())
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Il tondo che sta dietro all'icona del filtro quando il filtro è in vigore.
 *
 * ⚠️ **Più piccolo del bersaglio del tocco**: il tasto resta 48dp perché un bersaglio più
 * stretto si manca, ma il tondo dipinto è [FILTER_MARK], così sta dentro la testata invece
 * di sembrare un secondo tasto attaccato agli altri.
 * ⚠️ **36dp e non 32, dalla 1.06** (riscontro dell'utente sul collaudo: *tondo più grande ma
 * proprio di un filo, poco di più*). Quattro punti sono il massimo che si può prendere
 * restando dentro il bersaglio da 48: da lì in su il tondo comincia a toccarne i bordi, e
 * torna a sembrare un tasto invece di un segno.
 */
@Composable
private fun FilterMark(lit: Boolean, glyph: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(FILTER_MARK)
            .clip(CircleShape)
            .background(
                if (lit) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides
                if (lit) MaterialTheme.colorScheme.onSecondaryContainer
                else LocalContentColor.current
        ) { glyph() }
    }
}

/** Il disegno che questo filtro porta **sul tasto**: spento sono le tre righe. */
private fun MediaKind.onKey(): ImageVector = when (this) {
    MediaKind.ALL -> Icons.Outlined.FilterList
    MediaKind.IMAGES -> Icons.Outlined.Image
    MediaKind.VIDEOS -> Icons.Outlined.Movie
}

/** Il disegno che questo filtro porta **nel popup**: 'tutto' là è la croce che lo toglie. */
private fun MediaKind.inMenu(): ImageVector = when (this) {
    MediaKind.ALL -> Icons.Default.Close
    else -> onKey()
}

/** Come si chiama questo filtro, per chi legge lo schermo. */
private fun MediaKind.label(): Int = when (this) {
    MediaKind.ALL -> R.string.filter_all
    MediaKind.IMAGES -> R.string.filter_images
    MediaKind.VIDEOS -> R.string.filter_videos
}

/**
 * Il peso totale di quello che si è scelto, in testata a destra.
 *
 * ⚠️⚠️ **SI CHIEDE AL MEDIASTORE E NON SI SOMMA A OCCHIO**: il peso di un file non sta
 * nell'indirizzo, quindi ogni cambio di selezione è una interrogazione. È la stessa
 * `factsOf` del dialogo delle informazioni, che quel conto lo sa già fare.
 * ⚠️⚠️ **L'ATTESA PRIMA DI CONTARE È LA COSA CHE RENDE LA FUNZIONE POSSIBILE**: scegliendo
 * col trascinamento la selezione cambia decine di volte al secondo, e senza questa pausa
 * partirebbe una interrogazione per ogni fotografia sfiorata. `produceState` annulla la
 * precedente a ogni cambio, quindi durante il trascinamento non ne parte nessuna e il conto
 * si fa quando il dito si ferma.
 * ⚠️ **Vuoto e non uno zero mentre conta**: uno zero è un peso, e per un istante direbbe
 * una cosa falsa. Lo spazio vuoto si legge come 'sto arrivando'.
 */
@Composable
private fun PickWeight(chosen: Set<Uri>) {
    val context = LocalContext.current
    val weight by produceState<Long?>(null, chosen, context) {
        value = null
        delay(WEIGH_WAIT)
        value = factsOf(context, chosen.toList()).bytes
    }
    Text(
        text = weight?.let { formatBytes(it) }.orEmpty(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun GridName(uri: Uri, room: Int) {
    val context = LocalContext.current
    val name by produceState(Names.cached(uri), uri, context) {
        if (value == null) value = Names.of(context, uri)
    }
    val style = MaterialTheme.typography.labelSmall
    val measurer = rememberTextMeasurer()
    // ⚠️⚠️ **IL MARGINE SI TOGLIE DALLA MISURA, e senza questo il nome sforerebbe**: il
    // numero che arriva è la larghezza della **cella**, e il testo ne ha due dp in meno per
    // lato. Misurando sulla cella intera, `fitName` crederebbe di avere quattro dp che il
    // layout poi non gli dà, e l'ultima lettera finirebbe tagliata.
    // ⚠️ Il margine c'è perché il distacco fra le celle è 3dp: due nomi lunghi in due celle
    // vicine si toccherebbero quasi.
    val bordo = with(LocalDensity.current) { NAME_PAD.roundToPx() }
    val utile = room - bordo * 2
    val shown = remember(name, utile, style, measurer) {
        name?.let { fitName(it, utile, NAME_LINES, style, measurer) }
    } ?: return
    Text(
        text = shown.text,
        // ⚠️ Il corpo è quello a cui il nome è stato MISURATO, dalla 1.62: `fitName` può
        // stringerlo di un gradino per far stare il nome intero, e scriverlo alla misura
        // piena rimetterebbe lo sforo che quella stretta ha appena tolto.
        style = style.shrunk(shown.scale),
        maxLines = NAME_LINES,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = NAME_PAD)
    )
}

/**
 * Quanto si aspetta prima di contare il peso della selezione.
 *
 * ⚠️ Un terzo di secondo: abbastanza perché un trascinamento non faccia partire niente,
 * poco perché un tocco singolo non sembri lento. Da rivedere col dito, come ogni numero
 * che dipende da un gesto.
 */
private const val WEIGH_WAIT = 300L

/**
 * Il lato minimo di una miniatura.
 *
 * ⚠️ È una misura, non un gusto: a 108dp uno schermo da 360dp di larghezza tiene
 * **tre** colonne con i distacchi, che è la densità delle gallerie di sistema; a 96
 * ne terrebbe quattro, e su un telefono la faccia in una foto di gruppo non si
 * riconosce più.
 */
/**
 * Quante colonne stanno davvero in scena: [scelte] sul lato corto, di più se lo schermo è largo.
 *
 * ⚠️⚠️ **SOSTITUISCE `GridCells.Adaptive` DALLA `1.66`, E NE CONSERVA IL PREGIO.** Quella dava
 * tre colonne su un telefono e sei ruotandolo, senza un ramo per ogni forma di schermo, ma
 * decideva **lei** quanto sono grandi le miniature, e dalla `1.66` quel numero è una scelta
 * dell'utente. Qui la scelta dice quante colonne stanno sul **lato corto**, e ruotando o su uno
 * schermo più largo ne entrano altrettante della stessa misura: il rapporto fra i due lati fa il
 * conto, quindi non c'è nessuna larghezza di riferimento scritta a mano da rifare il giorno che
 * esce un telefono di un'altra forma.
 * ⚠️ **Non scende mai sotto la scelta**: in verticale il rapporto vale 1 e il conto la restituisce
 * intera, e su una finestra più stretta dell'alta il tetto la protegge lo stesso.
 * ⚠️ **Con una finestra ancora da misurare vale la scelta**: al primo fotogramma la misura può
 * essere zero, e una divisione per zero darebbe una griglia a una colonna che poi salta.
 */
private fun spread(scelte: Int, finestra: WindowInfo): Int {
    val misura = finestra.containerSize
    val corto = minOf(misura.width, misura.height)
    if (corto <= 0) return scelte
    return (scelte.toFloat() * misura.width / corto).roundToInt().coerceAtLeast(scelte)
}

private val THUMB = 108.dp

/** Il distacco fra le miniature: c'è, ma non deve leggersi come una cornice. */
private val GAP = 3.dp

/**
 * Quante righe può prendere il nome sotto una miniatura: **due**, come chiesto.
 *
 * ⚠️ Due e non tre come nella pastiglia di 'Info': là il nome è il soggetto del dialogo, qui
 * è una didascalia sotto una fotografia, e una terza riga la farebbe diventare il soggetto
 * della cella.
 */
private const val NAME_LINES = 2

/** Quanto stacca il nome dalla sua miniatura: poco, perché sono la stessa cosa. */
private val NAME_GAP = 2.dp

/** Il margine laterale del nome, che [GridName] toglie anche dalla misura. */
private val NAME_PAD = 2.dp

// ── La targhetta della durata ───────────────────────────────────────────────
/** Quanto la targhetta si stacca dall'angolo della miniatura. */
private val BADGE_EDGE = 4.dp

/** L'arrotondamento della targhetta: poco, perché è una targhetta e non una pastiglia. */
private val BADGE_CORNER = 4.dp

/** Il nero della targhetta, semitrasparente perché la fotografia si intraveda sotto. */
private val BADGE_INK = Color(0x99000000)

/** Il respiro ai lati del testo dentro la targhetta. */
private val BADGE_PAD = 4.dp

/** Il respiro sopra e sotto, e il distacco fra triangolo e cifre: sono la stessa misura. */
private val BADGE_LIP = 2.dp

/** Il triangolo: della misura del testo che gli sta accanto, non di più. */
private val BADGE_GLYPH = 12.dp

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
 * Il menu del cestino: **sul lato del tastino**, sopra di lui.
 *
 * ⚠️⚠️ **STAVA AL CENTRO FINO ALLA `1.53`, E ADESSO STA DOVE STA QUELLO DELLA SCHERMATA
 * INIZIALE** (riscontro dell'utente, giro della `1.53`, voce `sfocatura-segue`: *i due pannelli
 * che si aprono alla pressione sul FAB hanno due funzionamenti diversi e non capisco perché. Il
 * pannello della schermata home a questo punto è eccellente, e mi va bene che stia sul lato del
 * FAB. Ma perché quella del cestino non fa altrettanto?*). Adesso i due chiedono lo **stesso**
 * posizionatore, con gli stessi due lati e senza distacco, quindi non possono più comportarsi
 * in due modi.
 * ⚠️⚠️ **E TOGLIE LA RAGIONE PER CUI IL DIFETTO DELLA SFOCATURA SI VEDEVA PROPRIO LÌ**: un
 * pannello centrato ha lo sfondo dai **due** lati, sopra le miniature, mentre appoggiato al
 * bordo ne ha uno solo. Non è la cura di quel difetto, ma è la ragione per cui il cestino era
 * il posto in cui si notava di più.
 * ⚠️ **Il centro veniva dalla `0.75`** (richiesta di allora: *al centro in basso, con angoli un
 * po' più stondati*), quando questo menu serviva alla **selezione** e non al cestino: la
 * selezione da tempo ha la sua scheda in fondo, e quella richiesta è stata sostituita da questa.
 *
 * ⚠️⚠️ **NON È UN `DropdownMenu`, dalla 0.75**. Un `DropdownMenu` si posiziona **accanto al suo
 * genitore** e non accetta un posizionatore: attaccato a un tastino in basso a destra,
 * usciva da quell'angolo. La superficie e il posizionatore stanno in [MenuShell] e `MenuSpot`,
 * condivisi dalla `1.46` con **ogni** menu dell'app.
 * ⚠️⚠️ **DALLA 1.06 SI CHIUDE TOCCANDO FUORI**, che fino alla `1.05` era spento apposta
 * perché il tastino lo **alternava** e le due cose si pestavano (il perché sta in
 * [MenuShell], dove fino alla `1.46` era un parametro). Adesso il tastino si limita ad
 * aprire, e a chiudere ci pensa
 * il velo trasparente della schermata: nessuno dei due può più riaprire quello che l'altro
 * ha appena chiuso. Questo resta acceso per il caso che il velo non copre, cioè un tocco
 * fuori dalla finestra dell'app.
 */
@Composable
private fun PickMenu(menu: MenuState, content: @Composable () -> Unit) {
    MenuShell(
        // ⚠️ La stessa coppia della schermata iniziale, e non una che le somiglia: allineato al
        // tastino in orizzontale, e sopra di lui perché sotto non ci sta. Scriverla uguale è
        // quello che rende impossibile che i due menu si comportino in modo diverso.
        state = menu,
        position = rememberMenuSpot(MenuSide.AT_ANCHOR, MenuSide.AFTER_ANCHOR),
        content = content
    )
}

/*
 * ⚠️ **Lo stondamento non vive più qui, dalla 1.28**: era 16 mentre gli altri due menu
 * erano a 8, e adesso è `MENU_ROUND` in `Menus.kt`, uno per tutti. La nota vecchia
 * spiegava perché questo ne volesse più degli altri (quasi quadrato contro lista larga):
 * l'argomento era buono e la conclusione sbagliata, perché uno stondamento dice che cosa
 * è una superficie, non quanto è larga.
 */

/**
 * Il tondo che segna il filtro in vigore, dietro alla sua icona.
 *
 * ⚠️ **32 e non 48**: il tasto resta il bersaglio da 48 che Material chiede, ma il tondo
 * dipinto è più stretto dell'icona più il suo respiro. A 48 toccherebbe i vicini e la
 * testata sembrerebbe avere un tasto in più.
 */
private val FILTER_MARK = 36.dp

/**
 * Il respiro ai lati della fila di simboli del filtro.
 *
 * ⚠️ Serve perché quel popup non ha voci di menu, e senza voci non ha nemmeno il loro
 * rientro: i tre tasti finirebbero appiccicati al bordo stondato.
 */
private val FILTER_PAD = 4.dp

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


package io.github.roccobot.aiv

import android.graphics.Color
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Una scheda dal basso: appoggiata al bordo di sotto, larga quanto lo schermo, con un titolo in
 * cima, un contenuto che scorre e un piede fermo.
 *
 * ⚠️ **Entra salendo dal basso, con la sua animazione e non con quella della finestra** (richiesta
 * dell'utente: arrivo fluido che decelera, con una breve dissolvenza). L'animazione della finestra
 * è spenta (`setWindowAnimations(0)` in [sheetWindow]) e la scheda sale con la molla di
 * [ARRIVO_RIGIDITA]: riaccendere quella di sistema darebbe due salite sovrapposte. Si muove solo
 * l'asse verticale, senza crescita, perché una superficie ancorata al bordo che si stringe ai
 * fianchi contraddirebbe la salita. La dissolvenza finisce presto ([SHEET_FADE_MS]), e il resto del
 * viaggio la scheda lo fa opaca. Non è una `ModalBottomSheet`, che si trascina e prende tutti i
 * tocchi.
 *
 * Il piede è fuori dalla colonna che scorre, quindi il contenuto gli scorre dietro. Il corpo prende
 * il posto che avanza e non di più (`weight(1f, fill = false)`): con poche righe la scheda è bassa,
 * con molte cresce fino allo schermo e scorre.
 * ⚠️ **Lo scorrimento è di questa scheda, e il contenuto non deve averne un altro: due annidati
 * chiudono l'app.** Quello di fuori misura il contenuto con altezza infinita, e
 * `checkScrollableContainerConstraints` va in errore (il caso vero era `FileFacts`, nota là).
 *
 * Si chiude toccando fuori, e quel tocco lo raccoglie la scheda: con `usePlatformDefaultWidth =
 * false` la finestra copre lo schermo, e per il sistema non esiste un fuori. Il gesto Indietro
 * chiude sempre, e la crocetta è la via visibile.
 */
@Composable
fun Sheet(
    title: String,
    onDismiss: () -> Unit,
    foot: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    /*
     * ⚠️ **La scheda si chiude in due tempi**, o l'uscita non si vedrebbe: chiamato `onDismiss`, la
     * finestra non c'è più. Quindi `chiudi` spegne [visibile], che avvia la discesa, e il congedo
     * vero lo fa il `finishedListener` quando il movimento è a zero. I tre modi di chiudere (tocco
     * sopra la scheda, crocetta, gesto Indietro) passano tutti da qui. [visibile] parte da `false`
     * e si accende all'ingresso, e serve anche alla salita.
     */
    var visibile by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visibile = true }
    val chiudi: () -> Unit = { visibile = false }
    /*
     * Sul tablet, da 600 dp, la scheda non supera [Adaptive.sheetMaxWidth] ed è centrata in basso
     * (mockup `help`, `.dialog-preview`); sul telefono è a tutta larghezza.
     */
    val sheetMax = Adaptive.sheetMaxWidth(LocalConfiguration.current.screenWidthDp)

    Dialog(
        onDismissRequest = chiudi,
        /*
         * ⚠️ **`decorFitsSystemWindows = false` porta il fondo della scheda sotto la barra di
         * sistema** (richiesta dell'utente: la barra in basso del colore della scheda). Di serie la
         * finestra di un dialogo si ferma sopra quella barra, e là sotto si vedeva la pagina. Il
         * contenuto resta dov'è: il rientro lo applica la colonna interna
         * ([navigationBarsPadding]).
         */
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        sheetWindow()

        /*
         * La patina si scioglie all'uscita con una coda che decelera (richiesta dell'utente), e
         * all'andata è piena da subito ([snap]). Non costa niente: l'uscita dura già [USCITA_MS],
         * più della coda.
         */
        val patina by animateFloatAsState(
            targetValue = if (visibile) 1f else 0f,
            animationSpec = if (visibile) snap() else tween(VEIL_FADE_MS, easing = VEIL_FADE),
            label = "sheet-veil"
        )

        /*
         * La sfocatura ha il suo tempo e se ne va con la dissolvenza della scheda: ridurre un
         * raggio a poco a poco si legge come una messa a fuoco (il perché per esteso è in fondo a
         * `Veil.kt`). A funzione spenta conta la sola [patina], che dosa il velo semplice
         * ([SHEET_DIM]): quello è un livello con la sua opacità, e la coda là è giusta.
         */
        val sfoca by animateFloatAsState(
            targetValue = if (visibile) 1f else 0f,
            animationSpec = if (visibile) snap() else tween(SHEET_FADE_MS),
            label = "sheet-blur"
        )
        val acceso = LocalAivDepth.current == PanelDepth.BLUR
        val alzata = LocalAivDepth.current == PanelDepth.SHADOW
        AppPatina { patina }
        WindowVeil(bare = SHEET_DIM, patina = { patina }) { if (acceso) sfoca else patina }

        /*
         * ⚠️ **Due animazioni e non una, perché durano tempi diversi**: la salita è una molla, la
         * dissolvenza dura [SHEET_FADE_MS]. Un'opacità ricavata dalla posizione rallenterebbe con
         * la molla proprio in fondo. Ognuna ha due versi: all'andata la molla e la dissolvenza in
         * testa, al ritorno la molla letta all'indietro ([fuga]) e la dissolvenza in coda
         * ([sfumaVia]).
         * ⚠️ **Il congedo vero è nel `finishedListener` della salita**: è l'animazione più lunga,
         * quindi la sola che finisce quando la scheda è davvero fuori.
         */
        val lift by animateFloatAsState(
            targetValue = if (visibile) 1f else 0f,
            animationSpec = if (visibile) arrivo() else fuga(),
            label = "sheet-lift",
            finishedListener = { fine -> if (fine == 0f) onDismiss() }
        )
        val show by animateFloatAsState(
            targetValue = if (visibile) 1f else 0f,
            animationSpec = if (visibile) tween(durationMillis = SHEET_FADE_MS) else sfumaVia(),
            label = "sheet-fade"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                /*
                 * ⚠️ **Il rientro in alto è qui, contropartita di `decorFitsSystemWindows =
                 * false`**: senza, una scheda lunga crescerebbe fino al bordo di sopra e il titolo
                 * finirebbe sotto l'orologio. Applicato al riquadro diventa un tetto: una scheda
                 * corta non si muove, una lunga si ferma sotto la barra di stato. Solo in alto: in
                 * fondo il riquadro deve arrivare al bordo.
                 */
                .statusBarsPadding()
                /*
                 * Niente increspatura e nessuna descrizione: è lo spazio vuoto sopra la scheda, non
                 * un tasto.
                 */
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = chiudi
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (sheetMax != null) Modifier.widthIn(max = sheetMax) else Modifier
                    )
                    /*
                     * ⚠️ **La distanza è l'altezza della scheda stessa** (`size` di questo strato):
                     * la scheda parte da sotto il proprio bordo qualunque sia la sua altezza,
                     * mentre una distanza fissa in `dp` farebbe partire una scheda alta già mezza
                     * dentro. Nessun `clip`: quello che è sotto il bordo è fuori dallo schermo, e
                     * un `clipToBounds` taglierebbe anche il velo.
                     */
                    .graphicsLayer {
                        alpha = show
                        translationY = (1f - lift) * size.height
                    }
                    /*
                     * ⚠️ **L'aria in cima c'è solo con l'ombra, e senza l'ombra sarebbe tagliata**:
                     * il `graphicsLayer` qui sopra ha un'opacità, quindi Compose disegna quello che
                     * è sotto in un buffer grande quanto questo nodo (il perché per esteso è su
                     * [LIFT_ROOM]). Solo in cima, perché fianchi e fondo sono contro i bordi dello
                     * schermo. È fuori dal `clickable` qui sotto, quindi un tocco sull'aria chiude
                     * la scheda come il resto dello spazio sopra.
                     */
                    .padding(top = if (alzata) LIFT_ROOM else 0.dp)
                    /*
                     * Il tocco sulla scheda non deve chiudere: il velo di sopra è un genitore, e un
                     * tocco che nessuno consuma gli arriverebbe. Un `clickable` senza effetto lo
                     * ferma qui.
                     */
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { }
                    )
                    // L'ombra delle sole cime: il perché è in testa a `Edge.kt`.
                    .liftedTop(PANEL_ROUND),
                shape = RoundedCornerShape(topStart = PANEL_ROUND, topEnd = PANEL_ROUND),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(
                    /*
                     * Il rientro di sistema è qui e non sulla scheda: il fondo della scheda arriva
                     * al bordo dello schermo, il contenuto no, o l'ultima riga finirebbe sotto la
                     * barra.
                     */
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(
                            start = SHEET_PAD,
                            end = SHEET_PAD,
                            top = SHEET_PAD_TOP,
                            bottom = SHEET_PAD
                        )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        /*
                         * Il titolo è allineato a sinistra (richiesta dell'utente) e comincia dal
                         * rientro, come ogni altro testo qui dentro.
                         */
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = chiudi, modifier = Modifier.size(SHEET_SHUT)) {
                            Icon(
                                /* La croce piena, come tutte le altre chiusure dell'app. */
                                imageVector = Glyphs.Material.Close,
                                // Descritta come 'Chiudi', il nome del tasto.
                                contentDescription = stringResource(R.string.pick_close)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = SHEET_GAP),
                        verticalArrangement = Arrangement.spacedBy(SHEET_GAP)
                    ) {
                        content()
                    }
                    foot()
                }
            }
        }
    }
}

/**
 * Toglie alla finestra del dialogo la sua animazione e la stende su tutto lo schermo.
 *
 * ⚠️ **`setWindowAnimations(0)` evita due movimenti**: l'animazione di serie di una finestra
 * appoggiata in basso è proprio una salita, e accesa si sommerebbe a quella di [Sheet], con un
 * tempo deciso dal telefono. Togliendo questa riga si otterrebbe un sussulto. La finestra si stende
 * su tutto e la scheda si appoggia in basso da sé, così lo spazio sopra raccoglie il tocco che
 * chiude. `Gravity.BOTTOM` con una finestra a tutto schermo non sposta niente: dice qual è il bordo
 * di riferimento se la finestra tornasse alta quanto il contenuto.
 */
@Composable
private fun sheetWindow() {
    val view = LocalView.current
    DisposableEffect(view) {
        // La stessa risalita del velo per trovare la finestra: vedi `dialogWindow` in `Veil.kt`.
        val window = view.dialogWindow()
        window?.apply {
            setWindowAnimations(0)
            setGravity(Gravity.BOTTOM)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // Sfondo trasparente, o dietro la scheda si vedrebbe il rettangolo di serie del
            // dialogo.
            setBackgroundDrawableResource(android.R.color.transparent)
            edgeToEdge()
        }
        onDispose { }
    }
}

/**
 * Le tre cose che servono a una finestra di dialogo per dipingere da sé sotto le barre di sistema,
 * cioè quello che `enableEdgeToEdge()` fa all'attività e a nessun'altra finestra.
 *
 * ⚠️ **Il flag lo aggiunge Compose solo a una finestra alta quanto il contenuto**: nel sorgente di
 * `compose-ui` 1.12.0 `DialogLayout.internalOnMeasure` aggiunge `FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS`
 * solo se `window.attributes.height == WRAP_CONTENT`, e questa scheda la mette a `MATCH_PARENT`.
 * Senza il flag il sistema dipinge la sua barra opaca sopra il fondo della scheda.
 * ⚠️ **Da Android 10 serve anche spegnere il contrasto della barra**
 * (`isNavigationBarContrastEnforced`, vero di fabbrica), che nella navigazione a gesti dipinge una
 * patina traslucida: `enableEdgeToEdge()` lo spegne sull'attività, e su una finestra nuova torna
 * acceso. La striscia in fondo sarebbe quasi del colore della scheda, e sembrerebbe un difetto di
 * tinta. I due colori si dichiarano anche se `Theme.AIV` li ha già trasparenti: il dialogo ha una
 * finestra sua, e un tema cambiato li rimetterebbe opachi qui senza che nessuno se ne accorga.
 */
@Suppress("DEPRECATION")
private fun Window.edgeToEdge() {
    addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    statusBarColor = Color.TRANSPARENT
    navigationBarColor = Color.TRANSPARENT
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        isNavigationBarContrastEnforced = false
    }
}


/** Il rientro attorno al contenuto, e l'aria fra una riga e l'altra. */
private val SHEET_PAD = 24.dp
private val SHEET_GAP = 12.dp

/**
 * Il rientro in alto, la metà degli altri (richiesta dell'utente: meno spazio sopra la crocetta).
 * ⚠️ Quello che si vede è il rientro più il bersaglio di [SHEET_SHUT]: sopra il disegno della
 * crocetta ci sono 12 di rientro più 12 di bersaglio, cioè 24. Ritoccando il solo rientro si
 * sbaglia di dodici punti.
 */
private val SHEET_PAD_TOP = 12.dp

/**
 * Il lato della crocetta: 48 dp, l'area toccabile minima di Material, non la misura del disegno.
 */
private val SHEET_SHUT = 48.dp

/**
 * Come arriva una scheda dal basso: una molla critica, che frena e non rimbalza.
 *
 * `internal` perché le schede dal basso sono due, questa e quella della selezione (`PickSheet`), e
 * arrivare dal basso è un gesto solo (vedi [arrivaDalBasso]).
 *
 * ⚠️ **Una molla e non una curva, per una misura**: l'`emphasized decelerate` di Material 3
 * (`cubic-bezier(0.05, 0.7, 0.1, 1.0)`) su 320 ms fa l'83% della strada nei primi 80 ms, e a
 * schermo è uno scatto seguito da niente. La molla, agli stessi 80 ms, ha ancora da fare il 52%, ed
 * entra nell'ultimo 1% a 332 ms. La curva scartata resta scritta perché è quella che ogni documento
 * di Material consiglia.
 * ⚠️ **Non è il tempo dei menu** (`MENU_IN` in `Menus.kt`): un menu compare dov'era il dito e non
 * viaggia, una scheda percorre la sua altezza, e lo stesso tempo su quella distanza sarebbe uno
 * scatto. `StiffnessMediumLow` è anche la molla di fabbrica di `slideInVertically`, quella che la
 * scheda della selezione ha da sempre.
 */
internal const val ARRIVO_RIGIDITA = Spring.StiffnessMediumLow

/** La molla di sopra, per un numero da 0 a 1. Vedi [ARRIVO_RIGIDITA]. */
internal fun arrivo(): SpringSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = ARRIVO_RIGIDITA
)

/**
 * Quanto dura la dissolvenza: breve, come chiesto, quindi a 110 ms la scheda è già opaca e la molla
 * ha fatto poco più di metà strada. Ha un tempo suo e non la curva della salita, che la farebbe
 * rallentare in fondo. La curva è quella di serie di `tween`, `FastOutSlowIn` (misurato sul
 * bytecode di `animation-core`).
 */
internal const val SHEET_FADE_MS = 110

// ── L'uscita, che è l'entrata letta all'indietro ────────────────────────────
/*
 * 'Speculare' preso alla lettera (richiesta dell'utente: uscita speculare all'entrata, che accelera
 * e scorre giù con una breve dissolvenza): l'uscita è la stessa molla percorsa dalla fine
 * all'inizio, quindi accelera per costruzione. L'`emphasized accelerate` di Material sarebbe
 * un'altra animazione. La molla critica ha una posizione in forma chiusa, scritta in [MOLLA].
 */

/**
 * Quanto dura l'uscita: 332 ms, misurato, l'istante in cui la molla di [ARRIVO_RIGIDITA] entra nel
 * suo ultimo 1%. Una durata diversa romperebbe la specularità aprendo e chiudendo di seguito.
 */
internal const val USCITA_MS = 332

/**
 * La curva dell'uscita: [MOLLA] letta all'indietro, quindi accelera. Misurata: a un decimo del
 * tempo ha fatto lo 0,8% della strada, a un quarto il 3,1%, a metà il 14,8%; l'entrata agli stessi
 * istanti è al 14,5%, al 49,9% e all'85,2%.
 */
internal val ACCELERA: Easing = Easing { f -> 1f - MOLLA(1f - f) }

/** L'uscita per un numero da 0 a 1. Vedi [ACCELERA]. */
internal fun fuga(): TweenSpec<Float> = tween(durationMillis = USCITA_MS, easing = ACCELERA)

/**
 * Arrivare dal basso: il gesto di tutte le superfici appoggiate al bordo di sotto (la scheda della
 * selezione, quella dell'editor e la notifica). Oltre ai numeri condivisi è condivisa la forma,
 * cioè quale curva su quale proprietà e dove cade la dissolvenza, che l'utente ha dettato.
 * - La molla si scrive per esteso apposta: lasciata implicita, sarebbe quella di Compose e un
 *   ritocco a [ARRIVO_RIGIDITA] non arriverebbe qui.
 */
internal fun arrivaDalBasso(): EnterTransition = slideInVertically(
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = ARRIVO_RIGIDITA,
        visibilityThreshold = IntOffset.VisibilityThreshold
    ),
    initialOffsetY = { it }
) + fadeIn(animationSpec = tween(durationMillis = SHEET_FADE_MS))

/**
 * Andarsene in basso: l'uscita speculare a [arrivaDalBasso], con la dissolvenza in coda (vedi
 * [sfumaVia]).
 */
internal fun vaGiu(): ExitTransition = slideOutVertically(
    // La curva di [fuga], scritta per un `IntOffset`: quella funzione torna una specifica per un
    // numero da 0 a 1, e uno spostamento in pixel vuole la sua. I numeri sono gli stessi.
    animationSpec = tween(durationMillis = USCITA_MS, easing = ACCELERA),
    targetOffsetY = { it }
) + fadeOut(animationSpec = sfumaVia())

/**
 * La dissolvenza dell'uscita: la stessa di [SHEET_FADE_MS], ma in coda e non in testa. All'andata è
 * nei primi 110 ms, al ritorno negli ultimi: senza il ritardo la scheda diventerebbe trasparente
 * subito e scenderebbe come un fantasma.
 */
internal fun sfumaVia(): TweenSpec<Float> =
    tween(durationMillis = SHEET_FADE_MS, delayMillis = USCITA_MS - SHEET_FADE_MS)

/**
 * La posizione della molla critica di [ARRIVO_RIGIDITA], normalizzata a 0..1 su [USCITA_MS].
 *
 * La formula dello smorzamento critico, `1 - (1 + wt)e^(-wt)`, con `w` la radice della rigidità:
 * chi cambia [ARRIVO_RIGIDITA] cambia [TAU] con lei, o le due animazioni non sono più speculari. La
 * divisione per [CODA] serve perché a `USCITA_MS` la molla è al 99%: senza, l'uscita partirebbe con
 * un gradino dell'1%.
 */
private val MOLLA: (Float) -> Float = { u ->
    val x = TAU * u
    ((1f - (1f + x) * exp(-x)) / CODA).coerceIn(0f, 1f)
}

/** `w * T`: la radice di [ARRIVO_RIGIDITA] per [USCITA_MS] in secondi. */
private val TAU = sqrt(ARRIVO_RIGIDITA) * (USCITA_MS / 1000f)

/** Quanto ha fatto la molla allo scadere di [USCITA_MS]: 0,99. Vedi [MOLLA]. */
private val CODA = 1f - (1f + TAU) * exp(-TAU)

/**
 * Il velo dietro la scheda quando la Sfocatura dietro i pannelli è spenta: 0,6, il
 * `backgroundDimAmount` del tema da cui discende `Theme.AIV`, come ogni altro dialogo. Si imposta e
 * non si somma (`setDimAmount`).
 *
 * ⚠️ **Si scrive anche se oggi la finestra ha già un velo di sistema**: nelle risorse di
 * `compose-ui` 1.12.0 la finestra prende `DialogWindowTheme`, senza `backgroundDimEnabled`, o
 * `FloatingDialogWindowTheme`, che lo dichiara, e lo decide `decorFitsSystemWindows`. Il velo di
 * sistema è un effetto collaterale di quel parametro, e cambiandolo sparirebbe.
 */
private const val SHEET_DIM = 0.6f

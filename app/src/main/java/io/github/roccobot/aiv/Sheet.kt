package io.github.roccobot.aiv

import android.graphics.Color
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Una **bottomsheet**: una scheda appoggiata al bordo di sotto, larga quanto lo schermo, con
 * un titolo in cima, un contenuto che scorre e un piede fermo.
 *
 * ⚠️⚠️ **ENTRA SALENDO DAL BASSO, DALLA 1.43, e fino alla 1.42 era vietato** (istruzione
 * dell'utente, 2026-09-03: *ho visto che una bottomsheet che non appare dal basso è troppo
 * strana. Facciamo che arriva dal basso con un'animazione fluida ed entra decelerando, ma al
 * tempo stesso c'è una mini dissolvenza*). Il divieto veniva dal giro della `1.37` (*info
 * dettagliate come bottomsheet, ma che non deve entrare scorrendo da sotto*) ed era una
 * richiesta vera, provata: quello che l'ha ribaltata è averla vista in mano.
 * ⚠️⚠️ **LA SALITA È NOSTRA E NON QUELLA DELLA FINESTRA, e la differenza va saputa perché
 * sono due movimenti che si sommerebbero**: l'animazione della finestra resta **spenta**
 * (`setWindowAnimations(0)` in [sheetWindow]), e la scheda sale per conto suo con la molla di
 * [ARRIVO_RIGIDITA]. Riaccendere quella di sistema adesso darebbe due salite sovrapposte, una
 * col nostro tempo e una col tempo del telefono.
 * ⚠️⚠️ **LA CRESCITA DA 0,96 È USCITA, e non è una dimenticanza**: era il *sostituto* della
 * salita, quando la salita non si poteva fare. Tenuta insieme a lei, una superficie larga
 * quanto lo schermo e ancorata al bordo si stringerebbe ai fianchi mentre arriva, cioè un
 * secondo movimento che contraddice il primo: 'arriva dal basso' si legge se il solo asse che
 * si muove è quello verticale.
 * ⚠️ **La dissolvenza è 'mini' come l'ha chiesta**, cioè finisce presto: a [SHEET_FADE_MS] la
 * scheda è già opaca e la molla è appena oltre metà strada, quindi il resto del viaggio lo fa
 * da corpo solido. È quello che distingue un accenno da un fantasma che sale.
 * ⚠️ **Perciò non è una `ModalBottomSheet`**: quella si trascina e si prende tutti i tocchi,
 * e sono le due cose che qui non si vogliono. Che salga adesso non la rende quella: la salita
 * era la sola delle sue proprietà che questa scheda non aveva.
 *
 * ⚠️⚠️ **IL PIEDE È FERMO E IL CORPO SCORRE SOTTO DI LUI** (*la pillola con il nome è in fondo,
 * verso il bordo inferiore, ancorata: il resto rimane sotto allo scorrimento*): il piede sta
 * fuori dalla colonna che scorre e porta il fondo della scheda, quindi il contenuto gli
 * sparisce dietro invece di scorrergli sopra.
 * ⚠️ **Il corpo prende il posto che avanza e non di più** (`weight(1f, fill = false)`): con
 * poche righe la scheda è bassa, con molte cresce fino allo schermo e da lì scorre. Una
 * altezza fissa sarebbe giusta su un telefono solo.
 * ⚠️⚠️ **LO SCORRIMENTO È DI QUESTA SCHEDA, E IL CONTENUTO NON DEVE AVERNE UN ALTRO: due
 * annidati fanno CRASHARE l'app** (riscontro dell'utente, 2026-09-03, sulla 1.38: *va
 * sistematicamente in crash*). Quello di fuori misura il contenuto con altezza **infinita**, e
 * `ScrollNode.measure` chiama `checkScrollableContainerConstraints`, che su un'altezza infinita
 * va in errore. Il caso vero era `FileFacts`, che si portava dietro il proprio scorrimento da quando
 * viveva in un `AlertDialog`: la nota per esteso sta là.
 *
 * ⚠️⚠️ **SI CHIUDE TOCCANDO FUORI, e quel tocco lo raccogliamo NOI**: con
 * `usePlatformDefaultWidth = false` la finestra copre lo schermo, quindi per il sistema non
 * esiste più un 'fuori' da riconoscere. Il velo sopra la scheda è una superficie trasparente
 * che chiude, ed è deterministico invece di dipendere da come Compose misura il contenuto.
 * ⚠️ **Il gesto Indietro chiude sempre**, come in ogni dialogo, e la crocetta in testa è la
 * via visibile per chi non conosce nessuna delle due.
 */
@Composable
fun Sheet(
    title: String,
    onDismiss: () -> Unit,
    foot: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    /*
     * ⚠️⚠️ **LA SCHEDA SI CHIUDE IN DUE TEMPI, dalla 1.44, e senza questo non ci sarebbe
     * nessuna uscita da vedere** (istruzione dell'utente, 2026-09-03: *le bottomsheet devono
     * sparire nello stesso modo in cui entrano, ma con animazione speculare*). Il congedo di un
     * dialogo è immediato: chiamato `onDismiss`, la finestra non c'è più e qualunque animazione
     * non ha nessuno da animare. Quindi `chiudi` **non** congeda: spegne [visibile], che avvia
     * la discesa, e il congedo vero lo fa il `finishedListener` quando il movimento è a zero.
     * ⚠️ **I tre modi di chiudere passano tutti da qui** (il tocco sopra la scheda, la
     * crocetta, il gesto Indietro): uno che chiamasse `onDismiss` dritto salterebbe l'uscita, e
     * si vedrebbe come una scheda che a volte scende e a volte sparisce.
     * ⚠️ **Parte da `false` e si accende all'ingresso in scena**: è la stessa riga che faceva
     * partire la salita, e adesso serve a due animazioni invece che a una.
     */
    var visibile by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visibile = true }
    val chiudi: () -> Unit = { visibile = false }

    Dialog(
        onDismissRequest = chiudi,
        /*
         * ⚠️⚠️ **`decorFitsSystemWindows = false` È QUELLO CHE PORTA LA SCHEDA SOTTO LA BARRA
         * DI SISTEMA** (richiesta dell'utente, 2026-09-03: *fa' in modo che la barra
         * multi-attività in basso assuma lo stesso colore dello sfondo, altrimenti l'effetto è
         * tremendo, se sommato alla stondatura fisica del display*). Di serie la finestra di un
         * dialogo si ferma **sopra** quella barra, quindi là sotto si vedeva la pagina, di un
         * colore diverso dalla scheda, e le due strisce si leggevano come due pannelli.
         * ⚠️ **Il contenuto non si muove di un pixel**: il rientro che la finestra applicava da
         * sé adesso lo applica la colonna qui dentro ([navigationBarsPadding]), quindi a
         * cambiare è il solo **fondo**, che arriva fino al bordo. Vedi la nota là.
         */
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        sheetWindow()
        WindowVeil(bare = SHEET_DIM)

        /*
         * ⚠️⚠️ **DUE ANIMAZIONI E NON UNA, e sono due perché durano tempi diversi**: la
         * salita è una molla che frena ([ARRIVO_RIGIDITA]), la dissolvenza dura
         * [SHEET_FADE_MS] e basta. Una sola con due letture (l'opacità ricavata dalla
         * posizione) legherebbe la 'mini dissolvenza' alla curva della salita, cioè la
         * farebbe rallentare insieme a lei proprio in fondo, dov'è già finita.
         * ⚠️⚠️ **E OGNUNA HA DUE VERSI, dalla 1.44**: all'andata la molla e la dissolvenza
         * corta, al ritorno la curva che accelera ([fuga]) e la stessa dissolvenza **in
         * coda** ([sfumaVia]). Il perché quelle due e non altre sta sulle costanti: sono la
         * prima letta all'indietro e la seconda spostata dall'inizio alla fine, cioè
         * l'animazione speculare che l'utente ha chiesto.
         * ⚠️ **Il congedo vero sta nel `finishedListener` della SALITA e non della
         * dissolvenza**: la posizione è quella che dura più a lungo, quindi è l'unica che
         * finisce quando la scheda è davvero fuori. Legato all'altra, la finestra si
         * chiuderebbe con la scheda ancora a metà strada, invisibile ma non arrivata.
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
                 * ⚠️⚠️ **IL RIENTRO IN ALTO STA QUI, ed è la contropartita di
                 * `decorFitsSystemWindows = false`**: senza la finestra a fitto sistema, una
                 * scheda con molte righe crescerebbe fino al bordo di sopra e il titolo
                 * finirebbe sotto l'orologio. Applicato al riquadro che la contiene, diventa
                 * un **tetto** invece di uno spazio: una scheda corta non si muove di niente,
                 * e una lunga si ferma sotto la barra di stato.
                 * ⚠️ **Solo in alto**: in fondo il riquadro deve arrivare al bordo, o la
                 * scheda tornerebbe a fermarsi sopra la barra di sistema, che è il difetto
                 * che questa versione toglie.
                 */
                .statusBarsPadding()
                /*
                 * ⚠️ **Niente increspatura e nessuna descrizione**: questo non è un tasto, è
                 * lo spazio vuoto sopra la scheda, e un cerchio d'inchiostro che si apre dove
                 * si tocca per chiudere direbbe che là c'era qualcosa da premere.
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
                    /*
                     * ⚠️⚠️ **LA DISTANZA È L'ALTEZZA DELLA SCHEDA STESSA, letta qui dentro**:
                     * `size` è la misura di questo strato, quindi la scheda parte esattamente
                     * da sotto il proprio bordo inferiore e arriva a posto, qualunque sia la
                     * sua altezza. Una distanza in `dp` sarebbe giusta su una scheda sola:
                     * quella delle informazioni cresce col numero di righe, e con un numero
                     * fisso una scheda alta partirebbe già mezza dentro e una bassa da fuori
                     * schermo.
                     * ⚠️ **Non serve nessun `clip`**: quello che sta sotto il bordo del
                     * riquadro è fuori dalla finestra, cioè fuori dallo schermo, e il sistema
                     * non lo disegna. Un `clipToBounds` sul riquadro taglierebbe invece anche
                     * il velo, che deve coprire tutto.
                     */
                    .graphicsLayer {
                        alpha = show
                        translationY = (1f - lift) * size.height
                    }
                    /*
                     * ⚠️ **Il tocco sulla scheda NON deve chiudere**, e senza questa riga lo
                     * farebbe: il velo di sopra è un genitore, e un tocco che nessuno consuma
                     * gli arriverebbe. Un `clickable` senza effetto è il modo di dire 'qui mi
                     * fermo' senza inventare un gesto.
                     */
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { }
                    ),
                shape = RoundedCornerShape(topStart = SHEET_ROUND, topEnd = SHEET_ROUND),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(
                    /*
                     * ⚠️ **Il rientro di sistema sta QUI e non sulla scheda**: il fondo della
                     * scheda deve arrivare al bordo dello schermo (vedi la nota sul dialogo),
                     * il contenuto no, o l'ultima riga finirebbe sotto la barra. Applicandolo
                     * al contenuto, quello che si vede resta dov'era e cambia il solo colore
                     * della striscia in fondo.
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
                         * ⚠️⚠️ **IL TITOLO È ALLINEATO A SINISTRA DALLA 1.40, e prima era
                         * centrato** (richiesta dell'utente, 2026-09-03: *'Info dettagliate sul
                         * file' -> 'Informazioni' (allineato a sinistra)*). Il centro era una
                         * decisione della `0.73` presa su un suo mockup, quando la pastiglia
                         * del nome stava sulla riga sotto e faceva da contrappeso; scesa la
                         * pastiglia in fondo, quella ragione era caduta e restava la sola
                         * abitudine.
                         * ⚠️ **Con lui se n'è andato lo spazio speculare alla crocetta**, che
                         * serviva solo a fare del centro il centro vero della scheda: un titolo
                         * a sinistra comincia dal rientro, come ogni altro testo qui dentro.
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
                                imageVector = Icons.Outlined.Close,
                                // ⚠️ Descritta col nome che il tasto aveva quando era una
                                // parola: chi la sente leggere sente 'Chiudi', come prima.
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
 * ⚠️⚠️ **`setWindowAnimations(0)` È LA RIGA CHE CHIUDE UNA QUESTIONE APERTA DALLA 1.25**: là
 * si era concluso che l'animazione di entrata la decide il telefono e che *da dentro non si
 * spegne*. Si spegne: uno stile di animazione a zero vuol dire nessuna animazione, e senza di
 * lei l'unico movimento è quello scritto in [Sheet].
 * ⚠️⚠️ **E SERVE ANCORA ADESSO CHE LA SALITA È TORNATA, per una ragione OPPOSTA a quella per
 * cui era nata**: fino alla `1.42` spegneva un movimento vietato; dalla `1.43` evita che i
 * movimenti siano **due**. L'animazione di serie di una finestra appoggiata in basso è proprio
 * una salita, quindi accesa si sommerebbe alla nostra: due corse sulla stessa distanza, una
 * col nostro tempo e una col tempo che decide il telefono. Chi togliesse
 * questa riga pensando che adesso non serva più otterrebbe un sussulto.
 * ⚠️ **La finestra si stende su tutto** e la scheda si appoggia in basso da sé, dentro: così
 * lo spazio sopra esiste, è nostro, e può raccogliere il tocco che chiude.
 * ⚠️ **`Gravity.BOTTOM` con una finestra a tutto schermo non sposta niente**, e si scrive lo
 * stesso: dice qual è il bordo di riferimento se un domani la finestra tornasse alta quanto il
 * contenuto.
 */
@Composable
private fun sheetWindow() {
    val view = LocalView.current
    DisposableEffect(view) {
        // ⚠️ La stessa risalita del velo, e non un secondo modo di trovare la finestra:
        // vedi `dialogWindow` in `Veil.kt`.
        val window = view.dialogWindow()
        window?.apply {
            setWindowAnimations(0)
            setGravity(Gravity.BOTTOM)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // ⚠️ Lo sfondo della finestra deve essere trasparente, o dietro la scheda si
            // vedrebbe il rettangolo di serie del dialogo al posto del velo.
            setBackgroundDrawableResource(android.R.color.transparent)
            edgeToEdge()
        }
        onDispose { }
    }
}

/**
 * Le tre cose che servono a una finestra di dialogo per **dipingere da sé** sotto le barre di
 * sistema, cioè quello che `enableEdgeToEdge()` fa all'attività e a nessun'altra finestra.
 *
 * ⚠️⚠️ **ESISTE PERCHÉ LA 1.40 NON HA FUNZIONATO, e la causa è la riga sopra a questa**
 * (riscontro `sotto-barra` della `1.41`: *fallo dappertutto, ma anche dove l'avevi già fatto,
 * perché non ha funzionato*). Misurato sul sorgente di `compose-ui` 1.12.0, non ragionato:
 * `DialogLayout.internalOnMeasure` aggiunge `FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS` **solo** se
 * `window.attributes.height == WRAP_CONTENT`, e questa scheda l'altezza della finestra la mette
 * a `MATCH_PARENT` da sé (le serve per avere lo spazio sopra che raccoglie il tocco che chiude).
 * Quindi `decorFitsSystemWindows = false` stendeva sì la finestra fino al bordo, ma senza quel
 * flag il sistema continuava a dipingersi la sua barra opaca sopra: il fondo della scheda
 * arrivava là sotto e non si vedeva.
 * ⚠️⚠️ **E DA ANDROID 10 NON BASTA IL FLAG: c'è un velo che il sistema aggiunge da sé**
 * (`isNavigationBarContrastEnforced`, vero di fabbrica), che su una barra trasparente in
 * navigazione a gesti dipinge una patina traslucida. `enableEdgeToEdge()` lo spegne
 * sull'attività, e su una finestra nuova torna acceso: senza questa riga la striscia in fondo
 * sarebbe **quasi** del colore della scheda, che è il modo peggiore di sbagliare, perché
 * sembra un difetto di tinta e non un velo.
 * ⚠️ **I due colori si dichiarano benché siano già nel tema**: `Theme.AIV` li ha trasparenti,
 * ma il dialogo vive su una finestra sua, e un tema che un domani non li portasse più li
 * rimetterebbe opachi qui senza che nessuno colleghi le due cose.
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

/**
 * Lo stondamento dei due angoli in alto.
 *
 * ⚠️ **28dp, che è quello delle bottomsheet**, e le regole di casa lo dicono già: i menu
 * stanno a 20 apposta per non confondersi con loro (vedi `MENU_ROUND`). Una scheda appoggiata
 * in basso è una bottomsheet e prende il numero delle bottomsheet.
 */
private val SHEET_ROUND = 28.dp

/** Il rientro attorno al contenuto, e l'aria fra una riga e l'altra. */
private val SHEET_PAD = 24.dp
private val SHEET_GAP = 12.dp

/**
 * Il rientro in ALTO, che è la metà degli altri.
 *
 * ⚠️ **12 e non 24, dalla 1.40** (richiesta dell'utente, 2026-09-03: *leggermente meno spazio
 * in alto (oltre la posizione verticale della × di chiusura)*). Il numero da guardare non è
 * questo ma quello che si vede: la crocetta vive in un bersaglio da [SHEET_SHUT], quindi sopra
 * il suo disegno c'erano 24 di rientro più 12 di bersaglio, cioè **36**; adesso 24. Chi
 * ritoccasse questo valore ragionando sul solo rientro sbaglierebbe di dodici punti.
 * ⚠️ **Solo in alto**: ai lati e in fondo il rientro è quello di prima, e una scheda che si
 * stringe da tutte le parti sarebbe un'altra richiesta.
 */
private val SHEET_PAD_TOP = 12.dp

/**
 * Il lato della crocetta, che è anche lo spazio speculare a sinistra del titolo.
 *
 * ⚠️ **48dp e non 24**: è l'area toccabile minima di Material, non la misura del disegno. Un
 * numero solo per i due lati, o il titolo sarebbe centrato di sbieco.
 */
private val SHEET_SHUT = 48.dp

/**
 * Come arriva una scheda dal basso: una **molla** critica, cioè che frena e non rimbalza.
 *
 * ⚠️⚠️ **È `internal` PERCHÉ LE BOTTOMSHEET SONO DUE**: questa e quella della selezione
 * (`PickSheet`). Non sono la stessa cosa (l'altra vive dentro la schermata, non in una
 * finestra, e ha anche un'uscita), ma *arrivare dal basso* è un gesto solo, e due definizioni
 * dello stesso gesto divergono al primo ritocco: è il difetto che `InfoBar.kt` è nato per
 * chiudere.
 *
 * ⚠️⚠️ **UNA MOLLA E NON UNA CURVA, ed è una MISURA che ha scartato la prima scelta.** Avevo
 * preso l'`emphasized decelerate` di Material 3 (`cubic-bezier(0.05, 0.7, 0.1, 1.0)`) su 320
 * ms, perché è la curva che le linee guida prescrivono a un elemento che entra in scena.
 * Valutandola, quella curva su quel tempo fa **l'83% della strada nei primi 80 ms** e passa
 * gli altri 240 a percorrere il 17% che resta: a schermo è uno scatto seguito da niente, cioè
 * il contrario dell'arrivo fluido che l'utente ha chiesto. La molla qui sotto, agli stessi 80
 * ms, ha ancora da fare il **52%**, ed entra nell'ultimo 1% a **332 ms**: il movimento è
 * distribuito su tutto il tempo.
 * ⚠️ **Il numero scartato resta scritto** perché la curva è quella che qualunque documento di
 * Material consiglia: chi ci ricapita la prende in buona fede, e la misura è la sola cosa che
 * la ferma.
 * ⚠️⚠️ **E NON SONO I 170 DEI MENU**: quel numero l'utente lo ha scelto su un mockup per un
 * **menu**, che compare dov'era il dito, cioè non viaggia. Una scheda percorre la propria
 * altezza, qualche centinaio di punti, e la stessa durata su quella distanza si legge come uno
 * scatto. I menu tengono i loro 170, e non si allineano a questo: là il numero è giusto, ed è
 * di chi lo ha scelto.
 * ⚠️ **`StiffnessMediumLow` è anche la molla di fabbrica di `slideInVertically`**, cioè quella
 * che la scheda della selezione ha da sempre e su cui non è mai arrivato un riscontro: la
 * scelta è di allineare la scheda nuova a un movimento già provato, invece di inventarne uno.
 */
internal const val ARRIVO_RIGIDITA = Spring.StiffnessMediumLow

/** La molla di sopra, per un numero da 0 a 1. Vedi [ARRIVO_RIGIDITA]. */
internal fun arrivo(): SpringSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = ARRIVO_RIGIDITA
)

/**
 * Quanto dura la dissolvenza.
 *
 * ⚠️ **'Mini' sono parole sue**, quindi finisce presto: a 110 ms la scheda è già opaca, e la
 * molla a quel punto ha fatto poco più della metà della strada. Il resto del viaggio lo fa da
 * corpo solido, che è quello che distingue un accenno di dissolvenza da un fantasma che sale.
 * ⚠️ **Lineare e non con la curva della salita**: legata alla molla rallenterebbe insieme a
 * lei proprio in fondo, dove ha già finito.
 */
internal const val SHEET_FADE_MS = 110

// ── L'uscita, che è l'entrata letta all'indietro ────────────────────────────
/*
 * ⚠️⚠️ **'SPECULARE' QUI È PRESO ALLA LETTERA, e per questo non c'è nessuna curva nuova da
 * scegliere** (istruzione dell'utente, 2026-09-03: *le bottomsheet devono sparire nello stesso
 * modo in cui entrano, ma con animazione speculare, accelerazione e scorrimento giù + breve
 * dissolvenza*). La strada ovvia sarebbe stata l'`emphasized accelerate` di Material, cioè
 * un'altra curva con altri quattro numeri, che non è la specularità: è un'altra animazione che
 * accelera. Qui l'uscita è **la stessa molla percorsa dalla fine all'inizio**, quindi accelera
 * per costruzione e i due movimenti sono l'uno l'immagine dell'altro.
 * ⚠️ **Costa una formula chiusa e nessuna misura da tarare**: la molla critica ha una
 * posizione scrivibile in una riga, e [MOLLA] la scrive.
 */

/**
 * Quanto dura l'uscita: **il tempo in cui la molla dell'entrata si posa**.
 *
 * ⚠️ **332 non è un numero scelto ma misurato**: è l'istante in cui la molla di
 * [ARRIVO_RIGIDITA] entra nel suo ultimo 1%, cioè quando l'entrata è finita. Prenderne uno
 * diverso renderebbe le due animazioni due durate diverse, e la specularità si perderebbe nel
 * punto in cui si nota di più, cioè aprendo e chiudendo di seguito.
 */
internal const val USCITA_MS = 332

/**
 * La curva dell'uscita: **[MOLLA] letta all'indietro**, quindi accelera.
 *
 * A che velocità va, misurato, per non doverlo ricavare da capo: a un decimo del tempo ha
 * fatto lo **0,8%** della strada, a un quarto il **3,1%**, a metà il **14,8%**, e nell'ultimo
 * quarto percorre la metà di tutto. È il rovescio esatto dell'entrata, che agli stessi istanti
 * sta al **14,5%**, al **49,9%** e all'**85,2%**.
 */
internal val ACCELERA: Easing = Easing { f -> 1f - MOLLA(1f - f) }

/** L'uscita per un numero da 0 a 1. Vedi [ACCELERA]. */
internal fun fuga(): TweenSpec<Float> = tween(durationMillis = USCITA_MS, easing = ACCELERA)

/**
 * La dissolvenza dell'uscita: la stessa di [SHEET_FADE_MS], ma **in coda invece che in testa**.
 *
 * ⚠️ **È l'altra metà della specularità**: all'andata la dissolvenza sta nei primi 110 ms,
 * quindi al ritorno sta negli ultimi. Senza il ritardo la scheda diventerebbe trasparente
 * subito e scenderebbe da fantasma, che è proprio l'effetto che sull'entrata si è evitato.
 */
internal fun sfumaVia(): TweenSpec<Float> =
    tween(durationMillis = SHEET_FADE_MS, delayMillis = USCITA_MS - SHEET_FADE_MS)

/**
 * La posizione della molla critica di [ARRIVO_RIGIDITA], normalizzata a 0..1 su [USCITA_MS].
 *
 * ⚠️ **La formula è quella di uno smorzamento critico**, `1 - (1 + wt)e^(-wt)`, con `w` la
 * radice della rigidità: qui non c'è niente di scelto a occhio, e chi cambia
 * [ARRIVO_RIGIDITA] deve cambiare [TAU] con lei o le due animazioni smettono di essere
 * specchiate.
 * ⚠️ **La divisione per [CODA] serve perché a `USCITA_MS` la molla è al 99% e non al 100%**:
 * senza, l'uscita partirebbe da un gradino dell'1%, che su una scheda alta sono qualche pixel
 * di salto al primo fotogramma.
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
 * Il velo che resta dietro la scheda quando la funzione della `1.39` è spenta.
 *
 * ⚠️ **0,6 è quello di Android**, il valore di `backgroundDimAmount` del tema da cui discende
 * `Theme.AIV`: così a funzione spenta la scheda è velata come ogni altro dialogo dell'app,
 * invece di essere velata a modo suo. ⚠️ Si **imposta** e non si somma (`setDimAmount`), quindi
 * scritto due volte resta 0,6.
 *
 * ⚠️⚠️ **NASCE PERCHÉ NELLA 1.39 LA SCHEDA NON AVEVA VELO, e dalla 1.40 CE L'HA: si scrive lo
 * stesso, e la ragione è che quel velo non ce l'ha per una scelta nostra.** Misurato nelle
 * risorse di `compose-ui` 1.12.0: la finestra di un dialogo prende `DialogWindowTheme`, che
 * **non** dichiara `backgroundDimEnabled`, oppure `FloatingDialogWindowTheme`, che lo dichiara,
 * e quale dei due lo decide `decorFitsSystemWindows`. Questa scheda ha cambiato ramo nella
 * `1.40` per un'altra richiesta (arrivare sotto la barra di sistema), cioè si è ritrovata un
 * velo di sistema come effetto collaterale. Chi togliesse questa riga fidandosi di quello si
 * consegnerebbe alla prossima richiesta che sposta quel parametro.
 */
private const val SHEET_DIM = 0.6f

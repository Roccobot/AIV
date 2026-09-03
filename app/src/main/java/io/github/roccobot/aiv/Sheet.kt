package io.github.roccobot.aiv

import android.graphics.Color
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
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

/**
 * Una **bottomsheet**: una scheda appoggiata al bordo di sotto, larga quanto lo schermo, con
 * un titolo in cima, un contenuto che scorre e un piede fermo.
 *
 * ⚠️⚠️ **NON ENTRA SCIVOLANDO DAL BASSO, ed è la richiesta che la definisce** (utente, giro
 * della 1.37: *info dettagliate come bottomsheet (ma che non deve entrare scorrendo da sotto).
 * Appare con animazione semplice e veloce*). Una bottomsheet di Material sale dal fondo per
 * costruzione, e una finestra di sistema porta comunque la sua animazione di entrata: qui
 * l'animazione della finestra si **spegne** ([Gravity] e `setWindowAnimations(0)` in
 * [sheetWindow]) e al suo posto resta la dissolvenza con la crescita da [SHEET_SMALL], che
 * sono i numeri già scelti da lui per i menu.
 * ⚠️ **Perciò non è una `ModalBottomSheet`**: quella è fatta per salire e per trascinarsi, e
 * spegnere le due cose che la definiscono vorrebbe dire tenerla per il colore dello sfondo.
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
    Dialog(
        onDismissRequest = onDismiss,
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

        var grown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { grown = true }
        val show by animateFloatAsState(
            targetValue = if (grown) 1f else 0f,
            animationSpec = tween(durationMillis = SHEET_IN),
            label = "sheet"
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
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = show
                        val k = SHEET_SMALL + (1f - SHEET_SMALL) * show
                        scaleX = k
                        scaleY = k
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
                        IconButton(onClick = onDismiss, modifier = Modifier.size(SHEET_SHUT)) {
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
 * lei l'unico movimento è quello scritto in [Sheet]. Su una scheda appoggiata in basso non è
 * un dettaglio, perché l'animazione di serie sarebbe **proprio** la salita dal fondo che lui
 * ha vietato.
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
 * Come arriva: da quanto piccola cresce, e in quanto tempo.
 *
 * ⚠️ **Sono i numeri dei menu**, scelti dall'utente su un mockup e già in vigore in
 * `MenuShell`: 'animazione semplice e veloce' in questa app è già stata definita una volta, e
 * definirla una seconda con altri due numeri farebbe due velocità per la stessa idea.
 */
private const val SHEET_SMALL = 0.96f
private const val SHEET_IN = 170

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

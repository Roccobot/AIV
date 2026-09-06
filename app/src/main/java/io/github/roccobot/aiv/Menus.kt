package io.github.roccobot.aiv

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

/**
 * La superficie di **ogni** menu dell'app: un riquadro che cresce, dove dice il posizionatore.
 *
 * ⚠️⚠️ **UNA SOLA, DALLA `1.46`, E PRIMA ERANO DUE MECCANISMI PER CINQUE MENU** (richiesta
 * dell'utente, 2026-09-03: *non è per il gusto dell'uniformità: è per avere un sistema
 * affidabile. Se volessi reintrodurre un elemento decorativo come la vecchia linea color
 * accento, basterebbe un unico ragionamento per tutti gli elementi*). Tre menu passavano di
 * qui e due erano `DropdownMenu` di Material, e ogni cosa che riguarda 'tutti i menu' andava
 * scritta due volte.
 * - **La prima prova che costa** è la `1.28`, che ha unificato lo stondamento di 'tutti i
 *   menu' e ha dimenticato quello della schermata iniziale, perché era il solo a non passare
 *   di qui. Il difetto è uscito otto versioni dopo, e lo ha visto l'utente prima di noi.
 * - **La seconda** è la striscia d'accento, abbandonata nella `1.38` non per lo spessore ma
 *   perché *ne esistono almeno 3 versioni diverse*: di una cosa sola esistevano tre rese
 *   perché di posti in cui disegnarla ce n'erano tre.
 * - **La terza è della `1.46` stessa**: il filtro della testata della griglia era l'unica
 *   superficie senza velo, e a nasconderlo era una frase che dava un altro menu per l'unico
 *   fuori da qui. Finché le superfici sono cinque, la difesa è un elenco che qualcuno deve
 *   tenere vero; adesso la difesa è che l'alternativa non esiste più nel progetto.
 *
 * ⚠️⚠️ **`Popup` E NON `DropdownMenu`, ed è quello che permette di scegliere il posto**: un
 * `DropdownMenu` si posiziona **contro il proprio genitore** e non accetta un posizionatore,
 * quindi con lui non si potevano scrivere né 'al centro della finestra' né 'sopra il tastino'.
 * ⚠️ **Il conto delle finestre non cambia**, e chi cercasse qui un guadagno di prestazioni non
 * lo trova: un `DropdownMenu` di Material **è** un `Popup`.
 *
 * ⚠️ **IL CONTENUTO RESTA LIBERO, ed è la superficie a essere una e non la disposizione**:
 * dentro ci vanno voci ([MenuRow]), un riquadro di tasti (`ActionPad`) o una fila di tasti,
 * come nel filtro della testata. Quelli sono contenuto di un menu, non un secondo modo di fare
 * un menu.
 *
 * ⚠️⚠️ **STA IN UN FILE A SÉ dalla 0.75, da quando i menu non sono più uno**: il secondo
 * avrebbe copiato la superficie, l'ombra e i numeri dell'animazione, e un'animazione scritta
 * in due posti diverge al primo ritocco. Quei numeri li ha scelti l'utente su un mockup,
 * quindi valgono per i menu, non per uno.
 * ⚠️⚠️ **DALLA 1.28 IL RAGGIO NON È PIÙ UN PARAMETRO, ed è la correzione di un difetto vero**
 * (richiesta dell'utente, 2026-09-02: *va uniformato TUTTO*). Fino alla `1.27` ogni menu
 * portava il suo numero, e i numeri erano **tre**: 8 nel visualizzatore, 8 nel navigatore, 16
 * nella selezione. Erano nati come 'dipende dalla forma del contenuto', che è una ragione
 * plausibile e sbagliata: uno stondamento dice **che cosa è** quella superficie, non quanto è
 * larga, e tre valori diversi dicevano che erano tre cose diverse. Adesso è [MENU_ROUND], uno
 * solo, e non si può più far divergere passandogli un numero.
 *
 * ⚠️⚠️ **LA STRISCIA D'ACCENTO NON C'È PIÙ, DALLA 1.38, ED È L'UTENTE A RINUNCIARCI**
 * (riscontro `striscia-sotto`, 2026-09-02: *Togli il bordino dappertutto: rinuncio e cercherò
 * un altro sistema per veicolare l'identità*). Era nata nella `1.29` da un suo mockup e ha
 * attraversato cinque versioni: pastiglia da 5dp, forma rifatta sul mockup a 6, spessore a 7,
 * poi anche sui sotto-menu e a una distanza sola dal fondo.
 * ⚠️⚠️ **E CON LEI ESCE `SubPanel`, il guscio che le serviva** (stessa richiesta: *torna anche
 * a disattivare i sistemi ad-hoc per il disegno di finestre e pannelli, salvo dove serve per
 * altri motivi*). Quel guscio esisteva perché un `AlertDialog` non lascia disegnare in fondo
 * alla propria superficie: senza striscia da disegnare, il pannellino del tocco lungo su
 * 'Info' è tornato a essere un `AlertDialog` di Material.
 * ⚠️ **Questa superficie invece RESTA, e non è un sistema ad-hoc dello stesso genere**: nasce
 * nella `0.75` per **posizionare** un menu dove Material non sa metterlo, che è un lavoro che
 * nessun componente di libreria fa. Chi ripassasse a togliere 'i gusci nostri' si fermi qui.
 */
@Composable
fun MenuShell(
    state: MenuState,
    /** Dove va il menu: [MenuInWindow] oppure un [rememberMenuSpot]. */
    position: PopupPositionProvider,
    content: @Composable () -> Unit
) {
    /*
     * ⚠️⚠️ **UN AVANZAMENTO SOLO PER PANNELLO, SFOCATURA E PATINA, DALLA `1.67`, E PRIMA ERANO
     * DUE** (riscontro del giro della `1.66`: *il menu se ne va troppo piano, la transizione
     * s'inceppa e rimane bloccato a schermo a mezza opacità per un tempo che lo rende visibile,
     * poi l'animazione finisce a scatti ... Semplifica al massimo e fai sparire il menu in modo
     * fluido e senza glitch, con una dissolvenza veloce*). La `1.61` aveva staccato la patina dal
     * pannello per darle una coda lunga, e la `1.65` aveva allungato l'uscita del pannello per
     * accompagnarla: con un'uscita di [MENU_OUT_MS] non c'è più nessuna coda da accompagnare,
     * quindi i due numeri tornano a essere uno.
     * ⚠️ **La regola della `1.65` NON è caduta, ed è quella che tiene in piedi questa forma**: una
     * sfocatura che cala vuole qualcosa in scena che se ne stia andando, e qui il pannello se ne va
     * **con** lei e per lo stesso tempo. Il fatto per esteso sta in fondo a `Veil.kt`.
     * ⚠️ **E riaprendo a metà uscita non c'è nessun salto**: `animateTo` riparte dal valore
     * corrente, quindi un pannello sbiadito a mezza strada risale da lì.
     *
     * ⚠️⚠️ **LA PRIMA COMPOSIZIONE NON ANIMA, DALLA `1.76`, E SENZA QUESTA RIGA OGNI SCHERMATA
     * CHE ARRIVA APRIVA UN MENU VUOTO PER SEI FOTOGRAMMI.** Un `LaunchedEffect` parte anche alla
     * prima composizione, e là `wanted` è falso e il valore è già zero: `animateTo` non muoveva
     * un pixel, ma teneva l'animazione **in corsa** per [MENU_OUT_MS]. Da lì [MenuState.visible]
     * diceva 'menu in scena', quindi nascevano una finestra di popup vuota, il cancello dei
     * tocchi di [MenuGuard] e il distacco del FAB nella sua finestra.
     * ⚠️ **È la stessa guardia che `TapHoldFab` ha da sempre**, con la sua stessa ragione scritta
     * accanto: chi anima dentro un `LaunchedEffect` senza chiave deve chiedersi che cosa fa al
     * primo giro. ⚠️ **Il confronto è un'uguaglianza esatta e regge**: `animateTo` chiude
     * assegnando il valore d'arrivo, quindi a corsa finita i due numeri sono lo stesso.
     */
    LaunchedEffect(state.wanted) {
        val meta = if (state.wanted) 1f else 0f
        if (state.show.value == meta) return@LaunchedEffect
        state.show.animateTo(
            targetValue = meta,
            animationSpec = tween(
                durationMillis = if (state.wanted) MENU_IN else MENU_OUT_MS,
                easing = if (state.wanted) MENU_EASE else MENU_OUT
            )
        )
    }

    /*
     * ⚠️⚠️ **QUI FUORI E NON DENTRO IL `Popup`**: la patina la dipinge l'app, e chiesta da qui la
     * richiesta non dipende dalla vita della finestra. ⚠️ **Dalla `1.67` non le serve più per
     * sopravviverle**, perché l'uscita è una dissolvenza sola: resta fuori perché così a spegnerla
     * è la composizione della **schermata**, che è quella che se ne va quando si naviga altrove,
     * ed è il caso in cui il menu spariva lasciando addosso all'app il proprio scuro.
     */
    AppPatina { state.show.value }

    /*
     * ⚠️⚠️ **SI DICHIARA IN SCENA, E QUESTO TOGLIE IL TOCCO CHE ARRIVAVA ANCHE SOTTO** (difetto
     * trovato dal censimento della UI del 2026-09-05, e confermato dal bytecode di Compose:
     * `createFlags` di `AndroidPopup_androidKt` parte da `FLAG_WATCH_OUTSIDE_TOUCH` e non mette
     * mai `FLAG_NOT_TOUCH_MODAL`, quindi da Android 12 un tocco fuori dal menu arriva **a tutte
     * e due** le finestre: il popup lo legge come 'fuori' e si chiude, l'app lo legge come un
     * tocco suo e apre la riga o la cartella che stava sotto il dito).
     * ⚠️⚠️ **A ripararlo è UN velo solo, in `AivTheme`, e non uno per schermata**: fino alla
     * `1.69` esisteva in un posto solo su cinque chiamanti (`GridScreen`, il menu del FAB del
     * cestino), e le altre quattro schermate non ce l'avevano. Ripeterlo quattro volte avrebbe
     * lasciato in piedi il quinto modo di dimenticarsene; dichiararsi qui lo dà a ogni menu che
     * nascerà, perché [MenuShell] è la sola via per aprirne uno dalla `1.46`. Vedi [MenuGuard].
     * ⚠️ **Sta PRIMA del cancello qui sotto**, quindi vale anche mentre il pannello se ne va:
     * l'uscita dura [MENU_OUT_MS], e un tocco in quel tratto arriverebbe sotto come gli altri.
     */
    val inScena = state.visible
    DisposableEffect(state, inScena) {
        if (inScena) MenuScene.enter(state)
        onDispose { MenuScene.leave(state) }
    }

    /*
     * ⚠️⚠️ **IL CANCELLO SERVE ALL'ORDINE FRA LE FINESTRE**: a menu chiuso non deve esistere
     * nessun `Popup`, o il tastino che si stacca sopra la propria finestra non sarebbe più
     * l'ultima aggiunta e finirebbe sotto. Un popup sempre presente e trasparente in più si
     * mangerebbe i tocchi.
     */
    if (!state.visible) return

    Popup(
        /*
         * ⚠️⚠️ **LA FINESTRA SEGUE IL PANNELLO CHE CRESCE, dalla 1.53, e questa riga è metà
         * della correzione** (riscontro dell'utente, giro della `1.51`, voce
         * `sfocatura-segue`: *si vede ancora, specialmente nel cestino*). Vedi [Growing] per il
         * meccanismo: senza di lei la finestra resta grande quanto il pannello **a riposo**
         * mentre il disegno dentro è al 96%, e la sfocatura, che è un attributo della finestra,
         * copre quella fascia di 2% per lato. È la cornice che lui vede, e la `1.50` l'aveva
         * solo attenuata dosando la sfocatura.
         */
        popupPositionProvider = remember(position, state) { Growing(position) { grown(state) } },
        onDismissRequest = state::close,
        properties = PopupProperties(
            /*
             * ⚠️⚠️ **COSTANTE, E NON DEVE TORNARE A CAMBIARE DURANTE UN'ANIMAZIONE**: la `1.61`
             * lo legava a `wanted`, perché là la finestra sopravviveva al pannello e col focus si
             * sarebbe mangiata il gesto Indietro. Costava uno **sfarfallio**, e la causa è
             * misurata sul bytecode di Compose: un `PopupProperties` che cambia fa **assegnare** i
             * flag della finestra da capo, quindi in quel fotogramma sparivano la sfocatura e il
             * velo che `Veil` le aveva messo. La nota in fondo a `Veil.kt` la scrive per esteso.
             * ⚠️ **Dalla `1.64` il problema non esiste più**: la finestra muore col pannello, e
             * non c'è nessun tratto in cui tenga il focus senza avere niente in scena.
             */
            focusable = true,
            dismissOnBackPress = true,
            /*
             * ⚠️⚠️ **SEMPRE ACCESO, dalla `1.46`, e prima era un parametro con un valore
             * solo.** Nasceva nella `0.75` per il menu della selezione, dove il tastino
             * **alternava** il menu e la chiusura di fuori faceva lampeggiare; la `1.06` ha
             * tolto quell'alternanza, e da allora tutti e tre i chiamanti passavano `true`
             * mentre il KDoc descriveva per esteso un comportamento che non esisteva più. Un
             * parametro con un valore solo è un invito a farlo ridiventare due.
             */
            dismissOnClickOutside = true
        )
    ) {
        /*
         * ⚠️⚠️ **DENTRO IL `Popup` E NON FUORI, ed è tutta la differenza**: il velo si applica
         * alla finestra che **ospita** chi lo chiede, e qui fuori la finestra sarebbe quella
         * della schermata, cioè si velerebbe da sé. Vedi [WindowVeil].
         *
         * ⚠️⚠️ **SEGUE IL PANNELLO, DALLA 1.50, e la riga qui sotto è tutta la correzione**
         * (riscontro dell'utente, giro della `1.48`: *una specie di cornice sfumata si
         * materializza dove c'era/ci sarà il margine del pannello effettivo*, e nella voce
         * accanto *la sfocatura non dovrebbe sparire all'inizio della transizione: dovrebbe
         * avere essa stessa una transizione*). Le due segnalazioni sono **la stessa cosa**: la
         * sfocatura è un attributo della **finestra**, quindi copriva il rettangolo pieno del
         * popup fin dal primo fotogramma mentre il pannello dentro era ancora rimpicciolito e
         * trasparente, e quel rettangolo coi bordi sfumati è la cornice che vedeva. Adesso il
         * velo riceve [MenuState.show], cioè lo stesso numero che muove l'opacità e la scala:
         * cresce col pannello e cala con lui, e a zero si spegne del tutto.
         * ⚠️ **Il costo è dichiarato**: un `updateViewLayout` per fotogramma per la durata
         * dell'animazione, che è il prezzo della cosa chiesta. La via che lo eviterebbe (la
         * sfocatura dipinta sulla vista dell'app con un `RenderEffect`) è un rifacimento a sé.
         *
         * ⚠️⚠️ **DUE VERSIONI PRIMA FACEVA IL CONTRARIO, E SAPERLO EVITA DI TORNARCI**: la
         * `1.48` lo faceva cadere all'**inizio** dell'uscita (`if (state.wanted)`), per togliere
         * la sovrapposizione di due finestre che chiedevano la sfocatura insieme. Quella
         * diagnosi era giusta a metà: la sovrapposizione c'era, ma toglierla di netto ha lasciato
         * un buco di uno o due fotogrammi fra il velo del menu che spariva e quello del dialogo
         * che arrivava, ed è il *flash* che lui ha segnalato dopo. Con la sfocatura che cala
         * invece di sparire, le due si sovrappongono per un momento **degradando**, che è la cosa
         * che un occhio legge come una transizione invece che come un lampo.
         *
         * ⚠️⚠️ **DALLA 1.53 QUESTA RIGA NON È PIÙ LA CURA DELLA CORNICE, ed è importante non
         * crederlo**: la cornice aveva una causa geometrica (la finestra più grande del disegno)
         * e adesso quella causa non c'è più, perché la finestra si stringe col pannello (vedi
         * [Growing]). Il dosaggio resta, e serve a due altre cose: la **sfocatura**, che cresce e
         * cala col pannello invece di comparire tutta insieme, e il velo dipinto, che segue lo
         * stesso numero.
         * ⚠️⚠️ **E IL LAMPO NON SI CURA PIÙ QUI, dalla `1.54`**: era una **somma** fra due
         * finestre velate (0,45 e 0,45 fanno 0,70), e nessun ordine di chiamate lo poteva
         * togliere, perché due finestre non si aggiornano nello stesso fotogramma. Adesso il velo
         * lo dipinge l'app, uno solo, e vale il massimo delle richieste in scena: il perché per
         * esteso sta su `VeilStage`, in `Veil.kt`.
         *
         * ⚠️⚠️ **QUI RESTA LA SOLA SFOCATURA, e il livello scuro lo chiede [AppPatina] più su**:
         * sono due meccanismi diversi (un attributo di finestra e un rettangolo che l'app
         * dipinge), e la `1.64` li ha separati per questo.
         * ⚠️⚠️ **MA IL NUMERO CHE LI MUOVE È DI NUOVO UNO, DALLA `1.67`**, ed è l'avanzamento del
         * pannello: separati erano per dare alla patina una coda più lunga dell'uscita, e con
         * [MENU_OUT_MS] la coda non c'è più. Il perché sta là e in fondo a `Veil.kt`.
         */
        WindowVeil { state.show.value }
        Surface(
            /*
             * ⚠️⚠️ **CRESCE DA 0,96 E NON DA ZERO, in 170ms** (scelta dell'utente sul
             * mockup). Prima sbucava **dal punto premuto** con una scala da 0,72: un menu che
             * si gonfia da un angolo dello schermo tira l'occhio dove il dito era già, e
             * l'utente ha chiesto una cosa sobria. Da 0,96 il movimento si sente e non si
             * guarda.
             * ⚠️ L'origine è quella di serie, il **centro** del riquadro, e resta il centro
             * anche dopo la correzione della `1.53`: è la ragione per cui il riquadro si mette
             * **in mezzo** alla misura che dichiara (vedi la riga `place` qui sotto). Farlo
             * crescere dall'angolo sarebbe la cosa che l'utente ha scartato.
             *
             * ⚠️⚠️ **E LA MISURA DICHIARATA È QUELLA SCALATA, dalla 1.53**: il `layout` qui
             * sotto misura il pannello intero e ne riporta il 96%, così la **finestra** del
             * `Popup` è grande quanto il pannello **disegnato** invece che quanto sarà. È
             * l'altra metà della correzione di `sfocatura-segue`, e il perché per esteso sta su
             * [Growing]. ⚠️ **Il contenuto non si stringe**: viene misurato coi vincoli interi
             * e solo il nodo di fuori dichiara meno, quindi nessun testo va a capo in modo
             * diverso mentre il menu entra.
             *
             * ⚠️⚠️ **`ModulateAlpha` SERVIVA ALL'OMBRA, E DALLA `1.54` L'OMBRA NON C'È PIÙ**
             * (riscontro dell'utente, giro della `1.46`: *c'è un glitch tremendo sugli angoli
             * del pannello in apertura/chiusura*). Il meccanismo di allora, che vale la pena
             * conoscere perché torna ogni volta che qualcosa esce da un nodo: con la strategia
             * di serie un'alfa minore di 1 fa disegnare il sottoalbero in un **buffer fuori
             * schermo** grande quanto il nodo, e l'ombra, che sta **intorno** a quello che la
             * getta, finiva contro il bordo del buffer e veniva tagliata di netto proprio agli
             * angoli. Adesso da questo nodo non esce più niente: il bordo è dentro, e la riga è
             * uscita con la sua ragione.
             */
            modifier = Modifier
                .layout { measurable, constraints ->
                    val pannello = measurable.measure(constraints)
                    val k = grown(state)
                    val w = (pannello.width * k).roundToInt()
                    val h = (pannello.height * k).roundToInt()
                    layout(w, h) {
                        pannello.place((w - pannello.width) / 2, (h - pannello.height) / 2)
                    }
                }
                .graphicsLayer {
                    alpha = state.show.value
                    val k = grown(state)
                    scaleX = k
                    scaleY = k
                }
                /*
                 * ⚠️⚠️ **IL BORDO D'ACCENTO AL POSTO DELL'OMBRA, dalla `1.54`** (richiesta
                 * dell'utente, 2026-09-04: *via le ombre e vai con il bordino da 2px del colore
                 * di accento*). Il perché per esteso, e che cosa c'entra col 'quadrato sfocato'
                 * che vedeva intorno ai menu, stanno in testa a `Edge.kt`.
                 * ⚠️ **Sta sul modificatore e non è un parametro della `Surface`**: il suo
                 * `border` disegnerebbe il filo **sotto** il contenuto, e con lo stesso colore
                 * di un'icona a filo di bordo si mescolerebbe; qui invece il contorno si
                 * sovrappone a tutto, che è quello che un bordo deve fare.
                 */
                .edged(MENU_ROUND),
            shape = RoundedCornerShape(MENU_ROUND),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                /*
                 * ⚠️⚠️ **LE TRE RIGHE NELL'ORDINE DI `DropdownMenuContent`, letto in `MenuKt`,
                 * e l'ordine conta**: il margine sta **fuori** dallo scorrimento, quindi non
                 * scorre via; la larghezza intrinseca sta in mezzo, ed è Material stesso a
                 * dimostrare che convive con un contenuto che scorre.
                 * ⚠️⚠️ **LO SCORRIMENTO NON È UN LUSSO**: in orizzontale, su una finestra alta
                 * 360dp, fra i margini di Material restano 264dp, mentre il menu del
                 * visualizzatore è alto circa 474dp. Fino alla `1.46` quel menu sforava e la
                 * sua ultima voce non si poteva raggiungere; i due `DropdownMenu` dell'app lo
                 * scorrimento ce l'avevano, perché glielo dava Material.
                 * ⚠️⚠️ **LA LARGHEZZA INTRINSECA NON È UN DETTAGLIO: senza, una voce sola si
                 * allarga a TUTTA la finestra.** Un `DropdownMenuItem` chiede `fillMaxWidth`, e
                 * il suo tetto di larghezza non lo trattiene, perché rispetta i vincoli che
                 * riceve e dentro un `Popup` quei vincoli sono la finestra.
                 * ⚠️ **Con un contenuto che porta la sua larghezza non cambia niente**, ed è il
                 * caso del visualizzatore: la larghezza intrinseca di una misura fissa è quella
                 * misura.
                 */
                modifier = Modifier
                    .padding(vertical = MENU_PAD)
                    .width(IntrinsicSize.Max)
                    .verticalScroll(rememberScrollState())
                    /*
                     * ⚠️⚠️ **MENTRE ESCE, IL MENU NON SI TOCCA PIÙ**: un tocco su una voce che
                     * sta sbiadendo ne eseguirebbe l'azione, e chi ha appena chiuso il menu non
                     * si aspetta di aver premuto qualcosa. Si consuma nella passata iniziale,
                     * dentro la composizione, senza toccare i flag della finestra: quelli li
                     * usa il velo, e riscriverli lo spegnerebbe.
                     */
                    .then(if (state.wanted) Modifier else Modifier.deaf())
            ) { content() }
        }
    }
}

/**
 * Non lascia passare nessun tocco, consumandolo prima che arrivi ai figli.
 *
 * ⚠️ Nella passata **iniziale**, cioè prima che i figli lo vedano: nella passata `Main` il nodo
 * più interno riceve per primo, quindi una voce lo consumerebbe lei.
 */
private fun Modifier.deaf(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    }
}

/** L'aria sopra e sotto il contenuto di OGNI menu. */
private val MENU_PAD = 8.dp

/**
 * Da quanto piccolo cresce il menu, e in quanti millisecondi.
 *
 * ⚠️ La scala è **scelta dell'utente**: 0,96, dove prima era 0,72 in 220ms **dal punto
 * premuto**. Il salto che si sentiva era la scala più della durata, ed è la ragione per cui è
 * la scala il numero che è cambiato di più.
 * ⚠️⚠️ **LA DURATA È SCESA DA 170 A 120 NELLA `1.54`, e nasce come una PROVA** (richiesta
 * dell'utente, 2026-09-04: *proviamo a impostare animazioni più veloci*). 120ms è la durata
 * con cui Material fa entrare un menu, quindi non è un numero inventato per l'occasione: è
 * il minimo sotto il quale un movimento smette di leggersi come un movimento.
 * ⚠️ **La ragione che l'ha chiesta non regge, e va detto invece di lasciarlo credere**: la
 * velocità serviva a non far vedere l'angolo sfocato che sporge dal pannello, ma quel difetto
 * è **fermo** finché il menu è aperto, quindi nessuna durata lo nasconde. La nota in fondo a
 * `Veil.kt` dice che cosa lo toglierebbe davvero.
 */
private const val MENU_SMALL = 0.96f
private const val MENU_IN = 120

/** L'accelerazione di Material per una cosa che entra: parte decisa e si posa piano. */
private val MENU_EASE = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * L'uscita di un menu: una dissolvenza corta, e senza nessuna curva.
 *
 * ⚠️⚠️ **75 MILLESIMI, DALLA `1.67`, E IL NUMERO È DI MATERIAL** (`OutTransitionDuration`, che è
 * la durata con cui la libreria fa sparire i propri menu). ⚠️ **Ed è la misura che qui era stata
 * SCARTATA due volte**: Material fa l'uscita più corta dell'entrata, e fino alla `1.66` non si
 * faceva, prima perché l'uscita era l'entrata letta all'indietro e poi perché doveva accompagnare
 * la coda della patina. Adesso è una richiesta esplicita dell'utente (riscontro del giro della
 * `1.66`: *semplifica al massimo e fai sparire il menu in modo fluido e senza glitch, con una
 * dissolvenza veloce*), e con lei cade tutto quello che la coda si portava dietro.
 * ⚠️⚠️ **IL DIFETTO CHE TOGLIE NON ERA UNA CURVA SBAGLIATA, ERA IL TEMPO PASSATO IN SCENA**
 * (*toccando 'Impostazioni' o 'Cestino', che portano ad un'altra schermata, il menu se ne va
 * troppo piano, la transizione s'inceppa e rimane bloccato a schermo a mezza opacità per un tempo
 * che lo rende visibile, poi l'animazione finisce a scatti*). Quelle voci navigano, quindi
 * mentre il menu usciva l'app componeva una schermata nuova **e** ne animava la transizione: il
 * pannello di un menu che non c'è più galleggiava a mezza opacità sopra la schermata di arrivo, e
 * i fotogrammi che la composizione si prendeva erano quelli della sua dissolvenza. A 75 ms il
 * pannello è già andato quando la schermata nuova arriva.
 * ⚠️ **La curva è LINEARE perché a questa durata una curva è una decorazione che nessuno vede**:
 * quattro fotogrammi. E una curva che parte piano è esattamente ciò che, sotto carico, si legge
 * come un pannello che si inceppa.
 * ⚠️ **L'entrata non si tocca**: quella deve restare immediata, ed è la richiesta con cui la
 * `1.54` ha portato [MENU_IN] a 120.
 */
private const val MENU_OUT_MS = 75
private val MENU_OUT: Easing = LinearEasing

/**
 * Quanto è grande **adesso** il pannello, in frazione della sua misura a riposo.
 *
 * ⚠️⚠️ **IN USCITA VALE UNO, DALLA `1.67`, ED È L'ALTRA METÀ DELLA SEMPLIFICAZIONE CHIESTA.**
 * Questo numero non muove soltanto una scala: lo leggono il `layout` del pannello, che al suo
 * cambiare **rimisura tutto il contenuto del menu**, e [Growing], che rimisura e riposiziona la
 * **finestra**. Quindi ogni fotogramma dell'uscita costava una misura completa più due giri dal
 * gestore delle finestre, e li costava proprio mentre una voce che naviga fa comporre una
 * schermata nuova. Fermo a uno, dell'uscita resta la sola opacità, che si legge nella fase di
 * **disegno**: nessuna misura, nessun giro di finestra.
 * ⚠️ **Il pannello si chiude alla sua misura piena e non c'è nessun salto**: una chiusura chiesta
 * a menu aperto trova questo numero già a uno. ⚠️ **L'unico caso che salta è chiudere mentre il
 * menu sta ancora ENTRANDO**, e sono i quattro centesimi di [MENU_SMALL] in un fotogramma solo:
 * sta scritto invece di essere nascosto, perché l'alternativa è il difetto qui sopra.
 */
private fun grown(state: MenuState): Float =
    if (!state.wanted) 1f else MENU_SMALL + (1f - MENU_SMALL) * state.show.value

/**
 * Il posizionatore che mette la finestra **rimpicciolita** dove starebbe quella intera.
 *
 * ⚠️⚠️ **NASCE PER TOGLIERE LA CORNICE SFUMATA, e il meccanismo va capito o sembra un giro
 * inutile** (riscontro dell'utente, giro della `1.51`, voce `sfocatura-segue`). La sfocatura è
 * un attributo della **finestra**: copre il rettangolo della finestra e nient'altro, e non sa
 * niente di quello che il pannello dentro sta disegnando. Finché la finestra era grande quanto
 * il pannello **a riposo**, mentre il disegno era al 96% restava una fascia sfocata larga il 2%
 * per lato, con un bordo netto di fuori: quella fascia **è** la cornice. La `1.50` l'aveva
 * attenuata dosando la sfocatura insieme al pannello, che era la cura del sintomo. Adesso la
 * finestra si stringe col disegno, e la fascia non esiste in nessun fotogramma.
 *
 * ⚠️⚠️ **E LA POLITICA DEL POSTO LAVORA SULLA MISURA INTERA, non su quella del fotogramma**:
 * altrimenti un candidato che non sta a riposo potrebbe starci al 96%, e il menu salterebbe da
 * un posto all'altro mentre entra. Quindi si chiede a [MenuSpot] dove andrebbe il pannello
 * intero, e la finestra rimpicciolita si centra **dentro** quel rettangolo: il pannello cresce
 * dal proprio centro, che è la cosa che l'utente ha scelto sul mockup, e finisce esattamente
 * dove finiva prima.
 *
 * ⚠️ **La misura intera si ricava dividendo, e l'errore è di un pixel**: `popupContentSize`
 * arriva già scalata, e il fattore non scende sotto [MENU_SMALL]. A riposo la divisione è per
 * uno, quindi la posizione finale è quella di prima **alla lettera**.
 *
 * ⚠️ **Il costo è dichiarato, ed è il secondo della stessa specie**: la finestra si rimisura e si
 * riposiziona a ogni fotogramma dell'animazione, come già si ridosa la sfocatura. È la stessa
 * spesa e la stessa durata, 170 millesimi di secondo, e la via che le eviterebbe entrambe
 * (dipingere la sfocatura sulla vista dell'app con un `RenderEffect`) è il rifacimento di cui
 * parla la nota in fondo a `Veil.kt`.
 */
private class Growing(
    private val dove: PopupPositionProvider,
    private val quanto: () -> Float
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val k = quanto().coerceIn(MENU_SMALL, 1f)
        val intero = IntSize(
            (popupContentSize.width / k).roundToInt(),
            (popupContentSize.height / k).roundToInt()
        )
        val at = dove.calculatePosition(anchorBounds, windowSize, layoutDirection, intero)
        return IntOffset(
            x = at.x + (intero.width - popupContentSize.width) / 2,
            y = at.y + (intero.height - popupContentSize.height) / 2
        )
    }
}

/**
 * Su un asse, da dove nasce il menu.
 *
 * ⚠️ **'Comincia' e 'finisce' seguono il verso di scrittura**: in arabo, persiano e urdu
 * cominciano a **destra**, e il manifesto dichiara `supportsRtl`. Senza lo scambio il menu
 * nascerebbe dall'angolo sbagliato in tre delle lingue dell'app, e in italiano non si
 * vedrebbe.
 */
enum class MenuSide {
    /** Comincia dove comincia l'ancora, cioè allineato al suo bordo iniziale. */
    AT_ANCHOR,

    /** Dopo l'ancora, cioè sotto di lei o dal suo lato finale. */
    AFTER_ANCHOR,

    /** Prima dell'ancora, cioè sopra di lei o dal suo lato iniziale. */
    BEFORE_ANCHOR,

    /** In mezzo alla finestra, senza guardare l'ancora. */
    IN_WINDOW,

    /** In mezzo alla finestra e il 15% più in basso, che in AIV vuol dire 'centrato'. */
    LOWERED_IN_WINDOW
}

/**
 * Il posizionatore di **ogni** menu: una politica per asse e una sola regola di bordo.
 *
 * ⚠️⚠️ **LA COLLISIONE COL BORDO SI SCRIVE UNA VOLTA, ed è la sola cosa che il posizionatore
 * di Material faceva e i due di casa no.** Fino alla `1.46` i menu di casa avevano
 * `MenuCenter` e `MenuAbove`, che centravano e basta: un menu più alto dello schermo usciva
 * dal bordo e non lo si poteva raggiungere. Questa regola è quella di
 * `DropdownMenuPositionProvider` trascritta dal bytecode di `material3` in uso, coi suoi
 * candidati, i suoi margini e il suo ripiego, non una inventata che le somiglia.
 * ⚠️ **La prova è un confronto**: la regola vecchia e questa sono state calcolate su cinque
 * densità per sei finestre nei due versi di scrittura, sulle misure vere dei menu dell'app, e
 * danno la stessa posizione dappertutto tranne dove quella vecchia lasciava sforare.
 *
 * @param across la politica sull'asse orizzontale.
 * @param along la politica sull'asse verticale.
 * @param gap quanti pixel stacca il menu dall'ancora, sull'asse in cui è ancorato.
 * @param edge il margine dal bordo di finestra, in pixel.
 * @param air lo stesso margine in frazione della finestra: qui il `Density` non c'è, e un
 *   numero fisso sarebbe otto volte più grande su un telefono vecchio.
 */
class MenuSpot(
    private val across: MenuSide,
    private val along: MenuSide,
    private val gap: Int = 0,
    private val edge: Int = 0,
    private val air: Float = 0f,
    /**
     * Entro quanti pixel dal bordo di finestra il pannello ci si appoggia del tutto.
     *
     * ⚠️⚠️ **NASCE DA UN DIFETTO CHE SI VEDE SOLO CON LA SFOCATURA ACCESA, dalla `1.68`**
     * (riscontro del giro della `1.67`, con schermata: *a prescindere dal numero di colonne
     * della griglia, il bordo destro della colonna di destra è sempre vicinissimo al bordo
     * destro del menu del FAB ... la vicinanza tra i due bordi genera un effetto 'linea
     * sfocata' assai fastidioso*). Il menu si ancora al FAB, e il FAB ha il suo margine dal
     * bordo ([HUB_PAD]): fra il fianco del pannello e il vetro resta una feritoia larga
     * esattamente quel margine, e là dentro si vede la griglia sfocata come una riga
     * verticale.
     * ⚠️ **Il rimedio è il suo, alla lettera**: *allargare il menu del FAB e/o avvicinarlo al
     * bordo di quel tanto che basta per coprire il margine della colonna*. Appoggiarlo al
     * bordo copre la feritoia e non cambia niente di quello che c'è dentro il pannello.
     * ⚠️⚠️ **SI SCRIVE COME UNA SOGLIA E NON COME UNO SPOSTAMENTO, e la differenza è tutta**:
     * uno spostamento fisso muoverebbe **ogni** menu, compresi quelli che stanno in mezzo allo
     * schermo; una soglia interviene solo dove un fianco è già a meno di [HUB_PAD] dal vetro,
     * cioè solo dove la feritoia esiste. Un menu centrato non la incontra mai.
     * ⚠️ **Vale sui due lati e nei due versi di scrittura**, perché guarda la distanza dai due
     * bordi e non un lato scelto: il difetto è speculare, e lo dice lui (*specularmente il
     * bordo sinistro della colonna di sinistra se il FAB è a sinistra*).
     * ⚠️ **Solo in ORIZZONTALE**: in verticale un menu si stacca dalla sua ancora di [gap] e
     * dal bordo di [edge], e appoggiarlo al vetro lo farebbe finire sotto la barra di sistema.
     */
    private val flush: Int = 0
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = IntOffset(
        x = place(
            spots(
                across, anchorBounds.left, anchorBounds.right,
                windowSize.width, popupContentSize.width, gap, layoutDirection
            ),
            size = popupContentSize.width,
            space = windowSize.width,
            // ⚠️ Zero, ed è quello che passa `DropdownMenu`: in orizzontale un menu si appoggia
            // al bordo, e un margine lo staccherebbe da dove Material lo mette.
            margin = 0
        ).let { appoggia(it, popupContentSize.width, windowSize.width, flush) },
        y = place(
            spots(
                along, anchorBounds.top, anchorBounds.bottom,
                windowSize.height, popupContentSize.height, gap, LayoutDirection.Ltr
            ),
            size = popupContentSize.height,
            space = windowSize.height,
            margin = edge + (windowSize.height * air).toInt()
        )
    )
}

/**
 * I posti in cui il menu può stare su un asse, dal preferito all'ultima risorsa.
 *
 * ⚠️ L'ordine di [MenuSide.AT_ANCHOR] e [MenuSide.AFTER_ANCHOR] è quello che Material usa per
 * un menu che si apre sotto la sua ancora, letto nel bytecode: cambiarlo sposta i due menu che
 * fino alla `1.46` Material posizionava da sé.
 */
private fun spots(
    side: MenuSide,
    from: Int,
    to: Int,
    space: Int,
    size: Int,
    gap: Int,
    dir: LayoutDirection
): IntArray = when (side) {
    MenuSide.AT_ANCHOR -> intArrayOf(
        if (dir == LayoutDirection.Ltr) from else to - size,
        if (dir == LayoutDirection.Ltr) to - size else from,
        // Ultima risorsa: il bordo di finestra più vicino all'ancora.
        if ((from + to) / 2 < space / 2) 0 else space - size
    )
    MenuSide.AFTER_ANCHOR -> intArrayOf(to + gap, from - size - gap, from - size / 2)
    MenuSide.BEFORE_ANCHOR -> intArrayOf(from - size - gap, to + gap, from - size / 2)
    MenuSide.IN_WINDOW -> intArrayOf((space - size) / 2)
    // Il centro più il 15%: 'centrato' in AIV vuol dire questo, e il numero è [LOWER_BY].
    MenuSide.LOWERED_IN_WINDOW -> intArrayOf((space - size) / 2 + (space * LOWER_BY).toInt())
}

/**
 * Appoggia il pannello al bordo di finestra quando gli manca meno di [entro] per arrivarci.
 *
 * ⚠️ **Il perché sta su `MenuSpot.flush`**: qui c'è solo il conto, che è una soglia sui due lati.
 * ⚠️ **A [entro] zero non fa niente**, e non per caso: `1..0` è un intervallo vuoto, quindi i
 * menu che non passano quel numero non cambiano di un pixel.
 */
private fun appoggia(at: Int, size: Int, space: Int, entro: Int): Int = when {
    at in 1..entro -> 0
    space - (at + size) in 1..entro -> space - size
    else -> at
}

/**
 * Il posto sull'asse: il primo candidato che ci sta, e se nessuno ci sta l'ultimo riportato
 * dentro.
 */
private fun place(candidates: IntArray, size: Int, space: Int, margin: Int): Int {
    for (i in candidates.indices) {
        val at = candidates[i]
        if (at >= margin && at + size <= space - margin) return at
        if (i == candidates.lastIndex) {
            // ⚠️ Un menu più grande dello spazio fra i margini non ha nessun posto in cui
            // starci: si centra e sfora dai due lati uguali, invece di appoggiarsi a uno solo
            // e uscire tutto dall'altro. È il ripiego di Material, e serve al menu del
            // visualizzatore in orizzontale, dove oggi esce dal bordo alto.
            return if (size >= space - 2 * margin) (space - size) / 2
            else at.coerceIn(margin, space - margin - size)
        }
    }
    return 0
}

/**
 * Il menu al centro della finestra: il visualizzatore e il tocco lungo su una cartella.
 *
 * ⚠️ Il margine è una frazione e non pixel, per la ragione scritta su [MenuSpot].
 */
val MenuInWindow = MenuSpot(MenuSide.IN_WINDOW, MenuSide.LOWERED_IN_WINDOW, air = MENU_AIR)

/**
 * Un menu ancorato a un angolo: i due tastini e il filtro della testata.
 *
 * ⚠️ Ricordato, perché costruirne uno nuovo a ogni ricomposizione farebbe rimisurare la
 * finestra al `Popup` senza che niente sia cambiato.
 */
@Composable
fun rememberMenuSpot(across: MenuSide, along: MenuSide, gap: Dp = 0.dp): MenuSpot {
    val density = LocalDensity.current
    return remember(density, across, along, gap) {
        with(density) {
            MenuSpot(
                across, along, gap.roundToPx(), MENU_KEEP_OUT.roundToPx(),
                // ⚠️ La soglia è il margine del FAB, perché la feritoia da coprire è la sua:
                // vedi `MenuSpot.flush`.
                flush = HUB_PAD.roundToPx()
            )
        }
    }
}

/**
 * Quanta aria resta comunque sotto un menu centrato, in frazione dell'altezza della finestra.
 *
 * ⚠️ **Una frazione e non pixel**, perché qui la densità non c'è: `calculatePosition` riceve
 * misure in pixel e nessun `Density`, quindi un numero fisso sarebbe otto volte più grande su
 * un telefono di dieci anni fa che su uno di adesso. ⚠️ **Ed è la ragione per cui questo
 * numero non è `LOWER_AIR`**, che invece è in dp perché là il `Density` c'è: sono la stessa
 * idea misurata in due unità, e unirle vorrebbe dire toglierne una a chi non può usarla.
 */
private const val MENU_AIR = 0.02f

/** Il margine dal bordo di finestra di un menu ancorato: quello di Material, letto in `MenuKt`. */
private val MENU_KEEP_OUT = 48.dp

/**
 * Lo stato di un menu: se deve stare in scena, e se ci sta ancora perché si sta chiudendo.
 *
 * ⚠️⚠️ **NON È UN BOOLEANO DEL CHIAMANTE, E QUESTO È TUTTO IL PUNTO**: finché la presenza del
 * menu era un `if` di chi lo apre, l'**uscita non poteva esistere**, perché al primo fotogramma
 * dell'animazione la finestra era già stata tolta dalla composizione. Qui la presenza la decide
 * [visible], che resta vero per tutta la discesa. È la stessa forma dei due tempi con cui si
 * chiude una bottomsheet, e la stessa idea con cui Material tiene in vita il proprio popup.
 * ⚠️ **E il tipo è quello che trova i chiamanti dimenticati**: una voce che chiudesse il menu
 * assegnando `false` a una variabile sua adesso non compila, mentre prima saltava l'uscita e il
 * difetto si vedeva solo provando quella voce.
 */
class MenuState internal constructor() {
    var wanted by mutableStateOf(false)
        private set

    internal val show = Animatable(0f)

    /**
     * Se il **pannello** si vede adesso, chiusura in corso compresa. Con lui vive la finestra, e
     * con lui vivono la sfocatura e il livello scuro.
     *
     * ⚠️⚠️ **ERANO DUE FINO ALLA `1.66`, E DALLA `1.67` TORNANO A ESSERE UNO.** Il secondo si
     * chiamava `veiling` e diceva che l'app era ancora velata a pannello già sparito, cioè
     * esisteva solo perché la patina aveva una coda più lunga dell'uscita (`1.61`). Con l'uscita
     * di [MENU_OUT_MS] quella coda non c'è più, quindi 'il pannello si vede', 'la finestra esiste'
     * e 'l'app è velata' sono di nuovo la stessa cosa. ⚠️ **Chi trova `veiling` in una nota
     * vecchia sappia che i suoi due chiamanti** (il FAB, che deve restare staccato sopra il velo,
     * e il cancello del menu del visualizzatore) **adesso leggono questo**.
     */
    val visible: Boolean get() = visto

    /*
     * ⚠️⚠️ **DERIVATO E NON CALCOLATO A OGNI LETTURA**: questa condizione legge il **valore**
     * dell'animazione, quindi letta in composizione invalidava chi la legge a ogni fotogramma, per
     * tutta la durata. Quello che conta è il suo esito, che cambia due volte in tutto, e
     * `derivedStateOf` invalida solo quando cambia lui.
     *
     * ⚠️⚠️ **C'ERA UN TERZO TERMINE, `show.isRunning`, E DALLA `1.76` NON C'È PIÙ: DICEVA SÌ
     * QUANDO NON C'ERA NIENTE IN SCENA.** Non aggiungeva niente ai due che restano, perché
     * aprendo è vero [wanted] e chiudendo il valore è sopra zero fino alla fine; l'unico caso in
     * cui parlava da solo era quello sbagliato, cioè un'animazione che corre **mentre il valore
     * è zero**. E quel caso esisteva a ogni prima composizione: vedi la guardia in [MenuShell].
     * ⚠️ **Il difetto che ne veniva è misurato sul banco** (`CambioSchermataTest`): per sei
     * fotogrammi dopo l'arrivo di una schermata questo stato diceva 'menu in scena', quindi il
     * FAB si staccava in una finestra sua e il cancello dei tocchi entrava in scena. Il perché
     * si vedesse proprio tornando da una cartella sta in `AIV/CLAUDE.md`, § '🎬 Le animazioni
     * dentro una schermata che arriva'.
     */
    private val visto by derivedStateOf { wanted || show.value > 0f }

    fun open() {
        wanted = true
    }

    fun close() {
        wanted = false
    }
}

/**
 * @param keys quello che, cambiando, deve far nascere uno stato nuovo (e quindi un menu
 *   chiuso): nel visualizzatore è l'immagine in scena.
 */
@Composable
fun rememberMenuState(vararg keys: Any?): MenuState = remember(*keys) { MenuState() }


/**
 * Una voce di menu a tutta larghezza: icona, testo, e un tocco lungo se serve.
 *
 * ⚠️⚠️ **DALLA 1.28 LA USANO TUTTE, e prima no: era la causa dell'ALLINEAMENTO SBAGLIATO**
 * (mockup dell'utente, 2026-09-02, con la riga rossa che lo mostra: *'Modifica' non è
 * allineato alle altre voci scritte per esteso*). 'Modifica' era l'unica voce scritta a mano,
 * perché ha due gesti, e le altre erano `DropdownMenuItem`: due implementazioni diverse della
 * stessa fila. Material dispone la sua voce con un `Layout` suo, che riserva all'icona uno
 * spazio proprio (`LeadingIconLayoutId`, letto nel bytecode di `MenuKt`), mentre questa
 * metteva icona e testo in una `Row`, dove il testo comincia dove finisce l'icona.
 * ⚠️⚠️ **La cura NON è stata copiare il numero di Material, ed è la parte da tenere**: un
 * numero copiato allinea finché la libreria non lo cambia, e allora si scolla in silenzio.
 * Adesso **tutte** le voci a tutta larghezza dei menu passano di qui, quindi si allineano
 * **per costruzione**: qualunque numero abbia questa funzione, ce l'hanno tutte.
 *
 * ⚠️⚠️ **NON PUÒ ESSERE UN `DropdownMenuItem`, e non si poteva usare quello**: il suo
 * `clickable` sta **dentro**, in fondo alla propria catena di modificatori, quindi un
 * `combinedClickable` passato nel `modifier` gli finisce prima e non vede mai la pressione.
 * Nella passata `Main` il nodo più interno riceve per primo e consuma il down. È lo stesso
 * meccanismo per cui [TapHoldFab] non è un `SmallFloatingActionButton`, ed è la stessa
 * conclusione: per avere due gesti su un comando, il nodo che ascolta dev'essere **uno**.
 *
 * ⚠️ **LE MISURE SONO LETTE NEL BYTECODE DI `MenuKt`, NON RICORDATE** (`material3`
 * `1.5.0-alpha26`), perché queste voci stanno in mezzo al resto e due pixel si vedono:
 * altezza minima **48dp**, rientro orizzontale **12dp**, spazio fra icona e testo **8dp**
 * (`DropdownMenuIconTextPadding`, che diventa 12 solo con la resa da puntatore di precisione,
 * cioè col mouse), corpo **labelLarge**.
 * ⚠️ **I colori invece si CHIEDONO a Material** (`MenuDefaults.itemColors()`) invece di
 * essere copiati: quelli cambiano con la tavolozza, e una copia si scollerebbe al primo tema
 * diverso.
 *
 * @param holdLabel che cosa fa il tocco lungo, per il lettore di schermo. ⚠️ Senza, il gesto
 *   esiste solo per chi lo scopre per caso. Va insieme a [onHold]: o tutti e due o nessuno.
 */
@Composable
fun MenuRow(
    text: String,
    /**
     * Il glifo davanti al testo, e `null` per un menu che non ne ha.
     *
     * ⚠️⚠️ **PUÒ ESSERE NULLO, dalla `1.46`, E LA REGOLA È PER MENU E NON PER VOCE**: dentro un
     * menu le voci si allineano fra loro, quindi o l'hanno tutte o nessuna. Con icone su alcune
     * e non su altre i testi cominciano in due posti diversi, ed è esattamente il
     * disallineamento che la `1.28` ha corretto.
     * ⚠️⚠️ **DALLA `1.51` NESSUN MENU LE PASSA NULLE, e il parametro resta comunque**: fino a
     * lì era il menu del tastino della schermata iniziale a non averne, perché due delle sue
     * voci ('Apri un indirizzo' e 'Cestino') volevano un disegno che in Material non c'è e che
     * qui non si inventa, dato che i disegni li manda l'utente. Sono arrivati, e quel menu le
     * ha prese senza che si toccasse altro, che era esattamente quello che questa riga
     * prometteva.
     * ⚠️ **Il nullo non si toglie**, perché la regola che lo giustifica non è cambiata: se un
     * domani nasce un menu le cui voci un disegno non ce l'hanno, deve poterlo dire per il
     * menu intero invece di mettere un glifo qualunque su una riga.
     */
    icon: ImageVector?,
    onTap: () -> Unit,
    holdLabel: String? = null,
    onHold: (() -> Unit)? = null,
    /** Se la voce si può toccare adesso. Spenta resta in scena, in grigio. */
    enabled: Boolean = true,
    /**
     * Se la voce fa una cosa da cui non si torna indietro, e va scritta in colore d'errore.
     *
     * ⚠️ **Da spenta non cambia niente**, ed è voluto: il colore dello spento è un'altra
     * chiave della tavolozza, quindi una voce pericolosa e inattiva resta il grigio di sempre
     * invece di essere un rosso smorto che si legge come un errore in corso.
     */
    danger: Boolean = false
) {
    /*
     * ⚠️⚠️ **I COLORI SI CHIEDONO A MATERIAL, E LO SPENTO PURE**: `disabledTextColor` e
     * `disabledLeadingIconColor` esistono, e ricopiarne l'opacità a mano è esattamente
     * l'errore che il riquadro delle azioni ha fatto scrivendosi il proprio 0,38, che è lo
     * stesso numero della libreria copiato in casa.
     */
    val colors = MenuDefaults.itemColors(
        textColor = if (danger) MaterialTheme.colorScheme.error else Color.Unspecified,
        leadingIconColor = if (danger) MaterialTheme.colorScheme.error else Color.Unspecified
    )
    val gestures = if (onHold != null) {
        Modifier.combinedClickable(
            enabled = enabled,
            onLongClickLabel = holdLabel,
            onLongClick = withHaptics(onHold),
            onClick = onTap
        )
    } else {
        Modifier.clickable(enabled = enabled, onClick = onTap)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            /*
             * ⚠️ **Le due larghezze sono di Material, lette in `MenuKt`**, e servono da quando
             * anche le voci del cestino passano di qui: senza il tetto, una voce si allarga
             * fino alla finestra nelle lingue che scrivono lungo, e fino alla `1.45` a
             * trattenerla era Material perché quelle voci erano sue.
             */
            .sizeIn(
                minWidth = MENU_ITEM_MIN,
                maxWidth = MENU_ITEM_MAX,
                minHeight = MENU_ITEM_HEIGHT
            )
            .then(gestures)
            /*
             * ⚠️⚠️ **IL RIENTRO DI SINISTRA È PIÙ LARGO DI QUELLO DI DESTRA, dalla 1.38, e la
             * causa è il glifo che SPORGE** (riscontro `icona-copia`, 2026-09-02: *l'estremità
             * sinistra dell'icona 'Copia immagine' adesso è un po' troppo vicina al margine
             * sinistro -> sposta il contenuto delle tre righe delle azioni (e anche i due zoom
             * nascosti) di 2/3 pixel apparenti a destra*). `Glyphs.PhotoPair` esce dalla tela di
             * 1dp a sinistra (il perché sta su `Glyphs.COPY_IMAGE`), quindi con un rientro
             * uguale ai due lati quella sola voce cominciava un punto più in là delle altre e a
             * 11dp dal bordo della scheda.
             * ⚠️ **Si sposta il contenuto di TUTTE le voci e non la sola icona che sporge**, ed
             * è quello che lui ha chiesto: le voci di un menu si allineano fra loro, e mettere a
             * posto la sola 'Copia immagine' avrebbe rotto l'allineamento che la 1.28 aveva
             * appena costruito.
             */
            // ⚠️ Senza icona il rientro in più non serve: quei tre punti esistono per il
            // glifo che sporge, e senza glifo sposterebbero il testo per niente.
            .padding(
                start = if (icon != null) MENU_ITEM_LEFT else MENU_ITEM_SIDE,
                end = MENU_ITEM_SIDE
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MENU_ITEM_GAP)
    ) {
        // ⚠️ L'icona non porta descrizione e il testo sì: il modificatore di gesto fonde le
        // semantiche dei figli, quindi TalkBack legge una voce sola. Vedi `PadButton`.
        /*
         * ⚠️⚠️ **LO SLOT È SEMPRE 24dp E IL DISEGNO PUÒ ESSERE PIÙ GRANDE, dalla 1.37**:
         * `Glyphs.PhotoPair` è 25dp perché il suo foglio dietro esce dalla tela (il perché
         * sta su `Glyphs.COPY_IMAGE`), e quel dp in più deve **sporgere** invece di entrare
         * nel conto della fila. Se entrasse, quella voce avrebbe l'icona larga 25 e il suo
         * testo comincerebbe 1dp più a destra delle altre, che è il disallineamento dei
         * testi già visto e corretto nella 1.28.
         * ⚠️ **`requiredSize` e non `size`**: `size` negozia col genitore, quindi lo slot da
         * 24 schiaccerebbe il disegno da 25 e il quadrato-base tornerebbe 17,28 invece di 18.
         * La differenza fra i due è misurata e sta in `Identity.kt`, su `LAUNCHER_ZOOM`.
         * ⚠️ **In basso a destra**, così quello che sporge va a sinistra e in alto, che è da
         * dove esce quel glifo. Per i glifi da 24, che sono tutti gli altri, questo `Box` non
         * cambia niente: 24 in uno slot da 24 sta fermo qualunque allineamento si dia.
         */
        if (icon != null) {
            Box(
                modifier = Modifier.size(MENU_ITEM_ICON),
                contentAlignment = Alignment.BottomEnd
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) {
                        colors.leadingIconColor
                    } else {
                        colors.disabledLeadingIconColor
                    },
                    modifier = Modifier.requiredSize(icon.defaultWidth, icon.defaultHeight)
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) colors.textColor else colors.disabledTextColor
        )
    }
}

/** Le misure di una voce di menu, lette in `MenuKt`. Vedi [MenuRow]. */
private val MENU_ITEM_HEIGHT = 48.dp
private val MENU_ITEM_MIN = 112.dp
private val MENU_ITEM_MAX = 280.dp
/**
 * ⚠️ **16 e non 12, dalla `1.57`** (riscontro dell'utente, giro della `1.56`: *allarga un po'
 * il pannello popup, perché la parola 'Cestino' è molto vicina al bordo*). Il rientro vale per
 * i due fianchi, quindi il menu si allarga di otto in tutto e nessuna parola arriva a filo.
 */
private val MENU_ITEM_SIDE = 16.dp
private val MENU_ITEM_GAP = 8.dp

/**
 * Il rientro di **sinistra** di una voce, che è quello di Material più tre punti.
 *
 * ⚠️ **3 e non 2, e la scelta sta dentro il '2/3' che ha detto lui**: il glifo che sporge esce
 * di **1dp** oltre lo slot, quindi con 2 il suo bordo si sposterebbe di 2 ma resterebbe a 13dp
 * dalla scheda, cioè ancora meno del rientro di Material che tutte le altre voci hanno. Con 3
 * il bordo di quel glifo cade a 14 e le altre icone a 15: il glifo largo continua a sporgere,
 * come deve, ma non tocca più il margine.
 * ⚠️ **A destra resta [MENU_ITEM_SIDE]**, perché è la sinistra che aveva il difetto: lo
 * spostamento è del **contenuto**, non un allargamento simmetrico della scheda.
 */
private val MENU_ITEM_LEFT = MENU_ITEM_SIDE + 3.dp

/**
 * Lo slot dell'icona di una voce, e vale per ogni glifo qualunque sia il suo disegno.
 *
 * ⚠️ È la misura di serie di `Icon` scritta a mano, e serve scritta perché adesso c'è un
 * glifo che dichiara 25dp: senza questo numero lo slot lo deciderebbe il disegno più grande.
 * Vedi [MenuRow].
 */
private val MENU_ITEM_ICON = 24.dp

/**
 * Dove cade il **centro** dell'icona di una voce, misurato dal fianco del pannello.
 *
 * ⚠️⚠️ **ESISTE PERCHÉ IL RIQUADRO A ICONE CI SI DEVE ALLINEARE, dalla `1.59`** (richiesta
 * dell'utente, giro della `1.58`, con una schermata e una riga verticale tracciata sopra:
 * *sarebbe bello se le icone delle voci in lista si allineassero perfettamente in verticale
 * con la prima colonna di icone nella griglia*). Prima i due rientri erano indipendenti e si
 * incontravano **per caso**: misurato sulla sua schermata, la lista cadeva a 31dp e la prima
 * colonna a 31,33dp, cioè due pixel.
 * ⚠️⚠️ **E DUE PIXEL ERANO LA FORTUNA, NON LA REGOLA**: il rientro della lista è un numero
 * fisso, quello della prima colonna era una **frazione della larghezza del pannello**, e quella
 * larghezza la fa la voce di testo più lunga. Nelle lingue con parole lunghe lo stesso conto
 * dava sette o otto dp di scarto. Adesso il riquadro parte da qui e lo scarto è zero per
 * costruzione, in tutte e ventotto.
 */
val MENU_ICON_MID = MENU_ITEM_LEFT + MENU_ITEM_ICON / 2

/**
 * Lo stondamento di **ogni** menu.
 *
 * ⚠️⚠️ **UNO SOLO PER TUTTI, dalla 1.28**: prima erano tre numeri in tre file (8 nel
 * visualizzatore, 8 nel navigatore, 16 nella selezione), e l'utente li ha visti diversi
 * prima di noi. Stanno qui, accanto alla superficie che li usa, e non passano più come
 * parametro: un parametro è un invito a ridiventare tre.
 * ⚠️ **20 e non 28**: le bottomsheet stanno a 28 e vanno bene così (parole sue), e un menu
 * che le raggiungesse smetterebbe di distinguersi da loro. 20 è il gradino sopra il 16 della
 * selezione, cioè quello che era già il più morbido dei tre.
 * ⚠️ **Questa costante governava anche la striscia d'accento**, uscita di scena con la 1.38:
 * il perché sta nella nota di [MenuShell]. Lo stondamento resta, ed è indipendente da lei.
 */
val MENU_ROUND = 20.dp

/**
 * Chi è in scena adesso fra i menu, e serve a una cosa sola: **far sapere all'app che deve
 * stare zitta**.
 *
 * ⚠️⚠️ **NON SI PUÒ RIUSARE `VeilStage` PER QUESTO, ed è la ragione per cui questo oggetto
 * esiste**: quella mappa è vuota quando 'Sfocatura dietro i pannelli' è spenta, cioè nel caso
 * di fabbrica, e legare a lei il tocco che non deve passare renderebbe la correzione una
 * funzione facoltativa. Qui dentro un menu entra sempre, acceso o spento che sia
 * l'interruttore.
 * ⚠️ **Una mappa di richiedenti e non un numero**, per la stessa ragione scritta su
 * `VeilStage`: due menu possono sovrapporsi per il tratto di un'uscita, e con un contatore
 * solo il primo che se ne va spegnerebbe anche il secondo.
 */
internal object MenuScene {
    private val aperti = mutableStateMapOf<Any, Unit>()

    /** Se c'è almeno un menu in scena. Si legge **nel giro dei tocchi**, non in composizione. */
    val open: Boolean get() = aperti.isNotEmpty()

    fun enter(chi: Any) {
        aperti[chi] = Unit
    }

    fun leave(chi: Any) {
        aperti.remove(chi)
    }

    /** La rete contro il menu fantasma, gemella di `VeilStage.clear`. */
    fun clear() {
        aperti.clear()
    }
}

/**
 * Il velo che, mentre un menu è aperto, **impedisce al tocco di arrivare anche all'app**.
 *
 * ⚠️⚠️ **IL DIFETTO CHE TOGLIE È MISURATO SUL BYTECODE DI COMPOSE**: `createFlags` di
 * `AndroidPopup_androidKt` parte da `FLAG_WATCH_OUTSIDE_TOUCH` e non mette mai
 * `FLAG_NOT_TOUCH_MODAL`, quindi da Android 12 la finestra di un popup **non è modale al
 * tocco**: un dito fuori dal pannello arriva a tutte e due le finestre. Il popup lo legge come
 * 'fuori' e si chiude, e l'app lo legge come un tocco suo: nella vista ad albero apriva la riga
 * sotto il dito, nella testata della griglia la miniatura, nella schermata iniziale la cartella.
 * ⚠️⚠️ **UNO SOLO E NON UNO PER SCHERMATA, ED È IL PUNTO DI QUESTA FORMA.** Fino alla `1.69` il
 * rimedio era un `Box` con `detectTapGestures` scritto **dentro una schermata**, e viveva in
 * una schermata su cinque: gli altri quattro chiamanti di [MenuShell] non l'avevano, e in una
 * di quelle lo stato del menu non era nemmeno raggiungibile da fuori. Ripeterlo quattro volte
 * avrebbe risolto quattro casi e lasciato in piedi il modo di sbagliare il quinto; qui sta
 * sopra tutto quello che l'app disegna, quindi vale per ogni menu che esiste e per ogni menu
 * che nascerà.
 * ⚠️ **Consuma nella passata `Initial`**, cioè prima di chiunque altro: quello che deve
 * rispondere a quel tocco è il popup, e il popup vive in un'altra finestra, dove questo nodo
 * non arriva.
 *
 * ⚠️⚠️ **A MENU CHIUSO NON ESISTE, E NON BASTAVA CHE NON CONSUMASSE: la `1.70` ha bloccato
 * l'intera app**, che si avviava e non rispondeva a nulla (riscontro dell'utente, 2026-09-05:
 * *l'app si avvia e appare la griglia delle cartelle, ma nulla risponde ai comandi*). ⚠️ **La
 * causa non è il consumo: è la HIT-TEST**, che in Compose sceglie **a chi mandare** l'evento
 * prima che un solo `awaitPointerEvent` giri. Fra fratelli sovrapposti quel giro va dall'ultimo
 * disegnato al primo e **si ferma sul primo ramo che colpisce**: un `Box` con `pointerInput`
 * grande quanto lo schermo e disegnato dopo `content()` è quel ramo, quindi la griglia sotto
 * non veniva nemmeno interpellata. Non consumare vuol dire 'lascio decidere anche agli altri
 * che hanno ricevuto l'evento', non 'lo lascio passare a chi sta sotto': quelli sotto l'evento
 * non lo ricevono affatto.
 * - ⚠️⚠️ **Il fatto era già scritto tre volte in questo file**, sul cancello di [MenuShell]
 *   (*un popup sempre presente e trasparente in più si mangerebbe i tocchi*): la stessa
 *   trappola, presa da un `Popup` invece che da un `Box`. Una nota che descrive il difetto non
 *   impedisce di rifarlo con un altro strumento.
 * - **Il rimedio è l'ASSENZA, non una condizione più furba**: finché nessun menu è in scena il
 *   nodo non è nell'albero, quindi non c'è niente che possa intercettare un tocco. Una guardia
 *   dentro il giro dei tocchi lascerebbe in piedi il nodo, cioè la causa.
 * - ⚠️ **Il prezzo è una ricomposizione della radice** a ogni apertura e a ogni chiusura di un
 *   menu, che è quello che la `1.70` voleva evitare leggendo [MenuScene] dentro il giro dei
 *   tocchi. Costa un `Box` vuoto: si paga.
 *
 * ⚠️ **I DIALOGHI non hanno bisogno di lui**: la finestra di un `Dialog` è modale al tocco, e
 * quello che si tocca fuori non arriva mai all'app. Il loro caso gemello è un'altra cosa e vive
 * in `Centred.kt`, sull'aria dichiarata sopra il pannello.
 */
@Composable
fun MenuGuard(modifier: Modifier = Modifier) {
    DisposableEffect(Unit) { onDispose { MenuScene.clear() } }
    if (!MenuScene.open) return
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                }
            }
    )
}

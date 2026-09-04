package io.github.roccobot.aiv

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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
    LaunchedEffect(state.wanted) {
        state.show.animateTo(
            targetValue = if (state.wanted) 1f else 0f,
            animationSpec = tween(
                durationMillis = MENU_IN,
                easing = if (state.wanted) MENU_EASE else MENU_OUT
            )
        )
    }

    /*
     * ⚠️⚠️ **IL CANCELLO SERVE ALL'ORDINE FRA LE FINESTRE**: a menu chiuso non deve esistere
     * nessun `Popup`, o il tastino che si stacca sopra la propria finestra non sarebbe più
     * l'ultima aggiunta e finirebbe sotto. Un popup sempre presente e trasparente in più si
     * mangerebbe i tocchi.
     */
    if (!state.inScene) return

    Popup(
        popupPositionProvider = position,
        onDismissRequest = state::close,
        properties = PopupProperties(
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
         */
        WindowVeil { state.show.value }
        Surface(
            /*
             * ⚠️⚠️ **CRESCE DA 0,96 E NON DA ZERO, in 170ms** (scelta dell'utente sul
             * mockup). Prima sbucava **dal punto premuto** con una scala da 0,72: un menu che
             * si gonfia da un angolo dello schermo tira l'occhio dove il dito era già, e
             * l'utente ha chiesto una cosa sobria. Da 0,96 il movimento si sente e non si
             * guarda.
             * ⚠️ L'origine è quella di serie, il **centro** del riquadro.
             *
             * ⚠️⚠️ **`ModulateAlpha` NON È UN'OTTIMIZZAZIONE: SENZA DI LUI GLI ANGOLI SI
             * ROMPONO MENTRE IL MENU ENTRA ED ESCE** (riscontro dell'utente, giro della `1.46`,
             * su due menu diversi: *c'è un glitch tremendo sugli angoli del pannello in
             * apertura/chiusura*). Il meccanismo, e va capito o la riga sembra superflua: con
             * la strategia di serie un'alfa minore di 1 fa disegnare tutto il sottoalbero in un
             * **buffer fuori schermo**, e quel buffer è grande quanto il nodo. L'ombra di
             * questa superficie invece **esce** dal nodo, perché un'ombra sta intorno a quello
             * che la getta: finisce contro il bordo del buffer e viene tagliata di netto. Il
             * taglio si vede **agli angoli**, che è dove il riquadro stondato si allontana di
             * più dal suo rettangolo e l'ombra è più larga. E si vede **solo durante
             * l'animazione**, perché è l'unico momento in cui l'alfa non vale 1.
             * ⚠️ **Quello che si paga in cambio, dichiarato**: `ModulateAlpha` applica l'alfa a
             * ogni istruzione di disegno invece che al risultato, quindi un contenuto che si
             * sovrappone a se stesso si mescola in modo un po' diverso. Qui il contenuto è
             * testo opaco sopra un fondo opaco, per 170 millesimi di secondo: la differenza non
             * si vede, il taglio dell'ombra sì.
             * ⚠️ **L'incognita che resta è una sola**: se il taglio dell'ombra fosse l'unica
             * causa del difetto che si vede, o se sotto ce ne fosse una seconda. Il
             * meccanismo qui sopra si legge nel codice; che spieghi **tutto** quello che lui
             * vede lo dice solo la prova.
             */
            modifier = Modifier.graphicsLayer {
                alpha = state.show.value
                val k = MENU_SMALL + (1f - MENU_SMALL) * state.show.value
                scaleX = k
                scaleY = k
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
            shape = RoundedCornerShape(MENU_ROUND),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = MENU_LIFT
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

/** L'ombra: sopra un'immagine è l'unica cosa che stacca il menu da lei. */
private val MENU_LIFT = 6.dp

/**
 * Da quanto piccolo cresce il menu, e in quanti millisecondi (scelta dell'utente).
 *
 * ⚠️ 0,96 e 170ms: prima era 0,72 in 220ms **dal punto premuto**. Il salto che si sentiva era
 * la scala più della durata, ed è la ragione per cui è la scala il numero che è cambiato di
 * più.
 */
private const val MENU_SMALL = 0.96f
private const val MENU_IN = 170

/** L'accelerazione di Material per una cosa che entra: parte decisa e si posa piano. */
private val MENU_EASE = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * L'uscita è l'entrata letta all'indietro: accelera per costruzione.
 *
 * ⚠️ **Stessa durata dell'entrata**, e non più corta: Material fa più corta l'uscita, ma in
 * questa app 'speculare' ha già una definizione, quella con cui esce una bottomsheet, e due
 * durate diverse la contraddirebbero. La misura scartata è quella, e sta scritta perché non la
 * si riproponga come una novità.
 */
private val MENU_OUT: Easing = Easing { f -> 1f - MENU_EASE.transform(1f - f) }

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
    private val air: Float = 0f
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
        ),
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
        with(density) { MenuSpot(across, along, gap.roundToPx(), MENU_KEEP_OUT.roundToPx()) }
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
 * [inScene], che resta vero per tutta la discesa. È la stessa forma dei due tempi con cui si
 * chiude una bottomsheet, e la stessa idea con cui Material tiene in vita il proprio popup.
 * ⚠️ **E il tipo è quello che trova i chiamanti dimenticati**: una voce che chiudesse il menu
 * assegnando `false` a una variabile sua adesso non compila, mentre prima saltava l'uscita e il
 * difetto si vedeva solo provando quella voce.
 */
class MenuState internal constructor() {
    var wanted by mutableStateOf(false)
        private set

    internal val show = Animatable(0f)

    /** Se il menu deve stare in scena adesso, chiusura in corso compresa. */
    val inScene: Boolean get() = wanted || show.isRunning || show.value > 0f

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
private val MENU_ITEM_SIDE = 12.dp
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

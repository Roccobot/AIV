package io.github.roccobot.aiv

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * La superficie condivisa dei menu: un riquadro che cresce, dove dice il posizionatore.
 *
 * ⚠️⚠️ **`Popup` E NON `DropdownMenu`, ed è quello che permette di scegliere il posto**: un
 * `DropdownMenu` si posiziona **contro il proprio genitore** e non accetta un posizionatore,
 * quindi con lui non si potevano scrivere né 'al centro dello schermo' (il visualizzatore,
 * dalla `0.69`) né 'sopra il tastino e al centro' (la selezione, dalla `0.75`).
 * ⚠️ **Le voci restano `DropdownMenuItem`**: sono composabili come gli altri, quindi si usano
 * dentro la nostra superficie e la resa Material (altezze, margini, corpi, icone ai lati) non
 * si perde. Quello che si scrive a mano è **solo** la superficie e dove sta.
 * ⚠️⚠️ **DALLA 1.28 LE VOCI NON SONO PIÙ `DropdownMenuItem` ma [MenuRow]**, e la riga qui
 * sopra racconta com'era: prendere le voci di Material senza il suo posizionamento andava
 * bene finché tutte erano sue, ma una di loro ha due gesti e non poteva esserlo. Due
 * disposizioni diverse della stessa fila non si allineano, e si vedeva.
 *
 * ⚠️⚠️ **STA IN UN FILE A SÉ dalla 0.75, quando i menu sono diventati due**: il secondo
 * avrebbe copiato la superficie, l'ombra e i tre numeri dell'animazione, e un'animazione
 * scritta in due posti diverge al primo ritocco. Quei numeri li ha scelti l'utente su un
 * mockup, quindi valgono per i menu, non per uno.
 * ⚠️⚠️ **DALLA 1.28 IL RAGGIO NON È PIÙ UN PARAMETRO, ed è la correzione di un difetto vero**
 * (richiesta dell'utente, 2026-09-02: *va uniformato TUTTO*). Fino alla `1.27` ogni menu
 * portava il suo numero, e i numeri erano **tre**: 8 nel visualizzatore, 8 nel navigatore, 16
 * nella selezione. Erano nati come 'dipende dalla forma del contenuto', che è una ragione
 * plausibile e sbagliata: uno stondamento dice **che cosa è** quella superficie, non quanto è
 * larga, e tre valori diversi dicevano che erano tre cose diverse. Adesso è [MENU_ROUND], uno
 * solo, e non si può più far divergere passandogli un numero.
 */
@Composable
fun MenuShell(
    /** Dove va il menu: [MenuCenter] o [MenuAbove]. */
    position: PopupPositionProvider,
    /**
     * Se un tocco **fuori** dal menu lo chiude.
     *
     * ⚠️⚠️ **NON È UN GUSTO: è quello che distingue un menu di passaggio da un pannello.**
     * Nel visualizzatore il menu è di passaggio e si chiude toccando altrove; nella selezione
     * invece è il tastino ad **alternarlo**, e le due cose insieme non stanno. Da Android 12
     * una finestra non è più modale al tocco, quindi il tocco sul tastino arriva **a tutti e
     * due**: il `Popup` lo vede come 'fuori' e si chiude, il tastino alterna e lo riapre, e
     * l'utente vede un lampeggio invece di una chiusura. Spenta la chiusura di fuori, il
     * tastino è l'unico a decidere e non c'è nessuna corsa da arbitrare a colpi di
     * millisecondi.
     * ⚠️ **Il gesto indietro chiude sempre**, ed è la via d'uscita che resta in tutti e due i
     * casi.
     */
    dismissOnOutside: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    /*
     * ⚠️ L'animazione parte al primo giro di composizione, e serve una bandierina perché
     * `animateFloatAsState` anima un CAMBIAMENTO: partendo già a 1 non ci sarebbe niente da
     * animare, e il menu comparirebbe di scatto.
     */
    var grown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { grown = true }
    val show by animateFloatAsState(
        targetValue = if (grown) 1f else 0f,
        animationSpec = tween(durationMillis = MENU_IN, easing = MENU_EASE),
        label = "menu"
    )

    Popup(
        popupPositionProvider = position,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = dismissOnOutside
        )
    ) {
        Surface(
            /*
             * ⚠️⚠️ **CRESCE DA 0,96 E NON DA ZERO, in 170ms** (scelta dell'utente sul
             * mockup). Prima sbucava **dal punto premuto** con una scala da 0,72: un menu che
             * si gonfia da un angolo dello schermo tira l'occhio dove il dito era già, e
             * l'utente ha chiesto una cosa sobria. Da 0,96 il movimento si sente e non si
             * guarda.
             * ⚠️ L'origine è quella di serie, il **centro** del riquadro: tutti e due i menu
             * stanno al centro di qualcosa, e con un'origine ancorata al dito la scala
             * sembrava venire dal posto sbagliato.
             */
            modifier = Modifier.graphicsLayer {
                alpha = show
                val k = MENU_SMALL + (1f - MENU_SMALL) * show
                scaleX = k
                scaleY = k
            },
            shape = RoundedCornerShape(MENU_ROUND),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = MENU_LIFT
        ) {
            /*
             * ⚠️⚠️ **LA LARGHEZZA INTRINSECA NON È UN DETTAGLIO: senza, una voce sola si
             * allarga a TUTTA la finestra.** Un `DropdownMenuItem` chiede `fillMaxWidth`, e
             * il suo `sizeIn(maxWidth = 280dp)` non lo trattiene, perché rispetta i vincoli
             * che riceve e dentro un `Popup` quei vincoli sono la finestra. `DropdownMenu` di
             * Material mette qui la stessa riga (verificato nel bytecode di `MenuKt`, dove
             * `DropdownMenuContent` chiama `width(IntrinsicSize.Max)`): prendere le sue voci
             * senza il suo posizionamento vuol dire prendere anche questo.
             * ⚠️ **Con un contenuto che porta la sua larghezza non cambia niente**, ed è il
             * caso del visualizzatore: la larghezza intrinseca di una misura fissa è quella
             * misura.
             */
            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                content()
                MenuStripe()
            }
        }
    }
}

/**
 * La striscia d'accento in fondo a un menu.
 *
 * ⚠️⚠️ **È UN DISEGNO DELL'UTENTE** (mockup del 2026-09-02: *solo per un tocco di carattere e
 * di colore*), e sta **dentro** la superficie invece che sotto: così i due angoli in basso la
 * tagliano insieme al resto, e la striscia sembra parte del riquadro e non un nastro appoggiato
 * sopra.
 * ⚠️ **Sta su TUTTI i menu e non solo su quello che l'ha vista nascere**, perché è il segno che
 * dice 'questa superficie è un menu': metterla su uno solo direbbe che gli altri sono un'altra
 * cosa.
 * ⚠️⚠️ **È COMPOSABILE A SÉ DALLA 1.36, e prima era scritta dentro [MenuShell]**: il menu del
 * tastino della schermata iniziale è un `DropdownMenu` di Material (si posiziona da sé contro
 * l'ancora, che è quello che serve a un tastino in un angolo), quindi non passa da quella
 * superficie e la striscia gli va aggiunta in fondo al contenuto. Scritta due volte
 * divergerebbe al primo ritocco, ed è già la seconda volta che questa striscia si ritocca.
 */
@Composable
fun MenuStripe(giu: Dp = 0.dp, round: Dp = MENU_ROUND) {
    Box(
        /*
         * ⚠️⚠️ **NON ARRIVA AI BORDI, dalla 1.29** (riscontro dell'utente, 2026-09-02: *va
         * bene in basso, ma leggermente più spessa e non fino in fondo, né di lato né in
         * basso, come se intorno ci fosse un filetto sovrapposto dello stesso colore dello
         * sfondo*). Il rientro è un `padding` sui tre lati, che è **esattamente** il filetto
         * che descrive: la superficie del menu resta intorno, quindi la striscia si stacca dal
         * bordo senza che nessuno disegni una cornice.
         */
        modifier = Modifier
            .fillMaxWidth()
            /*
             * ⚠️⚠️ **`giu` SERVE A UNA SUPERFICIE CHE AGGIUNGE PADDING SOTTO IL CONTENUTO,
             * e oggi è una sola**: il `DropdownMenu` di Material mette 8dp sopra e sotto la
             * propria colonna, quindi là la striscia si fermava a 8 + [MENU_STRIPE_UNDER]
             * dal fondo della scheda mentre in [MenuShell] si fermava a 2. L'utente ha visto
             * le due e le ha giudicate entrambe sbagliate (riscontro `striscia-sette`:
             * *FAB della schermata home troppo staccata dal margine inferiore; FAB del
             * cestino l'opposto: troppo vicina. Una via di mezzo sarebbe l'ideale*).
             * ⚠️ **Adesso il numero è UNO** ([MENU_STRIPE_UNDER], la via di mezzo), e chi ha
             * del padding sotto lo dichiara qui invece di ritrovarsi una distanza diversa.
             */
            .offset(y = giu)
            .padding(
                start = MENU_STRIPE_SIDE,
                end = MENU_STRIPE_SIDE,
                bottom = MENU_STRIPE_UNDER
            )
            .height(MENU_STRIPE)
            /*
             * ⚠️⚠️ **LA FORMA È MISURATA SUL SUO MOCKUP, dalla 1.34, e prima era una
             * PASTIGLIA**: l'utente ha bocciato la versione della `1.29` e ha mandato la forma
             * che voleva (voce `striscia-spessa`: *la forma dovrebbe essere questa*). Misurata
             * sulla sua schermata, colonna per colonna: **spigoli quasi vivi in alto** e
             * **angoli in basso che seguono la curva della scheda**, non due punte tonde.
             * ⚠️ **Il raggio in basso è quello del menu meno il rientro**: così i due archi
             * restano **paralleli**, che è quello che l'occhio legge come 'dentro la scheda'.
             * Compose lo stringe da sé all'altezza disponibile, quindi il numero grande non
             * sfonda una striscia bassa.
             */
            /*
             * ⚠️ **`round` è lo stondamento della SUPERFICIE che la contiene**, e di serie è
             * quello dei menu: dalla 1.37 la striscia sta anche sui pannellini, che sono
             * dialoghi e hanno l'angolo di un dialogo. Il raggio in basso è quello della
             * superficie **meno il rientro**, che è ciò che tiene i due archi paralleli: con
             * un numero fisso, su una superficie più tonda i due archi divergono e la striscia
             * si legge come appoggiata sopra invece che dentro.
             */
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(
                    topStart = MENU_STRIPE_TOP,
                    topEnd = MENU_STRIPE_TOP,
                    bottomStart = round - MENU_STRIPE_UNDER,
                    bottomEnd = round - MENU_STRIPE_UNDER
                )
            )
    )
}

/**
 * Un **sotto-menu**: un pannellino che si apre da una voce di menu e porta una scelta.
 *
 * ⚠️⚠️ **NASCE PER LA STRISCIA, dalla 1.37** (riscontro `striscia-sette`: *OK sul menu a
 * pressione lunga. Va aggiunta sui sotto-menu (es. pressione lunga su Info)*). Un `AlertDialog`
 * di Material **non** lascia disegnare in fondo alla propria superficie: sotto i tasti c'è il
 * suo padding e la superficie è sua, quindi la striscia si poteva mettere solo dentro il
 * contenuto, cioè **sopra** i tasti, che è un altro posto. Per averla in fondo bisogna
 * possedere la superficie, e questo guscio è quella.
 * ⚠️⚠️ **I NUMERI SONO QUELLI DI MATERIAL, scritti a mano perché adesso la superficie è
 * nostra**: contenitore `surfaceContainerHigh`, angolo `extraLarge` (28dp), 24dp di rientro
 * tutto attorno e 16 sotto il titolo, letti nei token e nel bytecode di `AlertDialogKt`
 * (`TitlePadding` è un `PaddingValues` col solo bordo inferiore a 16). ⚠️ Se un giorno un
 * pannellino si vedesse diverso dai dodici dialoghi che restano di Material, è qui che si
 * guarda: la differenza sarebbe un numero fuori posto, non una scelta.
 * ⚠️⚠️ **NON VA SUI DIALOGHI CHE NON SONO MENU, e la riga è dichiarata**: la conferma di una
 * cancellazione, la rinomina, la conversione e l'indirizzo sono **moduli** (un campo, due
 * tasti), non scelte, e una striscia d'accento su di loro direbbe 'questo è un menu' di una
 * cosa che non lo è. Il criterio è: si apre da una voce di menu **e** offre una scelta.
 * ⚠️ **La stretta del 15% resta la sua**: [Modifier.lowered] sta sulla superficie, dove stava
 * sul dialogo di Material.
 */
@Composable
fun SubPanel(
    title: String,
    onDismiss: () -> Unit,
    buttons: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.lowered(),
            shape = RoundedCornerShape(PANEL_ROUND),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column {
                Column(modifier = Modifier.padding(PANEL_PAD)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(PANEL_GAP))
                    content()
                    Spacer(Modifier.height(PANEL_GAP))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        content = buttons
                    )
                }
                // ⚠️ Fuori dal rientro e in fondo alla colonna: è il fondo della SUPERFICIE
                // che si colora, e per questo il guscio esiste.
                MenuStripe(round = PANEL_ROUND)
            }
        }
    }
}

/** L'angolo, il rientro e l'aria di un sotto-menu. Vedi [SubPanel]: sono i numeri di Material. */
private val PANEL_ROUND = 28.dp
private val PANEL_PAD = 24.dp
private val PANEL_GAP = 16.dp

/**
 * Il menu al centro della finestra: quello del visualizzatore.
 *
 * ⚠️ **Fa quello che faceva `Alignment.Center` di `Popup`, ma sulla FINESTRA e non
 * sull'ancora**: l'allineamento di serie centra il menu sui limiti del **genitore**, che là
 * era lo schermo intero e quindi coincideva. Scritta sulla finestra, la coincidenza non serve
 * più e i due menu possono condividere [MenuShell].
 */
object MenuCenter : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        /*
         * ⚠️⚠️ **NON È PIÙ AL CENTRO ESATTO, dalla 1.28** (richiesta dell'utente, 2026-09-02:
         * *è meglio se appare un po' spostato verso il basso, perché si raggiunge meglio con
         * il pollice*), e si misura sull'**altezza della finestra**: sul residuo fra menu e
         * schermo sarebbe una frazione di una frazione, cioè un movimento che a menu lungo
         * sparisce proprio dove il pollice fatica di più.
         * ⚠️⚠️ **DALLA 1.29 IL NUMERO È QUELLO DI TUTTA L'APP, [LOWER_BY]**: qui era 17%,
         * cioè la metà di un intervallo indicato a occhio, e poi l'utente ha definito che
         * 'centrato' in AIV vuol dire **centrato più il 15% in basso**, per ogni elemento che
         * si apre in mezzo. Due numeri per la stessa idea sono la stessa trappola degli
         * angoli dei menu, e fra 15 e 17 non c'è niente da vedere.
         * ⚠️ **La stretta esiste perché lo spostamento può portare fuori**: un menu alto
         * quasi quanto lo schermo, spinto giù, uscirebbe dal bordo inferiore. Il limite lo
         * riporta dentro con [MENU_AIR] di aria, e a quel punto il menu è comunque in basso,
         * che è quello che serviva.
         */
        val free = windowSize.height - popupContentSize.height
        val centre = free / 2
        val floor = free - (windowSize.height * MENU_AIR).toInt()
        val wanted = centre + (windowSize.height * LOWER_BY).toInt()
        return IntOffset(
            x = (windowSize.width - popupContentSize.width) / 2,
            y = if (floor <= centre) centre else wanted.coerceAtMost(floor)
        )
    }
}

/**
 * Quanta aria resta comunque sotto il menu, in frazione dell'altezza della finestra.
 *
 * ⚠️ **Una frazione e non pixel**, perché qui la densità non c'è: `calculatePosition` riceve
 * misure in pixel e nessun `Density`, quindi un numero fisso sarebbe otto volte più grande su
 * un telefono di dieci anni fa che su uno di adesso. ⚠️ **Ed è la ragione per cui questo
 * numero non è [LOWER_AIR]**, che invece è in dp perché là il `Density` c'è: sono la stessa
 * idea misurata in due unità, e unirle vorrebbe dire toglierne una a chi non può usarla.
 */
private const val MENU_AIR = 0.02f

/**
 * Il menu **sopra l'ancora** e centrato nella finestra: quello della selezione.
 *
 * ⚠️⚠️ **LA POSIZIONE SI RICAVA DALL'ANCORA, e non da una somma di margini.** Il
 * posizionatore riceve i limiti del tastino in coordinate di finestra, quindi basta il suo
 * bordo alto. La strada alternativa era sommare rientro di sistema, margini della schermata,
 * margine del tastino e sua altezza: quattro numeri da tenere d'accordo con tre file
 * diversi, e sbagliati il giorno che uno cambia. Così invece si corregge da sé.
 * ⚠️ **Il tastino resta scoperto**, ed è il requisito che decide la posizione: il menu si
 * chiude **col tastino**, quindi un menu che lo coprisse renderebbe impossibile la cosa per
 * cui esiste.
 *
 * @param gap quanti pixel stacca il menu dal bordo alto dell'ancora.
 */
class MenuAbove(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = IntOffset(
        x = (windowSize.width - popupContentSize.width) / 2,
        y = anchorBounds.top - popupContentSize.height - gap
    )
}

/** L'ombra: sopra una fotografia è l'unica cosa che stacca il menu da lei. */
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
    icon: ImageVector,
    onTap: () -> Unit,
    holdLabel: String? = null,
    onHold: (() -> Unit)? = null
) {
    val colors = MenuDefaults.itemColors()
    val gestures = if (onHold != null) {
        Modifier.combinedClickable(
            onLongClickLabel = holdLabel,
            onLongClick = withHaptics(onHold),
            onClick = onTap
        )
    } else {
        Modifier.clickable(onClick = onTap)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = MENU_ITEM_HEIGHT)
            .then(gestures)
            .padding(horizontal = MENU_ITEM_SIDE),
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
        Box(modifier = Modifier.size(MENU_ITEM_ICON), contentAlignment = Alignment.BottomEnd) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.leadingIconColor,
                modifier = Modifier.requiredSize(icon.defaultWidth, icon.defaultHeight)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = colors.textColor
        )
    }
}

/** Le tre misure di una voce di menu, lette in `MenuKt`. Vedi [MenuRow]. */
private val MENU_ITEM_HEIGHT = 48.dp
private val MENU_ITEM_SIDE = 12.dp
private val MENU_ITEM_GAP = 8.dp

/**
 * Lo slot dell'icona di una voce, e vale per ogni glifo qualunque sia il suo disegno.
 *
 * ⚠️ È la misura di serie di `Icon` scritta a mano, e serve scritta perché adesso c'è un
 * glifo che dichiara 25dp: senza questo numero lo slot lo deciderebbe il disegno più grande.
 * Vedi [MenuRow].
 */
private val MENU_ITEM_ICON = 24.dp

/**
 * Lo stondamento di **ogni** menu, e la striscia d'accento in fondo.
 *
 * ⚠️⚠️ **UNO SOLO PER TUTTI, dalla 1.28**: prima erano tre numeri in tre file (8 nel
 * visualizzatore, 8 nel navigatore, 16 nella selezione), e l'utente li ha visti diversi
 * prima di noi. Stanno qui, accanto alla superficie che li usa, e non passano più come
 * parametro: un parametro è un invito a ridiventare tre.
 * ⚠️ **20 e non 28**: le bottomsheet stanno a 28 e vanno bene così (parole sue), e un menu
 * che le raggiungesse smetterebbe di distinguersi da loro. 20 è il gradino sopra il 16 della
 * selezione, cioè quello che era già il più morbido dei tre.
 * ⚠️⚠️ **I TRE NUMERI DELLA STRISCIA SONO MISURATI SUL MOCKUP DELL'UTENTE, dalla 1.34, e la
 * versione della 1.29 era BOCCIATA** (voce `striscia-spessa`: *la forma dovrebbe essere
 * questa*, con una schermata). Quella era 5dp di spessore, 8dp di rientro su tre lati e le
 * estremità **tonde** come una pastiglia; le note che lo spiegavano stanno nella storia git
 * della `1.29`, e il ragionamento era buono ma il risultato no.
 * ⚠️ **Come sono stati letti**: la sua schermata è stata misurata colonna per colonna, e la
 * scala si ricava dalla larghezza del menu ([MENU_WIDTH], 252dp, che nel suo ritaglio sono 808
 * pixel, cioè 3,2 pixel per dp). Da lì: spessore **18px = 5,6dp**, rientro laterale **12px =
 * 3,7dp**, aria sotto **6px = 1,9dp**, spigoli in alto quasi vivi. Arrotondati ai valori qui
 * sotto.
 * ⚠️⚠️ **IL RIENTRO NON È PIÙ UGUALE SUI TRE LATI, ed è la differenza che si vede**: sotto è
 * un terzo dei lati, quindi la striscia sta **appoggiata** al fondo della scheda invece di
 * galleggiarci in mezzo. La regola vecchia (*il rientro è più grande dello spessore*) è
 * decaduta con la forma che la reggeva.
 */
val MENU_ROUND = 20.dp
/*
 * ⚠️ **7 e non 6, dalla 1.36**: la forma della `1.34` è passata (voce `striscia-forma`: *non è
 * proprio uguale, mi va bene lo stesso, ma a questo punto falla leggermente più spessa e sono a
 * posto*). È il secondo aumento di un punto: 5 nella `1.29`, 6 nella `1.34`, 7 adesso, e ogni
 * volta è stato lui a guardarla sul telefono. ⚠️ Il rientro sotto resta 2, quindi la striscia
 * cresce **verso l'alto** e continua ad appoggiarsi al fondo della scheda.
 */
private val MENU_STRIPE = 7.dp
private val MENU_STRIPE_SIDE = 4.dp

/**
 * Quanto la striscia si stacca dal fondo della scheda.
 *
 * ⚠️⚠️ **5 DALLA 1.37, ED È LA VIA DI MEZZO FRA DUE COSE CHE L'UTENTE HA VISTO SBAGLIATE**
 * (riscontro `striscia-sette`): 2 nel menu del cestino, *troppo vicina al margine inferiore*,
 * e 10 in quello della schermata iniziale, *troppo staccata*, perché là il `DropdownMenu` di
 * Material aggiunge i suoi 8 sotto il contenuto. Quegli 8 sono **misurati e non supposti**:
 * `MenuKt.getDropdownMenuVerticalPadding` ritorna `bipush 8` e finisce in un
 * `padding(vertical = ...)` intorno alla colonna del menu, letto nel bytecode di
 * `material3-android`.
 * ⚠️ **Non è la media aritmetica (che farebbe 6) ma il numero che sta in mezzo alle sue due
 * parole**: 'troppo vicina' a 2 e 'troppo staccata' a 10 non dicono che il giusto sia 6, e 5
 * è il gradino che tiene la striscia dentro la scheda senza appoggiarla al bordo. Resta una
 * **scelta**: se sul telefono non è quella, il numero da girare è questo, uno solo.
 */
private val MENU_STRIPE_UNDER = 5.dp

/**
 * Il padding verticale che il `DropdownMenu` di Material mette intorno alla propria colonna.
 *
 * ⚠️ **Sta qui perché è di Material e non nostro**: si passa a [MenuStripe] come `giu` dal solo
 * menu che quella superficie usa (il tastino della schermata iniziale), e serve a far cadere la
 * striscia a [MENU_STRIPE_UNDER] dal fondo come in tutti gli altri. ⚠️ Letto nel bytecode, non
 * supposto: vedi la nota di [MENU_STRIPE_UNDER]. Se un domani Material lo cambia, il sintomo è
 * la striscia di quel solo menu che si stacca o si incolla al bordo.
 */
val MENU_DROPDOWN_PAD = 8.dp

/**
 * Quanto sono stondati gli spigoli **in alto** della striscia.
 *
 * ⚠️ **Quasi vivi e non tondi**: nel mockup l'arrotondamento in alto è di un paio di pixel su
 * uno spessore di diciotto, cioè un accenno. Le punte tonde della `1.29` facevano leggere la
 * striscia come un nastro appoggiato sopra la scheda, mentre così è il fondo della scheda che
 * si colora.
 */
private val MENU_STRIPE_TOP = 2.dp

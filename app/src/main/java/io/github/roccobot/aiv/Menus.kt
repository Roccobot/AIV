package io.github.roccobot.aiv

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
 * ⚠️ **L'unica eccezione è [TapHoldMenuItem]**, e là sta scritto perché una voce con due
 * gesti non può essere un `DropdownMenuItem`.
 *
 * ⚠️⚠️ **STA IN UN FILE A SÉ dalla 0.75, quando i menu sono diventati due**: il secondo
 * avrebbe copiato la superficie, l'ombra e i tre numeri dell'animazione, e un'animazione
 * scritta in due posti diverge al primo ritocco. Quei numeri li ha scelti l'utente su un
 * mockup, quindi valgono per i menu, non per uno.
 * ⚠️ **Quello che NON è condiviso sono il posto e il raggio**, ed è la ragione per cui sono
 * parametri: il menu della selezione è quasi quadrato e ne vuole 16, quello del
 * visualizzatore è una lista larga e bassa e resta a 8.
 */
@Composable
fun MenuShell(
    /** Dove va il menu: [MenuCenter] o [MenuAbove]. */
    position: PopupPositionProvider,
    /** Il raggio degli angoli, che dipende dalla forma del contenuto. */
    corner: Dp,
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
            shape = RoundedCornerShape(corner),
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
            Column(modifier = Modifier.width(IntrinsicSize.Max)) { content() }
        }
    }
}

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
    ): IntOffset = IntOffset(
        x = (windowSize.width - popupContentSize.width) / 2,
        y = (windowSize.height - popupContentSize.height) / 2
    )
}

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
 * Una voce di menu con **due** gesti: tocco breve e tocco lungo.
 *
 * ⚠️⚠️ **NON È UN `DropdownMenuItem`, e non si poteva usare quello**: il suo `clickable` sta
 * **dentro**, in fondo alla propria catena di modificatori, quindi un `combinedClickable`
 * passato nel `modifier` gli finisce prima e non vede mai la pressione. Nella passata `Main`
 * il nodo più interno riceve per primo e consuma il down. È lo stesso meccanismo per cui
 * [TapHoldFab] non è un `SmallFloatingActionButton`, ed è la stessa conclusione: per avere
 * due gesti su un comando, il nodo che ascolta dev'essere **uno**.
 *
 * ⚠️⚠️ **LE MISURE SONO LETTE NEL BYTECODE DI `MenuKt`, NON RICORDATE** (`material3`
 * `1.5.0-alpha26`), perché questa voce sta in mezzo alle altre e una differenza di due pixel
 * si vede: altezza minima **48dp** (`MenuListItemContainerHeight`), rientro orizzontale
 * **12dp** (`DropdownMenuItemHorizontalPadding`), spazio fra icona e testo **8dp**
 * (`DropdownMenuIconTextPadding`, che diventa 12 solo con la resa da puntatore di
 * precisione, cioè col mouse), corpo **labelLarge**.
 * ⚠️ **I colori invece si CHIEDONO a Material** (`MenuDefaults.itemColors()`) invece di
 * essere copiati: quelli cambiano con la tavolozza, e una copia si scollerebbe al primo tema
 * diverso.
 *
 * @param holdLabel che cosa fa il tocco lungo, per il lettore di schermo. ⚠️ Senza, il gesto
 *   esiste solo per chi lo scopre per caso.
 */
@Composable
fun TapHoldMenuItem(
    text: String,
    icon: ImageVector,
    holdLabel: String,
    onTap: () -> Unit,
    onHold: () -> Unit
) {
    val colors = MenuDefaults.itemColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = MENU_ITEM_HEIGHT)
            .combinedClickable(
                onLongClickLabel = holdLabel,
                onLongClick = withHaptics(onHold),
                onClick = onTap
            )
            .padding(horizontal = MENU_ITEM_SIDE),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MENU_ITEM_GAP)
    ) {
        // ⚠️ L'icona non porta descrizione e il testo sì: `combinedClickable` fonde le
        // semantiche dei figli, quindi TalkBack legge una voce sola. Vedi `PadButton`.
        Icon(imageVector = icon, contentDescription = null, tint = colors.leadingIconColor)
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = colors.textColor
        )
    }
}

/** Le tre misure di una voce di menu, lette in `MenuKt`. Vedi [TapHoldMenuItem]. */
private val MENU_ITEM_HEIGHT = 48.dp
private val MENU_ITEM_SIDE = 12.dp
private val MENU_ITEM_GAP = 8.dp

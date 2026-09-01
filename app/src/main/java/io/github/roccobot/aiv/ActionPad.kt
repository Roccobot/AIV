package io.github.roccobot.aiv

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Le operazioni sui file, come riquadro di icone a tre colonne.
 *
 * ⚠️⚠️ **NASCE PER DUE POSTI INSIEME, e questa è la ragione per cui è un file a sé**: le
 * azioni della selezione nella griglia e quelle del tocco lungo nel visualizzatore sono le
 * **stesse sei**, e l'utente le ha chieste nella stessa forma. Due copie divergerebbero al
 * primo ritocco, e l'ordine dei tasti è precisamente la cosa che non deve cambiare fra una
 * schermata e l'altra: chi impara dove sta 'sposta' lo impara una volta.
 *
 * ⚠️ **L'ordine è quello dell'utente** (richiesta del 2026-08-30: *copia, sposta, elimina /
 * rinomina, condividi, info*) e non uno mio, quindi non si riordina 'per sicurezza': chi
 * volesse spostare 'elimina' lontano da 'sposta' cambierebbe una scelta, non un difetto.
 * ⚠️⚠️ **Ed è per QUELL'ordine che il colore dell'errore non è decorativo**: 'elimina' sta
 * accanto a 'sposta' invece di stare in fondo dopo una riga di separazione, come nel menu
 * che c'era prima, quindi il colore è l'unica cosa che la distingue dalla vicina. Chi lo
 * togliesse lascerebbe l'unica voce irreversibile identica a quelle che si possono disfare.
 *
 * ⚠️ **Icona più parola, e la parola non è un ripensamento**: l'utente ha chiesto icone, e
 * l'icona è quello che si riconosce a colpo d'occhio, ma 'copia' e 'sposta' hanno due glifi
 * che si somigliano, e fra sei tasti la parola minuta è quello che impedisce di sbagliare
 * mirando. Costa una riga di testo e non un tocco.
 */
@Composable
fun ActionPad(
    actions: List<PadAction>,
    modifier: Modifier = Modifier,
    columns: Int = PAD_COLUMNS,
    /**
     * Se le celle si dividono tutta la larghezza invece di misurare [PAD_CELL].
     *
     * ⚠️ **Serve alla bottomsheet della selezione, che è larga quanto lo schermo**: là
     * cinque celle da 76dp lascerebbero un vuoto a destra su un telefono largo e
     * sforerebbero su uno stretto. Nel menu del tocco lungo, che si apre attorno a un
     * tastino, la larghezza fissa resta quella giusta: là è il riquadro a doversi
     * adattare al contenuto, non il contrario.
     */
    stretch: Boolean = false
) {
    Column(
        modifier = modifier.padding(horizontal = PAD_EDGE, vertical = PAD_GAP),
        verticalArrangement = Arrangement.spacedBy(PAD_GAP)
    ) {
        // ⚠️ Le righe si ricavano a gruppi invece di essere scritte a mano: con sei azioni
        // fanno le due righe di tre del menu, con dieci le due da cinque della
        // bottomsheet, e con cinque l'ultima riga ne tiene due invece di lasciare un buco
        // da riempire con un tasto finto. Serve al cestino, dove 'rinomina' diventa
        // 'ripristina' e le voci possono non essere sei.
        for (row in actions.chunked(columns)) {
            Row(
                modifier = if (stretch) Modifier.fillMaxWidth() else Modifier,
                horizontalArrangement = Arrangement.spacedBy(PAD_GAP)
            ) {
                for (action in row) {
                    PadButton(action, if (stretch) Modifier.weight(1f) else Modifier.width(PAD_CELL))
                }
            }
        }
    }
}

/**
 * Le stesse operazioni, ma come **pannello che entra dal basso**: la selezione multipla.
 *
 * ⚠️⚠️ **NON È UNA `ModalBottomSheet`, ed è la richiesta a imporlo** (utente, 2026-08-31:
 * *mentre la bottomsheet è attiva, si deve poter agire sia sui suoi tasti che sulla
 * selezione*). Quella di Material mette un velo davanti a tutto il resto e si prende i
 * tocchi, quindi con lei aperta non si potrebbe più aggiungere una fotografia alla
 * selezione: sarebbe la contraddizione esatta della cosa chiesta. Qui è una `Surface`
 * appoggiata in fondo al `Box` della schermata, che occupa il posto suo e basta.
 * ⚠️ **Il tastino della selezione se n'è andato con lei** (stessa istruzione: *il FAB di
 * selezione non serve più*), e la ragione l'ha trovata l'utente: se il menu si apre da sé,
 * un tastino che lo apre non ha più niente da fare.
 * ⚠️ **La maniglia non trascina**: è un segno, non un comando. Il pannello si chiude col
 * tasto Indietro, come chiesto, e si riapre da sé quando la selezione riparte. Farla
 * trascinabile vorrebbe dire un gesto in più che compete con lo scorrimento della griglia
 * sotto, e nessuno l'ha chiesto.
 */
@Composable
fun BoxScope.PickSheet(visible: Boolean, actions: List<PadAction>, onHeight: (Int) -> Unit = {}) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        Surface(
            /*
             * ⚠️⚠️ **L'ALTEZZA SI MISURA E NON SI STIMA, e serve alla griglia sotto**: senza
             * il numero vero, l'ultima fila di fotografie resterebbe sotto il pannello e
             * nessuno scorrimento la porterebbe fuori. Una costante scritta a mano
             * sbaglierebbe il giorno che un'etichetta va a capo in una lingua lunga, che è
             * esattamente il caso in cui il pannello cresce.
             */
            modifier = Modifier.onSizeChanged { onHeight(it.height) },
            shape = RoundedCornerShape(topStart = SHEET_CORNER, topEnd = SHEET_CORNER),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = SHEET_LIFT,
            shadowElevation = SHEET_LIFT
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = PAD_GAP)
                        .size(width = GRIP_WIDE, height = GRIP_TALL)
                        .clip(RoundedCornerShape(GRIP_TALL))
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                ActionPad(actions = actions, columns = SHEET_COLUMNS, stretch = true)
            }
        }
    }
}

/**
 * Un'azione del riquadro: l'icona, la parola, e se è quella da cui non si torna.
 *
 * ⚠️ [danger] non è 'importante': è **irreversibile**. Vale per l'eliminazione, e per
 * niente che si possa disfare con l'operazione contraria.
 * ⚠️⚠️ **[onHold] È NULL PER QUASI TUTTE, dalla 0.79**: il tocco lungo su un tasto del
 * riquadro è una scorciatoia in più, e per adesso ce l'ha la sola 'Copia' (duplica dove sei).
 * Una scorciatoia su ogni tasto sarebbe sei gesti nascosti da imparare, e nessuno li scopre.
 */
class PadAction(
    val icon: ImageVector,
    @StringRes val label: Int,
    val danger: Boolean = false,
    /**
     * Se il tasto si può premere adesso.
     *
     * ⚠️⚠️ **SPENTO E NON NASCOSTO, ed è la ragione per cui è nato** (bottomsheet dell'editor,
     * 1.17): 'Applica' e 'Annulla' non hanno sempre qualcosa da fare, e una fila che perde e
     * riacquista tasti si riordina sotto le dita, cioè sposta gli altri proprio mentre li si
     * mira. Spento, il posto resta suo e si vede che esiste.
     * ⚠️ **Nelle sei azioni sui file non lo usa nessuno**, e va bene: là un'operazione o c'è
     * per tutta la selezione o non c'è la voce.
     */
    val enabled: Boolean = true,
    /**
     * Che cosa fa il tocco lungo, e `null` quando non fa niente.
     *
     * ⚠️ Va **insieme** a [holdLabel]: un gesto che il lettore di schermo non annuncia esiste
     * solo per chi lo scopre per caso.
     */
    val onHold: (() -> Unit)? = null,
    @StringRes val holdLabel: Int? = null,
    val onClick: () -> Unit
)

/**
 * Un tasto del riquadro.
 *
 * ⚠️ **L'icona non porta descrizione e il testo sì**: `clickable` fonde le semantiche dei
 * figli, quindi TalkBack legge una voce sola. Descrivendo anche l'icona la leggerebbe due
 * volte, che è il difetto già evitato nelle copertine delle cartelle.
 * ⚠️ **Il tocco sta sull'intera colonna**, non sull'icona: un bersaglio di 24dp si manca,
 * e qui i tasti sono sei e vicini. La cella è larga [PAD_CELL] e alta quanto icona più
 * parola, che è l'area minima toccabile di Material.
 */
@Composable
private fun PadButton(action: PadAction, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val hold = remember(action.onHold, haptics) {
        action.onHold?.let { premuto ->
            {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                premuto()
            }
        }
    }
    val full =
        if (action.danger) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurface
    // ⚠️ Lo spento è il colore di sempre a un terzo, che è il valore di Material per un
    // comando inattivo: un grigio scritto a mano andrebbe bene in un tema e non nell'altro.
    val tint = if (action.enabled) full else full.copy(alpha = OFF_INK)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(PAD_CORNER))
            // ⚠️⚠️ **`combinedClickable` SEMPRE, anche senza tocco lungo**: con
            // `onLongClick` a null si comporta come un `clickable`, quindi un `if` fra i due
            // modificatori sarebbe due catene da tenere d'accordo per niente.
            .combinedClickable(
                enabled = action.enabled,
                onLongClickLabel = action.holdLabel?.let { stringResource(it) },
                // ⚠️ Qui il gesto può non esserci, quindi la vibrazione si compone a mano
                // invece di passare da [withHaptics]: vedi la sua nota.
                onLongClick = hold,
                onClick = action.onClick
            )
            .padding(vertical = PAD_GAP),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PAD_LABEL_GAP)
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(PAD_ICON)
        )
        Text(
            text = stringResource(action.label),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            textAlign = TextAlign.Center,
            // ⚠️ Due righe e non una: fra le lingue che stanno per arrivare ce ne sono
            // di più lunghe dell'italiano, e una parola tagliata a metà in un tasto di
            // icone lascia il tasto senza nome. Le celle di una riga si allineano in
            // alto, quindi una parola che va a capo allunga la sua colonna e non
            // scompagina le icone.
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Lo stesso gesto, con la vibrazione breve del sistema davanti.
 *
 * ⚠️⚠️ **COMPOSE NON VIBRA DA SÉ SUL TOCCO LUNGO, e questa è la differenza con le View di
 * Android**, dove `setOnLongClickListener` lo fa per conto suo quando il richiamo risponde
 * `true`. `combinedClickable` no: il gesto arriva muto, e su un telefono un tocco lungo che
 * non si sente non si distingue da un tocco lungo non riuscito. Richiesta dell'utente,
 * 2026-09-01: *feedback aptico in tutti gli eventi a pressione lunga*.
 * ⚠️ **Sta qui e non in dieci punti**, ed è la ragione per cui è una funzione: i tocchi
 * lunghi dell'app sono sette in cinque file, e il giorno che ne nasce l'ottavo lo prende
 * anche lui se passa di qui. Un `performHapticFeedback` copiato sette volte se lo dimentica
 * l'ottavo.
 * ⚠️ **`LongPress` e non `TextHandleMove`**: sono due vibrazioni diverse del sistema, e la
 * prima è quella che Android usa per questo gesto dappertutto. La seconda, più leggera, la
 * griglia la usa apposta per il tocco che **aggiunge** una foto alla selezione, che è un
 * gesto ripetuto: là una vibrazione piena a ogni foto sarebbe un martello.
 * ⚠️ **Prende un gesto che C'È**: l'unico punto in cui il tocco lungo è opzionale è
 * [PadButton], e là la vibrazione si scrive sul posto. Una funzione nullabile in entrata e in
 * uscita avrebbe costretto tutti gli altri, che il gesto ce l'hanno, a spiegare al
 * compilatore che non è nullo.
 */
@Composable
fun withHaptics(action: () -> Unit): () -> Unit {
    val haptics = LocalHapticFeedback.current
    return remember(action, haptics) {
        {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            action()
        }
    }
}

/** Quante colonne ha il riquadro: tre, come l'utente le ha chieste. */
private const val PAD_COLUMNS = 3

/** Quanto resta di un tasto spento: il valore di Material per un comando inattivo. */
private const val OFF_INK = 0.38f

/**
 * La larghezza di una cella.
 *
 * ⚠️ Tre celle più i distacchi fanno poco meno di 250dp, che sta dentro uno schermo da
 * 360dp con il margine del menu: è il vincolo che decide questo numero, non l'estetica.
 */
private val PAD_CELL = 76.dp

/** Il lato dell'icona: quella di un tasto, non quella di una barra. */
private val PAD_ICON = 24.dp

/** Il distacco fra le celle, e il respiro dentro ognuna. */
private val PAD_GAP = 8.dp

/** Quanto stacca la parola dalla sua icona: poco, perché sono la stessa cosa. */
private val PAD_LABEL_GAP = 2.dp

/** Il margine laterale del riquadro dentro il menu che lo contiene. */
private val PAD_EDGE = 4.dp

/** Lo smusso dell'alone del tocco su una cella. */
private val PAD_CORNER = 10.dp

/**
 * Quante colonne ha la bottomsheet della selezione: **cinque**, come chieste.
 *
 * ⚠️ Cinque e non tre come il menu, e non è simmetria: le azioni là sono dieci, e a tre
 * colonne verrebbero quattro file, cioè un pannello alto quanto mezzo schermo sopra le
 * fotografie che si stanno scegliendo.
 * ⚠️ **Non è privata perché la legge anche chi ROVESCIA le file** per la mano sinistra
 * (`GridScreen`): là serve sapere dove finisce una fila, e un 5 scritto una seconda volta
 * sarebbe il numero che un giorno diverge da questo.
 */
internal const val SHEET_COLUMNS = 5

/** Lo smusso dei due angoli alti del pannello, che è quello di una bottomsheet Material. */
private val SHEET_CORNER = 28.dp

/**
 * Quanto il pannello si stacca da quello che ha sotto.
 *
 * ⚠️ Serve **doppio**, di tono e di ombra: il tono lo distingue dal fondo nel tema chiaro,
 * dove un'ombra sola sparisce, e l'ombra nel tema scuro, dove i toni si somigliano tutti.
 */
private val SHEET_LIFT = 6.dp

/** La maniglia: larga abbastanza da leggersi come un segno, bassa abbastanza da non pesare. */
private val GRIP_WIDE = 32.dp
private val GRIP_TALL = 4.dp

/**
 * Lo smusso del tastino quadrato, uguale in tutte le schermate.
 *
 * ⚠️ Quadrato ma non tagliente: il tondo pieno griderebbe 'azione principale', e in questa
 * app l'azione principale sono sempre le fotografie. ⚠️ **Sta qui e non in una schermata**
 * perché i tastini sono due, quello delle cartelle e quello della selezione, e due numeri
 * uguali scritti in due file sono un numero che prima o poi diverge.
 */
val FAB_CORNER = 12.dp

/**
 * La misura di `SmallFloatingActionButton`, che [TapHoldFab] rifà a mano.
 *
 * ⚠️ È anche l'altezza che [FAB_REACH] somma al margine: i due numeri descrivono lo stesso
 * tastino, e slegati si sarebbero mossi uno per volta.
 */
val FAB_SIZE = 40.dp

/**
 * L'ombra di serie di un tastino galleggiante in Material 3.
 *
 * ⚠️ Una sola per tutte e due le schermate dalla `0.78`: i tastini sono due e l'ombra di un
 * tastino non è una scelta di schermata.
 */
val FAB_LIFT = 6.dp

/**
 * Il margine del tastino quadrato dalle due sponde della schermata delle cartelle.
 *
 * ⚠️ Sta qui e non là perché [FAB_REACH] lo somma: il giorno che il tastino si sposta di un
 * dp, il conto che tiene le cartelle sopra di lui deve muoversi con lui.
 */
val HUB_PAD = 16.dp

/**
 * Quanto arriva in su il tastino quadrato delle cartelle, misurato dal fondo dello schermo:
 * il suo margine ([HUB_PAD]) più la sua altezza.
 *
 * ⚠️⚠️ **È LA Y DA CUI PARTE LA SFUMATURA che inghiotte quello che sta sotto** (richiesta
 * dell'utente, dalla `0.77`). Comincia dove comincia il **tastino**, non dove finisce lo
 * spazio che gli si lascia, che è [BELOW_FAB] e vale una ventina di dp in più: la differenza
 * fra i due numeri è l'aria che al riposo resta fra l'ultima cartella e il tastino, e la
 * sfumatura deve trovarla vuota.
 * ⚠️ L'altezza è [FAB_SIZE], cioè la misura che Material dà a un tastino piccolo senza
 * esporla come costante pubblica: è un dato suo, non una nostra scelta.
 */
val FAB_REACH = HUB_PAD + FAB_SIZE

/**
 * Quanto spazio resta sotto l'ultimo elemento di una griglia, perché il tastino non gli si
 * sieda sopra.
 *
 * ⚠️ Serve **solo** quando il tastino c'è: nella griglia delle foto compare con la
 * selezione, quindi il fondo cresce da quel momento. Senza, la fotografia in basso a
 * destra resterebbe coperta proprio mentre si sta scegliendo, cioè quando la si deve poter
 * toccare.
 * ⚠️ **Scritto come [FAB_REACH] più aria** dalla `0.77`, e prima era 76dp nudi: i due numeri
 * descrivono la stessa cosa a due altezze diverse, e slegati si sarebbero mossi uno per
 * volta.
 */
val BELOW_FAB = FAB_REACH + 20.dp

/**
 * Un tastino galleggiante con **due** gesti: tocco breve e tocco lungo.
 *
 * ⚠️⚠️ **NON È `SmallFloatingActionButton`, e non è un capriccio**: quel composabile prende
 * un `onClick` solo, e il `modifier` che gli si passa finisce **fuori** dal suo `clickable`,
 * cioè come genitore. Un `combinedClickable` messo là non vedrebbe mai il tocco lungo, perché
 * nella passata `Main` il figlio consuma il down per primo: è esattamente il meccanismo che
 * aveva rotto il tocco lungo sulla griglia. Per avere due gesti su un tastino bisogna che di
 * nodo che ascolta ce ne sia **uno**.
 * ⚠️ **La resa non cambia**: `SmallFloatingActionButton` è una `Surface` da [FAB_SIZE] con
 * `primaryContainer`, il suo contrasto e 6dp d'ombra, e questa è quella. L'unica cosa che si
 * perde è l'ombra che cresce al passaggio del **mouse**, che su un telefono non succede: in
 * Material 3 la pressione lascia l'ombra dov'è.
 * ⚠️ Il gesto sta **dentro** la `Surface` e non sul suo modificatore, così l'increspatura
 * prende il colore del contenuto ([ink]) invece di quello che c'era fuori.
 *
 * ⚠️⚠️ **STA QUI, CONDIVISO, DALLA 0.78**: i tastini col tocco lungo sono diventati **due**,
 * quello della selezione e quello quadrato delle cartelle, e differiscono per il **glifo** e
 * per quello che i due gesti fanno. Tutto il resto (misura, smusso, ombra, il nodo unico che
 * ascolta, l'etichetta del tocco lungo per il lettore di schermo) è la stessa cosa scritta
 * una volta.
 */
@Composable
fun TapHoldFab(
    icon: ImageVector,
    /** Che cos'è il tastino, per il lettore di schermo: la sua azione breve. */
    label: String,
    container: Color,
    ink: Color,
    lift: Dp,
    /**
     * Che cosa fa il tocco lungo, per il lettore di schermo.
     *
     * ⚠️ **Si DICHIARA, o resta una scorciatoia che esiste solo per chi vede il velo**:
     * l'etichetta la legge il lettore di schermo fra le azioni disponibili sul tastino.
     * ⚠️ Arriva da fuori perché il gesto fa cose diverse a seconda della schermata e di dove
     * si è dentro di lei, e un'etichetta fissa ne annuncerebbe una mentre succede l'altra.
     */
    holdLabel: String,
    onTap: () -> Unit,
    onHold: () -> Unit
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
                onLongClickLabel = holdLabel,
                onLongClick = withHaptics(onHold),
                onClick = onTap
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
    }
}

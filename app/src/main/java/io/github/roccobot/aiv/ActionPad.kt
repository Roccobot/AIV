package io.github.roccobot.aiv

import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
     * FAB, la larghezza fissa resta quella giusta: là è il riquadro a doversi
     * adattare al contenuto, non il contrario.
     */
    stretch: Boolean = false,
    /**
     * Se sotto ogni icona si legge la parola.
     *
     * ⚠️ **Arriva da [LocalPadLook] e non da un parametro obbligatorio**, per la stessa ragione
     * del velo: questi riquadri vivono dentro finestre che le impostazioni non ricevono, e la
     * catena per portarci un booleano attraversa cinque schermate. Chi ha una ragione per
     * ignorare l'interruttore lo passa a mano.
     */
    labels: Boolean = LocalPadLook.current.labels
) {
    /*
     * ⚠️⚠️ **IL RIQUADRO DISTESO SI MISURA PRIMA DI DISEGNARSI, DALLA `2.02`**: gli serve la
     * larghezza vera per sapere se le sue colonne ci stanno (vedi [cella] in [PadBody]).
     * ⚠️⚠️ **E IL RAMO A LARGHEZZA FISSA NON PASSA DI QUI, di proposito**: `BoxWithConstraints`
     * è un `SubcomposeLayout`, e una subcomposizione dentro una misura **intrinseca** non si
     * può fare. I menu chiedono proprio quella (la loro larghezza la fanno le voci di testo
     * sopra il riquadro), quindi là si va dritti al corpo, con la cella piena di sempre.
     */
    if (stretch) {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            PadBody(
                actions = actions,
                columns = columns,
                stretch = true,
                labels = labels,
                // ⚠️ **Al netto dei due fianchi**, che il corpo mette come padding: è la
                // larghezza che le celle possono davvero dividersi.
                room = maxWidth - PAD_EDGE * 2
            )
        }
    } else {
        PadBody(
            actions = actions,
            modifier = modifier,
            columns = columns,
            stretch = false,
            labels = labels,
            room = Dp.Unspecified
        )
    }
}

/**
 * Il corpo del riquadro: le righe di celle, con la larghezza già decisa da chi lo chiama.
 *
 * @param room quanto spazio hanno le celle in tutto, o [Dp.Unspecified] dove non si misura.
 */
@Composable
private fun PadBody(
    actions: List<PadAction>,
    columns: Int,
    stretch: Boolean,
    labels: Boolean,
    room: Dp,
    modifier: Modifier = Modifier
) {
    /*
     * ⚠️⚠️ **IL FIANCO SINISTRO NON È PIÙ [PAD_EDGE], dalla `1.59`: È DERIVATO DALLA COLONNA
     * DELLE ICONE DELLA LISTA** ([MENU_ICON_MID]), perché sopra questo riquadro, nei menu, ci
     * sono voci in lista e le loro icone devono cadere sulla stessa verticale della prima
     * colonna (richiesta dell'utente con una schermata e una riga tracciata sopra). Il conto è
     * quello e non un numero a occhio: il centro della cella dista mezza cella dal suo bordo,
     * quindi il bordo va messo mezza cella prima del punto in cui il centro deve cadere.
     * ⚠️ **Col riquadro a etichette il conto darebbe un rientro NEGATIVO** (la cella è 76 e il
     * punto è a 31, quindi servirebbero -7), cioè la cella dovrebbe cominciare fuori dal
     * pannello: là il pavimento riporta a [PAD_EDGE] e le due colonne restano quelle di prima.
     * Non è una rinuncia mascherata: con le parole sotto il riquadro è una griglia di
     * piastrelle, e una piastrella non ha nessun motivo di allinearsi all'icona di una riga.
     * ⚠️⚠️ **E VALE SOLO NEI MENU, DALLA `1.81`: nelle due schede il fianco torna [PAD_EDGE]**
     * (censimento della UI del 2026-09-05). Il conto esiste per allinearsi alle icone delle
     * **righe in lista sopra il riquadro**, e quelle ci sono nei due menu; nella scheda della
     * selezione e nelle due file dell'editor sopra non c'è nessuna lista, quindi con le parole
     * spente il riquadro veniva 7dp da sinistra e 4 da destra, cioè un'asimmetria visibile che
     * non allineava niente. ⚠️ **La distinzione è [stretch] e non [labels]**: in un menu con le
     * parole spente l'allineamento serve ancora, ed è là che è stato chiesto.
     */
    val piena = if (labels) PAD_CELL else PAD_CELL_BARE
    /*
     * ⚠️⚠️ **LA CELLA SI STRINGE SE LE COLONNE NON CI STANNO, DALLA `2.02`, E IL CONTO È
     * QUELLO DELLA REPLICA** ([PadArrange], che lo faceva da sempre): cinque celle da 76dp più
     * i distacchi fanno 412dp, cioè più di uno schermo da 360, e senza questa riga la fila
     * usciva dal vetro invece di restringersi. Si vedeva già oggi nella scheda della selezione
     * con le parole accese, e la prima fila dell'editor lo avrebbe fatto sempre da quando ha
     * cinque tasti.
     * ⚠️ **Non cambia niente dove c'è posto**, perché il tetto resta [PAD_CELL]: una fila che
     * entra è larga come prima, cifra per cifra.
     * ⚠️⚠️ **VALE SOLO CON [stretch], e non è una prudenza**: là il riquadro riempie la
     * larghezza, quindi il vincolo che arriva è quello vero. Dove la larghezza è **intrinseca**
     * (i menu) il vincolo può essere illimitato, e un conto fatto su quello darebbe una cella
     * infinita: là si tiene la misura piena, che è quella che il pannello usa per dimensionarsi.
     */
    val cella = if (stretch && room != Dp.Unspecified) {
        ((room - PAD_GAP * (columns - 1)) / columns).coerceAtMost(piena)
    } else {
        piena
    }
    val avvio =
        if (stretch) PAD_EDGE else (MENU_ICON_MID - cella / 2).coerceAtLeast(PAD_EDGE)
    Column(
        modifier = modifier.padding(start = avvio, end = PAD_EDGE, top = PAD_GAP, bottom = PAD_GAP),
        verticalArrangement = Arrangement.spacedBy(PAD_GAP)
    ) {
        /*
         * ⚠️⚠️ **SENZA LE PAROLE LA CELLA SI STRINGE, e senza questa riga il riquadro
         * compatto non sarebbe compatto**: [PAD_CELL] è larga quanto serve a una PAROLA, non a
         * un glifo, e con le sole icone lascerebbe due terzi di aria fra l'una e l'altra. Il
         * perché di quei 76dp sta sulla costante.
         * ⚠️ **Riguarda solo i riquadri a larghezza fissa**: dove le celle si dividono la
         * larghezza (`stretch`) non c'è niente da stringere, e a stringersi è la scheda.
         */
        /*
         * ⚠️⚠️ **SENZA ETICHETTE LE CELLE SI DISTRIBUISCONO SULLA LARGHEZZA, dalla `1.57`**
         * (riscontro dell'utente, giro della `1.56`, con schermata: *lì vanno ridistribuite
         * sullo spazio disponibile, o va ristretto il popup*). Il difetto era di misura, non di
         * disegno: la larghezza del menu la fanno le righe di testo sopra il riquadro, e con le
         * sole icone le celle scendevano a 48dp e restavano ammucchiate a sinistra con mezzo
         * pannello di aria a destra.
         * ⚠️ **Distribuire è la metà che costa zero**; stringere il menu è l'altra metà, e
         * quella costa: la larghezza è l'intrinseca delle voci di testo, quindi tagliarla manda
         * a capo 'Copia immagine' nelle lingue lunghe. Il riquadro compatto si ottiene lo
         * stesso, e il menu resta leggibile in tutte e ventotto.
         * ⚠️⚠️ **A DISTRIBUIRE È LO SPAZIO FRA LE CELLE E NON LA CELLA, dalla `1.59`, e il
         * cambio serve all'allineamento**: con celle a peso uguale il centro della prima è una
         * **frazione** della larghezza del pannello, quindi si sposta con la lingua e non può
         * stare su una verticale fissa. Con celle di misura fissa e il vuoto in mezzo che
         * cresce, il centro della prima dipende solo dal fianco, che è quello che [avvio]
         * fissa.
         * ⚠️ **Le colonne restano allineate fra le righe** perché i vuoti sono tutti uguali, e
         * lo restano anche nell'ultima riga corta, dove i posti mancanti li tengono degli
         * spaziatori larghi come una cella: `SpaceBetween` distribuisce fra **tutti** i figli,
         * e figli tutti della stessa larghezza cadono sulle stesse colonne.
         */
        val disteso = stretch || !labels
        // ⚠️ Il minimo resta la misura naturale del riquadro: dentro una colonna a larghezza
        // intrinseca un figlio che chiede tutta la larghezza non ne dichiara nessuna, e senza
        // questo pavimento un menu di sole icone si accartoccerebbe.
        val minimo = cella * columns + PAD_GAP * (columns - 1)
        // ⚠️ Le righe si ricavano a gruppi invece di essere scritte a mano: con sei azioni
        // fanno le due righe di tre del menu, con dieci le due da cinque della
        // bottomsheet, e con cinque l'ultima riga ne tiene due invece di lasciare un buco
        // da riempire con un tasto finto. Serve al cestino, dove 'rinomina' diventa
        // 'ripristina' e le voci possono non essere sei.
        for (row in actions.chunked(columns)) {
            Row(
                modifier = if (disteso) Modifier.fillMaxWidth().widthIn(min = minimo) else Modifier,
                horizontalArrangement =
                    if (disteso) Arrangement.SpaceBetween else Arrangement.spacedBy(PAD_GAP)
            ) {
                for (action in row) {
                    PadButton(action = action, modifier = Modifier.width(cella), labels = labels)
                }
                /*
                 * ⚠️⚠️ **L'ULTIMA RIGA CORTA SI RIEMPIE DI POSTI VUOTI, o le sue celle si
                 * spargono sulla larghezza e le colonne non si allineano più con la riga
                 * sopra.** Succede dove le voci non sono un multiplo delle colonne, cioè nel
                 * cestino, che ne ha cinque su tre.
                 * ⚠️ **Larghi come una cella e non elastici**: `SpaceBetween` mette lo stesso
                 * vuoto fra tutti i figli, quindi figli tutti uguali cadono sulle colonne di
                 * sopra. Uno spaziatore elastico se le prenderebbe tutto lo spazio avanzato e
                 * spingerebbe le celle vere ai due estremi.
                 */
                if (disteso) {
                    repeat(columns - row.size) { Spacer(Modifier.width(cella)) }
                }
            }
        }
    }
}

/**
 * Lo stesso riquadro, ma per **cambiare l'ordine invece di eseguire**: la replica che vive
 * nelle impostazioni.
 *
 * ⚠️⚠️ **NASCE DA UNA BOCCIATURA, E LA RAGIONE VALE PIÙ DELLA CORREZIONE** (riscontro
 * dell'utente, giro della `1.56`: *i tasti sono disposti in orizzontale nella vera UI, non mi
 * piace dover ragionare con un trascinamento verticale ... voglio agire su un oggetto che
 * somiglia al vero menu*). La `1.56` metteva un **elenco** di righe, e un elenco chiede di
 * tradurre a mente 'terzo dall'alto' in 'seconda colonna della prima riga'. La cosa che si
 * riordina è un riquadro a griglia, quindi lo strumento per riordinarlo è quel riquadro.
 *
 * ⚠️⚠️ **LE CELLE SONO TUTTE UGUALI, ED È QUELLO CHE RENDE IL CONTO ESATTO**: sapendo il passo
 * di una colonna e di una riga, il posto d'arrivo è la posizione del dito divisa per il passo
 * e arrotondata. Con celle di misure diverse servirebbe misurarle una per una a ogni pixel di
 * trascinamento.
 * ⚠️ **La larghezza si stringe se non ci sta**: dieci tasti in cinque colonne da 76dp fanno
 * 412dp, che su un telefono non entrano. È la stessa cosa che fa il riquadro vero nella scheda
 * della selezione (`stretch`), quindi la replica somiglia al modello anche in questo.
 *
 * ⚠️⚠️ **IL RIFLUSSO È LA COSA CHE SI GUARDA MENTRE SI TRASCINA** (sua richiesta: *se sposto un
 * pulsante in posizione 2 i successivi devono scorrere in avanti, e uno andrà a capo*). Non è
 * uno scambio fra due caselle: la lista si ricompone a ogni pixel con il tasto preso tolto e
 * rimesso al posto d'arrivo, quindi quello che si vede **è già** il risultato, andata a capo
 * compresa.
 *
 * ⚠️ **Niente maniglia** (sua indicazione: *non credo che serva*): il gesto è il tocco lungo,
 * che è già quello con cui si apre il riquadro vero, e ogni tasto porta il fondo tondo che
 * dice 'questo si prende'. La riga di istruzioni sopra il riquadro lo scrive comunque, perché
 * un gesto senza segno visibile non lo prova nessuno.
 *
 * ⚠️⚠️ **LE PAROLE QUI CI SONO SEMPRE, ANCHE A 'Etichette sotto le icone' SPENTA, ED È UNA
 * SCELTA CHE FINO ALLA `1.80` NON ERA SCRITTA** (censimento della UI del 2026-09-05, dove il
 * rilievo chiede l'argomento). Le tre ragioni, in ordine di peso:
 * - **Qui un tasto lo si NOMINA per spostarlo**, e la parola è la sola cosa che dice quale si
 *   sta prendendo. È anche lo stesso testo che alimenta le azioni parlate di questa pagina e i
 *   testi con cui la ricerca delle impostazioni la trova.
 * - **La cella ha un'altezza fissa** ([ARRANGE_HIGH]), che serve al conto del passo: senza le
 *   parole resterebbe un glifo in mezzo all'aria, cioè una cella che somiglia **meno** al
 *   riquadro vero, non più.
 * - **La somiglianza che ha chiesto riguarda la DISPOSIZIONE**: griglia orizzontale, riflusso
 *   sotto il dito, andata a capo dove va a capo il modello (*voglio agire su un oggetto che
 *   somiglia al vero menu*, giro della `1.56`). Di quella non si perde niente.
 *
 * @param columns quante colonne ha il riquadro **vero**, non quante ne stanno qui: la replica
 *   deve rompere le righe dove le rompe il modello.
 */
@Composable
fun PadArrange(
    order: List<PadKey>,
    columns: Int,
    onOrder: (List<PadKey>) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val prima = stringResource(R.string.settings_buttons_before)
    val dopo = stringResource(R.string.settings_buttons_after)
    val righe = (order.size + columns - 1) / columns
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val larga = ((maxWidth - PAD_GAP * (columns - 1)) / columns).coerceAtMost(PAD_CELL)
        val passoX = with(LocalDensity.current) { (larga + PAD_GAP).toPx() }
        val passoY = with(LocalDensity.current) { (ARRANGE_HIGH + PAD_GAP).toPx() }

        var preso by remember { mutableStateOf<PadKey?>(null) }
        var scarto by remember { mutableStateOf(Offset.Zero) }

        /** Dove sta la cella numero [i], in pixel dal vertice del riquadro. */
        fun posto(i: Int) = Offset((i % columns) * passoX, (i / columns) * passoY)

        /*
         * ⚠️⚠️ **SI LEGGE DALLO STATO VIVO, e questa funzione esiste per quello**: la chiamano
         * sia la composizione (per disegnare il riflusso) sia la fine del trascinamento, che
         * vive dentro un `pointerInput` ricordato. Un indice calcolato una volta e chiuso
         * dentro quel blocco resterebbe quello del primo fotogramma, ed è il difetto che il
         * riordino a elenco ha già avuto una volta.
         */
        fun bersaglio(da: Int): Int {
            if (da < 0) return -1
            val p = posto(da) + scarto
            val col = (p.x / passoX).roundToInt().coerceIn(0, columns - 1)
            val rig = (p.y / passoY).roundToInt().coerceIn(0, righe - 1)
            return (rig * columns + col).coerceIn(0, order.lastIndex)
        }

        val da = order.indexOf(preso)
        val a = bersaglio(da)
        val visto = if (da < 0 || a < 0) order else order.moved(da, a)

        Box(modifier = Modifier.height(ARRANGE_HIGH * righe + PAD_GAP * (righe - 1))) {
            for (chiave in order) {
                val suo = chiave == preso
                val meta = if (suo) posto(da) + scarto else posto(visto.indexOf(chiave))
                /*
                 * ⚠️ **Un `Animatable` e non un `animate*AsState`**: quello insegue sempre, e
                 * al rilascio il tasto salterebbe dal dito alla casella. Qui mentre il dito è
                 * giù si fa `snapTo`, e appena si stacca l'animazione parte **da dove il dito
                 * l'ha lasciato**, che è la differenza fra un riordino fluido e uno a scatti.
                 */
                val moto = remember(chiave) { Animatable(posto(order.indexOf(chiave)), Offset.VectorConverter) }
                LaunchedEffect(meta, suo) {
                    if (suo) moto.snapTo(meta)
                    else moto.animateTo(meta, spring(stiffness = Spring.StiffnessMediumLow))
                }
                val tinta = MaterialTheme.colorScheme.primary
                /*
                 * ⚠️⚠️ **IL TASTO IRREVERSIBILE PORTA IL COLORE DELL'ERRORE ANCHE QUI, DALLA
                 * `1.81`** (censimento della UI del 2026-09-05): nel riquadro vero quel colore
                 * è l'unica cosa che distingue 'elimina' dalla vicina 'sposta' (vedi la nota in
                 * testa a [ActionPad]), e una replica che lo perde smette di somigliare al
                 * modello proprio sulla voce in cui somigliare conta.
                 * ⚠️ **Il rosso si legge, ed è misurato e non supposto**: il fondo della cella
                 * è l'accento al 10% ([ARRANGE_BED]) sopra la superficie del pannello, non una
                 * tinta piena, quindi non c'era nessun conflitto da evitare.
                 */
                val inchiostro =
                    if (chiave.danger()) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                Box(
                    modifier = Modifier
                        .offset { IntOffset(moto.value.x.roundToInt(), moto.value.y.roundToInt()) }
                        // ⚠️ Il tasto preso sta sopra gli altri, o passerebbe sotto il vicino
                        // proprio nel momento in cui lo scavalca.
                        .zIndex(if (suo) 1f else 0f)
                        .size(width = larga, height = ARRANGE_HIGH)
                        .clip(RoundedCornerShape(PAD_CORNER))
                        .background(tinta.copy(alpha = if (suo) ARRANGE_HELD else ARRANGE_BED))
                        .pointerInput(chiave, order, columns, passoX, passoY) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    preso = chiave
                                    scarto = Offset.Zero
                                    haptics.performHapticFeedback(HOLD_BUZZ)
                                },
                                onDrag = { evento, delta ->
                                    evento.consume()
                                    scarto += delta
                                },
                                onDragEnd = {
                                    val partenza = order.indexOf(chiave)
                                    val fine = bersaglio(partenza)
                                    if (fine >= 0 && fine != partenza) {
                                        onOrder(order.moved(partenza, fine))
                                    }
                                    preso = null
                                    scarto = Offset.Zero
                                },
                                onDragCancel = {
                                    preso = null
                                    scarto = Offset.Zero
                                }
                            )
                        }
                        .semantics {
                            // ⚠️ Il trascinamento con un lettore di schermo non si fa: queste
                            // due azioni sono l'unica via al riordino per chi non vede, ed è
                            // la stessa rete che ha l'elenco dei campi delle info.
                            customActions = listOf(
                                CustomAccessibilityAction(prima) {
                                    val i = order.indexOf(chiave)
                                    onOrder(order.moved(i, i - 1)); true
                                },
                                CustomAccessibilityAction(dopo) {
                                    val i = order.indexOf(chiave)
                                    onOrder(order.moved(i, i + 1)); true
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(PAD_LABEL_GAP, Alignment.CenterVertically)
                    ) {
                        Icon(
                            imageVector = chiave.glyph(),
                            contentDescription = null,
                            tint = inchiostro,
                            modifier = Modifier.size(PAD_ICON)
                        )
                        Text(
                            text = stringResource(chiave.label()),
                            style = padLabel(),
                            color = inchiostro,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
 * ⚠️ **Il FAB della selezione se n'è andato con lei** (stessa istruzione: *il FAB di
 * selezione non serve più*), e la ragione l'ha trovata l'utente: se il menu si apre da sé,
 * un FAB che lo apre non ha più niente da fare.
 * ⚠️⚠️ **LA MANIGLIA NON C'È PIÙ, dalla 1.42, e la regola che l'ha tolta è generale**
 * (riscontro `niente-ombre`: *togli anche il tratto-manopola, a meno che non sia interattivo,
 * lo si può trascinare*). Questa non si trascinava: era un segno che diceva 'qui c'è un
 * pannello', cioè una promessa di gesto che il pannello non manteneva. ⚠️ **La ragione per cui
 * non si trascinava resta valida** e va saputa da chi pensasse di renderla vera invece di
 * togliere il segno: un trascinamento qui competerebbe con lo scorrimento della griglia sotto,
 * che con questa scheda aperta deve restare tutto disponibile. Il pannello si chiude col tasto
 * Indietro e si riapre da sé quando la selezione riparte.
 */
@Composable
fun BoxScope.PickSheet(visible: Boolean, actions: List<PadAction>, onHeight: (Int) -> Unit = {}) {
    val finestra = LocalWindowInfo.current.containerSize.height
    AnimatedVisibility(
        visible = visible,
        /*
         * ⚠️⚠️ **L'ENTRATA HA I NUMERI DELL'ALTRA SCHEDA, dalla 1.43, e la dissolvenza è
         * nuova** (istruzione dell'utente, 2026-09-03, sulla scheda delle informazioni:
         * *arriva dal basso con un'animazione fluida ed entra decelerando, ma al tempo stesso
         * c'è una mini dissolvenza*). Lui parlava di quella, che dal basso non arrivava
         * affatto: qui la salita c'era già.
         * ⚠️⚠️ **QUINDI PERCHÉ TOCCARE ANCHE QUESTA: perché quello che c'era NON era una
         * scelta.** Era la molla di fabbrica di `AnimatedVisibility`, cioè un valore che
         * nessuno aveva deciso, e da adesso 'arrivare dal basso' in questa app ha una
         * definizione. Allineare un valore di fabbrica a una decisione non ribalta niente;
         * lasciarle diverse avrebbe fatto due movimenti per lo stesso gesto, in due schermate
         * che si aprono a un tocco di distanza.
         * ⚠️⚠️ **E DALLA 1.44 ANCHE L'USCITA È SUA** (istruzione dell'utente: *le bottomsheet
         * devono sparire nello stesso modo in cui entrano, ma con animazione speculare*):
         * scende con [ACCELERA], che è la molla dell'entrata letta all'indietro, e la
         * dissolvenza sta **in coda** invece che in testa. Nella `1.43` questa uscita era
         * ancora la molla di fabbrica, e la nota di allora diceva che l'utente aveva descritto
         * come una scheda **entra**: era vero quel giorno.
         */
        /*
         * ⚠️ **La forma del gesto vive in `Sheet.kt`**, come i suoi quattro numeri: fino alla
         * `1.80` era scritta qui parola per parola, e in altri due posti uguale.
         */
        enter = arrivaDalBasso(),
        exit = vaGiu(),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        /*
         * ⚠️⚠️ **QUANTO LA SCHEDA COPRE DAL FONDO SI DICHIARA A OGNI FOTOGRAMMA, DALLA `2.11`,
         * E NON È LA STESSA COSA DI [onHeight]** (punto C del campo libero del giro accorpato:
         * *in alcune circostanze (es. si inizia una selezione dopo un 'copia', 'sposta' o
         * 'elimina'), la bottomsheet della selezione va a finire sotto la notifica in basso*).
         * Quel parametro dice quanto è alto il **contenuto**, e serve alla griglia per lasciargli
         * il posto; questo dice quanta parte di schermo la scheda **occupa adesso**, cioè un
         * numero che cambia mentre sale e mentre scende.
         * ⚠️⚠️ **A SALIRE È LA NOTIFICA E NON LA SCHEDA, ed è una scelta contro la sua proposta**
         * (*finché è visibile l'avviso la bottomsheet arriva più in alto e poi si abbassa?*):
         * muovere la scheda sposterebbe i **comandi** mentre il dito sta per toccarli, che è la
         * stessa famiglia del difetto della griglia che scorre sotto un dito appoggiato (la nota
         * della `1.78` sull'intestazione). Una notifica invece non si tocca quasi mai, e quando
         * la si tocca è per disfare, cioè prima che la selezione ricominci.
         * ⚠️⚠️ **SI MISURA LA POSIZIONE E NON SI ANIMA NIENTE, e qui sta il valore**: la scheda
         * entra ed esce con le sue curve, e chi la legge le segue **per costruzione**, senza una
         * seconda animazione da tenere allineata alla prima. Due animazioni scritte in due posti
         * divergono al primo ritocco, ed è un difetto che questo repository ha già pagato.
         * ⚠️ **Sul contenitore e non sul contenuto**: la scheda arriva al vetro e il rientro di
         * sistema ce l'ha dentro, quindi quello che copre parte dal bordo dello schermo.
         */
        val quota = remember { Any() }
        DisposableEffect(quota) { onDispose { FootStage.off(quota) } }
        Surface(
            /*
             * ⚠️⚠️ **IL BORDO D'ACCENTO CE L'HA ANCHE LEI, dalla `1.55`, ed è una decisione
             * dell'utente contro il criterio che l'aveva esclusa** (giro della `1.54`: *sì,
             * voglio la riga anche lì: in realtà dappertutto. Capisco che quella fa eccezione
             * perché non è in sovrapposizione e non ha sfocatura o velo ... Ma per coerenza deve
             * avere il tratto intorno come tutti gli altri elementi simili*). L'esenzione dal
             * **velo** resta, e la ragione è sua: quella scheda non copre la griglia, perché con
             * lei aperta si deve poter continuare a scegliere.
             * ⚠️ **Quindi il bordo e il velo non hanno più lo stesso elenco**, e chi legge
             * `Edge.kt` lo sappia: il velo dice 'mi apro sopra qualcosa', il bordo dice 'sono
             * una superficie di questa app'. La seconda cosa vale anche per chi non copre niente.
             * ⚠️ **Tre lati come l'altra scheda**: è appoggiata al bordo di sotto, e una riga
             * sull'ultima fila di pixel si legge come un taglio.
             * ⚠️⚠️ **E DALLA `1.56` LA RIGA CORRE DI FUORI** (sua prova, giro della `1.55`:
             * *le bottomsheet non stanno bene con la riga intorno*): resta la cima con i due
             * archi, e i fianchi finiscono fuori dallo schermo. Il perché sta su `edgedTop`.
             */
            modifier = Modifier
                .edgedTop(PANEL_ROUND)
                .onGloballyPositioned {
                    FootStage.cover(quota, finestra - it.positionInWindow().y.roundToInt())
                },
            shape = RoundedCornerShape(topStart = PANEL_ROUND, topEnd = PANEL_ROUND),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            /*
             * ⚠️⚠️ **NIENTE OMBRA DALLA 1.40, e il difetto era misurabile** (richiesta
             * dell'utente, 2026-09-03: *evita le ombreggiature in basso, altrimenti il
             * risultato è brutto*). Un'ombra si dipinge tutto attorno alla superficie, ma
             * questa è appoggiata al bordo dello schermo: sopra non si vede (la copre la
             * scheda stessa), ai lati nemmeno, e resta la sola striscia **di sotto**, cioè
             * quella che finisce sull'angolo stondato del vetro.
             * ⚠️ **Misurata sullo screenshot dell'utente**: quattro gradini di grigio sotto la
             * scheda, da `208,207,203` a `248,247,243`, prima della barra di sistema.
             * ⚠️⚠️ **E IL RILIEVO TONALE NON C'È PIÙ NEMMENO LUI, DALLA `1.78`: NON FACEVA
             * NIENTE.** Fino alla `1.77` qui c'era un `tonalElevation` di 6dp e la nota diceva
             * che *stacca la scheda dalla griglia dietro*. Misurato sul comportamento di
             * `Surface`: il rilievo tonale cambia il colore **solo** quando il colore ricevuto è
             * `surface`, e qui è `surfaceContainerHigh`, cioè la superficie tornava intatta.
             * L'unica via che restava, l'elevazione che si propaga ai figli, non ha nessun
             * lettore: nessuna `Surface` dentro la scheda chiede il colore `surface`.
             * ⚠️ **A staccare la scheda ci pensano il colore e il bordo d'accento**, che sono
             * scelte dichiarate qui sopra: una nota che attribuisce l'effetto a un parametro
             * spento manda chi ritocca l'aspetto a girare la manopola sbagliata.
             */
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    /*
                     * ⚠️⚠️ **IL RIENTRO DI SISTEMA STA QUI, e la scheda arriva al bordo**
                     * (stessa richiesta: *fa' in modo che la barra multi-attività in basso
                     * assuma lo stesso colore dello sfondo*). Il fondo della scheda passa
                     * **sotto** la barra, il contenuto no: le due file di icone restano dove
                     * sono e a cambiare è la sola striscia in fondo, che prende il colore
                     * della scheda invece di quello della pagina.
                     * ⚠️⚠️ **PERCIÒ QUESTA SCHEDA VIVE NEL `Box` DI RADICE DELLA SCHERMATA, e
                     * non dentro la colonna**: là il rientro di sistema è già stato applicato
                     * e **consumato**, quindi questa riga non aggiungerebbe niente e la
                     * scheda si fermerebbe sopra la barra come prima.
                     */
                    .navigationBarsPadding()
                    /*
                     * ⚠️⚠️ **L'ALTEZZA SI MISURA, e serve alla griglia sotto**:
                     * senza il numero vero, l'ultima fila di fotografie resterebbe sotto il
                     * pannello e nessuno scorrimento la porterebbe fuori. Una costante scritta
                     * a mano sbaglierebbe il giorno che un'etichetta va a capo in una lingua
                     * lunga, che è esattamente il caso in cui il pannello cresce.
                     * ⚠️⚠️ **SI MISURA DOPO IL RIENTRO DI SISTEMA, cioè il solo contenuto**, e
                     * l'ordine di queste due righe è la ragione: la griglia vive in uno spazio
                     * che il rientro lo ha già tolto, quindi un'altezza che lo comprendesse
                     * lascerebbe sotto l'ultima fila un buco alto quanto la barra.
                     */
                    .onSizeChanged { onHeight(it.height) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ActionPad(actions = actions, columns = SHEET_COLUMNS, stretch = true)
            }
        }
    }
}

/**
 * Quanta parte del fondo dello schermo è già occupata, **in questo fotogramma**.
 *
 * ⚠️⚠️ **ESISTE PERCHÉ CHI DEVE SAPERLO NON VIVE DENTRO LA SCHERMATA**: la notifica di casa e la
 * fascia della copertina vivono sopra la transizione fra schermate (`ViewerActivity`), cioè in un
 * ramo che non discende da [PickSheet]. Un `CompositionLocal` va dall'alto in basso e qui il verso
 * è il contrario, quindi la via è la stessa di [Notices] e di [VeilStage]: un oggetto di processo
 * che una superficie scrive e le altre leggono.
 * ⚠️⚠️ **A SCRIVERCI SONO IN DUE DALLA `2.24`, E IL SECONDO È IL FAB** (segnalazione dell'utente,
 * punto A1 del campo libero del giro della `2.23`: *la notifica inferiore con 'Annulla' (es. per
 * 'Sposta') a volte va sopra il FAB (su qualunque lato sia)*). Fino alla `2.23` il solo chiedente
 * era la scheda della selezione, e la notifica la copriva: quel tasto è in un angolo, la notifica
 * è larga quasi tutto lo schermo, e a disegnarla è la radice dell'app, cioè dopo di lui.
 * ⚠️ **Il FAB della griglia si stacca in una finestra sua a menu aperto, e la misura regge lo
 * stesso**: là resta un segnaposto della stessa misura (vedi [TapHoldFab]), che vive nella
 * finestra dell'app ed è quello che si misura.
 * ⚠️⚠️ **SI LEGGE IN COMPOSIZIONE, E LA VIA PIÙ ECONOMICA NON FUNZIONA: È MISURATO.** La prima
 * stesura lo leggeva dentro `Modifier.offset { }`, cioè nella fase di layout, che è la strada che
 * costa zero ricomposizioni; col banco alla mano quel lambda è stato valutato **una volta sola**,
 * con lo zero di partenza, e non è più tornato quando il valore è salito a 94. Perché non torni
 * non si sa, e si scrive così invece di inventare una causa: il sospetto è che a scriverlo sia un
 * `onGloballyPositioned`, cioè la stessa passata di layout che dovrebbe rileggerlo.
 * ⚠️ **Il costo si paga e si dichiara**: la notifica si ricompone a ogni fotogramma nei due terzi
 * di secondo in cui la scheda sale o scende, ed è un nodo con dentro una frase e un tasto.
 */
internal object FootStage {
    /*
     * ⚠️⚠️ **UNA MAPPA E NON UN NUMERO, DALLA `2.24`, E LA RAGIONE È CHE ADESSO I CHIEDENTI SONO
     * DUE**: la scheda della selezione e il FAB. Con un numero solo, il secondo a scrivere
     * cancellerebbe la richiesta del primo, e chi se ne va porterebbe via anche l'ingombro
     * dell'altro. È la stessa forma di [VeilStage], e per la stessa ragione.
     * ⚠️ **Vale il massimo e non la somma**: le due superfici sono appoggiate allo stesso bordo,
     * quindi quella che sta più in alto le comprende tutte.
     */
    private val coprono = mutableStateMapOf<Any, Int>()

    /*
     * ⚠️⚠️ **E DALLA `2.25` UN COMANDO PUÒ OCCUPARE UN FIANCO INVECE DI UNA FASCIA, ED È IL SUO
     * RISCONTRO** (giro della `2.24`, voce `avviso-fab` approvata con una richiesta, e risposta
     * **`stringe`** a `d-avviso-forma`: *non si potrebbe fare lo stesso avviso meno largo di quel
     * tanto che basta a stare a fianco del FAB?*). La `2.24` faceva **salire** la notifica sopra il
     * FAB, che è la risposta giusta per una scheda larga tutto lo schermo e quella sbagliata per un
     * tasto che vive in un angolo: là accanto lo spazio c'è.
     * ⚠️ **Due mappe e non una firmata**: il lato di un comando può cambiare (la scelta di
     * `fabSide`), quindi chi scrive deve togliersi dall'altra parte, e con un numero col segno
     * quella riga sarebbe un trucco da rileggere ogni volta.
     */
    private val aDestra = mutableStateMapOf<Any, Int>()
    private val aSinistra = mutableStateMapOf<Any, Int>()

    /** Quanti pixel di schermo copre, contati dal bordo di sotto: `0` quando non c'è nessuno. */
    val covers: Int get() = coprono.values.maxOrNull() ?: 0

    /** Quanti pixel occupa in fondo a destra, contati dal bordo destro della finestra. */
    val right: Int get() = aDestra.values.maxOrNull() ?: 0

    /** Quanti pixel occupa in fondo a sinistra, contati dal bordo sinistro della finestra. */
    val left: Int get() = aSinistra.values.maxOrNull() ?: 0

    fun cover(chi: Any, px: Int) {
        if (px <= 0) coprono.remove(chi) else coprono[chi] = px
    }

    /**
     * Dice che [chi] occupa [px] pixel del fondo dello schermo dal lato dichiarato, invece di una
     * fascia larga quanto la finestra.
     */
    fun beside(chi: Any, px: Int, right: Boolean) {
        val qui = if (right) aDestra else aSinistra
        val altrove = if (right) aSinistra else aDestra
        altrove.remove(chi)
        if (px <= 0) qui.remove(chi) else qui[chi] = px
    }

    fun off(chi: Any) {
        coprono.remove(chi)
        aDestra.remove(chi)
        aSinistra.remove(chi)
    }
}

/**
 * Tiene una superficie appoggiata in fondo **sopra** quello che il fondo dello schermo già porta.
 *
 * ⚠️⚠️ **LO CHIAMANO LE DUE COSE CHE VIVONO IN FONDO SOPRA LA TRANSIZIONE**, cioè la notifica di
 * casa e la fascia della copertina: sono le sole due superfici che possono finire sopra qualcosa,
 * perché tutto il resto o vive dentro una schermata o è una finestra sua. Una riga sola per tutte
 * e due, così una terza che nascesse non ha un secondo modo con cui sbagliare.
 * ⚠️⚠️ **CHI SI SCANSA È SEMPRE QUESTA, E NON CHI LE È SOTTO**: là ci sono i **comandi** (la scheda
 * della selezione, il FAB), e muoverli vorrebbe dire spostarli mentre il dito sta per toccarli,
 * che è la stessa famiglia del difetto della griglia che scorre sotto un dito appoggiato. Una
 * notifica invece si tocca di rado, e quando la si tocca è per disfare.
 * ⚠️⚠️ **SI SOTTRAE IL RIENTRO DI SISTEMA, e senza quel termine la notifica salirebbe troppo**:
 * chi chiama si è già scansato dalla barra di navigazione, e quello che dichiara l'ingombro conta
 * dal bordo della **finestra**, quindi alzarsi di tutto quello che copre conterebbe quella
 * striscia due volte.
 * ⚠️ **Uno spostamento e non un rientro**: la superficie resta larga e alta com'era, e a muoversi
 * è solo dove viene posata. Un `padding` la rimisurerebbe a ogni fotogramma della salita.
 *
 * ⚠️⚠️ **E DALLA `2.25` DAVANTI A UN COMANDO DI FIANCO SI STRINGE INVECE DI SALIRE, ED È LA SUA
 * RISPOSTA `stringe`**: accanto a un tasto che vive in un angolo lo spazio c'è, e salirgli sopra
 * lascia una striscia vuota larga tutto lo schermo. Il costo lo ha previsto lui (*ci sarebbe meno
 * spazio per il testo*), e le due cose convivono: si sale sopra chi occupa una fascia, si stringe
 * accanto a chi occupa un fianco.
 * ⚠️ **Qui il rientro è la scelta giusta, al contrario della salita**: la larghezza di un comando
 * non cambia mentre lo si guarda (il FAB che se ne va si rimpicciolisce in un `graphicsLayer`,
 * quindi il suo riquadro resta), e una rimisurazione si paga solo quando quel comando compare o
 * sparisce.
 * ⚠️ **Il lato lo dichiara chi occupa lo spazio e non lo legge questa riga**: `fabSide` è una
 * preferenza, e leggerla qui vorrebbe dire un secondo posto che decide dov'è il FAB.
 *
 * ⚠️⚠️ **E CHI SALE NON SI STRINGE, DALLA `2.26`, ED È LA SUA RISPOSTA `sempre` A
 * `d-avviso-forma-2`** (giro della `2.25`: *può stare massimizzata il larghezza solo quando (per
 * la presenza della bottomsheet) si sposta sopra*). Sopra una scheda larga tutto lo schermo non
 * c'è nessun comando da schivare di fianco, quindi rientrare là costerebbe spazio al testo senza
 * guadagnare niente. ⚠️ **Oggi i due casi non si incontrano quasi mai** (appena c'è una selezione
 * il FAB lascia il posto alla scheda), ma 'quasi' non è una regola: durante quel cambio le due
 * dichiarazioni convivono per qualche fotogramma, e senza questa riga la notifica salirebbe **e**
 * si stringerebbe insieme.
 */
@Composable
internal fun Modifier.aboveFoot(): Modifier {
    val density = LocalDensity.current
    val barra = WindowInsets.navigationBars.getBottom(density)
    val su = (FootStage.covers - barra).coerceAtLeast(0)
    val stretta = su == 0
    val sinistra = with(density) { (if (stretta) FootStage.left else 0).toDp() }
    val destra = with(density) { (if (stretta) FootStage.right else 0).toDp() }
    return padding(start = sinistra, end = destra).offset { IntOffset(0, -su) }
}

/**
 * I tasti che possono comparire in un riquadro, con il gettone con cui si salvano.
 *
 * ⚠️⚠️ **UN SOLO ELENCO PER QUATTRO RIQUADRI, e non quattro elenchi**: le sei azioni sui file,
 * le quattro della selezione, e le due file dell'editor vivono qui insieme perché il gettone
 * deve essere unico nell'archivio. Quale riquadro porta quali tasti lo dice il **suo ordine di
 * fabbrica** in `Settings`, non questo elenco.
 *
 * ⚠️ **[RENAME] è una sola voce e nel cestino diventa 'Ripristina'**: cambia icona, etichetta e
 * azione, ma è lo stesso posto nel riquadro, e un secondo gettone lo spezzerebbe in due righe
 * da riordinare per una cosa sola.
 */
enum class PadKey(override val token: String) : Choice {
    // Le sei azioni sui file: il riquadro del visualizzatore e dell'albero, e le prime della
    // scheda della selezione.
    COPY("copy"),
    MOVE("move"),
    SHARE("share"),
    RENAME("rename"),
    DELETE("delete"),
    INFO("info"),

    // Le quattro che vivono nella sola scheda della selezione.
    LIST("list"),
    ALL("all"),
    NONE("none"),
    INVERT("invert"),

    // La prima fila dell'editor: girare, riflettere e centrare.
    TURN_LEFT("turn-left"),
    TURN_RIGHT("turn-right"),
    CENTRE_ACROSS("centre-across"),
    CENTRE_DOWN("centre-down"),
    FLIP("flip"),

    // La seconda fila dell'editor: la cronologia e la conferma.
    ORIGINAL("original"),
    UNDO("undo"),
    REDO("redo"),
    APPLY("apply")
}

/**
 * Come si chiama un tasto, quando lo si deve nominare fuori dal suo riquadro.
 *
 * ⚠️⚠️ **STA QUI E NON IN UN `when` DELLA SCHERMATA DELLE IMPOSTAZIONI**, per la stessa ragione
 * di `FactField`: serve in **due** posti (la pagina che riordina e i testi che la ricerca
 * confronta), e un `when` da diciotto rami scritto due volte è il posto in cui le due copie
 * prima o poi diranno due parole diverse per la stessa cosa.
 * ⚠️ **Sono le stringhe che i tasti già portano**, quindi la pagina che riordina non costa
 * nessuna traduzione nuova.
 * ⚠️ **'Rinomina' e non 'Ripristina'**: nel cestino quel tasto cambia nome, ma qui si riordina
 * un posto e non un contesto, e il nome che si legge è quello che si vede quasi sempre.
 */
/**
 * Se il tasto è quello **irreversibile**, cioè quello che porta il colore dell'errore.
 *
 * ⚠️⚠️ **VIVE QUI PER LO STESSO MOTIVO DI [label] E [glyph]: serve in due posti**, il riquadro
 * vero (dove diventa il valore di serie di `PadAction.danger`) e la **replica** che si riordina,
 * che di `PadAction` non ne ha nessuna e lavora sulle sole chiavi. Fino alla `1.80` là il tasto
 * che elimina era identico agli altri, e la replica esiste per somigliare al modello.
 * ⚠️ **Irreversibile e non 'importante'**: vale per l'eliminazione, e per niente che si possa
 * disfare con l'operazione contraria. Chi aggiunge una voce da cui non si torna la nomina qui.
 */
fun PadKey.danger(): Boolean = this == PadKey.DELETE

@StringRes
fun PadKey.label(): Int = when (this) {
    PadKey.COPY -> R.string.menu_copy_here
    PadKey.MOVE -> R.string.pick_move
    PadKey.SHARE -> R.string.menu_share
    PadKey.RENAME -> R.string.pick_rename
    PadKey.DELETE -> R.string.pick_delete
    PadKey.INFO -> R.string.pick_info
    PadKey.LIST -> R.string.pick_list
    PadKey.ALL -> R.string.pick_all_short
    PadKey.NONE -> R.string.pick_none
    PadKey.INVERT -> R.string.pick_invert
    PadKey.TURN_LEFT -> R.string.editor_left
    PadKey.TURN_RIGHT -> R.string.editor_right
    PadKey.CENTRE_ACROSS -> R.string.editor_center_across
    PadKey.CENTRE_DOWN -> R.string.editor_center_down
    PadKey.FLIP -> R.string.editor_flip
    PadKey.ORIGINAL -> R.string.editor_original
    PadKey.UNDO -> R.string.editor_undo
    PadKey.REDO -> R.string.editor_redo
    PadKey.APPLY -> R.string.editor_apply
}

/**
 * Il disegno di un tasto, per la riga che lo fa riordinare.
 *
 * ⚠️ **È composabile perché quattro di questi glifi vivono in `res/`**, e una risorsa si legge
 * solo da dentro una composizione.
 * ⚠️ **Le due frecce di rotazione sono quelle DISEGNATE DALL'UTENTE**, come nell'editor: un
 * glifo di sistema qui e il suo là farebbe sembrare due tasti diversi.
 */
@Composable
fun PadKey.glyph(): ImageVector = when (this) {
    PadKey.COPY -> Glyphs.FolderPair
    PadKey.MOVE -> Glyphs.FolderPairDashed
    PadKey.SHARE -> Icons.Default.Share
    PadKey.RENAME -> Glyphs.TextCursor
    PadKey.DELETE -> Glyphs.PickDelete
    PadKey.INFO -> Icons.Outlined.Info
    PadKey.LIST -> Icons.AutoMirrored.Outlined.FormatListBulleted
    PadKey.ALL -> Glyphs.PickAll
    PadKey.NONE -> Glyphs.PickNone
    PadKey.INVERT -> Glyphs.PickInvert
    // ⚠️ Le due frecce sue, come nell'editor, dalla 1.63: il verso di un giro è fisico e
    // non segue la lettura, quindi non si specchiano (vedi la nota là).
    PadKey.TURN_LEFT -> Glyphs.TurnLeft
    PadKey.TURN_RIGHT -> Glyphs.TurnRight
    PadKey.CENTRE_ACROSS -> Glyphs.AlignAcross
    PadKey.CENTRE_DOWN -> Glyphs.AlignDown
    PadKey.FLIP -> Icons.Filled.Flip
    PadKey.ORIGINAL -> Glyphs.EditReset
    PadKey.UNDO -> Glyphs.EditUndo
    PadKey.REDO -> Glyphs.EditRedo
    PadKey.APPLY -> Glyphs.EditApply
}

/**
 * Rimette una lista di azioni nell'ordine che l'utente ha scelto.
 *
 * ⚠️⚠️ **UN TASTO CHE L'ORDINE NON NOMINA NON SPARISCE: finisce in coda.** Qui è una rete e non
 * la garanzia vera, che sta a monte: l'ordine arriva da [padOrderOf], che ai gettoni salvati
 * aggiunge **tutti** quelli mancanti al posto che hanno di fabbrica. Quindi una versione futura
 * che aggiunge un'azione la vede comparire dove l'ha messa, e non in fondo. Se qualcosa arriva
 * qui senza posto, è un riquadro che porta un tasto non suo, cioè un difetto: la coda lo rende
 * visibile invece di farlo sparire.
 * ⚠️ **L'ordinamento è STABILE**, quindi fra due tasti senza posto resta quello di partenza.
 * ⚠️ **Si ordina la lista VERA e non i gettoni**: chi chiama costruisce le sue azioni con le
 * loro chiusure, e qui si spostano soltanto.
 */
fun List<PadAction>.inOrder(order: List<PadKey>): List<PadAction> {
    if (order.isEmpty()) return this
    val posto = order.withIndex().associate { (i, k) -> k to i }
    // ⚠️ `sortedBy` è STABILE, ed è quello che tiene fermi i tasti che l'ordine non nomina:
    // fra due sconosciuti resta l'ordine di partenza.
    return sortedBy { posto[it.key] ?: Int.MAX_VALUE }
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
    /**
     * Chi è questo tasto, per l'archivio.
     *
     * ⚠️⚠️ **NASCE PERCHÉ L'ORDINE SI SALVA, dalla `1.56`**: prima un'azione era solo
     * un'icona e uno `@StringRes`, cioè non aveva identità, e un ordine salvato ha bisogno di
     * sapere **quale** tasto sta in quale posto. La prova che non bastava guardare l'icona: nel
     * cestino 'Rinomina' diventa 'Ripristina' cambiando disegno, etichetta e azione, e resta lo
     * stesso posto nel riquadro.
     * ⚠️ **Il gettone non è il nome della costante**, come per ogni [Choice]: rinominare una
     * costante non deve azzerare l'ordine salvato su un telefono.
     */
    val key: PadKey,
    val icon: ImageVector,
    @StringRes val label: Int,
    /**
     * Se è quella da cui non si torna.
     *
     * ⚠️⚠️ **SI RICAVA DALLA CHIAVE, DALLA `1.81`, E PRIMA ERA UN `false` CHE OGNI CHIAMANTE
     * DOVEVA SMENTIRE** (censimento della UI del 2026-09-05). I quattro riquadri passavano
     * `danger = true` sulla stessa voce, quindi il vincolo era scritto quattro volte e chi
     * apriva un riquadro nuovo poteva dimenticarlo senza che niente lo segnalasse: il tasto
     * irreversibile sarebbe uscito identico ai vicini. Adesso il colore arriva per
     * costruzione, com'è per il velo di `lowered()`.
     * ⚠️ **Resta un parametro e non diventa una lettura**: un chiamante che avesse una ragione
     * per spegnerlo lo passa, e la ragione la scrive accanto.
     */
    val danger: Boolean = key.danger(),
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
 * ⚠️ **Con la parola in scena, l'icona non porta descrizione e il testo sì**: `clickable`
 * fonde le semantiche dei figli, quindi TalkBack legge una voce sola. Descrivendo anche
 * l'icona la leggerebbe due volte, che è il difetto già evitato nelle copertine delle
 * cartelle.
 * ⚠️⚠️ **SENZA LA PAROLA, LA DESCRIZIONE PASSA ALL'ICONA, e non è un dettaglio di
 * cortesia**: il nome parlato del tasto **è** quel testo, quindi togliendolo senza spostare
 * la descrizione il tasto resterebbe muto, cioè inservibile con un lettore di schermo. Non
 * costa nessuna stringa nuova: è la stessa [PadAction.label].
 * ⚠️ **Il tocco sta sull'intera colonna**, non sull'icona: un bersaglio di 24dp si manca,
 * e qui i tasti sono sei e vicini.
 * ⚠️⚠️ **E L'ALTEZZA MINIMA SI DICHIARA, perché senza la parola la colonna non ci arriva
 * più**: con l'etichetta la cella è alta icona più parola, cioè oltre i 48dp che Material
 * chiede a un bersaglio; senza, sarebbero icona più due respiri, quaranta. [PAD_TAP] tiene
 * il pavimento, e il riquadro compatto lo è di disegno e non di area toccabile.
 */
@Composable
private fun PadButton(
    action: PadAction,
    modifier: Modifier = Modifier,
    labels: Boolean = LocalPadLook.current.labels
) {
    val haptics = LocalHapticFeedback.current
    val hold = remember(action.onHold, haptics) {
        action.onHold?.let { premuto ->
            {
                haptics.performHapticFeedback(HOLD_BUZZ)
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
                /*
                 * ⚠️⚠️ **IL RUOLO SI DICHIARA, e fino alla `1.80` questi tasti non lo
                 * facevano** (censimento della UI del 2026-09-05): il KDoc qui sopra cura con
                 * precisione la **voce** del tasto, cioè chi porta la descrizione fra icona e
                 * testo, e sul ruolo taceva, quindi un lettore di schermo leggeva un
                 * contenitore con dentro un testo. Il FAB dello stesso file lo dichiarava
                 * già, e la differenza correva fra due comandi della stessa superficie.
                 */
                role = Role.Button,
                onLongClickLabel = action.holdLabel?.let { stringResource(it) },
                // ⚠️ Qui il gesto può non esserci, quindi la vibrazione si compone a mano
                // invece di passare da [withHaptics]: vedi la sua nota.
                onLongClick = hold,
                onClick = action.onClick
            )
            .heightIn(min = PAD_TAP)
            .padding(vertical = PAD_GAP),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PAD_LABEL_GAP, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = if (labels) null else stringResource(action.label),
            tint = tint,
            modifier = Modifier.size(PAD_ICON)
        )
        if (!labels) return@Column
        Text(
            text = stringResource(action.label),
            style = padLabel(),
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
 * Il vestito dell'etichetta di un tasto, per il modello e per la sua replica.
 *
 * ⚠️⚠️ **UN GRADINO SOTTO `labelSmall`, DALLA `2.03`, ED È UNA MISURA E NON UN GUSTO** (sua
 * segnalazione, con schermata: *riduci leggermente la dimensione delle etichette di testo
 * delle funzioni dell'editor, in modo che ci stia l'intero contenuto senza tagliare
 * 'orizzontale'*). Nella fila da cinque dell'editor la cella vale [PAD_CELL], cioè 76dp, e
 * 'Centra in orizzontale' va a capo sulla parola più lunga: misurata sulla sua schermata,
 * quella parola a 11sp chiede **77dp**, cioè un punto più di quanto ce n'è, e l'ellissi si
 * mangiava le ultime tre lettere.
 * - ⚠️⚠️ **IL BANCO NON POTEVA VEDERLO, ed è la ragione per cui il numero viene dai suoi
 *   pixel**: con la grafica di Robolectric la stessa parola a 11sp entra in 64dp, perché là
 *   il carattere è più stretto di quello del telefono. È il caso dichiarato in
 *   `AIV/CLAUDE.md` § '🧪 Quando si scrive una prova, e quando no', cioè quello che dipende
 *   dall'apparecchio: una prova che lo misurasse sarebbe verde con e senza la correzione.
 * - **Perché 10sp e non 10,5**: a 10,5 la stessa parola viene 73dp, cioè tre punti di
 *   margine, e con un carattere di sistema un po' più largo si torna a tagliare. A 10sp ne
 *   restano sei, e la riduzione resta quella che ha chiesto, cioè leggera.
 * - ⚠️ **Vale per TUTTE le file e non per la sola riga di mezzo**: il tasto è uno solo
 *   ([PadButton]), e due corpi diversi sotto due icone identiche si vedrebbero prima nella
 *   scheda della selezione, dove le due file stanno una sopra l'altra.
 */
@Composable
private fun padLabel(): TextStyle =
    MaterialTheme.typography.labelSmall.copy(fontSize = PAD_LABEL_SIZE)

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
            haptics.performHapticFeedback(HOLD_BUZZ)
            action()
        }
    }
}

/**
 * Il colpetto di **ogni** pressione lunga dell'app, scritto in un posto solo.
 *
 * ⚠️⚠️ **DALLA 1.21 È `TextHandleMove` E NON `LongPress`, per riscontro dell'utente**
 * (2026-09-01: *vorrei una vibrazione leggermente più breve di quella attualmente impostata:
 * dev'essere morbida e discreta*). `LongPress` è la vibrazione piena che Android usa per
 * questo gesto di serie, e sul suo telefono era troppo: `TextHandleMove` è il colpetto
 * leggero delle maniglie del testo, cioè la stessa cosa più corta.
 * ⚠️⚠️ **E NON `SegmentTick` o `ToggleOn`, che per nome sarebbero i tipi giusti**: quelle
 * costanti sono arrivate con **Android 14**, e Compose passa il numero grezzo a
 * `performHapticFeedback` **senza nessun ripiego** (verificato sul bytecode di
 * `DefaultHapticFeedback`). Sotto Android 14 il telefono riceve una costante che non conosce
 * e **non vibra affatto**, e il minSdk qui è 28. `TextHandleMove` esiste dall'API 27.
 * ⚠️ **Con questo cade la distinzione fra ENTRARE nella selezione e muoversi dentro**, che
 * fino alla 1.20 era un colpetto forte contro uno leggero: adesso sono lo stesso. È una
 * conseguenza voluta della richiesta, non una svista, e si dichiara perché il giorno che
 * l'ingresso nella selezione dovesse tornare a farsi sentire, quello è il posto da cui
 * ripartire.
 */
val HOLD_BUZZ = HapticFeedbackType.TextHandleMove


/**
 * Quante colonne ha il riquadro: tre, come l'utente le ha chieste.
 *
 * ⚠️ **Non è privata perché la legge anche la pagina che RIORDINA i tasti**, come già
 * [SHEET_COLUMNS]: quella replica il riquadro, e un 3 scritto una seconda volta sarebbe il
 * numero che un giorno diverge da questo.
 */
internal const val PAD_COLUMNS = 3

/**
 * Quanto è alta una cella della replica che si riordina.
 *
 * ⚠️ **Fissa, e non quella del riquadro vero**: là l'altezza la fa il contenuto, quindi una
 * parola che va a capo allunga la sua riga. Qui il passo di riga deve essere un numero, o il
 * conto del posto d'arrivo non torna. 72dp tengono un glifo da 24 e due righe di parola.
 */
private val ARRANGE_HIGH = 72.dp

/** Quanto si vede il fondo tondo sotto un tasto fermo. */
private const val ARRANGE_BED = 0.10f

/** Quanto si vede quando il tasto è in mano: la stessa cosa, più evidente. */
private const val ARRANGE_HELD = 0.24f


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

/** Il corpo dell'etichetta di un tasto. Il perché di questo numero vive su [padLabel]. */
private val PAD_LABEL_SIZE = 10.sp

/**
 * Il margine laterale del riquadro dentro il menu che lo contiene.
 *
 * ⚠️ **Dalla `1.59` è il PAVIMENTO del fianco sinistro e non più il fianco**: quello lo decide
 * la colonna delle icone della lista, e questo numero interviene solo dove quel conto darebbe
 * un rientro negativo. A destra resta il margine di sempre.
 */
private val PAD_EDGE = 4.dp

/**
 * Il rimbalzo del FAB al tocco: di quanto si stringe, e in quanto tempo scende e risale.
 *
 * ⚠️⚠️ **DALLA `1.68` IL FAB NON HA PIÙ NÉ OMBRA NÉ SFOCATURA NÉ UNA TINTA CHE SI SPOSTA, ED È
 * UNA SUA ISTRUZIONE** (riscontro del giro della `1.67`: *elimina tutte le animazioni e gli
 * effetti applicati finora al FAB, che comunque non stavano funzionando bene*, e sulla voce
 * dell'ombra *togli del tutto l'ombra*). Tre versioni di fila avevano provato a farsi vedere
 * **aumentando una quantità**: mezzo punto di rientro, poi due, poi quattro, e ogni volta il
 * riscontro è stato lo stesso, *non vedo nulla*.
 * ⚠️⚠️ **PERCHÉ NON SI VEDEVA, ed è la cosa da non rifare**: quattro punti su un lato di
 * [FAB_SIZE] sono il 10% della misura, ma una piastrella isolata non ha accanto niente con cui
 * confrontarsi, e senza un riferimento l'occhio non legge una dimensione. Quello che mancava non
 * era la quantità: era il **confronto**. Un movimento, un simbolo diverso e un colore diverso si
 * vedono da soli, perché ognuno confronta il tasto con se stesso di un attimo prima.
 * ⚠️ **Il 30% e i due tempi sono suoi, alla lettera**: *rimpicciolisce del 30% in 30 ms, torna
 * alla dimensione iniziale in 60 ms, tutto con ease-in/ease-out*.
 * ⚠️ **È una SCALA e non una misura in punti**, al contrario di quello che faceva la `1.62`: là
 * il glifo doveva restare fermo mentre la piastrella si stringeva, e per escluderlo serviva una
 * misura sul solo fondo; qui rimbalza il **tasto intero**, glifo compreso, quindi la scala è la
 * cosa giusta e costa un livello grafico invece di una rimisurazione.
 */
private const val RIMBALZO = 0.30f
private const val RIMBALZO_GIU_MS = 30
private const val RIMBALZO_SU_MS = 60

/**
 * In quanto tempo il glifo dell'app diventa una ×, e in quanto il fondo cambia accento.
 *
 * ⚠️ **I 90 ms del simbolo sono la somma dei due tempi del rimbalzo**, ed è la sua richiesta:
 * *contemporaneamente, con zoom-in/zoom-out e crossfade, negli stessi 90 ms*. Così il simbolo
 * finisce di cambiare nell'istante in cui il tasto torna alla sua misura, e i due movimenti si
 * leggono come uno.
 * ⚠️ **Il colore è più corto, e anche questo è suo**: *in meno di 90 ms: un crossfade pressoché
 * istantaneo*. Un colore che sfuma piano si legge come un'animazione a sé; uno che salta si
 * legge come lo stato nuovo del tasto, che è quello che deve dire.
 * ⚠️ **Non si scrive come frazione di [MORPH_MS]**: sono due numeri che lui ha dato separati, e
 * legarli vorrebbe dire che cambiando l'uno cambia l'altro senza che nessuno l'abbia chiesto.
 */
private const val MORPH_MS = RIMBALZO_GIU_MS + RIMBALZO_SU_MS
private const val TINTA_MS = 55

/**
 * In quanto tempo il FAB si rimpicciolisce fino a sparire, andando dove non c'è.
 *
 * ⚠️⚠️ **IL NUMERO VIENE DALLA DISSOLVENZA DI SCHERMATA, E IL CONTO È MISURATO**: quei 180 ms
 * scendono con la curva di serie di `tween`, che è ripida in mezzo, e l'opacità della schermata
 * che se ne va vale 0,54 a 60 ms, 0,30 a 80 e 0,16 a 100. Cioè tutto quello che il FAB fa dopo il
 * primo decimo di secondo lo fa dietro un velo che non lascia passare quasi niente.
 * ⚠️ **Con questi cento millisecondi e una curva che parte veloce**, a 40 ms il tasto è già sotto
 * la metà e la schermata è ancora al 72%: il rimpicciolimento si vede quando c'è da vederlo, e
 * l'ultimo tratto se ne va insieme a tutto il resto.
 * ⚠️ **Allungarlo peggiora invece di migliorare**, ed è la lezione della `1.73` letta al
 * rovescio: là un'animazione di entrata giocata sotto la dissolvenza si vedeva solo per la coda.
 */
private const val VIA_MS = 100

/**
 * Il lato della × che prende il posto del glifo, e quanto zoom fanno i due simboli.
 *
 * ⚠️ **Più piccola del glifo dell'app**, come ha chiesto (*il simbolo × centrato e piccolo*):
 * [PAD_ICON] è la misura di un glifo di comando, e questa sta un gradino sotto.
 * ⚠️⚠️ **È `Icons.Default.Close` E NON UN DISEGNO NUOVO**: la regola di casa dice che un file che
 * arriva si misura contro quello che Compose già porta e a zero scarto vince Material
 * (`CLAUDE.md`, § '🖌️ Come entra un disegno'). Qui non è arrivato nessun file: lui ha chiesto
 * *il simbolo ×*, che è esattamente quel glifo, e disegnarlo a mano vorrebbe dire tenere in
 * `res/` una seconda copia di una croce.
 * ⚠️ **Lo zoom è una FRAZIONE e non una misura**: il glifo dell'app non è quadrato (nella
 * schermata iniziale è il marchio, più largo che alto), quindi una misura in punti lo
 * schiaccerebbe su un asse.
 * ⚠️ **Chi entra parte piccolo e chi esce finisce piccolo**, cioè lo stesso numero letto nei due
 * versi: a metà strada i due simboli hanno la stessa misura, e la dissolvenza non ha nessun
 * salto di scala da nascondere.
 */
private val CHIUDI_LATO = 20.dp
private const val GLIFO_ZOOM = 0.35f

/** Lo smusso dell'alone del tocco su una cella. */
private val PAD_CORNER = 10.dp

/**
 * La larghezza di una cella quando le parole sono spente.
 *
 * ⚠️ **Nasce dal bersaglio e non dal glifo**: [PAD_TAP] è il pavimento in altezza, e una cella
 * più stretta di così sarebbe un tasto alto e sottile, cioè difficile da centrare col pollice.
 * Un glifo da 24 in una cella da 48 lascia dodici punti d'aria per lato, che è il rapporto con
 * cui Material disegna un `IconButton`.
 */
private val PAD_CELL_BARE = 48.dp

/**
 * L'altezza minima di una cella, con o senza parola.
 *
 * ⚠️⚠️ **48dp È IL BERSAGLIO MINIMO DI MATERIAL, e senza questa riga il riquadro compatto ci
 * andava sotto**: con la parola la colonna misura da sé oltre i cinquanta (respiro, glifo,
 * distacco, riga di testo, respiro); togliendola resterebbero quaranta, cioè un tasto che si
 * manca. La compattezza chiesta è di **disegno**, non di area toccabile.
 */
private val PAD_TAP = 48.dp

/**
 * Come si presentano i riquadri di questa app: le parole e i quattro ordini.
 *
 * ⚠️⚠️ **STA IN UN `CompositionLocal` PER LA STESSA RAGIONE DEL VELO** (vedi `LocalAivDepth`):
 * queste cose le chiedono superfici che vivono in **finestre**, e le finestre le impostazioni
 * non le ricevono. La catena per portarci quattro liste e un booleano attraversa il
 * visualizzatore, la griglia con i suoi tre richiami, la scheda della selezione, l'albero e
 * l'editor: cinque schermate per un dato che non cambia mai durante un gesto.
 * ⚠️ **Il valore di fabbrica è quello che l'app aveva prima**, quindi una finestra che
 * nascesse fuori dall'albero della composizione si comporta come sempre invece di sparire.
 */
class PadLook(
    val labels: Boolean = true,
    /** Il riquadro delle sei azioni: visualizzatore e albero. */
    val menu: List<PadKey> = MENU_KEYS,
    /** La scheda della selezione, dieci azioni. */
    val pick: List<PadKey> = PICK_KEYS,
    /** La prima fila dell'editor: girare e centrare. */
    val turn: List<PadKey> = TURN_KEYS,
    /** La seconda fila dell'editor: la cronologia e la conferma. */
    val step: List<PadKey> = STEP_KEYS,
    /**
     * Da che parte dello schermo sta il FAB.
     *
     * ⚠️⚠️ **VIAGGIA QUI E NON PER PARAMETRO, ed è la stessa ragione delle altre cinque**: i
     * FAB vivono in tre schermate e in un velo di onboarding, e la catena per portarci un
     * valore dalle impostazioni le attraversa tutte. ⚠️ **E sta con l'aspetto dei riquadri
     * invece che per conto suo** perché il FAB apre il riquadro: chi sposta l'uno sposta
     * anche dove si apre l'altro.
     */
    val hand: Hand = Hand.RIGHT
)

/** Quello che i riquadri leggono, messo in scena accanto al tema. */
val LocalPadLook = compositionLocalOf { PadLook() }

/**
 * Da che angolo in basso sta il FAB, secondo l'impostazione.
 *
 * ⚠️⚠️ **NASCE NELLA `1.57` E PRENDE IL POSTO DELLA SPECCHIATURA** (tappa del piano d'azione,
 * e decisione dell'utente: *la specchiatura se ne va del tutto*). Prima l'impostazione diceva
 * quale **mano** si usa e rovesciava le file di un riquadro; adesso dice da che parte sta il
 * FAB, e con lui si sposta tutto quello che gli gira intorno.
 * ⚠️ **La chiave sull'archivio non cambia**, quindi chi aveva scelto la sinistra ritrova la
 * sinistra: la domanda ha cambiato forma ma non verso, ed è il caso in cui una chiave si
 * tiene invece di scriverne una nuova.
 * ⚠️ **`End` e `Start` e non 'destra' e 'sinistra' vere**: in arabo, persiano e urdu tutta
 * l'interfaccia si specchia, e un FAB inchiodato a destra sarebbe l'unico pezzo a non
 * seguirla. Nelle venticinque lingue che si leggono da sinistra le due cose coincidono.
 */
@Composable
fun fabSide(): Alignment =
    if (LocalPadLook.current.hand == Hand.RIGHT) Alignment.BottomEnd else Alignment.BottomStart

/**
 * Lo stesso lato, per chi allinea una fila invece di posare un tasto in un angolo.
 *
 * ⚠️⚠️ **NASCE NELLA `1.89` PER LE PASTIGLIE DELL'INTESTAZIONE** (sua richiesta, con schermata:
 * *quando le pastiglie vanno a capo, voglio che quella nella seconda ... sia centrata a destra o
 * a sinistra a seconda del lato in cui si trova il FAB. È un'impostazione trasparente ma molto
 * comoda*): la riga che va a capo si trova sotto il pollice invece che dalla parte opposta.
 * ⚠️ **Legge lo stesso valore di [fabSide] e non l'impostazione una seconda volta**: due letture
 * della stessa scelta sono due posti che divergono il giorno che la domanda cambia forma, come
 * è già successo alla specchiatura nella `1.57`.
 */
@Composable
fun fabEdge(): Alignment.Horizontal =
    if (LocalPadLook.current.hand == Hand.RIGHT) Alignment.End else Alignment.Start

/**
 * Quante colonne ha la bottomsheet della selezione: **cinque**, come chieste.
 *
 * ⚠️ Cinque e non tre come il menu, e non è simmetria: le azioni là sono dieci, e a tre
 * colonne verrebbero quattro file, cioè un pannello alto quanto mezzo schermo sopra le
 * fotografie che si stanno scegliendo.
 * ⚠️ **Non è privata perché la legge anche la pagina che RIORDINA i tasti** (`SettingsScreen`):
 * quella replica la scheda, e un 5 scritto una seconda volta sarebbe il numero che un giorno
 * diverge da questo.
 * ⚠️ **Fino alla `1.78` la ragione scritta era un'altra e non esisteva più**: diceva che la
 * leggeva chi rovesciava le file per la mano sinistra, e quella specchiatura è uscita del tutto
 * nella `1.57` (lo dichiara `GridScreen`). Il criterio regge identico, a cambiare era il
 * lettore.
 */
internal const val SHEET_COLUMNS = 5


/**
 * Lo smusso del FAB quadrato, uguale in tutte le schermate.
 *
 * ⚠️ Quadrato ma non tagliente: il tondo pieno griderebbe 'azione principale', e in questa
 * app l'azione principale sono sempre le fotografie. ⚠️ **Sta qui e non in una schermata**
 * perché i FAB sono due, quello delle cartelle e quello della selezione, e due numeri
 * uguali scritti in due file sono un numero che prima o poi diverge.
 */
val FAB_CORNER = 12.dp

/**
 * La misura di `SmallFloatingActionButton`, che [TapHoldFab] rifà a mano.
 *
 * ⚠️ È anche l'altezza che [FAB_REACH] somma al margine: i due numeri descrivono lo stesso
 * FAB, e slegati si sarebbero mossi uno per volta.
 */
val FAB_SIZE = 40.dp


/**
 * Il margine del FAB quadrato dalle due sponde della schermata delle cartelle.
 *
 * ⚠️ Sta qui e non là perché [FAB_REACH] lo somma: il giorno che il FAB si sposta di un
 * dp, il conto che tiene le cartelle sopra di lui deve muoversi con lui.
 */
val HUB_PAD = 16.dp

/**
 * Quanto arriva in su il FAB quadrato delle cartelle, misurato dal fondo dello schermo:
 * il suo margine ([HUB_PAD]) più la sua altezza.
 *
 * ⚠️⚠️ **È LA Y DA CUI PARTE LA SFUMATURA che inghiotte quello che sta sotto** (richiesta
 * dell'utente, dalla `0.77`). Comincia dove comincia il **FAB**, non dove finisce lo
 * spazio che gli si lascia, che è [BELOW_FAB] e vale una ventina di dp in più: la differenza
 * fra i due numeri è l'aria che al riposo resta fra l'ultima cartella e il FAB, e la
 * sfumatura deve trovarla vuota.
 * ⚠️ L'altezza è [FAB_SIZE], cioè la misura che Material dà a un FAB piccolo senza
 * esporla come costante pubblica: è un dato suo, non una nostra scelta.
 */
val FAB_REACH = HUB_PAD + FAB_SIZE

/**
 * Quanto spazio resta sotto l'ultimo elemento di una griglia, perché il FAB non gli si
 * sieda sopra.
 *
 * ⚠️ Serve **solo** quando il FAB c'è: nella griglia delle foto compare con la
 * selezione, quindi il fondo cresce da quel momento. Senza, la fotografia in basso a
 * destra resterebbe coperta proprio mentre si sta scegliendo, cioè quando la si deve poter
 * toccare.
 * ⚠️ **Scritto come [FAB_REACH] più aria** dalla `0.77`, e prima era 76dp nudi: i due numeri
 * descrivono la stessa cosa a due altezze diverse, e slegati si sarebbero mossi uno per
 * volta.
 */
val BELOW_FAB = FAB_REACH + 20.dp

/**
 * Se questa schermata sta uscendo verso una **senza FAB**.
 *
 * ⚠️⚠️ **LO FORNISCE `AivApp`, DENTRO LA TRANSIZIONE FRA SCHERMATE, ED È L'UNICO POSTO CHE SA
 * DOVE SI STA ANDANDO** (punto A del campo libero del giro accorpato: *quando dal menu del FAB
 * approdo ad una schermata senza FAB (esempio → Impostazioni), il pulsante deve sparire
 * rimpicciolendosi fino a sparire*). Una schermata non sa dove porta una voce del proprio menu,
 * e il FAB meno che mai: quello che sa tutti e due gli stati è chi li mette in scena.
 * ⚠️ **Lo legge [TapHoldFab] e non i suoi chiamanti**, così un FAB nuovo prende l'uscita per
 * costruzione: è lo stesso criterio per cui `lowered()` porta con sé il velo.
 * ⚠️ **Fuori dalla transizione vale `false`**, che è il valore giusto per chi non sta andando da
 * nessuna parte: una prova che monta il solo FAB lo trova fermo, com'è a riposo nell'app.
 */
internal val LocalSenzaFab = compositionLocalOf { false }

/**
 * Un FAB con **due** gesti: tocco breve e tocco lungo.
 *
 * ⚠️⚠️ **NON È `SmallFloatingActionButton`, e non è un capriccio**: quel composabile prende
 * un `onClick` solo, e il `modifier` che gli si passa finisce **fuori** dal suo `clickable`,
 * cioè come genitore. Un `combinedClickable` messo là non vedrebbe mai il tocco lungo, perché
 * nella passata `Main` il figlio consuma il down per primo: è esattamente il meccanismo che
 * aveva rotto il tocco lungo sulla griglia. Per avere due gesti su un FAB bisogna che di
 * nodo che ascolta ce ne sia **uno**.
 * ⚠️ **La misura è quella di Material**, [FAB_SIZE], e da lì non si scosta.
 * ⚠️⚠️ **MA 'LA RESA NON CAMBIA' NON È PIÙ VERO, E FINO ALLA `1.78` ERA SCRITTO QUI**: la nota
 * garantiva `primaryContainer` e 6dp d'ombra, cioè quello che `SmallFloatingActionButton` dà, e
 * di quelle tre cose ne resta una. L'ombra è a zero dalla `1.68` (il perché sta sulla costante
 * che la portava), e i due colori arrivano **dai chiamanti**, che passano le risorse dell'icona
 * dell'app: qui dentro la parola `primaryContainer` non compare. Chi legge la firma incontrava
 * prima questa nota e dopo i fatti che la smentiscono.
 * ⚠️ Il gesto sta **dentro** la `Surface` e non sul suo modificatore, così l'increspatura
 * prende il colore del contenuto ([ink]) invece di quello che c'era fuori.
 *
 * ⚠️⚠️ **STA QUI, CONDIVISO, DALLA 0.78**: i FAB col tocco lungo sono diventati **due**,
 * quello della selezione e quello quadrato delle cartelle, e differiscono per il **glifo** e
 * per quello che i due gesti fanno. Tutto il resto (misura, smusso, ombra, il nodo unico che
 * ascolta, l'etichetta del tocco lungo per il lettore di schermo) è la stessa cosa scritta
 * una volta.
 */
@Composable
fun TapHoldFab(
    /** Che cos'è il FAB, per il lettore di schermo: la sua azione breve. */
    label: String,
    container: Color,
    ink: Color,
    /**
     * Che cosa fa il tocco lungo, per il lettore di schermo.
     *
     * ⚠️ **Si DICHIARA, o resta una scorciatoia che esiste solo per chi vede**: l'etichetta la
     * legge il lettore di schermo fra le azioni disponibili sul FAB.
     * ⚠️ Arriva da fuori perché il gesto fa cose diverse a seconda della schermata e di dove si
     * è dentro di lei, e un'etichetta fissa ne annuncerebbe una mentre succede l'altra.
     */
    holdLabel: String,
    /**
     * Il FAB si stacca in una **finestra sua**, per restare sopra la sfocatura del suo menu.
     *
     * ⚠️⚠️ **RICHIESTA DELL'UTENTE, 1.39** (2026-09-03: *quando la sfocatura si applica dove c'è
     * un FAB, questo deve rimanere SOPRA l'area sfocata e velata*). Col velo di finestra (vedi
     * `WindowVeil`) non si può ritagliare un buco: quel velo sta **dietro** la finestra che lo
     * chiede, e tutto quello che è più in basso ci finisce sotto, FAB compreso. La sola via
     * per tenerlo fuori è metterlo in una finestra **più in alto** di quella che vela.
     * ⚠️ **Vale solo per il menu che il FAB stesso apre**, e chi lo accende è il suo `open`.
     * Un dialogo di Material è una finestra di **altro tipo**, sempre sopra le finestre dei menu,
     * quindi con un dialogo aperto il FAB resta velato: ed è giusto, perché un modale deve
     * restare modale.
     * ⚠️⚠️ **IL MENU VA COMPOSTO PRIMA DEL TASTINO**, o questo non serve a niente: fra finestre
     * dello stesso tipo l'ordine è quello in cui sono state aggiunte, e la composizione decide
     * quell'ordine. I due posti che lo usano hanno il menu scritto sopra.
     * ⚠️⚠️ **DA STACCATO IL TASTINO È SOLO DA GUARDARE**, e la sua finestra lascia passare le
     * dita: il perché sta su [untouchable], ed è quello che tiene in piedi la chiusura del menu
     * al tocco, che è del giro della `1.06`.
     */
    lifted: Boolean = false,
    /**
     * Se il FAB deve mostrarsi **premuto**, cioè col rimbalzo fatto, la × al posto del glifo
     * e l'accento dell'altro tema.
     *
     * ⚠️⚠️ **È UN SEGNALE A SÉ E NON [lifted], DALLA `1.60`, PERCHÉ I DUE NON FINISCONO
     * INSIEME** (riscontro del giro della `1.59`: *niente rimbalzo, solo un movimento unico*).
     * [lifted] dice se il FAB vive nella sua finestra, quindi resta vero per tutta la
     * **discesa** del menu (`MenuState.visible`); e legandoci lo stato premuto, il ritorno del
     * FAB cominciava solo dopo che il menu era sparito del tutto. Erano due movimenti con una
     * pausa in mezzo, ed è quello che si legge come un secondo tempo.
     * ⚠️ **Chi apre un menu passa `wanted`**, che è il verso opposto: cade nell'istante in cui si
     * chiede la chiusura, quindi il FAB torna al suo colore **insieme** al menu che se ne va.
     * ⚠️ **Il valore di serie è [lifted]** perché per chi non ha un menu i due coincidono, e un
     * FAB senza menu non ha nessun secondo tempo da evitare.
     */
    pressed: Boolean = lifted,
    onTap: () -> Unit,
    onHold: () -> Unit,
    /**
     * Che cosa si vede sul FAB, con la descrizione da dare al lettore di schermo.
     *
     * ⚠️⚠️ **È UNA FESSURA E NON UN `ImageVector`, dalla `1.55`**: il FAB della schermata
     * iniziale porta il **marchio dell'app**, che è un disegno più largo che alto e va messo in
     * scena con una misura sua e uno spostamento suo (vedi `Marchio` in `FolderScreen.kt`). Un
     * `ImageVector` obbligava a una scatola quadrata da 24dp, che quel disegno schiaccia.
     */
    glyph: @Composable (descrizione: String?) -> Unit
) {
    /*
     * ⚠️⚠️ **IL TASTINO CAMBIA TRE COSE AL TOCCO, DALLA `1.68`, E NESSUNA DELLE TRE È UNA
     * QUANTITÀ** (istruzione dell'utente, giro della `1.67`): rimbalza, il suo glifo diventa una
     * ×, e il fondo prende l'accento dell'**altro** tema. Il perché di questo cambio di strada,
     * dopo tre versioni che alzavano un numero senza mai farsi vedere, sta su [RIMBALZO].
     * ⚠️ **La × dice che cosa fa adesso il tasto**, e sono sue parole: *indicando che la sua
     * nuova funzione è chiudere il menu*. Quindi non è una decorazione: è l'unica cosa sul
     * FAB che comunichi un'azione diversa da quella di prima.
     * ⚠️⚠️ **DUE ANIMAZIONI SI LEGGONO NEL DISEGNO E UNA IN COMPOSIZIONE, ed è una scelta
     * misurata**: il rimbalzo e la dissolvenza dei simboli stanno dentro `graphicsLayer`, cioè
     * costano un ridisegno per fotogramma; il colore invece arriva a `Surface`, che lo vuole in
     * composizione. Costa [TINTA_MS] di ricomposizioni, cioè tre o quattro fotogrammi, ed è il
     * prezzo per avere l'increspatura del tocco e il `contentColor` che Material dà da sé.
     * ⚠️ **Il primo giro non anima**: `LaunchedEffect` parte anche alla prima composizione, e
     * senza questa guardia il FAB rimbalzerebbe ogni volta che la sua schermata entra in scena.
     */
    val morph = remember { Animatable(if (pressed) 1f else 0f) }
    val tinta = remember { Animatable(if (pressed) 1f else 0f) }
    val rimbalzo = remember { Animatable(1f) }
    var primo by remember { mutableStateOf(true) }
    /*
     * ⚠️⚠️ **L'USCITA VERSO UNA SCHERMATA SENZA FAB, DALLA `2.11`, ED È LA VARIANTE 1 DELLE DUE
     * CHE HA DESCRITTO** (punto A del campo libero: *1) si rimpicciolisce tutto, incluso il glifo.
     * 2) si rimpicciolisce il pulsante, il glifo resta identico ma il pulsante fa da maschera*).
     * La scelta è dichiarata perché ha detto *decidi tu*: la seconda taglia il glifo mentre il
     * cerchio si stringe, cioè fa vedere per qualche fotogramma un disegno mutilato, mentre la
     * scala uniforme è già il linguaggio con cui questo tasto si muove (il rimbalzo, l'entrata
     * del cestino).
     * ⚠️⚠️ **SI MOLTIPLICA AL RIMBALZO INVECE DI SOSTITUIRLO, e i due possono capitare insieme**:
     * toccando una voce del menu il FAB sta ancora tornando su dal suo rimbalzo, e due scale
     * scritte su due nodi darebbero un tasto che si stringe mentre un altro lo allarga.
     * ⚠️⚠️ **PIÙ CORTA DELLA DISSOLVENZA DI SCHERMATA, E IL CONTO È QUELLO DELLA `1.73`**: le
     * opacità si moltiplicano, quindi quello che il FAB fa in quei 180 ms lo fa dietro un velo
     * che cala. A [VIA_MS] la scala è a zero quando l'opacità è ancora sopra i due terzi, cioè il
     * rimpicciolimento si vede tutto e quello che sparisce dopo è un tasto già sparito.
     * ⚠️ **Non c'è un ritorno da animare**: la schermata che rientra compone un FAB nuovo, che
     * nasce a uno. Lo `snapTo` copre il caso in cui la navigazione venga annullata, dove il tasto
     * deve tornare intero senza una seconda animazione che non racconta niente.
     */
    val via = remember { Animatable(1f) }
    val senzaFab = LocalSenzaFab.current
    LaunchedEffect(senzaFab) {
        if (senzaFab) via.animateTo(0f, tween(VIA_MS, easing = FastOutLinearInEasing))
        else via.snapTo(1f)
    }
    LaunchedEffect(pressed) {
        val a = if (pressed) 1f else 0f
        if (primo) {
            primo = false
            morph.snapTo(a)
            tinta.snapTo(a)
            return@LaunchedEffect
        }
        launch { morph.animateTo(a, tween(MORPH_MS, easing = FastOutSlowInEasing)) }
        launch { tinta.animateTo(a, tween(TINTA_MS, easing = FastOutSlowInEasing)) }
        rimbalzo.animateTo(1f - RIMBALZO, tween(RIMBALZO_GIU_MS, easing = FastOutSlowInEasing))
        rimbalzo.animateTo(1f, tween(RIMBALZO_SU_MS, easing = FastOutSlowInEasing))
    }
    /*
     * ⚠️⚠️ **DALLA `1.86` I DUE ESTREMI SONO SCAMBIATI, ED È SUO** (riscontro del giro della
     * `1.85`, campo libero punto A: *il FAB di tutte le pagine incluso il cestino deve avere il
     * FAB del colore del tema scuro, che poi diventa chiaro quando premuto*, e al rovescio sul
     * tema scuro). Fino alla `1.85` il FAB stava nell'accento **di questo** tema e passava a
     * quello dell'altro da premuto; adesso a riposo porta l'altro e da premuto torna a questo.
     * La sua ragione è scritta: *dà uno stacco maggiore, e un accento opposto mette più in
     * risalto il pulsante flottante*.
     * ⚠️ **Il colore a riposo lo passa il chiamante** ([container]), perché non è sempre quello
     * dell'app: i due FAB che un mini-onboarding evidenzia portano l'arancione. Quello che
     * questa funzione decide è il **secondo** estremo, cioè dove va il colore quando si preme.
     * ⚠️ **Anche l'inchiostro cambia**, e senza di lui il contrasto cadrebbe: sull'accento scuro
     * (`#00727B`) l'inchiostro del tema chiaro (`#00382F`) misura 2,05, cioè illeggibile. Le due
     * coppie sono quelle di Material e stanno insieme per costruzione.
     */
    val light = LocalAivLight.current
    val fondo = lerp(container, aivAccent(light), tinta.value)
    val segno = lerp(ink, aivOnAccent(light), tinta.value)
    /*
     * ⚠️ **I due simboli stanno uno sopra l'altro in una scatola sola**, e la scatola si misura
     * sul più grande: così il tasto non si rimisura mentre la dissolvenza va, e il glifo che sta
     * uscendo non trascina il posto di quello che entra.
     */
    val simboli = @Composable { descrizione: String? ->
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.graphicsLayer {
                    val m = morph.value
                    alpha = 1f - m
                    val s = 1f - GLIFO_ZOOM * m
                    scaleX = s
                    scaleY = s
                }
            ) { glyph(descrizione) }
            Box(
                modifier = Modifier.graphicsLayer {
                    val m = morph.value
                    alpha = m
                    val s = 1f - GLIFO_ZOOM * (1f - m)
                    scaleX = s
                    scaleY = s
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    // ⚠️ Muta: a parlare è il glifo dell'app, e due descrizioni sullo stesso
                    // tasto dànno due voci per un tasto solo.
                    contentDescription = null,
                    modifier = Modifier.size(CHIUDI_LATO)
                )
            }
        }
    }
    /*
     * ⚠️ **Il rimbalzo sta sul nodo che contiene tutto**, fondo e simboli: è il tasto che
     * rimbalza, e scalare il solo fondo lascerebbe il glifo fermo in mezzo a una piastrella che
     * si muove.
     */
    val tasto = @Composable { muto: Boolean ->
        Surface(
            modifier = Modifier
                .size(FAB_SIZE)
                .graphicsLayer {
                    val k = rimbalzo.value * via.value
                    scaleX = k
                    scaleY = k
                }
                // ⚠️ Da staccato il FAB non prende i tocchi ([untouchable]), quindi non è
                // più un comando: annunciarlo darebbe un tasto che il lettore di schermo trova e
                // che non fa niente.
                .then(if (muto) Modifier.clearAndSetSemantics { } else Modifier),
            shape = RoundedCornerShape(FAB_CORNER),
            color = fondo,
            contentColor = segno,
            shadowElevation = 0.dp
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
                simboli(label)
            }
        }
    }

    /*
     * ⚠️⚠️ **IL FAB DICHIARA QUANTO OCCUPA DEL FONDO DELLO SCHERMO, COSÌ LA NOTIFICA NON GLI FINISCE
     * SOPRA** (segnalazione dell'utente, punto A1 del giro della `2.23`): il perché a scansarsi sia
     * la notifica e non questo tasto vive su [Modifier.aboveFoot].
     * ⚠️⚠️ **DALLA `2.25` DICHIARA UN FIANCO E NON UNA FASCIA, ED È IL SUO RISCONTRO** (giro della
     * `2.24`, voce `avviso-fab`, e risposta `stringe` a `d-avviso-forma`): la `2.24` faceva salire
     * la notifica sopra di lui, e lui ha chiesto che si stringa e resti in fondo, perché *di
     * fianco* al FAB lo spazio c'è.
     * ⚠️⚠️ **DA CHE PARTE STA SI MISURA E NON SI LEGGE DA UNA PREFERENZA**: il nodo sa dov'è nella
     * finestra, quindi il lato si ricava dal suo centro e la larghezza da occupare è il pezzo che
     * lo separa dal bordo, margine compreso. Leggendo `fabSide` ci sarebbero due posti a decidere
     * dov'è il FAB, e il giorno che uno dei due cambia la notifica si stringerebbe dalla parte
     * sbagliata.
     * ⚠️ **Vive QUI e non nei due chiamanti**, così un FAB nuovo lo dichiara per costruzione: è lo
     * stesso criterio per cui `lowered()` si porta dietro il velo, e per cui l'uscita verso una
     * schermata senza FAB la legge questa funzione invece dei suoi chiamanti.
     * ⚠️ **Una chiave per ogni FAB in scena**: durante la dissolvenza fra due schermate ce ne sono
     * due, e con una chiave sola il secondo cancellerebbe la misura del primo.
     */
    val quota = remember { Any() }
    val larga = LocalWindowInfo.current.containerSize.width
    DisposableEffect(quota) { onDispose { FootStage.off(quota) } }
    val misura = Modifier.onGloballyPositioned {
        val da = it.positionInWindow().x.roundToInt()
        val fino = da + it.size.width
        val destra = da + fino > larga
        FootStage.beside(quota, if (destra) larga - da else fino, destra)
    }

    if (!lifted) {
        Box(modifier = misura) { tasto(false) }
        return
    }
    /*
     * ⚠️ **Il posto resta occupato da una scatola della stessa misura**, e non è un dettaglio:
     * una finestra non occupa spazio nel genitore, quindi senza questa scatola il riquadro si
     * stringerebbe e il menu, che si ancora a lui, salterebbe altrove proprio mentre si apre.
     * ⚠️ **`Alignment.TopStart` mette la finestra sull'angolo dell'ancora**, cioè esattamente
     * dove il FAB sarebbe stato: il FAB non si muove, cambia solo la finestra che lo
     * disegna.
     *
     * ⚠️⚠️ **IL SOSIA C'È SOLO FINCHÉ LA FINESTRA NON DISEGNA, DALLA `1.68`, ED È UNA SUA
     * ISTRUZIONE** (giro della `1.67`: *a menu aperto, deve esistere SOLO il FAB ricolorato SOPRA
     * la sfocatura (se presente); nel livello della sfocatura non dev'esserci nessun FAB*). Prima
     * il sosia restava per tutto il tempo, e con la sfocatura accesa si vedeva un FAB sfocato
     * dietro quello nitido: due FAB invece di uno.
     * ⚠️⚠️ **MA NON SI PUÒ TOGLIERE DEL TUTTO, o torna il lampo della `1.46`** (*il tastino FAB
     * fa un flash*): passare da 'disegnato qui' a 'disegnato in una finestra sua' vuol dire
     * togliere un nodo e chiedere al gestore delle finestre di aggiungerne una, e le due cose non
     * capitano nello stesso fotogramma. Per quel fotogramma il FAB non ci sarebbe da nessuna
     * parte. Quindi il sosia riempie **solo** quel buco, e se ne va appena la finestra ha
     * disegnato.
     * ⚠️ **`withFrameNanos` e non un semplice effetto**: un `DisposableEffect` dentro il `Popup`
     * si esegue quando la sua composizione finisce, che è prima che quella finestra abbia
     * disegnato; aspettare un fotogramma è la cosa più corta che garantisca che ci sia qualcosa
     * da vedere sopra.
     * ⚠️ **IL SOSIA NON È UNA `Surface`, e non è pigrizia**: una `Surface` di Material si mangia i
     * tocchi (ha un `pointerInput` suo anche senza `onClick`), e qui sotto i tocchi devono
     * **passare**, perché è il velo trasparente della schermata a raccoglierli e a chiudere il
     * menu. È la chiusura al tocco del giro della `1.06`, la stessa che [untouchable] protegge
     * dall'altra parte.
     *
     * ⚠️⚠️ **IL SOSIA SI CENTRA CON `align` E NON COL `contentAlignment` DELLA SCATOLA, dalla
     * `1.62`**: un `Popup` emette un nodo di misura zero e ricava da **dove quel nodo atterra** il
     * rettangolo a cui ancorarsi. Un `contentAlignment` centrato sposterebbe anche quel nodo, e la
     * finestra del FAB vero nascerebbe mezza scatola più in là.
     */
    var suo by remember { mutableStateOf(false) }
    Box(modifier = Modifier.size(FAB_SIZE)) {
        if (!suo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(FAB_SIZE)
                    .graphicsLayer {
                        val k = rimbalzo.value
                        scaleX = k
                        scaleY = k
                    }
                    .background(fondo, RoundedCornerShape(FAB_CORNER)),
                contentAlignment = Alignment.Center
            ) {
                /*
                 * ⚠️ **La tinta si dà col `CompositionLocal` e non a mano**: qui non c'è la
                 * `Surface` che nel FAB vero porta `contentColor`, e la fessura disegna un
                 * `Icon` che quel colore lo legge da lì. Passarlo come parametro vorrebbe dire
                 * chiedere a chi riempie la fessura di saperlo, e i due disegni divergerebbero.
                 */
                CompositionLocalProvider(LocalContentColor provides segno) { simboli(null) }
            }
        }
        Popup(alignment = Alignment.TopStart) {
            untouchable()
            LaunchedEffect(Unit) {
                withFrameNanos { }
                suo = true
            }
            DisposableEffect(Unit) { onDispose { suo = false } }
            tasto(true)
        }
    }
}

/**
 * Rende la finestra che ospita questa vista **trasparente al tocco**: le dita ci passano
 * attraverso e arrivano a quello che sta sotto.
 *
 * ⚠️⚠️ **SENZA QUESTA RIGA IL TASTINO STACCATO ROMPEREBBE LA CHIUSURA DEL MENU, dalla 1.06**
 * (*tutti i menu di tutti i FAB devono andarsene se si tocca un punto qualsiasi fuori dal
 * popup, incluso il FAB stesso*). Quel tocco oggi lo raccoglie il velo trasparente della
 * schermata, che vive nella finestra dell'app: un FAB in una finestra **più alta** se lo
 * prenderebbe per primo e il menu resterebbe aperto, con l'aggravante che il suo `onTap` lo
 * riaprirebbe subito dopo. È il lampeggio che la `1.06` aveva chiuso.
 * ⚠️ **Quindi il FAB staccato è solo da guardare**, ed è giusto così: mentre il suo menu
 * è aperto l'unica cosa che il suo tocco deve fare è chiudere quel menu, e a chiuderlo pensa
 * chi lo faceva già.
 * ⚠️ **Si passa dai `LayoutParams` della radice**, come il velo dei popup: un `Popup` non ha
 * un `Window` suo, e la sua finestra sono i parametri della vista che Compose ha aggiunto al
 * gestore. La nota per esteso sta in `Veil.kt`.
 * ⚠️ **Niente da rimettere a posto all'uscita**: la finestra muore col popup, e questa esiste
 * solo finché il FAB sta per conto suo.
 */
@Composable
private fun untouchable() {
    val view = LocalView.current
    DisposableEffect(view) {
        val root = view.rootView
        val params = root.layoutParams as? WindowManager.LayoutParams
        val manager = view.context.getSystemService(WindowManager::class.java)
        if (params != null && manager != null) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            runCatching { manager.updateViewLayout(root, params) }
        }
        onDispose { }
    }
}

/**
 * Il **marchio dell'app** sul FAB, al posto dei tre puntini.
 *
 * ⚠️⚠️ **RICHIESTA DELL'UTENTE, giro della `1.54`** (*sostituisci i tre pallini del FAB
 * principale con il glifo dell'app ... Il glifo è da centrare OTTICAMENTE*). I tre puntini non
 * sono spariti: sono scesi sul FAB del cestino, dove c'era un disco ancora più generico.
 *
 * ⚠️⚠️ **CENTRATO OTTICAMENTE VUOL DIRE CHE IL SUO BARICENTRO STA AL CENTRO, e i due numeri
 * sono MISURATI e non scelti**: reso il disegno in Chromium a 700 x 600 e pesato l'inchiostro
 * pixel per pixel, il baricentro cade al 5,0% della larghezza a **sinistra** del centro del
 * riquadro e al 9,4% dell'altezza **sotto** di lui. Lo spostamento è quello, cambiato di segno.
 * - **Perché cade lì, e conviene saperlo**: la A è un triangolo, quindi ha la massa in basso, e
 *   il disco solare sta in alto a **sinistra**. Sono due cose che tirano da parti diverse, e a
 *   occhio non si indovinano.
 * - ⚠️ **La misura si rifà se il disegno cambia**: `ic_aiv_mark.xml` è l'inchiostro nudo (la
 *   tela è esattamente il riquadro dei due tracciati), quindi basta rendere quel file e pesarlo.
 *
 * ⚠️ **Si dà la larghezza e l'altezza segue**, come nella barra delle info: il glifo è 70 x 60,
 * e una misura sola lo schiaccerebbe.
 */
@Composable
internal fun Marchio(descrizione: String?) {
    Icon(
        imageVector = Glyphs.AivMark,
        contentDescription = descrizione,
        modifier = Modifier
            .offset(x = MARK_WIDE * MARK_DX, y = MARK_HIGH * MARK_DY)
            .size(width = MARK_WIDE, height = MARK_HIGH)
    )
}

/**
 * Quanto è largo il marchio sul FAB.
 *
 * ⚠️ **24dp è la scatola che avevano i tre puntini**, cioè la misura standard di un glifo di
 * Material: il FAB è 40dp, quindi restano otto punti d'aria per lato. Chi lo volesse più
 * discreto muove questo numero e basta: l'altezza e lo spostamento lo seguono.
 */
private val MARK_WIDE = 24.dp

/** L'altezza che segue dalla forma del disegno, 70 x 60. */
private val MARK_HIGH = MARK_WIDE * 60f / 70f

/** Lo spostamento ottico, in frazione del glifo: vedi la misura in testa a [Marchio]. */
private const val MARK_DX = 0.050f
private const val MARK_DY = -0.094f

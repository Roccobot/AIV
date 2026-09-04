package io.github.roccobot.aiv

import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.Redo
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
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
     * tastino, la larghezza fissa resta quella giusta: là è il riquadro a doversi
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
     */
    val cella = if (labels) PAD_CELL else PAD_CELL_BARE
    val avvio = (MENU_ICON_MID - cella / 2).coerceAtLeast(PAD_EDGE)
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
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(PAD_ICON)
                        )
                        Text(
                            text = stringResource(chiave.label()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
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
 * ⚠️ **Il tastino della selezione se n'è andato con lei** (stessa istruzione: *il FAB di
 * selezione non serve più*), e la ragione l'ha trovata l'utente: se il menu si apre da sé,
 * un tastino che lo apre non ha più niente da fare.
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
        enter = slideInVertically(
            /*
             * ⚠️ **È la molla di fabbrica scritta a mano**, e la ragione di scriverla è che
             * adesso il suo numero vive in un posto solo ([ARRIVO_RIGIDITA] in `Sheet.kt`):
             * lasciata implicita, un ritocco là non arriverebbe qui e le due schede
             * tornerebbero a muoversi in due modi.
             */
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = ARRIVO_RIGIDITA,
                visibilityThreshold = IntOffset.VisibilityThreshold
            ),
            initialOffsetY = { it }
        ) + fadeIn(animationSpec = tween(durationMillis = SHEET_FADE_MS)),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = USCITA_MS, easing = ACCELERA),
            targetOffsetY = { it }
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = SHEET_FADE_MS,
                delayMillis = USCITA_MS - SHEET_FADE_MS
            )
        ),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
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
            modifier = Modifier.edgedTop(SHEET_CORNER),
            shape = RoundedCornerShape(topStart = SHEET_CORNER, topEnd = SHEET_CORNER),
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
             * ⚠️ **Il rilievo TONALE resta**: quello non è un'ombra ma il colore della
             * superficie, ed è la cosa che stacca la scheda dalla griglia dietro.
             */
            tonalElevation = SHEET_LIFT
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

    // La prima fila dell'editor: girare e centrare.
    TURN_LEFT("turn-left"),
    TURN_RIGHT("turn-right"),
    CENTRE_ACROSS("centre-across"),
    CENTRE_DOWN("centre-down"),

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
    // ⚠️ Le due frecce di sistema, come nell'editor: là sono dichiarate deprecate e usate
    // lo stesso, perché le sostitute girano dalla parte sbagliata (vedi la nota là).
    @Suppress("DEPRECATION") PadKey.TURN_LEFT -> Icons.Default.RotateLeft
    @Suppress("DEPRECATION") PadKey.TURN_RIGHT -> Icons.Default.RotateRight
    PadKey.CENTRE_ACROSS -> Glyphs.AlignAcross
    PadKey.CENTRE_DOWN -> Glyphs.AlignDown
    PadKey.ORIGINAL -> Icons.Outlined.RestartAlt
    PadKey.UNDO -> Icons.AutoMirrored.Outlined.Undo
    PadKey.REDO -> Icons.AutoMirrored.Outlined.Redo
    PadKey.APPLY -> Icons.Outlined.Check
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

/** Quante colonne ha il riquadro: tre, come l'utente le ha chieste. */
private const val PAD_COLUMNS = 3

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

/**
 * Il margine laterale del riquadro dentro il menu che lo contiene.
 *
 * ⚠️ **Dalla `1.59` è il PAVIMENTO del fianco sinistro e non più il fianco**: quello lo decide
 * la colonna delle icone della lista, e questo numero interviene solo dove quel conto darebbe
 * un rientro negativo. A destra resta il margine di sempre.
 */
private val PAD_EDGE = 4.dp

/**
 * Quanto si rimpicciolisce il tastino mentre il suo menu è aperto.
 *
 * ⚠️ **Sei centesimi e non di più**: la richiesta era *molto sobrio*, e su un tastino da
 * [FAB_SIZE] questo vale 2,4dp, cioè poco più di un pixel per lato su uno schermo denso. Basta
 * a leggersi come una pressione, e non abbastanza a sembrare un movimento.
 */
private const val PREMUTO_GIU = 0.06f

/**
 * Quanto si sposta la tinta del tastino verso il suo inchiostro mentre il menu è aperto.
 *
 * ⚠️ **È la frazione degli strati di stato di Material**, che per una superficie premuta sta
 * fra un decimo e un ottavo. Preso da lì, il tastino premuto si comporta come ogni altra
 * superficie dell'app in tutti e due i temi.
 */
private const val PREMUTO_TINTA = 0.12f

/**
 * In quanto tempo il tastino si preme e si rilascia.
 *
 * ⚠️ **La stessa del menu che apre**, e deve esserlo: sono la stessa azione vista da due parti,
 * e due durate diverse darebbero un tastino che finisce di premersi mentre il menu è già lì.
 */
private const val PREMUTO_MS = 120

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
 * ⚠️⚠️ **STA IN UN `CompositionLocal` PER LA STESSA RAGIONE DEL VELO** (vedi `LocalAivVeil`):
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
     * Da che parte dello schermo sta il tastino.
     *
     * ⚠️⚠️ **VIAGGIA QUI E NON PER PARAMETRO, ed è la stessa ragione delle altre cinque**: i
     * tastini vivono in tre schermate e in un velo di onboarding, e la catena per portarci un
     * valore dalle impostazioni le attraversa tutte. ⚠️ **E sta con l'aspetto dei riquadri
     * invece che per conto suo** perché il tastino apre il riquadro: chi sposta l'uno sposta
     * anche dove si apre l'altro.
     */
    val hand: Hand = Hand.RIGHT
)

/** Quello che i riquadri leggono, messo in scena accanto al tema. */
val LocalPadLook = compositionLocalOf { PadLook() }

/**
 * Da che angolo in basso sta il tastino, secondo l'impostazione.
 *
 * ⚠️⚠️ **NASCE NELLA `1.57` E PRENDE IL POSTO DELLA SPECCHIATURA** (tappa del piano d'azione,
 * e decisione dell'utente: *la specchiatura se ne va del tutto*). Prima l'impostazione diceva
 * quale **mano** si usa e rovesciava le file di un riquadro; adesso dice da che parte sta il
 * tastino, e con lui si sposta tutto quello che gli gira intorno.
 * ⚠️ **La chiave sull'archivio non cambia**, quindi chi aveva scelto la sinistra ritrova la
 * sinistra: la domanda ha cambiato forma ma non verso, ed è il caso in cui una chiave si
 * tiene invece di scriverne una nuova.
 * ⚠️ **`End` e `Start` e non 'destra' e 'sinistra' vere**: in arabo, persiano e urdu tutta
 * l'interfaccia si specchia, e un tastino inchiodato a destra sarebbe l'unico pezzo a non
 * seguirla. Nelle venticinque lingue che si leggono da sinistra le due cose coincidono.
 */
@Composable
fun fabSide(): Alignment =
    if (LocalPadLook.current.hand == Hand.RIGHT) Alignment.BottomEnd else Alignment.BottomStart

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
 * ⚠️ **È il rilievo TONALE e basta, dalla 1.40**: l'ombra è uscita su richiesta dell'utente, e
 * il perché sta sulla `Surface` di [PickSheet]. Restava il dubbio che sul tema scuro il tono da
 * solo non bastasse (là i toni si somigliano tutti): il riscontro dice che basta.
 */
private val SHEET_LIFT = 6.dp

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
    /**
     * Il tastino si stacca in una **finestra sua**, per restare sopra il velo del suo menu.
     *
     * ⚠️⚠️ **RICHIESTA DELL'UTENTE, 1.39** (2026-09-03: *quando la sfocatura si applica dove
     * c'è un FAB, questo deve rimanere SOPRA l'area sfocata e velata*). Col velo di finestra
     * (vedi `WindowVeil`) non si può ritagliare un buco: quel velo sta **dietro** la finestra
     * che lo chiede, e tutto quello che è più in basso ci finisce sotto, tastino compreso. La
     * sola via per tenerlo fuori è metterlo in una finestra **più in alto** di quella che vela.
     * ⚠️ **Vale solo per il menu che il tastino stesso apre**, e chi lo accende è il suo
     * `open`. Un dialogo di Material è una finestra di **altro tipo**, sempre sopra le finestre
     * dei menu, quindi con un dialogo aperto il tastino resta velato: ed è giusto, perché un
     * modale deve restare modale.
     * ⚠️⚠️ **IL MENU VA COMPOSTO PRIMA DEL TASTINO**, o questo non serve a niente: fra
     * finestre dello stesso tipo l'ordine è quello in cui sono state aggiunte, e la
     * composizione decide quell'ordine. I due posti che lo usano hanno il menu scritto sopra.
     * ⚠️⚠️ **DA STACCATO IL TASTINO È SOLO DA GUARDARE**, e la sua finestra lascia passare le
     * dita: il perché sta su [untouchable], ed è quello che tiene in piedi la chiusura del menu
     * al tocco, che è del giro della `1.06`.
     * ⚠️ **L'incognita è se una finestra basti a stare sopra il velo**, e il caso peggiore è
     * dichiarato: se non bastasse, il tastino resterebbe velato come prima della `1.39`, che è
     * il comportamento di allora e non un guasto nuovo.
     */
    lifted: Boolean = false,
    onTap: () -> Unit,
    onHold: () -> Unit,
    /**
     * Che cosa si vede sul tastino, con la descrizione da dare al lettore di schermo.
     *
     * ⚠️⚠️ **È UNA FESSURA E NON UN `ImageVector`, dalla `1.55`**: il tastino della schermata
     * iniziale porta il **marchio dell'app**, che è un disegno più largo che alto e va messo in
     * scena con una misura sua e uno spostamento suo (vedi `Marchio` in `FolderScreen.kt`). Un
     * `ImageVector` obbligava a una scatola quadrata da 24dp, che quel disegno schiaccia.
     * ⚠️ **La descrizione arriva da qui perché i due tastini disegnati sono due**: quello vero
     * parla al lettore di schermo, il sosia che tiene il posto no, o si sentirebbero due voci per
     * un tasto solo. Chi riempie la fessura passa la stringa all'`Icon` e basta.
     */
    glyph: @Composable (descrizione: String?) -> Unit
) {
    /*
     * ⚠️⚠️ **IL TASTINO SEMBRA PREMUTO FINCHÉ IL SUO MENU È IN SCENA, dalla `1.59`** (richiesta
     * dell'utente, giro della `1.58`: *quando lo tocchi, mentre appare il menu lui si
     * rimpicciolisce leggermente e si schiarisce/scurisce un poco. All'opposto, quando il
     * pannello si chiude, torna al suo colore e dimensione originali. In questo modo sembra più
     * un tasto premuto*).
     * ⚠️⚠️ **NON È L'INCRESPATURA DEL TOCCO, ed è la differenza che lo rende utile**: quella
     * dura il tempo del dito e dice 'ti ho sentito'; questa dura quanto il menu e dice 'sono io
     * che l'ho aperto'. Con un menu che si apre lontano dal dito, senza questa il tastino resta
     * un tasto qualunque mentre la sua conseguenza è sullo schermo.
     * ⚠️ **La tinta si sposta verso l'INCHIOSTRO del tastino e non verso il bianco o il nero**:
     * così schiarisce nel tema chiaro e nel tema scuro senza sapere in quale si trova, perché
     * l'inchiostro è per definizione il colore che si stacca dal fondo del tastino. È anche il
     * modo in cui Material fa i suoi strati di stato, quindi non è un'invenzione locale.
     * ⚠️ **Vale sui DUE disegni**, il sosia e quello nella finestra: sono lo stesso tastino
     * visto in due momenti, e uno solo dei due animato darebbe uno scatto nello scambio.
     */
    val premuto by animateFloatAsState(
        targetValue = if (lifted) 1f else 0f,
        animationSpec = tween(PREMUTO_MS),
        label = "premuto"
    )
    val scala = 1f - PREMUTO_GIU * premuto
    val tinta = lerp(container, ink, PREMUTO_TINTA * premuto)
    val schiaccia = Modifier.graphicsLayer { scaleX = scala; scaleY = scala }
    val tasto = @Composable { alza: Dp ->
        Surface(
            modifier = Modifier.size(FAB_SIZE).then(schiaccia),
            shape = RoundedCornerShape(FAB_CORNER),
            color = tinta,
            contentColor = ink,
            shadowElevation = alza
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
                glyph(label)
            }
        }
    }

    if (!lifted) {
        tasto(lift)
        return
    }
    /*
     * ⚠️ **Il posto resta occupato da una scatola della stessa misura**, e non è un dettaglio:
     * una finestra non occupa spazio nel genitore, quindi senza questa scatola il riquadro si
     * stringerebbe e il menu, che si ancora a lui, salterebbe altrove proprio mentre si apre.
     * ⚠️ **`Alignment.TopStart` mette la finestra sull'angolo dell'ancora**, cioè esattamente
     * dove il tastino sarebbe stato: il tastino non si muove, cambia solo la finestra che lo
     * disegna.
     *
     * ⚠️⚠️ **DENTRO LA SCATOLA C'È UN SOSIA, E SENZA DI LUI IL TASTINO LAMPEGGIA** (riscontro
     * dell'utente, giro della `1.46`: *il tastino FAB fa un flash*). La causa è nello scambio
     * stesso: passare da 'disegnato qui' a 'disegnato in una finestra sua' vuol dire togliere
     * un nodo e chiedere al gestore delle finestre di aggiungerne una, e le due cose non
     * capitano nello stesso fotogramma. Per quel fotogramma il tastino **non c'è da nessuna
     * parte**, e la stessa cosa succede alla chiusura, al contrario. Il sosia riempie il buco:
     * sta sempre lì, e il tastino vero gli si posa sopra quando la sua finestra è pronta.
     * ⚠️⚠️ **L'OMBRA CE L'HA IL SOSIA E NON QUELLO SOPRA**, o sarebbero due ombre sovrapposte
     * per tutto il tempo in cui il menu è aperto: quella sotto vive nella finestra della
     * schermata e quella sopra nella sua, quindi si sommerebbero invece di coincidere. Con
     * l'ombra a chi sta sotto, la sagoma disegnata resta una sola in ogni istante, e col velo
     * acceso l'ombra cade sul velo, che è dove deve cadere.
     * ⚠️⚠️ **IL SOSIA NON È UNA `Surface`, e non è pigrizia**: una `Surface` di Material si
     * mangia i tocchi (ha un `pointerInput` suo anche senza `onClick`), e qui sotto i tocchi
     * devono **passare**, perché è il velo trasparente della schermata a raccoglierli e a
     * chiudere il menu. È la chiusura al tocco del giro della `1.06`, la stessa che [untouchable]
     * protegge dall'altra parte.
     * ⚠️ **E non parla al lettore di schermo**: il tastino vero è quello di sopra, e un sosia
     * che si annunciasse darebbe due voci per un tasto solo.
     */
    Box(modifier = Modifier.size(FAB_SIZE)) {
        Box(
            modifier = Modifier
                .size(FAB_SIZE)
                .then(schiaccia)
                .shadow(lift, RoundedCornerShape(FAB_CORNER))
                .background(tinta, RoundedCornerShape(FAB_CORNER)),
            contentAlignment = Alignment.Center
        ) {
            /*
             * ⚠️ **La tinta si dà col `CompositionLocal` e non a mano**: qui non c'è la
             * `Surface` che nel tastino vero porta `contentColor`, e la fessura disegna un
             * `Icon` che quel colore lo legge da lì. Passarlo come parametro vorrebbe dire
             * chiedere a chi riempie la fessura di saperlo, e i due disegni divergerebbero.
             */
            CompositionLocalProvider(LocalContentColor provides ink) { glyph(null) }
        }
        Popup(alignment = Alignment.TopStart) {
            untouchable()
            tasto(0.dp)
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
 * schermata, che sta nella finestra dell'app: un tastino in una finestra **più alta** se lo
 * prenderebbe per primo e il menu resterebbe aperto, con l'aggravante che il suo `onTap` lo
 * riaprirebbe subito dopo. È il lampeggio che la `1.06` aveva chiuso.
 * ⚠️ **Quindi il tastino staccato è solo da guardare**, ed è giusto così: mentre il suo menu
 * è aperto l'unica cosa che il suo tocco deve fare è chiudere quel menu, e a chiuderlo pensa
 * chi lo faceva già.
 * ⚠️ **Si passa dai `LayoutParams` della radice**, come il velo dei popup: un `Popup` non ha
 * un `Window` suo, e la sua finestra sono i parametri della vista che Compose ha aggiunto al
 * gestore. La nota per esteso sta in `Veil.kt`.
 * ⚠️ **Niente da rimettere a posto all'uscita**: la finestra muore col popup, e questa esiste
 * solo finché il tastino sta per conto suo.
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

package io.github.roccobot.aiv

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * Un elenco che si riordina **trascinando**, con la manopola a destra di ogni riga.
 *
 * ⚠️⚠️ **PRENDE IL POSTO DELLE DUE FRECCE, ed è una richiesta dell'utente** (*i tastini su e giù
 * sono scomodi*). Le frecce non spariscono del tutto: restano come **azioni di accessibilità**
 * sulla riga, perché un trascinamento con un lettore di schermo non si può fare, e togliere le
 * frecce senza rimetterle là vorrebbe dire togliere la funzione a chi non vede.
 *
 * ⚠️⚠️ **LE RIGHE SONO TUTTE ALTE UGUALE, e non è una comodità grafica: è quello che rende il
 * conto ESATTO.** Sapendo l'altezza, la riga di arrivo è la partenza più lo spostamento diviso
 * quell'altezza, arrotondato. Con righe di altezze diverse servirebbe misurare ognuna e
 * ricalcolare a ogni pixel, cioè molto codice per elenchi che qui sono di quattro o dieci voci.
 * Chi mettesse qui dentro una riga di due righe di testo romperebbe il conto, e per questo
 * l'altezza è imposta da qui e non lasciata al contenuto.
 *
 * ⚠️ **Niente `LazyColumn`**: le righe restano tutte composte, perché una lista pigra
 * riciclerebbe proprio quelle che si stanno spostando.
 *
 * ⚠️⚠️ **IL DITO SUL BORDO FA SCORRERE LA PAGINA, DALLA `1.81`, E FINO ALLA `1.80` QUESTA KDOC
 * ASSUMEVA CHE NON SERVISSE** (censimento della UI del 2026-09-05). Diceva che *questi elenchi
 * si vedono tutti insieme*, e per l'unico chiamante che il componente ha era falso: tredici
 * righe da [ROW] fanno più di settecento dp di solo elenco, su una finestra che di posto ne
 * lascia intorno ai seicento. Quindi l'ultima riga non si poteva raggiungere trascinando: il
 * gesto **consuma** gli eventi, quindi finché il dito è giù il guscio che scorre non si muove
 * da sé.
 * ⚠️ **Lo schema è quello della selezione da/a della griglia**, che lo stesso problema lo aveva
 * già risolto: la spinta si aggiorna a ogni **fotogramma** e non a ogni evento del dito (con la
 * pagina che scorre sotto un dito fermo non arriva nessun evento), e cresce avvicinandosi al
 * bordo invece di essere un interruttore.
 * ⚠️⚠️ **E LO SCARTO CRESCE DI QUANTO LA PAGINA È SCORSA**: il dito resta fermo **sullo
 * schermo**, quindi sotto di lui passa contenuto nuovo, e senza quella somma la riga presa
 * scapperebbe via da sotto il dito nel verso opposto.
 * ⚠️ **Questo pezzo il banco di prova non lo vede**, e va detto: vuole un gesto continuo e un
 * viewport vero, cioè le due cose che su una macchina senza telefono non ci sono.
 *
 * @param fixed quante righe in testa **non** si spostano e non si possono scavalcare.
 * @param scroll il guscio che scorre, quando il chiamante ne ha uno. ⚠️ Senza, il
 *   trascinamento resta quello di prima e non scorre niente: è un valore di serie che **spegne**
 *   una funzione, non uno che la finge.
 */
@Composable
fun <T> Reorderable(
    items: List<T>,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    fixed: Int = 0,
    scroll: ScrollState? = null,
    row: @Composable (item: T, index: Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val altaPx = with(LocalDensity.current) { ROW.toPx() }
    val edgePx = with(LocalDensity.current) { EDGE_BAND.toPx() }
    val speedPx = with(LocalDensity.current) { EDGE_SPEED.toPx() }
    // ⚠️ L'altezza della FINESTRA e non quella del riquadro: la banda che fa scorrere è quella
    // vicina al bordo dello schermo, cioè dove il dito non ha più strada.
    val alta = LocalWindowInfo.current.containerSize.height.toFloat()
    // Dove comincia questo riquadro dentro la finestra: serve a sapere dov'è il dito.
    var testa by remember { mutableFloatStateOf(0f) }
    /*
     * ⚠️ **Le due etichette si leggono QUI e non dentro `semantics`**: quel blocco non è un
     * ambito composabile, quindi una risorsa letta là dentro non si compila. Sono le stringhe
     * delle frecce di ieri, non due nuove: la funzione è la stessa.
     */
    val su = stringResource(R.string.settings_facts_up)
    val giu = stringResource(R.string.settings_facts_down)
    // ⚠️ Da dove si è partiti, e `-1` quando nessuno si sta muovendo: serve a distinguere la
    // riga sollevata da tutte le altre, che intanto si scansano.
    var da by remember { mutableIntStateOf(-1) }
    var scarto by remember { mutableFloatStateOf(0f) }
    val a = if (da < 0) -1 else
        (da + (scarto / altaPx).roundToInt()).coerceIn(fixed, items.lastIndex)

    LaunchedEffect(da >= 0, scroll) {
        if (scroll == null) return@LaunchedEffect
        while (da >= 0) {
            withFrameNanos { }
            if (da < 0) break
            val dito = testa + da * altaPx + scarto + altaPx / 2f
            val spinta = when {
                alta <= 0f -> 0f
                dito < edgePx -> -(edgePx - dito) / edgePx
                dito > alta - edgePx -> (dito - (alta - edgePx)) / edgePx
                else -> 0f
            }
            if (spinta != 0f) scarto += scroll.scrollBy(spinta.coerceIn(-1f, 1f) * speedPx)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { testa = it.positionInWindow().y }
    ) {
        items.forEachIndexed { at, item ->
            val preso = at == da
            /*
             * ⚠️ **Le righe fra la partenza e l'arrivo si scansano di UNA posizione**, così
             * quello che si vede mentre il dito è giù è già il risultato: senza, l'elenco
             * resterebbe fermo e il posto di arrivo sarebbe da indovinare.
             */
            val scansa = when {
                da < 0 || preso -> 0f
                da < a && at in (da + 1)..a -> -altaPx
                da > a && at in a until da -> altaPx
                else -> 0f
            }
            val fermo = at < fixed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ROW)
                    // ⚠️ La riga presa sta SOPRA le altre mentre si muove, o passerebbe sotto
                    // la vicina proprio nel momento in cui la scavalca.
                    .zIndex(if (preso) 1f else 0f)
                    .graphicsLayer { translationY = if (preso) scarto else scansa }
                    .semantics {
                        /*
                         * ⚠️ Le due frecce di prima, come azioni: sono l'unica via al riordino
                         * per chi usa un lettore di schermo.
                         * ⚠️⚠️ **OGNUNA DELLE DUE VA OFFERTA SOLO SE HA DOVE ANDARE, e fino
                         * alla `1.80` la prima riga mobile offriva 'sposta su'** (censimento
                         * della UI del 2026-09-05). Il trascinamento le righe fisse le
                         * rispetta da sempre (`coerceIn(fixed, ...)`), l'azione parlata no:
                         * `moved` accettava lo spostamento, quindi l'oggetto in memoria
                         * portava davvero il nome del file in seconda posizione, e a
                         * rimetterlo in testa era **solo** il giro dall'archivio. Cioè un
                         * comando di accessibilità faceva una cosa che le dita non possono
                         * fare, e la disfaceva un salvataggio.
                         * ⚠️ **Il gemello di sotto invece è innocuo e si chiude per
                         * simmetria**: sull'ultima riga `moved(lastIndex, lastIndex + 1)`
                         * torna la lista intatta, quindi l'azione c'era e non faceva niente.
                         * Annunciare un comando che non fa niente è un difetto più piccolo,
                         * ma dello stesso genere.
                         */
                        val salire = at > fixed
                        val scendere = at < items.lastIndex
                        if (!fermo) {
                            customActions = buildList {
                                if (salire) {
                                    add(CustomAccessibilityAction(su) { onMove(at, at - 1); true })
                                }
                                if (scendere) {
                                    add(CustomAccessibilityAction(giu) { onMove(at, at + 1); true })
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                row(item, at)
                if (fermo) return@Box
                Icon(
                    /*
                     * ⚠️⚠️ **SEI PUNTI E NON TRE RIGHE, dalla `1.57`** (riscontro dell'utente,
                     * giro della `1.56`: *usa una maniglia come questa*, con in allegato la
                     * griglia di sei punti). Le tre righe sono il segno della bottomsheet che
                     * si tira giù; i sei punti sono il segno di una cosa che si sposta, ed è
                     * quello che questa riga fa.
                     */
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(HANDLE)
                        // ⚠️ Mentre una riga è in viaggio le altre manopole si spengono a
                        // vista: due dita su due manopole darebbero due partenze e un conto
                        // solo.
                        .alpha(if (da < 0 || preso) 1f else GHOST)
                        .pointerInput(at, items.size) {
                            detectDragGestures(
                                onDragStart = {
                                    da = at
                                    scarto = 0f
                                    haptics.performHapticFeedback(HOLD_BUZZ)
                                },
                                onDrag = { evento, delta ->
                                    evento.consume()
                                    scarto += delta.y
                                },
                                onDragEnd = {
                                    /*
                                     * ⚠️⚠️ **L'ARRIVO SI RICALCOLA QUI, e non si legge quello
                                     * disegnato**: questo blocco vive dentro `pointerInput`,
                                     * cioè viene ricordato, e un valore preso dalla
                                     * composizione in cui è nato resterebbe quello del primo
                                     * fotogramma. Gli stati invece si leggono vivi, perché il
                                     * delegato legge al momento della chiamata.
                                     */
                                    val arrivo = (da + (scarto / altaPx).roundToInt())
                                        .coerceIn(fixed, items.lastIndex)
                                    if (arrivo != da) onMove(da, arrivo)
                                    da = -1
                                    scarto = 0f
                                },
                                onDragCancel = {
                                    da = -1
                                    scarto = 0f
                                }
                            )
                        }
                )
            }
        }
    }
}

/**
 * L'altezza di ogni riga.
 *
 * ⚠️ **56dp è la riga di elenco di Material**, e qui è anche il passo con cui si conta lo
 * spostamento: cambiarla è lecito, cambiarla per una riga sola no.
 */
private val ROW = 56.dp

/**
 * La manopola: un bersaglio comodo, non un glifo da 24.
 *
 * ⚠️ **Non è privata perché il rientro della riga si RICAVA da lei** (`SettingsScreen`, la
 * pagina dei campi delle info): quel rientro esiste solo per lasciarle posto, e fino alla
 * `1.80` erano due numeri in due file che non si nominavano (44 qui e 40 là). Chi allargasse
 * la manopola non aveva nessun modo di trovare il numero da correggere.
 */
internal val HANDLE: Dp = 40.dp

/** Quanto si spengono le manopole delle righe ferme mentre una viaggia. */
private const val GHOST = 0.3f

/**
 * La stessa lista con un elemento spostato.
 *
 * ⚠️ Si toglie e si rimette invece di scambiare i due: lo scambio funziona solo fra vicini,
 * e un trascinamento vero attraversa più di una posizione per volta.
 * ⚠️ **Sta qui e non in una delle due schermate** perché la usano tutti e due i riordini,
 * l'elenco dei campi e la replica a griglia dei tasti: due copie divergerebbero il giorno
 * che una delle due impara a spostare più di un elemento.
 */
internal fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    val out = toMutableList()
    out.add(to, out.removeAt(from))
    return out
}

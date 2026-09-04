package io.github.roccobot.aiv

import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
 * ⚠️ **Niente `LazyColumn`**: questi elenchi si vedono tutti insieme e non scorrono da soli, e
 * una lista pigra riciclerebbe proprio le righe che si stanno spostando.
 *
 * @param fixed quante righe in testa **non** si spostano e non si possono scavalcare.
 */
@Composable
fun <T> Reorderable(
    items: List<T>,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    fixed: Int = 0,
    row: @Composable (item: T, index: Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val altaPx = with(LocalDensity.current) { ROW.toPx() }
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

    Column(modifier = modifier.fillMaxWidth()) {
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
                        // ⚠️ Le due frecce di prima, come azioni: sono l'unica via al riordino
                        // per chi usa un lettore di schermo.
                        if (!fermo) {
                            customActions = listOf(
                                CustomAccessibilityAction(su) { onMove(at, at - 1); true },
                                CustomAccessibilityAction(giu) { onMove(at, at + 1); true }
                            )
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

/** La manopola: un bersaglio comodo, non un glifo da 24. */
private val HANDLE: Dp = 40.dp

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

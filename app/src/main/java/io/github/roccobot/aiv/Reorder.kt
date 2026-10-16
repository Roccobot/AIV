package io.github.roccobot.aiv

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
 * Le frecce su e giù restano come azioni di accessibilità sulla riga: col lettore di schermo un
 * trascinamento non si può fare.
 *
 * ⚠️ **Le righe sono tutte alte uguale, ed è quello che rende il conto esatto**: la riga di
 * arrivo è la partenza più lo spostamento diviso l'altezza, arrotondato. Con altezze diverse
 * servirebbe misurare ogni riga e ricalcolare a ogni pixel; per questo l'altezza la impone
 * questo componente e non il contenuto.
 *
 * Niente `LazyColumn`: una lista pigra riciclerebbe proprio le righe che si stanno spostando.
 *
 * ⚠️ **Il dito sul bordo fa scorrere la pagina**: l'elenco può essere più alto della finestra, e
 * il gesto consuma gli eventi, quindi finché il dito è giù il guscio che scorre non si muove da
 * sé. Lo schema è quello della selezione da/a della griglia: la spinta si aggiorna a ogni
 * fotogramma (con la pagina che scorre sotto un dito fermo non arriva nessun evento) e cresce
 * avvicinandosi al bordo.
 * Lo scarto cresce di quanto la pagina è scorsa: il dito resta fermo sullo schermo, e senza
 * quella somma la riga presa scapperebbe da sotto il dito nel verso opposto.
 * Lo scorrimento il banco non lo vede: vuole un gesto continuo e un viewport vero.
 *
 * @param fixed quante righe in testa non si spostano e non si possono scavalcare.
 * @param scroll il guscio che scorre, quando il chiamante ne ha uno; senza, il trascinamento non
 *   fa scorrere niente.
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
    // L'altezza della finestra, non del riquadro: in basso la banda che fa scorrere è quella
    // vicina al bordo dello schermo, dove il dito non ha più strada.
    val alta = LocalWindowInfo.current.containerSize.height.toFloat()
    // Dove comincia questo riquadro dentro la finestra: serve a sapere dov'è il dito.
    var testa by remember { mutableFloatStateOf(0f) }
    // Le due etichette si leggono qui: `semantics` non è un ambito composabile, e una risorsa
    // letta là dentro non si compila.
    val su = stringResource(R.string.settings_facts_up)
    val giu = stringResource(R.string.settings_facts_down)
    // Da dove si è partiti, `-1` quando nessuna riga si muove: distingue la riga sollevata da
    // tutte le altre, che intanto si scansano.
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
            /*
             * ⚠️ **In alto la soglia è la cima dell'elenco, non il bordo dello schermo** (voce
             * `riordino-scorre`): sopra l'elenco ci sono il titolo della pagina e la sua
             * spiegazione, dove la riga presa non può entrare, e misurando dal bordo all'insù non
             * scorreva quasi niente. In basso l'elenco arriva fino in fondo alla pagina, quindi
             * il bordo della finestra è già la fine della strada.
             */
            val cima = testa.coerceAtLeast(0f)
            val spinta = when {
                alta <= 0f -> 0f
                dito < cima + edgePx -> -(cima + edgePx - dito) / edgePx
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
            // Le righe fra la partenza e l'arrivo si scansano di una posizione, così mentre il
            // dito è giù si vede già il risultato.
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
                    // La riga presa si disegna sopra le altre mentre si muove, o passerebbe
                    // sotto la vicina proprio nel momento in cui la scavalca.
                    .zIndex(if (preso) 1f else 0f)
                    .graphicsLayer { translationY = if (preso) scarto else scansa }
                    .semantics {
                        /*
                         * Le frecce come azioni: sono l'unica via al riordino per chi usa un
                         * lettore di schermo.
                         * ⚠️ **Ognuna si offre solo se ha dove andare**: 'sposta su' sulla prima
                         * riga mobile scavalcherebbe le righe fisse, che il trascinamento
                         * rispetta (`coerceIn(fixed, ...)`) e `moved` no. 'Sposta giù'
                         * sull'ultima riga non farebbe niente, e un comando annunciato che non
                         * fa niente è un difetto dello stesso genere.
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
                    // Sei punti (scelta dell'utente, 1.57): le tre righe sono il segno della
                    // scheda che si tira giù, i sei punti quello di una cosa che si sposta.
                    imageVector = Glyphs.Material.DragIndicator,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(HANDLE_TOUCH)
                        /*
                         * Mentre una riga è in viaggio le altre manopole si spengono: due dita su
                         * due manopole darebbero due partenze e un conto solo. A riposo la
                         * manopola è al 60% ([HANDLE_INK]), così pesa meno dei nomi accanto.
                         */
                        .alpha(if (da < 0 || preso) HANDLE_INK else GHOST)
                        /*
                         * ⚠️ **Il gesto è prima del rientro**, quindi ascolta su tutto il bersaglio
                         * da [HANDLE_TOUCH]; dopo, ascolterebbe sul solo disegno da 24.
                         */
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
                                     * ⚠️ **L'arrivo si ricalcola qui**: il blocco vive in
                                     * `pointerInput`, che lo ricorda, e il valore `a` preso
                                     * dalla composizione resterebbe quello del primo
                                     * fotogramma. Gli stati invece il delegato li legge al
                                     * momento della chiamata.
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
                        /*
                         * Il rientro rimpicciolisce il disegno e lascia intero il bersaglio (voce
                         * `riordino-scorre`: maniglie più piccole e al 60%). È più largo a sinistra
                         * perché il disegno resti dov'era quando il bersaglio era [HANDLE]: al
                         * centro della fascia che la riga gli lascia.
                         */
                        .padding(
                            start = HANDLE_TOUCH - (HANDLE + HANDLE_GLYPH) / 2,
                            end = (HANDLE - HANDLE_GLYPH) / 2,
                            top = (HANDLE_TOUCH - HANDLE_GLYPH) / 2,
                            bottom = (HANDLE_TOUCH - HANDLE_GLYPH) / 2
                        )
                )
            }
        }
    }
}

/**
 * L'altezza di ogni riga.
 *
 * 56dp è la riga di elenco di Material, ed è anche il passo con cui si conta lo spostamento:
 * vale per tutte le righe, mai per una sola.
 */
private val ROW = 56.dp

/**
 * La fascia che la riga lascia alla manopola, in fondo a destra: il disegno è al suo centro.
 *
 * Non è privata perché il rientro della riga in `SettingsScreen` (la pagina dei campi delle
 * info) e in `StyleSettings` si ricava da lei: esiste solo per lasciarle posto, e così il numero
 * è uno solo.
 */
internal val HANDLE: Dp = 40.dp

/**
 * Il bersaglio della manopola: 48 dp, il minimo di Material e di Android (scelta F1 sulla voce G5
 * della revisione Setaccio).
 *
 * Più grande di [HANDLE] e senza spostare il disegno: gli 8 dp in più sono tutti a sinistra, 4 sul
 * filo d'aria che le righe lasciano fra il testo e la manopola (`HANDLE + 4.dp`) e 4 sul bordo
 * della riga accanto, che lì perde il tocco.
 */
private val HANDLE_TOUCH: Dp = 48.dp

/**
 * Quanto è grande il **disegno** dentro il bersaglio.
 *
 * 24dp, come ogni altra icona dell'app: quello che deve restare grande è il bersaglio, [HANDLE].
 */
private val HANDLE_GLYPH: Dp = 24.dp

/** Quanto si spengono le manopole delle righe ferme mentre una viaggia. */
private const val GHOST = 0.3f

/** Quanto si vede una manopola a riposo: il 60% chiesto dall'utente (voce `riordino-scorre`). */
private const val HANDLE_INK = 0.6f

/**
 * La stessa lista con un elemento spostato.
 *
 * L'elemento si toglie e si rimette: uno scambio funziona solo fra vicini, e un trascinamento
 * attraversa più di una posizione per volta.
 * È qui perché la usano tutti e due i riordini, l'elenco dei campi e la replica a griglia dei
 * tasti, e due copie potrebbero divergere.
 */
internal fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    val out = toMutableList()
    out.add(to, out.removeAt(from))
    return out
}

package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Il velo del mini onboarding: oscura la schermata, dice la frase e mette in evidenza una
 * copia **funzionante** del tastino che sta insegnando.
 *
 * ⚠️⚠️ **NASCE COSÌ NELLA 0.67 E DIVENTA CONDIVISO NELLA 0.78** (richiesta dell'utente:
 * *un mini onboarding grafico, che oscura la schermata ed evidenzia in arancione il FAB*).
 * Serve perché il tocco lungo è una scorciatoia che **non si scopre da sola**: un tastino non
 * dichiara i propri gesti. I veli sono diventati **tre** (selezione, cestino, colonne) e
 * vivono in due schermate diverse: il colore, il contrasto misurato e la geometria stanno qui
 * una volta sola, e quello che cambia sono la frase e il tastino.
 *
 * ⚠️⚠️ **LA COPIA EVIDENZIATA FUNZIONA, non è un disegno**, ed è la differenza fra insegnare
 * e raccontare: chi tiene premuto sul velo fa la cosa mentre gliela si spiega, invece di
 * doverla richiudere e rifare. È anche il motivo per cui è lo **stesso** [TapHoldFab] del
 * tastino vero, alla stessa misura e nello stesso angolo: cade **sopra** l'originale.
 *
 * ⚠️⚠️ **IL VELO COPRE TUTTO LO SCHERMO dalla `0.73`**, testata e margini di sistema
 * compresi, ed è una correzione: fino alla `0.72` copriva la sola griglia, perché nasceva
 * dentro la `Column` che i margini li ha già applicati. Il rimedio non è stato spostare i
 * margini ma **avvolgere la schermata in un `Box`** e far nascere il velo là. Per questo è
 * un'estensione di [BoxScope]: senza un `Box` intorno, `matchParentSize` non esiste e il velo
 * tornerebbe a coprire solo il suo pezzo.
 *
 * ⚠️ **Chi tocca il velo per chiuderlo senza leggerlo la scorciatoia non la scopre**, e la
 * rete di sicurezza è l'etichetta che il lettore di schermo legge sul tocco lungo (vedi
 * `holdLabel` di [TapHoldFab]). È il costo della scelta, ed è dichiarato.
 */
@Composable
fun BoxScope.HintVeil(
    text: String,
    /**
     * I rientri che portano la copia del tastino **esattamente** sopra l'originale.
     *
     * ⚠️⚠️ **NON SONO DECORAZIONE, e sono l'unica cosa che il velo non può ricavare da sé**:
     * il tastino vero vive dentro il rientro di sistema più i margini della sua schermata, e
     * il velo nasce fuori da tutti perché è il suo mestiere. Chi ne dimentica uno vede la
     * copia scivolare in un angolo.
     * ⚠️ Arriva come `Modifier` e non come misura perché le catene sono diverse: nella griglia
     * sono tre (sistema, margine della schermata, margine del tastino), nelle cartelle due.
     */
    inset: Modifier,
    onDone: () -> Unit,
    fab: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(HINT_SCRIM)
            // ⚠️ Niente increspatura e nessuna descrizione: questo non è un tasto, è il velo,
            // e un tocco qualunque lo archivia. Un onboarding che si deve leggere due volte
            // non è un onboarding.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDone
            )
    ) {
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).then(inset),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(HINT_GAP)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(max = HINT_WIDTH)
            )
            fab()
        }
    }
}

/**
 * Il velo che dice una cosa e basta: frase **in mezzo allo schermo**, nessun tastino da
 * evidenziare.
 *
 * ⚠️⚠️ **NASCE NELLA 1.25 PERCHÉ IL GESTO NON HA UN POSTO** (richiesta dell'utente,
 * 2026-09-02: *nuovo mini-onboarding, con testo centrato in mezzo allo schermo, alla
 * visualizzazione della prima immagine dopo l'installazione*). Gli altri tre veli indicano un
 * **tastino** e ne mettono in scena una copia funzionante; il doppio tocco si fa sulla
 * fotografia intera, quindi non c'è niente da indicare, e una copia evidenziata coprirebbe
 * proprio la cosa di cui si sta parlando.
 * ⚠️ **Sono la stessa macchina di [HintVeil]**, e condividono il velo e la sua misura di
 * contrasto: cambiano dove sta il testo e il fatto che qui non c'è un tastino. Chi li fondesse
 * in una funzione sola con due parametri opzionali otterrebbe una firma che nessuno dei due
 * usa per intero.
 * ⚠️ **Un tocco qualunque lo archivia**, come gli altri: un onboarding che si deve leggere due
 * volte non è un onboarding.
 */
@Composable
fun BoxScope.HintCentre(text: String, onDone: () -> Unit) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(HINT_SCRIM)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDone
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = HINT_SIDE).widthIn(max = HINT_WIDTH)
        )
    }
}

/**
 * Lo stesso velo centrato, ma **sopra un dialogo**: una finestra sua, che copre lo schermo.
 *
 * ⚠️⚠️ **ESISTE PERCHÉ UN VELO DENTRO UN DIALOGO COPRE IL DIALOGO E NON LO SCHERMO** (nasce
 * nella `1.36` per l'avviso sul cambio di estensione, che parte da dentro la finestra di
 * rinomina). [HintCentre] è un'estensione di `BoxScope` e si stende sul `Box` che lo contiene:
 * là dentro sarebbe un velo largo come la finestrella, cioè un riquadro scuro in mezzo a un
 * dialogo, mentre l'utente ha chiesto un avviso **in mezzo allo schermo**. Una finestra
 * propria è l'unico modo di stare sopra un'altra finestra.
 * ⚠️ **`usePlatformDefaultWidth = false` è la riga che conta**: senza, il dialogo prende la
 * larghezza di un dialogo Material (il 90% meno i margini) e il velo si vedrebbe come una
 * scheda scura invece che come un velo.
 * ⚠️ **Il velo, il corpo e il margine sono gli STESSI di [HintCentre]**, e non una copia con
 * altri numeri: cambia soltanto la finestra in cui vivono. Il giorno che il contrasto del velo
 * si ritocca, si ritocca una volta.
 * ⚠️ **Un tocco qualunque lo archivia**, come tutti gli altri.
 */
@Composable
fun HintNotice(text: String, onDone: () -> Unit) {
    Dialog(
        onDismissRequest = onDone,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HINT_SCRIM)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDone
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = HINT_SIDE).widthIn(max = HINT_WIDTH)
            )
        }
    }
}

/**
 * Il velo del mini onboarding.
 *
 * ⚠️ **Il 70% di nero e non il 50%**: sotto c'è una griglia di fotografie, cioè il fondo più
 * chiassoso che ci sia, e a metà velo le miniature continuano a chiamare l'occhio. Col 70% il
 * bianco del testo misura 8.45 anche sulla fotografia più chiara possibile.
 */
private val HINT_SCRIM = Color(0xB3000000)

/**
 * Quanto la frase centrata sta lontana dai bordi.
 *
 * ⚠️ Serve solo a [HintCentre]: là il testo è in mezzo allo schermo e senza margine, su un
 * telefono stretto, una frase lunga toccherebbe i due bordi. Il velo con il tastino non ne ha
 * bisogno perché il suo margine glielo dà il rientro del tastino.
 */
private val HINT_SIDE = 32.dp

/**
 * L'arancione della copia evidenziata, e **l'unico posto in cui la tavolozza si rompe
 * apposta** (richiesta dell'utente).
 *
 * ⚠️ L'accento dell'app è verde acqua: un velo che evidenzia col colore di casa non evidenzia
 * niente, perché quel colore è già dappertutto. Misurato: 4.35 sul velo steso sulla fotografia
 * più chiara possibile, cioè sopra il 3:1 delle grafiche non testuali nel caso peggiore, e
 * 10.81 nel caso normale.
 */
val HINT_MARK = Color(0xFFFFA726)

/** Il glifo sopra l'arancione: misurato 7.29, cioè leggibile senza discussioni. */
val HINT_INK = Color(0xFF3E2600)

/** Quanto sta lontano il testo dal tastino che indica: abbastanza da non sembrarne parte. */
private val HINT_GAP = 14.dp

/**
 * Quanto è larga al massimo la frase del velo.
 *
 * ⚠️ Un limite serve perché la frase è lunga e le lingue non sono l'italiano: senza, in tedesco
 * diventerebbe una riga sola da bordo a bordo, e in un telefono stretto si spezzerebbe dove
 * capita invece che dove si legge.
 */
private val HINT_WIDTH = 260.dp

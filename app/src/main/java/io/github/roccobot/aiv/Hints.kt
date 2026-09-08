package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Il velo del mini onboarding: oscura la schermata, dice la frase e mette in evidenza una
 * copia **funzionante** del FAB che sta insegnando.
 *
 * ⚠️⚠️ **NASCE COSÌ NELLA 0.67 E DIVENTA CONDIVISO NELLA 0.78** (richiesta dell'utente:
 * *un mini onboarding grafico, che oscura la schermata ed evidenzia in arancione il FAB*).
 * Serve perché il tocco lungo è una scorciatoia che **non si scopre da sola**: un FAB non
 * dichiara i propri gesti. I veli di questa forma vivono in due schermate diverse (il cestino
 * nella griglia, le colonne nella schermata iniziale): il colore, il contrasto misurato e la
 * geometria stanno qui una volta sola, e quello che cambia sono la frase e il FAB.
 * ⚠️ **Fino alla `1.78` la nota ne contava tre e nominava per primo quello della selezione**,
 * uscito nella `0.94` insieme alla sua chiave: quanti siano non si scrive, perché il conto
 * invecchia da sé (`Roccobot.md`, § '🪶 Come si mantiene un file di regole').
 *
 * ⚠️⚠️ **LA COPIA EVIDENZIATA FUNZIONA, non è un disegno**, ed è la differenza fra insegnare
 * e raccontare: chi tiene premuto sul velo fa la cosa mentre gliela si spiega, invece di
 * doverla richiudere e rifare. È anche il motivo per cui è lo **stesso** [TapHoldFab] del
 * FAB vero, alla stessa misura e nello stesso angolo: cade **sopra** l'originale.
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
     * I rientri che portano la copia del FAB **esattamente** sopra l'originale.
     *
     * ⚠️⚠️ **NON SONO DECORAZIONE, e sono l'unica cosa che il velo non può ricavare da sé**:
     * il FAB vero vive dentro il rientro di sistema più i margini della sua schermata, e
     * il velo nasce fuori da tutti perché è il suo mestiere. Chi ne dimentica uno vede la
     * copia scivolare in un angolo.
     * ⚠️ Arriva come `Modifier` e non come misura perché le catene sono diverse: nella griglia
     * sono tre (sistema, margine della schermata, margine del FAB), nelle cartelle due.
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
        /*
         * ⚠️ **Anche l'onboarding segue il lato del FAB**, dalla `1.57`: quello che spiega
         * è il FAB, e una freccia che punta dalla parte sbagliata spiegherebbe il vuoto.
         */
        val destra = LocalPadLook.current.hand == Hand.RIGHT
        Column(
            modifier = Modifier
                .align(if (destra) Alignment.BottomEnd else Alignment.BottomStart)
                .then(inset),
            horizontalAlignment = if (destra) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(HINT_GAP)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                /*
                 * ⚠️⚠️ **ANCHE LA FRASE SEGUE LA MANO, DALLA `1.81`, e fino alla `1.80` era
                 * ferma a destra** (censimento della UI del 2026-09-05). La colonna si
                 * specchiava (l'allineamento nel riquadro e quello dei figli), il paragrafo no,
                 * e la differenza si vede: la frase va a capo davvero, perché a settanta
                 * caratteri di `titleMedium` la sua larghezza intrinseca supera [HINT_WIDTH],
                 * quindi la scatola misura quei 260dp pieni e le righe si appoggiavano al
                 * fianco **destro** mentre la colonna viveva sul fianco sinistro.
                 */
                textAlign = if (destra) TextAlign.End else TextAlign.Start,
                modifier = Modifier.widthIn(max = HINT_WIDTH)
            )
            fab()
        }
    }
}

/**
 * Il velo che evidenzia **un pezzo qualunque della schermata**, dov'è, e scrive la frase sotto.
 *
 * ⚠️⚠️ **NASCE NELLA `1.95` PER L'ICONA DELL'INTESTAZIONE**, cioè per il gesto che sceglie la
 * copertina di una cartella. Gli altri due veli non bastavano: [HintVeil] mette una copia del FAB
 * nell'angolo in fondo, e [HintCentre] non indica niente. Qui la cosa da indicare sta **in cima**,
 * al centro, e la sua posizione dipende da quanto la fascia è aperta.
 *
 * ⚠️⚠️ **IL POSTO NON SI RICALCOLA: SI MISURA**, e questa è la differenza che rende il velo
 * esatto per costruzione. Rifare qui la catena dei rientri (barra di sistema, testata, fascia, e
 * la misura che l'icona cede scorrendo) vorrebbe dire una seconda geometria da tenere allineata
 * alla prima, e basterebbe un ritocco all'intestazione per far cadere l'evidenziazione sul vuoto.
 * Chi chiama passa il riquadro che l'icona vera occupa, letto con `onGloballyPositioned`.
 * ⚠️ **L'origine si sottrae**, perché il riquadro arriva in coordinate della radice e questo velo
 * vive dentro il `Box` della schermata: senza, su una schermata che non comincia a zero la copia
 * scivolerebbe di tutto il rientro.
 *
 * ⚠️ **La copia è un disegno e non un tasto**, al contrario di quella di [HintVeil]: là il velo
 * insegna un gesto che si può fare **mentre** lo si legge, qui il gesto è già stato fatto (il velo
 * compare perché l'icona è stata toccata), quindi una copia che risponde rifarebbe l'azione.
 * ⚠️ **Un tocco qualunque lo archivia**, come tutti gli altri.
 */
@Composable
fun BoxScope.HintSpot(
    text: String,
    /** Dove sta la cosa da evidenziare, in coordinate della radice. */
    spot: Rect,
    glyph: ImageVector,
    onDone: () -> Unit
) {
    var origine by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .matchParentSize()
            .onGloballyPositioned { origine = it.positionInRoot() }
            .background(HINT_SCRIM)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDone
            )
    ) {
        with(LocalDensity.current) {
            Icon(
                imageVector = glyph,
                contentDescription = null,
                tint = HINT_MARK,
                modifier = Modifier
                    .offset(
                        x = (spot.left - origine.x).toDp(),
                        y = (spot.top - origine.y).toDp()
                    )
                    .size(width = spot.width.toDp(), height = spot.height.toDp())
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (spot.bottom - origine.y).toDp() + HINT_GAP)
                    .padding(horizontal = HINT_SIDE)
                    .widthIn(max = HINT_WIDTH)
            )
        }
    }
}

/**
 * Il velo che dice una cosa e basta: frase **in mezzo allo schermo**, nessun FAB da
 * evidenziare.
 *
 * ⚠️⚠️ **NASCE NELLA 1.25 PERCHÉ IL GESTO NON HA UN POSTO** (richiesta dell'utente,
 * 2026-09-02: *nuovo mini-onboarding, con testo centrato in mezzo allo schermo, alla
 * visualizzazione della prima immagine dopo l'installazione*). Gli altri tre veli indicano un
 * **FAB** e ne mettono in scena una copia funzionante; il doppio tocco si fa sulla
 * fotografia intera, quindi non c'è niente da indicare, e una copia evidenziata coprirebbe
 * proprio la cosa di cui si sta parlando.
 * ⚠️ **Sono la stessa macchina di [HintVeil]**, e condividono il velo e la sua misura di
 * contrasto: cambiano dov'è il testo e il fatto che qui non c'è un FAB. Chi li fondesse
 * in una funzione sola con due parametri opzionali otterrebbe una firma che nessuno dei due
 * usa per intero.
 * ⚠️ **Un tocco qualunque lo archivia**, come gli altri: un onboarding che si deve leggere due
 * volte non è un onboarding.
 */
@Composable
fun BoxScope.HintCentre(text: String, onDone: () -> Unit) {
    // ⚠️ `matchParentSize` e non `fillMaxSize`: dentro un `Box` questo velo prende la misura
    // del genitore **senza** entrare nel suo conto, e il genitore qui è una schermata intera.
    CentredHint(text = text, onDone = onDone, modifier = Modifier.matchParentSize())
}

/**
 * Il corpo di un velo centrato: il velo, il testo e i suoi margini.
 *
 * ⚠️⚠️ **NASCE PERCHÉ [HintNotice] RICOPIAVA [HintCentre] MENTRE LA NOTA DICHIARAVA IL
 * CONTRARIO** (censimento della UI del 2026-09-05): là era scritto che *il velo, il corpo e il
 * margine sono gli STESSI di [HintCentre], e non una copia con altri numeri*, e le costanti
 * erano davvero condivise; il **corpo** però era ricopiato riga per riga, e di quello la nota
 * diceva il falso. Cambiava una riga sola, la misura del velo, che adesso è il parametro.
 * ⚠️ **La differenza vera fra i due chiamanti è la FINESTRA**, non il disegno: uno si stende sul
 * `Box` che lo contiene, l'altro apre una finestra propria per stare sopra un dialogo.
 */
@Composable
private fun CentredHint(text: String, onDone: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
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
 * ⚠️ **Il velo, il corpo e il margine sono gli STESSI di [HintCentre]**, e dalla `1.81` non è
 * più una raccomandazione: il disegno lo fa `CentredHint` per tutti e due, e questa funzione
 * gli passa soltanto la finestra in cui vive.
 * ⚠️ **Un tocco qualunque lo archivia**, come tutti gli altri.
 */
@Composable
fun HintNotice(text: String, onDone: () -> Unit) {
    Dialog(
        onDismissRequest = onDone,
        /*
         * ⚠️⚠️ **[fullWindow] E NON LA SOLA LARGHEZZA, DALLA `1.81`**: fino alla `1.80` il decoro
         * si adattava alle barre di sistema, quindi il velo si fermava **prima** di loro e
         * lasciava due bande col fondo della schermata sotto. Un velo che copre tutto lo schermo
         * è la richiesta dell'utente per questo onboarding, e con la barra di sistema scoperta
         * non era vera fino in fondo.
         * ⚠️ **Qui non serve nessun `safeDrawingPadding()`**, che è l'eccezione dichiarata su
         * [fullWindow]: il testo è al centro e un velo non ha niente da rientrare.
         */
        properties = fullWindow()
    ) {
        // ⚠️ Qui `fillMaxSize`, perché la finestra è sua e la deve riempire tutta.
        CentredHint(text = text, onDone = onDone, modifier = Modifier.fillMaxSize())
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
 * telefono stretto, una frase lunga toccherebbe i due bordi. Il velo con il FAB non ne ha
 * bisogno perché il suo margine glielo dà il rientro del FAB.
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

/** Quanto sta lontano il testo dal FAB che indica: abbastanza da non sembrarne parte. */
private val HINT_GAP = 14.dp

/**
 * Quanto è larga al massimo la frase del velo.
 *
 * ⚠️ Un limite serve perché la frase è lunga e le lingue non sono l'italiano: senza, in tedesco
 * diventerebbe una riga sola da bordo a bordo, e in un telefono stretto si spezzerebbe dove
 * capita invece che dove si legge.
 */
private val HINT_WIDTH = 260.dp

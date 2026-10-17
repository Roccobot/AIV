package io.github.roccobot.aiv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Il canale con cui l'app dice com'è andata: una riga per volta, in fondo allo schermo (risposta
 * `casa` dell'utente a `d-avvisi`).
 *
 * ⚠️ **Un canale solo**, così lo stesso genere di esito ha una voce sola, e una schermata nuova non
 * ha un secondo modo di parlare.
 *
 * ⚠️ **Una riga per volta, e la più nuova vince: niente coda, apposta.** Una coda mostrerebbe a
 * turno cose di un momento già passato, quando l'utente ha già cambiato schermata. Le operazioni
 * che parlano non si sovrappongono per costruzione (`FileKind.speaks`); l'unico caso di due
 * messaggi insieme è un'eliminazione riuscita a metà, e là il secondo copre il primo.
 *
 * Il testo arriva già risolto, non come `@StringRes`: metà dei chiamanti compone una frase con un
 * plurale o con un nome di file.
 */
object Notices {

    /**
     * Quello che l'app sta dicendo adesso.
     *
     * ⚠️ **L'identificatore serve a distinguere due messaggi uguali di fila**: il conto alla
     * rovescia e l'entrata sono legati a lui, e senza due salvataggi identici darebbero una riga
     * sola che scade quando scadeva la prima.
     */
    data class Line(
        val id: Long,
        val text: String,
        /** L'etichetta del tasto a destra, o `null` per una riga che si limita a dire. */
        val action: String? = null,
        /** Che cosa fa quel tasto. La riga se ne va da sé prima di eseguirlo. */
        val onAction: (() -> Unit)? = null,
        /**
         * Che cosa succede quando la riga se ne va, per scadenza o perché il tasto è stato toccato.
         *
         * ⚠️ **Tiene un'attesa sola**: l'offerta di disfare vive in [Undo] e deve finire con la
         * riga che la offre. Con due attese parallele i due tempi potrebbero divergere, e
         * resterebbe un tasto 'Annulla' che non fa più niente.
         */
        val onGone: (() -> Unit)? = null,
        val millis: Long = NOTICE_MS
    )

    var line by mutableStateOf<Line?>(null)
        private set

    private var conta = 0L

    /** Una riga che dice e basta. Torna il suo identificatore, per chi deve toglierla prima. */
    fun say(text: String, millis: Long = NOTICE_MS): Long =
        posa(Line(id = ++conta, text = text, millis = millis))

    /**
     * Una riga con un tasto: dice che cosa è successo e offre di rimediare. La durata di serie è
     * quella dell'offerta di disfare ([UNDO_MS]).
     */
    fun offer(
        text: String,
        action: String,
        millis: Long = UNDO_MS,
        onGone: (() -> Unit)? = null,
        onAction: () -> Unit
    ): Long = posa(
        Line(
            id = ++conta,
            text = text,
            action = action,
            onAction = onAction,
            onGone = onGone,
            millis = millis
        )
    )

    private fun posa(nuova: Line): Long {
        // Una riga sostituita da un'altra si chiude come se fosse scaduta: chi ne aspettava la fine
        // (l'offerta di disfare) deve saperlo lo stesso.
        line?.onGone?.invoke()
        line = nuova
        return nuova.id
    }

    /**
     * Toglie la riga [id], se è ancora quella in scena.
     *
     * ⚠️ **Per identificatore e non in blocco**: fra la scadenza di una riga e il momento in cui
     * l'attesa se ne accorge può esserne arrivata un'altra, e un congedo cieco toglierebbe quella
     * nuova. Chi lega una riga a una schermata, uscendo, toglie la sua.
     */
    fun dismiss(id: Long) {
        val quale = line ?: return
        if (quale.id != id) return
        line = null
        quale.onGone?.invoke()
    }
}

/**
 * Quanto resta in scena una riga che dice e basta.
 *
 * [NOTICE_MS] è il `Toast.LENGTH_SHORT` di sistema (2 secondi, letto in
 * `NotificationManagerService`). [NOTICE_LONG_MS] vale per gli esiti di un'operazione sui file,
 * frasi con un numero dentro: 3 secondi per scelta dell'utente, come [UNDO_MS], così una frase che
 * offre 'Annulla' e una che dice com'è andata restano in scena lo stesso tempo.
 */
const val NOTICE_MS = 2000L
const val NOTICE_LONG_MS = 3000L

/**
 * La superficie unica con cui l'app parla, in fondo allo schermo.
 *
 * ⚠️ **Uno `Snackbar` di Material**: frase a sinistra e azione a destra sono la sua forma, e con
 * lui arrivano colore, raggio, rientri e stili di testo (`Glyphs.kt` dice di non ridisegnare quello
 * che Material ha già). Senza `SnackbarHost` né `SnackbarHostState`, che servono a una coda e
 * vogliono uno `Scaffold`: qui la coda non c'è (vedi [Notices]) e la durata la decide chi manda la
 * riga.
 *
 * Il fondo non è rovesciato, per scelta dell'utente (`d-avviso`, disegno `tempo`): una notifica di
 * Material chiara sul tema scuro sarebbe l'unica superficie dell'app che inverte i colori. Solo
 * dove c'è un tasto ha una riga che si consuma in fondo per dire quanto tempo resta per toccarlo. Il tasto è in [accentInk], l'accento
 * che si può leggere come parola.
 *
 * Arriva e se ne va come le schede ([arrivaDalBasso], [vaGiu] in `Sheet.kt`). È in una funzione a
 * sé per la stessa ragione di `FabPop`: chiamata sul posto, `AnimatedVisibility` finisce
 * sull'overload di `ColumnScope` e il compilatore la rifiuta.
 *
 * Resta un solo avviso di sistema, quello che spiega perché si apre la pagina delle impostazioni di
 * Android (`ViewerActivity`, `folder_why`): là l'app va in secondo piano nello stesso istante, e
 * una notifica di casa non si vedrebbe.
 */
@Composable
fun AppNotice(line: Notices.Line?, modifier: Modifier = Modifier) {
    /*
     * L'ultima riga vista si tiene: l'animazione d'uscita dura più della riga, e senza il testo
     * l'uscita mostrerebbe una striscia vuota.
     */
    var ultima by remember { mutableStateOf<Notices.Line?>(null) }
    if (line != null) ultima = line
    val quale = ultima

    /*
     * L'attesa è qui, dove è la riga: chi manda un messaggio dice quanto deve durare e non deve
     * ricordarsi di toglierlo. La chiave è l'identificatore, così due messaggi uguali di fila sono
     * due attese.
     */
    LaunchedEffect(line?.id) {
        val viva = line ?: return@LaunchedEffect
        delay(viva.millis)
        Notices.dismiss(viva.id)
    }

    /*
     * La riga si consuma nel tempo del messaggio, quindi arriva a zero quando la notifica se ne va,
     * e riparte da capo a ogni messaggio. Si legge nel disegno (`scaleX` dentro `graphicsLayer`):
     * un ridisegno per fotogramma e nessuna ricomposizione.
     */
    val resta = remember { Animatable(1f) }
    LaunchedEffect(line?.id) {
        val viva = line ?: return@LaunchedEffect
        resta.snapTo(1f)
        resta.animateTo(0f, tween(viva.millis.toInt(), easing = LinearEasing))
    }

    AnimatedVisibility(
        visible = line != null,
        modifier = modifier,
        enter = arrivaDalBasso(),
        exit = vaGiu()
    ) {
        Box(
            // Il rientro di sistema se lo mette da sé, come le schede: vive nel `Box` di radice,
            // che arriva al bordo dello schermo, e finirebbe sotto la barra di navigazione. Al
            // contrario della scheda, il cui fondo passa sotto la barra apposta, una notifica non è
            // appoggiata a niente e si sposta intera.
            modifier = Modifier
                .navigationBarsPadding()
                .padding(NOTICE_EDGE)
        ) {
            Snackbar(
                /*
                 * ⚠️ **La regione viva è la metà di `SnackbarHost` da recuperare a mano**: nel
                 * bytecode `liveRegion` e l'azione di congedo sono in `SnackbarHostKt`, non in
                 * `SnackbarKt`, e senza l'ospite la notifica non sarebbe annunciata. `Polite` e non
                 * `Assertive`: la notifica dice una cosa già successa, e non deve interrompere
                 * quello che il lettore di schermo sta leggendo.
                 */
                modifier = Modifier
                    .semantics { liveRegion = LiveRegionMode.Polite },
                shape = RoundedCornerShape(NOTICE_ROUND),
                /*
                 * Fondo e inchiostro della stessa famiglia, `surfaceVariant` e `onSurfaceVariant`:
                 * il ruolo giusto anche quando le tinte di due famiglie sono vicine.
                 */
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                action = quale?.action?.let { etichetta ->
                    {
                        TextButton(
                            onClick = {
                                // La riga se ne va prima del lavoro: quello che il tasto fa può
                                // aprire una schermata o durare qualche istante, e una notifica
                                // ancora in scena si farebbe toccare due volte.
                                Notices.dismiss(quale.id)
                                quale.onAction?.invoke()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = accentInk())
                        ) { Text(etichetta) }
                    }
                }
            ) {
                Text(quale?.text ?: "")
            }
            /*
             * Il ritaglio ha la forma della notifica, così la riga segue gli angoli stondati.
             * L'origine della scala è il fianco iniziale, verso cui la riga si ritira.
             */
            if (quale?.action != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(NOTICE_ROUND))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(CLESSIDRA)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(0f, 0.5f)
                                scaleX = resta.value
                            }
                            // La tavolozza e non [aivAccent], che serve a chi legge un colore da un
                            // nodo di modificatore, dove il tema non si raggiunge.
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

/**
 * Il respiro fra la notifica e i tre bordi intorno: 12 dp, quello che `SnackbarHost` di Material
 * mette da sé e che senza l'ospite va scritto.
 */
private val NOTICE_EDGE = 12.dp

/**
 * Il raggio della notifica e lo spessore della riga che si consuma.
 *
 * Il raggio è suo: i raggi di casa sono 20 dp per i menu e 28 per schede e finestre, ma su una
 * striscia alta una riga un raggio da 28 farebbe un fianco tutto curva. Tre punti per la riga, la
 * misura del disegno scelto: più spessa diventa una seconda cornice.
 */
private val NOTICE_ROUND = 14.dp
private val CLESSIDRA = 3.dp

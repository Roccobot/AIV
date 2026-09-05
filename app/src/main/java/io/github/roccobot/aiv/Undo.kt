package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * L'eliminazione che si può ancora disfare, e la notifica con cui si offre.
 *
 * ⚠️⚠️ **RICHIESTA DELL'UTENTE, giro della `1.59`** (*se elimino un file singolo o un gruppo di
 * file con il cestino attivo, non c'è conferma (corretto), ma voglio anche un 'Annulla' rapido
 * come quello della selezione svuotata, che mi piace molto. Sempre durata 3 secondi, ma resta
 * disponibile anche se si cambia cartella*).
 *
 * ⚠️⚠️ **'ANCHE SE SI CAMBIA CARTELLA' È LA CLAUSOLA CHE DECIDE DOVE VIVE QUESTO STATO, ed è
 * l'opposto di quella dell'azzeramento della selezione**: quella notifica ha per chiave il
 * titolo della cartella **apposta**, perché deve sparire uscendo (*o finché non si cambia
 * cartella*, sue parole). Qui la richiesta è rovesciata, quindi lo stato non può stare nella
 * schermata: sta sopra di lei, e la notifica la disegna [AivApp] fuori dalla transizione fra
 * schermate.
 * ⚠️⚠️ **ED È UN OGGETTO DI PROCESSO E NON UN PARAMETRO, e la ragione è il conto dei posti da
 * toccare**: l'eliminazione passa da un imbuto solo (`FileOps`), ma quell'imbuto lo chiamano
 * **tre** schermate, e nessuna delle tre ha una ragione propria per conoscere un'offerta che
 * non riguarda lei. Passandola come parametro, ogni schermata nuova nascerebbe con un modo di
 * dimenticarsene. ⚠️ **Il rovescio è dichiarato**: è stato condiviso da tutto il processo,
 * quindi vive quanto lui; a tenerlo pulito è il conto alla rovescia, non l'ambito.
 * ⚠️ **Muore col processo, che è giusto**: un'offerta di tre secondi non deve sopravvivere a
 * un'app chiusa e riaperta, e la sua non sopravvive perché non è scritta da nessuna parte.
 */
object Undo {
    /**
     * Che cosa l'ultima eliminazione ha messo nel cestino, e che si può ancora riportare
     * indietro. Vuoto vuol dire che non c'è niente da offrire.
     *
     * ⚠️ **Gli indirizzi sono quelli NEL CESTINO e non quelli d'origine**: `Bin.restore` cerca
     * la riga d'archivio per il nome del file che trova là dentro, e i nomi d'origine possono
     * essere cambiati per non pestarsi i piedi (vedi `FileTree.freeName`).
     */
    var offerta by mutableStateOf<List<Uri>>(emptyList())
        private set

    /**
     * Un'eliminazione è appena andata a buon fine: si può disfare.
     *
     * ⚠️ **Un elenco vuoto non apre nessuna offerta**: se non è finito niente nel cestino non
     * c'è niente da rimettere a posto, e una notifica con un 'Annulla' che non fa nulla è
     * peggio di nessuna notifica.
     */
    fun offer(landed: List<Uri>) {
        if (landed.isEmpty()) return
        offerta = landed
    }

    /** L'offerta è scaduta, o è stata accettata. */
    fun clear() {
        offerta = emptyList()
    }
}

/**
 * La notifica che dice che cosa è appena successo e offre di disfarlo.
 *
 * ⚠️⚠️ **UNA SOLA PER DUE USI, DALLA `1.60`, ed è una richiesta sua che lo impone**: *in base
 * alla mia scelta aggiorna sia quello del cestino che quello dell'eliminazione*. Due copie
 * della stessa forma si aggiornano una sola volta, e la seconda resta indietro; qui il
 * disegno è uno e i due chiamanti passano soltanto le parole.
 *
 * ⚠️⚠️ **È UNO `Snackbar` DI MATERIAL E NON UNA SUPERFICIE DISEGNATA IN CASA**: la frase a
 * sinistra e l'azione a destra sulla stessa riga sono esattamente la sua forma, e con lui
 * arrivano il colore rovesciato, lo stondamento, i rientri e i due stili di testo, che
 * rifatti a mano sarebbero sei valori da indovinare (la nota in testa a `Glyphs.kt` dice di
 * non ridisegnare quello che Material ha già).
 * ⚠️⚠️ **SENZA `SnackbarHost` E SENZA `SnackbarHostState`, e non è una scorciatoia**: quella
 * coppia serve a chi ha una **coda** di messaggi da mostrare a turno, e vuole un
 * `Scaffold`, che queste schermate non hanno; qui il messaggio è uno solo, e la sua durata la
 * decide chi lo mostra, che è anche il posto in cui 'o finché non si cambia cartella' si può
 * scrivere. Con la coda, la durata sarebbe di Material e quella condizione non ci starebbe
 * dentro.
 * ⚠️⚠️ **DALLA `1.69` IL FONDO NON È PIÙ ROVESCIATO, ED È UNA SUA SCELTA FRA CINQUE
 * DISEGNI** (giro della `1.67`, domanda `d-avviso`: ha scelto `tempo`). Una notifica di
 * Material è chiara sul tema scuro e scura sul chiaro, cioè l'unica superficie dell'app che
 * inverte i colori, e in mezzo a pannelli e schede che non lo fanno si legge come un pezzo di
 * un'altra applicazione. Adesso prende la superficie dell'app, il suo inchiostro, il **bordo
 * d'accento** che portano tutte le altre ([Modifier.edged], dalla `1.54`) e lo stesso raggio
 * dei pannelli.
 * ⚠️⚠️ **E UNA RIGA CHE SI CONSUMA IN FONDO, che è la ragione del nome che quel disegno ha nel
 * documento**: dice quanto tempo resta per disfare, che è l'unica cosa che questa notifica non
 * sapeva comunicare. Prima il conto scorreva e basta, e chi non lo conosceva scopriva la
 * scadenza vedendola sparire.
 * ⚠️ **Il colore del tasto resta scritto a mano**, ma adesso è [accentInk]: su un fondo che non
 * è più rovesciato il colore che Material sceglie per il fondo rovesciato sarebbe sbagliato, e
 * questo è l'accento nella versione che si può **leggere**, che è il caso di una parola.
 * ⚠️⚠️ **ARRIVA E SE NE VA COME LE DUE SCHEDE, con gli stessi numeri** ([ARRIVO_RIGIDITA],
 * [SHEET_FADE_MS], [USCITA_MS], [ACCELERA] in `Sheet.kt`): dalla 1.43 'arrivare dal basso'
 * in questa app ha una definizione, e una notifica che comparisse di scatto accanto a due
 * schede che scorrono direbbe di essere un'altra famiglia di cose. ⚠️ **Non è la molla di
 * fabbrica**: quella non l'aveva scelta nessuno, ed è la ragione per cui in `ActionPad` è
 * stata sostituita anche dove funzionava.
 * ⚠️ **Sta in una funzione a sé per la stessa ragione di `FabPop`**: chiamata sul posto,
 * `AnimatedVisibility` finisce sull'overload di `ColumnScope` e il compilatore la rifiuta.
 */
@Composable
fun UndoNotice(
    visible: Boolean,
    text: String,
    action: String,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    /*
     * ⚠️⚠️ **LA RIGA SI CONSUMA IN [UNDO_MS], CHE È LA VITA VERA DELLA NOTIFICA**: i due
     * chiamanti aspettano esattamente quel tempo prima di toglierla, quindi la riga arriva a
     * zero nell'istante in cui la notifica se ne va. ⚠️ Chi un domani desse a una notifica una
     * vita diversa deve passare qui la sua durata, o la riga direbbe una scadenza che non è
     * quella: una barra ferma a zero sopra un tasto che funziona ancora è peggio di nessuna
     * barra.
     * ⚠️ **Riparte da capo a ogni comparsa** e non alla prima soltanto: due eliminazioni di
     * fila sono due notifiche, e la seconda deve avere il suo tempo intero.
     * ⚠️ **Si legge nel DISEGNO**: `scaleX` sta dentro `graphicsLayer`, quindi tre secondi di
     * animazione costano un ridisegno per fotogramma e nessuna ricomposizione.
     */
    val resta = remember { Animatable(1f) }
    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        resta.snapTo(1f)
        resta.animateTo(0f, tween(UNDO_MS.toInt(), easing = LinearEasing))
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
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
        )
    ) {
        Box(
            // ⚠️ **Il rientro di sistema se lo mette da sé**, come le due schede: questa vive
            // nel `Box` di radice di chi la mostra, che arriva al bordo dello schermo, quindi
            // senza questa riga starebbe sotto la barra di navigazione.
            // ⚠️ **E qui la scheda si comporta al contrario**: là il fondo passa sotto la
            // barra apposta (per prenderne il colore) e il rientro sta sul contenuto; una
            // notifica non è appoggiata a niente e va spostata intera.
            // ⚠️ **La scatola è nuova nella `1.69` e serve alla riga**: la riga deve stare
            // sopra la notifica e prenderne la misura senza cambiarla, che è quello che
            // `matchParentSize` fa e un figlio dello `Snackbar` non farebbe.
            modifier = Modifier
                .navigationBarsPadding()
                .padding(NOTICE_EDGE)
        ) {
            Snackbar(
                modifier = Modifier.edged(NOTICE_ROUND),
                shape = RoundedCornerShape(NOTICE_ROUND),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                action = {
                    TextButton(
                        onClick = onUndo,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = accentInk()
                        )
                    ) { Text(action) }
                }
            ) {
                Text(text)
            }
            /*
             * ⚠️ **Il ritaglio sta sulla scatola della riga e non su quella di fuori**: il
             * bordo d'accento sconfina di mezzo pixel oltre la superficie (vedi `Edge.kt`), e
             * un ritaglio sul genitore glielo taglierebbe proprio sugli archi, che è il
             * difetto che la `1.56` aveva chiuso.
             * ⚠️ **L'origine della scala è il fianco iniziale**, non il centro: una riga che
             * si consuma parte piena e si ritira verso il punto da cui è partita.
             */
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
                        .background(aivAccent(LocalAivLight.current))
                )
            }
        }
    }
}

/**
 * Quanto resta in scena una notifica che offre di disfare.
 *
 * ⚠️ **Tre secondi, come li ha chiesti** (*deve apparire per 3 secondi*, e poi *sempre durata
 * 3 secondi* per quella dell'eliminazione), e non è il valore di Material:
 * `SnackbarDuration.Short` sono 4 secondi e `Long` 10. Con la coda di Material non si potrebbe
 * nemmeno scegliere, ed è una delle ragioni per cui qui non c'è (vedi [UndoNotice]).
 */
const val UNDO_MS = 3000L

/**
 * Il respiro fra la notifica e i tre bordi che la circondano.
 *
 * ⚠️ **12dp, che è quello che `SnackbarHost` di Material mette da sé**: qui l'ospite non
 * c'è, quindi il margine che avrebbe messo lui va scritto. Senza, la notifica toccherebbe
 * i lati dello schermo e la barra di sistema.
 */
private val NOTICE_EDGE = 12.dp

/**
 * Lo stondamento della notifica e lo spessore della riga che si consuma.
 *
 * ⚠️ **Il raggio è quello dei pannelli e non un numero suo**: dalla `1.69` questa notifica è
 * una superficie dell'app come le altre, e un raggio diverso la rimetterebbe fuori famiglia
 * proprio mentre la si porta dentro.
 * ⚠️ **Tre punti per la riga**, che è la misura del disegno che ha scelto: più sottile non si
 * vede su un fondo che ha già un bordo da due, più spessa diventa una seconda cornice.
 */
private val NOTICE_ROUND = 14.dp
private val CLESSIDRA = 3.dp

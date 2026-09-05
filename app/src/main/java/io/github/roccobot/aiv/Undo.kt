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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
 * ⚠️ **Il colore del tasto si scrive a mano**: dentro `Snackbar` un `TextButton` prende il
 * suo `primary` di fabbrica, che è pensato per il fondo della pagina e non per quello
 * rovesciato di una notifica. [SnackbarDefaults.actionContentColor] è il colore che
 * Material ha scelto per **quel** fondo.
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
        Snackbar(
            modifier = Modifier
                // ⚠️ **Il rientro di sistema se lo mette da sé**, come le due schede: questa
                // vive nel `Box` di radice di chi la mostra, che arriva al bordo dello
                // schermo, quindi senza questa riga starebbe sotto la barra di navigazione.
                // ⚠️ **E qui la scheda si comporta al contrario**: là il fondo passa sotto
                // la barra apposta (per prenderne il colore) e il rientro sta sul contenuto;
                // una notifica non è appoggiata a niente e va spostata intera.
                .navigationBarsPadding()
                .padding(NOTICE_EDGE),
            action = {
                TextButton(
                    onClick = onUndo,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = SnackbarDefaults.actionContentColor
                    )
                ) { Text(action) }
            }
        ) {
            Text(text)
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

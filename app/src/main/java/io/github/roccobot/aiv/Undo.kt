package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Quello che si può ancora disfare, e per quanto tempo.
 *
 * ⚠️⚠️ **QUI NON C'È PIÙ NESSUNA NOTIFICA, DALLA `1.84`**: la superficie con cui l'app parla è
 * una sola e vive in `Notice.kt`, insieme al canale che la alimenta. Questo oggetto dice
 * **che cosa** si può disfare; a offrirlo è [AivApp], che compone la frase e la manda al canale.
 * Chi cerca il disegno o la durata della riga lo trova là.
 *
 * ⚠️⚠️ **RICHIESTA DELL'UTENTE, giro della `1.59`** (*se elimino un file singolo o un gruppo di
 * file con il cestino attivo, non c'è conferma (corretto), ma voglio anche un 'Annulla' rapido
 * come quello della selezione svuotata, che mi piace molto. Sempre durata 3 secondi, ma resta
 * disponibile anche se si cambia cartella*).
 *
 * ⚠️⚠️ **'ANCHE SE SI CAMBIA CARTELLA' È LA CLAUSOLA CHE DECIDE DOVE VIVE QUESTO STATO, ed è
 * l'opposto di quella dell'azzeramento della selezione**: quella riga deve sparire uscendo dalla
 * cartella (*o finché non si cambia cartella*, sue parole), e da quando il canale è unico lo
 * ottiene togliendola quando la griglia lascia la scena. Qui la richiesta è rovesciata, quindi
 * lo stato non può vivere in una schermata: vive sopra di loro, e a offrirlo è [AivApp], fuori
 * dalla transizione fra schermate.
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
     * Che cosa si può disfare adesso, e come.
     *
     * ⚠️⚠️ **DA UN ELENCO DI INDIRIZZI A UN TIPO, DALLA `1.83`** (campo libero del giro della
     * `1.82`, punto B: *aggiungi degli 'Annulla' temporizzati (avvisi in basso) anche per le
     * operazioni di copia e spostamento*). Fino alla `1.82` qui c'era la sola lista dei file
     * finiti nel cestino, perché l'eliminazione era l'unica cosa che si potesse disfare: adesso
     * le operazioni sono tre e ognuna torna indietro in un modo suo, quindi l'offerta deve dire
     * **che cosa** è successo e non solo su quali file.
     * ⚠️ **Il tipo di operazione serve anche alla frase**: la notifica scrive il plurale di
     * [FileKind.done], che esiste già in ventotto lingue per tutte e tre.
     */
    sealed interface Offer {
        /** Che operazione è stata fatta: decide la frase e il modo di tornare indietro. */
        val kind: FileKind

        /** Su quanti file, cioè il numero che finisce nella frase. */
        val count: Int

        /**
         * Un'eliminazione col cestino acceso: i file sono nel cestino e da lì si ripristinano.
         *
         * ⚠️ **Gli indirizzi sono quelli NEL CESTINO e non quelli d'origine**: `Bin.restore`
         * cerca la riga d'archivio per il nome del file che trova là dentro, e i nomi d'origine
         * possono essere cambiati per non pestarsi i piedi (vedi `FileTree.freeName`).
         */
        data class Trashed(val landed: List<Uri>) : Offer {
            override val kind = FileKind.TRASH
            override val count = landed.size
        }

        /**
         * Una copia o uno spostamento appena fatti, che si disfano sui file.
         *
         * ⚠️ **I passi li produce l'operazione stessa** (`FileTree.Outcome.undo`), e a eseguirli
         * al contrario è `FileTree.revert`: qui dentro non c'è nessuna logica di file, perché
         * questo oggetto vive quanto il processo e non deve sapere niente del disco.
         */
        data class Files(
            override val kind: FileKind,
            val steps: List<FileTree.Undoable>
        ) : Offer {
            override val count = steps.size
        }
    }

    /** Che cosa si può disfare adesso, e `null` quando non c'è niente da offrire. */
    var offerta by mutableStateOf<Offer?>(null)
        private set

    /**
     * Un'operazione è appena andata a buon fine: si può disfare.
     *
     * ⚠️ **Un'offerta vuota non apre niente**: se non è stato toccato nessun file non c'è niente
     * da rimettere a posto, e una notifica con un 'Annulla' che non fa nulla è peggio di nessuna
     * notifica. La prova vive qui e non nei tre chiamanti, che altrimenti la scriverebbero in tre
     * modi.
     */
    fun offer(what: Offer) {
        if (what.count == 0) return
        offerta = what
    }

    /** L'offerta è scaduta, o è stata accettata. */
    fun clear() {
        offerta = null
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

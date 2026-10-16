package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Quello che si può ancora disfare, e per quanto tempo.
 *
 * Questo oggetto dice che cosa si può disfare; a offrirlo è [AivApp], che compone la frase e la
 * manda al canale unico degli avvisi (`Notice.kt`, dove sono anche il disegno e la durata).
 *
 * ⚠️ **L'offerta resta disponibile anche se si cambia cartella** (richiesta dell'utente, giro
 * della `1.59`), al contrario di quella dell'azzeramento della selezione, che sparisce quando la
 * griglia lascia la scena: per questo lo stato non vive in una schermata ma sopra di loro, fuori
 * dalla transizione fra schermate.
 * È un oggetto di processo e non un parametro perché l'imbuto delle operazioni (`FileOps`) lo
 * chiamano tre schermate, e nessuna ha una ragione propria per conoscere l'offerta: passata come
 * parametro, ogni schermata nuova potrebbe dimenticarsene. Vive quanto il processo, e a tenerlo
 * pulito è il conto alla rovescia; un'app chiusa e riaperta non ritrova l'offerta, ed è giusto.
 */
object Undo {
    /**
     * Che cosa si può disfare adesso, e come.
     *
     * Le operazioni che si disfano sono tre (eliminazione, copia, spostamento) e ognuna torna
     * indietro in un modo suo, quindi l'offerta dice che cosa è successo e non solo su quali
     * file. Il tipo serve anche alla frase: la notifica scrive il plurale di [FileKind.done].
     */
    sealed interface Offer {
        /** Che operazione è stata fatta: decide la frase e il modo di tornare indietro. */
        val kind: FileKind

        /** Su quanti file, cioè il numero che finisce nella frase. */
        val count: Int

        /**
         * Un'eliminazione col cestino acceso: i file sono nel cestino e da lì si ripristinano.
         *
         * ⚠️ **Gli indirizzi sono quelli nel cestino, non quelli d'origine**: `Bin.restore`
         * cerca la riga d'archivio per il nome del file nel cestino, e quel nome può essere
         * diverso dall'originale per evitare un doppione (vedi `FileTree.freeName`).
         */
        data class Trashed(val landed: List<Uri>) : Offer {
            override val kind = FileKind.TRASH
            override val count = landed.size
        }

        /**
         * Una copia o uno spostamento appena fatti, che si disfano sui file.
         *
         * I passi li produce l'operazione stessa (`FileTree.Outcome.undo`) e li esegue al
         * contrario `FileTree.revert`: questo oggetto vive quanto il processo e non sa niente
         * del disco.
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
     * Un'offerta vuota è ignorata: una notifica con un 'Annulla' che non fa nulla è peggio di
     * nessuna notifica. Il controllo è qui, una volta sola per i tre chiamanti.
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
 * Tre secondi, come li ha chiesti l'utente; i valori di Material sono altri
 * (`SnackbarDuration.Short` 4 secondi, `Long` 10), e la sua coda non li lascerebbe scegliere
 * (vedi [AppNotice]).
 */
const val UNDO_MS = 3000L

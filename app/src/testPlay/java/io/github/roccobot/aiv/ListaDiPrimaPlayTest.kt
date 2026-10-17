package io.github.roccobot.aiv

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * La lista della schermata nuova nella `6.27` (rilievo C30 della revisione della 6.25): una
 * lettura partita per la schermata di prima non la sostituisce più quando arriva in ritardo.
 *
 * ⚠️ **Il caso è la ricerca, perché è il solo con un ritardo che si comanda**: la pausa prima della
 * query (`SEARCH_PAUSE_MS`) si attraversa spostando l'orologio del looper principale, mentre una
 * cascata del web aspetta la rete. Il meccanismo è lo stesso per tutte le letture della lista,
 * che dalla `6.27` passano da un lavoro solo.
 * ⚠️ **È una prova di AIV Play anche se il codice è comune**: senza il permesso delle immagini la
 * ricerca risponde subito `NoPermission`, che si distingue dalla lista della schermata nuova. Nella
 * variante GitHub il controllo passa da `Environment.isExternalStorageManager()`, che nel banco va
 * in errore per mancanza del pacchetto, e la lettura non arriva mai.
 * ⚠️ **Prima si aspettano le impostazioni**: al loro arrivo il modello apre la cartella d'avvio, e
 * se arrivassero a metà prova riscriverebbero la schermata e la lista.
 */
@RunWith(AndroidJUnit4::class)
class ListaDiPrimaPlayTest {

    /** I modelli costruiti dalle prove, chiusi alla fine di ognuna (`ModelliChiusi`). */
    @get:Rule
    val modelli = ModelliChiusi()

    private val main get() = shadowOf(Looper.getMainLooper())

    /**
     * Fa girare il looper principale finché [fatto] non è vero, o per circa cinque secondi.
     *
     * ⚠️ Con [avanti] l'orologio fa prima un salto di un secondo, che attraversa la pausa della
     * ricerca; senza, il tempo del looper resta fermo e la pausa non finisce.
     */
    private fun aspetta(avanti: Boolean = true, fatto: () -> Boolean = { false }) {
        if (avanti) main.idleFor(Duration.ofSeconds(1))
        repeat(250) {
            if (fatto()) return
            Thread.sleep(20)
            main.idle()
        }
    }

    /**
     * **Si scrive nella ricerca e si apre il cestino prima della pausa: resta la lista del cestino.**
     *
     * ⚠️ **Controprovata** col lancio di prima, che non teneva il lavoro: la ricerca risponde dopo
     * la pausa e la lista del cestino diventa `NoPermission`.
     */
    @Test
    fun `la ricerca lasciata non sostituisce la lista nuova`() {
        val model = modelli.nuovo(ApplicationProvider.getApplicationContext<Application>())
        aspetta { model.settings != null }
        assertNotNull("Le impostazioni non sono arrivate", model.settings)

        model.openSearch()
        model.search("mare")
        model.openBin()
        // Il cestino si legge a orologio fermo, quindi prima che la pausa della ricerca finisca.
        aspetta(avanti = false) { model.folder != null }
        val cestino = model.folder
        assertNotNull("Il cestino non si è letto", cestino)

        aspetta()
        assertEquals("La ricerca lasciata ha sostituito la lista del cestino", cestino, model.folder)
    }
}

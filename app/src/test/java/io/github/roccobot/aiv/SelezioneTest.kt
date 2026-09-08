package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Il banco della **selezione dal FAB**, nato con la `1.87`.
 *
 * ⚠️⚠️ **ESISTE PER LA METÀ PROATTIVA DELLA REGOLA** (`AIV/CLAUDE.md`, § 'Quando si scrive una
 * prova, e quando no'): la scorciatoia vive su un tasto che si stacca in una finestra sua e
 * sparisce a metà gesto, e il suo difetto tipico non dà nessun errore. Un gesto che non arriva
 * al suo tasto compila, si tocca, e semplicemente non fa niente.
 * ⚠️ **Il conto è la spia, e non la selezione delle miniature**: l'app scrive il numero sotto il
 * titolo con due chiavi diverse a seconda che ci sia o no una selezione, quindi leggere quel
 * testo misura in un colpo il gesto e il conto che lo racconta.
 * ⚠️ **Che cosa NON vede**: la vibrazione del tocco lungo, che dipende dall'apparecchio.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [OmbraArchivio::class])
class SelezioneTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **Il tocco lungo sul FAB prende tutto, e da lì in poi il FAB non è più in scena.**
     *
     * ⚠️⚠️ **NASCE DALLA SUA RICHIESTA E HA MISURATO PERCHÉ NON SI PUÒ FARE COM'È SCRITTA**
     * (2026-09-08: *la pressione lunga sul FAB in una cartella deve selezionare/deselezionare
     * tutto*). La prima stesura provava il gesto due volte aspettandosi il rovescio, ed è
     * fallita alla prima corsa con 'the node is no longer in the tree': appena c'è una
     * selezione il FAB lascia il posto alla scheda dei comandi, quindi su quel tasto un
     * secondo gesto non arriva.
     * ⚠️⚠️ **LE DUE METÀ VANNO INSIEME**: la prima presidia la scorciatoia, la seconda il
     * fatto su cui poggia la risposta data a lui. Se un domani il FAB dovesse restare in scena
     * durante la selezione, questa prova diventa rossa e chi la legge trova scritto perché,
     * invece di scoprirlo con un gesto che non fa niente.
     */
    @Test
    fun `il tocco lungo sul FAB prende tutto e poi il FAB lascia il posto`() {
        banco.setContent { Scena() }
        banco.waitForIdle()

        val comandi = app.getString(R.string.pick_actions)
        banco.onNodeWithContentDescription(comandi).performTouchInput { longClick() }
        banco.waitForIdle()

        assertTrue(
            "Dopo il tocco lungo il conto non dice che sono selezionate: " +
                "il gesto non ha preso niente",
            inScena(scelte = true)
        )
        assertTrue(
            "Con la selezione in corso il FAB è ancora in scena: allora il suo tocco lungo " +
                "può avere un rovescio, e questa prova va riscritta insieme alla scorciatoia",
            banco.onAllNodesWithContentDescription(comandi).fetchSemanticsNodes().isEmpty()
        )
    }

    /**
     * Se in scena c'è il conto della selezione oppure quello del totale.
     *
     * ⚠️ **`onAllNodes` e non `onNode`**: il conto sta due volte nell'albero, nella fascia e nella
     * testata, perché le due copie si scambiano con due opacità complementari. Chiederne uno solo
     * darebbe errore proprio quando la scena è quella giusta.
     */
    private fun inScena(scelte: Boolean): Boolean {
        val detto = app.resources.getQuantityString(
            if (scelte) R.plurals.pick_count else R.plurals.items_count,
            FOTO.size,
            FOTO.size
        )
        return banco.onAllNodesWithText(detto).fetchSemanticsNodes().isNotEmpty()
    }

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * Una cartella con le sue immagini e il FAB in scena.
     *
     * ⚠️ **`onSearchHere` serve a far esistere il FAB**: senza nessuna delle tre voci il menu
     * sarebbe vuoto, e un FAB che apre un menu vuoto la griglia non lo disegna affatto.
     */
    @Composable
    private fun Scena() {
        AivTheme(darkTheme = false) {
            Box(modifier = Modifier.fillMaxSize()) {
                GridScreen(
                    title = TITOLO,
                    items = FOTO,
                    highlight = null,
                    onOpen = {},
                    onBack = {},
                    onChanged = {},
                    onSearch = {},
                    onSearchHere = {}
                )
            }
        }
    }
}

/** Le immagini della cartella finta: poche, perché qui si conta e non si scorre. */
private val FOTO = (1..6).map { Uri.parse("file:///finta/$it.jpg") }

/** Il nome della cartella finta. */
private const val TITOLO = "Cartella di prova"

package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Il banco di prova dei due **tasti dello scorrimento**, nati nella `1.95`.
 *
 * ⚠️⚠️ **NASCE CON LA FUNZIONE, per la metà proattiva della regola** (`CLAUDE.md`, § '🧪 Quando
 * si scrive una prova, e quando no'): il salto passa il proprio movimento allo **scorrimento
 * annidato** prima che alla lista, cioè fa a mano quello che un dito ottiene dal sistema, e là i
 * segni sono due mondi opposti. Un `-` di troppo dà un tasto che va dalla parte sbagliata, e
 * nessun compilatore lo vede.
 *
 * ⚠️ **Che cosa NON vede**: quando i tasti compaiono e quando se ne vanno. Quella è un'attesa di
 * due secondi dopo la quiete, e nel banco il clock di prova la porta a termine dentro
 * `waitForIdle`, cioè la misurerebbe sempre scaduta. Si guarda sul telefono, ed è dichiarato
 * nella voce di collaudo. Neanche la decelerazione si vede: quella è resa, non struttura.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [OmbraArchivio::class])
class SaltiTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **A riposo i due tasti non sono nell'albero.**
     *
     * ⚠️⚠️ **È LA TRAPPOLA DELLA `1.70` IN PICCOLO**: un nodo che sta in scena sempre e si limita
     * a non farsi vedere continua a esistere per la prova del tocco, e quello che è sotto non
     * riceve niente. La difesa è l'assenza, e questa prova la misura: appena aperta una cartella
     * nessuno ha ancora scorso, quindi di tasti non ce ne devono essere.
     */
    @Test
    fun `a riposo i due tasti non ci sono`() {
        banco.setContent { Scena() }
        banco.waitForIdle()

        assertEquals("Il tasto 'in cima' non deve esistere prima di scorrere", 0, quanti(R.string.jump_top))
        assertEquals("Il tasto 'in fondo' non deve esistere prima di scorrere", 0, quanti(R.string.jump_bottom))
    }

    /**
     * **Il salto in giù chiude l'intestazione e scorre la lista; quello in su le riapre tutte e
     * due.**
     *
     * ⚠️⚠️ **LA SECONDA METÀ È LA SUA RICHIESTA ALLA LETTERA** (*il tasto 'su' fa scorrere in
     * cima fino alla visualizzazione piena dell'intestazione*): una corsa che muovesse la sola
     * lista arriverebbe in cima con la fascia ancora chiusa, e la prova diventa rossa.
     * ⚠️ **Controprovata rimettendo il difetto, e la misura corregge quello che mi aspettavo**:
     * passando `nested = null` fallisce la **prima** asserzione, perché senza lo scorrimento
     * annidato la fascia non si chiude affatto (resta a 0) invece di restare chiusa alla fine.
     * Cioè il difetto si vede già scendendo, un passo prima di dove lo cercavo.
     */
    @Test
    fun `il salto passa dallo scorrimento annidato`() {
        var shut = 0f
        var lista: LazyListState? = null
        var vai: (Float) -> Unit = {}
        banco.setContent {
            val state = rememberLazyListState()
            var chiuso by remember { mutableFloatStateOf(0f) }
            val paging = remember { frontScroll(FASCIA, { chiuso }, { chiuso = it }) }
            val scope = rememberCoroutineScope()
            lista = state
            vai = { quanti -> scope.launch { glide(state, paging, quanti) }; Unit }
            shut = chiuso
            Box(Modifier.fillMaxSize().nestedScroll(paging)) {
                LazyColumn(state = state) {
                    items(RIGHE) { n -> Text("riga $n", modifier = Modifier.height(RIGA.dp)) }
                }
            }
        }
        banco.waitForIdle()

        banco.runOnIdle { vai(LONTANO) }
        banco.waitForIdle()
        assertEquals("Scendendo, la fascia deve chiudersi tutta", FASCIA, shut, 0.5f)
        assertTrue(
            "Scendendo, la lista deve muoversi",
            (lista?.firstVisibleItemIndex ?: 0) > 0
        )

        banco.runOnIdle { vai(-LONTANO) }
        banco.waitForIdle()
        assertEquals("Salendo, la lista deve tornare in cima", 0, lista?.firstVisibleItemIndex)
        assertEquals("Salendo, la fascia deve riaprirsi", 0f, shut, 0.5f)
    }

    /**
     * **Un tasto occupa esattamente la misura dichiarata, e non una più grande.**
     *
     * ⚠️⚠️ **NASCE DA UNA VOCE NON APPROVATA, ED È LA REGOLA** (`AIV/CLAUDE.md`, § '🧪 Quando si
     * scrive una prova, e quando no'): nel giro della `1.95` la voce `salti-tasti` è tornata
     * indietro (*i tondi in cui si trovano ... appaiono come rettangoli ad ogni tocco*), e la
     * misura ha trovato una causa che nessuno aveva ragionato: un [androidx.compose.material3.IconButton]
     * porta `minimumInteractiveComponentSize`, che **ignora i vincoli in entrata** e restituisce
     * al genitore 48dp qualunque misura gli si dia. Il tondo dipinto era quindi più grande del
     * FAB stesso, che ne misura 40.
     *
     * ⚠️ **Che cosa questa prova NON vede**: la forma che il tasto prendeva **premuto**, che in
     * Material 3 Expressive è un'altra ([androidx.compose.material3.IconButton] morfa da tondo a
     * quadrato arrotondato). Quella è resa, e a toglierla è la stessa correzione: senza quel
     * componente non c'è più nessuno stato premuto da disegnare.
     */
    @Test
    fun `il tasto occupa la misura dichiarata`() {
        banco.mainClock.autoAdvance = false
        banco.setContent { Scena() }
        banco.mainClock.advanceTimeBy(RESPIRO)

        /*
         * ⚠️ **Il trascinamento ha coordinate esplicite**, come in `IntestazioneTest`: `swipeUp()`
         * nudo parte dal bordo di sotto della radice, dove non c'è nessuna griglia sotto il dito.
         */
        val scena = banco.onRoot().fetchSemanticsNode().size
        banco.onRoot().performTouchInput {
            swipe(
                start = Offset(scena.width / 2f, scena.height * DA),
                end = Offset(scena.width / 2f, scena.height * A),
                durationMillis = LENTO
            )
        }
        /*
         * ⚠️⚠️ **IL CLOCK RESTA FERMO, o i tasti non ci sono più**: dopo la quiete parte il conto
         * alla rovescia di due secondi, e `waitForIdle` lo porterebbe a termine. Si avanza quel
         * tanto che basta alla dissolvenza di entrata.
         */
        banco.mainClock.advanceTimeBy(JUMP_FADE_MS.toLong())

        val riquadro = banco.onAllNodesWithContentDescription(voce(R.string.jump_top))[0]
            .getUnclippedBoundsInRoot()
        assertEquals("Il tasto è largo quanto dichiarato", JUMP_TAP.value, riquadro.width.value, 0.5f)
        assertEquals("Il tasto è alto quanto dichiarato", JUMP_TAP.value, riquadro.height.value, 0.5f)
    }

    private fun voce(id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun quanti(id: Int): Int =
        banco.onAllNodesWithContentDescription(voce(id)).fetchSemanticsNodes().size

    /** La griglia di una cartella con gli argomenti minimi, come nelle altre prove. */
    @Composable
    private fun Scena() {
        AivTheme(darkTheme = false) {
            Box(modifier = Modifier.fillMaxSize()) {
                GridScreen(
                    title = "Cartella di prova",
                    items = (1..40).map { Uri.parse("file:///finta/$it.jpg") },
                    highlight = null,
                    onOpen = {},
                    onBack = {},
                    onChanged = {},
                    onSearch = {}
                )
            }
        }
    }
}

/** Quanto è alta la fascia finta della prova, in pixel. */
private const val FASCIA = 300f

/** Quante righe ha la lista finta: abbastanza da avere sempre dove andare. */
private const val RIGHE = 200

/** Quanto è alta una riga, in punti. */
private const val RIGA = 48

/**
 * Un salto più lungo di tutta la lista.
 *
 * ⚠️ **Di proposito eccessivo**: la corsa finisce quando nessuno prende più niente, quindi una
 * stima lunga arriva al bordo e si ferma. Chiedendo esattamente la distanza, la prova
 * misurerebbe la stima invece del meccanismo.
 */
private const val LONTANO = 100_000f

/** Quanto si lascia comporre la scena prima di toccarla, col clock fermo. */
private const val RESPIRO = 1_000L

/** Da dove a dove va il trascinamento, in frazioni dell'altezza della scena. */
private const val DA = 0.8f
private const val A = 0.2f

/** Quanto dura il trascinamento: lento abbastanza da essere uno scorrimento e non un lancio. */
private const val LENTO = 300L

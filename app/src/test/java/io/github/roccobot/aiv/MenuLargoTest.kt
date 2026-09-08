package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Il banco di prova della **larghezza** del menu ancorato al FAB.
 *
 * ⚠️⚠️ **NASCE DA UNA VOCE NON APPROVATA, ED È LA REGOLA** (`AIV/CLAUDE.md` § '🧪 Quando si
 * scrive una prova, e quando no': *un difetto che è arrivato a lui torna indietro con la prova
 * che lo avrebbe fermato, nella stessa versione della correzione*). La voce è `menu-bordo` del
 * giro della `1.89`: *bisogna far sì che con l'ombra attiva il pannello sia più largo di due
 * miniature della griglia più lo spazio che le separa*.
 *
 * ⚠️⚠️ **LA CELLA SI MISURA NELL'ALBERO, NON SI RICALCOLA**: rifare qui il conto di `menuFloor`
 * darebbe una prova che ricopia l'implementazione, cioè verde per costruzione e cieca al difetto.
 * La scena monta una fila di riquadri disposti **come** le celle di una griglia (stesso margine
 * di lato, stesso spazio in mezzo, stessa divisione dello spazio restante), e la misura esce da
 * lì.
 *
 * ⚠️ **Quello che questa prova NON vede**: se il pannello, dal vivo, sembri largo al punto
 * giusto. Misura la relazione che lui ha dettato, cioè che il pannello copra più di due celle e
 * che non si allarghi oltre il tetto. Come si vede sullo schermo lo dice il telefono.
 */
@RunWith(AndroidJUnit4::class)
class MenuLargoTest {

    @get:Rule
    val banco = createComposeRule()

    @After
    fun pulisci() {
        MenuScene.clear()
        VeilStage.clear()
    }

    /**
     * **Caso 1: con tre colonne il pannello è più largo di due celle più il loro spazio.**
     *
     * ⚠️ **Tre colonne è il caso del suo mockup**, dove il tetto non interviene: il conto vero si
     * vede solo là, perché con due colonne 'due celle più lo spazio' è già la griglia intera.
     * ⚠️ **Controprovata rimettendo il difetto**: togliendo `minWidth` da `MenuShell` il pannello
     * torna largo quanto il suo contenuto e la prova diventa rossa.
     */
    @Test
    fun `con tre colonne il menu supera due celle`() {
        var cella = 0
        var pannello = 0
        monta(colonne = 3, gap = GAP_GRIGLIA) {
            cella = banco.onNodeWithTag(CELLA).fetchSemanticsNode().size.width
            pannello = pannello()
        }
        val due = 2 * cella + px(GAP_GRIGLIA)
        assertTrue(
            "Il pannello non copre due celle più lo spazio: $pannello contro $due",
            pannello > due
        )
    }

    /**
     * **Caso 2: con due colonne il tetto entra in funzione.**
     *
     * ⚠️⚠️ **SENZA IL TETTO IL MENU SAREBBE LARGO QUANTO LA FINESTRA**, ed è il caso della
     * schermata iniziale, che di fabbrica ha due colonne: là 'due celle più lo spazio' è tutta la
     * griglia, e il pannello finirebbe da bordo a bordo. Limitato, si ferma a [MENU_INSET] per
     * lato, cioè **dentro** il margine delle colonne, che è la cosa che lui ha chiesto.
     */
    @Test
    fun `con due colonne il menu si ferma al tetto`() {
        var finestra = 0
        var dentro = 0
        var pannello = 0
        monta(colonne = 2, gap = GAP_CARTELLE) {
            finestra = banco.onNodeWithTag(SCENA).fetchSemanticsNode().size.width
            dentro = px(MENU_INSET)
            pannello = pannello()
        }
        assertEquals(
            "Il pannello non si è fermato al tetto: con due colonne sfora i margini",
            finestra - 2 * dentro,
            pannello
        )
    }

    /** Monta la scena con la finta griglia e il menu aperto, poi esegue le misure. */
    private fun monta(colonne: Int, gap: Dp, misura: () -> Unit) {
        banco.setContent {
            CompositionLocalProvider(LocalAivDepth provides PanelDepth.NONE) {
                AivTheme(darkTheme = false) { Scena(colonne, gap) }
            }
        }
        banco.waitForIdle()
        misura()
    }

    /**
     * La larghezza del **pannello**, che senza effetto coincide con quella della sua finestra.
     *
     * ⚠️ **Senza effetto e non con l'ombra**, di proposito: con l'ombra la finestra porta la sua
     * aria per lato, e la misura direbbe un numero più grande di quello che si vede. Il caso
     * dell'ombra ha già la sua prova, in `ProfonditaTest`.
     */
    private fun pannello(): Int =
        banco.onNode(isRoot() and hasAnyDescendant(hasTestTag(VOCE)))
            .fetchSemanticsNode().size.width

    private fun px(quanto: Dp): Int = with(banco.density) { quanto.roundToPx() }

    /**
     * Una fila di riquadri disposta **come** una griglia, più il menu aperto sopra di lei.
     *
     * ⚠️ **Il peso uguale e lo spazio in mezzo sono quello che fa una `LazyVerticalGrid` a
     * colonne fisse**: la cella che ne esce è la stessa, e questo è il motivo per cui la misura
     * presa qui vale per la griglia vera.
     */
    @Composable
    private fun Scena(colonne: Int, gap: Dp) {
        Box(modifier = Modifier.fillMaxWidth().testTag(SCENA)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = GRID_PAD_X),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                repeat(colonne) { quale ->
                    Box(
                        modifier = Modifier
                            .then(if (quale == 0) Modifier.testTag(CELLA) else Modifier)
                            .weight(1f)
                            .height(60.dp)
                    )
                }
            }
            val stato = rememberMenuState()
            LaunchedEffect(stato) { stato.open() }
            MenuShell(
                state = stato,
                position = rememberMenuAtAnchor(),
                minWidth = menuFloor(colonne, gap)
            ) {
                // ⚠️ Stretto apposta: se il minimo non arriva, il pannello resta di questa misura.
                Box(modifier = Modifier.testTag(VOCE).width(40.dp).height(40.dp))
            }
        }
    }
}

/** Lo spazio fra le miniature di una cartella, cioè `GAP` di `GridScreen`. */
private val GAP_GRIGLIA = 3.dp

/** Lo spazio fra le copertine della schermata iniziale, cioè `FOLDER_GAP` di `FolderScreen`. */
private val GAP_CARTELLE = 12.dp

private const val VOCE = "voce-larga"
private const val CELLA = "cella"
private const val SCENA = "scena"

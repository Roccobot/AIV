package io.github.roccobot.aiv

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * I due gesti di una **voce di menu**: il tocco breve e quello lungo, con e senza il secondo.
 *
 * ⚠️⚠️ **NASCE CON LA CATENA SOLA DELLA `1.81`, ED È LA PROVA CHE QUELLA MODIFICA DEVE
 * PORTARE.** Fino alla `1.80` [MenuRow] installava il gesto in due modi opposti
 * (`combinedClickable` con un tocco lungo, `clickable` senza), e il censimento della UI del
 * 2026-09-05 lo ha segnalato come incoerenza interna. Unificarlo tocca la **gerarchia dei
 * tocchi**, ed è uno dei tre casi in cui `AIV/CLAUDE.md` § '🧪 Quando si scrive una prova, e
 * quando no' chiede la prova anche senza un difetto alle spalle: là un codice valido può non
 * fare niente, e non lo vede nessun compilatore.
 *
 * ⚠️ **Ogni voce dei menu dell'app passa da qui** dalla `1.46`, quindi questa prova copre la
 * superficie e non un chiamante: non esiste un secondo modo di scrivere una voce di menu.
 *
 * ⚠️ **Quello che il banco non vede** resta quello dichiarato in [TocchiTest]: il colpetto di
 * ritorno del tocco lungo esiste solo su un telefono, e qui si verifica che l'azione arrivi,
 * non che la mano la senta.
 */
@RunWith(AndroidJUnit4::class)
class MenuRowTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **Caso 1: la voce con due gesti li riceve entrambi, e non li confonde.**
     *
     * ⚠️ **I due conti sono separati apposta**: un `combinedClickable` che chiamasse anche
     * `onClick` alla fine di una pressione lunga passerebbe una prova che guardasse il solo
     * tocco lungo, e all'utente arriverebbero due azioni per un gesto.
     */
    @Test
    fun `una voce con tocco lungo riceve i due gesti separati`() {
        var brevi = 0
        var lunghi = 0
        banco.setContent {
            AivTheme(darkTheme = false) {
                MenuRow(
                    text = VOCE,
                    icon = Icons.Default.Info,
                    onTap = { brevi++ },
                    holdLabel = TENUTA,
                    onHold = { lunghi++ }
                )
            }
        }

        banco.onNodeWithText(VOCE).performClick()
        assertEquals("Il tocco breve non è arrivato", 1, brevi)
        assertEquals("Il tocco breve ha fatto scattare anche quello lungo", 0, lunghi)

        banco.onNodeWithText(VOCE).performTouchInput { longClick() }
        assertEquals("Il tocco lungo non è arrivato", 1, lunghi)
        assertEquals("Il tocco lungo ha fatto scattare anche quello breve", 1, brevi)
    }

    /**
     * **Caso 2: la voce SENZA tocco lungo risponde comunque al tocco breve.**
     *
     * ⚠️⚠️ **È IL CASO CHE LA `1.81` HA CAMBIATO**: quella voce prendeva un `clickable` e adesso
     * prende un `combinedClickable` con `onLongClick` nullo. Se quel nullo installasse un gesto
     * lungo che intercetta, o se cambiasse il riconoscimento del tocco breve, il difetto
     * arriverebbe su **ogni** menu dell'app senza che niente lo segnali.
     */
    @Test
    fun `una voce senza tocco lungo risponde al tocco breve`() {
        var brevi = 0
        banco.setContent {
            AivTheme(darkTheme = false) {
                MenuRow(text = VOCE, icon = Icons.Default.Info, onTap = { brevi++ })
            }
        }

        banco.onNodeWithText(VOCE).performClick()

        assertEquals("Il tocco breve non è arrivato alla voce senza gesto lungo", 1, brevi)
    }

    /**
     * **Caso 3: una voce spenta non risponde a nessuno dei due gesti.**
     *
     * ⚠️ **Spenta resta in scena**, che è la scelta dichiarata su `MenuRow.enabled`: quindi il
     * nodo si trova e si tocca, e quello che deve mancare è l'azione. Una catena che ignorasse
     * `enabled` su uno dei due rami darebbe un comando inerte che funziona.
     */
    @Test
    fun `una voce spenta non risponde`() {
        var brevi = 0
        var lunghi = 0
        banco.setContent {
            AivTheme(darkTheme = false) {
                MenuRow(
                    text = VOCE,
                    icon = Icons.Default.Info,
                    onTap = { brevi++ },
                    holdLabel = TENUTA,
                    onHold = { lunghi++ },
                    enabled = false
                )
            }
        }

        banco.onNodeWithText(VOCE).performClick()
        banco.onNodeWithText(VOCE).performTouchInput { longClick() }

        assertEquals("Una voce spenta ha risposto al tocco breve", 0, brevi)
        assertEquals("Una voce spenta ha risposto al tocco lungo", 0, lunghi)
    }
}

/** Il testo della voce finta: serve solo a trovarla nell'albero. */
private const val VOCE = "Voce di prova"

/** Che cosa fa il tocco lungo, per il lettore di schermo. Vedi `MenuRow.holdLabel`. */
private const val TENUTA = "Tieni premuto"

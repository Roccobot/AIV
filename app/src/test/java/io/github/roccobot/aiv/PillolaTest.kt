package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The bench of the pill in place of the FAB on a phone held upright, new in 4.00.
 *
 * ⚠️⚠️ **IT IS BORN WITH THE FEATURE, AND THE RULE ASKS FOR IT** (`Rules.md` § '🧪 Quando si
 * scrive una prova, e quando no'): the pill is a surface that replaces another one, with keys
 * that are hidden behind a round key, a long press that changes meaning, and a fold that follows
 * the finger. In all of these the code can compile and the command not be there.
 *
 * ⚠️ **What it does not see**: the glass and the timing of the animation, which are rendering;
 * the tablet and Android 11 are seen only through the settings item, which is where they differ.
 */
@RunWith(AndroidJUnit4::class)
class PillolaTest {

    @get:Rule
    val banco = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private fun voce(id: Int) = app.getString(id)

    /** As in `NascosteTest`: without it the onboarding veil takes the first gesture. */
    @Before
    fun onboardingGiaVisto() {
        runBlocking {
            Hint.COLUMNS.remember(app)
            Hint.BIN_EMPTY.remember(app)
        }
    }

    /**
     * **Sliding: the round key opens the pill, a key acts and folds it, the × folds it too.**
     *
     * ⚠️ **The menu never opens**: a menu row has a written label, a pill key does not, so no node
     * with the text 'Impostazioni' may appear. A pill that opened the old menu as well would
     * pass every other check here.
     * ⚠️ **Folded, the keys are not commands**: they live behind the round key, and a tap that
     * reached them would act on something nobody sees.
     */
    @Test
    fun `a scorrimento il tasto tondo apre la pillola, e la x la richiude`() {
        var impostazioni = 0
        banco.setContent { Griglia(PhonePill.SLIDE, onSettings = { impostazioni++ }) }
        banco.waitForIdle()

        val chiave = voce(R.string.hub_settings)
        banco.onNodeWithContentDescription(chiave).assertIsNotEnabled()

        banco.onNodeWithContentDescription(voce(R.string.pick_actions)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(chiave).assertIsEnabled()
        assertTrue(
            "Il tasto tondo ha aperto anche il menu del FAB",
            banco.onAllNodesWithText(chiave).fetchSemanticsNodes().isEmpty()
        )

        banco.onNodeWithContentDescription(chiave).performClick()
        banco.waitForIdle()
        assertEquals("La voce della pillola non ha agito", 1, impostazioni)
        banco.onNodeWithContentDescription(chiave).assertIsNotEnabled()

        banco.onNodeWithContentDescription(voce(R.string.pick_actions)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(voce(R.string.pick_close)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(chiave).assertIsNotEnabled()
        assertEquals("La × ha agito come una voce", 1, impostazioni)
    }

    /** **Extended: no FAB, no menu, and a key acts at the first tap.** */
    @Test
    fun `estesa non ha il FAB, e le voci si toccano subito`() {
        var impostazioni = 0
        banco.setContent { Griglia(PhonePill.EXTENDED, onSettings = { impostazioni++ }) }
        banco.waitForIdle()

        assertTrue(
            "Con la pillola estesa il FAB è ancora in scena",
            banco.onAllNodesWithContentDescription(voce(R.string.pick_actions))
                .fetchSemanticsNodes().isEmpty()
        )
        banco.onNodeWithContentDescription(voce(R.string.hub_settings)).performClick()
        banco.waitForIdle()
        assertEquals("La voce della pillola estesa non ha agito", 1, impostazioni)
    }

    /**
     * **Extended, while scrolling: the pill folds to the two jump keys, top inside and bottom in
     * the corner** (answer C2).
     *
     * ⚠️ **The first entry must leave**: a pill that showed the two jumps beside 'Cerca' would
     * have the right labels and the wrong width, which is the half of the request about the fold.
     * ⚠️ **The clock is stopped**, as in `SaltiTest`, and for the same reason: an idle wait would
     * run out the jump's farewell and the pill would open again before the bench looks.
     */
    @Test
    fun `estesa scorrendo si piega sui due salti`() {
        banco.mainClock.autoAdvance = false
        banco.setContent { Griglia(PhonePill.EXTENDED) }
        banco.mainClock.advanceTimeBy(NASCITA)

        assertEquals("A riposo la pillola annuncia già un salto", 0, quanti(R.string.jump_bottom))
        assertEquals("A riposo 'Cerca' non è nella pillola", 1, quanti(R.string.hub_search))

        scorri()

        assertEquals("Scorrendo non compare 'Vai alla fine'", 1, quanti(R.string.jump_bottom))
        assertEquals("Scorrendo non compare 'Vai all'inizio'", 1, quanti(R.string.jump_top))
        assertEquals("Piegata, la pillola tiene ancora 'Cerca'", 0, quanti(R.string.hub_search))
    }

    /**
     * **A long press on a pill key shows its label, and does nothing else** (answers B1 and B3).
     *
     * ⚠️⚠️ **'Mostra nascoste' is the key with something to lose**: in the FAB's menu its long
     * press opens the hidden folders' panel, and in a pill it must not. The tooltip is the only
     * place where its label is written as text, so finding the text is finding the tooltip.
     */
    @Test
    @Config(shadows = [ArchivioAperto::class])
    fun `il tocco lungo su un tasto mostra l'etichetta e basta`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                CompositionLocalProvider(LocalPillLook provides PillLook(PhonePill.EXTENDED)) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Casa(CARTELLE, hidden = setOf("Segreta"))
                    }
                }
            }
        }
        banco.waitForIdle()

        val mostra = voce(R.string.hub_peek)
        assertTrue(
            "Prima del tocco lungo l'etichetta è già scritta",
            banco.onAllNodesWithText(mostra).fetchSemanticsNodes().isEmpty()
        )
        banco.onNodeWithContentDescription(mostra).performTouchInput { longClick() }
        banco.waitForIdle()

        assertTrue(
            "Il tocco lungo non ha mostrato l'etichetta del tasto",
            banco.onAllNodesWithText(mostra).fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            "Il tocco lungo nella pillola ha aperto il pannello delle cartelle nascoste",
            banco.onAllNodesWithText(voce(R.string.settings_hidden)).fetchSemanticsNodes().isEmpty()
        )
    }

    /**
     * **The settings item is on the phone, at the top of 'Pulsanti e indicatori', with both rows
     * of chips, and the glass from Android 12 on.**
     */
    @Test
    fun `la voce c'e sul telefono, col vetro`() {
        apriPulsanti()
        banco.onNodeWithText(voce(R.string.settings_phone_pill)).assertExists()
        banco.onNodeWithText(voce(R.string.pill_slide)).assertExists()
        banco.onNodeWithText(voce(R.string.pill_translucent)).assertExists()
        banco.onNodeWithText(voce(R.string.pill_glass)).assertExists()
    }

    /** **Below Android 12 the glass chip is not there** (the user's answer, 2026-10-05). */
    @Test
    @Config(sdk = [30])
    fun `sotto Android 12 il vetro non c'e`() {
        apriPulsanti()
        banco.onNodeWithText(voce(R.string.settings_phone_pill)).assertExists()
        banco.onNodeWithText(voce(R.string.pill_glass)).assertDoesNotExist()
    }

    /** **On a tablet the item is not there**: the tablet held upright has its own pill. */
    @Test
    @Config(qualifiers = "sw600dp-w600dp-h960dp")
    fun `sul tablet la voce non c'e`() {
        apriPulsanti()
        banco.onNodeWithText(voce(R.string.settings_hand)).assertExists()
        banco.onNodeWithText(voce(R.string.settings_phone_pill)).assertDoesNotExist()
    }

    private fun apriPulsanti() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                SettingsScreen(
                    settings = Settings(),
                    onChange = {},
                    onStartFolder = {},
                    onResetHints = {},
                    onChooseEditor = {},
                    onBack = {}
                )
            }
        }
        banco.waitForIdle()
        // ⚠️ Sul tablet il pannello ha due colonne e la stessa etichetta compare due volte: nella
        // colonna delle sezioni e nella pagina. La prima è la porta.
        banco.onAllNodesWithText(voce(R.string.settings_page_controls), substring = false)[0]
            .performScrollTo().performClick()
        banco.waitForIdle()
    }

    private fun quanti(id: Int): Int =
        banco.onAllNodesWithContentDescription(voce(id)).fetchSemanticsNodes().size

    /** The same drag as `SaltiTest`, for the same measured reasons, written there. */
    private fun scorri() {
        val scena = banco.onRoot().fetchSemanticsNode().size
        banco.onRoot().performTouchInput {
            down(Offset(scena.width * LATO, scena.height * DA))
            moveTo(Offset(scena.width * LATO, scena.height * A))
            advanceEventTime(FERMO)
            up()
        }
        banco.mainClock.advanceTimeBy(RESPIRO)
    }

    /**
     * A folder's grid with three entries in the pill: 'Cerca', 'Cestino' and 'Impostazioni'.
     *
     * ⚠️ **Three and not one**: with a single entry there is nothing to fold to the two jumps, and
     * the grid shows an entry only where it has somewhere to send.
     */
    @Composable
    private fun Griglia(mode: PhonePill, onSettings: () -> Unit = {}) {
        AivTheme(darkTheme = false) {
            CompositionLocalProvider(LocalPillLook provides PillLook(mode)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    GridScreen(
                        title = "Cartella di prova",
                        items = (1..60).map { Uri.parse("file:///finta/$it.jpg") },
                        highlight = null,
                        onOpen = {},
                        onBack = {},
                        onChanged = {},
                        onSearch = {},
                        onSearchHere = {},
                        onBin = {},
                        onSettings = onSettings
                    )
                }
            }
        }
    }
}

/** The folders of the long-press test: one is hidden, so 'Mostra nascoste' is in the pill. */
private val CARTELLE = (1..4).map {
    Folder.Bucket(
        id = it.toLong(),
        name = if (it == 1) "Segreta" else "Cartella $it",
        pictures = 2,
        clips = 0,
        cover = null,
        path = if (it == 1) "/storage/emulated/0/Segreta" else "/storage/emulated/0/Foto$it"
    )
}

/** As in `SaltiTest`: the first composition and its effects. */
private const val NASCITA = 1_000L

/** As in `SaltiTest`: shorter than the jump's quiet, so the pill does not open again. */
private const val RESPIRO = 100L

/** As in `SaltiTest`: longer than the velocity tracker's window, so the list does not fling. */
private const val FERMO = 300L

/** Where the drag starts and ends, as fractions of the screen's height. */
private const val DA = 0.8f
private const val A = 0.2f

/** Where the drag runs across, away from the pill in the corner. */
private const val LATO = 0.25f

package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
     * **'Elemento interattivo principale' is the first item of 'Pulsanti e indicatori', on the
     * phone, and with the pill chosen it shows the pill's two chips.** From 4.10 (his note E on the
     * 4.04 round); before, the pill was a switch last in 'Tema e dettagli grafici'.
     */
    @Test
    fun `l'elemento principale e in cima ai pulsanti, coi gettoni della pillola`() {
        apriPagina(R.string.settings_page_controls, Settings(mainControl = MainControl.PILL))
        banco.onNodeWithText(voce(R.string.main_control_fab)).assertExists()
        banco.onNodeWithText(voce(R.string.main_control_pill)).assertExists()
        banco.onNodeWithText(voce(R.string.pill_slide)).assertExists()
        banco.onNodeWithText(voce(R.string.pill_extended)).assertExists()
        // First in the page: above 'Lato preferito', the item that was first.
        val sotto = banco.onNodeWithText(voce(R.string.settings_hand)).fetchSemanticsNode().positionInRoot
        val prima = banco.onNodeWithText(voce(R.string.settings_main_control)).fetchSemanticsNode().positionInRoot
        assertTrue("L'elemento principale non è in cima alla pagina", prima.y < sotto.y)
    }

    /**
     * **With the FAB the pill's chips are not there** (*'a scomparsa' solo per pillola e menu
     * inferiore*), and choosing the pill writes the main control and leaves the chosen pill alone.
     */
    @Test
    fun `col FAB i gettoni della pillola non ci sono, e la pillola si sceglie`() {
        var scritte: Settings? = null
        apriPagina(R.string.settings_page_controls, Settings(phonePill = PhonePill.EXTENDED)) { scritte = it }
        banco.onNodeWithText(voce(R.string.settings_main_control)).assertExists()
        banco.onNodeWithText(voce(R.string.pill_slide)).assertDoesNotExist()
        banco.onNodeWithText(voce(R.string.main_control_pill)).performClick()
        banco.waitForIdle()
        assertEquals(MainControl.PILL, scritte?.mainControl)
        assertEquals(PhonePill.EXTENDED, scritte?.phonePill)
    }

    /**
     * **An archive written before 4.10 keeps meaning what it meant**: the switch of 4.02-4.05 and,
     * before it, the chip of 4.00-4.01 (`off` is the FAB, the two pills are the pill). The new key
     * wins over both.
     */
    @Test
    fun `gli archivi di prima della 4_10 si rileggono con l'elemento principale`() {
        val gettone = stringPreferencesKey("phone-pill")
        val interruttore = booleanPreferencesKey("phone-pill-on")
        val elemento = stringPreferencesKey("main-control")
        val spenta = SettingsStore.read(mutablePreferencesOf(gettone to "off"))
        assertEquals(MainControl.FAB, spenta.mainControl)
        assertEquals(PhonePill.SLIDE, spenta.phonePill)
        val estesa = SettingsStore.read(mutablePreferencesOf(gettone to "extended"))
        assertEquals(MainControl.PILL, estesa.mainControl)
        assertEquals(PhonePill.EXTENDED, estesa.phonePill)
        val accesa = SettingsStore.read(mutablePreferencesOf(interruttore to true, gettone to "slide"))
        assertEquals(MainControl.PILL, accesa.mainControl)
        val nuova = SettingsStore.read(mutablePreferencesOf(interruttore to true, elemento to "fab"))
        assertEquals(MainControl.FAB, nuova.mainControl)
        val vuoto = SettingsStore.read(emptyPreferences())
        assertEquals(Settings().mainControl, vuoto.mainControl)
        assertEquals(Settings().phonePill, vuoto.phonePill)
    }

    /**
     * **'Aspetto dei pulsanti principali' is last in 'Tema e dettagli grafici', with the glass from
     * Android 12 on, and 'Vetro' is now 'Traslucido'.** From 4.10 (his note E).
     */
    @Test
    fun `l'aspetto dei pulsanti e in fondo al tema, col traslucido`() {
        apriPagina(R.string.settings_page_look)
        banco.onNodeWithText(voce(R.string.settings_button_look)).assertExists()
        banco.onNodeWithText(voce(R.string.pill_solid)).assertExists()
        banco.onNodeWithText(voce(R.string.pill_translucent)).assertExists()
        banco.onNodeWithText(voce(R.string.pill_glass)).assertExists()
        val sopra = banco.onNodeWithText(voce(R.string.settings_depth)).fetchSemanticsNode().positionInRoot
        val aspetto = banco.onNodeWithText(voce(R.string.settings_button_look)).fetchSemanticsNode().positionInRoot
        assertTrue("L'aspetto dei pulsanti non è in fondo alla pagina", aspetto.y > sopra.y)
    }

    /**
     * **In the round key of the pill the mark is 22dp wide, and its A sits in the middle** (the
     * user's note on the 4.01 round, and `4.02-03` not approved: on the phone nothing changed).
     *
     * ⚠️⚠️ **It mounts the real pill and not a copy**: the 4.02 version of this test gave the mark
     * its round-key flag by hand, and passed while the pill never gave it.
     */
    @Test
    // ⚠️ At a phone's density: at 1x the offset rounds to whole pixels, and 1.5dp becomes one.
    @Config(qualifiers = "xxhdpi")
    fun `nel tasto tondo il marchio e piu piccolo e centrato sulla A`() {
        banco.setContent { Griglia(PhonePill.SLIDE) }
        banco.waitForIdle()
        val tasto = banco.onNodeWithContentDescription(voce(R.string.pick_actions))
            .fetchSemanticsNode().boundsInRoot
        val segno = banco.onAllNodesWithTag(MARK_TAG, useUnmergedTree = true).fetchSemanticsNodes()
            .map { it.boundsInRoot }
            .single { tasto.contains(it.center) }
        val dp = app.resources.displayMetrics.density
        assertEquals(22f, segno.width / dp, 0.5f)
        // The A spans the whole width and 6..60 of the 60-unit height, so its box is centred.
        assertEquals(tasto.center.x, segno.center.x, 0.5f * dp)
        // And from 4.05 it sits 1.5dp higher still, his optical correction.
        assertEquals(tasto.center.y - 1.5f * dp, segno.top + segno.height * 33f / 60f, 0.5f * dp)
    }

    /** **Below Android 12 the glass chip is not there** (the user's answer, 2026-10-05). */
    @Test
    @Config(sdk = [30])
    fun `sotto Android 12 il traslucido non c'e`() {
        apriPagina(R.string.settings_page_look)
        banco.onNodeWithText(voce(R.string.settings_button_look)).assertExists()
        banco.onNodeWithText(voce(R.string.pill_glass)).assertDoesNotExist()
    }

    /**
     * **On a tablet the main control is not there** (the tablet held upright has its own pill), and
     * the look of the main buttons is, because the FAB and the wide pills are there too.
     */
    @Test
    @Config(qualifiers = "sw600dp-w600dp-h960dp")
    fun `sul tablet l'elemento principale non c'e, l'aspetto si`() {
        apriPagina(R.string.settings_page_controls)
        banco.onAllNodesWithText(voce(R.string.settings_hand), substring = false)[0].assertExists()
        banco.onNodeWithText(voce(R.string.settings_main_control)).assertDoesNotExist()
        // ⚠️ One `setContent` per test: the second page opens from the column of sections.
        banco.onAllNodesWithText(voce(R.string.settings_page_look), substring = false)[0]
            .performScrollTo().performClick()
        banco.waitForIdle()
        banco.onAllNodesWithText(voce(R.string.settings_button_look), substring = false)[0].assertExists()
    }

    private fun apriPagina(pagina: Int, settings: Settings = Settings(), onChange: (Settings) -> Unit = {}) {
        banco.setContent {
            AivTheme(darkTheme = false) {
                SettingsScreen(
                    settings = settings,
                    onChange = onChange,
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
        banco.onAllNodesWithText(voce(pagina), substring = false)[0]
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

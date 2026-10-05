package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.math.abs

/*
 * The 4.15 round: the bottom menu, the pill with seven entries, and the mirrored order on the left.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️ Every scene mounts the real screens and the real pill: a copy beside them passed with the
 * defect inside once (`PillolaTest`, the mark of 4.02).
 */
@RunWith(AndroidJUnit4::class)
class MenuInferioreTest {

    @get:Rule
    val banco = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private fun voce(id: Int) = app.getString(id)

    /** As in `PillolaTest`: without it the onboarding veil takes the first gesture. */
    @Before
    fun onboardingGiaVisto() {
        runBlocking {
            Hint.COLUMNS.remember(app)
            Hint.BIN_EMPTY.remember(app)
        }
    }

    /**
     * **The two views keep their order when the pill is mirrored, and the rest is reversed** (his
     * order for the left side: *Impostazioni, Cestino, Apri un indirizzo, Cerca, Mostra nascoste,
     * Altra vista 1, Altra vista 2*).
     */
    @Test
    fun `a sinistra l'ordine e il rovescio, con le due viste nel loro ordine`() {
        fun e(name: String, run: Int? = null) = PillEntry(Icons.Default.Close, name, run = run) {}
        val destra = listOf(
            e("Vista 1", 0), e("Vista 2", 0), e("Nascoste"), e("Cerca"), e("Indirizzo"),
            e("Cestino"), e("Impostazioni")
        )
        assertEquals(
            listOf("Impostazioni", "Cestino", "Indirizzo", "Cerca", "Nascoste", "Vista 1", "Vista 2"),
            mirrored(destra).map { it.label }
        )
    }

    /**
     * **On the left the extended pill reads from 'Impostazioni' to the views, on the real home.**
     *
     * ⚠️ **Measured on the positions**: the order of the semantics tree is the order of the code,
     * and a pill drawn right to left would read the same there.
     */
    @Test
    @Config(shadows = [ArchivioAperto::class])
    fun `col lato sinistro la pillola comincia da Impostazioni`() {
        banco.setContent { Home(PillLook(PhonePill.EXTENDED), Hand.LEFT) }
        banco.waitForIdle()
        val x = { id: Int -> banco.onNodeWithContentDescription(voce(id)).fetchSemanticsNode().positionInRoot.x }
        assertTrue("A sinistra Impostazioni non è prima del Cestino", x(R.string.hub_settings) < x(R.string.bin_title))
        assertTrue("A sinistra il Cestino non è prima di Cerca", x(R.string.bin_title) < x(R.string.hub_search))
        assertTrue("A sinistra Cerca non è prima di Mostra nascoste", x(R.string.hub_search) < x(R.string.hub_peek))
    }

    /**
     * **With seven entries the extended pill spans the row, and 'Cerca' is on the centre line** (his
     * rule P1, only for the extended pill).
     */
    @Test
    // ⚠️ A real phone's width: on the bench's default the row is so narrow that seven keys of 44dp
    // fill it anyway, and the test passed with the rule taken out.
    @Config(shadows = [ArchivioAperto::class], qualifiers = "w411dp-h891dp")
    fun `con sette voci la pillola estesa mette Cerca al centro`() {
        banco.setContent { Home(PillLook(PhonePill.EXTENDED), Hand.RIGHT) }
        banco.waitForIdle()
        assertEquals(
            "La pillola della prova non ha sette voci",
            PILL_FULL,
            listOf(R.string.hub_search, R.string.hub_url, R.string.bin_title, R.string.hub_settings, R.string.hub_peek)
                .sumOf { banco.onAllNodesWithContentDescription(voce(it)).fetchSemanticsNodes().size } + 2
        )
        val cerca = banco.onNodeWithContentDescription(voce(R.string.hub_search)).fetchSemanticsNode()
        val centro = cerca.positionInRoot.x + cerca.size.width / 2f
        val scena = banco.onRoot().fetchSemanticsNode().size.width / 2f
        assertTrue("'Cerca' è a ${centro - scena}px dal centro", abs(centro - scena) < 1.5f)
    }

    /**
     * **The sliding bottom menu: at rest a vertical pill with 'in cima' and the mark, and a tap on
     * the mark brings up the bar with every entry and its ×; the × folds it again.**
     */
    @Test
    fun `il menu inferiore a scomparsa si apre dalla pillola verticale e si richiude`() {
        var impostazioni = 0
        banco.setContent { Griglia(PillLook(PhonePill.SLIDE, bar = true), onSettings = { impostazioni++ }) }
        banco.waitForIdle()

        banco.onNodeWithContentDescription(voce(R.string.jump_top)).assertExists()
        assertTrue(
            "A riposo la barra è già in scena",
            banco.onAllNodesWithContentDescription(voce(R.string.hub_settings)).fetchSemanticsNodes().isEmpty()
        )
        assertFalse("A riposo la linea dei gesti è già bianca", BarStage.under)

        banco.onNodeWithContentDescription(voce(R.string.pick_actions)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(voce(R.string.hub_settings)).assertIsEnabled()
        assertTrue("Aperto, il menu non è sotto la linea dei gesti", BarStage.under)
        val barra = banco.onNodeWithContentDescription(voce(R.string.hub_settings)).fetchSemanticsNode()
        val scena = banco.onRoot().fetchSemanticsNode().size
        assertTrue("La barra non è in fondo allo schermo", barra.positionInRoot.y > scena.height * 0.8f)

        banco.onNodeWithContentDescription(voce(R.string.hub_settings)).performClick()
        banco.waitForIdle()
        assertEquals("La voce del menu inferiore non ha agito", 1, impostazioni)
        assertTrue(
            "La voce non ha richiuso il menu",
            banco.onAllNodesWithContentDescription(voce(R.string.hub_settings)).fetchSemanticsNodes().isEmpty()
        )
    }

    /** **Fixed, the bar is always there, without × and without the vertical pill.** */
    @Test
    fun `il menu inferiore fisso e sempre aperto e senza x`() {
        banco.setContent { Griglia(PillLook(PhonePill.EXTENDED, bar = true)) }
        banco.waitForIdle()
        banco.onNodeWithContentDescription(voce(R.string.hub_settings)).assertIsEnabled()
        banco.onNodeWithContentDescription(voce(R.string.hub_search)).assertIsEnabled()
        assertTrue(
            "Il menu fisso ha una ×",
            banco.onAllNodesWithContentDescription(voce(R.string.pick_close)).fetchSemanticsNodes().isEmpty()
        )
        assertTrue("Il menu fisso non è sotto la linea dei gesti", BarStage.under)
    }

    /**
     * **Folded, while scrolling: the upper key is still 'in cima' and the mark turns into 'in fondo'**
     * (answer M2). **Open, the two keys in the corner turn into the two arrows.**
     *
     * ⚠️ **The clock is stopped**, as in `PillolaTest`, so the jump's farewell does not run out
     * before the bench looks.
     */
    @Test
    fun `scorrendo i due tasti nell'angolo diventano i salti`() {
        banco.mainClock.autoAdvance = false
        banco.setContent { Griglia(PillLook(PhonePill.EXTENDED, bar = true)) }
        banco.mainClock.advanceTimeBy(NASCITA)
        assertEquals("A riposo il menu annuncia già un salto", 0, quanti(R.string.jump_bottom))

        scorri()

        assertEquals("Scorrendo non compare 'Vai alla fine'", 1, quanti(R.string.jump_bottom))
        assertEquals("Scorrendo non compare 'Vai all'inizio'", 1, quanti(R.string.jump_top))
        assertEquals("Scorrendo Impostazioni è ancora nell'angolo", 0, quanti(R.string.hub_settings))
        assertEquals("Scorrendo Cerca se n'è andata", 1, quanti(R.string.hub_search))
    }

    /** The same for the folded pill: the mark becomes 'in fondo', and 'in cima' stays above. */
    @Test
    fun `scorrendo la pillola verticale diventa in cima e in fondo`() {
        banco.mainClock.autoAdvance = false
        banco.setContent { Griglia(PillLook(PhonePill.SLIDE, bar = true)) }
        banco.mainClock.advanceTimeBy(NASCITA)
        assertEquals(0, quanti(R.string.jump_bottom))
        assertEquals(1, quanti(R.string.jump_top))

        scorri()

        assertEquals("Scorrendo il marchio non diventa 'Vai alla fine'", 1, quanti(R.string.jump_bottom))
        assertEquals(1, quanti(R.string.jump_top))
    }

    /**
     * **'Menu inferiore' is the third chip, and with it the second row says `Fisso`.** The chosen
     * value is the same as the pill's: the menu does not ask the question twice.
     */
    @Test
    fun `il gettone del menu inferiore c'e e la seconda fila dice Fisso`() {
        var scritte: Settings? = null
        banco.setContent {
            AivTheme(darkTheme = false) {
                SettingsScreen(
                    settings = Settings(mainControl = MainControl.BOTTOM, phonePill = PhonePill.EXTENDED),
                    onChange = { scritte = it },
                    onStartFolder = {},
                    onResetHints = {},
                    onChooseEditor = {},
                    onBack = {}
                )
            }
        }
        banco.waitForIdle()
        banco.onAllNodesWithText(voce(R.string.settings_page_controls), substring = false)[0]
            .performScrollTo().performClick()
        banco.waitForIdle()
        banco.onNodeWithText(voce(R.string.main_control_bar)).assertExists()
        banco.onNodeWithText(voce(R.string.bar_fixed)).assertExists()
        banco.onNodeWithText(voce(R.string.pill_extended)).assertDoesNotExist()
        banco.onNodeWithText(voce(R.string.main_control_pill)).performClick()
        banco.waitForIdle()
        assertEquals(MainControl.PILL, scritte?.mainControl)
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

    /** The home with one hidden folder, so the pill has its seven entries. */
    @Composable
    private fun Home(look: PillLook, hand: Hand) {
        AivTheme(darkTheme = false) {
            CompositionLocalProvider(LocalPillLook provides look, LocalPadLook provides PadLook(hand = hand)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Casa(CARTELLE, hidden = setOf("Segreta"))
                }
            }
        }
    }

    /** A folder's grid with 'Cerca', 'Cestino' and 'Impostazioni', as in `PillolaTest`. */
    @Composable
    private fun Griglia(look: PillLook, onSettings: () -> Unit = {}) {
        AivTheme(darkTheme = false) {
            CompositionLocalProvider(LocalPillLook provides look) {
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

/** The folders of the home: one is hidden, so 'Mostra nascoste' is in the pill. */
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

/** As in `SaltiTest`. */
private const val FERMO = 300L

/** As in `SaltiTest`. */
private const val DA = 0.8f

/** As in `SaltiTest`. */
private const val A = 0.2f

/** As in `SaltiTest`. */
private const val LATO = 0.25f

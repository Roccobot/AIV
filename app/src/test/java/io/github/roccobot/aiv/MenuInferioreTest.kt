package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
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
 * Since 4.20 the corner menu, and since 4.25 its round rest and switch, the 24dp margins (A2) and
 * the glass's two colours.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️ Every scene mounts the real screens and the real pill: a copy beside them passed with the
 * defect inside once (`PillolaTest`, the mark of 4.02).
 */
@RunWith(AndroidJUnit4::class)
class MenuInferioreTest {

    /** The new rule, since 4.98: see [aspettaIlVelo] for why the old one let the hint fall. */
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
     * **On the left the order is the exact mirror, the two views included** (his comment on
     * `4.15-04`: *volevo specchiati anche quei due*).
     */
    @Test
    fun `a sinistra l'ordine e il rovescio, viste comprese`() {
        fun e(name: String) = PillEntry(Icons.Default.Close, name) {}
        val destra = listOf("Vista 1", "Vista 2", "Nascoste", "Cerca", "Indirizzo", "Cestino", "Impostazioni").map { e(it) }
        assertEquals(
            listOf("Impostazioni", "Cestino", "Indirizzo", "Cerca", "Nascoste", "Vista 2", "Vista 1"),
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
     * **The bottom menu's keys sit 8dp into the navigation inset** (note B on the 4.32 round: *la
     * modalità Menu deve avere il menu più basso di 7/8dp*).
     *
     * ⚠️ The bench has no system bars: the inset is handed to the scene's view, as in
     * `BarraInfoTest`.
     */
    @Test
    fun `il menu basso scende di 8dp dentro la barra di navigazione`() {
        var vista: android.view.View? = null
        banco.setContent {
            vista = androidx.compose.ui.platform.LocalView.current
            Griglia(PillLook(PhonePill.EXTENDED, bar = true))
        }
        banco.waitForIdle()
        val dp = app.resources.displayMetrics.density
        banco.runOnUiThread {
            val spazi = androidx.core.view.WindowInsetsCompat.Builder()
                .setInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars(), androidx.core.graphics.Insets.of(0, 0, 0, (48 * dp).toInt()))
                .setVisible(androidx.core.view.WindowInsetsCompat.Type.navigationBars(), true)
                .build()
            androidx.core.view.ViewCompat.dispatchApplyWindowInsets(vista!!, spazi)
        }
        banco.waitForIdle()
        val scena = banco.onRoot().fetchSemanticsNode().boundsInRoot
        val tasto = banco.onNodeWithContentDescription(voce(R.string.hub_settings)).fetchSemanticsNode().boundsInRoot
        val fila = tasto.center.y + 28f * dp
        assertEquals("La fila dei tasti non scende di 8dp nella barra di navigazione", scena.bottom - 40f * dp, fila, 1f * dp)
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

    /**
     * **With three keys the fixed bar gathers them on the preferred side** (note N2 on the 4.15
     * round: *se sono 2, 3 o 4 devono stare sul lato preferito*), the corner key [PILL_SIDE] from the
     * edge: 16dp until 4.20, 24dp since 4.25 (his choice A2).
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `con tre tasti il menu inferiore li raccoglie sul lato preferito`() {
        banco.setContent { Griglia(PillLook(PhonePill.EXTENDED, bar = true)) }
        banco.waitForIdle()
        val scena = banco.onRoot().fetchSemanticsNode().size.width
        val cerca = banco.onNodeWithContentDescription(voce(R.string.hub_search)).fetchSemanticsNode()
        val impostazioni = banco.onNodeWithContentDescription(voce(R.string.hub_settings)).fetchSemanticsNode()
        assertTrue("'Cerca' è ancora a sinistra del centro", cerca.positionInRoot.x > scena / 2f)
        val dp = app.resources.displayMetrics.density
        val margine = scena - (impostazioni.positionInRoot.x + impostazioni.size.width)
        assertEquals("Il tasto d'angolo non è a 24dp dal bordo", 24f * dp, margine, 1.5f * dp)
    }

    /**
     * **The corner menu in a folder: the folded pill opens a 2x2, with Impostazioni and the × in the
     * bottom row and the × in the corner** (decisions G1 and C3). With the pill at rest, chosen from
     * the switch since 4.25: the round rest has its own tests in `MenuAngolareTest`.
     */
    @Test
    // ⚠️ In Italian: the swap follows the longest label, and in English that is the bin's.
    @Config(qualifiers = "it")
    fun `il menu angolare nelle cartelle e un 2x2 con la x nell'angolo`() {
        banco.setContent { Griglia(PillLook(PhonePill.SLIDE, corner = true, cornerRound = false)) }
        banco.waitForIdle()
        banco.onNodeWithContentDescription(voce(R.string.jump_top)).assertExists()
        banco.onNodeWithContentDescription(voce(R.string.pick_actions)).performClick()
        banco.waitForIdle()
        val pos = { id: Int -> banco.onNodeWithContentDescription(voce(id)).fetchSemanticsNode().positionInRoot }
        val chiudi = pos(R.string.pick_close)
        val impostazioni = pos(R.string.hub_settings)
        val cerca = pos(R.string.hub_search)
        val cestino = pos(R.string.bin_title)
        // ⚠️ Since 4.35 'Impostazioni', the longest label, is never in the bottom row's rounded
        // corner (item `4.34-01`): it swaps with 'Cestino'.
        assertEquals("Cestino e la × non sono sulla stessa riga", cestino.y, chiudi.y, 1f)
        assertTrue("La × non è nell'angolo", chiudi.x > cestino.x)
        assertEquals("Cerca e Impostazioni non sono sulla stessa riga", cerca.y, impostazioni.y, 1f)
        assertTrue("La riga di Cerca non è sopra", cerca.y < cestino.y)
        assertFalse("Il menu angolare colora la linea dei gesti", BarStage.under)
        banco.onNodeWithContentDescription(voce(R.string.pick_close)).performClick()
        banco.waitForIdle()
        assertTrue(
            "La × non ha richiuso il menu angolare",
            banco.onAllNodesWithContentDescription(voce(R.string.hub_settings)).fetchSemanticsNodes().isEmpty()
        )
    }

    /**
     * **In the home the corner menu is a 3x3: on top the switch and the two other views, and the ×
     * under the thumb** (decisions C1 and C2, and since 4.25 `4.20-01`: *non si vedono più tutte e
     * tre le viste con la selezionata*), mirrored on the left.
     */
    @Test
    @Config(shadows = [ArchivioAperto::class])
    fun `nella home il menu angolare e un 3x3 col commutatore e le due viste`() {
        banco.setContent { Home(PillLook(PhonePill.SLIDE, corner = true), Hand.LEFT) }
        banco.waitForIdle()
        val tondo = banco.onNodeWithContentDescription(voce(R.string.hub_open)).fetchSemanticsNode().boundsInRoot
        banco.onNodeWithContentDescription(voce(R.string.hub_open)).performClick()
        banco.waitForIdle()
        val pos = { id: Int -> banco.onNodeWithContentDescription(voce(id)).fetchSemanticsNode().positionInRoot }
        assertEquals("La vista in cui si è è ancora nel menu", 0, quanti(R.string.hub_view_grid))
        // ⚠️ Since 4.33 (note F on the 4.32 round): a short label under every icon but the ×, and
        // the ×, in a larger cell, still centred on the round key.
        for (id in listOf(R.string.start_pill, R.string.start_list, R.string.start_tree, R.string.start_show, R.string.start_url)) {
            banco.onNodeWithText(voce(id), useUnmergedTree = true).assertExists()
        }
        val x = banco.onNodeWithContentDescription(voce(R.string.pick_close)).fetchSemanticsNode().boundsInRoot
        val dp = app.resources.displayMetrics.density
        assertEquals("Il tasto della × non è di 64dp", 64f * dp, x.width, 0.5f * dp)
        assertEquals("La × non è sul centro del tondo in orizzontale", tondo.center.x, x.center.x, 0.5f * dp)
        assertEquals("La × non è sul centro del tondo in verticale", tondo.center.y, x.center.y, 0.5f * dp)
        val commutatore = pos(R.string.corner_rest_pill)
        val lista = pos(R.string.hub_view_list)
        val albero = pos(R.string.hub_view_tree)
        assertEquals("Il commutatore non è sulla riga delle viste", commutatore.y, albero.y, 1f)
        assertEquals(commutatore.y, lista.y, 1f)
        assertTrue("A sinistra la prima riga non è a specchio", albero.x < lista.x && lista.x < commutatore.x)
        val chiudi = pos(R.string.pick_close)
        assertTrue("A sinistra la × non è nell'angolo", chiudi.x < pos(R.string.hub_settings).x)
        assertTrue("La × non è nell'ultima riga", chiudi.y > pos(R.string.hub_search).y)
        assertEquals("La riga di Cerca non ha anche Mostra nascoste", pos(R.string.hub_search).y, pos(R.string.hub_peek).y, 1f)
    }

    /**
     * **With the translucent look the four sliders and the live preview appear** (note N1 and his
     * correction: *serve un elemento traslucido che si aggiorna in tempo reale*), and not otherwise.
     */
    @Test
    fun `col traslucido compaiono i cursori e i due colori`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                SettingsScreen(
                    settings = Settings(pillFill = PillFill.GLASS),
                    onChange = {},
                    onStartFolder = {},
                    onResetHints = {},
                    onChooseEditor = {},
                    onBack = {}
                )
            }
        }
        banco.waitForIdle()
        banco.onAllNodesWithText(voce(R.string.settings_page_look), substring = false)[0]
            .performScrollTo().performClick()
        banco.waitForIdle()
        listOf(
            R.string.glass_radius, R.string.glass_intensity, R.string.glass_tint, R.string.glass_light,
            R.string.glass_colour_light, R.string.glass_colour_dark
        ).forEach {
            banco.onNodeWithText(voce(it), substring = true).assertExists()
        }
    }

    /** **The corner menu's chip is there, and with it the second row is not** (always sliding). */
    @Test
    fun `il gettone del menu angolare c'e e non ha la seconda fila`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                SettingsScreen(
                    settings = Settings(mainControl = MainControl.CORNER),
                    onChange = {},
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
        banco.onNodeWithText(voce(R.string.main_control_corner)).assertExists()
        banco.onNodeWithText(voce(R.string.pill_slide)).assertDoesNotExist()
    }

    /**
     * **At rest the corner menu is a single round key, and while scrolling it stretches into the
     * vertical pill of the two jumps** (`4.20-01`, R2 and R4, the factory rest since 4.25).
     *
     * ⚠️ **The clock is stopped**, as in the other jump tests.
     */
    @Test
    fun `col tondo a riposo il menu angolare scorrendo diventa la pillola dei salti`() {
        banco.mainClock.autoAdvance = false
        banco.setContent { Griglia(PillLook(PhonePill.SLIDE, corner = true)) }
        banco.mainClock.advanceTimeBy(NASCITA)
        assertEquals("A riposo il tondo ha già 'in cima' sopra di sé", 0, quanti(R.string.jump_top))
        assertEquals(1, quanti(R.string.pick_actions))

        scorri()

        assertEquals("Scorrendo non compare 'in cima'", 1, quanti(R.string.jump_top))
        assertEquals("Scorrendo il tondo non diventa 'in fondo'", 1, quanti(R.string.jump_bottom))
    }

    /**
     * **The switch in the home's corner menu writes the rest and closes the menu** (`4.20-01`, R1 and
     * R3: *il menu angolare deve chiudersi dopo ogni interazione*), and with the pill at rest it
     * offers the round key back.
     */
    @Test
    @Config(shadows = [ArchivioAperto::class])
    fun `il commutatore cambia il riposo e chiude il menu angolare`() {
        var cambi = 0
        var look by mutableStateOf(PillLook(PhonePill.SLIDE, corner = true))
        banco.setContent { Home(look, Hand.RIGHT, onCornerRest = { cambi++ }) }
        banco.waitForIdle()
        banco.onNodeWithContentDescription(voce(R.string.hub_open)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(voce(R.string.corner_rest_pill)).performClick()
        banco.waitForIdle()
        assertEquals("Il commutatore non ha scritto il riposo", 1, cambi)
        assertEquals("Il commutatore non ha chiuso il menu", 0, quanti(R.string.hub_search))

        look = look.copy(cornerRound = false)
        banco.waitForIdle()
        banco.onNodeWithContentDescription(voce(R.string.hub_open)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(voce(R.string.corner_rest_round)).assertExists()
    }

    /**
     * **The open sliding pill spans the row between two margins of 24dp, and the round key sits on
     * its edge** (note A on the 4.20 round, his choice A2 after the preview).
     */
    @Test
    // ⚠️ A real phone's width, for the reason written on the test of 'Cerca' in the centre.
    @Config(shadows = [ArchivioAperto::class], qualifiers = "w411dp-h891dp")
    fun `aperta la pillola a scomparsa arriva ai due margini di 24dp`() {
        banco.setContent { Home(PillLook(PhonePill.SLIDE), Hand.RIGHT) }
        banco.waitForIdle()
        val dp = app.resources.displayMetrics.density
        val scena = banco.onRoot().fetchSemanticsNode().size.width
        val tondo = banco.onNodeWithContentDescription(voce(R.string.hub_open)).fetchSemanticsNode()
        val aria = scena - (tondo.positionInRoot.x + tondo.size.width)
        assertEquals("A riposo il tondo non è a 24dp dal bordo", 24f * dp, aria, 1.5f * dp)

        banco.onNodeWithContentDescription(voce(R.string.hub_open)).performClick()
        banco.waitForIdle()
        // ⚠️ Since 4.34 the row keeps 6dp from both ends of the pill (note A on the 4.33 round).
        val cella = (scena - 60f * dp) / 8f
        val centro = { id: Int ->
            banco.onNodeWithContentDescription(voce(id)).fetchSemanticsNode().let { it.positionInRoot.x + it.size.width / 2f }
        }
        // ⚠️ The far end may be off by half a pixel per cell, rounded to the pixel: the corner is exact.
        assertEquals("La prima voce non è nella prima cella dopo 30dp", 30f * dp + cella / 2f, centro(R.string.hub_view_list), 0.5f * dp + 4f)
        assertEquals("La × non è nell'ultima cella prima dei 30dp", scena - 30f * dp - cella / 2f, centro(R.string.pick_close), 0.5f * dp)
    }

    /**
     * **Open, every key of the sliding pill is centred in height on the ×** (his note of 2026-10-06,
     * with a screenshot: *l'unica icona centrata verticalmente sulla pillola è la × ... tutte le
     * altre appaiono molto più in alto*). With eight entries on a narrow phone the keys shrink
     * under 44dp, and the row put them at its top.
     * ⚠️ **On a narrow phone on purpose**: at 411dp the keys lose a tenth of a dp and the defect
     * is under a pixel; at 360dp they lose more than 4dp.
     */
    @Test
    @Config(shadows = [ArchivioAperto::class], qualifiers = "w360dp-h800dp")
    fun `aperta la pillola a scomparsa ogni tasto e centrato in altezza sulla x`() {
        banco.setContent { Home(PillLook(PhonePill.SLIDE), Hand.RIGHT) }
        banco.waitForIdle()
        banco.onNodeWithContentDescription(voce(R.string.hub_open)).performClick()
        banco.waitForIdle()
        val centro = { id: Int ->
            banco.onNodeWithContentDescription(voce(id)).fetchSemanticsNode().boundsInRoot.center.y
        }
        val x = centro(R.string.pick_close)
        for (id in listOf(R.string.hub_view_list, R.string.hub_search, R.string.hub_settings)) {
            assertEquals("'${voce(id)}' non è centrato in altezza sulla ×", x, centro(id), 0.5f)
        }
    }

    /**
     * **With the sliding pill open, the two keys beside the × turn into the jump while scrolling**
     * (note B on the 4.33 round: *come accade nelle altre modalità. Anche nelle cartelle e nel
     * cestino*). Until 4.33 the open pill had no jump.
     */
    @Test
    fun `aperta la pillola a scomparsa scorrendo i tasti accanto alla x diventano i salti`() {
        banco.mainClock.autoAdvance = false
        banco.setContent { Griglia(PillLook(PhonePill.SLIDE)) }
        banco.mainClock.advanceTimeBy(NASCITA)
        banco.onNodeWithContentDescription(voce(R.string.pick_actions)).performClick()
        banco.mainClock.advanceTimeBy(NASCITA)
        assertEquals("La pillola non si è aperta", 1, quanti(R.string.pick_close))
        assertEquals("Aperta e ferma, la pillola annuncia già un salto", 0, quanti(R.string.jump_top) + quanti(R.string.jump_bottom))

        scorri()

        assertEquals("La pillola si è chiusa scorrendo", 1, quanti(R.string.hub_search))
        assertEquals("Scorrendo non compare 'Vai all'inizio'", 1, quanti(R.string.jump_top))
        assertEquals("Scorrendo non compare 'Vai alla fine'", 1, quanti(R.string.jump_bottom))
        // ⚠️ Since 4.35 the two keys nearest the corner, × included (item `4.34-04`): the × is
        // 'in fondo' and the key beside it 'in cima'.
        assertEquals("Scorrendo la × è ancora la ×", 0, quanti(R.string.pick_close))
        val giu = banco.onNodeWithContentDescription(voce(R.string.jump_bottom)).fetchSemanticsNode().boundsInRoot
        val su = banco.onNodeWithContentDescription(voce(R.string.jump_top)).fetchSemanticsNode().boundsInRoot
        val cerca = banco.onNodeWithContentDescription(voce(R.string.hub_search)).fetchSemanticsNode().boundsInRoot
        assertTrue("'In fondo' non è nell'angolo", giu.left > su.left && su.left > cerca.left)
    }

    /**
     * **The open Start menu is square, in the home and in a folder** (item `4.34-01`: *fa' in modo
     * che il menu sia sempre quadrato*). In 4.34 it was 222 by 206dp in the home.
     */
    @Test
    @Config(shadows = [ArchivioAperto::class])
    fun `il menu Start aperto e quadrato`() {
        var casa by mutableStateOf(true)
        banco.setContent {
            if (casa) Home(PillLook(PhonePill.SLIDE, corner = true), Hand.RIGHT)
            else Griglia(PillLook(PhonePill.SLIDE, corner = true))
        }
        banco.waitForIdle()
        val dp = app.resources.displayMetrics.density
        fun pannello(): androidx.compose.ui.geometry.Rect =
            banco.onAllNodesWithTag(START_PANEL_TAG, useUnmergedTree = true).fetchSemanticsNodes().single().boundsInRoot
        banco.onNodeWithContentDescription(voce(R.string.hub_open)).performClick()
        banco.waitForIdle()
        val inCasa = pannello()
        assertEquals("In home il menu Start non è quadrato", inCasa.width, inCasa.height, 0.5f * dp)
        banco.onNodeWithContentDescription(voce(R.string.pick_close)).performClick()
        casa = false
        banco.waitForIdle()
        banco.onNodeWithContentDescription(voce(R.string.pick_actions)).performClick()
        banco.waitForIdle()
        val inCartella = pannello()
        assertEquals("In cartella il menu Start non è quadrato", inCartella.width, inCartella.height, 0.5f * dp)
    }

    /**
     * **A long press on 'Mostra' opens the hidden folders and closes the menu** (item `4.34-01`:
     * *possiamo rimettere la scorciatoia alle cartelle escluse/incluse al tap lungo su
     * Mostra/Nascondi*). From 4.30 to 4.34 the long press only showed the label.
     */
    @Test
    @Config(shadows = [ArchivioAperto::class])
    fun `il tocco lungo su Mostra apre le cartelle nascoste`() {
        banco.setContent { Home(PillLook(PhonePill.SLIDE, corner = true), Hand.RIGHT) }
        banco.waitForIdle()
        banco.onNodeWithContentDescription(voce(R.string.hub_open)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(voce(R.string.hub_peek)).performTouchInput { longClick() }
        banco.waitForIdle()
        assertEquals("Il tocco lungo non ha chiuso il menu", 0, quanti(R.string.hub_search))
        banco.onNodeWithText(voce(R.string.settings_hidden)).assertExists()
    }

    /**
     * **The open Start menu's side keeps clear of the thumbnails' edge, in the home and in a folder**
     * (item `4.33-07` B, `startMenu.png`: *le linee verticali e orizzontali non devono avvicinarsi
     * troppo a quelle sottostanti*). In 4.33 it fell 2dp inside the home's thumbnails and 6dp inside a
     * grid's.
     */
    @Test
    @Config(shadows = [ArchivioAperto::class], qualifiers = "w393dp-h873dp")
    fun `il fianco del menu Start resta fuori dal bordo delle miniature`() {
        var casa by mutableStateOf(true)
        banco.setContent {
            if (casa) Home(PillLook(PhonePill.SLIDE, corner = true), Hand.RIGHT)
            else Griglia(PillLook(PhonePill.SLIDE, corner = true))
        }
        banco.waitForIdle()
        val dp = app.resources.displayMetrics.density
        fun misura(apri: Int): Float {
            banco.onNodeWithContentDescription(voce(apri)).performClick()
            banco.waitForIdle()
            val griglia = banco.onAllNodes(androidx.compose.ui.test.hasScrollAction()).fetchSemanticsNodes().first()
            fun tutti(n: androidx.compose.ui.semantics.SemanticsNode): List<androidx.compose.ui.semantics.SemanticsNode> =
                listOf(n) + n.children.flatMap { tutti(it) }
            val miniature = tutti(griglia).drop(1).maxOf { it.boundsInRoot.right }
            val pannello = banco.onAllNodesWithTag(START_PANEL_TAG, useUnmergedTree = true)
                .fetchSemanticsNodes().single().boundsInRoot.right
            return (pannello - miniature) / dp
        }
        val inCasa = misura(R.string.hub_open)
        banco.onNodeWithContentDescription(voce(R.string.pick_close)).performClick()
        casa = false
        banco.waitForIdle()
        val inCartella = misura(R.string.pick_actions)
        assertTrue("In home il fianco del menu è a ${inCasa}dp dal bordo delle miniature", inCasa >= 4f)
        assertTrue("In cartella il fianco del menu è a ${inCartella}dp dal bordo delle miniature", inCartella >= 4f)
    }

    /**
     * **On a glass colour of his the glyphs take the ink with more contrast** (answer A1 to
     * `colore-icone-vetro`): a light yellow gets the dark ink, a deep blue the light one, and without
     * his colour the pill keeps the accent's ink.
     */
    @Test
    fun `sul vetro col suo colore l'inchiostro e quello di maggior contrasto`() {
        assertEquals(FAB_GLASS_INK_DARK, inkOn(androidx.compose.ui.graphics.Color(0xFFFFE680)))
        assertEquals(FAB_GLASS_INK_LIGHT, inkOn(androidx.compose.ui.graphics.Color(0xFF102060)))
        var letto: androidx.compose.ui.graphics.Color? = null
        var look by mutableStateOf(PillLook(PhonePill.SLIDE, fill = PillFill.GLASS))
        banco.setContent {
            AivTheme(darkTheme = false) {
                CompositionLocalProvider(LocalPillLook provides look) { letto = pillInk() }
            }
        }
        banco.waitForIdle()
        assertEquals("Senza il suo colore il vetro non tiene l'inchiostro dell'accento", aivOnAccent(false), letto)
        look = look.copy(glass = look.glass.withColour(light = true, colour = 0xFFFFE680.toInt()))
        banco.waitForIdle()
        assertEquals("Sul suo giallo chiaro l'inchiostro non è quello scuro", FAB_GLASS_INK_DARK, letto)
    }

    /**
     * **'Colore chiaro' opens the picker: 'Predefinito' goes back to the accent and 'Applica' writes a
     * colour** (his answer B3).
     */
    @Test
    fun `il selettore del colore torna al predefinito o scrive il colore`() {
        var scritte: Settings? = null
        banco.setContent {
            AivTheme(darkTheme = false) {
                SettingsScreen(
                    settings = Settings(pillFill = PillFill.GLASS, glass = GlassTune(lightColour = 0xFF336699.toInt())),
                    onChange = { scritte = it },
                    onStartFolder = {},
                    onResetHints = {},
                    onChooseEditor = {},
                    onBack = {}
                )
            }
        }
        banco.waitForIdle()
        banco.onAllNodesWithText(voce(R.string.settings_page_look), substring = false)[0]
            .performScrollTo().performClick()
        banco.waitForIdle()
        banco.onNodeWithText(voce(R.string.glass_colour_light)).performScrollTo().performClick()
        banco.waitForIdle()
        banco.onNodeWithText(voce(R.string.glass_default)).performClick()
        banco.waitForIdle()
        assertTrue("'Predefinito' non ha scritto niente", scritte != null)
        assertEquals("'Predefinito' non è tornato all'accento", null, scritte?.glass?.lightColour)

        banco.onNodeWithText(voce(R.string.glass_colour_light)).performScrollTo().performClick()
        banco.waitForIdle()
        banco.onNodeWithText(voce(R.string.editor_apply)).performClick()
        banco.waitForIdle()
        assertTrue("'Applica' non ha scritto il colore", scritte?.glass?.lightColour != null)
        assertEquals("'Applica' ha toccato il colore scuro", null, scritte?.glass?.darkColour)
    }

    /**
     * **The open Start menu closes with any touch outside it, a drag on the grid too** (his note on
     * `4.25-02`: *QUALSIASI tocco fuori, anche un trascinamento sulla griglia*).
     */
    @Test
    fun `il menu Start si chiude trascinando sulla griglia`() {
        banco.setContent { Griglia(PillLook(PhonePill.SLIDE, corner = true)) }
        banco.waitForIdle()
        banco.onNodeWithContentDescription(voce(R.string.pick_actions)).performClick()
        banco.waitForIdle()
        assertEquals("Il menu Start non si è aperto", 1, quanti(R.string.hub_settings))
        scorri()
        banco.waitForIdle()
        assertEquals("Il trascinamento sulla griglia non ha chiuso il menu Start", 0, quanti(R.string.hub_settings))
    }

    /**
     * **The drag that closes the Start menu scrolls the grid too, in one gesture** (item `4.30-03`
     * on the 4.32 round: *al primo tocco si chiude il menu e al secondo posso agire. Dev'essere un
     * unico gesto*).
     */
    @Test
    fun `il trascinamento che chiude il menu Start scorre anche la griglia`() {
        banco.setContent { Griglia(PillLook(PhonePill.SLIDE, corner = true)) }
        banco.waitForIdle()
        val riferimento = scorrimento()
        banco.onNodeWithContentDescription(voce(R.string.pick_actions)).performClick()
        banco.waitForIdle()
        assertEquals("Il menu Start non si è aperto", 1, quanti(R.string.hub_settings))
        scorriAPassi()
        banco.waitForIdle()
        assertEquals("Il trascinamento non ha chiuso il menu Start", 0, quanti(R.string.hub_settings))
        assertTrue("Il trascinamento che chiude il menu non ha scorso la griglia", scorrimento() != riferimento)
    }

    /** The same in the home, with enough folders to scroll. */
    @Test
    @Config(shadows = [ArchivioAperto::class])
    fun `nella home il trascinamento che chiude il menu Start scorre anche le cartelle`() {
        val molte = (1..40).map {
            Folder.Bucket(id = it.toLong(), name = "Cartella $it", pictures = 2, clips = 0, cover = null, path = "/storage/emulated/0/F$it")
        }
        banco.setContent {
            AivTheme(darkTheme = false) {
                CompositionLocalProvider(LocalPillLook provides PillLook(PhonePill.SLIDE, corner = true), LocalPadLook provides PadLook(hand = Hand.RIGHT)) {
                    Box(modifier = Modifier.fillMaxSize()) { Casa(molte) }
                }
            }
        }
        banco.waitForIdle()
        val riferimento = scorrimento()
        banco.onNodeWithContentDescription(voce(R.string.hub_open)).performClick()
        banco.waitForIdle()
        assertEquals("Il menu Start non si è aperto", 1, quanti(R.string.hub_settings))
        scorriAPassi()
        banco.waitForIdle()
        assertEquals("Il trascinamento non ha chiuso il menu Start", 0, quanti(R.string.hub_settings))
        assertTrue("Il trascinamento che chiude il menu non ha scorso le cartelle", scorrimento() != riferimento)
    }

    /**
     * A drag in steps with frames between them, as on a phone: the menu closes after the press, and
     * the rest of the gesture arrives on the screen as it is after the close.
     */
    private fun scorriAPassi() {
        val scena = banco.onRoot().fetchSemanticsNode().size
        val x = scena.width * LATO
        banco.onRoot().performTouchInput { down(Offset(x, scena.height * DA)) }
        banco.mainClock.advanceTimeBy(50)
        for (i in 1..12) {
            val y = scena.height * (DA + (A - DA) * i / 12f)
            banco.onRoot().performTouchInput { moveTo(Offset(x, y)) }
            banco.mainClock.advanceTimeBy(16)
        }
        banco.onRoot().performTouchInput { up() }
        banco.mainClock.advanceTimeBy(RESPIRO)
    }

    /** How far the grid has scrolled, from its semantics: it changes when the grid scrolls. */
    private fun scorrimento(): Float =
        banco.onAllNodes(androidx.compose.ui.test.hasScrollAction()).fetchSemanticsNodes().first()
            .config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange].value()

    /**
     * **The round key is 44dp and sits in the same corner in the home and in a folder** (note B on
     * the 4.25 round: *tra home e cartelle il tondo col glifo salta da una posizione all'altra ...
     * addirittura cambia dimensione*).
     *
     * ⚠️ **A narrow phone**: on a wide one eight keys of 44dp fit the row, and the key did not
     * shrink even with the defect.
     */
    @Test
    @Config(shadows = [ArchivioAperto::class], qualifiers = "w340dp-h740dp")
    fun `il tondo ha la stessa misura e lo stesso angolo in home e in cartella`() {
        var casa by mutableStateOf(true)
        banco.setContent { if (casa) Home(PillLook(PhonePill.SLIDE), Hand.RIGHT) else Griglia(PillLook(PhonePill.SLIDE)) }
        banco.waitForIdle()
        val dp = app.resources.displayMetrics.density
        val inCasa = banco.onNodeWithContentDescription(voce(R.string.hub_open)).fetchSemanticsNode().boundsInRoot
        casa = false
        banco.waitForIdle()
        val inCartella = banco.onNodeWithContentDescription(voce(R.string.pick_actions)).fetchSemanticsNode().boundsInRoot
        assertEquals("Il tondo della home non è largo 44dp", 44f * dp, inCasa.width, 0.5f * dp)
        assertEquals("Il tondo della cartella non è largo 44dp", 44f * dp, inCartella.width, 0.5f * dp)
        assertEquals("Il tondo salta in verticale fra home e cartella", inCasa.bottom, inCartella.bottom, 0.5f * dp)
        assertEquals("Il tondo salta in orizzontale fra home e cartella", inCasa.right, inCartella.right, 0.5f * dp)
    }

    /**
     * **On the square FAB the A is centred sideways, and the sun disc follows it** (note C on the
     * 4.25 round: *è il triangolo a dover essere centrato, e il tondo di conseguenza*). The A spans
     * the whole width of the mark's canvas, so the mark's centre is the A's.
     */
    @Test
    @Config(shadows = [ArchivioAperto::class])
    fun `nel FAB quadrato il triangolo del marchio e centrato`() {
        banco.setContent { Home(PillLook(), Hand.RIGHT) }
        banco.waitForIdle()
        val fab = banco.onNodeWithContentDescription(voce(R.string.hub_open)).fetchSemanticsNode().boundsInRoot
        val segno = banco.onAllNodesWithTag(MARK_TAG, useUnmergedTree = true).fetchSemanticsNodes().first().boundsInRoot
        val dp = app.resources.displayMetrics.density
        assertEquals("Il marchio è fuori centro in orizzontale nel FAB", fab.center.x, segno.center.x, 0.25f * dp)
    }

    /**
     * **The FAB's centre is the round key's centre, in the home and in a folder** (his rule after the
     * 4.30: *anche il centro del FAB dev'essere centrato sul centro di quel tondo*). Until 4.30 the FAB
     * kept its own corner: 16dp from the glass in the home, 16 by 20 in a grid.
     */
    @Test
    @Config(shadows = [ArchivioAperto::class])
    fun `il FAB ha il centro sul centro del tondo in home e in cartella`() {
        var scena by mutableStateOf(0)
        banco.setContent {
            when (scena) {
                0 -> Home(PillLook(PhonePill.SLIDE), Hand.RIGHT)
                1 -> Home(PillLook(), Hand.RIGHT)
                else -> Griglia(PillLook())
            }
        }
        banco.waitForIdle()
        val dp = app.resources.displayMetrics.density
        val tondo = banco.onNodeWithContentDescription(voce(R.string.hub_open)).fetchSemanticsNode().boundsInRoot
        scena = 1
        banco.waitForIdle()
        val inCasa = banco.onNodeWithContentDescription(voce(R.string.hub_open)).fetchSemanticsNode().boundsInRoot
        scena = 2
        banco.waitForIdle()
        val inCartella = banco.onNodeWithContentDescription(voce(R.string.pick_actions)).fetchSemanticsNode().boundsInRoot
        assertEquals("Il FAB della home non è largo 40dp", 40f * dp, inCasa.width, 0.5f * dp)
        assertEquals("Il FAB della home è fuori asse in orizzontale", tondo.center.x, inCasa.center.x, 0.5f * dp)
        assertEquals("Il FAB della home è fuori asse in verticale", tondo.center.y, inCasa.center.y, 0.5f * dp)
        assertEquals("Il FAB della cartella è fuori asse in orizzontale", tondo.center.x, inCartella.center.x, 0.5f * dp)
        assertEquals("Il FAB della cartella è fuori asse in verticale", tondo.center.y, inCartella.center.y, 0.5f * dp)
    }

    /**
     * **At the first start the hint shows the Start menu open, its corner on the round key** (his
     * request after the 4.30: *è fondamentale che al primo avvio il micro-onboarding mostri quello,
     * magari espanso*): the home's three columns, and the copy of the key exactly over the real one.
     */
    @Test
    @Config(shadows = [ArchivioAperto::class])
    fun `al primo avvio il velo mostra il menu Start aperto sul tondo`() {
        runBlocking { Hint.COLUMNS.forget(app) }
        banco.setContent { Home(PillLook(PhonePill.SLIDE, corner = true), Hand.RIGHT) }
        aspettaIlVelo(banco, app)
        banco.waitForIdle()
        val dp = app.resources.displayMetrics.density
        banco.onNodeWithText(voce(R.string.corner_hint)).assertExists()
        val tondi = banco.onAllNodesWithContentDescription(voce(R.string.hub_open)).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals("Sotto il velo non ci sono il tondo e la sua copia", 2, tondi.size)
        assertEquals("La copia del tondo è fuori asse in orizzontale", tondi[0].center.x, tondi[1].center.x, 0.5f * dp)
        assertEquals("La copia del tondo è fuori asse in verticale", tondi[0].center.y, tondi[1].center.y, 0.5f * dp)
        val pannello = banco.onAllNodesWithTag(CORNER_COPY_TAG, useUnmergedTree = true).fetchSemanticsNodes().single().boundsInRoot
        // ⚠️ Since 4.34 the columns are 72dp and the panel grows 6dp past them towards the glass,
        // 4 down and 10 up (item `4.33-07` B); since 4.35 it is square (item `4.34-01`), so a row
        // is 68dp, a third of 222 less 14 rounded down to an even number. The corner cell's centre
        // is still the round key's.
        // An even number of dp (`startRow`), the rest above.
        val riga = 68f
        assertEquals("Il menu Start del velo non è aperto su tre colonne", (3 * 72f + 6f) * dp, pannello.width, 0.5f * dp)
        assertEquals("Il menu Start del velo non è quadrato", pannello.width, pannello.height, 0.5f * dp)
        assertEquals("Il menu Start del velo non ha l'angolo sul tondo", tondi[0].center.x, pannello.right - (6f + 36f) * dp, 0.5f * dp)
        assertEquals("Il menu Start del velo non ha il fondo sul tondo", tondi[0].center.y, pannello.bottom - (4f + riga / 2f) * dp, 0.5f * dp)
    }

    private fun quanti(id: Int): Int =
        banco.onAllNodesWithContentDescription(voce(id)).fetchSemanticsNodes().size

    /** The same flick as `SaltiTest`, for the same measured reasons, written there. */
    private fun scorri() {
        val scena = banco.onRoot().fetchSemanticsNode().size
        banco.onRoot().performTouchInput {
            down(Offset(scena.width * LATO, scena.height * DA))
            for (i in 1..PASSI) moveTo(Offset(scena.width * LATO, scena.height * (DA + (A - DA) * i / PASSI)), PASSO_MS)
            up()
        }
        banco.mainClock.advanceTimeBy(RESPIRO)
    }

    /** The home with one hidden folder, so the pill has its seven entries. */
    @Composable
    private fun Home(look: PillLook, hand: Hand, onCornerRest: () -> Unit = {}) {
        AivTheme(darkTheme = false) {
            CompositionLocalProvider(LocalPillLook provides look, LocalPadLook provides PadLook(hand = hand)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Casa(CARTELLE, hidden = setOf("Segreta"), onCornerRest = onCornerRest)
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

/**
 * How long a test waits for the first start's hint, read from the preferences' store.
 *
 * ⚠️ **20 s since 4.36, and it was 5**: see [aspettaIlVelo].
 */
internal const val VELO_MS = 20_000L

/**
 * Waits for the first start's hint of the Start menu, and says why when it does not come.
 *
 * ⚠️ The hint comes from the preferences' store, read on another thread that `waitForIdle` does not
 * wait for: in the full bench the veil was not there yet, and the test failed.
 * ⚠️⚠️ **WHY IT FELL NOW AND THEN, FOUND IN 4.98** (B2, after it stopped the releases of 4.96 and
 * 4.97): the old `createComposeRule` runs a composition's effects on an unconfined test dispatcher,
 * so the coroutine of `produceState` in `FolderScreen` resumes on whatever thread hands it the
 * value. When the store has to read its file again (after `DisegnoTest`, which writes other hints),
 * 'not seen' arrives on a `DefaultDispatcher` worker, the state is written there, and no recomposition
 * follows: the hint never comes, with the store saying 'not seen'. Measured on this machine on two
 * cores, `DisegnoTest` first: the value logged on a worker thread in every fall, 3 falls in 6 runs.
 * Calling `Snapshot.sendApplyNotifications` while waiting was tried and is not enough (2 falls in
 * 5 runs, in `EtichetteStartTest`).
 * ⚠️⚠️ **THE FIX IS THE NEW RULE, `junit4.v2.createComposeRule`**, in this class and in
 * `EtichetteStartTest`, the two that wait for the hint: its standard dispatcher runs every resume
 * on the test's main thread, as the app's own dispatcher does on a phone. 0 falls in 5 runs on the
 * same two cores, every value logged on the main thread. The app was never at fault.
 * ⚠️ The earlier guesses are fallen: not a slow machine, not two class loaders, not a stuck store.
 * The 20 s wait and the report on a timeout stay, as a net.
 */
internal fun aspettaIlVelo(banco: androidx.compose.ui.test.junit4.ComposeContentTestRule, app: android.content.Context) {
    val frase = app.getString(R.string.corner_hint)
    val inizio = System.nanoTime()
    try {
        banco.waitUntil(VELO_MS) { banco.onAllNodesWithText(frase).fetchSemanticsNodes().isNotEmpty() }
    } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
        val p = runBlocking { storedPreferences(app) }
        val testi = banco.onAllNodes(androidx.compose.ui.test.hasText("", substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .mapNotNull { n -> n.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.Text) { null }?.joinToString() }
        val flusso = runBlocking { kotlinx.coroutines.withTimeoutOrNull(2_000) { Hint.COLUMNS.flow(app).first() } }
        throw AssertionError(
            "Il velo non è comparso. permesso=${Folder.granted(app)} flusso=$flusso archivio=${p.asMap()} " +
                "vista=${SettingsStore.read(p).folderView} testi=$testi", e
        )
    }
    val ms = (System.nanoTime() - inizio) / 1_000_000
    if (ms > VELO_LENTO_MS) println("VELO LENTO: $ms ms")
}

/** Beyond this, a hint that came is reported as slow: the solid variant took 0,3 s. */
private const val VELO_LENTO_MS = 2_000L

/** As in `SaltiTest`: the flick's steps. */
private const val PASSI = 6
private const val PASSO_MS = 10L


/** As in `SaltiTest`. */
private const val DA = 0.8f

/** As in `SaltiTest`. */
private const val A = 0.2f

/** As in `SaltiTest`. */
private const val LATO = 0.25f

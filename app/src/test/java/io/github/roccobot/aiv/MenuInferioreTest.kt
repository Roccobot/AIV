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
import androidx.compose.ui.test.junit4.createComposeRule
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
        assertEquals("Impostazioni e la × non sono sulla stessa riga", impostazioni.y, chiudi.y, 1f)
        assertTrue("La × non è nell'angolo", chiudi.x > impostazioni.x)
        assertEquals("Cerca e Cestino non sono sulla stessa riga", cerca.y, cestino.y, 1f)
        assertTrue("La riga di Cerca non è sopra", cerca.y < impostazioni.y)
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
        banco.onNodeWithContentDescription(voce(R.string.hub_open)).performClick()
        banco.waitForIdle()
        val pos = { id: Int -> banco.onNodeWithContentDescription(voce(id)).fetchSemanticsNode().positionInRoot }
        assertEquals("La vista in cui si è è ancora nel menu", 0, quanti(R.string.hub_view_grid))
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
        val cella = (scena - 48f * dp) / 8f
        val centro = { id: Int ->
            banco.onNodeWithContentDescription(voce(id)).fetchSemanticsNode().let { it.positionInRoot.x + it.size.width / 2f }
        }
        // ⚠️ The far end may be off by half a pixel per cell, rounded to the pixel: the corner is exact.
        assertEquals("La prima voce non è nella prima cella dopo 24dp", 24f * dp + cella / 2f, centro(R.string.hub_view_list), 1.5f * dp + 4f)
        assertEquals("La × non è nell'ultima cella prima dei 24dp", scena - 24f * dp - cella / 2f, centro(R.string.pick_close), 1.5f * dp)
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

/** As in `SaltiTest`. */
private const val FERMO = 300L

/** As in `SaltiTest`. */
private const val DA = 0.8f

/** As in `SaltiTest`. */
private const val A = 0.2f

/** As in `SaltiTest`. */
private const val LATO = 0.25f

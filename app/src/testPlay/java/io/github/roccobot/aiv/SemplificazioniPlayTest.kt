package io.github.roccobot.aiv

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/*
 * AIV Play made simpler (5.30): his note in Altro on the 5.20 round, headed *solo AIV Play*.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️⚠️ **What is measured**: a store written before 5.30, or a settings file from AIV GitHub,
 * cannot bring back what AIV Play dropped (`Store.fit`); the rows of those settings are not in
 * the panel, not even through the search; the 'Estensione' key never shows; the grid does not
 * mark the last item seen.
 * ⚠️ Runs only in the `play` variant: see `AccessoPlayTest`.
 */
@RunWith(AndroidJUnit4::class)
class SemplificazioniPlayTest {

    @get:Rule
    val banco = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private fun testo(id: Int) = app.getString(id)

    @Before
    fun prepara() {
        shadowOf(app as android.app.Application).grantPermissions(Manifest.permission.READ_MEDIA_IMAGES)
    }

    /** **Every value he fixed wins over the store**, whatever an older AIV wrote there. */
    @Test
    fun `l'archivio non rimette quello che AIV Play ha tolto`() {
        val letto = SettingsStore.read(
            mutablePreferencesOf(
                stringPreferencesKey("main-control") to "bottom",
                stringPreferencesKey("panel-depth") to "blur",
                booleanPreferencesKey("front-wash") to false,
                booleanPreferencesKey("front-serif") to true,
                booleanPreferencesKey("front-facts") to false,
                stringPreferencesKey("folder-colour") to "frame",
                booleanPreferencesKey("sequence-reversed") to true,
                booleanPreferencesKey("ext-edit") to true,
                booleanPreferencesKey("ext-download") to true
            )
        )
        assertEquals(MainControl.PILL, letto.mainControl)
        assertEquals(PanelDepth.SHADOW, letto.panelDepth)
        assertEquals(true, letto.frontWash)
        assertEquals(false, letto.frontSerif)
        assertEquals(FolderColour.NAME, letto.folderColour)
        assertEquals(false, letto.reverseSequence)
        assertEquals(false, letto.extRename)
        assertEquals(false, letto.extDownload)
        // ⚠️ 'Info' stays a choice of his: the store keeps it off.
        assertEquals(false, letto.frontFacts)
    }

    /** **The two menus that are gone become the pill, and the FAB stays the FAB.** */
    @Test
    fun `menu basso e menu Start diventano la pillola e il FAB resta`() {
        fun con(token: String) =
            SettingsStore.read(mutablePreferencesOf(stringPreferencesKey("main-control") to token)).mainControl
        assertEquals(MainControl.PILL, con("corner"))
        assertEquals(MainControl.PILL, con("bottom"))
        assertEquals(MainControl.FAB, con("fab"))
        assertEquals(listOf(MainControl.FAB, MainControl.PILL), Store.mainControls)
    }

    /**
     * **The rows he dropped are not in the panel, and the search does not find them**, while the
     * rows that stay are found: the folder header with only 'Info' and 'Select all', and the main
     * control with the FAB and the pill.
     *
     * ⚠️ The query is the label without its last letter, so the field itself never matches the
     * label it looks for.
     */
    @Test
    fun `le impostazioni non hanno le voci tolte`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                SettingsScreen(
                    settings = SettingsStore.read(mutablePreferencesOf()),
                    onChange = {},
                    onStartFolder = {},
                    onResetHints = {},
                    onChooseEditor = {},
                    onBack = {}
                )
            }
        }
        banco.waitForIdle()
        fun cerca(id: Int): Int {
            val voce = testo(id)
            banco.onNode(hasSetTextAction()).performTextReplacement(voce.dropLast(1))
            banco.waitForIdle()
            return banco.onAllNodesWithText(voce, useUnmergedTree = true).fetchSemanticsNodes().size
        }
        listOf(
            R.string.settings_depth,
            R.string.settings_colour,
            R.string.settings_reverse_order,
            R.string.settings_last_mark,
            R.string.settings_ext_edit,
            R.string.settings_front_wash,
            R.string.settings_front_serif,
            R.string.main_control_bar,
            R.string.main_control_corner
        ).forEach { assertEquals("'${testo(it)}' c'è ancora", 0, cerca(it)) }
        assertEquals(1, cerca(R.string.settings_front))
        // ⚠️ With the header found, its two chips that stay are on screen.
        listOf(R.string.settings_front_facts, R.string.pick_all).forEach {
            assertEquals("'${testo(it)}' manca", true,
                banco.onAllNodesWithText(testo(it), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        }
        assertEquals(1, cerca(R.string.main_control_fab))
        assertEquals(1, cerca(R.string.main_control_pill))
    }

    /**
     * **The 'Estensione' key is never there, not even with the long press on 'Scarica'**, which in
     * AIV GitHub brings it back for one time (`force`).
     */
    @Test
    fun `il tasto Estensione non c'e nemmeno col tocco lungo`() {
        var forzato: ExtensionGate? = null
        var normale: ExtensionGate? = null
        banco.setContent {
            forzato = extensionGate(where = ExtWhere.DOWNLOAD, force = true, initial = { "jpg" }, onPick = {})
            normale = extensionGate(where = ExtWhere.DOWNLOAD, force = false, initial = { "jpg" }, onPick = {})
        }
        banco.waitForIdle()
        assertFalse("Col tocco lungo il tasto c'è", forzato!!.allowed)
        assertFalse("Il tasto c'è", normale!!.allowed)
    }

    /**
     * **The grid does not mark the last item seen**, and still knows which one it is: the jump
     * back to it uses the same index.
     */
    @Test
    fun `la griglia non segna l'ultimo elemento visto`() {
        runBlocking { Hint.COLUMNS.remember(app) }
        banco.setContent {
            AivTheme(darkTheme = false) {
                GridScreen(title = "Cartella", items = FOTO_PLAY, highlight = 2, onOpen = {}, onBack = {},
                    onChanged = {}, onSearch = {}, shape = Adaptive.Shape.PHONE)
            }
        }
        banco.waitForIdle()
        val ultima = app.getString(R.string.grid_item_last, 3, FOTO_PLAY.size)
        val qualunque = app.getString(R.string.grid_item, 3, FOTO_PLAY.size)
        assertEquals(0, banco.onAllNodesWithContentDescription(ultima).fetchSemanticsNodes().size)
        assertEquals(1, banco.onAllNodesWithContentDescription(qualunque).fetchSemanticsNodes().size)
        assertFalse(Store.marksLast)
    }
}

/** A short folder, so the third picture is on screen. */
private val FOTO_PLAY = (1..12).map { Uri.parse("file:///finta/$it.jpg") }

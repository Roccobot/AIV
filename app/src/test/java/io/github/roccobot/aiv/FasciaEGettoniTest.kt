package io.github.roccobot.aiv

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * His notes A and B on the giro of the 6.10, in the settings (6.20).
 *
 * ⚠️ **The real settings screen**, opened as he opens it: the group, then the page.
 */
@RunWith(AndroidJUnit4::class)
class FasciaEGettoniTest {
    @get:Rule val scene = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private fun impostazioni(settings: Settings) {
        scene.setContent {
            AivTheme(darkTheme = false) {
                SettingsScreen(
                    settings = settings,
                    onChange = {},
                    onStartFolder = {},
                    onResetHints = {},
                    onChooseEditor = {},
                    onBack = {}
                )
            }
        }
    }

    private fun apri(gruppo: Int, pagina: Int) {
        scene.onNodeWithText(app.getString(gruppo)).performScrollTo().performClick()
        scene.waitForIdle()
        scene.onNodeWithText(app.getString(pagina)).performScrollTo().performClick()
        scene.waitForIdle()
    }

    /**
     * **Note A: with a long list of included folders 'Aggiungi cartella' stays in sight.**
     *
     * Forty folders are taller than the window, so a button at the end of a page that scrolls as a
     * whole is in the tree but off screen.
     * ⚠️ **Counter-tested** with the page of the 6.10 (the whole page scrolling, the button after the
     * list): `assertIsDisplayed` fails on the button.
     */
    @Test
    fun `con tante cartelle incluse Aggiungi cartella resta in vista`() {
        val tante = (1..40).map { "Pictures/Cartella$it" }.toSet()
        impostazioni(Settings(folderMode = FolderMode.INCLUDED, includedFolders = tante))
        apri(R.string.settings_group_browse, R.string.folder_selection)
        scene.onNodeWithText(app.getString(R.string.folder_add)).assertIsDisplayed()
    }

    /**
     * **Note B: the two chips of 'Rendering dei pixel' are as wide as each other, and as the page.**
     *
     * ⚠️ **Counter-tested** with the flowing row of the 6.10: each chip is as wide as its words, and
     * the two widths differ.
     */
    @Test
    fun `i due gettoni dei pixel sono larghi uguali e a tutta larghezza`() {
        impostazioni(Settings())
        apri(R.string.settings_group_viewer, R.string.settings_zoom_page)
        val fisico = scene.onNodeWithText(app.getString(R.string.settings_scale_physical)).getBoundsInRoot()
        val logico = scene.onNodeWithText(app.getString(R.string.settings_scale_logical)).getBoundsInRoot()
        assertEquals(fisico.right - fisico.left, logico.right - logico.left)
        assertEquals(fisico.left, logico.left)
        val pagina = scene.onNodeWithText(app.getString(R.string.settings_zoom_page)).getBoundsInRoot()
        assertTrue("i gettoni arrivano al bordo destro della pagina", fisico.right > pagina.right - 1.dp)
    }
}

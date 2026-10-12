package io.github.roccobot.aiv

import android.content.Context
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
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
 * The view options of the long press on the main button, from the `5.90` (his note B on the giro
 * of the `5.80`, mockup `homeLongPress.png`).
 *
 * ⚠️ **The dialog is mounted on its own**: what these cases look at is its content, and reaching it
 * through the long press would measure the button too, which other tests already do.
 */
@RunWith(AndroidJUnit4::class)
class VistaOpzioniTest {
    @get:Rule val scene = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private fun testo(id: Int) = app.getString(id)

    private fun apri(view: FolderView, onOrder: (FolderOrder) -> Unit = {}, onFavourites: () -> Unit = {}) {
        scene.setContent {
            AivTheme(darkTheme = false) {
                ViewOptions(
                    view = view,
                    columns = 3,
                    order = FolderOrder.ALPHA,
                    listCount = true,
                    listText = TextSize.NORMAL,
                    treeHidden = false,
                    treePictures = false,
                    onView = {},
                    onColumns = {},
                    onOrder = onOrder,
                    onListCount = {},
                    onListText = {},
                    onTreeHidden = {},
                    onTreePictures = {},
                    onFavourites = onFavourites,
                    onDismiss = {}
                )
            }
        }
        scene.waitForIdle()
    }

    /**
     * **Case 1: the grid has the order and the favourites, and no colour.**
     *
     * The class runs on AIV GitHub, where the colour exists, so the last line is the one that would
     * see it come back.
     */
    @Test
    fun `in griglia la disposizione e le preferite, senza colori`() {
        var scelta: FolderOrder? = null
        var preferite = false
        apri(FolderView.GRID, onOrder = { scelta = it }, onFavourites = { preferite = true })
        scene.onNodeWithText(testo(R.string.folder_order_short)).assertExists()
        scene.onNodeWithText(testo(R.string.folder_order_recent)).performClick()
        assertEquals(FolderOrder.RECENT, scelta)
        scene.onNodeWithText(testo(R.string.favourite_folders)).performScrollTo().performClick()
        assertTrue(preferite)
        scene.onNodeWithText(testo(R.string.settings_colour)).assertDoesNotExist()
    }

    /** **Case 2: the list has the favourites but no order**, which only the grid follows. */
    @Test
    fun `in lista le preferite senza disposizione`() {
        apri(FolderView.LIST)
        scene.onNodeWithText(testo(R.string.favourite_folders)).assertExists()
        scene.onNodeWithText(testo(R.string.folder_order_short)).assertDoesNotExist()
        scene.onNodeWithText(testo(R.string.settings_colour)).assertDoesNotExist()
    }

    /**
     * **Case 3: every row of chips starts on the vertical of the titles and ends on the same edge.**
     *
     * His note B (*a sinistra testi e bordi dei gettoni si allineino sulla stessa verticale*) and
     * his reminder of the same day: the chips of the columns were not aligned on the horizontal
     * axis. The left edges of '2' and 'Alfabetica' are those of their titles, and the right edges
     * of '4' and 'Più recenti in alto' coincide, which is a row that fills the width.
     * ⚠️ **Counter-tested** with the rows of the `5.80` (a `Row` and a `FlowRow` of chips at their
     * own width): '2' starts to the right of its title, and the right edges differ.
     */
    @Test
    fun `i gettoni riempiono la riga e partono dalla verticale dei titoli`() {
        apri(FolderView.GRID)
        val tolleranza = 1.dp
        val titolo = scene.onNodeWithText(testo(R.string.columns_title)).getBoundsInRoot()
        val due = scene.onNodeWithText("2").getBoundsInRoot()
        val quattro = scene.onNodeWithText("4").getBoundsInRoot()
        val disposizione = scene.onNodeWithText(testo(R.string.folder_order_short)).getBoundsInRoot()
        val alfabetica = scene.onNodeWithText(testo(R.string.folder_order_alpha)).getBoundsInRoot()
        val recenti = scene.onNodeWithText(testo(R.string.folder_order_recent)).getBoundsInRoot()
        assertTrue("2: ${due.left} contro ${titolo.left}", (due.left - titolo.left) in -tolleranza..tolleranza)
        assertTrue(
            "Alfabetica: ${alfabetica.left} contro ${disposizione.left}",
            (alfabetica.left - disposizione.left) in -tolleranza..tolleranza
        )
        assertTrue("4: ${quattro.right} contro ${recenti.right}", (quattro.right - recenti.right) in -tolleranza..tolleranza)
    }
}

package io.github.roccobot.aiv

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The order of the grid and the favourite folders, from the `5.80` (his request of 2026-10-12).
 *
 * ⚠️ **The order is written by `FolderSelection.arrange`, the one function every view calls**, so
 * the unit cases measure what the grid, the list and the tablet column show; the screen cases
 * measure the two windows and the question asked while the order is not chosen.
 */
@RunWith(AndroidJUnit4::class)
class CartellePreferiteTest {
    @get:Rule val scene = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private fun bucket(name: String, latest: Long) =
        Folder.Bucket(name.hashCode().toLong(), name, 1, 0, null, "/storage/emulated/0/Pictures/$name", latest)

    /** Alphabetical, as `Folder.buckets` returns them; 'Mare' holds the newest file. */
    private val cartelle = listOf(bucket("Albe", 10L), bucket("Mare", 30L), bucket("Neve", 20L))

    private fun nomi(list: List<Folder.Bucket>) = list.map { it.name }

    /**
     * **Case 1: the grid follows the order, the list does not, the favourites come first.**
     *
     * ⚠️ **Counter-tested** by sorting also when `grid` is false: the list comes out by date and
     * the test fails on its first line.
     */
    @Test
    fun `la griglia segue la disposizione, la lista no, le preferite in testa`() {
        val recenti = FolderSelection(order = FolderOrder.RECENT)
        assertEquals(listOf("Albe", "Mare", "Neve"), nomi(recenti.arrange(cartelle, grid = false)))
        assertEquals(listOf("Mare", "Neve", "Albe"), nomi(recenti.arrange(cartelle, grid = true)))
        assertEquals(
            "senza una scelta la griglia resta alfabetica",
            listOf("Albe", "Mare", "Neve"), nomi(FolderSelection().arrange(cartelle, grid = true))
        )
        val preferite = recenti.copy(favourites = listOf("Pictures/Neve", "Pictures/Albe"))
        assertEquals(
            "le preferite nel loro ordine, poi le altre",
            listOf("Neve", "Albe", "Mare"), nomi(preferite.arrange(cartelle, grid = true))
        )
        assertEquals(listOf("Neve", "Albe", "Mare"), nomi(preferite.arrange(cartelle, grid = false)))
    }

    /** **Case 2: the same gesture adds and removes a favourite, at the end of the list.** */
    @Test
    fun `lo stesso gesto aggiunge e toglie una preferita`() {
        val una = FolderSelection(favourites = listOf("Pictures/Mare"))
        val due = una.flipFavourite("/storage/emulated/0/Pictures/Albe")
        assertEquals(listOf("Pictures/Mare", "Pictures/Albe"), due.favourites)
        assertTrue(due.favourite("/storage/emulated/0/Pictures/Albe"))
        assertEquals(listOf("Pictures/Albe"), due.flipFavourite("/storage/emulated/0/Pictures/Mare").favourites)
    }

    /**
     * **Case 3: the favourites keep their order, and 'not chosen' survives a save.**
     *
     * ⚠️ **Counter-tested** by writing the order's token also when it is `null` (as `"null"`): the
     * read gives no order, but the key stays, and the check on the stored key fails.
     */
    @Test
    fun `le preferite tengono l'ordine e la disposizione non scelta resta tale`() {
        val salvate = listOf("Pictures/Neve", "DCIM/Camera", "Pictures/Albe")
        runBlocking { SettingsStore.save(app, Settings(favouriteFolders = salvate)) }
        val lette = runBlocking { SettingsStore.flow(app).first() }
        assertEquals(salvate, lette.favouriteFolders)
        assertNull(lette.folderOrder)
        assertNull("la chiave della disposizione non si scrive", runBlocking { storedPreferences(app) }[FOLDER_ORDER])

        runBlocking { SettingsStore.save(app, lette.copy(folderOrder = FolderOrder.RECENT)) }
        assertEquals(FolderOrder.RECENT, runBlocking { SettingsStore.flow(app).first() }.folderOrder)
        assertEquals(
            "un doppione e le righe vuote si tolgono in lettura",
            listOf("A", "B"), favouritesOf("A\n\nB\nA")
        )
    }

    /**
     * **Case 4: the long press asks the action, and the favourite key flips at once.**
     *
     * ⚠️ **Counter-tested** by calling `onVisibility` from the favourite key: the window of hiding
     * opens and no favourite arrives.
     */
    @Test
    fun `il tocco lungo chiede l'azione e la preferita si aggiunge subito`() {
        var scelta: FolderSelection? = null
        var chiuso = false
        val mare = cartelle[1]
        scene.setContent {
            AivTheme(darkTheme = false) {
                FolderHoldDialog(mare, FolderSelection(), {}, {}, { scelta = it }, { chiuso = true })
            }
        }
        scene.onNodeWithText(app.getString(R.string.folder_action_title)).assertExists()
        scene.onNodeWithText(app.getString(R.string.favourite_add)).performClick()
        assertEquals(listOf("Pictures/Mare"), scelta?.favourites)
        assertTrue(chiuso)
    }

    /** **Case 5: 'Nascondi' goes on to the question of hiding, as before the `5.80`.** */
    @Test
    fun `nascondi prosegue nella domanda di sempre`() {
        val mare = cartelle[1]
        scene.setContent {
            AivTheme(darkTheme = false) {
                FolderHoldDialog(mare, FolderSelection(favourites = listOf("Pictures/Mare")), {}, {}, {}, {})
            }
        }
        scene.onNodeWithText(app.getString(R.string.favourite_remove)).assertExists()
        scene.onNodeWithText(app.getString(R.string.hide_folder_do)).performClick()
        scene.onNodeWithText(app.getString(R.string.hide_folder_title, "Mare")).assertExists()
        scene.onNodeWithText(app.getString(R.string.folder_action_title)).assertDoesNotExist()
    }

    /**
     * **Case 6: while the order is not chosen the home asks it, and the answer is the order.**
     *
     * ⚠️ **Counter-tested** by asking only when the order is `ALPHA`: the question does not appear,
     * and the test fails on its first line.
     */
    @Test
    @Config(shadows = [ArchivioAperto::class])
    fun `senza disposizione la casa la chiede una volta`() {
        runBlocking { Hint.COLUMNS.remember(app) }
        var scelta: FolderSelection? = null
        scene.setContent {
            AivTheme(darkTheme = false) {
                Casa(cartelle, selection = FolderSelection(), onSelectionChange = { scelta = it })
            }
        }
        scene.onNodeWithText(app.getString(R.string.folder_order_ask)).assertExists()
        scene.onNodeWithText(app.getString(R.string.folder_order_recent)).performClick()
        assertEquals(FolderOrder.RECENT, scelta?.order)
    }
}

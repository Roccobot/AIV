package io.github.roccobot.aiv

import android.app.Application
import android.content.Intent
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The actions of the long press on the app icon, from 6.10 (choices A1, A2, A4 and B1; `Shortcuts`).
 *
 * ⚠️ **The decisions live in the model**, as in `RitornoTest`: `handleIntent` is called without
 * mounting anything, and what is measured is where the screen goes and which view is in force.
 * ⚠️ **What this bench does NOT measure**: how a launcher shows the menu, and whether it clears the
 * app's task when an action is touched. That is looked at on the phone, in the round's test.
 * ⚠️⚠️ **[OmbraArchivio] is mounted because `openGrid` reads the folder in the background**, and the
 * reading asks `Environment.isExternalStorageManager()`, which Robolectric does not cover: without
 * the shadow it fails on its own thread, and the failure is charged to the next class that runs a
 * Compose rule. Up to 6.10 nobody saw it; on the 6.20 bench `AggiornaTest` and `SelezioneTest` fell
 * for it, and the order `CopertinaTest`, `ScorciatoieTest`, `SelezioneTest` shows it every time.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [OmbraArchivio::class])
class ScorciatoieTest {

    private val app = ApplicationProvider.getApplicationContext<Application>()

    private fun modello(): ViewerViewModel = ViewerViewModel(app)

    private fun azione(action: String): Intent = Intent(action)

    /**
     * **His condition on A1**: the Feed of the shortcut is for this time, and the app opened from
     * its icon goes back to the saved view.
     *
     * ⚠️ The second half is the case with the app already open (`fresh = false`), which is the one
     * where the guard of 2.78 returns early: the reset of the Feed has to come before it.
     */
    @Test
    fun `il Feed della scorciatoia vale una volta e l'icona riporta alla vista salvata`() {
        val model = modello()
        model.handleIntent(azione(Shortcuts.FEED), fresh = true)
        assertEquals(Screen.Folders(forStart = false), model.screen)
        assertEquals(FolderView.FEED, model.shownView(FolderView.GRID))
        model.handleIntent(Intent(Intent.ACTION_MAIN), fresh = false)
        assertEquals(FolderView.GRID, model.shownView(FolderView.GRID))
    }

    /** A view chosen by hand ends the Feed of the shortcut, even the view already saved. */
    @Test
    fun `una vista scelta a mano chiude il Feed della scorciatoia`() {
        val model = modello()
        model.handleIntent(azione(Shortcuts.FEED), fresh = true)
        model.chooseView(FolderView.GRID)
        assertEquals(FolderView.GRID, model.shownView(FolderView.GRID))
    }

    /** 'Cerca' opens the search over the whole gallery, also with the app already at work. */
    @Test
    fun `Cerca apre la ricerca anche ad app aperta`() {
        val model = modello()
        model.handleIntent(Intent(Intent.ACTION_MAIN), fresh = true)
        model.chooseStartFolder()
        model.handleIntent(azione(Shortcuts.SEARCH), fresh = false)
        assertEquals(Screen.Search(null, null), model.screen)
    }

    /** 'Apri un indirizzo' opens the address window over the home. */
    @Test
    fun `Apri un indirizzo apre la finestra sopra la casa`() {
        val model = modello()
        model.handleIntent(azione(Shortcuts.ADDRESS), fresh = true)
        assertEquals(Screen.Folders(forStart = false), model.screen)
        assertTrue(model.addressAsked)
    }

    /** The three fixed actions are published, in the order A1, A2, A4, with their intents. */
    @Test
    fun `le tre azioni fisse sono pubblicate in ordine`() {
        Shortcuts.publish(app)
        val fisse = ShortcutManagerCompat.getDynamicShortcuts(app).sortedBy { it.rank }
        assertEquals(listOf("feed", "search", "address"), fisse.map { it.id })
        assertEquals(
            listOf(Shortcuts.FEED, Shortcuts.SEARCH, Shortcuts.ADDRESS),
            fisse.map { it.intent.action }
        )
        assertEquals(app.getString(R.string.hub_url), fisse.last().longLabel.toString())
    }

    /**
     * **Choice B1**: a folder opened becomes the fourth action, and the next one takes its place.
     *
     * ⚠️ The count is the proof that the identifier is one: with a new one per folder the menu
     * would grow a line at every folder.
     */
    @Test
    fun `una cartella aperta diventa la quarta azione e la successiva la sostituisce`() {
        Shortcuts.publish(app)
        val model = modello()
        model.openGrid(7L, "Screenshots")
        model.openGrid(9L, "Camera")
        val tutte = ShortcutManagerCompat.getDynamicShortcuts(app)
        assertEquals(4, tutte.size)
        val cartella = tutte.single { it.id == "folder" }
        assertEquals("Camera", cartella.shortLabel.toString())
        assertEquals(9L, cartella.intent.getLongExtra(Shortcuts.BUCKET, 0L))
    }

    /**
     * **The AIV folder holds 3 to 1 on every ground it can have** (his *minimo di contrasto come da
     * norme*): the sixteen folder colours, light variant, and the app's green for a folder without one.
     *
     * ⚠️ Counter-tested with white always: on the app's green and on the lighter colours the contrast
     * falls under 3.
     */
    @Test
    fun `il glifo della cartella tiene 3 a 1 su ogni colore`() {
        val fondi = FRONT_TINTS.map { it.light.toArgb() } + app.getColor(R.color.launcher_background)
        fondi.forEach { fondo ->
            val inchiostro = Shortcuts.folderInk(fondo)
            val contrasto = ColorUtils.calculateContrast(ColorUtils.compositeColors(inchiostro, fondo), fondo)
            assertTrue("contrasto %.2f su #%06X".format(contrasto, fondo and 0xFFFFFF), contrasto >= 3.0)
        }
    }

    /**
     * **A hidden folder does not reach the home screen**: opened while 'Mostra nascoste' is on, it
     * does not become an action.
     */
    @Test
    fun `una cartella aperta con le nascoste in vista non diventa un'azione`() {
        val model = modello()
        model.peek(true)
        model.openGrid(5L, "Privata")
        model.peek(false)
        assertFalse(ShortcutManagerCompat.getDynamicShortcuts(app).any { it.id == "folder" })
    }
}

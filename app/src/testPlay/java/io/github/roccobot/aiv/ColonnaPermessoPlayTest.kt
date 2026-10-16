package io.github.roccobot.aiv

import android.Manifest
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * La colonna delle cartelle senza permesso nella `6.27` (rilievo D28 della revisione della 6.25):
 * offre il tasto per chiederlo, col testo di AIV Play, e si accorge quando il permesso arriva.
 *
 * ⚠️ **È una prova di AIV Play** perché il testo giusto è quello delle immagini, e perché nella
 * variante GitHub il controllo del permesso va in errore nel banco (vedi `ListaDiPrimaPlayTest`).
 */
@RunWith(AndroidJUnit4::class)
class ColonnaPermessoPlayTest {

    @get:Rule
    val banco = createAndroidComposeRule<ComponentActivity>()

    private val app: Application get() = ApplicationProvider.getApplicationContext()

    /**
     * **Senza permesso la colonna dice quello delle immagini e offre il tasto; concesso fuori
     * dall'app, al ritorno lo vede.**
     *
     * ⚠️ **Controprovata** con la colonna di prima: il testo era quello di AIV GitHub, senza il
     * tasto, e la prima asserzione cade.
     */
    @Test
    fun `la colonna chiede il permesso e si accorge quando arriva`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                FolderRailList(
                    buckets = null,
                    selected = null,
                    selection = FolderSelection(),
                    peeking = false,
                    colour = FolderColour.entries.first(),
                    tints = emptyMap(),
                    onPick = {},
                    onHide = {},
                    onUnhide = {},
                    onSelectionChange = {},
                    onRead = {},
                    listIndex = 0,
                    listOffset = 0,
                    onListScroll = { _, _ -> }
                )
            }
        }
        banco.waitForIdle()
        val testo = app.getString(R.string.folders_permission_media)
        banco.onNodeWithText(testo).assertIsDisplayed()
        banco.onNodeWithText(app.getString(R.string.folders_grant)).assertIsDisplayed()

        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_IMAGES)
        banco.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        banco.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        banco.waitForIdle()
        assertTrue(
            "Concesso il permesso, la colonna dice ancora che manca",
            banco.onAllNodesWithText(testo).fetchSemanticsNodes().isEmpty()
        )
    }
}

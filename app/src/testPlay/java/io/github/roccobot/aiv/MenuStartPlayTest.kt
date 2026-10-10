package io.github.roccobot.aiv

import android.Manifest
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/*
 * AIV Play's Start menu, squashed in 5.10 (his note in Altro on the 5.10 round: *Sia il menu
 * 'Start' reale che la sua versione nel micro-onboarding sono difettosi (schiacciati)*).
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️⚠️ **The cause**: without 'Cestino' and 'Cartelle' the home has seven cells, × included, so
 * the top row holds one; the row's height and the panel's room were counted from the top row,
 * that is from one column instead of three. Both the menu and its copy in the hint lay out the
 * same rows (`cornerRows`), and both were squashed.
 * ⚠️ Runs only in the `play` variant: see `AccessoPlayTest`.
 */
@RunWith(AndroidJUnit4::class)
class MenuStartPlayTest {

    @get:Rule
    val banco = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private val dp: Float get() = app.resources.displayMetrics.density

    @Before
    fun prepara() {
        shadowOf(app as android.app.Application).grantPermissions(Manifest.permission.READ_MEDIA_IMAGES)
    }

    /** The home with the Start menu on the right, as it comes from the factory. */
    @Composable
    private fun Home() {
        AivTheme(darkTheme = false) {
            CompositionLocalProvider(
                LocalPillLook provides PillLook(PhonePill.SLIDE, corner = true),
                LocalPadLook provides PadLook(hand = Hand.RIGHT)
            ) {
                Box(modifier = Modifier.fillMaxSize()) { Casa(CARTELLE_START) }
            }
        }
    }

    private fun misura(tag: String) =
        banco.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().single().boundsInRoot

    /** **The open Start menu is three columns wide and square, as in AIV GitHub.** */
    @Test
    fun `il menu Start aperto ha tre colonne ed e quadrato`() {
        runBlocking { Hint.COLUMNS.remember(app) }
        banco.setContent { Home() }
        banco.waitForIdle()
        banco.onNodeWithContentDescription(app.getString(R.string.hub_open)).performClick()
        banco.waitForIdle()
        val pannello = misura(START_PANEL_TAG)
        assertEquals("Il menu Start non è su tre colonne", (3 * 72f + 6f) * dp, pannello.width, 0.5f * dp)
        assertEquals("Il menu Start è schiacciato", pannello.width, pannello.height, 0.5f * dp)
    }

    /** **And so is its copy in the first start's hint.** */
    @Test
    fun `il menu Start del velo ha tre colonne ed e quadrato`() {
        runBlocking { Hint.COLUMNS.forget(app) }
        banco.setContent { Home() }
        aspettaIlVelo(banco, app)
        banco.waitForIdle()
        val pannello = misura(CORNER_COPY_TAG)
        assertEquals("Il menu Start del velo non è su tre colonne", (3 * 72f + 6f) * dp, pannello.width, 0.5f * dp)
        assertEquals("Il menu Start del velo è schiacciato", pannello.width, pannello.height, 0.5f * dp)
    }
}

/** A few folders, so the home shows its grid and the hint. */
private val CARTELLE_START = (1..4).map {
    Folder.Bucket(id = it.toLong(), name = "Cartella $it", pictures = 2, clips = 0, cover = null, path = "/storage/emulated/0/F$it")
}

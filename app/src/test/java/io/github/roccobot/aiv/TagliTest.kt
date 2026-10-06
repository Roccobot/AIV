package io.github.roccobot.aiv

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The hard cuts between two screens, which since 4.36 are a fade of [CUT_FADE_MS] (note A of the
 * 4.35 round, and his message: *una transizione di 100ms ogni volta che c'è un cambio netto di
 * schermata*).
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️ **What it reads is presence**: halfway through a fade the screen that leaves and the one that
 * arrives are both composed, while with a cut the first is gone at the next frame. With the cut
 * put back, both tests fail.
 * ⚠️ **What it does NOT see**: whether the flash he saw is gone, which is a matter of rendering on
 * his phone.
 */
@RunWith(AndroidJUnit4::class)
class TagliTest {

    @get:Rule
    val banco = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /** **A folder and the viewer cross-fade, in both directions.** It goes through [cambioSchermata]. */
    @Test
    fun `cartella e visualizzatore si incrociano`() {
        var schermo by mutableStateOf<Screen>(CARTELLA)
        banco.setContent { Scena(schermo) }
        banco.waitForIdle()
        banco.mainClock.autoAdvance = false

        schermo = Screen.Viewer
        aMeta()
        banco.onNodeWithTag(TAG_CARTELLA).assertExists()
        banco.onNodeWithTag(TAG_VISORE).assertExists()
        aFine()
        banco.onNodeWithTag(TAG_CARTELLA).assertDoesNotExist()

        schermo = CARTELLA
        aMeta()
        banco.onNodeWithTag(TAG_CARTELLA).assertExists()
        banco.onNodeWithTag(TAG_VISORE).assertExists()
        aFine()
        banco.onNodeWithTag(TAG_VISORE).assertDoesNotExist()
    }

    /** **Two pages of the settings cross-fade too**: until 4.35 the `when` swapped them at once. */
    @Test
    fun `due pagine delle impostazioni si incrociano`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                SettingsScreen(
                    settings = Settings(),
                    onChange = {},
                    onStartFolder = {},
                    onResetHints = {},
                    onChooseEditor = {},
                    onBack = {}
                )
            }
        }
        banco.waitForIdle()
        val radice = app.getString(R.string.settings_title)
        val zoom = app.getString(R.string.settings_zoom_page)
        // ⚠️ Scrolled while the clock still runs: see the trap in `ImpostazioniTest`.
        banco.onNodeWithText(app.getString(R.string.settings_group_viewer)).performScrollTo()
        banco.waitForIdle()
        banco.mainClock.autoAdvance = false

        banco.onNodeWithText(app.getString(R.string.settings_group_viewer)).performClick()
        aMeta()
        banco.onNodeWithText(radice).assertExists()
        banco.onNodeWithText(zoom).assertExists()
        aFine()
        banco.onNodeWithText(radice).assertDoesNotExist()
    }

    /** About half of the fade: three frames, 48 ms of 100. */
    // ⚠️ With the clock stopped, a change of state is composed at the first sync, not at the first
    // frame: without `waitForIdle` the three frames ran before the transition had started, and the
    // test failed with the fade there (measured frame by frame: both screens are on for 8 frames).
    private fun aMeta() {
        banco.waitForIdle()
        repeat(3) { banco.mainClock.advanceTimeByFrame() }
    }

    /** Well past the end of the fade, with the frames the transition needs to declare it over. */
    private fun aFine() = repeat(20) { banco.mainClock.advanceTimeByFrame() }
}

/** The app's own transition, with an empty folder and an empty viewer. */
@Composable
private fun Scena(schermo: Screen) {
    AivTheme(darkTheme = false) {
        AnimatedContent(
            targetState = schermo,
            transitionSpec = { cambioSchermata() },
            label = "schermata"
        ) { quale ->
            val tag = if (quale is Screen.Viewer) TAG_VISORE else TAG_CARTELLA
            Box(Modifier.fillMaxSize().testTag(tag))
        }
    }
}

private val CARTELLA = Screen.Grid(bucket = 1L, name = "prova")
private const val TAG_CARTELLA = "cartella"
private const val TAG_VISORE = "visore"

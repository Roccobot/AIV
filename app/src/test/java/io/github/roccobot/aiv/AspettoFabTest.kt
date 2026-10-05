package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

/*
 * The look of the main buttons reaches the FAB.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️⚠️ Since 4.10 (the user's note E on the 4.04 round: 'Aspetto dei pulsanti principali' *vale
 * anche per il FAB*): until 4.05 the fill reached the pills only, and the FAB stayed solid.
 * ⚠️ It wants real graphics (`@GraphicsMode(NATIVE)`), so it lives in a class of its own.
 * ⚠️ Counter-proved: with the FAB's `Surface` painting `fondo` again, the two pixels are equal.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AspettoFabTest {

    @get:Rule
    val banco = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /** As in `PillolaTest`: without it the onboarding veil covers the FAB. */
    @Before
    fun onboardingGiaVisto() {
        runBlocking {
            Hint.COLUMNS.remember(app)
            Hint.BIN_EMPTY.remember(app)
        }
    }

    /**
     * **Solid, the FAB's background is its colour; transparent, it lets the page through.**
     *
     * ⚠️ **The pixel is on the FAB's left edge, halfway up**: inside the rounded square and away
     * from the mark in the middle, so it measures the background and nothing else.
     */
    @Test
    fun `l'aspetto trasparente arriva al FAB`() {
        var fill by mutableStateOf(PillFill.SOLID)
        banco.setContent {
            AivTheme(darkTheme = false) {
                CompositionLocalProvider(LocalPillLook provides PillLook(PhonePill.OFF, fill)) {
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
                            onSettings = {}
                        )
                    }
                }
            }
        }
        banco.waitForIdle()
        val pieno = bordo()
        fill = PillFill.TRANSLUCENT
        banco.waitForIdle()
        val trasparente = bordo()
        assertNotEquals("Il FAB non segue l'aspetto dei pulsanti principali", pieno, trasparente)
        // Solid is opaque: nothing of the page comes through.
        assertEquals(1f, pieno.alpha, 0.01f)
    }

    private fun bordo(): Color {
        val fab = banco.onNodeWithContentDescription(app.getString(R.string.pick_actions))
            .captureToImage().toPixelMap()
        return fab[(fab.width * 0.08f).roundToInt(), fab.height / 2]
    }
}

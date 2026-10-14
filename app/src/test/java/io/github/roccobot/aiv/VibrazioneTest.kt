package io.github.roccobot.aiv

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * **One buzz per long press, and the full one** (5.92, his choice B1 of 2026-10-14).
 *
 * ⚠️⚠️ **Born from a double buzz nobody had asked for**: `combinedClickable` buzzes by itself on a
 * long press (`hapticFeedbackEnabled` is on by default, read in the bytecode of Compose
 * Foundation 1.12.0), and the app added [HOLD_BUZZ] on top through [withHaptics]. The presses
 * written with `pointerInput`, like the one that starts a selection in the grid, had the light
 * tick only: he felt the difference (*leggerissimo, diverso da altre vibrazioni*).
 *
 * ⚠️ **What the bench sees and what it does not**: it counts the calls the app makes to
 * [HapticFeedback], through a fake one; how strong a buzz feels is a matter of the phone, and is
 * checked in the round.
 */
@RunWith(AndroidJUnit4::class)
class VibrazioneTest {

    @get:Rule
    val banco = createComposeRule()

    /** A [HapticFeedback] that writes down every buzz it is asked for. */
    private class Conta : HapticFeedback {
        val colpi = mutableListOf<HapticFeedbackType>()
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            colpi += hapticFeedbackType
        }
    }

    /**
     * **Case 1: a menu row, which goes through [withHaptics], buzzes once and in full.**
     *
     * ⚠️ Seen failing with the defect put back (`hapticFeedbackEnabled` left on, [HOLD_BUZZ] set
     * to `TextHandleMove`): two buzzes, `TextHandleMove` and `LongPress`.
     */
    @Test
    fun `una voce di menu vibra una volta sola, piena`() {
        val conta = Conta()
        banco.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides conta) {
                AivTheme(darkTheme = false) {
                    MenuRow(text = VOCE, icon = Icons.Default.Info, onTap = {}, holdLabel = VOCE, onHold = {})
                }
            }
        }
        banco.onNodeWithText(VOCE).performTouchInput { longClick() }
        banco.waitForIdle()
        assertEquals(listOf(HapticFeedbackType.LongPress), conta.colpi)
    }

    /**
     * **Case 2: a text command whose long press comes from its caller ('Data' of the name
     * windows) buzzes once.**
     *
     * ⚠️ It is the other half of the defect: until 5.91 these presses buzzed only through
     * Compose, so turning Compose's buzz off without adding [HOLD_BUZZ] here would have left
     * them mute. Seen failing that way: no buzz at all.
     */
    @Test
    fun `un comando solo testo col tocco lungo vibra una volta sola`() {
        val conta = Conta()
        var lunghi = 0
        banco.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides conta) {
                AivTheme(darkTheme = false) {
                    Quiet(text = VOCE, enabled = true, onTap = {}, onHold = { lunghi++ })
                }
            }
        }
        banco.onNodeWithText(VOCE).performTouchInput { longClick() }
        banco.waitForIdle()
        assertEquals("Il tocco lungo non è arrivato", 1, lunghi)
        assertEquals(listOf(HapticFeedbackType.LongPress), conta.colpi)
    }

    /**
     * **Case 3: no `combinedClickable` in the app keeps Compose's own buzz.**
     *
     * ⚠️⚠️ **Why a reading of the sources and not one more gesture**: eleven files use
     * `combinedClickable`, most of them inside private composables a bench cannot reach, and
     * the next one written would bring the double buzz back without anything noticing. The rule
     * is one: every call turns `hapticFeedbackEnabled` off, and the buzz is [HOLD_BUZZ].
     */
    @Test
    fun `ogni combinedClickable spegne la vibrazione di Compose`() {
        val sorgenti = File("src/main/java")
        assertTrue("Sorgenti non trovati da ${File(".").absolutePath}", sorgenti.isDirectory)
        val senza = mutableListOf<String>()
        sorgenti.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val testo = file.readText()
            var da = 0
            while (true) {
                val i = testo.indexOf("combinedClickable(", da)
                if (i < 0) break
                val fine = chiusa(testo, i + "combinedClickable".length)
                if ("hapticFeedbackEnabled = false" !in testo.substring(i, fine)) {
                    senza += "${file.name}:${testo.substring(0, i).count { it == '\n' } + 1}"
                }
                da = fine
            }
        }
        assertEquals("combinedClickable con la vibrazione di Compose accesa", emptyList<String>(), senza)
    }

    /** The index just past the parenthesis that closes the one opening at [aperta]. */
    private fun chiusa(testo: String, aperta: Int): Int {
        var livello = 0
        for (k in aperta until testo.length) {
            when (testo[k]) {
                '(' -> livello++
                ')' -> if (--livello == 0) return k + 1
            }
        }
        return testo.length
    }

    private companion object {
        const val VOCE = "Voce di prova"
    }
}

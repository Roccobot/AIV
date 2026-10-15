package io.github.roccobot.aiv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The bench of [ValueSlider], the one slider of the app from 6.22.
 *
 * ⚠️⚠️ **It guards the trap of the Material 3 1.5.0-beta01 migration**: the replacement that the
 * deprecation suggests, `rememberSliderState(value, steps, trackRange = valueRange)`, keeps its
 * state with no key, so a value or a range that changes from outside never reaches the thumb.
 * In the app that would be the video bar standing still while the video plays, and a range stuck
 * at 0 because the length of the video arrives after the first frame. With that body in
 * [ValueSlider] the first two tests fail, and the third one too, because there the value moves
 * only through the screen's own state, as it does in every slider of the app.
 *
 * ⚠️ What it reads is the slider's semantics, the value and the range it declares to a screen
 * reader, which are the state the thumb is drawn from: the drawing itself is the same Material
 * slider as before, measured pixel by pixel when the version changed.
 */
@RunWith(AndroidJUnit4::class)
class ValueSliderTest {

    @get:Rule
    val bench = createComposeRule()

    /** The video plays, the screen moves the value, and the thumb follows. */
    @Test
    fun thumbFollowsAValueMovedFromOutside() {
        var value by mutableFloatStateOf(0.2f)
        bench.setContent {
            ValueSlider(value = value, onValueChange = {}, modifier = Modifier.testTag(TAG))
        }
        bench.onNodeWithTag(TAG).assertRangeInfoEquals(ProgressBarRangeInfo(0.2f, 0f..1f))

        value = 0.7f

        bench.onNodeWithTag(TAG).assertRangeInfoEquals(ProgressBarRangeInfo(0.7f, 0f..1f))
    }

    /** The length of a video is 0 until the player knows it, and the track takes the real one. */
    @Test
    fun trackFollowsARangeThatArrivesLater() {
        var length by mutableStateOf(0f)
        var position by mutableFloatStateOf(0f)
        bench.setContent {
            ValueSlider(
                value = position,
                onValueChange = {},
                valueRange = 0f..length,
                modifier = Modifier.testTag(TAG)
            )
        }
        bench.onNodeWithTag(TAG).assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..0f))

        length = 60_000f
        position = 15_000f

        bench.onNodeWithTag(TAG).assertRangeInfoEquals(ProgressBarRangeInfo(15_000f, 0f..60_000f))
    }

    /**
     * A move of the person goes out through `onValueChange`, the screen keeps it, and the thumb
     * shows it; `onValueChangeFinished` arrives once. The move is the semantic one (a screen
     * reader's), which takes the same `onValueChange` as a drag.
     */
    @Test
    fun aMoveGoesThroughTheScreenAndComesBack() {
        var value by mutableFloatStateOf(0f)
        var finished by mutableIntStateOf(0)
        bench.setContent {
            ValueSlider(
                value = value,
                onValueChange = { value = it },
                valueRange = 0f..4f,
                steps = 3,
                onValueChangeFinished = { finished++ },
                modifier = Modifier.testTag(TAG)
            )
        }

        bench.onNodeWithTag(TAG).performSemanticsAction(SemanticsActions.SetProgress) { it(2.2f) }

        bench.onNodeWithTag(TAG).assertRangeInfoEquals(ProgressBarRangeInfo(2f, 0f..4f, 3))
        assertEquals("the value kept by the screen", 2f, value)
        assertEquals("onValueChangeFinished, once", 1, finished)
    }

    private companion object {
        const val TAG = "slider"
    }
}

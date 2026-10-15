package io.github.roccobot.aiv

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * The one slider of the app: a Material 3 [Slider] whose value lives outside it, in the screen
 * that draws it. From 6.22, when Material 3 1.5.0-beta01 deprecates the `Slider(value, ...)` form
 * that the sixteen sliders of the app used.
 *
 * ⚠️⚠️ **It repeats the body of that deprecated form**, read in the beta01 sources (`Slider.kt`):
 * a [SliderState] made again when [steps] or [valueRange] change, and its value written from
 * [value] at every recomposition. So the thumb follows the value when something else moves it
 * (the video that plays, a style loaded, a reset), and the track follows a range that arrives
 * later (the length of a video, which is 0 until the player knows it).
 * ⚠️⚠️ **The replacement the deprecation suggests is not the same thing**:
 * `rememberSliderState(value, steps, trackRange = valueRange)` keeps the state across
 * recompositions with no key, so [value] and [valueRange] only set the start, and the thumb and
 * the track stay where they began. `ValueSliderTest` fails with it.
 *
 * The parameters are the ones the app passes; colours and interaction source keep the defaults
 * of [Slider], as they did before.
 */
@Composable
fun ValueSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val state = remember(steps, valueRange) { SliderState(value, steps, valueRange) }
    state.value = value
    Slider(
        state = state,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
    )
}

package io.github.roccobot.aiv

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * L'unico cursore dell'app: uno [Slider] di Material 3 il cui valore vive fuori, nella schermata
 * che lo disegna (`Rules.md` § '🎛️ Ogni cursore è `ValueSlider`').
 *
 * Ripete il corpo della forma `Slider(value, ...)`, deprecata da Material 3 1.5.0-beta01: uno
 * [SliderState] rifatto quando cambiano [steps] o [valueRange], col valore riscritto da [value] a
 * ogni ricomposizione. Così la manopola segue il valore quando lo muove altro (il video che
 * scorre, uno stile caricato, un azzeramento), e la traccia segue un intervallo che arriva dopo
 * (la durata di un video, che è 0 finché il lettore non la conosce).
 * ⚠️ **Il sostituto che la deprecazione suggerisce non è equivalente**: `rememberSliderState`
 * tiene lo stato senza chiave, quindi valore e intervallo fissano solo la partenza. Presidiato da
 * `ValueSliderTest`, che con quel sostituto fallisce.
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

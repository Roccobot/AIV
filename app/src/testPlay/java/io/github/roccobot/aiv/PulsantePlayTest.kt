package io.github.roccobot.aiv

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Test

/*
 * AIV Play's main control as it comes from the factory (5.12).
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️⚠️ **His note in Altro on the 5.11 round**: *Fino a nuovo ordine, solo per AIV Play, il
 * pulsante predefinito sarà la pillola a scomparsa*. AIV GitHub keeps the corner menu, and
 * `PillolaTest` says so for that variant.
 * ⚠️ Runs only in the `play` variant: see `AccessoPlayTest`.
 */
class PulsantePlayTest {

    /** **A phone that never chose gets the pill that slides out of its round key.** */
    @Test
    fun `di fabbrica AIV Play ha la pillola a scomparsa`() {
        val nuovo = SettingsStore.read(emptyPreferences())
        assertEquals(MainControl.PILL, nuovo.mainControl)
        assertEquals(PhonePill.SLIDE, nuovo.phonePill)
        assertEquals(MainControl.PILL, Settings().mainControl)
    }

    /** **Whoever picked a control keeps it**: the factory value counts only where the store says nothing. */
    @Test
    fun `chi ha scelto il menu Start lo tiene`() {
        val scelto = SettingsStore.read(mutablePreferencesOf(stringPreferencesKey("main-control") to "corner"))
        assertEquals(MainControl.CORNER, scelto.mainControl)
    }
}

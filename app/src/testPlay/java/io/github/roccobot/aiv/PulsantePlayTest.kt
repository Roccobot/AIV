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

    /**
     * **Whoever picked a control that AIV Play still has keeps it**: the factory value counts only
     * where the store says nothing.
     *
     * ⚠️ **Until 5.20 this case was the Start menu**, and since 5.30 AIV Play has no Start menu (his
     * note on the 5.20 round): a store that names it is read as the pill, and
     * `SemplificazioniPlayTest` measures that. The FAB is the control that can still be kept.
     */
    @Test
    fun `chi ha scelto il FAB lo tiene`() {
        val scelto = SettingsStore.read(mutablePreferencesOf(stringPreferencesKey("main-control") to "fab"))
        assertEquals(MainControl.FAB, scelto.mainControl)
    }
}

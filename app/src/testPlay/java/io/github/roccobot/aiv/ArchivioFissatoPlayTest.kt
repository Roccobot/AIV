package io.github.roccobot.aiv

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * In AIV Play il salvataggio non scrive i valori che la variante fissa, dalla `6.29` (rilievo E17
 * della revisione della 6.25, voce B10 del rapporto): la regola di `Store.fit` è che l'archivio non
 * si riscrive, e prima il primo salvataggio li scriveva tutti.
 */
@RunWith(AndroidJUnit4::class)
class ArchivioFissatoPlayTest {

    /** Le chiavi dei valori che `Store.fit` fissa, come le scrive l'archivio. */
    private val fissate = setOf(
        "panel-depth", "sequence-reversed", "main-control", "folder-colour",
        "front-wash", "front-serif", "ext-edit", "ext-download"
    )

    /**
     * **Si salva l'oggetto letto, già fissato: le chiavi fissate restano fuori dall'archivio.**
     *
     * ⚠️ **Controprovata** col salvataggio della `6.28`: c'erano tutte e otto.
     */
    @Test
    fun `il salvataggio non scrive i valori fissati`() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val letto = runBlocking { SettingsStore.flow(app).first() }
        runBlocking { SettingsStore.save(app, letto.copy(gridNames = !letto.gridNames)) }
        val chiavi = runBlocking { storedPreferences(app) }.asMap().keys.map { it.name }.toSet()
        assertTrue("Il salvataggio non ha scritto niente", "grid-names" in chiavi)
        assertEquals("Chiavi fissate scritte nell'archivio", emptySet<String>(), chiavi intersect fissate)
    }

    /** **Il pulsante principale scelto da te si scrive**: la pillola diventa il FAB, e resta. */
    @Test
    fun `il pulsante principale scelto si scrive`() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val letto = runBlocking { SettingsStore.flow(app).first() }
        runBlocking { SettingsStore.save(app, letto.copy(mainControl = MainControl.FAB)) }
        assertEquals(MainControl.FAB, runBlocking { SettingsStore.flow(app).first() }.mainControl)
    }
}

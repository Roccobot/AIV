package io.github.roccobot.aiv

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * L'indicatore dell'ultimo media, e **da che parte cade il suo valore di fabbrica**.
 *
 * ⚠️⚠️ **MISURA LA MIGRAZIONE E NON IL SEGNO, ED È UNA SCELTA DICHIARATA**: la clausola che ha
 * scritto lui (*anche se 'Cornice' è l'impostazione di fabbrica, deve restare 'Angolo', per non
 * stravolgere la UI di chi è già utente*) è la sola cosa qui dentro che possa rompersi **in
 * silenzio**, perché nessuno la vede finché non aggiorna l'app su un telefono già usato. Il
 * disegno invece si vede al primo sguardo, e si guarda sul telefono.
 *
 * ⚠️ **Perché il disegno non passa dal banco**: una miniatura vuole una griglia con dentro delle
 * immagini vere, e il MediaStore di Robolectric è vuoto; montarne una finta misurerebbe la finta,
 * che è la trappola già pagata da `EntrataTest` nella `1.74`.
 */
@RunWith(AndroidJUnit4::class)
class IndicatoreTest {

    /**
     * **Chi installa l'app adesso trova la cornice.**
     *
     * ⚠️ L'archivio vuoto è il segno di un'installazione nuova: in questo store vivono anche i
     * promemoria che l'app scrive da sé, quindi chi l'ha già aperta una volta ha una chiave.
     */
    @Test
    fun `un archivio vuoto riceve la cornice`() = runTest {
        val dopo = MarkMigration.migrate(emptyPreferences())
        assertEquals(LastMark.FRAME.token, dopo[LAST_MARK])
    }

    /**
     * **Chi aggiorna tiene l'angolo, ed è la clausola che lui ha chiesto.**
     *
     * ⚠️ La chiave di prova è una qualunque, e non una in particolare: quello che conta è che
     * l'archivio **porti qualcosa**, cioè che qualcuno abbia già usato questa app.
     */
    @Test
    fun `un archivio con dentro qualcosa tiene l'angolo`() = runTest {
        val prima = mutablePreferencesOf(booleanPreferencesKey("all-files-asked") to true)
        val dopo = MarkMigration.migrate(prima)
        assertEquals(LastMark.CORNER.token, dopo[LAST_MARK])
    }

    /**
     * **La scelta si fa una volta sola: con la chiave scritta, la migrazione non tocca niente.**
     *
     * ⚠️⚠️ **È LA MISURA CHE TIENE IN PIEDI LE ALTRE DUE**: senza, il valore continuerebbe a
     * dipendere da 'quante chiavi ha l'archivio adesso', e una scelta esplicita dell'utente
     * verrebbe riscritta al primo avvio dopo che ha toccato qualunque altra impostazione.
     */
    @Test
    fun `una scelta fatta non si tocca una seconda volta`() = runTest {
        val scelto = mutablePreferencesOf(LAST_MARK to LastMark.CORNER.token)
        assertFalse("la migrazione non deve girare due volte", MarkMigration.shouldMigrate(scelto))
        assertTrue("su un archivio senza la chiave deve girare", MarkMigration.shouldMigrate(emptyPreferences()))
    }
}

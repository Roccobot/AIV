package io.github.roccobot.aiv

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * La domanda di sistema rimasta aperta quando la schermata se ne va, nella `6.27` (rilievo E10
 * della revisione della 6.25): riceve 'no', e il lavoro che la aspettava finisce.
 *
 * ⚠️ **Serve perché dalla `6.27` i lavori sui file vivono in `FileWork`**, e non finiscono più con
 * la schermata: senza la risposta, un lavoro che aspetta una finestra di sistema persa alla
 * ricreazione aspetterebbe per sempre.
 * ⚠️ **La domanda è un permesso che manca**: apre la finestra del sistema e aspetta, che è lo
 * stesso cammino della richiesta del cestino e del consenso di Android 10.
 */
@RunWith(AndroidJUnit4::class)
class DomandaPersaTest {

    @get:Rule
    val banco = createAndroidComposeRule<ComponentActivity>()

    /**
     * **La schermata se ne va con la domanda aperta: la risposta è 'no'.**
     *
     * ⚠️ **Controprovata** senza la risposta all'uscita: la domanda resta senza esito.
     */
    @Test
    fun `una domanda aperta riceve no quando la schermata se ne va`() {
        var inScena by mutableStateOf(true)
        var domanda: SystemAsk? = null
        banco.setContent { if (inScena) domanda = rememberSystemAsk() }
        banco.waitForIdle()

        var esito: Boolean? = null
        FileWork.scope.launch {
            @Suppress("DEPRECATION")
            esito = domanda!!.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        banco.waitForIdle()
        assertNull("La domanda ha risposto da sola", esito)

        inScena = false
        banco.waitForIdle()
        assertEquals("La domanda aperta è rimasta senza risposta", false, esito)
    }
}

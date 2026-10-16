package io.github.roccobot.aiv

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Il lavoro dell'editor completo nella `6.27` (rilievo A9 della revisione della 6.25): attraversa
 * una ricreazione dell'attività, e non sopravvive a un'uscita vera.
 *
 * ⚠️⚠️ **LA RICREAZIONE È QUELLA VERA**: `recreate()` dello scenario, cioè quello che Android fa
 * ruotando il tablet largo o cambiando tema o lingua. Dopo, l'attività nuova non ha contenuto, e la
 * prova lo rimette come farebbe `ViewerActivity`.
 * ⚠️ `@GraphicsMode(NATIVE)`: senza, l'anteprima non si decodifica e la scheda resta spenta.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorRicreatoTest {

    @get:Rule
    val banco = createAndroidComposeRule<ComponentActivity>()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private var salvato: Look? = null

    /** I veli d'aiuto dell'editor si segnano come visti prima di montarlo: vedi [senzaOnboarding]. */
    @Before
    fun veliVisti() {
        senzaOnboarding(Hint.DRAW)
    }

    /**
     * **Un elemento disegnato c'è ancora dopo la ricreazione dell'attività.**
     *
     * ⚠️ **Controprovata** con `keep.close()` chiamata anche durante il cambio di configurazione:
     * dopo la ricreazione 'Salva' è spento, perché l'immagine è tornata intonsa.
     */
    @Test
    fun `il lavoro attraversa la ricreazione dell'attivita`() {
        mostra { Editor() }
        disegnaUnRettangolo()
        assertEquals(1, salva()?.drawing?.marks?.size)

        banco.activityRule.scenario.recreate()
        mostra { Editor() }
        assertEquals("il rettangolo doveva esserci ancora", 1, salva()?.drawing?.marks?.size)
    }

    /**
     * **Uscendo davvero dall'editor e rientrando sulla stessa immagine si riparte da zero.**
     *
     * ⚠️ **Controprovata** togliendo `keep.close()`: rientrando il rettangolo c'è ancora.
     */
    @Test
    fun `uscendo davvero il lavoro si butta`() {
        var aperto by mutableStateOf(true)
        mostra { if (aperto) Editor() }
        disegnaUnRettangolo()
        aperto = false
        banco.waitForIdle()
        aperto = true
        banco.waitForIdle()
        banco.pronta()
        assertNull("l'immagine doveva tornare intonsa", salva())
    }

    private fun mostra(contenuto: @Composable () -> Unit) {
        banco.activityRule.scenario.onActivity { it.setContent { AivTheme(darkTheme = false) { contenuto() } } }
        banco.waitForIdle()
        banco.pronta()
    }

    @Composable
    private fun Editor() {
        Editore(onSave = { look, _ -> salvato = look })
    }

    private fun disegnaUnRettangolo() {
        banco.onNodeWithContentDescription(app.getString(R.string.look_draw)).performScrollTo().performClick()
        banco.waitForIdle()
        val palco = banco.onNodeWithContentDescription(app.getString(R.string.look_compare))
        palco.performTouchInput { down(center) }
        palco.performTouchInput { moveTo(center + Offset(48f, 48f)) }
        palco.performTouchInput { up() }
        banco.waitForIdle()
    }

    /** Tocca 'Salva' e restituisce quello che è arrivato, o `null` se il tasto era spento. */
    private fun salva(): Look? {
        salvato = null
        banco.onNodeWithText(app.getString(R.string.editor_save)).performClick()
        banco.waitForIdle()
        return salvato
    }
}

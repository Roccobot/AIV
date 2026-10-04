package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * La testa di casa sul telefono in orizzontale: l'icona allineata alle cartelle, il nome intero.
 *
 * ⚠️ **Nasce dalla voce `3.72-04` non approvata**, con l'allegato `alignment`: l'icona era più a
 * destra delle cartelle, e 'Image Viewer' era tagliato.
 * ⚠️ **La colonna è più stretta di quella dell'allegato (circa 216 punti), di proposito**: il
 * carattere del banco è più stretto di quello del telefono (MiSans su HyperOS), e a 195 punti il
 * margine vecchio di 24 per lato taglia il nome come lo tagliava sul telefono.
 * ⚠️ `@GraphicsMode(NATIVE)` perché si misura un testo: con la grafica di serie misura una
 * frazione di quello che misura su un telefono, e non verrebbe tagliato mai.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TestaCasaTest {

    @get:Rule
    val banco = createComposeRule()

    @Test
    fun `telefono icona allineata alle cartelle e nome intero`() {
        banco.setContent {
            AivTheme(darkTheme = true) {
                Box(Modifier.size(195.dp, 160.dp)) { RailIdentity(phone = true) }
            }
        }
        banco.waitForIdle()
        val etichetta = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.identity_page)
        val icona = banco.onNodeWithContentDescription(etichetta).getBoundsInRoot()
        assertEquals("L'icona non comincia dove cominciano le cartelle", RAIL_ROW_INSET.value, icona.left.value, 1f)
        val nome = banco.onNodeWithText("Astonishing", substring = true)
        val layout = mutableListOf<TextLayoutResult>()
        nome.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layout)
        // Two lines, 'Astonishing' and 'Image Viewer', and nothing cut.
        assertEquals("Il nome non è su due righe", 2, layout.first().lineCount)
        // ⚠️ Altezza e non larghezza: un nome che non entra va su una terza riga, che maxLines
        // taglia; la larghezza il banco la dichiara traboccata anche quando il testo entra.
        assertFalse("Il nome è tagliato", layout.first().didOverflowHeight)
    }
}

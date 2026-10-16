package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * **I bersagli del dito arrivano a 48 dp, e i nomi provvisori della rinomina in blocco sono
 * visibili** (scelta F1 sulla voce G5 della revisione Setaccio, rilievi dell'area D).
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️ La manopola del riordino si prova col dito, perché il disegno non ha nome per il lettore
 * di schermo: il trascinamento parte da un punto dentro i 48 dp e fuori da quello che il gesto
 * ascoltava prima. Prima ascoltava il solo disegno da 24, che Compose allarga da sé a 48 intorno
 * al centro: misurato col codice vecchio, il dito partiva fino a 44 dp dal bordo della riga e non
 * a 46, perché a destra i 48 finivano oltre la riga. La maniglia della colonna del tablet si
 * prova in `RailListaTest`.
 */
@RunWith(AndroidJUnit4::class)
class BersagliTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **La manopola del riordino si prende a 26 dp a sinistra del centro del disegno.**
     *
     * Il disegno è a 20 dp dal bordo destro della riga (al centro della fascia da 40), e il
     * bersaglio da 48 va da 48 dp a 0 dal bordo: il dito a 46 dp dal bordo è dentro.
     */
    @Test
    fun `la manopola del riordino si prende su tutti i 48 dp`() {
        var mosso: Pair<Int, Int>? = null
        banco.setContent {
            Box(Modifier.width(LARGO.dp).testTag("elenco")) {
                Reorderable(
                    items = listOf("Uno", "Due", "Tre"),
                    onMove = { da, a -> mosso = da to a }
                ) { nome, _ -> Text(nome) }
            }
        }
        banco.waitForIdle()
        banco.onNodeWithTag("elenco").performTouchInput {
            val dp = density
            val partenza = Offset((LARGO - 46) * dp, 28f * dp)
            down(partenza)
            repeat(8) { moveBy(Offset(0f, 8f * dp)) }
            up()
        }
        banco.waitForIdle()
        assertEquals("Il dito sul bordo del bersaglio non ha spostato la riga", 0 to 1, mosso)
    }

    /** **Il nome provvisorio non è nascosto, e tiene l'estensione del file.** */
    @Test
    fun `il nome provvisorio della rinomina non comincia col punto`() {
        val nome = FileTree.parkingName(1_700_000_000_000L, 3, "jpg")
        assertFalse("Il nome provvisorio è un file nascosto: $nome", nome.startsWith("."))
        assertTrue("Il nome provvisorio ha perso l'estensione: $nome", nome.endsWith(".jpg"))
        assertEquals("aiv-1700000000000-3", FileTree.parkingName(1_700_000_000_000L, 3, ""))
    }
}

/**
 * La larghezza dell'elenco della prova, in dp: dentro lo schermo di serie del banco (320), o il
 * dito finirebbe fuori dalla finestra.
 */
private const val LARGO = 300

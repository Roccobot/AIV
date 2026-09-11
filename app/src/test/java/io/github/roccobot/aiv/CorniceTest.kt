package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * La **cornice** dell'ultimo media visualizzato: quanto è spessa e di che colore.
 *
 * ⚠️⚠️ **NASCE DA UNA VOCE NON APPROVATA, ED È LA REGOLA** (`AIV/CLAUDE.md`, § '🧪 Quando si
 * scrive una prova, e quando no'): nel giro della `2.11` la voce `ind-ultimo` è tornata indietro
 * perché il tratto era troppo sottile e troppo poco vivido. Quanto si veda è percezione e il
 * banco non la sa guardare, ma le due cose che la correzione ha cambiato sono fatti: che lo
 * spessore sia una **frazione del lato** invece di una misura fissa, e che il colore sia
 * l'arancione degli onboarding invece del verde acqua di casa.
 *
 * ⚠️⚠️ **MISURA IL MECCANISMO E NON LA GRIGLIA, e non è un ripiego**: una miniatura vuole un
 * MediaStore con dentro delle immagini, che in Robolectric è vuoto (vedi `IndicatoreTest`).
 * Quello che si può montare è il modificatore che disegna il tratto, cioè esattamente il pezzo
 * che la griglia chiama, ed è la stessa strada di `BandeTest` col gradiente.
 *
 * ⚠️ **Che cosa NON vede**: se il segno si distingua su una fotografia vera, che è la cosa per
 * cui il giro con lui esiste.
 */
@RunWith(AndroidJUnit4::class)
/* ⚠️ La grafica vera vale per la classe: senza `NATIVE` la cattura torna vuota. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CorniceTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **Il tratto cresce con la piastrella**, cioè è una frazione del lato e non una misura fissa.
     *
     * ⚠️⚠️ **È LA COSA CHE PUÒ TORNARE INDIETRO IN SILENZIO**: con un valore in punti la cornice
     * si vede giusta sulle tre colonne di un telefono e diventa un filo a due colonne, dove la
     * cella è quasi il doppio. Chi rimettesse un `Dp` vedrebbe questa prova fallire sul secondo
     * lato, che è il solo posto in cui la differenza si manifesta.
     * ⚠️ **La tolleranza è di un pixel per parte**, e serve all'antialiasing: il bordo interno del
     * tratto non cade su un pixel intero.
     */
    @Test
    fun `lo spessore e una frazione del lato`() {
        banco.setContent {
            Column {
                Scena(60.dp, STRETTA)
                Scena(120.dp, LARGA)
            }
        }
        val stretta = spessore(STRETTA)
        val larga = spessore(LARGA)
        assertTrue("il tratto deve esserci: $stretta", stretta > 0)
        assertEquals("a lato doppio il tratto raddoppia", stretta * 2.0, larga.toDouble(), 1.0)
    }

    /**
     * **Il colore è l'arancione degli onboarding e non l'accento di casa.**
     *
     * ⚠️⚠️ **SI MISURA SUL ROSSO CONTRO IL BLU, e i due colori non si somigliano affatto**:
     * l'arancione [HINT_MARK] steso all'80% su un fondo bianco dà un rosso pieno e un blu basso,
     * mentre il verde acqua che c'era prima ha il rosso basso. Un confronto col valore esatto
     * dipenderebbe da come il banco arrotonda la fusione, e non direbbe niente di più.
     */
    @Test
    fun `il tratto e arancione`() {
        banco.setContent { Scena(120.dp, LARGA) }
        val pixel = banco.onNodeWithTag(LARGA).captureToImage().toPixelMap()
        val tinta = pixel[1, pixel.height / 2]
        assertTrue("il rosso deve essere pieno: $tinta", tinta.red > 0.9f)
        assertTrue("il blu deve stare sotto la metà: $tinta", tinta.blue < 0.5f)
        assertTrue("il verde sta in mezzo: $tinta", tinta.green > 0.5f && tinta.green < 0.85f)
    }

    /**
     * Quanti pixel di tratto si contano da sinistra, a metà altezza, sulla cella [tag].
     *
     * ⚠️ **Si conta a metà altezza** perché là il bordo è dritto: vicino a un angolo il tratto
     * è un arco, e un conto orizzontale misurerebbe la curva invece dello spessore.
     */
    private fun spessore(tag: String): Int {
        val pixel = banco.onNodeWithTag(tag).captureToImage().toPixelMap()
        val riga = pixel.height / 2
        var conta = 0
        while (conta < pixel.width && pixel[conta, riga] != Color.White) conta++
        return conta
    }

    /** Un quadrato bianco col solo tratto sopra: il fondo chiaro è quello che lo fa contare. */
    @Composable
    private fun Scena(lato: Dp, tag: String) {
        Box(
            Modifier
                .size(lato)
                .background(Color.White)
                .lastFrame(RoundedCornerShape(4.dp))
                .testTag(tag)
        )
    }

    private companion object {
        const val STRETTA = "stretta"
        const val LARGA = "larga"
    }
}

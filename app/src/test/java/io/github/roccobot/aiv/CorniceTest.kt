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
 * spessore sia una **frazione del lato** invece di una misura fissa, e che il tratto prenda il
 * colore che gli si passa, all'opacità dichiarata.
 *
 * ⚠️⚠️ **LA SECONDA PROVA HA CAMBIATO BERSAGLIO CON LA `2.13`, E IL BERSAGLIO NUOVO È PIÙ FORTE**:
 * fino alla `2.12` misurava che il tratto fosse **arancione**, cioè ricopiava una costante, e
 * sarebbe diventata rossa per una decisione invece che per un difetto (è successo: il colore è
 * cambiato su sua richiesta). Adesso il colore lo passa il chiamante, e quello che si misura è il
 * **legame**: che il tratto sia esattamente quel colore fuso al [MARK_FRAME_ALPHA] sul fondo. Chi
 * rimettesse una costante dentro [lastFrame] la vedrebbe fallire.
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
     * **Il tratto porta il colore che riceve, all'opacità dichiarata.**
     *
     * ⚠️ **Il colore atteso si CALCOLA invece di scriverlo**: è la tinta passata, fusa al
     * [MARK_FRAME_ALPHA] sul bianco del fondo. Un valore scritto a mano direbbe soltanto come il
     * banco arrotonda, e cadrebbe al primo ritocco dell'opacità senza che nulla sia rotto.
     * ⚠️ **La tolleranza è di un centesimo per canale**, che è l'arrotondamento a otto bit: un
     * confronto esatto fra due `Float` fallirebbe per l'ultimo decimale.
     */
    @Test
    fun `il tratto prende il colore che riceve`() {
        banco.setContent { Scena(120.dp, LARGA, PROVA) }
        val pixel = banco.onNodeWithTag(LARGA).captureToImage().toPixelMap()
        val tinta = pixel[1, pixel.height / 2]
        val a = MARK_FRAME_ALPHA
        assertEquals("rosso: $tinta", PROVA.red * a + (1 - a), tinta.red, 0.01f)
        assertEquals("verde: $tinta", PROVA.green * a + (1 - a), tinta.green, 0.01f)
        assertEquals("blu: $tinta", PROVA.blue * a + (1 - a), tinta.blue, 0.01f)
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
    private fun Scena(lato: Dp, tag: String, tinta: Color = PROVA) {
        Box(
            Modifier
                .size(lato)
                .background(Color.White)
                .lastFrame(RoundedCornerShape(4.dp), tinta)
                .testTag(tag)
        )
    }

    private companion object {
        const val STRETTA = "stretta"
        const val LARGA = "larga"

        /**
         * La tinta che la scena passa al tratto.
         *
         * ⚠️ **Non è un colore del tema di proposito**: la prova misura che il tratto usi quello
         * che riceve, e un colore preso da `MaterialTheme` renderebbe verde anche un tratto che se
         * lo va a prendere da sé, cioè proprio il difetto da cui questa prova difende.
         */
        val PROVA = Color(0xFF3366CC)
    }
}

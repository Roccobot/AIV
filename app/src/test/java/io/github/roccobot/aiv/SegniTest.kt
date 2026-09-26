package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Il **tondo** e il **nastro** dell'ultimo media visualizzato: dove stanno, quanto sono grandi,
 * e di che colore.
 *
 * ⚠️⚠️ **IL TONDO NASCE CON LA `2.92`, E QUESTA PROVA NASCE CON LUI** (punto 2 del campo libero
 * del giro della `2.91`: *un pallino colore accento 50% in basso a sinistra nel quadrato di
 * miniatura dell'elemento*). Le tre cose che si possono rompere in silenzio sono le tre della sua
 * frase: il colore alla sua opacità, l'angolo, e la misura, che deve seguire la piastrella perché
 * le colonne le sceglie lui. ⚠️ **L'opacità è il 60% dalla `2.94`**, e la prova non ne ricopia il
 * numero: lo legge da [MARK_DOT_ALPHA].
 *
 * ⚠️⚠️ **E IL NASTRO ENTRA QUI PERCHÉ DIVIDE L'ANGOLO COL TONDO**: fino alla `2.91` si
 * disegnava in coordinate assolute, cioè sempre in basso a sinistra, mentre la spunta e la durata
 * di un filmato seguono il verso della lingua. Da destra a sinistra restava nell'angolo della
 * durata. La prova lo misura nei due versi, e il tondo ha la stessa regola.
 *
 * ⚠️ **Misura il meccanismo e non la griglia**, per la ragione scritta in `CorniceTest`: una
 * miniatura vuole un MediaStore con dentro delle immagini, che in Robolectric è vuoto.
 *
 * ⚠️ **Che cosa NON vede**: se il tondo si distingua su una fotografia vera. La misura l'ha scelta
 * lui guardando le anteprime del documento di feedback (`d-pallino-misura`: `14`).
 */
@RunWith(AndroidJUnit4::class)
/* ⚠️ La grafica vera vale per la classe: senza `NATIVE` la cattura torna vuota. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SegniTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **Il tondo è in basso all'inizio della riga**, cioè a sinistra, e da destra a sinistra
     * passa a destra.
     *
     * ⚠️ **Il centro si trova e non si calcola**: è il baricentro dei pixel tinti, quindi la prova
     * non ricopia né la misura né l'aria, e cade solo se il disco cambia angolo o smette di stare
     * alla stessa distanza dai due bordi che lo chiudono.
     * ⚠️ **La tolleranza è di un pixel**, e serve all'antialiasing del bordo del disco.
     */
    @Test
    fun `il tondo vive in basso a sinistra, e da destra a sinistra in basso a destra`() {
        banco.setContent {
            Column {
                Scena(LATO, SINISTRA) { Modifier.lastDot(PROVA) }
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scena(LATO, DESTRA) { Modifier.lastDot(PROVA) }
                }
            }
        }
        val sx = baricentro(foto(SINISTRA))
        val dx = baricentro(foto(DESTRA))
        val lato = foto(SINISTRA).width.toFloat()
        assertTrue("a sinistra: $sx", sx.first < lato / 4 && sx.second > lato * 3 / 4)
        assertEquals("stessa aria dai due bordi: $sx", sx.first, lato - sx.second, 1f)
        assertTrue("da destra a sinistra: $dx", dx.first > lato * 3 / 4 && dx.second > lato * 3 / 4)
        assertEquals("i due posti si specchiano", lato - sx.first, dx.first, 1f)
        assertEquals("alla stessa altezza", sx.second, dx.second, 1f)
    }

    /**
     * **Il tondo cresce con la piastrella**: a lato doppio il diametro raddoppia.
     *
     * ⚠️⚠️ **È LA COSA CHE PUÒ TORNARE INDIETRO IN SILENZIO**, come lo spessore della cornice: con
     * un diametro in punti il tondo si vedrebbe giusto sulle tre colonne di un telefono e
     * diventerebbe un puntino a due. ⚠️ **Il diametro si conta sulla riga del baricentro**, dove la
     * corda è la più lunga.
     */
    @Test
    fun `il tondo cresce con la piastrella`() {
        banco.setContent {
            Column {
                Scena(LATO / 2, STRETTA) { Modifier.lastDot(PROVA) }
                Scena(LATO, LARGA) { Modifier.lastDot(PROVA) }
            }
        }
        val stretta = diametro(foto(STRETTA))
        val larga = diametro(foto(LARGA))
        assertTrue("il tondo deve esserci: $stretta", stretta > 0)
        assertEquals("a lato doppio il diametro raddoppia", stretta * 2.0, larga.toDouble(), 1.5)
    }

    /**
     * **Il tondo porta il colore che riceve, alla sua opacità.**
     *
     * ⚠️ **Il colore atteso si calcola** dalla tinta passata e da [MARK_DOT_ALPHA], come in
     * `CorniceTest`: un valore scritto a mano direbbe soltanto come il banco arrotonda. Per questo
     * il passaggio dalla metà al 60%, nella `2.94`, non ha toccato il conto.
     */
    @Test
    fun `il tondo prende il colore che riceve alla sua opacita`() {
        banco.setContent { Scena(LATO, LARGA) { Modifier.lastDot(PROVA) } }
        val pixel = foto(LARGA)
        val (cx, cy) = baricentro(pixel)
        val tinta = pixel[cx.toInt(), cy.toInt()]
        val a = MARK_DOT_ALPHA
        assertEquals("rosso: $tinta", PROVA.red * a + (1 - a), tinta.red, 0.01f)
        assertEquals("verde: $tinta", PROVA.green * a + (1 - a), tinta.green, 0.01f)
        assertEquals("blu: $tinta", PROVA.blue * a + (1 - a), tinta.blue, 0.01f)
    }

    /**
     * **Il nastro si specchia da destra a sinistra**, e fino alla `2.91` non lo faceva.
     *
     * ⚠️ **Si guardano i due angoli in basso**, a due pixel dai bordi: là il triangolo è pieno, e
     * l'angolo opposto deve restare bianco.
     */
    @Test
    fun `il nastro si specchia da destra a sinistra`() {
        val forma = RoundedCornerShape(4.dp)
        banco.setContent {
            Column {
                Scena(LATO, SINISTRA) { Modifier.lastCorner(forma, PROVA) }
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scena(LATO, DESTRA) { Modifier.lastCorner(forma, PROVA) }
                }
            }
        }
        val sx = foto(SINISTRA)
        val dx = foto(DESTRA)
        val giu = sx.height - 1 - BORDO
        val ultimo = sx.width - 1 - BORDO
        assertTrue("a sinistra, angolo di sinistra", tinto(sx[BORDO, giu]))
        assertTrue("a sinistra, angolo di destra bianco", !tinto(sx[ultimo, giu]))
        assertTrue("da destra a sinistra, angolo di destra", tinto(dx[ultimo, giu]))
        assertTrue("da destra a sinistra, angolo di sinistra bianco", !tinto(dx[BORDO, giu]))
    }

    private fun foto(tag: String): PixelMap =
        banco.onNodeWithTag(tag).captureToImage().toPixelMap()

    /** Se un pixel porta del segno: si scosta dal bianco del fondo in almeno un canale. */
    private fun tinto(c: Color): Boolean =
        abs(c.red - 1f) > SOGLIA || abs(c.green - 1f) > SOGLIA || abs(c.blue - 1f) > SOGLIA

    /**
     * Il baricentro dei pixel tinti, come coppia (x, y) nelle coordinate della scena.
     *
     * ⚠️ **Conta dal CENTRO di ogni pixel**, cioè dall'indice più mezzo: la prima stesura contava
     * gli indici, e la distanza dal bordo di sinistra veniva un pixel più corta di quella dal
     * bordo di sotto, con un disco disegnato giusto. Un pixel di indice `x` copre da `x` a `x + 1`.
     */
    private fun baricentro(pixel: PixelMap): Pair<Float, Float> {
        var sx = 0.0
        var sy = 0.0
        var n = 0
        for (y in 0 until pixel.height) for (x in 0 until pixel.width) {
            if (tinto(pixel[x, y])) {
                sx += x + 0.5
                sy += y + 0.5
                n++
            }
        }
        assertTrue("nessun pixel tinto", n > 0)
        return (sx / n).toFloat() to (sy / n).toFloat()
    }

    /** Quanti pixel tinti ci sono sulla riga del baricentro, cioè la corda più lunga del disco. */
    private fun diametro(pixel: PixelMap): Int {
        val riga = baricentro(pixel).second.toInt()
        return (0 until pixel.width).count { tinto(pixel[it, riga]) }
    }

    /** Un quadrato bianco col solo segno sopra: il fondo chiaro è quello che lo fa contare. */
    @Composable
    private fun Scena(lato: Dp, tag: String, segno: () -> Modifier) {
        Box(
            Modifier
                .size(lato)
                .background(Color.White)
                .then(segno())
                .testTag(tag)
        )
    }

    private companion object {
        const val SINISTRA = "sinistra"
        const val DESTRA = "destra"
        const val STRETTA = "stretta"
        const val LARGA = "larga"

        /** Il lato della scena: abbastanza da dare al tondo una ventina di pixel. */
        val LATO = 160.dp

        /** A quanti pixel dai bordi si guardano gli angoli del nastro. */
        const val BORDO = 2

        /** Quanto un canale si deve scostare dal bianco per contare come segno. */
        const val SOGLIA = 0.02f

        /**
         * La tinta che la scena passa al segno. ⚠️ **Non è un colore del tema di proposito**, per
         * la ragione scritta in `CorniceTest`: la prova misura che il segno usi quello che riceve.
         */
        val PROVA = Color(0xFF3366CC)
    }
}

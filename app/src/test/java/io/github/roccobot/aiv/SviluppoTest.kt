package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.abs

/**
 * La riga su cui i due moduli si sovrappongono: la **seconda** di tutti e due, cioè il contrasto
 * della Luce e la tinta del Colore. È il numero che il difetto della `2.19` metteva in comune.
 */
private const val RIGA = 1

/**
 * Il banco del modulo **Colore**, e della fila che sceglie i moduli.
 *
 * ⚠️⚠️ **NON SI CHIAMA `ColoreTest` PERCHÉ QUEL NOME È GIÀ PRESO, e da un'altra cosa**: là vive
 * il colore di una **cartella**, cioè la tinta della sua intestazione. Qui si sviluppa
 * un'immagine, che è il verbo con cui `AIV/CLAUDE.md` distingue i due editor, e due prove
 * omonime in un repository che ne parla in ogni giro sono due cose che prima o poi qualcuno
 * scambia.
 *
 * ⚠️⚠️ **NASCE CON LA FUNZIONE E NON DOPO UN DIFETTO**, ed è il caso proattivo di
 * `AIV/CLAUDE.md` § '🧪 Quando si scrive una prova, e quando no': col secondo modulo i cursori
 * **non si vedono più tutti insieme**, quindi un gettone che non cambiasse l'elenco, o un 'Reset
 * modulo' che azzerasse anche l'altro, sarebbero difetti che il compilatore non vede e che si
 * scoprono solo muovendo un cursore e guardando sparire il lavoro fatto altrove.
 *
 * ⚠️ **Che cosa questo banco NON vede**: i pixel che escono dal conto, perché una prova gira
 * senza scheda grafica. Che la temperatura scaldi davvero e che la vividezza lasci stare i colori
 * già accesi si guardano sul telefono, e la voce di collaudo li chiede.
 */
@RunWith(AndroidJUnit4::class)
// ⚠️ La grafica vera serve per DECODIFICARE il PNG dell'anteprima: senza, la scheda resta
// spenta e non c'è niente da toccare. Vedi la stessa riga in `LuceTest`.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SviluppoTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **Caso 1: sotto la soglia il Colore è a riposo, e il bianco e nero non ha soglia.**
     *
     * ⚠️ **Il bianco e nero è un interruttore e non un cursore**: acceso, il modulo non è a
     * riposo qualunque cosa dicano gli altri quattro valori, e senza questa riga un'immagine
     * portata in bianco e nero si salverebbe **senza perdita**, cioè tornerebbe a colori.
     */
    @Test
    fun `sotto la soglia il colore e a riposo`() {
        assertTrue(Chroma.NONE.idle)
        assertTrue(Chroma(temp = 0.0001f).idle)
        assertTrue(Chroma(vibrance = -0.0004f).idle)
        assertFalse(Chroma(saturation = 0.01f).idle)
        assertFalse(Chroma(mono = true).idle)
    }

    /**
     * **Caso 2: un valore di Colore toglie il senza perdita, come uno di Luce.**
     *
     * ⚠️⚠️ **È LA CLAUSOLA DELL'UTENTE LETTA SUL MODULO NUOVO** (*quelle che non prevedono la
     * riscrittura del file pixel per pixel devono essere lossless*): `Look.lossless` guardava il
     * solo modulo Luce, quindi prima della `2.19` un'immagine con la sola temperatura cambiata si
     * sarebbe dichiarata senza perdita e sarebbe stata salvata **senza il conto applicato**.
     */
    @Test
    fun `un valore di colore toglie il senza perdita`() {
        assertTrue(Look.NONE.lossless)
        assertFalse(Look(chroma = Chroma(temp = 0.4f)).lossless)
        assertFalse(Look(chroma = Chroma(mono = true)).lossless)
        assertFalse(Look(chroma = Chroma(temp = 0.4f)).idle)
    }

    /**
     * **Caso 3: la fila cambia i cursori che si vedono.**
     *
     * ⚠️ Col secondo modulo i cursori non stanno più tutti sulla scheda, quindi un gettone che
     * non cambiasse l'elenco lascerebbe il Colore irraggiungibile senza dare nessun errore.
     */
    @Test
    fun `il gettone cambia i cursori in scena`() {
        banco.setContent { Scena() }
        pronta()

        banco.onNodeWithText(testo(R.string.look_exposure)).assertExists()
        assertEquals(0, quanti(testo(R.string.look_saturation)))

        banco.onNodeWithText(testo(R.string.look_color)).performClick()
        banco.waitForIdle()

        banco.onNodeWithText(testo(R.string.look_saturation)).assertExists()
        assertEquals(0, quanti(testo(R.string.look_exposure)))
    }

    /**
     * **Caso 4: il tocco lungo su un gettone azzera QUEL modulo e lascia stare l'altro.**
     *
     * ⚠️⚠️ **È IL 'RESET MODULO' CHE LUI HA CHIESTO** (campo libero del giro della `2.14`), e la
     * metà che conta è la seconda: un azzeramento che si portasse via anche la Luce sarebbe
     * 'Originale', che è lì accanto, e il lavoro fatto nell'altro modulo sparirebbe senza che
     * niente lo annunci.
     */
    @Test
    fun `il tocco lungo su un gettone azzera solo il suo modulo`() {
        banco.setContent { Scena() }
        pronta()

        muovi(1, 0.5f)
        assertTrue(valore(1) > 0.2f)

        banco.onNodeWithText(testo(R.string.look_color)).performClick()
        banco.waitForIdle()
        muovi(0, 0.5f)
        assertTrue(valore(0) > 0.2f)

        banco.onNodeWithText(testo(R.string.look_color)).performTouchInput { longClick() }
        banco.waitForIdle()
        assertEquals("il colore doveva azzerarsi", 0f, valore(0), 1e-3f)

        banco.onNodeWithText(testo(R.string.look_light)).performClick()
        banco.waitForIdle()
        assertTrue("la luce non doveva essere toccata", valore(1) > 0.2f)
    }

    /**
     * **Caso 5: col bianco e nero acceso i due cursori dei colori si spengono.**
     *
     * ⚠️ Là non c'è più niente da saturare, e un cursore che si muove senza cambiare l'immagine
     * si legge come un guasto.
     */
    @Test
    fun `il bianco e nero spegne saturazione e vividezza`() {
        banco.setContent { Scena() }
        pronta()
        banco.onNodeWithText(testo(R.string.look_color)).performClick()
        banco.waitForIdle()

        cursore(2).assertIsEnabled()
        cursore(3).assertIsEnabled()

        banco.onNodeWithText(testo(R.string.look_bw)).performClick()
        banco.waitForIdle()

        cursore(2).assertIsNotEnabled()
        cursore(3).assertIsNotEnabled()
        // ⚠️ La temperatura invece resta viva: il bianco e nero toglie i colori, non la
        // differenza fra una luce calda e una fredda, che sui grigi si vede come densità.
        cursore(0).assertIsEnabled()
    }

    /**
     * **Caso 6: il dito su un cursore del Colore non muove quello della Luce.**
     *
     * ⚠️⚠️ **QUESTA PROVA NON MISURA IL DIFETTO DEL GIRO DELLA `2.19`, E VA DETTO** (campo libero:
     * *il mio tocco, mentre provo a spostare la tinta o la saturazione, sposta invece il contrasto
     * che è nell'altro modulo*): quel difetto **sul banco non si riproduce**, e questo caso è
     * rimasto verde anche togliendo a mano la correzione, in tutte e due le sue metà. Una spia
     * messa dentro il gesto ha detto perché: a rispondere è sempre il cursore che si tocca, sia
     * col tocco secco sia con un trascinamento vero. Quello che questa prova presidia è la
     * **funzione** (un cursore scrive il proprio campo e nient'altro), non la causa di quel
     * difetto, che resta non accertata.
     *
     * ⚠️⚠️ **SI TOCCA COL DITO, E LE ALTRE PROVE NON LO FANNO**: i cinque casi qui sopra muovono i
     * cursori con l'azione semantica, che vive in un `Modifier.semantics` e si riscrive a ogni
     * ricomposizione. Il dito passa invece dal `pointerInput`, che è un'altra strada e va
     * percorsa da qualcuno.
     *
     * ⚠️ **I due cursori sono lo STESSO numero di riga**, ed è il punto: la tinta è la seconda del
     * Colore e il contrasto la seconda della Luce, cioè la coppia che lui ha nominato.
     */
    @Test
    fun `il cursore di un modulo non muove quello dell'altro`() {
        banco.setContent { Scena() }
        pronta()

        banco.onNodeWithText(testo(R.string.look_color)).performClick()
        banco.waitForIdle()
        tocca(RIGA, 0.25f)
        assertTrue("Il tocco non ha mosso la tinta", abs(valore(RIGA)) > 0.1f)

        banco.onNodeWithText(testo(R.string.look_light)).performClick()
        banco.waitForIdle()
        assertEquals(
            "Il tocco sulla tinta ha mosso il contrasto, che è nell'altro modulo",
            0f,
            valore(RIGA),
            1e-3f
        )
    }

    /** Vedi la nota su `pronta()` in `LuceTest`: il palco compare quando l'immagine esiste. */
    private fun pronta() {
        banco.waitUntil(5_000) {
            banco.onAllNodesWithContentDescription(testo(R.string.look_compare))
                .fetchSemanticsNodes().isNotEmpty()
        }
        banco.waitForIdle()
    }

    private fun cursore(quale: Int) =
        banco.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))[quale]

    private fun muovi(quale: Int, a: Float) {
        cursore(quale).performSemanticsAction(SemanticsActions.SetProgress) { it(a) }
        banco.waitForIdle()
    }

    /**
     * Tocca la barra di un cursore a una frazione della sua larghezza, come farebbe un dito.
     *
     * ⚠️ **Un tocco secco e non una strisciata**: toccando lontano dal tondo il cursore salta
     * subito al punto, quindi il valore si muove senza iniettare un gesto con la sua durata, che
     * col clock di prova è una delle trappole di casa.
     */
    private fun tocca(quale: Int, dove: Float) {
        cursore(quale).performTouchInput { click(Offset(width * dove, height / 2f)) }
        banco.waitForIdle()
    }

    private fun valore(quale: Int): Float =
        cursore(quale).fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo].current

    private fun quanti(detto: String): Int =
        banco.onAllNodesWithText(detto).fetchSemanticsNodes().size

    private fun testo(id: Int, vararg args: Any): String = app.getString(id, *args)

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    @Composable
    private fun Scena() {
        AivTheme(darkTheme = false) {
            Box(modifier = Modifier.fillMaxSize()) {
                AdvancedEditorScreen(uri = quadrato(), busy = false, onSave = {}, onBack = {})
            }
        }
    }

    /** Un PNG vero su disco, e il suo indirizzo: senza, i comandi restano spenti. */
    private fun quadrato(): Uri {
        val file = File(app.cacheDir, "sviluppo.png")
        if (!file.exists()) {
            val mappa = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            mappa.eraseColor(Color.WHITE)
            file.outputStream().use { mappa.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return Uri.fromFile(file)
    }
}

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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.down
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlinx.coroutines.runBlocking

/** Il lato del PNG di prova, in pixel. */
private const val LATO = 64

/**
 * Quanto il dito si sposta oltre la soglia del trascinamento, in pixel del banco: vedi la stessa
 * costante in `LuceTest`.
 */
private const val PASSO = 48f

/**
 * Il modulo **Disegno**, prima fase (`4.40`): il modello, il conto che lo posa e lo salva, e il
 * gesto sul palco.
 *
 * ⚠️ **La grafica vera serve due volte**: per decodificare l'anteprima, senza la quale la scheda
 * resta spenta, e per leggere i pixel che `Draw` dipinge.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DisegnoTest {

    @get:Rule
    val banco = createComposeRule()

    /** I veli d'aiuto si spengono prima: consumano il primo tocco (vedi `SviluppoTest`). */
    @Before
    fun senzaOnboarding() {
        runBlocking {
            Hint.MODULES.remember(ApplicationProvider.getApplicationContext())
            Hint.EDITOR_TOOLS.remember(ApplicationProvider.getApplicationContext())
        }
    }

    /**
     * **La posa del disegno è quella dell'immagine, in tutte e otto le pose.**
     *
     * ⚠️ `Draw.posed` riscrive il conto di `spunBy` invece di chiamarlo, perché lavora su un punto
     * e non su una bitmap: qui si confrontano i due su un pixel solo, in un'immagine non quadrata,
     * dove un quarto di giro dalla parte sbagliata cade altrove.
     * ⚠️ **CONTROPROVATA** togliendo lo specchio da `Draw.posed`: cade alla prima posa specchiata.
     */
    @Test
    fun `il disegno si posa come l'immagine in tutte le otto pose`() {
        val w = 6
        val h = 4
        val sorgente = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        sorgente.setPixel(1, 0, Color.RED)
        for (mirror in listOf(false, true)) for (turns in 0..3) {
            val girata = sorgente.spunBy(turns, mirror)
            val dove = (0 until girata.width * girata.height)
                .first { girata.getPixel(it % girata.width, it / girata.width) == Color.RED }
            val punto = floatArrayOf(1.5f, 0.5f)
            Draw.posed(w, h, Spin(turns, mirror)).mapPoints(punto)
            assertEquals("x, giri $turns, specchio $mirror", dove % girata.width + 0.5f, punto[0], 1e-3f)
            assertEquals("y, giri $turns, specchio $mirror", dove / girata.width + 0.5f, punto[1], 1e-3f)
        }
    }

    /**
     * **Il salvataggio dipinge nella posa, e senza disegno restituisce l'immagine com'è.**
     *
     * ⚠️ Un segmento verticale nell'originale diventa orizzontale dopo un quarto di giro: è il
     * caso in cui un disegno dipinto nella cornice sbagliata cade fuori dal punto atteso.
     */
    @Test
    fun `il salvataggio dipinge il segno nella posa dell'immagine`() {
        val bianca = Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        assertSame("senza disegno l'immagine doveva passare intatta", bianca, Draw.onto(bianca, Drawing.NONE, Spin.STILL))

        // In the original (20 x 40) a vertical line at x = 0.5; after a quarter turn the posed
        // image is 40 x 20 and the line is horizontal, across the middle.
        val segno = Mark(Pen.LINE, listOf(Offset(0.5f, 0.1f), Offset(0.5f, 0.9f)), Color.BLUE, 0.05f, false, false)
        val immutabile = bianca.copy(Bitmap.Config.ARGB_8888, false)
        val fatta = Draw.onto(immutabile, Drawing(listOf(segno)), Spin(1, false))
        assertNotSame("un'immagine immutabile doveva essere copiata", immutabile, fatta)
        assertEquals("il centro del segno doveva essere blu", Color.BLUE, fatta.getPixel(20, 10))
        assertEquals("fuori dal segno doveva restare bianco", Color.WHITE, fatta.getPixel(20, 2))
        assertEquals("il segno girato non doveva arrivare ai bordi", Color.WHITE, fatta.getPixel(1, 10))
    }

    /** **Un disegno toglie il senza perdita, e vive dalla parte del 'dove'.** */
    @Test
    fun `un disegno toglie il senza perdita e resta nel confronto`() {
        val segno = Mark(Pen.FREE, listOf(Offset(0.5f, 0.5f)), Color.RED, Draw.WIDTH, false, false)
        val look = Look(drawing = Drawing(listOf(segno)))
        assertFalse("un disegno non è un'immagine intonsa", look.idle)
        assertFalse("un disegno riscrive i pixel", look.lossless)
        assertEquals("il confronto col prima doveva tenere il disegno", look.drawing, look.place.drawing)
    }

    /**
     * **Il modulo è l'ultimo della fila, a destra degli Stili.**
     *
     * ⚠️ È la sua nota D del giro della `4.34`.
     */
    @Test
    fun `il Disegno e l'ultimo modulo della fila`() {
        banco.setContent { Scena() }
        pronta()
        val disegno = banco.onNodeWithContentDescription(testo(R.string.look_draw)).getUnclippedBoundsInRoot()
        val stili = banco.onNodeWithContentDescription(testo(R.string.look_presets)).getUnclippedBoundsInRoot()
        assertTrue("il Disegno doveva venire dopo gli Stili", disegno.left > stili.left)
    }

    /**
     * **Un trascinamento sul palco lascia un segno, con la penna, il colore e il tratto scelti.**
     *
     * ⚠️⚠️ **CONTROPROVATA** due volte: togliendo il ramo del disegno dal gesto del palco il
     * salvataggio non riceve nessun segno, e senza il punto che supera la soglia il rettangolo
     * nasce con un punto solo.
     */
    @Test
    fun `un trascinamento disegna un rettangolo coi parametri scelti`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        banco.onNodeWithContentDescription(testo(R.string.draw_rect)).performClick()
        banco.onNodeWithContentDescription(testo(R.string.ink_blue)).performClick()
        banco.onNodeWithText(testo(R.string.draw_dashed)).performClick()
        banco.waitForIdle()
        trascina()

        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        val look = salvato
        assertNotNull("il segno doveva accendere 'Salva'", look)
        val segni = look!!.drawing.marks
        assertEquals("un trascinamento, un segno", 1, segni.size)
        val segno = segni.single()
        assertEquals(Pen.RECT, segno.pen)
        assertEquals("il rettangolo vive di due vertici opposti", 2, segno.points.size)
        assertTrue("il secondo vertice doveva seguire il dito", segno.points[1].x > segno.points[0].x)
        assertTrue("il secondo vertice doveva seguire il dito", segno.points[1].y > segno.points[0].y)
        assertEquals("il colore scelto", Draw.INKS[3], segno.ink)
        assertTrue("il tratteggio scelto", segno.dashed)
    }

    /** **'Annulla' toglie l'ultimo segno, e solo quello.** */
    @Test
    fun `Annulla toglie l'ultimo segno`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        trascina()
        trascina(dy = -PASSO)
        banco.onNodeWithContentDescription(testo(R.string.editor_undo)).performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        assertEquals("dopo 'Annulla' doveva restare il primo segno", 1, salvato!!.drawing.marks.size)
        assertEquals(Pen.FREE, salvato!!.drawing.marks.single().pen)
    }

    /**
     * **'Riempimento' si accende solo per le forme chiuse.**
     *
     * ⚠️ Lo stesso criterio del 'Filtro BN': un comando che non cambia niente si legge come un
     * guasto.
     */
    @Test
    fun `Riempimento vale solo per rettangolo ed ellisse`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        banco.onNodeWithText(testo(R.string.draw_filled)).assertIsNotEnabled()
        banco.onNodeWithContentDescription(testo(R.string.draw_ellipse)).performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.draw_filled)).assertIsEnabled()
        banco.onNodeWithContentDescription(testo(R.string.draw_arrow)).performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.draw_filled)).assertIsNotEnabled()
    }

    /**
     * ⚠️ **Il gettone si porta in vista prima di toccarlo**: è l'ultimo della fila, e sullo
     * schermo del banco (320dp) cade oltre il bordo destro, dove un tocco non arriva.
     */
    private fun apriDisegno() {
        banco.onNodeWithContentDescription(testo(R.string.look_draw)).performScrollTo().performClick()
        banco.waitForIdle()
    }

    /**
     * Un trascinamento dal centro del palco, in tre chiamate: giù, oltre la soglia, su (vedi le
     * trappole dell'iniezione dei gesti in `Rules.md`).
     */
    private fun trascina(dx: Float = PASSO, dy: Float = PASSO) {
        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        palco.performTouchInput { down(center) }
        palco.performTouchInput { moveTo(center + Offset(dx, dy)) }
        palco.performTouchInput { up() }
        banco.waitForIdle()
    }

    /** Vedi la nota su `pronta()` in `LuceTest`: il palco compare quando l'immagine esiste. */
    private fun pronta() {
        banco.waitUntil(5_000) {
            banco.onAllNodesWithContentDescription(testo(R.string.look_compare))
                .fetchSemanticsNodes().isNotEmpty()
        }
        banco.waitForIdle()
    }

    @Composable
    private fun Scena(onSave: (Look) -> Unit = {}) {
        AivTheme(darkTheme = false) {
            Box(modifier = Modifier.fillMaxSize()) {
                AdvancedEditorScreen(
                    uri = quadrato(),
                    busy = false,
                    marked = false,
                    marking = false,
                    hasMark = false,
                    onMark = {},
                    onMarkSetup = {},
                    resize = Resize.Plan(Resize.Mode.LONG, Resize.DEFAULT_PX),
                    // ⚠️ Spento, come in `LuceTest`: un ridimensionamento accenderebbe 'Salva' a
                    // immagine intonsa.
                    saved = Resize.NONE,
                    resizing = false,
                    onResize = {},
                    onResizeDefault = {},
                    onSave = { look, _ -> onSave(look) },
                    onBack = {}
                )
            }
        }
    }

    private fun quadrato(): Uri {
        val file = File(app.cacheDir, "disegno.png")
        if (!file.exists()) {
            val mappa = Bitmap.createBitmap(LATO, LATO, Bitmap.Config.ARGB_8888)
            mappa.eraseColor(Color.WHITE)
            file.outputStream().use { mappa.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return Uri.fromFile(file)
    }

    private fun testo(id: Int): String = app.getString(id)

    private val app: Context get() = ApplicationProvider.getApplicationContext()
}

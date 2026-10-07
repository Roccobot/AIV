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
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.down
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
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
import org.robolectric.annotation.Config
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
        val segno = Mark(Pen.LINE, listOf(Offset(0.5f, 0.1f), Offset(0.5f, 0.9f)), Color.BLUE, 0.05f, false, null)
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
        val segno = Mark(Pen.FREE, listOf(Offset(0.5f, 0.5f)), Color.RED, Draw.WIDTH, false, null)
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
     * **Un trascinamento sul palco lascia un segno, con lo strumento, il colore e il tratto scelti.**
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
        banco.onNodeWithContentDescription(testo(R.string.draw_dashed)).performClick()
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

    /**
     * **Il riempimento ha colore e opacità suoi, e la freccia lo ignora.**
     *
     * ⚠️ È il suo esempio alla lettera, arrivato a G1 in corso: *un bordo rosso primario e un
     * riempimento bianco 50%*.
     * ⚠️⚠️ **CONTROPROVATA** dando alla freccia il riempimento in `Gaze.penMark`: il secondo segno
     * arriva con un riempimento che niente disegna.
     */
    @Test
    fun `bordo rosso e riempimento bianco al 50 per cento, la freccia senza`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        banco.onNodeWithContentDescription(testo(R.string.draw_rect)).performClick()
        banco.onNodeWithContentDescription(testo(R.string.ink_red)).performClick()
        banco.onNodeWithContentDescription(testo(R.string.draw_filled)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(testo(R.string.ink_white)).performClick()
        // ⚠️ Since 4.42 the factory opacity is about 15% (his values), so the 50% of his example
        // is set on the slider, as he would.
        banco.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))[0]
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        banco.waitForIdle()
        trascina()
        banco.onNodeWithContentDescription(testo(R.string.draw_arrow)).performClick()
        banco.waitForIdle()
        trascina(dy = -PASSO)

        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        val segni = salvato!!.drawing.marks
        assertEquals(2, segni.size)
        assertEquals("il bordo resta rosso", Draw.INKS[0], segni[0].ink)
        assertEquals("il riempimento bianco al 50%", 0x80FFFFFF.toInt(), segni[0].fill)
        assertEquals(Pen.ARROW, segni[1].pen)
        assertEquals("la freccia ignora il riempimento", null, segni[1].fill)
    }

    /**
     * **I valori di fabbrica sono i suoi** (2026-10-07, dopo il giro della `4.41`): spessore al 60%
     * della corsa del cursore, tratto `#FFFF4B3D`, riempimento delle forme `#26FFAE8E`; la freccia
     * nasce senza riempimento.
     * ⚠️⚠️ **CONTROPROVATA** rimettendo i valori della `4.41` (spessore 0,004, nessun riempimento).
     */
    @Test
    fun `i valori di fabbrica sono i suoi`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        assertEquals("il cursore doveva partire dal 60% della corsa", 0.6f,
            (valore() - Draw.WIDTH_MIN) / (Draw.WIDTH_MAX - Draw.WIDTH_MIN), 1e-3f)
        banco.onNodeWithContentDescription(testo(R.string.draw_rect)).performClick()
        banco.waitForIdle()
        trascina()
        banco.onNodeWithContentDescription(testo(R.string.draw_arrow)).performClick()
        banco.waitForIdle()
        trascina(dy = -PASSO)
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        val (rettangolo, freccia) = salvato!!.drawing.marks
        assertEquals("il tratto di fabbrica", 0xFFFF4B3D.toInt(), rettangolo.ink)
        assertEquals("lo spessore di fabbrica", Draw.WIDTH_MIN + 0.6f * (Draw.WIDTH_MAX - Draw.WIDTH_MIN), rettangolo.width, 1e-5f)
        assertEquals("il riempimento di fabbrica", 0x26FFAE8E, rettangolo.fill)
        assertEquals("la freccia nasce senza riempimento", null, freccia.fill)
        assertEquals("la freccia ha lo stesso tratto", rettangolo.ink, freccia.ink)
    }

    private fun valore(): Float =
        banco.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))[0].fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo].current

    /** **Il riempimento si dipinge con la sua opacità, sotto il contorno.** */
    @Test
    fun `il riempimento si dipinge con la sua opacita`() {
        val nera = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLACK) }
        val segno = Mark(
            Pen.RECT, listOf(Offset(0.1f, 0.1f), Offset(0.9f, 0.9f)), Color.RED, 0.02f, false,
            Draw.withAlpha(Color.WHITE, 0.5f)
        )
        val fatta = Draw.onto(nera, Drawing(listOf(segno)), Spin.STILL)
        val dentro = fatta.getPixel(20, 20)
        assertEquals("il bianco al 50% sul nero dà un grigio medio", 128f, Color.red(dentro).toFloat(), 2f)
        assertEquals("fuori dal rettangolo resta nero", Color.BLACK, fatta.getPixel(1, 20))
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
        banco.onNodeWithContentDescription(testo(R.string.draw_filled)).assertIsNotEnabled()
        banco.onNodeWithContentDescription(testo(R.string.draw_ellipse)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(testo(R.string.draw_filled)).assertIsEnabled()
        banco.onNodeWithContentDescription(testo(R.string.draw_arrow)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(testo(R.string.draw_filled)).assertIsNotEnabled()
    }

    /**
     * **Mentre il dito tiene 'Spessore', la punta si vede nella sua misura vera** (R1 del giro della
     * `4.40`): un tondo pieno del colore della linea, in basso a destra sull'immagine, che sparisce
     * quando il dito si alza.
     * ⚠️⚠️ **CONTROPROVATA** spegnendo il tondo nel palco: i pixel del colore della linea restano
     * zero col dito sul cursore.
     * ⚠️ **Su uno schermo da telefono e non su quello di serie del banco**: là il palco è alto 40
     * pixel e l'immagine 30, quindi la punta vera misura meno di un pixel e non si distingue da
     * niente.
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `tenendo Spessore si vede la punta piena del colore della linea`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        val spessore = banco.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))[0]
        spessore.performTouchInput { down(Offset(width * 0.9f, height / 2f)) }
        spessore.performTouchInput { moveTo(Offset(width * 0.95f, height / 2f)) }
        banco.waitForIdle()
        val tenuto = palco.captureToImage().toPixelMap()
        spessore.performTouchInput { up() }
        banco.waitForIdle()
        val lasciato = palco.captureToImage().toPixelMap()
        val rosso = androidx.compose.ui.graphics.Color(Draw.INKS[0])
        assertTrue("col dito sul cursore la punta rossa doveva vedersi", inchiostro(tenuto, rosso) > 20)
        assertEquals("a dito alzato la punta doveva sparire", 0, inchiostro(lasciato, rosso))
        val (cx, cy) = centro(tenuto, rosso)
        assertTrue("la punta doveva stare a destra", cx > tenuto.width / 2)
        assertTrue("la punta doveva stare in basso", cy > tenuto.height / 2)
    }

    /**
     * **Mentre si disegna una forma piccola compare la lente, e una forma grande non la vuole**
     * (R2 del giro della `4.40`, soglia di 1,5 cm sullo schermo).
     * ⚠️ Si confronta il palco col dito giù e a dito alzato: il segno resta in tutti e due, quindi
     * quello che cambia è la sola lente.
     * ⚠️⚠️ **CONTROPROVATA** togliendo la lente dal palco: col segno piccolo la differenza scende
     * a zero.
     */
    @Test
    fun `la lente compare sulle forme piccole e non su quelle grandi`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        banco.onNodeWithContentDescription(testo(R.string.draw_rect)).performClick()
        banco.waitForIdle()
        assertTrue("col rettangolo piccolo doveva comparire la lente", diffDurante(PASSO, PASSO) > 500)
        assertEquals("col rettangolo grande la lente non doveva esserci", 0, diffDurante(-PASSO * 4, PASSO * 4))
    }

    /**
     * **I tasti Tratto, Riempimento e Tratteggio sono disegni, non parole** (voce `4.42-01`): sullo
     * schermo non c'è più la parola, che resta come descrizione per il lettore di schermo.
     * ⚠️⚠️ **CONTROPROVATA** rimettendo il testo nell'etichetta di 'Tratteggio'.
     */
    @Test
    fun `i tre tasti sono disegni con la loro descrizione`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        for (id in listOf(R.string.draw_outline, R.string.draw_filled, R.string.draw_dashed)) {
            assertEquals("il tasto '${testo(id)}' doveva esserci, descritto", 1,
                banco.onAllNodesWithContentDescription(testo(id)).fetchSemanticsNodes().size)
            assertEquals("il tasto '${testo(id)}' non doveva mostrare la parola", 0,
                banco.onAllNodesWithText(testo(id)).fetchSemanticsNodes().size)
        }
    }

    /**
     * **Il tasto Tratto mostra il colore della linea, e lo cambia con lei.**
     * ⚠️⚠️ **CONTROPROVATA** disegnando la linea del tasto in un colore fisso.
     */
    @Test
    fun `il tasto Tratto ha il colore della linea`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        val rosso = androidx.compose.ui.graphics.Color(Draw.INK)
        val blu = androidx.compose.ui.graphics.Color(Draw.INKS[3])
        val prima = banco.onNodeWithContentDescription(testo(R.string.draw_outline)).captureToImage().toPixelMap()
        assertTrue("il tasto doveva essere rosso", inchiostro(prima, rosso) > 20)
        banco.onNodeWithContentDescription(testo(R.string.ink_blue)).performClick()
        banco.waitForIdle()
        val dopo = banco.onNodeWithContentDescription(testo(R.string.draw_outline)).captureToImage().toPixelMap()
        assertTrue("il tasto doveva diventare blu", inchiostro(dopo, blu) > 20)
        assertEquals("del rosso non doveva restare niente", 0, inchiostro(dopo, rosso))
    }

    /**
     * **Il tasto Riempimento mostra il colore del riempimento con l'opacità alzata, minimo 40%**
     * (voce `4.42-01`): il salmone al 15% di fabbrica si vede a circa il 49% sui quadretti bianchi.
     * ⚠️⚠️ **CONTROPROVATA** dipingendo il tasto con l'opacità vera: sul bianco il salmone al 15%
     * è un altro colore, e i pixel attesi scendono a zero.
     */
    @Test
    fun `il tasto Riempimento mostra il riempimento accentuato`() {
        assertEquals(KEY_ALPHA_MIN, keyAlpha(0f), 1e-6f)
        assertEquals(1f, keyAlpha(1f), 1e-6f)
        assertTrue("l'accentuazione deve crescere con l'opacità", keyAlpha(0.3f) > keyAlpha(0.2f))
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        banco.onNodeWithContentDescription(testo(R.string.draw_rect)).performClick()
        banco.waitForIdle()
        val salmone = Draw.FILL or 0xFF000000.toInt()
        val a = keyAlpha((Draw.FILL ushr 24) / 255f)
        fun su(fondo: Float, c: Int) = fondo * (1 - a) + (c and 0xFF) / 255f * a
        val atteso = androidx.compose.ui.graphics.Color(
            su(1f, salmone shr 16), su(1f, salmone shr 8), su(1f, salmone)
        )
        val tasto = banco.onNodeWithContentDescription(testo(R.string.draw_filled)).captureToImage().toPixelMap()
        assertTrue("sui quadretti bianchi il salmone doveva vedersi accentuato", inchiostro(tasto, atteso) > 10)
    }

    /** I pixel che cambiano fra il palco col dito giù dopo il trascinamento e a dito alzato. */
    private fun diffDurante(dx: Float, dy: Float): Int {
        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        palco.performTouchInput { down(center) }
        palco.performTouchInput { moveTo(center + Offset(dx, dy)) }
        banco.waitForIdle()
        val giu = palco.captureToImage().toPixelMap()
        palco.performTouchInput { up() }
        banco.waitForIdle()
        val su = palco.captureToImage().toPixelMap()
        var n = 0
        for (y in 0 until giu.height) for (x in 0 until giu.width) if (giu[x, y] != su[x, y]) n++
        return n
    }

    private fun vicino(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color) =
        kotlin.math.abs(a.red - b.red) < 0.04f && kotlin.math.abs(a.green - b.green) < 0.04f &&
            kotlin.math.abs(a.blue - b.blue) < 0.04f

    private fun inchiostro(mappa: PixelMap, colore: androidx.compose.ui.graphics.Color): Int {
        var n = 0
        for (y in 0 until mappa.height) for (x in 0 until mappa.width) if (vicino(mappa[x, y], colore)) n++
        return n
    }

    private fun centro(mappa: PixelMap, colore: androidx.compose.ui.graphics.Color): Pair<Int, Int> {
        var sx = 0L; var sy = 0L; var n = 0
        for (y in 0 until mappa.height) for (x in 0 until mappa.width) if (vicino(mappa[x, y], colore)) {
            sx += x; sy += y; n++
        }
        return Pair((sx / n).toInt(), (sy / n).toInt())
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

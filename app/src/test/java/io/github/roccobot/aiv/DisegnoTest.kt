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
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.down
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.MaterialTheme

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
            Hint.DRAW.remember(ApplicationProvider.getApplicationContext())
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
    fun `il salvataggio dipinge l'elemento nella posa dell'immagine`() {
        val bianca = Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        assertSame("senza disegno l'immagine doveva passare intatta", bianca, Draw.onto(bianca, Drawing.NONE, Spin.STILL))

        // In the original (20 x 40) a vertical line at x = 0.5; after a quarter turn the posed
        // image is 40 x 20 and the line is horizontal, across the middle.
        val segno = Mark(Pen.LINE, listOf(Offset(0.5f, 0.1f), Offset(0.5f, 0.9f)), Color.BLUE, 0.05f, false, null)
        val immutabile = bianca.copy(Bitmap.Config.ARGB_8888, false)
        val fatta = Draw.onto(immutabile, Drawing(listOf(segno)), Spin(1, false))
        assertNotSame("un'immagine immutabile doveva essere copiata", immutabile, fatta)
        assertEquals("il centro dell'elemento doveva essere blu", Color.BLUE, fatta.getPixel(20, 10))
        assertEquals("fuori dall'elemento doveva restare bianco", Color.WHITE, fatta.getPixel(20, 2))
        assertEquals("l'elemento girato non doveva arrivare ai bordi", Color.WHITE, fatta.getPixel(1, 10))
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
     * **Gli Stili sono sempre l'ultimo modulo a destra, subito dopo il Disegno** (sua nota E sul
     * giro della `4.49`; fino alla `4.49` il Disegno era dopo gli Stili, sua nota D del giro della
     * `4.34`), anche in un ordine salvato che li mette altrove: quello di fabbrica della `4.49`, e
     * uno riordinato a mano con gli Stili in testa.
     * ⚠️⚠️ **CONTROPROVATA** due volte: rimettendo il Disegno in fondo alle due tabelle (la fila
     * mostra gli Stili prima), e togliendo `stylesLast` dalla lettura dell'archivio.
     */
    @Test
    fun `gli Stili sono sempre l'ultimo modulo a destra`() {
        val altri = MOD_KEYS - PadKey.MOD_PRESET - PadKey.MOD_DRAW
        for (salvato in listOf(
            altri + PadKey.MOD_PRESET + PadKey.MOD_DRAW,
            listOf(PadKey.MOD_PRESET) + altri + PadKey.MOD_DRAW
        )) {
            val letto = SettingsStore.read(
                mutablePreferencesOf(stringPreferencesKey("mod-order") to salvato.joinToString(",") { it.token })
            ).modOrder
            assertEquals("gli Stili dovevano tornare in fondo a ${salvato.map { it.token }}",
                altri + PadKey.MOD_DRAW + PadKey.MOD_PRESET, letto)
        }
        banco.setContent { Scena() }
        pronta()
        val disegno = banco.onNodeWithContentDescription(testo(R.string.look_draw)).getUnclippedBoundsInRoot()
        val stili = banco.onNodeWithContentDescription(testo(R.string.look_presets)).getUnclippedBoundsInRoot()
        assertTrue("gli Stili dovevano venire dopo il Disegno", stili.left > disegno.left)
    }

    /**
     * **Un trascinamento sul palco lascia un elemento, con lo strumento, il colore e il tratto scelti.**
     *
     * ⚠️⚠️ **CONTROPROVATA** due volte: togliendo il ramo del disegno dal gesto del palco il
     * salvataggio non riceve nessun elemento, e senza il punto che supera la soglia il rettangolo
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
        // ⚠️ Dashed is on at the factory since 4.50, so the touch turns it off.
        banco.onNodeWithContentDescription(testo(R.string.draw_dashed)).performClick()
        banco.waitForIdle()
        trascina()

        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        val look = salvato
        assertNotNull("l'elemento doveva accendere 'Salva'", look)
        val segni = look!!.drawing.marks
        assertEquals("un trascinamento, un elemento", 1, segni.size)
        val segno = segni.single()
        assertEquals(Pen.RECT, segno.pen)
        assertEquals("il rettangolo vive di due vertici opposti", 2, segno.points.size)
        assertTrue("il secondo vertice doveva seguire il dito", segno.points[1].x > segno.points[0].x)
        assertTrue("il secondo vertice doveva seguire il dito", segno.points[1].y > segno.points[0].y)
        assertEquals("il colore scelto, all'opacità di fabbrica", Draw.withAlpha(Draw.INKS[3], Draw.INK_ALPHA), segno.ink)
        assertFalse("il tratteggio spento", segno.dashed)
    }

    /**
     * **Il riempimento ha colore e opacità suoi, e la freccia lo ignora.**
     *
     * ⚠️ È il suo esempio alla lettera, arrivato a G1 in corso: *un bordo rosso primario e un
     * riempimento bianco 50%*.
     * ⚠️⚠️ **CONTROPROVATA** dando alla freccia il riempimento in `Gaze.penMark`: il secondo elemento
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
        assertEquals("il bordo resta rosso", Draw.withAlpha(Draw.INKS[0], Draw.INK_ALPHA), segni[0].ink)
        assertEquals("il riempimento bianco al 50%", 0x80FFFFFF.toInt(), segni[0].fill)
        assertEquals(Pen.ARROW, segni[1].pen)
        assertEquals("la freccia ignora il riempimento", null, segni[1].fill)
    }

    /**
     * **I valori di fabbrica sono i suoi** (sua nota A sul giro della `4.49`): rettangolo
     * arrotondato con la linea tratteggiata, traccia rossa con la luminosità al 25% della corsa,
     * l'opacità al 50% e lo spessore al 40%; il riempimento resta l'ambra al 20% della `4.44`
     * (risposta `D2`), e la freccia nasce senza.
     * ⚠️ 'Cursore al X%' si legge come un posto sulla corsa: è la lettura dichiarata nella voce di
     * collaudo.
     * ⚠️⚠️ **CONTROPROVATA** rimettendo i valori della `4.49` (mano libera senza tratteggio,
     * luminosità e opacità di base, spessore al 60%).
     */
    @Test
    fun `i valori di fabbrica sono i suoi`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        banco.onNodeWithContentDescription(testo(R.string.draw_rect)).assertIsSelected()
        banco.onNodeWithContentDescription(testo(R.string.draw_dashed)).assertIsOn()
        assertEquals("l'opacità doveva partire dal 50% della corsa", 0.5f, (valore() - 0.1f) / 0.9f, 1e-3f)
        banco.onNodeWithContentDescription(testo(R.string.draw_width)).performClick()
        banco.waitForIdle()
        assertEquals("lo spessore doveva partire dal 40% della corsa", 0.4f,
            (valore() - Draw.WIDTH_MIN) / (Draw.WIDTH_MAX - Draw.WIDTH_MIN), 1e-3f)
        trascina()
        banco.onNodeWithContentDescription(testo(R.string.draw_arrow)).performClick()
        banco.waitForIdle()
        trascina(dy = -PASSO)
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        val (rettangolo, freccia) = salvato!!.drawing.marks
        assertEquals(Pen.RECT, rettangolo.pen)
        assertTrue("il tratteggio di fabbrica", rettangolo.dashed)
        assertEquals("il tratto di fabbrica", Draw.withAlpha(Draw.lit(0xFFFF4C3F.toInt(), -0.5f), 0.55f), rettangolo.ink)
        assertEquals("lo spessore di fabbrica", Draw.WIDTH_MIN + 0.4f * (Draw.WIDTH_MAX - Draw.WIDTH_MIN), rettangolo.width, 1e-5f)
        assertEquals("il riempimento di fabbrica", 0x33FFBF00, rettangolo.fill)
        assertEquals("la freccia nasce senza riempimento", null, freccia.fill)
        assertEquals("la freccia ha lo stesso tratto", rettangolo.ink, freccia.ink)
    }

    /** Il valore del cursore in fondo alla scheda, o con [luce] quello della luminosità. */
    private fun valore(luce: Boolean = false): Float =
        (if (luce) banco.onNode(cursoreLuce()) else banco.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))[0])
            .fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo].current

    private fun cursoreLuce() = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress) and
        SemanticsMatcher.expectValue(
            androidx.compose.ui.semantics.SemanticsProperties.ContentDescription, listOf(testo(R.string.draw_light))
        )

    /** Porta l'opacità della traccia al pieno, dove una prova guarda i colori puri. */
    private fun opacitaPiena() {
        banco.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))[0]
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        banco.waitForIdle()
    }

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

    /** **'Annulla' toglie l'ultimo elemento, e solo quello.** */
    @Test
    fun `Annulla toglie l'ultimo elemento`() {
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
        assertEquals("dopo 'Annulla' doveva restare il primo elemento", 1, salvato!!.drawing.marks.size)
        assertEquals(Pen.RECT, salvato!!.drawing.marks.single().pen)
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
        banco.onNodeWithContentDescription(testo(R.string.draw_rect)).assertIsSelected()
        banco.onNodeWithContentDescription(testo(R.string.draw_filled)).assertIsEnabled()
        banco.onNodeWithContentDescription(testo(R.string.draw_free)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(testo(R.string.draw_filled)).assertIsNotEnabled()
        banco.onNodeWithContentDescription(testo(R.string.draw_ellipse)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(testo(R.string.draw_filled)).assertIsEnabled()
        banco.onNodeWithContentDescription(testo(R.string.draw_arrow)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(testo(R.string.draw_filled)).assertIsNotEnabled()
    }

    /**
     * **Mentre il dito tiene 'Spessore', l'anteprima è una lineetta curva alla sua misura vera**
     * (R1 del giro della `4.40`; curva dalla `4.50`, sua nota D sul giro della `4.49`: *una
     * lineetta curva, non un punto*), del colore della linea, in basso a destra sull'immagine, e
     * sparisce quando il dito si alza.
     * ⚠️⚠️ **CONTROPROVATA** due volte: spegnendo l'anteprima nel palco (i pixel del colore della
     * linea restano zero col dito sul cursore), e rimettendo il tondo pieno della `4.49` (l'elemento
     * è largo quanto alto).
     * ⚠️ **Su uno schermo da telefono e non su quello di serie del banco**: là il palco è alto 40
     * pixel e l'immagine 30, quindi l'anteprima vera misura meno di un pixel.
     * ⚠️ Rosso e opacità piena, perché la prova cerca il colore puro.
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `tenendo Spessore si vede una lineetta curva del colore della linea`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        banco.onNodeWithContentDescription(testo(R.string.ink_red)).performClick()
        opacitaPiena()
        banco.onNodeWithContentDescription(testo(R.string.draw_width)).performClick()
        banco.waitForIdle()
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
        assertTrue("col dito sul cursore l'anteprima rossa doveva vedersi", inchiostro(tenuto, rosso) > 20)
        assertEquals("a dito alzato l'anteprima doveva sparire", 0, inchiostro(lasciato, rosso))
        val (cx, cy) = centro(tenuto, rosso)
        assertTrue("l'anteprima doveva stare a destra", cx > tenuto.width / 2)
        assertTrue("l'anteprima doveva stare in basso", cy > tenuto.height / 2)
        val (largo, alto) = ingombro(tenuto, rosso)
        assertTrue("l'anteprima doveva essere una lineetta, più larga che alta: $largo per $alto", largo > alto * 2)
    }

    /**
     * **Mentre si disegna una forma piccola compare la lente, e una forma grande non la vuole**
     * (R2 del giro della `4.40`, soglia di 1,5 cm sullo schermo).
     * ⚠️ Si confronta il palco col dito giù e a dito alzato: l'elemento resta in tutti e due, quindi
     * quello che cambia è la sola lente.
     * ⚠️⚠️ **CONTROPROVATA** togliendo la lente dal palco: con l'elemento piccolo la differenza scende
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
     * **I tasti Traccia, Riempimento, Tratteggio e Spessore sono disegni, non parole** (voce
     * `4.42-01`; Spessore dalla `4.47`): sullo schermo non c'è più la parola, che resta come
     * descrizione per il lettore di schermo.
     * ⚠️⚠️ **CONTROPROVATA** rimettendo il testo nell'etichetta di 'Tratteggio'.
     */
    @Test
    fun `i quattro tasti sono disegni con la loro descrizione`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        for (id in listOf(R.string.draw_outline, R.string.draw_filled, R.string.draw_dashed, R.string.draw_width)) {
            assertEquals("il tasto '${testo(id)}' doveva esserci, descritto", 1,
                banco.onAllNodesWithContentDescription(testo(id)).fetchSemanticsNodes().size)
            assertEquals("il tasto '${testo(id)}' non doveva mostrare la parola", 0,
                banco.onAllNodesWithText(testo(id)).fetchSemanticsNodes().size)
        }
    }

    /**
     * **Il tasto Tratteggio acceso mostra il tratteggio nel colore della traccia, spento lo mostra
     * grigio sbiadito** (sua risposta `B2` alla nota B sul giro della `4.49`: *non si capisce bene
     * quando la linea tratteggiata è attiva*). Rosso puro e opacità piena, per cercare il colore
     * esatto.
     * ⚠️⚠️ **CONTROPROVATA** rimettendo il grigio pieno della `4.50` nei due stati: acceso, il
     * rosso non c'è.
     */
    @Test
    fun `il Tratteggio acceso ha il colore della traccia, spento e sbiadito`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        banco.onNodeWithContentDescription(testo(R.string.ink_red)).performClick()
        opacitaPiena()
        val rosso = androidx.compose.ui.graphics.Color(Draw.INK)
        val grigio = androidx.compose.ui.graphics.Color(0xFF616161)
        val tasto = banco.onNodeWithContentDescription(testo(R.string.draw_dashed))
        tasto.assertIsOn()
        val acceso = tasto.captureToImage().toPixelMap()
        assertTrue("acceso, il tratteggio doveva essere rosso", inchiostro(acceso, rosso) > 20)
        tasto.performClick()
        banco.waitForIdle()
        val spento = tasto.captureToImage().toPixelMap()
        assertEquals("spento, il rosso doveva sparire", 0, inchiostro(spento, rosso))
        assertEquals("spento, il grigio doveva essere sbiadito", 0, inchiostro(spento, grigio))
        val y = spento.height / 2
        assertTrue("spento, il tratteggio doveva vedersi lo stesso",
            (0 until spento.width).map { spento[it, y] }.distinct().size > 1)
    }

    /**
     * **Il tasto Traccia mostra il colore della linea, e lo cambia con lei; la linea arriva ai bordi
     * del tasto** (sua nota 2 su `4.43-01`: *fino ai limiti dello spazio del tasto*).
     * ⚠️⚠️ **CONTROPROVATA** due volte: disegnando la linea in un colore fisso, e rimettendo la linea
     * della `4.43`, che si fermava prima dei bordi.
     */
    @Test
    fun `il tasto Traccia ha il colore della linea, da bordo a bordo`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        // ⚠️ The pure red at full opacity: since 4.50 the factory stroke is darker and at 55%.
        banco.onNodeWithContentDescription(testo(R.string.ink_red)).performClick()
        opacitaPiena()
        val rosso = androidx.compose.ui.graphics.Color(Draw.INK)
        val blu = androidx.compose.ui.graphics.Color(Draw.INKS[3])
        val prima = banco.onNodeWithContentDescription(testo(R.string.draw_outline)).captureToImage().toPixelMap()
        assertTrue("il tasto doveva essere rosso", inchiostro(prima, rosso) > 20)
        // ⚠️ Two pixels in from each side, on the middle row: the edge itself is antialiased.
        val y = prima.height / 2
        assertTrue("la linea doveva arrivare al bordo sinistro", vicino(prima[2, y], rosso))
        assertTrue("la linea doveva arrivare al bordo destro", vicino(prima[prima.width - 3, y], rosso))
        banco.onNodeWithContentDescription(testo(R.string.ink_blue)).performClick()
        banco.waitForIdle()
        val dopo = banco.onNodeWithContentDescription(testo(R.string.draw_outline)).captureToImage().toPixelMap()
        assertTrue("il tasto doveva diventare blu", inchiostro(dopo, blu) > 20)
        assertEquals("del rosso non doveva restare niente", 0, inchiostro(dopo, rosso))
    }

    /**
     * **Il tasto Riempimento mostra il colore del riempimento con l'opacità alzata, minimo 40%**
     * (voce `4.42-01`): l'ambra al 20% di fabbrica si vede al 52% sui quadretti bianchi.
     * ⚠️⚠️ **CONTROPROVATA** dipingendo il tasto con l'opacità vera: sul bianco l'ambra al 20% è un
     * altro colore, e i pixel attesi scendono a zero.
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
        val ambra = Draw.FILL or 0xFF000000.toInt()
        val a = keyAlpha((Draw.FILL ushr 24) / 255f)
        fun su(fondo: Float, c: Int) = fondo * (1 - a) + (c and 0xFF) / 255f * a
        val atteso = androidx.compose.ui.graphics.Color(
            su(1f, ambra shr 16), su(1f, ambra shr 8), su(1f, ambra)
        )
        val tasto = banco.onNodeWithContentDescription(testo(R.string.draw_filled)).captureToImage().toPixelMap()
        assertTrue("sui quadretti bianchi l'ambra doveva vedersi accentuata", inchiostro(tasto, atteso) > 10)
    }

    /**
     * **L'ordine dei tasti è Tratteggio, Traccia, Spessore, Riempimento** (sua risposta sul giro
     * della `4.43` per i primi tre; Spessore dalla `4.47` al posto di Luminosità, e dalla `4.49` a
     * sinistra di Riempimento, sua nota su `4.47-01`).
     * ⚠️⚠️ **CONTROPROVATA** rimettendo l'ordine della `4.47` (Riempimento prima di Spessore).
     */
    @Test
    fun `i tasti sono Tratteggio, Traccia, Spessore e Riempimento da sinistra`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        val (tratteggio, traccia, spessore, riempimento) = listOf(
            R.string.draw_dashed, R.string.draw_outline, R.string.draw_width, R.string.draw_filled
        ).map { banco.onNodeWithContentDescription(testo(it)).fetchSemanticsNode().boundsInRoot.left }
        assertTrue("Tratteggio doveva stare a sinistra di Traccia", tratteggio < traccia)
        assertTrue("Traccia doveva stare a sinistra di Spessore", traccia < spessore)
        assertTrue("Spessore doveva stare a sinistra di Riempimento", spessore < riempimento)
        assertEquals("Luminosità non è più un tasto", 0,
            banco.onAllNodes(SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.ContentDescription, listOf(testo(R.string.draw_light))
            )).fetchSemanticsNodes().size)
    }

    /**
     * **Il cursore della luminosità si apre sopra la fila dei tondi e la copre**, largo quanto lei
     * (sua nota su `4.49-02`: *come un livello sovrapposto a coprirli*), con l'anteprima a lato.
     * ⚠️ Le posizioni si leggono sullo schermo, perché il cursore vive nella finestra del menu. Il
     * pannello non è un nodo: se ne misura il cursore, che ne occupa la parte dopo l'anteprima.
     * ⚠️⚠️ **CONTROPROVATA** due volte: col cursore aperto sotto la fila, e più corto di 24dp per
     * lato come nella `4.49`. La prima volta cade la misura del centro, la seconda quella del lato
     * destro.
     */
    @Test
    fun `il cursore della luminosita copre la fila dei tondi`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        val rosso = banco.onNodeWithContentDescription(testo(R.string.ink_red))
        rosso.performTouchInput { longClick() }
        banco.waitForIdle()
        assertEquals("tenuto il tondo, il cursore doveva restare", 1, luci())
        val tondo = rosso.fetchSemanticsNode()
        val nero = banco.onNodeWithContentDescription(testo(R.string.ink_black)).fetchSemanticsNode()
        val cursore = banco.onNode(cursoreLuce()).fetchSemanticsNode()
        val mezzoTondi = tondo.positionOnScreen.y + tondo.size.height / 2f
        val mezzoCursore = cursore.positionOnScreen.y + cursore.size.height / 2f
        assertEquals("il cursore doveva avere il centro sulla fila dei tondi", mezzoTondi, mezzoCursore, 2f)
        // The panel runs as wide as the row: its padding (12dp) is all that stays between the end
        // of the slider and the black swatch's right edge, which closes the row.
        val destra = nero.positionOnScreen.x + nero.size.width
        val fine = cursore.positionOnScreen.x + cursore.size.width
        assertTrue("il cursore doveva arrivare in fondo alla fila: finisce a $fine, la fila a $destra", fine > destra - 16)
        assertTrue("il cursore doveva cominciare dopo l'anteprima, a lato",
            cursore.positionOnScreen.x > tondo.positionOnScreen.x - tondo.size.width)
    }

    /**
     * **Il pollice va sotto il dito: la luminosità è il posto del dito sul cursore** (sua nota C
     * sul giro della `4.49`: *Lo slider pare seguire il dito solo verso un lato*). Dal tondo rosso,
     * che è il secondo da sinistra, il dito va a un quarto e a tre quarti della corsa, e poi oltre
     * il bordo sinistro dello schermo, dove la luminosità tocca il fondo.
     * ⚠️ Il gesto parte dal tondo e il cursore vive in un'altra finestra: i posti si convertono
     * dallo schermo alle coordinate del tondo, che sono quelle del gesto.
     * ⚠️⚠️ **CONTROPROVATA** rimettendo il conto della `4.49` (lo spostamento del dito aggiunto alla
     * luminosità di partenza): a un quarto della corsa il valore non è -0,5.
     */
    @Test
    fun `il pollice della luminosita va sotto il dito`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        val rosso = banco.onNodeWithContentDescription(testo(R.string.ink_red))
        val da = rosso.fetchSemanticsNode().positionOnScreen
        rosso.performTouchInput { down(center) }
        rosso.performTouchInput { advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100); moveBy(Offset(1f, 0f)) }
        banco.waitForIdle()
        assertEquals("tenuto il tondo, il cursore della luminosità doveva comparire", 1, luci())
        val pista = banco.onNode(cursoreLuce()).fetchSemanticsNode()
        fun a(frazione: Float) {
            val x = pista.positionOnScreen.x + pista.size.width * frazione - da.x
            rosso.performTouchInput { moveTo(Offset(x, center.y)) }
            banco.waitForIdle()
        }
        a(0.25f)
        assertEquals("a un quarto della corsa la luminosità è -0,5", -0.5f, valore(luce = true), 0.05f)
        a(0.75f)
        assertEquals("a tre quarti della corsa la luminosità è 0,5", 0.5f, valore(luce = true), 0.05f)
        rosso.performTouchInput { moveTo(Offset(-da.x - 4f, center.y)) }
        banco.waitForIdle()
        assertEquals("oltre il bordo sinistro la luminosità tocca il fondo", -1f, valore(luce = true), 1e-4f)
        rosso.performTouchInput { up() }
        banco.waitForIdle()
        assertEquals("allo stacco il cursore doveva chiudersi", 0, luci())
    }

    /**
     * **I tondi svaniscono mentre una superficie si apre sopra l'editor** (sua nota F sul giro della
     * `4.49`: sotto la sfocatura i loro colori pieni sbordavano da 'Vuoi scartare le modifiche?').
     * ⚠️⚠️ **Il velo si chiede da qui, e non aprendo un menu**: il cursore della luminosità copre la
     * fila, quindi una cattura non distinguerebbe un tondo svanito da uno coperto. E il velo dipinto
     * scurisce tutta l'app, quindi il tondo si confronta col fondo che gli sta intorno, scurito allo
     * stesso modo: svanito, al centro del tondo c'è quel fondo.
     * ⚠️⚠️ **CONTROPROVATA** togliendo l'opacità che segue il velo dalla fila dei tondi: al centro
     * resta il blu.
     */
    @Test
    fun `i tondi svaniscono sotto una superficie aperta`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        val tondo = banco.onNodeWithContentDescription(testo(R.string.ink_blue))
        fun pieno(): Boolean {
            val mappa = tondo.captureToImage().toPixelMap()
            return !vicino(mappa[mappa.width / 2, mappa.height / 2], mappa[1, 1])
        }
        assertTrue("prima il tondo blu doveva vedersi sul fondo", pieno())
        val chi = Any()
        try {
            VeilStage.at(chi, VEIL_DOSE, VEIL_DOSE, androidx.compose.ui.graphics.Color.Black)
            banco.waitForIdle()
            assertFalse("sotto il velo il tondo blu doveva svanire nel fondo", pieno())
        } finally {
            VeilStage.off(chi)
        }
        banco.waitForIdle()
        assertTrue("tolto il velo il tondo doveva tornare", pieno())
    }

    /**
     * **All'apertura il tondo del colore di fabbrica risulta scelto, per la traccia e per il
     * riempimento** (sue risposte `D1` e `D2`): un colore di fabbrica fuori tavolozza aprirebbe il
     * modulo senza nessun tondo scelto. La traccia prende il primo tondo per costruzione; il
     * riempimento ha una costante sua, ed è lei che questa prova sorveglia.
     * ⚠️⚠️ **CONTROPROVATA** rimettendo il riempimento di fabbrica della `4.43` (`#26FFAE8E`), che
     * nella tavolozza nuova non c'è.
     */
    @Test
    fun `all'apertura i tondi di fabbrica sono scelti`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        banco.onNodeWithContentDescription(testo(R.string.ink_red)).assertIsSelected()
        banco.onNodeWithContentDescription(testo(R.string.draw_rect)).performClick()
        banco.onNodeWithContentDescription(testo(R.string.draw_filled)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(testo(R.string.ink_amber)).assertIsSelected()
    }

    /**
     * **Luminosità schiarisce e scurisce senza arrivare al bianco o al nero** (4.45; sua nota su
     * `4.43-01`), e tiene tinta e opacità.
     * ⚠️⚠️ **CONTROPROVATA** togliendo i due limiti (schiarire fino a 1 e scurire fino a 0).
     */
    @Test
    fun `Luminosita resta fra i suoi limiti e tiene tinta e opacita`() {
        val hsl = FloatArray(3)
        val blu = 0xFF3EB7FF.toInt()
        assertEquals("a zero il colore è il tondo", blu, Draw.lit(blu, 0f))
        androidx.core.graphics.ColorUtils.colorToHSL(Draw.lit(blu, 1f), hsl)
        assertEquals("il più chiaro si ferma all'85%", Draw.LIGHT_MAX, hsl[2], 0.01f)
        val tinta = hsl[0]
        androidx.core.graphics.ColorUtils.colorToHSL(Draw.lit(blu, -1f), hsl)
        assertEquals("il più scuro si ferma al 15%", Draw.LIGHT_MIN, hsl[2], 0.01f)
        assertEquals("la tinta resta quella del tondo", tinta, hsl[0], 1.5f)
        assertEquals("il bianco non diventa più chiaro", 0xFFFFFFFF.toInt(), Draw.lit(0xFFFFFFFF.toInt(), 1f))
        androidx.core.graphics.ColorUtils.colorToHSL(Draw.lit(0xFFFFFFFF.toInt(), -1f), hsl)
        assertEquals("il bianco scurito si ferma al 15%", Draw.LIGHT_MIN, hsl[2], 0.01f)
        assertEquals("l'opacità resta", 0x33, Draw.lit(Draw.FILL, 0.5f) ushr 24)
    }

    /**
     * **Tre scelte esclusive: Traccia regola l'opacità della linea, Riempimento quella del
     * riempimento, Spessore lo spessore** (sua risposta `S1`, `4.47`). Con Spessore scelto i tondi
     * restano sulla linea.
     * ⚠️⚠️ **CONTROPROVATA** dando all'elemento nuovo la linea piena invece della sua opacità
     * (`Gaze.penMark`).
     */
    @Test
    fun `Traccia regola l'opacita della linea, Spessore lo spessore`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        banco.onNodeWithContentDescription(testo(R.string.draw_line)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(testo(R.string.draw_outline)).assertIsSelected()
        assertEquals("con Traccia il cursore è l'opacità, quella di fabbrica", Draw.INK_ALPHA, valore(), 1e-4f)
        banco.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))[0]
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        banco.onNodeWithContentDescription(testo(R.string.draw_width)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(testo(R.string.draw_width)).assertIsSelected()
        banco.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))[0]
            .performSemanticsAction(SemanticsActions.SetProgress) { it(Draw.WIDTH_MAX) }
        banco.onNodeWithContentDescription(testo(R.string.ink_blue)).performClick()
        banco.waitForIdle()
        trascina()
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        val segno = salvato!!.drawing.marks.single()
        assertEquals("la linea al 50%, blu: con Spessore i tondi restano sulla linea",
            Draw.withAlpha(Draw.INKS[3], 0.5f), segno.ink)
        assertEquals("lo spessore scelto", Draw.WIDTH_MAX, segno.width, 1e-6f)
    }

    /**
     * **L'opacità della linea vale per l'elemento intero**: dove la freccia incrocia la sua asta il
     * colore resta quello del resto, non più scuro.
     * ⚠️⚠️ **CONTROPROVATA** dipingendo la linea con un colore trasparente invece che in un livello:
     * dove punta e asta si sovrappongono il nero al 50% sul bianco scende verso il 25%.
     */
    @Test
    fun `l'opacita della linea non scurisce dove l'elemento si sovrappone`() {
        val bianca = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val segno = Mark(
            Pen.ARROW, listOf(Offset(0.1f, 0.5f), Offset(0.9f, 0.5f)), Draw.withAlpha(Color.BLACK, 0.5f),
            0.04f, false, null
        )
        val fatta = Draw.onto(bianca, Drawing(listOf(segno)), Spin.STILL)
        var piu = 255
        for (y in 0 until 80) for (x in 0 until 80) piu = minOf(piu, Color.red(fatta.getPixel(x, y)))
        assertEquals("il punto più scuro doveva essere il nero al 50% sul bianco", 128f, piu.toFloat(), 3f)
    }

    /**
     * **La linea del tasto Spessore è spessa quanto il tratto sullo schermo**, cioè quanto la
     * lineetta dell'anteprima (sua nota su `4.49-01`), e alle misure più piccole resta di 2dp.
     * Lo spessore si misura sulla colonna di mezzo del tasto, e su quella dell'anteprima a un
     * decimo della sua larghezza, dove la curva è quasi orizzontale.
     * ⚠️⚠️ **CONTROPROVATA** rimettendo la linea della `4.49`, che andava da 2 a 16dp lungo il
     * cursore: al massimo è più spessa del tratto vero.
     * ⚠️ Su uno schermo da telefono, per la ragione scritta sulla prova dell'anteprima.
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `il tasto Spessore e spesso quanto il tratto`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        banco.onNodeWithContentDescription(testo(R.string.ink_red)).performClick()
        opacitaPiena()
        val rosso = androidx.compose.ui.graphics.Color(Draw.INK)
        val tasto = banco.onNodeWithContentDescription(testo(R.string.draw_width))
        tasto.performClick()
        banco.waitForIdle()
        val cursore = banco.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))[0]
        cursore.performSemanticsAction(SemanticsActions.SetProgress) { it(Draw.WIDTH_MIN) }
        banco.waitForIdle()
        assertEquals("alle misure più piccole la linea resta di 2dp", 2f, colonna(tasto.captureToImage().toPixelMap()).toFloat(), 1f)
        // The finger holds the slider near its end, so the stage shows the preview, thick.
        cursore.performTouchInput { down(Offset(width * 0.9f, height / 2f)) }
        cursore.performTouchInput { moveTo(Offset(width - 1f, height / 2f)) }
        banco.waitForIdle()
        val chiave = colonna(tasto.captureToImage().toPixelMap())
        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare)).captureToImage().toPixelMap()
        cursore.performTouchInput { up() }
        banco.waitForIdle()
        val (largo, _) = ingombro(palco, rosso)
        var sinistra = palco.width
        for (y in 0 until palco.height) for (x in 0 until palco.width) if (vicino(palco[x, y], rosso)) sinistra = minOf(sinistra, x)
        val tratto = colonna(palco, sinistra + largo / 10)
        assertTrue("il tratto dell'anteprima doveva essere più spesso del minimo: $tratto", tratto > 4)
        assertEquals("la linea del tasto doveva essere spessa quanto il tratto", tratto.toFloat(), chiave.toFloat(), 2f)
    }

    /**
     * **Tenendo premuto un tondo e scorrendo si sceglie la luminosità, e allo stacco il cursore si
     * chiude** (sua nota su `4.45-02`, risposte `L1a` e `L2a`). Il tondo resta scelto, e un tocco
     * successivo gli rende il suo colore. Dalla `4.63` il valore si posa mentre il dito scorre (sua
     * nota su `4.61-04`), e fino alla `4.62` si posava allo stacco: qui conta che ci sia, allo
     * stacco.
     * ⚠️⚠️ **CONTROPROVATA** due volte: senza applicare il valore allo stacco (la linea nasce col
     * colore del tondo), e senza chiudere il cursore (resta in scena).
     */
    @Test
    fun `tenendo un tondo e scorrendo si schiarisce la linea in un gesto`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        banco.onNodeWithContentDescription(testo(R.string.draw_line)).performClick()
        banco.waitForIdle()
        val rosso = banco.onNodeWithContentDescription(testo(R.string.ink_red))
        rosso.performTouchInput { down(center) }
        rosso.performTouchInput { advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100); moveBy(Offset(1f, 0f)) }
        banco.waitForIdle()
        assertEquals("tenuto il tondo, il cursore della luminosità doveva comparire", 1, luci())
        rosso.performTouchInput { moveBy(Offset(PASSO, 0f)) }
        rosso.performTouchInput { moveBy(Offset(PASSO * 4, 0f)) }
        banco.waitForIdle()
        rosso.performTouchInput { up() }
        banco.waitForIdle()
        assertEquals("allo stacco il cursore doveva chiudersi", 0, luci())
        banco.onNodeWithContentDescription(testo(R.string.ink_red)).assertIsSelected()
        trascina()
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        val ink = salvato!!.drawing.marks.single().ink
        val hsl = FloatArray(3)
        val base = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(ink, hsl)
        androidx.core.graphics.ColorUtils.colorToHSL(Draw.INK, base)
        assertTrue("la linea doveva nascere più chiara del tondo", hsl[2] > base[2] + 0.02f)

        banco.onNodeWithContentDescription(testo(R.string.ink_red)).performClick()
        banco.onNodeWithContentDescription(testo(R.string.draw_line)).performClick()
        banco.waitForIdle()
        trascina(dy = -PASSO)
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        assertEquals("un tocco sul tondo doveva rendergli il suo colore", Draw.withAlpha(Draw.INK, Draw.INK_ALPHA),
            salvato!!.drawing.marks.last().ink)
    }

    /**
     * **Tenendo premuto un tondo e alzando il dito fermo, il cursore resta e si regola, e un tocco
     * fuori non disegna** (risposte `L1a` e `L3a`).
     * ⚠️ **Che il tocco fuori chiuda il cursore lo fa la finestra del menu** (`MenuShell`, come per
     * ogni menu dell'app), e il banco non le consegna quel tocco: qui si misura che il tocco non
     * arriva al palco, cioè che `MenuGuard` lo ferma.
     * ⚠️⚠️ **CONTROPROVATA** chiudendo il cursore allo stacco anche senza movimento: il cursore non
     * c'è più quando la prova lo cerca.
     */
    @Test
    fun `tenendo un tondo fermo il cursore resta e il tocco fuori non disegna`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        banco.onNodeWithContentDescription(testo(R.string.draw_line)).performClick()
        banco.waitForIdle()
        opacitaPiena()
        banco.onNodeWithContentDescription(testo(R.string.ink_blue)).performTouchInput { longClick() }
        banco.waitForIdle()
        assertEquals("a dito alzato il cursore doveva restare", 1, luci())
        banco.onNodeWithContentDescription(testo(R.string.ink_blue)).assertIsSelected()
        banco.onNode(cursoreLuce()).performSemanticsAction(SemanticsActions.SetProgress) { it(-1f) }
        banco.waitForIdle()
        val scuro = androidx.compose.ui.graphics.Color(Draw.lit(Draw.INKS[3], -1f))
        val tasto = banco.onNodeWithContentDescription(testo(R.string.draw_outline)).captureToImage().toPixelMap()
        assertTrue("il tasto Traccia doveva mostrare il blu scurito", inchiostro(tasto, scuro) > 20)
        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        palco.performTouchInput { down(center) }
        palco.performTouchInput { moveTo(center + Offset(PASSO, PASSO)) }
        palco.performTouchInput { up() }
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.editor_save)).assertIsNotEnabled()
    }

    /** Quanti cursori della luminosità sono in scena. */
    private fun luci(): Int = banco.onAllNodes(
        SemanticsMatcher.expectValue(
            androidx.compose.ui.semantics.SemanticsProperties.ContentDescription, listOf(testo(R.string.draw_light))
        )
    ).fetchSemanticsNodes().size

    /**
     * **Una linea vicina all'orizzontale o alla verticale si posa esattamente su di lei** (sua nota
     * A sul giro della `4.45`), entro [Draw.SNAP_DEG].
     * ⚠️⚠️ **CONTROPROVATA** togliendo la chiamata a `Draw.snap` dal gesto: i due punti salvati
     * hanno altezze diverse.
     */
    @Test
    fun `una linea quasi orizzontale si posa sull'orizzontale`() {
        val a = Offset(10f, 10f)
        assertEquals(Offset(110f, 10f) to SnapAxis.HORIZONTAL, Draw.snap(a, Offset(110f, 15f)))
        assertEquals(Offset(10f, 110f) to SnapAxis.VERTICAL, Draw.snap(a, Offset(14f, 110f)))
        assertEquals("a 30 gradi la linea resta dov'è", Offset(110f, 68f) to null, Draw.snap(a, Offset(110f, 68f)))
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        for (pen in listOf(R.string.draw_line, R.string.draw_arrow)) {
            banco.onNodeWithContentDescription(testo(pen)).performClick()
            banco.waitForIdle()
            trascina(dx = PASSO * 2, dy = 4f)
        }
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        for (segno in salvato!!.drawing.marks) {
            assertEquals("${segno.pen} doveva posarsi sull'orizzontale", segno.points[0].y, segno.points[1].y, 1e-5f)
            assertTrue("${segno.pen} doveva seguire il dito lungo l'asse", segno.points[1].x > segno.points[0].x)
        }
    }

    /**
     * **Mentre il dito tiene una linea agganciata compare la guida, e sparisce allo stacco** (sua
     * nota A). Su uno schermo da telefono e con un elemento lungo, così la lente non c'è e quello che
     * cambia fra dito giù e dito alzato è la sola guida.
     * ⚠️⚠️ **CONTROPROVATA** togliendo la guida dal palco: la differenza scende a zero.
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `la guida compare mentre la linea e agganciata`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        banco.onNodeWithContentDescription(testo(R.string.draw_line)).performClick()
        banco.waitForIdle()
        assertTrue("con la linea agganciata la guida doveva vedersi", diffDurante(PASSO * 5, 6f) > 50)
        assertEquals("con la linea storta la guida non doveva esserci", 0, diffDurante(PASSO * 5, PASSO * 3))
    }

    /**
     * **Il grigio è fra il bianco e il nero, e i dieci posti entrano anche su uno schermo
     * stretto** (sua nota su `4.44-01`; il banco di serie è largo 320dp, meno di dieci tondi da
     * 32dp).
     * ⚠️⚠️ **CONTROPROVATA** rimettendo i tondi da 32dp fissi: il nero esce dal bordo.
     */
    @Test
    fun `il grigio e fra bianco e nero e la fila entra nello schermo`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        val (bianco, grigio, nero) = listOf(R.string.ink_white, R.string.ink_grey, R.string.ink_black)
            .map { banco.onNodeWithContentDescription(testo(it)).fetchSemanticsNode().boundsInRoot }
        assertTrue("il grigio doveva stare dopo il bianco", grigio.left > bianco.left)
        assertTrue("il nero doveva stare dopo il grigio", nero.left > grigio.left)
        val largo = banco.onRoot().fetchSemanticsNode().boundsInRoot.right
        assertTrue("il nero doveva entrare nello schermo: ${nero.right} su $largo", nero.right <= largo)
    }

    /**
     * **La prova del tocco della G2** (`4.60`): una forma riempita si prende anche dentro, una
     * vuota solo vicino alla linea; una linea entro la portata; fra due elementi sovrapposti vince
     * quello sopra.
     * ⚠️⚠️ **CONTROPROVATA** prendendo il rettangolo vuoto anche dentro: il tocco al centro lo
     * sceglie.
     */
    @Test
    fun `il tocco prende l'elemento giusto`() {
        val pieno = Mark(Pen.RECT, listOf(Offset(0.1f, 0.1f), Offset(0.4f, 0.4f)), Color.RED, 0.01f, false, Color.WHITE)
        val vuoto = Mark(Pen.RECT, listOf(Offset(0.6f, 0.1f), Offset(0.9f, 0.4f)), Color.RED, 0.01f, false, null)
        val linea = Mark(Pen.LINE, listOf(Offset(0.1f, 0.8f), Offset(0.9f, 0.8f)), Color.RED, 0.01f, false, null)
        val sopra = Mark(Pen.ELLIPSE, listOf(Offset(0.2f, 0.2f), Offset(0.3f, 0.3f)), Color.BLUE, 0.01f, false, Color.BLUE)
        val disegno = Drawing(listOf(pieno, vuoto, linea, sopra))
        fun h(x: Float, y: Float) = Draw.hit(disegno, Offset(x, y), 100, 100, 0.02f)
        assertEquals("dentro il rettangolo pieno", 0, h(0.15f, 0.35f))
        assertEquals("dentro il rettangolo vuoto, lontano dalla linea, niente", null, h(0.75f, 0.25f))
        assertEquals("sulla linea del rettangolo vuoto", 1, h(0.6f, 0.25f))
        assertEquals("vicino alla linea", 2, h(0.5f, 0.815f))
        assertEquals("lontano da tutto", null, h(0.5f, 0.6f))
        assertEquals("dove due elementi si sovrappongono vince quello sopra", 3, h(0.25f, 0.25f))
    }

    /**
     * **Un tocco su un elemento lo sceglie e mostra i suoi vertici; un tocco nel vuoto toglie la scelta
     * e non disegna** (G2, `4.60`, sua specifica: *un tocco singolo seleziona un oggetto; il
     * rettangolo selezionato mostra 4 vertici color accento*). Con la mano libera e senza scelta, un
     * tocco nel vuoto lascia il punto come prima.
     * ⚠️⚠️ **CONTROPROVATA** due volte: senza la scelta nel gesto del palco (il tocco sull'elemento
     * disegna un punto), e senza i vertici nel palco (il palco non cambia).
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `un tocco sceglie l'elemento e il vuoto toglie la scelta`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        trascinaDa(Offset(-100f, -100f), Offset(-40f, -40f))
        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        val elimina = banco.onNodeWithContentDescription(testo(R.string.pick_delete))
        elimina.assertIsNotEnabled()
        val prima = palco.captureToImage().toPixelMap()
        tocca(Offset(-70f, -70f))
        elimina.assertIsEnabled()
        val scelto = palco.captureToImage().toPixelMap()
        assertTrue("scelto l'elemento, i suoi vertici dovevano comparire", differenza(prima, scelto) > 40)
        tocca(Offset(80f, 80f))
        elimina.assertIsNotEnabled()
        assertEquals("tolta la scelta, il palco doveva tornare com'era", 0, differenza(prima, palco.captureToImage().toPixelMap()))
        banco.onNodeWithContentDescription(testo(R.string.draw_free)).performClick()
        banco.waitForIdle()
        tocca(Offset(80f, 80f))
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        assertEquals("i tocchi che sceglievano non dovevano disegnare, l'ultimo sì", 2, salvato!!.drawing.marks.size)
        assertEquals(Pen.FREE, salvato!!.drawing.marks.last().pen)
    }

    /**
     * **Con un elemento scelto, i parametri cambiano quell'elemento e non gli altri** (G2, `4.60`, sua
     * specifica: *i parametri (colore della linea, spessore, ecc.) cambiano quell'oggetto*).
     * ⚠️⚠️ **CONTROPROVATA** togliendo il passaggio dei parametri all'elemento scelto: il primo resta
     * rosso.
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `i parametri cambiano l'elemento scelto`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        trascinaDa(Offset(-100f, -100f), Offset(-40f, -40f))
        trascinaDa(Offset(40f, 40f), Offset(100f, 100f))
        tocca(Offset(-70f, -70f))
        banco.onNodeWithContentDescription(testo(R.string.ink_blue)).performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        val (primo, secondo) = salvato!!.drawing.marks
        assertEquals("l'elemento scelto doveva diventare blu", Draw.withAlpha(Draw.INKS[3], Draw.INK_ALPHA), primo.ink)
        assertEquals("l'altro doveva restare com'era", Draw.withAlpha(Draw.lit(Draw.INK, Draw.INK_LIGHT), Draw.INK_ALPHA), secondo.ink)
        assertEquals("l'elemento scelto tiene la sua ricetta", Draw.INKS[3], primo.tint?.ink)
    }

    /**
     * **Con un elemento scelto, la luminosità si vede sull'elemento mentre il dito scorre, nei due
     * gesti** (sua nota su `4.61-04`: *man mano che trascino vedo il colore che cambia
     * nell'oggetto*). Nel gesto unico il valore si posa dal vivo dalla `4.63`, e il cursore si chiude
     * allo stacco come prima; col dito alzato e il cursore trascinato a parte, il valore si posava
     * dal vivo già dalla `4.60`.
     * ⚠️⚠️ **CONTROPROVATA** due volte: col codice della `4.62` (nel gesto unico il valore aspetta
     * lo stacco, e la prima metà fallisce), e col cursore che non posa niente (la seconda metà).
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `la luminosita si vede sull'elemento scelto mentre il dito scorre`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        trascinaDa(Offset(-100f, -100f), Offset(-40f, -40f))
        tocca(Offset(-70f, -70f))
        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        val prima = palco.captureToImage().toPixelMap()
        val rosso = banco.onNodeWithContentDescription(testo(R.string.ink_red))
        rosso.performTouchInput { down(center) }
        rosso.performTouchInput { advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100); moveBy(Offset(1f, 0f)) }
        banco.waitForIdle()
        assertEquals("tenuto il tondo, il cursore della luminosità doveva comparire", 1, luci())
        rosso.performTouchInput { moveBy(Offset(PASSO, 0f)) }
        rosso.performTouchInput { moveBy(Offset(PASSO * 4, 0f)) }
        banco.waitForIdle()
        assertTrue("col dito ancora giù l'elemento scelto doveva già cambiare colore",
            differenza(prima, palco.captureToImage().toPixelMap()) > 40)
        rosso.performTouchInput { up() }
        banco.waitForIdle()
        assertEquals("allo stacco il cursore doveva chiudersi", 0, luci())

        banco.onNodeWithContentDescription(testo(R.string.ink_red)).performTouchInput { longClick() }
        banco.waitForIdle()
        assertEquals("a dito alzato il cursore doveva restare", 1, luci())
        val fermo = palco.captureToImage().toPixelMap()
        banco.onNode(cursoreLuce()).performSemanticsAction(SemanticsActions.SetProgress) { it(-1f) }
        banco.waitForIdle()
        assertTrue("trascinando il cursore l'elemento scelto doveva cambiare colore",
            differenza(fermo, palco.captureToImage().toPixelMap()) > 40)
    }

    /**
     * **L'elemento scelto si sposta trascinandolo, e si elimina col tasto; Annulla lo riporta** (G2,
     * `4.60`).
     * ⚠️⚠️ **CONTROPROVATA** due volte: senza lo spostamento nel gesto (il trascinamento disegna un
     * terzo elemento), e con 'Elimina' che non toglie niente.
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `l'elemento scelto si sposta e si elimina`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        trascinaDa(Offset(-100f, -100f), Offset(-40f, -40f))
        trascinaDa(Offset(40f, 40f), Offset(100f, 100f))
        val prima = mutableListOf<Mark>()
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        prima += salvato!!.drawing.marks
        tocca(Offset(-70f, -70f))
        trascinaDa(Offset(-70f, -70f), Offset(-70f + PASSO, -70f))
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        val spostati = salvato!!.drawing.marks
        assertEquals("lo spostamento non doveva disegnare", 2, spostati.size)
        assertTrue("l'elemento scelto doveva spostarsi a destra", spostati[0].points[0].x > prima[0].points[0].x)
        assertEquals("in verticale doveva restare", prima[0].points[0].y, spostati[0].points[0].y, 1e-4f)
        assertEquals("l'altro doveva restare dov'era", prima[1].points, spostati[1].points)
        banco.onNodeWithContentDescription(testo(R.string.pick_delete)).performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        assertEquals("Elimina doveva togliere l'elemento scelto", listOf(spostati[1]), salvato!!.drawing.marks)
        banco.onNodeWithContentDescription(testo(R.string.editor_undo)).performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        assertEquals("Annulla doveva riportarlo", 2, salvato!!.drawing.marks.size)
    }

    /**
     * **Il tasto acceso ha un bordo pieno color accento, negli strumenti e nei tasti** (sua nota A
     * sul giro della `4.60`: *aggiungi anche un bordo pieno di qualche DP intorno al tasto attivo*).
     * Di fabbrica sono accesi il rettangolo, Traccia e Tratteggio; spenti la linea e Spessore.
     * ⚠️⚠️ **CONTROPROVATA** togliendo il bordo dai due posti: nei tasti accesi l'accento non c'è.
     */
    @Test
    fun `il tasto acceso ha un bordo pieno`() {
        var accento = androidx.compose.ui.graphics.Color.Unspecified
        banco.setContent { Scena(onAccent = { accento = it }) }
        pronta()
        apriDisegno()
        fun bordo(id: Int) =
            inchiostro(banco.onNodeWithContentDescription(testo(id)).captureToImage().toPixelMap(), accento)
        for (id in listOf(R.string.draw_rect, R.string.draw_outline, R.string.draw_dashed)) {
            assertTrue("'${testo(id)}' acceso doveva avere il bordo", bordo(id) > 100)
        }
        for (id in listOf(R.string.draw_line, R.string.draw_width)) {
            assertEquals("'${testo(id)}' spento non doveva averlo", 0, bordo(id))
        }
    }

    /**
     * **Il tratteggio del tasto comincia e finisce con un trattino** (sua nota C sul giro della
     * `4.60`: *tocchi le due estremità laterali del tasto con il tratto effettivo, non con lo
     * spazio*). Il conto: a ogni larghezza i trattini chiudono sul bordo, col ritmo del suo mockup
     * (10 e 6). Il disegno: sul tasto spento, che non ha il bordo pieno, i due capi della banda
     * sono pieni.
     * ⚠️ Lo schermo di 441 dp perché là il tasto è largo 77 dp, e col tratteggio fisso la banda
     * finiva a metà di uno spazio; a 411 dp finiva per caso su un trattino.
     * ⚠️⚠️ **CONTROPROVATA** rimettendo il tratteggio fisso: a destra la banda finisce nel vuoto.
     */
    @Test
    @Config(qualifiers = "w441dp-h891dp")
    fun `il tratteggio del tasto comincia e finisce con un trattino`() {
        for (w in listOf(40f, 52.8f, 71f, 100f, 213f)) {
            val (t, v) = keyDashes(w, 10f, 6f)
            val n = ((w + v) / (t + v)).roundToInt()
            assertEquals("a $w i trattini chiudono sul bordo", w, n * t + (n - 1) * v, 1e-3f)
            assertEquals("a $w il ritmo resta il suo", 10f / 6f, t / v, 1e-4f)
        }
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        val tasto = banco.onNodeWithContentDescription(testo(R.string.draw_dashed))
        tasto.performClick()
        banco.waitForIdle()
        val m = tasto.captureToImage().toPixelMap()
        // ⚠️ The scene is chosen so the fixed pattern of 4.60 (10 and 6) would end in a space: on a
        // key where it ends on a dash by chance, the two drawings look the same.
        val largo = m.width / app.resources.displayMetrics.density
        assertTrue("la scena deve far finire il tratteggio fisso in uno spazio: tasto di $largo dp",
            largo % 16f in 10.5f..15.5f)
        val y = m.height / 2
        val sopra = m.height / 4
        assertFalse("a sinistra la banda doveva cominciare con un trattino", vicino(m[2, y], m[2, sopra]))
        assertFalse("a destra la banda doveva finire con un trattino", vicino(m[m.width - 3, y], m[m.width - 3, sopra]))
    }

    /**
     * **Il cursore della luminosità non vela quello che c'è sotto** (sua nota D sul giro della
     * `4.60`: *la sfocatura e il velo non consentono di vedere in tempo reale il colore che si sta
     * applicando*): con la sfocatura scelta, come di fabbrica, il palco catturato col cursore
     * aperto è identico a prima. Il velo lo dipinge l'app sopra tutto, quindi una cattura lo vede;
     * la sfocatura di finestra il banco non la vede, e la toglie la stessa riga che toglie il velo.
     * ⚠️⚠️ **CONTROPROVATA** togliendo la scelta dell'ombra intorno al cursore: il palco si scurisce.
     */
    @Test
    fun `il cursore della luminosita non vela il palco`() {
        banco.setContent { CompositionLocalProvider(LocalAivDepth provides PanelDepth.BLUR) { Scena() } }
        pronta()
        apriDisegno()
        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        val prima = palco.captureToImage().toPixelMap()
        banco.onNodeWithContentDescription(testo(R.string.ink_red)).performTouchInput { longClick() }
        banco.waitForIdle()
        assertEquals("tenuto il tondo, il cursore doveva restare", 1, luci())
        assertEquals("col cursore aperto il palco non doveva cambiare", 0,
            differenza(prima, palco.captureToImage().toPixelMap()))
    }

    /**
     * **La prima volta che il Disegno si apre compare il suo velo d'aiuto**, col tondo rosso
     * cerchiato d'arancione e la frase sopra, senza coprirlo (sua nota B sul giro della `4.60`); un
     * tocco lo chiude e lo ricorda.
     * ⚠️⚠️ **CONTROPROVATA** due volte: col velo spento, e col cerchio del colore del tondo.
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `alla prima apertura del Disegno compare il suo velo d'aiuto`() {
        runBlocking { Hint.DRAW.forget(app) }
        banco.setContent { Scena() }
        pronta()
        assertEquals("prima del Disegno il velo non doveva esserci", 0,
            banco.onAllNodesWithText(testo(R.string.hint_draw)).fetchSemanticsNodes().size)
        apriDisegno()
        // ⚠️ The unmerged tree: the veil is one clickable node that takes the sentence in, and the
        // merged search would answer with the whole screen.
        val frase = banco.onNodeWithText(testo(R.string.hint_draw), useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val tondo = banco.onNodeWithContentDescription(testo(R.string.ink_red)).fetchSemanticsNode().boundsInRoot
        assertTrue("la frase doveva stare sopra il tondo, senza coprirlo: ${frase.bottom} e ${tondo.top}",
            frase.bottom <= tondo.top)
        val schermo = banco.onRoot().captureToImage().toPixelMap()
        val x = tondo.center.x.toInt()
        assertTrue("il bordo del tondo rosso doveva essere arancione", vicino(schermo[x, tondo.top.toInt() + 1], HINT_MARK))
        assertTrue("dentro, il tondo doveva restare rosso",
            vicino(schermo[x, tondo.center.y.toInt()], androidx.compose.ui.graphics.Color(Draw.INKS.first())))
        banco.onNodeWithText(testo(R.string.hint_draw)).performClick()
        // ⚠️ The veil goes when the archive answers, which the bench's idling does not wait for.
        banco.waitUntil(5_000) { banco.onAllNodesWithText(testo(R.string.hint_draw)).fetchSemanticsNodes().isEmpty() }
        assertTrue("e ricordarlo", runBlocking { Hint.DRAW.flow(app).first() })
    }

    /**
     * **Il conto dell'aggancio ai bordi** (`4.62`, sua richiesta del 2026-10-08: *un piccolo scatto
     * calamitato ... a filo del bordo*): un lato entro la portata va sul bordo, da dentro o da fuori;
     * oltre la portata resta dov'è; su un asse vince il bordo più vicino; la punta di una freccia
     * conta nell'ingombro, e una freccia che sale verso il bordo ci arriva con la punta intera.
     */
    @Test
    fun `il conto dell'aggancio ai bordi`() {
        val cornice = androidx.compose.ui.geometry.Rect(0f, 0f, 1000f, 800f)
        fun box(l: Float, t: Float, r: Float, b: Float) = androidx.compose.ui.geometry.Rect(l, t, r, b)
        assertEquals(Offset(-8f, 0f) to setOf(ImageEdge.LEFT), Draw.rest(box(8f, 300f, 200f, 400f), cornice, 12f))
        assertEquals("da fuori rientra", Offset(6f, 0f) to setOf(ImageEdge.LEFT), Draw.rest(box(-6f, 300f, 200f, 400f), cornice, 12f))
        assertEquals("oltre la portata resta", Offset.Zero to emptySet<ImageEdge>(), Draw.rest(box(-30f, 300f, 200f, 400f), cornice, 12f))
        assertEquals("vince il bordo più vicino", Offset(-5f, 0f) to setOf(ImageEdge.LEFT), Draw.rest(box(5f, 300f, 990f, 400f), cornice, 12f))
        assertEquals("in un angolo, due bordi", Offset(-3f, 4f) to setOf(ImageEdge.LEFT, ImageEdge.BOTTOM),
            Draw.rest(box(3f, 700f, 100f, 796f), cornice, 12f))
        val freccia = Draw.extent(Pen.ARROW, Offset(100f, 100f), Offset(300f, 100f), 10f, 1000f)
        assertEquals("la punta e il mezzo tratto", 305f, freccia.right, 1e-3f)
        assertTrue("le ali della punta allargano l'ingombro", freccia.top < 100f - 5f - 20f)
        val (fine, bordi) = Draw.restEnd(Pen.ARROW, Offset(500f, 400f), Offset(530f, 8f), 10f, 1000f, cornice, 12f)
        assertEquals("la freccia arriva al bordo con la punta intera", 0f,
            Draw.extent(Pen.ARROW, Offset(500f, 400f), fine, 10f, 1000f).top, 0.5f)
        assertTrue(ImageEdge.TOP in bordi)
    }

    /**
     * **Un rettangolo cominciato vicino a un angolo si appoggia ai due bordi, e uno finito vicino
     * all'angolo opposto anche; lontano dai bordi resta dove cade il dito** (`4.62`). A filo vuol dire
     * che il mezzo tratto esterno cade sul bordo: il punto è a metà spessore dal bordo.
     * ⚠️ L'immagine è quadrata, quindi sullo schermo il suo lato lungo è la sua larghezza e il mezzo
     * spessore, in frazioni, è la metà di [Mark.width].
     * ⚠️⚠️ **CONTROPROVATA** due volte: senza l'aggancio del primo punto, e senza quello del secondo.
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `un rettangolo vicino a un angolo si appoggia ai due bordi`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        val img = immagine()
        trascinaDa(Offset(img.left + 4f, img.top + 4f), Offset(-20f, -20f))
        trascinaDa(Offset(20f, 20f), Offset(img.right - 4f, img.bottom - 4f))
        trascinaDa(Offset(img.left + 40f, -60f), Offset(0f, -30f))
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        val (primo, secondo, terzo) = salvato!!.drawing.marks
        val mezzo = primo.width / 2f
        assertEquals("a filo del bordo sinistro", mezzo, primo.points.minOf { it.x }, 1e-3f)
        assertEquals("a filo del bordo di sopra", mezzo, primo.points.minOf { it.y }, 1e-3f)
        assertEquals("a filo del bordo destro", 1f - mezzo, secondo.points.maxOf { it.x }, 1e-3f)
        assertEquals("a filo del bordo di sotto", 1f - mezzo, secondo.points.maxOf { it.y }, 1e-3f)
        assertTrue("lontano dai bordi l'elemento resta dov'è", terzo.points.minOf { it.x } > mezzo + 0.05f)
    }

    /**
     * **Un elemento spostato vicino a un bordo vi si appoggia, e spinto oltre la portata esce
     * dall'immagine** (`4.62`, *dev'essere possibile ... spostare gli elementi di Disegno anche OLTRE
     * i bordi*).
     * ⚠️⚠️ **CONTROPROVATA** togliendo l'aggancio dallo spostamento: il lato sinistro resta a 5 pixel
     * dal bordo.
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `un elemento spostato vicino al bordo vi si appoggia, e oltre esce`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        val img = immagine()
        trascinaDa(Offset(-40f, -40f), Offset(40f, 40f))
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        val mezzo = salvato!!.drawing.marks.single().width / 2f
        // The stroke's outer side, on the screen: half the stroke left of the corner at -40.
        val fuori = -40f - mezzo * (img.right - img.left)
        tocca(Offset.Zero)
        trascinaDa(Offset.Zero, Offset(img.left + 5f - fuori, 0f))
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        assertEquals("a filo del bordo sinistro", mezzo, salvato!!.drawing.marks.single().points.minOf { it.x }, 1e-3f)
        trascinaDa(Offset(img.left + 30f, 0f), Offset(img.left - 40f, 0f))
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        assertTrue("spinto oltre, l'elemento doveva uscire dal bordo",
            salvato!!.drawing.marks.single().points.minOf { it.x } < 0f)
    }

    /**
     * **Mentre il dito tiene un elemento appoggiato a un bordo, la guida corre lungo quel bordo; allo
     * stacco sparisce** (`4.62`, *una nuova 'guida dinamica'*).
     * ⚠️⚠️ **CONTROPROVATA** togliendo la guida dei bordi dal palco: nella colonna del bordo l'accento
     * non c'è.
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `mentre l'elemento tocca un bordo la guida corre lungo il bordo`() {
        var accento = androidx.compose.ui.graphics.Color.Unspecified
        banco.setContent { Scena(onAccent = { accento = it }) }
        pronta()
        apriDisegno()
        val img = immagine()
        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        palco.performTouchInput { down(center + Offset(img.left + 4f, -60f)) }
        palco.performTouchInput { moveTo(center + Offset(-20f, 60f)) }
        banco.waitForIdle()
        val giu = palco.captureToImage().toPixelMap()
        palco.performTouchInput { up() }
        banco.waitForIdle()
        val su = palco.captureToImage().toPixelMap()
        val x = (giu.width / 2f + img.left).toInt()
        val alto = (img.bottom - img.top).toInt()
        fun colonna(m: PixelMap) = (0 until m.height).count { vicino(m[x, it], accento) }
        assertTrue("col dito giù la guida doveva correre lungo il bordo sinistro: ${colonna(giu)} su $alto",
            colonna(giu) > alto * 8 / 10)
        assertEquals("allo stacco la guida doveva sparire", 0, colonna(su))
    }

    /**
     * Il riquadro dell'immagine sul palco, misurato dal centro del palco come i gesti di
     * [trascinaDa]: i pixel bianchi del quadrato di prova.
     */
    private fun immagine(): androidx.compose.ui.geometry.Rect {
        val m = banco.onNodeWithContentDescription(testo(R.string.look_compare)).captureToImage().toPixelMap()
        var x0 = Int.MAX_VALUE; var x1 = -1; var y0 = Int.MAX_VALUE; var y1 = -1
        for (y in 0 until m.height) for (x in 0 until m.width) {
            val c = m[x, y]
            if (c.red > 0.97f && c.green > 0.97f && c.blue > 0.97f) {
                x0 = minOf(x0, x); x1 = maxOf(x1, x); y0 = minOf(y0, y); y1 = maxOf(y1, y)
            }
        }
        val cx = m.width / 2f
        val cy = m.height / 2f
        return androidx.compose.ui.geometry.Rect(x0 - cx, y0 - cy, x1 + 1 - cx, y1 + 1 - cy)
    }

    /** Un tocco sul palco, a [da] dal suo centro. */
    private fun tocca(da: Offset) {
        banco.onNodeWithContentDescription(testo(R.string.look_compare)).performTouchInput {
            down(center + da)
            up()
        }
        banco.waitForIdle()
    }

    /** Un trascinamento sul palco da [da] ad [a], misurati dal suo centro, nelle tre chiamate di [trascina]. */
    private fun trascinaDa(da: Offset, a: Offset) {
        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        palco.performTouchInput { down(center + da) }
        palco.performTouchInput { moveTo(center + a) }
        palco.performTouchInput { up() }
        banco.waitForIdle()
    }

    /** Quanti pixel differiscono fra due catture della stessa misura. */
    private fun differenza(a: PixelMap, b: PixelMap): Int {
        var n = 0
        for (y in 0 until a.height) for (x in 0 until a.width) if (a[x, y] != b[x, y]) n++
        return n
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

    /**
     * Quanti pixel rossi ha la colonna [x], di serie quella di mezzo: lo spessore di una linea rossa.
     * ⚠️ Conta anche i pixel del bordo coperti per più di metà, che sul fondo chiaro hanno ancora
     * il verde sotto 0,6: così una linea a cavallo di due righe di pixel non perde un pixel.
     */
    private fun colonna(mappa: PixelMap, x: Int = mappa.width / 2): Int =
        (0 until mappa.height).count { mappa[x, it].let { c -> c.red > 0.8f && c.green < 0.6f && c.blue < 0.6f } }

    /** Larghezza e altezza del rettangolo che contiene i pixel di [colore]. */
    private fun ingombro(mappa: PixelMap, colore: androidx.compose.ui.graphics.Color): Pair<Int, Int> {
        var x0 = Int.MAX_VALUE; var x1 = -1; var y0 = Int.MAX_VALUE; var y1 = -1
        for (y in 0 until mappa.height) for (x in 0 until mappa.width) if (vicino(mappa[x, y], colore)) {
            x0 = minOf(x0, x); x1 = maxOf(x1, x); y0 = minOf(y0, y); y1 = maxOf(y1, y)
        }
        return Pair(x1 - x0 + 1, y1 - y0 + 1)
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
    private fun Scena(onSave: (Look) -> Unit = {}, onAccent: (androidx.compose.ui.graphics.Color) -> Unit = {}) {
        AivTheme(darkTheme = false) {
            onAccent(MaterialTheme.colorScheme.primary)
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

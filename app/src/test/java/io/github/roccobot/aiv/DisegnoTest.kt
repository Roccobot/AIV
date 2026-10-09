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
import androidx.compose.ui.test.assertIsOff
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
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isPopup
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
     * ⚠️ Dalla `4.90` il rettangolo grande comincia lontano dai lati di quello piccolo: cominciato
     * nello stesso punto ne condivide due lati, e le guide verso gli altri elementi cambiano il
     * palco quanto la lente.
     */
    @Test
    fun `la lente compare sulle forme piccole e non su quelle grandi`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        banco.onNodeWithContentDescription(testo(R.string.draw_rect)).performClick()
        banco.waitForIdle()
        assertTrue("col rettangolo piccolo doveva comparire la lente", diffDurante(PASSO, PASSO) > 500)
        assertEquals("col rettangolo grande la lente non doveva esserci", 0,
            diffDurante(-PASSO * 4, PASSO * 4, Offset(-30f, -60f)))
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
     * ⚠️ Dalla `4.90` la linea storta comincia lontano da quella agganciata: cominciata nello
     * stesso punto ne condivide l'inizio, e le guide verso gli altri elementi cambiano il palco.
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
        assertEquals("con la linea storta la guida non doveva esserci", 0,
            diffDurante(PASSO * 5, PASSO * 3, Offset(-30f, -60f)))
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
        // ⚠️ Until 4.70 the 'Elimina' key said whether an element was chosen; since 4.80 its column
        // is Sfocatura, and the handles on the stage say it.
        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        val prima = palco.captureToImage().toPixelMap()
        tocca(Offset(-70f, -70f))
        val scelto = palco.captureToImage().toPixelMap()
        assertTrue("scelto l'elemento, i suoi vertici dovevano comparire", differenza(prima, scelto) > 40)
        tocca(Offset(80f, 80f))
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
     * **The module's command is 'Elimina tutto', and it empties the drawing and drops the choice**
     * (4.64, his answer in chat: *Allora può restare, ma rinominalo in 'Elimina tutto'*). Until
     * 4.63 it was called 'Azzera', the word of the bar's key that resets every module.
     * ⚠️⚠️ **CONTROPROVATA** with the old string: the command is not found by its new name. And
     * since 4.80, without the line that drops the choice: a free hand tap leaves no dot.
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `Elimina tutto svuota il disegno e toglie la scelta`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        banco.onNodeWithContentDescription(testo(R.string.draw_free)).performClick()
        banco.waitForIdle()
        trascinaDa(Offset(-100f, -100f), Offset(-40f, -40f))
        tocca(Offset(-70f, -70f))
        banco.onNodeWithText(testo(R.string.editor_original)).assertDoesNotExist()
        val tutto = banco.onNodeWithText(testo(R.string.draw_clear))
        tutto.performClick()
        banco.waitForIdle()
        tutto.assertIsNotEnabled()
        assertFalse("dopo Elimina tutto la scelta non doveva restare", sceltaRimasta {
            // ⚠️ With nothing drawn 'Salva' is off, and nothing is saved.
            banco.onNodeWithText(testo(R.string.editor_save)).performClick()
            banco.waitForIdle()
            salvato?.drawing?.marks.orEmpty()
        })
    }

    /**
     * **Whether a choice is left in place after the drawing was emptied**: with one, a tap of the
     * free hand only drops it and leaves no dot. Since 4.80 the 'Elimina' key, whose light used to
     * say it, has left its column to Sfocatura.
     */
    private fun sceltaRimasta(salva: () -> List<Mark>): Boolean {
        tocca(Offset(80f, 80f))
        return salva().isEmpty()
    }

    /**
     * **Holding the Disegno chip empties the drawing and drops the choice too** (4.64). Until 4.63
     * it emptied the drawing and left the choice on an element that was no longer there: 'Elimina'
     * stayed lit, and the next element drawn was born chosen, so the module's parameters changed it.
     * 'Elimina tutto' already dropped the choice, and with it kept the hold is the other way to the
     * same result.
     * ⚠️⚠️ **CONTROPROVATA** without the line that drops the choice: until 4.70 'Elimina' stayed lit
     * after the hold; since 4.80, when that key has left its column to Sfocatura, a free hand tap
     * after the hold only drops the choice that was left, and leaves no dot ([sceltaRimasta]).
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `tenere il gettone del Disegno toglie anche la scelta`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        banco.onNodeWithContentDescription(testo(R.string.draw_free)).performClick()
        banco.waitForIdle()
        trascinaDa(Offset(-100f, -100f), Offset(-40f, -40f))
        tocca(Offset(-70f, -70f))
        banco.onNodeWithContentDescription(testo(R.string.look_draw)).performTouchInput { longClick() }
        banco.waitForIdle()
        assertFalse("tenuto il gettone, la scelta non doveva restare", sceltaRimasta {
            // ⚠️ With nothing drawn 'Salva' is off, and nothing is saved.
            banco.onNodeWithText(testo(R.string.editor_save)).performClick()
            banco.waitForIdle()
            salvato?.drawing?.marks.orEmpty()
        })
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
        // ⚠️ 'Elimina' is in the element's menu since 4.80, and a key of words of the module since 4.92.
        banco.onNodeWithContentDescription(testo(R.string.look_compare))
            .performTouchInput { longClick(center + Offset(-70f + PASSO, -70f)) }
        banco.waitForIdle()
        vocePopup(R.string.pick_delete).performClick()
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
     * ⚠️ Lo schermo di 441 dp perché là il tasto è largo circa 46 dp, e col tratteggio fisso la
     * banda finirebbe a metà di uno spazio. Fino alla `4.81` era di 441 dp, coi tasti larghi 77;
     * nella `4.90`, con sette colonne, di 495 dp, coi tasti larghi 61; dalla `4.91` le colonne sono
     * otto, e a 495 il tratteggio fisso finiva per caso su un trattino.
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
     * **Il conto delle guide verso gli altri elementi** (`4.90`, sua richiesta del 2026-10-08:
     * *Voglio che mi propongano di allineare dinamicamente gli elementi a lati/centro/estremi di
     * altri elementi già presenti*): un lato entro la portata va sul lato o sul centro di un altro
     * elemento; spostando tutto l'elemento si allinea anche il suo centro; vince il bersaglio più
     * vicino e, a parità, il bordo dell'immagine; un lato che non si muove non si aggancia; le
     * guide vanno da un elemento all'altro lungo la linea che hanno in comune.
     */
    @Test
    fun `il conto delle guide verso gli altri elementi`() {
        val cornice = androidx.compose.ui.geometry.Rect(0f, 0f, 1000f, 800f)
        fun box(l: Float, t: Float, r: Float, b: Float) = androidx.compose.ui.geometry.Rect(l, t, r, b)
        val altro = box(300f, 300f, 500f, 400f)
        assertEquals("il lato sinistro va sul lato sinistro dell'altro", Offset(-7f, 0f),
            Draw.rest(box(307f, 500f, 420f, 600f), cornice, 12f, others = listOf(altro)).first)
        assertEquals("il lato sinistro va sul centro dell'altro", Offset(5f, 0f),
            Draw.rest(box(395f, 500f, 450f, 600f), cornice, 12f, others = listOf(altro)).first)
        assertEquals("il centro va sul centro dell'altro", Offset(0f, -6f),
            Draw.rest(box(600f, 320f, 700f, 392f), cornice, 12f, others = listOf(altro)).first)
        assertEquals("oltre la portata resta", Offset.Zero,
            Draw.rest(box(330f, 500f, 380f, 600f), cornice, 12f, others = listOf(altro)).first)
        assertEquals("a parità vince il bordo dell'immagine", Offset(-6f, 0f) to setOf(ImageEdge.LEFT),
            Draw.rest(box(6f, 500f, 100f, 600f), cornice, 12f, others = listOf(box(12f, 100f, 30f, 200f))))
        assertEquals("un lato che non si muove non si aggancia", Offset.Zero,
            Draw.rest(box(305f, 500f, 450f, 600f), cornice, 12f, setOf(ImageEdge.RIGHT), listOf(altro)).first)
        val guide = Draw.lines(box(300f, 500f, 380f, 600f), listOf(altro))
        assertEquals("una guida sola, lungo il lato sinistro, da un elemento all'altro",
            listOf(Offset(300f, 300f) to Offset(300f, 600f)), guide)
        assertEquals("senza niente in comune, nessuna guida", emptyList<Pair<Offset, Offset>>(),
            Draw.lines(box(320f, 500f, 380f, 600f), listOf(altro)))
    }

    /**
     * **Spostato, un elemento si centra sull'immagine** (`4.91`, sua nota A sul giro della `4.90`:
     * *devono apparire anche delle guide per la centratura (che faccia fare uno scatto allo
     * spostamento di un elemento quando è al centro verticale/orizzontale/entrambi dell'intera
     * immagine)*): il centro dell'elemento entro la portata va sul centro dell'immagine, su un asse o
     * su tutti e due, con la guida che attraversa l'immagine; un elemento che si disegna non ci va, e
     * nemmeno un lato.
     * ⚠️⚠️ **CONTROPROVATA**: senza il centro dell'immagine fra i bersagli di `Draw.rest` i due scatti
     * non avvengono, e senza la guida in `Draw.lines` la guida manca.
     */
    @Test
    fun `spostato un elemento si centra sull'immagine`() {
        val cornice = androidx.compose.ui.geometry.Rect(0f, 0f, 1000f, 800f)
        fun box(l: Float, t: Float, r: Float, b: Float) = androidx.compose.ui.geometry.Rect(l, t, r, b)
        assertEquals("il centro va sul centro orizzontale", Offset(-7f, 0f),
            Draw.rest(box(457f, 100f, 557f, 200f), cornice, 12f, centred = true).first)
        assertEquals("e su tutti e due", Offset(5f, -4f),
            Draw.rest(box(445f, 354f, 545f, 454f), cornice, 12f, centred = true).first)
        assertEquals("un elemento che si disegna non ci va", Offset.Zero,
            Draw.rest(box(457f, 100f, 557f, 200f), cornice, 12f).first)
        assertEquals("un lato non si ferma sul centro", Offset.Zero,
            Draw.rest(box(505f, 100f, 700f, 200f), cornice, 12f, setOf(ImageEdge.LEFT), centred = true).first)
        assertEquals("la guida attraversa l'immagine", listOf(Offset(500f, 0f) to Offset(500f, 800f)),
            Draw.lines(box(450f, 100f, 550f, 200f), emptyList(), cornice))
        assertEquals("centrato su tutti e due, due guide", 2, Draw.lines(box(450f, 350f, 550f, 450f), emptyList(), cornice).size)
        assertEquals("fuori centro nessuna", emptyList<Pair<Offset, Offset>>(), Draw.lines(box(460f, 100f, 550f, 200f), emptyList(), cornice))
    }

    /**
     * **Un elemento disegnato o spostato vicino a un altro si allinea a lui** (`4.90`, sua richiesta
     * del 2026-10-08). Il secondo rettangolo comincia 3 pixel a destra del lato sinistro del primo,
     * e il suo punto di partenza va su quel lato; poi, spostato con il centro a 2 pixel da quello del
     * primo, il suo centro va sul centro del primo.
     * ⚠️⚠️ **CONTROPROVATA**: senza gli altri elementi fra i bersagli di `Draw.rest` il punto resta
     * dove cade il dito.
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `un elemento disegnato o spostato vicino a un altro si allinea a lui`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        trascinaDa(Offset(-100f, -100f), Offset(-40f, -40f))
        trascinaDa(Offset(-97f, 20f), Offset(-10f, 60f))
        fun salva(): List<Mark> {
            banco.onNodeWithText(testo(R.string.editor_save)).performClick()
            banco.waitForIdle()
            return salvato!!.drawing.marks
        }
        val (primo, secondo) = salva()
        assertEquals("il secondo doveva cominciare sul lato sinistro del primo",
            primo.points.first().x, secondo.points.first().x, 1e-4f)
        // ⚠️ The second one's centre is at -55 px and the first one's at -70: 14.5 px to the left
        // brings it within half a pixel, and no side of the second comes as near a side. Under the
        // bench's touch slop (16 px, Robolectric's ShadowViewConfiguration) a single move of 14.5
        // is a tap, so the finger goes past it first and comes back: every frame moves the element
        // from where the finger went down.
        tocca(Offset(-50f, 40f))
        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        palco.performTouchInput { down(center + Offset(-50f, 40f)) }
        palco.performTouchInput { moveTo(center + Offset(-90f, 40f)) }
        palco.performTouchInput { moveTo(center + Offset(-64.5f, 40f)) }
        palco.performTouchInput { up() }
        banco.waitForIdle()
        val (uno, due) = salva()
        assertEquals("spostato, il centro del secondo doveva andare sul centro del primo",
            (uno.points.first().x + uno.points.last().x) / 2f, (due.points.first().x + due.points.last().x) / 2f, 1e-4f)
    }

    /**
     * **In `Trasforma` il lato tirato da una maniglia si ferma sul lato di un altro elemento**
     * (`4.90`, lettura B1 della sessione: le guide valgono anche ridimensionando). Il secondo
     * rettangolo, tirato per la maniglia a metà del lato destro fino a 3 pixel dal lato destro del
     * primo, finisce sullo stesso lato; quello sinistro resta dov'era.
     * ⚠️⚠️ **CONTROPROVATA**: senza l'appoggio nel ramo delle maniglie il lato resta a 3 pixel.
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `il lato tirato da una maniglia si ferma sul lato di un altro elemento`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        trascinaDa(Offset(-100f, -100f), Offset(-40f, -40f))
        trascinaDa(Offset(-160f, 20f), Offset(0f, 60f))
        fun salva(): List<Mark> {
            banco.onNodeWithText(testo(R.string.editor_save)).performClick()
            banco.waitForIdle()
            return salvato!!.drawing.marks
        }
        val prima = salva()[1]
        tocca(Offset(-80f, 40f))
        trascinaDa(Offset(0f, 40f), Offset(-37f, 40f))
        val (uno, due) = salva()
        assertEquals("il lato destro del secondo doveva finire sul lato destro del primo",
            uno.points.last().x, due.points.last().x, 1e-4f)
        assertEquals("il lato sinistro doveva restare", prima.points.first().x, due.points.first().x, 1e-4f)
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
     * **Il conto della `4.70`: giro, maniglie, ridimensionamento, stile e livelli**, su un'immagine
     * non quadrata (200 x 100), dove un giro fatto sulle frazioni invece che sui pixel deformerebbe
     * la forma.
     * ⚠️⚠️ **CONTROPROVATA** tre volte: senza la rotazione della tela in `Draw.paint` (il pixel del
     * rettangolo girato resta trasparente), senza il tocco riportato indietro in `Draw.touches` (il
     * tocco dentro la forma girata la manca), e col giro della linea fatto sulle frazioni (la linea
     * girata di 90 gradi cambia lunghezza).
     */
    @Test
    fun `il conto di giro, maniglie, stile e livelli`() {
        val w = 200
        val h = 100
        fun uguale(cosa: String, atteso: Offset, visto: Offset) {
            assertEquals("$cosa, x", atteso.x, visto.x, 1e-4f)
            assertEquals("$cosa, y", atteso.y, visto.y, 1e-4f)
        }
        val rett = Mark(Pen.RECT, listOf(Offset(0.2f, 0.4f), Offset(0.6f, 0.6f)), Color.RED, 0.01f, false, Color.GREEN)
        val maniglie = rett.handles(w, h)
        assertEquals("quattro angoli e quattro lati", 8, maniglie.size)
        uguale("angolo in alto a sinistra", Offset(0.2f, 0.4f), maniglie[0])
        uguale("angolo in basso a destra", Offset(0.6f, 0.6f), maniglie[2])
        uguale("mezzo del lato di sopra", Offset(0.4f, 0.4f), maniglie[4])
        uguale("mezzo del lato sinistro", Offset(0.2f, 0.5f), maniglie[7])

        assertEquals("un giro libero resta com'è", 30f, rett.turned(30f, w, h).angle, 1e-4f)
        assertEquals("vicino a 45 gradi si aggancia", 45f, rett.turned(42f, w, h).angle, 1e-4f)
        assertEquals("vicino al mezzo giro si aggancia, nel suo intervallo", 180f, rett.turned(-178f, w, h).angle, 1e-4f)
        val girato = rett.turned(90f, w, h)
        assertEquals("il giro non tocca i punti del rettangolo", rett.points, girato.points)
        uguale("l'angolo in alto a sinistra del rettangolo girato", Offset(0.45f, 0.1f), girato.handles(w, h)[0])
        assertEquals("dentro la forma girata, fuori da quella dritta", 0,
            Draw.hit(Drawing(listOf(girato)), Offset(0.4f, 0.85f), w, h, 0.01f))
        assertEquals("dentro la forma dritta, fuori da quella girata", null,
            Draw.hit(Drawing(listOf(girato)), Offset(0.55f, 0.5f), w, h, 0.01f))
        val tela = Draw.overlay(Drawing(listOf(girato)), w, h, Spin(0, false))!!
        assertEquals("il riempimento è dove la forma girata arriva", Color.GREEN, tela.getPixel(80, 85))
        assertEquals("e non dove arrivava quella dritta", 0, tela.getPixel(110, 50))
        val contorno = Draw.outline(girato, w, h)
        assertEquals(0.35f, contorno.minOf { it.x }, 1e-3f)
        assertEquals(0.45f, contorno.maxOf { it.x }, 1e-3f)
        assertEquals(0.1f, contorno.minOf { it.y }, 1e-3f)
        assertEquals(0.9f, contorno.maxOf { it.y }, 1e-3f)

        val tirato = rett.reshaped(2, Offset(0.8f, 0.8f), w, h)
        uguale("tirando un angolo l'opposto resta", Offset(0.2f, 0.4f), tirato.points.first())
        uguale("e l'angolo va dove è tirato", Offset(0.8f, 0.8f), tirato.points.last())
        val allungato = girato.reshaped(5, Offset(0.4f, 0.95f), w, h)
        assertEquals("allungando un lato il giro resta", 90f, allungato.angle, 1e-4f)
        uguale("il lato destro del rettangolo girato scende, il sinistro resta", Offset(0.1875f, 0.425f), allungato.points.first())
        uguale("il lato destro del rettangolo girato scende", Offset(0.6125f, 0.625f), allungato.points.last())
        val mano = Mark(Pen.FREE, listOf(Offset(0.1f, 0.1f), Offset(0.2f, 0.3f), Offset(0.3f, 0.2f)), Color.RED, 0.01f, false, null)
        assertEquals("la mano libera si stira col suo riquadro", listOf(0.1f, 0.3f, 0.5f),
            mano.reshaped(5, Offset(0.5f, 0.9f), w, h).points.map { (it.x * 1e4f).roundToInt() / 1e4f })
        assertEquals("e in altezza resta", mano.points.map { (it.y * 1e4f).roundToInt() },
            mano.reshaped(5, Offset(0.5f, 0.9f), w, h).points.map { (it.y * 1e4f).roundToInt() })

        val linea = Mark(Pen.LINE, listOf(Offset(0.2f, 0.5f), Offset(0.6f, 0.5f)), Color.RED, 0.01f, false, null)
        val ritta = linea.turned(88f, w, h)
        uguale("la linea girata si aggancia alla verticale e tiene la sua lunghezza", Offset(0.4f, 0.1f), ritta.points.first())
        uguale("la linea girata, l'altro capo", Offset(0.4f, 0.9f), ritta.points.last())

        val blu = Mark(Pen.LINE, listOf(Offset(0f, 0f), Offset(1f, 1f)), Color.BLUE, 0.02f, true, null)
        val stilato = rett.styledLike(blu)
        assertEquals(Color.BLUE, stilato.ink)
        assertEquals(0.02f, stilato.width)
        assertTrue(stilato.dashed)
        assertEquals("una linea non ha un riempimento da dare", Color.GREEN, stilato.fill)
        assertEquals("i punti restano suoi", rett.points, stilato.points)
        val ellisse = Mark(Pen.ELLIPSE, listOf(Offset(0f, 0f), Offset(0.1f, 0.1f)), Color.BLACK, 0.005f, false, Color.YELLOW)
        assertEquals("fra due forme chiuse passa anche il riempimento", Color.YELLOW, girato.styledLike(ellisse).fill)
        assertEquals("il giro resta suo", 90f, girato.styledLike(ellisse).angle, 1e-4f)
        assertEquals("una freccia non prende un riempimento", null,
            Mark(Pen.ARROW, linea.points, Color.RED, 0.01f, false, null).styledLike(ellisse).fill)

        val disegno = Drawing(listOf(rett, linea, ellisse))
        assertEquals(listOf(linea, rett, ellisse), disegno.swapping(0, 1).marks)
        assertEquals("oltre la cima non si sposta niente", disegno, disegno.swapping(2, 3))
        assertEquals(listOf(rett, blu, linea, ellisse), disegno.inserting(1, blu).marks)
        uguale("la copia si posa al 3% del lato lungo", Offset(0.03f, 0.06f), Draw.duplicateShift(w, h))
    }

    /**
     * **Tenendo fermo un elemento si apre il suo menu di sei voci, e le voci funzionano** (`4.70`,
     * sua nota E sul giro della `4.60` e sue risposte su `4.64-03`): `Duplica` posa una copia
     * scostata e la sceglie, `Sposta sotto` la porta sotto l'originale, il tocco fuori dal menu lo
     * chiude senza disegnare, `Copia` diventa `Incolla` e porta lo stile, e tenendo `Incolla` la
     * memoria si svuota con l'avviso.
     * ⚠️⚠️ **CONTROPROVATA** tre volte: senza l'attesa del tocco lungo nel palco (il menu non
     * compare), con `Incolla` che non posa lo stile (l'elemento tiene il suo colore), e senza lo
     * svuotamento al tocco lungo (l'avviso non compare).
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `tenendo fermo un elemento si apre il suo menu`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        trascinaDa(Offset(-100f, -100f), Offset(-40f, -40f))
        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        fun tieni(da: Offset) {
            palco.performTouchInput { longClick(center + da) }
            banco.waitForIdle()
        }
        fun salva(): List<Mark> {
            banco.onNodeWithText(testo(R.string.editor_save)).performClick()
            banco.waitForIdle()
            return salvato!!.drawing.marks
        }
        banco.onNodeWithText(testo(R.string.draw_raise)).assertDoesNotExist()
        tieni(Offset(-70f, -70f))
        for (voce in listOf(R.string.draw_raise, R.string.draw_copy, R.string.pick_duplicate, R.string.draw_lower, R.string.draw_rotate)) {
            banco.onNodeWithText(testo(voce)).assertExists()
        }
        banco.onNodeWithText(testo(R.string.draw_raise)).assertIsNotEnabled()
        banco.onNodeWithText(testo(R.string.draw_lower)).assertIsNotEnabled()
        vocePopup(R.string.pick_delete).assertIsEnabled()
        banco.onNodeWithText(testo(R.string.pick_duplicate)).performClick()
        banco.waitForIdle()
        val dopo = salva()
        assertEquals("Duplica doveva aggiungere un elemento", 2, dopo.size)
        assertEquals("la copia doveva essere scostata", dopo[0].moved(Draw.duplicateShift(LATO, LATO)).points, dopo[1].points)

        // ⚠️ La copia è sopra e scelta: tenerla e portarla sotto la mette prima dell'originale.
        tieni(Offset(-70f, -70f))
        banco.onNodeWithText(testo(R.string.draw_lower)).performClick()
        banco.waitForIdle()
        val scambiati = salva()
        assertEquals("Sposta sotto doveva scambiare i due", listOf(dopo[1], dopo[0]), scambiati)

        /*
         * Il tocco fuori dal menu non disegna, nemmeno con la mano libera. ⚠️ Che lo chiuda lo fa la
         * finestra del menu, e il banco non le consegna quel tocco (vedi la prova del cursore della
         * luminosità): qui si misura che il palco non cambia, poi il menu si chiude con 'Ruota'.
         */
        banco.onNodeWithContentDescription(testo(R.string.draw_free)).performClick()
        banco.waitForIdle()
        tieni(Offset(-70f, -70f))
        banco.onNodeWithText(testo(R.string.draw_copy)).assertExists()
        val aperto = palco.captureToImage().toPixelMap()
        tocca(Offset(80f, 80f))
        assertEquals("il tocco fuori dal menu non doveva disegnare", 0, differenza(aperto, palco.captureToImage().toPixelMap()))
        banco.onNodeWithText(testo(R.string.draw_rotate)).performClick()
        banco.waitForIdle()
        assertEquals(2, salva().size)

        // Copia, poi Incolla su un altro elemento: lo stile passa.
        banco.onNodeWithContentDescription(testo(R.string.draw_rect)).performClick()
        banco.onNodeWithContentDescription(testo(R.string.ink_blue)).performClick()
        banco.waitForIdle()
        trascinaDa(Offset(40f, 40f), Offset(100f, 100f))
        tieni(Offset(70f, 70f))
        banco.onNodeWithText(testo(R.string.draw_copy)).performClick()
        banco.waitForIdle()
        tieni(Offset(-70f, -70f))
        banco.onNodeWithText(testo(R.string.draw_copy)).assertDoesNotExist()
        banco.onNodeWithText(testo(R.string.draw_paste)).performClick()
        banco.waitForIdle()
        val incollati = salva()
        assertEquals("Incolla doveva dare all'elemento il colore copiato", incollati[2].ink, incollati[1].ink)

        // Tenendo Incolla la memoria si svuota, lo dice l'avviso, e torna Copia.
        tieni(Offset(-70f, -70f))
        banco.onNodeWithText(testo(R.string.draw_paste)).performTouchInput { longClick() }
        banco.waitForIdle()
        assertEquals(testo(R.string.draw_style_cleared), Notices.line?.text)
        tieni(Offset(-70f, -70f))
        banco.onNodeWithText(testo(R.string.draw_copy)).assertExists()
        banco.onNodeWithText(testo(R.string.draw_paste)).assertDoesNotExist()
    }

    /**
     * **Le maniglie ridimensionano l'elemento scelto, e in `Ruota` lo girano** (`4.70`, sua risposta
     * su `4.64-04`: *le maniglie di base permetteranno di ridimensionare gli oggetti. Invece,
     * toccando `Ruota` si passa in modalità rotazione*). In `Ruota` il tasto diventa `Trasforma`, e
     * le maniglie si vedono vuote.
     * ⚠️⚠️ **CONTROPROVATA** due volte: senza il ramo delle maniglie nel palco (il trascinamento
     * dall'angolo sposta l'elemento invece di tirarlo), e con `Ruota` che non cambia modalità (le
     * maniglie restano piene).
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `le maniglie ridimensionano e in Ruota girano`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        trascinaDa(Offset(-100f, -100f), Offset(-40f, -40f))
        fun salva(): List<Mark> {
            banco.onNodeWithText(testo(R.string.editor_save)).performClick()
            banco.waitForIdle()
            return salvato!!.drawing.marks
        }
        val prima = salva().single()
        tocca(Offset(-70f, -70f))
        trascinaDa(Offset(-40f, -40f), Offset(-10f, -10f))
        val tirato = salva()
        assertEquals("il trascinamento da un angolo non doveva disegnare", 1, tirato.size)
        assertEquals("l'angolo opposto doveva restare, x", prima.points.first().x, tirato[0].points.first().x, 1e-4f)
        assertEquals("l'angolo opposto doveva restare, y", prima.points.first().y, tirato[0].points.first().y, 1e-4f)
        assertTrue("l'angolo tirato doveva allontanarsi", tirato[0].points.last().x > prima.points.last().x + 0.02f)

        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        val pieno = palco.captureToImage().toPixelMap()
        palco.performTouchInput { longClick(center + Offset(-55f, -55f)) }
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.draw_rotate)).performClick()
        banco.waitForIdle()
        assertTrue("in Ruota le maniglie dovevano cambiare aspetto", differenza(pieno, palco.captureToImage().toPixelMap()) > 20)
        // ⚠️ Dall'angolo in basso a destra, a 45 gradi dal centro, a dritto sotto il centro: 45 gradi.
        trascinaDa(Offset(-10f, -10f), Offset(-55f, -55f + 45f * 1.4142f))
        val girato = salva().single()
        assertEquals("il trascinamento in Ruota doveva girare l'elemento di 45 gradi", 45f, girato.angle, 1e-3f)
        assertEquals("girando i punti restano", tirato[0].points, girato.points)
        palco.performTouchInput { longClick(center + Offset(-55f, -55f)) }
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.draw_transform)).assertExists()
    }

    /**
     * **Un tocco sull'elemento scelto alterna `Trasforma` e `Ruota`** (`4.81`, sua nota su
     * `4.70-04`: *un tap singolo su un oggetto già selezionato lo fa passare ciclicamente da
     * trasformazione e rotazione*). Il primo tocco sceglie l'elemento; il secondo lo mette in
     * `Ruota`, con le maniglie vuote, e un trascinamento da un angolo lo gira; il terzo lo riporta
     * in `Trasforma`, e il menu offre di nuovo `Ruota`.
     * ⚠️⚠️ **CONTROPROVATA**: senza il ramo del tocco sull'elemento scelto nel palco, il secondo
     * tocco lascia le maniglie piene.
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `un tocco sull'elemento scelto alterna Trasforma e Ruota`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        trascinaDa(Offset(-100f, -100f), Offset(-40f, -40f))
        tocca(Offset(-70f, -70f))
        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        val pieno = palco.captureToImage().toPixelMap()
        tocca(Offset(-70f, -70f))
        assertTrue("il secondo tocco doveva mettere l'elemento in Ruota, con le maniglie vuote",
            differenza(pieno, palco.captureToImage().toPixelMap()) > 20)
        // ⚠️ Dall'angolo in basso a destra, a 45 gradi dal centro, a dritto sotto il centro: 45 gradi.
        trascinaDa(Offset(-40f, -40f), Offset(-70f, -70f + 30f * 1.4142f))
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        assertEquals("in Ruota il trascinamento da un angolo doveva girare l'elemento", 45f,
            salvato!!.drawing.marks.single().angle, 1e-3f)
        tocca(Offset(-70f, -70f))
        palco.performTouchInput { longClick(center + Offset(-70f, -70f)) }
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.draw_rotate)).assertExists()
    }

    /**
     * **Il conto del Pannello** (`4.91`, il suo `Non approvato` su `4.81-01`: *l'area sfocata non
     * sarà più attributo di ogni forma, bensì uno strumento a parte. Si chiamerà 'Pannello'*):
     * l'area dentro il pannello diventa grigia su un'immagine a righe e fuori resta com'è; senza
     * colore il pannello non dipinge niente sopra il vetro, col colore lo stende al 20%; il pezzo
     * letto a piena risoluzione sfoca come l'immagine intera; un tocco prende l'elemento più in alto
     * e il pannello anche dove non ha colore; lo stile porta colore e sfocatura fra due pannelli e
     * non a un rettangolo; un rettangolo con una sfocatura, come quelli della `4.80`, non sfoca più.
     * ⚠️⚠️ **CONTROPROVATA** due volte: senza il pannello fra gli elementi che stendono il vetro
     * (`Mark.glass`: il centro resta nero o bianco), e col rettangolo ancora fra quelli (l'immagine
     * sotto il rettangolo si sfoca).
     */
    /**
     * **I colori della pillola tengono leggibili le parole bianche** (`4.91`, sua nota su `4.90-05`:
     * *tutti colori 'stravaganti', neon e ben visibili*): otto, il suo rosso per primo, ognuno con un
     * contrasto di almeno 3 contro il bianco delle parole.
     * ⚠️⚠️ **CONTROPROVATA**: con un giallo neon (`#FFFF00`) fra i colori la prova cade.
     */
    @Test
    fun `i colori della pillola tengono leggibili le parole`() {
        assertEquals(8, Draw.PILL_INKS.size)
        assertEquals("il primo è il suo rosso", Draw.PILL_FILL or 0xFF000000.toInt(), Draw.PILL_INKS.first())
        for (ink in Draw.PILL_INKS) {
            val contrasto = androidx.core.graphics.ColorUtils.calculateContrast(Draw.PILL_WORDS, ink)
            assertTrue("sul colore ${Integer.toHexString(ink)} le parole bianche dovevano leggersi ($contrasto)", contrasto >= Draw.WORDS_CONTRAST)
        }
        assertEquals("i colori sono tutti diversi", 8, Draw.PILL_INKS.toSet().size)
    }

    @Test
    fun `il conto del pannello`() {
        val w = 200
        val h = 100
        fun righe(): Bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until w) for (y in 0 until h) setPixel(x, y, if ((x / 2) % 2 == 0) Color.BLACK else Color.WHITE)
        }
        val pannello = Mark(Pen.PANEL, listOf(Offset(0.25f, 0.25f), Offset(0.75f, 0.75f)), Draw.PILL_WORDS, 0f, false, null,
            blur = 0.1f, words = Words(""))
        val dentro = Draw.blurAreas(righe(), Drawing(listOf(pannello)), mine = true)
        fun grigio(c: Int) = Color.red(c) in 80..175
        assertTrue("al centro del pannello l'immagine doveva diventare grigia", grigio(dentro.getPixel(100, 50)))
        assertTrue("e accanto anche", grigio(dentro.getPixel(101, 50)))
        assertEquals("fuori dal pannello l'immagine doveva restare com'era", righe().getPixel(10, 10), dentro.getPixel(10, 10))
        val nudo = Draw.overlay(Drawing(listOf(pannello)), w, h, Spin(0, false))!!
        assertEquals("senza colore il pannello non dipinge niente", 0, nudo.getPixel(100, 50))
        val rosso = pannello.copy(fill = Draw.panelFill(Draw.PILL_INKS.first()))
        val tinto = Draw.overlay(Drawing(listOf(rosso)), w, h, Spin(0, false))!!
        assertEquals("col colore lo stende al 20%", 0x33.toFloat(), Color.alpha(tinto.getPixel(100, 50)).toFloat(), 1f)
        val niente = righe()
        // ⚠️ `mine = false`: senza elementi da sfocare torna la stessa immagine, con uno la copia.
        assertSame("senza sfocatura l'immagine resta la stessa", niente,
            Draw.blurAreas(niente, Drawing(listOf(pannello.copy(blur = null))), mine = false))
        val rettangolo = Mark(Pen.RECT, listOf(Offset(0.25f, 0.25f), Offset(0.75f, 0.75f)), Color.RED, 0.01f, false, null, blur = 0.1f)
        assertSame("un rettangolo non sfoca più", niente, Draw.blurAreas(niente, Drawing(listOf(rettangolo)), mine = false))

        // ⚠️ Il pezzo del centro letto a parte, come fa il palco ingrandito: lo stesso grigio.
        val pezzo = android.graphics.RectF(0.4f, 0.3f, 0.6f, 0.7f)
        val ritaglio = Bitmap.createBitmap(righe(), 80, 30, 40, 40)
        val pezzoSfocato = Draw.blurAreas(ritaglio.copy(Bitmap.Config.ARGB_8888, true), Drawing(listOf(pannello)), mine = true, at = pezzo)
        assertEquals("il pezzo doveva sfocarsi come l'immagine intera",
            Color.red(dentro.getPixel(100, 50)).toFloat(), Color.red(pezzoSfocato.getPixel(20, 20)).toFloat(), 30f)

        val linea = Mark(Pen.LINE, listOf(Offset(0.2f, 0.5f), Offset(0.8f, 0.5f)), Color.RED, 0.01f, false, null)
        val pieno = Mark(Pen.RECT, listOf(Offset(0.4f, 0.4f), Offset(0.6f, 0.6f)), Color.RED, 0.01f, false, Color.WHITE)
        assertEquals("un tocco prende l'elemento più in alto", 1,
            Draw.hit(Drawing(listOf(pannello, pieno)), Offset(0.5f, 0.5f), w, h, 0.01f))
        assertEquals("anche quando è il pannello", 1,
            Draw.hit(Drawing(listOf(pieno, pannello)), Offset(0.5f, 0.5f), w, h, 0.01f))
        assertEquals("e il pannello si prende dentro, anche senza colore", 1,
            Draw.hit(Drawing(listOf(linea, pannello)), Offset(0.3f, 0.3f), w, h, 0.01f))
        val altro = pannello.copy(blur = 0.2f, fill = Draw.panelFill(Draw.PILL_INKS[3]))
        assertEquals("lo stile porta la sfocatura fra due pannelli", 0.2f, pannello.styledLike(altro).blur)
        assertEquals("e il colore", Draw.panelFill(Draw.PILL_INKS[3]), pannello.styledLike(altro).fill)
        assertEquals("e non a un rettangolo", null, pieno.styledLike(altro).blur)
    }

    /**
     * **L'area sfocata ha la forma del pannello, pixel per pixel** (`4.81` per il rettangolo, il suo
     * `Non approvato` su `4.80-01`, con un parallelogramma inclinato e due angoli stondati staccati;
     * `4.91` per il pannello). Su un'immagine a righe ogni pixel ben dentro il pannello è grigio e
     * ogni pixel ben fuori resta com'era: diritto e girato di 30 gradi.
     * ⚠️ Gli angoli del pannello sono stondati dello 0,3% del suo lato lungo, meno di mezzo pixel qui:
     * la prova guarda un rettangolo, e la sua rotazione.
     * ⚠️⚠️ **CONTROPROVATA**: col contorno del pannello non girato in `Draw.outline` i pixel fuori
     * posto sono centinaia.
     */
    @Test
    fun `l'area sfocata ha la forma del pannello`() {
        val lato = 200
        fun righe(): Bitmap = Bitmap.createBitmap(lato, lato, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until lato) for (y in 0 until lato) setPixel(x, y, if ((x / 2) % 2 == 0) Color.BLACK else Color.WHITE)
        }
        val originale = righe()
        val diritto = Mark(Pen.PANEL, listOf(Offset(0.2f, 0.2f), Offset(0.8f, 0.8f)), Draw.PILL_WORDS, 0f, false, null,
            blur = 0.1f, words = Words(""))
        val metà = 60f
        fun quadro(lx: Float, ly: Float): Float = kotlin.math.max(kotlin.math.abs(lx), kotlin.math.abs(ly)) - metà
        for ((nome, elemento) in listOf("il pannello diritto" to diritto, "il pannello girato di 30 gradi" to diritto.copy(angle = 30f))) {
            val sfocata = Draw.blurAreas(righe(), Drawing(listOf(elemento)), mine = true)
            val rad = Math.toRadians(elemento.angle.toDouble())
            val cs = kotlin.math.cos(rad).toFloat()
            val sn = kotlin.math.sin(rad).toFloat()
            var fuoriPosto = 0
            for (y in 0 until lato) for (x in 0 until lato) {
                val dx = x + 0.5f - 100f
                val dy = y + 0.5f - 100f
                val d = quadro(dx * cs + dy * sn, -dx * sn + dy * cs)
                val c = sfocata.getPixel(x, y)
                if (d < -3f && Color.red(c) !in 80..175) fuoriPosto++
                if (d > 3f && c != originale.getPixel(x, y)) fuoriPosto++
            }
            assertEquals("$nome: ogni pixel dentro doveva essere sfocato e ogni pixel fuori com'era", 0, fuoriPosto)
        }
    }

    /**
     * **Il Pannello sfoca l'area sul palco, il cursore ne regola l'entità e i tondi il colore**
     * (`4.91`): disegnato, il pannello chiede le parole e nasce senza colore e con la sfocatura di
     * fabbrica; scelto, il cursore e i tondi cambiano lui. Nella fila dei tasti ci sono quelli del
     * testo, con `Sfondo` spento.
     * ⚠️⚠️ **CONTROPROVATA** due volte: senza il vetro dei pannelli nell'anteprima del palco (l'area
     * resta a righe), e senza il colore del pannello fra i valori che il modulo posa sull'elemento
     * scelto (il tondo non lo cambia).
     */
    @Test
    @Config(qualifiers = "w320dp-h891dp")
    fun `il Pannello sfoca l'area sul palco`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }, uri = righe()) }
        pronta()
        apriDisegno()
        fun salva(): List<Mark> {
            banco.onNodeWithText(testo(R.string.editor_save)).performClick()
            banco.waitForIdle()
            return salvato!!.drawing.marks
        }
        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        banco.onNodeWithContentDescription(testo(R.string.draw_panel)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(testo(R.string.draw_ground)).assertIsNotEnabled()
        banco.onNodeWithContentDescription(testo(R.string.settings_colour_none)).assertIsSelected()
        val prima = palco.captureToImage().toPixelMap()
        trascinaDa(Offset(-100f, -100f), Offset(-20f, -20f))
        banco.onNodeWithText(testo(R.string.cancel)).performClick()
        banco.waitForIdle()
        val dopo = palco.captureToImage().toPixelMap()
        val nato = salva().single()
        assertEquals(Pen.PANEL, nato.pen)
        assertEquals("il pannello doveva nascere con la sfocatura di fabbrica", Draw.BLUR, nato.blur)
        assertEquals("e senza colore", null, nato.fill)
        assertTrue("dentro il pannello le righe dovevano diventare grigie",
            grigi(dopo, Offset(-60f, -60f), 30) > grigi(prima, Offset(-60f, -60f), 30) + 200)
        banco.onAllNodesWithContentDescription(testo(R.string.draw_blur))
            .filterToOne(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(Draw.BLUR_MAX) }
        banco.waitForIdle()
        assertEquals("il cursore doveva regolare la sfocatura del pannello scelto", Draw.BLUR_MAX, salva().single().blur)
        banco.onNodeWithContentDescription("${testo(R.string.draw_ground)} 3").performClick()
        banco.waitForIdle()
        assertEquals("il tondo doveva dare il suo colore al 20%", Draw.panelFill(Draw.PILL_INKS[2]), salva().single().fill)
        banco.onNodeWithContentDescription(testo(R.string.settings_colour_none)).performClick()
        banco.waitForIdle()
        assertEquals("'Nessuno' doveva togliere il colore", null, salva().single().fill)
    }

    /**
     * **`Elimina` torna nella quinta colonna** (`4.91`, sua nota C sul giro della `4.90`: *vorrei
     * riavere il tasto 'Elimina', era molto comodo*): spento finché nessun elemento è scelto, toglie
     * quello scelto. Dalla `4.92` è un tasto di testo accanto a `Elimina tutto`, per ogni strumento
     * (suo `Non approvato` su `4.91-04`), e la sua prova con la pillola vive in
     * `pillola e pannello si applicano senza parole ed Elimina vale per loro`.
     * ⚠️⚠️ **CONTROPROVATA**: col tasto che non toglie l'elemento, l'elemento resta.
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `Elimina toglie l'elemento scelto`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        val elimina = banco.onNodeWithText(testo(R.string.pick_delete))
        elimina.assertIsNotEnabled()
        val r = immagine()
        fun a(x: Float, y: Float) = Offset(r.left + x * r.width, r.top + y * r.height)
        trascinaDa(a(0.1f, 0.1f), a(0.35f, 0.35f))
        trascinaDa(a(0.6f, 0.6f), a(0.9f, 0.9f))
        tocca(a(0.75f, 0.6f))
        elimina.assertIsEnabled().performClick()
        banco.waitForIdle()
        elimina.assertIsNotEnabled()
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        val resta = salvato!!.drawing.marks.single()
        assertEquals("Elimina doveva togliere il rettangolo scelto e lasciare l'altro", 0.1f, resta.points.first().x, 0.05f)
    }

    /**
     * **Il conto del Testo** (`4.90`, G3): il riquadro nasce dalle parole misurate, una riga in più lo
     * allunga di un passo e il fondo lo allarga del suo margine; il grassetto ha il suo peso; le
     * maniglie sono gli angoli e i mezzi dei lati; una maniglia tirata cambia il corpo intorno al
     * centro, entro i limiti del cursore; il giro tiene il punto; il tocco prende il testo dentro il
     * riquadro, anche girato; lo stile passa intero fra due testi, parole escluse, e fra un testo e
     * una forma passa solo il colore, ricetta compresa; le parole bianche o nere prendono sul fondo
     * quella delle due che si legge meglio; un fondo si offre se stacca dall'immagine e tiene
     * leggibili le parole.
     * ⚠️⚠️ **CONTROPROVATA** quattro volte: con la scala della maniglia senza il limite (il corpo
     * oltre il tetto), con la riga della ricetta tolta in `Mark.styledLike` (il rettangolo tiene il
     * colore vecchio nella ricetta), con `Draw.wordsOn` che tiene le parole neutre (le bianche restano
     * bianche sul giallo), e con il contrasto di luminanza al posto della distanza dei colori in
     * `Draw.readable` (un fondo giallo non si offre su una pagina bianca).
     */
    /**
     * **Il testo arriva più grande dell'immagine, e il cursore resta preciso sui corpi piccoli**
     * (`4.91`, sua nota su `4.90-02`: *voglio poter fare un testo grande come l'intera immagine e
     * anche oltre*): il corpo arriva a 1,5 volte il lato lungo, e il cursore moltiplica il corpo
     * per lo stesso fattore a ogni tratto, quindi il corpo di fabbrica non finisce schiacciato in
     * fondo alla pista.
     * ⚠️⚠️ **CONTROPROVATA** due volte: col tetto vecchio di 0,2, e con la pista lineare, dove il
     * corpo di fabbrica cade a meno di un trentesimo della pista.
     */
    @Test
    fun `il corpo del testo supera l'immagine e il cursore resta preciso`() {
        assertTrue("una lettera doveva poter essere più alta dell'immagine", Draw.TEXT_MAX >= 1.1f)
        assertEquals("in fondo alla pista il corpo più piccolo", Draw.TEXT_MIN, Draw.textSize(0f), 1e-6f)
        assertEquals("in cima il più grande", Draw.TEXT_MAX, Draw.textSize(1f), 1e-5f)
        for (corpo in listOf(0.01f, 0.03f, 0.05f, 0.2f, 1f, 1.5f)) {
            assertEquals("il corpo $corpo doveva tornare dalla pista", corpo, Draw.textSize(Draw.textTrack(corpo)), corpo * 1e-4f)
        }
        assertTrue("il corpo di fabbrica doveva essere oltre un quarto della pista", Draw.textTrack(Draw.TEXT) > 0.25f)
        val w = 400
        val h = 200
        val testo = Mark(Pen.TEXT, listOf(Offset(0.5f, 0.5f)), Color.WHITE, 0.05f, false, null, words = Words("A"))
        assertEquals("tirando un angolo il corpo arriva al tetto", Draw.TEXT_MAX, testo.reshaped(2, Offset(99f, 99f), w, h).width, 1e-4f)
    }

    @Test
    fun `il conto del testo`() {
        Faces.load(app)
        val w = 400
        val h = 200
        val corpo = 0.05f * w
        val testo = Mark(Pen.TEXT, listOf(Offset(0.5f, 0.5f)), Color.WHITE, 0.05f, false, null, words = Words("Ciao mondo"))
        val (hw, hh) = Draw.textHalf(testo, w.toFloat(), h.toFloat())
        assertEquals("una riga è alta un passo, 1,3 volte il corpo", corpo * 1.3f / 2f, hh, 1e-3f)
        assertTrue("la riga doveva avere la sua larghezza", hw > corpo)
        val due = Draw.textHalf(testo.copy(words = Words("Ciao mondo\nCiao")), w.toFloat(), h.toFloat())
        assertEquals("due righe sono alte due passi", 2f * hh, due.second, 1e-3f)
        assertEquals("e larghe come la più larga", hw, due.first, 1e-3f)
        val etichetta = Draw.textHalf(testo.copy(words = Words("Ciao mondo", label = true)), w.toFloat(), h.toFloat())
        assertEquals("il fondo aggiunge il suo margine ai due capi", hw + 0.35f * corpo, etichetta.first, 1e-3f)
        // ⚠️ Il banco non disegna le variazioni di un carattere (misurato: il grassetto vi risulta largo
        // quanto il normale), quindi qui si guarda il peso che il carattere dichiara.
        assertEquals("il grassetto doveva avere il suo peso nel carattere", Faces.BOLD, Faces.of(Face.ROBOTO, false, true).weight)
        assertEquals("e il normale il suo", Faces.REGULAR, Faces.of(Face.LITERATA, true, false).weight)

        val maniglie = testo.handles(w, h)
        assertEquals("quattro angoli e quattro lati", 8, maniglie.size)
        assertEquals(0.5f - hw / w, maniglie[0].x, 1e-4f)
        assertEquals(0.5f - hh / h, maniglie[0].y, 1e-4f)
        val doppio = testo.reshaped(2, Offset(0.5f + 2f * hw / w, 0.5f + 2f * hh / h), w, h)
        assertEquals("l'angolo tirato al doppio della distanza doveva raddoppiare il corpo", 0.1f, doppio.width, 1e-4f)
        assertEquals("il centro doveva restare", testo.points, doppio.points)
        assertEquals("oltre il tetto il corpo si ferma", Draw.TEXT_MAX, testo.reshaped(2, Offset(9f, 9f), w, h).width, 1e-4f)
        assertEquals("verso il centro il corpo si ferma al minimo", Draw.TEXT_MIN, testo.reshaped(2, Offset(0.5f, 0.5f), w, h).width, 1e-4f)

        val girato = testo.turned(88f, w, h)
        assertEquals("il testo si aggancia a 90 gradi", 90f, girato.angle, 1e-4f)
        assertEquals("girando il punto resta", testo.points, girato.points)
        val sotto = Offset(0.5f, 0.5f + (hw - 4f) / h)
        assertEquals("sotto il centro il testo girato si prende", 0, Draw.hit(Drawing(listOf(girato)), sotto, w, h, 0.01f))
        assertEquals("e quello dritto no", null, Draw.hit(Drawing(listOf(testo)), sotto, w, h, 0.01f))
        assertEquals("accanto al centro il testo dritto si prende", 0,
            Draw.hit(Drawing(listOf(testo)), Offset(0.5f + (hw - 4f) / w, 0.5f), w, h, 0.01f))
        val contorno = Draw.outline(girato, w, h)
        assertEquals("girato, il riquadro è largo quanto la riga è alta", 2f * hh / w, contorno.maxOf { it.x } - contorno.minOf { it.x }, 1e-3f)

        val altro = Mark(Pen.TEXT, listOf(Offset(0.1f, 0.1f)), Color.YELLOW, 0.08f, false, null,
            words = Words("Altro", Face.LITERATA, bold = true, label = true, ground = Draw.LABEL_INKS[1]))
        val stilato = testo.styledLike(altro)
        assertEquals("le parole restano sue", "Ciao mondo", stilato.words?.text)
        assertEquals(Words("Ciao mondo", Face.LITERATA, bold = true, label = true, ground = Draw.LABEL_INKS[1]), stilato.words)
        assertEquals(Color.YELLOW, stilato.ink)
        assertEquals(0.08f, stilato.width)
        assertEquals("il punto resta suo", testo.points, stilato.points)
        val ricetta = Tint(Color.WHITE, 0f, 1f, null, 0f, 1f)
        val rett = Mark(Pen.RECT, listOf(Offset(0.2f, 0.2f), Offset(0.4f, 0.4f)), Color.BLUE, 0.01f, false, Color.GREEN,
            tint = Tint(Color.BLUE, 0f, 1f, Color.GREEN, 0f, 1f))
        val blu = testo.copy(tint = ricetta).styledLike(rett)
        assertEquals("da una forma il testo prende il colore", Color.BLUE, blu.ink)
        assertEquals("e la ricetta del colore", Color.BLUE, blu.tint?.ink)
        assertEquals("non lo spessore", 0.05f, blu.width)
        val bianco = rett.styledLike(testo.copy(tint = ricetta))
        assertEquals("da un testo la forma prende il colore", Color.WHITE, bianco.ink)
        assertEquals("e la ricetta del colore", Color.WHITE, bianco.tint?.ink)
        assertEquals("e tiene il riempimento", Color.GREEN, bianco.fill)
        assertEquals("e lo spessore", 0.01f, bianco.width)

        val giallo = 0xFFFFE15A.toInt()
        assertEquals("sul giallo le parole bianche diventano nere", Color.BLACK, Draw.wordsOn(giallo, Color.WHITE))
        assertEquals("sulla striscia viola le nere diventano bianche", Color.WHITE, Draw.wordsOn(Draw.LABEL_INK, Color.BLACK))
        assertEquals("un colore che si legge resta", 0xFF1F5FA8.toInt(), Draw.wordsOn(giallo, 0xFF1F5FA8.toInt()))
        assertEquals("un colore che non si legge diventa nero o bianco", Color.BLACK, Draw.wordsOn(giallo, 0xFFFFF0A0.toInt()))
        assertTrue("un fondo giallo stacca da una pagina bianca", Draw.readable(giallo, Color.WHITE, Color.BLACK))
        assertFalse("un fondo bianco non stacca da una pagina bianca", Draw.readable(Color.WHITE, Color.WHITE, Color.BLACK))
        assertFalse("le parole bianche non si leggono sul giallo", Draw.readable(giallo, Color.BLACK, Color.WHITE))
        assertTrue("e sulla striscia viola sì", Draw.readable(Draw.LABEL_INK, Color.WHITE, Color.WHITE))
    }

    /**
     * **Il testo va a capo dalla sua larghezza, e le righe si allineano** (`4.90`, sue `B2` e `B3`:
     * *Aggiungo volentieri B2 e B3 sul testo*): la maniglia a metà del lato destro stringe il testo
     * col lato sinistro fermo, e le parole vanno a capo; una maniglia d'angolo ingrandisce anche la
     * larghezza, quindi le righe restano quelle; sinistra e destra mettono la riga corta contro il
     * suo lato del riquadro; lo stile copiato da un altro testo lascia a ognuno la sua larghezza.
     * ⚠️⚠️ **CONTROPROVATA** tre volte: con le righe spezzate solo dove c'è Invio (il testo stretto
     * non si allunga), con l'allineamento ignorato nel disegno (la riga corta resta al centro), e con
     * la scala d'angolo che lascia ferma la larghezza (le righe cambiano).
     */
    @Test
    fun `il testo va a capo e si allinea`() {
        Faces.load(app)
        val lato = 400
        val f = lato.toFloat()
        val testo = Mark(Pen.TEXT, listOf(Offset(0.5f, 0.5f)), Color.WHITE, 0.05f, false, null,
            words = Words("Ciao mondo, come stai oggi"))
        val (hw, hh) = Draw.textHalf(testo, f, f)
        val stretto = testo.reshaped(5, Offset(0.5f, 0.5f), lato, lato)
        val (shw, shh) = Draw.textHalf(stretto, f, f)
        assertTrue("stretto, il testo doveva andare a capo", shh > 1.5f * hh)
        assertEquals("largo quanto la maniglia tirata", hw / 2f, shw, 0.5f)
        assertEquals("col lato sinistro fermo", 0.5f * f - hw, stretto.points.single().x * f - shw, 0.5f)
        val doppio = stretto.reshaped(2, Offset(stretto.points.single().x + 2f * shw / f, 0.5f + 2f * shh / f), lato, lato)
        assertEquals("l'angolo doveva ingrandire anche la larghezza", 2f * stretto.words!!.wrap, doppio.words!!.wrap, 1e-3f)
        assertEquals("e le righe restare quelle", 2f * shh, Draw.textHalf(doppio, f, f).second, 1f)

        fun righe(align: Align): Pair<IntRange, IntRange> {
            val segno = Mark(Pen.TEXT, listOf(Offset(0.5f, 0.5f)), Color.WHITE, 0.08f, false, null,
                words = Words("I\nmondo intero", align = align))
            val tela = Draw.overlay(Drawing(listOf(segno)), lato, lato, Spin(0, false))!!
            var c0 = lato; var c1 = -1; var t0 = lato; var t1 = -1
            for (y in 0 until lato) for (x in 0 until lato) {
                if (Color.alpha(tela.getPixel(x, y)) < 200) continue
                if (y < lato / 2) { c0 = minOf(c0, x); c1 = maxOf(c1, x) }
                t0 = minOf(t0, x); t1 = maxOf(t1, x)
            }
            return c0..c1 to t0..t1
        }
        val (corta, tutto) = righe(Align.LEFT)
        assertEquals("a sinistra la riga corta doveva toccare il lato sinistro", tutto.first.toFloat(), corta.first.toFloat(), 4f)
        val (cortaD, tuttoD) = righe(Align.RIGHT)
        assertEquals("a destra la riga corta doveva toccare il lato destro", tuttoD.last.toFloat(), cortaD.last.toFloat(), 4f)
        val (cortaC, _) = righe(Align.CENTER)
        assertEquals("al centro la riga corta doveva stare in mezzo", lato / 2f, (cortaC.first + cortaC.last) / 2f, 4f)

        val altro = testo.copy(words = Words("Altro", Face.MONTSERRAT, align = Align.RIGHT, wrap = 0.3f))
        val stilato = stretto.styledLike(altro)
        assertEquals("lo stile doveva passare l'allineamento", Align.RIGHT, stilato.words?.align)
        assertEquals("e lasciare la larghezza", stretto.words?.wrap, stilato.words?.wrap)
    }

    /**
     * **Le righe si centrano sulla H, in tutti e quattro i caratteri** (`4.90`, sua nota: *Literata
     * ha una baseline stranamente bassa: credo sia l'unico font per il quale sarà necessario
     * aggiustare la centratura verticale dell'etichetta*): su una striscia di `Etichetta` la H
     * bianca ha lo stesso centro della striscia, entro un pixel e mezzo su un corpo di 80. Le
     * larghezze diverse della H dicono che i quattro file sono caricati davvero.
     * ⚠️⚠️ **CONTROPROVATA**: con le righe centrate sul riquadro del carattere (la metà fra
     * `ascent` e `descent`) cade Literata, e solo lei.
     */
    @Test
    fun `l'etichetta tiene la H al centro in tutti e quattro i caratteri`() {
        Faces.load(app)
        val lato = 400
        val larghezze = mutableSetOf<Int>()
        for (face in Face.entries) {
            val segno = Mark(Pen.TEXT, listOf(Offset(0.5f, 0.5f)), Color.WHITE, 0.2f, false, null,
                words = Words("H", face, label = true))
            val tela = Draw.overlay(Drawing(listOf(segno)), lato, lato, Spin(0, false))!!
            var a0 = lato; var a1 = -1; var x0 = lato; var x1 = -1; var s0 = lato; var s1 = -1
            for (y in 0 until lato) for (x in 0 until lato) {
                val c = tela.getPixel(x, y)
                if (Color.alpha(c) > 250 && Color.red(c) > 245 && Color.green(c) > 245 && Color.blue(c) > 245) {
                    a0 = minOf(a0, y); a1 = maxOf(a1, y); x0 = minOf(x0, x); x1 = maxOf(x1, x)
                }
                if (Color.alpha(c) > 250 && kotlin.math.abs(Color.red(c) - 0xA3) < 3 && kotlin.math.abs(Color.green(c) - 0x40) < 3 &&
                    kotlin.math.abs(Color.blue(c) - 0x8F) < 3) {
                    s0 = minOf(s0, y); s1 = maxOf(s1, y)
                }
            }
            assertTrue("${face.label}: la H doveva esserci", a1 > a0)
            assertTrue("${face.label}: la striscia doveva esserci", s1 > s0)
            assertEquals("${face.label}: la H doveva essere al centro della striscia", (s0 + s1 + 1) / 2f, (a0 + a1 + 1) / 2f, 1.5f)
            larghezze += x1 - x0
        }
        assertTrue("i quattro caratteri dovevano essere caricati davvero, con H di larghezze diverse: $larghezze", larghezze.size >= 3)
    }

    /**
     * **Il conto della Pillola** (`4.90`, sua nota A sul giro della `4.43`): il riquadro è quello
     * disegnato; le parole prendono la misura più grande che ci sta, quindi in un riquadro più alto
     * sono più grandi; parole che non ci stanno nemmeno al minimo allungano il riquadro verso il
     * basso, col lato di sopra fermo; la traccia scura è sul bordo, quella chiara dentro, il
     * riempimento più dentro ancora, coi suoi colori; sotto la pillola l'immagine si sfoca, come un
     * vetro; il tocco la prende dentro; fra un testo e una pillola passano il carattere e i tre
     * stili, mai il colore; la pillola posata da un tocco contiene le parole senza allungarsi.
     * ⚠️⚠️ **CONTROPROVATA** tre volte: con le parole a misura fissa (la pillola alta ha le parole
     * alte come quella bassa), senza la crescita del riquadro (il lato di sotto resta dov'era e le
     * parole escono), e senza la pillola fra gli elementi di `Draw.blurAreas` (sotto la pillola le
     * righe restano nette).
     */
    @Test
    fun `il conto della pillola`() {
        Faces.load(app)
        val lato = 1000
        fun pillola(dall: Offset, al: Offset, parole: String) =
            Mark(Pen.PILL, listOf(dall, al), Draw.PILL_WORDS, 0f, false, null, words = Words(parole))
        val bassa = pillola(Offset(0.2f, 0.45f), Offset(0.8f, 0.55f), "Ciao")
        val (c, hw, hh) = Draw.textFrame(bassa, lato.toFloat(), lato.toFloat())
        assertEquals("il riquadro è quello disegnato, x", 500f, c.x, 1e-3f)
        assertEquals("il riquadro è quello disegnato, y", 500f, c.y, 1e-3f)
        assertEquals(300f, hw, 1e-3f)
        assertEquals(50f, hh, 1e-3f)
        assertEquals("otto maniglie, come un rettangolo", 8, bassa.handles(lato, lato).size)
        assertEquals(0.2f, bassa.handles(lato, lato)[0].x, 1e-4f)

        fun altezzaParole(segno: Mark): Int {
            val tela = Draw.overlay(Drawing(listOf(segno)), lato, lato, Spin(0, false))!!
            var y0 = lato; var y1 = -1
            for (y in 0 until lato) for (x in 0 until lato) {
                val p = tela.getPixel(x, y)
                if (Color.alpha(p) > 250 && Color.red(p) > 245 && Color.green(p) > 245 && Color.blue(p) > 245) {
                    y0 = minOf(y0, y); y1 = maxOf(y1, y)
                }
            }
            return y1 - y0 + 1
        }
        val alta = pillola(Offset(0.2f, 0.4f), Offset(0.8f, 0.6f), "Ciao")
        assertTrue("in un riquadro più alto le parole dovevano essere più grandi",
            altezzaParole(alta) > altezzaParole(bassa) * 3 / 2)

        val lunga = pillola(Offset(0.4f, 0.48f), Offset(0.6f, 0.52f), "uno due tre quattro cinque sei sette otto nove dieci")
        val (cl, _, hl) = Draw.textFrame(lunga, lato.toFloat(), lato.toFloat())
        assertTrue("le parole che non ci stanno dovevano allungare il riquadro", hl > 20f + 1f)
        assertEquals("col lato di sopra fermo", 480f, cl.y - hl, 1e-2f)
        val contorno = Draw.outline(lunga, lato, lato)
        assertEquals("il contorno segue il riquadro allungato", 2f * hl / lato, contorno.maxOf { it.y } - contorno.minOf { it.y }, 1e-3f)

        val tela = Draw.overlay(Drawing(listOf(bassa)), lato, lato, Spin(0, false))!!
        fun vicino(p: Int, atteso: Int) = kotlin.math.abs(Color.alpha(p) - Color.alpha(atteso)) <= 3 &&
            kotlin.math.abs(Color.red(p) - Color.red(atteso)) <= 3 && kotlin.math.abs(Color.green(p) - Color.green(atteso)) <= 3 &&
            kotlin.math.abs(Color.blue(p) - Color.blue(atteso)) <= 3
        // La traccia è lo 0,5% del lato maggiore, 600: 3 pixel per traccia, dal lato di sopra a y 450.
        assertTrue("sul bordo la traccia scura", vicino(tela.getPixel(500, 451), Draw.PILL_DARK))
        assertTrue("dentro la traccia chiara", vicino(tela.getPixel(500, 454), Draw.PILL_LIGHT))
        assertTrue("più dentro il riempimento", vicino(tela.getPixel(500, 460), Draw.PILL_FILL))
        assertEquals("fuori dalla pillola niente", 0, tela.getPixel(500, 445))

        val righe = Bitmap.createBitmap(lato, lato, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until lato) for (y in 0 until lato) setPixel(x, y, if ((x / 2) % 2 == 0) Color.BLACK else Color.WHITE)
        }
        val vetro = Draw.blurAreas(righe, Drawing(listOf(bassa)), mine = true)
        assertTrue("sotto la pillola l'immagine doveva sfocarsi", Color.red(vetro.getPixel(300, 500)) in 80..175)
        assertTrue("fuori no", Color.red(vetro.getPixel(300, 300)).let { it == 0 || it == 255 })

        assertEquals("la pillola si prende dentro", 0, Draw.hit(Drawing(listOf(bassa)), Offset(0.3f, 0.5f), lato, lato, 0.01f))
        val testo = Mark(Pen.TEXT, listOf(Offset(0.1f, 0.1f)), Color.RED, 0.05f, false, null,
            words = Words("Altro", Face.LITERATA, bold = true, label = true))
        val presa = bassa.styledLike(testo)
        assertEquals("fra un testo e una pillola passano carattere e stili", Words("Ciao", Face.LITERATA, bold = true), presa.words)
        assertEquals("e non il colore", Draw.PILL_WORDS, presa.ink)
        assertEquals("né il riquadro", bassa.points, presa.points)
        val data = testo.styledLike(bassa)
        assertEquals("da una pillola il testo prende il carattere", Face.ROBOTO, data.words?.face)
        assertEquals("e tiene il suo colore e il suo fondo", Color.RED, data.ink)
        assertEquals(true, data.words?.label)

        val posata = Mark(Pen.PILL, Draw.pillAround(Words("Ciao"), 0.05f, Offset(0.5f, 0.5f), lato.toFloat(), lato.toFloat()),
            Draw.PILL_WORDS, 0f, false, null, words = Words("Ciao"))
        val alto = kotlin.math.abs(posata.points.last().y - posata.points.first().y) * lato
        assertEquals("la pillola posata da un tocco contiene le parole senza allungarsi", alto / 2f,
            Draw.textFrame(posata, lato.toFloat(), lato.toFloat()).third, 1e-3f)
        assertTrue("ed è più larga che alta", kotlin.math.abs(posata.points.last().x - posata.points.first().x) * lato > alto)
    }

    /**
     * **Con `Testo` un tocco apre la finestra, e il testo nasce dove si è toccato, scelto** (`4.90`,
     * lettura `A1`): `Applica` aspetta una lettera; le parole compaiono sul palco nel colore scelto
     * coi tondi; `Modifica testo` riapre la finestra con le parole e le cambia, e il testo resta dove
     * era; un trascinamento con `Testo` non disegna niente.
     * ⚠️⚠️ **CONTROPROVATA** due volte: senza il ramo di `Testo` nel gesto del palco (il tocco non
     * apre la finestra), e con `Modifica testo` che aggiunge un testo invece di cambiare quello
     * scelto (i testi salvati diventano due).
     * ⚠️⚠️ **Lo schermo è quello di serie del banco, di proposito**, e così le altre due prove che
     * aprono la finestra delle parole: su uno schermo di 411 per 891 dp una finestra con un campo di
     * testo, aperta sopra l'editor durante la prova, non lascia mai il banco in quiete, e lo stesso
     * fa la finestra di `Ridimensiona` (misurato il 2026-10-08). La causa è legata alla misura
     * dello schermo e non nella finestra, e il perché non è accertato: la trappola è scritta in
     * `Rules.md` § '🧪 Quando si scrive una prova, e quando no'.
     */
    @Test
    fun `con Testo un tocco apre la finestra e il testo nasce dove si tocca`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        fun salva(): List<Mark> {
            banco.onNodeWithText(testo(R.string.editor_save)).performClick()
            banco.waitForIdle()
            return salvato?.drawing?.marks.orEmpty()
        }
        val r = immagine()
        // ⚠️ I tocchi sono in proporzione all'immagine, che cambia misura con lo schermo del banco.
        fun su(fx: Float, fy: Float) = Offset(r.left + fx * r.width, r.top + fy * r.height)
        val qui = su(0.3f, 0.25f)
        banco.onNodeWithContentDescription(testo(R.string.draw_text)).performClick()
        banco.onNodeWithContentDescription(testo(R.string.ink_blue)).performClick()
        banco.waitForIdle()
        trascinaDa(su(0.2f, 0.2f), su(0.6f, 0.6f))
        banco.onNodeWithText(testo(R.string.editor_apply)).assertDoesNotExist()
        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        val prima = palco.captureToImage().toPixelMap()
        tocca(qui)
        banco.onNodeWithText(testo(R.string.editor_apply)).assertIsNotEnabled()
        banco.onNode(hasSetTextAction()).performTextInput("Ciao")
        banco.onNodeWithText(testo(R.string.editor_apply)).assertIsEnabled().performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.editor_apply)).assertDoesNotExist()
        assertTrue("le parole dovevano comparire sul palco", differenza(prima, palco.captureToImage().toPixelMap()) > 50)
        val nato = salva().single()
        assertEquals(Pen.TEXT, nato.pen)
        assertEquals("Ciao", nato.words?.text)
        assertTrue("le parole dovevano avere il colore scelto", (nato.ink or 0xFF000000.toInt()) != Color.WHITE)
        assertEquals("il testo doveva nascere dove si è toccato, x", 0.3f, nato.points.single().x, 0.03f)
        assertEquals("il testo doveva nascere dove si è toccato, y", 0.25f, nato.points.single().y, 0.03f)

        banco.onNodeWithContentDescription(testo(R.string.draw_text_edit)).performClick()
        banco.waitForIdle()
        banco.onNode(hasSetTextAction()).performTextReplacement("Ciao\nmondo")
        banco.onNodeWithText(testo(R.string.editor_apply)).performClick()
        banco.waitForIdle()
        val cambiato = salva().single()
        assertEquals("Modifica testo doveva cambiare le parole", "Ciao\nmondo", cambiato.words?.text)
        assertEquals("e il testo doveva restare dov'era", nato.points, cambiato.points)
    }

    /**
     * **I tasti del Testo cambiano il testo scelto** (`4.90`, letture `A3`, `A5` e `A6`): `Grassetto`
     * si accende; `Carattere` passa al carattere dopo e lo dice; `Fondo` passa a `Evidenziato` col
     * giallo, e le parole bianche diventano nere; su una pagina bianca il fondo bianco non si offre e
     * gli altri sì; `Fondo` passa poi a `Etichetta` col viola, e le parole tornano bianche;
     * `Dimensione` cambia il corpo; `Allineamento` passa dal centro a sinistra, dalla `4.92` (sua nota
     * A sul giro della `4.91`; fino alla `4.91` passava a destra).
     * ⚠️⚠️ **CONTROPROVATA** tre volte: senza i parametri del testo fra quelli che il modulo posa
     * sull'elemento scelto (il grassetto non arriva al testo), con la fila dei fondi che offre
     * tutti i colori (il fondo bianco si offre su una pagina bianca), e senza l'allineamento fra le
     * parole del modulo (il testo resta al centro).
     */
    @Test
    fun `i tasti del Testo cambiano il testo scelto`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        fun salva(): Mark {
            banco.onNodeWithText(testo(R.string.editor_save)).performClick()
            banco.waitForIdle()
            return salvato!!.drawing.marks.single()
        }
        banco.onNodeWithContentDescription(testo(R.string.draw_text)).performClick()
        banco.waitForIdle()
        val r = immagine()
        tocca(Offset(r.left + 0.3f * r.width, r.top + 0.25f * r.height))
        banco.onNode(hasSetTextAction()).performTextInput("Ciao")
        banco.onNodeWithText(testo(R.string.editor_apply)).performClick()
        banco.waitForIdle()

        val grassetto = banco.onNodeWithContentDescription(testo(R.string.draw_bold))
        grassetto.performClick()
        banco.waitForIdle()
        grassetto.assertIsOn()
        assertTrue("Grassetto doveva arrivare al testo scelto", salva().words!!.bold)

        val carattere = testo(R.string.draw_face)
        banco.onNodeWithContentDescription("$carattere: Roboto").performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription("$carattere: Montserrat").assertExists()
        assertEquals(Face.MONTSERRAT, salva().words!!.face)

        val fondo = testo(R.string.draw_ground)
        val sfondo = banco.onNodeWithContentDescription(fondo)
        sfondo.assertIsOff().performClick()
        banco.waitForIdle()
        sfondo.assertIsOn()
        val etichetta = salva()
        assertEquals(true, etichetta.words!!.label)
        assertEquals(Draw.LABEL_INK, etichetta.words!!.ground)
        assertEquals("sulla striscia viola le parole restano bianche", Color.WHITE, etichetta.ink)
        banco.onNodeWithContentDescription("$fondo 2").assertIsEnabled().performClick()
        banco.waitForIdle()
        assertEquals(Draw.LABEL_INKS[1], salva().words!!.ground)
        sfondo.performClick()
        banco.waitForIdle()
        sfondo.assertIsOff()
        assertEquals("Sfondo doveva togliere la striscia", false, salva().words!!.label)

        banco.onAllNodesWithContentDescription(testo(R.string.draw_size))
            .filterToOne(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(Draw.textTrack(0.1f)) }
        banco.waitForIdle()
        assertEquals("Dimensione doveva cambiare il corpo", 0.1f, salva().width, 1e-4f)

        val allineamento = testo(R.string.draw_align)
        banco.onNodeWithContentDescription("$allineamento: ${testo(R.string.draw_center)}").performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription("$allineamento: ${testo(R.string.settings_left)}").assertExists()
        assertEquals("Allineamento doveva passare a sinistra", Align.LEFT, salva().words!!.align)
        banco.onNodeWithContentDescription("$allineamento: ${testo(R.string.settings_left)}").performClick()
        banco.waitForIdle()
        assertEquals("e poi a destra", Align.RIGHT, salva().words!!.align)
    }

    /**
     * **Con `Pillola` un trascinamento disegna la pillola e chiede le parole** (`4.90`, sua nota A sul
     * giro della `4.43`): la pillola nasce scelta, coi due angoli del trascinamento, e col rosso suo;
     * `Dimensione` e `Sfondo` sono spenti. Dalla `4.91` i tondi sono accesi e danno i colori della
     * pillola (sua nota su `4.90-05`), con l'opacità di sempre.
     * ⚠️⚠️ **CONTROPROVATA** due volte: senza la domanda delle parole alla fine del trascinamento (la
     * finestra non compare), e senza il colore della pillola fra i valori che il modulo posa
     * sull'elemento scelto (il tondo non lo cambia).
     * ⚠️ Il tocco che posa la pillola ha una prova sua: sullo schermo di serie del banco l'immagine
     * misura una trentina di pixel, e la portata del dito sugli elementi (24 dp) la copre tutta,
     * quindi qui un tocco sul vuoto prende sempre la pillola appena disegnata.
     */
    @Test
    fun `con Pillola un trascinamento disegna la pillola e chiede le parole`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        fun salva(): List<Mark> {
            banco.onNodeWithText(testo(R.string.editor_save)).performClick()
            banco.waitForIdle()
            return salvato!!.drawing.marks
        }
        banco.onNodeWithContentDescription(testo(R.string.draw_pill)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription("${testo(R.string.draw_ground)} 6").assertIsEnabled()
        banco.onAllNodesWithContentDescription(testo(R.string.draw_size))
            .filterToOne(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)).assertIsNotEnabled()
        banco.onNodeWithContentDescription(testo(R.string.draw_ground)).assertIsNotEnabled()
        val r = immagine()
        trascinaDa(Offset(r.left + 0.1f * r.width, r.top + 0.2f * r.height), Offset(r.left + 0.8f * r.width, r.top + 0.4f * r.height))
        // ⚠️ Dalla `4.92` una pillola può non avere parole (sua nota C sul giro della `4.91`).
        banco.onNodeWithText(testo(R.string.editor_apply)).assertIsEnabled()
        banco.onNode(hasSetTextAction()).performTextInput("Ciao")
        banco.onNodeWithText(testo(R.string.editor_apply)).performClick()
        banco.waitForIdle()
        val disegnata = salva().single()
        assertEquals(Pen.PILL, disegnata.pen)
        assertEquals("Ciao", disegnata.words?.text)
        assertEquals("la pillola ha i due angoli del trascinamento", 2, disegnata.points.size)
        assertEquals("la pillola nasce col suo rosso", Draw.PILL_FILL, disegnata.fill)
        banco.onNodeWithContentDescription(testo(R.string.draw_text_edit)).assertIsEnabled()
        banco.onNodeWithContentDescription("${testo(R.string.draw_ground)} 3").performClick()
        banco.waitForIdle()
        assertEquals("il tondo doveva dare il suo colore alla pillola scelta", Draw.pillFill(Draw.PILL_INKS[2]), salva().single().fill)
        assertEquals("con l'opacità della pillola", 0xCC, Color.alpha(salva().single().fill!!))
    }

    /**
     * **Con `Pillola` un tocco chiede le parole e posa la pillola attorno a loro** (`4.90`): la pillola
     * nasce dove si tocca, più larga che alta per una parola sola, e già scelta.
     * ⚠️⚠️ **CONTROPROVATA**: con la pillola posata nel solo punto toccato, senza `Draw.pillAround`, la
     * pillola non è più larga che alta.
     */
    @Test
    fun `con Pillola un tocco posa la pillola attorno alle parole`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        banco.onNodeWithContentDescription(testo(R.string.draw_pill)).performClick()
        banco.waitForIdle()
        tocca(Offset.Zero)
        banco.onNodeWithText(testo(R.string.editor_apply)).assertIsEnabled()
        banco.onNode(hasSetTextAction()).performTextInput("Seconda")
        banco.onNodeWithText(testo(R.string.editor_apply)).performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        val posata = salvato!!.drawing.marks.single()
        assertEquals(Pen.PILL, posata.pen)
        assertEquals("Seconda", posata.words?.text)
        val (a, b) = posata.points.first() to posata.points.last()
        assertTrue("posata attorno alle parole, più larga che alta", kotlin.math.abs(b.x - a.x) > kotlin.math.abs(b.y - a.y))
        assertEquals("posata dove si è toccato", 0.5f, (a.y + b.y) / 2f, 0.1f)
        banco.onNodeWithContentDescription(testo(R.string.draw_text_edit)).assertIsEnabled()
    }

    /**
     * **Un testo nuovo nasce con la riga più lunga larga metà dell'immagine** (`4.92`, sua nota su
     * `4.91-03`: *forse è bene stabilire una dimensione predefinita dei testi: la riga più lunga deve
     * misurare il 50% della larghezza dell'immagine*), su un'immagine larga e su una alta; una
     * lettera sola si ferma al tetto. E l'allineamento va dal centro a sinistra, poi a destra (sua
     * nota A sul giro della `4.91`).
     * ⚠️⚠️ **CONTROPROVATA**: con `Draw.fitSize` che rende il corpo di fabbrica, la riga non è larga
     * metà dell'immagine.
     */
    @Test
    fun `un testo nuovo nasce con la riga lunga mezza immagine`() {
        Faces.load(app)
        for ((w, h) in listOf(400 to 200, 200 to 400)) {
            val parole = Words("Ciao mondo\nCiao")
            val corpo = Draw.fitSize(parole, w.toFloat(), h.toFloat())
            val testo = Mark(Pen.TEXT, listOf(Offset(0.5f, 0.5f)), Color.WHITE, corpo, false, null, words = parole)
            val (hw, _) = Draw.textHalf(testo, w.toFloat(), h.toFloat())
            // ⚠️ Il banco arrotonda il corpo al pixel intero (misurato: a 20 e a 20,75 px la riga è
            // larga uguale), quindi lo scarto ammesso è quello di un pixel di corpo.
            val pixel = w / 2f / (corpo * maxOf(w, h)) + 1f
            assertEquals("la riga più lunga doveva misurare metà della larghezza, $w x $h", w / 2f, 2f * hw, pixel)
        }
        assertEquals("una lettera sola si ferma al tetto", Draw.TEXT_MAX, Draw.fitSize(Words("i"), 4000f, 100f), 1e-6f)
        assertEquals(Align.LEFT, Align.CENTER.next)
        assertEquals(Align.RIGHT, Align.LEFT.next)
        assertEquals(Align.CENTER, Align.RIGHT.next)
        assertEquals("gli angoli del Pannello sono all'1% del suo lato lungo", 0.01f, Draw.PANEL_ROUND, 0f)
    }

    /**
     * **Le righe di minuscole si centrano sulla loro fascia** (`4.92`, sua nota B sul giro della
     * `4.91`, col mockup di una pillola alta 126 px sopra le minuscole e 96 sotto): in una pillola e
     * su una striscia dell'etichetta le lettere di `xo`, che stanno tutte nella fascia delle
     * minuscole, hanno lo stesso centro del riquadro, entro un pixel e mezzo.
     * ⚠️⚠️ **CONTROPROVATA**: con le righe centrate sempre sulla metà della H, le minuscole cadono
     * sotto il centro di parecchi pixel, nella pillola e sulla striscia.
     */
    @Test
    fun `le minuscole stanno al centro della pillola e della striscia`() {
        Faces.load(app)
        val lato = 1000
        fun bianchi(tela: Bitmap): IntRange {
            var y0 = lato; var y1 = -1
            for (y in 0 until lato) for (x in 0 until lato) {
                val p = tela.getPixel(x, y)
                if (Color.alpha(p) > 250 && Color.red(p) > 245 && Color.green(p) > 245 && Color.blue(p) > 245) {
                    y0 = minOf(y0, y); y1 = maxOf(y1, y)
                }
            }
            return y0..y1
        }
        val pillola = Mark(Pen.PILL, listOf(Offset(0.2f, 0.4f), Offset(0.8f, 0.6f)), Draw.PILL_WORDS, 0f, false, null, words = Words("xo"))
        val nella = bianchi(Draw.overlay(Drawing(listOf(pillola)), lato, lato, Spin(0, false))!!)
        assertTrue("le parole dovevano esserci", nella.last > nella.first + 20)
        assertEquals("le minuscole al centro della pillola", 500f, (nella.first + nella.last + 1) / 2f, 1.5f)

        val etichetta = Mark(Pen.TEXT, listOf(Offset(0.5f, 0.5f)), Color.WHITE, 0.2f, false, null, words = Words("xo", label = true))
        val tela = Draw.overlay(Drawing(listOf(etichetta)), lato, lato, Spin(0, false))!!
        val parole = bianchi(tela)
        var s0 = lato; var s1 = -1
        for (y in 0 until lato) {
            val p = tela.getPixel(lato / 2, y)
            if (Color.alpha(p) > 250 && kotlin.math.abs(Color.red(p) - 0xA3) < 3 && kotlin.math.abs(Color.green(p) - 0x40) < 3 &&
                kotlin.math.abs(Color.blue(p) - 0x8F) < 3) { s0 = minOf(s0, y); s1 = maxOf(s1, y) }
        }
        assertTrue("la striscia doveva esserci", s1 > s0)
        assertEquals("le minuscole al centro della striscia", (s0 + s1 + 1) / 2f, (parole.first + parole.last + 1) / 2f, 1.5f)
    }

    /**
     * **Dove una riga dell'etichetta è più stretta della vicina, l'incastro ha un raccordo concavo**
     * (`4.92`, sua nota D sul giro della `4.91`, col suo mockup: *gli arrotondamenti non devono stare
     * nelle linee intermedie, anzi lì ci vorrebbe un arrotondamento contrario*): appena sopra il
     * punto in cui la riga stretta incontra quella larga la striscia è più larga che a metà della
     * riga, perché il raccordo la allarga verso la vicina; gli angoli di fuori restano tondi, cioè
     * in cima alla riga stretta e in cima alla parte che sporge della riga larga la striscia è più
     * stretta che a metà.
     * ⚠️⚠️ **CONTROPROVATA**: con quattro angoli tondi per ogni striscia, come fino alla `4.91`, appena
     * sopra l'incastro la striscia stretta è più stretta che a metà.
     */
    @Test
    fun `l'etichetta ha raccordi concavi fra righe di larghezza diversa`() {
        Faces.load(app)
        val lato = 400
        val segno = Mark(Pen.TEXT, listOf(Offset(0.5f, 0.5f)), Color.WHITE, 0.1f, false, null,
            words = Words("x\nxxxxxxx", label = true))
        val tela = Draw.overlay(Drawing(listOf(segno)), lato, lato, Spin(0, false))!!
        fun largo(y: Int): Int {
            var x0 = lato; var x1 = -1
            for (x in 0 until lato) {
                val p = tela.getPixel(x, y)
                if (Color.alpha(p) > 250 && kotlin.math.abs(Color.red(p) - 0xA3) < 3 && kotlin.math.abs(Color.green(p) - 0x40) < 3 &&
                    kotlin.math.abs(Color.blue(p) - 0x8F) < 3) { x0 = minOf(x0, x); x1 = maxOf(x1, x) }
            }
            return x1 - x0 + 1
        }
        // Due righe di un corpo di 40 px, a un passo di 52: la stretta va da 148 a 200, la larga da 200 a 252.
        val meta = largo(174)
        assertTrue("la riga stretta doveva esserci", meta > 10)
        assertTrue("sopra l'incastro il raccordo doveva allargare la striscia: ${largo(198)} contro $meta", largo(198) > meta + 4)
        assertTrue("in cima l'angolo di fuori doveva restare tondo: ${largo(149)} contro $meta", largo(149) < meta - 4)
        assertTrue("la riga larga doveva essere più larga", largo(226) > meta + 40)
        assertTrue("e in cima la sua parte che sporge doveva avere gli angoli tondi", largo(202) < largo(226) - 4)
    }

    /**
     * **Cambiare il colore non riporta il testo al corpo di prima** (`4.92`, sua nota su `4.91-03`:
     * *Se ingrandisco il testo e poi cambio colore, il colore si applica ma il testo ritorna piccolo
     * come in origine*): il testo nasce col suo corpo, una maniglia d'angolo lo ingrandisce, e un tondo
     * gli cambia il colore lasciando il corpo nuovo. `Dimensione` è spento finché nessun testo è
     * scelto, perché un testo nuovo nasce col corpo suo.
     * ⚠️⚠️ **CONTROPROVATA**: senza il modulo che ricarica l'elemento scelto alla fine del gesto, il
     * testo torna al corpo con cui era nato.
     */
    @Test
    @Config(qualifiers = "w320dp-h891dp")
    fun `il colore non riporta il testo al corpo di prima`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        fun salva(): Mark {
            banco.onNodeWithText(testo(R.string.editor_save)).performClick()
            banco.waitForIdle()
            return salvato!!.drawing.marks.single()
        }
        banco.onNodeWithContentDescription(testo(R.string.draw_text)).performClick()
        banco.waitForIdle()
        banco.onAllNodesWithContentDescription(testo(R.string.draw_size))
            .filterToOne(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)).assertIsNotEnabled()
        val r = immagine()
        fun su(p: Offset) = Offset(r.left + p.x * r.width, r.top + p.y * r.height)
        tocca(su(Offset(0.5f, 0.5f)))
        banco.onNode(hasSetTextAction()).performTextInput("Ciao")
        banco.onNodeWithText(testo(R.string.editor_apply)).performClick()
        banco.waitForIdle()
        banco.onAllNodesWithContentDescription(testo(R.string.draw_size))
            .filterToOne(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)).assertIsEnabled()
        val nato = salva()
        assertEquals("il testo nasce col corpo che fa la riga larga metà dell'immagine",
            Draw.fitSize(nato.words!!, LATO.toFloat(), LATO.toFloat()), nato.width, 1e-4f)
        val centro = nato.points.single()
        val angolo = nato.handles(LATO, LATO)[2]
        trascinaDa(su(angolo), su(centro + (angolo - centro) * 1.5f))
        val grande = salva()
        assertTrue("la maniglia doveva ingrandire il testo: ${grande.width} contro ${nato.width}", grande.width > 1.3f * nato.width)
        banco.onNodeWithContentDescription(testo(R.string.ink_blue)).performClick()
        banco.waitForIdle()
        val blu = salva()
        assertEquals("il tondo doveva dare il suo colore", Draw.INKS[3], blu.ink or 0xFF000000.toInt())
        assertEquals("e lasciare il corpo nuovo", grande.width, blu.width, 1e-4f)
    }

    /**
     * **Il cursore e le file dei tasti restano dove sono con ogni strumento** (`4.92`, sua nota E sul
     * giro della `4.91`: *Quando si passa allo strumento Testo, le due file di tasti principali del
     * modulo si avvicinano fino a toccarsi ... posiziona lo slider nella stessa posizione anche con
     * gli altri strumenti*): col Rettangolo e col Testo con `Sfondo` acceso, la fila dei tasti e il
     * cursore hanno lo stesso posto, e le strisce sono rettangoli affiancati senza spazio fra loro.
     * Il tasto `Sfondo` acceso è un rettangolo del colore della striscia con le lettere in negativo,
     * cioè del fondo del tasto, e spento non ha il colore della striscia (sua nota F).
     * ⚠️⚠️ **CONTROPROVATA** due volte: con la fila delle strisce che compare sotto il cursore senza
     * un posto suo, il cursore del Testo si sposta; e con le lettere bianche sul rettangolo al posto
     * del negativo, dentro il rettangolo non c'è il fondo del tasto.
     */
    @Test
    @Config(qualifiers = "w320dp-h891dp")
    fun `il cursore e le file dei tasti restano dove sono con ogni strumento`() {
        banco.setContent { Scena() }
        pronta()
        apriDisegno()
        fun cursore() = banco.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)).getUnclippedBoundsInRoot()
        banco.onNodeWithContentDescription(testo(R.string.draw_rect)).performClick()
        banco.waitForIdle()
        val prima = cursore()
        val tasti = banco.onNodeWithContentDescription(testo(R.string.draw_dashed)).getUnclippedBoundsInRoot()
        banco.onNodeWithContentDescription(testo(R.string.draw_text)).performClick()
        banco.waitForIdle()
        val r = immagine()
        tocca(Offset(r.left + 0.5f * r.width, r.top + 0.5f * r.height))
        banco.onNode(hasSetTextAction()).performTextInput("Ciao")
        banco.onNodeWithText(testo(R.string.editor_apply)).performClick()
        banco.waitForIdle()
        val fondo = testo(R.string.draw_ground)
        val sfondo = banco.onNodeWithContentDescription(fondo)
        val spento = sfondo.captureToImage().toPixelMap()
        val striscia = androidx.compose.ui.graphics.Color(Draw.LABEL_INK)
        assertEquals("spento, il tasto Sfondo non ha il colore della striscia", 0, inchiostro(spento, striscia))
        sfondo.performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription("$fondo 1").assertExists()
        assertEquals("col Testo il cursore doveva restare dov'era", prima.top.value, cursore().top.value, 0.5f)
        assertEquals("e la fila dei tasti", tasti.top.value,
            banco.onNodeWithContentDescription(testo(R.string.draw_bold)).getUnclippedBoundsInRoot().top.value, 0.5f)
        val uno = banco.onNodeWithContentDescription("$fondo 1").getUnclippedBoundsInRoot()
        val due = banco.onNodeWithContentDescription("$fondo 2").getUnclippedBoundsInRoot()
        assertEquals("le strisce dovevano essere affiancate senza spazio", uno.right.value, due.left.value, 0.5f)
        assertTrue("e più basse dei tondi", uno.bottom.value - uno.top.value < 32f)

        val acceso = sfondo.captureToImage().toPixelMap()
        assertTrue("acceso, il tasto doveva avere il colore della striscia", inchiostro(acceso, striscia) > 100)
        val (cx, cy) = centro(acceso, striscia)
        val dietro = acceso[acceso.width / 2, 4]
        var lettere = 0
        for (y in cy - 6..cy + 6) for (x in cx - 12..cx + 12) if (vicino(acceso[x, y], dietro)) lettere++
        assertTrue("le lettere dovevano essere in negativo, del fondo del tasto: $lettere", lettere > 10)
    }

    /**
     * **Pillola e Pannello si applicano senza parole, ed `Elimina` vale anche per loro** (`4.92`, sua
     * nota C sul giro della `4.91`: *Nella pillola e nel pannello, il testo non dev'essere
     * obbligatorio*; e suo `Non approvato` su `4.91-04`: *Se sto usando la pillola o ne ho una
     * selezionata, il tasto 'Elimina' non appare*): `Applica` si accende a campo vuoto, il pannello
     * nasce senza parole e scelto, e `Elimina`, un tasto di testo accanto a `Elimina tutto`, lo toglie.
     * ⚠️⚠️ **CONTROPROVATA** due volte: con `Applica` che aspetta una lettera anche qui, e con `Elimina`
     * nella sola fila delle forme, come nella `4.91`.
     */
    @Test
    fun `pillola e pannello si applicano senza parole ed Elimina vale per loro`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        pronta()
        apriDisegno()
        banco.onNodeWithContentDescription(testo(R.string.draw_panel)).performClick()
        banco.waitForIdle()
        val r = immagine()
        trascinaDa(Offset(r.left + 0.1f * r.width, r.top + 0.2f * r.height), Offset(r.left + 0.8f * r.width, r.top + 0.6f * r.height))
        banco.onNodeWithText(testo(R.string.editor_apply)).assertIsEnabled().performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        val pannello = salvato!!.drawing.marks.single()
        assertEquals(Pen.PANEL, pannello.pen)
        assertEquals("il pannello nasce senza parole", "", pannello.words?.text)
        banco.onNodeWithText(testo(R.string.pick_delete)).assertIsEnabled().performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.draw_clear)).assertIsNotEnabled()
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

    /**
     * La voce [id] del menu della pressione lunga: dalla `4.92` `Elimina` è anche un tasto del
     * modulo, quindi la voce si cerca dentro il menu.
     */
    private fun vocePopup(id: Int) = banco.onAllNodesWithText(testo(id)).filterToOne(hasAnyAncestor(isPopup()))

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
    private fun diffDurante(dx: Float, dy: Float, da: Offset = Offset.Zero): Int {
        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        palco.performTouchInput { down(center + da) }
        palco.performTouchInput { moveTo(center + da + Offset(dx, dy)) }
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
    private fun Scena(
        onSave: (Look) -> Unit = {},
        onAccent: (androidx.compose.ui.graphics.Color) -> Unit = {},
        uri: Uri = quadrato()
    ) {
        AivTheme(darkTheme = false) {
            onAccent(MaterialTheme.colorScheme.primary)
            Box(modifier = Modifier.fillMaxSize()) {
                AdvancedEditorScreen(
                    uri = uri,
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

    /** A [LATO] square of black and white columns two pixels wide: a blur turns it grey. */
    private fun righe(): Uri {
        val file = File(app.cacheDir, "righe.png")
        if (!file.exists()) {
            val mappa = Bitmap.createBitmap(LATO, LATO, Bitmap.Config.ARGB_8888)
            for (x in 0 until LATO) for (y in 0 until LATO) mappa.setPixel(x, y, if ((x / 2) % 2 == 0) Color.BLACK else Color.WHITE)
            file.outputStream().use { mappa.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return Uri.fromFile(file)
    }

    /** How many pixels of [mappa] are a middle grey, in the square of side [lato] around [dove] from its centre. */
    private fun grigi(mappa: PixelMap, dove: Offset, lato: Int): Int {
        val cx = (mappa.width / 2f + dove.x).toInt()
        val cy = (mappa.height / 2f + dove.y).toInt()
        var n = 0
        for (y in cy - lato / 2 until cy + lato / 2) for (x in cx - lato / 2 until cx + lato / 2) {
            val c = mappa[x, y]
            if (c.red in 0.25f..0.75f && kotlin.math.abs(c.red - c.green) < 0.05f && kotlin.math.abs(c.red - c.blue) < 0.05f) n++
        }
        return n
    }

    private fun testo(id: Int): String = app.getString(id)

    private val app: Context get() = ApplicationProvider.getApplicationContext()
}

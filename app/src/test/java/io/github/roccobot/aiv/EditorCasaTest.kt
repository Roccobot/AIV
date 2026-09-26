package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.down
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Le proporzioni su un'immagine larga, nell'ordine dei gettoni: crescente dalla `2.88`, e col 5:4
 * fra 1:1 e 4:3 dalla `2.89` (sue istruzioni).
 */
private val NUMERI = listOf("1:1", "5:4", "4:3", "3:2", "16:9")

/**
 * Il banco dell'**editor semplice**, dalla `2.87`.
 *
 * ⚠️⚠️ **NASCE CON LA RISPOSTA `intera` A `d-originale-verso`**, e prima di lei questo editor non
 * aveva prove sue: le forme del ritaglio le disegna lo stesso pezzo nei due editor (`ShapeRow`),
 * ma quello che succede girando l'immagine no. Qui un quarto di giro **rifà** il rettangolo
 * sull'aspetto nuovo, e di là lo porta con sé, quindi una prova sul solo editor completo
 * misurerebbe una delle due strade.
 *
 * ⚠️ **Che cosa NON vede**: il file salvato, che vuole una decodifica vera e una scrittura su disco.
 * Qui si guarda quello che la schermata consegna al salvataggio, cioè il giro e il rettangolo.
 */
@RunWith(AndroidJUnit4::class)
// ⚠️ La grafica vera serve per DECODIFICARE il PNG dell'anteprima: senza, i comandi restano
// spenti e non c'è niente da toccare. È la stessa riga di `SviluppoTest`.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorCasaTest {

    @get:Rule
    val banco = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private fun testo(id: Int): String = app.getString(id)

    /**
     * **Dopo un quarto di giro 'Originale' è ancora l'immagine intera.**
     *
     * ⚠️⚠️ **È LA SUA RISPOSTA `intera`**, con la richiesta in chat (*'Originale' che si adatta
     * anche al verso della rotazione attuale*): girando, il verso resta quello di partenza, quindi
     * fino alla `2.86` 'Originale' su un'immagine larga girata di un quarto dava una cornice larga
     * dentro un'immagine alta.
     * ⚠️ **Si guarda quello che il salvataggio riceve**: la rotazione da sola accende 'Salva', quindi
     * il tasto acceso non direbbe niente, mentre il rettangolo sì.
     * ⚠️⚠️ **CONTROPROVATA** rimettendo il rapporto che segue il verso: il rettangolo consegnato
     * taglia tre quarti dell'immagine.
     */
    @Test
    fun `dopo un quarto di giro Originale e ancora l'immagine intera`() {
        var giri = -1
        var taglio: ImageEdit.Crop? = null
        banco.setContent {
            Scena(onSave = { turns, _, crop ->
                giri = turns
                taglio = crop
            })
        }
        pronta()
        banco.onNodeWithText(testo(R.string.editor_right)).performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.editor_shape_original)).performClick()
        banco.waitForIdle()

        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        assertEquals("il quarto di giro doveva arrivare al salvataggio", 1, giri)
        assertNotNull("il tocco su 'Salva' doveva consegnare il rettangolo", taglio)
        assertTrue("'Originale' doveva tenere l'immagine intera: $taglio", taglio!!.whole)
    }

    /**
     * **Con 'Originale' i due versi si spengono, e con una proporzione tornano.**
     *
     * ⚠️ **È la seconda metà della stessa risposta** (*'Originale' ignora i due gettoni del
     * verso*): accesi, un tocco non cambierebbe la cornice e si leggerebbe come un comando rotto.
     * Qui i due versi sono una riga di chip scritti a parole, di là due icone: la condizione è la
     * stessa, e vive in due posti perché le due righe sono due.
     * ⚠️⚠️ **CONTROPROVATA** togliendo la condizione da questa riga: i due chip restano accesi e
     * la prima asserzione cade.
     */
    @Test
    fun `con Originale i due versi si spengono`() {
        banco.setContent { Scena() }
        pronta()
        val verticale = banco.onNodeWithText(testo(R.string.editor_tall))
        verticale.assertIsEnabled()

        banco.onNodeWithText(testo(R.string.editor_shape_original)).performClick()
        banco.waitForIdle()
        verticale.assertIsNotEnabled()
        banco.onNodeWithText(testo(R.string.editor_wide)).assertIsNotEnabled()

        banco.onNodeWithText("1:1").performClick()
        banco.waitForIdle()
        verticale.assertIsEnabled()
    }

    /**
     * **Le forme vanno su due righe: 'Libero' coi numeri in ordine crescente, 'Originale' coi due
     * versi.**
     *
     * ⚠️⚠️ **È LA SUA RICHIESTA ALLA LETTERA** (campo libero del giro della `2.87`, con una
     * schermata: *Riga 1: Libero, 1:1, 4:3, 3:2, 16:9. Riga 2: Originale, Verticale,
     * Orizzontale*), e l'ordine dei numeri vale nei due editor (sua risposta in chat).
     * ⚠️ **Dalla `2.89` c'è anche il 5:4, fra 1:1 e 4:3** (2026-09-26: *1:1, 5:4, 4:3, 3:2, 16:9*).
     * ⚠️ **L'immagine è larga, quindi i numeri si scrivono in orizzontale**: sono i testi della sua
     * riga.
     * ⚠️⚠️ **CONTROPROVATA** tre volte: con 'Originale' nella prima riga cade la seconda asserzione,
     * con l'ordine di prima (3:2 prima di 4:3) cade l'ultima, e dalla `2.89` cade anche col 5:4
     * rimesso dopo il 4:3.
     */
    @Test
    fun `le forme vanno su due righe, Libero coi numeri e Originale coi versi`() {
        banco.setContent { Scena() }
        pronta()
        fun riquadro(t: String) = banco.onNodeWithText(t).fetchSemanticsNode().boundsInRoot
        val libero = riquadro(testo(R.string.editor_free))
        val numeri = NUMERI.map(::riquadro)
        val originale = riquadro(testo(R.string.editor_shape_original))
        val versi = listOf(R.string.editor_tall, R.string.editor_wide).map { riquadro(testo(it)) }

        for (n in numeri) {
            assertEquals("i numeri vivono sulla riga di 'Libero'", libero.top, n.top, 2f)
        }
        for (v in versi) {
            assertEquals("i due versi vivono sulla riga di 'Originale'", originale.top, v.top, 2f)
        }
        assertTrue("e quella riga viene dopo", originale.top > libero.top + 2f)
        assertTrue(
            "i numeri vanno in ordine crescente: ${NUMERI.joinToString()}",
            numeri.zipWithNext().all { (a, b) -> a.left < b.left }
        )
    }

    /**
     * **Nella prima riga, se non c'è posto per tutti, si stringe la parola e non i numeri.**
     *
     * ⚠️⚠️ **NASCE CON LA `2.89`**: col 5:4 le celle sono sei, e 'Libero' non ha più un peso fisso
     * ma la larghezza della sua parola (vedi `shapeCell`). La regola si prova qui con le misure
     * vere, perché sul banco i testi misurano meno che sul telefono: Roboto Medium a `labelMedium`
     * con la spaziatura delle lettere di Material, più i due rientri del chip, su una riga che tolti
     * i distacchi ne lascia 282.
     * - **In italiano la riga ci sta**, e l'avanzo si divide in proporzione.
     * - **In tamil no**, e a pagare il punto che manca è la parola: '16:9' troncato direbbe un'altra
     *   proporzione.
     * ⚠️⚠️ **CONTROPROVATA** dividendo sempre in proporzione: in tamil un numero scende sotto quello
     * che chiede (37,48 punti contro 37,62), e l'asserzione del tamil cade.
     */
    @Test
    fun `se la riga non ci sta si stringe la parola e non i numeri`() {
        val stanza = 282.dp
        val numero = 37.62.dp // '16:9', il più largo dei numeri
        val italiano = 48.61.dp // 'Libero'
        val tamil = 94.96.dp // il 'Libero' tamil, col Noto Sans Tamil

        val cella = shapeCell(italiano, numero, 5, stanza)
        val parola = stanza - cella * 5
        assertTrue("in italiano un numero non doveva stringersi: $cella", cella >= numero)
        assertTrue("e la parola nemmeno: $parola", parola >= italiano)
        assertEquals("l'avanzo si divide in proporzione", cella / numero, parola / italiano, 1e-3f)

        assertEquals(
            "in tamil un numero doveva tenere la sua misura",
            numero.value, shapeCell(tamil, numero, 5, stanza).value, 1e-4f
        )
        assertTrue("e a stringersi doveva essere la parola", stanza - numero * 5 < tamil)

        assertEquals(
            "dove non ci stanno nemmeno i numeri le celle si dividono la riga",
            25f, shapeCell(italiano, numero, 5, 150.dp).value, 1e-4f
        )
    }

    /**
     * **Su uno schermo da 360dp i numeri della prima riga sono larghi uguali, e la riga arriva ai
     * due rientri.**
     *
     * ⚠️ **È la metà della `2.89` che il banco può vedere**: quanto è larga ogni cella dipende dai
     * testi, che qui misurano meno che sul telefono, mentre che i numeri siano larghi uguali e che
     * 'Libero' prenda il resto fino al bordo no. ⚠️ **E conferma la larghezza della fila**, cioè il
     * numero su cui si regge il conto in `ShapeRow`: 360 meno i due rientri.
     * ⚠️⚠️ **CONTROPROVATA** due volte: con ogni numero largo quanto il proprio testo cade la prima
     * asserzione (38 pixel contro 47), e con 'Libero' fermo alla misura della sua parola cade
     * l'ultima (la riga finisce a 328 invece che a 336).
     */
    @Test
    @Config(qualifiers = "w360dp-h720dp")
    fun `i numeri della prima riga sono larghi uguali e la riga arriva ai rientri`() {
        banco.setContent { Scena() }
        pronta()
        fun riquadro(t: String) = banco.onNodeWithText(t).fetchSemanticsNode().boundsInRoot
        val libero = riquadro(testo(R.string.editor_free))
        val numeri = NUMERI.map(::riquadro)
        val schermo = banco.onRoot().fetchSemanticsNode().boundsInRoot.width
        for (n in numeri) {
            assertEquals("i numeri dovevano essere larghi uguali", numeri[0].width, n.width, 1f)
        }
        with(banco.density) {
            assertEquals("lo schermo di prova è largo 360dp", 360.dp.toPx(), schermo, 1f)
            assertEquals("la riga comincia al rientro di sinistra", STAGE_SIDE.toPx(), libero.left, 1f)
            assertEquals(
                "e 'Libero' prende il resto fino al rientro di destra",
                schermo - STAGE_SIDE.toPx(), numeri.last().right, 1f
            )
        }
    }

    /**
     * **Tirando un angolo compare la lente, in alto dalla parte opposta al dito, e al rilascio se ne
     * va.**
     *
     * ⚠️ **Nasce con la `2.88`, e non per un difetto di questo editor**: la lente adesso la usano
     * tutti e due (sua richiesta del giro della `2.87`), e quello che si vede dentro arriva da chi
     * la chiama. Qui si presidia che il trasloco non l'abbia persa per strada.
     * ⚠️ **L'immagine è ROSSA**, perché il bordo si trova nei pixel: su un'immagine bianca si
     * confonderebbe con le squadrette, che sono bianche anche loro.
     * ⚠️⚠️ **CONTROPROVATA** togliendo la lente dal disegno: i due scatti sono identici.
     */
    @Test
    fun `tirando un angolo compare la lente`() {
        banco.setContent { Scena(uri = rosso()) }
        pronta()
        val radice = banco.onRoot()
        val prima = radice.captureToImage().toPixelMap()
        val pieno = androidx.compose.ui.graphics.Color.Red.toArgb()
        var sx = prima.width
        var su = prima.height
        for (y in 0 until prima.height) {
            for (x in 0 until prima.width) {
                if (prima[x, y].toArgb() == pieno) {
                    if (x < sx) sx = x
                    if (y < su) su = y
                }
            }
        }
        assertTrue("l'immagine rossa doveva essere in scena", sx < prima.width && su < prima.height)
        /*
         * ⚠️⚠️ **IL MOVIMENTO VA IN DUE COLPI, E IL BANCO LO HA IMPOSTO**: questo palco usa
         * `detectDragGestures`, che fa cominciare il trascinamento **dove il dito ha superato la
         * soglia**. Con un salto solo quel punto è la destinazione, cioè lontano dall'angolo, e la
         * presa diventa l'interno. Il primo colpo paga la soglia vicino all'angolo, il secondo tira:
         * misurato, un primo colpo da 12 pixel non la supera, e uno da 26 sì, restando dentro i
         * quaranta della presa.
         */
        radice.performTouchInput { down(Offset(sx + 2f, su + 2f)) }
        banco.waitForIdle()
        radice.performTouchInput { moveTo(Offset(sx + 24f, su + 10f)) }
        banco.waitForIdle()
        radice.performTouchInput { moveTo(Offset(width * 0.35f, su + 60f)) }
        banco.waitForIdle()
        val tenuto = radice.captureToImage().toPixelMap()
        radice.performTouchInput { up() }
        banco.waitForIdle()
        val lasciato = radice.captureToImage().toPixelMap()

        var destra = 0
        var sinistra = 0
        for (y in 0 until minOf(tenuto.height, lasciato.height)) {
            for (x in 0 until minOf(tenuto.width, lasciato.width)) {
                if (tenuto[x, y] != lasciato[x, y]) {
                    if (x >= tenuto.width / 2) destra += 1 else sinistra += 1
                }
            }
        }
        assertTrue("col dito su un angolo la lente doveva comparire a destra", destra > 0)
        assertEquals("e a sinistra non doveva cambiare niente", 0, sinistra)
    }

    /**
     * Aspetta l'anteprima: finché non c'è, i comandi della scheda sono spenti.
     *
     * ⚠️ **Si aspetta un comando ACCESO e non il comando**: il chip c'è anche prima, spento, quindi
     * aspettarne la sola presenza lascerebbe partire la prova con l'immagine ancora da decodificare.
     */
    private fun pronta() {
        banco.waitUntil(5_000) {
            banco.onAllNodes(hasText(testo(R.string.editor_right)) and isEnabled())
                .fetchSemanticsNodes().isNotEmpty()
        }
        banco.waitForIdle()
    }

    @androidx.compose.runtime.Composable
    private fun Scena(
        uri: Uri = largo(),
        onSave: (Int, Boolean, ImageEdit.Crop) -> Unit = { _, _, _ -> }
    ) {
        AivTheme(darkTheme = false) {
            Box(modifier = Modifier.fillMaxSize()) {
                EditorScreen(
                    uri = uri,
                    busy = false,
                    marked = false,
                    marking = false,
                    hasMark = false,
                    onMark = {},
                    onMarkSetup = {},
                    resize = Resize.Plan(Resize.Mode.LONG, Resize.DEFAULT_PX),
                    // ⚠️ Spento: un ridimensionamento che rimpicciolisce accenderebbe 'Salva' a
                    // immagine intonsa, e il caso suo vive in `RidimensionaTest`.
                    saved = Resize.NONE,
                    resizing = false,
                    onResize = {},
                    onResizeDefault = {},
                    onSave = onSave,
                    onBack = {}
                )
            }
        }
    }

    /**
     * Un PNG **largo il doppio della sua altezza**, perché il verso di partenza sia 'Orizzontale'.
     *
     * ⚠️ **Il quadrato non serve qui**: conta come verticale e girato viene identico, quindi una
     * prova che misura una rotazione resterebbe verde con qualunque codice.
     */
    private fun largo(): Uri {
        val file = File(app.cacheDir, "casa-largo.png")
        if (!file.exists()) {
            val mappa = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
            mappa.eraseColor(Color.WHITE)
            file.outputStream().use { mappa.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return Uri.fromFile(file)
    }

    /** Lo stesso PNG largo, ma rosso pieno: vedi la prova della lente. */
    private fun rosso(): Uri {
        val file = File(app.cacheDir, "casa-rosso.png")
        if (!file.exists()) {
            val mappa = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
            mappa.eraseColor(Color.RED)
            file.outputStream().use { mappa.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return Uri.fromFile(file)
    }
}

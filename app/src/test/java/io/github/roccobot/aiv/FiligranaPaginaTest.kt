package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Il banco della **pagina 'Filigrana'** dalla `4.99`, nato con le note A e D del giro della `4.98`.
 *
 * ⚠️ **Che cosa misura**: l'ordine della pagina (il paragrafo in cima e senza interruttore dalla
 * `5.01`, l'avviso sul senza perdita in fondo, niente secondo titolo), la regola con cui
 * l'anteprima sceglie fondo, opacità e misura, la luminanza dell'inchiostro pesata sull'opacità, e
 * la squadretta di un posto dipinta una volta sola sulla piega. Con le note su `4.99-01` e
 * `5.00-01`, anche il paragrafo largo tutta la pagina, 'Posizione' dentro il riquadro, il blocco
 * centrato con la nota sotto e su una riga, il tondo del centro con lo stelo, e la riga
 * dell'editor con 'Imposta' e 'app:'. ⚠️ **Che cosa non vede**: come appare la firma vera
 * nell'anteprima sul telefono, che dipende dal file scelto; la voce di collaudo la chiede.
 *
 * ⚠️ **Usa la regola `v2`** (`Rules.md` di AIV, voce sul velo d'aiuto): le prove nuove non usano la
 * regola deprecata.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "it-w400dp-h1400dp")
class FiligranaPaginaTest {

    @get:Rule
    val banco = createComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun testo(id: Int) = app.getString(id)

    /**
     * Monta la pagina col rientro che ha nell'app.
     *
     * ⚠️ **Il rientro serve dalla `5.00`**: le righe delle impostazioni si allargano oltre il
     * rientro della pagina (`bordo`), e senza di lui quello che sporge cadrebbe fuori dalla finestra,
     * dove una cattura lo taglia.
     */
    private fun monta(settings: Settings = Settings()) {
        banco.setContent {
            AivTheme(darkTheme = false) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = PAGE_SIDE)) {
                    MarkPage(settings = settings, onChange = {})
                }
            }
        }
        banco.waitForIdle()
    }

    /**
     * **Caso 1: il paragrafo apre la pagina, senza interruttore, e l'avviso la chiude.**
     *
     * ⚠️ Dalla `5.01` è la sua nota su `5.00-01` (*togliamo del tutto l'interruttore in alto*): la
     * pagina comincia col paragrafo, poi il file con 'Seleziona', poi il resto, e l'avviso sul senza
     * perdita come ultimo elemento. Nella pagina non c'è più niente che si accenda o si spenga.
     */
    @Test
    fun `il paragrafo in cima, l'avviso in fondo, nessun interruttore`() {
        monta()
        val paragrafo = banco.onNodeWithText(testo(R.string.settings_mark_on_desc)).getUnclippedBoundsInRoot()
        val seleziona = banco.onNodeWithText(testo(R.string.settings_mark_pick)).getUnclippedBoundsInRoot()
        val alpha = banco.onNodeWithText(testo(R.string.settings_mark_alpha)).getUnclippedBoundsInRoot()
        val avviso = banco.onNodeWithText(testo(R.string.settings_mark_lossy)).getUnclippedBoundsInRoot()
        assertTrue("il paragrafo sopra 'Seleziona'", paragrafo.top < seleziona.top)
        assertTrue("'Seleziona' sopra 'Opacità'", seleziona.top < alpha.top)
        assertTrue("l'avviso sotto 'Opacità'", alpha.top < avviso.top)
        banco.onAllNodes(isToggleable()).assertCountEquals(0)
        banco.onNodeWithText(testo(R.string.settings_mark_desc)).assertDoesNotExist()
    }

    /**
     * **Caso 2: la squadretta di un posto spento ha la piega chiara quanto i bracci.**
     *
     * ⚠️⚠️ **È LA SUA NOTA D DEL GIRO DELLA `4.98`** (`corners.png`): con due linee dai capi tondi la
     * piega era coperta due volte, e con l'inchiostro spento al 55% veniva più scura. Si confronta il
     * pixel della piega con quello a metà del braccio orizzontale: un tracciato solo li dipinge
     * uguali. Le coordinate sono quelle di [SpotHandle] per l'angolo in alto a sinistra: piega a
     * 22 meno 5 punti dai due lati del bersaglio, braccio lungo 22.
     */
    @Test
    fun `la squadretta spenta non e piu scura sulla piega`() {
        monta(Settings().copy(markSpot = Watermark.Spot.BOTTOM_RIGHT))
        val px = banco.onNodeWithContentDescription(testo(R.string.mark_spot_tl))
            .captureToImage().toPixelMap()
        val d = app.resources.displayMetrics.density
        val piega = (17 * d).toInt()
        val braccio = ((17 + 11) * d).toInt()
        val a = px[piega, piega]
        val b = px[braccio, piega]
        val scarto = maxOf(abs(a.red - b.red), abs(a.green - b.green), abs(a.blue - b.blue))
        assertTrue("piega $a contro braccio $b", scarto < 0.02f)
    }

    /**
     * **Caso 3: il suo esempio, cioè un logo bianco su un grigio chiaro.**
     *
     * ⚠️ *Il grigio di default diventa nero, l'opacità della filigrana diventa 100%, la dimensione
     * diventa 200%* (nota A del giro della `4.98`).
     */
    @Test
    fun `un logo bianco sul grigio chiaro va sul nero, pieno e doppio`() {
        val piano = Watermark.Plan(spot = Watermark.Spot.BOTTOM_LEFT, size = 6, air = 12, alpha = 40)
        val aspetto = previewLook(ink = 1f, surface = 0.85f, plan = piano)
        assertEquals(Ground.BLACK, aspetto.ground)
        assertEquals(1f, aspetto.alpha)
        assertEquals(12, aspetto.size)
    }

    /**
     * **Caso 4: un logo che contrasta già resta sul grigio, e la misura ha un tetto.**
     *
     * ⚠️ Il fondo cambia solo sotto il contrasto minimo: un logo scuro su un grigio chiaro si vede
     * già. La misura raddoppia fino a 50 centesimi, e una misura già oltre resta com'è.
     */
    @Test
    fun `il fondo cambia solo se serve, e la misura ha un tetto`() {
        val piano = Watermark.Plan(spot = Watermark.Spot.CENTRE, size = 30, air = 0, alpha = 100)
        assertEquals(PreviewLook(Ground.SURFACE, 1f, 50), previewLook(0.02f, 0.85f, piano))
        assertEquals(Ground.WHITE, previewLook(0.05f, 0.03f, piano).ground)
        assertEquals(60, previewLook(0.02f, 0.85f, piano.copy(size = 60)).size)
        assertEquals(Ground.SURFACE, previewLook(null, 0.85f, piano).ground)
    }

    /**
     * **Caso 5: la luminanza dell'inchiostro non conta i pixel trasparenti.**
     *
     * ⚠️ Un PNG con un logo bianco su un fondo trasparente: se contassero i pixel trasparenti, che
     * valgono nero, il logo risulterebbe grigio scuro e l'anteprima sceglierebbe il fondo sbagliato.
     */
    @Test
    fun `la luminanza conta il solo inchiostro`() {
        val disegno = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        disegno.eraseColor(Color.TRANSPARENT)
        for (x in 0 until 3) for (y in 0 until 3) disegno.setPixel(x, y, Color.WHITE)
        assertEquals(1f, inkLuminance(disegno)!!, 0.001f)
        disegno.eraseColor(Color.TRANSPARENT)
        assertNull(inkLuminance(disegno))
    }

    /** Il risultato dell'impaginazione di un testo: righe e larghezza che gli è stata data. */
    private fun impaginato(testo: String): TextLayoutResult {
        val esiti = mutableListOf<TextLayoutResult>()
        banco.onNodeWithText(testo, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(esiti) }
        return esiti.first()
    }

    /**
     * **Caso 6: il paragrafo in cima è largo tutta la pagina.**
     *
     * ⚠️ Era la sua nota su `4.99-01` (*il testo deve occupare tutta la larghezza*), e dalla `5.01`
     * resta vera senza l'interruttore. La larghezza che il paragrafo riceve deve essere quella
     * dell'avviso in fondo, che riempie la pagina.
     */
    @Test
    fun `il paragrafo in cima e largo tutta la pagina`() {
        monta()
        val paragrafo = impaginato(testo(R.string.settings_mark_on_desc))
        val pagina = banco.onNodeWithText(testo(R.string.settings_mark_lossy)).fetchSemanticsNode().size.width
        assertEquals(pagina, paragrafo.layoutInput.constraints.maxWidth)
    }

    /**
     * **Caso 7: 'Posizione' vive dentro il riquadro, centrata e in alto.**
     *
     * ⚠️ Sua nota su `4.99-01`: *non al centro, perché lì potrebbe apparire la filigrana, ma spostato
     * verso l'alto*. Il centro della parola deve cadere nel terzo superiore del riquadro.
     */
    @Test
    fun `Posizione e dentro il riquadro, in alto al centro`() {
        monta()
        val riquadro = banco.onNodeWithContentDescription(testo(R.string.settings_mark_preview))
            .getUnclippedBoundsInRoot()
        val parola = banco.onNodeWithText(testo(R.string.settings_mark_where)).getUnclippedBoundsInRoot()
        assertTrue("dentro in orizzontale", parola.left >= riquadro.left && parola.right <= riquadro.right)
        assertTrue("dentro in verticale", parola.top >= riquadro.top && parola.bottom <= riquadro.bottom)
        val centroY = (parola.top + parola.bottom) / 2
        assertTrue("nel terzo superiore", centroY < riquadro.top + (riquadro.bottom - riquadro.top) / 3)
        val scarto = abs(((parola.left + parola.right) - (riquadro.left + riquadro.right)).value / 2)
        assertTrue("centrata, scarto $scarto", scarto < 1f)
    }

    /**
     * **Caso 8: il riquadro è centrato, e la nota è sotto di lui, centrata anche lei.**
     *
     * ⚠️ Sua nota su `5.00-01`: *non mi piace la posizione variabile*, e *mettila al centro*. Fino
     * alla `5.00` il blocco era spostato a sinistra e la nota cambiava posto con la lingua.
     */
    @Test
    fun `il riquadro e centrato e la nota e sotto`() {
        monta()
        val pagina = banco.onRoot().getUnclippedBoundsInRoot()
        val riquadro = banco.onNodeWithContentDescription(testo(R.string.settings_mark_preview))
            .getUnclippedBoundsInRoot()
        val nota = banco.onNodeWithText(testo(R.string.settings_mark_preview_note)).getUnclippedBoundsInRoot()
        val centro = (pagina.left + pagina.right).value / 2
        val scartoRiquadro = abs((riquadro.left + riquadro.right).value / 2 - centro)
        val scartoNota = abs((nota.left + nota.right).value / 2 - centro)
        assertTrue("riquadro centrato, scarto $scartoRiquadro", scartoRiquadro < 1f)
        assertTrue("nota centrata, scarto $scartoNota", scartoNota < 1f)
        assertTrue("la nota sotto il riquadro", nota.top > riquadro.bottom)
    }

    /**
     * **Caso 9: la nota entra in una riga, in italiano e in inglese, anche su un telefono stretto.**
     *
     * ⚠️ Sua nota su `5.00-01`: *carattere abbastanza piccolo da far stare la frase in una sola riga
     * almeno in ITA e ENG*. Le due misure sono a 360 punti, la larghezza dei telefoni più stretti
     * fra quelli comuni, e la sua pagina è a 400.
     */
    @Test
    @Config(qualifiers = "it-w360dp-h1400dp")
    fun `la nota entra in una riga in italiano`() {
        monta()
        assertEquals(1, impaginato(testo(R.string.settings_mark_preview_note)).lineCount)
    }

    @Test
    @Config(qualifiers = "en-w360dp-h1400dp")
    fun `la nota entra in una riga in inglese`() {
        monta()
        assertEquals(1, impaginato(testo(R.string.settings_mark_preview_note)).lineCount)
    }

    /**
     * **Caso 10: il tondo del centro è sotto il riquadro, e il suo stelo sale fino al centro.**
     *
     * ⚠️ Sua nota su `5.00-01`, col mockup: *aggiungi uno stelo al cerchio di selezione per il
     * centro*. Si confronta, poco sotto e poco sopra il centro del riquadro, il pixel sull'asse con
     * uno a lato, dove c'è il solo fondo: sotto devono differire, perché lì passa lo stelo; sopra
     * devono essere uguali, perché lo stelo si ferma al centro.
     */
    @Test
    fun `il tondo del centro e sotto, e lo stelo arriva al centro`() {
        monta()
        val riquadro = banco.onNodeWithContentDescription(testo(R.string.settings_mark_preview))
            .getUnclippedBoundsInRoot()
        val tondo = banco.onNodeWithContentDescription(testo(R.string.mark_spot_c)).getUnclippedBoundsInRoot()
        assertTrue("il tondo sotto il riquadro", tondo.bottom > riquadro.bottom)
        val px = banco.onRoot().captureToImage().toPixelMap()
        val d = app.resources.displayMetrics.density
        val cx = ((riquadro.left + riquadro.right).value / 2 * d).toInt()
        val cy = ((riquadro.top + riquadro.bottom).value / 2 * d).toInt()
        val passo = (8 * d).toInt()
        val lato = (30 * d).toInt()
        assertTrue("lo stelo sotto il centro", px[cx, cy + passo] != px[cx + lato, cy + passo])
        assertEquals("niente stelo sopra il centro", px[cx + lato, cy - passo], px[cx, cy - passo])
    }

    /**
     * **Caso 11: 'Posizione' segue il fondo che l'anteprima sceglie per la firma.**
     *
     * ⚠️ Sua nota su `4.99-01`: *con le stesse regole di contrasto/leggibilità della filigrana*.
     */
    @Test
    fun `l'inchiostro di Posizione segue il fondo`() {
        val pagina = androidx.compose.ui.graphics.Color(0xFF1A1C1B)
        assertEquals(pagina, labelInk(Ground.SURFACE, pagina))
        assertEquals(androidx.compose.ui.graphics.Color.White, labelInk(Ground.BLACK, pagina))
        assertEquals(androidx.compose.ui.graphics.Color.Black, labelInk(Ground.WHITE, pagina))
    }

    /**
     * **Caso 12: 'Imposta' è sulla riga del titolo dell'editor, e l'app ha la sua riga sotto.**
     *
     * ⚠️ Sua nota del giro della `4.99` (*deve stare a destra del titolo 'Editor di immagini'*) e
     * su `5.00-02` (*a sinistra 'app:', a destra il nome dell'app*). Il tasto deve stare a destra del
     * titolo, alla sua altezza, e sopra la spiegazione; sotto la spiegazione, 'app:' comincia dove
     * comincia il titolo e il nome finisce a destra, oltre l'inizio del tasto.
     */
    @Test
    fun `Imposta e sulla riga del titolo, e l'app ha la sua riga`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    EditorChoice(
                        label = testo(R.string.settings_editor),
                        detail = testo(R.string.settings_editor_desc),
                        current = testo(R.string.editor_internal),
                        onChoose = {}
                    )
                }
            }
        }
        banco.waitForIdle()
        val titolo = banco.onNodeWithText(testo(R.string.settings_editor)).getUnclippedBoundsInRoot()
        val tasto = banco.onNodeWithText(testo(R.string.settings_editor_pick)).getUnclippedBoundsInRoot()
        // ⚠️ Gli apici del testo diventano grassetto (vedi `emphasizeSettingsCopy`), quindi il testo
        // che si legge a schermo è quello senza apici.
        val spiegazione = banco.onNodeWithText(emphasizeSettingsCopy(testo(R.string.settings_editor_desc)).text)
            .getUnclippedBoundsInRoot()
        val etichetta = banco.onNodeWithText(testo(R.string.settings_editor_app)).getUnclippedBoundsInRoot()
        val nome = banco.onNodeWithText(testo(R.string.editor_internal)).getUnclippedBoundsInRoot()
        assertTrue("il tasto a destra del titolo", tasto.left >= titolo.right)
        val scarto = abs(((tasto.top + tasto.bottom) - (titolo.top + titolo.bottom)).value / 2)
        assertTrue("il tasto alla quota del titolo, scarto $scarto", scarto < 1f)
        assertTrue("il tasto sopra la spiegazione", tasto.bottom <= spiegazione.top)
        assertTrue("'app:' sotto la spiegazione", etichetta.top >= spiegazione.bottom)
        assertEquals(titolo.left.value, etichetta.left.value, 0.5f)
        assertTrue("il nome a destra", nome.right > tasto.left)
    }
}

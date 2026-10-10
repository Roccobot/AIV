package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
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
import java.text.BreakIterator
import kotlin.math.abs

/**
 * Il banco della **pagina 'Filigrana'** dalla `4.99`, nato con le note A e D del giro della `4.98`.
 *
 * ⚠️ **Che cosa misura**: l'ordine della pagina (l'interruttore 'Attiva' in cima, l'avviso sul
 * senza perdita in fondo, niente secondo titolo), la regola con cui l'anteprima sceglie fondo,
 * opacità e misura, la luminanza dell'inchiostro pesata sull'opacità, e la squadretta di un posto
 * dipinta una volta sola sulla piega. Dalla `5.00`, con le note su `4.99-01`, anche la spiegazione
 * di 'Attiva' larga tutta la pagina, 'Posizione' dentro il riquadro, la nota accanto o sotto senza
 * parole spezzate, e 'Imposta app' sulla riga del titolo dell'editor. ⚠️ **Che cosa non vede**:
 * come appare la firma vera nell'anteprima sul telefono, che dipende dal file scelto; la voce di
 * collaudo la chiede.
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
     * ⚠️ **Il rientro serve dalla `5.00`**: il blocco del riquadro si sposta a sinistra oltre il
     * rientro, e senza di lui la squadretta sinistra cadrebbe fuori dalla finestra, dove una cattura
     * la taglia.
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
     * **Caso 1: l'interruttore apre la pagina e l'avviso la chiude, senza un secondo titolo.**
     *
     * ⚠️ È l'ordine del suo mockup (nota A del giro della `4.98`): 'Attiva', poi il file con
     * 'Seleziona', poi il resto, e l'avviso sul senza perdita come ultimo elemento. Il mini-paragrafo
     * che stava sotto il titolo non deve esserci più.
     */
    @Test
    fun `Attiva in cima, l'avviso in fondo, niente secondo titolo`() {
        monta()
        val attiva = banco.onNodeWithText(testo(R.string.settings_mark_on)).getUnclippedBoundsInRoot()
        val seleziona = banco.onNodeWithText(testo(R.string.settings_mark_pick)).getUnclippedBoundsInRoot()
        val alpha = banco.onNodeWithText(testo(R.string.settings_mark_alpha)).getUnclippedBoundsInRoot()
        val avviso = banco.onNodeWithText(testo(R.string.settings_mark_lossy)).getUnclippedBoundsInRoot()
        assertTrue("'Attiva' sopra 'Seleziona'", attiva.top < seleziona.top)
        assertTrue("'Seleziona' sopra 'Opacità'", seleziona.top < alpha.top)
        assertTrue("l'avviso sotto 'Opacità'", alpha.top < avviso.top)
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
     * **Caso 6: la spiegazione di 'Attiva' è larga tutta la pagina.**
     *
     * ⚠️ È la sua nota su `4.99-01` (*il testo deve occupare tutta la larghezza: l'interruttore sta
     * sopra, allineato ad 'Attiva'*). La larghezza che la spiegazione riceve deve essere quella
     * dell'avviso in fondo, che riempie la pagina: con l'interruttore accanto sarebbe più stretta
     * della sua larghezza e dei 16 punti d'aria.
     */
    @Test
    fun `la spiegazione di Attiva e larga tutta la pagina`() {
        monta()
        val spiegazione = impaginato(testo(R.string.settings_mark_on_desc))
        val pagina = banco.onNodeWithText(testo(R.string.settings_mark_lossy)).fetchSemanticsNode().size.width
        assertEquals(pagina, spiegazione.layoutInput.constraints.maxWidth)
        val titolo = banco.onNodeWithText(testo(R.string.settings_mark_on), useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val sotto = banco.onNodeWithText(testo(R.string.settings_mark_on_desc), useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue("la spiegazione sotto 'Attiva'", titolo.bottom <= sotto.top)
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

    /** Se la nota va a capo solo dove si può, cioè mai a metà di una parola. */
    private fun controllaGliACapo() {
        val nota = testo(R.string.settings_mark_preview_note)
        val righe = impaginato(nota)
        val confini = BreakIterator.getLineInstance()
        confini.setText(nota)
        for (riga in 0 until righe.lineCount - 1) {
            val fine = righe.getLineEnd(riga)
            assertTrue("la riga $riga finisce a metà di una parola, al carattere $fine", confini.isBoundary(fine))
        }
    }

    /**
     * **Caso 8: con lo spazio, la nota va accanto al riquadro e senza parole spezzate.**
     *
     * ⚠️ Sua nota su `4.99-01`: *non andare a capo spezzando le parole*.
     */
    @Test
    fun `con lo spazio la nota va accanto, senza parole spezzate`() {
        monta()
        val riquadro = banco.onNodeWithContentDescription(testo(R.string.settings_mark_preview))
            .getUnclippedBoundsInRoot()
        val nota = banco.onNodeWithText(testo(R.string.settings_mark_preview_note)).getUnclippedBoundsInRoot()
        assertTrue("la nota a destra del riquadro", nota.left > riquadro.right)
        controllaGliACapo()
    }

    /**
     * **Caso 9: senza lo spazio, la nota va sotto il riquadro e resta senza parole spezzate.**
     *
     * ⚠️ A 280 punti la parola più larga non entra accanto al riquadro: tenendo la nota lì, Compose
     * spezzerebbe 'ingrandita' a metà.
     */
    @Test
    @Config(qualifiers = "it-w280dp-h1400dp")
    fun `senza lo spazio la nota va sotto, senza parole spezzate`() {
        monta()
        val riquadro = banco.onNodeWithContentDescription(testo(R.string.settings_mark_preview))
            .getUnclippedBoundsInRoot()
        val nota = banco.onNodeWithText(testo(R.string.settings_mark_preview_note)).getUnclippedBoundsInRoot()
        assertTrue("la nota sotto il riquadro", nota.top > riquadro.bottom)
        controllaGliACapo()
    }

    /**
     * **Caso 10: la parola più larga si cerca fra i punti in cui si può andare a capo.**
     *
     * ⚠️ In cinese gli spazi non ci sono: diviso sugli spazi, il testo sarebbe una parola sola, e la
     * nota finirebbe sempre sotto il riquadro.
     */
    @Test
    fun `la parola piu larga segue i punti di a capo`() {
        assertEquals(10, longestWord("Anteprima ingrandita a qualità bozza:") { it.length })
        assertEquals(12, longestWord("verifica nell'output.") { it.length })
        assertTrue(longestWord("放大的草稿质量预览") { it.length } <= 2)
        assertTrue(noteFits(longestWord = 40, room = 40))
        assertTrue(!noteFits(longestWord = 41, room = 40))
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
     * **Caso 12: 'Imposta app' è sulla riga del titolo dell'editor.**
     *
     * ⚠️ Sua nota del giro della `4.99`: *deve stare a destra del titolo 'Editor di immagini'*. Il
     * tasto deve stare a destra del titolo, alla sua altezza, e sopra la spiegazione.
     */
    @Test
    fun `Imposta app e sulla riga del titolo dell'editor`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    EditorChoice(
                        label = testo(R.string.settings_editor),
                        detail = testo(R.string.settings_editor_desc),
                        current = testo(R.string.settings_editor_none),
                        onChoose = {}
                    )
                }
            }
        }
        banco.waitForIdle()
        val titolo = banco.onNodeWithText(testo(R.string.settings_editor)).getUnclippedBoundsInRoot()
        val tasto = banco.onNodeWithText(testo(R.string.settings_editor_pick)).getUnclippedBoundsInRoot()
        val spiegazione = banco.onNodeWithText(testo(R.string.settings_editor_desc)).getUnclippedBoundsInRoot()
        assertTrue("il tasto a destra del titolo", tasto.left >= titolo.right)
        val scarto = abs(((tasto.top + tasto.bottom) - (titolo.top + titolo.bottom)).value / 2)
        assertTrue("il tasto alla quota del titolo, scarto $scarto", scarto < 1f)
        assertTrue("il tasto sopra la spiegazione", tasto.bottom <= spiegazione.top)
    }
}

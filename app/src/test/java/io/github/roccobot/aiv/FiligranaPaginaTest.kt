package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
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
 * ⚠️ **Che cosa misura**: l'ordine della pagina (l'interruttore 'Attiva' in cima, l'avviso sul
 * senza perdita in fondo, niente secondo titolo), la regola con cui l'anteprima sceglie fondo,
 * opacità e misura, la luminanza dell'inchiostro pesata sull'opacità, e la squadretta di un posto
 * dipinta una volta sola sulla piega. ⚠️ **Che cosa non vede**: come appare la firma vera
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

    private fun monta(settings: Settings = Settings()) {
        banco.setContent {
            AivTheme(darkTheme = false) {
                Column(modifier = Modifier.fillMaxWidth()) {
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
}

package io.github.roccobot.aiv

import androidx.compose.ui.geometry.Rect
import android.view.View
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.DpRect
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The info bar and the picture under it, on the real viewer (3.60, mockups `Viewer_H` and
 * `Viewer_NOPE`).
 *
 * ⚠️⚠️ It mounts `ViewerScreen` and not the pure rule: the rule lives in `Fit.kt` and has its
 * own bench (`RiposoTest`); here the question is whether the bar really reports its height and
 * its text, and whether the canvas really uses them. Both are wiring that compiles just as well
 * when it does nothing.
 * ⚠️ `@GraphicsMode(NATIVE)` because the one-row choice measures text: with the default
 * graphics a text measures a fraction of what it measures on a phone.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BarraInfoTest {

    @get:Rule
    val banco = createComposeRule()

    /** A tall picture, so on both screens the height is what limits it. */
    private fun immagine() = LoadedImage(
        bitmap = ImageBitmap(900, 2000).also {
            Canvas(it).drawRect(Rect(0f, 0f, 900f, 2000f), Paint().apply { color = Color.Red })
        },
        mimeType = "image/jpeg",
        byteSize = 2_100_000,
        pixelWidth = 900,
        pixelHeight = 2000,
        sampled = false,
        displayName = NOME
    )

    /** The view that hosts the scene, to hand it the system bars' insets. */
    private var vista: View? = null

    private fun monta(avanza: Boolean = true) {
        banco.setContent {
            vista = LocalView.current
            AivTheme(darkTheme = false) {
                ViewerScreen(
                    state = ViewerState.Ready(immagine()),
                    settings = Settings(),
                    source = null,
                    folder = null,
                    onStep = {},
                    onSettings = {},
                    onRetry = {},
                    onClipStarted = {},
                    onFileChanged = {},
                    onFileAdded = {},
                    onEdit = {},
                    onEditWith = {},
                    onInfoBar = { _, _ -> },
                    inBin = false
                )
            }
        }
        if (avanza) banco.waitForIdle()
    }

    private fun rect(r: DpRect): Rect = Rect(r.left.value, r.top.value, r.right.value, r.bottom.value)

    // ⚠️ `substring`: the name goes through `fitName`, which puts invisible characters in it.
    private fun nome() =
        rect(banco.onNodeWithText("foto", substring = true, useUnmergedTree = true).getBoundsInRoot())
    private fun dati() =
        rect(banco.onNodeWithText("JPEG", substring = true, useUnmergedTree = true).getBoundsInRoot())

    /**
     * Where the data text really is: its box is as wide as the widest data, and the text is
     * aligned to its right end, so the ink starts at the line's left edge inside the box.
     */
    private fun inchiostroDati(): Rect {
        val nodo = banco.onNodeWithText("JPEG", substring = true, useUnmergedTree = true)
        val box = rect(nodo.getBoundsInRoot())
        val layout = mutableListOf<TextLayoutResult>()
        nodo.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layout)
        return Rect(box.left + layout.first().getLineLeft(0), box.top, box.right, box.bottom)
    }
    /**
     * Where the picture is drawn, read from the pixels: it is red, and pink under the veil of the
     * bar, while the checkerboard and the bar are grey (equal channels).
     * ⚠️ The threshold is low because the double-tap hint of the first picture veils the whole
     * scene: under it and under the bar the red is a few hundredths above the grey.
     *
     * ⚠️ From the pixels and not from the node: the scale lives in a `graphicsLayer`, and the
     * node's bounds are the bitmap's before scaling.
     * ⚠️ The scene is in dp and the bench density is 1, so pixels and dp are the same number.
     */
    private fun figura(): Rect {
        val mappa = banco.onRoot().captureToImage().toPixelMap()
        var l = Int.MAX_VALUE; var t = Int.MAX_VALUE; var r = -1; var b = -1
        for (y in 0 until mappa.height) for (x in 0 until mappa.width) {
            val c = mappa[x, y]
            if (c.red - c.green > 0.03f && c.red - c.blue > 0.03f) {
                if (x < l) l = x
                if (y < t) t = y
                if (x > r) r = x
                if (y > b) b = y
            }
        }
        return Rect(l.toFloat(), t.toFloat(), r + 1f, b + 1f)
    }

    /**
     * **In verticale la barra ha due righe, e l'immagine comincia dove finisce la barra.**
     *
     * ⚠️ È il difetto di `Viewer_NOPE`: fino alla `3.54` l'immagine si adattava a tutto lo schermo,
     * e la sua parte alta restava sotto la barra.
     */
    @Test
    @Config(qualifiers = "w360dp-h740dp")
    fun `in verticale due righe e l'immagine sotto la barra`() {
        monta()
        val nome = nome()
        val dati = dati()
        assertTrue("Il nome non è sopra i dati: $nome / $dati", nome.bottom <= dati.top + 1f)
        val figura = figura()
        assertTrue("L'immagine entra sotto la barra: $figura / $dati", figura.top >= dati.bottom)
        assertTrue("L'immagine non arriva in fondo: $figura", figura.bottom >= 739f)
    }

    /**
     * **In orizzontale la barra ha una riga sola, e l'immagine va sotto la barra al massimo del 5%
     * dell'altezza, senza toccare il testo.**
     */
    @Test
    @Config(qualifiers = "w740dp-h360dp")
    fun `in orizzontale una riga e la tolleranza senza testo`() {
        monta()
        val nome = nome()
        val dati = dati()
        assertTrue(
            "Nome e dati non sono sulla stessa riga: $nome / $dati",
            nome.top < dati.center.y && dati.center.y < nome.bottom
        )
        val figura = figura()
        val barra = dati.bottom
        assertTrue("L'immagine non usa la tolleranza: $figura / $barra", figura.top < barra)
        assertTrue("L'immagine va sotto la barra oltre il 5%: $figura", barra - figura.top <= 360f * 0.05f + 3f)
        for (testo in listOf(nome, inchiostroDati())) {
            val sovrapposti = figura.left < testo.right && testo.left < figura.right
            assertTrue("L'immagine passa sotto il testo $testo: $figura", !sovrapposti)
        }
        assertTrue("L'immagine non arriva in fondo: $figura", figura.bottom >= 359f)
    }

    /**
     * **All'apertura l'immagine compare già nella misura finale, senza restringersi** (voce
     * `3.60-02` non approvata: *si apre ingrandito e si restringe con un'animazione*).
     *
     * ⚠️ L'orologio è fermo e si avanza un fotogramma per volta: a corsa finita l'immagine era
     * giusta anche col difetto, che viveva tutto nei primi fotogrammi.
     */
    @Test
    @Config(qualifiers = "w360dp-h740dp")
    fun `all'apertura l'immagine nasce nella misura finale`() {
        banco.mainClock.autoAdvance = false
        monta(avanza = false)
        val visti = mutableListOf<Rect>()
        repeat(40) {
            banco.mainClock.advanceTimeByFrame()
            val f = figura()
            if (f.right > f.left) visti += f
        }
        banco.mainClock.autoAdvance = true
        banco.waitForIdle()
        val finale = figura()
        assertTrue("L'immagine non è mai comparsa", visti.isNotEmpty())
        for (f in visti) {
            assertTrue("Un fotogramma con l'immagine fuori misura: $f contro $finale", kotlin.math.abs(f.top - finale.top) <= 1f && kotlin.math.abs(f.height - finale.height) <= 1f)
        }
    }

    /**
     * **La riga unica è alta quanto il carattere, senza l'interlinea di Material** (voce
     * `3.60-01` non approvata: *devi anche assottigliare l'overlay*). Il corpo dei dati è 14 sp
     * con una riga da 20: tagliata, la riga resta sotto i 18.
     */
    @Test
    @Config(qualifiers = "w740dp-h360dp")
    fun `la riga unica e sottile`() {
        monta()
        val dati = dati()
        assertTrue("La riga dei dati è alta ${dati.height}", dati.height < 18f)
    }

    /**
     * **La barra in cima non prende lo spazio della barra di navigazione in basso** (voce
     * `3.70-01` non approvata: *TUTTE le barre info devono essere più sottili*). Fino alla `3.70`
     * la barra aggiungeva lo spazio di sistema dei quattro lati, e sotto il testo restava una
     * fascia vuota alta quanto i gesti di sistema.
     *
     * ⚠️ Il banco non ha barre di sistema: lo spazio della barra di navigazione si consegna a mano
     * alla vista della scena, come farebbe Android.
     */
    @Test
    @Config(qualifiers = "w360dp-h740dp")
    fun `la barra in cima non prende lo spazio della navigazione`() {
        monta()
        banco.runOnUiThread {
            val spazi = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, 48))
                .setVisible(WindowInsetsCompat.Type.navigationBars(), true)
                .build()
            ViewCompat.dispatchApplyWindowInsets(vista!!, spazi)
        }
        banco.waitForIdle()
        val dati = dati()
        val figura = figura()
        assertTrue("Sotto i dati resta una fascia vuota: $dati / $figura", figura.top - dati.bottom <= 6f)
    }

    /**
     * **Sul tablet in orizzontale l'immagine alta non entra nella barra di stato** (voce
     * `3.70-01`: *su tablet in orizzontale le immagini alte vanno ancora a finire sotto l'overlay
     * info*). Là la barra di stato resta, e il 5% dell'altezza arrivava fino all'orologio.
     */
    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun `tablet in orizzontale l'immagine non entra nella barra di stato`() {
        monta()
        banco.runOnUiThread {
            val spazi = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, STATO, 0, 0))
                .setVisible(WindowInsetsCompat.Type.statusBars(), true)
                .build()
            ViewCompat.dispatchApplyWindowInsets(vista!!, spazi)
        }
        banco.waitForIdle()
        val figura = figura()
        assertTrue("L'immagine entra nella barra di stato: $figura", figura.top >= STATO - 1f)
    }

    /**
     * **Il doppio tocco ingrandisce intorno al centro della fascia libera** (voce `3.71-01`: *al
     * doppio tocco lo zoom fa un percorso strano*). Fino alla `3.71` a ogni fotogramma lo
     * spostamento era moltiplicato di nuovo per il rapporto fra la scala e quella di partenza,
     * cioè cresceva su sé stesso, e l'immagine scivolava fino al bordo della fascia: a fine corsa
     * la sua cima era appoggiata sotto la barra invece di coprire lo schermo.
     */
    @Test
    @Config(qualifiers = "w360dp-h740dp")
    fun `il doppio tocco ingrandisce sul centro della fascia`() {
        monta()
        // ⚠️ La prima immagine ha il velo che spiega il doppio tocco: il primo tocco lo chiude.
        banco.onNodeWithText("Quick zoom", substring = true).performClick()
        // ⚠️ Il velo se ne va quando l'archivio ha scritto, in un'altra coroutine: si aspetta lui.
        banco.waitUntil(5_000) {
            banco.onAllNodesWithText("Quick zoom", substring = true).fetchSemanticsNodes().isEmpty()
        }
        banco.onRoot().performTouchInput { doubleClick(center) }
        banco.waitForIdle()
        val figura = figura()
        assertTrue("L'immagine ingrandita è scivolata sotto la barra: $figura", figura.top <= 1f)
    }

    private companion object {
        const val STATO = 24
        const val NOME = "foto.jpg"
    }
}

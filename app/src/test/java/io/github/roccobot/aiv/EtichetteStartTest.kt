package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * The Start menu's labels and the hint's copy of the round key, read from the pixels (items
 * `4.33-07` A and `4.33-05` on the 4.33 round).
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️ `@GraphicsMode(NATIVE)` in a class of its own, because it reads pixels and measures text
 * (`Rules.md`, § 'Quando si scrive una prova, e quando no').
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EtichetteStartTest {

    @get:Rule
    val banco = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<android.app.Application>()

    private fun voce(id: Int) = app.getString(id)

    /**
     * **A long label is drawn whole, past the round key's circle** (item `4.33-07` A: *le etichette
     * dei comandi sono come tagliate da una maschera*): until 4.33 the key clipped to a circle, and
     * the label's two ends, wider than the circle's chord at that height, were cut.
     *
     * ⚠️ It counts the label's ink outside the circle: with the clip there is none.
     * ⚠️ **In the home, where 'Impostazioni' is the middle column**: in a folder's 2x2 it is on the
     * panel's outer side, and the scan read the panel's rounded corner as ink.
     * ⚠️⚠️ **IN GERMAN, AND IT IS MEASURED**: the bench's font draws 'Impostazioni' narrower than the
     * circle's chord, so the Italian label had no ink outside it with the clip or without, while on
     * his phone it was cut. 'Einstellungen' gives 39 pixels without the clip and none with it.
     */
    @Test
    // ⚠️ A real phone's density: at density 1 the label's ends outside the circle are a pixel or two,
    // and the circle's antialiased rim alone passed the count. German: see above.
    @Config(shadows = [ArchivioAperto::class], qualifiers = "de-xxhdpi")
    fun `un'etichetta lunga esce intera dal tondo del tasto`() {
        runBlocking { Hint.COLUMNS.remember(app); Hint.BIN_EMPTY.remember(app) }
        banco.setContent { Home() }
        banco.waitForIdle()
        banco.onNodeWithContentDescription(voce(R.string.hub_open)).performClick()
        banco.waitForIdle()
        val tasto = banco.onNodeWithContentDescription(voce(R.string.hub_settings)).fetchSemanticsNode().boundsInRoot
        val testo = banco.onAllNodesWithText(voce(R.string.hub_settings), useUnmergedTree = true)
            .fetchSemanticsNodes().single().boundsInRoot
        val dp = app.resources.displayMetrics.density
        val mappa = banco.onRoot().captureToImage().toPixelMap()
        // ⚠️ The panel's fill, read at the key's top: above the glyph, where nothing is drawn.
        val fondo = mappa[tasto.center.x.toInt(), (tasto.top + 1).toInt()]
        val r = tasto.width / 2f
        var fuori = 0
        for (y in testo.top.toInt() until testo.bottom.toInt()) {
            val dy = y + 0.5f - tasto.center.y
            if (abs(dy) >= r) continue
            val corda = sqrt(r * r - dy * dy)
            // ⚠️ The text node reports the key's width, not the label's: the label is wider and
            // overflows it, so the scan takes the whole column.
            for (x in (tasto.left - 4 * dp).toInt() until (tasto.right + 4 * dp).toInt()) {
                if (abs(x + 0.5f - tasto.center.x) <= corda + dp) continue
                val c = mappa[x, y]
                if (abs(c.red - fondo.red) + abs(c.green - fondo.green) + abs(c.blue - fondo.blue) > 0.3f) fuori++
            }
        }
        assertTrue("L'etichetta ha $fuori pixel d'inchiostro fuori dal tondo del tasto: è tagliata", fuori >= INCHIOSTRO)
    }

    /**
     * **On the hint's open Start menu the copy of the round key is a yellow disc** (item `4.33-05`:
     * *riproducilo con un tondo dello stesso giallo che vedi nel mio mockup*). Until 4.33 it was the
     * panel's orange, and on the panel it did not read as a key.
     */
    @Test
    @Config(shadows = [ArchivioAperto::class])
    fun `sul velo la copia del tondo e gialla`() = copiaGialla(PillFill.SOLID)

    /**
     * **And it stays a flat yellow with the glass** (item `4.34-02`: *il tondo lo voglio di colore
     * #ffda3c. Colore finto, come quello arancione del velo*): in 4.34 the copy took the look chosen
     * for the real buttons, and on his phone, with the glass, the yellow came out veiled.
     */
    @Test
    @Config(shadows = [ArchivioAperto::class])
    fun `col vetro la copia del tondo resta gialla piena`() = copiaGialla(PillFill.GLASS)

    private fun copiaGialla(fill: PillFill) {
        runBlocking { Hint.COLUMNS.forget(app) }
        banco.setContent { Home(fill) }
        // ⚠️ As in `MenuInferioreTest`: the hint is read on another thread.
        /*
         * ⚠️⚠️ **THE HINT ONCE NEVER CAME, ON GITHUB ONLY, AND THE CAUSE IS NOT KNOWN** (release of
         * 4.36, glass variant: 5 s without the hint, while the solid one found it in 0,3 s; the
         * same commit was green on the next run). The best guess, not proved, is a state left in
         * the preferences' store by another class: the store is one per process, and on GitHub the
         * classes run in another order. So a timeout reports the store and the texts on screen,
         * which is the data to close the question the next time it happens.
         */
        try {
            banco.waitUntil(VELO_MS) { banco.onAllNodesWithText(voce(R.string.corner_hint)).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
            val p = runBlocking { storedPreferences(app) }
            val testi = banco.onAllNodes(androidx.compose.ui.test.hasText("", substring = true), useUnmergedTree = true)
                .fetchSemanticsNodes().mapNotNull { n -> n.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.Text) { null }?.joinToString() }
            throw AssertionError("Il velo non è comparso. archivio=${p.asMap()} vista=${SettingsStore.read(p).folderView} testi=$testi", e)
        }
        banco.waitForIdle()
        val dp = app.resources.displayMetrics.density
        val mappa = banco.onRoot().captureToImage().toPixelMap()
        val tondi = banco.onAllNodesWithContentDescription(voce(R.string.hub_open)).fetchSemanticsNodes().map { it.boundsInRoot }
        // ⚠️ Beside the glyph and inside the disc: the copy lies over the real key, so one of the
        // two nodes is enough, and it is the copy that is drawn on top.
        val gialla = tondi.any { b ->
            val c = mappa[(b.center.x - 15f * dp).toInt(), b.center.y.toInt()]
            vicino(c, HINT_KEY)
        }
        assertTrue("La copia del tondo sul menu Start del velo non è gialla", gialla)
    }

    /** The home with the Start menu, right-handed. */
    @Composable
    private fun Home(fill: PillFill = PillFill.SOLID) {
        AivTheme(darkTheme = false) {
            CompositionLocalProvider(
                LocalPillLook provides PillLook(PhonePill.SLIDE, corner = true, fill = fill),
                LocalPadLook provides PadLook(hand = Hand.RIGHT)
            ) {
                Box(modifier = Modifier.fillMaxSize()) { Casa(CARTELLE_VELO) }
            }
        }
    }

    private fun vicino(a: Color, b: Color): Boolean =
        abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue) < 0.08f
}

/** How many pixels of ink, outside the key's circle, say that the label is drawn there. */
private const val INCHIOSTRO = 20

/** A few folders, so the home shows its grid and the hint. */
private val CARTELLE_VELO = (1..4).map {
    Folder.Bucket(id = it.toLong(), name = "Cartella $it", pictures = 2, clips = 0, cover = null, path = "/storage/emulated/0/F$it")
}

package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * **Nel Feed la selezione ha l'ombra sopra la scheda, come nella griglia di una cartella**
 * (scelta F1 sulla voce G5 della revisione Setaccio, rilievo D18: `docs/Folders.md` vuole la
 * selezione del Feed uguale a quella della griglia, e le celle ricevevano `shade = null`).
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️ Monta la casa vera nel Feed, con due immagini: sotto l'ultima riga la griglia è vuota, e
 * l'ombra si misura lì, nel margine di sinistra e al centro, pochi pixel sopra il bordo della
 * scheda, come fa `OmbraGrigliaTest` sulla griglia di una cartella.
 * ⚠️ `@GraphicsMode(NATIVE)` in una classe sua, perché legge i pixel.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(shadows = [ArchivioAperto::class], qualifiers = "it-w393dp-h873dp")
class OmbraFeedTest {

    @get:Rule
    val banco = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /** I veli della casa e quello del Feed si segnano come visti prima di montarla. */
    @Before
    fun veliVisti() {
        onboardingGiaVisto()
        runBlocking { Hint.FEED.remember(app) }
    }

    @Test
    fun `nel Feed l'ombra della selezione arriva ai bordi e fino alla scheda`() {
        val righe = listOf(
            Feed.Row(Uri.parse("content://media/external/images/media/1"), 1, MS_OTTOBRE, 0),
            Feed.Row(Uri.parse("content://media/external/images/media/2"), 2, MS_SETTEMBRE, 0)
        )
        val pagina = Feed.order(righe, FeedDate.TAKEN, java.time.ZoneOffset.UTC)
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) { Casa(view = FolderView.FEED, feed = pagina) }
            }
        }
        banco.waitForIdle()
        banco.onNodeWithContentDescription(app.getString(R.string.grid_item, 1, 2))
            .performTouchInput { longClick() }
        banco.mainClock.advanceTimeBy(2_000)
        banco.waitForIdle()

        val mappa = banco.onRoot().captureToImage().toPixelMap()
        // Il bordo alto della scheda: dal suo tasto 'Sposta' in su, l'ultima riga del suo colore.
        val tasto = banco.onNodeWithText(app.getString(R.string.pick_move)).fetchSemanticsNode().boundsInRoot
        val x = mappa.width / 2
        val foglio = mappa[x, tasto.top.roundToInt() - 2]
        var bordo = tasto.top.roundToInt() - 2
        while (bordo > 0 && simile(mappa, x, bordo - 1, foglio)) bordo--
        val sopra = bordo - 3
        val pagina0 = mappa[2, 2 * mappa.height / 5]
        fun buio(px: Int) = (pagina0.red - mappa[px, sopra].red).coerceAtLeast(0f)

        assertTrue("al centro, sopra la scheda, l'ombra doveva esserci: ${buio(x)}", buio(x) > SHADE_MAX * 0.6f)
        assertTrue("nel margine di sinistra l'ombra doveva esserci: ${buio(2)}", buio(2) > SHADE_MAX * 0.6f)
        assertEquals("ai bordi e al centro l'ombra doveva essere la stessa", buio(x), buio(2), 0.02f)
    }

    private fun simile(m: PixelMap, x: Int, y: Int, c: Color) =
        abs(m[x, y].red - c.red) < 0.01f && abs(m[x, y].green - c.green) < 0.01f &&
            abs(m[x, y].blue - c.blue) < 0.01f
}

/** Due date a mezzogiorno, lontane dalla mezzanotte di ogni fuso del banco. */
private const val MS_OTTOBRE = 1_791_028_800_000L
private const val MS_SETTEMBRE = 1_788_264_000_000L

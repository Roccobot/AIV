package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.fromHtml
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The Feed, from 6.02: every photo and video of the visible folders, newest first, by month (his
 * request of 2026-10-13 and his choices of 2026-10-14).
 *
 * ⚠️ **The order is the real [Feed.order]**, the function [Folder.feed] hands the MediaStore's rows
 * to: the cases build rows as the cursor gives them (taken in milliseconds or `null`, modified in
 * seconds) and read the stream the home draws.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 */
@RunWith(AndroidJUnit4::class)
class FeedTest {
    @get:Rule val scene = createComposeRule()

    private val zone = ZoneOffset.UTC

    private fun ms(y: Int, m: Int, d: Int) = LocalDateTime.of(y, m, d, 12, 0).toInstant(zone).toEpochMilli()

    private fun riga(id: Long, taken: Long?, modified: Long) =
        Feed.Row(Uri.parse("content://media/external/images/media/$id"), id, taken, modified)

    /**
     * **Case 1: with the date taken the newest shot is first, and a file without it falls back on
     * its modification date** (his wording: *usare la data di scatto e ripiegare sulla data di
     * modifica*). The screenshot of March sits between the two photos, not at the bottom.
     * ⚠️ **Counter-tested** with the fallback removed (`row.taken ?: 0`): the screenshot goes last.
     */
    @Test
    fun `con lo scatto il ripiego sulla modifica tiene la cattura al suo posto`() {
        val vecchia = riga(1, ms(2026, 1, 10), ms(2026, 9, 1) / 1000)
        val cattura = riga(2, null, ms(2026, 3, 5) / 1000)
        val nuova = riga(3, ms(2026, 6, 20), ms(2026, 6, 20) / 1000)
        val pagina = Feed.order(listOf(vecchia, cattura, nuova), FeedDate.TAKEN, zone)
        assertEquals(listOf(nuova.uri, cattura.uri, vecchia.uri), pagina.items)
    }

    /**
     * **Case 2: with the modification date only that one counts**: the old photo edited in
     * September comes first.
     */
    @Test
    fun `con la modifica conta solo la modifica`() {
        val vecchia = riga(1, ms(2026, 1, 10), ms(2026, 9, 1) / 1000)
        val nuova = riga(3, ms(2026, 6, 20), ms(2026, 6, 20) / 1000)
        val pagina = Feed.order(listOf(nuova, vecchia), FeedDate.MODIFIED, zone)
        assertEquals(listOf(vecchia.uri, nuova.uri), pagina.items)
    }

    /**
     * **Case 3: a month starts where its first picture is** (his choice C1, a heading for every
     * month), and a taken date of zero counts as missing.
     */
    @Test
    fun `i mesi partono dalla loro prima immagine`() {
        val righe = listOf(
            riga(1, ms(2026, 10, 2), 0),
            riga(2, ms(2026, 10, 1), 0),
            riga(3, ms(2026, 8, 30), 0),
            riga(4, 0, ms(2025, 12, 31) / 1000)
        )
        val pagina = Feed.order(righe, FeedDate.TAKEN, zone)
        assertEquals(
            listOf(Feed.Month(2026, 10, 0), Feed.Month(2026, 8, 2), Feed.Month(2025, 12, 3)),
            pagina.months
        )
    }

    /** **Case 4: two pictures of the same moment keep the higher id first**, reading after reading. */
    @Test
    fun `a parità di data vince l'id più alto`() {
        val stesso = ms(2026, 5, 5)
        val pagina = Feed.order(listOf(riga(7, stesso, 0), riga(9, stesso, 0)), FeedDate.TAKEN, zone)
        assertEquals(listOf(9L, 7L), pagina.items.map { it.lastPathSegment!!.toLong() })
    }

    /**
     * **Case 5: the home shows the Feed with its month headings, and a tap opens that picture of
     * the stream** (the series and the position the viewer receives).
     */
    @Test
    @Config(qualifiers = "it", shadows = [ArchivioAperto::class])
    fun `la casa mostra il Feed coi mesi e il tocco apre l'immagine`() {
        val righe = listOf(riga(1, ms(2026, 10, 2), 0), riga(2, ms(2026, 9, 1), 0))
        val pagina = Feed.order(righe, FeedDate.TAKEN, zone)
        var aperta: Pair<List<Uri>, Int>? = null
        // ⚠️ The veil already seen: otherwise it lies over the grid and takes the tap (case 6).
        runBlocking { Hint.FEED.remember(ApplicationProvider.getApplicationContext()) }
        scene.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Casa(view = FolderView.FEED, feed = pagina, onFeedOpen = { items, at -> aperta = items to at })
                }
            }
        }
        scene.waitForIdle()
        // ⚠️ The headings depend on the time zone of the machine, as on the phone: the dates are at
        // noon, far enough from midnight for every zone of the bench.
        scene.onNodeWithText("Ottobre 2026").assertExists()
        scene.onNodeWithText("Settembre 2026").assertExists()
        val app = ApplicationProvider.getApplicationContext<android.content.Context>()
        scene.onNodeWithContentDescription(app.getString(R.string.grid_item, 2, 2)).performClick()
        assertEquals(pagina.items to 1, aperta)
    }

    /**
     * **Case 6: the first time the Feed shows pictures, its veil explains the date** (his request:
     * *un mini-onboarding ... che spieghi in breve la scelta della data*), and a tap puts it away
     * for good.
     */
    @Test
    @Config(qualifiers = "it", shadows = [ArchivioAperto::class])
    fun `la prima volta il velo del Feed spiega la data`() {
        val app = ApplicationProvider.getApplicationContext<android.content.Context>()
        runBlocking { Hint.FEED.forget(app) }
        val pagina = Feed.order(listOf(riga(1, ms(2026, 10, 2), 0)), FeedDate.TAKEN, zone)
        scene.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) { Casa(view = FolderView.FEED, feed = pagina) }
            }
        }
        scene.waitForIdle()
        // ⚠️ The veil reads its resource as HTML from 6.03 (*feed* in italics): the node holds the
        // text without the tag.
        val velo = AnnotatedString.fromHtml(app.getString(R.string.hint_feed)).text
        // ⚠️ **The view's name is in italics, as he wrote it** (6.03): counter-tested with the
        // resource passed as plain text, where the italics are gone and the tag is shown.
        val scritto = scene.onNodeWithText(velo).fetchSemanticsNode().config[SemanticsProperties.Text].first()
        val corsivo = scritto.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
            .map { scritto.text.substring(it.start, it.end) }
        assertEquals(listOf("feed"), corsivo)
        scene.onNodeWithText(velo).assertExists().performClick()
        scene.waitForIdle()
        scene.onNodeWithText(velo).assertDoesNotExist()
        // ⚠️ The veil is stored by a coroutine that writes the DataStore on its own thread: read at
        // once, on a slower machine the value may still be the old one (6.03, red on the CI bench
        // with the same line that was green on 6.02). The case waits for the write, up to 5 s.
        scene.waitUntil(5_000) { runBlocking { Hint.FEED.flow(app).first() } }
        assertTrue("Il velo non si è archiviato", runBlocking { Hint.FEED.flow(app).first() })
    }

    /**
     * **Case 7: 'Mostra nascoste' lets the hidden folders' pictures into the stream** (his request
     * for 6.03: *`Mostra nascoste` deve far apparire nel feed anche gli elementi di quelle
     * cartelle*). The rows are read by [Folder.feed] and filtered by the real [Feed.shown].
     * ⚠️ **Counter-tested** with the filter that ignores [peeking] (`visible(it.dir)`): the hidden
     * picture stays out with the minute on.
     */
    @Test
    fun `mostra nascoste porta nel Feed le immagini delle cartelle nascoste`() {
        val nascosta = "/storage/emulated/0/Nascosta"
        val selezione = FolderSelection().exclude(listOf(nascosta), recursive = true)
        val righe = listOf(
            riga(1, ms(2026, 10, 2), 0).copy(dir = "/storage/emulated/0/DCIM/Camera"),
            riga(2, ms(2026, 10, 1), 0).copy(dir = nascosta)
        )
        assertEquals(listOf(righe[0]), Feed.shown(righe, selezione, peeking = false))
        assertEquals(righe, Feed.shown(righe, selezione, peeking = true))
    }

    /**
     * **Case 8: turning 'Mostra nascoste' on, and its minute ending, reads the Feed again**: the
     * home asks for a reading whenever the switch changes.
     * ⚠️ **Counter-tested** with `peeking` out of the keys of the reading: one reading only.
     */
    @Test
    @Config(shadows = [ArchivioAperto::class])
    fun `mostra nascoste chiede una nuova lettura del Feed`() {
        val app = ApplicationProvider.getApplicationContext<android.content.Context>()
        runBlocking { Hint.FEED.remember(app) }
        val pagina = Feed.order(listOf(riga(1, ms(2026, 10, 2), 0)), FeedDate.TAKEN, zone)
        var letture = 0
        var guarda by mutableStateOf(false)
        scene.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Casa(view = FolderView.FEED, feed = pagina, peeking = guarda, onFeedRead = { letture++ })
                }
            }
        }
        scene.waitForIdle()
        val prima = letture
        guarda = true
        scene.waitForIdle()
        assertEquals(prima + 1, letture)
        guarda = false
        scene.waitForIdle()
        assertEquals(prima + 2, letture)
    }

    /**
     * **Case 9: the Feed has the multiple selection** (his request for 6.03): a long press chooses
     * a picture without opening it, the sheet of the actions appears, and while choosing a tap
     * chooses another one.
     * ⚠️ **Counter-tested** without the long press gesture on the Feed's grid: the first picture
     * is not chosen and the sheet does not appear.
     * ⚠️ **A phone's height**: on the bench's default screen the second month falls under the sheet,
     * which takes the tap as it does on a phone.
     */
    @Test
    @Config(qualifiers = "it-w393dp-h873dp", shadows = [ArchivioAperto::class])
    fun `il Feed ha la selezione multipla`() {
        val app = ApplicationProvider.getApplicationContext<android.content.Context>()
        runBlocking { Hint.FEED.remember(app) }
        val righe = listOf(riga(1, ms(2026, 10, 2), 0), riga(2, ms(2026, 9, 1), 0))
        val pagina = Feed.order(righe, FeedDate.TAKEN, zone)
        var aperta = false
        scene.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Casa(view = FolderView.FEED, feed = pagina, onFeedOpen = { _, _ -> aperta = true })
                }
            }
        }
        scene.waitForIdle()
        val prima = scene.onNodeWithContentDescription(app.getString(R.string.grid_item, 1, 2))
        val seconda = scene.onNodeWithContentDescription(app.getString(R.string.grid_item, 2, 2))
        prima.performTouchInput { longClick() }
        scene.waitForIdle()
        prima.assertIsSelected()
        scene.onAllNodesWithText(app.getString(R.string.pick_invert)).onFirst().assertExists()
        seconda.performClick()
        scene.waitForIdle()
        seconda.assertIsSelected()
        assertFalse("Il tocco in selezione ha aperto l'immagine", aperta)
    }
}

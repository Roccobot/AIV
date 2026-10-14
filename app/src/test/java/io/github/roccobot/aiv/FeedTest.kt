package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
    @Config(shadows = [ArchivioAperto::class])
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
        scene.onNodeWithText(app.getString(R.string.hint_feed)).assertExists().performClick()
        scene.waitForIdle()
        scene.onNodeWithText(app.getString(R.string.hint_feed)).assertDoesNotExist()
        assertTrue("Il velo non si è archiviato", runBlocking { Hint.FEED.flow(app).first() })
    }
}

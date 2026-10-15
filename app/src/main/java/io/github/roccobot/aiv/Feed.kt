package io.github.roccobot.aiv

import android.net.Uri
import java.time.Instant
import java.time.ZoneId

/**
 * The Feed: every photo and video of the visible folders in one stream, the newest first,
 * grouped by month. His request of 2026-10-13, his choices of 2026-10-14 (C1 by month, D1 the
 * folders the grid shows, without the bin), for both variants, from 6.02.
 *
 * ⚠️⚠️ **The order is a pure function of the rows**, and the MediaStore only supplies them
 * ([Folder.feed]): the bench checks the order and the months without a phone.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 */
object Feed {

    /**
     * One row as the MediaStore gives it.
     *
     * @param taken when the picture was taken, in milliseconds, or `null` when the file does not
     *   say (screenshots, downloads, edited copies often do not).
     * @param modified when the file was last written, in **seconds**, as `DATE_MODIFIED` is.
     * @param dir the folder the file lives in, for [shown], or `null` when the row does not say.
     */
    data class Row(
        val uri: Uri,
        val id: Long,
        val taken: Long?,
        val modified: Long,
        val dir: String? = null
    )

    /** A month of the stream: its first item's place in [Page.items]. */
    data class Month(val year: Int, val month: Int, val start: Int)

    /** The stream, in order, and where each month starts. */
    data class Page(val items: List<Uri>, val months: List<Month>) {
        val series: Folder.Series get() = Folder.Series(items, 0)
    }

    /**
     * The moment a row sorts by, in milliseconds.
     *
     * ⚠️⚠️ **[FeedDate.TAKEN] falls back on the modification date** (his wording: *usare la data
     * di scatto e ripiegare sulla data di modifica*): a screenshot has no date of taking, and
     * without the fallback it would sink to the bottom of the stream, at 1970.
     * ⚠️ **A taken date of zero or below counts as missing**: some apps write 0 instead of nothing.
     */
    fun moment(row: Row, date: FeedDate): Long = when (date) {
        FeedDate.TAKEN -> row.taken?.takeIf { it > 0 } ?: (row.modified * 1000)
        FeedDate.MODIFIED -> row.modified * 1000
    }

    /**
     * The rows of the folders the grid shows: the filter of [FolderSelection.visible], the same as
     * the grid's and the search's (his choice D1).
     *
     * ⚠️ **[peeking] lets the hidden folders in**, from 6.03 (his request: *`Mostra nascoste` deve
     * far apparire nel feed anche gli elementi di quelle cartelle*): for the minute of 'Mostra
     * nascoste' their pictures are in the stream, as their folders are in the grid.
     */
    fun shown(rows: List<Row>, selection: FolderSelection, peeking: Boolean): List<Row> =
        rows.filter { selection.visible(it.dir, peeking) }

    /**
     * The rows in stream order, newest first, and the months they fall in, read in [zone].
     *
     * ⚠️ **Ties go to the higher id**, the most recently indexed, as in a folder: two shots of the
     * same second keep a stable order between one reading and the next.
     */
    fun order(rows: List<Row>, date: FeedDate, zone: ZoneId = ZoneId.systemDefault()): Page {
        val sorted = rows.sortedWith(
            compareByDescending<Row> { moment(it, date) }.thenByDescending { it.id }
        )
        val months = mutableListOf<Month>()
        sorted.forEachIndexed { i, row ->
            val day = Instant.ofEpochMilli(moment(row, date)).atZone(zone)
            val last = months.lastOrNull()
            if (last == null || last.year != day.year || last.month != day.monthValue) {
                months += Month(day.year, day.monthValue, i)
            }
        }
        return Page(sorted.map { it.uri }, months)
    }
}

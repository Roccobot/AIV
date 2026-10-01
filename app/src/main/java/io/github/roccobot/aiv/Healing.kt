package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Local corrections belong to an image, never to a reusable style. */
object Healing {
    const val MIN_RADIUS = 0.001f
    const val MAX_RADIUS = 0.06f
    const val START_RADIUS = 0.006f
    const val MAX_POLYGONS = 4096
    const val MAX_SELECTION = 24_576
    const val MAX_WORK = 393_216
    const val MAX_HISTORY_BYTES = 16 * 1024 * 1024

    data class Plan(
        val patches: List<Patch> = emptyList(),
    ) {
        val idle: Boolean get() = patches.isEmpty()

        companion object {
            val NONE = Plan()
        }
    }

    // No caller can mutate patch pixels. History shares the same immutable payload.
    class Patch internal constructor(
        internal val left: Int,
        internal val top: Int,
        internal val width: Int,
        internal val height: Int,
        internal val sourceWidth: Int,
        internal val sourceHeight: Int,
        pixels: IntArray,
    ) {
        private val pixels = pixels.copyOf()
        val bytes: Int get() = pixels.size * 4

        internal fun bitmap(): Bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    /** Polygons are normalized in the EXIF-oriented original, before user geometry. */
    data class Selection(
        val polygons: List<List<Offset>> = emptyList(),
    ) {
        val idle: Boolean get() = polygons.isEmpty()

        fun add(polygon: List<Offset>): Selection =
            if (polygon.size < 3 ||
                polygon.any { !it.x.isFinite() || !it.y.isFinite() }
            ) {
                this
            } else {
                copy(polygons = polygons + listOf(polygon.toList()))
            }

        companion object {
            val NONE = Selection()
        }
    }

    internal fun unpose(
        point: Offset,
        spin: Spin,
    ): Offset {
        val turned =
            when (spin.turns.mod(4)) {
                1 -> Offset(point.y, 1f - point.x)
                2 -> Offset(1f - point.x, 1f - point.y)
                3 -> Offset(1f - point.y, point.x)
                else -> point
            }
        return if (spin.mirror) Offset(1f - turned.x, turned.y) else turned
    }

    private fun path(
        selection: Selection,
        width: Int,
        height: Int,
        left: Int = 0,
        top: Int = 0,
    ): Path =
        Path().apply {
            fillType = Path.FillType.WINDING
            for (polygon in selection.polygons) {
                moveTo(polygon.first().x * width - left, polygon.first().y * height - top)
                polygon.drop(1).forEach { lineTo(it.x * width - left, it.y * height - top) }
                close()
            }
        }

    internal fun overlay(
        selection: Selection,
        width: Int,
        height: Int,
        color: Int,
    ): Bitmap? {
        if (selection.idle) return null
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            Canvas(this).drawPath(
                path(selection, width, height),
                Paint().apply {
                    this.color = color or 0xff000000.toInt()
                    isAntiAlias = true
                },
            )
        }
    }

    /** Reuses exactly the pixels computed by Apply, including their original alpha. */
    internal fun render(
        bitmap: Bitmap,
        plan: Plan,
        region: RectF = RectF(0f, 0f, 1f, 1f),
    ): Bitmap {
        if (plan.idle) return bitmap
        val relevant =
            plan.patches.filter {
                RectF.intersects(
                    region,
                    RectF(
                        it.left.toFloat() / it.sourceWidth,
                        it.top.toFloat() / it.sourceHeight,
                        (it.left + it.width).toFloat() / it.sourceWidth,
                        (it.top + it.height).toFloat() / it.sourceHeight,
                    ),
                )
            }
        if (relevant.isEmpty()) return bitmap
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint =
            Paint(Paint.FILTER_BITMAP_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
            }
        for (patch in relevant) {
            val image = patch.bitmap()
            try {
                val x = (patch.left.toFloat() / patch.sourceWidth - region.left) / region.width() * bitmap.width
                val y = (patch.top.toFloat() / patch.sourceHeight - region.top) / region.height() * bitmap.height
                val w = patch.width.toFloat() / patch.sourceWidth / region.width() * bitmap.width
                val h = patch.height.toFloat() / patch.sourceHeight / region.height() * bitmap.height
                val exact =
                    bitmap.width == (patch.sourceWidth * region.width()).roundToInt() &&
                        bitmap.height == (patch.sourceHeight * region.height()).roundToInt()
                val destination =
                    if (exact) {
                        RectF(
                            x.roundToInt().toFloat(),
                            y.roundToInt().toFloat(),
                            (x + w).roundToInt().toFloat(),
                            (y + h).roundToInt().toFloat(),
                        )
                    } else {
                        RectF(x, y, x + w, y + h)
                    }
                canvas.drawBitmap(image, null, destination, paint)
            } finally {
                image.recycle()
            }
        }
        return result
    }

    /** Decode only a local full-resolution crop when the format supports region reading. */
    internal suspend fun prepare(
        context: Context,
        uri: Uri,
        source: RegionSource?,
        plan: Plan,
        selection: Selection,
    ): Patch? =
        withContext(Dispatchers.Default) {
            if (selection.idle) return@withContext null
            val job = currentCoroutineContext()
            var whole: Bitmap? = null
            var crop: Bitmap? = null
            var corrected: Bitmap? = null
            try {
                if (source == null) whole = withContext(Dispatchers.IO) { ImageSource.pixels(context, uri, 0) }
                val width = source?.width ?: whole?.width ?: return@withContext null
                val height = source?.height ?: whole?.height ?: return@withContext null
                val points = selection.polygons.flatten()
                val left = floor(points.minOf { it.x } * width).toInt().coerceIn(0, width)
                val top = floor(points.minOf { it.y } * height).toInt().coerceIn(0, height)
                val right = ceil(points.maxOf { it.x } * width).toInt().coerceIn(0, width)
                val bottom = ceil(points.maxOf { it.y } * height).toInt().coerceIn(0, height)
                if (right <= left || bottom <= top || (right - left).toLong() * (bottom - top) > MAX_WORK) return@withContext null
                val padding = max(16, min(96, max(right - left, bottom - top)))
                val area = Rect(max(0, left - padding), max(0, top - padding), min(width, right + padding), min(height, bottom + padding))
                if (area.width().toLong() * area.height() > MAX_WORK) return@withContext null
                job.ensureActive()
                crop = source?.tile(area, 1) ?: whole?.let { Bitmap.createBitmap(it, area.left, area.top, area.width(), area.height()) }
                    ?: return@withContext null
                corrected =
                    render(
                        crop,
                        plan,
                        RectF(
                            area.left.toFloat() / width,
                            area.top.toFloat() / height,
                            area.right.toFloat() / width,
                            area.bottom.toFloat() / height,
                        ),
                    )
                val maskBitmap = Bitmap.createBitmap(area.width(), area.height(), Bitmap.Config.ARGB_8888)
                val mask =
                    try {
                        Canvas(maskBitmap).drawPath(path(selection, width, height, area.left, area.top), Paint().apply { color = -1 })
                        val values = IntArray(area.width() * area.height())
                        maskBitmap.getPixels(values, 0, area.width(), 0, 0, area.width(), area.height())
                        BooleanArray(values.size) { values[it] ushr 24 >= 128 }
                    } finally {
                        maskBitmap.recycle()
                    }
                if (mask.count { it } !in 1..MAX_SELECTION) return@withContext null
                val pixels = IntArray(mask.size)
                corrected.getPixels(pixels, 0, area.width(), 0, 0, area.width(), area.height())
                val healed =
                    Inpaint.repair(pixels, area.width(), area.height(), mask) { job.ensureActive() }
                        ?: return@withContext null
                val patchPixels = IntArray((right - left) * (bottom - top))
                for (y in top until bottom) {
                    healed.copyInto(
                        patchPixels,
                        (y - top) * (right - left),
                        (y - area.top) * area.width() + left - area.left,
                        (y - area.top) * area.width() + right - area.left,
                    )
                }
                Patch(left, top, right - left, bottom - top, width, height, patchPixels)
            } finally {
                if (corrected !== crop) corrected?.recycle()
                if (crop !== whole) crop?.recycle()
                whole?.recycle()
            }
        }
}

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

/** Le correzioni locali appartengono a un'immagine, mai a uno stile riutilizzabile. */
object Healing {
    const val MIN_RADIUS = 0.001f
    const val MAX_RADIUS = 0.06f
    const val START_RADIUS = 0.006f
    const val MAX_POLYGONS = 4096
    // La piramide di [Inpaint] rende sostenibile anche un buco grande, quindi la selezione
    // accetta il pennello al massimo su un'immagine da 12 MP. L'area di lavoro limita la memoria
    // di un'applicazione (picco stimato vicino a 90 MB, non misurato su un telefono).
    const val MAX_SELECTION = 196_608
    const val MAX_WORK = 1_048_576
    const val MAX_HISTORY_BYTES = 16 * 1024 * 1024

    data class Plan(
        val patches: List<Patch> = emptyList(),
    ) {
        val idle: Boolean get() = patches.isEmpty()

        companion object {
            val NONE = Plan()
        }
    }

    // Nessun chiamante può modificare i pixel della toppa: la cronologia condivide lo stesso
    // contenuto immutabile.
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

    /** I poligoni sono normalizzati sull'originale orientato secondo l'EXIF, prima della geometria dell'utente. */
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

    /** Riusa esattamente i pixel calcolati dall'applicazione, compresa la loro trasparenza originale. */
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

    /** Decodifica solo un ritaglio locale a piena risoluzione, quando il formato permette la lettura per regioni. */
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
                // Il margine di ricerca è ampio anche attorno a un difetto piccolo (richiesta
                // dell'utente, 3.52). Se non entra nell'area di lavoro si restringe, e la
                // selezione resta valida.
                var padding = max(64, min(352, max(right - left, bottom - top) * 7 / 2))
                var area = Rect(max(0, left - padding), max(0, top - padding), min(width, right + padding), min(height, bottom + padding))
                while (padding > 0 && area.width().toLong() * area.height() > MAX_WORK) {
                    padding = padding * 3 / 4
                    area = Rect(max(0, left - padding), max(0, top - padding), min(width, right + padding), min(height, bottom + padding))
                }
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
                // Il bordo sfuma verso l'esterno, quindi la toppa cresce della stessa fascia.
                val edge = Inpaint.feather(max(right - left, bottom - top))
                val healed =
                    Inpaint.repair(pixels, area.width(), area.height(), mask, edge) { job.ensureActive() }
                        ?: return@withContext null
                val patchLeft = max(area.left, left - edge)
                val patchTop = max(area.top, top - edge)
                val patchRight = min(area.right, right + edge)
                val patchBottom = min(area.bottom, bottom + edge)
                val patchWidth = patchRight - patchLeft
                val patchPixels = IntArray(patchWidth * (patchBottom - patchTop))
                for (y in patchTop until patchBottom) {
                    healed.copyInto(
                        patchPixels,
                        (y - patchTop) * patchWidth,
                        (y - area.top) * area.width() + patchLeft - area.left,
                        (y - area.top) * area.width() + patchRight - area.left,
                    )
                }
                Patch(patchLeft, patchTop, patchWidth, patchBottom - patchTop, width, height, patchPixels)
            } finally {
                if (corrected !== crop) corrected?.recycle()
                if (crop !== whole) crop?.recycle()
                whole?.recycle()
            }
        }
}

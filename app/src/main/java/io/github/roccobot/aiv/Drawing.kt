package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * The **Disegno** module of the full editor, first phase (G1, 4.40): free hand, lines, arrows,
 * rounded rectangles and ellipses, with a colour, a width, a dashed stroke and an optional fill.
 *
 * ⚠️⚠️ **His four answers decide its shape** (2026-10-06, `d-disegno-*`):
 * - D1a: it lives only in the full editor, as the last module of the row, so below Android 13
 *   there is no drawing;
 * - D2a: the drawing is attached to the image, so it turns, warps and is cut with it;
 * - D3a: it melts into the saved image like the watermark, and a reopened file holds pixels, not
 *   shapes;
 * - D4a: three versions in sequence (G1 shapes, G2 selection and handles, G3 text).
 *
 * ⚠️⚠️ **The points live in the ORIGINAL frame of the image, as fractions of its sides**, the same
 * choice as the Correggi/Rimuovi selection: a quarter turn or a mirror after drawing moves
 * nothing in the model, and the drawing follows the image for free. A point in the posed frame
 * would have to be rewritten at every pose, like the crop rectangle is.
 *
 * ⚠️⚠️ **It is laid after the development and before the geometry**, on the stage and in the saved
 * file alike: after, so the colour sliders don't repaint the ink the user picked; before, so
 * straightening, the keystones, the lens distortion, Fluidifica and the crop move it with the
 * image (D2a). The two places run the same functions of this file.
 */
data class Drawing(val marks: List<Mark> = emptyList()) {

    /** Whether there is nothing drawn, so the image leaves exactly as it came. */
    val idle: Boolean get() = marks.isEmpty()

    /** This drawing with [mark] on top. */
    fun with(mark: Mark): Drawing = Drawing(marks + mark)

    companion object {
        val NONE = Drawing()
    }
}

/** The five shapes of the first phase, in the order the module shows them. */
enum class Pen { FREE, LINE, ARROW, RECT, ELLIPSE }

/**
 * One shape of the drawing.
 *
 * ⚠️ **[points] are fractions of the original image**: x of its width, y of its height. A free
 * hand stroke keeps every point the finger passed; the other four keep where the finger went
 * down and where it is (or was lifted).
 * ⚠️ **[width] is a fraction of the long side of the original image**, so the stroke looks the
 * same on the preview and in the saved file at full resolution, which have different sizes.
 */
data class Mark(
    val pen: Pen,
    val points: List<Offset>,
    val ink: Int,
    val width: Float,
    val dashed: Boolean,
    /** Whether a closed shape (rectangle, ellipse) is filled with [ink]; the others ignore it. */
    val filled: Boolean
) {
    /** This mark with its last point moved to [to], or appended for a free hand stroke. */
    fun reaching(to: Offset): Mark = when (pen) {
        Pen.FREE -> copy(points = points + to)
        else -> copy(points = listOf(points.first(), to))
    }
}

/** How a drawing becomes pixels, the same way on the stage and in the saved file. */
internal object Draw {

    /** The factory width: about 4 pixels on a 1600 pixel preview, 16 on a 12 MP photo. */
    const val WIDTH = 0.004f

    /** The width slider, as fractions of the long side. */
    const val WIDTH_MIN = 0.001f
    const val WIDTH_MAX = 0.03f

    /**
     * The eight inks of the palette: white, black, and six clear hues.
     *
     * ⚠️ **No colour picker in G1**: eight swatches cover the use the module is for (marking,
     * pointing, underlining), and a picker costs a dialog. The first is red because an arrow on a
     * photo is red more often than not.
     */
    val INKS = listOf(
        0xFFE53935.toInt(), 0xFFFFB300.toInt(), 0xFF43A047.toInt(), 0xFF1E88E5.toInt(),
        0xFF8E24AA.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF7043.toInt()
    )

    /**
     * The matrix from the original frame ([w] x [h] pixels) to the posed frame.
     *
     * ⚠️⚠️ **It is the same transform as `spunBy`, step by step**: mirror, then turn, then bring
     * the bounds back to the origin, which is what `Bitmap.createBitmap` does with a matrix. A
     * second derivation (say, from `Healing.unpose`) would agree on the corners and could still
     * disagree by a pixel; `DisegnoTest` compares the two on a dot.
     */
    fun posed(w: Int, h: Int, spin: Spin): Matrix = Matrix().apply {
        if (spin.mirror) postScale(-1f, 1f)
        postRotate(90f * spin.turns)
        val bounds = RectF(0f, 0f, w.toFloat(), h.toFloat())
        mapRect(bounds)
        postTranslate(-bounds.left, -bounds.top)
    }

    /**
     * Paints [drawing] on [canvas], whose coordinates are the original frame of a [w] x [h]
     * image (the caller sets the pose on the canvas).
     */
    fun paint(canvas: Canvas, drawing: Drawing, w: Int, h: Int) {
        val long = max(w, h).toFloat()
        for (mark in drawing.marks) paint(canvas, mark, w.toFloat(), h.toFloat(), long)
    }

    private fun paint(canvas: Canvas, mark: Mark, w: Float, h: Float, long: Float) {
        if (mark.points.isEmpty()) return
        val stroke = (mark.width * long).coerceAtLeast(1f)
        val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mark.ink
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            // ⚠️ The dashes scale with the stroke, so a thick line is not a row of dots.
            if (mark.dashed) pathEffect = DashPathEffect(floatArrayOf(stroke * 2f, stroke * 2.2f), 0f)
        }
        val pts = mark.points.map { Offset(it.x * w, it.y * h) }
        val a = pts.first()
        val b = pts.last()
        when (mark.pen) {
            Pen.FREE -> {
                // ⚠️ A tap with no movement is a dot: a path of one point draws nothing.
                if (pts.size == 1 || pts.all { it == a }) {
                    canvas.drawPoint(a.x, a.y, pen.apply { pathEffect = null })
                    return
                }
                canvas.drawPath(smooth(pts), pen)
            }
            Pen.LINE -> canvas.drawLine(a.x, a.y, b.x, b.y, pen)
            Pen.ARROW -> {
                canvas.drawLine(a.x, a.y, b.x, b.y, pen)
                // ⚠️ The head is never dashed: a broken head no longer reads as a head.
                val head = Paint(pen).apply { pathEffect = null }
                val angle = atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble())
                val size = max(stroke * HEAD, long * HEAD_MIN)
                for (side in listOf(-1, 1)) {
                    val t = angle + Math.PI + side * HEAD_ANGLE
                    canvas.drawLine(
                        b.x, b.y,
                        b.x + (size * cos(t)).toFloat(), b.y + (size * sin(t)).toFloat(),
                        head
                    )
                }
            }
            Pen.RECT, Pen.ELLIPSE -> {
                val box = RectF(min(a.x, b.x), min(a.y, b.y), max(a.x, b.x), max(a.y, b.y))
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = mark.ink
                    style = Paint.Style.FILL
                }
                if (mark.pen == Pen.ELLIPSE) {
                    if (mark.filled) canvas.drawOval(box, fill)
                    canvas.drawOval(box, pen)
                } else {
                    // ⚠️ The corner radius follows the stroke, and never more than half a side.
                    val r = min(stroke * CORNER, min(box.width(), box.height()) / 2f)
                    if (mark.filled) canvas.drawRoundRect(box, r, r, fill)
                    canvas.drawRoundRect(box, r, r, pen)
                }
            }
        }
    }

    /**
     * A free hand path through [pts], smoothed with quadratic curves through the midpoints.
     *
     * ⚠️ The finger reports a point per frame, and straight segments between them show corners on
     * a fast curve; the midpoint curve passes near every point and has no corners.
     */
    private fun smooth(pts: List<Offset>): Path = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        if (pts.size == 2) {
            lineTo(pts[1].x, pts[1].y)
            return@apply
        }
        for (i in 1 until pts.size - 1) {
            val mid = Offset((pts[i].x + pts[i + 1].x) / 2f, (pts[i].y + pts[i + 1].y) / 2f)
            quadTo(pts[i].x, pts[i].y, mid.x, mid.y)
        }
        lineTo(pts.last().x, pts.last().y)
    }

    /**
     * [drawing] on a transparent bitmap in the posed frame of a [w] x [h] original, or `null`
     * when there is nothing drawn: the stage lays it over the developed preview.
     *
     * ⚠️ **[into], when given and of the right size, is reused**: the stage redraws at every move
     * of the finger, and a new bitmap per frame would be several megabytes of garbage.
     */
    fun overlay(drawing: Drawing, w: Int, h: Int, spin: Spin, into: Bitmap? = null): Bitmap? {
        if (drawing.idle || w <= 0 || h <= 0) return null
        val odd = spin.turns.mod(2) == 1
        val pw = if (odd) h else w
        val ph = if (odd) w else h
        val out = into?.takeIf { it.width == pw && it.height == ph && it.isMutable }
            ?.apply { eraseColor(0) }
            ?: Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.concat(posed(w, h, spin))
        paint(canvas, drawing, w, h)
        return out
    }

    /**
     * [target], an image in the posed frame, with [drawing] laid on it: the step of the saved
     * file between the development and the geometry.
     *
     * ⚠️ **It draws on [target] itself when it can**, and on a copy otherwise (a bitmap read from
     * a file can be immutable): the caller recycles by identity, as for the other steps.
     */
    fun onto(target: Bitmap, drawing: Drawing, spin: Spin): Bitmap {
        if (drawing.idle) return target
        val odd = spin.turns.mod(2) == 1
        val w = if (odd) target.height else target.width
        val h = if (odd) target.width else target.height
        val out = if (target.isMutable && target.config != Bitmap.Config.HARDWARE) target
        else target.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        canvas.concat(posed(w, h, spin))
        paint(canvas, drawing, w, h)
        return out
    }

    /** The arrow head, as strokes long, at least a fraction of the long side, and its half angle. */
    private const val HEAD = 5f
    private const val HEAD_MIN = 0.012f
    private const val HEAD_ANGLE = Math.PI / 7

    /** The corner radius of a rectangle, as strokes. */
    private const val CORNER = 3f

    /** The distance between two points of a free hand stroke worth keeping, as a fraction. */
    const val STEP = 0.002f

    /** Whether [b] is far enough from [a] to be kept in a free hand stroke. */
    fun far(a: Offset, b: Offset): Boolean = hypot(b.x - a.x, b.y - a.y) >= STEP
}

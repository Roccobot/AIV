package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.core.graphics.ColorUtils
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
enum class Pen {
    FREE, LINE, ARROW, RECT, ELLIPSE;

    /** Whether this pen closes a shape, so a fill means something. */
    val closed: Boolean get() = this == RECT || this == ELLIPSE
}

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
    /**
     * The fill of a closed shape (rectangle, ellipse) as a colour with its alpha, or `null` for
     * none; the other three pens ignore it.
     *
     * ⚠️⚠️ **Its own colour and its own opacity, apart from the outline**: his note during G1
     * (2026-10-06) gives the example of a red border with a white fill at 50%, so a fill in the
     * outline's ink, as G1 was first written, could not say it.
     */
    val fill: Int?
) {
    /** This mark with its last point moved to [to], or appended for a free hand stroke. */
    fun reaching(to: Offset): Mark = when (pen) {
        Pen.FREE -> copy(points = points + to)
        else -> copy(points = listOf(points.first(), to))
    }
}

/** How a drawing becomes pixels, the same way on the stage and in the saved file. */
internal object Draw {

    /** The width slider, as fractions of the long side. */
    const val WIDTH_MIN = 0.001f
    const val WIDTH_MAX = 0.03f

    /**
     * The factory width: the slider at 60% of its travel (his values of 2026-10-07, after the
     * 4.41 round). Until 4.41 it was 0.004, near the bottom of the slider, and he found it thin.
     */
    const val WIDTH = WIDTH_MIN + 0.6f * (WIDTH_MAX - WIDTH_MIN)

    /**
     * The factory ink of the stroke, the first swatch: `#FFFF4C3F` (his palette of 4.44, answer
     * `D1`). In 4.42 and 4.43 it was `#FFFF4B3D`, his value; his palette of 4.44 starts with a red a
     * shade away, and he chose to move the factory ink onto it.
     */
    const val INK = 0xFFFF4C3F.toInt()

    /**
     * The factory fill of a closed shape, `#33FFBF00` in ARGB (his answer `D2`, 4.44): the amber
     * swatch at 20%, so rectangles and ellipses are born filled. In 4.42 and 4.43 it was a salmon at
     * about 15% (`#26FFAE8E`), which his palette of 4.44 no longer has.
     */
    const val FILL = 0x33FFBF00

    /**
     * **[colour] made lighter or darker by [shift]**, from -1 (the darkest) to 1 (the lightest),
     * with 0 the colour itself (Luminosità, 4.45; his note on `4.43-01`: *dal colore mostrato lo
     * schiarisce o scurisce senza avvicinarsi troppo né al bianco né al nero*).
     *
     * ⚠️ It moves the HSL lightness towards [LIGHT_MAX] or [LIGHT_MIN] and never past them, so hue
     * and saturation stay those of the swatch. A colour already beyond a bound does not move that
     * way: white does not get lighter, black does not get darker. The alpha is kept.
     */
    fun lit(colour: Int, shift: Float): Int {
        if (shift == 0f) return colour
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(colour, hsl)
        val l = hsl[2]
        val s = shift.coerceIn(-1f, 1f)
        hsl[2] = if (s > 0f) l + s * max(0f, LIGHT_MAX - l) else l + s * max(0f, l - LIGHT_MIN)
        return (ColorUtils.HSLToColor(hsl) and 0xFFFFFF) or (colour and 0xFF000000.toInt())
    }

    /**
     * How far Luminosità goes, as HSL lightness: 15% and 85%, so it never reaches black or white.
     * A choice of the session, declared in the test item.
     */
    const val LIGHT_MIN = 0.15f
    const val LIGHT_MAX = 0.85f

    /** [colour] with its alpha set to [alpha], from 0 to 1. */
    fun withAlpha(colour: Int, alpha: Float): Int =
        ((alpha.coerceIn(0f, 1f) * 255f + 0.5f).toInt() shl 24) or (colour and 0xFFFFFF)

    /**
     * The nine inks of the palette, his of 4.44 in his order: six hues, then white, grey and black
     * last. The grey is a 30% black, `#B3B3B3` (his note on `4.44-01`, read as a graphic designer
     * says it; declared in the test item, the other reading being `#4D4D4D`).
     *
     * ⚠️ **No colour picker in G1**: eight swatches cover the use the module is for (marking,
     * pointing, underlining), and a picker costs a dialog.
     * ⚠️ **The factory colours are swatches**: [INK] is the first, and [FILL] is the amber at 20%.
     * A swatch is chosen when its colour equals the current one, so a factory colour off the palette
     * would open the module with no swatch chosen. The stroke takes the first swatch by
     * construction (`Gaze.ink`); the fill keeps its own constant, and `DisegnoTest` watches it.
     */
    val INKS = listOf(
        INK, 0xFFFFBF00.toInt(), 0xFF5ACB8C.toInt(), 0xFF3EB7FF.toInt(),
        0xFF846AE2.toInt(), 0xFFCC6898.toInt(), 0xFFFFFFFF.toInt(), 0xFFB3B3B3.toInt(), 0xFF000000.toInt()
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
            if (mark.dashed) pathEffect = DashPathEffect(floatArrayOf(stroke * DASH, stroke * GAP), 0f)
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
                // ⚠️ The head is never dashed: a broken head no longer reads as a head.
                val head = Paint(pen).apply { pathEffect = null }
                val angle = atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble())
                val size = max(stroke * HEAD, long * HEAD_MIN)
                if (mark.dashed) dashedShaft(canvas, a, b, size, stroke, pen, head)
                else canvas.drawLine(a.x, a.y, b.x, b.y, pen)
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
                val fill = mark.fill?.let {
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = it
                        style = Paint.Style.FILL
                    }
                }
                if (mark.pen == Pen.ELLIPSE) {
                    fill?.let { canvas.drawOval(box, it) }
                    canvas.drawOval(box, pen)
                } else {
                    // ⚠️ The corner radius follows the stroke, and never more than half a side.
                    val r = min(stroke * CORNER, min(box.width(), box.height()) / 2f)
                    fill?.let { canvas.drawRoundRect(box, r, r, it) }
                    canvas.drawRoundRect(box, r, r, pen)
                }
            }
        }
    }

    /**
     * The shaft of a dashed arrow, laid from the tip back to the tail (his note B on the 4.45
     * round, with a drawing: *che non rimanesse un buco tra il tratto finale e la punta*, and *che
     * il primo tratto fosse lungo almeno quanto basta per 'uscire' dall'angolo concavo della
     * punta*).
     *
     * ⚠️⚠️ **The dashes start from the tip, not from the tail**: counted from the tail, the pattern
     * reached the tip wherever the length left it, often in a gap, and a short dash could sit
     * inside the head. Now the piece against the tip is solid and as long as the head's arms reach
     * along the shaft, plus a stroke for their round caps; the pattern goes on from there toward
     * the tail, starting with a gap, and it is the tail's end that comes out partial.
     * ⚠️ A shaft shorter than that piece is drawn solid, whole.
     */
    private fun dashedShaft(canvas: Canvas, a: Offset, b: Offset, size: Float, stroke: Float, dashed: Paint, solid: Paint) {
        val len = hypot(a.x - b.x, a.y - b.y)
        if (len <= 0f) return
        val ux = (a.x - b.x) / len
        val uy = (a.y - b.y) / len
        val piena = min(len, (size * cos(HEAD_ANGLE)).toFloat() + stroke)
        val cx = b.x + ux * piena
        val cy = b.y + uy * piena
        canvas.drawLine(b.x, b.y, cx, cy, solid)
        if (len > piena) {
            // ⚠️ The phase skips the first dash, so the pattern opens with a gap after the solid piece.
            val gap = Paint(dashed).apply { pathEffect = DashPathEffect(floatArrayOf(stroke * DASH, stroke * GAP), stroke * DASH) }
            canvas.drawLine(cx, cy, a.x, a.y, gap)
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

    /** A dash and the gap after it, as strokes: the round caps eat half a stroke at each end. */
    private const val DASH = 2f
    private const val GAP = 2.2f

    /** The corner radius of a rectangle, as strokes. */
    private const val CORNER = 3f

    /** The distance between two points of a free hand stroke worth keeping, as a fraction. */
    const val STEP = 0.002f

    /** Whether [b] is far enough from [a] to be kept in a free hand stroke. */
    fun far(a: Offset, b: Offset): Boolean = hypot(b.x - a.x, b.y - a.y) >= STEP
}

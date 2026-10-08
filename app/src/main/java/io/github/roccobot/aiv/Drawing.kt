package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.core.graphics.ColorUtils
import kotlin.math.abs
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

    /** This drawing with the mark at [index] replaced by [mark] (G2, 4.60). */
    fun replacing(index: Int, mark: Mark): Drawing =
        Drawing(marks.mapIndexed { i, m -> if (i == index) mark else m })

    /** This drawing without the mark at [index] (G2, 4.60). */
    fun without(index: Int): Drawing = Drawing(marks.filterIndexed { i, _ -> i != index })

    /** This drawing with [mark] put at [index], the marks from there on one place higher (4.70). */
    fun inserting(index: Int, mark: Mark): Drawing =
        Drawing(marks.toMutableList().apply { add(index.coerceIn(0, size), mark) })

    /**
     * This drawing with the marks at [i] and [j] changed places (4.70, 'Sposta sopra' and 'Sposta
     * sotto'): the list is the order of the layers, the last one on top.
     */
    fun swapping(i: Int, j: Int): Drawing {
        if (i !in marks.indices || j !in marks.indices) return this
        return Drawing(marks.toMutableList().apply { this[i] = marks[j]; this[j] = marks[i] })
    }

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
    val fill: Int?,
    /**
     * How the module chose [ink] and [fill], or `null` for a mark made elsewhere (the tests): the
     * module loads it back when the mark is chosen (G2, 4.60). See [Tint].
     */
    val tint: Tint? = null,
    /**
     * **The turn of a rectangle or an ellipse around the centre of its box**, in degrees,
     * clockwise on the original image (4.70, his answer on `4.64-04`: *toccando `Ruota` si passa in
     * modalità rotazione*). [points] stay the two corners of the box before the turn.
     *
     * ⚠️ **Only the two closed pens keep an angle**: a line, an arrow and a free hand stroke turn
     * by turning their points, which say everything about them, so a second place for the same
     * turn would be a second truth. [turned] does both.
     * ⚠️ The turn is in the pixels of the original image, not in its fractions: on an image that
     * is not square, a fraction turned would shear the shape.
     */
    val angle: Float = 0f,
    /**
     * **How much a rectangle or an ellipse blurs the image under it**, as a fraction of the longer
     * side of its box, or `null` for an element drawn with its line and fill (4.80, his
     * specification in the brief, round of 4.43: *una selezione tipo rettangolo arrotondato, che
     * anziché riempire la propria area di un colore la sfoca ... da 0,5% a 25% del lato maggiore
     * dell'oggetto. Si applica solo agli oggetti con un'area*).
     *
     * ⚠️ With a value the element ignores its line and its fill, which it keeps: turned off, it is
     * the element it was. The blur is laid by [Draw.blurAreas], under every other element.
     */
    val blur: Float? = null
) {
    /** Whether this element blurs the image under it instead of being drawn ([blur]). */
    val blurs: Boolean get() = blur != null && pen.closed

    /** This mark with its last point moved to [to], or appended for a free hand stroke. */
    fun reaching(to: Offset): Mark = when (pen) {
        Pen.FREE -> copy(points = points + to)
        else -> copy(points = listOf(points.first(), to))
    }

    /** This mark moved by [by], in fractions of the original image (G2, 4.60). */
    fun moved(by: Offset): Mark = copy(points = points.map { it + by })

    /**
     * **The frame the handles of this mark live in**, in the pixels of a [w] x [h] original
     * (4.70): the centre of its box, its half sides, and the cosine and sine of its turn. For the
     * free hand the box holds every point, and the turn is zero.
     */
    private class Frame(val c: Offset, val hw: Float, val hh: Float, val cos: Float, val sin: Float) {
        /** A point of the box's own axes, from its centre, as a point of the image. */
        fun out(x: Float, y: Float) = Offset(c.x + x * cos - y * sin, c.y + x * sin + y * cos)

        /** A point of the image, in the box's own axes from its centre. */
        fun into(p: Offset): Offset {
            val dx = p.x - c.x
            val dy = p.y - c.y
            return Offset(dx * cos + dy * sin, -dx * sin + dy * cos)
        }
    }

    private fun frame(w: Float, h: Float): Frame {
        val xs = if (pen == Pen.FREE) points.map { it.x * w } else listOf(points.first().x * w, points.last().x * w)
        val ys = if (pen == Pen.FREE) points.map { it.y * h } else listOf(points.first().y * h, points.last().y * h)
        val l = xs.min(); val r = xs.max(); val t = ys.min(); val b = ys.max()
        val rad = Math.toRadians(angle.toDouble())
        return Frame(Offset((l + r) / 2f, (t + b) / 2f), (r - l) / 2f, (b - t) / 2f, cos(rad).toFloat(), sin(rad).toFloat())
    }

    /**
     * **Where the chosen mark shows its handles**, in fractions of a [w] x [h] original (G2, 4.60,
     * his specification: *il rettangolo selezionato mostra 4 vertici color accento, la freccia 2
     * punti color accento*; 4.70, *le maniglie di base permetteranno di ridimensionare gli
     * oggetti*).
     *
     * - The line and the arrow: their two ends.
     * - The rectangle, the ellipse and the free hand: the four corners of the box, then the middle
     *   of each side, in the order top left, top right, bottom right, bottom left, then top,
     *   right, bottom, left of the box's own axes. The four corners were there in 4.60; the middles
     *   of the sides came with 4.70, to stretch along one axis (a reading of the session, declared
     *   in the test item).
     *
     * ⚠️ The index of a handle is what [reshaped] reads, so the order is a contract.
     */
    fun handles(w: Int, h: Int): List<Offset> {
        if (points.isEmpty()) return emptyList()
        if (pen == Pen.LINE || pen == Pen.ARROW) return listOf(points.first(), points.last())
        val fw = w.toFloat()
        val fh = h.toFloat()
        val f = frame(fw, fh)
        return listOf(
            -f.hw to -f.hh, f.hw to -f.hh, f.hw to f.hh, -f.hw to f.hh,
            0f to -f.hh, f.hw to 0f, 0f to f.hh, -f.hw to 0f
        ).map { (x, y) -> f.out(x, y).let { Offset(it.x / fw, it.y / fh) } }
    }

    /**
     * **This mark with the handle [handle] of [handles] dragged to [to]**, in fractions of a [w] x
     * [h] original (4.70, 'Trasforma').
     *
     * - A corner moves its two sides and the opposite corner stays; the middle of a side moves that
     *   side and the opposite one stays. The sides are those of the box's own axes, so a turned
     *   rectangle stretches along itself and not along the image.
     * - An end of a line or of an arrow goes where it is dragged.
     * - The free hand stretches its points with its box, and dragged past the opposite side it
     *   mirrors, as in the drawing programs; the rectangle and the ellipse, being symmetric, just
     *   grow on the other side.
     *
     * ⚠️ The gesture calls it with the mark as it was when the finger went down, every frame: from
     * the last frame the rounding would add up.
     */
    fun reshaped(handle: Int, to: Offset, w: Int, h: Int): Mark {
        if (points.isEmpty()) return this
        if (pen == Pen.LINE || pen == Pen.ARROW) {
            return copy(points = if (handle == 0) listOf(to, points.last()) else listOf(points.first(), to))
        }
        val fw = w.toFloat()
        val fh = h.toFloat()
        val f = frame(fw, fh)
        val p = f.into(Offset(to.x * fw, to.y * fh))
        var l = -f.hw; var r = f.hw; var t = -f.hh; var b = f.hh
        when (handle) {
            0 -> { l = p.x; t = p.y }
            1 -> { r = p.x; t = p.y }
            2 -> { r = p.x; b = p.y }
            3 -> { l = p.x; b = p.y }
            4 -> t = p.y
            5 -> r = p.x
            6 -> b = p.y
            7 -> l = p.x
            else -> return this
        }
        if (pen == Pen.FREE) {
            // ⚠️ A free hand stroke has no turn, so its box's axes are the image's: each point
            // keeps its place in the box. A box with no width (a straight stroke) has nothing to
            // stretch along that axis.
            fun along(v: Float, lo: Float, size: Float, nlo: Float, nsize: Float) =
                if (size > FLAT) nlo + (v - lo) / size * nsize else v
            val x0 = f.c.x - f.hw
            val y0 = f.c.y - f.hh
            return copy(points = points.map {
                Offset(
                    along(it.x * fw, x0, 2f * f.hw, f.c.x + l, r - l) / fw,
                    along(it.y * fh, y0, 2f * f.hh, f.c.y + t, b - t) / fh
                )
            })
        }
        val c = f.out((l + r) / 2f, (t + b) / 2f)
        val hw = abs(r - l) / 2f
        val hh = abs(b - t) / 2f
        return copy(points = listOf(Offset((c.x - hw) / fw, (c.y - hh) / fh), Offset((c.x + hw) / fw, (c.y + hh) / fh)))
    }

    /**
     * **This mark turned by [delta] degrees, clockwise, around the centre of its box**, in a [w] x
     * [h] original (4.70, 'Ruota'), with the snap to the multiples of 45 degrees within
     * [Draw.SNAP_DEG] (a reading of the session, declared in the test item: the R2 of his question,
     * *libera, con scatti a 0, 45 e 90 gradi*).
     *
     * - The rectangle and the ellipse turn their [angle], and the snap is on where they end up.
     * - The line and the arrow turn their points around their middle, and the snap is on the
     *   direction of the line: a line that comes near the horizontal lies on it.
     * - The free hand turns its points around the centre of its box, and the snap is on [delta]:
     *   a stroke has no direction of its own.
     *
     * ⚠️ Like [reshaped], it is called with the mark of when the finger went down.
     */
    fun turned(delta: Float, w: Int, h: Int): Mark {
        if (points.isEmpty()) return this
        val fw = w.toFloat()
        val fh = h.toFloat()
        if (pen.closed) return copy(angle = Draw.snapTurn(angle + delta))
        val by = if (pen == Pen.FREE) Draw.snapTurn(delta) else {
            val a = points.first()
            val b = points.last()
            val dir = Math.toDegrees(atan2(((b.y - a.y) * fh).toDouble(), ((b.x - a.x) * fw).toDouble())).toFloat()
            Draw.snapTurn(dir + delta) - dir
        }
        val f = frame(fw, fh)
        val rad = Math.toRadians(by.toDouble())
        val cs = cos(rad).toFloat()
        val sn = sin(rad).toFloat()
        return copy(points = points.map {
            val dx = it.x * fw - f.c.x
            val dy = it.y * fh - f.c.y
            Offset((f.c.x + dx * cs - dy * sn) / fw, (f.c.y + dx * sn + dy * cs) / fh)
        })
    }

    /** The centre of this mark's box, in fractions of a [w] x [h] original: what [turned] turns around. */
    fun centre(w: Int, h: Int): Offset {
        if (points.isEmpty()) return Offset.Zero
        val f = frame(w.toFloat(), h.toFloat())
        return Offset(f.c.x / w, f.c.y / h)
    }

    /**
     * **This mark with the style of [source]** (4.70, his answer on `4.64-03`: *È C2 (stile), che è
     * poi applicato (nelle parti compatibili/applicabili) ad un altro oggetto selezionato*): the
     * line's colour, light and opacity, the width and the dashes always; the fill only between two
     * closed shapes, since a line has no fill to give and an arrow none to take. The pen, the points
     * and the turn are the mark's own.
     */
    fun styledLike(source: Mark): Mark {
        val fillToo = pen.closed && source.pen.closed
        val ricetta = source.tint?.let { s ->
            if (fillToo) s else {
                val mine = tint
                if (mine != null) s.copy(fill = mine.fill, fillLight = mine.fillLight, fillAlpha = mine.fillAlpha)
                else s.copy(
                    fill = fill?.let { it or 0xFF000000.toInt() }, fillLight = 0f,
                    fillAlpha = fill?.let { (it ushr 24) / 255f } ?: s.fillAlpha
                )
            }
        }
        return copy(
            ink = source.ink, width = source.width, dashed = source.dashed,
            fill = if (fillToo) source.fill else fill, tint = ricetta,
            // ⚠️ The blur is an area's, like the fill (4.80): it passes between two closed shapes.
            blur = if (fillToo) source.blur else blur
        )
    }

    private companion object {
        /** Below this size, in pixels, a free hand box has no side to stretch. */
        const val FLAT = 0.5f
    }
}

/**
 * **How the module chose a mark's colours**: the swatch, its light and its opacity, for the
 * outline and the fill (G2, 4.60).
 *
 * ⚠️ The mark draws [Mark.ink] and [Mark.fill], which are these values already mixed: from them
 * alone the module could not tell which swatch was chosen and how far its light was moved, and
 * choosing a mark has to show its swatch chosen and its sliders where they were.
 */
data class Tint(
    val ink: Int,
    val inkLight: Float,
    val inkAlpha: Float,
    val fill: Int?,
    val fillLight: Float,
    val fillAlpha: Float
)

/** How a drawing becomes pixels, the same way on the stage and in the saved file. */
internal object Draw {

    /** The width slider, as fractions of the long side. */
    const val WIDTH_MIN = 0.001f
    const val WIDTH_MAX = 0.03f

    /**
     * The factory width: the slider at 40% of its travel since 4.50 (his note A on the 4.49 round:
     * *spessore con slider al 40%*). It was 60% from 4.42, and 0.004 before, near the bottom.
     */
    const val WIDTH = WIDTH_MIN + 0.4f * (WIDTH_MAX - WIDTH_MIN)

    /**
     * The factory light of the stroke: its slider at 25% of the travel, so -0.5 on -1..1 (note A:
     * *luminosità con slider al 25%*). ⚠️ A reading declared in the test item: 'slider al X%' is a
     * place on the travel, as for the width. It holds for the factory red only: a tap on a swatch
     * gives that swatch its own colour, by his rule of `4.45-02`.
     */
    const val INK_LIGHT = -0.5f

    /**
     * The factory opacity of the stroke: its slider (0.1 to 1) at 50% of the travel (note A:
     * *opacità con slider al 50%*), the same reading.
     */
    const val INK_ALPHA = 0.1f + 0.5f * (1f - 0.1f)

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
        // ⚠️ A blurring element is not drawn: [blurAreas] has already blurred the image under it.
        if (mark.points.isEmpty() || mark.blurs) return
        val pts = mark.points.map { Offset(it.x * w, it.y * h) }
        val a = pts.first()
        val b = pts.last()
        // ⚠️ A turned rectangle or ellipse is the same shape drawn on a turned canvas (4.70): its
        // fill, its outline, its round corners and its dashes all turn with it, with no second
        // drawing of the turned shape.
        val turned = mark.pen.closed && mark.angle != 0f
        val kept = if (turned) canvas.save() else -1
        if (turned) canvas.rotate(mark.angle, (a.x + b.x) / 2f, (a.y + b.y) / 2f)
        // ⚠️ The fill goes first and on its own: its opacity is its own, and the outline's layer
        // below must not multiply it.
        if (mark.pen.closed) mark.fill?.let {
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = it
                style = Paint.Style.FILL
            }
            val box = RectF(min(a.x, b.x), min(a.y, b.y), max(a.x, b.x), max(a.y, b.y))
            if (mark.pen == Pen.ELLIPSE) canvas.drawOval(box, fill)
            else {
                val r = corner(mark, long, box)
                canvas.drawRoundRect(box, r, r, fill)
            }
        }
        /*
         * ⚠️⚠️ **The outline's opacity is laid on the whole mark, through a layer** (Opacità of
         * Traccia, 4.47, his answer `S1`): drawn with a translucent paint, every place where two
         * strokes of the same mark cross (the arrow's head on its shaft, the round caps of the
         * dashes, a free hand stroke over itself) would come out darker than the rest.
         */
        val alpha = mark.ink ushr 24
        val layer = if (alpha < 255) canvas.saveLayerAlpha(null, alpha) else -1
        strokeOf(canvas, mark, pts, long)
        if (layer >= 0) canvas.restoreToCount(layer)
        if (kept >= 0) canvas.restoreToCount(kept)
    }

    /**
     * **The outline of a turned rectangle or ellipse**, as points in fractions of a [w] x [h]
     * original (4.70): enough of them that the box they span is the shape's box on the screen,
     * round corners included. The stage reads it to lay a turned element on an edge.
     * ⚠️ Points and not a box: the geometry can bend the outline, and only points go through it.
     */
    fun outline(mark: Mark, w: Int, h: Int): List<Offset> = outline(mark, w.toFloat(), h.toFloat())

    private fun outline(mark: Mark, fw: Float, fh: Float): List<Offset> {
        if (mark.points.isEmpty()) return emptyList()
        val a = Offset(mark.points.first().x * fw, mark.points.first().y * fh)
        val b = Offset(mark.points.last().x * fw, mark.points.last().y * fh)
        val c = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
        val hw = abs(b.x - a.x) / 2f
        val hh = abs(b.y - a.y) / 2f
        val rad = Math.toRadians(mark.angle.toDouble())
        val cs = cos(rad).toFloat()
        val sn = sin(rad).toFloat()
        val local = mutableListOf<Offset>()
        if (mark.pen == Pen.ELLIPSE) {
            for (i in 0 until OUTLINE_STEPS) {
                val t = 2.0 * Math.PI * i / OUTLINE_STEPS
                local += Offset(hw * cos(t).toFloat(), hh * sin(t).toFloat())
            }
        } else {
            // ⚠️⚠️ One way round, corner after corner: top left from the left side to the top, top
            // right from the top to the right side, and so on. In 4.80 the arcs of the top right
            // and bottom left corners ran backwards, so the outline crossed itself: harmless for
            // the box it spans (4.70), and a slanted area with two loose corners once Sfocatura
            // cut the image along it (his `Non approvato` on `4.80-01`).
            val r = corner(mark, max(fw, fh), RectF(c.x - hw, c.y - hh, c.x + hw, c.y + hh))
            for ((k, s) in listOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f).withIndex()) {
                val o = Offset(s.first * (hw - r), s.second * (hh - r))
                for (i in 0..CORNER_STEPS) {
                    val t = Math.PI * (1.0 + k / 2.0) + Math.PI / 2 * i / CORNER_STEPS
                    local += Offset(o.x + r * cos(t).toFloat(), o.y + r * sin(t).toFloat())
                }
            }
        }
        return local.map { Offset((c.x + it.x * cs - it.y * sn) / fw, (c.y + it.x * sn + it.y * cs) / fh) }
    }

    /** How many points [outline] takes on an ellipse, and on each round corner of a rectangle. */
    private const val OUTLINE_STEPS = 72
    private const val CORNER_STEPS = 6

    /**
     * **[deg] on the nearest multiple of 45 degrees when it is within [SNAP_DEG] of it**, and in
     * the range from -180 to 180 (4.70, the turn of 'Ruota'). The same 5 degrees as the lines on the
     * horizontal and the vertical, so the two snaps feel alike under the finger.
     */
    fun snapTurn(deg: Float): Float {
        var d = deg % 360f
        if (d > 180f) d -= 360f
        if (d <= -180f) d += 360f
        val m = Math.round(d / 45f) * 45f
        val out = if (abs(d - m) <= SNAP_DEG) m else d
        return if (out <= -180f) out + 360f else out
    }

    /** The corner radius of a rectangle [box]: it follows the stroke, never past half a side. */
    private fun corner(mark: Mark, long: Float, box: RectF): Float =
        min((mark.width * long).coerceAtLeast(1f) * CORNER, min(box.width(), box.height()) / 2f)

    /** The outline of [mark], opaque: [paint] lays its opacity. */
    private fun strokeOf(canvas: Canvas, mark: Mark, pts: List<Offset>, long: Float) {
        val stroke = (mark.width * long).coerceAtLeast(1f)
        val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mark.ink or 0xFF000000.toInt()
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            // ⚠️ The dashes scale with the stroke, so a thick line is not a row of dots.
            if (mark.dashed) pathEffect = DashPathEffect(floatArrayOf(stroke * DASH, stroke * GAP), 0f)
        }
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
                if (mark.pen == Pen.ELLIPSE) canvas.drawOval(box, pen)
                else {
                    val r = corner(mark, long, box)
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

    /** The Sfocatura slider, as fractions of the longer side of the element (his figures). */
    const val BLUR_MIN = 0.005f
    const val BLUR_MAX = 0.25f

    /**
     * The factory blur: 10% of the element's longer side, enough that a face or a number plate
     * under it can no longer be read, which is the use he named (*oscuramento di parti di immagini
     * che voglio nascondere prima della condivisione*). A choice of the session, declared in the
     * test item.
     */
    const val BLUR = 0.1f

    /**
     * **[target] with the image under every blurring element of [drawing] blurred** (4.80), in the
     * original frame of the image: [at] says which part of the original [target] holds, as
     * fractions (the whole of it, or the piece the stage reads at full resolution).
     *
     * ⚠️⚠️ **Before the development and before every other element, on the stage and in the saved
     * file alike**, like the patches of Correggi: the colour sliders then develop the blurred
     * pixels, the geometry and the crop carry the blurred area with the image, and the other
     * elements are drawn over it (his specification: *con l'esclusione di tutti gli altri elementi
     * di Disegno (anzi, per definizione la sfocatura sarà sempre al livello più basso)*).
     * ⚠️ **The radius is a fraction of the element's longer side**, measured in the pixels of
     * [target], so the preview and the full file blur in the same proportion.
     * ⚠️ **How**: the area around the element, grown by the radius, is halved until the radius is
     * a few pixels (each halving averages four pixels, so nothing is skipped), blurred there with
     * three box passes, which come close to a Gaussian, and laid back through the element's own
     * outline, stretched with a bilinear filter. A blur of hundreds of pixels costs as much as one
     * of a few.
     * ⚠️ It draws on [target] when [mine] says the caller owns it and it is mutable, and on a copy
     * otherwise: the stage's preview must never be touched.
     */
    fun blurAreas(target: Bitmap, drawing: Drawing, mine: Boolean, at: RectF = WHOLE): Bitmap {
        val zone = drawing.marks.filter { it.blurs && it.points.isNotEmpty() }
        if (zone.isEmpty() || at.width() <= 0f || at.height() <= 0f) return target
        val out = if (mine && target.isMutable && target.config != Bitmap.Config.HARDWARE) target
        else target.copy(Bitmap.Config.ARGB_8888, true)
        val ow = out.width / at.width()
        val oh = out.height / at.height()
        val canvas = Canvas(out)
        for (mark in zone) {
            val path = Path()
            outline(mark, ow, oh).forEachIndexed { i, f ->
                val x = (f.x - at.left) * ow
                val y = (f.y - at.top) * oh
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            val a = mark.points.first()
            val b = mark.points.last()
            val radius = (mark.blur ?: BLUR) * max(abs(b.x - a.x) * ow, abs(b.y - a.y) * oh)
            if (radius < 0.5f) continue
            val box = RectF().also { path.computeBounds(it, true) }
            val area = android.graphics.Rect(
                (box.left - radius).toInt().coerceAtLeast(0), (box.top - radius).toInt().coerceAtLeast(0),
                kotlin.math.ceil(box.right + radius).toInt().coerceAtMost(out.width),
                kotlin.math.ceil(box.bottom + radius).toInt().coerceAtMost(out.height)
            )
            if (area.width() <= 0 || area.height() <= 0) continue
            val down = max(1f, radius / BLUR_SMALL)
            val sw = max(1, (area.width() / down).toInt())
            val sh = max(1, (area.height() / down).toInt())
            val small = shrink(out, area, sw, sh)
            boxBlur(small, max(1, Math.round(radius / down)))
            val shader = android.graphics.BitmapShader(small, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
            shader.setLocalMatrix(Matrix().apply {
                setScale(area.width().toFloat() / sw, area.height().toFloat() / sh)
                postTranslate(area.left.toFloat(), area.top.toFloat())
            })
            canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader })
            small.recycle()
        }
        return out
    }

    /** The whole of the original image, as fractions: what [blurAreas] reads by default. */
    private val WHOLE = RectF(0f, 0f, 1f, 1f)

    /** The radius, in pixels, that [blurAreas] blurs at after halving the area. */
    private const val BLUR_SMALL = 4f

    /**
     * The [from] part of [src] made [toW] x [toH], halving it while it is more than twice as large:
     * a bilinear filter at half size averages two by two, and at a larger step it would skip pixels.
     */
    private fun shrink(src: Bitmap, from: android.graphics.Rect, toW: Int, toH: Int): Bitmap {
        val filtro = Paint(Paint.FILTER_BITMAP_FLAG)
        var cur: Bitmap? = null
        var w = from.width()
        var h = from.height()
        while (w / 2 >= toW && h / 2 >= toH && w > 1 && h > 1) {
            val next = Bitmap.createBitmap(w / 2, h / 2, Bitmap.Config.ARGB_8888)
            Canvas(next).drawBitmap(cur ?: src, if (cur == null) from else null, android.graphics.Rect(0, 0, w / 2, h / 2), filtro)
            cur?.recycle()
            cur = next
            w /= 2
            h /= 2
        }
        val last = Bitmap.createBitmap(toW, toH, Bitmap.Config.ARGB_8888)
        Canvas(last).drawBitmap(cur ?: src, if (cur == null) from else null, android.graphics.Rect(0, 0, toW, toH), filtro)
        cur?.recycle()
        return last
    }

    /** Three box passes of radius [r] on [bitmap], across and down, with the edges repeated. */
    private fun boxBlur(bitmap: Bitmap, r: Int) {
        val w = bitmap.width
        val h = bitmap.height
        val px = IntArray(w * h)
        bitmap.getPixels(px, 0, w, 0, 0, w, h)
        val tmp = IntArray(max(w, h))
        repeat(3) {
            for (y in 0 until h) pass(px, y * w, 1, w, r, tmp)
            for (x in 0 until w) pass(px, x, w, h, r, tmp)
        }
        bitmap.setPixels(px, 0, w, 0, 0, w, h)
    }

    /** One box pass along a row or a column of [n] pixels, from [start] by [step]. */
    private fun pass(px: IntArray, start: Int, step: Int, n: Int, r: Int, tmp: IntArray) {
        for (i in 0 until n) tmp[i] = px[start + i * step]
        var sa = 0; var sr = 0; var sg = 0; var sb = 0
        val span = 2 * r + 1
        fun at(i: Int) = tmp[i.coerceIn(0, n - 1)]
        for (i in -r..r) {
            val c = at(i)
            sa += c ushr 24; sr += (c shr 16) and 0xFF; sg += (c shr 8) and 0xFF; sb += c and 0xFF
        }
        for (i in 0 until n) {
            px[start + i * step] = ((sa / span) shl 24) or ((sr / span) shl 16) or ((sg / span) shl 8) or (sb / span)
            val out = at(i - r)
            val inn = at(i + r + 1)
            sa += (inn ushr 24) - (out ushr 24)
            sr += ((inn shr 16) and 0xFF) - ((out shr 16) and 0xFF)
            sg += ((inn shr 8) and 0xFF) - ((out shr 8) and 0xFF)
            sb += (inn and 0xFF) - (out and 0xFF)
        }
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

    /**
     * **How far 'Duplica' lays the copy from its original**, in fractions of a [w] x [h] original
     * (4.70): [DUPLICATE_SHIFT] of the long side, right and down on the original image, so the copy
     * shows beside the original instead of hiding it. A choice of the session, declared in the
     * test item.
     */
    fun duplicateShift(w: Int, h: Int): Offset {
        val long = max(w, h).toFloat()
        return Offset(DUPLICATE_SHIFT * long / w.coerceAtLeast(1), DUPLICATE_SHIFT * long / h.coerceAtLeast(1))
    }

    private const val DUPLICATE_SHIFT = 0.03f

    /**
     * **[b] laid onto the horizontal or the vertical through [a]** when the line from [a] to [b] is
     * within [SNAP_DEG] of it, with that axis; otherwise [b] itself and `null` (his note A on the
     * 4.45 round: *all'avvicinarsi della direzione perfettamente orizzontale e perfettamente
     * verticale fossero posizionate PRECISAMENTE sulla direttrice corrispondente*).
     *
     * ⚠️⚠️ **It works in screen pixels, not in the image's frame**: the horizontal he means is the
     * one he sees, and on a straightened or mirrored image the two differ.
     * ⚠️ The point is projected, not turned: only the coordinate across the axis changes, so the
     * end stays under the finger along the axis.
     */
    fun snap(a: Offset, b: Offset): Pair<Offset, SnapAxis?> {
        val dx = abs(b.x - a.x)
        val dy = abs(b.y - a.y)
        if (dx == 0f && dy == 0f) return b to null
        val deg = Math.toDegrees(atan2(dy, dx).toDouble())
        return when {
            deg <= SNAP_DEG -> Offset(b.x, a.y) to SnapAxis.HORIZONTAL
            deg >= 90.0 - SNAP_DEG -> Offset(a.x, b.y) to SnapAxis.VERTICAL
            else -> b to null
        }
    }

    /**
     * **The index of the topmost mark of [drawing] under [at]**, or `null` (G2, 4.60: *un tocco
     * singolo seleziona un oggetto*). [at] is in fractions of the original [w] x [h] image, and
     * [reach] is how far from the line a touch still takes it, as a fraction of the long side, on
     * top of half the line's width.
     *
     * ⚠️ The distances are measured on the long side of the image, as the width is: in fractions
     * of each side a rectangle that is not square would take a touch further on one axis.
     * ⚠️ A closed shape with a fill is taken anywhere inside, one without only near its line: its
     * inside shows the image, and a touch there means the image. The arrow is taken by its shaft.
     */
    fun hit(drawing: Drawing, at: Offset, w: Int, h: Int, reach: Float): Int? {
        if (w <= 0 || h <= 0) return null
        val long = max(w, h).toFloat()
        val sx = w / long
        val sy = h / long
        val p = Offset(at.x * sx, at.y * sy)
        // ⚠️ The blurring elements lie under all the others whatever their place in the list
        // (4.80), so a touch tries them last.
        for (sotto in listOf(false, true)) for (i in drawing.marks.indices.reversed()) {
            val mark = drawing.marks[i]
            if (mark.blurs != sotto) continue
            if (touches(mark, mark.points.map { Offset(it.x * sx, it.y * sy) }, p, reach + mark.width / 2f)) return i
        }
        return null
    }

    private fun touches(mark: Mark, pts: List<Offset>, at: Offset, r: Float): Boolean {
        if (pts.isEmpty()) return false
        val a = pts.first()
        val b = pts.last()
        /*
         * ⚠️ A turned rectangle or ellipse (4.70) is tested with the touch turned back around its
         * centre, so the box below stays the box it was drawn in. The coordinates here are the
         * image's pixels scaled by its long side, where a turn is still a turn.
         */
        val p = if (!mark.pen.closed || mark.angle == 0f) at else {
            val cx = (a.x + b.x) / 2f
            val cy = (a.y + b.y) / 2f
            val rad = Math.toRadians(mark.angle.toDouble())
            val cs = cos(rad).toFloat()
            val sn = sin(rad).toFloat()
            val dx = at.x - cx
            val dy = at.y - cy
            Offset(cx + dx * cs + dy * sn, cy - dx * sn + dy * cs)
        }
        return when (mark.pen) {
            Pen.FREE -> if (pts.size == 1) hypot(p.x - a.x, p.y - a.y) <= r
            else pts.zipWithNext().any { (u, v) -> segment(p, u, v) <= r }
            Pen.LINE, Pen.ARROW -> segment(p, a, b) <= r
            Pen.RECT -> {
                val l = min(a.x, b.x); val rt = max(a.x, b.x); val t = min(a.y, b.y); val bt = max(a.y, b.y)
                val inside = p.x in l..rt && p.y in t..bt
                if (inside) mark.fill != null || mark.blurs || min(min(p.x - l, rt - p.x), min(p.y - t, bt - p.y)) <= r
                else hypot(max(max(l - p.x, 0f), p.x - rt), max(max(t - p.y, 0f), p.y - bt)) <= r
            }
            Pen.ELLIPSE -> {
                val cx = (a.x + b.x) / 2f; val cy = (a.y + b.y) / 2f
                val rx = abs(b.x - a.x) / 2f; val ry = abs(b.y - a.y) / 2f
                // ⚠️ A flat ellipse is a segment: the formula below would divide by zero.
                if (min(rx, ry) < 1e-4f) segment(p, a, b) <= r
                else {
                    val d = hypot((p.x - cx) / rx, (p.y - cy) / ry)
                    (d <= 1f && (mark.fill != null || mark.blurs)) || abs(d - 1f) * min(rx, ry) <= r
                }
            }
        }
    }

    /** The distance from [p] to the segment from [a] to [b]. */
    private fun segment(p: Offset, a: Offset, b: Offset): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = dx * dx + dy * dy
        val t = if (len <= 0f) 0f else (((p.x - a.x) * dx + (p.y - a.y) * dy) / len).coerceIn(0f, 1f)
        return hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy))
    }

    /**
     * How close to an axis a line snaps, in degrees. ⚠️ A choice of the session, declared in the
     * test item: at 5 degrees a line 3 cm long snaps when its end is within about 2.6 mm of the axis.
     */
    const val SNAP_DEG = 5.0

    /**
     * **The box on the screen that the outline of a two-point [pen] from [a] to [b] covers**
     * (4.62), for a stroke [stroke] pixels thick on an image whose long side is [long] pixels: the
     * shape's box with the arrow's head, grown by half the stroke, since the outline is centred on
     * the shape and its ends are round. The same numbers as [strokeOf], read on the screen.
     */
    fun extent(pen: Pen, a: Offset, b: Offset, stroke: Float, long: Float): Rect {
        var l = min(a.x, b.x)
        var t = min(a.y, b.y)
        var r = max(a.x, b.x)
        var bt = max(a.y, b.y)
        if (pen == Pen.ARROW && a != b) {
            val angle = atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble())
            val size = max(stroke * HEAD, long * HEAD_MIN)
            for (side in listOf(-1, 1)) {
                val th = angle + Math.PI + side * HEAD_ANGLE
                val x = b.x + (size * cos(th)).toFloat()
                val y = b.y + (size * sin(th)).toFloat()
                l = min(l, x); r = max(r, x); t = min(t, y); bt = max(bt, y)
            }
        }
        val h = stroke / 2f
        return Rect(l - h, t - h, r + h, bt + h)
    }

    /** The box on the screen that a free hand outline through [points] covers (4.62). */
    fun extent(points: List<Offset>, stroke: Float): Rect {
        val h = stroke / 2f
        return Rect(
            points.minOf { it.x } - h, points.minOf { it.y } - h,
            points.maxOf { it.x } + h, points.maxOf { it.y } + h
        )
    }

    /**
     * **The shift that lays the sides of [box] near the edges of [frame] on them** (4.62, his
     * request of 2026-10-08: *un piccolo scatto calamitato simile a quello delle linee
     * orizzontali/verticali che renda semplice far sì che una linea rimanga a filo del bordo
     * (totalmente visibile e a 0 pixel di distanza dal bordo)*), with the edges it rests on.
     *
     * ⚠️ A side within [reach] of an edge comes onto it, from inside or from outside; farther, it
     * stays, so an element still goes past the edge (*dev'essere possibile ... anche OLTRE i
     * bordi*). On each axis the nearer edge wins, and only the sides in [sides] may move.
     * ⚠️ Screen pixels, like [snap]: the edge he means is the one he sees.
     */
    fun rest(box: Rect, frame: Rect, reach: Float, sides: Set<ImageEdge> = ImageEdge.entries.toSet()): Pair<Offset, Set<ImageEdge>> {
        fun axis(lo: Float, hi: Float, edgeLo: Float, edgeHi: Float, low: ImageEdge, high: ImageEdge): Pair<Float, ImageEdge?> {
            val toLo = edgeLo - lo
            val toHi = edgeHi - hi
            val canLo = low in sides && abs(toLo) <= reach
            val canHi = high in sides && abs(toHi) <= reach
            return when {
                canLo && (!canHi || abs(toLo) <= abs(toHi)) -> toLo to low
                canHi -> toHi to high
                else -> 0f to null
            }
        }
        val (dx, ex) = axis(box.left, box.right, frame.left, frame.right, ImageEdge.LEFT, ImageEdge.RIGHT)
        val (dy, ey) = axis(box.top, box.bottom, frame.top, frame.bottom, ImageEdge.TOP, ImageEdge.BOTTOM)
        return Offset(dx, dy) to setOfNotNull(ex, ey)
    }

    /**
     * **[b], the end the finger draws, moved so the outline of a two-point [pen] from [a] rests on
     * the edges of [frame]** (4.62), with the edges it rests on. Only the sides that [b] draws
     * move, so the start stays where it was laid, and only along the axes in [axes]: a line laid
     * on the horizontal keeps its height.
     * ⚠️ Up to three passes, because the arrow's head turns with the end it sits on, and a pass
     * that moves nothing ends early; for the other pens the first pass is exact.
     */
    fun restEnd(
        pen: Pen, a: Offset, b: Offset, stroke: Float, long: Float, frame: Rect, reach: Float,
        axes: Set<SnapAxis> = SnapAxis.entries.toSet()
    ): Pair<Offset, Set<ImageEdge>> {
        val start = extent(pen, a, a, stroke, long)
        var end = b
        repeat(3) {
            val box = extent(pen, a, end, stroke, long)
            val own = buildSet {
                if (SnapAxis.HORIZONTAL in axes) {
                    if (box.left < start.left - OWN) add(ImageEdge.LEFT)
                    if (box.right > start.right + OWN) add(ImageEdge.RIGHT)
                }
                if (SnapAxis.VERTICAL in axes) {
                    if (box.top < start.top - OWN) add(ImageEdge.TOP)
                    if (box.bottom > start.bottom + OWN) add(ImageEdge.BOTTOM)
                }
            }
            val (d, _) = rest(box, frame, reach, own)
            if (d == Offset.Zero) return end to on(box, frame)
            end += d
        }
        return end to on(extent(pen, a, end, stroke, long), frame)
    }

    /** The edges of [frame] that a side of [box] lies on, within a fraction of a pixel. */
    fun on(box: Rect, frame: Rect): Set<ImageEdge> = buildSet {
        if (abs(box.left - frame.left) < ON) add(ImageEdge.LEFT)
        if (abs(box.top - frame.top) < ON) add(ImageEdge.TOP)
        if (abs(box.right - frame.right) < ON) add(ImageEdge.RIGHT)
        if (abs(box.bottom - frame.bottom) < ON) add(ImageEdge.BOTTOM)
    }

    /** How far past the start's own box a side must be to belong to the end, in pixels. */
    private const val OWN = 0.01f

    /** How near a side must be to an edge to lie on it, in pixels. */
    private const val ON = 0.5f
}

/** The two directions a line or an arrow snaps to ([Draw.snap]). */
enum class SnapAxis { HORIZONTAL, VERTICAL }

/** The four edges of the visible image an element of the drawing rests on ([Draw.rest], 4.62). */
enum class ImageEdge { LEFT, TOP, RIGHT, BOTTOM }

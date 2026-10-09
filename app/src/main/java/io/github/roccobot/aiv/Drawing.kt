package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.graphics.fonts.FontFamily
import android.graphics.fonts.FontStyle
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.core.graphics.ColorUtils
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
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

/**
 * The five shapes of the first phase, the text and the pill of the third (4.90), and the panel
 * (4.91), in the order the module shows them.
 */
enum class Pen {
    FREE, LINE, ARROW, RECT, ELLIPSE, TEXT, PILL, PANEL;

    /** Whether this pen closes a shape, so a fill means something. */
    val closed: Boolean get() = this == RECT || this == ELLIPSE

    /** Whether this pen writes words: the text, the pill (4.90) and the panel (4.91). */
    val written: Boolean get() = this == TEXT || framed

    /**
     * **Whether this pen is a box drawn by its two corners, with its words fitted inside**: the pill
     * (4.90) and the panel (4.91), which differ only in their look.
     */
    val framed: Boolean get() = this == PILL || this == PANEL

    /**
     * Whether this pen is a box that turns by its [Mark.angle]: the rectangle and the ellipse
     * (4.70), and the text and the pill (4.90), which turn as a whole and keep their lines straight.
     */
    val boxed: Boolean get() = closed || written
}

/**
 * **The four faces of the text** (G3, 4.90, his choice of 2026-10-08 from the artefact of the
 * faces: *tengo: Roboto, Montserrat, Archivo Narrow, Literata*), each an upright and an italic
 * file in `res/font`, variable in their weight. Roboto is the factory face (*il predefinito
 * dev'essere roboto*). All four are under the SIL Open Font License, in `docs/fonts`.
 */
enum class Face(val upright: Int, val italic: Int, val label: String) {
    ROBOTO(R.font.roboto, R.font.roboto_italic, "Roboto"),
    MONTSERRAT(R.font.montserrat, R.font.montserrat_italic, "Montserrat"),
    ARCHIVO_NARROW(R.font.archivo_narrow, R.font.archivo_narrow_italic, "Archivo Narrow"),
    LITERATA(R.font.literata, R.font.literata_italic, "Literata")
}

/**
 * **Where the lines of a text or a pill sit** (4.90, his `B2` of 2026-10-08: *Aggiungo volentieri B2
 * e B3 sul testo*): to the left, in the middle (the factory value, as before) or to the right of the
 * text's box.
 */
enum class Align { LEFT, CENTER, RIGHT }

/**
 * **The words of a text element and how they look** (G3, 4.90): the three styles (*grassetto,
 * corsivo, barrato*), the label behind them and its colour. The text's own colour is [Mark.ink]
 * and its size [Mark.width], as for the other elements.
 */
data class Words(
    val text: String,
    val face: Face = Face.ROBOTO,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val strike: Boolean = false,
    /**
     * **Whether a label's strip sits behind the text**, one per line, the strips melting into one
     * shape (his example of 2026-10-08, *etichetta*). ⚠️ Until 4.90 a highlighter's band was the
     * other ground: since 4.91 it is gone (his note on `4.90-04`: *'Etichetta' è talmente ben fatta
     * che 'Evidenziato' non serve più a niente*), and `Sfondo` turns the strip on and off.
     */
    val label: Boolean = false,
    /** The colour of the strip, with its alpha; read only when [label] is on. */
    val ground: Int = Draw.LABEL_INK,
    /** Where the lines sit in the box (4.90, `B2`). */
    val align: Align = Align.CENTER,
    /**
     * **The width the lines wrap at**, as a fraction of the image's long side, or 0 for lines that
     * break only where Invio breaks them (4.90, his `B3`). Like the words it belongs to the element:
     * a style copied onto a text leaves it its own.
     */
    val wrap: Float = 0f
)

/**
 * **The typefaces of the four [Face]s**, loaded once from the app's resources (4.90). The drawing
 * is painted where there is no context to hand (the stage's overlay, the saved file), so the
 * editor and the save load them first; until then the text falls back on the system's face.
 */
internal object Faces {
    private data class Key(val face: Face, val italic: Boolean, val bold: Boolean)

    // ⚠️ A concurrent map: the editor loads the faces off the main thread, which paints with them.
    private val loaded = ConcurrentHashMap<Key, Typeface>()

    /** The weights of the variable files: regular and bold. */
    const val REGULAR = 400
    const val BOLD = 700

    /**
     * How many times the faces have been loaded: the keys that show a face read it, so they draw it
     * again once the faces arrive, the editor loading them off the main thread.
     */
    var loads by mutableIntStateOf(0)
        private set

    /** Loads the sixteen typefaces of the eight files, once: later calls find them loaded. */
    fun load(context: Context) {
        if (loaded.size == Face.entries.size * 4) return
        synchronized(loaded) {
            for (face in Face.entries) for (italic in listOf(false, true)) for (bold in listOf(false, true)) {
                val key = Key(face, italic, bold)
                if (loaded[key] == null) loaded[key] = build(context, key)
            }
        }
        loads++
    }

    /**
     * **One typeface, with its weight set on the font itself** (4.90).
     * ⚠️⚠️ The weight is a variation of the file, and it goes on the [android.graphics.fonts.Font]:
     * a variation set on the paint (`Paint.fontVariationSettings`) left the bold as wide as the
     * regular on the bench, which measured it. The editor that draws exists from Android 13, so
     * the builder of Android 10 is always there; below it the file opens at its own weight.
     * ⚠️ The system's fallback stays behind the face, for the scripts the four files do not cover.
     */
    private fun build(context: Context, key: Key): Typeface {
        val res = if (key.italic) key.face.italic else key.face.upright
        val weight = if (key.bold) BOLD else REGULAR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val slant = if (key.italic) FontStyle.FONT_SLANT_ITALIC else FontStyle.FONT_SLANT_UPRIGHT
                val font = android.graphics.fonts.Font.Builder(context.resources, res)
                    .setFontVariationSettings("'wght' $weight")
                    .setWeight(weight)
                    .setSlant(slant)
                    .build()
                // ⚠️ The typeface asks for the font's own style: the builder's default is 400 upright.
                return Typeface.CustomFallbackBuilder(FontFamily.Builder(font).build())
                    .setStyle(FontStyle(weight, slant)).build()
            }
        }
        return ResourcesCompat.getFont(context, res) ?: Typeface.DEFAULT
    }

    /** The typeface of [face], upright or [italic], regular or [bold]. */
    fun of(face: Face, italic: Boolean, bold: Boolean): Typeface =
        loaded[Key(face, italic, bold)] ?: Typeface.create(Typeface.DEFAULT, if (bold) BOLD else REGULAR, italic)
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
     * **How much a panel blurs the image under it**, as a fraction of the longer side of its box
     * (4.80 for the rectangle and the ellipse, his specification in the brief, round of 4.43: *da
     * 0,5% a 25% del lato maggiore dell'oggetto*), or `null` for the other pens. The blur is laid by
     * [Draw.blurAreas], with the image.
     * ⚠️⚠️ **Since 4.91 only the panel blurs** (his `Non approvato` on `4.81-01`: *l'area sfocata
     * non sarà più attributo di ogni forma, bensì uno strumento a parte. Si chiamerà 'Pannello'*):
     * until then a rectangle or an ellipse with a value ignored its line and its fill.
     */
    val blur: Float? = null,
    /**
     * **The words of a text element** (G3, 4.90), or `null` for the other pens. A text has one
     * point, the centre of its box, and the box is measured from the words every time it is needed
     * ([Draw.textHalf]): a second copy of its size could drift from the words. A pill (4.90) keeps
     * the two corners of its box, as a rectangle, and its words fit inside ([Draw.textFrame]).
     */
    val words: Words? = null
) {
    /** Whether this element lays a glass under itself: a pill always, a panel with its [blur]. */
    val glass: Boolean get() = pen == Pen.PILL || (pen == Pen.PANEL && blur != null)

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
        val rad = Math.toRadians(angle.toDouble())
        if (pen.written) {
            val (c, hw, hh) = Draw.textFrame(this, w, h)
            return Frame(c, hw, hh, cos(rad).toFloat(), sin(rad).toFloat())
        }
        val xs = if (pen == Pen.FREE) points.map { it.x * w } else listOf(points.first().x * w, points.last().x * w)
        val ys = if (pen == Pen.FREE) points.map { it.y * h } else listOf(points.first().y * h, points.last().y * h)
        val l = xs.min(); val r = xs.max(); val t = ys.min(); val b = ys.max()
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
     *   in the test item). The text and the pill (4.90) have the same eight.
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
        if (pen == Pen.TEXT) {
            /*
             * ⚠️⚠️ **The middles of the left and the right side set the width the lines wrap at,
             * since 4.90** (his `B3`: the text goes to a new line by itself): the opposite side stays
             * and the words flow again, at least one size wide. The other six handles scale the text.
             */
            val parole = words
            if ((handle == 5 || handle == 7) && parole != null) {
                val long = max(fw, fh)
                val pad = if (parole.label) width * long * Draw.PAD else 0f
                val least = 2f * pad + width * long
                val wide = max(least, if (handle == 5) p.x + f.hw else f.hw - p.x)
                val cx = if (handle == 5) -f.hw + wide / 2f else f.hw - wide / 2f
                val c = f.out(cx, 0f)
                return copy(points = listOf(Offset(c.x / fw, c.y / fh)), words = parole.copy(wrap = (wide - 2f * pad) / long))
            }
            /*
             * ⚠️⚠️ **A text grows and shrinks as a whole, around its centre** (4.90): its box is
             * its words measured, so a handle sets the size, by how far along its own direction it
             * is dragged. A text stretched along one axis would be a distorted face. The width it
             * wraps at grows with it, so the lines stay the same.
             */
            val h0 = listOf(
                -f.hw to -f.hh, f.hw to -f.hh, f.hw to f.hh, -f.hw to f.hh,
                0f to -f.hh, f.hw to 0f, 0f to f.hh, -f.hw to 0f
            ).getOrNull(handle) ?: return this
            val d = h0.first * h0.first + h0.second * h0.second
            if (d < 1f) return this
            val scale = (p.x * h0.first + p.y * h0.second) / d
            val corpo = (width * scale).coerceIn(Draw.TEXT_MIN, Draw.TEXT_MAX)
            return copy(width = corpo, words = parole?.let { it.copy(wrap = it.wrap * corpo / width) })
        }
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
        if (pen.boxed) return copy(angle = Draw.snapTurn(angle + delta))
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
        /*
         * ⚠️ **Between two texts the style is the text's** (4.90): colour, size, face, the three
         * styles, the label and where the lines sit, the words being the element's own. Between a text and a shape
         * only the colour passes: a size is not a line's width, and a shape has no face.
         * ⚠️ **A pill has its own look and no colour to give or take** (his note A on the 4.43
         * round: *senza dover configurare ogni volta tratto, riempimento, opacità*): from a text or
         * to one only the face and the three styles pass. Between two pills the colour passes too
         * (4.91), as a fill passes between two closed shapes.
         */
        if (pen.written && source.pen.written) {
            val mine = words ?: return this
            val theirs = source.words ?: return this
            if (pen.framed || source.pen.framed) {
                return copy(
                    fill = if (pen == source.pen) source.fill else fill,
                    blur = if (pen == source.pen) source.blur else blur,
                    words = mine.copy(face = theirs.face, bold = theirs.bold, italic = theirs.italic, strike = theirs.strike, align = theirs.align)
                )
            }
            return copy(ink = source.ink, width = source.width, tint = source.tint, words = theirs.copy(text = mine.text, wrap = mine.wrap))
        }
        if (pen.framed || source.pen.framed) return this
        if (pen == Pen.TEXT || source.pen == Pen.TEXT) {
            // ⚠️ The recipe goes with the colour: the module loads the swatch from [tint], so a
            // recipe left behind would show the old colour once the element is chosen.
            return copy(
                ink = (source.ink and 0xFFFFFF) or (ink and 0xFF000000.toInt()),
                tint = tint?.copy(ink = source.tint?.ink ?: (source.ink or 0xFF000000.toInt()), inkLight = source.tint?.inkLight ?: 0f)
            )
        }
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
     * **The sizes of the text, as fractions of the long side, and its factory size** (4.90).
     * ⚠️⚠️ **The largest is 1,5 since 4.91** (his note on `4.90-02`: *voglio poter fare un testo
     * grande come l'intera immagine e anche oltre*; 1,1 or more, the number is the session's,
     * declared in the test item): one letter as tall as the image and half again. Until 4.90 it
     * was 0,2.
     */
    const val TEXT_MIN = 0.01f
    const val TEXT_MAX = 1.5f
    const val TEXT = 0.05f

    /**
     * **Where the size slider is for a text of [size]**, from 0 to 1, and back with [textSize]
     * (4.91). ⚠️⚠️ **The scale is a ratio's, not a difference's**: from 0,01 to 1,5 an even slider
     * would leave the sizes a text is written at (0,02 to 0,1) in the first twentieth of the track,
     * under the width of a finger. Each step along the track multiplies the size by the same amount.
     */
    fun textTrack(size: Float): Float =
        (ln(size.coerceIn(TEXT_MIN, TEXT_MAX) / TEXT_MIN) / ln(TEXT_MAX / TEXT_MIN)).coerceIn(0f, 1f)

    fun textSize(track: Float): Float = (TEXT_MIN * exp(track.coerceIn(0f, 1f) * ln(TEXT_MAX / TEXT_MIN)))
        .coerceIn(TEXT_MIN, TEXT_MAX)

    /** The factory colour of the text: white (his factory label, *testo #FFFFFF*; reading `A4`). */
    const val TEXT_INK = 0xFFFFFFFF.toInt()

    /** The factory strip of the label (his value of 2026-10-08). */
    const val LABEL_INK = 0xFFA3408F.toInt()

    /**
     * **The strips the module offers** (4.90, his note: *il colore dev'essere selezionabile tra 4-8
     * colori proposti da te*): six, his factory one first. The strip holds white words, so its
     * colours are deep. [readable] says which of them stand out where the text lies.
     */
    val LABEL_INKS = listOf(
        LABEL_INK, 0xFF1F5FA8.toInt(), 0xFF1E7A4A.toInt(), 0xFFB3261E.toInt(), 0xFFB35400.toInt(), 0xFF262626.toInt()
    )

    /**
     * **Whether a ground [ground] stands out from the image [under] it and keeps [ink] readable**
     * (4.90, his note: *solo colori che contrastano a sufficienza*, read as 'where the label lies':
     * reading `A5`, declared in the test item): at least [GROUND_DISTANCE] from the image in the
     * CIELAB space, and at least [WORDS_CONTRAST] of the WCAG's contrast ratio against the words.
     * ⚠️⚠️ **Two measures, since the two questions differ**: the words are read by their lightness
     * against the ground, and the ground stands out by its hue too, so a strip as light as the
     * image under it can still stand out from it by its colour.
     */
    fun readable(ground: Int, under: Int, ink: Int): Boolean {
        val a = DoubleArray(3)
        val b = DoubleArray(3)
        ColorUtils.colorToLAB(ground or OPAQUE, a)
        ColorUtils.colorToLAB(under or OPAQUE, b)
        return ColorUtils.distanceEuclidean(a, b) >= GROUND_DISTANCE &&
            ColorUtils.calculateContrast(ink or OPAQUE, ground or OPAQUE) >= WORDS_CONTRAST
    }

    /**
     * **The colour of the words [ink] on the ground [ground]** (4.90, when the `Sfondo` key lays the
     * strip): white or black words take whichever of the two reads better on it, and words of
     * another colour stay, unless they would not be readable, and then turn white or black too.
     */
    fun wordsOn(ground: Int, ink: Int): Int {
        val fondo = ground or OPAQUE
        val parole = ink or OPAQUE
        val meglio = if (ColorUtils.calculateContrast(WHITE, fondo) >= ColorUtils.calculateContrast(BLACK, fondo)) WHITE else BLACK
        val neutre = parole == WHITE || parole == BLACK
        return if (neutre || ColorUtils.calculateContrast(parole, fondo) < WORDS_CONTRAST) meglio else parole
    }

    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val BLACK = 0xFF000000.toInt()

    /**
     * **The colour of [image] under [mark]'s box, or of the whole image without one** (4.90): the
     * average of its pixels, which [readable] holds the grounds against. The box is the text's
     * measured one, in the original frame the drawing lives in.
     */
    fun under(image: Bitmap, mark: Mark?): Int {
        val w = image.width
        val h = image.height
        if (w <= 0 || h <= 0) return 0xFF808080.toInt()
        val box = mark?.let { outline(it, w, h) }?.takeIf { it.isNotEmpty() }
        val l = ((box?.minOf { it.x } ?: 0f) * w).toInt().coerceIn(0, w - 1)
        val t = ((box?.minOf { it.y } ?: 0f) * h).toInt().coerceIn(0, h - 1)
        val r = ((box?.maxOf { it.x } ?: 1f) * w).toInt().coerceIn(l + 1, w)
        val b = ((box?.maxOf { it.y } ?: 1f) * h).toInt().coerceIn(t + 1, h)
        val piccola = Bitmap.createScaledBitmap(Bitmap.createBitmap(image, l, t, r - l, b - t), UNDER, UNDER, true)
        var sr = 0L; var sg = 0L; var sb = 0L
        for (y in 0 until UNDER) for (x in 0 until UNDER) {
            val c = piccola.getPixel(x, y)
            sr += (c shr 16) and 0xFF; sg += (c shr 8) and 0xFF; sb += c and 0xFF
        }
        val n = UNDER * UNDER
        return (0xFF shl 24) or ((sr / n).toInt() shl 16) or ((sg / n).toInt() shl 8) or (sb / n).toInt()
    }

    /** The side of the small copy whose pixels [under] averages. */
    private const val UNDER = 8

    /**
     * How much a ground must differ from the image under it (a distance in CIELAB, where 2 or so is
     * the least difference an eye sees), and the words from the ground (the WCAG's large text).
     */
    const val GROUND_DISTANCE = 20.0
    const val WORDS_CONTRAST = 3.0
    private const val OPAQUE = 0xFF000000.toInt()

    /**
     * The text's lines, as multiples of its size: the step between two lines, the ground's margin
     * at the ends of a line, the strip's round corners, and the strip's shadow (blur and drop).
     * Choices of the session, declared in the test item.
     */
    private const val LINE = 1.3f
    const val PAD = 0.35f
    private const val ROUND = 0.3f
    private const val SHADOW_BLUR = 0.12f
    private const val SHADOW_DROP = 0.06f
    private const val SHADOW_INK = 0x40000000

    /** A text laid out: its paint, its lines with their widths, and the measures the box reads. */
    private class Lines(
        val paint: Paint, val lines: List<String>, val widths: List<Float>,
        val size: Float, val step: Float, val cap: Float, val pad: Float, val hw: Float, val hh: Float,
        val content: Float, val align: Align
    ) {
        /** Where the middle of line [i] is, from the box's middle. */
        fun x(i: Int): Float = lineX(align, content, widths[i])
    }

    /** Where the middle of a line [line] wide sits, from the middle of a box [content] wide. */
    private fun lineX(align: Align, content: Float, line: Float): Float = when (align) {
        Align.LEFT -> (line - content) / 2f
        Align.CENTER -> 0f
        Align.RIGHT -> (content - line) / 2f
    }

    /**
     * **[text] broken into the lines that fit [wide] pixels with [paint]**: every line of the text
     * breaks where it no longer fits, as a paragraph does (4.90, `B3`).
     */
    private fun wrapped(text: String, paint: TextPaint, wide: Float): List<String> = text.split('\n').flatMap { riga ->
        if (riga.isEmpty()) return@flatMap listOf("")
        val layout = StaticLayout.Builder.obtain(riga, 0, riga.length, paint, wide.toInt().coerceAtLeast(1))
            .setIncludePad(false).build()
        (0 until layout.lineCount).map { riga.substring(layout.getLineStart(it), layout.getLineEnd(it)).trimEnd() }
    }

    /**
     * **[mark]'s words laid out for a [w] x [h] original** (4.90): one line per line of the text,
     * centred, [LINE] sizes apart; the box half as wide as the widest line, the ground's margin
     * included, and half as tall as the lines.
     * ⚠️⚠️ **The lines sit on the middle of the capitals, not on the face's own box** (his note:
     * *Literata ha una baseline stranamente bassa*): Literata's ascent is 1177 units for capitals of
     * 700, so a strip centred on its box would hold the words low. Centred on half the height of an
     * 'H' above the baseline, the four faces sit alike, with no correction for one face.
     * ⚠️ The weight comes with the typeface ([Faces]): Montserrat's file opens at its thinnest, 100.
     */
    private fun lines(mark: Mark, w: Float, h: Float): Lines? {
        val words = mark.words ?: return null
        val size = (mark.width * max(w, h)).coerceAtLeast(1f)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = Faces.of(words.face, words.italic, words.bold)
            textSize = size
            isStrikeThruText = words.strike
            textAlign = Paint.Align.CENTER
            color = mark.ink
        }
        val wrap = words.wrap * max(w, h)
        val lines = if (wrap > 0f) wrapped(words.text, paint, wrap) else words.text.split('\n')
        val widths = lines.map { paint.measureText(it) }
        val cap = android.graphics.Rect().also { paint.getTextBounds("H", 0, 1, it) }.height().toFloat()
        val step = size * LINE
        val pad = if (words.label) size * PAD else 0f
        val content = if (wrap > 0f) wrap else widths.maxOrNull() ?: 0f
        return Lines(paint, lines, widths, size, step, cap, pad, content / 2f + pad, lines.size * step / 2f, content, words.align)
    }

    /** **Half the width and half the height of [mark]'s text**, in the pixels of a [w] x [h] original (4.90). */
    fun textHalf(mark: Mark, w: Float, h: Float): Pair<Float, Float> =
        textFrame(mark, w, h).let { it.second to it.third }

    /**
     * **The centre of a text's or a pill's box and its two half sides**, in the pixels of a [w] x [h]
     * original, in the box's own axes (4.90): what the handles, the touch, the outline and the guides
     * read. A text's box is its words measured around its point; a pill's is the box drawn, grown
     * downward when its words need more room ([pill]).
     */
    fun textFrame(mark: Mark, w: Float, h: Float): Triple<Offset, Float, Float> {
        if (mark.points.isEmpty()) return Triple(Offset.Zero, 0f, 0f)
        if (mark.pen.framed) return pill(mark, w, h).let { Triple(it.c, it.hw, it.hh) }
        val t = lines(mark, w, h)
        return Triple(Offset(mark.points.first().x * w, mark.points.first().y * h), t?.hw ?: 0f, t?.hh ?: 0f)
    }

    /**
     * **The pill's look** (4.90, his note A on the 4.43 round, in ARGB: *il riempimento dev'essere
     * #ccff4b3d, deve applicare sotto di sé un effetto vetro con sfocatura 12px, e deve avere 2 tracce
     * sottili che aumentano in proporzione con le sue dimensioni. dall'interno: contorno pari al 0,5%
     * della misura del lato maggiore in colore #e6fffefa, poi 0,5% del lato maggiore di colore
     * #e6373737*; and the words *sempre bianco opaco, #ffffffff*).
     * ⚠️ **The two traces are inside the box**, the dark one on its edge and the light one within, so
     * the box drawn is the pill's whole extent and rests on the edges like a rectangle.
     * ⚠️⚠️ **The glass's 12 px are a fraction of the pill's long side** (a reading of the session,
     * declared in the test item): 12 px on the screen of a pill half as wide as a phone's, about
     * 545 px. A fixed number of pixels would blur a pill on a large photo by nothing, and the same
     * pill in the preview and in the file by different amounts.
     */
    const val PILL_FILL = 0xCCFF4B3D.toInt()
    const val PILL_LIGHT = 0xE6FFFEFA.toInt()
    const val PILL_DARK = 0xE6373737.toInt()
    const val PILL_WORDS = 0xFFFFFFFF.toInt()
    const val PILL_TRACE = 0.005f
    const val PILL_BLUR = 12f / 545f

    /**
     * **The pill's colours** (4.91, his note on `4.90-05`: *si potesse selezionare il colore di sfondo
     * della pillola, ma devo avere a disposizione una palette diversa (proponi tu: tutti colori
     * 'stravaganti', neon e ben visibili); gli altri parametri come bordo, trasparenza, ecc. restano
     * invariati*): eight, opaque, his red first; the pill lays them at the alpha of [PILL_FILL].
     * ⚠️⚠️ **Vivid and not light**: each keeps the pill's white words at a contrast of 3 or more
     * (the WCAG's large text, [WORDS_CONTRAST]), so neon yellow and lime, which would hide them, are
     * out. Red, orange, pink, magenta, indigo, blue, teal and green: the session's choice, declared
     * in the test item.
     */
    val PILL_INKS = listOf(
        PILL_FILL or OPAQUE, 0xFFFF5A00.toInt(), 0xFFFF1F8E.toInt(), 0xFFD500F9.toInt(),
        0xFF6B2BFF.toInt(), 0xFF2962FF.toInt(), 0xFF009E9E.toInt(), 0xFF00A651.toInt()
    )

    /** **[ink] laid at the pill's alpha**, the one of [PILL_FILL] (4.91). */
    fun pillFill(ink: Int): Int = (ink and 0xFFFFFF) or (PILL_FILL and OPAQUE)

    /**
     * **The least size of a pill's words, as a fraction of the image's long side** (his note: *fino ad
     * un minimo di leggibilità (da definire)*): 2%, about 16 sp when the whole of a photo fills a
     * phone's screen. A reading of the session, declared in the test item.
     */
    const val PILL_MIN = 0.02f

    /**
     * How far the words keep from the pill's edge, beyond the two traces, as a part of the radius of
     * its round ends: at 0.35 the corners of a line of words stay inside the round ends.
     */
    private const val PILL_INSET = 0.35f

    /**
     * **The panel's look** (4.91, his `Non approvato` on `4.81-01`: *Forma: rettangolo con un
     * arrotondamento piccolissimo (0,3% del lato lungo). Sfocatura: come adesso ... Supporto testo:
     * esattamente come l'altra pillola. Colore ... si applica sempre e solo al 20% di opacità e deve
     * esserci anche 'nessuna'*): the radius of its corners as a part of its own long side (a reading
     * of the session, declared in the test item: the panel's and not the image's), and the alpha
     * of its colour. The words are the pill's, white.
     * ⚠️ No traces, which are the pill's: he listed the panel's attributes, and they were not there.
     */
    const val PANEL_ROUND = 0.003f
    private const val PANEL_ALPHA = 0x33000000

    /** **[ink] laid at the panel's 20%** (4.91). */
    fun panelFill(ink: Int): Int = (ink and 0xFFFFFF) or PANEL_ALPHA

    /**
     * How far the words keep from the panel's edge, as a part of its shorter half side: less than
     * the pill's, whose round ends take room. A choice of the session, declared in the test item.
     */
    private const val PANEL_INSET = 0.15f

    /** The margin of [pen]'s words from its edge, beyond the traces, as a part of the shorter half side. */
    private fun insetOf(pen: Pen): Float = if (pen == Pen.PANEL) PANEL_INSET else PILL_INSET

    /** A pill laid out: its box, its traces, and its words fitted. */
    private class Pill(
        val c: Offset, val hw: Float, val hh: Float, val trace: Float,
        val paint: Paint?, val lines: List<String>, val step: Float, val cap: Float,
        val content: Float = 0f, val align: Align = Align.CENTER
    )

    /**
     * **[mark]'s pill laid out for a [w] x [h] original** (4.90): the words wrap to the box's width
     * and take the largest size at which they fit its height (*si dispone al suo interno andando a capo
     * automaticamente e massimizzando la propria dimensione*), down to [PILL_MIN]. Words that do not
     * fit at that size keep it, and the box grows downward to hold them, its top edge staying where it
     * was drawn: a reading of the session, declared in the test item, since words cut off would be
     * lost without a word.
     * ⚠️ The lines are spaced and centred as a text's ([lines]), on the middle of the capitals.
     */
    private fun pill(mark: Mark, w: Float, h: Float): Pill {
        val a = Offset(mark.points.first().x * w, mark.points.first().y * h)
        val b = Offset(mark.points.last().x * w, mark.points.last().y * h)
        val c0 = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
        val hw = abs(b.x - a.x) / 2f
        val hh0 = abs(b.y - a.y) / 2f
        // ⚠️ A panel has no traces (4.91), so its words keep only their margin from the edge.
        val trace = if (mark.pen == Pen.PILL) PILL_TRACE * 2f * max(hw, hh0) else 0f
        val words = mark.words
        if (words == null || words.text.isBlank()) return Pill(c0, hw, hh0, trace, null, emptyList(), 0f, 0f)
        val inset = 2f * trace + insetOf(mark.pen) * min(hw, hh0)
        val wide = (2f * (hw - inset)).coerceAtLeast(1f)
        val tall = 2f * (hh0 - inset)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = Faces.of(words.face, words.italic, words.bold)
            isStrikeThruText = words.strike
            textAlign = Paint.Align.CENTER
            color = PILL_WORDS
        }
        fun laid(size: Float): List<String> {
            paint.textSize = size
            return wrapped(words.text, paint, wide)
        }
        val least = (PILL_MIN * max(w, h)).coerceAtLeast(1f)
        var lo = least
        var hi = max(least, tall / LINE)
        if (laid(hi).size * hi * LINE <= tall) lo = hi
        else repeat(FIT_STEPS) {
            val mid = (lo + hi) / 2f
            if (laid(mid).size * mid * LINE <= tall) lo = mid else hi = mid
        }
        val lines = laid(lo)
        val need = lines.size * lo * LINE
        val hh = max(hh0, need / 2f + inset)
        // ⚠️ The growth goes down the pill's own axes, so a turned pill grows along itself.
        val rad = Math.toRadians(mark.angle.toDouble())
        val grow = hh - hh0
        val c = Offset(c0.x - grow * sin(rad).toFloat(), c0.y + grow * cos(rad).toFloat())
        val cap = android.graphics.Rect().also { paint.getTextBounds("H", 0, 1, it) }.height().toFloat()
        return Pill(c, hw, hh, trace, paint, lines, lo * LINE, cap, wide, words.align)
    }

    /** How many halvings the fitting of a pill's words takes: a size within a thousandth. */
    private const val FIT_STEPS = 12

    /**
     * **Paints a pill** (4.90): the dark trace on the edge, the light one within, the fill inside,
     * then the white words. The glass under it is laid by [blurAreas], with the image.
     * ⚠️ A panel (4.91) is painted here too: its colour, when it has one, on its slightly round box,
     * then the same words.
     */
    private fun paintPill(canvas: Canvas, mark: Mark, w: Float, h: Float) {
        val p = pill(mark, w, h)
        val kept = canvas.save()
        canvas.translate(p.c.x, p.c.y)
        if (mark.angle != 0f) canvas.rotate(mark.angle)
        if (mark.pen == Pen.PANEL) {
            mark.fill?.let {
                val r = panelRound(p.hw, p.hh)
                canvas.drawRoundRect(RectF(-p.hw, -p.hh, p.hw, p.hh), r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = it
                    style = Paint.Style.FILL
                })
            }
            words(canvas, p)
            canvas.restoreToCount(kept)
            return
        }
        val r = min(p.hw, p.hh)
        val t = p.trace
        fun ring(inset: Float, colour: Int) {
            val box = RectF(-p.hw + inset, -p.hh + inset, p.hw - inset, p.hh - inset)
            if (box.width() <= 0f || box.height() <= 0f) return
            canvas.drawRoundRect(box, r - inset, r - inset, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colour
                style = Paint.Style.STROKE
                strokeWidth = t
            })
        }
        ring(t / 2f, PILL_DARK)
        ring(1.5f * t, PILL_LIGHT)
        val dentro = RectF(-p.hw + 2f * t, -p.hh + 2f * t, p.hw - 2f * t, p.hh - 2f * t)
        if (dentro.width() > 0f && dentro.height() > 0f) {
            canvas.drawRoundRect(dentro, r - 2f * t, r - 2f * t, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                // ⚠️ A pill made elsewhere (the tests) has no fill, and takes his red (4.91).
                color = mark.fill ?: PILL_FILL
                style = Paint.Style.FILL
            })
        }
        words(canvas, p)
        canvas.restoreToCount(kept)
    }

    /** The words of a laid out pill or panel, on a canvas centred on its box. */
    private fun words(canvas: Canvas, p: Pill) {
        p.paint?.let { paint ->
            val top = -p.lines.size * p.step / 2f
            p.lines.forEachIndexed { i, line ->
                canvas.drawText(line, lineX(p.align, p.content, paint.measureText(line)), top + p.step * (i + 0.5f) + p.cap / 2f, paint)
            }
        }
    }

    /** The radius of a panel's corners, for half sides [hw] and [hh] (4.91). */
    private fun panelRound(hw: Float, hh: Float): Float = min(PANEL_ROUND * 2f * max(hw, hh), min(hw, hh))

    /** **The radius of the glass under a pill**, in the pixels of a [w] x [h] original (4.90). */
    fun pillGlass(mark: Mark, w: Float, h: Float): Float = pill(mark, w, h).let { PILL_BLUR * 2f * max(it.hw, it.hh) }

    /**
     * **The box of a pill that holds [words] at [size]**, centred on [at], as its two corners in
     * fractions of a [w] x [h] original (4.90): a tap with Pillola lays a pill around its words, one
     * line per line written, with the same insets [pill] keeps. A tap with Pannello lays a panel
     * the same way (4.91), with its own margin and no traces ([pen]).
     */
    fun pillAround(words: Words, size: Float, at: Offset, w: Float, h: Float, pen: Pen = Pen.PILL): List<Offset> {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Faces.of(words.face, words.italic, words.bold)
            textSize = (size * max(w, h)).coerceAtLeast(1f)
        }
        val lines = words.text.split('\n')
        val wide = lines.maxOf { paint.measureText(it) }
        val tall = lines.size * paint.textSize * LINE
        // ⚠️ The insets are a part of the radius, which is half the box's height: solved for it.
        val traces = if (pen == Pen.PILL) 4f * PILL_TRACE else 0f
        val boxH = tall / (1f - insetOf(pen) - traces)
        val boxW = max(boxH, wide + boxH * (insetOf(pen) + traces) + 2f * paint.textSize * PILL_SIDE)
        val c = Offset(at.x * w, at.y * h)
        return listOf(Offset((c.x - boxW / 2f) / w, (c.y - boxH / 2f) / h), Offset((c.x + boxW / 2f) / w, (c.y + boxH / 2f) / h))
    }

    /** A little air at the ends of a pill laid by a tap, as a part of the words' size. */
    private const val PILL_SIDE = 0.2f

    /**
     * **Paints a text element** (4.90): the ground first, as one shape (the strips of the lines melt
     * into each other, as in his example), then the lines, centred on the element's point and turned
     * by its angle.
     * ⚠️ The label's strip has a soft shadow below (his example).
     */
    private fun paintText(canvas: Canvas, mark: Mark, w: Float, h: Float) {
        val words = mark.words ?: return
        val t = lines(mark, w, h) ?: return
        val kept = canvas.save()
        canvas.translate(mark.points.first().x * w, mark.points.first().y * h)
        if (mark.angle != 0f) canvas.rotate(mark.angle)
        if (words.label) {
            val ground = Path()
            t.lines.forEachIndexed { i, line ->
                if (line.isBlank()) return@forEachIndexed
                val mid = -t.hh + t.step * (i + 0.5f)
                val half = t.widths[i] / 2f + t.pad
                val x = t.x(i)
                val band = t.step / 2f
                val r = min(band, t.size * ROUND)
                ground.op(
                    Path().apply { addRoundRect(RectF(x - half, mid - band, x + half, mid + band), r, r, Path.Direction.CW) },
                    Path.Op.UNION
                )
            }
            canvas.drawPath(ground, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = words.ground
                style = Paint.Style.FILL
                setShadowLayer(t.size * SHADOW_BLUR, 0f, t.size * SHADOW_DROP, SHADOW_INK)
            })
        }
        t.lines.forEachIndexed { i, line ->
            val mid = -t.hh + t.step * (i + 0.5f)
            canvas.drawText(line, t.x(i), mid + t.cap / 2f, t.paint)
        }
        canvas.restoreToCount(kept)
    }

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
        if (mark.pen == Pen.TEXT) {
            paintText(canvas, mark, w, h)
            return
        }
        if (mark.pen.framed) {
            paintPill(canvas, mark, w, h)
            return
        }
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
        val rad = Math.toRadians(mark.angle.toDouble())
        val cs = cos(rad).toFloat()
        val sn = sin(rad).toFloat()
        if (mark.pen == Pen.TEXT) {
            // ⚠️ A text's box is its four corners, turned with it (4.90).
            val (c, hw, hh) = textFrame(mark, fw, fh)
            return listOf(-hw to -hh, hw to -hh, hw to hh, -hw to hh).map { (x, y) ->
                Offset((c.x + x * cs - y * sn) / fw, (c.y + x * sn + y * cs) / fh)
            }
        }
        // ⚠️ A pill's box is the one its words need (4.90): it can be taller than the one drawn.
        val (c, hw, hh) = if (mark.pen.framed) textFrame(mark, fw, fh) else {
            val a = Offset(mark.points.first().x * fw, mark.points.first().y * fh)
            val b = Offset(mark.points.last().x * fw, mark.points.last().y * fh)
            Triple(Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f), abs(b.x - a.x) / 2f, abs(b.y - a.y) / 2f)
        }
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
            // ⚠️ A pill is a rectangle with ends as round as they can be (4.90), a panel one with
            // corners barely round (4.91).
            val r = when (mark.pen) {
                Pen.PILL -> min(hw, hh)
                Pen.PANEL -> panelRound(hw, hh)
                else -> corner(mark, max(fw, fh), RectF(c.x - hw, c.y - hh, c.x + hw, c.y + hh))
            }
            val steps = if (mark.pen == Pen.PILL) PILL_STEPS else CORNER_STEPS
            for ((k, s) in listOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f).withIndex()) {
                val o = Offset(s.first * (hw - r), s.second * (hh - r))
                for (i in 0..steps) {
                    val t = Math.PI * (1.0 + k / 2.0) + Math.PI / 2 * i / steps
                    local += Offset(o.x + r * cos(t).toFloat(), o.y + r * sin(t).toFloat())
                }
            }
        }
        return local.map { Offset((c.x + it.x * cs - it.y * sn) / fw, (c.y + it.x * sn + it.y * cs) / fh) }
    }

    /** How many points [outline] takes on an ellipse, and on each round corner of a rectangle. */
    private const val OUTLINE_STEPS = 72
    private const val CORNER_STEPS = 6

    /** How many points [outline] takes on each quarter of a pill's round ends: the blur cuts along them. */
    private const val PILL_STEPS = 18

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
            // ⚠️ A text, a pill and a panel never reach here: [paint] draws them with [paintText]
            // and [paintPill].
            Pen.TEXT, Pen.PILL, Pen.PANEL -> Unit
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
        // ⚠️ A pill (4.90) and a panel (4.91) lay their glass here, and are drawn over it as the
        // other elements are.
        val zone = drawing.marks.filter { it.glass && it.points.isNotEmpty() }
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
            // ⚠️ A panel's blur is a part of the long side of the box its words need, as its outline is.
            val radius = if (mark.pen == Pen.PILL) pillGlass(mark, ow, oh)
            else textFrame(mark, ow, oh).let { (_, hw, hh) -> (mark.blur ?: BLUR) * 2f * max(hw, hh) }
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
        for (i in drawing.marks.indices.reversed()) {
            val mark = drawing.marks[i]
            if (mark.pen.written) {
                // ⚠️ A text, a pill and a panel are taken anywhere in their box, as a filled shape (4.90).
                if (mark.points.isEmpty()) continue
                val (c, hw, hh) = textFrame(mark, w.toFloat(), h.toFloat())
                val rad = Math.toRadians(mark.angle.toDouble())
                val dx = at.x * w - c.x
                val dy = at.y * h - c.y
                val lx = dx * cos(rad).toFloat() + dy * sin(rad).toFloat()
                val ly = -dx * sin(rad).toFloat() + dy * cos(rad).toFloat()
                if (abs(lx) <= hw + reach * long && abs(ly) <= hh + reach * long) return i
                continue
            }
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
                if (inside) mark.fill != null || min(min(p.x - l, rt - p.x), min(p.y - t, bt - p.y)) <= r
                else hypot(max(max(l - p.x, 0f), p.x - rt), max(max(t - p.y, 0f), p.y - bt)) <= r
            }
            // ⚠️ A text is tested in [hit], which has the image's sides to measure it with.
            Pen.TEXT, Pen.PILL, Pen.PANEL -> false
            Pen.ELLIPSE -> {
                val cx = (a.x + b.x) / 2f; val cy = (a.y + b.y) / 2f
                val rx = abs(b.x - a.x) / 2f; val ry = abs(b.y - a.y) / 2f
                // ⚠️ A flat ellipse is a segment: the formula below would divide by zero.
                if (min(rx, ry) < 1e-4f) segment(p, a, b) <= r
                else {
                    val d = hypot((p.x - cx) / rx, (p.y - cy) / ry)
                    (d <= 1f && mark.fill != null) || abs(d - 1f) * min(rx, ry) <= r
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
     * ⚠️⚠️ **Since 4.90 the boxes of the [others] are there to rest on too** (his request of
     * 2026-10-08: *Voglio che mi propongano di allineare dinamicamente gli elementi a
     * lati/centro/estremi di altri elementi già presenti*): a moving side, or the centre when both
     * sides move, comes onto a side or the centre of another element within the same [reach]. On
     * each axis the nearest of all wins, and on a tie the edge of the image; [lines] says which
     * guides to show.
     * ⚠️⚠️ **Since 4.91 the centre of the image is there too, for the centre of a moving element**
     * (his note A on the 4.90 round: *devono apparire anche delle guide per la centratura (che faccia
     * fare uno scatto allo spostamento di un elemento quando è al centro verticale/orizzontale/
     * entrambi dell'intera immagine)*): only the element's centre, since he named the centring, and
     * a side that stopped on the middle of the image would be a snap he did not ask for; and only
     * when [centred] says the element is being moved, so the start of a new one is not pulled there.
     */
    fun rest(
        box: Rect, frame: Rect, reach: Float, sides: Set<ImageEdge> = ImageEdge.entries.toSet(),
        others: List<Rect> = emptyList(), centred: Boolean = false
    ): Pair<Offset, Set<ImageEdge>> {
        fun axis(
            lo: Float, hi: Float, edgeLo: Float, edgeHi: Float, low: ImageEdge, high: ImageEdge,
            marks: List<Float>, middle: Float
        ): Pair<Float, ImageEdge?> {
            var best = 0f
            var edge: ImageEdge? = null
            var found = false
            // ⚠️ The first of two equal distances wins: the low edge, then the high one, then the
            // elements, as before 4.90.
            fun offer(d: Float, e: ImageEdge?) {
                if (abs(d) <= reach && (!found || abs(d) < abs(best))) {
                    best = d
                    edge = e
                    found = true
                }
            }
            val moveLo = low in sides
            val moveHi = high in sides
            if (moveLo) offer(edgeLo - lo, low)
            if (moveHi) offer(edgeHi - hi, high)
            val own = buildList {
                if (moveLo) add(lo)
                if (moveHi) add(hi)
                if (moveLo && moveHi) add((lo + hi) / 2f)
            }
            for (t in marks) for (v in own) offer(t - v, null)
            if (centred && moveLo && moveHi) offer(middle - (lo + hi) / 2f, null)
            return best to edge
        }
        val (dx, ex) = axis(
            box.left, box.right, frame.left, frame.right, ImageEdge.LEFT, ImageEdge.RIGHT,
            others.flatMap { listOf(it.left, it.center.x, it.right) }, frame.center.x
        )
        val (dy, ey) = axis(
            box.top, box.bottom, frame.top, frame.bottom, ImageEdge.TOP, ImageEdge.BOTTOM,
            others.flatMap { listOf(it.top, it.center.y, it.bottom) }, frame.center.y
        )
        return Offset(dx, dy) to setOfNotNull(ex, ey)
    }

    /**
     * **The guides between [box] and the [others] it lines up with** (4.90), as segments on the
     * screen: where a side or the centre of [box] meets a side or the centre of another box, a
     * line along it from the farther end of one to the farther end of the other, so the guide
     * says which element it lines up with. The same [ON] as the edges.
     * ⚠️ Since 4.91 a [box] centred on the middle of [frame], the image on the screen, has the guide
     * of that middle too, across the whole image (his note A on the 4.90 round).
     */
    fun lines(box: Rect, others: List<Rect>, frame: Rect? = null): List<Pair<Offset, Offset>> = buildList {
        if (frame != null) {
            if (abs(box.center.x - frame.center.x) < ON) add(Offset(frame.center.x, frame.top) to Offset(frame.center.x, frame.bottom))
            if (abs(box.center.y - frame.center.y) < ON) add(Offset(frame.left, frame.center.y) to Offset(frame.right, frame.center.y))
        }
        val xs = listOf(box.left, box.center.x, box.right)
        val ys = listOf(box.top, box.center.y, box.bottom)
        for (o in others) {
            for (t in listOf(o.left, o.center.x, o.right)) if (xs.any { abs(it - t) < ON }) {
                add(Offset(t, min(box.top, o.top)) to Offset(t, max(box.bottom, o.bottom)))
            }
            for (t in listOf(o.top, o.center.y, o.bottom)) if (ys.any { abs(it - t) < ON }) {
                add(Offset(min(box.left, o.left), t) to Offset(max(box.right, o.right), t))
            }
        }
    }.distinct()

    /**
     * **[b], the end the finger draws, moved so the outline of a two-point [pen] from [a] rests on
     * the edges of [frame]** (4.62), with the edges it rests on. Only the sides that [b] draws
     * move, so the start stays where it was laid, and only along the axes in [axes]: a line laid
     * on the horizontal keeps its height.
     * ⚠️ Up to three passes, because the arrow's head turns with the end it sits on, and a pass
     * that moves nothing ends early; for the other pens the first pass is exact.
     * ⚠️ Since 4.90 the end rests on the [others] too, as in [rest].
     */
    fun restEnd(
        pen: Pen, a: Offset, b: Offset, stroke: Float, long: Float, frame: Rect, reach: Float,
        axes: Set<SnapAxis> = SnapAxis.entries.toSet(), others: List<Rect> = emptyList()
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
            val (d, _) = rest(box, frame, reach, own, others)
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

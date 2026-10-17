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
 * Il modulo **Disegno** dell'editor completo: mano libera, linee, frecce, rettangoli arrotondati,
 * ellissi, testo, pillola e pannello, con colore, spessore, tratteggio e riempimento.
 *
 * ⚠️ **Le quattro risposte dell'utente ne decidono la forma** (`d-disegno-*`):
 * - D1a: vive solo nell'editor completo, come ultimo modulo della fila, quindi sotto Android 13
 *   non c'è;
 * - D2a: il disegno è attaccato all'immagine, quindi gira, si deforma e si ritaglia con lei;
 * - D3a: si fonde nell'immagine salvata come la filigrana, e un file riaperto contiene pixel, non
 *   forme;
 * - D4a: tre versioni in fila (G1 forme, G2 selezione e maniglie, G3 testo).
 *
 * ⚠️ **I punti vivono nel riferimento ORIGINALE dell'immagine, in frazioni dei suoi lati**, come la
 * selezione di Correggi e Rimuovi: un quarto di giro o uno specchio dopo il disegno non cambiano
 * niente nel modello, e il disegno segue l'immagine da sé. Un punto nel riferimento della posa
 * andrebbe riscritto a ogni posa, come il rettangolo del ritaglio.
 *
 * ⚠️ **Si posa dopo lo sviluppo e prima della geometria**, sul palco e nel file salvato: dopo, così
 * i cursori del colore non ridipingono l'inchiostro scelto; prima, così raddrizzamento, trapezi,
 * distorsione dell'obiettivo, Fluidifica e ritaglio lo spostano con l'immagine (D2a). I due posti
 * chiamano le stesse funzioni di questo file.
 */
data class Drawing(val marks: List<Mark> = emptyList()) {

    /** Se non c'è niente di disegnato, e l'immagine esce esattamente com'è entrata. */
    val idle: Boolean get() = marks.isEmpty()

    /** Questo disegno con [mark] in cima. */
    fun with(mark: Mark): Drawing = Drawing(marks + mark)

    /** Questo disegno con l'elemento in [index] sostituito da [mark]. */
    fun replacing(index: Int, mark: Mark): Drawing =
        Drawing(marks.mapIndexed { i, m -> if (i == index) mark else m })

    /** Questo disegno senza l'elemento in [index]. */
    fun without(index: Int): Drawing = Drawing(marks.filterIndexed { i, _ -> i != index })

    /** Questo disegno con [mark] in [index], e gli elementi da lì in poi un posto più su. */
    fun inserting(index: Int, mark: Mark): Drawing =
        Drawing(marks.toMutableList().apply { add(index.coerceIn(0, size), mark) })

    /**
     * Questo disegno con gli elementi in [i] e [j] scambiati ('Sposta sopra' e 'Sposta sotto'):
     * l'elenco è l'ordine dei livelli, l'ultimo in cima.
     */
    fun swapping(i: Int, j: Int): Drawing {
        if (i !in marks.indices || j !in marks.indices) return this
        return Drawing(marks.toMutableList().apply { this[i] = marks[j]; this[j] = marks[i] })
    }

    companion object {
        val NONE = Drawing()
    }
}

/** Gli strumenti del modulo, nell'ordine in cui il modulo li mostra. */
enum class Pen {
    FREE, LINE, ARROW, RECT, ELLIPSE, TEXT, PILL, PANEL;

    /** Se lo strumento chiude una forma, e un riempimento ha senso. */
    val closed: Boolean get() = this == RECT || this == ELLIPSE

    /** Se lo strumento scrive parole: il testo, la pillola e il pannello. */
    val written: Boolean get() = this == TEXT || framed

    /**
     * Se lo strumento è un riquadro disegnato dai due angoli, con le parole adattate dentro: la
     * pillola e il pannello, che differiscono solo nell'aspetto.
     */
    val framed: Boolean get() = this == PILL || this == PANEL

    /**
     * Se lo strumento è un riquadro che gira col suo [Mark.angle]: il rettangolo, l'ellisse e gli
     * strumenti che scrivono, che girano interi e tengono dritte le righe.
     */
    val boxed: Boolean get() = closed || written
}

/**
 * I quattro caratteri del testo, scelti dall'utente (Roboto di fabbrica, Montserrat, Archivo
 * Narrow, Literata): ognuno un file dritto e uno corsivo in `res/font`, variabili nel peso. Tutti
 * sotto la SIL Open Font License, in `docs/fonts`.
 */
enum class Face(val upright: Int, val italic: Int, val label: String) {
    ROBOTO(R.font.roboto, R.font.roboto_italic, "Roboto"),
    MONTSERRAT(R.font.montserrat, R.font.montserrat_italic, "Montserrat"),
    ARCHIVO_NARROW(R.font.archivo_narrow, R.font.archivo_narrow_italic, "Archivo Narrow"),
    LITERATA(R.font.literata, R.font.literata_italic, "Literata")
}

/**
 * Dove sono le righe di un testo o di una pillola nel riquadro: a sinistra, al centro (di fabbrica)
 * o a destra (scelta `B2` dell'utente).
 */
enum class Align {
    LEFT, CENTER, RIGHT;

    /**
     * Il posto successivo del tasto `Allineamento`: centro, sinistra, destra (nota A dell'utente,
     * giro della 4.91).
     */
    val next: Align get() = when (this) {
        CENTER -> LEFT
        LEFT -> RIGHT
        RIGHT -> CENTER
    }
}

/**
 * Le parole di un elemento di testo e il loro aspetto: i tre stili (grassetto, corsivo, barrato),
 * l'etichetta dietro e il suo colore. Il colore del testo è [Mark.ink] e la sua dimensione
 * [Mark.width], come per gli altri elementi.
 */
data class Words(
    val text: String,
    val face: Face = Face.ROBOTO,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val strike: Boolean = false,
    /**
     * Se dietro il testo c'è la striscia dell'etichetta, una per riga, fuse in una forma sola. Il
     * tasto `Sfondo` la accende e la spegne.
     */
    val label: Boolean = false,
    /** Il colore della striscia, con l'alfa; si legge solo con [label] acceso. */
    val ground: Int = Draw.LABEL_INK,
    /** Dove sono le righe nel riquadro (`B2`). */
    val align: Align = Align.CENTER,
    /**
     * La larghezza a cui le righe vanno a capo, in frazione del lato lungo dell'immagine, o 0 per
     * righe che vanno a capo solo con Invio (`B3`). Come le parole, è dell'elemento: uno stile
     * copiato su un testo gli lascia la sua.
     */
    val wrap: Float = 0f
)

/**
 * I caratteri tipografici dei quattro [Face], caricati una volta dalle risorse dell'app. Il disegno
 * si dipinge dove non c'è un contesto a portata (la sovrapposizione del palco, il file salvato),
 * quindi l'editor e il salvataggio li caricano prima; fino ad allora il testo usa il carattere di
 * sistema.
 */
internal object Faces {
    private data class Key(val face: Face, val italic: Boolean, val bold: Boolean)

    // Una mappa concorrente: l'editor carica i caratteri fuori dal thread principale, che ci
    // dipinge.
    private val loaded = ConcurrentHashMap<Key, Typeface>()

    /** I pesi dei file variabili: normale e grassetto. */
    const val REGULAR = 400
    const val BOLD = 700

    /**
     * Quante volte i caratteri sono stati caricati: i tasti che mostrano un carattere lo leggono,
     * così si ridisegnano quando i caratteri arrivano.
     */
    var loads by mutableIntStateOf(0)
        private set

    /**
     * Carica una volta i sedici caratteri degli otto file: le chiamate successive li trovano
     * pronti.
     */
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
     * Un carattere, col peso impostato sul font stesso.
     * ⚠️ **Il peso è una variazione del file, e va sul [android.graphics.fonts.Font]**: impostata
     * sul paint (`Paint.fontVariationSettings`) lasciava il grassetto largo come il normale,
     * misurato sul banco. L'editor che disegna esiste da Android 13, quindi il costruttore di
     * Android 10 c'è sempre; sotto, il file si apre al suo peso. Il ripiego di sistema resta
     * dietro, per le scritture che i quattro file non coprono.
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
                // Il carattere chiede lo stile del font: quello di serie del costruttore è 400
                // dritto.
                return Typeface.CustomFallbackBuilder(FontFamily.Builder(font).build())
                    .setStyle(FontStyle(weight, slant)).build()
            }
        }
        return ResourcesCompat.getFont(context, res) ?: Typeface.DEFAULT
    }

    /** Il carattere di [face], dritto o [italic], normale o [bold]. */
    fun of(face: Face, italic: Boolean, bold: Boolean): Typeface =
        loaded[Key(face, italic, bold)] ?: Typeface.create(Typeface.DEFAULT, if (bold) BOLD else REGULAR, italic)
}

/**
 * Un elemento del disegno.
 *
 * ⚠️ **[points] sono frazioni dell'immagine originale**: x della larghezza, y dell'altezza. Un
 * tratto a mano libera tiene ogni punto toccato dal dito; gli altri tengono dove il dito si è
 * appoggiato e dove è (o si è alzato).
 * ⚠️ **[width] è una frazione del lato lungo dell'immagine originale**, così il tratto si vede
 * uguale nell'anteprima e nel file salvato a piena risoluzione.
 */
data class Mark(
    val pen: Pen,
    val points: List<Offset>,
    val ink: Int,
    val width: Float,
    val dashed: Boolean,
    /**
     * Il riempimento di una forma chiusa, colore con l'alfa, o `null` per nessuno; gli altri
     * strumenti lo ignorano. Ha colore e opacità suoi, separati dal contorno (esempio dell'utente:
     * un bordo rosso con un riempimento bianco al 50%).
     */
    val fill: Int?,
    /**
     * Come il modulo ha scelto [ink] e [fill], o `null` per un elemento fatto altrove (le prove):
     * il modulo lo ricarica quando l'elemento è scelto. Vedi [Tint].
     */
    val tint: Tint? = null,
    /**
     * La rotazione del riquadro intorno al suo centro, in gradi, in senso orario sull'immagine
     * originale ('Ruota'). [points] restano i due angoli del riquadro prima della rotazione.
     *
     * ⚠️ **Solo gli strumenti a riquadro ([Pen.boxed]) tengono un angolo**: una linea, una freccia
     * e un tratto a mano libera girano girando i loro punti, che dicono tutto di loro, e un secondo
     * posto per la stessa rotazione sarebbe una seconda verità. [turned] fa le due cose.
     * ⚠️ La rotazione è nei pixel dell'immagine originale, non nelle sue frazioni: su un'immagine
     * non quadrata una frazione ruotata deformerebbe la forma.
     */
    val angle: Float = 0f,
    /**
     * Quanto il pannello sfoca l'immagine sotto di sé, in frazione del lato lungo del suo riquadro
     * (dallo 0,5% al 25%, numeri dell'utente), o `null`. La sfocatura la posa [Draw.blurAreas], con
     * l'immagine. Solo il pannello sfoca: la sfocatura è uno strumento a parte (non approvato di
     * `4.81-01`).
     */
    val blur: Float? = null,
    /**
     * Le parole di un elemento di testo, o `null` per gli altri strumenti. Un testo ha un punto
     * solo, il centro del riquadro, e il riquadro si misura dalle parole ogni volta che serve
     * ([Draw.textFrame]): una seconda copia della misura potrebbe divergere dalle parole. Una
     * pillola e un pannello tengono i due angoli del riquadro, come un rettangolo, e le parole ci
     * si adattano dentro.
     */
    val words: Words? = null
) {
    /**
     * Se l'elemento posa un vetro sotto di sé: la pillola sempre, il pannello con la sua [blur].
     */
    val glass: Boolean get() = pen == Pen.PILL || (pen == Pen.PANEL && blur != null)

    /**
     * Questo elemento con l'ultimo punto spostato in [to], o aggiunto per un tratto a mano libera.
     */
    fun reaching(to: Offset): Mark = when (pen) {
        Pen.FREE -> copy(points = points + to)
        else -> copy(points = listOf(points.first(), to))
    }

    /** Questo elemento spostato di [by], in frazioni dell'immagine originale. */
    fun moved(by: Offset): Mark = copy(points = points.map { it + by })

    /**
     * Il riferimento in cui vivono le maniglie dell'elemento, nei pixel di un originale [w] x [h]:
     * il centro del riquadro, i semilati, e coseno e seno della rotazione. Per la mano libera il
     * riquadro contiene ogni punto, e la rotazione è zero.
     */
    private class Frame(val c: Offset, val hw: Float, val hh: Float, val cos: Float, val sin: Float) {
        /** Un punto negli assi del riquadro, dal suo centro, come punto dell'immagine. */
        fun out(x: Float, y: Float) = Offset(c.x + x * cos - y * sin, c.y + x * sin + y * cos)

        /** Un punto dell'immagine, negli assi del riquadro dal suo centro. */
        fun into(p: Offset): Offset {
            val dx = p.x - c.x
            val dy = p.y - c.y
            return Offset(dx * cos + dy * sin, -dx * sin + dy * cos)
        }

        /**
         * Le otto maniglie negli assi del riquadro, nell'ordine di [handles]: i quattro angoli, poi
         * il centro di ogni lato. Vive qui una volta sola perché [reshaped] legge lo stesso ordine.
         */
        fun eight() = listOf(
            -hw to -hh, hw to -hh, hw to hh, -hw to hh,
            0f to -hh, hw to 0f, 0f to hh, -hw to 0f
        )
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
     * Dove l'elemento scelto mostra le maniglie, in frazioni di un originale [w] x [h] (specifica
     * dell'utente: vertici e punti color accento per scegliere e ridimensionare).
     *
     * - Linea e freccia: i due estremi.
     * - Rettangolo, ellisse, mano libera e gli strumenti che scrivono: i quattro angoli del
     * riquadro,
     *   poi il centro di ogni lato, nell'ordine alto a sinistra, alto a destra, basso a destra,
     *   basso a sinistra, poi alto, destra, basso, sinistra negli assi del riquadro. I centri dei
     *   lati servono a stirare lungo un asse.
     *
     * ⚠️ L'indice di una maniglia è quello che [reshaped] legge, quindi l'ordine è un contratto.
     */
    fun handles(w: Int, h: Int): List<Offset> {
        if (points.isEmpty()) return emptyList()
        if (pen == Pen.LINE || pen == Pen.ARROW) return listOf(points.first(), points.last())
        val fw = w.toFloat()
        val fh = h.toFloat()
        val f = frame(fw, fh)
        return f.eight().map { (x, y) -> f.out(x, y).let { Offset(it.x / fw, it.y / fh) } }
    }

    /**
     * Questo elemento con la maniglia [handle] di [handles] trascinata in [to], in frazioni di un
     * originale [w] x [h] ('Trasforma').
     *
     * - Un angolo muove i suoi due lati e l'angolo opposto resta; il centro di un lato muove quel
     * lato
     *   e l'opposto resta. I lati sono quelli degli assi del riquadro, quindi un rettangolo ruotato
     *   si stira lungo se stesso e non lungo l'immagine.
     * - Un estremo di linea o di freccia va dove è trascinato.
     * - La mano libera stira i punti col riquadro, e trascinata oltre il lato opposto si specchia,
     *   come nei programmi di disegno; rettangolo ed ellisse, simmetrici, crescono dall'altra
     *   parte.
     *
     * ⚠️ Il gesto la chiama a ogni fotogramma con l'elemento com'era quando il dito si è
     * appoggiato: partendo dall'ultimo fotogramma gli arrotondamenti si sommerebbero.
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
             * ⚠️ **I centri dei lati sinistro e destro decidono la larghezza a cui le righe vanno a
             * capo** (`B3`): il lato opposto resta e le parole si ridispongono, larghe almeno una
             * dimensione. Le altre sei maniglie scalano il testo.
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
             * ⚠️ **Un testo cresce e cala intero, intorno al centro**: il riquadro sono le parole
             * misurate, quindi una maniglia decide la dimensione, secondo quanto è trascinata lungo
             * la sua direzione. Un testo stirato su un asse sarebbe un carattere deformato. La
             * larghezza di a capo cresce con lui, e le righe restano le stesse.
             */
            val h0 = f.eight().getOrNull(handle) ?: return this
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
            // Un tratto a mano libera non ha rotazione, quindi gli assi del riquadro sono quelli
            // dell'immagine: ogni punto tiene il suo posto nel riquadro. Un riquadro senza
            // larghezza (un tratto dritto) non ha niente da stirare su quell'asse.
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
     * Questo elemento ruotato di [delta] gradi, in senso orario, intorno al centro del riquadro, in
     * un originale [w] x [h] ('Ruota'), con lo scatto sui multipli di 45 gradi entro
     * [Draw.SNAP_DEG] (lettura R2 della domanda all'utente, dichiarata nella voce di collaudo).
     *
     * - Gli strumenti a riquadro girano il loro [angle], e lo scatto è sulla posizione d'arrivo.
     * - Linea e freccia girano i punti intorno al loro centro, e lo scatto è sulla direzione: una
     *   linea vicina all'orizzontale ci si posa.
     * - La mano libera gira i punti intorno al centro del riquadro, e lo scatto è su [delta]: un
     *   tratto non ha una direzione sua.
     *
     * ⚠️ Come [reshaped], si chiama con l'elemento di quando il dito si è appoggiato.
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

    /**
     * Il centro del riquadro, in frazioni di un originale [w] x [h]: quello intorno a cui [turned]
     * gira.
     */
    fun centre(w: Int, h: Int): Offset {
        if (points.isEmpty()) return Offset.Zero
        val f = frame(w.toFloat(), h.toFloat())
        return Offset(f.c.x / w, f.c.y / h)
    }

    /**
     * Questo elemento con lo stile di [source] (risposta dell'utente a `4.64-03`: lo stile si
     * applica nelle parti compatibili a un altro oggetto): colore, luminosità e opacità del
     * contorno, spessore e tratteggio sempre; il riempimento solo fra due forme chiuse. Strumento,
     * punti e rotazione restano dell'elemento.
     */
    fun styledLike(source: Mark): Mark {
        /*
         * ⚠️ **Fra due testi lo stile è quello del testo**: colore, dimensione, carattere, i tre
         * stili, l'etichetta e l'allineamento; le parole restano dell'elemento. Fra un testo e una
         * forma passa solo il colore: una dimensione non è uno spessore, e una forma non ha un
         * carattere.
         * ⚠️ **Pillola e pannello non danno né prendono il colore da un testo**: hanno un aspetto
         * loro (nota A dell'utente, giro della 4.43); da un testo o verso un testo passano
         * carattere, i tre stili e l'allineamento. Fra due elementi dello stesso strumento passano
         * anche colore e sfocatura.
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
            // La ricetta va col colore: il modulo carica il campione da [tint], e una ricetta
            // rimasta indietro mostrerebbe il colore vecchio quando l'elemento è scelto.
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
        /** Sotto questa misura, in pixel, un riquadro a mano libera non ha un lato da stirare. */
        const val FLAT = 0.5f
    }
}

/**
 * Come il modulo ha scelto i colori di un elemento: campione, luminosità e opacità, per contorno e
 * riempimento.
 *
 * ⚠️ L'elemento disegna [Mark.ink] e [Mark.fill], che sono questi valori già mescolati: da loro il
 * modulo non saprebbe quale campione era scelto e dove erano i cursori.
 */
data class Tint(
    val ink: Int,
    val inkLight: Float,
    val inkAlpha: Float,
    val fill: Int?,
    val fillLight: Float,
    val fillAlpha: Float
)

/** Come un disegno diventa pixel, allo stesso modo sul palco e nel file salvato. */
internal object Draw {

    /** Il cursore dello spessore, in frazioni del lato lungo. */
    const val WIDTH_MIN = 0.001f
    const val WIDTH_MAX = 0.03f

    /**
     * Lo spessore di fabbrica: il cursore al 66% della corsa (nota A dell'utente sul giro della
     * 4.94, e risposta `A3`: vale per tutti gli strumenti di forma).
     */
    const val WIDTH = WIDTH_MIN + 0.66f * (WIDTH_MAX - WIDTH_MIN)

    /**
     * La luminosità di fabbrica del tratto: zero, cioè il rosso del primo campione com'è (nota A,
     * giro della 4.94).
     */
    const val INK_LIGHT = 0f

    /** L'opacità di fabbrica del tratto: 95% (nota A, giro della 4.94). */
    const val INK_ALPHA = 0.95f

    /**
     * L'inchiostro di fabbrica del tratto, il primo campione della tavolozza dell'utente:
     * `#FFFF4C3F` (risposta `D1`).
     */
    const val INK = 0xFFFF4C3F.toInt()

    /**
     * Il riempimento di fabbrica di una forma chiusa, `#33FF4C3F` in ARGB: il rosso del primo
     * campione al 20%, così rettangoli ed ellissi nascono riempiti (richiesta dell'utente).
     */
    const val FILL = 0x33FF4C3F

    /**
     * [colour] schiarito o scurito di [shift], da -1 (il più scuro) a 1 (il più chiaro), con 0 il
     * colore com'è (Luminosità; nota dell'utente su `4.43-01`: senza avvicinarsi troppo al bianco o
     * al nero).
     *
     * ⚠️ Sposta la luminosità HSL verso [LIGHT_MAX] o [LIGHT_MIN] e mai oltre, quindi tinta e
     * saturazione restano quelle del campione. Un colore già oltre un limite non si muove in quel
     * verso: il bianco non schiarisce, il nero non scurisce. L'alfa resta.
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
     * Fin dove arriva Luminosità, in luminosità HSL: 15% e 85%, così non tocca mai il nero o il
     * bianco. Scelta della sessione, dichiarata nella voce di collaudo.
     */
    const val LIGHT_MIN = 0.15f
    const val LIGHT_MAX = 0.85f

    /** [colour] con l'alfa portata ad [alpha], da 0 a 1. */
    fun withAlpha(colour: Int, alpha: Float): Int =
        ((alpha.coerceIn(0f, 1f) * 255f + 0.5f).toInt() shl 24) or (colour and 0xFFFFFF)

    /**
     * I nove inchiostri della tavolozza, quella dell'utente nel suo ordine: sei tinte, poi bianco,
     * grigio e nero. Il grigio è un nero al 30%, `#B3B3B3` (lettura da grafico della nota su
     * `4.44-01`, dichiarata nella voce di collaudo; l'altra lettura era `#4D4D4D`).
     *
     * ⚠️ **Niente selettore di colore**: nove campioni coprono l'uso del modulo (segnare, indicare,
     * sottolineare), e un selettore costa una finestra.
     * ⚠️ **I colori di fabbrica sono campioni**: [INK] è il primo, e [FILL] è il rosso del primo
     * campione al 20%. Un campione è scelto quando il suo colore è quello corrente, quindi un
     * colore di fabbrica fuori tavolozza aprirebbe il modulo senza campione scelto. Il tratto
     * prende il primo campione per costruzione (`Gaze.ink`); il riempimento ha la sua costante,
     * presidiata da `DisegnoTest`.
     */
    val INKS = listOf(
        INK, 0xFFFFBF00.toInt(), 0xFF5ACB8C.toInt(), 0xFF3EB7FF.toInt(),
        0xFF846AE2.toInt(), 0xFFCC6898.toInt(), 0xFFFFFFFF.toInt(), 0xFFB3B3B3.toInt(), 0xFF000000.toInt()
    )

    /**
     * Le dimensioni del testo, in frazioni del lato lungo, e quella di fabbrica. La più grande è
     * 1,5 (nota dell'utente su `4.90-02`: un testo grande come l'immagine e oltre; il numero è
     * della sessione, dichiarato nella voce di collaudo): una lettera alta una volta e mezza
     * l'immagine.
     */
    const val TEXT_MIN = 0.01f
    const val TEXT_MAX = 1.5f
    const val TEXT = 0.05f

    /**
     * Dove è il cursore della dimensione per un testo di [size], da 0 a 1, e ritorno con
     * [textSize].
     * ⚠️ **La scala è di rapporti, non di differenze**: da 0,01 a 1,5 un cursore uniforme
     * lascerebbe le dimensioni d'uso (da 0,02 a 0,1) nel primo ventesimo della corsa, sotto la
     * larghezza di un dito. Ogni passo lungo la corsa moltiplica la dimensione per lo stesso
     * fattore.
     */
    fun textTrack(size: Float): Float =
        (ln(size.coerceIn(TEXT_MIN, TEXT_MAX) / TEXT_MIN) / ln(TEXT_MAX / TEXT_MIN)).coerceIn(0f, 1f)

    fun textSize(track: Float): Float = (TEXT_MIN * exp(track.coerceIn(0f, 1f) * ln(TEXT_MAX / TEXT_MIN)))
        .coerceIn(TEXT_MIN, TEXT_MAX)

    /**
     * La dimensione con cui nasce un testo (nota dell'utente su `4.91-03`: la riga più lunga misura
     * il 50% della larghezza dell'immagine): la dimensione, in frazione del lato lungo di un
     * originale [w] x [h], a cui la riga più larga di [words] è [FIT_WIDE] della larghezza, fra
     * [TEXT_MIN] e [TEXT_MAX]. La prendono anche pillola e pannello posati con un tocco, per le
     * loro parole (lettura della sessione, dichiarata nella voce di collaudo).
     * ⚠️ Misurata col paint con cui il testo si dipinge ([lines]), prima a [FIT_PROBE] pixel e poi
     * di nuovo alla dimensione trovata: la larghezza di una riga non cresce esattamente in
     * proporzione, perché le lettere si adattano ai pixel interi.
     */
    fun fitSize(words: Words, w: Float, h: Float): Float {
        if (w <= 0f || h <= 0f) return TEXT
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = Faces.of(words.face, words.italic, words.bold)
        }
        val righe = words.text.split('\n')
        fun wide(px: Float): Float {
            paint.textSize = px
            return righe.maxOfOrNull { paint.measureText(it) } ?: 0f
        }
        val bersaglio = FIT_WIDE * w
        val prova = wide(FIT_PROBE).takeIf { it > 0f } ?: return TEXT
        var px = bersaglio * FIT_PROBE / prova
        wide(px).takeIf { it > 0f }?.let { px *= bersaglio / it }
        return (px / max(w, h)).coerceIn(TEXT_MIN, TEXT_MAX)
    }

    /**
     * Quanto è larga la riga più larga di un testo nuovo, in parte della larghezza dell'immagine
     * (il suo 50%).
     */
    const val FIT_WIDE = 0.5f
    private const val FIT_PROBE = 100f

    /** Il colore di fabbrica del testo: bianco (lettura `A4`). */
    const val TEXT_INK = 0xFFFFFFFF.toInt()

    /** La striscia di fabbrica dell'etichetta (valore dell'utente). */
    const val LABEL_INK = 0xFFA3408F.toInt()

    /**
     * Le strisce che il modulo offre: sei, quella di fabbrica per prima (nota dell'utente: da 4 a 8
     * colori proposti). La striscia porta parole bianche, quindi i colori sono scuri. [readable]
     * dice quali risaltano dove il testo si trova.
     */
    val LABEL_INKS = listOf(
        LABEL_INK, 0xFF1F5FA8.toInt(), 0xFF1E7A4A.toInt(), 0xFFB3261E.toInt(), 0xFFB35400.toInt(), 0xFF262626.toInt()
    )

    /**
     * Se un fondo [ground] risalta sull'immagine [under] e tiene leggibile [ink] (nota dell'utente:
     * solo colori che contrastano, letta come 'dove si trova l'etichetta', lettura `A5`): almeno
     * [GROUND_DISTANCE] dall'immagine nello spazio CIELAB, e almeno [WORDS_CONTRAST] di rapporto di
     * contrasto WCAG con le parole.
     * ⚠️ **Due misure perché le domande sono due**: le parole si leggono per luminosità sul fondo,
     * e il fondo risalta anche per tinta, quindi una striscia chiara come l'immagine può risaltare
     * per il colore.
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
     * Il colore delle parole [ink] sul fondo [ground], quando il tasto `Sfondo` posa la striscia:
     * parole bianche o nere prendono quella delle due che si legge meglio, e parole di un altro
     * colore restano, salvo diventare bianche o nere se non si leggerebbero.
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
     * Il colore di [image] sotto il riquadro di [mark], o di tutta l'immagine senza: la media dei
     * pixel, su cui [readable] misura i fondi. Il riquadro è quello misurato del testo, nel
     * riferimento originale del disegno.
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

    /** Il lato della copia piccola di cui [under] fa la media. */
    private const val UNDER = 8

    /**
     * Quanto un fondo deve differire dall'immagine sotto (una distanza CIELAB, dove circa 2 è la
     * minima differenza visibile), e le parole dal fondo (il testo grande delle WCAG).
     */
    const val GROUND_DISTANCE = 20.0
    const val WORDS_CONTRAST = 3.0
    private const val OPAQUE = 0xFF000000.toInt()

    /**
     * Le misure delle righe, in multipli della dimensione: il passo fra due righe, il margine del
     * fondo ai capi di una riga, gli angoli tondi della striscia e la sua ombra (sfocatura e
     * scostamento). Scelte della sessione, dichiarate nella voce di collaudo.
     */
    private const val LINE = 1.3f
    const val PAD = 0.35f
    private const val ROUND = 0.3f
    private const val SHADOW_BLUR = 0.12f
    private const val SHADOW_DROP = 0.06f
    private const val SHADOW_INK = 0x40000000

    /**
     * Un testo disposto: il paint, le righe con le larghezze, e le misure che il riquadro legge.
     */
    private class Lines(
        val paint: Paint, val lines: List<String>, val widths: List<Float>,
        val size: Float, val step: Float, val cap: Float, val pad: Float, val hw: Float, val hh: Float,
        val content: Float, val align: Align
    ) {
        /** Dove è il centro della riga [i], dal centro del riquadro. */
        fun x(i: Int): Float = lineX(align, content, widths[i])
    }

    /** Dove è il centro di una riga larga [line], dal centro di un riquadro largo [content]. */
    private fun lineX(align: Align, content: Float, line: Float): Float = when (align) {
        Align.LEFT -> (line - content) / 2f
        Align.CENTER -> 0f
        Align.RIGHT -> (content - line) / 2f
    }

    /**
     * [text] spezzato nelle righe che entrano in [wide] pixel con [paint]: ogni riga va a capo dove
     * non entra più, come un paragrafo (`B3`).
     */
    private fun wrapped(text: String, paint: TextPaint, wide: Float): List<String> = text.split('\n').flatMap { riga ->
        if (riga.isEmpty()) return@flatMap listOf("")
        val layout = StaticLayout.Builder.obtain(riga, 0, riga.length, paint, wide.toInt().coerceAtLeast(1))
            .setIncludePad(false).build()
        (0 until layout.lineCount).map { riga.substring(layout.getLineStart(it), layout.getLineEnd(it)).trimEnd() }
    }

    /**
     * Le parole di [mark] disposte per un originale [w] x [h]: una riga per riga del testo, a
     * [LINE] dimensioni di distanza; il riquadro largo la metà della riga più larga, margine del
     * fondo compreso, e alto la metà delle righe.
     * ⚠️ **Le righe sono centrate sulla metà delle maiuscole, non sul riquadro del carattere**
     * (nota dell'utente: la linea di base di Literata è bassa): l'ascendente di Literata è 1177
     * unità per maiuscole di 700, e una striscia centrata sul riquadro terrebbe le parole in basso.
     * Centrati su metà altezza di una 'H', i quattro caratteri sono allineati senza correzioni; un
     * testo quasi tutto in minuscolo si centra sulla 'x' ([core]).
     * ⚠️ Il peso arriva col carattere ([Faces]): il file di Montserrat si apre al suo peso più
     * sottile, 100.
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
        val cap = core(paint, words.text)
        val step = size * LINE
        val pad = if (words.label) size * PAD else 0f
        val content = if (wrap > 0f) wrap else widths.maxOrNull() ?: 0f
        return Lines(paint, lines, widths, size, step, cap, pad, content / 2f + pad, lines.size * step / 2f, content, words.align)
    }

    /**
     * L'altezza della fascia su cui si centra una riga di [text], per [paint]: quella delle
     * minuscole quando il testo ne ha più che di maiuscole e cifre, delle maiuscole altrimenti
     * (nota B dell'utente sul giro della 4.91: una centratura ottica, sul rettangolo che contiene
     * le minuscole).
     * ⚠️ Una fascia per tutto il testo, letta sulle sue lettere: una per riga spazierebbe le righe
     * in modo irregolare. La maggioranza è una lettura della sessione, dichiarata nella voce di
     * collaudo.
     */
    private fun core(paint: Paint, text: String): Float {
        val minuscole = text.count { it.isLowerCase() }
        val maiuscole = text.count { it.isUpperCase() || it.isDigit() }
        val segno = if (minuscole > maiuscole) "x" else "H"
        return android.graphics.Rect().also { paint.getTextBounds(segno, 0, 1, it) }.height().toFloat()
    }

    /**
     * Il centro del riquadro di un testo o di una pillola e i due semilati, nei pixel di un
     * originale [w] x [h], negli assi del riquadro: quello che leggono maniglie, tocco, contorno e
     * guide. Il riquadro di un testo sono le parole misurate intorno al punto; quello di una
     * pillola o di un pannello è il riquadro disegnato, allungato in basso quando le parole
     * chiedono più spazio ([pill]).
     */
    fun textFrame(mark: Mark, w: Float, h: Float): Triple<Offset, Float, Float> {
        if (mark.points.isEmpty()) return Triple(Offset.Zero, 0f, 0f)
        if (mark.pen.framed) return pill(mark, w, h).let { Triple(it.c, it.hw, it.hh) }
        val t = lines(mark, w, h)
        return Triple(Offset(mark.points.first().x * w, mark.points.first().y * h), t?.hw ?: 0f, t?.hh ?: 0f)
    }

    /**
     * L'aspetto della pillola (nota A dell'utente sul giro della 4.43, in ARGB: riempimento
     * `#ccff4b3d`, vetro sfocato di 12 px sotto, due tracce sottili proporzionali, dall'interno lo
     * 0,5% del lato lungo in `#e6fffefa` e poi lo 0,5% in `#e6373737`; parole bianche opache).
     * ⚠️ **Le due tracce sono dentro il riquadro**, la scura sul bordo e la chiara dentro, così il
     * riquadro disegnato è tutta la pillola e si posa sui bordi come un rettangolo.
     * ⚠️ **I 12 px del vetro sono una frazione del lato lungo della pillola** (lettura della
     * sessione, dichiarata nella voce di collaudo): 12 px sullo schermo di una pillola larga metà
     * telefono, circa 545 px. Un numero fisso di pixel non sfocherebbe niente su una foto grande, e
     * sfocherebbe diversamente anteprima e file.
     */
    const val PILL_FILL = 0xCCFF4B3D.toInt()
    const val PILL_LIGHT = 0xE6FFFEFA.toInt()
    const val PILL_DARK = 0xE6373737.toInt()
    const val PILL_WORDS = 0xFFFFFFFF.toInt()
    const val PILL_TRACE = 0.005f
    const val PILL_BLUR = 12f / 545f

    /**
     * I colori della pillola (nota dell'utente su `4.90-05`: una tavolozza a parte, colori vivaci,
     * il resto invariato): otto, opachi, il suo rosso per primo; la pillola li posa all'alfa di
     * [PILL_FILL].
     * ⚠️ **Vivaci e non chiari**: ognuno tiene le parole bianche a un contrasto di almeno 3 (il
     * testo grande delle WCAG, [WORDS_CONTRAST]), quindi giallo e lime fluorescenti sono fuori. La
     * scelta è della sessione, dichiarata nella voce di collaudo.
     */
    val PILL_INKS = listOf(
        PILL_FILL or OPAQUE, 0xFFFF5A00.toInt(), 0xFFFF1F8E.toInt(), 0xFFD500F9.toInt(),
        0xFF6B2BFF.toInt(), 0xFF2962FF.toInt(), 0xFF009E9E.toInt(), 0xFF00A651.toInt()
    )

    /** [ink] posato all'alfa della pillola, quella di [PILL_FILL]. */
    fun pillFill(ink: Int): Int = (ink and 0xFFFFFF) or (PILL_FILL and OPAQUE)

    /**
     * La dimensione minima delle parole di una pillola, in frazione del lato lungo dell'immagine
     * (nota dell'utente: fino a un minimo di leggibilità): 2%, circa 16 sp quando una foto intera
     * riempie lo schermo di un telefono. Lettura della sessione, dichiarata nella voce di collaudo.
     */
    const val PILL_MIN = 0.02f

    /**
     * Quanto le parole restano lontane dal bordo della pillola, oltre le due tracce, in parte del
     * raggio delle estremità tonde: a 0,35 gli angoli di una riga restano dentro le estremità.
     */
    private const val PILL_INSET = 0.35f

    /**
     * L'aspetto del pannello (non approvato di `4.81-01`: rettangolo appena arrotondato, sfocatura,
     * testo come la pillola, colore sempre al 20% di opacità, anche 'nessuno'): il raggio degli
     * angoli in parte del lato lungo del pannello stesso (lettura della sessione, dichiarata nella
     * voce di collaudo), e l'alfa del colore. Le parole sono quelle della pillola, bianche. Niente
     * tracce, che sono della pillola: l'utente non le ha elencate. Il raggio è 1% (nota su
     * `4.91-01`).
     */
    const val PANEL_ROUND = 0.01f
    private const val PANEL_ALPHA = 0x33000000

    /** [ink] posato al 20% del pannello. */
    fun panelFill(ink: Int): Int = (ink and 0xFFFFFF) or PANEL_ALPHA

    /**
     * Quanto le parole restano lontane dal bordo del pannello, in parte del semilato minore: meno
     * della pillola, le cui estremità tonde prendono spazio. Scelta della sessione, dichiarata
     * nella voce di collaudo.
     */
    private const val PANEL_INSET = 0.15f

    /**
     * Il margine delle parole di [pen] dal bordo, oltre le tracce, in parte del semilato minore.
     */
    private fun insetOf(pen: Pen): Float = if (pen == Pen.PANEL) PANEL_INSET else PILL_INSET

    /** Una pillola disposta: il riquadro, le tracce e le parole adattate. */
    private class Pill(
        val c: Offset, val hw: Float, val hh: Float, val trace: Float,
        val paint: Paint?, val lines: List<String>, val step: Float, val cap: Float,
        val content: Float = 0f, val align: Align = Align.CENTER
    )

    /**
     * La pillola di [mark] disposta per un originale [w] x [h]: le parole vanno a capo alla
     * larghezza del riquadro e prendono la dimensione più grande a cui entrano in altezza
     * (specifica dell'utente), fino a [PILL_MIN]. Parole che a quella dimensione non entrano la
     * tengono, e il riquadro si allunga verso il basso col lato di sopra fermo: lettura della
     * sessione, dichiarata nella voce di collaudo, perché parole tagliate si perderebbero in
     * silenzio.
     * ⚠️ Le righe sono spaziate e centrate come quelle di un testo ([lines]), sulla fascia di
     * [core].
     */
    private fun pill(mark: Mark, w: Float, h: Float): Pill {
        val a = Offset(mark.points.first().x * w, mark.points.first().y * h)
        val b = Offset(mark.points.last().x * w, mark.points.last().y * h)
        val c0 = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
        val hw = abs(b.x - a.x) / 2f
        val hh0 = abs(b.y - a.y) / 2f
        // Un pannello non ha tracce, quindi le parole tengono solo il margine dal bordo.
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
        // La crescita va lungo gli assi della pillola, quindi una pillola ruotata cresce lungo se
        // stessa.
        val rad = Math.toRadians(mark.angle.toDouble())
        val grow = hh - hh0
        val c = Offset(c0.x - grow * sin(rad).toFloat(), c0.y + grow * cos(rad).toFloat())
        val cap = core(paint, words.text)
        return Pill(c, hw, hh, trace, paint, lines, lo * LINE, cap, wide, words.align)
    }

    /**
     * Quanti dimezzamenti fa l'adattamento delle parole di una pillola: una dimensione entro un
     * millesimo.
     */
    private const val FIT_STEPS = 12

    /**
     * Dipinge una pillola: la traccia scura sul bordo, quella chiara dentro, il riempimento, poi le
     * parole bianche. Il vetro sotto lo posa [blurAreas], con l'immagine. Qui si dipinge anche il
     * pannello: il suo colore, se c'è, sul riquadro appena arrotondato, poi le stesse parole.
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
                // Una pillola fatta altrove (le prove) non ha riempimento, e prende il rosso
                // dell'utente.
                color = mark.fill ?: PILL_FILL
                style = Paint.Style.FILL
            })
        }
        words(canvas, p)
        canvas.restoreToCount(kept)
    }

    /** Le parole di una pillola o di un pannello disposti, su una tela centrata sul riquadro. */
    private fun words(canvas: Canvas, p: Pill) {
        p.paint?.let { paint ->
            val top = -p.lines.size * p.step / 2f
            p.lines.forEachIndexed { i, line ->
                canvas.drawText(line, lineX(p.align, p.content, paint.measureText(line)), top + p.step * (i + 0.5f) + p.cap / 2f, paint)
            }
        }
    }

    /** Il raggio degli angoli di un pannello, per i semilati [hw] e [hh]. */
    private fun panelRound(hw: Float, hh: Float): Float = min(PANEL_ROUND * 2f * max(hw, hh), min(hw, hh))

    /** Il raggio del vetro sotto una pillola, nei pixel di un originale [w] x [h]. */
    fun pillGlass(mark: Mark, w: Float, h: Float): Float = pill(mark, w, h).let { PILL_BLUR * 2f * max(it.hw, it.hh) }

    /**
     * Il riquadro di una pillola che contiene [words] a [size], centrato in [at], come due angoli
     * in frazioni di un originale [w] x [h]: un tocco con Pillola posa una pillola intorno alle
     * parole, una riga per riga scritta, coi margini che [pill] tiene. Un tocco con Pannello posa
     * un pannello allo stesso modo, col suo margine e senza tracce ([pen]).
     */
    fun pillAround(words: Words, size: Float, at: Offset, w: Float, h: Float, pen: Pen = Pen.PILL): List<Offset> {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Faces.of(words.face, words.italic, words.bold)
            textSize = (size * max(w, h)).coerceAtLeast(1f)
        }
        val lines = words.text.split('\n')
        val wide = lines.maxOf { paint.measureText(it) }
        val tall = lines.size * paint.textSize * LINE
        // I margini sono in parte del raggio, che è metà dell'altezza del riquadro: risolti per
        // lei.
        val traces = if (pen == Pen.PILL) 4f * PILL_TRACE else 0f
        val boxH = tall / (1f - insetOf(pen) - traces)
        val boxW = max(boxH, wide + boxH * (insetOf(pen) + traces) + 2f * paint.textSize * PILL_SIDE)
        val c = Offset(at.x * w, at.y * h)
        return listOf(Offset((c.x - boxW / 2f) / w, (c.y - boxH / 2f) / h), Offset((c.x + boxW / 2f) / w, (c.y + boxH / 2f) / h))
    }

    /**
     * Un po' d'aria ai capi di una pillola posata con un tocco, in parte della dimensione delle
     * parole.
     */
    private const val PILL_SIDE = 0.2f

    /**
     * Dipinge un elemento di testo: prima il fondo, come forma unica (le strisce delle righe si
     * fondono, come nell'esempio dell'utente) con un'ombra morbida sotto, poi le righe, centrate
     * sul punto dell'elemento e ruotate col suo angolo.
     */
    private fun paintText(canvas: Canvas, mark: Mark, w: Float, h: Float) {
        val words = mark.words ?: return
        val t = lines(mark, w, h) ?: return
        val kept = canvas.save()
        canvas.translate(mark.points.first().x * w, mark.points.first().y * h)
        if (mark.angle != 0f) canvas.rotate(mark.angle)
        if (words.label) {
            canvas.drawPath(labelGround(t), Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
     * Il fondo dell'etichetta per le righe [t], centrato sul punto del testo: una striscia per ogni
     * riga con parole, fuse in una forma sola.
     * ⚠️ **Tondo solo dove una striscia sporge, con un raccordo concavo dove incontra una più
     * larga** (nota D dell'utente sul giro della 4.91, con un mockup): ogni angolo che incontra la
     * riga vicina la guarda:
     * - la più larga delle due tiene l'angolo tondo, come gli angoli esterni;
     * - la più stretta ha l'angolo vivo, e un raccordo concavo riempie l'angolo fra il suo lato e
     * il
     *   bordo della striscia più larga;
     * - due lati entro mezzo pixel proseguono dritti, senza tondo e senza raccordo.
     * ⚠️ Tondo e raccordo hanno lo stesso raggio, al massimo metà della differenza fra i due lati,
     * così non si incrociano.
     */
    private fun labelGround(t: Lines): Path {
        val ground = Path()
        val band = t.step / 2f
        val r = min(band, t.size * ROUND)
        val pieni = t.lines.indices.filter { t.lines[it].isNotBlank() }
        fun left(i: Int): Float = t.x(i) - t.widths[i] / 2f - t.pad
        fun right(i: Int): Float = t.x(i) + t.widths[i] / 2f + t.pad
        /*
         * Il raggio di un angolo della riga [i] sul lato [onLeft], contro la riga [n] sopra o
         * sotto: quello di un angolo convesso, o 0 per un angolo vivo, il cui raccordo si aggiunge
         * al fondo.
         */
        fun corner(i: Int, n: Int, onLeft: Boolean, top: Boolean): Float {
            if (n !in pieni) return r
            val sporge = if (onLeft) left(n) - left(i) else right(i) - right(n)
            if (abs(sporge) < JOIN_SLACK) return 0f
            val rr = min(r, abs(sporge) / 2f)
            if (sporge > 0f) return rr
            // La striscia più stretta: angolo vivo, e il raccordo nella sua fascia, fuori da lei.
            val x = if (onLeft) left(i) else right(i)
            val y = -t.hh + t.step * i + if (top) 0f else t.step
            val fuori = if (onLeft) -rr else rr
            val dentro = if (top) rr else -rr
            val quadro = RectF(min(x, x + fuori), min(y, y + dentro), max(x, x + fuori), max(y, y + dentro))
            val raccordo = Path().apply { addRect(quadro, Path.Direction.CW) }
            raccordo.op(Path().apply { addCircle(x + fuori, y + dentro, rr, Path.Direction.CW) }, Path.Op.DIFFERENCE)
            ground.op(raccordo, Path.Op.UNION)
            return 0f
        }
        for (i in pieni) {
            val top = -t.hh + t.step * i
            val sopra = i - 1
            val sotto = i + 1
            val tl = corner(i, sopra, onLeft = true, top = true)
            val tr = corner(i, sopra, onLeft = false, top = true)
            val br = corner(i, sotto, onLeft = false, top = false)
            val bl = corner(i, sotto, onLeft = true, top = false)
            val striscia = Path().apply {
                addRoundRect(
                    RectF(left(i), top, right(i), top + 2f * band),
                    floatArrayOf(tl, tl, tr, tr, br, br, bl, bl), Path.Direction.CW
                )
            }
            ground.op(striscia, Path.Op.UNION)
        }
        return ground
    }

    /** Entro quanti pixel due lati delle strisce contano come un lato dritto solo. */
    private const val JOIN_SLACK = 0.5f

    /**
     * La matrice dal riferimento originale ([w] x [h] pixel) a quello della posa.
     *
     * ⚠️ **È la stessa trasformazione di `spunBy`, passo per passo**: specchio, poi rotazione, poi
     * i limiti riportati all'origine, che è quello che fa `Bitmap.createBitmap` con una matrice.
     * Una seconda derivazione (per esempio da `Healing.unpose`) coinciderebbe sugli angoli e
     * potrebbe sbagliare di un pixel; `DisegnoTest` confronta le due su un punto.
     */
    fun posed(w: Int, h: Int, spin: Spin): Matrix = Matrix().apply {
        if (spin.mirror) postScale(-1f, 1f)
        postRotate(90f * spin.turns)
        val bounds = RectF(0f, 0f, w.toFloat(), h.toFloat())
        mapRect(bounds)
        postTranslate(-bounds.left, -bounds.top)
    }

    /**
     * Dipinge [drawing] su [canvas], le cui coordinate sono il riferimento originale di un'immagine
     * [w] x [h] (la posa sulla tela la imposta il chiamante).
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
        // Un rettangolo o un'ellisse ruotati sono la stessa forma su una tela ruotata: riempimento,
        // contorno, angoli tondi e tratteggio girano con lei, senza un secondo disegno.
        val turned = mark.pen.closed && mark.angle != 0f
        val kept = if (turned) canvas.save() else -1
        if (turned) canvas.rotate(mark.angle, (a.x + b.x) / 2f, (a.y + b.y) / 2f)
        // Il riempimento va prima e da solo: ha la sua opacità, e il livello del contorno sotto non
        // deve moltiplicarla.
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
         * ⚠️ **L'opacità del contorno si posa sull'elemento intero, con un livello** (Opacità di
         * Traccia, risposta `S1`): con un paint traslucido ogni punto in cui due tratti dello
         * stesso elemento si incrociano (la punta della freccia sull'asta, i capi tondi del
         * tratteggio, una mano libera sopra se stessa) verrebbe più scuro del resto.
         */
        val alpha = mark.ink ushr 24
        val layer = if (alpha < 255) canvas.saveLayerAlpha(null, alpha) else -1
        strokeOf(canvas, mark, pts, long)
        if (layer >= 0) canvas.restoreToCount(layer)
        if (kept >= 0) canvas.restoreToCount(kept)
    }

    /**
     * Il contorno di un elemento a riquadro, come punti in frazioni di un originale [w] x [h]:
     * tanti che il riquadro che coprono sia quello della forma sullo schermo, angoli tondi
     * compresi. Il palco lo legge per posare un elemento ruotato su un bordo, e [blurAreas] per
     * tagliare la sfocatura.
     * ⚠️ Punti e non un riquadro: la geometria può piegare il contorno, e solo i punti la
     * attraversano.
     */
    fun outline(mark: Mark, w: Int, h: Int): List<Offset> = outline(mark, w.toFloat(), h.toFloat())

    private fun outline(mark: Mark, fw: Float, fh: Float): List<Offset> {
        if (mark.points.isEmpty()) return emptyList()
        val rad = Math.toRadians(mark.angle.toDouble())
        val cs = cos(rad).toFloat()
        val sn = sin(rad).toFloat()
        if (mark.pen == Pen.TEXT) {
            // Il riquadro di un testo sono i suoi quattro angoli, ruotati con lui.
            val (c, hw, hh) = textFrame(mark, fw, fh)
            return listOf(-hw to -hh, hw to -hh, hw to hh, -hw to hh).map { (x, y) ->
                Offset((c.x + x * cs - y * sn) / fw, (c.y + x * sn + y * cs) / fh)
            }
        }
        // Il riquadro di una pillola è quello che chiedono le parole: può essere più alto di quello
        // disegnato.
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
            // ⚠️ Un verso solo, angolo dopo angolo: in alto a sinistra dal lato sinistro alla cima,
            // in alto a destra dalla cima al lato destro, e così via. Con due archi percorsi al
            // contrario il contorno si incrocerebbe, e la sfocatura taglierebbe l'immagine con due
            // angoli sbagliati. Una pillola è un rettangolo con le estremità più tonde possibile,
            // un pannello uno con gli angoli appena tondi.
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

    /** Quanti punti [outline] prende su un'ellisse, e su ogni angolo tondo di un rettangolo. */
    private const val OUTLINE_STEPS = 72
    private const val CORNER_STEPS = 6

    /**
     * Quanti punti [outline] prende su ogni quarto delle estremità tonde di una pillola: la
     * sfocatura taglia lungo loro.
     */
    private const val PILL_STEPS = 18

    /**
     * [deg] sul multiplo di 45 gradi più vicino quando è entro [SNAP_DEG], e nell'intervallo da
     * -180 a 180 (la rotazione di 'Ruota'). Gli stessi 5 gradi delle linee sull'orizzontale e sulla
     * verticale, così i due scatti si sentono uguali sotto il dito.
     */
    fun snapTurn(deg: Float): Float {
        var d = deg % 360f
        if (d > 180f) d -= 360f
        if (d <= -180f) d += 360f
        val m = Math.round(d / 45f) * 45f
        val out = if (abs(d - m) <= SNAP_DEG) m else d
        return if (out <= -180f) out + 360f else out
    }

    /** Il raggio degli angoli di un rettangolo [box]: segue il tratto, mai oltre metà lato. */
    private fun corner(mark: Mark, long: Float, box: RectF): Float =
        min((mark.width * long).coerceAtLeast(1f) * CORNER, min(box.width(), box.height()) / 2f)

    /** Il contorno di [mark], opaco: l'opacità la posa [paint]. */
    private fun strokeOf(canvas: Canvas, mark: Mark, pts: List<Offset>, long: Float) {
        val stroke = (mark.width * long).coerceAtLeast(1f)
        val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mark.ink or 0xFF000000.toInt()
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            // Il tratteggio scala col tratto, così una linea spessa non è una fila di punti.
            if (mark.dashed) pathEffect = DashPathEffect(floatArrayOf(stroke * DASH, stroke * GAP), 0f)
        }
        val a = pts.first()
        val b = pts.last()
        when (mark.pen) {
            Pen.FREE -> {
                // Un tocco senza movimento è un punto: un tracciato di un punto solo non disegna
                // niente.
                if (pts.size == 1 || pts.all { it == a }) {
                    canvas.drawPoint(a.x, a.y, pen.apply { pathEffect = null })
                    return
                }
                canvas.drawPath(smooth(pts), pen)
            }
            Pen.LINE -> canvas.drawLine(a.x, a.y, b.x, b.y, pen)
            Pen.ARROW -> {
                // La punta non è mai tratteggiata: spezzata non si legge più come una punta.
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
            // Testo, pillola e pannello non arrivano qui: [paint] li disegna con [paintText] e
            // [paintPill].
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
     * L'asta di una freccia tratteggiata, posata dalla punta verso la coda (nota B dell'utente sul
     * giro della 4.45, con un disegno: nessun buco fra l'ultimo tratto e la punta, e il primo
     * tratto abbastanza lungo da uscire dall'angolo concavo della punta).
     *
     * ⚠️ **Il tratteggio parte dalla punta, non dalla coda**: contato dalla coda arrivava alla
     * punta dove capitava, spesso in un vuoto. Il pezzo contro la punta è pieno e lungo quanto i
     * bracci della punta lungo l'asta, più un tratto per i loro capi tondi; il tratteggio prosegue
     * verso la coda cominciando con un vuoto, ed è la coda a uscire parziale. Un'asta più corta di
     * quel pezzo si disegna piena.
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
            // La fase salta il primo tratto, così il tratteggio comincia con un vuoto dopo il pezzo
            // pieno.
            val gap = Paint(dashed).apply { pathEffect = DashPathEffect(floatArrayOf(stroke * DASH, stroke * GAP), stroke * DASH) }
            canvas.drawLine(cx, cy, a.x, a.y, gap)
        }
    }

    /**
     * Un tracciato a mano libera per [pts], ammorbidito con curve quadratiche per i punti medi. Il
     * dito dà un punto per fotogramma, e segmenti dritti mostrerebbero spigoli su una curva veloce.
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
     * [drawing] su una bitmap trasparente nel riferimento della posa di un originale [w] x [h], o
     * `null` quando non c'è niente di disegnato: il palco la posa sopra l'anteprima sviluppata.
     * [into], se della misura giusta, si riusa: il palco ridisegna a ogni movimento del dito, e una
     * bitmap nuova per fotogramma sarebbe qualche megabyte di scarto.
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
     * [target], un'immagine nel riferimento della posa, col [drawing] posato sopra: il passo del
     * file salvato fra lo sviluppo e la geometria. Disegna su [target] stesso quando può, e su una
     * copia altrimenti (una bitmap letta da un file può essere immutabile): il chiamante ricicla
     * per identità, come per gli altri passi.
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

    /**
     * Il cursore della sfocatura, in frazioni del lato lungo dell'elemento (numeri dell'utente).
     */
    const val BLUR_MIN = 0.005f
    const val BLUR_MAX = 0.25f

    /**
     * La sfocatura di fabbrica: il 10% del lato lungo dell'elemento, abbastanza che un volto o una
     * targa sotto non si leggano, che è l'uso nominato dall'utente. Scelta della sessione,
     * dichiarata nella voce di collaudo.
     */
    const val BLUR = 0.1f

    /**
     * [target] con l'immagine sfocata sotto ogni elemento con vetro di [drawing], nel riferimento
     * originale: [at] dice quale parte dell'originale [target] contiene, in frazioni (tutto, o il
     * pezzo che il palco legge a piena risoluzione).
     *
     * ⚠️ **Prima dello sviluppo e prima di ogni altro elemento, sul palco e nel file salvato**,
     * come le toppe di Correggi: i cursori del colore sviluppano i pixel sfocati, geometria e
     * ritaglio portano l'area con l'immagine, e gli altri elementi si disegnano sopra (specifica
     * dell'utente: la sfocatura è sempre al livello più basso).
     * ⚠️ **Il raggio è una frazione del lato lungo dell'elemento**, misurato nei pixel di [target],
     * così anteprima e file sfocano nella stessa proporzione.
     * ⚠️ **Come**: l'area intorno all'elemento, allargata del raggio, si dimezza finché il raggio è
     * di pochi pixel (ogni dimezzamento fa la media di quattro pixel, quindi non salta niente), si
     * sfoca là con tre passate a scatola, che si avvicinano a una gaussiana, e si riposa attraverso
     * il contorno dell'elemento, stirata con un filtro bilineare. Una sfocatura di centinaia di
     * pixel costa quanto una di pochi.
     * ⚠️ Disegna su [target] quando [mine] dice che è del chiamante ed è mutabile, e su una copia
     * altrimenti: l'anteprima del palco non si tocca mai.
     */
    fun blurAreas(target: Bitmap, drawing: Drawing, mine: Boolean, at: RectF = WHOLE): Bitmap {
        // Pillola e pannello posano qui il loro vetro, e si disegnano sopra come gli altri
        // elementi.
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
            // La sfocatura di un pannello è in parte del lato lungo del riquadro che chiedono le
            // parole, come il suo contorno.
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

    /** Tutto l'originale, in frazioni: quello che [blurAreas] legge di serie. */
    private val WHOLE = RectF(0f, 0f, 1f, 1f)

    /** Il raggio, in pixel, a cui [blurAreas] sfoca dopo aver dimezzato l'area. */
    private const val BLUR_SMALL = 4f

    /**
     * La parte [from] di [src] portata a [toW] x [toH], dimezzandola finché è più del doppio: un
     * filtro bilineare a metà misura fa la media due per due, e a un passo più grande salterebbe
     * dei pixel.
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

    /**
     * Tre passate a scatola di raggio [r] su [bitmap], in orizzontale e in verticale, coi bordi
     * ripetuti.
     */
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

    /**
     * Una passata a scatola lungo una riga o una colonna di [n] pixel, da [start] a passi di
     * [step].
     */
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

    /**
     * La punta della freccia, in tratti, almeno una frazione del lato lungo, e il suo semiangolo.
     */
    private const val HEAD = 5f
    private const val HEAD_MIN = 0.012f
    private const val HEAD_ANGLE = Math.PI / 7

    /**
     * Un tratto del tratteggio e il vuoto dopo, in spessori: i capi tondi tolgono mezzo spessore a
     * ogni estremo.
     */
    private const val DASH = 2f
    private const val GAP = 2.2f

    /** Il raggio degli angoli di un rettangolo, in spessori. */
    private const val CORNER = 3f

    /**
     * La distanza fra due punti di un tratto a mano libera che vale la pena tenere, in frazione.
     */
    const val STEP = 0.002f

    /** Se [b] è abbastanza lontano da [a] da tenerlo in un tratto a mano libera. */
    fun far(a: Offset, b: Offset): Boolean = hypot(b.x - a.x, b.y - a.y) >= STEP

    /**
     * Quanto 'Duplica' sposta la copia dall'originale, in frazioni di un originale [w] x [h]:
     * [DUPLICATE_SHIFT] del lato lungo, a destra e in basso, così la copia si vede accanto
     * all'originale. Scelta della sessione, dichiarata nella voce di collaudo.
     */
    fun duplicateShift(w: Int, h: Int): Offset {
        val long = max(w, h).toFloat()
        return Offset(DUPLICATE_SHIFT * long / w.coerceAtLeast(1), DUPLICATE_SHIFT * long / h.coerceAtLeast(1))
    }

    private const val DUPLICATE_SHIFT = 0.03f

    /**
     * [b] posato sull'orizzontale o sulla verticale per [a] quando la linea da [a] a [b] è entro
     * [SNAP_DEG], con quell'asse; altrimenti [b] stesso e `null` (nota A dell'utente sul giro della
     * 4.45: posizionate precisamente sulla direttrice).
     *
     * ⚠️ **Lavora in pixel dello schermo, non nel riferimento dell'immagine**: l'orizzontale che
     * l'utente intende è quella che vede, e su un'immagine raddrizzata o specchiata le due
     * differiscono. Il punto si proietta, non si ruota: cambia solo la coordinata trasversale, e
     * l'estremo resta sotto il dito lungo l'asse.
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
     * L'indice dell'elemento più in alto di [drawing] sotto [at], o `null` (un tocco singolo
     * seleziona un oggetto). [at] è in frazioni dell'originale [w] x [h], e [reach] è quanto
     * lontano dalla linea un tocco la prende ancora, in frazione del lato lungo, oltre metà dello
     * spessore.
     *
     * ⚠️ Le distanze si misurano sul lato lungo dell'immagine, come lo spessore: in frazioni di
     * ogni lato un rettangolo non quadrato prenderebbe un tocco più lontano su un asse.
     * ⚠️ Una forma chiusa riempita si prende ovunque dentro, una vuota solo vicino alla linea:
     * dentro si vede l'immagine, e un tocco là vuol dire l'immagine. La freccia si prende
     * dall'asta.
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
                // Testo, pillola e pannello si prendono ovunque nel riquadro, come una forma
                // riempita.
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
         * Un rettangolo o un'ellisse ruotati si provano col tocco ruotato all'indietro intorno al
         * centro, così il riquadro sotto resta quello in cui sono disegnati. Qui le coordinate sono
         * i pixel dell'immagine scalati sul lato lungo, dove una rotazione resta una rotazione.
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
            // Testo, pillola e pannello si provano in [hit], che ha i lati dell'immagine per
            // misurarli.
            Pen.TEXT, Pen.PILL, Pen.PANEL -> false
            Pen.ELLIPSE -> {
                val cx = (a.x + b.x) / 2f; val cy = (a.y + b.y) / 2f
                val rx = abs(b.x - a.x) / 2f; val ry = abs(b.y - a.y) / 2f
                // Un'ellisse piatta è un segmento: la formula sotto dividerebbe per zero.
                if (min(rx, ry) < 1e-4f) segment(p, a, b) <= r
                else {
                    val d = hypot((p.x - cx) / rx, (p.y - cy) / ry)
                    (d <= 1f && mark.fill != null) || abs(d - 1f) * min(rx, ry) <= r
                }
            }
        }
    }

    /** La distanza da [p] al segmento da [a] a [b]. */
    private fun segment(p: Offset, a: Offset, b: Offset): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = dx * dx + dy * dy
        val t = if (len <= 0f) 0f else (((p.x - a.x) * dx + (p.y - a.y) * dy) / len).coerceIn(0f, 1f)
        return hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy))
    }

    /**
     * Quanto vicino a un asse scatta una linea, in gradi. Scelta della sessione, dichiarata nella
     * voce di collaudo: a 5 gradi una linea di 3 cm scatta quando il suo estremo è a circa 2,6 mm
     * dall'asse.
     */
    const val SNAP_DEG = 5.0

    /**
     * Il riquadro sullo schermo che copre il contorno di uno strumento a due punti [pen] da [a] a
     * [b], per un tratto spesso [stroke] pixel su un'immagine il cui lato lungo è [long] pixel: il
     * riquadro della forma con la punta della freccia, allargato di metà tratto, perché il contorno
     * è centrato sulla forma e i capi sono tondi. Gli stessi numeri di [strokeOf], letti sullo
     * schermo.
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

    /** Il riquadro sullo schermo che copre un contorno a mano libera per [points]. */
    fun extent(points: List<Offset>, stroke: Float): Rect {
        val h = stroke / 2f
        return Rect(
            points.minOf { it.x } - h, points.minOf { it.y } - h,
            points.maxOf { it.x } + h, points.maxOf { it.y } + h
        )
    }

    /**
     * Lo spostamento che posa sui bordi di [frame] i lati di [box] vicini a loro, coi bordi su cui
     * poggia (richiesta dell'utente: uno scatto calamitato per tenere un elemento a filo del
     * bordo).
     *
     * ⚠️ Un lato entro [reach] da un bordo ci si posa, da dentro o da fuori; più lontano resta,
     * così un elemento può ancora andare oltre il bordo, come chiesto. Su ogni asse vince il bordo
     * più vicino, e si muovono solo i lati in [sides]. Pixel dello schermo, come [snap].
     * ⚠️ **Ci si posa anche sui riquadri degli [others]** (richiesta dell'utente: allineare agli
     * altri elementi): un lato che si muove, o il centro quando si muovono tutti e due, si posa su
     * un lato o sul centro di un altro elemento entro lo stesso [reach]. Su ogni asse vince il più
     * vicino, e a parità il bordo dell'immagine; [lines] dice quali guide mostrare.
     * ⚠️ **E sul centro dell'immagine, per il centro di un elemento che si sposta** (nota A
     * dell'utente sul giro della 4.90): solo il centro, perché l'utente ha chiesto la centratura, e
     * solo quando [centred] dice che l'elemento si sta spostando, così l'inizio di un elemento
     * nuovo non è tirato là.
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
            // A parità di distanza vince il primo offerto: il bordo basso, poi l'alto, poi gli
            // elementi.
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
     * Le guide fra [box] e gli [others] con cui si allinea, come segmenti sullo schermo: dove un
     * lato o il centro di [box] incontra un lato o il centro di un altro riquadro, una linea
     * dall'estremo più lontano dell'uno a quello dell'altro, così la guida dice con quale elemento
     * si allinea. Lo stesso [ON] dei bordi. Un [box] centrato sul centro di [frame], l'immagine
     * sullo schermo, ha anche la guida di quel centro, su tutta l'immagine.
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
     * [b], l'estremo che il dito disegna, spostato perché il contorno di uno strumento a due punti
     * [pen] da [a] si posi sui bordi di [frame], coi bordi su cui poggia. Si muovono solo i lati
     * che [b] disegna, così l'inizio resta dov'è, e solo lungo gli assi in [axes]: una linea posata
     * sull'orizzontale tiene la sua altezza.
     * ⚠️ Fino a tre passate, perché la punta della freccia gira con l'estremo su cui è, e una
     * passata che non muove niente termina prima; per gli altri strumenti la prima è esatta.
     * L'estremo si posa anche sugli [others], come in [rest].
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

    /** I bordi di [frame] su cui è un lato di [box], entro una frazione di pixel. */
    fun on(box: Rect, frame: Rect): Set<ImageEdge> = buildSet {
        if (abs(box.left - frame.left) < ON) add(ImageEdge.LEFT)
        if (abs(box.top - frame.top) < ON) add(ImageEdge.TOP)
        if (abs(box.right - frame.right) < ON) add(ImageEdge.RIGHT)
        if (abs(box.bottom - frame.bottom) < ON) add(ImageEdge.BOTTOM)
    }

    /**
     * Quanto oltre il riquadro dell'inizio deve essere un lato per appartenere all'estremo, in
     * pixel.
     */
    private const val OWN = 0.01f

    /** Quanto vicino deve essere un lato a un bordo per esserci sopra, in pixel. */
    private const val ON = 0.5f
}

/** Le due direzioni su cui scatta una linea o una freccia ([Draw.snap]). */
enum class SnapAxis { HORIZONTAL, VERTICAL }

/** I quattro bordi dell'immagine visibile su cui poggia un elemento del disegno ([Draw.rest]). */
enum class ImageEdge { LEFT, TOP, RIGHT, BOTTOM }

package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader.TileMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * ⚠️ **Il modulo Geometria dell'editor completo non passa dallo shader.** I moduli del colore
 * dicono di che colore è un pixel, e il loro conto gira in AGSL (`Adjust.kt`); questo dice dove va
 * un pixel. Nello shader non funzionerebbe: il salvataggio lavora a tessere, ognuna legge la sua
 * porzione di sorgente, e una deformazione fa leggere a un pixel un punto che può essere dall'altra
 * parte dell'immagine.
 *
 * La regola di fondo resta: la stessa matematica non si scrive due volte. Il conto è scritto una
 * volta, in Kotlin, e l'anteprima del palco e il salvataggio passano tutti e due da [Warp.draw].
 * In più il banco lo può provare: andata e ritorno, scala di copertura e griglia.
 *
 * ⚠️ **La geometria viene per ultima nella catena, dopo il colore**: il Dettaglio guarda i pixel
 * vicini e li deve vedere com'erano nel file. Deformando prima, misurerebbe una nitidezza che il
 * ricampionamento ha già ammorbidito.
 */

/**
 * Il modulo **Geometria**: dove va a finire un pixel, cioè come l'immagine si raddrizza, si stira,
 * si corregge della prospettiva e della curvatura dell'obiettivo.
 *
 * Cinque cursori: [straighten] e [aspect] sono una rotazione e una scala, che tengono dritte le
 * righe dritte; [horizontal] e [vertical] sono un keystone, che stringe un lato e allarga
 * l'opposto; [distortion] è radiale, l'unica che curva le righe. Sono frazioni da -1 a +1,
 * mostrate da -100 a +100 come in Lightroom; quanto valgono a fondo corsa lo dicono le costanti
 * di [Warp].
 *
 * Due strumenti si danno col dito sul palco: 'Angoli' ([corners]), un'omografia come i due
 * keystone ma coi quattro vertici dichiarati uno per uno, e 'Fluidifica' ([liquify]).
 */
data class Geometry(
    val straighten: Float = 0f,
    val aspect: Float = 0f,
    val horizontal: Float = 0f,
    val vertical: Float = 0f,
    val distortion: Float = 0f,
    /** Lo strumento 'Angoli': vedi [Corners]. */
    val corners: Corners = Corners.NONE,
    /** Lo strumento 'Fluidifica': gli scarti della maglia in coordinate normalizzate. */
    val liquify: Liquify = Liquify.NONE
) {

    /** Se questo modulo non sposta un pixel: vedi la nota sulla tolleranza in [Light.idle]. */
    val idle: Boolean
        get() = abs(straighten) < DEAD && abs(aspect) < DEAD && abs(horizontal) < DEAD &&
            abs(vertical) < DEAD && abs(distortion) < DEAD && corners.idle && liquify.idle

    companion object {
        val NONE = Geometry()

        private const val DEAD = 0.0005f
    }
}

/**
 * La deformazione libera di 'Fluidifica'. Ogni vertice conserva uno scarto normalizzato, così la
 * stessa maglia vale per l'anteprima e per il file pieno. Il bordo resta fermo: una pennellata non
 * può scoprire il fondo dell'immagine.
 */
class Liquify private constructor(private val shifts: FloatArray) {
    val idle: Boolean get() = shifts.isEmpty()

    fun dx(i: Int, j: Int): Float = if (idle) 0f else shifts[index(i, j)]
    fun dy(i: Int, j: Int): Float = if (idle) 0f else shifts[index(i, j) + 1]

    /** Applica un tratto del pennello; tutte le misure sono frazioni della misura dell'immagine. */
    fun stroke(
        x: Float,
        y: Float,
        moveX: Float,
        moveY: Float,
        radius: Float,
        strength: Float,
        rebuild: Boolean,
        aspect: Float = 1f,
        geometry: Geometry = Geometry.NONE
    ): Liquify {
        val out = if (idle) FloatArray(POINTS * POINTS * 2) else shifts.copyOf()
        val safeRadius = radius.coerceIn(MIN_RADIUS, MAX_RADIUS)
        val safeStrength = strength.coerceIn(0f, 1f)
        val safeAspect = aspect.coerceAtLeast(0.001f)
        val unitX = min(1f, safeAspect)
        val unitY = min(1f, 1f / safeAspect)
        // Il pennello colpisce i vertici come appaiono, dopo Geometria e le pennellate precedenti.
        // Un lato lungo pari a due dà a Warp le stesse unità isotrope della vista vera.
        val base = geometry.copy(liquify = NONE)
        val plan = if (base.idle) null else Warp.plan(base, unitX, unitY, 2f * unitX, 2f * unitY)
        for (j in 1 until CELLS) {
            val gy = j.toFloat() / CELLS
            for (i in 1 until CELLS) {
                val gx = i.toFloat() / CELLS
                val point = plan?.map(2f * gx * unitX, 2f * gy * unitY)
                val shownX = (point?.get(0)?.div(2f) ?: (gx * unitX)) + dx(i, j) * unitX
                val shownY = (point?.get(1)?.div(2f) ?: (gy * unitY)) + dy(i, j) * unitY
                val dx = shownX - x * unitX
                val dy = shownY - y * unitY
                val distance = sqrt(dx * dx + dy * dy)
                if (distance >= safeRadius) continue
                val weight = (1f - distance / safeRadius).let { it * it } * safeStrength
                val at = index(i, j)
                if (rebuild) {
                    out[at] *= 1f - weight
                    out[at + 1] *= 1f - weight
                } else {
                    out[at] = (out[at] + moveX * weight).coerceIn(-MAX_SHIFT, MAX_SHIFT)
                    out[at + 1] = (out[at + 1] + moveY * weight).coerceIn(-MAX_SHIFT, MAX_SHIFT)
                }
            }
        }
        return if (out.all { abs(it) < DEAD }) NONE else Liquify(out)
    }

    override fun equals(other: Any?): Boolean = other is Liquify && shifts.contentEquals(other.shifts)
    override fun hashCode(): Int = shifts.contentHashCode()

    private fun index(i: Int, j: Int): Int = (j * POINTS + i) * 2

    companion object {
        const val CELLS = 128
        const val POINTS = CELLS + 1
        const val MIN_RADIUS = 1f / 60f
        const val MAX_RADIUS = 0.25f
        private const val MAX_SHIFT = 0.35f
        private const val DEAD = 0.00001f
        val NONE = Liquify(FloatArray(0))
    }
}

/**
 * Lo strumento **Angoli**: dove sono finiti i quattro vertici dell'immagine, uno per volta.
 *
 * Richiesta dell'utente (giro della 2.40): deformare l'immagine trascinando un angolo per volta,
 * sempre con un ritaglio automatico che non lasci parti vuote. Il nome dice che cosa si tocca.
 *
 * Non è un cursore: i gradi di libertà sono otto, due per angolo, e il comando sono quattro
 * maniglie sul palco, come nel Ritaglio.
 *
 * Gli scarti sono in unità isotrope, frazioni del semilato lungo, come tutto in [WarpPlan]: in
 * frazione di asse, su un'immagine larga lo stesso dito sposterebbe di quantità diverse l'angolo
 * di sopra e quello di fianco.
 *
 * ⚠️ **L'ordine è orario partendo da in alto a sinistra**, lo stesso che [Warp.quad] si aspetta:
 * cambiarlo solo qui darebbe un'omografia che incrocia due lati, un'immagine ripiegata.
 */
data class Corners(
    val x0: Float = 0f,
    val y0: Float = 0f,
    val x1: Float = 0f,
    val y1: Float = 0f,
    val x2: Float = 0f,
    val y2: Float = 0f,
    val x3: Float = 0f,
    val y3: Float = 0f
) {

    /** Se nessun angolo si è mosso. */
    val idle: Boolean
        get() = abs(x0) < DEAD && abs(y0) < DEAD && abs(x1) < DEAD && abs(y1) < DEAD &&
            abs(x2) < DEAD && abs(y2) < DEAD && abs(x3) < DEAD && abs(y3) < DEAD

    /** Lo scarto orizzontale dell'angolo [i], con [i] da 0 a 3 in senso orario da in alto a sinistra. */
    fun dx(i: Int): Float = when (i) {
        0 -> x0
        1 -> x1
        2 -> x2
        else -> x3
    }

    /** Lo scarto verticale dell'angolo [i]. */
    fun dy(i: Int): Float = when (i) {
        0 -> y0
        1 -> y1
        2 -> y2
        else -> y3
    }

    /** Gli stessi angoli con il numero [i] spostato a ([dx], [dy]). */
    fun with(i: Int, dx: Float, dy: Float): Corners = when (i) {
        0 -> copy(x0 = dx, y0 = dy)
        1 -> copy(x1 = dx, y1 = dy)
        2 -> copy(x2 = dx, y2 = dy)
        else -> copy(x3 = dx, y3 = dy)
    }

    companion object {
        val NONE = Corners()

        private const val DEAD = 0.0005f
    }
}

/**
 * Il conto della geometria già pronto per un'immagine di misura nota: gli angoli, le scale e la
 * **scala di copertura**, calcolati una volta invece che per ogni punto.
 *
 * Esiste perché la copertura costa un giro sul contorno: la griglia dell'anteprima ha più di mille
 * punti, e il conto per vertice sarebbe mille volte lo stesso numero.
 *
 * ⚠️ **Le coordinate di lavoro sono isotrope**, normalizzate sul lato lungo: una rotazione in
 * coordinate normalizzate per asse non è una rotazione, e raddrizzare un orizzonte lo
 * storcerebbe.
 */
internal data class WarpPlan(
    /** Metà del lato lungo del rettangolo, cioè l'unità in cui vivono i conti. */
    val half: Float,
    /** Il centro del rettangolo. */
    val cx: Float,
    val cy: Float,
    /** Metà larghezza e metà altezza in unità di [half]: il rettangolo da coprire. */
    val ax: Float,
    val ay: Float,
    val cosT: Float,
    val sinT: Float,
    /** Quanto l'aspetto stira in orizzontale; in verticale si stringe dello stesso fattore. */
    val stretch: Float,
    val slantX: Float,
    val slantY: Float,
    val bend: Float,
    /** L'omografia dei quattro angoli, o `null` se nessuno si è mosso: vedi [Warp.quad]. */
    val quad: Quad?,
    /** Quanto si ingrandisce perché nessun bordo resti scoperto: vedi [Warp.cover]. */
    val cover: Float
) {

    /**
     * Dove finisce il punto ([x], [y]) del rettangolo, cioè la mappatura **diretta**.
     *
     * ⚠️ **L'ordine dei passi è la specifica**: prima la curvatura dell'obiettivo, il difetto più
     * vicino al sensore; poi l'aspetto e la rotazione, che mettono l'immagine dritta; poi il
     * keystone, il punto di vista; poi gli angoli tirati a mano; per ultima la scala di copertura,
     * che serve solo a non lasciare bordi vuoti.
     */
    fun map(x: Float, y: Float): FloatArray {
        var nx = (x - cx) / half
        var ny = (y - cy) / half

        // 1. La distorsione dell'obiettivo: un punto si allontana o si avvicina al centro in
        // proporzione al quadrato della sua distanza, il primo termine del modello classico.
        // Positivo è cuscinetto, negativo è barile.
        if (bend != 0f) {
            val f = 1f + bend * (nx * nx + ny * ny)
            nx *= f
            ny *= f
        }

        // 2. L'aspetto: si stira in orizzontale e si stringe in verticale dello stesso fattore,
        // così l'area resta quella e il cursore cambia solo le proporzioni.
        if (stretch != 1f) {
            nx *= stretch
            ny /= stretch
        }

        // 3. Il raddrizzamento.
        if (sinT != 0f) {
            val rx = nx * cosT - ny * sinT
            val ry = nx * sinT + ny * cosT
            nx = rx
            ny = ry
        }

        // 4. I due keystone, uno per asse e in sequenza: un lato si allarga, quello opposto si
        // stringe di altrettanto, e la retta di mezzo resta dov'è. Il conto è su [Warp.SLANT].
        // ⚠️ **Il divisore si tiene lontano dallo zero**: un vertice all'infinito farebbe sparire
        // l'immagine.
        if (slantY != 0f && ay > 0f) {
            val u = ny / ay
            val d = max(1f + slantY * u, EDGE)
            nx = nx * (1f - slantY * slantY) / d
            ny = ay * (u + slantY) / d
        }
        if (slantX != 0f && ax > 0f) {
            val v = nx / ax
            val d = max(1f + slantX * v, EDGE)
            ny = ny * (1f - slantX * slantX) / d
            nx = ax * (v + slantX) / d
        }

        // 5. Gli angoli tirati a mano, anche loro un'omografia: per ultimi perché li detta il dito,
        // che guarda il risultato mentre tira. Davanti ai keystone, l'angolo tirato finirebbe
        // altrove.
        val q = quad
        if (q != null && ax > 0f && ay > 0f) {
            val p = q.map((nx + ax) / (2f * ax), (ny + ay) / (2f * ay))
            nx = p[0]
            ny = p[1]
        }

        return floatArrayOf(cx + nx * cover * half, cy + ny * cover * half)
    }

    /**
     * Da dove viene il punto ([x], [y]), cioè la mappatura **inversa**.
     *
     * ⚠️ **Serve al colore mirato**: il dito tocca l'immagine deformata, e il colore da leggere è
     * nel file, prima della deformazione. I passi si disfano nell'ordine contrario.
     *
     * Solo la distorsione non ha una formula chiusa, e si inverte con quattro giri di Newton, che
     * portano l'errore sotto il milionesimo per ogni valore dei cursori (lo misura il banco).
     */
    fun back(x: Float, y: Float): FloatArray {
        var nx = (x - cx) / (half * cover)
        var ny = (y - cy) / (half * cover)

        /*
         * Gli angoli si disfano per primi, perché nell'andata sono l'ultimo passo; un'omografia
         * si inverte in forma chiusa ([Quad.back]).
         */
        val q = quad
        if (q != null && ax > 0f && ay > 0f) {
            val p = q.back(nx, ny)
            nx = p[0] * 2f * ax - ax
            ny = p[1] * 2f * ay - ay
        }

        /*
         * I due keystone si disfano in ordine contrario, ognuno in forma chiusa: la mappa di un
         * asse è una Möbius, e l'inversa è la stessa espressione col segno cambiato. Il conto è
         * su [Warp.SLANT].
         */
        if (slantX != 0f && ax > 0f) {
            val v = nx / ax
            val d = max(1f - slantX * v, EDGE)
            ny /= d
            nx = ax * (v - slantX) / d
        }
        if (slantY != 0f && ay > 0f) {
            val u = ny / ay
            val d = max(1f - slantY * u, EDGE)
            nx /= d
            ny = ay * (u - slantY) / d
        }

        if (sinT != 0f) {
            val rx = nx * cosT + ny * sinT
            val ry = -nx * sinT + ny * cosT
            nx = rx
            ny = ry
        }

        if (stretch != 1f) {
            nx /= stretch
            ny *= stretch
        }

        if (bend != 0f) {
            val got = sqrt(nx * nx + ny * ny)
            if (got > 1e-6f) {
                var r = got
                repeat(NEWTON) {
                    val f = r + bend * r * r * r - got
                    val d = 1f + 3f * bend * r * r
                    if (abs(d) > 1e-6f) r -= f / d
                }
                val k = r / got
                nx *= k
                ny *= k
            }
        }

        return floatArrayOf(cx + nx * half, cy + ny * half)
    }

    private companion object {
        /** Il divisore del keystone non scende sotto questo: vedi [map]. */
        const val EDGE = 0.05f

        /** Quanti giri di Newton per invertire la distorsione: vedi [back]. */
        const val NEWTON = 4
    }
}

/**
 * L'omografia che porta il quadrato unitario sui quattro angoli dello strumento 'Angoli'.
 *
 * È la mappa 'square to quad' di Heckbert: dal quadrato unitario i conti si chiudono in una decina
 * di righe, senza risolvere un sistema a otto incognite, e il solo caso da distinguere è quello
 * affine (il parallelogramma), dove il denominatore è zero.
 *
 * La formula vive sul quadrato unitario: chi la chiama normalizza ([WarpPlan.map]) e
 * denormalizza ([WarpPlan.back]), senza una seconda derivazione sul rettangolo.
 */
internal class Quad(
    val a: Float,
    val b: Float,
    val c: Float,
    val d: Float,
    val e: Float,
    val f: Float,
    val g: Float,
    val h: Float
) {

    /** Dove finisce il punto ([u], [v]) del quadrato unitario. */
    fun map(u: Float, v: Float): FloatArray {
        val w = g * u + h * v + 1f
        val safe = if (abs(w) > EPS) w else EPS
        return floatArrayOf((a * u + b * v + c) / safe, (d * u + e * v + f) / safe)
    }

    /**
     * Da quale punto del quadrato unitario viene ([x], [y]).
     *
     * I nove coefficienti sono l'aggiunta della matrice, cioè l'inversa a meno del determinante,
     * che si semplifica nella divisione.
     */
    fun back(x: Float, y: Float): FloatArray {
        val ia = e - f * h
        val ib = c * h - b
        val ic = b * f - c * e
        val id = f * g - d
        val ie = a - c * g
        val iff = c * d - a * f
        val ig = d * h - e * g
        val ih = b * g - a * h
        val ii = a * e - b * d
        val w = ig * x + ih * y + ii
        val safe = if (abs(w) > EPS) w else EPS
        return floatArrayOf((ia * x + ib * y + ic) / safe, (id * x + ie * y + iff) / safe)
    }

    private companion object {
        /** Un denominatore non scende sotto questo: un punto all'infinito farebbe sparire l'immagine. */
        const val EPS = 1e-6f
    }
}

/**
 * Come una [Geometry] diventa un disegno: il piano, la griglia dei vertici, e le due strade che la
 * applicano.
 *
 * ⚠️ **La deformazione si disegna come una maglia di triangoli**: rotazione, aspetto e keystone
 * sarebbero una matrice 3x3, ma la distorsione e Fluidifica curvano le righe, e nessuna matrice lo
 * sa fare. Con una maglia tutti i comandi passano dalla stessa strada.
 * - Il costo è l'approssimazione: dentro una cella la deformazione è lineare, quindi la curvatura
 *   è a tratti. Con [CELLS] celle per lato lo scarto resta sotto il pixel. È la stessa via di
 *   `drawBitmapMesh`.
 * - Le coordinate della texture sono la griglia non deformata: il disegno prende il pixel di quel
 *   punto e lo posa nel vertice deformato, che è la definizione della mappatura diretta.
 */
internal object Warp {

    /** Quanti gradi vale il raddrizzamento al fondo della corsa. */
    const val TILT = 15f

    /** Quanto l'aspetto stira al fondo della corsa. */
    const val STRETCH = 0.25f

    /**
     * Quanto pesa un keystone al fondo della corsa, cioè di quanto si allarga il lato che si apre.
     *
     * ⚠️ **Il perno è la retta di mezzo** (riscontro `geo-dritto` dell'utente): una divisione
     * prospettica unica (`nx` e `ny` divisi per `1 + sx*nx + sy*ny`) non è centrata, e a 0,35 il
     * lato che si apre crescerebbe del 54% e quello che si stringe calerebbe del 26%: il trapezio
     * scivolerebbe da una parte.
     *
     * Ogni asse è una Möbius sul suo lato, e i due keystone si applicano in sequenza. Per il
     * verticale, con `u = ny / ay` e `s` questo coefficiente: `u -> (u + s) / (1 + s*u)` e
     * `nx -> nx * (1 - s^2) / (1 + s*u)`. Tre proprietà:
     * - i bordi non si muovono (`u = ±1` resta `±1`), e il trapezio è isoscele: il lato di sopra
     *   vale `1 + s` e quello di sotto `1 - s`, cioè +35% e -35%;
     * - il centro dei quattro vertici resta il centro;
     * - resta un'omografia, quindi le righe dritte restano dritte. ⚠️ Deformare la sola coordinata
     *   trasversale (`nx /= 1 + s*ny`) sarebbe più corto, ma manda una retta in un'iperbole, cioè
     *   curva le verticali.
     *
     * ⚠️ **Si normalizza sul lato vero, non su quello lungo**: le coordinate sono isotrope
     * ([WarpPlan]), quindi su un'immagine larga il bordo di sopra è a `ay`. Sul lato lungo il perno
     * tornerebbe a scappare.
     *
     * Il prezzo è un filo di copertura in più: su un 4:3 a fondo corsa la scala passa da 1,26 a
     * 1,34, perché un trapezio centrato rientra da tutte e due le parti. Il valore a fondo corsa è
     * confermato dall'utente (risposta `bene` a `d-geo-corsa`).
     */
    const val SLANT = 0.35f

    /**
     * Quanto curva la distorsione al fondo della corsa, come coefficiente del raggio quadrato.
     *
     * ⚠️ **È sotto il tetto oltre il quale il disegno si ripiega**: la mappa radiale
     * `r * (1 + k * r^2)` è invertibile finché la derivata `1 + 3 * k * r^2` resta positiva, cioè
     * finché `k > -1 / (3 * r^2)`. Il raggio più grande è quello dell'angolo, al massimo la radice
     * di due (un'immagine quadrata): là il tetto è `-0,167`. Oltre, l'inversa non esiste agli
     * angoli, e il palco disegna senza errori mentre il colore mirato prende un pixel da un'altra
     * parte; lo prende la prova dell'andata e ritorno.
     * Il margine è voluto: a `0,12` la derivata all'angolo di un quadrato vale `0,28`; a `0,16`
     * sarebbe un decimo, con un angolo che quasi non si muove e un Newton molto più lento. Una
     * correzione a barile di un grandangolo è ben dentro questa corsa.
     */
    const val BEND = 0.12f

    /**
     * Quante celle per lato ha la maglia.
     *
     * Il numero governa l'errore dentro le celle: la maglia è esatta ai vertici. Fluidifica ha la
     * sua maglia ([Liquify.CELLS]).
     */
    const val CELLS = 32

    /**
     * Quanti punti si campionano su ogni lato del contorno per la scala di copertura.
     *
     * Non bastano i quattro angoli: con la distorsione a barile il punto più rientrato è in mezzo
     * a un lato.
     * ⚠️ **Il minimo della [cover] cade fra due campioni**, quindi una griglia rada lo sopravvaluta
     * e la scala risulta un filo corta: con 24 campioni e il raddrizzamento a fondo corsa la prova
     * contava quattro pixel di fondo sul bordo. Il conto si fa una volta per piano, quindi alzare
     * il numero costa poco.
     */
    const val EDGE_STEPS = 64

    /**
     * Il conto pronto per un rettangolo largo [w] e alto [h] col centro in ([cx], [cy]).
     *
     * Il rettangolo si passa col suo centro: l'anteprima lo disegna dove la vista lo mette, il
     * salvataggio all'origine.
     */
    fun plan(
        geo: Geometry,
        cx: Float,
        cy: Float,
        w: Float,
        h: Float,
        /**
         * La scala di lavoro al posto di quella di copertura: quanto rimpicciolire l'immagine
         * intera dentro il rettangolo.
         *
         * ⚠️ **Serve allo strumento 'Angoli'**: con la copertura un angolo tirato in fuori finirebbe
         * oltre il bordo dello schermo, maniglia compresa. Armato lo strumento, il palco chiede
         * l'immagine intera e disegna sopra il riquadro che resterà.
         * ⚠️ **La scala non dipende dagli angoli**: il contenimento si misura sulla geometria senza
         * di loro, così la vista non si muove e la maniglia segue il dito. Sul contorno vero,
         * l'immagine si stringerebbe di quanto si tira, e la maniglia resterebbe ferma al bordo.
         * Il salvataggio non lo passa mai: sul file va la copertura, cioè il ritaglio automatico
         * che l'utente ha chiesto.
         */
        hold: Float? = null
    ): WarpPlan {
        val half = max(w, h) / 2f
        val safe = if (half > 0f) half else 1f
        val angle = geo.straighten * TILT * Math.PI.toFloat() / 180f
        val ax = (w / 2f) / safe
        val ay = (h / 2f) / safe
        val plain = WarpPlan(
            half = safe,
            cx = cx,
            cy = cy,
            ax = ax,
            ay = ay,
            cosT = cos(angle),
            sinT = sin(angle),
            stretch = 1f + geo.aspect * STRETCH,
            slantX = geo.horizontal * SLANT,
            slantY = geo.vertical * SLANT,
            bend = geo.distortion * BEND,
            quad = quad(geo.corners, ax, ay),
            cover = 1f
        )
        val scala =
            if (hold != null) hold * fit(plain.copy(quad = null)) else cover(plain)
        return plain.copy(cover = scala)
    }

    /**
     * Quanto può allontanarsi un angolo dal suo posto, in unità isotrope.
     *
     * Il limite fin dove l'app segue il dito. Un terzo di semilato lungo basta a raddrizzare
     * qualunque facciata, tiene la copertura ragionevole (un angolo tirato dentro di un terzo
     * chiede già una scala di una volta e mezzo) e la maniglia dentro lo schermo: il conto è su
     * `ARMED_FIT`, in `AdvancedEditorScreen.kt`.
     */
    const val PULL = 0.35f

    /**
     * L'omografia degli angoli, o `null` se nessuno si è mosso.
     *
     * Il `null` è la neutralità: a riposo la formula dà l'identità in aritmetica esatta, non in
     * `Float`, e applicarla sposterebbe i pixel di un millesimo.
     */
    fun quad(c: Corners, ax: Float, ay: Float): Quad? {
        if (c.idle || ax <= 0f || ay <= 0f) return null
        // I quattro vertici di arrivo, in unità isotrope e in senso orario da in alto a sinistra.
        val x0 = -ax + c.x0
        val y0 = -ay + c.y0
        val x1 = ax + c.x1
        val y1 = -ay + c.y1
        val x2 = ax + c.x2
        val y2 = ay + c.y2
        val x3 = -ax + c.x3
        val y3 = ay + c.y3

        val sx = x0 - x1 + x2 - x3
        val sy = y0 - y1 + y2 - y3
        if (abs(sx) < FLAT && abs(sy) < FLAT) {
            // Un parallelogramma: la mappa è affine, cioè un'omografia col denominatore costante.
            return Quad(
                a = x1 - x0,
                b = x3 - x0,
                c = x0,
                d = y1 - y0,
                e = y3 - y0,
                f = y0,
                g = 0f,
                h = 0f
            )
        }
        val dx1 = x1 - x2
        val dx2 = x3 - x2
        val dy1 = y1 - y2
        val dy2 = y3 - y2
        val den = dx1 * dy2 - dx2 * dy1
        if (abs(den) < FLAT) return null
        val g = (sx * dy2 - sy * dx2) / den
        val h = (dx1 * sy - dy1 * sx) / den
        return Quad(
            a = x1 - x0 + g * x1,
            b = x3 - x0 + h * x3,
            c = x0,
            d = y1 - y0 + g * y1,
            e = y3 - y0 + h * y3,
            f = y0,
            g = g,
            h = h
        )
    }

    /**
     * Se i quattro angoli formano ancora un quadrilatero **convesso**, cioè se la deformazione si
     * può disegnare.
     *
     * ⚠️ **Senza questo controllo l'immagine si ripiega, senza errori**: un angolo oltre la
     * diagonale incrocia due lati, e l'omografia mette una parte dell'immagine sopra l'altra a
     * rovescio. [PULL] da solo non basta: su un'immagine molto allungata supera la semialtezza.
     *
     * Si misura col verso dei quattro prodotti vettoriali, che in un quadrilatero convesso hanno
     * tutti lo stesso segno. Il chiamante la usa per fermare il dito, così l'angolo non scatta
     * indietro al confine.
     */
    fun convex(c: Corners, ax: Float, ay: Float): Boolean {
        val xs = floatArrayOf(-ax + c.x0, ax + c.x1, ax + c.x2, -ax + c.x3)
        val ys = floatArrayOf(-ay + c.y0, -ay + c.y1, ay + c.y2, ay + c.y3)
        var sign = 0f
        for (i in 0..3) {
            val j = (i + 1) % 4
            val k = (i + 2) % 4
            val cross = (xs[j] - xs[i]) * (ys[k] - ys[j]) - (ys[j] - ys[i]) * (xs[k] - xs[j])
            if (abs(cross) < FLAT) return false
            if (sign == 0f) sign = cross else if (sign * cross < 0f) return false
        }
        return true
    }

    /** Sotto questo un determinante si legge come zero: vedi [quad] e [convex]. */
    private const val FLAT = 1e-6f

    /**
     * Di quanto si deve ingrandire l'immagine deformata perché il rettangolo resti coperto.
     *
     * Senza, un raddrizzamento lascerebbe quattro cunei vuoti agli angoli. La scala si ricava, e
     * chi raddrizza non deve ingrandire a mano.
     *
     * Si misura sul contorno: il punto più rientrato è un angolo con un keystone, il mezzo di un
     * lato con la distorsione a barile.
     *
     * ⚠️ **Regge perché la regione deformata contiene il centro e non si ripiega**: entro le
     * costanti qui sopra la mappatura è monotona lungo ogni raggio. Chi le alza di molto guardi
     * prima questo conto.
     */
    fun cover(plan: WarpPlan): Float {
        var worst = Float.MAX_VALUE
        val w = plan.ax * plan.half
        val h = plan.ay * plan.half
        for (i in 0..EDGE_STEPS) {
            val t = i.toFloat() / EDGE_STEPS
            val xs = plan.cx - w + 2f * w * t
            val ys = plan.cy - h + 2f * h * t
            // I quattro lati del contorno: sopra, sotto, sinistra, destra.
            for (p in listOf(
                plan.map(xs, plan.cy - h),
                plan.map(xs, plan.cy + h),
                plan.map(plan.cx - w, ys),
                plan.map(plan.cx + w, ys)
            )) {
                val dx = if (w > 0f) abs(p[0] - plan.cx) / w else 0f
                val dy = if (h > 0f) abs(p[1] - plan.cy) / h else 0f
                worst = min(worst, max(dx, dy))
            }
        }
        if (worst <= 0f || worst >= 1f || worst == Float.MAX_VALUE) return 1f
        return 1f / worst
    }

    /**
     * Di quanto si deve **rimpicciolire** l'immagine deformata perché entri tutta nel rettangolo.
     *
     * È il rovescio di [cover], per lo strumento 'Angoli': cerca il punto più sporgente del
     * contorno invece del più rientrato, con lo stesso ciclo. Non supera mai uno: una
     * deformazione che rientra da tutte le parti lascia la vista com'è.
     */
    fun fit(plan: WarpPlan): Float {
        var worst = 0f
        val w = plan.ax * plan.half
        val h = plan.ay * plan.half
        for (i in 0..EDGE_STEPS) {
            val t = i.toFloat() / EDGE_STEPS
            val xs = plan.cx - w + 2f * w * t
            val ys = plan.cy - h + 2f * h * t
            for (p in listOf(
                plan.map(xs, plan.cy - h),
                plan.map(xs, plan.cy + h),
                plan.map(plan.cx - w, ys),
                plan.map(plan.cx + w, ys)
            )) {
                val dx = if (w > 0f) abs(p[0] - plan.cx) / w else 0f
                val dy = if (h > 0f) abs(p[1] - plan.cy) / h else 0f
                worst = max(worst, max(dx, dy))
            }
        }
        if (worst <= 1f) return 1f
        return 1f / worst
    }

    /**
     * I vertici della maglia: dove ogni punto della griglia **arriva**.
     *
     * La griglia di partenza è uniforme sul rettangolo, ed è anche l'elenco delle coordinate da cui
     * il disegno legge ([grid]): le due insieme sono la mappatura.
     */
    fun points(plan: WarpPlan, dest: RectF, cells: Int = CELLS): FloatArray {
        val out = FloatArray((cells + 1) * (cells + 1) * 2)
        var at = 0
        for (j in 0..cells) {
            val y = dest.top + dest.height() * j / cells
            for (i in 0..cells) {
                val x = dest.left + dest.width() * i / cells
                val p = plan.map(x, y)
                out[at++] = p[0]
                out[at++] = p[1]
            }
        }
        return out
    }

    /** La griglia non deformata, cioè da dove si legge ogni vertice. */
    fun grid(dest: RectF, cells: Int = CELLS): FloatArray {
        val out = FloatArray((cells + 1) * (cells + 1) * 2)
        var at = 0
        for (j in 0..cells) {
            val y = dest.top + dest.height() * j / cells
            for (i in 0..cells) {
                val x = dest.left + dest.width() * i / cells
                out[at++] = x
                out[at++] = y
            }
        }
        return out
    }

    /**
     * I triangoli della maglia, due per cella.
     *
     * Gli indici sono `Short` perché `drawVertices` li vuole così: anche la maglia di Fluidifica,
     * 129 per 129 vertici, resta sotto il tetto di un indice a sedici bit.
     */
    fun indices(cells: Int = CELLS): ShortArray {
        val out = ShortArray(cells * cells * 6)
        var at = 0
        val row = cells + 1
        for (j in 0 until cells) {
            for (i in 0 until cells) {
                val a = (j * row + i).toShort()
                val b = (j * row + i + 1).toShort()
                val c = ((j + 1) * row + i).toShort()
                val d = ((j + 1) * row + i + 1).toShort()
                out[at++] = a
                out[at++] = b
                out[at++] = c
                out[at++] = b
                out[at++] = d
                out[at++] = c
            }
        }
        return out
    }

    /** Gli indici non dipendono né dall'immagine né dalla pennellata. */
    private val geometryOrder by lazy { indices(CELLS) }
    private val liquifyOrder by lazy { indices(Liquify.CELLS) }

    /** I vertici deformati e la griglia da cui leggono, cioè tutto quello che [draw] e [back] usano. */
    internal class Mesh(
        val plan: WarpPlan,
        val dest: RectF,
        val liquify: Liquify,
        val verts: FloatArray,
        val tex: FloatArray
    )

    /*
     * ⚠️ **L'ultima maglia resta da parte** (rilievi A25 e A26): Correggi e il Disegno riportano
     * al file un punto per volta, sedici per ogni poligono di una pennellata, e il palco disegna
     * immagine, disegno e selezione con la stessa maglia nello stesso fotogramma; senza, ogni
     * chiamata ricostruirebbe i 16.641 vertici di Fluidifica (presidiato da `MagliaTest`).
     * Un posto solo, perché le chiamate uguali arrivano di fila. La maglia non cambia più e si
     * sostituisce intera, quindi un salvataggio su un altro thread legge l'una o l'altra.
     */
    @Volatile
    private var lastMesh: Mesh? = null

    internal fun mesh(plan: WarpPlan, dest: RectF, liquify: Liquify): Mesh {
        lastMesh?.let { if (it.plan == plan && it.dest == dest && it.liquify == liquify) return it }
        DrawWork.meshes++
        val cells = if (liquify.idle) CELLS else Liquify.CELLS
        val verts = points(plan, dest, cells)
        if (!liquify.idle) {
            var at = 0
            for (j in 0..cells) for (i in 0..cells) {
                verts[at++] += liquify.dx(i, j) * dest.width()
                verts[at++] += liquify.dy(i, j) * dest.height()
            }
        }
        return Mesh(plan, RectF(dest), liquify, verts, grid(dest, cells)).also { lastMesh = it }
    }

    /**
     * Disegna dentro [dest] quello che [paint] sa dipingere, deformato da [plan].
     *
     * ⚠️ **È la funzione comune all'anteprima e al salvataggio**: il palco le passa il pennello con
     * lo shader del colore, il salvataggio un pennello col file già sviluppato, e la maglia è la
     * stessa.
     *
     * Le coordinate della texture sono in unità del pennello: chi chiama costruisce un pennello
     * che dipinge [dest] con l'immagine intera, quindi la griglia non deformata è già l'elenco dei
     * punti da cui leggere.
     */
    fun draw(
        canvas: Canvas,
        dest: RectF,
        plan: WarpPlan,
        paint: Paint,
        liquify: Liquify = Liquify.NONE
    ) {
        val maglia = mesh(plan, dest, liquify)
        val verts = maglia.verts
        val order = if (liquify.idle) geometryOrder else liquifyOrder
        canvas.drawVertices(
            Canvas.VertexMode.TRIANGLES,
            verts.size,
            verts,
            0,
            maglia.tex,
            0,
            null,
            0,
            order,
            0,
            order.size,
            paint
        )
    }

    /**
     * Dove arriva sullo schermo il punto ([x], [y]) di [dest], deformato da [plan] e da
     * [liquify]: il rovescio di [back].
     *
     * Serve a disegnare sopra l'immagine deformata qualcosa che non si deforma: i vertici
     * dell'elemento scelto nel modulo Disegno (G2), tondi a misura fissa nel punto in cui la maglia
     * porta il vertice.
     * Lo scarto di Fluidifica si legge sulla griglia di [draw], interpolato fra i quattro vertici
     * della cella; la maglia interpola nei triangoli, quindi i due conti differiscono di una
     * frazione di pixel.
     */
    fun to(plan: WarpPlan, dest: RectF, liquify: Liquify, x: Float, y: Float): FloatArray {
        val p = plan.map(x, y)
        if (liquify.idle || dest.width() <= 0f || dest.height() <= 0f) return p
        val cells = Liquify.CELLS
        val gx = ((x - dest.left) / dest.width() * cells).coerceIn(0f, cells.toFloat())
        val gy = ((y - dest.top) / dest.height() * cells).coerceIn(0f, cells.toFloat())
        val i = gx.toInt().coerceAtMost(cells - 1)
        val j = gy.toInt().coerceAtMost(cells - 1)
        val fx = gx - i
        val fy = gy - j
        fun mix(f: (Int, Int) -> Float): Float =
            (f(i, j) * (1f - fx) + f(i + 1, j) * fx) * (1f - fy) +
                (f(i, j + 1) * (1f - fx) + f(i + 1, j + 1) * fx) * fy
        p[0] += mix(liquify::dx) * dest.width()
        p[1] += mix(liquify::dy) * dest.height()
        return p
    }

    /** Il punto della texture che cade sotto ([x], [y]) nella maglia di Fluidifica. */
    fun back(plan: WarpPlan, dest: RectF, liquify: Liquify, x: Float, y: Float): FloatArray {
        if (liquify.idle) return plan.back(x, y)
        val cells = Liquify.CELLS
        val maglia = mesh(plan, dest, liquify)
        val verts = maglia.verts
        val tex = maglia.tex
        val row = cells + 1
        for (j in 0 until cells) for (i in 0 until cells) {
            val a = j * row + i
            val b = a + 1
            val c = a + row
            val d = c + 1
            inside(verts, tex, a, b, c, x, y)?.let { return it }
            inside(verts, tex, b, d, c, x, y)?.let { return it }
        }
        return plan.back(x, y)
    }

    private fun inside(
        verts: FloatArray,
        tex: FloatArray,
        ia: Int,
        ib: Int,
        ic: Int,
        x: Float,
        y: Float
    ): FloatArray? {
        val ax = verts[ia * 2]; val ay = verts[ia * 2 + 1]
        val bx = verts[ib * 2]; val by = verts[ib * 2 + 1]
        val cx = verts[ic * 2]; val cy = verts[ic * 2 + 1]
        val den = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
        if (abs(den) < 1e-6f) return null
        val wa = ((by - cy) * (x - cx) + (cx - bx) * (y - cy)) / den
        val wb = ((cy - ay) * (x - cx) + (ax - cx) * (y - cy)) / den
        val wc = 1f - wa - wb
        if (wa < -1e-4f || wb < -1e-4f || wc < -1e-4f) return null
        return floatArrayOf(
            wa * tex[ia * 2] + wb * tex[ib * 2] + wc * tex[ic * 2],
            wa * tex[ia * 2 + 1] + wb * tex[ib * 2 + 1] + wc * tex[ic * 2 + 1]
        )
    }

    /**
     * [source] deformato da [geo], oppure `null` se la mappa di pixel non si è potuta costruire.
     *
     * Qui non serve la scheda grafica, al contrario del conto del colore: una maglia di triangoli
     * si disegna anche su una tela in memoria, quindi il file pieno si deforma in un colpo solo,
     * senza tessere.
     *
     * L'immagine risulta della stessa misura: la deformazione lavora dentro il riquadro, garantita
     * dalla scala di copertura. Il ritaglio lo decide l'utente.
     */
    fun render(source: Bitmap, geo: Geometry): Bitmap? {
        if (geo.idle) return null
        val w = source.width
        val h = source.height
        if (w <= 0 || h <= 0) return null
        val out = try {
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            return null
        }
        val dest = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val plan = plan(geo, w / 2f, h / 2f, w.toFloat(), h.toFloat())
        val paint = Paint().apply {
            isAntiAlias = true
            /*
             * Il filtro: i vertici cadono quasi sempre fra un pixel e l'altro, e senza il
             * ricampionamento prenderebbe il pixel più vicino, coi gradini su ogni riga inclinata.
             */
            isFilterBitmap = true
            shader = BitmapShader(source, TileMode.CLAMP, TileMode.CLAMP)
        }
        return try {
            draw(Canvas(out), dest, plan, paint, geo.liquify)
            out
        } catch (e: RuntimeException) {
            out.recycle()
            null
        }
    }
}

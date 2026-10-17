package io.github.roccobot.aiv

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Ricostruzione deterministica per esempi, su più scale (EM alla Wexler con la ricerca
 * PatchMatch). I donatori sono tutti fuori dalla selezione originale: un pixel generato non è mai
 * preso per un dato nuovo. Tutte le letture sono dal ritaglio locale ricevuto.
 *
 * ⚠️⚠️ **Il buco si riempie su una piramide**: al livello più grossolano è largo poche toppe,
 * quindi una toppa 7x7 vede già la struttura intorno; lì lo semina il riempimento avido ([seed]).
 * Ogni livello più fine ingrandisce il campo dei donatori scelti, ricostruisce il buco coi propri
 * pixel e lo rifinisce con PatchMatch. Misurato su quattro scene sintetiche (diagonale, mattoni,
 * erba, cielo con orizzonte), buchi da 40 a 220 px: il solo riempimento avido spezzava le fughe
 * dei mattoni e l'orizzonte, la piramide le tiene dritte.
 * ⚠️ **Si ingrandisce il campo dei donatori, non i pixel**: ingrandendo i pixel il buco diventa
 * una media piatta, perché i pixel sfocati somigliano di più ai donatori piatti.
 * Limite noto: una trama morbida come le nuvole diventa cielo liscio, con un bordo appena visibile.
 */
internal object Inpaint {
    /** Il raggio della toppa della ricerca e del voto: 7x7. */
    private const val RADIUS = 3

    /** Il raggio della toppa del riempimento avido sul livello più grossolano. */
    private const val SEED_RADIUS = 3

    /** La piramide si ferma quando il buco è largo tanti pixel, o il ritaglio è così stretto. */
    private const val COARSE_SPAN = 4 * RADIUS
    private const val COARSE_SIDE = 24
    private const val COARSE_EM = 8
    private const val FINE_EM = 3
    private const val SEARCH_ROUNDS = 4

    /**
     * ⚠️ Un livello con più toppe da riempire di così ricostruisce solo dal campo ingrandito,
     * senza cercare di nuovo: il costo di una selezione grande su un'immagine ad alta risoluzione
     * resta limitato, e la trama viene comunque dai pixel veri di quel livello.
     */
    private const val SEARCH_BUDGET = 120_000

    /** [blocked] è il buco più i pixel quasi trasparenti, il cui colore non è un dato. */
    private class Level(val width: Int, val height: Int, val image: FloatArray, val hole: BooleanArray, val blocked: BooleanArray)

    /**
     * La larghezza in pixel del bordo sfumato per una selezione larga [span] pixel: un decimo
     * della selezione, fra 3 e 24 pixel (riscontro dell'utente sulla voce `3.52-01`: bordi troppo
     * netti).
     */
    fun feather(span: Int): Int = (span / 10).coerceIn(3, 24)

    /**
     * Ripara [mask] e sfuma il risultato nell'immagine intatta per [feather] pixel fuori dalla
     * selezione. La fascia si ricostruisce insieme al buco, poi si mescola con l'originale, da
     * tutta ricostruzione sulla selezione a niente sul bordo esterno, lungo uno smoothstep: dentro
     * la selezione il difetto non torna. Con [feather] a 0 il bordo resta netto.
     */
    fun repair(
        pixels: IntArray,
        width: Int,
        height: Int,
        mask: BooleanArray,
        feather: Int = 0,
        checkpoint: () -> Unit = {},
    ): IntArray? {
        require(feather >= 0)
        if (feather == 0) return fill(pixels, width, height, mask, checkpoint)
        val distance = distanceFrom(mask, width, height)
        val grown = BooleanArray(mask.size) { mask[it] || distance[it] <= feather }
        // Una fascia che non lascia donatori ripiega sul bordo netto, che è meglio di niente.
        val healed = fill(pixels, width, height, grown, checkpoint) ?: return fill(pixels, width, height, mask, checkpoint)
        val result = pixels.copyOf()
        for (index in result.indices) {
            if (!grown[index]) continue
            if (mask[index]) {
                result[index] = healed[index]
                continue
            }
            val t = 1f - distance[index] / (feather + 1f)
            val weight = t * t * (3f - 2f * t)
            var mixed = pixels[index] and 0xff000000.toInt()
            for (shift in intArrayOf(16, 8, 0)) {
                val a = pixels[index] ushr shift and 255
                val b = healed[index] ushr shift and 255
                mixed = mixed or (((a + (b - a) * weight) + 0.5f).toInt().coerceIn(0, 255) shl shift)
            }
            result[index] = mixed
        }
        return result
    }

    /** Distanza quasi euclidea (chamfer 1 e radice di 2) dal pixel selezionato più vicino. */
    private fun distanceFrom(
        mask: BooleanArray,
        width: Int,
        height: Int,
    ): FloatArray {
        val far = Float.MAX_VALUE / 4
        val diagonal = sqrt(2f)
        val distance = FloatArray(mask.size) { if (mask[it]) 0f else far }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                var d = distance[i]
                if (x > 0) d = min(d, distance[i - 1] + 1f)
                if (y > 0) {
                    d = min(d, distance[i - width] + 1f)
                    if (x > 0) d = min(d, distance[i - width - 1] + diagonal)
                    if (x < width - 1) d = min(d, distance[i - width + 1] + diagonal)
                }
                distance[i] = d
            }
        }
        for (y in height - 1 downTo 0) {
            for (x in width - 1 downTo 0) {
                val i = y * width + x
                var d = distance[i]
                if (x < width - 1) d = min(d, distance[i + 1] + 1f)
                if (y < height - 1) {
                    d = min(d, distance[i + width] + 1f)
                    if (x < width - 1) d = min(d, distance[i + width + 1] + diagonal)
                    if (x > 0) d = min(d, distance[i + width - 1] + diagonal)
                }
                distance[i] = d
            }
        }
        return distance
    }

    private fun fill(
        pixels: IntArray,
        width: Int,
        height: Int,
        mask: BooleanArray,
        checkpoint: () -> Unit,
    ): IntArray? {
        require(width > 0 && height > 0 && width.toLong() * height == pixels.size.toLong())
        require(mask.size == pixels.size)
        checkpoint()
        if (mask.none { it }) return pixels.copyOf()
        if (mask.all { it }) return null
        var minX = width
        var maxX = -1
        var minY = height
        var maxY = -1
        for (index in mask.indices) {
            if (!mask[index]) continue
            val x = index % width
            val y = index / width
            minX = min(minX, x)
            maxX = max(maxX, x)
            minY = min(minY, y)
            maxY = max(maxY, y)
        }
        val span = max(maxX - minX + 1, maxY - minY + 1)
        val levels = ArrayList<Level>()
        levels += Level(width, height, toFloats(pixels), mask, BooleanArray(mask.size) { mask[it] || pixels[it] ushr 24 < 16 })
        while ((span shr (levels.size - 1)) > COARSE_SPAN && min(levels.last().width, levels.last().height) / 2 >= COARSE_SIDE) {
            levels += shrink(levels.last())
        }
        var field: IntArray? = null
        var finer: Level? = null
        for (depth in levels.indices.reversed()) {
            checkpoint()
            val level = levels[depth]
            val w = level.width
            val h = level.height
            val valid = validCentres(level.blocked, w, h)
            val validList = valid.indices.filter { valid[it] }.toIntArray()
            // ⚠️ Un buco che occupa gran parte del ritaglio non lascia toppe intere fuori di sé su
            // un livello piccolo: la partenza sale di una scala (caso del banco: un quadrato di
            // 200 px in un'immagine di 400).
            if (validList.isEmpty()) {
                if (field == null && depth > 0) continue
                return null
            }
            val image = level.image.copyOf()
            val target = BooleanArray(w * h)
            for (index in level.hole.indices) {
                if (!level.hole[index]) continue
                val x = index % w
                val y = index / w
                for (ty in max(0, y - RADIUS)..min(h - 1, y + RADIUS)) {
                    for (tx in max(0, x - RADIUS)..min(w - 1, x + RADIUS)) target[ty * w + tx] = true
                }
            }
            val targets = target.indices.filter { target[it] }.toIntArray()
            // Un seme fisso per livello: la stessa selezione dà sempre gli stessi pixel.
            val random = Random(depth * 7919 + w)
            val next = IntArray(w * h) { -1 }
            val distances = FloatArray(w * h)
            val previous = field
            val coarse = finer
            if (previous == null || coarse == null) {
                val seeded = seed(toInts(image, pixels.size == w * h, pixels), w, h, level.hole, checkpoint)
                if (seeded == null) {
                    if (depth > 0) continue
                    return null
                }
                for (index in level.hole.indices) if (level.hole[index]) setPixel(image, index, seeded[index])
                for (p in targets) next[p] = validList[random.nextInt(validList.size)]
            } else {
                for (p in targets) {
                    val x = p % w
                    val y = p / w
                    val q = previous[min(coarse.height - 1, y / 2) * coarse.width + min(coarse.width - 1, x / 2)]
                    var candidate = -1
                    if (q >= 0) {
                        val cx = q % coarse.width * 2 + (x and 1)
                        val cy = q / coarse.width * 2 + (y and 1)
                        if (cx < w && cy < h && valid[cy * w + cx]) candidate = cy * w + cx
                    }
                    next[p] = if (candidate >= 0) candidate else validList[random.nextInt(validList.size)]
                }
                // Ricostruisce il buco dal campo ingrandito coi pixel di questo livello.
                vote(image, level.hole, w, h, targets, next, distances)
            }
            val rounds =
                when {
                    previous == null -> COARSE_EM
                    targets.size > SEARCH_BUDGET -> 0
                    else -> FINE_EM
                }
            for (round in 0 until rounds) {
                checkpoint()
                for (p in targets) distances[p] = distance(image, w, h, p, next[p], Float.MAX_VALUE)
                search(image, w, h, targets, next, distances, valid, random, checkpoint)
                vote(image, level.hole, w, h, targets, next, distances)
            }
            field = next
            finer = Level(w, h, image, level.hole, level.blocked)
        }
        val image = finer!!.image
        val result = pixels.copyOf()
        for (index in mask.indices) {
            if (mask[index]) result[index] = (pixels[index] and 0xff000000.toInt()) or (toInt(image, index) and 0x00ffffff)
        }
        return result
    }

    private fun toFloats(pixels: IntArray): FloatArray {
        val image = FloatArray(pixels.size * 3)
        for (index in pixels.indices) setPixel(image, index, pixels[index])
        return image
    }

    private fun setPixel(
        image: FloatArray,
        index: Int,
        argb: Int,
    ) {
        image[3 * index] = (argb ushr 16 and 255).toFloat()
        image[3 * index + 1] = (argb ushr 8 and 255).toFloat()
        image[3 * index + 2] = (argb and 255).toFloat()
    }

    private fun toInt(
        image: FloatArray,
        index: Int,
    ): Int =
        (0xff shl 24) or
            ((image[3 * index] + 0.5f).toInt().coerceIn(0, 255) shl 16) or
            ((image[3 * index + 1] + 0.5f).toInt().coerceIn(0, 255) shl 8) or
            (image[3 * index + 2] + 0.5f).toInt().coerceIn(0, 255)

    /** A piena misura il seme tiene l'alfa originale, che la sua prova dei donatori legge. */
    private fun toInts(
        image: FloatArray,
        full: Boolean,
        pixels: IntArray,
    ): IntArray =
        IntArray(image.size / 3) {
            val rgb = toInt(image, it)
            if (full) (pixels[it] and 0xff000000.toInt()) or (rgb and 0x00ffffff) else rgb
        }

    /** Dimezza un livello; un pixel grossolano è buco, o bloccato, appena lo è uno dei suoi quattro. */
    private fun shrink(level: Level): Level {
        val w = max(1, level.width / 2)
        val h = max(1, level.height / 2)
        val image = FloatArray(w * h * 3)
        val hole = BooleanArray(w * h)
        val blocked = BooleanArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var known = 0
                var r = 0f
                var g = 0f
                var b = 0f
                val out = y * w + x
                for (dy in 0..1) {
                    for (dx in 0..1) {
                        val index = (2 * y + dy) * level.width + 2 * x + dx
                        if (level.blocked[index]) blocked[out] = true
                        if (level.hole[index]) {
                            hole[out] = true
                            continue
                        }
                        r += level.image[3 * index]
                        g += level.image[3 * index + 1]
                        b += level.image[3 * index + 2]
                        known++
                    }
                }
                if (known > 0) {
                    image[3 * out] = r / known
                    image[3 * out + 1] = g / known
                    image[3 * out + 2] = b / known
                }
            }
        }
        return Level(w, h, image, hole, blocked)
    }

    /** I centri la cui toppa intera è dentro il ritaglio e fuori dai pixel bloccati. */
    private fun validCentres(
        blocked: BooleanArray,
        w: Int,
        h: Int,
    ): BooleanArray {
        val pitch = w + 1
        val integral = IntArray(pitch * (h + 1))
        for (y in 0 until h) {
            var row = 0
            for (x in 0 until w) {
                if (blocked[y * w + x]) row++
                integral[(y + 1) * pitch + x + 1] = integral[y * pitch + x + 1] + row
            }
        }
        val valid = BooleanArray(w * h)
        for (y in RADIUS until h - RADIUS) {
            for (x in RADIUS until w - RADIUS) {
                val left = x - RADIUS
                val top = y - RADIUS
                val right = x + RADIUS + 1
                val bottom = y + RADIUS + 1
                val count =
                    integral[bottom * pitch + right] - integral[top * pitch + right] -
                        integral[bottom * pitch + left] + integral[top * pitch + left]
                if (count == 0) valid[y * w + x] = true
            }
        }
        return valid
    }

    /** La somma dei quadrati delle differenze, interrotta appena supera [limit]. */
    private fun distance(
        image: FloatArray,
        w: Int,
        h: Int,
        p: Int,
        q: Int,
        limit: Float,
    ): Float {
        val px = p % w
        val py = p / w
        val qx = q % w
        val qy = q / w
        var sum = 0f
        for (dy in -RADIUS..RADIUS) {
            val ty = py + dy
            if (ty !in 0 until h) continue
            for (dx in -RADIUS..RADIUS) {
                val tx = px + dx
                if (tx !in 0 until w) continue
                val a = 3 * (ty * w + tx)
                val b = 3 * ((qy + dy) * w + qx + dx)
                val dr = image[a] - image[b]
                val dg = image[a + 1] - image[b + 1]
                val db = image[a + 2] - image[b + 2]
                sum += dr * dr + dg * dg + db * db
            }
            if (sum > limit) return sum
        }
        return sum
    }

    /** PatchMatch: propagazione dai vicini già visitati, poi ricerca casuale a raggi dimezzati. */
    private fun search(
        image: FloatArray,
        w: Int,
        h: Int,
        targets: IntArray,
        field: IntArray,
        distances: FloatArray,
        valid: BooleanArray,
        random: Random,
        checkpoint: () -> Unit,
    ) {
        for (round in 0 until SEARCH_ROUNDS) {
            checkpoint()
            val forward = round % 2 == 0
            val step = if (forward) -1 else 1
            for (k in targets.indices) {
                if (k % 4096 == 0) checkpoint()
                val p = targets[if (forward) k else targets.size - 1 - k]
                val x = p % w
                val y = p / w

                fun consider(candidate: Int) {
                    if (candidate == field[p] || !valid[candidate]) return
                    val d = distance(image, w, h, p, candidate, distances[p])
                    if (d < distances[p]) {
                        distances[p] = d
                        field[p] = candidate
                    }
                }
                for (vertical in 0..1) {
                    val nx = if (vertical == 0) x + step else x
                    val ny = if (vertical == 1) y + step else y
                    if (nx !in 0 until w || ny !in 0 until h) continue
                    val q = field[ny * w + nx]
                    if (q < 0) continue
                    val cx = q % w - (nx - x)
                    val cy = q / w - (ny - y)
                    if (cx in 0 until w && cy in 0 until h) consider(cy * w + cx)
                }
                var radius = max(w, h)
                while (radius >= 1) {
                    val cx = field[p] % w + random.nextInt(-radius, radius + 1)
                    val cy = field[p] / w + random.nextInt(-radius, radius + 1)
                    if (cx in 0 until w && cy in 0 until h) consider(cy * w + cx)
                    radius /= 2
                }
            }
        }
    }

    /**
     * Ogni pixel del buco diventa la media pesata dei pixel donatori proposti dalle toppe che lo
     * coprono. Le toppe che somigliano di più ai dintorni contano di più; la scala è il terzo
     * quartile delle distanze, quindi i pesi non dipendono dal contrasto dell'immagine.
     */
    private fun vote(
        image: FloatArray,
        hole: BooleanArray,
        w: Int,
        h: Int,
        targets: IntArray,
        field: IntArray,
        distances: FloatArray,
    ) {
        val sorted = FloatArray(targets.size) { distances[targets[it]] }.apply { sort() }
        val scale = 2f * max(1f, sorted[(sorted.size - 1) * 3 / 4])
        val sum = FloatArray(w * h * 3)
        val weights = FloatArray(w * h)
        for (p in targets) {
            val weight = exp(-distances[p] / scale)
            val px = p % w
            val py = p / w
            val q = field[p]
            val qx = q % w
            val qy = q / w
            for (dy in -RADIUS..RADIUS) {
                val ty = py + dy
                if (ty !in 0 until h) continue
                for (dx in -RADIUS..RADIUS) {
                    val tx = px + dx
                    if (tx !in 0 until w) continue
                    val t = ty * w + tx
                    if (!hole[t]) continue
                    val s = 3 * ((qy + dy) * w + qx + dx)
                    sum[3 * t] += image[s] * weight
                    sum[3 * t + 1] += image[s + 1] * weight
                    sum[3 * t + 2] += image[s + 2] * weight
                    weights[t] += weight
                }
            }
        }
        for (t in hole.indices) {
            if (!hole[t] || weights[t] <= 0f) continue
            for (c in 0..2) image[3 * t + c] = sum[3 * t + c] / weights[t]
        }
    }


    /**
     * Il riempimento avido del livello più grossolano, dal bordo verso l'interno: le toppe
     * donatrici si copiano intere, non si mediano, perché il centro di un buco grande non diventi
     * una media piatta.
     */
    private fun seed(
        pixels: IntArray,
        width: Int,
        height: Int,
        mask: BooleanArray,
        checkpoint: () -> Unit,
    ): IntArray? {
        checkpoint()
        val holes = mask.indices.filter { mask[it] }.toIntArray()
        if (holes.isEmpty()) return pixels.copyOf()
        if (holes.size == pixels.size) return null
        val radius = min(SEED_RADIUS, (min(width, height) - 3) / 2)
        if (radius < 1) return null
        val missing = mask.copyOf()
        val filled = pixels.copyOf()
        val confidence = FloatArray(pixels.size) { if (mask[it]) 0f else 1f }
        val donors = donorCentres(pixels, width, height, mask, radius + 1)
        if (donors.isEmpty()) return null
        val donorSet = BooleanArray(pixels.size)
        for (candidate in donors) donorSet[candidate] = true
        val gridStep = max(1, sqrt(donors.size / 640.0).toInt())
        val coarseDonors =
            donors
                .filter { it % width % gridStep == 0 && it / width % gridStep == 0 }
                .ifEmpty { donors.toList() }
        val laplace = Array(3) { FloatArray(pixels.size) }
        var remaining = holes.size

        fun value(
            index: Int,
            channel: Int,
            from: IntArray = filled,
        ): Int = (from[index] ushr (16 - channel * 8)) and 255

        fun grey(index: Int): Float =
            (
                value(index, 0) * 0.2126f + value(index, 1) * 0.7152f +
                    value(index, 2) * 0.0722f
            ) / 255f

        fun unknown(
            x: Int,
            y: Int,
        ): Float = if (x !in 0 until width || y !in 0 until height || !missing[y * width + x]) 0f else 1f

        while (remaining > 0) {
            checkpoint()
            var target = -1
            var bestPriority = -1f
            for (index in holes) {
                if (!missing[index]) continue
                val x = index % width
                val y = index / width
                if ((x == 0 || missing[index - 1]) && (x == width - 1 || missing[index + 1]) &&
                    (y == 0 || missing[index - width]) && (y == height - 1 || missing[index + width])
                ) {
                    continue
                }
                var sum = 0f
                var samples = 0
                var gradient = 0f
                var gx = 0f
                var gy = 0f
                for (py in max(0, y - radius)..min(height - 1, y + radius)) {
                    for (px in max(0, x - radius)..min(width - 1, x + radius)) {
                        val p = py * width + px
                        sum += confidence[p]
                        samples++
                        if (missing[p] || px == 0 || px == width - 1 || py == 0 || py == height - 1 ||
                            missing[p - 1] || missing[p + 1] || missing[p - width] || missing[p + width]
                        ) {
                            continue
                        }
                        val dx = (grey(p + 1) - grey(p - 1)) * 0.5f
                        val dy = (grey(p + width) - grey(p - width)) * 0.5f
                        val norm = dx * dx + dy * dy
                        if (norm > gradient) {
                            gradient = norm
                            gx = dx
                            gy = dy
                        }
                    }
                }
                val nx = unknown(x + 1, y) - unknown(x - 1, y)
                val ny = unknown(x, y + 1) - unknown(x, y - 1)
                val norm = sqrt(nx * nx + ny * ny).coerceAtLeast(1f)
                // ⚠️ Il peso dell'isofota è alto perché continua meglio orli e linee spezzate.
                val priority = (sum / samples) * (0.012f + abs(-gy * nx + gx * ny) / norm * 1.25f)
                if (priority > bestPriority) {
                    bestPriority = priority
                    target = index
                }
            }
            if (target < 0) return null
            val tx = target % width
            val ty = target / width
            // I campioni noti pesano per la loro fiducia, così le prime ricostruzioni non
            // prevalgono sui dati intatti dall'altra parte di una linea sottile interrotta.
            val offsets = ArrayList<Int>()
            val weights = ArrayList<Float>()
            var confidenceSum = 0f
            var patchSize = 0
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    val x = tx + dx
                    val y = ty + dy
                    if (x !in 0 until width || y !in 0 until height) continue
                    val index = y * width + x
                    patchSize++
                    confidenceSum += confidence[index]
                    if (!missing[index]) {
                        offsets.add(dy * width + dx)
                        weights.add(max(0.05f, confidence[index]))
                    }
                }
            }
            if (offsets.isEmpty()) return null
            var donor = -1
            var error = Double.POSITIVE_INFINITY

            fun greyOf(argb: Int): Float =
                ((argb ushr 16 and 255) * 0.2126f + (argb ushr 8 and 255) * 0.7152f +
                    (argb and 255) * 0.0722f) / 255f

            fun consider(candidate: Int) {
                var cost = 0.0
                for (k in offsets.indices) {
                    val a = filled[target + offsets[k]]
                    val b = pixels[candidate + offsets[k]]
                    val dr = (a ushr 16 and 255) - (b ushr 16 and 255)
                    val dg = (a ushr 8 and 255) - (b ushr 8 and 255)
                    val db = (a and 255) - (b and 255)
                    cost += (dr * dr + dg * dg + db * db) * weights[k]
                    if (cost > error) return
                }
                /*
                 * ⚠️⚠️ **Il termine di struttura**: oltre al centro, confronta il gradiente su
                 * fino a quattro campioni noti della toppa. Senza, un orlo si riempirebbe con
                 * pezzi di tessuto che spezzano la piega. ⚠️ Niente bilanciamento di luminanza
                 * sulla somma dei quadrati: su un orlo scuro in campo chiaro schiarisce il bordo
                 * (banco `hem-like`).
                 */
                fun localGx(base: Int, from: IntArray): Float =
                    (greyOf(from[base + 1]) - greyOf(from[base - 1])) * 0.5f
                fun localGy(base: Int, from: IntArray): Float =
                    (greyOf(from[base + width]) - greyOf(from[base - width])) * 0.5f
                fun structureAt(tBase: Int, cBase: Int): Double {
                    val cx = cBase % width
                    val cy = cBase / width
                    if (tBase % width !in 1 until width - 1 || tBase / width !in 1 until height - 1) {
                        return 0.0
                    }
                    if (cx !in 1 until width - 1 || cy !in 1 until height - 1) return 0.0
                    if (missing[tBase - 1] || missing[tBase + 1] ||
                        missing[tBase - width] || missing[tBase + width]
                    ) {
                        return 0.0
                    }
                    val dgx = localGx(tBase, filled) - localGx(cBase, pixels)
                    val dgy = localGy(tBase, filled) - localGy(cBase, pixels)
                    return (dgx * dgx + dgy * dgy) * 3200.0
                }
                cost += structureAt(target, candidate)
                if (tx > radius && !missing[target - 1]) {
                    cost += structureAt(target - 1, candidate - 1) * 0.35
                }
                if (tx + radius < width - 1 && !missing[target + 1]) {
                    cost += structureAt(target + 1, candidate + 1) * 0.35
                }
                if (ty > radius && !missing[target - width]) {
                    cost += structureAt(target - width, candidate - width) * 0.35
                }
                if (ty + radius < height - 1 && !missing[target + width]) {
                    cost += structureAt(target + width, candidate + width) * 0.35
                }
                // La distanza decide solo fra corrispondenze altrimenti uguali: trama e struttura
                // devono prevalere sulla preferenza per un donatore vicino.
                cost += 0.00001 * (abs(candidate % width - tx) + abs(candidate / width - ty))
                if (cost < error) {
                    error = cost
                    donor = candidate
                }
            }
            for (n in coarseDonors.indices) {
                consider(coarseDonors[n])
                if (n % 128 == 0) checkpoint()
            }
            val coarse = donor
            if (coarse < 0) return null
            for (y in max(radius + 1, coarse / width - gridStep)..min(height - radius - 2, coarse / width + gridStep)) {
                for (x in max(radius + 1, coarse % width - gridStep)..min(width - radius - 2, coarse % width + gridStep)) {
                    val candidate = y * width + x
                    if (donorSet[candidate]) consider(candidate)
                }
            }
            val confidenceValue = confidenceSum / patchSize
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    val x = tx + dx
                    val y = ty + dy
                    if (x !in 0 until width || y !in 0 until height) continue
                    val destination = y * width + x
                    if (!missing[destination]) continue
                    val source = donor + dy * width + dx
                    filled[destination] = (pixels[destination] and 0xff000000.toInt()) or
                        (pixels[source] and 0x00ffffff)
                    for (channel in 0..2) {
                        laplace[channel][destination] =
                            4f * value(source, channel, pixels) - value(source - 1, channel, pixels) -
                            value(source + 1, channel, pixels) - value(source - width, channel, pixels) -
                            value(source + width, channel, pixels)
                    }
                    confidence[destination] = confidenceValue
                    missing[destination] = false
                    remaining--
                }
            }
        }
        // Un piccolo termine di schermatura tiene la trama del donatore mentre il bordo esterno,
        // fisso, ne accorda la luce. Nessun pixel fuori dalla selezione originale è scritto.
        // ⚠️ Una schermatura bassa lascia più trama del donatore, e molte iterazioni chiudono
        // meglio i gradienti sui bordi.
        val screen = 0.20f
        for (channel in 0..2) {
            val levels = FloatArray(pixels.size) { value(it, channel).toFloat() }
            for (iteration in 0 until 140) {
                checkpoint()
                var largest = 0f
                for (index in holes) {
                    val x = index % width
                    val y = index / width
                    var sum = 0f
                    var count = 0
                    if (x > 0) {
                        sum += levels[index - 1]
                        count++
                    }
                    if (x + 1 < width) {
                        sum += levels[index + 1]
                        count++
                    }
                    if (y > 0) {
                        sum += levels[index - width]
                        count++
                    }
                    if (y + 1 < height) {
                        sum += levels[index + width]
                        count++
                    }
                    val guidance = if (count == 4) laplace[channel][index] else 0f
                    val next = ((sum + guidance + screen * value(index, channel)) / (count + screen)).coerceIn(0f, 255f)
                    largest = max(largest, abs(next - levels[index]))
                    levels[index] = next
                }
                if (largest < 0.018f && iteration > 14) break
            }
            for (index in holes) {
                val shift = 16 - channel * 8
                filled[index] = (filled[index] and (255 shl shift).inv()) or
                    ((levels[index] + 0.5f).toInt().coerceIn(0, 255) shl shift)
            }
        }
        return filled
    }

    private fun donorCentres(
        pixels: IntArray,
        width: Int,
        height: Int,
        mask: BooleanArray,
        radius: Int,
    ): IntArray {
        val pitch = width + 1
        val integral = IntArray(pitch * (height + 1))
        for (y in 0 until height) {
            var row = 0
            for (x in 0 until width) {
                val i = y * width + x
                if (mask[i] || pixels[i] ushr 24 < 16) row++
                integral[(y + 1) * pitch + x + 1] = integral[y * pitch + x + 1] + row
            }
        }
        val donors = ArrayList<Int>()
        for (y in radius until height - radius) {
            for (x in radius until width - radius) {
                val left = x - radius
                val top = y - radius
                val right = x + radius + 1
                val bottom = y + radius + 1
                val count =
                    integral[bottom * pitch + right] - integral[top * pitch + right] -
                        integral[bottom * pitch + left] + integral[top * pitch + left]
                if (count == 0) donors.add(y * width + x)
            }
        }
        return donors.toIntArray()
    }
}

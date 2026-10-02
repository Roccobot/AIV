package io.github.roccobot.aiv

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Deterministic exemplar reconstruction followed by screened Poisson blending.
 * The fill front favours the continuation of visible edges (Criminisi-style priority).
 * Donors must be completely outside the original selection: generated pixels are never
 * mistaken for new evidence. All reads are from the supplied local image crop.
 *
 * Dalla `3.29`/`3.37` (`3.24-04`/`3.30-01`): raggio adattivo, peso isofota e struttura
 * sul donatore ancora alzati, per preservare meglio orli, linee e pattern.
 * Dalla `3.40` (`3.38-11`): ultimo giro euristico senza AI: raggio un po' più generoso,
 * termine di struttura sul patch noto (non solo al centro) e blending di Poisson un filo
 * più lungo sui bordi (senza bilanciare la luminanza nel SSD: lava gli orli scuri).
 */
internal object Inpaint {
    fun repair(
        pixels: IntArray,
        width: Int,
        height: Int,
        mask: BooleanArray,
        checkpoint: () -> Unit = {},
    ): IntArray? {
        require(width > 0 && height > 0 && width.toLong() * height == pixels.size.toLong())
        require(mask.size == pixels.size)
        checkpoint()
        val holes = mask.indices.filter { mask[it] }.toIntArray()
        if (holes.isEmpty()) return pixels.copyOf()
        if (holes.size == pixels.size) return null
        /*
         * ⚠️⚠️ **Raggio adattivo dalla `3.29`** (giro 3.24, `3.24-04`): un tassello fisso da 4
         * pixel ricostruiva male trame e linee. Con buchi piu grandi si pedina un pezzo piu
         * ampio di contesto, entro un tetto che resta compatibile col tempo di calcolo.
         */
        val holeSpan =
            run {
                var minX = width
                var maxX = -1
                var minY = height
                var maxY = -1
                for (index in holes) {
                    val x = index % width
                    val y = index / width
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
                max(maxX - minX + 1, maxY - minY + 1)
            }
        val wanted = when {
            holeSpan >= 64 -> 9
            holeSpan >= 40 -> 8
            holeSpan >= 20 -> 7
            holeSpan >= 10 -> 6
            else -> 5
        }
        val radius = min(wanted, (min(width, height) - 3) / 2)
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
                // ⚠️ Peso isofota alzato (`3.29`): continua meglio orli e linee spezzate.
                val priority = (sum / samples) * (0.012f + abs(-gy * nx + gx * ny) / norm * 1.25f)
                if (priority > bestPriority) {
                    bestPriority = priority
                    target = index
                }
            }
            if (target < 0) return null
            val tx = target % width
            val ty = target / width
            // Known samples carry their confidence, so early reconstructions do not
            // outweigh intact evidence at the opposite side of a narrow interrupted line.
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
                 * ⚠️⚠️ **Termine di struttura dalla `3.29`, rinforzato in `3.40`**: oltre al
                 * centro, confronta il gradiente su fino a quattro campioni noti del patch.
                 * Senza, un orlo poteva essere riempito con pezzi di tessuto che spezzavano
                 * la piega. ⚠️ Il bilanciamento di luminanza sul SSD e stato ritirato: su un
                 * orlo scuro su campo chiaro lavava il bordo (banco `hem-like`).
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
                // Only break otherwise equal matches by distance; texture/structure
                // must dominate the preference for a nearby source patch.
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
        // A small screening term keeps donor texture while the fixed outside boundary
        // reconciles its lighting. No pixel outside the original selection is written.
        // ⚠️ `3.40`: screening un filo più basso lascia piu trama del donatore; iterazioni
        // extra chiudono meglio i gradienti sui bordi senza AI.
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

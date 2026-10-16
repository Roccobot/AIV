package io.github.roccobot.aiv

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.SequenceInputStream

/*
 * Come un'immagine tiene i suoi colori, e quanti fotogrammi ha.
 *
 * Si leggono le intestazioni e non si decodifica niente, come in [Encoding]: 'colore
 * indicizzato, 256 colori' costa pochi byte, dai pixel costerebbe l'immagine intera. Anche i
 * fotogrammi si contano saltando di blocco in blocco i dati compressi.
 * Cinque formati: PNG, GIF, JPEG e WebP, la cui intestazione si legge in poche righe, e AVIF, che
 * ha già un suo lettore ([Avif]). ⚠️ **HEIC no**, anche se usa lo stesso contenitore: il metodo
 * colore è in scatole diverse (`hvcC` e non `av1C`), non c'è un file su cui provarlo, e una
 * risposta inventata è peggio di nessuna.
 */

/**
 * Il metodo colore di un'immagine.
 *
 * [bitsPerChannel] è per canale: il totale si ricava moltiplicando per i canali del modello, e
 * dal totale non si potrebbe scrivere la riga '8 bit/canale' chiesta dall'utente.
 */
class Colours(
    val model: Model,
    val bitsPerChannel: Int?,
    /** Quanti colori ha la tavolozza. Solo su [Model.INDEXED]. */
    val palette: Int?,
    val transparent: Boolean
) {
    enum class Model(val channels: Int) {
        GREY(1), GREY_ALPHA(2), RGB(3), RGBA(4), INDEXED(1)
    }

    /**
     * I bit di un pixel, e `null` quando non si sanno.
     *
     * Su una tavolozza non si scrive: un pixel è un indice, e il numero che conta è quanti
     * colori ci sono.
     */
    val bitsPerPixel: Int?
        get() = if (model == Model.INDEXED) null else bitsPerChannel?.times(model.channels)
}

/** Quanti fotogrammi ha un'immagine animata, e quanto dura in tutto. */
class Motion(val frames: Int, val durationMs: Int)

/**
 * Il metodo colore, dai primi byte del file.
 *
 * Il flusso si legge una volta sola e in avanti: chi chiama ne apre uno nuovo per ogni lettore,
 * come per [encodingOf].
 */
fun coloursOf(input: InputStream): Colours? = runCatching {
    val head = ByteArray(HEAD)
    val got = input.readFully(head, HEAD)
    when {
        got >= 8 && head.startsWith(PNG_SIGNATURE) -> pngColours(head, got, input)
        got >= 13 && head.startsWith(GIF_SIGNATURE) -> gifColours(head)
        // Il SOF si cerca dall'inizio: la testa è già letta, quindi si rimette davanti al seguito.
        got >= 4 && head[0] == 0xFF.toByte() && head[1] == 0xD8.toByte() ->
            jpegColours(SequenceInputStream(ByteArrayInputStream(head, 0, got), input))
        got >= 16 && head.startsWith(RIFF_SIGNATURE) &&
            String(head, 8, 4, Charsets.US_ASCII) == "WEBP" -> webpColours(head, got, input)
        got >= 12 && Avif.looksLike(head) -> Avif.colours(avifHead(head, got, input))
        else -> null
    }
}.getOrNull()

/**
 * I byte in cui cercare le scatole di un AVIF: [HEAD] più il seguito, fino a [Avif.HEAD].
 *
 * Il flusso è già avanti di [HEAD] byte e non si riavvolge: si legge il seguito e si incolla.
 */
private fun avifHead(head: ByteArray, valid: Int, input: InputStream): ByteArray {
    if (valid < head.size) return head.copyOf(valid)
    val more = ByteArray(Avif.HEAD - head.size)
    val got = input.readFully(more, more.size)
    return head + more.copyOf(got)
}

/**
 * Quanti fotogrammi e quanto dura, dai byte del file.
 *
 * Risponde `null` su un'immagine ferma, non un fotogramma: la finestra mostra la riga solo
 * quando c'è un'animazione.
 */
fun motionOf(input: InputStream): Motion? = runCatching {
    val head = ByteArray(HEAD)
    val got = input.readFully(head, HEAD)
    val found = when {
        got >= 13 && head.startsWith(GIF_SIGNATURE) -> gifMotion(head, got, input)
        got >= 16 && head.startsWith(RIFF_SIGNATURE) &&
            String(head, 8, 4, Charsets.US_ASCII) == "WEBP" -> webpMotion(head, got, input)
        else -> null
    }
    found?.takeIf { it.frames > 1 }
}.getOrNull()

// ── PNG ────────────────────────────────────────────────────────────────────

/**
 * Il tipo di colore è un byte e i suoi valori non sono progressivi: 0 grigio, 2 RGB, 3
 * tavolozza, 4 grigio con alfa, 6 RGBA. Cinque casi scritti per esteso si leggono meglio delle
 * maschere sui bit.
 * ⚠️ **La trasparenza non è solo l'alfa del modello**: un `tRNS` dichiara un colore trasparente
 * anche su RGB e su grigio, e su tavolozza un'opacità per voce. Per trovarlo si camminano i
 * chunk oltre l'intestazione.
 */
private fun pngColours(head: ByteArray, valid: Int, input: InputStream): Colours? {
    if (valid < PNG_IHDR_END) return null
    val bits = (head[24].toInt() and 0xFF).takeIf { it > 0 } ?: return null
    val model = when (head[25].toInt() and 0xFF) {
        0 -> Colours.Model.GREY
        2 -> Colours.Model.RGB
        3 -> Colours.Model.INDEXED
        4 -> Colours.Model.GREY_ALPHA
        6 -> Colours.Model.RGBA
        else -> return null
    }
    var palette: Int? = null
    var trns = false
    val rest = Rest(head, valid, PNG_IHDR_END, input)
    var seen = 0
    while (seen < MAX_CHUNKS) {
        val length = rest.beInt() ?: break
        val name = rest.text(4) ?: break
        if (length < 0) break
        when (name) {
            "PLTE" -> palette = length / 3
            "tRNS" -> trns = true
        }
        // Al primo IDAT si smette: dopo ci sono solo i pixel compressi, e per specifica i due
        // chunk che interessano vengono prima.
        if (name == "IDAT" || name == "IEND") break
        if (rest.skip(length + 4) == null) break
        seen++
    }
    val alpha = trns || model == Colours.Model.RGBA || model == Colours.Model.GREY_ALPHA
    return Colours(model, bits, palette, alpha)
}

// ── GIF ────────────────────────────────────────────────────────────────────

/**
 * Una GIF è sempre a tavolozza, e la sua misura è nei tre bit bassi del byte impacchettato del
 * descrittore di schermo: `2^(n+1)` voci.
 * ⚠️ **La trasparenza qui non si vede**: è nell'estensione di controllo grafica di ogni
 * fotogramma, più avanti. La legge [gifTransparencyOf].
 */
private fun gifColours(head: ByteArray): Colours? {
    val packed = head[10].toInt() and 0xFF
    val hasTable = (packed and 0x80) != 0
    val palette = if (hasTable) 1 shl ((packed and 0x07) + 1) else null
    return Colours(Colours.Model.INDEXED, GIF_BITS, palette, false)
}

/**
 * Cammina i blocchi di una GIF contando i fotogrammi e sommando i ritardi.
 *
 * I dati compressi si saltano: sono in sotto-blocchi preceduti dalla loro lunghezza su un byte,
 * e la catena finisce con uno zero. Costa un byte letto ogni 255.
 * ⚠️ **Il ritardo è in centesimi di secondo**, non in millesimi: da qui il per dieci.
 */
private fun gifMotion(head: ByteArray, valid: Int, input: InputStream): Motion? {
    val packed = head[10].toInt() and 0xFF
    var at = 13
    if ((packed and 0x80) != 0) {
        val table = 3 * (1 shl ((packed and 0x07) + 1))
        at += table
    }
    // Quello che resta di `head` si consuma prima del flusso: la tavolozza globale di una GIF a
    // 256 colori è di 768 byte, e può finire oltre il buffer letto.
    val rest = Rest(head, valid, at, input)
    var frames = 0
    var delay = 0
    while (true) {
        when (val block = rest.byte() ?: break) {
            0x2C -> {
                // Descrittore di immagine: 9 byte, poi l'eventuale tavolozza locale.
                val local = rest.skip(8)?.let { rest.byte() } ?: break
                if ((local and 0x80) != 0) rest.skip(3 * (1 shl ((local and 0x07) + 1)))
                rest.skip(1) // la misura minima del codice LZW
                if (!rest.subBlocks()) break
                frames++
            }
            0x21 -> {
                val label = rest.byte() ?: break
                if (label == 0xF9) {
                    val size = rest.byte() ?: break
                    if (size >= 4) {
                        rest.byte() // i flag: la trasparenza la legge [gifTransparent]
                        val low = rest.byte() ?: break
                        val high = rest.byte() ?: break
                        delay += ((high shl 8) or low) * 10
                        rest.skip(size - 4 + 1)
                    } else {
                        rest.skip(size)
                        rest.subBlocks()
                    }
                    rest.byte() // il terminatore del blocco
                } else {
                    if (!rest.subBlocks()) break
                }
            }
            0x3B -> break
            else -> break
        }
        if (frames > MAX_FRAMES) break
    }
    return if (frames > 0) Motion(frames, delay) else null
}

/**
 * Se una GIF dichiara un colore trasparente, in uno qualsiasi dei suoi fotogrammi.
 *
 * Basta un fotogramma: la riga delle informazioni dice se l'immagine ha trasparenza, non quanti
 * fotogrammi ce l'hanno.
 */
private fun gifTransparent(head: ByteArray, valid: Int, input: InputStream): Boolean {
    val packed = head[10].toInt() and 0xFF
    var at = 13
    if ((packed and 0x80) != 0) at += 3 * (1 shl ((packed and 0x07) + 1))
    val rest = Rest(head, valid, at, input)
    var seen = 0
    while (seen < MAX_FRAMES) {
        when (rest.byte() ?: return false) {
            0x21 -> {
                val label = rest.byte() ?: return false
                if (label == 0xF9) {
                    val size = rest.byte() ?: return false
                    if (size >= 1) {
                        val flags = rest.byte() ?: return false
                        if ((flags and 0x01) != 0) return true
                        rest.skip(size - 1)
                    }
                    rest.subBlocks()
                } else if (!rest.subBlocks()) return false
            }
            0x2C -> {
                val local = rest.skip(8)?.let { rest.byte() } ?: return false
                if ((local and 0x80) != 0) rest.skip(3 * (1 shl ((local and 0x07) + 1)))
                rest.skip(1)
                if (!rest.subBlocks()) return false
                seen++
            }
            else -> return false
        }
    }
    return false
}

// ── JPEG ───────────────────────────────────────────────────────────────────

/**
 * Il JPEG non ha trasparenza né tavolozza: resta da sapere se ha un componente (grigio) o tre
 * (colore), e con quanti bit. Lo dice il SOF, che si cerca di segmento in segmento come in
 * [encodingOf].
 * ⚠️ **Quattro componenti (CMYK o YCCK, i file pronti per la stampa) non hanno una risposta**:
 * [Colours.Model] non ha una voce per il CMYK, e dirli RGB a 24 bit sarebbe una risposta
 * inventata, peggio di nessuna, come per l'HEIC. Lo stesso per un numero di componenti che il
 * formato non prevede.
 */
private fun jpegColours(input: InputStream): Colours? {
    val two = ByteArray(2)
    while (true) {
        if (input.readFully(two, 2) < 2) return null
        if ((two[0].toInt() and 0xFF) != 0xFF) return null
        val marker = two[1].toInt() and 0xFF
        if (marker == 0xD8 || marker == 0x01 || marker in 0xD0..0xD7) continue
        if (marker == 0xD9 || marker == 0xDA) return null
        if (input.readFully(two, 2) < 2) return null
        val length = (((two[0].toInt() and 0xFF) shl 8) or (two[1].toInt() and 0xFF)) - 2
        if (length < 0) return null
        val isSof = marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC
        if (!isSof) {
            if (input.skipFully(length.toLong()) < length.toLong()) return null
            continue
        }
        val body = ByteArray(length)
        if (input.readFully(body, length) < length) return null
        val bits = if (body.isNotEmpty()) (body[0].toInt() and 0xFF).takeIf { it > 0 } else null
        val components = if (body.size > 5) body[5].toInt() and 0xFF else 0
        val model = when (components) {
            1 -> Colours.Model.GREY
            3 -> Colours.Model.RGB
            else -> return null
        }
        return Colours(model, bits, null, false)
    }
}

// ── WebP ───────────────────────────────────────────────────────────────────

/**
 * ⚠️ **L'alfa di una WebP è in tre posti, secondo com'è fatta**: nel bit `0x10` dei flag di
 * `VP8X` sulle estese, nel bit `0x10` del byte 4 di `VP8L` sulle senza perdita, e in un chunk
 * `ALPH` su quelle con perdita.
 * I bit per canale sono sempre 8, e non si leggono.
 */
private fun webpColours(head: ByteArray, valid: Int, input: InputStream): Colours? {
    var alpha = false
    var found = false
    walkRiff(head, valid, input) { name, body, _ ->
        when (name) {
            "VP8X" -> {
                if (body.isNotEmpty() && (body[0].toInt() and 0x10) != 0) alpha = true
                found = true
            }
            "VP8L" -> {
                if (body.size > 4 && (body[4].toInt() and 0x10) != 0) alpha = true
                found = true
            }
            "ALPH" -> { alpha = true; found = true }
            "VP8 " -> found = true
        }
        true
    }
    if (!found) return null
    return Colours(
        if (alpha) Colours.Model.RGBA else Colours.Model.RGB, WEBP_BITS, null, alpha
    )
}

/**
 * Il ritardo di una WebP animata è in millesimi, a differenza della GIF: è nei tre byte little
 * endian dall'offset 12 di ogni `ANMF`.
 */
private fun webpMotion(head: ByteArray, valid: Int, input: InputStream): Motion? {
    var frames = 0
    var delay = 0
    walkRiff(head, valid, input) { name, body, _ ->
        if (name == "ANMF" && body.size >= 16) {
            frames++
            delay += (body[12].toInt() and 0xFF) or
                ((body[13].toInt() and 0xFF) shl 8) or
                ((body[14].toInt() and 0xFF) shl 16)
        }
        frames <= MAX_FRAMES
    }
    return if (frames > 0) Motion(frames, delay) else null
}

/**
 * Cammina i chunk di un contenitore RIFF, dando a [onChunk] nome e corpo.
 *
 * Di un chunk si leggono al massimo [CHUNK_PEEK] byte e il resto si salta: dentro un `ANMF` c'è
 * il fotogramma compresso, che qui non serve.
 * ⚠️ **I chunk sono allineati a due byte**: una lunghezza dispari ha un byte di riempimento
 * fuori dal corpo, e senza contarlo la lettura si disallinea.
 */
private fun walkRiff(
    head: ByteArray,
    valid: Int,
    input: InputStream,
    onChunk: (String, ByteArray, Int) -> Boolean
) {
    val rest = Rest(head, valid, 12, input)
    while (true) {
        val name = rest.text(4) ?: return
        val size = rest.leInt() ?: return
        if (size < 0) return
        val peek = minOf(size, CHUNK_PEEK)
        val body = rest.bytes(peek) ?: return
        if (!onChunk(name, body, size)) return
        val padded = size + (size and 1)
        if (rest.skip(padded - peek) == null) return
    }
}

// ── Lettura ────────────────────────────────────────────────────────────────

/**
 * Quello che resta da leggere: prima la coda del buffer già in mano, poi il flusso.
 *
 * Esiste perché il primo blocco è già letto: senza, ogni lettore dovrebbe tenere il conto di
 * dove finisce il buffer e comincia il flusso, e sbagliando leggerebbe byte sbagliati senza
 * accorgersene.
 */
private class Rest(
    private val head: ByteArray,
    /**
     * Quanti byte di [head] sono veri.
     *
     * ⚠️ **Non è `head.size`**: il buffer è sempre di [HEAD] byte, ma un file più corto ne
     * riempie solo una parte e il resto è a zero. Senza questo limite un lettore prenderebbe quegli
     * zeri per dati.
     */
    private val valid: Int,
    private var at: Int,
    private val input: InputStream
) {

    fun byte(): Int? {
        if (at < valid) return head[at++].toInt() and 0xFF
        val one = input.read()
        return if (one < 0) null else one
    }

    fun bytes(count: Int): ByteArray? {
        if (count <= 0) return ByteArray(0)
        val out = ByteArray(count)
        var done = 0
        while (done < count && at < valid) {
            out[done++] = head[at++]
        }
        while (done < count) {
            val got = input.read(out, done, count - done)
            if (got <= 0) return null
            done += got
        }
        return out
    }

    fun text(count: Int): String? = bytes(count)?.let { String(it, Charsets.US_ASCII) }

    fun beInt(): Int? {
        val four = bytes(4) ?: return null
        return ((four[0].toInt() and 0xFF) shl 24) or ((four[1].toInt() and 0xFF) shl 16) or
            ((four[2].toInt() and 0xFF) shl 8) or (four[3].toInt() and 0xFF)
    }

    fun leInt(): Int? {
        val four = bytes(4) ?: return null
        return (four[0].toInt() and 0xFF) or ((four[1].toInt() and 0xFF) shl 8) or
            ((four[2].toInt() and 0xFF) shl 16) or ((four[3].toInt() and 0xFF) shl 24)
    }

    /** Salta [count] byte, e `null` se il flusso finisce prima. */
    fun skip(count: Int): Unit? {
        if (count <= 0) return Unit
        var left = count
        while (left > 0 && at < valid) {
            at++
            left--
        }
        if (left == 0) return Unit
        return if (input.skipFully(left.toLong()) >= left.toLong()) Unit else null
    }

    /** Salta una catena di sotto-blocchi GIF, fino al terminatore. */
    fun subBlocks(): Boolean {
        while (true) {
            val size = byte() ?: return false
            if (size == 0) return true
            if (skip(size) == null) return false
        }
    }
}

private fun ByteArray.startsWith(other: ByteArray): Boolean {
    if (size < other.size) return false
    for (i in other.indices) if (this[i] != other[i]) return false
    return true
}

/**
 * Quanti byte si leggono in un colpo solo.
 *
 * 1 KB copre l'intestazione di PNG, GIF, JPEG e WebP e la tavolozza globale di una GIF a 256
 * colori (768 byte). Per AVIF [avifHead] legge il seguito.
 */
private const val HEAD = 1024

/**
 * Dove finisce l'IHDR di un PNG: 8 di firma, 4 di lunghezza, 4 di nome, 13 di corpo, 4 di
 * controllo. Da qui cominciano i chunk che questa lettura cerca.
 */
private const val PNG_IHDR_END = 33

/**
 * Quanti chunk PNG si guardano prima di rinunciare.
 *
 * Come [MAX_FRAMES]: un file malformato può descrivere una catena che non finisce.
 */
private const val MAX_CHUNKS = 512

/** Quanto si guarda dentro un chunk RIFF prima di saltarlo: basta per `ANMF` e per i flag. */
private const val CHUNK_PEEK = 16

/** I bit per canale di una GIF: la tavolozza è sempre RGB a otto bit. */
private const val GIF_BITS = 8

/** I bit per canale di una WebP: il formato non ne prevede altri. */
private const val WEBP_BITS = 8

/**
 * Oltre questo numero di fotogrammi si smette di contare.
 *
 * ⚠️ Un file malformato può descrivere una catena di blocchi che non finisce, e senza limite la
 * lettura girerebbe fino in fondo al flusso.
 */
private const val MAX_FRAMES = 10_000

private val PNG_SIGNATURE = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
)
private val GIF_SIGNATURE = "GIF8".toByteArray(Charsets.US_ASCII)
private val RIFF_SIGNATURE = "RIFF".toByteArray(Charsets.US_ASCII)

/**
 * La trasparenza di una GIF, che [gifColours] non può vedere.
 *
 * È una funzione a parte: la trasparenza è nei blocchi dei fotogrammi, oltre l'intestazione, e
 * chi vuole solo il metodo colore non deve camminare tutto il file.
 */
fun gifTransparencyOf(input: InputStream): Boolean = runCatching {
    val head = ByteArray(HEAD)
    val got = input.readFully(head, HEAD)
    if (got >= 13 && head.startsWith(GIF_SIGNATURE)) gifTransparent(head, got, input) else false
}.getOrDefault(false)

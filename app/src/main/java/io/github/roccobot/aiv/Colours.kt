package io.github.roccobot.aiv

import java.io.InputStream

/**
 * Come un'immagine tiene i suoi colori, e quanti fotogrammi ha.
 *
 * ⚠️⚠️ **SI LEGGONO LE INTESTAZIONI E NON SI DECODIFICA NIENTE, come in [Encoding]**: dire
 * 'colore indicizzato, 256 colori' costa una manciata di byte, mentre ricavarlo dai pixel
 * vorrebbe dire ricostruire l'immagine intera per scrivere una riga. Vale anche per il
 * conteggio dei fotogrammi: si cammina di blocco in blocco saltando i dati compressi, senza
 * mai srotolarne uno.
 * ⚠️ **Cinque formati e il resto tace**: PNG, GIF, JPEG e WebP sono quelli la cui
 * intestazione si legge in poche righe e senza ambiguità, e dalla `1.26` c'è anche
 * **AVIF**, che il lettore suo adesso ce l'ha (vedi [Avif]) perché serviva comunque alla
 * decodifica. ⚠️ **HEIC no**, benché stia nello stesso contenitore: là il metodo colore va
 * cercato in scatole diverse (`hvcC` invece di `av1C`) e nessuno ha un file su cui
 * provarlo, e una risposta inventata è peggio di nessuna risposta.
 */

/**
 * Il metodo colore di un'immagine.
 *
 * ⚠️ **[bitsPerChannel] è per canale e non in tutto**: il totale si ricava moltiplicando per
 * i canali del modello, e tenere il totale invece del fattore renderebbe impossibile scrivere
 * la seconda riga che l'utente ha chiesto ('8 bit/canale').
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
     * ⚠️ **Su una tavolozza non si scrive**: là un pixel è un **indice**, quindi otto bit per
     * pixel non dicono niente sul colore, e il numero che conta è quanti colori ci sono.
     */
    val bitsPerPixel: Int?
        get() = if (model == Model.INDEXED) null else bitsPerChannel?.times(model.channels)
}

/** Quanti fotogrammi ha un'immagine animata, e quanto dura in tutto. */
class Motion(val frames: Int, val durationMs: Int)

/**
 * Il metodo colore, dai primi byte del file.
 *
 * ⚠️ Il flusso si legge una volta sola e in avanti: chi chiama ne apre uno nuovo per ogni
 * lettore, come già fa per [encodingOf].
 */
fun coloursOf(input: InputStream): Colours? = runCatching {
    val head = ByteArray(HEAD)
    val got = input.readFully(head, HEAD)
    when {
        got >= 8 && head.startsWith(PNG_SIGNATURE) -> pngColours(head, got, input)
        got >= 13 && head.startsWith(GIF_SIGNATURE) -> gifColours(head)
        got >= 4 && head[0] == 0xFF.toByte() && head[1] == 0xD8.toByte() -> jpegColours(input)
        got >= 16 && head.startsWith(RIFF_SIGNATURE) &&
            String(head, 8, 4, Charsets.US_ASCII) == "WEBP" -> webpColours(head, got, input)
        got >= 12 && Avif.looksLike(head) -> Avif.colours(avifHead(head, got, input))
        else -> null
    }
}.getOrNull()

/**
 * I byte in cui cercare le scatole di un AVIF: [HEAD] più il seguito, fino a [Avif.HEAD].
 *
 * ⚠️ **Il flusso è già avanti di [HEAD] byte**, quindi qui si legge il **seguito** e si
 * incolla: riavvolgere non si può, ed è la stessa ragione per cui ogni lettore di questo
 * file riceve un flusso suo.
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
 * ⚠️ **Torna `null` su un'immagine ferma**, e non un uno: 'un fotogramma' è la stessa
 * informazione di 'non è animata' detta in un modo che invita a cercare un comando che non
 * c'è. Il dialogo mostra la riga solo quando esiste un'animazione.
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
 * ⚠️⚠️ **IL TIPO DI COLORE È UN BYTE SOLO, e i suoi valori non sono progressivi**: 0 grigio,
 * 2 RGB, 3 tavolozza, 4 grigio con alfa, 6 RGBA. Il 5 e l'1 non esistono, e i bit del numero
 * hanno un senso (bit 0 tavolozza, bit 1 colore, bit 2 alfa) che qui non si sfrutta perché
 * cinque casi scritti per esteso si leggono meglio di tre maschere.
 * ⚠️ **La trasparenza NON è solo l'alfa del modello**: un `tRNS` dichiara un colore
 * trasparente anche su RGB e su grigio, e su tavolozza dà un'opacità per ogni voce. Per
 * trovarlo bisogna camminare i chunk, ed è l'unica ragione per cui questa funzione legge
 * oltre l'intestazione.
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
        // ⚠️ Al primo IDAT si smette: da lì in poi ci sono solo i pixel compressi, e i due
        // chunk che interessano stanno per specifica **prima** di quello.
        if (name == "IDAT" || name == "IEND") break
        if (rest.skip(length + 4) == null) break
        seen++
    }
    val alpha = trns || model == Colours.Model.RGBA || model == Colours.Model.GREY_ALPHA
    return Colours(model, bits, palette, alpha)
}

// ── GIF ────────────────────────────────────────────────────────────────────

/**
 * ⚠️ **Una GIF è SEMPRE a tavolozza**, e la sua misura sta nei tre bit bassi del byte
 * impacchettato del descrittore di schermo: la tavolozza ha `2^(n+1)` voci.
 * ⚠️ **La trasparenza qui NON si vede**: sta nell'estensione di controllo grafica di ogni
 * fotogramma, che arriva dopo. La riempie [gifMotion], che quei blocchi li cammina comunque.
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
 * ⚠️⚠️ **I DATI COMPRESSI SI SALTANO SENZA LEGGERLI, ed è quello che rende questa funzione
 * economica**: dopo il descrittore di immagine i dati stanno in **sotto-blocchi**, ognuno
 * preceduto dalla sua lunghezza su un byte, e la catena finisce con uno zero. Saltare di
 * lunghezza in lunghezza costa un byte letto ogni 255.
 * ⚠️ **Il ritardo è in CENTESIMI di secondo**, non in millesimi, ed è l'errore classico su
 * questo formato: moltiplicare per dieci non è una conversione elegante, è la conversione.
 */
private fun gifMotion(head: ByteArray, valid: Int, input: InputStream): Motion? {
    val packed = head[10].toInt() and 0xFF
    var at = 13
    if ((packed and 0x80) != 0) {
        val table = 3 * (1 shl ((packed and 0x07) + 1))
        at += table
    }
    // ⚠️ Quello che resta di `head` si consuma prima del flusso: la tavolozza globale di una
    // GIF a 256 colori sono 768 byte, quindi puo' finire oltre il buffer letto.
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
                        rest.byte() // i flag: la trasparenza la legge gifTransparent
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
 * ⚠️ **Basta UN fotogramma**: la riga delle informazioni dice se l'immagine ha trasparenza,
 * non quanti fotogrammi ce l'hanno.
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
 * ⚠️ **Il JPEG non ha trasparenza e non ha tavolozza**: quello che resta da sapere è se è a
 * un componente (grigio) o a tre (colore), e con quanti bit. Li dice il segmento SOF, e per
 * trovarlo si salta di segmento in segmento come fa [encodingOf].
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
        val model = if (components == 1) Colours.Model.GREY else Colours.Model.RGB
        return Colours(model, bits, null, false)
    }
}

// ── WebP ───────────────────────────────────────────────────────────────────

/**
 * ⚠️⚠️ **L'ALFA DI UNA WebP STA IN TRE POSTI DIVERSI, a seconda di com'è fatta**: nel bit
 * `0x10` dei flag di `VP8X` sulle estese, nel bit `0x10` del byte 4 di `VP8L` sulle senza
 * perdita, e in un chunk `ALPH` a parte su quelle con perdita. Guardarne uno solo darebbe
 * 'senza trasparenza' su due file su tre che ce l'hanno.
 * ⚠️ **I bit per canale sono 8 e non si leggono**: il formato non ne prevede altri, quindi
 * l'unico numero possibile è quello, e cercarlo nei byte sarebbe cercare una cosa che c'è
 * per definizione.
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
 * ⚠️ **Il ritardo di una WebP animata è in MILLESIMI**, al contrario della GIF: sta nei tre
 * byte in little endian a partire dall'offset 12 di ogni `ANMF`.
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
 * ⚠️⚠️ **DI UN `ANMF` SI LEGGONO SOLO I PRIMI 16 BYTE, e il resto si salta**: dentro c'è il
 * fotogramma compresso, che qui non serve, e leggerlo vorrebbe dire tenere in memoria
 * l'animazione intera per contare quanti pezzi ha.
 * ⚠️ **I chunk sono allineati a due byte**: una lunghezza dispari porta un byte di riempimento
 * che non fa parte del corpo, e chi lo dimentica si disallinea al primo chunk dispari e poi
 * legge spazzatura.
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
 * ⚠️⚠️ **ESISTE PERCHÉ IL PRIMO BLOCCO È GIÀ STATO LETTO, e senza di lui ogni lettore
 * dovrebbe tenere il conto di dove finisce il buffer e comincia il flusso**: è la parte in
 * cui si sbaglia, e sbagliandola si legge spazzatura senza accorgersene, perché i byte
 * arrivano lo stesso.
 */
private class Rest(
    private val head: ByteArray,
    /**
     * Quanti byte di [head] sono veri.
     *
     * ⚠️⚠️ **NON È `head.size`, ED È IL DIFETTO CHE QUESTO PARAMETRO TOGLIE**: il buffer si
     * alloca sempre di [HEAD] byte, ma un file più corto ne riempie solo una parte e il resto
     * resta a **zero**. Senza questo limite ogni lettore proseguirebbe dentro quegli zeri
     * credendoli dati, e su un file piccolo tornerebbe risposte inventate invece di fermarsi.
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
 * ⚠️ **1 KB copre l'intestazione di tutti e quattro i formati e la tavolozza globale di una
 * GIF a 256 colori** (768 byte), che è il pezzo più lungo che serve leggere per intero.
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
 * ⚠️ Come [MAX_FRAMES]: un file malformato può descrivere una catena che non finisce.
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
 * ⚠️ **Non è un tetto di prudenza, è una difesa**: un file malformato può descrivere una
 * catena di blocchi che non finisce mai, e senza un limite questa lettura girerebbe finché
 * il flusso non si esaurisce, che su un file grande vuol dire secondi.
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
 * ⚠️ **È una funzione a parte e non un parametro**: la trasparenza sta nei blocchi dei
 * fotogrammi, cioè oltre l'intestazione, e leggerla insieme al metodo colore vorrebbe dire
 * far camminare tutto il file anche a chi vuole solo sapere quanti colori ha.
 */
fun gifTransparencyOf(input: InputStream): Boolean = runCatching {
    val head = ByteArray(HEAD)
    val got = input.readFully(head, HEAD)
    if (got >= 13 && head.startsWith(GIF_SIGNATURE)) gifTransparent(head, got, input) else false
}.getOrDefault(false)

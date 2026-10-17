package io.github.roccobot.aiv

import java.io.InputStream

/**
 * Come è codificata un'immagine, letto dai **byte del file** e non dai metadati.
 *
 * ⚠️ **Non basta l'EXIF**: i tag TIFF della compressione (`Compression`, `BitsPerSample`) in
 * un JPEG di telefono quasi sempre mancano, o descrivono la miniatura. Le informazioni vere sono
 * nell'intestazione del formato, dove le legge il decodificatore stesso.
 *
 * Solo JPEG e PNG, che hanno un'intestazione leggibile in poche righe e senza ambiguità. WebP e
 * HEIF sono dentro contenitori a scatole (RIFF e ISO-BMFF), che chiederebbero un parser di
 * contenitore: quando non si sa, la riga non si scrive, ed è la regola di tutta la schermata.
 *
 * I campi sono numeri e nomi propri ('JPEG', '8', '4:2:0'), uguali in ogni lingua; l'unica
 * parola ([interlaced]) la scrive l'interfaccia con la sua stringa.
 */
class Encoding(
    /** `JPEG` o `PNG`: nome proprio del formato, non si traduce. */
    val codec: String,
    /** Bit per campione, o `null` se l'intestazione non lo dice. */
    val bits: Int?,
    /**
     * Il sottocampionamento del croma, nella notazione `4:2:0`.
     *
     * Solo JPEG a tre componenti: in scala di grigi non vuol dire niente, e il PNG non
     * sottocampiona.
     */
    val chroma: String?,
    /** JPEG progressivo, o PNG interlacciato: due nomi per la stessa idea. */
    val interlaced: Boolean
)

/**
 * Legge l'intestazione e dice come è codificata l'immagine, o `null` se il formato non è
 * fra i due che si sanno leggere.
 *
 * Il formato si riconosce dai byte: il tipo MIME e l'estensione dicono che cosa il file
 * dovrebbe essere, e un `.jpg` che contiene un PNG esiste.
 * ⚠️ **Lo stream si consuma e non si riavvolge**: chi chiama ne apre uno nuovo per gli usi
 * successivi.
 */
fun encodingOf(input: InputStream): Encoding? {
    val head = ByteArray(2)
    if (input.readFully(head, 2) != 2) return null
    val first = head[0].toInt() and 0xFF
    val second = head[1].toInt() and 0xFF
    return when {
        first == 0xFF && second == 0xD8 -> jpegEncoding(input)
        first == 0x89 && second == 0x50 -> pngEncoding(input)
        else -> null
    }
}

/**
 * L'intestazione di un JPEG: si salta di segmento in segmento fino al SOF.
 *
 * ⚠️ **Il SOF può essere molto lontano dall'inizio**: un JPEG di telefono ha in testa un
 * `APP1` con l'EXIF e la miniatura, fino a 64 KB. Una finestra fissa lo troverebbe su alcune
 * foto e non su altre.
 * I SOF sono più di uno: `C0` sequenziale di base, `C1` sequenziale estesa, `C2` progressiva,
 * `C9` e `CA` aritmetiche. Col solo `C0` ogni foto progressiva risulterebbe ignota.
 * ⚠️ **`C4`, `C8` e `CC` non sono SOF**: `C4` sono le tabelle di Huffman, e scambiarle per un
 * SOF darebbe misure e componenti inventate.
 */
private fun jpegEncoding(input: InputStream): Encoding? {
    val two = ByteArray(2)
    while (true) {
        // I marcatori sono allineati e possono essere preceduti da byte di riempimento
        // `FF`: si scorre finché si trova un byte che non è `FF`.
        var marker = input.read()
        if (marker < 0) return null
        if (marker != 0xFF) continue
        while (marker == 0xFF) marker = input.read()
        if (marker < 0) return null

        // I marcatori senza corpo: fine dei dati e riavvii.
        if (marker == 0xD9 || marker == 0xDA) return Encoding("JPEG", null, null, false)
        if (marker in 0xD0..0xD8 || marker == 0x01) continue

        if (input.readFully(two, 2) != 2) return null
        val length = ((two[0].toInt() and 0xFF) shl 8 or (two[1].toInt() and 0xFF)) - 2
        if (length < 0) return null

        val isSof = marker in setOf(0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF)
        if (!isSof) {
            if (input.skipFully(length.toLong()) != length.toLong()) return null
            continue
        }

        val body = ByteArray(length)
        if (input.readFully(body, length) != length) return null
        return sofEncoding(marker, body)
    }
}

/**
 * Il corpo di un SOF: precisione, componenti e sottocampionamento.
 *
 * Struttura: 1 byte di precisione, 2 di altezza, 2 di larghezza, 1 col numero di
 * componenti, poi 3 byte per componente (identificativo, fattori di campionamento in due
 * mezzi byte, tabella di quantizzazione).
 *
 * ⚠️ **Il sottocampionamento si ricava dal rapporto** fra i fattori del canale di luce e
 * quelli del croma: `2x2` sulla luce con `1x1` sul croma è `4:2:0`, ma `1x1` su tutti e tre è
 * `4:4:4`.
 * Si dichiara solo per tre componenti: in scala di grigi il croma non esiste, e per il CMYK
 * la notazione a quattro cifre è un'altra cosa.
 */
private fun sofEncoding(marker: Int, body: ByteArray): Encoding {
    val progressive = marker == 0xC2 || marker == 0xC6 || marker == 0xCA || marker == 0xCE
    val bits = if (body.isNotEmpty()) (body[0].toInt() and 0xFF).takeIf { it > 0 } else null
    val components = if (body.size > 5) body[5].toInt() and 0xFF else 0
    var chroma: String? = null
    if (components == 3 && body.size >= 6 + 9) {
        val factors = (0 until 3).map { at ->
            val f = body[6 + at * 3 + 1].toInt() and 0xFF
            (f shr 4) to (f and 0x0F)
        }
        val (lumaH, lumaV) = factors[0]
        val chromaWide = factors.drop(1).all { it.first > 0 && lumaH / it.first >= 2 }
        val chromaTall = factors.drop(1).all { it.second > 0 && lumaV / it.second >= 2 }
        chroma = when {
            chromaWide && chromaTall -> "4:2:0"
            chromaWide -> "4:2:2"
            chromaTall -> "4:4:0"
            else -> "4:4:4"
        }
    }
    return Encoding("JPEG", bits, chroma, progressive)
}

/**
 * L'intestazione di un PNG, che è sempre nello stesso posto: la firma di 8 byte, poi il
 * blocco `IHDR` con larghezza, altezza, profondità, tipo di colore, compressione, filtro e
 * interlacciamento.
 *
 * La profondità è per canale, non per pixel (`8` su un RGBA vuol dire 32 bit per pixel): è la
 * convenzione degli altri visualizzatori.
 * Il sottocampionamento resta vuoto: `4:4:4` sarebbe vero, ma suggerirebbe una scelta di
 * codifica che il PNG non ha.
 */
private fun pngEncoding(input: InputStream): Encoding? {
    // I due byte della firma già letti sono `89 50`: ne restano sei, poi la lunghezza e il
    // nome del blocco, che si saltano perché l'IHDR è sempre il primo e sempre di 13 byte.
    if (input.skipFully(6L) != 6L) return null
    val header = ByteArray(8 + 13)
    if (input.readFully(header, header.size) != header.size) return null
    if (String(header, 4, 4, Charsets.US_ASCII) != "IHDR") return null
    val bits = (header[8 + 8].toInt() and 0xFF).takeIf { it > 0 }
    val interlaced = (header[8 + 12].toInt() and 0xFF) != 0
    return Encoding("PNG", bits, null, interlaced)
}

/**
 * Legge esattamente [count] byte, insistendo finché lo stream ne dà.
 *
 * ⚠️ **`read` può tornare meno byte di quelli chiesti senza che lo stream sia finito**: su uno
 * stream di rete o di un `ContentResolver` capita davvero, e un parser che si fida della prima
 * lettura sbaglierebbe l'intestazione una volta ogni tanto.
 * Questa e [skipFully] sono `internal` perché le usano anche [coloursOf], le miniature, il
 * visualizzatore, le serie web e le animazioni: una seconda copia rischierebbe di perdere il
 * ciclo, ed è successo tre volte.
 */
internal fun InputStream.readFully(into: ByteArray, count: Int): Int {
    var got = 0
    while (got < count) {
        val step = read(into, got, count - got)
        if (step <= 0) return got
        got += step
    }
    return got
}

/** Come sopra per il salto: anche `skip` ha licenza di saltarne meno di quanti chiesti. */
internal fun InputStream.skipFully(count: Long): Long {
    var done = 0L
    while (done < count) {
        val step = skip(count - done)
        if (step <= 0L) {
            // `skip` può rispondere 0 senza essere alla fine: si insiste leggendo.
            if (read() < 0) return done
            done++
        } else {
            done += step
        }
    }
    return done
}

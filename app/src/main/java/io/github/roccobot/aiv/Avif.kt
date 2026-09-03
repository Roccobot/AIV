package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import org.aomedia.avif.android.AvifDecoder
import java.nio.ByteBuffer

/**
 * La decodifica AVIF, e perché non la fa il telefono.
 *
 * ⚠️⚠️ **`ImageDecoder` DICHIARA AVIF DA API 31 MA SI APPOGGIA AL DECODIFICATORE AV1 DEL
 * TELEFONO, e quello non regge i file veri.** Misurato sul file dell'utente (24.793.385
 * byte, esportato da Lightroom) leggendone l'intestazione, non dedotto: marchio `MA1A`
 * cioè **profilo AVIF avanzato**, `seq_profile` **1** (AV1 High, croma **4:4:4**),
 * `seq_level_idx` **16** cioè **livello 6.0**, e **6016x4016**, cioè 24,2 megapixel.
 * Ognuna delle tre è fuori da quello che un decodificatore AV1 di telefono garantisce: la
 * CDD di Android obbliga al solo **profilo Main** (0, 4:2:0), e i decodificatori hardware
 * si fermano quasi sempre al livello 5.1, cioè al 4K. Da lì l'errore che ha visto
 * l'utente, `getPixels failed with error invalid input`, che non è un file rotto: è un
 * decodificatore che non sa leggere quel profilo.
 *
 * ⚠️ **La pista dell'utente era giusta, e la verifica è sul codice di Fossify Gallery**:
 * non si fida del decodificatore di sistema e se ne porta uno dentro (`libs.avif` e
 * `libs.avif.integration` nel suo catalogo). Qui si prende la stessa strada dalla fonte:
 * la libreria ufficiale di AOMedia, cioè **libavif con dav1d**, che decodifica in software
 * e quindi non dipende da che cosa sa fare il telefono.
 *
 * **Che cosa costa, misurato sull'AAR e non stimato**: la libreria nativa pesa 868.592
 * byte per `arm64-v8a` e 655.780 per `armeabi-v7a`, e nell'APK sta **non compressa**
 * perché è nativa. Le due varianti `x86` (1,1 e 1,8 MB) sono escluse nel
 * `build.gradle.kts`: servirebbero ai soli emulatori, e qui l'APK si scarica da un sito,
 * dove nessuno spacchetta per architettura. ⚠️ **Per questo [ready] esiste**: su un
 * dispositivo x86 la libreria non c'è, `System.loadLibrary` va in errore, e senza quel
 * controllo la classe resterebbe rotta per sempre invece di sfilarsi.
 *
 * ⚠️⚠️ **LA SCALA LA FA LIBAVIF, e questo è il fatto che rende la cosa possibile**: se il
 * bitmap che si passa è più piccolo dell'immagine, la decodifica ci scala dentro
 * (`avifImageScale`). Quindi una fotografia da 24 megapixel non costa mai 96 MB di heap
 * Java: si chiede la misura che il budget consente e i piani interi restano nella memoria
 * **nativa**, che non è quella contata da `Runtime.maxMemory`.
 */
object Avif {

    /**
     * Se la libreria nativa c'è.
     *
     * ⚠️ Si tocca una funzione nativa qualunque per **forzare** l'inizializzazione statica,
     * che è dove sta `System.loadLibrary`: senza, il primo errore arriverebbe più tardi e
     * come `NoClassDefFoundError`, che è quello che una classe rotta dà al secondo tentativo
     * e che nessun `catch` sul punto d'uso si aspetta.
     */
    val ready: Boolean = try {
        AvifDecoder.versionString()
        true
    } catch (t: Throwable) {
        false
    }

    /** Quanti byte bastano a riconoscere il formato: `ftyp` sta sempre in testa. */
    const val SNIFF = 32

    /**
     * Fin dove si legge un AVIF per trovarne le proprietà, quando servono solo quelle.
     *
     * ⚠️⚠️ **MILLE BYTE NON BASTANO SEMPRE**: in un AVIF `ispe` e `av1C` stanno dentro
     * `meta`, ma accanto a loro ci può stare un **profilo ICC** o un XMP di parecchi
     * kilobyte, e l'ordine dentro `ipco` non lo fissa nessuno. Sul file di prova
     * dell'utente `meta` misura 13.474 byte. Centoventotto kilobyte restano una frazione
     * di un file da decine di megabyte, e la scansione si ferma da sé a `mdat`.
     */
    const val HEAD = 128 * 1024

    /**
     * Con quanti fili decodificare.
     *
     * ⚠️⚠️ **SI DICHIARA, perché la scorciatoia a tre argomenti passa ZERO e quello vuol
     * dire UN filo solo**: `AvifDecoder.decode(buffer, len, bitmap)` chiama la versione
     * nativa con `threads = 0`, e libavif lo porta a uno. Su questa immagine è la
     * differenza fra usare un core e usarli tutti, e il costo è già alto di suo: la stessa
     * libreria, misurata qui su un Xeon a 4 core, ci mette **1,6 secondi** a decodificare
     * i 24 megapixel del file dell'utente.
     * ⚠️ Il tetto a 8 non è prudenza generica: oltre, dav1d spartisce righe sempre più
     * corte fra sempre più fili, e su un telefono i core in più sono quelli piccoli.
     */
    private val THREADS: Int = Runtime.getRuntime().availableProcessors().coerceIn(1, 8)

    /**
     * Riconosce un AVIF dai primi byte, senza chiamare la libreria nativa.
     *
     * ⚠️ **Serve prima e non dopo**: il ripiego deve sapere se vale la pena leggere in
     * memoria un file che può pesare decine di megabyte, e questa risposta costa
     * trentadue byte. La forma è quella di ISO-BMFF: quattro byte di lunghezza, `ftyp`,
     * poi il marchio principale e l'elenco di quelli compatibili, che si guardano tutti
     * perché un file può dichiarare `mif1` come principale e `avif` fra i compatibili.
     */
    fun looksLike(head: ByteArray): Boolean {
        if (head.size < 12) return false
        if (head[4] != 'f'.code.toByte() || head[5] != 't'.code.toByte() ||
            head[6] != 'y'.code.toByte() || head[7] != 'p'.code.toByte()
        ) return false
        var at = 8
        while (at + 4 <= head.size) {
            val brand = String(head, at, 4, Charsets.ISO_8859_1)
            if (brand == "avif" || brand == "avis") return true
            at += 4
        }
        return false
    }

    /**
     * Decodifica [bytes] in un bitmap grande al massimo [cap] pixel, girato secondo `irot`.
     *
     * Torna `null` quando la libreria non c'è, il file non è leggibile, o la decodifica
     * fallisce: chi chiama resta col proprio errore invece di riceverne uno nuovo.
     */
    fun decode(bytes: ByteArray, cap: Long): Decoded? {
        // ⚠️ **Il buffer diretto si costruisce UNA volta e si passa in giro**: è una copia
        // dell'intero file in memoria nativa, e su una fotografia da 25 MB farne due (una
        // per l'intestazione e una per i pixel) vorrebbe dire cinquanta megabyte per niente.
        val buffer = direct(bytes)
        val info = header(buffer) ?: return null
        val step = stepFor(info.width, info.height, cap)
        return run(
            buffer = buffer,
            bytes = bytes,
            info = info,
            target = Size(
                (info.width / step).coerceAtLeast(1),
                (info.height / step).coerceAtLeast(1)
            )
        )
    }

    /**
     * Decodifica [bytes] direttamente nella misura di una miniatura: il lato lungo diventa
     * [box] e l'altro segue le proporzioni.
     *
     * ⚠️ **Qui NON si va a potenze di due**, al contrario di [decode], e la ragione è la
     * qualità: da 6016 pixel il passo più vicino a 512 sarebbe 16, cioè 376, e un riquadro
     * da 512 lo mostrerebbe morbido. Libavif scala a qualunque misura, quindi chiedere
     * quella giusta non costa niente in più. Il `sampled` che a [decode] serve, qui non
     * serve: una miniatura è campionata per definizione.
     */
    fun thumbnail(bytes: ByteArray, box: Int): Bitmap? {
        val buffer = direct(bytes)
        val info = header(buffer) ?: return null
        val long = maxOf(info.width, info.height)
        val target = if (long <= box) {
            Size(info.width, info.height)
        } else {
            Size(
                (info.width.toLong() * box / long).toInt().coerceAtLeast(1),
                (info.height.toLong() * box / long).toInt().coerceAtLeast(1)
            )
        }
        return run(buffer, bytes, info, target)?.bitmap
    }

    /** L'intestazione, o `null` se questo non è un AVIF leggibile. */
    private fun header(buffer: ByteBuffer): AvifDecoder.Info? {
        if (!ready) return null
        val info = AvifDecoder.Info()
        buffer.rewind()
        if (!AvifDecoder.getInfo(buffer, buffer.remaining(), info)) return null
        if (info.width <= 0 || info.height <= 0) return null
        return info
    }

    private fun run(
        buffer: ByteBuffer,
        bytes: ByteArray,
        info: AvifDecoder.Info,
        target: Size
    ): Decoded? {
        val bitmap = try {
            Bitmap.createBitmap(target.width, target.height, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            return null
        }
        // ⚠️ Si riparte da capo: `getInfo` ha già camminato questo stesso buffer, e un
        // `ByteBuffer` porta con sé la propria posizione.
        buffer.rewind()
        val done = try {
            AvifDecoder.decode(buffer, buffer.remaining(), bitmap, THREADS)
        } catch (t: Throwable) {
            false
        }
        if (!done) {
            bitmap.recycle()
            return null
        }
        // ⚠️ **Anche le misure dichiarate si girano**, non solo i pixel: sono quelle che
        // finiscono nella barra delle info, e una fotografia mostrata in verticale con
        // scritto sotto '6016 x 4016' si contraddice da sola.
        val quarters = quarters(bytes)
        val dritta = quarters % 2 == 0
        return Decoded(
            bitmap = turn(bitmap, quarters),
            fullWidth = if (dritta) info.width else info.height,
            fullHeight = if (dritta) info.height else info.width,
            sampled = target.width < info.width || target.height < info.height
        )
    }

    /** Quello che la decodifica sa dire, oltre ai pixel. */
    data class Decoded(
        val bitmap: Bitmap,
        val fullWidth: Int,
        val fullHeight: Int,
        val sampled: Boolean
    )

    /**
     * Di quanto rimpicciolire per stare sotto [cap] pixel.
     *
     * ⚠️ **Potenze di due come fa `ImageDecoder`, benché libavif accetti qualunque
     * misura**, e la ragione è la coerenza di quello che si racconta: `sampled` nella
     * barra delle info vuol dire 'questa non è la fotografia intera', e due strade che
     * scelgono misure diverse direbbero la stessa cosa a due condizioni diverse.
     */
    private fun stepFor(width: Int, height: Int, cap: Long): Int {
        var step = 1
        var pixels = width.toLong() * height.toLong()
        while (pixels > cap && step < 16) {
            step *= 2
            pixels = (width.toLong() / step) * (height.toLong() / step)
        }
        return step
    }

    /**
     * Il buffer che la libreria nativa pretende.
     *
     * ⚠️⚠️ **DEVE essere DIRETTO**, e non è un dettaglio di prestazione: il codice JNI
     * chiama `GetDirectBufferAddress` e con un buffer normale **fallisce e basta**,
     * scrivendo *encoded is not a direct ByteBuffer* in un log che nessuno legge. Un
     * `ByteBuffer.wrap` qui darebbe una decodifica che non riesce mai, senza dire perché.
     */
    private fun direct(bytes: ByteArray): ByteBuffer =
        ByteBuffer.allocateDirect(bytes.size).apply {
            put(bytes)
            rewind()
        }

    /**
     * I quarti di giro dichiarati dal file, letti dalla scatola `irot`.
     *
     * ⚠️⚠️ **SERVONO PERCHÉ LIBAVIF NON LI APPLICA**: il suo strato JNI decodifica i pixel
     * come stanno e dichiara `ignoreExif`, quindi una fotografia scattata di traverso
     * uscirebbe di traverso. ⚠️ **E per un AVIF l'autorità è `irot`, non l'EXIF**: la
     * specifica dice che le trasformazioni del contenitore vincono, quindi leggere
     * l'orientamento EXIF sarebbe la risposta sbagliata anche quando c'è.
     * ⚠️ **`imir` (lo specchio) NON si applica, ed è una scelta**: la sua convenzione
     * sull'asse è scritta in due modi diversi fra le edizioni della specifica, quindi
     * indovinare vorrebbe dire mostrare l'immagine ribaltata con la stessa sicurezza con
     * cui oggi la si mostra dritta. Se un file vero lo porterà, si aggiunge allora, con
     * quel file davanti.
     */
    private fun quarters(bytes: ByteArray): Int {
        val at = findBox(bytes, "irot", IROT_LEN) ?: return 0
        if (at >= bytes.size) return 0
        return bytes[at].toInt() and 0x03
    }

    /**
     * Le misure dichiarate dal file, con la rotazione già applicata.
     *
     * ⚠️⚠️ **SERVE PERCHÉ IL DIALOGO DELLE INFO LE CHIEDE A `BitmapFactory`**, che su un
     * AVIF passa dallo stesso decodificatore di sistema che non ce la fa: senza questa
     * lettura la scheda direbbe '?' proprio sul formato appena aggiunto. Costa i primi
     * kilobyte del file, come per gli altri formati, e non decodifica niente.
     */
    fun dimensions(head: ByteArray): Size? {
        val at = findBox(head, "ispe", ISPE_LEN) ?: return null
        if (at + 12 > head.size) return null
        val width = int32(head, at + 4)
        val height = int32(head, at + 8)
        if (width <= 0 || height <= 0) return null
        // Un quarto di giro dispari scambia i lati: quello che interessa è la fotografia
        // come si vede, non come sta scritta nel file.
        return if (quarters(head) % 2 == 1) Size(height, width) else Size(width, height)
    }

    /**
     * Il metodo colore, letto dalla scatola `av1C` e dal tipo ausiliario dell'alfa.
     *
     * ⚠️ **Il sottocampionamento della crominanza NON entra nella risposta** (4:4:4 contro
     * 4:2:0), benché sia proprio la differenza che rende difficile questo file: [Colours]
     * non ha un posto dove metterlo, e aggiungerlo vorrebbe dire cambiare la riga per tutti
     * i formati. Resta scritto qui che il dato c'è ed è a due bit di distanza.
     */
    fun colours(head: ByteArray): Colours? {
        val at = findBox(head, "av1C", AV1C_LEN) ?: return null
        if (at + 4 > head.size) return null
        val flags = head[at + 2].toInt()
        val twelve = (flags shr 5) and 1
        val high = (flags shr 6) and 1
        val mono = (flags shr 4) and 1
        val bits = if (twelve == 1) 12 else if (high == 1) 10 else 8
        val alpha = hasAlpha(head)
        val model = when {
            mono == 1 && alpha -> Colours.Model.GREY_ALPHA
            mono == 1 -> Colours.Model.GREY
            alpha -> Colours.Model.RGBA
            else -> Colours.Model.RGB
        }
        return Colours(model, bits, palette = null, transparent = alpha)
    }

    /**
     * Se il file porta un canale alfa.
     *
     * ⚠️ **Si cerca il tipo ausiliario per NOME e non la scatola `auxC`**: in un AVIF
     * l'alfa è un secondo elemento immagine marcato da questa URN, che è una stringa
     * lunga e inconfondibile, mentre `auxC` da sola dice solo 'c'è un ausiliario' e
     * potrebbe essere una mappa di profondità.
     */
    private fun hasAlpha(head: ByteArray): Boolean =
        findRaw(head, ALPHA_URN, head.size) != null

    private val ALPHA_URN =
        "urn:mpeg:mpegB:cicp:systems:auxiliary:alpha".toByteArray(Charsets.ISO_8859_1)

    /** Le lunghezze fisse delle tre scatole che si cercano, intestazione compresa. */
    private const val ISPE_LEN = 20
    private const val AV1C_LEN = 12
    private const val IROT_LEN = 9

    /**
     * Dove comincia il contenuto della prima scatola [name] lunga [length] byte.
     *
     * ⚠️ **Una scansione lineare e non una discesa nell'albero**, ed è una semplificazione
     * dichiarata: queste scatole vivono in `meta > iprp > ipco`, e in teoria `ipco` porta
     * le proprietà di **tutti** gli elementi, con `ipma` a dire quale vale per quale. In un
     * AVIF di macchina fotografica o di Lightroom l'elemento immagine è **uno**, e le
     * proprietà del suo eventuale gemello alfa dichiarano le stesse misure e la stessa
     * profondità: la prima che si incontra è quella giusta in tutti e due i casi.
     * ⚠️⚠️ **La lunghezza è il controllo che rende la scansione affidabile**: quattro
     * lettere possono capitare per caso dentro un profilo ICC o un XMP, ma non precedute
     * dai quattro byte che dichiarano esattamente la lunghezza di quella scatola. La
     * ricerca si ferma comunque prima di `mdat`, dove stanno i byte compressi.
     */
    private fun findBox(bytes: ByteArray, name: String, length: Int): Int? {
        val tag = name.toByteArray(Charsets.ISO_8859_1)
        val stop = findRaw(bytes, MDAT, bytes.size) ?: bytes.size
        var from = 0
        while (true) {
            val found = findRaw(bytes, tag, stop, from) ?: return null
            if (found >= 4 && int32(bytes, found - 4) == length) return found + 4
            from = found + 1
        }
    }

    private val MDAT = "mdat".toByteArray(Charsets.ISO_8859_1)

    /** L'intero a 32 bit senza segno che sta in [at], come vuole ISO-BMFF (big endian). */
    private fun int32(bytes: ByteArray, at: Int): Int {
        if (at + 4 > bytes.size || at < 0) return -1
        return ((bytes[at].toInt() and 0xFF) shl 24) or
            ((bytes[at + 1].toInt() and 0xFF) shl 16) or
            ((bytes[at + 2].toInt() and 0xFF) shl 8) or
            (bytes[at + 3].toInt() and 0xFF)
    }

    /** L'indice della prima occorrenza di [tag] fra [from] e [stop], o `null`. */
    private fun findRaw(bytes: ByteArray, tag: ByteArray, stop: Int, from: Int = 0): Int? {
        val last = minOf(stop, bytes.size) - tag.size
        var i = from
        while (i <= last) {
            var k = 0
            while (k < tag.size && bytes[i + k] == tag[k]) k++
            if (k == tag.size) return i
            i++
        }
        return null
    }

    /** Gira [bitmap] di [quarters] quarti di giro in senso antiorario, come vuole `irot`. */
    private fun turn(bitmap: Bitmap, quarters: Int): Bitmap {
        if (quarters == 0) return bitmap
        val matrix = Matrix().apply { postRotate(-90f * quarters) }
        val turned = try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: OutOfMemoryError) {
            return bitmap
        }
        if (turned !== bitmap) bitmap.recycle()
        return turned
    }
}

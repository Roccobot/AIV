package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import org.aomedia.avif.android.AvifDecoder
import java.nio.ByteBuffer

/**
 * La decodifica AVIF, e perché non la fa il telefono.
 *
 * ⚠️ **`ImageDecoder` dichiara AVIF da API 31, ma si appoggia al decodificatore AV1 del
 * telefono, che non regge i file veri.** Il file di prova dell'utente (esportato da Lightroom)
 * è in profilo AVIF avanzato (`MA1A`), AV1 High con croma 4:4:4, livello 6.0, 6016x4016: la CDD
 * di Android obbliga al solo profilo Main (4:2:0), e i decodificatori hardware si fermano quasi
 * sempre al livello 5.1. Ne risulta `getPixels failed with error invalid input`, su un file
 * sano. Quindi qui decodifica libavif con dav1d, la libreria ufficiale di AOMedia, in software.
 *
 * La libreria nativa pesa circa 870 KB per `arm64-v8a` e 660 KB per `armeabi-v7a`; le varianti
 * `x86`, che servirebbero ai soli emulatori, sono escluse in `build.gradle.kts`. ⚠️ **Per questo
 * esiste [ready]**: su un dispositivo x86 `System.loadLibrary` va in errore, e senza quel
 * controllo la classe resterebbe inutilizzabile.
 *
 * ⚠️ **La scala la fa libavif**: se il bitmap passato è più piccolo dell'immagine, la decodifica
 * ci scala dentro (`avifImageScale`). Una fotografia da 24 megapixel non costa 96 MB di heap Java:
 * si chiede la misura che il budget consente, e i piani interi restano nella memoria nativa, che
 * `Runtime.maxMemory` non conta.
 */
object Avif {

    /**
     * Se la libreria nativa c'è.
     *
     * ⚠️ Si chiama una funzione nativa qualunque per forzare l'inizializzazione statica, dove vive
     * `System.loadLibrary`: senza, l'errore arriverebbe più tardi come `NoClassDefFoundError`,
     * che nessun `catch` sul punto d'uso si aspetta.
     */
    val ready: Boolean = try {
        AvifDecoder.versionString()
        true
    } catch (t: Throwable) {
        false
    }

    /** Quanti byte bastano a riconoscere il formato: `ftyp` è sempre in testa. */
    const val SNIFF = 32

    /**
     * Fin dove si legge un AVIF per trovarne le proprietà, quando servono solo quelle.
     *
     * ⚠️ **Mille byte non bastano sempre**: `ispe` e `av1C` sono dentro `meta`, e accanto a loro ci
     * può essere un profilo ICC o un XMP di parecchi kilobyte (sul file di prova dell'utente
     * `meta` misura 13.474 byte). La scansione si ferma comunque a `mdat`.
     */
    const val HEAD = 128 * 1024

    /**
     * Con quanti fili decodificare.
     *
     * ⚠️ **Si dichiara, perché la scorciatoia a tre argomenti passa zero, cioè un filo solo**:
     * `AvifDecoder.decode(buffer, len, bitmap)` chiama la versione nativa con `threads = 0`. Su un
     * Xeon a 4 core la decodifica dei 24 megapixel del file di prova richiede già 1,6 secondi.
     * Il tetto a 8: oltre, dav1d divide righe sempre più corte fra più fili, e su un telefono i
     * core in più sono quelli piccoli.
     */
    private val THREADS: Int = Runtime.getRuntime().availableProcessors().coerceIn(1, 8)

    /**
     * Riconosce un AVIF dai primi byte, senza chiamare la libreria nativa.
     *
     * Serve prima di leggere in memoria un file che può pesare decine di megabyte. La forma è
     * quella di ISO-BMFF: quattro byte di lunghezza, `ftyp`, il marchio principale e quelli
     * compatibili, che si guardano tutti perché un file può dichiarare `mif1` come principale e
     * `avif` fra i compatibili.
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
     * Restituisce `null` quando la libreria non c'è, il file non è leggibile o la decodifica
     * fallisce: chi chiama resta col proprio errore.
     */
    fun decode(bytes: ByteArray, cap: Long): Decoded? {
        // ⚠️ Il buffer diretto si costruisce una volta sola: è una copia dell'intero file in
        // memoria nativa, e due copie su un file da 25 MB sarebbero 50 MB.
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
     * Qui, al contrario di [decode], non si va a potenze di due: da 6016 pixel il passo vicino a
     * 512 darebbe 376, e il riquadro la mostrerebbe sfocata. Libavif scala a qualunque misura
     * senza costi in più.
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
        // ⚠️ Si riparte da capo: `getInfo` ha già letto questo buffer, e un `ByteBuffer` tiene
        // la propria posizione.
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
        // ⚠️ Anche le misure dichiarate si girano, non solo i pixel: finiscono nella barra delle
        // informazioni, accanto all'immagine girata.
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
     * Di quanto rimpicciolire per restare sotto [cap] pixel.
     *
     * Potenze di due come `ImageDecoder`, anche se libavif accetta qualunque misura: `sampled`
     * nella barra delle informazioni deve voler dire la stessa cosa per tutte e due le strade.
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
     * ⚠️ **Deve essere diretto**: il codice JNI chiama `GetDirectBufferAddress`, e con un
     * `ByteBuffer.wrap` la decodifica fallisce sempre, scrivendolo solo nel log.
     */
    private fun direct(bytes: ByteArray): ByteBuffer =
        ByteBuffer.allocateDirect(bytes.size).apply {
            put(bytes)
            rewind()
        }

    /**
     * I quarti di giro dichiarati dal file, letti dalla scatola `irot`.
     *
     * ⚠️ **Servono perché libavif non li applica**: il suo strato JNI decodifica i pixel così come
     * sono scritti e dichiara `ignoreExif`. Per un AVIF vale `irot`, non l'EXIF: la specifica dà
     * la precedenza alle trasformazioni del contenitore.
     * `imir` (lo specchio) non si applica: la sua convenzione sull'asse è scritta in due modi
     * diversi fra le edizioni della specifica. Si aggiungerà con un file vero che lo contenga.
     */
    private fun quarters(bytes: ByteArray): Int {
        val at = findBox(bytes, "irot", IROT_LEN) ?: return 0
        if (at >= bytes.size) return 0
        return bytes[at].toInt() and 0x03
    }

    /**
     * Le misure dichiarate dal file, con la rotazione già applicata.
     *
     * Serve perché la scheda `Info` chiede le misure a `BitmapFactory`, che su un AVIF passa dal
     * decodificatore di sistema che non ce la fa. Legge i primi kilobyte e non decodifica niente.
     */
    fun dimensions(head: ByteArray): Size? {
        val at = findBox(head, "ispe", ISPE_LEN) ?: return null
        if (at + 12 > head.size) return null
        val width = int32(head, at + 4)
        val height = int32(head, at + 8)
        if (width <= 0 || height <= 0) return null
        // Un numero dispari di quarti di giro scambia i lati: contano le misure dell'immagine
        // come si vede.
        return if (quarters(head) % 2 == 1) Size(height, width) else Size(width, height)
    }

    /**
     * Il metodo colore, letto dalla scatola `av1C` e dal tipo ausiliario dell'alfa.
     *
     * Il sottocampionamento della crominanza (4:4:4 o 4:2:0) non entra nella risposta: [Colours]
     * non ha un campo per lui. Il dato è nei due bit dopo `mono`.
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
     * Se il file ha un canale alfa.
     *
     * Si cerca il tipo ausiliario per nome e non la scatola `auxC`: l'alfa è un secondo elemento
     * immagine marcato da questa URN, mentre `auxC` da sola potrebbe essere una mappa di
     * profondità.
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
     * Una scansione lineare e non una discesa nell'albero, semplificazione dichiarata: le scatole
     * vivono in `meta > iprp > ipco`, che in teoria contiene le proprietà di tutti gli elementi.
     * In un AVIF di macchina fotografica o di Lightroom l'elemento immagine è uno, e il suo
     * eventuale gemello alfa dichiara le stesse misure e la stessa profondità.
     * ⚠️ **La lunghezza rende la scansione affidabile**: quattro lettere possono capitare per caso
     * in un profilo ICC o in un XMP, ma non precedute dalla lunghezza esatta di quella scatola.
     * La ricerca si ferma prima di `mdat`, dove sono i byte compressi.
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

    /** L'intero a 32 bit senza segno in [at], come vuole ISO-BMFF (big endian). */
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

package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * La **filigrana**: un logo o un simbolo che l'app scrive sull'immagine quando la salva.
 *
 * ⚠️⚠️ **DALLA `2.69`, ED È LA SUA SPECIFICA ALLA LETTERA** (risposta `altro` a `d-filigrana-file`,
 * giro della `2.67`: *è configurabile dalle impostazioni, sezione editor. Input PNG o SVG. Si
 * sceglie un solo logo o simbolo per volta e vale per tutti gli editing se il suo interruttore è
 * attivo al salvataggio. Per cambiare watermark, si rientra nelle impostazioni e si sceglie un
 * altro file. Cosa importante: l'utente lo sceglie e l'app lo memorizza in una sua cartella
 * interna, in modo che sopravviva anche alla cancellazione dell'originale*).
 *
 * ⚠️⚠️ **IL FILE SI COPIA COM'È E NON SI RASTERIZZA ALL'ADOZIONE, ED È LA RAGIONE PER CUI UN SVG
 * SERVE A QUALCOSA**: un vettore disegnato al momento resta nitido su un file da venti megapixel,
 * mentre uno ridotto a una misura scelta oggi sarebbe pixel come gli altri. Il PNG si copia per lo
 * stesso motivo al rovescio: è già pixel, e ridurlo all'adozione butterebbe via quello che porta.
 *
 * ⚠️⚠️ **UN FILE SOLO PER VOLTA, E LO DICE IL NOME**: la copia si chiama sempre `mark` col suffisso
 * del suo tipo, quindi sceglierne un altro prende il posto del primo senza che nessuno debba
 * cancellare niente. È la sua clausola *un solo logo o simbolo per volta*, ottenuta dalla forma
 * dell'archivio invece che da una riga che potrebbe dimenticarsi.
 *
 * ⚠️ **In `filesDir` e non in `cacheDir`**, per la stessa ragione delle copertine e degli stili:
 * quello che vive nella cache il sistema lo può buttare quando ha bisogno di spazio, e qui la
 * promessa è che il logo resti anche dopo che l'originale è sparito.
 *
 * ⚠️⚠️ **NON VIVE IN `Look`, E NON È UN DETTAGLIO**: un `Look` è un **aspetto**, cioè una cosa che
 * si porta da un'immagine all'altra e che uno stile salva; una filigrana è una **firma**, vale per
 * tutti gli editing e non ha senso dentro un preset. Per questo arriva al salvataggio come un
 * argomento a sé, come la copia di sicurezza.
 */
object Watermark {

    /**
     * I due tipi che si accettano, e si riconoscono dai **byte** e non dal nome.
     *
     * ⚠️⚠️ **DAL CONTENUTO E NON DAL SUFFISSO**: chi sceglie un file passa dal selettore di
     * sistema, che consegna un indirizzo e un tipo dichiarato da chi lo serve, e tutti e due
     * possono mentire. I byte no: un PNG comincia con la propria firma e un SVG è un documento
     * XML che contiene `<svg`.
     * ⚠️ **Un JPEG si rifiuta di proposito**: non ha trasparenza, quindi come filigrana
     * stamperebbe un rettangolo pieno sopra la fotografia. Chi vuole quella è un'altra funzione.
     */
    enum class Kind(val suffix: String) { PNG("png"), SVG("svg") }

    /**
     * Quanto grande può essere il file scelto.
     *
     * ⚠️ **Il tetto non protegge il disco, protegge il disegno**: la filigrana si rasterizza a ogni
     * salvataggio, e un PNG da cinquanta megabyte costerebbe una pausa per un logo che sullo
     * schermo occupa un ottavo del lato. Otto megabyte tengono comodamente dentro un logo a
     * risoluzione piena e qualunque SVG.
     */
    const val MAX_BYTES = 8L * 1024 * 1024

    /**
     * Dove cade la filigrana sull'immagine.
     *
     * ⚠️⚠️ **CINQUE POSTI E NON NOVE, ED È UNA SCELTA DICHIARATA**: i quattro angoli sono dove una
     * firma va a finire da sempre, e il centro è il caso di chi vuole marcare un'immagine che
     * verrà condivisa. I mezzi dei lati non aggiungono un gesto che qualcuno faccia, e
     * porterebbero quattro nomi in più in ventotto lingue.
     */
    enum class Spot(override val token: String) : Choice {
        TOP_LEFT("top-left"),
        TOP_RIGHT("top-right"),
        BOTTOM_LEFT("bottom-left"),
        BOTTOM_RIGHT("bottom-right"),
        CENTRE("centre")
    }

    /**
     * Quanto è larga la filigrana, in frazione del **lato lungo** dell'immagine.
     *
     * ⚠️⚠️ **UNA FRAZIONE E NON PIXEL, ED È LO STESSO CRITERIO DEL DETTAGLIO**: la stessa scelta
     * deve pesare uguale su un file da quattromila pixel e su uno da mille, e un numero in pixel
     * darebbe una firma enorme sul secondo e invisibile sul primo.
     * ⚠️ **Il lato LUNGO e non la larghezza**: su una fotografia verticale la larghezza è il lato
     * corto, quindi la stessa scelta darebbe una firma più piccola solo per averla girata.
     */
    enum class Size(val share: Float, override val token: String) : Choice {
        SMALL(0.08f, "small"),
        MEDIUM(0.14f, "medium"),
        LARGE(0.22f, "large"),
        HUGE(0.32f, "huge")
    }

    /**
     * Quello che il salvataggio deve sapere: dove va la filigrana e quanto è grande.
     *
     * ⚠️ **Non porta il file**, che lo sa questo oggetto: chi salva dichiara l'intenzione, e dove
     * vive la copia è una faccenda di archivio.
     */
    data class Plan(val spot: Spot, val size: Size)

    /**
     * Quanto la filigrana sta lontano dal bordo, in frazione del lato lungo.
     *
     * ⚠️ **Lo legge anche l'anteprima delle impostazioni**, che è la ragione per cui non è
     * privato: quel riquadro deve mostrare dove la firma cadrà davvero, e un secondo numero
     * scritto là divergerebbe al primo ritocco.
     */
    const val AIR = 0.03f

    /** Dove vive la copia, dentro `filesDir`. */
    private const val DIR = "watermark"

    /** La firma di un PNG: gli otto byte con cui comincia ogni file. */
    private val PNG_HEAD = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )

    /** Quanti byte bastano a riconoscere un tipo: la firma del PNG, o il `<svg` di un documento. */
    private const val SNIFF = 1024

    private fun home(context: Context): File? =
        File(context.filesDir, DIR).takeIf { it.isDirectory || it.mkdirs() }

    /**
     * Il file della filigrana scelta, se c'è.
     *
     * ⚠️ **Il file È l'archivio**, come per le copertine: non c'è nessuna preferenza da tenere
     * allineata, e la domanda *ho scelto una filigrana?* si risponde guardando il disco.
     */
    fun file(context: Context): File? {
        val casa = home(context) ?: return null
        return Kind.entries.asSequence()
            .map { File(casa, "mark.${it.suffix}") }
            .firstOrNull { it.isFile && it.length() > 0L }
    }

    /** Il tipo della filigrana scelta, ricavato dal nome che questo oggetto le ha dato. */
    fun kindOf(file: File): Kind? =
        Kind.entries.firstOrNull { it.suffix == file.extension.lowercase() }

    /**
     * Copia in casa il file scelto, e risponde se è andata.
     *
     * ⚠️⚠️ **SI LEGGE PRIMA E SI SCRIVE POI, E NON È PRUDENZA GENERICA**: il file vecchio si
     * cancella **solo** quando quello nuovo è stato letto e riconosciuto, o chi sceglie un file
     * illeggibile resterebbe senza la filigrana che aveva.
     */
    suspend fun adopt(context: Context, source: Uri): Boolean = withContext(Dispatchers.IO) {
        val casa = home(context) ?: return@withContext false
        val bytes = runCatching {
            context.contentResolver.openInputStream(source)?.use { input ->
                input.readBytes(MAX_BYTES + 1)
            }
        }.getOrNull() ?: return@withContext false
        if (bytes.size > MAX_BYTES) return@withContext false
        val kind = kindOf(bytes) ?: return@withContext false
        // ⚠️ Provato PRIMA di adottarlo: un documento che non si disegna darebbe una filigrana
        // che non compare, cioè un salvataggio che non fa quello che promette.
        if (render(bytes, kind, 64) == null) return@withContext false

        val file = File(casa, "mark.${kind.suffix}")
        val written = runCatching { file.writeBytes(bytes); true }.getOrDefault(false)
        if (!written) {
            file.delete()
            return@withContext false
        }
        // Un solo logo per volta: quello dell'altro tipo se ne va.
        Kind.entries.filter { it != kind }.forEach { File(casa, "mark.${it.suffix}").delete() }
        true
    }

    /** Toglie la filigrana scelta. */
    fun forget(context: Context) {
        val casa = home(context) ?: return
        Kind.entries.forEach { File(casa, "mark.${it.suffix}").delete() }
    }

    /**
     * Legge i byte fino a un tetto, e dice se il file è più grande **senza** caricarlo tutto.
     *
     * ⚠️ Si legge uno in più del tetto: così chi chiama distingue 'grande quanto il tetto' da
     * 'più grande del tetto' guardando la sola lunghezza.
     */
    private fun java.io.InputStream.readBytes(cap: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read <= 0) break
            total += read
            out.write(buffer, 0, read)
            if (total > cap) break
        }
        return out.toByteArray()
    }

    /** Che cosa sono questi byte, o `null` se non è nessuno dei due tipi ammessi. */
    private fun kindOf(bytes: ByteArray): Kind? {
        if (bytes.size >= PNG_HEAD.size && PNG_HEAD.indices.all { bytes[it] == PNG_HEAD[it] }) {
            return Kind.PNG
        }
        val head = String(bytes, 0, minOf(SNIFF, bytes.size), Charsets.ISO_8859_1)
        return if (head.contains("<svg")) Kind.SVG else null
    }

    /**
     * Disegna la filigrana, col lato lungo a [box] pixel.
     *
     * ⚠️ **L'SVG passa da `Svg.render`**, che è la stessa strada del visualizzatore e delle
     * miniature: riscrivere qui il rendering vorrebbe dire due parser dello stesso formato.
     */
    private fun render(bytes: ByteArray, kind: Kind, box: Int): Bitmap? = when (kind) {
        Kind.SVG -> Svg.render(bytes, box)?.bitmap
        Kind.PNG -> renderPng(bytes, box)
    }

    /**
     * Il PNG, campionato in lettura e poi portato alla misura voluta.
     *
     * ⚠️⚠️ **SI INGRANDISCE SE SERVE, E IL COSTO SI DICHIARA**: un logo da 200 pixel chiesto
     * 'Grande' su una fotografia da quattromila viene sfocato, perché i pixel che mancano non li
     * inventa nessuno. La via alternativa (fermarsi ai pixel del file) darebbe una firma che
     * cambia misura a seconda dell'immagine, cioè un'impostazione che non fa quello che dice. Chi
     * vuole una filigrana nitida a ogni misura usa un SVG, che è la ragione per cui si accettano
     * tutti e due i formati.
     */
    private fun renderPng(bytes: ByteArray, box: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val long = max(bounds.outWidth, bounds.outHeight)
        if (long <= 0) return null
        val options = BitmapFactory.Options().apply {
            // ⚠️ Il campionamento si chiede solo per RIDURRE: `inSampleSize` a uno lascia il file
            // com'è, e chiedere una potenza di due più grande di quanto serve butterebbe via
            // pixel che poi si dovrebbero inventare.
            var step = 1
            while (long / (step * 2) >= box) step *= 2
            inSampleSize = step
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) }
            .getOrNull() ?: return null
        val rawLong = max(raw.width, raw.height)
        if (rawLong == box || rawLong <= 0) return raw
        val scale = box.toFloat() / rawLong
        val w = max(1, (raw.width * scale).roundToInt())
        val h = max(1, (raw.height * scale).roundToInt())
        return runCatching { Bitmap.createScaledBitmap(raw, w, h, true) }
            .getOrNull()
            ?.also { if (it !== raw) raw.recycle() }
    }

    /**
     * La filigrana pronta per un'immagine col lato lungo [imageLong], alla misura di [plan].
     *
     * Torna `null` quando non c'è nessuna filigrana scelta o quando il file non si disegna: chi
     * chiama in quel caso salva l'immagine e basta, invece di fallire.
     */
    fun bitmapFor(context: Context, imageLong: Int, plan: Plan): Bitmap? =
        artwork(context, max(1, (imageLong * plan.size.share).roundToInt()))

    /**
     * Il disegno nudo, col lato lungo a [box] pixel, o `null` se non c'è o non si disegna.
     *
     * ⚠️ **Lo chiama anche l'anteprima delle impostazioni**, che la misura se la dà da sé: là
     * quello che serve è il disegno, e quanto grande finirà sull'immagine lo dicono i due numeri
     * che quel riquadro ha già.
     */
    fun artwork(context: Context, box: Int): Bitmap? {
        val file = file(context) ?: return null
        val kind = kindOf(file) ?: return null
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        return render(bytes, kind, max(1, box))
    }

    /**
     * Firma l'immagine, e risponde con quella firmata: **la stessa** quando si può scrivere
     * sopra, una copia quando il bitmap ricevuto è immutabile, `null` quando non c'è niente da
     * scrivere.
     *
     * ⚠️⚠️ **SI SCRIVE SUL BITMAP RICEVUTO QUANDO SI PUÒ, E LA COPIA È IL RIPIEGO**: chi chiama ha
     * in mano l'immagine finita e la sta per comprimere, e una copia a venti megapixel sono
     * ottanta megabyte per un logo in un angolo. Nel caso comune non serve: quello che esce dal
     * ritaglio, dallo shader e dalla maglia della geometria è un bitmap costruito da loro, cioè
     * mutabile. ⚠️ **Se la copia non c'è memoria per farla, la firma salta**: chi chiama scrive
     * l'immagine senza, che è meglio di un salvataggio fallito.
     */
    fun stamp(context: Context, image: Bitmap, plan: Plan): Bitmap? {
        val long = max(image.width, image.height)
        val mark = bitmapFor(context, long, plan) ?: return null
        val sheet = if (image.isMutable) image else runCatching {
            image.copy(Bitmap.Config.ARGB_8888, true)
        }.getOrNull()
        if (sheet == null) {
            mark.recycle()
            return null
        }
        try {
            val air = long * AIR
            val x = when (plan.spot) {
                Spot.TOP_LEFT, Spot.BOTTOM_LEFT -> air
                Spot.TOP_RIGHT, Spot.BOTTOM_RIGHT -> sheet.width - mark.width - air
                Spot.CENTRE -> (sheet.width - mark.width) / 2f
            }
            val y = when (plan.spot) {
                Spot.TOP_LEFT, Spot.TOP_RIGHT -> air
                Spot.BOTTOM_LEFT, Spot.BOTTOM_RIGHT -> sheet.height - mark.height - air
                Spot.CENTRE -> (sheet.height - mark.height) / 2f
            }
            // ⚠️ Il filtro serve perché la destinazione non cade su pixel interi: senza, il
            // disegno verrebbe arrotondato e i bordi si scalinerebbero.
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
            Canvas(sheet).drawBitmap(
                mark,
                Rect(0, 0, mark.width, mark.height),
                RectF(x, y, x + mark.width, y + mark.height),
                paint
            )
        } finally {
            mark.recycle()
        }
        return sheet
    }
}

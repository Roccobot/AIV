package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * La **filigrana**: un logo o un simbolo che l'app scrive sull'immagine quando la salva.
 *
 * La specifica è dell'utente (risposta `altro` a `d-filigrana-file`): si sceglie nelle
 * impostazioni dell'editor, PNG o SVG, un file per volta, e vale per ogni salvataggio con
 * l'interruttore acceso. L'app ne tiene una copia sua, che resta anche se l'originale è cancellato.
 *
 * ⚠️ **Il file si copia com'è, senza rasterizzarlo**: un SVG disegnato al momento del salvataggio
 * resta nitido su un file da venti megapixel; un PNG è già pixel, e ridurlo all'adozione
 * perderebbe dettaglio.
 * Un file per volta, per costruzione: la copia si chiama sempre `mark` col suffisso del tipo,
 * quindi una scelta nuova prende il posto della vecchia.
 * In `filesDir`, come copertine e stili: da `cacheDir` il sistema può cancellare quando gli serve
 * spazio.
 *
 * ⚠️ **Non è in `Look`**: un `Look` è un aspetto che passa da un'immagine all'altra e che uno stile
 * salva; la filigrana è una firma, vale per tutti i salvataggi, e arriva al salvataggio come
 * argomento a sé, come la copia di sicurezza.
 */
object Watermark {

    /**
     * I due tipi che si accettano, e si riconoscono dai **byte** e non dal nome.
     *
     * Il selettore di sistema consegna un indirizzo e un tipo dichiarato da chi serve il file, e
     * tutti e due possono mentire; i byte no: un PNG comincia con la sua firma, un SVG contiene
     * `<svg`.
     * Un JPEG si rifiuta: senza trasparenza stamperebbe un rettangolo pieno sull'immagine.
     */
    enum class Kind(val suffix: String) { PNG("png"), SVG("svg") }

    /**
     * Quanto grande può essere il file scelto.
     *
     * Il tetto protegge il disegno: la filigrana si rasterizza a ogni salvataggio, e un PNG enorme
     * costerebbe una pausa. Otto megabyte bastano a un logo a piena risoluzione e a qualunque SVG.
     */
    const val MAX_BYTES = 8L * 1024 * 1024

    /**
     * Dove cade la filigrana sull'immagine.
     *
     * Cinque posti: i quattro angoli, dove una firma va da sempre, e il centro, per marcare
     * un'immagine da condividere. I punti a metà dei lati costerebbero quattro nomi in più in
     * ventotto lingue, per un uso che nessuno fa.
     */
    enum class Spot(override val token: String) : Choice {
        TOP_LEFT("top-left"),
        TOP_RIGHT("top-right"),
        BOTTOM_LEFT("bottom-left"),
        BOTTOM_RIGHT("bottom-right"),
        CENTRE("centre")
    }

    /**
     * La misura della firma, in **centesimi** del lato lungo dell'immagine, scritta a mano come in
     * Lightroom (voce `filigrana` del giro della 2.70): chi riproduce una firma tarata altrove ha
     * bisogno del numero.
     * Una frazione, non pixel, come nel modulo Dettaglio: la stessa scelta pesa uguale su un file
     * da quattromila pixel e su uno da mille.
     * Il lato lungo, non la larghezza: su un'immagine verticale la larghezza è il lato corto, e la
     * firma rimpicciolirebbe solo per averla girata.
     */
    val SIZE = 1..50

    /**
     * La distanza della filigrana dal bordo, in **decimi** di centesimo del lato lungo: da zero a
     * venticinque centesimi, col passo di un decimo, perché l'utente vuole poter scrivere 0,2 e 0,5
     * (giro dalla 2.71 alla 2.74). La conversione la fa [AIR_STEP].
     * Lo zero è ammesso: una firma a filo del bordo è una scelta possibile.
     */
    val AIR = 0..250

    /**
     * Quanti passi di [AIR] fanno un centesimo.
     *
     * Vive qui perché la leggono in due: il conto del posto ([cornerFor]) e il campo delle
     * impostazioni, che scrive `0,2`. Scritta due volte, la firma potrebbe cadere a una distanza
     * diversa da quella che il numero dice.
     */
    const val AIR_STEP = 10

    /**
     * Quanto la filigrana è opaca, in centesimi.
     *
     * Solo opacità, senza metodi di fusione (risposta `si` a `d-mark-opacita`): la firma si posa
     * sopra, e questo numero muove il solo canale alfa.
     * Il minimo è cinque: a zero la firma non lascerebbe un pixel diverso, e per non scriverla c'è
     * l'interruttore. Sotto il cinque per cento non si vede comunque.
     */
    val ALPHA = 5..100

    /** La misura di fabbrica della firma. */
    const val SIZE_DEFAULT = 14

    /** La distanza di fabbrica dal bordo: tre centesimi, cioè trenta decimi. */
    const val AIR_DEFAULT = 3 * AIR_STEP

    /** L'opacità di fabbrica: piena, cioè quella del file. */
    const val ALPHA_DEFAULT = 100

    /**
     * Quello che il salvataggio deve sapere: dove va la firma, quanto è grande, quanto è lontana
     * dal bordo e quanto è opaca.
     *
     * Il file non c'è: lo conosce questo oggetto. I tre numeri viaggiano col posto e non si
     * leggono dalle preferenze qui dentro, così l'anteprima delle impostazioni disegna con gli
     * stessi valori del salvataggio.
     */
    data class Plan(
        val spot: Spot,
        val size: Int = SIZE_DEFAULT,
        val air: Int = AIR_DEFAULT,
        val alpha: Int = ALPHA_DEFAULT
    )

    /** Dove vive la copia, dentro `filesDir`. */
    private const val DIR = "watermark"

    /** Quanti byte bastano a riconoscere un tipo: la firma del PNG, o il `<svg` di un documento. */
    private const val SNIFF = 1024

    private fun home(context: Context): File? =
        File(context.filesDir, DIR).takeIf { it.isDirectory || it.mkdirs() }

    /**
     * Il file della filigrana scelta, se c'è.
     *
     * Il file è l'archivio, come per le copertine: nessuna preferenza da tenere allineata, e se
     * una filigrana è scelta lo dice il disco.
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
     * ⚠️ **Il file vecchio si cancella solo quando il nuovo è stato letto e riconosciuto**: chi
     * sceglie un file illeggibile tiene la filigrana che aveva.
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
        // Provato prima di adottarlo: un documento che non si disegna darebbe una filigrana che
        // non compare.
        if (render(bytes, kind, 64) == null) return@withContext false

        // ⚠️ Con [AtomicWrite] (rilievo B21): una scrittura interrotta sul posto cancellerebbe
        // anche la filigrana dello stesso tipo che c'era prima.
        val file = File(casa, "mark.${kind.suffix}")
        if (!AtomicWrite.bytes(file, bytes)) return@withContext false
        // Un solo logo per volta: quello dell'altro tipo si cancella.
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
     * Si legge oltre il tetto di almeno un byte: così chi chiama distingue 'grande quanto il
     * tetto' da 'più grande' guardando la sola lunghezza.
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
        if (Signatures.png(bytes)) {
            return Kind.PNG
        }
        val head = String(bytes, 0, minOf(SNIFF, bytes.size), Charsets.ISO_8859_1)
        return if (head.contains("<svg")) Kind.SVG else null
    }

    /**
     * Disegna la filigrana, col lato lungo a [box] pixel.
     *
     * L'SVG passa da `Svg.render`, la stessa strada del visualizzatore e delle miniature.
     */
    private fun render(bytes: ByteArray, kind: Kind, box: Int): Bitmap? = when (kind) {
        Kind.SVG -> Svg.render(bytes, box)?.bitmap
        Kind.PNG -> renderPng(bytes, box)
    }

    /**
     * Il PNG, campionato in lettura e poi portato alla misura voluta.
     *
     * ⚠️ **Si ingrandisce se serve, e resta morbido**: un logo da 200 pixel chiesto grande su
     * un'immagine da quattromila è sfocato. Fermarsi ai pixel del file darebbe una firma che
     * cambia misura secondo l'immagine. Per una filigrana nitida a ogni misura c'è l'SVG.
     */
    private fun renderPng(bytes: ByteArray, box: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val long = max(bounds.outWidth, bounds.outHeight)
        if (long <= 0) return null
        val options = BitmapFactory.Options().apply {
            // Il campionamento solo per ridurre, e mai sotto [box]: una potenza di due più
            // grande del necessario perderebbe pixel che poi andrebbero inventati.
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
     * `null` quando nessuna filigrana è scelta o il file non si disegna: allora chi chiama salva
     * l'immagine senza.
     */
    fun bitmapFor(context: Context, imageLong: Int, plan: Plan): Bitmap? =
        artwork(context, max(1, sideFor(plan, imageLong.toFloat()).roundToInt()))

    /**
     * Quanto è lungo il lato lungo della firma su un foglio il cui lato lungo misura [long].
     *
     * È una funzione a sé perché un conto con un nome si prova da solo, senza contare i pixel di
     * un foglio. Il lato lungo, non la larghezza: il perché è su [SIZE].
     */
    fun sideFor(plan: Plan, long: Float): Float = long * plan.size / 100f

    /**
     * Dove cade l'angolo in alto a sinistra di una firma larga [markWide] e alta [markHigh], su un
     * foglio di [sheetWide] per [sheetHigh].
     *
     * Le misure arrivano come numeri, non come bitmap: il conto si prova senza un'immagine, come
     * [sideFor].
     * La distanza si misura sul lato lungo del foglio, come la firma: la stessa scelta lascia la
     * stessa distanza proporzionale in verticale e in orizzontale.
     * Al centro non c'è un bordo, quindi la distanza non entra nel conto (come nell'anteprima
     * delle impostazioni).
     * `100f * AIR_STEP` è la sola conversione dell'unità della distanza, che è in decimi di
     * centesimo.
     */
    fun cornerFor(
        plan: Plan,
        sheetWide: Float,
        sheetHigh: Float,
        markWide: Float,
        markHigh: Float
    ): PointF {
        val air = max(sheetWide, sheetHigh) * plan.air / (100f * AIR_STEP)
        val x = when (plan.spot) {
            Spot.TOP_LEFT, Spot.BOTTOM_LEFT -> air
            Spot.TOP_RIGHT, Spot.BOTTOM_RIGHT -> sheetWide - markWide - air
            Spot.CENTRE -> (sheetWide - markWide) / 2f
        }
        val y = when (plan.spot) {
            Spot.TOP_LEFT, Spot.TOP_RIGHT -> air
            Spot.BOTTOM_LEFT, Spot.BOTTOM_RIGHT -> sheetHigh - markHigh - air
            Spot.CENTRE -> (sheetHigh - markHigh) / 2f
        }
        return PointF(x, y)
    }

    /**
     * Il disegno nudo, col lato lungo a [box] pixel, o `null` se non c'è o non si disegna.
     *
     * Lo chiama anche l'anteprima delle impostazioni, che sceglie la misura da sé.
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
     * ⚠️ **Si scrive sul bitmap ricevuto quando si può**: una copia a venti megapixel sono
     * ottanta megabyte per un logo in un angolo. Nel caso comune è mutabile, perché lo
     * costruiscono il ritaglio, lo shader e la maglia della geometria.
     * Senza memoria per la copia la firma salta, e chi chiama scrive l'immagine senza.
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
            val corner = cornerFor(
                plan,
                sheet.width.toFloat(),
                sheet.height.toFloat(),
                mark.width.toFloat(),
                mark.height.toFloat()
            )
            /*
             * ⚠️ **La firma si posa su pixel interi**: il disegno è già alla misura giusta, ma
             * [cornerFor] dà un angolo in virgola mobile, e un bitmap posato a `123,7` si
             * campiona su ogni pixel come media di quattro vicini, cioè arriva morbido
             * (segnalazione dell'utente, giro dalla 2.71 alla 2.74). Arrotondato, l'angolo
             * sposta la firma al massimo di mezzo pixel, e ogni pixel arriva com'è; per questo
             * il pennello non ha il filtro.
             */
            val x = corner.x.roundToInt()
            val y = corner.y.roundToInt()
            /*
             * L'opacità va sul pennello: il suo `alpha` moltiplica il canale alfa del disegno,
             * quindi un PNG tiene le sue trasparenze e in più si smorza, senza una passata sui
             * pixel. La miscelazione resta quella normale (`d-mark-opacita`).
             */
            val paint = Paint().apply {
                alpha = (plan.alpha.coerceIn(ALPHA) * 255 / 100f).roundToInt()
            }
            Canvas(sheet).drawBitmap(mark, x.toFloat(), y.toFloat(), paint)
        } finally {
            mark.recycle()
        }
        return sheet
    }
}

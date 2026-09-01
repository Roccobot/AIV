package io.github.roccobot.aiv

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.json.JSONObject
import java.io.Closeable
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.math.sqrt

/**
 * Il motore della ricerca per contenuto: due modelli, un tokenizzatore, e i numeri che ne
 * escono.
 *
 * ⚠️⚠️ **QUESTO FILE GIRA ANCHE FUORI DA ANDROID, ed è il motivo per cui non nomina un
 * `Bitmap`**: ONNX Runtime esiste come libreria desktop con la **stessa API Java** dell'AAR
 * Android, quindi lo stesso codice si esegue su una JVM normale coi modelli veri. È così che
 * è stato verificato prima di arrivare su un telefono, e chi lo tocca può rifare la prova
 * invece di sperare.
 *
 * **I nomi e le forme sono LETTI DAI MODELLI, non ricordati** (`getInputInfo` sui file veri,
 * 2026-08-31):
 * - immagine: `pixel_values` float32 `[batch, 3, 256, 256]` -> `image_embeds` float32
 *   `[batch, 512]`;
 * - testo: `input_ids` **int64** `[batch, sequence]` -> `text_embeds` float32 `[batch, 512]`.
 *
 * ⚠️⚠️ **LA SEQUENZA DEL TESTO SI DICHIARA DINAMICA E NON LO È**: il modello annuncia
 * `sequence_length` variabile, ma dentro ha l'embedding di posizione di **77** posti, e con
 * una sequenza più corta il runtime **solleva** (`Attempting to broadcast an axis by a
 * dimension other than 1. 4 by 77`, misurato). Per questo [ClipTokenizer] riempie sempre fino
 * a 77: la forma dichiarata dal file mente, quella vera è fissa.
 */
class ClipEngine private constructor(
    private val env: OrtEnvironment,
    private val visionPath: String,
    private val textPath: String,
    private val tokenizerJson: () -> String
) : Closeable {

    /*
     * ⚠️⚠️ **I DUE MODELLI SI APRONO SEPARATI E SOLO QUANDO SERVONO, dalla 1.02**: prima
     * [open] li apriva tutti e due insieme più il tokenizzatore, cioè teneva in memoria 65 MB
     * di pesi e un vocabolario da 49.408 voci anche per fare una cosa sola. Ma indicizzare usa
     * il **solo** encoder immagine, e cercare il **solo** encoder testuale: la punta di memoria
     * era il doppio del necessario senza che niente lo richiedesse, ed è il primo sospettato
     * del crollo che rendeva l'app irrecuperabile (vedi [ClipGuard]).
     * ⚠️ **Non è la prova della causa**, che resta ignota perché il processo muore senza
     * lasciare un errore: è la riduzione della cosa più probabile. La protezione vera è la
     * sicura, non questo.
     */
    private var vision: OrtSession? = null
    private var text: OrtSession? = null
    private var words: ClipTokenizer? = null

    private fun visionSession(): OrtSession =
        vision ?: env.createSession(visionPath, options()).also { vision = it }

    private fun textSession(): OrtSession =
        text ?: env.createSession(textPath, options()).also { text = it }

    private fun tokenizer(): ClipTokenizer =
        words ?: readTokenizer(tokenizerJson()).let { (vocab, merges) ->
            ClipTokenizer(vocab, merges).also { words = it }
        }

    /**
     * Apre la sessione dell'encoder immagine, senza darle niente da fare.
     *
     * ⚠️⚠️ **ESISTE PER POTER DIRE DOVE SI MUORE, dalla 1.07**: aprire un modello e dargli
     * una fotografia sono i due punti in cui il processo può sparire senza lasciare un
     * errore, e finché stavano nella stessa chiamata la sicura non poteva distinguerli.
     * Separati, il segno lasciato prima dice quale dei due era in corso, e le due cause non
     * si somigliano affatto: la prima è il file del modello o il runtime, la seconda è una
     * fotografia o un accumulo.
     * ⚠️ **Non è un riscaldamento per andare più veloce**, e chi legge il nome potrebbe
     * crederlo: la sessione si sarebbe aperta comunque alla prima immagine, e qui si sposta
     * soltanto il momento in cui succede.
     */
    fun warmImage() {
        visionSession()
    }

    /**
     * Il vettore di una fotografia: 512 numeri, già normalizzati.
     *
     * ⚠️ Chi chiama arriva coi pixel **già rimpiccioliti** quanto basta (vedi [ClipPixels]):
     * il ridimensionamento fine lo fa questa catena, ma partire da venti megapixel vorrebbe
     * dire una bilineare in Kotlin su venti milioni di punti.
     */
    fun ofImage(argb: IntArray, width: Int, height: Int): FloatArray {
        val pixels = ClipPixels.square(argb, width, height)
        val shape = longArrayOf(1, 3, ClipPixels.SIDE.toLong(), ClipPixels.SIDE.toLong())
        val session = visionSession()
        OnnxTensor.createTensor(env, FloatBuffer.wrap(pixels), shape).use { tensor ->
            session.run(mapOf(IMAGE_IN to tensor)).use { result ->
                @Suppress("UNCHECKED_CAST")
                val rows = result.get(0).value as Array<FloatArray>
                return unit(rows[0])
            }
        }
    }

    /** Il vettore di una frase: 512 numeri, già normalizzati. */
    fun ofText(phrase: String): FloatArray {
        val session = textSession()
        val ids = tokenizer().encode(phrase)
        val longs = LongArray(ids.size) { ids[it].toLong() }
        val shape = longArrayOf(1, ids.size.toLong())
        OnnxTensor.createTensor(env, LongBuffer.wrap(longs), shape).use { tensor ->
            session.run(mapOf(TEXT_IN to tensor)).use { result ->
                @Suppress("UNCHECKED_CAST")
                val rows = result.get(0).value as Array<FloatArray>
                return unit(rows[0])
            }
        }
    }

    override fun close() {
        runCatching { vision?.close() }
        runCatching { text?.close() }
        vision = null
        text = null
        words = null
    }

    companion object {

        /** Il nome dell'ingresso dell'encoder immagine, letto dal modello. */
        private const val IMAGE_IN = "pixel_values"

        /** Il nome dell'ingresso dell'encoder testuale, letto dal modello. */
        private const val TEXT_IN = "input_ids"

        /**
         * Apre i due modelli e prepara il tokenizzatore.
         *
         * ⚠️ **Prende PERCORSI e il JSON già letto**, non un `Context`: è quello che permette
         * di aprirlo su una JVM per provarlo. Chi lo usa nell'app passa i file di
         * `ClipModels`.
         * ⚠️ **Questa chiamata non tocca ONNX Runtime**: tiene i percorsi e basta, e le
         * sessioni nascono alla prima immagine o alla prima frase. Aprire costa qualche
         * centinaio di millisecondi e decine di megabyte, e va fatto quando si sa che
         * serviranno davvero.
         */
        fun open(visionPath: String, textPath: String, tokenizerJson: () -> String): ClipEngine =
            ClipEngine(OrtEnvironment.getEnvironment(), visionPath, textPath, tokenizerJson)

        /**
         * Come si apre una sessione.
         *
         * ⚠️ **Due fili e non tutti quelli che ci sono**: di serie ONNX Runtime ne prende
         * quanti sono i nuclei, e su un telefono vuol dire otto arene di memoria per un
         * lavoro che gira in sottofondo mentre l'utente sfoglia le fotografie. Due bastano a
         * non far durare l'indicizzazione il doppio, e la punta di memoria si abbassa.
         */
        private fun options(): OrtSession.SessionOptions =
            OrtSession.SessionOptions().apply { setIntraOpNumThreads(THREADS) }

        /** Quanti fili per il motore. Vedi [options]. */
        private const val THREADS = 2

        /**
         * Quanto due vettori si somigliano, da -1 a 1.
         *
         * ⚠️ **È un prodotto scalare e basta**, perché i due arrivano già normalizzati: su
         * vettori di lunghezza uno il coseno **è** il prodotto scalare, e rifare le radici a
         * ogni confronto vorrebbe dire pagarle diecimila volte per fotografia indicizzata.
         */
        fun near(a: FloatArray, b: FloatArray): Float {
            var sum = 0f
            for (i in a.indices) sum += a[i] * b[i]
            return sum
        }

        /**
         * Il vettore riportato a lunghezza uno.
         *
         * ⚠️⚠️ **SERVE, anche se MobileCLIP normalizza dentro**: quello che normalizza dentro
         * è l'**ingresso** dell'immagine (`do_normalize: false` nel preprocessing), non
         * l'uscita. I due vettori vanno confrontati col coseno, e il coseno vuole lunghezza
         * uno da tutti e due i lati.
         */
        private fun unit(v: FloatArray): FloatArray {
            var sum = 0.0
            for (x in v) sum += (x * x).toDouble()
            val len = sqrt(sum).toFloat()
            if (len <= 0f) return v
            return FloatArray(v.size) { v[it] / len }
        }

        /**
         * Vocabolario e fusioni da `tokenizer.json`.
         *
         * ⚠️ **`org.json` e non una libreria in più**: su Android c'è già, e sulla JVM è un
         * artefatto da settanta kilobyte, quindi lo stesso codice si prova senza cambiare
         * niente. ⚠️ Sono 2,2 MB di JSON e 49.408 voci: si legge **una volta**, all'apertura
         * del motore.
         * ⚠️ **Le fusioni sono stringhe con uno spazio in mezzo** (`"i n"`), non coppie:
         * misurato sul file vero, dove sono 48.894. Chi si aspetta un array di due elementi
         * (la forma nuova di `tokenizers`) prende un elenco vuoto e una ricerca che risponde
         * a caso senza errori.
         */
        private fun readTokenizer(json: String): Pair<Map<String, Int>, List<Pair<String, String>>> {
            val model = JSONObject(json).getJSONObject("model")
            val raw = model.getJSONObject("vocab")
            val vocab = HashMap<String, Int>(raw.length() * 2)
            val keys = raw.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                vocab[key] = raw.getInt(key)
            }
            val list = model.getJSONArray("merges")
            val merges = ArrayList<Pair<String, String>>(list.length())
            for (i in 0 until list.length()) {
                val line = list.getString(i)
                val gap = line.indexOf(' ')
                if (gap > 0) merges.add(line.substring(0, gap) to line.substring(gap + 1))
            }
            return vocab to merges
        }
    }
}

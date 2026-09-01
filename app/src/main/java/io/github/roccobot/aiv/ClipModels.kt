package io.github.roccobot.aiv

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/**
 * I due modelli della ricerca per contenuto, e il tokenizzatore: dove stanno e come arrivano.
 *
 * ⚠️⚠️ **NON STANNO DENTRO L'APK, e sono 86 MB**: l'app pesa tre megabyte e mezzo, e la
 * ricerca per contenuto è **spenta di fabbrica**. Metterli dentro vorrebbe dire far scaricare
 * a tutti venti volte l'app per una funzione che molti non accenderanno mai. Si scaricano
 * quando si accende l'interruttore, e si possono togliere.
 *
 * ⚠️⚠️ **DA HUGGING FACE E NON DA UNA RELEASE DI AIV, ed è uno scostamento dal piano
 * dichiarato**: il brief prevedeva di ospitarli fra gli asset di una release, e questa
 * sessione non può caricarci 86 MB. Il rischio dello scostamento è che i file cambino o
 * spariscano da sotto, e il rimedio è la ragione per cui esistono le impronte qui sotto:
 * quello che si scarica si **verifica**, e se non combacia non si usa. Chi un domani li
 * ospita altrove cambia [BASE] e nient'altro.
 *
 * ⚠️⚠️ **PERCHÉ PROPRIO QUESTE DUE COMBINAZIONI, misurato il 2026-08-30 e non scelto**: la
 * quantizzazione `int8` distrugge l'encoder **immagine** e non quello **testuale**. Con
 * `vision int8` i punteggi si schiacciano fra 0,10 e 0,14 e la classifica è casuale (margini
 * +0,009 contro i +0,102 della versione buona), e la cosa **non dà nessun errore**: la
 * ricerca semplicemente risponde a caso. Chi cambia l'encoder immagine in `int8` per
 * risparmiare 30 MB rompe tutto in silenzio.
 *
 * ⚠️⚠️ **L'ENCODER IMMAGINE È `fp32` DALLA 1.11, E COSTA 23 MB IN PIÙ: è il tentativo di far
 * funzionare la funzione su ARM.** Fino alla `1.10` era `vision_model_fp16.onnx`, 22.876.479
 * byte, e su un telefono vero l'indicizzazione **chiudeva il processo alla prima fotografia**
 * a ogni giro, mentre lo stesso file girava su x86 in 0,04 secondi (catena di misure in
 * [ClipEngine], `options`). Il sospetto che regge tutte le misure è la strada **float16 su
 * arm64**, e questo file la toglie del tutto: dei 227 initializer, zero sono `FLOAT16`.
 * ⚠️ **La ricerca non cambia**: stesso nome d'ingresso, stessa forma, e i due vettori
 * misurati sulla stessa immagine hanno coseno **0,999997**. Non è una stima, è una prova
 * fatta con lo stesso ONNX Runtime `1.29.0`.
 * ⚠️⚠️ **SE ANCHE COSÌ SI CHIUDE, LA FUNZIONE VA TOLTA, non tentata una quarta volta**: gli
 * altri due sospetti (i nuclei SME di KleidiAI, il pool di fili) sono già stati spenti e non
 * è bastato. Rimettere `fp16` per riguadagnare 23 MB significa rimettere il crollo.
 */
object ClipModels {

    /**
     * Che cosa c'è sul telefono.
     *
     * ⚠️ **[Broken] è distinto da [Absent]**, e non è pedanteria: un file arrivato a metà o
     * con l'impronta sbagliata è un caso in cui **si è già scaricato** e qualcosa non ha
     * funzionato, e chi legge deve poterlo dire invece di ripresentare 'scarica' come se non
     * fosse successo niente.
     */
    sealed interface State {
        data object Absent : State
        data class Fetching(val done: Long, val total: Long) : State
        data object Ready : State
        data class Broken(val detail: String) : State
    }

    /**
     * Un file da scaricare, con quanto deve pesare e che impronta deve avere.
     *
     * ⚠️⚠️ **L'IMPRONTA È IL CONTROLLO VERO, il peso è solo quello a buon mercato**: due file
     * diversi possono pesare uguale, mentre due `sha256` uguali sono lo stesso file. Il peso
     * serve prima, per mostrare l'avanzamento, e per scartare in un istante un file
     * palesemente sbagliato senza leggerlo tutto.
     */
    data class Piece(val name: String, val path: String, val size: Long, val print: String)

    /** Da dove si scaricano. Cambiare qui, e solo qui, per ospitarli altrove. */
    private const val BASE = "https://huggingface.co/Xenova/mobileclip_s0/resolve/main/"

    /**
     * I tre pezzi, con peso e impronta **misurati scaricandoli davvero** (i due invariati il
     * 2026-08-31, l'encoder immagine il 2026-09-01 quando è passato a `fp32`).
     *
     * ⚠️ Il `tokenizer.json` sta qui e non fra gli asset dell'APK per la stessa ragione degli
     * altri due: sono 2,2 MB che servono solo a chi accende la ricerca per contenuto, e
     * dentro l'APK li pagherebbe anche chi non l'accende. ⚠️ Il tokenizzatore in Kotlin
     * esiste già e non legge JSON (vedi [ClipTokenizer]): vocabolario e fusioni gli arrivano
     * da fuori, ed è di qui che vengono.
     */
    val PIECES = listOf(
        Piece(
            name = "vision.onnx",
            path = "onnx/vision_model.onnx",
            size = 45_543_630L,
            print = "17d3c037b1d488c10c50e09f6009ea5a198caef4e0e8f4ea5617b7cb2d067ac0"
        ),
        Piece(
            name = "text.onnx",
            path = "onnx/text_model_int8.onnx",
            size = 42_799_230L,
            print = "fc8d87978623385c17a46331ffb9cb5ab7fe8b61c513c094602b85f08edd0a0b"
        ),
        Piece(
            name = "tokenizer.json",
            path = "tokenizer.json",
            size = 2_224_081L,
            print = "72ed5c96db5729294468543e4bc75fce14ca63f58e37300290189ba1c1e52b85"
        )
    )

    /** Quanto pesano in tutto: il numero da mostrare prima di far scaricare. */
    val WEIGHT: Long = PIECES.sumOf { it.size }

    /** Dove vivono: nella cartella privata dell'app, che si svuota disinstallandola. */
    fun home(context: Context): File = File(context.filesDir, "clip")

    fun fileOf(context: Context, piece: Piece): File = File(home(context), piece.name)

    /**
     * Che cosa c'è, guardando i file.
     *
     * ⚠️⚠️ **NON RICALCOLA LE IMPRONTE, e guarda solo i pesi**: verificare 86 MB costa quasi
     * un secondo, e questa risposta serve a disegnare una schermata. L'impronta si controlla
     * una volta, **quando si scarica**, che è il momento in cui può essere sbagliata; da lì
     * in poi quei file li tocca solo il sistema operativo.
     */
    fun state(context: Context): State {
        val missing = PIECES.count { !fileOf(context, it).isFile }
        if (missing == PIECES.size) return State.Absent
        val wrong = PIECES.filter { fileOf(context, it).length() != it.size }
        return when {
            wrong.isEmpty() -> State.Ready
            else -> State.Broken(wrong.joinToString { it.name })
        }
    }

    /**
     * Scarica quello che manca, un pezzo per volta, e dice quanto ha fatto.
     *
     * ⚠️⚠️ **SI SCARICA IN UN FILE `.part` E SI RINOMINA ALLA FINE**, ed è la sola cosa che
     * impedisce il difetto peggiore di un download: una connessione caduta a metà lascia un
     * file del nome giusto e del peso sbagliato, e da lì in poi l'app crede di avere il
     * modello. Con il nome provvisorio, un file col nome buono **esiste solo se è completo e
     * verificato**.
     * ⚠️⚠️ **L'IMPRONTA SI CONTROLLA PRIMA DI RINOMINARE**: è il momento in cui costa nulla
     * (il file è appena passato in memoria) ed è l'unico in cui serve.
     * ⚠️ **Quello che c'è già non si riscarica**: riaccendendo l'interruttore dopo una caduta
     * si riprende dal pezzo mancante, non dagli 86 MB.
     * ⚠️ **L'annullamento è vero**: il ciclo controlla la coroutine a ogni blocco, quindi
     * spegnere l'interruttore ferma il traffico invece di lasciarlo correre in sottofondo.
     */
    suspend fun fetch(context: Context, onStep: (Long, Long) -> Unit): State =
        withContext(Dispatchers.IO) {
            val dir = home(context)
            if (!dir.isDirectory && !dir.mkdirs()) {
                return@withContext State.Broken("mkdir")
            }
            var done = PIECES.filter { fileOf(context, it).length() == it.size }.sumOf { it.size }
            for (piece in PIECES) {
                val target = fileOf(context, piece)
                if (target.length() == piece.size) continue
                val partial = File(dir, piece.name + ".part")
                val fatto = done
                // ⚠️⚠️ **`runCatching` INGOIEREBBE ANCHE L'ANNULLAMENTO**, ed è la trappola
                // già documentata in `Thumbs`: subito dopo si chiede alla coroutine se è
                // ancora viva, e quella rilancia. Senza, spegnere l'interruttore avrebbe
                // dato 'scaricamento fallito' invece di fermarsi in silenzio.
                val outcome = runCatching { pull(piece, partial) { onStep(fatto + it, WEIGHT) } }
                    .onFailure { partial.delete() }
                coroutineContext.ensureActive()
                val got = outcome.getOrNull()
                    ?: return@withContext State.Broken("${piece.name}: rete")
                if (got != piece.print) {
                    partial.delete()
                    return@withContext State.Broken("${piece.name}: impronta")
                }
                target.delete()
                if (!partial.renameTo(target)) {
                    return@withContext State.Broken("${piece.name}: rinomina")
                }
                done += piece.size
                onStep(done, WEIGHT)
            }
            state(context)
        }

    /** Toglie tutto: 86 MB non si tengono per una funzione spenta. */
    fun remove(context: Context) {
        home(context).listFiles()?.forEach { it.delete() }
    }

    /**
     * Scarica un pezzo e ne torna l'impronta, riferendo l'avanzamento.
     *
     * ⚠️ **L'impronta si calcola MENTRE si scrive**, non rileggendo il file dopo: sono 86 MB,
     * e leggerli due volte raddoppia l'unica parte lenta che non sia la rete.
     */
    private suspend fun pull(piece: Piece, into: File, onStep: (Long) -> Unit): String {
        val link = URL(BASE + piece.path)
        val connection = (link.openConnection() as HttpURLConnection).apply {
            connectTimeout = WAIT_MS
            readTimeout = WAIT_MS
            // ⚠️ I redirect si seguono: Hugging Face risponde 302 verso la sua rete di
            // distribuzione, quindi senza questo si scaricherebbe la paginetta del rimando.
            instanceFollowRedirects = true
        }
        val digest = MessageDigest.getInstance("SHA-256")
        var written = 0L
        connection.inputStream.use { source ->
            into.outputStream().use { sink ->
                val buffer = ByteArray(BLOCK)
                while (true) {
                    coroutineContext.ensureActive()
                    val read = source.read(buffer)
                    if (read < 0) break
                    sink.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    written += read
                    onStep(written)
                }
            }
        }
        connection.disconnect()
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Quanto si aspetta la rete: generoso, perché sono decine di megabyte. */
    private const val WAIT_MS = 30_000

    /** Il blocco di lettura: 64 KB, che è dove la copia smette di migliorare. */
    private const val BLOCK = 64 * 1024
}

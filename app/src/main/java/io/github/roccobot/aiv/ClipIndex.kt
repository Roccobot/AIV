package io.github.roccobot.aiv

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

/**
 * L'indice della ricerca per contenuto: un vettore per fotografia, su file.
 *
 * ⚠️⚠️ **512 BYTE PER FOTO, non 2048**: il vettore che esce dal modello è di 512 numeri in
 * virgola mobile, cioè 2 KB, e su diecimila fotografie sarebbero 20 MB da leggere a ogni
 * ricerca. Quantizzati a **int8** diventano 5 MB, e la classifica non cambia: la
 * quantizzazione del **confronto** è tutt'altra cosa dalla quantizzazione del **modello**, che
 * invece rompe tutto (vedi `ClipModels`).
 * ⚠️ **Il conto della perdita, dichiarato**: un numero fra -1 e 1 diventa un intero fra -127 e
 * 127, cioè un errore massimo di 1/254 per componente. Sui punteggi si vede alla terza cifra,
 * e i margini che separano una risposta giusta da una sbagliata sono di **un decimo**
 * (misurato: +0,1198 fra 'gatto' e 'cane' sulla stessa foto).
 *
 * ⚠️⚠️ **SI SCRIVE IN CODA E NON SI RISCRIVE**: l'indicizzazione va a pezzi mentre l'app è
 * aperta, e riscrivere l'intero file dopo ogni pezzo vorrebbe dire riscrivere cinque megabyte
 * duecento volte. Un file interrotto a metà record si ripara **leggendolo**: la lettura si
 * ferma al primo record incompleto e tiene quello che c'è.
 */
object ClipIndex {

    /** Quanti numeri ha un vettore. Letto dal modello: `image_embeds` ha forma `[batch, 512]`. */
    const val WIDTH = 512

    /** Il file dell'indice, accanto ai modelli. */
    fun file(context: Context): File = File(ClipModels.home(context), "index.bin")

    /**
     * Tutto l'indice in memoria, indirizzo per vettore.
     *
     * ⚠️ **Una `LinkedHashMap`**, perché l'ordine di inserimento è l'ordine in cui le foto
     * sono state indicizzate, e a parità di punteggio quello è l'ordine più sensato da
     * mostrare. ⚠️ **Cinque megabyte su diecimila foto**: sta in memoria senza discussioni, e
     * tenerlo aperto su file per risparmiarli vorrebbe dire una lettura per confronto.
     */
    suspend fun load(context: Context): LinkedHashMap<String, ByteArray> =
        withContext(Dispatchers.IO) {
            val out = LinkedHashMap<String, ByteArray>()
            val source = file(context)
            if (!source.isFile) return@withContext out
            runCatching {
                DataInputStream(source.inputStream().buffered()).use { input ->
                    if (input.readInt() != MAGIC) return@use
                    if (input.readInt() != FORM) return@use
                    while (true) {
                        // ⚠️ `readInt` solleva a fine file, ed è così che il ciclo finisce:
                        // un contatore in testa avrebbe voluto una riscrittura a ogni pezzo.
                        val length = input.readInt()
                        if (length <= 0 || length > 4096) break
                        val name = ByteArray(length)
                        input.readFully(name)
                        val code = ByteArray(WIDTH)
                        input.readFully(code)
                        out[String(name, Charsets.UTF_8)] = code
                    }
                }
            }
            out
        }

    /**
     * Aggiunge in coda quello che si è appena calcolato.
     *
     * ⚠️ **Apre e chiude a ogni pezzo**, non a ogni fotografia: aprire un file costa, e
     * tenerlo aperto per tutta l'indicizzazione vorrebbe dire un descrittore appeso a un
     * lavoro che può durare un quarto d'ora.
     */
    suspend fun append(context: Context, rows: List<Pair<String, ByteArray>>) {
        if (rows.isEmpty()) return
        withContext(Dispatchers.IO) {
            val target = file(context)
            val fresh = !target.isFile || target.length() < HEAD
            target.parentFile?.mkdirs()
            // ⚠️⚠️ **`append = !fresh`, e la forma sbagliata di questa riga cancellava
            // l'indice a ogni pezzo**: `target.outputStream()` apre TRONCANDO, quindi
            // costruirlo per poi scartarlo in favore di un flusso in coda avrebbe svuotato
            // il file prima ancora di scriverci. Un difetto che non dà errori: l'indice
            // resta grande come l'ultimo pezzo.
            DataOutputStream(java.io.FileOutputStream(target, !fresh).buffered()).use { out ->
                if (fresh) {
                    out.writeInt(MAGIC)
                    out.writeInt(FORM)
                }
                for ((uri, code) in rows) {
                    val name = uri.toByteArray(Charsets.UTF_8)
                    out.writeInt(name.size)
                    out.write(name)
                    out.write(code)
                }
            }
        }
    }

    /** Butta via l'indice: si rifà da capo. */
    fun clear(context: Context) {
        file(context).delete()
    }

    /**
     * Il vettore ridotto a 512 byte.
     *
     * ⚠️ **Si moltiplica per 127 e si arrotonda**, non per 128: 128 non ci sta in un byte con
     * segno, e un valore esattamente 1 diventerebbe -128, cioè il contrario di sé stesso. È
     * il classico errore che si vede solo sulle foto che somigliano moltissimo alla query.
     */
    fun pack(v: FloatArray): ByteArray = ByteArray(WIDTH) { i ->
        val x = if (i < v.size) v[i] else 0f
        Math.round(x * 127f).coerceIn(-127, 127).toByte()
    }

    /**
     * Quanto una foto indicizzata somiglia alla domanda.
     *
     * ⚠️ **La domanda resta in virgola mobile e la foto è quantizzata**: quantizzare anche la
     * domanda raddoppierebbe l'errore per guadagnare niente, perché la domanda è **una** e il
     * suo vettore lo si ha già in mano.
     */
    fun score(query: FloatArray, code: ByteArray): Float {
        var sum = 0f
        val n = minOf(query.size, code.size)
        for (i in 0 until n) sum += query[i] * code[i]
        return sum / 127f
    }

    /** Il marchio in testa al file: dice che è nostro. */
    private const val MAGIC = 0x41495643

    /** La forma del file: cambiando record, cambia questo, e i vecchi si ignorano. */
    private const val FORM = 1

    /** Quanto pesa l'intestazione: due interi. */
    private const val HEAD = 8L
}

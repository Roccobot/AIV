package io.github.roccobot.aiv

import java.text.Normalizer

/**
 * Il tokenizzatore di CLIP, quello che trasforma una frase nei numeri che l'encoder testuale
 * si aspetta.
 *
 * ⚠️⚠️ **È IL PEZZO CHE PUÒ FALLIRE IN SILENZIO, e per questo è il primo scritto e il solo
 * con una prova sua**: se sbaglia un token la ricerca non dà nessun errore, risponde
 * semplicemente a caso. Un encoder rotto si vede, un tokenizzatore rotto no.
 * ⚠️⚠️ **Verificato contro il riferimento** (la libreria `tokenizers` di HuggingFace) su 68
 * frasi scelte per fare male: accenti italiani, emoji, apostrofi, maiuscole, cifre, spazi
 * doppi, alfabeti non latini, stringa vuota. Chi lo tocca rifà quella prova, e la procedura
 * sta nel brief.
 *
 * ⚠️ **Non sa niente di JSON e non legge file**, ed è deliberato: riceve il vocabolario e le
 * fusioni già caricati. Così la stessa classe si prova su una JVM normale, senza Android
 * intorno, che è precisamente come è stata verificata.
 *
 * La catena, nell'ordine esatto in cui `tokenizer.json` la dichiara:
 * 1. **NFC**, spazi consecutivi ridotti a uno, tutto minuscolo;
 * 2. divisione con la **regola di CLIP** ([PATTERN]), che tiene i pezzi invece di scartarli;
 * 3. ogni pezzo passa dalla mappa **byte-verso-carattere** di GPT-2 ([byteToChar]);
 * 4. **BPE**: si parte dai singoli caratteri, l'ultimo porta `</w>`, e si fondono le coppie
 *    nell'ordine di priorità delle fusioni;
 * 5. in testa e in coda i due marcatori, [BOS] e [EOS].
 */
class ClipTokenizer(
    private val vocab: Map<String, Int>,
    merges: List<Pair<String, String>>
) {

    /**
     * Il rango di ogni fusione, cioè la sua priorità: più basso, prima si applica.
     *
     * ⚠️ **L'ordine del file È l'algoritmo**, non un dettaglio di serializzazione: il BPE a
     * ogni passo fonde la coppia col rango minore fra quelle presenti nella parola. Con le
     * fusioni in ordine sparso i token uscirebbero diversi, e uguali abbastanza da non
     * sembrare sbagliati.
     */
    private val ranks: Map<Pair<String, String>, Int> =
        merges.withIndex().associate { (at, pair) -> pair to at }

    /** L'id di `<|endoftext|>`, che in CLIP fa anche da token sconosciuto. */
    private val unknown: Int = vocab[EOS_TOKEN] ?: 0

    /**
     * La frase come la vuole l'encoder: esattamente [CONTEXT] numeri.
     *
     * ⚠️⚠️ **La lunghezza è FISSA e non c'è margine**: l'encoder ha l'embedding di posizione
     * di 77 posti, e con una sequenza più corta ONNX Runtime **solleva** invece di adattarsi
     * (`Attempting to broadcast an axis by a dimension other than 1`). Il riempimento è col
     * token **0**, che nel vocabolario di CLIP è `!` e che il tokenizzatore dichiara come
     * riempimento.
     * ⚠️ **Il troncamento conserva l'ultimo marcatore**: una frase lunghissima perde le
     * parole in fondo, non l'`<|endoftext|>`, che è quello da cui l'encoder legge il
     * risultato.
     */
    fun encode(text: String): IntArray {
        val ids = tokens(text)
        // ⚠️ Un `IntArray` nasce già tutto a zero e [PAD] **è** zero, quindi riempirlo a mano
        // era un giro in più (lint lo segnala, e ha ragione). Questa nota tiene il legame che
        // la scrittura esplicita rendeva visibile: chi un domani cambiasse [PAD] deve tornare
        // a riempire, perché da qui in poi lo zero non è più una coincidenza innocua.
        val out = IntArray(CONTEXT)
        val cut = minOf(ids.size, CONTEXT)
        for (at in 0 until cut) out[at] = ids[at]
        if (ids.size > CONTEXT) out[CONTEXT - 1] = vocab[EOS_TOKEN] ?: unknown
        return out
    }

    /** Gli id coi due marcatori ma senza riempimento: è la forma che la prova confronta. */
    fun tokens(text: String): List<Int> {
        val out = ArrayList<Int>(16)
        out += vocab[BOS_TOKEN] ?: unknown
        for (piece in PATTERN.findAll(clean(text)).map { it.value }) {
            for (part in bpe(encodeBytes(piece))) out += vocab[part] ?: unknown
        }
        out += vocab[EOS_TOKEN] ?: unknown
        return out
    }

    /**
     * NFC, spazi compattati, minuscolo: i tre normalizzatori del file, in quell'ordine.
     *
     * ⚠️ **NFC prima del minuscolo**, come sta scritto là: su una lettera accentata composta
     * in due modi diversi (`à` come un carattere o come `a` più il segno) l'ordine cambia il
     * risultato, e l'italiano ne è pieno.
     */
    private fun clean(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFC)
            .replace(SPACES, " ")
            .lowercase()

    /**
     * Da byte a caratteri visibili, la mappa di GPT-2 che CLIP eredita.
     *
     * ⚠️⚠️ **SERVE PROPRIO ALL'ITALIANO, e non è un dettaglio esotico**: `città` in UTF-8 ha
     * un carattere da due byte, e il vocabolario di CLIP non contiene byte ma **caratteri**.
     * Senza questo passaggio ogni parola accentata finirebbe nel token sconosciuto, cioè
     * ogni seconda parola di una ricerca in italiano.
     */
    private fun encodeBytes(piece: String): String {
        val sb = StringBuilder(piece.length + 4)
        for (b in piece.toByteArray(Charsets.UTF_8)) sb.append(byteToChar[b.toInt() and 0xFF])
        return sb.toString()
    }

    /**
     * Il cuore: fonde le coppie di una parola finché nessuna è più fondibile.
     *
     * ⚠️ **`</w>` sta sull'ultimo carattere dall'inizio**, non appiccicato alla fine del
     * risultato: è un simbolo che partecipa alle fusioni come gli altri, ed è così che il
     * vocabolario distingue `cat` dentro `catalogo` da `cat` come parola intera.
     * ⚠️ **Si fonde la coppia col rango MINORE fra quelle presenti**, e si rifà il giro da
     * capo: non si scorre la parola una volta sola applicando quello che capita, che darebbe
     * un risultato plausibile e diverso.
     */
    private fun bpe(word: String): List<String> {
        if (word.isEmpty()) return emptyList()
        val parts = ArrayList<String>(word.length)
        // ⚠️ Si itera sui PUNTI DI CODICE e non sui `Char`: un'emoji è due `Char` in Java, e
        // spezzarla a metà darebbe due simboli che non esistono nel vocabolario. Qui i pezzi
        // sono già passati dalla mappa dei byte, quindi in pratica sono ASCII, ma la mappa
        // produce anche caratteri sopra U+00FF e la regola vale comunque.
        var at = 0
        while (at < word.length) {
            val n = word.offsetByCodePoints(at, 1)
            parts += word.substring(at, n)
            at = n
        }
        parts[parts.size - 1] = parts[parts.size - 1] + WORD_END

        while (parts.size > 1) {
            var bestAt = -1
            var bestRank = Int.MAX_VALUE
            for (i in 0 until parts.size - 1) {
                val rank = ranks[parts[i] to parts[i + 1]] ?: continue
                if (rank < bestRank) {
                    bestRank = rank
                    bestAt = i
                }
            }
            if (bestAt < 0) break
            parts[bestAt] = parts[bestAt] + parts[bestAt + 1]
            parts.removeAt(bestAt + 1)
        }
        return parts
    }

    companion object {
        /** Quanti numeri vuole l'encoder testuale, sempre. Vedi [encode]. */
        const val CONTEXT = 77

        /** Il token di riempimento: `!` nel vocabolario di CLIP. */
        const val PAD = 0

        const val BOS_TOKEN = "<|startoftext|>"
        const val EOS_TOKEN = "<|endoftext|>"

        /** Il suffisso che marca la fine di una parola, dichiarato in `tokenizer.json`. */
        private const val WORD_END = "</w>"

        private val SPACES = Regex("\\s+")

        /**
         * La regola con cui CLIP spezza una frase, copiata dal `pre_tokenizer` del file.
         *
         * ⚠️ **Una cifra per volta** (`[\p{N}]` senza il più): `2026` diventa quattro pezzi,
         * non uno. Sembra un errore di trascrizione e non lo è.
         * ⚠️ Le sette forme contratte inglesi stanno **prima** delle lettere, o `'s`
         * finirebbe spezzato in apostrofo e lettera.
         */
        private val PATTERN = Regex("'s|'t|'re|'ve|'m|'ll|'d|[\\p{L}]+|[\\p{N}]|[^\\s\\p{L}\\p{N}]+")

        /**
         * La mappa byte-verso-carattere di GPT-2, costruita come la costruisce lui.
         *
         * ⚠️ **I byte stampabili restano sé stessi, gli altri vanno da U+0100 in su**, in
         * ordine di byte. È l'unico modo di avere un vocabolario di soli caratteri visibili
         * che sappia comunque rappresentare qualunque byte.
         */
        private val byteToChar: Array<String> = buildByteMap()

        private fun buildByteMap(): Array<String> {
            val printable = ArrayList<Int>(256)
            for (b in '!'.code..'~'.code) printable += b
            for (b in 0xA1..0xAC) printable += b
            for (b in 0xAE..0xFF) printable += b
            val out = arrayOfNulls<String>(256)
            for (b in printable) out[b] = b.toChar().toString()
            var next = 0x100
            for (b in 0..0xFF) {
                if (out[b] == null) {
                    out[b] = next.toChar().toString()
                    next++
                }
            }
            @Suppress("UNCHECKED_CAST")
            return out as Array<String>
        }
    }
}

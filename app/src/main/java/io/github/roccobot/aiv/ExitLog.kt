package io.github.roccobot.aiv

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build

/**
 * Perché il processo dell'app si è chiuso l'ultima volta, chiesto al sistema.
 *
 * ⚠️⚠️ **SERVE PERCHÉ UN ERRORE NEL CODICE NATIVO NON LASCIA NIENTE IN KOTLIN**: ONNX Runtime
 * è C++, e quando termina il processo (un `SIGSEGV`, un `abort`) non passa da nessun `catch`.
 * `ClipGuard` sa **a che punto** si era arrivati, perché è un file scritto da noi; che cosa sia
 * successo lo sa solo il sistema.
 *
 * ⚠️⚠️ **DALLA 11 (`API 30`) ANDROID TIENE UN REGISTRO**: `getHistoricalProcessExitReasons` dà
 * la ragione, il segnale e la memoria in uso all'istante della chiusura. Dalla 31 dà anche la
 * traccia del crash nativo, dove c'è il messaggio dell'errore.
 *
 * ⚠️ **Il testo NON si traduce**: è una diagnosi, come il messaggio di un'eccezione, e vive
 * nella stessa riga delle impostazioni dove già compare il `toString` di un errore Java.
 * Tradurre `REASON_CRASH_NATIVE` lo renderebbe irriconoscibile a chi deve cercarlo.
 *
 * ⚠️ **Sotto Android 11 il registro non esiste**, e allora non si dice niente invece di
 * inventare una spiegazione.
 */
object ExitLog {

    /**
     * Come si è chiuso l'ultimo processo, e `null` se si è chiuso in modo ordinario.
     *
     * ⚠️⚠️ **SOLO LE CHIUSURE ANORMALI**: chiudere l'app dal gestore, o lasciarla scaricare
     * dal sistema perché serviva memoria altrove, sono cose che succedono ogni giorno e non
     * spiegano niente. Rispondere anche per quelle vorrebbe dire attaccare una causa a un
     * segno della sicura che magari nasce da un falso positivo (vedi [ClipGuard]).
     * ⚠️ **Si guarda il record più recente e basta**: quello è la chiusura di cui la sicura
     * sta parlando, perché il segno lo si legge al primo avvio dopo.
     */
    fun lastExit(context: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val manager = context.getSystemService(ActivityManager::class.java) ?: return null
        val last = runCatching {
            manager.getHistoricalProcessExitReasons(context.packageName, 0, 1)
        }.getOrNull()?.firstOrNull() ?: return null
        val name = nameOf(last.reason) ?: return null
        val detail = last.description?.takeIf { it.isNotBlank() }
        // ⚠️ La memoria in uso è il secondo numero che conta: un errore nativo con mezzo
        // gigabyte in mano e uno con trenta megabyte non hanno la stessa causa.
        val room = last.pss.takeIf { it > 0 }?.let { " / rss " + formatBytes(it * 1024) }.orEmpty()
        val trace = trace(last)?.let { " / $it" }.orEmpty()
        return listOfNotNull("exit: $name", detail).joinToString(" / ") + room + trace
    }

    /**
     * Quello che si riesce a leggere nella traccia di un errore nativo.
     *
     * ⚠️⚠️ **QUELLO CHE CONTA È IL MESSAGGIO DI ABORT, non i nomi delle funzioni**, ed è la
     * correzione della `1.10`: un `SIGABRT` viene quasi sempre da un'eccezione C++ non
     * catturata, e in quel caso Android scrive nella traccia una riga come *terminating with
     * uncaught exception of type ...* seguita dal testo dell'errore. Quel testo è la
     * diagnosi; i nomi delle librerie caricate non lo sono.
     * ⚠️ **Nella `1.09` il filtro pescava anche i percorsi delle librerie mappate in memoria**
     * (`/apex/...`, `/data/app/...`), e siccome il tetto è di poche voci quelli si prendevano
     * il posto del messaggio. Adesso i percorsi si scartano e le voci si scelgono per
     * **utilità** invece che per ordine di comparsa.
     * ⚠️⚠️ **SI LEGGE A FORZA DI STRINGHE, e non decodificando il protobuf**: la traccia è un
     * messaggio `Tombstone`, e per leggerlo per bene servirebbero il suo schema e un
     * generatore di codice per estrarre due righe. Il testo dentro è ASCII, quindi si pesca
     * quello: è approssimativo, e il solo rischio è riportare **meno** di quello che c'era.
     * ⚠️ **La traccia esiste dalla 31**, e sotto non si dice niente.
     */
    private fun trace(last: ApplicationExitInfo): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        if (last.reason != ApplicationExitInfo.REASON_CRASH_NATIVE) return null
        val raw = runCatching {
            last.traceInputStream?.use { it.readNBytes(TRACE_CAP) }
        }.getOrNull() ?: return null
        val words = ascii(raw).filterNot { it.isPath() }.distinct()
        // ⚠️ L'ordine è quello dell'utilità: prima il perché, poi il segnale, poi chi.
        val out = words.filter { it.isMessage() }.take(MAX_MESSAGES) +
            words.filter { it.isSignal() }.take(1) +
            words.filter { it.isSymbol() }.take(MAX_SYMBOLS)
        return out.distinct().takeIf { it.isNotEmpty() }?.joinToString(" | ")
    }

    /** Le sequenze di testo leggibile dentro un blocco di byte. */
    private fun ascii(raw: ByteArray): List<String> {
        val out = ArrayList<String>()
        val word = StringBuilder()
        for (b in raw) {
            val c = b.toInt()
            if (c in 0x20..0x7E) {
                word.append(c.toChar())
            } else {
                if (word.length >= LEAST_WORD) out.add(word.toString())
                word.setLength(0)
            }
        }
        if (word.length >= LEAST_WORD) out.add(word.toString())
        return out
    }

    /** Il testo dell'errore: è la riga che dice davvero che cosa non ha funzionato. */
    private fun String.isMessage(): Boolean =
        contains("uncaught exception") ||
            contains("terminating with") ||
            contains("Abort message") ||
            contains("what():") ||
            contains("Non-zero status") ||
            contains("Status Message") ||
            contains("onnxruntime::")

    /** Il segnale, che dice di che genere di errore si tratta. */
    private fun String.isSignal(): Boolean = length <= 12 && startsWith("SIG")

    /** Il nome di una funzione fra quelle che in questo caso vale la pena riconoscere. */
    private fun String.isSymbol(): Boolean =
        contains("kai_") || contains("Mlas") || contains("Kleidi") || contains("Conv")

    /**
     * Un percorso di libreria mappata in memoria, che non spiega niente.
     *
     * ⚠️ Sono la parte più voluminosa di una traccia, e senza questo filtro riempiono da soli
     * il tetto delle voci.
     */
    private fun String.isPath(): Boolean =
        contains("/apex/") || contains("/data/app/") || contains("/system/")

    /** Quanti byte di traccia si leggono, e quante voci se ne tengono per tipo. */
    private const val TRACE_CAP = 512 * 1024
    private const val MAX_MESSAGES = 2
    private const val MAX_SYMBOLS = 3

    /** Sotto questa lunghezza una sequenza leggibile è rumore. */
    private const val LEAST_WORD = 6

    /**
     * Il nome della ragione, e `null` per le chiusure che non spiegano niente.
     *
     * ⚠️ **I nomi sono quelli di `ApplicationExitInfo`**, non una parafrasi: chi cerca
     * `REASON_CRASH_NATIVE` nella documentazione lo trova, mentre 'errore interno' no.
     */
    private fun nameOf(reason: Int): String? = when (reason) {
        ApplicationExitInfo.REASON_CRASH -> "CRASH"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
        ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
        ApplicationExitInfo.REASON_ANR -> "ANR"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "RESOURCE_USAGE"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INIT_FAILURE"
        else -> null
    }
}

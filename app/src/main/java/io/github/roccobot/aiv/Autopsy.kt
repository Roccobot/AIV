package io.github.roccobot.aiv

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build

/**
 * Perché il processo è morto l'ultima volta: la si chiede al sistema, che lo sa.
 *
 * ⚠️⚠️ **NASCE DA UN CRASH CHE NON SI RIESCE A DIAGNOSTICARE** (riscontro dell'utente sul
 * collaudo: *l'indicizzazione causa un crash sistematico*). Quello che uccide il processo è
 * **nativo**: ONNX Runtime è C++, e un suo `SIGSEGV` o un `abort` non passa da nessun `catch`
 * di Kotlin, quindi `ClipGuard` sa **dove** si era arrivati ma non **che cosa** è successo.
 * Leggendo il codice non si trova il difetto, e senza un fatto in mano la sola cosa possibile
 * sarebbe tirare a indovinare una correzione.
 *
 * ⚠️⚠️ **MA ANDROID IL FATTO CE L'HA**: dalla 11 (`API 30`) il sistema tiene un registro di
 * come sono morti i processi di ogni app, `getHistoricalProcessExitReasons`, e ci scrive la
 * ragione, il segnale, e quanta memoria era in uso nell'istante della morte. È quello che
 * trasforma 'l'app è sparita' in 'SIGSEGV con 486 MB in uso', cioè la differenza fra una
 * congettura e una misura.
 *
 * ⚠️ **Il testo NON si traduce, ed è una scelta**: è una diagnosi, come il messaggio di
 * un'eccezione, e vive nella stessa riga delle impostazioni dove già compare il `toString` di
 * un errore Java. Tradurre i nomi che dà il sistema li renderebbe irriconoscibili proprio a
 * chi deve cercarli.
 *
 * ⚠️ **Sotto Android 11 non si sa e si dice niente**: il registro non esiste, e inventare una
 * spiegazione sarebbe peggio di non darne.
 */
object Autopsy {

    /**
     * Come è morto l'ultimo processo di questa app, e `null` se è morto in modo ordinario.
     *
     * ⚠️⚠️ **SOLO LE MORTI ANORMALI**: chiudere l'app dal gestore, o lasciarla scaricare dal
     * sistema perché serviva memoria altrove, sono cose che succedono ogni giorno e che non
     * spiegano niente. Rispondere anche per quelle vorrebbe dire attaccare una causa a un
     * segno della sicura che magari nasce da un falso positivo (vedi [ClipGuard]), cioè
     * fabbricare la diagnosi invece di leggerla.
     * ⚠️ **Si guarda il record più recente e basta**: quello è la morte di cui la sicura sta
     * parlando, perché il segno lo si legge al primo avvio dopo. Scorrere più indietro
     * vorrebbe dire pescare crolli di giorni prima e attribuirli a questo giro.
     */
    fun lastDeath(context: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val manager = context.getSystemService(ActivityManager::class.java) ?: return null
        val last = runCatching {
            manager.getHistoricalProcessExitReasons(context.packageName, 0, 1)
        }.getOrNull()?.firstOrNull() ?: return null
        val name = nameOf(last.reason) ?: return null
        // ⚠️ La descrizione la scrive il sistema e porta il segnale (`Native crash: SIGSEGV`
        // e simili): è la riga che dice davvero che cosa è successo, e va riportata com'è.
        val detail = last.description?.takeIf { it.isNotBlank() }
        // ⚠️ La memoria in uso è il secondo numero che conta: un crollo nativo con mezzo
        // gigabyte in mano e uno con trenta megabyte non hanno la stessa causa.
        val room = last.pss.takeIf { it > 0 }?.let { " / rss " + formatBytes(it * 1024) }.orEmpty()
        val where = frames(last)?.let { " / $it" }.orEmpty()
        return listOfNotNull("exit: $name", detail).joinToString(" / ") + room + where
    }

    /**
     * I nomi che compaiono nella lapide di un crollo nativo: la libreria e la funzione.
     *
     * ⚠️⚠️ **È LA SOLA COSA CHE DICE *CHE COSA* È ESPLOSO, e non solo che è esploso**: la
     * ragione e il segnale dicono 'codice nativo', il che restringe a trentadue megabyte di
     * runtime. Un nome di funzione restringe a una riga.
     * ⚠️⚠️ **SI LEGGE A FORZA DI STRINGHE, e non decodificando il protobuf**: la lapide è un
     * messaggio `Tombstone` di Android, e per leggerlo per bene servirebbe il suo schema, cioè
     * una dipendenza e un generatore di codice per estrarre otto parole. I nomi dentro sono
     * testo ASCII, quindi si pescano quelli e basta: è approssimativo, e la sola cosa che
     * rischia è di riportare **meno** di quello che c'era.
     * ⚠️ **La lapide esiste dalla 31**, e sotto non si dice niente invece di inventare.
     * ⚠️ **Il tetto sui byte letti c'è apposta**: una lapide con tutte le mappe di memoria
     * arriva a qualche megabyte, e qui interessano le prime righe della pila.
     */
    private fun frames(last: ApplicationExitInfo): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        if (last.reason != ApplicationExitInfo.REASON_CRASH_NATIVE) return null
        val raw = runCatching {
            last.traceInputStream?.use { it.readNBytes(TOMB_CAP) }
        }.getOrNull() ?: return null
        val words = ascii(raw).filter { it.telling() }.distinct().take(TOMB_WORDS)
        return words.takeIf { it.isNotEmpty() }?.joinToString(" < ")
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

    /**
     * Se questa parola dice qualcosa su dove si è morti.
     *
     * ⚠️ **Un elenco di indizi e non 'tutto quello che è leggibile'**: una lapide è piena di
     * percorsi di sistema e di nomi di thread, e riportarli tutti vorrebbe dire una riga rossa
     * illeggibile che nasconde le tre parole che contano.
     */
    private fun String.telling(): Boolean =
        startsWith("SIG") ||
            endsWith(".so") ||
            contains("kai_") ||
            contains("Mlas") ||
            contains("Kleidi") ||
            contains("onnxruntime")

    /** Quanti byte di lapide si leggono, e quante parole se ne tengono. */
    private const val TOMB_CAP = 256 * 1024
    private const val TOMB_WORDS = 8

    /** Sotto questa lunghezza una sequenza leggibile è rumore. */
    private const val LEAST_WORD = 4

    /**
     * Il nome della ragione, e `null` per le morti che non spiegano niente.
     *
     * ⚠️ **I nomi sono quelli di `ApplicationExitInfo`**, non una parafrasi: chi cerca
     * `REASON_CRASH_NATIVE` nella documentazione lo trova, mentre 'crollo interno' no.
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

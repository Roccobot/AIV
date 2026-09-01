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
        return listOfNotNull("exit: $name", detail).joinToString(" / ") + room
    }

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

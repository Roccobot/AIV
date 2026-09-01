package io.github.roccobot.aiv

import android.content.Context
import android.os.Process
import java.io.File

/**
 * La sicura della ricerca per contenuto: un segno lasciato prima di toccare il motore.
 *
 * ⚠️⚠️ **NASCE DA UN DIFETTO CHE RENDEVA L'APP IRRECUPERABILE** (segnalato il 2026-08-31):
 * finito lo scaricamento dei modelli l'app si chiudeva, e da lì in poi si chiudeva **a ogni
 * avvio**, perché l'indicizzazione partiva da sola all'apertura e moriva sempre allo stesso
 * punto. L'unica via d'uscita era cancellare i dati dell'app dalle impostazioni di Android,
 * cioè perdere anche il cestino.
 *
 * ⚠️⚠️ **PERCHÉ UN FILE E NON UN `try`**: quello che uccide il processo è **nativo**. ONNX
 * Runtime è codice C++, e un suo `SIGSEGV` o un `abort` non passa da nessun `catch` di Kotlin:
 * il processo sparisce e basta. Un `runCatching` intorno alla chiamata dava la sensazione di
 * essere protetti e non proteggeva niente, ed è esattamente quello che c'era. L'unica cosa che
 * sopravvive a un processo ucciso è **il disco**: si scrive prima, si cancella dopo, e se al
 * giro seguente il segno c'è ancora vuol dire che in mezzo non si è tornati.
 *
 * ⚠️ **Il falso positivo è dichiarato e si accetta**: anche chiudere l'app dal gestore delle
 * applicazioni mentre indicizza lascia il segno, e la funzione si spegne pur non essendo
 * successo niente di male. Il prezzo è un tocco su 'Riprova'; il prezzo dell'errore opposto è
 * un'app che non si apre più.
 *
 * ⚠️ **Il segno porta anche il PERCHÉ, quando si riesce a saperlo**: un errore Kotlin lo si
 * cattura e lo si scrive qui, così la volta dopo la schermata delle impostazioni dice che cosa
 * è andato storto invece di dire soltanto che è andato storto. Un crollo nativo non scrive
 * niente, e allora resta la sola fase.
 */
object ClipGuard {

    /** Dove sta il segno: accanto ai modelli, così sparisce con loro. */
    private fun file(context: Context): File = File(ClipModels.home(context), "attempt")

    /**
     * Segna che si sta per entrare nel motore.
     *
     * @param phase in che punto si sta entrando, per ritrovarlo nel messaggio dopo.
     */
    fun arm(context: Context, phase: String) {
        val dir = ClipModels.home(context)
        if (!dir.isDirectory && !dir.mkdirs()) return
        runCatching { file(context).writeText(phase) }
    }

    /** Tutto è andato bene: il segno si toglie. */
    fun disarm(context: Context) {
        runCatching { file(context).delete() }
    }

    /**
     * L'errore che si è riusciti a catturare, scritto sopra al segno.
     *
     * ⚠️ Si scrive **senza** togliere il segno: un errore catturato non prova che il giro
     * dopo andrà bene, e la funzione resta ferma finché non lo dice l'utente.
     */
    fun note(context: Context, phase: String, why: String) {
        runCatching { file(context).writeText(phase + ": " + why.take(200)) }
    }

    /** Che cosa dice il segno, e `null` se non c'è: allora la strada è libera. */
    fun tripped(context: Context): String? {
        val f = file(context)
        if (!f.isFile) return null
        return runCatching { f.readText() }.getOrNull()?.ifBlank { "?" } ?: "?"
    }

    /** L'utente vuole riprovare: si toglie il segno e si ricomincia. */
    fun clear(context: Context) = disarm(context)

    /**
     * Mette in ascolto le eccezioni Java che nessuno prende, per tutta la vita del processo.
     *
     * ⚠️⚠️ **NASCE DA UN FATTO DELLA 1.10, E IL FATTO VA LETTO BENE**: con un filo solo il
     * processo non muore più dentro il codice nativo (`CRASH_NATIVE`) ma con un `CRASH`, cioè
     * un'eccezione **Java** non catturata. Ma il segno diceva `indice 0/1448 modello`, e quella
     * riga sta dentro **due** `runCatching` annidati (la fotografia in [ClipRun], l'intera
     * indicizzazione in chi la chiama): un'eccezione sollevata là verrebbe **scritta**, non
     * ucciderebbe niente. Quindi arriva da un **altro filo**, e nessun `try` messo in
     * [ClipRun] potrà mai vederla.
     * ⚠️⚠️ **QUESTO È L'UNICO PUNTO CHE LE VEDE TUTTE**: il gestore predefinito riceve le
     * eccezioni di **qualunque** filo, compresi quelli che una libreria apre per conto suo. È
     * la ragione per cui questa funzione esiste invece di allargare un `catch`.
     * ⚠️ **Si registra una volta sola**: `onCreate` torna a ogni rotazione dello schermo, e
     * senza il controllo si incatenerebbe un gestore sopra l'altro a ogni giro.
     * ⚠️ **Si tiene il contesto dell'APPLICAZIONE**, non l'attività: questo oggetto vive
     * quanto il processo, e trattenere un'attività vorrebbe dire tenerla in memoria per sempre.
     */
    fun watch(context: Context) {
        if (watching) return
        watching = true
        val app = context.applicationContext
        val before = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            blame(app, thread, error)
            /*
             * ⚠️⚠️ **LA PALLA TORNA A CHI C'ERA PRIMA, SEMPRE**: è quel gestore che scrive il
             * rapporto nel registro di sistema e che chiude il processo. Fermandosi qui, il
             * filo principale morirebbe lasciando la finestra ferma sullo schermo, che per chi
             * guarda è peggio di un'app che si chiude.
             */
            if (before != null) before.uncaughtException(thread, error)
            else Process.killProcess(Process.myPid())
        }
    }

    /**
     * Attacca al segno un'eccezione Java che ha chiuso l'app senza che nessuno la prendesse.
     *
     * ⚠️⚠️ **SI SCRIVE SOLO SE LA SICURA È ARMATA, e la condizione è tutto**: senza, un
     * qualunque difetto in un'altra parte dell'app finirebbe scritto qui, e la prossima
     * apertura darebbe la colpa alla ricerca per contenuto. Il segno c'è **solo** mentre si è
     * dentro il motore.
     * ⚠️ **E solo la prima volta**: se il segno porta già una spiegazione, quella è più vicina
     * alla causa di una seconda che arriva dopo.
     * ⚠️ **Deve poter girare mentre il processo sta morendo**, quindi non fa niente di
     * costoso: legge un file corto, ne scrive uno corto, e non solleva.
     */
    private fun blame(context: Context, thread: Thread, error: Throwable) {
        runCatching {
            val phase = tripped(context) ?: return
            if (phase.contains(MARK)) return
            file(context).writeText(phase + MARK + describe(thread, error))
        }
    }

    /**
     * L'eccezione in una riga: su che filo, che cos'è, che cosa dice, e da dove viene.
     *
     * ⚠️⚠️ **IL NOME DEL FILO STA PER PRIMO, e non è un dettaglio di cortesia**: è la sola
     * cosa che distingue un difetto sul filo dell'indicizzazione
     * (`DefaultDispatcher-worker-1`) da uno su un filo che una libreria si è aperta per conto
     * suo. Vedi [watch] per la ragione per cui la domanda si pone.
     * ⚠️ **La causa PRIMA di tutto**: una `RuntimeException` che ne avvolge un'altra dice
     * poco, e quella dentro dice tutto. Si scende fino in fondo alla catena.
     * ⚠️ **Tre righe di pila e non l'intera**: il primo fotogramma nel nostro codice o in
     * quello di ONNX Runtime basta a sapere dove guardare, e questa riga va letta su uno
     * schermo di telefono.
     */
    private fun describe(thread: Thread, error: Throwable): String {
        var deep: Throwable = error
        while (deep.cause != null && deep.cause !== deep) deep = deep.cause!!
        val where = deep.stackTrace.take(FRAMES).joinToString(" < ") {
            it.className.substringAfterLast('.') + "." + it.methodName
        }
        val what = deep.message?.take(WHY)?.replace('\n', ' ').orEmpty()
        return listOf("[" + thread.name + "]", deep.javaClass.name, what, where)
            .filter { it.isNotBlank() }
            .joinToString(": ")
    }

    /** Se il gestore è già stato messo: [watch] passa di qui a ogni rotazione. */
    private var watching = false

    /** Il separatore fra la fase e la spiegazione, che serve anche a non riscriverla. */
    private const val MARK = " -> "

    /** Quanti fotogrammi di pila e quanti caratteri di messaggio si tengono. */
    private const val FRAMES = 3
    private const val WHY = 240
}

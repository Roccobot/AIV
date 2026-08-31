package io.github.roccobot.aiv

import android.content.Context
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
}

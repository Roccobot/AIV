package io.github.roccobot.aiv

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * La cronologia dei ripristini: che cosa è tornato dov'era, e quando.
 *
 * ⚠️⚠️ **NASCE DALLA 0.76** (richiesta dell'utente, 2026-08-31, con la sua ragione: *talvolta
 * si fa un ripristina senza pensarci troppo e senza ricordare dove è stato ripristinato un
 * file*). Non è un registro di tutto quello che l'app fa: **solo** i ripristini, perché sono
 * gli unici che mettono un file in un posto che chi lo ha chiesto non ha scelto.
 *
 * ⚠️⚠️ **È UN SECONDO REGISTRO E NON UNA LETTURA DI QUELLO DEL CESTINO**, e la differenza è
 * sostanziale: l'archivio di [Bin] descrive quello che sta **dentro** il cestino, e la riga di
 * un file **sparisce** nel momento in cui lo si ripristina. Cioè proprio l'istante che questa
 * cronologia deve ricordare. Leggere là dentro darebbe l'elenco di quello che non è ancora
 * stato ripristinato, che è l'opposto.
 *
 * ⚠️ **Le righe scadono da sé dopo [DAYS] giorni, e non c'è un'impostazione** (scelta
 * dell'utente: *si svuota da sé ogni 7 giorni, non prorogabili*, dichiarato ragionevole con la
 * riserva di chiedere un valore più alto). La scadenza si applica **a ogni lettura e a ogni
 * scrittura**, non a un orario: un'app che non gira non ha nessun momento in cui fare le
 * pulizie, e legare la purga a un timer vorrebbe dire un servizio in più per cancellare
 * qualche riga di testo.
 *
 * ⚠️ **Il formato è lo stesso di [Bin]**, righe e colonne separate da tabulazione: nessuna
 * libreria, e un file che si legge a occhio se un giorno qualcosa non torna. ⚠️ E i percorsi
 * che ci finiscono **non possono** contenere i separatori, perché un file con una tabulazione
 * o un ritorno a capo nel nome non entra nel cestino (vedi `Bin.send`), quindi non può
 * nemmeno esserne ripristinato.
 */
object History {

    /**
     * Registra un ripristino: un istante, e i percorsi dove i file sono finiti.
     *
     * ⚠️ **L'istante arriva da fuori e non si prende qui**, ed è quello che rende un gruppo un
     * gruppo: tutti i file rimessi a posto dalla stessa operazione portano lo stesso numero, e
     * la schermata li raggruppa su quello. Preso dentro il ciclo, dieci file darebbero dieci
     * gruppi da un file.
     * ⚠️ **`NonCancellable`**, come le operazioni del cestino: se il ripristino è avvenuto, la
     * sua riga deve esistere, e una schermata che si chiude nel frattempo non è una ragione
     * per perderla.
     */
    suspend fun add(context: Context, at: Long, paths: List<String>) {
        if (paths.isEmpty()) return
        withContext(Dispatchers.IO + NonCancellable) {
            lock.withLock {
                val kept = alive(read(context), System.currentTimeMillis())
                write(context, kept + paths.map { Row(at, it) })
            }
        }
    }

    /**
     * La cronologia da mostrare: i gruppi, dal più recente.
     *
     * ⚠️ **Riscrive il file quando la purga toglie qualcosa**, e non si limita a filtrare in
     * lettura: senza, un archivio di un mese resterebbe sul disco per sempre a farsi filtrare
     * ogni volta. Con la purga scritta, aprire la schermata è anche il momento in cui il file
     * si accorcia.
     */
    suspend fun batches(context: Context): List<Batch> = withContext(Dispatchers.IO) {
        lock.withLock {
            val all = read(context)
            val kept = alive(all, System.currentTimeMillis())
            if (kept.size != all.size) write(context, kept)
            grouped(kept)
        }
    }

    /**
     * Le righe che non sono scadute.
     *
     * ⚠️ **Pura, come le funzioni di [Bin] che decidono un ordine**: prende righe e
     * restituisce righe, quindi si prova su una JVM normale senza Android intorno, ed è
     * precisamente così che è stata verificata.
     * ⚠️ **Anche le righe con un istante nel FUTURO restano**: capitano se l'orologio del
     * telefono va indietro (fuso, ora legale, un aggiornamento), e cancellare una riga perché
     * l'orologio è cambiato vorrebbe dire perdere il ripristino di un minuto prima.
     */
    fun alive(rows: List<Row>, now: Long): List<Row> = rows.filter { now - it.at < LIFE }

    /**
     * Le righe raggruppate per operazione, dalla più recente.
     *
     * ⚠️ **L'ordine dentro il gruppo è quello di scrittura**, cioè quello in cui i file sono
     * stati ripristinati, e non alfabetico: chi cerca 'dove è finita l'ultima' la trova in
     * fondo al suo gruppo, dove l'ha messa l'operazione.
     */
    fun grouped(rows: List<Row>): List<Batch> = rows.groupBy { it.at }
        .map { (at, its) -> Batch(at, its.map { it.path }) }
        .sortedByDescending { it.at }

    /** Una riga: quando è stato fatto il ripristino, e dove è finito il file. */
    class Row(val at: Long, val path: String)

    /** Un'operazione di ripristino: il suo istante, e i percorsi che ha rimesso a posto. */
    class Batch(val at: Long, val paths: List<String>)

    /**
     * Le righe di un archivio, saltando quelle rovinate.
     *
     * ⚠️ **Le righe malformate si scartano** invece di far fallire la lettura: un archivio
     * rovinato a metà deve far perdere le righe che descriveva, non tutte le altre. Stessa
     * scelta di `Bin.records`, e per la stessa ragione.
     */
    fun rows(text: String): List<Row> = text.lineSequence().mapNotNull { line ->
        val parts = line.split('\t')
        if (parts.size != 2) return@mapNotNull null
        val at = parts[0].toLongOrNull() ?: return@mapNotNull null
        if (parts[1].isBlank()) return@mapNotNull null
        Row(at, parts[1])
    }.toList()

    /** L'archivio come testo. L'inversa di [rows]. */
    fun text(rows: List<Row>): String = rows.joinToString("\n") { "${it.at}\t${it.path}" }

    private fun read(context: Context): List<Row> {
        val file = file(context)
        return rows(runCatching { if (file.isFile) file.readText() else "" }.getOrDefault(""))
    }

    private fun write(context: Context, rows: List<Row>) {
        runCatching { file(context).writeText(text(rows)) }
    }

    /**
     * L'archivio, **accanto** al cestino e non dentro.
     *
     * ⚠️ Dentro sarebbe un file in più che l'elenco del cestino dovrebbe filtrare, e prima o
     * poi comparirebbe fra le miniature come una fotografia rotta. È la stessa ragione, e lo
     * stesso posto, dell'archivio delle provenienze.
     */
    private fun file(context: Context): File = File(Bin.dir(context).parentFile, FILE)

    private const val FILE = "restored.tsv"

    /** Quanti giorni vive una riga. Vedi la nota in testa: non è un'impostazione. */
    const val DAYS = 7
    private const val LIFE = DAYS * 24L * 60 * 60 * 1000

    /** ⚠️ Le due operazioni non si sovrappongono: leggono e riscrivono lo stesso archivio. */
    private val lock = Mutex()
}

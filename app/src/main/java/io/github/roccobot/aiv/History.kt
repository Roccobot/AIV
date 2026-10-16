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
 * Registra **solo** i ripristini (richiesta dell'utente, 0.76): sono le sole operazioni che
 * mettono un file in un posto che chi le chiede non ha scelto.
 *
 * ⚠️ **È un secondo registro, non una lettura di quello del cestino**: l'archivio di [Bin]
 * descrive quello che è dentro il cestino, e la riga di un file sparisce proprio nel momento in
 * cui lo si ripristina, cioè l'istante che questa cronologia deve ricordare.
 *
 * Le righe scadono da sé dopo [DAYS] giorni, senza un'impostazione (scelta dell'utente). La
 * scadenza si applica a ogni lettura e a ogni scrittura: un'app che non gira non ha un orario in
 * cui fare pulizia, e un timer vorrebbe dire un servizio in più.
 *
 * Il formato è quello di [Bin], righe e colonne separate da tabulazione: nessuna libreria, e un
 * file che si legge a occhio. I percorsi non possono contenere i separatori, perché un file con
 * una tabulazione o un ritorno a capo nel nome non entra nel cestino (`Bin.send`).
 */
object History {

    /**
     * Registra un ripristino: un istante, e i percorsi dove i file sono finiti.
     *
     * ⚠️ **L'istante arriva dal chiamante**: tutti i file della stessa operazione hanno lo
     * stesso numero, e la schermata li raggruppa su quello. Preso dentro il ciclo, dieci file
     * darebbero dieci gruppi da un file.
     * `NonCancellable`, come le operazioni del cestino: un ripristino avvenuto deve avere la sua
     * riga anche se la schermata si chiude nel frattempo.
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
     * Riscrive il file quando la purga toglie qualcosa: così aprire la schermata è anche il
     * momento in cui l'archivio si accorcia, e non resta sul disco a farsi filtrare ogni volta.
     */
    suspend fun batches(context: Context): List<Batch> = withContext(Dispatchers.IO) {
        lock.withLock {
            val all = read(context)
            val kept = alive(all, System.currentTimeMillis())
            if (kept.size != all.size) write(context, kept)
            grouped(kept.filterNot { MediaFiles.ignored(it.path) })
        }
    }

    /**
     * Le righe vive, per il backup del cestino.
     *
     * Viaggia col cestino: dice dove è finito un file uscito dal cestino, e da sola sarebbe una
     * lista di percorsi senza contesto.
     */
    internal suspend fun snapshot(context: Context): List<Row> = withContext(Dispatchers.IO) {
        lock.withLock { alive(read(context), System.currentTimeMillis()) }
    }

    /**
     * Aggiunge le righe di un backup a quelle che ci sono.
     *
     * Si aggiunge, come il cestino: le righe di adesso raccontano ripristini veri, e
     * un'importazione non li deve cancellare. Una riga identica (stesso istante, stesso percorso)
     * entra una volta sola, e le scadute escono come sempre.
     */
    internal suspend fun merge(context: Context, rows: List<Row>) {
        if (rows.isEmpty()) return
        withContext(Dispatchers.IO + NonCancellable) {
            lock.withLock {
                val now = System.currentTimeMillis()
                val tutte = alive(read(context), now) + alive(rows, now)
                write(context, tutte.distinctBy { it.at to it.path })
            }
        }
    }

    /**
     * Le righe che non sono scadute.
     *
     * Pura, come le funzioni di [Bin] che decidono un ordine: si prova su una JVM senza Android.
     * ⚠️ **Le righe con un istante nel futuro restano**: capitano se l'orologio del telefono va
     * indietro (fuso, ora legale, un aggiornamento), e cancellarle farebbe perdere il ripristino
     * di un minuto prima.
     */
    fun alive(rows: List<Row>, now: Long): List<Row> = rows.filter { now - it.at < LIFE }

    /**
     * Le righe raggruppate per operazione, dalla più recente.
     *
     * L'ordine dentro il gruppo è quello di scrittura, cioè quello del ripristino: l'ultimo file
     * rimesso a posto è in fondo al suo gruppo.
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
     * Le righe malformate si scartano e la lettura prosegue: un archivio rovinato a metà perde
     * solo le righe rovinate. Stessa scelta di `Bin.records`.
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

    /** Riscrive l'archivio con [AtomicWrite], come quello del cestino (rilievo C25). */
    private fun write(context: Context, rows: List<Row>) {
        AtomicWrite.text(file(context), text(rows))
    }

    /**
     * L'archivio, accanto al cestino: dentro sarebbe un file in più da filtrare nell'elenco del
     * cestino, e comparirebbe fra le miniature come un'immagine rotta. Stesso posto, e stessa
     * ragione, dell'archivio delle provenienze.
     */
    private fun file(context: Context): File = File(Bin.dir(context).parentFile, FILE)

    private const val FILE = "restored.tsv"

    /** Quanti giorni vive una riga. Vedi la nota in testa: non è un'impostazione. */
    const val DAYS = 7
    private const val LIFE = DAYS * 24L * 60 * 60 * 1000

    /** Le operazioni non si sovrappongono: leggono e riscrivono lo stesso archivio. */
    private val lock = Mutex()
}

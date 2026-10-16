package io.github.roccobot.aiv

import android.net.Uri
import java.time.Instant
import java.time.ZoneId

/**
 * Il Feed: tutte le immagini e i video delle cartelle visibili in un solo flusso, dal più recente,
 * raggruppati per mese (scelte C1 e D1 dell'utente, 6.02), in tutte e due le varianti.
 *
 * ⚠️ **L'ordine è una funzione pura delle righe**, e il MediaStore si limita a fornirle
 * ([Folder.feed]): il banco di prova controlla ordine e mesi senza un telefono.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 */
object Feed {

    /**
     * Una riga come la dà il MediaStore.
     *
     * @param taken il momento dello scatto, in millisecondi, o `null` quando il file non lo dice
     *   (spesso le schermate, i download e le copie modificate).
     * @param modified l'ultima scrittura del file, in **secondi**, come `DATE_MODIFIED`.
     * @param dir la cartella del file, per [shown], o `null` quando la riga non la dice.
     */
    data class Row(
        val uri: Uri,
        val id: Long,
        val taken: Long?,
        val modified: Long,
        val dir: String? = null
    )

    /** Un mese del flusso: la posizione del suo primo elemento in [Page.items]. */
    data class Month(val year: Int, val month: Int, val start: Int)

    /** Il flusso, in ordine, e dove comincia ogni mese. */
    data class Page(val items: List<Uri>, val months: List<Month>) {
        val series: Folder.Series get() = Folder.Series(items, 0)
    }

    /**
     * Il momento su cui si ordina una riga, in millisecondi.
     *
     * ⚠️ **[FeedDate.TAKEN] ripiega sulla data di modifica** (scelta dell'utente): una schermata
     * non ha data di scatto, e senza il ripiego finirebbe in fondo al flusso, al 1970.
     * Una data di scatto pari a zero o negativa vale come assente: alcune app scrivono 0.
     */
    fun moment(row: Row, date: FeedDate): Long = when (date) {
        FeedDate.TAKEN -> row.taken?.takeIf { it > 0 } ?: (row.modified * 1000)
        FeedDate.MODIFIED -> row.modified * 1000
    }

    /**
     * Le righe delle cartelle che la griglia mostra: lo stesso filtro di
     * [FolderSelection.visible] della griglia e della ricerca (scelta D1 dell'utente).
     *
     * [peeking] fa entrare le cartelle nascoste: finché 'Mostra nascoste' è attivo le loro
     * immagini sono nel flusso, come le cartelle sono nella griglia (richiesta dell'utente, 6.03).
     */
    fun shown(rows: List<Row>, selection: FolderSelection, peeking: Boolean): List<Row> =
        rows.filter { selection.visible(it.dir, peeking) }

    /**
     * Le righe nell'ordine del flusso, dalla più recente, e i mesi in cui cadono, letti in [zone].
     *
     * A parità di momento vince l'id più alto, l'ultimo indicizzato, come in una cartella: due
     * scatti dello stesso secondo tengono lo stesso ordine da una lettura all'altra.
     */
    fun order(rows: List<Row>, date: FeedDate, zone: ZoneId = ZoneId.systemDefault()): Page {
        val sorted = rows.sortedWith(
            compareByDescending<Row> { moment(it, date) }.thenByDescending { it.id }
        )
        val months = mutableListOf<Month>()
        sorted.forEachIndexed { i, row ->
            val day = Instant.ofEpochMilli(moment(row, date)).atZone(zone)
            val last = months.lastOrNull()
            if (last == null || last.year != day.year || last.month != day.monthValue) {
                months += Month(day.year, day.monthValue, i)
            }
        }
        return Page(sorted.map { it.uri }, months)
    }
}

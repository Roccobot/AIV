package io.github.roccobot.aiv

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Il modo unico di riscrivere un file che deve restare intero: o il contenuto nuovo per intero,
 * o quello vecchio com'era.
 *
 * ⚠️ **Si scrive accanto e poi si rinomina sopra**: scrivere direttamente sul file lo tronca a
 * zero prima di scrivere, quindi un processo chiuso a metà o una memoria piena lasciano un file
 * vuoto o tagliato col nome giusto; per l'archivio del cestino vuol dire perdere la provenienza
 * di tutti i file. Sullo stesso volume `renameTo` sostituisce il file in un passo solo.
 * Il file accanto comincia col punto, così in una cartella dell'utente non compare fra le
 * immagini per l'istante in cui esiste.
 * `sync` prima di rinominare: senza, dopo uno spegnimento il nome nuovo può puntare a byte che
 * non sono ancora sul disco. Costa poco, perché qui passano file piccoli.
 *
 * Rocco Casadei, a.k.a. Roccobot
 */
object AtomicWrite {

    /**
     * Scrive [target] con quello che [body] mette nel flusso, e risponde se è andata.
     *
     * ⚠️ [body] **non chiude il flusso**: lo chiude questa funzione, dopo `sync`. Un errore
     * dentro [body] lascia [target] com'era e cancella il file accanto.
     */
    fun write(target: File, body: (OutputStream) -> Unit): Boolean {
        val parent = target.absoluteFile.parentFile ?: return false
        val part = File(parent, ".${target.name}.part")
        val ok = runCatching {
            parent.mkdirs()
            FileOutputStream(part).use { out ->
                body(out)
                out.flush()
                out.fd.sync()
            }
            part.renameTo(target)
        }.getOrDefault(false)
        if (!ok) runCatching { part.delete() }
        return ok
    }

    /** [write] con dei byte già pronti. */
    fun bytes(target: File, bytes: ByteArray): Boolean = write(target) { it.write(bytes) }

    /** [write] con un testo, in UTF-8 come `File.writeText`. */
    fun text(target: File, text: String): Boolean = bytes(target, text.toByteArray(Charsets.UTF_8))
}

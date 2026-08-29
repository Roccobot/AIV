package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import java.io.File
import java.security.MessageDigest

/**
 * I byte delle immagini prese dalla rete, tenuti da parte.
 *
 * ⚠️⚠️ **ESISTE PERCHÉ UN'IMMAGINE DEL WEB SI PAGAVA A OGNI SGUARDO.** Le foto del
 * telefono si riaprono in un istante perché il file è già lì; un indirizzo diretto
 * invece ripartiva dalla connessione ogni volta, anche tornando indietro sulla stessa
 * immagine di dieci secondi prima. È la differenza che rendeva l'online la parte
 * trascurata dell'app, ed è quello che questo file toglie.
 *
 * ⚠️ **NON è la cache di Coil, e le due non si uniscono.** Quella serve le miniature
 * delle foto locali e ha il disco spento apposta (i file sono già sul telefono, vedi
 * `Thumbs`). Questa tiene i byte di ciò che sul telefono **non c'è**. Chi le unificasse
 * spegnerebbe l'una o accenderebbe l'altra dove non serve.
 *
 * ⚠️ **Sta in `cacheDir` e non in una cartella dell'app**, ed è la scelta corretta anche
 * quando fa perdere qualcosa: quello che c'è dentro si rifà scaricandolo, quindi il
 * sistema può cancellarlo quando lo spazio scarseggia, e l'utente lo svuota da
 * 'Cancella cache' senza toccare le impostazioni. Una cartella nostra chiederebbe di
 * gestire a mano una cosa che il sistema gestisce meglio.
 */
object RemoteCache {

    /**
     * Il tetto di UN file, e il file più grande del tetto semplicemente non si tiene.
     *
     * ⚠️ Senza questo limite un'immagine sola potrebbe riempire tutta la cache e buttare
     * fuori le venti che c'erano: il caso peggiore diventerebbe 'ho guardato un
     * panorama da 40 MB e ho perso tutto il resto'. Il tetto del trasferimento resta
     * quello di `ImageSource`, molto più alto: qui si decide che cosa **conservare**,
     * non che cosa si può aprire.
     */
    private const val MAX_ENTRY_BYTES = 16L * 1024 * 1024

    /** Quanto occupa in tutto, prima che le più vecchie escano. */
    private const val MAX_TOTAL_BYTES = 96L * 1024 * 1024

    /**
     * I byte tenuti per questo indirizzo, o null.
     *
     * ⚠️ La lettura **tocca** il file, e non è un dettaglio: la data di modifica è
     * l'unico ordinamento che questa cache ha, quindi senza quel tocco la sfoltitura
     * butterebbe fuori l'immagine guardata più spesso solo perché è stata scaricata per
     * prima.
     */
    fun read(context: Context, uri: Uri): ByteArray? {
        val file = fileFor(context, uri)
        if (!file.isFile) return null
        return runCatching {
            val bytes = file.readBytes()
            file.setLastModified(System.currentTimeMillis())
            bytes
        }.getOrNull()
    }

    /**
     * Tiene da parte i byte di questo indirizzo, se ci stanno.
     *
     * ⚠️ Si scrive su un file temporaneo e poi si rinomina: un'app chiusa a metà
     * scrittura lascerebbe altrimenti un file **troncato** con il nome giusto, cioè una
     * cache che serve immagini rotte, che è peggio di una cache vuota.
     */
    fun write(context: Context, uri: Uri, bytes: ByteArray) {
        if (bytes.size > MAX_ENTRY_BYTES) return
        val file = fileFor(context, uri)
        runCatching {
            file.parentFile?.mkdirs()
            val temporary = File(file.parentFile, "${file.name}.part")
            temporary.writeBytes(bytes)
            if (!temporary.renameTo(file)) temporary.delete()
            trim(file.parentFile)
        }
    }

    /**
     * Le più vecchie escono finché il totale non rientra.
     *
     * ⚠️ Si sfoltisce **dopo** aver scritto e non prima: prima vorrebbe dire indovinare
     * quanto occuperà la scrittura, e sbagliare la stima per difetto lascia la cartella
     * sopra il tetto fino al giro successivo.
     */
    private fun trim(directory: File?) {
        val files = directory?.listFiles()?.filter { it.isFile } ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_TOTAL_BYTES) return
        for (file in files.sortedBy { it.lastModified() }) {
            if (total <= MAX_TOTAL_BYTES) return
            val size = file.length()
            if (file.delete()) total -= size
        }
    }

    /**
     * Il nome del file per un indirizzo.
     *
     * ⚠️⚠️ **SHA-256 dell'indirizzo INTERO, query compresa**, e nessuna parte di quel
     * testo finisce nel nome: un indirizzo può contenere un gettone di autenticazione, e
     * un nome di file leggibile lo scriverebbe in chiaro in una cartella che altre app
     * col permesso giusto possono elencare. Il digest risolve insieme questo e i
     * caratteri che un filesystem non accetta.
     * ⚠️ **La query fa parte della chiave**: su moltissimi siti è lei a dire *quale*
     * immagine è, e troncarla farebbe servire una foto al posto di un'altra.
     */
    private fun fileFor(context: Context, uri: Uri): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(uri.toString().toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(File(context.cacheDir, "remote"), digest)
    }
}

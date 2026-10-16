package io.github.roccobot.aiv

import android.content.Context
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

/**
 * La scrittura sicura della `6.26` (rilievi E12, B21, B36, C9 e C25 della revisione della 6.25):
 * un file che si riscrive resta intero, e il cestino non perde le provenienze.
 *
 * ⚠️⚠️ **LA SCRITTURA FALLITA SI SIMULA CON UNA CARTELLA AL POSTO DEL FILE ACCANTO**: dove
 * [AtomicWrite] vorrebbe aprire `.nome.part` c'è una cartella con quel nome, quindi l'apertura
 * fallisce come fallirebbe con la memoria piena, e il codice che si prova è quello vero.
 * ⚠️ **Vive in una classe sua per la stessa ragione di `CestinoSpazioTest`**: il cestino è nella
 * cartella esterna dell'app, che con `OmbraArchivio` non si raggiunge.
 */
@RunWith(AndroidJUnit4::class)
class ScritturaSicuraTest {

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /** L'archivio del cestino, e il posto del suo file accanto. */
    private val archivio: File get() = File(Bin.dir(app).parentFile, "bin.tsv")
    private val accanto: File get() = File(archivio.parentFile, ".bin.tsv.part")

    @After
    fun pulisci() {
        accanto.deleteRecursively()
        runBlocking { Bin.empty(app) }
    }

    /**
     * **Un errore a metà scrittura lascia il file com'era, e nessun file accanto.**
     *
     * ⚠️ **Controprovata** scrivendo direttamente su `target` in [AtomicWrite.write]: il file
     * resta con la sola metà scritta e il primo confronto cade.
     */
    @Test
    fun `una scrittura interrotta lascia il file com'era`() {
        val cartella = File(app.cacheDir, "scrittura-sicura").also { it.mkdirs() }
        val file = File(cartella, "dati.txt").also { it.writeText("vecchio contenuto") }

        val esito = AtomicWrite.write(file) {
            it.write("metà".toByteArray())
            throw IOException("memoria piena")
        }

        assertFalse(esito)
        assertEquals("vecchio contenuto", file.readText())
        assertFalse("il file accanto doveva sparire", File(cartella, ".dati.txt.part").exists())

        assertTrue(AtomicWrite.text(file, "nuovo"))
        assertEquals("nuovo", file.readText())
    }

    /**
     * **Senza l'archivio scritto, i file mandati nel cestino tornano a casa** e l'esito li conta
     * come falliti.
     *
     * ⚠️ **Controprovata** ignorando l'esito di `write` in `Bin.send`: il file resta nel cestino
     * senza la sua riga, e l'esito dice che è andata.
     */
    @Test
    fun `senza archivio i file non restano nel cestino`() {
        runBlocking { Bin.empty(app) }
        val casa = File(app.cacheDir, "casa-cestino").also { it.mkdirs() }
        val foto = File(casa, "foto.jpg").also { it.writeBytes(ByteArray(100)) }
        Bin.dir(app).mkdirs()
        accanto.mkdirs()

        val esito = runBlocking { Bin.send(app, listOf(foto.toUri())) }

        assertEquals(0, esito.outcome.done)
        assertEquals(1, esito.outcome.failed)
        assertTrue("la foto doveva tornare a casa", foto.isFile)
        assertTrue("il cestino doveva restare vuoto", Bin.dir(app).listFiles().orEmpty().none { it.isFile })
    }

    /**
     * **Senza l'archivio scritto, la copia di sicurezza non c'è**, così l'editor non salva sopra
     * un originale che non potrebbe più tornare.
     *
     * ⚠️ **Controprovata** ignorando l'esito di `write` in `Bin.keep`: torna la copia.
     */
    @Test
    fun `senza archivio la copia di sicurezza non c'è`() {
        runBlocking { Bin.empty(app) }
        val casa = File(app.cacheDir, "casa-copia").also { it.mkdirs() }
        val foto = File(casa, "foto.jpg").also { it.writeBytes(ByteArray(100)) }
        Bin.dir(app).mkdirs()
        accanto.mkdirs()

        val copia = runBlocking { Bin.keep(app, foto) }

        assertNull(copia)
        assertTrue(foto.isFile)
        assertTrue("il cestino doveva restare vuoto", Bin.dir(app).listFiles().orEmpty().none { it.isFile })
    }
}

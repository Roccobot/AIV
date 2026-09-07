package io.github.roccobot.aiv

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Disfare una copia e disfare uno spostamento: le due operazioni inverse della `1.83`.
 *
 * ⚠️⚠️ **NASCE COL LAVORO E NON DOPO UN DIFETTO** (campo libero del giro della `1.82`, punto B:
 * *aggiungi degli 'Annulla' temporizzati (avvisi in basso) anche per le operazioni di copia e
 * spostamento*), e la ragione è quella che `AIV/CLAUDE.md` § '🧪 Quando si scrive una prova, e
 * quando no' dà per il cestino: questa funzione **cancella file** e ne **sposta** altri, quindi
 * un difetto qui non si vede e non si può disfare a sua volta.
 * ⚠️ **Gira con Robolectric per il solo `Context`**: `FileTree.revert` avvisa il MediaStore dei
 * file toccati, e senza un contesto non si può chiamare. Quello che si misura invece è tutto sul
 * disco, in una cartella temporanea vera.
 */
@RunWith(RobolectricTestRunner::class)
class DisfareTest {

    @get:Rule
    val cartella = TemporaryFolder()

    private val context get() = ApplicationProvider.getApplicationContext<android.app.Application>()

    private fun file(dove: File, nome: String, testo: String = "x"): File =
        File(dove, nome).apply { writeText(testo) }

    @Test
    fun `disfare una copia cancella quello che era stato scritto`() {
        val dentro = cartella.newFolder("dentro")
        val copia = file(dentro, "foto.jpg")
        val out = runBlocking {
            FileTree.revert(context, listOf(FileTree.Undoable(made = copia.absolutePath)))
        }
        assertEquals(1, out.done)
        assertEquals(0, out.failed)
        assertFalse("la copia doveva sparire", copia.exists())
    }

    @Test
    fun `disfare uno spostamento riporta il file da dove veniva`() {
        val da = cartella.newFolder("da")
        val a = cartella.newFolder("a")
        val spostato = file(a, "foto.jpg", "contenuto")
        val casa = File(da, "foto.jpg")
        val out = runBlocking {
            FileTree.revert(
                context,
                listOf(FileTree.Undoable(made = spostato.absolutePath, from = casa.absolutePath))
            )
        }
        assertEquals(1, out.done)
        assertFalse("non deve restare nella destinazione", spostato.exists())
        assertTrue("deve essere tornato a casa", casa.exists())
        assertEquals("contenuto", casa.readText())
    }

    /**
     * ⚠️⚠️ **È IL CASO CHE CONTA, e senza di lui l'offerta direbbe che qualcosa è andato
     * storto**: fra l'operazione e il tocco su 'Annulla' passano dei secondi, e in quei secondi
     * la copia può essere già stata cancellata da un'altra app. Il risultato voluto (quel file
     * non c'è) è esattamente quello che si è ottenuto.
     */
    @Test
    fun `un file gia sparito non conta come fallito`() {
        val dentro = cartella.newFolder("vuota")
        val mai = File(dentro, "mai-esistita.jpg")
        val out = runBlocking {
            FileTree.revert(context, listOf(FileTree.Undoable(made = mai.absolutePath)))
        }
        assertEquals(1, out.done)
        assertEquals(0, out.failed)
    }

    /**
     * ⚠️ **Il ritorno non sovrascrive**: se nel frattempo è arrivato un file con lo stesso nome,
     * quello resta dov'è e il ritorno prende un nome libero. L'alternativa sarebbe cancellare
     * qualcosa che nessuno ha chiesto di toccare.
     */
    @Test
    fun `se a casa e arrivato un altro file, il ritorno non lo sovrascrive`() {
        val da = cartella.newFolder("origine")
        val a = cartella.newFolder("altrove")
        val intruso = file(da, "foto.jpg", "sono arrivato dopo")
        val spostato = file(a, "foto.jpg", "quello di prima")
        val out = runBlocking {
            FileTree.revert(
                context,
                listOf(
                    FileTree.Undoable(made = spostato.absolutePath, from = intruso.absolutePath)
                )
            )
        }
        assertEquals(1, out.done)
        assertEquals("sono arrivato dopo", intruso.readText())
        val tornato = File(da, "foto (2).jpg")
        assertTrue("il ritorno prende un nome libero", tornato.exists())
        assertEquals("quello di prima", tornato.readText())
    }
}

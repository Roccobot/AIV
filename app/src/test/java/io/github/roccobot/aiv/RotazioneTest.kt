package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.runtime.saveable.SaverScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Il banco di prova di quello che deve **sopravvivere a una rotazione**.
 *
 * ⚠️⚠️ **NASCE DAI DIFETTI CHE SONO ARRIVATI A LUI** (tappa 'La rotazione, e il visualizzatore
 * che non si azzera più': *ruotando si chiude la finestra aperta e con lei quello che stavi
 * scrivendo, e la selezione della griglia si scioglie*), come prescrive `AIV/CLAUDE.md`
 * § '🧪 Quando si scrive una prova, e quando no'.
 *
 * ⚠️⚠️ **PROVA I SALVATORI E NON LA ROTAZIONE, e la ragione è che il difetto vive là**: girare il
 * telefono ricrea l'attività, e quello che passa dall'altra parte è esattamente ciò che i
 * salvatori scrivono nel `Bundle`. Un salvatore che perde un dato o che ne inventa uno è il
 * difetto, e si misura andata e ritorno.
 * ⚠️ **Quello che il banco NON vede**: la rotazione vera, cioè che il sistema chiami davvero il
 * salvataggio e che la composizione si riattacchi. Quello lo dice il telefono, e la voce di
 * collaudo lo chiede con un gesto solo (girare il telefono con una finestra aperta e trenta
 * miniature spuntate).
 */
@RunWith(AndroidJUnit4::class)
class RotazioneTest {

    /** Un ambito che accetta tutto: qui si prova il contenuto, non che il `Bundle` lo regga. */
    private val ambito = SaverScope { true }

    private val uno = Uri.parse("content://media/external/images/media/11")
    private val due = Uri.parse("content://media/external/images/media/22")

    /*
     * ⚠️ **`save` vuole DUE receiver e non uno**: è un membro di `Saver` dichiarato come
     * estensione di `SaverScope`, quindi la chiamata passa da due `with` annidati. Scritto una
     * volta qui invece che in ognuno dei casi.
     */
    private fun salva(job: FileJob?): Any? = with(FileJobSaver) { with(ambito) { save(job) } }

    private fun salva(scelti: Set<Uri>): Any? =
        with(UriSetSaver) { with(ambito) { save(scelti) } }

    private fun giroCompleto(job: FileJob?): FileJob? {
        val salvato = salva(job) ?: return null
        return FileJobSaver.restore(salvato)
    }

    /**
     * **Caso 1: le finestre tornano indietro identiche.**
     *
     * ⚠️ Il flag di 'Sposta' si prova in tutti e due i versi: salvato al contrario, una copia
     * diventerebbe uno spostamento, cioè un file che sparisce da dove stava.
     */
    @Test
    fun `le finestre attraversano la rotazione`() {
        val uris = listOf(uno, due)

        val sposta = giroCompleto(FileJob.Transfer(uris, move = true))
        assertTrue("Lo spostamento non è tornato tale", sposta is FileJob.Transfer)
        assertEquals("Lo spostamento è diventato una copia", true, (sposta as FileJob.Transfer).move)
        assertEquals("Gli indirizzi non sono tornati", uris, sposta.uris)

        val copia = giroCompleto(FileJob.Transfer(uris, move = false))
        assertEquals("La copia è diventata uno spostamento", false, (copia as FileJob.Transfer).move)

        val rinomina = giroCompleto(FileJob.Rename(uris))
        assertTrue("La rinomina non è tornata tale", rinomina is FileJob.Rename)
        assertEquals("Gli indirizzi della rinomina non sono tornati", uris, rinomina!!.uris)

        val schede = giroCompleto(FileJob.Facts(uris))
        assertTrue("La scheda delle informazioni non è tornata tale", schede is FileJob.Facts)

        val perSempre = giroCompleto(FileJob.Delete(uris, forGood = true))
        assertTrue("La conferma di eliminazione non è tornata tale", perSempre is FileJob.Delete)
        assertEquals(
            "L'eliminazione definitiva è tornata come un giro di cestino",
            true,
            (perSempre as FileJob.Delete).forGood
        )
    }

    /**
     * **Caso 2: i lavori che partono da sé NON tornano indietro.**
     *
     * ⚠️⚠️ **È LA CLAUSOLA CHE EVITA DI RIFARE L'OPERAZIONE**: ripristino, duplicazione e
     * eliminazione col cestino attivo non hanno una finestra da riaprire e partono da un effetto
     * sulla chiave del lavoro. Rimessi in scena dopo la rotazione, farebbero partire l'operazione
     * una seconda volta: un file duplicato due volte, o due giri di cestino.
     */
    @Test
    fun `i lavori che partono da se non si ripristinano`() {
        val uris = listOf(uno)
        for (job in listOf(
            FileJob.Restore(uris),
            FileJob.Duplicate(uris),
            FileJob.Delete(uris, forGood = false)
        )) {
            assertNull(
                "Un lavoro che parte da sé è stato salvato: la rotazione lo rifarebbe",
                salva(job)
            )
        }
    }

    /**
     * **Caso 3: niente resta niente.**
     *
     * Senza questo, un salvatore che inventasse un lavoro dal nulla riaprirebbe una finestra che
     * nessuno aveva aperto.
     */
    @Test
    fun `nessun lavoro resta nessun lavoro`() {
        assertNull("Dal niente è uscito un lavoro", salva(null as FileJob?))
    }

    /**
     * **Caso 4: la selezione attraversa la rotazione.**
     *
     * ⚠️ L'insieme torna insieme, e con gli stessi indirizzi: la lista di passaggio è un
     * dettaglio del `Bundle`, non un cambio di forma del dato.
     */
    @Test
    fun `la selezione attraversa la rotazione`() {
        val scelti = setOf(uno, due)
        val salvato = salva(scelti)
        assertTrue("La selezione non è stata salvata", salvato != null)
        assertEquals("La selezione non è tornata", scelti, UriSetSaver.restore(salvato!!))
    }

    /**
     * **Caso 5: una selezione vuota non diventa una selezione.**
     *
     * L'altra metà del caso 4: un insieme vuoto non deve tornare come un elemento fantasma, e la
     * griglia deve ritrovarsi fuori dalla selezione com'era.
     */
    @Test
    fun `una selezione vuota resta vuota`() {
        val salvato = salva(emptySet<Uri>())
        val tornato = salvato?.let { UriSetSaver.restore(it) } ?: emptySet()
        assertTrue("Una selezione vuota è tornata piena: $tornato", tornato.isEmpty())
    }
}

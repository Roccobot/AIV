package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.geometry.Offset
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

    /**
     * **Caso 6: l'ingrandimento attraversa la rotazione, e quello che passa è un RAPPORTO.**
     *
     * ⚠️⚠️ **È IL DIFETTO CHE È ARRIVATO A LUI DUE VOLTE** (voce `zoom-rotazione`, non approvata
     * nel giro della `1.80` e in quello della `1.81`: *è come prima*), quindi torna qui con la
     * prova, come prescrive `AIV/CLAUDE.md` § '🧪 Quando si scrive una prova, e quando no'.
     * ⚠️ **I due riposi sono diversi di proposito**: ruotando la vista cambia forma, quindi la
     * scala che mostrava l'immagine intera prima non la mostra intera dopo. Se la prova usasse lo
     * stesso riposo, passerebbe anche conservando la scala, cioè col difetto rimesso.
     */
    @Test
    fun `l ingrandimento attraversa la rotazione`() {
        val ritratto = 0.5f
        val paesaggio = 0.8f
        val larghezza = 4000f
        val altezza = 3000f

        val tenuto = ZoomKeep(1f, 0f, 0f)
        // Ingrandito il doppio del riposo, guardando un punto spostato di un decimo di immagine.
        val scala = 2f * ritratto
        val scostamento = Offset(-0.1f * larghezza * scala, 0.05f * altezza * scala)
        tenuto.record(scala, scostamento, ritratto, larghezza, altezza)

        val salvato = with(ZoomKeep.Saver) { with(ambito) { save(tenuto) } }
        assertTrue("L'ingrandimento non è stato salvato", salvato != null)
        val dopo = ZoomKeep.Saver.restore(salvato!!)!!

        assertEquals("Il rapporto non è tornato", 2f, dopo.zoom, 0.0001f)
        assertEquals(
            "La scala non si è adattata al riposo nuovo",
            2f * paesaggio,
            dopo.scaleFor(paesaggio),
            0.0001f
        )
        // Il difetto vero, detto al rovescio: con la scala conservata invece del rapporto, qui si
        // leggerebbe il riposo nudo e l'immagine si riaprirebbe a schermo pieno.
        assertTrue(
            "Ruotando l'ingrandimento è tornato a riposo",
            dopo.scaleFor(paesaggio) > paesaggio * 1.5f
        )

        val nuovaScala = dopo.scaleFor(paesaggio)
        val nuovo = dopo.offsetFor(nuovaScala, larghezza, altezza)
        assertEquals(
            "Il punto inquadrato è scivolato in orizzontale",
            0.1f,
            -nuovo.x / (nuovaScala * larghezza),
            0.0001f
        )
        assertEquals(
            "Il punto inquadrato è scivolato in verticale",
            -0.05f,
            -nuovo.y / (nuovaScala * altezza),
            0.0001f
        )
    }

    /**
     * **Caso 7: un'immagine a riposo resta a riposo.**
     *
     * L'altra metà del caso 6: il rapporto di partenza vale `1`, quindi dopo una rotazione la
     * scala è esattamente il riposo nuovo e il centro è il centro. Senza questo, un ripristino che
     * sbagliasse il segno o l'unità aprirebbe **ogni** immagine spostata.
     */
    @Test
    fun `un immagine a riposo attraversa la rotazione senza muoversi`() {
        val tenuto = ZoomKeep(1f, 0f, 0f)
        tenuto.record(0.5f, Offset.Zero, 0.5f, 4000f, 3000f)
        val dopo = ZoomKeep.Saver.restore(with(ZoomKeep.Saver) { with(ambito) { save(tenuto) } }!!)!!
        assertEquals("Il riposo non è più riposo", 0.8f, dopo.scaleFor(0.8f), 0.0001f)
        /*
         * ⚠️ **Le due componenti si confrontano con una tolleranza e non l'oggetto intero**: un
         * centro a zero moltiplicato per meno uno dà **meno zero**, e `Offset` confronta i bit,
         * quindi `Offset(-0.0, 0.0)` non è uguale a `Offset.Zero` mentre è lo stesso punto. La
         * prova era scritta sull'oggetto e diceva 'il centro si è spostato' mostrando due volte
         * `Offset(0.0, 0.0)`, cioè accusava il codice di un difetto che non c'era.
         */
        val fermo = dopo.offsetFor(0.8f, 4000f, 3000f)
        assertEquals("Il centro si è spostato in orizzontale", 0f, fermo.x, 0.0001f)
        assertEquals("Il centro si è spostato in verticale", 0f, fermo.y, 0.0001f)
    }

    /**
     * **Caso 8: una misura non ancora presa non scrive niente.**
     *
     * ⚠️ Nel fotogramma in cui la vista è misurata ma l'immagine no, il riposo vale zero: senza la
     * guardia il rapporto diventerebbe `NaN`, e un `NaN` conservato non si corregge più da sé.
     */
    @Test
    fun `una misura a zero non sporca l ingrandimento`() {
        val tenuto = ZoomKeep(2f, 0.1f, 0.1f)
        tenuto.record(0f, Offset.Zero, 0f, 0f, 0f)
        assertEquals("Il rapporto è stato sporcato", 2f, tenuto.zoom, 0.0001f)
        assertEquals("Il centro è stato sporcato", 0.1f, tenuto.centreX, 0.0001f)
    }
}

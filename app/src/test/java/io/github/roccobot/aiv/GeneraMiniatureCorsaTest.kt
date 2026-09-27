package io.github.roccobot.aiv

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Una generazione delle miniature dall'inizio alla fine, su una collezione vuota.
 *
 * ⚠️⚠️ **VIVE IN UNA CLASSE SUA PER L'OMBRA**: [Warmup] chiede l'elenco a `Folder.everything`, che
 * passa da `Folder.granted`, e sul banco quella domanda muore dentro il metodo di sistema senza
 * [OmbraArchivio]. L'ombra risponde 'permesso non concesso', quindi l'elenco è vuoto e il giro
 * arriva in fondo subito, che è quello che serve per misurare la parte del giro che non dipende dai
 * file: la pagina che compare, se ne va, e dice com'è andata. Il perché l'ombra non si mette nella
 * classe accanto vive su di lei.
 *
 * ⚠️ **Il giro vero è su un'altra corsia**, cioè sui fili di I/O, e la frase finale passa dal filo
 * principale, che sul banco gira solo quando lo si fa girare: per questo i casi aspettano con
 * [aspetta] invece di guardare una volta sola.
 * ⚠️ **Senza il permesso la frase dice che è andata bene**, ed è dichiarato: senza il permesso
 * l'app non mostra nessuna cartella, quindi quel tasto non lo raggiunge nessuno che non l'abbia
 * già concesso.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [OmbraArchivio::class])
class GeneraMiniatureCorsaTest {

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /*
     * ⚠️ **Il canale degli avvisi si azzera, e la generazione si ferma**: sono due oggetti di
     * processo, e quello che una prova lascia in scena entrerebbe nella prova dopo.
     */
    @Before
    fun prepara() = azzera()

    @After
    fun ripulisci() {
        Warmup.cancel()
        azzera()
    }

    private fun azzera() {
        Notices.line?.let { Notices.dismiss(it.id) }
    }

    /**
     * **La pagina compare nell'istante del tocco, e alla fine se ne va dicendo che è andata bene.**
     *
     * ⚠️ **Nell'istante del tocco, e non quando il lavoro parte**: fra il tocco su 'Sì' e il primo
     * file passa il conto delle immagini, che su una collezione grande dura, e senza la pagina il
     * tocco sembrerebbe non aver fatto niente.
     * ⚠️ **Controprovata togliendo la chiusura in fondo al lavoro**: la pagina resta in scena, e
     * cade l'asserzione sulla fine.
     * ⚠️⚠️ **LA PRIMA ASSERZIONE NON HA UNA CONTROPROVA CHE VALGA, E SI DICHIARA**: con la prima
     * scrittura della pagina spostata dentro il lavoro, l'esito dipende da quale dei due fili arriva
     * prima, cioè sarebbe una prova rossa a volte. Resta perché dice quello che la pagina promette.
     */
    @Test
    fun `la generazione compare subito e alla fine lo dice`() {
        val prima = Warmup.finished
        Warmup.start(app, emptySet())
        assertNotNull("la pagina doveva comparire nell'istante del tocco", Warmup.run)

        aspetta("la generazione non è arrivata in fondo") { Warmup.finished > prima && Notices.line != null }

        assertNull("finita la generazione, la pagina doveva andarsene", Warmup.run)
        assertEquals(app.getString(R.string.settings_thumbs_gen_done), Notices.line?.text)
    }

    /**
     * **'Annulla' toglie la pagina nell'istante del tocco, e alla fine non dice niente.**
     *
     * ⚠️ **Nell'istante, e non quando il lavoro se ne accorge**: le miniature già chieste finiscono
     * il loro giro, e una pagina che restasse fino ad allora direbbe che il tocco non è arrivato.
     * ⚠️ **La frase finale è di chi arriva in fondo**: un annullamento l'ha chiesto chi guarda.
     * ⚠️ **Controprovata togliendo lo spegnimento della pagina da [Warmup.cancel]**: la pagina
     * resta in scena e cade la prima asserzione. ⚠️ **Con quel difetto cade anche il caso qui
     * sopra, quando gira dopo**, e non è un secondo difetto: con una pagina in scena
     * [Warmup.start] non fa niente, quindi la generazione non parte.
     */
    @Test
    fun `Annulla toglie la pagina subito e non dice niente`() {
        Warmup.start(app, emptySet())
        Warmup.cancel()
        assertNull("'Annulla' doveva togliere la pagina subito", Warmup.run)

        repeat(30) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }

        assertNull("dopo 'Annulla' la pagina non deve tornare", Warmup.run)
        assertNull("dopo 'Annulla' non si dice niente", Notices.line)
    }

    /**
     * Fa girare il filo principale finché [fatto] non risponde di sì, per al più tre secondi.
     *
     * ⚠️ **Il filo principale va fatto girare a mano**: sul banco è fermo, e la frase finale della
     * generazione gli arriva come un compito da eseguire.
     */
    private fun aspetta(perche: String, fatto: () -> Boolean) {
        repeat(300) {
            shadowOf(Looper.getMainLooper()).idle()
            if (fatto()) return
            Thread.sleep(10)
        }
        fail(perche)
    }
}

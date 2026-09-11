package io.github.roccobot.aiv

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Il canale con cui l'app dice com'è andata, e la riga che ne esce.
 *
 * ⚠️⚠️ **NASCE COL LAVORO E NON DOPO UN DIFETTO** (sua risposta `casa` a `d-avvisi`), e la
 * ragione è quella di `AIV/CLAUDE.md` § '🧪 Quando si scrive una prova, e quando no': questo
 * canale è **uno per tutta l'app**, quindi un suo difetto non si vede in una schermata sola, e
 * quello che sostituisce (diciassette avvisi di sistema) funzionava.
 * ⚠️⚠️ **E DUE DI QUESTI CASI SONO CORSE, cioè la classe di difetto che non si riproduce
 * guardando il codice**: una riga che scade mentre ne arriva un'altra, e una schermata che esce
 * di scena mentre la sua riga è ancora viva. Là il rimedio è l'identificatore, e senza una prova
 * un ritocco che lo togliesse passerebbe verde.
 */
@RunWith(AndroidJUnit4::class)
class AvvisiTest {

    @get:Rule
    val banco = createComposeRule()

    /*
     * ⚠️ **Il canale è un oggetto di processo, quindi si azzera prima e dopo ogni caso**: senza,
     * una riga lasciata in scena da una prova entrerebbe in quella dopo, e l'ordine dei casi
     * cambierebbe l'esito. È il prezzo dichiarato di uno stato condiviso da tutta l'app.
     */
    @Before
    fun pulisci() = azzera()

    @After
    fun ripulisci() = azzera()

    /*
     * ⚠️ **Si toglie per identificatore anche qui**, che è la sola via che il canale offre: un
     * congedo incondizionato esisterebbe soltanto per queste prove, e un'API di sola prova nel
     * codice di produzione è una porta che prima o poi qualcuno usa.
     */
    private fun azzera() {
        Notices.line?.let { Notices.dismiss(it.id) }
    }

    /*
     * ⚠️⚠️ **IL TEMPO NON AVANZA DA SÉ, E SENZA QUESTA RIGA LE DUE PROVE DELL'ALBERO SONO
     * IMPOSSIBILI**: con l'avanzamento automatico `waitForIdle` porta a termine ogni attesa
     * pendente, e le attese pendenti qui sono la vita della notifica e la riga che si consuma.
     * Cioè il banco farebbe **scadere** il messaggio prima di poterlo toccare, e il caso
     * fallirebbe per una ragione che non è quella che misura. Misurato: con l'avanzamento
     * automatico lo stato risponde già `null` alla prima asserzione.
     * ⚠️⚠️ **E LA RIGA SI METTE PRIMA DI MONTARE LA SCENA, che è l'altra metà della stessa
     * trappola**: [Notices] è un oggetto di **processo**, e una sua scrittura fatta dopo
     * `setContent` non arriva alla composizione finché lo snapshot non viene propagato, cosa che
     * col clock fermo non succede da sé. Misurato con una spia nell'albero: la composizione
     * continuava a leggere `null` mentre lo stato portava già il messaggio. Mettendola prima, la
     * prima composizione la trova, che è anche il caso vero (una notifica nasce da un gesto
     * fatto in una schermata che è già in scena).
     */
    private fun inScena() {
        banco.mainClock.autoAdvance = false
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box { AppNotice(Notices.line) }
            }
        }
    }

    @Test
    fun `una riga si mette e si toglie`() {
        val id = Notices.say("copiato")
        assertEquals("copiato", Notices.line?.text)
        Notices.dismiss(id)
        assertNull("la riga doveva sparire", Notices.line)
    }

    /**
     * ⚠️⚠️ **È LA CORSA CHE L'IDENTIFICATORE ESISTE PER CHIUDERE**: fra il momento in cui una
     * riga scade e quello in cui la sua attesa se ne accorge può esserne arrivata un'altra, e un
     * congedo cieco porterebbe via il messaggio nuovo. Con `clear()` al posto di `dismiss(id)`
     * questo caso diventa rosso, che è esattamente quello che deve fare.
     */
    @Test
    fun `un congedo vecchio non porta via la riga nuova`() {
        val primo = Notices.say("prima")
        Notices.say("dopo")
        Notices.dismiss(primo)
        assertEquals("dopo", Notices.line?.text)
    }

    /**
     * ⚠️⚠️ **QUESTO CASO SOSTITUISCE UNA RIGA DI CODICE, e va saputo**: fino alla `1.83` la
     * griglia spegneva a mano l'offerta di disfare prima di mostrare la propria notifica, perché
     * le due superfici avevano lo stesso posto in fondo allo schermo e si coprivano. Adesso il
     * canale è uno, quindi la riga nuova prende il posto della vecchia **e ne chiude il congedo**:
     * chi aspettava la fine di quella riga (l'offerta di disfare, in `Undo`) lo sa lo stesso.
     */
    @Test
    fun `una riga nuova chiude quella che trova`() {
        var chiusa = false
        Notices.offer(text = "eliminati", action = "Annulla", onGone = { chiusa = true }) { }
        assertFalse("non deve chiudersi da sola", chiusa)
        Notices.say("selezione azzerata")
        assertTrue("la riga di prima doveva essere congedata", chiusa)
        assertEquals("selezione azzerata", Notices.line?.text)
    }

    @Test
    fun `il congedo di una riga scaduta scatta una volta sola`() {
        var quante = 0
        val id = Notices.offer(text = "spostati", action = "Annulla", onGone = { quante++ }) { }
        Notices.dismiss(id)
        Notices.dismiss(id)
        assertEquals(1, quante)
    }

    /**
     * ⚠️ **La riga si vede davvero**, e non solo nello stato: la superficie la disegna
     * [AppNotice], e una prova che guardasse il solo oggetto passerebbe anche se nessuno la
     * mostrasse più.
     */
    @Test
    fun `la riga in scena compare nell'albero`() {
        Notices.say("immagine copiata")
        inScena()
        banco.onNodeWithText("immagine copiata").assertExists()
    }

    /**
     * ⚠️⚠️ **IL TASTO TOGLIE LA RIGA PRIMA DI FARE IL LAVORO**, e il caso misura l'ordine: quello
     * che il tasto fa può aprire una schermata o durare qualche istante, e una notifica che
     * restasse in scena mentre il suo effetto è già in corso si farebbe toccare due volte. Al
     * momento dell'azione la riga deve essere già sparita.
     */
    @Test
    fun `il tasto toglie la riga e poi esegue`() {
        var vistaDurante: Notices.Line? = null
        var fatto = false
        Notices.offer(text = "eliminati", action = "Annulla") {
            vistaDurante = Notices.line
            fatto = true
        }
        inScena()
        banco.onNodeWithText("Annulla").performClick()
        assertTrue("il tasto non ha fatto niente", fatto)
        assertNull("la riga doveva essere già sparita", vistaDurante)
    }

    /**
     * ⚠️ **Due messaggi uguali di fila sono due messaggi**, ed è la ragione per cui la riga porta
     * un identificatore: senza, il secondo non farebbe ripartire né l'entrata né il conto alla
     * rovescia, e scadrebbe quando scadeva il primo. Il precedente sono due salvataggi di fila,
     * che dicono la stessa frase.
     */
    @Test
    fun `due messaggi uguali sono due righe`() {
        val primo = Notices.say("Salvato")
        val secondo = Notices.say("Salvato")
        assertNotNull(Notices.line)
        assertTrue("il secondo deve avere un identificatore suo", secondo != primo)
        assertEquals(secondo, Notices.line?.id)
    }

    /**
     * **Con la scheda della selezione in scena, la notifica le resta sopra e non la copre.**
     *
     * ⚠️⚠️ **È IL PUNTO C DEL CAMPO LIBERO** (*in alcune circostanze (es. si inizia una selezione
     * dopo un 'copia', 'sposta' o 'elimina'), la bottomsheet della selezione va a finire sotto la
     * notifica in basso*), e il banco lo vede perché è una questione di **posizione**, cioè di
     * struttura: dove finisce una superficie rispetto a un'altra.
     * ⚠️ **Il confronto è col tasto e non col bordo della scheda**, che nell'albero non ha un
     * nodo suo: se la notifica copre i comandi il difetto c'è, e il bordo della scheda sta ancora
     * più in alto del tasto, quindi la misura è più stretta del vero.
     * ⚠️ **Controprovata** togliendo `abovePickSheet()` dalla notifica: il suo bordo di sotto
     * finisce in fondo allo schermo, cioè sotto i tasti, e il caso cade.
     */
    @Test
    fun `la notifica sale sopra la scheda della selezione`() {
        Notices.say("1 elemento copiato")
        banco.mainClock.autoAdvance = false
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize()) {
                    PickSheet(
                        visible = true,
                        actions = listOf(
                            PadAction(
                                key = PadKey.COPY,
                                icon = Icons.Outlined.Info,
                                label = R.string.menu_copy_here
                            ) { }
                        )
                    )
                    AppNotice(
                        Notices.line,
                        modifier = Modifier.align(Alignment.BottomCenter).abovePickSheet()
                    )
                }
            }
        }
        banco.waitForIdle()
        // ⚠️ La scheda entra con un'animazione, quindi il posto vero ce l'ha a corsa finita: col
        // clock fermo il tempo lo si fa passare a mano, o si misura un pannello ancora in viaggio.
        banco.mainClock.advanceTimeBy(1_000)
        banco.waitForIdle()

        val notifica = banco.onNodeWithText("1 elemento copiato").getBoundsInRoot()
        val tasto = banco.onNodeWithText(
            ApplicationProvider.getApplicationContext<Context>().getString(R.string.menu_copy_here)
        ).getBoundsInRoot()
        assertTrue(
            "la notifica (fino a ${notifica.bottom}) copre i tasti (da ${tasto.top})",
            notifica.bottom <= tasto.top
        )
    }
}

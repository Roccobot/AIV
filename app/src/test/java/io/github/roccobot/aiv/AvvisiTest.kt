package io.github.roccobot.aiv

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
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
     * ⚠️ **Controprovata** togliendo `aboveFoot()` dalla notifica: il suo bordo di sotto
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
                        modifier = Modifier.align(Alignment.BottomCenter).aboveFoot()
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

    /**
     * **Col FAB in scena la notifica si stringe accanto a lui, da tutte e due le parti.**
     *
     * ⚠️⚠️ **È IL PUNTO A1 DEL CAMPO LIBERO DEL GIRO DELLA `2.23`** (*la notifica inferiore con
     * 'Annulla' (es. per 'Sposta') a volte va sopra il FAB (su qualunque lato sia)*), letto fino in
     * fondo col riscontro del giro della `2.24` (*non si potrebbe fare lo stesso avviso meno largo
     * di quel tanto che basta a stare a fianco del FAB?*, e la risposta **`stringe`**). La `2.24` lo
     * faceva salire, ed è la misura che questo caso ha sostituito.
     * ⚠️⚠️ **SI GUARDANO TUTTI E DUE I LATI, ED È SUA LA RAGIONE** (*'di fianco' ha un significato
     * di default e un altro se il FAB è a sinistra*): il lato lo decide il FAB misurando dov'è, e
     * una prova su un lato solo passerebbe anche con la parte fissa scritta a mano.
     * ⚠️ **Il riquadro del FAB si prende dal nodo che lo avvolge e non dal suo glifo**: quello che
     * il tasto annuncia è un'icona da 24dp centrata in 56, quindi misurandola il caso passerebbe
     * anche con la notifica addosso al bordo del tasto.
     * ⚠️⚠️ **E LO STESSO VALE PER LA NOTIFICA, CHE SI MISURA DAL SUO NODO E NON DAL TESTO: LO HA
     * DETTO LA CONTROPROVA.** La prima stesura guardava `onNodeWithText`, cioè la frase, che dentro
     * la sua superficie finisce ben prima del bordo: col rientro tolto a mano il caso del FAB a
     * destra **restava verde**, perché là fra la fine del testo e il tasto c'è lo spazio della
     * superficie vuota. È il caso generale scritto in `AIV/CLAUDE.md` § '🧪 Quando si scrive una
     * prova, e quando no': una prova che non si vede fallire col difetto rimesso non misura niente.
     * ⚠️ **Controprovata** togliendo il rientro da `aboveFoot()`: la notifica resta larga quanto lo
     * schermo e prende il FAB sotto di sé, in tutti e due i casi.
     */
    @Test
    fun `la notifica si stringe accanto al FAB a destra`() = accantoAlFab(destra = true)

    /** Vedi il caso qui sopra: è lo stesso, con il FAB dall'altra parte. */
    @Test
    fun `la notifica si stringe accanto al FAB a sinistra`() = accantoAlFab(destra = false)

    /**
     * La scena dei due casi qui sopra: un FAB in un angolo in fondo e la notifica di casa.
     *
     * ⚠️ **La misura è orizzontale e non verticale**: quello che si vuole è che i due non si
     * tocchino **restando** tutti e due in fondo, quindi si guarda dove finisce la notifica dal
     * lato del tasto.
     */
    private fun accantoAlFab(destra: Boolean) {
        Notices.say("1 elemento spostato")
        banco.mainClock.autoAdvance = false
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize()) {
                    val dove = if (destra) Alignment.BottomEnd else Alignment.BottomStart
                    Box(modifier = Modifier.align(dove).testTag("fab")) {
                        TapHoldFab(
                            label = "Comandi",
                            container = Color.Black,
                            ink = Color.White,
                            holdLabel = "Tutti",
                            onTap = { },
                            onHold = { }
                        ) { quale -> Icon(Icons.Outlined.Info, contentDescription = quale) }
                    }
                    AppNotice(
                        Notices.line,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .aboveFoot()
                            .testTag("avviso")
                    )
                }
            }
        }
        banco.waitForIdle()
        banco.mainClock.advanceTimeBy(1_000)
        banco.waitForIdle()

        val notifica = banco.onNodeWithTag("avviso").getBoundsInRoot()
        val tasto = banco.onNodeWithTag("fab").getBoundsInRoot()
        if (destra) {
            assertTrue(
                "la notifica (fino a ${notifica.right}) copre il FAB (da ${tasto.left})",
                notifica.right <= tasto.left
            )
        } else {
            assertTrue(
                "la notifica (da ${notifica.left}) copre il FAB (fino a ${tasto.right})",
                notifica.left >= tasto.right
            )
        }
    }

    /**
     * **Quando sale sopra una fascia, la notifica resta larga: non si stringe anche di fianco.**
     *
     * ⚠️⚠️ **È LA SUA RISPOSTA `sempre` A `d-avviso-forma-2`** (giro della `2.25`: *può stare
     * massimizzata il larghezza solo quando (per la presenza della bottomsheet) si sposta sopra*).
     * Sopra una scheda larga tutto lo schermo non c'è nessun comando da schivare di fianco, quindi
     * rientrare là toglierebbe spazio al testo senza guadagnare niente.
     * ⚠️⚠️ **LA SCENA È QUELLA CHE SUL TELEFONO DURA QUALCHE FOTOGRAMMA**: appena c'è una selezione
     * il FAB lascia il posto alla scheda, quindi i due chiedenti convivono solo durante il cambio.
     * Il banco li tiene in scena insieme, che è il solo modo di misurare quel fotogramma.
     * ⚠️ **Si guarda che la notifica ARRIVI sopra il FAB**, cioè il contrario dei due casi qui
     * sopra: è la forma esatta della regola, perché il rientro o c'è o non c'è.
     * ⚠️ **Controprovata** togliendo la condizione da `aboveFoot()`: la notifica si stringe anche
     * qui, il suo bordo destro torna prima del tasto e il caso cade.
     */
    @Test
    fun `sopra una fascia la notifica non si stringe`() {
        Notices.say("1 elemento spostato")
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
                    Box(modifier = Modifier.align(Alignment.BottomEnd).testTag("fab")) {
                        TapHoldFab(
                            label = "Comandi",
                            container = Color.Black,
                            ink = Color.White,
                            holdLabel = "Tutti",
                            onTap = { },
                            onHold = { }
                        ) { quale -> Icon(Icons.Outlined.Info, contentDescription = quale) }
                    }
                    AppNotice(
                        Notices.line,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .aboveFoot()
                            .testTag("avviso")
                    )
                }
            }
        }
        banco.waitForIdle()
        banco.mainClock.advanceTimeBy(1_000)
        banco.waitForIdle()

        val notifica = banco.onNodeWithTag("avviso").getBoundsInRoot()
        val tasto = banco.onNodeWithTag("fab").getBoundsInRoot()
        assertTrue(
            "salendo sopra la scheda la notifica (fino a ${notifica.right}) doveva restare larga, " +
                "e invece si è stretta prima del FAB (da ${tasto.left})",
            notifica.right > tasto.left
        )
    }
}

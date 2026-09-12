package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.abs

/**
 * La riga su cui i due moduli si sovrappongono: la **seconda** di tutti e due, cioè il contrasto
 * della Luce e la tinta del Colore. È il numero che il difetto della `2.19` metteva in comune.
 */
private const val RIGA = 1

/** I nomi delle otto fasce, per contare le pastiglie in scena senza ricopiarne l'elenco. */
private val BANDE = listOf(
    R.string.look_band_red,
    R.string.look_band_orange,
    R.string.look_band_yellow,
    R.string.look_band_green,
    R.string.look_band_aqua,
    R.string.look_band_blue,
    R.string.look_band_purple,
    R.string.look_band_magenta
)

/**
 * Il banco dei moduli **Colore** e **HSL**, e della fila che sceglie i moduli.
 *
 * ⚠️⚠️ **NON SI CHIAMA `ColoreTest` PERCHÉ QUEL NOME È GIÀ PRESO, e da un'altra cosa**: là vive
 * il colore di una **cartella**, cioè la tinta della sua intestazione. Qui si sviluppa
 * un'immagine, che è il verbo con cui `AIV/CLAUDE.md` distingue i due editor, e due prove
 * omonime in un repository che ne parla in ogni giro sono due cose che prima o poi qualcuno
 * scambia.
 *
 * ⚠️⚠️ **NASCE CON LA FUNZIONE E NON DOPO UN DIFETTO**, ed è il caso proattivo di
 * `AIV/CLAUDE.md` § '🧪 Quando si scrive una prova, e quando no': col secondo modulo i cursori
 * **non si vedono più tutti insieme**, quindi un gettone che non cambiasse l'elenco, o un 'Reset
 * modulo' che azzerasse anche l'altro, sarebbero difetti che il compilatore non vede e che si
 * scoprono solo muovendo un cursore e guardando sparire il lavoro fatto altrove.
 *
 * ⚠️ **Che cosa questo banco NON vede**: i pixel che escono dal conto, perché una prova gira
 * senza scheda grafica. Che la temperatura scaldi davvero e che la vividezza lasci stare i colori
 * già accesi si guardano sul telefono, e la voce di collaudo li chiede.
 */
@RunWith(AndroidJUnit4::class)
// ⚠️ La grafica vera serve per DECODIFICARE il PNG dell'anteprima: senza, la scheda resta
// spenta e non c'è niente da toccare. Vedi la stessa riga in `LuceTest`.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SviluppoTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **Caso 1: sotto la soglia il Colore è a riposo, e il bianco e nero non ha soglia.**
     *
     * ⚠️ **Il bianco e nero è un interruttore e non un cursore**: acceso, il modulo non è a
     * riposo qualunque cosa dicano gli altri quattro valori, e senza questa riga un'immagine
     * portata in bianco e nero si salverebbe **senza perdita**, cioè tornerebbe a colori.
     */
    @Test
    fun `sotto la soglia il colore e a riposo`() {
        assertTrue(Chroma.NONE.idle)
        assertTrue(Chroma(temp = 0.0001f).idle)
        assertTrue(Chroma(vibrance = -0.0004f).idle)
        assertFalse(Chroma(saturation = 0.01f).idle)
        assertFalse(Chroma(mono = true).idle)
    }

    /**
     * **Caso 2: un valore di Colore toglie il senza perdita, come uno di Luce.**
     *
     * ⚠️⚠️ **È LA CLAUSOLA DELL'UTENTE LETTA SUL MODULO NUOVO** (*quelle che non prevedono la
     * riscrittura del file pixel per pixel devono essere lossless*): `Look.lossless` guardava il
     * solo modulo Luce, quindi prima della `2.19` un'immagine con la sola temperatura cambiata si
     * sarebbe dichiarata senza perdita e sarebbe stata salvata **senza il conto applicato**.
     */
    @Test
    fun `un valore di colore toglie il senza perdita`() {
        assertTrue(Look.NONE.lossless)
        assertFalse(Look(chroma = Chroma(temp = 0.4f)).lossless)
        assertFalse(Look(chroma = Chroma(mono = true)).lossless)
        assertFalse(Look(chroma = Chroma(temp = 0.4f)).idle)
    }

    /**
     * **Caso 3: la fila cambia i cursori che si vedono.**
     *
     * ⚠️ Col secondo modulo i cursori non stanno più tutti sulla scheda, quindi un gettone che
     * non cambiasse l'elenco lascerebbe il Colore irraggiungibile senza dare nessun errore.
     */
    @Test
    fun `il gettone cambia i cursori in scena`() {
        banco.setContent { Scena() }
        pronta()

        banco.onNodeWithText(testo(R.string.look_exposure)).assertExists()
        assertEquals(0, quanti(testo(R.string.look_saturation)))

        banco.onNodeWithText(testo(R.string.look_color)).performClick()
        banco.waitForIdle()

        banco.onNodeWithText(testo(R.string.look_saturation)).assertExists()
        assertEquals(0, quanti(testo(R.string.look_exposure)))
    }

    /**
     * **Caso 4: il tocco lungo su un gettone azzera QUEL modulo e lascia stare l'altro.**
     *
     * ⚠️⚠️ **È IL 'RESET MODULO' CHE LUI HA CHIESTO** (campo libero del giro della `2.14`), e la
     * metà che conta è la seconda: un azzeramento che si portasse via anche la Luce sarebbe
     * 'Originale', che è lì accanto, e il lavoro fatto nell'altro modulo sparirebbe senza che
     * niente lo annunci.
     */
    @Test
    fun `il tocco lungo su un gettone azzera solo il suo modulo`() {
        banco.setContent { Scena() }
        pronta()

        muovi(1, 0.5f)
        assertTrue(valore(1) > 0.2f)

        banco.onNodeWithText(testo(R.string.look_color)).performClick()
        banco.waitForIdle()
        muovi(0, 0.5f)
        assertTrue(valore(0) > 0.2f)

        banco.onNodeWithText(testo(R.string.look_color)).performTouchInput { longClick() }
        banco.waitForIdle()
        assertEquals("il colore doveva azzerarsi", 0f, valore(0), 1e-3f)

        banco.onNodeWithText(testo(R.string.look_light)).performClick()
        banco.waitForIdle()
        assertTrue("la luce non doveva essere toccata", valore(1) > 0.2f)
    }

    /**
     * **Caso 5: col bianco e nero acceso i due cursori dei colori si spengono.**
     *
     * ⚠️ Là non c'è più niente da saturare, e un cursore che si muove senza cambiare l'immagine
     * si legge come un guasto.
     */
    @Test
    fun `il bianco e nero spegne saturazione e vividezza`() {
        banco.setContent { Scena() }
        pronta()
        banco.onNodeWithText(testo(R.string.look_color)).performClick()
        banco.waitForIdle()

        cursore(2).assertIsEnabled()
        cursore(3).assertIsEnabled()

        banco.onNodeWithText(testo(R.string.look_bw)).performClick()
        banco.waitForIdle()

        cursore(2).assertIsNotEnabled()
        cursore(3).assertIsNotEnabled()
        // ⚠️ La temperatura invece resta viva: il bianco e nero toglie i colori, non la
        // differenza fra una luce calda e una fredda, che sui grigi si vede come densità.
        cursore(0).assertIsEnabled()
    }

    /**
     * **Caso 6: il dito su un cursore del Colore non muove quello della Luce.**
     *
     * ⚠️⚠️ **QUESTA PROVA NON MISURA IL DIFETTO DEL GIRO DELLA `2.19`, E VA DETTO** (campo libero:
     * *il mio tocco, mentre provo a spostare la tinta o la saturazione, sposta invece il contrasto
     * che è nell'altro modulo*): quel difetto **sul banco non si riproduce**, e questo caso è
     * rimasto verde anche togliendo a mano la correzione, in tutte e due le sue metà. Una spia
     * messa dentro il gesto ha detto perché: a rispondere è sempre il cursore che si tocca, sia
     * col tocco secco sia con un trascinamento vero. Quello che questa prova presidia è la
     * **funzione** (un cursore scrive il proprio campo e nient'altro), non la causa di quel
     * difetto, che resta non accertata.
     *
     * ⚠️⚠️ **SI TOCCA COL DITO, E LE ALTRE PROVE NON LO FANNO**: i cinque casi qui sopra muovono i
     * cursori con l'azione semantica, che vive in un `Modifier.semantics` e si riscrive a ogni
     * ricomposizione. Il dito passa invece dal `pointerInput`, che è un'altra strada e va
     * percorsa da qualcuno.
     *
     * ⚠️ **I due cursori sono lo STESSO numero di riga**, ed è il punto: la tinta è la seconda del
     * Colore e il contrasto la seconda della Luce, cioè la coppia che lui ha nominato.
     */
    @Test
    fun `il cursore di un modulo non muove quello dell'altro`() {
        banco.setContent { Scena() }
        pronta()

        banco.onNodeWithText(testo(R.string.look_color)).performClick()
        banco.waitForIdle()
        tocca(RIGA, 0.25f)
        assertTrue("Il tocco non ha mosso la tinta", abs(valore(RIGA)) > 0.1f)

        banco.onNodeWithText(testo(R.string.look_light)).performClick()
        banco.waitForIdle()
        assertEquals(
            "Il tocco sulla tinta ha mosso il contrasto, che è nell'altro modulo",
            0f,
            valore(RIGA),
            1e-3f
        )
    }

    /**
     * **Caso 7: i raggi delle fasce combaciano, che è quello che fa sommare i pesi a uno.**
     *
     * ⚠️⚠️ **MISURA LA PROPRIETÀ E NON RICOPIA IL CONTO**: il conto dei pesi vive in AGSL e il
     * banco non lo può eseguire, ma quello che lo rende corretto è una relazione fra i numeri che
     * gli arrivano, e questa si misura. Se il raggio di una fascia verso l'alto non è lo stesso
     * che la vicina ha verso il basso, fra i due centri i pesi non sommano più a uno e in quel
     * tratto l'immagine riceve **meno** di quanto i due cursori chiedono, senza che niente lo
     * dica.
     *
     * ⚠️ **E la somma dei raggi verso l'alto fa un giro intero**: senza, resterebbe un arco di
     * tonalità che non appartiene a nessuna fascia.
     */
    @Test
    fun `i raggi delle fasce combaciano`() {
        var giro = 0f
        for (i in 0 until Mix.COUNT) {
            val dopo = (i + 1) % Mix.COUNT
            assertEquals(
                "il raggio fra la fascia $i e la $dopo non combacia",
                Mix.SPAN_HI[i],
                Mix.SPAN_LO[dopo],
                1e-6f
            )
            assertTrue("la fascia $i ha un raggio nullo", Mix.SPAN_HI[i] > 0f)
            giro += Mix.SPAN_HI[i]
        }
        assertEquals("i raggi non coprono un giro intero", 1f, giro, 1e-5f)
    }

    /**
     * **Caso 8: un valore di HSL toglie il senza perdita, e sotto la soglia il modulo è a riposo.**
     *
     * ⚠️ **È la clausola dell'utente letta sul terzo modulo** (*quelle che non prevedono la
     * riscrittura del file pixel per pixel devono essere lossless*): una fascia mossa riscrive i
     * pixel come un cursore di Luce, e senza questa riga l'immagine si salverebbe girando un tag
     * EXIF, cioè senza il conto applicato.
     */
    @Test
    fun `un valore di hsl toglie il senza perdita`() {
        assertTrue(Mix.NONE.idle)
        assertTrue(Mix.NONE.swap(3) { it.copy(lum = 0.0001f) }.idle)
        assertFalse(Mix.NONE.swap(3) { it.copy(lum = 0.4f) }.idle)
        assertFalse(Look(mix = Mix.NONE.swap(0) { it.copy(hue = 0.5f) }).lossless)
    }

    /**
     * **Caso 9: la fascia scelta cambia i valori che i tre cursori mostrano e scrivono.**
     *
     * ⚠️⚠️ **È LA CORREZIONE DELLA `2.20` LETTA SU UNA DIMENSIONE IN PIÙ**: i cursori dell'HSL
     * sono tre soli e le fasce otto, quindi lo stesso nodo serve otto insiemi di valori. Se la
     * riga risolvesse la fascia della composizione in cui è nata invece di quella scelta adesso,
     * muovere un cursore scriverebbe nel colore sbagliato, e a vederlo sarebbe solo l'immagine.
     */
    @Test
    fun `la fascia scelta cambia i cursori`() {
        banco.setContent { Scena() }
        pronta()
        banco.onNodeWithText(testo(R.string.look_mix)).performClick()
        banco.waitForIdle()

        muovi(0, 0.5f)
        assertTrue("il tocco non ha mosso la tonalità del rosso", valore(0) > 0.2f)

        fascia(R.string.look_band_green)
        assertEquals("il verde doveva essere intatto", 0f, valore(0), 1e-3f)
        muovi(0, -0.5f)
        assertTrue("il tocco non ha mosso la tonalità del verde", valore(0) < -0.2f)

        fascia(R.string.look_band_red)
        assertTrue("il rosso doveva ritrovare il suo valore", valore(0) > 0.2f)
    }

    /**
     * **Caso 10: il tocco lungo su una pastiglia azzera quella fascia e lascia stare le altre.**
     *
     * ⚠️ **È il gesto del gettone di un modulo un gradino più in basso**, e la metà che conta è la
     * seconda: un azzeramento che si portasse via anche le altre fasce sarebbe il 'Reset modulo',
     * che è la riga sopra.
     */
    @Test
    fun `il tocco lungo su una fascia azzera solo quella`() {
        banco.setContent { Scena() }
        pronta()
        banco.onNodeWithText(testo(R.string.look_mix)).performClick()
        banco.waitForIdle()

        muovi(2, 0.5f)
        fascia(R.string.look_band_blue)
        muovi(2, 0.5f)
        assertTrue(valore(2) > 0.2f)

        banco.onNodeWithContentDescription(testo(R.string.look_band_blue))
            .performTouchInput { longClick() }
        banco.waitForIdle()
        assertEquals("il blu doveva azzerarsi", 0f, valore(2), 1e-3f)

        fascia(R.string.look_band_red)
        assertTrue("il rosso non doveva essere toccato", valore(2) > 0.2f)
    }

    /**
     * **Caso 11: col bianco e nero l'HSL tiene la sola luminanza, che è la miscela del grigio.**
     *
     * ⚠️⚠️ **È LA SUA RISPOSTA `hsl` A `d-bn-pesi`, MISURATA**: i pesi per fascia del bianco e nero
     * non sono un secondo meccanismo, sono questa riga. Se si spegnesse anche lei, la risposta non
     * sarebbe implementata affatto; se restassero accese le altre due, si offrirebbero due cursori
     * che su un'immagine senza colori non fanno niente.
     */
    @Test
    fun `il bianco e nero lascia la sola luminanza dell'hsl`() {
        banco.setContent { Scena() }
        pronta()
        banco.onNodeWithText(testo(R.string.look_color)).performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.look_bw)).performClick()
        banco.waitForIdle()

        banco.onNodeWithText(testo(R.string.look_mix)).performClick()
        banco.waitForIdle()

        cursore(0).assertIsNotEnabled()
        cursore(1).assertIsNotEnabled()
        cursore(2).assertIsEnabled()
    }

    /**
     * **Caso 12: la fila delle fasce c'è solo nel modulo che ne ha.**
     *
     * ⚠️ Una pastiglia che restasse in scena negli altri due moduli sceglierebbe una fascia che là
     * non vuol dire niente, e il suo tocco lungo azzererebbe un colore da una schermata che di
     * colori non parla.
     */
    @Test
    fun `le pastiglie ci sono solo nell'hsl`() {
        banco.setContent { Scena() }
        pronta()
        assertEquals(0, quanteFasce())

        banco.onNodeWithText(testo(R.string.look_mix)).performClick()
        banco.waitForIdle()
        assertEquals(Mix.COUNT, quanteFasce())

        banco.onNodeWithText(testo(R.string.look_color)).performClick()
        banco.waitForIdle()
        assertEquals(0, quanteFasce())
    }

    /**
     * **Caso 13: il Dettaglio è a riposo anche col raggio e la mascheratura mossi.**
     *
     * ⚠️⚠️ **DUE CURSORI SU CINQUE NON CAMBIANO UN PIXEL DA SOLI, E QUESTA È LA RIGA CHE LO
     * PRESIDIA**: il raggio e la mascheratura dicono **come** la maschera di contrasto lavora,
     * non quanto. Contandoli in [Detail.idle], un'immagine con la sola mascheratura mossa si
     * dichiarerebbe da riscrivere, cioè verrebbe ricompressa per niente, e il senza perdita se
     * ne andrebbe senza che nessuno abbia chiesto niente.
     */
    @Test
    fun `il raggio e la mascheratura da soli lasciano il dettaglio a riposo`() {
        assertTrue(Detail.NONE.idle)
        assertTrue(Detail(radius = 1f, masking = 1f).idle)
        assertTrue(Look(detail = Detail(radius = -1f)).lossless)
        assertFalse(Detail(sharpen = 0.01f).idle)
        assertFalse(Detail(noise = 0.01f).idle)
        assertFalse(Detail(noiseColor = 0.01f).idle)
        assertFalse(Look(detail = Detail(noise = 0.5f)).lossless)
    }

    /**
     * **Caso 14: il raggio e la mascheratura sono spenti finché la nitidezza è a zero.**
     *
     * ⚠️ **È la stessa regola del bianco e nero letta su un altro modulo**: un cursore che si
     * muove senza cambiare l'immagine si legge come un guasto. Qui misura anche il meccanismo
     * nuovo, cioè che un cursore dichiari **quando** non governa niente invece di guardare un
     * interruttore scritto nella scheda.
     */
    @Test
    fun `il raggio e la mascheratura seguono la nitidezza`() {
        banco.setContent { Scena() }
        pronta()
        banco.onNodeWithText(testo(R.string.look_detail)).performClick()
        banco.waitForIdle()

        cursore(1).assertIsNotEnabled()
        cursore(2).assertIsNotEnabled()
        cursore(3).assertIsEnabled()

        muovi(0, 0.5f)
        cursore(1).assertIsEnabled()
        cursore(2).assertIsEnabled()
    }

    /**
     * **Caso 15: il 'Reset modulo' del Dettaglio azzera il suo e lascia stare gli altri.**
     *
     * ⚠️ Col quarto modulo i gettoni sono quattro, e un azzeramento che prendesse anche i vicini
     * porterebbe via il lavoro fatto in una schermata che non si sta guardando.
     */
    @Test
    fun `il tocco lungo sul dettaglio azzera solo il dettaglio`() {
        banco.setContent { Scena() }
        pronta()
        muovi(1, 0.5f)
        assertTrue(valore(1) > 0.2f)

        banco.onNodeWithText(testo(R.string.look_detail)).performClick()
        banco.waitForIdle()
        muovi(0, 0.6f)
        assertTrue(valore(0) > 0.2f)

        banco.onNodeWithText(testo(R.string.look_detail)).performTouchInput { longClick() }
        banco.waitForIdle()
        assertEquals("il dettaglio doveva azzerarsi", 0f, valore(0), 1e-3f)

        banco.onNodeWithText(testo(R.string.look_light)).performClick()
        banco.waitForIdle()
        assertTrue("la luce non doveva essere toccata", valore(1) > 0.2f)
    }

    /**
     * **Caso 16: le misure del Dettaglio si scalano col lato dell'immagine.**
     *
     * ⚠️⚠️ **È QUELLO CHE TIENE INSIEME L'ANTEPRIMA E IL FILE SALVATO**: il conto gira su due
     * immagini di misura diversa, e un raggio scritto in pixel peserebbe più del doppio
     * sull'anteprima, che è una riduzione. Scritto come frazione del lato, il rapporto fra i due
     * risultati è **uno** per costruzione, e questa prova lo misura invece di ricopiare il conto,
     * che vive in AGSL e il banco non lo può eseguire.
     */
    @Test
    fun `il raggio del dettaglio segue il lato dell'immagine`() {
        val fine = Detail(sharpen = 0.5f)
        assertEquals(2f * fine.sharpReach(1000f), fine.sharpReach(2000f), 1e-4f)
        assertEquals(2f * Detail.grainReach(1000f), Detail.grainReach(2000f), 1e-4f)

        // Il cursore del raggio raddoppia e dimezza: è il verso che l'utente si aspetta, e un
        // segno di troppo lo rovescerebbe senza che nessun compilatore lo veda.
        val stretto = Detail(sharpen = 0.5f, radius = -1f).sharpReach(4000f)
        val largo = Detail(sharpen = 0.5f, radius = 1f).sharpReach(4000f)
        assertEquals(4f * stretto, largo, 1e-3f)
    }

    /**
     * **Caso 17: le tessere del salvataggio leggono il bordo e copiano solo quello che vale.**
     *
     * ⚠️⚠️ **QUESTO È IL CONTO CHE IL DETTAGLIO HA RESO NECESSARIO, ed è l'unica cosa del
     * salvataggio che il banco possa misurare**: disegnare vuole una scheda grafica, ma un indice
     * sbagliato di un pixel si vede qui. Senza bordo, il filtro dell'ultima colonna di una tessera
     * leggerebbe il bordo ripetuto invece del pixel che sta di là, e su ogni giunzione comparirebbe
     * una riga.
     * - **A modulo spento il bordo vale zero**, quindi le tessere tornano quelle di prima e chi non
     *   usa il Dettaglio non paga niente.
     * - **Il bordo si taglia ai margini dell'immagine**, dove non c'è niente da leggere, e là
     *   quello che si tiene comincia da zero.
     */
    @Test
    fun `le tessere prendono il bordo e copiano solo il centro`() {
        assertEquals(0, Look.NONE.detail.bleed(4000f))
        val fine = Detail(sharpen = 0.5f)
        assertTrue("il bordo deve coprire il raggio", fine.bleed(4000f) > fine.sharpReach(4000f))

        // Una tessera in mezzo: legge il bordo da tutti e quattro i lati, e tiene il centro.
        val dentro = tileBox(w = 100, h = 100, x = 40, y = 40, tw = 20, th = 20, bleed = 4)
        assertEquals(36, dentro.read.left)
        assertEquals(64, dentro.read.bottom)
        assertEquals(4, dentro.take.left)
        assertEquals(24, dentro.take.right)
        assertEquals(20, dentro.put.width())

        // Una tessera d'angolo: di là dall'immagine non c'è niente, quindi il bordo si taglia e
        // quello che si tiene comincia da zero.
        val angolo = tileBox(w = 100, h = 100, x = 0, y = 0, tw = 20, th = 20, bleed = 4)
        assertEquals(0, angolo.read.left)
        assertEquals(0, angolo.take.left)
        assertEquals(20, angolo.take.right)
        assertEquals(0, angolo.put.left)

        // Due tessere vicine non si sovrappongono in scrittura: quello che si copia non dipende
        // dall'ordine in cui si disegnano.
        val prima = tileBox(w = 100, h = 100, x = 0, y = 0, tw = 20, th = 20, bleed = 4)
        val dopo = tileBox(w = 100, h = 100, x = 20, y = 0, tw = 20, th = 20, bleed = 4)
        assertEquals(prima.put.right, dopo.put.left)
        assertTrue("in lettura invece si sovrappongono", dopo.read.left < prima.put.right)
    }

    /** Sceglie una fascia toccando la sua pastiglia. */
    private fun fascia(nome: Int) {
        banco.onNodeWithContentDescription(testo(nome)).performClick()
        banco.waitForIdle()
    }

    /** Quante pastiglie di fascia sono in scena, contate dai loro nomi. */
    private fun quanteFasce(): Int = BANDE.count {
        banco.onAllNodesWithContentDescription(testo(it)).fetchSemanticsNodes().isNotEmpty()
    }

    /** Vedi la nota su `pronta()` in `LuceTest`: il palco compare quando l'immagine esiste. */
    private fun pronta() {
        banco.waitUntil(5_000) {
            banco.onAllNodesWithContentDescription(testo(R.string.look_compare))
                .fetchSemanticsNodes().isNotEmpty()
        }
        banco.waitForIdle()
    }

    private fun cursore(quale: Int) =
        banco.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))[quale]

    private fun muovi(quale: Int, a: Float) {
        cursore(quale).performSemanticsAction(SemanticsActions.SetProgress) { it(a) }
        banco.waitForIdle()
    }

    /**
     * Tocca la barra di un cursore a una frazione della sua larghezza, come farebbe un dito.
     *
     * ⚠️ **Un tocco secco e non una strisciata**: toccando lontano dal tondo il cursore salta
     * subito al punto, quindi il valore si muove senza iniettare un gesto con la sua durata, che
     * col clock di prova è una delle trappole di casa.
     */
    private fun tocca(quale: Int, dove: Float) {
        cursore(quale).performTouchInput { click(Offset(width * dove, height / 2f)) }
        banco.waitForIdle()
    }

    private fun valore(quale: Int): Float =
        cursore(quale).fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo].current

    private fun quanti(detto: String): Int =
        banco.onAllNodesWithText(detto).fetchSemanticsNodes().size

    private fun testo(id: Int, vararg args: Any): String = app.getString(id, *args)

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    @Composable
    private fun Scena() {
        AivTheme(darkTheme = false) {
            Box(modifier = Modifier.fillMaxSize()) {
                AdvancedEditorScreen(uri = quadrato(), busy = false, onSave = {}, onBack = {})
            }
        }
    }

    /** Un PNG vero su disco, e il suo indirizzo: senza, i comandi restano spenti. */
    private fun quadrato(): Uri {
        val file = File(app.cacheDir, "sviluppo.png")
        if (!file.exists()) {
            val mappa = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            mappa.eraseColor(Color.WHITE)
            file.outputStream().use { mappa.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return Uri.fromFile(file)
    }
}

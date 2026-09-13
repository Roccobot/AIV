package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.up
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

/**
 * I nomi dei sette moduli, nell'ordine in cui la fila li disegna.
 *
 * ⚠️ **Sono ricopiati e non presi dalla schermata**, che li tiene in un valore privato: qui serve
 * proprio il confronto fra quello che la fila mostra e quello che ci si aspetta di trovare.
 */
private val MODULI = listOf(
    R.string.look_crop,
    R.string.look_geometry,
    R.string.look_light,
    R.string.look_color,
    R.string.look_mix,
    R.string.look_detail,
    R.string.look_tone
)

/** I tre comandi di posa del modulo Ritaglio, che sono quelli dell'editor di casa. */
private val POSA = listOf(R.string.editor_left, R.string.editor_right, R.string.editor_flip)

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
 * I nomi dei quattro canali delle curve.
 *
 * ⚠️ **Sono ricopiati e non presi dalla schermata**, che li tiene in un valore privato: qui serve
 * sapere che cosa deve comparire, e una prova che leggesse la stessa lista del codice direbbe solo
 * che quella lista è uguale a se stessa.
 */
private val TONE_NAMES_TEST = listOf(
    R.string.look_tone_rgb,
    R.string.look_band_red,
    R.string.look_band_green,
    R.string.look_band_blue
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

        banco.onNodeWithContentDescription(testo(R.string.look_color)).performClick()
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

        banco.onNodeWithContentDescription(testo(R.string.look_color)).performClick()
        banco.waitForIdle()
        muovi(0, 0.5f)
        assertTrue(valore(0) > 0.2f)

        banco.onNodeWithContentDescription(testo(R.string.look_color)).performTouchInput { longClick() }
        banco.waitForIdle()
        assertEquals("il colore doveva azzerarsi", 0f, valore(0), 1e-3f)

        banco.onNodeWithContentDescription(testo(R.string.look_light)).performClick()
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
        banco.onNodeWithContentDescription(testo(R.string.look_color)).performClick()
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

        banco.onNodeWithContentDescription(testo(R.string.look_color)).performClick()
        banco.waitForIdle()
        tocca(RIGA, 0.25f)
        assertTrue("Il tocco non ha mosso la tinta", abs(valore(RIGA)) > 0.1f)

        banco.onNodeWithContentDescription(testo(R.string.look_light)).performClick()
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
        banco.onNodeWithContentDescription(testo(R.string.look_mix)).performClick()
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
        banco.onNodeWithContentDescription(testo(R.string.look_mix)).performClick()
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
        banco.onNodeWithContentDescription(testo(R.string.look_color)).performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.look_bw)).performClick()
        banco.waitForIdle()

        banco.onNodeWithContentDescription(testo(R.string.look_mix)).performClick()
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

        banco.onNodeWithContentDescription(testo(R.string.look_mix)).performClick()
        banco.waitForIdle()
        assertEquals(Mix.COUNT, quanteFasce())

        banco.onNodeWithContentDescription(testo(R.string.look_color)).performClick()
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
        banco.onNodeWithContentDescription(testo(R.string.look_detail)).performClick()
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

        banco.onNodeWithContentDescription(testo(R.string.look_detail)).performClick()
        banco.waitForIdle()
        muovi(0, 0.6f)
        assertTrue(valore(0) > 0.2f)

        banco.onNodeWithContentDescription(testo(R.string.look_detail)).performTouchInput { longClick() }
        banco.waitForIdle()
        assertEquals("il dettaglio doveva azzerarsi", 0f, valore(0), 1e-3f)

        banco.onNodeWithContentDescription(testo(R.string.look_light)).performClick()
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

    /**
     * **Caso 18: una curva a riposo è l'identità, e la spline non oltrepassa mai.**
     *
     * ⚠️⚠️ **LE DUE COSE SI PRESIDIANO INSIEME PERCHÉ SONO LA STESSA PROPRIETÀ**: la tabella che va
     * alla scheda grafica è l'unica cosa che l'immagine vede, quindi a riposo deve essere la
     * diagonale **esatta** (o un'immagine non toccata perderebbe un livello qua e là), e con un
     * punto alzato non deve mai tornare indietro, o fra due toni vicini ne uscirebbe uno più scuro
     * del suo vicino più chiaro, che sull'immagine si vede come un anello di tono invertito.
     *
     * ⚠️ **Controprovata con una cubica NATURALE al posto della monotona**: con quel conto, un
     * punto alzato di poco fa scendere la curva sotto il valore del punto prima, cioè il caso che
     * questa riga esiste per escludere.
     */
    @Test
    fun `la curva a riposo è l'identità e non oltrepassa`() {
        val ferma = Curve.NONE.table()
        for (i in 0 until Curve.SIZE) {
            assertEquals("livello $i", i.toFloat() / (Curve.SIZE - 1), ferma[i], 1e-6f)
        }
        assertTrue(Curve.NONE.idle)
        assertTrue(Look().lossless)

        // Un punto alzato in mezzo: la curva passa di lì e resta monotona su tutta la corsa.
        val alzata = Curve(listOf(Knot(0f, 0f), Knot(0.5f, 0.8f), Knot(1f, 1f)))
        val tavola = alzata.table()
        assertFalse(alzata.idle)
        assertFalse(Look(tone = Tone(all = alzata)).lossless)
        assertEquals("il punto deve stare sulla curva", 0.8f, tavola[128], 0.01f)
        for (i in 1 until Curve.SIZE) {
            assertTrue("inversione al livello $i", tavola[i] >= tavola[i - 1] - 1e-4f)
        }

        /*
         * ⚠️⚠️ **IL TRATTO PIATTO È IL CASO CHE DISTINGUE I DUE CONTI, ed è la ragione per cui
         * questa curva ha quattro punti**: fra due punti alla stessa altezza la secante vale zero,
         * e una cubica naturale ci arriva con le tangenti dei tratti vicini, cioè **sale e poi
         * torna giù**. La correzione monotona azzera quelle due tangenti, e il tratto viene piatto
         * esatto. Con una curva a tre punti i due conti danno quasi lo stesso disegno, e la
         * controprova resterebbe verde.
         */
        val piana = Curve(
            listOf(Knot(0f, 0f), Knot(0.4f, 0.75f), Knot(0.7f, 0.75f), Knot(1f, 1f))
        )
        val steso = piana.table()
        for (i in 103..178) {
            assertEquals("il tratto piatto si è gonfiato al livello $i", 0.75f, steso[i], 2e-3f)
        }
    }

    /**
     * **Caso 19: la tabella compone `all(canale(v))`, e non il contrario.**
     *
     * ⚠️⚠️ **L'ORDINE ROVESCIATO NON DÀ NESSUN ERRORE E SI VEDE SOLO SULL'IMMAGINE**: con la curva
     * di un canale e quella di tutti i toni mosse insieme, i due ordini danno due immagini diverse,
     * e quello sbagliato fa saltare di posto la curva del canale ogni volta che si tocca il
     * composito. Le due curve di questa prova sono **costanti e diverse**, che è il solo modo di
     * distinguere i due ordini con un numero.
     */
    @Test
    fun `la tabella compone il canale sotto il composito`() {
        val mezzo = Curve(listOf(Knot(0f, 0.5f), Knot(1f, 0.5f)))
        val quarto = Curve(listOf(Knot(0f, 0.25f), Knot(1f, 0.25f)))
        val lut = Tone(all = mezzo, red = quarto).lut()
        val rosso = (lut[200] shr 16) and 0xFF
        val verde = (lut[200] shr 8) and 0xFF
        assertTrue(
            "il rosso deve passare prima dal canale e poi dal composito, ed è $rosso",
            abs(rosso - 128) <= 2
        )
        assertTrue("il verde ha solo il composito, ed è $verde", abs(verde - 128) <= 2)
    }

    /**
     * **Caso 20: gli estremi restano agli estremi, e non si tolgono.**
     *
     * ⚠️⚠️ **SONO LA CONDIZIONE PER CUI LA CURVA DICE QUALCOSA DI OGNI TONO**: un primo punto
     * spostato a mezza scala lascerebbe la prima metà dei toni senza risposta, e la spline
     * risponderebbe con la coda del primo tratto, cioè con un valore che nessuno ha chiesto.
     * ⚠️ **E due punti non si scavalcano**: un tratto di larghezza zero è una divisione per zero
     * nella spline, e due punti in ordine invertito una curva che torna indietro.
     */
    @Test
    fun `gli estremi non si muovono in orizzontale e non si tolgono`() {
        val tre = Curve(listOf(Knot(0f, 0f), Knot(0.5f, 0.5f), Knot(1f, 1f)))
        assertEquals(0f, tre.move(0, 0.9f, 0.2f).knots[0].at, 1e-6f)
        assertEquals(0.2f, tre.move(0, 0.9f, 0.2f).knots[0].to, 1e-6f)
        assertEquals(1f, tre.move(2, 0.1f, 0.7f).knots[2].at, 1e-6f)
        assertEquals(3, tre.drop(0).knots.size)
        assertEquals(3, tre.drop(2).knots.size)
        assertEquals(2, tre.drop(1).knots.size)
        // Il punto di mezzo non scavalca i vicini, in nessuno dei due versi.
        assertTrue(tre.move(1, 2f, 0.5f).knots[1].at < 1f)
        assertTrue(tre.move(1, -2f, 0.5f).knots[1].at > 0f)
    }

    /**
     * **Caso 21: il modulo Curve porta i quattro canali e nessun cursore, e il tocco lungo su un
     * canale azzera solo quello.**
     *
     * ⚠️ **È la stessa forma del caso del Dettaglio, su un modulo che di cursori non ne ha
     * affatto**: qui a rompersi in silenzio sarebbe un gettone che scrive nel canale sbagliato,
     * cioè un azzeramento che porta via la curva di un altro colore.
     */
    @Test
    fun `il modulo curve porta i canali e nessun cursore`() {
        banco.setContent { Scena() }
        pronta()
        banco.onNodeWithContentDescription(testo(R.string.look_tone)).performClick()
        banco.waitForIdle()

        assertEquals(4, TONE_NAMES_TEST.count { quanti(testo(it)) > 0 })
        assertEquals(
            "un modulo di sole curve non ha cursori",
            0,
            banco.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
                .fetchSemanticsNodes().size
        )
        assertTrue("il grafico deve essere in scena", quantiDetti(R.string.look_tone_board) > 0)
    }

    /**
     * **Caso 22: il colore mirato si offre nei soli moduli che hanno un bersaglio.**
     *
     * ⚠️⚠️ **È LA CONDIZIONE CHE TIENE VIVO IL PALCO**: quel tasto arma una modalità in cui il
     * palco fa solo il mirato, e in un modulo senza bersaglio sarebbe una modalità che non fa
     * niente mentre spegne pinza e doppio tocco.
     */
    @Test
    fun `il mirato c'è nelle curve e nell'hsl e non negli altri`() {
        banco.setContent { Scena() }
        pronta()
        assertEquals("nella Luce non c'è niente da mirare", 0, quanti(testo(R.string.look_target)))

        banco.onNodeWithContentDescription(testo(R.string.look_tone)).performClick()
        banco.waitForIdle()
        assertEquals(1, quanti(testo(R.string.look_target)))

        banco.onNodeWithContentDescription(testo(R.string.look_mix)).performClick()
        banco.waitForIdle()
        assertEquals(1, quanti(testo(R.string.look_target)))

        banco.onNodeWithContentDescription(testo(R.string.look_detail)).performClick()
        banco.waitForIdle()
        assertEquals(0, quanti(testo(R.string.look_target)))
    }

    /**
     * **Caso 23: i due conti che il colore mirato fa su un pixel.**
     *
     * ⚠️⚠️ **UN GRIGIO NON APPARTIENE A NESSUNA FASCIA, ed è la risposta che non si può inventare**:
     * dando 'rosso' a un pixel senza colore, il dito finirebbe su una fascia che con quel pixel non
     * c'entra, e i tre cursori parlerebbero di un colore che là non esiste.
     * ⚠️ **E per il composito il livello è la luminanza percettiva**: un giallo pieno è un tono
     * alto e un blu pieno un tono basso, che è dove l'occhio li vede; con la media dei canali
     * sarebbero lo stesso tono.
     */
    @Test
    fun `il mirato legge la fascia e il livello di un pixel`() {
        assertEquals(0, Mix.bandOf(Color.rgb(255, 0, 0)))
        assertEquals(5, Mix.bandOf(Color.rgb(0, 0, 255)))
        assertEquals(3, Mix.bandOf(Color.rgb(0, 255, 0)))
        assertEquals("un grigio non ha fascia", -1, Mix.bandOf(Color.rgb(128, 128, 128)))

        val giallo = Color.rgb(255, 255, 0)
        val blu = Color.rgb(0, 0, 255)
        assertTrue(Tone.levelOf(giallo, Tone.WHOLE) > 0.9f)
        assertTrue(Tone.levelOf(blu, Tone.WHOLE) < 0.1f)
        assertEquals(1f, Tone.levelOf(giallo, Tone.RED), 1e-3f)
        assertEquals(0f, Tone.levelOf(giallo, Tone.BLUE), 1e-3f)
    }

    /**
     * **La lente del colore mirato c'è mentre il dito è giù, e sparisce quando si alza.**
     *
     * ⚠️⚠️ **NASCE DALLA SUA RISPOSTA A `d-mirato-hsl`** (giro della `2.23`: *serve un selettore
     * con zoom e anteprima dei pixel campionati*), e quello che il banco può vederne è questo:
     * che il palco cambi disegno col dito giù e torni **identico** al rilascio. Il difetto che
     * presidia è quello della `2.17`, cioè una cosa che si accende con un gesto e resta accesa.
     * ⚠️ **Si contano i pixel DIVERSI e non quelli di un colore**: il bordo e il mirino sono
     * tratti sottili, e l'antialiasing non garantisce un solo pixel del colore esatto, quindi una
     * misura sul colore potrebbe restare a zero con la lente in scena.
     * ⚠️ **Quello che NON vede**: se la lente mostri il pixel giusto, che è il suo mestiere. Il
     * conto vive sulla scheda grafica e qui non gira, quindi quello si guarda sul telefono.
     */
    @Test
    fun `la lente del mirato c'e col dito giu e sparisce al rilascio`() {
        banco.setContent { Scena() }
        pronta()
        banco.onNodeWithContentDescription(testo(R.string.look_tone)).performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.look_target)).performClick()
        banco.waitForIdle()

        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        val riposo = palco.captureToImage().toPixelMap()

        palco.performTouchInput { down(center) }
        banco.waitForIdle()
        val conDito = palco.captureToImage().toPixelMap()
        assertTrue(
            "col dito giù il palco deve portare la lente, e invece è identico",
            diversi(riposo, conDito) > 0
        )

        palco.performTouchInput { up() }
        banco.waitForIdle()
        assertEquals(
            "al rilascio la lente doveva sparire",
            0,
            diversi(riposo, palco.captureToImage().toPixelMap())
        )
    }

    /**
     * **La lente segue il dito, invece di restare dove il dito è sceso.**
     *
     * ⚠️⚠️ **È LA SUA RICHIESTA DEL GIRO DELLA `2.24`** (voce `mirato-lente` non approvata:
     * *dev'essere possibile trascinare il 'mirino', perché difficilmente con il dito si azzecca il
     * punto giusto al primo colpo*), e rovescia quello che la `2.24` aveva scritto apposta.
     * ⚠️ **Si confrontano due scatti col dito GIÙ**, in due punti diversi: se la lente restasse
     * ancorata, il secondo sarebbe identico al primo, perché nient'altro si muove.
     * ⚠️⚠️ **SI MISURA IN ORIZZONTALE, E IL BANCO LO HA IMPOSTO**: qui il palco è alto una
     * quarantina di pixel, quindi un movimento verticale grande quanto un quarto di lui non arriva
     * nemmeno alla soglia del tocco, e il gesto resta fermo **col codice giusto**. Una spia messa
     * dentro il rilevatore lo ha misurato: gli eventi arrivavano, e la distanza era sei pixel
     * contro sedici di soglia. In larghezza lo spazio c'è.
     * ⚠️⚠️ **IL CLOCK VA FERMATO, E SENZA QUELLA RIGA LA PROVA MISURAVA IL CONTRARIO**: con
     * l'avanzamento automatico `waitForIdle` porta a termine le attese pendenti, cioè fa **scadere**
     * l'attesa dell'armamento; da lì in poi il dito muove la curva e non più la lente, e il secondo
     * scatto è identico al primo **col codice giusto**. Quindi questo caso misura anche l'altra
     * metà della richiesta: prima dell'armamento a muoversi è il mirino.
     * ⚠️⚠️ **SI GUARDA DOVE SONO I PIXEL CAMBIATI E NON QUANTI: LO HA DETTO LA
     * CONTROPROVA.** La prima stesura confrontava i due scatti col dito giù e chiedeva che
     * fossero diversi: col difetto rimesso **restava verde**, perché fra i due fotogrammi cambiava
     * anche il contatore dell'armamento, che cresceva da sé. ⚠️ **Quel contatore è uscito con la
     * `2.26`** (§ '📈 Il modulo Curve, e il colore mirato'), quindi oggi il conto dei pixel diversi
     * direbbe il vero lo stesso: il baricentro resta perché dice *dove* è la lente, che è la cosa
     * che questo caso misura, e non dipende da che cosa la lente porti dentro.
     * ⚠️ **Controprovata** rimettendo l'ancoraggio della `2.24`, cioè togliendo l'assegnazione di
     * `lens` dentro il ciclo: il baricentro non si muove.
     */
    @Test
    fun `la lente segue il dito che si sposta`() {
        banco.setContent { Scena() }
        pronta()
        banco.onNodeWithContentDescription(testo(R.string.look_tone)).performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.look_target)).performClick()
        banco.waitForIdle()
        banco.mainClock.autoAdvance = false

        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        val riposo = palco.captureToImage().toPixelMap()
        palco.performTouchInput { down(center) }
        banco.mainClock.advanceTimeByFrame()
        val primo = centroX(riposo, palco.captureToImage().toPixelMap())

        val salto = riposo.width / 3f
        palco.performTouchInput { moveTo(center + Offset(-salto, 0f)) }
        banco.mainClock.advanceTimeByFrame()
        val poi = centroX(riposo, palco.captureToImage().toPixelMap())

        assertTrue("senza la lente in scena non c'e niente da misurare", primo > 0f && poi > 0f)
        assertTrue(
            "la lente doveva seguire il dito: era a $primo e adesso e a $poi",
            primo - poi > salto / 3f
        )
        palco.performTouchInput { up() }
    }

    /**
     * **Il mirino porta il colore della FASCIA, non quello del pixel.**
     *
     * ⚠️⚠️ **È L'ALTRA METÀ DELLA SUA RICHIESTA** (*deve variare dinamicamente il colore per
     * corrispondere a uno degli 8 colori standard, in modo che si capisca all'istante su cosa si
     * agirà se ci si ferma lì*): quello che deve dire l'anello è **quale delle otto** si sta per
     * toccare, quindi due rossi diversi devono darlo identico. Prendendo il colore del pixel la
     * lente direbbe una cosa vera e inutile, cioè quello che già si vede.
     * ⚠️ **E un grigio non ha fascia**: là il mirino resta bianco, che è il caso in cui `Mix.bandOf`
     * risponde `-1`.
     * ⚠️ **Controprovata** facendo tornare il colore del pixel: i due rossi divergono.
     */
    @Test
    fun `il mirino prende il colore della fascia e non del pixel`() {
        val chiaro = tintOfPixel(Color.rgb(255, 40, 40))
        val cupo = tintOfPixel(Color.rgb(120, 12, 12))
        assertTrue("un rosso deve avere la sua fascia", chiaro != null)
        assertEquals("due rossi sono la stessa fascia, quindi lo stesso segno", chiaro, cupo)
        assertEquals("un grigio non appartiene a nessuna fascia", null, tintOfPixel(Color.GRAY))
    }

    /**
     * **Caso 25: la geometria a riposo non sposta un pixel, e la maglia è la griglia.**
     *
     * ⚠️⚠️ **A RIPOSO IL CONTO DEVE ESSERE L'IDENTITÀ ESATTA, E NON 'QUASI'**: la deformazione
     * ricampiona, quindi un'immagine che passasse dalla maglia con uno scarto di mezzo pixel
     * perderebbe nitidezza senza che nessuno abbia mosso niente. La guardia che lo tiene è che il
     * palco a geometria ferma disegna un rettangolo e non una maglia, e questa riga misura che
     * anche il conto, da solo, non muoverebbe niente.
     */
    @Test
    fun `la geometria a riposo lascia ogni punto dov'è`() {
        assertTrue(Geometry.NONE.idle)
        assertTrue(Look(geo = Geometry.NONE).lossless)

        val piano = Warp.plan(Geometry.NONE, cx = 50f, cy = 40f, w = 100f, h = 80f)
        assertEquals("a riposo non si ingrandisce niente", 1f, piano.cover, 1e-5f)
        for (x in listOf(0f, 37f, 100f)) {
            for (y in listOf(0f, 21f, 80f)) {
                val p = piano.map(x, y)
                assertEquals(x, p[0], 1e-4f)
                assertEquals(y, p[1], 1e-4f)
            }
        }

        val dove = RectF(0f, 0f, 100f, 80f)
        val verts = Warp.points(piano, dove, cells = 4)
        val texs = Warp.grid(dove, cells = 4)
        assertEquals(texs.size, verts.size)
        for (i in verts.indices) assertEquals(texs[i], verts[i], 1e-4f)
    }

    /**
     * **Caso 26: l'andata e il ritorno della geometria si disfano a vicenda.**
     *
     * ⚠️⚠️ **È LA PROVA CHE REGGE IL COLORE MIRATO, ED È L'UNICA COSA DI QUESTO MODULO CHE POSSA
     * ROMPERSI IN SILENZIO**: col dito si tocca l'immagine **deformata**, e il pixel da leggere
     * vive prima della deformazione. Un segno sbagliato in [WarpPlan.back] non dà nessun errore e
     * non si vede sul palco: si vede solo prendendo un colore che sta da un'altra parte.
     * ⚠️ **La distorsione non ha una formula chiusa** e si inverte con quattro giri di Newton:
     * qui si misura che quei quattro bastino, invece di fidarsi del numero.
     * ⚠️ **Controprovata** rovesciando un segno nel keystone dell'inversa: lo scarto passa da
     * meno di un millesimo di pixel a parecchi pixel.
     */
    @Test
    fun `la geometria torna indietro dove è andata`() {
        val prove = listOf(
            Geometry(straighten = 1f),
            Geometry(aspect = -1f),
            Geometry(horizontal = 1f),
            Geometry(vertical = -1f),
            Geometry(distortion = 1f),
            Geometry(distortion = -1f),
            Geometry(straighten = 0.4f, aspect = 0.3f, horizontal = -0.6f, vertical = 0.5f, distortion = 0.7f)
        )
        for (geo in prove) {
            val piano = Warp.plan(geo, cx = 60f, cy = 45f, w = 120f, h = 90f)
            for (x in listOf(5f, 60f, 115f)) {
                for (y in listOf(5f, 45f, 85f)) {
                    val p = piano.map(x, y)
                    val q = piano.back(p[0], p[1])
                    assertEquals("$geo in x", x, q[0], 0.02f)
                    assertEquals("$geo in y", y, q[1], 0.02f)
                }
            }
        }
    }

    /**
     * **Caso 27: la scala di copertura non lascia bordi scoperti.**
     *
     * ⚠️⚠️ **SENZA DI LEI UN RADDRIZZAMENTO LASCEREBBE QUATTRO CUNEI VUOTI AGLI ANGOLI**, che è
     * quello che si vede in ogni editor che quel conto non ce l'ha. La misura è al rovescio, e per
     * questo vale: si prende il **contorno del rettangolo di arrivo** e si guarda da dove viene
     * ogni suo punto; se viene da dentro il rettangolo di partenza, là c'è un pixel da disegnare.
     * ⚠️ **La controprova è dentro la prova**: lo stesso piano con `cover = 1` lascia scoperti dei
     * punti, cioè senza quel conto il difetto ci sarebbe.
     * ⚠️ **Si prova anche col barile**, dove il punto più rientrato non è un angolo ma il mezzo di
     * un lato: è il caso che i quattro angoli da soli non prenderebbero.
     * ⚠️⚠️ **LA TOLLERANZA È MEZZO PIXEL E NON UN MILLESIMO, ED È LA MISURA GIUSTA**: quattro
     * punti stanno **sul bordo per costruzione**, perché la scala è la più piccola che copre, e là
     * l'errore del campionamento del contorno li porta fuori di qualche centesimo. Un bordo
     * scoperto si vede da un pixel in su, e la controprova ne misura sette.
     */
    @Test
    fun `la copertura non lascia bordi vuoti`() {
        for (geo in listOf(
            Geometry(straighten = 1f),
            Geometry(distortion = -1f),
            Geometry(horizontal = 0.8f, vertical = -0.8f)
        )) {
            val piano = Warp.plan(geo, cx = 60f, cy = 45f, w = 120f, h = 90f)
            assertTrue("$geo doveva ingrandire", piano.cover > 1f)
            assertEquals("$geo: un bordo è rimasto scoperto", 0, scoperti(piano))
            assertTrue(
                "$geo: senza copertura il difetto doveva vedersi",
                scoperti(senzaCopertura(piano)) > 0
            )
        }
    }

    /**
     * **Caso 29: un keystone tiene il centro, e apre i due lati in modo simmetrico.**
     *
     * ⚠️⚠️ **È LA FORMA ESATTA DEL SUO RISCONTRO** (giro della `2.29`, voce `geo-dritto` non
     * approvata: *dovrebbero avere come perno una retta che rimane al centro, anziché un appoggio
     * laterale*). Il difetto non era la deformazione ma **dove** finiva: con la divisione
     * prospettica unica della `2.29` il trapezio scivolava tutto da una parte, e dopo la scala di
     * copertura l'immagine sembrava appoggiata a un bordo.
     * ⚠️ **Si guarda il piano SENZA copertura**, perché quella scala nasconderebbe metà della
     * misura riportando il disegno dentro il riquadro.
     * ⚠️ **Il centro dei quattro vertici è la misura giusta**, e non il centro dell'immagine: un
     * trapezio isoscele ha là il suo baricentro, e uno scivolato no.
     * ⚠️ **Controprovata** rimettendo la divisione unica: il centro cade 12,7 pixel più su, su 90
     * di altezza.
     */
    @Test
    fun `il keystone tiene il centro e apre i due lati alla pari`() {
        val w = 120f
        val h = 90f
        for (geo in listOf(Geometry(vertical = 1f), Geometry(horizontal = -1f))) {
            val angoli = quattroAngoli(geo, w, h)
            assertEquals(
                "$geo: il centro dei quattro vertici doveva restare il centro, in orizzontale",
                60f, angoli.map { it[0] }.average().toFloat(), 0.01f
            )
            assertEquals(
                "$geo: e in verticale",
                45f, angoli.map { it[1] }.average().toFloat(), 0.01f
            )
        }

        /*
         * ⚠️ **E il numero si legge direttamente**: col cursore a fondo corsa il lato che si apre
         * vale `1 + SLANT` e quello che si stringe `1 - SLANT`, cioè il coefficiente è la frazione
         * di cui i due lati si muovono. Fino alla `2.29` erano +54% e -26%, che è la stessa
         * asimmetria vista dall'altra parte.
         */
        val angoli = quattroAngoli(Geometry(vertical = 1f), w, h)
        assertEquals(
            "il lato che si apre doveva valere 1 + SLANT",
            w * (1f + Warp.SLANT), angoli[1][0] - angoli[0][0], 0.02f
        )
        assertEquals(
            "quello che si stringe, 1 - SLANT",
            w * (1f - Warp.SLANT), angoli[2][0] - angoli[3][0], 0.02f
        )
        assertEquals("e l'altezza non si tocca", 0f, angoli[0][1], 0.02f)
        assertEquals("nemmeno di sotto", h, angoli[2][1], 0.02f)
    }

    /**
     * **Caso 30: la lente del colore mirato inquadra il punto toccato, con la stessa
     * deformazione.**
     *
     * ⚠️⚠️ **È LA SUA SEGNALAZIONE** (giro della `2.29`, voce `geo-mirato` non approvata: *Il punto
     * non è quello giusto, si vede l'immagine prima della distorsione*), e quello che il banco può
     * misurarne è il conto su cui la lente si regge: la deformazione è **invariante per
     * similitudine**, quindi costruita sul riquadro ingrandito attorno al dito posa il pixel
     * toccato esattamente al centro del tondo.
     * ⚠️ **Senza quella proprietà la lente non si poteva fare così**, e la `2.29` infatti aveva
     * preso l'altra strada: disegnare l'immagine non deformata e spostare l'inquadratura sul punto
     * sorgente, cioè mostrare un'altra immagine.
     * ⚠️ **Controprovata** costruendo il riquadro attorno al punto sorgente, come faceva la `2.29`:
     * il pixel toccato cade lontano dal centro.
     */
    @Test
    fun `la lente inquadra il punto toccato con la stessa deformazione`() {
        val geo = Geometry(
            straighten = 0.5f, aspect = 0.2f, horizontal = -0.4f, vertical = 0.6f, distortion = 0.5f
        )
        val l = 0f
        val t = 0f
        val r = 120f
        val b = 90f
        val piano = Warp.plan(geo, (l + r) / 2f, (t + b) / 2f, r - l, b - t)

        // Il dito da qualche parte sul palco, il tondo della lente sopra di lui, e il riquadro
        // dell'immagine ingrandito di `k` attorno al dito: è il conto di `AdvancedEditorScreen`.
        val k = 6f
        val ditoX = 80f
        val ditoY = 30f
        val cx = 200f
        val cy = 150f
        val vl = cx + (l - ditoX) * k
        val vt = cy + (t - ditoY) * k
        val lente = Warp.plan(
            geo,
            cx + ((l + r) / 2f - ditoX) * k,
            cy + ((t + b) / 2f - ditoY) * k,
            (r - l) * k,
            (b - t) * k
        )

        val fonte = piano.back(ditoX, ditoY)
        val posato = lente.map(cx + (fonte[0] - ditoX) * k, cy + (fonte[1] - ditoY) * k)
        assertEquals("il pixel toccato doveva cadere al centro del tondo", cx, posato[0], 0.1f)
        assertEquals("e in verticale", cy, posato[1], 0.1f)

        // E non solo al centro: dentro il tondo la deformazione è quella del palco, scalata.
        for (x in listOf(10f, 60f, 110f)) {
            for (y in listOf(10f, 45f, 80f)) {
                val sul = piano.map(x, y)
                val nel = lente.map(vl + (x - l) * k, vt + (y - t) * k)
                assertEquals("in ($x, $y)", cx + (sul[0] - ditoX) * k, nel[0], 0.1f)
                assertEquals("in ($x, $y)", cy + (sul[1] - ditoY) * k, nel[1], 0.1f)
            }
        }

        /*
         * ⚠️ **La controprova vive dentro la prova**: col riquadro costruito attorno al punto
         * SORGENTE, che è la strada della `2.29`, il pixel toccato non cade più al centro del
         * tondo. Cioè la misura qui sopra distingue le due scelte invece di essere vera comunque.
         */
        val comeAllora = Warp.plan(
            geo,
            cx + ((l + r) / 2f - fonte[0]) * k,
            cy + ((t + b) / 2f - fonte[1]) * k,
            (r - l) * k,
            (b - t) * k
        )
        val storto = comeAllora.map(cx, cy)
        assertTrue(
            "ancorata al punto sorgente la lente doveva sbagliare bersaglio",
            abs(storto[0] - cx) + abs(storto[1] - cy) > 1f
        )
    }

    /**
     * **Caso 31: la geometria non esce dal riquadro dell'immagine.**
     *
     * ⚠️⚠️ **È L'ALTRA METÀ DEL SUO RISCONTRO** (giro della `2.29`, voce `geo-dritto`: *anche il
     * ritaglio per non lasciare angoli vuoti dovrebbe vedersi in tempo reale*). La scala di
     * copertura c'era già, ma sul palco la maglia si disegnava senza confini: ingrandita per
     * coprire, finiva sul fondo intorno all'immagine, mentre il salvataggio disegna dentro un
     * bitmap grande quanto l'originale, cioè taglia. Si vedeva una cosa e se ne salvava un'altra.
     * ⚠️ **Si guarda un pixel del FONDO accanto all'immagine**: qui l'anteprima è quadrata e il
     * palco è più largo, quindi ai fianchi resta una banda, e senza il ritaglio il raddrizzamento
     * a fondo corsa ci arriva sopra (la copertura vale circa 1,34, cioè sfora del 17% del lato).
     * ⚠️ **Controprovata** togliendo il `clipRect`: quel pixel diventa l'immagine.
     */
    @Test
    fun `la geometria non deborda dal riquadro dell'immagine`() {
        banco.setContent { Scena() }
        pronta()
        /*
         * ⚠️⚠️ **AL MODULO SI PASSA PRIMA DELLO SCATTO A RIPOSO, E IL BANCO LO HA IMPOSTO**: la
         * scheda è alta quanto il suo contenuto, e la Geometria ha un cursore in meno della Luce,
         * quindi cambiando modulo il palco si allunga e l'immagine adattata cresce. Scattando prima
         * si confrontavano due scene diverse, e la prova falliva **col codice giusto**.
         */
        banco.onNodeWithContentDescription(testo(R.string.look_geometry)).performClick()
        banco.waitForIdle()

        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        val riposo = palco.captureToImage().toPixelMap()
        val riga = riposo.height / 2
        /*
         * ⚠️ **Il bordo dell'immagine si TROVA nei pixel invece di calcolarlo**: l'anteprima è un
         * quadrato bianco su fondo, e dove cominci dipende da quanto spazio la scheda lascia al
         * palco. Un conto scritto qui direbbe il vero finché la scheda non cambia di un cursore.
         */
        val fondo = riposo[0, riga]
        val bordo = (0 until riposo.width).firstOrNull { riposo[it, riga] != fondo } ?: 0
        assertTrue("serve una banda di fondo accanto all'immagine", bordo > 8)
        val spia = bordo - 4

        /*
         * ⚠️ **Il cursore è 'Proporzioni' e non il raddrizzamento**: quello sporge di un decimo di
         * pixel alla riga di mezzo (la rotazione muove gli angoli, non i fianchi), mentre l'aspetto
         * allarga del 25% e la copertura ne aggiunge altrettanto, cioè una dozzina di pixel qui.
         * Una prova che non può vedere il difetto è peggio del niente.
         */
        muovi(1, 1f)

        assertEquals(
            "l'immagine deformata doveva restare dentro il suo riquadro",
            fondo,
            palco.captureToImage().toPixelMap()[spia, riga]
        )
    }

    /** I quattro vertici del rettangolo deformato, in senso orario da quello in alto a sinistra. */
    private fun quattroAngoli(geo: Geometry, w: Float, h: Float): List<FloatArray> {
        val nudo = senzaCopertura(Warp.plan(geo, cx = w / 2f, cy = h / 2f, w = w, h = h))
        return listOf(nudo.map(0f, 0f), nudo.map(w, 0f), nudo.map(w, h), nudo.map(0f, h))
    }

    /**
     * Lo stesso piano senza la scala di copertura, cioè la sola deformazione.
     *
     * ⚠️ **Serve a guardare quello che la copertura poi nasconde**: quella scala riporta il disegno
     * dentro il riquadro, quindi una misura fatta dopo di lei non distingue un trapezio centrato da
     * uno scivolato.
     */
    private fun senzaCopertura(piano: WarpPlan) = WarpPlan(
        half = piano.half, cx = piano.cx, cy = piano.cy, ax = piano.ax, ay = piano.ay,
        cosT = piano.cosT, sinT = piano.sinT, stretch = piano.stretch,
        slantX = piano.slantX, slantY = piano.slantY, bend = piano.bend, cover = 1f
    )

    /**
     * **Caso 28: il sesto modulo porta i suoi cinque cursori, azzera solo i suoi, e toglie il
     * senza perdita.**
     *
     * ⚠️ **La geometria ricampiona per definizione**, quindi appena un suo cursore si muove il file
     * va riscritto: dichiararsi senza perdita qui vorrebbe dire salvare un'immagine diversa da
     * quella che si vede.
     */
    @Test
    fun `il modulo geometria porta i cinque cursori e ricampiona`() {
        assertTrue(Look(geo = Geometry(straighten = 0.2f)).lossless.not())
        assertFalse(Geometry(distortion = 0.01f).idle)

        banco.setContent { Scena() }
        pronta()
        muovi(1, 0.5f)
        assertTrue(valore(1) > 0.2f)

        /*
         * ⚠️⚠️ **IL GETTONE SI CERCA PER DESCRIZIONE, DALLA `2.31`**: la fila è di icone, quindi il
         * nome del modulo non è più un testo in scena ma quello che il gettone **annuncia**. ⚠️ E
         * non serve più scorrere: fino alla `2.30` la fila scorreva in orizzontale e il sesto nome
         * cadeva fuori dalla larghezza del banco, cioè il tocco non cambiava modulo senza dare
         * nessun errore, e si contavano i sei cursori della Luce credendo di guardare la Geometria.
         */
        banco.onNodeWithContentDescription(testo(R.string.look_geometry)).performClick()
        banco.waitForIdle()
        assertEquals(5, quantiCursori())
        assertEquals(0, quanteFasce())

        muovi(0, 0.6f)
        assertTrue(valore(0) > 0.2f)
        banco.onNodeWithContentDescription(testo(R.string.look_geometry)).performTouchInput { longClick() }
        banco.waitForIdle()
        assertEquals("la geometria doveva azzerarsi", 0f, valore(0), 1e-3f)

        banco.onNodeWithContentDescription(testo(R.string.look_light)).performClick()
        banco.waitForIdle()
        assertTrue("la luce non doveva essere toccata", valore(1) > 0.2f)
    }

    /**
     * **Caso 32: una posa porta con sé il rettangolo di ritaglio.**
     *
     * ⚠️⚠️ **È LA COSA CHE SI ROMPE IN SILENZIO**, e per questo la prova nasce con la funzione: il
     * rettangolo è in frazioni dell'immagine **già posata**, quindi un quarto di giro che non lo
     * riscrivesse lo lascerebbe dov'è sullo schermo, cioè su un'altra porzione di fotografia.
     * Niente errore, niente segno: si vede solo con un ritaglio già fatto.
     * ⚠️ **La controprova vive dentro la prova**: la metà sinistra deve diventare la metà **di
     * sopra**, che è quello che un rettangolo fermo non farebbe.
     */
    @Test
    fun `una posa porta con sé il rettangolo di ritaglio`() {
        val sinistra = ImageEdit.Crop(0f, 0f, 0.5f, 1f)
        val fermo = Look(crop = sinistra)

        val giro = spunLook(fermo, Spin(1, false))
        assertEquals(Spin(1, false), giro.spin)
        assertEquals("girando in orario la metà sinistra va in alto", 0f, giro.crop.top, 1e-4f)
        assertEquals(0.5f, giro.crop.bottom, 1e-4f)
        assertEquals(0f, giro.crop.left, 1e-4f)
        assertEquals(1f, giro.crop.right, 1e-4f)

        val specchio = spunLook(fermo, Spin.ACROSS)
        assertEquals("riflettendo la metà sinistra va a destra", 0.5f, specchio.crop.left, 1e-4f)
        assertEquals(1f, specchio.crop.right, 1e-4f)

        // Quattro quarti di giro riportano tutto al punto di partenza, posa e rettangolo.
        var tondo = fermo
        repeat(4) { tondo = spunLook(tondo, Spin(1, false)) }
        assertEquals(Spin.STILL, tondo.spin)
        assertEquals(sinistra.left, tondo.crop.left, 1e-4f)
        assertEquals(sinistra.right, tondo.crop.right, 1e-4f)
    }

    /**
     * **Caso 33: che cosa toglie il riposo e che cosa toglie il senza perdita.**
     *
     * ⚠️⚠️ **LE DUE DOMANDE NON SONO LA STESSA, ED È LA CLAUSOLA DELL'UTENTE** (*quelle che non
     * prevedono la riscrittura del file pixel per pixel devono essere lossless*): una **posa** si
     * scrive in un tag EXIF, quindi l'immagine è cambiata ma il file non si riscrive; un
     * **ritaglio** toglie dei pixel, quindi va riscritto. Confonderle vorrebbe dire ricomprimere
     * una fotografia per averla girata, che è proprio quello che l'editor di casa non fa dalla
     * `1.03`.
     */
    @Test
    fun `la posa resta senza perdita e il ritaglio no`() {
        assertTrue(Look.NONE.idle)
        assertTrue(Look.NONE.lossless)

        val girato = Look(spin = Spin(1, false))
        assertFalse("una posa è una modifica", girato.idle)
        assertTrue("una posa non riscrive i pixel", girato.lossless)

        val tagliato = Look(crop = ImageEdit.Crop(0.1f, 0.1f, 0.9f, 0.9f))
        assertFalse(tagliato.idle)
        assertFalse("un ritaglio riscrive i pixel", tagliato.lossless)

        assertFalse(Look(spin = Spin.ACROSS, light = Light(contrast = 0.2f)).lossless)
    }

    /**
     * **Caso 34: la fila dei moduli parla per icone, comincia dal Ritaglio e si apre sulla Luce.**
     *
     * ⚠️⚠️ **L'ORDINE E L'APERTURA SONO SUOI, E SONO DUE COSE DIVERSE** (campo libero del giro
     * della `2.29`: *`Geometria` dev'essere il secondo modulo; il primo dev'essere `Ritaglio` ... Il
     * terzo (ma attivo di default) 'Luce'*): la fila è l'ordine in cui si lavora, l'apertura è dove
     * si lavora quasi sempre. Scritte in un posto solo, la seconda seguirebbe la prima.
     * ⚠️ **Che i gettoni siano muti si misura**, perché è la ragione per cui la fila non scorre più:
     * con le parole i sette non entrano nella larghezza, e la `2.23` teneva metà dei moduli fuori
     * dallo schermo per chi non sa che si scorre.
     */
    @Test
    fun `i moduli sono icone che si annunciano, e si apre la luce`() {
        banco.setContent { Scena() }
        pronta()

        for (nome in MODULI) {
            assertEquals("il gettone di ${testo(nome)} deve annunciarsi", 1, quantiDetti(nome))
            assertEquals("il gettone di ${testo(nome)} non deve scrivere", 0, quanti(testo(nome)))
        }
        assertEquals("di fabbrica si guarda la Luce, coi suoi sei cursori", 6, quantiCursori())
    }

    /**
     * **Caso 35: il modulo Ritaglio porta i tre comandi di posa e nessun cursore.**
     *
     * ⚠️ **I tre tasti sono quelli dell'editor di casa**, stesse etichette comprese: due segni per
     * lo stesso gesto a un tocco di distanza sarebbero due cose da imparare, visto che dalla stessa
     * immagine si entra nell'uno o nell'altro editor.
     * ⚠️ **La controprova è nella Luce**, dove quei tasti non ci sono: senza di lei la misura
     * direbbe soltanto che tre parole esistono da qualche parte nella scheda.
     */
    @Test
    fun `il modulo ritaglio porta la posa e nessun cursore`() {
        banco.setContent { Scena() }
        pronta()

        for (id in POSA) assertEquals("nella Luce la posa non c'è", 0, quantiDetti(id))

        banco.onNodeWithContentDescription(testo(R.string.look_crop)).performClick()
        banco.waitForIdle()

        assertEquals("il Ritaglio non ha cursori", 0, quantiCursori())
        assertEquals(0, quanteFasce())
        for (id in POSA) {
            assertTrue(
                "manca il comando ${testo(id)}",
                quantiDetti(id) + quanti(testo(id)) > 0
            )
        }
    }

    /**
     * **Caso 36: col Ritaglio in scena il dito tira le squadrette, e il palco lo mostra.**
     *
     * ⚠️⚠️ **È IL PRIMO MODULO CHE PRENDE IL DITO SULL'IMMAGINE, E QUELLO CHE PUÒ ROMPERSI È IL
     * COLLEGAMENTO**: il rettangolo, il gesto e il disegno vivono in tre posti (il modello, il
     * palco, `cropOverlay`), e se il parametro non arrivasse il codice compilerebbe lo stesso e il
     * palco non risponderebbe. È lo stesso genere di difetto del velo mancante.
     * ⚠️ **Si guardano i pixel e non lo stato**, perché quello che deve cambiare è il disegno: il
     * velo copre la parte esclusa, quindi dopo il gesto una fetta di immagine si scurisce.
     * ⚠️ **L'angolo si TROVA nei pixel**: dove cominci l'immagine dipende da quanto spazio la scheda
     * lascia al palco, e un conto scritto qui direbbe il vero fino al primo cursore in più.
     */
    @Test
    fun `il ritaglio si tira col dito sul palco`() {
        banco.setContent { Scena() }
        pronta()
        banco.onNodeWithContentDescription(testo(R.string.look_crop)).performClick()
        banco.waitForIdle()

        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        val riposo = palco.captureToImage().toPixelMap()
        val riga = riposo.height / 2
        val fondo = riposo[0, riga]
        val bordo = (0 until riposo.width).firstOrNull { riposo[it, riga] != fondo } ?: 0
        assertTrue("serve una banda di fondo accanto all'immagine", bordo > 4)

        /*
         * ⚠️⚠️ **I TRE MOMENTI DEL DITO VANNO IN TRE CHIAMATE, ED È IL BANCO CHE LO HA IMPOSTO**:
         * scritti in un blocco solo, il movimento e il distacco arrivano insieme e a `drag` resta
         * un evento con delta **zero**, cioè il rettangolo non si muove di un pixel. Misurato con
         * una spia dentro il gesto, e la prima stesura era rossa col codice giusto.
         */
        palco.performTouchInput { down(Offset(bordo + 2f, 2f)) }
        banco.waitForIdle()
        palco.performTouchInput { moveTo(Offset(width / 2f, height / 2f)) }
        banco.waitForIdle()

        val tirato = palco.captureToImage().toPixelMap()
        palco.performTouchInput { up() }
        banco.waitForIdle()

        assertTrue(
            "tirando la squadretta il palco doveva cambiare disegno",
            diversi(riposo, tirato) > 0
        )
    }

    /**
     * Quanti punti del contorno del rettangolo di arrivo vengono da **fuori** dell'immagine, cioè
     * quanti pixel resterebbero scoperti: vedi il caso 27.
     */
    private fun scoperti(piano: WarpPlan): Int {
        val w = piano.ax * piano.half
        val h = piano.ay * piano.half
        var conto = 0
        for (i in 0..40) {
            val t = i / 40f
            val xs = piano.cx - w + 2f * w * t
            val ys = piano.cy - h + 2f * h * t
            for (p in listOf(
                piano.back(xs, piano.cy - h),
                piano.back(xs, piano.cy + h),
                piano.back(piano.cx - w, ys),
                piano.back(piano.cx + w, ys)
            )) {
                val fuori = abs(p[0] - piano.cx) > w + 0.5f || abs(p[1] - piano.cy) > h + 0.5f
                if (fuori) conto += 1
            }
        }
        return conto
    }

    /** Quanti cursori sono in scena, contati dalla loro azione semantica. */
    private fun quantiCursori(): Int =
        banco.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .fetchSemanticsNodes().size

    /**
     * Il punto medio, in orizzontale, dei pixel cambiati fra due scatti: `0` se non ne è cambiato
     * nessuno.
     *
     * ⚠️ **Dice DOVE è successo qualcosa**, che è la sola misura che distingue una lente che si
     * sposta da una lente ferma con dentro qualcosa che si muove.
     */
    private fun centroX(a: PixelMap, b: PixelMap): Float {
        var somma = 0f
        var conto = 0
        for (y in 0 until minOf(a.height, b.height)) {
            for (x in 0 until minOf(a.width, b.width)) {
                if (a[x, y] != b[x, y]) {
                    somma += x
                    conto += 1
                }
            }
        }
        return if (conto == 0) 0f else somma / conto
    }

    /** Quanti pixel cambiano fra due scatti dello stesso nodo. */
    private fun diversi(a: PixelMap, b: PixelMap): Int {
        var conto = 0
        for (y in 0 until minOf(a.height, b.height)) {
            for (x in 0 until minOf(a.width, b.width)) {
                if (a[x, y] != b[x, y]) conto += 1
            }
        }
        return conto
    }

    /** Quanti nodi portano questa descrizione parlata. */
    private fun quantiDetti(id: Int): Int =
        banco.onAllNodesWithContentDescription(testo(id)).fetchSemanticsNodes().size

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

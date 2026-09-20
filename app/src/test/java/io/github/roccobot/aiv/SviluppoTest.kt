package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.runBlocking
import org.junit.Before

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
    R.string.look_tone,
    R.string.look_detail,
    R.string.look_presets
)

/**
 * I cinque comandi di posa del modulo Ritaglio, che sono quelli dell'editor di casa.
 *
 * ⚠️ **Erano tre fino alla `2.31`**, e le due centrature sono entrate con i formati: senza una forma
 * scelta non avevano niente da centrare.
 */
private val POSA = listOf(
    R.string.editor_center_across,
    R.string.editor_center_down,
    R.string.editor_flip,
    R.string.editor_left,
    R.string.editor_right
)

/** Le sei forme del ritaglio: le due che si dicono a parole, e le quattro proporzioni. */
private val FORME = listOf("1:1", "2:3", "3:4", "9:16")

/**
 * I quattro comandi che vivono nel modulo Ritaglio dalla `2.40`, cioè la sua storia.
 *
 * ⚠️ **'Applica' è fra loro e non più nella fila di fondo**: la fila in fondo alla scheda porta i
 * comandi dell'**immagine**, questi parlano del solo ritaglio.
 */
private val CROP_CMD = listOf(
    R.string.editor_crop_back,
    R.string.editor_crop_on,
    R.string.editor_apply,
    R.string.editor_crop_clear
)

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
     * ⚠️⚠️ **IL MINI-ONBOARDING DEI MODULI SI SPEGNE PRIMA, DALLA `2.50`**: il suo velo copre lo
     * schermo e **consuma il primo tocco**, che è quello che deve fare davanti a chi apre l'editor
     * la prima volta e quello che rende rossa qualunque prova che tocchi un gettone. È la stessa
     * riga che [NascosteTest] scrive per la scorciatoia delle colonne.
     */
    @Before
    fun senzaOnboarding() {
        runBlocking {
            Hint.MODULES.remember(ApplicationProvider.getApplicationContext())
            Hint.EDITOR_TOOLS.remember(ApplicationProvider.getApplicationContext())
        }
    }


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
        modulo(R.string.look_light)

        banco.onNodeWithText(testo(R.string.look_exposure)).assertExists()
        assertEquals(0, quanti(testo(R.string.look_saturation)))

        banco.onNodeWithContentDescription(testo(R.string.look_color)).performClick()
        banco.waitForIdle()

        banco.onNodeWithText(testo(R.string.look_saturation)).assertExists()
        assertEquals(0, quanti(testo(R.string.look_exposure)))
    }

    /**
     * **'Applica' fa inquadrare al palco il taglio tenuto, e 'Annulla' lo disfa.**
     *
     * ⚠️⚠️ **È SUA RICHIESTA E LA SUA SCELTA FRA DUE LETTURE** (2026-09-13: *manca 'Applica' per
     * il ritaglio*, e poi *il palco passa a inquadrare la porzione scelta, e negli altri moduli
     * si lavora su quella*). Fino alla `2.32` il rettangolo si tirava e non si vedeva applicato
     * mai: il taglio compariva solo nel file salvato.
     *
     * ⚠️ **Si misura a pixel perché non c'è altro da guardare**: quello che cambia è **dove**
     * l'immagine è disegnata, e nessuna misura di struttura lo vede.
     * ⚠️ **La scena è grande**, come le altre prove che tirano una squadretta: vedi la nota là.
     */
    @Test
    @Config(qualifiers = "w600dp-h900dp")
    fun `applica fa inquadrare il taglio e annulla lo disfa`() {
        banco.setContent { Scena() }
        pronta()
        banco.onNodeWithContentDescription(testo(R.string.look_crop)).performClick()
        banco.waitForIdle()
        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))

        // Si tira una squadretta, come farebbe un dito: i tre momenti in tre chiamate.
        val riposo = palco.captureToImage().toPixelMap()
        val riga = riposo.height / 2
        val fondo = riposo[0, riga]
        val bordo = (0 until riposo.width).firstOrNull { riposo[it, riga] != fondo } ?: 0
        palco.performTouchInput { down(Offset(bordo + 2f, 2f)) }
        banco.waitForIdle()
        palco.performTouchInput { moveTo(Offset(width / 3f, height / 3f)) }
        banco.waitForIdle()
        palco.performTouchInput { up() }
        banco.waitForIdle()

        /*
         * ⚠️ **Il tasto vive nel solo Ritaglio**, come 'Mirato' nel solo HSL: in un altro modulo
         * non deve esserci, o prometterebbe un comando che quel modulo non ha.
         */
        banco.onNodeWithContentDescription(testo(R.string.editor_apply)).assertExists()
        banco.onNodeWithContentDescription(testo(R.string.look_light)).performClick()
        banco.waitForIdle()
        assertEquals(0, quantiDetti(R.string.editor_apply))
        val intera = palco.captureToImage().toPixelMap()

        banco.onNodeWithContentDescription(testo(R.string.look_crop)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(testo(R.string.editor_apply)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(testo(R.string.look_light)).performClick()
        banco.waitForIdle()
        val tagliata = palco.captureToImage().toPixelMap()
        assertTrue(
            "col taglio confermato il palco deve inquadrare la porzione",
            diversi(intera, tagliata) > 0
        )

        /*
         * ⚠️ **'Annulla' lo disfa come ogni altro passo**, ed è la ragione per cui la vista
         * confermata vive nel modello invece che nello sguardo: senza, il tasto non avrebbe
         * niente da riportare indietro.
         */
        banco.onNodeWithContentDescription(testo(R.string.editor_undo)).performClick()
        banco.waitForIdle()
        assertEquals(
            "dopo 'Annulla' si torna a vedere l'immagine intera",
            0,
            diversi(intera, palco.captureToImage().toPixelMap())
        )
    }

    /**
     * **Tirare una squadretta non fa cadere l'app quando il riquadro è più piccolo del lato
     * minimo.**
     *
     * ⚠️⚠️ **L'HA TROVATO IL BANCO, ED È UN DIFETTO VERO E NON UN LIMITE DELLA SCENA**: con un
     * riquadro più basso del lato minimo (un'immagine molto allungata, o un palco corto)
     * `frame.top .. bottom - small` è un intervallo **vuoto**, e `coerceIn` su un intervallo
     * rovesciato lancia. Quello che si vede è l'app che cade in mano a chi sta ritagliando, e
     * nessun compilatore lo poteva dire.
     *
     * ⚠️ **I due casi sono diversi**: il primo è il riquadro più piccolo del lato minimo, il
     * secondo è un dito che porta una squadretta molto oltre il bordo opposto, e la prima
     * correzione da sola non lo copre.
     */
    @Test
    fun `tirare una squadretta non cade su un riquadro basso`() {
        val stretto = Rect(0f, 0f, 300f, 100f)
        val tutto = Rect(0f, 0f, 300f, 100f)
        val giu = dragged(tutto, Grab.TOP_LEFT, Offset(10f, 10f), stretto, null, 122f)
        assertTrue("il rettangolo deve restare valido", giu.width >= 0f && giu.height >= 0f)

        val largo = Rect(0f, 0f, 300f, 300f)
        val dentro = Rect(50f, 50f, 250f, 250f)
        val oltre = dragged(dentro, Grab.TOP_LEFT, Offset(400f, 400f), largo, null, 40f)
        assertTrue("nemmeno tirando oltre il bordo opposto", oltre.width >= 0f)
    }

    /**
     * **La scheda non cambia altezza passando da un modulo all'altro, e si misura sul palco.**
     *
     * ⚠️⚠️ **È SUA RICHIESTA** (2026-09-13: *voglio che la bottomsheet dell'editor completo sia
     * sempre alta uguale: non deve ballare da un modulo all'altro*). La scheda e il palco si
     * dividono lo schermo, quindi l'altezza dell'immagine è la stessa misura letta dalla parte in
     * cui si guarda: se la scheda si allunga, il palco si accorcia di altrettanto.
     *
     * ⚠️ **Controprovata togliendo `SteadyBody`**: fra la Luce, che ha sei cursori, e le Curve,
     * che hanno un grafico, il palco cambiava di centinaia di pixel.
     */
    @Test
    fun `la scheda non cambia altezza cambiando modulo`() {
        banco.setContent { Scena() }
        pronta()

        val quanto = altezzaPalco()
        assertTrue(quanto > 0)
        for (modulo in listOf(
            R.string.look_crop,
            R.string.look_geometry,
            R.string.look_color,
            R.string.look_mix,
            R.string.look_tone,
            R.string.look_detail,
            R.string.look_light
        )) {
            banco.onNodeWithContentDescription(testo(modulo)).performClick()
            banco.waitForIdle()
            assertEquals(quanto, altezzaPalco())
        }
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
        modulo(R.string.look_light)

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
        modulo(R.string.look_light)
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
        // ⚠️ Dalla `2.55` le Curve sono penultime, quindi il loro gettone va portato in testa o
        // cade fuori dalla larghezza del banco: il perché vive su [davanti].
        banco.setContent { Scena(mods = davanti(PadKey.MOD_TONE)) }
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
     * **Caso 22: il colore mirato si offre nel solo modulo che ha un bersaglio, e come icona.**
     *
     * ⚠️⚠️ **È LA CONDIZIONE CHE TIENE VIVO IL PALCO**: quel tasto arma una modalità in cui il
     * palco fa solo il mirato, e in un modulo senza bersaglio sarebbe una modalità che non fa
     * niente mentre spegne pinza e doppio tocco.
     * ⚠️⚠️ **DALLA `2.32` LE CURVE NON MIRANO PIÙ, ED È LA SUA RISPOSTA `via` A `d-mirato-curve`**
     * (giro della `2.31`: *via, e si torna subito a zoomare/spostare l'immagine toccandola*). La
     * prova guarda **anche** quel modulo, perché una condizione che si allarga da sé non darebbe
     * nessun errore.
     * ⚠️ **Il nome non si scrive più**, perché il comando è un'icona: resta come descrizione, che è
     * quello che un lettore di schermo annuncia, ed è lo stesso criterio dei sette gettoni.
     */
    @Test
    fun `il mirato c'è nel solo hsl e si annuncia senza scriversi`() {
        banco.setContent { Scena() }
        pronta()
        assertEquals("nella Luce non c'è niente da mirare", 0, quantiDetti(R.string.look_target))

        banco.onNodeWithContentDescription(testo(R.string.look_tone)).performClick()
        banco.waitForIdle()
        assertEquals("dalla 2.32 le Curve non mirano", 0, quantiDetti(R.string.look_target))

        banco.onNodeWithContentDescription(testo(R.string.look_mix)).performClick()
        banco.waitForIdle()
        assertEquals(1, quantiDetti(R.string.look_target))
        assertEquals("il nome resta detto e non scritto", 0, quanti(testo(R.string.look_target)))

        banco.onNodeWithContentDescription(testo(R.string.look_detail)).performClick()
        banco.waitForIdle()
        assertEquals(0, quantiDetti(R.string.look_target))
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
    private fun senzaCopertura(piano: WarpPlan) = piano.copy(cover = 1f)

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
        modulo(R.string.look_light)
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
    fun `i moduli sono icone che si annunciano, e si apre il ritaglio`() {
        banco.setContent { Scena() }
        pronta()

        for (nome in MODULI) {
            assertEquals("il gettone di ${testo(nome)} deve annunciarsi", 1, quantiDetti(nome))
            assertEquals("il gettone di ${testo(nome)} non deve scrivere", 0, quanti(testo(nome)))
        }
        assertEquals("il Ritaglio non ha cursori", 0, quantiCursori())
        assertEquals(
            "di fabbrica si apre il Ritaglio, quindi ci sono i suoi formati",
            1,
            quanti(testo(R.string.editor_shape_original))
        )
        modulo(R.string.look_light)
        assertEquals("e la Luce, toccata, porta i suoi sei", 6, quantiCursori())
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
     * ⚠️⚠️ **LA SCENA È GRANDE, E DALLA `2.33` NON È PIÙ FACOLTATIVO**: da quando la scheda è alta
     * quanto il modulo più alto, sul banco di serie il palco perde una settantina di pixel, e là
     * dentro non ci sta un rettangolo di ritaglio col suo lato minimo. Misura la cosa che deve
     * misurare invece dei limiti di una scena minuscola.
     */
    @Test
    @Config(qualifiers = "w600dp-h900dp")
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
     * **Caso 37: le sei forme del Ritaglio, e 'Originale' che è l'immagine intera.**
     *
     * ⚠️⚠️ **'ORIGINALE' È SUA RICHIESTA** (2026-09-13: *tra i vincoli di proporzione dev'esserci
     * anche 'Originale', ma scelta di default resta 'Libera'*), ed è la sola forma il cui rapporto
     * non è scritto nel codice: lo porta l'immagine. Quello che può rompersi in silenzio è il verso,
     * cioè che nel verso naturale dia il rapporto della fotografia invece del suo reciproco: sul
     * quadrato di prova i due numeri **coincidono**, quindi qui il conto si misura su un'immagine
     * larga, e la scena misura soltanto che i sei gettoni ci siano.
     * ⚠️ **Nel suo verso non toglie niente**, ed è la proprietà da cui dipende il senza perdita: un
     * ritaglio che tagliasse un pixel per un arrotondamento farebbe riscrivere il file a chi ha
     * soltanto scelto 'Originale'.
     */
    @Test
    fun `il ritaglio porta le sei forme e Originale e l'immagine intera`() {
        val largo = 3f / 2f
        assertEquals(largo, Shape.ORIGINAL.value(Lay.WIDE, largo)!!, 1e-4f)
        assertEquals(1f / largo, Shape.ORIGINAL.value(Lay.TALL, largo)!!, 1e-4f)
        assertTrue(
            "nel suo verso 'Originale' non taglia niente",
            Shape.ORIGINAL.fit(largo, Lay.WIDE).whole
        )
        assertFalse(
            "nell'altro verso taglia, come ogni altra forma",
            Shape.ORIGINAL.fit(largo, Lay.TALL).whole
        )
        assertNull("'Libero' non ha un rapporto da tenere", Shape.FREE.value(Lay.WIDE, largo))
        assertNotNull("le due parole sono 'Libero' e 'Originale'", Shape.ORIGINAL.word)
        assertNull("una proporzione si scrive col suo numero", Shape.ONE.word)

        banco.setContent { Scena() }
        pronta()
        /*
         * ⚠️ **Si passa dalla Luce, e dalla `2.35` serve**: l'editor si apre sul Ritaglio (sua
         * istruzione), quindi la controprova 'in un altro modulo i formati non ci sono' vuole un
         * altro modulo davvero aperto.
         */
        modulo(R.string.look_light)
        for (numero in FORME) assertEquals("nella Luce i formati non ci sono", 0, quanti(numero))

        modulo(R.string.look_crop)
        for (numero in FORME) assertEquals("manca il formato $numero", 1, quanti(numero))
        assertEquals(1, quanti(testo(R.string.editor_free)))
        assertEquals(1, quanti(testo(R.string.editor_shape_original)))
    }

    /**
     * **Caso 38: col Ritaglio in scena l'immagine lascia l'aria alle squadrette.**
     *
     * ⚠️⚠️ **È IL SUO RISCONTRO** (giro della `2.31`, voce `crop-modulo` non approvata: *all'avvio
     * del modulo gli angoli di ritaglio non sono del tutto visibili*). Le squadrette si disegnano a
     * cavallo del bordo del rettangolo, quindi metà del loro spessore cade **fuori** dall'immagine:
     * con l'immagine a filo del palco, che ritaglia il proprio contenuto, quella metà spariva.
     * ⚠️⚠️ **SI MISURA DOVE COMINCIA L'IMMAGINE E NON DOVE COMINCIA IL DISEGNO, ED È IL BANCO CHE
     * LO HA IMPOSTO**: nell'aria ci vanno proprio le squadrette, quindi il primo pixel diverso dal
     * fondo è a un passo dal bordo del palco anche quando la correzione c'è, ed è giusto così. La
     * misura è il **bianco** dell'immagine lungo la colonna di mezzo, che con `air` a zero
     * comincerebbe alla riga zero.
     * ⚠️ **E la seconda metà è che sopra quel bianco ci sia la squadretta**: senza, la prova
     * direbbe soltanto che l'immagine è più piccola, non che quello spazio serve a qualcosa.
     */
    /*
     * ⚠️⚠️ **LA SCENA È GRANDE, DALLA `2.40`, E IL BANCO L'HA IMPOSTA**: in quella di serie il palco
     * viene largo 320 e alto una quarantina di punti, quindi l'immagine quadrata occupa una striscia
     * in mezzo e **ogni** sua riga è attraversata dal braccio di una squadretta d'angolo. Con una
     * scena grande esiste una riga libera, che è quello che questa misura cerca.
     */
    @Config(qualifiers = "w600dp-h900dp")
    @Test
    fun `col ritaglio l'immagine lascia l'aria alle squadrette`() {
        banco.setContent { Scena() }
        pronta()
        banco.onNodeWithContentDescription(testo(R.string.look_crop)).performClick()
        banco.waitForIdle()

        val scatto = banco.onNodeWithContentDescription(testo(R.string.look_compare))
            .captureToImage().toPixelMap()
        val fondo = scatto[0, 0]
        /*
         * ⚠️⚠️ **LA COLONNA È A UN QUARTO E NON A METÀ, DALLA `2.40`**: là passa la maniglia del
         * lato di sopra, che si disegna **fuori** dal bordo, quindi il primo pixel diverso dal
         * fondo sarebbe lei e la misura direbbe zero col codice giusto. Vedi la stessa nota su
         * [bordi].
         */
        val mezzo = scatto.width / 4
        val cima = (0 until scatto.height).firstOrNull { scatto[mezzo, it] != fondo }
        assertNotNull("il palco deve disegnare l'immagine", cima)
        assertTrue(
            "l'immagine comincia a filo del palco: le squadrette restano tagliate",
            cima!! >= 4
        )

        val riga = scatto.height / 4
        val sinistra = (0 until scatto.width).firstOrNull { scatto[it, riga] != fondo }
        assertNotNull(sinistra)
        assertTrue(
            "sopra l'immagine deve esserci la squadretta",
            (0 until cima).any { scatto[sinistra!! + 1, it] != fondo }
        )
    }

    /**
     * **Caso 39: il conto di 'Auto', cioè i due estremi e la dominante.**
     *
     * ⚠️⚠️ **È SUA RICHIESTA** (campo libero del giro della `2.31`: *aggiungi un tasto 'Auto' che
     * imita 'Colore automatico' di Photoshop ... dev'essere annullabile*), e quello che il banco può
     * misurare è il conto, che è puro: i pixel arrivano già campionati.
     * ⚠️ **I due numeri attesi vengono dall'inversa dello shader** e non da una taratura: con
     * l'immagine fra il 20% e l'80% della scala, per portare quei due estremi al nero e al bianco i
     * cursori valgono `0,2 / 0,25` e `0,2 / 0,25`, cioè otto decimi di corsa.
     * ⚠️ **La guardia dell'immagine piatta è la seconda metà**: senza, un rettangolo di un colore
     * solo si stirerebbe fino a diventare due colori.
     */
    @Test
    fun `auto porta i due estremi al nero e al bianco`() {
        val scala = IntArray(200) { i ->
            val v = 51 + 153 * i / 199
            Color.rgb(v, v, v)
        }
        val fatto = Auto.tuned(Look.NONE, scala)
        assertEquals("il punto di nero", -0.8f, fatto.light.blacks, 0.05f)
        assertEquals("il punto di bianco", 0.8f, fatto.light.whites, 0.05f)
        assertEquals("un grigio non ha dominante", 0f, fatto.chroma.temp, 0.02f)
        assertEquals(0f, fatto.chroma.tint, 0.02f)
        /*
         * ⚠️⚠️ **QUESTA RIGA È CAMBIATA CON LA `2.34`, ED È UNA DECISIONE E NON UN DIFETTO**: fino
         * alla `2.33` misurava che l'esposizione restasse a zero, perché 'Auto' toccava quattro
         * cursori; dalla sua risposta `piu` a `d-auto-quanto` ne tocca sei. Su questa scala la
         * mediana è già al grigio di mezzo, quindi il guadagno non ha niente da correggere, ed è la
         * misura giusta da fare qui: dice che il conto nuovo non muove quello che è già a posto.
         */
        assertEquals("una mediana già al centro non chiede guadagno", 0f, fatto.light.exposure, 0.1f)

        val piatta = IntArray(100) { Color.rgb(128, 128, 128) }
        val ferma = Auto.tuned(Look.NONE, piatta)
        assertEquals("un'immagine piatta non si stira", 0f, ferma.light.blacks, 1e-4f)
        assertEquals(0f, ferma.light.whites, 1e-4f)

        val caldo = IntArray(200) { i ->
            val v = 51 + 153 * i / 199
            Color.rgb(minOf(255, v + 40), v, v)
        }
        assertTrue(
            "un'immagine calda si raffredda",
            Auto.tuned(Look.NONE, caldo).chroma.temp < -0.05f
        )
    }

    /**
     * **Caso 40: la fila dei moduli segue l'ordine scelto, e li porta tutti e sette.**
     *
     * ⚠️⚠️ **È SUA RICHIESTA** (2026-09-13: *voglio poter ordinare anche i pulsanti dei moduli*), e
     * quello che può rompersi in silenzio è la **copertura**: l'ordine è un elenco di gettoni e i
     * moduli sono una tabella, quindi un modulo nuovo che si dimenticasse di dichiarare la propria
     * chiave sparirebbe dalla fila senza che niente dia errore. La prima misura conta i sette, la
     * seconda guarda che si dispongano dove l'ordine dice.
     * ⚠️ **Si misura la POSIZIONE e non la sequenza dei nodi**: l'albero di Compose li elenca
     * nell'ordine in cui li compone, che è quello dell'ordine, quindi una prova che leggesse quello
     * sarebbe verde anche con una fila disegnata al contrario.
     * ⚠️ **L'ordine di prova è rovesciato** e non spostato di uno: così ogni gettone cambia posto,
     * e la misura non può passare per caso.
     */
    @Test
    fun `la fila dei moduli segue l'ordine scelto`() {
        banco.setContent { Scena(mods = MOD_KEYS.reversed()) }
        pronta()

        for (nome in MODULI) {
            assertEquals("nella fila ci sono tutti e otto", 1, quantiDetti(nome))
        }
        /*
         * ⚠️⚠️ **SI MISURANO I DUE CHE APRONO LA FILA E NON I DUE ESTREMI, DALLA `2.50`**: con
         * l'ottavo gettone la fila **scorre** (sua richiesta: *nel mio caso, con il mio schermo,
         * sarà l'unico a richiedere uno scorrimento a destra*), quindi l'ultimo cade fuori dal
         * viewport e il suo riquadro arriva ritagliato. Rovesciando l'ordine di fabbrica i primi
         * due sono gli Stili e l'HSL, e quelli si vedono sempre.
         */
        val primo = dove(R.string.look_presets)
        val secondo = dove(R.string.look_mix)
        assertTrue(
            "coi gettoni rovesciati gli Stili aprono la fila e l'HSL li segue",
            primo < secondo
        )
        assertEquals("e restano su una riga sola", dove(R.string.look_presets, alto = true),
            dove(R.string.look_mix, alto = true), 1f)
    }

    /**
     * **Caso 41: le due parole e i due versi sopra, i quattro numeri sotto.**
     *
     * ⚠️⚠️ **È IL SUO PUNTO `crop-giu`, DALLA `2.80`** (riscontro del giro dalla `2.75` alla
     * `2.77`, col mockup: *le proporzioni numeriche tutte in una riga*, e *'Orizzontale' e
     * 'Verticale' diventano icone a destra di 'Originale'*). Fino alla `2.79` erano due righe da
     * tre celle uguali, quindi i quattro numeri stavano a cavallo delle due, e i due versi
     * vivevano in una terza riga scritta a parole.
     * ⚠️ **Si misura il bordo di SOPRA di ognuna**, che è la cosa che distingue una riga
     * dall'altra, e non la larghezza: quella dipende da quanto il carattere del banco è stretto,
     * che è il caso dichiarato in `AIV/CLAUDE.md` § '🧪 Quando si scrive una prova, e quando no'.
     * ⚠️⚠️ **E I DUE VERSI SI CERCANO PER DESCRIZIONE PARLATA, che è la cosa che può rompersi in
     * silenzio**: da icone il nome non si legge più a schermo, quindi senza `contentDescription`
     * quel comando è muto per un lettore di schermo e invisibile al banco.
     * ⚠️ Controprovata rimettendo i quattro numeri a cavallo delle due righe: la prima riga ne
     * conta tre invece di due.
     */
    @Test
    fun `le forme del ritaglio vanno su due righe, le parole coi versi`() {
        banco.setContent { Scena() }
        pronta()
        modulo(R.string.look_crop)

        fun cima(nodo: SemanticsNodeInteraction): Float =
            nodo.fetchSemanticsNode().boundsInRoot.top
        val parole = listOf(R.string.editor_free, R.string.editor_shape_original)
            .map { cima(banco.onNodeWithText(testo(it))) }
        val numeri = FORME.map { cima(banco.onNodeWithText(it)) }
        val versi = listOf(R.string.editor_tall, R.string.editor_wide)
            .map { cima(banco.onNodeWithContentDescription(testo(it))) }

        assertEquals("le due parole vivono sulla stessa riga", parole[0], parole[1], 2f)
        for (v in versi) {
            assertEquals("e i due versi vivono lì accanto", parole[0], v, 2f)
        }
        for (n in numeri) {
            assertEquals("i quattro numeri vivono su una riga sola", numeri[0], n, 2f)
        }
        assertTrue(
            "e quella riga viene dopo le parole",
            numeri[0] > parole[0] + 2f
        )

        /*
         * ⚠️ **E il tocco su un'icona sceglie ancora il verso**: da parola a icona quello che
         * cambia è il disegno, e la scelta si legge dalla semantica del chip, che è quella che
         * un lettore di schermo annuncia. Controprovata spegnendo il legame: la scelta non si
         * muove.
         */
        banco.onNodeWithContentDescription(testo(R.string.editor_wide)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(testo(R.string.editor_wide)).assertIsSelected()
        banco.onNodeWithContentDescription(testo(R.string.editor_tall)).assertIsNotSelected()
    }

    /**
     * **Caso 42: 'Auto' sistema anche la luce media, cioè esposizione e contrasto.**
     *
     * ⚠️⚠️ **È LA SUA RISPOSTA `piu` A `d-auto-quanto`** (giro della `2.32`: *che tocchi anche
     * esposizione e contrasto*), e quello che si misura è il verso: una fotografia scura riceve
     * guadagno, una già al centro no, e una addensata attorno al grigio riceve contrasto.
     * ⚠️⚠️ **L'ORDINE DEL CONTO È LA COSA CHE PUÒ ROMPERSI IN SILENZIO**: nella catena
     * l'esposizione viene prima dei punti, quindi i due estremi vanno misurati **dopo** il
     * guadagno. Calcolati prima, su un'immagine scura il punto di bianco resterebbe quello di
     * partenza e l'immagine finirebbe slavata: è il caso della prima misura qui sotto, dove col
     * conto sbagliato `whites` varrebbe quasi tutta la corsa.
     * ⚠️ **Il contrasto non scende sotto zero**, e la rampa uniforme lo controprova: là la
     * dispersione è `0,289`, cioè sopra il bersaglio, e un conto senza quel limite risponderebbe
     * con un numero negativo, cioè spianerebbe una fotografia che non ha nessun difetto.
     */
    @Test
    fun `auto porta la luce media al centro e apre le immagini piatte`() {
        /*
         * Una fotografia scura con qualche alta luce: la massa fra l'8% e il 25% della scala, e
         * una decina di pixel all'85%. È la scena che distingue i due ordini, e per questo ha una
         * coda chiara: con la sola massa scura il punto di bianco andrebbe a fondo corsa in tutti
         * e due i casi, e la misura non direbbe niente.
         */
        val scura = IntArray(200) { i ->
            val v = if (i < 190) 20 + 44 * i / 189 else 204 + (i - 190)
            Color.rgb(v, v, v)
        }
        val aperta = Auto.tuned(Look.NONE, scura)
        assertTrue("una fotografia scura chiede guadagno", aperta.light.exposure > 0.5f)
        assertTrue(
            "il guadagno resta nella corsa del cursore",
            aperta.light.exposure <= Light.EXPOSURE_RANGE
        )
        assertEquals(
            "dopo due stop le alte luci sono già al bianco, quindi il punto non ha da muoversi",
            0f, aperta.light.whites, 0.05f
        )

        // Una fotografia addensata attorno al grigio, coi soli estremi sparsi.
        val molle = IntArray(400) { i ->
            val v = if (i < 8) 8 + 30 * i else 118 + (i % 21)
            Color.rgb(v, v, v)
        }
        val aperto = Auto.tuned(Look.NONE, molle)
        assertTrue("una fotografia piatta chiede contrasto", aperto.light.contrast > 0.05f)

        // La rampa uniforme: già dispersa quanto basta, quindi il contrasto non si muove.
        val rampa = IntArray(256) { Color.rgb(it, it, it) }
        assertEquals(
            "una fotografia già distesa non si spiana",
            0f, Auto.tuned(Look.NONE, rampa).light.contrast, 1e-4f
        )
    }

    /**
     * **Caso 43: 'Auto' scrive nei cursori, e quello che scrive si disfa.**
     *
     * ⚠️⚠️ **QUELLO CHE PUÒ ROMPERSI È IL COLLEGAMENTO**: il conto vive in [Auto] e il tasto sta
     * nella scheda, quindi il codice compilerebbe lo stesso con un tasto che non consegna
     * l'immagine o che non scrive nel modello, e in scena non succederebbe niente.
     * ⚠️ **L'immagine di prova è una SFUMATURA e non il quadrato bianco**: su un colore solo la
     * guardia dell'immagine piatta risponde 'non c'è niente da stirare', quindi la misura direbbe
     * che il tasto non fa niente proprio dove è giusto che non lo faccia.
     * ⚠️ **'Annulla' acceso è la metà che lui ha chiesto** (*dev'essere annullabile*): un comando che
     * scrive nel modello entra nella storia dei passi come un gesto qualunque.
     */
    @Test
    fun `auto scrive nei punti e si puo disfare`() {
        banco.setContent { Scena(sfumato()) }
        pronta()
        modulo(R.string.look_light)

        val neri = valore(5)
        val bianchi = valore(4)
        banco.onNodeWithContentDescription(testo(R.string.editor_undo)).assertIsNotEnabled()

        banco.onNodeWithContentDescription(testo(R.string.look_auto)).performClick()
        banco.waitForIdle()

        assertTrue(
            "'Auto' doveva scrivere nei due punti",
            valore(5) != neri || valore(4) != bianchi
        )
        banco.onNodeWithContentDescription(testo(R.string.editor_undo)).assertIsEnabled()
    }

    /**
     * **Il respiro: un corpo corto si stacca fino al tetto e poi si centra.**
     *
     * ⚠️⚠️ **È LA SECONDA METÀ DELLA SUA RICHIESTA DELLA `2.35`** (*fa' respirare di più quelli
     * ristretti inutilmente*): dalla `2.33` la scheda è alta quanto il modulo più alto, e fino alla
     * `2.34` lo spazio che avanzava in un modulo corto restava **tutto in fondo**.
     * ⚠️⚠️ **SI MISURA CHIAMANDO IL CONTO, PERCHÉ È KOTLIN PURO**: `Breathe` è un `Arrangement`, e
     * una prova che montasse una scheda misurerebbe la somma di molte cose invece di questa.
     * ⚠️ **Il tetto è la cosa che conta**: senza, un corpo di tre file dentro una scheda alta il
     * doppio darebbe mezzo centimetro fra una fila e l'altra, cioè tre isole. Controprovata
     * togliendo il `min`: l'aria diventa 50 e il primo blocco parte da 0.
     */
    @Test
    fun `un corpo corto respira fino al tetto e poi si centra`() {
        val misure = intArrayOf(40, 40, 40)
        val dove = IntArray(3)
        with(Density(1f)) { with(Breathe) { arrange(240, misure, dove) } }

        val aria = dove[1] - (dove[0] + 40)
        assertEquals("l'aria fra due file non supera il tetto", 12, aria)
        assertEquals("la seconda aria è la stessa", aria, dove[2] - (dove[1] + 40))
        val sopra = dove[0]
        val sotto = 240 - (dove[2] + 40)
        assertTrue(
            "e quello che avanza si divide sopra e sotto: $sopra contro $sotto",
            abs(sopra - sotto) <= 1
        )
    }

    /**
     * **Il corpo più alto non guadagna aria: là non avanza niente.**
     *
     * ⚠️ **È il rovescio della prova qui sopra, e vale come controprova permanente**: il tetto non
     * deve diventare una spaziatura, o il modulo che detta l'altezza si allungherebbe di suo e la
     * scheda crescerebbe a ogni versione.
     */
    @Test
    fun `il corpo che riempie la scheda non prende aria`() {
        val misure = intArrayOf(60, 60, 60)
        val dove = IntArray(3)
        with(Density(1f)) { with(Breathe) { arrange(180, misure, dove) } }
        assertEquals(0, dove[0])
        assertEquals(60, dove[1])
        assertEquals(120, dove[2])
    }

    /**
     * **Il filtro del bianco e nero: a riposo è Rec. 709, e i pesi sommano sempre uno.**
     *
     * ⚠️⚠️ **LA SOMMA È LA PROPRIETÀ CHE TIENE FERMA L'ESPOSIZIONE**: con pesi che non sommano a
     * uno un grigio cambierebbe valore, cioè il cursore 'Filtro' sarebbe anche un'esposizione, e a
     * fondo corsa l'immagine si scurirebbe senza che nessuno abbia toccato la Luce.
     * ⚠️ **E a riposo dev'essere l'identità ESATTA**: chi aggiorna non deve ritrovarsi le sue
     * immagini in bianco e nero diverse da ieri.
     * ⚠️ **I due versi si misurano su un cielo**: verso il caldo deve venire più scuro (il filtro
     * rosso), verso il freddo più chiaro. È il verso, e un segno sbagliato non lo vedrebbe nessun
     * compilatore.
     */
    @Test
    fun `il filtro non cambia il grigio a riposo e i suoi pesi sommano uno`() {
        assertArrayEquals(Chroma.REC709, Chroma().grey, 1e-6f)
        for (k in listOf(-1f, -0.5f, -0.1f, 0f, 0.1f, 0.5f, 1f)) {
            val pesi = Chroma.greyMix(k)
            assertEquals("i pesi di $k devono sommare uno", 1f, pesi.sum(), 1e-5f)
            assertTrue("e nessuno può essere negativo", pesi.all { it >= 0f })
        }

        // Un cielo azzurro, cioè il caso da cui la funzione nasce.
        fun grigio(k: Float): Float {
            val p = Chroma.greyMix(k)
            return 0.35f * p[0] + 0.55f * p[1] + 0.95f * p[2]
        }
        assertTrue("col filtro caldo il cielo deve venire più scuro", grigio(1f) < grigio(0f))
        assertTrue("e col freddo più chiaro", grigio(-1f) > grigio(0f))
    }

    /**
     * **Il 'Filtro BN' c'è sempre, è spento a colori, e vive sotto l'interruttore.**
     *
     * ⚠️⚠️ **È IL PUNTO A DEL SUO CAMPO LIBERO** (giro della `2.36`: *'Filtro' ... va posizionato
     * (non attivo) DOPO l'interruttore 'Bianco e nero'. Si attiva solo con l'interruttore ON*).
     * La `2.36` lo toglieva dalla scena a colori, e adesso c'è sempre.
     * ⚠️⚠️ **QUELLO CHE PUÒ ROMPERSI IN SILENZIO SONO DUE COSE, E SI MISURANO INSIEME**: il verso
     * della condizione (scritta al contrario, il cursore sarebbe acceso proprio dove non governa
     * niente) e il **posto**, che dipende dall'identità della riga e non da un indice; spostandolo
     * nell'elenco, il codice compilerebbe uguale e l'interruttore finirebbe in coda.
     * ⚠️ **Il posto si misura in pixel e non contando le righe**: l'interruttore non è un cursore,
     * quindi fra le righe non ha un numero, e quello che lui vede è dove cade sullo schermo.
     */
    @Test
    fun `il filtro è spento a colori e vive sotto l'interruttore`() {
        banco.setContent { Scena() }
        pronta()
        modulo(R.string.look_color)

        assertEquals("il Colore porta cinque cursori", 5, quantiCursori())
        cursore(4).assertIsNotEnabled()
        cursore(3).assertIsEnabled()

        val interruttore = banco.onNodeWithText(testo(R.string.look_bw))
            .fetchSemanticsNode().positionInRoot.y
        assertTrue(
            "il 'Filtro BN' deve venire dopo l'interruttore",
            cursore(4).fetchSemanticsNode().positionInRoot.y > interruttore
        )
        assertTrue(
            "e la vividezza sopra",
            cursore(3).fetchSemanticsNode().positionInRoot.y < interruttore
        )

        val prima = altezzaPalco()
        assertTrue(prima > 0)
        banco.onNodeWithText(testo(R.string.look_bw)).performClick()
        banco.waitForIdle()

        assertEquals("col bianco e nero restano cinque", 5, quantiCursori())
        cursore(4).assertIsEnabled()
        cursore(2).assertIsNotEnabled()
        assertEquals("e il palco non deve accorciarsi", prima, altezzaPalco())
    }

    /**
     * **L'immagine non cambia misura entrando nel Ritaglio.**
     *
     * ⚠️⚠️ **È IL PUNTO C DEL SUO CAMPO LIBERO** (giro della `2.36`: *Consideralo un anti-jitter tra
     * moduli: al cambio da un altro modulo al ritaglio, l'immagine NON deve rimpicciolirsi*). Fino
     * alla `2.36` l'aria delle squadrette valeva zero negli altri sei moduli, quindi entrare nel
     * Ritaglio toglieva `CROP_AIR` per lato all'immagine, che si rimpiccioliva sotto gli occhi.
     * ⚠️ **Si misurano i BORDI e non l'altezza del palco**: il palco non si è mai mosso (ci pensa
     * `SteadyBody`), a muoversi era il rettangolo in cui l'immagine è disegnata **dentro** di lui,
     * e quello si vede solo guardando i pixel.
     * ⚠️⚠️ **UN PIXEL PER LATO SI CONCEDE, E NON È PRUDENZA GENERICA**: nel Ritaglio il velo del
     * ritaglio copre quello che sta **fuori** dal rettangolo, e il suo bordo interno cade
     * esattamente sul bordo dell'immagine; il pixel di sfumatura che l'antialiasing lascia lì non
     * è fondo, quindi il conto lo legge come immagine. La misura che conta è di un altro ordine di
     * grandezza: **controprovata** rimettendo la condizione di prima, il bordo sinistro passa da
     * 116 a 120, cioè rientra di quattro pixel (i cinque di `CROP_AIR` meno quello di sfumatura).
     */
    /*
     * ⚠️⚠️ **LA SCENA È GRANDE, DALLA `2.40`, E IL BANCO L'HA IMPOSTA**: in quella di serie il palco
     * viene largo 320 e alto una quarantina di punti, quindi l'immagine quadrata occupa una striscia
     * in mezzo e **ogni** sua riga è attraversata dal braccio di una squadretta d'angolo. Con una
     * scena grande esiste una riga libera, che è quello che questa misura cerca.
     */
    @Config(qualifiers = "w600dp-h900dp")
    @Test
    fun `l'immagine non cambia misura entrando nel Ritaglio`() {
        banco.setContent { Scena() }
        pronta()
        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))

        modulo(R.string.look_light)
        val (daFuori, aFuori) = bordi(palco.captureToImage().toPixelMap())

        modulo(R.string.look_crop)
        val (daDentro, aDentro) = bordi(palco.captureToImage().toPixelMap())
        assertTrue(
            "il bordo sinistro non deve rientrare: $daFuori diventa $daDentro",
            abs(daDentro - daFuori) <= 1
        )
        assertTrue(
            "e nemmeno il destro: $aFuori diventa $aDentro",
            abs(aDentro - aFuori) <= 1
        )
    }

    /**
     * **Caso 34: fra il nome di un cursore e la sua barra resta dell'aria.**
     *
     * ⚠️⚠️ **È LA SUA SEGNALAZIONE CON SCHERMATA** (2026-09-13: *lascia più spazio per i testi ...
     * più un po' di aria, perché al momento è tutto troppo attaccato*). Fino alla `2.37` le tre
     * colonne di una riga si toccavano: la colonna dei nomi è larga quanto il più largo di **tutti**
     * i moduli, quindi proprio quel nome arrivava a filo del tondo, che a riposo ha il centro sul
     * bordo della barra.
     * ⚠️ **La soglia è più bassa della costante di proposito**: qui si misura il **fatto** che
     * l'aria ci sia, non il numero che la produce, quindi un ritocco a [KNOB_GAP] non fa diventare
     * rossa questa prova mentre il comportamento è ancora giusto.
     * ⚠️ **Si misura in dp e non in pixel**: il confronto è con una misura dichiarata, e in pixel
     * dipenderebbe dalla densità della scena di prova.
     * ⚠️⚠️ **CONTROPROVATA** togliendo il distacco: là l'aria è **zero**, cioè il nome e la barra
     * si toccano, che è esattamente quello che lui ha visto sul telefono.
     */
    @Test
    fun `fra il nome di un cursore e la sua barra resta dell'aria`() {
        banco.setContent { Scena() }
        pronta()
        modulo(R.string.look_detail)

        val nome = banco
            .onNodeWithContentDescription(
                testo(R.string.look_peek_one, testo(R.string.look_sharpen))
            )
            .getUnclippedBoundsInRoot()
        val barra = cursore(0).getUnclippedBoundsInRoot()
        val aria = barra.left - nome.right
        assertTrue("fra il nome e la barra deve restare dell'aria, misurata $aria", aria >= 8.dp)
    }

    /**
     * **Caso 44: la storia del ritaglio, cioè quello che i suoi tre comandi fanno.**
     *
     * ⚠️⚠️ **È SUA RICHIESTA** (2026-09-14: *apparissero dei tasti 'Annulla'/'Ripristina'/'Azzera'
     * SOLO PER IL RITAGLIO*), e quello che può rompersi in silenzio è il **verso**: un 'Indietro'
     * che togliesse l'applicazione senza riportare indietro il rettangolo lascerebbe il palco a
     * inquadrare una porzione che il modello non ha più.
     * ⚠️ **Il conto è puro**, quindi si misura chiamandolo: [Framing] è Kotlin e non ha bisogno di
     * una scena.
     * ⚠️⚠️ **CONTROPROVATA** facendo ripartire 'Avanti' da una storia già troncata: senza il
     * `take(at)` l'applicazione nuova lascerebbe in coda quella disfatta, cioè un 'Avanti' che
     * porta dove nessuno è più passato.
     */
    @Test
    fun `la storia del ritaglio va avanti e indietro`() {
        val uno = ImageEdit.Crop(0.1f, 0.1f, 0.9f, 0.9f)
        val due = ImageEdit.Crop(0.2f, 0.2f, 0.6f, 0.6f)

        assertNull("a riposo il palco vede l'immagine intera", Framing.NONE.shown)
        assertFalse("e non c'è niente da disfare", Framing.NONE.undoable)
        assertFalse("né da rifare", Framing.NONE.redoable)

        val primo = Framing.NONE.applied(uno)
        assertEquals("applicato, il palco inquadra il taglio", uno, primo.shown)
        assertTrue(primo.undoable)
        assertFalse(primo.redoable)

        val secondo = primo.applied(due)
        assertEquals(due, secondo.shown)

        val indietro = secondo.back()
        assertEquals("'Indietro' torna al taglio di prima", uno, indietro.shown)
        assertTrue("e 'Avanti' ha qualcosa da fare", indietro.redoable)
        assertEquals("'Avanti' rimette quello disfatto", due, indietro.on().shown)

        // ⚠️ Un'applicazione nuova tronca la coda: da qui la strada è un'altra.
        val altro = ImageEdit.Crop(0f, 0f, 0.5f, 0.5f)
        val deviata = indietro.applied(altro)
        assertEquals(altro, deviata.shown)
        assertFalse("la strada abbandonata non si rifà", deviata.redoable)
        assertEquals("e la storia non cresce a vuoto", 2, deviata.steps.size)

        // ⚠️ Un taglio che copre tutto non è una porzione da inquadrare, e vale `null`.
        assertNull(Framing.NONE.applied(ImageEdit.Crop.WHOLE).shown)
    }

    /**
     * **Caso 45: i quattro comandi del ritaglio vivono nel suo modulo, e 'Applica' non è più in
     * fondo.**
     *
     * ⚠️⚠️ **È LA METÀ DELLA SUA RICHIESTA CHE SI VEDE** (2026-09-14: *magari posizionati altrove*),
     * e la controprova è in un altro modulo: senza di lei la misura direbbe soltanto che tre parole
     * esistono da qualche parte nella scheda.
     * ⚠️ **Si contano le descrizioni parlate**, che è quello che un gettone annuncia: le etichette
     * scritte dipendono dall'interruttore delle etichette, la descrizione no.
     */
    @Test
    fun `il ritaglio porta i suoi quattro comandi e nessun altro modulo li ha`() {
        banco.setContent { Scena() }
        pronta()

        modulo(R.string.look_light)
        for (id in CROP_CMD) {
            assertEquals("nella Luce ${testo(id)} non ci deve essere", 0, quantiDetti(id))
        }

        modulo(R.string.look_crop)
        for (id in CROP_CMD) {
            assertEquals("nel Ritaglio manca ${testo(id)}", 1, quantiDetti(id))
        }
    }

    /**
     * **Caso 46: 'Applica' taglia davvero, e 'Azzera' rimette l'immagine intera.**
     *
     * ⚠️⚠️ **È IL CUORE DELLA SUA RICHIESTA** (*Potrebbe avere senso se fosse applicato
     * effettivamente anche nel modulo Ritaglio (resta solo la parte ritagliata)*): fino alla `2.39`
     * dentro il Ritaglio l'immagine tornava intera, quindi quel tasto non faceva niente che si
     * vedesse proprio nel modulo in cui lo si tocca.
     * ⚠️ **Si guardano i PIXEL e non lo stato**, perché quello che deve cambiare è il disegno:
     * applicando, il palco inquadra la porzione, e l'immagine si vede più larga di prima.
     * ⚠️⚠️ **IL BANCO HA IMPOSTO LA SCENA GRANDE**, come nel caso 36: col palco di serie non ci sta
     * un rettangolo col suo lato minimo, e il gesto che tira la squadretta non avrebbe spazio.
     */
    @Test
    @Config(qualifiers = "w600dp-h900dp")
    fun `applica taglia davvero e azzera rimette l'immagine intera`() {
        banco.setContent { Scena() }
        pronta()
        modulo(R.string.look_crop)

        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        val intera = bordi(palco.captureToImage().toPixelMap())

        // Si tira la squadretta di sinistra verso il centro: il rettangolo si stringe.
        palco.performTouchInput { down(Offset(intera.first + 2f, 2f)) }
        banco.waitForIdle()
        palco.performTouchInput { moveTo(Offset(width / 2f, height / 3f)) }
        banco.waitForIdle()
        palco.performTouchInput { up() }
        banco.waitForIdle()

        val applica = banco.onNodeWithContentDescription(testo(R.string.editor_apply))
        applica.assertIsEnabled()
        applica.performClick()
        banco.waitForIdle()

        /*
         * ⚠️⚠️ **SI MISURA IL RAPPORTO E NON I SOLI BORDI, ED È LA CONTROPROVA A IMPORLO**: con la
         * condizione della `2.39` rimessa a mano (il taglio applicato che dentro il Ritaglio non si
         * vede) i bordi cambiano **lo stesso**, perché il velo che copre il fuori se ne va quando
         * il rettangolo torna intero, e il primo pixel diverso dal fondo si sposta con lui. Quello
         * che quella condizione non può dare è la **forma**: l'immagine di prova è quadrata, la
         * porzione tirata è più alta che larga, e il palco che la inquadra lo dice.
         */
        val scattoDopo = palco.captureToImage().toPixelMap()
        val tagliata = bordi(scattoDopo)
        val quadra = forma(scattoDopo)
        assertTrue(
            "applicando, il palco deve inquadrare la porzione: ${intera.first}..${intera.second}" +
                " resta ${tagliata.first}..${tagliata.second}",
            tagliata != intera
        )
        assertTrue(
            "e la porzione è più alta che larga: il rapporto resta $quadra",
            quadra < 0.9f
        )

        banco.onNodeWithContentDescription(testo(R.string.editor_crop_clear)).performClick()
        banco.waitForIdle()
        assertEquals(
            "'Azzera taglio' deve rimettere l'immagine intera",
            intera,
            bordi(palco.captureToImage().toPixelMap())
        )
    }

    /**
     * **Caso 47: le maniglie di lato muovono un bordo solo, e con una forma scelta non ci sono.**
     *
     * ⚠️⚠️ **È SUA RICHIESTA** (2026-09-14: *delle maniglie a metà dei lati ... servirebbero a
     * trascinare solo il lato, senza modificare l'altra dimensione*), e la seconda metà è la
     * lettura alla lettera di quella frase: con un rapporto forzato quella promessa non si può
     * mantenere, quindi là i lati non si prendono.
     * ⚠️ **Il conto è puro**: [grabbed] e [dragged] sono funzioni, e si misurano chiamandole.
     */
    @Test
    fun `le maniglie di lato muovono un bordo solo`() {
        val r = Rect(20f, 20f, 120f, 220f)
        val frame = Rect(0f, 0f, 200f, 300f)

        assertEquals(
            "il dito sul mezzo del lato sinistro prende quel lato",
            Grab.LEFT,
            grabbed(Offset(22f, 120f), r, 40f)
        )
        assertEquals(Grab.TOP, grabbed(Offset(70f, 22f), r, 40f))
        assertEquals(
            "e l'angolo vince quando il dito è più vicino a lui",
            Grab.TOP_LEFT,
            grabbed(Offset(22f, 24f), r, 40f)
        )
        assertEquals(
            "con una forma scelta i lati non si prendono",
            Grab.INSIDE,
            grabbed(Offset(22f, 120f), r, 40f, sides = false)
        )

        val tirato = dragged(r, Grab.LEFT, Offset(30f, 40f), frame, null, 10f)
        assertEquals("il lato sinistro segue il dito", 50f, tirato.left, 0.01f)
        assertEquals("e l'altra dimensione non si muove", r.top, tirato.top, 0.01f)
        assertEquals(r.bottom, tirato.bottom, 0.01f)
        assertEquals(r.right, tirato.right, 0.01f)

        val sotto = dragged(r, Grab.BOTTOM, Offset(30f, 40f), frame, null, 10f)
        assertEquals(260f, sotto.bottom, 0.01f)
        assertEquals("e i due fianchi restano dove sono", r.left, sotto.left, 0.01f)
        assertEquals(r.right, sotto.right, 0.01f)
    }

    /**
     * **Caso 48: nelle Curve un punto nasce e si muove nello stesso gesto.**
     *
     * ⚠️⚠️ **È SUA RICHIESTA** (2026-09-14: *toccare un punto libero e trascinarlo dovrebbe sia
     * aggiungere un nuovo punto che spostarlo creando la curva*), e la causa era la **chiave** del
     * rilevatore: portava il numero di punti, quindi l'istante in cui il gesto ne faceva nascere uno
     * annullava il gesto stesso. Il codice era giusto e la metà che lui chiedeva non arrivava mai.
     * ⚠️⚠️ **I TRE MOMENTI DEL DITO VANNO IN TRE CHIAMATE**, come nel caso 36: scritti in un blocco
     * solo il movimento e il distacco arrivano insieme, e a valle resta un evento con delta zero.
     * ⚠️ **Si misura che il punto sia NATO e SPOSTATO**: il primo lo dice il conto dei punti, il
     * secondo che la curva non sia più l'identità.
     * ⚠️⚠️ **MA IL DIFETTO DELLA CHIAVE IL BANCO NON LO VEDE, E SI SCRIVE COSÌ INVECE DI FINGERE**:
     * rimettendola a mano questa prova resta **verde**, perché i tre momenti del dito arrivano in
     * tre chiamate separate e il nodo ricostruito fra l'una e l'altra riprende il gesto, cosa che
     * su un telefono non succede. Quello che presidia è che il gesto **faccia** le due cose, ed è
     * ⚠️⚠️ **CONTROPROVATO** togliendo la nascita del punto: là diventa rossa.
     */
    @Test
    fun `nelle curve un punto nasce e si muove nello stesso gesto`() {
        // ⚠️ Dalla `2.55` il gettone delle Curve si raggiunge solo portandolo in testa: [davanti].
        banco.setContent { Scena(mods = davanti(PadKey.MOD_TONE)) }
        pronta()
        modulo(R.string.look_tone)

        val grafico = banco.onNodeWithContentDescription(testo(R.string.look_tone_board))
        val prima = grafico.captureToImage().toPixelMap()

        grafico.performTouchInput { down(Offset(width * 0.5f, height * 0.5f)) }
        banco.waitForIdle()
        grafico.performTouchInput { moveTo(Offset(width * 0.5f, height * 0.2f)) }
        banco.waitForIdle()
        grafico.performTouchInput { up() }
        banco.waitForIdle()

        assertTrue(
            "il punto doveva nascere e seguire il dito: il grafico non è cambiato",
            diversi(prima, grafico.captureToImage().toPixelMap()) > 0
        )
        assertTrue(
            "e il modulo deve dichiararsi toccato",
            quantiDetti(R.string.look_tone) > 0
        )
    }

    /**
     * **Caso 49: un estremo trascinato dentro lascia un punto al bordo, allo stesso livello.**
     *
     * ⚠️⚠️ **È SUA RICHIESTA** (2026-09-14: *se trascino il punto iniziale a destra o il finale a
     * sinistra, dovrebbero muoversi lasciando la loro vecchia posizione ad un nuovo punto allo
     * stesso livello*), ed è il comportamento degli strumenti che usa: fra il bordo e il punto
     * trascinato il tratto resta **piatto**.
     * ⚠️ **Il conto è puro**: il gemello lo fa [Curve.pin], il livello lo tiene [Curve.move] col suo
     * `edge`, e quello rimasto a filo lo toglie [Curve.tidy].
     * ⚠️⚠️ **CONTROPROVATA** togliendo l'`edge`: il bordo resta al valore di partenza, quindi fra i
     * due punti c'è una rampa e la tabella non è più piatta all'inizio. ⚠️ **Il conto e non il
     * chiamante**: che il gesto chiami [Curve.pin] lo misura il caso 48, e rimettendo quel difetto
     * qui non cambia niente, perché questa prova non monta nessuna scena.
     */
    @Test
    fun `un estremo trascinato lascia un punto al bordo`() {
        val nata = Curve.NONE
        assertEquals(2, nata.knots.size)

        val col = nata.pin(start = true)
        assertEquals("il gemello nasce in testa", 3, col.knots.size)
        assertEquals("e nasce sovrapposto", nata.knots[0], col.knots[0])

        val dentro = col.move(1, 0.3f, 0.25f, edge = 0)
        assertEquals("il punto mosso va dove dice il dito", 0.3f, dentro.knots[1].at, 1e-4f)
        assertEquals(0.25f, dentro.knots[1].to, 1e-4f)
        assertEquals("il bordo resta al bordo", 0f, dentro.knots[0].at, 1e-4f)
        assertEquals("e sale allo stesso livello", 0.25f, dentro.knots[0].to, 1e-4f)

        val tavola = dentro.table()
        val fino = (0.3f * (Curve.SIZE - 1)).toInt()
        for (k in 0..fino) {
            assertEquals("fra il bordo e il punto il tratto deve essere piatto", 0.25f, tavola[k], 0.01f)
        }

        // ⚠️ Riportandolo a filo, il gemello se ne va: due punti l'uno sull'altro non si separano.
        assertEquals(2, dentro.move(1, 0f, 0.25f, edge = 0).tidy().knots.size)
        assertEquals(
            "ma un punto tenuto dentro resta",
            3,
            dentro.tidy().knots.size
        )

        // In coda il gemello nasce ultimo, e l'indice del punto mosso non cambia.
        val coda = nata.pin(start = false)
        assertEquals(3, coda.knots.size)
        val tirato = coda.move(1, 0.7f, 0.8f, edge = 2)
        assertEquals(0.7f, tirato.knots[1].at, 1e-4f)
        assertEquals(1f, tirato.knots[2].at, 1e-4f)
        assertEquals(0.8f, tirato.knots[2].to, 1e-4f)
    }

    /**
     * **Caso 50: un punto nato in mezzo non trascina con sé l'estremo.**
     *
     * ⚠️⚠️ **È IL DIFETTO CRITICO DELLA `2.50`, ARRIVATO DA LUI** (2026-09-14: *nelle curve adesso,
     * se tocco e trascino la curva direttamente, si crea una retta orizzontale che arriva fino al
     * margine sinistro o destro, distruggendo l'immagine*). La causa è una lettura fatta troppo
     * presto: `viva` non si aggiorna dentro la stessa coroutine del gesto, quindi il conto degli
     * estremi guardava la curva **prima** che il punto nascesse, e l'indice del punto nuovo cadeva
     * esattamente sull'ultimo indice di quella. Da lì il gesto si credeva su un estremo, faceva
     * nascere il gemello e portava il bordo al livello del dito: fra il punto trascinato e il
     * margine la tabella diventava piatta.
     * ⚠️ **Si misura a PIXEL nella striscia vicino al bordo destro**, perché lo stato della curva
     * vive dentro la schermata e di qui non si legge: col difetto la curva passa **in basso** fin
     * là, senza difetto a quell'altezza non c'è niente.
     * ⚠️⚠️ **CONTROPROVATA** rimettendo la lettura tardiva: la striscia bassa cambia di centinaia
     * di pixel.
     */
    @Test
    fun `un punto nato in mezzo non trascina il bordo`() {
        // ⚠️ Come sopra: dalla `2.55` il gettone delle Curve si raggiunge solo portandolo in testa.
        banco.setContent { Scena(mods = davanti(PadKey.MOD_TONE)) }
        pronta()
        modulo(R.string.look_tone)

        val grafico = banco.onNodeWithContentDescription(testo(R.string.look_tone_board))
        val prima = grafico.captureToImage().toPixelMap()

        grafico.performTouchInput { down(Offset(width * 0.5f, height * 0.5f)) }
        banco.waitForIdle()
        // ⚠️ Due colpi e non uno: il primo oltre la soglia se lo prende `settled`, e senza il
        // secondo a `drag` non arriva niente. È la trappola misurata scrivendo la prova della
        // `2.18`, e con un colpo solo questa prova sarebbe verde a vuoto.
        grafico.performTouchInput { moveTo(Offset(width * 0.5f, height * 0.6f)) }
        banco.waitForIdle()
        grafico.performTouchInput { moveTo(Offset(width * 0.5f, height * 0.85f)) }
        banco.waitForIdle()
        grafico.performTouchInput { up() }
        banco.waitForIdle()

        val dopo = grafico.captureToImage().toPixelMap()
        assertTrue(
            "il punto doveva nascere e seguire il dito: il grafico non è cambiato",
            diversi(prima, dopo) > 0
        )
        assertEquals(
            "vicino al margine destro, in basso, non deve passare nessuna curva",
            0,
            diversiIn(prima, dopo, da = 0.86f, fino = 0.96f, su = 0.60f, giu = 0.95f)
        )
    }

    /**
     * **Caso 44: lo strumento 'Angoli' si disfa a vicenda, e a riposo non esiste.**
     *
     * ⚠️⚠️ **L'ANDATA E RITORNO È LA COSA CHE PUÒ ROMPERSI IN SILENZIO**: il palco disegna con la
     * mappa diretta e il colore mirato legge con l'inversa, quindi un segno sbagliato nella matrice
     * aggiunta non dà nessun errore e prende un pixel da un'altra parte della fotografia. È lo
     * stesso difetto che questa prova ha già trovato una volta, sul fondo corsa della distorsione.
     * ⚠️ **A riposo la mappa non si fa affatto**: con gli angoli fermi la formula darebbe
     * l'identità in aritmetica esatta e non in `Float`, quindi un'immagine non toccata perderebbe
     * un millesimo di pixel per niente.
     */
    @Test
    fun `gli angoli si disfano con la mappatura inversa`() {
        assertEquals(
            "a riposo l'omografia degli angoli non deve esistere",
            null,
            Warp.quad(Corners.NONE, 1f, 0.75f)
        )

        val tirati = Corners(
            x0 = 0.2f, y0 = -0.15f,
            x1 = -0.1f, y1 = 0.3f,
            x2 = 0.25f, y2 = 0.1f,
            x3 = -0.2f, y3 = -0.05f
        )
        assertTrue(
            "questi quattro angoli devono fare un quadrilatero convesso",
            Warp.convex(tirati, 1f, 0.75f)
        )
        val piano = Warp.plan(
            Geometry(straighten = 0.4f, distortion = -0.3f, corners = tirati),
            200f, 150f, 400f, 300f
        )
        for (x in listOf(20f, 200f, 380f)) {
            for (y in listOf(15f, 150f, 285f)) {
                val avanti = piano.map(x, y)
                val indietro = piano.back(avanti[0], avanti[1])
                assertEquals("l'andata e ritorno non torna in x", x, indietro[0], 0.5f)
                assertEquals("l'andata e ritorno non torna in y", y, indietro[1], 0.5f)
            }
        }
    }

    /**
     * **Caso 45: il quadrilatero non si rovescia, e la vista di lavoro non segue gli angoli.**
     *
     * ⚠️⚠️ **SONO LE DUE PROPRIETÀ CHE RENDONO USABILE LO STRUMENTO, e nessuna delle due dà errore
     * se cade.** Senza la guardia di convessità, un angolo tirato oltre la diagonale incrocia due
     * lati e l'immagine si ripiega su se stessa; e se la scala di lavoro guardasse il contorno
     * vero, tirando un angolo in fuori l'immagine si stringerebbe di altrettanto e la maniglia
     * resterebbe **incollata al bordo** senza avanzare di un pixel.
     */
    @Test
    fun `il quadrilatero resta convesso e la vista di lavoro sta ferma`() {
        // L'angolo di sopra a sinistra portato oltre quello di sotto a destra: i lati si incrociano.
        val rovescio = Corners(x0 = 2.5f, y0 = 2.5f)
        assertTrue(
            "un angolo portato oltre la diagonale doveva essere rifiutato",
            !Warp.convex(rovescio, 1f, 0.75f)
        )

        val geo = Geometry(vertical = 0.4f)
        val fermo = Warp.plan(geo, 200f, 150f, 400f, 300f, hold = 0.7f)
        val mosso = Warp.plan(
            geo.copy(corners = Corners(x0 = -0.3f, y0 = -0.3f)),
            200f, 150f, 400f, 300f, hold = 0.7f
        )
        assertEquals(
            "la scala di lavoro si è mossa con l'angolo",
            fermo.cover,
            mosso.cover,
            1e-4f
        )
        /*
         * ⚠️ **E la copertura invece li guarda**: quello che si salva non deve lasciare vuoti, e un
         * angolo tirato dentro chiede di ingrandire. Le due scale hanno due mestieri diversi.
         */
        val salvata = Warp.plan(
            geo.copy(corners = Corners(x0 = 0.3f, y0 = 0.3f)), 200f, 150f, 400f, 300f
        )
        assertTrue(
            "la copertura doveva crescere con un angolo tirato dentro",
            salvata.cover > Warp.plan(geo, 200f, 150f, 400f, 300f).cover
        )
    }

    /**
     * **Caso 46: armato lo strumento, tirare una maniglia cambia il disegno.**
     *
     * ⚠️⚠️ **È IL COLLEGAMENTO CHE PUÒ ROMPERSI, COME NEL RITAGLIO**: gli angoli, il gesto e il
     * disegno vivono in tre posti, e se il valore non arrivasse al palco il codice compilerebbe lo
     * stesso e il dito non farebbe niente. È il caso proattivo di una modifica che tocca la
     * gerarchia dei tocchi.
     * ⚠️ **La scena è grande** come le altre prove che tirano una presa, e la maniglia si cerca
     * dove l'immagine comincia davvero invece che a un conto scritto qui.
     */
    @Test
    @Config(qualifiers = "w600dp-h900dp")
    fun `una maniglia d angolo tirata cambia il disegno`() {
        banco.setContent { Scena() }
        pronta()
        banco.onNodeWithContentDescription(testo(R.string.look_geometry)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(testo(R.string.look_corners)).performClick()
        banco.waitForIdle()

        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))
        val riposo = palco.captureToImage().toPixelMap()
        val (da, _) = bordi(riposo)
        val cima = (0 until riposo.height).first { riposo[da + 4, it] != riposo[0, 0] }

        palco.performTouchInput { down(Offset(da + 2f, cima + 2f)) }
        banco.waitForIdle()
        palco.performTouchInput { moveTo(Offset(width / 3f, height / 3f)) }
        banco.waitForIdle()
        val tirato = palco.captureToImage().toPixelMap()
        palco.performTouchInput { up() }
        banco.waitForIdle()

        assertTrue(
            "tirando la maniglia d'angolo il palco doveva cambiare disegno",
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

    /**
     * Quanto è alto il palco, cioè l'immagine su cui si lavora.
     *
     * ⚠️ **È la misura della scheda letta dall'altra parte**: le due si dividono lo schermo sotto
     * la testata, quindi una scheda che si allunga è un palco che si accorcia, e il palco è quello
     * che l'utente guarda.
     */
    private fun altezzaPalco(): Int =
        banco.onNodeWithContentDescription(testo(R.string.look_compare))
            .fetchSemanticsNode().size.height

    /** Quanti cursori sono in scena, contati dalla loro azione semantica. */
    private fun quantiCursori(): Int =
        banco.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .fetchSemanticsNodes().size

    /**
     * Dove comincia e dove finisce l'immagine, su una riga a un quarto dell'altezza di uno scatto
     * del palco.
     *
     * ⚠️ **Il fondo è il pixel del bordo sinistro**, che è la stessa lettura con cui si trova la
     * squadretta: l'immagine è centrata nel palco, quindi il primo pixel diverso dal fondo è il suo
     * bordo.
     * ⚠️⚠️ **LA RIGA È A UN QUARTO E NON A METÀ, DALLA `2.40`, E NON È UNA PRUDENZA**: fino alla
     * `2.39` a metà altezza non passava nessuna presa, perché vivevano tutte agli angoli; con le
     * maniglie di lato quella premessa è caduta, e quella di sinistra sporge **fuori** dal bordo
     * esattamente di `CROP_AIR`, cioè misurava se stessa invece del bordo dell'immagine. A un
     * quarto non passa né una maniglia di mezzo né il braccio di una squadretta d'angolo.
     */
    private fun bordi(scatto: PixelMap): Pair<Int, Int> {
        val riga = scatto.height / 4
        val fondo = scatto[0, riga]
        val da = (0 until scatto.width).first { scatto[it, riga] != fondo }
        val a = (scatto.width - 1 downTo 0).first { scatto[it, riga] != fondo }
        return da to a
    }

    /**
     * Quante volte l'immagine disegnata è più larga che alta, su uno scatto del palco.
     *
     * ⚠️ **La colonna è a un quarto fra i due bordi**: non è un angolo, dove passa il braccio di una
     * squadretta, e non è la metà, dove dalla `2.40` passa la maniglia di lato.
     */
    private fun forma(scatto: PixelMap): Float {
        val (da, a) = bordi(scatto)
        val colonna = da + (a - da) / 4
        val fondo = scatto[0, 0]
        val cima = (0 until scatto.height).firstOrNull { scatto[colonna, it] != fondo } ?: 0
        val piede = (scatto.height - 1 downTo 0).firstOrNull { scatto[colonna, it] != fondo }
            ?: (scatto.height - 1)
        return (a - da).toFloat() / (piede - cima).coerceAtLeast(1)
    }

    /**
     * Quanti pixel cambiano fra due scatti dentro un riquadro, dato in frazioni del nodo.
     *
     * ⚠️ **In frazioni e non in pixel**: la scena di prova non ha la misura di un telefono, quindi
     * un riquadro scritto in pixel misurerebbe un'altra parte del grafico.
     */
    private fun diversiIn(
        a: PixelMap,
        b: PixelMap,
        da: Float,
        fino: Float,
        su: Float,
        giu: Float
    ): Int {
        val larghi = minOf(a.width, b.width)
        val alti = minOf(a.height, b.height)
        var conto = 0
        for (y in (alti * su).toInt() until (alti * giu).toInt()) {
            for (x in (larghi * da).toInt() until (larghi * fino).toInt()) {
                if (a[x, y] != b[x, y]) conto += 1
            }
        }
        return conto
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
    /**
     * Dove comincia il gettone che si annuncia con [id]: a sinistra, o in alto se [alto].
     *
     * ⚠️ **In pixel della scena e non in dp**: qui serve un confronto fra due posizioni, e la
     * conversione non cambierebbe nessuno dei due versi.
     */
    private fun dove(id: Int, alto: Boolean = false): Float {
        val riquadro = banco.onNodeWithContentDescription(testo(id)).fetchSemanticsNode().boundsInRoot
        return if (alto) riquadro.top else riquadro.left
    }

    private fun quantiDetti(id: Int): Int =
        banco.onAllNodesWithContentDescription(testo(id)).fetchSemanticsNodes().size

    /**
     * Apre il modulo che si annuncia con [nome], toccando il suo gettone.
     *
     * ⚠️⚠️ **DALLA `2.35` LA LUCE SI APRE, E PRIMA ERA APERTA DI SERIE**: l'editor nasce sul
     * Ritaglio (sua istruzione, 2026-09-13), quindi una prova che misura i cursori di un modulo
     * deve dire quale, come farebbe un dito.
     */
    private fun modulo(nome: Int) {
        banco.onNodeWithContentDescription(testo(nome)).performClick()
        banco.waitForIdle()
    }

    /**
     * L'ordine dei moduli con [chiave] in testa, e tutti gli altri dietro come sono.
     *
     * ⚠️⚠️ **UNA PROVA CHE TOCCA UN GETTONE NON PUÒ DIPENDERE DALL'ORDINE DI FABBRICA, E QUESTA
     * FUNZIONE NASCE DA QUATTRO PROVE ROSSE**: col nono modulo la fila scorre, quindi quello che
     * cade fuori dalla larghezza del banco non si può toccare, e il tocco non dà nessun errore
     * (si contano zero cursori credendo di guardare un altro modulo). Fino alla `2.54` quelle
     * prove montavano la fila **rovesciata**, che funzionava perché il loro bersaglio era in
     * coda: con l'ordine nuovo della `2.55` quel rimedio le ha rotte tutte e quattro in un colpo.
     * ⚠️ **Porta in testa invece di scrivere una fila a mano**, così l'elenco resta [MOD_KEYS] e
     * quello che la prova misura è ancora il legame fra i due elenchi: un modulo che si
     * dimenticasse della propria chiave sparirebbe dalla fila e la prova lo direbbe.
     */
    private fun davanti(chiave: PadKey): List<PadKey> = MOD_KEYS.sortedBy { it != chiave }

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

    /**
     * **Caso 51: lo specchio della barra bassa, con l'eccezione della coppia del tempo.**
     *
     * ⚠️⚠️ **È LA SUA RICHIESTA ALLA LETTERA** (2026-09-14: *l'ordine delle icone della barra bassa
     * deve essere speculare quando il FAB è a sinistra, con la sola eccezione di
     * 'Annulla'/'Ripristina', che devono essere sempre il primo a sinistra del secondo*).
     * ⚠️ **Si misura la funzione pura e non la fila disegnata**, che è il caso 52: qui quello che
     * può rompersi in silenzio è lo **scambio** della coppia, cioè una riga che senza questa prova
     * si proverebbe solo contando i pixel di cinque icone.
     * ⚠️ **La fila corta è il caso vero dei cinque moduli senza 'Auto' e senza 'Mirato'**: lo
     * scambio deve valere anche quando la coppia apre la fila rovesciata.
     */
    @Test
    fun `la barra bassa si specchia tranne Annulla e Ripristina`() {
        val piena = listOf(Bar.AIM, Bar.AUTO, Bar.UNDO, Bar.REDO, Bar.ORIGINAL)
        assertEquals(
            "col FAB a destra la fila non è quella di sempre",
            piena,
            barOrder(piena, mirror = false)
        )
        assertEquals(
            "col FAB a sinistra la fila non si specchia tenendo ferma la coppia del tempo",
            listOf(Bar.ORIGINAL, Bar.UNDO, Bar.REDO, Bar.AUTO, Bar.AIM),
            barOrder(piena, mirror = true)
        )
        val corta = listOf(Bar.UNDO, Bar.REDO, Bar.ORIGINAL)
        assertEquals(
            "senza i due facoltativi lo scambio della coppia non avviene",
            listOf(Bar.ORIGINAL, Bar.UNDO, Bar.REDO),
            barOrder(corta, mirror = true)
        )
    }

    /**
     * **Caso 52: col FAB a sinistra la fila disegnata parte da 'Originale'.**
     *
     * ⚠️ **Si misura la POSIZIONE e non la sequenza dei nodi**, per la stessa ragione della prova
     * sull'ordine dei moduli: l'albero li elenca come li compone, quindi una prova che leggesse
     * quello sarebbe verde anche con una fila disegnata al contrario.
     * ⚠️ **E la coppia del tempo si guarda qui e non solo nel caso 51**: è la sola cosa che lo
     * specchio non deve toccare, e il conto puro non dice che la fila la disegni davvero così.
     */
    @Test
    fun `col FAB a sinistra la barra bassa comincia da Originale`() {
        banco.setContent { Scena(hand = Hand.LEFT) }
        pronta()

        val originale = dove(R.string.editor_original)
        val annulla = dove(R.string.editor_undo)
        val ripristina = dove(R.string.editor_redo)
        assertTrue("'Originale' non apre la fila specchiata", originale < annulla)
        assertTrue("'Annulla' non resta prima di 'Ripristina'", annulla < ripristina)
    }

    /**
     * **Caso 52b: col FAB a destra la fila resta quella di sempre.**
     *
     * ⚠️ **È la controprova del caso 52 e non un doppione**: `setContent` si chiama una volta sola
     * per prova, quindi le due scene vogliono due prove, e senza questa lo specchio potrebbe
     * valere in tutti e due i versi senza che niente lo dica.
     */
    @Test
    fun `col FAB a destra la barra bassa finisce con Originale`() {
        banco.setContent { Scena(hand = Hand.RIGHT) }
        pronta()

        val annulla = dove(R.string.editor_undo)
        val ripristina = dove(R.string.editor_redo)
        val originale = dove(R.string.editor_original)
        assertTrue("'Annulla' non resta prima di 'Ripristina'", annulla < ripristina)
        assertTrue("'Originale' non chiude la fila", ripristina < originale)
    }

    /**
     * **Caso 53: 'Salva stile' vive sulla barra, nel solo modulo Stili e dal lato opposto.**
     *
     * ⚠️⚠️ **È IL SECONDO DEI TRE RITOCCHI** (sua richiesta del 2026-09-14: *'Salva stile' deve
     * stare in basso a sinistra, allineato all'inizio delle righe degli stili, ma fisso sulla barra
     * delle icone*): fino alla `2.51` era un tasto scritto in fondo al corpo del modulo, quindi
     * col FAB a destra stava dalla parte sbagliata e con l'elenco lungo si ritrovava scorrendo.
     * ⚠️ **La fila dei moduli si monta rovesciata**, come nella prova dell'ordine: con l'ottavo
     * gettone in coda la fila scorre, e quello che cade fuori dal viewport non si può toccare.
     */
    @Test
    fun `Salva stile compare nel solo modulo Stili e dalla parte del FAB`() {
        banco.setContent { Scena(mods = MOD_KEYS.reversed(), hand = Hand.RIGHT) }
        pronta()

        assertEquals(
            "il comando che salva compare in un modulo che non lo chiede",
            0,
            quantiDetti(R.string.look_preset_save)
        )
        banco.onNodeWithContentDescription(testo(R.string.look_presets)).performClick()
        banco.waitForIdle()
        assertEquals(
            "nel modulo Stili il comando che salva non c'è",
            1,
            quantiDetti(R.string.look_preset_save)
        )
        assertTrue(
            "col FAB a destra il comando che salva non sta dall'altra parte dei comandi",
            dove(R.string.look_preset_save) < dove(R.string.editor_undo)
        )
    }

    /**
     * **Caso 55: il modulo Effetti porta i suoi sei cursori, e nessun altro li ha.**
     *
     * ⚠️⚠️ **È IL NONO MODULO, DALLA `2.53`, E LA PRIMA COSA CHE PUÒ ROMPERSI IN SILENZIO È IL
     * LEGAME FRA I DUE ELENCHI**: un modulo vive nella tabella di `AdvancedEditorScreen` e la sua
     * chiave in `MOD_KEYS`, e chi ne dimenticasse una sparirebbe dalla fila senza che niente dia
     * errore. Qui si misura dalla parte di chi tocca: il gettone c'è, e aprendolo compaiono i
     * cursori che ha chiesto lui. ⚠️ **Erano cinque fino alla `2.63`, tre dalla `2.64`**, e dalla
     * `2.66` sono sei, coi tre secondari del suo campo libero.
     * ⚠️⚠️ **IL GETTONE SI PORTA IN TESTA, E SENZA QUELLA RIGA LA PROVA MENTIVA**: col nono
     * gettone la fila scorre, quindi in coda la pastiglia cade fuori dalla larghezza del banco; il
     * tocco non dà nessun errore e non cambia modulo, e si contavano zero cursori credendo di
     * guardare gli Effetti. È la stessa trappola del sesto gettone della `2.30`. ⚠️ **Fino alla
     * `2.54` la fila si montava rovesciata**, e con l'ordine nuovo quel rimedio si è rovesciato
     * addosso alla prova: il perché vive su [davanti].
     */
    @Test
    fun `il modulo Effetti porta i suoi sei cursori`() {
        banco.setContent { Scena(mods = davanti(PadKey.MOD_EFFECTS)) }
        pronta()

        assertEquals("il Ritaglio non ha cursori", 0, quantiCursori())
        modulo(R.string.look_effects)
        assertEquals(
            "gli Effetti devono portare i sei cursori del suo elenco", 6, quantiCursori()
        )

        modulo(R.string.look_light)
        assertEquals("e la Luce, toccata, porta i suoi sei", 6, quantiCursori())
    }

    /**
     * **Caso 55b: i tre cursori secondari si spengono finché il loro principale è a zero.**
     *
     * ⚠️⚠️ **È LA SECONDA METÀ DELLA SUA RICHIESTA, DALLA `2.66`** (campo libero del giro chiuso il
     * 2026-09-19: *tre slider secondari ... che si dovrà attivare solo se l'effetto relativo sta
     * modificando l'immagine*), e la cosa che può rompersi in silenzio è il **verso** della
     * condizione: scritta al contrario, i tre sarebbero accesi proprio quando non governano
     * niente, e spenti quando servono. Il codice sarebbe valido in tutti e due i casi.
     * ⚠️ **Le righe sono nell'ordine dell'elenco**: foschia, grana, 'Dimensione', 'Luci',
     * vignettatura, 'Sfumatura', cioè ognuno sotto il comando da cui dipende.
     */
    @Test
    fun `i cursori secondari degli Effetti si spengono col loro principale`() {
        banco.setContent { Scena(mods = davanti(PadKey.MOD_EFFECTS)) }
        pronta()
        modulo(R.string.look_effects)

        cursore(0).assertIsEnabled()
        cursore(1).assertIsEnabled()
        cursore(2).assertIsNotEnabled()
        cursore(3).assertIsNotEnabled()
        cursore(4).assertIsEnabled()
        cursore(5).assertIsNotEnabled()

        // La grana accende i suoi due e lascia spenta la 'Sfumatura' della vignettatura.
        muovi(1, 0.5f)
        cursore(2).assertIsEnabled()
        cursore(3).assertIsEnabled()
        cursore(5).assertIsNotEnabled()

        // E la vignettatura accende la sua.
        muovi(4, 0.8f)
        cursore(5).assertIsEnabled()
    }

    /**
     * **Caso 55c: i tre secondari non contano come lavoro, e la cella della grana segue la sua
     * 'Dimensione'.**
     *
     * ⚠️⚠️ **CONTANDOLI, UN'IMMAGINE CON LA SOLA 'Sfumatura' MOSSA SI DICHIAREREBBE DA
     * RISCRIVERE**, cioè verrebbe ricompressa per un valore che non cambia un pixel. È il criterio
     * del raggio e della mascheratura del Dettaglio, su un modulo in più.
     * ⚠️⚠️ **E LA CELLA DELLA GRANA RESTA UNA FRAZIONE DEL LATO**: il cursore la raddoppia e la
     * dimezza, e le due immagini su cui il conto gira (l'anteprima ridotta e il file pieno) devono
     * vederla in proporzione, o la grana dell'anteprima non sarebbe quella che si salva.
     */
    @Test
    fun `i secondari degli Effetti non contano come lavoro`() {
        assertTrue(Effects(grainSize = 1f).idle)
        assertTrue(Effects(grainLift = 1f).idle)
        assertTrue(Effects(vignetteFeather = 1f).idle)
        assertTrue(Look(effects = Effects(vignetteFeather = 1f)).lossless)
        assertFalse(Effects(grain = 0.01f).idle)

        /*
         * ⚠️ **E nemmeno un bordo sulle tessere**: nessuno dei tre legge un pixel vicino, quindi
         * la guardia di `bleed` resta scritta sul solo raggio della foschia.
         */
        assertEquals(0, Effects(grain = 1f, grainSize = 1f, grainLift = 1f).bleed(4000f))

        val serie = Effects.grainCell(4000f)
        assertEquals("a riposo la cella è quella di sempre", serie, Effects.grainCell(4000f, 0f), 1e-4f)
        assertEquals("a fondo corsa raddoppia", 2f * serie, Effects.grainCell(4000f, 1f), 1e-3f)
        assertEquals("e nell'altro verso dimezza", serie / 2f, Effects.grainCell(4000f, -1f), 1e-3f)
        assertEquals(
            "e resta una frazione del lato",
            2f * Effects.grainCell(2000f, 0.5f), Effects.grainCell(4000f, 0.5f), 1e-3f
        )
        /*
         * ⚠️⚠️ **SOTTO IL PIXEL IL PAVIMENTO ENTRA IN FUNZIONE, E DALLA `2.66` SI INCONTRA ANCHE
         * SULL'ANTEPRIMA**: una cella più stretta di un pixel non è una grana più fine, è uno
         * sfarfallio, quindi là la proporzione si rompe apposta.
         */
        assertEquals("il pavimento è il pixel", 1f, Effects.grainCell(800f, -1f), 1e-4f)
    }

    /**
     * **Caso 56: il 'Reset modulo' degli Effetti azzera i suoi e lascia stare gli altri.**
     *
     * ⚠️ Col nono modulo i gettoni sono nove, e un azzeramento che prendesse anche i vicini
     * porterebbe via il lavoro fatto in una schermata che non si sta guardando. È la stessa
     * misura del caso 15, su un modulo in più.
     * ⚠️ **La fila si monta rovesciata**, per la stessa ragione del caso qui sopra: in coda il
     * gettone non si può toccare.
     */
    @Test
    fun `il tocco lungo sugli Effetti azzera solo gli Effetti`() {
        // ⚠️ Il gettone si porta in testa, o cade fuori dalla larghezza del banco: vedi [davanti].
        banco.setContent { Scena(mods = davanti(PadKey.MOD_EFFECTS)) }
        pronta()
        modulo(R.string.look_light)
        muovi(1, 0.5f)
        assertTrue(valore(1) > 0.2f)

        modulo(R.string.look_effects)
        muovi(0, 0.6f)
        assertTrue(valore(0) > 0.2f)

        banco.onNodeWithContentDescription(testo(R.string.look_effects))
            .performTouchInput { longClick() }
        banco.waitForIdle()
        assertEquals("gli Effetti dovevano azzerarsi", 0f, valore(0), 1e-3f)

        modulo(R.string.look_light)
        assertTrue("la luce non doveva essere toccata", valore(1) > 0.2f)
    }

    /**
     * **Caso 57: gli Effetti tolgono il senza perdita, e le loro misure seguono il lato.**
     *
     * ⚠️⚠️ **IL SENZA PERDITA È LA CLAUSOLA CON CUI HA CHIESTO L'EDITOR** (*quelle che non
     * prevedono la riscrittura del file pixel per pixel devono essere lossless*): tutti e tre i
     * cursori riscrivono i pixel come la Luce, quindi un loro valore mosso lo toglie. Scritto al
     * contrario, una fotografia con la foschia tolta si salverebbe girando un tag EXIF, cioè non
     * si salverebbe affatto. ⚠️ **Vale anche per i due della `2.57`**, che non leggono i vicini ma
     * cambiano comunque ogni pixel che toccano.
     * ⚠️ **E le misure sono frazioni del lato**, come quelle del Dettaglio e per la stessa ragione:
     * il conto gira sull'anteprima e sul file pieno, e un raggio in pixel peserebbe il doppio da
     * una parte.
     */
    @Test
    fun `un valore di Effetti toglie il senza perdita`() {
        assertTrue(Effects.NONE.idle)
        assertTrue(Look.NONE.lossless)
        assertFalse(Effects(haze = 0.01f).idle)
        assertFalse(Effects(vignette = 0.01f).idle)
        assertFalse(Effects(grain = 0.01f).idle)
        assertFalse(Look(effects = Effects(haze = 0.5f)).lossless)
        assertFalse(Look(effects = Effects(vignette = 0.5f)).lossless)
        assertFalse(Look(effects = Effects(grain = 0.5f)).lossless)

        assertEquals(2f * Effects.hazeReach(1000f), Effects.hazeReach(2000f), 1e-4f)
        /*
         * ⚠️⚠️ **IL VELO SI STIMA PIÙ LONTANO DI QUANTO IL DETTAGLIO GUARDI, ANCHE AL FONDO DELLA
         * SUA CORSA**: la foschia è una proprietà di una **regione**, quindi la sua stima deve
         * cambiare più piano del disegno; scesa sotto quel confine, il conto scambierebbe il velo
         * per il dettaglio e ne accentuerebbe i bordi. È anche l'invariante da cui dipende il
         * bordo delle tessere, che è il massimo dei due moduli (caso 58).
         */
        assertTrue(
            "il velo si stima più lontano del raggio massimo della nitidezza",
            Effects.hazeReach(4000f) > Detail(sharpen = 1f, radius = 1f).sharpReach(4000f)
        )
    }

    /**
     * **Caso 58: il bordo delle tessere è il più largo dei due filtri che guardano i vicini.**
     *
     * ⚠️⚠️ **DALLA `2.53` I MODULI CHE LEGGONO I PIXEL VICINI SONO DUE, E UNA TESSERA HA UN BORDO
     * SOLO**: prendendo quello del solo Dettaglio, con gli Effetti mossi più forte l'ultima
     * colonna di una tessera leggerebbe il bordo ripetuto invece del pixel che sta di là, cioè su
     * ogni giunzione comparirebbe una riga. E sommarli sarebbe spazio buttato, perché i due filtri
     * girano sulla stessa tessera e non uno sull'uscita dell'altro.
     * ⚠️ **A riposo vale zero**, quindi chi non usa nessuno dei due paga le tessere di prima.
     */
    @Test
    fun `il bordo delle tessere copre il filtro più largo`() {
        assertEquals(0, Effects.NONE.bleed(4000f))

        val velo = Effects(haze = 0.5f)
        assertTrue(
            "il bordo deve coprire il raggio della stima del velo",
            velo.bleed(4000f) > Effects.hazeReach(4000f)
        )

        /*
         * ⚠️⚠️ **E I DUE DELLA `2.57` NON ENTRANO NEL CONTO, ED È QUELLO CHE LI DISTINGUE**:
         * vignettatura e grana non leggono nessun pixel vicino, quindi non c'è niente da buttare
         * via sul bordo di una tessera. La guardia va scritta sul **raggio** e non su `idle`: un
         * modulo mosso con la sola vignettatura non è a riposo, e con la guardia sbagliata quel
         * caso pagherebbe un pixel di bordo per niente, cioè un passo più stretto su ogni tessera.
         */
        assertEquals(
            "la vignettatura da sola non deve chiedere nessun bordo",
            0,
            Effects(vignette = -1f).bleed(4000f)
        )
        assertEquals(
            "e nemmeno la grana",
            0,
            Effects(grain = 1f).bleed(4000f)
        )
        assertEquals(
            "coi due addosso alla foschia il bordo resta quello della foschia",
            velo.bleed(4000f),
            Effects(haze = 0.5f, vignette = -1f, grain = 1f).bleed(4000f)
        )

        /*
         * ⚠️⚠️ **QUI SI CHIAMA LA FUNZIONE CHE IL SALVATAGGIO USA, E NON SI RIFÀ IL CONTO**: un
         * massimo riscritto nella prova sarebbe verde anche col difetto rimesso, perché
         * misurerebbe se stesso. `bleedFor` vive fuori dall'oggetto proprio per questo.
         */
        val fine = Detail(sharpen = 0.5f)
        assertEquals(
            "col solo Dettaglio il bordo deve restare il suo",
            fine.bleed(4000f),
            bleedFor(Look(detail = fine), 4000f)
        )
        assertEquals(
            "col solo modulo nuovo il bordo deve essere il suo, e non zero",
            velo.bleed(4000f),
            bleedFor(Look(effects = velo), 4000f)
        )
        // Coi due insieme si prende il più largo: né quello di uno solo, né la somma.
        val piccolo = Detail(sharpen = 0.5f, radius = -1f)
        assertTrue(
            "la scena deve avere due bordi diversi, o la misura non distingue niente",
            piccolo.bleed(4000f) < velo.bleed(4000f)
        )
        assertEquals(
            "il bordo dei due insieme non è il più largo",
            velo.bleed(4000f),
            bleedFor(Look(detail = piccolo, effects = velo), 4000f)
        )
        assertEquals("e a riposo resta zero", 0, bleedFor(Look.NONE, 4000f))
    }

    /**
     * **Caso 59: una tessera dichiara dove si trova, e il segno lo mette la funzione.**
     *
     * ⚠️⚠️ **È LA COSA CHE LA `2.57` PORTA DI ROMPIBILE IN SILENZIO**: vignettatura e grana leggono
     * **dove** cade un pixel dentro l'immagine intera, e nel salvataggio quel dato arriva da
     * [Framed]. Un segno rovesciato non dà nessun errore e non si vede sull'anteprima, dove la
     * tessera è una sola: si vedrebbe **solo** su un file grande, come un centro della vignettatura
     * spostato del doppio della distanza della tessera dall'angolo.
     * ⚠️ **Dentro una tessera `p` parte da zero sul suo angolo**, quindi l'immagine intera comincia
     * più indietro: l'origine è negativa, e a metterla è la funzione invece del chiamante.
     * ⚠️ **E il lato lungo si RICAVA**, invece di essere un secondo dato: è quello che i raggi in
     * frazione del lato leggono, e scritto a parte potrebbe non combaciare con la misura accanto.
     */
    @Test
    fun `una tessera dichiara dove si trova dentro l'immagine`() {
        val tutta = Framed.whole(4000f, 3000f)
        assertEquals("l'immagine intera comincia a zero", 0f, tutta.left, 1e-4f)
        assertEquals(0f, tutta.top, 1e-4f)
        assertEquals("il lato lungo si ricava dalle due misure", 4000f, tutta.span, 1e-4f)

        // Una tessera che comincia a (1200, 800): là l'immagine intera comincia a meno di quello.
        val pezzo = Framed.tile(1200f, 800f, 4000f, 3000f)
        assertEquals("l'origine di una tessera è negativa", -1200f, pezzo.left, 1e-4f)
        assertEquals(-800f, pezzo.top, 1e-4f)
        assertEquals("e la misura resta quella dell'immagine intera", 4000f, pezzo.wide, 1e-4f)
        assertEquals(3000f, pezzo.tall, 1e-4f)
        assertEquals(4000f, pezzo.span, 1e-4f)

        /*
         * Il palco dichiara il rettangolo in cui l'immagine è disegnata sullo schermo, e là
         * l'origine è quella vera: il conto che lo shader fa è `(p - origine) / misura`, quindi con
         * questi due un punto dell'angolo in alto a sinistra cade a zero e uno in fondo a destra a
         * uno, in tutti e due i casi.
         */
        val palco = Framed.shown(40f, 120f, 600f, 400f)
        assertEquals(40f, palco.left, 1e-4f)
        assertEquals(120f, palco.top, 1e-4f)
        assertEquals(600f, palco.span, 1e-4f)

        /*
         * ⚠️ **La cella della grana è una frazione del lato**, come ogni altra misura di questo
         * editor: senza, l'anteprima ridotta mostrerebbe una grana di un'altra misura rispetto al
         * file salvato, cioè quello che si vede non sarebbe quello che si salva.
         * ⚠️⚠️ **LE DUE MISURE SUPERANO IL PAVIMENTO DI PROPOSITO, E LA PRIMA STESURA NO**:
         * sotto il pixel quella funzione si ferma, quindi a 2000 contro 4000 il confronto misurava
         * il pavimento invece della proporzione e la prova era **rossa col codice giusto**. Il
         * pavimento è il caso qui sotto, e si misura a parte.
         */
        assertEquals(
            2f * Effects.grainCell(4000f), Effects.grainCell(8000f), 1e-4f
        )
        assertTrue(
            "sull'anteprima dell'editor la cella deve stare sopra il pixel",
            Effects.grainCell(1600f) > 1f
        )
        assertEquals(
            "e sotto il pixel non si scende, o il rumore diventa uno sfarfallio",
            1f, Effects.grainCell(10f), 1e-4f
        )
    }

    /**
     * **Caso 60: il confronto col prima toglie un gruppo di campi e non tutti.**
     *
     * ⚠️⚠️ **È LA SUA RICHIESTA DELLA `2.58`** (campo libero del giro della `2.55`): chi sta
     * tarando un colore e preme per vedere com'era vuole vedere **quel** colore com'era, non
     * un'immagine che salta anche di inquadratura.
     *
     * ⚠️ **Si misura sulla GEOMETRIA e non sul Ritaglio**, e non è una scorciatoia: là il palco
     * fa solo quello, cioè il dito serve alle squadrette e il confronto non parte affatto. La
     * nota sul palco lo dichiara, e quello che si può misurare è il confine fra i due
     * comportamenti.
     */
    @Test
    @Config(qualifiers = "w600dp-h900dp")
    fun `il confronto tiene la posa fuori dai moduli che la governano`() {
        banco.setContent { Scena(uri = largo()) }
        pronta()
        /*
         * Il Ritaglio è il modulo aperto di fabbrica: di là si mette in posa.
         * ⚠️ **Il tasto si cerca per TESTO e non per descrizione**: con le etichette accese, che è
         * il valore di fabbrica, il nome parlato è la parola scritta sotto l'icona e l'icona resta
         * muta, o un lettore di schermo leggerebbe due volte la stessa voce.
         */
        banco.onNodeWithText(testo(R.string.editor_right)).performClick()
        banco.waitForIdle()

        val palco = banco.onNodeWithContentDescription(testo(R.string.look_compare))

        banco.onNodeWithContentDescription(testo(R.string.look_light)).performClick()
        banco.waitForIdle()
        val inLuce = palco.captureToImage().toPixelMap()
        assertEquals(
            "in un modulo di colore il 'Prima' doveva tenere la posa, cioè non cambiare niente",
            0, diversi(inLuce, premuto(palco))
        )

        banco.onNodeWithContentDescription(testo(R.string.look_geometry)).performClick()
        banco.waitForIdle()
        val inGeo = palco.captureToImage().toPixelMap()
        assertTrue(
            "nella Geometria il 'Prima' doveva mostrare l'immagine non girata",
            diversi(inGeo, premuto(palco)) > 0
        )
    }

    /**
     * **Caso 61: quello che il confronto tiene, campo per campo.**
     *
     * ⚠️⚠️ **QUI SI MISURA LA COSA CHE PUÒ ROMPERSI IN SILENZIO**: un campo di colore dimenticato
     * in [Look.place] resta applicato nel confronto, cioè non si vede più che cosa fa quel
     * cursore; e un campo di posa dimenticato fa saltare l'inquadratura sotto il dito. Nessuno dei
     * due dà errore, e a nessuno dei due arriva un compilatore.
     */
    @Test
    fun `il confronto tiene il dove e butta il colore`() {
        val pieno = Look(
            spin = Spin(1, true),
            crop = ImageEdit.Crop(0.1f, 0.2f, 0.8f, 0.9f),
            light = Light(exposure = 0.4f),
            chroma = Chroma(temp = 0.3f),
            mix = Mix(List(Mix.COUNT) { Band(hue = 0.1f, sat = -0.2f, lum = 0.3f) }),
            detail = Detail(sharpen = 0.6f),
            effects = Effects(haze = 0.5f),
            tone = Tone(all = Curve(listOf(Knot(0f, 0.2f), Knot(1f, 1f)))),
            geo = Geometry(straighten = 0.25f),
            framing = Framing(listOf(ImageEdit.Crop(0f, 0f, 0.5f, 0.5f)), 1)
        )
        val resta = pieno.place

        assertEquals("la posa resta", pieno.spin, resta.spin)
        assertEquals("il taglio resta", pieno.crop, resta.crop)
        assertEquals("la geometria resta", pieno.geo, resta.geo)
        assertEquals("e la vista pure, o l'immagine tornerebbe intera", pieno.framing, resta.framing)

        assertTrue("il colore se ne va tutto", resta.plain)
        assertEquals(Light.NONE, resta.light)
        assertEquals(Chroma.NONE, resta.chroma)
        assertEquals(Mix.NONE, resta.mix)
        assertEquals(Detail.NONE, resta.detail)
        assertEquals(Effects.NONE, resta.effects)
        assertEquals(Tone.NONE, resta.tone)
    }

    /**
     * **Caso 62: i due gesti del comando che scrive.**
     *
     * ⚠️ **È il gesto che un `TextButton` non poteva portare**, ed è la ragione per cui quel tasto
     * è scritto in casa: qui si misura che i due gesti arrivino a chi salva con la risposta giusta
     * alla domanda *accanto o sopra*.
     */
    @Test
    @Config(qualifiers = "w600dp-h900dp")
    fun `il tocco lungo su Salva chiede un file nuovo`() {
        val chiesti = mutableListOf<Boolean>()
        banco.setContent { Scena(onSave = { _, accanto -> chiesti += accanto }) }
        pronta()
        // Senza niente da salvare il comando è spento: prima si muove un cursore.
        banco.onNodeWithContentDescription(testo(R.string.look_light)).performClick()
        banco.waitForIdle()
        muovi(0, 0.5f)

        val salva = banco.onNodeWithText(testo(R.string.editor_save))
        salva.performClick()
        banco.waitForIdle()
        assertEquals("il tocco normale doveva riscrivere il file", listOf(false), chiesti)

        salva.performTouchInput { longClick() }
        banco.waitForIdle()
        assertEquals(
            "il tocco lungo doveva chiedere un file nuovo accanto",
            listOf(false, true), chiesti
        )
    }

    /**
     * **Caso 63: col file nuovo il bersaglio non è quello di partenza.**
     *
     * ⚠️ **Il banco può misurare questo e non il file scritto**: quello vuole una decodifica vera e
     * una scheda grafica. Qui si misura la sola cosa che il tocco lungo cambia, cioè **dove** si
     * scrive, e il nome lo sceglie la stessa funzione di ogni copia dell'app.
     */
    @Test
    fun `il file nuovo prende un nome libero accanto all'originale`() {
        val dir = File(app.cacheDir, "accanto").apply { deleteRecursively(); mkdirs() }
        val source = File(dir, "foto.jpg").apply { writeBytes(ByteArray(8)) }
        val jpeg = Bitmap.CompressFormat.JPEG

        assertEquals(
            "col tocco normale si riscrive il file di partenza",
            source, ImageEdit.lookTarget(source, dir, jpeg)
        )
        val nuovo = ImageEdit.lookTarget(source, dir, jpeg, beside = true)
        assertEquals("col tocco lungo il nome è libero e accanto", "foto (2).jpg", nuovo.name)
        assertEquals(dir, nuovo.parentFile)
    }

    /**
     * **Caso 64: le costanti che 'Auto' ricopia dallo shader dicono ancora il vero.**
     *
     * ⚠️⚠️ **È IL SOLO PRESIDIO POSSIBILE DI UNA COPIA FRA DUE LINGUAGGI**: quei tre numeri vivono
     * dentro `LOOK_AGSL`, che per il compilatore di Kotlin è un testo qualunque, quindi cambiarne
     * uno di là e lasciare la copia qui **non dà nessun errore**: il conto di 'Auto' lavorerebbe
     * sul numero di ieri e scriverebbe nei cursori valori che l'immagine non chiede.
     * ⚠️ **La stringa si legge davvero**, invece di fidarsi: la riga della costante si cerca con la
     * sua forma, e il numero si confronta col `const val` di [Auto].
     * ⚠️⚠️ **QUESTO CASO NASCE CON LA `2.59`, DOVE LA COPIA È DIVENTATA TRE**: fino alla `2.58` il
     * conto del contrasto portava un `2` che veniva dalla pendenza della curva di allora, e la
     * curva nuova quel due lo ha reso falso. Un numero scritto a mano che dipende da un altro
     * numero scritto a mano è esattamente quello che questa prova esiste per prendere.
     */
    @Test
    fun `le costanti ricopiate dallo shader combaciano`() {
        fun nelloShader(nome: String): Float {
            val riga = Regex("const half $nome = ([-0-9.]+);").find(LOOK_AGSL)
            assertNotNull("la costante $nome non è più nello shader", riga)
            return riga!!.groupValues[1].toFloat()
        }

        assertEquals("POINT_SHIFT", nelloShader("POINT_SHIFT"), Auto.POINT_SHIFT, 1e-6f)
        assertEquals("WB_REACH", nelloShader("WB_REACH"), Auto.WB_REACH, 1e-6f)
        assertEquals("CONTRAST_RISE", nelloShader("CONTRAST_RISE"), Auto.CONTRAST_RISE, 1e-6f)
    }

    /**
     * **Caso 64b: i due cursori secondari che lavorano nello shader restano dentro i loro
     * confini.**
     *
     * ⚠️⚠️ **IL CONTO VIVE IN AGSL E IL BANCO NON LO ESEGUE, QUINDI QUELLO CHE SI MISURA SONO I
     * NUMERI CHE HA DETTATO LUI**: le costanti dei due conti vivono dentro `LOOK_AGSL`, che per il
     * compilatore di Kotlin è un testo qualunque, e quello che può rompersi in silenzio è che
     * qualcuno le sposti. Che l'immagine venga bene si guarda sul telefono, e la voce di collaudo
     * lo chiede.
     * ⚠️⚠️ **DALLA `2.67` I NUMERI DELLA GRANA SONO I SUOI, E LA PROVA LI RICALCOLA** (voce
     * `eff-grana-luci`: *il 'quasi zero' passa da 0,8 a 0,1 ... metà scala a 0,35*): sul cielo la
     * grana piena deve muovere `0,1` livelli su 255.
     * ⚠️⚠️ **E DALLA `2.68` I VINCOLI SONO ALTRI DUE, PERCHÉ IL CURSORE RIDISTRIBUISCE** (voce
     * `grana-luci-2`: *un cielo quasi immacolato e delle ombre con un 30% in più di grana*): le
     * ombre devono prendere esattamente il `30% in più`, e a metà scala la grana piena deve restare
     * **sotto mezzo livello su 255**, cioè sotto la quantizzazione, che è la lettura operativa di
     * *quasi immacolato*. ⚠️ **Il `0,35` della `2.67` non si misura più**: con questi due vincoli
     * quel punto non è libero, e il conto lo dice sulle costanti.
     * ⚠️⚠️ **E A 'Luci' PIENO IL PESO TORNA `4t(1-t)`, che è la proprietà che il termine nuovo
     * poteva rompere in silenzio**: i due capi devono ritirarsi insieme, e con uno solo dei due a
     * uno il conto della `2.65` non torna più.
     * ⚠️ **Non è ricopiare l'implementazione**: la `smoothstep` qui sotto è la definizione
     * standard, e quello che si confronta sono i numeri della sua richiesta.
     * ⚠️⚠️ **E LA CORSA DELLA VIGNETTATURA HA DUE CONFINI GEOMETRICI**: verso il positivo il pieno
     * deve arrivare **almeno al bordo**, cioè a `0,707` di [fromCentre], che è il suo *poco
     * all'interno del bordo*; verso il negativo deve cadere **fuori** dall'angolo. E il pieno non
     * può arrivare sotto metà raggio, o l'immagine sarebbe scura dappertutto invece di essere
     * vignettata.
     */
    @Test
    fun `i confini dei due conti secondari degli Effetti`() {
        fun nelloShader(nome: String): Float {
            val riga = Regex("const half $nome = ([-0-9.]+);").find(LOOK_AGSL)
            assertNotNull("la costante $nome non è più nello shader", riga)
            return riga!!.groupValues[1].toFloat()
        }

        // Il raggio di mezzo lato in `fromCentre`, dove l'angolo vale uno.
        val bordo = 0.5f / 0.70710678f

        val soft = nelloShader("VIGNETTE_SOFT")
        assertTrue(
            "a fondo corsa positiva il pieno deve arrivare almeno al bordo",
            1f - soft <= bordo
        )
        assertTrue("e non deve scendere sotto metà raggio", 1f - soft > 0.5f)
        assertTrue("a fondo corsa negativa il pieno cade fuori dal fotogramma", 1f + soft > 1f)

        val lo = nelloShader("GRAIN_LIFT_LO")
        val hi = nelloShader("GRAIN_LIFT_HI")
        val floor = nelloShader("GRAIN_LIFT_FLOOR")
        val shade = nelloShader("GRAIN_LIFT_SHADE")
        val reach = nelloShader("GRAIN_REACH")
        assertTrue("le ombre restano intatte fino al quarto di scala", lo >= 0.25f)
        assertTrue("e la rampa finisce dopo che è cominciata", hi > lo)
        assertTrue("e vive dentro la scala dei toni", hi <= 1f)
        assertTrue("alle luci resta un filo di grana, non zero", floor > 0f && floor < 0.5f)

        fun smoothstep(a: Float, b: Float, x: Float): Float {
            val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
            return t * t * (3f - 2f * t)
        }

        // L'inviluppo dei mezzi toni, `4t(1-t)`, e il peso che la rampa gli applica.
        fun inviluppo(t: Float): Float = 1f - (2f * t - 1f) * (2f * t - 1f)
        fun peso(t: Float, lift: Float): Float {
            val up = smoothstep(lo, hi, t)
            val keep = floor + (1f - floor) * lift
            val deep = shade + (1f - shade) * lift
            return deep + (keep - deep) * up
        }

        fun livelli(t: Float, lift: Float): Float = reach * inviluppo(t) * peso(t, lift) * 255f

        assertEquals("nelle ombre la grana prende il 30% in più", 1.3f, peso(lo, 0f), 0.001f)
        assertEquals("e lo prende per tutte le ombre", peso(lo, 0f), peso(0.05f, 0f), 1e-6f)
        assertTrue(
            "a metà scala il cielo resta sotto mezzo livello su 255",
            livelli(0.5f, 0f) <= 0.5f
        )
        assertEquals(
            "sul cielo la grana piena muove un decimo di livello",
            0.1f, livelli(0.82f, 0f), 0.02f
        )

        // A 'Luci' pieno i due capi si ritirano insieme e resta l'inviluppo nudo della `2.65`.
        // ⚠️ Qui si guarda il TESTO del programma e non il conto ricostruito qui sopra: che il
        // guadagno delle ombre torni a uno è una proprietà di come lo shader lo compone, e un
        // guadagno scritto come costante (senza passare da `grainLift`) lascerebbe le ombre al 130%
        // anche a cursore pieno, cioè romperebbe quella promessa senza che nessun conto lo dica.
        assertTrue(
            "il guadagno delle ombre deve ritirarsi col cursore, come il pavimento",
            LOOK_AGSL.contains("mix(GRAIN_LIFT_SHADE, half(1.0), grainLift)")
        )
        for (i in 0..100) {
            val t = i / 100f
            assertEquals("a 'Luci' pieno il peso torna quello della 2.65", 1f, peso(t, 1f), 1e-5f)
        }
    }

    /**
     * **Caso 65: il kernel della mappa del velo non passa nessuna frequenza intera.**
     *
     * ⚠️⚠️ **È IL PRESIDIO DEL RETICOLO DELLA `2.65`, ED È L'UNICO CHE IL BANCO POSSA DARE**: il
     * conto della foschia vive in AGSL e su una tela di memoria non gira, quindi che l'immagine
     * venga bene si guarda sul telefono. Quello che si può misurare è il **kernel**, cioè dove
     * cadono i nove campioni e quanto pesano, e da lì la sua risposta in frequenza, che è la
     * proprietà da cui il difetto nasceva.
     * ⚠️⚠️ **IL FATTO CHE MISURA**: nove delta su una **griglia** a passo `s` rispondono
     * esattamente **1,000** a ogni multiplo di `1/s`, cioè a quei periodi non mediano affatto e
     * la mappa del velo copia la trama dell'immagine. Con i due anelli nessuna frequenza passa
     * intera. ⚠️ **La soglia è più bassa del valore misurato di proposito** (0,870): qui si
     * presidia il fatto che nessuna frequenza passi **intera**, non il numero di oggi, così un
     * ritocco ai raggi non fa diventare rossa una prova mentre il comportamento è ancora giusto.
     * ⚠️ **Il kernel si legge dalla stringa dello shader**, come i tre numeri del caso 64: in
     * Kotlin quelle righe sono un testo qualunque, quindi una direzione cambiata di là non dà
     * nessun errore.
     * ⚠️ **Controprovata rimettendo la griglia 3x3 della `2.64`**: la risposta torna a 1,000 e
     * tutte e tre le asserzioni di questo caso cadono.
     */
    @Test
    fun `il kernel del velo non copia nessuna frequenza`() {
        val pesi = Regex("const half (HAZE_(?:HUB|WIDE|TIGHT)) = ([-0-9.]+);")
            .findAll(LOOK_AGSL)
            .associate { it.groupValues[1] to it.groupValues[2].toDouble() }
        assertEquals("i tre pesi del kernel sono ancora nello shader", 3, pesi.size)

        // Il corpo di `veiled`: ogni istruzione porta le sue direzioni e il peso che le governa,
        // e quella senza direzioni è il centro.
        val corpo = LOOK_AGSL.substringAfter("half veiled(float2 p, float2 step) {")
            .substringBefore("\n}")
        val campioni = mutableListOf<Triple<Double, Double, Double>>()
        for (pezzo in corpo.split(";")) {
            val quale = Regex("HAZE_(?:HUB|WIDE|TIGHT)").findAll(pezzo).lastOrNull() ?: continue
            val w = pesi.getValue(quale.value)
            val dove = Regex("""float2\(\s*(-?[0-9.]+),\s*(-?[0-9.]+)\)""").findAll(pezzo).toList()
            if (dove.isEmpty()) {
                campioni += Triple(0.0, 0.0, w)
            } else {
                dove.forEach {
                    campioni += Triple(
                        it.groupValues[1].toDouble(), it.groupValues[2].toDouble(), w
                    )
                }
            }
        }
        assertEquals("i campioni della mappa del velo sono nove", 9, campioni.size)

        val totale = campioni.sumOf { it.third }
        assertEquals("i pesi sommano a sedici", 16.0, totale, 1e-3)
        // Senza questo, la stima del velo sarebbe presa di lato e il velo si toglierebbe spostato.
        assertEquals("il kernel è centrato in orizzontale", 0.0, campioni.sumOf { it.first * it.third }, 1e-3)
        assertEquals("il kernel è centrato in verticale", 0.0, campioni.sumOf { it.second * it.third }, 1e-3)
        // Il bordo delle tessere del salvataggio dichiara esattamente questo raggio.
        assertEquals(
            "nessun campione va oltre il raggio dichiarato",
            1.0, campioni.maxOf { hypot(it.first, it.second) }, 1e-4
        )

        // La risposta del kernel, in cicli per unità di raggio: da un terzo (il velo di una
        // regione, che deve passare) fino a otto, che è Nyquist su un'anteprima da 1600 pixel.
        fun risposta(f: Double, ang: Double): Double {
            var re = 0.0
            var im = 0.0
            for ((x, y, w) in campioni) {
                val fase = -2.0 * Math.PI * (f * cos(ang) * x + f * sin(ang) * y)
                re += w * cos(fase)
                im += w * sin(fase)
            }
            return hypot(re, im) / totale
        }

        var peggio = 0.0
        var dovePeggio = 0.0
        for (i in 0..240) {
            val f = 1.0 / 3.0 + (8.0 - 1.0 / 3.0) * i / 240.0
            for (j in 0..40) {
                val v = risposta(f, Math.PI / 2 * j / 40.0)
                if (v > peggio) {
                    peggio = v
                    dovePeggio = f
                }
            }
        }
        assertTrue(
            "nessuna frequenza deve passare intera: %.3f a %.2f cicli per raggio"
                .format(peggio, dovePeggio),
            peggio < 0.95
        )
        // I quattro periodi su cui la griglia della `2.64` rispondeva 1,000, lungo un asse.
        for (perRaggio in listOf(1.0, 2.0, 3.0, 4.0)) {
            val v = risposta(perRaggio, 0.0)
            assertTrue(
                "a %d cicli per raggio la stima copia ancora (%.3f)".format(perRaggio.toInt(), v),
                v < 0.70
            )
        }
        // E il velo vero deve passare: una regione larga dieci volte il raggio.
        assertTrue(
            "il velo di una regione non deve perdersi",
            risposta(0.1, 0.0) > 0.9
        )
    }

    /*
     * ⚠️⚠️ **QUI VIVEVA IL CASO DEI COMANDI DEL RITAGLIO LONTANI DALLA BARRA, E DALLA `2.75` NON
     * C'È PIÙ**: presidiava l'aria della `2.73` (punto 2 del campo libero del giro della `2.70`),
     * e il giro dopo l'ha revocata (voce `crop-alti` non approvata: *Mi sembrava ci fosse spazio,
     * invece con lo spostamento s'è ammucchiato tutto. Riporta allo stato precedente*).
     * ⚠️ **La misura del banco diceva il vero e guardava la cosa sbagliata**: l'aria passava da 7
     * pixel a 14, cioè raddoppiava, e la scheda non si alzava di un pixel. Quello che nessuna
     * misura poteva vedere è come quel blocco si legge sul telefono, dove lo spazio che `Breathe`
     * distribuisce è calato della stessa quantità e il resto del corpo si è stretto. È il caso
     * dichiarato in § '🧪 Quando si scrive una prova, e quando no', cioè quello che dipende dalla
     * resa vera.
     */

    /**
     * **Caso 66: la seconda slide arriva dopo la prima, e i suoi due paragrafi cadono ognuno
     * accanto a quello che indicano.**
     *
     * ⚠️⚠️ **LE TRE COSE CHE POSSONO ROMPERSI IN SILENZIO SONO QUESTE.** La prima è l'**ordine**:
     * con la condizione scritta al contrario i due veli sarebbero in scena insieme, cioè un fondo
     * scuro doppio con due frasi sovrapposte, e nessun compilatore lo direbbe. La seconda è
     * **dove** cade una copia: `HintSpots` riceve un riquadro misurato per ognuna, e chi le
     * posasse in celle uguali le metterebbe accanto ai tasti invece che sopra. La terza è **da che
     * parte** cade ogni frase, che dalla `2.79` non è più una sola: i gruppi sono due e lontani,
     * quindi una frase messa dalla parte sbagliata copre il tasto che sta indicando.
     * ⚠️⚠️ **I DUE VERSI SI MISURANO TUTTI E DUE, ED È QUELLO CHE PRESIDIA LA REGOLA**: `hint_save`
     * indica un tasto in testata, quindi va **sotto**; `hint_tools` indica i due tasti della barra
     * in basso, quindi va **sopra**. Con una regola scritta fissa da una parte, uno dei due
     * paragrafi finirebbe sopra il suo bersaglio, e si vede solo aprendo l'editor la prima volta,
     * cioè una volta per telefono.
     * ⚠️ **Il centro e non il riquadro**: il tasto vero porta il proprio rientro dentro il nodo,
     * la copia è il solo testo centrato nella stessa scatola, quindi le due misure coincidono nel
     * centro e non nei lati.
     * ⚠️ **Non vede le copie dei GLIFI**, che sono disegni senza descrizione parlata: che
     * l'arancione cada sull'icona giusta si guarda sul telefono, e la voce di collaudo lo chiede.
     * ⚠️ **E il tasto della filigrana qui non c'è**, perché la scena non porta nessun logo scelto:
     * il gruppo di sotto ha il solo 'Ridimensiona', che è il tasto della barra che c'è sempre.
     * ⚠️⚠️ **E SI MISURA SULL'ALBERO NON FUSO, CHE È LA TRAPPOLA DEL BANCO QUI**: il velo porta un
     * `clickable`, quindi è un nodo che **assorbe** la semantica dei figli, e la copia di 'Salva'
     * col suo testo finisce dentro di lui. Sull'albero fuso il secondo nodo trovato è il velo
     * intero, cioè un riquadro grande quanto lo schermo: la prima stesura ne misurava il centro e
     * cadeva con 242 pixel di scarto, su un codice giusto.
     * ⚠️ **Controprovata tre volte**: rovesciando la condizione della sequenza i due veli si
     * trovano in scena insieme e la prima asserzione conta una frase invece di nessuna;
     * dimenticando la colonna del riquadro, cioè posando le copie all'inizio della riga, il centro
     * della parola si sposta di 251 pixel; e scrivendo il verso fisso, cioè la frase sempre sotto
     * il proprio gruppo, `hint_tools` finisce **fuori dal vetro** e cade l'asserzione che la vuole
     * in scena.
     */
    @Test
    fun `la seconda slide dell'onboarding segue la prima`() {
        // ⚠️ Tutte e due le chiavi tornano da capo: `@Before` le archivia, e qui si misura
        // proprio il velo che quelle righe esistono per togliere di mezzo.
        runBlocking {
            Hint.MODULES.forget(app)
            Hint.EDITOR_TOOLS.forget(app)
        }
        banco.setContent { Scena() }
        banco.waitForIdle()

        val sopra = testo(R.string.hint_save)
        val sotto = testo(R.string.hint_tools)
        banco.onNodeWithText(testo(R.string.hint_modules)).assertExists()
        assertEquals(
            "finché c'è la prima slide, la seconda non è in scena",
            0,
            banco.onAllNodesWithText(sotto, useUnmergedTree = true).fetchSemanticsNodes().size
        )

        // Archiviata la prima, arriva la seconda: è la strada di chi tocca il primo velo.
        runBlocking { Hint.MODULES.remember(app) }
        banco.waitForIdle()
        banco.onNodeWithText(sopra, useUnmergedTree = true).assertExists()
        banco.onNodeWithText(sotto, useUnmergedTree = true).assertExists()

        val salva = banco
            .onAllNodesWithText(testo(R.string.editor_save), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertEquals("il velo disegna la propria copia di 'Salva'", 2, salva.size)
        val centri = salva.map { it.boundsInRoot.center }
        val scarto = (centri[0] - centri[1]).getDistance()
        assertTrue(
            "la copia cade sul tasto vero (%.0f pixel di scarto)".format(scarto),
            scarto < 2f
        )

        // Il paragrafo di 'Salva' cade sotto di lui, che vive in cima allo schermo.
        assertTrue(
            "il paragrafo di 'Salva' non cade sotto il tasto",
            banco.onNodeWithText(sopra, useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot.top >= salva.maxOf { it.boundsInRoot.bottom }
        )

        // E quello dei due comandi cade sopra la barra, o li coprirebbe.
        val barra = banco.onNodeWithContentDescription(testo(R.string.look_resize))
            .fetchSemanticsNode().boundsInRoot
        val frase = banco.onNodeWithText(sotto, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        /*
         * ⚠️⚠️ **'È IN SCENA' SI MISURA PRIMA DI 'DOVE CADE', E SENZA QUELLA RIGA LA PROVA ERA
         * VERDE A VUOTO**: col verso scritto fisso quella frase finisce **sotto** i tasti, cioè
         * fuori dal vetro, e un nodo ritagliato via risponde con un riquadro vuoto. Il confronto
         * da solo lo leggeva come 'sopra la barra', perché uno zero è sopra qualunque cosa.
         */
        assertTrue("il paragrafo dei due comandi non è in scena", frase.height > 0f)
        assertTrue("il paragrafo dei due comandi non cade sopra la barra", frase.bottom <= barra.top)
    }

    /**
     * Il palco col dito premuto: il confronto si accende con un'attesa, quindi il tempo va mosso a
     * mano. Vedi il caso 14 di `LuceTest`.
     */
    private fun premuto(palco: SemanticsNodeInteraction): PixelMap {
        palco.performTouchInput { down(center) }
        banco.mainClock.advanceTimeBy(ATTESA_DITO)
        banco.waitForIdle()
        val scatto = palco.captureToImage().toPixelMap()
        palco.performTouchInput { up() }
        banco.waitForIdle()
        return scatto
    }

    @Composable
    private fun Scena(
        uri: Uri = quadrato(),
        mods: List<PadKey> = MOD_KEYS,
        hand: Hand = Hand.RIGHT,
        onSave: (Look, Boolean) -> Unit = { _, _ -> }
    ) {
        AivTheme(darkTheme = false) {
            // ⚠️ L'ordine dei moduli viaggia di qui anche nell'app: la scheda le impostazioni
            // non le riceve, quindi una prova che lo passasse per parametro misurerebbe una
            // strada che nessuno percorre.
            CompositionLocalProvider(LocalPadLook provides PadLook(mods = mods, hand = hand)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AdvancedEditorScreen(
                        uri = uri,
                        busy = false,
                        // ⚠️ Senza filigrana: il caso che la porta vive in `FiligranaTest`.
                        marked = false,
                        marking = false,
                        hasMark = false,
                        onMark = {},
                        onMarkSetup = {},
                        resize = Resize.Plan(Resize.Mode.LONG, Resize.DEFAULT_PX),
                        // ⚠️ Spento: un ridimensionamento che rimpicciolisce accenderebbe 'Salva' a
                        // immagine intonsa, e il caso suo vive in `RidimensionaTest`.
                        saved = Resize.NONE,
                        resizing = false,
                        onResize = {},
                        onResizeDefault = {},
                        onSave = onSave,
                        onBack = {}
                    )
                }
            }
        }
    }

    /**
     * Un PNG **rettangolare**, per le prove che guardano la posa.
     *
     * ⚠️ **Il quadrato non serve qui**: girato di un quarto viene identico, quindi una prova che
     * misura una rotazione resterebbe verde con qualunque codice.
     */
    private fun largo(): Uri {
        val file = File(app.cacheDir, "largo.png")
        if (!file.exists()) {
            val mappa = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
            for (y in 0 until 32) {
                for (x in 0 until 64) {
                    mappa.setPixel(x, y, if (x < 32) Color.WHITE else Color.rgb(90, 90, 90))
                }
            }
            file.outputStream().use { mappa.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return Uri.fromFile(file)
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

    /**
     * Un PNG che va dal 20% all'80% della scala, per le prove che hanno bisogno di **toni**.
     *
     * ⚠️ **Il quadrato bianco non serve a 'Auto'**: su un colore solo la guardia dell'immagine
     * piatta risponde che non c'è niente da stirare, quindi il tasto non scriverebbe niente e la
     * misura direbbe che non funziona proprio dove è giusto che non faccia nulla.
     */
    private fun sfumato(): Uri {
        val file = File(app.cacheDir, "sfumato.png")
        if (!file.exists()) {
            val mappa = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            for (y in 0 until 64) {
                val v = 51 + 153 * y / 63
                for (x in 0 until 64) mappa.setPixel(x, y, Color.rgb(v, v, v))
            }
            file.outputStream().use { mappa.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return Uri.fromFile(file)
    }
}

/**
 * Quanto si tiene il dito fermo sul palco perché valga come tocco lungo.
 *
 * ⚠️ **Il tempo del banco si muove a mano**, e per lo stesso motivo del caso 14 di `LuceTest`: il
 * confronto si accende dopo un'attesa, e col clock fermo quell'attesa non scatta da sé.
 */
private const val ATTESA_DITO = 1000L

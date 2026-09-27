package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.ComponentDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import coil3.request.allowHardware
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 'Genera miniature' e le sue due conferme, dalla `2.97`.
 *
 * ⚠️⚠️ **NASCE CON LA FUNZIONE E NON DOPO UN DIFETTO**, ed è il caso proattivo di `AIV/CLAUDE.md`
 * § '🧪 Quando si scrive una prova, e quando no': la generazione passa da tre cose che nessun
 * compilatore guarda, cioè un caricatore che deve **non** decodificare, un segno che deve **non**
 * consumare, e un lavoro su più corsie che deve fare ogni file una volta sola.
 *
 * ⚠️⚠️ **CHE COSA MISURA E CHE COSA NO.** Misura il caricatore della generazione su un file vero,
 * le corsie con un lavoro finto, le due conferme con due comandi finti, e la pagina di
 * avanzamento. **Non** misura la generazione su una collezione vera: vuole un MediaStore con delle
 * immagini dentro, e quello del banco è vuoto; e non vede la miniatura che il sistema salva, perché
 * il banco non ha un provider che la faccia. Il giro vuoto dall'inizio alla fine vive in
 * `GeneraMiniatureCorsaTest`, che ha bisogno di un'ombra che qui non serve.
 *
 * ⚠️ **La grafica è quella vera** (`NATIVE`), perché il caso del caricatore decodifica un PNG per
 * davvero: senza, la decodifica di confronto non avrebbe una misura da dare.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GeneraMiniatureTest {

    @get:Rule
    val banco = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private fun testo(id: Int) = app.getString(id)

    /*
     * ⚠️ **Il canale degli avvisi si azzera**, perché è un oggetto di processo: 'Sì' su 'Svuota'
     * posa la sua riga, e una riga lasciata in scena entrerebbe nella prova dopo.
     */
    @Before
    fun prepara() = azzera()

    @After
    fun ripulisci() = azzera()

    private fun azzera() {
        Notices.line?.let { Notices.dismiss(it.id) }
    }

    /**
     * **La generazione non consuma il segno di un file appena riscritto.**
     *
     * ⚠️⚠️ **È LA RIGA CHE PROTEGGE LA CORREZIONE DELLA `2.25`**: il segno dice alla prossima
     * miniatura mostrata di quel file di venire dal file vero, e la generazione la sua non la mostra
     * a nessuno. Consumandolo, la griglia tornerebbe a chiedere al sistema la miniatura di prima.
     * ⚠️ **Controprovata togliendo la guardia da [Thumbs.warm]**: il fetcher di sistema consuma il
     * segno, e la prova cade.
     */
    @Test
    fun `la generazione lascia il segno di un file riscritto`() = runBlocking {
        val file = File(app.filesDir, "genera-riscritto.png").apply { writeBytes(pngConAlfa()) }
        val uri = file.toUri()
        Thumbs.forget(app, uri)

        val caricatore = Thumbs.warmer(app)
        try {
            Thumbs.warm(app, uri, caricatore)
        } finally {
            caricatore.shutdown()
        }

        assertTrue("la generazione ha consumato il segno che spetta alla griglia", Thumbs.rewritten(uri))
    }

    /**
     * **Il caricatore della generazione si ferma dove quello della griglia decodificherebbe.**
     *
     * ⚠️⚠️ **UN PNG CON L'ALFA È IL CASO GIUSTO, perché non passa dal sistema**: la sua miniatura la
     * fa la decodifica normale, e per la generazione sarebbe lavoro buttato (il perché vive su
     * [Thumbs.warmer]). Al posto dell'immagine arriva il segnaposto da un pixel.
     * ⚠️ **La prima metà è la condizione della prova**: la stessa richiesta, col caricatore della
     * griglia, dà la miniatura vera, quindi il pixel solo non viene da un file che non si legge.
     * ⚠️ **Controprovata rimettendo il decodificatore della griglia nel caricatore della
     * generazione**: la miniatura esce alta [Thumbs.PX] anche là, e la prova cade.
     */
    @Test
    fun `il caricatore della generazione non decodifica niente`() = runBlocking {
        val file = File(app.filesDir, "genera-alfa.png").apply { writeBytes(pngConAlfa()) }
        val richiesta = Thumbs.request(app, file.toUri()).newBuilder().allowHardware(false).build()

        val vera = Thumbs.loader(app).execute(richiesta)
        assertTrue("il file doveva decodificarsi: ${(vera as? ErrorResult)?.throwable}", vera is SuccessResult)
        assertEquals(Thumbs.PX, (vera as SuccessResult).image.height)

        val caricatore = Thumbs.warmer(app)
        val generata = try {
            caricatore.execute(richiesta)
        } finally {
            caricatore.shutdown()
        }
        assertTrue(
            "la catena della generazione si è fermata: ${(generata as? ErrorResult)?.throwable}",
            generata is SuccessResult
        )
        assertEquals(1, (generata as SuccessResult).image.width)
    }

    /**
     * **Ogni voce passa una volta sola, qualunque corsia la prenda, e un file che non si legge
     * conta come fatto.**
     *
     * ⚠️ **Il `yield` alterna davvero le corsie**: senza, la prima prenderebbe tutto prima che le
     * altre partano, e una voce presa due volte non si vedrebbe.
     * ⚠️ **Il conto va da uno al totale senza buchi**, che è il numero X della pagina: un file che
     * non si legge e non contasse lo farebbe fermare prima della fine.
     * ⚠️ **Controprovata due volte**: togliendo la cattura dell'errore la prova cade col primo file
     * che non si legge, e con un contatore per corsia ogni voce passa tre volte.
     */
    @Test
    fun `ogni voce passa una volta sola e un errore conta come fatto`() = runBlocking {
        val voci = (0 until 40).toList()
        val viste = ConcurrentHashMap<Int, Int>()
        val conti = mutableListOf<Int>()

        warmAll(voci, lanes = 3, work = { voce ->
            viste.merge(voce, 1, Int::plus)
            yield()
            if (voce % 7 == 0) error("il file $voce non si legge")
        }) { fatti -> conti += fatti }

        assertEquals(voci.toSet(), viste.keys)
        assertTrue("una voce è passata più di una volta: $viste", viste.values.all { it == 1 })
        assertEquals((1..voci.size).toList(), conti)
    }

    /**
     * **Un annullamento non conta come fatto il file che interrompe.**
     *
     * ⚠️ **È la sola eccezione che la cattura lascia passare**: presa come un errore di un file,
     * l'annullamento segnerebbe un file in più, cioè un numero X che dice una cosa falsa nell'istante
     * in cui la pagina se ne va.
     * ⚠️ **Controprovata togliendo il rilancio dell'annullamento**: il conto arriva a quattro.
     */
    @Test
    fun `un annullamento non conta la voce che interrompe`() = runBlocking {
        val partite = mutableListOf<Int>()
        val conti = mutableListOf<Int>()
        val arrivata = CompletableDeferred<Unit>()

        val lavoro = launch {
            warmAll((0 until 10).toList(), lanes = 1, work = { voce ->
                partite += voce
                if (voce == 3) {
                    arrivata.complete(Unit)
                    awaitCancellation()
                }
            }) { conti += it }
        }
        arrivata.await()
        lavoro.cancel()
        lavoro.join()

        assertEquals(listOf(0, 1, 2, 3), partite)
        assertEquals(listOf(1, 2, 3), conti)
    }

    /**
     * **Dopo un annullamento non parte nessun file nuovo, anche se quello in corso finisce.**
     *
     * ⚠️⚠️ **IL FILE IN CORSO FINISCE SENZA SOSPENDERE**, ed è il caso che il controllo in cima al
     * giro (`ensureActive`) esiste per coprire: un lavoro che non si ferma da sé, come una
     * miniatura che il sistema sta già scrivendo, lascia la corsia libera di prendere il prossimo. I
     * file dopo, qui, non sospendono mai, quindi senza quel controllo passerebbero tutti.
     * ⚠️ **Controprovata togliendo `ensureActive`**: partono tutti e dieci.
     */
    @Test
    fun `dopo un annullamento non parte nessuna voce nuova`() = runBlocking {
        val partite = mutableListOf<Int>()
        val arrivata = CompletableDeferred<Unit>()
        val via = CompletableDeferred<Unit>()

        val lavoro = launch {
            warmAll((0 until 10).toList(), lanes = 1, work = { voce ->
                partite += voce
                if (voce == 3) {
                    withContext(NonCancellable) {
                        arrivata.complete(Unit)
                        via.await()
                    }
                }
            }) {}
        }
        arrivata.await()
        lavoro.cancel()
        via.complete(Unit)
        lavoro.join()

        assertEquals(listOf(0, 1, 2, 3), partite)
    }

    /**
     * **I due tasti chiedono prima di fare, ognuno con la sua domanda, e 'Annulla' non fa niente.**
     *
     * ⚠️⚠️ **FINO ALLA `2.96` 'SVUOTA' SVUOTAVA AL TOCCO**, e la conferma è la sua specifica della
     * `2.97` (*Al tocco, sarà richiesta conferma*): il caso misura che il tocco da solo non basti.
     * ⚠️ **Le domande si guardano per testo**, perché sono due finestre dello stesso componente: con
     * le due invertite ognuno dei tasti farebbe la domanda dell'altro, e nessuno lo vedrebbe.
     * ⚠️ **Controprovata due volte**: col tasto che svuota subito cade la prima asserzione, e con le
     * due domande scambiate cade quella della prima finestra.
     */
    @Test
    fun `le due conferme chiedono prima di fare`() {
        var svuotate = 0
        var generate = 0
        banco.setContent {
            AivTheme(darkTheme = false) {
                ThumbsCard(head = null, onClear = { svuotate++ }, onGenerate = { generate++ })
            }
        }
        banco.waitForIdle()

        banco.onNodeWithText(testo(R.string.settings_thumbs_do)).performClick()
        banco.waitForIdle()
        assertEquals("il tocco da solo non deve svuotare", 0, svuotate)
        banco.onNodeWithText(testo(R.string.settings_thumbs_ask)).assertExists()
        banco.onNodeWithText(testo(R.string.cancel)).performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.settings_thumbs_ask)).assertDoesNotExist()
        assertEquals("'Annulla' non deve svuotare", 0, svuotate)

        banco.onNodeWithText(testo(R.string.settings_thumbs_do)).performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.yes)).performClick()
        banco.waitForIdle()
        assertEquals(1, svuotate)

        banco.onNodeWithText(testo(R.string.settings_thumbs_gen)).performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.settings_thumbs_gen_ask)).assertExists()
        banco.onNodeWithText(testo(R.string.yes)).performClick()
        banco.waitForIdle()
        assertEquals(1, generate)
        assertEquals("'Genera' non deve svuotare", 1, svuotate)
    }

    /**
     * **La pagina dice a che punto si è, col numero e con la barra.**
     *
     * ⚠️ **Mentre si contano le immagini la barra gira senza misura**: il totale non c'è ancora, e
     * una barra ferma a zero si leggerebbe come una generazione bloccata.
     * ⚠️ **La barra e il numero dicono la stessa cosa**: tre su dodici è un quarto, e il caso guarda
     * tutti e due. ⚠️ **Controprovata** passando alla barra il conto sbagliato: cade l'asserzione
     * sulla sua misura.
     */
    @Test
    fun `la pagina della generazione mostra il conto e la barra`() {
        var annullate = 0
        var corsa by mutableStateOf(Warmup.Run(0, null))
        banco.setContent {
            AivTheme(darkTheme = false) {
                WarmupPage(corsa, onCancel = { annullate++ })
            }
        }
        banco.waitForIdle()

        banco.onNodeWithText(testo(R.string.settings_thumbs_gen_title)).assertExists()
        banco.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()

        corsa = Warmup.Run(3, 12)
        banco.waitForIdle()
        banco.onNodeWithText("3/12").assertExists()
        banco.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.25f, 0f..1f))).assertExists()

        banco.onNodeWithText(testo(R.string.cancel)).performClick()
        banco.waitForIdle()
        assertEquals(1, annullate)
    }

    /**
     * **Indietro sulla pagina della generazione vale 'Annulla'.**
     *
     * ⚠️ **Il gesto arriva alla finestra della pagina e non all'attività**: la pagina è un `Dialog`,
     * e a ricevere Indietro è il suo gestore. Chiuderla lasciando il lavoro in corso vorrebbe dire
     * una generazione che nessuno vede più e che non si può fermare.
     * ⚠️ **Controprovata** passando alla finestra una chiusura vuota: la prova cade.
     */
    @Test
    fun `Indietro sulla pagina della generazione vale Annulla`() {
        var annullate = 0
        banco.setContent {
            AivTheme(darkTheme = false) {
                WarmupPage(Warmup.Run(1, 4), onCancel = { annullate++ })
            }
        }
        banco.waitForIdle()

        banco.runOnUiThread {
            (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed()
        }
        banco.waitForIdle()

        assertEquals(1, annullate)
    }

    /**
     * Un PNG con la trasparenza vera, cioè col tipo di colore RGBA che la griglia non chiede al
     * sistema.
     *
     * ⚠️ **Più piccolo del riquadro di proposito**, come in `RiconoscimentoTest`: con una misura
     * dichiarata Coil ingrandisce fino a riempirlo, quindi la miniatura vera esce alta [Thumbs.PX],
     * che è un numero che il segnaposto non può dare.
     */
    private fun pngConAlfa(): ByteArray {
        val bitmap = Bitmap.createBitmap(31, 50, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        bitmap.setPixel(15, 25, Color.RED)
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            .toByteArray()
    }
}

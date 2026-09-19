package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.abs
import kotlin.math.max

/**
 * Il banco di prova della **filigrana**, nata nella `2.69`.
 *
 * ⚠️⚠️ **NASCE CON LA FUNZIONE E NON DOPO UN DIFETTO**, ed è il caso proattivo di `AIV/CLAUDE.md`
 * § '🧪 Quando si scrive una prova, e quando no': qui si **riscrive un file dell'utente**, e ogni
 * cosa che può andare storta va storta in silenzio. Un tipo riconosciuto dal nome invece che dai
 * byte stampa un rettangolo opaco sopra una fotografia; un file vecchio che non se ne va lascia in
 * scena il logo di prima; una misura presa dalla larghezza dà una firma che cambia con
 * l'orientamento. Nessuno dei tre dà un errore, e tutti e tre arrivano su un file già salvato.
 *
 * ⚠️ **Che cosa NON vede**: che la firma si veda bene su una fotografia vera, e la scelta del file
 * dal selettore di sistema, che è una schermata di Android. Quelli si guardano sul telefono, e la
 * voce di collaudo li chiede.
 *
 * ⚠️ **Vuole la grafica vera** (`@GraphicsMode(NATIVE)`), per due ragioni insieme: l'adozione
 * **disegna** il file prima di accettarlo, e i casi del posto contano i pixel.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FiligranaTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * ⚠️ **Si parte senza filigrana**: il file vive in `filesDir`, cioè fuori da quello che il
     * banco azzera fra un caso e l'altro, quindi senza questa riga l'ordine dei casi deciderebbe
     * l'esito.
     * ⚠️ E si spegne il mini-onboarding dei moduli, che consuma il primo tocco (vedi [LuceTest]).
     */
    @Before
    fun pulito() {
        Watermark.forget(app)
        runBlocking { Hint.MODULES.remember(app) }
    }

    /**
     * **Caso 1: un JPEG non diventa una filigrana, e a dirlo sono i byte.**
     *
     * ⚠️⚠️ **IL NOME MENTE, E QUI MENTE APPOSTA**: l'indirizzo si chiama `logo.png` e dentro c'è un
     * JPEG. Chi guardasse il suffisso lo accetterebbe, e un JPEG non ha trasparenza: la 'firma'
     * sarebbe un rettangolo pieno stampato sopra la fotografia. È il difetto che non dà nessun
     * errore e che si vede solo sul file salvato.
     */
    @Test
    fun `un JPEG non diventa una filigrana`() {
        val fatto = runBlocking { Watermark.adopt(app, offri("logo.png", jpeg())) }

        assertFalse("un JPEG non si accetta come filigrana", fatto)
        assertNull("e non deve restare niente in casa", Watermark.file(app))
    }

    /**
     * **Caso 2: un PNG si adotta anche se il nome dice un'altra cosa.**
     *
     * È il rovescio del caso 1: il tipo viene dai byte in tutti e due i versi, quindi un file
     * buono con un suffisso qualunque deve passare. Senza questa metà, un riconoscimento rotto che
     * rifiuta tutto sembrerebbe prudente.
     */
    @Test
    fun `un PNG si adotta e il tipo viene dai byte`() {
        val fatto = runBlocking { Watermark.adopt(app, offri("logo.txt", png(40))) }

        assertTrue("un PNG si accetta", fatto)
        val file = Watermark.file(app)
        assertNotNull("la copia deve vivere in casa", file)
        assertEquals("ed è dichiarata PNG", Watermark.Kind.PNG, Watermark.kindOf(file!!))
    }

    /**
     * **Caso 3: un logo solo per volta, ed è la sua clausola.**
     *
     * ⚠️⚠️ **È LA FORMA DELL'ARCHIVIO A GARANTIRLO, E PER QUESTO SI MISURA**: la copia si chiama
     * sempre `mark` col suffisso del proprio tipo, quindi un file dello **stesso** tipo prende il
     * posto del primo da sé; uno dell'altro tipo no, e senza la riga che lo toglie resterebbero
     * due filigrane. Da lì in poi a decidere quale si usa sarebbe l'ordine di un `enum`, cioè la
     * scelta dell'utente verrebbe ignorata in silenzio.
     */
    @Test
    fun `un logo solo per volta`() {
        assertTrue(runBlocking { Watermark.adopt(app, offri("a.png", png(40))) })
        assertTrue(runBlocking { Watermark.adopt(app, offri("b.svg", svg())) })

        val file = Watermark.file(app)
        assertNotNull("la filigrana scelta per ultima deve esserci", file)
        assertEquals("ed è quella nuova", Watermark.Kind.SVG, Watermark.kindOf(file!!))
        assertFalse(
            "quella di prima se ne va, o due filigrane si contenderebbero il posto",
            File(file.parentFile, "mark.png").exists()
        )
    }

    /**
     * **Caso 4: un file illeggibile non porta via quello scelto.**
     *
     * ⚠️ Si legge prima e si scrive poi: chi prova un file sbagliato deve ritrovare la filigrana
     * che aveva, invece di restare senza per aver toccato il selettore.
     */
    @Test
    fun `un file illeggibile non porta via quello scelto`() {
        assertTrue(runBlocking { Watermark.adopt(app, offri("a.png", png(40))) })

        val fatto = runBlocking {
            Watermark.adopt(app, offri("b.png", ByteArray(64) { 0x2A }))
        }

        assertFalse("dei byte qualunque non si accettano", fatto)
        assertEquals(
            "e la filigrana di prima resta",
            Watermark.Kind.PNG,
            Watermark.file(app)?.let(Watermark::kindOf)
        )
    }

    /**
     * **Caso 5: il tetto ferma un file grande, e lo ferma per la misura.**
     *
     * ⚠️⚠️ **I BYTE COMINCIANO CON LA FIRMA DI UN PNG, ED È QUELLO CHE RENDE LA PROVA ONESTA**:
     * senza, a rifiutarli sarebbe il riconoscimento del tipo, cioè il caso 1 scritto una seconda
     * volta, e il tetto resterebbe non misurato.
     */
    @Test
    fun `un file oltre il tetto non si adotta`() {
        val grosso = ByteArray((Watermark.MAX_BYTES + 32).toInt())
        png(8).copyInto(grosso)

        val fatto = runBlocking { Watermark.adopt(app, offri("enorme.png", grosso)) }

        assertFalse("oltre il tetto non si adotta", fatto)
        assertNull("e non resta niente in casa", Watermark.file(app))
    }

    /**
     * **Caso 6: la misura è una frazione, e la frazione è il numero scelto.**
     *
     * ⚠️ Con un numero in pixel la stessa scelta darebbe una firma enorme su un file piccolo e
     * invisibile su uno da fotocamera, e nessuno dei due casi dà errore.
     * ⚠️⚠️ **DALLA `2.71` IL NUMERO LO SCRIVE LUI, quindi la prova gira su tutta la corsa** e non
     * su quattro gettoni: i due capi e un valore in mezzo dicono che il conto è una proporzione e
     * non una tabella.
     */
    @Test
    fun `la misura e una frazione del lato`() {
        assertTrue(runBlocking { Watermark.adopt(app, offri("a.png", png(40))) })

        for (size in listOf(Watermark.SIZE.first, Watermark.SIZE_DEFAULT, Watermark.SIZE.last)) {
            val disegno = Watermark.bitmapFor(app, 1000, Watermark.Plan(SPOT, size = size))
            assertNotNull("la filigrana deve disegnarsi a misura $size", disegno)
            val lungo = max(disegno!!.width, disegno.height)
            assertEquals(
                "la misura $size deve valere i suoi centesimi del lato",
                1000 * size / 100,
                lungo
            )
            disegno.recycle()
        }
    }

    /**
     * **Caso 6b: la distanza dal bordo è quella scelta, e si misura a pixel.**
     *
     * ⚠️⚠️ **DALLA `2.71` NON È PIÙ UNA COSTANTE, ED È SUA RICHIESTA** (voce `filigrana` del giro
     * della `2.70`, punto 5). ⚠️ **Si misura dove la firma NON arriva**: con la distanza a zero
     * l'angolo è coperto, e con la distanza larga quello stesso pixel resta pulito. Un conto sul
     * solo inchiostro totale non lo vedrebbe, perché il disegno è lo stesso e cambia solo dov'è.
     */
    @Test
    fun `la distanza dal bordo sposta la firma`() {
        assertTrue(runBlocking { Watermark.adopt(app, offri("a.png", png(40))) })
        val spot = Watermark.Spot.TOP_LEFT

        val stretto = foglio()
        assertNotNull(Watermark.stamp(app, stretto, Watermark.Plan(spot, air = 0)))
        assertTrue("a filo del bordo la firma copre l'angolo", scuro(stretto, 2, 2))

        val largo = foglio()
        assertNotNull(Watermark.stamp(app, largo, Watermark.Plan(spot, air = Watermark.AIR.last)))
        assertFalse("con la distanza al massimo quell'angolo resta pulito", scuro(largo, 2, 2))
        assertTrue(
            "ma la firma c'è, più dentro",
            inchiostro(largo) > 0
        )
    }

    /**
     * **Caso 6c: l'opacità smorza la firma, e a fondo corsa non la toglie.**
     *
     * ⚠️⚠️ **È LA SUA RISPOSTA `si` A `d-mark-opacita`**, con la nota *niente metodi di fusione,
     * solo opacità assoluta*: quindi quello che si misura è **quanto** inchiostro arriva, non
     * dove. ⚠️ **Il confronto è fra due opacità e non con una soglia**: un numero scritto qui
     * dipenderebbe dal disegno di prova, mentre 'meno di prima e più di zero' è la proprietà.
     */
    @Test
    fun `l'opacita smorza la firma`() {
        assertTrue(runBlocking { Watermark.adopt(app, offri("a.png", png(40))) })

        val piena = foglio()
        assertNotNull(Watermark.stamp(app, piena, Watermark.Plan(SPOT, alpha = 100)))
        val scarsa = foglio()
        assertNotNull(
            Watermark.stamp(app, scarsa, Watermark.Plan(SPOT, alpha = Watermark.ALPHA.first))
        )

        val forte = inchiostro(piena)
        val debole = inchiostro(scarsa)
        assertTrue("a piena opacità la firma deve vedersi", forte > 0)
        assertTrue(
            "e smorzata deve lasciare meno inchiostro: $forte contro $debole",
            debole < forte
        )
    }

    /**
     * **Caso 7: la firma cade dove la scelta dice, e si misura a pixel.**
     *
     * ⚠️⚠️ **DUE ANGOLI OPPOSTI E NON UNO SOLO**: con un angolo solo, un codice che scrivesse
     * sempre in alto a sinistra passerebbe metà delle volte. Qui l'angolo scelto deve annerirsi e
     * quello opposto deve restare bianco, che è una misura che non si supera per caso.
     */
    @Test
    fun `la firma cade nell'angolo scelto`() {
        assertTrue(runBlocking { Watermark.adopt(app, offri("a.png", png(40))) })

        val alto = Watermark.Spot.TOP_LEFT
        val basso = Watermark.Spot.BOTTOM_RIGHT
        val vicino = 20
        val lontano = LATO - 20

        val primo = foglio()
        assertNotNull(
            "la firma deve scriversi",
            Watermark.stamp(app, primo, Watermark.Plan(alto))
        )
        assertTrue("in alto a sinistra la firma deve esserci", scuro(primo, vicino, vicino))
        assertFalse("e l'angolo opposto deve restare pulito", scuro(primo, lontano, lontano))

        val secondo = foglio()
        assertNotNull(
            "la firma deve scriversi",
            Watermark.stamp(app, secondo, Watermark.Plan(basso))
        )
        assertTrue("in basso a destra la firma deve esserci", scuro(secondo, lontano, lontano))
        assertFalse("e l'angolo opposto deve restare pulito", scuro(secondo, vicino, vicino))
    }

    /**
     * **Caso 8: la misura viene dal lato LUNGO, e non dalla larghezza.**
     *
     * ⚠️⚠️ **SI CONTA L'INCHIOSTRO SU DUE IMMAGINI GIRATE, ED È L'UNICO MODO DI VEDERLO**: la
     * stessa fotografia in verticale e in orizzontale deve portare la stessa firma. Con la
     * larghezza al posto del lato lungo, quella verticale ne prenderebbe una alta la metà, cioè un
     * quarto dell'inchiostro, e a schermo sembrerebbe soltanto 'un po' più piccola'.
     */
    @Test
    fun `la misura viene dal lato lungo`() {
        assertTrue(runBlocking { Watermark.adopt(app, offri("a.png", png(40))) })
        val piano = Watermark.Plan(SPOT)

        val steso = Bitmap.createBitmap(LATO, LATO / 2, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(Color.WHITE) }
        val ritto = Bitmap.createBitmap(LATO / 2, LATO, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(Color.WHITE) }
        assertNotNull(Watermark.stamp(app, steso, piano))
        assertNotNull(Watermark.stamp(app, ritto, piano))

        val uno = inchiostro(steso)
        val due = inchiostro(ritto)
        assertTrue("la firma deve esserci su tutte e due", uno > 0)
        assertTrue(
            "la stessa scelta deve pesare uguale girando l'immagine: $uno contro $due",
            abs(uno - due) <= uno / 10
        )
    }

    /**
     * **Caso 9: senza filigrana scelta non si scrive niente.**
     *
     * ⚠️ È la riga che tiene innocuo tutto il resto: chi salva chiede la firma e, se non c'è,
     * scrive l'immagine com'è invece di fallire.
     */
    @Test
    fun `senza filigrana scelta non si scrive niente`() {
        val foglio = foglio()
        assertNull(
            "senza un file scelto non c'è niente da stampare",
            Watermark.stamp(app, foglio, Watermark.Plan(SPOT))
        )
        assertFalse("e l'immagine non si tocca", scuro(foglio, LATO - 20, LATO - 20))
    }

    /**
     * **Caso 10: una filigrana pronta accende 'Salva' su un'immagine intonsa.**
     *
     * ⚠️⚠️ **SENZA QUESTA CONDIZIONE LA FUNZIONE NON SI POTREBBE USARE AFFATTO**: chi apre
     * l'editor per firmare un'immagine e basta non muove nessun cursore, quindi il tasto
     * resterebbe spento e la filigrana non arriverebbe mai su un file. ⚠️ **Le due metà si
     * misurano insieme**: senza il caso spento, una condizione scritta `true` fisso passerebbe.
     *
     * ⚠️ **La scena è UNA e il valore cambia sotto**, e non è un modo per aggirare il divieto di
     * montarne due: così si misura anche che il tasto si accenda quando la filigrana arriva, che è
     * il caso vero di chi la sceglie mentre l'editor è già aperto.
     */
    @Test
    fun `una filigrana accende Salva a immagine intonsa`() {
        val firmata = mutableStateOf(false)
        banco.setContent { Scena(marked = firmata.value) }
        pronta()
        banco.onNodeWithText(testo(R.string.editor_save)).assertIsNotEnabled()

        firmata.value = true
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.editor_save)).assertIsEnabled()
    }

    // ── Gli arnesi ──

    /** L'editor completo montato con gli argomenti minimi, come in [LuceTest]. */
    @Composable
    private fun Scena(marked: Boolean) {
        AivTheme(darkTheme = false) {
            Box(modifier = Modifier.fillMaxSize()) {
                AdvancedEditorScreen(
                    uri = quadrato(),
                    busy = false,
                    marked = marked,
                    resize = Resize.Plan(Resize.Mode.LONG, Resize.DEFAULT_PX),
                    // ⚠️ Spento: un ridimensionamento che rimpicciolisce accenderebbe 'Salva' a
                    // immagine intonsa, e il caso suo vive in `RidimensionaTest`.
                    resizing = false,
                    onResize = {},
                    onSave = { _, _ -> },
                    onBack = {}
                )
            }
        }
    }

    /** Aspetta che l'anteprima sia decodificata: prima i comandi sono spenti comunque. */
    private fun pronta() {
        banco.waitUntil(ATTESA) {
            banco.onAllNodesWithContentDescription(testo(R.string.look_compare))
                .fetchSemanticsNodes().isNotEmpty()
        }
        banco.waitForIdle()
    }

    /**
     * Un indirizzo che il resolver sa aprire, coi byte dati.
     *
     * ⚠️ **Il nome conta**, ed è la metà che rende onesti i casi 1 e 2: quello che si misura è che
     * il riconoscimento non lo guardi.
     */
    private fun offri(nome: String, bytes: ByteArray): Uri {
        val uri = Uri.parse("content://prova/$nome")
        shadowOf(app.contentResolver).registerInputStream(uri, ByteArrayInputStream(bytes))
        return uri
    }

    /** Un PNG quadrato e nero, di [lato] pixel. */
    private fun png(lato: Int): ByteArray {
        val mappa = Bitmap.createBitmap(lato, lato, Bitmap.Config.ARGB_8888)
        mappa.eraseColor(Color.BLACK)
        val out = ByteArrayOutputStream()
        mappa.compress(Bitmap.CompressFormat.PNG, 100, out)
        mappa.recycle()
        return out.toByteArray()
    }

    /** Un JPEG: stesso disegno, formato che la filigrana non accetta. */
    private fun jpeg(): ByteArray {
        val mappa = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        mappa.eraseColor(Color.BLACK)
        val out = ByteArrayOutputStream()
        mappa.compress(Bitmap.CompressFormat.JPEG, 90, out)
        mappa.recycle()
        return out.toByteArray()
    }

    /** Un documento SVG con dentro un quadrato nero. */
    private fun svg(): ByteArray = (
        "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"40\" height=\"40\" " +
            "viewBox=\"0 0 40 40\"><rect width=\"40\" height=\"40\" fill=\"#000\"/></svg>"
        ).toByteArray()

    /** Un foglio bianco su cui firmare. */
    private fun foglio(): Bitmap =
        Bitmap.createBitmap(LATO, LATO, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }

    /** Se quel punto porta inchiostro, cioè se la firma è caduta lì. */
    private fun scuro(mappa: Bitmap, x: Int, y: Int): Boolean =
        Color.red(mappa.getPixel(x, y)) < SOGLIA

    /** Quanti pixel di quel foglio porta la firma. */
    private fun inchiostro(mappa: Bitmap): Int {
        var conto = 0
        for (y in 0 until mappa.height) {
            for (x in 0 until mappa.width) {
                if (Color.red(mappa.getPixel(x, y)) < SOGLIA) conto++
            }
        }
        return conto
    }

    private fun testo(id: Int): String = app.getString(id)

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /** Un PNG vero su disco, che l'editor possa aprire. */
    private fun quadrato(): Uri {
        val file = File(app.cacheDir, "filigrana.png")
        if (!file.exists()) {
            val mappa = Bitmap.createBitmap(LATO, LATO, Bitmap.Config.ARGB_8888)
            mappa.eraseColor(Color.WHITE)
            file.outputStream().use { mappa.compress(Bitmap.CompressFormat.PNG, 100, it) }
            mappa.recycle()
        }
        return Uri.fromFile(file)
    }
}

/** Il lato dei fogli su cui questo banco firma. */
private const val LATO = 240

/** Sotto questo livello di rosso un pixel porta inchiostro, e non è il bianco del foglio. */
private const val SOGLIA = 200

/** Dove cade la firma quando il posto non è quello che si sta misurando. */
private val SPOT = Watermark.Spot.BOTTOM_RIGHT

/** Quanto si aspetta che l'anteprima arrivi. */
private const val ATTESA = 10_000L

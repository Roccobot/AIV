package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Il banco dell'**editor completo**: il modulo Luce e la pila dei passi.
 *
 * ⚠️⚠️ **NASCE CON LA FUNZIONE E NON DOPO UN DIFETTO**, ed è il caso proattivo di
 * `AIV/CLAUDE.md` § '🧪 Quando si scrive una prova, e quando no': qui c'è una superficie che
 * **misura** un gesto continuo e ne ricava un passo discreto, e un passo che porta il valore
 * sbagliato non dà nessun errore. L'utente muove un cursore, vede l'immagine cambiare, e
 * 'Annulla' resta spento: il codice è valido, compila, e la storia non si scrive.
 *
 * ⚠️⚠️ **E ALLA PRIMA CORSA HA TROVATO PROPRIO QUELLO.** La prima stesura di `LookKnob`
 * consegnava il valore con `onValueChangeFinished = { onSettled(value) }`, dove `value` è il
 * **parametro** del composable, cioè un valore catturato alla composizione. Compose chiama
 * `onValueChange` e subito dopo `onValueChangeFinished` **senza per forza una ricomposizione in
 * mezzo**, quindi il passo registrato portava il valore di prima. Con un dito vero il difetto si
 * sarebbe visto di rado, perché fra i due c'è quasi sempre un fotogramma: è la peggiore delle due
 * specie, quella che arriva a lui una volta su dieci.
 *
 * ⚠️ **Che cosa questo banco NON vede**: i pixel che escono dal conto, perché una prova gira
 * senza scheda grafica e `lookShader` risponde `null`; e il confronto col tocco lungo, che si
 * vede solo dai pixel. Quelli si guardano sul telefono, e la voce di collaudo li chiede.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LuceTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **Caso 1: sotto la soglia un cursore vale zero, e appena sopra no.**
     *
     * ⚠️ È la riga che decide se il file si riscrive: un cursore lasciato andare può fermarsi a
     * un millesimo dallo zero, e senza tolleranza quel millesimo costringerebbe a ricomprimere
     * un'immagine per una differenza che non muove nemmeno un livello su 255.
     */
    @Test
    fun `sotto la soglia il modulo e a riposo`() {
        assertTrue(Light.NONE.idle)
        assertTrue(Light(brightness = 0.0001f).idle)
        assertTrue(Light(exposure = -0.0004f).idle)
        assertFalse(Light(brightness = 0.01f).idle)
        assertFalse(Light(highlights = -0.01f).idle)
    }

    /**
     * **Caso 2: il guadagno è due elevato agli stop.**
     *
     * ⚠️ L'unità del cursore è quella della fotografia, cioè +1 vuol dire il **doppio** della
     * luce. Chi passasse il valore del cursore com'è allo shader otterrebbe un'esposizione che si
     * muove appena, e non lo direbbe nessun errore.
     */
    @Test
    fun `il guadagno e due elevato agli stop`() {
        assertEquals(1f, Light.NONE.gain, 1e-6f)
        assertEquals(2f, Light(exposure = 1f).gain, 1e-6f)
        assertEquals(0.5f, Light(exposure = -1f).gain, 1e-6f)
        assertEquals(4f, Light(exposure = Light.EXPOSURE_RANGE).gain, 1e-6f)
    }

    /**
     * **Caso 3: appena un valore di Luce entra, il salvataggio non è più senza perdita.**
     *
     * ⚠️⚠️ **È LA CLAUSOLA DELL'UTENTE** (*quelle che non prevedono la riscrittura del file pixel
     * per pixel devono essere lossless*), e vive nel modello invece che in una riga del
     * salvataggio: la stessa domanda la fanno il salvataggio e chi decide che cosa scrivere nel
     * cestino, e due risposte scritte in due posti divergono al primo modulo nuovo.
     */
    @Test
    fun `un valore di luce toglie il senza perdita`() {
        assertTrue(Look.NONE.lossless)
        assertTrue(Look.NONE.idle)
        val mosso = Look(Light(contrast = 0.2f))
        assertFalse(mosso.lossless)
        assertFalse(mosso.idle)
    }

    /**
     * **Caso 4: un gesto compiuto scrive un passo, e il passo porta il valore che si vede.**
     *
     * ⚠️⚠️ **È IL CASO CHE HA PRESO IL DIFETTO**, ed è scritto in due metà perché le due spie
     * dicono cose diverse: il **numero** accanto al cursore dice che l'anteprima si è mossa,
     * 'Annulla' **acceso** dice che la storia se n'è accorta. Col difetto la prima era verde e la
     * seconda rossa, cioè l'immagine cambiava e non si poteva più tornare indietro.
     */
    @Test
    fun `un gesto compiuto scrive un passo nella storia`() {
        banco.setContent { Scena() }
        banco.waitForIdle()

        banco.onNodeWithContentDescription(testo(R.string.editor_undo)).assertIsNotEnabled()

        muovi(LUMINOSITA, 0.5f)

        assertEquals(
            "Il cursore non ha mosso l'anteprima",
            1,
            quanti("+50")
        )
        banco.onNodeWithContentDescription(testo(R.string.editor_undo)).assertIsEnabled()
    }

    /**
     * **Caso 5: Annulla e Ripristina camminano nella storia, e quello che si salva è quello.**
     *
     * ⚠️⚠️ **IL RITORNO È LA MISURA CHE VALE**, e non l'andata: disfacendo e rifacendo si legge
     * quello che la storia ha davvero scritto, invece di quello che l'anteprima mostrava. ⚠️ **E
     * la terza metà chiude il giro**: 'Salva' consegna il valore vivo, quindi misurarlo qui dice
     * che l'immagine che finisce sul disco è quella che si è guardata.
     */
    @Test
    fun `Annulla e Ripristina camminano nella storia`() {
        var salvato: Look? = null
        banco.setContent { Scena(onSave = { salvato = it }) }
        banco.waitForIdle()

        muovi(LUMINOSITA, 0.5f)

        banco.onNodeWithContentDescription(testo(R.string.editor_undo)).performClick()
        banco.waitForIdle()
        assertEquals("'Annulla' non ha riportato il cursore a zero", 0, quanti("+50"))

        banco.onNodeWithContentDescription(testo(R.string.editor_redo)).performClick()
        banco.waitForIdle()
        assertEquals(
            "'Ripristina' ha riportato un passo che non porta il valore mosso",
            1,
            quanti("+50")
        )

        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        assertNotNull("'Salva' non ha consegnato niente", salvato)
        assertEquals(0.5f, salvato?.light?.brightness ?: 0f, 1e-4f)
    }

    /**
     * **Caso 6: un passo nuovo taglia quello che veniva dopo.**
     *
     * ⚠️ È la ragione per cui la storia è una lista con un indice e non due pile: con due pile
     * quel taglio è una riga che ci si deve ricordare di scrivere, e chi se ne dimentica lascia un
     * 'Ripristina' che riporta a una strada abbandonata. Qui si misura che quel 'Ripristina' si
     * **spenga**.
     */
    @Test
    fun `un passo nuovo taglia quello che veniva dopo`() {
        banco.setContent { Scena() }
        banco.waitForIdle()

        muovi(LUMINOSITA, 0.5f)
        banco.onNodeWithContentDescription(testo(R.string.editor_undo)).performClick()
        banco.waitForIdle()
        banco.onNodeWithContentDescription(testo(R.string.editor_redo)).assertIsEnabled()

        muovi(CONTRASTO, 0.25f)

        banco.onNodeWithContentDescription(testo(R.string.editor_redo)).assertIsNotEnabled()
        assertEquals("Il passo nuovo non è quello che si vede", 1, quanti("+25"))
    }

    /**
     * **Caso 7: il numero azzera il suo cursore, e anche quello è un passo.**
     *
     * ⚠️ Il numero è il tasto che azzera, e non c'è un secondo comando. ⚠️ **Che sia un passo non
     * è un dettaglio**: un azzeramento che non entrasse nella storia sarebbe l'unico gesto
     * dell'editor che non si può disfare, e per giunta quello che butta via il lavoro.
     */
    @Test
    fun `il numero azzera il suo cursore e scrive un passo`() {
        banco.setContent { Scena() }
        banco.waitForIdle()

        muovi(LUMINOSITA, 0.5f)

        val azzera = testo(R.string.look_reset_one, testo(R.string.look_brightness))
        banco.onNodeWithContentDescription(azzera).performClick()
        banco.waitForIdle()
        assertEquals("Il numero non ha azzerato il cursore", 0, quanti("+50"))

        banco.onNodeWithContentDescription(testo(R.string.editor_undo)).performClick()
        banco.waitForIdle()
        assertEquals(
            "L'azzeramento non era un passo: 'Annulla' non lo ha disfatto",
            1,
            quanti("+50")
        )
    }

    /**
     * **Caso 8: 'Originale' riporta a zero e resta un passo come gli altri.**
     *
     * ⚠️ È il comando che butta via tutto in un colpo, quindi è quello che più di tutti deve
     * poter essere disfatto: chi lo tocca per sbaglio con cinque cursori mossi perde cinque gesti.
     */
    @Test
    fun `Originale riporta a zero e si disfa`() {
        banco.setContent { Scena() }
        banco.waitForIdle()

        muovi(LUMINOSITA, 0.5f)
        muovi(CONTRASTO, 0.25f)

        banco.onNodeWithContentDescription(testo(R.string.editor_original)).performClick()
        banco.waitForIdle()
        assertEquals(
            "'Originale' non ha riportato i cursori a zero",
            0,
            quanti("+50") + quanti("+25")
        )
        banco.onNodeWithContentDescription(testo(R.string.editor_original)).assertIsNotEnabled()

        banco.onNodeWithContentDescription(testo(R.string.editor_undo)).performClick()
        banco.waitForIdle()
        assertEquals(
            "'Annulla' non ha disfatto 'Originale'",
            2,
            quanti("+50") + quanti("+25")
        )
    }

    /**
     * **Caso 9: a riposo non c'è niente da salvare.**
     *
     * ⚠️ Salvare a cursori fermi vorrebbe dire riscrivere il file per ottenere la stessa
     * immagine, cioè spendere qualità per niente. È la [Look.idle] letta dalla schermata, e qui si
     * misura che quella risposta arrivi davvero al tasto.
     */
    @Test
    fun `a riposo il salvataggio e spento`() {
        banco.setContent { Scena() }
        banco.waitForIdle()

        banco.onNodeWithText(testo(R.string.editor_save)).assertIsNotEnabled()
        muovi(LUMINOSITA, 0.5f)
        banco.onNodeWithText(testo(R.string.editor_save)).assertIsEnabled()
    }

    /**
     * Muove un cursore fino in fondo al suo gesto, cioè come un dito che si alza.
     *
     * ⚠️⚠️ **`SetProgress` NON RICOMPONE FRA LE DUE CHIAMATE, ed è per questo che misura**:
     * l'azione semantica di uno `Slider` invoca `onValueChange` e subito dopo
     * `onValueChangeFinished`, nella stessa passata. Un dito vero ci mette quasi sempre un
     * fotogramma in mezzo, quindi il banco qui è **più severo** della mano, che è esattamente
     * quello che serve per prendere un difetto che di là si vedrebbe una volta su dieci.
     *
     * @param quale la riga del cursore, nell'ordine in cui la scheda li disegna.
     */
    private fun muovi(quale: Int, a: Float) {
        banco.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))[quale]
            .performSemanticsAction(SemanticsActions.SetProgress) { it(a) }
        banco.waitForIdle()
    }

    /** Quanti nodi portano scritto [detto]: è la spia di che cosa dicono i numeri dei cursori. */
    private fun quanti(detto: String): Int =
        banco.onAllNodesWithText(detto).fetchSemanticsNodes().size

    private fun testo(id: Int, vararg args: Any): String = app.getString(id, *args)

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * L'editor completo montato con gli argomenti minimi, e con un'immagine vera.
     *
     * ⚠️⚠️ **L'IMMAGINE SERVE DAVVERO, E NON È UN LUSSO**: finché l'anteprima non è decodificata
     * i comandi sono spenti, quindi una scena con un indirizzo finto misurerebbe una scheda in cui
     * non si può toccare niente. Un quadrato scritto nella cache basta: quello che si guarda qui è
     * la storia dei passi, e non i pixel.
     */
    @Composable
    private fun Scena(onSave: (Look) -> Unit = {}) {
        AivTheme(darkTheme = false) {
            Box(modifier = Modifier.fillMaxSize()) {
                AdvancedEditorScreen(
                    uri = quadrato(),
                    busy = false,
                    onSave = onSave,
                    onBack = {}
                )
            }
        }
    }

    /** Un PNG vero su disco, e il suo indirizzo. */
    private fun quadrato(): Uri {
        val file = File(app.cacheDir, "luce.png")
        if (!file.exists()) {
            val mappa = Bitmap.createBitmap(LATO, LATO, Bitmap.Config.ARGB_8888)
            mappa.eraseColor(Color.WHITE)
            file.outputStream().use { mappa.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return Uri.fromFile(file)
    }
}

/**
 * Quale riga occupano i due cursori che queste prove muovono.
 *
 * ⚠️ **Si contano nell'ordine in cui la scheda li disegna**, che è quello del banco di sviluppo
 * dichiarato in `Adjust.kt`: esposizione, luminosità, contrasto, ombre, luci. ⚠️ **Non è
 * l'esposizione** quella che si muove, e non è un caso: il suo numero si scrive con due decimali e
 * il separatore decimale dipende dalla lingua della macchina, quindi una prova che lo leggesse
 * sarebbe rossa o verde a seconda di dove gira.
 */
private const val LUMINOSITA = 1
private const val CONTRASTO = 2

/** Il lato del quadrato finto: piccolo, perché di lui serve solo che esista. */
private const val LATO = 64

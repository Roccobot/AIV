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
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Il banco di prova del **ridimensionamento al salvataggio**, nato nella `2.70`.
 *
 * ⚠️⚠️ **NASCE CON LA FUNZIONE E NON DOPO UN DIFETTO**, ed è il caso proattivo di `AIV/CLAUDE.md`
 * § '🧪 Quando si scrive una prova, e quando no': qui si decide **quanti pixel** finiscono in un
 * file dell'utente, e ognuna delle cose che possono andare storte va storta in silenzio. Un piano
 * che ingrandisce dà un file più pesante e non più nitido; un conto che guarda il lato sbagliato
 * rende una fotografia verticale un quarto di quella orizzontale; una misura presa dal file invece
 * che dall'immagine finita dice un numero che il salvataggio poi smentisce. Nessuno dei tre dà un
 * errore.
 *
 * ⚠️ **Che cosa NON vede**: che l'immagine ridimensionata sia nitida, cioè la resa del filtro, e la
 * finestra come si legge sul telefono. Quelle si guardano sul telefono, e la voce di collaudo le
 * chiede.
 *
 * ⚠️ **Vuole la grafica vera** (`@GraphicsMode(NATIVE)`), perché il caso 8 ridimensiona davvero un
 * bitmap e i due casi della schermata aprono l'editor completo.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RidimensionaTest {

    @get:Rule
    val banco = createComposeRule()

    /** ⚠️ Il mini-onboarding dei moduli consuma il primo tocco: vedi [LuceTest]. */
    @Before
    fun pulito() {
        runBlocking {
            Hint.MODULES.remember(app)
            Hint.EDITOR_TOOLS.remember(app)
        }
    }

    /**
     * **Caso 1: non si ingrandisce mai, in nessuno dei quattro modi.**
     *
     * ⚠️⚠️ **È LA REGOLA CHE COSTA DI PIÙ A DIMENTICARE**: i pixel che mancano non li inventa
     * nessuno, quindi chiedere un lato più lungo di quello che il file ha darebbe un'immagine più
     * pesante e non più nitida, cioè il contrario di quello che questa funzione serve a fare. Il
     * tetto vive in una riga sola di [Resize.Plan.sizeFor], e scritta per modo sarebbero quattro
     * occasioni di sbagliarne una.
     */
    @Test
    fun `nessun modo ingrandisce`() {
        val w = 2000
        val h = 1000
        assertNull(
            "Il lato lungo ha ingrandito",
            Resize.Plan(Resize.Mode.LONG, 4000).sizeFor(w, h)
        )
        assertNull(
            "La larghezza ha ingrandito",
            Resize.Plan(Resize.Mode.WIDE, 4000).sizeFor(w, h)
        )
        assertNull(
            "L'altezza ha ingrandito",
            Resize.Plan(Resize.Mode.TALL, 4000).sizeFor(w, h)
        )
        assertNull(
            "La misura identica non è un lavoro da fare",
            Resize.Plan(Resize.Mode.LONG, w).sizeFor(w, h)
        )
    }

    /**
     * **Caso 2: ogni modo governa il proprio lato, e le proporzioni restano.**
     *
     * ⚠️ **Le proporzioni non sono un'opzione**: un ridimensionamento che le rompe deforma
     * l'immagine, e chi vuole cambiare il rapporto ha il modulo Ritaglio, che toglie dei pixel
     * invece di stirarli.
     */
    @Test
    fun `i quattro modi governano il lato giusto`() {
        val w = 4000
        val h = 3000
        assertEquals(
            "Il lato lungo non ha portato la larghezza al valore chiesto",
            1600 to 1200,
            Resize.Plan(Resize.Mode.LONG, 1600).sizeFor(w, h)
        )
        assertEquals(
            "Su un'immagine verticale il lato lungo deve governare l'altezza",
            1200 to 1600,
            Resize.Plan(Resize.Mode.LONG, 1600).sizeFor(h, w)
        )
        assertEquals(
            "La larghezza non ha governato la larghezza",
            1000 to 750,
            Resize.Plan(Resize.Mode.WIDE, 1000).sizeFor(w, h)
        )
        assertEquals(
            "L'altezza non ha governato l'altezza",
            1000 to 750,
            Resize.Plan(Resize.Mode.TALL, 750).sizeFor(w, h)
        )
        assertEquals(
            "La percentuale non ha dimezzato",
            2000 to 1500,
            Resize.Plan(Resize.Mode.SHARE, 50).sizeFor(w, h)
        )
    }

    /**
     * **Caso 3: i confini dipendono dal modo, e una percentuale non si scrive in pixel.**
     */
    @Test
    fun `i confini dipendono dal modo`() {
        assertEquals(
            "I confini dei pixel non sono quelli dichiarati",
            Resize.MIN_PX..Resize.MAX_PX,
            Resize.range(Resize.Mode.LONG)
        )
        assertEquals(
            "I confini della percentuale non sono quelli dichiarati",
            Resize.MIN_SHARE..Resize.MAX_SHARE,
            Resize.range(Resize.Mode.SHARE)
        )
        assertTrue(
            "Il valore di fabbrica dei pixel cade fuori dai suoi confini",
            Resize.DEFAULT_PX in Resize.range(Resize.Mode.LONG)
        )
        assertTrue(
            "Il valore di fabbrica della percentuale cade fuori dai suoi confini",
            Resize.DEFAULT_SHARE in Resize.range(Resize.Mode.SHARE)
        )
    }

    /**
     * **Caso 4: la misura di partenza prende il lato lungo dal file e la forma dall'anteprima.**
     *
     * ⚠️⚠️ **È LA DIVISIONE CHE L'EXIF IMPONE**, ed è la cosa che questo caso presidia: le misure
     * dell'intestazione di una fotografia scattata in verticale sono quelle orizzontali, perché la
     * rotazione vive in un tag. Qui l'anteprima è **verticale** e il file dichiara 4000 di lato
     * lungo: se qualcuno prendesse la coppia dall'intestazione, il risultato sarebbe girato.
     */
    @Test
    fun `la misura di partenza nasce da due fonti`() {
        assertEquals(
            "Il lato lungo non è stato riportato sull'anteprima",
            3000 to 4000,
            Resize.frameSize(4000, 300, 400, 0, ImageEdit.Crop.WHOLE)
        )
        assertNull(
            "Senza il lato lungo non si può dire niente",
            Resize.frameSize(0, 300, 400, 0, ImageEdit.Crop.WHOLE)
        )
    }

    /**
     * **Caso 5: la posa scambia i lati e il ritaglio li riduce.**
     *
     * ⚠️⚠️ **SENZA QUESTO CONTO LA FINESTRA MENTIREBBE**: [Resize] si applica **dopo** il taglio,
     * come si vede in `ImageEdit.redraw`, quindi la misura da cui parte non è quella del file ma
     * quella dell'immagine finita. Chi legge 'da 4000 x 3000' su un'immagine tagliata a metà si
     * ritrova un file grande la metà di quello che gli è stato detto.
     */
    @Test
    fun `la posa e il ritaglio entrano nella misura`() {
        assertEquals(
            "Un quarto di giro non ha scambiato i lati",
            4000 to 3000,
            Resize.frameSize(4000, 300, 400, 1, ImageEdit.Crop.WHOLE)
        )
        assertEquals(
            "Mezzo ritaglio non ha dimezzato la misura",
            1500 to 2000,
            Resize.frameSize(4000, 300, 400, 0, ImageEdit.Crop(0f, 0f, 0.5f, 0.5f))
        )
    }

    /**
     * **Caso 6: il valore salvato si rilegge dentro i confini del proprio modo.**
     *
     * ⚠️⚠️ **È IL CASO CHE NESSUNO GUARDA**: chi sceglie 1600 pixel e poi passa alla percentuale
     * lascia nell'archivio due chiavi che, lette insieme alla lettera, darebbero un 1600 per
     * cento. Il valore si riporta nei confini in lettura, cioè dove l'archivio si legge, e non
     * nella finestra: uno stesso archivio deve dire la stessa cosa a chiunque lo apra.
     * ⚠️ **Le chiavi si scrivono col loro nome d'archivio**, perché in [SettingsStore] sono
     * private: è il modo di `ProfonditaTest`, e quel nome è quello vero.
     */
    @Test
    fun `il valore si riporta nei confini del suo modo`() {
        val modo = stringPreferencesKey("size-mode")
        val valore = intPreferencesKey("size-value")
        assertEquals(
            "Un valore in pixel letto come percentuale non è stato riportato nei confini",
            Resize.MAX_SHARE,
            SettingsStore.read(
                mutablePreferencesOf(modo to Resize.Mode.SHARE.token, valore to 1600)
            ).sizeValue
        )
        assertEquals(
            "Un valore buono non doveva essere toccato",
            1600,
            SettingsStore.read(
                mutablePreferencesOf(modo to Resize.Mode.LONG.token, valore to 1600)
            ).sizeValue
        )
        assertEquals(
            "Un archivio vuoto non dà il valore di fabbrica dichiarato",
            Resize.DEFAULT_PX,
            Settings().sizeValue
        )
    }

    /**
     * **Caso 7: l'immagine ridimensionata ha la misura chiesta, e a vuoto non se ne fa una nuova.**
     *
     * ⚠️ **La seconda metà è quella che conta**: un piano che non rimpicciolisce deve rispondere
     * `null`, o il salvataggio riscriverebbe il file per ottenere quello che aveva già, cioè
     * spenderebbe qualità per niente.
     */
    @Test
    fun `il ridimensionamento fa la misura chiesta`() {
        val mappa = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        mappa.eraseColor(Color.RED)
        val piccola = Resize.apply(mappa, Resize.Plan(Resize.Mode.LONG, 100))
        assertNotNull("Il ridimensionamento non ha prodotto niente", piccola)
        assertEquals("La larghezza non è quella chiesta", 100, piccola?.width)
        assertEquals("L'altezza non ha seguito le proporzioni", 50, piccola?.height)
        assertNull(
            "Un piano che non rimpicciolisce ha prodotto un bitmap nuovo",
            Resize.apply(mappa, Resize.Plan(Resize.Mode.LONG, 4000))
        )
        piccola?.recycle()
        mappa.recycle()
    }

    /**
     * **Caso 8: un ridimensionamento che rimpicciolisce accende 'Salva' su un'immagine intonsa.**
     *
     * ⚠️⚠️ **È LA RIGA CHE RENDE LA FUNZIONE USABILE**: chi apre l'editor per rimpicciolire
     * un'immagine e basta non muove nessun cursore, quindi senza quella condizione il tasto
     * resterebbe spento e il ridimensionamento non arriverebbe mai su un file. È la stessa forma
     * della filigrana, e per la stessa ragione.
     * ⚠️ **La scena è una sola e il valore cambia dentro**, o si misurerebbero due composizioni
     * diverse: è la lezione della prima stesura di `FiligranaTest`.
     */
    @Test
    fun `un ridimensionamento che rimpicciolisce accende Salva`() {
        val acceso = mutableStateOf(false)
        banco.setContent { Scena(resizing = acceso.value) }
        pronta()
        banco.onNodeWithText(testo(R.string.editor_save)).assertIsNotEnabled()
        acceso.value = true
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.editor_save)).assertIsEnabled()
    }

    /**
     * **Caso 9: un piano che su questa immagine non toglie un pixel lascia 'Salva' spento.**
     *
     * ⚠️⚠️ **QUESTA È LA METÀ CHE SI DIMENTICA**: 'acceso' e 'rimpicciolisce' non sono la stessa
     * cosa, e un tasto acceso su un piano che non fa niente prometterebbe una riscrittura che
     * spende qualità per ottenere il file di partenza. Il foglio di prova è di [LATO] pixel, e il
     * piano ne chiede molti di più.
     */
    @Test
    fun `un ridimensionamento che non rimpicciolisce lascia Salva spento`() {
        banco.setContent {
            Scena(resizing = true, piano = Resize.Plan(Resize.Mode.LONG, LATO * 10))
        }
        pronta()
        banco.onNodeWithText(testo(R.string.editor_save)).assertIsNotEnabled()
    }

    /**
     * **Caso 10: il tocco accende e spegne, il tocco lungo apre la finestra.**
     *
     * ⚠️⚠️ **I DUE GESTI SONO ROVESCIATI RISPETTO ALLA `2.70`, ED È LA SUA ISTRUZIONE** (riscontro
     * di quel giro, voce `resize` accettabile: *tocco normale = on/off. Tocco prolungato = imposti
     * il ridimensionamento*). Prima il tocco apriva la finestra e il gesto lungo spegneva.
     * ⚠️⚠️ **È LA COSA CHE PUÒ ROMPERSI IN SILENZIO**: due lambda scambiate compilano, e il tasto
     * continua a rispondere a tutti e due i gesti facendo l'una la cosa dell'altra. Il conto per
     * cui i due gesti vivono su un bersaglio solo vive su `ResizeButton`.
     * ⚠️ **I due versi dell'interruttore si misurano tutti e due**: con una sola scena si potrebbe
     * scrivere `onResize(resize)` fisso, che accende sempre e non spegne mai.
     */
    @Test
    fun `il tocco accende e spegne, il tocco lungo apre la finestra`() {
        var scritto: Resize.Plan? = null
        var chiamate = 0
        val acceso = mutableStateOf(false)
        banco.setContent {
            Scena(
                resizing = acceso.value,
                onResize = { scritto = it; chiamate++ }
            )
        }
        pronta()

        // Spento: il tocco lo accende col piano che c'è.
        banco.onNodeWithContentDescription(testo(R.string.look_resize)).performClick()
        banco.waitForIdle()
        assertEquals("Il tocco non ha acceso il ridimensionamento", 1, chiamate)
        assertNotNull("Il tocco ha spento invece di accendere", scritto)
        assertTrue(
            "La finestra si è aperta col tocco normale",
            banco.onAllNodesWithText(testo(R.string.look_resize_mode))
                .fetchSemanticsNodes().isEmpty()
        )

        // Acceso: lo stesso tocco lo spegne.
        acceso.value = true
        banco.waitForIdle()
        banco.onNodeWithContentDescription(testo(R.string.look_resize)).performClick()
        banco.waitForIdle()
        assertEquals("Il secondo tocco non ha scritto niente", 2, chiamate)
        assertNull("Il tocco non ha spento il ridimensionamento", scritto)

        // Il gesto lungo apre la finestra, in tutti e due gli stati.
        banco.onNodeWithContentDescription(testo(R.string.look_resize))
            .performTouchInput { longClick() }
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.look_resize_mode)).assertExists()
        assertEquals("Il gesto lungo ha toccato l'interruttore", 2, chiamate)
    }

    /**
     * **Caso 11: 'Ripristina' riporta a 'Lato lungo' col lato lungo dell'immagine.**
     *
     * ⚠️ **È sua richiesta** (2026-09-19, con una schermata), e quello che si misura è il numero:
     * il comando scrive nel campo la misura che l'immagine ha **adesso**, cioè quella che il
     * salvataggio troverebbe davanti, e quel piano non rimpicciolisce.
     */
    @Test
    fun `Ripristina riporta al lato lungo dell'immagine`() {
        banco.setContent {
            Scena(resizing = true, piano = Resize.Plan(Resize.Mode.SHARE, 25))
        }
        pronta()
        banco.onNodeWithContentDescription(testo(R.string.look_resize))
            .performTouchInput { longClick() }
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.look_resize_reset)).performClick()
        banco.waitForIdle()
        // Il campo porta il lato lungo del foglio, che è il piano che non fa niente.
        banco.onNodeWithText(LATO.toString()).assertExists()
        banco.onNodeWithText(testo(R.string.look_resize_keep)).assertExists()
    }

    /** L'editor completo su un foglio bianco, coi soli argomenti che questo banco muove. */
    @Composable
    private fun Scena(
        resizing: Boolean,
        piano: Resize.Plan = Resize.Plan(Resize.Mode.LONG, LATO / 2),
        onResize: (Resize.Plan?) -> Unit = {}
    ) {
        AivTheme(darkTheme = false) {
            Box(modifier = Modifier.fillMaxSize()) {
                AdvancedEditorScreen(
                    uri = foglio(),
                    busy = false,
                    // ⚠️ Nessuna filigrana: quella accenderebbe 'Salva' da sola, e il caso suo
                    // vive in `FiligranaTest`, dove vive anche il suo tasto in testata.
                    marked = false,
                    marking = false,
                    hasMark = false,
                    onMark = {},
                    onMarkSetup = {},
                    // ⚠️ E nessuna anteprima sul palco, per la stessa ragione.
                    stageMark = null,
                    resize = piano,
                    resizing = resizing,
                    onResize = onResize,
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
     * Un foglio bianco su disco.
     *
     * ⚠️ **Un file vero e non uno stream registrato**: qui l'indirizzo si apre **due** volte, una
     * per l'anteprima e una per il lato lungo, e uno stream registrato sullo shadow del resolver
     * si consuma alla prima.
     */
    private fun foglio(): Uri {
        val file = File(app.cacheDir, "ridimensiona.png")
        if (!file.exists()) {
            val mappa = Bitmap.createBitmap(LATO, LATO, Bitmap.Config.ARGB_8888)
            mappa.eraseColor(Color.WHITE)
            file.outputStream().use { mappa.compress(Bitmap.CompressFormat.PNG, 100, it) }
            mappa.recycle()
        }
        return Uri.fromFile(file)
    }

    private fun testo(id: Int): String = app.getString(id)

    private val app: Context get() = ApplicationProvider.getApplicationContext()
}

/** Il lato del foglio su cui questo banco lavora. */
private const val LATO = 240

/** Quanto si aspetta che l'anteprima arrivi, come negli altri banchi dell'editor. */
private const val ATTESA = 10_000L

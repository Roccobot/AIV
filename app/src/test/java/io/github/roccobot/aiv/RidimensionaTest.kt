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
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
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
import org.robolectric.annotation.Config
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
            Resize.NONE.value,
            Settings().sizeValue
        )
        /*
         * ⚠️⚠️ **E IL PIANO DI FABBRICA NON DEVE FARE NIENTE, DALLA `2.77`**: è quello che rende
         * vera la sua riga *nessun chip deve essere selezionato ... con le misure correnti
         * precompilate*, perché la finestra apre nel libero e scrive nei campi le misure che
         * l'immagine ha già. Con un piano che rimpicciolisce, al primo giro si troverebbe un
         * gettone acceso e un numero che nessuno ha scritto.
         */
        assertEquals(
            "Il modo di fabbrica non è quello del piano che non fa niente",
            Resize.NONE.mode,
            Settings().sizeMode
        )
        assertNull(
            "Il piano di fabbrica rimpicciolisce un'immagine da fotocamera",
            Resize.Plan(Settings().sizeMode, Settings().sizeValue).sizeFor(6000, 4000)
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
            banco.onAllNodesWithText(testo(R.string.look_resize_note))
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
        banco.onNodeWithText(testo(R.string.look_resize_note)).assertExists()
        assertEquals("Il gesto lungo ha toccato l'interruttore", 2, chiamate)
    }

    /**
     * **Caso 11: 'Ripristina' riporta al libero con le misure che l'immagine ha adesso.**
     *
     * ⚠️ **È sua richiesta** (voce `resize-ripristina`: *'Ripristina' deve riportare i valori ...
     * dell'immagine reale al suo stato corrente*), e quello che si misura sono le due cose che
     * quel comando fa: i campi tornano alle misure correnti, cioè al piano che non fa niente, e
     * il gettone se ne va, perché due campi liberi sono il libero.
     * ⚠️ **Fino alla `2.76` riportava a 'Lato lungo'**, che con la forma nuova sarebbe una regola
     * che nessuno ha chiesto.
     */
    @Test
    fun `Ripristina riporta alle misure correnti, senza gettone`() {
        banco.setContent {
            Scena(resizing = true, piano = Resize.Plan(Resize.Mode.SHARE, 25))
        }
        pronta()
        banco.onNodeWithContentDescription(testo(R.string.look_resize))
            .performTouchInput { longClick() }
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.look_resize_reset))
            .performScrollTo()
            .performClick()
        banco.waitForIdle()
        // I due campi portano le misure del foglio, che è il piano che non fa niente.
        assertEquals(
            "I due campi non sono tornati liberi",
            2,
            banco.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size
        )
        banco.onAllNodesWithText(LATO.toString())[0].assertExists()
        banco.onNodeWithText(testo(R.string.look_resize_keep)).assertExists()
        chip(R.string.resize_share).assertIsNotSelected()
        chip(R.string.look_resize_px).assertIsNotSelected()
    }

    /**
     * **Caso 12: quale campo comanda, in ognuno dei sei modi.**
     *
     * ⚠️⚠️ **È LA COSA CHE PUÒ ROMPERSI IN SILENZIO**: 'Lato lungo' accende il campo della
     * larghezza su una fotografia orizzontale e quello dell'altezza su una verticale, e scambiati
     * i due versi la finestra lascerebbe scrivere il lato sbagliato senza dare nessun errore. La
     * sua riga lo dice alla lettera (*se il lato lungo è la larghezza, quel campo è compilabile*).
     * ⚠️ **Il libero e la percentuale rispondono `null` per due ragioni diverse**: là si scrivono
     * tutti e due i campi, qui nessuno dei due, e a distinguerli è la finestra.
     */
    @Test
    fun `il modo dice quale campo comanda`() {
        assertEquals(Resize.Side.WIDE, Resize.edge(Resize.Mode.LONG, 2000, 1000))
        assertEquals(Resize.Side.TALL, Resize.edge(Resize.Mode.SHORT, 2000, 1000))
        assertEquals(
            "Su una verticale il lato lungo non è l'altezza",
            Resize.Side.TALL,
            Resize.edge(Resize.Mode.LONG, 1000, 2000)
        )
        assertEquals(
            "Su una verticale il lato corto non è la larghezza",
            Resize.Side.WIDE,
            Resize.edge(Resize.Mode.SHORT, 1000, 2000)
        )
        assertEquals(Resize.Side.WIDE, Resize.edge(Resize.Mode.WIDE, 1000, 2000))
        assertEquals(Resize.Side.TALL, Resize.edge(Resize.Mode.TALL, 2000, 1000))
        assertNull("Il libero governa un lato solo", Resize.edge(Resize.Mode.FREE, 2000, 1000))
        assertNull("La percentuale governa un lato", Resize.edge(Resize.Mode.SHARE, 2000, 1000))
    }

    /**
     * **Caso 13: il valore del piano si ricava dai due campi, e l'altro campo dalle proporzioni.**
     *
     * ⚠️⚠️ **SONO I DUE CONTI SU CUI LA FINESTRA NUOVA SI REGGE**: [Resize.valueOf] è quello che
     * permette di cambiare gettone senza muovere una cifra, e [Resize.mate] è quello che tiene i
     * due numeri in proporzione mentre si scrive. Sbagliati, la finestra mostra un
     * ridimensionamento e il salvataggio ne scrive un altro.
     * ⚠️ **Il lato lungo si legge dai campi e non dall'immagine**, perché le proporzioni sono
     * tenute: il più grande dei due è il lato lungo per costruzione.
     */
    @Test
    fun `il valore e il compagno si ricavano`() {
        assertEquals(800, Resize.valueOf(Resize.Mode.LONG, 800, 600, 50))
        assertEquals(600, Resize.valueOf(Resize.Mode.SHORT, 800, 600, 50))
        assertEquals(800, Resize.valueOf(Resize.Mode.WIDE, 800, 600, 50))
        assertEquals(800, Resize.valueOf(Resize.Mode.FREE, 800, 600, 50))
        assertEquals(600, Resize.valueOf(Resize.Mode.TALL, 800, 600, 50))
        assertEquals(50, Resize.valueOf(Resize.Mode.SHARE, 800, 600, 50))
        assertEquals(
            "Scrivendo la larghezza l'altezza non ha seguito le proporzioni",
            500,
            Resize.mate(Resize.Side.WIDE, 1000, 2000, 1000)
        )
        assertEquals(
            "Scrivendo l'altezza la larghezza non ha seguito le proporzioni",
            1000,
            Resize.mate(Resize.Side.TALL, 500, 2000, 1000)
        )
        assertEquals(
            "Su un'immagine molto allungata il compagno è sceso sotto il pixel",
            1,
            Resize.mate(Resize.Side.WIDE, Resize.MIN_PX, 4000, 100)
        )
        assertEquals("La percentuale non si ricava dalla larghezza", 50, Resize.shareOf(1000, 2000))
    }

    /**
     * **Caso 14: i campi scrivibili sono due nel libero e uno con un gettone, e 'Pixel' non si
     * accende mai.**
     *
     * ⚠️⚠️ **IL GETTONE DEL LIBERO CHE NON SI ACCENDE È UNA LETTURA DICHIARATA** (*nessun chip
     * deve essere selezionato*), quindi è la cosa da presidiare: acceso, direbbe che una regola
     * governa i due numeri mentre là non ce n'è nessuna. Il perché per esteso vive in testa a
     * [ResizeDialog].
     * ⚠️ **Il conto dei campi è la misura di quale è disattivato**: un campo spento perde
     * l'azione di scrittura, quindi contarli dice, senza guardare i pixel, che il gettone ha
     * chiuso l'altro.
     */
    @Test
    fun `il libero scrive in due campi e un gettone ne chiude uno`() {
        banco.setContent { Scena(resizing = true, piano = Resize.NONE) }
        pronta()
        banco.onNodeWithContentDescription(testo(R.string.look_resize))
            .performTouchInput { longClick() }
        banco.waitForIdle()
        assertEquals(
            "Nel libero non si scrivono tutti e due i campi",
            2,
            banco.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size
        )
        chip(R.string.look_resize_px).assertIsNotSelected()

        chip(R.string.resize_long).performClick()
        banco.waitForIdle()
        assertEquals(
            "Con un gettone di lato è rimasto più di un campo scrivibile",
            1,
            banco.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size
        )
        chip(R.string.resize_long).assertIsSelected()

        // ⚠️ Toccando 'Pixel' i due campi tornano scrivibili, e il gettone resta spento: è il
        // riscontro che quel comando dà, al posto di un tondo che si colora.
        chip(R.string.look_resize_px).performClick()
        banco.waitForIdle()
        assertEquals(
            "Il libero non ha riacceso il campo chiuso",
            2,
            banco.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size
        )
        chip(R.string.look_resize_px).assertIsNotSelected()
        chip(R.string.resize_long).assertIsNotSelected()
    }

    /**
     * **Caso 15: scrivendo in un campo l'altro si aggiorna.**
     *
     * ⚠️ **Senza, i due numeri direbbero un rapporto che il salvataggio non rispetterà**: le
     * proporzioni si mantengono sempre, quindi un'altezza rimasta indietro è una promessa falsa
     * scritta nella finestra. Il conto è misurato dal caso 13; qui si misura che la finestra lo
     * chiami.
     */
    @Test
    fun `scrivendo in un campo l'altro segue`() {
        banco.setContent { Scena(resizing = true, piano = Resize.NONE) }
        pronta()
        banco.onNodeWithContentDescription(testo(R.string.look_resize))
            .performTouchInput { longClick() }
        banco.waitForIdle()
        banco.onAllNodes(hasSetTextAction())[0].performTextReplacement("120")
        banco.waitForIdle()
        assertEquals(
            "L'altro campo non ha seguito la larghezza scritta",
            2,
            banco.onAllNodesWithText("120").fetchSemanticsNodes().size
        )
    }

    /**
     * **Caso 16: la misura di partenza si scrive sotto il titolo, senza dicitura.**
     *
     * ⚠️⚠️ **DALLA `2.81` QUELLA RIGA NON PASSA PIÙ DA UNA FRASE TRADOTTA** (testo
     * `t-resize-now`: *Restano solo le dimensioni effettive, senza `Dimensioni attuali: `*),
     * quindi la prova che misurava il segnaposto è uscita con la funzione che lo cercava: qui si
     * misura quello che resta, cioè che la riga sia i soli numeri col segno e l'unità.
     */
    @Test
    fun `la misura di partenza è il solo numero`() {
        assertEquals(
            "La riga sotto il titolo non è la sola misura",
            "1800 × 1200 px",
            plain(1800, 1200)
        )
    }

    /**
     * **Caso 17: un modo è portabile solo se dice la stessa cosa su qualunque immagine.**
     *
     * ⚠️ **È il conto su cui si spegne 'Rendi predefinito'** (voce `resize-finestra`: *cliccabile
     * solo se sono attivi 'Lato lungo', 'Lato corto' o '%'*), e si misura da sé perché è Kotlin
     * puro: un modo in più aggiunto alla famiglia sbagliata darebbe un predefinito che su
     * un'immagine girata dall'altra parte scrive un file di un'altra misura, senza dare errore.
     */
    @Test
    fun `solo tre modi valgono come predefinito`() {
        val portabili = Resize.Mode.entries.filter { Resize.portable(it) }.toSet()
        assertEquals(
            "I modi che valgono come predefinito non sono i suoi tre",
            setOf(Resize.Mode.LONG, Resize.Mode.SHORT, Resize.Mode.SHARE),
            portabili
        )
    }

    /**
     * **Caso 18: 'Rendi predefinito' si accende nei tre modi, scrive, e poi si spegne.**
     *
     * ⚠️⚠️ **LO SPEGNIMENTO È IL SOLO RISCONTRO CHE QUEL COMANDO DÀ**, perché una notifica di
     * casa si aprirebbe **dietro** questa finestra: senza, il tocco non direbbe niente e si
     * toccherebbe due volte.
     * ⚠️ **Controprovata togliendo il confronto col predefinito**: il tasto resta acceso dopo il
     * tocco, cioè la prova cade.
     */
    @Test
    fun `Rendi predefinito si accende, scrive e si spegne`() {
        var scritto: Resize.Plan? = null
        val salvato = mutableStateOf(Resize.NONE)
        banco.setContent {
            Scena(
                resizing = true,
                piano = Resize.Plan(Resize.Mode.LONG, LATO / 2),
                salvato = salvato.value,
                onDefault = { scritto = it; salvato.value = it }
            )
        }
        pronta()
        banco.onNodeWithContentDescription(testo(R.string.look_resize))
            .performTouchInput { longClick() }
        banco.waitForIdle()

        // Col libero, che dipende dall'immagine aperta, il comando non c'è da toccare.
        banco.onNodeWithText(testo(R.string.look_resize_reset))
            .performScrollTo()
            .performClick()
        banco.waitForIdle()
        comando().assertIsNotEnabled()

        // Con 'Lato lungo' si accende, e il tocco scrive il piano che la finestra ha in mano.
        // ⚠️ Lo scorrimento serve perché il tocco su 'Ripristina' ha portato il corpo in fondo.
        chip(R.string.resize_long).performScrollTo().performClick()
        banco.waitForIdle()
        comando().assertIsEnabled()
        comando().performClick()
        banco.waitForIdle()
        assertEquals(
            "Il comando non ha scritto il piano che la finestra aveva in mano",
            Resize.Mode.LONG,
            scritto?.mode
        )
        comando().assertIsNotEnabled()
    }

    /**
     * **Caso 19: il segno fra i due campi cade sulle cifre e non al centro del campo.**
     *
     * ⚠️ **È la sua riga alla lettera** (*il segno `×` è allineato meglio in verticale, in modo
     * che sia centrato in verticale rispetto alle cifre*): un campo con l'etichetta in alto porta
     * il testo più in basso del proprio centro, quindi un segno centrato sul campo si legge più
     * alto delle cifre che separa.
     * ⚠️ **Si misura il verso e non il numero**: che il centro del segno stia **sotto** quello del
     * campo è il fatto, e una soglia sul numero cadrebbe al primo ritocco di
     * [FIELD_TEXT_DROP]. Controprovata rimettendo `CenterVertically`: i due centri coincidono.
     */
    @Test
    fun `il segno fra i campi scende sulle cifre`() {
        banco.setContent { Scena(resizing = true, piano = Resize.NONE) }
        pronta()
        banco.onNodeWithContentDescription(testo(R.string.look_resize))
            .performTouchInput { longClick() }
        banco.waitForIdle()
        val segno = banco.onNodeWithText("×").fetchSemanticsNode().boundsInRoot
        val campo = banco.onAllNodes(hasSetTextAction())[0].fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Il segno è centrato sul campo invece che sulle cifre" +
                " (segno ${segno.center.y}, campo ${campo.center.y})",
            segno.center.y > campo.center.y + 1f
        )
    }

    /**
     * **Caso 20: i sei gettoni stanno tre e tre, e il segno è in prima riga.**
     *
     * ⚠️⚠️ **È LA SUA RIGA ALLA LETTERA** (voce `resize-finestra-2`: *'%' deve stare sulla prima
     * riga*): fino alla `2.81` la fila andava a capo da sé, quindi la ripartizione dipendeva da
     * quanto le parole misurano nella lingua del telefono, e sul suo il segno scendeva per
     * quattro punti.
     * ⚠️⚠️ **SI MISURA LA RIPARTIZIONE E NON LE LARGHEZZE**: quanto un testo misura sul banco non
     * è quanto misura su un telefono (§ '🧪 Quando si scrive una prova, e quando no'), quindi una
     * soglia in punti qui direbbe una cosa che sul telefono non vale. Quello che vale sempre è
     * che i primi tre gettoni siano alla stessa altezza e gli altri tre più sotto.
     * ⚠️⚠️ **LA SCENA È LARGA, E SENZA QUELLA RIGA LA PROVA ERA VERDE A VUOTO**: sulla scena di
     * serie la ripartizione viene tre e tre **anche** con la fila che va a capo da sé, quindi
     * rimettendo il difetto la prova restava verde, cioè non misurava niente. Con una finestra
     * larga le due forme si separano, perché questa dichiara le sue due righe e quella le
     * ricava dallo spazio.
     * ⚠️ **Controprovata rimettendo la `FlowRow` della `2.81`**: là la ripartizione diventa
     * `3 + 2 + 1` (misurato: le cime valgono 96 e 144 sulle ultime due), e cade l'asserzione
     * della seconda riga.
     */
    @Test
    @Config(qualifiers = "w600dp-h900dp")
    fun `i sei gettoni stanno tre e tre`() {
        banco.setContent { Scena(resizing = true, piano = Resize.NONE) }
        pronta()
        banco.onNodeWithContentDescription(testo(R.string.look_resize))
            .performTouchInput { longClick() }
        banco.waitForIdle()
        val cime = listOf(
            R.string.resize_long,
            R.string.resize_short,
            R.string.resize_share,
            R.string.resize_wide,
            R.string.resize_tall,
            R.string.look_resize_px
        ).map { chip(it).fetchSemanticsNode().boundsInRoot.top }

        assertEquals("La prima riga non è una riga sola", cime[0], cime[1], 1f)
        assertEquals("Il segno non è sulla prima riga", cime[0], cime[2], 1f)
        assertEquals("La seconda riga non è una riga sola", cime[3], cime[4], 1f)
        assertEquals("La seconda riga non è una riga sola", cime[3], cime[5], 1f)
        assertTrue(
            "Le due righe sono la stessa (${cime[0]} e ${cime[3]})",
            cime[3] > cime[0] + 1f
        )
    }

    /** Il comando 'Rendi predefinito' della riga del titolo. */
    private fun comando() =
        banco.onNodeWithText(testo(R.string.look_resize_default))

    /**
     * Il gettone di un modo, che si può scegliere e porta il suo nome.
     *
     * ⚠️ **Il nome può vivere nella descrizione parlata invece che nell'etichetta**: dalla `2.81`
     * la percentuale si scrive col segno, quindi cercarla per solo testo non la troverebbe.
     */
    private fun chip(id: Int) = banco.onNode(
        isSelectable() and (hasText(testo(id)) or hasContentDescription(testo(id)))
    )

    /** L'editor completo su un foglio bianco, coi soli argomenti che questo banco muove. */
    @Composable
    private fun Scena(
        resizing: Boolean,
        piano: Resize.Plan = Resize.Plan(Resize.Mode.LONG, LATO / 2),
        onResize: (Resize.Plan?) -> Unit = {},
        salvato: Resize.Plan = Resize.NONE,
        onDefault: (Resize.Plan) -> Unit = {}
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
                    resize = piano,
                    saved = salvato,
                    resizing = resizing,
                    onResize = onResize,
                    onResizeDefault = onDefault,
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

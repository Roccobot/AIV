package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Il banco di prova del **salto**, che dalla `2.07` vive sul glifo del FAB.
 *
 * ⚠️⚠️ **NASCE CON LA FUNZIONE E LE SOPRAVVIVE**: la corsa passa il proprio movimento allo
 * **scorrimento annidato** prima che alla lista, cioè fa a mano quello che un dito ottiene dal
 * sistema, e là i segni sono due mondi opposti. Un `-` di troppo dà un salto che va dalla parte
 * sbagliata, e nessun compilatore lo vede. Quella prova è l'unica che attraversa il cambio di
 * concept senza una riga diversa, perché [glide] non è cambiata.
 *
 * ⚠️⚠️ **E LE ALTRE MISURANO IL MOTORE DEL GLIFO, che è dove il concept nuovo può rompersi in
 * silenzio**: il verso che segue il dito, la soglia che impedisce lo sfarfallio, e soprattutto
 * che la **corsa non conti come trascinamento**. Quest'ultimo è un difetto che è arrivato prima
 * nel mockup che in Compose: toccando 'Vai all'inizio' la lista sale, cioè scorre nel verso
 * opposto a quello che ha armato il tasto, e senza una guardia il chevron si gira sotto il dito
 * che l'ha appena toccato.
 *
 * ⚠️ **Che cosa NON vede**: quanto il glifo impieghi a cambiare per l'occhio, la curva del
 * rientro e la decelerazione della corsa. Sono rese, e si guardano sul telefono.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [OmbraArchivio::class])
class SaltiTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **Il salto in giù chiude l'intestazione e scorre la lista; quello in su le riapre tutte e
     * due.**
     *
     * ⚠️⚠️ **LA SECONDA METÀ È LA SUA RICHIESTA ALLA LETTERA** (*il tasto 'su' fa scorrere in
     * cima fino alla visualizzazione piena dell'intestazione*): una corsa che muovesse la sola
     * lista arriverebbe in cima con la fascia ancora chiusa, e la prova diventa rossa.
     * ⚠️ **Controprovata rimettendo il difetto, e la misura corregge quello che mi aspettavo**:
     * passando `nested = null` fallisce la **prima** asserzione, perché senza lo scorrimento
     * annidato la fascia non si chiude affatto (resta a 0) invece di restare chiusa alla fine.
     * Cioè il difetto si vede già scendendo, un passo prima di dove lo cercavo.
     */
    @Test
    fun `il salto passa dallo scorrimento annidato`() {
        var shut = 0f
        var lista: LazyListState? = null
        var vai: (Float) -> Unit = {}
        banco.setContent {
            val state = rememberLazyListState()
            var chiuso by remember { mutableFloatStateOf(0f) }
            val paging = remember { frontScroll(FASCIA, { chiuso }, { chiuso = it }) }
            val scope = rememberCoroutineScope()
            lista = state
            vai = { quanti -> scope.launch { glide(state, paging, quanti) }; Unit }
            shut = chiuso
            Box(Modifier.fillMaxSize().nestedScroll(paging)) {
                LazyColumn(state = state) {
                    items(RIGHE) { n -> Text("riga $n", modifier = Modifier.height(RIGA.dp)) }
                }
            }
        }
        banco.waitForIdle()

        banco.runOnIdle { vai(LONTANO) }
        banco.waitForIdle()
        assertEquals("Scendendo, la fascia deve chiudersi tutta", FASCIA, shut, 0.5f)
        assertTrue(
            "Scendendo, la lista deve muoversi",
            (lista?.firstVisibleItemIndex ?: 0) > 0
        )

        banco.runOnIdle { vai(-LONTANO) }
        banco.waitForIdle()
        assertEquals("Salendo, la lista deve tornare in cima", 0, lista?.firstVisibleItemIndex)
        assertEquals("Salendo, la fascia deve riaprirsi", 0f, shut, 0.5f)
    }

    /**
     * **A riposo il glifo è quello dell'app, e il tocco non è ancora il salto.**
     *
     * ⚠️ Appena aperta una schermata nessuno ha ancora scorso, quindi il chevron non ha ragione
     * di esistere: questa è la misura di quello che si vede al primo sguardo.
     */
    @Test
    fun `a riposo il glifo non e il chevron`() {
        val arm = montaArm()
        banco.waitForIdle()

        assertEquals("Il chevron non deve essere in scena", 0f, arm().shown, 0.001f)
        assertFalse("Il tocco non deve ancora fare il salto", arm().armed)
    }

    /**
     * **Scorrendo verso il fondo il tasto si arma su 'Vai alla fine'.**
     *
     * ⚠️⚠️ **IL VERSO È QUELLO DELLA LISTA E NON QUELLO DEL DITO**, ed è il punto in cui un segno
     * sbagliato non darebbe nessun errore: un dito che sale porta la lista verso il fondo, quindi
     * il chevron giusto è quello che indica il fondo. Con il segno rovesciato la prova trova
     * `-1` dove si aspetta `+1`.
     */
    @Test
    fun `scorrendo il tasto si arma nel verso della lista`() {
        val arm = montaArm()
        banco.waitForIdle()

        scorri(su = true)

        assertTrue("Dopo un gesto lungo il tasto deve essere armato", arm().armed)
        assertEquals("Il verso deve essere quello del fondo", 1, arm().toward)
        assertEquals("Il chevron deve essere al suo posto", 1f, arm().shown, 0.001f)
    }

    /**
     * **Cambiando verso, il chevron si gira sul posto.**
     *
     * ⚠️ **La soglia è la metà che conta**: un dito che scorre non va mai in un verso solo, e
     * senza [SWERVE] un rimbalzo di pochi pixel girerebbe il disegno a ogni gesto. Qui il gesto
     * contrario è lungo, quindi la soglia la supera e il verso deve cambiare.
     */
    @Test
    fun `il verso segue il dito`() {
        val arm = montaArm()
        banco.waitForIdle()

        scorri(su = true)
        assertEquals("Prima il fondo", 1, arm().toward)

        scorri(su = false)
        assertEquals("Poi l'inizio", -1, arm().toward)
    }

    /**
     * **Nella schermata vera il FAB annuncia il salto solo quando il salto è quello che fa.**
     *
     * ⚠️⚠️ **MISURA IL LEGAME, che è la cosa che un chiamante può sbagliare in silenzio**: il
     * motore può funzionare benissimo e il FAB restare quello di prima, se chi lo disegna non
     * legge l'arm. Il codice compilerebbe e la funzione non ci sarebbe.
     * ⚠️ **L'etichetta e non i pixel**: quello che cambia sul FAB è un disegno incrociato, e
     * contarne i pixel misurerebbe la dissolvenza invece del comando. Il lettore di schermo
     * invece riceve una frase sola, che è quella che dice che cosa fa il tasto adesso.
     */
    @Test
    fun `il FAB annuncia il salto solo a tasto armato`() {
        banco.mainClock.autoAdvance = false
        banco.setContent { Scena() }
        banco.mainClock.advanceTimeBy(NASCITA)

        assertEquals(
            "A riposo il FAB non deve annunciare il salto",
            0,
            quanti(R.string.jump_bottom)
        )

        scorri(su = true)

        assertEquals(
            "Dopo lo scorrimento il FAB deve annunciare 'Vai alla fine'",
            1,
            quanti(R.string.jump_bottom)
        )
    }

    /**
     * La scena minima: una lista lunga, col motore del glifo attaccato al suo gesto.
     *
     * ⚠️⚠️ **LA LISTA VUOLE `fillMaxSize`, E SENZA NON SCORRE AFFATTO**: dentro una `Box` una
     * `LazyColumn` si dimensiona sul proprio contenuto, quindi non ha un viewport più corto di
     * lui e non genera **nessun** evento di scorrimento annidato. La prima stesura di questa
     * prova era rossa col codice giusto per quella riga sola, e a trovarlo è stata una spia
     * messa dentro il nodo: dalla catena non arrivava niente, invece di arrivare zero.
     */
    private fun montaArm(): () -> JumpArm {
        /*
         * ⚠️ **Il clock si ferma**, o `waitForIdle` porterebbe a termine l'attesa del congedo e il
         * glifo rientrerebbe prima che la prova possa guardarlo. È la stessa trappola di
         * `AvvisiTest`, e là è scritta per esteso.
         */
        banco.mainClock.autoAdvance = false
        var arm: JumpArm? = null
        banco.setContent {
            val state = rememberLazyListState()
            var chiuso by remember { mutableFloatStateOf(0f) }
            val paging = remember { frontScroll(FASCIA, { chiuso }, { chiuso = it }) }
            val a = rememberJumpArm(
                state = state,
                up = { state.jumpUpPixels() },
                down = { state.jumpDownPixels() }
            )
            arm = a
            Box(Modifier.fillMaxSize().nestedScroll(a.watch).nestedScroll(paging)) {
                LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
                    items(RIGHE) { n -> Text("riga $n", modifier = Modifier.height(RIGA.dp)) }
                }
            }
        }
        banco.mainClock.advanceTimeBy(NASCITA)
        return { arm!! }
    }

    /**
     * Un trascinamento che è uno scorrimento e non un lancio.
     *
     * ⚠️⚠️ **`down` e `moveTo` E NON `swipe`, ED È MISURATO**: col clock fermo uno `swipe` con
     * la sua durata inietta i passi intermedi a un tempo che non avanza, e al motore del glifo
     * non arriva niente. La prima stesura di questa prova era rossa **col codice giusto** per
     * quella ragione, e la forma qui sotto è quella che l'app riceve da un dito vero.
     * ⚠️ **Il dito resta fermo prima di staccarsi**, più a lungo della finestra del velocimetro
     * (100 ms): così la velocità stimata è zero e la lista non parte per inerzia, che
     * rimetterebbe a zero il conto alla rovescia mentre la prova guarda.
     */
    private fun scorri(su: Boolean) {
        val scena = banco.onRoot().fetchSemanticsNode().size
        banco.onRoot().performTouchInput {
            val da = if (su) DA else A
            val a = if (su) A else DA
            down(Offset(scena.width * LATO, scena.height * da))
            moveTo(Offset(scena.width * LATO, scena.height * a))
            advanceEventTime(FERMO)
            up()
        }
        /*
         * ⚠️ **Il respiro resta sotto [QUIET_MS]**: più lungo, il congedo scadrebbe e il glifo
         * rientrerebbe prima che la prova possa guardarlo.
         */
        banco.mainClock.advanceTimeBy(RESPIRO)
    }

    private fun voce(id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun quanti(id: Int): Int =
        banco.onAllNodesWithContentDescription(voce(id)).fetchSemanticsNodes().size

    /** La griglia di una cartella con gli argomenti minimi, come nelle altre prove. */
    @Composable
    private fun Scena() {
        AivTheme(darkTheme = false) {
            Box(modifier = Modifier.fillMaxSize()) {
                GridScreen(
                    title = "Cartella di prova",
                    items = (1..40).map { Uri.parse("file:///finta/$it.jpg") },
                    highlight = null,
                    onOpen = {},
                    onBack = {},
                    onChanged = {},
                    onSearch = {},
                    /*
                     * ⚠️⚠️ **SENZA QUESTA RIGA IL FAB NON C'È AFFATTO, ed è la ragione per cui
                     * la prima stesura era rossa**: `FabPop` compare solo se la griglia ha
                     * dove mandare (il cestino, le impostazioni, la ricerca qui), e una
                     * cartella montata con gli argomenti minimi non ne ha nessuno. Il motore
                     * del glifo funzionava, e il tasto su cui disegnarlo non esisteva.
                     */
                    onSettings = {}
                )
            }
        }
    }
}

/** Quanto è alta la fascia finta della prova, in pixel. */
private const val FASCIA = 300f

/** Quante righe ha la lista finta: abbastanza da avere sempre dove andare. */
private const val RIGHE = 200

/** Quanto è alta una riga, in punti. */
private const val RIGA = 48

/**
 * Un salto più lungo di tutta la lista.
 *
 * ⚠️ **Di proposito eccessivo**: la corsa finisce quando nessuno prende più niente, quindi una
 * stima lunga arriva al bordo e si ferma. Chiedendo esattamente la distanza, la prova
 * misurerebbe la stima invece del meccanismo.
 */
private const val LONTANO = 100_000f

/** Quanto si lascia respirare la scena dopo un gesto, col clock fermo. */
private const val RESPIRO = 100L

/** Quanto la scena si compone prima che la prova la tocchi. */
private const val NASCITA = 1_000L

/**
 * Quanto il dito resta fermo prima di staccarsi, in millisecondi.
 *
 * ⚠️ **Più lungo della finestra del velocimetro** (100 ms): così l'ultimo campione di movimento è
 * fuori tempo massimo, la velocità stimata è zero e la lista non parte per inerzia.
 */
private const val FERMO = 300L

/** Da dove a dove va il trascinamento, in frazioni dell'altezza della scena. */
private const val DA = 0.8f
private const val A = 0.2f

/**
 * Su quale colonna passa il dito, in frazioni della larghezza.
 *
 * ⚠️ **A sinistra e non al centro**: il FAB vive in fondo a destra, e un gesto che gli passasse
 * sopra finirebbe su di lui invece che sulla lista.
 */
private const val LATO = 0.25f


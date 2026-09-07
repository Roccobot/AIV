package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Il banco di prova della scelta fra **sfocatura e ombreggiatura**, che l'utente vuole *MAI
 * insieme* (sua istruzione, 2026-09-07).
 *
 * ⚠️⚠️ **NASCE CON LA FUNZIONE E NON DOPO UN DIFETTO, ED È IL CASO PROATTIVO DELLA REGOLA**
 * (`AIV/CLAUDE.md` § '🧪 Quando si scrive una prova, e quando no'): una modifica che tocca la
 * gerarchia dei tocchi porta la sua prova anche senza un difetto alle spalle, e qui ce ne sono
 * due dei tre casi che quella regola elenca. Il menu con l'ombra **misura** e ci posa dentro
 * qualcosa (l'aria intorno al pannello), e quell'aria deve decidere che cosa fa il tocco.
 *
 * ⚠️⚠️ **CHE COSA QUESTA PROVA NON VEDE, e va detto invece di lasciarlo credere: L'OMBRA.** Un
 * `shadowElevation` si vede solo quando qualcuno lo disegna, e il banco non rende niente: quello
 * che si misura qui è che l'ombra abbia **dove** cadere e che chi ha scelto lei non si prenda
 * anche il velo. Se l'ombra si vede, e come si vede, lo dice il telefono.
 *
 * ⚠️ **Una prova monta la scena UNA volta sola**, e i due casi si confrontano cambiando la
 * scelta in uno stato: `setContent` una seconda volta va in errore, e la prima stesura di questa
 * classe lo faceva in un ciclo. ⚠️ **E nel nome di una prova non vanno gli accenti**: il report
 * di Gradle scrive un file per prova e su un nome accentato non riesce a nominarlo, quindi il
 * banco fallisce **dopo** aver eseguito tutto, con un errore che non parla del codice.
 */
@RunWith(AndroidJUnit4::class)
class ProfonditaTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * ⚠️ **La scena dei menu e la mappa del velo sono oggetti CONDIVISI**, quindi una prova che
     * li lascia sporchi fa fallire la prossima invece della propria.
     */
    @After
    fun pulisci() {
        MenuScene.clear()
        VeilStage.clear()
    }

    /**
     * **Caso 1: chi sceglie l'ombra non si prende anche il velo, e chi sceglie la sfocatura sì.**
     *
     * ⚠️⚠️ **È 'MAI INSIEME' MISURATO SU QUELLO CHE SI PUÒ MISURARE**: la patina scura è un
     * rettangolo che l'app dipinge, e quanta ne chiede una superficie in scena sta in
     * [VeilStage]. Con l'ombra quella richiesta deve valere **zero**, o lo schermo sarebbe
     * scurito **e** il pannello alzato, che è la cosa che l'utente ha escluso.
     * ⚠️ **La sfocatura di finestra non è in questa misura**, e non è una dimenticanza: è un
     * attributo che si chiede al gestore delle finestre, e su una macchina senza schermo non c'è
     * nessuno che risponda. A tenerla legata alla stessa scelta è una condizione sola, in
     * [WindowVeil].
     */
    @Test
    fun `la patina la chiede solo la sfocatura`() {
        val scelta = mutableStateOf(PanelDepth.BLUR)
        banco.setContent {
            Scena(scelta) { AppPatina { 1f } }
        }

        banco.waitForIdle()
        val conSfocatura = VeilStage.dose
        assertTrue("Con la sfocatura la patina non arriva: $conSfocatura", conSfocatura > 0f)

        scelta.value = PanelDepth.SHADOW
        banco.waitForIdle()
        assertEquals(
            "Con l'ombra lo schermo si scurisce anche: le due vie si sono sommate",
            0f,
            VeilStage.dose,
            0f
        )

        scelta.value = PanelDepth.NONE
        banco.waitForIdle()
        assertEquals("Senza nessuna delle due lo schermo si scurisce", 0f, VeilStage.dose, 0f)
    }

    /**
     * **Caso 2: con l'ombra il menu tiene aria intorno al pannello, con la sfocatura no.**
     *
     * ⚠️⚠️ **L'ARIA È LA CONDIZIONE PERCHÉ L'OMBRA ESISTA, quindi è la cosa da misurare**: la
     * finestra di un `Popup` è grande quanto quello che ci si misura dentro, e un'ombra esce dal
     * pannello. Senza aria viene tagliata sul rettangolo della finestra, ed è il taglio per cui
     * l'ombra era uscita dall'app nella `1.54`.
     * ⚠️ **E la seconda metà del caso serve quanto la prima**: con la sfocatura quell'aria non
     * deve esserci, perché una finestra più grande del pannello sfoca una fascia di sfondo
     * intorno a lui, che è la cornice del giro della `1.51`.
     */
    @Test
    fun `solo con l'ombra il menu tiene aria intorno al pannello`() {
        val scelta = mutableStateOf(PanelDepth.SHADOW)
        banco.setContent { Scena(scelta) { MenuDiProva() } }
        banco.waitForIdle()

        val conOmbra = attorno()
        scelta.value = PanelDepth.BLUR
        banco.waitForIdle()
        val conSfocatura = attorno()

        assertTrue(
            "Con l'ombra la finestra non è più alta del pannello: l'ombra verrebbe tagliata " +
                "($conOmbra contro $conSfocatura)",
            conOmbra > conSfocatura
        )
    }

    /**
     * **Caso 3: il tocco sull'aria del menu lo chiude.**
     *
     * ⚠️⚠️ **SENZA QUESTO L'ARIA SAREBBE UNA FASCIA MORTA, ed è un difetto che l'utente ha già
     * segnalato una volta** (giro della `1.69`, sull'aria dei dialoghi): quello che si tocca là
     * appartiene alla **finestra** del menu, quindi `dismissOnClickOutside` non lo vede, e un
     * dito appena fuori dal pannello non farebbe niente.
     * ⚠️ **Il tocco si dà sulla finestra del popup e non sull'app**: sono due root diverse
     * nell'albero della prova, e quella del menu si riconosce da chi ha dentro.
     */
    @Test
    fun `il tocco sull'aria del menu lo chiude`() {
        lateinit var stato: MenuState
        val scelta = mutableStateOf(PanelDepth.SHADOW)
        banco.setContent { Scena(scelta) { stato = MenuDiProva() } }
        banco.waitForIdle()
        assertTrue("Il menu non si è aperto: non c'è niente da chiudere", stato.visible)

        finestra().performTouchInput { click(Offset(2f, 2f)) }
        banco.waitForIdle()

        assertFalse("Il tocco sull'aria non ha chiuso il menu", stato.wanted)
    }

    /**
     * **Caso 4: il tocco DENTRO il pannello non chiude il menu.**
     *
     * L'altra metà del caso 3: senza di lui quello passerebbe anche con un nodo che chiude a
     * ogni tocco, cioè con un menu che si chiude quando si prova a toccarne una voce.
     */
    @Test
    fun `il tocco sulla voce non chiude il menu`() {
        lateinit var stato: MenuState
        val scelta = mutableStateOf(PanelDepth.SHADOW)
        banco.setContent { Scena(scelta) { stato = MenuDiProva() } }
        banco.waitForIdle()

        banco.onNodeWithTag(VOCE).performTouchInput { click(Offset(100f, 40f)) }
        banco.waitForIdle()

        assertTrue("Un tocco sulla voce ha chiuso il menu", stato.wanted)
    }

    /** La finestra del menu, che nell'albero della prova è una root a sé. */
    private fun finestra() = banco.onNode(isRoot() and hasAnyDescendant(hasTestTag(VOCE)))

    /** Quanto della finestra del menu non è occupato dalla voce, in pixel. */
    private fun attorno(): Int =
        finestra().fetchSemanticsNode().size.height -
            banco.onNodeWithTag(VOCE).fetchSemanticsNode().size.height

    /**
     * Il tema vero con la scelta in scena, che è il minimo perché la misura valga: il velo
     * dell'app e il cancello dei menu vivono dentro [AivTheme].
     */
    @Composable
    private fun Scena(scelta: MutableState<PanelDepth>, dentro: @Composable () -> Unit) {
        CompositionLocalProvider(LocalAivDepth provides scelta.value) {
            AivTheme(darkTheme = false) { dentro() }
        }
    }
}

/**
 * Un menu aperto con una voce di misura nota.
 *
 * ⚠️ **Si apre da un effetto e non durante la composizione**: `MenuShell` anima da `wanted`, e
 * chiamare [MenuState.open] mentre si compone farebbe partire l'animazione prima che il pannello
 * esista.
 */
@Composable
private fun MenuDiProva(): MenuState {
    val stato = rememberMenuState()
    LaunchedEffect(stato) { stato.open() }
    MenuShell(state = stato, position = MenuInWindow) {
        Box(
            modifier = Modifier
                .testTag(VOCE)
                .width(200.dp)
                .height(80.dp)
        )
    }
    return stato
}

private const val VOCE = "voce"

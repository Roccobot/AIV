package io.github.roccobot.aiv

import android.content.Context
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Il banco di prova del **pannello delle impostazioni su due livelli**, dalla `2.09`.
 *
 * ⚠️⚠️ **NASCE CON LA FUNZIONE E NON DOPO UN DIFETTO**, ed è il caso proattivo di
 * `AIV/CLAUDE.md` § '🧪 Quando si scrive una prova, e quando no': la strada B (sua risposta
 * `livelli` a `d-imp-strada`) tocca due cose che un compilatore non guarda, cioè **dove porta
 * Indietro** e **che cosa trova la ricerca**. In tutti e due i casi il codice può essere valido
 * e la funzione non esserci.
 *
 * ⚠️⚠️ **LA COPERTURA DELLA RICERCA A DUE LIVELLI ANDAVA MISURATA, e il brief lo chiedeva**: il
 * meccanismo che appiattisce una pagina dentro la radice ([PageOfRows]) **dovrebbe** annidarsi
 * da sé, perché il corpo di una pagina si compone dentro il provider di `LocalQuery` e là dentro
 * la stessa condizione vale un'altra volta. Questa prova è la misura, e dice di sì.
 *
 * ⚠️⚠️ **LE DUE CONTROPROVE SONO STATE FATTE, e senza di loro questa classe sarebbe tre righe
 * verdi**: rimettendo `back()` che svuota la pila (cioè il comportamento della `2.08`) cade la
 * prova della risalita, e togliendo l'appiattimento alla porta del visualizzatore cade quella
 * della ricerca. Ognuna cade da sola, quindi le due misure non si coprono a vicenda.
 *
 * ⚠️⚠️ **UNA TRAPPOLA DEL BANCO, TROVATA SCRIVENDO QUESTE PROVE**: una riga che vive **sotto il
 * bordo** della finestra è nell'albero semantico ma non riceve il tocco, quindi `performClick`
 * non apre niente e la prova fallisce **col codice giusto**. Le porte vivono in fondo alla
 * prima sezione, cioè fuori da una finestra alta come quella di Robolectric: prima si scorre
 * fin là (`performScrollTo`) e poi si tocca.
 *
 * ⚠️ **Che cosa NON vede**: quanti tocchi costi arrivare a una voce per chi usa l'app, che è
 * una questione di percezione, e se le famiglie siano quelle giuste, che è una decisione. Vede
 * che la navigazione ha una pila e che la ricerca arriva in fondo, che sono due fatti.
 */
@RunWith(AndroidJUnit4::class)
class ImpostazioniTest {

    @get:Rule
    val banco = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private fun testo(id: Int) = app.getString(id)

    /** Il pannello vero, con gli argomenti minimi: quello che si tocca qui è quello dell'app. */
    private fun apriIlPannello() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                SettingsScreen(
                    settings = Settings(),
                    onChange = {},
                    onStartFolder = {},
                    onResetHints = {},
                    onChooseEditor = {},
                    onBack = {}
                )
            }
        }
        banco.waitForIdle()
    }

    /**
     * **Indietro da una pagina di secondo livello riporta a quella di sopra, non alla radice.**
     *
     * ⚠️⚠️ **È IL FATTO CHE LA `2.08` NON POTEVA AVERE**: fino a lei la navigazione era un valore
     * solo e Indietro scriveva `Page.ROOT`, quindi da 'Adattamento e zoom' si finiva nella pagina
     * piatta saltando 'Visualizzatore'. Con la pila la risalita è di un gradino per volta.
     * ⚠️ **Il secondo Indietro è parte della misura**: senza, una pila che non si svuota mai
     * passerebbe lo stesso, e chi apre le impostazioni resterebbe chiuso dentro.
     */
    @Test
    fun `Indietro risale un gradino per volta`() {
        apriIlPannello()
        val radice = testo(R.string.settings_title)
        val visualizzatore = testo(R.string.settings_group_viewer)
        val zoom = testo(R.string.settings_zoom_page)
        val indietro = testo(R.string.settings_back)

        banco.onNodeWithText(radice).assertExists()
        banco.onNodeWithText(visualizzatore).performScrollTo().performClick()
        banco.waitForIdle()

        banco.onNodeWithText(zoom).assertExists()
        banco.onNodeWithText(radice).assertDoesNotExist()

        banco.onNodeWithText(zoom).performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.settings_fit_grow)).assertExists()

        banco.onNodeWithContentDescription(indietro).performClick()
        banco.waitForIdle()
        banco.onNodeWithText(zoom).assertExists()
        banco.onNodeWithText(radice).assertDoesNotExist()

        banco.onNodeWithContentDescription(indietro).performClick()
        banco.waitForIdle()
        banco.onNodeWithText(radice).assertExists()
    }

    /**
     * **Una voce che vive due livelli sotto si trova lo stesso con la ricerca.**
     *
     * ⚠️ La voce scelta è la più lontana che ci sia: vive in 'Adattamento e zoom', che vive in
     * 'Visualizzatore', che si apre dalla pagina piatta. Se l'appiattimento si fermasse al primo
     * livello, qui comparirebbe la riga di 'Visualizzatore' invece della voce.
     */
    @Test
    fun `la ricerca arriva fino al secondo livello`() {
        apriIlPannello()
        val voce = testo(R.string.settings_fit_grow)

        banco.onNodeWithText(voce).assertDoesNotExist()
        banco.onNode(hasSetTextAction()).performTextInput(voce.substringBefore(' '))
        banco.waitForIdle()

        banco.onNodeWithText(voce).assertExists()
    }

    /**
     * **Una pagina che è un ELENCO si trova dai nomi delle sue righe, anche se è scesa.**
     *
     * ⚠️ L'elenco dei dati non si appiattisce (le frecce lavorano sull'ordine intero), quindi la
     * sua copertura è il parametro `extra` della riga che lo apre. Quella riga adesso vive dentro
     * 'Informazioni', cioè un livello più in basso: se l'appiattimento non arrivasse là, il nome
     * di un campo non troverebbe più niente.
     */
    @Test
    fun `la ricerca trova un campo dell'elenco dei dati`() {
        apriIlPannello()
        val campo = testo(Settings().factOrder.first().label)
        val elenco = testo(R.string.settings_facts)

        banco.onNodeWithText(elenco).assertDoesNotExist()
        banco.onNode(hasSetTextAction()).performTextInput(campo)
        banco.waitForIdle()

        banco.onNodeWithText(elenco).assertExists()
    }

    /**
     * **Cercando il nome di una pagina compare la riga che la apre.**
     *
     * ⚠️⚠️ **È IL DIFETTO CHE GLI È ARRIVATO** (giro accorpato, voce `imp-ricerca`: *ho cercato
     * 'Adattamento' e non mi ha trovato 'Adattamento e zoom'*): appiattendo sempre, il titolo
     * della pagina usciva di scena, e nessuna delle voci di dentro porta quella parola.
     * ⚠️ **La parola cercata è del TITOLO e di nessuna voce**, o la prova sarebbe verde anche
     * col difetto rimesso: 'Fit and' non compare in nessuna delle righe di quella pagina.
     * ⚠️ **Controprovata** riportando la condizione a `LocalQuery.current.isBlank()`: la riga
     * non compare e la prova cade.
     */
    @Test
    fun `la ricerca trova il titolo di una pagina`() {
        apriIlPannello()
        val pagina = testo(R.string.settings_zoom_page)

        banco.onNodeWithText(pagina).assertDoesNotExist()
        banco.onNode(hasSetTextAction()).performTextInput(pagina.substringBeforeLast(' '))
        banco.waitForIdle()

        banco.onNodeWithText(pagina).assertExists()
    }

    /**
     * **Cercando il nome di una sezione compaiono il suo titolo e le voci che contiene.**
     *
     * ⚠️⚠️ **È L'ALTRA META DELLA SUA VOCE** (*forse dovrebbe trovare anche i titoli di
     * sezione*), e misura le due cose insieme perché una senza l'altra sarebbe peggio del
     * niente: le voci compaiono senza portare la parola cercata, quindi il titolo in cima è la
     * sola cosa che dice perché sono lì.
     * ⚠️ **La voce scelta non ha niente in comune col titolo della sezione**, o la corrispondenza
     * potrebbe venire dal suo testo invece che dalla sezione.
     * ⚠️ **Controprovata** togliendo da `shown` la riga che guarda `LocalSection`: il titolo
     * resta (è lui a corrispondere) e la voce sparisce, cioè cade la prima asserzione.
     * ⚠️⚠️ **IL TITOLO SI CERCA COME INTESTAZIONE, e la prima stesura falliva senza**: quello
     * che si cerca è scritto anche nel **campo di ricerca**, quindi i nodi con quel testo sono
     * due e `assertExists` ne vuole uno. [Group] è dichiarato come intestazione, il campo no.
     */
    @Test
    fun `la ricerca trova le voci di una sezione dal suo titolo`() {
        apriIlPannello()
        val sezione = testo(R.string.settings_group_advanced)
        val voce = testo(R.string.settings_gpu_thumbs)

        banco.onNode(hasSetTextAction()).performTextInput(sezione)
        banco.waitForIdle()

        banco.onNodeWithText(voce).assertExists()
        banco.onNode(hasText(sezione) and isHeading()).assertExists()
    }
}

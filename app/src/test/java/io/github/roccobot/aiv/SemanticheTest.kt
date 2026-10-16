package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Il banco di prova di quello che l'app **dice a un lettore di schermo**, e del bersaglio dei
 * comandi che non sono tasti.
 *
 * ⚠️⚠️ **NASCE COL LOTTO DI ACCESSIBILITÀ DEL CENSIMENTO DELLA UI**, che di rilievi ne aveva
 * quindici, e serve a una cosa che nessun altro controllo di questo repository sa fare: quelle
 * dichiarazioni **non si vedono**. Un ruolo mancante, un'intestazione che non c'è, un bersaglio
 * alto la metà del dovuto non cambiano un solo pixel dello schermo, quindi non li avrebbe presi
 * nemmeno un giro di collaudo col telefono in mano: sono difetti di **struttura**, cioè
 * esattamente quello che il banco vede (`Rules.md` § '🧪 Quando si scrive una prova, e
 * quando no').
 *
 * ⚠️ **Che cosa NON prova**: che TalkBack pronunci una frase sensata, che è la resa vera e
 * dipende dal servizio e dalla lingua del telefono. Quello che si misura è l'albero semantico,
 * che è un dato.
 *
 * ⚠️ **Le scene sono quelle VERE**, come vuole la lezione di `CambioSchermataTest`: ogni prova
 * monta il componente dell'app (la schermata iniziale, la finestra di rinomina, la riga della
 * barra delle info, gli indici del tablet, il riordino dei tasti, i comandi dei filmati). Un
 * albero finto scritto qui accanto misurerebbe l'albero finto.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [OmbraArchivio::class])
class SemanticheTest {

    @get:Rule
    val banco = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * **Il titolo di una schermata è un'intestazione.**
     *
     * ⚠️ Nessuno dei sette titoli dell'app lo dichiarava, quindi la navigazione per intestazioni
     * di TalkBack non trovava un solo punto di aggancio: l'unico modo di scendere una schermata
     * era passare voce per voce. Qui si misura quello della schermata iniziale, che è il primo
     * che si incontra aprendo l'app.
     * ⚠️ **Nella casa il titolo vive nel INTESTAZIONE e non nella testata**: là in cima non c'è
     * niente da leggere, perché il nome dell'app è quello grande sotto l'icona (`Identity`).
     */
    @Test
    fun `il titolo della schermata iniziale e un'intestazione`() {
        banco.setContent { AivTheme(darkTheme = false) { Casa() } }
        banco.waitForIdle()
        val nodo = banco.onNodeWithText(NOME_APP).fetchSemanticsNode()
        assertNotNull(
            "Il titolo non è dichiarato come intestazione: TalkBack non ha dove fermarsi",
            nodo.config.getOrNull(SemanticsProperties.Heading)
        )
    }

    /**
     * **Un comando della finestra di rinomina si annuncia come pulsante.**
     *
     * ⚠️⚠️ **DEL BERSAGLIO A 48dp IL BANCO NON PUÒ DIRE NIENTE, ED È MISURATO RIMETTENDO IL
     * DIFETTO**: togliendo `tapRoom()` da `Quiet` questa prova continuava a passare, perché in
     * Robolectric il misuratore di testo è finto (dà un pixel per carattere a qualunque corpo, e
     * un'altezza di riga che non è quella del telefono), quindi il nodo arrivava a 48 comunque.
     * Una prova che non vede quello che cerca è peggio del niente, e quell'asserzione è stata
     * tolta invece di essere lasciata in verde: il bersaglio resta una cosa che si misura sul
     * telefono, e il perché della forma vive su `TAP_MIN` in `Talk.kt`.
     * ⚠️ **Il ruolo invece è un dato e si misura**: la prova fallisce se qualcuno lo toglie di
     * nuovo.
     */
    @Test
    fun `un comando della rinomina si annuncia come pulsante`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                RenameDialog(uris = finti(3), onDismiss = {}, onRename = { _, _, _ -> })
            }
        }
        banco.waitUntil(ATTESA) {
            banco.onAllNodesWithText(app.getString(R.string.save_name_date))
                .fetchSemanticsNodes().isNotEmpty()
        }
        val nodo = banco
            .onNode(hasText(app.getString(R.string.save_name_date)) and hasClickAction())
            .fetchSemanticsNode()
        assertEquals(
            "Il comando non si annuncia come pulsante",
            Role.Button,
            nodo.config.getOrNull(SemanticsProperties.Role)
        )
    }

    /**
     * **Le due pastiglie della posizione sono una scelta sola, e ognuna dice se è la scelta.**
     *
     * ⚠️⚠️ **IL RUOLO DI SERIE È QUELLO SBAGLIATO, e viene dal COMPONENTE**: un `FilterChip` di
     * Material si scrive addosso `Role.Checkbox`, cioè si annuncia come una casella che si
     * spunta da sola, mentre queste due sono una scelta a risposta unica. Le sei chiamate
     * dell'app erano tutte così.
     * ⚠️ **Si misura il ruolo E lo stato**: il ruolo dice che genere di scelta è, `selected`
     * dice quale delle due è in vigore, e senza il secondo un lettore di schermo legge due voci
     * indistinguibili.
     */
    @Test
    fun `le pastiglie della posizione sono una scelta sola`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                InfoSideRow(selected = InfoPosition.BOTTOM, enabled = true, onSelect = {})
            }
        }
        banco.waitForIdle()
        val scelte = InfoPosition.entries.map { side ->
            banco.onNodeWithText(nomeDel(side)).fetchSemanticsNode()
        }
        scelte.forEach { nodo ->
            assertEquals(
                "Una pastiglia della posizione si annuncia come casella invece che come scelta",
                Role.RadioButton,
                nodo.config.getOrNull(SemanticsProperties.Role)
            )
        }
        assertEquals(
            "Fra le due pastiglie nessuna dice di essere quella scelta, o lo dicono entrambe",
            1,
            scelte.count { it.config.getOrNull(SemanticsProperties.Selected) == true }
        )
    }

    /**
     * **Le date della cronologia sul tablet sono una scelta sola, e dicono quale è scelta**
     * (rilievo C26).
     *
     * ⚠️ Prima erano testi toccabili: la data scelta si distingueva solo dal colore, e un lettore
     * di schermo leggeva tre voci uguali.
     */
    @Test
    fun `le date della cronologia sono una scelta sola`() {
        val gruppi = listOf(1_000L, 2_000L, 3_000L).map { History.Batch(it, emptyList()) }
        banco.setContent {
            AivTheme(darkTheme = false) {
                HistoryDateRail(groups = gruppi, selected = 2_000L, onPick = {}, width = 240.dp)
            }
        }
        banco.waitForIdle()
        unaSceltaSola(gruppi.size, "le date della cronologia")
    }

    /**
     * **L'indice delle impostazioni sul tablet è una scelta sola, e dice quale pagina è aperta**
     * (rilievo F25).
     */
    @Test
    fun `l'indice delle impostazioni e una scelta sola`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                SettingsIndexRail(selected = Page.LOOK, onPick = {}, width = 240.dp)
            }
        }
        banco.waitForIdle()
        unaSceltaSola(SETTINGS_INDEX.size, "le voci dell'indice delle impostazioni")
    }

    /**
     * **Nel riordino dei tasti la prima cella non offre 'sposta prima', e l'ultima non offre
     * 'sposta dopo'** (rilievo F37).
     *
     * ⚠️ Prima le due azioni c'erano su ogni cella: ai due capi l'azione rispondeva 'fatto',
     * scriveva le impostazioni uguali e non spostava niente.
     */
    @Test
    fun `il riordino dei tasti offre solo le azioni che spostano`() {
        val ordine = PadKey.entries.take(3)
        banco.setContent {
            AivTheme(darkTheme = false) {
                PadArrange(order = ordine, columns = 3, onOrder = {})
            }
        }
        banco.waitForIdle()
        val prima = app.getString(R.string.settings_buttons_before)
        val dopo = app.getString(R.string.settings_buttons_after)
        val celle = banco.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
            .fetchSemanticsNodes()
        assertEquals("Le celle del riordino non sono quelle attese", ordine.size, celle.size)
        val azioni = celle.map { nodo ->
            nodo.config[SemanticsActions.CustomActions].map { it.label }
        }
        assertEquals("La prima cella offre di andare prima, che non sposta niente", listOf(dopo), azioni.first())
        assertEquals("Una cella in mezzo deve offrire le due azioni", listOf(prima, dopo), azioni[1])
        assertEquals("L'ultima cella offre di andare dopo, che non sposta niente", listOf(prima), azioni.last())
    }

    /**
     * **Il tasto grande dei filmati si annuncia come pulsante** (rilievo C39).
     */
    @Test
    fun `il tasto del filmato si annuncia come pulsante`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box {
                    ClipKeys(playing = false, onPlayPause = {}, position = 0L, duration = 10_000L, onSeek = {})
                }
            }
        }
        banco.waitForIdle()
        val nodo = banco.onNodeWithContentDescription(app.getString(R.string.clip_play)).fetchSemanticsNode()
        assertEquals(
            "Il tasto di riproduzione non si annuncia come pulsante",
            Role.Button,
            nodo.config.getOrNull(SemanticsProperties.Role)
        )
    }

    /** Che i nodi con il ruolo di una scelta siano [quanti], e che uno solo sia quello scelto. */
    private fun unaSceltaSola(quanti: Int, che: String) {
        val scelte = banco
            .onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .fetchSemanticsNodes()
        assertEquals("Non tutte $che si annunciano come voci di una scelta", quanti, scelte.size)
        assertEquals(
            "Fra $che nessuna dice di essere quella scelta, o lo dicono in più",
            1,
            scelte.count { it.config.getOrNull(SemanticsProperties.Selected) == true }
        )
    }

    /**
     * Il nome di un lato, chiesto alle risorse come fa la riga vera.
     *
     * ⚠️ Le due chiavi sono quelle di `infoSideName`, che è la funzione dell'app: scrivere qui
     * due stringhe qualunque farebbe una prova che non trova le pastiglie il giorno che quei
     * testi cambiano.
     */
    private fun nomeDel(side: InfoPosition): String = app.getString(
        when (side) {
            InfoPosition.TOP -> R.string.settings_top
            InfoPosition.BOTTOM -> R.string.settings_bottom
        }
    )

    private fun finti(quanti: Int): List<Uri> = (1..quanti).map { quale ->
        "content://media/external/images/media/$quale".let(Uri::parse)
    }

    private companion object {
        /**
         * Il nome dell'app come lo scrive l'intestazione.
         *
         * ⚠️ **Non è una stringa di risorsa**, ed è una scelta dell'app: il nome del prodotto
         * non si traduce, quindi `Identity` lo scrive come letterale. Qui si ripete per la
         * stessa ragione, e se un domani diventasse una risorsa questa prova non lo troverebbe
         * più, cioè lo direbbe subito.
         */
        const val NOME_APP = "Astonishing Image Viewer"
    }
}

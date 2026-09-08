package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
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
 * esattamente quello che il banco vede (`AIV/CLAUDE.md` § '🧪 Quando si scrive una prova, e
 * quando no').
 *
 * ⚠️ **Che cosa NON prova**: che TalkBack pronunci una frase sensata, che è la resa vera e
 * dipende dal servizio e dalla lingua del telefono. Quello che si misura è l'albero semantico,
 * che è un dato.
 *
 * ⚠️ **Le tre scene sono quelle VERE**, come vuole la lezione di `CambioSchermataTest`: la
 * schermata iniziale, la finestra di rinomina e la riga della barra delle info. Un albero finto
 * scritto qui accanto misurerebbe l'albero finto.
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
        /** Quanto si aspetta che una finestra si apra, in millisecondi. */
        const val ATTESA = 5_000L

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

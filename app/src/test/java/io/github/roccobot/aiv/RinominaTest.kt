package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Il banco di prova della **finestra di rinomina in blocco**.
 *
 * ⚠️⚠️ **NASCE CON LA RISPOSTA A `d-data-blocco`, E LA PROVA TORNA CON LA CORREZIONE**
 * (`CLAUDE.md`, § '🧪 Quando si scrive una prova, e quando no'): fino alla `1.79` il tocco lungo
 * su 'Data' rifaceva il template con la sola data, cioè un nome uguale per tutti i file, e
 * 'Rinomina' restava **spento** finché non si aggiungeva un cancelletto a mano. Era una domanda
 * aperta e lui l'ha chiusa (*con più file, scrivi AAAAMMDD più uno spazio seguito da un numero
 * di cancelletti adeguato alla dimensione del set*).
 *
 * ⚠️⚠️ **I FILE SONO VERI, e non è un vezzo**: `FileTree.namesOf` passa da `FileTree.fileOf`,
 * che di un indirizzo `file://` tiene solo quello che sul disco **è un file**. Con indirizzi
 * finti la finestra si aprirebbe con l'elenco vuoto, cioè misurerebbe il caso in cui i nomi non
 * si leggono, che è il ramo che qui non interessa.
 *
 * ⚠️ **Che cosa NON vede**: che i file si rinominino davvero, che è lavoro su disco fatto da
 * `FileTree`, e come il pannello si **percepisce**. Vede quello che la finestra propone e che
 * cosa consegna al tasto di conferma.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [OmbraArchivio::class])
class RinominaTest {

    @get:Rule
    val banco = createComposeRule()

    @get:Rule
    val cartella = TemporaryFolder()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * **Col tocco lungo sulla data il template porta i cancelletti, e 'Rinomina' resta acceso.**
     *
     * ⚠️⚠️ **LE DUE ASSERZIONI SONO UNA COPPIA**: quella sul testo dice che il template è quello
     * che lui ha chiesto, quella sul tasto dice che la finestra è **usabile** dopo quel gesto, ed
     * è la metà che era rotta. Una sola delle due passerebbe anche col difetto: il tasto è già
     * acceso all'apertura, perché il template proposto ha i suoi cancelletti.
     * ⚠️ **Il numero di cancelletti si conta e la data non si confronta**: si guarda la forma
     * (otto cifre, uno spazio, due cancelletti), che è la specifica alla lettera. Confrontare
     * con una data calcolata qui vorrebbe dire ricopiare l'implementazione, e a mezzanotte
     * sarebbe anche una prova che sbaglia da sé.
     */
    @Test
    fun `la data in blocco lascia i cancelletti e Rinomina acceso`() {
        val indirizzi = finti(50)
        banco.setContent {
            AivTheme(darkTheme = false) {
                RenameDialog(uris = indirizzi, onDismiss = {}, onRename = { _, _, _ -> })
            }
        }
        // ⚠️ Si aspetta la casella del primo numero e non un tempo: quella c'è solo con più di
        // un file, quindi la sua comparsa dice due cose insieme, che la finestra si è aperta e
        // che si è aperta in modo blocco. `namesOf` passa da un thread di I/O, che il banco non
        // conta come lavoro in corso: aspettare 'quando è fermo' non basterebbe.
        banco.waitUntil(ATTESA) {
            banco.onAllNodesWithText(app.getString(R.string.rename_start))
                .fetchSemanticsNodes().isNotEmpty()
        }

        banco.onNodeWithText(app.getString(R.string.save_name_date))
            .performTouchInput { longClick() }
        banco.waitForIdle()

        val scritto = template()
        assertTrue(
            "Il template dopo il tocco lungo è '$scritto', non una data con due cancelletti",
            scritto.matches(Regex("""\d{8} #{2}"""))
        )
        banco.onNode(hasText(app.getString(R.string.pick_rename)) and hasClickAction())
            .assertIsEnabled()
    }

    /**
     * **I cancelletti sono quelli dei suoi due esempi, e il primo numero conta.**
     *
     * ⚠️⚠️ **È LA SUA SPECIFICA IN NUMERI** (*se sono 100 file, aggiunge 3 cancelletti; con 50
     * file aggiungi 2 cancelletti*), e non è una ricopiatura del conto: quello che si fissa qui
     * sono le due risposte che ha dato lui, più i due fatti che le governano ai bordi.
     * ⚠️ **Il minimo è due**, che è un'altra sua istruzione, del 2026-08-29: con un file solo la
     * rinomina in blocco non esiste, ma con due il numero più alto ha una cifra e i nomi si
     * ordinerebbero male.
     * ⚠️ **E il primo numero fa parte del conto**: cinquanta file che partono da 51 arrivano a
     * 100, cioè vogliono tre cifre. Senza questa riga, un conto fatto sul solo totale
     * passerebbe.
     */
    @Test
    fun `i cancelletti seguono la dimensione del set`() {
        assertEquals("Con 100 file i cancelletti non sono 3", 3, hashesFor(100, 1))
        assertEquals("Con 50 file i cancelletti non sono 2", 2, hashesFor(50, 1))
        assertEquals("Il minimo non è due", 2, hashesFor(2, 1))
        assertEquals("Il primo numero non entra nel conto", 3, hashesFor(50, 51))
    }

    /** Il testo della prima casella della finestra, cioè il template. */
    private fun template(): String =
        banco.onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode()
            .config[SemanticsProperties.EditableText].text

    /**
     * [quanti] file veri in una cartella temporanea, coi loro indirizzi.
     *
     * ⚠️ **I nomi sono numerati con tre cifre** perché l'ordine alfabetico e quello numerico
     * coincidano: qui non si misura l'ordinamento, e un elenco che arriva in un ordine diverso
     * da quello che si legge renderebbe difficile leggere un fallimento.
     */
    private fun finti(quanti: Int): List<Uri> = (1..quanti).map { quale ->
        val file = cartella.newFile("prova%03d.jpg".format(quale))
        Uri.fromFile(file)
    }
}

/**
 * Quanto si aspetta che la finestra si apra, in millisecondi.
 *
 * ⚠️ **Largo di proposito**: quello che deve fallire è una finestra che non si apre affatto, non
 * una macchina lenta. La lettura dei nomi passa da un thread di I/O e cinquanta file sono
 * cinquanta accessi al disco.
 */
private const val ATTESA = 5_000L

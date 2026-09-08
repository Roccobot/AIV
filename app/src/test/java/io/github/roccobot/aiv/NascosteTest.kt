package io.github.roccobot.aiv

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Il banco di prova di **'Mostra nascoste'**, la funzione nuova della `1.92`.
 *
 * ⚠️⚠️ **NASCE CON LA FUNZIONE E NON DOPO UN DIFETTO**, ed è la metà proattiva della regola
 * (`AIV/CLAUDE.md` § '🧪 Quando si scrive una prova, e quando no'): la voce **cambia quello che
 * la schermata mostra**, cioè tocca il filtro dell'elenco, e un filtro che smettesse di filtrare
 * farebbe comparire per sempre cartelle che l'utente ha nascosto apposta. Il codice può essere
 * valido e sbagliare verso.
 *
 * ⚠️⚠️ **CHE COSA NON VEDE, e va detto invece di lasciarlo credere**: il **minuto**. Il conto
 * alla rovescia e la notifica che lo chiude vivono nel modello (`ViewerViewModel.peek`), che qui
 * non c'è: il banco monta la schermata e le passa lo stato già deciso. Quindi la scadenza, il suo
 * avviso e l'Annulla che proroga si guardano sul telefono, che è quello che la voce di collaudo
 * chiede di fare.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ArchivioAperto::class])
class NascosteTest {

    @get:Rule
    val banco = createComposeRule()

    /** Come in `RientroTest`: senza, il velo dell'onboarding si mangia il primo gesto. */
    @Before
    fun onboardingGiaVisto() {
        runBlocking { Hint.COLUMNS.remember(ApplicationProvider.getApplicationContext()) }
    }

    /**
     * **Una cartella nascosta non è in scena, e col minuto acceso c'è, col suo segno.**
     *
     * ⚠️ **Le due metà sono la stessa misura al rovescio**: la prima dice che il filtro filtra,
     * la seconda che 'Mostra nascoste' lo sospende. Una sola delle due passerebbe anche con un
     * filtro inchiodato in una delle due posizioni.
     * ⚠️ **Il segno si cerca per il suo carattere**: è quello che lui ha chiesto (`∅`), e non
     * passa da una stringa tradotta, quindi la prova lo confronta con la costante dell'app.
     */
    @Test
    fun `la nascosta compare solo col minuto acceso, e porta il segno`() {
        /*
         * ⚠️ **Una scena sola con lo stato che cambia, e non due `setContent`**: la seconda
         * chiamata va in errore (*has already set content*), e questa forma è anche più fedele,
         * perché nell'app quello stato cambia sotto la stessa schermata.
         */
        var acceso by mutableStateOf(false)
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Casa(CARTELLE, hidden = NASCOSTE, peeking = acceso)
                }
            }
        }
        banco.waitForIdle()

        assertTrue(
            "La cartella nascosta è in scena senza che nessuno abbia acceso niente",
            banco.onAllNodesWithText(SEGRETA).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            "Le cartelle normali non ci sono: la scena non ha composto l'elenco",
            banco.onAllNodesWithText(NORMALE).fetchSemanticsNodes().isNotEmpty()
        )

        acceso = true
        banco.waitForIdle()

        assertTrue(
            "Col minuto acceso la cartella nascosta non compare",
            banco.onAllNodesWithText(SEGRETA).fetchSemanticsNodes().isNotEmpty()
        )
        assertEquals(
            "Il segno del prestito non è sulla sola cartella nascosta",
            1,
            banco.onAllNodesWithText(SEGNO).fetchSemanticsNodes().size
        )
    }

    /**
     * **La voce del menu nomina quello che il tocco fa, e cambia con lo stato.**
     *
     * ⚠️⚠️ **È LA REGOLA DELLE ALTRE VOCI DI QUESTO MENU, applicata a una riga che ha due
     * stati**: una riga di menu è una richiesta e non un indicatore, quindi con le nascoste in
     * scena deve leggersi 'Nascondi cartelle'. Rovesciarla è il difetto che questa prova prende,
     * e nessun compilatore lo vedrebbe.
     * ⚠️ **E senza cartelle nascoste la voce non c'è affatto**: accenderebbe un minuto in cui non
     * compare niente.
     */
    @Test
    fun `la voce dice quello che fa, e senza nascoste non c'è`() {
        var quale by mutableStateOf(false)
        var quali by mutableStateOf(NASCOSTE)
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Casa(CARTELLE, hidden = quali, peeking = quale)
                }
            }
        }
        banco.waitForIdle()

        apriIlMenu()
        assertTrue(
            "Con una cartella nascosta e il minuto spento la voce non dice '${app.getString(R.string.hub_peek)}'",
            banco.onAllNodesWithText(app.getString(R.string.hub_peek))
                .fetchSemanticsNodes().isNotEmpty()
        )

        quale = true
        banco.waitForIdle()
        assertTrue(
            "Col minuto acceso la voce non dice '${app.getString(R.string.hub_unpeek)}'",
            banco.onAllNodesWithText(app.getString(R.string.hub_unpeek))
                .fetchSemanticsNodes().isNotEmpty()
        )

        quali = emptySet()
        banco.waitForIdle()
        assertTrue(
            "Senza cartelle nascoste la voce è ancora nel menu",
            banco.onAllNodesWithText(app.getString(R.string.hub_unpeek))
                .fetchSemanticsNodes().isEmpty() &&
                banco.onAllNodesWithText(app.getString(R.string.hub_peek))
                    .fetchSemanticsNodes().isEmpty()
        )
    }

    /**
     * **Il tocco lungo su una cartella in prestito propone di mostrarla, non di nasconderla.**
     *
     * ⚠️⚠️ **È IL DIFETTO CHE HA TROVATO LUI** (2026-09-08: *la pressione lunga su una cartella
     * nascosta deve proporre il contrario, ovvero di renderla di nuovo visibile*), quindi la
     * correzione porta la sua prova, come prescrive `AIV/CLAUDE.md` § '🧪 Quando si scrive una
     * prova, e quando no'. Il codice era valido e il comando non faceva niente: nascondere una
     * cartella già nascosta.
     * ⚠️ **Si guarda il TITOLO e non il tasto**: il tasto del ripristino riusa la stringa del
     * pannello ('Mostra'), che compare anche altrove; il titolo è la frase che distingue i due
     * versi della stessa finestra.
     */
    @Test
    fun `il tocco lungo su una nascosta in prestito propone di mostrarla`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Casa(CARTELLE, hidden = NASCOSTE, peeking = true)
                }
            }
        }
        banco.waitForIdle()

        banco.onAllNodesWithText(SEGRETA)[0].performTouchInput { longClick() }
        banco.waitForIdle()

        assertTrue(
            "La finestra non propone di mostrare di nuovo la cartella in prestito",
            banco.onAllNodesWithText(app.getString(R.string.show_folder_title, SEGRETA))
                .fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            "La finestra propone ancora di nascondere una cartella già nascosta",
            banco.onAllNodesWithText(app.getString(R.string.hide_folder_title, SEGRETA))
                .fetchSemanticsNodes().isEmpty()
        )
    }

    /**
     * **Su una cartella normale la stessa finestra propone di nasconderla.**
     *
     * ⚠️ **È la metà che tiene onesta l'altra**: una finestra inchiodata sul ripristino
     * passerebbe la prova qui sopra e romperebbe il gesto di sempre.
     */
    @Test
    fun `il tocco lungo su una cartella normale propone di nasconderla`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Casa(CARTELLE, hidden = NASCOSTE, peeking = true)
                }
            }
        }
        banco.waitForIdle()

        banco.onAllNodesWithText(NORMALE)[0].performTouchInput { longClick() }
        banco.waitForIdle()

        assertTrue(
            "La finestra non propone di nascondere una cartella normale",
            banco.onAllNodesWithText(app.getString(R.string.hide_folder_title, NORMALE))
                .fetchSemanticsNodes().isNotEmpty()
        )
    }

    /** Apre il menu del FAB, che è l'unico modo per arrivare alla voce. */
    private fun apriIlMenu() {
        banco.onNodeWithContentDescription(app.getString(R.string.hub_open)).performClick()
        banco.waitForIdle()
    }

    private val app: Context get() = ApplicationProvider.getApplicationContext()
}

/** Le cartelle finte: una sola è nascosta, e le altre servono a dire che l'elenco c'è. */
private val CARTELLE = (1..4).map {
    Folder.Bucket(
        id = it.toLong(),
        name = if (it == 1) "Segreta" else "Cartella $it",
        pictures = 2,
        clips = 0,
        cover = null,
        path = if (it == 1) "/storage/emulated/0/Segreta" else "/storage/emulated/0/Foto$it"
    )
}

private val NASCOSTE = setOf("/storage/emulated/0/Segreta")
private const val SEGRETA = "Segreta"
private const val NORMALE = "Cartella 2"

/** Il segno del prestito, lo stesso carattere che disegna la cella. */
private const val SEGNO = "∅"

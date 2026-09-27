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
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    /**
     * **'Mostra' su una cartella dentro una nascosta toglie la voce che la copre, e il titolo la
     * nomina.**
     *
     * ⚠️⚠️ **È UN DIFETTO CHE C'ERA E CHE NESSUNO AVEVA VISTO, TROVATO SCRIVENDO LA `2.96`**: fino
     * alla `2.95` 'Mostra' toglieva la sola voce col percorso della cartella toccata, e su `Camera`
     * dentro una `DCIM` nascosta quella voce non c'è, quindi il tocco non faceva niente e la cartella
     * restava nascosta. Adesso le voci che la coprono arrivano tutte insieme, e il titolo nomina la
     * più in alto, che è quella che torna davvero con le sue sorelle.
     * ⚠️ **Controprovata** rimettendo la voce col percorso della cartella: il titolo dice 'Camera' e
     * la voce tolta non è `DCIM`, e la prova cade due volte.
     */
    @Test
    fun `mostrare una cartella dentro una nascosta toglie la voce che la copre`() {
        val tolte = mutableListOf<String>()
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Casa(DENTRO, hidden = setOf("DCIM"), peeking = true, onUnhide = { tolte += it })
                }
            }
        }
        banco.waitForIdle()

        banco.onAllNodesWithText("Camera")[0].performTouchInput { longClick() }
        banco.waitForIdle()

        assertTrue(
            "Il titolo non nomina la cartella che torna davvero",
            banco.onAllNodesWithText(app.getString(R.string.show_folder_title, "DCIM"))
                .fetchSemanticsNodes().isNotEmpty()
        )
        banco.onAllNodesWithText(app.getString(R.string.settings_hidden_show))[0].performClick()
        banco.waitForIdle()

        assertEquals("'Mostra' non ha tolto la voce che copre la cartella", listOf("DCIM"), tolte)
    }

    // ── I percorsi senza la radice del volume, dalla `2.96` ──────────────────

    /**
     * **La radice del volume se ne va, e il resto del percorso resta.**
     *
     * ⚠️⚠️ **È LA SUA RICHIESTA** (campo libero del giro della `2.93` e della `2.94`: *se nascondo
     * /DCIM/Temp da un dispositivo, dovrebbe essere nascosto anche su un altro dispositivo*): quello
     * che cambia da un telefono all'altro è la radice, cioè il numero dell'utente e l'identificativo di una scheda.
     * ⚠️ **Le forme che NON sono un volume restano intere**, ed è la metà che tiene onesta l'altra:
     * `emulated` senza il numero, un nome che comincia come `sdcard` e un percorso fuori da
     * `/storage`. Una radice riconosciuta troppo larga toglierebbe un pezzo che da un telefono
     * all'altro invece torna.
     * ⚠️ **Controprovata** togliendo il controllo in avanti e poi quello sul confine: cadono
     * `emulated/legacy` e `/sdcardx`.
     */
    @Test
    fun `la radice del volume se ne va e il resto del percorso resta`() {
        assertEquals("DCIM/Temp", portablePath("/storage/emulated/0/DCIM/Temp"))
        assertEquals("il profilo di lavoro", "DCIM", portablePath("/storage/emulated/10/DCIM"))
        assertEquals("DCIM/Temp", portablePath("/sdcard/DCIM/Temp"))
        assertEquals("DCIM", portablePath("/mnt/sdcard/DCIM"))
        assertEquals("Pictures", portablePath("/storage/self/primary/Pictures"))
        assertEquals("una scheda", "Scan", portablePath("/storage/1234-5678/Scan"))
        assertEquals("la radice del volume", "", portablePath("/storage/emulated/0"))
        assertEquals(
            "emulated senza il numero non è un volume",
            "/storage/emulated/legacy/X",
            portablePath("/storage/emulated/legacy/X")
        )
        assertEquals("un nome che comincia come sdcard", "/sdcardx/DCIM", portablePath("/sdcardx/DCIM"))
        assertEquals("fuori da un volume", "/data/local/tmp", portablePath("/data/local/tmp"))
    }

    /**
     * **Una voce nasconde la cartella e quello che ha dentro, su ogni volume, e niente di più.**
     *
     * ⚠️⚠️ **SU OGNI VOLUME È LA CONSEGUENZA DICHIARATA DELLA SUA RICHIESTA**: `DCIM/Temp` nasconde
     * quella cartella sull'archivio del telefono e sulla scheda, perché l'identificativo della scheda
     * è proprio la parte che non si porta su un altro telefono.
     * ⚠️ **Il separatore resta il confine**, come dalla `0.84`: `Temp2` è un'altra cartella, e il
     * genitore di una cartella nascosta non è nascosto.
     * ⚠️ **La radice copre solo se stessa**: con la barra dopo, la voce vuota coprirebbe ogni
     * percorso fuori da un volume, che comincia proprio con la barra. **Una voce scritta intera**
     * copre solo quel percorso, e non lo stesso nome dentro un volume.
     * ⚠️ **Controprovata due volte**: togliendo la guardia della voce vuota cade la riga del percorso
     * fuori da un volume, e togliendo la barra dal confronto cade quella del separatore.
     */
    @Test
    fun `una voce nasconde la cartella e quello che ha dentro su ogni volume`() {
        val voce = setOf("DCIM/Temp")
        assertTrue(hiddenIn(voce, "/storage/emulated/0/DCIM/Temp"))
        assertTrue("sulla scheda", hiddenIn(voce, "/storage/1234-5678/DCIM/Temp"))
        assertTrue("dentro", hiddenIn(voce, "/storage/emulated/0/DCIM/Temp/Vecchie"))
        assertFalse("il separatore", hiddenIn(voce, "/storage/emulated/0/DCIM/Temp2"))
        assertFalse("il genitore", hiddenIn(voce, "/storage/emulated/0/DCIM"))

        assertTrue("la radice", hiddenIn(setOf(""), "/storage/emulated/0"))
        assertFalse("la radice copre solo se stessa", hiddenIn(setOf(""), "/storage/emulated/0/DCIM"))
        assertFalse("la radice non copre un percorso fuori da un volume", hiddenIn(setOf(""), "/data/x"))

        assertTrue("una voce assoluta", hiddenIn(setOf("/data/x"), "/data/x/y"))
        assertFalse(
            "una voce assoluta non vale su un volume",
            hiddenIn(setOf("/data/x"), "/storage/emulated/0/data/x")
        )

        assertEquals(
            "le voci che coprono una cartella sono tutte quelle sulla sua strada",
            setOf("DCIM", "DCIM/Camera"),
            coveringOf(setOf("DCIM", "DCIM/Camera", "Musica"), "/storage/emulated/0/DCIM/Camera").toSet()
        )
        assertTrue(
            "il segno della vista ad albero va sulla voce",
            listedIn(setOf("DCIM"), "/storage/emulated/0/DCIM")
        )
        assertFalse(
            "e non su quello che la voce copre",
            listedIn(setOf("DCIM"), "/storage/emulated/0/DCIM/Camera")
        )
    }

    /**
     * **Una voce si legge col nome davanti e col percorso con la barra, come l'ha scritto lui.**
     *
     * ⚠️ **La radice ha per nome e per percorso la stessa barra**, ed è la ragione per cui il
     * pannello e la pagina delle impostazioni ne scrivono una riga sola.
     */
    @Test
    fun `una voce si legge col nome e col percorso dalla radice`() {
        assertEquals("Temp", hiddenName("DCIM/Temp"))
        assertEquals("/DCIM/Temp", hiddenShown("DCIM/Temp"))
        assertEquals("/data/x", hiddenShown("/data/x"))
        assertEquals("/", hiddenName(""))
        assertEquals(hiddenName(""), hiddenShown(""))
    }

    /**
     * **Chi aggiorna ritrova le sue cartelle nascoste, scritte nella forma nuova.**
     *
     * ⚠️⚠️ **È LA SOLA COSA DELLA `2.96` CHE SI ROMPEREBBE IN SILENZIO SUL SUO TELEFONO**: senza la
     * migrazione la chiave vecchia resterebbe lì senza nessuno che la legge, e le cartelle che ha
     * nascosto tornerebbero tutte in vista al primo avvio. ⚠️ **E l'elenco nuovo si unisce e non si
     * sostituisce**, per la ragione scritta su [HiddenMigration].
     * ⚠️ **Controprovata** togliendo la traduzione dei percorsi e poi la rimozione della chiave
     * vecchia: cadono le due metà.
     */
    @Test
    fun `chi aggiorna ritrova le nascoste nella forma nuova`() = runTest {
        val prima = mutablePreferencesOf(
            HIDDEN_FOLDERS_ABSOLUTE to setOf(
                "/storage/emulated/0/DCIM/Temp",
                "/storage/1234-5678/Scan",
                "/data/x"
            ),
            HIDDEN_FOLDERS to setOf("Musica")
        )
        assertTrue("con la chiave vecchia la migrazione gira", HiddenMigration.shouldMigrate(prima))

        val dopo = HiddenMigration.migrate(prima)

        assertEquals(setOf("Musica", "DCIM/Temp", "Scan", "/data/x"), dopo[HIDDEN_FOLDERS])
        assertNull("la chiave vecchia se ne va", dopo[HIDDEN_FOLDERS_ABSOLUTE])
        assertFalse("tradotta, non gira una seconda volta", HiddenMigration.shouldMigrate(dopo))
        assertFalse("su un archivio senza la chiave vecchia non gira", HiddenMigration.shouldMigrate(emptyPreferences()))
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

/**
 * L'elenco delle nascoste, scritto come lo scrive l'app dalla `2.96`: senza la radice del volume.
 *
 * ⚠️ **Fino alla `2.95` qui c'era il percorso intero**, e la prova sarebbe restata verde anche col
 * confronto sbagliato, perché i due erano scritti nella stessa forma.
 */
private val NASCOSTE = setOf("Segreta")
private const val SEGRETA = "Segreta"

/** Una cartella dentro una nascosta: `Camera` in `DCIM`, e accanto una normale. */
private val DENTRO = listOf(
    Folder.Bucket(id = 11L, name = "Camera", pictures = 2, clips = 0, cover = null, path = "/storage/emulated/0/DCIM/Camera"),
    Folder.Bucket(id = 12L, name = "Musica", pictures = 2, clips = 0, cover = null, path = "/storage/emulated/0/Musica")
)
private const val NORMALE = "Cartella 2"

/** Il segno del prestito, lo stesso carattere che disegna la cella. */
private const val SEGNO = "∅"

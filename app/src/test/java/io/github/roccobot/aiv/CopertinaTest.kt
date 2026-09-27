package io.github.roccobot.aiv

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Il banco di prova della **copertina scelta a mano**, nata nella `1.94`.
 *
 * ⚠️⚠️ **NASCE CON LA FUNZIONE E NON DOPO UN DIFETTO, per la metà proattiva della regola**
 * (`CLAUDE.md`, § '🧪 Quando si scrive una prova, e quando no'): il gesto che la accende vive su
 * un nodo che porta **già** un altro gesto (il tocco lungo che sceglie il colore), cioè uno dei
 * casi in cui il codice può essere valido e non fare niente. Un `onTap` che non arriva non dà
 * nessun errore né al build né a schermo.
 *
 * ⚠️ **Che cosa NON vede**: la copia dell'immagine in casa, che decodifica e riscrive un file, e
 * quindi vuole un'immagine vera e un archivio vero. Quella si prova sul telefono, ed è dichiarato
 * nella voce di collaudo.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [OmbraArchivio::class])
class CopertinaTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **Il tocco sull'icona dell'intestazione comincia a scegliere la copertina.**
     *
     * ⚠️⚠️ **È LA SUA SPECIFICA ALLA LETTERA** (risposta a `d-copertina-come`, giro della `1.92`:
     * *solo con il tocco singolo sull'icona dell'intestazione di una cartella*), ed è il gesto che
     * dalla `1.86` era spento aspettando *un'azione alternativa realmente utile*.
     * ⚠️ **La seconda metà tiene onesta la prima**: sullo stesso nodo vive il tocco lungo che
     * sceglie il colore, e un riconoscitore scritto male li confonde. Senza questa riga, un
     * `onTap` che rubasse anche il gesto lungo passerebbe.
     */
    @Test
    fun `il tocco sull'icona comincia a scegliere la copertina`() {
        var chiesto = 0
        banco.setContent { Scena(onCoverPick = { chiesto++ }) }
        banco.waitForIdle()

        val icona = app.getString(R.string.folder_cover)
        banco.onNodeWithContentDescription(icona).performClick()
        banco.waitForIdle()

        assertEquals("Il tocco sull'icona non ha cominciato la scelta", 1, chiesto)
        assertTrue(
            "Il tocco ha aperto il selettore del colore invece della scelta",
            banco.onAllNodesWithText(app.getString(R.string.front_tint)).fetchSemanticsNodes()
                .isEmpty()
        )

        banco.onNodeWithContentDescription(icona).performTouchInput { longClick() }
        banco.waitForIdle()

        assertEquals("Il tocco lungo ha cominciato una scelta: i due gesti si confondono", 1, chiesto)
        assertTrue(
            "Il tocco lungo non apre più il selettore del colore",
            banco.onAllNodesWithText(app.getString(R.string.front_tint)).fetchSemanticsNodes()
                .isNotEmpty()
        )
    }

    /**
     * **Senza una copertina scelta, il menu del FAB non offre di toglierla.**
     *
     * ⚠️ **È lo stesso criterio di 'Mostra nascoste'**: una voce che agisce su una cosa che non
     * c'è è una riga che non fa niente. Le due prove servono insieme, e questa da sola passerebbe
     * anche con la voce tolta del tutto.
     */
    @Test
    fun `senza copertina scelta il menu non offre di toglierla`() {
        banco.setContent { Scena(coverSet = false) }
        banco.waitForIdle()
        apriIlMenu()

        assertTrue(
            "Il menu offre di togliere una copertina che non c'è",
            banco.onAllNodesWithText(app.getString(R.string.folder_cover_auto))
                .fetchSemanticsNodes().isEmpty()
        )
    }

    /**
     * **La voce del menu segue [COVER_MENU_ROW], e quando c'è riporta alla copertina
     * predefinita.**
     *
     * ⚠️⚠️ **DALLA `1.95` QUELL'INTERRUTTORE È SPENTO, ED È SUA ISTRUZIONE** (*spegni la
     * funzionalità del FAB senza eliminarla, in caso cambiassi idea, ma rinomina la voce in
     * `Copertina predefinita`*): con la voce fuori scena, la prova di prima chiedeva una riga
     * che l'utente ha tolto, quindi è **la prova** a essere cambiata, e la ragione è questa.
     * ⚠️⚠️ **MA NON SI È RIDOTTA A 'la voce non c'è'**, che sarebbe una prova contro il giorno in
     * cui lui cambia idea: misura il **legame** fra l'interruttore e quello che si vede, quindi
     * regge in tutti e due gli stati, e la seconda metà (il tocco che toglie davvero la
     * copertina) torna a girare da sé appena la voce rientra.
     */
    @Test
    fun `la voce del menu segue il suo interruttore`() {
        var tolta = 0
        banco.setContent { Scena(coverSet = true, onCoverClear = { tolta++ }) }
        banco.waitForIdle()
        apriIlMenu()

        val voce = app.getString(R.string.folder_cover_auto)
        val inScena = banco.onAllNodesWithText(voce).fetchSemanticsNodes().isNotEmpty()
        assertEquals(
            "La voce del menu non segue COVER_MENU_ROW",
            COVER_MENU_ROW,
            inScena
        )
        if (!inScena) return

        banco.onNodeWithText(voce).performClick()
        banco.waitForIdle()

        assertEquals("La voce non ha tolto la copertina", 1, tolta)
    }

    /**
     * **La fascia dell'invito nomina la cartella, e il suo tasto lascia perdere.**
     *
     * ⚠️ **Il nome è un dato dentro una frase**, quindi si compone dalla stessa risorsa che la
     * fascia usa: scriverlo qui vorrebbe dire una prova vera in italiano e falsa nelle altre
     * ventisette lingue.
     */
    @Test
    fun `la fascia dell'invito nomina la cartella e si può lasciar perdere`() {
        var basta = 0
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CoverInvite(name = CARTELLA, onCancel = { basta++ })
                }
            }
        }
        banco.waitForIdle()

        val invito = app.getString(R.string.folder_cover_pick, CARTELLA)
        assertTrue(
            "La fascia non dice di quale cartella si sta scegliendo la copertina",
            banco.onAllNodesWithText(invito).fetchSemanticsNodes().isNotEmpty()
        )

        banco.onNodeWithText(app.getString(R.string.cancel)).performClick()
        banco.waitForIdle()

        assertEquals("Il tasto della fascia non lascia perdere la scelta", 1, basta)
    }

    /**
     * **La copertina scelta vince su quella automatica, e senza scelta resta l'automatica.**
     *
     * ⚠️⚠️ **NON È RISCRIVERE IL CODICE IN UNA PROVA, ed è la distinzione della regola**: quello
     * che si misura è la **precedenza**, cioè una decisione (una scelta a mano non deve cadere
     * alla prossima fotografia aggiunta alla cartella), non la riga che la esprime.
     */
    @Test
    fun `la copertina scelta vince su quella automatica`() {
        val cartella = Folder.Bucket(
            id = 7L,
            name = CARTELLA,
            pictures = 3,
            clips = 0,
            cover = AUTOMATICA,
            path = "/storage/emulated/0/Prova"
        )

        assertEquals(
            "Senza una scelta a mano non si mostra la copertina automatica",
            AUTOMATICA,
            cartella.coverIn(emptyMap())
        )
        assertEquals(
            "La copertina scelta a mano non vince su quella automatica",
            SCELTA,
            cartella.coverIn(mapOf(7L to SCELTA))
        )
        assertEquals(
            "La copertina di un'altra cartella finisce su questa",
            AUTOMATICA,
            cartella.coverIn(mapOf(8L to SCELTA))
        )
        assertNull(
            "Una cartella senza immagini e senza scelta dovrebbe restare senza copertina",
            cartella.copy(cover = null).coverIn(emptyMap())
        )
        assertFalse(
            "Le due copertine sono lo stesso indirizzo: la prova non distingue niente",
            AUTOMATICA == SCELTA
        )
    }

    // ── Dalla cartella all'elenco iniziale, dalla `2.96` ─────────────────────

    /**
     * **Con il mini-onboarding già visto, il tocco sull'icona porta all'elenco iniziale.**
     *
     * ⚠️⚠️ **È LA SUA RICHIESTA** (campo libero del giro della `2.93` e della `2.94`: *quando si
     * tocca l'icona dell'intestazione, la vista deve tornare sulla cartella root (elenco delle
     * cartelle iniziale) per facilitare la selezione da qualsiasi percorso*). Qui si misura che la griglia lo chieda;
     * che il modello ci porti davvero lo misura il caso del modello, più sotto.
     * ⚠️ **Controprovata** togliendo la chiamata dal tocco: la griglia non chiede di uscire, e la
     * prova cade.
     */
    @Test
    fun `col mini-onboarding gia visto il tocco sull'icona porta all'elenco`() {
        runBlocking { Hint.COVER.remember(app) }
        var chiesto = 0
        var via = 0
        banco.setContent { Scena(onCoverPick = { chiesto++ }, onCoverAway = { via++ }) }
        banco.waitForIdle()

        banco.onNodeWithContentDescription(app.getString(R.string.folder_cover)).performClick()
        banco.waitForIdle()

        assertEquals("Il tocco sull'icona non ha cominciato la scelta", 1, chiesto)
        assertEquals("Il tocco sull'icona non porta all'elenco iniziale", 1, via)
    }

    /**
     * **La prima volta si esce quando il mini-onboarding si chiude, e non prima.**
     *
     * ⚠️⚠️ **IL VELO INDICA L'ICONA DI QUESTA CARTELLA**, quindi uscendo col tocco stesso il
     * mini-onboarding comparirebbe su una schermata che non c'è più, cioè non lo vedrebbe nessuno.
     * Le due metà sono le due cose che possono andare storte: uscire subito, e non uscire affatto.
     * ⚠️ **La scena accende `coverHere` quando la scelta parte**, che è quello che fa il modello: col
     * velo in scena fin dall'inizio, il primo tocco lo chiuderebbe invece di arrivare all'icona.
     * ⚠️ **Controprovata due volte**: uscendo sempre col tocco cade la prima metà, e senza l'uscita
     * alla chiusura del velo cade la seconda.
     */
    @Test
    fun `la prima volta si esce quando il mini-onboarding si chiude`() {
        runBlocking { Hint.COVER.forget(app) }
        var qui by mutableStateOf(false)
        var via = 0
        banco.setContent {
            Scena(onCoverPick = { qui = true }, onCoverAway = { via++ }, coverHere = qui)
        }
        banco.waitForIdle()

        banco.onNodeWithContentDescription(app.getString(R.string.folder_cover)).performClick()
        banco.waitForIdle()

        val velo = app.getString(R.string.hint_cover)
        assertTrue(
            "Il mini-onboarding della copertina non è comparso",
            banco.onAllNodesWithText(velo).fetchSemanticsNodes().isNotEmpty()
        )
        assertEquals("Si esce prima che lui abbia letto il mini-onboarding", 0, via)

        banco.onNodeWithText(velo).performClick()
        banco.waitForIdle()

        assertEquals("Chiuso il mini-onboarding non si torna all'elenco iniziale", 1, via)
    }

    /**
     * **Il modello porta all'elenco iniziale solo se una scelta è partita.**
     *
     * ⚠️⚠️ **IL SECONDO TOCCO SULLA STESSA CARTELLA NON PARTE, RIMETTE LA COPERTINA PREDEFINITA**
     * (dalla `1.95`), e allora si resta nella cartella: uscire dopo aver tolto una copertina
     * sarebbe un salto che nessuno ha chiesto. È la ragione per cui `coverAway` guarda `covering`.
     * ⚠️ **Controprovata** togliendo quella condizione: il secondo tocco porta fuori, e la prova
     * cade.
     */
    @Test
    fun `il modello esce solo se una scelta e partita`() {
        val model = ViewerViewModel(ApplicationProvider.getApplicationContext<Application>())
        model.openGrid(7L, CARTELLA)
        model.startCover()
        model.coverAway()
        assertEquals("La scelta partita non porta all'elenco iniziale", Screen.Folders(forStart = false), model.screen)
        assertEquals("La scelta è partita per un'altra cartella", 7L, model.covering?.bucket)

        model.openGrid(7L, CARTELLA)
        model.startCover()
        model.coverAway()
        assertNull("Il secondo tocco sulla stessa cartella non chiude la scelta", model.covering)
        assertEquals("Tolta la copertina, si è usciti dalla cartella", Screen.Grid(7L, CARTELLA), model.screen)
    }

    /** Apre il menu del FAB, che è l'unico modo per arrivare alla voce. */
    private fun apriIlMenu() {
        banco.onNodeWithContentDescription(app.getString(R.string.pick_actions)).performClick()
        banco.waitForIdle()
    }

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    @Composable
    private fun Scena(
        onCoverPick: () -> Unit = {},
        onCoverClear: () -> Unit = {},
        coverSet: Boolean = false,
        onCoverAway: () -> Unit = {},
        coverHere: Boolean = false
    ) {
        AivTheme(darkTheme = false) {
            Box(modifier = Modifier.fillMaxSize()) {
                GridScreen(
                    title = CARTELLA,
                    items = FOTO,
                    highlight = null,
                    onOpen = {},
                    onBack = {},
                    onChanged = {},
                    onSearch = {},
                    // ⚠️ Il menu del FAB c'è solo se ha almeno una voce da mostrare, e la voce
                    // della copertina non basta a farlo comparire: senza uno di questi due il
                    // FAB non si disegna affatto, e le due prove sul menu misurerebbero un
                    // pulsante che non c'è.
                    onBin = {},
                    onSettings = {},
                    onCoverPick = onCoverPick,
                    onCoverAway = onCoverAway,
                    onCoverClear = onCoverClear,
                    coverSet = coverSet,
                    coverHere = coverHere
                )
            }
        }
    }
}

/** Le immagini della cartella finta: bastano quelle che riempiono una schermata. */
private val FOTO = (1..12).map { Uri.parse("file:///finta/$it.jpg") }

/** Il nome della cartella finta, che l'intestazione scrive e la fascia nomina. */
private const val CARTELLA = "Cartella di prova"

/** La copertina che il MediaStore darebbe da sé: l'immagine più recente. */
private val AUTOMATICA: Uri = Uri.parse("content://media/external/images/media/42")

/** La copertina scelta a mano, cioè la copia che vive in casa dell'app. */
private val SCELTA: Uri = Uri.parse("file:///data/user/0/io.github.roccobot.aiv/files/covers/7-1.webp")

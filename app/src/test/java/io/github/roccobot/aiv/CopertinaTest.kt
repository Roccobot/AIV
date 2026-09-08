package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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

    /** **Con una copertina scelta, il menu del FAB offre di tornare a quella automatica.** */
    @Test
    fun `con una copertina scelta il menu offre di togliere`() {
        var tolta = 0
        banco.setContent { Scena(coverSet = true, onCoverClear = { tolta++ }) }
        banco.waitForIdle()
        apriIlMenu()

        val voce = app.getString(R.string.folder_cover_auto)
        assertTrue(
            "Il menu non offre di tornare alla copertina automatica",
            banco.onAllNodesWithText(voce).fetchSemanticsNodes().isNotEmpty()
        )

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
        coverSet: Boolean = false
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
                    onCoverClear = onCoverClear,
                    coverSet = coverSet
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

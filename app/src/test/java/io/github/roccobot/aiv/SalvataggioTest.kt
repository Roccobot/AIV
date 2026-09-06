package io.github.roccobot.aiv

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Il banco di prova della **finestra del nome**, nata con la tappa del salvataggio in Download.
 *
 * ⚠️⚠️ **ESISTE PER LA METÀ PROATTIVA DELLA REGOLA** (`CLAUDE.md`, § '🧪 Quando si scrive una
 * prova, e quando no'): una superficie che si **apre sopra** un'altra e deve decidere che cosa
 * fa il tocco fuori è uno dei tre casi che vogliono la prova anche senza un difetto alle
 * spalle. [SaveNameDialog] è quello, e in più promette una cosa che si può misurare: che il
 * suffisso non si tocca.
 *
 * ⚠️ **Che cosa NON vede**: che il file finisca davvero in Download, perché quello vuole un
 * `MediaStore` vero e un telefono. Vede quello che la finestra consegna a chi salva, che è il
 * confine fra le due metà.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [OmbraArchivio::class])
class SalvataggioTest {

    @get:Rule
    val banco = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * **Il nome ricomposto è identico all'intero, sempre.**
     *
     * ⚠️ **È l'invariante su cui poggia tutta la finestra**: quello che si batte è la prima
     * metà, e chi salva riattacca la seconda. Se la coppia perdesse o aggiungesse un carattere,
     * un nome lasciato com'era tornerebbe indietro diverso.
     */
    @Test
    fun `il nome spezzato e ricomposto non cambia`() {
        val casi = listOf(
            "foto.jpg", "IMG_20260906.jpeg", "senza suffisso", "archivio.tar.gz",
            ".nascosto", "punto.", "foto.2026"
        )
        for (intero in casi) {
            val (base, suffisso) = ImageActions.splitName(intero)
            assertEquals("Il nome ricomposto non è quello di partenza", intero, base + suffisso)
        }
    }

    /**
     * **Il suffisso è fuori dalla parte che si batte.**
     *
     * ⚠️ **La promessa della finestra è questa**, e senza la prova sarebbe una frase nel KDoc:
     * `foto.jpg` deve arrivare al campo come `foto`, o cancellando tutto si perderebbe anche
     * l'estensione.
     */
    @Test
    fun `il suffisso non entra nella parte che si batte`() {
        val (base, suffisso) = ImageActions.splitName("foto.jpg")
        assertEquals("foto", base)
        assertEquals(".jpg", suffisso)

        val (nudo, niente) = ImageActions.splitName("IMG_20260906")
        assertEquals("IMG_20260906", nudo)
        assertEquals("", niente)
    }

    /**
     * **La finestra mostra il nome senza suffisso, e il suffisso accanto.**
     *
     * ⚠️ **Le due asserzioni sono una coppia**: la prima dice che il campo porta la parte
     * giusta, la seconda che il suffisso non è sparito dagli occhi di chi guarda. Senza la
     * seconda, una finestra che butta via l'estensione passerebbe.
     */
    @Test
    fun `la finestra parte dal nome senza suffisso`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                SaveNameDialog(full = "foto.jpg", onDismiss = {}, onSave = {})
            }
        }

        banco.onNodeWithText("foto").assertExists()
        banco.onNodeWithText(".jpg").assertExists()
    }

    /**
     * **Il chip della data aggiunge otto cifre, e col tocco lungo rifà il nome da capo.**
     *
     * ⚠️⚠️ **I DUE GESTI SI PROVANO INSIEME PERCHÉ UNO SOLO NON DIREBBE NIENTE**: un chip che
     * sostituisce sempre passerebbe la prima prova (il nome è cambiato) e sarebbe il difetto.
     * Quello che si misura è la **differenza** fra i due, cioè che il breve aggiunge e il lungo
     * rifà.
     * ⚠️ **La data non si confronta con una data calcolata qui**, che sarebbe ricopiare
     * l'implementazione: si contano le cifre, che è quello che la specifica dice
     * (*inserisce `YYYYMMDD`*).
     */
    /*
     * ⚠️ **Il nome della prova non porta accenti, e non è una svista**: Gradle ricava da qui il
     * nome del file HTML del rapporto, e su una macchina con codifica di sistema stretta un
     * carattere accentato lo fa fallire con `Malformed input`, cioè un build rosso per una
     * lettera. È la stessa ragione per cui in `TocchiTest` c'è `proprieta` e in
     * `CambioSchermataTest` c'è `in piu`.
     */
    @Test
    fun `il chip della data aggiunge in coda e col tocco lungo rifa il nome`() {
        var consegnato: String? = null
        banco.setContent {
            AivTheme(darkTheme = false) {
                SaveNameDialog(full = "foto.jpg", onDismiss = {}, onSave = { consegnato = it })
            }
        }

        val data = app.getString(R.string.save_name_date)
        val salva = app.getString(R.string.editor_save)

        banco.onNodeWithText(data).performClick()
        banco.onNodeWithText(salva).performClick()

        val conData = consegnato
        assertTrue("Il chip non ha aggiunto niente", conData != null && conData.length > 4)
        assertTrue(
            "Il nome non comincia più con quello di partenza: il chip ha sostituito invece di aggiungere",
            conData!!.startsWith("foto")
        )
        assertEquals("La data aggiunta non è di otto cifre", 8, conData.length - "foto".length)
        assertTrue("La coda aggiunta non è fatta di cifre", conData.drop(4).all { it.isDigit() })

        consegnato = null
        banco.onNodeWithText(data).performTouchInput { longClick() }
        banco.onNodeWithText(salva).performClick()

        val soloData = consegnato
        assertTrue("Il tocco lungo non ha rifatto il nome", soloData != null)
        assertEquals("Il tocco lungo non ha lasciato le sole otto cifre", 8, soloData!!.length)
        assertTrue("Quello che resta non è una data", soloData.all { it.isDigit() })
    }
}

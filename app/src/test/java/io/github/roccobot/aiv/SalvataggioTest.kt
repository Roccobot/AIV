package io.github.roccobot.aiv

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
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
        banco.setContent { AivTheme(darkTheme = false) { Finestra() } }

        banco.onNodeWithText("foto").assertExists()
        banco.onNodeWithText(".jpg").assertExists()
    }

    /**
     * **I comandi sul nome ci sono, e 'Estensione' NO finché l'impostazione è spenta.**
     *
     * ⚠️⚠️ **QUESTO DIFETTO È ARRIVATO A LUI, e la prova torna con la correzione, nella stessa
     * versione** (`CLAUDE.md`, § '🧪 Quando si scrive una prova, e quando no'): la voce
     * `scarica-download` del giro della `1.77` non è stata approvata perché la finestra portava
     * la sola 'Data', mentre *esattamente come in 'Rinomina', devono esserci i tasti 'Seleziona
     * tutto' e 'Svuota'*.
     * ⚠️⚠️ **E L'ASSENZA DI 'Estensione' SI PROVA COME LA PRESENZA DEGLI ALTRI**: quel comando
     * esiste solo se l'impostazione è accesa, che è la griglia di sicurezza chiesta da lui, e di
     * fabbrica è spenta. La `1.78` ha spostato quel cancello in un pezzo condiviso: se un domani
     * il suo `allowed` diventasse un `true` scritto a mano, l'app non darebbe nessun errore e la
     * protezione sarebbe sparita in silenzio.
     * ⚠️⚠️ **DALLA `1.80` L'ASSENZA SI CERCA FRA LE DESCRIZIONI E NON FRA I TESTI, e prima
     * questa riga sarebbe passata misurando NIENTE**: 'Estensione' è diventata un'icona sulla
     * riga del titolo (voce `scarica-comandi` del giro della `1.79`), quindi il suo nome non è
     * più il testo di un nodo ma la descrizione parlata di uno. Cercarlo fra i testi darebbe
     * 'non c'è' anche con l'icona in scena, che è il modo tipico in cui una prova mente in
     * verde.
     */
    @Test
    fun `i comandi sul nome ci sono e l'estensione no`() {
        banco.setContent { AivTheme(darkTheme = false) { Finestra() } }

        banco.onNodeWithText(app.getString(R.string.rename_select_all)).assertExists()
        banco.onNodeWithText(app.getString(R.string.rename_clear)).assertExists()
        banco.onNodeWithText(app.getString(R.string.save_name_date)).assertExists()
        banco.onNodeWithContentDescription(app.getString(R.string.rename_ext))
            .assertDoesNotExist()
        banco.onNodeWithContentDescription(app.getString(R.string.save_name_path))
            .assertDoesNotExist()
    }

    /**
     * **Col tocco lungo i due comandi della riga del titolo ci sono tutti e due, e 'Percorso'
     * consegna il nome che si vede.**
     *
     * ⚠️⚠️ **È LA SUA RICHIESTA ALLA LETTERA** (campo libero del giro della `1.79`, punto D: *la
     * pressione lunga su 'Scarica' metterà a disposizione la finestra di download con entrambe
     * le icone-tasto attive ('Percorso', 'Estensione')*), e senza prova non ce l'avrebbe
     * nessuno: quei due comandi compaiono per un `force` e per un parametro non nullo, cioè per
     * due strade diverse che non danno nessun errore se una delle due non arriva.
     * ⚠️⚠️ **E CHE 'Percorso' CONSEGNI LA COPPIA GIUSTA È LA METÀ CHE FA DANNO**: un comando che
     * aprisse il selettore col nome intero, o col solo nome senza suffisso, salverebbe un file
     * che si chiama in un altro modo. Qui si guarda quello che la finestra passa a chi salva,
     * che è il confine fra le due metà dichiarato in testa a questa classe.
     * ⚠️ **L'ordine non si prova qui**: 'prima Percorso e poi Estensione' è una posizione, e
     * quello che il banco può dire è che i due nodi esistono. Il posto si vede.
     */
    @Test
    fun `col tocco lungo i due comandi del titolo ci sono tutti e due`() {
        var chiesto: Pair<String, String>? = null
        banco.setContent {
            AivTheme(darkTheme = false) {
                Finestra(hold = true, onPath = { nome, coda -> chiesto = nome to coda })
            }
        }

        val percorso = app.getString(R.string.save_name_path)
        banco.onNodeWithContentDescription(percorso).assertExists()
        banco.onNodeWithContentDescription(app.getString(R.string.rename_ext)).assertExists()

        banco.onNodeWithContentDescription(percorso).performClick()
        assertEquals("'Percorso' non ha consegnato il nome spezzato", "foto" to ".jpg", chiesto)
    }

    /**
     * **'Svuota' svuota davvero, e allora non c'è più niente da salvare.**
     *
     * ⚠️⚠️ **UN COMANDO PUÒ ESSERCI E NON FARE NIENTE, ed è la trappola che il banco esiste per
     * prendere**: [Quiet] mette il tocco su un nodo di testo, e un `enabled` sbagliato o un
     * gesto non collegato compilano senza una parola. Che il comando **compaia** lo prova la
     * prova qui sopra; che **agisca** lo prova questa.
     * ⚠️ **Si guarda il tasto di conferma e non il campo**: un campo vuoto si legge anche
     * cercando l'assenza di un testo, ma quello che conta per chi usa l'app è che 'Salva' si
     * spenga, cioè che la finestra non consegni un nome vuoto.
     */
    @Test
    fun `svuota lascia la finestra senza niente da salvare`() {
        banco.setContent { AivTheme(darkTheme = false) { Finestra() } }

        banco.onNodeWithText(app.getString(R.string.editor_save)).assertIsEnabled()
        banco.onNodeWithText(app.getString(R.string.rename_clear)).performClick()
        banco.onNodeWithText("foto").assertDoesNotExist()
        banco.onNodeWithText(app.getString(R.string.editor_save)).assertIsNotEnabled()
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
                Finestra(onSave = { nome, _ -> consegnato = nome })
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

    /**
     * La finestra con gli argomenti minimi, così una firma nuova si aggiorna in un posto solo.
     *
     * ⚠️ **I due valori di serie sono il caso di fabbrica**: nessun tocco lungo e nessun
     * 'Percorso', cioè quello che vede chi accende la sola rinomina al salvataggio. Le prove che
     * guardano l'altro caso lo dicono passando gli argomenti, e così si legge nella prova quale
     * dei due casi sta misurando.
     */
    @Composable
    private fun Finestra(
        hold: Boolean = false,
        onPath: ((String, String) -> Unit)? = null,
        onSave: (String, String) -> Unit = { _, _ -> }
    ) {
        SaveNameDialog(
            full = "foto.jpg",
            hold = hold,
            onPath = onPath,
            onDismiss = {},
            onSave = onSave
        )
    }
}

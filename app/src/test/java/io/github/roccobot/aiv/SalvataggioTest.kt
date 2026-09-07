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
     * ⚠️⚠️ **DALLA `1.81` I DUE COMANDI SI CERCANO DI NUOVO FRA I TESTI, PERCHÉ SONO TORNATI
     * PASTIGLIE** (voce `rinomina-icona` del giro della `1.80`: *Ho cambiato idea ... testuale,
     * tasto stondato a destra*). Nella sola `1.80` erano icone e il loro nome viveva nella
     * descrizione parlata: cercarlo nel posto sbagliato darebbe 'non c'è' anche col comando in
     * scena, che è il modo tipico in cui una prova mente in verde. Chi cambia di nuovo quella
     * forma guardi anche queste due righe.
     */
    @Test
    fun `i comandi sul nome ci sono e l'estensione no`() {
        banco.setContent { AivTheme(darkTheme = false) { Finestra() } }

        banco.onNodeWithText(app.getString(R.string.rename_select_all)).assertExists()
        banco.onNodeWithText(app.getString(R.string.rename_clear)).assertExists()
        banco.onNodeWithText(app.getString(R.string.save_name_date)).assertExists()
        banco.onNodeWithText(app.getString(R.string.rename_ext)).assertDoesNotExist()
        banco.onNodeWithText(app.getString(R.string.save_name_dest)).assertDoesNotExist()
    }

    /**
     * **Con un comando solo la riga del titolo porta una pastiglia col testo, e 'Destinazione' non
     * chiude la finestra.**
     *
     * ⚠️⚠️ **LA PROVA È CAMBIATA DUE VOLTE PERCHÉ LA SPECIFICA È CAMBIATA DUE VOLTE, E OGNI VOLTA
     * L'HA SCRITTA LUI.** Nella `1.81` il tocco lungo accendeva tutti e due i comandi; nella
     * `1.82` 'Estensione' seguiva la sola opzione (voce `save-comandi`) e col tocco lungo ne
     * restava **uno**; dalla `1.83` il tocco lungo è di nuovo la versione con tutto (voce
     * `save-quando` non approvata: *se si tiene premuto 'Scarica' ... i due tasti 'Destinazione'
     * ed 'Estensione' entrambi disponibili*). Non è una prova piegata per far passare un build:
     * è la riga che qui sopra dice quale specifica misura.
     * ⚠️⚠️ **QUINDI IL CASO 'UNO SOLO' NON È PIÙ IL TOCCO LUNGO: è il tocco normale con l'opzione
     * del percorso accesa**, che è la via per cui quel comando esiste da solo. Il caso 'tutti e
     * due' vive nella prova qui sotto, e i due vestiti sono diversi: pastiglia col testo contro
     * icona.
     * ⚠️⚠️ **E CHE LA FINESTRA RESTI APERTA È LA METÀ DELLA `1.81` CHE RESTA VERA** (voce
     * `scarica-percorso`: *Voglio solo SELEZIONARE la destinazione, non salvare*): quel comando
     * apre un selettore di cartella e quello che si era battuto deve essere ancora là al ritorno,
     * quindi la prova guarda che il campo del nome sia ancora in scena.
     */
    @Test
    fun `con un comando solo il titolo porta la pastiglia col testo`() {
        var aperto = 0
        banco.setContent {
            AivTheme(darkTheme = false) {
                Finestra(hold = false, onPickFolder = { aperto += 1 })
            }
        }

        val destinazione = app.getString(R.string.save_name_dest)
        banco.onNodeWithText(destinazione).assertExists()
        banco.onNodeWithText(app.getString(R.string.rename_ext)).assertDoesNotExist()

        banco.onNodeWithText(destinazione).performClick()
        assertEquals("'Destinazione' non ha aperto il selettore di cartella", 1, aperto)
        banco.onNodeWithText("foto").assertExists()
    }

    /**
     * **Col tocco lungo i comandi sono due, e allora diventano due icone.**
     *
     * ⚠️⚠️ **È LA VOCE `save-quando` DELLA `1.83`, CIOÈ UNA CHE NON AVEVA APPROVATO** (giro della
     * `1.82`: *qualunque sia lo stato di 'Consenti la rinomina al salvataggio', la pressione lunga
     * su 'Scarica' rende sempre disponibili sia 'Destinazione' che 'Estensione'*), quindi torna
     * qui con la prova che l'avrebbe fermata, come prescrive `AIV/CLAUDE.md` § '🧪 Quando si
     * scrive una prova, e quando no'.
     * ⚠️ **Le due icone si cercano per DESCRIZIONE e non per testo**: una pastiglia porta il suo
     * nome come testo, un'icona come descrizione parlata, e cercare nel posto sbagliato darebbe
     * 'non c'è' anche col comando in scena. È la stessa trappola dichiarata sulla prova sopra.
     * ⚠️ **La griglia di sicurezza dell'estensione resta chiusa**: qui si misura che il comando
     * **c'è**, non che il pannellino si apra senza avviso.
     */
    @Test
    fun `col tocco lungo i due comandi sono due icone`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                Finestra(hold = true, onPickFolder = {})
            }
        }

        val destinazione = app.getString(R.string.save_name_dest)
        val estensione = app.getString(R.string.rename_ext)
        banco.onNodeWithContentDescription(destinazione).assertExists()
        banco.onNodeWithContentDescription(estensione).assertExists()
        // ⚠️ Con due comandi in scena nessuno dei due è più una pastiglia col testo: è la
        // richiesta del punto C del campo libero della `1.81`, e senza questa riga la prova
        // passerebbe anche se tornassero tutti e due testuali.
        banco.onNodeWithText(destinazione).assertDoesNotExist()
    }

    /**
     * **Con una cartella scelta c'è la via del ritorno a Download.**
     *
     * ⚠️⚠️ **È IL VICOLO CIECO DELLA VOCE `save-percorso`** (giro della `1.81`: *se cambio
     * cartella di download, non posso più tornare a storage/emulated/0/Download. Il file picker mi
     * dice che 'per tutelare la mia privacy' non posso scegliere quella cartella*), quindi torna
     * qui con la prova che lo avrebbe fermato: il comando esiste, si tocca, e chiama chi lo scorda.
     * ⚠️ **Il caso opposto è nella prova della riga senza cartella**: senza una cartella scelta non
     * c'è niente da scordare, e il comando non deve comparire.
     */
    @Test
    fun `con una cartella scelta si torna a Download`() {
        var tornato = 0
        banco.setContent {
            AivTheme(darkTheme = false) {
                Finestra(folder = "Vacanze", onUseDownloads = { tornato += 1 })
            }
        }
        val ritorno = app.getString(R.string.save_name_default)
        banco.onNodeWithText(ritorno).assertExists()
        banco.onNodeWithText(ritorno).performClick()
        assertEquals("Il ritorno a Download non ha scordato la cartella", 1, tornato)
    }

    /**
     * **La riga della cartella dice la cartella scelta.**
     *
     * ⚠️⚠️ **UNA DESTINAZIONE MEMORIZZATA E INVISIBILE È PEGGIO DI NESSUNA DESTINAZIONE**, e
     * dalla `1.81` la cartella si ricorda (voce `scarica-percorso` e campo libero punto E del
     * giro della `1.80`): senza questa riga, chi ha scelto una cartella un mese fa salva in un
     * posto che non ricorda di aver scelto.
     * ⚠️ **La faccia opposta è la prova dopo**, e sono due perché `setContent` si chiama una
     * volta sola per prova: chiamarlo due volte va in errore con un messaggio che parla di
     * `ComposeView`, cioè di un'altra cosa.
     */
    @Test
    fun `la riga della cartella dice la cartella scelta`() {
        banco.setContent { AivTheme(darkTheme = false) { Finestra(folder = "Vacanze") } }
        banco.onNodeWithText(app.getString(R.string.save_name_path, "Vacanze")).assertExists()
    }

    /**
     * **Senza una cartella scelta quella riga non c'è.**
     *
     * ⚠️ **Una riga che comparisse sempre direbbe 'Download' ogni volta** a chi non ha scelto
     * niente, cioè al caso di fabbrica: è quello che l'app fa da sempre e che nessuno ha bisogno
     * di rileggere a ogni salvataggio.
     */
    @Test
    fun `senza cartella scelta la riga non c'e`() {
        banco.setContent { AivTheme(darkTheme = false) { Finestra() } }
        banco.onNodeWithText(app.getString(R.string.save_name_path, "Vacanze"))
            .assertDoesNotExist()
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
     * ⚠️ **I valori di serie sono il caso di fabbrica**: nessun tocco lungo, nessuna
     * 'Destinazione' e nessuna cartella scelta, cioè quello che vede chi accende la sola rinomina
     * al salvataggio. Le prove che guardano l'altro caso lo dicono passando gli argomenti, e così
     * si legge nella prova quale dei due casi sta misurando.
     */
    @Composable
    private fun Finestra(
        hold: Boolean = false,
        folder: String? = null,
        onPickFolder: (() -> Unit)? = null,
        onUseDownloads: (() -> Unit)? = null,
        onSave: (String, String) -> Unit = { _, _ -> }
    ) {
        SaveNameDialog(
            full = "foto.jpg",
            hold = hold,
            folder = folder,
            onPickFolder = onPickFolder,
            onUseDownloads = onUseDownloads,
            onDismiss = {},
            onSave = onSave
        )
    }
    /**
     * **Il registro dei download riconosce un doppione e scorda il vecchio.**
     *
     * ⚠️⚠️ **NASCE COL REGISTRO, NELLA `1.82`** (campo libero del giro della `1.81`, punto E):
     * la firma è suffisso più byte, quindi le due cose da provare sono che due scritture diverse
     * dello stesso suffisso contino come una, e che una voce vecchia esca dal registro.
     * ⚠️ **Le funzioni pure e non l'archivio**: `DataStore` in una prova vorrebbe un contesto e
     * un file, e quello che qui può sbagliare è il conto, non la scrittura su disco.
     */
    @Test
    fun `il registro dei download riconosce la stessa firma`() {
        assertEquals(
            "Due scritture dello stesso suffisso non hanno dato la stessa firma",
            DownloadLog.mark(".JPG", 12345L),
            DownloadLog.mark("jpg", 12345L)
        )
        assertTrue(
            "Due pesi diversi hanno dato la stessa firma",
            DownloadLog.mark("jpg", 12345L) != DownloadLog.mark("jpg", 12346L)
        )
    }

    /**
     * **Le voci scadute e quelle in eccesso escono dal registro.**
     *
     * ⚠️ **Trentuno giorni è un giorno oltre la soglia**, cioè il primo caso che deve uscire: una
     * prova a trenta esatti misurerebbe l'arrotondamento invece della regola.
     */
    @Test
    fun `il registro dei download si pota`() {
        val ora = 1_800_000_000_000L
        val giorno = 24L * 60 * 60 * 1000
        val vecchia = "jpg:1@${ora - 31 * giorno}"
        val fresca = "png:2@${ora - giorno}"
        val potato = DownloadLog.prune(setOf(vecchia, fresca, "rotta:3"), ora)
        assertTrue("La voce scaduta è rimasta", vecchia !in potato)
        assertTrue("La voce fresca è stata buttata", fresca in potato)
        assertTrue("Una voce senza istante è rimasta", potato.none { it.startsWith("rotta") })

        val tante = (1..500).map { "jpg:$it@${ora - it}" }.toSet()
        assertTrue(
            "Il tetto del registro non ha tenuto: ${DownloadLog.prune(tante, ora).size} voci",
            DownloadLog.prune(tante, ora).size < 200
        )
    }

}

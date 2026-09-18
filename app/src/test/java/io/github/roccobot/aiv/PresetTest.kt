package io.github.roccobot.aiv

import android.content.Context
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Il banco di prova dei **preset** dell'editor completo, dalla `2.39`.
 *
 * ⚠️⚠️ **QUELLO CHE QUI SI ROMPE IN SILENZIO È L'ANDATA E RITORNO**: un campo dimenticato in
 * [Presets] non dà nessun errore e non lo vede nessun compilatore, perché ogni campo che manca
 * vale il suo valore di riposo. Il preset si salva, si riapre, e fa un'altra cosa: chi lo ha
 * salvato pensa di averlo perso e non sa perché.
 *
 * ⚠️ **Quello che il banco non vede**: come i venti di casa cambiano un'immagine, che è la sola
 * cosa che conta davvero di un preset. Quello si guarda sul telefono, e la voce di collaudo lo
 * chiede.
 */
@RunWith(AndroidJUnit4::class)
class PresetTest {

    @get:Rule
    val banco = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * ⚠️ **L'archivio si svuota prima di ogni prova**: è un file, quindi quello che una prova
     * scrive lo trova la prossima, e due prove che si passano lo stato sono due prove che
     * falliscono in ordine sparso.
     */
    @Before
    fun pulisci() {
        Presets.mine(app).forEach { Presets.remove(app, it) }
        assertTrue("L'archivio doveva partire vuoto", Presets.mine(app).isEmpty())
    }

    /**
     * **Caso 1: quello che si salva è quello che si riapre, campo per campo.**
     *
     * ⚠️⚠️ **IL `Look` DI PROVA HA I CINQUE MODULI TUTTI PIENI, e non è abbondanza**: un solo
     * modulo lasciato a riposo sarebbe un campo che la prova non guarda, cioè esattamente quello
     * che ci si dimentica di scrivere. Le otto fasce portano valori **diversi** l'una dall'altra
     * per la stessa ragione: con otto bande uguali, un indice scambiato nel giro di lettura non
     * si vedrebbe.
     */
    @Test
    fun `un preset salvato si riapre identico`() {
        Presets.save(app, "Tutto", PIENO)
        val letto = Presets.mine(app).single()

        assertEquals("Il nome non è tornato", "Tutto", letto.name)
        assertEquals("La Luce non è tornata", PIENO.light, letto.look.light)
        assertEquals("Il Colore non è tornato", PIENO.chroma, letto.look.chroma)
        assertEquals("L'HSL non è tornato", PIENO.mix, letto.look.mix)
        assertEquals("Il Dettaglio non è tornato", PIENO.detail, letto.look.detail)
        assertEquals("Gli Effetti non sono tornati", PIENO.effects, letto.look.effects)
        assertEquals("Le Curve non sono tornate", PIENO.tone, letto.look.tone)
    }

    /**
     * **Caso 2: un preset non porta la posa, il ritaglio e la geometria.**
     *
     * ⚠️⚠️ **SENZA QUESTA RIGA UN PRESET RADDRIZZEREBBE LE FOTOGRAFIE DRITTE**: quei tre
     * dipendono da come è stata scattata quell'immagine, non dall'aspetto che si vuole dare.
     * ⚠️ **E si misura in tutti e due i versi**: che non li **prenda** salvando, e che non li
     * **tocchi** applicando, perché sono due righe diverse e sbagliarne una sola è possibile.
     */
    @Test
    fun `un preset non porta la posa il ritaglio e la geometria`() {
        val storto = PIENO.copy(
            spin = Spin(turns = 1, mirror = false),
            crop = ImageEdit.Crop(0.1f, 0.1f, 0.8f, 0.8f),
            geo = Geometry(straighten = 0.3f),
            framing = Framing(listOf(ImageEdit.Crop(0.1f, 0.1f, 0.8f, 0.8f)), 1)
        )
        val preso = Preset.of("Solo colore", storto)
        assertEquals("Il preset si è portato la posa", Spin.STILL, preso.look.spin)
        assertEquals("Il preset si è portato il ritaglio", ImageEdit.Crop.WHOLE, preso.look.crop)
        assertEquals("Il preset si è portato la geometria", Geometry.NONE, preso.look.geo)

        // ⚠️ La base porta una posa e un ritaglio suoi: applicare un preset non li deve muovere.
        val base = Look(
            spin = Spin(turns = 2, mirror = false),
            crop = ImageEdit.Crop(0.2f, 0.2f, 0.5f, 0.5f),
            geo = Geometry(distortion = 0.05f),
            framing = Framing(listOf(ImageEdit.Crop(0.2f, 0.2f, 0.5f, 0.5f)), 1)
        )
        val dopo = preso.applyTo(base)
        assertEquals("Applicare ha girato l'immagine", base.spin, dopo.spin)
        assertEquals("Applicare ha spostato il ritaglio", base.crop, dopo.crop)
        assertEquals("Applicare ha toccato la geometria", base.geo, dopo.geo)
        assertEquals("Applicare ha cambiato la porzione inquadrata", base.framing, dopo.framing)
        assertEquals("Il Colore non è arrivato", PIENO.chroma, dopo.chroma)
        assertEquals("La Luce non è arrivata", PIENO.light, dopo.light)
        assertEquals("Gli Effetti non sono arrivati", PIENO.effects, dopo.effects)
    }

    /**
     * **Caso 3: applicare sostituisce invece di sommare.**
     *
     * ⚠️⚠️ **È LA PROPRIETÀ CHE RENDE UN PRESET PREVEDIBILE**: sommando, applicarne uno sopra un
     * altro darebbe qualcosa che nessuno dei due descrive, e applicare due volte lo stesso darebbe
     * due immagini diverse. Qui si misura proprio quello: due applicazioni di fila e una sola
     * devono dare lo stesso risultato.
     */
    @Test
    fun `applicare due volte lo stesso preset da la stessa immagine`() {
        val preset = Preset.of("Doppio", PIENO)
        val una = preset.applyTo(Look.NONE)
        val due = preset.applyTo(preset.applyTo(Look.NONE))
        assertEquals("La seconda applicazione ha cambiato qualcosa", una, due)

        // ⚠️ E un preset applicato su un'immagine già sviluppata cancella quello che c'era: la
        // Luce della base non deve restare a metà.
        val sopra = preset.applyTo(Look(light = Light(exposure = 1.5f, blacks = -0.4f)))
        assertEquals("Un valore della base è sopravvissuto", PIENO.light, sopra.light)
    }

    /**
     * **Caso 4: lo stesso nome sostituisce, e non lascia due righe indistinguibili.**
     *
     * ⚠️ **Il confronto non guarda le maiuscole**, perché nell'elenco due nomi che differiscono
     * solo per quelle si leggono uguali: l'unica cosa che si potrebbe fare col secondo è cercare
     * di capire quale sia.
     */
    @Test
    fun `salvare con un nome gia usato sostituisce`() {
        Presets.save(app, "Sera", Look(light = Light(exposure = 0.5f)))
        Presets.save(app, "SERA", Look(light = Light(exposure = -0.5f)))

        val miei = Presets.mine(app)
        assertEquals("Sono due righe invece di una", 1, miei.size)
        assertEquals("Ha tenuto il valore vecchio", -0.5f, miei.single().look.light.exposure, 1e-4f)
        assertEquals("Il nome non è quello scritto per ultimo", "SERA", miei.single().name)
    }

    /**
     * **Caso 5: quello che si toglie se ne va, e il resto resta.**
     *
     * ⚠️ **Si misura anche che rimetterlo funzioni**, perché è la strada che percorre 'Annulla'
     * della notifica: là si salva di nuovo, cioè si passa dalla stessa porta dell'andata.
     */
    @Test
    fun `togliere un preset lascia gli altri al loro posto`() {
        Presets.save(app, "Uno", Look(light = Light(exposure = 0.5f)))
        Presets.save(app, "Due", Look(chroma = Chroma(saturation = 0.3f)))

        val uno = Presets.mine(app).first { it.name == "Uno" }
        val dove = Presets.remove(app, uno)
        assertEquals("Non è rimasto il solo 'Due'", listOf("Due"), Presets.mine(app).map { it.name })

        /*
         * ⚠️ **Rimetterlo è la strada di 'Annulla', dalla `2.50`**: là si ripassa dalla stessa
         * porta con il posto che aveva, quindi la prova misura anche che torni **dov'era** e non
         * in coda, che è il difetto che nessuno noterebbe con un elenco di due.
         */
        Presets.restore(app, uno, dove)
        assertEquals(
            "Rimetterlo non lo ha riportato al suo posto",
            listOf("Uno", "Due"),
            Presets.mine(app).map { it.name }
        )
    }

    /**
     * **Caso 6: i venti di casa sono venti, nessuno vuoto e nessuno omonimo.**
     *
     * ⚠️⚠️ **UN PRESET VUOTO NON SI VEDE E NON DÀ NESSUN ERRORE**: toccarlo non cambierebbe un
     * pixel, e chi lo prova penserebbe che i preset non funzionino. Quattordici nascono dai suoi
     * XMP e sei sono scritti in casa, e un errore di battitura in uno dei due elenchi si ferma
     * qui.
     */
    @Test
    fun `i venti di casa sono venti pieni e distinti`() {
        assertEquals("Non sono venti", 20, HOUSE.size)
        HOUSE.forEach { p ->
            assertTrue("Il preset '${p.name}' non cambia niente", !p.look.idle)
            assertTrue("Il preset '${p.name}' non si dichiara di casa", p.house)
            assertTrue("Un preset di casa ha il nome vuoto", p.name.isNotBlank())
        }
        assertEquals(
            "Due preset di casa si chiamano uguale",
            HOUSE.size,
            HOUSE.map { it.name.lowercase() }.toSet().size
        )
    }

    /**
     * **Caso 7: gli 'Stili AIV' vengono prima, i propri sotto.**
     *
     * ⚠️⚠️ **L'ORDINE SI È ROVESCIATO CON LA `2.50`, ED È SUA ISTRUZIONE** (nota sulla voce
     * `preset-salva` del giro della `2.40`: *'Di serie' ... deve restare, ma diventa 'Stili AIV'
     * ... mentre quelli salvati, in basso, diventeranno 'Stili personali'*). Fino alla `2.40` i
     * propri stavano in cima, col più recente per primo.
     * ⚠️ **E fra i propri l'ordine è quello di salvataggio**, cioè il più recente in fondo: da
     * quando la pagina delle impostazioni li riordina a mano, mettere il nuovo in cima
     * scavalcherebbe l'ordine che l'utente ha scelto.
     */
    @Test
    fun `gli stili di casa vengono prima dei propri`() {
        Presets.save(app, "Vecchio", Look(light = Light(exposure = 0.5f)))
        Presets.save(app, "Nuovo", Look(light = Light(contrast = 0.5f)))

        val tutti = Presets.all(app)
        assertEquals("I venti di casa non aprono l'elenco", HOUSE.size + 2, tutti.size)
        assertTrue("Il primo non è di casa", tutti[0].house)
        assertTrue("L'ultimo di casa non è al suo posto", tutti[HOUSE.size - 1].house)
        assertEquals("Il primo dei propri non segue quelli di casa", "Vecchio", tutti[HOUSE.size].name)
        assertEquals("Il più recente non è l'ultimo", "Nuovo", tutti.last().name)
        assertTrue("Un proprio si dichiara di casa", !tutti.last().house)
    }

    /**
     * **Caso 8: il modulo elenca, applica, e resta in scena.**
     *
     * ⚠️⚠️ **CHE RESTI IN SCENA È METÀ DELLA FUNZIONE**: un preset si sceglie confrontando, e un
     * elenco che si chiudesse a ogni tocco costringerebbe a riaprirlo per provare il prossimo.
     * Misurato dal nome, che dopo il tocco deve essere ancora in scena.
     * ⚠️ **Dalla `2.50` è il corpo di un modulo e non una scheda che si apre** (sua istruzione:
     * *inserisci i modelli in un modulo a parte*), quindi non c'è più niente da chiudere e il
     * tocco dichiara anche se era lungo, cioè se applica in modo additivo.
     */
    @Test
    fun `il modulo applica il preset toccato e resta in scena`() {
        var scelto: Preset? = null
        var additivo: Boolean? = null
        banco.setContent {
            AivTheme(darkTheme = false) {
                PresetBody(
                    height = 320.dp,
                    mine = Presets.mine(app),
                    onPick = { p, add ->
                        scelto = p
                        additivo = add
                    }
                )
            }
        }

        val nome = HOUSE.first().name
        banco.onNodeWithText(nome).performClick()
        banco.waitForIdle()

        assertEquals("Il tocco non ha applicato quel preset", nome, scelto?.name)
        assertTrue("Il preset applicato non cambia niente", !(scelto?.look?.idle ?: true))
        assertEquals("Un tocco normale si è dichiarato additivo", false, additivo)
        banco.onNodeWithText(nome).assertIsDisplayed()
    }

    /**
     * **Caso 9: nell'elenco del modulo non si cancella niente, e non è una dimenticanza.**
     *
     * ⚠️⚠️ **DALLA `2.50` GLI STILI SI GESTISCONO NELLE IMPOSTAZIONI, ED È SUA ISTRUZIONE**
     * (nota sulla voce `preset-salva` del giro della `2.40`: *servirà una nuova sezione delle
     * Impostazioni ... in cui si possono riordinare, rinominare e cancellare*). Qui si **sceglie**
     * uno stile, e un comando che cancella accanto a uno che applica è il modo per perdere uno
     * stile mentre se ne prova un altro. Il caso vale anche con un preset proprio in elenco, che
     * è il solo che si potrebbe cancellare.
     */
    @Test
    fun `l elenco del modulo non porta nessun comando che toglie`() {
        Presets.save(app, "Mio", Look(light = Light(exposure = 0.5f)))
        banco.setContent {
            AivTheme(darkTheme = false) {
                PresetBody(height = 320.dp, mine = Presets.mine(app), onPick = { _, _ -> })
            }
        }
        /*
         * ⚠️ **Si scorre fino a lui**: i propri stanno **sotto** i venti di casa (sua istruzione,
         * *quelli salvati, in basso*), e in un elenco pigro quello che è fuori scena non è nemmeno
         * nell'albero. Senza questa riga la prova misurerebbe zero comandi perché la riga non c'è,
         * invece che perché il comando non esiste.
         */
        banco.onNode(hasScrollAction()).performScrollToNode(hasText("Mio"))
        banco.onNodeWithText("Mio").assertIsDisplayed()
        assertEquals(
            "L'elenco che applica porta anche un comando che cancella",
            0,
            comandiCheTolgono()
        )
    }

    /**
     * **Caso 9b: la pagina 'Stili di modifica' li cancella tutti, di casa compresi.**
     *
     * ⚠️ **Sono due prove e non una**, e non è una scelta di stile: `setContent` si chiama una
     * volta sola per regola, quindi le due scene vogliono due prove. Questa salva **prima** di
     * montare, perché la pagina legge l'archivio all'apertura.
     * ⚠️⚠️ **ANCHE QUELLI DI CASA SI CANCELLANO, ED È SUA ISTRUZIONE** (*sia i predefiniti di
     * fabbrica che quelli creati dall'utente*): il conto è quindi i venti di casa più i propri, e
     * una prova che ne contasse uno solo direbbe che i suoi non si toccano.
     */
    @Test
    fun `la pagina degli stili porta il comando che toglie su tutti`() {
        Presets.save(app, "Mio", Look(light = Light(exposure = 0.5f)))
        banco.setContent {
            AivTheme(darkTheme = false) {
                StyleSettings(scroll = rememberScrollState())
            }
        }
        assertEquals(
            "I comandi che tolgono non sono uno per stile",
            HOUSE.size + 1,
            comandiCheTolgono()
        )
    }

    /**
     * **Caso 9c: senza stili propri l'elenco non porta nessun titolino.**
     *
     * ⚠️⚠️ **È IL PRIMO DEI TRE RITOCCHI DELLA `2.52`** (suo, 2026-09-14: *il nome della categoria
     * ('Stili AIV') a ben vedere non serve: in questo contesto i pixel verticali sono preziosi e si
     * capisce perfettamente che i primi sono di fabbrica*). Con quel titolo via, il separatore dei
     * propri da solo annuncerebbe una parte che al primo avvio non esiste, quindi anche lui c'è
     * **solo se** c'è almeno uno stile salvato.
     * ⚠️ **Si misurano tutti e due i titoli**: una prova che guardasse il solo 'Stili AIV' sarebbe
     * verde anche con un separatore che compare sopra il vuoto.
     */
    @Test
    fun `l elenco senza stili propri non porta nessun titolino`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                PresetBody(height = 320.dp, mine = emptyList(), onPick = { _, _ -> })
            }
        }
        assertEquals(
            "il titolo degli stili dell'app è ancora scritto",
            0,
            quantiDetti(R.string.look_preset_house)
        )
        assertEquals(
            "il separatore dei salvati compare senza niente sotto",
            0,
            quantiDetti(R.string.look_preset_mine)
        )
    }

    /**
     * **Caso 9d: con uno stile proprio compare il solo separatore dei salvati.**
     *
     * ⚠️ **È la controprova del caso 9c**: senza di lei un elenco che non scrivesse **mai** il
     * separatore resterebbe verde, e i due gruppi si leggerebbero come uno solo.
     */
    @Test
    fun `con uno stile proprio compare il solo separatore dei salvati`() {
        Presets.save(app, "Mio", Look(light = Light(exposure = 0.5f)))
        banco.setContent {
            AivTheme(darkTheme = false) {
                PresetBody(height = 320.dp, mine = Presets.mine(app), onPick = { _, _ -> })
            }
        }
        banco.onNode(hasScrollAction()).performScrollToNode(hasText(testo(R.string.look_preset_mine)))
        assertEquals(
            "il separatore dei salvati non c'è",
            1,
            quantiDetti(R.string.look_preset_mine)
        )
        assertEquals(
            "il titolo degli stili dell'app è tornato",
            0,
            quantiDetti(R.string.look_preset_house)
        )
    }

    /** Quante volte il testo [id] è in scena: il banco non ha un conto pronto. */
    private fun quantiDetti(id: Int): Int = banco
        .onAllNodesWithText(testo(id))
        .fetchSemanticsNodes().size

    /** Il testo di una risorsa, come lo legge l'app. */
    private fun testo(id: Int): String = app.getString(id)

    /** Quanti comandi 'Elimina' sono in scena: il banco non ha un conto pronto. */
    private fun comandiCheTolgono(): Int = banco
        .onAllNodesWithContentDescription(app.getString(R.string.look_preset_remove))
        .fetchSemanticsNodes().size

    /**
     * **Caso 10: il tasto che salva è spento quando non c'è niente da salvare.**
     *
     * ⚠️ **Un preset preso da un'immagine non toccata sarebbe una riga che non fa niente**, cioè
     * lo stesso difetto del caso 6 con un nome scelto a mano.
     */
    @Test
    fun `il tasto che salva e spento su un immagine non toccata`() {
        assertTrue("Un Look a riposo si dichiara degno di un preset", Preset.of("", Look.NONE).look.idle)
        assertTrue("Un Look sviluppato non si dichiara degno", !Preset.of("", PIENO).look.idle)
        // ⚠️ La posa da sola non basta: un preset non la porta, quindi non c'è niente da salvare.
        assertTrue(
            "Una posa da sola fa credere che ci sia un preset da prendere",
            Preset.of("", Look(spin = Spin(turns = 1, mirror = false))).look.idle
        )
    }

    /**
     * **Caso 11: un modulo a riposo non finisce nel file.**
     *
     * ⚠️⚠️ **NON È UN RISPARMIO DI BYTE**: un preset di sola Luce deve **rileggersi** come tale, e
     * con i moduli scritti per intero direbbe che tocca anche il colore e le curve, a zero. Chi
     * apre quel file vedrebbe un preset che fa tutto.
     */
    @Test
    fun `un modulo a riposo non si scrive`() {
        Presets.save(app, "Sola luce", Look(light = Light(exposure = 0.5f)))
        val letto = Presets.mine(app).single()
        assertEquals("Il Colore è entrato da solo", Chroma.NONE, letto.look.chroma)
        assertEquals("L'HSL è entrato da solo", Mix.NONE, letto.look.mix)
        assertEquals("Il Dettaglio è entrato da solo", Detail.NONE, letto.look.detail)
        assertEquals("Gli Effetti sono entrati da soli", Effects.NONE, letto.look.effects)
        assertEquals("Le Curve sono entrate da sole", Tone.NONE, letto.look.tone)
        assertNotEquals("La Luce non è arrivata", Light.NONE, letto.look.light)
    }
}

/**
 * Un [Look] coi sei moduli di colore tutti pieni, e le otto fasce diverse l'una dall'altra.
 *
 * ⚠️ **Dalla `2.53` sono sei e non cinque**: gli Effetti sono un aspetto come gli altri, quindi un
 * preset se li porta, e senza il loro campo qui la prova non guarderebbe proprio il modulo nuovo.
 * ⚠️⚠️ **E DALLA `2.57` QUEL MODULO HA CINQUE CURSORI, TUTTI E CINQUE SCRITTI QUI**: un campo nuovo
 * che si dimenticasse di questa riga non darebbe nessun errore, perché l'andata e ritorno
 * confronterebbe due valori di riposo. È la stessa forma di prova che mente in verde, e il rimedio
 * è che qui non ci sia **nessun** campo a zero.
 *
 * ⚠️ **I numeri non sono tondi di proposito**: un arrotondamento nella scrittura del file si
 * vedrebbe su `0,37` e non su `0,5`.
 */
private val PIENO = Look(
    light = Light(
        exposure = 0.75f, contrast = -0.37f, highlights = 0.21f,
        shadows = -0.62f, whites = 0.14f, blacks = -0.08f
    ),
    chroma = Chroma(
        temp = 0.31f, tint = -0.17f, saturation = 0.44f,
        vibrance = -0.23f, mono = true, filter = 0.56f
    ),
    mix = Mix(List(Mix.COUNT) { i ->
        Band(hue = 0.01f * (i + 1), sat = -0.02f * (i + 1), lum = 0.03f * (i + 1))
    }),
    detail = Detail(
        sharpen = 0.61f, radius = -0.29f, masking = 0.47f,
        noise = 0.33f, noiseColor = 0.18f
    ),
    effects = Effects(
        clarity = 0.42f, texture = -0.26f, haze = 0.31f, vignette = -0.53f, grain = 0.27f
    ),
    tone = Tone(
        all = Curve(listOf(Knot(0f, 0.05f), Knot(0.5f, 0.62f), Knot(1f, 0.97f))),
        red = Curve(listOf(Knot(0f, 0f), Knot(0.33f, 0.41f), Knot(1f, 1f))),
        green = Curve(listOf(Knot(0f, 0.02f), Knot(1f, 0.98f))),
        blue = Curve(listOf(Knot(0f, 0f), Knot(0.7f, 0.58f), Knot(1f, 1f)))
    )
)

package io.github.roccobot.aiv

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Il banco di prova dei **nomi di file che vanno a capo**.
 *
 * ⚠️⚠️ **NASCE COL DIFETTO CHE È ARRIVATO A LUI DUE VOLTE** (riscontro del giro della `1.80`,
 * punto C: *accade ancora che le estensioni dei file siano spezzate*, e prima nel giro della
 * `1.59` sulle pastiglie della rinomina), come prescrive `AIV/CLAUDE.md` § '🧪 Quando si scrive
 * una prova, e quando no'. La `1.59` aveva corretto **una** delle due vie che compongono un
 * nome, e nell'altra il difetto è rimasto vivo per ventidue versioni.
 *
 * ⚠️⚠️ **IL LAYOUT NON SI PUÒ MISURARE QUI, ED È MISURATO CHE NON SI PUÒ** (sonda del
 * 2026-09-07): sulla piattaforma finta il misuratore di testo dà a ogni carattere **un pixel** di
 * larghezza a qualunque corpo, tronca la larghezza al vincolo che riceve, e riporta **una riga
 * sola** con `hasVisualOverflow` falso perfino chiedendogli di impaginare quaranta caratteri in
 * quattro pixel. Cioè su questo banco un testo non va a capo affatto, e una prova che guardasse
 * dove il layout rompe le righe passerebbe in verde senza aver misurato niente.
 * ⚠️ **Quindi si prova quello che DECIDE il layout, e che è nostro**: i punti in cui il testo
 * concede o vieta un'andata a capo, e l'ordine delle leve con cui si stringe. Se il layout
 * rispetta quei punti lo dice il telefono, e quella è la voce di collaudo.
 */
@RunWith(AndroidJUnit4::class)
class NomiTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * Il nome della sua schermata, che è il caso peggiore possibile: nessuno spazio, punti in
     * mezzo, e un suffisso che il layout aveva rotto come `...png.w` a capo `ebp`.
     */
    private val nome = "whale-watercolor-illustration-png.png.webp"

    private lateinit var measurer: TextMeasurer
    private lateinit var style: TextStyle

    private fun scena() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                measurer = rememberTextMeasurer()
                style = MaterialTheme.typography.titleSmall
            }
        }
        banco.waitForIdle()
    }

    /**
     * **Caso 1: il nome concede un'andata a capo PRIMA dell'estensione e la vieta dentro.**
     *
     * ⚠️⚠️ **SONO LE DUE METÀ DELLA STESSA REGOLA, e la `1.80` ne aveva una sola**: il giuntore
     * dentro l'estensione **vieta** di romperla, e quel divieto vale finché il layout ha un altro
     * posto in cui andare a capo; un nome senza spazi che non ne ha nemmeno uno il layout lo rompe
     * per forza, e rompe dove capita, giuntori compresi. L'appiglio prima del punto è quello che
     * gli dà il posto legale, e in [fitName] mancava.
     */
    @Test
    fun `il nome concede un capo prima dell'estensione e lo vieta dentro`() {
        scena()
        val messo = fitName(nome, room = 200, lines = 3, style = style, measurer = measurer)
        val testo = messo.text.text

        val punto = testo.lastIndexOf('.')
        assertTrue("Nel testo composto non c'è nessuna estensione", punto > 0)
        assertEquals(
            "Prima dell'estensione non c'è un punto in cui andare a capo: il layout la spezza",
            APPIGLIO,
            testo[punto - 1]
        )

        val coda = testo.substring(punto)
        val lettere = coda.filter { it != GIUNTORE }
        assertEquals("L'estensione composta non è quella del nome", ".webp", lettere)
        for (i in 1 until lettere.length) {
            assertTrue(
                "Fra due caratteri dell'estensione manca il giuntore: il layout può romperla",
                coda.contains("${lettere[i - 1]}$GIUNTORE${lettere[i]}")
            )
        }
    }

    /**
     * **Caso 2: un nome senza estensione non porta nessun appiglio.**
     *
     * L'altra metà del caso 1: senza di lei passerebbe anche infilando quel carattere sempre,
     * cioè scrivendo un carattere invisibile dentro un nome che non ne ha bisogno.
     */
    @Test
    fun `un nome senza estensione non riceve appigli`() {
        scena()
        val messo = fitName("senzasuffisso", room = 200, lines = 3, style = style, measurer = measurer)
        assertEquals("Il nome è stato ritoccato", "senzasuffisso", messo.text.text)
    }

    /**
     * **Caso 3: un file nascosto non ha un'estensione, e il suo punto iniziale non si tocca.**
     *
     * ⚠️ `.gitignore` è tutto nome: trattare quel punto come un suffisso vorrebbe dire un
     * appiglio e cinque giuntori dentro una parola sola.
     */
    @Test
    fun `un file nascosto non ha estensione`() {
        scena()
        val messo = fitName(".gitignore", room = 200, lines = 3, style = style, measurer = measurer)
        assertEquals("Il punto iniziale è stato preso per un suffisso", ".gitignore", messo.text.text)
        assertFalse("Un nome nascosto ha ricevuto un appiglio", messo.text.text.contains(APPIGLIO))
    }

    /**
     * **Caso 4: la spaziatura viene prima del corpo, che è l'ordine di priorità che ha dettato.**
     *
     * ⚠️ Senza questo, il giorno che qualcuno rimette il corpo come prima leva la stretta
     * continuerebbe a funzionare e si vedrebbe di più, cioè un peggioramento che nessun controllo
     * dichiara.
     * ⚠️⚠️ **E HA TROVATO UN DIFETTO PREESISTENTE ALLA PRIMA CORSA**: la stretta minima
     * dichiarata è l'80%, e sottraendo il gradino in virgola mobile la corsa si fermava all'85%,
     * cioè un gradino prima. Adesso i gradini si contano interi.
     */
    @Test
    fun `il corpo si riduce solo dopo aver stretto la spaziatura`() {
        scena()
        val prove = strette(style).toList()

        assertEquals("La prima prova non è lo stile pieno", style, prove.first())
        assertTrue("Non c'è nessuna stretta da provare", prove.size > 2)

        val seconda = prove[1]
        assertEquals("La seconda prova non stringe la spaziatura", NAME_TIGHT, seconda.letterSpacing)
        assertEquals(
            "La seconda prova riduce già il corpo: le due leve sono invertite",
            style.fontSize,
            seconda.fontSize
        )

        for ((quale, stile) in prove.withIndex().drop(1)) {
            assertEquals(
                "La prova $quale riduce il corpo senza aver stretto la spaziatura",
                NAME_TIGHT,
                stile.letterSpacing
            )
        }

        val ultimo = prove.last().fontSize.value / style.fontSize.value
        assertTrue("L'ultima prova non arriva al minimo dichiarato: $ultimo", ultimo <= NAME_FLOOR + 0.001f)
        assertTrue("L'ultima prova va sotto il minimo dichiarato: $ultimo", ultimo >= NAME_FLOOR - 0.001f)
    }

    /**
     * **Caso 5: un nome che sta comodo non si tocca.**
     *
     * L'altra metà del caso 4: senza di lei passerebbe anche stringendo sempre.
     */
    @Test
    fun `un nome corto resta alla misura piena`() {
        scena()
        val messo = fitName("a.webp", room = 2000, lines = 3, style = style, measurer = measurer)
        assertEquals("Un nome che sta comodo è stato stretto", style, messo.style)
    }
}

/**
 * `U+200B`: invisibile, e **concede** al layout di andare a capo dove sta.
 *
 * ⚠️ Scritto per **codepoint** e non incollato, come i suoi gemelli in `Names.kt` e per la stessa
 * ragione: incollato, questo file conterrebbe un carattere che a schermo non si vede.
 */
private const val APPIGLIO = '\u200B'

/** `U+2060`: invisibile, e **vieta** al layout di andare a capo dove sta. */
private const val GIUNTORE = '\u2060'

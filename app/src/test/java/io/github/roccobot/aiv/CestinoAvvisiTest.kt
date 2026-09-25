package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * La prima delle due richieste del cestino della `2.86`: la notifica che non copre il FAB. La
 * seconda, lo spazio liberato, vive in `CestinoSpazioTest`, e il perché è scritto là.
 *
 * ⚠️⚠️ **LA PRIMA È UNA SUA SEGNALAZIONE, E LA FORMA DEL DIFETTO L'HA DETTA IL BANCO** (2026-09-21:
 * *l'avviso dal basso (es. selezione scartata o file eliminato) deve evitare di coprire il FAB
 * anche nel cestino*). Le cause erano due, e questi due casi sono le loro controprove: dopo
 * Indietro il FAB restava fuori scena per tutta la vita della notifica, e dopo un'eliminazione la
 * notifica scendeva con la scheda larga tutto lo schermo e negli ultimi fotogrammi passava sopra
 * il tasto già rientrato.
 * ⚠️⚠️ **SI GUARDA OGNI FOTOGRAMMA DELLA DISCESA, E NON SOLO LA FINE**: a corsa finita la notifica
 * era già stretta anche prima della correzione, quindi una prova che guardasse solo lì sarebbe
 * verde col difetto dentro. Il difetto viveva in tre fotogrammi su quaranta.
 * ⚠️ **Il riquadro del FAB si stringe di un pixel**, ed è una tolleranza dichiarata: la notifica
 * legge la cima del tasto misurata nel fotogramma prima, e durante il rimbalzo quella cima si
 * muove di frazioni di pixel. Il riquadro della notifica comprende il suo margine, quindi un pixel
 * di contatto lì non è un pixel coperto.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [OmbraArchivio::class])
class CestinoAvvisiTest {

    @get:Rule
    val banco = createAndroidComposeRule<ComponentActivity>()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private val foto = (1..6).map { Uri.parse("file:///finta/$it.jpg") }

    /*
     * ⚠️ **Il velo del cestino si dà per visto**: al primo ingresso illumina il FAB e consuma il
     * primo tocco, quindi senza questa riga il tocco lungo che apre la selezione non arriverebbe
     * alla miniatura. È anche il caso vero di chi il cestino l'ha già aperto una volta.
     * ⚠️ **E il canale si azzera**, perché è un oggetto di processo: una riga lasciata in scena
     * da un'altra prova entrerebbe in questa.
     */
    @Before
    fun prepara() {
        runBlocking { Hint.BIN_EMPTY.remember(app) }
        azzera()
    }

    @After
    fun ripulisci() = azzera()

    private fun azzera() {
        Notices.line?.let { Notices.dismiss(it.id) }
    }

    /** La griglia del cestino con la notifica di casa sopra, come la mette in scena `AivApp`. */
    private fun scena() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize()) {
                    GridScreen(
                        title = "Cestino",
                        items = foto,
                        highlight = null,
                        onOpen = {},
                        onBack = {},
                        onChanged = {},
                        onSearch = {},
                        bin = true
                    )
                    AppNotice(
                        Notices.line,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .aboveFoot()
                            .testTag("avviso")
                    )
                }
            }
        }
        banco.waitForIdle()
    }

    private fun fab(): Rect? = banco.onAllNodesWithContentDescription(app.getString(R.string.pick_actions))
        .fetchSemanticsNodes().firstOrNull()?.boundsInRoot

    private fun avviso(): Rect? = banco.onAllNodesWithTag("avviso").fetchSemanticsNodes()
        .firstOrNull()?.boundsInRoot

    private fun seleziona() {
        banco.onAllNodesWithContentDescription(app.getString(R.string.grid_item, 1, foto.size))
            .onFirst()
            .performTouchInput { longClick() }
        banco.waitForIdle()
        banco.mainClock.advanceTimeBy(1_000)
        banco.waitForIdle()
    }

    /**
     * Fa scorrere il tempo a passi di un fotogramma e dice in quale fotogramma la notifica è
     * finita sopra il FAB, oppure `null` se non è successo mai. In più dice se il FAB c'è stato.
     */
    private fun discesa(): Pair<String?, Boolean> {
        var sopra: String? = null
        var visto = false
        for (passo in 0..50) {
            banco.mainClock.advanceTimeBy(16)
            val f = fab() ?: continue
            visto = true
            val a = avviso() ?: continue
            if (sopra == null && f.deflate(1f).overlaps(a)) {
                sopra = "a ${passo * 16} ms la notifica ($a) è sopra il FAB ($f)"
            }
        }
        return sopra to visto
    }

    /**
     * **Dopo Indietro il FAB torna subito, e la notifica non gli passa mai sopra.**
     *
     * ⚠️⚠️ **È LA METÀ 'SELEZIONE SCARTATA' DELLA SUA SEGNALAZIONE**: fino alla `2.85` il FAB
     * restava fuori scena finché la notifica viveva (la regola della `1.44`), e ricompariva
     * mentre lei se ne andava. Il caso misura le due cose: che il tasto ci sia durante la discesa,
     * e che a discesa finita la notifica gli stia accanto.
     * ⚠️ **Controprovata** rimettendo `cleared == null` nella condizione del FAB: il tasto non
     * compare in nessuno dei cinquanta fotogrammi, e il caso cade sulla prima asserzione.
     */
    @Test
    fun `dopo Indietro il FAB resta in scena e la notifica gli sta accanto`() {
        scena()
        seleziona()
        banco.mainClock.autoAdvance = false
        banco.runOnUiThread { banco.activity.onBackPressedDispatcher.onBackPressed() }
        val (sopra, visto) = discesa()
        assertTrue("dopo Indietro il FAB doveva tornare in scena", visto)
        assertEquals(null, sopra)
        val f = assertNotNullRect(fab(), "il FAB")
        val a = assertNotNullRect(avviso(), "la notifica")
        assertTrue(
            "a discesa finita la notifica (fino a ${a.right}) doveva stare accanto al FAB (da ${f.left})",
            a.right <= f.left
        )
    }

    /**
     * **Mentre la scheda scende dopo un'eliminazione, la notifica non passa sopra il FAB.**
     *
     * ⚠️⚠️ **È LA METÀ 'FILE ELIMINATO', E IL DIFETTO ERA IN TRE FOTOGRAMMI**: il FAB rientra
     * appena la selezione finisce, la notifica nasce sopra la scheda e scende con lei, e fino alla
     * `2.85` si stringeva solo a terra. Misurato: da 320 a 352 millisecondi il suo bordo di sotto
     * era già dentro la fascia del tasto, larga tutto lo schermo.
     * ⚠️ **L'eliminazione fallisce, e va bene così**: i file della scena sono indirizzi finti, e la
     * notifica che ne esce dice quanti non sono passati. Quello che si misura è dove sta, non che
     * cosa dice.
     * ⚠️ **Controprovata** rimettendo `stretta = su == 0` in `aboveFoot`: il caso cade al
     * fotogramma dei 320 millisecondi.
     */
    @Test
    fun `mentre la scheda scende la notifica non passa sopra il FAB`() {
        scena()
        seleziona()
        val parola = app.getString(R.string.pick_delete)
        banco.onAllNodes(hasText(parola) or hasContentDescription(parola)).onFirst().performClick()
        banco.waitForIdle()
        banco.mainClock.autoAdvance = false
        banco.onAllNodes(hasText(parola)).onLast().performClick()
        val (sopra, visto) = discesa()
        assertTrue("dopo l'eliminazione il FAB doveva tornare in scena", visto)
        assertEquals(null, sopra)
    }

    private fun assertNotNullRect(r: Rect?, chi: String): Rect {
        assertNotNull("$chi doveva essere in scena", r)
        return r!!
    }
}

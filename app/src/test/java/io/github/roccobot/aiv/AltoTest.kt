package io.github.roccobot.aiv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Una finestra in cui si scrive va in alto, non si muove mentre la tastiera cambia misura, e
 * quando la tastiera le toglie spazio cede l'aria prima del contenuto.
 *
 * ⚠️⚠️ **NASCE COI DUE DIFETTI CHE SONO ARRIVATI A LUI**, come prescrive `Rules.md` § '🧪 Quando
 * si scrive una prova, e quando no': il pannello che ballava mentre si scriveva (giro della
 * `1.82`, voce `rinomina-ferma`), e la rinomina tagliata quando la si apre dal visualizzatore
 * (giro della `5.50`, nota su `5.50-01`: *la finestra Rinomina appare incompleta anche starebbe
 * comodamente nello spazio a disposizione*).
 * ⚠️⚠️ **PROVA LE FUNZIONI E NON LA SCHERMATA, E LA RAGIONE È MISURATA**: in Robolectric
 * `getRootWindowInsets` non riporta nessuna tastiera, quindi una prova che aprisse la rinomina
 * non vedrebbe mai lo spazio cambiare. Passerebbe in verde senza guardare niente, che è il modo
 * in cui una prova mente.
 */
class AltoTest {

    /** Le misure di casa, in dp: [TEXT_AIR], [LOWER_AIR] e [TEXT_TUCK]. */
    private val cielo = 70
    private val aria = 16
    private val margine = 24

    /**
     * I numeri delle sue due schermate del giro della `5.50` (Xiaomi 15, 3,33 pixel per dp): la
     * rinomina di un file solo chiede 418dp; aperta dal visualizzatore, che nasconde le barre, ha
     * 428dp sopra la tastiera; aperta dalla griglia, sotto la barra di stato, ne ha 392.
     */
    private val contenuto = 418
    private val visualizzatore = 428
    private val griglia = 392

    /** Il pannello che il tetto lascia a un contenuto di [contenuto]. */
    private fun pannello(spazio: Int) = minOf(contenuto, pinRoof(avail = spazio, tuck = margine))

    private fun top(spazio: Int, pannello: Int) =
        pinTop(avail = spazio, panel = pannello, sky = cielo, air = aria)

    @Test
    fun `dal visualizzatore la rinomina è intera e sopra la tastiera`() {
        // ⚠️ È la forma esatta del difetto: fino alla `5.50` il tetto toglieva 86dp e il pannello
        // restava di 342dp, con l'anteprima tagliata a metà.
        val alto = pannello(visualizzatore)
        assertEquals("il contenuto è stato accorciato", contenuto, alto)
        val sopra = top(visualizzatore, alto)
        assertEquals("con lo spazio stretto l'aria sopra cede tutta", 0, sopra)
        assertTrue("il pannello finisce dietro la tastiera", sopra + alto <= visualizzatore)
    }

    @Test
    fun `dalla griglia dietro la tastiera finisce al più il margine sotto i tasti`() {
        // ⚠️ La sua scelta B1: prima cede l'aria sopra, poi il margine sotto i tasti. Qui lo
        // spazio è 26dp più corto del contenuto, quindi il margine cede tutto e l'anteprima
        // scorre di 2dp.
        val alto = pannello(griglia)
        assertEquals(griglia + margine, alto)
        val sopra = top(griglia, alto)
        assertEquals(0, sopra)
        assertTrue(sopra + alto - griglia <= margine)
    }

    @Test
    fun `con spazio a sufficienza il pannello comincia all'aria dichiarata`() {
        // ⚠️ I numeri di un telefono con la tastiera chiusa, aperta, e con la barra dei
        // suggerimenti in più: fra questi passa tutto quello che succede mentre si scrive.
        val spazi = listOf(733, 433, 407)
        val posti = spazi.map { top(it, 300) }
        assertEquals("il pannello si è spostato al variare dello spazio: $posti", listOf(cielo), posti.distinct())
    }

    @Test
    fun `quando il pannello non ci sta più l'aria sopra cala senza salti`() {
        // ⚠️ Al confine le due regole danno lo stesso numero: un carattere in più che allunga
        // l'anteprima sposta il pannello di quanto si allunga, non di un salto.
        val spazio = 500
        val confine = spazio - cielo - aria
        assertEquals(cielo, top(spazio, confine))
        assertEquals(cielo - 1, top(spazio, confine + 1))
        assertEquals(spazio - aria - (confine + 40), top(spazio, confine + 40))
        assertEquals(0, top(spazio, spazio - aria))
    }

    @Test
    fun `con la tastiera ricordata il pannello non si muove quando lei arriva`() {
        // ⚠️ È la forma esatta del salto della 5.51 (sua nota: *all'apparizione c'è un salto
        // velocissimo*): i numeri sono quelli del visualizzatore, schermo alto 801dp, barre
        // nascoste, tastiera di 373dp. Al primo fotogramma la tastiera non c'è ancora.
        val schermo = 801
        val tastiera = 373
        fun posto(ime: Int, attesa: Boolean): Int {
            val spazio = schermo - keyboardBottom(bars = 0, ime = ime, remembered = tastiera, waiting = attesa)
            return top(spazio, pannello(spazio))
        }
        assertEquals("il pannello salta quando arriva la tastiera", posto(tastiera, false), posto(0, true))
    }

    @Test
    fun `senza una tastiera in arrivo non si conta quella ricordata`() {
        // ⚠️ Finita l'attesa (una tastiera fisica, o nessun campo col fuoco) conta solo quella vera.
        assertEquals(0, keyboardBottom(bars = 0, ime = 0, remembered = 373, waiting = false))
        assertEquals(48, keyboardBottom(bars = 48, ime = 0, remembered = 0, waiting = true))
        assertEquals(373, keyboardBottom(bars = 48, ime = 373, remembered = 300, waiting = false))
    }

    @Test
    fun `senza una misura dello spazio non c'è tetto e il pannello resta all'aria dichiarata`() {
        // ⚠️ Succede al primo passaggio, quando la finestra non è ancora disposta: meglio la
        // misura di prima che un tetto costruito su uno spazio inventato.
        assertEquals(0, pinRoof(avail = 0, tuck = margine))
        assertEquals(cielo, top(0, 300))
    }
}

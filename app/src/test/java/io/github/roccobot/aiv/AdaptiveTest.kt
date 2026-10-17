package io.github.roccobot.aiv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le soglie del layout tablet: 600 / 800 / 1024 del mockup, non Material 600 / 840.
 *
 * ⚠️ Prova la funzione pura e non la schermata: le fasce decidono se il pannello info
 * del Visualizzatore esiste e se nasce aperto, e un errore di una soglia sposterebbe
 * il layout su ogni tablet senza un fallimento evidente in UI.
 */
class AdaptiveTest {

    @Test
    fun `sotto 600 resta il layout telefono`() {
        assertFalse(Adaptive.sideAvailable(599))
        assertEquals(Adaptive.Band.PHONE, Adaptive.band(599))
        assertEquals(0, Adaptive.sideWidth(599).value.toInt())
    }

    @Test
    fun `da 600 il pannello laterale esiste ma parte chiuso`() {
        assertTrue(Adaptive.sideAvailable(600))
        assertFalse(Adaptive.sideDefaultOpen(600))
        assertEquals(Adaptive.Band.NARROW, Adaptive.band(600))
        assertEquals(210, Adaptive.sideWidth(600).value.toInt())
    }

    @Test
    fun `a 800 e la fascia media del mockup verticale`() {
        assertTrue(Adaptive.sideAvailable(800))
        assertFalse(Adaptive.sideDefaultOpen(800))
        assertEquals(Adaptive.Band.MEDIUM, Adaptive.band(800))
        assertEquals(240, Adaptive.sideWidth(800).value.toInt())
        // Un dp sotto la fascia larga resta chiuso di serie.
        assertFalse(Adaptive.sideDefaultOpen(1023))
        assertEquals(Adaptive.Band.MEDIUM, Adaptive.band(1023))
    }

    @Test
    fun `da 1024 il pannello nasce aperto come nel mockup orizzontale`() {
        assertTrue(Adaptive.sideAvailable(1024))
        assertTrue(Adaptive.sideDefaultOpen(1024))
        assertEquals(Adaptive.Band.WIDE, Adaptive.band(1024))
        assertEquals(280, Adaptive.sideWidth(1024).value.toInt())
    }

    @Test
    fun `le soglie non sono quelle Material 840`() {
        // A 840 Material considererebbe expanded; qui siamo ancora sotto SIDE_OPEN_MIN.
        assertFalse(Adaptive.sideDefaultOpen(840))
        assertEquals(Adaptive.Band.MEDIUM, Adaptive.band(840))
    }

    /**
     * **Gli strumenti dell'editor vanno a lato da 1024, e in orizzontale su uno schermo grande.**
     *
     * ⚠️ **L'orizzontale da 600 è la sua scelta D1** (voce G3 della revisione Setaccio): su uno
     * schermo col lato minore da 600 dp Android 16 ignora il verticale che l'editor chiede, e con
     * la sola soglia dei 1024 un tablet piccolo girato (960 per 600) o un pieghevole aperto in
     * orizzontale avevano la disposizione del telefono. Il telefono in orizzontale, col lato
     * minore sotto i 600, resta com'è, e così il tablet in verticale.
     */
    @Test
    fun `editor strumenti a lato da 1024 e in orizzontale su schermo grande`() {
        // Tablet in verticale: sotto i 1024 la scheda resta in basso.
        assertFalse(Adaptive.editorBeside(600, 960, 600))
        assertFalse(Adaptive.editorBeside(800, 1280, 800))
        assertFalse(Adaptive.editorBeside(1023, 1366, 1023))
        // Tablet piccolo e pieghevole in orizzontale: a lato.
        assertTrue(Adaptive.editorBeside(960, 600, 600))
        assertTrue(Adaptive.editorBeside(841, 701, 701))
        // Telefono in orizzontale: lato minore sotto i 600, quindi in basso.
        assertFalse(Adaptive.editorBeside(891, 411, 411))
        // Da 1024 sempre a lato.
        assertTrue(Adaptive.editorBeside(1024, 768, 768))
        assertTrue(Adaptive.editorBeside(1024, 1366, 1024))
        assertEquals(320, Adaptive.editorToolsWidth(true).value.toInt())
        assertEquals(0, Adaptive.editorToolsWidth(false).value.toInt())
    }

    @Test
    fun `dialoghi centrati al massimo 520 come il mockup`() {
        assertEquals(520, Adaptive.dialogMaxWidth.value.toInt())
        assertEquals(800, Adaptive.destinationMaxWidth(1024)!!.value.toInt())
        assertEquals(null, Adaptive.destinationMaxWidth(360))
    }

    @Test
    fun `anteprima Dimensioni e filigrana a lato solo da 1024`() {
        assertFalse(Adaptive.previewBeside(600))
        assertFalse(Adaptive.previewBeside(800))
        assertFalse(Adaptive.previewBeside(1023))
        assertTrue(Adaptive.previewBeside(1024))
        assertEquals(720, Adaptive.dialogBesideMaxWidth.value.toInt())
        assertEquals(720, Adaptive.sheetMaxWidth(1024)!!.value.toInt())
        assertEquals(null, Adaptive.sheetMaxWidth(360))
    }
}

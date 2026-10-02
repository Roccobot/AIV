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
    fun `da 600 il pannello laterale è possibile ma chiuso di serie`() {
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

    @Test
    fun `editor strumenti a lato solo da 1024`() {
        assertFalse(Adaptive.editorBeside(600))
        assertFalse(Adaptive.editorBeside(800))
        assertFalse(Adaptive.editorBeside(1023))
        assertTrue(Adaptive.editorBeside(1024))
        assertEquals(320, Adaptive.editorToolsWidth(1024).value.toInt())
        assertEquals(0, Adaptive.editorToolsWidth(800).value.toInt())
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

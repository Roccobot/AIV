package io.github.roccobot.aiv

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * The icon of a launcher action (6.10, `Shortcuts.iconBitmap`): the app's glyph drawn in the launcher
 * icon's colours, swapped, read from the pixels.
 *
 * ⚠️ `@GraphicsMode(NATIVE)` in a class of its own, because it reads pixels (`Rules.md`, § 'Quando si
 * scrive una prova, e quando no').
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IconaScorciatoiaTest {

    private val app = ApplicationProvider.getApplicationContext<Application>()

    /**
     * The folder's glyph is full in the middle, so the centre has the glyph's colour and a corner the
     * background's. With the glyph not drawn the centre would be the background.
     */
    @Test
    fun `il glifo copre il centro sul fondo chiaro`() {
        val icona = Shortcuts.iconBitmap(app, Icons.Filled.Folder)
        val centro = icona.width / 2
        assertEquals(app.getColor(R.color.launcher_background), icona.getPixel(centro, centro))
        assertEquals(app.getColor(R.color.launcher_foreground), icona.getPixel(1, 1))
    }

    /**
     * **'Faro' (6.20): his AIV folder on the folder's colour, with the sun cut out.**
     *
     * The folder's body takes the ink over the ground, the middle of the sun shows the ground: his
     * drawing has the sun and the mountain as holes, by the direction of its paths under `nonZero`.
     * ⚠️ Counter-tested with the glyph not drawn: the body of the folder is the ground.
     */
    @Test
    fun `la cartella sulla Home ha il glifo sul suo colore e il sole ritagliato`() {
        val fondo = FRONT_TINTS[10].light.toArgb()
        val inchiostro = Shortcuts.folderInk(fondo)
        val glifo = Glyphs.folderHome(app)
        val icona = Shortcuts.iconBitmap(app, glifo, fondo, inchiostro, 0.45f)
        // The glyph's viewport is 0.45 of the side and centred: a point of the viewport in pixels.
        val lato = icona.width
        val scala = lato * 0.45f / glifo.viewportWidth
        val inizio = (lato - glifo.viewportWidth * scala) / 2f
        fun punto(x: Float, y: Float) = icona.getPixel((inizio + x * scala).toInt(), (inizio + y * scala).toInt())
        // The canvas blends the translucent ink with its own rounding, one unit away from
        // `compositeColors` on some channel: the comparison allows that unit and no more.
        val atteso = ColorUtils.compositeColors(inchiostro, fondo)
        val corpo = punto(8f, 30f)
        listOf(16, 8, 0).forEach { shift ->
            val scarto = ((atteso shr shift) and 0xFF) - ((corpo shr shift) and 0xFF)
            assertTrue("il corpo della cartella: #%06X contro #%06X".format(atteso and 0xFFFFFF, corpo and 0xFFFFFF),
                scarto in -1..1)
        }
        assertEquals("il centro del sole", fondo, punto(19.82f, 18.74f))
        assertEquals("fuori dal disegno", fondo, icona.getPixel(1, 1))
    }
}

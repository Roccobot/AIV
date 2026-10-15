package io.github.roccobot.aiv

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
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
}

package io.github.roccobot.aiv

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The DNG, from the `5.80` (his request of 2026-10-12, answer `D1`).
 *
 * ⚠️ **The decoding is the system's** (see `Folder.EXTENSIONS`), so what can break here is the
 * declaration: a folder read from the disk must show the file, an address must be offered, and
 * the editor must save a JPEG copy instead of writing over the raw file.
 * ⚠️ **Counter-tested** by taking `dng` out of `Folder.EXTENSIONS`: the first check fails.
 */
@RunWith(AndroidJUnit4::class)
class DngTest {

    @Test
    fun `un DNG si mostra, si propone e si salva in una copia JPEG`() {
        assertTrue("una cartella letta dal disco mostra il DNG", Folder.isPicture("DNG"))
        assertTrue(
            "un indirizzo che finisce in .dng si propone",
            ImageActions.looksLikeImage(Uri.parse("https://esempio.it/scatti/IMG_0001.dng"))
        )
        assertFalse("il DNG non si sovrascrive", ImageEdit.canOverwrite("IMG_0001.dng"))
        assertEquals("ne esce una copia JPEG", "IMG_0001.jpg", ImageEdit.outputName("IMG_0001.dng"))
    }
}

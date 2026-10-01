package io.github.roccobot.aiv

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class IgnoredPsdBinTest {
    @Test fun `restored PSD is absent from the visible history but retained in backup`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.content.Context>()
        val psd = File(app.filesDir, "restored.PSD").apply { writeText("PSD") }
        val png = File(app.filesDir, "restored.png").apply { writeText("PNG") }
        val at = System.currentTimeMillis()
        try {
            History.add(app, at, listOf(psd.path, png.path))
            val visible = History.batches(app).flatMap { it.paths }
            assertFalse(psd.path in visible)
            assertTrue(png.path in visible)
            assertTrue(History.snapshot(app).any { it.path == psd.path })
        } finally { psd.delete(); png.delete() }
    }

    @Test fun `PSD in the bin remains on disk but is absent from its gallery`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dir = Bin.dir(app).apply { mkdirs() }
        val psd = File(dir, "ignored.PSD").apply { writeText("PSD") }
        val png = File(dir, "kept.png").apply { writeText("PNG") }
        try {
            val names = Bin.list(app).map { it.lastPathSegment }
            assertFalse("PSD was listed in the bin", psd.name in names)
            assertTrue(png.name in names)
            assertTrue("Ignoring must not delete the file", psd.exists())
        } finally { psd.delete(); png.delete() }
    }
}

package io.github.roccobot.aiv

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
class CartelleIncluseTest {
    @Test fun `i valori di fabbrica includono solo cartelle presenti anche se vuote`() {
        val root = Files.createTempDirectory("aiv-factory").toFile()
        try {
            val screenshots = File(root, "DCIM/Screenshots").apply { mkdirs() }
            val camera = Folder.Bucket(1, "Camera", 1, 0, null, "/storage/1234-5678/DCIM/Camera")
            val other = Folder.Bucket(2, "Other", 1, 0, null, "/sdcard/Other")
            assertEquals(setOf(screenshots.path, camera.path), defaultIncludedFolders(listOf(root), listOf(camera, other)))
        } finally { root.deleteRecursively() }
    }

    @Test fun `le due liste restano indipendenti e le incluse non ricevono il prestito`() {
        val selection = FolderSelection(hidden = setOf("Privato"), included = setOf("DCIM/Camera"))
        assertFalse(selection.visible("/sdcard/Privato"))
        assertTrue(selection.visible("/sdcard/Altro"))
        val included = selection.copy(mode = FolderMode.INCLUDED)
        assertTrue(included.visible("/sdcard/DCIM/Camera"))
        assertFalse(included.visible("/sdcard/DCIM/Camera/Altro"))
        assertFalse(included.visible("/sdcard/Altro", peeking = true))
        assertFalse(included.visible(null, peeking = true))
        assertEquals(selection, included.copy(mode = FolderMode.EXCLUDED))
    }

    @Test fun `solo la cartella mantiene visibili figli futuri e nomi simili`() {
        val selection = FolderSelection().exclude(listOf("/sdcard/Foo"), false)
        assertFalse(selection.visible("/storage/emulated/10/Foo"))
        assertTrue(selection.visible("/sdcard/Foo/Nuova"))
        assertTrue(selection.visible("/sdcard/Foo2"))
        assertEquals(listOf("Foo"), selection.covering("/sdcard/Foo"))
        assertTrue(selection.covering("/sdcard/Foo/Nuova").isEmpty())
        assertTrue(selection.remove(listOf("Foo")).visible("/sdcard/Foo"))
        val recursive = selection.exclude(listOf("/sdcard/Foo"), true)
        assertFalse(recursive.visible("/sdcard/Foo/Nuova"))
        assertTrue(recursive.exact.isEmpty())
    }

    @Test fun `i fratelli contano le directory vuote e i figli nuovi restano visibili`() {
        val parent = Files.createTempDirectory("aiv-siblings").toFile()
        try {
            val a = File(parent, "A").apply { mkdir() }
            val b = File(parent, "B").apply { mkdir() }
            File(parent, "documento.txt").writeText("file")
            var selection = FolderSelection()
            assertEquals(listOf(a, b), siblingFolders(a.path, selection))
            selection = selection.exclude(childFolders(parent).map { it.path }, true)
            assertFalse(selection.visible(a.path))
            assertFalse(selection.visible(b.path))
            assertTrue(siblingFolders(a.path, selection).isEmpty())
            val c = File(parent, "C").apply { mkdir() }
            assertTrue(selection.visible(c.path))
            assertEquals(listOf(a, b, c), siblingFolders(a.path, selection))
        } finally { parent.deleteRecursively() }
    }

    @Test fun `le destinazioni rispettano entrambe le modalita e le esclusioni esatte`() {
        fun bucket(path: String) = Folder.Bucket(path.hashCode().toLong(), path, 1, 0, null, path)
        val paths = listOf("/sdcard/X", "/sdcard/X/Y", "/sdcard/Camera", "/sdcard/bin")
        val buckets = paths.map(::bucket)
        val excluded = FolderSelection(exact = setOf("X"))
        assertEquals(paths.drop(1).dropLast(1), destinations(buckets, "/sdcard/bin", emptySet(), false, excluded).map { it.path })
        val included = excluded.copy(mode = FolderMode.INCLUDED, included = setOf("Camera", "bin"))
        assertEquals(listOf("/sdcard/Camera"), destinations(buckets, "/sdcard/bin", emptySet(), true, included).map { it.path })
    }

    @Test fun `le autorizzazioni distinguono scheda e memoria interna`() {
        val selection = FolderSelection(mode = FolderMode.INCLUDED, included = setOf("DCIM/Camera"))
        assertTrue(selection.visible("/storage/emulated/7/DCIM/Camera"))
        assertFalse(selection.visible("/storage/1234-5678/DCIM/Camera"))
        assertTrue(selection.copy(included = setOf("/storage/1234-5678/DCIM/Camera"))
            .visible("/storage/1234-5678/DCIM/Camera"))
    }
}

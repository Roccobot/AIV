package io.github.roccobot.aiv

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlinx.coroutines.runBlocking
import androidx.compose.runtime.CompositionLocalProvider
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
class CartelleDialoghiTest {
    @get:Rule val scene = createComposeRule()

    @Test
    @Config(shadows = [ArchivioAperto::class])
    fun `la schermata mostra solo autorizzate e non offre il prestito`() {
        runBlocking { Hint.COLUMNS.remember(ApplicationProvider.getApplicationContext()) }
        fun bucket(name: String) = Folder.Bucket(name.hashCode().toLong(), name, 1, 0, null, "/sdcard/$name")
        val selection = FolderSelection(FolderMode.INCLUDED, hidden = setOf("Private"), included = setOf("Camera"))
        scene.setContent { AivTheme(darkTheme = false) {
            Casa(listOf(bucket("Camera"), bucket("Other"), bucket("Private")), selection = selection, peeking = true)
        } }
        scene.onNodeWithText("Camera").assertExists()
        scene.onNodeWithText("Other").assertDoesNotExist()
        scene.onNodeWithText("Private").assertDoesNotExist()
        val app = ApplicationProvider.getApplicationContext<android.content.Context>()
        scene.onNodeWithContentDescription(app.getString(R.string.hub_open)).performClick()
        scene.onNodeWithText(app.getString(R.string.hub_peek)).assertDoesNotExist()
        scene.onNodeWithText(app.getString(R.string.hub_unpeek)).assertDoesNotExist()
    }

    @Test
    @Config(shadows = [ArchivioAperto::class])
    fun `il prestito della sola cartella non segna i discendenti`() {
        runBlocking { Hint.COLUMNS.remember(ApplicationProvider.getApplicationContext()) }
        fun bucket(name: String, path: String) = Folder.Bucket(name.hashCode().toLong(), name, 1, 0, null, path)
        scene.setContent { AivTheme(darkTheme = false) {
            Casa(listOf(bucket("Parent", "/sdcard/Parent"), bucket("Child", "/sdcard/Parent/Child")),
                selection = FolderSelection(exact = setOf("Parent")), peeking = true)
        } }
        scene.onNodeWithText("Parent").assertExists()
        scene.onNodeWithText("Child").assertExists()
        assertEquals(1, scene.onAllNodesWithText("∅").fetchSemanticsNodes().size)
    }

    @Test
    @Config(sdk = [28])
    fun `destinazioni incluse dalla vista di sistema richiedono sfoglia esplicito`() {
        scene.setContent { AivTheme(darkTheme = false) {
            CompositionLocalProvider(LocalDestLook provides DestLook(view = FolderView.TREE, folderMode = FolderMode.INCLUDED)) {
                DestinationDialog(R.string.dest_here, {}, {})
            }
        } }
        val app = ApplicationProvider.getApplicationContext<android.content.Context>()
        scene.onNodeWithText(app.getString(R.string.dest_browse)).assertExists()
    }

    @Test fun `il bivio nasconde solo il padre senza toccare il figlio`() {
        val parent = Files.createTempDirectory("aiv-parent").toFile()
        try {
            val child = File(parent, "Child").apply { mkdir() }
            var selected: FolderSelection? = null
            var dismissed = false
            scene.setContent { AivTheme(darkTheme = false) {
                SystemFolderDialog(parent.path, FolderSelection(), { selected = it }, { dismissed = true })
            } }
            scene.waitForIdle()
            scene.onNodeWithText("Hide").performClick()
            scene.onNodeWithText("Which folders do you want to hide?").assertExists()
            scene.onNodeWithText("Only ${parent.name}").performClick()
            assertTrue(dismissed)
            assertFalse(selected!!.visible(parent.path))
            assertTrue(selected!!.visible(child.path))
        } finally { parent.deleteRecursively() }
    }

    @Test fun `il trivio registra le sottocartelle anche se vuote`() {
        val parent = Files.createTempDirectory("aiv-parent").toFile()
        try {
            val a = File(parent, "A").apply { mkdir() }
            val b = File(parent, "B").apply { mkdir() }
            var selected: FolderSelection? = null
            scene.setContent { AivTheme(darkTheme = false) {
                SystemFolderDialog(parent.path, FolderSelection(), { selected = it }, {})
            } }
            scene.waitForIdle()
            scene.onNodeWithText("Hide").performClick()
            scene.onNodeWithText("Only the subfolders of ${parent.name}").performClick()
            assertTrue(selected!!.visible(parent.path))
            assertFalse(selected!!.visible(a.path))
            assertFalse(selected!!.visible(b.path))
            assertTrue(selected!!.visible(File(parent, "New").path))
        } finally { parent.deleteRecursively() }
    }

    @Test fun `autorizzare una cartella vuota avvisa e non modifica le escluse`() {
        val parent = Files.createTempDirectory("aiv-empty").toFile()
        try {
            val before = FolderSelection(FolderMode.INCLUDED, hidden = setOf("Private"), included = emptySet())
            var selected: FolderSelection? = null
            scene.setContent { AivTheme(darkTheme = false) {
                AuthorizeFolderDialog(parent.path, before, { selected = it }, {})
            } }
            scene.waitForIdle()
            scene.onNodeWithText("Add this folder to the authorized list?").assertExists()
            scene.onNodeWithText("Note: this folder currently has no displayable items. It will appear in grid or list mode when it contains at least one image or video.").assertExists()
            scene.onNodeWithText("Authorize folder").performClick()
            assertTrue(selected!!.visible(parent.path))
            assertEquals(before.hidden, selected!!.hidden)
        } finally { parent.deleteRecursively() }
    }
}

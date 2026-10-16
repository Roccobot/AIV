package io.github.roccobot.aiv

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 'Nuova cartella' nella `6.26` (rilievi D10 ed E29 della revisione della 6.25): un nome già preso
 * porta nella cartella che c'è, un errore vero risponde `null` (e le due finestre lo dicono con un
 * avviso), e `.` e `..` non si accettano.
 */
@RunWith(AndroidJUnit4::class)
class NuovaCartellaTest {

    @get:Rule val scena = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * **Un nome già preso dà la cartella che c'è, uno nuovo la crea, sotto un file non si può.**
     *
     * ⚠️ **Controprovata** con `mkdirs` da solo, com'era fino alla `6.25`: il nome già preso
     * risponde `null`.
     */
    @Test
    fun `un nome gia preso porta nella cartella che c'e`() {
        val casa = File(app.cacheDir, "nuova-cartella").also { it.mkdirs() }
        val presa = File(casa, "Viaggi").also { it.mkdirs() }
        val file = File(casa, "foto.jpg").also { it.writeBytes(ByteArray(10)) }

        assertEquals(presa.absolutePath, runBlocking { FileTree.makeFolder(casa, "Viaggi") }?.absolutePath)
        val nuova = runBlocking { FileTree.makeFolder(casa, "Mare") }
        assertTrue("la cartella nuova doveva esistere", nuova?.isDirectory == true)
        assertNull("sotto un file non si crea niente", runBlocking { FileTree.makeFolder(file, "Dentro") })
    }

    /**
     * **'Crea' si spegne con `.` e con `..`.**
     *
     * ⚠️ **Controprovata** togliendo i due confronti dal tasto: con `..` resta acceso.
     */
    @Test
    fun `punto e due punti non sono nomi`() {
        scena.setContent { AivTheme(darkTheme = false) { NewFolderDialog(onDismiss = {}, onCreate = {}) } }
        val crea = scena.onNodeWithText(app.getString(R.string.dest_create))
        val campo = scena.onNode(hasSetTextAction())

        campo.performTextInput("..")
        crea.assertIsNotEnabled()
        campo.performTextClearance()
        campo.performTextInput(".")
        crea.assertIsNotEnabled()
        campo.performTextClearance()
        campo.performTextInput("..Viaggi")
        crea.assertIsEnabled()
    }
}

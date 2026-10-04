package io.github.roccobot.aiv

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.window.DialogProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * In una finestra bassa un dialogo centrato si allarga, perché il suo contenuto entri senza
 * essere tagliato.
 *
 * ⚠️ **Nasce dalla voce `3.71-08` non approvata**: sul telefono in orizzontale la conferma 'Vuoi
 * nascondere...' era larga circa quanto il lato corto dello schermo, e l'ultimo tasto del testo
 * restava tagliato a metà. Monta un `AlertDialog` vero con `lowered` e `loweredWindow`, che è
 * quello che fanno le conferme.
 * ⚠️ **Il banco non applica la larghezza di serie dei dialoghi di Android**, quindi quella parte
 * si prova sulle proprietà della finestra; la larghezza del pannello si prova sul pannello.
 */
@RunWith(AndroidJUnit4::class)
class CentroLargoTest {

    @get:Rule
    val banco = createComposeRule()

    private var finestra: DialogProperties? = null

    private fun monta() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                val proprieta = loweredWindow {}
                finestra = proprieta
                AlertDialog(
                    onDismissRequest = {},
                    modifier = Modifier.lowered {}.testTag(PANNELLO),
                    properties = proprieta,
                    title = { Text("Titolo") },
                    text = { Text("Corto") },
                    confirmButton = { TextButton(onClick = {}) { Text("OK") } }
                )
            }
        }
        banco.waitForIdle()
    }

    private fun larghezza() =
        banco.onNodeWithTag(PANNELLO).getBoundsInRoot().let { it.right - it.left }.value

    @Test
    @Config(qualifiers = "w891dp-h411dp")
    fun `telefono in orizzontale il dialogo si allarga`() {
        monta()
        assertFalse("La finestra usa ancora la larghezza di serie", finestra!!.usePlatformDefaultWidth)
        assertTrue("Il pannello è rimasto stretto: ${larghezza()}", larghezza() >= 500f)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `telefono in verticale il dialogo resta quello di Material`() {
        monta()
        assertTrue("La finestra non usa la larghezza di serie", finestra!!.usePlatformDefaultWidth)
        assertTrue("Il pannello si è allargato in verticale: ${larghezza()}", larghezza() < 320f)
    }

    private companion object {
        const val PANNELLO = "pannello"
    }
}

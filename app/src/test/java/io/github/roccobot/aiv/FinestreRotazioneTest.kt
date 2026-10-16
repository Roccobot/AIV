package io.github.roccobot.aiv

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Il banco delle **finestre che devono sopravvivere a una rotazione** (rilievo E28).
 *
 * ⚠️⚠️ **NASCE DAL DIFETTO CHE LA `1.81` HA CORRETTO PER LE FINESTRE DEI LAVORI SUI FILE** (*ruotando
 * si chiude la finestra aperta e con lei quello che stavi scrivendo*), rimasto in due finestre:
 * 'Apri un indirizzo' perdeva l'indirizzo scritto, e l'albero di 'Sfoglia...' tornava alla radice.
 * ⚠️ **La rotazione si simula con `StateRestorationTester`**, che salva e ripristina lo stato come
 * fa l'attività ricreata: quello che passa è esattamente quello che `rememberSaveable` scrive.
 * Quello che il banco NON vede è la rotazione vera, che si guarda sul telefono.
 */
@RunWith(AndroidJUnit4::class)
class FinestreRotazioneTest {

    @get:Rule
    val banco = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /** **L'indirizzo scritto a mano resta nel campo dopo una rotazione.** */
    @Test
    fun `l'indirizzo scritto sopravvive alla rotazione`() {
        val prova = StateRestorationTester(banco)
        prova.setContent {
            AivTheme(darkTheme = false) {
                AddressDialog(recents = emptyList(), onOpen = {}, onOpenPage = {}, onForget = {}, onDismiss = {})
            }
        }
        banco.waitForIdle()
        banco.onNode(hasSetTextAction()).performTextReplacement(INDIRIZZO)
        banco.waitForIdle()

        prova.emulateSavedInstanceStateRestore()
        banco.waitForIdle()

        banco.onNode(hasSetTextAction() and hasText(INDIRIZZO)).assertExists(
            "Dopo la rotazione l'indirizzo scritto non è più nel campo"
        )
    }

    /** **Nell'albero di 'Sfoglia...' si resta nella cartella in cui si è dopo una rotazione.** */
    @Test
    fun `l'albero delle destinazioni resta nella cartella dopo la rotazione`() {
        val radici = FileTree.roots(app)
        assertTrue("Il banco non ha una memoria su cui provare", radici.isNotEmpty())
        radici.forEach { File(it, CARTELLA).mkdirs() }

        val prova = StateRestorationTester(banco)
        prova.setContent {
            AivTheme(darkTheme = false) {
                CompositionLocalProvider(
                    LocalDestLook provides DestLook(view = FolderView.TREE, folderMode = FolderMode.EXCLUDED)
                ) {
                    DestinationDialog(R.string.dest_here, {}, {}, from = emptySet())
                }
            }
        }
        // Con più memorie si parte dal loro elenco: si entra nella prima.
        if (radici.size > 1) {
            banco.waitUntil(ATTESA) {
                banco.onAllNodesWithText(radici.first().name).fetchSemanticsNodes().isNotEmpty()
            }
            banco.onNodeWithText(radici.first().name).performClick()
        }
        banco.waitUntil(ATTESA) { banco.onAllNodesWithText(CARTELLA).fetchSemanticsNodes().isNotEmpty() }
        banco.onNodeWithText(CARTELLA).performClick()
        banco.waitForIdle()
        assertTrue(
            "Toccata la cartella, il titolo non la nomina",
            banco.onAllNodesWithText(CARTELLA).fetchSemanticsNodes().isNotEmpty()
        )

        prova.emulateSavedInstanceStateRestore()
        banco.waitForIdle()

        assertTrue(
            "Dopo la rotazione l'albero non è più nella cartella in cui si era",
            banco.onAllNodesWithText(CARTELLA).fetchSemanticsNodes().size == 1 &&
                banco.onAllNodesWithText(radici.first().name).fetchSemanticsNodes().isEmpty()
        )
    }

    private companion object {
        const val INDIRIZZO = "https://esempio.it/foto"
        const val CARTELLA = "Viaggio"
    }
}

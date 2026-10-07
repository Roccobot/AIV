package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * **The folder the files come from cannot be their destination** (`4.45`, his note C: *disattivare
 * (sia in senso effettivo che graficamente) la cartella di origine dalla navigazione quando si deve
 * selezionare la destinazione*).
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️ It mounts the two views the destination window uses, with the source among the folders: a
 * touch on the source picks nothing, a touch on another folder picks it.
 * ⚠️⚠️ **COUNTER-PROVED** with the source left enabled (`enabled = true` in both views): the touch
 * on the source picks it, in the grid and in the list.
 */
@RunWith(AndroidJUnit4::class)
class OrigineTest {

    @get:Rule
    val banco = createComposeRule()

    @Test
    fun `nella griglia la cartella d'origine non si sceglie`() = prova(griglia = true)

    @Test
    fun `nell'elenco la cartella d'origine non si sceglie`() = prova(griglia = false)

    private fun prova(griglia: Boolean) {
        val scelte = mutableListOf<String>()
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.size(400.dp, 800.dp)) {
                    if (griglia) {
                        Covers(
                            folders = CARTELLE,
                            columns = 2,
                            peeked = emptySet(),
                            counted = false,
                            nameStyle = folderNameStyle(2),
                            colour = FolderColour.NONE,
                            tints = emptyMap(),
                            covers = emptyMap(),
                            onPick = { scelte += it.name },
                            off = setOf(portablePath(ORIGINE.path!!)),
                            onHide = {}
                        )
                    } else {
                        Rows(
                            folders = CARTELLE,
                            peeked = emptySet(),
                            counted = false,
                            size = TextSize.NORMAL,
                            colour = FolderColour.NONE,
                            tints = emptyMap(),
                            covers = emptyMap(),
                            onPick = { scelte += it.name },
                            off = setOf(portablePath(ORIGINE.path!!)),
                            onHide = {}
                        )
                    }
                }
            }
        }
        banco.onNodeWithText(ORIGINE.name).performClick()
        banco.onNodeWithText(ALTRA.name).performClick()
        banco.waitForIdle()
        assertEquals("solo l'altra cartella doveva essere scelta", listOf(ALTRA.name), scelte)
    }
}

private val ORIGINE = Folder.Bucket(id = 1, name = "Origine", pictures = 2, clips = 0, cover = null, path = "/storage/emulated/0/Origine")
private val ALTRA = Folder.Bucket(id = 2, name = "Altra", pictures = 2, clips = 0, cover = null, path = "/storage/emulated/0/Altra")
private val CARTELLE = listOf(ORIGINE, ALTRA)

package io.github.roccobot.aiv

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Il rail delle cartelle sul tablet: il trascinamento della lista e i due tasti in basso.
 *
 * ⚠️⚠️ **NASCE DAL COLLAUDO `3.40-01`**. La lista non tornava giù al tocco di una cartella,
 * ma il dito sulla maniglia la faceva solo traballare: ogni delta partiva dal lift letto
 * all'inizio del gesto, non da quello già accumulato. E Cestino / Impostazioni non erano
 * al centro della colonna.
 * ⚠️ **Controprova del trascinamento**: rimettendo `onLift(liftPx - dragAmount)` con il
 * `Float` del parametro, l'ultimo spostamento resta piccolo e la prova cade.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [29], qualifiers = "it")
class RailListaTest {

    @get:Rule
    val banco = createComposeRule()

    private fun testo(id: Int) =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    @Test
    fun `trascinare la maniglia sposta la lista e la tiene`() {
        var lift = 0f
        banco.setContent {
            AivTheme(darkTheme = false) {
                FolderRail(
                    buckets = emptyList(),
                    selected = null,
                    selection = FolderSelection(),
                    peeking = false,
                    colour = FolderColour.NONE,
                    tints = emptyMap(),
                    onPick = {},
                    onRead = {},
                    onSearch = {},
                    onBin = {},
                    onSettings = {},
                    width = 280.dp,
                    liftPx = lift,
                    onLift = { lift = it },
                    listIndex = 0,
                    listOffset = 0,
                    onListScroll = { _, _ -> },
                    modifier = Modifier.width(280.dp).height(640.dp)
                )
            }
        }
        banco.waitForIdle()

        banco.onNodeWithContentDescription(testo(R.string.folders_rail_move)).performTouchInput {
            down(center)
            // Tre scatti, non uno: col difetto resta solo l'ultimo, qui circa 30 px.
            moveBy(Offset(0f, -80f))
            moveBy(Offset(0f, -80f))
            moveBy(Offset(0f, -80f))
            up()
        }
        banco.waitForIdle()

        assertTrue(
            "Il lift non accumula il trascinamento (vale $lift)",
            lift > 160f
        )
    }

    @Test
    fun `cestino e impostazioni sono centrati nella colonna`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                FolderRail(
                    buckets = emptyList(),
                    selected = null,
                    selection = FolderSelection(),
                    peeking = false,
                    colour = FolderColour.NONE,
                    tints = emptyMap(),
                    onPick = {},
                    onRead = {},
                    onSearch = {},
                    onBin = {},
                    onSettings = {},
                    width = 280.dp,
                    liftPx = 0f,
                    onLift = {},
                    listIndex = 0,
                    listOffset = 0,
                    onListScroll = { _, _ -> },
                    modifier = Modifier
                        .testTag("rail")
                        .width(280.dp)
                        .height(640.dp)
                )
            }
        }
        banco.waitForIdle()

        val rail = banco.onNodeWithTag("rail").getUnclippedBoundsInRoot()
        val cestino = banco.onNodeWithText(testo(R.string.bin_title), substring = false)
            .getUnclippedBoundsInRoot()
        val impostazioni = banco.onNodeWithText(testo(R.string.hub_settings), substring = false)
            .getUnclippedBoundsInRoot()
        val mezzo = ((cestino.left + cestino.right) / 2 + (impostazioni.left + impostazioni.right) / 2) / 2
        val colonna = (rail.left + rail.right) / 2
        val scarto = abs((mezzo - colonna).value)
        assertTrue("La coppia non è al centro della colonna (scarto $scarto)", scarto < 2f)
    }
}

package io.github.roccobot.aiv

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
import org.junit.Assert.assertEquals
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
                    onHide = {},
                    onUnhide = {},
                    onSelectionChange = {},
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
    fun `il lift segue il dito in modo monotono`() {
        val campioni = mutableListOf<Float>()
        banco.setContent {
            var lift by remember { mutableFloatStateOf(0f) }
            val cartelle = List(40) { i ->
                Folder.Bucket(i.toLong(), "Cartella $i", 1, 0, null, "/tmp/c$i")
            }
            AivTheme(darkTheme = false) {
                FolderRail(
                    buckets = cartelle,
                    selected = null,
                    selection = FolderSelection(),
                    peeking = false,
                    colour = FolderColour.NONE,
                    tints = emptyMap(),
                    onPick = {},
                    onHide = {},
                    onUnhide = {},
                    onSelectionChange = {},
                    onRead = {},
                    onSearch = {},
                    onBin = {},
                    onSettings = {},
                    width = 280.dp,
                    liftPx = lift,
                    onLift = {
                        campioni += it
                        lift = it
                    },
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
            repeat(12) { moveBy(Offset(0f, -24f)) }
            up()
        }
        banco.waitForIdle()
        assertTrue("Nessun campione di trascinamento: $campioni", campioni.size >= 4)
        val passi = campioni.zipWithNext().map { (prima, dopo) -> dopo - prima }
        assertTrue("Il lift torna indietro: $campioni", passi.all { it >= -0.5f })
        val mossi = passi.filter { it > 0.5f }
        assertTrue("Pochi passi veri: $campioni", mossi.size >= 8)
        assertTrue(
            "Un passo non segue il dito: $mossi in $campioni",
            mossi.all { it >= 12f }
        )
        assertTrue(
            "Il lift non segue il dito (ultimo ${campioni.last()} su $campioni)",
            campioni.last() > 160f
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
                    onHide = {},
                    onUnhide = {},
                    onSelectionChange = {},
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
        val cerca = banco.onNodeWithContentDescription(testo(R.string.hub_search))
            .getUnclippedBoundsInRoot()
        assertTrue(
            "Cerca non è subito sopra il cestino (${cerca.bottom} vs ${cestino.top})",
            cerca.bottom.value <= cestino.top.value + 1f
        )
        val altezza = (rail.bottom - rail.top).value
        assertTrue(
            "Cerca è ancora troppo in alto (${cerca.top} su $altezza)",
            cerca.top.value > altezza * 0.45f
        )
    }

    @Test
    fun `il lift resta sul dito anche se il delta locale contiene lo spostamento`() {
        // Il difetto: a ogni evento il delta locale è lo spostamento del dito meno
        // quello già dato al nodo. Sommarlo lascia il lift fermo un evento sì e uno no.
        var lift = 0f
        var spostamentoNodo = 0f
        val contaminati = mutableListOf<Float>()
        repeat(8) {
            val dragAmount = -24f - spostamentoNodo
            val next = (lift - dragAmount).coerceIn(0f, 5000f)
            spostamentoNodo = dragAmount
            lift = next
            contaminati += lift
        }
        assertTrue(
            "La controprova non riproduce più il passo mancante: $contaminati",
            contaminati.zipWithNext().any { (prima, dopo) -> prima == dopo }
        )
        var dito = 0f
        val seguiti = mutableListOf<Float>()
        repeat(8) {
            dito -= 24f
            seguiti += railLiftForFinger(0f, 0f, dito, 5000f)
        }
        seguiti.zipWithNext().forEach { (prima, dopo) ->
            assertEquals(24f, dopo - prima, 0.01f)
        }
        assertEquals(192f, seguiti.last(), 0.01f)
    }

    @Test
    fun `il tocco lungo chiede di nascondere come sul telefono`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        org.robolectric.Shadows.shadowOf(app).grantPermissions(
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
        var nascosta = false
        val cartella = Folder.Bucket(7, "Vacanze", 3, 0, null, "/tmp/Vacanze")
        banco.setContent {
            AivTheme(darkTheme = false) {
                FolderRail(
                    buckets = listOf(cartella),
                    selected = null,
                    selection = FolderSelection(),
                    peeking = false,
                    colour = FolderColour.NONE,
                    tints = emptyMap(),
                    onPick = {},
                    onHide = { nascosta = true },
                    onUnhide = {},
                    onSelectionChange = {},
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
                    modifier = Modifier.width(280.dp).height(640.dp)
                )
            }
        }
        banco.waitForIdle()
        banco.onNodeWithText("Vacanze").performTouchInput { longClick() }
        banco.waitForIdle()
        assertTrue("Il tocco lungo non deve nascondere da solo", !nascosta)
        banco.onNodeWithText(testo(R.string.hide_folder_do)).assertExists()
        banco.onNodeWithText(testo(R.string.cancel)).assertExists()
        banco.onNodeWithText(testo(R.string.hide_folder_do)).performClick()
        banco.waitForIdle()
        assertTrue("Confermare non ha nascosto la cartella", nascosta)
    }

    /**
     * ⚠️ **Dalla `3.54` il nome va a capo sulle giunture camelCase** (sua richiesta): il testo
     * reso è quello di [camelBreak], con gli spazi a larghezza zero prima di ogni maiuscola
     * che segue una minuscola, e non il nome nudo. Due righe e l'ellissi restano quelle di
     * prima. Col nome nudo nel nodo (il codice della `3.53`) la seconda asserzione cade.
     */
    @Test
    fun `il nome lungo va a capo sulle giunture camelCase`() {
        val nome = "FotografieVacanzeEstateLunghissimeDavvero"
        val cartella = Folder.Bucket(8, nome, 3, 0, null, "/tmp/$nome")
        banco.setContent {
            AivTheme(darkTheme = false) {
                FolderRail(
                    buckets = listOf(cartella),
                    selected = null,
                    selection = FolderSelection(),
                    peeking = false,
                    colour = FolderColour.NONE,
                    tints = emptyMap(),
                    onPick = {},
                    onHide = {},
                    onUnhide = {},
                    onSelectionChange = {},
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
                    modifier = Modifier.width(280.dp).height(640.dp)
                )
            }
        }
        banco.waitForIdle()
        val spezzato = camelBreak(nome)
        assertEquals("Fotografie\u200BVacanze\u200BEstate\u200BLunghissime\u200BDavvero", spezzato)
        banco.onNodeWithText(spezzato).assertExists()
        banco.onNodeWithText(nome).assertDoesNotExist()
    }

}

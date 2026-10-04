package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The wide and tall layouts of 3.70 (user's request of 2026-10-04, mockups `Tablet_H` and
 * `Tablet_V`), on the real folder grid.
 *
 * ⚠️ It mounts `GridScreen` with the shape and the column a caller would pass: what is measured
 * is where the FAB's entries end up, where the header's chips end up, and how tall the folder
 * list is. All three compile just as well when they do nothing.
 */
@RunWith(AndroidJUnit4::class)
class LayoutLargoTest {

    @get:Rule
    val banco = createComposeRule()

    private fun rect(r: DpRect) = Rect(r.left.value, r.top.value, r.right.value, r.bottom.value)

    /** The grid of a folder, with the three callbacks that make the FAB (or the pill) appear. */
    private fun monta(shape: Adaptive.Shape, cartelle: Int = 3) {
        banco.setContent {
            AivTheme(darkTheme = false) {
                GridScreen(
                    title = TITOLO,
                    items = FOTO,
                    highlight = null,
                    onOpen = {},
                    onBack = {},
                    onChanged = {},
                    onSearch = {},
                    onBin = {},
                    onSettings = {},
                    onSearchHere = {},
                    shape = shape,
                    rail = if (shape == Adaptive.Shape.WIDE) {
                        WideRail(width = 240.dp, onStart = true) { m ->
                            LazyColumn(modifier = m.testTag(ELENCO)) {
                                items((1..cartelle).toList()) { Text("C$it", Modifier.height(48.dp)) }
                            }
                        }
                    } else {
                        null
                    }
                )
            }
        }
        banco.waitForIdle()
    }

    @Test
    fun `la forma dello schermo dal lato minore`() {
        // Phone upright, phone sideways, tablet sideways, tablet upright, a narrow phone sideways.
        assertEquals(Adaptive.Shape.PHONE, Adaptive.shape(411, 891, 411))
        assertEquals(Adaptive.Shape.WIDE, Adaptive.shape(891, 411, 411))
        assertEquals(Adaptive.Shape.WIDE, Adaptive.shape(1280, 800, 800))
        assertEquals(Adaptive.Shape.TALL, Adaptive.shape(800, 1280, 800))
        assertEquals(Adaptive.Shape.PHONE, Adaptive.shape(560, 320, 320))
        // Full screen only on the phone held sideways, the narrow one included.
        assertTrue(Adaptive.immersive(891, 411, 411))
        assertTrue(Adaptive.immersive(560, 320, 320))
        assertFalse(Adaptive.immersive(411, 891, 411))
        assertFalse(Adaptive.immersive(1280, 800, 800))
    }

    /**
     * **Sullo schermo largo i comandi del FAB sono nella pillola sotto il filtro, il nome e il
     * numero sono su una riga, e 'Seleziona tutto' è nella testa della colonna.**
     */
    @Test
    @Config(qualifiers = "w891dp-h411dp")
    fun `schermo largo pillola a destra e intestazione nella colonna`() {
        monta(Adaptive.Shape.WIDE)
        val cerca = rect(banco.onNodeWithContentDescription("Search").getBoundsInRoot())
        val cestino = rect(banco.onNodeWithContentDescription("Bin").getBoundsInRoot())
        val impostazioni = rect(banco.onNodeWithContentDescription("Settings").getBoundsInRoot())
        // A vertical pill on the right edge, in the FAB's order.
        assertTrue("La pillola non è a destra: $cerca", cerca.left > 891f - 80f)
        assertTrue("Le voci non sono in colonna", cerca.top < cestino.top && cestino.top < impostazioni.top)

        val nome = rect(banco.onNodeWithText(TITOLO).getBoundsInRoot())
        val numero = rect(banco.onNodeWithText("40 items").getBoundsInRoot())
        assertTrue("Nome e numero non sono su una riga: $nome / $numero", numero.top < nome.bottom && nome.top < numero.bottom)
        assertTrue("Il numero non segue il nome: $nome / $numero", numero.left >= nome.right)

        val tutte = rect(banco.onNodeWithText("Select all").getBoundsInRoot())
        assertTrue("'Seleziona tutto' non è nella colonna: $tutte", tutte.right <= 240f)
    }

    /** **L'elenco delle cartelle prende il 40% della colonna se è corto, il 70% se è lungo.** */
    @Test
    @Config(qualifiers = "w891dp-h411dp")
    fun `elenco ancorato fra il 40 e il 70 per cento`() {
        monta(Adaptive.Shape.WIDE, cartelle = 2)
        val alto = rect(banco.onRoot().getBoundsInRoot()).height
        val corto = rect(banco.onNodeWithTag(ELENCO).getBoundsInRoot())
        assertEquals(alto * RAIL_LIST_MIN, corto.height, 2f)
        assertEquals(alto, corto.bottom, 1f)
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp")
    fun `elenco lungo al 70 per cento`() {
        monta(Adaptive.Shape.WIDE, cartelle = 40)
        val alto = rect(banco.onRoot().getBoundsInRoot()).height
        val lungo = rect(banco.onNodeWithTag(ELENCO).getBoundsInRoot())
        assertEquals(alto * RAIL_LIST_MAX, lungo.height, 2f)
        assertEquals(alto, lungo.bottom, 1f)
    }

    /** **Sul tablet in verticale la pillola è in basso, col campo di ricerca in testa.** */
    @Test
    @Config(qualifiers = "w800dp-h1280dp")
    fun `tablet in verticale pillola in basso col campo`() {
        monta(Adaptive.Shape.TALL)
        val campo = rect(banco.onNodeWithText("Search in $TITOLO").getBoundsInRoot())
        val impostazioni = rect(banco.onNodeWithContentDescription("Settings").getBoundsInRoot())
        val cestino = rect(banco.onNodeWithContentDescription("Bin").getBoundsInRoot())
        assertTrue("La pillola non è in basso: $campo", campo.top > 1280f - 120f)
        assertTrue("L'ordine non è campo, Impostazioni, Cestino", campo.right < impostazioni.left && impostazioni.left < cestino.left)
        banco.onNodeWithContentDescription("Search").assertDoesNotExist()
    }

    /**
     * **Nel cestino la pillola porta le sue tre voci, e a cestino vuoto le due azioni sono
     * spente**, come nel menu del FAB (*se il FAB prevede scelte diverse o più scelte, la
     * pillola si adatta*).
     */
    @Test
    @Config(qualifiers = "w891dp-h411dp")
    fun `cestino largo con le sue voci`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                GridScreen(
                    title = "Bin",
                    items = emptyList(),
                    highlight = null,
                    onOpen = {},
                    onBack = {},
                    onChanged = {},
                    onSearch = {},
                    bin = true,
                    shape = Adaptive.Shape.WIDE
                )
            }
        }
        banco.waitForIdle()
        banco.onNodeWithContentDescription("History").assertIsEnabled()
        banco.onNodeWithContentDescription("Restore everything").assertIsNotEnabled()
        banco.onNodeWithContentDescription("Empty the bin").assertIsNotEnabled()
        banco.onNodeWithContentDescription("Settings").assertDoesNotExist()
    }

    /** **Sul telefono la pillola non c'è: resta il FAB.** */
    @Test
    fun `telefono senza pillola`() {
        monta(Adaptive.Shape.PHONE)
        banco.onNodeWithContentDescription("Settings").assertDoesNotExist()
        banco.onNodeWithContentDescription("Bin").assertDoesNotExist()
    }

    private companion object {
        const val TITOLO = "Lightroom"
        const val ELENCO = "elenco"
        val FOTO = (1..40).map { Uri.parse("file:///finta/$it.jpg") }
    }
}

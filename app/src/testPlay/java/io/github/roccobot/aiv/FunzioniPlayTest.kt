package io.github.roccobot.aiv

import android.Manifest
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/*
 * AIV Play without the functions that write on the files (5.10): the four keys of the file
 * pads, the bin and the 'Cartelle di sistema' view.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️ Runs only in the `play` variant: see `AccessoPlayTest`.
 */
@RunWith(AndroidJUnit4::class)
class FunzioniPlayTest {

    @get:Rule
    val banco = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private fun voce(id: Int) = app.getString(id)

    private fun quante(id: Int) = banco.onAllNodesWithContentDescription(voce(id)).fetchSemanticsNodes().size

    @Before
    fun prepara() {
        runBlocking {
            Hint.COLUMNS.remember(app)
            Hint.BIN_EMPTY.remember(app)
        }
        shadowOf(app as android.app.Application).grantPermissions(Manifest.permission.READ_MEDIA_IMAGES)
    }

    /**
     * **The two file pads keep what reads, and 'Elimina'**: 'Condividi', 'Info' and, since 5.20,
     * 'Elimina' in the viewer's menu, and in the selection sheet those with the three that choose.
     * 'Elimina' goes through the MediaStore (`MediaDelete`, his choices A1 and B1); the three that
     * write through `java.io.File` stay out, and since 5.30 'Lista', which copies the names, is out
     * too (his note on the 5.20 round). The same lists feed the page that reorders them.
     */
    @Test
    fun `i riquadri dei file non hanno i tre tasti che scrivono sui file ma hanno Elimina`() {
        val scrivono = setOf(PadKey.COPY, PadKey.MOVE, PadKey.RENAME)
        assertEquals(setOf(PadKey.SHARE, PadKey.INFO, PadKey.DELETE), MENU_KEYS.toSet())
        assertTrue("La selezione ha ancora $PICK_KEYS", PICK_KEYS.none { it in scrivono })
        assertTrue("La selezione non ha Elimina: $PICK_KEYS", PadKey.DELETE in PICK_KEYS)
        assertFalse("La selezione ha ancora Lista: $PICK_KEYS", PadKey.LIST in PICK_KEYS)
        assertEquals(6, PICK_KEYS.size)
        (scrivono + PadKey.LIST).forEach { assertFalse("$it c'è ancora", Store.allows(it)) }
        assertTrue("Elimina manca", Store.allows(PadKey.DELETE))
    }

    @Test
    fun `la vista Cartelle di sistema non c'e`() {
        assertEquals(listOf(FolderView.GRID, FolderView.LIST), Store.views)
    }

    /**
     * **On the real home, the extended pill has neither 'Cestino' nor 'Cartelle di sistema'**,
     * and 'Impostazioni' is there: the pill is drawn, and only the two entries are out.
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `la pillola di casa non ha il cestino ne la vista di sistema`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                CompositionLocalProvider(LocalPillLook provides PillLook(PhonePill.EXTENDED)) {
                    Box(modifier = Modifier.fillMaxSize()) { Casa(CARTELLE) }
                }
            }
        }
        banco.waitForIdle()
        assertEquals(1, quante(R.string.hub_settings))
        assertEquals(1, quante(R.string.hub_view_list))
        assertEquals(0, quante(R.string.bin_title))
        assertEquals(0, quante(R.string.hub_view_tree))
    }
}

/** A few folders, so the home shows its grid. */
private val CARTELLE = (1..4).map {
    Folder.Bucket(id = it.toLong(), name = "Cartella $it", pictures = 2, clips = 0, cover = null, path = "/storage/emulated/0/F$it")
}

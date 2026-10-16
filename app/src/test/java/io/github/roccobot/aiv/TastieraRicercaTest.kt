package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Il fuoco del campo della ricerca nella `6.27` (rilievo D6 della revisione della 6.25): lo prende
 * una volta per visita, e non di nuovo quando finisce una selezione fra i risultati.
 *
 * ⚠️ **Il fuoco è la spia della tastiera**: nel banco la tastiera non c'è, e il campo che prende il
 * fuoco è quello che sul telefono la apre.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [OmbraArchivio::class])
class TastieraRicercaTest {

    @get:Rule
    val banco = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * **Finita una selezione, il campo non riprende il fuoco.**
     *
     * ⚠️ **Controprovata** con l'effetto di prima, `LaunchedEffect(Unit)` dentro il ramo del
     * campo: la selezione toglie il campo dalla scena, la fine lo rimette e il fuoco torna.
     */
    @Test
    fun `la fine di una selezione non riapre la tastiera`() {
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    GridScreen(
                        title = "Risultati",
                        items = FOTO,
                        highlight = null,
                        onOpen = {},
                        onBack = {},
                        onChanged = {},
                        query = "mare",
                        onSearch = {},
                        onSearchHere = {}
                    )
                }
            }
        }
        banco.waitForIdle()
        banco.onNode(hasSetTextAction()).assertIsFocused()

        banco.onNodeWithContentDescription(app.getString(R.string.pick_actions))
            .performTouchInput { longClick() }
        banco.waitForIdle()
        banco.onNodeWithContentDescription(app.getString(R.string.pick_leave)).performClick()
        banco.waitForIdle()

        banco.onNode(hasSetTextAction()).assertIsNotFocused()
    }
}

/** Le immagini dei risultati finti. */
private val FOTO = (1..6).map { Uri.parse("file:///finta/ricerca-$it.jpg") }

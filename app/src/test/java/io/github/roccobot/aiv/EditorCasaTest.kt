package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Il banco dell'**editor di casa**, dalla `2.87`.
 *
 * ⚠️⚠️ **NASCE CON LA RISPOSTA `intera` A `d-originale-verso`**, e prima di lei questo editor non
 * aveva prove sue: le forme del ritaglio le disegna lo stesso pezzo nei due editor (`ShapeRow`),
 * ma quello che succede girando l'immagine no. Qui un quarto di giro **rifà** il rettangolo
 * sull'aspetto nuovo, e di là lo porta con sé, quindi una prova sul solo editor completo
 * misurerebbe una delle due strade.
 *
 * ⚠️ **Che cosa NON vede**: il file salvato, che vuole una decodifica vera e una scrittura su disco.
 * Qui si guarda quello che la schermata consegna al salvataggio, cioè il giro e il rettangolo.
 */
@RunWith(AndroidJUnit4::class)
// ⚠️ La grafica vera serve per DECODIFICARE il PNG dell'anteprima: senza, i comandi restano
// spenti e non c'è niente da toccare. È la stessa riga di `SviluppoTest`.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorCasaTest {

    @get:Rule
    val banco = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private fun testo(id: Int): String = app.getString(id)

    /**
     * **Dopo un quarto di giro 'Originale' è ancora l'immagine intera.**
     *
     * ⚠️⚠️ **È LA SUA RISPOSTA `intera`**, con la richiesta in chat (*'Originale' che si adatta
     * anche al verso della rotazione attuale*): girando, il verso resta quello di partenza, quindi
     * fino alla `2.86` 'Originale' su un'immagine larga girata di un quarto dava una cornice larga
     * dentro un'immagine alta.
     * ⚠️ **Si guarda quello che il salvataggio riceve**: la rotazione da sola accende 'Salva', quindi
     * il tasto acceso non direbbe niente, mentre il rettangolo sì.
     * ⚠️⚠️ **CONTROPROVATA** rimettendo il rapporto che segue il verso: il rettangolo consegnato
     * taglia tre quarti dell'immagine.
     */
    @Test
    fun `dopo un quarto di giro Originale e ancora l'immagine intera`() {
        var giri = -1
        var taglio: ImageEdit.Crop? = null
        banco.setContent {
            Scena(onSave = { turns, _, crop ->
                giri = turns
                taglio = crop
            })
        }
        pronta()
        banco.onNodeWithText(testo(R.string.editor_right)).performClick()
        banco.waitForIdle()
        banco.onNodeWithText(testo(R.string.editor_shape_original)).performClick()
        banco.waitForIdle()

        banco.onNodeWithText(testo(R.string.editor_save)).performClick()
        banco.waitForIdle()
        assertEquals("il quarto di giro doveva arrivare al salvataggio", 1, giri)
        assertNotNull("il tocco su 'Salva' doveva consegnare il rettangolo", taglio)
        assertTrue("'Originale' doveva tenere l'immagine intera: $taglio", taglio!!.whole)
    }

    /**
     * **Con 'Originale' i due versi si spengono, e con una proporzione tornano.**
     *
     * ⚠️ **È la seconda metà della stessa risposta** (*'Originale' ignora i due gettoni del
     * verso*): accesi, un tocco non cambierebbe la cornice e si leggerebbe come un comando rotto.
     * Qui i due versi sono una riga di chip scritti a parole, di là due icone: la condizione è la
     * stessa, e vive in due posti perché le due righe sono due.
     * ⚠️⚠️ **CONTROPROVATA** togliendo la condizione da questa riga: i due chip restano accesi e
     * la prima asserzione cade.
     */
    @Test
    fun `con Originale i due versi si spengono`() {
        banco.setContent { Scena() }
        pronta()
        val verticale = banco.onNodeWithText(testo(R.string.editor_tall))
        verticale.assertIsEnabled()

        banco.onNodeWithText(testo(R.string.editor_shape_original)).performClick()
        banco.waitForIdle()
        verticale.assertIsNotEnabled()
        banco.onNodeWithText(testo(R.string.editor_wide)).assertIsNotEnabled()

        banco.onNodeWithText("1:1").performClick()
        banco.waitForIdle()
        verticale.assertIsEnabled()
    }

    /**
     * Aspetta l'anteprima: finché non c'è, i comandi della scheda sono spenti.
     *
     * ⚠️ **Si aspetta un comando ACCESO e non il comando**: il chip c'è anche prima, spento, quindi
     * aspettarne la sola presenza lascerebbe partire la prova con l'immagine ancora da decodificare.
     */
    private fun pronta() {
        banco.waitUntil(5_000) {
            banco.onAllNodes(hasText(testo(R.string.editor_right)) and isEnabled())
                .fetchSemanticsNodes().isNotEmpty()
        }
        banco.waitForIdle()
    }

    @androidx.compose.runtime.Composable
    private fun Scena(
        onSave: (Int, Boolean, ImageEdit.Crop) -> Unit = { _, _, _ -> }
    ) {
        AivTheme(darkTheme = false) {
            Box(modifier = Modifier.fillMaxSize()) {
                EditorScreen(
                    uri = largo(),
                    busy = false,
                    marked = false,
                    marking = false,
                    hasMark = false,
                    onMark = {},
                    onMarkSetup = {},
                    resize = Resize.Plan(Resize.Mode.LONG, Resize.DEFAULT_PX),
                    // ⚠️ Spento: un ridimensionamento che rimpicciolisce accenderebbe 'Salva' a
                    // immagine intonsa, e il caso suo vive in `RidimensionaTest`.
                    saved = Resize.NONE,
                    resizing = false,
                    onResize = {},
                    onResizeDefault = {},
                    onSave = onSave,
                    onBack = {}
                )
            }
        }
    }

    /**
     * Un PNG **largo il doppio della sua altezza**, perché il verso di partenza sia 'Orizzontale'.
     *
     * ⚠️ **Il quadrato non serve qui**: conta come verticale e girato viene identico, quindi una
     * prova che misura una rotazione resterebbe verde con qualunque codice.
     */
    private fun largo(): Uri {
        val file = File(app.cacheDir, "casa-largo.png")
        if (!file.exists()) {
            val mappa = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
            mappa.eraseColor(Color.WHITE)
            file.outputStream().use { mappa.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return Uri.fromFile(file)
    }
}

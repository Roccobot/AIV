package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Il banco di prova della **riga del titolo** di una finestra: il titolo e i suoi comandi.
 *
 * ⚠️⚠️ **VUOLE LA GRAFICA VERA, E SENZA DI LEI NON MISURA NIENTE**: dalla `1.86` a scegliere fra
 * la pastiglia col testo e l'icona è la **larghezza** del titolo (la sua risposta a
 * `d-pill-soglia`), e con la grafica di serie del banco un testo misura una frazione di quello
 * che misura su un telefono. La prima stesura di questa prova viveva in [SalvataggioTest], dove
 * la grafica è quella di serie: la scena stretta dava lo stesso il testo, e il difetto era della
 * prova.
 * ⚠️ **Vive in una classe sua per questo**: `@GraphicsMode` vale per tutta la classe, e le altre
 * prove della finestra del nome non hanno niente da guadagnare a pagare la grafica vera.
 *
 * ⚠️ **Che cosa NON vede**: come la riga **appare**, cioè se una parola lunga stia stretta o se
 * l'icona si legga. Vede quale delle due forme il componente sceglie, che è la regola.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TitoloTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **La pastiglia del titolo passa all'icona quando il testo non entra, e non prima.**
     *
     * ⚠️⚠️ **È LA SUA RISPOSTA A `d-pill-soglia` (**misura**), MISURATA**: fino alla `1.85` a
     * decidere era il **conto** dei comandi in scena, quindi con uno solo il testo restava
     * scritto anche dove il titolo accanto doveva andare a capo. Qui la scena cambia **una cosa
     * sola**, la larghezza, e la forma del comando deve cambiare con lei.
     * ⚠️ **La misura si guarda dai due lati**: senza il caso largo, la prova passerebbe anche con
     * un componente che disegna sempre l'icona, cioè con la pastiglia sparita dall'app.
     * ⚠️ **Il caso di DUE comandi resta il conto**, ed è l'ultima asserzione: la larghezza è la
     * stessa del caso largo, quindi se la misura scavalcasse la sua istruzione della `1.82` là
     * comparirebbero due pastiglie.
     */
    @Test
    fun `il comando del titolo passa all'icona quando il testo non entra`() {
        val largo = mutableStateOf(true)
        val due = mutableStateOf(false)
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.width(if (largo.value) 400.dp else 120.dp)) {
                    TitleRow(
                        title = TITOLO,
                        commands = buildList {
                            add(TitleCommand(COMANDO, Glyphs.Extension) { })
                            if (due.value) add(TitleCommand(ALTRO, Glyphs.FolderDownload) { })
                        }
                    )
                }
            }
        }
        banco.waitForIdle()
        banco.onNodeWithText(COMANDO).assertExists()
        banco.onNodeWithContentDescription(COMANDO).assertDoesNotExist()

        largo.value = false
        banco.waitForIdle()
        banco.onNodeWithContentDescription(COMANDO).assertExists()
        banco.onNodeWithText(COMANDO).assertDoesNotExist()

        largo.value = true
        due.value = true
        banco.waitForIdle()
        banco.onNodeWithContentDescription(COMANDO).assertExists()
        banco.onNodeWithText(COMANDO).assertDoesNotExist()
    }
}

/**
 * Il titolo di prova della riga dei comandi.
 *
 * ⚠️ Sono parole scritte qui e non stringhe di risorsa perché a decidere è la **larghezza**: una
 * parola presa dalle risorse cambierebbe con la lingua del banco, e con lei l'esito della misura.
 */
private const val TITOLO = "Scarica il file"

/** Il comando di cui si guarda la forma. */
private const val COMANDO = "Destinazione"

/** Il secondo comando, che serve solo a far scattare il caso di due. */
private const val ALTRO = "Estensione"

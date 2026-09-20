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
import org.junit.Assert.assertTrue
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
                            add(TitleCommand(COMANDO, Glyphs.Extension, onTap = { }))
                            if (due.value) add(TitleCommand(ALTRO, Glyphs.FolderDownload, onTap = { }))
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

    /**
     * **Una pastiglia a due righe è larga quanto la sua parola più lunga, e va a capo.**
     *
     * ⚠️⚠️ **È IL DIFETTO ARRIVATO A LUI CON LA `2.81`** (voce `resize-finestra-2`: *'Rendi
     * predefinito' deve andare a capo*): il campo `lines` dava al testo il permesso di prendere
     * due righe, e un testo va a capo solo quando non ci sta. La pastiglia si dimensionava sul
     * proprio contenuto, quindi la frase restava su una riga e la pastiglia veniva larga quanto
     * lei, mangiandosi lo spazio del titolo, che è la seconda metà della stessa voce (*e
     * 'Ridimensiona' deve stare interamente senza troncature*).
     * ⚠️⚠️ **SI MISURANO TUTTE E DUE LE MISURE, E NON UNA**: la larghezza dice che la pastiglia si
     * è stretta sulla parola più lunga, l'altezza che il testo si è davvero spezzato in due. Con
     * una sola delle due, una pastiglia stretta col testo troncato passerebbe.
     * ⚠️ **La scena è LARGA**, perché il caso da guardare è quello in cui il posto ci sarebbe: è
     * là che la pastiglia si allargava.
     * ⚠️ **Controprovata togliendo la larghezza imposta in [TitleRow]**: le due misure coincidono
     * a una riga e a due, cioè la prova cade da tutte e due le parti.
     */
    @Test
    fun `la pastiglia a due righe si stringe sulla parola lunga`() {
        val righe = mutableStateOf(1)
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.width(400.dp)) {
                    TitleRow(
                        title = TITOLO,
                        commands = listOf(
                            TitleCommand(
                                text = LOCUZIONE,
                                glyph = Glyphs.Extension,
                                onTap = { },
                                lines = righe.value
                            )
                        )
                    )
                }
            }
        }
        banco.waitForIdle()
        val una = scritta()

        righe.value = 2
        banco.waitForIdle()
        val spezzato = scritta()

        assertTrue(
            "La pastiglia a due righe è larga quanto la frase intera" +
                " (una riga ${una.width}, due righe ${spezzato.width})",
            spezzato.width < una.width
        )
        assertTrue(
            "Il testo non si è spezzato in due righe" +
                " (una riga ${una.height}, due righe ${spezzato.height})",
            spezzato.height > una.height
        )
    }

    /**
     * La misura del **testo** dentro la pastiglia, e non quella del bottone che lo porta.
     *
     * ⚠️⚠️ **CON L'ALBERO FUSO QUESTA PROVA È VERDE A VUOTO, ED È LA CONTROPROVA CHE L'HA DETTO**:
     * un `Button` di Material fonde la semantica dei figli e ha una larghezza minima sua (58dp),
     * e sul banco i caratteri sono così stretti che tutte e due le forme cadono su quel minimo,
     * cioè le due misure coincidono qualunque cosa faccia il codice. Il testo invece si dimensiona
     * su quello che scrive, che è la cosa da guardare.
     */
    private fun scritta() = banco
        .onNodeWithText(LOCUZIONE, useUnmergedTree = true)
        .fetchSemanticsNode()
        .size
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

/**
 * Il comando di DUE parole, quello che deve andare a capo.
 *
 * ⚠️ **Le due parole sono lunghe in modo diverso**, ed è quello che rende il caso
 * misurabile: la pastiglia si stringe sulla più lunga, quindi con due parole uguali la
 * larghezza direbbe meno.
 */
private const val LOCUZIONE = "Rendi predefinito"

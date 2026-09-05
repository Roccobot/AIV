package io.github.roccobot.aiv

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Il banco di prova dei **tocchi**: apre l'app finta su una macchina senza telefono, la tocca
 * e verifica che il tocco arrivi dove deve.
 *
 * ⚠️⚠️ **NASCE DAL BLOCCO TOTALE DELLA `1.70`, ED È IL CONTROLLO CHE L'AVREBBE FERMATO.** Là un
 * velo a tutto schermo stava sempre nell'albero e rubava la hit-test a tutta l'app: il codice
 * era valido, `compileDebugKotlin` è passato senza una parola, e il difetto l'ha trovato
 * l'utente sul telefono, con l'app che si avviava e non rispondeva a niente. Nessun controllo
 * di questo repository poteva vederlo, perché tutti guardano il **testo** del programma e
 * quello era giusto.
 *
 * ⚠️ **Robolectric finge di essere Android sulla JVM**, quindi queste prove girano dove gira il
 * build: niente emulatore, niente `connectedAndroidTest`, nessun telefono agganciato. Si
 * lanciano con `./gradlew :app:testDebugUnitTest`.
 *
 * ⚠️⚠️ **CHE COSA IL BANCO NON PRENDE, e va detto invece di lasciarlo credere**: tutto quello
 * che dipende dalla resa vera (sfocature, ombre, animazioni come si percepiscono) e tutto
 * quello che dipende dall'apparecchio (permessi, provider di file, memoria grafica). Prende i
 * difetti di **struttura**, non quelli di **aspetto**. Chi legge un banco verde e ne ricava che
 * l'app è a posto sta leggendo una cosa che qui non è scritta.
 *
 * ⚠️ **Le prove montano [AivTheme] e non un albero finto**, ed è la ragione per cui valgono:
 * il velo dell'app e il cancello dei menu vivono là dentro, quindi un nodo che rubasse i tocchi
 * entrerebbe in scena da sé, senza che una prova debba ricordarsi di chiamarlo.
 */
@RunWith(AndroidJUnit4::class)
class TocchiTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * ⚠️ **La scena dei menu è un oggetto CONDIVISO**, quindi una prova che la lascia sporca
     * fa fallire la prossima invece della propria, e il conto non torna a chi legge.
     */
    @After
    fun pulisci() {
        MenuScene.clear()
    }

    /**
     * **Caso 1, quello che paga da solo il lavoro: l'app risponde a un tocco.**
     *
     * Fallisce su qualunque nodo che copra lo schermo e si prenda la hit-test, che è
     * esattamente la classe di difetti della `1.70`. ⚠️ **Non serve che quel nodo CONSUMI il
     * tocco**: fra fratelli sovrapposti la hit-test si ferma sul primo ramo che colpisce, e chi
     * sta sotto l'evento non lo riceve affatto.
     */
    @Test
    fun `senza menu aperti il tocco arriva all'app`() {
        var toccato = false
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(APP)
                        .clickable { toccato = true }
                )
            }
        }

        banco.onNodeWithTag(APP).performClick()

        assertTrue("Il tocco non è arrivato all'app: qualcosa copre lo schermo", toccato)
    }

    /**
     * **Caso 2: col menu aperto, il tocco fuori NON arriva all'app.**
     *
     * ⚠️ **Il difetto gemello della `1.70`, quello che non si vede**: da Android 12 la finestra
     * di un menu non è modale al tocco, quindi un dito fuori dal pannello arriva a tutte e due
     * le finestre, e l'app apre la riga o la cartella che stava sotto il dito. A mangiare quel
     * tocco è [MenuGuard], uno solo, dentro [AivTheme].
     * ⚠️ **Qui il menu si dichiara aperto invece di aprirlo davvero**: un menu vero vive in
     * un'altra finestra, e quello che questa prova guarda è la finestra dell'app, cioè proprio
     * il pezzo che il tocco non deve raggiungere.
     */
    @Test
    fun `col menu aperto il tocco non arriva all'app`() {
        var toccato = false
        MenuScene.enter(this)
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(APP)
                        .clickable { toccato = true }
                )
            }
        }

        banco.onNodeWithTag(APP).performClick()

        assertFalse("Il tocco ha attraversato il cancello dei menu", toccato)
    }

    /**
     * **Caso 3: il tocco sull'aria sopra una finestra centrata vale come il tocco fuori.**
     *
     * ⚠️⚠️ **È LA VOCE DELLA `1.70` SULL'ASIMMETRIA**: per far scendere del 15% una finestra
     * centrata, `Modifier.lowered` non la sposta, **gonfia la scatola** e ce la posa in fondo.
     * Sopra il pannello resta quindi una fascia trasparente che **appartiene alla finestra**, e
     * toccarla era toccare dentro, quindi la finestra si comportava da modale sopra e da
     * secondaria sotto.
     * ⚠️ **La prova comincia misurando l'aria**: se la scatola non fosse più alta del pannello
     * non ci sarebbe niente da toccare, e il resto passerebbe senza aver guardato nulla.
     */
    @Test
    fun `il tocco sull'aria sopra un pannello centrato lo chiude`() {
        var fuori = false
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .testTag(SCATOLA)
                            .lowered { fuori = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .testTag(PANNELLO)
                                .width(200.dp)
                                .height(80.dp)
                        )
                    }
                }
            }
        }

        val scatola = banco.onNodeWithTag(SCATOLA).fetchSemanticsNode().size
        val pannello = banco.onNodeWithTag(PANNELLO).fetchSemanticsNode().size
        val aria = scatola.height - pannello.height
        assertTrue("Nessuna aria sopra il pannello: non c'è niente da toccare", aria > 8)

        banco.onNodeWithTag(SCATOLA).performTouchInput {
            click(Offset(scatola.width / 2f, 4f))
        }
        assertTrue("Il tocco sull'aria non ha chiuso la finestra", fuori)

        fuori = false
        banco.onNodeWithTag(PANNELLO).performClick()
        assertFalse("Il tocco DENTRO il pannello ha chiuso la finestra", fuori)
    }

    /**
     * **Caso 4: su una modale vera, il tocco sull'aria non fa niente.**
     *
     * L'altra metà del caso 3, e senza di lui quel caso passerebbe anche con un nodo che
     * chiude sempre.
     */
    @Test
    fun `su una modale il tocco sull'aria non fa niente`() {
        var fuori = false
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .testTag(SCATOLA)
                            .lowered(null)
                    ) {
                        Box(
                            modifier = Modifier
                                .testTag(PANNELLO)
                                .width(200.dp)
                                .height(80.dp)
                        )
                    }
                }
            }
        }

        val scatola = banco.onNodeWithTag(SCATOLA).fetchSemanticsNode().size
        banco.onNodeWithTag(SCATOLA).performTouchInput {
            click(Offset(scatola.width / 2f, 4f))
        }

        assertFalse("Una modale si è chiusa toccando l'aria", fuori)
    }

    /**
     * **Caso 5: la finestra di una modale non si chiude toccando SOTTO il pannello.**
     *
     * ⚠️ **Quello è fuori dalla finestra, quindi non lo decide nessun nodo di Compose**: lo
     * decide `dismissOnClickOutside`, che il gestore delle finestre legge prima che l'app veda
     * qualcosa. Dalla `1.70` alla `1.72` `lowered(null)` dichiarava l'intenzione senza
     * applicarla, e le quattro finestre che chiedono un testo si chiudevano lo stesso: è la
     * voce `modali-quattro`, non approvata dall'utente.
     */
    @Test
    fun `le proprieta di una modale spengono la chiusura dal fuori`() {
        assertFalse(
            "Una modale accetta ancora la chiusura toccando fuori dalla finestra",
            loweredWindow(null).dismissOnClickOutside
        )
        assertTrue(
            "Una finestra secondaria non si chiude più toccando fuori",
            loweredWindow { }.dismissOnClickOutside
        )
    }
}

private const val APP = "app"
private const val SCATOLA = "scatola"
private const val PANNELLO = "pannello"

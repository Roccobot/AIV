package io.github.roccobot.aiv

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Il banco che guarda i **pixel** dei due tasti dello scorrimento: dietro il glifo non c'è niente.
 *
 * ⚠️⚠️ **NASCE DA UNA VOCE NON APPROVATA, ED È LA REGOLA** (`AIV/CLAUDE.md`, § '🧪 Quando si
 * scrive una prova, e quando no'): nel giro della `1.95` la voce `salti-tasti` è tornata indietro
 * perché ogni tasto stava dentro un tondo che lui non aveva chiesto (*elimina i tondi di
 * sfondo*), e quel tondo era anche la superficie su cui si vedeva lo **stato premuto** del
 * componente di Material, cioè la forma che lui ha descritto come un rettangolo.
 * ⚠️⚠️ **NESSUNA MISURA DI STRUTTURA POTEVA VEDERLO**: un fondo è del colore dentro un riquadro
 * che c'è comunque, quindi l'albero semantico è identico con e senza. Questa è la classe di
 * difetti che il banco ha imparato a vedere nella `1.85`.
 * ⚠️ **Che cosa NON vede**: come il tasto si comporta **premuto**, che è resa e vuole un dito
 * vero. A togliere quel comportamento è la stessa correzione, perché senza il componente di
 * Material non c'è più nessuno stato premuto da disegnare.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [OmbraArchivio::class])
/*
 * ⚠️ **La grafica vera vale per la classe**, come in `ColoreTest`: senza `NATIVE`
 * `captureToImage` restituisce un'immagine vuota, cioè una prova che passa con qualunque codice.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SaltiSfondoTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **Nel riquadro del tasto il fondo bianco resta bianco: si tinge il solo glifo.**
     *
     * ⚠️⚠️ **CONTA I PIXEL INVECE DI GUARDARE UN PUNTO**: dove cada l'inchiostro dipende dal
     * disegno del glifo, mentre un fondo pieno tinge quasi tutto il riquadro. La soglia è larga
     * apposta, perché la misura deve distinguere 'un tracciato' da 'una superficie' e non pesare
     * il tracciato.
     * ⚠️ **Controprovata rimettendo il difetto**: col tondo della `1.95` la quota misurata è
     * dell'82%, senza è del 10%.
     */
    @Test
    fun `il tasto non ha nessun fondo dietro il glifo`() {
        banco.mainClock.autoAdvance = false
        banco.setContent { Scena() }
        banco.mainClock.advanceTimeBy(RESPIRO_PIXEL)

        /*
         * ⚠️ **Il trascinamento accende i tasti**, che a riposo non sono nell'albero, e il clock
         * resta fermo perché dopo la quiete parte il congedo di due secondi.
         */
        val scena = banco.onRoot().fetchSemanticsNode().size
        banco.onRoot().performTouchInput {
            swipe(
                start = Offset(scena.width / 2f, scena.height * 0.8f),
                end = Offset(scena.width / 2f, scena.height * 0.2f),
                durationMillis = 300
            )
        }
        banco.mainClock.advanceTimeBy(JUMP_FADE_MS.toLong())

        val voce = ApplicationProvider.getApplicationContext<Context>().getString(R.string.jump_top)
        val mappa = banco.onAllNodesWithContentDescription(voce)[0].captureToImage().toPixelMap()
        var tinti = 0
        for (x in 0 until mappa.width) {
            for (y in 0 until mappa.height) {
                if (mappa[x, y] != Color.White) tinti++
            }
        }
        val quota = tinti.toFloat() / (mappa.width * mappa.height)
        assertTrue(
            "Il ${(quota * 100).toInt()}% del riquadro è tinto: dietro il glifo c'è un fondo",
            quota < SOGLIA_FONDO
        )
    }

    /**
     * Una lista nuda su fondo bianco, coi due tasti sopra.
     *
     * ⚠️ **Le righe sono vuote di proposito**: quello che si misura è il riquadro del tasto, e
     * del testo che ci passasse sotto sarebbe inchiostro che non c'entra con questa prova.
     */
    @androidx.compose.runtime.Composable
    private fun Scena() {
        AivTheme(darkTheme = false) {
            val state = rememberLazyListState()
            Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
                    items((1..RIGHE_PIXEL).toList()) {
                        Spacer(modifier = Modifier.fillMaxWidth().height(RIGA_PIXEL.dp))
                    }
                }
                JumpFabs(
                    state = state,
                    up = { state.jumpUpPixels() },
                    down = { state.jumpDownPixels() },
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }
}

/** Quanto si lascia comporre la scena prima di toccarla, col clock fermo. */
private const val RESPIRO_PIXEL = 1_000L

/** Quante righe ha la lista finta, e quanto è alta ognuna. */
private const val RIGHE_PIXEL = 200
private const val RIGA_PIXEL = 48

/**
 * Quanto del riquadro può essere tinto perché sia un glifo e non una superficie.
 *
 * ⚠️ **Sta in mezzo alle due misure e non vicino a una delle due**: misurando, un tondo pieno ne
 * tinge l'82% e il glifo da solo il 10%. Una soglia stretta misurerebbe lo spessore del disegno,
 * che cambia il giorno che il glifo cambia.
 */
private const val SOGLIA_FONDO = 0.5f

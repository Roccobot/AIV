package io.github.roccobot.aiv

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Il banco di prova dell'**entrata del FAB**: monta la schermata iniziale vera, la fa arrivare
 * con la transizione vera, e guarda dove sta e quanto è grande il FAB a ogni fotogramma.
 *
 * ⚠️⚠️ **NASCE DA UN DIFETTO ARRIVATO ALL'UTENTE DUE VOLTE** (giri della `1.71` e della `1.72`:
 * *se torno in home da una cartella arriva di nuovo da in basso a destra ... l'ingrandimento
 * dev'essere dal centro del FAB, quindi ingrandimento sì, movimento no*), e la prima cosa che ha
 * misurato è che la correzione della `1.72` **non correggeva niente**: con e senza la
 * `SizeTransform` che quella versione aveva tolto, il centro del FAB sta nello stesso punto a
 * ogni fotogramma, cifra per cifra. Il perché sta sul KDoc di [cambioSchermata].
 *
 * ⚠️⚠️ **MONTA LA SCHERMATA VERA E NON UNA MINIATURA, E LA DIFFERENZA È TUTTA**: una miniatura
 * scritta qui accanto misura la miniatura. La prima stesura di questa prova ne aveva una, e
 * passava anche con la `SizeTransform` rimessa: pareva una conferma, ed era una prova che
 * guardava un'altra cosa. È la stessa ragione per cui la transizione e la dichiarazione
 * dell'arrivo vivono in due funzioni dell'app ([cambioSchermata] e [ConArrivo]) invece di essere
 * riscritte qui.
 *
 * ⚠️ **Che cosa questa prova NON vede**: come l'entrata si **percepisce**, che dipende dalla resa
 * vera. Vede dove sta il FAB, quanto è grande e quando comincia a muoversi, che sono misure di
 * struttura. Un'entrata giusta al pixel e sbagliata all'occhio passa di qui senza che nessuno se
 * ne accorga.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [OmbraArchivio::class])
class EntrataTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **Il FAB cresce sul posto: il suo centro non si muove di un pixel.**
     *
     * ⚠️ **Le due asserzioni servono insieme**: senza la seconda, un'entrata che non facesse
     * niente passerebbe la prima a mani basse, perché un FAB fermo non si sposta.
     * ⚠️ **Il centro si misura sul riquadro RESO**, quindi la scala della molla lo lascia dov'è
     * per costruzione e un movimento vero lo sposta: è la sola misura che distingue le due cose.
     * ⚠️ **I fotogrammi in cui il FAB non c'è si saltano**: durante la transizione la schermata
     * si ricompone e per qualche fotogramma non espone niente all'albero semantico. Saltarli non
     * toglie nulla, perché quello che si guarda è il posto, non la presenza.
     */
    @Test
    fun `il FAB cresce sul posto durante il cambio di schermata`() {
        var schermo by mutableStateOf<Screen>(PARTENZA)
        banco.setContent { Scena(schermo) }
        banco.waitForIdle()
        banco.mainClock.autoAdvance = false
        schermo = ARRIVO

        var centro: Offset? = null
        var minima = Float.MAX_VALUE
        var massima = 0f
        repeat(FOTOGRAMMI) {
            banco.mainClock.advanceTimeByFrame()
            val riquadro = misura() ?: return@repeat
            val ora = riquadro.center
            val primo = centro ?: ora.also { centro = it }
            assertEquals("Il FAB si è spostato in orizzontale", primo.x, ora.x, TOLLERANZA)
            assertEquals("Il FAB si è spostato in verticale", primo.y, ora.y, TOLLERANZA)
            minima = minOf(minima, riquadro.width)
            massima = maxOf(massima, riquadro.width)
        }

        assertNotNull("Il FAB non è mai comparso: la prova non ha guardato niente", centro)
        assertTrue(
            "Il FAB non è mai cresciuto: l'entrata non ha giocato",
            massima - minima > CRESCITA
        )
    }

    /**
     * **L'entrata aspetta che la schermata sia arrivata, dalla `1.74`.**
     *
     * ⚠️⚠️ **È LA VOCE `fab-centro`, NON APPROVATA DUE VOLTE** (giro della `1.73`: *è l'animazione
     * con cui appare all'avvio o all'uscita da una cartella che è sbagliata*). Finché la
     * dissolvenza è in corso l'opacità del FAB si **moltiplica** per quella della schermata,
     * quindi una crescita giocata là sotto si vede per l'ultimo decimo: quello che restava era
     * un'apparizione seguita da un'oscillazione. Le due curve stanno su [rememberEntrata].
     * ⚠️ **Che cosa vede questa prova**: che durante la dissolvenza la misura del FAB **non
     * cambia**, e che dopo cresce. Togliendo l'attesa diventa rossa nei primi fotogrammi.
     * ⚠️ **Il tratto guardato non è la durata esatta della dissolvenza**, ed è voluto: la prova
     * dice 'per tutto questo tratto il FAB sta fermo', che resta vero anche il giorno che quella
     * durata cambia.
     */
    @Test
    fun `l'entrata aspetta che la schermata sia arrivata`() {
        var schermo by mutableStateOf<Screen>(PARTENZA)
        banco.setContent { Scena(schermo) }
        banco.waitForIdle()
        banco.mainClock.autoAdvance = false
        schermo = ARRIVO

        var partenza: Float? = null
        repeat(DISSOLVENZA) {
            banco.mainClock.advanceTimeByFrame()
            val larghezza = misura()?.width ?: return@repeat
            val prima = partenza ?: larghezza.also { partenza = it }
            assertEquals(
                "L'entrata è partita mentre la schermata stava ancora arrivando",
                prima,
                larghezza,
                TOLLERANZA
            )
        }
        val ferma = partenza
        assertNotNull("Il FAB non è mai comparso: la prova non ha guardato niente", ferma)

        var massima = 0f
        repeat(FOTOGRAMMI - DISSOLVENZA) {
            banco.mainClock.advanceTimeByFrame()
            massima = maxOf(massima, misura()?.width ?: 0f)
        }
        assertTrue(
            "Dopo la dissolvenza l'entrata non ha giocato",
            massima - ferma!! > CRESCITA
        )
    }

    /** Il riquadro reso del FAB, o `null` nei fotogrammi in cui la schermata si ricompone. */
    private fun misura() = banco
        .onAllNodesWithContentDescription(
            ApplicationProvider.getApplicationContext<Context>().getString(R.string.hub_open)
        )
        .fetchSemanticsNodes()
        .firstOrNull()
        ?.boundsInRoot
}

/**
 * La scena delle due prove: il cambio di schermata vero, con dentro la schermata iniziale vera.
 *
 * ⚠️ **Passa da [cambioSchermata] e da [ConArrivo]**, che sono le due funzioni dell'app: è la
 * ragione per cui queste prove misurano l'app e non se stesse.
 */
@Composable
private fun Scena(schermo: Screen) {
    AivTheme(darkTheme = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = schermo,
                transitionSpec = { cambioSchermata() },
                label = "schermata"
            ) { quale ->
                ConArrivo {
                    if (quale is Screen.Folders) {
                        SchermataIniziale()
                    } else {
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

/**
 * La schermata iniziale con gli argomenti minimi, cioè senza cartelle e senza permesso.
 *
 * ⚠️ **Quello che si guarda è il FAB, che c'è in ogni caso**: dipende dalla vista scelta e
 * non dai dati, quindi una casa vuota è la scena più piccola che lo contiene. Le cartelle vere
 * porterebbero le copertine, cioè il caricamento delle miniature, che su una macchina senza
 * telefono non porta niente in più e può soltanto fallire.
 */
@Composable
private fun SchermataIniziale() {
    FolderScreen(
        view = FolderView.GRID,
        columns = 3,
        counted = true,
        hidden = emptySet(),
        onHide = {},
        recents = emptyList(),
        onPick = {},
        onOpen = {},
        onOpenPage = {},
        onView = {},
        onForget = {},
        onSettings = {},
        onSearch = {},
        onBin = {},
        onColumns = {},
        listCount = true,
        listText = TextSize.NORMAL,
        treeHidden = false,
        treePictures = false,
        onListCount = {},
        onListText = {},
        onTreeHidden = {},
        onTreePictures = {},
        treePath = null,
        binOn = false,
        factFields = emptyList(),
        onTreePath = {},
        onTreeOpen = { _, _ -> },
        onBack = null,
        buckets = emptyList(),
        onRead = {}
    )
}

/** Da dove si arriva: una cartella aperta, cioè il caso che l'utente ha segnalato. */
private val PARTENZA = Screen.Grid(bucket = 1L, name = "prova")

/** Dove si arriva: la schermata iniziale, quella che porta il FAB. */
private val ARRIVO = Screen.Folders(forStart = false)

/**
 * Quanti fotogrammi si guardano in tutto.
 *
 * ⚠️ **Coprono l'entrata intera dopo l'attesa**: la dissolvenza dura 180 ms e la molla si assesta
 * in 247, quindi quaranta fotogrammi arrivano oltre la fine con margine.
 */
private const val FOTOGRAMMI = 40

/**
 * Per quanti fotogrammi la schermata sta ancora arrivando, e il FAB non si deve muovere.
 *
 * ⚠️ **Undici, cioè i 180 ms della dissolvenza**, ed è un tratto che deve restare fermo, non
 * l'istante esatto in cui l'entrata parte: misurato, la molla comincia a muoversi al sedicesimo
 * fotogramma, perché un `Transition` dichiara finita la propria corsa un paio di fotogrammi dopo
 * l'ultimo valore animato.
 */
private const val DISSOLVENZA = 11

/**
 * Quanto si concede al centro prima di chiamarlo movimento.
 *
 * ⚠️ **Mezzo pixel e non zero**: la scala arriva da una molla in virgola mobile, quindi i due
 * lati del riquadro si arrotondano indipendentemente e il centro può ballare di una frazione
 * senza che niente si sia spostato. Il movimento che questa prova cerca era una **diagonale
 * lunga quanto lo schermo**.
 */
private const val TOLLERANZA = 0.5f

/** Di quanto deve crescere il lato perché si possa dire che l'entrata ha giocato. */
private const val CRESCITA = 4f

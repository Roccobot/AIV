package io.github.roccobot.aiv

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
 * con la transizione vera, e guarda dove sta il tastino a ogni fotogramma.
 *
 * ⚠️⚠️ **NASCE DA UN DIFETTO ARRIVATO ALL'UTENTE DUE VOLTE** (giri della `1.71` e della `1.72`:
 * *se torno in home da una cartella arriva di nuovo da in basso a destra ... l'ingrandimento
 * dev'essere dal centro del FAB, quindi ingrandimento sì, movimento no*), e la prima cosa che ha
 * misurato è che la correzione della `1.72` **non correggeva niente**: con e senza la
 * `SizeTransform` che quella versione aveva tolto, il centro del tastino sta nello stesso punto
 * a ogni fotogramma, cifra per cifra. Il perché sta sul KDoc di [cambioSchermata].
 *
 * ⚠️⚠️ **MONTA LA SCHERMATA VERA E NON UNA MINIATURA, E LA DIFFERENZA È TUTTA**: una miniatura
 * scritta qui accanto misura la miniatura. La prima stesura di questa prova ne aveva una, e
 * passava anche con la `SizeTransform` rimessa: pareva una conferma, ed era una prova che
 * guardava un'altra cosa. È la stessa ragione per cui la transizione è stata estratta in una
 * funzione invece di essere riscritta qui.
 *
 * ⚠️ **Che cosa questa prova NON vede**: come l'entrata si **percepisce**, che dipende dalla resa
 * vera. Vede dove sta il tastino e quanto è grande, che sono misure di struttura. Un'entrata
 * giusta al pixel e sbagliata all'occhio passa di qui senza che nessuno se ne accorga.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [OmbraArchivio::class])
class EntrataTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **Il tastino cresce sul posto: il suo centro non si muove di un pixel.**
     *
     * ⚠️ **Le due asserzioni servono insieme**: senza la seconda, un'entrata che non facesse
     * niente passerebbe la prima a mani basse, perché un tastino fermo non si sposta.
     * ⚠️ **Il centro si misura sul riquadro RESO**, quindi la scala della molla lo lascia dov'è
     * per costruzione e un movimento vero lo sposta: è la sola misura che distingue le due cose.
     * ⚠️ **I fotogrammi in cui il tastino non c'è si saltano**: durante la transizione la
     * schermata si ricompone e per qualche fotogramma non espone niente all'albero semantico.
     * Saltarli non toglie nulla, perché quello che si guarda è il posto, non la presenza.
     */
    @Test
    fun `il FAB cresce sul posto durante il cambio di schermata`() {
        val etichetta = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.hub_open)
        var schermo by mutableStateOf<Screen>(Screen.Grid(bucket = 1L, name = "prova"))
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = schermo,
                        transitionSpec = { cambioSchermata() },
                        label = "schermata"
                    ) { quale ->
                        if (quale is Screen.Folders) SchermataIniziale() else Box(Modifier.fillMaxSize())
                    }
                }
            }
        }

        banco.waitForIdle()
        banco.mainClock.autoAdvance = false
        schermo = Screen.Folders(forStart = false)

        var centro: Offset? = null
        var minima = Float.MAX_VALUE
        var massima = 0f
        repeat(FOTOGRAMMI) {
            banco.mainClock.advanceTimeByFrame()
            val trovati = banco.onAllNodesWithContentDescription(etichetta).fetchSemanticsNodes()
            if (trovati.isEmpty()) return@repeat
            val riquadro = trovati.first().boundsInRoot
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
}

/**
 * La schermata iniziale con gli argomenti minimi, cioè senza cartelle e senza permesso.
 *
 * ⚠️ **Quello che si guarda è il tastino, che c'è in ogni caso**: dipende dalla vista scelta e
 * non dai dati, quindi una casa vuota è la scena più piccola che lo contiene. Le cartelle vere
 * porterebbero le copertine, cioè il caricamento delle miniature, che su una macchina senza
 * telefono non porta niente in più e può soltanto fallire.
 */
@androidx.compose.runtime.Composable
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

/** Quanti fotogrammi si guardano: la dissolvenza dura 180 ms e la molla si assesta in 247. */
private const val FOTOGRAMMI = 30

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

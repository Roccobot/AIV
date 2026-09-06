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
import androidx.compose.ui.geometry.Rect
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
 * Il banco di prova del **cambio di schermata**: monta la schermata iniziale vera, la fa arrivare
 * con la transizione vera, e guarda dove sta e quanto è grande il FAB a ogni fotogramma.
 *
 * ⚠️⚠️ **NASCE DA UN DIFETTO ARRIVATO ALL'UTENTE DUE VOLTE** (giri della `1.71` e della `1.72`:
 * *se torno in home da una cartella arriva di nuovo da in basso a destra ... l'ingrandimento
 * dev'essere dal centro del FAB, quindi ingrandimento sì, movimento no*), e la prima cosa che ha
 * misurato è che la correzione della `1.72` **non correggeva niente**: con e senza la
 * `SizeTransform` che quella versione aveva tolto, il centro del FAB sta nello stesso punto a
 * ogni fotogramma, cifra per cifra. Il perché sta sul KDoc di [cambioSchermata].
 *
 * ⚠️⚠️ **SI CHIAMAVA `EntrataTest` FINO ALLA `1.75`, e con l'entrata del FAB è cambiato quello
 * che misura**: quella versione ha tolto l'animazione d'ingresso su istruzione dell'utente
 * (*se ne va. Preferisco semplificare*), quindi la prova che guardava la sua attesa non ha più un
 * oggetto. Quello che resta, e che è il presidio vero, è che durante il cambio di schermata il
 * FAB **non si muove e non cambia misura**: è la forma esatta del difetto che gli è arrivato, e
 * copre sia una `SizeTransform` che tornasse sia un'animazione d'ingresso che rinascesse.
 *
 * ⚠️⚠️ **MONTA LA SCHERMATA VERA E NON UNA MINIATURA, E LA DIFFERENZA È TUTTA**: una miniatura
 * scritta qui accanto misura la miniatura. La prima stesura di questa prova ne aveva una, e
 * passava anche con la `SizeTransform` rimessa: pareva una conferma, ed era una prova che
 * guardava un'altra cosa. È la stessa ragione per cui la transizione vive in una funzione
 * dell'app ([cambioSchermata]) invece di essere riscritta qui.
 *
 * ⚠️ **Che cosa questa prova NON vede**: come il cambio di schermata si **percepisce**, che
 * dipende dalla resa vera. Vede dove sta il FAB e quanto è grande, che sono misure di struttura.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [OmbraArchivio::class])
class CambioSchermataTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **Il FAB sta fermo: né il suo centro né la sua misura cambiano di un pixel.**
     *
     * ⚠️ **Le due misure servono insieme**: il centro da solo lascia passare un ingrandimento
     * concentrico, la misura da sola lascia passare una traslazione. Il difetto segnalato era una
     * **diagonale lunga quanto lo schermo**, ma la voce che l'ha chiuso chiede che il FAB arrivi
     * con la schermata e basta, quindi il presidio è che non faccia niente.
     * ⚠️ **I fotogrammi in cui il FAB non c'è si saltano**: durante la transizione la schermata si
     * ricompone e per qualcuno non espone niente all'albero semantico. L'ultima asserzione copre
     * il caso in cui non comparisse mai, che è il modo in cui questa prova potrebbe mentire in
     * verde.
     */
    @Test
    fun `il FAB sta fermo durante il cambio di schermata`() {
        var schermo by mutableStateOf<Screen>(PARTENZA)
        banco.setContent { Scena(schermo) }
        banco.waitForIdle()
        banco.mainClock.autoAdvance = false
        schermo = ARRIVO

        var primo: Rect? = null
        repeat(FOTOGRAMMI) {
            banco.mainClock.advanceTimeByFrame()
            val riquadro = misura() ?: return@repeat
            val atteso = primo ?: riquadro.also { primo = it }
            assertEquals(
                "Il FAB si è spostato in orizzontale",
                atteso.center.x,
                riquadro.center.x,
                TOLLERANZA
            )
            assertEquals(
                "Il FAB si è spostato in verticale",
                atteso.center.y,
                riquadro.center.y,
                TOLLERANZA
            )
            assertEquals(
                "Il FAB ha cambiato misura: un'animazione d'ingresso è tornata",
                atteso.width,
                riquadro.width,
                TOLLERANZA
            )
        }

        assertNotNull("Il FAB non è mai comparso: la prova non ha guardato niente", primo)
        banco.mainClock.autoAdvance = true
        banco.waitForIdle()
        assertTrue(
            "A transizione finita il FAB non c'è: la schermata iniziale non è arrivata",
            misura() != null
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
 * La scena della prova: il cambio di schermata vero, con dentro la schermata iniziale vera.
 *
 * ⚠️ **Passa da [cambioSchermata]**, che è la funzione dell'app: è la ragione per cui questa
 * prova misura l'app e non se stessa.
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
                if (quale is Screen.Folders) {
                    SchermataIniziale()
                } else {
                    Box(Modifier.fillMaxSize())
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
 * ⚠️ **Coprono la dissolvenza intera con margine**: dura 180 ms, cioè undici fotogrammi, e la
 * transizione dichiara finita la propria corsa un paio di fotogrammi dopo l'ultimo valore
 * animato.
 */
private const val FOTOGRAMMI = 40

/**
 * Quanto si concede prima di chiamarlo movimento.
 *
 * ⚠️ **Mezzo pixel e non zero**: il riquadro reso arriva da numeri in virgola mobile, quindi i due
 * lati si arrotondano indipendentemente e il centro può ballare di una frazione senza che niente
 * si sia spostato. Il movimento che questa prova cerca era una **diagonale lunga quanto lo
 * schermo**.
 */
private const val TOLLERANZA = 0.5f

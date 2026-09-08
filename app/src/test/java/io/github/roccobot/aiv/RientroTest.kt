package io.github.roccobot.aiv

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Il banco di prova del **rientro nella schermata iniziale**: quello che si lascia si ritrova.
 *
 * ⚠️⚠️ **NASCE DA UNA VOCE NON APPROVATA FINO IN FONDO, ED È LA REGOLA** (`AIV/CLAUDE.md`
 * § '🧪 Quando si scrive una prova, e quando no': *un difetto che è arrivato a lui torna indietro
 * con la prova che lo avrebbe fermato, nella stessa versione della correzione*). La voce è
 * `scorri-torna` del giro della `1.91`, accettabile: *al ritorno in home ritorno al punto giusto
 * ma l'intestazione è attiva. Comportamento sbagliato: l'intestazione deve apparire solo se mi
 * trovo in cima alla griglia/lista*.
 *
 * ⚠️⚠️ **IL DIFETTO ERA UNA METÀ DEL RIPRISTINO, e questa prova guarda proprio quella**: la
 * `1.91` ha fatto sopravvivere lo **scorrimento** alla schermata, e l'apertura della fascia era
 * rimasta in un `remember` che il cambio di schermata portava via. Quindi la lista rientrava
 * dov'era e la fascia ripartiva aperta.
 *
 * ⚠️ **Che cosa questa prova NON vede**: come la fascia si **percepisce** mentre si chiude, che
 * dipende dalla resa vera. Vede dov'è la prima cartella prima di uscire e dopo il rientro, che
 * è una misura di struttura.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ArchivioAperto::class])
class RientroTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * La casa parte **come dopo il primo avvio**, cioè con l'onboarding delle colonne già visto.
     *
     * ⚠️⚠️ **SENZA QUESTA RIGA LA PROVA NON MISURA NIENTE, ed è costato un giro scoprirlo**: al
     * primo avvio la schermata iniziale mette in scena [HintVeil], che copre tutto e **consuma
     * il primo gesto** per archiviarsi. Misurato: lo stesso trascinamento non muoveva un pixel
     * la prima volta e chiudeva la fascia la seconda.
     * ⚠️ **Si scrive la preferenza vera invece di toccare il velo per congedarlo**: un tocco a
     * caso è un gesto che fa qualcosa, e quello che serve qui è la casa nello stato in cui la
     * trova chi usa l'app da più di un minuto.
     */
    @Before
    fun onboardingGiaVisto() {
        runBlocking { Hint.COLUMNS.remember(ApplicationProvider.getApplicationContext()) }
    }

    /**
     * **Uscendo e rientrando, l'intestazione è chiusa come la si è lasciata.**
     *
     * ⚠️⚠️ **LA PRIMA METÀ NON È UN CONTORNO**: senza la misura che dice che la fascia si è
     * davvero chiusa, il confronto dopo il rientro passerebbe anche con un gesto che non ha fatto
     * niente, cioè la prova direbbe di sì senza aver provato niente.
     * ⚠️ **Si guarda la prima cartella e non la fascia**: quello che l'utente vede è dove
     * comincia la griglia, e una fascia che si riapre la spinge giù di un terzo di schermo.
     * ⚠️⚠️ **LA SCENA MONTA IL `SaveableStateHolder` VERO, quello di `AivApp`**: è il meccanismo
     * che la `1.91` ha messo in mezzo, e il difetto viveva **esattamente** nella differenza fra
     * quello che passa di là e quello che non ci passa. Una scena che tenesse la schermata sempre
     * in composizione non avrebbe niente da ripristinare.
     * ⚠️ **Controprovata rimettendo il difetto**: riportando l'apertura della fascia in un
     * `remember` non salvato, la prova diventa rossa sull'ultima asserzione.
     */
    @Test
    fun `tornando in home l'intestazione resta chiusa`() {
        var dentro by mutableStateOf(false)
        banco.setContent {
            val stanze = rememberSaveableStateHolder()
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (dentro) {
                        stanze.SaveableStateProvider(CARTELLA) { Box(Modifier.fillMaxSize()) }
                    } else {
                        stanze.SaveableStateProvider(HOME) { Casa(CARTELLE) }
                    }
                }
            }
        }
        banco.waitForIdle()

        val alto = banco.onRoot().fetchSemanticsNode().size.height.toFloat()
        val largo = banco.onRoot().fetchSemanticsNode().size.width.toFloat()
        val aperta = dove()

        /*
         * ⚠️ **Il trascinamento ha coordinate esplicite e dura a lungo**, come nella prova
         * dell'intestazione di una cartella: `swipeUp()` nudo parte dal bordo di sotto del nodo,
         * dove non c'è nessuna griglia sotto il dito, e un gesto veloce porterebbe la lista molto
         * oltre la chiusura della fascia, cioè misurerebbe l'inerzia.
         */
        banco.onRoot().performTouchInput {
            swipe(
                start = Offset(largo / 2f, alto * DA),
                end = Offset(largo / 2f, alto * (DA - HEADER_SHARE)),
                durationMillis = LENTO
            )
        }
        banco.waitForIdle()

        val chiusa = dove()
        assertTrue(
            "La prima cartella era a ${aperta}px e adesso è a ${chiusa}px: l'intestazione non " +
                "si è chiusa, quindi qui non ci sarebbe niente da ritrovare",
            aperta - chiusa > alto * HEADER_SHARE * QUASI_TUTTO
        )

        dentro = true
        banco.waitForIdle()
        dentro = false
        banco.waitForIdle()

        assertEquals(
            "Tornando in home la prima cartella è scesa: l'intestazione si è riaperta da sé",
            chiusa,
            dove(),
            FERMO
        )
    }

    /** Dove comincia la prima cartella, in pixel dal bordo di sopra. */
    private fun dove(): Float {
        val trovata = banco.onAllNodesWithText(PRIMA).fetchSemanticsNodes().firstOrNull()
        return requireNotNull(trovata) {
            "La cartella '$PRIMA' non è in scena: la schermata iniziale non ha composto l'elenco"
        }.boundsInRoot.top
    }

    private val app: Context get() = ApplicationProvider.getApplicationContext()
}

/**
 * Le cartelle finte della casa.
 *
 * ⚠️ **Abbastanza da riempire lo schermo e avanzare**: con poche, chiudere l'intestazione non
 * lascerebbe niente sotto e la griglia non avrebbe una posizione da ritrovare.
 * ⚠️ **Senza copertina**: il caricatore delle miniature su una macchina senza telefono non
 * risponde, e quello che questa prova legge è il **nome**, che la cella scrive comunque.
 */
private val CARTELLE = (1..12).map {
    Folder.Bucket(id = it.toLong(), name = "Cartella $it", pictures = 3, clips = 0, cover = null)
}

/** Il nome della cartella che si misura: la prima in ordine alfabetico, quindi sempre in cima. */
private const val PRIMA = "Cartella 1"

/** La chiave con cui la schermata iniziale mette da parte il suo stato, come in `Screen.saveKey`. */
private const val HOME = "folders:false"

/** La chiave dell'altra schermata: serve solo a portare la casa fuori dalla composizione. */
private const val CARTELLA = "grid:1"

/**
 * Da che altezza parte il trascinamento, in frazione di schermo.
 *
 * ⚠️ **Sette decimi e non il bordo**: il dito deve partire sopra la griglia, e in fondo allo
 * schermo ci sono il margine della schermata e i rientri di sistema.
 */
private const val DA = 0.7f

/**
 * Quanto dura il trascinamento simulato.
 *
 * ⚠️ **Lungo di proposito**: la velocità che il banco ricava dagli ultimi campioni diventa
 * inerzia, e un gesto veloce porterebbe la griglia oltre la chiusura della fascia.
 */
private const val LENTO = 400L

/**
 * Quanta parte della fascia deve risalire perché la si chiami chiusa.
 *
 * ⚠️ **Non tutta**: uno scorrimento simulato spende quello che ha, e fra la fine del gesto e
 * l'inerzia il numero esatto dipende dalla velocità che il banco produce.
 */
private const val QUASI_TUTTO = 0.9f

/**
 * Quanto può muoversi una cartella e continuare a dirsi ferma, in pixel.
 *
 * ⚠️ **Un pixel e non zero**: la posizione arriva da una misura in virgola mobile. Quello che deve
 * fallire è una fascia che si riapre, cioè un salto di un terzo di schermo.
 */
private const val FERMO = 1f

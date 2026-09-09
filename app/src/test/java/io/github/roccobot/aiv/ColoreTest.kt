package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Il banco del **colore di una cartella fuori dall'intestazione**, nato con la `1.87`.
 *
 * ⚠️⚠️ **GUARDA I PIXEL PERCHÉ NESSUNA MISURA DI STRUTTURA POTEVA VEDERLO**: gli stili di
 * [FolderColour] non aggiungono un nodo che si possa cercare per nome, aggiungono del colore
 * dentro un riquadro che c'era già. È la classe di difetti che il banco ha imparato a vedere
 * nella `1.85` (`AIV/CLAUDE.md`, § 'Quando si scrive una prova, e quando no').
 * ⚠️ **Che cosa NON vede**: se quel colore si **distingue** dalla copertina che ha sotto, che
 * dipende dall'immagine e dall'occhio. Vede che c'è, e che senza stile non c'è.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [OmbraArchivio::class])
/*
 * ⚠️ **La grafica vera vale per la classe**, come in `IntestazioneTest`: senza `NATIVE`
 * `captureToImage` restituisce un'immagine vuota, cioè una prova che passa con qualunque codice.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ColoreTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **Con lo stile 'filetto' il colore della cartella arriva sulla copertina.**
     *
     * ⚠️ **Conta i pixel invece di cercarli in un punto**: dove cada il filetto dipende da quanto
     * è larga la cella, cioè da una geometria che questa prova non deve ricopiare. Quello che
     * misura è che una striscia di quel colore esista, ed è larga almeno quanto una riga.
     */
    @Test
    fun `il filetto porta il colore della cartella sulla copertina`() {
        banco.setContent { Scena(FolderColour.EDGE) }
        banco.waitForIdle()

        val tinti = tinti()
        assertTrue(
            "Di pixel del colore della cartella ne ho contati $tinti: il filetto non è arrivato",
            tinti >= RIGA
        )
    }

    /**
     * **Senza stile la copertina resta quella di prima.**
     *
     * ⚠️⚠️ **È LA METÀ CHE FA DELLA PRIMA UNA MISURA**: senza, una prova che conta pixel colorati
     * passerebbe anche se il colore lo mettesse qualcos'altro. E [FolderColour.NONE] è il valore
     * di fabbrica, quindi questa guarda anche che aggiornando l'app non cambi niente da sé.
     */
    @Test
    fun `senza stile la copertina non porta colore`() {
        banco.setContent { Scena(FolderColour.NONE) }
        banco.waitForIdle()

        assertEquals(
            "La copertina porta il colore della cartella con lo stile spento",
            0,
            tinti()
        )
    }

    /**
     * **Una cartella senza colore scelto prende l'accento dell'app.**
     *
     * ⚠️⚠️ **È LA SUA CORREZIONE ALLA `1.87`** (*'di fabbrica' è il colore di accento del tema,
     * che è comunque un colore, e se attivassi il filetto dovrebbero essere tutti di quel
     * colore*): là il disegno era saltato quando la tinta mancava, quindi lo stile si vedeva
     * solo sulle cartelle già segnate. Il difetto non lo prendeva nessuna delle due prove
     * sopra, perché tutte e due montano una cartella **con** la sua tinta.
     */
    @Test
    fun `senza colore scelto il filetto porta l'accento dell'app`() {
        /*
         * ⚠️⚠️ **L'ACCENTO SI LEGGE DENTRO `AivTheme` E NON FUORI, e la prima stesura di questa
         * prova sbagliava proprio qui**: scritta prima di [Scena], la riga leggeva il
         * `MaterialTheme` di serie invece di quello dell'app, quindi cercava un colore che nella
         * scena non c'è e falliva con la correzione già in vigore.
         * ⚠️ **E non si scrive il numero qui**: ricopiare l'accento nella prova vorrebbe dire
         * misurare una costante invece del tema.
         */
        var accento = Color.Unspecified
        banco.setContent {
            Scena(FolderColour.EDGE, segnata = false) { accento = it }
        }
        banco.waitForIdle()

        val quanti = quantiSono(accento)
        assertTrue(
            "Di pixel dell'accento ne ho contati $quanti: senza una tinta scelta il filetto " +
                "non si disegna, e lo stile si vedrebbe solo su qualche cartella",
            quanti >= RIGA
        )
    }

    /**
     * **La tinta di una cartella segue il tema scelto DENTRO l'app.**
     *
     * ⚠️⚠️ **NASCE CON LE COPPIE DELLA `1.89`, ed è la prova che le presidia**: da quella
     * versione una tinta è due colori, e a sceglierli è [LocalAivLight], che può dire il
     * contrario della configurazione di sistema. Un `colorResource` o una costante sola
     * darebbero il colore dell'altro tema senza nessun errore.
     * ⚠️ **La controprova è dentro**: non conta solo che ci sia la variante scura, ma che di
     * quella chiara non resti **nessun** pixel. Con la lettura sbagliata la prima metà potrebbe
     * ancora passare per caso su due tinte simili, la seconda no.
     */
    @Test
    fun `la tinta di una cartella segue il tema dell'app`() {
        banco.setContent { Scena(FolderColour.EDGE, chiaro = false) }
        banco.waitForIdle()

        val scuri = quantiSono(FRONT_TINTS[QUALE].dark)
        assertTrue(
            "Di pixel della variante scura ne ho contati $scuri: sul tema scuro il filetto " +
                "non porta la sua metà della coppia",
            scuri >= RIGA
        )
        assertEquals(
            "Sul tema scuro la copertina porta la variante chiara della tinta",
            0,
            quantiSono(FRONT_TINTS[QUALE].light)
        )
    }

    /** Quanti pixel della scena sono del colore scelto per la cartella. */
    private fun tinti(): Int = quantiSono(TINTA)

    /** Quanti pixel della scena sono di un colore dato. */
    private fun quantiSono(quale: Color): Int {
        val mappa = banco.onRoot().captureToImage().toPixelMap()
        var quanti = 0
        for (x in 0 until mappa.width) {
            for (y in 0 until mappa.height) {
                if (mappa[x, y] == quale) quanti++
            }
        }
        return quanti
    }

    /**
     * Una cartella sola, in una scena larga quanto una copertina.
     *
     * ⚠️ **Senza copertina e senza conto**: la miniatura vera vorrebbe un caricamento che su una
     * macchina senza telefono non arriva, e il conto sotto il nome qui non c'entra. Quello che
     * serve è il riquadro, che c'è comunque col simbolo di ripiego.
     */
    @Composable
    private fun Scena(
        colour: FolderColour,
        segnata: Boolean = true,
        /** Quale dei due temi dell'app monta la scena. */
        chiaro: Boolean = true,
        /** L'accento del tema dell'app, che solo chi è dentro [AivTheme] può leggere. */
        onAccento: (Color) -> Unit = {}
    ) {
        AivTheme(darkTheme = !chiaro) {
            onAccento(MaterialTheme.colorScheme.primary)
            Box(modifier = Modifier.size(LATO).background(Color.White)) {
                Covers(
                    folders = listOf(CARTELLA),
                    columns = 1,
                    counted = false,
                    nameStyle = folderNameStyle(1),
                    colour = colour,
                    tints = if (segnata) mapOf(CARTELLA.id to QUALE) else emptyMap(),
                    // ⚠️ Vuota e scritta, perché dalla `2.01` quel parametro non ha un valore
                    // di serie: qui si misura il colore, e nessuna cartella ha una copertina
                    // scelta a mano. Il perché vive sul parametro, in `FolderScreen.kt`.
                    covers = emptyMap(),
                    onPick = {},
                    onHide = {}
                )
            }
        }
    }
}

/** La cartella finta: un identificatore, un nome, e nessuna copertina da caricare. */
private val CARTELLA = Folder.Bucket(id = 7L, name = "Cartella", pictures = 3, clips = 0, cover = null)

/** Quale delle sedici tinte: la prima, che è il grigio-blu scuro delle sue. */
private const val QUALE = 0

/**
 * Il colore che la scena deve portare, cioè quello che [QUALE] sceglie.
 *
 * ⚠️ **La variante chiara**, perché le prove che lo contano montano il tema chiaro: dalla
 * `1.89` una tinta è una coppia, e prendere il valore sbagliato darebbe una prova che non trova
 * mai niente.
 */
private val TINTA = FRONT_TINTS[QUALE].light

/** Quanto è larga la scena: una copertina sola, senza andare a cercare la geometria vera. */
private val LATO = 200.dp

/**
 * Quanti pixel valgono 'una striscia c'è', al minimo.
 *
 * ⚠️ **Molto meno della larghezza vera**, che dipende dai rientri della griglia e dal
 * ritaglio degli angoli: la soglia serve a separare 'una riga di colore' da 'qualche pixel di
 * antialiasing', non a misurare il filetto.
 */
private const val RIGA = 100

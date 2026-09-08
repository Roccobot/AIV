package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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

    /** Quanti pixel della scena sono del colore scelto per la cartella. */
    private fun tinti(): Int {
        val mappa = banco.onRoot().captureToImage().toPixelMap()
        var quanti = 0
        for (x in 0 until mappa.width) {
            for (y in 0 until mappa.height) {
                if (mappa[x, y] == TINTA) quanti++
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
    private fun Scena(colour: FolderColour) {
        AivTheme(darkTheme = false) {
            Box(modifier = Modifier.size(LATO).background(Color.White)) {
                Covers(
                    folders = listOf(CARTELLA),
                    columns = 1,
                    counted = false,
                    nameStyle = folderNameStyle(1),
                    colour = colour,
                    tints = mapOf(CARTELLA.id to QUALE),
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

/** Il colore che la scena deve portare, cioè quello che [QUALE] sceglie. */
private val TINTA = FRONT_TINTS[QUALE]

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

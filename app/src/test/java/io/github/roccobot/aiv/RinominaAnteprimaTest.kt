package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Il banco di prova dell'**anteprima della rinomina**: quali righe mostra, e come le posa.
 *
 * ⚠️⚠️ **NASCE CON LA `2.91`, CHE È IL SUO MOCKUP** (campo libero del giro della `2.90`: coppie
 * più strette, un separatore fra l'una e l'altra, e con quattro file o più i primi due, lo
 * stacco e l'ultimo). Le tre prove presidiano le tre cose che la forma nuova può perdere in
 * silenzio: il **conto**, che è logica, le **distanze**, che sono struttura, e il **taglio** della
 * freccia, che è disegno.
 * ⚠️ **Monta l'anteprima da sola e non la finestra**: i nomi di dopo li scrive il template, e
 * in una scena minima si sceglie un template che rende i nomi riconoscibili. La finestra vera, coi
 * file veri, la monta [RinominaTest].
 * ⚠️ **Vive in una classe sua perché vuole la grafica vera**, che vale per tutta la classe: la
 * terza prova guarda i pixel. ⚠️ **E la densità è tripla** perché la freccia sia fatta di pixel
 * abbastanza da contarli: a quella di serie un dp è un pixel, e i 2dp fra la punta e la pastiglia
 * sarebbero due righe.
 *
 * ⚠️ **Che cosa NON vede**: come l'anteprima **si legge**, cioè se il separatore si veda e se le
 * coppie si distinguano sul telefono. Quello si guarda col telefono in mano.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xxhdpi")
class RinominaAnteprimaTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **Fino a tre file si vedono tutti; da quattro, i primi due, lo stacco e l'ultimo.**
     *
     * ⚠️⚠️ **È LA SUA SPECIFICA IN QUATTRO CASI**, uno per ogni riga che ha scritto (uno, due,
     * tre, e quattro o più), più un caso con nove file per dire che da quattro in su la forma non
     * cambia.
     * La forma si scrive come una stringa (`P` l'abbinamento, `R` il separatore, `S` lo stacco),
     * così un fallimento dice in una riga che cosa è arrivato al posto di che cosa.
     * ⚠️ **L'ultimo si guarda anche nei nomi**: è quello che porta il numero più alto, cioè la
     * ragione per cui c'è, e uno stacco giusto con l'abbinamento sbagliato dopo passerebbe il
     * solo controllo della forma.
     */
    @Test
    fun `fino a tre file si vedono tutti, poi i primi due, lo stacco e l'ultimo`() {
        assertEquals("con un file", "P", forma(1))
        assertEquals("con due file", "PRP", forma(2))
        assertEquals("con tre file", "PRPRP", forma(3))
        assertEquals("con quattro file", "PRPSP", forma(4))
        assertEquals("con nove file", "PRPSP", forma(9))
        val ultimo = previewOf(nomi(9), MODELLO, 1, null).last() as PreviewLine.Pairing
        assertEquals("l'ultimo abbinamento non parte dall'ultimo file", "f9.jpg", ultimo.before)
        assertEquals("l'ultimo abbinamento non porta il numero più alto", "dopo 9.jpg", ultimo.after)
    }

    /**
     * **Fra due coppie c'è più aria che dentro una coppia, e lo stacco prende il posto del
     * separatore.**
     *
     * ⚠️⚠️ **È LA PRIMA METÀ DELLA SUA RICHIESTA, SCRITTA COME UNA PROPRIETÀ** (*il separatore
     * che ho aggiunto per separare meglio le coppie*): fino alla `2.90` il risultato di un file
     * era più vicino al nome del file dopo che al proprio, e questa prova lo vede. Non si ricopiano
     * le misure, che sono del mockup e cambieranno al primo ritocco: si misura il verso.
     * ⚠️ **Le distanze si prendono fra i testi**, e il riempimento delle pastiglie è lo stesso da
     * tutte e due le parti, quindi il confronto regge anche se i riquadri dei testi lo
     * comprendessero.
     * ⚠️ **Il terzo nome non deve esserci**: con quattro file lo stacco nasconde proprio lui, ed
     * è il caso che la regola di prima non aveva (si vedevano tutti e quattro).
     */
    /*
     * ⚠️ **Il nome della prova non porta lettere accentate**, per la ragione scritta in
     * `SalvataggioTest`: Gradle ne ricava il nome di un file del rapporto, e con una codifica di
     * sistema stretta un accento fa fallire il build proprio quando una prova cade.
     */
    @Test
    fun `lo spazio fra le coppie supera quello dentro una coppia`() {
        val nomi = listOf("uno.jpg", "due.jpg", "tre.jpg", "quattro.jpg")
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(Modifier.width(300.dp)) {
                    PreviewLines(previewOf(nomi, MODELLO, 1, null))
                }
            }
        }
        banco.onNode(hasText("tre", substring = true)).assertDoesNotExist()
        val dentroPrima = alto("dopo 1") - basso("uno")
        val fraPrimaSeconda = alto("due") - basso("dopo 1")
        val dentroSeconda = alto("dopo 2") - basso("due")
        val fraSecondaUltima = alto("quattro") - basso("dopo 2")
        assertTrue(
            "fra due coppie ($fraPrimaSeconda px) non c'è più aria che dentro una ($dentroPrima px)",
            fraPrimaSeconda > dentroPrima
        )
        assertEquals("lo stacco non prende il posto del separatore", fraPrimaSeconda, fraSecondaUltima, 1f)
        assertEquals("le due coppie non hanno la stessa misura", dentroPrima, dentroSeconda, 1f)
    }

    /**
     * **La freccia nasce dalla pastiglia di sopra, non ci entra, e non tocca quella di sotto.**
     *
     * ⚠️⚠️ **SONO LE TRE COSE CHE DICONO 'LA FORMA E IL POSIZIONAMENTO DELLE FRECCE'**, e ognuna
     * cade da sola: senza il ritaglio il gambo entra nel riquadro di sopra, che è disegnato prima
     * e non copre niente; posata dall'alto invece che dalla punta, il gambo comincia più giù e fra
     * la pastiglia e la freccia resta dell'aria; senza l'aria della punta, la punta tocca il filo
     * di sotto.
     * ⚠️ **Si guarda la colonna di mezzo contro una colonna a tre quarti**, dove non ci sono né il
     * nome, che è a sinistra, né la freccia: nella stessa riga, le due devono avere lo stesso
     * colore dovunque la freccia non debba esserci. È un confronto fra due pixel della stessa
     * scena, quindi non dipende dai colori del tema.
     * ⚠️ **A dire dove comincia e dove finisce lo spazio fra le due pastiglie è la colonna a tre
     * quarti**: là il fondo della scena compare due volte, sopra la prima pastiglia (il
     * riempimento dell'anteprima) e fra le due, e il secondo tratto è lo spazio della freccia.
     */
    @Test
    fun `la freccia nasce dalla pastiglia di sopra e non tocca quella di sotto`() {
        var fondo = Color.Unspecified
        banco.setContent {
            AivTheme(darkTheme = false) {
                fondo = MaterialTheme.colorScheme.surfaceContainerHigh
                Box(Modifier.width(240.dp).background(fondo).testTag(SCENA)) {
                    PreviewLines(listOf(PreviewLine.Pairing("a.jpg", "b.jpg")))
                }
            }
        }
        val pixel = banco.onNodeWithTag(SCENA).captureToImage().toPixelMap()
        val mezzo = pixel.width / 2
        val lato = pixel.width * 3 / 4
        val tratti = (0 until pixel.height).filter { simili(pixel[lato, it], fondo) }.tratti()
        assertTrue("la scena non ha lo spazio fra le due pastiglie: $tratti", tratti.size >= 2)
        val cima = tratti[0].last + 1
        val spazio = tratti[1]

        for (riga in cima until spazio.first) {
            assertTrue(
                "alla riga $riga la freccia entra nella pastiglia di sopra",
                simili(pixel[mezzo, riga], pixel[lato, riga])
            )
        }
        assertTrue(
            "la freccia non nasce dalla pastiglia di sopra: alla riga ${spazio.first} non c'è",
            !simili(pixel[mezzo, spazio.first], fondo)
        )
        assertTrue(
            "la punta tocca la pastiglia di sotto: alla riga ${spazio.last} c'è la freccia",
            simili(pixel[mezzo, spazio.last], fondo)
        )
    }

    /** La forma dell'anteprima con [quanti] file, una lettera per riga. */
    private fun forma(quanti: Int): String =
        previewOf(nomi(quanti), MODELLO, 1, null).joinToString("") {
            when (it) {
                is PreviewLine.Pairing -> "P"
                PreviewLine.Rule -> "R"
                PreviewLine.Skip -> "S"
            }
        }

    private fun nomi(quanti: Int) = (1..quanti).map { "f$it.jpg" }

    private fun alto(testo: String) =
        banco.onNode(hasText(testo, substring = true)).fetchSemanticsNode().boundsInRoot.top

    private fun basso(testo: String) =
        banco.onNode(hasText(testo, substring = true)).fetchSemanticsNode().boundsInRoot.bottom

    /**
     * Se due colori sono lo stesso, a meno dell'arrotondamento a otto bit.
     *
     * ⚠️ **Due livelli su 255 per canale**: il fondo e le pastiglie sono pieni, quindi dove la
     * freccia non c'è i due pixel coincidono, e dove c'è la differenza è di decine di livelli.
     */
    private fun simili(a: Color, b: Color): Boolean =
        abs(a.red - b.red) < 2f / 255f && abs(a.green - b.green) < 2f / 255f &&
            abs(a.blue - b.blue) < 2f / 255f

    /** Le righe consecutive di questo elenco ordinato, raccolte in tratti. */
    private fun List<Int>.tratti(): List<IntRange> {
        val fuori = ArrayList<IntRange>()
        var da = -1
        var a = -1
        for (riga in this) {
            if (da >= 0 && riga == a + 1) {
                a = riga
            } else {
                if (da >= 0) fuori += da..a
                da = riga
                a = riga
            }
        }
        if (da >= 0) fuori += da..a
        return fuori
    }

    private companion object {
        /** Il template della scena: i nomi di dopo si riconoscono dal numero. */
        const val MODELLO = "dopo #"
        const val SCENA = "scena"
    }
}

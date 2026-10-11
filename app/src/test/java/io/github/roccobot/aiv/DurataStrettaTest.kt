package io.github.roccobot.aiv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * La durata di un filmato su una miniatura stretta.
 *
 * ⚠️⚠️ **NASCE DA UN DIFETTO ARRIVATO A LUI** (nota A del giro della `5.70`, con la schermata
 * del telefono in orizzontale: *le miniature video non riescono a contenere l'info sulla
 * durata*). Su una miniatura di circa 70 dp la durata `12:00` andava a capo in due righe.
 * ⚠️ **La misura è più stretta di quella del telefono, apposta**: 60 dp lasciano al testo meno
 * spazio di quello che gli serve col suo corpo pieno, che è la condizione del difetto, e la
 * prova non dipende dal carattere che il banco usa al posto di quello del telefono.
 * ⚠️ **Controprovata rimettendo il testo di prima** (un `Text` libero di andare a capo): la prova
 * cade sulla seconda asserzione, perché sul banco il testo resta su una riga ma esce dal suo
 * spazio. Il telefono, col suo motore di impaginazione, lo manda a capo: sono le due facce dello
 * stesso spazio che manca, e la prova le ferma tutte e due.
 */
@RunWith(AndroidJUnit4::class)
class DurataStrettaTest {

    @get:Rule
    val banco = createComposeRule()

    @Test
    fun `la durata resta su una riga anche su una miniatura stretta`() {
        banco.setContent {
            Box(Modifier.width(60.dp)) { ClipLabel(length = 12 * 60_000L) }
        }
        val righe = mutableListOf<TextLayoutResult>()
        banco.onNodeWithText("12:00").fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action?.invoke(righe)
        assertEquals("la durata è andata a capo", 1, righe.single().lineCount)
        assertEquals("la durata è stata tagliata", false, righe.single().hasVisualOverflow)
    }
}

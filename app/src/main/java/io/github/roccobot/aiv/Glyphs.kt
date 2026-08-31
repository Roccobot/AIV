package io.github.roccobot.aiv

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Le icone che Material non ha, disegnate qui.
 *
 * ⚠️⚠️ **ESISTE PERCHÉ UN'ICONA MANCAVA DAVVERO, non per gusto**: il cursore di testo dei
 * programmi da tavolo non c'è in Material, in nessuno dei due set (cercato, non supposto), e
 * la cosa più vicina è la matita di `Icons.Default.Edit`, che dice 'modifica' e non
 * 'rinomina'. Chi volesse aggiungere qui un glifo che Material ha già sta duplicando un
 * disegno mantenuto da altri, e prima o poi i due divergeranno.
 *
 * ⚠️ **Il colore dichiarato è nero e non è un difetto**: `Icon` disegna il vettore con un
 * `ColorFilter.tint`, quindi la tinta che si vede è quella passata a `Icon` e questa non si
 * vede mai. È la stessa convenzione delle icone di Material, che dichiarano tutte nero.
 */
object Glyphs {

    /**
     * Il cursore di testo, cioè la I con le due lineette.
     *
     * ⚠️ **Di tratto e non di pieno**, a differenza di tutte le altre sei del riquadro: è
     * l'unica fatta di aria, ed è il baratto dichiarato quando l'utente l'ha scelta. Lo
     * spessore è 2, come le altre a 24dp, così non sembra più leggera.
     * ⚠️ **Le estremità sono tonde** (`StrokeCap.Round`): a spigolo vivo, a 24dp, i tre
     * tratti sembrano tagliati da una lama e l'insieme perde il richiamo al cursore.
     */
    val TextCursor: ImageVector by lazy {
        ImageVector.Builder(
            name = "TextCursor",
            defaultWidth = SIZE,
            defaultHeight = SIZE,
            viewportWidth = GRID,
            viewportHeight = GRID
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round
            ) {
                // La lineetta in alto.
                moveTo(9f, 4f)
                horizontalLineTo(15f)
                // Quella in basso.
                moveTo(9f, 20f)
                horizontalLineTo(15f)
                // L'asta che le unisce.
                moveTo(12f, 4f)
                verticalLineTo(20f)
            }
        }.build()
    }

    /** La griglia di Material: ogni icona del sistema è disegnata dentro un 24x24. */
    private const val GRID = 24f

    /** Lo spessore delle altre icone a 24dp, in unità di griglia. */
    private const val STROKE = 2f

    private val SIZE = 24.dp
}

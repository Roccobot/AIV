package io.github.roccobot.aiv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * L'ingrandimento che sopravvive a una rotazione dello schermo.
 *
 * ⚠️ **Si conserva un rapporto, non una scala**: ruotando, la vista cambia forma e la scala che
 * mostrava l'immagine intera non la mostra più intera. Si tiene quante volte l'immagine è
 * ingrandita rispetto al suo riposo ([zoom]), che dopo la rotazione si moltiplica per il riposo
 * nuovo. E il punto inquadrato si tiene in frazioni del lato dell'immagine ([centreX], [centreY]),
 * perché in pixel della vista ruotando indicherebbe un altro punto.
 *
 * ⚠️ **`rememberSaveable` perché ruotando l'activity si ricrea**: il manifesto non dichiara
 * `configChanges`, e un `remember` è già perso, con qualunque chiave. La chiave è il nome del file
 * e non il bitmap, che dopo la rotazione è un oggetto nuovo: così l'ingrandimento resta per quella
 * immagine e si azzera passando alla successiva.
 *
 * @property zoom quante volte l'immagine è ingrandita rispetto alla sua scala di riposo: `1` a
 *   riposo, col punto inquadrato al centro.
 */
@Stable
class ZoomKeep internal constructor(zoom: Float, centreX: Float, centreY: Float) {

    var zoom by mutableFloatStateOf(zoom)
        private set

    /** La frazione della larghezza dell'immagine che è al centro della vista, da -0,5 a 0,5. */
    var centreX by mutableFloatStateOf(centreX)
        private set

    /** Come [centreX], per l'altezza. */
    var centreY by mutableFloatStateOf(centreY)
        private set

    /** La scala da applicare con questo riposo. */
    fun scaleFor(rest: Float): Float = zoom * rest

    /**
     * Lo scostamento che rimette al centro il punto inquadrato. Il segno è meno perché lo scostamento
     * sposta l'immagine e non lo sguardo.
     */
    fun offsetFor(scale: Float, imageWidth: Float, imageHeight: Float): Offset =
        Offset(-centreX * imageWidth * scale, -centreY * imageHeight * scale)

    /**
     * Prende nota di dove si è arrivati, per la prossima rotazione. Un riposo o una scala a zero non si
     * scrivono: capitano nel fotogramma in cui la vista è misurata e l'immagine no, e la divisione
     * darebbe un `NaN` che poi si conserva.
     */
    fun record(scale: Float, offset: Offset, rest: Float, imageWidth: Float, imageHeight: Float) {
        if (rest <= 0f || scale <= 0f || imageWidth <= 0f || imageHeight <= 0f) return
        zoom = scale / rest
        centreX = -offset.x / (scale * imageWidth)
        centreY = -offset.y / (scale * imageHeight)
    }

    companion object {
        /** I tre numeri da ripristinare dopo una rotazione. */
        val Saver: Saver<ZoomKeep, Any> = listSaver(
            save = { listOf(it.zoom, it.centreX, it.centreY) },
            restore = { ZoomKeep(it[0], it[1], it[2]) }
        )
    }
}

/**
 * L'ingrandimento tenuto per l'immagine [key], che sopravvive a una rotazione.
 *
 * @param key ciò che identifica l'immagine e non cambia ruotando: il nome del file.
 */
@Composable
fun rememberZoomKeep(key: Any?): ZoomKeep =
    rememberSaveable(key, saver = ZoomKeep.Saver) { ZoomKeep(1f, 0f, 0f) }

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
 * ⚠️⚠️ **NASCE NELLA `1.82` DOPO DUE GIRI, E LA SECONDA VOLTA IL RISCONTRO ERA DI DUE PAROLE**
 * (voce `zoom-rotazione` del giro della `1.81`: *è come prima*). La `1.81` aveva tolto la metà
 * facile del difetto, cioè lo zoom che si azzerava a **ogni impostazione toccata**, e sulla
 * rotazione aveva scritto per bene che non la copriva. Metà di un difetto corretta è un difetto.
 *
 * ⚠️⚠️ **QUELLO CHE SI CONSERVA È UN RAPPORTO, NON UNA SCALA, ed è tutta la questione.** Ruotando
 * il telefono la vista cambia forma, quindi la scala che mostrava la fotografia intera prima non
 * la mostra intera dopo: conservare il numero vorrebbe dire riaprire l'immagine tagliata o
 * rimpicciolita senza che nessuno abbia toccato niente. Quello che ha senso conservare è **quante
 * volte** l'immagine è ingrandita rispetto al proprio riposo ([zoom]), che dopo la rotazione si
 * moltiplica per il riposo nuovo.
 * ⚠️⚠️ **E CON LUI IL PUNTO INQUADRATO, in coordinate dell'IMMAGINE e non in pixel di schermo**:
 * lo scostamento vive in pixel della vista, quindi ruotando indicherebbe un altro punto della
 * fotografia. Qui si tiene la frazione ([centreX], [centreY]) del lato dell'immagine che sta al
 * centro dello schermo, che è la stessa cosa prima e dopo.
 *
 * ⚠️⚠️ **`rememberSaveable` E NON `remember`, PERCHÉ RUOTANDO L'ACTIVITY SI RICREA**: il manifesto
 * non dichiara `configChanges`, quindi alla rotazione la composizione riparte da zero e un
 * `remember`, con qualunque chiave, è già stato buttato via. È la ragione per cui la correzione è
 * un pezzo a sé e non una chiave scritta meglio.
 * ⚠️ **La chiave è il NOME DEL FILE e non l'immagine**: dopo la rotazione il bitmap è un oggetto
 * nuovo, quindi una chiave sull'identità scarterebbe il valore appena ripristinato. Con il nome,
 * l'ingrandimento si conserva per **quella** immagine e si azzera passando alla successiva, che è
 * il comportamento di sempre.
 *
 * @property zoom quante volte l'immagine è ingrandita rispetto alla propria scala di riposo. Vale
 *   `1` a riposo, e con lui il punto inquadrato è il centro.
 */
@Stable
class ZoomKeep internal constructor(zoom: Float, centreX: Float, centreY: Float) {

    var zoom by mutableFloatStateOf(zoom)
        private set

    /** La frazione della larghezza dell'immagine che sta al centro della vista, da -0,5 a 0,5. */
    var centreX by mutableFloatStateOf(centreX)
        private set

    /** Come [centreX], per l'altezza. */
    var centreY by mutableFloatStateOf(centreY)
        private set

    /** La scala da applicare con questo riposo. */
    fun scaleFor(rest: Float): Float = zoom * rest

    /**
     * Lo scostamento che rimette al centro il punto inquadrato.
     *
     * ⚠️ **Il segno è meno** perché lo scostamento sposta l'immagine e non lo sguardo: per portare
     * al centro un punto che si trova a destra, l'immagine va spinta a sinistra.
     */
    fun offsetFor(scale: Float, imageWidth: Float, imageHeight: Float): Offset =
        Offset(-centreX * imageWidth * scale, -centreY * imageHeight * scale)

    /**
     * Prende nota di dove si è arrivati, per la prossima rotazione.
     *
     * ⚠️ **Un riposo o una scala a zero non si scrivono**: capitano nel fotogramma in cui la vista
     * è stata misurata ma l'immagine no, e dividerci darebbe un `NaN` che poi si conserva.
     */
    fun record(scale: Float, offset: Offset, rest: Float, imageWidth: Float, imageHeight: Float) {
        if (rest <= 0f || scale <= 0f || imageWidth <= 0f || imageHeight <= 0f) return
        zoom = scale / rest
        centreX = -offset.x / (scale * imageWidth)
        centreY = -offset.y / (scale * imageHeight)
    }

    companion object {
        /** Tre numeri, che è tutto quello che serve rimettere in piedi dopo una rotazione. */
        val Saver: Saver<ZoomKeep, Any> = listSaver(
            save = { listOf(it.zoom, it.centreX, it.centreY) },
            restore = { ZoomKeep(it[0], it[1], it[2]) }
        )
    }
}

/**
 * L'ingrandimento tenuto per l'immagine [key], che sopravvive a una rotazione.
 *
 * @param key qualcosa che identifichi l'immagine e che **non** cambi ruotando: il nome del file.
 */
@Composable
fun rememberZoomKeep(key: Any?): ZoomKeep =
    rememberSaveable(key, saver = ZoomKeep.Saver) { ZoomKeep(1f, 0f, 0f) }

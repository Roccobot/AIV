package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Size

/**
 * Il fotogramma che fa da miniatura a un video quando quella del sistema è nera o manca.
 *
 * La miniatura di un video la fa il `MediaProvider` con `ThumbnailUtils.createVideoThumbnail`, e il
 * fotogramma scelto dipende dalla versione di Android (letto nel sorgente AOSP):
 * - fino ad Android 16 nella versione di lancio, la copertina incorporata se c'è, altrimenti il
 *   fotogramma chiave più vicino alla metà del video;
 * - da Android 16 QPR1, per un MP4, il più pesante fra i primi venti fotogrammi chiave
 *   (`SampleTable.findThumbnailSample`): se il video comincia con un tratto nero che li copre
 *   tutti, la miniatura è nera.
 * Quindi si rimedia al risultato e non alla regola, che cambia col telefono.
 *
 * Si prova prima la metà del video, la più lontana da un'apertura e da una chiusura in nero, poi un
 * quarto, tre quarti e un decimo. Si chiede un fotogramma chiave (`OPTION_CLOSEST_SYNC`), come fa
 * il sistema: quello esatto vuole la decodifica di tutti i fotogrammi dalla chiave precedente. Se
 * sono tutti neri vince il più chiaro.
 *
 * Si paga una volta per video: il risultato finisce in [AvifCache] (il perché è là), e i video con
 * una miniatura di sistema buona non passano di qui.
 */
internal object ClipFrames {

    /**
     * Se [bitmap] non mostra niente: al più l'1% dei punti della griglia supera [DARK].
     *
     * ⚠️ **Si contano i punti chiari e non la media**: una scritta bianca su fondo nero ha una
     * media bassissima e un contenuto vero. Una bitmap che non si legge risponde no, cioè si tiene.
     */
    fun isBlack(bitmap: Bitmap): Boolean = brightOf(bitmap)?.let { black(it) } ?: false

    /**
     * Quanti punti della griglia superano [DARK], o `null` se la bitmap non si legge.
     *
     * Una griglia di [GRID] per [GRID] punti, al centro delle celle così il bordo pesa come il
     * resto: su una miniatura da 512 sono mille letture e non centocinquantamila. In memoria
     * grafica i pixel non si leggono, e la risposta è `null` (le miniature che arrivano qui sono
     * software, vedi `inGraphics`).
     * ⚠️ **Conta il canale più acceso e non la luminanza**: il blu pesa l'11% della luminanza, e un
     * blu pieno a 200 varrebbe 'nero'. Un nero vero ha spenti tutti e tre i canali.
     */
    internal fun brightOf(bitmap: Bitmap): Int? {
        if (bitmap.config == Bitmap.Config.HARDWARE || bitmap.width <= 0 || bitmap.height <= 0) return null
        return runCatching {
            var bright = 0
            for (row in 0 until GRID) {
                val y = (2 * row + 1) * bitmap.height / (2 * GRID)
                for (col in 0 until GRID) {
                    val x = (2 * col + 1) * bitmap.width / (2 * GRID)
                    val c = bitmap.getPixel(x, y)
                    if (maxOf(Color.red(c), Color.green(c), Color.blue(c)) > DARK) bright++
                }
            }
            bright
        }.getOrNull()
    }

    /** Se un conteggio di punti chiari dice 'nero'. Vedi [isBlack]. */
    private fun black(bright: Int): Boolean = bright * 100 <= GRID * GRID

    /**
     * I momenti da provare, in microsecondi e nell'ordine: metà, un quarto, tre quarti, un decimo.
     *
     * Senza la durata, tre momenti fissi dall'inizio (1, 5 e 15 secondi): oltre l'ultimo fotogramma
     * la richiesta risponde col più vicino o con niente, e allora si passa al momento dopo. Le
     * frazioni sono due interi: un'ora in microsecondi per tre resta ben sotto il massimo di un
     * `Long`.
     */
    fun times(durationMs: Long?): LongArray = if (durationMs != null && durationMs > 0) {
        LongArray(FRACTIONS.size) { i -> durationMs * 1000L * FRACTIONS[i].first / FRACTIONS[i].second }
    } else {
        LongArray(FALLBACK_MS.size) { i -> FALLBACK_MS[i] * 1000L }
    }

    /**
     * Il fotogramma da usare come miniatura di [uri], dentro un riquadro [box], o `null`.
     *
     * Il primo che non è nero vince, e gli altri non si chiedono: ognuno costa una ricerca e una
     * decodifica. [alive] si guarda fra un fotogramma e l'altro, perché le chiamate di
     * `MediaMetadataRetriever` non si interrompono. `release()` a mano e non `use`, per la ragione
     * scritta in [Videos].
     */
    fun pick(context: Context, uri: Uri, box: Size, alive: () -> Boolean = { true }): Bitmap? {
        val reader = MediaMetadataRetriever()
        return try {
            if (uri.scheme?.lowercase() == "file") reader.setDataSource(uri.path) else reader.setDataSource(context, uri)
            val duration = reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            var best: Bitmap? = null
            var bestBright = -1
            for (at in times(duration)) {
                if (!alive()) return null
                val frame = runCatching {
                    reader.getScaledFrameAtTime(at, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, box.width, box.height)
                }.getOrNull() ?: continue
                val bright = brightOf(frame) ?: return frame
                if (!black(bright)) return frame
                if (bright > bestBright) {
                    best = frame
                    bestBright = bright
                }
            }
            best
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { reader.release() }
        }
    }

    /**
     * Il lato della griglia di [brightOf], 1024 punti in tutto: l'1% sono dieci punti. Meno di così
     * una scritta piccola sparirebbe, di più conterebbe il rumore di un nero compresso.
     */
    private const val GRID = 32

    /**
     * Il buio sotto il quale un punto non conta, su 255, sul suo canale più acceso. 32 e non 0: il
     * nero di un video è 0 dopo la conversione dal YUV, ma la compressione ci lascia sopra qualche
     * livello di rumore, e 32 resta sotto ogni scena buia che abbia un disegno.
     */
    private const val DARK = 32

    /** Metà, un quarto, tre quarti e un decimo della durata: vedi [times]. */
    private val FRACTIONS = listOf(1L to 2L, 1L to 4L, 3L to 4L, 1L to 10L)

    /** I momenti da provare quando la durata non si sa, in millisecondi. */
    private val FALLBACK_MS = longArrayOf(1_000L, 5_000L, 15_000L)
}

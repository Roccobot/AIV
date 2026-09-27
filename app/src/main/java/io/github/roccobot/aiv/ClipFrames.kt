package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Size

/**
 * Il fotogramma che fa da miniatura a un video quando quella del sistema è nera o manca, dalla
 * `2.98`.
 *
 * ⚠️⚠️ **È LA SUA RICHIESTA, E NASCE DA UN FATTO DEL SISTEMA CHE NON DIPENDE DA NOI** (campo
 * libero del giro della `2.93` e della `2.94`, poi in chat il 2026-09-27: *la miniatura in effetti
 * c'è ed è nera*, e *sì, voglio che rimedi se manca o se è nera*). La miniatura di un video la fa il
 * `MediaProvider` con `ThumbnailUtils.createVideoThumbnail`, e il fotogramma da cui nasce dipende
 * dalla versione di Android, letto nel sorgente AOSP e non ricordato:
 * - **fino ad Android 16 nella versione di lancio** è la copertina incorporata nel file, se c'è, e
 *   altrimenti il fotogramma chiave più vicino alla **metà** del video;
 * - **dal primo aggiornamento trimestrale di Android 16 (QPR1)** il sistema chiede al decodificatore
 *   il fotogramma che considera rappresentativo, e per un MP4 lo Stagefright prende il più pesante
 *   fra i primi **venti** fotogrammi chiave (`SampleTable.findThumbnailSample`). Un fotogramma nero
 *   pesa poco, quindi quella regola lo scarta; ma se il video comincia con un tratto nero che li
 *   copre tutti e venti, la miniatura viene nera.
 * ⚠️ **Quindi si rimedia al risultato e non alla regola**: la regola è del telefono e cambia con
 * lui, mentre accorgersi che una miniatura non mostra niente vale su qualunque versione.
 *
 * ⚠️⚠️ **SI CHIEDE PRIMA IL FOTOGRAMMA DI METÀ VIDEO**, cioè quello che il sistema ha scelto per
 * anni: è il più lontano da un'apertura e da una chiusura in nero, che sono i due casi in cui un
 * video comincia o finisce su un fotogramma vuoto. Poi un quarto, tre quarti e un decimo.
 * ⚠️ **Un fotogramma CHIAVE e non quello esatto** (`OPTION_CLOSEST_SYNC`), come fa il sistema: un
 * fotogramma esatto vuole la decodifica di tutti quelli fra lui e la chiave precedente, cioè fino a
 * qualche secondo di video per una miniatura.
 * ⚠️ **Se sono neri tutti vince il più chiaro**: un video buio ha comunque un fotogramma meno buio
 * degli altri, e la tessera nera è quella da cui si parte.
 *
 * ⚠️ **Si paga una volta per video**: il risultato finisce in [AvifCache], che dalla `2.98` tiene
 * anche questi fotogrammi (il perché vive là), e i video con una miniatura di sistema buona non
 * passano mai di qui.
 */
internal object ClipFrames {

    /**
     * Se [bitmap] non mostra niente: al più l'1% dei punti della griglia supera il buio di [DARK].
     *
     * ⚠️⚠️ **SI CONTANO I PUNTI CHIARI E NON LA MEDIA**: una scritta bianca su fondo nero (un
     * titolo, un logo) ha una media bassissima e un contenuto vero, e con la media si scarterebbe.
     * Contando, una scritta che copre il 2% della tela basta a tenerla.
     * ⚠️ **Una bitmap che non si legge risponde no**, cioè si tiene: nel dubbio non si cerca altro.
     */
    fun isBlack(bitmap: Bitmap): Boolean = brightOf(bitmap)?.let { black(it) } ?: false

    /**
     * Quanti punti della griglia superano [DARK], o `null` se la bitmap non si legge.
     *
     * ⚠️ **Una griglia di [GRID] per [GRID] punti e non ogni pixel**: su una miniatura da 512 sono
     * mille letture contro centocinquantamila, e un disegno che la griglia non vede è un disegno
     * che in una tessera non si vedrebbe nemmeno. ⚠️ **I punti cadono al centro delle celle**, così
     * il bordo, dove una miniatura può avere una riga di riempimento, pesa come il resto.
     * ⚠️ **In memoria grafica i pixel non si leggono**, e allora la risposta è `null`: le miniature
     * che arrivano qui sono software, e la conversione avviene dopo (vedi `inGraphics`).
     * ⚠️⚠️ **CONTA IL CANALE PIÙ ACCESO E NON LA LUMINANZA**: il blu pesa l'11% della luminanza,
     * quindi un blu pieno a 200 varrebbe 22 su 255, cioè 'nero', mentre è un fotogramma che si vede
     * benissimo. Un nero vero invece ha spenti tutti e tre i canali, e questa è la sola cosa da
     * sapere.
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
     * ⚠️ **Senza la durata, tre momenti fissi dall'inizio** (1, 5 e 15 secondi): un contenitore che
     * non la dichiara ha comunque un fotogramma dopo un secondo, e oltre il suo ultimo la richiesta
     * risponde col più vicino o con niente, che qui vuol dire passare al momento dopo.
     * ⚠️ **Frazioni scritte come due interi**, cioè senza virgola mobile: un'ora in microsecondi,
     * moltiplicata per tre, resta sotto il massimo di un `Long` di nove ordini di grandezza.
     */
    fun times(durationMs: Long?): LongArray = if (durationMs != null && durationMs > 0) {
        LongArray(FRACTIONS.size) { i -> durationMs * 1000L * FRACTIONS[i].first / FRACTIONS[i].second }
    } else {
        LongArray(FALLBACK_MS.size) { i -> FALLBACK_MS[i] * 1000L }
    }

    /**
     * Il fotogramma da usare come miniatura di [uri], dentro un riquadro [box], o `null`.
     *
     * ⚠️ **Il primo che non è nero vince subito**, e gli altri non si chiedono: ognuno costa una
     * ricerca nel contenitore e una decodifica, e un video normale si ferma al primo.
     * ⚠️ **[alive] si guarda fra un fotogramma e l'altro**: le chiamate di `MediaMetadataRetriever`
     * non si possono interrompere, quindi una tessera uscita dallo schermo smette di chiedere al
     * fotogramma dopo, e non a metà di uno.
     * ⚠️ **`release()` a mano e non `use`**, per la ragione scritta in [Videos]: il lettore è
     * chiudibile solo da Android 10, e `minSdk` è 28.
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
     * Il lato della griglia di [brightOf], cioè 1024 punti in tutto.
     *
     * ⚠️ **L'1% di 1024 sono dieci punti**, e sono loro a fare la differenza fra 'nero' e 'buio con
     * qualcosa dentro': meno di così una scritta piccola sparirebbe fra i punti, di più il rumore
     * di un nero compresso comincerebbe a contare.
     */
    private const val GRID = 32

    /**
     * Il buio sotto il quale un punto non conta, su 255, misurato sul suo canale più acceso.
     *
     * ⚠️ **32 e non 0**: il nero di un video è 16 nella scala ridotta del YUV, cioè 0 dopo la
     * conversione, ma la compressione ci lascia sopra un rumore di qualche livello. 32 lo copre con
     * margine e resta sotto ogni scena buia che abbia un disegno.
     */
    private const val DARK = 32

    /** Metà, un quarto, tre quarti e un decimo della durata: vedi [times]. */
    private val FRACTIONS = listOf(1L to 2L, 1L to 4L, 3L to 4L, 1L to 10L)

    /** I momenti da provare quando la durata non si sa, in millisecondi. */
    private val FALLBACK_MS = longArrayOf(1_000L, 5_000L, 15_000L)
}

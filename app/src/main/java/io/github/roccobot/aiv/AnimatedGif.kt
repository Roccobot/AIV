package io.github.roccobot.aiv

import android.graphics.Bitmap
import com.bumptech.glide.gifdecoder.GifDecoder
import com.bumptech.glide.gifdecoder.GifHeaderParser
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import java.nio.ByteBuffer

/**
 * Le GIF animate, lette dal decodificatore di Glide.
 *
 * ⚠️⚠️ **PERCHÉ UNA DIPENDENZA E NON CODICE NOSTRO, deciso il 2026-09-01 dopo aver messo a
 * confronto cinque strade**: `com.github.bumptech.glide:gifdecoder` sono **18.072 byte** di
 * archivio, **zero librerie native**, nessuna dipendenza transitiva nuova (chiede
 * `annotation 1.7.1` e `kotlin-stdlib 2.2.10`, che il progetto ha già più recenti), e
 * licenza **BSD a due clausole**. Scriverlo in casa avrebbe voluto dire 650-900 righe fra
 * decodificatore LZW e compositore coi quattro metodi di disposal, cioè esattamente questo
 * codice riscritto e da collaudare daccapo.
 * ⚠️ **La via di fuga è dichiarata e vale la pena saperla**: sono 7 classi BSD. Il giorno
 * che la dipendenza desse noia, i sorgenti si copiano nel repo dietro l'interfaccia
 * [Animated] e **nessun chiamante cambia**. È una scelta reversibile in tutte e due le
 * direzioni.
 *
 * ⚠️⚠️ **LE STRADE SCARTATE, coi motivi misurati, perché nessuno le riapra**:
 * - `android.graphics.Movie`: non conta i fotogrammi, salta al **millisecondo** e non
 *   all'indice, e **non vede affatto** i fotogrammi con ritardo 0 (letto in `GIFMovie.cpp`
 *   di AOSP: la somma cumulativa non avanza, quindi `setTime` non li seleziona mai).
 *   Deprecata da API 28.
 * - `ImageDecoder` e `AnimatedImageDrawable`: verificato con `javap` che tutta la loro
 *   superficie è `start`, `stop`, `isRunning`, `setRepeatCount` e i callback di inizio e
 *   fine. Sanno riprodurre, non sanno contare né cercare.
 * - koral `android-gif-drawable`: fa tutto, ma porta **4 file `.so`**, cioè esattamente
 *   quello che la `1.12` ha buttato fuori per tornare da 37 a 4,7 MB.
 * - Fresco: 413 KB di archivio più un megabyte di `.so`, e un secondo caricatore di
 *   immagini accanto a Coil.
 */
class AnimatedGif private constructor(
    private val decoder: GifDecoder
) : Animated {

    /**
     * Se [close] è già passato di qui.
     *
     * ⚠️⚠️ **ESISTE PERCHÉ UN DECODIFICATORE CHIUSO NON DICE DI ESSERLO, LO DIMOSTRA
     * SOLLEVANDO UN ERRORE**: `GifDecoder.clear()` azzera l'intestazione, e da lì
     * `frameCount`, `getDelay` e `advance` cadono su un riferimento nullo. Con un lettore in
     * mano a un ciclo di riproduzione, quell'errore arriva dentro una coroutine e chiude
     * l'app. È successo nella `1.13`, per una chiusura sbagliata in [rememberAnimation] che
     * adesso non c'è più: questa bandierina è la rete sotto quella correzione, perché
     * chiudere due volte o usare un lettore chiuso deve costare un fotogramma mancante, non
     * l'applicazione.
     * ⚠️ **Non rende [AnimatedWebp] e questo lettore diversi: li rende uguali.** L'altro
     * regge già la stessa sequenza da sé, perché la sua chiusura butta soltanto la tela.
     */
    private var closed = false

    override val width: Int get() = if (closed) 0 else decoder.width
    override val height: Int get() = if (closed) 0 else decoder.height
    override val frameCount: Int get() = if (closed) 0 else runCatching { decoder.frameCount }.getOrDefault(0)
    override val index: Int get() = if (closed) 0 else decoder.currentFrameIndex

    /**
     * ⚠️ `getNetscapeLoopCount` è il numero **scritto nel file**, e vale 0 per 'per sempre':
     * è già la convenzione di [Animated], quindi non si traduce. Quando l'estensione
     * NETSCAPE manca del tutto il decodificatore risponde con la sua costante di 'non
     * dichiarato', e per un visualizzatore quel caso si legge come 'una volta sola'.
     */
    override val loopCount: Int
        get() = decoder.netscapeLoopCount.let { if (it < 0) 1 else it }

    override fun delayOf(index: Int): Int =
        if (closed) 0 else runCatching { decoder.getDelay(index) }.getOrDefault(0).coerceAtLeast(0)

    /**
     * ⚠️⚠️ **`getNextFrame` NON è 'il prossimo': è IL CORRENTE, ed è il nome più
     * ingannevole di questa API.** Il decodificatore tiene un indice, `advance()` lo sposta e
     * `getNextFrame()` compone e restituisce il fotogramma **a quell'indice**. Chiamandolo
     * due volte di fila senza `advance()` si riottiene lo stesso fotogramma, non il seguente.
     * Chi legge questo file senza la nota scriverebbe un ciclo che salta un fotogramma su
     * due.
     * ⚠️ **Il Bitmap appartiene al decodificatore e verrà RIUSATO**: chi lo vuole tenere
     * (l'esportazione, una cache) ne fa una copia. Qui si restituisce l'originale, perché
     * disegnarlo e basta è il caso normale e copiare a ogni fotogramma sarebbe uno spreco.
     */
    override fun current(): Bitmap? =
        if (closed) null else runCatching { decoder.nextFrame }.getOrNull()

    override fun advance() {
        if (closed) return
        runCatching { decoder.advance() }
    }

    /**
     * ⚠️⚠️ **`resetFrameIndex` DA SOLO NON BASTA, ed è un difetto di contratto trovato
     * scrivendo il passo indietro**: quel metodo riporta l'indice a **-1**, cioè a 'prima
     * del primo', mentre [AnimatedWebp] dopo `rewind` è già sul fotogramma **0**. Due
     * lettori che rispondono in modo diverso alla stessa chiamata avrebbero fatto sbagliare
     * di uno ogni salto all'indietro, e solo sulle GIF. L'`advance` qui sotto è quello che
     * pareggia i due: dopo questa funzione l'indice è **0** in tutti e due i lettori.
     * ⚠️ **Privata dalla 1.21**: l'unico che la chiama è [seek], perché riavvolgere senza poi
     * ricomporre la pila è precisamente l'errore che ha prodotto la scia.
     */
    private fun rewind() {
        if (closed) return
        runCatching {
            decoder.resetFrameIndex()
            decoder.advance()
        }
    }

    /**
     * ⚠️⚠️ **OGNI PASSO CHIEDE ANCHE IL FOTOGRAMMA, e quel `nextFrame` apparentemente inutile
     * è tutta la correzione**: è la lettura a comporre la toppa sulla tela, non `advance()`.
     * Saltando con soli `advance()` la tela non viene toccata, e il fotogramma d'arrivo si
     * posa sopra quello che c'era: da lì la scia. Il valore restituito si butta apposta,
     * perché quello che serve è **l'effetto** sulla tela interna del decodificatore.
     * ⚠️ **All'indietro si riavvolge**, perché la pila si può solo rifare da capo: dopo
     * `rewind()` l'indice è 0 e la tela viene ripulita dal decodificatore stesso, che a
     * fotogramma zero non ha nessun precedente da conservare.
     * ⚠️ **Il ciclo conta i PASSI e non confronta gli indici**: `advance()` gira in tondo
     * dopo l'ultimo fotogramma, quindi un `while (index != target)` su un file con un solo
     * fotogramma, o su un indice che il decodificatore rifiuta di muovere, non finirebbe mai.
     */
    override fun seek(target: Int) {
        if (closed) return
        runCatching {
            val quanti = decoder.frameCount
            if (quanti <= 0) return
            val dove = target.coerceIn(0, quanti - 1)
            if (dove < decoder.currentFrameIndex) {
                rewind()
                // ⚠️⚠️ **ANCHE IL FOTOGRAMMA ZERO VA CHIESTO, e dimenticarlo lascia il
                // difetto intero**: è la lettura a fotogramma 0 a ripulire la tela, perché è
                // il solo indice in cui il decodificatore non ha un precedente da conservare.
                // Riavvolgendo e ripartendo dal fotogramma 1, la tela vecchia sopravvive e la
                // scia resta esattamente com'era. Trovato rileggendo questa funzione, non
                // provandola.
                decoder.nextFrame
            }
            var passi = dove - decoder.currentFrameIndex
            if (passi < 0) passi = 0
            repeat(passi) {
                decoder.advance()
                decoder.nextFrame
            }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        runCatching { decoder.clear() }
    }

    companion object {

        /**
         * Apre una GIF già letta in memoria, e `null` se non è leggibile.
         *
         * ⚠️ **Si controlla lo stato dell'intestazione prima di fidarsi**: su un file
         * troncato il parser torna comunque un `GifHeader`, ma con uno stato di errore e
         * zero fotogrammi. Senza questo controllo si andrebbe avanti fino a un'animazione
         * vuota che nessuno sa spiegare.
         */
        fun open(bytes: ByteArray): Animated? = runCatching {
            val buffer = ByteBuffer.wrap(bytes)
            val parser = GifHeaderParser().setData(buffer)
            val header = parser.parseHeader()
            parser.clear()
            if (header.status != GifDecoder.STATUS_OK || header.numFrames <= 0) return null
            val decoder = StandardGifDecoder(Pool(), header, buffer)
            // ⚠️ ARGB_8888 e non RGB_565: le GIF hanno la trasparenza a chiave, e in 565 il
            // colore trasparente diventa un colore vero, di solito nero. Il risparmio di
            // memoria non vale un'immagine sbagliata.
            decoder.setDefaultBitmapConfig(Bitmap.Config.ARGB_8888)
            decoder.advance()
            AnimatedGif(decoder)
        }.getOrNull()
    }

    /**
     * Da dove il decodificatore prende i suoi Bitmap e i suoi vettori.
     *
     * ⚠️⚠️ **SERVE PERCHÉ L'INTERFACCIA LO PRETENDE, e la sua ragione d'essere è il
     * RIUSO**: comporre un fotogramma vuol dire allocare una tela grande quanto l'immagine, e
     * farlo venti volte al secondo darebbe al garbage collector un lavoro che si vede come
     * scatti. Con un serbatoio di uno, la tela si alloca una volta e si riscrive.
     * ⚠️ **Un solo Bitmap in serbatoio e non un magazzino**: il decodificatore ne chiede uno
     * per volta e restituisce il precedente, quindi tenerne di più sarebbe memoria ferma.
     * ⚠️⚠️ **Software e non hardware**, ed è ciò che rende possibile l'esportazione: un
     * `Bitmap` con configurazione `HARDWARE` non si può leggere pixel per pixel né
     * comprimere su file. Qui non si chiede mai quella configurazione, e la nota sta qui
     * perché è il punto in cui un domani qualcuno sarebbe tentato di 'ottimizzare'.
     */
    private class Pool : GifDecoder.BitmapProvider {

        private var spare: Bitmap? = null

        override fun obtain(width: Int, height: Int, config: Bitmap.Config): Bitmap {
            val kept = spare
            if (kept != null && !kept.isRecycled &&
                kept.width == width && kept.height == height && kept.config == config
            ) {
                spare = null
                // ⚠️ Si azzera prima di ridarlo: il decodificatore disegna una toppa sopra
                // quello che trova, e quello che trova sarebbe il fotogramma di due giri fa.
                kept.eraseColor(0)
                return kept
            }
            return Bitmap.createBitmap(width, height, config)
        }

        override fun release(bitmap: Bitmap) {
            spare = bitmap
        }

        override fun obtainByteArray(size: Int): ByteArray = ByteArray(size)

        override fun release(bytes: ByteArray) = Unit

        override fun obtainIntArray(size: Int): IntArray = IntArray(size)

        override fun release(array: IntArray) = Unit
    }
}

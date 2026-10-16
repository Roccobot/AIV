package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream

/**
 * Un'immagine animata aperta: quanti fotogrammi ha, quanto durano, e come si scorrono.
 *
 * ⚠️ **Un contratto solo per due lettori diversi**: le GIF le legge [AnimatedGif] (un
 * decodificatore di terze parti), le WebP animate [AnimatedWebp] (un lettore di contenitore scritto
 * qui). Riproduzione, cache dei fotogrammi ed esportazione sono una cosa sola, o ci sarebbero due
 * riproduttori da tenere d'accordo.
 *
 * ⚠️ **I fotogrammi si scorrono in avanti**: in tutti e due i formati un fotogramma è una toppa
 * disegnata sopra quello di prima, e per vedere il fotogramma N bisogna aver composto i precedenti.
 * Per questo c'è [advance] e non un `frameAt(n)`; [seek] salta ricostruendo la pila, e una cache
 * dei fotogrammi già composti, se servisse, va sopra questo contratto e non nei lettori.
 *
 * [current] può tornare `null`: un file troncato o un fotogramma rifiutato dal decodificatore sono
 * frequenti fra le immagini prese dal web.
 */
interface Animated : Closeable {

    val width: Int
    val height: Int

    /** Quanti fotogrammi ha in tutto: uno solo vuol dire che non c'è niente di animato. */
    val frameCount: Int

    /**
     * Quante volte l'animazione si ripete, e zero vuol dire per sempre: lo scrivono così i due
     * formati (l'estensione NETSCAPE della GIF e il chunk `ANIM` della WebP).
     */
    val loopCount: Int

    /** Quanto dura il fotogramma [index], in millisecondi. */
    fun delayOf(index: Int): Int

    /** L'indice del fotogramma che [current] restituisce in questo momento. */
    val index: Int

    /** Il fotogramma corrente, già composto sopra i precedenti. */
    fun current(): Bitmap?

    /** Va al fotogramma dopo. Dopo l'ultimo si torna al primo. */
    fun advance()

    /**
     * Porta l'animazione al fotogramma [target], ricostruendo la pila quanto serve.
     *
     * ⚠️ **Saltare non è ripetere `advance`**: nella GIF il decodificatore separa lo spostamento
     * dell'indice dalla composizione del fotogramma, e N `advance()` di fila spostano l'indice
     * senza comporre i fotogrammi attraversati. La toppa d'arrivo si posa sulla tela vecchia, e
     * restano in scena le tracce di tutti i fotogrammi (la scia). Tornare indietro di uno costa la
     * pila rifatta da capo: è la natura del formato.
     */
    fun seek(target: Int)

    /**
     * Quanto dura tutta l'animazione, in millisecondi: la somma dei ritardi, perché nessuno dei due
     * formati la dichiara.
     */
    val duration: Int
        get() = (0 until frameCount).sumOf { delayOf(it) }
}

/**
 * Come si apre un'immagine animata, e come si riconosce che lo è.
 *
 * ⚠️ **Si guardano i byte, non l'estensione né il tipo MIME**: un file `.gif` può essere una PNG, e
 * il tipo del MediaStore viene a sua volta dal nome. La risposta decide se il visualizzatore mostra
 * un'immagine ferma o accende un riproduttore. Bastano pochi byte: la firma di una GIF è nei primi
 * 6, quella di una WebP animata nei primi 21.
 */
object Animations {

    /** Quanti byte bastano a riconoscere il formato: vedi [kindOf]. */
    private const val SNIFF = 64

    enum class Kind { GIF, WEBP, NONE }

    /**
     * Che genere di animazione promettono i primi byte di un file.
     *
     * ⚠️ **Dice 'forse', non 'sì'**: una GIF di un fotogramma ha la stessa firma di una animata, e
     * alcuni codificatori mettono il bit dell'animazione anche su WebP di un fotogramma. Il conto
     * vero lo sa il lettore, quindi [open] apre e poi richiude quello che si rivela fermo.
     */
    fun kindOf(head: ByteArray): Kind {
        if (head.size >= 6 && head[0] == 'G'.code.toByte() && head[1] == 'I'.code.toByte() &&
            head[2] == 'F'.code.toByte() && head[3] == '8'.code.toByte()
        ) {
            return Kind.GIF
        }
        // Una WebP è un contenitore RIFF: 'RIFF' in testa, 'WEBP' all'ottavo byte, poi il chunk
        // 'VP8X' con i flag, dove il bit 0x02 del primo byte dice che c'è un'animazione. Senza VP8X
        // la WebP ha un fotogramma solo.
        if (head.size >= 21 &&
            String(head, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            String(head, 8, 4, Charsets.US_ASCII) == "WEBP" &&
            String(head, 12, 4, Charsets.US_ASCII) == "VP8X" &&
            (head[20].toInt() and 0x02) != 0
        ) {
            return Kind.WEBP
        }
        return Kind.NONE
    }

    /**
     * Apre l'immagine animata a [uri], o `null` se non c'è niente da animare.
     *
     * Il file si legge tutto in memoria, fino a [MAX_BYTES]: tutti e due i lettori tornano indietro
     * nei byte, e uno `InputStream` non si riavvolge. Oltre il tetto si mostra l'immagine ferma. Un
     * fotogramma solo non è un'animazione: si chiude e si torna `null`, così il visualizzatore
     * prende la strada normale.
     */
    fun open(context: Context, uri: Uri): Animated? = runCatching {
        val bytes = bytesOf(context, uri) ?: return null
        val animated = when (kindOf(bytes.copyOf(minOf(SNIFF, bytes.size)))) {
            Kind.GIF -> AnimatedGif.open(bytes)
            Kind.WEBP -> AnimatedWebp.open(bytes)
            Kind.NONE -> null
        } ?: return null
        if (animated.frameCount <= 1) {
            animated.close()
            return null
        }
        animated
    }.getOrNull()

    /**
     * I byte del file a [uri], da qualunque parte arrivi.
     *
     * ⚠️ **Un indirizzo di rete non passa dal `ContentResolver`**: su `https` `openInputStream` va
     * in errore, il `runCatching` di [open] lo ingoia, e la GIF remota resta ferma senza nessun
     * errore. I byte remoti li ha già in cache `ImageSource.loadRemote`, quindi si rileggono senza
     * scaricarli di nuovo.
     * ⚠️ **Il primo tentativo può arrivare prima che il download sia finito**: a ritentare pensa
     * [rememberAnimation], che riprova quando l'immagine è pronta.
     */
    private fun bytesOf(context: Context, uri: Uri): ByteArray? =
        when (uri.scheme?.lowercase()) {
            "http", "https" -> RemoteCache.read(context, uri)?.takeIf {
                it.size <= MAX_BYTES && kindOf(it.copyOf(minOf(SNIFF, it.size))) != Kind.NONE
            }
            else -> context.contentResolver.openInputStream(uri)?.use { candidate(it) }
        }

    /**
     * Tutto il flusso, ma solo se può essere un'animazione: altrimenti `null`.
     *
     * ⚠️ **I primi byte decidono prima che si legga il resto**: il visualizzatore fa questa domanda
     * per ogni pagina, filmati compresi, e un MKV di 250 MB letto tutto in memoria chiudeva l'app.
     * Un file che non è né GIF né WebP costa [SNIFF] byte, e uno oltre [MAX_BYTES] si scarta appena
     * il flusso lo supera; presidiato da `AnimazioniGrandiTest`. La testa si legge a mano perché
     * `readNBytes` esiste solo da Android 13, e un `read` può restituire meno byte di quelli
     * chiesti.
     */
    internal fun candidate(stream: InputStream): ByteArray? {
        val head = ByteArray(SNIFF)
        var got = 0
        while (got < SNIFF) {
            val n = stream.read(head, got, SNIFF - got)
            if (n < 0) break
            got += n
        }
        if (kindOf(head.copyOf(got)) == Kind.NONE) return null
        val out = ByteArrayOutputStream().apply { write(head, 0, got) }
        val buffer = ByteArray(BUFFER)
        while (true) {
            val n = stream.read(buffer)
            if (n < 0) return out.toByteArray()
            out.write(buffer, 0, n)
            if (out.size() > MAX_BYTES) return null
        }
    }

    /** Quanto legge [candidate] a ogni giro. */
    private const val BUFFER = 64 * 1024

    /**
     * Oltre questo peso non si anima. Il tetto è sul file e non sui pixel: i lettori tengono in
     * memoria pochi fotogrammi per volta, anche in una GIF che ne ha migliaia. Chi supera il tetto
     * vede comunque l'immagine, ferma.
     */
    private const val MAX_BYTES = 32 * 1024 * 1024
}

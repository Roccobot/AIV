package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.Closeable

/**
 * Un'immagine animata aperta: quanti fotogrammi ha, quanto durano, e come si scorrono.
 *
 * ⚠️⚠️ **UN CONTRATTO SOLO DAVANTI A DUE LETTORI DIVERSI, ed è la scelta che tiene in piedi
 * tutto il resto**: le GIF le legge [AnimatedGif], le WebP animate [AnimatedWebp], e i due
 * non si somigliano affatto (uno è un decodificatore di terze parti, l'altro è un lettore
 * di contenitore scritto qui). Ma i comandi di riproduzione, la cache dei fotogrammi e
 * l'esportazione devono essere **una cosa sola**: se ognuno dei due formati si portasse
 * dietro la sua interfaccia, ci sarebbero due riproduttori da tenere d'accordo, e il secondo
 * prenderebbe le correzioni del primo con mesi di ritardo.
 *
 * ⚠️⚠️ **I FOTOGRAMMI SI SCORRONO IN AVANTI, e non si salta a piacere**: in tutti e due i
 * formati un fotogramma è una **toppa** disegnata sopra quello di prima, non un'immagine a
 * sé. Per vedere il fotogramma N bisogna aver composto tutti i precedenti, ed è il motivo
 * per cui qui c'è [advance] e non un `frameAt(n)`.
 * ⚠️ **[seek] non è la smentita di quella riga, è la sua conseguenza**: il salto esiste, ma
 * costa la ricostruzione della pila, e sta nel contratto proprio perché farlo bene richiede
 * di sapere come è fatto il lettore. Una cache dei fotogrammi già composti, se un giorno
 * servisse, va **sopra** questo contratto e non dentro i lettori.
 *
 * ⚠️ **[current] può tornare `null`**, e chi chiama deve reggerlo: un file troncato o un
 * fotogramma che il decodificatore rifiuta non sono un caso raro su immagini prese dal web.
 */
interface Animated : Closeable {

    val width: Int
    val height: Int

    /** Quanti fotogrammi ha in tutto. Uno solo vuol dire che di animato non c'è niente. */
    val frameCount: Int

    /**
     * Quante volte l'animazione si ripete, e **zero vuol dire per sempre**.
     *
     * ⚠️ Lo zero come infinito non è una nostra invenzione: è così che lo scrivono i due
     * formati (l'estensione NETSCAPE della GIF e il chunk `ANIM` della WebP), e tradurlo in
     * qualcos'altro qui vorrebbe dire ritradurlo indietro per mostrarlo.
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

    /*
     * ⚠️ **`rewind()` NON STA PIÙ QUI, dalla 1.21**: era la sola via per tornare indietro,
     * e chi la usava doveva poi rifare la pila a mano, che è esattamente l'errore che ha
     * prodotto la scia. Adesso quel lavoro lo fa [seek], e il riavvolgimento è un dettaglio
     * interno di chi sa come è fatta la propria tela.
     */

    /**
     * Porta l'animazione al fotogramma [target], ricostruendo la pila quel tanto che serve.
     *
     * ⚠️⚠️ **ESISTE PERCHÉ SALTARE NON È RIPETERE `advance`, ed è il difetto della scia**
     * (riscontro dell'utente, 2026-09-01, con la fotografia della scia: *se muovo all'indietro
     * i fotogrammi succede questo*). Nella GIF il decodificatore separa lo **spostamento**
     * dell'indice dalla **composizione** del fotogramma: `advance()` muove e basta, la toppa
     * la posa la lettura. Un salto scritto come `rewind()` più N `advance()` sposta l'indice
     * di N caselle **senza comporre nessuno** dei fotogrammi attraversati, e la tela resta
     * quella di prima: la toppa del fotogramma d'arrivo si posa sopra tutte le altre, e le
     * palline restano tutte in scena. Era esattamente quello che si vedeva.
     * ⚠️ **Costa quello che il formato impone**: tornare indietro di uno vuol dire rifare la
     * pila da capo, cioè decodificare N fotogrammi. Non è una lentezza da togliere: è la
     * proprietà di un formato in cui il fotogramma N esiste solo come somma dei precedenti.
     * La cura, se un giorno servisse, è una cache dei fotogrammi già composti **sopra**
     * questo contratto.
     */
    fun seek(target: Int)

    /**
     * Quanto dura tutta l'animazione, in millisecondi.
     *
     * ⚠️ Si somma invece di chiederlo al formato, perché nessuno dei due lo dichiara: la
     * durata totale **è** la somma dei ritardi, e tenerla in un campo vorrebbe dire un
     * secondo posto da cui può mentire.
     */
    val duration: Int
        get() = (0 until frameCount).sumOf { delayOf(it) }
}

/**
 * Come si apre un'immagine animata, e come si riconosce che lo è.
 *
 * ⚠️⚠️ **SI GUARDANO I BYTE, NON L'ESTENSIONE NÉ IL TIPO MIME**: un file chiamato `.gif`
 * può essere una PNG, e il tipo che dichiara il MediaStore viene a sua volta dal nome. Qui
 * la domanda 'è animata?' decide se il visualizzatore mostra un'immagine ferma o accende un
 * riproduttore, quindi sbagliarla si vede subito.
 * ⚠️ **E si guardano POCHI byte**: la firma di una GIF sta nei primi 6, quella di una WebP
 * animata nei primi 21. [SNIFF] è il tetto, e ci sta dentro con abbondanza.
 */
object Animations {

    /** Quanti byte bastano a riconoscere il formato: vedi [kindOf]. */
    private const val SNIFF = 64

    enum class Kind { GIF, WEBP, NONE }

    /**
     * Che genere di animazione promettono i primi byte di un file.
     *
     * ⚠️⚠️ **DICE 'FORSE', NON 'SÌ', e la differenza conta**: una GIF con un fotogramma solo
     * ha la stessa firma di una animata, e la WebP dichiara l'animazione in un bit che
     * qualche codificatore mette anche su file di un fotogramma. Il conteggio vero lo sa solo
     * il lettore aperto, quindi [open] apre e poi **richiude** quello che si rivela fermo.
     */
    fun kindOf(head: ByteArray): Kind {
        if (head.size >= 6 && head[0] == 'G'.code.toByte() && head[1] == 'I'.code.toByte() &&
            head[2] == 'F'.code.toByte() && head[3] == '8'.code.toByte()
        ) {
            return Kind.GIF
        }
        // ⚠️ Una WebP è un contenitore RIFF: 'RIFF' in testa, 'WEBP' all'ottavo byte, e poi
        // il chunk 'VP8X' con i flag. Il bit 0x02 del primo byte dei flag è quello che dice
        // 'qui dentro c'è un'animazione'. Senza VP8X la WebP è per forza di un fotogramma.
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
     * Apre l'immagine animata che sta a [uri], e `null` se non c'è niente da animare.
     *
     * ⚠️⚠️ **IL FILE SI LEGGE TUTTO IN MEMORIA, ed è una scelta con un tetto**: tutti e due
     * i lettori hanno bisogno di tornare indietro nei byte (la GIF per ricomporre dal primo
     * fotogramma, la WebP per estrarre un fotogramma qualsiasi), e uno `InputStream` che non
     * si riavvolge non basta. Il tetto è [MAX_BYTES]: oltre, si rinuncia all'animazione e si
     * mostra l'immagine ferma, che è molto meglio di un'app che si chiude.
     * ⚠️ **Un fotogramma solo non è un'animazione**: si chiude e si torna `null`, così il
     * visualizzatore prende la strada normale invece di accendere comandi che non servono.
     */
    fun open(context: Context, uri: Uri): Animated? = runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.use { source ->
            val out = source.readBytes()
            if (out.size > MAX_BYTES) return null
            out
        } ?: return null
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
     * Oltre questo peso non si anima.
     *
     * ⚠️ **32 MB è il FILE, non i pixel**, e i pixel sono la parte che spaventa: una GIF di
     * 32 MB compressi può avere migliaia di fotogrammi, ma i lettori ne tengono in memoria
     * pochi per volta (uno il decodificatore, qualcuno la cache), quindi il file è il numero
     * giusto da limitare. ⚠️ Chi supera il tetto **vede comunque l'immagine**, ferma: non è
     * un errore, è una rinuncia all'animazione.
     */
    private const val MAX_BYTES = 32 * 1024 * 1024
}

package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.times
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * Il nome accorciato quanto basta a entrare in [lines] righe, con l'estensione salva, in
 * grassetto e mai spezzata. Lo usano la pastiglia di 'Info dettagliate sul file' (tre righe), il
 * nome sotto la miniatura in griglia (due) e la barra del visualizzatore (una).
 *
 * ⚠️⚠️ **Si misura, non si contano i caratteri**: le lettere non hanno la stessa larghezza, il
 * grassetto è più largo, e la larghezza utile cambia col riquadro. Si chiede al misuratore se il
 * testo già impaginato coi suoi pesi sfora, la stessa domanda che farà il layout. Ricerca binaria:
 * su un nome di duecento caratteri sono otto misure e non duecento.
 * L'estensione è legata dal giuntore di parole ([WORD_JOINER]) e preceduta da un appiglio
 * ([BREAK_HERE]): vedi le loro note. Lo spazio dopo l'ellissi è un punto in cui il layout può
 * andare a capo, ed è ammesso: il vincolo è che l'estensione non si spezzi.
 * ⚠️ **`TextOverflow.Ellipsis` toglierebbe proprio l'estensione**, perché mette i tre punti in
 * fondo. `TextOverflow.MiddleEllipsis` divide sulla larghezza senza garantire l'estensione
 * intera, non stringe il corpo e non tiene il grassetto sul suffisso.
 */
fun fitName(
    name: String,
    room: Int,
    lines: Int,
    style: TextStyle,
    measurer: TextMeasurer
): Fitted {
    fun comporre(testa: String, coda: String) = buildAnnotatedString {
        append(testa)
        if (coda.isNotEmpty()) {
            /*
             * ⚠️⚠️ **L'appiglio prima dell'estensione serve anche qui**, come in [nameWithExt]:
             * senza, un nome senza spazi si rompe dove capita, giuntori compresi (vedi
             * [BREAK_HERE]).
             */
            append(BREAK_HERE)
            withStyle(EXT_BOLD) { append(coda) }
        }
    }

    fun sta(testo: AnnotatedString, stile: TextStyle) = room <= 0 || !measurer.measure(
        text = testo,
        style = stile,
        maxLines = lines,
        constraints = Constraints(maxWidth = room)
    ).hasVisualOverflow

    val punto = name.lastIndexOf('.')
    // ⚠️ `punto > 0`: un nome che comincia col punto è un file nascosto, e quel punto fa parte
    // del nome.
    val coda = if (punto > 0) glue(name.substring(punto)) else ""
    val corpo = if (punto > 0) name.substring(0, punto) else name

    /*
     * ⚠️ **Prima si stringe, poi si accorcia** (ordine di priorità dell'utente): un nome intero un
     * filo più piccolo vale più di un nome accorciato. Le strette da provare sono in [strette].
     * Se nemmeno l'ultima basta, si accorcia al corpo pieno: la stretta serve ai casi limite, e su
     * un nome lunghissimo il taglio arriva comunque. Un nome che entra alla misura piena costa una
     * misura sola.
     */
    val intero = comporre(corpo, coda)
    for (stile in strette(style)) {
        if (sta(intero, stile)) return Fitted(intero, stile)
    }

    var basso = 0
    var alto = corpo.length
    while (basso < alto) {
        val mezzo = (basso + alto + 1) / 2
        if (sta(comporre(corpo.take(mezzo) + CUT, coda), style)) basso = mezzo else alto = mezzo - 1
    }
    return Fitted(comporre(corpo.take(basso) + CUT, coda), style)
}

/**
 * Le strette da provare, dalla meno visibile alla più visibile.
 *
 * ⚠️ **La spaziatura viene prima del corpo**: stringerla di [NAME_TIGHT] guadagna circa quanto
 * un gradino di corpo e si vede molto meno, perché un corpo più piccolo si nota accanto a un altro
 * nome della griglia. Una volta stretta resta stretta mentre il corpo scende.
 *
 * È `internal` perché il banco di prova la misuri (come `pinTop` in `Centred.kt`): sulla
 * piattaforma finta ogni carattere è largo un pixel, quindi una prova che passasse da [fitName]
 * non eserciterebbe quest'ordine.
 *
 * Non c'è l'ingrandimento, che l'utente aveva ammesso se aiutava ad andare a capo meglio: un
 * corpo più grande non crea nessun punto in cui andare a capo, quindi non evita nessuna rottura.
 */
internal fun strette(style: TextStyle): Sequence<TextStyle> = sequence {
    yield(style)
    /*
     * ⚠️ **I gradini si contano interi**: sottraendo [NAME_STEP] in virgola mobile, l'errore di
     * arrotondamento fermerebbe la corsa all'85% e non all'80%. Col contatore il valore si
     * ricostruisce a ogni passo, e l'ultimo gradino è quello scritto.
     */
    val gradini = ((1f - NAME_FLOOR) / NAME_STEP).roundToInt()
    for (passo in 0..gradini) {
        yield(style.shrunk(1f - NAME_STEP * passo, tight = true))
    }
}

/**
 * Il nome impaginato e lo stile a cui è stato misurato.
 *
 * ⚠️ **Contiene lo stile e non una scala**: chi disegna non deve riapplicare le strette, e una
 * dimenticata darebbe un testo che sfora dopo essere stato misurato dentro.
 */
data class Fitted(val text: AnnotatedString, val style: TextStyle)

/**
 * Lo stesso stile, col corpo e l'interlinea stretti di [k] e, se [tight], le lettere più vicine.
 *
 * ⚠️ **Una copia sola**: la usano il misuratore di [fitName] e la pastiglia dell'anteprima della
 * rinomina, e due copie vorrebbero dire misurare a un corpo e scrivere a un altro.
 * Le due misure si guardano prima di moltiplicarle: un valore `Unspecified` non si scala.
 * La spaziatura si sostituisce: quella di partenza è in `sp` negli stili di Material e questa in
 * `em`, due unità che non si sommano. Si perde meno di un centesimo di em.
 */
internal fun TextStyle.shrunk(k: Float, tight: Boolean = false): TextStyle {
    if (k >= 1f && !tight) return this
    val stretto = k < 1f
    return copy(
        fontSize = if (stretto && fontSize.isSpecified) fontSize * k else fontSize,
        lineHeight = if (stretto && lineHeight.isSpecified) lineHeight * k else lineHeight,
        letterSpacing = if (tight) NAME_TIGHT else letterSpacing
    )
}

/**
 * Di quanto si stringe il corpo a ogni passata, e fin dove.
 *
 * Sotto l'80% due nomi vicini smettono di leggersi come una coppia, perché hanno due corpi
 * diversi. Il gradino del 5%: più fine vorrebbe più misure per un guadagno che non si vede, più
 * grosso salterebbe la misura giusta.
 */
internal const val NAME_STEP = 0.05f
internal const val NAME_FLOOR = 0.80f

/**
 * Di quanto si avvicinano le lettere quando la spaziatura è la leva in uso.
 *
 * ⚠️ **In `em`, perché valga lo stesso su ogni corpo**: in `sp` stringerebbe un testo piccolo
 * molto più di uno grande, e i corpi in gioco sono due (`titleSmall` e `labelSmall`).
 * Il 2% dell'em, su un carattere largo in media mezzo em, stringe la riga del 4% circa, quanto un
 * gradino di corpo; il doppio comincerebbe a far toccare le lettere.
 */
internal val NAME_TIGHT = (-0.02).em

/**
 * Il nome col punto e l'estensione **in grassetto** e **mai spezzati** andando a capo.
 *
 * Serve dove il nome non si accorcia, cioè dove [fitName] non arriva (le pastiglie della rinomina).
 *
 * ⚠️⚠️ **L'estensione non si spezza, ed è una regola generale dell'utente**: un nome di file si
 * legge come corpo più estensione, e un'estensione tagliata in due non si riconosce. Il punto
 * entra nel pezzo saldato da [glue], o si andrebbe a capo dopo il punto, ed entra anche nel
 * grassetto (riscontro `ext-griglia`): si legge `nome` più `.avif`. Grassetto e giuntore sono
 * indipendenti: il peso non impedisce di andare a capo, e il giuntore non si vede.
 */
fun nameWithExt(name: String): AnnotatedString {
    val punto = name.lastIndexOf('.')
    if (punto <= 0) return AnnotatedString(name)
    return buildAnnotatedString {
        append(name.substring(0, punto))
        append(BREAK_HERE)
        withStyle(EXT_BOLD) { append(glue(name.substring(punto))) }
    }
}

/**
 * Il peso dell'estensione, uno per tutta l'app (griglia, barra del visualizzatore, pastiglia di
 * 'Info', anteprima della rinomina): copie diverse divergerebbero al primo ritocco.
 *
 * `Black` per il riscontro `ext-grassetto`. ⚠️ **Non `ExtraBold`**: prima di Android 12 il Roboto
 * di sistema non ha una faccia per l'800, e la regola di abbinamento lo porterebbe a 900 su quei
 * telefoni e a 800 su quelli nuovi. Il 900 è lo stesso dappertutto.
 */
val EXT_BOLD = SpanStyle(fontWeight = FontWeight.Black)

/** L'estensione con un giuntore fra ogni carattere, così il layout non la spezza. */
private fun glue(ext: String): String = ext.toCharArray().joinToString(WORD_JOINER)

/**
 * `U+200B ZERO WIDTH SPACE`: invisibile, e **concede** al layout di andare a capo in quel punto.
 *
 * ⚠️⚠️ **Il giuntore da solo non basta**: vieta di spezzare dentro l'estensione finché il layout
 * ha un altro posto in cui andare a capo. In un nome come `io.sono.flemd_3977.mp4` un punto fra
 * due lettere o fra una cifra e una lettera non apre un appiglio, quindi tutto il nome è una
 * parola sola, e una parola più larga della riga il layout la rompe dove capita, giuntori
 * compresi. Con questo carattere prima del punto c'è un posto legale in cui andare a capo, e
 * l'estensione scende intera sulla riga dopo.
 * Scritto per codepoint, come [WORD_JOINER]. ⚠️ Solo per la resa, mai in un nome vero, che
 * conterrebbe un carattere invisibile.
 */
private const val BREAK_HERE = "\u200B"

/**
 * Il taglio: i tre punti e **uno spazio**.
 *
 * Tre punti e non il carattere unico, che il repo vieta. Lo spazio (correzione dell'utente)
 * evita che l'ellissi si saldi al punto dell'estensione e si legga `....HEIC`.
 */
private const val CUT = "... "

/**
 * `U+2060 WORD JOINER`: invisibile, e vieta al layout di andare a capo in quel punto.
 *
 * Un nome di file non ha spazi, quindi senza questo carattere il layout potrebbe andare a capo
 * fra due lettere qualunque e spezzare `.HEIC` in `.HE` e `IC`.
 * Scritto per codepoint, come vuole la regola del repo sui caratteri invisibili. Non è lo spazio
 * insecabile (`U+00A0`), che è vietato: questo non occupa larghezza.
 */
private const val WORD_JOINER = "\u2060"

/**
 * I nomi dei file già letti, uno per indirizzo.
 *
 * ⚠️ **Esiste per la griglia** (impostazione 'Nome del file in vista griglia'): il nome di un
 * `content://` va chiesto al MediaStore, e scorrendo una cartella grande la domanda si
 * ripeterebbe centinaia di volte per la stessa immagine. `Folder.Series` è una lista di soli
 * indirizzi, e farle contenere i nomi cambierebbe il visualizzatore, la ricerca e il cestino per
 * un'impostazione spenta di fabbrica: una domanda per immagine, una volta sola.
 * Vive quanto il processo e non si svuota: un nome occupa poche decine di byte. Una rinomina di
 * AIV GitHub cambia l'indirizzo; quella di AIV Play lo lascia uguale, e chiama [forget].
 */
object Names {

    /**
     * Il nome se è già stato letto, e `null` se non lo si sa ancora.
     *
     * ⚠️ **Evita che la griglia lampeggi**: una lettura sospesa vale comunque una
     * ricomposizione, quindi senza questa ogni miniatura che rientra in vista avrebbe un
     * fotogramma senza nome. Così il primo disegno ha già il testo.
     */
    fun cached(uri: Uri): String? = known[uri]

    suspend fun of(context: Context, uri: Uri): String {
        known[uri]?.let { return it }
        val letto = withContext(Dispatchers.IO) { FileTree.displayName(context, uri) }
            ?: uri.lastPathSegment.orEmpty()
        known[uri] = letto
        return letto
    }

    /**
     * Dimentica il nome di [uri], quando il file dietro di lui ne ha uno nuovo.
     *
     * Serve alla rinomina di AIV Play (`MediaRename`): il MediaStore rinomina il file sul posto,
     * e l'indirizzo resta lo stesso.
     */
    fun forget(uri: Uri) {
        known.remove(uri)
    }

    private val known = ConcurrentHashMap<Uri, String>()
}

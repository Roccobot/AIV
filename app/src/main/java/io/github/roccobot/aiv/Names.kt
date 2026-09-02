package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Il nome accorciato quanto basta a starci in [lines] righe, con l'estensione salva, in
 * grassetto e mai spezzata.
 *
 * ⚠️⚠️ **STA IN UN FILE A SÉ DALLA 0.82, quando i posti che accorciano un nome sono diventati
 * DUE**: la pastiglia di 'Info dettagliate sul file' (tre righe) e il nome sotto la miniatura
 * in griglia (due, richiesta dell'utente). Il numero di righe è quindi un parametro e non più
 * una costante: è la sola cosa che cambia fra i due.
 *
 * ⚠️⚠️ **SI MISURA, non si conta**: un tetto di caratteri sarebbe sbagliato tre volte, perché
 * le lettere non hanno tutte la stessa larghezza (`WWW` occupa il triplo di `iii`), perché il
 * grassetto è più largo del peso normale, e perché la larghezza utile cambia col riquadro. Qui
 * si chiede al misuratore se il testo sfora, che è la stessa domanda che si farà il layout, e
 * gliela si chiede sul testo **già impaginato coi suoi pesi**.
 * ⚠️ **Ricerca binaria e non un ciclo che toglie una lettera per volta**: su un nome di
 * duecento caratteri sarebbero duecento misure a ogni composizione. Così sono otto.
 * ⚠️⚠️ **Il GIUNTORE DI PAROLE (`U+2060`) dentro l'estensione è l'unico modo di rispettare
 * il 'mai'**: un nome di file non ha spazi, quindi il layout può andare a capo fra due
 * caratteri qualunque, e `.HEIC` finirebbe a cavallo di due righe come `.HE` più `IC`.
 * Quel carattere è invisibile e dice al layout 'qui non si rompe'. Scritto per codepoint,
 * come vuole la regola del repo sui caratteri invisibili.
 * ⚠️ **Lo spazio dopo l'ellissi, invece, è un punto in cui il layout PUÒ andare a capo**, e
 * va bene: il vincolo era che l'estensione non si spezzi, non che stia sulla stessa riga del
 * nome.
 * ⚠️⚠️ **`TextOverflow.Ellipsis` NON serve e farebbe il danno esatto che si vuole evitare**:
 * mette i tre punti alla **fine**, cioè mangia proprio l'estensione, che è la parte che
 * l'utente ha chiesto di salvare sempre. Serve un'ellissi **in mezzo**, che Compose non ha.
 */
fun fitName(
    name: String,
    room: Int,
    lines: Int,
    style: TextStyle,
    grassetto: SpanStyle,
    measurer: TextMeasurer
): AnnotatedString {
    fun comporre(testa: String, coda: String) = buildAnnotatedString {
        append(testa)
        if (coda.isNotEmpty()) withStyle(grassetto) { append(coda) }
    }

    fun sta(testo: AnnotatedString) = room <= 0 || !measurer.measure(
        text = testo,
        style = style,
        maxLines = lines,
        constraints = Constraints(maxWidth = room)
    ).hasVisualOverflow

    val punto = name.lastIndexOf('.')
    // ⚠️ `punto > 0` e non `>= 0`: un nome che comincia col punto è un file nascosto, e là
    // quel punto non introduce un'estensione, fa parte del nome.
    val coda = if (punto > 0) glue(name.substring(punto)) else ""
    val corpo = if (punto > 0) name.substring(0, punto) else name

    val intero = comporre(corpo, coda)
    if (sta(intero)) return intero

    var basso = 0
    var alto = corpo.length
    while (basso < alto) {
        val mezzo = (basso + alto + 1) / 2
        if (sta(comporre(corpo.take(mezzo) + CUT, coda))) basso = mezzo else alto = mezzo - 1
    }
    return comporre(corpo.take(basso) + CUT, coda)
}

/**
 * Lo stesso nome, ma con l'estensione che **non si può spezzare** andando a capo.
 *
 * ⚠️⚠️ **È UNA REGOLA GENERALE E NON UN RITOCCO DELL'ANTEPRIMA** (riscontro dell'utente,
 * 2026-09-02, sulla rinomina: *il nome del file va a capo spezzando l'estensione (nella mia
 * prova: `.a⏎vif`) -> da mettere a posto: è una regola generale*). Un nome di file si legge
 * come **corpo più estensione**, e un'estensione tagliata in due smette di essere
 * riconoscibile: `.a` su una riga e `vif` sull'altra non si leggono più come AVIF.
 * ⚠️ **Serve dove il nome sta su PIÙ righe**, cioè dove [fitName] non arriva: quella
 * accorcia in mezzo e tiene una riga sola, quindi l'estensione la salda già da sé (la stessa
 * [WORD_JOINER], usata dentro). Qui il nome resta intero e si va a capo, e il giuntore è
 * l'unica cosa che dice al layout dove non può tagliare.
 * ⚠️ **Il punto entra nel pezzo saldato**, non solo le lettere: senza, il layout potrebbe
 * andare a capo **dopo** il punto, che è esattamente il caso che l'utente ha visto.
 */
fun unbroken(name: String): String {
    // ⚠️ `punto > 0` e non `>= 0`, come in [fitName]: un nome che comincia col punto è un
    // file nascosto, e là quel punto non introduce un'estensione.
    val punto = name.lastIndexOf('.')
    if (punto <= 0) return name
    return name.substring(0, punto) + glue(name.substring(punto))
}

/** L'estensione con un giuntore fra ogni carattere, così il layout non la spezza. */
private fun glue(ext: String): String = ext.toCharArray().joinToString(WORD_JOINER)

/**
 * Il taglio: i tre punti e **uno spazio**.
 *
 * ⚠️ I tre punti si scrivono così e non col carattere unico, che il repo vieta. Lo spazio
 * è la correzione dell'utente al difetto dei quattro punti di fila: senza di lui l'ellissi
 * si salda al punto dell'estensione e si legge `....HEIC`.
 */
private const val CUT = "... "

/**
 * `U+2060 WORD JOINER`: invisibile, e vieta al layout di andare a capo dove sta.
 *
 * ⚠️ Scritto per **codepoint** e non incollato, come vuole la regola del repo sui caratteri
 * invisibili: incollato, questo file conterrebbe un carattere che a schermo non si vede e
 * che nessuno saprebbe di aver toccato.
 * ⚠️ **Non è lo spazio insecabile** (`U+00A0`), che è vietato: quello è uno spazio e si
 * vede, questo non occupa larghezza e serve solo a legare due caratteri.
 */
private const val WORD_JOINER = "\u2060"

/**
 * I nomi dei file già letti, uno per indirizzo.
 *
 * ⚠️⚠️ **ESISTE PER LA GRIGLIA, dove il nome si legge una miniatura per volta** (impostazione
 * 'Nome del file in vista griglia', dalla `0.82`): il nome di un `content://` non si ricava
 * dall'indirizzo, va chiesto al MediaStore, e una cartella da trecento foto scorsa avanti e
 * indietro farebbe quella domanda centinaia di volte per la stessa fotografia.
 * ⚠️ **La serie NON porta i nomi, ed è la ragione per cui questa memoria esiste**: `Folder.
 * Series` è una lista di indirizzi, e farle portare anche i nomi vorrebbe dire cambiarne la
 * forma e con lei il visualizzatore, la ricerca e il cestino, per un'impostazione che di
 * fabbrica è **spenta**. Il baratto è dichiarato: una domanda per fotografia, una volta sola.
 * ⚠️ **Vive quanto il processo e non si svuota**: un nome sta in poche decine di byte, e la
 * cosa peggiore che può fare una voce vecchia è descrivere un file rinominato. Chi rinomina
 * passa da `FileTree`, che fa rileggere la cartella, e la griglia chiede di nuovo con
 * l'indirizzo **nuovo**.
 */
object Names {

    /**
     * Il nome se è già stato letto, e `null` se non lo si sa ancora.
     *
     * ⚠️ **Serve a non far lampeggiare la griglia**: senza, ogni miniatura che rientra in
     * vista comparirebbe un fotogramma senza nome anche quando il nome è lì da un pezzo,
     * perché una lettura sospesa vale comunque una ricomposizione. Con questa, il primo
     * disegno ha già il testo e la ricomposizione non serve.
     */
    fun cached(uri: Uri): String? = known[uri]

    suspend fun of(context: Context, uri: Uri): String {
        known[uri]?.let { return it }
        val letto = withContext(Dispatchers.IO) { FileTree.displayName(context, uri) }
            ?: uri.lastPathSegment.orEmpty()
        known[uri] = letto
        return letto
    }

    private val known = ConcurrentHashMap<Uri, String>()
}

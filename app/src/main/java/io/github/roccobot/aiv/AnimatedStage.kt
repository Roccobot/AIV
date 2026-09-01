package io.github.roccobot.aiv

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.NavigateBefore
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Un'immagine animata mentre la si guarda: il fotogramma corrente e se sta andando.
 *
 * ⚠️⚠️ **LA DECODIFICA NON STA SUL FILO PRINCIPALE, e non è una precauzione teorica**:
 * comporre un fotogramma vuol dire decomprimere e disegnare una tela grande quanto
 * l'immagine, e farlo dove Compose disegna vorrebbe dire saltare fotogrammi **della
 * schermata**, non dell'animazione. Il ciclo gira su [Dispatchers.Default] e allo stato
 * arriva soltanto il risultato.
 *
 * ⚠️⚠️ **OGNI GIRO CREA UN INVOLUCRO NUOVO, ed è la trappola di questo file**: i due lettori
 * **riusano lo stesso `Bitmap`** per non allocare venti tele al secondo, quindi i pixel
 * cambiano ma l'oggetto resta quello. Se si tenesse lo stesso [ImageBitmap], Compose non
 * vedrebbe nessun cambiamento e l'animazione starebbe **ferma pur girando**. `asImageBitmap()`
 * costruisce un involucro nuovo a ogni chiamata, ed è per questo che si chiama a ogni
 * fotogramma invece di ricordarlo.
 */
@Stable
class Animation(private val source: Animated) {

    /** Se l'animazione sta andando. Parte accesa, come ci si aspetta da una GIF. */
    var playing by mutableStateOf(true)
        private set

    /** Il fotogramma da disegnare, e `null` finché il primo non è pronto. */
    var frame by mutableStateOf<ImageBitmap?>(null)
        private set

    /** A che fotogramma siamo, contando da uno per chi legge. */
    var shown by mutableIntStateOf(1)
        private set

    /**
     * Un comando per volta sul lettore.
     *
     * ⚠️⚠️ **SENZA QUESTO IL CICLO DI RIPRODUZIONE E I DUE PASSI SI PESTANO I PIEDI, ed è il
     * difetto che l'utente ha visto come 'scia' sulle GIF e rettangoli bianchi e neri sulle
     * WebP** (riscontro del 2026-09-01). I due passi mettono in pausa, ma **la pausa non è
     * istantanea**: il ciclo può essere già dentro `advance()` o dentro la composizione, e
     * cancellarlo non lo ferma a metà di quelle. Per un istante due coroutine compongono
     * sulla **stessa tela**, e quello che si vede è una tela scritta da due mani.
     * ⚠️ **Il rimedio è la serializzazione e non un ritardo**: aspettare un attimo prima di
     * fare il passo renderebbe il difetto raro invece di impossibile, e i difetti rari sono
     * quelli che tornano dopo il rilascio.
     * ⚠️ **Copre anche [snapshot]**, che legge il fotogramma corrente per esportarlo: senza
     * il lucchetto potrebbe copiarne uno composto a metà.
     */
    private val turn = Mutex()

    val frameCount: Int get() = source.frameCount

    fun toggle() {
        playing = !playing
    }

    /**
     * Un fotogramma avanti, e mette in pausa se stava andando.
     *
     * ⚠️ **Mettere in pausa fa parte del comando**: chiedere un fotogramma preciso mentre
     * l'animazione corre vorrebbe dire vederlo per un decimo di secondo e perderlo.
     */
    suspend fun stepForward() = guarded {
        playing = false
        turn.withLock {
            withContext(Dispatchers.Default) { source.advance() }
            draw()
        }
    }

    /**
     * Un fotogramma indietro.
     *
     * ⚠️⚠️ **COSTA N E NON UNO, ed è una proprietà del FORMATO e non di questo codice**: in
     * GIF come in WebP un fotogramma è una **toppa** disegnata sopra il precedente, quindi
     * il fotogramma N esiste solo come risultato di tutti quelli prima. Per tornare indietro
     * di uno bisogna rifare la pila da capo.
     * ⚠️ **Gira fuori dal filo principale**, che è quello che lo rende sopportabile: su
     * un'animazione di duecento fotogrammi il ritorno dal fondo è qualche decimo di secondo,
     * e nel frattempo la schermata resta viva.
     * ⚠️ **La cura, se dovesse servire, è una cache dei fotogrammi già composti**, non un
     * `seek` dentro i lettori, che il formato non permette. Non c'è ancora perché costa
     * memoria (larghezza per altezza per quattro byte a fotogramma) e potrebbe non servire.
     */
    suspend fun stepBack() = guarded {
        playing = false
        turn.withLock {
            val target = if (source.index <= 0) source.frameCount - 1 else source.index - 1
            withContext(Dispatchers.Default) {
                source.rewind()
                repeat(target) { source.advance() }
            }
            draw()
        }
    }

    /**
     * Una copia del fotogramma corrente, da salvare su file.
     *
     * ⚠️⚠️ **UNA COPIA E NON L'ORIGINALE, ed è obbligatorio**: il Bitmap che i lettori
     * restituiscono è **loro** e viene riscritto al fotogramma dopo. Salvando quello si
     * scriverebbe su file un'immagine che intanto cambia sotto le mani.
     */
    suspend fun snapshot(): android.graphics.Bitmap? = turn.withLock {
        withContext(Dispatchers.Default) {
            runCatching {
                source.current()?.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
            }.getOrNull()
        }
    }

    /**
     * Il ciclo di riproduzione: aspetta il ritardo del fotogramma, avanza, ridisegna.
     *
     * ⚠️ **La pausa NON ferma questo ciclo, lo lascia semplicemente non chiamato**: chi lo
     * lancia lo rilancia al cambio di [playing], e riprendendo si riparte dal fotogramma
     * dove si era, perché l'indice vive nel lettore e non qui.
     * ⚠️ **[FLOOR] esiste perché un ritardo di zero è legale**: nelle GIF vuol dire 'il più
     * in fretta possibile', e preso alla lettera sarebbe un ciclo stretto che scalda il
     * telefono. I browser fanno la stessa cosa da vent'anni.
     */
    private suspend fun run() {
        // Il primo fotogramma si disegna subito, anche in pausa: senza, una GIF aperta e
        // messa in pausa prima del primo scatto resterebbe un rettangolo vuoto.
        turn.withLock { draw() }
        while (playing) {
            delay(source.delayOf(source.index).coerceAtLeast(FLOOR).toLong())
            turn.withLock {
                withContext(Dispatchers.Default) { source.advance() }
                draw()
            }
        }
    }

    /**
     * Il ciclo di riproduzione, con la sicura.
     *
     * ⚠️⚠️ **UN ERRORE QUI DENTRO CHIUDE L'APP, e non è teorico: è successo nella `1.13`**
     * (segnalazione dell'utente: *manda in crash 10 volte su 10*). Questo ciclo gira dentro
     * una coroutine di composizione, e quello che vi si solleva non lo prende nessuno.
     * ⚠️ **Fermare l'animazione è la risposta giusta**: sotto c'è sempre l'immagine ferma
     * disegnata dal visualizzatore, quindi il peggio che si vede è una GIF che non si muove,
     * e non un'app che se ne va. ⚠️ La causa vera della `1.13` è corretta a monte, in
     * [rememberAnimation]: questa resta perché una funzione nuova non deve poter chiudere
     * l'app, qualunque cosa le succeda dentro.
     */
    suspend fun play() = guarded { run() }

    /**
     * Esegue [corpo] senza che un suo errore possa arrivare fino all'applicazione.
     */
    private suspend fun guarded(corpo: suspend () -> Unit) {
        try {
            corpo()
        } catch (cancelled: CancellationException) {
            // ⚠️ La cancellazione NON è un errore e va lasciata passare: è il modo in cui
            // Compose spegne questo ciclo quando si cambia fotografia o si mette in pausa.
            // Inghiottirla vorrebbe dire una coroutine che non si ferma quando le si chiede.
            throw cancelled
        } catch (failure: Throwable) {
            playing = false
        }
    }

    private suspend fun draw() {
        val bitmap = withContext(Dispatchers.Default) { source.current() } ?: return
        frame = bitmap.asImageBitmap()
        shown = source.index + 1
    }

    fun close() {
        source.close()
    }

    private companion object {
        /** Sotto questo ritardo si va comunque a questo passo. In millisecondi. */
        const val FLOOR = 20
    }
}

/**
 * Apre l'immagine animata che sta a [source], e la chiude quando si cambia fotografia.
 *
 * ⚠️⚠️ **IL `DisposableEffect` È QUELLO CHE IMPEDISCE LA PERDITA DI MEMORIA**: ogni
 * animazione aperta tiene il file intero in memoria più una tela, e sfogliando una cartella
 * di GIF senza chiuderle si arriverebbe a decine di megabyte in una manciata di scorrimenti.
 * ⚠️ **La chiave è l'indirizzo**: cambiando fotografia il vecchio si chiude e il nuovo si
 * apre, che è esattamente quello che deve succedere.
 * ⚠️ **Torna `null` per tutto quello che animato non è**, ed è il caso normale: una fotografia
 * ferma non paga niente, perché [Animations.open] guarda i primi byte e se ne va.
 *
 * ⚠️⚠️ **L'ANIMAZIONE NON PUÒ STARE FRA LE CHIAVI DEL `DisposableEffect`, e metterla ci ha
 * fatto uscire una `1.13` che chiudeva l'app su ogni GIF** (segnalazione dell'utente,
 * 2026-09-01: *manda in crash 10 volte su 10*). `onDispose` non riceve il valore vecchio: legge
 * la variabile **nel momento in cui gira**. Con `animation` fra le chiavi, l'istante in cui
 * l'apertura la porta da `null` a un lettore fa scadere l'effetto, e il suo `onDispose`
 * chiude il lettore **appena creato**. Chiuderlo e continuare a usarlo è il difetto.
 * ⚠️⚠️ **E si è visto solo sulle GIF per un'asimmetria fra i due lettori**, che è la parte
 * che rendeva il sintomo incomprensibile: [AnimatedWebp.close] butta la tela e la rifà alla
 * prima richiesta, mentre [AnimatedGif.close] passa da `GifDecoder.clear()`, che azzera
 * l'intestazione, e da lì `frameCount`, `delayOf` e `advance` sollevano un errore di
 * riferimento nullo. La stessa identica chiusura sbagliata: una la sopporta, l'altra no.
 * ⚠️ **La chiave giusta è il solo indirizzo**: l'effetto scade quando si cambia fotografia o
 * si esce, e in quei due momenti la variabile porta davvero il lettore da chiudere.
 */
@Composable
fun rememberAnimation(source: Uri?): Animation? {
    val context = LocalContext.current
    var animation by remember(source) { mutableStateOf<Animation?>(null) }

    LaunchedEffect(source) {
        animation = source
            ?.let { withContext(Dispatchers.IO) { Animations.open(context, it) } }
            ?.let { Animation(it) }
    }
    DisposableEffect(source) {
        onDispose { animation?.close() }
    }
    // ⚠️ Rilanciato anche al cambio di `playing`: è cosi che la ripresa riparte, e la pausa
    // lascia esaurire il ciclo invece di tenerlo in giro a controllare una bandierina.
    val current = animation
    LaunchedEffect(current, current?.playing) {
        current?.play()
    }
    return animation
}

/**
 * La fila dei comandi dell'animazione.
 *
 * ⚠️⚠️ **STA SOPRA L'IMMAGINE E NON SOTTO LA BARRA DEI DETTAGLI**: i comandi si toccano
 * mentre si guarda, e mandarli in fondo allo schermo insieme alle informazioni vorrebbe dire
 * il pollice che copre proprio la cosa che si sta osservando cambiare.
 * ⚠️ **Il contatore sta nella fila e non altrove**: è l'unica cosa che dice se un comando ha
 * fatto effetto, e separato dai tasti si guarderebbe in due posti.
 * ⚠️ **E si può spegnere dalle impostazioni** ([Settings.animCounter], acceso di fabbrica):
 * è la sola parte della fila che non è un comando, quindi la sola che qualcuno possa voler
 * togliere per guardare e basta. Spento, la fila si stringe da sé: il numero è l'ultimo
 * elemento della riga e non lascia un vuoto.
 *
 * @param onExported il fotogramma è stato scritto su file. ⚠️ **Serve perché la cartella
 *   aperta è un elenco già letto**: il file nuovo c'è sul disco e nel MediaStore, ma la
 *   griglia dietro al visualizzatore tiene in mano la lista di prima, quindi tornando
 *   indietro il fotogramma appena salvato non si vedrebbe finché non si esce dalla cartella
 *   e ci si rientra. Riscontro dell'utente, 2026-09-01.
 */
@Composable
fun AnimatedBar(
    animation: Animation,
    name: String?,
    counter: Boolean,
    onExported: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    /*
     * ⚠️⚠️ **UN SELETTORE TUTTO SUO, e NON il riuso di quello di 'Scarica'**: quello scrive
     * il **file originale** byte per byte e dichiara il tipo della sorgente; qui si scrive un
     * fotogramma composto in memoria, e il tipo è PNG qualunque cosa fosse il file di
     * partenza. Due contenuti diversi vogliono due contratti diversi.
     * ⚠️ **PNG e non JPEG**: un fotogramma di GIF o WebP può avere trasparenza, e il JPEG non
     * la sa tenere. Salvarlo in JPEG riempirebbe di nero le parti trasparenti, in silenzio.
     * ⚠️ **Il fotogramma si prende PRIMA di aprire il selettore**, e si tiene qui: fra il
     * tocco e la scelta della cartella passano secondi, e in quel tempo l'animazione
     * potrebbe essere ripartita.
     */
    var pending by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val exporter = rememberLauncherForActivityResult(
        remember { ActivityResultContracts.CreateDocument("image/png") }
    ) { target ->
        val shot = pending
        pending = null
        if (target == null || shot == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(target)?.use { out ->
                        shot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                    } ?: false
                }.getOrDefault(false)
            }
            Toast.makeText(
                context,
                if (ok) R.string.toast_saved else R.string.toast_save_failed,
                Toast.LENGTH_SHORT
            ).show()
            if (ok) onExported()
        }
    }

    Row(
        modifier = modifier
            .background(BAR_INK, RoundedCornerShape(BAR_CORNER))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Key(Icons.Outlined.NavigateBefore, R.string.anim_prev) {
            scope.launch { animation.stepBack() }
        }
        Key(
            icon = if (animation.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            label = if (animation.playing) R.string.anim_pause else R.string.anim_play
        ) {
            animation.toggle()
        }
        Key(Icons.Outlined.NavigateNext, R.string.anim_next) {
            scope.launch { animation.stepForward() }
        }
        Key(Glyphs.PhotoOut, R.string.anim_export) {
            scope.launch {
                val shot = animation.snapshot() ?: return@launch
                pending = shot
                exporter.launch(frameName(name, animation.shown))
            }
        }
        if (counter) {
            Text(
                text = counterText(animation.shown, animation.frameCount),
                // ⚠️ Vedi [counterText]: le cifre tabulari sono metà della cura, e senza
                // di loro il riempimento non basterebbe.
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = BAR_TEXT,
                modifier = Modifier.padding(end = 10.dp, start = 2.dp)
            )
        }
    }
}

/** Un tasto della fila: stessa misura, stesso colore, una riga per chiamarlo. */
@Composable
private fun Key(icon: ImageVector, @StringRes label: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(BAR_KEY)) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(label),
            tint = Color.White
        )
    }
}

/**
 * Il nome proposto per il fotogramma esportato.
 *
 * ⚠️ **Porta il numero del fotogramma**, o esportandone tre dalla stessa animazione si
 * otterrebbero tre file che si chiamano uguale e il selettore aggiungerebbe '(1)', '(2)'.
 * Con il numero, i file si riordinano da soli e si capisce da dove vengono.
 */
private fun frameName(name: String?, frame: Int): String {
    val base = name?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "fotogramma"
    return "%s-%04d.png".format(base, frame)
}

/**
 * Il contatore dei fotogrammi, di larghezza **costante**: `  7 / 120` e non `7 / 120`.
 *
 * ⚠️⚠️ **DUE CAUSE DIVERSE FANNO BALLARE LA FILA, e una sola cura non basta**, perché il
 * numero è l'ultimo elemento di una riga che si stringe sul contenuto: se il testo cambia
 * larghezza, si spostano anche i quattro tasti.
 * 1. **Le cifre non sono larghe uguali**: in un carattere proporzionale l'`1` è più stretto
 *    dell'`8`, quindi la riga si muove **a ogni fotogramma**, non solo ai cambi di
 *    lunghezza. La cura è `tnum`, la variante tabulare, che dà a tutte le cifre lo stesso
 *    passo. È l'unico modo: nessun riempimento può pareggiare glifi di larghezza diversa.
 * 2. **Cambia il numero delle cifre**, da `9` a `10` e da `99` a `100`. La cura è riempire a
 *    sinistra fino alle cifre del totale, che è il numero più lungo che si possa mostrare.
 *
 * ⚠️ **Il riempimento è lo SPAZIO CIFRA (U+2007) e non lo spazio normale**: quello è largo
 * quanto una cifra **per definizione**, mentre lo spazio ordinario è più stretto e lascerebbe
 * un residuo di ballo proprio al cambio di lunghezza, cioè nel caso che doveva risolvere.
 */
private fun counterText(shown: Int, total: Int): String =
    "${shown.toString().padStart(total.toString().length, FIGURE_SPACE)} / $total"

/**
 * Lo spazio largo come una cifra, usato per riempire il contatore.
 *
 * ⚠️ **Si scrive col CODICE e non col carattere**: nel sorgente sarebbe indistinguibile da
 * uno spazio normale, e il primo che riallinea la riga lo sostituirebbe senza accorgersene.
 */
private const val FIGURE_SPACE = '\u2007'

/** Il fondo scuro della fila: legge sopra qualunque immagine, chiara o scura. */
private val BAR_INK = Color(0xB8121316)
private val BAR_TEXT = Color(0xFFC9C8C4)
private val BAR_CORNER = 28.dp
private val BAR_KEY = 46.dp

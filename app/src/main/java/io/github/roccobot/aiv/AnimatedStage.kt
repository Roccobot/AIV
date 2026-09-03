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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
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
     * Ferma la riproduzione, e se era già ferma non fa niente.
     *
     * ⚠️ **Non è [toggle], ed è la differenza che conta**: la chiama il tocco lungo che apre
     * il menu (richiesta dell'utente, 2026-09-01), e chi apre il menu su un'animazione già in
     * pausa non se la deve vedere ripartire in faccia.
     */
    fun pause() {
        playing = false
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
            // ⚠️⚠️ **`seek` E NON `rewind` PIÙ N VOLTE `advance`, dalla 1.21**: quella era la
            // scia. Spostare l'indice non compone niente, quindi il fotogramma d'arrivo
            // finiva sopra tutti quelli già in scena. Il perché per esteso sta su
            // [Animated.seek], che esiste per questo.
            withContext(Dispatchers.Default) { source.seek(target) }
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
     * una coroutine di composizione, e l'errore che ci nasce dentro non lo prende nessuno.
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
 * l'intestazione, e da lì `frameCount`, `delayOf` e `advance` danno un errore di
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
 * ⚠️⚠️ **IL TASTO DELL'ESPORTAZIONE SE N'È ANDATO NELLA 1.21, ed è una richiesta**
 * (utente, 2026-09-01: *si può togliere del tutto quel tasto e usare direttamente la
 * funzionalità 'Esporta' del menu a pressione lunga, che agisce sul fotogramma corrente*).
 * Il motivo che l'ha fatto nascere era evitare i tocchi accidentali: premendo 'avanti' più
 * volte di fila il dito finiva sul tasto accanto, che apriva un selettore di file. Adesso
 * quella funzione sta nel menu del tocco lungo, cioè dietro un gesto che non si fa per
 * sbaglio, e la fila torna a essere fatta di soli comandi di riproduzione.
 * ⚠️ **Con lui se ne sono andati il selettore, il fotogramma tenuto da parte e `onExported`**:
 * quel giro adesso lo fa `ConvertDialog`, che ha già il suo `onSaved`.
 */
@Composable
fun AnimatedBar(
    animation: Animation,
    counter: Boolean,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

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
        if (counter) {
            val stile = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
            /*
             * ⚠️⚠️ **LARGHEZZA FISSA MISURATA SUL PIÙ LUNGO CHE POSSA CAPITARE, e il
             * riempimento a spazi è uscito** (istruzione dell'utente, 2026-09-01: *allarga e
             * prenditi lo spazio che basterebbe per 1000/1000, e fa' in modo che stia
             * allineato a destra*). Il riempimento con lo spazio-cifra copriva il cambio di
             * lunghezza ma non tutto il resto, perché quel carattere è largo come una cifra
             * **per definizione tipografica**, non in ogni carattere che esiste: restava un
             * ballo su un fotogramma su dieci, ed è esattamente quello che l'utente ha
             * misurato provando.
             * ⚠️ **Si misura, non si scrive in dp**: un numero fisso andrebbe storto appena
             * qualcuno ingrandisce il testo di sistema, e [rememberTextMeasurer] dà la
             * larghezza vera del carattere in uso adesso.
             * ⚠️ **Le cifre tabulari restano**, e non sono un doppione: dentro una larghezza
             * fissa tengono ferme anche le cifre **in mezzo** al numero.
             */
            val metro = rememberTextMeasurer()
            val larghezza = with(LocalDensity.current) {
                metro.measure(COUNTER_WIDEST, stile).size.width.toDp()
            }
            Text(
                text = "${animation.shown} / ${animation.frameCount}",
                style = stile,
                color = BAR_TEXT,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .padding(end = 10.dp, start = 2.dp)
                    .widthIn(min = larghezza)
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
 * Il numero più lungo che il contatore possa dover mostrare.
 *
 * ⚠️ **Mille e non il totale vero**: la larghezza si prende una volta e non deve cambiare
 * fra un'animazione e l'altra, e un'animazione da più di mille fotogrammi in un
 * visualizzatore di fotografie non esiste. Se un giorno esistesse, il contatore si
 * allargherebbe da sé, perché il minimo è un minimo e non un taglio.
 */
private const val COUNTER_WIDEST = "1000 / 1000"


/** Il fondo scuro della fila: legge sopra qualunque immagine, chiara o scura. */
private val BAR_INK = Color(0xB8121316)
private val BAR_TEXT = Color(0xFFC9C8C4)
private val BAR_CORNER = 28.dp
private val BAR_KEY = 46.dp

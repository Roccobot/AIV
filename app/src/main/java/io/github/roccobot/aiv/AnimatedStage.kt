package io.github.roccobot.aiv

import android.net.Uri
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
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
 * La decodifica non è sul filo principale: comporre un fotogramma vuol dire decomprimere e
 * disegnare una tela grande quanto l'immagine, e farlo dove Compose disegna farebbe saltare
 * fotogrammi alla schermata. Il ciclo gira su [Dispatchers.Default] e allo stato arriva solo il
 * risultato.
 *
 * ⚠️⚠️ **Ogni giro crea un involucro nuovo**: i due lettori riusano due `Bitmap` a turno, quindi
 * un fotogramma può arrivare sullo stesso oggetto di due giri fa, coi pixel cambiati. Con lo
 * stesso [ImageBitmap] Compose non vedrebbe nessun cambiamento e l'animazione resterebbe ferma
 * pur girando. Per questo `asImageBitmap()`, che crea un involucro nuovo, si chiama a ogni
 * fotogramma.
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
     * ⚠️⚠️ **Senza il lucchetto il ciclo di riproduzione e i due passi compongono insieme**
     * sulla stessa tela (una scia sulle GIF, rettangoli bianchi e neri sulle WebP). I passi
     * mettono in pausa, ma la pausa non è istantanea: il ciclo può essere già dentro `advance()`
     * o dentro la composizione, e la cancellazione non lo ferma a metà. Un ritardo renderebbe il
     * difetto raro, il lucchetto lo rende impossibile.
     * Copre anche [snapshot], che altrimenti potrebbe copiare un fotogramma composto a metà.
     */
    private val turn = Mutex()

    val frameCount: Int get() = source.frameCount

    fun toggle() {
        playing = !playing
    }

    /**
     * Ferma la riproduzione, e se era già ferma non fa niente.
     *
     * Non è [toggle]: la chiama il tocco lungo che apre il menu, e un'animazione già in pausa
     * non deve ripartire.
     */
    fun pause() {
        playing = false
    }

    /**
     * Un fotogramma avanti, e mette in pausa se stava andando.
     *
     * La pausa fa parte del comando: un fotogramma chiesto ad animazione in corsa si vedrebbe per
     * un decimo di secondo.
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
     * ⚠️ **Costa N passi e non uno, per il formato**: in GIF come in WebP un fotogramma è una
     * toppa disegnata sopra il precedente, quindi per tornare indietro di uno si rifà la pila da
     * capo. Gira fuori dal filo principale: su duecento fotogrammi è qualche decimo di secondo.
     * Se servisse di più, la strada è una cache dei fotogrammi composti, che costa memoria.
     */
    suspend fun stepBack() = guarded {
        playing = false
        turn.withLock {
            val target = if (source.index <= 0) source.frameCount - 1 else source.index - 1
            // ⚠️ `seek` e non lo spostamento dell'indice: spostare l'indice non compone niente,
            // e il fotogramma d'arrivo finirebbe sopra quelli già in scena ([Animated.seek]).
            withContext(Dispatchers.Default) { source.seek(target) }
            draw()
        }
    }

    /**
     * Una copia del fotogramma corrente, da salvare su file.
     *
     * ⚠️ **Una copia**: il Bitmap dei lettori è riscritto al fotogramma dopo, e salvarlo
     * scriverebbe un'immagine che intanto cambia.
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
     * Con la pausa il ciclo termina, e chi lo lancia lo rilancia al cambio di [playing]: si
     * riparte dal fotogramma dove si era, perché l'indice è nel lettore.
     * [FLOOR] esiste perché nelle GIF un ritardo di zero è legale e vuol dire 'il più in fretta
     * possibile': preso alla lettera sarebbe un ciclo stretto che scalda il telefono. I browser
     * fanno lo stesso.
     */
    private suspend fun run() {
        // Il primo fotogramma si disegna subito, anche in pausa: senza, una GIF messa in pausa
        // prima del primo scatto resterebbe un rettangolo vuoto.
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
     * ⚠️ **Un errore qui dentro chiuderebbe l'app**: il ciclo gira in una coroutine di
     * composizione, dove nessuno prende l'errore. Fermare l'animazione basta: sotto c'è sempre
     * l'immagine ferma disegnata dal visualizzatore.
     */
    suspend fun play() = guarded { run() }

    /** Esegue [corpo] senza che un suo errore possa arrivare fino all'applicazione. */
    private suspend fun guarded(corpo: suspend () -> Unit) {
        try {
            corpo()
        } catch (cancelled: CancellationException) {
            // ⚠️ La cancellazione non è un errore e va lasciata passare: è il modo in cui
            // Compose ferma il ciclo quando si cambia immagine o si mette in pausa.
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
 * Apre l'immagine animata all'indirizzo [source], e la chiude quando si cambia immagine.
 *
 * Il `DisposableEffect` evita la perdita di memoria: ogni animazione aperta tiene il file intero
 * più una tela. Risponde `null` per tutto quello che animato non è: [Animations.open] guarda i
 * primi byte e rinuncia.
 *
 * ⚠️⚠️ **La chiave del `DisposableEffect` è il solo indirizzo, mai l'animazione**: `onDispose`
 * legge la variabile nel momento in cui gira, e con `animation` fra le chiavi il passaggio da
 * `null` a un lettore farebbe chiudere il lettore appena creato. Sulle GIF l'app andrebbe in
 * crash ([AnimatedGif.close] azzera l'intestazione), sulle WebP no ([AnimatedWebp.close] rifà la
 * tela alla prima richiesta).
 *
 * ⚠️ **[loaded] serve alle immagini di rete**: i loro byte si leggono dalla cache
 * (`Animations.bytesOf`), e l'apertura parte insieme allo scaricamento. Il primo tentativo trova
 * la cache vuota; quando l'immagine è pronta l'effetto riparte una volta. È un booleano e non lo
 * stato del visualizzatore, che cambia a ogni pizzicata di zoom e farebbe rileggere il file.
 * ⚠️ **Il secondo giro non sostituisce un lettore aperto** (`if (animation != null) return`):
 * il vecchio resterebbe aperto e il disegno potrebbe pescare fra i due.
 */
@Composable
fun rememberAnimation(source: Uri?, loaded: Boolean = true): Animation? {
    val context = LocalContext.current
    var animation by remember(source) { mutableStateOf<Animation?>(null) }

    LaunchedEffect(source, loaded) {
        if (animation != null) return@LaunchedEffect
        animation = source
            ?.let { withContext(Dispatchers.IO) { Animations.open(context, it) } }
            ?.let { Animation(it) }
    }
    DisposableEffect(source) {
        onDispose { animation?.close() }
    }
    // Rilanciato anche al cambio di `playing`: così la ripresa riparte, e con la pausa il ciclo
    // termina.
    val current = animation
    /*
     * ⚠️ **Il ciclo si ferma quando l'app va in sottofondo** (`repeatOnLifecycle`): da sé non si
     * fermerebbe, perché aspetta con `delay` e non col clock dei fotogrammi, e il dispatcher
     * della composizione consegna le riprese anche a finestra ferma (sull'`Handler` oltre che
     * sul `Choreographer`). L'attività resta aperta in sottofondo, e a schermo spento la GIF
     * continuerebbe a decodificare per nessuno. Alla ripresa il ciclo riparte da capo.
     */
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(current, current?.playing, owner) {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            current?.play()
        }
    }
    return animation
}

/**
 * La fila dei comandi dell'animazione.
 *
 * È sopra l'immagine e non sotto la barra dei dettagli: i comandi si toccano mentre si guarda,
 * e in fondo allo schermo il pollice coprirebbe quello che cambia.
 * Il contatore è nella fila perché dice se un comando ha fatto effetto. Si spegne dalle
 * impostazioni ([Settings.animCounter], acceso di fabbrica); spento, la fila si stringe da sé.
 * L'esportazione del fotogramma è nel menu del tocco lungo (`ConvertDialog`), lontano dai
 * tasti che si premono di fila.
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
             * ⚠️ **Larghezza minima misurata sul numero più lungo** ([COUNTER_WIDEST]),
             * allineata a destra (istruzione dell'utente), così il contatore non balla.
             * Si misura col carattere in uso e non si scrive in dp, che andrebbero storti col
             * testo di sistema ingrandito. Le cifre tabulari tengono ferme anche le cifre in
             * mezzo al numero.
             * La misura è ricordata: la lambda della `Row` è `inline` e si ricompone a ogni
             * fotogramma, mentre il numero dipende solo dal carattere e dalla densità.
             */
            val metro = rememberTextMeasurer()
            val density = LocalDensity.current
            val larghezza = remember(metro, stile, density) {
                with(density) { metro.measure(COUNTER_WIDEST, stile).size.width.toDp() }
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
 * Mille e non il totale vero: la larghezza non deve cambiare fra un'animazione e l'altra. Oltre
 * mille fotogrammi il contatore si allarga da sé, perché è un minimo e non un taglio.
 */
private const val COUNTER_WIDEST = "1000 / 1000"


/** Il fondo scuro della fila: legge sopra qualunque immagine, chiara o scura. */
private val BAR_INK = Color(0xB8121316)
private val BAR_TEXT = Color(0xFFC9C8C4)
private val BAR_CORNER = 28.dp
private val BAR_KEY = 46.dp

package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    val frameCount: Int get() = source.frameCount

    fun toggle() {
        playing = !playing
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
    suspend fun run() {
        // Il primo fotogramma si disegna subito, anche in pausa: senza, una GIF aperta e
        // messa in pausa prima del primo scatto resterebbe un rettangolo vuoto.
        draw()
        while (playing) {
            delay(source.delayOf(source.index).coerceAtLeast(FLOOR).toLong())
            withContext(Dispatchers.Default) { source.advance() }
            draw()
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
    DisposableEffect(source, animation) {
        onDispose { animation?.close() }
    }
    // ⚠️ Rilanciato anche al cambio di `playing`: è cosi che la ripresa riparte, e la pausa
    // lascia morire il ciclo invece di tenerlo in giro a controllare una bandierina.
    val current = animation
    LaunchedEffect(current, current?.playing) {
        current?.run()
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
 */
@Composable
fun AnimatedBar(animation: Animation, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(BAR_INK, RoundedCornerShape(BAR_CORNER))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(onClick = { animation.toggle() }, modifier = Modifier.size(BAR_KEY)) {
            Icon(
                imageVector = if (animation.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (animation.playing) R.string.anim_pause else R.string.anim_play
                ),
                tint = Color.White
            )
        }
        Text(
            text = "${animation.shown} / ${animation.frameCount}",
            style = MaterialTheme.typography.labelMedium,
            color = BAR_TEXT,
            modifier = Modifier.padding(end = 10.dp, start = 2.dp)
        )
    }
}

/** Il fondo scuro della fila: legge sopra qualunque immagine, chiara o scura. */
private val BAR_INK = Color(0xB8121316)
private val BAR_TEXT = Color(0xFFC9C8C4)
private val BAR_CORNER = 28.dp
private val BAR_KEY = 46.dp

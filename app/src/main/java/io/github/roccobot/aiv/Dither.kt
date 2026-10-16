package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.RuntimeShader
import android.graphics.Shader.TileMode
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativePaint
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.roundToInt

/*
 * Questo file toglie le bande dalle sfumature, e il difetto è aritmetico.
 *
 * **Il conto.** Lo schermo ha 256 livelli per canale. Una sfumatura da un quarto di opacità a
 * zero, con una tinta che dista un centinaio di livelli dal fondo, attraversa venticinque gradini
 * su mezzo migliaio di pixel: un gradino ogni venti pixel, che si vede. Non si toglie con più
 * tappe (fra due tappe l'interpolazione è già continua: a quantizzare è la destinazione), né con
 * più bit dentro l'app, perché il taglio a otto bit avviene alla fine, sullo schermo.
 * Una sfumatura sopra un'immagine attraversa i livelli fra il fondo e l'immagine sotto,
 * moltiplicati per la copertura: una zona scura che dista sessanta livelli dal fondo, sotto la
 * fascia grande in fondo allo schermo (al 60%, circa 420 pixel), ne attraversa trentasei.
 *
 * ⚠️ **L'unico rimedio è il rumore (dithering)**: si somma al colore un rumore piccolo prima
 * dell'arrotondamento, così i pixel a cavallo di un gradino cadono un po' di qua e un po' di là, e
 * il bordo netto diventa una transizione sfumata. Il rumore è esplicito, per pixel, di un livello
 * pieno e a distribuzione triangolare: con questa quantità l'errore di arrotondamento smette di
 * dipendere dal colore. Il dither di Skia (`isDither` del paint) resta solo dove manca il nostro,
 * e non si somma mai al nostro: due dither insieme danno solo più grana.
 *
 * Le strade sono due ([GrainedRamp]): da Android 13 il programma [DITHER_AGSL] su ogni pixel;
 * sotto, la rampa precalcolata col rumore dentro ([rampMask]), su risposta `copri` dell'utente
 * alla domanda `d-dither-vecchi`. Nessuno dei due ha un telefono sotto Android 13: quel ramo lo
 * presidia solo il banco, che controlla soprattutto che il colore arrivi giusto (una maschera che
 * Skia non modulasse darebbe una fascia nera).
 *
 * Nel programma il rumore va sui soli canali del colore, non sull'opacità: il colore a schermo vale
 * `sorgente + fondo * (1 - opacità)`, e un rumore sull'opacità si cancellerebbe da sé sui fondi
 * chiari. Resta dentro l'opacità (`clamp` fra zero e `a`), perché un colore premoltiplicato più
 * luminoso della propria opacità non è un colore valido.
 *
 * Il rumore è ancorato allo schermo, non alla sfumatura: scorrendo, la tinta si muove e la grana
 * resta ferma. Una grana che scorresse con la tinta si noterebbe.
 *
 * Usano [GrainedRamp] tutte le rampe di una tinta sola: l'intestazione, le due fasce in fondo
 * allo schermo (`GroundFade`) e l'ombra della selezione (`PickShade.kt`). Restano fuori le
 * sfumature piccole (`FadedRule`, l'alone di `FolderMark`, la dissolvenza della fila nel velo
 * d'aiuto): sono alte o larghe pochi punti, e là un gradino è di un pixel.
 */

/**
 * Il programma che gira **su ogni pixel**: legge la sfumatura e le somma il rumore.
 *
 * ⚠️ **La sfumatura arriva come ingresso e non si riscrive qui**: le tappe restano una fonte sola,
 * in Kotlin, e questo programma non sa che forma abbiano.
 * ⚠️ **Il rumore è la somma di due sorgenti indipendenti**, che dà una distribuzione triangolare:
 * con una sola (distribuzione piatta) le bande si attenuano invece di sparire.
 * ⚠️ **La seconda sorgente ha coefficienti propri (i due scambiati)**: la stessa funzione valutata
 * in un punto spostato di una costante non è indipendente, perché dipende da `p` solo attraverso
 * un prodotto scalare, e la somma tornerebbe piatta.
 * La sorgente è un rumore a gradiente interlacciato: un seno moltiplicato per un numero grande, a
 * mezza precisione, degenera a strisce.
 */
private const val DITHER_AGSL = """
uniform shader ramp;

half grain(float2 p, float2 k) {
    return half(fract(52.9829189 * fract(dot(p, k))));
}

half4 main(float2 p) {
    half4 c = ramp.eval(p);
    half due = grain(p, float2(0.06711056, 0.00583715)) + grain(p, float2(0.00583715, 0.06711056));
    half noise = (due - 1.0) * half(1.0 / 255.0);
    return half4(clamp(c.rgb + noise, half3(0.0), half3(c.a)), c.a);
}
"""

/**
 * Una rampa di una tinta sola, posata col rumore che le toglie le bande: lo strumento di tutte le
 * sfumature dell'app da una tinta al trasparente.
 *
 * Le strade le sceglie la versione di Android: da Android 13 il programma [DITHER_AGSL] legge la
 * rampa come ingresso; sotto, la rampa arriva precalcolata da [rampMask] e il colore lo mette il
 * paint. Se la memoria non basta per la maschera resta il dither del paint.
 * ⚠️ **Il programma si compila una volta per rampa, e l'ingresso si rifà solo quando cambiano la
 * tinta, i due capi o la larghezza**: l'ombra della selezione si ridisegna a ogni fotogramma dello
 * scorrimento, e compilare a ogni fotogramma costerebbe più del disegno (presidiato da
 * `RampeTest`).
 * La rampa è verticale: una orizzontale chiederebbe un'altra maschera, che si ripete lungo la riga.
 *
 * @param stops le tappe: posizione da 0 a 1 fra i due capi, e quanto di [peak] copre la tinta.
 * @param peak quanto copre la tinta nel suo punto più forte.
 */
internal class GrainedRamp(private val stops: List<Pair<Float, Float>>, private val peak: Float) {
    private val pittura = Paint()
    private val grana: android.graphics.Shader? = grainShader().also { if (it != null) DrawWork.programs++ }
    private var maschera: android.graphics.Shader? = null
    private var altaMaschera = -1
    private var fatta: Rampa? = null

    /** Quello che decide la rampa disegnata: se non cambia, l'ingresso del programma resta quello. */
    private data class Rampa(val ink: Color, val from: Float, val to: Float, val width: Float)

    /**
     * Dipinge la rampa nel rettangolo da [left] a [right] e da [top] a [bottom], con la tinta [ink]
     * che parte dal pieno di [peak] a [from] (cioè dalla prima tappa) e arriva all'ultima tappa a
     * [to]; fuori dai due capi la rampa resta ferma sul suo valore. [alpha] moltiplica tutto.
     */
    fun paint(
        scope: DrawScope,
        ink: Color,
        from: Float,
        to: Float,
        alpha: Float = 1f,
        left: Float = 0f,
        top: Float = 0f,
        right: Float = scope.size.width,
        bottom: Float = scope.size.height
    ) {
        if (alpha <= 0f || right <= left || bottom <= top || to <= from) return
        val rampa = Rampa(ink, from, to, right - left)
        if (rampa != fatta) {
            fatta = rampa
            DrawWork.gradients++
            val pennello = Brush.verticalGradient(
                colorStops = stops.map { (at, quanto) -> at to ink.copy(alpha = ink.alpha * quanto * peak) }
                    .toTypedArray(),
                startY = from,
                endY = to
            )
            val dentro = (pennello as ShaderBrush).createShader(Size(right - left, bottom - top))
            val cornice = pittura.nativePaint
            if (grana != null) {
                feed(grana, dentro)
                cornice.isDither = false
                cornice.shader = grana
            } else {
                val alta = ceil(to - from).toInt()
                if (alta != altaMaschera) {
                    altaMaschera = alta
                    maschera = rampMask(stops, peak, alta, 0f)
                }
                val m = maschera
                if (m != null) {
                    m.setLocalMatrix(Matrix().apply { setTranslate(0f, from) })
                    pittura.color = ink
                    cornice.isDither = false
                    cornice.shader = m
                } else {
                    cornice.isDither = true
                    cornice.shader = dentro
                }
            }
        }
        // ⚠️ L'opacità si scrive dopo il colore, che in Compose riscrive anche il suo byte.
        pittura.alpha = alpha * (if (grana == null && maschera != null) ink.alpha else 1f)
        scope.drawIntoCanvas { it.drawRect(left, top, right, bottom, pittura) }
    }
}

/**
 * Il conto del lavoro di disegno che uno scorrimento può rifare: i programmi del rumore compilati,
 * i gradienti di rampa rifatti, gli effetti del vetro creati (`PhonePill.kt`), le maglie deformate
 * costruite dall'editor completo (`Geometry.kt`) e le tele nuove dei lettori delle animazioni
 * (`AnimatedGif.kt`, `AnimatedWebp.kt`).
 *
 * È la misura della scelta B1 dell'utente: le prestazioni si misurano contando le ricostruzioni
 * per fotogramma sul banco. La leggono `RampeTest`, `VetroTest`, `MagliaTest`, `TeleGifTest` e
 * `TeleWebpTest`.
 */
internal object DrawWork {
    var programs = 0
    var gradients = 0
    var glass = 0
    var meshes = 0
    var canvases = 0
}

/** Il programma del rumore, oppure `null` sotto Android 13, dove `RuntimeShader` non c'è. */
private fun grainShader(): android.graphics.Shader? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) newGrain() else null

/**
 * Il programma compilato.
 *
 * ⚠️ **Vive in una funzione sua, come [feed]**: il controllo di versione e l'uso di una classe di
 * Android 13 devono essere in due funzioni diverse, o l'analizzatore statico non riconosce la
 * guardia.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun newGrain(): android.graphics.Shader = RuntimeShader(DITHER_AGSL)

/** Aggancia la rampa [ramp] al programma [grain] come suo ingresso. */
private fun feed(grain: android.graphics.Shader, ramp: android.graphics.Shader) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) attach(grain, ramp)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun attach(grain: android.graphics.Shader, ramp: android.graphics.Shader) {
    (grain as RuntimeShader).setInputShader("ramp", ramp)
}

/**
 * La stessa rampa col rumore già dentro, disegnata una volta come maschera: la strada dei telefoni
 * che il programma di [GrainedRamp] non può servire, cioè Android 12 e precedenti.
 *
 * ⚠️ **È una maschera e non un'immagine a colori**: la rampa è una tinta sola con l'opacità che
 * scende, quindi basta un byte per pixel invece di quattro, e il colore lo mette il paint.
 * ⚠️ **Quindi il rumore va sull'opacità**, al contrario del programma, che ha il colore in mano. La
 * dose si regola da sé: il colore a schermo vale `tinta * a + fondo * (1 - a)`, quindi un livello
 * di rumore sull'opacità ne muove `|tinta - fondo| / 255` sul risultato, forte dove i gradini sono
 * grossi e piano dove le bande quasi non ci sono. Con [GRAIN_STEPS] livelli e un distacco tipico
 * di un centinaio, sul risultato è circa un livello.
 *
 * La tessera è stretta e si ripete: le bande sono orizzontali, quindi basta che il rumore cambi
 * lungo la riga, e la ripetizione ogni [GRAIN_TILE] pixel di un paio di livelli non si vede. È alta
 * quanto il rettangolo.
 * Il generatore è un LCG a seme fisso: costa poco e dà la stessa immagine a ogni corsa, l'unico
 * modo perché il banco la possa misurare. Il rumore è la somma di due estrazioni, come nel
 * programma.
 *
 * @param stops le tappe della rampa, come le riceve il pennello: posizione e quanto di [peak].
 * @param peak quanto copre la tinta nel suo punto più forte.
 * @param height quanto è alto in pixel il rettangolo da coprire.
 * @param top dove comincia quel rettangolo nello spazio del disegno: uno shader nasce ancorato
 *   all'origine del nodo, e la fascia comincia sopra di lei.
 * @return la maschera pronta da mettere in un paint colorato, o `null` se non c'è niente da
 *   coprire (altezza zero) o se la memoria non basta.
 */
internal fun rampMask(stops: List<Pair<Float, Float>>, peak: Float, height: Int, top: Float): Shader? {
    val mappa = rampBitmap(stops, peak, height) ?: return null
    return BitmapShader(mappa, TileMode.REPEAT, TileMode.CLAMP).apply {
        if (top != 0f) setLocalMatrix(Matrix().apply { setTranslate(0f, top) })
    }
}

/**
 * La tessera con dentro la rampa e il suo rumore, cioè quello che [rampMask] stende.
 *
 * Vive in una funzione sua perché è la sola cosa che il banco può misurare davvero: nei pixel resi
 * Skia aggiunge già un rumore suo, mentre nell'opacità della tessera il rumore nostro si conta.
 */
internal fun rampBitmap(stops: List<Pair<Float, Float>>, peak: Float, height: Int): Bitmap? {
    if (height <= 0 || stops.isEmpty()) return null
    val mappa = try {
        Bitmap.createBitmap(GRAIN_TILE, height, Bitmap.Config.ALPHA_8)
    } catch (e: OutOfMemoryError) {
        return null
    }
    /*
     * ⚠️ La riga si misura con `rowBytes`: Skia può allinearla oltre la larghezza, e un buffer più
     * corto di `byteCount` fa fallire la copia.
     */
    val passo = mappa.rowBytes
    val byte = ByteArray(passo * height)
    var seme = GRAIN_SEED
    for (y in 0 until height) {
        val quanto = rampAt(stops, (y + 0.5f) / height) * peak * 255f
        val riga = y * passo
        for (x in 0 until GRAIN_TILE) {
            seme = seme * 1664525 + 1013904223
            val u1 = (seme ushr 8 and 0xFFFF) / 65535f - 0.5f
            seme = seme * 1664525 + 1013904223
            val u2 = (seme ushr 8 and 0xFFFF) / 65535f - 0.5f
            val con = quanto + (u1 + u2) * GRAIN_STEPS
            byte[riga + x] = con.roundToInt().coerceIn(0, 255).toByte()
        }
    }
    mappa.copyPixelsFromBuffer(ByteBuffer.wrap(byte))
    return mappa
}

/**
 * Quanto vale la rampa alla frazione `t`, interpolando fra le due tappe che la racchiudono.
 *
 * Lineare, come `Brush.verticalGradient`: con una tinta sola che cambia solo opacità, la
 * premoltiplicazione non cambia niente, quindi la maschera ripete la stessa rampa del pennello.
 */
private fun rampAt(stops: List<Pair<Float, Float>>, t: Float): Float {
    if (t <= stops.first().first) return stops.first().second
    for (i in 1 until stops.size) {
        val (fine, quantoFine) = stops[i]
        if (t > fine) continue
        val (inizio, quantoInizio) = stops[i - 1]
        val largo = fine - inizio
        if (largo <= 0f) return quantoFine
        val dove = (t - inizio) / largo
        return quantoInizio + (quantoFine - quantoInizio) * dove
    }
    return stops.last().second
}

/**
 * Quanto è larga la tessera del rumore, in pixel.
 *
 * Il numero regola insieme la memoria e il periodo: la maschera pesa `GRAIN_TILE` byte per riga,
 * cioè circa 128 kB per una fascia di mille pixel, e a 128 la ripetizione di un rumore di un
 * paio di livelli non si vede.
 */
private const val GRAIN_TILE = 128

/**
 * Quanti livelli di opacità vale il rumore.
 *
 * Due, per il conto scritto su [rampMask]: con un distacco tipico di un centinaio di livelli fra
 * tinta e fondo servono due livelli di opacità per un livello pieno sul risultato. Anche con una
 * tinta bianca su fondo nero restano meno dell'uno per cento del colore, sotto la soglia in cui
 * una grana si vede.
 */
private const val GRAIN_STEPS = 2f

/**
 * Il seme del generatore.
 *
 * Fisso di proposito, perché il banco misuri la stessa maschera che va sul telefono. Il valore non
 * ha significato: è un numero dispari abbastanza grande da far partire l'LCG lontano da zero.
 */
private const val GRAIN_SEED = 0x5EED_1CE7

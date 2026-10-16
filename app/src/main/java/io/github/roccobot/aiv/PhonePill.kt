package io.github.roccobot.aiv

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.min
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt

/*
 * La pillola al posto del FAB sul telefono in verticale, e il modo in cui ogni pillola si riempie.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * Un'impostazione di 'Pulsanti e indicatori' con due file di gettoni (risposte A1-D1
 * dell'utente, 4.00). La prima dice che cosa prende il posto del FAB ([PhonePill]): niente (il
 * FAB), un tasto tondo che si allunga nella pillola ([PhonePill.SLIDE]) o la pillola sempre
 * aperta ([PhonePill.EXTENDED]). La seconda dice di che cosa è fatta la pillola ([PillFill]), e
 * riempie anche le pillole degli schermi larghi.
 * La pillola è in basso, ancorata al lato preferito dove era il FAB: il tasto d'angolo è quello
 * sotto il pollice, e le due modalità lo tengono lì.
 * ⚠️ Un tasto della pillola non ha etichetta: il tocco lungo la mostra in un suggerimento, e
 * nient'altro (risposta B1). Quindi il tocco lungo di 'Mostra nascoste' in una pillola non esiste.
 */

/**
 * Che cos'è la pillola e di che cosa è fatta, come la applica questo apparecchio.
 *
 * ⚠️ **I valori sono quelli in vigore, non quelli salvati**: [pillMode] spegne la modalità su un
 * tablet e [pillFillIn] toglie il vetro sotto Android 12. Le schermate leggono questo e mai le
 * impostazioni, quindi non possono dissentire fra loro.
 */
@Immutable
data class PillLook(
    val mode: PhonePill = PhonePill.OFF,
    val fill: PillFill = PillFill.SOLID,
    /**
     * Se [mode] si disegna come Menu basso ([MainControl.BOTTOM]) e non come pillola: allora
     * [PhonePill.SLIDE] è il menu che sale dal basso e [PhonePill.EXTENDED] quello fisso.
     */
    val bar: Boolean = false,
    /** Se il controllo principale è il menu 'Start' ([MainControl.CORNER]): sempre [PhonePill.SLIDE]. */
    val corner: Boolean = false,
    /** La regolazione dell'aspetto traslucido. */
    val glass: GlassTune = GlassTune(),
    /**
     * Se il menu 'Start' a riposo è un tasto tondo solo ([cornerRound] `true`) o la pillola
     * verticale di due tasti. Lo legge solo il menu 'Start'. Vedi [Settings.cornerRound].
     */
    val cornerRound: Boolean = true
)

/**
 * La regolazione dell'aspetto traslucido: i cursori e i due colori che compaiono con lui
 * (richiesta dell'utente, nota N1 del giro della 4.15).
 *
 * ⚠️ **I valori di fabbrica sono dell'utente** (note D del giro della 4.20 e B del giro della
 * 4.36, risposta C2): raggio 16dp, intensità 300%, opacità 40%, scostamento 35%. Il vetro è legato
 * al tema: [shift] scurisce sul tema chiaro e schiarisce su quello scuro, e ogni tema ha il suo
 * colore (risposta B3). Un telefono che ha mosso un cursore tiene il suo valore.
 *
 * @property radius il raggio della sfocatura, in dp.
 * @property intensity quanto si ravvivano i colori dietro, in percentuale: 100 li lascia come sono.
 * @property tint quanto colore ha il vetro ('Opacità'), in percentuale dell'inchiostro del tema.
 * @property shift quanto l'immagine sfocata si allontana dalla pagina ('Scostamento'), in
 *   percentuale: nero sul tema chiaro, bianco su quello scuro.
 * @property lightColour il colore del vetro sul tema chiaro, in ARGB, o `null` per l'accento in
 *   vigore; [darkColour] lo stesso sul tema scuro.
 */
@Immutable
data class GlassTune(
    val radius: Int = RADIUS,
    val intensity: Int = INTENSITY,
    val tint: Int = TINT,
    val shift: Int = SHIFT,
    val lightColour: Int? = null,
    val darkColour: Int? = null
) {
    /** Il colore scelto per il tema in vigore, o `null` per l'accento. */
    fun colourFor(light: Boolean): Int? = if (light) lightColour else darkColour

    /** Questa regolazione col colore di un tema sostituito: `null` torna all'accento. */
    fun withColour(light: Boolean, colour: Int?): GlassTune =
        if (light) copy(lightColour = colour) else copy(darkColour = colour)

    companion object {
        const val RADIUS = 16
        const val INTENSITY = 300
        const val TINT = 40
        const val SHIFT = 35
        val RADIUS_RANGE = 0..40
        val INTENSITY_RANGE = 0..300
        val TINT_RANGE = 0..150
        val SHIFT_RANGE = 0..50
    }
}

/**
 * Se i quattro cursori di [GlassTune] si mostrano: l'interruttore chiesto nella nota N1.
 *
 * Spento, nasconde i cursori e tiene i loro valori: lo stile raggiunto resta in vigore.
 */
const val GLASS_TUNING = true

/**
 * La pillola come l'attività la mette in scena, accanto agli altri aspetti.
 *
 * È un `CompositionLocal`, come `LocalAivDepth`, perché la leggono finestre che non ricevono le
 * impostazioni (il velo dei menu legge [PillLook.fill]).
 */
val LocalPillLook = staticCompositionLocalOf { PillLook() }

/** Se questo telefono sa disegnare il vetro smerigliato: un `RenderEffect` vuole Android 12. */
fun glassAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Il riempimento in vigore: dove il vetro non si può disegnare, torna a quello di fabbrica. */
fun pillFillIn(fill: PillFill): PillFill =
    if (fill == PillFill.GLASS && !glassAvailable()) PillFill.SOLID else fill

/**
 * La modalità in vigore: la pillola scelta su un telefono il cui controllo principale è la
 * pillola o il Menu basso, il FAB altrove. Se si disegna come Menu basso lo dice [barIn].
 *
 * ⚠️ **La larghezza minima e non quella corrente**: un telefono in orizzontale resta un telefono,
 * e gli schermi larghi hanno la loro pillola; qui si decide solo se la voce esiste.
 */
fun pillMode(control: MainControl, mode: PhonePill, smallestWidthDp: Int): PhonePill = when {
    control == MainControl.FAB || !pillOffered(smallestWidthDp) -> PhonePill.OFF
    // ⚠️ Il menu 'Start' non ha una forma fissa: si chiude sempre nella sua pillola.
    control == MainControl.CORNER -> PhonePill.SLIDE
    else -> mode
}

/** Se il controllo principale in vigore è il menu 'Start': solo su un telefono, come [pillMode]. */
fun cornerIn(control: MainControl, smallestWidthDp: Int): Boolean =
    control == MainControl.CORNER && pillOffered(smallestWidthDp)

/** Se il controllo principale in vigore è il Menu basso: solo su un telefono, come [pillMode]. */
fun barIn(control: MainControl, smallestWidthDp: Int): Boolean =
    control == MainControl.BOTTOM && pillOffered(smallestWidthDp)

/** Se questo apparecchio è un telefono, l'unico posto in cui l'impostazione esiste. */
fun pillOffered(smallestWidthDp: Int): Boolean = smallestWidthDp < Adaptive.PHONE_MAX

/**
 * Quello che c'è dietro una pillola, registrato perché il vetro lo possa sfocare.
 *
 * ⚠️⚠️ **Fratello della pillola, mai suo genitore**: la pillola disegna questo livello, e una
 * sorgente che la contenesse disegnerebbe sé stessa dentro sé stessa. Ogni schermata segna il
 * contenuto che scorre sotto la pillola ([backdropSource]) e passa lo stesso oggetto alla pillola.
 * Solo dove il vetro è in vigore ([rememberBackdrop] risponde `null` altrimenti): registrare costa
 * un passaggio in più sull'elenco di disegno del contenuto.
 */
class Backdrop internal constructor(internal val layer: GraphicsLayer) {
    /**
     * Dove comincia il contenuto registrato, in coordinate della radice.
     *
     * ⚠️ È uno stato perché il vetro si ridisegni quando il contenuto si sposta: la pillola lo
     * legge mentre disegna, e un campo semplice lascerebbe l'immagine sfocata dov'era.
     */
    internal var origin by mutableStateOf(Offset.Zero)
}

/** La registrazione di quello che c'è dietro la pillola, o `null` dove il vetro non è in vigore. */
@Composable
fun rememberBackdrop(): Backdrop? {
    val layer = rememberGraphicsLayer()
    val glass = LocalPillLook.current.fill == PillFill.GLASS
    return remember(layer, glass) { if (glass) Backdrop(layer) else null }
}

/** Segna il contenuto su cui una pillola può posarsi: si disegna come prima, e si registra per il vetro. */
fun Modifier.backdropSource(backdrop: Backdrop?): Modifier =
    if (backdrop == null) this else this
        .onGloballyPositioned { backdrop.origin = it.positionInRoot() }
        .drawWithContent {
            backdrop.layer.record { this@drawWithContent.drawContent() }
            drawLayer(backdrop.layer)
        }

/**
 * Quanto sfoca il vetro dietro il velo dei menu: il raggio di fabbrica del vetro (16dp, nota D
 * dell'utente sul giro della 4.20). Non quello del cursore: il velo è dei menu, e lo disegnano
 * finestre che non leggono la regolazione.
 */
internal val GLASS_BLUR = GlassTune.RADIUS.dp


/**
 * Quanto accento ha il vetro: un po' meno trasparente della variante A2 scelta dall'utente, come
 * ha chiesto, perché sul tema scuro il colore quasi spariva.
 */
private const val GLASS_INK_LIGHT = 0.80f
private const val GLASS_INK_DARK = 0.65f

/** L'opacità dell'accento in [PillFill.TRANSLUCENT]: 80%, il numero dell'utente. */
private const val TRANSLUCENT_INK = 0.80f

/**
 * Riempie la superficie della pillola: l'accento, l'accento all'80%, o il vetro smerigliato.
 *
 * ⚠️ **È un modificatore e non un colore di `Surface`** perché il vetro disegna quello che un
 * colore non può: quello che c'è dietro, sfocato. I tre casi passano di qui perché il tasto tondo
 * di [PhonePill.SLIDE], la pillola e le pillole degli schermi larghi si riempiano in un modo solo.
 * Senza un [backdrop] il vetro è l'accento su quello che la finestra mostra dietro, come nella
 * schermata iniziale larga, dove la pillola è su un fondo vuoto.
 */
@Composable
fun Modifier.pillFill(backdrop: Backdrop?): Modifier =
    buttonFill(backdrop, pillAccent(), RoundedCornerShape(50))

/**
 * Riempie la superficie di un pulsante principale col riempimento in vigore: [colour] com'è,
 * all'80%, o vetro smerigliato tinto con lui.
 *
 * Ci passa anche il FAB (nota E dell'utente sul giro della 4.04), col suo colore e la sua forma:
 * pillole e FAB si riempiono in un modo solo, e solo questi due valori li distinguono.
 * Il vetro tiene i due inchiostri della pillola, [GLASS_INK_LIGHT] e [GLASS_INK_DARK], applicati
 * all'alfa di [colour], così un FAB che sfuma nell'altro accento mentre è premuto resta vetro.
 */
@Composable
fun Modifier.buttonFill(backdrop: Backdrop?, colour: Color, shape: Shape): Modifier {
    val fill = LocalPillLook.current.fill
    val light = LocalAivLight.current
    return when (fill) {
        PillFill.SOLID -> this.clip(shape).drawBehind {
            drawOutline(shape.createOutline(size, layoutDirection, this), colour)
        }
        PillFill.TRANSLUCENT -> this.clip(shape).drawBehind {
            drawOutline(
                shape.createOutline(size, layoutDirection, this),
                colour.copy(alpha = colour.alpha * TRANSLUCENT_INK)
            )
        }
        PillFill.GLASS -> {
            val tune = LocalPillLook.current.glass
            val ink = (if (light) GLASS_INK_LIGHT else GLASS_INK_DARK) * tune.tint / 100f
            // ⚠️ Un colore scelto dall'utente sostituisce [colour] e ne tiene l'alfa, così la
            // pressione del FAB si vede ancora (risposta B3).
            val base = tune.colourFor(light)?.let { Color(it).copy(alpha = colour.alpha) } ?: colour
            val tint = base.copy(alpha = (base.alpha * ink).coerceIn(0f, 1f))
            val radiusPx = with(LocalDensity.current) { tune.radius.dp.toPx() }
            val saturation = tune.intensity / 100f
            /*
             * ⚠️ **L'effetto si costruisce una volta per coppia di cursori**, fuori dal disegno:
             * il disegno gira a ogni fotogramma in cui il contenuto sotto il vetro si muove, e uno
             * scorrimento ne costruirebbe uno per fotogramma. Presidiato da `VetroTest`. La
             * registrazione segue comunque il contenuto.
             */
            val effect = remember(radiusPx, saturation) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) glassEffect(radiusPx, saturation) else null
            }
            /*
             * ⚠️ **'Scostamento' non ha segno** (risposta C2): allontana il vetro dalla pagina,
             * quindi scurisce sul tema chiaro e schiarisce su quello scuro. Con un segno, lo
             * stesso valore apparirebbe opposto sui due temi.
             */
            val wash = (if (light) Color.Black else Color.White).copy(alpha = tune.shift / 100f)
            val ground = MaterialTheme.colorScheme.background
            val blurred = rememberGraphicsLayer()
            var me by remember { mutableStateOf(Offset.Zero) }
            this
                .onGloballyPositioned { me = it.positionInRoot() }
                .drawBehind {
                    val outline = shape.createOutline(size, layoutDirection, this)
                    val path = Path().apply {
                        when (outline) {
                            is Outline.Rounded -> addRoundRect(outline.roundRect)
                            is Outline.Rectangle -> addRect(outline.rect)
                            is Outline.Generic -> addPath(outline.path)
                        }
                    }
                    if (backdrop != null && effect != null) {
                        /*
                         * ⚠️ **La registrazione è più larga della pillola di due raggi**, e si
                         * ridisegna spostata di altrettanto: la sfocatura legge i vicini di ogni
                         * pixel, e al bordo della pillola sarebbero trasparenti, cioè un bordo
                         * scuro.
                         */
                        val edge = (radiusPx * 2).roundToInt()
                        blurred.renderEffect = effect
                        blurred.topLeft = IntOffset(-edge, -edge)
                        blurred.record(
                            size = IntSize(size.width.roundToInt() + edge * 2, size.height.roundToInt() + edge * 2)
                        ) {
                            translate(backdrop.origin.x - me.x + edge, backdrop.origin.y - me.y + edge) {
                                drawLayer(backdrop.layer)
                            }
                        }
                        /*
                         * ⚠️⚠️ **Il fondo della pagina va sotto la copia sfocata**: il contenuto
                         * registrato non ha un fondo suo (il colore della pagina è la `Surface`
                         * del tema, fuori), quindi testi e bordi disegnati sul niente
                         * diventerebbero un alone tenue, e gli originali nitidi si vedrebbero
                         * attraverso. Le immagini sono opache, e per questo si sfocano comunque.
                         */
                        clipPath(path) {
                            drawRect(ground)
                            drawLayer(blurred)
                        }
                    }
                    if (tune.shift != 0) drawOutline(outline, wash)
                    drawOutline(outline, tint)
                }
                .clip(shape)
        }
    }
}

/** La sfocatura e poi la saturazione, in quest'ordine, come le scrive il `backdrop-filter` del DF. */
@RequiresApi(Build.VERSION_CODES.S)
private fun glassEffect(radius: Float, saturation: Float): androidx.compose.ui.graphics.RenderEffect {
    DrawWork.glass++
    // ⚠️ Un raggio zero non sfoca, e `createBlurEffect` lo rifiuta.
    val colour = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(saturation) })
    return if (radius <= 0f) {
        RenderEffect.createColorFilterEffect(colour).asComposeRenderEffect()
    } else {
        RenderEffect.createColorFilterEffect(
            colour,
            RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
        ).asComposeRenderEffect()
    }
}

/**
 * Il colore della pillola: l'accento dell'altro tema, in ogni riempimento (scelta dell'utente,
 * varianti A2 e C1). È la coppia del FAB (`aivLauncher`), che si stacca dalla pagina più
 * dell'accento del tema. Il tema è quello dell'app e non del sistema ([LocalAivLight]), per la
 * ragione scritta sui colori del FAB.
 */
@Composable
fun pillAccent(): Color = aivAccent(!LocalAivLight.current)

/**
 * L'inchiostro dei glifi della pillola: quello che va con [pillAccent], in ogni riempimento,
 * salvo un vetro col colore scelto dall'utente ([ownGlassInk]).
 */
@Composable
fun pillInk(): Color = ownGlassInk() ?: aivOnAccent(!LocalAivLight.current)

/**
 * L'inchiostro dei glifi su un vetro col colore scelto dall'utente, o `null` dove non c'è.
 *
 * ⚠️ **L'app sceglie da sé chiaro o scuro** (risposta A1 a `colore-icone-vetro`): l'inchiostro
 * dell'accento, su un colore chiaro, sparirebbe. I due inchiostri sono quelli del FAB sul vetro
 * ([FAB_GLASS_INK_LIGHT], [FAB_GLASS_INK_DARK]), e vince quello col contrasto più alto sul colore
 * scelto, prima che il vetro lo diluisca su quello che c'è dietro.
 */
@Composable
internal fun ownGlassInk(): Color? {
    val look = LocalPillLook.current
    if (look.fill != PillFill.GLASS) return null
    val own = look.glass.colourFor(LocalAivLight.current) ?: return null
    return inkOn(Color(own))
}

/** Dei due inchiostri del vetro, quello con più contrasto su [colour]. */
internal fun inkOn(colour: Color): Color {
    val ground = colour.copy(alpha = 1f).luminance()
    fun contrast(ink: Color): Float {
        val l = ink.luminance()
        return (maxOf(l, ground) + 0.05f) / (minOf(l, ground) + 0.05f)
    }
    return if (contrast(FAB_GLASS_INK_LIGHT) >= contrast(FAB_GLASS_INK_DARK)) FAB_GLASS_INK_LIGHT else FAB_GLASS_INK_DARK
}

/**
 * Quanto la pillola impiega ad aprirsi o chiudersi: al massimo 160 ms, il tetto dell'utente.
 *
 * Parte velocissima e rallenta (richiesta dell'utente): la curva è l'ease-out esponenziale, che
 * percorre quasi tutta la strada nel primo terzo.
 */
private const val PILL_OPEN_MS = 160

/** L'ease-out esponenziale: `1 - 2^(-10x)`, fissato a 1 alla fine. */
internal val PILL_EASE = Easing { x -> if (x >= 1f) 1f else 1f - 2f.pow(-10f * x) }

/**
 * La distanza dei due tasti alle estremità della pillola a scomparsa aperta dai suoi capi, a fila
 * piena: 'qualche dp' nella nota A dell'utente sul giro della 4.33, e il numero ne è una lettura.
 */
private val SLIDE_INSET = 6.dp

/** Quanto inchiostro tiene la ×: semitrasparente, la parola dell'utente. */
private const val CLOSE_INK = 0.6f

/** Quanto si rimpicciolisce il simbolo che se ne va, e cresce quello che arriva: lo 0,35 del FAB. */
private const val SWAP_ZOOM = 0.35f

/**
 * La pillola del telefono in verticale, nella modalità che l'impostazione chiede.
 *
 * ⚠️ **Chi chiama la mette dove sarebbe il FAB** (lato, rientri, margini): il tasto d'angolo cade
 * esattamente dove era il FAB.
 *
 * @param entries il menu del FAB, come lo legge la pillola larga.
 * @param arm il salto, o `null` dove la schermata non ne ha.
 * @param nested lo scorrimento annidato della schermata, da cui il salto passa come un dito.
 * @param fabLabel che cosa fa ora il tasto tondo, per i lettori di schermo ([PhonePill.SLIDE]).
 * @param holdLabel che cosa fa il suo tocco lungo, e [onHold] il gesto; `null` dove il FAB non ne
 *   ha. Ce l'ha solo il tasto tondo (risposta C1): a pillola aperta il FAB non c'è.
 * @param fabGlyph che cosa mostra il tasto tondo a riposo: il segno dell'app, o la freccia quando
 *   il salto è armato.
 */
@Composable
fun PhonePillBar(
    entries: List<PillEntry>,
    arm: JumpArm?,
    nested: NestedScrollConnection?,
    fabLabel: String,
    holdLabel: String?,
    onHold: (() -> Unit)?,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    /**
     * Dove va il tasto d'angolo, dentro [modifier]: i rientri e i margini che aveva il FAB.
     *
     * È separato da [modifier] perché il Menu basso occupa tutta la larghezza fino al vetro, e
     * solo la sua pillola chiusa tiene l'angolo del FAB.
     */
    corner: Modifier = Modifier,
    /** Il glifo del FAB senza la freccia del salto, per la pillola chiusa del Menu basso. */
    mark: @Composable (String?) -> Unit = {},
    /**
     * Che cosa fa il tasto tondo a riposo, senza il salto: la pillola chiusa del Menu basso dice il
     * suo salto ('in fondo', sempre), mentre [fabLabel] segue la freccia del FAB.
     */
    restLabel: String? = null,
    /**
     * Le celle del menu 'Start', nell'ordine di lettura per la mano destra, dove differiscono da
     * [entries]: il 3x3 della schermata iniziale tiene al loro posto le tre viste e 'Mostra
     * nascoste' (decisioni C1 e C2).
     */
    cornerEntries: List<PillEntry>? = null,
    fabGlyph: @Composable (String?) -> Unit
) {
    if (entries.isEmpty()) return
    val atEnd = LocalPadLook.current.hand == Hand.RIGHT
    val look = LocalPillLook.current
    val scope = rememberCoroutineScope()
    val ordered = if (atEnd) entries else mirrored(entries)
    if (look.corner) {
        CompositionLocalProvider(LocalContentColor provides pillInk()) {
            CornerMenu(
                // ⚠️ Nell'ordine di lettura per la mano destra: il menu 'Start' specchia da sé ogni riga.
                entries = cornerEntries ?: entries,
                arm = arm,
                fabLabel = restLabel ?: fabLabel,
                holdLabel = holdLabel,
                onHold = onHold,
                backdrop = backdrop,
                atEnd = atEnd,
                corner = corner,
                modifier = modifier,
                onJump = { toward -> scope.launch { arm?.leapToward(toward, nested) } },
                mark = mark,
                // ⚠️ A riposo il tondo è il tasto tondo della pillola a scomparsa: etichetta, salto
                // e glifo sono quelli del FAB, come là.
                roundLabel = fabLabel,
                onLeap = { scope.launch { arm?.leap(nested) } },
                fabGlyph = fabGlyph
            )
        }
        return
    }
    if (look.bar) {
        CompositionLocalProvider(LocalContentColor provides pillInk()) {
            BottomMenu(
                entries = ordered,
                arm = arm,
                fabLabel = restLabel ?: fabLabel,
                holdLabel = holdLabel,
                onHold = onHold,
                backdrop = backdrop,
                atEnd = atEnd,
                slide = look.mode == PhonePill.SLIDE,
                corner = corner,
                modifier = modifier,
                onJump = { toward -> scope.launch { arm?.leapToward(toward, nested) } },
                mark = mark
            )
        }
        return
    }
    BoxWithConstraints(modifier = modifier.then(corner).declaresFoot()) {
        CompositionLocalProvider(LocalContentColor provides pillInk()) {
            when (look.mode) {
                PhonePill.SLIDE -> SlidePill(
                    ordered, arm, fabLabel, holdLabel, onHold, backdrop, atEnd, maxWidth,
                    onJump = { scope.launch { arm?.leap(nested) } },
                    onJumpTo = { toward -> scope.launch { arm?.leapToward(toward, nested) } },
                    fabGlyph = fabGlyph
                )
                else -> ExtendedPill(
                    ordered, arm, backdrop, atEnd, maxWidth,
                    onJump = { toward -> scope.launch { arm?.leapToward(toward, nested) } }
                )
            }
        }
    }
}

/**
 * Le voci nell'ordine in cui le legge una pillola per la mano sinistra: lo specchio di quella per
 * la destra, viste comprese (ordine dell'utente, voce `4.15-04`). Il tasto sotto il pollice è
 * 'Impostazioni' da tutti e due i lati.
 */
internal fun mirrored(entries: List<PillEntry>): List<PillEntry> = entries.asReversed()

/**
 * La misura di un tasto quando la pillola deve farne entrare [count] in [room].
 *
 * Mai più grande di [PILL_KEY], e più piccola solo su un telefono stretto con molte voci: nove
 * tasti da 44 non entrano in uno schermo largo 360.
 */
private fun keyFor(count: Int, room: Dp): Dp = min(PILL_KEY, room / count.coerceAtLeast(1))

/**
 * [PhonePill.SLIDE]: un tasto tondo che si allunga nella pillola e si richiude sulla sua ×.
 *
 * ⚠️⚠️ **Il tasto tondo tiene i due gesti del FAB** (risposta C1): con il salto armato il tocco
 * salta, altrimenti apre la pillola; il tocco lungo fa quello del FAB. Aperta, il tasto d'angolo è
 * la × e il suo tocco lungo ne mostra l'etichetta, come ogni altro tasto.
 * Indietro chiude la pillola, come chiuderebbe il menu del FAB. L'apertura è uno stato che
 * sopravvive alla rotazione, e un tocco su una voce chiude la pillola prima di agire, come una
 * riga di menu chiude il suo menu.
 */
@Composable
private fun SlidePill(
    entries: List<PillEntry>,
    arm: JumpArm?,
    fabLabel: String,
    holdLabel: String?,
    onHold: (() -> Unit)?,
    backdrop: Backdrop?,
    atEnd: Boolean,
    room: Dp,
    onJump: () -> Unit,
    onJumpTo: (Int) -> Unit,
    fabGlyph: @Composable (String?) -> Unit
) {
    var open by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = open) { open = false }
    val p by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(PILL_OPEN_MS, easing = PILL_EASE),
        label = "pill"
    )
    val count = entries.size + 1
    /*
     * ⚠️⚠️ **Il tasto tondo è sempre di [PILL_KEY]** (nota B dell'utente sul giro della 4.25):
     * con molte voci su un telefono stretto si rimpiccioliscono solo i tasti della pillola aperta,
     * non il tondo né l'altezza della pillola.
     *
     * Aperta con molte voci occupa tutta la fila (scelta A2 dell'utente): come nella pillola
     * estesa ([PILL_FULL]) si allargano le celle e non i tasti. La cella d'angolo cresce dal tasto
     * alla cella mentre si apre, così a riposo il tasto tondo è esattamente dov'era. La fila tiene
     * [SLIDE_INSET] dai due capi, e le celle si dividono il resto.
     */
    val wide = count >= PILL_FULL
    val inset = if (wide) SLIDE_INSET else 0.dp
    val key = keyFor(count, room - inset * 2)
    val cell = if (wide) (room - inset * 2) / count else key
    val full = cell * count + inset * 2
    val q = arm?.shown ?: 0f
    val armed = arm?.armed == true
    /*
     * ⚠️ **Il salto prende i due tasti più vicini all'angolo, × compresa** (voce `4.34-04`): la ×
     * diventa `↓` e il tasto accanto `↑`. Il tasto accanto alla ×, in ordine di lettura: a
     * destra è l'ultimo.
     */
    val outer = if (atEnd) entries.size - 1 else 0
    val jumping = open && armed
    val top = stringResource(R.string.jump_top)
    val bottom = stringResource(R.string.jump_bottom)
    val closeLabel = stringResource(R.string.pick_close)
    val corner = PillEntry(
        icon = Icons.Default.Close,
        label = when {
            jumping -> bottom
            open -> closeLabel
            else -> fabLabel
        },
        onHold = if (open) null else onHold,
        onTap = {
            when {
                jumping -> onJumpTo(1)
                open -> open = false
                arm?.armed == true -> onJump()
                else -> open = true
            }
        }
    )
    val cornerKey = @Composable {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.width(lerp(PILL_KEY, cell, p))) {
            PillKey(
                entry = corner,
                size = PILL_KEY,
                // ⚠️ Chiusa, il tasto tondo è il FAB: il suo tocco lungo è quello del FAB e non un
                // suggerimento. Aperta, è un tasto della pillola come gli altri.
                holdLabel = if (open) null else holdLabel,
                glyph = {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier.graphicsLayer {
                                alpha = 1f - p
                                // ⚠️ Senza un buffer: vedi [MARK_FADE].
                                compositingStrategy = MARK_FADE
                                val s = 1f - SWAP_ZOOM * p
                                scaleX = s
                                scaleY = s
                            }
                        ) {
                            // ⚠️ Il tasto tondo si disegna qui e non con [TapHoldFab]: senza, il
                            // segno terrebbe misura e centro del FAB quadrato.
                            CompositionLocalProvider(LocalRoundKey provides true) { fabGlyph(null) }
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.graphicsLayer {
                                alpha = p
                                // ⚠️ Senza un buffer: vedi [MARK_FADE].
                                compositingStrategy = MARK_FADE
                                val s = 1f - SWAP_ZOOM * (1f - p)
                                scaleX = s
                                scaleY = s
                            }
                        ) {
                            // Aperta, la × sfuma in `↓` col salto, come i tasti di [JumpKey]
                            // (voce `4.34-04`).
                            val j = if (open) q else 0f
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    alpha = CLOSE_INK * (1f - j).pow(JUMP_FULL)
                                    val s = 1f - JUMP_ZOOM * j
                                    scaleX = s
                                    scaleY = s
                                }
                            ) { Icon(Icons.Default.Close, contentDescription = null) }
                            if (j > 0f) {
                                Box(
                                    modifier = Modifier.graphicsLayer {
                                        alpha = j.pow(JUMP_FULL)
                                        val s = 1f - JUMP_ZOOM * (1f - j)
                                        scaleX = s
                                        scaleY = s
                                    }
                                ) { Icon(Glyphs.BrowseBottom, contentDescription = null, modifier = Modifier.size(PILL_GLYPH)) }
                            }
                        }
                    }
                }
            )
        }
    }
    Box(
        /*
         * ⚠️ **La fila si appoggia al lato preferito**: ogni cella arrotonda la sua larghezza al
         * pixel, quindi otto celle possono risultare qualche pixel più corte della pillola, e
         * appoggiata all'inizio la × resterebbe staccata dal bordo (3 px misurati sul banco, a
         * densità 1). Così il tasto d'angolo è esatto, e la differenza va all'altro capo.
         */
        contentAlignment = if (atEnd) Alignment.CenterEnd else Alignment.CenterStart,
        modifier = Modifier
            .width(lerp(PILL_KEY, full, p))
            .height(PILL_KEY)
            .pillFill(backdrop)
    ) {
        /*
         * ⚠️⚠️ **`wrapContentWidth` senza limite e non `requiredWidth`**: una fila più larga del
         * suo riquadro, con `requiredWidth`, si centra sul riquadro, e il tasto d'angolo finirebbe
         * mezza pillola fuori dallo schermo, dove il tocco non lo trova. Così la fila si allinea
         * al lato preferito, e il tasto tondo resta dov'era il FAB.
         *
         * ⚠️ **Centrata in altezza**: una `Row` mette i figli in alto, e con molte voci su un
         * telefono stretto [keyFor] rende i tasti aperti più piccoli della pillola, mentre la ×
         * tiene [PILL_KEY]. Presidiato da `PillRowTest`.
         */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .wrapContentWidth(
                    align = if (atEnd) Alignment.End else Alignment.Start,
                    unbounded = true
                )
                .padding(horizontal = inset * p)
        ) {
            if (!atEnd) cornerKey()
            entries.forEachIndexed { i, entry ->
                // ⚠️ Un tasto nascosto dietro quello tondo non è ancora un comando.
                val usable = entry.copy(enabled = entry.enabled && open, onTap = { open = false; entry.onTap() })
                /*
                 * La pillola aperta diventa il salto mentre si scorre, come le altre modalità (nota
                 * B dell'utente sul giro della 4.33): il tasto accanto alla × è `↑`, e la × è `↓`
                 * (vedi [outer]).
                 */
                val jump = if (arm != null && i == outer) -1 else 0
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.width(cell).graphicsLayer { alpha = p }
                ) {
                    if (jump == 0) {
                        PillKey(entry = usable, size = key, enabled = usable.enabled && !armed)
                    } else {
                        JumpKey(usable, jump, q, armed, key, top, bottom, onJumpTo)
                    }
                }
            }
            if (atEnd) cornerKey()
        }
    }
}

/**
 * [PhonePill.EXTENDED]: la pillola sempre aperta, che si chiude sui due tasti del salto mentre si
 * scorre.
 *
 * ⚠️⚠️ **La chiusura segue lo stato del salto** (risposta C2): i tasti che se ne vanno si
 * rimpiccioliscono con [JumpArm.shown], il pixel che il dito muove è il pixel che la pillola
 * chiude, e si riapre quando la freccia tornerebbe al segno. I due tasti d'angolo diventano `↑`
 * (dentro) e `↓` (nell'angolo) con la stessa dissolvenza della freccia, insieme alla pillola.
 * Le due direzioni insieme, a differenza della freccia unica del FAB: la pillola ha due tasti.
 * ⚠️ **Il tocco salta solo a salto armato**: a metà chiusura i tasti sono ancora le voci, e fanno
 * quello che mostrano.
 */
@Composable
private fun ExtendedPill(
    entries: List<PillEntry>,
    arm: JumpArm?,
    backdrop: Backdrop?,
    atEnd: Boolean,
    room: Dp,
    onJump: (Int) -> Unit
) {
    val q = arm?.shown ?: 0f
    val armed = arm?.armed == true
    val count = entries.size
    val key = keyFor(count, room)
    /*
     * ⚠️⚠️ **Da [PILL_FULL] voci la pillola occupa tutta la fila** (regola dell'utente): [room] è
     * già la fila fra i due margini, quindi sette celle uguali mettono la quarta, 'Cerca', sulla
     * linea di mezzo dello schermo. Si allargano le celle e non i tasti: bersaglio del tocco e
     * increspatura restano quelli del tasto, e la larghezza in più è spaziatura.
     * Con il Feed come terza vista la schermata iniziale può avere otto voci (scelta A1 dell'utente,
     * 6.02): [keyFor] rimpicciolisce un poco i tasti, e 'Cerca' è la quarta di otto, spostata di
     * mezza cella dalla linea di mezzo.
     */
    val cell = if (count >= PILL_FULL) room / count else key
    // ⚠️ I due tasti d'angolo, in ordine di lettura: a destra sono gli ultimi due.
    val inner = if (atEnd) count - 2 else 1
    val outer = if (atEnd) count - 1 else 0
    val folds = count >= 2
    val top = stringResource(R.string.jump_top)
    val bottom = stringResource(R.string.jump_bottom)
    // ⚠️ Centrata in altezza come la fila della pillola a scomparsa (vedi [SlidePill]): qui i
    // tasti hanno tutti la stessa misura, ma uno di un'altra misura finirebbe in alto.
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.pillFill(backdrop)) {
        entries.forEachIndexed { i, entry ->
            val jump = when {
                !folds -> 0
                i == inner -> -1
                i == outer -> 1
                else -> 0
            }
            if (jump == 0) {
                if (q < 1f || !folds) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(cell * (if (folds) 1f - q else 1f))
                            .graphicsLayer { alpha = if (folds) 1f - q else 1f }
                    ) {
                        PillKey(entry = entry, size = key, enabled = !armed)
                    }
                }
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.width(lerp(cell, key, q))) {
                    JumpKey(entry, jump, q, armed, key, top, bottom, onJump)
                }
            }
        }
    }
}

/** Da quante voci una pillola occupa tutta la fila: sette. */
internal const val PILL_FULL = 7

/**
 * Un tasto che diventa un salto mentre la schermata scorre: [entry] a riposo, `↑` ([jump] `-1`) o
 * `↓` ([jump] `1`) quando [armed], con la stessa dissolvenza della freccia guidata da [q].
 *
 * Scritto una volta sola: la pillola estesa, il Menu basso e la sua pillola chiusa trasformano
 * due tasti nel salto con lo stesso meccanismo. ⚠️ Il tocco salta solo a salto armato: a metà
 * strada il tasto è ancora la voce, e fa quello che mostra.
 */
@Composable
internal fun JumpKey(
    entry: PillEntry,
    jump: Int,
    q: Float,
    armed: Boolean,
    size: Dp,
    top: String,
    bottom: String,
    onJump: (Int) -> Unit,
    holdLabel: String? = null,
    rest: (@Composable () -> Unit)? = null
) {
    val shown = if (armed) {
        entry.copy(label = if (jump < 0) top else bottom, onHold = null, onTap = { onJump(jump) })
    } else {
        entry
    }
    PillKey(
        entry = shown,
        size = size,
        holdLabel = if (armed) null else holdLabel,
        glyph = {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = (1f - q).pow(JUMP_FULL)
                        // ⚠️ Senza un buffer: vedi [MARK_FADE].
                        compositingStrategy = MARK_FADE
                        val s = 1f - JUMP_ZOOM * q
                        scaleX = s
                        scaleY = s
                    }
                ) {
                    if (rest != null) rest()
                    else Icon(entry.icon, contentDescription = null, modifier = Modifier.size(PILL_GLYPH))
                }
                if (q > 0f) {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = q.pow(JUMP_FULL)
                            val s = 1f - JUMP_ZOOM * (1f - q)
                            scaleX = s
                            scaleY = s
                        }
                    ) {
                        Icon(
                            imageVector = if (jump < 0) Glyphs.BrowseTop else Glyphs.BrowseBottom,
                            contentDescription = null,
                            modifier = Modifier.size(PILL_GLYPH)
                        )
                    }
                }
            }
        }
    )
}

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
 * The pill in place of the FAB on a phone held upright, and how every pill is filled.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️⚠️ Since 4.00 (the user's request of 2026-10-05, answers A1-D1 the same night): a setting
 * in 'Pulsanti e indicatori' with two rows of chips. The first says what stands in for the FAB
 * ([PhonePill]): nothing (the FAB), a round key that stretches into the pill ([PhonePill.SLIDE]),
 * or the pill always open ([PhonePill.EXTENDED]). The second says what the pill is made of
 * ([PillFill]), and it fills the pills of the wide screens too.
 * ⚠️ The pill lies at the bottom and is anchored to the preferred side, where the FAB was: the
 * corner key is the one under the thumb, and both modes keep it there.
 * ⚠️ A pill key has no label by definition: a long press shows it in a tooltip, and nothing else
 * (answer B1). The long press of 'Mostra nascoste' therefore does not exist in a pill.
 */

/**
 * What the pill is and what it is made of, as this device applies it.
 *
 * ⚠️ **The two values are the ones in force, not the stored ones**: [pillMode] turns the mode off
 * on a tablet and [pillFillIn] takes the glass away below Android 12. The screens read this and
 * never the settings, so they cannot disagree with each other.
 */
@Immutable
data class PillLook(
    val mode: PhonePill = PhonePill.OFF,
    val fill: PillFill = PillFill.SOLID,
    /**
     * Whether [mode] is drawn as the bottom menu ([MainControl.BOTTOM]) instead of a pill: then
     * [PhonePill.SLIDE] is the menu that slides in from below and [PhonePill.EXTENDED] the fixed one.
     */
    val bar: Boolean = false,
    /** Whether the main control is the corner menu ([MainControl.CORNER]): always [PhonePill.SLIDE]. */
    val corner: Boolean = false,
    /** How the translucent look is tuned. */
    val glass: GlassTune = GlassTune(),
    /**
     * Whether the corner menu rests as a single round key ([cornerRound] `true`) or as the vertical
     * pill of two keys. Only the corner menu reads it. See [Settings.cornerRound].
     */
    val cornerRound: Boolean = true
)

/**
 * How the translucent look is tuned: the sliders and the two colours that appear with it.
 *
 * ⚠️⚠️ **SINCE 4.20, HIS REQUEST** (note N1 on the 4.15 round: *se all'attivazione dell'effetto
 * traslucido si attivassero dei controlli tipo Luminosità, Colore, Raggio (della sfocatura),
 * Intensità (della sfocatura)? ... poi si possono disattivare spegnendo un feature flag*).
 * ⚠️⚠️ **SINCE 4.25 THE FACTORY VALUES ARE HIS** (note D on the 4.20 round: *Raggio 16dp, Intensità
 * 300%, Colore 20%, Luminosità -35 (chiaro) / +35 (scuro)*, and 'Scostamento' 35 in his answer
 * C2), and the glass is tied to the theme: [shift] darkens on the light theme and lightens on the
 * dark one, and each theme has its own colour (answer B3). Until 4.20 they were the DF's glass
 * (13dp, 160%, 100%, a signed 'Luminosità' at zero).
 *
 * @property radius the blur's radius, in dp.
 * @property intensity how vivid the colours behind get, in percent: 100 leaves them as they are.
 * @property tint how much of the colour the glass carries ('Opacità'), in percent of the theme's
 *   own ink.
 * @property shift how far the blurred picture moves away from the page ('Scostamento'), in percent:
 *   black on the light theme, white on the dark one.
 * @property lightColour the glass's colour on the light theme, as ARGB, or `null` for the accent in
 *   force; [darkColour] the same on the dark theme.
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
    /** The colour chosen for the theme in force, or `null` for the accent. */
    fun colourFor(light: Boolean): Int? = if (light) lightColour else darkColour

    /** This tune with the colour of one theme replaced: `null` goes back to the accent. */
    fun withColour(light: Boolean, colour: Int?): GlassTune =
        if (light) copy(lightColour = colour) else copy(darkColour = colour)

    companion object {
        const val RADIUS = 16
        const val INTENSITY = 300
        const val TINT = 20
        const val SHIFT = 35
        val RADIUS_RANGE = 0..40
        val INTENSITY_RANGE = 0..300
        val TINT_RANGE = 0..150
        val SHIFT_RANGE = 0..50
    }
}

/**
 * Whether the four sliders of [GlassTune] are shown: the 'feature flag' of his note N1.
 *
 * ⚠️ **Turning it off hides the sliders and keeps their values**: the style he reached stays the
 * one in force, and the factory values only matter to a phone that never moved them.
 */
const val GLASS_TUNING = true

/**
 * The pill as the activity sets it on stage, next to the other looks.
 *
 * ⚠️ **Read by windows that do not receive the settings** (the menus' veil reads [PillLook.fill]),
 * which is the reason it is a `CompositionLocal`, like `LocalAivDepth`.
 */
val LocalPillLook = staticCompositionLocalOf { PillLook() }

/** Whether this phone can draw the frosted glass: a `RenderEffect` needs Android 12. */
fun glassAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** The fill in force: the glass falls back to the factory fill where it cannot be drawn. */
fun pillFillIn(fill: PillFill): PillFill =
    if (fill == PillFill.GLASS && !glassAvailable()) PillFill.SOLID else fill

/**
 * The mode in force: the chosen pill on a phone whose main control is the pill or the bottom menu,
 * the FAB elsewhere. Whether it is drawn as the bottom menu says [barIn].
 *
 * ⚠️ **The smallest width and not the current one**: a phone held sideways is still a phone, and
 * the wide screens have their own pill; this only decides whether the item exists at all.
 */
fun pillMode(control: MainControl, mode: PhonePill, smallestWidthDp: Int): PhonePill = when {
    control == MainControl.FAB || !pillOffered(smallestWidthDp) -> PhonePill.OFF
    // ⚠️ The corner menu has no fixed form: it always folds into its pill.
    control == MainControl.CORNER -> PhonePill.SLIDE
    else -> mode
}

/** Whether the main control in force is the corner menu: only on a phone, like [pillMode]. */
fun cornerIn(control: MainControl, smallestWidthDp: Int): Boolean =
    control == MainControl.CORNER && pillOffered(smallestWidthDp)

/** Whether the main control in force is the bottom menu: only on a phone, like [pillMode]. */
fun barIn(control: MainControl, smallestWidthDp: Int): Boolean =
    control == MainControl.BOTTOM && pillOffered(smallestWidthDp)

/** Whether this device is a phone, which is where the setting exists at all. */
fun pillOffered(smallestWidthDp: Int): Boolean = smallestWidthDp < Adaptive.PHONE_MAX

/**
 * What lies behind a pill, recorded so that the glass can blur it.
 *
 * ⚠️⚠️ **A SIBLING OF THE PILL AND NEVER ITS PARENT**: the pill draws this layer, so a source
 * that contained the pill would draw itself inside itself. Each screen marks the content that
 * scrolls under the pill ([backdropSource]) and hands the same object to the pill.
 * ⚠️ **Only where the glass is in force** ([rememberBackdrop] answers `null` otherwise): recording
 * costs one more pass over the content's display list, and nobody else needs it.
 */
class Backdrop internal constructor(internal val layer: GraphicsLayer) {
    /**
     * Where the recorded content starts, in the root's coordinates.
     *
     * ⚠️ **A state, so the glass is drawn again when the content moves**: the pill reads it while
     * drawing, and a plain field would leave the blurred picture where it was.
     */
    internal var origin by mutableStateOf(Offset.Zero)
}

/** The recording of what lies behind the pill, or `null` where the glass is not in force. */
@Composable
fun rememberBackdrop(): Backdrop? {
    val layer = rememberGraphicsLayer()
    val glass = LocalPillLook.current.fill == PillFill.GLASS
    return remember(layer, glass) { if (glass) Backdrop(layer) else null }
}

/** Marks the content a pill may sit on: it is drawn as before, and recorded for the glass. */
fun Modifier.backdropSource(backdrop: Backdrop?): Modifier =
    if (backdrop == null) this else this
        .onGloballyPositioned { backdrop.origin = it.positionInRoot() }
        .drawWithContent {
            backdrop.layer.record { this@drawWithContent.drawContent() }
            drawLayer(backdrop.layer)
        }

/**
 * How much the glass blurs behind the menus' veil: the glass's factory radius.
 *
 * ⚠️ **His 16dp since 4.25** (note D on the 4.20 round). Until 4.20 it was the DF pill's
 * `blur(8px)` turned into a radius, 13: Android turns a radius into a standard deviation as
 * `0.57735 * r + 0.5`, so 8 of the CSS deviation wanted 13 of radius.
 * ⚠️ **The factory value and not the slider's**: the veil is the menus' and is drawn by windows
 * that do not read the tune.
 */
internal val GLASS_BLUR = GlassTune.RADIUS.dp


/**
 * How much accent the glass carries.
 *
 * ⚠️⚠️ **SINCE 4.01, LESS TRANSPARENT THAN THE DF** (the user's answer, 2026-10-05, after the
 * variants page: *accento dell'altro tema. In più, disegnalo un po' meno trasparente*). In 4.00
 * they were the DF's `--pill-fill`, 70% and 25%, and on the dark theme the colour nearly vanished;
 * the variant he picked (A2) had 70% and 55%, and 'a bit less transparent' took both up.
 */
private const val GLASS_INK_LIGHT = 0.80f
private const val GLASS_INK_DARK = 0.65f

/** The accent's opacity in [PillFill.TRANSLUCENT]: 80%, the user's number. */
private const val TRANSLUCENT_INK = 0.80f

/**
 * Fills the pill's surface: the accent, the accent at 80%, or frosted glass.
 *
 * ⚠️ **It is a modifier and not a `Surface` colour** because the glass draws something a colour
 * cannot: what lies behind, blurred. The three cases go through here so that the FAB-like key of
 * [PhonePill.SLIDE], the pill and the pills of the wide screens are filled in one way.
 * ⚠️ **Without a [backdrop] the glass is the accent over what the window shows behind it**, which
 * is the case of the wide home, where the pill sits on an empty background.
 */
@Composable
fun Modifier.pillFill(backdrop: Backdrop?): Modifier =
    buttonFill(backdrop, pillAccent(), RoundedCornerShape(50))

/**
 * Fills a main button's surface in the fill in force: [colour] as it is, at 80%, or frosted glass
 * tinted with it.
 *
 * ⚠️⚠️ **SINCE 4.10 THE FAB GOES THROUGH HERE TOO** (the user's note E on the 4.04 round: *'Aspetto
 * dei pulsanti principali' ... vale anche per il FAB*), with its own colour and its own shape: the
 * pills and the FAB are filled in one way, and only these two values tell them apart.
 * ⚠️ **The glass keeps the pill's two inks**, [GLASS_INK_LIGHT] and [GLASS_INK_DARK], applied to
 * [colour]'s own alpha, so a FAB fading to the other accent while pressed stays glass all along.
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
            // ⚠️ A colour of his replaces [colour] and keeps its alpha, so the FAB's press still
            // shows (answer B3: *due colori memorizzabili*).
            val base = tune.colourFor(light)?.let { Color(it).copy(alpha = colour.alpha) } ?: colour
            val tint = base.copy(alpha = (base.alpha * ink).coerceIn(0f, 1f))
            val radiusDp = tune.radius.dp
            val saturation = tune.intensity / 100f
            /*
             * ⚠️⚠️ **'SCOSTAMENTO' HAS NO SIGN, SINCE 4.25, AND IT IS HIS CHOICE** (answer C2): it
             * moves the glass away from the page, so it darkens on the light theme and lightens on
             * the dark one. In 4.20 the slider was signed, and the same value looked opposite on
             * the two themes.
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
                    if (backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        /*
                         * ⚠️ **The recording is wider than the pill by twice the radius**, and
                         * drawn back shifted by the same amount: the blur reads the neighbours of
                         * every pixel, and at the pill's edge those would be transparent, which
                         * draws a dark rim the DF's glass does not have.
                         */
                        val edge = (radiusDp.toPx() * 2).roundToInt()
                        blurred.renderEffect = glassEffect(radiusDp.toPx(), saturation)
                        blurred.topLeft = IntOffset(-edge, -edge)
                        blurred.record(
                            size = IntSize(size.width.roundToInt() + edge * 2, size.height.roundToInt() + edge * 2)
                        ) {
                            translate(backdrop.origin.x - me.x + edge, backdrop.origin.y - me.y + edge) {
                                drawLayer(backdrop.layer)
                            }
                        }
                        /*
                         * ⚠️⚠️ **THE PAGE'S GROUND GOES UNDER THE BLURRED COPY, SINCE 4.30, AND IT
                         * IS MEASURED ON HIS SCREENSHOTS** (note E on the 4.25 round: *non sfoca gli
                         * elementi di UI creati dall'app stessa: testi e bordi delle miniature
                         * rimangono sempre nitidi sotto il 'vetro'*). The recorded content has no
                         * ground of its own (the page's colour is the theme's `Surface`, outside
                         * it), so text and edges drawn on nothing blurred into a faint halo, and the
                         * sharp originals showed through it. The pictures are opaque, which is why
                         * they looked blurred and the rest did not.
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

/** The blur plus the saturation, in that order, as the DF's `backdrop-filter` writes them. */
@RequiresApi(Build.VERSION_CODES.S)
private fun glassEffect(radius: Float, saturation: Float): androidx.compose.ui.graphics.RenderEffect {
    // ⚠️ A radius of zero is no blur at all, and `createBlurEffect` refuses it.
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
 * The pill's colour: the accent of the OTHER theme, in every fill.
 *
 * ⚠️⚠️ **SINCE 4.01, THE USER'S CHOICE** (*accento dell'altro tema*, variants A2 and C1): it is the
 * pair the FAB already wears (`aivLauncher`), the teal on the dark theme and the petrol on the
 * light one, which stand out more from the page than the theme's own accent did. In 4.00 the pill
 * wore the theme's accent.
 * ⚠️ **The theme is the app's and not the system's** ([LocalAivLight]), for the reason written
 * on the FAB's colours.
 */
@Composable
fun pillAccent(): Color = aivAccent(!LocalAivLight.current)

/**
 * The ink of the pill's glyphs: the ink that goes with [pillAccent], in every fill, unless the glass
 * wears a colour of his ([ownGlassInk]).
 */
@Composable
fun pillInk(): Color = ownGlassInk() ?: aivOnAccent(!LocalAivLight.current)

/**
 * The ink of the glyphs on a glass that wears a colour of his, or `null` where it does not.
 *
 * ⚠️⚠️ **SINCE 4.34, HIS ANSWER A1** (to `colore-icone-vetro`, on `4.30-04`: *l'app sceglie da sé
 * chiaro o scuro, in base al contrasto col colore che hai scelto per il vetro*): until 4.33 the
 * glyphs kept the accent's ink, which on a light colour of his could vanish.
 * ⚠️ **The two inks are the FAB's on the glass** ([FAB_GLASS_INK_LIGHT], [FAB_GLASS_INK_DARK]), his
 * two values: the choice is between them, by the contrast with the colour as he chose it, before
 * the glass dilutes it over what lies behind.
 */
@Composable
internal fun ownGlassInk(): Color? {
    val look = LocalPillLook.current
    if (look.fill != PillFill.GLASS) return null
    val own = look.glass.colourFor(LocalAivLight.current) ?: return null
    return inkOn(Color(own))
}

/** Of the glass's two inks, the one with more contrast on [colour]. */
internal fun inkOn(colour: Color): Color {
    val ground = colour.copy(alpha = 1f).luminance()
    fun contrast(ink: Color): Float {
        val l = ink.luminance()
        return (maxOf(l, ground) + 0.05f) / (minOf(l, ground) + 0.05f)
    }
    return if (contrast(FAB_GLASS_INK_LIGHT) >= contrast(FAB_GLASS_INK_DARK)) FAB_GLASS_INK_LIGHT else FAB_GLASS_INK_DARK
}

/**
 * How long the pill takes to open or fold: at most 160 ms, the user's ceiling.
 *
 * ⚠️ **It starts very fast and slows down organically** (*un'animazione che parte velocissima,
 * rallenta organicamente*): the curve is the exponential ease-out, which covers most of the way
 * in the first third and spends the rest arriving.
 */
private const val PILL_OPEN_MS = 160

/** The exponential ease-out: `1 - 2^(-10x)`, pinned to 1 at the end. */
internal val PILL_EASE = Easing { x -> if (x >= 1f) 1f else 1f - 2f.pow(-10f * x) }

/**
 * How far the open sliding pill's two end keys keep from its ends, when the row is full.
 *
 * ⚠️ **Since 4.34, his note** (note A on the 4.33 round: *vanno spostate di qualche dp verso
 * l'interno e le altre icone spaziate di conseguenza*): 'qualche dp' is his, the number a first
 * reading of it.
 */
private val SLIDE_INSET = 6.dp

/** How much ink the × keeps: *semitrasparente*, the user's word. */
private const val CLOSE_INK = 0.6f

/** How much the symbol that leaves shrinks, and the one that arrives grows: the FAB's 0.35. */
private const val SWAP_ZOOM = 0.35f

/**
 * The pill of a phone held upright, in the mode the setting asks for.
 *
 * ⚠️ **The caller places it where the FAB would be** (side, insets, margins): the corner key sits
 * exactly where the FAB sat, which is the point of the two modes.
 *
 * @param entries the FAB's menu, as the wide pill reads it.
 * @param arm the jump, or `null` where the screen has none.
 * @param nested the screen's nested scroll, which the jump goes through like a finger.
 * @param fabLabel what the round key does now, for screen readers ([PhonePill.SLIDE]).
 * @param holdLabel what its long press does, and [onHold] the gesture itself; `null` where the
 *   FAB has none. Only the round key has it (answer C1): with the pill open there is no FAB.
 * @param fabGlyph what the round key shows at rest: the app's mark, or the chevron while armed.
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
     * Where the corner key sits, inside [modifier]: the insets and the margins the FAB had.
     *
     * ⚠️ **Apart from [modifier] since 4.15**: the bottom menu spans the whole width down to the
     * glass, and only its folded pill keeps the FAB's corner.
     */
    corner: Modifier = Modifier,
    /** The FAB's glyph without the jump's chevron, for the bottom menu's folded pill. */
    mark: @Composable (String?) -> Unit = {},
    /**
     * What the round key does at rest, without the jump: the bottom menu's folded pill says its own
     * jump ('in fondo', always), and [fabLabel] follows the FAB's chevron instead.
     */
    restLabel: String? = null,
    /**
     * The corner menu's cells, in the right-handed reading order, where they differ from [entries]:
     * the home's 3x3 keeps all three views and 'Mostra nascoste' in place (decisions C1 and C2).
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
                // ⚠️ In reading order for the right hand: the corner menu mirrors each row itself.
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
                mark = mark
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
 * The entries in the order a left-handed pill reads them: the mirror of the right-handed one.
 *
 * ⚠️⚠️ **SINCE 4.15, THE USER'S ORDER** (*quando il lato preferito è sinistra, l'ordine deve
 * cambiare: sarà (da sinistra a destra) Impostazioni, Cestino, Apri un indirizzo, Cerca, Mostra
 * nascoste, Altra vista 1, Altra vista 2*): the key under the thumb is 'Impostazioni' on both sides.
 * ⚠️ **Since 4.20 the two views are mirrored too** (his comment on `4.15-04`: *volevo specchiati
 * anche quei due*): in 4.15 they kept their order, read from his list. Until 4.10 a left-handed
 * pill kept the right-handed order, with the round key moved to the left.
 */
internal fun mirrored(entries: List<PillEntry>): List<PillEntry> = entries.asReversed()

/**
 * The size of a key when the pill has to fit [count] of them in [room].
 *
 * ⚠️ **Never larger than [PILL_KEY]**, and smaller only on a narrow phone with many entries: the
 * home has up to nine, and nine keys of 44 do not fit on a 360-wide screen.
 */
private fun keyFor(count: Int, room: Dp): Dp = min(PILL_KEY, room / count.coerceAtLeast(1))

/**
 * [PhonePill.SLIDE]: a round key that stretches into the pill and folds back on its ×.
 *
 * ⚠️⚠️ **THE ROUND KEY KEEPS THE FAB'S TWO GESTURES** (answer C1): while armed the tap jumps,
 * otherwise it opens the pill; the long press does what the FAB's did. Open, the corner key is
 * the × and its long press shows its label, like every other key.
 * ⚠️ **Back folds the pill**, as it would close the FAB's menu: an open pill is a menu.
 * ⚠️ **Opening is a remembered state that survives a rotation**, and a tap on an entry folds the
 * pill before acting, as a menu row closes its menu.
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
     * ⚠️⚠️ **THE ROUND KEY IS ALWAYS 44DP, SINCE 4.30, AND IT IS HIS NOTE** (B on the 4.25 round: *in
     * modalità pillola a scomparsa, addirittura cambia dimensione ... La dimensione corretta è
     * quella del tondo della pillola a scomparsa*): until 4.25 the whole pill used [keyFor], and in
     * the home eight keys on a narrow phone made every key, the round one too, a little smaller
     * than in a folder. Now only the open keys shrink, the round key and the pill's height do not.
     */
    /*
     * ⚠️⚠️ **OPEN WITH MANY ENTRIES IT SPANS THE ROW, SINCE 4.25, AND IT IS HIS CHOICE** (note A on
     * the 4.20 round, and A2 after the preview): eight keys of 44dp left the open pill short of
     * both the thumbnails' edge and its own margin. As in the extended pill ([PILL_FULL]) the cells
     * widen and the keys do not.
     * ⚠️ **The corner cell grows from the key to the cell while opening**, so at rest the round key
     * is exactly where it was.
     * ⚠️⚠️ **AND SINCE 4.34 THE ROW KEEPS [SLIDE_INSET] FROM BOTH ENDS** (note A on the 4.33 round:
     * *modalità lista (a un estremo) e × di chiusura (all'altro) sono troppo vicini ai margini*):
     * the two end keys move in, and the cells share what is left.
     */
    val wide = count >= PILL_FULL
    val inset = if (wide) SLIDE_INSET else 0.dp
    val key = keyFor(count, room - inset * 2)
    val cell = if (wide) (room - inset * 2) / count else key
    val full = cell * count + inset * 2
    val q = arm?.shown ?: 0f
    val armed = arm?.armed == true
    /*
     * ⚠️⚠️ **THE JUMP TAKES THE TWO KEYS NEAREST THE CORNER, × INCLUDED, SINCE 4.35** (item `4.34-04`:
     * *sono letteralmente i primi due tasti (incluso ×) che si devono trasformare in top/bottom*): the
     * × turns into `↓` and the key beside it into `↑`. In 4.34 they were the two keys beside the ×.
     * The key beside the ×, in reading order: on the right it is the last one.
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
                // ⚠️ Closed, the round key is the FAB: its long press is the FAB's and not a
                // tooltip. Open, it is a key of the pill like the others.
                holdLabel = if (open) null else holdLabel,
                glyph = {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier.graphicsLayer {
                                alpha = 1f - p
                                // ⚠️ Without a buffer: see [MARK_FADE].
                                compositingStrategy = MARK_FADE
                                val s = 1f - SWAP_ZOOM * p
                                scaleX = s
                                scaleY = s
                            }
                        ) {
                            // ⚠️ The round key is drawn here and not by [TapHoldFab]: without this
                            // the mark kept the square FAB's size and centre (4.02-03, not approved).
                            CompositionLocalProvider(LocalRoundKey provides true) { fabGlyph(null) }
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.graphicsLayer {
                                alpha = p
                                // ⚠️ Without a buffer: see [MARK_FADE].
                                compositingStrategy = MARK_FADE
                                val s = 1f - SWAP_ZOOM * (1f - p)
                                scaleX = s
                                scaleY = s
                            }
                        ) {
                            // Open, the × crossfades into `↓` with the jump, as the keys of
                            // [JumpKey] do (item `4.34-04`).
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
         * ⚠️ **The row leans on the preferred side, and it is measured**: each cell rounds its width
         * to the pixel, so eight cells can come out a few pixels short of the pill, and leaning on
         * the start left the × that much off the edge (3px on the bench, at density 1). On this side
         * the corner key is exact, and the shortfall goes to the far end.
         */
        contentAlignment = if (atEnd) Alignment.CenterEnd else Alignment.CenterStart,
        modifier = Modifier
            .width(lerp(PILL_KEY, full, p))
            .height(PILL_KEY)
            .pillFill(backdrop)
    ) {
        /*
         * ⚠️⚠️ **`wrapContentWidth` SENZA LIMITE E NON `requiredWidth`, ED È MISURATO**: una fila più
         * larga del suo riquadro, scritta con `requiredWidth`, si centra sul riquadro, cioè il
         * tasto d'angolo finiva mezza pillola fuori dallo schermo e il tocco non lo trovava (lo
         * ha visto il banco). Così la fila si allinea al lato preferito, e il tasto tondo resta
         * dov'era il FAB.
         */
        /*
         * ⚠️⚠️ **CENTRED IN HEIGHT, SINCE 4.36** (his note of 2026-10-06, with a screenshot: *l'unica
         * icona centrata verticalmente sulla pillola è la × ... tutte le altre appaiono molto più in
         * alto*): a `Row` puts its children at the top unless told otherwise, and with eight entries
         * on a narrow phone [keyFor] makes the open keys smaller than the pill, while the × keeps
         * [PILL_KEY]. So every key but the × sat at the top of the pill. `PillRowTest` measures it.
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
                // ⚠️ A key hidden behind the round one is not a command yet.
                val usable = entry.copy(enabled = entry.enabled && open, onTap = { open = false; entry.onTap() })
                /*
                 * ⚠️⚠️ **SINCE 4.34 THE OPEN PILL TURNS INTO THE JUMP WHILE SCROLLING** (note B on
                 * the 4.33 round: *devono diventare top/bottom mentre si scorre, e tornare alle loro
                 * funzioni una volta finito di scorrere, come accade nelle altre modalità*); until
                 * 4.33 it had no jump at all. Since 4.35 the key beside the × is `↑`, and the × is
                 * `↓` (see [outer]).
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
 * [PhonePill.EXTENDED]: the pill always open, which folds to the two jump keys while scrolling.
 *
 * ⚠️⚠️ **THE FOLD FOLLOWS THE JUMP'S OWN STATE** (answer C2: *con le stesse tempistiche e lo
 * stesso meccanismo*): the keys that leave shrink with [JumpArm.shown], the pixel the finger moves
 * is the pixel the pill folds, and it opens again when the chevron would go back to the mark. The
 * two keys at the corner turn into `↑` (inside) and `↓` (in the corner) with the chevron's own
 * crossfade, at the same time as the pill (*avvengono contemporaneamente*).
 * ⚠️ **Both directions at once**, unlike the FAB's single chevron: the pill has two keys to give.
 * ⚠️ **A tap jumps only while armed**: halfway through the fold the keys are still the entries,
 * and they do what they show.
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
     * ⚠️⚠️ **WITH THE MOST ENTRIES THE PILL SPANS THE WHOLE ROW, SINCE 4.15, AND IT IS HIS RULE**
     * (*se si raggiungono le 7 icone (il massimo), la pillola raggiunge a sinistra la stessa
     * distanza che la separa dal bordo a destra, aumentando leggermente la spaziatura in modo che
     * l'icona 'Cerca' sia esattamente al centro*): [room] is already the row between the two
     * margins, so seven equal cells put the fourth, 'Cerca', on the screen's centre line.
     * ⚠️ **Since 4.25 the open sliding pill spans the row too** (his choice A2), with its × among
     * the eight cells: in 4.15 it was *solo nella pillola estesa*.
     * ⚠️ **The cells widen, the keys do not**: the touch target and the round ripple stay the
     * key's, and the extra width is spacing.
     */
    val cell = if (count >= PILL_FULL) room / count else key
    // ⚠️ The two keys at the corner, in reading order: on the right they are the last two.
    val inner = if (atEnd) count - 2 else 1
    val outer = if (atEnd) count - 1 else 0
    val folds = count >= 2
    val top = stringResource(R.string.jump_top)
    val bottom = stringResource(R.string.jump_bottom)
    // ⚠️ Centred in height like the sliding pill's row (see [SlidePill]): here every key has the
    // same size, so they agree among themselves, but a key of another size would sit at the top.
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

/** From how many entries the extended pill spans the whole row: seven, the most there are. */
internal const val PILL_FULL = 7

/**
 * A key that turns into a jump while the screen scrolls: [entry] at rest, `↑` ([jump] `-1`) or
 * `↓` ([jump] `1`) while [armed], with the chevron's own crossfade driven by [q].
 *
 * ⚠️ **Written once since 4.15**: the extended pill, the bottom menu and its folded pill all turn
 * two of their keys into the jump, and with the same mechanism (*con il solito meccanismo*).
 * ⚠️ **A tap jumps only while armed**: halfway through, the key is still the entry, and it does
 * what it shows.
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
                        // ⚠️ Without a buffer: see [MARK_FADE].
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

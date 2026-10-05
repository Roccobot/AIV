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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
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
    val fill: PillFill = PillFill.SOLID
)

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
 * The mode in force: the setting on a phone with the switch on, the FAB everywhere else.
 *
 * ⚠️ **The smallest width and not the current one**: a phone held sideways is still a phone, and
 * the wide screens have their own pill; this only decides whether the item exists at all.
 */
fun pillMode(on: Boolean, mode: PhonePill, smallestWidthDp: Int): PhonePill =
    if (on && pillOffered(smallestWidthDp)) mode else PhonePill.OFF

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
 * How much the glass blurs: the DF pill's `blur(8px)` turned into a radius.
 *
 * ⚠️⚠️ **THE CSS NUMBER IS A STANDARD DEVIATION, THE ANDROID ONE A RADIUS**: Android turns a
 * radius into a deviation as `0.57735 * r + 0.5` (the same conversion in `RenderEffect` and in
 * the compositor's window blur), so 8 of deviation want 13 of radius. Writing 8 would have given
 * a glass visibly clearer than the DF's.
 * ⚠️ **In dp, like the CSS pixel**: the DF's 8px are 8 device-independent pixels.
 */
internal val GLASS_BLUR = 13.dp

/** The DF pill's `saturate(160%)`: the colours behind the glass get 60% more vivid. */
private const val GLASS_SATURATION = 1.6f

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
fun Modifier.pillFill(backdrop: Backdrop?): Modifier {
    val fill = LocalPillLook.current.fill
    val light = LocalAivLight.current
    val accent = pillAccent()
    val shape = RoundedCornerShape(50)
    return when (fill) {
        PillFill.SOLID -> this.clip(shape).drawBehind { drawRect(accent) }
        PillFill.TRANSLUCENT -> this.clip(shape).drawBehind {
            drawRect(accent.copy(alpha = TRANSLUCENT_INK))
        }
        PillFill.GLASS -> {
            val tint = accent.copy(alpha = if (light) GLASS_INK_LIGHT else GLASS_INK_DARK)
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
                        val edge = (GLASS_BLUR.toPx() * 2).roundToInt()
                        blurred.renderEffect = glassEffect(GLASS_BLUR.toPx())
                        blurred.topLeft = IntOffset(-edge, -edge)
                        blurred.record(
                            size = IntSize(size.width.roundToInt() + edge * 2, size.height.roundToInt() + edge * 2)
                        ) {
                            translate(backdrop.origin.x - me.x + edge, backdrop.origin.y - me.y + edge) {
                                drawLayer(backdrop.layer)
                            }
                        }
                        clipPath(path) { drawLayer(blurred) }
                    }
                    drawOutline(outline, tint)
                }
                .clip(shape)
        }
    }
}

/** The blur plus the saturation, in that order, as the DF's `backdrop-filter` writes them. */
@RequiresApi(Build.VERSION_CODES.S)
private fun glassEffect(radius: Float) = RenderEffect.createColorFilterEffect(
    ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(GLASS_SATURATION) }),
    RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
).asComposeRenderEffect()

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

/** The ink of the pill's glyphs: the ink that goes with [pillAccent], in every fill. */
@Composable
fun pillInk(): Color = aivOnAccent(!LocalAivLight.current)

/**
 * How long the pill takes to open or fold: at most 160 ms, the user's ceiling.
 *
 * ⚠️ **It starts very fast and slows down organically** (*un'animazione che parte velocissima,
 * rallenta organicamente*): the curve is the exponential ease-out, which covers most of the way
 * in the first third and spends the rest arriving.
 */
private const val PILL_OPEN_MS = 160

/** The exponential ease-out: `1 - 2^(-10x)`, pinned to 1 at the end. */
private val PILL_EASE = Easing { x -> if (x >= 1f) 1f else 1f - 2f.pow(-10f * x) }

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
    fabGlyph: @Composable (String?) -> Unit
) {
    if (entries.isEmpty()) return
    val atEnd = LocalPadLook.current.hand == Hand.RIGHT
    val mode = LocalPillLook.current.mode
    val scope = rememberCoroutineScope()
    BoxWithConstraints(modifier = modifier.declaresFoot()) {
        CompositionLocalProvider(LocalContentColor provides pillInk()) {
            when (mode) {
                PhonePill.SLIDE -> SlidePill(
                    entries, arm, fabLabel, holdLabel, onHold, backdrop, atEnd, maxWidth,
                    onJump = { scope.launch { arm?.leap(nested) } },
                    fabGlyph = fabGlyph
                )
                else -> ExtendedPill(
                    entries, arm, backdrop, atEnd, maxWidth,
                    onJump = { toward -> scope.launch { arm?.leapToward(toward, nested) } }
                )
            }
        }
    }
}

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
    val key = keyFor(count, room)
    val full = key * count
    val closeLabel = stringResource(R.string.pick_close)
    val corner = PillEntry(
        icon = Icons.Default.Close,
        label = if (open) closeLabel else fabLabel,
        onHold = if (open) null else onHold,
        onTap = {
            when {
                open -> open = false
                arm?.armed == true -> onJump()
                else -> open = true
            }
        }
    )
    val cornerKey = @Composable {
        PillKey(
            entry = corner,
            size = key,
            // ⚠️ Closed, the round key is the FAB: its long press is the FAB's and not a
            // tooltip. Open, it is a key of the pill like the others.
            holdLabel = if (open) null else holdLabel,
            glyph = {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = 1f - p
                            val s = 1f - SWAP_ZOOM * p
                            scaleX = s
                            scaleY = s
                        }
                    ) { fabGlyph(null) }
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = p * CLOSE_INK
                            val s = 1f - SWAP_ZOOM * (1f - p)
                            scaleX = s
                            scaleY = s
                        }
                    ) { Icon(Icons.Default.Close, contentDescription = null) }
                }
            }
        )
    }
    Box(
        modifier = Modifier
            .width(lerp(key, full, p))
            .height(key)
            .pillFill(backdrop)
    ) {
        /*
         * ⚠️⚠️ **`wrapContentWidth` SENZA LIMITE E NON `requiredWidth`, ED È MISURATO**: una fila più
         * larga del suo riquadro, scritta con `requiredWidth`, si centra sul riquadro, cioè il
         * tasto d'angolo finiva mezza pillola fuori dallo schermo e il tocco non lo trovava (lo
         * ha visto il banco). Così la fila si allinea al lato preferito, e il tasto tondo resta
         * dov'era il FAB.
         */
        Row(
            modifier = Modifier.wrapContentWidth(
                align = if (atEnd) Alignment.End else Alignment.Start,
                unbounded = true
            )
        ) {
            if (!atEnd) cornerKey()
            entries.forEach { entry ->
                PillKey(
                    entry = entry.copy(onTap = { open = false; entry.onTap() }),
                    size = key,
                    // ⚠️ A key hidden behind the round one is not a command yet.
                    enabled = open,
                    modifier = Modifier.graphicsLayer { alpha = p }
                )
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
    // ⚠️ The two keys at the corner, in reading order: on the right they are the last two.
    val inner = if (atEnd) count - 2 else 1
    val outer = if (atEnd) count - 1 else 0
    val folds = count >= 2
    val top = stringResource(R.string.jump_top)
    val bottom = stringResource(R.string.jump_bottom)
    Row(modifier = Modifier.pillFill(backdrop)) {
        entries.forEachIndexed { i, entry ->
            val jump = when {
                !folds -> 0
                i == inner -> -1
                i == outer -> 1
                else -> 0
            }
            if (jump == 0) {
                if (q < 1f || !folds) {
                    PillKey(
                        entry = entry,
                        size = key,
                        enabled = !armed,
                        modifier = Modifier
                            .width(key * (if (folds) 1f - q else 1f))
                            .graphicsLayer { alpha = if (folds) 1f - q else 1f }
                    )
                }
            } else {
                val shown = if (armed) {
                    entry.copy(
                        label = if (jump < 0) top else bottom,
                        onHold = null,
                        onTap = { onJump(jump) }
                    )
                } else {
                    entry
                }
                PillKey(
                    entry = shown,
                    size = key,
                    glyph = {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    alpha = (1f - q).pow(JUMP_FULL)
                                    val s = 1f - JUMP_ZOOM * q
                                    scaleX = s
                                    scaleY = s
                                }
                            ) { Icon(entry.icon, contentDescription = null, modifier = Modifier.size(PILL_GLYPH)) }
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
        }
    }
}

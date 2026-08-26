package io.github.roccobot.aiv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** How far in one can go, as a multiple of the real pixels. */
private const val MAX_SCALE = 40f

/** Below this, a picture is a speck: it is the floor of the pinch, not of the fit. */
private const val MIN_SCALE = 0.02f

/** Side of one checkerboard square, in dp. */
private val CHECKER = 12.dp

@Composable
fun ViewerScreen(state: ViewerState, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (state) {
            is ViewerState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            is ViewerState.Error -> ErrorMessage(state, Modifier.align(Alignment.Center))
            is ViewerState.Ready -> ImageCanvas(state.image)
        }
    }
}

@Composable
private fun ErrorMessage(state: ViewerState.Error, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(state.messageRes),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        state.detail?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ImageCanvas(image: LoadedImage) {
    val density = LocalDensity.current
    val checkerPx = with(density) { CHECKER.toPx() }
    val light = MaterialTheme.colorScheme.background.luminanceIsLight()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .drawBehind { drawChecker(size, checkerPx, light) }
    ) {
        val viewWidth = constraints.maxWidth.toFloat()
        val viewHeight = constraints.maxHeight.toFloat()
        val imageWidth = image.bitmap.width.toFloat()
        val imageHeight = image.bitmap.height.toFloat()

        // 'Fit' means: shown whole. Enlarging a small picture to fill the screen
        // is a separate wish, and it is not the default here for the same reason
        // it is not in the userscript: blowing up a 64px icon helps nobody.
        val fitScale = min(viewWidth / imageWidth, viewHeight / imageHeight)
        val restScale = min(fitScale, 1f)

        var scale by remember(image) { mutableFloatStateOf(restScale) }
        var offset by remember(image) { mutableStateOf(Offset.Zero) }
        var panelVisible by remember(image) { mutableStateOf(true) }
        val scope = rememberCoroutineScope()

        fun clampOffset(candidate: Offset, atScale: Float): Offset {
            val slackX = max(0f, (imageWidth * atScale - viewWidth) / 2f)
            val slackY = max(0f, (imageHeight * atScale - viewHeight) / 2f)
            return Offset(
                candidate.x.coerceIn(-slackX, slackX),
                candidate.y.coerceIn(-slackY, slackY)
            )
        }

        fun animateTo(target: Float) {
            scope.launch {
                val from = scale
                val animation = Animatable(from)
                animation.animateTo(target) {
                    scale = value
                    offset = clampOffset(offset * (value / from), value)
                }
            }
        }

        Image(
            bitmap = image.bitmap,
            contentDescription = image.displayName,
            modifier = Modifier
                .align(Alignment.Center)
                .size(with(density) { imageWidth.toDp() }, with(density) { imageHeight.toDp() })
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(image) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val next = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                        // Keep the point under the fingers still: the centroid is
                        // measured from the centre, because that is where the
                        // image is anchored.
                        val fromCentre = centroid - Offset(size.width / 2f, size.height / 2f)
                        val corrected = fromCentre - (fromCentre - offset) * (next / scale)
                        scale = next
                        offset = clampOffset(corrected + pan, next)
                    }
                }
                .pointerInput(image) {
                    detectTapGestures(
                        onTap = { panelVisible = !panelVisible },
                        onDoubleTap = {
                            // Two states only, as on the desktop viewer: whole, or
                            // one image pixel per screen pixel.
                            val target = if (abs(scale - restScale) < 0.01f) 1f else restScale
                            animateTo(target)
                        }
                    )
                }
        )

        AnimatedVisibility(
            visible = panelVisible,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            DetailsPanel(image = image, scale = scale)
        }
    }
}

@Composable
private fun DetailsPanel(image: LoadedImage, scale: Float) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = buildString {
                    append(image.mimeType?.substringAfter('/')?.uppercase() ?: "?")
                    append("  ")
                    append(image.pixelWidth).append(" x ").append(image.pixelHeight)
                    image.byteSize?.let { append("  ").append(formatBytes(it)) }
                    append("  ").append((scale * 100).roundToInt()).append('%')
                    if (image.sampled) append("  (sampled)")
                },
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * The checkerboard is what makes transparency visible, and on a viewer that is
 * information rather than decoration. Its two greys follow the theme, so a PNG
 * with holes reads the same way in daylight and at night.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawChecker(
    size: Size,
    square: Float,
    light: Boolean
) {
    val a = if (light) Color(0xFFDDDDDD) else Color(0xFF2A2A2A)
    val b = if (light) Color(0xFFEEEEEE) else Color(0xFF333333)
    drawRect(color = b, size = size)
    var y = 0f
    var row = 0
    while (y < size.height) {
        var x = if (row % 2 == 0) 0f else square
        while (x < size.width) {
            drawRect(
                color = a,
                topLeft = Offset(x, y),
                size = Size(min(square, size.width - x), min(square, size.height - y))
            )
            x += square * 2
        }
        y += square
        row++
    }
}

private fun Color.luminanceIsLight(): Boolean = (0.2126f * red + 0.7152f * green + 0.0722f * blue) > 0.5f

private fun formatBytes(value: Long): String = when {
    value < 1024 -> "$value B"
    value < 1024 * 1024 -> String.format("%.1f kB", value / 1024f)
    else -> String.format("%.2f MB", value / (1024f * 1024f))
}

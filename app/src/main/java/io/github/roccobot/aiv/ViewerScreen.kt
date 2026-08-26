package io.github.roccobot.aiv

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Below this, a picture is a speck: it is the floor of the pinch, not of the fit. */
private const val MIN_SCALE = 0.02f

/** Side of one checkerboard square, in dp. */
private val CHECKER = 12.dp

@Composable
fun ViewerScreen(
    state: ViewerState,
    settings: Settings,
    source: Uri?,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (state) {
            is ViewerState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            is ViewerState.Error -> ErrorMessage(state, Modifier.align(Alignment.Center))
            is ViewerState.Ready -> ImageCanvas(state.image, settings, source, onHome)
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
private fun ImageCanvas(
    image: LoadedImage,
    settings: Settings,
    source: Uri?,
    onHome: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val checkerPx = with(density) { CHECKER.toPx() }
    val lightGreys = when (settings.bgTheme) {
        BgTheme.LIGHT -> true
        BgTheme.DARK -> false
        BgTheme.AUTO -> MaterialTheme.colorScheme.background.luminanceIsLight()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .drawBehind { drawBackground(size, checkerPx, lightGreys, settings.bgType) }
    ) {
        val viewWidth = constraints.maxWidth.toFloat()
        val viewHeight = constraints.maxHeight.toFloat()
        val imageWidth = image.bitmap.width.toFloat()
        val imageHeight = image.bitmap.height.toFloat()

        /**
         * What '100%' is on THIS screen. Compose lays the picture out in device
         * pixels, so a scale of 1 already means one pixel of the file per pixel of
         * the screen: that is [ScaleMode.PHYSICAL]. Asking instead for one pixel
         * per LAYOUT pixel means scaling by the screen's density, which on a phone
         * is two or three.
         */
        val oneToOne = if (settings.scaleMode == ScaleMode.PHYSICAL) 1f else density.density

        val fitScale = min(viewWidth / imageWidth, viewHeight / imageHeight)
        // 'Fit' means shown whole. Growing a small picture to fill the screen is a
        // separate wish, and it is a setting rather than the default for the same
        // reason it is in the userscript: blowing up a 64px icon helps nobody.
        val restScale = if (settings.fitGrow) fitScale else min(fitScale, oneToOne)

        var scale by remember(image, settings) { mutableFloatStateOf(restScale) }
        var offset by remember(image, settings) { mutableStateOf(Offset.Zero) }
        var panelVisible by remember(image, settings) { mutableStateOf(settings.infoVisible) }
        var menuAt by remember(image) { mutableStateOf<Offset?>(null) }
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
                .pointerInput(image, settings) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val next = (scale * zoom).coerceIn(MIN_SCALE, settings.zoomMax)
                        // Keep the point under the fingers still: the centroid is
                        // measured from the centre, because that is where the
                        // image is anchored.
                        val fromCentre = centroid - Offset(size.width / 2f, size.height / 2f)
                        val corrected = fromCentre - (fromCentre - offset) * (next / scale)
                        scale = next
                        offset = clampOffset(corrected + pan, next)
                    }
                }
                .pointerInput(image, settings) {
                    detectTapGestures(
                        onTap = { panelVisible = !panelVisible },
                        onLongPress = { menuAt = it },
                        onDoubleTap = {
                            // Two states only, as on the desktop viewer: whole, or
                            // one pixel of the file per pixel of the screen.
                            val target = if (abs(scale - restScale) < 0.01f) oneToOne else restScale
                            animateTo(target)
                        }
                    )
                }
        )

        menuAt?.let { at ->
            // The menu is anchored where the finger was, like the right click menu
            // it comes from. An empty Box at that point is the anchor: a
            // DropdownMenu positions itself against its parent, and it flips on its
            // own when there is no room below.
            Box(modifier = Modifier.offset { IntOffset(at.x.roundToInt(), at.y.roundToInt()) }) {
                ImageMenu(
                    image = image,
                    source = source,
                    settings = settings,
                    onDismiss = { menuAt = null },
                    onZoom = { animateTo(it) },
                    oneToOne = oneToOne,
                    restScale = restScale,
                    onToggleDetails = { panelVisible = !panelVisible },
                    onHome = onHome
                )
            }
        }

        AnimatedVisibility(
            visible = panelVisible,
            modifier = Modifier.align(
                if (settings.infoPosition == InfoPosition.TOP) Alignment.TopCenter
                else Alignment.BottomCenter
            )
        ) {
            DetailsPanel(image = image, percent = scale / oneToOne)
        }
    }
}

/**
 * The menu a long press opens: the same entries as the userscript's right click
 * menu, in the same order, with the two an Android app can add on top.
 *
 * ⚠️ 'Copy address' and the direct form of 'Image search' are shown only for a
 * picture that came from the web, and hiding them is deliberate: the address of a
 * `content://` handed over by another app means nothing to anyone else, and an
 * entry that copies a useless string is worse than a shorter menu.
 */
@Composable
private fun ImageMenu(
    image: LoadedImage,
    source: Uri?,
    settings: Settings,
    onDismiss: () -> Unit,
    onZoom: (Float) -> Unit,
    oneToOne: Float,
    restScale: Float,
    onToggleDetails: () -> Unit,
    onHome: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val remote = source?.scheme?.lowercase() in setOf("http", "https")

    fun say(res: Int) = Toast.makeText(context, res, Toast.LENGTH_SHORT).show()

    // The system's own file picker, so saving needs no permission at all and the
    // person chooses where it lands. On Android 9, which this app still supports,
    // writing into the gallery through MediaStore would have wanted
    // WRITE_EXTERNAL_STORAGE.
    val saver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(image.mimeType ?: "image/*")
    ) { target ->
        if (target == null || source == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = context.contentResolver.openOutputStream(target)?.use { out ->
                ImageActions.copyOriginalTo(context, source, out)
            } ?: false
            say(if (ok) R.string.toast_saved else R.string.toast_save_failed)
        }
    }

    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_copy_image)) },
            onClick = {
                onDismiss()
                say(
                    if (ImageActions.copyImage(context, image)) R.string.toast_image_copied
                    else R.string.toast_copy_failed
                )
            }
        )
        if (remote) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_copy_url)) },
                onClick = {
                    onDismiss()
                    ImageActions.copyText(context, source.toString())
                    say(R.string.toast_url_copied)
                }
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_search)) },
            onClick = {
                onDismiss()
                // False means the engine could not be handed the address, so the
                // picture was copied instead: said out loud, because an engine
                // opened on nothing looks like a defect of the app.
                if (!ImageActions.search(context, settings.searchEngine, image, source)) {
                    say(R.string.toast_local_search)
                }
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_share)) },
            onClick = {
                onDismiss()
                scope.launch {
                    if (!ImageActions.share(context, image, source)) say(R.string.toast_copy_failed)
                }
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_save)) },
            onClick = {
                onDismiss()
                saver.launch(ImageActions.fileName(image, source))
            }
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text(stringResource(R.string.fit_label)) },
            onClick = { onDismiss(); onZoom(restScale) }
        )
        listOf(1f, 2f, 4f).forEach { times ->
            DropdownMenuItem(
                text = { Text("${(times * 100).roundToInt()}%") },
                onClick = { onDismiss(); onZoom(oneToOne * times) }
            )
        }

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_details)) },
            onClick = { onDismiss(); onToggleDetails() }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_home)) },
            onClick = { onDismiss(); onHome() }
        )
    }
}

@Composable
private fun DetailsPanel(image: LoadedImage, percent: Float) {
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
                    append("  ").append((percent * 100).roundToInt()).append('%')
                    if (image.sampled) append("  (sampled)")
                },
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * The checkerboard is what makes transparency visible, and on a viewer that is
 * information rather than decoration. The flat colour is there for looking at a
 * photograph without a pattern under it, where the checkerboard says nothing
 * because there is nothing to see through.
 *
 * ⚠️ The four greys are the userscript's, not new ones: #DDD/#EEE light and
 * #333/#222 dark, and the flat colour takes the light of one pair and the dark of
 * the other. AIV had drifted to #2A2A2A for the dark pair, which is the kind of
 * difference nobody notices and nobody can justify later.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBackground(
    size: Size,
    square: Float,
    light: Boolean,
    type: BgType
) {
    val a = if (light) Color(0xFFDDDDDD) else Color(0xFF333333)
    val b = if (light) Color(0xFFEEEEEE) else Color(0xFF222222)
    if (type == BgType.SOLID) {
        // `b` in both themes, and that is the point: it is #EEE in the light pair
        // and #222 in the dark one, which is exactly 'the light of one and the dark
        // of the other'.
        drawRect(color = b, size = size)
        return
    }
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

/**
 * ⚠️ The locale is named, and it is `US` on purpose rather than the phone's: the
 * separator stays a point in both languages, which is the convention the
 * userscript settled on after trying the other way. Leaving it implicit is also
 * what lint flags here, and 'implicit' would have meant a comma on an Italian
 * phone and a point on an English one, for the same file.
 */
private fun formatBytes(value: Long): String = when {
    value < 1024 -> "$value B"
    value < 1024 * 1024 -> String.format(Locale.US, "%.1f kB", value / 1024f)
    else -> String.format(Locale.US, "%.2f MB", value / (1024f * 1024f))
}

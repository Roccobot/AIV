package io.github.roccobot.aiv

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 Expressive, and how it got here: in material3 1.4.0, the stable
 * release, MaterialExpressiveTheme and MotionScheme are declared INTERNAL and
 * the compiler refuses them outright. They are public only on the 1.5.0 line,
 * which is alpha, so material3 is pinned outside the Compose BOM.
 * The owner chose the alpha deliberately, this being an experimental project.
 * The cost is written down rather than forgotten: alpha APIs change between
 * releases, so a library bump can break this file. It is one file on purpose.
 *
 * Dynamic colour is used where the platform offers it (Android 12 and up), so
 * the app wears the wallpaper palette like the rest of the system; below that
 * there is a fixed pair of schemes built on the same teal the other Roccobot
 * projects use.
 */
private val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF1F6FB2),
    onPrimary = Color.White,
    secondary = Color(0xFF4E6579),
    background = Color(0xFFF4F6F7),
    surface = Color(0xFFFFFFFF)
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF7FC4F2),
    onPrimary = Color(0xFF06202F),
    secondary = Color(0xFFB4C9DC),
    background = Color(0xFF101619),
    surface = Color(0xFF182126)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AivTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}

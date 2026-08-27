package io.github.roccobot.aiv

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        motionScheme = MotionScheme.expressive()
    ) {
        // ⚠️⚠️ QUESTA `Surface` NON È DECORAZIONE, ED È LA SOLA COSA CHE DÀ UN
        // COLORE AL TESTO. Il tema porta la tavolozza ma NON tocca
        // `LocalContentColor`: quello lo imposta `Surface`, e senza di lei resta
        // al suo default, che è il NERO FISSO. Quindi ogni `Text` e ogni `Icon`
        // senza colore dichiarato usciva nero: in tema chiaro non si vede, in
        // tema scuro il testo spariva nel fondo. Segnalato dall'utente sul nome
        // dell'app nella schermata iniziale, ma il difetto era di tutta l'app, e
        // per questo il rimedio sta QUI e non su quella riga: rimediare al punto
        // dove si è visto avrebbe lasciato gli altri, uno per volta.
        // ⚠️ Sotto al visualizzatore non si vede, e va bene così: quello dipinge
        // già il proprio fondo a tutta schermata, quindi questa gli sta dietro
        // senza cambiargli niente.
        Surface(modifier = Modifier.fillMaxSize(), color = scheme.background) {
            content()
        }
    }
}

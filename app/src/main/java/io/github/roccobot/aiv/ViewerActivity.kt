package io.github.roccobot.aiv

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed interface ViewerState {
    data object Loading : ViewerState
    data class Ready(val image: LoadedImage) : ViewerState
    data class Error(@param:StringRes val messageRes: Int, val detail: String?) : ViewerState
}

/** Which of the three screens is in front. */
sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data object Viewer : Screen
}

/**
 * The decoded picture lives in the ViewModel and not in the composition: a
 * rotation must not send the phone back to the network, and on a big file that
 * would be a visible pause rather than a purist's detail.
 */
class ViewerViewModel(application: Application) : AndroidViewModel(application) {

    var state: ViewerState by mutableStateOf(ViewerState.Loading)
        private set

    var screen: Screen by mutableStateOf(Screen.Home)
        private set

    /**
     * Null until the stored settings have been read once.
     *
     * ⚠️ The screens wait for it instead of starting on the defaults, and the
     * reason is visible rather than theoretical: the resting scale of a picture
     * depends on two settings, so drawing with the defaults and then receiving the
     * real ones would show the image at one size and snap it to another.
     */
    var settings: Settings? by mutableStateOf(null)
        private set

    var recents: List<RecentImage> by mutableStateOf(emptyList())
        private set

    /** Where the picture on screen came from. The menu needs it, the loader has already used it. */
    var source: Uri? by mutableStateOf(null)
        private set

    /** Whether the viewer was reached from the opening screen, which decides where Back goes. */
    var cameFromHome: Boolean by mutableStateOf(false)
        private set

    init {
        val context = getApplication<Application>()
        viewModelScope.launch { SettingsStore.flow(context).collect { settings = it } }
        viewModelScope.launch { Recents.flow(context).collect { recents = it } }
    }

    /**
     * Called from onCreate and onNewIntent, and NOT from the composition: reading
     * the intent while composing meant re-reading it on every recomposition, and
     * the only thing that kept it from re-loading was a guard on the address.
     */
    fun handleIntent(intent: Intent?) {
        val uri = intent.imageUri()
        if (uri == null) {
            // Started from its own icon: the opening screen, not an error. Until
            // this existed the app showed a spinner and then 'no image to show',
            // which reads as 'this app does nothing'.
            screen = Screen.Home
        } else {
            open(uri, fromHome = false)
        }
    }

    fun open(uri: Uri, fromHome: Boolean = true) {
        source = uri
        cameFromHome = fromHome
        screen = Screen.Viewer
        state = ViewerState.Loading
        val context = getApplication<Application>()
        viewModelScope.launch {
            state = when (val result = ImageSource.load(context, uri)) {
                is LoadResult.Ok -> {
                    // Remembered only once it has actually opened: a list of
                    // addresses that failed would be a list of traps.
                    Recents.remember(context, uri.toString(), ImageActions.fileName(result.image, uri))
                    ViewerState.Ready(result.image)
                }
                is LoadResult.Failed -> ViewerState.Error(result.reason.messageRes(), result.detail)
            }
        }
    }

    fun goHome() {
        screen = Screen.Home
        state = ViewerState.Loading
        source = null
    }

    fun openSettings() {
        screen = Screen.Settings
    }

    /** Out of the settings, back to whichever screen was showing the picture, or home. */
    fun leaveSettings() {
        screen = if (state is ViewerState.Ready) Screen.Viewer else Screen.Home
    }

    fun updateSettings(next: Settings) {
        settings = next
        viewModelScope.launch { SettingsStore.save(getApplication(), next) }
    }

    fun forgetRecents() {
        viewModelScope.launch { Recents.clear(getApplication()) }
    }

    private fun LoadResult.Reason.messageRes(): Int = when (this) {
        LoadResult.Reason.NO_IMAGE -> R.string.no_image
        LoadResult.Reason.UNSUPPORTED -> R.string.unsupported
        LoadResult.Reason.TOO_LARGE -> R.string.too_large
        LoadResult.Reason.OPEN_FAILED -> R.string.open_failed
    }
}

class ViewerActivity : ComponentActivity() {

    private val model: ViewerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on a fresh start: on a rotation the ViewModel already holds the
        // picture, and re-reading the intent would load it a second time.
        if (savedInstanceState == null) model.handleIntent(intent)
        setContent { AivTheme { AivApp(model) } }
    }

    /**
     * The activity is singleTop, so a second link arrives here instead of starting
     * a new copy of the app. Without this the screen would keep showing the
     * previous picture, which looks exactly like a bug.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        model.handleIntent(intent)
    }
}

@Composable
private fun AivApp(model: ViewerViewModel) {
    val settings = model.settings
    if (settings == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        return
    }
    when (model.screen) {
        Screen.Home -> HomeScreen(
            recents = model.recents,
            onOpen = { model.open(it) },
            onSettings = { model.openSettings() },
            onForget = { model.forgetRecents() }
        )

        Screen.Settings -> {
            BackHandler { model.leaveSettings() }
            SettingsScreen(
                settings = settings,
                onChange = { model.updateSettings(it) },
                onBack = { model.leaveSettings() }
            )
        }

        Screen.Viewer -> {
            // Back returns to the opening screen only when that is where we came
            // from. Opened from a link, Back has to leave the app: swallowing it
            // would trap the reader in a viewer they never chose to enter.
            if (model.cameFromHome) BackHandler { model.goHome() }
            ViewerScreen(
                state = model.state,
                settings = settings,
                source = model.source,
                onHome = { model.goHome() }
            )
        }
    }
}

/**
 * Three ways in, and they are all real: a tap on a link (VIEW with the address),
 * a file opened from another app (VIEW with a content:// address) and a share
 * (SEND with the address in an extra).
 */
private fun Intent?.imageUri(): Uri? {
    if (this == null) return null
    return when (action) {
        Intent.ACTION_SEND -> getParcelableExtraCompat(Intent.EXTRA_STREAM)
        else -> data
    }
}

@Suppress("DEPRECATION")
private fun Intent.getParcelableExtraCompat(name: String): Uri? =
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, Uri::class.java)
    } else {
        getParcelableExtra(name)
    }

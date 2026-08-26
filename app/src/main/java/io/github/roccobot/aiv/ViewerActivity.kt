package io.github.roccobot.aiv

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

sealed interface ViewerState {
    data object Loading : ViewerState
    data class Ready(val image: LoadedImage) : ViewerState
    data class Error(@param:StringRes val messageRes: Int, val detail: String?) : ViewerState
}

/**
 * The decoded picture lives in the ViewModel and not in the composition: a
 * rotation must not send the phone back to the network, and on a big file that
 * would be a visible pause rather than a purist's detail.
 */
class ViewerViewModel(application: Application) : AndroidViewModel(application) {

    var state: ViewerState by mutableStateOf(ViewerState.Loading)
        private set

    private var openedUri: Uri? = null

    fun open(uri: Uri?) {
        if (uri == openedUri && state !is ViewerState.Error) return
        openedUri = uri
        state = ViewerState.Loading
        viewModelScope.launch {
            state = when (val result = ImageSource.load(getApplication(), uri)) {
                is LoadResult.Ok -> ViewerState.Ready(result.image)
                is LoadResult.Failed -> ViewerState.Error(result.reason.messageRes(), result.detail)
            }
        }
    }

    private fun LoadResult.Reason.messageRes(): Int = when (this) {
        LoadResult.Reason.NO_IMAGE -> R.string.no_image
        LoadResult.Reason.UNSUPPORTED -> R.string.unsupported
        LoadResult.Reason.TOO_LARGE -> R.string.too_large
        LoadResult.Reason.OPEN_FAILED -> R.string.open_failed
    }
}

class ViewerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AivTheme {
                val model: ViewerViewModel = viewModel()
                model.open(intent.imageUri())
                ViewerScreen(state = model.state)
            }
        }
    }

    /**
     * The activity is singleTop, so a second link arrives here instead of
     * starting a new copy of the app. Without this the screen would keep showing
     * the previous picture, which looks exactly like a bug.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
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

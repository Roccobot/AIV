package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch

/**
 * What the app shows when it is started from its own icon, with no picture handed
 * to it.
 *
 * Before this screen existed there was a spinner and then the words 'no image to
 * show', which is accurate and reads as 'this app does nothing'. The three ways
 * in are the three that a viewer with no file manager of its own actually has,
 * and they are laid out as three targets a thumb can hit rather than as a menu.
 */
@Composable
fun HomeScreen(
    recents: List<RecentImage>,
    onOpen: (Uri) -> Unit,
    onSettings: () -> Unit,
    onForget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var typing by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }

    /**
     * An address is checked before the viewer is opened, so a wrong paste says so
     * here instead of turning into a failed load two screens later.
     */
    fun openChecked(uri: Uri, whenNotImage: Int) {
        message = null
        busy = true
        scope.launch {
            if (ImageActions.leadsToImage(uri)) onOpen(uri) else message = whenNotImage
            busy = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        BigAction(
            icon = Icons.Default.ContentPaste,
            label = stringResource(R.string.home_clipboard),
            detail = stringResource(R.string.home_clipboard_sub),
            enabled = !busy
        ) {
            // Android 12 and up shows its own notice that an app has read the
            // clipboard. That is the system telling the truth, not a defect: this
            // button exists to read the clipboard, and it only does so on a press.
            val found = ImageActions.urlInClipboard(context)
            when {
                found == null -> message = R.string.clipboard_no_url
                else -> openChecked(found, R.string.clipboard_not_image)
            }
        }

        if (typing) {
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it; message = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.home_url)) },
                placeholder = { Text(stringResource(R.string.home_url_hint)) },
                singleLine = true,
                enabled = !busy,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = {
                    typedToUri(typed)?.let { openChecked(it, R.string.url_not_image) }
                })
            )
            // The arrow sits UNDER the field and not inside it, as asked. It is
            // also the only control here that is not a wide button: it acts on
            // what is above it, and a full width button would read as a fourth
            // way in rather than as the end of this one.
            FilledIconButton(
                onClick = { typedToUri(typed)?.let { openChecked(it, R.string.url_not_image) } },
                enabled = !busy && typed.isNotBlank(),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.home_url_go)
                )
            }
        } else {
            BigAction(
                icon = Icons.Default.Link,
                label = stringResource(R.string.home_url),
                detail = null,
                enabled = !busy
            ) { typing = true }
        }

        BigAction(
            icon = Icons.Default.Settings,
            label = stringResource(R.string.home_settings),
            detail = null,
            enabled = !busy,
            onClick = onSettings
        )

        if (busy) {
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator(Modifier.size(28.dp))
        }
        message?.let {
            Text(
                text = stringResource(it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        if (recents.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.home_recent),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            recents.forEach { entry ->
                TextButton(
                    onClick = { onOpen(entry.address.toUri()) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = entry.name.ifBlank { entry.address },
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            TextButton(onClick = onForget, enabled = !busy) {
                Text(stringResource(R.string.home_recent_forget))
            }
        }
    }
}

/**
 * What was typed, as an address, with the one repair that is always safe: no
 * scheme gets `https://`. Typing 'example.com/cat.jpg' is what people do, and
 * refusing it would be pedantry rather than caution.
 */
private fun typedToUri(typed: String): Uri? {
    val text = typed.trim()
    if (text.isEmpty()) return null
    val hasScheme = Regex("^[a-z][a-z0-9+.-]*://", RegexOption.IGNORE_CASE).containsMatchIn(text)
    return (if (hasScheme) text else "https://$text").toUri()
}

/** One of the three targets: tall enough for a thumb, with room for a second line. */
@Composable
private fun BigAction(
    icon: ImageVector,
    label: String,
    detail: String?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = label, style = MaterialTheme.typography.titleMedium)
                detail?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

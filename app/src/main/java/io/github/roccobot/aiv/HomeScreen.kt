package io.github.roccobot.aiv

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch

/**
 * What the app shows when it is started from its own icon, with no picture handed
 * to it.
 *
 * ⚠️ The ways in sit at the BOTTOM, and the identity at the top, because a phone is
 * held in one hand: the thumb reaches the lower third comfortably and the upper
 * third barely at all. So the half that is only there to be read goes where
 * reading is easy, and the half that is there to be pressed goes where pressing
 * is easy. Asked for by the user after holding the first version.
 *
 * ⚠️ There are FIVE of them and not three, because the first version could open
 * anything except the pictures already on the phone: it received them from other
 * apps through the manifest, but had no way of going to look for one itself.
 * ⚠️ **La cartella sta PRIMA della foto singola**, ed è il rovesciamento chiesto
 * dall'utente (*perché devo sempre scegliere una singola foto per partire?*): finché
 * c'era solo il selettore di sistema, partire da una foto sola non era una scelta ma
 * l'unica cosa possibile, perché di quel lasciapassare non si risale a nessun
 * genitore. Con l'accesso a tutti i file la cartella è la via più generale, e la foto
 * singola resta per quando si sa già quale si vuole.
 */
@Composable
fun HomeScreen(
    recents: List<RecentImage>,
    onOpen: (Uri) -> Unit,
    onFolders: () -> Unit,
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
     * A URL is checked before the viewer is opened, so a wrong paste says so here
     * instead of turning into a failed load two screens later.
     */
    fun openChecked(uri: Uri) {
        message = null
        busy = true
        scope.launch {
            if (ImageActions.leadsToImage(uri)) onOpen(uri) else message = R.string.url_not_image
            busy = false
        }
    }

    /**
     * The system photo picker.
     *
     * ⚠️⚠️ **It needs no permission at all**, and that is the reason to use it over
     * anything that reads the media store directly: the person picks one picture
     * and the app is granted that one picture, instead of the app asking to see
     * every photo on the phone in order to show one. On Android 13 and up this is
     * the system picker; below it, the library provides the same contract over the
     * old chooser, so there is no version fork to write here.
     *
     * ⚠️ The one thing it will NOT show is an image the media store has not indexed
     * (a TIFF sitting in Downloads, say). Those already arrive through the share
     * and open-with filters in the manifest, which is how a local picture reached
     * this viewer before this button existed.
     */
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { picked -> picked?.let(onOpen) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Identity()

        // The elastic middle. It carries the recents when there are any and
        // nothing when there are not, so the block below stays pinned to the
        // bottom either way. ⚠️ The scroll lives HERE and not on the whole
        // screen: a scrollable Column has no bounded height, so `weight` inside
        // one does not work, and the buttons would drift up the page.
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            if (recents.isNotEmpty()) {
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

        if (busy) {
            CircularProgressIndicator(Modifier.size(24.dp))
            Spacer(Modifier.height(10.dp))
        }
        message?.let {
            Text(
                text = stringResource(it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
        }

        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Prima di tutte perché è la cosa più comune da volere da un visualizzatore
        // che sta su un telefono: le foto che il telefono ha già, e non una sola.
        BigAction(
            icon = Icons.Default.Folder,
            label = stringResource(R.string.home_folder),
            detail = stringResource(R.string.home_folder_sub),
            enabled = !busy,
            onClick = onFolders
        )
        Spacer(Modifier.height(10.dp))

        BigAction(
            icon = Icons.Default.PhotoLibrary,
            label = stringResource(R.string.home_local),
            detail = stringResource(R.string.home_local_sub),
            enabled = !busy
        ) {
            picker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        Spacer(Modifier.height(10.dp))

        BigAction(
            icon = Icons.Default.ContentPaste,
            label = stringResource(R.string.home_clipboard),
            detail = stringResource(R.string.home_clipboard_sub),
            enabled = !busy
        ) {
            // Android 12 and up shows its own notice that an app has read the
            // clipboard. That is the system telling the truth, not a defect: this
            // button exists to read the clipboard, and only does so on a press.
            val found = ImageActions.urlInClipboard(context)
            if (found == null) message = R.string.clipboard_no_url else openChecked(found)
        }
        Spacer(Modifier.height(10.dp))

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
                keyboardActions = KeyboardActions(onGo = { typedToUri(typed)?.let(::openChecked) })
            )
            // The arrow sits UNDER the field and not inside it, as asked. It is
            // also the only control here that is not a wide button: it acts on
            // what is above it, and a full width button would read as a fourth
            // way in rather than as the end of this one.
            FilledIconButton(
                onClick = { typedToUri(typed)?.let(::openChecked) },
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
        Spacer(Modifier.height(10.dp))

        BigAction(
            icon = Icons.Default.Settings,
            label = stringResource(R.string.home_settings),
            detail = null,
            enabled = !busy,
            onClick = onSettings
        )
    }
}

/**
 * The icon, the name, and where it comes from.
 *
 * ⚠️ None of this text is a string resource, and that is deliberate: it is a name,
 * a signature and a domain. Translating 'by Roccobot' would be a mistake rather
 * than a courtesy, and a resource would invite one.
 */
@Composable
private fun Identity() {
    AppIcon(96.dp)
    Spacer(Modifier.height(12.dp))
    Text(
        text = "Astonishing Image Viewer",
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center
    )
    Text(
        text = "(AIV) by Roccobot 天",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Text(
        text = buildAnnotatedString {
            withLink(LinkAnnotation.Url("https://roccobot.me")) {
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                ) { append("roccobot.me") }
            }
        },
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
}

/**
 * Quanto si ingrandisce il livello di primo piano perché questa anteprima mostri la
 * stessa cosa che mostra il launcher.
 *
 * ⚠️⚠️ **I due fattori sono uno per ciascuna cosa che il launcher fa, e non sono
 * intercambiabili.** `108 / 72`, cioè esattamente 1.5, perché di un'icona adattiva
 * si vedono **solo i 72dp centrali** della tela da 108: l'anello esterno esiste per
 * la maschera e la parallasse del launcher, e chi rende la tela intera sta mostrando
 * un margine che sul telefono nessuno vede. Il **1.3** è l'altra metà di una coppia:
 * HyperOS ingrandisce il primo piano di circa un terzo, quindi
 * `ic_launcher_foreground` porta un glifo rimpicciolito di 1.3 per venire giusto sul
 * telefono, e qui si moltiplica per lo stesso 1.3 per rivedere il disegno com'è.
 * Chi ne cambia uno deve cambiare anche l'altro.
 */
private const val LAUNCHER_ZOOM = 1.5f * 1.3f

/**
 * The launcher icon, drawn large.
 *
 * ⚠️⚠️ **L'ingrandimento si fa al DISEGNO e non alla misura, e fino alla 0.27 questa
 * differenza costava tutto l'ingrandimento.** `Modifier.size` **negozia** col
 * genitore: `SizeNode.measure` chiama `constrain(vincoli in ingresso, misura
 * chiesta)` quando `enforceIncoming` è vero, e per `size` è vero (per `requiredSize`
 * no) - verificato nel bytecode di `foundation-layout`, non a memoria. Il `Box` qui
 * sotto passa ai figli i propri vincoli col solo minimo azzerato, quindi il massimo
 * resta la misura della piastrella: l'immagine chiedeva 187.2dp, ne otteneva 96, e
 * l'anteprima mostrava la **tela intera** invece dei 72dp centrali. Cioè un glifo
 * **1.95 volte più piccolo** di quello del launcher, che è esattamente quello che
 * l'utente ha visto e segnalato.
 * ⚠️ Un `Modifier.scale` è una trasformazione di disegno: nessun genitore la può
 * limitare, e il ritaglio del `Box` continua a valere. Con `requiredSize` si otterrebbe
 * lo stesso risultato, ma resterebbe una misura da far rispettare a chi sta sopra.
 */
@Composable
private fun AppIcon(size: Dp) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 24))
            .background(colorResource(R.color.launcher_background)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().scale(LAUNCHER_ZOOM)
        )
    }
}

/**
 * What was typed, as a URL, with the one repair that is always safe: no scheme
 * gets `https://`. Typing 'example.com/cat.jpg' is what people do, and refusing it
 * would be pedantry rather than caution.
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
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
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

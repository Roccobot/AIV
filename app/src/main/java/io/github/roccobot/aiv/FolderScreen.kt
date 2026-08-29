package io.github.roccobot.aiv

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Le cartelle di immagini del telefono, in copertine, da scegliere.
 *
 * ⚠️⚠️ **Esiste perché il selettore di sistema non sa dire una cartella**: restituisce
 * un lasciapassare per **un** elemento, e da lì non si risale a niente. Finché l'app
 * aveva solo quello, partire da una foto sola non era una scelta di interfaccia ma
 * l'unica cosa possibile. Con l'accesso a tutti i file la domanda 'quali cartelle
 * ci sono' ha finalmente una risposta, e questa schermata è quella risposta.
 *
 * ⚠️ **Serve a DUE cose e la seconda non è un doppione**: da qui si apre una cartella
 * adesso, e da qui si sceglie quella da aprire all'avvio. È la stessa domanda
 * ('quale cartella?'), quindi è la stessa schermata: due elenchi identici che
 * divergono al primo ritocco sono il modo classico di far invecchiare una funzione.
 *
 * ⚠️⚠️ **DALLA 0.37 SONO COPERTINE E NON UN ELENCO DI RIGHE**, ed è il passo 2 della
 * galleria: una cartella si riconosce da quello che contiene molto prima che dal suo
 * nome, e i nomi veri di un telefono (`Camera`, `Screenshots`, `WhatsApp Images`,
 * `.thumbnails`) si somigliano tutti. ⚠️ Non è costato nessuna interrogazione in più:
 * la copertina esce dalla stessa passata che conta le foto (vedi `Folder.Bucket`).
 */
@Composable
fun FolderScreen(
    onPick: (Folder.Bucket) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(Folder.granted(context)) }
    var folders by remember { mutableStateOf<List<Folder.Bucket>?>(null) }

    // ⚠️ Al ritorno dalla pagina di sistema non arriva nessun esito, perché non è un
    // dialogo: si RICHIEDE allo stato delle cose, come fa il viewer.
    val fromSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { granted = Folder.granted(context) }

    LaunchedEffect(granted) {
        folders = if (granted) Folder.buckets(context) else emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            // ⚠️ Il margine laterale è sceso da 20 a 12 col passaggio alle copertine, e
            // non è un gusto: è quello che permette **due** colonne su uno schermo da
            // 360dp. Il conto sta in `FOLDER_CELL`.
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back)
                )
            }
            Text(
                text = stringResource(R.string.folders_title),
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Spacer(Modifier.height(8.dp))

        when {
            !granted -> Column(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.folders_permission),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(onClick = {
                    // Il ripiego sull'elenco generale non è un lusso: la pagina mirata
                    // all'app manca su qualche sistema.
                    Folder.settingsIntents(context).any {
                        runCatching { fromSettings.launch(it) }.isSuccess
                    }
                }) { Text(stringResource(R.string.folders_grant)) }
            }

            folders == null -> CircularProgressIndicator(
                Modifier.padding(top = 24.dp).size(28.dp).align(Alignment.CenterHorizontally)
            )

            folders!!.isEmpty() -> Text(
                text = stringResource(R.string.folders_none),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp)
            )

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = FOLDER_CELL),
                horizontalArrangement = Arrangement.spacedBy(FOLDER_GAP),
                verticalArrangement = Arrangement.spacedBy(FOLDER_GAP),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = folders!!,
                    key = { it.id },
                    // Una piastrella sola per tutte, così Compose riusa la composizione
                    // di quelle che escono invece di ricostruirla mentre si scorre.
                    contentType = { FOLDER_KIND }
                ) { bucket -> FolderCard(bucket = bucket, onClick = { onPick(bucket) }) }
            }
        }
    }
}

/**
 * Una cartella: la sua foto più recente, il nome e quante ne contiene.
 *
 * ⚠️ **La miniatura è la STESSA richiesta della griglia e del visualizzatore**
 * (`Thumbs.request`, 512 px): la misura è parte della chiave di cache, quindi una
 * copertina già vista altrove è gratis, e chiederla qui più piccola perché il riquadro
 * è più piccolo costerebbe una seconda generazione della stessa immagine.
 * ⚠️ **La copertina è decorativa e non porta descrizione**: sotto ci sono già il nome e
 * il conto, e `clickable` fonde le semantiche dei figli, quindi TalkBack legge una voce
 * sola. Descrivere anche l'immagine la farebbe leggere due volte.
 * ⚠️ Il simbolo di ripiego non è un riquadro vuoto: una cartella senza copertina resta
 * toccabile e va detto che è una cartella, o sembra una piastrella rotta.
 */
@Composable
private fun FolderCard(bucket: Folder.Bucket, onClick: () -> Unit) {
    val shape = RoundedCornerShape(FOLDER_CORNER)
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Cover(bucket.cover)
        }
        Text(
            text = bucket.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = pluralStringResource(R.plurals.folders_count, bucket.count, bucket.count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Cover(uri: Uri?) {
    if (uri == null) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )
        return
    }
    val context = LocalContext.current
    val model = remember(uri, context) { Thumbs.request(context, uri) }
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Il lato minimo di una copertina.
 *
 * ⚠️⚠️ **È IL NUMERO CHE DECIDE QUANTE COLONNE, e va letto col conto in mano**:
 * `GridCells.Adaptive` tiene `(spazio + distacco) / (minimo + distacco)` colonne. Su uno
 * schermo da 360dp, tolti i 12 di margine per lato, restano 336: `(336 + 12) / (150 + 12)`
 * fa 2.1, quindi **due**. A 156 farebbe 2.06 e a 164 scenderebbe a 1, cioè una colonna
 * sola: il salto è vicino, ed è la ragione per cui questo numero non si ritocca a occhio.
 * ⚠️ **Due e non tre come le foto**: qui sotto la copertina ci sono un nome e un conto, e
 * su tre colonne un nome vero (`WhatsApp Images`) resterebbe una sigla. Le miniature di
 * una cartella non hanno didascalia, e infatti stanno a 108dp.
 */
private val FOLDER_CELL = 150.dp

/** Il distacco fra le copertine: più largo di quello delle foto, perché qui separa schede. */
private val FOLDER_GAP = 12.dp

/**
 * Lo smusso della copertina.
 *
 * ⚠️ Più tondo dei 4dp delle miniature apposta: una piastrella molto smussata si legge
 * come un **contenitore**, una quasi quadrata come una fotografia. Qui la differenza fra
 * le due cose è tutta quella che l'occhio ha per capire dove si trova.
 */
private val FOLDER_CORNER = 12.dp

/** Tutte le piastrelle sono la stessa cosa, e dirlo permette a Compose di riusarle. */
private const val FOLDER_KIND = "folder"

package io.github.roccobot.aiv

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
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
 * Le cartelle di immagini del telefono: **la schermata iniziale dell'app**, dalla `0.41`.
 *
 * ⚠️⚠️ **PRIMA C'ERA UNA SCHERMATA CHE CHIEDEVA DA DOVE COMINCIARE, con cinque tasti, ed
 * è sparita** (decisione dell'utente, 2026-08-29: *non ha più senso la scelta multipla
 * all'avvio, la risolviamo in modo smart*). Chi apre un visualizzatore di immagini vuole
 * vedere immagini: la domanda 'da dove?' la si risponde da soli, e le vie che non sono la
 * risposta di quasi sempre stanno dietro un tastino.
 *
 * ⚠️ **Serve a DUE cose e la seconda non è un doppione**: da qui si apre una cartella
 * adesso, e da qui si sceglie quella da aprire all'avvio. È la stessa domanda ('quale
 * cartella?'), quindi è la stessa schermata: due elenchi identici che divergono al primo
 * ritocco sono il modo classico di far invecchiare una funzione. ⚠️ Nella seconda veste
 * il tastino **non c'è** e il gesto Indietro **sì**, che è l'esatto contrario della
 * prima: si sta rispondendo a una domanda, non girando per l'app.
 *
 * ⚠️ Le copertine e l'elenco sono **due viste della stessa cosa** e non una migliore
 * dell'altra: vedi `FolderView`.
 */
@Composable
fun FolderScreen(
    view: FolderView,
    recents: List<RecentImage>,
    onPick: (Folder.Bucket) -> Unit,
    onOpen: (Uri) -> Unit,
    onView: (FolderView) -> Unit,
    onForget: () -> Unit,
    onSettings: () -> Unit,
    onBack: (() -> Unit)?,
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

    Box(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // ⚠️ Il margine laterale è 12 e non 20 perché è quello che permette **due**
                // colonne di copertine su uno schermo da 360dp. Il conto sta in `FOLDER_CELL`.
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                onBack?.let {
                    IconButton(onClick = it) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.folders_title),
                    style = MaterialTheme.typography.headlineSmall,
                    // Senza la freccia il titolo partirebbe dal bordo vivo.
                    modifier = if (onBack == null) Modifier.padding(start = 4.dp) else Modifier
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

                view == FolderView.GRID -> Covers(folders!!, onPick)
                else -> Rows(folders!!, onPick)
            }
        }

        // ⚠️ Il tastino manca quando si sta scegliendo la cartella dell'avvio: là dentro
        // ci sono le impostazioni, da cui si è arrivati, e un giro chiuso non serve a
        // nessuno.
        if (onBack == null) {
            Hub(
                view = view,
                recents = recents,
                onOpen = onOpen,
                onView = onView,
                onForget = onForget,
                onSettings = onSettings,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            )
        }
    }
}

/**
 * Il tastino quadrato in basso a destra, e tutto quello che c'è dietro.
 *
 * ⚠️⚠️ **È IL MENU DELL'APP, non una scorciatoia**: con la schermata iniziale sparita,
 * impostazioni, indirizzo manuale, selettore di sistema e scelta della vista non hanno
 * più nessun altro posto dove stare. Sta **in basso a destra** perché è l'unico angolo
 * che un pollice raggiunge senza cambiare presa, ed è **piccolo** perché non deve
 * competere con le fotografie: la richiesta dell'utente era *un FAB quadrato piccolo*.
 *
 * ⚠️⚠️ **E il selettore di sistema è QUI dentro anche per una ragione che non è di
 * comodo**: è l'unica via per aprire un'immagine **senza il permesso** sui file. Chi
 * quel permesso non lo concede vede una schermata delle cartelle vuota, e senza questa
 * voce l'app non gli servirebbe più a niente.
 */
@Composable
private fun Hub(
    view: FolderView,
    recents: List<RecentImage>,
    onOpen: (Uri) -> Unit,
    onView: (FolderView) -> Unit,
    onForget: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    var asking by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { picked -> picked?.let(onOpen) }

    Box(modifier = modifier) {
        SmallFloatingActionButton(
            onClick = { open = true },
            // Quadrato con gli angoli appena smussati, come chiesto: il tondo pieno
            // griderebbe 'azione principale', e qui l'azione principale sono le cartelle.
            shape = RoundedCornerShape(HUB_CORNER)
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = stringResource(R.string.hub_open)
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            // ⚠️ La voce nomina la vista che si OTTIENE, non quella in cui si è: una
            // riga di menu è una cosa che si chiede, non un'etichetta di stato.
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (view == FolderView.GRID) R.string.hub_view_list
                            else R.string.hub_view_grid
                        )
                    )
                },
                onClick = {
                    open = false
                    onView(if (view == FolderView.GRID) FolderView.LIST else FolderView.GRID)
                }
            )

            HorizontalDivider()

            DropdownMenuItem(
                text = { Text(stringResource(R.string.hub_url)) },
                onClick = { open = false; asking = true }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.hub_pick)) },
                onClick = {
                    open = false
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            HorizontalDivider()

            DropdownMenuItem(
                text = { Text(stringResource(R.string.hub_settings)) },
                onClick = { open = false; onSettings() }
            )
        }
    }

    if (asking) {
        AddressDialog(
            recents = recents,
            onOpen = onOpen,
            onForget = onForget,
            onDismiss = { asking = false }
        )
    }
}

/** Le cartelle come copertine. */
@Composable
private fun Covers(folders: List<Folder.Bucket>, onPick: (Folder.Bucket) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = FOLDER_CELL),
        horizontalArrangement = Arrangement.spacedBy(FOLDER_GAP),
        verticalArrangement = Arrangement.spacedBy(FOLDER_GAP),
        // ⚠️ Lo spazio in fondo tiene l'ultima cartella fuori da sotto il tastino, che
        // le si siederebbe sopra proprio quando si è scorso fino in fondo.
        contentPadding = PaddingValues(bottom = BELOW_HUB),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(
            items = folders,
            key = { it.id },
            // Una piastrella sola per tutte, così Compose riusa la composizione di quelle
            // che escono invece di ricostruirla mentre si scorre.
            contentType = { FOLDER_KIND }
        ) { bucket -> FolderCard(bucket = bucket, onClick = { onPick(bucket) }) }
    }
}

/**
 * Le cartelle come elenco.
 *
 * ⚠️ **Porta la copertina anche qui, piccola**: l'elenco della `0.29` mostrava la stessa
 * icona di cartella davanti a ogni riga, ed è esattamente il difetto che le copertine
 * hanno corretto. Questa vista serve a leggere **più nomi insieme**, non a rinunciare a
 * vedere che cosa c'è dentro.
 */
@Composable
private fun Rows(folders: List<Folder.Bucket>, onPick: (Folder.Bucket) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = BELOW_HUB),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(items = folders, key = { it.id }, contentType = { ROW_KIND }) { bucket ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(bucket) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(ROW_COVER)
                        .clip(RoundedCornerShape(FOLDER_CORNER))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Cover(bucket.cover)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bucket.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.folders_count,
                            bucket.count,
                            bucket.count
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider()
        }
    }
}

/**
 * Una cartella: la sua foto più recente, il nome e quante ne contiene.
 *
 * ⚠️ **La miniatura è la STESSA richiesta della griglia e del visualizzatore**
 * (`Thumbs.request`, 512 px): la misura è parte della chiave di cache, quindi una
 * copertina già vista altrove è gratis, e chiederla più piccola perché il riquadro è più
 * piccolo costerebbe una seconda generazione della stessa immagine. Vale anche per la
 * copertina minuscola dell'elenco, che è la stessa identica richiesta.
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

/** La copertina dell'elenco: grande quanto due righe di testo, che è l'altezza della riga. */
private val ROW_COVER = 48.dp

/** Lo smusso del tastino: quadrato, ma non tagliente. */
private val HUB_CORNER = 12.dp

/** Quanto spazio resta sotto l'ultima cartella, perché il tastino non le si sieda sopra. */
private val BELOW_HUB = 76.dp

/** Tutte le piastrelle sono la stessa cosa, e dirlo permette a Compose di riusarle. */
private const val FOLDER_KIND = "folder"

/** Come sopra, per le righe dell'elenco. */
private const val ROW_KIND = "folder-row"

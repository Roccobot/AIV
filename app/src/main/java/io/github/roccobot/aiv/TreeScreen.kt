package io.github.roccobot.aiv

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import java.io.File

/**
 * La vista **'Cartelle di sistema'**, la terza dopo la griglia e la lista.
 *
 * ⚠️⚠️ **NASCE DALLA `0.84`, ed è la richiesta dell'utente**: *navigare liberamente la
 * memoria come un gestore di file, col fuoco che resta sulle immagini*. Le fotografie e i
 * filmati si aprono nel visualizzatore, gli altri file si vedono e si aprono col sistema.
 * ⚠️⚠️ **E SERVE ANCHE A GIUSTIFICARE IL PERMESSO PESANTE davanti al Play Store**: l'accesso
 * a tutti i file non è ammesso per un visualizzatore di immagini, lo è per un gestore di
 * file. Chi togliesse questa vista dovrebbe togliere anche il permesso, e con lui metà di
 * quello che l'app sa fare.
 *
 * ⚠️ **NON è una terza resa dello stesso elenco**, al contrario di griglia e lista, che
 * mostrano le stesse cartelle del MediaStore in due modi: qui l'elenco è **un altro**, viene
 * dal disco (vedi [Tree]), e si naviga invece di scorrerlo soltanto. Sta nello stesso posto
 * delle altre due perché la domanda dell'utente era *dove sono le mie cose*, e le tre
 * risposte sono sorelle; ma chi ci lavora sopra sappia che sotto non condividono niente.
 *
 * ⚠️⚠️ **Gli indirizzi che escono di qui sono `file://` e non `content://`**, ed è
 * deliberato: questa vista **è** il disco. Ricavare l'indirizzo del MediaStore per ogni riga
 * vorrebbe dire una query per file, cioè trecento query per aprire una cartella. Il costo
 * dichiarato è che i `file://` sono il secondo genere di indirizzo dell'app, quello del
 * ripiego di `Folder.fromDisk`: le miniature passano da `ThumbnailUtils` invece che dal
 * provider, e la consegna a un altro programma non è possibile (vedi [openWithSystem]).
 */
@Composable
fun TreeList(
    /** Dove si è, e `null` vuol dire 'in cima'. Vive nel modello: vedi `ViewerViewModel`. */
    path: String?,
    /** I percorsi che l'utente ha nascosto, per segnarli. Vedi `Settings.hiddenFolders`. */
    hidden: Set<String>,
    onPath: (String?) -> Unit,
    onOpen: (List<Uri>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val roots = remember(context) { Tree.roots(context) }

    /*
     * ⚠️⚠️ **CON UNA MEMORIA SOLA NON SI MOSTRA L'ELENCO DELLE MEMORIE**, ed è quello che
     * fanno i gestori di file: una schermata con una voce sola da toccare per forza è un
     * passo che non decide niente. Con una scheda SD inserita le radici sono due, e allora
     * la scelta esiste davvero.
     */
    val here = path ?: roots.singleOrNull()?.file?.absolutePath

    Column(modifier = modifier.fillMaxWidth()) {
        if (here == null) {
            Roots(roots, onPath)
            return@Column
        }
        val dir = remember(here) { File(here) }
        val up = remember(dir, roots) { Tree.parent(dir, roots) }
        PathBar(dir, up, roots, onPath)
        val spots by produceState<List<Tree.Spot>?>(null, here) { value = Tree.list(File(here)) }
        when {
            spots == null -> Unit
            spots!!.isEmpty() -> Text(
                text = stringResource(R.string.tree_empty),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp)
            )
            else -> Spots(spots!!, hidden, onPath, onOpen)
        }
    }
}

/** Le memorie da cui si può partire, quando ce n'è più di una. */
@Composable
private fun Roots(roots: List<Tree.Root>, onPath: (String?) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = BELOW_FAB)) {
        items(items = roots, key = { it.file.absolutePath }) { root ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPath(root.file.absolutePath) }
                    .padding(vertical = ROW_PAD, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SdStorage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(GLYPH)
                )
                Text(text = root.name, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/**
 * Dove si è, e il tasto per salire.
 *
 * ⚠️ **Il nome della cartella grande e il percorso piccolo sotto**, e non il percorso solo:
 * dentro `/storage/emulated/0/DCIM/Camera` quello che si guarda è `Camera`, e in una riga
 * sola quella parola finirebbe in coda, cioè nel pezzo che l'ellissi mangia.
 * ⚠️ **Il tasto per salire manca in cima**, invece di esserci spento: un tasto grigio che non
 * si può premere occupa lo stesso spazio di uno che funziona e non dice niente di più della
 * sua assenza.
 */
@Composable
private fun PathBar(dir: File, up: File?, roots: List<Tree.Root>, onPath: (String?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ⚠️ Salire dalla radice riporta all'elenco delle memorie, e solo quando quell'elenco
        // esiste: con una memoria sola la radice è il capolinea.
        val above: (() -> Unit)? = when {
            up != null -> ({ onPath(up.absolutePath) })
            roots.size > 1 -> ({ onPath(null) })
            else -> null
        }
        if (above != null) {
            IconButton(onClick = above) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = stringResource(R.string.tree_up)
                )
            }
        }
        Column(modifier = Modifier.padding(start = if (above == null) 4.dp else 0.dp)) {
            Text(
                text = dir.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dir.absolutePath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Le righe di una cartella.
 *
 * ⚠️⚠️ **LA SERIE DA SFOGLIARE SI COSTRUISCE QUI, dall'elenco GIÀ LETTO**, e non rileggendo
 * la cartella: così l'ordine che si sfoglia è esattamente quello che si è appena visto, per
 * costruzione e non per accordo fra due funzioni. Una seconda lettura avrebbe anche potuto
 * dare un elenco diverso, perché fra il disegno e il tocco il disco può cambiare.
 * ⚠️ **E l'ordine NON è quello della galleria**: qui si ordina per nome, come in un gestore
 * di file (vedi [Tree.list]). Chi apre una fotografia da qui sfoglia in ordine di nome, ed è
 * la cosa giusta: è l'elenco che ha davanti agli occhi.
 */
@Composable
private fun Spots(
    spots: List<Tree.Spot>,
    hidden: Set<String>,
    onPath: (String?) -> Unit,
    onOpen: (List<Uri>, Int) -> Unit
) {
    val context = LocalContext.current
    // ⚠️ Si ricava una volta per elenco e non a ogni tocco: la posizione di un file dentro i
    // soli media non è la sua posizione fra le righe, che comprendono anche le cartelle.
    val reels = remember(spots) { spots.filter { it.media } }
    val addresses = remember(reels) { reels.map { Uri.fromFile(it.file) } }

    LazyColumn(contentPadding = PaddingValues(bottom = BELOW_FAB)) {
        items(items = spots, key = { it.path }) { spot ->
            SpotRow(
                spot = spot,
                marked = spot.folder && spot.path in hidden,
                onClick = {
                    when {
                        spot.folder -> onPath(spot.path)
                        spot.media -> {
                            val at = reels.indexOfFirst { it.path == spot.path }
                            if (at >= 0) onOpen(addresses, at)
                        }
                        else -> openWithSystem(context, spot.file)
                    }
                }
            )
        }
    }
}

@Composable
private fun SpotRow(spot: Tree.Spot, marked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = ROW_PAD, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SpotGlyph(spot)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = spot.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // ⚠️ Sotto una cartella non c'è nessuna seconda riga, e non è una dimenticanza:
            // l'unica cosa che si potrebbe scrivere è quanto contiene, e saperlo costa una
            // lettura di directory per riga (vedi [Tree.Spot]).
            if (!spot.folder) {
                Text(
                    text = "${formatBytes(spot.size)}  ${moment(spot.stamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        // ⚠️ Il segno delle cartelle nascoste esiste perché questa vista le MOSTRA, al
        // contrario di tutte le altre: un gestore di file che non fa vedere una cartella che
        // sul disco c'è dice una bugia sul disco. Ma chi l'ha nascosta deve poterlo sapere,
        // o si chiederà perché quella cartella non compare nella griglia.
        if (marked) {
            Text(
                text = stringResource(R.string.tree_hidden),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Il quadratino di sinistra: la miniatura se è un'immagine o un filmato, il simbolo se no.
 *
 * ⚠️ **La miniatura passa dal caricatore di sempre** (`Thumbs`), quindi un `file://` finisce
 * su `ThumbnailUtils`, che per i video usa la funzione giusta dalla `0.83`. Niente di nuovo
 * da scrivere qui.
 */
@Composable
private fun SpotGlyph(spot: Tree.Spot) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(GLYPH)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            spot.media -> {
                val model = remember(spot.path, context) {
                    Thumbs.request(context, Uri.fromFile(spot.file))
                }
                Image(
                    painter = rememberAsyncImagePainter(model = model),
                    contentDescription = null,
                    // ⚠️ `Crop` come in griglia: in un quadratino da 44dp una fotografia
                    // adattata lascerebbe due bande vuote invece di riempirlo.
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(GLYPH).clip(RoundedCornerShape(4.dp))
                )
                if (spot.clip) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.grid_item_clip),
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(GLYPH * 0.6f)
                    )
                }
            }
            spot.folder -> Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(GLYPH * 0.7f)
            )
            else -> Icon(
                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(GLYPH * 0.7f)
            )
        }
    }
}

/**
 * Consegna al sistema un file che non è né una fotografia né un filmato.
 *
 * ⚠️⚠️ **PASSA DAL MEDIASTORE E NON DAL NOSTRO PROVIDER, ed è una scelta di sicurezza.** Un
 * altro programma non può leggere un nostro `file://` (da Android 7 il sistema solleva), e la
 * via comoda sarebbe allargare il FileProvider a tutta la memoria. Non si fa: quel provider
 * serve **una cartella sola**, quella delle condivisioni, apposta (vedi
 * `ImageActions.shareMany`), e allargarlo per la comodità di aprire un PDF vorrebbe dire
 * pagare in sicurezza una cosa che il sistema sa già fare da sé. Il MediaStore indicizza
 * anche i documenti, quindi per la stragrande maggioranza dei file un indirizzo `content://`
 * esiste già, ed è **suo**, non nostro.
 * ⚠️ **Quando quell'indirizzo non c'è si dice, invece di provarci e fallire in silenzio**: un
 * file appena copiato che l'indicizzazione non ha ancora visto non si apre, e chi tocca deve
 * sapere perché.
 * ⚠️ **Il tipo si chiede all'estensione** (`MimeTypeMap`), perché il MediaStore lo dichiara
 * solo per quello che ha indicizzato come media: senza tipo il sistema non sa a chi
 * proporlo, e il dialogo esce vuoto.
 */
private fun openWithSystem(context: Context, file: File) {
    val uri = Tree.contentUri(context, file)
    if (uri == null) {
        Toast.makeText(context, R.string.tree_unopenable, Toast.LENGTH_SHORT).show()
        return
    }
    val kind = MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(file.extension.lowercase())
        ?: runCatching { context.contentResolver.getType(uri) }.getOrNull()
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, kind)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val ok = runCatching { context.startActivity(intent); true }
        .getOrElse { if (it is ActivityNotFoundException) false else throw it }
    if (!ok) Toast.makeText(context, R.string.tree_unopenable, Toast.LENGTH_SHORT).show()
}

/** Il lato del quadratino di sinistra: come la miniatura della lista, ma più piccolo. */
private val GLYPH = 44.dp

/** Il respiro sopra e sotto una riga. */
private val ROW_PAD = 8.dp

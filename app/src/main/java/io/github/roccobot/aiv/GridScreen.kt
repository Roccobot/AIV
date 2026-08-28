package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Una cartella intera in miniature, ed è il primo passo della galleria.
 *
 * ⚠️⚠️ **L'ORDINE È QUELLO DI LETTURA, non uno suo**, ed è la decisione che tiene
 * insieme le due viste: le miniature stanno nella stessa sequenza in cui la
 * strisciata le sfoglierà, quindi 'la prossima' è la stessa cosa qui e là. Col verso
 * predefinito (`Cambia verso` spenta) è la più recente per prima, cioè l'ordine di
 * una galleria; accendendo l'impostazione si girano tutte e due insieme, perché
 * vengono dalla stessa lista girata una volta sola (vedi `Folder.Series.reversed`).
 * ⚠️ Chi un domani volesse la griglia 'sempre dalla più recente' rompe questa
 * corrispondenza: il tocco sulla terza miniatura aprirebbe la terzultima foto.
 *
 * ⚠️ **La lista arriva GIÀ PRONTA dal modello e non si interroga il MediaStore qui**:
 * è la stessa serie che il visualizzatore userà per sfogliare, quindi aprire una foto
 * dalla griglia non costa nessuna query e non può dare due ordini diversi.
 */
@Composable
fun GridScreen(
    title: String,
    items: List<Uri>?,
    startAt: Int,
    onOpen: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberLazyGridState()

    /*
     * ⚠️⚠️ UNA VOLTA SOLA PER VISITA, e la bandierina non è pignoleria: alla ROTAZIONE
     * `rememberLazyGridState` ripristina da sé il punto in cui si stava scorrendo
     * (dentro è un `rememberSaveable`), e un effetto che riparte lo butterebbe via
     * riportando la griglia sulla foto da cui si era entrati. Anche la bandierina è
     * saveable, per la stessa ragione.
     * ⚠️ Cambiando SCHERMATA invece il composable esce dalla composizione e si porta via
     * la bandierina: rientrando ci si riposiziona, che è esattamente quello che serve.
     */
    var placed by rememberSaveable { mutableStateOf(false) }

    // ⚠️ Tornando dal visualizzatore la griglia si porta SULLA foto che si stava
    // guardando, invece di ripartire dall'inizio: dopo dieci strisciate, ritrovarsi
    // in cima è perdere il posto.
    LaunchedEffect(items) {
        if (placed || items == null) return@LaunchedEffect
        placed = true
        if (startAt in items.indices) state.scrollToItem(startAt)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1
                )
                items?.let {
                    Text(
                        text = pluralStringResource(R.plurals.folders_count, it.size, it.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        when {
            items == null -> CircularProgressIndicator(
                Modifier.padding(top = 24.dp).size(28.dp).align(Alignment.CenterHorizontally)
            )

            items.isEmpty() -> Text(
                text = stringResource(R.string.folder_empty),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp, start = 12.dp)
            )

            else -> LazyVerticalGrid(
                // ⚠️ `Adaptive` e non un numero fisso di colonne: la stessa misura
                // minima dà tre colonne su un telefono e sei su un tablet o in
                // orizzontale, senza un ramo per ogni forma di schermo.
                columns = GridCells.Adaptive(minSize = THUMB),
                state = state,
                horizontalArrangement = Arrangement.spacedBy(GAP),
                verticalArrangement = Arrangement.spacedBy(GAP),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(
                    items = items,
                    // ⚠️ La chiave è l'indirizzo e non la posizione: senza, ruotando
                    // il telefono le miniature già decodificate si rimescolerebbero
                    // fra i riquadri.
                    key = { _, uri -> uri.toString() }
                ) { index, uri ->
                    AsyncImage(
                        model = uri,
                        // Ogni riquadro è toccabile, quindi non è decorativo: chi
                        // legge con TalkBack deve sapere dove si trova nella cartella.
                        contentDescription = stringResource(
                            R.string.grid_item, index + 1, items.size
                        ),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            // Il fondo si vede finché la miniatura non è pronta: senza,
                            // la griglia lampeggerebbe del colore della pagina.
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onOpen(index) }
                    )
                }
            }
        }
    }
}

/**
 * Il lato minimo di una miniatura.
 *
 * ⚠️ È una misura, non un gusto: a 108dp uno schermo da 360dp di larghezza tiene
 * **tre** colonne con i distacchi, che è la densità delle gallerie di sistema; a 96
 * ne terrebbe quattro, e su un telefono la faccia in una foto di gruppo non si
 * riconosce più.
 */
private val THUMB = 108.dp

/** Il distacco fra le miniature: c'è, ma non deve leggersi come una cornice. */
private val GAP = 3.dp

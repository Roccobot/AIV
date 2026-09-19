package io.github.roccobot.aiv

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * I due comandi del **salvataggio** in testata ai due editor: 'Filigrana' e 'Ridimensiona'.
 *
 * ⚠️⚠️ **I DUE GESTI SONO QUESTI DALLA `2.72`, ED È LA SUA ISTRUZIONE** (riscontro del giro
 * della `2.70`, voce `resize` accettabile: *Il pulsante deve funzionare come l'altro tasto che
 * ho descritto prima: tocco normale = on/off. Tocco prolungato = imposti il ridimensionamento*;
 * e sulla voce `filigrana`: *aggiungi un'icona 'Filigrana' in alto a destra, prima di
 * 'Ridimensiona', che si accende o spegne con un tap normale. Il tap prolungato porta alle
 * impostazioni della filigrana*). Quindi il tocco fa la cosa che si fa spesso, cioè accendere e
 * spegnere, e il gesto lungo porta quella che si fa una volta.
 * ⚠️⚠️ **NELLA `2.70` IL RIDIMENSIONAMENTO LI AVEVA ROVESCIATI**, cioè il tocco apriva la
 * finestra e il gesto lungo spegneva: chi legge quella nota in un commento vecchio sappia che
 * oggi i due tasti si comportano allo stesso modo, che è quello che lui ha chiesto.
 *
 * ⚠️⚠️ **UN PEZZO SOLO PER TUTTI E DUE, E NON È UN RISPARMIO DI RIGHE**: sono due tasti gemelli
 * a mezzo centimetro di distanza nella stessa testata, quindi due disegni separati divergono al
 * primo ritocco e chi lo vedrebbe per primo è lui. È lo stesso criterio per cui le squadrette
 * del ritaglio dell'editor completo chiamano quelle di casa.
 *
 * ⚠️ **L'accento dice se è acceso**, e l'inchiostro attenuato se il comando è spento del tutto:
 * un tasto che non porta un tondo non ha un altro modo di dirlo.
 */
@Composable
private fun EditorTool(
    icon: ImageVector,
    /** Come si chiama: è la descrizione parlata, e la parola che l'app usa per questa funzione. */
    label: String,
    /** Se si applica al salvataggio. */
    on: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onSetup: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(TOOL_TOUCH)
            .clip(CircleShape)
            .combinedClickable(
                enabled = enabled,
                role = Role.Switch,
                // ⚠️ L'etichetta del gesto lungo è quella della schermata a cui porta, e non una
                // stringa nuova: 'Impostazioni' esiste in tutte e ventotto le lingue e dice
                // esattamente dove si va. Il conto delle stringhe si fa prima di cominciare, che è
                // la regola di `AIV/CLAUDE.md` § '⚙️ Dove va un'impostazione, e chi la deve trovare'.
                onLongClickLabel = stringResource(R.string.settings_title),
                onLongClick = {
                    haptics.performHapticFeedback(HOLD_BUZZ)
                    onSetup()
                },
                onClick = onToggle
            )
            /*
             * ⚠️⚠️ **LO STATO LO ANNUNCIA `toggleableState`, E SENZA DI LUI `Role.Switch` NON DICE
             * NIENTE**: un lettore di schermo legge 'acceso' o 'spento' solo se quel dato c'è nella
             * semantica, e `Modifier.toggleable`, che lo metterebbe da sé, non offre il secondo
             * gesto. Costa una riga e nessuna stringa, perché le due parole le mette Android.
             */
            .semantics { toggleableState = ToggleableState(on) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = OFF_INK)
                on -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/**
 * Il comando **'Filigrana'**, che nella fila viene prima di 'Ridimensiona' come ha chiesto.
 *
 * ⚠️⚠️ **SENZA UN LOGO SCELTO IL TASTO NON C'È, E IL PRECEDENTE È 'MOSTRA NASCOSTE'**: senza un
 * file adottato non c'è niente da accendere né da spegnere, e un interruttore che non cambia
 * nessuna immagine è un comando che non fa niente. È lo stesso criterio per cui quella voce del
 * menu compare **se e solo se** una cartella è nascosta (`AIV/CLAUDE.md` § '👁️ Mostra nascoste, e
 * perché dura un minuto').
 * ⚠️ **Quindi la porta per sceglierlo resta quella di sempre**, cioè le impostazioni: è la strada
 * che la sua specifica della `2.69` descrive (*è configurabile dalle impostazioni, sezione
 * editor*), e chi un logo ce l'ha si ritrova anche la scorciatoia del gesto lungo.
 *
 * ⚠️⚠️ **IL GLIFO RESTA QUELLO DI MATERIAL, E NON È UNA COSA RIMANDATA: È UNA MISURA** (punto 4
 * del campo libero del giro della `2.70`: *le icone di 'Filigrana' e 'Ridimensiona' possono
 * venire da Material ma vanno arrotondate come da regola nuova*). Questa cornice è già stondata e
 * il rettangolino dentro è un buco, quindi di angoli convessi **esterni** non ne ha nemmeno uno e
 * l'arrotondamento la lascerebbe a **zero** pixel di scarto. La regola di `AIV/CLAUDE.md`
 * § '🖌️ Come entra un disegno' dice che a zero pixel vince Material: un file in `res/` sarebbe
 * una seconda copia dello stesso disegno. Il gemello, 'Ridimensiona', ne aveva quarantotto ed è
 * entrato.
 */
@Composable
fun MarkButton(
    /** Se un logo è stato scelto: senza, il tasto non si disegna affatto. */
    has: Boolean,
    /** Se la filigrana si scrive al salvataggio, cioè l'interruttore delle impostazioni. */
    on: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onSetup: () -> Unit
) {
    if (!has) return
    EditorTool(
        icon = MARK_GLYPH,
        label = stringResource(R.string.settings_mark),
        on = on,
        enabled = enabled,
        onToggle = onToggle,
        onSetup = onSetup
    )
}

/**
 * Il comando **'Ridimensiona'**, dalla `2.70`.
 *
 * ⚠️⚠️ **'UN TASTO PIÙ UN INTERRUTTORE' SU UN BERSAGLIO SOLO, ED È UNA LETTURA DICHIARATA**
 * (sua risposta `editor` a `d-resize-dove`): la sua frase ne descrive due, e in testata non
 * entrano. Il conto, su uno schermo da 360 punti: il tasto Indietro ne prende 48, 'Salva' una
 * settantina, questa icona 48, e uno `Switch` di Material altri 52; al titolo, che è l'unico a
 * cedere, ne resterebbe un centinaio. Quindi i gesti sono due sullo stesso tasto, che è il modo
 * di questa app (è la regola di `SaveButton` e dei gettoni dei moduli).
 * ⚠️⚠️ **E DALLA `2.72` IL TITOLO DICE 'MODIFICA' E LE ICONE SONO DUE**: quel conto è la ragione
 * per cui lui ha accorciato il titolo nello stesso giro in cui ha chiesto il secondo tasto.
 *
 * ⚠️ **Il piano esiste sempre**, anche spento, quindi accendere non chiede di configurare
 * niente: chi non l'ha mai toccato accende quello di fabbrica, e se su quell'immagine non
 * rimpicciolisce, 'Salva' resta spento e la finestra lo dice.
 *
 * ⚠️ **Il glifo è in `res/` dalla `2.73`**, cioè quello di Material **ammorbidito** come ha
 * chiesto: qui gli spigoli vivi erano quarantotto, quindi il raccordo cambia davvero il disegno.
 * La misura e il perché di ogni raccordo vivono in testa a `res/drawable/ic_resize.xml`.
 */
@Composable
fun ResizeButton(
    /** Se il ridimensionamento si applica al salvataggio: l'icona prende l'accento. */
    on: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onSetup: () -> Unit
) {
    EditorTool(
        icon = Glyphs.Resize,
        label = stringResource(R.string.look_resize),
        on = on,
        enabled = enabled,
        onToggle = onToggle,
        onSetup = onSetup
    )
}

/**
 * Il disegno della filigrana scelta, pronto per il palco, o `null` se non c'è niente da mostrare.
 *
 * ⚠️⚠️ **DALLA `2.74`, ED È SUA RICHIESTA** (punto 5 del campo libero del giro della `2.70`:
 * *un'anteprima della filigrana (se attiva) nella posizione e con l'opacità corrette sull'immagine
 * dell'editor, solo a zoom adattato, dietro un interruttore nuovo 'Mostra nell'editor'*).
 *
 * ⚠️⚠️ **IL DISEGNO SI LEGGE UNA VOLTA SOLA E A UNA MISURA FISSA, E A RIMPICCIOLIRLO È IL PALCO**:
 * i quattro numeri del piano cambiano mentre si trascina un cursore nelle impostazioni, e un file
 * riletto a ogni cambiamento vorrebbe dire decodificare un SVG sessanta volte al secondo. È la
 * stessa scelta dell'anteprima delle impostazioni, e qui vale di più, perché il palco ridisegna a
 * ogni fotogramma di panoramica.
 * ⚠️ **La chiave è 'c'è una firma da mostrare' e non il piano**: quello che si legge dal disco è
 * il disegno nudo, che non dipende da dove cade né da quanto è grande.
 */
@Composable
fun rememberMarkArt(plan: Watermark.Plan?): ImageBitmap? {
    val context = LocalContext.current
    val art by produceState<ImageBitmap?>(null, plan != null) {
        value = if (plan == null) null else withContext(Dispatchers.IO) {
            Watermark.artwork(context, MARK_ART)?.asImageBitmap()
        }
    }
    return art
}

/**
 * La firma disegnata dentro [frame], cioè dove cadrà sull'immagine che si salverà.
 *
 * ⚠️⚠️ **IL CONTO È QUELLO DEL SALVATAGGIO, CHIAMATO E NON RICOPIATO**: la misura viene da
 * [Watermark.sideFor] e il posto da [Watermark.cornerFor], che sono le stesse due funzioni che
 * [Watermark.stamp] usa sul file. Riscritte qui, l'anteprima direbbe il vero fino al primo
 * ritocco, e da lì in poi mostrerebbe una firma in un posto e il file la scriverebbe in un altro,
 * che è il difetto peggiore che un'anteprima possa avere.
 * ⚠️ **[frame] è il riquadro dell'immagine FINALE**, cioè della porzione che il ritaglio tiene:
 * la firma si scrive dopo il taglio, quindi il suo angolo è quello del rettangolo tagliato e non
 * quello della fotografia intera.
 * ⚠️ **L'opacità è quella del piano**, cioè la stessa che il pennello del salvataggio mette sul
 * proprio `Paint`.
 */
fun DrawScope.markOverlay(frame: Rect, art: ImageBitmap, plan: Watermark.Plan) {
    if (frame.width <= 0f || frame.height <= 0f) return
    val long = max(art.width, art.height)
    if (long <= 0) return
    val side = Watermark.sideFor(plan, max(frame.width, frame.height))
    val k = side / long
    val wide = art.width * k
    val high = art.height * k
    val corner = Watermark.cornerFor(plan, frame.width, frame.height, wide, high)
    drawImage(
        image = art,
        dstOffset = IntOffset(
            (frame.left + corner.x).roundToInt(),
            (frame.top + corner.y).roundToInt()
        ),
        // ⚠️ Almeno un pixel per lato: alla misura minima su un palco piccolo il conto cade sotto
        // l'unità, e un riquadro di lato zero non disegna niente invece di una firma piccola.
        dstSize = IntSize(wide.roundToInt().coerceAtLeast(1), high.roundToInt().coerceAtLeast(1)),
        alpha = plan.alpha / 100f,
        // ⚠️ Il filtro serve perché il disegno arriva a una misura fissa e qui si rimpicciolisce:
        // senza, i suoi bordi si scalinerebbero.
        filterQuality = FilterQuality.Medium
    )
}

/** Il bersaglio di un comando in testata, come quello di un `IconButton` di Material. */
private val TOOL_TOUCH = 48.dp

/**
 * A che lato lungo si disegna la filigrana per il palco.
 *
 * ⚠️ **Più larga di quanto servirà**: la firma arriva al massimo a metà del lato lungo del riquadro
 * ([Watermark.SIZE]), e su un telefono quel riquadro non supera il migliaio di pixel, quindi qui
 * c'è margine anche a fondo corsa. Chiederne di più costerebbe memoria per un'anteprima.
 */
private const val MARK_ART = 768

/**
 * Il glifo di 'Filigrana', che leggono il tasto **e** il velo che lo insegna.
 *
 * ⚠️ **Vive qui e non in [Glyphs], perché quello è il catalogo dei disegni di `res/`** e questo è
 * di Material: il perché resti di Material è la misura scritta su [MarkButton]. ⚠️ **Ed è una
 * costante e non due chiamate**, per la ragione di sempre: dalla `2.73` il mini-onboarding della
 * testata ne disegna una copia in arancione, e due `Icons.Filled` scritte in due file divergono
 * il giorno che una delle due cambia. Il gemello non ne ha bisogno, perché `Glyphs.Resize` è già
 * una fonte sola.
 */
val MARK_GLYPH: ImageVector get() = Icons.Filled.BrandingWatermark

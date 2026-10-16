package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Il velo del mini onboarding: oscura la schermata, dice la frase e mette in evidenza una
 * copia funzionante del FAB che sta insegnando (richiesta dell'utente). Serve perché un FAB non
 * dichiara i propri gesti, e il tocco lungo non si scopre da solo. Colore, contrasto e geometria
 * vivono qui una volta sola; cambiano la frase e il FAB.
 *
 * ⚠️⚠️ **La copia evidenziata funziona**: chi tiene premuto sul velo fa la cosa mentre gliela si
 * spiega. Per questo è lo stesso [TapHoldFab] del FAB vero, alla stessa misura e nello stesso
 * angolo, e cade sopra l'originale.
 *
 * ⚠️⚠️ **Il velo copre tutto lo schermo**, testata e margini di sistema compresi: nasce nel `Box`
 * che avvolge la schermata, ed è un'estensione di [BoxScope] perché senza un `Box` intorno
 * `matchParentSize` non esiste e il velo coprirebbe solo il suo pezzo.
 *
 * Chi chiude il velo senza leggerlo non scopre la scorciatoia: la rete è l'etichetta che il
 * lettore di schermo legge sul tocco lungo (`holdLabel` di [TapHoldFab]).
 */
@Composable
fun BoxScope.HintVeil(
    text: String,
    /**
     * I rientri che posano la copia del FAB esattamente sopra l'originale.
     *
     * ⚠️ **Sono l'unica cosa che il velo non può ricavare da sé**: il FAB vero vive dentro il
     * rientro di sistema più i margini della sua schermata, e il velo nasce fuori da tutti. Chi
     * ne dimentica uno vede la copia spostata. È un `Modifier` perché le catene cambiano da una
     * schermata all'altra.
     */
    inset: Modifier,
    onDone: () -> Unit,
    fab: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(HINT_SCRIM)
            // Niente increspatura e nessuna descrizione: non è un tasto, è il velo, e un tocco
            // qualunque lo archivia.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDone
            )
    ) {
        /*
         * Il velo segue il lato del FAB, che è quello che spiega.
         */
        val destra = LocalPadLook.current.hand == Hand.RIGHT
        Column(
            modifier = Modifier
                .align(if (destra) Alignment.BottomEnd else Alignment.BottomStart)
                .then(inset),
            horizontalAlignment = if (destra) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(HINT_GAP)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                /*
                 * ⚠️ **Anche il paragrafo segue la mano**, non solo la colonna: la frase supera
                 * [HINT_WIDTH] e va a capo, quindi la scatola misura la larghezza piena e le
                 * righe si appoggerebbero al fianco opposto a quello della colonna.
                 */
                textAlign = if (destra) TextAlign.End else TextAlign.Start,
                modifier = Modifier.widthIn(max = HINT_WIDTH)
            )
            /*
             * La copia è sempre piena (voce `4.34-02`): il suo colore è del velo e non dell'app,
             * quindi non segue l'aspetto scelto per i pulsanti, e col vetro arriverebbe velato.
             */
            val look = LocalPillLook.current
            CompositionLocalProvider(LocalPillLook provides look.copy(fill = PillFill.SOLID)) { fab() }
        }
    }
}

/**
 * Il velo che evidenzia un pezzo qualunque della schermata, dov'è, e scrive la frase sotto (per
 * esempio l'icona dell'intestazione, che sceglie la copertina).
 *
 * ⚠️⚠️ **Il posto non si ricalcola, si misura**: rifare qui la catena dei rientri sarebbe una
 * seconda geometria da tenere allineata, e un ritocco all'intestazione farebbe cadere
 * l'evidenziazione sul vuoto. Chi chiama passa il riquadro dell'oggetto vero, letto con
 * `onGloballyPositioned`. L'origine del velo si sottrae, perché il riquadro è in coordinate della
 * radice.
 *
 * ⚠️ **La copia è un disegno e non un tasto**: qui il gesto è già stato fatto (il velo compare
 * dopo il tocco), e una copia che risponde lo rifarebbe. Un tocco qualunque lo archivia.
 */
@Composable
fun BoxScope.HintSpot(
    text: String,
    /** Dov'è la cosa da evidenziare, in coordinate della radice. */
    spot: Rect,
    glyph: ImageVector,
    onDone: () -> Unit
) {
    HintSpots(
        groups = listOf(
            HintGroup(
                text = text,
                spots = listOf(
                    spot to { Icon(glyph, null, tint = HINT_MARK, modifier = Modifier.fillMaxSize()) }
                )
            )
        ),
        onDone = onDone
    )
}

/**
 * Un gruppo di cose da evidenziare insieme, con la frase che le riguarda: quando i tasti spiegati
 * da un velo sono ai due capi dello schermo, ogni gruppo ha la sua frase vicina (richiesta
 * dell'utente).
 */
class HintGroup(
    val text: String,
    /** Dove sono le cose da evidenziare, in coordinate della radice, e che cosa disegnarci sopra. */
    val spots: List<Pair<Rect, @Composable () -> Unit>>
)

/**
 * Lo stesso velo di [HintSpot] quando le cose da evidenziare sono più di una, in uno o più
 * gruppi ([HintGroup]), per esempio i tasti della testata dell'editor.
 *
 * ⚠️⚠️ **Ogni cosa ha il proprio riquadro e il proprio disegno, non una cella calcolata**: i
 * tasti non sono larghi uguali, e dividere il riquadro comune in parti uguali poserebbe le copie
 * accanto agli originali. Il disegno lo passa chi chiama, perché non è sempre un glifo ('Salva' è
 * una parola).
 *
 * La frase di un gruppo si posa oltre il suo riquadro più lontano, o coprirebbe una copia.
 * ⚠️ **Da che parte va lo dice una regola e non un parametro**: un gruppo nella metà di sopra ha
 * la frase sotto, uno nella metà di sotto la ha sopra. È la sola disposizione che tenga la frase
 * dentro lo schermo in tutti e due i casi.
 */
@Composable
fun BoxScope.HintSpots(
    /** I gruppi da evidenziare, ognuno con la sua frase. */
    groups: List<HintGroup>,
    onDone: () -> Unit
) {
    var origine by remember { mutableStateOf(Offset.Zero) }
    var altezza by remember { mutableStateOf(0) }
    Box(
        modifier = Modifier
            .matchParentSize()
            .onGloballyPositioned {
                origine = it.positionInRoot()
                altezza = it.size.height
            }
            .background(HINT_SCRIM)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDone
            )
    ) {
        with(LocalDensity.current) {
            // ⚠️ Un gruppo senza riquadri si salta: nel fotogramma prima della misura può essere
            // vuoto, e `minOf` e `maxOf` su un elenco vuoto vanno in errore.
            groups.filter { it.spots.isNotEmpty() }.forEach { gruppo ->
                gruppo.spots.forEach { (dove, disegno) ->
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (dove.left - origine.x).toDp(),
                                y = (dove.top - origine.y).toDp()
                            )
                            .size(width = dove.width.toDp(), height = dove.height.toDp()),
                        contentAlignment = Alignment.Center
                    ) {
                        disegno()
                    }
                }
                val cima = gruppo.spots.minOf { it.first.top } - origine.y
                val fondo = gruppo.spots.maxOf { it.first.bottom } - origine.y
                val frase: @Composable () -> Unit = {
                    Text(
                        text = gruppo.text,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = HINT_SIDE).widthIn(max = HINT_WIDTH)
                    )
                }
                if ((cima + fondo) / 2 < altezza / 2f) {
                    Box(
                        modifier = Modifier.align(Alignment.TopCenter).offset(y = fondo.toDp() + HINT_GAP)
                    ) {
                        frase()
                    }
                } else {
                    /*
                     * Sopra il gruppo la frase si appoggia in fondo a un riquadro alto fin lì: la
                     * sua altezza dipende da quanto va a capo, una misura che qui non si ha.
                     */
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height((cima.toDp() - HINT_GAP).coerceAtLeast(0.dp)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        frase()
                    }
                }
            }
        }
    }
}

/**
 * Il velo che evidenzia una fila che continua fuori dallo schermo (la fila dei moduli
 * dell'editor completo): le sue icone in arancione, che sfumano nel velo verso destra, una
 * freccia che indica il verso, e la frase sopra. Il disegno è dell'utente.
 *
 * ⚠️⚠️ **Le icone sono una copia disegnata**: la fila vera è dentro la scheda, sotto il velo.
 * La copia cade sopra l'originale perché usa lo stesso riquadro misurato e la stessa misura di
 * cella ([modCell]), una funzione sola letta da tutti e due.
 * Non risponde al tocco: il gesto da imparare è uno scorrimento, e il velo si chiude al primo
 * tocco.
 */
@Composable
fun BoxScope.HintStrip(
    text: String,
    /** Dov'è la fila da evidenziare, in coordinate della radice. */
    spot: Rect,
    /** I glifi della fila, nell'ordine in cui si vedono. */
    icons: List<ImageVector>,
    onDone: () -> Unit
) {
    var origine by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .matchParentSize()
            .onGloballyPositioned { origine = it.positionInRoot() }
            .background(HINT_SCRIM)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDone
            )
    ) {
        with(LocalDensity.current) {
            val x = (spot.left - origine.x).toDp()
            val y = (spot.top - origine.y).toDp()
            val larga = spot.width.toDp()
            val alta = spot.height.toDp()
            /*
             * ⚠️ **La sfumatura toglie opacità, non dipinge sopra**: un rettangolo del colore del
             * velo spegnerebbe le icone verso un nero che sotto non c'è. Con `DstIn` su un livello
             * a parte sbiadisce la copia, e verso destra resta il velo sopra la fila vera.
             */
            Row(
                modifier = Modifier
                    .offset(x = x, y = y)
                    .size(width = larga, height = alta)
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.Black,
                                HINT_FADE to Color.Black,
                                1f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    },
                horizontalArrangement = Arrangement.spacedBy(MOD_GAP)
            ) {
                val cella = modCell(larga, icons.size)
                icons.forEach { glifo ->
                    Box(
                        modifier = Modifier.size(width = cella, height = alta),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = glifo, contentDescription = null, tint = HINT_MARK)
                    }
                }
            }
            /*
             * La freccia è fuori dalla fila, al bordo destro: dentro la sfumatura sparirebbe
             * proprio dalla parte in cui indica.
             */
            Icon(
                imageVector = Glyphs.Material.ArrowForward,
                contentDescription = null,
                tint = HINT_MARK,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = y + (alta - HINT_ARROW) / 2)
                    .padding(end = HINT_ARROW_SIDE)
                    .size(HINT_ARROW)
            )
            /*
             * Il testo si posa in fondo a una scatola che finisce dove comincia la fila: resta
             * appena sopra le icone senza conoscere la propria altezza.
             */
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(y)
                    .padding(horizontal = HINT_SIDE, vertical = HINT_GAP),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = HINT_WIDTH)
                )
            }
        }
    }
}

/**
 * Il velo che dice una cosa e basta: frase in mezzo allo schermo, niente da evidenziare. Serve
 * a un gesto che non ha un posto, come il doppio tocco sull'immagine intera (richiesta
 * dell'utente): una copia evidenziata coprirebbe proprio la cosa di cui si parla.
 * Condivide con [HintVeil] il velo e il suo contrasto. Un tocco qualunque lo archivia.
 */
@Composable
fun BoxScope.HintCentre(text: String, onDone: () -> Unit) {
    // ⚠️ `matchParentSize`: dentro un `Box` prende la misura del genitore senza entrare nel suo
    // conto, e il genitore qui è una schermata intera.
    CentredHint(text = text, onDone = onDone, modifier = Modifier.matchParentSize())
}

/**
 * Il velo del Feed: uno schizzo del flusso che dice dove sono le immagini più recenti e le meno
 * recenti.
 *
 * ⚠️ **Il disegno è dell'utente** (nota sulla voce `6.02-04`): quattro livelli, cioè il fondo
 * della pagina, che copre le immagini vere; lo schizzo in [HINT_KEY]; il velo; le due celle piene
 * con 'Più recenti' e 'Meno recenti', e la frase, che è il testo dell'impostazione 'Disposizione
 * in vista feed'.
 * Lo schizzo ha tre colonne qualunque cosa mostri il Feed: è un'immagine dell'ordine, non della
 * griglia, e le celle smettono di crescere a [SKETCH_CELL_MAX]. La geometria è [feedSketch], una
 * funzione pura letta dai due livelli.
 * La frase è fra le due celle piene; con una riga sola (telefono in orizzontale) va sotto. Un
 * tocco qualunque lo chiude.
 */
@Composable
fun BoxScope.HintFeed(text: String, newer: String, older: String, onDone: () -> Unit) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDone
            )
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            feedSketch(maxWidth, maxHeight).cells.forEach { SketchCell(it) }
        }
        Box(Modifier.fillMaxSize().background(HINT_SCRIM))
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val sketch = feedSketch(maxWidth, maxHeight)
            SketchCell(sketch.newest, newer)
            SketchCell(sketch.oldest, older)
            val sameRow = sketch.oldest.y == sketch.newest.y
            val top = sketch.newest.y + sketch.newest.size + HINT_GAP
            val bottom = if (sameRow) maxHeight else sketch.oldest.y - HINT_GAP
            Box(
                modifier = Modifier
                    .offset(y = top)
                    .fillMaxWidth()
                    .height((bottom - top).coerceAtLeast(0.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = HINT_SIDE).widthIn(max = HINT_WIDTH)
                )
            }
        }
    }
}

/** Una cella dello schizzo del Feed, al suo posto: vuota, o piena con una parola dentro. */
@Composable
private fun SketchCell(cell: SketchSquare, word: String? = null) {
    Box(
        modifier = Modifier
            .offset(x = cell.x, y = cell.y)
            .size(cell.size)
            .background(HINT_KEY, RoundedCornerShape(SKETCH_CORNER)),
        contentAlignment = Alignment.Center
    ) {
        word?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleSmall,
                color = HINT_KEY_INK,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

/** Un quadrato dello schizzo del Feed: l'angolo e il lato, in dp dall'angolo dell'area sicura. */
data class SketchSquare(val x: Dp, val y: Dp, val size: Dp)

/**
 * Lo schizzo del Feed: tutte le celle che si vedono, la più recente (in alto a sinistra) e la
 * meno recente (a destra nell'ultima riga intera).
 *
 * Una riga tagliata dal bordo si disegna ma non si sceglie: la griglia continua sotto il bordo,
 * come un flusso, ma 'quella più in basso a destra fra le visibili' è una cella intera.
 */
class FeedSketch(val cells: List<SketchSquare>, val newest: SketchSquare, val oldest: SketchSquare)

/** La geometria di [HintFeed] in un'area sicura [width] per [height]: tre colonne, centrate. */
fun feedSketch(width: Dp, height: Dp): FeedSketch {
    val cell = ((width - SKETCH_SIDE * 2 - SKETCH_GAP * (SKETCH_COLUMNS - 1)) / SKETCH_COLUMNS)
        .coerceIn(0.dp, SKETCH_CELL_MAX)
    val left = (width - cell * SKETCH_COLUMNS - SKETCH_GAP * (SKETCH_COLUMNS - 1)) / 2
    val step = cell + SKETCH_GAP
    // Le righe che entrano intere, almeno una, più quella tagliata dal bordo di sotto.
    val whole = ((height - SKETCH_SIDE + SKETCH_GAP) / step).toInt().coerceAtLeast(1)
    val cells = (0..whole).flatMap { row ->
        (0 until SKETCH_COLUMNS).map { col ->
            SketchSquare(x = left + step * col, y = SKETCH_SIDE + step * row, size = cell)
        }
    }
    return FeedSketch(
        cells = cells,
        newest = cells.first(),
        oldest = cells[whole * SKETCH_COLUMNS - 1]
    )
}

/** Le colonne dello schizzo del Feed: il numero è dell'utente. */
private const val SKETCH_COLUMNS = 3

/** Il margine intorno allo schizzo, l'aria fra le celle e la stondatura. */
private val SKETCH_SIDE = 16.dp
private val SKETCH_GAP = 8.dp
private val SKETCH_CORNER = 12.dp

/** La cella più grande dello schizzo: oltre, un tablet mostrerebbe due righe di quadrati giganti. */
private val SKETCH_CELL_MAX = 160.dp

/**
 * Il corpo di un velo centrato: il velo, il testo e i suoi margini.
 *
 * Lo usano [HintCentre] e [HintNotice], che differiscono solo per la finestra: il primo si stende
 * sul `Box` che lo contiene, il secondo apre una finestra propria per stare sopra un dialogo. La
 * misura del velo è il parametro.
 */
@Composable
private fun CentredHint(text: String, onDone: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .background(HINT_SCRIM)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDone
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = HINT_SIDE).widthIn(max = HINT_WIDTH)
        )
    }
}

/**
 * Lo stesso velo centrato, ma sopra un dialogo: una finestra sua, che copre lo schermo.
 *
 * ⚠️⚠️ **Un velo dentro un dialogo coprirebbe il dialogo e non lo schermo**: [HintCentre] si
 * stende sul `Box` che lo contiene, e dentro una finestra (per esempio l'avviso sul cambio di
 * estensione, dalla rinomina) sarebbe un riquadro scuro largo come lei. Una finestra propria è
 * l'unico modo di stare sopra un'altra finestra. Il disegno lo fa [CentredHint]. Un tocco
 * qualunque lo archivia.
 */
@Composable
fun HintNotice(text: String, onDone: () -> Unit) {
    Dialog(
        onDismissRequest = onDone,
        /*
         * ⚠️ **[fullWindow] e non la sola larghezza**: con un decoro adattato alle barre di
         * sistema il velo si fermerebbe prima di loro e lascerebbe due bande scoperte. Niente
         * `safeDrawingPadding()`, l'eccezione dichiarata su [fullWindow]: il testo è al centro.
         */
        properties = fullWindow()
    ) {
        // Qui `fillMaxSize`, perché la finestra è sua e la deve riempire tutta.
        CentredHint(text = text, onDone = onDone, modifier = Modifier.fillMaxSize())
    }
}

/**
 * Il velo del mini onboarding.
 *
 * ⚠️ **Il 70% di nero**: sotto c'è una griglia di immagini, il fondo più chiassoso che ci sia, e
 * a metà velo le miniature continuano a chiamare l'occhio. Col 70% il bianco del testo misura
 * 8,45 anche sull'immagine più chiara possibile.
 */
private val HINT_SCRIM = Color(0xB3000000)

/**
 * La distanza della frase dai bordi, nei veli in cui la frase non è appoggiata al FAB: senza, su
 * un telefono stretto una frase lunga toccherebbe i due bordi. Il velo col FAB ha il margine del
 * rientro del FAB.
 */
private val HINT_SIDE = 32.dp

/**
 * L'arancione della copia evidenziata (richiesta dell'utente): l'accento dell'app è verde acqua,
 * già dappertutto, e un velo che evidenzia con quel colore non evidenzia niente. Misurato: 4,35
 * sul velo steso sull'immagine più chiara possibile, sopra il 3:1 delle grafiche non testuali, e
 * 10,81 nel caso normale.
 */
val HINT_MARK = Color(0xFFFFA726)

/** Il glifo sopra l'arancione: misurato 7,29. */
val HINT_INK = Color(0xFF3E2600)

/**
 * Il giallo della copia del tondo sul menu 'Start' aperto del velo, e il suo glifo: i colori del
 * mockup dell'utente (voce `4.33-05`), campionati dal file. Arancione sul pannello arancione non
 * si leggerebbe come un tasto. Misurato: il glifo sul giallo 7,15; il giallo sull'arancione 1,42,
 * che basta a staccare una forma piena.
 */
val HINT_KEY = Color(0xFFFFDA3C)
val HINT_KEY_INK = Color(0xFF643A00)

/** La distanza fra la frase e la cosa che indica: abbastanza da non sembrarne parte. */
private val HINT_GAP = 14.dp

/**
 * La larghezza massima della frase del velo: senza, in tedesco sarebbe una riga sola da bordo a
 * bordo, e su un telefono stretto andrebbe a capo dove capita.
 */
private val HINT_WIDTH = 260.dp

/**
 * Da dove comincia a sbiadire la copia di una fila che continua fuori dallo schermo: poco più di
 * metà fila, perché la sfumatura dica 'ce n'è ancora' senza spegnere le prime icone.
 */
private const val HINT_FADE = 0.55f

/** Il lato della freccia che indica il verso dello scorrimento. */
private val HINT_ARROW = 28.dp

/** La distanza della freccia dal bordo dello schermo. */
private val HINT_ARROW_SIDE = 4.dp

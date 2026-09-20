package io.github.roccobot.aiv

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp

/**
 * I due comandi del **salvataggio** dei due editor: 'Filigrana' e 'Ridimensiona'.
 *
 * ⚠️⚠️ **DALLA `2.79` VIVONO NELLA BARRA IN BASSO E NON PIÙ IN TESTATA, ED È SUA RICHIESTA**
 * (punto C del campo libero del giro dalla `2.75` alla `2.77`: *ci ho ripensato, i tasti
 * 'Filigrana' e 'Ridimensiona' sono troppo lontani e poco raggiungibili dal pollice: mettili
 * nella barra delle funzioni in basso, non del tutto a sinistra*). Il pezzo che li disegna non
 * cambia: cambia dove la schermata lo mette, e il come vive su [EditorToolBar].
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
 * ⚠️⚠️ **IL GLIFO È IN `res/` DALLA `2.79`, E FINO ALLA `2.78` ERA DI MATERIAL**: è lo stesso
 * disegno **specchiato in orizzontale**, cioè col rettangolino nell'angolo in basso a sinistra,
 * ed è il punto B del suo campo libero (*è ciò che associo istantaneamente al concetto di
 * filigrana perché di solito la metto lì*). ⚠️ **Quello che cambia è quale metà della regola
 * vince**: a zero pixel di scarto vince Material, e l'arrotondamento a 0,4 qui non toccava niente
 * (di angoli convessi esterni non ne ha nemmeno uno, perché il rettangolino è un buco); uno
 * specchio invece cambia il 18,75% della tela, quindi il disegno diventa un file. La misura vive
 * in testa a `res/drawable/ic_watermark.xml`.
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
        icon = Glyphs.Watermark,
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
 * ⚠️⚠️ **DALLA `2.79` QUEL CONTO NON SERVE PIÙ, E IL TITOLO RESTA CORTO LO STESSO**: i due tasti
 * sono scesi nella barra in basso, quindi in testata lo spazio è tornato; ma 'Modifica' è una
 * sua parola e non un rimedio, e non si rovescia un'istruzione perché la sua ragione è caduta.
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

/*
 * ⚠️⚠️ **QUI VIVEVANO `rememberMarkArt` E `markOverlay`, L'ANTEPRIMA DELLA FIRMA SUL PALCO, E
 * DALLA `2.75` NON CI SONO PIÙ**: erano nate nella `2.74` su sua richiesta (punto 5 del campo
 * libero del giro della `2.70`) e il giro dopo le ha revocate (voce `mark-palco` non approvata:
 * *In realtà funziona bene, ma mi sono accorto che non serve, e forse confonde pure.
 * Funzionalità da togliere*). Quindi non c'è un difetto da correggere: c'è una funzione provata e
 * scartata, e con lei se ne va tutto quello che esisteva per lei (l'interruttore 'Mostra
 * nell'editor', la sua chiave, `ViewerViewModel.stageMark`, la soglia dello zoom a riposo e le
 * prove del banco).
 * ⚠️ **Il riquadro delle impostazioni resta**, ed è l'anteprima che lui tiene: là la firma si
 * vede su un fondo neutro mentre si tarano i quattro numeri, che è il posto in cui quei numeri si
 * scelgono. Chi volesse rimettere quella sul palco la ritrova nella storia git.
 */

/**
 * La coppia di comandi appoggiata alla barra in basso, dalla `2.79`.
 *
 * ⚠️⚠️ **VIVE NELLA BARRA E NON IN TESTATA PERCHÉ LÀ IL POLLICE NON ARRIVA, ED È SUA RICHIESTA**
 * (punto C del campo libero del giro dalla `2.75` alla `2.77`, col mockup: *sono troppo lontani e
 * poco raggiungibili dal pollice: mettili nella barra delle funzioni in basso, non del tutto a
 * sinistra*). In testata ci sono arrivati con la `2.72` e ci sono rimasti sette versioni.
 *
 * ⚠️⚠️ **IL BLOCCO CAMBIA LATO COL FAB, E L'ORDINE DEI DUE SI SPECCHIA CON LUI**: la seconda metà
 * della sua richiesta dice *con il FAB sul lato opposto, anche 'Filigrana' e 'Ridimensiona'
 * cambiano posizione e passano a destra, lasciando un po' di spazio dopo per raggiungibilità*.
 * ⚠️ **Che si specchi anche l'ordine interno è una LETTURA, e si dichiara**: la sua frase sposta
 * il blocco e non nomina l'ordine, ma questa è la barra della `2.52`, dove *l'ordine delle icone
 * deve essere speculare* con la sola eccezione della coppia del tempo. Specchiati, 'Filigrana'
 * resta il più vicino al bordo da cui arriva il pollice, che è la ragione per cui si sono mossi.
 *
 * ⚠️⚠️ **E LO SPAZIO DAL BORDO È IL SUO 'NON DEL TUTTO A SINISTRA', MISURATO SUL MOCKUP**: là il
 * bersaglio del primo tasto comincia a 41 punti dal vetro, e la scheda ne ha già 16 di suoi,
 * quindi ne restano 25. Il numero scritto è [TOOL_EDGE], cioè [STAGE_SIDE]: è la stessa aria che
 * il palco lascia ai suoi fianchi, e su uno schermo da 360 punti il tasto comincia a 40.
 */
@Composable
fun EditorToolBar(
    /** Se il FAB vive a sinistra: il blocco passa a destra e i due tasti si scambiano. */
    mirror: Boolean,
    hasMark: Boolean,
    marking: Boolean,
    resizing: Boolean,
    enabled: Boolean,
    onMark: () -> Unit,
    onMarkSetup: () -> Unit,
    onResize: () -> Unit,
    onResizeSetup: () -> Unit,
    /**
     * Dov'è finito il tasto 'Filigrana', in coordinate della radice, o un riquadro **vuoto** se
     * quel tasto non c'è.
     *
     * ⚠️ **Serve al mini-onboarding, che vive nella schermata e non qui**: quel velo copre tutto
     * lo schermo, quindi nasce fuori dalla scheda, e il riquadro da illuminare lo sa solo chi il
     * tasto lo disegna. È lo stesso criterio della fila dei moduli e del velo della copertina.
     */
    onMarkSpot: (Rect) -> Unit = {},
    onResizeSpot: (Rect) -> Unit = {}
) {
    val filigrana: @Composable () -> Unit = {
        Box(modifier = Modifier.onGloballyPositioned { onMarkSpot(it.boundsInRoot()) }) {
            MarkButton(
                has = hasMark,
                on = marking,
                enabled = enabled,
                onToggle = onMark,
                onSetup = onMarkSetup
            )
        }
    }
    val misura: @Composable () -> Unit = {
        Box(modifier = Modifier.onGloballyPositioned { onResizeSpot(it.boundsInRoot()) }) {
            ResizeButton(
                on = resizing,
                enabled = enabled,
                onToggle = onResize,
                onSetup = onResizeSetup
            )
        }
    }
    Row(
        modifier = Modifier.padding(
            if (mirror) PaddingValues(end = TOOL_EDGE) else PaddingValues(start = TOOL_EDGE)
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (mirror) {
            misura()
            filigrana()
        } else {
            filigrana()
            misura()
        }
    }
}

/** Il bersaglio di un comando dell'editor, come quello di un `IconButton` di Material. */
private val TOOL_TOUCH = 48.dp

/**
 * Quanto la coppia rientra dal bordo esterno della barra.
 *
 * ⚠️ **È il *non del tutto a sinistra* della sua richiesta**, e il conto che lo regge vive su
 * [EditorToolBar]: sul mockup il primo bersaglio comincia a 41 punti dal vetro, e con i 16 della
 * scheda questo numero lo porta a 40.
 */
private val TOOL_EDGE = STAGE_SIDE

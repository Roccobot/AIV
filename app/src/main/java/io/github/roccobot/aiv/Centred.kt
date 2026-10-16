package io.github.roccobot.aiv

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Che cosa vuol dire 'centrato' in AIV: centrato in orizzontale, e centrato ma il 15% più in
 * basso in verticale (definizione dell'utente; regola in `Rules.md` § '📍 Che cosa vuol dire
 * centrato'). Il pollice arriva più facilmente sotto la metà dello schermo. Vale per tutto quello
 * che si apre in mezzo: conferme, pannelli, modali, scheda delle informazioni, menu.
 *
 * ⚠️ **La stretta è parte della definizione**: le cose alte si prendono lo spazio che serve, quindi
 * lo spostamento non supera l'aria che c'è sotto, e su un dialogo alto quanto lo schermo vale zero.
 * Il 15% si misura sull'altezza della finestra, non sullo spazio libero, dove su un dialogo alto
 * sparirebbe proprio quando serve di più.
 *
 * ⚠️ **In Compose non c'è un aggancio globale per i dialoghi**: `AlertDialog` centra la sua
 * superficie nella propria finestra, e nessuna proprietà sposta quel centro. Quindi un
 * modificatore solo, che ogni dialogo nuovo aggiunge.
 *
 * ⚠️ **Una finestra in cui si scrive è in alto, non centrata** (voci `rinomina-ferma` ed
 * `ext-aria`): a [TEXT_AIR] dal bordo di sopra della propria finestra finché ci sta, senza seguire
 * la tastiera, che cambia lo spazio mentre si scrive e farebbe ballare il pannello; sale solo
 * quando la tastiera non gli lascia spazio (scelta B1 dell'utente: il conto è su [pinTop]).
 * Quali sono lo dice già [onOutside]: per l'utente sono modali vere solo le finestre che devono
 * ricevere un input, quindi 'modale vera' e 'ha un campo di testo' coincidono, e sono quelle che
 * passano `null`. Una modale senza campo di testo finirebbe in alto, e il criterio dice che non
 * dovrebbe essere modale.
 */
fun Modifier.lowered(onOutside: (() -> Unit)?): Modifier =
    lowered(onOutside = onOutside, maxWidth = Adaptive.dialogMaxWidth)

/**
 * Come [lowered], con la larghezza massima scelta dal chiamante (Ridimensiona a due colonne).
 *
 * ⚠️ **Due firme e non un default sul secondo parametro**: `lowered { ... }` deve restare la forma
 * corta per [onOutside]; con un `maxWidth` dopo, la lambda finale finirebbe al parametro sbagliato,
 * in silenzio.
 */
fun Modifier.lowered(
    onOutside: (() -> Unit)?,
    maxWidth: Dp
): Modifier {
    /*
     * ⚠️ **Questo modificatore fa tutto quello che serve a un dialogo**, perché l'elenco delle
     * superfici che lo vogliono è esattamente quello di chi lo chiama, e un secondo
     * modificatore da ricordare sarebbe un modo in più di dimenticarsene:
     * - il velo e la sfocatura dietro (richiesta dell'utente: dietro qualsiasi pannello), o
     *   l'ombra se è lei la scelta (mai insieme), con [Modifier.veiled] e [DIALOG_LIFT];
     * - il tocco sull'aria uguale al tocco fuori ([OutsideElement], il perché è su [Air]);
     * - lo spostamento ([LowerElement]);
     * - il bordo d'accento ([DIALOG_EDGE]).
     * ⚠️ **Velo e spostamento sono nodi, non `@Composable`**: un `@Composable` chiamato da qui
     * girerebbe dove la riga è scritta, fuori dal dialogo, e leggerebbe la finestra
     * dell'attività, senza errori. Un nodo si aggancia dove il modificatore arriva, dentro il
     * dialogo.
     * ⚠️ **Ombra e bordo sono dopo lo spostamento**: [LowerElement] gonfia l'altezza per far
     * scendere il pannello, e prima di lui girerebbero intorno all'aria. L'ombra è prima del
     * bordo, perché avvolge tutto il pannello, tratto compreso.
     * [onOutside] non ha un valore di serie: chi apre una finestra nuova deve dire se è una
     * modale vera.
     */
    val aria = Air()
    /*
     * La larghezza ha un tetto (mockup `.dialog-preview`, 520 dp di serie): su tablet una
     * finestra a tutta fascia si legge come una pagina. [maxWidth] può salire (Ridimensiona a
     * due colonne usa [Adaptive.dialogBesideMaxWidth]).
     */
    return veiled() then OutsideElement(onOutside, aria) then
        LowerElement(aria, pinTop = onOutside == null) then DIALOG_LIFT then DIALOG_EDGE then
        Modifier.widthIn(max = maxWidth)
}

/**
 * Le proprietà della finestra che vanno **insieme** a [Modifier.lowered], con lo **stesso**
 * argomento.
 *
 * ⚠️ **Il modificatore governa l'aria dentro la finestra, queste quello che è fuori**: la fascia
 * trasparente sopra il pannello appartiene alla finestra, e la chiude un nodo di Compose; lo
 * schermo sotto il pannello è fuori dalla finestra, e lo chiude `dismissOnClickOutside`, che il
 * gestore delle finestre legge prima dell'app.
 * ⚠️ **Senza questa riga `lowered(null)` non è modale, e non dà errori** (voce `modali-quattro`):
 * `AlertDialog` senza `properties` prende quelle di serie, con `dismissOnClickOutside` acceso,
 * e un nome scritto a metà si perde toccando sotto il pannello. Regola in `Rules.md` § '👆 Che
 * cosa fa il tocco FUORI da una finestra'.
 */
@Composable
fun loweredWindow(onOutside: (() -> Unit)?) =
    DialogProperties(
        dismissOnClickOutside = onOutside != null,
        usePlatformDefaultWidth = !lowWindow()
    )

/**
 * Se la finestra è bassa, cioè il telefono in orizzontale.
 *
 * ⚠️ **Una finestra bassa non usa la larghezza di serie dei dialoghi** (voce `3.71-08`): in
 * orizzontale Android dà al dialogo una finestra larga circa quanto il lato corto dello schermo
 * (352 punti su un telefono largo 914), e in 400 punti di altezza il contenuto andrebbe su troppe
 * righe e taglierebbe l'ultimo tasto. Senza, il pannello si allarga fino a
 * [Adaptive.dialogMaxWidth] (`lowWindowWidth`).
 * Altrove la larghezza di serie resta: in verticale toglierla porterebbe il dialogo a filo dei
 * bordi.
 */
@Composable
fun lowWindow(): Boolean = windowHeightDp() < LOW_WINDOW.value

/**
 * Le proprietà di una finestra che copre lo schermo **intero**, barre di sistema comprese.
 *
 * Le due righe vanno insieme, ed è la ragione di una funzione sola per tutti i `Dialog` scritti in
 * casa (e per `Sheet`):
 * - `usePlatformDefaultWidth = false` toglie la larghezza di un dialogo di Material (il 90% meno i
 *   margini): senza, un elenco da scorrere diventa una fessura e un velo sembra una scheda scura.
 * - `decorFitsSystemWindows = false` porta la finestra sotto le barre. Senza, il decoro consuma i
 *   rientri, e un `safeDrawingPadding()` dentro aggiungerebbe il margine due volte.
 * Chi la chiama tiene il contenuto nell'area sicura da sé, con `safeDrawingPadding()`, salvo un
 * velo, che deve coprire tutto.
 *
 * ⚠️ **Una finestra così non chiama `WindowVeil()`**, ed è l'esenzione dichiarata alla regola di
 * `Rules.md` (§ '🌫️ Sfocatura dietro i pannelli e bordo'): il velo dice 'mi apro sopra
 * qualcosa', e qui sotto non si vede niente, perché la superficie copre lo schermo ed è opaca,
 * oppure è essa stessa un velo.
 */
fun fullWindow() =
    DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)

/**
 * Dov'è l'aria, cioè quello che [LowerNode] **misura** e [OutsideNode] **legge**.
 *
 * ⚠️ **I nodi sono due perché uno solo non riceverebbe mai un tocco** (misurato sul banco): un
 * `Modifier.Node` che implementa sia `LayoutModifierNode` sia `PointerInputModifierNode` non
 * riceve eventi, perché la hit-test scorre i nodi fino al primo nodo di layout e si ferma là, e
 * un nodo che è anche di layout è quel confine. Compila, e la funzione non c'è.
 * ⚠️ **Chi ascolta i tocchi va prima del nodo di misura** nel `then`: così il suo riquadro è la
 * scatola gonfiata, aria compresa. Dopo, l'aria gli resterebbe fuori.
 * ⚠️ **L'oggetto è uno solo, condiviso**: nasce a ogni chiamata di [Modifier.lowered], ma i due
 * `update` non lo riassegnano, quindi i nodi tengono quello della prima composizione, lo stesso
 * per tutti e due.
 */
private class Air {
    /** L'aria dichiarata **sopra** il pannello, in pixel. */
    var top = 0

    /** La quota da cui comincia l'aria **sotto**, in pixel. */
    var from = Int.MAX_VALUE
}

/**
 * Il tocco sull'aria vale come il tocco fuori.
 *
 * Servono pressione e rilascio fuori tutti e due: un dito che parte dal pannello e finisce
 * sull'aria sta trascinando.
 */
private class OutsideNode(
    var onOutside: (() -> Unit)?,
    private val aria: Air
) : Modifier.Node(), PointerInputModifierNode {

    private var pressedOutside = false

    /**
     * Si consuma il rilascio, o il tocco arriverebbe anche a quello che c'è sotto, come per il
     * velo che chiude i menu.
     * La passata `Main`: sull'aria nient'altro vuole quel tocco, e la passata normale lascia
     * intatto quello che c'è dentro il pannello.
     */
    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass != PointerEventPass.Main) return
        val close = onOutside ?: return
        val change = pointerEvent.changes.firstOrNull() ?: return
        val y = change.position.y
        val outside = y < aria.top || y >= aria.from
        when (pointerEvent.type) {
            PointerEventType.Press -> pressedOutside = outside
            PointerEventType.Release -> {
                if (pressedOutside && outside) {
                    change.consume()
                    close()
                }
                pressedOutside = false
            }
            else -> Unit
        }
    }

    override fun onCancelPointerInput() {
        pressedOutside = false
    }
}

private class OutsideElement(
    val onOutside: (() -> Unit)?,
    val aria: Air
) : ModifierNodeElement<OutsideNode>() {
    override fun create() = OutsideNode(onOutside, aria)

    /** [Air] non si riassegna: il perché è sulla sua dichiarazione. */
    override fun update(node: OutsideNode) {
        node.onOutside = onOutside
    }

    override fun equals(other: Any?) = other is OutsideElement && other.onOutside == onOutside

    override fun hashCode() = onOutside?.hashCode() ?: 0

    override fun InspectorInfo.inspectableProperties() {
        name = "loweredOutside"
        properties["modale"] = onOutside == null
    }
}

/**
 * Il bordo dei dialoghi, con la forma che Material dà loro.
 *
 * ⚠️ **Il raggio è [PANEL_ROUND]**: un nodo non legge il tema di Material, quindi la forma di un
 * `AlertDialog` si dichiara a mano, una volta. Chi desse ai dialoghi una forma propria cambia
 * quella, o il bordo taglia gli angoli in un altro punto.
 */
private val DIALOG_EDGE = Modifier.edged(PANEL_ROUND)

/**
 * L'ombra dei dialoghi, con lo stesso raggio di [DIALOG_EDGE] e per la stessa ragione. Il raggio
 * è quello della superficie, senza la correzione del rientro: l'ombra nasce dal contorno.
 */
private val DIALOG_LIFT = Modifier.lifted(PANEL_ROUND)

private class LowerElement(
    val aria: Air,
    val pinTop: Boolean
) : ModifierNodeElement<LowerNode>() {
    override fun create() = LowerNode(aria, pinTop)

    /**
     * [Air] non si riassegna (il perché è su [Air]); l'ancoraggio invece si aggiorna, perché una
     * finestra può nascere modale e non esserlo più.
     */
    override fun update(node: LowerNode) {
        node.pinTop = pinTop
    }

    override fun equals(other: Any?) = other is LowerElement && other.pinTop == pinTop

    override fun hashCode() = "lowered".hashCode() * 31 + pinTop.hashCode()

    override fun InspectorInfo.inspectableProperties() {
        name = "lowered"
        properties["inAlto"] = pinTop
    }
}

/**
 * Lo spostamento in basso, misurato sulla **finestra vera**.
 *
 * ⚠️ **Il 15% non si calcola su `constraints.maxHeight`**: quel vincolo è lo spazio che il
 * genitore concede alla passata, e la finestra di un dialogo si misura più di una volta
 * (`DialogLayout.internalOnMeasure` ricalcola il tetto, e con `UNSPECIFIED` lo passa come
 * infinito). Lo spostamento cambierebbe fra una passata e l'altra, con un fotogramma nel posto
 * sbagliato. Nemmeno la stretta legge il vincolo: l'aria sotto è finestra meno contenuto.
 * ⚠️ **Menu e dialoghi usano due formule diverse**: i menu (`MenuSpot`) contano su `windowSize`
 * del `Popup` e limitano la posizione finale; qui si conta su `currentWindowMetrics` meno i
 * rientri, e si limita lo spostamento. Su una superficie alta possono dare due posti diversi.
 * Non sono unificate: cambiare la formula muoverebbe ogni finestra centrata, comprese quelle
 * già approvate dall'utente, quindi prima si guarda se lo scarto si vede.
 */
private class LowerNode(
    private val aria: Air,
    var pinTop: Boolean
) : Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {

    /**
     * Misura il pannello e scrive in [Air] l'aria sopra e la quota da cui comincia quella sotto,
     * che legge [OutsideNode].
     *
     * ⚠️ **Per far scendere il pannello la scatola si gonfia** di `2*shift`, col pannello posato
     * in fondo. La finestra del dialogo si dimensiona sulla scatola, quindi sopra il pannello
     * resta una fascia trasparente, fino al 30% della finestra, che appartiene al dialogo: un
     * tocco lì è dentro la finestra, e `dismissOnClickOutside` non scatta. Per questo c'è
     * [OutsideNode] (riscontro del giro della 1.69: modale sopra, secondaria sotto).
     * In una finestra ancorata in alto l'aria è da tutti e due i lati: la scatola è alta quanto la
     * finestra ([pinned]), aria sopra fino a `Air.top` e sotto da `Air.from`.
     * Zero e [Int.MAX_VALUE] vogliono dire 'nessuna aria da quel lato'.
     */
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        /*
         * Una lettura sola dei rientri per passata, qui e non alla costruzione del nodo: finestra
         * e tastiera cambiano fra una misura e l'altra.
         */
        val insets = ViewCompat.getRootWindowInsets(currentValueOf(LocalView))
        val window = windowHeight(insets, hostInsets())
        val air = LOWER_AIR.roundToPx()
        if (pinTop) return pinned(measurable, constraints, window, air)
        /*
         * ⚠️ **Il tetto**: la stretta riduce lo spostamento fino a zero, ma un dialogo più alto
         * della finestra resta tagliato anche fermo al centro (riscontro del giro della 1.60).
         * Col tetto il contenuto scorre: `AlertDialogContent` dà al testo un peso che non
         * riempie, quindi con un massimo il testo si stringe e il suo scorrimento entra in
         * funzione.
         * Si lascia [LOWER_AIR] per lato: un pannello a filo dei bordi sembra tagliato.
         * Zero vuol dire 'non lo so ancora' ([windowHeight]), e allora non si limita niente.
         */
        val roof = window - air * 2
        val placed = measurable.measure(
            lowWindowWidth(
                if (roof > 0) constraints.copy(maxHeight = minOf(constraints.maxHeight, roof))
                else constraints,
                window
            )
        )
        val free = (window - placed.height).coerceAtLeast(0)
        val room = (free / 2 - air).coerceAtLeast(0)
        val wanted = (window * LOWER_BY).toInt()
        val shift = minOf(wanted, room)
        /*
         * ⚠️ **Lo spostamento è dentro l'altezza dichiarata**: un figlio posato fuori dalla
         * scatola dichiarata è ritagliato, e la finestra del dialogo si dimensiona su quella
         * scatola, quindi il pannello perderebbe la fila dei tasti (voce `centro-15`).
         * ⚠️ **Il `2` è il conto**: la finestra centra la scatola, quindi una scatola più alta di
         * `2*shift` sposta il contenuto di `shift`. Senza, il movimento si dimezza senza che
         * niente lo mostri.
         * `shift` non supera l'aria sotto meno [LOWER_AIR]: su un dialogo alto quanto la finestra
         * vale zero.
         */
        aria.top = shift * 2
        aria.from = Int.MAX_VALUE
        return layout(placed.width, placed.height + shift * 2) { placed.place(0, shift * 2) }
    }

    /**
     * In una finestra bassa il pannello si allarga, perché il testo vada su meno righe.
     *
     * Voce `3.71-08`: il dialogo di Material si stringe sul contenuto, fino ai 280 punti del
     * minimo, e in circa 400 punti di altezza un titolo su due righe e un testo su quattro non
     * entrano, e l'ultimo tasto risulta tagliato. Largo [Adaptive.dialogMaxWidth] lo stesso
     * contenuto entra.
     * ⚠️ **Da solo non basta, serve [loweredWindow]**: con la larghezza di serie, in orizzontale
     * la finestra è larga circa quanto il lato corto dello schermo.
     * Solo sotto [LOW_WINDOW] di altezza, e mai oltre la larghezza concessa.
     */
    private fun MeasureScope.lowWindowWidth(constraints: Constraints, window: Int): Constraints {
        if (window <= 0 || window >= LOW_WINDOW.roundToPx() || !constraints.hasBoundedWidth) return constraints
        val wide = minOf(Adaptive.dialogMaxWidth.roundToPx(), constraints.maxWidth)
        return if (wide > constraints.minWidth) constraints.copy(minWidth = wide) else constraints
    }

    /**
     * Una finestra in cui si scrive: ancorata in alto, e ferma finché la tastiera le lascia spazio.
     *
     * ⚠️ **Il bordo di sopra non segue la tastiera** (voce `rinomina-ferma`): lo spazio sopra la
     * tastiera cambia mentre si scrive (barra dei suggerimenti, animazione dell'IME), e il
     * pannello ballerebbe. Finché ci sta, il bordo di sopra vale [TEXT_AIR]; cambia solo il tetto.
     * ⚠️ **La scatola è alta quanto la finestra, e il pannello lo posa questo conto**: queste
     * finestre non hanno una modalità per la tastiera (`DialogWrapper` la lascia a zero quando il
     * decoro segue le barre), quindi a tastiera aperta Android non le ridimensiona, le sposta
     * finché il bordo di sopra coincide con quello visibile. Una scatola centrata da Android
     * perderebbe così l'aria sopra e anche il tetto (nota del giro della 5.50). Una scatola
     * grande quanto la finestra non ha niente da spostare.
     * Quando lo spazio non basta cedono l'aria sopra e il margine sotto i tasti (scelta B1
     * dell'utente: [pinTop] e [pinRoof]); solo oltre scorre il contenuto.
     * Senza una finestra ancora disposta il ripiego è il conto sullo schermo ([window]): il nodo
     * si rimisura appena la finestra c'è, perché [watch] vede lo spazio cambiare.
     */
    private fun MeasureScope.pinned(
        measurable: Measurable,
        constraints: Constraints,
        window: Int,
        air: Int
    ): MeasureResult {
        val visto = visible()
        seen = visto
        val span = visto ?: Span(0, window.takeIf { it > 0 } ?: 0)
        val avail = span.bottom - span.top
        val roof = pinRoof(avail = avail, tuck = TEXT_TUCK.roundToPx())
        val placed = measurable.measure(
            if (roof > 0) {
                constraints.copy(maxHeight = minOf(constraints.maxHeight, roof).coerceAtLeast(constraints.minHeight))
            } else {
                constraints
            }
        )
        val top = span.top + pinTop(avail = avail, panel = placed.height, sky = TEXT_AIR.roundToPx(), air = air)
        val box = if (constraints.hasBoundedHeight) constraints.maxHeight else top + placed.height
        aria.top = top
        aria.from = top + placed.height
        return layout(placed.width, box) {
            // ⚠️ Invisibile, e non assente, mentre aspetta una tastiera di cui non sa l'altezza:
            // il campo deve esistere per prendere il fuoco, che fa comparire la tastiera.
            if (span.hidden) placed.placeWithLayer(0, top) { alpha = 0f } else placed.place(0, top)
        }
    }

    /**
     * Dove comincia e dove finisce, rispetto a questo contenuto, lo spazio che barre e tastiera
     * lasciano libero nella finestra.
     *
     * Si misura dentro la finestra: i rientri dicono quanto barre e tastiera la coprono, e
     * valgono per intero perché la scatola è alta quanto la finestra. Il conto sullo schermo
     * ([windowHeight]) resta per le superfici centrate e per il primo passaggio.
     * `null` vuol dire 'non lo so ancora'.
     * ⚠️ **Per un attimo conta una tastiera che non c'è ancora** (nota dell'utente sul salto
     * all'apertura): queste finestre danno il fuoco al campo appena si aprono, quindi la
     * tastiera arriva sempre, ma un attimo dopo, e il pannello salirebbe al suo arrivo. Per
     * [WAIT_MS] dall'aggancio, finché la tastiera non c'è, conta quella ricordata ([Keyboard]);
     * il conto è su [keyboardBottom].
     */
    private fun visible(): Span? {
        val view = attached ?: return null
        val root = view.rootView
        if (root.height <= 0) return null
        val insets = ViewCompat.getRootWindowInsets(root) ?: return null
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val orientation = view.resources.configuration.orientation
        if (ime > 0) Keyboard.remember(view.context, orientation, ime)
        val remembered = Keyboard.height(view.context, orientation)
        val waiting = ime == 0 && SystemClock.uptimeMillis() - attachedAt < WAIT_MS
        val dove = IntArray(2)
        view.getLocationInWindow(dove)
        return Span(
            top = (bars.top - dove[1]).coerceAtLeast(0),
            bottom = root.height - keyboardBottom(bars.bottom, ime, remembered, waiting) - dove[1],
            hidden = waiting && remembered == 0
        )
    }

    /** Quando il nodo si è agganciato, cioè quando la finestra si è aperta: vedi [visible]. */
    private var attachedAt = 0L

    /** Rimisura alla fine dell'attesa, se la tastiera non è arrivata: vedi [visible]. */
    private val endWait = Runnable { if (isAttached) invalidateMeasurement() }

    /** La vista che ospita questo nodo, letta all'aggancio: vedi [watch]. */
    private var attached: View? = null

    /** Lo spazio con cui [pinned] ha misurato l'ultima volta. */
    private var seen: Span? = null

    /**
     * Rimisura la finestra quando lo spazio visibile cambia.
     *
     * ⚠️ **Serve perché la tastiera non cambia il vincolo**: la finestra non è ridimensionata
     * ([pinned]), quindi Compose non rimisura, e il conto non vedrebbe la tastiera, che coprirebbe
     * il pannello. Un cambio di rientri passa sempre da un giro di disegno, ed è lì che questo lo
     * guarda.
     * Il confronto ferma il giro: la misura nuova riscrive [seen], e il disegno dopo non rimisura.
     */
    private val watch = ViewTreeObserver.OnPreDrawListener {
        if (pinTop && visible() != seen) invalidateMeasurement()
        true
    }

    override fun onAttach() {
        val view = currentValueOf(LocalView)
        attached = view
        attachedAt = SystemClock.uptimeMillis()
        view.viewTreeObserver.addOnPreDrawListener(watch)
        view.postDelayed(endWait, WAIT_MS)
    }

    override fun onDetach() {
        attached?.viewTreeObserver?.takeIf { it.isAlive }?.removeOnPreDrawListener(watch)
        attached?.removeCallbacks(endWait)
        attached = null
        seen = null
    }

    /**
     * L'altezza della finestra **dentro le barre di sistema**, in pixel.
     *
     * ⚠️ **Si tolgono le barre**: il contenuto di un dialogo è misurato dentro i rientri di
     * sistema, e un'altezza che le comprendesse farebbe scendere un dialogo alto sotto la barra di
     * navigazione.
     * `currentWindowMetrics` da Android 11, perché dà la finestra (giusto anche a schermo diviso
     * e sui pieghevoli); sotto `displayMetrics`, il display, la sola cosa disponibile su Android 9
     * e 10, che coincide con la finestra quando l'app è sola a schermo.
     * ⚠️ **Si toglie anche la tastiera**: l'aria che la stretta conta non deve essere quella che
     * la tastiera ha già preso, o il pannello scende dentro di lei. Vale per ogni superficie
     * centrata, ed è il solo posto in cui la misura si prende. Lo spostamento può solo ridursi: se
     * il sistema alza già la finestra, il peggio è un dialogo centrato senza il 15%.
     * Zero vuol dire 'non lo so ancora' (il nodo misura prima che la vista sia agganciata), e
     * allora non si sposta niente.
     */
    private fun windowHeight(insets: WindowInsetsCompat?, host: WindowInsetsCompat?): Int {
        val view = currentValueOf(LocalView)
        val whole = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.context.getSystemService(WindowManager::class.java)
                ?.currentWindowMetrics?.bounds?.height() ?: 0
        } else {
            @Suppress("DEPRECATION")
            view.context.resources.displayMetrics.heightPixels
        }
        if (whole <= 0) return 0
        return insideBars(whole, insets?.let(::barsOf), host?.let(::barsOf))
    }

    /**
     * I rientri della finestra dell'attività sotto il dialogo.
     *
     * ⚠️ **Servono perché la finestra di un dialogo è disposta dentro le barre di sistema**, quindi
     * i suoi rientri non le contano, mentre [windowHeight] parte dallo schermo intero: senza, la
     * stretta lascerebbe scendere il pannello sopra la barra dei gesti (allegato `popup_phoneH`).
     * Le barre le conosce l'attività.
     */
    private fun hostInsets(): WindowInsetsCompat? {
        val view = currentValueOf(LocalView)
        val decor = Knobs.activityOf(view.context)?.window?.decorView ?: return null
        return ViewCompat.getRootWindowInsets(decor)
    }
}

/**
 * Le barre di sistema e la tastiera di una finestra, sopra e sotto.
 *
 * Una lettura sola con `or`: `getInsets` di un insieme di tipi restituisce il massimo per ogni
 * lato, quindi a tastiera aperta il lato di sotto è quello della tastiera, più alta della barra di
 * navigazione che copre.
 */
internal fun barsOf(insets: WindowInsetsCompat): Insets =
    insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())

/**
 * L'altezza di [whole] dentro le barre: per ogni lato vale la più alta fra quelle della finestra
 * ([own]) e quelle dell'attività sotto di lei ([host]); il perché è su `hostInsets`.
 *
 * Il massimo, non la somma: dove la finestra vede già le barre (un'attività, un dialogo a schermo
 * intero) le due letture coincidono, e sommarle toglierebbe le barre due volte.
 */
internal fun insideBars(whole: Int, own: Insets?, host: Insets?): Int {
    val top = maxOf(own?.top ?: 0, host?.top ?: 0)
    val bottom = maxOf(own?.bottom ?: 0, host?.bottom ?: 0)
    return (whole - top - bottom).coerceAtLeast(0)
}

/**
 * Di quanto scende quello che è 'centrato', e quanta aria resta comunque sotto.
 *
 * Il 15% è la scelta dell'utente, uno solo per tutta l'app, menu compresi.
 */
const val LOWER_BY = 0.15f

/**
 * Sotto questa altezza la finestra è 'bassa', e i pannelli centrati si allargano fino a
 * [Adaptive.dialogMaxWidth]: vedi [loweredWindow].
 */
val LOW_WINDOW = 480.dp
val LOWER_AIR = 16.dp

/**
 * L'aria sopra una finestra in cui si scrive, ancorata in alto: settanta punti, numero
 * dell'utente (voce `ext-aria`).
 *
 * Vale a tastiera aperta e chiusa (il perché è su [Modifier.lowered]), e cede quando la tastiera
 * non lascia spazio (scelta B1: il conto è su [pinTop]).
 */
val TEXT_AIR = 70.dp

/**
 * Quanto di una finestra in cui si scrive può finire dietro la tastiera: il margine sotto i tasti.
 *
 * ⚠️ **È il margine che `AlertDialog` di Material lascia sotto i tasti, 24dp**: quando lo spazio
 * non basta è la seconda cosa a cedere, dopo l'aria sopra (scelta B1). Dietro la tastiera finisce
 * aria vuota, e i tasti restano interi. Chi cambia il margine dei dialoghi cambia anche questo, o
 * i tasti finiscono dietro la tastiera.
 */
val TEXT_TUCK = 24.dp

/**
 * Lo spazio visibile di una finestra, in pixel dal bordo di sopra del suo contenuto.
 *
 * È una classe di dati perché il nodo confronta due misure per sapere se rimisurare.
 *
 * @property hidden se il pannello aspetta, invisibile, una tastiera di cui non si sa l'altezza.
 */
private data class Span(val top: Int, val bottom: Int, val hidden: Boolean = false)

/**
 * Quanto copre il bordo di sotto della finestra: le barre, la tastiera, o la tastiera che sta per
 * arrivare.
 *
 * È una funzione a sé perché il banco la provi: il pannello deve essere dove resterà già al primo
 * fotogramma, e in Robolectric nessuna tastiera arriva mai.
 *
 * @param bars quanto coprono le barre di sistema, in pixel.
 * @param ime quanto copre la tastiera adesso; zero se non c'è.
 * @param remembered l'altezza ricordata della tastiera in questo orientamento; zero se non si sa.
 * @param waiting se la finestra si è appena aperta e la tastiera non è ancora arrivata.
 */
internal fun keyboardBottom(bars: Int, ime: Int, remembered: Int, waiting: Boolean): Int =
    maxOf(bars, if (ime > 0) ime else if (waiting) remembered else 0)

/**
 * Per quanto, dall'apertura di una finestra in cui si scrive, si aspetta la tastiera.
 *
 * Un secondo: la tastiera compare in qualche centinaio di millisecondi, e l'attesa finisce appena
 * arriva. Il numero conta solo quando non arriva (una tastiera fisica): allora la finestra si
 * rimisura senza di lei dopo questo tempo.
 */
private const val WAIT_MS = 1_000L

/**
 * L'altezza della tastiera, ricordata per orientamento.
 *
 * Si ricorda fra un avvio e l'altro, in un archivio a sé: è una misura del telefono, non una
 * scelta, quindi non va nel file di impostazioni né nel backup. Con la sola memoria del processo,
 * la prima rinomina di ogni avvio salterebbe. Per orientamento, perché in orizzontale la tastiera
 * ha un'altra altezza; si scrive solo quando cambia, perché si legge a ogni misura.
 */
private object Keyboard {
    private val seen = HashMap<Int, Int>()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("keyboard", Context.MODE_PRIVATE)

    fun height(context: Context, orientation: Int): Int =
        seen.getOrPut(orientation) { prefs(context).getInt("ime-$orientation", 0) }

    fun remember(context: Context, orientation: Int, height: Int) {
        if (height(context, orientation) == height) return
        seen[orientation] = height
        prefs(context).edit().putInt("ime-$orientation", height).apply()
    }
}

/**
 * Quanto può essere alto, al massimo, il pannello di una finestra in cui si scrive.
 *
 * Tutto lo spazio visibile più [TEXT_TUCK] (scelta B1): quando lo spazio manca, prima cede l'aria
 * sopra ([pinTop]) e poi il margine sotto i tasti, che può finire dietro la tastiera.
 *
 * @param avail l'altezza dello spazio visibile, in pixel.
 * @param tuck quanto del pannello può finire dietro la tastiera, in pixel.
 * @return il tetto, e `0` quando lo spazio non si conosce, cioè nessun tetto.
 */
internal fun pinRoof(avail: Int, tuck: Int): Int = if (avail > 0) avail + tuck else 0

/**
 * Dove comincia il pannello di una finestra in cui si scrive, in pixel dal bordo di sopra dello
 * spazio visibile.
 *
 * A [sky] finché il pannello entra, poi sale quanto serve, fino a zero (scelta B1): con spazio a
 * sufficienza il bordo di sopra non si muove e sotto resta almeno [air]; quando lo spazio si
 * stringe l'aria sopra cala fino a sparire. Al confine i due conti danno lo stesso numero, quindi
 * non ci sono salti.
 * È una funzione a sé perché il banco la provi: in Robolectric nessuna finestra vede una tastiera.
 *
 * @param avail l'altezza dello spazio visibile, in pixel; zero se non si conosce.
 * @param panel l'altezza del pannello già misurato, in pixel.
 * @param sky l'aria sopra quando c'è spazio, in pixel ([TEXT_AIR]).
 * @param air l'aria minima sotto, in pixel ([LOWER_AIR]).
 */
internal fun pinTop(avail: Int, panel: Int, sky: Int, air: Int): Int {
    if (avail <= 0 || panel <= avail - sky - air) return sky
    return (avail - air - panel).coerceIn(0, sky)
}

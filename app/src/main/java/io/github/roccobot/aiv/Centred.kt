package io.github.roccobot.aiv

import android.os.Build
import android.view.WindowManager
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
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Che cosa vuol dire 'centrato' in AIV, dalla 1.29.
 *
 * ⚠️⚠️ **CENTRATO IN ORIZZONTALE, E CENTRATO MA IL 15% PIÙ IN BASSO IN VERTICALE**
 * (definizione dell'utente, 2026-09-02: *da questo momento in AIV dire 'centrato' (su
 * elementi di UI di questo tipo) significa centrato in orizzontale + centrato, ma un 15% più
 * in basso, in verticale*). Non è un gusto: il pollice arriva più facilmente sotto la metà
 * dello schermo, e un dialogo esattamente al centro fa allungare la mano su un telefono
 * grande. Vale per **tutto** quello che si apre in mezzo: i dialoghi di conferma, i pannelli,
 * i modali, la scheda delle informazioni, i menu.
 *
 * ⚠️⚠️ **LA STRETTA È PARTE DELLA DEFINIZIONE, non una prudenza aggiunta**: *le cose
 * particolarmente alte si prendono lo spazio che serve*. Un dialogo alto quasi quanto lo
 * schermo, spinto giù del 15%, ne uscirebbe: qui lo spostamento si riduce da sé fino a
 * sparire, perché quello che scende non può superare l'aria che ha sotto.
 *
 * ⚠️ **Il 15% si misura sull'altezza della FINESTRA, non sullo spazio libero**: sullo spazio
 * libero sarebbe una frazione di una frazione, quindi su un dialogo alto il movimento
 * sparirebbe proprio dove il pollice fatica di più. La stretta interviene dopo, e solo se
 * serve.
 *
 * ⚠️⚠️ **NON ESISTE UN AGGANCIO GLOBALE PER I DIALOGHI IN COMPOSE**, e per questo il numero
 * sta qui e la riga si scrive a ogni chiamata: `AlertDialog` centra la sua superficie dentro
 * la propria finestra, e nessuna proprietà del dialogo sposta quel centro. Quello che si può
 * fare è avere **un** modificatore, che è quello che si è fatto: chi apre un dialogo nuovo lo
 * aggiunge, e il valore non è mai scritto due volte. La regola sta anche in `CLAUDE.md`,
 * perché un modificatore da ricordare senza una regola scritta prima o poi si dimentica.
 */
fun Modifier.lowered(onOutside: (() -> Unit)?): Modifier {
    /*
     * ⚠️⚠️ **QUESTO MODIFICATORE FA UNA SECONDA COSA DALLA 1.38, E STA QUI PER UNA RAGIONE
     * PRECISA**: ogni superficie che si apre sopra la schermata vuole anche il **velo e la
     * sfocatura** dietro (richiesta dell'utente: *dietro qualsiasi pannello, popup, modale,
     * menu*), e l'elenco di quelle superfici è **esattamente** l'elenco di chi chiama questa
     * riga: la nota qui sopra lo dice già da tre versioni, parola per parola.
     * ⚠️ **L'alternativa era un secondo modificatore da ricordare**, e sarebbe stata la
     * trappola che questa stessa nota descrive: 'un modificatore da ricordare senza una regola
     * scritta prima o poi si dimentica'. Averne due raddoppierebbe il modo di dimenticarsene,
     * e la seconda dimenticanza non si vedrebbe nemmeno, perché un velo che manca non taglia
     * niente.
     * ⚠️⚠️ **E DEV'ESSERE UN NODO, NON UN `@Composable`: la prima versione velava la finestra
     * SBAGLIATA.** Un `@Composable` chiamato da qui verrebbe eseguito dove la riga è
     * **scritta**, cioè fuori dal dialogo, e leggerebbe la finestra dell'attività: nessun velo
     * visibile e nessun errore. Un nodo si aggancia dove il modificatore **atterra**, che è
     * dentro il dialogo. Vedi [Modifier.veiled].
     * ⚠️⚠️ **E DALLA 1.45 ANCHE LO SPOSTAMENTO È UN NODO, per la stessa ragione rovesciata**:
     * gli serve la finestra, e solo un nodo sa qual è. Vedi [LowerNode].
     * ⚠️⚠️ **E DALLA 1.54 FA UNA TERZA COSA: il BORDO D'ACCENTO** (richiesta dell'utente,
     * 2026-09-04: *via le ombre e vai con il bordino da 2px del colore di accento*). La ragione
     * per cui sta qui è la stessa delle altre due, e vale la pena rileggerla: l'elenco delle
     * superfici che vogliono il bordo è **esattamente** quello di chi chiama questa riga, cioè
     * i dialoghi di Material. Un quarto modificatore da ricordare sarebbe il modo di
     * dimenticarsene su quello nuovo.
     * ⚠️ **Il bordo sta DENTRO lo spostamento e non fuori**: `LowerElement` gonfia l'altezza per
     * far scendere il pannello, quindi un bordo scritto prima di lui girerebbe intorno alla
     * scatola gonfiata, cioè sull'aria. Scritto dopo, riceve la misura della superficie vera.
     * ⚠️⚠️ **E DALLA 1.70 FA UNA QUARTA COSA: RENDE IL TOCCO SULL'ARIA UGUALE AL TOCCO FUORI**
     * (riscontro del giro della 1.69: *alcune finestre hanno un comportamento da modale se si
     * tocca lo schermo SOPRA e da finestra secondaria se si tocca SOTTO*). Quell'asimmetria
     * non era una scelta: era il prezzo del gonfiaggio qui sopra, e il perché sta su [Air].
     * Il parametro non ha un valore di serie **di proposito**: chi apre una finestra nuova
     * deve dire se è una modale vera, e non può farlo per omissione.
     */
    val aria = Air()
    return veiled() then OutsideElement(onOutside, aria) then LowerElement(aria) then DIALOG_EDGE
}

/**
 * Le proprietà della finestra che vanno **insieme** a [Modifier.lowered], con lo **stesso**
 * argomento.
 *
 * ⚠️⚠️ **PERCHÉ SERVONO IN DUE POSTI: IL MODIFICATORE GOVERNA L'ARIA DENTRO LA FINESTRA, QUESTE
 * GOVERNANO QUELLO CHE STA FUORI.** Sono due meccanismi diversi e nessuno dei due può fare il
 * lavoro dell'altro: la fascia trasparente sopra il pannello **appartiene** alla finestra, e a
 * chiuderla è un nodo di Compose; lo schermo sotto il pannello è fuori dalla finestra, e a
 * chiuderla è `dismissOnClickOutside`, che il gestore delle finestre legge prima che l'app veda
 * qualcosa.
 * ⚠️⚠️ **DALLA `1.70` ALLA `1.72` LE QUATTRO MODALI NON ERANO MODALI** (riscontro dell'utente,
 * giro della `1.70`, voce `modali-quattro` non approvata): `lowered(null)` **dichiarava**
 * l'intenzione e non la applicava, perché `AlertDialog` senza `properties` prende quelle di
 * serie, dove `dismissOnClickOutside` è acceso. Quindi Rinomina, Estensione, Indirizzo e Nuova
 * cartella si chiudevano toccando **sotto** il pannello, e un nome scritto a metà si perdeva.
 * ⚠️ **Si scrive lo stesso argomento delle due chiamate**, e la regola sta in `CLAUDE.md`: un
 * dialogo nuovo che scrive `lowered(null)` e dimentica questa riga torna a non essere modale, e
 * non dà nessun errore.
 */
fun loweredWindow(onOutside: (() -> Unit)?) =
    DialogProperties(dismissOnClickOutside = onOutside != null)

/**
 * Dov'è l'aria, cioè quello che [LowerNode] **misura** e [OutsideNode] **legge**.
 *
 * ⚠️⚠️ **I DUE NODI SONO DUE PERCHÉ UNO SOLO NON RICEVEREBBE MAI UN TOCCO, e il fatto è
 * misurato sul banco di prova** (2026-09-05, la prima cosa che il banco ha trovato). Un
 * `Modifier.Node` che implementa **sia** `LayoutModifierNode` **sia** `PointerInputModifierNode`
 * non riceve nessun evento: nella stessa catena, un nodo che implementa il solo
 * `PointerInputModifierNode` prende il tocco e quello che implementa tutti e due non lo prende,
 * a parità di tutto il resto. La ragione è la hit-test, che per i tocchi scorre i nodi fino al
 * primo nodo di **layout** e si ferma là: un nodo che è anche di layout **è** quel confine, e
 * resta fuori dalla propria passata.
 * ⚠️⚠️ **QUINDI DALLA `1.70` ALLA `1.72` IL TOCCO SULL'ARIA NON HA MAI FATTO NIENTE**, e non
 * dava nessun errore: il codice era giusto, compilava, e la funzione non c'era. È lo stesso
 * genere di difetto del blocco della `1.70`, e per lo stesso motivo nessun controllo sul testo
 * del programma poteva vederlo.
 * ⚠️ **L'ordine dei due nel `then` non è indifferente**: chi ascolta i tocchi va **prima** del
 * nodo di misura, perché così il suo riquadro è la scatola gonfiata, cioè quella che comprende
 * l'aria. Scritto dopo, riceverebbe il riquadro del pannello e l'aria gli starebbe fuori.
 * ⚠️ **E l'oggetto è UNO SOLO condiviso**: nasce a ogni chiamata di [Modifier.lowered], ma i due
 * `update` non lo riassegnano mai, quindi i nodi tengono per sempre quello della prima
 * composizione, che è lo stesso per tutti e due.
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
 * ⚠️ **Servono tutti e due gli estremi, la pressione e il rilascio**: un dito che parte dal
 * pannello e finisce sull'aria sta trascinando, non toccando fuori, e chiudere là sarebbe
 * peggio dell'asimmetria che questo nodo toglie.
 */
private class OutsideNode(
    var onOutside: (() -> Unit)?,
    private val aria: Air
) : Modifier.Node(), PointerInputModifierNode {

    private var pressedOutside = false

    /**
     * ⚠️ **Si consuma il rilascio**, o il tocco arriverebbe anche a quello che sta sotto: è lo
     * stesso motivo per cui il velo che chiude i menu consuma quello che prende.
     * ⚠️ **`Main` e non `Initial`**: sull'aria non c'è nient'altro che possa volere quel tocco,
     * quindi non serve rubarlo prima; prenderlo nella passata normale lascia intatto il
     * comportamento di tutto quello che sta dentro il pannello.
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

    /** ⚠️ [Air] non si riassegna: il perché sta sulla sua dichiarazione. */
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
 * ⚠️ **28dp è `shapes.extraLarge`**, cioè la forma di `AlertDialog` quando nessuno la cambia, e
 * qui va scritta perché un nodo non legge il tema di Material. Chi un giorno desse ai dialoghi
 * una forma propria deve cambiarla anche qui, o il bordo taglia gli angoli in un altro punto.
 */
private val DIALOG_EDGE = Modifier.edged(28.dp)

private class LowerElement(val aria: Air) : ModifierNodeElement<LowerNode>() {
    override fun create() = LowerNode(aria)

    /**
     * ⚠️ **Non c'è niente da aggiornare, e [Air] in particolare non si riassegna**: la misura
     * dipende dalla finestra e dalla tastiera, che si leggono quando si misura, e l'oggetto
     * condiviso dev'essere quello della prima composizione. Il perché sta su [Air].
     */
    override fun update(node: LowerNode) = Unit

    override fun equals(other: Any?) = other is LowerElement

    override fun hashCode() = "lowered".hashCode()

    override fun InspectorInfo.inspectableProperties() {
        name = "lowered"
    }
}

/**
 * Lo spostamento in basso, misurato sulla **finestra vera**.
 *
 * ⚠️⚠️ **FINO ALLA 1.44 IL 15% SI CALCOLAVA SU `constraints.maxHeight`, ED È LA CAUSA DEL
 * SALTO** (segnalazione dell'utente, 2026-09-03, sul tocco di 'Rinomina' dal menu a pressione
 * lunga: *appare per una frazione di secondo più in alto, poi si sistema in basso con un
 * lampo/jitter*). Quel vincolo **non è** l'altezza della finestra: è lo spazio che il
 * genitore concede a quella passata di misurazione, e la finestra di un dialogo viene
 * misurata più di una volta (`DialogLayout.internalOnMeasure` ricalcola il tetto, e con la
 * modalità `UNSPECIFIED` lo passa perfino come infinito). Cambiando fra una passata e l'altra,
 * cambia lo spostamento: un fotogramma nel posto sbagliato, e quello è il lampo.
 * ⚠️⚠️ **E SPIEGA L'ALTRA METÀ DELLA SEGNALAZIONE** (*ho l'impressione che il menu e le
 * finestre che apre abbiano criteri di posizionamento diversi*): era vero alla lettera. I menu
 * passano da `MenuSpot`, che è un `PopupPositionProvider` e riceve `windowSize`, cioè la
 * finestra, **prima** che si disegni il primo fotogramma; i dialoghi passavano da un
 * modificatore di layout, che vede solo il vincolo della passata in corso. Due meccanismi che
 * misuravano due cose diverse, e uno dei due poteva sbagliare il primo fotogramma.
 * ⚠️⚠️ **MA 'ADESSO MISURANO LA STESSA COSA' NON ERA VERO, e questa riga lo diceva dalla
 * `1.45`**: quello che la `1.45` ha tolto è il **vincolo**, non la differenza. I menu contano
 * su `windowSize`, cioè la finestra che il `Popup` riceve, e limitano la **posizione finale**;
 * qui si conta su `currentWindowMetrics` meno i rientri di **questa** finestra, e si limita lo
 * **spostamento**. Sono due formule su due grandezze diverse, quindi su una superficie alta
 * possono dare due posti diversi, e la differenza è statica: si misura con uno screenshot solo,
 * aprendo un menu e la finestra che apre. ⚠️ **Non è stata unificata nella `1.47`**, perché
 * cambiare la formula muove ogni finestra centrata dell'app, comprese quelle che l'utente ha
 * già approvato: prima si guarda se lo scarto si vede.
 * ⚠️ **Il vincolo non si legge più affatto**, nemmeno per la stretta: la stretta ha bisogno di
 * quanta aria c'è sotto, e quell'aria è (finestra - contenuto), non (vincolo - contenuto). Con
 * la finestra, il conto è lo stesso a ogni passata.
 */
private class LowerNode(
    private val aria: Air
) : Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {

    /**
     * L'aria dichiarata **sopra** il pannello, in pixel, e la quota da cui comincia quella
     * **sotto**. Vivono in [Air] perché a leggerle è un **altro** nodo, [OutsideNode]: il perché
     * sta là.
     *
     * ⚠️⚠️ **QUESTA ARIA È LA CAUSA DELL'ASIMMETRIA CHE L'UTENTE HA SEGNALATO NELLA `1.69`**, e
     * il meccanismo va scritto o si rifà: per far scendere il pannello, [measure] non lo sposta,
     * **gonfia la scatola** di `2*shift` e ce lo posa in fondo. La finestra del dialogo si
     * dimensiona su quella scatola, quindi sopra il pannello resta una fascia trasparente alta
     * fino al 30% della finestra che **appartiene al dialogo**: un tocco là dentro è un tocco
     * *dentro* la finestra, e `dismissOnClickOutside` non scatta. Sotto il pannello si è fuori
     * dalla scatola, e la chiusura scatta. Da qui 'modale se tocchi sopra, secondaria se tocchi
     * sotto', su ogni finestra centrata dell'app.
     * ⚠️ **A tastiera aperta i due lati si scambiano**: là la scatola è gonfia **sotto** e il
     * contenuto sta in cima, quindi l'aria è quella che comincia a `Air.from`.
     * ⚠️ **Zero e [Int.MAX_VALUE] vogliono dire 'nessuna aria da quel lato'**, ed è il caso
     * normale di una finestra alta, dove la stretta ha già ridotto lo spostamento a zero.
     */
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val window = windowHeight()
        val air = LOWER_AIR.roundToPx()
        /*
         * ⚠️⚠️ **IL TETTO NASCE NELLA `1.62`, E TOGLIE UN TAGLIO CHE NESSUNA STRETTA POTEVA
         * TOGLIERE** (riscontro del giro della `1.60`, con schermata: *in presenza di un nome
         * molto lungo (ma valido) la finestra è tagliata brutalmente*). La stretta qui sotto
         * riduce lo **spostamento** fino a zero, ma un dialogo più alto della finestra resta più
         * alto della finestra anche fermo al centro: quello che mancava non era un movimento in
         * meno, era un limite.
         * ⚠️ **Con il tetto il contenuto scorre invece di sparire**: `AlertDialogContent` dà al
         * proprio testo un peso che non riempie, quindi appena la superficie ha un massimo il
         * testo si stringe e lo scorrimento che ha già dentro entra in funzione.
         * ⚠️ **Si lascia [LOWER_AIR] per lato**, la stessa aria della stretta: un pannello che
         * arriva a filo dei bordi si legge come tagliato anche quando è intero.
         * ⚠️ **Zero vuol dire 'non lo so ancora'** (vedi [windowHeight]), e allora non si limita
         * niente: meglio la misura di prima che un tetto costruito su una finestra inventata.
         */
        val roof = window - air * 2
        val placed = measurable.measure(
            if (roof > 0) constraints.copy(maxHeight = minOf(constraints.maxHeight, roof))
            else constraints
        )
        val free = (window - placed.height).coerceAtLeast(0)
        val room = (free / 2 - air).coerceAtLeast(0)
        val wanted = (window * LOWER_BY).toInt()
        val shift = minOf(wanted, room)
        /*
         * ⚠️⚠️ **A TASTIERA APERTA LA CENTRATURA HA UNA DEROGA, ED È SUA** (riscontro del giro
         * della `1.60`: *voglio che il pannello scorra MOLTO in alto, con il campo testo
         * praticamente in cima allo schermo, quando la tastiera è aperta. In quella circostanza
         * la centratura dell'app ha una deroga, che serve per rendere davvero fruibile il
         * pannello*). Non è la stretta portata all'estremo: la stretta **riduce** la discesa, qui
         * si va nel verso opposto e si **sale**.
         * ⚠️⚠️ **E SALIRE SI SCRIVE COL CONTENUTO IN CIMA ALLA SCATOLA GONFIA**, che è il rovescio
         * esatto della riga qui sotto: la finestra centra la scatola dichiarata, quindi una
         * scatola più alta di `2*su` col contenuto posato a **zero** lo porta `su` più in alto.
         * Chi cercasse un `offset` negativo troverebbe il ritaglio che la `1.33` ha già pagato.
         * ⚠️ **Si sale di [room], cioè il massimo che si può**: il pannello si ferma a
         * [LOWER_AIR] dal bordo di sopra dell'area che la tastiera lascia libera.
         * ⚠️ **La deroga vale finché la tastiera è in scena e non un istante di più**: quando si
         * chiude, questo nodo rimisura e il pannello torna al suo 15% in basso.
         */
        if (room > 0 && typing()) {
            aria.top = 0
            aria.from = placed.height
            return layout(placed.width, placed.height + room * 2) { placed.place(0, 0) }
        }
        /*
         * ⚠️⚠️ **LO SPOSTAMENTO STA DENTRO L'ALTEZZA RIPORTATA, e fino alla 1.33 NON c'era: è
         * il difetto che TAGLIAVA I DIALOGHI ALTI.** La nota di prima diceva l'opposto (misura
         * vera e movimento nel solo `place`, *così il genitore continua a centrare*), e la
         * ragione sembrava buona: un genitore che centra la scatola gonfia annulla metà del
         * movimento. Il guaio è che un figlio posato **fuori** dalla scatola dichiarata viene
         * **ritagliato**, e la finestra di un dialogo si dimensiona proprio su quella scatola:
         * il pannello scendeva del 15% e perdeva gli ultimi pixel, cioè la fila dei tasti.
         * Segnalato dall'utente con tre schermate (2026-09-02, voce `centro-15`): il dialogo
         * 'Info' senza la sua riga di comandi, e la rinomina tagliata a metà dei tasti.
         * ⚠️ **Il RADDOPPIO è quel conto, non una compensazione a occhio**: la finestra si
         * dimensiona sull'altezza dichiarata e la centra, quindi una scatola più alta di
         * `2*shift` sposta il contenuto vero di `shift`, che è la misura voluta. Chi togliesse
         * il `2` dimezzerebbe il movimento senza accorgersene, perché il difetto non si vede.
         * ⚠️ **E la stretta resta la stessa di prima**: `shift` non supera l'aria che c'è sotto
         * meno [LOWER_AIR], quindi su un dialogo alto quanto la finestra vale zero e la scatola
         * non si gonfia affatto.
         */
        aria.top = shift * 2
        aria.from = Int.MAX_VALUE
        return layout(placed.width, placed.height + shift * 2) { placed.place(0, shift * 2) }
    }

    /**
     * L'altezza della finestra **dentro le barre di sistema**, in pixel.
     *
     * ⚠️⚠️ **SI TOLGONO LE BARRE, e non è pignoleria**: il contenuto di un dialogo è misurato
     * dentro i rientri di sistema, quindi un'altezza che le comprendesse farebbe credere che
     * sotto ci sia un centinaio di pixel d'aria in più di quelli veri, e su un dialogo alto la
     * stretta lo lascerebbe scendere sotto la barra di navigazione. È esattamente il difetto
     * che la `1.33` ha tolto, e non va rimesso da un'altra porta.
     * ⚠️ **`currentWindowMetrics` da Android 11 e `displayMetrics` sotto**, non uno solo dei
     * due: il primo dà la **finestra**, quindi è giusto anche a schermo diviso e sui
     * pieghevoli; il secondo dà il **display**, che è la sola cosa disponibile su Android 9 e
     * 10 (il `minSdk` è 28) e coincide con la finestra quando l'app è sola a schermo.
     * ⚠️⚠️ **E DALLA `1.60` SI TOGLIE ANCHE LA TASTIERA** (riscontro dell'utente, giro della
     * `1.59`: *la finestra di rinomina dev'essere 'pronta' a scorrere più in alto quando
     * appaiono tastiere alte*). Fino alla `1.59` il 15% si calcolava sull'altezza intera anche
     * a tastiera aperta, e l'aria che la stretta contava era aria che la tastiera aveva già
     * preso: il pannello scendeva **dentro** di lei. ⚠️ **Non serviva una regola nuova**: la
     * definizione di 'centrato' dice già che *le cose particolarmente alte si prendono lo
     * spazio che serve*, e una tastiera alta rende alto qualunque dialogo. Bastava che la
     * misura dicesse la verità.
     * ⚠️ **Vale per ogni superficie centrata dell'app e non per la sola rinomina**, ed è il
     * motivo per cui la correzione sta qui: è l'unico posto in cui quella misura si prende.
     * ⚠️ **Lo spostamento può solo ridursi**, mai diventare negativo, quindi nel caso in cui il
     * sistema alzasse già la finestra da sé il peggio che capita è un dialogo centrato senza il
     * 15%, che a tastiera aperta è il posto giusto.
     * ⚠️ **Zero vuol dire 'non lo so ancora'**, e allora non si sposta niente: succede se
     * questo nodo misura prima che la vista sia agganciata, e uno spostamento calcolato su zero
     * sarebbe zero comunque.
     */
    /**
     * Se la tastiera è in scena adesso.
     *
     * ⚠️ **Si chiede se è VISIBILE e non quanto è alta**: una tastiera che si sta chiudendo ha
     * ancora un'altezza mentre scende, e la deroga deve finire quando finisce lei, non quando
     * l'ultimo pixel è sparito.
     */
    private fun typing(): Boolean =
        ViewCompat.getRootWindowInsets(currentValueOf(LocalView))
            ?.isVisible(WindowInsetsCompat.Type.ime()) == true

    private fun windowHeight(): Int {
        val view = currentValueOf(LocalView)
        val whole = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.context.getSystemService(WindowManager::class.java)
                ?.currentWindowMetrics?.bounds?.height() ?: 0
        } else {
            @Suppress("DEPRECATION")
            view.context.resources.displayMetrics.heightPixels
        }
        if (whole <= 0) return 0
        // ⚠️ `or` e non due letture: `getInsets` di un insieme di tipi restituisce il **massimo**
        // per ogni lato, quindi a tastiera chiusa il conto è identico a quello di prima e a
        // tastiera aperta il lato di sotto diventa quello della tastiera, che è più alto della
        // barra di navigazione che copre.
        val bars = ViewCompat.getRootWindowInsets(view)?.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
        )
        return (whole - (bars?.top ?: 0) - (bars?.bottom ?: 0)).coerceAtLeast(0)
    }
}

/**
 * Di quanto scende quello che è 'centrato', e quanta aria resta comunque sotto.
 *
 * ⚠️ **15% è la base scelta dall'utente**, e dalla 1.29 è **uno solo per tutta l'app**: il
 * menu contestuale della `1.28` scendeva del 17%, che era la metà di un intervallo indicato a
 * occhio. Due numeri per la stessa idea sono la stessa trappola degli angoli dei menu, e la
 * differenza fra 15 e 17 non la vede nessuno.
 */
const val LOWER_BY = 0.15f
val LOWER_AIR = 16.dp

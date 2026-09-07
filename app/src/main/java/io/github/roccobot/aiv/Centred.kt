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
 *
 * ⚠️⚠️ **UNA FINESTRA IN CUI SI SCRIVE NON È CENTRATA AFFATTO: È IN ALTO, DALLA `1.83`**
 * (riscontro del giro della `1.82`, voce `rinomina-ferma` non approvata, dove la specifica è
 * riscritta in tre righe: *posizionamento ... il più in alto possibile senza toccare la barra
 * delle notifiche o il notch*, e *niente glitch di posizione alla digitazione*; e la nota di
 * `ext-aria`: *fai 70 punti e posiziona le finestre di rinomina, salvataggio con nome, modifica
 * estensione, download, nuova cartella ... sempre in alto appoggiate a quella misura*). Le due
 * versioni prima di lei salivano **solo** a tastiera aperta, e la salita si ricavava dallo
 * spazio che la tastiera lasciava libero: quello spazio cambia mentre si scrive, quindi il
 * pannello ballava. Adesso non dipende più dalla tastiera: sta a [TEXT_AIR] dal bordo di sopra
 * della propria finestra, sempre, e non c'è niente che possa muoverlo.
 * ⚠️⚠️ **A DIRE QUALI SONO NON SERVE UN PARAMETRO NUOVO: LO DICE GIÀ [onOutside].** Il criterio
 * dell'utente sulle modali è *solo le finestre che devono ASSOLUTAMENTE fornire un input siano
 * modali vere* (giro della `1.69`), quindi 'modale vera' e 'ha un campo di testo' sono la stessa
 * cosa, e l'elenco che lui ha scritto nella nota di `ext-aria` è **esattamente** quello delle
 * finestre che passano `null` di qui. Un secondo parametro sarebbe un secondo modo di
 * dimenticarsene, ed è la trappola che questo file descrive da cinque versioni.
 * ⚠️ **Chi aprisse una modale senza campo di testo la troverebbe in alto**, e allora la domanda
 * da farsi è perché sia una modale: il criterio dice che non lo è.
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
     * ⚠️⚠️ **E DALLA 1.81 UNA QUINTA: l'OMBRA, quando è lei la scelta invece della sfocatura**
     * (istruzione dell'utente, 2026-09-07: *facciamo che si può scegliere tra sfocatura e
     * ombreggiatura (MAI insieme)*). Sta qui per la ragione delle altre, e questa volta la prova
     * è ancora più netta: le due vie sono **alternative**, quindi l'elenco di chi vuole l'una è
     * per definizione quello di chi vuole l'altra, che è l'elenco di chi scrive questa riga.
     * ⚠️ **Sta DENTRO lo spostamento come il bordo**, e per lo stesso motivo: scritta prima di
     * [LowerElement] l'ombra girerebbe intorno alla scatola gonfiata, cioè intorno all'aria.
     * ⚠️ **E prima del bordo**, perché il bordo è parte della superficie che si alza: l'ombra
     * avvolge tutto il pannello, tratto compreso.
     */
    val aria = Air()
    return veiled() then OutsideElement(onOutside, aria) then
        LowerElement(aria, pinTop = onOutside == null) then DIALOG_LIFT then DIALOG_EDGE
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
 * Le proprietà di una finestra che copre lo schermo **intero**, barre di sistema comprese.
 *
 * ⚠️⚠️ **NASCE PERCHÉ I TRE `Dialog` SCRITTI IN CASA TRATTAVANO LE BARRE IN TRE MODI DIVERSI, E
 * UNO SOLO DICEVA IL PERCHÉ** (censimento della UI del 2026-09-05). Le due righe vanno insieme e
 * fanno due cose distinte, che è la ragione per cui una funzione sola le tiene:
 * - `usePlatformDefaultWidth = false` toglie la larghezza di un dialogo di Material (il 90%
 *   meno i margini): senza, un elenco da scorrere diventa una fessura e un velo si legge come
 *   una scheda scura invece che come un velo.
 * - `decorFitsSystemWindows = false` è quella che porta la finestra **sotto** le barre. Senza,
 *   il decoro si adatta da sé e **consuma** i rientri, quindi un `safeDrawingPadding()` scritto
 *   dentro lavora su quello che il decoro ha già tolto e aggiunge un margine due volte.
 * ⚠️ **Chi la chiama tiene il contenuto nell'area sicura da sé**, con `safeDrawingPadding()`:
 * questa dice che i rientri li gestisce il contenuto, non che non esistono. L'eccezione è un
 * velo, che deve coprire tutto e non ha niente da rientrare.
 * ⚠️ **È la stessa coppia che `Sheet` passa alla propria finestra**, dove il perché della
 * seconda riga era scritto per esteso e valeva solo là.
 *
 * ⚠️⚠️ **E UNA FINESTRA COSÌ NON CHIAMA `WindowVeil()`: È L'ESENZIONE DICHIARATA ALLA REGOLA
 * GENERALE** (*chi apre un `Popup` o un `Dialog` scritto in casa chiama `WindowVeil()` a mano*,
 * `CLAUDE.md`). La ragione è che il velo dice 'mi apro **sopra** qualcosa', e qui non si vede
 * più niente sotto: la superficie copre lo schermo intero ed è opaca, oppure **è** essa stessa
 * un velo. Fino alla `1.80` l'assenza non era scritta da nessuna parte, cioè si leggeva come una
 * dimenticanza invece che come una scelta, ed è la forma di difetto per cui la regola esiste.
 */
fun fullWindow() =
    DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)

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
 * ⚠️ **Il raggio arriva da [PANEL_ROUND]** e non è scritto qui: un nodo non legge il tema di
 * Material, quindi la forma di un `AlertDialog` va dichiarata a mano, e la costante condivisa è
 * il posto in cui dichiararla una volta. Chi un giorno desse ai dialoghi una forma propria
 * cambia quella, o il bordo taglia gli angoli in un altro punto.
 */
private val DIALOG_EDGE = Modifier.edged(PANEL_ROUND)

/**
 * L'ombra dei dialoghi, con la stessa forma del bordo.
 *
 * ⚠️ **Lo stesso raggio di [DIALOG_EDGE] e per la stessa ragione**: un nodo non legge il tema di
 * Material, quindi la forma di un `AlertDialog` si dichiara a mano. ⚠️ E qui il raggio deve
 * combaciare con quello del bordo **senza** la correzione del rientro: l'ombra nasce dal
 * contorno della superficie, non da una linea che le corre dentro.
 */
private val DIALOG_LIFT = Modifier.lifted(PANEL_ROUND)

private class LowerElement(
    val aria: Air,
    val pinTop: Boolean
) : ModifierNodeElement<LowerNode>() {
    override fun create() = LowerNode(aria, pinTop)

    /**
     * ⚠️ **[Air] non si riassegna**: la misura dipende dalla finestra e dalla tastiera, che si
     * leggono quando si misura, e l'oggetto condiviso dev'essere quello della prima composizione.
     * Il perché sta su [Air].
     * ⚠️ **L'ancoraggio invece si aggiorna**, perché una finestra può nascere modale e non
     * esserlo più: senza questa riga il nodo terrebbe per sempre la scelta della prima
     * composizione, che è la stessa trappola dell'oggetto condiviso vista al rovescio.
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
    private val aria: Air,
    var pinTop: Boolean
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
     * ⚠️ **In una finestra ancorata in alto i due lati si scambiano**: là la scatola è gonfia
     * **sotto** e il contenuto sta in cima, quindi l'aria è quella che comincia a `Air.from`.
     * ⚠️ **Zero e [Int.MAX_VALUE] vogliono dire 'nessuna aria da quel lato'**, ed è il caso
     * normale di una finestra alta, dove la stretta ha già ridotto lo spostamento a zero.
     */
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        /*
         * ⚠️⚠️ **UNA LETTURA SOLA DEI RIENTRI PER PASSATA, e fino alla `1.80` erano due**
         * (censimento della UI del 2026-09-05): le due funzioni chiedevano due cose diverse
         * allo stesso oggetto (i rientri sommati l'una, la visibilità della tastiera l'altra),
         * e ognuna se lo andava a prendere da sé. Adesso lo prende chi misura e lo passa.
         * ⚠️ **Va letto QUI e non alla costruzione del nodo**: la finestra e la tastiera
         * cambiano fra una misurazione e l'altra, e un valore ricordato darebbe la stretta di
         * ieri.
         */
        val insets = ViewCompat.getRootWindowInsets(currentValueOf(LocalView))
        val window = windowHeight(insets)
        val air = LOWER_AIR.roundToPx()
        if (pinTop) return pinned(measurable, constraints, window, air)
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
     * Una finestra in cui si scrive: ancorata in alto, e ferma qualunque cosa faccia la tastiera.
     *
     * ⚠️⚠️ **IL CONTO NON GUARDA LA TASTIERA AFFATTO, ED È TUTTA LA CORREZIONE DELLA `1.83`**
     * (riscontro del giro della `1.82`, voce `rinomina-ferma`: *secondo terzo carattere inserito
     * o cancellato la finestra si sposta da troppo in basso a molto in alto, e poi ogni 3/4
     * caratteri c'è un flash della stessa finestra in posizione molto più ribassata*). Fino alla
     * `1.82` la salita si ricavava dallo spazio libero **sopra la tastiera**, e quello spazio
     * cambia mentre si scrive: la barra dei suggerimenti, un gesto che allarga la tastiera,
     * l'animazione dell'IME ancora in corso. La `1.82` ci aveva messo un'isteresi, cioè aveva
     * reso il ballo più raro invece di toglierne la causa.
     * ⚠️⚠️ **QUI IL TOP DEL PANNELLO VALE [air] PER COSTRUZIONE, e la prova è algebrica**: la
     * finestra centra la scatola dichiarata, quindi il contenuto posato a zero in una scatola
     * alta `pannello + 2*salita` comincia a `(box - pannello - 2*salita) / 2`; con
     * `salita = (box - pannello) / 2 - air` quel conto vale esattamente `air`, **qualunque**
     * siano `box` e `pannello`. Cioè la tastiera può muovere la finestra quanto vuole: il bordo
     * di sopra non si muove, perché a spostarsi è solo il bordo di sotto.
     * ⚠️ **Il tetto toglie di sotto e non di sopra**: un pannello troppo alto si accorcia e il
     * suo scorrimento entra in funzione, ma il campo di testo in cima resta dov'è.
     * ⚠️ **Il ripiego quando il contenitore non è vincolato è la finestra dello schermo**: senza
     * un `box` non c'è nessun bordo di sopra da cui misurare, e un pannello centrato è meglio di
     * uno posato su un numero inventato.
     */
    private fun MeasureScope.pinned(
        measurable: Measurable,
        constraints: Constraints,
        window: Int,
        air: Int
    ): MeasureResult {
        val sky = TEXT_AIR.roundToPx()
        val roof = window - sky - air
        val placed = measurable.measure(
            if (roof > 0) constraints.copy(maxHeight = minOf(constraints.maxHeight, roof))
            else constraints
        )
        val box = if (constraints.hasBoundedHeight) constraints.maxHeight else window
        val climb = pinClimb(box = box, panel = placed.height, air = sky)
        aria.top = 0
        aria.from = placed.height
        return layout(placed.width, placed.height + climb * 2) { placed.place(0, 0) }
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
    private fun windowHeight(insets: WindowInsetsCompat?): Int {
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
        val bars = insets?.getInsets(
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

/**
 * L'aria sopra una finestra in cui si scrive, che sta ancorata in alto: **settanta punti**.
 *
 * ⚠️⚠️ **IL NUMERO È SUO** (nota sulla voce `ext-aria` del giro della `1.82`: *fai 70 punti e
 * posiziona le finestre di rinomina, salvataggio con nome, modifica estensione, download, nuova
 * cartella ... sempre in alto appoggiate a quella misura*), ed è il terzo in tre versioni: la
 * `1.81` lasciava [LOWER_AIR], la `1.82` era salita a 56 perché *finisce ancora molto in alto, mi
 * sembra anche troppo*, e questa è la misura con cui ha detto di fermarsi.
 * ⚠️ **Adesso vale SEMPRE e non solo a tastiera aperta**: fino alla `1.82` era l'aria di una
 * deroga che scattava con l'IME in scena, e il perché del cambio è su [Modifier.lowered].
 */
val TEXT_AIR = 70.dp

/**
 * Di quanto si alza una finestra ancorata in alto, perché il suo bordo di sopra cada ad [air].
 *
 * ⚠️⚠️ **È UNA FUNZIONE A SÉ PERCHÉ IL BANCO DI PROVA LA POSSA MISURARE**: quello che deve
 * reggere è che il pannello **non si muova** al variare della finestra, e una prova che aprisse
 * una finestra vera in Robolectric non vedrebbe mai una tastiera cambiare misura. Come funzione,
 * il conto si esercita con i numeri di quello che è arrivato a lui.
 * ⚠️ **Non ha bisogno di un tetto come la `climbFor` che sostituisce**: quella partiva da una
 * salita ricavata dallo schermo e la doveva limitare alla finestra, cioè metteva d'accordo due
 * misure prese da due posti; qui la misura è una sola, il contenitore, e il pannello non può
 * uscirne per costruzione.
 *
 * @param box l'altezza che il contenitore concede, cioè la finestra che centrerà la scatola.
 * @param panel l'altezza del pannello già misurato.
 * @param air l'aria da lasciare sopra il pannello, in pixel.
 * @return la salita da applicare, e `0` quando il pannello riempie già la finestra.
 */
internal fun pinClimb(box: Int, panel: Int, air: Int): Int {
    if (box <= 0) return 0
    return ((box - panel) / 2 - air).coerceAtLeast(0)
}

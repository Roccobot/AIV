package io.github.roccobot.aiv

import android.os.Build
import android.view.WindowManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
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
fun Modifier.lowered(): Modifier {
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
     */
    return veiled() then LowerElement then DIALOG_EDGE
}

/**
 * Il bordo dei dialoghi, con la forma che Material dà loro.
 *
 * ⚠️ **28dp è `shapes.extraLarge`**, cioè la forma di `AlertDialog` quando nessuno la cambia, e
 * qui va scritta perché un nodo non legge il tema di Material. Chi un giorno desse ai dialoghi
 * una forma propria deve cambiarla anche qui, o il bordo taglia gli angoli in un altro punto.
 */
private val DIALOG_EDGE = Modifier.edged(28.dp)

private object LowerElement : ModifierNodeElement<LowerNode>() {
    override fun create() = LowerNode()
    override fun update(node: LowerNode) = Unit
    override fun hashCode() = "lowered".hashCode()
    override fun equals(other: Any?) = other === this
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
private class LowerNode : Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {

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

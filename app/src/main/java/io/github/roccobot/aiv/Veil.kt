package io.github.roccobot.aiv

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import kotlin.math.roundToInt

/**
 * Il **velo** e la **sfocatura** dietro tutto quello che si apre sopra la schermata.
 *
 * ⚠️⚠️ **RICHIESTA DELL'UTENTE, giro della 1.37** (*sfocatura leggera + velo chiaro/scuro a
 * seconda del tema: dietro qualsiasi pannello, popup, modale, menu. Solo le bottomsheet di
 * selezione e ritaglio ne sono esenti*). Serve a staccare la superficie che si apre da quello
 * che resta sotto: senza, un menu grigio sopra una griglia di miniature colorate galleggia in
 * mezzo al rumore.
 *
 * ⚠️⚠️ **LE DUE BOTTOMSHEET ESENTI LO SONO PER COSTRUZIONE, e non c'è niente da escludere**:
 * la fila della selezione (`PickSheet`) e quella del ritaglio non sono finestre ma `Surface`
 * appoggiate dentro la schermata, e nessuna delle due passa di qui. La ragione per cui non
 * sono finestre è più vecchia di questa richiesta e sta su `PickSheet`: con loro aperte si
 * deve poter continuare a toccare quello che c'è sotto, cioè l'esatto contrario di un velo.
 * Chi un domani le trasformasse in `ModalBottomSheet` si prenderebbe il velo insieme, e
 * romperebbe due cose con un cambio solo.
 *
 * ⚠️⚠️ **DALLA `1.54` IL VELO NON È PIÙ UN ATTRIBUTO DELLE FINESTRE: LO DIPINGE L'APP.** Il
 * perché, coi due tentativi che l'hanno preceduto, sta su [VeilStage]: in breve, due finestre
 * non possono cambiare il proprio velo nello **stesso fotogramma**, e da lì veniva il lampo che
 * l'utente ha bocciato tre volte. La **sfocatura** invece resta di finestra, perché quella non
 * si può dipingere senza il rifacimento descritto in fondo a questo file.
 * ⚠️ **E la tinta adesso si potrebbe scegliere**: il velo dipinto è un rettangolo nostro, non
 * più il `FLAG_DIM_BEHIND` di Android, che ha una sola quantità e nessun colore. Resta nero come
 * prima perché la `1.54` cambia il meccanismo e non l'aspetto: un colore nuovo sarebbe una
 * seconda cosa da giudicare nello stesso giro.
 * ⚠️ **Ma il 'chiaro/scuro a seconda del tema' si ottiene lo stesso, e non per compromesso**:
 * quello che si vede attraverso non è il velo, è lo **sfondo sfocato**, che sul tema chiaro è
 * chiaro e sullo scuro è scuro. Il velo aggiunge la quantità di buio che serve, e ne serve
 * meno sul chiaro (dove il contrasto con la scheda c'è già) e di più sullo scuro. Quindi il
 * numero cambia col tema, ed è [DIM_LIGHT] contro [DIM_DARK].
 *
 * ⚠️⚠️ **SENZA SFOCATURA IL VELO SI FA PIÙ FITTO, e non è un ripiego travestito**: la
 * sfocatura vuole Android 12 e un telefono che la conceda (si spegne da sé col risparmio
 * energetico, e certi apparecchi non la fanno affatto). Dove non c'è, a separare la superficie
 * resta il solo velo, e con la stessa quantità la separazione sarebbe minore. Vedi [DIM_MORE].
 *
 * ⚠️⚠️ **SI APPLICA ALLA FINESTRA CHE OSPITA CHI LO CHIEDE, e sbagliare finestra è
 * silenzioso**: chiesto dal posto sbagliato, questo velo va sulla finestra dell'**attività**,
 * cioè non si vede niente e non salta fuori nessun errore. Per questo ci sono due vie e non
 * una, e la differenza fra loro non è stilistica:
 * - [WindowVeil] è per chi sta **dentro** la finestra da velare: il contenuto di un `Popup`
 *   (i menu) o di un `Dialog` scritto da noi (le bottomsheet).
 * - [Modifier.veiled] è per i **dialoghi di Material**, dove non c'è un posto 'dentro' in cui
 *   scrivere una riga: le loro fessure (`title`, `text`) sono lambda, e infilarci un effetto
 *   sarebbe una riga da ricordare in ognuno. Un nodo di modificatore invece si aggancia
 *   **dove il modificatore atterra**, cioè sul contenuto del dialogo, quindi legge la finestra
 *   giusta. Ci arriva da [Modifier.lowered], che quei dialoghi hanno già.
 *
 * ⚠️⚠️ **E NE VALE UNO PER VOLTA, ma dalla `1.54` non è più una staffetta fra finestre**: il
 * velo dipinto è **uno** e vale quanto la richiesta più forte fra quelle in scena, quindi
 * mentre un menu esce e un dialogo entra non cambia affatto. Vedi [VeilStage].
 *
 * ⚠️⚠️ **DALLA 1.39 È SPENTO DI FABBRICA, DIETRO UN'IMPOSTAZIONE** (richiesta dell'utente,
 * 2026-09-03: *mettilo dietro un'opzione disattivata di default. Penserò se tenere o meno la
 * feature: rende tutto visibilmente più lento*). Lo dice [LocalAivVeil], e spento vuol dire
 * che qui non si tocca **niente**: ogni finestra resta com'era prima della 1.38.
 */
@Composable
fun WindowVeil(
    bare: Float = 0f,
    passante: () -> Boolean = { false },
    quanto: () -> Float = { PIENO }
) {
    val view = LocalView.current
    val on = LocalAivVeil.current
    val dark = !LocalAivLight.current
    val radius = with(LocalDensity.current) { BLUR.roundToPx() }
    val misura by rememberUpdatedState(quanto)
    val sorda by rememberUpdatedState(passante)
    val velo = remember(view, on, bare, dark, radius) { veilFor(view, on, bare, dark, radius) }
    /*
     * ⚠️⚠️ **IL VELO SI DOSA A OGNI FOTOGRAMMA, dalla 1.50, e prima cadeva in un colpo**
     * (riscontro dell'utente, 2026-09-04: *la sfocatura non dovrebbe sparire all'inizio della
     * transizione: dovrebbe avere essa stessa una transizione... si dovrebbe ridurre dal valore
     * di sfocatura in corso a 0, per poi essere disattivata del tutto*). `snapshotFlow` legge il
     * valore dell'animazione di chi chiama e lo passa qui a ogni cambiamento: chi non ne ha uno
     * passa [PIENO] e si comporta come prima.
     * ⚠️ **E il costo è dichiarato**: su un menu vuol dire un `updateViewLayout` per fotogramma
     * per la durata dell'animazione. È il prezzo della cosa chiesta, e la via che lo eviterebbe
     * (dipingere la sfocatura sulla vista dell'app con un `RenderEffect`) è un rifacimento a
     * sé, di cui si parla nella nota in fondo a questo file.
     *
     * ⚠️⚠️ **A FUNZIONE SPENTA IL DOSAGGIO NON SI APPLICA, ed è la regola della `1.39` scritta
     * in una riga**: là l'unico velo che resta è quello che la scheda in fondo chiede alla
     * propria finestra ([veilFor], ramo `bare`), e dosarlo vorrebbe dire cambiare come si vela
     * una superficie mentre la funzione è disattivata, cioè fare quello che la `1.39` ha
     * dichiarato di non fare.
     */
    LaunchedEffect(velo) {
        snapshotFlow { Dose(if (on) misura().coerceIn(0f, PIENO) else PIENO, sorda()) }
            .collect { velo?.at(it.quanto, it.passante) }
    }
    DisposableEffect(velo) { onDispose { velo?.off() } }
}

/**
 * Quanta patina vuole la finestra adesso, e se in questo momento è **passante**.
 *
 * ⚠️ **Una coppia e non due flussi**: i due numeri cambiano nello stesso fotogramma e finiscono
 * nella stessa scrittura sui parametri della finestra; separati, un fotogramma su due
 * scriverebbe due volte.
 */
private data class Dose(val quanto: Float, val passante: Boolean)

/**
 * L'interruttore della funzione: se velo e sfocatura sono accesi.
 *
 * ⚠️ **Un `CompositionLocal` e non un parametro**: chi chiede il velo sono i dialoghi, i menu
 * e le due schede, e nessuno di loro riceve le impostazioni. Il valore lo mette in scena
 * l'attività, che le ha già lette per il tema. ⚠️ **Quanti sono non si scrive**: qui c'era
 * 'tredici dialoghi', e la `1.44` ne ha tolto uno rendendolo falso. Il criterio sta in
 * `rules/Roccobot.md` § '🪶 Come si mantiene un file di regole'.
 * ⚠️ **Il nodo lo legge quando si attacca**, cioè quando la finestra si apre: cambiare
 * l'interruttore mentre un dialogo è aperto non lo cambia sotto gli occhi, e non è un caso che
 * esista (l'impostazione vive in una schermata, non in un dialogo).
 */
val LocalAivVeil = staticCompositionLocalOf { false }

/**
 * Lo stesso velo, ma agganciato **dove il modificatore atterra**. Vedi la nota in testa.
 *
 * ⚠️ **Non è un `@Composable` che ritorna un `Modifier`**, e la differenza è tutto il punto:
 * quello verrebbe eseguito dove la riga è **scritta**, cioè fuori dal dialogo, e leggerebbe la
 * finestra dell'attività. Un nodo legge i suoi `CompositionLocal` dalla posizione in cui è
 * **attaccato**, che è dentro il dialogo.
 */
fun Modifier.veiled(): Modifier = this then VeilElement

private object VeilElement : ModifierNodeElement<VeilNode>() {
    override fun create() = VeilNode()
    override fun update(node: VeilNode) = Unit
    override fun hashCode() = "veiled".hashCode()
    override fun equals(other: Any?) = other === this
    override fun InspectorInfo.inspectableProperties() {
        name = "veiled"
    }
}

private class VeilNode : Modifier.Node(), CompositionLocalConsumerModifierNode {
    private var velo: Veil? = null

    /*
     * ⚠️ **Qui il velo è pieno e basta, e non è una dimenticanza della 1.50**: questo nodo
     * serve ai dialoghi di Material, che compaiono di colpo e non hanno nessun avanzamento da
     * seguire. Chi ne ha uno passa da [WindowVeil] col suo, e la classe è la stessa.
     * ⚠️⚠️ **E DEVE ESSERE SINCRONO, cioè qui e non in una coroutine**: questo metodo gira
     * mentre il dialogo si compone, **prima** che la sua finestra disegni il primo fotogramma,
     * quindi il velo di sistema (0,6 del tema) non si vede mai. Rimandarlo di un giro, anche
     * uno solo, lo farebbe vedere. ⚠️ **E dalla `1.54` quel velo di sistema si azzera invece di
     * essere sostituito**: il nostro lo dipinge l'app, e lasciare acceso quello della finestra
     * vorrebbe dire due veli sovrapposti, cioè uno sfondo più scuro di quello approvato.
     * ⚠️ **La via scartata resta scritta**: far salire il velo del dialogo con un'animazione, su
     * un dialogo aperto **senza** un menu sotto, partiva da sotto il velo di sistema, cioè
     * schiariva lo sfondo prima di scurirlo.
     */
    override fun onAttach() {
        if (!currentValueOf(LocalAivVeil)) return
        val view = currentValueOf(LocalView)
        val dark = !currentValueOf(LocalAivLight)
        val radius = with(currentValueOf(LocalDensity)) { BLUR.roundToPx() }
        velo = veilFor(view, on = true, bare = 0f, dark = dark, radius = radius)
            ?.also { it.at(PIENO) }
    }

    override fun onDetach() {
        velo?.off()
        velo = null
    }
}

/**
 * Quanto velo e quanta sfocatura vuole questa finestra, e chi glieli stende.
 *
 * ⚠️ **Il calcolo sta qui e la stesura in [Veil]**, perché il velo semplice della `1.39` (la
 * scheda quando la funzione è spenta) vuole la seconda metà senza la prima: la sfocatura non
 * la chiede e la quantità gliela dice chi chiama.
 */
private fun veilFor(view: View, on: Boolean, bare: Float, dark: Boolean, radius: Int): Veil? =
    when {
        on -> {
            val manager = view.context.getSystemService(WindowManager::class.java)
            /*
             * ⚠️⚠️ **LA SFOCATURA SI CHIEDE AL SISTEMA, NON SI DÀ PER SCONTATA**: da Android 12
             * il telefono può dire di no in qualunque momento (risparmio energetico acceso,
             * hardware che non la fa), e `isCrossWindowBlurEnabled` è la domanda giusta. Chi la
             * desse per fatta si ritroverebbe con un velo troppo leggero proprio dove la
             * sfocatura manca.
             */
            val blurred = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                manager?.isCrossWindowBlurEnabled == true
            Veil(
                view = view,
                dim = (if (dark) DIM_DARK else DIM_LIGHT) + (if (blurred) 0f else DIM_MORE),
                radius = if (blurred) radius else null,
                dipinto = true
            )
        }
        /*
         * ⚠️ **IL SOLO CASO CHE RESTA UN VELO DI FINESTRA, ed è quello a funzione spenta**: la
         * scheda in fondo se lo chiede da sé perché la sua finestra non ne ha uno di serie
         * (`SHEET_DIM` in `Sheet.kt`). Là non c'è nessuna staffetta fra due finestre, quindi non
         * c'è nessun lampo da togliere, e dipingerlo vorrebbe dire accendere il rettangolo
         * dell'app anche quando la funzione è spenta, cioè fare quello che la `1.39` ha
         * dichiarato di non fare.
         */
        bare > 0f -> Veil(view, dim = bare, radius = null, dipinto = false)
        else -> null
    }

/**
 * Il velo **dipinto dall'app**: quanto ne vuole chi, fra le superfici in scena, ne chiede di più.
 *
 * ⚠️⚠️ **È IL TERZO RIMEDIO ALLO STESSO LAMPO, E I DUE PRIMI HANNO FALLITO PER LA STESSA
 * RAGIONE** (riscontro dell'utente, giro della `1.53`, voce `flash-menu-dialogo`: *c'è ancora,
 * più lento. E lo sfondo diventa anche più scuro*). La storia in breve, perché è quella che
 * spiega la scelta:
 * - la `1.48` spegneva il velo del menu **prima** che il dialogo esistesse: buco di uno o due
 *   fotogrammi, cioè un lampo chiaro;
 * - la `1.50` lo faceva calare invece di spegnerlo, e i due si **sommavano** (0,45 sotto 0,45 fa
 *   0,70, non 0,45): lampo scuro;
 * - la `1.53` ha fatto spegnere quello sotto **nello stesso istante** in cui il nuovo si
 *   accende, cioè dentro la stessa chiamata.
 *
 * ⚠️⚠️ **E LA `1.53` MANCAVA IL PUNTO, che è quello che il riscontro dice a chiare lettere**:
 * 'nello stesso istante' vale per il **codice**, non per lo schermo. Il velo di una finestra si
 * applica quando quella finestra viene aggiornata, e due finestre diverse si aggiornano quando
 * ognuna è pronta: fra il `setDimAmount` del dialogo e l'`updateViewLayout` del menu passa
 * almeno un fotogramma, e in quel fotogramma i veli sono due (più scuro) oppure nessuno (più
 * chiaro). Nessuna sequenza di chiamate può renderlo atomico, perché la sincronia non è nostra.
 *
 * ⚠️⚠️ **QUINDI IL VELO SMETTE DI ESSERE UN ATTRIBUTO DI FINESTRA E DIVENTA UN RETTANGOLO CHE
 * L'APP DIPINGE** ([AppVeil], messo in scena da `AivTheme`). Uno solo, nella finestra
 * dell'attività, che attraversa la transizione senza nascere né morire: quello che cambia è la
 * sua opacità, e vale il **massimo** delle richieste, quindi due superfici in scena insieme
 * dànno lo stesso velo di una. Il lampo non è corretto: non ha più il posto in cui accadere.
 * ⚠️ **Il massimo e non la somma, ed è la differenza che conta**: sommare due veli era il
 * difetto della `1.50`, e la somma non serve a niente, perché quello che si guarda è una
 * superficie sola sopra uno sfondo solo.
 *
 * ⚠️ **La SFOCATURA resta di finestra e non entra qui**: non si può dipingere senza il
 * rifacimento in fondo a questo file. Durante il passaggio menu -> dialogo le due finestre la
 * chiedono entrambe per un momento, quindi lo sfondo diventa un filo più sfocato e torna: è un
 * cambio di **nitidezza** e non di luminosità, che è la cosa che l'occhio non legge come un
 * lampo. Spegnere quella di sotto per evitarlo riporterebbe il buco della `1.48`, sull'altro
 * attributo.
 * ⚠️ **Dalla `1.61` quel momento dura più a lungo**, perché la finestra di sotto sopravvive al
 * proprio pannello per il tempo della coda: il sovrappiù però è quasi tutto nel primo tratto,
 * visto che a metà coda la sfocatura del menu è già scesa a pochi pixel di raggio.
 *
 * ⚠️ **A funzione spenta non c'è niente in questa mappa**: senza l'impostazione i menu e i
 * dialoghi non creano nessun velo (lo dice [veilFor]), e la scheda in fondo chiede il suo velo
 * semplice **alla propria finestra**, che è l'unico caso in cui quel meccanismo resta.
 */
internal object VeilStage {
    /*
     * ⚠️ **Una mappa di richiedenti e non un numero**: chi chiede sono al più due o tre
     * superfici, e ognuna se ne va quando vuole, anche in un ordine diverso da quello in cui è
     * arrivata. Con un numero solo, la seconda che se ne va porterebbe via anche il velo della
     * prima.
     */
    private val chiedono = mutableStateMapOf<Any, Float>()

    /** Quanto velo si dipinge adesso. Si legge nella fase di **disegno**, non in composizione. */
    val dose: Float get() = chiedono.values.maxOrNull() ?: 0f

    fun at(chi: Any, quanto: Float) {
        if (quanto <= 0f) chiedono.remove(chi) else chiedono[chi] = quanto
    }

    fun off(chi: Any) {
        chiedono.remove(chi)
    }

    /**
     * ⚠️ **La rete contro il velo fantasma**: se la composizione dell'app se ne va mentre una
     * superficie è aperta, il suo `off` non arriva più e resterebbe uno schermo velato per
     * sempre. Lo chiama [AppVeil] quando esce di scena, cioè quando non c'è più niente da velare
     * comunque.
     */
    fun clear() {
        chiedono.clear()
    }
}

/**
 * Il velo, dipinto sopra il contenuto dell'app. Lo mette in scena `AivTheme`, una volta sola.
 *
 * ⚠️⚠️ **NON RUBA I TOCCHI, e non è una speranza**: Compose fa la prova del tocco solo sui nodi
 * che hanno un modificatore di puntatore, e questo ne ha uno solo di disegno. È lo stesso fatto
 * su cui poggia la fascia sfumata della schermata iniziale.
 * ⚠️ **La dose si legge DENTRO il disegno**: così un velo che sale o scende costa un ridisegno e
 * non una ricomposizione, che su sessanta fotogrammi al secondo è la differenza fra un effetto e
 * un rallentamento.
 * ⚠️ **Sopra tutto quello che l'app disegna, tastino compreso**, come faceva il velo di finestra:
 * quello che deve restare sopra è la superficie che si è aperta, e quella vive in un'altra
 * finestra.
 */
@Composable
fun AppVeil(modifier: Modifier = Modifier) {
    DisposableEffect(Unit) { onDispose { VeilStage.clear() } }
    Box(
        modifier = modifier.drawBehind {
            val quanto = VeilStage.dose
            if (quanto > 0f) drawRect(color = VEIL_INK, alpha = quanto)
        }
    )
}

/**
 * Di che colore è il velo.
 *
 * ⚠️ **Nero, com'era quando lo dipingeva Android**: la `1.54` cambia chi lo dipinge e non che
 * cosa si vede. Il 'chiaro/scuro a seconda del tema' che l'utente ha chiesto ce l'ha già, e non
 * viene da qui: quello che si vede attraverso è lo **sfondo sfocato**, chiaro sul tema chiaro e
 * scuro su quello scuro; il velo aggiunge solo la quantità di buio, che cambia col tema
 * ([DIM_LIGHT] contro [DIM_DARK]).
 */
private val VEIL_INK = Color.Black

/**
 * Il velo steso sulla finestra di [view]: si **dosa** con [at] e si toglie con [off].
 *
 * ⚠️⚠️ **DOSABILE E NON ACCESO-SPENTO, dalla 1.50**: [dim] e [radius] sono il **pieno**, e
 * quello che finisce sulla finestra è la loro frazione. È la cosa che permette alla sfocatura
 * di crescere e calare insieme al pannello invece di comparire tutta insieme.
 * ⚠️⚠️ **E IL DIFETTO CHE TOGLIE È QUELLO CHE SI VEDEVA DI PIÙ** (riscontro dell'utente,
 * 2026-09-04: *una specie di cornice sfumata si materializza dove c'era/ci sarà il margine del
 * pannello effettivo*). La sfocatura è un attributo della **finestra**, quindi al primo
 * fotogramma copriva il rettangolo pieno del popup mentre il pannello dentro era ancora
 * rimpicciolito e trasparente: quel rettangolo, coi bordi sfumati dal raggio, **era** la
 * cornice. Con la sfocatura che cresce da zero non c'è nessun fotogramma in cui la finestra
 * sfoca più di quanto il pannello sia in scena.
 *
 * ⚠️ **Si aggancia alla prima chiamata e non alla costruzione**: i `LayoutParams` della radice
 * di un `Popup` possono non essere ancora quelli del gestore mentre la composizione è in corso,
 * e leggerli troppo presto darebbe un velo che non si applica, in silenzio.
 */
private class Veil(
    private val view: View,
    private val dim: Float,
    private val radius: Int?,
    /**
     * Se il velo lo dipinge l'app ([VeilStage]) invece della finestra.
     *
     * ⚠️ Quando è vero, alla finestra si chiede la sola **sfocatura**, e il suo velo si porta a
     * **zero**: su un dialogo quello di serie è 0,6 e sommato al nostro darebbe uno sfondo più
     * scuro di quello che l'utente ha approvato. Azzerarlo qui è sincrono, quindi non si vede.
     */
    private val dipinto: Boolean
) {
    private var window: Window? = null
    private var manager: WindowManager? = null
    private var params: WindowManager.LayoutParams? = null
    private var dimPrima = 0f
    private var flagsPrima = 0
    private var acceso = false

    /** Se l'ultima stesura ha lasciato la finestra passante. Vedi [stendi]. */
    private var passa = false

    /** Aggancia la finestra e ricorda com'era. Falso se qui non c'è niente da velare. */
    private fun grab(): Boolean {
        if (acceso) return true
        manager = view.context.getSystemService(WindowManager::class.java)
        window = view.dialogWindow()
        val w = window
        if (w != null) {
            dimPrima = w.attributes.dimAmount
        } else {
            /*
             * ⚠️⚠️ **UN `Popup` È UNA FINESTRA ANCHE LUI, ma non ha un `Window`**: Compose lo
             * aggiunge al `WindowManager` come vista, quindi i suoi parametri sono i
             * `LayoutParams` della **radice** di questo albero, e per farli valere si ripassa
             * dal gestore con `updateViewLayout`. È la sola via per dare un velo a un menu, che
             * di suo non ne ha nessuno.
             */
            val p = view.rootView.layoutParams as? WindowManager.LayoutParams ?: return false
            if (manager == null) return false
            params = p
            flagsPrima = p.flags
            dimPrima = p.dimAmount
        }
        acceso = true
        return true
    }

    /** Il dosaggio chiesto: al velo dipinto, e alla finestra quello che tocca a lei. */
    fun at(q: Float, passante: Boolean = false) {
        if (!grab()) return
        if (dipinto) VeilStage.at(this, dim * q)
        stendi(q, passante)
    }

    private fun stendi(q: Float, passante: Boolean) {
        // ⚠️ A velo dipinto la finestra non ne vuole: quello che le si chiede è la sfocatura, e
        // il suo velo di serie va a zero (vedi [dipinto]).
        val velo = if (dipinto) 0f else dim * q
        val raggio = radius?.let { (it * q).roundToInt() } ?: 0
        val w = window
        if (w != null) {
            // ⚠️ Su un dialogo si passa dalla finestra e non dai suoi parametri a mano: è lei
            // che li riapplica, e `setDimAmount` accende già il flag che serve.
            w.setDimAmount(velo)
            if (radius != null) {
                w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                w.attributes = w.attributes.apply { blurBehindRadius = raggio }
            }
            return
        }
        /*
         * ⚠️⚠️ **UN MENU A VELO DIPINTO E SENZA SFOCATURA NON CHIEDE NIENTE ALLA SUA FINESTRA, e
         * questa riga è un guadagno vero**: prima, per ogni fotogramma dell'animazione, passava
         * un `updateViewLayout` che aggiornava un velo da zero. Sui telefoni che la sfocatura non
         * la fanno (o col risparmio energetico acceso) quel giro era tutto sprecato.
         * ⚠️ **Il passaggio da passante a non passante scavalca la scorciatoia**, perché quello
         * non è un dosaggio ma un cambio di stato della finestra: saltarlo lascerebbe un menu
         * intoccabile su un telefono senza sfocatura.
         */
        if (dipinto && radius == null && passante == passa) return
        val p = params ?: return
        val m = manager ?: return
        passa = passante
        p.flags = p.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        p.dimAmount = velo
        if (radius != null) {
            p.flags = p.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            p.blurBehindRadius = raggio
        }
        /*
         * ⚠️⚠️ **LA FINESTRA CHE STA SOLO FINENDO DI SCIOGLIERSI NON PRENDE PIÙ I TOCCHI**, ed è
         * la contropartita della coda della `1.61`: il pannello è già sparito, ma la finestra
         * resta viva perché la sfocatura è un suo attributo e con lei morirebbe. Senza questo
         * flag, per tutta la coda un tocco nel rettangolo dove stava il menu verrebbe raccolto da
         * una finestra invisibile invece di arrivare alla schermata.
         * ⚠️ **Si toglie e non solo si mette**: riaprendo il menu, `passante` torna falso e la
         * finestra deve tornare toccabile nello stesso fotogramma in cui il pannello ricompare.
         */
        p.flags = if (passante) {
            p.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            p.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        }
        runCatching { m.updateViewLayout(view.rootView, p) }
    }

    fun off() {
        if (!acceso) return
        acceso = false
        passa = false
        VeilStage.off(this)
        val w = window
        if (w != null) {
            w.setDimAmount(dimPrima)
            if (radius != null) {
                w.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                /*
                 * ⚠️ **Anche il raggio torna a zero, e prima restava scritto**: con la
                 * bandierina spenta non si vede niente, quindi non era un difetto visibile, ma
                 * lasciava sulla finestra un numero che dice 'sfoca di trentatré pixel' mentre
                 * nessuno sfoca. Il giorno che qualcosa riaccende quella bandierina, la
                 * sfocatura riparte da sola con un valore che nessuno ha chiesto in quel
                 * momento. Costa una riga e toglie uno stato che mente.
                 */
                w.attributes = w.attributes.apply { blurBehindRadius = 0 }
            }
            return
        }
        val p = params ?: return
        val m = manager ?: return
        p.flags = flagsPrima
        p.dimAmount = dimPrima
        if (radius != null) p.blurBehindRadius = 0
        // ⚠️ La vista può essere già stata staccata quando questo scatta, e allora il gestore
        // va in errore: non c'è niente da rimettere a posto su una finestra che non c'è più.
        runCatching { m.updateViewLayout(view.rootView, p) }
    }
}

/**
 * La finestra del dialogo che ospita questa vista, se è un dialogo.
 *
 * ⚠️ **Si risale la catena invece di guardare il solo genitore**: la scorciatoia che gira
 * dappertutto (`view.parent as DialogWindowProvider`) vale finché Compose tiene quell'esatto
 * annidamento, e un livello in più la farebbe fallire in silenzio, cioè senza velo e senza
 * errore. Il ciclo costa tre confronti.
 */
fun View.dialogWindow(): Window? {
    var nodo = parent
    while (nodo != null) {
        (nodo as? DialogWindowProvider)?.let { return it.window }
        nodo = (nodo as? View)?.parent
    }
    return null
}

/**
 * Quanto è sfocato lo sfondo.
 *
 * ⚠️ **'Leggera' sono parole sue**, quindi il raggio deve far leggere che c'è qualcosa dietro
 * senza cancellarlo: 12dp, cioè una trentina di pixel su un telefono normale. Il doppio
 * trasformerebbe lo sfondo in una macchia di colore, che è un'altra cosa e non è stata chiesta.
 * ⚠️ **In dp e non in pixel**, benché l'API voglia pixel: un raggio fisso in pixel sarebbe una
 * sfocatura diversa su ogni densità di schermo.
 */
private val BLUR = 12.dp

/**
 * Quanto scurisce il velo, sul tema chiaro e su quello scuro.
 *
 * ⚠️ **Meno sul chiaro e non per timidezza**: là la scheda è chiara sopra uno sfondo chiaro ma
 * la sua ombra e il suo bordo la staccano già, mentre sullo scuro una scheda scura sopra uno
 * sfondo scuro ha bisogno che il fondo si allontani davvero.
 * ⚠️ **Sono più bassi del velo di serie di Material** (0,6 di fabbrica sui dialoghi), perché
 * qui il lavoro lo fa in gran parte la sfocatura: 0,6 sopra uno sfondo già sfocato cancella
 * quello che c'è sotto invece di allontanarlo.
 */
private const val DIM_LIGHT = 0.20f
private const val DIM_DARK = 0.45f

/**
 * Quanto si aggiunge al velo quando la sfocatura non c'è.
 *
 * ⚠️ Vedi la nota in testa: la separazione la devono fare in due, e se uno dei due manca
 * l'altro deve valere di più. Con questo, sul chiaro senza sfocatura si arriva a 0,32, che è
 * il velo classico di un dialogo Material.
 */
private const val DIM_MORE = 0.12f

/**
 * Il velo al massimo: quello che chiede chi non ha un'animazione da seguire.
 *
 * ⚠️ **Ha un nome perché è un valore di dosaggio e non un numero qualunque**: i dialoghi di
 * Material compaiono di colpo, quindi il loro velo non ha niente da inseguire e vale sempre
 * questo. Chi invece ha un'animazione passa il suo avanzamento e il velo lo segue.
 */
private const val PIENO = 1f

/*
 * ── La coda: come la patina si scioglie ──────────────────────────────────────
 *
 * ⚠️⚠️ **DALLA `1.61` LA PATINA NON SEGUE PIÙ IL PANNELLO MENTRE ESCE, E HA UNA DISCESA
 * PROPRIA** (istruzione dell'utente, giro della `1.60`: *a prescindere da tutto il resto,
 * incluse le altre animazioni (anche contemporanee), il passaggio da sfocatura massima a
 * nessuna sfocatura dev'essere graduale e decelerare sul finale: è ancora troppo brusca*).
 *
 * ⚠️⚠️ **IL DIFETTO ERA LA CURVA, NON LA DURATA, e saperlo evita di allungare e basta**: dalla
 * `1.50` la sfocatura seguiva l'avanzamento del pannello, e l'uscita di un menu è l'entrata
 * **letta all'indietro** (`MENU_OUT` in `Menus.kt`), cioè una curva che **accelera**. Quindi gli
 * ultimi pixel di sfocatura, che sono esattamente quelli in cui l'occhio legge il passaggio da
 * sfocato a nitido, se ne andavano nel tratto più veloce di tutta l'animazione. Seguire il
 * pannello era giusto in **entrata**, dove serviva a togliere la cornice sfumata, ed era la cosa
 * sbagliata in uscita.
 *
 * ⚠️ **La cosa che l'entrata NON cambia**: in entrata la patina continua a crescere insieme al
 * pannello, con la sua stessa durata e la sua stessa curva. Il conto della `1.50` (nessun
 * fotogramma in cui la finestra sfoca più di quanto il pannello sia in scena) regge solo se il
 * numero è lo stesso, e questa coda riguarda il solo verso dell'uscita.
 *
 * ⚠️⚠️ **E LA FINESTRA DEVE RESTARE VIVA FINCHÉ LA CODA NON È FINITA**: la sfocatura è un
 * attributo della finestra, quindi una finestra che sparisce si porta via la sfocatura
 * qualunque animazione le si sia data. Per i menu lo fa `MenuState.inScene`, che adesso guarda
 * anche la patina; per la scheda in fondo non serve niente, perché la sua uscita dura già più
 * della coda. ⚠️ **Il prezzo si paga con [FLAG_NOT_TOUCHABLE]** (vedi `stendi`), o il rettangolo
 * di una finestra invisibile mangerebbe i tocchi.
 */

/**
 * Quanto ci mette la patina a sciogliersi, quando la superficie se ne è andata.
 *
 * ⚠️ **Più del doppio dell'uscita di un menu (120 ms), e non è una durata scelta a caso**: sotto
 * il doppio la discesa resta legata a quella del pannello, che è la cosa da cui l'utente l'ha
 * staccata. ⚠️ **Il tetto invece è la scheda in fondo**, la cui uscita dura `USCITA_MS`: oltre
 * quella, la coda finirebbe fuori dalla vita della finestra e l'ultimo tratto verrebbe tagliato
 * proprio dove deve decelerare.
 */
internal const val VEIL_FADE_MS = 280

/**
 * La curva con cui si scioglie: arriva con pendenza **nulla**, cioè decelerando.
 *
 * ⚠️⚠️ **SCELTA SU UNA MISURA CHE TIENE CONTO DELL'OCCHIO**: la
 * quantità di sfocatura che si **percepisce** cresce meno del raggio (fra 33 e 25 pixel non si
 * vede quasi differenza, fra 4 e 0 c'è tutto il passaggio da sfocato a nitido), quindi una curva
 * che scende in modo uniforme sul **numero** all'occhio precipita in fondo. Provate tre curve
 * sulla percezione approssimata con la radice del raggio, lo scarto da una discesa uniforme
 * viene **6,1%** per questa, 12,4% per `FastOutSlowIn` e 15,4% per la curva d'entrata dei menu.
 * ⚠️ **E il finale è comunque il tratto più lento**: `(0.25, 1)` come secondo punto dà pendenza
 * nulla all'arrivo, e negli ultimi 30 ms il raggio scende di **7,2 pixel al secondo** contro i
 * **668** di quando la sfocatura seguiva l'uscita del pannello.
 * ⚠️ **Il numero che dice tutto**: gli ultimi 4 pixel di raggio, cioè quelli in cui il passaggio
 * si vede, duravano **11 ms** (meno di un fotogramma) e adesso durano **115 ms**.
 */
internal val VEIL_FADE: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

/*
 * ── Il limite che resta, e il rifacimento che lo toglierebbe ──────────────────
 *
 * ⚠️⚠️ **UNA SFOCATURA DI FINESTRA NON PUÒ SEGUIRE UN RIQUADRO STONDATO, e questo è il solo
 * pezzo di `sfocatura-segue` che la `1.53` non chiude.** Il rettangolo della finestra adesso è
 * grande quanto il pannello disegnato (vedi `Growing` in `Menus.kt`), quindi lungo i quattro
 * lati non resta niente. Agli **angoli** sì: il pannello è stondato di `MENU_ROUND` e la
 * finestra è quadrata, quindi in ognuno dei quattro angoli sporge un triangolino di sfondo
 * sfocato largo al massimo `r * (1 - 1 / radice(2))`, cioè meno di sei dp. Non c'è modo di
 * arrotondare la regione sfocata: `WindowManager.LayoutParams` dichiara un raggio di sfocatura e
 * non una forma.
 *
 * ⚠️ **Che sia visibile o no lo dice l'occhio dell'utente e non questo commento**: è un difetto
 * costante, che c'è da quando la sfocatura esiste (`1.38`) e non si muove con l'animazione,
 * quindi è un'altra cosa dalla cornice che compariva e spariva. Chi lo cerca guardi un angolo del
 * pannello sopra una miniatura molto contrastata.
 *
 * ⚠️⚠️ **LA VIA CHE LO TOGLIEREBBE È UN RIFACIMENTO A SÉ, e va detto per che cosa costa tanto**:
 * dipingere la sfocatura **dentro** il pannello, con un `RenderEffect` ritagliato sulla sua
 * forma, invece di chiederla alla finestra. Toglie gli angoli, toglie i due
 * `updateViewLayout` per fotogramma, e in cambio vuole che l'app si disegni in un
 * `GraphicsLayer` da cui campionare quello che sta sotto: un menu è una **finestra** a sé, e
 * dalla sua finestra i pixel dell'app non si leggono. Quindi il prezzo vero non è l'effetto, è
 * che i menu smetterebbero di essere finestre, e con loro se ne andrebbero l'ordine sopra il
 * tastino, la chiusura col tasto Indietro e quella toccando fuori, che oggi arrivano gratis con
 * `Popup`.
 */

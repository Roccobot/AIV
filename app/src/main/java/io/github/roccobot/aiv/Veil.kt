package io.github.roccobot.aiv

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
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
 * ⚠️⚠️ **IL VELO È QUELLO DELLA FINESTRA, ED È NERO: la sua TINTA non si sceglie.** Android
 * offre `FLAG_DIM_BEHIND` con una sola quantità e nessun colore, e per avere un velo bianco
 * bisognerebbe possedere la finestra di ogni dialogo dell'app e dipingerla a mano, cioè
 * proprio i 'sistemi ad-hoc per il disegno di finestre e pannelli' che l'utente ha appena
 * chiesto di smettere (riscontro `striscia-sotto`).
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
 * ⚠️⚠️ **E NE VALE UNO PER VOLTA, dalla 1.53**: due finestre velate insieme si compongono e
 * scuriscono più di una sola, quindi il velo che arriva spegne quelli sotto e glieli restituisce
 * quando se ne va. Il perché per esteso, coi numeri e con le due vie scartate, sta su `Veils`.
 *
 * ⚠️⚠️ **DALLA 1.39 È SPENTO DI FABBRICA, DIETRO UN'IMPOSTAZIONE** (richiesta dell'utente,
 * 2026-09-03: *mettilo dietro un'opzione disattivata di default. Penserò se tenere o meno la
 * feature: rende tutto visibilmente più lento*). Lo dice [LocalAivVeil], e spento vuol dire
 * che qui non si tocca **niente**: ogni finestra resta com'era prima della 1.38.
 */
@Composable
fun WindowVeil(bare: Float = 0f, quanto: () -> Float = { PIENO }) {
    val view = LocalView.current
    val on = LocalAivVeil.current
    val dark = !LocalAivLight.current
    val radius = with(LocalDensity.current) { BLUR.roundToPx() }
    val misura by rememberUpdatedState(quanto)
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
     */
    LaunchedEffect(velo) {
        snapshotFlow { misura().coerceIn(0f, PIENO) }.collect { velo?.at(it) }
    }
    DisposableEffect(velo) { onDispose { velo?.off() } }
}

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
     * uno solo, lo farebbe vedere: si è provato a farlo salire con un'animazione e la prova è
     * finita qui, perché su un dialogo aperto **senza** un menu sotto quella salita partiva da
     * sotto il velo di sistema, cioè schiariva lo sfondo prima di scurirlo. Il passaggio di
     * consegne col menu si risolve altrove, in `Veils`.
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
                radius = if (blurred) radius else null
            )
        }
        bare > 0f -> Veil(view, dim = bare, radius = null)
        else -> null
    }

/**
 * Chi ha il velo **adesso**: uno solo per volta, e il passaggio avviene in un fotogramma.
 *
 * ⚠️⚠️ **NASCE PER TOGLIERE IL LAMPO, dalla 1.53, e il meccanismo va capito o sembra una
 * complicazione** (riscontro dell'utente, giro della `1.51`, voce `flash-menu-dialogo`: *c'è
 * ancora*). Il fatto da cui parte tutto: due finestre che chiedono il velo insieme **si
 * compongono**, perché ognuna scurisce tutto quello che sta dietro di lei, compresa l'altra. Un
 * velo da 0,45 sotto un altro da 0,45 non fa 0,45: fa `1-(1-0,45)²`, cioè 0,70. Quando una voce
 * di menu apre un dialogo, il menu resta in scena il tempo della sua uscita, e in quel tratto lo
 * sfondo diventava più buio di quanto sia prima e dopo. Quel buio che va e viene è il lampo, e
 * non era la sfocatura a farlo.
 *
 * ⚠️⚠️ **E IL RIMEDIO NON È UNA DISSOLVENZA INCROCIATA, che è la cosa che si prova per prima**:
 * far salire il velo del dialogo mentre quello del menu scende terrebbe la somma quasi ferma
 * (0,45 -> 0,40 -> 0,45 invece di 0,45 -> 0,70 -> 0,45), ma romperebbe il caso più comune, cioè
 * un dialogo aperto **senza** nessun menu sotto: la finestra di un dialogo porta già il velo di
 * sistema del tema, quindi una salita da zero lo **schiarirebbe** per il tempo della salita
 * prima di scurirlo. Un lampo al posto di un altro.
 *
 * ⚠️⚠️ **QUINDI IL VELO CHE ARRIVA SPEGNE QUELLI SOTTO, e lo fa nello stesso istante in cui si
 * accende**: la sostituzione avviene dentro la stessa chiamata, quindi non esiste nessun
 * fotogramma con due veli e nessuno con zero. È la differenza con la `1.48`, che aveva provato a
 * spegnere il velo del menu **prima** che il dialogo esistesse e aveva lasciato un buco di uno o
 * due fotogrammi.
 * ⚠️ **La sfocatura non si perde spegnendo quello sotto**: chi sta in cima sfoca tutto quello che
 * ha dietro, cioè l'app **e** la finestra del menu che sta uscendo. Quello che si vede attraverso
 * resta sfocato per tutta la transizione.
 * ⚠️ **E il dosaggio di chi sta sotto non si perde**: se il velo di cima se ne va prima, quello
 * sotto ritorna col valore che aveva ([Veil.restore]), che per un menu è il punto in cui la sua
 * animazione è arrivata.
 *
 * ⚠️ **Una pila e non un contatore**, perché serve sapere **chi** era sotto per farlo tornare; e
 * gira tutta sul thread principale, dove vivono la composizione e le animazioni, quindi non ha
 * niente da sincronizzare.
 * ⚠️ **A funzione spenta non fa niente**: senza l'impostazione i menu e i dialoghi non creano
 * nessun velo (lo dice `veilFor`), quindi in pila c'è al più la scheda in fondo e non c'è niente
 * da spegnere.
 */
private object Veils {
    private val pila = mutableListOf<Veil>()

    fun push(velo: Veil) {
        if (pila.lastOrNull() === velo) return
        pila.remove(velo)
        pila.lastOrNull()?.mute()
        pila += velo
    }

    fun drop(velo: Veil) {
        val cima = pila.lastOrNull() === velo
        pila.remove(velo)
        if (cima) pila.lastOrNull()?.restore()
    }

    fun top(velo: Veil) = pila.lastOrNull() === velo
}

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
private class Veil(private val view: View, private val dim: Float, private val radius: Int?) {
    private var window: Window? = null
    private var manager: WindowManager? = null
    private var params: WindowManager.LayoutParams? = null
    private var dimPrima = 0f
    private var flagsPrima = 0
    private var acceso = false
    private var dose = 0f

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
        Veils.push(this)
        return true
    }

    /**
     * Il dosaggio chiesto: si stende solo se questo velo è quello in cima. Vedi [Veils].
     */
    fun at(q: Float) {
        if (!grab()) return
        dose = q
        if (Veils.top(this)) stendi(q)
    }

    /** Si fa da parte perché un altro velo è arrivato sopra. */
    fun mute() = stendi(0f)

    /** Torna al dosaggio che aveva quando si è fatto da parte. */
    fun restore() = stendi(dose)

    private fun stendi(q: Float) {
        val velo = dim * q
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
        val p = params ?: return
        val m = manager ?: return
        p.flags = p.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        p.dimAmount = velo
        if (radius != null) {
            p.flags = p.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            p.blurBehindRadius = raggio
        }
        runCatching { m.updateViewLayout(view.rootView, p) }
    }

    fun off() {
        if (!acceso) return
        acceso = false
        dose = 0f
        Veils.drop(this)
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

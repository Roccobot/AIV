package io.github.roccobot.aiv

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
 * ⚠️⚠️ **DALLA 1.39 È SPENTO DI FABBRICA, DIETRO UN'IMPOSTAZIONE** (richiesta dell'utente,
 * 2026-09-03: *mettilo dietro un'opzione disattivata di default. Penserò se tenere o meno la
 * feature: rende tutto visibilmente più lento*). Lo dice [LocalAivVeil], e spento vuol dire
 * che qui non si tocca **niente**: ogni finestra resta com'era prima della 1.38.
 */
@Composable
fun WindowVeil(bare: Float = 0f) {
    val view = LocalView.current
    val on = LocalAivVeil.current
    val dark = !LocalAivLight.current
    val radius = with(LocalDensity.current) { BLUR.roundToPx() }
    DisposableEffect(view, on, bare, dark, radius) {
        val disfa = when {
            on -> veilWindow(view, dark, radius)
            bare > 0f -> paint(view, dim = bare, radius = null)
            else -> ({ })
        }
        onDispose { disfa() }
    }
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
    private var disfa: (() -> Unit)? = null

    override fun onAttach() {
        if (!currentValueOf(LocalAivVeil)) return
        val view = currentValueOf(LocalView)
        val dark = !currentValueOf(LocalAivLight)
        val radius = with(currentValueOf(LocalDensity)) { BLUR.roundToPx() }
        disfa = veilWindow(view, dark, radius)
    }

    override fun onDetach() {
        disfa?.invoke()
        disfa = null
    }
}

/**
 * Quanto velo e quanta sfocatura vuole questa finestra, e poi li mette.
 *
 * ⚠️ **Il calcolo sta qui e la stesura in [paint]**, perché il velo semplice della `1.39` (la
 * scheda quando la funzione è spenta) vuole la seconda metà senza la prima: la sfocatura non
 * la chiede e la quantità gliela dice chi chiama.
 */
private fun veilWindow(view: View, dark: Boolean, radius: Int): () -> Unit {
    val manager = view.context.getSystemService(WindowManager::class.java)
    /*
     * ⚠️⚠️ **LA SFOCATURA SI CHIEDE AL SISTEMA, NON SI DÀ PER SCONTATA**: da Android 12 il
     * telefono può dire di no in qualunque momento (risparmio energetico acceso, hardware che
     * non la fa), e `isCrossWindowBlurEnabled` è la domanda giusta. Chi la desse per fatta si
     * ritroverebbe con un velo troppo leggero proprio dove la sfocatura manca.
     */
    val blurred = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        manager?.isCrossWindowBlurEnabled == true
    val dim = (if (dark) DIM_DARK else DIM_LIGHT) + (if (blurred) 0f else DIM_MORE)
    return paint(view, dim = dim, radius = if (blurred) radius else null)
}

/**
 * Stende [dim] di velo, e [radius] di sfocatura se non è nullo, sulla finestra di [view]; poi
 * ritorna come si disfa.
 *
 * ⚠️ **Ritorna l'annullamento invece di lasciarlo ricostruire a chi chiama**: quello che va
 * rimesso a posto dipende da quello che è stato acceso (la sfocatura può non esserci), e due
 * copie di quella condizione, una all'andata e una al ritorno, divergono al primo ritocco.
 */
private fun paint(view: View, dim: Float, radius: Int?): () -> Unit {
    val manager = view.context.getSystemService(WindowManager::class.java)

    val window = view.dialogWindow()
    if (window != null) {
        // ⚠️ Su un dialogo si passa dalla finestra e non dai suoi parametri a mano: è lei che
        // li riapplica, e `setDimAmount` accende già il flag che serve.
        val prima = window.attributes.dimAmount
        window.setDimAmount(dim)
        if (radius != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = radius }
        }
        return {
            window.setDimAmount(prima)
            if (radius != null) window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }
    }

    /*
     * ⚠️⚠️ **UN `Popup` È UNA FINESTRA ANCHE LUI, ma non ha un `Window`**: Compose lo aggiunge
     * al `WindowManager` come vista, quindi i suoi parametri sono i `LayoutParams` della
     * **radice** di questo albero, e per farli valere si ripassa dal gestore con
     * `updateViewLayout`. È la sola via per dare un velo a un menu, che di suo non ne ha
     * nessuno.
     */
    val root = view.rootView
    val params = root.layoutParams as? WindowManager.LayoutParams ?: return { }
    if (manager == null) return { }
    val flagsPrima = params.flags
    val dimPrima = params.dimAmount
    params.flags = params.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
    params.dimAmount = dim
    if (radius != null) {
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        params.blurBehindRadius = radius
    }
    runCatching { manager.updateViewLayout(root, params) }
    return {
        params.flags = flagsPrima
        params.dimAmount = dimPrima
        if (radius != null) params.blurBehindRadius = 0
        // ⚠️ La vista può essere già stata staccata quando questo scatta, e allora il gestore
        // va in errore: non c'è niente da rimettere a posto su una finestra che non c'è più.
        runCatching { manager.updateViewLayout(root, params) }
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

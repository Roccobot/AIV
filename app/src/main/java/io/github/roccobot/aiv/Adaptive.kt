package io.github.roccobot.aiv

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Le soglie di larghezza del layout tablet, in dp.
 *
 * ⚠️⚠️ **SONO LE MISURE DEL MOCKUP (600 / 800 / 1024), non quelle Material (600 / 840)**
 * (decisione di Rocco, 2026-10-02: breakpoint del mockup; prima schermata nativa il
 * Visualizzatore). Il mockup in `publish/tablet.html` usa tre formati fissi: orizzontale
 * 1.024, verticale 800, finestra ridotta 600. Qui diventano soglie: sotto 600 resta il
 * layout telefono; da 600 in su il pannello laterale è possibile; da 1.024 in su si apre
 * di serie.
 * ⚠️ **Non sono WindowSizeClass di Material**: adottarle avrebbe spostato il pannello info
 * a 840 invece che a 1.024, e avrebbe smentito il mockup già approvato (`d-tablet-layout`).
 */
object Adaptive {
    /** Sotto questa larghezza resta il layout telefono: niente colonna laterale. */
    const val PHONE_MAX = 600

    /**
     * Fra [PHONE_MAX] e questa soglia: tablet stretto / finestra ridotta (formato 600 del
     * mockup). Il pannello info esiste ma resta chiuso finché non lo si chiede.
     */
    const val NARROW_MAX = 800

    /**
     * Da qui in su il pannello info del Visualizzatore si apre di serie, come nel mockup
     * orizzontale a 1.024 dp.
     */
    const val SIDE_OPEN_MIN = 1024

    /** Se a questa larghezza il Visualizzatore può ospitare il pannello info a lato. */
    fun sideAvailable(widthDp: Int): Boolean = widthDp >= PHONE_MAX

    /**
     * Se il pannello info nasce aperto.
     *
     * ⚠️ **Chiuso sotto i 1.024 dp** (decisione di Rocco): in verticale e nella finestra
     * ridotta le informazioni tornano a richiesta, come nel mockup (`.viewer-side` nascosto
     * in `portrait` e `split`).
     */
    fun sideDefaultOpen(widthDp: Int): Boolean = widthDp >= SIDE_OPEN_MIN

    /** Fascia di larghezza, per prove e per tarare la colonna. */
    fun band(widthDp: Int): Band = when {
        widthDp < PHONE_MAX -> Band.PHONE
        widthDp < NARROW_MAX -> Band.NARROW
        widthDp < SIDE_OPEN_MIN -> Band.MEDIUM
        else -> Band.WIDE
    }

    /**
     * Larghezza della colonna info, allineata al mockup.
     *
     * ⚠️ I numeri vengono da `publish/tablet.css`: `.side` 280, `.split .side` 210,
     * e una via di mezzo a 800. Non sono misure inventate sul telefono.
     */
    fun sideWidth(widthDp: Int): Dp = when (band(widthDp)) {
        Band.PHONE -> 0.dp
        Band.NARROW -> 210.dp
        Band.MEDIUM -> 240.dp
        Band.WIDE -> 280.dp
    }


    /**
     * Se l'editor porta gli strumenti a lato del canvas.
     *
     * ⚠️ **Solo da 1.024 dp in su**, come nel mockup: in verticale (800) e nella finestra
     * ridotta (600) gli strumenti restano sotto il canvas (`.portrait .tools` / `.split .tools`).
     */
    fun editorBeside(widthDp: Int): Boolean = widthDp >= SIDE_OPEN_MIN

    /**
     * Larghezza della colonna strumenti dell'editor, allineata al mockup (`.tools` 320).
     */
    fun editorToolsWidth(widthDp: Int): Dp =
        if (editorBeside(widthDp)) 320.dp else 0.dp


    /**
     * Larghezza massima delle finestre centrate (rinomina, download, conversione, salva),
     * allineata al mockup (`.dialog-preview` 520).
     *
     * ⚠️ **Vale su ogni larghezza**: sotto i 520 dp non stringe niente; da tablet in su
     * evita finestre a tutta fascia.
     */
    val dialogMaxWidth: Dp = 520.dp

    /**
     * Larghezza massima del dialogo Copia/sposta su tablet, più ampia delle finestre
     * a campo singolo perché porta un elenco (mockup ~800).
     *
     * ⚠️ **`null` sul telefono**: niente tetto, la finestra resta a tutto schermo.
     */
    fun destinationMaxWidth(widthDp: Int): Dp? =
        if (sideAvailable(widthDp)) 800.dp else null

    /**
     * Se Dimensioni e filigrana mettono l'anteprima a lato dei parametri.
     *
     * ⚠️ **Solo da 1.024 dp**, come l'editor: in verticale e nella finestra ridotta
     * restano uno sotto l'altro (mockup `.portrait` / `.split`).
     */
    fun previewBeside(widthDp: Int): Boolean = widthDp >= SIDE_OPEN_MIN

    /**
     * Larghezza massima di una finestra con anteprima a lato (Ridimensiona),
     * piu ampia di [dialogMaxWidth] perche porta due colonne.
     */
    val dialogBesideMaxWidth: Dp = 720.dp

    /**
     * Larghezza massima di una bottomsheet sul tablet (Guida, Filigrana, Informazioni).
     *
     * ⚠️ **`null` sul telefono**: la scheda resta a tutta fascia come prima.
     */
    fun sheetMaxWidth(widthDp: Int): Dp? =
        if (sideAvailable(widthDp)) 720.dp else null

    enum class Band { PHONE, NARROW, MEDIUM, WIDE }

    /**
     * La forma dello schermo, che dalla `3.70` decide dove vivono l'elenco delle cartelle e i
     * comandi del FAB (richiesta dell'utente del 2026-10-04, mockup `Tablet_H` e `Tablet_V`).
     *
     * - [Shape.PHONE]: il telefono in verticale, o una finestra stretta. Tutto come prima.
     * - [Shape.WIDE]: lo schermo più largo che alto, con la larghezza del layout a due colonne:
     *   telefono e tablet in orizzontale. L'elenco delle cartelle è ancorato in basso nella sua
     *   colonna, e i comandi del FAB sono in una pillola sotto il filtro.
     * - [Shape.TALL]: il tablet in verticale. L'elenco resta com'è, e i comandi del FAB sono in
     *   una pillola in basso.
     *
     * ⚠️⚠️ **Telefono e tablet si distinguono dal LATO MINORE, non dalla larghezza** (scelta B1):
     * sotto i [PHONE_MAX] dp è un telefono in qualunque orientamento, che è il criterio di
     * Android. La larghezza da sola cambia ruotando, e un telefono in orizzontale passerebbe per
     * un tablet.
     */
    fun shape(widthDp: Int, heightDp: Int, smallestDp: Int): Shape = when {
        widthDp > heightDp && sideAvailable(widthDp) -> Shape.WIDE
        smallestDp >= PHONE_MAX -> Shape.TALL
        else -> Shape.PHONE
    }

    /**
     * Se l'app va a tutto schermo, con le barre di sistema nascoste: solo sul telefono, in tutte
     * le schermate quando è in orizzontale e nel solo visualizzatore quando è in verticale.
     *
     * ⚠️⚠️ **Il visualizzatore in verticale c'è dalla `3.71`** (voce `3.70-01` non approvata:
     * *solo per lo smartphone: lasciare il resto dell'app in modalità normale e mettere a tutto
     * schermo solo visualizzatore (verticale) e tutte le schermate incluso il visualizzatore
     * (orizzontale)*). In verticale il resto dell'app tiene le sue barre.
     */
    fun immersive(widthDp: Int, heightDp: Int, smallestDp: Int, viewer: Boolean): Boolean =
        smallestDp < PHONE_MAX && (widthDp > heightDp || viewer)

    enum class Shape { PHONE, WIDE, TALL }
}

/** La forma dello schermo in questo momento: vedi [Adaptive.shape]. */
@Composable
fun screenShape(): Adaptive.Shape {
    val conf = LocalConfiguration.current
    return Adaptive.shape(conf.screenWidthDp, conf.screenHeightDp, conf.smallestScreenWidthDp)
}

/**
 * Lo spazio di sistema che una schermata lascia libero, fermo mentre le barre compaiono.
 *
 * ⚠️⚠️ **DALLA `3.73`, PER LO SFARFALLIO TORNANDO ALLA GRIGLIA** (voce `3.72-01`: *dopo aver
 * visualizzato un'immagine, se torno alla griglia c'è (non sempre ma quasi) uno sfarfallio tipo
 * nastro video difettoso*). Dalla `3.71` il visualizzatore in verticale è a tutto schermo, e
 * uscendo le barre ricompaiono con un'animazione: `safeDrawing` le segue fotogramma per
 * fotogramma, quindi durante la dissolvenza fra le schermate la griglia si spostava a ogni
 * fotogramma. La causa è dedotta e non misurata sul telefono: nel banco le barre non esistono.
 * ⚠️ **Dove la schermata non è a tutto schermo si contano le barre anche mentre sono nascoste**
 * (`systemBarsIgnoringVisibility`), quindi lo spazio è già quello finale quando compaiono. Dove
 * è a tutto schermo resta `safeDrawing`, che lì vale il solo ritaglio del display.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun steadyDrawing(
    /**
     * Se a chiedere è il visualizzatore, che sul telefono è a tutto schermo anche in verticale.
     *
     * ⚠️⚠️ **DALLA `4.30`, PER LO ZOOM ALL'APERTURA** (nota D del giro della `4.25`: *è tornata
     * un'animazione di zoom all'apertura delle immagini*, e in chat: *piccola, poi cresce*, aprendo
     * dalla griglia). Il visualizzatore misurava la sua barra con `safeDrawing`, che segue le barre
     * di sistema mentre si nascondono: la barra delle info nasceva alta quanto la sua riga più la
     * barra di navigazione e si accorciava fotogramma per fotogramma, e lo spazio che lascia
     * all'immagine cresceva con lei, animato. Qui vale già lo spazio finale, cioè il solo ritaglio
     * del display (e la tastiera). La causa è dedotta e non misurata sul telefono: nel banco le
     * barre non esistono.
     * ⚠️⚠️ **E NON ERA LA SOLA, NÉ FORSE LA VERA**: sulla `4.30` lo zoom restava con le info
     * spente, e la causa di quel caso era lo stato della barra delle info, che nasceva visibile
     * anche con l'impostazione spenta (corretto nella `4.33`, misurato da `BarraInfoTest`). Se
     * questa riga serva anche con le info accese non è misurato.
     */
    viewer: Boolean = false
): WindowInsets {
    val conf = LocalConfiguration.current
    val full = Adaptive.immersive(
        conf.screenWidthDp, conf.screenHeightDp, conf.smallestScreenWidthDp, viewer = viewer
    )
    // ⚠️ Fuori dal tutto schermo il visualizzatore tiene `safeDrawing`, come prima della `4.30`: là
    // le barre restano, e la misura che le conta non si muove (la prova del tablet in orizzontale).
    if (viewer) return if (full) WindowInsets.displayCutout.union(WindowInsets.ime) else WindowInsets.safeDrawing
    return if (full) {
        WindowInsets.safeDrawing
    } else {
        WindowInsets.systemBarsIgnoringVisibility
            .union(WindowInsets.displayCutout)
            .union(WindowInsets.ime)
    }
}

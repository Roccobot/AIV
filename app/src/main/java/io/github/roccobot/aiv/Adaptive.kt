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
 * ⚠️ **Sono le misure del mockup `publish/tablet.html` (600, 800, 1024), non le `WindowSizeClass`
 * di Material (600, 840)**: decisione dell'utente (`d-tablet-layout`). Sotto 600 resta il layout
 * del telefono, da 600 il pannello laterale è possibile, da 1024 si apre di serie; con Material il
 * pannello delle informazioni si sposterebbe a 840.
 */
object Adaptive {
    /** Sotto questa larghezza resta il layout telefono: niente colonna laterale. */
    const val PHONE_MAX = 600

    /**
     * Fra [PHONE_MAX] e questa soglia: tablet stretto o finestra ridotta (il formato 600 del
     * mockup). Il pannello delle informazioni esiste ma resta chiuso finché non lo si chiede.
     */
    const val NARROW_MAX = 800

    /** Da qui in su il pannello delle informazioni del visualizzatore si apre di serie. */
    const val SIDE_OPEN_MIN = 1024

    /** Se a questa larghezza il visualizzatore può avere il pannello delle informazioni a lato. */
    fun sideAvailable(widthDp: Int): Boolean = widthDp >= PHONE_MAX

    /**
     * Se il pannello delle informazioni nasce aperto: chiuso sotto i 1024 dp, come nel mockup
     * (`.viewer-side` nascosto in `portrait` e `split`).
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
     * Larghezza della colonna delle informazioni, presa da `publish/tablet.css`: `.side` 280,
     * `.split .side` 210, e una via di mezzo a 800.
     */
    fun sideWidth(widthDp: Int): Dp = when (band(widthDp)) {
        Band.PHONE -> 0.dp
        Band.NARROW -> 210.dp
        Band.MEDIUM -> 240.dp
        Band.WIDE -> 280.dp
    }


    /**
     * Se l'editor mette gli strumenti a lato della tela: solo da 1024 dp, come nel mockup; sotto,
     * gli strumenti sono sotto la tela (`.portrait .tools`, `.split .tools`).
     */
    fun editorBeside(widthDp: Int): Boolean = widthDp >= SIDE_OPEN_MIN

    /** Larghezza della colonna degli strumenti dell'editor, dal mockup (`.tools` 320). */
    fun editorToolsWidth(widthDp: Int): Dp =
        if (editorBeside(widthDp)) 320.dp else 0.dp


    /**
     * Larghezza massima delle finestre centrate (rinomina, download, conversione, salva), dal
     * mockup (`.dialog-preview` 520). Vale su ogni larghezza: sotto i 520 dp non stringe niente.
     */
    val dialogMaxWidth: Dp = 520.dp

    /**
     * Larghezza massima della finestra Copia o sposta sul tablet, più ampia delle finestre a campo
     * singolo perché contiene un elenco. `null` sul telefono, dove la finestra è a tutto schermo.
     */
    fun destinationMaxWidth(widthDp: Int): Dp? =
        if (sideAvailable(widthDp)) 800.dp else null

    /**
     * Se Ridimensiona e Filigrana mettono l'anteprima a lato dei parametri: solo da 1024 dp, come
     * l'editor (mockup `.portrait`, `.split`).
     */
    fun previewBeside(widthDp: Int): Boolean = widthDp >= SIDE_OPEN_MIN

    /**
     * Larghezza massima di una finestra con anteprima a lato (Ridimensiona), più ampia di
     * [dialogMaxWidth] perché ha due colonne.
     */
    val dialogBesideMaxWidth: Dp = 720.dp

    /**
     * Larghezza massima di una scheda dal basso sul tablet (Guida, Filigrana, Informazioni). `null`
     * sul telefono, dove la scheda è a tutta larghezza.
     */
    fun sheetMaxWidth(widthDp: Int): Dp? =
        if (sideAvailable(widthDp)) 720.dp else null

    enum class Band { PHONE, NARROW, MEDIUM, WIDE }

    /**
     * La forma dello schermo, che decide dove sono l'elenco delle cartelle e i comandi del FAB
     * (mockup `Tablet_H` e `Tablet_V`).
     *
     * - [Shape.PHONE]: il telefono in verticale, o una finestra stretta.
     * - [Shape.WIDE]: lo schermo più largo che alto, con la larghezza delle due colonne (telefono e
     *   tablet in orizzontale). L'elenco delle cartelle è ancorato in basso nella sua colonna, e i
     *   comandi del FAB sono in una pillola sotto il filtro.
     * - [Shape.TALL]: il tablet in verticale. I comandi del FAB sono in una pillola in basso.
     *
     * ⚠️ **Telefono e tablet si distinguono dal lato minore, non dalla larghezza** (scelta B1
     * dell'utente): sotto [PHONE_MAX] dp è un telefono in qualunque orientamento, come per Android.
     * La larghezza cambia ruotando, e un telefono in orizzontale passerebbe per un tablet.
     */
    fun shape(widthDp: Int, heightDp: Int, smallestDp: Int): Shape = when {
        widthDp > heightDp && sideAvailable(widthDp) -> Shape.WIDE
        smallestDp >= PHONE_MAX -> Shape.TALL
        else -> Shape.PHONE
    }

    /**
     * Se l'app va a tutto schermo, con le barre di sistema nascoste: solo sul telefono, in tutte le
     * schermate in orizzontale e nel solo visualizzatore in verticale (voce `3.70-01`).
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
 * Uscendo dal visualizzatore a tutto schermo le barre ricompaiono con un'animazione, e
 * `safeDrawing` le segue fotogramma per fotogramma: durante la dissolvenza la griglia sfarfallava.
 * Dove la schermata non è a tutto schermo si contano le barre anche nascoste
 * (`systemBarsIgnoringVisibility`), quindi lo spazio è già quello finale; dove è a tutto schermo
 * resta `safeDrawing`, che vale il solo ritaglio del display. La causa è dedotta e non misurata sul
 * telefono: nel banco le barre non esistono.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun steadyDrawing(
    /**
     * Se a chiedere è il visualizzatore, che sul telefono è a tutto schermo anche in verticale.
     *
     * A tutto schermo vale già lo spazio finale (ritaglio del display e tastiera): con
     * `safeDrawing` la barra delle informazioni nasceva alta quanto la barra di navigazione e si
     * accorciava mentre le barre si nascondevano, e l'immagine cresceva con lei all'apertura. Causa
     * dedotta e non misurata; l'altra causa dello stesso effetto, la barra delle informazioni
     * visibile con l'impostazione spenta, è presidiata da `BarraInfoTest`.
     */
    viewer: Boolean = false
): WindowInsets {
    val conf = LocalConfiguration.current
    val full = Adaptive.immersive(
        conf.screenWidthDp, conf.screenHeightDp, conf.smallestScreenWidthDp, viewer = viewer
    )
    // Fuori dal tutto schermo il visualizzatore tiene `safeDrawing`: là le barre restano, e la
    // misura che le conta non si muove.
    if (viewer) return if (full) WindowInsets.displayCutout.union(WindowInsets.ime) else WindowInsets.safeDrawing
    return if (full) {
        WindowInsets.safeDrawing
    } else {
        WindowInsets.systemBarsIgnoringVisibility
            .union(WindowInsets.displayCutout)
            .union(WindowInsets.ime)
    }
}

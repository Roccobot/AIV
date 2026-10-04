package io.github.roccobot.aiv

import androidx.compose.runtime.Composable
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
     * Se l'app va a tutto schermo, con le barre di sistema nascoste: solo il telefono in
     * orizzontale (richiesta B1, scelte B2 e A4), dove lo spazio in altezza è il più prezioso.
     */
    fun immersive(widthDp: Int, heightDp: Int, smallestDp: Int): Boolean =
        smallestDp < PHONE_MAX && widthDp > heightDp

    enum class Shape { PHONE, WIDE, TALL }
}

/** La forma dello schermo in questo momento: vedi [Adaptive.shape]. */
@Composable
fun screenShape(): Adaptive.Shape {
    val conf = LocalConfiguration.current
    return Adaptive.shape(conf.screenWidthDp, conf.screenHeightDp, conf.smallestScreenWidthDp)
}


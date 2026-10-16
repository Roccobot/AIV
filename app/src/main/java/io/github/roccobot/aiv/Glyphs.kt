package io.github.roccobot.aiv

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp

/**
 * Le icone dell'app: il catalogo, coi nomi con cui si chiamano dal codice. Quelle disegnate qui o
 * dall'utente sono voci dirette, quelle di Material sono in [Material]. Come entra un disegno, e
 * quando vince Material, lo dice `Rules.md` § '🖌️ Come entra un disegno'.
 *
 * I disegni vivono in `res/drawable`, un file per glifo, e questo file dice che cosa vuol dire
 * ognuno: così il verificatore `tools/icon-check.py` e l'app leggono lo stesso file.
 * ⚠️ **`aapt2` non guarda dentro `android:pathData`**: un tracciato malformato compila, e lo
 * legge `PathParser` al primo disegno a schermo. La rete è il verificatore, e va lanciato.
 *
 * Il cursore di testo è il solo disegno scritto qui: è calcolato da costanti con una relazione
 * ([PULL] è il doppio di [NOTCH]) che in XML diventerebbe due numeri slegati.
 *
 * Un disegno si legge con `ImageVector.vectorResource`, che è `@Composable` e tiene il risultato
 * in un `remember` con chiave la risorsa, il tema e la configurazione. Il tipo resta
 * `ImageVector`, non `Painter`: `MenuRow` di `Menus.kt` misura lo spazio dell'icona con
 * `defaultWidth` e `defaultHeight`, che un `Painter` non ha.
 * Il colore dei file è nero pieno: `Icon` lo tinge con un `ColorFilter.tint`, come le icone di
 * Material.
 *
 * Una famiglia di glifi alternativa disegnata dall'utente è in Claude Design (`Roccobot Design`,
 * `assets/icons/`) e nella storia git; per ora restano questi, su sua istruzione.
 */
object Glyphs {

    /**
     * Il cursore di testo, cioè la I con le due lineette: 'Rinomina'.
     *
     * Di tratto e non di pieno, a differenza di tutti gli altri, come l'ha scelto l'utente; lo
     * spessore è 2, come le altre a 24dp. Le estremità sono tonde (`StrokeCap.Round`): a spigolo
     * vivo i tre tratti perdono il richiamo al cursore.
     *
     * Le due lineette rientrano verso il centro, appena percettibili (richiesta dell'utente):
     * è la forma della I con le grazie. La rientranza vale [NOTCH] unità su 24, due pixel scarsi
     * a densità 2,75.
     */
    val TextCursor: ImageVector by lazy {
        ImageVector.Builder(
            name = "TextCursor",
            defaultWidth = SIZE,
            defaultHeight = SIZE,
            viewportWidth = GRID,
            viewportHeight = GRID
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round
            ) {
                // La lineetta in alto, che si incurva verso il basso.
                moveTo(9f, 4f)
                quadTo(12f, 4f + PULL, 15f, 4f)
                // Quella in basso, che si incurva verso l'alto.
                moveTo(9f, 20f)
                quadTo(12f, 20f - PULL, 15f, 20f)
                // ⚠️ L'asta si ferma sul ventre della curva, non su 4 e 20, dove sono le punte
                // delle lineette: sporgerebbe oltre la rientranza. La capocchia tonda sporge di
                // mezzo spessore e coincide col bordo esterno della lineetta, senza gradino.
                moveTo(12f, 4f + NOTCH)
                verticalLineTo(20f - NOTCH)
            }
        }.build()
    }

    /**
     * Un cursore di testo davanti alle lettere `xyz`: 'Estensione'.
     *
     * Disegnato dall'utente. Il cursore è lo stesso segno di [TextCursor], che firma 'Rinomina':
     * le due finestre hanno gli stessi comandi. Il resto è in testa a `ic_extension.xml`.
     */
    val Extension: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_extension)

    /**
     * 'Mostra nascoste': una cartella con un occhio, nel menu della schermata iniziale.
     *
     * Non è l'occhio di Material, che non dice di che cosa: qui si nomina la cartella.
     */
    val FolderEye: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_folder_eye)

    /** 'Nascondi cartelle': la gemella con l'occhio sbarrato. */
    val FolderEyeOff: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_folder_eye_off)

    /**
     * Un chevron in su: il tasto che porta in cima alla lista.
     *
     * Disegnato dall'utente. Non è la freccia di Material, di spessore costante: questo si
     * assottiglia verso le punte, che sono tonde.
     */
    val BrowseTop: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_browse_top)

    /** Un chevron in giù: la gemella che porta in fondo. */
    val BrowseBottom: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_browse_bottom)

    /**
     * La pillola verticale del Menu 'Start' a riposo: una capsula, disegno dell'utente. Segna il
     * chip `Pillola` della forma a riposo nelle impostazioni (`MainControlChoices`).
     */
    val RestPill: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_rest_pill)

    /** Il tondo del Menu 'Start' a riposo, sul chip `Tondo`. Disegno dell'utente. */
    val RestRound: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_rest_round)

    /**
     * Due fogli sovrapposti, quello davanti con montagne e sole: 'Copia immagine'.
     *
     * ⚠️ **È il solo glifo che esce dalla propria tela**: si dichiara 25dp su una griglia da 25, e
     * chi lo disegna gli dà uno spazio da 24. Il perché è in testa a `ic_photo_pair.xml`, il
     * trattamento dello spazio in `MenuRow` di `Menus.kt`.
     */
    val PhotoPair: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_photo_pair)

    /**
     * Una matita dentro un riquadro: 'Modifica'.
     *
     * Il riquadro è il segno di famiglia delle voci sull'immagine, come le due cartelle
     * sovrapposte lo sono di 'Copia' e 'Sposta'.
     */
    val ImageEdit: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_image_edit)

    /**
     * Due frecce opposte dentro un riquadro: 'Esporta/Converti'. Il riquadro dice su che cosa.
     */
    val ImageConvert: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_image_convert)

    /** Due cartelle sovrapposte: 'Copia'. */
    val FolderPair: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_folder_pair)

    /**
     * Due cartelle sovrapposte con quella dietro **tratteggiata**: 'Sposta'.
     *
     * Il tratteggio è sulla cartella di dietro: spostando, l'originale non resta dov'era. Perché i
     * trattini sono pezzi di tracciato è scritto in testa a `ic_folder_pair_dashed.xml`.
     */
    val FolderPairDashed: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_folder_pair_dashed)

    /**
     * Una cartella a contorno con dentro un più: 'Nuova cartella'.
     *
     * Disegnata dall'utente, per la voce del menu del FAB in 'Cartelle di sistema'. Non è
     * `Icons.Filled.CreateNewFolder`, una cartella piena (la misura è in testa a
     * `ic_folder_new.xml`).
     */
    val FolderNew: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_folder_new)

    /**
     * Una cartella a contorno con dentro una freccia che scende: 'Percorso'.
     *
     * Disegnata dall'utente. La freccia scende: la sagoma più vicina di Material,
     * `DriveFolderUpload`, ha la freccia in su (la misura è in testa a `ic_folder_download.xml`).
     * La cartella è la stessa di [FolderNew] e di [FolderAiv], allo stesso inchiostro.
     */
    val FolderDownload: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_folder_download)

    /**
     * Un foglio dietro, e davanti un riquadro **tagliato da una spunta**: 'Seleziona tutto'.
     *
     * Una spunta grande, nel disegno dell'utente. La spunta non è dipinta: è il taglio che divide
     * il riquadro in due pezzi. Il resto è in testa a `ic_pick_all.xml`.
     */
    val PickAll: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_pick_all)

    /**
     * Un foglio dietro a un riquadro con una **croce**: 'Svuota selezione'.
     *
     * Disegno dell'utente: la croce arriva quasi al bordo del riquadro.
     */
    val PickNone: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_pick_none)

    /**
     * Un riquadro con una freccia che scende: 'Scarica'.
     *
     * Disegnato dall'utente: il riquadro è il segno di famiglia delle voci del menu del
     * visualizzatore, e dice che cosa si scarica.
     */
    val Download: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_download)

    /**
     * Un foglio dietro a un riquadro con **mezzo quadrato pieno**: 'Inverti selezione'.
     *
     * Il mezzo quadrato dice 'l'altra metà'. Il triangolo gira al contrario del riquadro perché è
     * così che lo buca: il perché è in testa a `ic_pick_invert.xml`.
     */
    val PickInvert: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_pick_invert)

    /**
     * Centra la selezione in orizzontale, nell'editor.
     *
     * Disegnata dall'utente su una griglia 800x800 (voce `ed-sheet`): la tela resta la sua, e la
     * misura la dichiarano `android:width` e `android:height`.
     * ⚠️ **È più piccola della famiglia** (17,04 unità su 24, gli altri fra 18 e 20), e l'utente
     * l'ha confermata così: non si corregge.
     */
    val AlignAcross: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_align_across)

    /** Vedi [AlignAcross]: è la stessa icona girata di un quarto. */
    val AlignDown: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_align_down)

    /*
     * Da qui molti glifi ammorbidiscono un glifo che Material ha: divergono apposta, e lo scarto
     * è misurato in testa a ogni file. I glifi che l'ammorbidimento lascia a zero pixel di scarto
     * (`search`, `image`, `public`, `folder`) restano quelli di Material, in [Material]; `settings`
     * pure, perché la differenza è fra due esportazioni dello stesso disegno di Google.
     */

    /**
     * Il cestino pieno: 'Elimina', l'icona rossa del pannello della selezione.
     *
     * Ammorbidisce `Icons.Default.Delete` (0,2% della tela). Non è [Bin], il cassone vuoto di
     * contorno: quello dice il posto, questo l'azione.
     */
    val PickDelete: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_pick_delete)

    /**
     * Il cestino con la freccia che sale: 'Ripristina', e anche 'Ripristina tutto' (istruzione
     * dell'utente: 'tutti' lo dice la parola accanto). Il cassone dice 'tira fuori questo file',
     * che un orologio con la freccia non dice.
     */
    val BinRestore: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_bin_restore)

    /**
     * Il cassone di contorno, vuoto: la voce 'Cestino' del menu della schermata iniziale.
     *
     * Ammorbidisce `Icons.Outlined.Delete` (0,1% della tela). Perché sia di contorno è scritto su
     * [PickDelete].
     */
    val Bin: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_bin)

    /**
     * Il cassone con dentro la freccia che torna indietro: 'Cronologia del cestino'.
     *
     * Disegno dell'utente: dice il giro e il cestino insieme. Material ha il bidone e l'orologio
     * con la freccia, non la loro unione (vedi `ic_bin_history.xml`).
     */
    val BinHistory: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_bin_history)

    /*
     * ── I sei comandi dell'editor ────────────────────────────────────────────
     *
     * Disegnati dall'utente: i glifi di sistema con angoli e punte raccordati, da 0,11% a 2,52%
     * della tela di scarto (i numeri sono in testa a ogni file).
     * ⚠️ **`Undo` e `Redo` si specchiano nelle lingue da destra a sinistra, le rotazioni no**: lo
     * dichiarano i file, perché un vettore non si specchia da sé.
     */

    /** La freccia che torna indietro: 'Annulla'. Vedi `ic_undo.xml`. */
    val EditUndo: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_undo)

    /** La freccia che torna avanti: 'Ripristina'. Vedi `ic_redo.xml`. */
    val EditRedo: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_redo)

    /** Il cerchio aperto con la freccia che rientra: 'Azzera'. Vedi `ic_reset.xml`. */
    val EditReset: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_reset)

    /** Il segno di spunta: 'Applica'. Vedi `ic_apply.xml`. */
    val EditApply: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_apply)

    /** Il cerchio con la freccia oraria: 'Ruota a destra'. Vedi `ic_turn_right.xml`. */
    val TurnRight: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_turn_right)

    /** Il cerchio con la freccia antioraria: 'Ruota a sinistra'. Vedi `ic_turn_left.xml`. */
    val TurnLeft: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_turn_left)

    /**
     * Una pila di immagini: la vista 'Feed' (scelta A5 dell'utente). È un disegno nuovo, non un
     * glifo di Material ammorbidito. Vedi `ic_feed.xml`.
     */
    val Feed: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_feed)

    /** Quattro riquadri cavi: 'Visualizzazione griglia'. Ammorbidisce `Icons.Default.GridView`. */
    val ViewGrid: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_view_grid)

    /**
     * Tre righe con la miniatura a sinistra: 'Visualizzazione lista'.
     *
     * È quello che si scosta di più dal suo glifo di sistema, il 5% della tela contro
     * `Icons.Default.ViewList`: oltre ai raccordi, l'inchiostro è un'unità più stretto.
     */
    val ViewList: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_view_list)

    /**
     * La A di AIV senza la piastrella: il marchio che firma una superficie.
     *
     * ⚠️ **Non è quadrato, il solo del catalogo**: la tela è 70 x 60, quindi chi lo disegna gli
     * dà larghezza e altezza, o lo schiaccia su un asse. I due chiamanti (il FAB della schermata
     * iniziale e la barra delle informazioni) lo fanno già.
     */
    val AivMark: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_aiv_mark)

    /** La cartella col marchio dell'app: l'icona dell'intestazione di una cartella. */
    val FolderAiv: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_folder_aiv)

    /**
     * La cartella AIV dell'icona di una cartella nella schermata Home ('Aggiungi alla schermata
     * Home'): disegno dell'utente, `ic_folder_home.xml`.
     *
     * Si legge fuori da una composizione, perché l'icona si dipinge in una bitmap per il launcher
     * (`Shortcuts.folderIcon`): per questo è una funzione con un contesto.
     */
    fun folderHome(context: Context): ImageVector =
        ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_folder_home)

    /**
     * Il glifo che firma il piede della schermata delle informazioni.
     *
     * Si disegna dentro una riga di testo (`InlineTextContent`): la misura gliela dà il
     * `Placeholder` del chiamante.
     */
    val Tian: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_tian)

    /*
     * I glifi dei moduli dell'editor completo: glifi di Material coi bordi esterni arrotondati di
     * 0,4, su richiesta dell'utente, da 0,10% a 0,66% della tela di scarto (in testa a ogni file).
     * `Palette` e `Timeline` sono tutti curve, l'arrotondamento li lascia identici, quindi i
     * moduli 'Colore' e 'Curve' usano i glifi di [Material]. Il tracciato si ricostruiva dal
     * bytecode di `material-icons` (`Rules.md` § '🖌️ Come entra un disegno').
     */

    /** Le forbici del ritaglio: il primo modulo dell'editor completo. */
    val ModCrop: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_mod_crop)

    /** Il riquadro con le due frecce: il modulo 'Geometria'. */
    val ModGeometry: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_mod_geometry)

    /** Il cerotto: la correzione locale ('Correggi' e 'Rimuovi'). */
    val Heal: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_heal)

    /** La maglia piegata dello strumento 'Fluidifica'. */
    val Liquify: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_liquify)

    /** Il sole coi raggi: il modulo 'Luce'. */
    val ModLight: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_mod_light)

    /**
     * I tre cursori: il modulo 'HSL', che lavora su un colore per volta.
     *
     * Cursori stilizzati e non un contagocce, su istruzione dell'utente; lo scarto dal glifo di
     * Material è in testa al file.
     */
    val ModMix: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_mod_mix)

    /** La semiluna sfumata coi tre punti: il modulo 'Dettaglio'. */
    val ModDetail: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_mod_detail)

    /**
     * La cornice quadrata col tondo scavato: il modulo 'Effetti'.
     *
     * Disegno dell'utente: la cornice è un superellisse. Le misure e lo scarto del trasporto sono
     * in testa al file.
     */
    val ModEffects: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_mod_effects)

    /**
     * La matita con lo scarabocchio: il modulo 'Disegno'. È `Icons.Filled.Draw` con gli spigoli
     * convessi esterni raccordati (le misure sono in testa al file).
     */
    val ModDraw: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_mod_draw)

    /**
     * Le otto penne del modulo 'Disegno': cinque tratti (vedi i file), il testo (il glifo di
     * Material ammorbidito), la pillola e il pannello.
     */
    @Composable
    fun pen(pen: Pen): ImageVector = when (pen) {
        Pen.FREE -> ImageVector.vectorResource(R.drawable.ic_pen_free)
        Pen.LINE -> ImageVector.vectorResource(R.drawable.ic_pen_line)
        Pen.ARROW -> ImageVector.vectorResource(R.drawable.ic_pen_arrow)
        Pen.RECT -> ImageVector.vectorResource(R.drawable.ic_pen_rect)
        Pen.ELLIPSE -> ImageVector.vectorResource(R.drawable.ic_pen_ellipse)
        Pen.TEXT -> ImageVector.vectorResource(R.drawable.ic_pen_text)
        Pen.PILL -> ImageVector.vectorResource(R.drawable.ic_pen_pill)
        Pen.PANEL -> ImageVector.vectorResource(R.drawable.ic_pen_panel)
    }

    /**
     * Le righe a sinistra, al centro o a destra: il tasto 'Allineamento' del testo, coi glifi di
     * Material ammorbiditi.
     */
    @Composable
    fun align(align: Align): ImageVector = when (align) {
        Align.LEFT -> ImageVector.vectorResource(R.drawable.ic_align_left)
        Align.CENTER -> ImageVector.vectorResource(R.drawable.ic_align_center)
        Align.RIGHT -> ImageVector.vectorResource(R.drawable.ic_align_right)
    }

    /** Il mirino: il tasto 'Mirato', che arma il colore mirato. */
    val Aim: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_aim)

    /** La bacchetta con le tre stelle: il tasto 'Auto' dell'editor completo. */
    val Auto: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_auto)

    /**
     * Il segnalibro col più: il comando 'Salva stile' della barra dell'editor completo.
     *
     * È `Icons.Filled.BookmarkAdd` ammorbidito, su istruzione dell'utente; lo scarto e il perché
     * di ogni raccordo sono in testa al file.
     */
    val PresetSave: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_preset_save)

    /**
     * L'immagine nella cornice tratteggiata: il tasto 'Ridimensiona' in testata all'editor.
     *
     * È `Icons.Filled.PhotoSizeSelectLarge` ammorbidito, su richiesta dell'utente; lo scarto e il
     * perché di ogni raccordo sono in testa al file.
     */
    val Resize: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_resize)

    /**
     * La cornice col rettangolino in basso a sinistra: il tasto 'Filigrana' dei due editor.
     *
     * È `Icons.Filled.BrandingWatermark` specchiato in orizzontale, su richiesta dell'utente: il
     * rettangolino è dove di solito lui mette la filigrana. Lo specchio cambia il disegno, quindi
     * è un file come gli altri.
     */
    val Watermark: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_watermark)

    /*
     * ⚠️ `ic_launcher_foreground.xml` non è nel catalogo: è l'icona dell'app, si disegna con
     * `Image` sopra il proprio fondo colorato e non si tinge. Il suo chiamante è `Identity.kt`.
     */

    /**
     * I glifi di Material che l'app usa così come sono: un file `ic_m_<nome>.xml` per glifo, col
     * nome di Material e lo stile (`Outlined` in coda al nome di quelli di contorno).
     *
     * ⚠️⚠️ **Vengono dalla libreria `material-icons-extended` 1.7.8, che dalla 6.30 non è più fra
     * le dipendenze** (scelta H1a dell'utente): era ferma dal febbraio 2025 e pesava 35,7 MB, dei quali
     * l'app usava solo i glifi di questo elenco. Ogni tracciato è ricostruito dalle chiamate a `PathBuilder` della
     * libreria, in ordine, e riscritto in coordinate assolute; reso a 240 px accanto alla libreria,
     * da sinistra a destra e da destra a sinistra, cambiava zero pixel. L'origine di ognuno è in
     * testa al suo file, e le icone che si specchiano nelle lingue da destra a sinistra (le
     * `AutoMirrored` di Material) lo dichiarano con `android:autoMirrored`.
     * ⚠️ **Non si raccordano**: il disegno è quello di Material, identico, e `tools/icon-round.py`
     * li dichiara esclusi (`Rules.md` § '🖌️ Come entra un disegno').
     * La croce di contorno di Material (`Icons.Outlined.Close`) è lo stesso disegno di quella piena,
     * a zero pixel, quindi c'è solo [Close].
     */
    object Material {

        val ArrowBack: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_arrow_back)

        val ArrowDownward: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_arrow_downward)

        val ArrowForward: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_arrow_forward)

        val ArrowUpward: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_arrow_upward)

        val Check: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_check)

        val Close: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_close)

        val ContentPaste: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_content_paste)

        val ControlPointDuplicate: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_control_point_duplicate)

        val CreateNewFolder: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_create_new_folder)

        val CropFree: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_crop_free)

        val CropLandscape: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_crop_landscape)

        val CropPortrait: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_crop_portrait)

        val DeleteForever: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_delete_forever)

        val DragIndicator: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_drag_indicator)

        val Edit: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_edit)

        val FilterListOutlined: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_filter_list_outlined)

        val FitScreenOutlined: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_fit_screen_outlined)

        val Flip: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_flip)

        val FlipToBack: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_flip_to_back)

        val FlipToFront: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_flip_to_front)

        val Folder: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_folder)

        val FormatListBulletedOutlined: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_format_list_bulleted_outlined)

        val FormatPaint: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_format_paint)

        val Image: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_image)

        val ImageOutlined: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_image_outlined)

        val Info: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_info)

        val InfoOutlined: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_info_outlined)

        val InsertDriveFile: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_insert_drive_file)

        val KeyboardArrowRight: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_keyboard_arrow_right)

        val LightModeOutlined: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_light_mode_outlined)

        val Lock: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_lock)

        val MoreHoriz: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_more_horiz)

        val MovieOutlined: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_movie_outlined)

        val NavigateBeforeOutlined: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_navigate_before_outlined)

        val NavigateNextOutlined: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_navigate_next_outlined)

        val Palette: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_palette)

        val Pause: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_pause)

        val PhotoSizeSelectActualOutlined: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_photo_size_select_actual_outlined)

        val PlayArrow: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_play_arrow)

        val Public: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_public)

        val PushPin: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_push_pin)

        val SdStorage: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_sd_storage)

        val Search: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_search)

        val Settings: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_settings)

        val Share: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_share)

        val Style: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_style)

        val Timeline: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_timeline)

        val Transform: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_transform)

        val VolumeUpOutlined: ImageVector
            @Composable get() = ImageVector.vectorResource(R.drawable.ic_m_volume_up_outlined)
    }

    /** La griglia di Material: ogni icona del sistema è disegnata dentro un 24x24. */
    private const val GRID = 24f

    /** Lo spessore del solo [TextCursor], in unità di griglia. */
    private const val STROKE = 2f

    /**
     * Quanto rientra il centro di una lineetta, in unità di griglia.
     *
     * Scelto dall'utente fra quattro provini: sotto 0,5 la curva sparisce nell'antialiasing,
     * sopra 1 il glifo diventa una clessidra.
     */
    private const val NOTCH = 0.8f

    /**
     * Dov'è il punto di controllo della curva, che non è dove passa la curva.
     *
     * ⚠️ Una quadratica passa a metà fra la corda e il suo punto di controllo: per una rientranza
     * vera di [NOTCH] il controllo va al doppio, o la rientranza sarebbe la metà.
     */
    private const val PULL = NOTCH * 2f

    private val SIZE = 24.dp
}

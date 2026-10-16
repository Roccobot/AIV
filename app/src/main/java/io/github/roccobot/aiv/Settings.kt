package io.github.roccobot.aiv

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/*
 * Le impostazioni, e quali esistono.
 *
 * Restano fuori di proposito:
 * - la **lingua**: la sceglie Android dal sistema, e un selettore dentro l'app sarebbe un secondo
 *   meccanismo che fa quello che la piattaforma fa già;
 * - la **rotella** del mouse (verso, passo, sensibilità): su un telefono non c'è, e il pizzico lo
 *   gestisce il sistema;
 * - i DPI dell'**esportazione SVG**: il visualizzatore legge gli SVG (vedi [Svg]) ma niente qui ne
 *   scrive. Se un giorno servisse uno zoom più nitido su un vettoriale, la misura raster di un SVG
 *   in arrivo, oggi la costante [Svg.BOX], verrebbe qui;
 * - la correzione del **testo** da tavolo, misurata su un carattere che questa app non carica.
 *
 * Resta quello che un dito raggiunge e un telefono può cambiare.
 */

/** Un'immagine che l'app ha già aperto, come la elenca la schermata iniziale. */
data class RecentImage(val address: String, val name: String)

/**
 * Quello che ogni scelta salvata ha: un token che sopravvive a una rinomina.
 *
 * ⚠️ **Si salva il token e NON il nome della costante**: oggi sono uguali, e il giorno che una
 * costante si rinomina ogni telefono che aveva salvato un valore ripiegherebbe in silenzio su
 * quello di fabbrica.
 */
interface Choice {
    val token: String
}

/** Come si dipinge lo spazio dietro l'immagine. */
enum class BgType(override val token: String) : Choice {
    /** La scacchiera, che fa vedere la trasparenza. */
    CHECKER("checker"),
    /** Un colore unito, per guardare un'immagine senza un motivo sotto. */
    SOLID("solid")
}

/**
 * Quale coppia di grigi usa lo sfondo. Separata da [BgType] apposta: sono due assi indipendenti, e
 * unirli costringerebbe a scegliere fra trasparenza e colore, perdendo la scacchiera chiara.
 */
enum class BgTheme(override val token: String) : Choice { AUTO("auto"), LIGHT("light"), DARK("dark") }

/**
 * Che cosa vuol dire '100%', e su un telefono le due risposte sono lontane.
 *
 * Uno schermo di telefono ha da due a tre pixel del dispositivo per pixel di layout, quindi
 * un'immagine a un pixel per pixel del DISPOSITIVO è un terzo di quella a un pixel per pixel di
 * LAYOUT. Nessuna delle due è sbagliata: [PHYSICAL] mostra il file com'è, [LOGICAL] alla misura di
 * una pagina web.
 */
enum class ScaleMode(override val token: String) : Choice { PHYSICAL("physical"), LOGICAL("logical") }

/**
 * Che cosa stacca dallo schermo una superficie che si apre sopra la schermata.
 *
 * ⚠️ **Tre risposte, mai due insieme** (istruzione dell'utente: sfocatura o ombra, mai insieme).
 * Non è solo gusto: la sfocatura è un attributo della finestra e vuole una finestra grande quanto
 * il pannello, o sfoca una fascia intorno; l'ombra esce dal pannello e vuole una finestra più
 * grande, o è tagliata sul bordo. Con una scelta sola in vigore, ognuna ha la finestra che le
 * serve. [NONE] serve a chi ha un telefono che non regge nessuno dei due effetti. Il bordo
 * d'accento non è in questo elenco: c'è in tutti e tre i casi (vedi `Edge.kt`).
 */
enum class PanelDepth(override val token: String) : Choice {
    /** Lo sfondo sfocato più la patina scura. */
    BLUR("blur"),
    /** L'ombra intorno alla superficie, e nient'altro dietro: quello che fa Material. */
    SHADOW("shadow"),
    /** Niente: le finestre restano come Android le dà. */
    NONE("none")
}

/**
 * Come si riconosce l'**ultimo media visualizzato** in una griglia: cornice, angolo o tondo
 * (richieste dell'utente). L'ordine di dichiarazione è quello dei gettoni.
 *
 * ⚠️ **La cornice era stata scartata come proposta iniziale** (in `GridScreen`: intorno a una
 * miniatura è il segno della selezione), e l'utente l'ha poi voluta fra le scelte: i segni dicono
 * cose diverse, e la scelta è sua.
 * ⚠️ **Di fabbrica il tondo** (voce `tondo-opaco`), ma chi ha già l'app tiene il segno che ha:
 * l'archivio non distingue 'ho scelto la cornice' da 'la cornice è arrivata di fabbrica'. Il come è
 * su `MarkMigration`. La voce si chiama 'Tondo', e il token resta `dot`.
 */
enum class LastMark(override val token: String) : Choice {
    /** Una cornice intorno alla miniatura, del colore d'accento. */
    FRAME("frame"),
    /** Il nastro triangolare nell'angolo in basso a sinistra. */
    CORNER("corner"),
    /** Un tondo d'accento nello stesso angolo del nastro. */
    DOT("dot")
}

/**
 * Da che parte sono le funzioni usate più spesso, nel pannello della selezione.
 *
 * ⚠️ **È un'impostazione di pollice** (richiesta dell'utente): chi tiene il telefono con la destra
 * arriva comodo solo all'ultimo terzo della fila, e rovesciando le file le stesse funzioni
 * finiscono sotto lo stesso dito dell'altra mano. Si rovesciano le file, non l'ordine dell'elenco:
 * la prima fila resta la prima. L'ordine di dichiarazione è quello dei due tasti, sinistra prima
 * (richiesta dell'utente): la scelta parla di lati. Il valore di fabbrica, `RIGHT`, è scritto in
 * `Settings.hand` e nel ripiego della rilettura; sul disco va il token (vedi `byToken`), non la
 * posizione.
 */
enum class Hand(override val token: String) : Choice { LEFT("left"), RIGHT("right") }

/**
 * L'elemento interattivo principale del telefono in verticale: la voce in cima a 'Pulsanti e
 * indicatori' (nota E dell'utente sul giro della `4.04`). La sua chiave è `main-control`; quella
 * dell'interruttore di prima si traduce (`PREF_RETIRED`, in `Backup.kt`). Solo sul telefono in
 * verticale (risposta G4): sul tablet la voce non compare, e vale [FAB] (`pillMode`, in
 * `PhonePill.kt`).
 */
enum class MainControl(override val token: String) : Choice {
    /** Il FAB col suo menu: 'Tasto fluttuante'. */
    FAB("fab"),
    /**
     * La pillola di icone, a scomparsa o estesa secondo [Settings.phonePill]: 'Pillola di icone'.
     * Valore di fabbrica in AIV Play ([Store.mainControl]).
     */
    PILL("pill"),
    /**
     * Il Menu basso, a scomparsa o fisso secondo lo stesso [Settings.phonePill] della pillola: la
     * domanda è una sola, cioè se il comando resta aperto. Il disegno è in `BottomMenu.kt`.
     */
    BOTTOM("bottom"),
    /**
     * Il Menu 'Start', sempre a scomparsa, quindi [Settings.phonePill] non lo riguarda: a riposo è
     * la pillola verticale del Menu basso, aperto un pannello 3x3 nella schermata iniziale e 2x2
     * nelle cartelle (decisioni G1 e C1-C3). Il disegno è in `BottomMenu.kt`. Valore di fabbrica in
     * AIV GitHub ([Store.mainControl]).
     */
    CORNER("corner")
}

/**
 * Quale pillola, quando l'elemento principale è la pillola di icone (richiesta dell'utente): la
 * pillola d'accento degli schermi larghi, coricata in basso e ancorata al lato preferito ([Hand]).
 * Solo sul telefono: la regola è in `pillMode`, in `PhonePill.kt`.
 */
enum class PhonePill(override val token: String) : Choice {
    /**
     * Il FAB di sempre: il valore in vigore sul tablet (`pillMode`). Non è più una scelta (risposta
     * dell'utente a `4.00-01`): [Settings.phonePill] non vale mai questo, e il gettone resta solo
     * nei file scritti prima, che [SettingsStore.read] legge come interruttore spento.
     */
    OFF("off"),
    /**
     * Un FAB tondo color accento che al tocco si allunga nella pillola e si richiude sulla sua ×.
     * Il salto e il tocco lungo del FAB restano sul tasto tondo (risposta C1).
     */
    SLIDE("slide"),
    /** La pillola sempre aperta: niente FAB, niente ×, niente menu (risposta A1). */
    EXTENDED("extended")
}

/**
 * Di che cosa è fatta la pillola. [GLASS] vuole Android 12 (`RenderEffect`): sotto, il gettone non
 * compare (risposta dell'utente), e un valore letto da un backup ripiega su [SOLID] (`pillFillIn`,
 * in `PhonePill.kt`).
 */
enum class PillFill(override val token: String) : Choice {
    /** L'accento pieno. */
    SOLID("solid"),
    /** L'accento all'80%. */
    TRANSLUCENT("translucent"),
    /**
     * Il vetro nell'accento (gettone 'Traslucido'). Valore di fabbrica (nota D del giro della
     * `4.20`).
     */
    GLASS("glass")
}

/**
 * Dopo quanto un file eliminato se ne va da solo dal cestino.
 *
 * ⚠️ **Durate con un nome e non un numero di giorni**: '7 giorni' vuole un plurale, e le categorie
 * di plurale non coincidono fra le ventotto lingue; 'Una settimana' è una frase fissa. [NEVER] è il
 * valore di fabbrica: lo svuotamento automatico cancella per sempre, ed è spento finché non lo si
 * accende.
 */
enum class BinKeep(override val token: String, val days: Int) : Choice {
    NEVER("never", 0),
    WEEK("week", 7),
    MONTH("month", 30),
    QUARTER("quarter", 90)
}

/**
 * Quanto è grande il testo nella modalità lista. Vale per la sola lista: là ogni riga è nome più
 * conto, e chi sceglie la lista lo fa per leggere. Si moltiplica sopra il corpo di sistema, che
 * decide il resto.
 */
enum class TextSize(override val token: String) : Choice {
    SMALL("small"), NORMAL("normal"), LARGE("large")
}

/**
 * Come si chiama una delle tre misure. È accanto all'enum perché la mostrano due superfici, la
 * scorciatoia della finestra e la pagina delle impostazioni.
 */
@StringRes
fun TextSize.label(): Int = when (this) {
    TextSize.SMALL -> R.string.text_small
    TextSize.NORMAL -> R.string.text_normal
    TextSize.LARGE -> R.string.text_large
}

/** Dove è la riga dei dettagli: in alto o in basso (richiesta dell'utente). */
enum class InfoPosition(override val token: String) : Choice { TOP("top"), BOTTOM("bottom") }

/**
 * Come si guarda quello che c'è sul telefono: le viste della schermata iniziale.
 *
 * ⚠️ **Griglia e lista sono due domande diverse**: le copertine si riconoscono a colpo d'occhio ma
 * ne entrano quattro per schermata; la lista ne mostra una decina coi nomi interi, l'unico modo di
 * trovare per nome una cartella fra trenta. La copertina è in tutte e due, in piccolo nella riga
 * della lista.
 * ⚠️ **[TREE] è di un'altra natura**: [GRID] e [LIST] sono due rese delle cartelle del MediaStore,
 * [TREE] è la memoria del telefono navigata cartella per cartella, letta dal disco (vedi `Tree`).
 */
enum class FolderView(override val token: String) : Choice {
    GRID("grid"), LIST("list"), TREE("tree"),

    /**
     * Tutte le immagini e i video in un flusso solo, per mese ([Feed]), col nome che l'utente gli
     * ha dato.
     *
     * ⚠️ **Non è una resa dell'elenco delle cartelle come [GRID] e [LIST]**: mostra le immagini
     * delle cartelle che quelle due mostrano, senza le cartelle. Nel Menu 'Start' prende il posto
     * del commutatore fra il tasto tondo e la pillola (scelta dell'utente).
     */
    FEED("feed")
}

/**
 * Quale data mette in ordine il Feed: scelta dell'utente, con [TAKEN] di fabbrica. [TAKEN] ripiega
 * sulla data di modifica dove un file non ha la data di scatto (vedi [Feed.moment]); i due gettoni
 * lo dicono (`Scatto, Modifica` e `Modifica`).
 */
enum class FeedDate(override val token: String) : Choice { TAKEN("taken"), MODIFIED("modified") }

/**
 * Dove si vede il colore di una cartella fuori dalla sua intestazione (risposte dell'utente a
 * `d-colore-come`): nella schermata iniziale, dove serve a riconoscere una cartella.
 *
 * Quattro stili e non cinque: l'angolo piegato del mockup è stato visto e scartato. [NAME] è il
 * valore di fabbrica, scelto dall'utente dopo averli provati. Nella vista 'Cartelle di sistema' non
 * c'è niente da tingere: là le cartelle sono percorsi letti dal disco, e una tinta è appesa al
 * `BUCKET_ID` del MediaStore (vedi [FolderTints]). L'ordine di dichiarazione è l'ordine dei
 * gettoni, scelto dall'utente (`t-colore-stili`); nell'archivio vive il token, quindi riordinare
 * non chiede una migrazione.
 */
enum class FolderColour(override val token: String) : Choice {
    /** Niente: la tinta resta nella sola intestazione. */
    NONE("none"),
    /** Il nome della cartella scritto nel suo colore. Il valore di fabbrica. */
    NAME("name"),
    /**
     * Un filetto sotto la copertina, dentro il suo riquadro: appeso fuori sposterebbe il nome nelle
     * sole cartelle con un colore, e le righe della griglia non sarebbero allineate.
     */
    EDGE("edge"),
    /** Una cornice intorno alla copertina, disegnata all'interno del suo bordo. */
    FRAME("frame"),
    /** Un alone che scende dal bordo di sopra della copertina e si spegne prima della metà. */
    GLOW("glow")
}

/**
 * Il tema dell'interfaccia (richiesta dell'utente).
 *
 * ⚠️ **Non è [BgType] né [BgTheme]**: quelli dicono di che grigi è fatto lo sfondo dietro
 * l'immagine, questo se l'app è chiara o scura. Chi guarda al buio può volere l'app scura e la
 * scacchiera chiara. Di fabbrica [SYSTEM].
 */
enum class UiTheme(override val token: String) : Choice {
    SYSTEM("system"), LIGHT("light"), DARK("dark")
}

/**
 * Quante colonne mostra la griglia delle CARTELLE, non delle immagini, che resta adattiva sui suoi
 * 108 dp. Il numero lo sceglie l'utente, quindi non dipende dalla larghezza dello schermo; il costo
 * dichiarato: a 4 colonne su un telefono stretto la copertina scende sotto i 90 dp.
 */
val FOLDER_COLUMNS = listOf(2, 3, 4)

data class Settings(
    val bgType: BgType = BgType.CHECKER,
    val bgTheme: BgTheme = BgTheme.AUTO,
    /**
     * Se un'immagine più piccola della vista cresce fino a riempirla. Spento, come nello
     * userscript: ingrandire un'icona da 64 px non serve a nessuno.
     */
    val fitGrow: Boolean = false,
    val zoomMax: Float = 40f,
    val scaleMode: ScaleMode = ScaleMode.PHYSICAL,
    /**
     * Dove è la barra delle info. In alto di serie (scelta dell'utente): là non finisce sotto il
     * pollice di chi sfoglia, e non si scontra con la fila dei comandi delle immagini animate, in
     * basso al centro.
     */
    val infoPosition: InfoPosition = InfoPosition.TOP,
    val infoVisible: Boolean = true,
    /**
     * Che cosa c'è dietro finestre, menu e pannelli: la sfocatura, l'ombra o niente (vedi
     * [PanelDepth]). Di fabbrica la sfocatura (istruzione dell'utente).
     *
     * ⚠️ **Chi ha già l'app non se ne accorge**: [SettingsStore.save] riscrive tutte le chiavi in
     * un colpo, quindi chi ha toccato una qualunque impostazione ha già questa scritta, e un valore
     * di fabbrica vale solo dove l'archivio tace. Una migrazione come quella dell'indicatore
     * ([MarkMigration]) non si può fare: qui il valore di prima è scritto, e l'archivio non
     * distingue una scelta da un valore ereditato.
     * ⚠️ **Il valore di fabbrica è in due posti**: qui e in [SettingsStore.read], e il banco li
     * confronta leggendo un archivio vuoto. La sfocatura non costa fotogrammi dove il telefono non
     * la regge: è un attributo di finestra (`FLAG_BLUR_BEHIND` più `blurBehindRadius`), che da
     * Android 12 esegue il compositore di sistema, e il sistema decide se farla (risparmio
     * energetico, hardware). L'app lo chiede in `blurs`, in `Veil.kt`.
     */
    val panelDepth: PanelDepth = PanelDepth.BLUR,
    /**
     * Se il menu a pressione lunga contiene anche 'Adatta alla vista' e '100%'.
     *
     * ⚠️ **Spento di fabbrica, ed è una rimozione** (richiesta dell'utente): quelle due voci fanno
     * quello che fa già il doppio tocco, e sono in mezzo a comandi che agiscono sul file.
     * L'interruttore esiste perché un gesto non si dichiara da sé; la contropartita è l'onboarding
     * del doppio tocco (`Hint.ZOOM_TAP`).
     */
    val zoomInMenu: Boolean = false,
    /**
     * Se la sequenza si sfoglia in ordine cronologico e non dalla più recente (il verso della
     * galleria, che è il predefinito: decisione dell'utente).
     *
     * ⚠️ **La chiave è `sequence-reversed` perché il significato si è rovesciato**: con la chiave
     * di prima (`reverse-order`, che resta scritta negli archivi e non si legge più) chi l'aveva
     * accesa avrebbe avuto l'opposto, in silenzio. Una domanda nuova vuole una chiave nuova.
     */
    val reverseSequence: Boolean = false,
    /**
     * Non compare nella schermata delle impostazioni: si cambia dal menu della schermata delle
     * cartelle, dove si vede l'effetto.
     */
    val folderView: FolderView = FolderView.GRID,
    /**
     * Le opzioni della modalità lista. Sono nelle impostazioni anche se si cambiano da un popup,
     * come le colonne: il popup è una scorciatoia a una scelta che deve sopravvivere alla chiusura
     * dell'app. Il filtro dei generi, volatile per richiesta, non è qui.
     */
    val listCount: Boolean = true,
    val listText: TextSize = TextSize.NORMAL,
    /** Se la vista ad albero mostra anche i file che cominciano per punto. */
    val treeHidden: Boolean = false,
    /** Se la vista ad albero nasconde le cartelle che non conducono a nessuna immagine. */
    val treePictures: Boolean = false,
    /** Da che parte sono le funzioni principali del pannello. Vedi [Hand]. */
    val hand: Hand = Hand.RIGHT,
    /**
     * L'elemento interattivo principale del telefono in verticale. Vedi [MainControl].
     *
     * ⚠️ **Il valore di fabbrica lo dà [Store.mainControl]**, perché dipende dalla variante (il
     * Menu 'Start' in AIV GitHub, scelta dell'utente; la pillola a scomparsa in AIV Play); qui e in
     * [SettingsStore.read] si legge da lì, e il banco confronta un archivio vuoto con questo campo.
     * Chi ha già l'app tiene la sua scelta. Un archivio senza `main-control` lo ricava da
     * `phone-pill-on`, e uno ancora più vecchio dal gettone di [phonePill], così chi aveva acceso
     * la pillola la ritrova.
     */
    val mainControl: MainControl = Store.mainControl,
    /**
     * Quale pillola, quando [mainControl] è [MainControl.PILL]. Vedi [PhonePill]. Mai
     * [PhonePill.OFF]; a scomparsa di fabbrica, che tiene il FAB al suo posto finché non lo si
     * tocca.
     */
    val phonePill: PhonePill = PhonePill.SLIDE,
    /**
     * Di che cosa è fatta la pillola, dovunque ci sia. Vedi [PillFill].
     *
     * Riempie anche la pillola degli schermi larghi: la voce compare solo sul telefono, che in
     * orizzontale mostra quella pillola. Traslucido di fabbrica (nota D dell'utente sul giro della
     * `4.20`); sotto Android 12 `pillFillIn` ripiega sul solido.
     */
    val pillFill: PillFill = PillFill.GLASS,
    /**
     * Come si regola il traslucido, coi cursori e i due colori che compaiono con lui (vedi
     * [GlassTune]; nota N1 dell'utente). Si nascondono spegnendo [GLASS_TUNING]. I valori di
     * fabbrica sono quelli dell'utente (nota D del giro della `4.20`) e valgono dove l'archivio
     * tace; 'Ripristina' sotto i cursori li riporta.
     */
    val glass: GlassTune = GlassTune(),
    /**
     * Come riposa il Menu 'Start': un tasto tondo solo (di fabbrica), o la pillola verticale di due
     * tasti (`4.20-01`). Si sceglie solo dal commutatore nella prima riga del menu nella schermata
     * iniziale: nel pannello delle impostazioni non c'è una voce, perché si sceglie guardando il
     * menu.
     */
    val cornerRound: Boolean = true,
    /**
     * Se 'Copia lista' mette anche il percorso della cartella in testa ai nomi. Spenta di fabbrica,
     * come chiesto.
     */
    val listPath: Boolean = false,
    /**
     * Se in testa alla selezione si legge anche il peso di quello che si è scelto.
     *
     * ⚠️ **L'interruttore esiste perché il peso costa** (riscontro dell'utente): ogni selezione
     * ferma è una domanda al MediaStore su tutti gli elementi scelti. Governa il solo peso e non il
     * conto, che è il titolo della selezione. Accesa di fabbrica, per non togliere la funzione a
     * chi non sa che esiste.
     */
    val pickWeight: Boolean = true,
    /**
     * Chi apre un'immagine quando si tocca 'Modifica'.
     *
     * ⚠️ **Vuoto vuol dire 'mai scelto', diverso da 'nessuno'**: la prima volta il menu chiede, poi
     * non più. Il valore è un componente appiattito (`pacchetto/classe`), o `Editors.INTERNAL` per
     * l'editor interno (anche `Editors.FULL`, la forma scritta da versioni vecchie). Vedi
     * `Editors`.
     */
    val editorApp: String = "",

    /**
     * Con quanta cura l'editor completo riscrive il file.
     *
     * ⚠️ **'Alta' di fabbrica, per come funziona un JPEG**: a 95 la differenza dal massimo non si
     * vede, e a 100 il file quasi raddoppia, perché la quantizzazione smette di lavorare. 'Senza
     * perdita' scriverebbe un PNG a ogni salvataggio, file parecchie volte più grossi. Si salva il
     * token (vedi `Quality`): l'ordine dei gettoni può cambiare senza che nessuno perda la sua
     * scelta.
     */
    val editorQuality: Quality = Quality.DEFAULT,

    /**
     * Se prima di sovrascrivere un'immagine se ne mette una copia nel cestino.
     *
     * ⚠️ **Accesa di fabbrica, perché il danno è asimmetrico** (richiesta dell'utente): spenta, un
     * ritaglio sbagliato si porta via l'originale; accesa, il prezzo è un file in più nel cestino.
     * Non dipende da [binOn]: il cestino si raggiunge dal FAB anche quando l'eliminazione non ci
     * passa.
     */
    val editorBackup: Boolean = true,

    /**
     * Se la filigrana scelta si scrive sull'immagine quando l'editor salva.
     *
     * ⚠️ **Spenta di fabbrica** (risposta `altro` a `d-filigrana-file`): accesa senza averlo
     * chiesto scriverebbe un logo su un'immagine. Senza un file scelto non fa niente, e la pagina
     * lo dice.
     * ⚠️ **Toglie il senza perdita**: girare un JPEG si fa con un tag EXIF, ma una filigrana scrive
     * i pixel. La regola è su `Look.lossless`.
     */
    val markOn: Boolean = false,

    /**
     * Dove cade la filigrana sull'immagine: in basso a destra di fabbrica, l'angolo di una firma e
     * il più lontano da dove cade di solito un soggetto.
     */
    val markSpot: Watermark.Spot = Watermark.Spot.BOTTOM_RIGHT,

    /**
     * Quanto è larga la filigrana, in centesimi del lato lungo (il perché è su [Watermark.SIZE]).
     * Quattordici di fabbrica.
     */
    val markSize: Int = Watermark.SIZE_DEFAULT,

    /**
     * Quanto la filigrana è lontana dal bordo, in decimi di centesimo del lato lungo (il perché è
     * su [Watermark.AIR], e la lettura delle chiavi su `MARK_AIR_FINE`). Tre centesimi di fabbrica.
     */
    val markAir: Int = Watermark.AIR_DEFAULT,

    /**
     * Quanto la filigrana è opaca, in centesimi: piena di fabbrica, e chi la vuole discreta lo dice
     * qui.
     */
    val markAlpha: Int = Watermark.ALPHA_DEFAULT,

    /**
     * Se il salvataggio dell'editor completo ridimensiona l'immagine.
     *
     * ⚠️ **Questi tre campi non compaiono nella schermata delle impostazioni**: i loro comandi sono
     * nell'editor (risposta `editor` a `d-resize-dove`, clausola 4 di `Rules.md` § '⚙️ Dove va
     * un'impostazione, e chi la deve trovare'), ma vivono nelle preferenze per il lavoro in serie:
     * chi rimpicciolisce lo fa su parecchie immagini di fila. Spento di fabbrica, perché butterebbe
     * via pixel; se il piano non rimpicciolisce quell'immagine non c'è niente da fare
     * ([Resize.Plan.sizeFor]).
     */
    val sizeOn: Boolean = false,

    /**
     * Che cosa governa il valore del ridimensionamento. Il libero di fabbrica: la finestra si apre
     * senza gettoni accesi, e i due campi contengono le misure dell'immagine (vedi [Resize.NONE]).
     */
    val sizeMode: Resize.Mode = Resize.NONE.mode,

    /**
     * Quanto vale il ridimensionamento, in pixel o in per cento secondo [sizeMode]. Uno solo per
     * tutti i modi: con un valore per modo, cambiando gettone si troverebbe un numero mai scritto;
     * così la finestra lo ricava dai due campi, che non si muovono.
     */
    val sizeValue: Int = Resize.NONE.value,
    /**
     * Se la fila dei comandi di un'immagine animata mostra il contatore dei fotogrammi. Acceso di
     * fabbrica, con l'interruttore chiesto dall'utente: il contatore è la sola parte della fila che
     * non è un comando, quindi la sola che si può voler togliere.
     */
    val animCounter: Boolean = true,
    /**
     * Se all'avvio si guarda negli appunti. Spenta di fabbrica, per scelta dell'utente: da Android
     * 12 il sistema annuncia ogni lettura degli appunti, quindi un avviso a ogni avvio anche con
     * gli appunti inutili.
     */
    val clipboardStart: Boolean = false,
    /**
     * L'ultimo indirizzo degli appunti che l'app ha aperto da sé. Vuoto vuol dire 'nessuno ancora'.
     *
     * ⚠️ **Non è una preferenza ma uno stato, e vive qui apposta**: la decisione si prende in
     * [ViewerViewModel.readClipboard], che deve rispondere subito per non perdere la corsa con la
     * cartella d'avvio (nota là). Un archivio a parte si leggerebbe con una sua coroutine, troppo
     * tardi; le impostazioni quella funzione le aspetta già.
     */
    val clipboardDone: String = "",
    /**
     * Quando gli appunti contenevano l'indirizzo che l'app ha aperto da sé: ricopiare lo stesso
     * indirizzo è una richiesta chiara, e col solo confronto delle stringhe non si distinguerebbe
     * da un link negli appunti da giorni (segnalazione dell'utente). Il tempo è quello del sistema,
     * `ClipDescription.getTimestamp`, cioè quando il contenuto è entrato negli appunti, non quando
     * lo abbiamo letto. Zero vuol dire 'nessuno ancora'.
     */
    val clipboardWhen: Long = 0L,
    /**
     * La cartella da aprire all'avvio, `null` se non se n'è scelta nessuna. Due campi: l'id apre,
     * il nome si legge nelle impostazioni senza interrogare il MediaStore.
     */
    val startFolder: Long? = null,
    val startFolderName: String = "",
    /**
     * Se all'avvio si apre l'immagine più recente di [startFolder] e non la schermata iniziale.
     * Separato dalla cartella: 'quale cartella' resta scritta anche spegnendo l'avvio automatico.
     * Chi lo accende senza aver scelto è portato all'elenco.
     */
    val openAtStart: Boolean = false,
    /** Chiaro, scuro o come il sistema. Vedi [UiTheme]: non è il tema dello sfondo. */
    val uiTheme: UiTheme = UiTheme.SYSTEM,
    /** Quante colonne ha la griglia delle cartelle. Vedi [FOLDER_COLUMNS]. */
    val folderColumns: Int = 2,
    /**
     * Se sotto la copertina di una cartella si vede il conto delle immagini. Spegnendola la riga
     * della griglia si accorcia e l'intestazione cresce di altrettanto, così il conto delle righe
     * visibili (`coverHeader`) resta esatto (punto 1a dell'utente). Accesa di fabbrica.
     */
    val folderCount: Boolean = true,
    /**
     * Dove si vede il colore di una cartella fuori dalla sua intestazione. Vedi [FolderColour].
     * Vale per le due modalità griglia e lista della schermata iniziale: la domanda, come riconosco
     * una cartella, non cambia cambiando vista.
     */
    val folderColour: FolderColour = FolderColour.NAME,
    /**
     * Se l'eliminazione manda le immagini nel cestino invece di cancellarle.
     *
     * ⚠️ **Decide anche se c'è una conferma** (testo dettato dall'utente): col cestino acceso
     * eliminare non chiede niente, perché si disfa; spento, l'eliminazione è definitiva e prima
     * compare una conferma. Si chiede conferma quando e solo quando non si può tornare indietro.
     * Accesa di fabbrica. Spegnerla non svuota il cestino e non lo rende irraggiungibile.
     */
    val binOn: Boolean = true,
    /**
     * Se sfogliando nel visualizzatore si saltano i video.
     *
     * ⚠️ **Spenta di fabbrica** (parole dell'utente): il caso normale è sfogliare anche i video.
     * Tocca il solo gesto dello sfoglio: da una griglia un video si apre comunque, perché toccarlo
     * è una richiesta. Il contatore mostra dei salti (da `3/10` a `5/10`), perché la serie resta
     * quella della cartella.
     */
    val imagesOnly: Boolean = false,
    /**
     * Se un video raggiunto con un tocco parte da sé: il rovescio di [imagesOnly], che spegne il
     * gesto, mentre questa accende il tocco (richiesta dell'utente). Sfogliando non parte niente
     * nemmeno con questa accesa: la regola è in un punto solo, `Arrival.plays`. Spenta di fabbrica:
     * un audio che comincia da sé mette in pausa la musica di un'altra app.
     */
    val clipAutoplay: Boolean = false,
    /**
     * Se sotto ogni miniatura della griglia si legge il nome del file. Spenta di fabbrica
     * (richiesta dell'utente). Accenderla costa una domanda al MediaStore per miniatura, una volta
     * per indirizzo: il nome di un `content://` non si ricava dall'indirizzo. Vedi [Names].
     */
    val gridNames: Boolean = false,
    /**
     * Se l'intestazione di una cartella ha il gradiente (variante 10 del mockup, `d-frontespizio`):
     * la tinta parte piena sotto la barra di sistema e si spegne prima della griglia, e con lei
     * l'icona della cartella passa in negativo. Acceso di fabbrica: senza il gradiente la variante
     * 10 non è la 10.
     */
    val frontWash: Boolean = true,
    /**
     * Se il nome della cartella nell'intestazione è grande e col carattere graziato (variante 7 del
     * mockup, che l'utente voleva poter provare). Spento di fabbrica.
     */
    val frontSerif: Boolean = false,
    /**
     * Se l'intestazione ha le pastiglie del peso e del numero di video (variante 4): due dati che
     * non si sanno senza aprire i file. Il conto si fa una volta per cartella (`Folder.weigh`).
     */
    val frontFacts: Boolean = true,
    /**
     * Se l'intestazione ha la pastiglia 'Seleziona tutto': un comando in mezzo a due dati, e per
     * questo con un aspetto diverso (variante 10).
     */
    val frontPickAll: Boolean = true,
    /**
     * Se la finestra di rinomina ha il tasto che cambia l'estensione: il gettone 'Rinomina' della
     * voce 'Consenti la modifica dell'estensione' (l'altro è [extDownload]).
     *
     * ⚠️ **Spento di fabbrica, una protezione chiesta dall'utente**: cambiare l'estensione non
     * converte niente, e con un'estensione non multimediale il file sparisce dalle modalità griglia
     * e lista, come cancellato. La funzione resta per il caso dell'utente (rinominare un `.svg` in
     * `.txt` per allegarlo in un'app che rifiuta gli SVG). I presidi sono tre: questo interruttore,
     * il paragrafo sotto di lui nelle impostazioni, e il velo della prima volta ([Hint.EXT_WARN]).
     * ⚠️ **La chiave resta `ext-edit`**: la domanda di questo campo è la stessa di prima, quindi
     * chi l'aveva accesa la ritrova accesa.
     */
    val extRename: Boolean = false,
    /**
     * Se la finestra del salvataggio ha il tasto che cambia l'estensione: la gemella di
     * [extRename], un gettone per funzione (richiesta dell'utente). Spenta di fabbrica come la
     * gemella. Il tocco lungo su 'Scarica' la scavalca, come scavalca [saveRename] ('per questa
     * volta sola'), ma non scavalca l'avviso della prima volta.
     */
    val extDownload: Boolean = false,
    /**
     * Se le miniature sono nella memoria grafica invece che in quella dell'app (richiesta e
     * autorizzazione dell'utente).
     *
     * Il guadagno è il budget: `ImageSource.pixelBudget` misura la memoria che resta per decidere
     * se campionare l'immagine grande, e ogni miniatura in memoria grafica non pesa più su quella
     * dell'app.
     * ⚠️ **Spenta di fabbrica, fra le funzionalità avanzate, per una ragione misurabile**: un
     * bitmap in memoria grafica costa un descrittore di file, e il tetto di Android è dell'ordine
     * del migliaio. Una cache che cresce li esaurisce, e l'app va in errore. Spenta vuol dire
     * software dappertutto, anche nella decodifica di ripiego di Coil. Non tocca il visualizzatore,
     * che deve poter rileggere i pixel (esportazione, editor, appunti).
     */
    val gpuThumbs: Boolean = false,
    /**
     * Se AIV GitHub cerca da sé una versione nuova all'apertura, una volta al giorno.
     *
     * ⚠️ **Acceso di fabbrica, scelta D1 dell'utente**: chi scarica da GitHub non ha un negozio che
     * lo avvisi. Il controllo non scarica niente: propone. In AIV Play la voce non c'è
     * (`Store.updates`), e il campo resta senza effetto; il perché è in `Updater`.
     */
    val autoUpdate: Boolean = true,
    /**
     * Se salvare un'immagine chiede prima il nome.
     *
     * ⚠️ **Spenta di fabbrica, specifica dell'utente**: salvare è un gesto di fretta. Chi la vuole
     * una volta sola tiene premuto 'Scarica'. Non decide dove si salva: lo decide [downloadPath],
     * nella stessa finestra. È in 'Modifica e backup' perché risponde alla domanda su che cosa
     * l'app scrive su disco, e con che nome.
     */
    val saveRename: Boolean = false,
    /**
     * Se salvare offre anche di scegliere dove (richiesta dell'utente): accesa, nella finestra del
     * nome compare il comando che apre il selettore di sistema; spenta, tutto va in Download, che
     * funziona senza autorizzazioni, anche in vista di Play. Spenta di fabbrica, come ha scritto
     * l'utente. È accanto a [saveRename] perché è della stessa famiglia (`Rules.md` § '⚙️ Dove va
     * un'impostazione, e chi la deve trovare').
     */
    val downloadPath: Boolean = false,
    /**
     * Dopo quanto un file eliminato se ne va dal cestino da solo. Le tre decisioni dell'utente sono
     * in `Rules.md` § '🗑️ Lo svuotamento automatico del cestino' (`d-cestino-quando`,
     * `d-cestino-chiusa`, `d-cestino-editor`). Spento di fabbrica, perché cancella file per sempre.
     */
    val binKeep: BinKeep = BinKeep.NEVER,
    /**
     * I percorsi delle cartelle che non devono comparire fra quelle da sfogliare.
     *
     * ⚠️ **Percorsi e non identificativi** (richiesta dell'utente): l'id di una cartella nel
     * MediaStore si ricalcola dal percorso, quindi cambia con una rinomina o uno spostamento, e un
     * elenco di id smetterebbe di nascondere proprio le cartelle toccate. Nasconde anche il ramo
     * sotto (`Foo/Bar` con `Foo`), col confronto sul separatore. Nascondere non cancella niente: la
     * cartella si rivede dalle impostazioni.
     * ⚠️ **Un percorso del telefono è scritto senza la radice** (richiesta dell'utente):
     * `DCIM/Temp`, così un file di impostazioni funziona su un altro telefono; una scheda resta
     * intera, perché il suo identificativo la ritrova. Il perché è su `portablePath`, il confronto
     * su `coveringOf`: questo campo non si legge mai con `in` o `startsWith`. La chiave è nuova, e
     * il passaggio lo fa [HiddenMigration].
     */
    val hiddenFolders: Set<String> = emptySet(),
    /** Solo questa cartella è esclusa; le sottocartelle create dopo restano visibili. */
    val hiddenExactFolders: Set<String> = emptySet(),
    /** Di serie l'esclusione, il comportamento di prima, anche per chi aggiorna. */
    val folderMode: FolderMode = FolderMode.EXCLUDED,
    /** Fotocamera, Screenshots e Movies sono incluse di serie, quando ci sono. */
    val includedFolders: Set<String> = DEFAULT_INCLUDED_FOLDERS,
    /**
     * L'ordine delle cartelle nella griglia della schermata iniziale. Vedi [FolderOrder].
     *
     * ⚠️ **`null` è il valore di fabbrica, scelta dell'utente**: al primo avvio una finestra
     * chiede. Finché è `null` la griglia è alfabetica e la schermata iniziale chiede una volta
     * (`FolderOrderDialog`); la sua risposta `C1` chiede anche a chi aggiorna. La chiave si toglie
     * e non si scrive, così 'non scelto' sopravvive al salvataggio dell'oggetto intero.
     */
    val folderOrder: FolderOrder? = null,
    /**
     * Le cartelle preferite, come percorsi portabili (vedi `portablePath`), nell'ordine mostrato in
     * cima alla griglia e alla lista.
     *
     * ⚠️ **Una lista e non un insieme**, perché l'ordine è dell'utente; si salva come una stringa,
     * un percorso per riga, perché un insieme di stringhe in `DataStore` non ha ordine. Vuota di
     * serie.
     */
    val favouriteFolders: List<String> = emptyList(),
    /** Quale data mette in ordine il Feed. Vedi [FeedDate]. */
    val feedDate: FeedDate = FeedDate.TAKEN,
    /**
     * L'ordine dei campi delle informazioni sul file. Vedi [FactField]. Ci sono tutti, anche gli
     * spenti: con i soli visibili, uno riacceso non saprebbe dove tornare.
     */
    val factOrder: List<FactField> = FactField.entries,
    /**
     * I campi che l'utente ha spento: l'insieme dice chi non si vede, così un campo nuovo nasce
     * acceso. I tre campi [FactField.always] non ci finiscono mai: lo garantisce la lettura.
     */
    val factOff: Set<FactField> = emptySet(),
    /**
     * L'ordine dei sei tasti nel riquadro del tocco lungo: visualizzatore e albero lo condividono,
     * così chi impara dov'è 'Sposta' lo sa in tutti e due. Di fabbrica quello dettato dall'utente.
     * Nel cestino non si scambia niente: con un ordine scelto dall'utente, uno scambio sopra
     * sarebbe l'app che disobbedisce a chi ha appena riordinato.
     */
    val menuOrder: List<PadKey> = MENU_KEYS,
    /**
     * L'ordine dei dieci tasti della scheda della selezione: un ordine a sé, perché le due
     * superfici hanno insiemi diversi in ordini diversi, dettati dall'utente (vedi [MENU_KEYS]).
     */
    val pickOrder: List<PadKey> = PICK_KEYS,
    /** L'ordine della prima fila dell'editor: girare e centrare. */
    val turnOrder: List<PadKey> = TURN_KEYS,
    /** L'ordine della seconda fila dell'editor: la cronologia e la conferma. */
    val stepOrder: List<PadKey> = STEP_KEYS,
    /**
     * L'ordine dei moduli dell'editor completo (richiesta dell'utente). È scritto anche sui
     * telefoni senza editor completo (sotto Android 13): una lettura condizionale vorrebbe due
     * strade per un dato solo.
     */
    val modOrder: List<PadKey> = MOD_KEYS,
    /**
     * Se sotto le icone dei riquadri a griglia si legge la parola. Accesa di fabbrica, com'è sempre
     * stata: l'interruttore è per chi vuole i comandi più compatti (richiesta dell'utente). Vale
     * per i riquadri a griglia e non per i menu a lista, dove l'icona è accanto al testo.
     */
    val padLabels: Boolean = true,
    /**
     * Con che segno si riconosce l'ultimo media visualizzato in una griglia. Di fabbrica il tondo
     * (voce `tondo-opaco`); chi aggiorna da prima che la scelta esistesse trova l'angolo, deciso da
     * `MarkMigration`. Il valore di fabbrica è scritto in tre posti (questo, la lettura e la
     * migrazione), tenuti allineati dal banco (`ProfonditaTest` e `IndicatoreTest`).
     */
    val lastMark: LastMark = LastMark.DOT,
) {
    /** I campi da mostrare, nell'ordine scelto e senza quelli spenti. */
    val factRows: List<FactField> get() = factOrder.filterNot { it in factOff }
}

/**
 * Gli ordini di fabbrica dei quattro riquadri, che sono anche l'elenco di chi ci vive.
 *
 * ⚠️ **Queste liste fanno due mestieri**: dicono l'ordine dei tasti e quali tasti quel riquadro
 * conosce. [padOrderOf] usa il secondo per riempire i buchi di un archivio vecchio, quindi togliere
 * una voce da qui vuol dire che quel tasto non esiste più in quel riquadro. L'ordine è quello
 * dettato dall'utente. In AIV Play le prime due perdono i tasti che scrivono sui file
 * (`Store.allows`); 'Elimina' c'è, perché passa dal MediaStore (`MediaDelete`).
 */
val MENU_KEYS = listOf(
    PadKey.COPY, PadKey.MOVE, PadKey.SHARE,
    PadKey.RENAME, PadKey.DELETE, PadKey.INFO
).filter(Store::allows)

/** I dieci della scheda della selezione, nell'ordine dettato dall'utente. */
val PICK_KEYS = listOf(
    PadKey.RENAME, PadKey.INFO, PadKey.MOVE, PadKey.COPY, PadKey.SHARE,
    PadKey.DELETE, PadKey.LIST, PadKey.ALL, PadKey.NONE, PadKey.INVERT
).filter(Store::allows)

/**
 * La prima fila dell'editor: centrare, riflettere e girare.
 *
 * L'ordine di fabbrica è quello della mano destra. 'Rifletti' è al centro, dove l'ha chiesto
 * l'utente; chi aggiorna lo trova lì e non in fondo, perché `padOrderOf` infila un tasto nuovo
 * subito dopo l'ultimo dei suoi predecessori già in scena.
 * ⚠️ **La lunghezza è anche il numero di colonne della fila**, e la legge `EditorScreen`: un 5
 * scritto due volte farebbe andare a capo la fila al primo tasto aggiunto.
 */
val TURN_KEYS = listOf(
    PadKey.CENTRE_DOWN, PadKey.CENTRE_ACROSS, PadKey.FLIP, PadKey.TURN_LEFT, PadKey.TURN_RIGHT
)

/** I quattro della seconda fila dell'editor, nell'ordine della mano destra. */
val STEP_KEYS = listOf(
    PadKey.ORIGINAL, PadKey.UNDO, PadKey.REDO, PadKey.APPLY
)

/**
 * I moduli dell'editor completo, nell'ordine dettato dall'utente (Dettagli, Effetti, Geometria,
 * Ritaglio, Luce, Colore, HSL, Curve, Disegno, Stili).
 *
 * ⚠️ **È una permutazione della tabella dei moduli, non una seconda tabella**: qui le chiavi con
 * cui l'ordine si salva, là (`MODULES`, in `AdvancedEditorScreen.kt`) che cosa fa ogni modulo. Il
 * banco misura che i due elenchi si coprano: un modulo dimenticato qui sparirebbe dalla fila senza
 * errori. Chi ha già riordinato la fila tiene il suo ordine: questo elenco è il valore di fabbrica,
 * e `padOrderOf` lo usa per quello che l'archivio non dice.
 */
val MOD_KEYS = listOf(
    PadKey.MOD_DETAIL,
    /*
     * Gli Effetti accanto al Dettaglio, l'altro modulo che guarda i pixel vicini e con cui dividono
     * il bordo delle tessere.
     */
    PadKey.MOD_EFFECTS,
    PadKey.MOD_GEOMETRY, PadKey.MOD_CROP,
    PadKey.MOD_LIGHT, PadKey.MOD_COLOUR, PadKey.MOD_MIX,
    PadKey.MOD_TONE,
    /* Il Disegno è penultimo: dopo vengono gli Stili, sempre in fondo (vedi [stylesLast]). */
    PadKey.MOD_DRAW,
    /*
     * Gli Stili in coda: su uno schermo stretto sono da raggiungere scorrendo, e restano in coda
     * anche a chi ha riordinato la fila ([stylesLast]).
     */
    PadKey.MOD_PRESET
)

/**
 * L'ordine dei moduli con gli Stili in fondo, qualunque sia quello salvato (nota E dell'utente: gli
 * Stili sempre l'ultimo modulo a destra). Vale anche per chi ha già riordinato, e per il riquadro
 * delle impostazioni. Un modulo nuovo entra prima di loro da sé, perché `padOrderOf` lo infila dove
 * [MOD_KEYS] lo mette.
 */
fun stylesLast(order: List<PadKey>): List<PadKey> = order.sortedBy { it == PadKey.MOD_PRESET }

/**
 * Rilegge un ordine salvato, tollerando tutto quello che un archivio può avere di storto.
 *
 * ⚠️ **Quello che manca si aggiunge al posto che ha di fabbrica, non in fondo**: così una versione
 * futura può aggiungere un tasto senza che sparisca a chi ha già riordinato, o finisca dove nessuno
 * guarda. Un token che non appartiene a questo riquadro si scarta, e i doppioni contano una volta,
 * come in `factOrderOf`.
 */
fun padOrderOf(tokens: List<String>, base: List<PadKey>): List<PadKey> {
    val known = tokens.mapNotNull { t -> base.firstOrNull { it.token == t } }.distinct()
    if (known.size == base.size) return known
    val fuori = known.toMutableList()
    // Si scorre la lista di fabbrica e si infila ogni mancante subito dopo l'ultimo dei suoi
    // predecessori già in scena: un tasto nuovo compare accanto a quelli con cui è stato disegnato.
    // Mancanti consecutivi restano nel loro ordine, perché ognuno diventa il predecessore del
    // successivo.
    for (k in base) {
        if (k in fuori) continue
        val prima = base.subList(0, base.indexOf(k)).lastOrNull { it in fuori }
        fuori.add(if (prima == null) 0 else fuori.indexOf(prima) + 1, k)
    }
    return fuori
}

/**
 * Un archivio solo per tutto quello che l'app ricorda, dichiarato a livello di file perché lo
 * raggiungano le impostazioni e gli indirizzi recenti.
 *
 * ⚠️ **Deve essere UNO**: due delegati `preferencesDataStore` sullo stesso nome vanno in errore
 * alla prima lettura ('there are multiple DataStores active for the same file'), e due nomi diversi
 * farebbero due file per poche preferenze.
 */
private val Context.aivStore: DataStore<Preferences> by preferencesDataStore(
    name = "aiv-settings",
    produceMigrations = { listOf(MarkMigration, HiddenMigration) }
)

/**
 * Ogni scrittura su [aivStore] passa da qui, mai da `edit` direttamente.
 *
 * ⚠️ **DataStore esegue la modifica sul dispatcher del chiamante, tenendo il lucchetto di
 * scrittura** (`DataStoreImpl.transformAndWrite`, `withContext(callerContext)`, letto nei sorgenti
 * 1.2.1). Un chiamante il cui dispatcher si ferma lascia il lucchetto preso, e ogni scrittura
 * successiva del processo aspetta per sempre. Sul telefono il thread principale non si ferma; sul
 * banco ogni prova ha un dispatcher suo che si ferma con lei, e una prova finita a metà scrittura
 * ha bloccato `FeedTest` per ore su GitHub. La modifica gira su [Dispatchers.IO], che non si ferma:
 * presidiato da `ScritturaPreferenzeTest`.
 */
private suspend fun Context.writeStore(change: suspend (MutablePreferences) -> Unit) {
    withContext(Dispatchers.IO) { aivStore.edit(change) }
}

/**
 * La chiave dell'indicatore dell'ultimo media, a livello di file e non fra le altre: la legge anche
 * [MarkMigration], che vive accanto al delegato dello store, fuori da [SettingsStore].
 */
internal val LAST_MARK = stringPreferencesKey("last-mark")

/**
 * Chi aggiorna da prima che l'indicatore fosse una scelta tiene l'angolo; chi installa adesso trova
 * il valore di fabbrica di [Settings.lastMark], il tondo.
 *
 * ⚠️ **È una clausola dell'utente**: chi è già utente non deve vedere cambiare la sua interfaccia.
 * ⚠️ **Si decide una volta e si scrive**: leggere 'l'archivio è vuoto' a ogni lettura darebbe il
 * valore di fabbrica finché l'utente non tocca un'altra impostazione, e l'angolo da lì in poi. Una
 * migrazione gira prima della prima lettura e lascia una risposta che non cambia più. Il segno di
 * 'già utente' è un archivio non vuoto: qui vivono anche i promemoria che l'app scrive da sé (la
 * domanda sul permesso ai file, i mini onboarding). Un archivio davvero vuoto, cioè l'app
 * installata e mai aperta, riceve il valore di fabbrica.
 */
internal object MarkMigration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData[LAST_MARK] == null

    override suspend fun migrate(currentData: Preferences): Preferences =
        currentData.toMutablePreferences().apply {
            val quale = if (currentData.asMap().isEmpty()) Settings().lastMark else LastMark.CORNER
            this[LAST_MARK] = quale.token
        }

    override suspend fun cleanUp() = Unit
}

/**
 * L'elenco delle cartelle nascoste, coi percorsi senza la radice del volume (vedi `portablePath`),
 * e le altre chiavi delle cartelle. Sono a livello di file come [LAST_MARK]: [HIDDEN_FOLDERS] la
 * legge anche [HiddenMigration].
 */
internal val HIDDEN_FOLDERS = stringSetPreferencesKey("hidden-relative")
internal val HIDDEN_EXACT_FOLDERS = stringSetPreferencesKey("hidden-only")
internal val INCLUDED_FOLDERS = stringSetPreferencesKey("included-relative")
internal val FOLDER_MODE = stringPreferencesKey("folder-mode")
internal val INCLUDED_INITIALIZED = booleanPreferencesKey("included-initialized")
internal val FOLDER_ORDER = stringPreferencesKey("folder-order")
internal val FAVOURITE_FOLDERS = stringPreferencesKey("favourite-folders")
internal val FEED_DATE = stringPreferencesKey("feed-date")

/**
 * L'elenco nella forma di prima, coi percorsi interi: si legge una volta e poi non c'è più. Il nome
 * non si riusa mai: un file di impostazioni vecchio contiene questa chiave, e l'importazione la
 * traduce come la migrazione (`PREF_RETIRED`, in `Backup.kt`).
 */
internal val HIDDEN_FOLDERS_ABSOLUTE = stringSetPreferencesKey("hidden-folders")

/**
 * Chi aggiorna ritrova le sue cartelle nascoste, scritte nella forma nuova.
 *
 * ⚠️ **Una migrazione e non un ripiego in lettura, al contrario della distanza della filigrana**:
 * là la chiave vecchia diceva la stessa cosa con un'altra unità; qui l'elenco si scrive a ogni
 * cartella nascosta o rimostrata, e con due chiavi la prima scrittura le farebbe divergere. Le due
 * si uniscono, così un archivio che le avesse tutte e due non perde una metà.
 */
internal object HiddenMigration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData[HIDDEN_FOLDERS_ABSOLUTE] != null

    override suspend fun migrate(currentData: Preferences): Preferences =
        currentData.toMutablePreferences().apply {
            val vecchie = currentData[HIDDEN_FOLDERS_ABSOLUTE].orEmpty().map(::portablePath)
            this[HIDDEN_FOLDERS] = currentData[HIDDEN_FOLDERS].orEmpty() + vecchie
            remove(HIDDEN_FOLDERS_ABSOLUTE)
        }

    override suspend fun cleanUp() = Unit
}

/**
 * L'archivio com'è, chiave per chiave: per il backup, che lo porta fuori.
 *
 * ⚠️ **Con [rewritePreferences] sono le sole porte sull'archivio grezzo, e hanno un lettore solo**:
 * un accesso per nome può leggere un intero come una stringa, e `DataStore` allora va in errore. Il
 * backup può farlo perché la mappa dei tipi la tiene lui ([PREF_KEYS]), e una prova la confronta
 * con quello che [SettingsStore.save] scrive.
 */
internal suspend fun storedPreferences(context: Context): Preferences = context.aivStore.data.first()

/**
 * Riscrive l'archivio in una transazione sola, per il backup: un'importazione fermata a metà
 * lascerebbe un archivio fatto di due backup.
 */
internal suspend fun rewritePreferences(context: Context, change: (MutablePreferences) -> Unit) {
    context.writeStore { change(it) }
}

/** Legge e scrive le impostazioni. */
object SettingsStore {

    /** Le cartelle incluse di fabbrica si decidono quando il permesso c'è già. */
    internal suspend fun initializeIncluded(context: Context, defaults: Set<String>) {
        context.writeStore { p ->
            if (p[INCLUDED_INITIALIZED] != true) {
                val previous = p[INCLUDED_FOLDERS]
                if (previous == null || previous == DEFAULT_INCLUDED_FOLDERS) p[INCLUDED_FOLDERS] = defaults
                p[INCLUDED_INITIALIZED] = true
            }
        }
    }

    private val BG_TYPE = stringPreferencesKey("bg-type")
    private val BG_THEME = stringPreferencesKey("bg-theme")
    private val FIT_GROW = booleanPreferencesKey("fit-grow")
    private val ZOOM_MAX = floatPreferencesKey("zoom-max")
    private val SCALE_MODE = stringPreferencesKey("scale-mode")
    private val INFO_POSITION = stringPreferencesKey("info-position")
    private val INFO_VISIBLE = booleanPreferencesKey("info-visible")
    private val PANEL_DEPTH = stringPreferencesKey("panel-depth")

    /**
     * L'interruttore della sfocatura di prima, che si legge una volta sola e non si scrive più: chi
     * l'aveva spento perché il telefono non la regge non la ritrova accesa. Non si riscrive, o le
     * due chiavi sarebbero due fonti della stessa cosa: appena [PANEL_DEPTH] esiste, questa non si
     * guarda.
     */
    private val VEIL = booleanPreferencesKey("veil")
    private val ZOOM_IN_MENU = booleanPreferencesKey("zoom-in-menu")
    private val REVERSE_SEQUENCE = booleanPreferencesKey("sequence-reversed")
    private val START_FOLDER = longPreferencesKey("start-folder")
    private val START_FOLDER_NAME = stringPreferencesKey("start-folder-name")
    private val OPEN_AT_START = booleanPreferencesKey("open-at-start")
    private val FOLDER_VIEW = stringPreferencesKey("folder-view")
    private val CLIPBOARD_START = booleanPreferencesKey("clipboard-start")
    private val CLIPBOARD_DONE = stringPreferencesKey("clipboard-done")
    private val CLIPBOARD_WHEN = longPreferencesKey("clipboard-when")
    private val HAND = stringPreferencesKey("hand")
    private val MAIN_CONTROL = stringPreferencesKey("main-control")
    /** Ritirata: si legge solo per ricavare [MAIN_CONTROL]. */
    private val PHONE_PILL_ON = booleanPreferencesKey("phone-pill-on")
    private val PHONE_PILL = stringPreferencesKey("phone-pill")
    private val PILL_FILL = stringPreferencesKey("pill-fill")
    private val GLASS_RADIUS = intPreferencesKey("glass-radius")
    private val GLASS_INTENSITY = intPreferencesKey("glass-intensity")
    private val GLASS_TINT = intPreferencesKey("glass-tint")
    /** Ritirata: un archivio vecchio la contiene ancora, ma non si legge più. */
    private val GLASS_LIGHT = intPreferencesKey("glass-light")
    private val GLASS_SHIFT = intPreferencesKey("glass-shift")
    private val GLASS_COLOUR_LIGHT = intPreferencesKey("glass-colour-light")
    private val GLASS_COLOUR_DARK = intPreferencesKey("glass-colour-dark")
    private val CORNER_ROUND = booleanPreferencesKey("corner-round")
    private val LIST_PATH = booleanPreferencesKey("list-path")
    private val PICK_WEIGHT = booleanPreferencesKey("pick-weight")
    private val EDITOR_APP = stringPreferencesKey("editor-app")
    private val EDITOR_QUALITY = stringPreferencesKey("editor-quality")
    private val EDITOR_BACKUP = booleanPreferencesKey("editor-backup")
    private val MARK_ON = booleanPreferencesKey("mark-on")
    private val MARK_SPOT = stringPreferencesKey("mark-spot")

    /*
     * ⚠️ **La chiave della misura è nuova perché è cambiato il tipo**: `mark-size` conteneva il
     * token di un gettone, una stringa, e qui c'è un numero. Riusandola, una preferenza vecchia si
     * leggerebbe col tipo sbagliato, e `DataStore` va in errore invece di rispondere il valore di
     * fabbrica. Quella vecchia resta scritta e non si legge: chi aveva 'Grande' ritrova la misura
     * di fabbrica.
     */
    private val MARK_SIZE = intPreferencesKey("mark-size-pct")

    /**
     * La distanza dal bordo, in decimi di centesimo, e la chiave di prima che la traduce.
     *
     * ⚠️ **La chiave è nuova perché è cambiata l'unità**: `mark-air` conteneva centesimi, e un `3`
     * là e un `3` qui sono due distanze diverse; con la stessa chiave chi aggiorna troverebbe la
     * firma a filo del bordo. Non serve una migrazione, al contrario dell'indicatore
     * ([MarkMigration]): il ripiego guarda una chiave che nessuno scrive più, quindi risponde
     * sempre uguale.
     */
    private val MARK_AIR_FINE = intPreferencesKey("mark-air-tenths")
    private val MARK_AIR_OLD = intPreferencesKey("mark-air")
    private val MARK_ALPHA = intPreferencesKey("mark-alpha")
    private val SIZE_ON = booleanPreferencesKey("size-on")
    private val SIZE_MODE = stringPreferencesKey("size-mode")
    private val SIZE_VALUE = intPreferencesKey("size-value")
    private val ANIM_COUNTER = booleanPreferencesKey("anim-counter")
    private val LIST_COUNT = booleanPreferencesKey("list-count")
    private val LIST_TEXT = stringPreferencesKey("list-text")
    private val TREE_HIDDEN = booleanPreferencesKey("tree-hidden")
    private val TREE_PICTURES = booleanPreferencesKey("tree-pictures")
    private val UI_THEME = stringPreferencesKey("ui-theme")
    private val FOLDER_COLUMNS_KEY = intPreferencesKey("folder-columns")
    private val BIN_ON = booleanPreferencesKey("bin-on")
    private val IMAGES_ONLY = booleanPreferencesKey("images-only")
    private val CLIP_AUTOPLAY = booleanPreferencesKey("clip-autoplay")
    private val GRID_NAMES = booleanPreferencesKey("grid-names")
    private val FRONT_WASH = booleanPreferencesKey("front-wash")
    private val FRONT_SERIF = booleanPreferencesKey("front-serif")
    private val FRONT_FACTS = booleanPreferencesKey("front-facts")
    private val FRONT_PICK_ALL = booleanPreferencesKey("front-pick-all")
    // La chiave resta `ext-edit` anche se il campo si chiama `extRename`: chi l'aveva accesa la
    // ritrova accesa.
    private val EXT_EDIT = booleanPreferencesKey("ext-edit")
    private val EXT_DOWNLOAD = booleanPreferencesKey("ext-download")
    private val GPU_THUMBS = booleanPreferencesKey("gpu-thumbs")
    private val AUTO_UPDATE = booleanPreferencesKey("auto-update")
    private val SAVE_RENAME = booleanPreferencesKey("save-rename")
    private val DOWNLOAD_PATH = booleanPreferencesKey("download-path")
    private val BIN_KEEP = stringPreferencesKey("bin-keep")
    private val FOLDER_COUNT = booleanPreferencesKey("folder-count")
    private val FOLDER_COLOUR = stringPreferencesKey("folder-colour")

    // Due chiavi, perché sono due domande: in che ordine sono i campi, e quali sono spenti (vedi
    // `Settings.factOrder`).
    private val FACT_ORDER = stringPreferencesKey("fact-order")
    private val FACT_OFF = stringSetPreferencesKey("fact-off")

    /*
     * ⚠️ **Quattro chiavi per quattro riquadri**: i due riquadri delle azioni sui file hanno
     * insiemi diversi di tasti in ordini diversi, tutti e due dettati dall'utente, e nessuno è il
     * sottoinsieme ordinato dell'altro. Le due file dell'editor sono due gruppi (girare e centrare,
     * la cronologia), e un tasto non passa dall'una all'altra.
     */
    private val MENU_ORDER = stringPreferencesKey("menu-order")
    private val PICK_ORDER = stringPreferencesKey("pick-order")
    private val TURN_ORDER = stringPreferencesKey("turn-order")
    private val STEP_ORDER = stringPreferencesKey("step-order")
    private val MOD_ORDER = stringPreferencesKey("mod-order")
    private val PAD_LABELS = booleanPreferencesKey("pad-labels")

    /**
     * I limiti della sola impostazione numerica, così un valore salvato fuori scala non arriva al
     * visualizzatore.
     */
    const val ZOOM_MAX_MIN = 2f
    const val ZOOM_MAX_MAX = 200f

    fun flow(context: Context): Flow<Settings> = context.aivStore.data.map(::read)

    /**
     * Le preferenze come le legge l'app, coi ripieghi di fabbrica di [Settings]. È una funzione a
     * sé perché il banco la possa chiamare con delle `Preferences` scritte a mano, come
     * `IndicatoreTest` con [MarkMigration]: un valore di fabbrica vive qui e sul campo (vedi
     * [Settings.panelDepth]).
     */
    internal fun read(p: Preferences): Settings {
        // Il modo si legge prima, perché il valore del ridimensionamento ha confini diversi secondo
        // il modo: leggerlo nel costruttore vorrebbe leggere la chiave due volte.
        val sizeMode = Resize.Mode.entries.byToken(p[SIZE_MODE], Resize.NONE.mode)
        // AIV Play fissa alcuni valori, qualunque cosa dica l'archivio: il perché è su `Store.fit`.
        return Store.fit(Settings(
            bgType = BgType.entries.byToken(p[BG_TYPE], BgType.CHECKER),
            bgTheme = BgTheme.entries.byToken(p[BG_THEME], BgTheme.AUTO),
            fitGrow = p[FIT_GROW] ?: false,
            zoomMax = (p[ZOOM_MAX] ?: 40f).coerceIn(ZOOM_MAX_MIN, ZOOM_MAX_MAX),
            scaleMode = ScaleMode.entries.byToken(p[SCALE_MODE], ScaleMode.PHYSICAL),
            infoPosition = InfoPosition.entries.byToken(p[INFO_POSITION], InfoPosition.TOP),
            infoVisible = p[INFO_VISIBLE] ?: true,
            /*
             * Il ripiego sulla chiave vecchia vale finché la nuova non è mai stata scritta (vedi
             * [VEIL]). I casi sono tre: chi aveva acceso la sfocatura la tiene, chi l'aveva spenta
             * tiene il niente, chi non l'ha mai toccata riceve il valore di fabbrica. ⚠️ Il primo e
             * il terzo oggi coincidono, ma non si accorpano: al cambio del valore di fabbrica una
             * scelta esplicita se ne andrebbe con lui.
             */
            panelDepth = p[PANEL_DEPTH]
                ?.let { PanelDepth.entries.byToken(it, PanelDepth.BLUR) }
                ?: when (p[VEIL]) {
                    false -> PanelDepth.NONE
                    true -> PanelDepth.BLUR
                    else -> PanelDepth.BLUR
                },
            zoomInMenu = p[ZOOM_IN_MENU] ?: false,
            reverseSequence = p[REVERSE_SEQUENCE] ?: false,
            startFolder = p[START_FOLDER],
            startFolderName = p[START_FOLDER_NAME] ?: "",
            openAtStart = p[OPEN_AT_START] ?: false,
            folderView = Store.views.byToken(p[FOLDER_VIEW], FolderView.GRID),
            clipboardStart = p[CLIPBOARD_START] ?: false,
            clipboardDone = p[CLIPBOARD_DONE] ?: "",
            clipboardWhen = p[CLIPBOARD_WHEN] ?: 0L,
            hand = Hand.entries.byToken(p[HAND], Hand.RIGHT),
            // Un archivio vecchio ha l'interruttore, uno più vecchio il solo gettone: `off` vuol
            // dire spenta, gli altri due accesa. Un archivio che non dice niente dà il valore di
            // fabbrica della variante ([Store.mainControl]).
            mainControl = p[MAIN_CONTROL]?.let { MainControl.entries.byToken(it, Store.mainControl) }
                ?: when (p[PHONE_PILL_ON] ?: p[PHONE_PILL]?.let { it != PhonePill.OFF.token }) {
                    null -> Store.mainControl
                    true -> MainControl.PILL
                    false -> MainControl.FAB
                },
            phonePill = PhonePill.entries.byToken(p[PHONE_PILL], PhonePill.SLIDE)
                .takeIf { it != PhonePill.OFF } ?: PhonePill.SLIDE,
            pillFill = PillFill.entries.byToken(p[PILL_FILL], PillFill.GLASS),
            glass = GlassTune(
                radius = (p[GLASS_RADIUS] ?: GlassTune.RADIUS).coerceIn(GlassTune.RADIUS_RANGE),
                intensity = (p[GLASS_INTENSITY] ?: GlassTune.INTENSITY).coerceIn(GlassTune.INTENSITY_RANGE),
                tint = (p[GLASS_TINT] ?: GlassTune.TINT).coerceIn(GlassTune.TINT_RANGE),
                // Non si ricava da `glass-light`: là zero era il valore di fabbrica, e un archivio
                // vecchio spegnerebbe lo scostamento che l'utente ha voluto di fabbrica.
                shift = (p[GLASS_SHIFT] ?: GlassTune.SHIFT).coerceIn(GlassTune.SHIFT_RANGE),
                lightColour = p[GLASS_COLOUR_LIGHT],
                darkColour = p[GLASS_COLOUR_DARK]
            ),
            cornerRound = p[CORNER_ROUND] ?: true,
            listPath = p[LIST_PATH] ?: false,
            pickWeight = p[PICK_WEIGHT] ?: true,
            editorApp = p[EDITOR_APP] ?: "",
            editorQuality = Quality.entries.byToken(p[EDITOR_QUALITY], Quality.DEFAULT),
            editorBackup = p[EDITOR_BACKUP] ?: true,
            markOn = p[MARK_ON] ?: false,
            markSpot = Watermark.Spot.entries.byToken(
                p[MARK_SPOT], Watermark.Spot.BOTTOM_RIGHT
            ),
            // I tre numeri si riportano nei loro confini in lettura: un archivio scritto a mano, o
            // da una versione che allarga una corsa, darebbe un cursore fuori scala.
            markSize = (p[MARK_SIZE] ?: Watermark.SIZE_DEFAULT).coerceIn(Watermark.SIZE),
            // Il ripiego traduce i centesimi di prima in decimi: il perché è su [MARK_AIR_FINE].
            markAir = (
                p[MARK_AIR_FINE]
                    ?: p[MARK_AIR_OLD]?.times(Watermark.AIR_STEP)
                    ?: Watermark.AIR_DEFAULT
                ).coerceIn(Watermark.AIR),
            markAlpha = (p[MARK_ALPHA] ?: Watermark.ALPHA_DEFAULT).coerceIn(Watermark.ALPHA),
            sizeOn = p[SIZE_ON] ?: false,
            sizeMode = sizeMode,
            // Il valore si riporta nei confini del suo modo: fra pixel e percentuale i limiti sono
            // diversi.
            sizeValue = (p[SIZE_VALUE] ?: Resize.NONE.value).coerceIn(Resize.range(sizeMode)),
            animCounter = p[ANIM_COUNTER] ?: true,
            listCount = p[LIST_COUNT] ?: true,
            listText = TextSize.entries.byToken(p[LIST_TEXT], TextSize.NORMAL),
            treeHidden = p[TREE_HIDDEN] ?: false,
            treePictures = p[TREE_PICTURES] ?: false,
            uiTheme = UiTheme.entries.byToken(p[UI_THEME], UiTheme.SYSTEM),
            // Ricondotto all'elenco ammesso: un numero fuori posto darebbe una griglia a zero
            // colonne, cioè una schermata vuota senza errori.
            folderColumns = p[FOLDER_COLUMNS_KEY]?.takeIf { it in FOLDER_COLUMNS } ?: 2,
            folderCount = p[FOLDER_COUNT] ?: true,
            folderColour = FolderColour.entries.byToken(p[FOLDER_COLOUR], FolderColour.NAME),
            binOn = p[BIN_ON] ?: true,
            imagesOnly = p[IMAGES_ONLY] ?: false,
            clipAutoplay = p[CLIP_AUTOPLAY] ?: false,
            gridNames = p[GRID_NAMES] ?: false,
            // I quattro valori di fabbrica sono scritti anche qui e non solo sui campi: il perché è
            // in `Rules.md`, § '⚙️ Dove va un'impostazione, e chi la deve trovare'.
            frontWash = p[FRONT_WASH] ?: true,
            frontSerif = p[FRONT_SERIF] ?: false,
            frontFacts = p[FRONT_FACTS] ?: true,
            frontPickAll = p[FRONT_PICK_ALL] ?: true,
            extRename = p[EXT_EDIT] ?: false,
            extDownload = p[EXT_DOWNLOAD] ?: false,
            gpuThumbs = p[GPU_THUMBS] ?: false,
            autoUpdate = p[AUTO_UPDATE] ?: true,
            saveRename = p[SAVE_RENAME] ?: false,
            downloadPath = p[DOWNLOAD_PATH] ?: false,
            binKeep = BinKeep.entries.byToken(p[BIN_KEEP], BinKeep.NEVER),
            hiddenFolders = p[HIDDEN_FOLDERS] ?: emptySet(),
            hiddenExactFolders = p[HIDDEN_EXACT_FOLDERS] ?: emptySet(),
            includedFolders = p[INCLUDED_FOLDERS] ?: DEFAULT_INCLUDED_FOLDERS,
            folderMode = FolderMode.entries.firstOrNull { it.token == p[FOLDER_MODE] } ?: FolderMode.EXCLUDED,
            // Un token sconosciuto vale 'non scelto', e la schermata iniziale chiede di nuovo: un
            // ordine sbagliato dato in silenzio sarebbe peggio di una domanda in più.
            folderOrder = FolderOrder.entries.firstOrNull { it.token == p[FOLDER_ORDER] },
            favouriteFolders = favouritesOf(p[FAVOURITE_FOLDERS]),
            feedDate = FeedDate.entries.byToken(p[FEED_DATE], FeedDate.TAKEN),
            factOrder = factOrderOf((p[FACT_ORDER] ?: "").split(',')),
            menuOrder = padOrderOf((p[MENU_ORDER] ?: "").split(','), MENU_KEYS),
            pickOrder = padOrderOf((p[PICK_ORDER] ?: "").split(','), PICK_KEYS),
            turnOrder = padOrderOf((p[TURN_ORDER] ?: "").split(','), TURN_KEYS),
            stepOrder = padOrderOf((p[STEP_ORDER] ?: "").split(','), STEP_KEYS),
            modOrder = stylesLast(padOrderOf((p[MOD_ORDER] ?: "").split(','), MOD_KEYS)),
            padLabels = p[PAD_LABELS] ?: true,
            /*
             * Il ripiego è il valore di fabbrica dichiarato, e in pratica non si usa: la chiave la
             * scrive `MarkMigration` prima della prima lettura.
             */
            lastMark = LastMark.entries.byToken(p[LAST_MARK], LastMark.DOT),
            // I campi sempre visibili si tolgono in lettura: un archivio che li dichiarasse spenti
            // non deve far sparire il nome del file.
            factOff = (p[FACT_OFF] ?: emptySet())
                .mapNotNull { t -> FactField.entries.firstOrNull { it.token == t } }
                .filterNot { it.always }
                .toSet(),
        ))
    }

    /**
     * Segna che questo indirizzo degli appunti è stato aperto, e non si riaprirà. Scrive le sole
     * due chiavi e non passa da [save], che riscrive tutto l'oggetto e potrebbe sovrascrivere con
     * una copia vecchia quello che le impostazioni hanno cambiato nel frattempo.
     */
    suspend fun clipboardOpened(context: Context, address: String, at: Long) {
        context.writeStore { p ->
            p[CLIPBOARD_DONE] = address
            // Le due chiavi si scrivono insieme, in una transazione (`edit`): separate, un arresto
            // fra le due lascerebbe un indirizzo con l'istante di un altro.
            p[CLIPBOARD_WHEN] = at
        }
    }

    /**
     * Scrive l'oggetto intero.
     *
     * ⚠️ **In AIV Play le chiavi che `Store.fit` fissa non si scrivono** (rilievo E17): l'oggetto è
     * già passato da `Store.fit`, e scriverne i valori riscriverebbe l'archivio, contro la regola
     * di `Store.fit`; si vedrebbe il giorno in cui una restrizione cade, o esportando da AIV Play
     * verso AIV GitHub. L'elemento principale si scrive quando la scelta differisce da quella che
     * l'archivio dava, cioè quando l'utente l'ha cambiata.
     */
    suspend fun save(context: Context, settings: Settings) {
        val fixed = !Store.fullSettings
        context.writeStore { p ->
            val control = read(p).mainControl
            p[BG_TYPE] = settings.bgType.token
            p[BG_THEME] = settings.bgTheme.token
            p[FIT_GROW] = settings.fitGrow
            p[ZOOM_MAX] = settings.zoomMax.coerceIn(ZOOM_MAX_MIN, ZOOM_MAX_MAX)
            p[SCALE_MODE] = settings.scaleMode.token
            p[INFO_POSITION] = settings.infoPosition.token
            p[INFO_VISIBLE] = settings.infoVisible
            if (!fixed) p[PANEL_DEPTH] = settings.panelDepth.token
            p[ZOOM_IN_MENU] = settings.zoomInMenu
            if (!fixed) p[REVERSE_SEQUENCE] = settings.reverseSequence
            // Una cartella tolta si cancella e non si scrive a zero: zero è un id come un altro.
            // L'assenza della chiave è l'unico modo di dire 'nessuna'.
            settings.startFolder?.let { p[START_FOLDER] = it } ?: p.remove(START_FOLDER)
            p[START_FOLDER_NAME] = settings.startFolderName
            p[OPEN_AT_START] = settings.openAtStart
            p[FOLDER_VIEW] = settings.folderView.token
            p[CLIPBOARD_START] = settings.clipboardStart
            p[HAND] = settings.hand.token
            if (!fixed || settings.mainControl != control) p[MAIN_CONTROL] = settings.mainControl.token
            p[PHONE_PILL] = settings.phonePill.token
            p[PILL_FILL] = settings.pillFill.token
            p[GLASS_RADIUS] = settings.glass.radius
            p[GLASS_INTENSITY] = settings.glass.intensity
            p[GLASS_TINT] = settings.glass.tint
            p[GLASS_SHIFT] = settings.glass.shift
            // Un colore che torna all'accento si toglie: l'assenza è il valore di fabbrica.
            settings.glass.lightColour?.let { p[GLASS_COLOUR_LIGHT] = it } ?: p.remove(GLASS_COLOUR_LIGHT)
            settings.glass.darkColour?.let { p[GLASS_COLOUR_DARK] = it } ?: p.remove(GLASS_COLOUR_DARK)
            p.remove(GLASS_LIGHT)
            p[CORNER_ROUND] = settings.cornerRound
            p[LIST_PATH] = settings.listPath
            p[PICK_WEIGHT] = settings.pickWeight
            p[EDITOR_APP] = settings.editorApp
            p[EDITOR_QUALITY] = settings.editorQuality.token
            p[EDITOR_BACKUP] = settings.editorBackup
            p[MARK_ON] = settings.markOn
            p[MARK_SPOT] = settings.markSpot.token
            p[MARK_SIZE] = settings.markSize
            p[MARK_AIR_FINE] = settings.markAir
            p[MARK_ALPHA] = settings.markAlpha
            p[SIZE_ON] = settings.sizeOn
            p[SIZE_MODE] = settings.sizeMode.token
            p[SIZE_VALUE] = settings.sizeValue
            p[ANIM_COUNTER] = settings.animCounter
            p[LIST_COUNT] = settings.listCount
            p[LIST_TEXT] = settings.listText.token
            p[TREE_HIDDEN] = settings.treeHidden
            p[TREE_PICTURES] = settings.treePictures
            // Si riscrive anche qui, o il salvataggio delle impostazioni, che scrive l'oggetto
            // intero, cancellerebbe l'indirizzo già aperto, e gli appunti si riaprirebbero.
            p[CLIPBOARD_DONE] = settings.clipboardDone
            p[CLIPBOARD_WHEN] = settings.clipboardWhen
            p[UI_THEME] = settings.uiTheme.token
            p[FOLDER_COLUMNS_KEY] = settings.folderColumns
            p[FOLDER_COUNT] = settings.folderCount
            if (!fixed) p[FOLDER_COLOUR] = settings.folderColour.token
            p[BIN_ON] = settings.binOn
            p[IMAGES_ONLY] = settings.imagesOnly
            p[CLIP_AUTOPLAY] = settings.clipAutoplay
            p[GRID_NAMES] = settings.gridNames
            if (!fixed) p[FRONT_WASH] = settings.frontWash
            if (!fixed) p[FRONT_SERIF] = settings.frontSerif
            p[FRONT_FACTS] = settings.frontFacts
            p[FRONT_PICK_ALL] = settings.frontPickAll
            if (!fixed) p[EXT_EDIT] = settings.extRename
            if (!fixed) p[EXT_DOWNLOAD] = settings.extDownload
            p[GPU_THUMBS] = settings.gpuThumbs
            p[AUTO_UPDATE] = settings.autoUpdate
            p[SAVE_RENAME] = settings.saveRename
            p[DOWNLOAD_PATH] = settings.downloadPath
            p[BIN_KEEP] = settings.binKeep.token
            p[HIDDEN_FOLDERS] = settings.hiddenFolders
            p[HIDDEN_EXACT_FOLDERS] = settings.hiddenExactFolders
            p[INCLUDED_FOLDERS] = settings.includedFolders
            p[FOLDER_MODE] = settings.folderMode.token
            settings.folderOrder?.let { p[FOLDER_ORDER] = it.token } ?: p.remove(FOLDER_ORDER)
            p[FAVOURITE_FOLDERS] = settings.favouriteFolders.joinToString(FAVOURITE_JOIN)
            p[FEED_DATE] = settings.feedDate.token
            p[FACT_ORDER] = settings.factOrder.joinToString(",") { it.token }
            p[MENU_ORDER] = settings.menuOrder.joinToString(",") { it.token }
            p[PICK_ORDER] = settings.pickOrder.joinToString(",") { it.token }
            p[TURN_ORDER] = settings.turnOrder.joinToString(",") { it.token }
            p[STEP_ORDER] = settings.stepOrder.joinToString(",") { it.token }
            p[MOD_ORDER] = settings.modOrder.joinToString(",") { it.token }
            p[PAD_LABELS] = settings.padLabels
            p[LAST_MARK] = settings.lastMark.token
            p[FACT_OFF] = settings.factOff.filterNot { it.always }.map { it.token }.toSet()
        }
    }
}

/** Il separatore dei preferiti salvati: un percorso non contiene mai un a capo. */
internal const val FAVOURITE_JOIN = "\n"

/**
 * I preferiti riletti dalla loro stringa, senza vuoti e senza doppioni: un file di impostazioni
 * modificato a mano, o due importazioni, non devono mettere due volte la stessa cartella in cima.
 */
internal fun favouritesOf(stored: String?): List<String> =
    stored.orEmpty().split(FAVOURITE_JOIN).filter { it.isNotBlank() }.distinct()

/**
 * Un token sconosciuto, o assente, ripiega sul valore di serie senza andare in errore: un archivio
 * che una versione futura non riconosce non deve impedire all'app di partire.
 */
private fun <T : Choice> List<T>.byToken(token: String?, fallback: T): T =
    if (token == null) fallback else firstOrNull { it.token == token } ?: fallback

/**
 * Se la domanda sul permesso alle cartelle è già stata fatta una volta.
 *
 * ⚠️ È qui e non fra le impostazioni perché non è una scelta dell'utente: è un promemoria dell'app.
 * Il permesso si chiede una volta sola, alla prima immagine locale aperta: chiedere e richiedere
 * insegna a rifiutare per riflesso.
 */
object FolderAsk {

    // ⚠️ La chiave è `all-files-asked` perché la domanda è cambiata (la pagina di sistema
    // sull'accesso a tutti i file): con la chiave della domanda di prima, ogni telefono che aveva
    // già risposto non avrebbe mai visto quella nuova. Una domanda nuova vuole un promemoria nuovo.
    private val ASKED = booleanPreferencesKey("all-files-asked")

    fun flow(context: Context): Flow<Boolean> = context.aivStore.data.map { p -> p[ASKED] ?: false }

    suspend fun remember(context: Context) {
        context.writeStore { p -> p[ASKED] = true }
    }

    /**
     * Dimentica di aver chiesto, così la domanda torna alla prossima immagine locale: l'unica via
     * di ritorno da un 'no' dato di fretta (richiesta dell'utente). Non concede niente: il permesso
     * resta quello del sistema, e chi l'ha già concesso non vede niente, perché la richiesta guarda
     * prima `Folder.granted`.
     */
    suspend fun forget(context: Context) {
        context.writeStore { p -> p.remove(ASKED) }
    }
}

/**
 * I mini onboarding dell'app, e se si sono già visti.
 *
 * ⚠️ **Sono qui e non in [Settings]**: `Settings` è quello che l'utente sceglie nella schermata
 * delle impostazioni, questi sono promemoria che l'app tiene per sé. Ognuno si archivia una volta
 * sola: un onboarding che torna è un avviso, e un avviso che torna insegna a chiuderlo senza
 * leggerlo (la stessa ragione di [FolderAsk]).
 */
enum class Hint(internal val token: String) {
    BIN_EMPTY("bin-empty-hint-seen"),

    /**
     * La scorciatoia delle colonne, solo nella schermata delle cartelle: nella griglia delle
     * immagini non si insegna, quindi il `when` sulle frasi in `GridScreen` ha un ramo che non si
     * vedrà mai.
     */
    COLUMNS("columns-hint-seen"),

    /**
     * Il doppio tocco che cambia lo zoom, nel visualizzatore, alla prima immagine aperta. È la
     * contropartita della rimozione di 'Adatta alla vista' e '100%' dal menu a pressione lunga:
     * insegna l'unico modo rimasto. Il gesto si fa sull'immagine intera, quindi `HintCentre` e non
     * `HintVeil`.
     */
    ZOOM_TAP("zoom-tap-hint-seen"),

    /**
     * I due rischi del cambio di estensione, nel pannellino della rinomina, la prima volta che si
     * apre.
     *
     * ⚠️ **È il terzo presidio** (richiesta dell'utente), con l'interruttore spento di fabbrica e
     * il suo paragrafo (vedi [Settings.extRename]). È il solo velo che avvisa invece di insegnare,
     * e compare prima che il pannellino si apra: un avviso dopo il gesto non è un avviso.
     */
    EXT_WARN("ext-warn-hint-seen"),

    /**
     * La copertina scelta a mano, nella griglia di una cartella, la prima volta che il tocco
     * sull'icona dell'intestazione avvia la scelta (voce `copertina-togli`): quel gesto fa due cose
     * secondo la cartella, e senza il velo resterebbe per chi lo sa. Il testo è dell'utente. Indica
     * l'icona e non un FAB, quindi `HintSpot`.
     */
    COVER("cover-hint-seen"),

    /**
     * La fila dei moduli dell'editor completo, la prima volta che quell'editor si apre: la fila
     * continua fuori dallo schermo, e lo scorrimento non si dichiara da sé. Il testo è dell'utente,
     * e dice che si scorre e che il tocco lungo su un gettone azzera quel modulo. Indica una fila,
     * quindi `HintStrip`.
     */
    MODULES("modules-hint-seen"),

    /**
     * I due comandi del salvataggio e il tasto 'Salva', la seconda slide del velo del primo avvio
     * dell'editor completo (testo dell'utente): quei comandi hanno due gesti ciascuno, e un gesto
     * lungo non si dichiara da sé.
     * ⚠️ **È una chiave a sé e non un secondo passo di [MODULES]**: con una chiave sola, chi aveva
     * già archiviato la prima non vedrebbe mai la seconda. Viene dopo la prima, e la condizione la
     * guarda.
     */
    EDITOR_TOOLS("editor-tools-hint-seen"),

    /**
     * Il modulo Disegno, la prima volta che si apre, col tondo rosso evidenziato e la frase sopra
     * (testo dell'utente, nota B sul giro della `4.60`): che cosa si disegna, che il salvataggio
     * fonde gli elementi nell'immagine, e che la pressione lunga su un tondo regola la luminosità.
     * Viene dopo le due slide dell'editor, e la condizione le guarda.
     */
    DRAW("draw-hint-seen"),

    /**
     * Il Feed, la prima volta che la schermata iniziale lo mostra: una frase al centro (richiesta
     * dell'utente, col testo lasciato alla sessione). 'Recenti' ha due significati, e il flusso ne
     * segue uno finché non si sceglie l'altro: la frase dice dov'è la scelta. Non indica niente,
     * come [ZOOM_TAP]: la scelta è dietro il tocco lungo sull'elemento principale. Da qui
     * `HintCentre`.
     */
    FEED("feed-hint-seen");

    private val seen = booleanPreferencesKey(token)

    fun flow(context: Context): Flow<Boolean> = context.aivStore.data.map { p -> p[seen] ?: false }

    suspend fun remember(context: Context) {
        context.writeStore { p -> p[seen] = true }
    }

    /**
     * Dimentica di averlo mostrato, così il velo torna. Toglie la chiave e non scrive `false`: una
     * chiave assente vuol dire 'mai visto', che è la verità che si ripristina.
     */
    suspend fun forget(context: Context) {
        context.writeStore { p -> p.remove(seen) }
    }
}

/**
 * Le immagini che AIV ha aperto, dalla più recente.
 *
 * ⚠️ **Sostituisce quello che era stato chiesto**, cioè le immagini delle pagine visitate in Chrome
 * o Brave, che non si può fare: la cronologia di un browser vive nella sua memoria privata, e il
 * fornitore pubblico che la esponeva (`Browser.BOOKMARKS_URI`, dietro READ_HISTORY_BOOKMARKS) è
 * stato chiuso con Android 6. Questa è la cosa vera più vicina: quello che si è aperto qui.
 * ⚠️ **Si ricordano solo gli indirizzi web**: il permesso su un `content://` ricevuto da un'altra
 * app dura quanto quell'intent, e un'immagine locale ricordata sarebbe una riga che sembra apribile
 * e fallisce.
 */
object Recents {

    private val ENTRIES = stringPreferencesKey("recent")

    /** Otto, perché la lista deve entrare sotto tre tasti senza diventare la schermata. */
    private const val KEEP = 8

    /**
     * Tabulazione fra i due campi e a capo fra le voci, senza sequenze di escape: nessuno dei due
     * caratteri è ammesso in un URL, e un nome che ne contenesse uno verrebbe da un file system che
     * non lo ammette.
     */
    fun flow(context: Context): Flow<List<RecentImage>> = context.aivStore.data.map { p ->
        (p[ENTRIES] ?: "").lineSequence()
            .mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size == 2 && parts[0].isNotBlank()) RecentImage(parts[0], parts[1]) else null
            }
            .take(KEEP)
            .toList()
    }

    suspend fun remember(context: Context, address: String, name: String) {
        if (!address.startsWith("http://", true) && !address.startsWith("https://", true)) return
        if (address.contains('\t') || address.contains('\n')) return
        val clean = name.replace('\t', ' ').replace('\n', ' ')
        context.writeStore { p ->
            val kept = (p[ENTRIES] ?: "").lineSequence()
                .filter { it.isNotBlank() && it.substringBefore('\t') != address }
                .take(KEEP - 1)
                .toList()
            p[ENTRIES] = (listOf("$address\t$clean") + kept).joinToString("\n")
        }
    }

    suspend fun clear(context: Context) {
        context.writeStore { p -> p.remove(ENTRIES) }
    }
}

/**
 * La cartella di destinazione del salvataggio, scelta dall'utente e ricordata dall'app: il file si
 * salva lì solo alla pressione di 'Salva' (richiesta dell'utente).
 *
 * ⚠️ **Non è in [Settings], apposta**: quella classe è quello che si tocca nel pannello, e ogni suo
 * campo ha una riga in una schermata (`Rules.md` § '⚙️ Dove va un'impostazione, e chi la deve
 * trovare'). Questo è un dato che l'app ricorda, come i veli ([Hint]) e gli indirizzi recenti.
 * ⚠️ **L'indirizzo non basta: serve il permesso persistente**, preso dal richiamo del selettore
 * (`takePersistableUriPermission`). Senza, l'indirizzo si rilegge il giorno dopo e la scrittura va
 * in `SecurityException`, un difetto che non si vede provando subito.
 */
object DownloadFolder {
    private val TREE = stringPreferencesKey("download-tree")

    /** L'albero scelto, o `null` se non ne è stato scelto nessuno: allora si scrive in Download. */
    fun flow(context: Context): Flow<String?> = context.aivStore.data.map { p -> p[TREE] }

    suspend fun set(context: Context, tree: String?) {
        context.writeStore { p ->
            if (tree.isNullOrBlank()) p.remove(TREE) else p[TREE] = tree
        }
    }

    /**
     * Scorda la cartella scelta e rende il permesso persistente che l'accompagnava.
     *
     * ⚠️ **Il selettore non sa riportare a Download** (voce `save-percorso`: Android rifiuta quella
     * cartella 'per tutelare la privacy'), quindi senza questa funzione una cartella scelta sarebbe
     * per sempre. Il permesso si rende, perché un albero inutilizzato tiene uno dei pochi posti che
     * il sistema concede a un'app. La resa può fallire (permesso scaduto, cartella smontata), e non
     * è un guasto: l'indirizzo si toglie comunque.
     */
    suspend fun forget(context: Context) {
        val tree = context.aivStore.data.first()[TREE]
        if (tree != null) {
            try {
                context.contentResolver.releasePersistableUriPermission(
                    android.net.Uri.parse(tree),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Non era più nostro: l'indirizzo si toglie lo stesso.
            }
        }
        set(context, null)
    }

    /**
     * Il nome da mostrare per una cartella scelta, ricavato dall'identificatore del documento: un
     * albero arriva come `content://...tree/primary%3APictures%2FVacanze`, l'identificatore è
     * `primary:Pictures/Vacanze`, e serve l'ultimo pezzo. La radice di un volume non ha un pezzo
     * dopo i due punti, e si mostra il volume: meglio `primary` che una riga vuota.
     */
    fun label(tree: String): String {
        val id = tree.substringAfterLast("/tree/", "").let { android.net.Uri.decode(it) }
        val path = id.substringAfter(':', "").trim('/')
        return path.substringAfterLast('/').ifBlank { id.substringBefore(':') }.ifBlank { id }
    }
}

/**
 * Che cosa è già finito in Download di recente, per non scaricarlo due volte (richiesta
 * dell'utente: stesso suffisso e stessi byte, notifica 'Hai già scaricato questa immagine' con
 * 'Scarica di nuovo').
 *
 * ⚠️ **La firma è suffisso più byte, come chiesto**: il nome cambia con una rinomina, e un'impronta
 * del contenuto vorrebbe rileggere il file intero. Due immagini diverse con lo stesso suffisso e lo
 * stesso numero esatto di byte sono rare, e l'errore costa una notifica.
 * ⚠️ **Il registro dura trenta giorni e duecento voci** (il consiglio chiesto dall'utente): una
 * voce pesa una trentina di byte, quindi duecento sono meno di sette kilobyte; oltre trenta giorni
 * 'l'ho già scaricata' sorprende invece di ricordare. Il tempo tiene il registro onesto, il tetto
 * lo tiene piccolo. La potatura si fa in scrittura, che succede solo a salvataggio riuscito.
 */
object DownloadLog {
    private val SEEN = stringSetPreferencesKey("download-seen")

    /** Trenta giorni in millisecondi: vedi il perché in testa. */
    private const val KEEP_MS = 30L * 24 * 60 * 60 * 1000

    /** Quante voci al massimo: vedi il perché in testa. */
    private const val KEEP_MAX = 200

    /**
     * La firma di un file: il suffisso, minuscolo e senza punto (`.JPG` e `jpg` sono lo stesso
     * formato), e i byte.
     */
    fun mark(suffix: String, bytes: Long): String =
        "${suffix.removePrefix(".").lowercase()}:$bytes"

    /** Se quella firma è già passata di qui. */
    suspend fun seen(context: Context, mark: String): Boolean =
        context.aivStore.data.first()[SEEN].orEmpty().any { it.substringBeforeLast('@') == mark }

    /**
     * Prende nota di un salvataggio riuscito.
     *
     * @param now l'istante da scrivere accanto alla firma: arriva da fuori, così la funzione si
     * misura
     *   senza aspettare un mese.
     */
    suspend fun note(context: Context, mark: String, now: Long) {
        context.writeStore { p ->
            val vive = prune(p[SEEN].orEmpty(), now)
            p[SEEN] = vive + "$mark@$now"
        }
    }

    /** Scorda tutto: serve a chi vuole ricominciare, e alle prove. */
    suspend fun clear(context: Context) {
        context.writeStore { p -> p.remove(SEEN) }
    }

    /**
     * Le voci che restano: degli ultimi trenta giorni, al massimo duecento. Le più nuove per prime,
     * e si taglia la coda; una voce senza istante si butta, o non scadrebbe mai.
     */
    internal fun prune(seen: Set<String>, now: Long): Set<String> = seen
        .mapNotNull { riga ->
            val quando = riga.substringAfterLast('@', "").toLongOrNull() ?: return@mapNotNull null
            if (now - quando > KEEP_MS) null else quando to riga
        }
        .sortedByDescending { it.first }
        .take(KEEP_MAX - 1)
        .map { it.second }
        .toSet()
}

/**
 * Che colore ha l'intestazione di una cartella: l'archivio della scelta fatta col tocco lungo
 * (richiesta dell'utente). È qui accanto a [DownloadLog] e non in [Settings]: è un elenco che
 * cresce col numero di cartelle, e una data class di preferenze non è il posto di una mappa.
 *
 * ⚠️ **Una cartella cancellata non si rincorre** (istruzione dell'utente): nessuna potatura. Non è
 * nemmeno una perdita: il `BUCKET_ID` è il CRC del percorso in minuscolo, quindi una cartella
 * ricreata con lo stesso nome nello stesso posto ritrova il suo colore. La chiave è il `BUCKET_ID`
 * e non il nome, che cambia con una rinomina ed è uguale in posti diversi.
 */
object FolderTints {
    private val TINTS = stringSetPreferencesKey("folder-tints")

    /** La tinta di una cartella, o `null` se non ne ha una. */
    suspend fun of(context: Context, bucket: Long): Int? =
        read(context.aivStore.data.first()[TINTS].orEmpty())[bucket]

    /**
     * Le tinte di tutte le cartelle segnate, per la schermata iniziale: chiederle una per cella con
     * [of] rileggerebbe l'archivio intero per ogni cella. Una cartella senza tinta non è nella
     * mappa.
     */
    suspend fun all(context: Context): Map<Long, Int> =
        read(context.aivStore.data.first()[TINTS].orEmpty())

    /**
     * Segna la tinta di una cartella, o la toglie con un [index] nullo. Riscrive l'insieme intero:
     * un `Set` di preferenze non ha un elemento da sostituire.
     */
    suspend fun set(context: Context, bucket: Long, index: Int?) {
        context.writeStore { p ->
            val ora = read(p[TINTS].orEmpty()).toMutableMap()
            if (index == null) ora.remove(bucket) else ora[bucket] = index
            p[TINTS] = ora.map { (dove, quale) -> "$dove=$quale" }.toSet()
        }
    }

    /**
     * Porta la tinta di [da] su [a]: la cartella è la stessa, rinominata da AIV. Il `BUCKET_ID`
     * dipende dal percorso, quindi dopo una rinomina il colore resterebbe su un id che non esiste
     * più. Una rinomina fatta fuori dall'app non si può seguire. Senza niente da spostare non
     * scrive, per non ricomporre chi legge l'archivio.
     */
    suspend fun move(context: Context, da: Long, a: Long) {
        if (da == a) return
        context.writeStore { p ->
            val ora = read(p[TINTS].orEmpty()).toMutableMap()
            val quale = ora.remove(da) ?: return@writeStore
            ora[a] = quale
            p[TINTS] = ora.map { (dove, indice) -> "$dove=$indice" }.toSet()
        }
    }

    /**
     * Le tinte di [qui] con quelle di [arrivo] aggiunte, e dove la cartella è la stessa vince
     * [arrivo]: per il backup, che le fonde (risposta `fonde` a `d-backup-importa`). La fusione è
     * qui perché il formato delle righe è di questo oggetto.
     * ⚠️ **Le righe passano come sono, anche quelle che non si leggono**: di una riga di qui si
     * guarda solo la cartella, e se ne va solo se il file ne contiene una per la stessa. Rilette e
     * riscritte, le righe illeggibili sparirebbero, e importare cambierebbe una cosa che il file
     * non nomina (presidiato dal caso 1 di `BackupTest`).
     */
    internal fun merged(qui: Set<String>, arrivo: Set<String>): Set<String> {
        val coperte = read(arrivo).keys
        return arrivo + qui.filter { riga -> riga.substringBefore('=').toLongOrNull() !in coperte }
    }

    /**
     * Le voci lette dall'archivio. Una riga illeggibile si butta senza chiudere l'app: l'archivio è
     * un insieme di stringhe, e una cartella senza colore è un esito accettabile.
     */
    private fun read(righe: Set<String>): Map<Long, Int> = righe.mapNotNull { riga ->
        val dove = riga.substringBefore('=').toLongOrNull() ?: return@mapNotNull null
        val quale = riga.substringAfter('=', "").toIntOrNull() ?: return@mapNotNull null
        dove to quale
    }.toMap()
}

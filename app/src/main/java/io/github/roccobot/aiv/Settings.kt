package io.github.roccobot.aiv

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/*
 * The settings, and which ones exist.
 *
 * The list is a subset of the ones 'Decent Image Viewer' exposes, plus three that
 * only make sense here. Choosing the subset is the whole design of this file, so
 * the reasons are written down rather than left to be guessed:
 *
 * - The **language** is deliberately absent. Android picks it from the system and
 *   the strings live in values/ and values-it/: a switch inside the app would be
 *   a second mechanism doing what the platform already does, and the two would
 *   disagree the first time one of them was touched.
 * - Everything about the **wheel** is gone, and it is most of the userscript's
 *   panel: direction, step, mode, sensitivity. There is no wheel on a phone, and
 *   pinch is handled by the system with its own tuning.
 * - The **SVG export** settings (two DPI values) still have nothing to act on, and
 *   from 1.31 the reason is a narrower one than the note used to give: the viewer
 *   does read SVG now (see [Svg]), but those two values are about *writing* one,
 *   which nothing here does. ⚠️ They are however the obvious home for the raster
 *   size of an incoming SVG, today the constant [Svg.BOX]: if sharper zoom on a
 *   vector is ever asked for, this is where the number would move.
 * - The **text nudge** is desktop typography, measured on a font this app never
 *   loads.
 *
 * What stays is what a finger can reach and a phone can change.
 */

/** One picture the app has opened before, as the opening screen lists it. */
data class RecentImage(val address: String, val name: String)

/**
 * What every stored choice has: a token that outlives a rename.
 *
 * ⚠️ The stored value is the token and NOT the enum constant's name. The two look
 * identical today and part company the day a constant is renamed: with the name,
 * every phone that had saved a value would silently fall back to the default, and
 * the setting would look like it had never been touched.
 */
interface Choice {
    val token: String
}

/** How the space behind the picture is painted. */
enum class BgType(override val token: String) : Choice {
    /** The checkerboard, which is what makes transparency visible. */
    CHECKER("checker"),
    /** One flat colour, for looking at a photograph without a pattern under it. */
    SOLID("solid")
}

/**
 * Which pair of greys the background uses. Separate from [BgType] on purpose:
 * they are independent axes, and merging them would force a choice between
 * transparency and colour, losing the light checkerboard entirely. The userscript
 * made that mistake once and had to undo it.
 */
enum class BgTheme(override val token: String) : Choice { AUTO("auto"), LIGHT("light"), DARK("dark") }

/**
 * What '100%' means, and on a phone the two answers are far apart.
 *
 * A phone screen has two to three device pixels per layout pixel, so an image
 * shown at one image pixel per DEVICE pixel is a third of the size it would be at
 * one image pixel per LAYOUT pixel. Neither is wrong: [PHYSICAL] shows the file
 * as it really is, [LOGICAL] shows it the size a web page would.
 */
enum class ScaleMode(override val token: String) : Choice { PHYSICAL("physical"), LOGICAL("logical") }

/**
 * Da che parte stanno le funzioni usate più spesso, nel pannello della selezione.
 *
 * ⚠️⚠️ **NON È UN'IMPOSTAZIONE DI ESTETICA MA DI POLLICE** (richiesta dell'utente,
 * 2026-08-31): le dieci azioni sono in fila, e chi tiene il telefono con la destra arriva
 * comodo solo all'ultimo terzo. Rovesciando le due file, le stesse funzioni finiscono sotto
 * lo stesso dito dell'altra mano.
 * ⚠️ **Rovescia le FILE e non l'ordine dell'elenco**: la prima fila resta la prima, cambia
 * solo da che parte comincia. Girando l'elenco intero, 'Copia' finirebbe nella seconda fila.
 *
 * ⚠️⚠️ **L'ORDINE DI DICHIARAZIONE È QUELLO DEI DUE TASTI, e la sinistra viene prima anche
 * se il valore di fabbrica è la destra** (richiesta dell'utente, 2026-08-31: *anche se
 * 'Destra' è predefinita, deve essere al secondo posto, in modo che 'Sinistra' sia a sinistra
 * e 'Destra' a destra*). Questa scelta parla di **lati**, quindi i due tasti stanno dove
 * stanno le mani: metterli in ordine di frequenza costringerebbe a leggere l'etichetta per
 * capire quale si sta toccando.
 * ⚠️ **Il valore predefinito NON dipende da quest'ordine e non è cambiato**: sta scritto due
 * volte, nel valore di serie di `Settings.hand` e nel ripiego della rilettura, ed è `RIGHT`
 * in tutti e due. ⚠️ E la memoria non si rompe: sul disco va il **token**, non la posizione
 * (vedi `byToken`), quindi un telefono aggiornato ritrova la scelta di ieri.
 */
enum class Hand(override val token: String) : Choice { LEFT("left"), RIGHT("right") }

/**
 * Dopo quanto un file eliminato se ne va dal cestino da solo.
 *
 * ⚠️⚠️ **DURATE CON UN NOME E NON UN NUMERO DI GIORNI, ed è una scelta di traduzione**: '7
 * giorni' vuole un plurale, e le categorie di plurale non coincidono fra le ventotto lingue
 * (in arabo 7 e 14 ne chiedono due diverse). 'Una settimana' e 'Un mese' sono frasi fisse:
 * si traducono una volta e non hanno un caso in cui sbagliano.
 * ⚠️ **[NEVER] è il valore di fabbrica**, e vale zero giorni: lo svuotamento automatico è
 * spento finché non lo si accende, perché cancella file per sempre.
 */
enum class BinKeep(override val token: String, val days: Int) : Choice {
    NEVER("never", 0),
    WEEK("week", 7),
    MONTH("month", 30),
    QUARTER("quarter", 90)
}

/**
 * Quanto è grande il testo nella vista a **elenco**.
 *
 * ⚠️ Vale per il solo elenco e non per tutta l'app: là ogni riga è nome più conto, e chi
 * sceglie l'elenco lo sceglie per **leggere**, quindi il corpo è parte della vista come le
 * colonne lo sono della griglia. Il corpo di sistema resta quello che decide tutto il
 * resto, e questa scelta ci si moltiplica sopra invece di sostituirlo.
 */
enum class TextSize(override val token: String) : Choice {
    SMALL("small"), NORMAL("normal"), LARGE("large")
}

/**
 * Come si chiama una delle tre misure.
 *
 * ⚠️ **Sta accanto all'enum e non nella schermata che la mostra, dalla `1.46`**: adesso i tre
 * nomi servono a due superfici (la scorciatoia del dialogo e la pagina delle impostazioni), e
 * un `when` privato in una delle due avrebbe costretto l'altra a riscriverlo.
 */
@StringRes
fun TextSize.label(): Int = when (this) {
    TextSize.SMALL -> R.string.text_small
    TextSize.NORMAL -> R.string.text_normal
    TextSize.LARGE -> R.string.text_large
}

/** Where the one line of details sits. Asked for by the user. */
enum class InfoPosition(override val token: String) : Choice { TOP("top"), BOTTOM("bottom") }

/**
 * Come si guarda quello che c'è sul telefono: le tre viste della casa.
 *
 * ⚠️⚠️ **L'ELENCO È TORNATO A ESSERE UNA SCELTA, e non una versione superata.** La `0.37`
 * lo aveva **sostituito** con le copertine, che si riconoscono a colpo d'occhio ma
 * mostrano quattro cartelle per schermata; l'elenco ne mostra una decina e i nomi per
 * intero, che su un telefono con trenta cartelle è l'unico modo di trovarne una per nome.
 * Sono due domande diverse ('quale cartella' e 'quale nome'), quindi due viste e non una
 * migliore dell'altra.
 * ⚠️ **La copertina resta in tutti e due**: la riga dell'elenco porta la stessa miniatura
 * piccola, perché l'icona di cartella uguale per tutte era proprio il difetto della `0.29`.
 *
 * ⚠️⚠️ **LA TERZA È ARRIVATA CON LA `0.84`, ed è di un'altra natura**: [GRID] e [LIST] sono
 * due rese dello **stesso** elenco, le cartelle che il MediaStore conosce; [TREE] è la
 * memoria del telefono navigata cartella per cartella, letta dal disco (vedi `Tree`). Sta
 * accanto alle altre due perché rispondono tutte a 'dove sono le mie cose', ma sotto non
 * condividono niente.
 */
enum class FolderView(override val token: String) : Choice {
    GRID("grid"), LIST("list"), TREE("tree")
}

/**
 * Il tema dell'interfaccia, chiesto dall'utente il 2026-08-29.
 *
 * ⚠️⚠️ **NON È LO STESSO DI [BgTheme], e confonderli è l'errore facile**: quello dice di
 * che grigi è fatto lo **sfondo dietro la fotografia**, questo dice se l'**app** è chiara
 * o scura. Sono davvero due domande: chi guarda foto al buio può volere l'app scura e la
 * scacchiera chiara, perché la scacchiera serve a far vedere la trasparenza e non a
 * intonarsi.
 * ⚠️ Fino alla `0.44` la seconda domanda non esisteva e vinceva sempre il sistema; da qui
 * il valore di fabbrica resta **[SYSTEM]**, cioè il comportamento di prima, e le altre due
 * sono una scelta esplicita.
 */
enum class UiTheme(override val token: String) : Choice {
    SYSTEM("system"), LIGHT("light"), DARK("dark")
}

/**
 * Quante colonne mostra la griglia delle CARTELLE.
 *
 * ⚠️ **Le cartelle e non le fotografie**, ed è la lettura letterale della richiesta: la
 * domanda a cui l'utente rispondeva era se due colonne fossero la densità giusta per le
 * **copertine**. La griglia delle fotografie resta adattiva sui suoi 108dp, che sono una
 * misura confermata.
 * ⚠️ **Fisso e non più adattivo**, per la sola griglia delle cartelle: il numero lo sceglie
 * l'utente, quindi non può dipendere dalla larghezza dello schermo. Il costo dichiarato: a
 * 4 colonne su un telefono stretto la copertina scende sotto i 90dp e si riconosce meno,
 * che è esattamente la ragione per cui l'adattivo esisteva.
 */
val FOLDER_COLUMNS = listOf(2, 3, 4)

// ⚠️ QUI VIVEVA `SearchEngine`, il motore della ricerca immagine, tolto nella
// 0.18 insieme alla funzione (istruzione dell'utente dopo la prova sul telefono).
// La chiave `search-engine` può essere rimasta scritta nell'archivio dei telefoni
// che avevano la versione vecchia: non si legge più, e non è un difetto da
// inseguire, perché DataStore ignora le chiavi che nessuno chiede.

data class Settings(
    val bgType: BgType = BgType.CHECKER,
    val bgTheme: BgTheme = BgTheme.AUTO,
    /** Whether a picture smaller than the view grows to fill it. Off, as in the userscript: blowing up a 64px icon helps nobody. */
    val fitGrow: Boolean = false,
    val zoomMax: Float = 40f,
    val scaleMode: ScaleMode = ScaleMode.PHYSICAL,
    /**
     * Dove sta la barra delle info.
     *
     * ⚠️ **In alto di serie dalla 1.20** (scelta dell'utente, 2026-09-01): là non finisce
     * sotto il pollice di chi sfoglia, e non litiga con la fila dei comandi delle immagini
     * animate, che sta in basso al centro.
     */
    val infoPosition: InfoPosition = InfoPosition.TOP,
    val infoVisible: Boolean = true,
    /**
     * Se dietro dialoghi, menu e pannelli ci sono sfocatura e velo.
     *
     * ⚠️⚠️ **SPENTA DI FABBRICA PERCHÉ COSTA, e a dirlo è stato l'utente** (2026-09-03:
     * *mettilo dietro un'opzione disattivata di default. Penserò se tenere o meno la feature:
     * rende tutto visibilmente più lento*). La funzione è nata accesa nella `1.38` ed è durata
     * una versione: la sfocatura fra finestre fa ridisegnare al compositore quello che sta
     * sotto, e su un telefono che non la regge si sente.
     * ⚠️ **L'impostazione resta invece di essere una rimozione**, che sarebbe stata la strada
     * più corta: lui ha detto che ci deve pensare, quindi la funzione va tenuta provabile.
     * ⚠️ **Spenta NON vuol dire schermate diverse dalla `1.37`**: i dialoghi tornano al velo
     * che Android dà loro, i menu a non averne, e la sola scheda in fondo se lo chiede da sé
     * (vedi `SHEET_DIM` in `Sheet.kt`), perché la sua finestra non ne ha uno di serie.
     */
    val veil: Boolean = false,
    /**
     * Se il menu a pressione lunga porta anche 'Adatta alla vista' e '100%'.
     *
     * ⚠️⚠️ **SPENTA DI FABBRICA, ed è una RIMOZIONE travestita da impostazione** (richiesta
     * dell'utente, 2026-09-02: *aggiungi 'Mostra zoom nel menu contestuale del visualizzatore'
     * nelle impostazioni (default: spento); come conseguenza, togli '100%' e 'Adatta alla
     * vista' dal menu a pressione lunga*). Quelle due voci fanno quello che il **doppio tocco**
     * fa già, e stanno in mezzo a comandi che agiscono sul file: chi apre quel menu cerca di
     * solito un'altra cosa.
     * ⚠️ **L'interruttore esiste lo stesso perché la scorciatoia è nascosta**: un gesto non si
     * dichiara da sé, e chi non lo conosce resterebbe senza. Da qui l'onboarding del doppio
     * tocco (vedi `Hint.ZOOM_TAP`), che è la vera contropartita della rimozione.
     */
    val zoomInMenu: Boolean = false,
    /**
     * Se la sequenza si sfoglia in ordine **cronologico** invece che dalla più recente.
     *
     * ⚠️⚠️ **IL SIGNIFICATO SI È ROVESCIATO NELLA 0.30, e per questo la chiave è NUOVA**
     * (`sequence-reversed`): dalla 0.27 alla 0.29 il verso predefinito era quello del
     * MediaStore, cronologico, e questa voce accesa dava il verso della galleria; adesso
     * il verso della galleria è il predefinito (decisione dell'utente: *è la cosa più
     * naturale su smartphone*) e questa voce accesa riporta al cronologico. Tenere la
     * chiave vecchia avrebbe dato a chi l'aveva accesa **l'opposto** di quello che aveva
     * scelto, in silenzio: è la stessa lezione della chiave del permesso nella 0.21, cioè
     * che **una domanda nuova vuole una chiave nuova**.
     * ⚠️ La chiave vecchia (`reverse-order`) resta scritta negli archivi e non si legge
     * più: DataStore ignora le chiavi che nessuno chiede.
     */
    val reverseSequence: Boolean = false,
    /**
     * ⚠️ Non compare nella schermata delle impostazioni, e non è una dimenticanza: si
     * cambia dal menu della schermata delle cartelle, cioè dal posto in cui si vede
     * l'effetto. Una vista si sceglie guardandola, non leggendo un interruttore due
     * schermate più in là.
     */
    val folderView: FolderView = FolderView.GRID,
    /**
     * Le quattro opzioni delle due viste che non sono la griglia, dalla `0.99`.
     *
     * ⚠️ **Stanno nelle impostazioni pur vivendo in un popup**, come le colonne: il popup è
     * una **scorciatoia** a una scelta che deve sopravvivere alla chiusura dell'app, non
     * uno stato di schermata. Il filtro dei generi, che invece è volatile per richiesta,
     * infatti non è qui.
     */
    val listCount: Boolean = true,
    val listText: TextSize = TextSize.NORMAL,
    /** Se la vista ad albero mostra anche i file che cominciano per punto. */
    val treeHidden: Boolean = false,
    /** Se la vista ad albero nasconde le cartelle che non portano a nessuna immagine. */
    val treePictures: Boolean = false,
    /** Da che parte stanno le funzioni principali del pannello. Vedi [Hand]. */
    val hand: Hand = Hand.RIGHT,
    /**
     * Se 'Copia lista' mette anche il percorso della cartella, in testa ai nomi.
     *
     * ⚠️ Spenta di fabbrica, come chiesto: una lista di nomi si incolla dove si vuole,
     * mentre un percorso di sistema in testa è utile a chi sa che cosa farsene.
     */
    val listPath: Boolean = false,
    /**
     * Se in testa alla selezione si legge anche il **peso** di quello che si è scelto.
     *
     * ⚠️⚠️ **L'INTERRUTTORE ESISTE PERCHÉ IL PESO COSTA, dalla 1.06** (riscontro dell'utente
     * sul collaudo: *meglio renderla attivabile/disattivabile dalle impostazioni*): quel
     * numero non sta nell'indirizzo, quindi ogni selezione ferma è una interrogazione al
     * MediaStore su tutti gli elementi scelti. Chi non lo guarda mai la paga lo stesso.
     * ⚠️ **Governa il solo peso e non il conto**, che è il titolo della selezione: senza il
     * conto la testata resterebbe vuota, e l'impostazione diventerebbe un modo di rompere la
     * schermata invece di alleggerirla.
     * ⚠️ **Accesa di fabbrica**: è la funzione così com'era fino alla `1.05`, e spegnerla di
     * default vorrebbe dire toglierla a chi non sa che esiste.
     */
    val pickWeight: Boolean = true,
    /**
     * Chi apre una fotografia quando si tocca 'Modifica', dalla `1.03`.
     *
     * ⚠️ **Vuoto vuol dire 'mai scelto', ed è diverso da 'nessuno'**: la prima volta il menu
     * chiede, e da lì in poi non chiede più. Senza la distinzione, o si chiederebbe a ogni
     * modifica o non si chiederebbe mai.
     * ⚠️ **Il valore è un componente appiattito** (`pacchetto/classe`), o `Editors.INTERNAL`
     * per l'editor di casa. Vedi `Editors`.
     */
    val editorApp: String = "",

    /**
     * Se prima di sovrascrivere una fotografia se ne mette una copia nel cestino.
     *
     * ⚠️⚠️ **ACCESA DI FABBRICA, e la ragione è che il danno è ASIMMETRICO** (richiesta
     * dell'utente, 2026-08-31): spenta, un ritaglio sbagliato si porta via l'originale e non
     * c'è nessuna via di ritorno; accesa, il prezzo è un file in piu nel cestino, che si
     * svuota con un tocco. Fra i due, il valore di serie deve essere quello che perdona.
     * ⚠️ **Non dipende da [binOn]**: il cestino si raggiunge dal tastino di casa anche quando
     * l'eliminazione non ci passa, quindi la copia resta recuperabile in tutti e due i casi.
     */
    val editorBackup: Boolean = true,
    /**
     * Se la fila dei comandi di un'immagine animata mostra il contatore dei fotogrammi.
     *
     * ⚠️⚠️ **ACCESO DI FABBRICA, ed è l'utente ad averlo chiesto insieme all'interruttore**
     * (2026-09-01: *contatore: sì, lo voglio, ma mettiamo un on/off*). L'interruttore non
     * nasce da un dubbio sul contatore: nasce dal fatto che un numero sopra l'immagine è
     * l'unica parte della fila che non è un comando, quindi è la sola che qualcuno possa
     * voler togliere per guardare e basta.
     */
    val animCounter: Boolean = true,
    /**
     * Se all'avvio si guarda negli appunti.
     *
     * ⚠️⚠️ **SPENTA DI DEFAULT, per volontà dell'utente dopo averla provata** (2026-08-29:
     * *niente avviso, opzione spenta di default*). Il motivo è il prezzo che si paga: da
     * Android 12 il sistema **annuncia** ogni lettura degli appunti, quindi una lettura a
     * ogni avvio è un avvisino a ogni avvio, anche quando negli appunti non c'è niente di
     * utile. Accesa, l'app fa quello che era stato chiesto; spenta, l'avviso non compare
     * mai. La scelta è di chi la usa e non nostra, ed è la ragione per cui è
     * un'impostazione e non un comportamento.
     */
    val clipboardStart: Boolean = false,
    /**
     * L'ultimo indirizzo degli appunti che l'app ha aperto da sé.
     *
     * ⚠️⚠️ **NON È UNA PREFERENZA MA UNO STATO, e vive qui per una ragione precisa**: la
     * decisione di aprire o no si prende dentro [ViewerViewModel.readClipboard], che deve
     * rispondere **subito** per non perdere la corsa con la cartella d'avvio (vedi la nota
     * là). Un archivio a parte si leggerebbe con una sua coroutine, e quando la risposta
     * arrivasse la fotografia sarebbe già stata aperta o già coperta. Le impostazioni
     * invece quella funzione le aspetta già, quindi qui il valore c'è per costruzione.
     * ⚠️ Vuoto vuol dire 'nessuno ancora', ed è diverso da 'non lo so'.
     */
    val clipboardDone: String = "",
    /**
     * Quando gli appunti portavano l'indirizzo che l'app ha aperto da sé.
     *
     * ⚠️⚠️ **NASCE PERCHÉ RICOPIARE LO STESSO INDIRIZZO NON RIAPRIVA NIENTE, dalla 1.45**
     * (segnalazione dell'utente, 2026-09-03, sulla GIF di Wikimedia: *se l'URL è negli
     * appunti, non si apre all'avvio*). La memoria della `0.94` teneva il solo indirizzo, e la
     * sua nota diceva la cosa giusta a metà: *copiando un indirizzo nuovo la funzione deve
     * tornare a rispondere*. Ma copiare **di nuovo** lo stesso indirizzo è una richiesta
     * altrettanto chiara, e con il solo confronto delle stringhe era indistinguibile da 'quel
     * link sta negli appunti da tre giorni'.
     * ⚠️ **Il tempo lo dà il sistema e non lo misuriamo noi**: `ClipDescription.getTimestamp`
     * dice quando quel contenuto è stato messo negli appunti (da Android 8, e il `minSdk` è
     * 28). Un tempo nostro direbbe solo quando lo abbiamo letto, che è un'altra cosa.
     * ⚠️ Zero vuol dire 'nessuno ancora', come la stringa vuota accanto.
     */
    val clipboardWhen: Long = 0L,
    /**
     * La cartella da aprire all'avvio, e `null` quando non se n'è scelta nessuna.
     *
     * ⚠️ **Due campi per una cosa sola, e il secondo non è ridondante**: l'id è quello
     * che apre, il nome è quello che si legge nelle impostazioni. Senza il nome la voce
     * direbbe un numero, e per tradurlo bisognerebbe interrogare il MediaStore ogni
     * volta che si apre quella schermata, permesso compreso.
     */
    val startFolder: Long? = null,
    val startFolderName: String = "",
    /**
     * Se all'avvio si apre la foto più recente di [startFolder] invece della schermata
     * iniziale.
     *
     * ⚠️ Separato dalla cartella perché sono due decisioni: 'quale cartella' resta
     * scritta anche quando si spegne l'avvio automatico, e riaccenderlo non costa
     * riscegliere. Chi lo accende senza aver scelto viene portato all'elenco.
     */
    val openAtStart: Boolean = false,
    /** Chiaro, scuro o come il sistema. Vedi [UiTheme]: non è il tema dello sfondo. */
    val uiTheme: UiTheme = UiTheme.SYSTEM,
    /** Quante colonne ha la griglia delle cartelle. Vedi [FOLDER_COLUMNS]. */
    val folderColumns: Int = 2,
    /**
     * Se sotto la copertina di una cartella si vede il conto delle immagini.
     *
     * ⚠️⚠️ **SPEGNENDOLA la riga di griglia si ACCORCIA, e il frontespizio cresce di
     * altrettanto**: non resta uno spazio vuoto al suo posto. È la lettura di 'opzionale'
     * che dà qualcosa in cambio, e tiene esatto il conto delle righe visibili
     * (`coverHeader`), che è quello che l'utente ha chiesto al punto 1a.
     * ⚠️ **Accesa di serie**: è un dato che si guarda, e togliere qualcosa a chi non l'ha
     * chiesto è il modo di far cercare un'impostazione che non si sa di avere.
     */
    val folderCount: Boolean = true,
    /**
     * Se l'eliminazione manda le fotografie nel **cestino** invece di cancellarle.
     *
     * ⚠️⚠️ **DECIDE ANCHE SE C'È UNA CONFERMA, e i due fatti sono uno** (testo dettato
     * dall'utente, dalla `0.79`): col cestino acceso eliminare **non chiede niente**, perché
     * l'azione si disfa con un 'Ripristina'; spegnendolo l'eliminazione diventa istantanea e
     * definitiva, quindi **prima** di ogni eliminazione compare una conferma. La regola che
     * ne esce è una sola: si chiede conferma **quando e solo quando** non si può tornare
     * indietro.
     * ⚠️ **Accesa di serie**: un'app che cancella per sempre al primo tocco sbagliato non è
     * quello che si attende da una galleria, e chi vuole l'eliminazione secca la sceglie.
     * ⚠️ **Spegnerla NON svuota il cestino** e non lo rende irraggiungibile: quello che c'è
     * dentro resta, e la sua voce nel menu del tastino pure. Sarebbe la sorpresa peggiore.
     */
    val binOn: Boolean = true,
    /**
     * Se sfogliando nel visualizzatore si saltano i filmati.
     *
     * ⚠️⚠️ **SPENTA DI FABBRICA, ed è quello che decide l'architettura** (parole dell'utente,
     * 2026-08-31: *dev'essere OFF di default, perciò sì, serve per forza il ramo decisionale
     * e un secondo visualizzatore*). Spenta vuol dire che il caso normale è 'sfoglio anche i
     * video', quindi il ramo che sceglie fra fotografia e filmato serve **sempre**, e non si
     * poteva rimandare accendendo l'opzione per finta. Quel ramo esiste dalla `0.83`.
     * ⚠️⚠️ **TOCCA IL SOLO SFOGLIO, e non il tocco**: da una griglia un filmato si apre
     * comunque, perché toccarlo è una richiesta esplicita. Questa spegne il **gesto**, cioè
     * il caso in cui il filmato arriva senza che nessuno l'abbia chiesto.
     * ⚠️ **Il contatore mostra dei salti, ed è voluto**: la serie resta quella della
     * cartella, quindi si passa da `3/10` a `5/10`. L'alternativa era rinumerare, cioè
     * mentire su quante cose ci sono nella cartella.
     */
    val imagesOnly: Boolean = false,
    /**
     * Se un filmato raggiunto **con un tocco** parte da sé, dalla `1.46`.
     *
     * ⚠️⚠️ **È IL ROVESCIO ESATTO DI [imagesOnly], E LE DUE NON SI SOVRAPPONGONO**: quella
     * spegne il **gesto**, questa accende il **tocco** (richiesta dell'utente, 2026-09-03: *al
     * tocco sulla miniatura di un video parte subito la riproduzione. Quando si sfoglia,
     * invece, i video sono sempre in modalità con 'Play' in sovrimpressione*). Sfogliando non
     * parte niente **nemmeno con questa accesa**, e non è una dimenticanza: è la seconda metà
     * della richiesta, e la regola vive in un punto solo, `Arrival.plays`.
     * ⚠️ **Spenta di fabbrica**: un audio che comincia da sé è una sorpresa, e chiede il fuoco
     * audio mettendo in pausa la musica di un'altra app. Chi la vuole paga un tocco; chi non la
     * conosce non si trova l'app che parla.
     */
    val clipAutoplay: Boolean = false,
    /**
     * Se sotto ogni miniatura della griglia delle foto si legge il nome del file.
     *
     * ⚠️ **Spenta di fabbrica** (richiesta dell'utente): una galleria mostra fotografie, e un
     * nome sotto ognuna è un dato che serve a chi lavora coi file, non a chi guarda.
     * ⚠️ **Accenderla costa una domanda al MediaStore per fotografia**, una volta sola per
     * indirizzo: il nome di un `content://` non si ricava dall'indirizzo. Vedi [Names].
     */
    val gridNames: Boolean = false,
    /**
     * Se la finestra di rinomina porta il tasto che cambia l'**estensione**.
     *
     * ⚠️⚠️ **SPENTA DI FABBRICA, ED È UNA GRIGLIA DI SICUREZZA CHIESTA DALL'UTENTE**
     * (2026-09-02: *per far sì che la funzionalità sia usata solo da chi sa cosa sta facendo,
     * la disattiviamo di default*). Cambiare l'estensione **non converte niente**: lascia
     * dentro un JPEG con l'etichetta di un PNG, e su un'estensione non multimediale il file
     * sparisce dalle viste a griglia e a lista, che è il modo peggiore di perdere una
     * fotografia, perché sembra cancellata.
     * ⚠️ **Ma la funzione serve, e per questo non è stata tolta**: il caso vero è il suo
     * (*a volte mi capita di dover rinominare un .svg in .txt perché l'app Claude non è in
     * grado di allegare file .svg in chat*), cioè aggirare il filtro di un'altra app.
     * ⚠️ **Tre presidi e non uno**: questo interruttore, il paragrafo che sta sotto di lui
     * nelle impostazioni, e il velo che compare la **prima volta** che si apre quel
     * pannellino (vedi `Hint.EXT_EDIT`). Il primo tiene fuori chi non la cerca, il terzo
     * avvisa chi la cerca senza sapere che cosa comporta.
     */
    val extEdit: Boolean = false,
    /**
     * Se le miniature vivono nella **memoria grafica** invece che in quella dell'app.
     *
     * ⚠️⚠️ **RICHIESTA DELL'UTENTE, e autorizzata in chiaro due volte** (giro della `1.67`:
     * *PROCEDI*; giro della `1.74`: *aggiungi l'opzione come stabilito*). Il guadagno vero non
     * è la velocità di disegno ma il **budget**: `ImageSource.pixelBudget` misura la memoria
     * che resta all'app per decidere se campionare la fotografia grande, e ogni miniatura
     * tenuta in caldo da Coil gliela toglie. In memoria grafica quelle non pesano più sul
     * mucchio dell'app, quindi il visualizzatore ha più spazio proprio dove la qualità si
     * vede.
     * ⚠️⚠️ **SPENTA DI FABBRICA, E STA FRA LE 'FUNZIONALITÀ AVANZATE' PER UNA RAGIONE
     * MISURABILE**: un bitmap in memoria grafica costa un **descrittore di file** al processo,
     * e il tetto di Android è dell'ordine del migliaio. Una cache di miniature che cresce
     * senza freno li esaurisce, e quando succede l'app non rallenta: va in errore. È
     * esattamente il genere di funzione per cui quel gruppo è nato.
     * ⚠️⚠️ **SPENTA VUOL DIRE SOFTWARE DAPPERTUTTO, e questo cambia una cosa rispetto alla
     * `1.74`**: fino a lì la decodifica di ripiego di Coil poteva già dare bitmap in memoria
     * grafica, perché è il suo valore di serie, mentre le miniature del sistema erano software.
     * Erano due comportamenti diversi per due strade, e nessuno dei due era stato scelto.
     * Adesso a decidere è questo interruttore, e da spento tutte e due le strade sono software.
     * Un interruttore che governa metà di quello che il suo titolo dice è peggio di nessun
     * interruttore.
     * ⚠️ **Non tocca il visualizzatore**: là la decodifica è quella di `ImageSource`, che deve
     * poter **rileggere** i pixel (esportazione, editor, copia negli appunti) e un bitmap in
     * memoria grafica non si rilegge. Questo interruttore parla delle sole miniature.
     */
    val gpuThumbs: Boolean = false,
    /**
     * Dopo quanto un file eliminato se ne va dal cestino da solo.
     *
     * ⚠️⚠️ **LE TRE DECISIONI CHE LA GOVERNANO SONO SUE, e si citano con la loro chiave perché
     * si possano riverificare** (giro della 1.67): `d-cestino-quando`: **`file`**, cioè ogni
     * file conta la propria età e non si svuota tutto a intervalli; `d-cestino-chiusa`:
     * **`aperta`**, cioè la pulizia gira **solo mentre l'app è in primo piano**;
     * `d-cestino-editor`: **`fuori`**, cioè le copie di sicurezza dell'editor non scadono mai.
     * ⚠️⚠️ **LA SECONDA ERA STATA REGISTRATA AL CONTRARIO nel brief, e l'ha dovuto segnalare
     * lui** (2026-09-06: *nell'apposito artefatto ti avevo già risposto la stessa cosa e tu hai
     * memorizzato il contrario*). Da quell'inversione era nata l'idea che servisse
     * un'operazione programmata di sistema, e con lei una libreria in più: non serve niente.
     * La regola che ne è uscita sta in `Roccobot.md` § '🔑 Una risposta si travasa con la sua
     * chiave, e si rilegge dalla fonte'.
     * ⚠️ **Spenta di fabbrica**, perché cancella file per sempre.
     */
    val binKeep: BinKeep = BinKeep.NEVER,
    /**
     * I percorsi delle cartelle che non devono comparire fra quelle da sfogliare.
     *
     * ⚠️⚠️ **PERCORSI E NON IDENTIFICATIVI, ed è la scelta che regge la funzione**
     * (richiesta dell'utente, 2026-08-29: *percorsi che non devono apparire*). L'id di
     * una cartella nel MediaStore **non è stabile**: si ricalcola dal percorso, quindi
     * cambia quando la cartella viene rinominata o spostata, e su un archivio
     * riscansionato può cambiare da solo. Un elenco di id smetterebbe di nascondere
     * proprio le cartelle che qualcuno ha toccato, cioè in silenzio.
     * ⚠️ **Nasconde anche quello che sta SOTTO**: escludere `/storage/emulated/0/Foo`
     * toglie anche `Foo/Bar`, perché chi esclude un percorso esclude un ramo. Il
     * confronto è sul separatore, o `/Foo2` verrebbe nascosto insieme a `/Foo`.
     * ⚠️ **Nascondere non cancella niente**: le foto restano dove sono, e la cartella si
     * rivede dalle impostazioni. Vale la pena dirlo perché una funzione che si chiama
     * 'escludi' accanto a una che cancellerà davvero è il posto giusto per un equivoco.
     */
    val hiddenFolders: Set<String> = emptySet(),
    /**
     * L'ordine dei campi delle informazioni sul file. Vedi [FactField].
     *
     * ⚠️ **Ci sono TUTTI, anche quelli spenti**, ed è la differenza fra un ordine e un
     * elenco: se qui vivessero solo i visibili, riaccenderne uno vorrebbe dire non sapere
     * più dove rimetterlo, e ricomparirebbe in fondo invece che al suo posto.
     */
    val factOrder: List<FactField> = FactField.entries,
    /**
     * I campi che l'utente ha spento.
     *
     * ⚠️ **L'insieme dice chi NON si vede, non chi si vede**: così un campo aggiunto da
     * una versione futura nasce **acceso**, che è il verso giusto. Al contrario nascerebbe
     * invisibile su ogni telefono che ha toccato l'impostazione una volta, cioè
     * introvabile.
     * ⚠️ I tre campi [FactField.always] non ci finiscono mai: lo garantisce la lettura,
     * non la buona volontà di chi scrive l'interfaccia.
     */
    val factOff: Set<FactField> = emptySet(),
    /**
     * L'ordine dei sei tasti nel riquadro del tocco lungo: visualizzatore e albero.
     *
     * ⚠️ **Di fabbrica è quello che ha dettato lui per la `1.54`**, e i due posti in cui vive
     * quel riquadro lo condividono: chi impara dove sta 'Sposta' lo sa in tutti e due.
     * ⚠️⚠️ **NEL CESTINO NON SI SCAMBIA PIÙ NIENTE, dalla `1.56`**: fino alla `1.55` là 'Sposta'
     * e 'Copia' si invertivano, per una richiesta della `1.54`. Con un ordine scelto dall'utente
     * quello scambio diventerebbe una **seconda trasformazione sopra la sua scelta**, cioè
     * l'app che disobbedisce proprio a chi ha appena riordinato. Una funzione che si può
     * configurare non può anche correggere la configurazione.
     */
    val menuOrder: List<PadKey> = MENU_KEYS,
    /**
     * L'ordine dei dieci tasti della scheda della selezione.
     *
     * ⚠️ **È un ordine a sé e non lo stesso di [menuOrder]**: le due superfici portano insiemi
     * diversi in ordini diversi, tutti e due dettati da lui. Il perché per esteso sta accanto
     * alle chiavi, in `Store`.
     */
    val pickOrder: List<PadKey> = PICK_KEYS,
    /** L'ordine della prima fila dell'editor: girare e centrare. */
    val turnOrder: List<PadKey> = TURN_KEYS,
    /** L'ordine della seconda fila dell'editor: la cronologia e la conferma. */
    val stepOrder: List<PadKey> = STEP_KEYS,
    /**
     * Se sotto le icone dei riquadri a griglia si legge la parola.
     *
     * ⚠️ **Accesa di fabbrica, perché è come l'app è sempre stata**: l'interruttore nasce per
     * chi vuole i comandi più compatti (richiesta dell'utente, 2026-09-04), non per cambiare
     * l'aspetto a chi non lo chiede. Il valore di fabbrica non si sceglie per far vedere la
     * funzione.
     * ⚠️ **Vale per i riquadri a GRIGLIA e non per i menu a lista**: quelli hanno l'icona
     * accanto al testo, e togliere il testo lascerebbe una riga con un glifo e un vuoto.
     */
    val padLabels: Boolean = true,
) {
    /** I campi da mostrare, nell'ordine scelto e senza quelli spenti. */
    val factRows: List<FactField> get() = factOrder.filterNot { it in factOff }
}

/**
 * Gli ordini di fabbrica dei quattro riquadri, che sono anche l'elenco di chi ci vive.
 *
 * ⚠️⚠️ **QUESTE LISTE FANNO DUE MESTIERI, e conviene saperlo prima di toccarle**: dicono in che
 * ordine stanno i tasti **e** quali tasti quel riquadro conosce. [padOrderOf] usa il secondo per
 * riempire i buchi di un archivio vecchio, quindi togliere una voce da qui non è un ritocco
 * all'ordine: è dire che quel tasto non esiste più in quel riquadro.
 * ⚠️ **L'ordine è quello dettato dall'utente per la `1.54`**, riga per riga, e non uno mio.
 */
val MENU_KEYS = listOf(
    PadKey.COPY, PadKey.MOVE, PadKey.SHARE,
    PadKey.RENAME, PadKey.DELETE, PadKey.INFO
)

/** I dieci della scheda della selezione, nell'ordine dettato per la `1.54`. */
val PICK_KEYS = listOf(
    PadKey.RENAME, PadKey.INFO, PadKey.MOVE, PadKey.COPY, PadKey.SHARE,
    PadKey.DELETE, PadKey.LIST, PadKey.ALL, PadKey.NONE, PadKey.INVERT
)

/**
 * I quattro della prima fila dell'editor.
 *
 * ⚠️ **L'ordine di fabbrica è quello della mano DESTRA**, che è il valore di fabbrica
 * dell'impostazione che fino alla `1.55` specchiava le due file. Chi teneva la sinistra
 * ritrova la sua fila rovesciata **una volta sola**, alla prima apertura dopo l'aggiornamento,
 * e da lì la mette come vuole trascinando.
 */
val TURN_KEYS = listOf(
    PadKey.CENTRE_DOWN, PadKey.CENTRE_ACROSS, PadKey.TURN_LEFT, PadKey.TURN_RIGHT
)

/** I quattro della seconda fila dell'editor, sempre nell'ordine della mano destra. */
val STEP_KEYS = listOf(
    PadKey.ORIGINAL, PadKey.UNDO, PadKey.REDO, PadKey.APPLY
)

/**
 * Rilegge un ordine salvato, tollerando tutto quello che un archivio può avere di storto.
 *
 * ⚠️⚠️ **QUELLO CHE MANCA SI AGGIUNGE AL POSTO CHE HA DI FABBRICA, e non in fondo**: è la
 * garanzia che una versione futura possa aggiungere un tasto a un riquadro senza che sparisca
 * dai telefoni di chi ha già riordinato. In fondo sarebbe la soluzione facile, e metterebbe
 * l'azione nuova nel posto in cui nessuno guarda.
 * ⚠️ **Un gettone che non appartiene a questo riquadro si scarta**: l'archivio è un file di
 * testo, e una chiave sbagliata non deve far comparire 'Inverti' dentro il menu di una foto.
 * ⚠️ **I doppioni si contano una volta sola**, come in `factOrderOf`.
 */
fun padOrderOf(tokens: List<String>, base: List<PadKey>): List<PadKey> {
    val known = tokens.mapNotNull { t -> base.firstOrNull { it.token == t } }.distinct()
    if (known.size == base.size) return known
    val fuori = known.toMutableList()
    // ⚠️ Si scorre la lista di FABBRICA e si infila ogni mancante subito dopo l'ultimo dei
    // suoi predecessori che è già in scena: così un tasto nuovo compare accanto a quelli con
    // cui è stato disegnato. Mancanti consecutivi restano nel loro ordine, perché ognuno
    // diventa il predecessore del successivo appena entra.
    for (k in base) {
        if (k in fuori) continue
        val prima = base.subList(0, base.indexOf(k)).lastOrNull { it in fuori }
        fuori.add(if (prima == null) 0 else fuori.indexOf(prima) + 1, k)
    }
    return fuori
}

/**
 * One store for everything the app remembers, declared at file level so that both
 * the settings and the recents can reach it.
 *
 * ⚠️ It has to be ONE: two `preferencesDataStore` delegates over the same name
 * crash at the first read with 'there are multiple DataStores active for the same
 * file', and two different names would mean two files for one small set of
 * preferences.
 */
private val Context.aivStore: DataStore<Preferences> by preferencesDataStore(name = "aiv-settings")

/** Reads and writes the settings. */
object SettingsStore {

    private val BG_TYPE = stringPreferencesKey("bg-type")
    private val BG_THEME = stringPreferencesKey("bg-theme")
    private val FIT_GROW = booleanPreferencesKey("fit-grow")
    private val ZOOM_MAX = floatPreferencesKey("zoom-max")
    private val SCALE_MODE = stringPreferencesKey("scale-mode")
    private val INFO_POSITION = stringPreferencesKey("info-position")
    private val INFO_VISIBLE = booleanPreferencesKey("info-visible")
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
    private val LIST_PATH = booleanPreferencesKey("list-path")
    private val PICK_WEIGHT = booleanPreferencesKey("pick-weight")
    private val EDITOR_APP = stringPreferencesKey("editor-app")
    private val EDITOR_BACKUP = booleanPreferencesKey("editor-backup")
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
    private val EXT_EDIT = booleanPreferencesKey("ext-edit")
    private val GPU_THUMBS = booleanPreferencesKey("gpu-thumbs")
    private val BIN_KEEP = stringPreferencesKey("bin-keep")
    private val FOLDER_COUNT = booleanPreferencesKey("folder-count")
    private val HIDDEN_FOLDERS = stringSetPreferencesKey("hidden-folders")

    // ⚠️ Due chiavi e non una, perché sono due domande: in che ordine stanno i campi, e
    // quali sono spenti. Una sola stringa coi soli accesi perderebbe la posizione di
    // quelli spenti (vedi `Settings.factOrder`).
    private val FACT_ORDER = stringPreferencesKey("fact-order")
    private val FACT_OFF = stringSetPreferencesKey("fact-off")

    /*
     * ⚠️⚠️ **QUATTRO CHIAVI PER QUATTRO RIQUADRI, e non una sola** (dalla `1.56`). La ragione
     * non è di comodo: i due riquadri delle azioni sui file portano insiemi diversi di tasti
     * **in ordini diversi**, e tutti e due dettati dall'utente. Il menu del tocco lungo va
     * copia-sposta-condividi e poi rinomina-elimina-info; la scheda della selezione va
     * rinomina-info-sposta-copia-condividi e poi le sue quattro. Nessuno dei due è il
     * sottoinsieme ordinato dell'altro, quindi una chiave sola ne cambierebbe uno.
     * ⚠️ **Le due file dell'editor sono due chiavi perché sono due gruppi**: il filetto in
     * mezzo dice che girare e centrare è un mestiere, e la cronologia un altro. Un tasto non
     * passa da una fila all'altra.
     */
    private val MENU_ORDER = stringPreferencesKey("menu-order")
    private val PICK_ORDER = stringPreferencesKey("pick-order")
    private val TURN_ORDER = stringPreferencesKey("turn-order")
    private val STEP_ORDER = stringPreferencesKey("step-order")
    private val PAD_LABELS = booleanPreferencesKey("pad-labels")

    /** Bounds of the only numeric setting, so a stored value out of range cannot reach the viewer. */
    const val ZOOM_MAX_MIN = 2f
    const val ZOOM_MAX_MAX = 200f

    fun flow(context: Context): Flow<Settings> = context.aivStore.data.map { p ->
        Settings(
            bgType = BgType.entries.byToken(p[BG_TYPE], BgType.CHECKER),
            bgTheme = BgTheme.entries.byToken(p[BG_THEME], BgTheme.AUTO),
            fitGrow = p[FIT_GROW] ?: false,
            zoomMax = (p[ZOOM_MAX] ?: 40f).coerceIn(ZOOM_MAX_MIN, ZOOM_MAX_MAX),
            scaleMode = ScaleMode.entries.byToken(p[SCALE_MODE], ScaleMode.PHYSICAL),
            infoPosition = InfoPosition.entries.byToken(p[INFO_POSITION], InfoPosition.TOP),
            infoVisible = p[INFO_VISIBLE] ?: true,
            veil = p[VEIL] ?: false,
            zoomInMenu = p[ZOOM_IN_MENU] ?: false,
            reverseSequence = p[REVERSE_SEQUENCE] ?: false,
            startFolder = p[START_FOLDER],
            startFolderName = p[START_FOLDER_NAME] ?: "",
            openAtStart = p[OPEN_AT_START] ?: false,
            folderView = FolderView.entries.byToken(p[FOLDER_VIEW], FolderView.GRID),
            clipboardStart = p[CLIPBOARD_START] ?: false,
            clipboardDone = p[CLIPBOARD_DONE] ?: "",
            clipboardWhen = p[CLIPBOARD_WHEN] ?: 0L,
            hand = Hand.entries.byToken(p[HAND], Hand.RIGHT),
            listPath = p[LIST_PATH] ?: false,
            pickWeight = p[PICK_WEIGHT] ?: true,
            editorApp = p[EDITOR_APP] ?: "",
            editorBackup = p[EDITOR_BACKUP] ?: true,
            animCounter = p[ANIM_COUNTER] ?: true,
            listCount = p[LIST_COUNT] ?: true,
            listText = TextSize.entries.byToken(p[LIST_TEXT], TextSize.NORMAL),
            treeHidden = p[TREE_HIDDEN] ?: false,
            treePictures = p[TREE_PICTURES] ?: false,
            uiTheme = UiTheme.entries.byToken(p[UI_THEME], UiTheme.SYSTEM),
            // ⚠️ Ricondotto all'elenco ammesso e non solo letto: un numero fuori posto
            // nell'archivio (una versione futura, un file modificato a mano) darebbe una
            // griglia a zero colonne, cioè una schermata vuota senza nessun errore.
            folderColumns = p[FOLDER_COLUMNS_KEY]?.takeIf { it in FOLDER_COLUMNS } ?: 2,
            folderCount = p[FOLDER_COUNT] ?: true,
            binOn = p[BIN_ON] ?: true,
            imagesOnly = p[IMAGES_ONLY] ?: false,
            clipAutoplay = p[CLIP_AUTOPLAY] ?: false,
            gridNames = p[GRID_NAMES] ?: false,
            extEdit = p[EXT_EDIT] ?: false,
            gpuThumbs = p[GPU_THUMBS] ?: false,
            binKeep = BinKeep.entries.byToken(p[BIN_KEEP], BinKeep.NEVER),
            hiddenFolders = p[HIDDEN_FOLDERS] ?: emptySet(),
            factOrder = factOrderOf((p[FACT_ORDER] ?: "").split(',')),
            menuOrder = padOrderOf((p[MENU_ORDER] ?: "").split(','), MENU_KEYS),
            pickOrder = padOrderOf((p[PICK_ORDER] ?: "").split(','), PICK_KEYS),
            turnOrder = padOrderOf((p[TURN_ORDER] ?: "").split(','), TURN_KEYS),
            stepOrder = padOrderOf((p[STEP_ORDER] ?: "").split(','), STEP_KEYS),
            padLabels = p[PAD_LABELS] ?: true,
            // ⚠️ I campi sempre visibili si tolgono **in lettura**: un archivio che li
            // dichiarasse spenti (una versione futura, un file modificato a mano) non deve
            // poter far sparire il nome del file.
            factOff = (p[FACT_OFF] ?: emptySet())
                .mapNotNull { t -> FactField.entries.firstOrNull { it.token == t } }
                .filterNot { it.always }
                .toSet(),
        )
    }

    /**
     * Segna che questo indirizzo degli appunti è stato aperto, e non si riaprirà più.
     *
     * ⚠️ Scrive la **sola** chiave invece di passare da [save]: quella riscrive l'oggetto
     * intero, e chiamarla da qui vorrebbe dire riscrivere **ogni** preferenza per ricordare
     * una stringa, con in mezzo la possibilità di sovrascrivere con una copia vecchia quello
     * che la schermata delle impostazioni ha cambiato nel frattempo.
     * ⚠️ Fino alla `1.46` qui c'era il conto, 'ventiquattro preferenze', e le assegnazioni di
     * [save] erano già trentacinque: il numero non aggiungeva niente all'argomento, che è
     * 'tutte invece di una', e intanto mentiva. Il criterio sta in `rules/Roccobot.md`
     * § '🔢 I conti si contano, non si scrivono'.
     */
    suspend fun clipboardOpened(context: Context, address: String, at: Long) {
        context.aivStore.edit { p ->
            p[CLIPBOARD_DONE] = address
            // ⚠️ Le due chiavi si scrivono INSIEME, e non è pignoleria: separate, un arresto
            // fra le due lascerebbe un indirizzo con l'istante di un altro, cioè una memoria
            // che dice di aver aperto una cosa che non ha aperto. `edit` è una transazione.
            p[CLIPBOARD_WHEN] = at
        }
    }

    suspend fun save(context: Context, settings: Settings) {
        context.aivStore.edit { p ->
            p[BG_TYPE] = settings.bgType.token
            p[BG_THEME] = settings.bgTheme.token
            p[FIT_GROW] = settings.fitGrow
            p[ZOOM_MAX] = settings.zoomMax.coerceIn(ZOOM_MAX_MIN, ZOOM_MAX_MAX)
            p[SCALE_MODE] = settings.scaleMode.token
            p[INFO_POSITION] = settings.infoPosition.token
            p[INFO_VISIBLE] = settings.infoVisible
            p[VEIL] = settings.veil
            p[ZOOM_IN_MENU] = settings.zoomInMenu
            p[REVERSE_SEQUENCE] = settings.reverseSequence
            // ⚠️ Una cartella tolta si CANCELLA invece di essere scritta a zero: zero è
            // un id come un altro, e un giorno finirebbe per somigliare a una cartella
            // vera. L'assenza della chiave è l'unico modo di dire 'nessuna'.
            settings.startFolder?.let { p[START_FOLDER] = it } ?: p.remove(START_FOLDER)
            p[START_FOLDER_NAME] = settings.startFolderName
            p[OPEN_AT_START] = settings.openAtStart
            p[FOLDER_VIEW] = settings.folderView.token
            p[CLIPBOARD_START] = settings.clipboardStart
            p[HAND] = settings.hand.token
            p[LIST_PATH] = settings.listPath
            p[PICK_WEIGHT] = settings.pickWeight
            p[EDITOR_APP] = settings.editorApp
            p[EDITOR_BACKUP] = settings.editorBackup
            p[ANIM_COUNTER] = settings.animCounter
            p[LIST_COUNT] = settings.listCount
            p[LIST_TEXT] = settings.listText.token
            p[TREE_HIDDEN] = settings.treeHidden
            p[TREE_PICTURES] = settings.treePictures
            // ⚠️ Si riscrive anche qui, o il salvataggio della schermata delle impostazioni
            // (che scrive l'oggetto INTERO) cancellerebbe l'indirizzo già aperto, e gli
            // appunti tornerebbero a riaprirsi al primo avvio dopo un giro nelle opzioni.
            p[CLIPBOARD_DONE] = settings.clipboardDone
            p[CLIPBOARD_WHEN] = settings.clipboardWhen
            p[UI_THEME] = settings.uiTheme.token
            p[FOLDER_COLUMNS_KEY] = settings.folderColumns
            p[FOLDER_COUNT] = settings.folderCount
            p[BIN_ON] = settings.binOn
            p[IMAGES_ONLY] = settings.imagesOnly
            p[CLIP_AUTOPLAY] = settings.clipAutoplay
            p[GRID_NAMES] = settings.gridNames
            p[EXT_EDIT] = settings.extEdit
            p[GPU_THUMBS] = settings.gpuThumbs
            p[BIN_KEEP] = settings.binKeep.token
            p[HIDDEN_FOLDERS] = settings.hiddenFolders
            p[FACT_ORDER] = settings.factOrder.joinToString(",") { it.token }
            p[MENU_ORDER] = settings.menuOrder.joinToString(",") { it.token }
            p[PICK_ORDER] = settings.pickOrder.joinToString(",") { it.token }
            p[TURN_ORDER] = settings.turnOrder.joinToString(",") { it.token }
            p[STEP_ORDER] = settings.stepOrder.joinToString(",") { it.token }
            p[PAD_LABELS] = settings.padLabels
            p[FACT_OFF] = settings.factOff.filterNot { it.always }.map { it.token }.toSet()
        }
    }
}

/**
 * A token that is unknown, or missing, falls back to the default instead of
 * throwing: a stored file that a future version does not recognise must not stop
 * the app from starting.
 */
private fun <T : Choice> List<T>.byToken(token: String?, fallback: T): T =
    if (token == null) fallback else firstOrNull { it.token == token } ?: fallback

/**
 * The pictures AIV has opened, most recent first.
 *
 * ⚠️⚠️ This exists INSTEAD of the thing that was asked for, which was to look at
 * what has recently been visited in Chrome or Brave and offer the pictures from
 * those pages. That cannot be done, and not for want of a permission to request:
 * a browser's history lives in its own private storage, and the public provider
 * that once exposed it (`Browser.BOOKMARKS_URI`, behind
 * READ_HISTORY_BOOKMARKS) was cut off in Android 6 precisely so that one app
 * could not read another's browsing. No app can offer that list, so this offers
 * the nearest true thing: what you opened here.
 *
 * ⚠️ Only web addresses are remembered, and a local file deliberately is not. The
 * permission on a `content://` handed over by another app lasts as long as that
 * intent does, so a remembered local picture would be a row that looks openable
 * and fails when tapped. A list that lies is worse than a shorter one.
 */
/**
 * Whether the folder permission has already been asked for once.
 *
 * ⚠️ Sta qui e non fra le impostazioni perché NON è una scelta dell'utente: è un
 * promemoria dell'app a sé stessa. Serve perché il permesso si chiede **una volta
 * sola**, alla prima immagine locale aperta: un sistema che chiede e richiede è
 * quello che insegna a rifiutare per riflesso, e chi ha detto no una volta ha
 * detto abbastanza.
 */
object FolderAsk {

    // ⚠️⚠️ LA CHIAVE È CAMBIATA NELLA 0.21, E NON È UN RIORDINO: la 0.20 ha
    // cambiato la DOMANDA (dal dialogo su READ_MEDIA_IMAGES alla pagina di
    // sistema sull'accesso a tutti i file) ma aveva tenuto la chiave vecchia,
    // quindi su ogni telefono che aveva già risposto alla 0.19 il promemoria era
    // già a `true` e la domanda nuova non è mai stata fatta. Il difetto non si
    // vede da nessuna parte: la funzione semplicemente non fa niente, e sui
    // telefoni che l'avevano provata prima, cioè proprio quelli. Una domanda
    // nuova vuole un promemoria nuovo.
    // ⚠️ La chiave vecchia resta scritta negli archivi e non si legge più:
    // DataStore ignora le chiavi che nessuno chiede.
    private val ASKED = booleanPreferencesKey("all-files-asked")

    fun flow(context: Context): Flow<Boolean> = context.aivStore.data.map { p -> p[ASKED] ?: false }

    suspend fun remember(context: Context) {
        context.aivStore.edit { p -> p[ASKED] = true }
    }

    /**
     * Dimentica di aver chiesto, così la domanda torna alla prossima immagine locale.
     *
     * ⚠️⚠️ **È L'UNICA VIA DI RITORNO da un 'no', ed è la ragione per cui esiste** (chiesta
     * dall'utente, 2026-09-01, insieme al ripristino degli onboarding): la domanda si fa una
     * volta sola per non insegnare a rifiutare per riflesso, ma quella scelta di disegno,
     * senza questa funzione, chiuderebbe la porta per sempre a chi ha toccato 'no' di fretta.
     * ⚠️ **Non concede niente**: rimette in piedi la **domanda**, e il permesso resta quello
     * che il sistema dice. Chi l'ha già concesso non vedrà nulla, perché la richiesta guarda
     * prima `Folder.granted`.
     */
    suspend fun forget(context: Context) {
        context.aivStore.edit { p -> p.remove(ASKED) }
    }
}

/**
 * I mini onboarding del tocco lungo sul tastino, e se si sono già visti.
 *
 * ⚠️⚠️ **Stanno qui e NON in [Settings], e la differenza non è di comodo**: `Settings` è
 * quello che l'utente sceglie e ritrova nella schermata delle impostazioni, questi sono
 * promemoria che l'app tiene per sé. Metterli là darebbero righe in una schermata che
 * l'utente ha già chiesto di alleggerire, e sarebbero righe che non decidono niente.
 * ⚠️ Ognuno si archivia **una volta sola**, e da lì in poi non si mostra più: un onboarding
 * che torna è un avviso, e un avviso che torna insegna a chiuderlo senza leggerlo. Vale la
 * stessa ragione già scritta per [FolderAsk].
 *
 * ⚠️⚠️ **QUI C'ERA ANCHE `PICK_ALL`, il velo che insegnava il tocco lungo sul tastino
 * della selezione, ed è uscito nella `0.94` col tastino stesso**: 'scegli tutto' adesso è
 * un tasto della bottomsheet, che si vede, quindi non c'è più niente da insegnare. La sua
 * chiave in archivio (`pick-all-hint-seen`) resta scritta sui telefoni di chi l'aveva già
 * visto e non dà fastidio a nessuno: cancellarla vorrebbe dire una migrazione per liberare
 * un booleano.
 */
enum class Hint(token: String) {
    BIN_EMPTY("bin-empty-hint-seen"),

    /**
     * La scorciatoia delle colonne, dalla `0.78`: **solo nella schermata delle cartelle**.
     *
     * ⚠️ È la terza, e le prime due stanno nella griglia delle foto: il velo che la insegna
     * vive quindi in un'altra schermata, ed è la ragione per cui il `when` sulle frasi in
     * `GridScreen` ha un ramo che non si vedrà mai.
     */
    COLUMNS("columns-hint-seen"),

    /**
     * Il doppio tocco che cambia lo zoom, dalla `1.25`: **nel visualizzatore**, alla prima
     * fotografia che si apre.
     *
     * ⚠️⚠️ **NASCE COME CONTROPARTITA DI UNA RIMOZIONE** (richiesta dell'utente, 2026-09-02):
     * 'Adatta alla vista' e '100%' escono dal menu a pressione lunga, e senza un avviso il
     * doppio tocco resterebbe un gesto che nessuno sa di avere. È il primo velo che non
     * insegna una **scorciatoia**: insegna l'unico modo rimasto.
     * ⚠️ **È anche il primo che non evidenzia un tastino**, perché il gesto si fa sulla
     * fotografia intera: da qui `HintCentre` invece di `HintVeil`.
     */
    ZOOM_TAP("zoom-tap-hint-seen"),

    /**
     * I due rischi del cambio di estensione, dalla `1.36`: **nel pannellino della rinomina**,
     * la prima volta che si apre.
     *
     * ⚠️⚠️ **È IL TERZO PRESIDIO DELLA GRIGLIA DI SICUREZZA** (richiesta dell'utente,
     * 2026-09-02: *la prima volta che l'utente usa la funzione di rinomina delle estensioni,
     * deve apparire un mini-onboarding in mezzo allo schermo*), e gli altri due sono
     * l'interruttore spento di fabbrica e il paragrafo che lo accompagna (vedi
     * [Settings.extEdit]).
     * ⚠️⚠️ **È IL PRIMO VELO CHE NON INSEGNA UNA SCORCIATOIA: AVVISA.** Gli altri quattro
     * dicono 'esiste anche questo', questo dice 'attento a che cosa comporta'. Da qui il testo
     * con due punti esclamativi invece di una frase sola, e il fatto che compaia **prima** che
     * il pannellino si apra: un avviso dopo il gesto non è un avviso.
     * ⚠️ **Non ha bisogno di un secondo interruttore**: chi ha acceso la funzione ha già letto
     * il paragrafo nelle impostazioni, e questo velo è quello che si vede **mentre** la si usa,
     * cioè nel momento in cui serve.
     */
    EXT_WARN("ext-warn-hint-seen");

    private val seen = booleanPreferencesKey(token)

    fun flow(context: Context): Flow<Boolean> = context.aivStore.data.map { p -> p[seen] ?: false }

    suspend fun remember(context: Context) {
        context.aivStore.edit { p -> p[seen] = true }
    }

    /**
     * Dimentica di averlo mostrato, così il velo torna.
     *
     * ⚠️ **Rimuove la chiave invece di scriverci `false`**, e la differenza si vede solo il
     * giorno che si legge l'archivio a mano: una chiave assente vuol dire 'mai visto', che è
     * la verità che si sta ripristinando, mentre un `false` scritto sopra racconta una
     * storia in più. Per `flow` sono la stessa cosa, perché legge `?: false`.
     */
    suspend fun forget(context: Context) {
        context.aivStore.edit { p -> p.remove(seen) }
    }
}

object Recents {

    private val ENTRIES = stringPreferencesKey("recent")

    /** Eight, because the list has to fit under three buttons without becoming the screen. */
    private const val KEEP = 8

    /**
     * ⚠️ Tab between the two fields and newline between the entries, so no
     * escaping is needed: neither character can occur in a URL, and a name that
     * contained one would have arrived from a file system that cannot hold it.
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
        context.aivStore.edit { p ->
            val kept = (p[ENTRIES] ?: "").lineSequence()
                .filter { it.isNotBlank() && it.substringBefore('\t') != address }
                .take(KEEP - 1)
                .toList()
            p[ENTRIES] = (listOf("$address\t$clean") + kept).joinToString("\n")
        }
    }

    suspend fun clear(context: Context) {
        context.aivStore.edit { p -> p.remove(ENTRIES) }
    }
}

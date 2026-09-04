package io.github.roccobot.aiv

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp

/**
 * Le icone che Material non ha: il catalogo, coi nomi con cui si chiamano dal codice.
 *
 * ⚠️⚠️ **DALLA 1.46 I DISEGNI NON VIVONO QUI MA IN `res/drawable`**, un file per glifo, e
 * questo file è diventato il posto in cui si dice **che cosa vuol dire** ognuno. Fino alla
 * `1.45` i tracciati erano costanti di stringa qui sotto, e la ragione dello spostamento è
 * una misura, non un gusto: il verificatore `tools/icon-check.py` e l'app leggono adesso **lo
 * stesso file**, mentre prima il verificatore doveva ricostruire il tracciato dalle stringhe
 * Kotlin, e quella ricostruzione **ha sbagliato**. Diceva che i due glifi dell'allineamento
 * erano alti 0,75 unità su 24, perché trattava ogni riga come un sottotracciato mentre là le
 * righe spezzano **un numero a metà**. Un controllo che deve interpretare il codice sorgente
 * per vedere il disegno prima o poi misura il codice invece del disegno.
 * ⚠️ **La nota che c'era qui era proprio quella sbagliata**, e va saputo per non riscriverla:
 * diceva che 'le lettere di comando spezzano le righe e nient'altro, quindi ogni riga è un
 * sottotracciato'. Era falsa per due glifi su dieci.
 * ⚠️⚠️ **QUELLO CHE LO SPOSTAMENTO NON COMPRA È LA VALIDAZIONE AL BUILD**, e conviene
 * saperlo per non credere di avere una rete che non c'è: `aapt2` **non** guarda dentro
 * `android:pathData`. Provato con un tracciato che contiene la parola `ciao`: compila con
 * esito 0. Il tracciato lo legge `PathParser` al primo disegno a schermo, esattamente come
 * prima. La rete è il verificatore, e va lanciato.
 *
 * ⚠️ **Il cursore di testo è il solo disegno che resta scritto qui**, e non è una
 * dimenticanza: tutti gli altri sono **trasportati** da un file dell'utente, questo è
 * **calcolato** da quattro costanti con una relazione dichiarata ([PULL] è il doppio di
 * [NOTCH], e chi lo ignora ottiene metà rientranza credendola giusta). In XML quelle
 * diventerebbero i numeri `4.8` e `1.6`, cioè la relazione sparirebbe e la trappola con lei.
 * E non ci sarebbe niente da guadagnare: un tracciato composto da chiamate tipizzate non può
 * essere malformato, quindi non c'è nessun controllo a cui sottoporlo.
 *
 * ⚠️ **Come si legge un disegno**: `ImageVector.vectorResource`, che è `@Composable` e tiene
 * il risultato in un `remember` con chiave la risorsa, il tema e la configurazione. Quindi il
 * tracciato si legge una volta per punto di chiamata e non a ogni ricomposizione, che è il
 * comportamento che avevano i `by lazy` di prima.
 * ⚠️ **Il tipo resta `ImageVector` e non diventa `Painter`**, ed è una scelta: `MenuRow` di
 * `Menus.kt` misura lo slot dell'icona con `icon.defaultWidth` e `icon.defaultHeight`, che un
 * `Painter` non ha (la sua dimensione intrinseca è in pixel e chiederebbe una `Density`).
 * Così i punti di chiamata non hanno cambiato una riga, e il glifo da 25 unità continua a
 * dichiararsi 25dp.
 *
 * ⚠️ **Il colore dichiarato nei file è nero pieno e non è un difetto**: `Icon` disegna il
 * vettore con un `ColorFilter.tint`, quindi la tinta che si vede è quella passata a `Icon` e
 * quella dei file non si vede mai. È la stessa convenzione delle icone di Material.
 * ⚠️ Chi volesse aggiungere qui un glifo che Material ha già sta duplicando un disegno
 * mantenuto da altri, e prima o poi i due divergeranno. ⚠️ **L'eccezione, dalla `1.51`, è il
 * glifo che l'utente ha ammorbidito**, dove la divergenza è voluta e misurata: il blocco che
 * la dichiara sta più sotto, sopra [PickDelete].
 *
 * ⚠️⚠️ **QUESTO CATALOGO È ANDATO AVANTI E TORNATO INDIETRO, e la storia va saputa per non
 * rifare il giro.** Nella `1.33` gli otto glifi trasportati sono stati **sostituiti** con una
 * famiglia nuova disegnata dall'utente (tratto 1,8, capi e giunti tondi, una staffa a L al
 * posto della seconda forma cava, un solo riquadro per le tre voci di selezione), e nella
 * `1.35` sono **tornati questi**, su sua istruzione: *per il momento ripristina le icone
 * precedenti*.
 * ⚠️ **NON era un errore di lettura del brief**, ed è la parte che conta: quei glifi stavano
 * nella sezione 2 del brief dei disegni e la richiesta li nominava. L'utente ha spiegato dopo
 * che quel brief era un copiaincolla e che quella parte non l'aveva vista: *volevo cambiare
 * solo l'icona principale e la pagina di download per il momento*. Quindi il lavoro era
 * corretto e la **priorità** è cambiata.
 * ⚠️⚠️ **I DISEGNI NUOVI NON SONO PERSI, e stanno in due posti**: gli otto SVG dell'utente
 * (griglia 24) in Claude Design, progetto `Roccobot Design`, cartella `assets/icons/`, e il
 * codice che li portava qui nella **storia git**, commit della `1.33`. ⚠️ **L'ICONA NUOVA
 * DELL'APP RESTA**: quella era l'altra metà del lavoro e non è mai stata in discussione (vedi
 * `res/drawable/ic_launcher_foreground.xml`).
 * ⚠️ **Chi li rimettesse rifaccia il trasporto dalla storia git**, non a mano: la conversione
 * di `<rect rx>`, `<circle>` e `stroke-dasharray` in tracciati è misurata (da 0 a 25 pixel di
 * scarto su 57.600, rese a 240px), e rifarla senza quella misura la butterebbe via.
 */
object Glyphs {

    /**
     * Il cursore di testo, cioè la I con le due lineette: 'Rinomina'.
     *
     * ⚠️ **Di tratto e non di pieno**, a differenza di tutti gli altri: è l'unico fatto di
     * aria, ed è il baratto dichiarato quando l'utente l'ha scelto (*adesso è perfetta*). Lo
     * spessore è 2, come le altre a 24dp, così non sembra più leggero.
     * ⚠️ **Le estremità sono tonde** (`StrokeCap.Round`): a spigolo vivo, a 24dp, i tre
     * tratti sembrano tagliati da una lama e l'insieme perde il richiamo al cursore.
     * ⚠️ Material non ha niente che gli somigli: la cosa più vicina è la matita di
     * `Icons.Default.Edit`, che dice 'modifica' e non 'rinomina'.
     *
     * ⚠️⚠️ **LE DUE LINEETTE RIENTRANO VERSO IL CENTRO**, richiesta dell'utente (*un accenno
     * di rientranza sopra e sotto al centro delle lineette orizzontali, appena
     * percettibile*): è la forma della I maiuscola dei caratteri con le grazie, ed è ciò che
     * distingue un cursore di testo da una I stampatello. La rientranza vale [NOTCH] unità
     * di griglia su 24, cioè un trentesimo dell'icona: a densità 2,75 sono due pixel scarsi,
     * che è esattamente il 'appena percettibile' chiesto.
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
                // ⚠️⚠️ L'asta si ferma sul VENTRE della curva, non sui 4 e sui 20 dove
                // stanno le punte delle lineette: lassù sporgerebbe oltre la rientranza e
                // il glifo diventerebbe una farfalla. Provato disegnandolo, perché a
                // leggerlo sembrava indifferente. ⚠️ La capocchia tonda sporge di mezzo
                // spessore, cioè di 1, e va a coincidere col bordo esterno del tratto
                // della lineetta: è per questo che i due si saldano senza gradino.
                moveTo(12f, 4f + NOTCH)
                verticalLineTo(20f - NOTCH)
            }
        }.build()
    }

    /**
     * Due fogli sovrapposti, quello davanti con montagne e sole: 'Copia immagine'.
     *
     * ⚠️⚠️ **È IL SOLO GLIFO CHE ESCE DALLA PROPRIA TELA, dalla 1.37**, quindi si dichiara
     * 25dp su una griglia da 25 e chi lo disegna deve dargli uno slot da 24. Il perché, con
     * le misure, sta in testa a `res/drawable/ic_photo_pair.xml`, e come si tratta lo slot
     * sta in `MenuRow` di `Menus.kt`.
     */
    val PhotoPair: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_photo_pair)

    /**
     * Una matita dentro un riquadro: 'Modifica'.
     *
     * ⚠️ Sostituisce `Icons.Outlined.Edit` dalla `1.29`: la matita nuda diceva 'modifica' e
     * non 'modifica un'immagine'. Il riquadro è il segno di famiglia delle tre voci
     * sull'immagine, come le due cartelle sovrapposte lo sono di 'Copia' e 'Sposta'.
     */
    val ImageEdit: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_image_edit)

    /**
     * Due frecce opposte dentro un riquadro: 'Esporta/Converti'.
     *
     * ⚠️ Sostituisce `Icons.Outlined.SwapHoriz` dalla `1.29`: quelle erano due frecce nude,
     * cioè 'scambia', che è quello che fa un convertitore ma non dice su che cosa.
     */
    val ImageConvert: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_image_convert)

    /** Due cartelle sovrapposte: 'Copia'. */
    val FolderPair: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_folder_pair)

    /**
     * Due cartelle sovrapposte con quella dietro **tratteggiata**: 'Sposta'.
     *
     * ⚠️ Il tratteggio sta sulla cartella **di dietro** e non su quella davanti, e il verso
     * conta: spostare vuol dire che l'originale non resta dov'era, quindi la cartella che si
     * svuota è quella da cui si parte. Perché i trattini sono pezzi di tracciato, e non un
     * tratteggio, sta in testa a `res/drawable/ic_folder_pair_dashed.xml`.
     */
    val FolderPairDashed: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_folder_pair_dashed)

    /**
     * Un foglio dietro, e davanti un riquadro **tagliato da una spunta**: 'Seleziona tutto'.
     *
     * ⚠️⚠️ **UNA SPUNTA GRANDE AL POSTO DI DUE PICCOLE, dalla 1.46**, e qui era scritto il
     * contrario: la nota diceva che *due spunte e non una: una sola vuol dire 'questo è
     * scelto', due vogliono dire 'tutti'*. L'argomento era buono e l'utente ha deciso
     * altrimenti mandando il disegno nuovo, che è quello che decide. ⚠️ La spunta non è
     * dipinta dentro il riquadro: è il **taglio** che lo divide in due pezzi, quindi è spazio
     * non dipinto. Il resto sta in testa a `res/drawable/ic_pick_all.xml`.
     * ⚠️ Prima della `1.01` qui c'era `Icons.Outlined.SelectAll` di Material, un rettangolo
     * tratteggiato, cioè il gesto del riquadro di selezione col mouse, che su un telefono non
     * esiste.
     */
    val PickAll: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_pick_all)

    /**
     * Un foglio dietro a un riquadro con una **croce**: 'Svuota selezione'.
     *
     * ⚠️ Ridisegnato dall'utente nella `1.46`: la croce è più grande e arriva quasi al bordo
     * del riquadro. Il segno di famiglia, cioè il foglio e il riquadro, non è cambiato.
     */
    val PickNone: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_pick_none)

    /**
     * Un riquadro con una freccia che scende: 'Scarica'.
     *
     * ⚠️ **Disegnato dall'utente nella `1.46`**, e sostituisce `Icons.Outlined.Download` di
     * Material: quella era una freccia nuda sopra una lineetta, cioè diceva 'scarica' e non
     * 'scarica QUESTO', e nel menu del visualizzatore stava fra voci che portano tutte il
     * riquadro come segno di famiglia. Adesso lo porta anche lei.
     */
    val Download: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_download)

    /**
     * Un foglio dietro a un riquadro con **mezzo quadrato pieno**: 'Inverti selezione'.
     *
     * ⚠️ Dalla `1.37` la forma dentro il riquadro è un triangolo e non più due frecce che
     * girano: quelle dicevano 'ricomincia', il mezzo quadrato dice 'l'altra metà'. Che il
     * triangolo giri al contrario del riquadro non è un dettaglio del file ma il modo in cui
     * buca: il perché sta in testa a `res/drawable/ic_pick_invert.xml`.
     */
    val PickInvert: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_pick_invert)

    /**
     * Centra la selezione in orizzontale, nell'editor.
     *
     * ⚠️ **Disegnata dall'utente** (2026-09-01) e arrivata in una griglia **800x800**, non
     * nel 24x24 di Material: la tela resta la sua e a dichiarare la misura sono
     * `android:width` e `android:height`. Sostituisce `Icons.Outlined.AlignHorizontalCenter`,
     * che ha giudicato non abbastanza chiara (voce `ed-sheet` del collaudo).
     * ⚠️ **È più piccola della famiglia e va bene così**: 17,04 unità riportate su 24 dove
     * gli altri glifi stanno fra 18 e 20. L'utente lo ha confermato il 2026-09-03, quindi non
     * si corregge.
     */
    val AlignAcross: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_align_across)

    /** Vedi [AlignAcross]: è la stessa icona girata di un quarto. */
    val AlignDown: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_align_down)

    /*
     * ⚠️⚠️ **I SEI DELLA `1.51` SONO UN CASO NUOVO: AMMORBIDISCONO UN GLIFO CHE MATERIAL HA**,
     * mentre i dieci di prima disegnavano una cosa che Material non aveva. Sembra la
     * duplicazione che la nota in testa a questo file vieta, e non lo è, perché la ragione del
     * divieto è che i due divergano: qui **divergono apposta**, e la divergenza è il disegno.
     * Lo scarto contro il glifo di sistema è misurato in testa a ogni file, e va dallo 0,1% al
     * 5% della tela.
     * ⚠️⚠️ **E SEI CHE ARRIVAVANO NELLO STESSO INVIO NON SONO ENTRATI**, ed è la parte da non
     * rifare: `search`, `image`, `public`, `settings`, `folder` e `recycling` rendono a **zero
     * pixel di scarto** dal glifo che Compose già porta (misurato a 240px ricostruendo il
     * tracciato dal bytecode di `material-icons`), quindi là il divieto vale in pieno e le
     * voci chiamano `Icons` invece di un file.
     * ⚠️ **Con un'eccezione dichiarata, `settings`**: il file dell'utente e quello di Compose
     * differiscono del 5,6% della tela, ma è **la stessa ruota al 96%** (l'inchiostro va da
     * 19,20 a 20,00 unità, stesso centro), cioè due esportazioni successive dello stesso
     * disegno di Google. Nessuno dei due è una scelta di nessuno, e congelarne uno qui
     * costerebbe un file per una differenza che a 24dp non si vede.
     */

    /**
     * Il cestino pieno: 'Elimina', l'icona rossa del pannello della selezione.
     *
     * ⚠️ **Ammorbidisce `Icons.Default.Delete`** e non cambia nient'altro: gli spigoli vivi
     * diventano raccordi, per lo 0,2% della tela. L'inchiostro cade nello stesso riquadro.
     * ⚠️ **Non è [Bin]**, che è il cassone vuoto di contorno: quello dice il posto, questo
     * l'azione, e il pieno contro il contorno è ciò che li distingue a colpo d'occhio.
     */
    val PickDelete: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_pick_delete)

    /**
     * Il cestino con la freccia che sale: 'Ripristina', **e anche 'Ripristina tutto'**.
     *
     * ⚠️ **Sostituisce `Icons.Default.SettingsBackupRestore`**, che era un orologio con la
     * freccia circolare, cioè 'torna indietro nel tempo': nel cestino la domanda è 'tira fuori
     * questo file', e il cassone la dice mentre l'orologio no.
     * ⚠️⚠️ **UNO SOLO PER LE DUE VOCI, dalla `1.56`, e prima erano due** (istruzione
     * dell'utente, giro della `1.55`: *sostituisci l'icona di 'Ripristina tutto' con quella
     * usata per il ripristino di un file singolo*). Il disegno di prima aveva il cassone aperto
     * in fondo per dire 'tutti', e quella distinzione a 24dp era un dettaglio che nessuno
     * leggeva: a dire 'tutti' ci pensa la parola, che nel menu sta accanto all'icona.
     */
    val BinRestore: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_bin_restore)

    /**
     * Il cassone di contorno, vuoto: la voce 'Cestino' del menu della schermata iniziale.
     *
     * ⚠️ **Ammorbidisce `Icons.Outlined.Delete`**, per lo 0,1% della tela. Perché sia di
     * contorno e non di pieno sta su [PickDelete].
     */
    val Bin: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_bin)

    /**
     * Il cassone con dentro la freccia che torna indietro: 'Cronologia del cestino'.
     *
     * ⚠️⚠️ **DISEGNO DELL'UTENTE, dalla `1.55`, e prende il posto di `Icons.Default.Recycling`**
     * (giro della `1.54`: *pensavo che l'icona 'Cronologia del cestino' fosse un miglioramento,
     * ma non mi piaceva. L'ho ridisegnata*). Le tre frecce del riciclo dicevano il giro, che era
     * il punto della scelta della `1.51`, ma non dicevano il cestino: questa dice tutte e due.
     * ⚠️ **Qui non c'è nessun glifo di Material da cui questo sarebbe una copia**, e la regola
     * chiede di dirlo: il catalogo ha il bidone e ha l'orologio con la freccia, non la loro
     * unione. Il perché per esteso sta in testa a `ic_bin_history.xml`.
     */
    val BinHistory: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_bin_history)

    /** Quattro riquadri cavi: 'Visualizzazione griglia'. Ammorbidisce `Icons.Default.GridView`. */
    val ViewGrid: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_view_grid)

    /**
     * Tre righe con la miniatura a sinistra: 'Visualizzazione lista'.
     *
     * ⚠️ **È quello che si scosta di più dal suo glifo di sistema**, il 5% della tela contro
     * `Icons.Default.ViewList`: oltre ai raccordi, l'inchiostro è un'unità più stretto. Non è
     * un difetto del trasporto, che di pixel non ne cambia nessuno.
     */
    val ViewList: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_view_list)

    /** La griglia di Material: ogni icona del sistema è disegnata dentro un 24x24. */
    private const val GRID = 24f

    /** Lo spessore del solo [TextCursor], in unità di griglia. */
    private const val STROKE = 2f

    /**
     * Quanto rientra il centro di una lineetta, in unità di griglia.
     *
     * ⚠️ Scelto fra quattro provini (0 / 0,5 / 0,8 / 1,2) mostrati all'utente: sotto lo
     * 0,5 la curva sparisce nell'antialiasing, sopra l'1 il glifo diventa una clessidra.
     */
    private const val NOTCH = 0.8f

    /**
     * Dove sta il punto di controllo della curva, che NON è dove passa la curva.
     *
     * ⚠️⚠️ Una quadratica passa a **metà** fra la corda e il suo punto di controllo, quindi
     * per una rientranza vera di [NOTCH] il controllo va al doppio. Chi mettesse [NOTCH] qui
     * otterrebbe metà rientranza e la crederebbe giusta, perché il numero nel codice
     * direbbe la cosa voluta.
     */
    private const val PULL = NOTCH * 2f

    private val SIZE = 24.dp
}

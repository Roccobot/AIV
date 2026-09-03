package io.github.roccobot.aiv

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Le icone che Material non ha.
 *
 * ⚠️⚠️ **SEI DELLE SETTE LE HA DISEGNATE L'UTENTE** (le tre del riquadro dalla `0.81`, *ecco
 * le icone ridisegnate come le volevo*, e le tre della selezione dalla `1.01`), e questo file
 * le **trasporta** invece di interpretarle: tracciati pieni in una griglia 24x24, arrivati
 * come SVG e copiati qui **verbatim** nella loro forma compatta. Fino alla `0.80` le prime
 * tre erano provvisorie, disegnate qui a tratti e curve dietro sua richiesta di provare: quel
 * codice (rettangoli stondati, spezzate, dischi, e un tratteggio calcolato trattino per
 * trattino) è sparito con loro, ed è la ragione per cui questo file è un terzo di quello che
 * era.
 * ⚠️ **Il tracciato è il disegno, e non c'è una seconda copia**: gli SVG di Illustrator
 * portano 220 KB di metadati suoi che in un repo di codice non servono, mentre la `d` che sta
 * qui, incollata in qualunque visualizzatore SVG dentro un `<path>`, ridà l'icona identica.
 * ⚠️ **Le lettere di comando spezzano le righe e nient'altro**: ogni riga è un
 * sottotracciato, e la loro concatenazione è **esattamente** la stringa dell'SVG. Chi la
 * ricompone la può confrontare col file originale carattere per carattere.
 *
 * ⚠️ **Il cursore di testo resta disegnato qui**, ed è l'unico: l'utente l'ha approvato
 * (*adesso è perfetta*) e Material non ha niente che gli somigli (la cosa più vicina è la
 * matita di `Icons.Default.Edit`, che dice 'modifica' e non 'rinomina').
 * ⚠️ Chi volesse aggiungere qui un glifo che Material ha già sta duplicando un disegno
 * mantenuto da altri, e prima o poi i due divergeranno.
 *
 * ⚠️ **Il colore dichiarato è nero e non è un difetto**: `Icon` disegna il vettore con un
 * `ColorFilter.tint`, quindi la tinta che si vede è quella passata a `Icon` e questa non si
 * vede mai. È la stessa convenzione delle icone di Material, che dichiarano tutte nero.
 *
 * ⚠️⚠️ **QUESTO FILE È ANDATO AVANTI E TORNATO INDIETRO, e la storia va saputa per non
 * rifare il giro.** Nella `1.33` gli otto glifi qui sotto sono stati **sostituiti** con una
 * famiglia nuova disegnata dall'utente (tratto 1,8, capi e giunti tondi, una staffa a L al
 * posto della seconda forma cava, un solo riquadro per le tre voci di selezione), e nella
 * `1.35` sono **tornati questi**, su sua istruzione: *per il momento ripristina le icone
 * precedenti*.
 * ⚠️ **NON era un errore di lettura del brief**, ed è la parte che conta: quei glifi stavano
 * nella sezione 2 del brief dei disegni e la richiesta li nominava (*drawable e glifi in
 * `Glyphs.kt`*). L'utente ha spiegato dopo che quel brief era un copiaincolla e che quella
 * parte non l'aveva vista: *volevo cambiare solo l'icona principale e la pagina di download
 * per il momento*. Quindi il lavoro era corretto e la **priorità** è cambiata.
 * ⚠️⚠️ **I DISEGNI NUOVI NON SONO PERSI, e stanno in due posti**: gli otto SVG dell'utente
 * (griglia 24) in Claude Design, progetto `Roccobot Design`, cartella `assets/icons/`, e il
 * codice che li portava qui nella **storia git**, commit della `1.33`. ⚠️ **Nel repository
 * non ci sono più**: vivevano in una cartella `dev/`, svuotata il 2026-09-03 su istruzione
 * dell'utente. ⚠️ **L'ICONA NUOVA DELL'APP RESTA**: quella era l'altra metà del
 * lavoro e non è mai stata in discussione (vedi `res/drawable/ic_launcher_foreground.xml`).
 * ⚠️ **Chi li rimettesse rifaccia il trasporto dalla storia git**, non a mano: la conversione
 * di `<rect rx>`, `<circle>` e `stroke-dasharray` in tracciati è misurata (da 0 a 25 pixel di
 * scarto su 57.600, rese a 240px), e rifarla a occhio costerebbe quella misura.
 */
object Glyphs {

    /**
     * Il cursore di testo, cioè la I con le due lineette.
     *
     * ⚠️ **Di tratto e non di pieno**, a differenza delle altre tre del riquadro: è l'unica
     * fatta di aria, ed è il baratto dichiarato quando l'utente l'ha scelta. Lo spessore è 2,
     * come le altre a 24dp, così non sembra più leggera.
     * ⚠️ **Le estremità sono tonde** (`StrokeCap.Round`): a spigolo vivo, a 24dp, i tre
     * tratti sembrano tagliati da una lama e l'insieme perde il richiamo al cursore.
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
     * ⚠️ Il **sole** è la ragione per cui questo glifo esiste invece di
     * `Icons.Outlined.PhotoLibrary`: nessuna icona-immagine di Material ha il disco del sole
     * (verificato sui sorgenti di `image`, `photo`, `photo_library` e `collections`, che
     * portano la stessa spezzata a due cime e nessun disco), e la richiesta dell'utente lo
     * nominava.
     *
     * ⚠️⚠️ **È IL SOLO GLIFO CHE ESCE DALLA PROPRIA TELA, dalla 1.37**, e per questo passa
     * da [sporgente] e non da [filled]: il suo quadrato-base sta dove ce l'hanno [ImageEdit]
     * e [ImageConvert], e il foglio dietro arriva a -1. Il perché per esteso, con le misure,
     * sta su [COPY_IMAGE].
     */
    val PhotoPair: ImageVector by lazy { sporgente("PhotoPair", COPY_IMAGE) }

    /**
     * Una matita dentro un riquadro: 'Modifica'.
     *
     * ⚠️⚠️ **DISEGNATA DALL'UTENTE, dalla 1.29, e sostituisce `Icons.Outlined.Edit`**: la
     * matita nuda di Material diceva 'modifica' e non 'modifica un'immagine', e nel menu
     * stava accanto a due voci che il riquadro ce l'hanno. Il riquadro è il segno di
     * famiglia, come le due cartelle sovrapposte lo sono di 'Copia' e 'Sposta'.
     * ⚠️ **DUE tracciati e non uno**, ed è la prima volta che capita: il file ne porta tre,
     * di cui uno è il rettangolo trasparente con cui Illustrator dichiara la tela e che qui
     * non serve. La nota su [filled] lo prevedeva, e la via è `addPath` per ognuno.
     */
    val ImageEdit: ImageVector by lazy { filled("ImageEdit", EDIT_FRAME, EDIT_PENCIL) }

    /**
     * Due frecce opposte dentro un riquadro: 'Esporta/Converti'.
     *
     * ⚠️⚠️ **DISEGNATA DALL'UTENTE, dalla 1.29, e sostituisce `Icons.Outlined.SwapHoriz`**:
     * quelle erano due frecce nude, cioè 'scambia', che è quello che fa un convertitore ma
     * non dice su che cosa. Col riquadro dice su un'immagine, e sta in fila con le altre due.
     */
    val ImageConvert: ImageVector by lazy { filled("ImageConvert", CONVERT) }

    /** Due cartelle sovrapposte: 'Copia'. */
    val FolderPair: ImageVector by lazy { filled("FolderPair", COPY) }

    /**
     * Due cartelle sovrapposte con quella dietro **tratteggiata**: 'Sposta'.
     *
     * ⚠️ Il tratteggio sta sulla cartella **di dietro** e non su quella davanti, e il verso
     * conta: spostare vuol dire che l'originale non resta dov'era, quindi la cartella che si
     * svuota è quella da cui si parte, cioè quella in fondo.
     * ⚠️⚠️ **I TRATTINI SONO PEZZI DI TRACCIATO, e non un tratteggio**: un vettore di Android
     * non ha un `stroke-dasharray`, quindi non esiste altro modo. Qui non si vede perché
     * l'icona è **piena** e i trattini sono già cinque dei suoi sette sottotracciati: fino
     * alla `0.80`, che li disegnava di tratto, li calcolava una funzione apposta.
     */
    val FolderPairDashed: ImageVector by lazy { filled("FolderPairDashed", MOVE) }

    /**
     * Un foglio dietro a un riquadro con **due spunte**: 'Seleziona tutto'.
     *
     * ⚠️ Due spunte e non una: una sola vuol dire 'questo è scelto', due vogliono dire
     * 'tutti'. Il glifo di Material che stava qui prima (`SelectAll`) era invece un
     * rettangolo tratteggiato, cioè il gesto del riquadro di selezione col mouse, che su un
     * telefono non esiste.
     */
    val PickAll: ImageVector by lazy { filled("PickAll", PICK_ALL) }

    /** Un foglio dietro a un riquadro con una **croce**: 'Annulla selezione'. */
    val PickNone: ImageVector by lazy { filled("PickNone", PICK_NONE) }

    /**
     * Un foglio dietro a un riquadro con **due frecce che girano**: 'Inverti selezione'.
     *
     * ⚠️ Sostituisce `SwapHoriz` della `0.94`, cioè due frecce affiancate: quelle dicono
     * 'scambia due cose' e non 'ribalta la scelta', ed era la sola delle dieci su cui avevo
     * dichiarato un dubbio all'utente.
     */
    val PickInvert: ImageVector by lazy { filled("PickInvert", PICK_INVERT) }

    /**
     * Centra la selezione in orizzontale, e la sua gemella in verticale.
     *
     * ⚠️⚠️ **DISEGNATE DALL'UTENTE** (2026-09-01) e arrivate in una griglia **800x800**,
     * non nel 24x24 di Material: il tracciato si trasporta **verbatim** come tutti gli
     * altri, e a cambiare è la sola dichiarazione del riquadro. Riscalarne i numeri a mano
     * per farli stare in 24 vorrebbe dire mille arrotondamenti e un disegno che non si può
     * più confrontare col file di partenza.
     * ⚠️ **Misurate prima di entrare**: l'inchiostro sta in 568x466 unità su 800 ed è
     * centrato in (400, 400) esatte, quindi non serve nessuna correzione. Il glifo
     * dell'esportazione, misurato lo stesso giorno, non lo era, e andava ricentrato con un
     * gruppo: se n'è andato con il suo tasto nella 1.21, e la misura resta scritta nel
     * messaggio di quel commit.
     * ⚠️ **Sostituiscono `Icons.Outlined.AlignHorizontalCenter` e la sua gemella**, che
     * l'utente ha giudicato non abbastanza chiare (voce `ed-sheet` del collaudo).
     */
    val AlignAcross: ImageVector by lazy { grande("AlignAcross", ALIGN_ACROSS) }

    /** Vedi [AlignAcross]: è la stessa icona girata di un quarto. */
    val AlignDown: ImageVector by lazy { grande("AlignDown", ALIGN_DOWN) }

    /**
     * Il guscio dei glifi dell'utente: un 24x24 con un tracciato pieno.
     *
     * ⚠️ **Il riempimento è NON-ZERO**, che è il valore di serie di `addPath` e la regola di
     * serie dell'SVG: i controcampi (l'interno delle cartelle, il cielo fra le montagne) sono
     * sottotracciati che girano al contrario, e con la regola pari-dispari verrebbero uguali
     * solo perché non si sovrappongono. Uguale per caso non è uguale.
     * ⚠️ **Un tracciato solo per glifo, e regge perché i file arrivano così**: se un domani
     * ne arrivasse uno con più `<path>`, la via è un `addPath` per ognuno e **non** la
     * concatenazione delle loro `d`. Il perché sta sulle costanti, in fondo, ed è una misura.
     * ⚠️ **`PathParser` e non `addPathNodes`**: quel richiamo comodo non c'è in questa versione
     * di Compose (verificato nel bytecode di `PathNodeKt`, dove l'omonimo prende quattro
     * parametri interni). ⚠️ E il tracciato si legge una volta sola, perché i glifi sono
     * `by lazy`.
     */
    private fun filled(name: String, vararg d: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = SIZE,
            defaultHeight = SIZE,
            viewportWidth = GRID,
            viewportHeight = GRID
        ).apply {
            // ⚠️ **Un `addPath` per ogni `<path>` del file**, e la ragione (misurata) sta
            // sulle costanti in fondo: concatenare le `d` sfrangia la cucitura fra le forme.
            for (uno in d) {
                addPath(
                    pathData = PathParser().parsePathString(uno).toNodes(),
                    fill = SolidColor(Color.Black)
                )
            }
        }.build()

    /**
     * Il guscio del solo glifo che **esce dalla propria tela**: 25 unità invece di 24, col
     * disegno spostato di 1 da un gruppo. Vedi [COPY_IMAGE], che è il perché.
     *
     * ⚠️⚠️ **UN `ImageVector` NON HA UN'ORIGINE DI VIEWPORT**: dichiara `viewportWidth` e
     * `viewportHeight` e nient'altro, quindi l'origine è sempre (0,0) e un tracciato con
     * coordinate negative viene ritagliato. Un SVG risolverebbe la cosa con
     * `viewBox="-1 -1 25 25"`; qui la via è un **gruppo che traspone di 1**, e la tela che
     * cresce di 1 in entrambi i versi.
     * ⚠️ **La traslazione NON è una riscrittura dei numeri del tracciato**, ed è la ragione
     * per cui si fa così invece di sommare 1 a mano a ogni coordinata: la `d` resta
     * **verbatim** quella del file dell'utente e la si confronta carattere per carattere,
     * come prescrive la nota in testa a questo file.
     * ⚠️⚠️ **E LA MISURA CRESCE CON LA TELA, di proposito**: `defaultWidth` va a 25dp
     * insieme al viewport, così **un'unità resta un dp** e il disegno ha esattamente la
     * scala degli altri glifi. Se restasse 24dp, `Icon` schiaccerebbe 25 unità in 24 e il
     * quadrato-base diventerebbe 17,28 invece di 18: cioè si perderebbe proprio la cosa per
     * cui questo guscio esiste.
     * ⚠️ **Chi lo disegna deve dargli uno slot da 24**, o l'icona sposta il testo della sua
     * fila di 1dp: come, sta in `MenuRow` di `Menus.kt`.
     */
    private fun sporgente(name: String, d: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = OVER_SIZE,
            defaultHeight = OVER_SIZE,
            viewportWidth = OVER_GRID,
            viewportHeight = OVER_GRID
        ).group(translationX = OVER, translationY = OVER) {
            addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = SolidColor(Color.Black)
            )
        }.build()

    /** Il guscio dei glifi arrivati in una griglia più grande. Vedi [AlignAcross]. */
    private fun grande(name: String, d: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = SIZE,
            defaultHeight = SIZE,
            viewportWidth = WIDE_GRID,
            viewportHeight = WIDE_GRID
        ).addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            fill = SolidColor(Color.Black)
        ).build()

    /** La griglia di Material: ogni icona del sistema è disegnata dentro un 24x24. */
    private const val GRID = 24f

    /**
     * Di quanto esce dalla tela il solo glifo che ne esce, e quindi di quanto crescono la
     * sua griglia e la sua misura. Vedi [sporgente]: i tre numeri sono lo stesso numero, e
     * scriverli separati sarebbe un invito a farli divergere.
     */
    private const val OVER = 1f
    private const val OVER_GRID = GRID + OVER
    private val OVER_SIZE = (GRID + OVER).dp

    /** La griglia dei due glifi di allineamento, che Illustrator ha esportato a 800. */
    private const val WIDE_GRID = 800f


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

    /**
     * I tracciati delle due icone del menu, arrivate il 2026-09-02.
     *
     * ⚠️ **Il rettangolo trasparente del file di `imageEdit.svg` NON è qui**: Illustrator lo
     * esporta con `fill="none"` per dichiarare la tela da 24, e in un `ImageVector` la tela
     * la dichiarano `viewportWidth` e `viewportHeight`. Portarlo dentro aggiungerebbe un
     * tracciato nero a tutta l'icona, perché qui il riempimento è dichiarato dal codice e non
     * dal file.
     */
    private const val EDIT_FRAME =
        "M19,5v14H5V5h14M19,3H5c-1.1,0-2,.9-2,2v14c0,1.1.9,2,2,2h14c1.1,0,2-.9,2-2V5c0-1.1-.9-2-2-2Z"

    private const val EDIT_PENCIL =
        "M15.08,6.99c-.14,0-.28.06-.39.16l-1.02,1.02,2.09,2.09,1.26-1.26c.08-.08.08-.22,0-.3l-1.54-1.54c-.11-.11-.25-.16-.4-.16Z" +
            "M13.08,8.76l-6.16,6.16v2.09h2.09l6.16-6.16-2.09-2.09Z"

    private const val CONVERT =
        "M19,3H5c-1.1,0-2,.9-2,2v14c0,1.1.9,2,2,2h14c1.1,0,2-.9,2-2V5c0-1.1-.9-2-2-2Z" +
            "M5,19v-3.66l2.68,2.69v-2.58h6.04v-1.72h-6.04v-2.58l-2.68,2.69V5h14v3.66l-2.68-2.69v2.58h-6.04v1.72h6.04v2.58l2.68-2.69v8.83H5Z"

    private const val COPY =
        "M3,19h16.6c.22,0,.4.18.4.4h0c0,.88-.72,1.6-1.6,1.6H3c-1.1,0-2-.9-2-2V7.6c0-.88.72-1.6,1.6-1.6h0c.22,0,.4.18.4.4v12.6Z" +
            "M23,6v9c0,1.1-.9,2-2,2H7c-1.1,0-2-.9-2-2V4c.01-1.1.9-2,2-2h5l2,2h7c1.1,0,2,.9,2,2Z" +
            "M7,15h14V6h-7.83l-2-2h-4.17v11Z"

    /**
     * ⚠️⚠️ **QUESTO TRACCIATO ESCE DALLA TELA A SINISTRA E IN ALTO, e non è un difetto del
     * file**: il foglio dietro arriva a **-1** su un `viewBox="0 0 24 24"`. È il prezzo
     * dell'allineamento che l'utente ha chiesto, e il conto è misurato in Chromium con
     * `getBBox`: il quadrato-base sta in **x 3..21, y 3..21**, cioè **esattamente** dove ce
     * l'hanno [ImageEdit] e [ImageConvert], e per starci il foglio dietro deve sporgere.
     * Vedi [sporgente], che è il guscio che glielo permette.
     * ⚠️ **Il disegno di prima aveva il quadrato in 6..22 e grande 16 invece di 18**, ed è
     * il difetto che l'utente ha visto: *'Copia' ha il quadrato-base più piccolo e non
     * sembra allineato alle altre due*. Non era una svista di trasporto, era il disegno.
     * ⚠️ **Delle due versioni che ha mandato è la 1**, e la scelta è sua: la 2 è centrata
     * (inchiostro 1..23) e quindi ha il quadrato in 5..23, che non si allinea. ⚠️ **I due
     * file non sono più nel repository**: stavano in una cartella `dev/`, svuotata il
     * 2026-09-03 su istruzione dell'utente, e si ripescano dalla storia git.
     */
    private const val COPY_IMAGE =
        "M19,3H5c-1.1,0-2,.9-2,2v14c0,1.1.9,2,2,2h14c1.1,0,2-.9,2-2V5c0-1.1-.9-2-2-2Z" +
            "M19,19H5V5h14v14Z" +
            "M10.97,13.75c.76,0,1.38-.62,1.38-1.38s-.62-1.38-1.38-1.38-1.38.62-1.38,1.38.62,1.38,1.38,1.38Z" +
            "M15,.6c0-.89-.72-1.6-1.6-1.6H1C-.1-1-1-.1-1,1v12.4c0,.88.72,1.6,1.6,1.6.22,0,.4-.18.4-.4V1h13.6c.22,0,.4-.18.4-.4Z" +
            "M13.84,13.28l-2.73,3.41-1.86-2.49-2.75,3.66h11l-3.66-4.59Z"

    private const val MOVE =
        "M3,11.91H1v4.42h2v-4.42Z" +
            "M2.6,6h0c-.88,0-1.6.72-1.6,1.6v2.82h2v-4.02c0-.22-.18-.4-.4-.4Z" +
            "M3,17.82H1v1.18c0,1.1.9,2,2,2h3.18v-2h-3.18v-1.18Z" +
            "M14.58,21h3.82c.88,0,1.6-.72,1.6-1.6h0c0-.22-.18-.4-.4-.4h-5.02v2Z" +
            "M7.67,21h5.42v-2h-5.42v2Z" +
            "M21,4h-7l-2-2h-5c-1.1,0-1.99.9-1.99,2v11c-.01,1.1.89,2,1.99,2h14c1.1,0,2-.9,2-2V6c0-1.1-.9-2-2-2Z" +
            "M21,15H7V4h4.17l2,2h7.83v9Z"

    /**
     * I tre tracciati delle icone della selezione, uno per icona.
     *
     * ⚠️ **Ognuno è UN tracciato composto**, e i primi due sottotracciati (il foglio dietro
     * e il riquadro davanti) sono identici carattere per carattere nei tre file: è il segno
     * di famiglia del riquadro della selezione, come le due cartelle sovrapposte lo sono di
     * 'Copia' e 'Sposta'. Non si estraggono in una costante condivisa apposta: così la
     * concatenazione delle righe di ognuna resta **esattamente** la `d` del file
     * dell'utente, e la si confronta con l'originale carattere per carattere.
     * ⚠️⚠️ **Se un domani un file arrivasse con PIÙ `<path>` separati, non si concatenano
     * le loro `d`.** Misurato il 2026-08-31 sulla prima versione di queste tre, che arrivava
     * così: rese in Chromium e confrontate pixel per pixel, le due forme tenute separate
     * dànno zero scarto, concatenate ne dànno da 86 a 172, con differenze fino a 11 su 255.
     * Non è l'avvolgimento che si rompe, è la cucitura fra le due forme che si sfrangia.
     * La via giusta è un `addPath` per ognuno.
     */
    private const val PICK_ALL =
        "M17.6,20H5.41c-.78,0-1.41-.63-1.41-1.41V6.4c0-.22-.18-.4-.4-.4-.88,0-1.6.72-1.6,1.6v12.4c0,1.1.9,2,2,2h12.4c.88,0,1.6-.72,1.6-1.6h0c0-.22-.18-.4-.4-.4Z" +
            "M20,2h-12c-1.1,0-2,.9-2,2v12c0,1.1.9,2,2,2h12c1.1,0,2-.9,2-2V4c0-1.1-.9-2-2-2Z" +
            "M18.23,9.64l-5,5c-.15.15-.39.15-.54,0l-2.92-2.92c-.15-.15-.15-.4,0-.55l.28-.28c.15-.15.4-.15.55,0l2.29,2.29s.1.04.14,0l4.38-4.38c.15-.15.39-.15.55,0l.28.28c.15.15.15.4,0,.55Z" +
            "M18.23,6.18l-5,5c-.15.15-.39.15-.54,0l-2.92-2.92c-.15-.15-.15-.4,0-.55l.28-.28c.15-.15.4-.15.55,0l2.29,2.29s.1.04.14,0l4.38-4.38c.15-.15.39-.15.55,0l.28.28c.15.15.15.39,0,.55Z"

    private const val PICK_NONE =
        "M17.6,20H5.41c-.78,0-1.41-.63-1.41-1.41V6.4c0-.22-.18-.4-.4-.4-.88,0-1.6.72-1.6,1.6v12.4c0,1.1.9,2,2,2h12.4c.88,0,1.6-.72,1.6-1.6h0c0-.22-.18-.4-.4-.4Z" +
            "M20,2h-12c-1.1,0-2,.9-2,2v12c0,1.1.9,2,2,2h12c1.1,0,2-.9,2-2V4c0-1.1-.9-2-2-2Z" +
            "M18.2,13.38c.16.16.16.41,0,.57l-.25.25c-.16.16-.41.16-.57,0l-3.38-3.38-3.38,3.38c-.16.16-.41.16-.57,0l-.25-.25c-.16-.16-.16-.41,0-.57l3.38-3.38-3.38-3.38c-.16-.16-.16-.41,0-.57l.25-.25c.16-.16.41-.16.57,0l3.38,3.38,3.38-3.38c.16-.16.41-.16.57,0l.25.25c.16.16.16.41,0,.57l-3.38,3.38,3.38,3.38Z"

    /**
     * ⚠️ **Il terzo sottotracciato è UN TRIANGOLO e non due frecce, dalla 1.37**: i primi
     * due sono identici carattere per carattere a quelli di [PICK_ALL] e [PICK_NONE], come
     * vuole il segno di famiglia, e a cambiare è solo quello dentro il riquadro. Le due
     * frecce che girano dicevano 'ricomincia', il mezzo quadrato pieno dice 'l'altra metà'.
     * ⚠️ **Il triangolo gira al CONTRARIO del riquadro** (misurato: area con segno -496 il
     * riquadro, +144 il triangolo), quindi col riempimento non-zero di [filled] buca la metà
     * in alto a sinistra invece di riempirla. È lo stesso meccanismo con cui le due frecce di
     * prima stavano in negativo.
     */
    private const val PICK_INVERT =
        "M17.6,20H5.41c-.78,0-1.41-.63-1.41-1.41V6.4c0-.22-.18-.4-.4-.4-.88,0-1.6.72-1.6,1.6v12.4c0,1.1.9,2,2,2h12.4c.88,0,1.6-.72,1.6-1.6h0c0-.22-.18-.4-.4-.4Z" +
            "M20,2h-12c-1.1,0-2,.9-2,2v12c0,1.1.9,2,2,2h12c1.1,0,2-.9,2-2V4c0-1.1-.9-2-2-2Z" +
            "M8,16V4h12l-12,12Z"

    /** I due tracciati dell'allineamento, su griglia 800. Vedi [AlignAcross]. */
    private const val ALIGN_ACROSS =
            "M173.37,578.33c4.39,7.6,10.7,13.91,18.3,18.3,11.6,6.7,27.18,6.7,58.33,6.7h124.9c.06,0,.1" +
            ".05.1.1v76.56c0,2.21,1.79,4,4,4h41.99c2.21,0,4-1.79,4-4v-76.56c0-.06.05-.1.1-.1h124.9c31" +
            ".15,0,46.73,0,58.33-6.7,7.6-4.39,13.91-10.7,18.3-18.3,6.7-11.6,6.7-27.18,6.7-58.33s0-46." +
            "73-6.7-58.33c-4.39-7.6-10.7-13.91-18.3-18.3-11.6-6.7-27.18-6.7-58.33-6.7h-124.9c-.06,0-." +
            "1-.05-.1-.1v-73.13c0-.06.05-.1.1-.1h58.23c31.15,0,46.73,0,58.33-6.7,7.6-4.39,13.91-10.7," +
            "18.3-18.3,6.7-11.6,6.7-27.18,6.7-58.33s0-46.73-6.7-58.33c-4.39-7.6-10.7-13.91-18.3-18.3-" +
            "11.6-6.7-27.18-6.7-58.33-6.7h-58.23c-.06,0-.1-.05-.1-.1v-76.56c0-2.21-1.79-4-4-4h-42c-2." +
            "21,0-4,1.79-4,4v76.56c0,.06-.05.1-.1.1h-58.23c-31.15,0-46.73,0-58.33,6.7-7.6,4.39-13.91," +
            "10.7-18.3,18.3-6.7,11.6-6.7,27.18-6.7,58.33s0,46.73,6.7,58.33c4.39,7.6,10.7,13.91,18.3,1" +
            "8.3,11.6,6.7,27.18,6.7,58.33,6.7h58.23c.06,0,.1.05.1.1v73.13c0,.06-.05.1-.1.1h-124.9c-31" +
            ".15,0-46.73,0-58.33,6.7-7.6,4.39-13.91,10.7-18.3,18.3-6.7,11.6-6.7,27.18-6.7,58.33s0,46." +
            "73,6.7,58.33Z"

    private const val ALIGN_DOWN =
            "M221.67,173.37c-7.6,4.39-13.91,10.7-18.3,18.3-6.7,11.6-6.7,27.18-6.7,58.33v124.9c0,.06-." +
            "05.1-.1.1h-76.56c-2.21,0-4,1.79-4,4v41.99c0,2.21,1.79,4,4,4h76.56c.06,0,.1.05.1.1v124.9c" +
            "0,31.15,0,46.73,6.7,58.33,4.39,7.6,10.7,13.91,18.3,18.3,11.6,6.7,27.18,6.7,58.33,6.7s46." +
            "73,0,58.33-6.7c7.6-4.39,13.91-10.7,18.3-18.3,6.7-11.6,6.7-27.18,6.7-58.33v-124.9c0-.06.0" +
            "5-.1.1-.1h73.13c.06,0,.1.05.1.1v58.23c0,31.15,0,46.73,6.7,58.33,4.39,7.6,10.7,13.91,18.3" +
            ",18.3,11.6,6.7,27.18,6.7,58.33,6.7s46.73,0,58.33-6.7c7.6-4.39,13.91-10.7,18.3-18.3,6.7-1" +
            "1.6,6.7-27.18,6.7-58.33v-58.23c0-.06.05-.1.1-.1h76.56c2.21,0,4-1.79,4-4v-42c0-2.21-1.79-" +
            "4-4-4h-76.56c-.06,0-.1-.05-.1-.1v-58.23c0-31.15,0-46.73-6.7-58.33-4.39-7.6-10.7-13.91-18" +
            ".3-18.3-11.6-6.7-27.18-6.7-58.33-6.7s-46.73,0-58.33,6.7c-7.6,4.39-13.91,10.7-18.3,18.3-6" +
            ".7,11.6-6.7,27.18-6.7,58.33v58.23c0,.06-.05.1-.1.1h-73.13c-.06,0-.1-.05-.1-.1v-124.9c0-3" +
            "1.15,0-46.73-6.7-58.33-4.39-7.6-10.7-13.91-18.3-18.3-11.6-6.7-27.18-6.7-58.33-6.7s-46.73" +
            ",0-58.33,6.7Z"

}

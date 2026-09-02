package io.github.roccobot.aiv

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Le icone che Material non ha.
 *
 * ⚠️⚠️ **LE HA DISEGNATE L'UTENTE, e otto sono state RIDISEGNATE INSIEME il
 * 2026-09-02**: arrivano in `dev/glifi/` con un brief che ne dichiara la **grammatica comune**,
 * ed è quella la cosa nuova. Prima erano nate a scaglioni (le tre del riquadro dalla `0.81`, le
 * tre della selezione dalla `1.01`, le due dell'allineamento dalla `1.21`), ognuna col suo
 * criterio: piene, in griglie diverse, senza un tratto condiviso.
 * ⚠️ **La grammatica, da applicare a ogni glifo futuro della famiglia**: tratto **1,8** con
 * capi e giunti tondi e niente sotto quel valore; la coppia copia/sposta è una **staffa a L a
 * due tratti** dietro, non una seconda forma cava; la forma davanti è piena (cartella) o
 * contornata (lastra immagine), **mai entrambe cave**, perché a 24px il contorno interno si
 * chiude; copiare contro spostare è staffa continua contro **tratteggiata**; le tre voci di
 * selezione condividono **un solo riquadro** e si distinguono per il segno dentro.
 *
 * ⚠️⚠️ **QUESTA FAMIGLIA NON È UN TRASPORTO VERBATIM, e la differenza va saputa perché la
 * nota che stava qui prometteva il contrario.** I file dell'utente usano `<rect rx>`, `<circle>`
 * e `stroke-dasharray`, e nessuno dei tre esiste in un tracciato di Compose: il primo e il
 * secondo diventano archi, il terzo diventa **sei segmenti separati**. Quindi la `d` che sta qui
 * non si confronta più carattere per carattere con la sorgente, e al suo posto c'è una
 * **misura**: le due versioni rese in Chromium a 240px (dieci volte la griglia) e confrontate
 * pixel per pixel dànno da 0 a 25 pixel di scarto su 57.600, con il massimo su
 * `folder-pair-dashed`, che è il glifo dove i trattini vanno ricostruiti a mano.
 * ⚠️ **I pezzi condivisi stanno in una costante ognuno** ([BRACKET_SIDE], [BRACKET_FOOT],
 * [PICK_FRAME], [FOLDER]), e questo prima era vietato apposta: la ragione di allora era tenere
 * la concatenazione delle righe identica alla `d` del file, e con un `addPath` per `<path>` quel
 * confronto non passa più da una concatenazione. Il guadagno è che il segno di famiglia si vede
 * nel codice invece di essere ripetuto quattro volte.
 *
 * ⚠️ **Il cursore di testo resta come era, ed è l'unico**: il brief lo dichiara esplicitamente
 * fuori dal giro (*il nono, `text-cursor`, non è toccato*). L'utente l'aveva approvato (*adesso
 * è perfetta*) e Material non ha niente che gli somigli (la cosa più vicina è la matita di
 * `Icons.Default.Edit`, che dice 'modifica' e non 'rinomina').
 * ⚠️ Chi volesse aggiungere qui un glifo che Material ha già sta duplicando un disegno
 * mantenuto da altri, e prima o poi i due divergeranno.
 *
 * ⚠️ **Il colore dichiarato è nero e non è un difetto**: `Icon` disegna il vettore con un
 * `ColorFilter.tint`, quindi la tinta che si vede è quella passata a `Icon` e questa non si vede
 * mai. È la stessa convenzione delle icone di Material, che dichiarano tutte nero.
 */
object Glyphs {

    /**
     * Il cursore di testo, cioè la I con le due lineette.
     *
     * ⚠️ **Di tratto e non di pieno**: è l'unica fatta di aria, ed è il baratto dichiarato
     * quando l'utente l'ha scelta. Lo spessore è [CURSOR_STROKE] e **non** l'1,8 della famiglia
     * nuova, perché il brief del 2026-09-02 lo lascia fuori dal ridisegno: la nota che diceva
     * 'come le altre' è superata, le altre ora sono più sottili di lui.
     * ⚠️ **Le estremità sono tonde** (`StrokeCap.Round`): a spigolo vivo, a 24dp, i tre tratti
     * sembrano tagliati da una lama e l'insieme perde il richiamo al cursore.
     *
     * ⚠️⚠️ **LE DUE LINEETTE RIENTRANO VERSO IL CENTRO**, richiesta dell'utente (*un accenno
     * di rientranza sopra e sotto al centro delle lineette orizzontali, appena percettibile*):
     * è la forma della I maiuscola dei caratteri con le grazie, ed è ciò che distingue un cursore
     * di testo da una I stampatello. La rientranza vale [NOTCH] unità di griglia su 24, cioè un
     * trentesimo dell'icona: a densità 2,75 sono due pixel scarsi, che è esattamente il 'appena
     * percettibile' chiesto.
     */
    val TextCursor: ImageVector by lazy {
        glifo("TextCursor") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = CURSOR_STROKE,
                strokeLineCap = StrokeCap.Round
            ) {
                // La lineetta in alto, che si incurva verso il basso.
                moveTo(9f, 4f)
                quadTo(12f, 4f + PULL, 15f, 4f)
                // Quella in basso, che si incurva verso l'alto.
                moveTo(9f, 20f)
                quadTo(12f, 20f - PULL, 15f, 20f)
                // ⚠️⚠️ L'asta si ferma sul VENTRE della curva, non sui 4 e sui 20 dove
                // stanno le punte delle lineette: lassù sporgerebbe oltre la rientranza e il
                // glifo diventerebbe una farfalla. Provato disegnandolo, perché a leggerlo
                // sembrava indifferente. ⚠️ La capocchia tonda sporge di mezzo spessore, cioè
                // di 1, e va a coincidere col bordo esterno del tratto della lineetta: è per
                // questo che i due si saldano senza gradino.
                moveTo(12f, 4f + NOTCH)
                verticalLineTo(20f - NOTCH)
            }
        }
    }

    /**
     * Due lastre sovrapposte, quella davanti con montagne e sole: 'Copia immagine'.
     *
     * ⚠️ Il **sole** è la ragione per cui questo glifo esiste invece di
     * `Icons.Outlined.PhotoLibrary`: nessuna icona-immagine di Material ha il disco del sole
     * (verificato sui sorgenti di `image`, `photo`, `photo_library` e `collections`, che portano
     * la stessa spezzata a due cime e nessun disco), e la richiesta dell'utente lo nominava.
     * ⚠️ **La lastra davanti è CONTORNATA e non piena**, mentre nella coppia delle cartelle la
     * forma davanti è piena: è la regola della famiglia, e serve a distinguere le due coppie a
     * colpo d'occhio quando stanno nello stesso menu.
     */
    val PhotoPair: ImageVector by lazy {
        glifo("PhotoPair") {
            tratto("M3.2 16.6V5.4A2.2 2.2 0 0 1 5.4 3.2h11.2")
            tratto(
                "M10,7.6 H18.4 A2.4,2.4 0 0 1 20.8,10 V18.4 A2.4,2.4 0 0 1 18.4,20.8 H10" +
                    " A2.4,2.4 0 0 1 7.6,18.4 V10 A2.4,2.4 0 0 1 10,7.6 Z"
            )
            pieno("M10,11.7 A1.7,1.7 0 0 1 13.4,11.7 A1.7,1.7 0 0 1 10,11.7 Z")
            pieno("M9.2 18.6 L12.6 13.7 L15.1 17 L16.7 14.9 L19.2 18.6 Z")
        }
    }

    /**
     * Una matita dentro un riquadro: 'Modifica'.
     *
     * ⚠️⚠️ **DISEGNATA DALL'UTENTE, dalla 1.29, e sostituisce `Icons.Outlined.Edit`**: la
     * matita nuda di Material diceva 'modifica' e non 'modifica un'immagine', e nel menu stava
     * accanto a due voci che il riquadro ce l'hanno. Il riquadro è il segno di famiglia, come le
     * due cartelle sovrapposte lo sono di 'Copia' e 'Sposta'.
     * ⚠️ **Resta PIENA**, e non è stata ridisegnata col giro del 2026-09-02: quel brief nomina
     * otto glifi e questo non è fra loro.
     */
    val ImageEdit: ImageVector by lazy {
        glifo("ImageEdit") {
            pieno(EDIT_FRAME)
            pieno(EDIT_PENCIL)
        }
    }

    /**
     * Due frecce opposte dentro un riquadro: 'Esporta/Converti'.
     *
     * ⚠️⚠️ **DISEGNATA DALL'UTENTE, dalla 1.29, e sostituisce `Icons.Outlined.SwapHoriz`**:
     * quelle erano due frecce nude, cioè 'scambia', che è quello che fa un convertitore ma non
     * dice su che cosa. Col riquadro dice su un'immagine, e sta in fila con le altre due.
     */
    val ImageConvert: ImageVector by lazy {
        glifo("ImageConvert") { pieno(CONVERT) }
    }

    /** Una cartella davanti alla staffa a L: 'Copia'. */
    val FolderPair: ImageVector by lazy {
        glifo("FolderPair") {
            tratto(BRACKET_SIDE)
            tratto(BRACKET_FOOT)
            pieno(FOLDER)
        }
    }

    /**
     * La stessa cartella, con la staffa dietro **tratteggiata**: 'Sposta'.
     *
     * ⚠️ Il tratteggio sta sulla staffa **di dietro** e non sulla cartella davanti, e il verso
     * conta: spostare vuol dire che l'originale non resta dov'era, quindi quello che si svuota è
     * il posto da cui si parte, cioè quello in fondo.
     * ⚠️⚠️ **I TRATTINI SONO SEI SEGMENTI, e non un tratteggio**: un tracciato di Compose non
     * ha `stroke-dasharray`, quindi non esiste altra via. Il file dell'utente li dichiara con
     * `2.8 2.6`, e i sei pezzi qui sotto sono quel conto svolto: è il glifo con lo scarto più
     * alto della famiglia (25 pixel su 57.600), tutto sulle estremità tonde dei trattini.
     */
    val FolderPairDashed: ImageVector by lazy {
        glifo("FolderPairDashed") {
            tratto("M3.4,7.6 L3.4,10.4")
            tratto("M3.4,13 L3.4,15.8")
            tratto("M3.4,18.4 L3.4,18.6")
            tratto("M3.4,18.6 L6.2,18.6")
            tratto("M8.8,18.6 L11.6,18.6")
            tratto("M14.2,18.6 L16.4,18.6")
            pieno(FOLDER)
        }
    }

    /**
     * Il riquadro della selezione con **due spunte**: 'Seleziona tutto'.
     *
     * ⚠️ Due spunte e non una: una sola vuol dire 'questo è scelto', due vogliono dire 'tutti'.
     * Il glifo di Material che stava qui prima (`SelectAll`) era invece un rettangolo
     * tratteggiato, cioè il gesto del riquadro di selezione col mouse, che su un telefono non
     * esiste.
     */
    val PickAll: ImageVector by lazy {
        glifo("PickAll") {
            tratto(BRACKET_SIDE)
            tratto(BRACKET_FOOT)
            tratto(PICK_FRAME)
            tratto("M10.2 9.6l2.4 2.4 5-5.2")
            tratto("M10.2 14.8l2.4 2.4 5-5.2")
        }
    }

    /** Il riquadro della selezione con una **croce**: 'Annulla selezione'. */
    val PickNone: ImageVector by lazy {
        glifo("PickNone") {
            tratto(BRACKET_SIDE)
            tratto(BRACKET_FOOT)
            tratto(PICK_FRAME)
            tratto("M11 7.8l7 7.6M18 7.8l-7 7.6")
        }
    }

    /**
     * Il riquadro della selezione **mezzo pieno in diagonale**: 'Inverti selezione'.
     *
     * ⚠️ Il mezzo riquadro sostituisce le due frecce che girano della `1.01`, e prima ancora
     * `SwapHoriz` della `0.94`: due frecce dicono 'scambia due cose' e non 'ribalta la scelta'.
     * Mezzo riquadro pieno lo dice in una forma sola, ed è il terzo segno del gruppo.
     */
    val PickInvert: ImageVector by lazy {
        glifo("PickInvert") {
            tratto(BRACKET_SIDE)
            tratto(BRACKET_FOOT)
            pieno("M9.4 17.4L20.4 6.4v9.6a1.4 1.4 0 0 1-1.4 1.4z")
            tratto(PICK_FRAME)
        }
    }

    /**
     * Centra la selezione in orizzontale, e la sua gemella in verticale: un asse più tre
     * blocchi, e l'una è l'altra girata di un quarto.
     *
     * ⚠️⚠️ **RIDISEGNATE NELLA GRIGLIA 24 col giro del 2026-09-02**, e questo è il fatto
     * nuovo: dalla `1.21` arrivavano in una griglia **800x800** di Illustrator, e questo file
     * dichiarava un riquadro a parte per loro due. Ora sono su 24 come tutte le altre, quindi
     * quel guscio (la funzione `grande` e la costante `WIDE_GRID`) è uscito insieme ai loro
     * tracciati vecchi.
     * ⚠️ **Sostituiscono `Icons.Outlined.AlignHorizontalCenter` e la sua gemella**, che l'utente
     * ha giudicato non abbastanza chiare (voce `ed-sheet` del collaudo).
     */
    val AlignAcross: ImageVector by lazy {
        glifo("AlignAcross") {
            tratto("M2.6 12h18.8")
            pieno(
                "M4.6,8 H7.2 A1.2,1.2 0 0 1 8.4,9.2 V14.8 A1.2,1.2 0 0 1 7.2,16 H4.6" +
                    " A1.2,1.2 0 0 1 3.4,14.8 V9.2 A1.2,1.2 0 0 1 4.6,8 Z"
            )
            pieno(
                "M10.7,8 H13.3 A1.2,1.2 0 0 1 14.5,9.2 V14.8 A1.2,1.2 0 0 1 13.3,16 H10.7" +
                    " A1.2,1.2 0 0 1 9.5,14.8 V9.2 A1.2,1.2 0 0 1 10.7,8 Z"
            )
            pieno(
                "M16.8,8 H19.4 A1.2,1.2 0 0 1 20.6,9.2 V14.8 A1.2,1.2 0 0 1 19.4,16 H16.8" +
                    " A1.2,1.2 0 0 1 15.6,14.8 V9.2 A1.2,1.2 0 0 1 16.8,8 Z"
            )
        }
    }

    /** Vedi [AlignAcross]: è la stessa icona girata di un quarto. */
    val AlignDown: ImageVector by lazy {
        glifo("AlignDown") {
            tratto("M12 2.6v18.8")
            pieno(
                "M9.2,3.4 H14.8 A1.2,1.2 0 0 1 16,4.6 V7.2 A1.2,1.2 0 0 1 14.8,8.4 H9.2" +
                    " A1.2,1.2 0 0 1 8,7.2 V4.6 A1.2,1.2 0 0 1 9.2,3.4 Z"
            )
            pieno(
                "M9.2,9.5 H14.8 A1.2,1.2 0 0 1 16,10.7 V13.3 A1.2,1.2 0 0 1 14.8,14.5 H9.2" +
                    " A1.2,1.2 0 0 1 8,13.3 V10.7 A1.2,1.2 0 0 1 9.2,9.5 Z"
            )
            pieno(
                "M9.2,15.6 H14.8 A1.2,1.2 0 0 1 16,16.8 V19.4 A1.2,1.2 0 0 1 14.8,20.6 H9.2" +
                    " A1.2,1.2 0 0 1 8,19.4 V16.8 A1.2,1.2 0 0 1 9.2,15.6 Z"
            )
        }
    }

    /**
     * Il guscio di ogni glifo: un 24x24 da riempire con [pieno] e [tratto].
     *
     * ⚠️ **`PathParser` e non `addPathNodes`**: quel richiamo comodo non c'è in questa versione
     * di Compose (verificato nel bytecode di `PathNodeKt`, dove l'omonimo prende quattro
     * parametri interni). ⚠️ E il tracciato si legge una volta sola, perché i glifi sono
     * `by lazy`.
     */
    private fun glifo(name: String, corpo: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = SIZE,
            defaultHeight = SIZE,
            viewportWidth = GRID,
            viewportHeight = GRID
        ).apply(corpo).build()

    /**
     * Un tracciato pieno, cioè **un `<path>` del file**.
     *
     * ⚠️ **Il riempimento è NON-ZERO**, che è il valore di serie di `addPath` e la regola di
     * serie dell'SVG: i controcampi (l'interno di una cartella, il cielo fra le montagne) sono
     * sottotracciati che girano al contrario, e con la regola pari-dispari verrebbero uguali solo
     * perché non si sovrappongono. Uguale per caso non è uguale.
     * ⚠️⚠️ **UN RICHIAMO PER OGNI `<path>`, e le `d` NON si concatenano.** Misurato il
     * 2026-08-31 sulla prima versione dei tre glifi della selezione, che arrivava così: rese in
     * Chromium e confrontate pixel per pixel, le forme tenute separate dànno zero scarto,
     * concatenate ne dànno da 86 a 172, con differenze fino a 11 su 255. Non è l'avvolgimento
     * che si rompe, è la cucitura fra le due forme che si sfrangia.
     */
    private fun ImageVector.Builder.pieno(d: String) = addPath(
        pathData = PathParser().parsePathString(d).toNodes(),
        fill = SolidColor(Color.Black)
    )

    /**
     * Un tracciato di tratto, nello spessore della famiglia.
     *
     * ⚠️ **Capi e giunti tondi, e vale per tutti e otto**: il brief li chiede insieme allo
     * spessore, e sono la metà della grammatica. Un giunto a spigolo, su un tratto largo 1,8,
     * sporgerebbe di mezzo spessore oltre la punta, cioè di un ventisettesimo dell'icona.
     */
    private fun ImageVector.Builder.tratto(d: String) = addPath(
        pathData = PathParser().parsePathString(d).toNodes(),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = FAMILY_STROKE,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    )

    /** La griglia di Material: ogni icona del sistema è disegnata dentro un 24x24. */
    private const val GRID = 24f

    /**
     * Lo spessore di tutta la famiglia, in unità di griglia.
     *
     * ⚠️ **1,8 è un minimo oltre che un valore** (brief dell'utente: *niente tratto sotto
     * 1,8*): sotto quello, a 24dp e su uno schermo a densità 2,75, il tratto scende sotto i
     * cinque pixel e il glifo si sfarina accanto alle icone piene di Material.
     */
    private const val FAMILY_STROKE = 1.8f

    /** Lo spessore del solo [TextCursor], che il ridisegno del 2026-09-02 non ha toccato. */
    private const val CURSOR_STROKE = 2f

    /**
     * Quanto rientra il centro di una lineetta del [TextCursor], in unità di griglia.
     *
     * ⚠️ Scelto fra quattro provini (0 / 0,5 / 0,8 / 1,2) mostrati all'utente: sotto lo 0,5 la
     * curva sparisce nell'antialiasing, sopra l'1 il glifo diventa una clessidra.
     */
    private const val NOTCH = 0.8f

    /**
     * Dove sta il punto di controllo della curva, che NON è dove passa la curva.
     *
     * ⚠️⚠️ Una quadratica passa a **metà** fra la corda e il suo punto di controllo, quindi
     * per una rientranza vera di [NOTCH] il controllo va al doppio. Chi mettesse [NOTCH] qui
     * otterrebbe metà rientranza e la crederebbe giusta, perché il numero nel codice direbbe la
     * cosa voluta.
     */
    private const val PULL = NOTCH * 2f

    private val SIZE = 24.dp

    /**
     * La staffa a L dietro la cartella, in due tratti: il fianco e il piede.
     *
     * ⚠️ **Due tratti e non un rettangolo cavo**, ed è la regola della famiglia: una seconda
     * forma chiusa dietro la prima, a 24px, si legge come una cornice e non come 'la cartella di
     * prima'. Gli stessi due tratti stanno dietro i tre glifi della selezione.
     */
    private const val BRACKET_SIDE = "M3.4 7.6V18.6"

    /** Vedi [BRACKET_SIDE]: è il piede della stessa staffa. */
    private const val BRACKET_FOOT = "M3.4 18.6H16.4"

    /**
     * Il riquadro condiviso dai tre glifi della selezione: `6.8,4`, lato 15,2, raggio 2,4.
     *
     * ⚠️ **Uno solo per tutti e tre**, come il brief lo dichiara: i tre si distinguono per il
     * segno che portano dentro (due spunte, croce, mezzo riquadro pieno), e il riquadro è il
     * loro segno di famiglia, come la cartella lo è di 'Copia' e 'Sposta'.
     */
    private const val PICK_FRAME =
        "M9.2,4 H19.6 A2.4,2.4 0 0 1 22,6.4 V16.8 A2.4,2.4 0 0 1 19.6,19.2 H9.2 A2.4,2.4 0 0 1 6.8,16.8 V6.4 A2.4,2.4 0 0 1 9.2,4 Z"

    /** La cartella piena davanti alla staffa, condivisa da 'Copia' e 'Sposta'. */
    private const val FOLDER =
        "M8 4.2h4.4l1.8 1.8h6.4a1.2 1.2 0 0 1 1.2 1.2v7.6a1.2 1.2 0 0 1-1.2 1.2H8a1.2 1.2 0 0" +
            " 1-1.2-1.2V5.4A1.2 1.2 0 0 1 8 4.2z"

    /**
     * I due tracciati dell'icona 'Modifica', arrivata il 2026-09-02 e non ridisegnata col giro
     * degli otto.
     *
     * ⚠️ **Il rettangolo trasparente del file di `imageEdit.svg` NON è qui**: Illustrator lo
     * esporta con `fill="none"` per dichiarare la tela da 24, e in un `ImageVector` la tela la
     * dichiarano `viewportWidth` e `viewportHeight`. Portarlo dentro aggiungerebbe un tracciato
     * nero a tutta l'icona, perché qui il riempimento è dichiarato dal codice e non dal file.
     */
    private const val EDIT_FRAME =
        "M19,5v14H5V5h14M19,3H5c-1.1,0-2,.9-2,2v14c0,1.1.9,2,2,2h14c1.1,0,2-.9,2-2V5c0-1.1-.9-2-2-2Z"

    private const val EDIT_PENCIL =
        "M15.08,6.99c-.14,0-.28.06-.39.16l-1.02,1.02,2.09,2.09,1.26-1.26c.08-.08.08-.22,0-.3l-1.54-1.54c-.11-.11-.25-.16-.4-.16Z" +
            "M13.08,8.76l-6.16,6.16v2.09h2.09l6.16-6.16-2.09-2.09Z"

    private const val CONVERT =
        "M19,3H5c-1.1,0-2,.9-2,2v14c0,1.1.9,2,2,2h14c1.1,0,2-.9,2-2V5c0-1.1-.9-2-2-2Z" +
            "M5,19v-3.66l2.68,2.69v-2.58h6.04v-1.72h-6.04v-2.58l-2.68,2.69V5h14v3.66l-2.68-2.69v2.58h-6.04v1.72h6.04v2.58l2.68-2.69v8.83H5Z"
}

package io.github.roccobot.aiv

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
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
     */
    val PhotoPair: ImageVector by lazy { filled("PhotoPair", COPY_IMAGE) }

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
     * Una cornice con le montagne e una freccia che ne esce: 'Esporta il fotogramma'.
     *
     * ⚠️ **Sostituisce `Icons.Outlined.AddPhotoAlternate`, che l'utente aveva chiesto come
     * base e poi ridisegnato**: quella dice 'aggiungi una fotografia a una raccolta', cioè il
     * verso opposto. Qui il verso è tutto: il fotogramma **esce** dall'animazione e diventa un
     * file a sé, e la freccia che buca il lato destro della cornice è la sola parte del glifo
     * che lo dice.
     */
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
     * dell'esportazione, misurato lo stesso giorno, non lo era: vedi [PhotoOut].
     * ⚠️ **Sostituiscono `Icons.Outlined.AlignHorizontalCenter` e la sua gemella**, che
     * l'utente ha giudicato non abbastanza chiare (voce `ed-sheet` del collaudo).
     */
    val AlignAcross: ImageVector by lazy { grande("AlignAcross", ALIGN_ACROSS) }

    /** Vedi [AlignAcross]: è la stessa icona girata di un quarto. */
    val AlignDown: ImageVector by lazy { grande("AlignDown", ALIGN_DOWN) }

    /**
     * Il fotogramma che esce dall'animazione.
     *
     * ⚠️⚠️ **NON È UN `filled` COME GLI ALTRI, e le due correzioni sono MISURATE**
     * (riscontro dell'utente, 2026-09-01: *deve spostarsi in alto di 1 pixel apparente e
     * probabilmente rimpicciolirla al 90%*). Renderizzando il tracciato a 960 px e leggendo
     * il riquadro dell'inchiostro: sta in **18x18 unità** su 24, ma il suo centro cade in
     * **(11, 13)** invece che in (12, 12). Era basso di un'unità e spostato a sinistra di
     * una, che a 24dp su uno schermo a densità 3 fanno tre pixel: quello che si vedeva.
     * ⚠️ **La correzione sta nel GRUPPO e non nel tracciato**: la `d` resta identica al file
     * dell'utente e si può ancora confrontare carattere per carattere. Il gruppo scala di
     * [SHRINK] intorno al centro della griglia e poi trasla di quel tanto che porta il
     * centro dell'inchiostro esattamente su (12, 12).
     * ⚠️ **La traslazione vale [SHRINK] e non 1**, ed è la parte che si sbaglia: la scala
     * agisce **prima**, e avvicina già il centro sbagliato a quello giusto di un decimo.
     */
    val PhotoOut: ImageVector by lazy {
        ImageVector.Builder(
            name = "PhotoOut",
            defaultWidth = SIZE,
            defaultHeight = SIZE,
            viewportWidth = GRID,
            viewportHeight = GRID
        ).addGroup(
            name = "centratura",
            pivotX = GRID / 2f,
            pivotY = GRID / 2f,
            scaleX = SHRINK,
            scaleY = SHRINK,
            translationX = SHRINK,
            translationY = -SHRINK
        ).addPath(
            pathData = PathParser().parsePathString(EXPORT_IMAGE).toNodes(),
            fill = SolidColor(Color.Black)
        ).build()
    }

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
    private fun filled(name: String, d: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = SIZE,
            defaultHeight = SIZE,
            viewportWidth = GRID,
            viewportHeight = GRID
        ).addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            fill = SolidColor(Color.Black)
        ).build()

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

    /** La griglia dei due glifi di allineamento, che Illustrator ha esportato a 800. */
    private const val WIDE_GRID = 800f

    /** Di quanto rimpicciolisce [PhotoOut] rispetto alla sua griglia. */
    private const val SHRINK = 0.9f

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
     * I tre tracciati dell'utente, una riga per sottotracciato. Vedi la nota in testa: la
     * concatenazione è esattamente la `d` del suo SVG, e le righe non aggiungono niente.
     */
    private const val COPY =
        "M3,19h16.6c.22,0,.4.18.4.4h0c0,.88-.72,1.6-1.6,1.6H3c-1.1,0-2-.9-2-2V7.6c0-.88.72-1.6,1.6-1.6h0c.22,0,.4.18.4.4v12.6Z" +
            "M23,6v9c0,1.1-.9,2-2,2H7c-1.1,0-2-.9-2-2V4c.01-1.1.9-2,2-2h5l2,2h7c1.1,0,2,.9,2,2Z" +
            "M7,15h14V6h-7.83l-2-2h-4.17v11Z"

    private const val COPY_IMAGE =
        "M13.06,15.26c.69,0,1.26-.56,1.26-1.26s-.56-1.26-1.26-1.26-1.26.56-1.26,1.26.56,1.26,1.26,1.26Z" +
            "M16.4,2H4c-1.1,0-2,.9-2,2v12.4c0,.88.72,1.6,1.6,1.6h0c.22,0,.4-.18.4-.4V4h13.6c.22,0,.4-.18.4-.4h0c0-.89-.72-1.6-1.6-1.6Z" +
            "M20,6h-12c-1.1,0-2,.9-2,2v12c0,1.1.9,2,2,2h12c1.1,0,2-.9,2-2v-12c0-1.1-.9-2-2-2Z" +
            "M20,20h-12v-12h12v12Z" +
            "M15.67,14.83l-2.48,3.1-1.69-2.26-2.5,3.33h10l-3.33-4.17Z"

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

    private const val PICK_INVERT =
        "M17.6,20H5.41c-.78,0-1.41-.63-1.41-1.41V6.4c0-.22-.18-.4-.4-.4-.88,0-1.6.72-1.6,1.6v12.4c0,1.1.9,2,2,2h12.4c.88,0,1.6-.72,1.6-1.6h0c0-.22-.18-.4-.4-.4Z" +
            "M20,2h-12c-1.1,0-2,.9-2,2v12c0,1.1.9,2,2,2h12c1.1,0,2-.9,2-2V4c0-1.1-.9-2-2-2Z" +
            "M13.43,15.71c-2.9-.29-5.17-2.73-5.17-5.71,0-1.66.71-3.15,1.84-4.19l-1.26-1.26h3.05c.22,0,.4.18.4.4v3.05l-1.37-1.37c-.92.84-1.5,2.04-1.5,3.38,0,2.34,1.76,4.27,4.02,4.55.34,0,.61.3.58.62-.02.29-.27.53-.58.54Z" +
            "M19.17,15.45h-3.05c-.22,0-.4-.18-.4-.4v-3.05l1.37,1.37c.92-.84,1.5-2.04,1.5-3.38,0-2.34-1.76-4.27-4.02-4.55-.34,0-.6-.3-.58-.62.02-.29.27-.53.58-.54,2.9.29,5.17,2.73,5.17,5.71,0,1.66-.71,3.15-1.84,4.19l1.26,1.26Z"

    /** Il tracciato dell'esportazione: montagne, cornice aperta a destra, freccia che esce. */
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

    private const val EXPORT_IMAGE =
        "M10.21,16.83l-1.96-2.36-2.75,3.53h11l-3.54-4.71-2.75,3.54Z" +
            "M19.6,12.43h-1.19c-.22,0-.4.18-.4.4v7.17H4V6h8.6c.22,0,.4-.18.4-.4v-1.2c0-.22-.18-.4-.4-.4H4c-1.1,0-2,.9-2,2v14c0,1.1.9,2,2,2h14c1.1,0,2-.9,2-2v-7.17c0-.22-.18-.4-.4-.4Z" +
            "M10,8.41v1.15c0,.22.18.4.4.4h4.6v4.01l4.97-4.98-4.97-4.98v4.01h-4.6c-.22,0-.4.18-.4.4Z"
}

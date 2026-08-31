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
 * ⚠️⚠️ **LE TRE DEL RIQUADRO LE HA DISEGNATE L'UTENTE, dalla 0.81** (*ecco le icone
 * ridisegnate come le volevo*), e questo file le **trasporta** invece di interpretarle: sono
 * tre tracciati pieni in una griglia 24x24, arrivati come SVG e copiati qui **verbatim** nella
 * loro forma compatta. Fino alla `0.80` erano provvisorie, disegnate qui a tratti e curve
 * dietro sua richiesta di provare: quel codice (rettangoli stondati, spezzate, dischi, e un
 * tratteggio calcolato trattino per trattino) è sparito con loro, ed è la ragione per cui
 * questo file è un terzo di quello che era.
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
     * Il guscio dei tre glifi dell'utente: un 24x24 con un tracciato pieno.
     *
     * ⚠️ **Il riempimento è NON-ZERO**, che è il valore di serie di `addPath` e la regola di
     * serie dell'SVG: i controcampi (l'interno delle cartelle, il cielo fra le montagne) sono
     * sottotracciati che girano al contrario, e con la regola pari-dispari verrebbero uguali
     * solo perché non si sovrappongono. Uguale per caso non è uguale.
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
}

package io.github.roccobot.aiv

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/**
 * Le icone che Material non ha, disegnate qui.
 *
 * ⚠️⚠️ **ESISTONO PERCHÉ MANCAVANO DAVVERO, non per gusto, e ogni volta è stato cercato e
 * non supposto.** Il cursore di testo dei programmi da tavolo non c'è in nessuno dei due set
 * (la cosa più vicina è la matita di `Icons.Default.Edit`, che dice 'modifica' e non
 * 'rinomina'); il **sole** non c'è in nessuna delle icone-immagine di Material, che hanno
 * tutte le sole montagne (verificato sui sorgenti di `image`, `photo`, `photo_library` e
 * `collections`, che portano la stessa spezzata a due cime e nessun disco); e una cartella
 * **tratteggiata** non esiste in nessun set, né potrebbe: un vettore di Android non ha un
 * tratteggio, e i trattini vanno disegnati uno per uno (vedi [dashed]).
 * ⚠️ Chi volesse aggiungere qui un glifo che Material ha già sta duplicando un disegno
 * mantenuto da altri, e prima o poi i due divergeranno.
 *
 * ⚠️⚠️ **[FolderPair] E [FolderPairDashed] NASCONO DALLO STESSO STAMPO, e non è eleganza**:
 * la richiesta dell'utente era *disegnane una uguale a quella di Copia ma con quella sotto
 * tratteggiata*, e due disegni separati sarebbero due cartelle diverse che si somigliano.
 * Uguali per costruzione, l'unica differenza che si vede è quella che porta il significato.
 * ⚠️ È anche la ragione per cui 'Copia' **non usa più** `Icons.Outlined.FolderCopy`: quella
 * era una cartella di Material accanto a una disegnata in casa, cioè due mani nello stesso
 * riquadro.
 *
 * ⚠️ **Il colore dichiarato è nero e non è un difetto**: `Icon` disegna il vettore con un
 * `ColorFilter.tint`, quindi la tinta che si vede è quella passata a `Icon` e questa non si
 * vede mai. È la stessa convenzione delle icone di Material, che dichiarano tutte nero.
 */
object Glyphs {

    /**
     * Il cursore di testo, cioè la I con le due lineette.
     *
     * ⚠️ **Di tratto e non di pieno**, a differenza di tutte le altre sei del riquadro: è
     * l'unica fatta di aria, ed è il baratto dichiarato quando l'utente l'ha scelta. Lo
     * spessore è 2, come le altre a 24dp, così non sembra più leggera.
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
     * ⚠️⚠️ **IL SOLE È LA RAGIONE PER CUI QUESTO GLIFO ESISTE.** `Icons.Outlined.PhotoLibrary`
     * ha già i due fogli e le montagne, e sarebbe bastata: ma nessuna icona-immagine di
     * Material ha il **disco del sole**, e la richiesta dell'utente lo nomina (*il classico
     * simbolo montagne più sole*), col permesso di disegnarne una se non esiste.
     * ⚠️ **Il foglio dietro è una L e non un rettangolo**, come in `PhotoLibrary`: con tratti
     * senza riempimento un rettangolo intero si vedrebbe **attraverso** quello davanti, e i
     * due si leggerebbero come una griglia invece che come due fogli. Della cornice dietro si
     * disegna solo quello che sporge.
     * ⚠️ **Le montagne hanno due cime**, come quelle di Material, e non una: alla misura vera
     * il profilo si impasta un poco, ma una cima sola legge 'triangolo' e non 'paesaggio'.
     * Provato disegnandolo alle due misure, non deciso a mente.
     */
    val PhotoPair: ImageVector by lazy {
        glyph(
            name = "PhotoPair",
            // Il sole, che è l'unico pezzo pieno di tutto il file.
            pieno = { disc(17.3f, 6.9f, SUN) }
        ) {
            // Il foglio dietro, in basso a sinistra: sporge di quattro unità per lato.
            moveTo(4f, 8f)
            verticalLineTo(20f)
            horizontalLineTo(17.5f)
            // La cornice davanti.
            roundRect(8f, 3f, 21f, 16f, 2f)
            // Le montagne.
            moveTo(9.6f, 13.7f)
            lineTo(11.9f, 10.1f)
            lineTo(13.5f, 12.3f)
            lineTo(15.7f, 9.6f)
            lineTo(19.4f, 13.7f)
        }
    }

    /** Due cartelle sovrapposte: 'Copia'. */
    val FolderPair: ImageVector by lazy {
        glyph("FolderPair") {
            polyline(FOLDER_BACK)
            folder()
        }
    }

    /**
     * Due cartelle sovrapposte con quella dietro **tratteggiata**: 'Sposta'.
     *
     * ⚠️ Il tratteggio sta sulla cartella **di dietro** e non su quella davanti, e il verso
     * conta: spostare vuol dire che l'originale non resta dov'era, quindi la cartella che
     * si svuota è quella da cui si parte, cioè quella in fondo.
     */
    val FolderPairDashed: ImageVector by lazy {
        glyph("FolderPairDashed") {
            dashed(FOLDER_BACK, DASH_ON, DASH_OFF)
            folder()
        }
    }

    /**
     * Il guscio di ogni glifo di questo file: un 24x24 con un tratto solo.
     *
     * ⚠️ Esiste perché i quattro glifi condividono misura, spessore e capocchie, e ripetere
     * quei numeri quattro volte vuol dire quattro occasioni di scriverne uno diverso. Chi
     * cambia lo spessore lo cambia una volta.
     */
    private fun glyph(
        name: String,
        /**
         * Un secondo tracciato, **pieno**, disegnato sotto quello di tratto.
         *
         * ⚠️ Serve al sole di [PhotoPair], e la ragione per cui non è un tratto è che un
         * disco fatto col tratto dipenderebbe da come il motore disegna una **capocchia
         * tonda su un segmento di lunghezza nulla**: in teoria è un disco, in pratica è un
         * comportamento che non si può verificare senza un telefono. Un tracciato pieno
         * disegna quello che dice, sempre.
         */
        pieno: (PathBuilder.() -> Unit)? = null,
        disegno: PathBuilder.() -> Unit
    ): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = SIZE,
            defaultHeight = SIZE,
            viewportWidth = GRID,
            viewportHeight = GRID
        ).apply {
            pieno?.let { path(fill = SolidColor(Color.Black), pathBuilder = it) }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = disegno
            )
        }.build()

    /**
     * Un disco pieno, approssimato con quattro quadratiche.
     *
     * ⚠️ Quattro quadratiche e non quattro cubiche: l'errore massimo di questa
     * approssimazione è il 5,6% del raggio, cioè **sei centesimi di unità** su un raggio di
     * 1,15 in una griglia da 24. Invisibile, e in cambio non ci sono flag di arco da
     * sbagliare (`isMoreThanHalf`, `isPositiveArc`), che sono il posto dove si sbaglia.
     */
    private fun PathBuilder.disc(cx: Float, cy: Float, r: Float) {
        moveTo(cx, cy - r)
        quadTo(cx + r, cy - r, cx + r, cy)
        quadTo(cx + r, cy + r, cx, cy + r)
        quadTo(cx - r, cy + r, cx - r, cy)
        quadTo(cx - r, cy - r, cx, cy - r)
        close()
    }

    /** Un rettangolo con gli angoli stondati, che `PathBuilder` non ha. */
    private fun PathBuilder.roundRect(x0: Float, y0: Float, x1: Float, y1: Float, r: Float) {
        moveTo(x0 + r, y0)
        horizontalLineTo(x1 - r)
        quadTo(x1, y0, x1, y0 + r)
        verticalLineTo(y1 - r)
        quadTo(x1, y1, x1 - r, y1)
        horizontalLineTo(x0 + r)
        quadTo(x0, y1, x0, y1 - r)
        verticalLineTo(y0 + r)
        quadTo(x0, y0, x0 + r, y0)
        close()
    }

    /** Una spezzata, dal primo punto all'ultimo. */
    private fun PathBuilder.polyline(punti: List<Pair<Float, Float>>) {
        moveTo(punti.first().first, punti.first().second)
        punti.drop(1).forEach { (x, y) -> lineTo(x, y) }
    }

    /** La cartella davanti, con la sua linguetta: la stessa nelle due coppie. */
    private fun PathBuilder.folder() {
        polyline(FOLDER_FRONT)
        close()
    }

    /**
     * La stessa spezzata, tagliata in trattini.
     *
     * ⚠️⚠️ **SERVE PERCHÉ UN VETTORE DI ANDROID NON SA TRATTEGGIARE**: `VectorDrawable` non
     * ha un `stroke-dasharray`, e `ImageVector` nemmeno. L'unico modo è disegnare i trattini
     * come sottotracciati separati, e l'unico modo sano di farlo è **calcolarli**: scriverli a
     * mano vorrebbe dire ricalcolare a mano venti coppie di coordinate ogni volta che si
     * ritocca la lunghezza di un trattino.
     * ⚠️ **Gira una volta sola**, quando il glifo si costruisce: i vettori di questo file
     * sono `by lazy`, quindi il conto si paga al primo disegno e mai più.
     * ⚠️ Il passo prosegue **fra un segmento e l'altro** della spezzata invece di ripartire a
     * ogni vertice: così l'angolo non prende sempre un trattino intero, che è quello che fa
     * sembrare un tratteggio disegnato invece che calcolato.
     */
    private fun PathBuilder.dashed(punti: List<Pair<Float, Float>>, acceso: Float, spento: Float) {
        var resto = acceso
        var disegna = true
        var da = punti.first()
        if (disegna) moveTo(da.first, da.second)
        for (a in 1 until punti.size) {
            val b = punti[a]
            val dx = b.first - da.first
            val dy = b.second - da.second
            val lungo = hypot(dx, dy)
            var fatto = 0f
            while (lungo - fatto > EPS) {
                val passo = minOf(resto, lungo - fatto)
                fatto += passo
                resto -= passo
                val x = da.first + dx * fatto / lungo
                val y = da.second + dy * fatto / lungo
                if (disegna) lineTo(x, y) else moveTo(x, y)
                if (resto <= EPS) {
                    disegna = !disegna
                    resto = if (disegna) acceso else spento
                }
            }
            da = b
        }
    }

    /** La griglia di Material: ogni icona del sistema è disegnata dentro un 24x24. */
    private const val GRID = 24f

    /** Lo spessore delle altre icone a 24dp, in unità di griglia. */
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
     * La cartella davanti: linguetta a sinistra, corpo a destra.
     *
     * ⚠️ Le coordinate stanno **fuori** dai due glifi che la usano, e non per risparmiare
     * righe: [FolderPair] e [FolderPairDashed] devono essere identiche tranne che nei
     * trattini, e due copie della stessa spezzata si separano al primo ritocco di una.
     */
    private val FOLDER_FRONT = listOf(
        7f to 16.5f, 7f to 5f, 11.5f to 5f, 13.5f to 7f, 21f to 7f, 21f to 16.5f
    )

    /** La cartella dietro, di cui si vede solo quello che sporge: una L. */
    private val FOLDER_BACK = listOf(3f to 8f, 3f to 19.5f, 17f to 19.5f)

    /**
     * Quanto è lungo un trattino e quanto lo stacco, in unità di griglia.
     *
     * ⚠️ **Lo stacco è il DOPPIO del trattino, e sembra sbagliato finché non si disegna**:
     * la capocchia tonda sporge di mezzo spessore per parte, cioè di 1 in tutto, quindi un
     * trattino di 1,6 si vede lungo 2,6 e uno stacco di 3,2 si vede lungo 2,2. Sulla carta
     * il rapporto è 1 a 2, a schermo è quasi 1 a 1, che è il tratteggio che si voleva.
     * Misurato disegnandolo alle due misure, non calcolato a mente.
     */
    private const val DASH_ON = 1.6f
    private const val DASH_OFF = 3.2f

    /**
     * Il raggio del sole, in unità di griglia.
     *
     * ⚠️ 1,15 e non 1: un disco pieno di raggio 1 accanto a tratti spessi 2 si legge come un
     * pallino sfuggito, non come un sole. Un filo più grosso del tratto lo rende un elemento
     * del disegno.
     */
    private const val SUN = 1.15f

    /** Sotto questo, una lunghezza è zero: serve a chiudere il ciclo di [dashed]. */
    private const val EPS = 1e-4f
}

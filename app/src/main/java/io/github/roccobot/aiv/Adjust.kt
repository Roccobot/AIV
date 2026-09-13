package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader.TileMode
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Shader
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*
 * ⚠️⚠️ **A CHE COSA SERVE QUESTO FILE: È IL CONTO DELL'EDITOR COMPLETO, E NE ESISTE UNA COPIA
 * SOLA.** L'editor di casa sa mettere un'immagine in posa e ritagliarla, cioè non tocca un pixel;
 * quello che l'utente ha chiesto (*luminosità/contrasto e tonalità/saturazione fatte BENE, curve,
 * raddrizzamento*) cambia invece ogni pixel, e per farlo mentre lui muove un cursore serve che il
 * conto giri sulla **scheda grafica**.
 *
 * ⚠️⚠️ **IL CONTO VIVE IN AGSL E NON ANCHE IN KOTLIN, ED È UNA DECISIONE DICHIARATA.** La via
 * comoda sarebbe scriverlo due volte: uno shader per l'anteprima, che dev'essere immediata, e un
 * giro sui pixel in Kotlin per il salvataggio, che deve lavorare sul file pieno. Sono **due
 * implementazioni della stessa matematica**, e il giorno che una cambia l'altra mente: l'utente
 * vedrebbe un'anteprima e salverebbe un'altra immagine, senza che niente dia errore. Quindi il
 * programma è uno, e il salvataggio lo fa girare fuori schermo (vedi `AdjustRender.kt`).
 * - ⚠️ **Il prezzo è dichiarato**: `RuntimeShader` nasce con Android 13, quindi sotto quella
 *   versione l'editor completo non c'è. È l'istruzione dell'utente dell'11 settembre, e là resta
 *   l'editor di casa, che non perde niente perché lavora sulla posa.
 * - ⚠️ **E il banco non può misurare i pixel che ne escono**: una prova gira senza scheda
 *   grafica. Quello che il banco misura è il modello e la struttura, ed è scritto nelle prove.
 *
 * ⚠️⚠️ **I CONTI SI FANNO IN LUCE LINEARE, NON SUI NUMERI DEL FILE.** Un valore sRGB non è la
 * quantità di luce: è quella quantità passata per una curva che imita l'occhio. Sommare o
 * moltiplicare là dentro dà i risultati sporchi che si vedono negli editor fatti male (un
 * contrasto che vira, un'esposizione che spegne i colori). Qui si va in lineare, si fa il conto,
 * e si torna: l'andata e il ritorno costano quattro `pow` per pixel, che sulla scheda grafica non
 * si sentono.
 *
 * ⚠️⚠️ **E I COLORI CHE ARRIVANO SONO PREMOLTIPLICATI**: `shader.eval` in Skia dà il colore già
 * moltiplicato per la propria opacità, quindi su un PNG con trasparenza un conto fatto così
 * com'è tratterebbe un pixel semitrasparente come un pixel scuro. Si divide per l'opacità prima e
 * si rimoltiplica dopo. Su una fotografia opaca la divisione è per uno e non cambia niente, ed è
 * la ragione per cui questo difetto non si vedrebbe provando.
 */

/**
 * Il modulo **Luce**: i sei valori che dicono quanta luce ha un'immagine e come è distribuita.
 *
 * ⚠️⚠️ **SONO I SEI DEL PANNELLO BASE DI LIGHTROOM, NELLO STESSO ORDINE, DALLA `2.16`, ED È IL SUO
 * RISCONTRO** (giro della `2.15`, voce `luce-taratura` accettabile: *'Luminosità', oltre a
 * confondermi (Lightroom ha solo 'Esposizione'), è anche ben poco 'smart' dato che l'output va da
 * 100% nero a 100% bianco*). Fino alla `2.15` erano cinque, e uno era **Luminosità**: un passo
 * additivo verso il bianco o verso il nero, che al fondo della corsa dava esattamente il
 * rettangolo bianco o nero che lui ha descritto. Al suo posto entrano i due che mancavano,
 * [whites] e [blacks], che spostano i **punti** dell'intervallo tonale invece di spingerci dentro
 * tutta l'immagine.
 * - ⚠️ **Non è una sostituzione alla pari, ed è la ragione per cui sono due**: la luminosità
 *   toccava tutto allo stesso modo, i punti toccano gli estremi e lasciano stare il resto. Quello
 *   che 'Luminosità' faceva bene lo fa [exposure], che è il cursore che Lightroom ha al suo posto.
 *
 * ⚠️⚠️ **OGNUNO FA UNA COSA CHE GLI ALTRI NON SANNO FARE**: [exposure] moltiplica la luce (è il
 * diaframma), [contrast] allarga o stringe la distanza fra scuri e chiari intorno al grigio medio,
 * [highlights] e [shadows] **recuperano** una fascia larga a un'estremità, [whites] e [blacks]
 * spostano il punto in cui l'immagine diventa bianca o nera. Le prime due toccano tutto, le altre
 * quattro lavorano su un'estremità, e le due coppie si distinguono per **quanto sono larghe**: una
 * fascia contro un punto.
 *
 * ⚠️ **L'unità di [exposure] è lo STOP**, cioè quella della fotografia: +1 vuol dire il doppio
 * della luce, -1 la metà. Gli altri cinque sono frazioni da -1 a +1, e l'interfaccia li mostra da
 * -100 a +100 perché è il linguaggio che lui conosce da Lightroom.
 */
data class Light(
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val whites: Float = 0f,
    val blacks: Float = 0f
) {

    /**
     * Se questo modulo non cambia un pixel.
     *
     * ⚠️ **Si confronta con una tolleranza e non con lo zero esatto**: un cursore lasciato
     * andare può fermarsi a un millesimo dallo zero, e quel millesimo non si vede ma
     * costringerebbe a riscrivere il file. La soglia è quella sotto cui il conto non muove
     * nemmeno un livello su 255.
     */
    val idle: Boolean
        get() = abs(exposure) < DEAD && abs(contrast) < DEAD && abs(highlights) < DEAD &&
            abs(shadows) < DEAD && abs(whites) < DEAD && abs(blacks) < DEAD

    /**
     * Il fattore per cui si moltiplica la luce, cioè due elevato agli stop.
     *
     * ⚠️ **Il conto vive qui e non nello shader**, perché uno stop è una convenzione della
     * fotografia e non un'operazione grafica: dentro il programma arriva un numero puro, e il
     * programma non ha bisogno di sapere che cosa sia uno stop.
     */
    val gain: Float get() = 2f.pow(exposure)

    companion object {
        val NONE = Light()

        /** Sotto questa soglia un cursore vale zero: vedi [idle]. */
        private const val DEAD = 0.0005f

        /** Quanti stop può coprire il cursore dell'esposizione, in su e in giù. */
        const val EXPOSURE_RANGE = 2f
    }
}

/**
 * Il modulo **Colore**: che tinta ha la luce, e quanto sono accesi i colori.
 *
 * ⚠️⚠️ **È IL SECONDO MODULO, DALLA `2.19`, ED È IL SUO CAMPO LIBERO** (giro della `2.18`: *vai
 * avanti con gli altri step dell'editor completo*). Dove la Luce dice **quanta** luce c'è e come è
 * distribuita, questo dice **di che colore** è: [temp] e [tint] rifanno il bilanciamento del
 * bianco, [saturation] e [vibrance] decidono quanto i colori sono accesi, [mono] li toglie.
 *
 * ⚠️⚠️ **SATURAZIONE E VIVIDEZZA NON SONO LO STESSO CURSORE PIÙ PIANO, e questa è la ragione per
 * cui sono due**: la saturazione muove tutti i colori allo stesso modo, quindi alzandola i colori
 * già accesi arrivano al limite e si impastano (una maglietta rossa diventa una macchia). La
 * vividezza pesa il suo effetto sull'**inverso** di quanto un colore è già saturo, quindi lavora
 * sui colori spenti e lascia stare quelli accesi: è il cursore che si usa sui ritratti, perché
 * l'incarnato è poco saturo e il cielo dietro no.
 *
 * ⚠️⚠️ **I PESI PER FASCIA DEL BIANCO E NERO NON SONO QUI, E DALLA `2.21` SI SA DOVE SONO**: il
 * piano d'azione li metteva in questo modulo, ma sono la **stessa macchina** delle otto fasce
 * dell'HSL, e adesso vivono là (vedi [Mix]), che è la sua risposta `hsl` a `d-bn-pesi`. Qui [mono]
 * usa i pesi percettivi di Rec. 709, cioè quelli con cui l'occhio vede il grigio, e chi vuole
 * scurire i cieli di una fotografia in bianco e nero muove la luminanza della fascia del blu.
 *
 * ⚠️ **Sono tutti frazioni da -1 a +1**, e l'interfaccia li mostra da -100 a +100 come quelli
 * della Luce: è il linguaggio di Lightroom, che è quello che lui conosce.
 */
data class Chroma(
    val temp: Float = 0f,
    val tint: Float = 0f,
    val saturation: Float = 0f,
    val vibrance: Float = 0f,
    val mono: Boolean = false
) {

    /** Se questo modulo non cambia un pixel: vedi la nota sulla tolleranza in [Light.idle]. */
    val idle: Boolean
        get() = !mono && abs(temp) < DEAD && abs(tint) < DEAD &&
            abs(saturation) < DEAD && abs(vibrance) < DEAD

    companion object {
        val NONE = Chroma()

        private const val DEAD = 0.0005f
    }
}

/**
 * Che cosa si chiede a **una** delle otto fasce di colore: spostane la tonalità, accendila o
 * spegnila, schiariscila o scuriscila.
 *
 * ⚠️ **Sono tre frazioni da -1 a +1**, come i cursori degli altri due moduli, e l'interfaccia le
 * mostra da -100 a +100: è il linguaggio di Lightroom, che è quello che lui conosce.
 */
data class Band(val hue: Float = 0f, val sat: Float = 0f, val lum: Float = 0f) {

    /** Se questa fascia non cambia un pixel: vedi la nota sulla tolleranza in [Light.idle]. */
    val idle: Boolean get() = abs(hue) < DEAD && abs(sat) < DEAD && abs(lum) < DEAD

    companion object {
        val NONE = Band()

        private const val DEAD = 0.0005f
    }
}

/**
 * Il modulo **HSL**: gli stessi tre comandi ripetuti su otto fasce di colore.
 *
 * ⚠️⚠️ **È IL TERZO MODULO, DALLA `2.21`, ED È IL SUO CAMPO LIBERO** (giro della `2.20`: *mi
 * sembra più logico implementare HSL dopo il colore, va' avanti con quello*). Prende il posto del
 * Dettaglio, che scala di un giro: fino a quel messaggio l'ordine era quello della sua risposta
 * `subito` a `d-dettaglio`, e questa istruzione lo rovescia.
 *
 * ⚠️⚠️ **DOVE IL MODULO COLORE PARLA A TUTTA L'IMMAGINE, QUESTO PARLA A UN COLORE SOLO, ed è
 * questo che lo rende un modulo a sé**: la saturazione del Colore accende tutto insieme, qui si
 * accende il cielo lasciando stare l'incarnato. La macchina è la stessa per tutte e otto le fasce,
 * e una fascia non toccata non costa niente.
 *
 * ⚠️⚠️ **E QUI DENTRO VIVONO ANCHE I PESI PER FASCIA DEL BIANCO E NERO, CHE È LA SUA RISPOSTA
 * `hsl` A `d-bn-pesi`** (giro della `2.19`): col bianco e nero acceso le prime due righe non hanno
 * più niente da fare e resta [lum], che diventa **quanto quel colore pesa nel grigio**. Non è un
 * secondo meccanismo che gli somiglia: è lo stesso conto, e il bianco e nero viene dopo di lui
 * proprio perché possa raccoglierne il risultato.
 *
 * ⚠️⚠️ **I CENTRI DELLE FASCE VIVONO QUI E NON NELLO SHADER, e non è una comodità**: li leggono in
 * due, il conto (per sapere a quale fascia appartiene un pixel) e l'interfaccia (per dare a ogni
 * pastiglia il colore della sua fascia). Scritti due volte, il giorno che uno si sposta la
 * pastiglia direbbe un colore che il conto non tocca.
 * - ⚠️ **Non sono equispaziati, ed è di proposito**: sono gli otto di Lightroom, dove fra il rosso
 *   e il giallo ci sono tre fasce in 60 gradi e fra il giallo e l'acqua due in 120. La ruota dei
 *   colori non è uniforme per l'occhio, e quei centri stanno dove l'occhio distingue.
 */
data class Mix(val bands: List<Band> = List(COUNT) { Band.NONE }) {

    /** Se nessuna fascia cambia un pixel. */
    val idle: Boolean get() = bands.all { it.idle }

    /** Questo stesso insieme con la fascia [i] riscritta da [how]. */
    fun swap(i: Int, how: (Band) -> Band): Mix =
        Mix(bands.mapIndexed { j, b -> if (j == i) how(b) else b })

    companion object {
        val NONE = Mix()

        /** Quante fasce: otto, come il pannello di Lightroom. */
        const val COUNT = 8

        /**
         * Dov'è il centro di ogni fascia, in frazione di giro (0 = rosso).
         *
         * ⚠️ **È un `FloatArray` perché finisce tale e quale in un uniform**: `setFloatUniform`
         * vuole quello, e una conversione a ogni fotogramma sarebbe una copia per niente.
         */
        val CENTRES = floatArrayOf(
            0f, 30f / 360f, 60f / 360f, 120f / 360f,
            180f / 360f, 240f / 360f, 280f / 360f, 320f / 360f
        )

        /**
         * Fin dove arriva una fascia dalla parte delle tonalità più alte, e da quella delle più
         * basse: esattamente fino al centro vicino.
         *
         * ⚠️⚠️ **SI RICAVANO DAI CENTRI E NON SI SCRIVONO, ed è quello che fa tornare il conto**:
         * con un raggio uguale alla distanza dal vicino, fra due centri adiacenti i due pesi
         * sommano a uno e tutti gli altri valgono zero, quindi il conto non ha bisogno di
         * normalizzare niente. Il perché un raggio unico non basti vive su `pick`, nello shader.
         */
        val SPAN_HI = FloatArray(COUNT) { round(CENTRES[(it + 1) % COUNT] - CENTRES[it]) }
        val SPAN_LO = FloatArray(COUNT) { round(CENTRES[it] - CENTRES[(it + COUNT - 1) % COUNT]) }

        /** Una differenza di tonalità riportata in un giro, cioè in `[0, 1)`. */
        private fun round(x: Float): Float = x - floor(x)

        /**
         * A quale fascia appartiene il colore [pixel], cioè quale centro è il più vicino alla sua
         * tonalità, oppure `-1` se quel colore è un grigio.
         *
         * ⚠️⚠️ **SERVE AL COLORE MIRATO, E IL GRIGIO NON È UN CASO LIMITE DA CHIUDERE CON UNO
         * ZERO**: un pixel senza colore non appartiene a nessuna fascia (è esattamente la ragione
         * per cui il conto dello shader lo lascia stare), quindi rispondere 'rosso' vorrebbe dire
         * mandare il dito su una fascia che con quel pixel non c'entra. Chi chiama non fa niente.
         *
         * ⚠️ **La tonalità si ricava qui e non si chiede allo shader**: quello gira sulla scheda
         * grafica e non risponde a domande, e il conto è di sei righe.
         */
        fun bandOf(pixel: Int): Int {
            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f
            val top = max(r, max(g, b))
            val bottom = min(r, min(g, b))
            val span = top - bottom
            if (span < 0.004f) return -1
            val sixth = when {
                top == r -> ((g - b) / span + 6f) % 6f
                top == g -> (b - r) / span + 2f
                else -> (r - g) / span + 4f
            }
            val hue = sixth / 6f
            var best = 0
            var near = 1f
            for (i in 0 until COUNT) {
                val s = hue - CENTRES[i]
                val d = abs(s - floor(s + 0.5f))
                if (d < near) {
                    near = d
                    best = i
                }
            }
            return best
        }
    }
}

/**
 * Il modulo **Dettaglio**: quanto il disegno fine si accentua, e quanto rumore si toglie.
 *
 * ⚠️⚠️ **È IL QUARTO MODULO, DALLA `2.22`, E SONO LE DUE FUNZIONI CHE HA CHIESTO LUI** (campo
 * libero del giro della `2.15`: *'Maschera di contrasto' e 'Riduzione rumore'*). Dove gli altri tre
 * moduli parlano del **colore** di un pixel, questo parla del suo **intorno**: sono le prime due
 * operazioni dell'editor che guardano i pixel vicini, e da qui viene tutto quello che costano.
 *
 * ⚠️⚠️ **LE MISURE SONO FRAZIONI DEL LATO E NON PIXEL, ED È QUESTO CHE TIENE INSIEME L'ANTEPRIMA E
 * IL FILE SALVATO.** Il conto gira su due immagini di misura diversa: l'anteprima, che l'editor
 * riduce per lavorare in fretta, e il file pieno, che il salvataggio lavora a tessere. Un raggio
 * scritto in pixel darebbe due risultati diversi (sull'anteprima, ridotta due volte e mezzo,
 * peserebbe più del doppio); scritto come frazione del lato, ognuno dei due lo converte con la
 * **propria** misura e i due risultati coincidono in proporzione, senza nessun secondo dato da
 * tenere allineato.
 * - ⚠️ **Quello che resta fuori si dichiara**: l'anteprima è già una riduzione, quindi la grana
 *   fine del sensore là è **già stata mediata**, e la riduzione del rumore si giudica davvero sul
 *   file salvato. È lo stesso limite per cui in un editor da tavolo la nitidezza si guarda al 100%.
 *
 * ⚠️⚠️ **DUE CURSORI SU CINQUE NON CAMBIANO UN PIXEL DA SOLI, ed è la ragione per cui [idle] ne
 * guarda tre**: [radius] e [masking] non sono quantità, sono **come** la maschera di contrasto
 * lavora. Con [sharpen] a zero non c'è nessuna maschera da governare, e infatti l'interfaccia li
 * spegne.
 *
 * ⚠️ **[radius] è l'unico bipolare dei cinque**: lo zero è il raggio di serie, e la corsa lo
 * raddoppia o lo dimezza. Gli altri quattro partono da zero perché 'nessuna nitidezza' e 'nessuna
 * riduzione' sono il loro fondo naturale: una nitidezza negativa sarebbe una sfocatura, e una
 * riduzione del rumore negativa non vuol dire niente.
 */
data class Detail(
    val sharpen: Float = 0f,
    val radius: Float = 0f,
    val masking: Float = 0f,
    val noise: Float = 0f,
    val noiseColor: Float = 0f
) {

    /** Se questo modulo non cambia un pixel: vedi la nota sui due cursori che non lavorano. */
    val idle: Boolean
        get() = abs(sharpen) < DEAD && abs(noise) < DEAD && abs(noiseColor) < DEAD

    /** Se la maschera di contrasto è spenta, cioè se [radius] e [masking] non governano niente. */
    val flat: Boolean get() = abs(sharpen) < DEAD

    /**
     * Il raggio della maschera di contrasto, in unità dello spazio in cui il conto gira, dato il
     * lato lungo dell'immagine [long] nello stesso spazio.
     *
     * ⚠️ **Il cursore raddoppia e dimezza invece di sommare**, perché un raggio si percepisce in
     * rapporti: a zero vale [SHARP_SPAN] del lato, che su un file da quattromila pixel sono quattro
     * pixel, cioè il micro-contrasto; al fondo della corsa si va da due a otto, che è il tratto
     * fra la nitidezza di cattura e la chiarezza.
     */
    fun sharpReach(long: Float): Float = SHARP_SPAN * long * 2f.pow(radius)

    /**
     * Quanti pixel di sovrapposizione vuole una tessera del salvataggio, dato il lato lungo
     * dell'immagine **intera**.
     *
     * ⚠️⚠️ **SI RICAVA DAL CONTO E NON È UN NUMERO SCRITTO A MANO**: il bordo serve perché il
     * filtro legge i vicini, quindi è esattamente quanto il filtro arriva lontano. Un numero fisso
     * sarebbe troppo piccolo sulle immagini grandi (una riga sulle giunzioni) o sprecato su quelle
     * piccole.
     * - ⚠️ **Il pixel in più copre il campionamento bilineare**, che a coordinate frazionarie legge
     *   un pixel oltre quello che il raggio dichiara.
     * - ⚠️ **A modulo spento vale zero**, quindi chi non usa questa funzione non paga niente: le
     *   tessere tornano quelle di prima.
     */
    fun bleed(long: Float): Int {
        if (idle) return 0
        var reach = 0f
        if (abs(sharpen) >= DEAD) reach = max(reach, sharpReach(long))
        if (abs(noise) >= DEAD || abs(noiseColor) >= DEAD) reach = max(reach, grainReach(long))
        return ceil(reach).toInt() + 1
    }

    companion object {
        val NONE = Detail()

        private const val DEAD = 0.0005f

        /**
         * Il raggio di serie della maschera di contrasto, in frazione del lato lungo.
         *
         * ⚠️ **Su un file da quattromila pixel sono quattro pixel**, cioè un micro-contrasto che si
         * vede anche guardando l'immagine intera. Un raggio da nitidezza di cattura (un pixel) si
         * vede solo ingrandendo, e un cursore che a occhio non fa niente si legge come rotto: qui
         * quel raggio c'è, ed è il fondo corsa di [radius].
         */
        const val SHARP_SPAN = 1f / 1000f

        /**
         * Il vicinato della riduzione del rumore, in frazione del lato lungo.
         *
         * ⚠️ **Non dipende da [radius], ed è una scelta**: il raggio governa **come** si accentua
         * il disegno, mentre il rumore vuole sempre lo stesso intorno stretto. Legarli vorrebbe
         * dire che chi cerca la chiarezza si ritrova un'immagine spianata.
         */
        const val GRAIN_SPAN = 1f / 1200f

        /** Il vicinato della riduzione del rumore: vedi [GRAIN_SPAN]. */
        fun grainReach(long: Float): Float = GRAIN_SPAN * long
    }
}

/**
 * Un punto di una curva tonale: il tono che **entra** e il tono che **esce**, tutti e due in
 * `[0, 1]`.
 *
 * ⚠️ **I due estremi hanno [at] fisso a 0 e a 1**, e si muovono solo in [to]: una curva tonale
 * deve dire che cosa fare di **ogni** tono, e un primo punto a mezza scala lascerebbe la prima
 * metà senza risposta.
 */
data class Knot(val at: Float, val to: Float)

/**
 * Una curva tonale: i suoi punti, e la tabella di 256 valori che se ne ricava.
 *
 * ⚠️⚠️ **IL CONTO CHE PASSA ALLA SCHEDA GRAFICA È UNA TABELLA E NON UN PROGRAMMA, ED È LA
 * DECISIONE CHE REGGE TUTTO IL RESTO.** Gli altri quattro moduli mandano allo shader dei **numeri**
 * (un guadagno, un raggio) e il conto vive tutto in AGSL, che è la regola scritta in testa a questo
 * file; una spline invece vuole un ciclo sui punti per ogni pixel, e i punti sono in numero
 * variabile. Scritta in AGSL costerebbe quel ciclo venti milioni di volte per un risultato che
 * dipende **solo** dal livello in ingresso, cioè da 256 valori possibili: la si calcola una volta e
 * si consegna come una riga di 256 pixel.
 * - ⚠️⚠️ **NON È LA SECONDA COPIA CHE QUESTO FILE ESISTE PER NON AVERE**, e la distinzione è
 *   precisa: una seconda copia sarebbe lo **stesso conto** scritto due volte, una per l'anteprima e
 *   una per il salvataggio, cioè due cose che possono divergere. Qui il conto è **uno** e vive qui;
 *   quello che va in AGSL è una lettura della sua tabella, e la stessa tabella la leggono
 *   l'anteprima, il salvataggio e il grafico che la disegna. Una fonte, tre lettori.
 *
 * ⚠️⚠️ **LA SPLINE È MONOTONA (Fritsch-Carlson) E NON UNA CUBICA NATURALE, E NON È UN DETTAGLIO
 * DI QUALITÀ**: una cubica naturale **oltrepassa** fra due punti vicini, quindi con un punto alzato
 * di poco la curva scende sotto il suo vicino, cioè un tono più chiaro esce più scuro di quello
 * accanto. Sull'immagine si vede come un anello di tono invertito, ed è il difetto classico delle
 * curve fatte male. La correzione monotona limita le tangenti e quel caso non esiste per
 * costruzione.
 * - ⚠️ **Su punti allineati la spline è ESATTAMENTE la retta**: le secanti valgono tutte la stessa
 *   pendenza, le tangenti diventano quella, e l'Hermite fra due punti con quei valori dà il
 *   segmento. È la ragione per cui [idle] può guardare i soli punti invece di confrontare 256
 *   valori.
 */
data class Curve(val knots: List<Knot> = ENDS) {

    /**
     * Se questa curva non cambia un tono, cioè se ogni punto è sulla diagonale.
     *
     * ⚠️ **Si guardano i PUNTI e non la tabella**, e regge sulla proprietà scritta qui sopra: una
     * spline monotona per punti allineati **è** la retta, quindi punti sulla diagonale vogliono
     * dire tabella identità, senza doverla calcolare per scoprirlo.
     */
    val idle: Boolean get() = knots.all { abs(it.to - it.at) < DEAD }

    /**
     * Questa curva col punto [i] portato in ([at], [to]), tenuto dentro l'intervallo e fra i suoi
     * vicini.
     *
     * ⚠️⚠️ **I DUE ESTREMI NON SI MUOVONO IN ORIZZONTALE, ed è la specifica del tipo e non una
     * prudenza**: una curva tonale deve dire che cosa fare di ogni tono, quindi il primo punto sta
     * a zero e l'ultimo a uno per costruzione. Di loro si muove la sola uscita, che è il modo di
     * alzare i neri o chiudere i bianchi.
     *
     * ⚠️ **Gli altri non scavalcano i vicini**: due punti alla stessa ascissa darebbero un tratto
     * di larghezza zero, cioè una divisione per zero nella spline, e due punti in ordine invertito
     * una curva che torna indietro. Il margine è [GAP], che è anche la distanza sotto la quale due
     * punti non si distinguerebbero col dito.
     */
    fun move(i: Int, at: Float, to: Float): Curve = copy(
        knots = knots.toMutableList().also {
            val x = when (i) {
                0 -> 0f
                it.size - 1 -> 1f
                else -> at.coerceIn(it[i - 1].at + GAP, it[i + 1].at - GAP)
            }
            it[i] = Knot(x, to.coerceIn(0f, 1f))
        }
    )

    /**
     * L'indice del punto che sta a un dito da [at], oppure `-1` se là non c'è niente.
     *
     * ⚠️ **La soglia è la stessa di [grow]**, ed è quello che tiene insieme i due gesti del
     * grafico: quello che il tocco lungo toglie è esattamente il punto che il trascinamento
     * avrebbe preso.
     */
    fun nearest(at: Float): Int = knots.indexOfFirst { abs(it.at - at) < NEAR }

    /**
     * Questa curva con un punto in più a [at], oppure com'è se là ce n'è già uno o se il tetto è
     * raggiunto. Il secondo valore dice dove è finito il punto da prendere col dito.
     *
     * ⚠️ **Il punto nuovo nasce SULLA curva e non dove il dito ha toccato**: chi tocca il grafico
     * vuole prendere quella curva in quel punto, e farla saltare al dito nell'istante in cui la si
     * prende vorrebbe dire un movimento che nessuno ha chiesto.
     *
     * ⚠️ **Il tetto è [MAX_KNOTS] e si dichiara**: oltre una dozzina di punti una curva tonale non
     * si governa più col dito, e ogni punto in più costa un tratto di spline in cui la monotonia
     * limita le tangenti, cioè una curva che si irrigidisce da sé.
     */
    fun grow(at: Float): Pair<Curve, Int> {
        val x = at.coerceIn(0f, 1f)
        val near = nearest(x)
        if (near >= 0) return this to near
        if (knots.size >= MAX_KNOTS) return this to knots.indexOfFirst { abs(it.at - x) < 0.5f }
            .coerceAtLeast(0)
        val where = knots.indexOfFirst { it.at > x }.let { if (it < 0) knots.size else it }
        val grown = knots.toMutableList().also {
            it.add(where, Knot(x, valueAt(x)))
        }
        return copy(knots = grown) to where
    }

    /**
     * Questa curva senza il punto [i], oppure com'è se [i] è uno dei due estremi.
     *
     * ⚠️ **Gli estremi non si tolgono**: senza di loro la curva non direbbe più che cosa fare dei
     * toni fuori dal primo e dall'ultimo punto.
     */
    fun drop(i: Int): Curve =
        if (i <= 0 || i >= knots.size - 1) this
        else copy(knots = knots.toMutableList().also { it.removeAt(i) })

    /** Quanto vale questa curva nel tono [x], letto dalla sua tabella. */
    fun valueAt(x: Float): Float =
        table()[(x.coerceIn(0f, 1f) * (SIZE - 1)).roundToInt()]

    /**
     * I 256 valori di questa curva, uno per livello in ingresso.
     *
     * ⚠️ **Il conto è quello di Fritsch-Carlson**, in tre passi: le pendenze secanti fra punti
     * vicini, le tangenti come loro media, e la **correzione** che tiene la curva monotona
     * (tangenti azzerate dove la secante è piatta, e limitate al cerchio di raggio 3 dove
     * altrimenti la cubica oltrepasserebbe). Senza il terzo passo questa sarebbe una cubica
     * naturale, cioè la curva che il KDoc del tipo esiste per escludere.
     */
    fun table(): FloatArray {
        val n = knots.size
        val out = FloatArray(SIZE)
        if (n < 2) {
            val flat = (knots.firstOrNull()?.to ?: 0f).coerceIn(0f, 1f)
            return out.also { it.fill(flat) }
        }
        val d = FloatArray(n - 1) { i ->
            val dx = knots[i + 1].at - knots[i].at
            if (dx > 1e-6f) (knots[i + 1].to - knots[i].to) / dx else 0f
        }
        val m = FloatArray(n)
        m[0] = d[0]
        m[n - 1] = d[n - 2]
        for (i in 1 until n - 1) m[i] = (d[i - 1] + d[i]) / 2f
        for (i in 0 until n - 1) {
            if (abs(d[i]) < 1e-6f) {
                m[i] = 0f
                m[i + 1] = 0f
                continue
            }
            val a = m[i] / d[i]
            val b = m[i + 1] / d[i]
            if (a < 0f) m[i] = 0f
            if (b < 0f) m[i + 1] = 0f
            val s = a * a + b * b
            if (s > 9f) {
                val t = 3f / sqrt(s)
                m[i] = t * a * d[i]
                m[i + 1] = t * b * d[i]
            }
        }
        var seg = 0
        for (k in 0 until SIZE) {
            val x = k.toFloat() / (SIZE - 1)
            while (seg < n - 2 && x > knots[seg + 1].at) seg++
            val h = knots[seg + 1].at - knots[seg].at
            val y = if (h <= 1e-6f) {
                knots[seg + 1].to
            } else {
                val t = ((x - knots[seg].at) / h).coerceIn(0f, 1f)
                val t2 = t * t
                val t3 = t2 * t
                (2f * t3 - 3f * t2 + 1f) * knots[seg].to +
                    (t3 - 2f * t2 + t) * h * m[seg] +
                    (-2f * t3 + 3f * t2) * knots[seg + 1].to +
                    (t3 - t2) * h * m[seg + 1]
            }
            out[k] = y.coerceIn(0f, 1f)
        }
        return out
    }

    companion object {
        /** Quanti valori ha una tabella: uno per livello di un file a 8 bit. */
        const val SIZE = 256

        /** Il tetto dei punti di una curva: vedi [grow]. */
        const val MAX_KNOTS = 16

        /** I due estremi, cioè la curva che non fa niente. */
        val ENDS = listOf(Knot(0f, 0f), Knot(1f, 1f))

        val NONE = Curve()

        private const val DEAD = 0.002f

        /**
         * Quanto vicino a un punto esistente un tocco vale 'prendi quello' invece di 'fanne uno
         * nuovo': un ventesimo di scala, cioè un dito su un grafico largo un palmo.
         */
        private const val NEAR = 0.05f

        /** La distanza minima fra due punti vicini: vedi [move]. */
        private const val GAP = 0.01f
    }
}

/**
 * Il modulo **Curve**: la curva di tutti i toni e le tre dei canali.
 *
 * ⚠️⚠️ **LE QUATTRO CURVE SI COMPONGONO IN TRE TABELLE, E L'ORDINE È `all(canale(v))`**: è la
 * convenzione di ogni editor che ha questo pannello (prima la curva del canale, poi quella del
 * composito), e con l'ordine rovesciato una curva sul rosso cambierebbe di posto ogni volta che si
 * tocca quella di tutti i toni.
 * - ⚠️⚠️ **LA COMPOSIZIONE SI FA IN KOTLIN E NON SULLA SCHEDA GRAFICA**, ed è quello che rende il
 *   conto per pixel **una lettura sola per canale**: comporre là vorrebbe dire due letture per
 *   canale e la stessa risposta.
 *
 * ⚠️ **Il canale che si sta guardando non vive qui**: è dove si ha lo sguardo, come il modulo e la
 * fascia dell'HSL, quindi vive nella scheda e non entra nella storia dei passi.
 */
data class Tone(
    val all: Curve = Curve.NONE,
    val red: Curve = Curve.NONE,
    val green: Curve = Curve.NONE,
    val blue: Curve = Curve.NONE
) {

    /** Se nessuna delle quattro curve cambia un tono. */
    val idle: Boolean get() = all.idle && red.idle && green.idle && blue.idle

    /** La curva del canale [i], nell'ordine in cui la fila dei canali li disegna. */
    fun curve(i: Int): Curve = when (i) {
        RED -> red
        GREEN -> green
        BLUE -> blue
        else -> all
    }

    /** Questo modulo con la curva del canale [i] passata per [edit]. */
    fun swap(i: Int, edit: (Curve) -> Curve): Tone = when (i) {
        RED -> copy(red = edit(red))
        GREEN -> copy(green = edit(green))
        BLUE -> copy(blue = edit(blue))
        else -> copy(all = edit(all))
    }

    /**
     * La tabella da consegnare alla scheda grafica: 256 colori opachi, in cui ogni canale porta la
     * propria curva **già composta** con quella di tutti i toni.
     *
     * ⚠️⚠️ **I COLORI SONO OPACHI DI PROPOSITO, e non è una svista sull'opacità**: Skia consegna a
     * uno shader i colori **premoltiplicati**, quindi una tabella con un'opacità qualunque
     * arriverebbe moltiplicata per lei, cioè con dei valori che non sono quelli che ci si è messi.
     * Con l'opacità piena la moltiplicazione è per uno. È la stessa trappola scritta in testa a
     * questo file per l'immagine.
     */
    fun lut(): IntArray {
        val base = all.table()
        val r = red.table()
        val g = green.table()
        val b = blue.table()
        return IntArray(Curve.SIZE) { i ->
            val top = Curve.SIZE - 1
            val rr = (base[(r[i] * top).roundToInt()] * 255f).roundToInt().coerceIn(0, 255)
            val gg = (base[(g[i] * top).roundToInt()] * 255f).roundToInt().coerceIn(0, 255)
            val bb = (base[(b[i] * top).roundToInt()] * 255f).roundToInt().coerceIn(0, 255)
            (0xFF shl 24) or (rr shl 16) or (gg shl 8) or bb
        }
    }

    companion object {
        val NONE = Tone()

        /** Gli indici dei canali, nell'ordine della fila: tutti i toni, rosso, verde, blu. */
        const val WHOLE = 0
        const val RED = 1
        const val GREEN = 2
        const val BLUE = 3

        /** Quanti canali ha questo modulo. */
        const val COUNT = 4

        /**
         * A quale livello della curva del canale [channel] sta il colore [pixel].
         *
         * ⚠️⚠️ **SERVE AL COLORE MIRATO, E PER IL COMPOSITO È LA LUMINANZA PERCETTIVA**: chi tocca
         * un punto dell'immagine indica un **tono**, e il tono di un pixel colorato è quanto quel
         * pixel sembra chiaro, non la media dei suoi canali. Coi pesi di Rec. 709 un giallo pieno
         * cade in alto e un blu pieno in basso, che è dove l'occhio li vede.
         *
         * ⚠️ **Per i tre canali invece è il canale**, senza pesi: là la curva governa quel canale, e
         * il punto da prendere è il suo.
         */
        fun levelOf(pixel: Int, channel: Int): Float {
            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f
            return when (channel) {
                RED -> r
                GREEN -> g
                BLUE -> b
                else -> 0.2126f * r + 0.7152f * g + 0.0722f * b
            }
        }
    }
}

/**
 * Tutto quello che l'editor completo sa fare a un'immagine, in un oggetto solo.
 *
 * ⚠️⚠️ **È UN VALORE E NON UNA CATENA DI GESTI, ed è la stessa scelta dell'editor di casa**: là
 * dieci rotazioni si compongono in una posa sola perché le pose sono otto; qui dieci
 * spostamenti di un cursore sono **un** valore di quel cursore. Un elenco di gesti costringerebbe
 * a riapplicarli uno per uno sul file pieno, cioè a rifare dieci volte lo stesso lavoro.
 *
 * ⚠️⚠️ **CRESCE COI MODULI E LA SUA FORMA NON CAMBIA**: [chroma] è entrato accanto a [light] con
 * la `2.19`, [mix] con la `2.21`, [detail] con la `2.22`, [tone] con la `2.23` e [geo] con la
 * `2.29`, senza toccare niente di quello che legge questo oggetto. Chi ne aggiunge uno tocca
 * [idle] e [lossless], che sono le due domande che tutto il resto fa qui, e nient'altro.
 *
 * ⚠️⚠️ **MA [geo] NON È UN MODULO COME GLI ALTRI CINQUE, ED È LA RAGIONE PER CUI ESISTE [plain]**:
 * quelli dicono di che **colore** è un pixel e passano dallo shader, questo dice **dove** va e
 * passa da una maglia di triangoli (vedi `Geometry.kt`). Le due strade sono due passate distinte,
 * quindi chi disegna deve poter chiedere se **quella** passata serve, e non solo se l'immagine è
 * intonsa.
 */
data class Look(
    /**
     * La posa e il rettangolo tenuto, cioè il modulo **Ritaglio**, dalla `2.31`.
     *
     * ⚠️⚠️ **SONO I DUE CHE NON RISCRIVONO I PIXEL, E PER QUESTO VENGONO PRIMA DI TUTTO**: girare
     * un'immagine di un quarto di giro o rifletterla è una permutazione, e tagliare è una
     * sottrazione. Messi in testa, il Dettaglio legge ancora i pixel del file (una permutazione
     * non interpola) e i due keystone lavorano sugli assi **che si vedono** invece che su quelli
     * dell'originale, che dopo un quarto di giro sono scambiati.
     */
    val spin: Spin = Spin.STILL,
    val crop: ImageEdit.Crop = ImageEdit.Crop.WHOLE,
    val light: Light = Light.NONE,
    val chroma: Chroma = Chroma.NONE,
    val mix: Mix = Mix.NONE,
    val detail: Detail = Detail.NONE,
    val tone: Tone = Tone.NONE,
    val geo: Geometry = Geometry.NONE
) {

    /**
     * Se il conto del **colore** non cambia un pixel, cioè se lo shader non ha niente da fare.
     *
     * ⚠️ **Non è [idle] e la differenza non è una sfumatura**: un'immagine con la sola geometria
     * mossa non è intonsa (i pixel si spostano), ma il programma dello shader là non serve, e
     * farlo girare per niente costerebbe una passata intera su ogni fotogramma dell'anteprima.
     */
    val plain: Boolean
        get() = light.idle && chroma.idle && mix.idle && detail.idle && tone.idle

    /** Se il modulo Ritaglio non tocca niente: nessuna posa e nessun taglio. */
    val square: Boolean
        get() = spin == Spin.STILL && crop.whole

    /** Se non c'è niente da applicare: l'immagine esce identica a com'è entrata. */
    val idle: Boolean
        get() = plain && geo.idle && square

    /**
     * Se quello che c'è da fare **non** riscrive i pixel.
     *
     * ⚠️⚠️ **È LA CLAUSOLA DELL'UTENTE, e per questo è una proprietà del modello e non una
     * riga nel salvataggio** (*quelle che non prevedono la riscrittura del file pixel per pixel
     * devono essere lossless*): finché la pila contiene solo posa e ritaglio senza taglio, il
     * file si può girare cambiando un tag EXIF, che è quello che l'editor di casa fa dalla
     * `1.03`. Appena entra un valore di Luce, i pixel vanno riscritti e non c'è modo di
     * evitarlo.
     * ⚠️ **E con la geometria, dalla `2.29`, la risposta è la stessa**: raddrizzare ricampiona,
     * cioè decide per ogni pixel di arrivo un colore che prima non stava là.
     * ⚠️⚠️ **MA DALLA `2.31` LA POSA VIVE ANCHE QUI, E CON LEI TORNA IL SENZA PERDITA**: girare un
     * JPEG è un tag EXIF, e la nota di prima diceva che quella strada vive 'nell'altro editor'
     * perché fino alla `2.30` questo non sapeva mettere in posa. Adesso lo sa, e col solo modulo
     * Ritaglio mosso il salvataggio **passa da quella strada**, cioè da `ImageEdit.save`: una
     * seconda copia di quel conto sarebbe un modo per divergere.
     * ⚠️ **Un taglio invece riscrive**, e non è una scelta: un ritaglio a blocchi lascerebbe il
     * bordo al multiplo di otto più vicino, cioè non taglierebbe dove l'utente ha chiesto.
     */
    val lossless: Boolean get() = plain && geo.idle && crop.whole

    companion object {
        val NONE = Look()
    }
}

/**
 * Con quanta cura si riscrive il file, ed è una sua scelta a tre.
 *
 * ⚠️⚠️ **NON SONO TRE GRADI DELLA STESSA COSA: LE PRIME DUE SONO UN JPEG, LA TERZA UN ALTRO
 * FORMATO.** 'Alta' e 'Massima' sono lo stesso codificatore spinto di più, e fra loro la
 * differenza a occhio quasi non c'è mentre il file quasi raddoppia; 'Senza perdita' cambia
 * mestiere, scrive un PNG e non butta via un bit, al prezzo di un file parecchie volte più
 * grosso. La terza si sceglie quando l'immagine si dovrà rilavorare ancora.
 *
 * ⚠️ **Il valore vive nelle impostazioni e non si chiede a ogni salvataggio**: salvare è un
 * gesto che si fa di fretta, e una domanda in mezzo lo rallenterebbe ogni volta per una
 * decisione che si prende una volta sola. È la stessa lettura che ha avuto 'Scarica'.
 */
enum class Quality(override val token: String) : Choice {
    HIGH("alta"),
    MAX("massima"),
    LOSSLESS("senza-perdita");

    companion object {
        /** Il valore di fabbrica: vedi il KDoc del campo in `Settings`. */
        val DEFAULT = HIGH
    }
}

/**
 * Il programma che gira **su ogni pixel** dell'immagine.
 *
 * ⚠️⚠️ **L'ORDINE DELLE OPERAZIONI È LA SPECIFICA, e cambiarlo cambia il risultato**: il Dettaglio,
 * poi il bilanciamento del bianco, poi l'esposizione, poi ombre e luci, poi i punti di bianco e di
 * nero, poi il contrasto, poi la curva tonale, poi l'HSL per fascia, e per ultimo quanto sono
 * accesi i colori. È l'ordine di un banco di sviluppo fotografico, e la ragione di ognuno dei
 * passaggi:
 * - **Il Dettaglio viene per primo, dalla `2.22`**, perché è il solo modulo che parla del **file**
 *   e non dell'immagine: quanto rumore ha il sensore, e quanto il disegno fine va accentuato.
 *   Messo dopo, il contrasto avrebbe già moltiplicato la grana che quel modulo esiste per togliere.
 * - **Il bilanciamento viene per primo, dalla `2.19`**, perché non corregge niente: dice di che
 *   colore era la luce dello scatto, cioè **da quale immagine si parte**. Messo dopo, la piega
 *   delle alte luci lavorerebbe su un canale che il bilanciamento sta ancora per spingere fuori.
 * - **Saturazione e vividezza vengono per ultime** perché sono un giudizio sull'immagine finita:
 *   messe prima, ogni cursore della Luce le rimetterebbe in discussione, e alzare il contrasto
 *   alzerebbe di suo anche la saturazione.
 * - **L'esposizione viene prima** perché è l'unica moltiplicativa pura: è come aver aperto di più
 *   il diaframma, quindi tutto quello che segue lavora sull'immagine 'come sarebbe stata'.
 *   ⚠️⚠️ **E dalla `2.18` si porta dietro la PIEGA delle alte luci**, perché è lei a portare la
 *   luce fuori dalla scala: quello che sforava veniva schiacciato sul bianco, e adesso si
 *   comprime. Il perché, le misure e il costo dichiarato vivono su `shoulder`, qui sotto. ⚠️ Con
 *   lei tutto quello che segue riceve un segnale già dentro l'intervallo, maschere comprese.
 * - **Ombre e luci vengono prima del contrasto** perché servono a **recuperare** quello che
 *   l'esposizione ha schiacciato, e il contrasto deve poi lavorare su un'immagine già recuperata.
 *   Al contrario, si recupererebbe quello che il contrasto ha appena bruciato.
 * - **I punti vengono prima del contrasto** perché dichiarano **dove finisce** l'immagine, e la
 *   curva a S lavora dentro l'intervallo che quei due estremi definiscono. Al contrario, i punti
 *   taglierebbero i toni che la curva ha appena creato.
 *
 * ⚠️⚠️ **IL CONTRASTO HA UN PERNO E NON È UNA MOLTIPLICAZIONE**: `(c - 0.5) * k + 0.5` fatto in
 * lineare sposterebbe il grigio medio, perché il grigio medio in luce lineare **non** è 0,5 ma
 * circa 0,18. Il perno è quello, ed è la ragione per cui alzando il contrasto l'immagine non si
 * scurisce tutta.
 * ⚠️ **E si usa una curva a S invece di una retta**: una retta ripida taglia i due estremi, cioè
 * brucia i bianchi e chiude i neri. La forma qui sotto tende agli estremi senza toccarli mai,
 * quindi alzando il contrasto al massimo non si perde nessun dettaglio.
 *
 * ⚠️⚠️ **OMBRE E LUCI PESANO SU UNA MASCHERA, ed è quello che le distingue dai punti**: la
 * maschera vale uno dove il pixel è scuro (per le ombre) o chiaro (per le luci) e si spegne
 * dall'altra parte. Elevata al quadrato, la transizione è morbida: con una maschera lineare il
 * confine fra la zona toccata e quella no si vede come un alone.
 *
 * ⚠️⚠️ **I PUNTI DI BIANCO E DI NERO SONO UNA RIMAPPATURA LINEARE, cioè i livelli in ingresso, e
 * per questo non possono appiattire l'immagine**: si spostano i due estremi dell'intervallo e si
 * ridistribuisce quello che c'è in mezzo. Al fondo della corsa si perde **un quarto** della scala
 * da una parte, e il resto dei toni resta distribuito: è la differenza col cursore che questo
 * conto aveva fino alla `2.15`, dove l'estremo era il bianco pieno o il nero pieno.
 */
internal const val LOOK_AGSL = """
uniform shader image;
// ⚠️⚠️ **LA CURVA TONALE ARRIVA COME IMMAGINE E NON COME NUMERI, ED È IL SOLO UNIFORM DI QUESTO
// PROGRAMMA CHE NON SIA UN VALORE**: una spline vuole un ciclo sui suoi punti, e quei punti sono in
// numero variabile, quindi il conto vive in Kotlin (`Curve` in `Adjust.kt`) e qui arriva la sua
// **tabella**, una riga di 256 pixel in cui ogni canale porta la propria curva già composta con
// quella di tutti i toni. Il perché, e perché non è la seconda copia che questo file esiste per non
// avere, vivono sul KDoc di quel tipo.
uniform shader tone;
uniform half gain;
uniform half contrast;
uniform half highlights;
uniform half shadows;
uniform half whites;
uniform half blacks;
uniform half warmth;
uniform half green;
uniform half saturation;
uniform half vibrance;
uniform half mono;
uniform half mixOn;
uniform half centre[8];
uniform half spanLo[8];
uniform half spanHi[8];
uniform half bandHue[8];
uniform half bandSat[8];
uniform half bandLum[8];
uniform half toneOn;
uniform half detailOn;
uniform half sharpen;
uniform half masking;
uniform half noise;
uniform half noiseColor;
// ⚠️ **I due passi arrivano in `float` e non in `half`**, e non è pignoleria: si sommano a `p`,
// che nel salvataggio arriva a duemila, e in `half` un numero così grande non ha più i decimali.
// Un passo arrotondato darebbe un vicinato storto proprio sulle immagini grandi.
uniform float2 reach;
uniform float2 grain;

// Quanto spostano i due cursori del bilanciamento del bianco, al fondo della corsa. Il numero
// dice quanto è forte il cursore, e a 0,3 il massimo copre lo scarto fra una luce di casa e la
// luce del giorno, che è il tratto in cui si lavora davvero.
const half WB_REACH = 0.3;

// Quanto è morbida la piega delle alte luci: vedi `shoulder`, dove il numero è misurato.
const half SHOULDER_SOFT = 1.5;

// Di quanto si sposta al massimo un punto, cioè un quarto della scala per parte. Il numero
// decide quanto è forte il cursore, e a un quarto l'intervallo più stretto che si può chiedere
// vale comunque metà scala: non esiste un valore dei due cursori che dia un'immagine piatta.
const half POINT_SHIFT = 0.25;

// Di quanto il cursore della tonalità sposta una fascia, al fondo della corsa: trenta gradi,
// cioè un dodicesimo di giro. È la distanza fra due fasce vicine nella metà fitta della ruota,
// quindi al massimo un colore arriva **accanto** al suo vicino senza scavalcarlo: oltre, un
// rosso spinto diventerebbe giallo e il cursore si leggerebbe come rotto.
const half HUE_REACH = 0.0833;

// Di quanto il cursore della luminanza schiarisce o scurisce una fascia, al fondo della corsa.
// A metà, un colore pieno diventa la metà più chiaro o più scuro: è il tratto in cui un cielo
// si stacca dalle nuvole senza che il resto dell'immagine se ne accorga.
const half LUM_REACH = 0.5;

// Quanto vale la maschera di contrasto al fondo della corsa: una volta e mezzo il dettaglio
// estratto. Oltre, gli aloni intorno ai bordi smettono di essere nitidezza e diventano un segno
// che si vede da solo.
const half SHARP_REACH = 1.5;

// Quanto bordo serve, al massimo della mascheratura, perché la nitidezza passi. Il numero si
// legge sulla scala di `edge`, che somma le tre differenze di canale: 0,6 vuol dire che al fondo
// corsa passano soltanto i contorni netti, e il cielo resta com'è.
const half MASK_REACH = 0.6;

// Quanto in fretta un vicino DIVERSO smette di contare, nella riduzione del rumore. La grana di
// un sensore muove pochi livelli su 255 (`edge` intorno a 0,05), un contorno vero ne muove
// decine (0,3 e oltre): con questo fattore il primo pesa quasi come il centro e il secondo non
// pesa affatto, quindi si media il rumore senza spianare i bordi.
const half NOISE_EDGE = 120.0;

// ⚠️⚠️ **LA PIEGA DELLE ALTE LUCI, DALLA `2.18`, ED È IL SUO RISCONTRO** (campo libero del giro
// della `2.17`: *l'esposizione è troppo brusca sulle tonalità chiare: aumentandola le parti
// chiare diventano bianche troppo velocemente*). La causa era un taglio: la luce si moltiplica e
// quello che usciva dall'intervallo veniva schiacciato sul bianco, quindi sopra una certa
// esposizione tutti i toni chiari diventavano **lo stesso** bianco. Misurato a +1,5 stop: dei 77
// livelli sopra il 70% di scala ne restava **uno**, e con la piega ne restano 17.
//
// ⚠️⚠️ **LA SOGLIA SI RICAVA DAL GUADAGNO E NON È UN NUMERO, e questo è quello che rende la
// funzione neutra a riposo**: la piega comincia al tono che moltiplicato per il guadagno arriva
// esattamente al bianco, cioè `1/g`. A guadagno 1 quella soglia vale 1, quindi la funzione è
// l'**identità** su tutto l'intervallo e un'immagine non toccata esce identica (misurato: scarto
// nullo su tutti e 256 i livelli). Con una soglia scritta a mano, invece, un'immagine a riposo
// perderebbe i suoi chiari senza che nessuno abbia mosso niente.
//
// ⚠️ **I mezzi toni tengono il guadagno pieno**: sotto la soglia non si tocca niente, quindi a
// +1 stop un grigio medio raddoppia come prima. La piega lavora solo dove il taglio bruciava.
//
// ⚠️ **Il costo è dichiarato e misurato**: un bianco pieno non resta esattamente pieno (a +1 stop
// arriva a 252 su 255), perché la curva tende al bianco senza raggiungerlo mai. È uniforme su
// tutta l'area, quindi non ha un bordo da cui si veda. ⚠️ **La variante che lo teneva a 255 è
// stata provata e scartata**: normalizzare la coda rende la pendenza alla piega **maggiore** di
// uno (1,12 a un quarto di stop), cioè apre un tratto in cui il contrasto cresce invece di
// comprimersi, e un'inversione di pendenza si vede come un gradino. Così la pendenza vale uno
// alla piega e cala da lì in poi.
//
// ⚠️ **Si applica per CANALE e non sulla luminanza**: un colore acceso che satura un canale solo
// virava, perché quel canale si fermava mentre gli altri salivano; piegandoli tutti e tre con la
// stessa curva, il colore si desatura dolcemente verso i chiari, che è quello che fa una
// pellicola.
//
// ⚠️⚠️ **E DALLA `2.19` LA PIEGA COMINCIA PRIMA, PERCHÉ LUI L'HA GUARDATA** (nota sulla voce
// `luce-piega` del giro della `2.18`, approvata: *ancora un pelo più morbida*). La soglia non è
// più `1/g` ma `1/g` elevato a `SHOULDER_SOFT`: un esponente sopra uno la abbassa, cioè fa
// cominciare la compressione più giù e la distribuisce su un tratto più lungo.
// - ⚠️⚠️ **L'ESPONENTE NON TOCCA LA NEUTRALITÀ A RIPOSO, ed è la ragione per cui si agisce lì**:
//   a guadagno 1 la soglia vale `1` elevato a qualunque cosa, cioè sempre 1, quindi la funzione
//   resta l'identità e un'immagine non toccata esce identica (rimisurato: scarto nullo su tutti e
//   256 i livelli). Una soglia abbassata con una sottrazione avrebbe perso quella proprietà.
// - **Il numero viene da una misura e non da un tentativo**: a +1,5 stop i livelli distinti che
//   restano sopra il 70% di scala passano da 17 (esponente 1) a **23** (esponente 1,5), mentre il
//   grigio medio a +1 stop non si muove di un livello. Oltre 1,5 il guadagno si ferma (24 a
//   esponente 2) e i mezzi toni alti cominciano a cedere, quindi quello è il punto in cui
//   l'immagine guadagna senza che l'esposizione smetta di lavorare sui mezzi toni.
half shoulder(half v, half g) {
    half k = min(half(1.0), pow(half(1.0) / g, SHOULDER_SOFT));
    half room = max(half(1.0) - k, half(0.0001));
    if (v <= k) {
        return v;
    }
    return k + room * (half(1.0) - exp(-(v - k) / room));
}

// Il bilanciamento del bianco: i tre moltiplicatori di canale, con la luminanza tenuta ferma.
//
// ⚠️⚠️ **LA NORMALIZZAZIONE NON È UNA RIFINITURA: SENZA, QUESTI DUE CURSORI DIVENTANO UN TERZO
// CURSORE DI ESPOSIZIONE.** Scaldare vuol dire alzare il rosso e abbassare il blu, e siccome il
// verde pesa il 71% della luminanza percepita, il solo cursore della tinta cambierebbe di brutto
// quanto l'immagine sembra luminosa. Dividendo per la luminanza dei moltiplicatori, un grigio
// resta esattamente della stessa chiarezza e a cambiare è solo il suo colore.
half3 balance(half3 c, half w, half g) {
    half3 mul = half3(
        half(1.0) + w * WB_REACH,
        half(1.0) + g * WB_REACH,
        half(1.0) - w * WB_REACH
    );
    half keep = dot(mul, half3(0.2126, 0.7152, 0.0722));
    return c * mul / max(keep, half(0.0001));
}

// Da sRGB a luce lineare, con la curva vera e non con un'elevazione a 2.2: la parte bassa
// della curva sRGB è un segmento di retta, e approssimarla con una potenza sbaglia proprio sui
// neri, cioè dove l'occhio guarda.
half3 toLinear(half3 c) {
    half3 low = c / half(12.92);
    half3 high = pow((c + half(0.055)) / half(1.055), half3(2.4));
    return mix(low, high, step(half3(0.04045), c));
}

half3 toSrgb(half3 c) {
    half3 low = c * half(12.92);
    half3 high = half(1.055) * pow(c, half3(1.0 / 2.4)) - half(0.055);
    return mix(low, high, step(half3(0.0031308), c));
}

// La luminanza percettiva, coi pesi di Rec. 709: serve alle maschere di ombre e luci, che
// devono seguire quanto un pixel SEMBRA chiaro e non quanto lo è il suo canale più forte.
half luma(half3 c) {
    return dot(c, half3(0.2126, 0.7152, 0.0722));
}

// Un pixel dell'immagine, già diviso per la propria opacità: vedi la nota sul premoltiplicato in
// testa a questo file. È l'unico posto da cui il Dettaglio guarda i vicini.
half3 tap(float2 at) {
    half4 s = image.eval(at);
    return s.a > half(0.0) ? s.rgb / s.a : s.rgb;
}

// Il modulo Dettaglio: la riduzione del rumore e la maschera di contrasto, sui valori del file.
//
// ⚠️⚠️ **È L'UNICO BLOCCO CHE GUARDA I PIXEL VICINI, e da qui viene tutto quello che costa**: gli
// altri tre moduli leggono un pixel e rispondono, questo ne legge nove per ogni mestiere. Le due
// guardie interne servono a questo: chi non chiede la nitidezza non paga i suoi nove campioni, e
// chi non chiede la riduzione non paga gli altri.
//
// ⚠️⚠️ **SI LAVORA SUI VALORI DEL FILE E NON IN LUCE LINEARE**, al contrario della Luce: il rumore
// e il disegno fine sono quello che l'occhio vede nei numeri del file, e una conversione per ognuno
// dei diciotto campioni costerebbe più di tutto il resto del programma messo insieme.
//
// ⚠️ **La riduzione viene PRIMA della nitidezza**: accentuare e poi spianare vuol dire lavorare due
// volte contro se stessi, e quello che resterebbe accentuato è proprio il rumore.
half3 detailed(float2 p, half3 c) {
    half3 done = c;

    if (noise > half(0.0) || noiseColor > half(0.0)) {
        // La media BILATERALE: ogni vicino pesa per quanto somiglia al centro, quindi la grana si
        // media e un contorno no. Il centro entra nel ciclo da sé, con distanza zero e peso uno.
        half3 sum = half3(0.0);
        half weight = half(0.0);
        for (int j = -1; j <= 1; j++) {
            for (int i = -1; i <= 1; i++) {
                half3 s = tap(p + float2(float(i) * grain.x, float(j) * grain.y));
                half far = dot(abs(s - c), half3(1.0));
                half w = exp(-far * far * NOISE_EDGE);
                sum += s * w;
                weight += w;
            }
        }
        half3 avg = sum / max(weight, half(0.0001));
        half lum = luma(c);
        half soft = luma(avg);
        // ⚠️ **Il rumore di COLORE prende la crominanza della media e le rimette la luminanza del
        // centro**: così le macchie colorate spariscono e il disegno resta, perché il disegno vive
        // nella luminanza. Senza quella correzione questo cursore sarebbe una seconda sfocatura.
        done = mix(done, avg + (lum - soft), noiseColor);
        // ⚠️ **Il rumore di LUMINANZA si somma invece di mescolare**: quello che si porta verso la
        // media è il solo valore chiaro/scuro, e il colore appena deciso qui sopra non si tocca.
        done += mix(lum, soft, noise) - lum;
    }

    if (sharpen > half(0.0)) {
        // La media BINOMIALE (pesi 1-2-1 per riga e per colonna): è la sfocatura da cui si ricava
        // il dettaglio, che è la differenza fra un pixel e il suo intorno.
        half3 sum = half3(0.0);
        half weight = half(0.0);
        half edge = half(0.0);
        for (int j = -1; j <= 1; j++) {
            for (int i = -1; i <= 1; i++) {
                half3 s = tap(p + float2(float(i) * reach.x, float(j) * reach.y));
                half w = half((2.0 - abs(float(i))) * (2.0 - abs(float(j))));
                sum += s * w;
                weight += w;
                // Quanto questo intorno ha un contorno dentro: serve alla mascheratura, e si
                // ricava dagli stessi campioni invece di costarne altri.
                edge = max(edge, dot(abs(s - c), half3(1.0)));
            }
        }
        half3 soft = sum / weight;
        // ⚠️ **La mascheratura protegge il PIATTO**: a zero passa tutto, e salendo la nitidezza
        // arriva solo dove c'è un contorno vero. Senza di lei, alzare la nitidezza su un cielo
        // vuol dire alzare il suo rumore, che è il difetto classico di questo cursore.
        half gate = half(1.0);
        if (masking > half(0.0)) {
            gate = smoothstep(half(0.0), masking * MASK_REACH, edge);
        }
        // ⚠️ **Il dettaglio si misura su `done` e non su `c`**, cioè sull'immagine già ripulita:
        // così il rumore appena tolto non torna dentro moltiplicato.
        done += (done - soft) * sharpen * SHARP_REACH * gate;
    }

    return clamp(done, half3(0.0), half3(1.0));
}

// La curva del contrasto, su un valore in [0, 1] e col perno in mezzo. Per k positivo allontana
// dal centro senza mai raggiungere gli estremi, per k negativo avvicina al centro.
half sCurve(half x, half k) {
    half t = clamp(x, half(0.0), half(1.0));
    if (k >= half(0.0)) {
        half s = t * t * (half(3.0) - half(2.0) * t);
        return mix(t, s, k);
    }
    // ⚠️⚠️ **IL RAMO NEGATIVO ANDAVA DALLA PARTE SBAGLIATA FINO ALLA `2.14`**, e non se ne era
    // accorto nessuno perché il programma non compilava affatto: la radice che c'era scritta
    // portava un tono a 0,6 fino a 0,72, cioè **allontanava** dal centro, quindi il cursore del
    // contrasto alzava il contrasto in tutti e due i versi.
    // ⚠️ **Il fattore non arriva a zero**: a -100 resta il 40% della distanza dal perno, o
    // l'immagine diventerebbe un rettangolo grigio, che non è quello che chiede chi abbassa il
    // contrasto.
    return half(0.5) + (t - half(0.5)) * (half(1.0) + k * half(0.6));
}

// Da RGB a tonalità, saturazione e valore, **senza rami**: la forma classica di Sam Hocevar, che
// ottiene con due `mix` l'ordinamento dei tre canali. Scritta con gli `if` costerebbe divergenza
// su ogni pixel di un bordo, cioè proprio dove i colori cambiano.
//
// ⚠️ **La tonalità esce in frazione di giro** (0 = rosso, 1/3 = verde, 2/3 = blu), che è l'unità
// in cui vivono i centri delle fasce: gradi e frazioni mescolati sarebbero due unità nello stesso
// conto.
//
// ⚠️ **L'epsilon è 1e-4 e non 1e-10**: in `half` il più piccolo numero normale vale circa 6e-5,
// quindi la costante che si legge in giro diventerebbe zero e su un pixel nero il conto
// dividerebbe per zero.
half3 toHsv(half3 c) {
    half4 p = mix(
        half4(c.bg, half(-1.0), half(2.0) / half(3.0)),
        half4(c.gb, half(0.0), half(-1.0) / half(3.0)),
        step(c.b, c.g)
    );
    half4 q = mix(half4(p.xyw, c.r), half4(c.r, p.yzx), step(p.x, c.r));
    half chroma = q.x - min(q.w, q.y);
    half e = half(0.0001);
    return half3(
        abs((q.w - q.y) / (half(6.0) * chroma + e) + q.z),
        chroma / (q.x + e),
        q.x
    );
}

half3 fromHsv(half3 c) {
    half3 p = abs(
        fract(half3(c.x) + half3(half(1.0), half(2.0) / half(3.0), half(1.0) / half(3.0))) *
            half(6.0) - half3(3.0)
    );
    return c.z * mix(half3(1.0), clamp(p - half3(1.0), half3(0.0), half3(1.0)), c.y);
}

// Quanto le otto fasce chiedono a un pixel di tonalità `h`: la somma dei tre valori di ognuna,
// pesata da quanto quel pixel le appartiene.
//
// ⚠️⚠️ **I PESI SONO TRIANGOLARI CON UN RAGGIO PER LATO, E COSÌ LA SOMMA VALE UNO SENZA
// NORMALIZZARE**: il raggio di una fascia da un lato è esattamente la distanza dal centro vicino,
// quindi fra due centri adiacenti i due pesi sommano a uno e tutti gli altri sono zero. ⚠️ **Un
// raggio unico non lo permetterebbe**, ed è il conto che ha fatto scartare la prima stesura: coi
// centri di Lightroom, che non sono equispaziati, con un raggio di sessanta gradi un rosso pieno
// riceveva tre fasce e del proprio cursore gli arrivava il 55 per cento.
//
// ⚠️ **I due raggi arrivano da Kotlin insieme ai centri**, e non sono un secondo dato: si ricavano
// dai centri, e chi sposta un centro si ritrova i raggi giusti senza toccare niente.
half3 pick(half h) {
    half3 want = half3(0.0);
    for (int i = 0; i < 8; i++) {
        // La distanza firmata dal centro, riportata in mezzo giro per parte: la ruota si chiude,
        // quindi fra un rosso a 359 gradi e il centro a 0 la distanza è un grado e non 359.
        half s = h - centre[i];
        s = s - floor(s + half(0.5));
        half span = s >= half(0.0) ? spanHi[i] : spanLo[i];
        half w = max(half(0.0), half(1.0) - abs(s) / span);
        want += w * half3(bandHue[i], bandSat[i], bandLum[i]);
    }
    return want;
}

half4 main(float2 p) {
    half4 src = image.eval(p);
    // ⚠️ Il colore arriva premoltiplicato: si divide per l'opacità prima di lavorare, o un
    // pixel semitrasparente verrebbe trattato come un pixel scuro.
    half a = src.a;
    half3 c = clamp(a > half(0.0) ? src.rgb / a : src.rgb, half3(0.0), half3(1.0));

    // 0. Il Dettaglio, che viene PRIMA di tutto perché è l'unico modulo che parla del **file** e
    // non dell'immagine: dice quanto rumore ha il sensore e quanto il disegno fine va accentuato.
    // Messo dopo, il contrasto avrebbe già moltiplicato la grana che questo modulo esiste per
    // togliere.
    // ⚠️⚠️ **LA GUARDIA UNIFORME NON È UN'OTTIMIZZAZIONE: È QUELLO CHE TIENE NEUTRO IL CONTO A
    // RIPOSO.** Dentro `detailed` si legge l'immagine altre otto volte per mestiere, e su un'
    // immagine non toccata quei campioni non devono nemmeno essere chiesti.
    if (detailOn > half(0.5)) {
        c = detailed(p, c);
    }

    half3 lin = toLinear(c);

    // 0. Bilanciamento del bianco, che viene PRIMA di tutto perché non è una correzione: dice di
    // che colore era la luce che ha fatto quello scatto, cioè da quale immagine si parte. Messo
    // dopo l'esposizione, la piega delle alte luci lavorerebbe su un canale che il bilanciamento
    // sta ancora per spingere fuori scala.
    lin = balance(lin, warmth, green);

    // 1. Esposizione: la luce si moltiplica, che è quello che fa un diaframma, e quello che
    // uscirebbe dalla scala si piega invece di essere tagliato: vedi `shoulder`.
    lin = half3(
        shoulder(lin.r * gain, gain),
        shoulder(lin.g * gain, gain),
        shoulder(lin.b * gain, gain)
    );

    // 2. Ombre e luci, ognuna sulla propria maschera quadratica.
    // ⚠️⚠️ **LA MASCHERA GUARDA IL VALORE PERCETTIVO E NON LA QUANTITÀ DI LUCE**: in luce
    // lineare un grigio medio vale 0,22, quindi una maschera costruita là darebbe 0,61 di
    // 'ombra' a un pixel che l'occhio vede esattamente a metà, e il cursore delle ombre
    // solleverebbe i mezzi toni come fa la luminosità. Costa una conversione in più per pixel,
    // e vale quella: è la sola cosa che distingue questi due cursori dal terzo.
    half l = clamp(luma(toSrgb(clamp(lin, half3(0.0), half3(1.0)))), half(0.0), half(1.0));
    half darkMask = (half(1.0) - l) * (half(1.0) - l);
    half lightMask = l * l;
    lin = lin * (half(1.0) + shadows * darkMask * half(0.8));
    lin = lin * (half(1.0) + highlights * lightMask * half(0.8));

    lin = max(lin, half3(0.0));
    // ⚠️⚠️ **QUESTA VARIABILE NON SI CHIAMA `out`, E IL NOME È LA CORREZIONE DELLA `2.15`**:
    // `out` è un qualificatore di parametro del linguaggio, quindi `half3 out` è un errore di
    // sintassi e il programma intero non compila. Il difetto è uscito nella `2.14` ed è arrivato
    // a lui: nessun cursore muoveva l'immagine, perché il programma non esisteva.
    half3 rgb = toSrgb(clamp(lin, half3(0.0), half3(1.0)));

    // 3. Punti di bianco e di nero: l'intervallo tonale si ridefinisce spostando i suoi due
    // estremi, e quello che c'è in mezzo si ridistribuisce fra loro.
    // ⚠️ **I due cursori vanno in versi opposti di proposito**: alzando i neri l'immagine si
    // apre (il punto scende sotto lo zero e nessun tono arriva più al nero), alzando i bianchi
    // si chiude verso l'alto (il punto scende sotto l'uno e i chiari arrivano al bianco). È il
    // verso che hanno in un pannello di livelli, ed è quello che lui conosce.
    half floorAt = -blacks * POINT_SHIFT;
    half ceilAt = half(1.0) - whites * POINT_SHIFT;
    rgb = (rgb - half3(floorAt)) / (ceilAt - floorAt);
    rgb = clamp(rgb, half3(0.0), half3(1.0));

    // 4. Contrasto: sul valore percettivo, che è dove una curva a S si comporta come l'occhio
    // si aspetta. In lineare la stessa curva sposterebbe tutto verso i neri.
    rgb = half3(sCurve(rgb.r, contrast), sCurve(rgb.g, contrast), sCurve(rgb.b, contrast));

    // 5. La curva tonale, cioè i toni rimappati uno per uno.
    // ⚠️⚠️ **VIENE DOPO IL CONTRASTO E PRIMA DELL'HSL, E LE DUE COSE HANNO DUE RAGIONI DIVERSE.**
    // Dopo il contrasto, perché la curva a S è una rimappatura predefinita e questa è quella fatta
    // a mano: al contrario, la S lavorerebbe su una distribuzione che la curva ha appena
    // ridisegnato, e i due comandi si contenderebbero gli stessi toni. Prima dell'HSL, perché le
    // tre curve di canale **cambiano la tonalità** di un pixel (una curva sul blu vira tutta
    // l'immagine), e chi scegle i colori per tonalità deve leggere quella definitiva.
    // ⚠️⚠️ **TRE LETTURE E NON UNA, e non è uno spreco**: i tre canali entrano nella tabella a tre
    // posizioni diverse, quindi una lettura sola darebbe i tre canali dello stesso livello, che è
    // un'altra cosa. Il canale che si legge da ognuna è quello a cui la sua colonna appartiene.
    // ⚠️ **Il mezzo pixel è il centro della voce**: la tabella è larga 256, quindi il livello `v`
    // vive a `v * 255 + 0.5`, e fra due voci il filtro lineare interpola.
    if (toneOn > half(0.5)) {
        rgb = half3(
            tone.eval(float2(float(rgb.r) * 255.0 + 0.5, 0.5)).r,
            tone.eval(float2(float(rgb.g) * 255.0 + 0.5, 0.5)).g,
            tone.eval(float2(float(rgb.b) * 255.0 + 0.5, 0.5)).b
        );
    }

    // 6. L'HSL per fascia: gli stessi tre comandi su otto colori.
    // ⚠️⚠️ **VIENE PRIMA DELLA SATURAZIONE E DOPO IL CONTRASTO, E LE DUE COSE HANNO DUE RAGIONI
    // DIVERSE.** Dopo il contrasto, perché sceglie i colori per **tonalità** e la tonalità è
    // quella che la luce ha finito di definire; prima della saturazione, perché quella è il
    // giudizio finale su tutta l'immagine mentre questo è mirato, e soprattutto perché il grigio
    // del bianco e nero si ricava da quello che esce **di qui**: è così che la luminanza per
    // fascia diventa la miscela del bianco e nero, che è la sua risposta a `d-bn-pesi`.
    // ⚠️⚠️ **LA GUARDIA UNIFORME TIENE NEUTRO IL CONTO A RIPOSO**: l'andata e il ritorno da HSV in
    // `half` non sono esattamente l'identità, quindi senza questa riga un'immagine non toccata
    // perderebbe un livello qua e là. Con nessuna fascia mossa il blocco non gira affatto.
    if (mixOn > half(0.5)) {
        half3 hsv = toHsv(rgb);
        half3 want = pick(hsv.x);
        // ⚠️ **E la seconda guardia vale per PIXEL**: chi appartiene a fasce tutte a zero riceve
        // esattamente zero da `pick`, quindi non ha niente da guadagnare dalla conversione e tutto
        // da perdere. Muovendo una fascia sola, il resto dell'immagine resta identico.
        if (dot(abs(want), half3(1.0)) > half(0.0)) {
            // Quanto il pixel ha colore: un grigio non appartiene a nessuna fascia, e senza questo
            // peso il cursore della luminanza schiarirebbe anche il cielo bianco e il rumore degli
            // scuri, che tonalità non ne hanno.
            half ink = hsv.y;
            hsv.x = fract(hsv.x + want.x * HUE_REACH);
            // ⚠️ **La saturazione si MOLTIPLICA e non si somma**: un grigio ha saturazione zero,
            // quindi resta grigio qualunque cosa chieda la sua fascia, e a -100 il colore arriva
            // esattamente al grigio invece di attraversarlo.
            hsv.y = clamp(hsv.y * (half(1.0) + want.y), half(0.0), half(1.0));
            hsv.z = clamp(
                hsv.z * (half(1.0) + want.z * LUM_REACH * ink), half(0.0), half(1.0)
            );
            rgb = fromHsv(hsv);
        }
    }

    // 7. Quanto sono accesi i colori, e viene per ULTIMO perché è un giudizio sull'immagine
    // finita: messo prima, ogni cursore della Luce lo rimetterebbe in discussione, e alzare il
    // contrasto alzerebbe di suo anche la saturazione.
    // ⚠️⚠️ **SI LAVORA SUL VALORE PERCETTIVO E NON IN LINEARE**, al contrario della Luce: la
    // saturazione è quanto un colore si distingue dal grigio **per l'occhio**, e in luce lineare
    // lo stesso conto spegnerebbe i colori scuri molto più di quelli chiari.
    half grey = luma(rgb);
    // La vividezza pesa il suo effetto sull'inverso di quanto un colore è GIÀ saturo, ed è
    // questo che la distingue dalla saturazione: dove il colore è acceso il peso va a zero,
    // quindi un cielo già pieno non si impasta mentre un incarnato spento si alza.
    half top = max(rgb.r, max(rgb.g, rgb.b));
    half bottom = min(rgb.r, min(rgb.g, rgb.b));
    half already = top > half(0.0) ? (top - bottom) / top : half(0.0);
    half push = half(1.0) + saturation + vibrance * (half(1.0) - already);
    rgb = mix(half3(grey), rgb, max(push, half(0.0)));

    // Il bianco e nero viene dopo, e non è la saturazione a -100: quello lascerebbe il conto
    // esposto a un cursore che qualcuno può aver alzato, mentre qui il grigio è il grigio.
    rgb = mix(rgb, half3(grey), mono);

    rgb = clamp(rgb, half3(0.0), half3(1.0));
    return half4(rgb * a, a);
}
"""

/**
 * La tabella di una curva a riposo, cioè la diagonale, costruita una volta sola per tutto il
 * processo.
 *
 * ⚠️ **Serve perché lo shader vuole quella tabella SEMPRE**: un `uniform shader` non dichiarato
 * fa rifiutare il programma, quindi anche un'immagine che non ha curve deve consegnarne una. Senza
 * questa, ogni fotogramma di ogni immagine pagherebbe 1024 valori di spline e una bitmap, per
 * ottenere sempre lo stesso risultato.
 * - ⚠️ **È immutabile e la leggono in due**, l'anteprima e il salvataggio, che girano su thread
 *   diversi: una bitmap che nessuno scrive si può condividere, e quello che si costruisce a ogni
 *   chiamata è il solo `BitmapShader`, che costa niente.
 */
private val FLAT_TONE: Bitmap by lazy { toneBitmap(Tone.NONE) }

/**
 * La tabella di [tone] come riga di 256 pixel, da consegnare allo shader.
 *
 * ⚠️⚠️ **LA BITMAP È `ARGB_8888` OPACA E IN sRGB, E LE DUE COSE SONO DUE TRAPPOLE EVITATE**: Skia
 * consegna a uno shader i colori **premoltiplicati**, quindi un'opacità diversa da uno
 * moltiplicherebbe i valori della tabella; e se lo spazio colore della bitmap non fosse quello
 * della destinazione, Skia convertirebbe i numeri mentre li legge. Qui la tabella non è
 * un'immagine: è un elenco di valori, e deve arrivare **identica** a come è stata scritta.
 */
private fun toneBitmap(tone: Tone): Bitmap =
    Bitmap.createBitmap(Curve.SIZE, 1, Bitmap.Config.ARGB_8888).apply {
        setPixels(tone.lut(), 0, Curve.SIZE, 0, 0, Curve.SIZE, 1)
    }

/**
 * Il programma compilato con [look] dentro, agganciato all'immagine [image], oppure `null` dove
 * questa strada non esiste. [span] è il lato lungo dell'immagine **nello spazio in cui il conto
 * gira**: i pixel della tessera per il salvataggio, il rettangolo disegnato per l'anteprima.
 *
 * ⚠️⚠️ **[span] NON HA UN VALORE DI SERIE, E NON È UNA DIMENTICANZA**: il modulo Dettaglio ragiona
 * in frazioni del lato, quindi chi chiama deve **dichiarare** quanto misura l'immagine da cui
 * legge. Un valore di serie sarebbe giusto per uno dei due chiamanti e sbagliato per l'altro,
 * senza che niente lo dica: è la stessa forma di presidio del parametro di `Modifier.lowered`.
 *
 * ⚠️⚠️ **`null` VUOL DIRE ANDROID 12 O PRIMA**, e chi chiama non ha una seconda strada: l'editor
 * completo non si offre nemmeno, ed è l'istruzione dell'utente. È lo stesso controllo di
 * `ditherShader`, e per la stessa ragione vive in due funzioni (il controllo di versione e l'uso
 * della classe che nasce con la 13 devono stare separati, o l'analizzatore statico non riconosce
 * la guardia).
 *
 * ⚠️ **Il programma si ricompila a ogni chiamata**, e non è uno spreco da correggere: `RuntimeShader`
 * compila una volta e tiene il risultato, e quello che cambia a ogni fotogramma sono i soli
 * `setFloatUniform`, che costano niente. Chi volesse tenerlo in una cache guardi prima se il
 * profilo dice che serve.
 */
internal fun lookShader(image: Shader, look: Look, span: Float): Shader? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    /*
     * ⚠️⚠️ **UN PROGRAMMA CHE NON COMPILA NON PUÒ FAR CADERE L'APP, e la rete vive qui e non nei
     * chiamanti**: `RuntimeShader` lancia se la scheda grafica rifiuta il testo del programma, e
     * chi chiama sta disegnando un fotogramma. Rispondendo `null` si vede l'immagine senza il
     * conto applicato, che è brutto ma è un'app viva; il salvataggio invece se ne accorge e
     * rifiuta, invece di scrivere un file sbagliato.
     * ⚠️ **Copre anche il banco di prova**, dove non c'è nessuna scheda grafica: una prova che
     * monta la schermata misura la pila dei passi e non i pixel, e senza questa riga cadrebbe
     * sul primo cursore mosso.
     */
    return runCatching { lightOver(image, look, span) }.getOrNull()
}

/**
 * Vedi la nota su [lookShader]: esiste perché la guardia di versione sia riconoscibile.
 *
 * ⚠️ **I valori si consegnano tutti, anche quelli a zero**: un uniform non scritto vale quello che
 * c'era prima, e con un programma ricompilato a ogni chiamata varrebbe zero per caso invece che
 * per scelta. Chi aggiunge un campo a [Look] aggiunge una riga qui, e il banco se ne accorge
 * perché il programma non compila senza il suo uniform.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun lightOver(image: Shader, look: Look, span: Float): Shader {
    val light = look.light
    val chroma = look.chroma
    val detail = look.detail
    val sharp = detail.sharpReach(span)
    val grain = Detail.grainReach(span)
    return RuntimeShader(LOOK_AGSL).apply {
        setInputShader("image", image)
        /*
         * ⚠️⚠️ **IL FILTRO LINEARE SULLA TABELLA È LA RIGA CHE LA RENDE UNA CURVA E NON UNA
         * SCALINATA**: senza, il campionamento arrotonda al pixel più vicino, quindi due livelli
         * vicini che cadono nella stessa voce escono identici e la curva si vede a gradini. È la
         * riga gemella di quella del Dettaglio, e per la stessa ragione: `isFilterBitmap` del
         * pennello governa il disegno e non i campioni che uno shader chiede a un altro.
         * ⚠️ **A riposo la tabella è quella tenuta da parte**, quindi un'immagine non toccata non
         * paga nemmeno la sua costruzione: vedi [FLAT_TONE].
         */
        val lut = if (look.tone.idle) FLAT_TONE else toneBitmap(look.tone)
        setInputShader(
            "tone",
            BitmapShader(lut, TileMode.CLAMP, TileMode.CLAMP).apply {
                setFilterMode(BitmapShader.FILTER_MODE_LINEAR)
            }
        )
        setFloatUniform("toneOn", if (look.tone.idle) 0f else 1f)
        setFloatUniform("gain", light.gain)
        setFloatUniform("contrast", light.contrast)
        setFloatUniform("highlights", light.highlights)
        setFloatUniform("shadows", light.shadows)
        setFloatUniform("whites", light.whites)
        setFloatUniform("blacks", light.blacks)
        setFloatUniform("warmth", chroma.temp)
        setFloatUniform("green", chroma.tint)
        setFloatUniform("saturation", chroma.saturation)
        setFloatUniform("vibrance", chroma.vibrance)
        setFloatUniform("mono", if (chroma.mono) 1f else 0f)
        setFloatUniform("mixOn", if (look.mix.idle) 0f else 1f)
        setFloatUniform("centre", Mix.CENTRES)
        setFloatUniform("spanLo", Mix.SPAN_LO)
        setFloatUniform("spanHi", Mix.SPAN_HI)
        setFloatUniform("bandHue", FloatArray(Mix.COUNT) { look.mix.bands[it].hue })
        setFloatUniform("bandSat", FloatArray(Mix.COUNT) { look.mix.bands[it].sat })
        setFloatUniform("bandLum", FloatArray(Mix.COUNT) { look.mix.bands[it].lum })
        setFloatUniform("detailOn", if (detail.idle) 0f else 1f)
        setFloatUniform("sharpen", detail.sharpen)
        setFloatUniform("masking", detail.masking)
        setFloatUniform("noise", detail.noise)
        setFloatUniform("noiseColor", detail.noiseColor)
        setFloatUniform("reach", sharp, sharp)
        setFloatUniform("grain", grain, grain)
    }
}

/**
 * Se questo telefono sa far girare l'editor completo.
 *
 * ⚠️⚠️ **SI CHIEDE QUI E NON IN TRE POSTI**: lo chiedono il selettore degli editor (per offrire o
 * no la voce), il modello (per sapere dove mandare chi tocca 'Modifica') e le impostazioni. Scritto
 * tre volte, il giorno che il requisito cambia due dei tre mentirebbero.
 */
internal fun advancedEditorAvailable(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

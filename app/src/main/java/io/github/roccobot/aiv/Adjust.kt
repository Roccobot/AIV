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
    val mono: Boolean = false,
    /**
     * Il **filtro** davanti all'obiettivo, dalla `2.35`: come i colori diventano grigi.
     *
     * ⚠️⚠️ **È SUA RICHIESTA** (2026-09-13: *in 'Colore', se attivo 'bianco e nero', voglio che
     * appaia uno slider 'Filtro' che definisca la resa del bianco e nero in base a come sono
     * mappati i colori nell'output*). In fotografia un filtro colorato davanti all'obiettivo
     * schiarisce i soggetti del proprio colore e scurisce i complementari: il giallo, l'arancione
     * e il rosso scuriscono il cielo, il blu lo schiarisce.
     * ⚠️⚠️ **ZERO È IL BIANCO E NERO DI SEMPRE**: a riposo i pesi sono quelli di Rec. 709, quindi
     * chi aggiorna non si ritrova le sue immagini diverse. Il conto vive in [greyMix].
     * ⚠️ **Conta solo col [mono] acceso**, e per questo non entra in [idle]: senza bianco e nero
     * non c'è nessun grigio da comporre, e l'interfaccia lo spegne.
     * ⚠️⚠️ **E DALLA `2.37` LA SUA RIGA VIVE SOTTO L'INTERRUTTORE E NON SI NASCONDE PIÙ**, che è il
     * punto A del suo campo libero del giro della `2.36`: la `2.36` la toglieva dalla scena a
     * colori, e adesso c'è sempre, spenta, subito dopo il comando da cui dipende. Il perché vive su
     * `FILTER_ROW`, in `AdvancedEditorScreen.kt`.
     */
    val filter: Float = 0f
) {

    /**
     * Se questo modulo non cambia un pixel: vedi la nota sulla tolleranza in [Light.idle].
     *
     * ⚠️ **Il filtro non ci entra**, ed è la conseguenza di che cosa fa: senza [mono] non tocca
     * niente, e con [mono] questa risposta è già `false`. Contarlo vorrebbe dire dichiarare da
     * riscrivere un'immagine a colori su cui è stato mosso un cursore che là non governa niente.
     */
    val idle: Boolean
        get() = !mono && abs(temp) < DEAD && abs(tint) < DEAD &&
            abs(saturation) < DEAD && abs(vibrance) < DEAD

    /**
     * I tre pesi con cui questo filtro fa il grigio, nell'ordine rosso, verde, blu.
     *
     * ⚠️⚠️ **SOMMANO SEMPRE UNO, E QUELLA È LA PROPRIETÀ CHE TIENE FERMA L'ESPOSIZIONE**: le tre
     * terne fra cui si interpola sommano a uno ciascuna, quindi ci somma anche qualunque loro
     * miscela, e un grigio esce **con lo stesso valore** qualunque filtro si scelga. Senza, il
     * cursore sarebbe anche un'esposizione, e a fondo corsa l'immagine si scurirebbe.
     * ⚠️⚠️ **I DUE ESTREMI SONO I DUE FILTRI CLASSICI, E LA CORSA CI PASSA IN MEZZO**: verso il
     * caldo si attraversano il giallo e l'arancione prima di arrivare al rosso, verso il freddo il
     * ciano prima del blu, perché un'interpolazione fra Rec. 709 e una terna sbilanciata su un
     * capo produce esattamente quella famiglia. Sono i quattro filtri che si mettevano davanti a
     * un obiettivo, e stanno tutti su un asse solo.
     * ⚠️ **È un asse e non una ruota**, ed è una scelta dichiarata: su una ruota lo zero (nessun
     * filtro) non avrebbe un posto, perché ogni angolo è un colore; su un asse lo zero è il centro
     * e i due versi sono le due cose che si vogliono davvero fare, cioè scurire o schiarire il
     * cielo.
     * ⚠️ **Il verde resta dov'è ai due estremi**: un filtro verde serve al fogliame ed è il terzo
     * della famiglia, ma su un asse solo non ci sta; chi lo vuole muove la luminanza della fascia
     * verde dell'HSL, che dalla `2.21` è la miscela per fascia del bianco e nero.
     */
    val grey: FloatArray get() = greyMix(filter)

    companion object {
        val NONE = Chroma()

        /**
         * I pesi del grigio a filtro fermo: quelli con cui l'occhio vede la luminanza.
         *
         * ⚠️ **Sono gli stessi che lo shader usa in `luma`**, e là restano scritti: quella
         * funzione serve alle maschere della Luce e alla saturazione, che col filtro non
         * c'entrano. Qui l'elenco esiste perché il conto del filtro parte da lui.
         */
        val REC709 = floatArrayOf(0.2126f, 0.7152f, 0.0722f)

        /**
         * Il filtro rosso: il cielo viene cupo e l'incarnato chiaro.
         *
         * ⚠️⚠️ **PIÙ FORTE DALLA `2.37`, ED È LA SUA NOTA SULLA VOCE `bn-filtro`** (giro della
         * `2.36`, esito accettabile: *me l'aspettavo più ampio, ma può anche andare*). Il conto
         * misura quanto: con i pesi della `2.35` un cielo azzurro scendeva di **11 punti su 100** e
         * l'incarnato saliva di 8, quindi lo stacco fra i due passava da 10 a 29; adesso il cielo
         * scende di 19 e l'incarnato sale di 13, e quello stacco vale **42**. Cioè il fondo corsa
         * fa poco più del doppio di prima.
         * ⚠️⚠️ **IL VERDE NON ARRIVA A ZERO, E QUELLO CHE LO TIENE SU È UN CASO MISURATO**: coi
         * pesi di un canale solo (1, 0, 0) un cielo blu **puro** diventerebbe nero, cioè le nuvole
         * scure perderebbero ogni disegno; un filtro vero davanti a un obiettivo lascia passare un
         * po' di tutto, e questi due numeri sono quel poco.
         */
        private val WARM = floatArrayOf(0.85f, 0.15f, 0.00f)

        /** Il filtro blu: il cielo viene lattiginoso e le labbra scure. Vedi la misura su [WARM]. */
        private val COOL = floatArrayOf(0.00f, 0.15f, 0.85f)

        /** I pesi del grigio per il filtro [f], da -1 (blu) a +1 (rosso): vedi [Chroma.grey]. */
        fun greyMix(f: Float): FloatArray {
            val k = f.coerceIn(-1f, 1f)
            if (abs(k) < DEAD) return REC709
            val verso = if (k > 0f) WARM else COOL
            val quanto = abs(k)
            return FloatArray(3) { REC709[it] + (verso[it] - REC709[it]) * quanto }
        }

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
     * che separa la nitidezza di cattura dal disegno a media scala.
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
        var reach = 0f
        if (abs(sharpen) >= DEAD) reach = max(reach, sharpReach(long))
        if (abs(noise) >= DEAD || abs(noiseColor) >= DEAD) reach = max(reach, grainReach(long))
        if (reach <= 0f) return 0
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
         * dire che chi allarga la nitidezza si ritrova un'immagine spianata.
         */
        const val GRAIN_SPAN = 1f / 1200f

        /** Il vicinato della riduzione del rumore: vedi [GRAIN_SPAN]. */
        fun grainReach(long: Float): Float = GRAIN_SPAN * long
    }
}

/**
 * Il modulo **Effetti**: il velo atmosferico, e i due segni che lascia l'apparecchio.
 *
 * ⚠️⚠️ **È IL NONO MODULO, DALLA `2.53`, ED È LA SUA RISPOSTA `effetti` A `d-dopo-editor`** (giro
 * della `2.50`, con la sua nota: *'Effetti', con 'Chiarezza', 'Texture', 'Foschia', `Grana` e
 * `Vignettatura`*). I cursori erano cinque, nell'ordine in cui li ha scritti, e dalla `2.64` sono
 * **tre**: [haze], [vignette] e [grain].
 *
 * ⚠️⚠️ **CHIAREZZA E TEXTURE SONO USCITE CON LA `2.64`, ED È LA SUA RISPOSTA `via` A
 * `d-eff-restano`** (giro della `2.63`: *Toglili tutti e due*). Le due che restano fanno un altro
 * mestiere, e la distinzione dice perché quella scelta non lascia un buco: il contrasto locale
 * accentua **il disegno che c'è**, e a quel mestiere risponde già la nitidezza del Dettaglio; la
 * foschia toglie una cosa che il disegno la **copre**, e le altre due dicono dove il fotogramma si
 * scurisce e di che grana è fatto.
 * - ⚠️⚠️ **IL DIFETTO CHE LE HA TOLTE È UN RETICOLO, E LA CAUSA È MISURATA**: quel filtro legge
 *   **nove campioni radi** a distanza del proprio raggio, cioè campiona invece di mediare, quindi
 *   quello che è più fine del passo si ripiega e a fondo corsa si vede come una trama. È lo
 *   stesso meccanismo che a raggio stretto non si vede affatto, ed è la ragione per cui la
 *   nitidezza del Dettaglio, che lavora a un millesimo del lato, non ha mai avuto quel problema.
 * - ⚠️ **Il rimedio esisteva e il conto lo dice**: leggere la media da una **riduzione**
 *   dell'immagine invece che da nove punti riporta quella trama a zero. Non è stato scritto
 *   perché la risposta è arrivata prima, ed è scritto qui perché chi rimettesse quei due cursori
 *   riparta di lì invece di rifare la misura.
 *
 * ⚠️ **Sono frazioni da -1 a +1 e l'interfaccia li mostra da -100 a +100**, come i cursori degli
 * altri moduli: è il linguaggio di Lightroom, che è quello che lui conosce.
 *
 * ⚠️⚠️ **I TRE SONO DI DUE SPECIE, E LA DIFFERENZA GOVERNA QUELLO CHE COSTANO**: [haze] **legge i
 * pixel vicini**, quindi paga campioni e un bordo sulle tessere del salvataggio; [vignette] e
 * [grain] leggono **dove si trova** il pixel, quindi non costano nessun campione e in cambio
 * pretendono che ogni tessera sappia dov'è nell'immagine intera. Quel dato è [Framed], e senza di
 * lui i due difetti non si vedrebbero sull'anteprima, dove la tessera è una sola.
 *
 * ⚠️⚠️ **VENGONO PER ULTIMI NELLA CATENA, E I DUE POSTI HANNO DUE RAGIONI DIVERSE**: la
 * vignettatura sta dopo tutto quello che parla di colore perché è quello che fa un **obiettivo**,
 * cioè meno luce ai bordi del fotogramma, e messa prima ogni cursore della Luce la rimetterebbe in
 * discussione (un 'Auto' calcolato su un'immagine già vignettata leggerebbe un istogramma che non è
 * il suo); la grana sta dopo ancora, perché è la **pellicola**, cioè il supporto su cui l'immagine
 * è stampata, e messa prima il contrasto e la saturazione la tratterebbero come disegno.
 */
data class Effects(
    /**
     * Quanto **velo atmosferico** si toglie, o si aggiunge verso il basso.
     *
     * ⚠️⚠️ **LA STIMA DEL VELO È LOCALE E NON GLOBALE, ED È IL CANALE SCURO**: dove c'è foschia
     * l'aria aggiunge luce bianca a tutti e tre i canali insieme, quindi il **minimo** dei tre non
     * scende più; dove non ce n'è, quasi ogni pixel ha almeno un canale quasi spento. Mediato
     * sull'intorno, quel minimo dice quanto velo c'è **in quel punto**, e il cursore dice quanto
     * toglierne.
     * - ⚠️⚠️ **LA LUCE ATMOSFERICA SI PRENDE BIANCA, E LA SCELTA È DICHIARATA**: il modello
     *   completo la stima sull'immagine intera, cioè con un dato che l'anteprima e il file pieno
     *   dovrebbero condividere; quel dato non può vivere in [Look], perché uno stile se lo
     *   porterebbe da un'immagine all'altra, e ricalcolato sulla tessera del salvataggio darebbe
     *   un numero diverso per ogni tessera. Con la luce bianca il conto sta tutto nello shader e
     *   non c'è niente da tenere allineato. **Quello che si perde** è la foschia molto colorata,
     *   dove resta una dominante: là c'è il bilanciamento del bianco, che è il cursore per quello.
     * - ⚠️ **I due versi sono l'uno l'inverso dell'altro**, quindi il cursore portato a +50 e poi
     *   a -50 riporta dov'era: toglierlo è `(c - k) / (1 - k)`, aggiungerlo è `c + k (1 - c)`, con
     *   lo stesso `k`.
     */
    val haze: Float = 0f,
    /**
     * Quanto si scuriscono gli angoli, o si schiariscono verso il basso.
     *
     * ⚠️⚠️ **NON GUARDA I PIXEL VICINI MA GUARDA DOVE SI TROVA, E QUESTO È IL SUO PREZZO**: i tre
     * cursori qui sopra costano campioni e un bordo sulle tessere del salvataggio; questo costa
     * zero campioni e in cambio pretende che ogni tessera sappia **dov'è nell'immagine
     * intera**. Senza quel dato ogni tessera si vignetterebbe per conto suo, cioè il file salvato
     * porterebbe un angolo scuro per ogni giunzione.
     * - ⚠️ **La distanza si misura sulla mezza diagonale**, quindi vale zero al centro e uno agli
     *   angoli qualunque sia il formato dell'immagine: su un panorama e su un quadrato lo stesso
     *   valore del cursore scurisce lo stesso angolo.
     * - ⚠️⚠️ **LA RAMPA PARTE DAL CENTRO E CRESCE FINO AL PUNTO IN CUI È PIENA, DALLA `2.67`**: è
     *   il profilo di un obiettivo vero, che cala da subito, e `smoothstep` gli toglie il gradino
     *   al centro. ⚠️ **Dove cade il pieno lo dice [vignetteFeather]**: a riposo è l'angolo. Fino
     *   alla `2.66` la rampa cominciava invece a metà raggio e finiva sempre all'angolo.
     */
    val vignette: Float = 0f,
    /**
     * Dove la vignettatura arriva al **pieno**: fuori dal fotogramma, o appena dentro il bordo.
     *
     * ⚠️⚠️ **È UN CURSORE SECONDARIO, DALLA `2.66`, ED È SUA RICHIESTA** (campo libero del giro
     * chiuso il 2026-09-19: *per `Vignettatura`: 'Sfumatura'. Indica quanto l'alone scuro intorno
     * si avvicina al centro, e/o l'opacità iniziale ai bordi esterni (credo)*).
     * - ⚠️⚠️ **MA DALLA `2.67` GOVERNA UN'ALTRA COSA, ED È IL SUO RISCONTRO** (voce
     *   `eff-vign-sfuma` accettabile: *voglio che la sfumatura sia sempre massima. Deve cambiare
     *   il punto di inizio, che in negativo dev'essere lontano (fuori dal fotogramma), mentre in
     *   positivo arriva poco all'interno del bordo*). Fino alla `2.66` spostava il punto in cui
     *   l'alone **comincia**, tenendo il pieno sull'angolo; adesso la rampa parte sempre dal
     *   centro, che è la sfumatura più lunga possibile, e a muoversi è il punto del **pieno**.
     * - ⚠️⚠️ **QUINDI A RIPOSO L'IMMAGINE CAMBIA, E VA DETTO**: a parità di cursore principale
     *   l'alone è più esteso e arriva più dentro (misurato con la vignettatura a -50: a metà
     *   raggio si perdono 35 livelli su 255 invece di zero, e a metà di un lato 56 invece di 26).
     *   L'angolo resta identico. Chi aveva una vignettatura tarata la rivede più diffusa.
     * - ⚠️ **Che cosa fanno i due estremi**, sui raggi notevoli di [Adjust.LOOK_AGSL]: a `+100` il
     *   pieno cade a `0,70`, cioè appena dentro il punto di mezzo di ogni lato (`0,707`); a riposo
     *   sull'angolo; a `-100` a `1,30`, cioè fuori dal fotogramma, e là l'angolo si ferma all'86%.
     * - ⚠️ **Da solo non cambia un pixel**, quindi non entra in [idle] e l'interfaccia lo spegne
     *   finché [vignette] è a zero: è il criterio della maschera di contrasto senza nitidezza.
     */
    val vignetteFeather: Float = 0f,
    /**
     * Quanta **grana** si aggiunge, come quella di una pellicola.
     *
     * ⚠️⚠️ **È MONOPOLARE, E NON PER SIMMETRIA CON GLI ALTRI**: 'meno grana' non vuol dire niente
     * su un'immagine che la grana non ce l'ha, e toglierla è il mestiere della riduzione del
     * rumore, che vive nel Dettaglio. Qui lo zero è l'immagine come il file la porta.
     * - ⚠️⚠️ **HA UNA CELLA, E SENZA DI LEI SPARIREBBE GUARDANDO L'IMMAGINE INTERA**: un rumore
     *   alto un pixel su un file da quattromila si vede solo ingrandendo, e rimpicciolito si
     *   media via. La cella è una frazione del lato, come ogni altra misura di questo editor,
     *   quindi l'anteprima mostra la stessa grana del file salvato in proporzione.
     * - ⚠️⚠️ **E ANCHE LEI VUOLE SAPERE DOVE SI TROVA**: un rumore generato dalla coordinata
     *   **locale** di una tessera si ripeterebbe identico in ogni tessera, cioè darebbe un
     *   motivo a scacchi grande quanto una tessera. Nasce dalla coordinata assoluta, come la
     *   vignettatura.
     * - ⚠️ **Pesa sui mezzi toni**: una pellicola mostra la grana dove c'è emulsione esposta a
     *   metà, e quasi niente nel nero chiuso e nel bianco bruciato. Senza quel peso il cursore
     *   sporcherebbe prima di tutto le ombre, che è l'effetto del rumore digitale e non della
     *   grana. ⚠️ **Dalla `2.66` quel peso scende anche sulle luci**, e quanto lo dice
     *   [grainLift].
     */
    val grain: Float = 0f,
    /**
     * Quanto è grossa la cella della grana, rispetto a quella di serie.
     *
     * ⚠️⚠️ **È UN CURSORE SECONDARIO, DALLA `2.66`, ED È SUA RICHIESTA** (campo libero del giro
     * chiuso il 2026-09-19: *per 'Grana': a) 'Dimensione'. L'attuale va benissimo ed è il
     * default, ma a volte mi piace generare grana un po' più grossa*).
     * - ⚠️⚠️ **RADDOPPIA E DIMEZZA INVECE DI SOMMARE, COME IL RAGGIO DEL DETTAGLIO**: una misura
     *   di questo genere si percepisce in rapporti, e il precedente in casa è già scritto su
     *   [Detail.sharpReach]. Lo zero è [GRAIN_CELL], cioè quella che lui ha approvato.
     * - ⚠️ **Su un file da quattromila pixel la corsa va da 1,7 a 6,7 pixel**, con 3,3 a riposo:
     *   sotto si arriva al rumore che si vede solo ingrandendo, sopra all'impasto che resta
     *   visibile anche rimpicciolendo.
     * - ⚠️⚠️ **VERSO IL FINE IL PAVIMENTO DI [grainCell] ENTRA ANCHE SULL'ANTEPRIMA, e va detto**:
     *   a 1600 pixel di lato la cella scende sotto il pixel intorno a `-0,4`, quindi là la grana
     *   si vede un po' più grossa di quella che il file salvato porterà. Fino alla `2.65` quel
     *   pavimento non si incontrava mai sopra i 1200 pixel, e questo cursore è la ragione per cui
     *   adesso si incontra.
     * - ⚠️ **Da solo non cambia un pixel**, quindi non entra in [idle] e l'interfaccia lo spegne
     *   finché [grain] è a zero.
     */
    val grainSize: Float = 0f,
    /**
     * Quanta grana arriva alle **luci**: a riposo quasi niente, a fondo corsa tutta.
     *
     * ⚠️⚠️ **IL VALORE DI RIPOSO È QUELLO CHE LUI HA CHIESTO DI FABBRICA, E LA CORSA RIMETTE IL
     * CONTO DI PRIMA** (campo libero del giro chiuso il 2026-09-19: *b) 'Luci', che è
     * l'abbreviazione di 'Applica alle luci'. Voglio che di default la grana sia aggiunta solo
     * alle ombre, e alle luci in misura non proprio zero ma quasi. Questo per evitare che ad aree
     * uniformi come il cielo sia aggiunta grana inutilmente*). Scritto al rovescio, cioè con lo
     * zero sul comportamento della `2.65`, il valore di fabbrica avrebbe dovuto essere un numero
     * diverso da zero, e allora un preset che non nomina questo campo lo rileggerebbe sbagliato.
     * - ⚠️⚠️ **A FONDO CORSA IL CONTO È ESATTAMENTE QUELLO DELLA `2.65`** (misurato: scarto nullo
     *   su 1001 toni), quindi chi vuole la grana di prima ha un posto dove chiederla.
     * - ⚠️⚠️ **E DALLA `2.67` LA RAMPA È PIÙ BASSA, PERCHÉ I TRE NUMERI SONO SUOI** (voce
     *   `eff-grana-luci` accettabile: *il 'quasi zero' passa da 0,8 a 0,1 ... metà scala a 0,35*).
     *   Quindi sul cielo resta un decimo di livello invece di otto, e a metà tono la grana tiene
     *   il 35% invece dell'81%: il conto e la lettura di quel `0,35` vivono sulle tre costanti,
     *   in [Adjust.LOOK_AGSL].
     * - ⚠️⚠️ **MA CHI HA GIÀ UNO STILE CON LA GRANA MOSSA LA RITROVA DIVERSA, E SI DICHIARA**:
     *   il peso di fabbrica è cambiato, e un preset salvato con la `2.65` non porta questo campo,
     *   quindi lo rilegge a riposo. Sulle sue immagini la grana resta dov'era nelle ombre e
     *   quasi sparisce nei chiari.
     * - ⚠️ **Quello che la sua richiesta non copre**: un cielo è chiaro **e** uniforme, e questo
     *   cursore guarda solo quanto è chiaro. Guardare anche l'uniformità vorrebbe dire leggere i
     *   pixel vicini, cioè un raggio e un bordo sulle tessere del salvataggio, che è il prezzo
     *   che la foschia paga da sola; qui costa zero campioni ed è la strada che lui ha indicato.
     * - ⚠️ **Da solo non cambia un pixel**, quindi non entra in [idle] e l'interfaccia lo spegne
     *   finché [grain] è a zero.
     */
    val grainLift: Float = 0f
) {

    /**
     * Se questo modulo non cambia un pixel: vedi la nota sulla tolleranza in [Light.idle].
     *
     * ⚠️⚠️ **I TRE CURSORI SECONDARI NON SI CONTANO, ED È IL CRITERIO DEL DETTAGLIO**: [grainSize],
     * [grainLift] e [vignetteFeather] dicono **come** lavorano i due che li governano, non
     * *quanto*. Contandoli, un'immagine con la sola 'Sfumatura' mossa si dichiarerebbe da
     * riscrivere, cioè verrebbe ricompressa per niente.
     */
    val idle: Boolean
        get() = abs(haze) < DEAD && abs(vignette) < DEAD && abs(grain) < DEAD

    /** Se la grana è spenta, cioè se [grainSize] e [grainLift] non governano niente. */
    val noGrain: Boolean get() = abs(grain) < DEAD

    /** Se la vignettatura è spenta, cioè se [vignetteFeather] non governa niente. */
    val noVignette: Boolean get() = abs(vignette) < DEAD

    /**
     * Quanti pixel di sovrapposizione vuole una tessera del salvataggio, dato il lato lungo
     * dell'immagine **intera**.
     *
     * ⚠️⚠️ **È LO STESSO CONTO DI [Detail.bleed] E PER LA STESSA RAGIONE**: anche qui il filtro
     * legge i vicini, quindi senza un bordo da buttare via l'ultima colonna di una tessera
     * leggerebbe il bordo ripetuto invece del pixel che sta di là. ⚠️ **Ma il numero è molto più
     * grande**, perché il raggio della foschia è decine di volte quello della nitidezza: su un
     * file da quattromila pixel sono decine di pixel per lato invece di cinque.
     * - ⚠️ **A modulo spento vale zero**, quindi chi non usa questi cursori non paga niente.
     * - ⚠️⚠️ **VIGNETTATURA E GRANA NON ENTRANO IN QUESTO CONTO, ED È QUELLO CHE LE DISTINGUE**:
     *   nessuna delle due legge un pixel vicino, quindi non c'è nessun bordo da buttare via. Chi
     *   usa solo quelle paga le tessere di sempre. Quello che pretendono invece è un altro dato,
     *   cioè dove la tessera si trova, e quello vive in `AdjustRender`.
     * - ⚠️ **Il conto resta scritto come un massimo su una guardia sola**, e con la `2.64` i
     *   cursori che leggono i vicini sono uno: chi ne aggiungesse un secondo trova la forma già
     *   pronta invece di una riga da riscrivere.
     */
    fun bleed(long: Float): Int {
        var reach = 0f
        if (abs(haze) >= DEAD) reach = max(reach, hazeReach(long))
        // ⚠️ **Zero vuol dire zero, e la guardia non è su [idle]**: un modulo mosso con la sola
        // vignettatura o la sola grana non è a riposo, e nessuna delle due legge un vicino. Scritta
        // su [idle], quel caso pagherebbe un pixel di bordo per niente.
        if (reach <= 0f) return 0
        return ceil(reach).toInt() + 1
    }

    companion object {
        val NONE = Effects()

        private const val DEAD = 0.0005f

        /**
         * Quanto lontano si guarda per stimare il velo, in frazione del lato lungo.
         *
         * ⚠️⚠️ **È LARGO PERCHÉ IL VELO NON È UN DETTAGLIO**: la foschia è una proprietà di una
         * **regione** dell'immagine, quindi la sua stima deve cambiare piano, o il conto la
         * scambierebbe per il disegno e ne accentuerebbe i bordi. A quaranta pixel su un file da
         * quattromila la mappa del velo è liscia e segue comunque il confine fra un primo piano
         * nitido e uno sfondo velato.
         * ⚠️ **E il tetto è il numero di campioni**: sono nove, quindi oltre una certa distanza
         * non descrivono più il loro intorno.
         */
        const val HAZE_SPAN = 1f / 100f

        /** Il raggio della stima del velo nello spazio del conto: vedi [HAZE_SPAN]. */
        fun hazeReach(long: Float): Float = HAZE_SPAN * long

        /**
         * Il lato della cella della grana, in frazione del lato lungo.
         *
         * ⚠️⚠️ **È UNA FRAZIONE E NON UN NUMERO DI PIXEL, PER LA STESSA RAGIONE DEI RAGGI**: il
         * conto gira sull'anteprima ridotta a 1600 pixel e sul file pieno, e una cella scritta in
         * pixel darebbe due grane diverse. Così l'anteprima mostra la stessa grana in proporzione,
         * che è la sola cosa che si può promettere senza guardare il file.
         * ⚠️ **Il conto che porta al numero**: su un file da quattromila pixel la cella è di poco
         * più di tre pixel, cioè il grano di una pellicola scansionata a quella misura. Sotto,
         * quello che si ottiene è rumore digitale; sopra, un impasto che si vede anche
         * rimpicciolendo, mentre una grana deve sparire guardando l'immagine intera.
         */
        const val GRAIN_CELL = 1f / 1200f

        /**
         * Il lato della cella della grana nello spazio in cui il conto gira, dato il lato lungo
         * [long] e quanto il cursore 'Dimensione' la ingrossa: vedi [GRAIN_CELL].
         *
         * ⚠️ **Il cursore raddoppia e dimezza invece di sommare**, come il raggio del Dettaglio:
         * il perché vive su [Effects.grainSize], insieme alla corsa misurata.
         *
         * ⚠️⚠️ **SOTTO IL PIXEL NON SI SCENDE, E QUEL PAVIMENTO ROMPE LA PROPORZIONE**: una cella
         * più stretta di un pixel non è una grana più fine, è un rumore che cambia più in fretta di
         * quanto lo schermo sappia mostrare, cioè uno sfarfallio. Là dove entra, la grana si vede
         * un po' più grossa di quella del file salvato, e va detto invece di prometterla identica.
         * ⚠️ **Dalla `2.66` si incontra anche sull'anteprima**: a cursore a riposo il pavimento
         * entra sotto i 1200 pixel di lato, cioè mai in questo editor (1600) né su un file da
         * fotocamera, ma verso il fine della corsa quella soglia sale.
         */
        fun grainCell(long: Float, size: Float = 0f): Float =
            max(1f, GRAIN_CELL * long * 2f.pow(size))
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
     * vicini, e col punto di bordo [edge] portato allo stesso livello.
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
     *
     * ⚠️⚠️ **[edge] NASCE CON LA `2.40`, ED È LA SUA RICHIESTA SUGLI ESTREMI** (2026-09-14: *se
     * trascino il punto iniziale a destra o il finale a sinistra, dovrebbero muoversi lasciando la
     * loro vecchia posizione ad un nuovo punto allo stesso livello*): il punto rimasto al bordo
     * segue in **verticale** quello che si sta trascinando, così fra i due il tratto è piatto
     * mentre si tira. Con un bordo fermo al valore di partenza si vedrebbe una rampa, cioè proprio
     * quello che nessuno ha chiesto. ⚠️ **Vale `-1` quando non c'è nessun bordo da tenere**, che è
     * il caso di ogni altro punto.
     */
    fun move(i: Int, at: Float, to: Float, edge: Int = -1): Curve = copy(
        knots = knots.toMutableList().also {
            val x = when (i) {
                0 -> 0f
                it.size - 1 -> 1f
                else -> at.coerceIn(it[i - 1].at + GAP, it[i + 1].at - GAP)
            }
            val y = to.coerceIn(0f, 1f)
            it[i] = Knot(x, y)
            if (edge in it.indices && edge != i) it[edge] = Knot(it[edge].at, y)
        }
    )

    /**
     * Questa curva con l'estremo raddoppiato, per il dito che lo sta portando dentro.
     *
     * ⚠️⚠️ **È IL PEZZO CHE FA NASCERE IL PUNTO CHE LUI HA CHIESTO**: il gemello resta al bordo e
     * quello che il dito muove è la copia, che da quel momento è un punto come gli altri, cioè si
     * sposta anche in orizzontale. Nasce **sovrapposto** e non a metà strada: il gesto è appena
     * cominciato, e un punto che saltasse via al primo pixel sarebbe un movimento che nessuno ha
     * chiesto.
     * ⚠️ **Al tetto dei punti non si fa niente**, e il chiamante lo sa: senza il gemello l'estremo
     * resta un estremo, cioè si muove solo in verticale come prima della `2.40`.
     * ⚠️ **Quello che resta a filo lo toglie [tidy]**, a gesto finito: chi porta l'estremo dentro e
     * poi lo riporta indietro non si ritrova due punti l'uno sull'altro.
     */
    fun pin(start: Boolean): Curve = when {
        knots.size >= MAX_KNOTS -> this
        start -> copy(knots = listOf(knots.first()) + knots)
        else -> copy(knots = knots + knots.last())
    }

    /**
     * Questa curva senza il gemello che [pin] ha lasciato a filo del proprio bordo.
     *
     * ⚠️ **Guarda i soli due bordi e non gli altri punti**: due punti vicini in mezzo alla curva
     * li ha messi l'utente, e toglierne uno sarebbe una decisione che nessuno ha preso. Qui si
     * chiude il solo caso che il gesto degli estremi può lasciare aperto.
     * ⚠️ **Serve che anche il livello coincida**: un punto tirato a filo del bordo ma a un'altra
     * altezza è un tratto ripidissimo, cioè una cosa che si può volere.
     */
    fun tidy(): Curve {
        var out = knots
        if (out.size > 2 && out[1].at - out[0].at < GAP * 2f &&
            abs(out[1].to - out[0].to) < DEAD
        ) {
            out = out.toMutableList().also { it.removeAt(1) }
        }
        val n = out.size
        if (n > 2 && out[n - 1].at - out[n - 2].at < GAP * 2f &&
            abs(out[n - 1].to - out[n - 2].to) < DEAD
        ) {
            out = out.toMutableList().also { it.removeAt(n - 2) }
        }
        return if (out === knots) this else copy(knots = out)
    }

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
    /**
     * Il modulo **Effetti**, dalla `2.53`.
     *
     * ⚠️ **Vive accanto al Dettaglio e non in fondo all'elenco**, ed è il posto giusto per una
     * ragione sola: sono i due che guardano i pixel **vicini**, quindi sono i due che il
     * salvataggio deve interrogare per sapere quanto bordo dare a una tessera.
     */
    val effects: Effects = Effects.NONE,
    val tone: Tone = Tone.NONE,
    val geo: Geometry = Geometry.NONE,
    /**
     * I tagli **applicati**, cioè quello che il palco inquadra al posto dell'immagine intera.
     *
     * ⚠️⚠️ **È L'UNICO CAMPO CHE NON CAMBIA UN PIXEL DEL FILE: DICE CHE COSA SI VEDE MENTRE SI
     * LAVORA**, e il taglio si applica al salvataggio comunque, come è sempre stato. Vive qui e
     * non nello sguardo per una ragione sola, ed è sua (2026-09-13): così 'Applica' è un passo
     * come gli altri, e 'Annulla' lo disfa senza bisogno di una strada tutta sua.
     * ⚠️ **Perciò non entra né in [idle] né in [lossless]**: un'immagine con la sola vista
     * confermata esce identica a com'è entrata, e un 'Salva' acceso per niente sarebbe una
     * promessa falsa.
     * ⚠️⚠️ **ERA UN BOOLEANO FINO ALLA `2.39`, E CON LUI L'IMMAGINE TORNAVA INTERA DENTRO IL
     * RITAGLIO**: adesso vale anche là, che è la sua richiesta, e il perché di ogni pezzo vive
     * su [Framing].
     */
    val framing: Framing = Framing.NONE
) {

    /**
     * Se il conto del **colore** non cambia un pixel, cioè se lo shader non ha niente da fare.
     *
     * ⚠️ **Non è [idle] e la differenza non è una sfumatura**: un'immagine con la sola geometria
     * mossa non è intonsa (i pixel si spostano), ma il programma dello shader là non serve, e
     * farlo girare per niente costerebbe una passata intera su ogni fotogramma dell'anteprima.
     */
    val plain: Boolean
        get() = light.idle && chroma.idle && mix.idle && detail.idle && effects.idle && tone.idle

    /** Se il modulo Ritaglio non tocca niente: nessuna posa e nessun taglio. */
    val square: Boolean
        get() = spin == Spin.STILL && crop.whole

    /**
     * Il solo **dove**: posa, taglio, geometria e vista, senza niente di quello che tocca i colori.
     *
     * ⚠️⚠️ **SERVE AL CONFRONTO COL PRIMA, DALLA `2.58`, ED È SUA RICHIESTA** (campo libero del
     * giro della `2.55`: *pressione lunga sulla foto nell'editor: se mi trovo nei moduli Ritaglio o
     * Geometria il 'Prima' deve mostrare tutto; se è attivo un altro modulo il 'Prima' deve
     * mostrare tutto tranne Geometria e Ritaglio*). Chi sta tarando un colore e preme per vedere
     * com'era, vuole vedere **quel** colore com'era: con un confronto che toglie tutto, l'immagine
     * salta anche di inquadratura, e fra le due c'è un movimento che non riguarda quello che si sta
     * guardando.
     *
     * ⚠️ **La divisione è quella che questo file dichiara in testa a [Look]**: i sei campi che
     * passano dallo shader dicono di che **colore** è un pixel, questi quattro dicono **dove** va.
     * Il confronto ne toglie uno dei due gruppi, e quale dei due lo decide il modulo che si sta
     * guardando.
     *
     * ⚠️⚠️ **[framing] STA DI QUA, E SENZA DI LUI IL CONFRONTO RIAPRIREBBE UN TAGLIO GIÀ
     * APPLICATO**: quel campo non cambia un pixel del file, ma dice che cosa il palco inquadra,
     * quindi azzerandolo l'immagine tornerebbe intera sotto il dito. Chi aggiunge un campo a
     * [Look] guardi questa riga: un campo di colore dimenticato qui resta applicato nel confronto,
     * cioè non si vede più che cosa fa.
     */
    val place: Look
        get() = Look(spin = spin, crop = crop, geo = geo, framing = framing)

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
 * I tagli **applicati** col comando 'Applica' del modulo Ritaglio, e dove si è nella loro storia.
 *
 * ⚠️⚠️ **DALLA `2.40` 'APPLICA' TAGLIA DAVVERO ANCHE DENTRO IL RITAGLIO, ED È SUA RICHIESTA**
 * (2026-09-14: *ho come l'impressione che il tasto sia in realtà inutile, perché il taglio è sempre
 * applicato in tempo reale e non distruttivo. Potrebbe avere senso se fosse applicato effettivamente
 * anche nel modulo Ritaglio (resta solo la parte ritagliata), ma apparissero dei tasti
 * 'Indietro'/'Avanti'/'Azzera' SOLO PER IL RITAGLIO*). Fino alla `2.39` questo era un booleano e
 * nel Ritaglio l'immagine tornava intera, quindi là dentro quel tasto non faceva niente che si
 * vedesse: la sua osservazione è esatta.
 *
 * ⚠️⚠️ **È UNA STORIA E NON UN VALORE, PERCHÉ I SUOI TRE COMANDI LO CHIEDONO**: 'Indietro' disfa
 * l'ultima applicazione e 'Avanti' la rifà, quindi serve sapere anche quelle che si sono disfatte.
 * La forma è quella della storia dei passi, cioè **una lista e un indice** e non due pile: con due
 * pile ogni applicazione nuova deve ricordarsi di svuotare la seconda, e chi se ne dimentica lascia
 * un 'Avanti' che riporta a una strada abbandonata.
 *
 * ⚠️⚠️ **I TAGLI SONO IN FRAZIONI DELL'IMMAGINE INTERA E NON UNO DENTRO L'ALTRO**: così
 * [Look.crop] resta quello che il salvataggio applica, cioè il rettangolo totale, e nessuno deve
 * comporre una catena per sapere che cosa tagliare. Quello che si compone è il gesto, sul palco,
 * dove il dito tira le squadrette dentro la porzione che si vede.
 */
data class Framing(
    /** I tagli applicati, dal primo all'ultimo, ognuno in frazioni dell'immagine **intera**. */
    val steps: List<ImageEdit.Crop> = emptyList(),
    /** Quanti ne valgono adesso: quelli oltre questo numero sono i disfatti che 'Avanti' rifà. */
    val at: Int = 0
) {

    /**
     * Il taglio che il palco inquadra, o `null` se si vede l'immagine intera.
     *
     * ⚠️ **Un taglio intero vale `null`**, così chi lo legge ha un ramo solo: applicare il
     * rettangolo pieno non cambia niente di quello che si vede, e un `Crop` che copre tutto
     * farebbe fare al palco un conto che dà sé stesso.
     */
    val shown: ImageEdit.Crop? get() = steps.getOrNull(at - 1)?.takeIf { !it.whole }

    /** Se c'è un'applicazione da disfare, cioè se 'Indietro' ha qualcosa da fare. */
    val undoable: Boolean get() = at > 0

    /** Se c'è un'applicazione da rifare, cioè se 'Avanti' ha qualcosa da fare. */
    val redoable: Boolean get() = at < steps.size

    /**
     * Questa storia con [cut] applicato.
     *
     * ⚠️ **Le applicazioni disfatte se ne vanno**, come i passi della storia dell'immagine: da qui
     * in avanti la strada è un'altra, e tenerle vorrebbe dire un 'Avanti' che porta dove nessuno
     * è più passato.
     */
    fun applied(cut: ImageEdit.Crop): Framing = Framing(steps.take(at) + cut, at + 1)

    /** Questa storia con l'ultima applicazione disfatta. */
    fun back(): Framing = if (undoable) copy(at = at - 1) else this

    /** Questa storia con l'applicazione disfatta rimessa. */
    fun on(): Framing = if (redoable) copy(at = at + 1) else this

    /**
     * Gli stessi tagli, riscritti dopo la posa [gesto].
     *
     * ⚠️ **Vanno girati come [Look.crop]**, e per la stessa ragione: sono in frazioni
     * dell'immagine **già posata**, quindi un quarto di giro che non li riscrivesse lascerebbe il
     * palco a inquadrare un'altra porzione di fotografia.
     */
    fun spun(gesto: Spin): Framing = copy(steps = steps.map { spunRect(it, gesto) })

    companion object {
        val NONE = Framing()
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
 * brucia i bianchi e chiude i neri. La forma di `sCurve` tiene i due estremi **fermi**, quindi
 * alzando il contrasto al massimo il nero resta nero e il bianco resta bianco.
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
// I tre pesi con cui i colori diventano grigi, cioè il cursore 'Filtro' (dalla `2.35`): il conto
// vive in Kotlin (`Chroma.greyMix`) perché è una miscela fra tre terne dichiarate, e qui arriva
// il suo risultato. Non è la seconda copia che questo file vieta: il conto è scritto una volta
// sola, e questo è il suo unico lettore.
uniform half3 greyMix;
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
// Il modulo Effetti (dalla `2.53`).
uniform half effectsOn;
// La foschia (dalla `2.54`), col raggio a cui si stima il velo.
uniform half haze;
uniform float2 broad;
uniform half vignette;
// Quanto l'alone della vignettatura si avvicina al centro (dalla `2.66`): vedi `VIGNETTE_SOFT`.
uniform half vignetteFeather;
uniform half filmGrain;
// Quanta grana arriva alle luci (dalla `2.66`): a zero quasi niente, a uno tutta.
uniform half grainLift;
// ⚠️⚠️ **DOVE COMINCIA L'IMMAGINE INTERA, NELLO SPAZIO IN CUI QUESTO CONTO GIRA**: `spot` è il suo
// angolo in alto a sinistra e `frame` la sua misura. Sull'anteprima è il rettangolo in cui
// l'immagine è disegnata sullo schermo; nel salvataggio a tessere l'origine è **negativa**, perché
// lì `p` parte da zero sull'angolo della tessera e l'immagine comincia più indietro. Senza questi
// due, ogni tessera si vignetterebbe per conto suo e la grana si ripeterebbe a scacchi, e nessuno
// dei due difetti si vede sull'anteprima, dove la tessera è una sola.
uniform float2 spot;
uniform float2 frame;
uniform float filmCell;

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

// Quanto il contrasto alza la pendenza al perno, al fondo della corsa positiva: a uno la pendenza
// passa da 1 a 2, cioè due toni vicini al grigio medio si allontanano del doppio. Il numero si
// legge come il contrasto vero, e il tetto è la monotonia: oltre 1,53 la curva tornerebbe
// indietro in due punti, cioè un tono più chiaro uscirebbe più scuro del suo vicino.
// ⚠️ **Lo legge anche `Auto`**, che dalla dispersione dei toni ricava il valore del cursore: il
// numero è ricopiato in `AutoLook.CONTRAST_RISE`, e a presidiare la copia c'è il banco.
const half CONTRAST_RISE = 1.0;

// Quanto la abbassa al fondo della corsa negativa. Sotto uno per forza: a uno la pendenza al
// perno varrebbe zero, cioè tutti i mezzi toni diventerebbero lo stesso grigio.
const half CONTRAST_FALL = 0.8;

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

// Quanta parte del velo stimato si toglie al fondo della corsa.
//
// ⚠️⚠️ **IL NUMERO È UN CONTO E NON UNA TARATURA**: con `k = HAZE_REACH * velo`, un grigio medio
// in un'area velata a metà (velo 0,5) passa da 0,50 a 0,36, e un'area di foschia piena (velo 0,7)
// da 0,70 a 0,56. Sopra 0,5 le ombre dentro una zona velata si chiudono sul nero, perché la
// sottrazione arriva più in basso del pixel più scuro che c'è lì: è l'effetto tipico di questo
// comando spinto, e questo numero lo tiene fuori dalla corsa.
//
// ⚠️ **Il velo che si stima non è mai uno intero**, quindi il denominatore `1 - k` non arriva mai
// vicino a zero: il caso peggiore è un'area di bianco pieno, dove vale `1 - HAZE_REACH`.
//
// ⚠️⚠️ **E LA CODA DI QUESTA NOTA ERA FALSA FINO ALLA `2.60`**: diceva che questo numero teneva
// fuori dalla corsa le ombre che si chiudono, e la misura dice il contrario. Dentro una zona
// velata a 0,62, a cursore pieno, i **64** livelli sotto il quarto di scala uscivano tutti allo
// stesso valore, cioè ne restava **uno solo**: il disegno negli scuri spariva del tutto. Adesso
// il tetto qui sotto ne lascia **40** distinti. Chi ritrova quella frase in una nota vecchia
// sappia che è stata smentita da un conto e non da un'impressione.
const half HAZE_REACH = 0.45;

// Quanto in fretta un vicino DIVERSO smette di contare nella mappa del velo.
//
// ⚠️⚠️ **È PIÙ LARGA DI `NOISE_EDGE` PERCHÉ DISTINGUE UN'ALTRA COSA**: là si separa la grana di un
// sensore (pochi livelli) da un contorno vero, qui il velo di una regione da quello della regione
// accanto. A 20 un vicino che dista 47 livelli pesa la metà, e il conto dice che è il punto
// giusto: da lì in su l'alone sul profilo vale zero livelli, e più in giù ne resta ancora uno.
const half HAZE_EDGE = 20.0;

// I pesi dei nove campioni della mappa del velo: il centro, i cinque dell'anello largo e i tre
// di quello stretto. Sommano a 16, come la media binomiale che c'era prima, e i due anelli
// pesano uguale (6,5 ciascuno), quindi il centro conta meno di una regione intera: la stima del
// velo deve parlare dell'intorno, non del pixel.
//
// ⚠️⚠️ **PERCHÉ NON SONO PIÙ UNA GRIGLIA 3x3, E IL CONTO CHE LO REGGE**: vedi la nota in testa a
// [veiled]. In breve, nove delta su una griglia a passo `s` hanno una risposta in frequenza
// **periodica**, quindi a ogni multiplo di `1/s` passa tutto invece di essere mediato.
const half HAZE_HUB = 3.0;
const half HAZE_WIDE = 1.3;
const half HAZE_TIGHT = 2.1667;

// Quanto la vignettatura scurisce l'angolo al fondo della corsa.
//
// ⚠️ **Il conto che lo regge**: a -100 l'angolo tiene il 45% della sua luce, cioè poco più di uno
// stop, che è quanto perde un obiettivo aperto tutto. Più giù si arriva al tondo scuro che si
// vede dove comincia, e quello si ottiene comunque spingendo il cursore su un'immagine già scura.
const half VIGNETTE_REACH = 0.55;

// Di quanto il cursore 'Sfumatura' sposta il punto in cui la vignettatura arriva al PIENO (dalla
// `2.67`; fino alla `2.66` spostava il punto in cui cominciava).
//
// ⚠️⚠️ **I DUE ESTREMI SONO LA SUA FRASE LETTA SULLA GEOMETRIA DI [fromCentre]** (riscontro del
// giro della `2.66`, voce `eff-vign-sfuma` accettabile: *voglio che la sfumatura sia sempre
// massima. Deve cambiare il punto di inizio, che in negativo dev'essere lontano (fuori dal
// fotogramma), mentre in positivo arriva poco all'interno del bordo*). Là il raggio vale **1**
// all'angolo e **0,707** a metà di ogni lato, quindi: a `+100` il pieno cade a **0,70**, cioè
// appena dentro il bordo; a riposo cade **sull'angolo**; a `-100` cade a **1,30**, cioè fuori dal
// fotogramma, e là il pieno non si raggiunge mai (all'angolo si ferma all'86%).
// ⚠️⚠️ **E LA RAMPA PARTE DAL CENTRO, CHE È LA 'SFUMATURA SEMPRE MASSIMA'**: più lunga di così non
// può essere, e con `smoothstep` la pendenza al centro è zero, quindi l'alone cresce piano invece
// di cominciare con un gradino. È anche il profilo di un obiettivo vero, che cala da subito.
// ⚠️ **Con lei la corsa negativa si accorcia**, che è il suo primo punto (*il minimo dev'essere ciò
// che adesso è -60: sotto è inutile e direi anche dannoso*): il 'dannoso' erano i quattro angoli
// scuri che la `2.66` lasciava a fondo corsa, e la rampa dal centro toglie la causa invece di
// spostare il limite.
const half VIGNETTE_SOFT = 0.3;

// Quanto la grana muove un pixel di mezzo tono, al fondo della corsa.
//
// ⚠️ **Il conto**: a 100 il grano sposta la luminanza di dodici livelli su 255 in un verso e
// nell'altro, cioè una grana ben visibile al cento per cento e appena percettibile guardando
// l'immagine intera. Sopra si arriva alla neve di una fotografia ad alto ISO, che è rumore e non
// grana.
const half GRAIN_REACH = 0.048;

// Dove comincia e dove finisce la rampa con cui la grana si ritira dalle luci (dalla `2.66`), e
// quanta ne resta là in fondo a cursore 'Luci' a riposo.
//
// ⚠️⚠️ **I TRE NUMERI SI RICAVANO DAI SUOI TRE, E NON SONO UNA TARATURA** (riscontro del giro della
// `2.66`, voce `eff-grana-luci` accettabile: *il 'quasi zero' passa da 0,8 a 0,1. Ovviamente il
// passaggio da ombre a luci dev'essere graduale; metà scala a 0,35*). I vincoli sono tre e ognuno
// fissa un numero: il pavimento è quello che dà **0,1 livelli su 255** sul cielo a `t=0,82` con la
// grana piena (misurato: 0,0997); la soglia bassa cade sul **quarto di scala**, cioè dove finiscono
// le ombre che devono restare intatte; e quella alta è quella che fa valere **0,35** il peso a metà
// scala, che è il numero che ha scritto lui (misurato: 0,3500).
// ⚠️⚠️ **'0,35' È IL PESO E NON I LIVELLI, ED È UNA LETTURA DICHIARATA**: la voce gli diceva che a
// metà tono la grana teneva l'**81%**, e la sua riga risponde a quella; letto in livelli darebbe
// 0,35 su 255 a metà scala contro i 9,2 del quarto, cioè un crollo, che è il contrario del
// *graduale* della stessa frase.
// ⚠️⚠️ **E A 'Luci' PIENO IL CONTO TORNA ESATTAMENTE QUELLO DELLA `2.65`** (misurato: scarto nullo
// su 1001 toni), quindi il comportamento di prima non si perde, si sposta a un capo della corsa.
// ⚠️ **Il prezzo è dichiarato**: la grana vive nelle ombre e nei toni medio-scuri, quindi a metà
// scala tiene il 43% di quanto teneva nella `2.66` e a due terzi il 13%. Il picco si sposta da 0,39
// a **0,31**, che è il *solo alle ombre* portato dove lo ha chiesto.
const half GRAIN_LIFT_LO = 0.25;
const half GRAIN_LIFT_HI = 0.6614;
const half GRAIN_LIFT_FLOOR = 0.0138;

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

// Un campione della mappa del velo: il canale scuro del punto, e il peso con cui conta. Il peso
// cade dove il vicino è diverso dal centro (vedi la nota della `2.60` qui sotto), e i due valori
// tornano insieme perché il chiamante li deve sommare tutti e due.
half2 veilTap(float2 at, half here) {
    half3 s = tap(at);
    half dark = min(min(s.r, s.g), s.b);
    half far = dark - here;
    return half2(dark, half(1.0)) * exp(-far * far * HAZE_EDGE);
}

// Quanto velo c'è intorno a `p`: il **canale scuro**, cioè il minimo dei tre canali, mediato
// sull'intorno con nove campioni su due anelli a distanza `step`.
//
// ⚠️⚠️ **PERCHÉ IL MINIMO DEI TRE CANALI DICA QUANTO VELO C'È, in una riga**: la foschia è luce
// bianca che l'aria aggiunge a tutti e tre i canali insieme, quindi alza anche il più basso; un
// pixel visto senza velo invece ha quasi sempre un canale quasi spento, perché un colore è tale
// proprio quando i tre canali non sono uguali. Un canale scuro alto vuol dire velo, e quanto è
// alto dice quanto ce n'è.
//
// ⚠️⚠️ **SI PRENDE LA MEDIA DEI MINIMI E NON IL MINIMO DEL BLOCCO**, che è la forma classica: il
// minimo su un blocco fa una mappa a gradini, e ogni gradino diventa un alone intorno ai contorni
// forti. La media cambia piano, che è quello che una mappa di velo deve fare.
//
// ⚠️⚠️ **MA DALLA `2.60` QUELLA MEDIA SEGUE I BORDI, E LA MISURA DICE PERCHÉ** (riscontro del giro
// della `2.55`: *Foschia: servirebbero regolazioni più raffinate*). Una media uniforme prende il
// velo del cielo e lo spalma **dentro** il profilo che ha sotto di sé, quindi là la sottrazione
// arriva più in basso di quanto spetti a quei pixel: su un profilo di montagna contro un cielo
// velato a 0,62, la roccia vicino al bordo perdeva **15 livelli** rispetto a quella lontana e
// finiva sul nero, su quattro righe; e il cielo appena sopra il bordo riceveva una stima
// sbagliata di **33 livelli**. Con un peso che cade dove il vicino è diverso dal centro, tutti e
// due gli scarti vanno a **zero**.
// ⚠️ **E il velo vero non si perde**: sul cielo che sfuma dolcemente, cioè il caso per cui questo
// cursore esiste, la mappa coincide col canale scuro a meno di **zero** livelli con e senza il
// peso. Quello che il peso toglie è la sbavatura fra due cose diverse, non la stima.
// ⚠️ **Il peso è quello della riduzione del rumore, con una soglia sua** ([HAZE_EDGE]): là si
// distingue la grana di un sensore da un contorno, qui il velo di una regione da un'altra, e le
// due distanze non sono la stessa.
//
// ⚠️⚠️ **E DALLA `2.65` I NOVE CAMPIONI CADONO SU DUE ANELLI E NON SU UNA GRIGLIA, PERCHÉ UNA
// GRIGLIA FA UN PETTINE** (riscontro del giro chiuso il 2026-09-19, voce `eff-foschia`: *mi sembra
// che adesso i valori negativi introducano un difetto simile a quello già rilevato per Chiarezza e
// Texture*, cioè un reticolo). Nove delta su una griglia a passo `s` hanno una risposta in
// frequenza **periodica**: vale uno a ogni multiplo di `1/s`, cioè ai periodi `s`, `s/2`, `s/3`...
// Là la stima non media affatto, **copia**, quindi la mappa del velo porta intera la trama che a
// quei periodi l'immagine ha. Misurato: ai periodi 16, 8, 5,33 e 4 pixel la risposta valeva
// esattamente **1,000**, contro lo 0,19-0,62 di adesso.
//
// ⚠️⚠️ **E PERCHÉ SI VEDA SOLO IN NEGATIVO LO DICE IL SEGNO**: dove la stima copia il contenuto,
// quelle frequenze si comportano diversamente dalle altre. Nel verso che **aggiunge** velo tutta
// la texture si attenua e quelle no, quindi **sporgono** e si leggono come un reticolo; nel verso
// che toglie tutta la texture si accentua e quelle no, quindi rientrano, e un buco non si nota.
// Sulla texture di prova il pettine passava da 1,236 (l'originale) a **1,312** aggiungendo, e
// adesso vale **1,230**, cioè quanto l'originale.
//
// ⚠️⚠️ **CINQUE E TRE SONO COPRIMI, ED È QUELLO CHE ROMPE LA PERIODICITÀ**: con due anelli di
// quei conti, sfasati, non esiste nessuna direzione in cui i campioni cadano a passo costante, e
// i due raggi non sono in rapporto intero. ⚠️ **Non azzera il massimo fuori banda**, che resta
// 0,870 contro 1,000: quello che cambia è che i picchi residui cadono a frequenze e direzioni
// sparse invece che su una griglia allineata agli assi, quindi non compongono una trama.
//
// ⚠️⚠️ **LA STRADA SCARTATA È LA RIDUZIONE, ED È MIGLIORE SULLA CARTA**: leggere i campioni da
// una versione rimpicciolita dell'immagine porta il massimo fuori banda a **0,246**, perché ogni
// campione è già la media della sua cella. Costa una texture in più nello shader, la sua
// costruzione a ogni tessera del salvataggio, una griglia da allineare fra le tessere e un bordo
// più largo; e sulla texture di prova dà **1,229** contro i 1,230 dei due anelli, cioè lo stesso.
// Chi ci tornasse riparta di lì invece di rifare la misura.
//
// ⚠️⚠️ **E CON LORO SI CHIUDE UN SECONDO DIFETTO CHE NESSUNO AVEVA VISTO: IL FILTRO ARRIVAVA PIÙ
// LONTANO DI QUANTO IL BORDO DELLE TESSERE DICHIARASSE.** I quattro campioni **diagonali** della
// griglia stavano a `step` per radice di due, cioè il **41%** oltre il raggio che
// [Effects.hazeReach] promette a `bleedFor`: su una giunzione, con la foschia mossa, quella fascia
// leggeva il bordo ripetuto invece del pixel che sta di là, ed è la riga che il bordo esiste per
// non far comparire. Adesso l'anello largo tocca esattamente `step` e il raggio massimo è **uno**,
// quindi il bordo dichiarato e quello vero coincidono. ⚠️ **A prenderlo è stata la prova**, non
// una rilettura: il caso 65 di `SviluppoTest` misura il raggio dei campioni letti dalla stringa.
half veiled(float2 p, float2 step) {
    half3 mid = tap(p);
    half here = min(min(mid.r, mid.g), mid.b);
    half2 acc = veilTap(p, here) * HAZE_HUB;
    acc += (veilTap(p + step * float2( 1.0000,  0.0000), here)
          + veilTap(p + step * float2( 0.3090,  0.9511), here)
          + veilTap(p + step * float2(-0.8090,  0.5878), here)
          + veilTap(p + step * float2(-0.8090, -0.5878), here)
          + veilTap(p + step * float2( 0.3090, -0.9511), here)) * HAZE_WIDE;
    acc += (veilTap(p + step * float2( 0.2725,  0.4720), here)
          + veilTap(p + step * float2(-0.5450,  0.0000), here)
          + veilTap(p + step * float2( 0.2725, -0.4720), here)) * HAZE_TIGHT;
    return acc.x / max(acc.y, half(0.0001));
}

// Il modulo Effetti: la foschia. Vignettatura e grana non passano di qui, perché leggono dove si
// trova il pixel invece dei suoi vicini, e vivono in fondo alla catena.
//
// ⚠️ **Il contrasto locale non c'è più dalla `2.64`**, ed è la sua risposta `via` a
// `d-eff-restano`: il perché, e il difetto misurato che lo ha tolto, vivono in testa a `Effects`.
half3 localed(float2 p, half3 c) {
    half3 done = c;

    if (abs(haze) > half(0.0)) {
        // Il modello atmosferico, con la luce dell'aria presa bianca: quello che si vede è
        // l'immagine vera attenuata più il velo, cioè `c = j (1 - k) + k`. Toglierlo vuol dire
        // invertire quella riga, aggiungerlo vuol dire applicarla.
        half k = abs(haze) * HAZE_REACH * veiled(p, broad);
        // ⚠️ **Il ramo è UNIFORME e non divergente**: il velo stimato non è mai negativo, quindi
        // il segno di `k` è quello del cursore, che è un uniform. Scritto come un `mix` sui due
        // risultati costerebbe le due formule su ogni pixel per non guadagnare niente.
        if (haze > half(0.0)) {
            // ⚠️⚠️ **IL VELO TOLTO NON SUPERA QUELLO CHE IL PIXEL PORTA, DALLA `2.60`, E SENZA
            // QUESTA RIGA LE OMBRE SI CHIUDEVANO TUTTE SULLO STESSO NERO**: la sottrazione azzera
            // quello che resta sotto `k`, quindi in una zona velata a 0,62 i **64** livelli sotto
            // il quarto di scala uscivano identici, cioè ne restava **uno**. Col tetto ne restano
            // **40** distinti.
            // ⚠️ **Il tetto è lo stesso fattore applicato al canale scuro del PIXEL**, non una
            // costante nuova: dove il velo c'è davvero quel numero vale quanto la stima, quindi
            // il `min` non entra in funzione (misurato: **zero** livelli di scarto su una zona
            // uniforme e sui pixel chiari). Entra solo dove il pixel è più scuro del proprio
            // intorno, cioè su un dettaglio fine che la stima non ha visto, ed è il posto in cui
            // il disegno spariva.
            half own = min(min(c.r, c.g), c.b);
            half take = min(k, HAZE_REACH * own);
            done = (done - half3(take)) / (half(1.0) - take);
        } else {
            // ⚠️⚠️ **QUI IL TETTO NON C'È, ED È UNA SCELTA E NON UNA DIMENTICANZA**: aggiungere
            // velo non azzera nessun pixel, quindi non c'è niente da proteggere; e limitarlo sugli
            // scuri toglierebbe al cursore proprio quello che fa da questa parte, cioè alzare le
            // ombre. ⚠️ **Il prezzo è che i due versi non si disfanno più esattamente dove il
            // tetto entra in funzione**, e si dichiara: là togliere il velo ne toglie meno di
            // quanto rimetterlo ne rimetta.
            done += half3(k) * (half3(1.0) - done);
        }
    }

    return clamp(done, half3(0.0), half3(1.0));
}

// Quanto un punto è lontano dal centro dell'immagine INTERA, da zero al centro a uno all'angolo.
//
// ⚠️⚠️ **SI MISURA SULLA MEZZA DIAGONALE E NON SUL LATO, E LA DIFFERENZA SI VEDE SUI PANORAMI**:
// dividendo per il lato, su un'immagine allungata lo stesso valore del cursore scurirebbe i due
// lati corti molto più degli altri due. Con la diagonale l'angolo vale uno su qualunque formato,
// che è quello che fa una vignettatura d'obiettivo.
//
// ⚠️ **`p` vive nello spazio del pezzo che si sta disegnando**, quindi si toglie l'origine
// dell'immagine intera: è la sola riga che rende il conto uguale sull'anteprima e sul file salvato
// a pezzi.
half fromCentre(float2 p) {
    float2 here = (p - spot) / max(frame, float2(1.0));
    float2 off = here - float2(0.5);
    return half(min(length(off) / 0.70710678, 1.0));
}

// Il rumore della grana: un valore fra -1 e 1 che cambia da cella a cella, interpolato dentro.
//
// ⚠️⚠️ **IL SEME È LA COORDINATA ASSOLUTA E NON QUELLA DELLA TESSERA**: con la coordinata locale
// ogni tessera del salvataggio porterebbe la **stessa** grana, cioè un motivo a scacchi grande
// quanto una tessera, e sull'anteprima (dove la tessera è una sola) non si vedrebbe.
//
// ⚠️ **La cella è interpolata e non a blocchi**: un valore per cella darebbe dei quadretti, e
// l'interpolazione morbida li scioglie in un grano tondo, che è quello che si vede su una
// pellicola.
half speck(float2 p) {
    float2 here = (p - spot) / max(filmCell, 1.0);
    float2 cell = floor(here);
    float2 t = fract(here);
    float2 w = t * t * (float2(3.0) - 2.0 * t);
    // Quattro valori casuali agli angoli della cella, dallo stesso hash.
    float a = fract(sin(dot(cell, float2(12.9898, 78.233))) * 43758.5453);
    float b = fract(sin(dot(cell + float2(1.0, 0.0), float2(12.9898, 78.233))) * 43758.5453);
    float c = fract(sin(dot(cell + float2(0.0, 1.0), float2(12.9898, 78.233))) * 43758.5453);
    float d = fract(sin(dot(cell + float2(1.0, 1.0), float2(12.9898, 78.233))) * 43758.5453);
    float top = mix(a, b, w.x);
    float bottom = mix(c, d, w.x);
    return half(mix(top, bottom, w.y) * 2.0 - 1.0);
}

// La curva del contrasto, su un valore in [0, 1] e col perno in mezzo: la diagonale più una gobba
// dispari, che allontana dal perno per k positivo e ci avvicina per k negativo.
//
// ⚠️⚠️ **I DUE ESTREMI NON SI MUOVONO E LÀ LA PENDENZA VALE UNO, ED È QUELLO CHE LA DISTINGUE
// DALLE DUE FORME DELLA `2.14`**, che lui ha bocciate in un colpo solo (campo libero del giro
// della `2.55`: *mi è sembrato un po' troppo vecchia scuola, e fa diventare tutto un po' troppo
// grigio in negativo*). Erano due difetti distinti con la stessa radice, cioè una curva che
// lavorava sugli estremi invece che sui mezzi toni:
// - **In su c'era una `smoothstep` mescolata alla diagonale**, e quella ha pendenza **zero** agli
//   estremi: a fondo corsa dei 26 livelli sotto il 10% di scala ne restavano **8**, e altrettanti
//   in cima. Il contrasto sembrava forte perché mangiava il dettaglio nelle ombre e nei chiari,
//   mentre al perno la pendenza arrivava solo a 1,5. Adesso i livelli che restano sono **21** e
//   **22**, e al perno la pendenza è **2**.
// - **In giù c'era una compressione lineare verso il perno**, che sposta anche il nero e il
//   bianco: a -100 il nero usciva a 0,30 (livello 76) e il bianco a 0,70 (livello 178), cioè
//   l'immagine viveva in 102 livelli su 255. Ecco il grigio. Adesso il nero resta **0** e il
//   bianco **1**, e a comprimersi sono i soli mezzi toni.
//
// ⚠️ **La gobba è `(t - 0.5)` per `(4t(1-t))` al cubo**: quel secondo fattore vale uno al perno e
// zero ai due estremi **con derivata nulla**, quindi la curva ci arriva tangente alla diagonale.
// Al cubo e non al quadrato perché il cubo concentra la gobba sui mezzi toni: a parità di
// pendenza al perno la pendenza minima passa da 0,20 a **0,35**, cioè i quarti di tono si
// comprimono meno.
// ⚠️ **E si scrive `4t(1-t)` e non `1 - u*u`**, che è lo stesso numero: in `half` la seconda
// forma cancella le cifre proprio dove il fattore va a zero, e la prima non ha niente da
// cancellare.
half sCurve(half x, half k) {
    half t = clamp(x, half(0.0), half(1.0));
    half s = k >= half(0.0) ? k * CONTRAST_RISE : k * CONTRAST_FALL;
    half bump = half(4.0) * t * (half(1.0) - t);
    return t + s * (t - half(0.5)) * bump * bump * bump;
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

    // 0-bis. Gli Effetti, cioè la foschia, subito dopo il Dettaglio e per la stessa ragione:
    // legge `image`, cioè i pixel di partenza, quindi deve stare dove quella lettura vale ancora.
    // E dopo la riduzione del rumore, o la stima del velo leggerebbe la grana che il Dettaglio ha
    // appena mediato.
    // ⚠️ La guardia uniforme vale come quella del Dettaglio: a riposo i nove campioni non si
    // chiedono nemmeno.
    if (effectsOn > half(0.5)) {
        c = localed(p, c);
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
    // ⚠️⚠️ **IL GRIGIO DEL BIANCO E NERO È UN ALTRO, DALLA `2.35`, E SI PRENDE QUI**: i pesi sono
    // quelli del cursore 'Filtro', e il punto in cui si legge `rgb` è lo stesso di `grey`, cioè
    // **prima** della saturazione. Senza quella cura il filtro dipenderebbe da un cursore che con
    // lui non c'entra, che è esattamente la ragione per cui il bianco e nero non è la saturazione
    // a -100.
    // ⚠️ **Si calcola sempre, anche a bianco e nero spento**: sono tre moltiplicazioni, e un ramo
    // costerebbe di più di quanto risparmia.
    half bw = dot(rgb, greyMix);
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
    rgb = mix(rgb, half3(bw), mono);

    // 8. La vignettatura, che viene DOPO tutto quello che parla di colore e non è un giudizio
    // sull'immagine: è quello che fa un obiettivo, cioè meno luce ai bordi del fotogramma. Messa
    // prima, ogni cursore della Luce e il contrasto la rimetterebbero in discussione, e un
    // 'Auto' calcolato su un'immagine già vignettata leggerebbe un istogramma che non è il suo.
    if (abs(vignette) > half(0.0)) {
        half r = fromCentre(p);
        // ⚠️⚠️ **LA RAMPA PARTE DAL CENTRO E FINISCE DOVE L'ALONE È PIENO, DALLA `2.67`**: è la
        // 'sfumatura sempre massima' che ha chiesto, cioè la transizione più lunga che il
        // fotogramma consenta. `smoothstep` parte con pendenza zero, quindi il centro non si
        // scurisce con un gradino, che era la ragione per cui fino alla `2.66` la corsa cominciava
        // a metà raggio.
        // ⚠️⚠️ **A MUOVERSI È IL PUNTO DEL PIENO, E LO SPOSTA IL CURSORE 'Sfumatura'**: fuori dal
        // fotogramma verso il negativo, appena dentro il bordo verso il positivo (vedi
        // [VIGNETTE_SOFT]). ⚠️ **Quindi a parità di cursore principale l'alone è più esteso di
        // quello della `2.66`**, e chi aveva una vignettatura tarata la ritrova più diffusa: è la
        // conseguenza diretta della sfumatura massima, e si dichiara.
        half end = half(1.0) - vignetteFeather * VIGNETTE_SOFT;
        half fall = half(smoothstep(0.0, float(end), float(r)));
        // Il fattore moltiplica la luce: verso il basso scurisce l'angolo, verso l'alto lo apre.
        half k = vignette * VIGNETTE_REACH * fall;
        rgb = k >= half(0.0)
            ? mix(rgb, half3(1.0), k)
            : rgb * (half(1.0) + k);
    }

    // 9. La grana, che viene per ULTIMA perché è la pellicola: tutto quello che c'è sopra
    // descrive l'immagine, questa descrive il supporto su cui è stampata. Messa prima, il
    // contrasto e la saturazione la tratterebbero come disegno e la moltiplicherebbero.
    if (filmGrain > half(0.0)) {
        // ⚠️⚠️ **PESA SUI MEZZI TONI, E SENZA QUELLA RIGA SAREBBE RUMORE DIGITALE**: una pellicola
        // mostra il grano dove l'emulsione è esposta a metà, e quasi niente nel nero chiuso e nel
        // bianco bruciato. Il conto è `4t(1-t)`, scritto come uno meno il quadrato dello scarto
        // dal centro.
        half tone = luma(rgb);
        half off = half(2.0) * tone - half(1.0);
        half mid = half(1.0) - off * off;
        // ⚠️⚠️ **E DALLA `2.66` SI RITIRA ANCHE DALLE LUCI, QUANTO LO DICE IL CURSORE 'Luci'**
        // (sua richiesta: *di default la grana sia aggiunta solo alle ombre, e alle luci in misura
        // non proprio zero ma quasi ... per evitare che ad aree uniformi come il cielo sia
        // aggiunta grana inutilmente*). L'inviluppo qui sopra tiene fuori il nero chiuso e il
        // bianco bruciato; questa rampa toglie la fascia in mezzo dove vive un cielo, e a cursore
        // pieno non toglie niente, cioè rimette il conto della `2.65`.
        half up = half(smoothstep(float(GRAIN_LIFT_LO), float(GRAIN_LIFT_HI), float(tone)));
        half keep = mix(GRAIN_LIFT_FLOOR, half(1.0), grainLift);
        mid = mid * mix(half(1.0), keep, up);
        // ⚠️ **Si somma lo stesso valore ai tre canali**: la grana di una pellicola in bianco e
        // nero è di densità e non di colore, e un rumore per canale darebbe i puntini colorati
        // del sensore, cioè proprio quello che la riduzione del rumore esiste per togliere.
        rgb = rgb + half3(speck(p) * filmGrain * GRAIN_REACH * mid);
    }

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
 * Dov'è il pezzo che si sta disegnando, dentro l'immagine **intera**, e quanto misura lei.
 *
 * ⚠️⚠️ **NASCE CON LA `2.57`, E PRIMA ERA UN NUMERO SOLO**: fino alla `2.56` allo shader si
 * consegnava il lato lungo ([span]), che basta a chi ragiona in frazioni del lato, cioè al
 * Dettaglio e ai primi tre cursori degli Effetti. La vignettatura e la grana invece chiedono
 * un'altra cosa: **dove** si trova questo pezzo, perché una vignettatura si misura dal centro
 * dell'immagine e una grana deve essere continua da una tessera all'altra.
 *
 * ⚠️⚠️ **E I DUE DATI STANNO INSIEME PERCHÉ SONO LA STESSA COSA DETTA PER INTERO**: il lato lungo
 * si **ricava** da qui ([span]), quindi non esiste più un secondo posto in cui scriverlo e non
 * può divergere dall'origine. Chi disegna un pezzo lo dichiara con [piece], chi disegna tutto con
 * [whole]: non c'è un valore di serie da dimenticare, che è lo stesso presidio del parametro di
 * `Modifier.lowered`.
 */
// ⚠️ **Anche `copy()` è privato, come il costruttore**: senza quell'annotazione una data class dal
// costruttore privato lo genera pubblico, e `copy(left = ...)` sarebbe la quarta strada, cioè
// esattamente quella che le tre funzioni qui sotto esistono per non lasciare aperta.
@ConsistentCopyVisibility
internal data class Framed private constructor(
    /** La larghezza dell'immagine intera, nello spazio in cui il conto gira. */
    val wide: Float,
    /** L'altezza dell'immagine intera, nello stesso spazio. */
    val tall: Float,
    /** Dove comincia l'immagine intera, in quello spazio: per una tessera è negativo. */
    val left: Float,
    /** Dove comincia l'immagine intera, in quello spazio: per una tessera è negativo. */
    val top: Float
) {
    /** Il lato lungo dell'immagine intera: è quello che i raggi in frazione del lato leggono. */
    val span: Float get() = max(wide, tall)

    companion object {
        /**
         * L'immagine intera, che comincia a zero: il file pieno disegnato in un colpo, e le prove.
         */
        fun whole(wide: Float, tall: Float) = Framed(wide, tall, 0f, 0f)

        /**
         * L'immagine intera disegnata dentro un rettangolo dello schermo, cioè il palco
         * dell'editor: [left] e [top] sono l'angolo di quel rettangolo.
         */
        fun shown(left: Float, top: Float, wide: Float, tall: Float) =
            Framed(wide, tall, left, top)

        /**
         * Una tessera del salvataggio, dichiarata col **proprio** angolo dentro l'immagine
         * ([atX], [atY]) e con la misura dell'immagine intera.
         *
         * ⚠️⚠️ **IL SEGNO LO METTE QUESTA FUNZIONE E NON IL CHIAMANTE**: dentro una tessera `p`
         * parte da zero sul suo angolo, quindi l'immagine intera comincia **più indietro**, cioè a
         * un'origine negativa. Scritta dal chiamante, quella negazione sarebbe una riga da
         * ricordare e un difetto che non dà nessun errore: la vignettatura cadrebbe fuori centro
         * di tanto quanto la tessera è lontana dall'angolo.
         */
        fun tile(atX: Float, atY: Float, wide: Float, tall: Float) =
            Framed(wide, tall, -atX, -atY)
    }
}

/**
 * Il programma compilato con [look] dentro, agganciato all'immagine [image], oppure `null` dove
 * questa strada non esiste. [where] dice quanto misura l'immagine **nello spazio in cui il conto
 * gira** e dov'è il pezzo che si disegna: i pixel del file per il salvataggio, il rettangolo
 * disegnato per l'anteprima.
 *
 * ⚠️⚠️ **[where] NON HA UN VALORE DI SERIE, E NON È UNA DIMENTICANZA**: il modulo Dettaglio ragiona
 * in frazioni del lato e la vignettatura in frazioni della diagonale, quindi chi chiama deve
 * **dichiarare** quanto misura l'immagine da cui legge e dov'è il proprio pezzo. Un valore di
 * serie sarebbe giusto per uno dei due chiamanti e sbagliato per l'altro, senza che niente lo
 * dica: è la stessa forma di presidio del parametro di `Modifier.lowered`.
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
internal fun lookShader(image: Shader, look: Look, where: Framed): Shader? {
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
    return runCatching { lightOver(image, look, where) }.getOrNull()
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
private fun lightOver(image: Shader, look: Look, where: Framed): Shader {
    val light = look.light
    val chroma = look.chroma
    val detail = look.detail
    val effects = look.effects
    val span = where.span
    val sharp = detail.sharpReach(span)
    val grain = Detail.grainReach(span)
    val broad = Effects.hazeReach(span)
    val cell = Effects.grainCell(span, effects.grainSize)
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
        val miscela = chroma.grey
        setFloatUniform("greyMix", miscela[0], miscela[1], miscela[2])
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
        setFloatUniform("effectsOn", if (effects.idle) 0f else 1f)
        setFloatUniform("haze", effects.haze)
        setFloatUniform("broad", broad, broad)
        setFloatUniform("vignette", effects.vignette)
        setFloatUniform("vignetteFeather", effects.vignetteFeather)
        setFloatUniform("filmGrain", effects.grain)
        setFloatUniform("grainLift", effects.grainLift)
        setFloatUniform("filmCell", cell)
        setFloatUniform("spot", where.left, where.top)
        setFloatUniform("frame", where.wide, where.tall)
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

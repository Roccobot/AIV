package io.github.roccobot.aiv

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Shader
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow

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
 * Tutto quello che l'editor completo sa fare a un'immagine, in un oggetto solo.
 *
 * ⚠️⚠️ **È UN VALORE E NON UNA CATENA DI GESTI, ed è la stessa scelta dell'editor di casa**: là
 * dieci rotazioni si compongono in una posa sola perché le pose sono otto; qui dieci
 * spostamenti di un cursore sono **un** valore di quel cursore. Un elenco di gesti costringerebbe
 * a riapplicarli uno per uno sul file pieno, cioè a rifare dieci volte lo stesso lavoro.
 *
 * ⚠️⚠️ **CRESCE COI MODULI E LA SUA FORMA NON CAMBIA**: [chroma] è entrato accanto a [light] con
 * la `2.19`, [mix] con la `2.21` e [detail] con la `2.22`, senza toccare niente di quello che legge
 * questo oggetto, e le curve e la geometria entreranno allo stesso modo. Chi li aggiunge tocca
 * [idle] e [lossless], che sono le due domande che tutto il resto fa qui, e nient'altro.
 */
data class Look(
    val light: Light = Light.NONE,
    val chroma: Chroma = Chroma.NONE,
    val mix: Mix = Mix.NONE,
    val detail: Detail = Detail.NONE
) {

    /** Se non c'è niente da applicare: l'immagine esce identica a com'è entrata. */
    val idle: Boolean get() = light.idle && chroma.idle && mix.idle && detail.idle

    /**
     * Se quello che c'è da fare **non** riscrive i pixel.
     *
     * ⚠️⚠️ **È LA CLAUSOLA DELL'UTENTE, e per questo è una proprietà del modello e non una
     * riga nel salvataggio** (*quelle che non prevedono la riscrittura del file pixel per pixel
     * devono essere lossless*): finché la pila contiene solo posa e ritaglio senza taglio, il
     * file si può girare cambiando un tag EXIF, che è quello che l'editor di casa fa dalla
     * `1.03`. Appena entra un valore di Luce, i pixel vanno riscritti e non c'è modo di
     * evitarlo.
     * ⚠️ **Quando la geometria entrerà in questo oggetto** (quarto giro, col raddrizzamento), la
     * risposta resta esattamente questa: la posa non tocca i pixel, tutto il resto sì.
     */
    val lossless: Boolean get() = idle

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
 * nero, poi il contrasto, poi l'HSL per fascia, e per ultimo quanto sono accesi i colori. È
 * l'ordine di un banco di sviluppo fotografico, e la ragione di ognuno dei passaggi:
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

    // 5. L'HSL per fascia: gli stessi tre comandi su otto colori.
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

    // 6. Quanto sono accesi i colori, e viene per ULTIMO perché è un giudizio sull'immagine
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

package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Il banco di prova del conto che sceglie **che pezzo di file rileggere** quando si ingrandisce.
 *
 * ⚠️⚠️ **NASCE COL TRASLOCO DELLA `2.27`, E MISURA UN CONTO CHE PRIMA ERA DI UNO SOLO**: fino
 * alla `2.26` viveva dentro `ViewerScreen` e lo chiamava il solo visualizzatore; adesso è
 * [sharpAsk] e lo chiede anche l'editor completo, che lavora su un'anteprima da 1600 pixel di
 * lato. Un difetto qui si vede in due posti, e in nessuno dei due dà errore: si vede un'immagine
 * sfocata dove doveva essere nitida, oppure un pezzo da decine di megabyte chiesto per niente.
 *
 * ⚠️ **Quello che il banco non vede**: che il pezzo letto sia davvero nitido, che finisca nel
 * posto giusto sullo schermo e che il file si sappia rileggere a pezzi. Quelle cose vogliono un
 * file vero e un `BitmapRegionDecoder` che lo apra, e si guardano sul telefono: la voce di
 * collaudo lo chiede.
 *
 * ⚠️⚠️ **E DALLA `2.28` MISURA ANCHE I DUE CONTI DEL PEZZO**, cioè [SharpPiece.place] e
 * [SharpPiece.pixel]: da quando la lente del colore mirato mostra il pezzo invece dell'anteprima,
 * ognuno dei due ha **due** chiamanti, e quello che i due devono dire è la stessa cosa. Sono
 * misurabili qui perché il trasloco della stessa versione li ha portati in `Regions.kt`: un pezzo
 * si costruisce con un bitmap scritto a mano, senza nessun file da aprire.
 *
 * ⚠️ **Ogni caso è controprovato rimettendo il difetto**, come prescrive
 * `AIV/CLAUDE.md` § '🧪 Quando si scrive una prova, e quando no': togliendo la guardia
 * dell'ingrandimento il caso 1 passa a `Read`, togliendo il tetto il caso 5 chiede un pezzo da
 * cinque milioni e mezzo di pixel, ricavando l'area dalle misure dell'anteprima invece che da
 * quelle del file il caso 3 la trova nell'angolo sbagliato, scambiando i due assi in [place] il
 * caso 7 posa il pezzo fuori dall'immagine, e leggendo le frazioni senza togliere l'origine del
 * pezzo il caso 8 prende l'ultimo pixel al posto di quello di mezzo.
 */
@RunWith(AndroidJUnit4::class)
class TasselloTest {

    /** Un file da 24 megapixel: quello che fa un telefono di oggi. */
    private val fileWide = 6000
    private val fileHigh = 4000

    /** L'anteprima dell'editor completo: 1600 sul lato lungo. */
    private val previewWide = 1600

    /** Il palco: quello che resta a un'immagine sopra la scheda dei cursori. */
    private val roomWide = 1080f
    private val roomHigh = 1500f

    /**
     * Dove finisce sullo schermo un'immagine adattata al palco e ingrandita [zoom] volte, attorno
     * al centro: è quello che [sharpAsk] riceve, e qui si costruisce con due righe invece che
     * chiamando la geometria dell'editor, che è privata.
     */
    private fun disegnata(zoom: Float): RectF {
        val wide = fileWide.toFloat() / fileHigh
        val w = roomWide * zoom
        val h = w / wide
        return RectF(
            (roomWide - w) / 2f,
            (roomHigh - h) / 2f,
            (roomWide + w) / 2f,
            (roomHigh + h) / 2f
        )
    }

    private fun chiedi(zoom: Float, base: Int = previewWide) =
        sharpAsk(fileWide, fileHigh, base, disegnata(zoom), roomWide, roomHigh)

    /**
     * **Caso 1: finché l'anteprima non è ingrandita oltre i propri pixel, non si legge niente.**
     *
     * ⚠️ È la soglia **ricavata** e non un numero: sotto quel confine un pixel dell'anteprima
     * copre meno di un pixel di schermo, quindi il dettaglio che manca non si vedrebbe comunque e
     * la lettura sarebbe lavoro puro.
     */
    @Test
    fun `a scala uno l'anteprima basta`() {
        val esito = chiedi(1f)
        assertTrue("A scala uno non si deve leggere niente: $esito", esito is Sharpening.None)
        assertEquals("no tile: zoom", (esito as Sharpening.None).why)
    }

    /**
     * **Caso 2: ingrandita oltre i suoi pixel, si legge un pezzo del file.**
     *
     * ⚠️ Il pezzo deve stare **dentro** l'immagine: un rettangolo che sborda manda in errore
     * `decodeRegion`, e là il rimedio è rinunciare, cioè restare sfocati.
     */
    @Test
    fun `ingrandendo si chiede un pezzo, dentro i bordi`() {
        val esito = chiedi(3f)
        assertTrue("Ingrandendo si deve leggere: $esito", esito is Sharpening.Read)
        val letto = esito as Sharpening.Read
        assertTrue("Campionamento sotto uno: ${letto.sample}", letto.sample >= 1)
        assertTrue("Il pezzo sborda: ${letto.area}", letto.area.left >= 0 && letto.area.top >= 0)
        assertTrue(
            "Il pezzo sborda: ${letto.area}",
            letto.area.right <= fileWide && letto.area.bottom <= fileHigh
        )
    }

    /**
     * **Caso 3: il pezzo è la porzione che si vede, non tutta la fotografia.**
     *
     * ⚠️ È metà della ragione per cui questa strada esiste: leggere tutto a piena risoluzione
     * sarebbe la decodifica intera che l'anteprima serve a evitare.
     */
    @Test
    fun `il pezzo copre la sola porzione inquadrata`() {
        val letto = chiedi(6f) as Sharpening.Read
        /*
         * ⚠️ **Il pezzo si misura in PIXEL DEL FILE, e l'intervallo è quello che lo dice**: a sei
         * volte si vede un sesto della larghezza, cioè mille pixel di seimila, e in mezzo.
         * Ricavando l'area dalle misure dell'anteprima verrebbe un sesto di 1600, cioè un
         * rettangolo tutto spostato verso l'angolo: un'asserzione che dicesse solo 'è piccolo'
         * resterebbe verde con quel difetto, ed è stato misurato.
         */
        assertTrue(
            "Un sesto dell'immagine, cioè mille pixel: ${letto.area.width()}",
            letto.area.width() in 900..1100
        )
        assertTrue(
            "E in mezzo alla fotografia, non a ${letto.area.left}",
            letto.area.left in 2400..2600
        )
    }

    /**
     * **Caso 4: se il bitmap in mano ha già tutti i pixel del file, non si legge mai.**
     *
     * ⚠️ È il caso del visualizzatore con una fotografia entrata intera in memoria: leggere
     * darebbe gli stessi pixel che si stanno già disegnando, per quanto si ingrandisca.
     * ⚠️ **L'ingrandimento è oltre la soglia di proposito**: sotto, a rispondere sarebbe la
     * guardia dell'ingrandimento, e la prova direbbe una cosa che sa già dal caso 1.
     */
    @Test
    fun `senza guadagno non si legge niente`() {
        val esito = chiedi(6f, base = fileWide)
        assertTrue("Senza guadagno non si deve leggere: $esito", esito is Sharpening.None)
        assertEquals("no tile: gain", (esito as Sharpening.None).why)
    }

    /**
     * **Caso 5: il tetto alza il campionamento invece di chiedere una montagna di pixel.**
     *
     * ⚠️ Il tetto si conta in **schermate** e non in megabyte: quello che deve restare vero è che
     * un pezzo non pesi molto più di quello che si sta guardando, e uno schermo grande può
     * permettersi di più di uno piccolo.
     * ⚠️ **L'ingrandimento è quello del caso 2 di proposito**: là il campionamento che si vorrebbe
     * è **uno**, e il pezzo verrebbe di cinque milioni e mezzo di pixel; è il tetto a portarlo a
     * due, e togliendolo questa prova lo dice.
     */
    @Test
    fun `il tetto alza il campionamento`() {
        val letto = chiedi(3f) as Sharpening.Read
        assertTrue("Il campionamento deve salire: ${letto.sample}", letto.sample > 1)
        val pixels =
            (letto.area.width().toLong() / letto.sample) * (letto.area.height().toLong() / letto.sample)
        assertTrue("Il pezzo pesa $pixels pixel, troppo per questo schermo", pixels <= 4_000_000L)
    }

    /**
     * **Caso 6: su un file enorme il campionamento parte già alto.**
     *
     * ⚠️ Non è il tetto: è il conto che guarda quanti pixel del file cadono in un pixel di
     * schermo. Su un file da 300 megapixel ingrandito appena sopra la soglia ce ne cadono undici,
     * e leggerli tutti vorrebbe dire sfocare per eccesso di dettaglio, oltre che per spreco.
     */
    @Test
    fun `su un file enorme si campiona da subito`() {
        val esito = sharpAsk(
            20000, 15000, previewWide,
            RectF(-310f, 112f, 1390f, 1387f), roomWide, roomHigh
        )
        val letto = esito as Sharpening.Read
        assertTrue("Undici pixel per pixel: il campionamento è ${letto.sample}", letto.sample >= 8)
    }

    /**
     * Un pezzo finto che copre la porzione centrale dell'immagine, con ogni pixel di un colore che
     * dice da dove viene: così un colore letto è insieme il valore e il punto che lo porta.
     *
     * ⚠️ **I lati sono dispari e diversi fra loro**: con un pezzo quadrato uno scambio degli assi
     * non si vedrebbe, e con lati pari il pixel di mezzo non esisterebbe.
     */
    private fun pezzo(): SharpPiece {
        val larghi = 9
        val alti = 5
        val pixels = Bitmap.createBitmap(larghi, alti, Bitmap.Config.ARGB_8888)
        for (y in 0 until alti) {
            for (x in 0 until larghi) {
                pixels.setPixel(x, y, 0xFF000000.toInt() or (x shl 8) or y)
            }
        }
        return SharpPiece(
            pixels = pixels,
            area = Rect(1200, 1600, 3600, 3200),
            at = RectF(0.2f, 0.4f, 0.6f, 0.8f)
        )
    }

    /**
     * **Caso 7: il pezzo si posa dove dicono le sue frazioni, sul rettangolo che gli si dà.**
     *
     * ⚠️ **La vista non è quadrata di proposito**: è quello che distingue il conto giusto da uno
     * che misuri tutti e due gli assi sulla larghezza, e su un palco quadrato i due coinciderebbero.
     * ⚠️ **Chi glielo chiede sono due**, il palco con la propria vista e la lente con quella
     * ingrandita del suo tondo: è la ragione per cui questo conto è una funzione e non due righe
     * scritte due volte.
     */
    @Test
    fun `il pezzo si posa dove dicono le frazioni`() {
        val dove = pezzo().place(RectF(100f, 200f, 500f, 400f))
        assertEquals("Bordo sinistro", 180f, dove.left, 0.01f)
        assertEquals("Bordo di sopra", 280f, dove.top, 0.01f)
        assertEquals("Bordo destro", 340f, dove.right, 0.01f)
        assertEquals("Bordo di sotto", 360f, dove.bottom, 0.01f)
    }

    /**
     * **Caso 8: il colore preso è quello del pixel del file sotto il punto.**
     *
     * ⚠️⚠️ **LE FRAZIONI IN INGRESSO SONO QUELLE DELL'IMMAGINE INTERA, NON DEL PEZZO**, ed è il
     * difetto che questa prova esiste per prendere: leggendole come frazioni del pezzo, il punto di
     * mezzo darebbe l'ultimo pixel e il colore preso sarebbe quello di un altro punto della
     * fotografia. Non dà nessun errore, e a occhio non si distingue da una scelta legittima.
     * ⚠️ **Il colore dice dove sta**, quindi l'asserzione nomina il pixel e non un valore: `0x0402`
     * è il pixel che cade a quattro colonne e due righe, cioè il centro.
     */
    @Test
    fun `il colore preso viene dal pixel giusto del pezzo`() {
        val fine = pezzo()
        assertEquals("Il primo pixel", 0xFF000000.toInt(), fine.pixel(0.2f, 0.4f))
        assertEquals("Quello di mezzo", 0xFF000402.toInt(), fine.pixel(0.4f, 0.6f))
        assertEquals("L'ultimo", 0xFF000804.toInt(), fine.pixel(0.6f, 0.8f))
    }

    /**
     * **Caso 9: fuori dalla finestra inquadrata non c'è colore, e non è un errore.**
     *
     * ⚠️ Il pezzo copre quello che si vede, e il mirino si trascina anche oltre: là il `null` manda
     * il chiamante all'anteprima, che l'immagine intera ce l'ha. Senza questa guardia si
     * prenderebbe il pixel del bordo del pezzo, cioè un colore sbagliato spacciato per buono.
     */
    @Test
    fun `fuori dal pezzo non si prende niente`() {
        val fine = pezzo()
        assertNull("A sinistra del pezzo", fine.pixel(0.1f, 0.6f))
        assertNull("Sopra il pezzo", fine.pixel(0.4f, 0.1f))
        assertNull("A destra del pezzo", fine.pixel(0.9f, 0.6f))
        assertNull("Sotto il pezzo", fine.pixel(0.4f, 0.95f))
    }
}

package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Il banco che guarda i **pixel** del gradiente dell'intestazione: dentro c'è il rumore che gli
 * toglie le bande.
 *
 * ⚠️⚠️ **NASCE DA UNA SUA SEGNALAZIONE, ED È LA REGOLA** (`AIV/CLAUDE.md`, § '🧪 Quando si scrive
 * una prova, e quando no'): nel giro della `2.03` ha scritto *vedo ancora del banding*, cioè la
 * seconda volta sullo stesso difetto dopo la `1.95`. Una correzione che esce senza una misura è
 * una correzione che può tornare indietro un'altra volta.
 * ⚠️⚠️ **QUELLO CHE SI MISURA È IL RUMORE, NON LA BELLEZZA**: se le strisce si vedano è una
 * percezione, e il banco non la sa guardare; che i pixel di una riga siano tutti uguali è un
 * fatto, e una riga tutta uguale è esattamente il mattone di cui una banda è fatta. Con il dither
 * i pixel vicini cadono su livelli diversi, e la riga diventa mista.
 * ⚠️ **Che cosa NON vede**: quanto il rimedio migliori le cose sul suo telefono. Là il taglio a
 * otto bit lo fa la GPU, qui il disegno è software, e il rumore è la cosa che i due percorsi
 * hanno in comune.
 */
@RunWith(AndroidJUnit4::class)
/*
 * ⚠️ **La grafica vera vale per la classe**, come in `SaltiSfondoTest`: senza `NATIVE`
 * `captureToImage` restituisce un'immagine vuota, cioè una prova che passa con qualunque codice.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BandeTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **Nel gradiente le righe non sono di un colore solo, e il rumore è di un livello.**
     *
     * ⚠️⚠️ **LE DUE ASSERZIONI GUARDANO I DUE VERSI DELLO STESSO NUMERO**: la prima dice che il
     * rumore c'è (senza, ogni riga è un colore solo e le bande sono quelle), la seconda che è
     * **piccolo**, cioè un livello e non una grana. Con la sola prima, un rumore dieci volte più
     * forte passerebbe: toglierebbe le bande e metterebbe al loro posto una tinta sporca.
     * ⚠️⚠️ **CONTROPROVATA DUE VOLTE, E I TRE NUMERI DICONO PIÙ DELLA PROVA**: senza nessun dither
     * le righe miste sono **0 su 210**, col solo `isDither` del paint (quello della `1.95`) sono
     * **184**, col rumore nostro passano la soglia. Cioè il rimedio di allora sul banco **agisce**,
     * e quello di adesso lo porta a compimento: la prova prende il caso nudo con un margine largo e
     * distingue anche i due dither, che è quanto una misura di struttura può dire qui.
     */
    @Test
    fun `il gradiente ha il rumore che gli toglie le bande`() {
        banco.setContent {
            Box(modifier = Modifier.background(Color.White)) {
                Box(
                    modifier = Modifier
                        .size(LARGA.dp, ALTA.dp)
                        .testTag(FASCIA)
                        .frontWash(tint = TINTA, air = 0.dp, up = 0.dp, bar = 0.dp) { 1f }
                )
            }
        }
        banco.waitForIdle()

        val mappa = banco.onNodeWithTag(FASCIA).captureToImage().toPixelMap()
        /*
         * ⚠️ **Si guarda il tratto in cui la rampa SCENDE**, cioè né il primo decimo né l'ultimo
         * quinto: agli estremi la tinta è ferma sul suo massimo o è già finita, e là due pixel
         * uguali sono la cosa giusta invece che un difetto.
         */
        val da = mappa.height / 10
        val a = mappa.height * 4 / 5
        var miste = 0
        var toniMax = 0
        for (y in da until a) {
            val toni = HashSet<Color>()
            for (x in 0 until mappa.width) toni += mappa[x, y]
            if (toni.size > 1) miste++
            toniMax = maxOf(toniMax, toni.size)
        }
        val righe = a - da

        assertTrue(
            "Solo $miste righe su $righe hanno più di un tono: senza rumore le bande restano",
            miste >= righe * QUOTA_MISTE
        )
        assertTrue(
            "Una riga porta $toniMax toni diversi: il rumore è una grana, non un livello",
            toniMax <= TONI_MAX
        )
    }

    /**
     * **La tessera dei telefoni vecchi porta il rumore nell'opacità, e la rampa resta la sua.**
     *
     * ⚠️⚠️ **SI GUARDA LA TESSERA E NON I PIXEL RESI, E LA RAGIONE È MISURATA**: la prima stesura
     * di questa prova contava le righe miste nel **disegno**, ed è rimasta **verde con il rumore
     * azzerato**, cioè non misurava niente. Sul banco una riga porta due toni adiacenti anche
     * senza nessun rumore nostro: nel disegno ne entra uno di Skia, e i due non si distinguono.
     * Nell'opacità della tessera invece il rumore o c'è o non c'è.
     * ⚠️ **Le due asserzioni guardano i due versi dello stesso numero**, come nella prova qui
     * sopra: che il rumore ci sia, e che resti piccolo.
     */
    @Test
    fun `la tessera dei telefoni vecchi porta il rumore nell'opacità`() {
        val alta = 300
        val mappa = rampBitmap(PROVE_STOPS, PICCO, alta)
        assertTrue("La tessera non si è costruita", mappa != null)
        val quadro = mappa!!
        var miste = 0
        var largoMax = 0
        for (y in 0 until alta) {
            var meno = 255
            var piu = 0
            for (x in 0 until quadro.width) {
                /*
                 * ⚠️ **L'opacità si legge da `getPixel`, che su una ALPHA_8 dà il nero con quel
                 * canale**: la bitmap non ha colori, e il valore che interessa è il byte alto.
                 */
                val a = (quadro.getPixel(x, y) ushr 24) and 0xFF
                meno = minOf(meno, a)
                piu = maxOf(piu, a)
            }
            if (piu > meno) miste++
            largoMax = maxOf(largoMax, piu - meno)
        }
        assertTrue(
            "Solo $miste righe su $alta hanno più di un livello: la tessera non porta rumore",
            miste >= alta * QUOTA_MISTE
        )
        assertTrue(
            "Una riga si allarga di $largoMax livelli: è una grana, non un dither",
            largoMax <= LARGO_MAX
        )
        /*
         * ⚠️ **E la rampa resta quella dichiarata**: la media di una riga deve cadere sul valore
         * delle tappe, o il rumore starebbe coprendo una rampa sbagliata. In cima vale il picco,
         * in fondo zero.
         */
        var somma = 0
        for (x in 0 until quadro.width) somma += (quadro.getPixel(x, 0) ushr 24) and 0xFF
        val media = somma.toFloat() / quadro.width
        assertTrue(
            "In cima la tessera vale $media invece di ${PICCO * 255}",
            kotlin.math.abs(media - PICCO * 255f) <= LARGO_MAX
        )
    }

    /**
     * **La maschera dei telefoni vecchi si tinge del colore del paint.**
     *
     * ⚠️⚠️ **MISURA IL MODO IN CUI QUESTA STRADA PUÒ FALLIRE**: la maschera è un'immagine di sola
     * opacità, e chi la posa si aspetta che Skia la moduli col colore del paint. Se non lo
     * facesse, la fascia verrebbe **nera** su un telefono che nessuno dei due ha, e nessuno se ne
     * accorgerebbe fino a una segnalazione.
     * ⚠️⚠️ **PERCHÉ NON SI MONTA `frontWash` COM'È**: là il ramo lo sceglie la versione di Android,
     * e il banco gira su una piattaforma recente, quindi passerebbe sempre dallo shader. Montare
     * una seconda piattaforma finta costerebbe duecento megabyte a ogni corsa in CI. Le due righe
     * che posano la maschera sono ricopiate qui, e sono le uniche: la rampa, il rumore e il modo di
     * ancorarla vengono tutte da [rampMask].
     * ⚠️ **Che cosa NON vede**: se sul telefono la strada venga presa davvero, che è un `if` sulla
     * versione di sistema, e se il rimedio basti a far sparire le bande dai suoi occhi.
     */
    @Test
    fun `la maschera dei telefoni vecchi si tinge`() {
        var costruita = false
        banco.setContent {
            Box(modifier = Modifier.background(Color.White)) {
                Box(
                    modifier = Modifier
                        .size(LARGA.dp, ALTA.dp)
                        .testTag(FASCIA)
                        /*
                         * ⚠️ **La maschera si costruisce QUI, alla misura vera in pixel**: quanto
                         * valga un punto sul banco è una cosa della piattaforma finta, e una
                         * prova che la desse per uno misurerebbe una rampa spostata il giorno che
                         * cambia. Nell'app la costruisce `drawWithCache`, una volta per misura.
                         */
                        .drawBehind {
                            val maschera =
                                rampMask(PROVE_STOPS, PICCO, size.height.toInt(), 0f) ?: return@drawBehind
                            costruita = true
                            val pittura = Paint().apply {
                                color = TINTA
                                asFrameworkPaint().shader = maschera
                            }
                            drawIntoCanvas { tela ->
                                tela.drawRect(0f, 0f, size.width, size.height, pittura)
                            }
                        }
                )
            }
        }
        banco.waitForIdle()

        val mappa = banco.onNodeWithTag(FASCIA).captureToImage().toPixelMap()
        /*
         * ⚠️ **La spia si guarda DOPO la cattura, e non prima**: il disegno gira quando la scena
         * viene dipinta davvero, non quando la composizione è a riposo. Messa prima, la prova
         * falliva sempre con un messaggio che accusava la maschera invece dell'attesa, ed è
         * successo alla prima corsa.
         */
        assertTrue("La maschera non si è costruita", costruita)
        /*
         * ⚠️ **Il colore atteso si calcola invece di scriverlo**: in cima la rampa vale il suo
         * massimo, quindi sopra il bianco esce la tinta miscelata a [PICCO]. Un numero scritto a
         * mano andrebbe rifatto al primo ritocco del picco, e nel frattempo mentirebbe.
         */
        val alto = mappa[mappa.width / 2, 1]
        val atteso = { canale: Float, tinta: Float -> tinta * PICCO + canale * (1f - PICCO) }
        val scartoRosso = kotlin.math.abs(alto.red - atteso(1f, TINTA.red))
        val scartoBlu = kotlin.math.abs(alto.blue - atteso(1f, TINTA.blue))
        assertTrue(
            "In cima il colore è $alto: la maschera non si è tinta col colore del paint",
            scartoRosso < SCARTO && scartoBlu < SCARTO
        )

    }
}

/** L'etichetta con cui la prova ritrova il rettangolo del gradiente. */
private const val FASCIA = "fascia"

/** Quanto è grande la scena, in punti. */
private const val LARGA = 200
private const val ALTA = 300

/**
 * La tinta della prova.
 *
 * ⚠️ **Scura su fondo bianco di proposito**: più i due colori distano, più livelli attraversa la
 * rampa e più grossi sono i gradini che il rumore deve sciogliere. Con una tinta chiara la prova
 * misurerebbe il caso facile.
 */
private val TINTA = Color(0xFF1A3A6B)

/**
 * Quante righe devono essere miste perché il rumore ci sia davvero.
 *
 * ⚠️ **Non tutte, e la ragione è aritmetica**: dove il valore vero cade quasi esattamente su un
 * livello, il rumore di un livello può non bastare a spostare nessuno dei pixel di quella riga.
 * Sono poche righe, e pretenderle tutte renderebbe la prova rossa per un caso che va bene.
 */
private const val QUOTA_MISTE = 0.9f

/**
 * Quanti toni può portare al massimo una riga.
 *
 * ⚠️ **Otto e non tre**: il rumore vale un livello per canale, ma i tre canali arrotondano ognuno
 * per conto suo, quindi le combinazioni sono più delle tre che si conterebbero a mente.
 */
private const val TONI_MAX = 8

/**
 * Le tappe con cui si prova la maschera, e il suo picco.
 *
 * ⚠️ **Sono una rampa di prova e non quelle dell'intestazione**: `WASH_STOPS` è privata di
 * `Front.kt`, e quello che questa prova misura è il **meccanismo** della maschera, cioè che si
 * tinga e che porti rumore. Le tappe vere le misura la prova qui sopra, che monta `frontWash`.
 */
private val PROVE_STOPS = listOf(0f to 1f, 1f to 0f)
private const val PICCO = 0.25f

/**
 * Di quanti livelli può allargarsi una riga della tessera.
 *
 * ⚠️ **Quattro, cioè i due livelli del rumore presi ai due estremi**: la distribuzione è
 * triangolare, quindi la coda arriva a più e meno due, e una riga larga più di così vorrebbe dire
 * che qualcuno ha alzato la dose.
 */
private const val LARGO_MAX = 4

/**
 * Quanto può discostarsi il colore misurato da quello atteso.
 *
 * ⚠️ **Un paio di livelli su 255, cioè il rumore stesso più l'arrotondamento**: una soglia più
 * stretta renderebbe la prova rossa proprio per la cosa che deve esserci.
 */
private const val SCARTO = 0.02f

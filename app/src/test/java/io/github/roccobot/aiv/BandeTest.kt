package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

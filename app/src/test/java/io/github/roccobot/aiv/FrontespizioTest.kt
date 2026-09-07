package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Il banco di prova del **frontespizio di una cartella**, nato con lui nella `1.76`.
 *
 * ⚠️⚠️ **ESISTE PER LA METÀ PROATTIVA DELLA REGOLA, non per un difetto arrivato a lui**
 * (`CLAUDE.md`, § 'Quando si scrive una prova, e quando no'): un modificatore che **misura** e
 * ci posa dentro qualcosa è uno dei tre casi che la vogliono comunque, perché il codice può
 * essere valido e non fare niente. [FrontBand] è esattamente quello: misura il figlio
 * all'altezza piena, si dichiara alta quel che resta e ce lo posa in fondo.
 *
 * ⚠️ **Che cosa NON vede**: come la fascia si **percepisce** chiudendosi, l'opacità dell'icona e
 * la dissolvenza del titolo, che dipendono dalla resa vera. Vede che la griglia comincia più in
 * basso e che scorrendo la fascia si chiude, che sono misure di struttura.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [OmbraArchivio::class])
class FrontespizioTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **La griglia parte sotto il frontespizio, e scorrendo il frontespizio si chiude.**
     *
     * ⚠️ **Le due metà servono insieme**: la prima da sola passerebbe con una fascia inchiodata
     * (cioè con la griglia condannata a cominciare a un terzo di schermo per sempre), la seconda
     * da sola passerebbe con una fascia alta zero. La misura che le lega è che la prima
     * miniatura **risalga** di quasi tutta l'altezza della fascia.
     * ⚠️ **Si guarda la prima miniatura e non la fascia**: quello che l'utente ha chiesto è che
     * *la griglia parta più in basso*, e la fascia è il mezzo. Una prova sul mezzo passerebbe
     * anche se la griglia gli finisse sotto.
     */
    @Test
    fun `la griglia parte sotto il frontespizio e si chiude scorrendo`() {
        banco.setContent { Scena() }
        banco.waitForIdle()

        val alto = banco.onRoot().fetchSemanticsNode().size.height.toFloat()
        val prima = riquadro()
        assertTrue(
            "La prima miniatura comincia a ${prima}px su $alto: il frontespizio non la spinge giù",
            prima > alto * SOGLIA_APERTO
        )

        /*
         * ⚠️⚠️ **IL TRASCINAMENTO HA COORDINATE ESPLICITE, e `swipeUp()` nudo NON FUNZIONA**:
         * quello parte dal bordo di sotto del nodo, che qui è il margine della schermata, e da
         * là non c'è nessuna griglia sotto il dito. Misurato: con `swipeUp()` la prima miniatura
         * non si muoveva di un pixel.
         * ⚠️ **Lungo quanto il frontespizio e non di più**: così [frontScroll] lo spende tutto per
         * chiudere la fascia e la griglia non ha bisogno di scorrere, che è il fatto scritto là.
         */
        val largo = banco.onRoot().fetchSemanticsNode().size.width.toFloat()
        banco.onRoot().performTouchInput {
            swipe(
                start = Offset(largo / 2f, alto * DA),
                end = Offset(largo / 2f, alto * (DA - HEADER_SHARE)),
                durationMillis = LENTO
            )
        }
        banco.waitForIdle()

        /*
         * ⚠️ **`null` conta come chiuso**: l'inerzia del trascinamento può portare la prima
         * miniatura fuori dallo schermo, e una griglia che ha scorso di più di così ha per forza
         * chiuso la fascia prima, perché il frontespizio si spende sempre per primo.
         */
        val dopo = miniature()[1]?.top
        assertTrue(
            "La prima miniatura è a ${dopo}px e prima era a $prima: il frontespizio non si chiude",
            dopo == null || prima - dopo > alto * HEADER_SHARE * QUASI_TUTTO
        )
    }

    /**
     * **La sfumatura in fondo non ruba il tocco a quello che le sta sotto.**
     *
     * ⚠️⚠️ **È IL PRIMO DEI TRE CASI CHE VOGLIONO UNA PROVA**: un nodo che copre una parte di
     * schermata. [GroundFade] non ha nessun modificatore di puntatore, quindi la prova del tocco
     * non lo guarda nemmeno, ⚠️ **ma il fatto non è verificabile leggendo il codice**: la `1.70`
     * ha bloccato l'app intera con un nodo che sembrava innocuo, e la lezione scritta là è che
     * di un nodo che copre si misura se il tocco passa.
     * ⚠️ **Il tocco si dà per coordinate e non sul nodo**: `performClick` su una miniatura
     * arriverebbe a lei per costruzione, cioè misurerebbe un'altra cosa. Un dito su un punto
     * dello schermo passa dalla stessa prova del tocco dell'app vera.
     */
    @Test
    fun `la sfumatura in fondo non ruba il tocco`() {
        var aperta: Int? = null
        banco.setContent { Scena(onOpen = { aperta = it }) }
        banco.waitForIdle()

        /*
         * ⚠️ **Si tocca la miniatura più in basso fra quelle in scena, e non un punto scelto a
         * occhio**: così il punto è per costruzione dentro la fascia dipinta e per costruzione
         * sopra un'immagine. Un punto fisso in frazione di schermo cadrebbe fuori dalla griglia
         * su un banco con una densità diversa.
         */
        val ultima = miniature().maxByOrNull { it.value.bottom }
        val quale = requireNotNull(ultima) { "Nessuna miniatura in scena" }
        val alto = banco.onRoot().fetchSemanticsNode().size.height
        val fascia = with(banco.density) { GRADIENT_REACH.toPx() }
        assertTrue(
            "La miniatura più in basso sta a ${quale.value.center.y} su $alto: fuori dalla " +
                "sfumatura, quindi questa prova non guarderebbe niente",
            quale.value.center.y > alto - fascia
        )

        banco.onRoot().performTouchInput { click(quale.value.center) }
        banco.waitForIdle()

        assertTrue("Il tocco dentro la sfumatura non è arrivato all'immagine", aperta != null)
    }

    /**
     * **Cominciare una selezione non chiude il frontespizio.**
     *
     * ⚠️⚠️ **QUESTO DIFETTO È ARRIVATO A LUI, e la prova torna con la correzione, nella stessa
     * versione** (`CLAUDE.md`, § '🧪 Quando si scrive una prova, e quando no'). La `1.76`
     * chiudeva la fascia appena la selezione cominciava, e la sua segnalazione dice il danno
     * meglio di qualunque riformulazione: *appena si tocca a lungo per iniziare a selezionare, lo
     * spostamento delle miniature in alto fa già selezionare più elementi a causa dello
     * spostamento repentino mentre si tiene premuto*.
     * ⚠️ **Si misura la prima miniatura e non la fascia**, per la stessa ragione dell'altra prova:
     * quello che faceva danno era il movimento **delle miniature** sotto un dito appoggiato.
     * ⚠️ **La prima asserzione non è un contorno**: senza di lei, un tocco lungo che non
     * cominciasse nessuna selezione passerebbe la seconda a mani vuote, cioè la prova direbbe di
     * sì senza aver provato niente.
     */
    @Test
    fun `la selezione non chiude il frontespizio`() {
        banco.setContent { Scena() }
        banco.waitForIdle()

        val prima = riquadro()
        val bersaglio = requireNotNull(miniature()[1]) { "Nessuna miniatura da tenere premuta" }
        banco.onRoot().performTouchInput { longClick(bersaglio.center) }
        banco.waitForIdle()

        val uno = app.resources.getQuantityString(R.plurals.pick_count, 1, 1)
        assertTrue(
            "Il tocco lungo non ha cominciato nessuna selezione: il conto '$uno' non c'è",
            banco.onAllNodesWithText(uno).fetchSemanticsNodes().isNotEmpty()
        )

        assertEquals(
            "La prima miniatura si è spostata cominciando la selezione: il frontespizio si chiude",
            prima,
            riquadro(),
            FERMO
        )
    }

    /**
     * **Il conto sta sotto il titolo, in testata e nella fascia.**
     *
     * ⚠️⚠️ **È LA SUA SPECIFICA ALLA LETTERA**: *il numero di elementi (non immagini) totali /
     * selezionati dev'essere indicato sotto il titolo*. Prima della `1.78` il conto viveva **al
     * posto** del titolo in testata, e con la fascia aperta non aveva posto affatto.
     * ⚠️⚠️ **LE DUE COPIE SI ACCOPPIANO PER POSIZIONE, e non si guarda la sola presenza**: il
     * nome e il conto stanno due volte nell'albero (la fascia e la testata si dissolvono l'una
     * nell'altra), quindi una prova che cercasse solo il testo passerebbe anche con il conto
     * messo **sopra** il titolo, che è l'errore che questa prova esiste per prendere.
     * ⚠️ **Dice 'elementi' e non 'immagini'**, che è l'altra metà della sua richiesta (*non va
     * più bene da quando ci sono anche i video*): la stringa si chiede alle risorse, quindi la
     * prova non ricopia il testo e vale in tutte le lingue.
     */
    @Test
    fun `il conto sta sotto il titolo`() {
        banco.setContent { Scena() }
        banco.waitForIdle()

        val quanti = app.resources.getQuantityString(R.plurals.items_count, FOTO.size, FOTO.size)
        val titoli = banco.onAllNodesWithText(TITOLO).fetchSemanticsNodes()
            .map { it.boundsInRoot }.sortedBy { it.top }
        val conti = banco.onAllNodesWithText(quanti).fetchSemanticsNodes()
            .map { it.boundsInRoot }.sortedBy { it.top }

        assertTrue("Il nome della cartella non è in scena", titoli.isNotEmpty())
        assertEquals(
            "Il conto non compare tante volte quante il nome: una delle due copie non ce l'ha",
            titoli.size,
            conti.size
        )
        for (quale in titoli.indices) {
            assertTrue(
                "Il conto sta a ${conti[quale].top}px e il nome finisce a ${titoli[quale].bottom}px",
                conti[quale].top >= titoli[quale].bottom
            )
        }
    }

    /**
     * **L'icona comincia a sbiadire nell'istante in cui comincia a stringersi, e arrivano a zero
     * insieme.**
     *
     * ⚠️⚠️ **È LA SUA NOTA ALLA LETTERA** (giro della `1.79`, voce `front-icona`: *l'icona
     * cartella deve iniziare la sua dissolvenza appena inizia a ridursi di dimensione, e
     * arrivare alla dimensione minima e a opacità 0 contemporaneamente*), e la `1.78` la
     * mancava con un `0.35f` scritto a mano: fra il punto in cui l'icona cominciava a stringersi
     * e quello in cui cominciava a sbiadire c'era un tratto a inchiostro pieno.
     * ⚠️⚠️ **LA SOGLIA SI VERIFICA PER INVERSIONE E NON RICOPIANDO LA FORMULA**: [frontIconFade]
     * deve rispondere l'apertura alla quale lo spazio concesso all'icona è **esattamente** il suo
     * lato massimo, cioè il punto in cui [Modifier.frontIconMeasure] passa da `max` a `libero *
     * FRONT_ICON_SHARE`. Ricopiare il conto qui vorrebbe dire una prova che segue qualunque
     * modifica invece di misurarla.
     * ⚠️ **Il caso dell'icona che non ci sta mai è l'altro bordo**: con un lato massimo più grande
     * di quanto la fascia possa concedere, la soglia è 1, cioè l'icona si stringe e sbiadisce dal
     * primo pixel di scorrimento. Senza questa riga, una soglia che sfora sopra 1 passerebbe.
     */
    @Test
    fun `l'icona sbiadisce mentre si stringe e arrivano a zero insieme`() {
        val fascia = 900f
        val lato = 120f
        val soglia = frontIconFade(fascia, lato)

        assertEquals(
            "Alla soglia lo spazio concesso non è il lato massimo: la dissolvenza non parte" +
                " dove parte il rimpicciolimento",
            lato,
            soglia * fascia * FRONT_ICON_SHARE,
            MEZZO_PIXEL
        )
        assertEquals(
            "Alla soglia l'inchiostro non è ancora pieno: la dissolvenza parte troppo presto",
            FRONT_INK,
            frontIconInk(soglia, soglia),
            NIENTE
        )
        assertEquals(
            "A fascia chiusa l'icona ha ancora inchiostro",
            0f,
            frontIconInk(0f, soglia),
            NIENTE
        )
        assertTrue(
            "Sopra la soglia l'inchiostro non è pieno",
            frontIconInk(1f, soglia) == FRONT_INK
        )

        var prima = frontIconInk(0f, soglia)
        for (passo in 1..100) {
            val aperto = passo / 100f
            val adesso = frontIconInk(aperto, soglia)
            assertTrue(
                "A $aperto di apertura l'inchiostro cala invece di crescere",
                adesso >= prima
            )
            assertTrue(
                "A $aperto di apertura l'icona si vede ancora ma è già trasparente",
                adesso > 0f
            )
            prima = adesso
        }

        val stretta = frontIconFade(fascia, fascia)
        assertEquals(
            "Un'icona più grande dello spazio che la fascia concede non parte da 1",
            1f,
            stretta,
            NIENTE
        )
    }

    /**
     * I riquadri delle miniature che stanno nell'albero semantico, per posizione.
     *
     * ⚠️ **Si chiedono per NOME e una per una**: la descrizione parlata di una miniatura dice
     * 'Item N of M', quindi la si ricompone dalla stessa risorsa che la griglia usa invece di
     * scriverla qui. Cercarne una sottostringa a caso funzionerebbe in inglese e in nessun'altra
     * lingua.
     */
    private fun miniature(): Map<Int, Rect> {
        val contesto = ApplicationProvider.getApplicationContext<Context>()
        return (1..FOTO.size).mapNotNull { quale ->
            val detto = contesto.getString(R.string.grid_item, quale, FOTO.size)
            banco.onAllNodesWithContentDescription(detto)
                .fetchSemanticsNodes()
                .firstOrNull()
                ?.let { quale to it.boundsInRoot }
        }.toMap()
    }

    /** Dove comincia la prima miniatura, in pixel dal bordo di sopra. */
    private fun riquadro() = requireNotNull(miniature()[1]) {
        "La prima miniatura non è nell'albero semantico: la griglia non si è composta"
    }.top

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    @Composable
    private fun Scena(onOpen: (Int) -> Unit = {}) {
        AivTheme(darkTheme = false) {
            Box(modifier = Modifier.fillMaxSize()) {
                GridScreen(
                    title = TITOLO,
                    items = FOTO,
                    highlight = null,
                    onOpen = onOpen,
                    onBack = {},
                    onChanged = {},
                    onSearch = {}
                )
            }
        }
    }
}

/**
 * Le immagini della cartella finta: abbastanza da riempire lo schermo e avanzare.
 *
 * ⚠️ **Servono davvero tante**: con poche, chiudere il frontespizio non lascerebbe niente da
 * scorrere e la seconda metà della prima prova misurerebbe una griglia che non si muove. ⚠️ Il
 * fatto che [frontScroll] chiuda la fascia **anche** senza niente da scorrere è vero e sta
 * scritto là, ma qui serve anche il tratto dopo.
 */
private val FOTO = (1..40).map { Uri.parse("file:///finta/$it.jpg") }

/** Il nome della cartella finta, che il frontespizio scrive e la testata ripete. */
private const val TITOLO = "Cartella di prova"

/**
 * Quanto può muoversi una miniatura e continuare a dirsi ferma, in pixel.
 *
 * ⚠️ **Un pixel e non zero**: la posizione arriva da una misura in virgola mobile, e pretendere
 * l'uguaglianza esatta farebbe fallire la prova per un arrotondamento. Quello che deve fallire è
 * una fascia che si chiude, cioè un salto di un terzo di schermo.
 */
private const val FERMO = 1f

/**
 * Quanto in basso deve cominciare la prima miniatura, in frazione di schermo.
 *
 * ⚠️ **Un quarto e non [HEADER_SHARE]**: sopra la fascia c'è la testata, e sotto la fascia i
 * margini della schermata, quindi la prima miniatura sta più in basso di così. Il numero è una
 * soglia larga di proposito: quello che deve fallire è una fascia che non c'è, e per misurare
 * quanto sia alta serve il confronto della seconda metà della prova.
 */
private const val SOGLIA_APERTO = 0.25f

/**
 * Quanta parte della fascia deve risalire perché la si chiami chiusa.
 *
 * ⚠️ **Non tutta**: uno scorrimento simulato spende quello che ha, e fra la fine del gesto e
 * l'inerzia il numero esatto dipende dalla velocità che il banco produce. Nove decimi separano
 * una fascia chiusa da una che non si è mossa, che è la distinzione che questa prova cerca.
 */
private const val QUASI_TUTTO = 0.9f

/**
 * Da che altezza parte il trascinamento, in frazione di schermo.
 *
 * ⚠️ **Sette decimi e non il bordo**: il dito deve partire **sopra** la griglia, e in fondo allo
 * schermo ci sono il margine della schermata e i rientri di sistema.
 */
private const val DA = 0.7f

/**
 * Quanto scarto si perdona a una misura in pixel: **mezzo**.
 *
 * ⚠️ **Non zero**: la soglia si ricava da una divisione in virgola mobile e si rimoltiplica per
 * tornare al lato, quindi l'uguaglianza esatta fallirebbe per l'ultimo bit. Mezzo pixel è meno
 * di quello che uno schermo può mostrare, cioè una tolleranza che non nasconde niente.
 */
private const val MEZZO_PIXEL = 0.5f

/**
 * Quanto scarto si perdona a una frazione: **niente**.
 *
 * ⚠️ **Zero di proposito**: qui i due estremi sono valori scritti, non misure, e devono cadere
 * esattamente sul loro numero. Una tolleranza qui vorrebbe dire una dissolvenza che parte 'quasi'
 * dove deve, che è precisamente il difetto del giro della `1.79`.
 */
private const val NIENTE = 0f

/**
 * Quanto dura il trascinamento simulato.
 *
 * ⚠️ **Lungo di proposito**: la velocità che il banco ricava dagli ultimi campioni diventa
 * inerzia, e un gesto veloce porterebbe la griglia molto oltre la chiusura della fascia, cioè
 * misurerebbe l'inerzia invece del frontespizio.
 */
private const val LENTO = 400L


package io.github.roccobot.aiv

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Il banco di prova del **intestazione di una cartella**, nato con lui nella `1.76`.
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
/*
 * ⚠️⚠️ **LA GRAFICA VERA SERVE A UNA PROVA SOLA, MA VALE PER LA CLASSE**: `captureToImage` senza
 * `NATIVE` restituisce un'immagine vuota, cioè una prova che passa sempre. Robolectric lo dichiara
 * per classe o per metodo, e per classe costa poco: le altre prove qui non guardano i pixel, e
 * girare in grafica vera non le cambia.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IntestazioneTest {

    @get:Rule
    val banco = createComposeRule()

    /**
     * **La griglia parte sotto l'intestazione, e scorrendo l'intestazione si chiude.**
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
    fun `la griglia parte sotto l'intestazione e si chiude scorrendo`() {
        banco.setContent { Scena() }
        banco.waitForIdle()

        val alto = banco.onRoot().fetchSemanticsNode().size.height.toFloat()
        val prima = riquadro()
        assertTrue(
            "La prima miniatura comincia a ${prima}px su $alto: l'intestazione non la spinge giù",
            prima > alto * SOGLIA_APERTO
        )

        /*
         * ⚠️⚠️ **IL TRASCINAMENTO HA COORDINATE ESPLICITE, e `swipeUp()` nudo NON FUNZIONA**:
         * quello parte dal bordo di sotto del nodo, che qui è il margine della schermata, e da
         * là non c'è nessuna griglia sotto il dito. Misurato: con `swipeUp()` la prima miniatura
         * non si muoveva di un pixel.
         * ⚠️ **Lungo quanto l'intestazione e non di più**: così [frontScroll] lo spende tutto per
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
         * chiuso la fascia prima, perché l'intestazione si spende sempre per prima.
         */
        val dopo = miniature()[1]?.top
        assertTrue(
            "La prima miniatura è a ${dopo}px e prima era a $prima: l'intestazione non si chiude",
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
     * **Cominciare una selezione non chiude l'intestazione.**
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
    fun `la selezione non chiude l'intestazione`() {
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
            "La prima miniatura si è spostata cominciando la selezione: l'intestazione si chiude",
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
     * **Chiudendo la fascia il nome arriva in testata, e il gradiente non se lo mangia.**
     *
     * ⚠️⚠️ **QUESTO DIFETTO È ARRIVATO A LUI E HA FATTO BOCCIARE LA `1.83`** (voce `front-dieci`:
     * *il nome della cartella e gli elementi (selezionati o meno) non passano più in testa allo
     * scorrimento (lo spazio rimane vuoto)*), quindi la prova torna con la correzione, nella
     * stessa versione. La causa era che il gradiente si dipingeva su un nodo che la colonna
     * disegnava **dopo** la testata: sconfinando verso l'alto le finiva sopra.
     *
     * ⚠️⚠️ **MA QUESTA MISURA IL SINTOMO E NON LA CAUSA, e va detto invece di lasciarlo credere**:
     * a rimettere il difetto e vederla fallire non ci riesce, ed è provato (vedi la prova sul
     * meccanismo qui sotto, che invece lo prende). Quello che presidia è **la scena come lui la
     * vede**: a fascia chiusa il nome in testata si legge, qualunque sia la ragione per cui non è
     * coperto. Fra le ragioni c'è anche che il gradiente sbiadisce con lo scorrimento, che è
     * l'altra metà della sua richiesta, quindi togliendo quella questa prova diventa rossa.
     *
     * ⚠️⚠️ **SI GUARDANO I PIXEL, ED È LA PRIMA VOLTA IN QUESTO BANCO**: nessuna misura di
     * struttura poteva vedere il difetto, perché il titolo c'era, era al posto giusto, era opaco
     * e aveva le sue dimensioni. Quello che non si vedeva era il **disegno**, e per guardarlo
     * serve la grafica vera (vedi `@GraphicsMode` sulla classe).
     * ⚠️⚠️ **SI CONTANO I COLORI RIGA PER RIGA E NON SUL RIQUADRO INTERO, ed è la differenza fra
     * misurare e fingere**: il gradiente è **verticale**, quindi su un riquadro intero i colori
     * distinti sarebbero tanti anche con il titolo completamente coperto, una tinta per riga. In
     * una singola riga orizzontale il gradiente ha un colore solo, quindi una riga con molti
     * colori è una riga in cui c'è scritto qualcosa.
     * ⚠️ **La tinta è quella scelta a mano e non quella dell'app**: così il fondo è un colore
     * noto e pieno, cioè il caso peggiore per la leggibilità del titolo.
     */
    @Test
    fun `il nome arriva in testata e il gradiente non lo copre`() {
        banco.setContent { Scena(tinta = TINTA_SCURA) }
        banco.waitForIdle()

        val alto = banco.onRoot().fetchSemanticsNode().size.height.toFloat()
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
         * ⚠️⚠️ **IL NODO SI SCEGLIE FRA QUELLI DENTRO LO SCHERMO, e la prima stesura di questa
         * prova sbagliava proprio qui**: il nome sta due volte nell'albero, e a fascia chiusa la
         * copia della fascia è **sopra il bordo di sopra** (la fascia la posa in negativo e la
         * ritaglia). `boundsInRoot` non ritaglia niente, quindi 'il più in alto' era quella, e la
         * prova finiva a contare i colori di zero pixel: 0 colori è 'non ho guardato', non 'non
         * si legge'.
         */
        val dentro = banco.onAllNodesWithText(TITOLO).fetchSemanticsNodes()
            .map { it.boundsInRoot }
            .filter { it.top >= 0f && it.bottom <= alto && it.height > 0f }
        val dove = requireNotNull(dentro.minByOrNull { it.top }) {
            "Il nome della cartella non è in scena dentro lo schermo"
        }

        val mappa = banco.onRoot().captureToImage().toPixelMap()
        var piuColori = 0
        for (y in dove.top.toInt().coerceAtLeast(0) until dove.bottom.toInt()
            .coerceAtMost(mappa.height)) {
            val riga = mutableSetOf<Long>()
            for (x in dove.left.toInt().coerceAtLeast(0) until dove.right.toInt()
                .coerceAtMost(mappa.width)) {
                riga.add(mappa[x, y].value.toLong())
            }
            piuColori = maxOf(piuColori, riga.size)
        }

        assertTrue(
            "Nella riga più ricca del titolo in testata ci sono $piuColori colori: il nome non " +
                "si legge, il gradiente gli è finito sopra",
            piuColori > COLORI_DI_UN_TESTO
        )
    }

    /**
     * **Il gradiente si dipinge DIETRO il contenuto del suo nodo.**
     *
     * ⚠️⚠️ **QUESTA È LA PROVA CHE FALLISCE COL DIFETTO RIMESSO, e l'altra da sola non
     * bastava**: misurando il titolo in testata a schermata intera, il difetto rimesso a mano
     * **non** faceva fallire niente, perché là dove passa il titolo il gradiente è già quasi
     * finito e il testo si legge lo stesso.
     * Provato, non supposto: con `onDrawWithContent` al posto di `onDrawBehind`, e anche con
     * l'opacità inchiodata al pieno come nella `1.83`, quella prova restava verde. Una prova che
     * non distingue il difetto dalla correzione non misura niente.
     * ⚠️ **Quindi si misura il meccanismo su una scena minima**: un quadrato bianco pieno dentro
     * un nodo che porta il gradiente. Dietro, il bianco resta bianco; sopra, il centro si tinge.
     * ⚠️ **Il colore è rosso e l'opacità è il pieno**: servono il caso più visibile possibile, o
     * la differenza fra le due vie sarebbe una sfumatura da soglia.
     */
    @Test
    fun `il gradiente si dipinge dietro il contenuto`() {
        banco.setContent {
            Box(
                modifier = Modifier
                    .size(LATO)
                    .frontWash(tint = Color.Red, air = 0.dp, up = 0.dp, bar = 0.dp, ink = { 1f })
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White))
            }
        }
        banco.waitForIdle()

        val mappa = banco.onRoot().captureToImage().toPixelMap()
        val centro = mappa[mappa.width / 2, mappa.height / 2]
        assertEquals(
            "Il centro del quadrato bianco è $centro: il gradiente gli è finito sopra",
            Color.White,
            centro
        )
    }

    /**
     * **Sopra la sfumatura c'è una fascia piatta del suo colore di partenza.**
     *
     * ⚠️⚠️ **È LA SUA RICHIESTA ALLA LETTERA** (2026-09-08, con schermata: *puoi colorare la barra
     * di sistema di Android dello stesso colore dell'inizio della sfumatura? ... Stesso colore
     * della prima striscia di pixel sul bordo*), e il pezzo che la disegna è [Modifier.frontWash].
     * ⚠️⚠️ **MISURA LA PIATTEZZA E NON UN COLORE**: quanto valga quel colore composto su bianco
     * dipende da come il canvas fonde i canali, e scriverlo qui vorrebbe dire ricopiare
     * un'implementazione. Quello che la richiesta chiede è che sopra il gradiente ci sia un tratto
     * **costante**, e sotto il gradiente cominci a scendere: sono due fatti, e questa li guarda.
     * ⚠️ **Il tratto si cerca invece di calcolarlo**: la sua altezza in pixel dipende dalla densità
     * della scena finta, e ricavarla qui vorrebbe dire rifare il conto che sto verificando.
     */
    @Test
    fun `la fascia sopra la sfumatura è piatta`() {
        banco.setContent {
            Box(modifier = Modifier.size(LATO).background(Color.White)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(META)
                        .frontWash(
                            tint = Color.Red,
                            air = 0.dp,
                            up = 0.dp,
                            bar = BARRA,
                            ink = { 1f }
                        )
                )
            }
        }
        banco.waitForIdle()

        val mappa = banco.onRoot().captureToImage().toPixelMap()
        val x = mappa.width / 2
        val cima = (0 until mappa.height).firstOrNull { mappa[x, it] != Color.White }
        val primo = requireNotNull(cima) { "Nessun pixel colorato: la tinta non si è dipinta" }
        val colore = mappa[x, primo]
        var giu = primo
        while (giu + 1 < mappa.height && mappa[x, giu + 1] == colore) giu++

        assertTrue(
            "Il tratto costante in cima è alto ${giu - primo + 1} pixel: senza la fascia " +
                "resterebbero i pochi in cui il gradiente arrotonda uguale",
            giu - primo + 1 >= FASCIA_MINIMA
        )
        assertTrue(
            "Sotto la fascia il colore non cambia: il gradiente non sta scendendo",
            mappa[x, mappa.height - 1] != colore
        )
    }

    /**
     * **La pastiglia dice 'Deseleziona' dopo il primo tocco, e un secondo tocco scarta.**
     *
     * ⚠️⚠️ **È LA SUA RICHIESTA ALLA LETTERA** (voce `front-dieci`: *dopo il tocco su 'Seleziona
     * tutto' il tasto deve cambiare testo in 'Deseleziona', con le logiche anti-jitter; un
     * secondo tocco scarta la selezione*), ed è di struttura, quindi il banco la vede tutta.
     * ⚠️⚠️ **LA LARGHEZZA SI MISURA PRIMA E DOPO, e senza quella metà la prova non guarderebbe
     * l'anti-jitter**: due parole di lunghezza diversa in una pastiglia che si adatta la farebbero
     * ballare, che è esattamente quello che lui non vuole.
     * ⚠️ **Il gesto passa per il TESTO e non per la posizione**: la pastiglia si sposta se il
     * titolo va a capo, e un tocco per coordinate misurerebbe dov'era invece di che cosa fa.
     */
    @Test
    fun `la pastiglia scarta la selezione e non cambia larghezza`() {
        banco.setContent { Scena() }
        banco.waitForIdle()

        val prendi = app.getString(R.string.pick_all)
        val scarta = app.getString(R.string.front_unpick)
        val primo = banco.onAllNodesWithText(prendi).fetchSemanticsNodes()
            .firstOrNull()?.boundsInRoot
        val largo = requireNotNull(primo) { "La pastiglia 'Seleziona tutto' non è in scena" }.width

        banco.onNodeWithText(prendi).performClick()
        banco.waitForIdle()

        val tutte = app.resources.getQuantityString(R.plurals.pick_count, FOTO.size, FOTO.size)
        assertTrue(
            "Il tocco non ha selezionato niente: il conto '$tutte' non c'è",
            banco.onAllNodesWithText(tutte).fetchSemanticsNodes().isNotEmpty()
        )

        val dopo = banco.onAllNodesWithText(scarta).fetchSemanticsNodes().firstOrNull()
        val adesso = requireNotNull(dopo) { "La pastiglia non dice '$scarta'" }.boundsInRoot
        assertEquals(
            "La pastiglia era larga $largo e adesso è ${adesso.width}: balla al cambio di parola",
            largo,
            adesso.width,
            FERMO
        )

        banco.onNodeWithText(scarta).performClick()
        banco.waitForIdle()

        assertTrue(
            "Il secondo tocco non ha scartato la selezione",
            banco.onAllNodesWithText(tutte).fetchSemanticsNodes().isEmpty()
        )
    }

    /**
     * **Il tocco sul nome copia il nome, e il tocco lungo copia il percorso.**
     *
     * ⚠️ **Sono due gesti suoi del giro della `1.83`** (*un tap sul nome della cartella copia il
     * suo nome (con notifica toast); un tap lungo copia il nome della cartella con il percorso*),
     * e sono di struttura: quello che finisce negli appunti si legge.
     * ⚠️⚠️ **SENZA PERCORSO IL TOCCO LUNGO COPIA IL NOME, e la prova lo misura invece di
     * saltarlo**: qui la cartella finta non ha nessun percorso, che è anche il caso vero di una
     * cartella appena aperta, e un gesto che in quel caso non facesse niente si leggerebbe come
     * rotto.
     */
    @Test
    fun `il tocco sul nome lo copia`() {
        banco.setContent { Scena() }
        banco.waitForIdle()

        val appunti = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        /*
         * ⚠️⚠️ **I GESTI VIVONO SUL NOME GRANDE DELL'INTESTAZIONE, NON SU QUELLO DELLA TESTATA**,
         * ed è una scelta dichiarata: la copia della testata è trasparente finché la fascia è
         * aperta, e un nodo trasparente riceve comunque i tocchi, quindi mettere i gesti anche là
         * vorrebbe dire un tocco sul vuoto che copia un nome. Il nome grande è quello più in
         * BASSO fra i due, e si tocca per coordinate perché è di quel disegno che si parla.
         */
        val grande = banco.onAllNodesWithText(TITOLO).fetchSemanticsNodes()
            .map { it.boundsInRoot }
            .maxByOrNull { it.top }
        val dove = requireNotNull(grande) { "Il nome della cartella non è in scena" }
        banco.onRoot().performTouchInput { click(dove.center) }
        banco.waitForIdle()

        assertEquals(
            "Il tocco sul nome non l'ha copiato",
            TITOLO,
            appunti.primaryClip?.getItemAt(0)?.text?.toString()
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
    private fun Scena(onOpen: (Int) -> Unit = {}, tinta: Int? = null) {
        AivTheme(darkTheme = false) {
            Box(modifier = Modifier.fillMaxSize()) {
                GridScreen(
                    title = TITOLO,
                    items = FOTO,
                    highlight = null,
                    onOpen = onOpen,
                    onBack = {},
                    onChanged = {},
                    onSearch = {},
                    frontTint = tinta
                )
            }
        }
    }
}

/**
 * Le immagini della cartella finta: abbastanza da riempire lo schermo e avanzare.
 *
 * ⚠️ **Servono davvero tante**: con poche, chiudere l'intestazione non lascerebbe niente da
 * scorrere e la seconda metà della prima prova misurerebbe una griglia che non si muove. ⚠️ Il
 * fatto che [frontScroll] chiuda la fascia **anche** senza niente da scorrere è vero e sta
 * scritto là, ma qui serve anche il tratto dopo.
 */
private val FOTO = (1..40).map { Uri.parse("file:///finta/$it.jpg") }

/** Il nome della cartella finta, che l'intestazione scrive e la testata ripete. */
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
 * misurerebbe l'inerzia invece dell'intestazione.
 */
private const val LENTO = 400L

/**
 * Quale delle sedici tinte usa la prova dei pixel: la prima, che è la più scura.
 *
 * ⚠️ **Una scelta a mano e non quella dell'app**: il caso peggiore per la leggibilità del titolo è
 * un fondo pieno e scuro, e con la tinta dell'app il colore dipenderebbe dal tema.
 */
private const val TINTA_SCURA = 0

/**
 * Quanti colori deve avere almeno una riga perché ci sia scritto qualcosa.
 *
 * ⚠️ **Tre e non due**: un testo antialiasato ne porta decine, mentre un fondo pieno ne ha uno e
 * un bordo di raccordo può portarne due. La soglia separa 'c'è una parola' da 'c'è una sfumatura'.
 */
private const val COLORI_DI_UN_TESTO = 3

/** Quanto è largo il quadrato della prova sul meccanismo del gradiente. */
private val LATO = 100.dp

/** Quanto è alto il nodo che porta la tinta, dentro la scena della fascia: la metà di sotto. */
private val META = 50.dp

/**
 * Quanto è alta la barra di sistema finta, nella prova della fascia.
 *
 * ⚠️ **Un numero qualunque ma grande abbastanza**: serve solo a separare la fascia dai pochi
 * pixel in cui il gradiente arrotonda allo stesso colore.
 */
private val BARRA = 24.dp

/**
 * Quanti pixel costanti in cima valgono 'la fascia c'è'.
 *
 * ⚠️ **Sotto l'altezza della barra e sopra la coda del gradiente**: la densità della scena finta
 * non è dichiarata, quindi [BARRA] in pixel non si sa; quello che si sa è che senza fascia il
 * tratto costante è di pochissimi pixel, perché la tinta comincia a scendere subito.
 */
private const val FASCIA_MINIMA = 12


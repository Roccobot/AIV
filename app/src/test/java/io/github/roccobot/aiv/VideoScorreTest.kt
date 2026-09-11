package io.github.roccobot.aiv

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Il banco di prova della **strisciata sopra un filmato**, che dalla `2.08` trascina la pagina
 * invece di contare i pixel e basta.
 *
 * ⚠️⚠️ **NASCE CON LA CORREZIONE DI UNA VOCE NON APPROVATA** (giro della `2.07`, `video-scorre`:
 * *allo scorrimento del dito non vedo comunque scorrere il video dentro o fuori la schermata con
 * animazione slide-in/slide-out*). Quello che la riscrittura poteva rompere in silenzio è il
 * **legame nuovo**: fino alla `2.07` il passo si chiedeva alla fine del gesto, adesso alla fine
 * dell'assestamento, e un errore là toglierebbe del tutto il cambio pagina sui filmati senza che
 * niente vada in errore.
 *
 * ⚠️⚠️ **E IL CAPOLINEA È UNA CORREZIONE A SÉ, che il riscontro non nominava**: fino alla `2.07`
 * una strisciata oltre la soglia chiedeva il passo **anche dove non c'era niente da aprire**,
 * perché il gesto guardava solo la soglia. Adesso il passo si chiede dove `toward` ha risposto,
 * che è la stessa regola di `ImageCanvas`.
 *
 * ⚠️ **Che cosa NON vede**: che il filmato si veda scorrere. Il banco non disegna nessun video e
 * il caricatore di miniature non risponde su una macchina senza telefono, quindi la traslazione
 * vera e la vicina che entra dal bordo si guardano sul telefono. Qui si misura che il gesto
 * arrivi dove deve arrivare.
 * ⚠️ **Controprovata rimettendo il difetto della `2.07`** (il passo chiesto in `onDragEnd`
 * invece che dentro `settle`): rosso il caso del capolinea, verdi gli altri tre, che è
 * esattamente quello che quella riga cambia.
 */
@RunWith(AndroidJUnit4::class)
class VideoScorreTest {

    @get:Rule
    val banco = createComposeRule()

    /** Tre filmati finti, col secondo aperto: di qua e di là c'è dove andare. */
    private val mezzo = Folder.Series(
        items = (1..3).map { Uri.parse("file:///finta/$it.mp4") },
        index = 1
    )

    /** Gli stessi tre con l'ultimo aperto: avanti non c'è niente. */
    private val fondo = Folder.Series(items = mezzo.items, index = 2)

    /**
     * **La strisciata avanti chiede il passo, e lo chiede dopo l'assestamento.**
     *
     * ⚠️ Con il clock automatico `waitForIdle` porta a termine l'animazione della pagina: se il
     * passo restasse appeso dentro `settle`, qui arriverebbe `0`.
     */
    @Test
    fun `la strisciata avanti chiede il passo`() {
        var passo = 0
        monta(mezzo) { passo = it }

        trascina(avanti = true)
        assertEquals("A dito ancora giù non si deve chiedere niente", 0, passo)

        alza()

        assertEquals("La strisciata verso sinistra deve chiedere il filmato dopo", 1, passo)
    }

    /** **E quella indietro chiede il passo opposto**, con la stessa taratura. */
    @Test
    fun `la strisciata indietro chiede il passo opposto`() {
        var passo = 0
        monta(mezzo) { passo = it }

        striscia(avanti = false)

        assertEquals("La strisciata verso destra deve chiedere il filmato prima", -1, passo)
    }

    /**
     * **All'ultimo filmato la pagina torna al suo posto e non chiede niente.**
     *
     * ⚠️⚠️ **È la controprova del difetto vecchio**: rimettendo il gesto della `2.07` (il passo
     * chiesto appena la soglia è superata) qui arriverebbe `1`, cioè il visualizzatore
     * chiederebbe un filmato che non esiste.
     */
    @Test
    fun `al capolinea non si chiede nessun passo`() {
        var passo = 0
        monta(fondo) { passo = it }

        striscia(avanti = true)

        assertEquals("Senza un filmato dopo non si deve chiedere niente", 0, passo)
    }

    /** **Fuori da una cartella il gesto non porta da nessuna parte**, e non deve fingere. */
    @Test
    fun `senza serie la strisciata non fa niente`() {
        var passo = 0
        monta(null) { passo = it }

        striscia(avanti = true)

        assertEquals("Senza serie non si deve chiedere nessun passo", 0, passo)
    }

    /** Il filmato aperto, con la serie che gli si vuole dare intorno. */
    private fun monta(series: Folder.Series?, onStep: (Int) -> Unit) {
        val aperto = series?.at(series.index) ?: Uri.parse("file:///finta/sola.mp4")
        banco.setContent {
            AivTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ClipStage(
                        uri = aperto,
                        series = series,
                        settings = Settings(),
                        onStep = onStep,
                        autoStart = false,
                        onStarted = {}
                    )
                }
            }
        }
        banco.waitForIdle()
    }

    /**
     * Una strisciata orizzontale ben oltre la soglia, che è un quinto della larghezza.
     *
     * ⚠️ **Tre passi e non uno**: `detectDragGestures` spende il primo movimento per il tocco
     * minimo, quindi un `moveTo` solo lascerebbe nell'accumulatore meno di quanto la prova crede
     * di aver mosso.
     */
    private fun striscia(avanti: Boolean) {
        trascina(avanti)
        alza()
    }

    /** Il dito scende e arriva a destinazione, e là si ferma: il gesto resta aperto. */
    private fun trascina(avanti: Boolean) {
        val scena = banco.onRoot().fetchSemanticsNode().size
        val meta = scena.height * MEZZO
        val da = if (avanti) PARTENZA else ARRIVO
        val a = if (avanti) ARRIVO else PARTENZA
        banco.onRoot().performTouchInput {
            down(Offset(scena.width * da, meta))
            moveTo(Offset(scena.width * ((da + a) / 2f), meta))
            moveTo(Offset(scena.width * a, meta))
        }
        banco.waitForIdle()
    }

    /** Il dito si alza, e da qui parte l'assestamento. */
    private fun alza() {
        banco.onRoot().performTouchInput { up() }
        banco.waitForIdle()
    }

    private companion object {
        const val PARTENZA = 0.85f
        const val ARRIVO = 0.15f
        const val MEZZO = 0.5f
    }
}

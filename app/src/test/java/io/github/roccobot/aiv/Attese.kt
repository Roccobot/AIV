package io.github.roccobot.aiv

import android.content.Context
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.fail
import org.robolectric.Shadows.shadowOf

/**
 * Le **attese condivise** del banco: i tempi e i cicli con cui una prova aspetta qualcosa che non
 * arriva nella stessa passata.
 *
 * ⚠️⚠️ **UN TEMPO D'ATTESA SCRITTO IN OGNI CLASSE È UN TEMPO CHE DIVERGE**: la stessa cosa (che
 * l'anteprima dell'editor arrivi) si aspettava 5 secondi in una classe e 10 in un'altra, e lo
 * stesso ciclo sul filo principale era scritto tre volte, una delle quali scadeva senza dirlo.
 * Qui ogni attesa ha un nome e un perché, una volta sola.
 */

/**
 * Quanto si aspetta qualcosa che arriva da un lavoro in sottofondo: l'anteprima dell'editor, una
 * finestra che legge i nomi dal disco, la fine di un lavoro sul filo principale.
 *
 * ⚠️ **Largo di proposito**: quello che deve fallire è una cosa che non arriva affatto, non una
 * macchina lenta. Una prova verde non lo aspetta mai per intero, perché ogni attesa finisce appena
 * la sua condizione è vera.
 */
internal const val ATTESA = 10_000L

/**
 * Quanto si fa avanzare il clock di Compose perché la prima composizione e i suoi effetti siano
 * in scena, nelle prove che lo muovono a mano.
 */
internal const val NASCITA = 1_000L

/**
 * Quanto si lascia respirare la scena dopo un gesto, col clock fermo.
 *
 * ⚠️ **Più corto della quiete del salto**: un respiro più lungo farebbe riaprire la pillola, e la
 * prova misurerebbe la riapertura invece del gesto.
 */
internal const val RESPIRO = 100L

/**
 * Quanto si aspetta il velo del primo avvio del menu Start, letto dall'archivio delle preferenze.
 *
 * ⚠️ **È più lungo di [ATTESA] perché è una rete, non una misura**: il velo arriva in pochi decimi
 * di secondo, e se tarda [aspettaIlVelo] lo scrive. Il perché tardava, e il rimedio, vivono su di
 * lei.
 */
internal const val VELO_MS = 20_000L

/** Oltre questo tempo un velo arrivato si segnala come lento: di solito arriva in 0,3 s. */
private const val VELO_LENTO_MS = 2_000L

/** La pausa fra un giro e l'altro di [aspetta], cioè il tempo lasciato ai fili veri. */
private const val PASSO_MS = 10L

/**
 * Fa girare il filo principale finché [condizione] non risponde di sì, e fallisce con [perche]
 * quando il [limite] scade.
 *
 * ⚠️ **Il filo principale va fatto girare a mano**: sul banco è fermo, e il seguito di un lavoro
 * che è passato da un filo vero gli arriva come un compito in coda. Fra un giro e l'altro si
 * dorme [PASSO_MS], perché quel lavoro ha bisogno di tempo vero per arrivare in fondo.
 * ⚠️ **Il limite conta le pause e non il tempo dei giri**: è un minimo, e una macchina lenta
 * aspetta di più, mai di meno.
 *
 * @param perche il messaggio con cui la prova cade, cioè che cosa non è successo.
 * @param ogniGiro un passo in più a ogni giro, per una prova che ha anche un'altra ruota da far
 *   girare (la regola di Compose, per esempio).
 * @param dettaglio lo stato da aggiungere al messaggio, letto nel momento in cui la prova cade.
 */
internal fun aspetta(
    perche: String,
    limite: Long = ATTESA,
    ogniGiro: () -> Unit = {},
    dettaglio: () -> String = { "" },
    condizione: () -> Boolean
) {
    repeat((limite / PASSO_MS).toInt()) {
        shadowOf(Looper.getMainLooper()).idle()
        ogniGiro()
        if (condizione()) return
        Thread.sleep(PASSO_MS)
    }
    fail(perche + dettaglio())
}

/**
 * Aspetta il velo del primo avvio del menu Start, e quando non arriva dice perché.
 *
 * ⚠️ Il velo nasce dall'archivio delle preferenze, letto su un altro filo che `waitForIdle` non
 * aspetta: per questo si aspetta il suo testo, e non la quiete della scena.
 * ⚠️⚠️ **PERCHÉ A VOLTE NON ARRIVAVA**: la regola vecchia di Compose (`createComposeRule` senza
 * `v2`) eseguiva gli effetti su un distributore che riprende sul filo che consegna il valore.
 * Quando l'archivio doveva rileggere il suo file (dopo `DisegnoTest`, che scrive altri veli),
 * 'non visto' arrivava su un filo di `DefaultDispatcher`, lo stato si scriveva là e nessuna
 * ricomposizione seguiva. Il rimedio è la regola nuova, `junit4.v2.createComposeRule`, che riprende
 * ogni effetto sul filo principale della prova, come fa l'app sul telefono: le classi che aspettano
 * il velo la usano tutte. L'app non aveva colpa.
 * ⚠️ L'attesa lunga ([VELO_MS]) e il resoconto quando scade restano come rete: dicono lo stato
 * dell'archivio, del permesso e della scena nel momento in cui la prova cade.
 */
internal fun aspettaIlVelo(banco: ComposeContentTestRule, app: Context) {
    val frase = app.getString(R.string.corner_hint)
    val inizio = System.nanoTime()
    try {
        banco.waitUntil(VELO_MS) { banco.onAllNodesWithText(frase).fetchSemanticsNodes().isNotEmpty() }
    } catch (e: ComposeTimeoutException) {
        val p = runBlocking { storedPreferences(app) }
        val testi = banco.onAllNodes(hasText("", substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .mapNotNull { n -> n.config.getOrElseNullable(SemanticsProperties.Text) { null }?.joinToString() }
        val flusso = runBlocking { withTimeoutOrNull(2_000) { Hint.COLUMNS.flow(app).first() } }
        throw AssertionError(
            "Il velo non è comparso. permesso=${Folder.granted(app)} flusso=$flusso archivio=${p.asMap()} " +
                "vista=${SettingsStore.read(p).folderView} testi=$testi", e
        )
    }
    val ms = (System.nanoTime() - inizio) / 1_000_000
    if (ms > VELO_LENTO_MS) println("VELO LENTO: $ms ms")
}

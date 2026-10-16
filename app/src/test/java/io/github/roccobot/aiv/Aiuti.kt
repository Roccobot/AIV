package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Gli **aiuti condivisi** delle prove che montano le schermate vere: i veli del primo avvio,
 * l'immagine finta e i gesti sui cursori dell'editor completo.
 *
 * ⚠️ È la stessa ragione di `Scene.kt`: un aiuto copiato in sei classi è sei aiuti che divergono,
 * e il giorno che la schermata cambia il posto da correggere deve essere uno. Dove due copie
 * differivano davvero, la differenza è un argomento.
 */

private val contesto: Context get() = ApplicationProvider.getApplicationContext()

// ── I veli del primo avvio ──

/**
 * Segna come già visti i veli d'aiuto dell'editor completo (quello dei moduli e quello degli
 * strumenti), più quelli in [anche].
 *
 * ⚠️⚠️ **SI CHIAMA PRIMA DI MONTARE LA SCENA**: un velo copre lo schermo e consuma il primo tocco,
 * che è quello che deve fare davanti a chi apre l'editor la prima volta, e che renderebbe rossa
 * qualunque prova che tocchi un gettone.
 *
 * @param anche i veli in più che la scena incontra, per esempio quello del Disegno.
 */
internal fun senzaOnboarding(vararg anche: Hint) {
    giaVisti(Hint.MODULES, Hint.EDITOR_TOOLS, *anche)
}

/**
 * Segna come già visti i veli della casa: quello delle colonne e, con [cestino], quello del
 * cestino vuoto.
 *
 * ⚠️ Si chiama prima di montare la scena, per la stessa ragione di [senzaOnboarding]: senza, il
 * velo copre il FAB e prende il primo gesto.
 *
 * @param cestino se segnare anche il velo del cestino vuoto. Due classi segnano il solo velo delle
 *   colonne, e restano così: aggiungere l'altro cambierebbe la scena che misurano.
 */
internal fun onboardingGiaVisto(cestino: Boolean = true) {
    if (cestino) giaVisti(Hint.COLUMNS, Hint.BIN_EMPTY) else giaVisti(Hint.COLUMNS)
}

/** Scrive nell'archivio delle preferenze che [quali] sono già stati visti, nell'ordine dato. */
private fun giaVisti(vararg quali: Hint) {
    runBlocking { quali.forEach { it.remember(contesto) } }
}

// ── L'immagine finta ──

/**
 * Un PNG bianco e quadrato di [lato] pixel, scritto nella cache, e il suo indirizzo.
 *
 * ⚠️⚠️ **L'IMMAGINE SERVE DAVVERO**: finché l'anteprima non è decodificata i comandi dell'editor
 * sono spenti, quindi una scena con un indirizzo finto misurerebbe una scheda in cui non si può
 * toccare niente.
 * ⚠️ **Un file vero e non uno stream registrato**: l'editor apre l'indirizzo più di una volta
 * (l'anteprima e il lato lungo), e uno stream registrato sull'ombra del resolver si consuma alla
 * prima lettura.
 * ⚠️ **Il nome porta il lato**, così due prove che vogliono lati diversi non si scambiano il file.
 */
internal fun quadrato(lato: Int = 64): Uri {
    val file = File(contesto.cacheDir, "quadrato-$lato.png")
    if (!file.exists()) {
        val mappa = Bitmap.createBitmap(lato, lato, Bitmap.Config.ARGB_8888)
        mappa.eraseColor(Color.WHITE)
        file.outputStream().use { mappa.compress(Bitmap.CompressFormat.PNG, 100, it) }
        mappa.recycle()
    }
    return Uri.fromFile(file)
}

// ── L'editor completo ──

/**
 * Aspetta che l'editor completo sia in piedi, cioè che l'anteprima sia decodificata, e poi apre
 * [modulo] se c'è.
 *
 * ⚠️⚠️ **`waitForIdle` NON BASTA**: la decodifica dell'anteprima gira su `Dispatchers.IO`, cioè su
 * un filo vero, e nessuna attesa di composizione la comprende. Finché non è finita i cursori sono
 * spenti e il palco non c'è, quindi una prova che agisce prima tocca una scheda che non si può
 * toccare: su una macchina più lenta l'anteprima arriva un attimo dopo, e la corsa cambia esito.
 * ⚠️ **Si aspetta il palco e non un cursore**: il palco compare solo quando l'immagine esiste,
 * mentre i cursori ci sono sempre e cambiano solo stato.
 *
 * @param modulo il gettone da toccare quando la scena è pronta. L'editor nasce sul Ritaglio, quindi
 *   una classe che misura i cursori di un altro modulo lo apre qui, che è anche quello che fa
 *   l'utente prima di lavorarci.
 */
internal fun ComposeTestRule.pronta(modulo: Int? = null) {
    waitUntil(ATTESA) {
        onAllNodesWithContentDescription(contesto.getString(R.string.look_compare))
            .fetchSemanticsNodes().isNotEmpty()
    }
    waitForIdle()
    if (modulo != null) {
        onNodeWithContentDescription(contesto.getString(modulo)).performClick()
        waitForIdle()
    }
}

/** Il cursore della riga [quale], contando nell'ordine in cui la scheda li disegna. */
internal fun ComposeTestRule.cursore(quale: Int): SemanticsNodeInteraction =
    onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))[quale]

/**
 * Porta il cursore [quale] al valore [a] in un gesto solo, come un dito che si alza.
 *
 * ⚠️⚠️ **`SetProgress` NON RICOMPONE FRA LE DUE CHIAMATE, ed è per questo che misura**: l'azione
 * semantica di uno `Slider` invoca `onValueChange` e subito dopo `onValueChangeFinished`, nella
 * stessa passata. Un dito vero ci mette quasi sempre un fotogramma in mezzo, quindi il banco qui è
 * **più severo** della mano, che è quello che serve per prendere un difetto che sul telefono si
 * vedrebbe una volta su dieci.
 */
internal fun ComposeTestRule.muovi(quale: Int, a: Float) {
    cursore(quale).performSemanticsAction(SemanticsActions.SetProgress) { it(a) }
    waitForIdle()
}

/**
 * Tocca la barra del cursore [quale] a una frazione [dove] della sua larghezza, come un dito.
 *
 * ⚠️ **Un tocco secco e non una strisciata**: toccando lontano dal tondo il cursore salta subito
 * al punto, quindi il valore si muove senza iniettare un gesto con la sua durata, che col clock
 * di prova è una delle trappole di casa.
 */
internal fun ComposeTestRule.tocca(quale: Int, dove: Float) {
    cursore(quale).performTouchInput { click(Offset(width * dove, height / 2f)) }
    waitForIdle()
}

/** Che cosa dice di sé il cursore [quale]: il valore che annuncia, non quello che il numero scrive. */
internal fun ComposeTestRule.valore(quale: Int): Float =
    cursore(quale).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current

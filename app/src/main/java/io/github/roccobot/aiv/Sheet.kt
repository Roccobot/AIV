package io.github.roccobot.aiv

import android.view.Gravity
import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Una **bottomsheet**: una scheda appoggiata al bordo di sotto, larga quanto lo schermo, con
 * un titolo in cima, un contenuto che scorre e un piede fermo.
 *
 * ⚠️⚠️ **NON ENTRA SCIVOLANDO DAL BASSO, ed è la richiesta che la definisce** (utente, giro
 * della 1.37: *info dettagliate come bottomsheet (ma che non deve entrare scorrendo da sotto).
 * Appare con animazione semplice e veloce*). Una bottomsheet di Material sale dal fondo per
 * costruzione, e una finestra di sistema porta comunque la sua animazione di entrata: qui
 * l'animazione della finestra si **spegne** ([Gravity] e `setWindowAnimations(0)` in
 * [sheetWindow]) e al suo posto resta la dissolvenza con la crescita da [SHEET_SMALL], che
 * sono i numeri già scelti da lui per i menu.
 * ⚠️ **Perciò non è una `ModalBottomSheet`**: quella è fatta per salire e per trascinarsi, e
 * spegnere le due cose che la definiscono vorrebbe dire tenerla per il colore dello sfondo.
 *
 * ⚠️⚠️ **IL PIEDE È FERMO E IL CORPO SCORRE SOTTO DI LUI** (*la pillola con il nome è in fondo,
 * verso il bordo inferiore, ancorata: il resto rimane sotto allo scorrimento*): il piede sta
 * fuori dalla colonna che scorre e porta il fondo della scheda, quindi il contenuto gli
 * sparisce dietro invece di scorrergli sopra.
 * ⚠️ **Il corpo prende il posto che avanza e non di più** (`weight(1f, fill = false)`): con
 * poche righe la scheda è bassa, con molte cresce fino allo schermo e da lì scorre. Una
 * altezza fissa sarebbe giusta su un telefono solo.
 *
 * ⚠️⚠️ **SI CHIUDE TOCCANDO FUORI, e quel tocco lo raccogliamo NOI**: con
 * `usePlatformDefaultWidth = false` la finestra copre lo schermo, quindi per il sistema non
 * esiste più un 'fuori' da riconoscere. Il velo sopra la scheda è una superficie trasparente
 * che chiude, ed è deterministico invece di dipendere da come Compose misura il contenuto.
 * ⚠️ **Il gesto Indietro chiude sempre**, come in ogni dialogo, e la crocetta in testa è la
 * via visibile per chi non conosce nessuna delle due.
 */
@Composable
fun Sheet(
    title: String,
    onDismiss: () -> Unit,
    foot: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        sheetWindow()
        WindowVeil()

        var grown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { grown = true }
        val show by animateFloatAsState(
            targetValue = if (grown) 1f else 0f,
            animationSpec = tween(durationMillis = SHEET_IN),
            label = "sheet"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                /*
                 * ⚠️ **Niente increspatura e nessuna descrizione**: questo non è un tasto, è
                 * lo spazio vuoto sopra la scheda, e un cerchio d'inchiostro che si apre dove
                 * si tocca per chiudere direbbe che là c'era qualcosa da premere.
                 */
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = show
                        val k = SHEET_SMALL + (1f - SHEET_SMALL) * show
                        scaleX = k
                        scaleY = k
                    }
                    /*
                     * ⚠️ **Il tocco sulla scheda NON deve chiudere**, e senza questa riga lo
                     * farebbe: il velo di sopra è un genitore, e un tocco che nessuno consuma
                     * gli arriverebbe. Un `clickable` senza effetto è il modo di dire 'qui mi
                     * fermo' senza inventare un gesto.
                     */
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { }
                    ),
                shape = RoundedCornerShape(topStart = SHEET_ROUND, topEnd = SHEET_ROUND),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(modifier = Modifier.padding(SHEET_PAD)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        /*
                         * ⚠️⚠️ **IL TITOLO RESTA CENTRATO, ed è una decisione della 0.73 presa
                         * su un suo mockup**: allora era centrato perché la pastiglia del nome
                         * stava sulla riga sotto, e adesso che la pastiglia è scesa in fondo
                         * quella ragione non c'è più, ma la scelta sì. Lo spazio a sinistra è
                         * largo come la crocetta a destra, quindi il centro è il centro vero
                         * della scheda e non quello dello spazio che avanza.
                         */
                        Spacer(Modifier.size(SHEET_SHUT))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(SHEET_SHUT)) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                // ⚠️ Descritta col nome che il tasto aveva quando era una
                                // parola: chi la sente leggere sente 'Chiudi', come prima.
                                contentDescription = stringResource(R.string.pick_close)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = SHEET_GAP),
                        verticalArrangement = Arrangement.spacedBy(SHEET_GAP)
                    ) {
                        content()
                    }
                    foot()
                }
            }
        }
    }
}

/**
 * Toglie alla finestra del dialogo la sua animazione e la stende su tutto lo schermo.
 *
 * ⚠️⚠️ **`setWindowAnimations(0)` È LA RIGA CHE CHIUDE UNA QUESTIONE APERTA DALLA 1.25**: là
 * si era concluso che l'animazione di entrata la decide il telefono e che *da dentro non si
 * spegne*. Si spegne: uno stile di animazione a zero vuol dire nessuna animazione, e senza di
 * lei l'unico movimento è quello scritto in [Sheet]. Su una scheda appoggiata in basso non è
 * un dettaglio, perché l'animazione di serie sarebbe **proprio** la salita dal fondo che lui
 * ha vietato.
 * ⚠️ **La finestra si stende su tutto** e la scheda si appoggia in basso da sé, dentro: così
 * lo spazio sopra esiste, è nostro, e può raccogliere il tocco che chiude.
 * ⚠️ **`Gravity.BOTTOM` con una finestra a tutto schermo non sposta niente**, e si scrive lo
 * stesso: dice qual è il bordo di riferimento se un domani la finestra tornasse alta quanto il
 * contenuto.
 */
@Composable
private fun sheetWindow() {
    val view = LocalView.current
    DisposableEffect(view) {
        // ⚠️ La stessa risalita del velo, e non un secondo modo di trovare la finestra:
        // vedi `dialogWindow` in `Veil.kt`.
        val window = view.dialogWindow()
        window?.apply {
            setWindowAnimations(0)
            setGravity(Gravity.BOTTOM)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // ⚠️ Lo sfondo della finestra deve essere trasparente, o dietro la scheda si
            // vedrebbe il rettangolo di serie del dialogo al posto del velo.
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        onDispose { }
    }
}

/**
 * Lo stondamento dei due angoli in alto.
 *
 * ⚠️ **28dp, che è quello delle bottomsheet**, e le regole di casa lo dicono già: i menu
 * stanno a 20 apposta per non confondersi con loro (vedi `MENU_ROUND`). Una scheda appoggiata
 * in basso è una bottomsheet e prende il numero delle bottomsheet.
 */
private val SHEET_ROUND = 28.dp

/** Il rientro attorno al contenuto, e l'aria fra una riga e l'altra. */
private val SHEET_PAD = 24.dp
private val SHEET_GAP = 12.dp

/**
 * Il lato della crocetta, che è anche lo spazio speculare a sinistra del titolo.
 *
 * ⚠️ **48dp e non 24**: è l'area toccabile minima di Material, non la misura del disegno. Un
 * numero solo per i due lati, o il titolo sarebbe centrato di sbieco.
 */
private val SHEET_SHUT = 48.dp

/**
 * Come arriva: da quanto piccola cresce, e in quanto tempo.
 *
 * ⚠️ **Sono i numeri dei menu**, scelti dall'utente su un mockup e già in vigore in
 * `MenuShell`: 'animazione semplice e veloce' in questa app è già stata definita una volta, e
 * definirla una seconda con altri due numeri farebbe due velocità per la stessa idea.
 */
private const val SHEET_SMALL = 0.96f
private const val SHEET_IN = 170

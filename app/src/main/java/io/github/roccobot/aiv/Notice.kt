package io.github.roccobot.aiv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Il canale con cui l'app dice com'è andata: una riga per volta, in fondo allo schermo.
 *
 * ⚠️⚠️ **NASCE NELLA `1.84` DALLA SUA RISPOSTA `casa` A `d-avvisi`** (giro della `1.81`), e la
 * questione che chiude era aperta da un censimento: con il cestino acceso un'eliminazione taceva
 * e parlava la notifica di casa, con il cestino spento la stessa azione produceva un **avviso di
 * sistema**, e copia, spostamento e rinomina parlavano sempre con l'avviso di sistema. Cioè lo
 * stesso genere di esito aveva due voci, e quale delle due si sentisse dipendeva da
 * un'impostazione che con la notizia non c'entra.
 *
 * ⚠️⚠️ **PERCHÉ UN CANALE E NON DICIASSETTE RITOCCHI**: gli avvisi erano diciassette in sette
 * file, e ognuno chiamava il sistema per conto suo. Correggerne uno per volta avrebbe fatto una
 * seconda incoerenza al posto della prima, perché la stessa chiamata compare identica in più
 * schermate; con un canale, una schermata nuova non ha un secondo modo di parlare.
 *
 * ⚠️⚠️ **UNA RIGA PER VOLTA, E LA PIÙ NUOVA VINCE: non c'è nessuna coda, ed è una scelta.** Una
 * coda mostrerebbe a turno cose che si riferiscono a un momento già passato, e la seconda
 * arriverebbe quando l'utente ha già cambiato schermata. Quello che l'app ha da dire riguarda
 * **l'ultima** cosa che è successa.
 * - ⚠️ **Le operazioni che parlano non si sovrappongono per costruzione** (`FileKind.speaks`), e
 *   l'unico caso in cui due messaggi nascono insieme è un'eliminazione riuscita a metà: là il
 *   secondo copre il primo, che è meglio di due notifiche sovrapposte come succedeva prima.
 *
 * ⚠️⚠️ **IL TESTO ARRIVA GIÀ RISOLTO, e non come identificatore di risorsa**: metà dei chiamanti
 * compone una frase con un plurale o con un nome di file, e un canale che accettasse solo
 * `@StringRes` costringerebbe a due strade. Chi chiama ha già il `Context` sotto mano, perché
 * era quello che serviva anche all'avviso di sistema.
 */
object Notices {

    /**
     * Quello che l'app sta dicendo adesso.
     *
     * ⚠️⚠️ **L'IDENTIFICATORE NON È UN LUSSO: senza, due messaggi UGUALI di fila sono un
     * messaggio solo.** Il conto alla rovescia e l'entrata si agganciano a lui, quindi due
     * salvataggi identici danno due notifiche invece di una che resta ferma e scade quando
     * scadeva la prima. È lo stesso difetto che `model.notice` aveva chiuso azzerandosi dopo
     * ogni avviso, e qui si chiude con un numero che cresce.
     */
    data class Line(
        val id: Long,
        val text: String,
        /** L'etichetta del tasto a destra, o `null` per una riga che si limita a dire. */
        val action: String? = null,
        /** Che cosa fa quel tasto. La riga se ne va da sé prima di eseguirlo. */
        val onAction: (() -> Unit)? = null,
        /**
         * Che cosa succede quando la riga se ne va, per scadenza o perché il tasto è stato
         * toccato.
         *
         * ⚠️⚠️ **È QUELLO CHE TIENE UN TIMER SOLO**: l'offerta di disfare vive in [Undo] e deve
         * morire con la notifica che la offre. Con due attese parallele (una per la riga e una
         * per l'offerta) i due tempi sarebbero due sorgenti dello stesso istante, e il giorno
         * che uno dei due cambia si vedrebbe un tasto 'Annulla' che non fa più niente.
         */
        val onGone: (() -> Unit)? = null,
        val millis: Long = NOTICE_MS
    )

    var line by mutableStateOf<Line?>(null)
        private set

    private var conta = 0L

    /** Una riga che dice e basta. Torna il suo identificatore, per chi deve toglierla prima. */
    fun say(text: String, millis: Long = NOTICE_MS): Long =
        posa(Line(id = ++conta, text = text, millis = millis))

    /**
     * Una riga con un tasto: dice che cosa è successo e offre di rimediare.
     *
     * ⚠️ **La durata di serie è quella dell'offerta di disfare** ([UNDO_MS]): un tasto che si può
     * toccare deve durare quanto il tempo che si dà a chi lo legge, e tre secondi sono i suoi.
     */
    fun offer(
        text: String,
        action: String,
        millis: Long = UNDO_MS,
        onGone: (() -> Unit)? = null,
        onAction: () -> Unit
    ): Long = posa(
        Line(
            id = ++conta,
            text = text,
            action = action,
            onAction = onAction,
            onGone = onGone,
            millis = millis
        )
    )

    private fun posa(nuova: Line): Long {
        // ⚠️ La riga che se ne va per fare posto a un'altra ha finito la sua vita come se fosse
        // scaduta: chi ne aspettava la fine (l'offerta di disfare) deve saperlo lo stesso.
        line?.onGone?.invoke()
        line = nuova
        return nuova.id
    }

    /**
     * Toglie la riga [id], se è ancora quella in scena.
     *
     * ⚠️⚠️ **SI TOGLIE PER IDENTIFICATORE E NON IN BLOCCO, e la ragione è una corsa vera**: fra
     * il momento in cui una riga scade e quello in cui l'attesa se ne accorge può esserne
     * arrivata un'altra, e un congedo cieco porterebbe via il messaggio nuovo. Vale anche per chi
     * lega una riga a una schermata: uscendo, deve togliere **la sua**.
     */
    fun dismiss(id: Long) {
        val quale = line ?: return
        if (quale.id != id) return
        line = null
        quale.onGone?.invoke()
    }
}

/**
 * Quanto resta in scena una riga che dice e basta.
 *
 * ⚠️⚠️ **SONO I DUE TEMPI DELL'AVVISO DI SISTEMA, e non due numeri nuovi**: `Toast.LENGTH_SHORT`
 * dura 2 secondi e `LENGTH_LONG` 3,5 (letti in `NotificationManagerService`), e sono le durate
 * che queste stesse frasi avevano fino alla `1.83`. Portarle in casa cambiando anche il tempo
 * avrebbe cambiato due cose insieme, e al primo 'mi sembra troppo veloce' non si saprebbe quale
 * delle due guardare.
 * ⚠️ **Chi diceva `LENGTH_LONG` tiene [NOTICE_LONG_MS]**, cioè gli esiti di un'operazione sui
 * file: sono frasi con un numero dentro, e si leggono più lentamente di 'Copiato'.
 * ⚠️⚠️ **MA DALLA `1.85` QUELLA LUNGA È DI TRE SECONDI, ED È SUA** (riscontro del giro della
 * `1.84`, voce `voce-unica` approvata con una domanda: *cosa dura tre secondi e mezzo? A meno che
 * non ci sia un motivo specifico, portalo a 3*). Il motivo c'era e adesso è **speso**: i 3,5
 * erano `LENGTH_LONG`, e servivano a non cambiare la superficie e il tempo insieme. Con la
 * superficie nuova già provata e approvata, quel vincolo è finito.
 * ⚠️ **Adesso coincide con [UNDO_MS]**, cioè coi tre secondi in cui si può disfare, e non è un
 * caso da correggere: una frase che offre 'Annulla' e una che dice com'è andata restano in scena
 * lo stesso tempo, che è la cosa che uno si aspetta guardandole.
 */
const val NOTICE_MS = 2000L
const val NOTICE_LONG_MS = 3000L

/**
 * La superficie unica con cui l'app parla, in fondo allo schermo.
 *
 * ⚠️⚠️ **ERA `UndoNotice` FINO ALLA `1.83`, e il nome mentiva già da un giro**: dalla `1.82`
 * mostrava anche 'Hai già scaricato questa immagine', che non offre di disfare niente. Adesso
 * porta qualunque cosa l'app abbia da dire, e il nome lo dice.
 *
 * ⚠️⚠️ **È UNO `Snackbar` DI MATERIAL E NON UNA SUPERFICIE DISEGNATA IN CASA**: la frase a
 * sinistra e l'azione a destra sulla stessa riga sono esattamente la sua forma, e con lui
 * arrivano il colore, lo stondamento, i rientri e i due stili di testo, che rifatti a mano
 * sarebbero sei valori da indovinare (la nota in testa a `Glyphs.kt` dice di non ridisegnare
 * quello che Material ha già).
 * ⚠️⚠️ **SENZA `SnackbarHost` E SENZA `SnackbarHostState`, e non è una scorciatoia**: quella
 * coppia serve a chi ha una **coda** di messaggi da mostrare a turno, e vuole un `Scaffold`, che
 * queste schermate non hanno. Qui la coda non c'è di proposito (vedi [Notices]) e la durata la
 * decide chi manda la riga, mentre con l'ospite sarebbe di Material.
 * ⚠️⚠️ **DALLA `1.69` IL FONDO NON È ROVESCIATO, ED È UNA SUA SCELTA FRA CINQUE DISEGNI** (giro
 * della `1.67`, domanda `d-avviso`: ha scelto `tempo`). Una notifica di Material è chiara sul
 * tema scuro e scura sul chiaro, cioè l'unica superficie dell'app che inverte i colori, e in
 * mezzo a pannelli e schede che non lo fanno si legge come un pezzo di un'altra applicazione.
 * Adesso prende la superficie dell'app, il suo inchiostro, il **bordo d'accento** che portano
 * tutte le altre ([Modifier.edged], dalla `1.54`) e lo stesso raggio.
 * ⚠️⚠️ **E UNA RIGA CHE SI CONSUMA IN FONDO, che è la ragione del nome che quel disegno ha nel
 * documento**: dice quanto tempo resta per toccare il tasto. ⚠️ **Solo dove un tasto c'è**: su
 * una riga che si limita a dire, una barra che scorre annuncerebbe una scadenza che non serve a
 * nessuna decisione.
 * ⚠️ **Il colore del tasto resta scritto a mano**, ma è [accentInk]: su un fondo che non è
 * rovesciato il colore che Material sceglie per il fondo rovesciato sarebbe sbagliato, e questo è
 * l'accento nella versione che si può **leggere**, che è il caso di una parola.
 * ⚠️⚠️ **ARRIVA E SE NE VA COME LE DUE SCHEDE, con gli stessi numeri** ([arrivaDalBasso],
 * [vaGiu] in `Sheet.kt`): dalla `1.43` 'arrivare dal basso' in questa app ha una definizione, e
 * una notifica che comparisse di scatto accanto a due schede che scorrono direbbe di essere
 * un'altra famiglia di cose.
 * ⚠️ **Vive in una funzione a sé per la stessa ragione di `FabPop`**: chiamata sul posto,
 * `AnimatedVisibility` finisce sull'overload di `ColumnScope` e il compilatore la rifiuta.
 *
 * ⚠️⚠️ **UN AVVISO DI SISTEMA RESTA, ED È UNO SOLO: quello che spiega perché si sta per aprire
 * la pagina delle impostazioni di Android** (`ViewerActivity`, `folder_why`). Là l'app va in
 * **secondo piano** nello stesso istante, quindi una notifica di casa non si vedrebbe affatto: è
 * esattamente il caso per cui l'avviso di sistema esiste, cioè dire una cosa mentre si esce.
 */
@Composable
fun AppNotice(line: Notices.Line?, modifier: Modifier = Modifier) {
    /*
     * ⚠️ **L'ultima riga vista si tiene, e non è ridondanza**: la notifica esce di scena con la
     * riga, ma la sua animazione dura più di lei, e in quei millisecondi il testo da disegnare è
     * quello che se ne sta andando. Senza, l'uscita mostrerebbe una striscia vuota.
     */
    var ultima by remember { mutableStateOf<Notices.Line?>(null) }
    if (line != null) ultima = line
    val quale = ultima

    /*
     * ⚠️⚠️ **L'ATTESA VIVE QUI, dove vive la riga**: prima ogni chiamante aspettava per conto suo
     * e poi toglieva la notifica, quindi la durata era scritta in tre posti e il conto alla
     * rovescia in un quarto. Adesso chi manda un messaggio dice quanto deve durare e non deve
     * ricordarsi di toglierlo.
     * ⚠️ **La chiave è l'identificatore**: due messaggi uguali di fila sono due attese, e la
     * seconda riparte da capo.
     */
    LaunchedEffect(line?.id) {
        val viva = line ?: return@LaunchedEffect
        delay(viva.millis)
        Notices.dismiss(viva.id)
    }

    /*
     * ⚠️⚠️ **LA RIGA SI CONSUMA NEL TEMPO CHE LA NOTIFICA VIVE DAVVERO**: il numero è quello del
     * messaggio, quindi la barra arriva a zero nell'istante in cui la notifica se ne va. Una
     * barra ferma a zero sopra un tasto che funziona ancora è peggio di nessuna barra.
     * ⚠️ **Riparte da capo a ogni messaggio** e non alla prima comparsa soltanto.
     * ⚠️ **Si legge nel DISEGNO**: `scaleX` vive dentro `graphicsLayer`, quindi l'animazione
     * costa un ridisegno per fotogramma e nessuna ricomposizione.
     */
    val resta = remember { Animatable(1f) }
    LaunchedEffect(line?.id) {
        val viva = line ?: return@LaunchedEffect
        resta.snapTo(1f)
        resta.animateTo(0f, tween(viva.millis.toInt(), easing = LinearEasing))
    }

    AnimatedVisibility(
        visible = line != null,
        modifier = modifier,
        enter = arrivaDalBasso(),
        exit = vaGiu()
    ) {
        Box(
            // ⚠️ **Il rientro di sistema se lo mette da sé**, come le due schede: questa vive
            // nel `Box` di radice, che arriva al bordo dello schermo, quindi senza questa riga
            // starebbe sotto la barra di navigazione.
            // ⚠️ **E qui la scheda si comporta al contrario**: là il fondo passa sotto la barra
            // apposta (per prenderne il colore) e il rientro va sul contenuto; una notifica non
            // è appoggiata a niente e va spostata intera.
            modifier = Modifier
                .navigationBarsPadding()
                .padding(NOTICE_EDGE)
        ) {
            Snackbar(
                /*
                 * ⚠️⚠️ **LA REGIONE VIVA È LA META DI `SnackbarHost` CHE ANDAVA RECUPERATA, e
                 * fino alla `1.80` la notifica non veniva annunciata affatto** (censimento della
                 * UI del 2026-09-05). La ragione scritta qui sopra per non usare l'ospite
                 * riguarda la coda e la durata, e resta buona; ma nel bytecode di
                 * `SnackbarHostKt` vivono anche `liveRegion` e l'azione di congedo, mentre
                 * `SnackbarKt` non ne porta nessuna: rinunciando all'ospite si era rinunciato
                 * anche a loro, senza accorgersene.
                 * ⚠️ **`Polite` e non `Assertive`**: la notifica dice che una cosa è **già**
                 * successa, quindi non deve interrompere quello che il lettore di schermo sta
                 * leggendo. Con `Assertive` ogni salvataggio tapperebbe la bocca alla schermata.
                 */
                modifier = Modifier
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .edged(NOTICE_ROUND),
                shape = RoundedCornerShape(NOTICE_ROUND),
                /*
                 * ⚠️ **I due ruoli sono della stessa famiglia, dalla `1.81`**: fino alla `1.80`
                 * il fondo era `surfaceVariant` e l'inchiostro `onSurface`, cioè quello di
                 * un'altra superficie, e la nota in testa promette *la superficie dell'app e il
                 * suo inchiostro*. Non si vedeva niente perché le due tinte si somigliano, che è
                 * il caso in cui il criterio di casa vale di più: il ruolo giusto anche quando i
                 * due valori sono vicini, come già scritto sulla pastiglia del nome.
                 */
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                action = quale?.action?.let { etichetta ->
                    {
                        TextButton(
                            onClick = {
                                // ⚠️ La riga se ne va PRIMA del lavoro: quello che il tasto fa
                                // può aprire una schermata o durare qualche istante, e una
                                // notifica che resta in scena mentre il suo effetto è già in
                                // corso si fa toccare due volte.
                                Notices.dismiss(quale.id)
                                quale.onAction?.invoke()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = accentInk())
                        ) { Text(etichetta) }
                    }
                }
            ) {
                Text(quale?.text ?: "")
            }
            /*
             * ⚠️ **Il ritaglio vive sulla scatola della riga e non su quella di fuori**: il bordo
             * d'accento sconfina di mezzo pixel oltre la superficie (vedi `Edge.kt`), e un
             * ritaglio sul genitore glielo taglierebbe proprio sugli archi, che è il difetto che
             * la `1.56` aveva chiuso.
             * ⚠️ **L'origine della scala è il fianco iniziale**, non il centro: una riga che si
             * consuma parte piena e si ritira verso il punto da cui è partita.
             */
            if (quale?.action != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(NOTICE_ROUND))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(CLESSIDRA)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(0f, 0.5f)
                                scaleX = resta.value
                            }
                            // ⚠️ **La tavolozza e non [aivAccent], dalla `1.81`**: quella
                            // funzione serve a chi legge un colore da un nodo di modificatore,
                            // dove il tema non si raggiunge, e qui siamo dentro un composabile
                            // che due righe più su la tavolozza la legge già.
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

/**
 * Il respiro fra la notifica e i tre bordi che la circondano.
 *
 * ⚠️ **12dp, che è quello che `SnackbarHost` di Material mette da sé**: qui l'ospite non c'è,
 * quindi il margine che avrebbe messo lui va scritto. Senza, la notifica toccherebbe i lati
 * dello schermo e la barra di sistema.
 */
private val NOTICE_EDGE = 12.dp

/**
 * Lo stondamento della notifica e lo spessore della riga che si consuma.
 *
 * ⚠️⚠️ **IL RAGGIO È SUO, E FINO ALLA `1.78` LA NOTA DICEVA CHE ERA 'QUELLO DEI PANNELLI'**:
 * quel raggio non esiste, perché i raggi di casa sono 20dp per i menu e 28dp per schede e
 * dialoghi, e nessuna superficie dell'app misura 14. Una nota che rimanda a una fonte condivisa
 * inesistente manda chi ritocca il valore a cercarla, e nel frattempo il numero lo riceve anche
 * il bordo d'accento.
 * ⚠️ **Perché più piccolo di quelli**: una notifica è alta una riga e larga quanto lo schermo
 * meno i margini, e su una striscia bassa un raggio da 28 diventa un fianco tutto curva.
 * ⚠️ **Tre punti per la riga**, che è la misura del disegno che ha scelto: più sottile non si
 * vede su un fondo che ha già un bordo da due, più spessa diventa una seconda cornice.
 */
private val NOTICE_ROUND = 14.dp
private val CLESSIDRA = 3.dp

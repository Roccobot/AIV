package io.github.roccobot.aiv

import android.content.res.Resources
import android.net.Uri
import androidx.annotation.PluralsRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Le operazioni sui file e i loro dialoghi, in un posto solo.
 *
 * ⚠️⚠️ **NASCE DALLA 0.62, PERCHÉ I POSTI CHE LE CHIEDONO SONO DIVENTATI DUE**: la
 * selezione nella griglia e il tocco lungo nel visualizzatore. Fino alla `0.61` tutto questo
 * viveva dentro `GridScreen`, che era giusto finché era l'unico a chiedere; una seconda copia
 * nel visualizzatore avrebbe voluto dire due dialoghi di eliminazione da tenere d'accordo, e
 * il cestino (che arriva dopo) ne cambia il testo.
 *
 * ⚠️ **Quello che NON sta qui, e non è una dimenticanza**: che cosa fare a operazione
 * finita. La griglia svuota la selezione e si rilegge, il visualizzatore deve capire quale
 * fotografia mostrare al posto di quella che non c'è più. Sono due risposte diverse alla
 * stessa domanda, quindi le decide chi chiama ([onRun]) e non questo file.
 */
@Composable
fun FileJobDialogs(
    job: FileJob?,
    /**
     * I campi delle informazioni, nell'ordine scelto e senza quelli spenti: arrivano da
     * `Settings.factRows`.
     *
     * ⚠️ **Li riceve invece di leggerli**: questo file non sa niente delle impostazioni, e
     * la griglia non ne ha una copia da cui leggerle. Chi chiama ce le passa perché è
     * l'unico che le ha in mano.
     */
    fields: List<FactField>,
    onClose: () -> Unit,
    /**
     * Fa partire l'operazione: riceve **che cosa** si sta facendo e il lavoro da svolgere.
     *
     * ⚠️ Il lavoro arriva come funzione sospesa e non eseguito: chi chiama lo lancia nel
     * **suo** ambito, che è la sola cosa che gli permette di sopravvivere al dialogo che
     * si chiude e alla schermata che cambia.
     */
    onRun: (FileKind, suspend () -> FileTree.Outcome) -> Unit
) {
    val context = LocalContext.current
    when (job) {
        null -> Unit

        is FileJob.Transfer -> DestinationDialog(
            action = if (job.move) R.string.dest_move_here else R.string.dest_here,
            onDismiss = onClose,
            onPick = { dir ->
                onClose()
                if (job.move) onRun(FileKind.MOVE) { FileTree.move(context, job.uris, dir) }
                else onRun(FileKind.COPY) { FileTree.copy(context, job.uris, dir) }
            }
        )

        is FileJob.Rename -> RenameDialog(
            uris = job.uris,
            onDismiss = onClose,
            onRename = { template, start ->
                onClose()
                onRun(FileKind.RENAME) { FileTree.rename(context, job.uris, template, start) }
            }
        )

        is FileJob.Delete -> DeleteDialog(
            count = job.uris.size,
            forGood = job.forGood,
            onDismiss = onClose,
            onConfirm = {
                onClose()
                if (job.forGood) onRun(FileKind.DELETE) { FileTree.delete(context, job.uris) }
                else onRun(FileKind.TRASH) { Bin.send(context, job.uris) }
            }
        )

        /*
         * ⚠️⚠️ **PARTE DA SÉ, ed è l'unica voce che non apre niente**: il ripristino non ha
         * domande da fare, quindi passa da qui solo per usare lo stesso imbuto delle altre
         * (l'avviso dell'esito, la rilettura, l'ambito che sopravvive). Un secondo canale
         * per una sola operazione avrebbe voluto un richiamo in più su due schermate.
         * ⚠️ **L'effetto è legato al lavoro e non alla composizione**: gira una volta, poi
         * `onClose` toglie questo ramo di scena e con lui l'effetto. Il lavoro no: `onRun`
         * lo ha già lanciato nell'ambito di chi chiama.
         */
        is FileJob.Restore -> LaunchedEffect(job) {
            onRun(FileKind.RESTORE) { Bin.restore(context, job.uris) }
            onClose()
        }

        is FileJob.Facts -> FactsDialog(uris = job.uris, fields = fields, onDismiss = onClose)
    }
}

/**
 * Il dialogo aperto, e su quali immagini.
 *
 * ⚠️⚠️ **LE IMMAGINI SI FOTOGRAFANO QUANDO IL DIALOGO SI APRE, non quando si conferma**:
 * l'elenco viaggia dentro il lavoro invece di essere riletto dalla selezione viva. Il
 * dialogo è modale, quindi le due letture darebbero lo stesso risultato **oggi**, e domani
 * basterebbe un dialogo non modale o una selezione che si aggiorna da sola perché una
 * conferma agisse su file diversi da quelli che sono stati mostrati.
 */
sealed interface FileJob {
    val uris: List<Uri>

    /**
     * Copia o spostamento, che differiscono **solo** per la parola sul tasto e per il
     * fatto che il secondo porta via l'originale: la cartella di arrivo la si chiede allo
     * stesso modo.
     */
    class Transfer(override val uris: List<Uri>, val move: Boolean) : FileJob
    class Rename(override val uris: List<Uri>) : FileJob

    /**
     * L'eliminazione, che dalla `0.64` vuol dire due cose diverse.
     *
     * ⚠️⚠️ **[forGood] NON è un'opzione ma il POSTO in cui si è**: fuori dal cestino
     * eliminare sposta là dentro, dentro il cestino cancella. Il dialogo lo dice con due
     * testi diversi, e chi passasse `false` stando nel cestino manderebbe una foto del
     * cestino nel cestino, cioè da nessuna parte.
     */
    class Delete(override val uris: List<Uri>, val forGood: Boolean) : FileJob

    /**
     * Il ripristino, che è la sola voce **senza dialogo**.
     *
     * ⚠️ Non c'è niente da chiedere e niente da scegliere: la destinazione è scritta
     * nell'archivio del cestino, e l'operazione è reversibile (si rielimina). Chiedere
     * conferma per un gesto che rimette le cose come stavano è il modo di insegnare a
     * confermare senza leggere.
     */
    class Restore(override val uris: List<Uri>) : FileJob
    class Facts(override val uris: List<Uri>) : FileJob
}

/**
 * Le quattro operazioni che toccano i file, con quello che di ognuna serve sapere dopo.
 *
 * ⚠️⚠️ **[gone] è la ragione per cui questo elenco esiste** invece di passare in giro la
 * sola stringa dell'esito: dice se l'indirizzo di partenza vale ancora. Per il
 * visualizzatore è la differenza fra restare dov'è e dover trovare un'altra fotografia da
 * mostrare, e scritto qui è una riga, sparso fra le schermate sarebbe una condizione da
 * ricordare in ogni posto.
 * ⚠️ **La rinomina è fra quelle che 'portano via' il file**, e non è un errore: la
 * fotografia c'è ancora, ma la sua riga nel MediaStore cambia numero, quindi l'indirizzo
 * che si aveva in mano non apre più niente.
 */
enum class FileKind(@PluralsRes val done: Int, val gone: Boolean) {
    COPY(R.plurals.copy_done, gone = false),
    MOVE(R.plurals.move_done, gone = true),
    RENAME(R.plurals.rename_done, gone = true),

    /**
     * L'eliminazione di tutti i giorni, dalla `0.64`: il file va nel **cestino**.
     *
     * ⚠️ Porta `gone = true` come le altre perché per la schermata è la stessa cosa: quel
     * file non è più là. Che sia recuperabile è una faccenda del cestino, non di chi
     * guardava.
     */
    TRASH(R.plurals.trash_done, gone = true),

    /** L'eliminazione definitiva: dentro il cestino, o svuotandolo. */
    DELETE(R.plurals.delete_done, gone = true),

    /** Il ritorno alla cartella d'origine, da dentro il cestino. */
    RESTORE(R.plurals.restore_done, gone = true)
}

/**
 * Che cosa dire quando un'operazione finisce: quante sono passate e, solo se ce ne sono,
 * quante no.
 *
 * ⚠️ Le forme plurali si risolvono con `getQuantityString` e non con
 * `pluralStringResource`, perché il numero si sa solo a lavoro finito e quella funzione si
 * può chiamare soltanto mentre si compone.
 * ⚠️ **Un avviso solo con tutti e due i numeri**: due avvisi di fila si coprono a vicenda,
 * e il secondo si leggerebbe senza il primo.
 */
fun outcomeText(res: Resources, out: FileTree.Outcome, @PluralsRes doneRes: Int): String =
    buildString {
        append(res.getQuantityString(doneRes, out.done, out.done))
        if (out.failed > 0) {
            append(", ")
            append(res.getQuantityString(R.plurals.op_failed, out.failed, out.failed))
        }
    }

/**
 * La conferma dell'eliminazione, con due testi.
 *
 * ⚠️⚠️ **IL TESTO CAMBIA PERCHÉ IL GESTO CAMBIA, dalla `0.64`**: fuori dal cestino le
 * fotografie ci vanno dentro e si possono ripristinare, dentro il cestino vanno via per
 * sempre. Fino alla `0.63` c'era un testo solo, e diceva *vanno via dal telefono per
 * sempre: AIV non ha un cestino suo*: era vero allora e sarebbe una bugia adesso.
 * ⚠️ **La conferma resta anche per il cestino**, benché il gesto sia reversibile: costa un
 * tocco, e su una selezione da quaranta fotografie toccate per sbaglio vale quel tocco.
 *
 * ⚠️⚠️ **IL CONTO SI È SPOSTATO DAL TITOLO AL CORPO nella 0.68** (testo dettato
 * dall'utente: titolo *Confermi l'eliminazione?*, corpo *Stai per spostare (X) immagini nel
 * cestino: potrai recuperarle fino all'eliminazione definitiva*). Fino alla `0.67` era il
 * contrario, con la ragione scritta che chi tocca in fretta legge solo la prima riga:
 * quella nota è **superata**, e il baratto nuovo è che il titolo dice il **gesto** e il
 * corpo dice **quanto** e **cosa succede dopo**.
 * ⚠️⚠️ **Perciò `delete_ask` NON è più un plurale e i due corpi lo sono diventati**, in
 * tutte e sedici le lingue: il conto se l'è portato dietro la frase che gli deve concordare
 * intorno. Chi rimettesse il conto nel titolo deve rifare quel giro al contrario, non
 * cambiare una riga qui.
 */
@Composable
private fun DeleteDialog(
    count: Int,
    forGood: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_ask)) },
        text = {
            Text(
                pluralStringResource(
                    if (forGood) R.plurals.delete_desc else R.plurals.trash_desc,
                    count,
                    count
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text(stringResource(R.string.pick_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Che cosa si è scelto: i numeri di una selezione, o i dati di un file solo.
 *
 * ⚠️ **I dati si leggono quando il dialogo si apre e non prima**: contare il peso di
 * trecento file vuol dire trecento interrogazioni, e farle a ogni tocco su una miniatura
 * sarebbe pagarle per una domanda che quasi nessuno fa.
 * ⚠️ **Finché non sono pronti si dice che si sta contando**, invece di mostrare uno zero
 * che poi cambia: uno zero che si corregge da solo si legge come un errore.
 */
@Composable
private fun FactsDialog(uris: List<Uri>, fields: List<FactField>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val facts by produceState<Facts?>(null, uris) { value = factsOf(context, uris) }

    AlertDialog(
        onDismissRequest = onDismiss,
        /*
         * ⚠️⚠️ **TITOLO CENTRATO SU UNA RIGA E PASTIGLIA SULLA SUA, dalla `0.73`**, come dal
         * mockup dell'utente, ed è la correzione di un difetto che lui stesso ha chiamato
         * suo: *è stato un mio errore chiederti titolo e nome del file sulla stessa riga,
         * non mi ero reso conto di quanto fosse facile che si sforasse lo spazio*. Sulla
         * stessa riga i due si dividevano la larghezza, quindi un nome un po' lungo faceva
         * a gomitate col titolo; su due righe la pastiglia ha tutta la larghezza del dialogo.
         * ⚠️ **Il titolo è una stringa SUA** (`facts_title`, 'Info dettagliate sul file') e
         * non più `pick_info`: quella è anche l'etichetta sotto un'icona nel riquadro delle
         * azioni, dove tre parole andrebbero a capo tre volte.
         * ⚠️ **Non serve più nessun accorgimento per le lingue da destra a sinistra**: un
         * testo centrato è centrato in tutte, e la riga di prima si specchiava perché era
         * una `Row`. La `Column` non ha un verso da specchiare.
         * ⚠️ Compare **quando i dati arrivano**: prima di allora non si sa ancora il nome, e
         * mettere un segnaposto vorrebbe dire far cambiare la testata due volte.
         */
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.facts_title),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                facts?.one?.name?.let { name -> NamePill(name = name) }
            }
        },
        text = {
            val f = facts
            when {
                f == null -> Text(
                    text = stringResource(R.string.pick_counting),
                    style = MaterialTheme.typography.bodyMedium
                )
                /*
                 * ⚠️ **Il PESO davanti e il conto dietro, dalla 0.63** (richiesta
                 * dell'utente: *'X MB in Y file'*). Era 'X file, Y MB totali' della `0.59`:
                 * la revisione mette avanti il numero che si va a cercare, perché quante
                 * foto si sono scelte lo dice già la barra sopra.
                 * ⚠️ Resta un **plurale** e non una stringa fissa: con un file solo la
                 * forma cambia in diverse lingue, e l'italiano non è quella che lo mostra.
                 */
                f.one == null -> Text(
                    text = pluralStringResource(
                        R.plurals.pick_facts, f.count, f.count, formatBytes(f.bytes)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                else -> FileFacts(facts = f, one = f.one, fields = fields)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pick_close)) }
        }
    )
}

/**
 * I dati di un file, una riga per campo.
 *
 * ⚠️⚠️ **UN CAMPO SENZA DATO NON PRENDE NESSUNA RIGA** (richiesta dell'utente): il `continue`
 * qui sotto è tutta la regola. Niente righe vuote e niente 'n.d.', quindi il dialogo di una
 * foto scaricata dal web è corto e quello di uno scatto col telefono è lungo, e la
 * differenza **è** l'informazione.
 * ⚠️ **Il nome sta in testa e non porta etichetta**: è il titolo di quello che si sta
 * guardando, non un dato fra gli altri, e la parola 'nome' davanti al nome è rumore.
 * ⚠️⚠️ **Dalla 0.68 il nome è in una PASTIGLIA del colore d'accento, in grassetto**
 * (richiesta dell'utente). Prima era un `titleSmall` nudo, che in un elenco di righe si
 * distingueva poco da un dato fra gli altri: la pastiglia lo rende quello che è, cioè il
 * titolo. ⚠️ Prende `primaryContainer` e non `primary` benché in questa tavolozza valgano
 * lo stesso: è il ruolo giusto per una superficie colorata, e se un giorno i due si
 * separassero questa resterebbe corretta.
 * ⚠️ **Il blocco scorre**: dieci righe più un percorso lungo passano l'altezza di un
 * dialogo su uno schermo basso, e senza scorrimento le ultime righe sarebbero irraggiungibili.
 */
@Composable
private fun FileFacts(facts: Facts, one: OneFile, fields: List<FactField>) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (field in fields) {
            // ⚠️ Il nome NON è più una riga di questo elenco: dalla `0.69` sta nella testata
            // del dialogo, accanto alla parola 'Info'. Vedi [NamePill] e [FactsDialog].
            if (field == FactField.NAME) continue
            val value = factValue(field, facts, one) ?: continue
            FactRow(label = stringResource(field.label), value = value)
            /*
             * ⚠️⚠️ **L'ALTITUDINE È UNA VOCE A SÉ MA NON UN CAMPO A SÉ** (richiesta
             * dell'utente: *da scrivere come voce a parte, senza parentesi*). Prima stava
             * fra parentesi in coda alle coordinate, dove si leggeva come una loro
             * precisazione invece che come il terzo numero che è.
             * ⚠️ **Perché non un `FactField` suo**: comparirebbe nell'elenco delle
             * impostazioni come una spunta in più, in una schermata che l'utente ha già
             * chiesto di alleggerire, e sarebbe una spunta che non decide niente da sola.
             * Altitudine e coordinate vengono dallo stesso dato GPS e viaggiano insieme:
             * chi spegne le coordinate spegne anche lei, ed è quello che ci si aspetta.
             */
            if (field == FactField.PLACE) {
                altitudeText(one)?.let {
                    FactRow(label = stringResource(R.string.facts_altitude_label), value = it)
                }
            }
        }
    }
}

/**
 * Il nome del file in una pastiglia del colore d'accento, in grassetto, larga quanto il
 * dialogo e alta quanto serve fino a [NAME_LINES] righe.
 *
 * ⚠️ Prende `primaryContainer` e non `primary` benché in questa tavolozza valgano lo stesso:
 * è il ruolo giusto per una superficie colorata, e se un giorno i due si separassero questa
 * resterebbe corretta. Il contrasto del testo sopra l'accento è misurato in `Theme.kt`: 5.19.
 *
 * ⚠️⚠️ **CRESCE IN VERTICALE FINO A TRE RIGHE E POI ACCORCIA, e le tre regole dell'utente
 * sono tutte e tre necessarie** (2026-08-31: *massimo 3 righe, che per un nome file mi
 * sembrano già un'enormità; se sfora ancora, metti un'ellissi finale nel nome, poi riporta
 * comunque l'estensione; non spezzare mai l'estensione andando a capo*).
 * ⚠️⚠️ **`TextOverflow.Ellipsis` NON serviva e faceva il danno esatto che si voleva
 * evitare**: mette i tre punti alla **fine**, cioè mangia proprio l'estensione, che è la
 * parte che l'utente ha chiesto di salvare sempre. Serve un'ellissi **in mezzo**, che
 * Compose non ha: da qui la misura in [fitName].
 */
@Composable
private fun NamePill(name: String, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NAME_CORNER),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        // ⚠️ `BoxWithConstraints` e non una misura in dp: la larghezza utile dipende dal
        // dialogo, che dipende dallo schermo, e una misura scritta a mano sarebbe giusta su
        // un telefono solo.
        BoxWithConstraints(
            modifier = Modifier.padding(horizontal = NAME_PAD_SIDE, vertical = NAME_PAD_TOP)
        ) {
            val measurer = rememberTextMeasurer()
            val room = with(LocalDensity.current) { maxWidth.roundToPx() }
            val shown = remember(name, room, style, measurer) {
                fitName(name, room, style, measurer)
            }
            Text(
                text = shown,
                style = style,
                maxLines = NAME_LINES,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Il nome accorciato quanto basta a starci in [NAME_LINES] righe, con l'estensione salva.
 *
 * ⚠️⚠️ **SI MISURA, non si conta**: un tetto di caratteri sarebbe sbagliato due volte, perché
 * le lettere non hanno tutte la stessa larghezza (`WWW` occupa il triplo di `iii`) e perché
 * la larghezza utile cambia col dialogo. Qui si chiede al misuratore se il testo sfora, che è
 * la stessa domanda che si farà il layout.
 * ⚠️ **Ricerca binaria e non un ciclo che toglie una lettera per volta**: su un nome di
 * duecento caratteri sarebbero duecento misure a ogni composizione. Così sono otto.
 * ⚠️⚠️ **Il GIUNTORE DI PAROLE (`U+2060`) dentro l'estensione è l'unico modo di rispettare
 * il 'mai'**: un nome di file non ha spazi, quindi il layout può andare a capo fra due
 * caratteri qualunque, e `.HEIC` finirebbe a cavallo di due righe come `.HE` più `IC`.
 * Quel carattere è invisibile e dice al layout 'qui non si rompe'. Scritto per codepoint,
 * come vuole la regola del repo sui caratteri invisibili.
 */
private fun fitName(name: String, room: Int, style: TextStyle, measurer: TextMeasurer): String {
    fun sta(testo: String) = room <= 0 || !measurer.measure(
        text = AnnotatedString(testo),
        style = style,
        maxLines = NAME_LINES,
        constraints = Constraints(maxWidth = room)
    ).hasVisualOverflow

    val punto = name.lastIndexOf('.')
    // ⚠️ `punto > 0` e non `>= 0`: un nome che comincia col punto è un file nascosto, e là
    // quel punto non introduce un'estensione, fa parte del nome.
    val coda = if (punto > 0) glue(name.substring(punto)) else ""
    val intero = if (punto > 0) name.substring(0, punto) + coda else name
    if (sta(intero)) return intero

    val corpo = if (punto > 0) name.substring(0, punto) else name
    var basso = 0
    var alto = corpo.length
    while (basso < alto) {
        val mezzo = (basso + alto + 1) / 2
        if (sta(corpo.take(mezzo) + ELLIPSIS + coda)) basso = mezzo else alto = mezzo - 1
    }
    return corpo.take(basso) + ELLIPSIS + coda
}

/** L'estensione con un giuntore fra ogni carattere, così il layout non la spezza. */
private fun glue(ext: String): String = ext.toCharArray().joinToString(WORD_JOINER)

/**
 * Una riga di dati: etichetta a sinistra, valore a destra.
 *
 * ⚠️⚠️ **[Modifier.alignByBaseline] SU TUTTI E DUE, ed è la correzione di un difetto che si
 * vedeva** (riscontro dell'utente: *sono sfalsati, le linee di base non combaciano*).
 * L'etichetta è `bodySmall` e il valore `bodyMedium`, cioè 12sp contro 14sp con interlinee
 * diverse: allineati in alto come erano, i due riquadri cominciavano insieme e le lettere
 * no. La linea di base è l'unica cosa che si vede davvero allineata, perché è quella su cui
 * poggiano i caratteri.
 * ⚠️ Su un valore di **due righe** si allinea la prima, che è quella accanto all'etichetta:
 * è esattamente quello che serve adesso che i megapixel e gli ISO vanno a capo.
 */
@Composable
private fun FactRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(LABEL_SHARE).alignByBaseline()
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f - LABEL_SHARE).alignByBaseline()
        )
    }
}

/** Quanta parte della riga tiene l'etichetta: meno della metà, perché il dato conta più di lei. */
private const val LABEL_SHARE = 0.42f

/** Il raggio della pastiglia del nome: abbastanza da leggersi come una targhetta. */
private val NAME_CORNER = 8.dp

/** Quanto respira il nome dentro la sua pastiglia, ai lati e sopra e sotto. */
private val NAME_PAD_SIDE = 10.dp
private val NAME_PAD_TOP = 4.dp

/**
 * Quante righe può prendere il nome nella pastiglia.
 *
 * ⚠️ Tre, scelta dell'utente: *che per un nome file mi sembrano già un'enormità, ma non
 * oltre*. Alla larghezza di un dialogo su un telefono sono circa novanta caratteri, cioè
 * più del doppio del nome più lungo che una fotocamera produce.
 */
private const val NAME_LINES = 3

/** I tre punti, che nel repo si scrivono così e non col carattere unico. */
private const val ELLIPSIS = "..."

/**
 * `U+2060 WORD JOINER`: invisibile, e vieta al layout di andare a capo dove sta.
 *
 * ⚠️ Scritto per **codepoint** e non incollato, come vuole la regola del repo sui caratteri
 * invisibili: incollato, questo file conterrebbe un carattere che a schermo non si vede e
 * che nessuno saprebbe di aver toccato.
 * ⚠️ **Non è lo spazio insecabile** (`U+00A0`), che è vietato: quello è uno spazio e si
 * vede, questo non occupa larghezza e serve solo a legare due caratteri.
 */
private const val WORD_JOINER = "\u2060"

/**
 * Il valore di un campo, già scritto come si legge, oppure `null` se il file non ce l'ha.
 *
 * ⚠️⚠️ **LA FORMATTAZIONE STA QUI E NON NELLA LETTURA DEI DATI, e non è pignoleria**: qui
 * si è dentro la composizione, quindi si possono leggere le stringhe tradotte e la lingua
 * del telefono. Un `Double` trasformato in testo dentro `factsOf` sarebbe un dato che non si
 * può più formattare bene, e le date e i separatori decimali cambiano con la lingua.
 */
@Composable
private fun factValue(field: FactField, facts: Facts, one: OneFile): String? = when (field) {
    FactField.NAME -> one.name

    // ⚠️ Si mostra la CODA del tipo MIME (`image/jpeg` -> `JPEG`): la prima metà è
    // 'image' su ogni fotografia, quindi è una parola che non distingue niente.
    // ⚠️ **Maiuscolo INVARIANTE e non della lingua del telefono**: un tipo MIME è ASCII e
    // non è una parola, e la regola turca della `i` cambierebbe `image/tiff` in `TİFF`.
    FactField.KIND -> one.mime?.substringAfterLast('/')?.uppercase()

    FactField.PIXELS -> pixelsText(one)
    FactField.SIZE -> formatBytes(facts.bytes)
    FactField.TAKEN -> one.taken?.let { moment(it) }
    FactField.MODIFIED -> one.modified?.let { moment(it) }
    FactField.FOLDER -> one.folder
    FactField.ENCODING -> encodingText(one.encoding)
    FactField.CAMERA -> cameraText(one)
    FactField.PLACE -> placeText(one)
}

/**
 * Le misure, coi megapixel quando ce ne sono abbastanza per dire qualcosa.
 *
 * ⚠️ **Sotto un decimo di megapixel il numero non si scrive**: su un'icona da 48x48 direbbe
 * '0,0 Mpx', che è una riga in più per dire meno di quello che le due misure già dicono.
 */
@Composable
private fun pixelsText(one: OneFile): String? {
    val w = one.width ?: return null
    val h = one.height ?: return null
    val mpx = w.toLong() * h / 1_000_000.0
    return if (mpx >= 0.1) stringResource(R.string.facts_pixels_mpx, w, h, neat(mpx))
    else stringResource(R.string.facts_pixels_value, w, h)
}

/**
 * La codifica, dai byte del file. Vedi [Encoding], che spiega perché non dall'EXIF.
 *
 * ⚠️ **La parola dell'ordine di scansione dipende dal formato**: un JPEG è 'progressivo',
 * un PNG 'interlacciato'. È la stessa idea con due nomi, e usarne uno per tutti e due
 * suonerebbe sbagliato a chi conosce il formato.
 */
@Composable
private fun encodingText(encoding: Encoding?): String? {
    if (encoding == null) return null
    val parts = ArrayList<String>(4)
    parts += encoding.codec
    encoding.bits?.let { parts += pluralStringResource(R.plurals.facts_bits, it, it) }
    encoding.chroma?.let { parts += it }
    if (encoding.interlaced) {
        parts += stringResource(
            if (encoding.codec == "PNG") R.string.facts_interlaced
            else R.string.facts_progressive
        )
    }
    return parts.joinToString(", ")
}

/**
 * Obiettivo ed esposizione, i tre dati che l'utente ha chiesto insieme.
 *
 * ⚠️ **Su una voce sola e non su tre**: focale, tempo e ISO sono i parametri di **uno**
 * scatto, e chi li guarda li guarda insieme. Tre voci con un numero ciascuna
 * allungherebbero il dialogo di tre volte per la stessa informazione.
 * ⚠️⚠️ **Ma dalla 0.68 gli ISO vanno a capo** (richiesta dell'utente): la voce resta una,
 * e sono le due **righe** a separare l'obiettivo dalla sensibilità. Focale e tempo
 * descrivono la luce che entra, gli ISO quanto la si amplifica dopo: sono due cose, e su
 * una riga sola di tre numeri separati da virgole non si vedeva.
 * ⚠️ **Il tempo si scrive come una frazione quando è meno di un secondo** (`1/120 s`), che
 * è come lo scrivono le fotocamere e chi fotografa: `0,008 s` è lo stesso numero e non lo
 * legge nessuno.
 */
@Composable
private fun cameraText(one: OneFile): String? {
    val lens = ArrayList<String>(2)
    one.focalMm?.let { lens += stringResource(R.string.facts_focal, neat(it)) }
    one.exposureSec?.let { seconds ->
        lens += if (seconds < 1.0) {
            stringResource(R.string.facts_exposure_fraction, (1.0 / seconds).roundToInt())
        } else {
            stringResource(R.string.facts_exposure_seconds, neat(seconds))
        }
    }
    val iso = one.iso?.let { stringResource(R.string.facts_iso, it) }
    val head = lens.joinToString(", ")
    // ⚠️ L'a capo esiste solo quando ci sono tutte e due le metà: una riga vuota sopra o
    // sotto sarebbe uno spazio che dice di aver perso un dato che non c'era.
    return when {
        head.isEmpty() -> iso
        iso == null -> head
        else -> "$head\n$iso"
    }
}

/**
 * Dove è stata scattata: le sole coordinate.
 *
 * ⚠️⚠️ **L'ALTITUDINE È USCITA DA QUI NELLA 0.68**, e adesso la scrive [altitudeText] su
 * una voce sua (richiesta dell'utente: *da scrivere come voce a parte, senza parentesi*).
 * Fra parentesi in coda alle coordinate si leggeva come una loro precisazione invece che
 * come il terzo numero che è.
 * ⚠️⚠️ **LE COORDINATE SI SCRIVONO COL PUNTO, sempre, anche in italiano**, ed è l'unica
 * deroga alla lingua del telefono in tutta questa schermata: sono un numero da **incollare
 * in una mappa**, e nessuna mappa accetta la virgola decimale, perché la virgola è già il
 * separatore fra i due valori. Scriverle come le scrive la lingua le renderebbe illeggibili
 * proprio a chi le usa.
 * ⚠️ **Sei decimali**: sono circa undici centimetri all'equatore, cioè più della precisione
 * di qualunque GPS di telefono. Con quattro si perderebbe la casa giusta.
 */
@Composable
private fun placeText(one: OneFile): String? {
    val lat = one.latitude ?: return null
    val lon = one.longitude ?: return null
    return String.format(Locale.US, "%.6f, %.6f", lat, lon)
}

/**
 * L'altitudine, nella forma che l'utente ha chiesto: `X m s.l.m.`
 *
 * ⚠️ **Si mostra solo insieme alle coordinate**, e la condizione sta in chi la chiama: un
 * file con l'altitudine e senza latitudine non esiste in pratica, ma se esistesse
 * mostrarla da sola sarebbe un dato senza il suo posto.
 * ⚠️ **Arrotondata al metro**: l'altitudine di un GPS di telefono sbaglia di parecchi
 * metri, e scriverne i decimali sarebbe dichiarare una precisione che non c'è.
 */
@Composable
private fun altitudeText(one: OneFile): String? {
    val high = one.altitudeM ?: return null
    return stringResource(R.string.facts_altitude, high.roundToInt())
}

/**
 * Un numero con un decimale, e senza lo zero inutile.
 *
 * ⚠️ `30,0 s` e `26,0 mm` sono il modo di scrivere un numero intero che si legge come una
 * misura di precisione che non c'è: quando il decimale è zero si toglie.
 */
private fun neat(value: Double): String =
    if (value == floor(value) && abs(value) < 1e9) value.toLong().toString()
    else String.format(Locale.getDefault(), "%.1f", value)

/**
 * Un istante come lo scrive la lingua del telefono.
 *
 * ⚠️ **Data media e ora breve**: la data per esteso ('30 agosto 2026') su una riga
 * affiancata a un'etichetta va a capo, e i secondi non servono a nessuno per sapere quando
 * è stata scattata una fotografia.
 * ⚠️ **Nessun formato scritto a mano** (`dd/MM/yyyy`): il giorno prima del mese è vero in
 * italiano e falso in inglese, e con quindici lingue in arrivo un formato fisso sarebbe
 * sbagliato in tredici.
 */
private fun moment(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

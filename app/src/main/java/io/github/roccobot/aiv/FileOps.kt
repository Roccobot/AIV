package io.github.roccobot.aiv

import android.content.res.Resources
import android.net.Uri
import androidx.annotation.PluralsRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
            onDismiss = onClose,
            onConfirm = {
                onClose()
                onRun(FileKind.DELETE) { FileTree.delete(context, job.uris) }
            }
        )

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
    class Delete(override val uris: List<Uri>) : FileJob
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
    DELETE(R.plurals.delete_done, gone = true)
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
 * La conferma dell'eliminazione.
 *
 * ⚠️⚠️ **NON È CORTESIA: qui non c'è un cestino.** Il MediaStore ne ha uno, ma ci si
 * finisce solo passando dal provider con la richiesta apposita, e questa app cancella dal
 * disco perché è l'unica via che copre anche i file che nella galleria non ci sono mai
 * entrati. Quindi il gesto è definitivo, e il testo lo dice invece di lasciarlo intuire.
 * ⚠️ Il conto sta nel TITOLO e non nel corpo: è il dato che fa cambiare idea, e chi tocca
 * in fretta legge solo la prima riga.
 */
@Composable
private fun DeleteDialog(count: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(pluralStringResource(R.plurals.delete_ask, count, count)) },
        text = { Text(stringResource(R.string.delete_desc)) },
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
        title = { Text(stringResource(R.string.pick_info)) },
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
            val value = factValue(field, facts, one) ?: continue
            if (field == FactField.NAME) {
                Text(text = value, style = MaterialTheme.typography.titleSmall)
                continue
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(field.label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(LABEL_SHARE)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f - LABEL_SHARE)
                )
            }
        }
    }
}

/** Quanta parte della riga tiene l'etichetta: meno della metà, perché il dato conta più di lei. */
private const val LABEL_SHARE = 0.42f

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
 * ⚠️ **Su una riga sola e non su tre**: focale, tempo e ISO sono i parametri di **uno**
 * scatto, e chi li guarda li guarda insieme. Tre righe con un numero ciascuna
 * allungherebbero il dialogo di tre volte per la stessa informazione.
 * ⚠️ **Il tempo si scrive come una frazione quando è meno di un secondo** (`1/120 s`), che
 * è come lo scrivono le fotocamere e chi fotografa: `0,008 s` è lo stesso numero e non lo
 * legge nessuno.
 */
@Composable
private fun cameraText(one: OneFile): String? {
    val parts = ArrayList<String>(3)
    one.focalMm?.let { parts += stringResource(R.string.facts_focal, neat(it)) }
    one.exposureSec?.let { seconds ->
        parts += if (seconds < 1.0) {
            stringResource(R.string.facts_exposure_fraction, (1.0 / seconds).roundToInt())
        } else {
            stringResource(R.string.facts_exposure_seconds, neat(seconds))
        }
    }
    one.iso?.let { parts += stringResource(R.string.facts_iso, it) }
    return parts.joinToString(", ").ifEmpty { null }
}

/**
 * Dove è stata scattata: coordinate e, quando c'è, altitudine.
 *
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
    val core = String.format(Locale.US, "%.6f, %.6f", lat, lon)
    val high = one.altitudeM ?: return core
    return core + " (" + stringResource(R.string.facts_altitude, high.roundToInt()) + ")"
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

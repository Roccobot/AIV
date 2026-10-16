package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * La copertina scelta a mano per una cartella, e perché è una copia e non un indirizzo.
 *
 * ⚠️⚠️ **La copertina deve restare anche se l'originale è cancellato** (condizione dell'utente
 * alla richiesta, 1.94): con l'indirizzo, cancellando o spostando l'immagine la cartella
 * tornerebbe alla copertina predefinita. Quindi l'immagine si copia nella cartella dell'app.
 *
 * ⚠️ **Il file è l'archivio**: nessuna preferenza da tenere allineata. Una chiave 'questa cartella
 * ha una copertina' accanto al file sarebbe una seconda fonte della stessa verità, e le due
 * potrebbero divergere. La domanda si risponde guardando se il file esiste.
 *
 * ⚠️ **È in `filesDir` e non in `cacheDir`**: la cache il sistema la può svuotare, e la
 * copertina sparirebbe da sola. Per lo stesso motivo 'Svuota la cache delle miniature' non la
 * tocca.
 *
 * La chiave è il `BUCKET_ID`, come per [FolderTints]. Come là, una cartella cancellata non si
 * rincorre (istruzione dell'utente): la copia pesa pochi kilobyte, e una cartella ricreata nello
 * stesso posto la ritrova.
 */
object FolderCovers {

    /**
     * Le copertine scelte, per identificatore di cartella.
     *
     * Si legge in blocco come [FolderTints.all]: una lettura di cartella e non un giro sul disco
     * per cella.
     * A parità di cartella vince la più recente: due file per la stessa cartella non dovrebbero
     * esserci (li toglie [sweep]), ma se una cancellazione fallisce vince l'ultima scelta.
     */
    suspend fun all(context: Context): Map<Long, Uri> = withContext(Dispatchers.IO) {
        home(context)?.listFiles().orEmpty()
            .mapNotNull { file -> owner(file)?.let { it to file } }
            .sortedBy { (_, file) -> file.lastModified() }
            .associate { (dove, file) -> dove to file.toUri() }
    }

    /** La copertina scelta per una cartella, o `null` se non ne ha una. */
    suspend fun of(context: Context, bucket: Long): Uri? = all(context)[bucket]

    /**
     * Copia [source] come copertina di [bucket], e torna il suo indirizzo o `null` se non è
     * riuscita.
     *
     * La copia è ridotta ([COVER_PX]): una copertina si vede al massimo a [Thumbs.PX] pixel di
     * lato, e i byte originali occuperebbero megabyte per sempre. La copertina è quindi una
     * riduzione dell'immagine, non l'immagine.
     *
     * ⚠️ **Il nome contiene l'istante**: Coil indirizza la sua cache col percorso, e riscrivendo
     * lo stesso percorso la griglia mostrerebbe la copertina di prima. Un nome nuovo è un
     * indirizzo nuovo.
     *
     * La vecchia si cancella dopo che la nuova è scritta: una scrittura fallita non deve lasciare
     * la cartella senza copertina.
     */
    suspend fun set(context: Context, bucket: Long, source: Uri): Uri? = withContext(Dispatchers.IO) {
        val casa = home(context) ?: return@withContext null
        val ridotta = shrink(context, source) ?: return@withContext null
        val modo = squeeze(ridotta)
        val file = File(casa, "$bucket-${System.currentTimeMillis()}.${modo.suffix}")
        val scritta = runCatching {
            file.outputStream().use { ridotta.compress(modo.format, COVER_QUALITY, it) }
        }.getOrDefault(false)
        if (!scritta) {
            file.delete()
            return@withContext null
        }
        sweep(casa, bucket, keep = file)
        file.toUri()
    }

    /** Toglie la copertina scelta di [bucket]: la cartella torna alla sua immagine più recente. */
    suspend fun clear(context: Context, bucket: Long) = withContext(Dispatchers.IO) {
        home(context)?.let { sweep(it, bucket, keep = null) }
        Unit
    }

    /**
     * Sposta la copertina di [da] su [a]: la cartella è la stessa, rinominata.
     *
     * ⚠️ Il `BUCKET_ID` lo ricava il MediaStore dal percorso, quindi una rinomina lo cambia.
     * Rinominando da AIV si conoscono tutti e due gli identificatori, e il file si rinomina con
     * la cartella; una rinomina fatta da fuori la gestisce [sweep] col periodo di grazia.
     * Il file si rinomina e non si riscrive, per non ricomprimerlo. Il nome nuovo contiene un
     * istante nuovo, per la cache di Coil come in [set].
     */
    suspend fun move(context: Context, da: Long, a: Long) = withContext(Dispatchers.IO) {
        if (da == a) return@withContext
        val casa = home(context) ?: return@withContext
        val quale = casa.listFiles().orEmpty().firstOrNull { owner(it) == da } ?: return@withContext
        val suffisso = quale.name.substringAfterLast('.', "")
        val nato = File(casa, "$a-${System.currentTimeMillis()}.$suffisso")
        if (runCatching { quale.renameTo(nato) }.getOrDefault(false)) sweep(casa, a, keep = nato)
    }

    /**
     * Cancella le copertine delle cartelle che non esistono più, con un periodo di grazia.
     *
     * Serve perché le rinomine fatte da altre app lasciano copertine orfane: la cartella nuova
     * non ha niente che la leghi alla vecchia.
     *
     * ⚠️ **Non gira in sottofondo** (risposta `d-cestino-chiusa` dell'utente: niente fuori
     * dall'app in primo piano): la potatura si fa quando la schermata iniziale legge le cartelle,
     * l'unico momento in cui l'elenco vero dei bucket è disponibile.
     *
     * ⚠️ **Non cancella subito, perché un'assenza non è una cancellazione**: una scheda SD
     * smontata, un archivio non ancora indicizzato o un permesso revocato fanno sparire cartelle
     * vere per un po'. Una copertina vista si data, e si cancella solo se resta assente per più
     * di [COVER_GRACE_MS]. La data di modifica del file è l'archivio, senza una tabella a parte.
     * ⚠️ **Con l'elenco vuoto non fa niente**: è quello che si legge senza permesso o prima che il
     * MediaStore risponda, e tutte le copertine sembrerebbero orfane.
     */
    suspend fun sweep(context: Context, alive: Set<Long>) = withContext(Dispatchers.IO) {
        if (alive.isEmpty()) return@withContext
        val casa = home(context) ?: return@withContext
        val ora = System.currentTimeMillis()
        casa.listFiles().orEmpty().forEach { file ->
            val di = owner(file) ?: return@forEach
            when {
                di in alive -> runCatching { file.setLastModified(ora) }
                ora - file.lastModified() > COVER_GRACE_MS -> runCatching { file.delete() }
            }
        }
    }

    /**
     * I file delle copertine scelte, per il file di impostazioni.
     *
     * I file e non gli indirizzi: l'esportazione ne copia i byte e il nome, e il nome contiene la
     * cartella e l'istante, quindi all'importazione basta rimetterli.
     */
    internal suspend fun files(context: Context): List<File> = withContext(Dispatchers.IO) {
        home(context)?.listFiles().orEmpty().filter { it.isFile && named(it.name) }
    }

    /**
     * Se questo è il nome di una copertina, cioè `<cartella>-<istante>.<suffisso>` con un
     * suffisso che questo oggetto scrive.
     *
     * Serve all'importazione, che riceve nomi da un file: un nome illeggibile [owner] lo
     * scarterebbe, e resterebbe un file che nessuno vede e nessuno pota.
     */
    internal fun named(name: String): Boolean {
        val suffisso = name.substringAfterLast('.', "")
        if (suffisso !in SUFFIXES) return false
        val stem = name.substringBeforeLast('.')
        return stem.substringBeforeLast('-', "").toLongOrNull() != null &&
            stem.substringAfterLast('-', "").toLongOrNull() != null
    }

    /**
     * Aggiunge alle copertine di adesso quelle del file di impostazioni: per le cartelle che il
     * file contiene vince la sua, le altre restano come sono (risposta `fonde` dell'utente a
     * `d-backup-importa`, 2.95).
     *
     * ⚠️ **Prima entrano le nuove, poi escono le vecchie delle loro cartelle**: una scrittura che
     * fallisce a metà lascia qualche copertina in più, mai nessuna. A parità di cartella vince la
     * più recente ([all]), e le nuove sono appena scritte.
     * Il periodo di grazia riparte da adesso, perché un file copiato ha la data di oggi: una
     * copertina importata su un telefono che non ha ancora quella cartella ha un mese per vederla
     * comparire.
     *
     * @return se sono entrate tutte.
     */
    internal suspend fun adopt(context: Context, incoming: List<File>): Boolean =
        withContext(Dispatchers.IO) {
            val casa = home(context) ?: return@withContext false
            val arrivati = HashSet<String>()
            var tutte = true
            for (file in incoming) {
                if (!named(file.name)) continue
                val dove = File(casa, file.name)
                val fatto = runCatching {
                    file.inputStream().use { input -> dove.outputStream().use { input.copyTo(it) } }
                    true
                }.getOrDefault(false)
                if (fatto) arrivati += file.name else tutte = false
            }
            // Escono le sole vecchie che una nuova ha già sostituito: le altre cartelle, e quelle
            // con una copia fallita, tengono la loro.
            val coperte = arrivati.mapNotNull { owner(File(it)) }.toSet()
            casa.listFiles().orEmpty()
                .filter { it.isFile && it.name !in arrivati && owner(it) in coperte }
                .forEach { runCatching { it.delete() } }
            tutte
        }

    /**
     * La cartella delle copertine, creata alla prima scrittura.
     *
     * Risponde `null` quando `mkdirs` fallisce (spazio esaurito, archivio smontato): una
     * copertina che non si scrive si dice con un avviso, senza un errore che chiude la schermata.
     */
    private fun home(context: Context): File? =
        File(context.filesDir, COVER_DIR).takeIf { it.isDirectory || it.mkdirs() }

    /** Di quale cartella è la copertina questo file, letto dal suo nome. */
    private fun owner(file: File): Long? =
        file.name.substringBeforeLast('.').substringBeforeLast('-').toLongOrNull()

    /** Cancella le copie vecchie di [bucket], tenendo [keep] se c'è. */
    private fun sweep(casa: File, bucket: Long, keep: File?) {
        casa.listFiles().orEmpty()
            .filter { owner(it) == bucket && it != keep }
            .forEach { it.delete() }
    }

    /**
     * L'immagine scelta, decodificata e rimpicciolita a [COVER_PX].
     *
     * `ImageDecoder` e non `BitmapFactory`: applica da sé l'orientamento EXIF (una foto in
     * verticale non diventa una copertina coricata) e legge HEIF e AVIF. Di un'immagine animata
     * prende il primo fotogramma.
     * ⚠️ **L'allocatore è software**: di serie `decodeBitmap` può dare un bitmap in memoria
     * grafica, che non si può comprimere.
     * Il campionamento è a potenze di due: `setTargetSize` deformerebbe l'immagine se le
     * proporzioni non tornano, e qui il lato finale cade fra [COVER_PX] e il suo doppio.
     */
    private fun shrink(context: Context, source: Uri): Bitmap? = runCatching {
        val src = ImageDecoder.createSource(context.contentResolver, source)
        ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val lato = max(info.size.width, info.size.height)
            var passo = 1
            while (lato / (passo * 2) >= COVER_PX) passo *= 2
            decoder.setTargetSampleSize(passo)
        }
    }.getOrNull()

    /**
     * Con che formato si scrive questa copertina.
     *
     * WebP da Android 11 in su: tiene la trasparenza e pesa come un JPEG. Sotto, la costante
     * `WEBP_LOSSY` non esiste, e la sola alternativa con l'alfa è il PNG, che su una foto pesa
     * dieci volte tanto: si sceglie secondo quello che l'immagine ha.
     * Decide `hasAlpha` e non il tipo del file di partenza: un PNG può essere opaco.
     */
    private fun squeeze(bitmap: Bitmap): Squeeze = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
            Squeeze(Bitmap.CompressFormat.WEBP_LOSSY, "webp")
        bitmap.hasAlpha() -> Squeeze(Bitmap.CompressFormat.PNG, "png")
        else -> Squeeze(Bitmap.CompressFormat.JPEG, "jpg")
    }

    /** Un formato di scrittura col suffisso che gli tocca. */
    private class Squeeze(val format: Bitmap.CompressFormat, val suffix: String)

    /** I suffissi che [squeeze] scrive, cioè quelli di una copertina vera. */
    private val SUFFIXES = setOf("webp", "png", "jpg")
}

/**
 * La fascia che dice che si sta scegliendo una copertina, e offre di lasciar perdere.
 *
 * ⚠️ **Non passa da [Notices]**: un avviso dice che una cosa è successa e se ne va da sé, questa
 * dice che cosa sta succedendo e resta finché la modalità è attiva.
 * La superficie è quella dell'avviso, bordo d'accento compreso, perché le due righe sembrino
 * della stessa app.
 * Il nome della cartella va fra apici, come nelle finestre del nascondere e del mostrare: è un
 * dato dentro una frase.
 */
@Composable
fun CoverInvite(name: String, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        // Il rientro di sistema lo aggiunge da sé, come l'avviso: è nel `Box` di radice, che
        // arriva al bordo dello schermo.
        modifier = modifier
            .navigationBarsPadding()
            .padding(INVITE_EDGE)
    ) {
        Snackbar(
            modifier = Modifier
                .semantics { liveRegion = LiveRegionMode.Polite }
                .edged(INVITE_ROUND),
            shape = RoundedCornerShape(INVITE_ROUND),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            action = {
                TextButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.textButtonColors(contentColor = accentInk())
                ) { Text(stringResource(R.string.cancel)) }
            }
        ) { Text(stringResource(R.string.folder_cover_pick, name)) }
    }
}

/**
 * Con quale immagine si mostra questa cartella: quella scelta a mano se c'è, altrimenti la sua
 * più recente.
 *
 * La scelta a mano vince: la copertina predefinita cambia a ogni immagine aggiunta, e con la
 * precedenza a lei la scelta durerebbe fino al prossimo scatto.
 */
fun Folder.Bucket.coverIn(covers: Map<Long, Uri>): Uri? = covers[id] ?: cover

/**
 * Se la voce 'Copertina predefinita' compare nel menu del FAB: no.
 *
 * ⚠️ **Spenta e non tolta** (voce `copertina-togli` dell'utente, giro della 1.94): un tocco
 * involontario ripristinava troppo facilmente la copertina predefinita, e una scelta persa non
 * si ritrova. Al suo posto c'è il gesto sull'icona dell'intestazione, che vuole due tocchi.
 * La voce intera, testo e posizione compresi, torna con `true`: chi la riaccende rilegga quella
 * voce, perché il motivo del no vale ancora. Per questo la stringa resta tradotta.
 */
internal const val COVER_MENU_ROW = false

/** La cartella delle copertine scelte, dentro `filesDir`. */
private const val COVER_DIR = "covers"

/** L'aria intorno alla fascia dell'invito, e lo stondamento: quelli dell'avviso. */
private val INVITE_EDGE = 12.dp
private val INVITE_ROUND = 14.dp

/**
 * Il lato massimo di una copertina copiata in casa.
 *
 * Il doppio di [Thumbs.PX], la misura delle miniature: margine per uno schermo che ne chieda di
 * più, senza occupare spazio per pixel che nessuno guarda.
 */
private const val COVER_PX = 1024

/**
 * Quanto si stringe una copertina.
 *
 * Novanta e non cento: a questa misura la differenza non si vede, e il file pesa la metà.
 */
private const val COVER_QUALITY = 90

/**
 * Quanto una copertina resta dopo che la sua cartella è sparita: un mese.
 *
 * Un mese e non un giorno: l'attesa protegge una cartella che c'è ma non si vede (una scheda SD
 * smontata, un archivio esterno staccato, un permesso revocato e ridato), e quelle assenze durano
 * quanto un viaggio. Sbagliare per eccesso costa qualche decina di kilobyte, per difetto una
 * scelta persa. Non un anno: tenerlo così a lungo vorrebbe dire tenerlo per sempre.
 */
private const val COVER_GRACE_MS = 30L * 24 * 60 * 60 * 1000

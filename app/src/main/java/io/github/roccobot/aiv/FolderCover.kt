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
 * La copertina **scelta a mano** per una cartella, e perché è una copia e non un indirizzo.
 *
 * ⚠️⚠️ **NASCE NELLA `1.94` DA UNA SUA RICHIESTA, CON LA SUA CONDIZIONE ATTACCATA** (*immagine
 * personalizzata per le cartelle*, con la clausola *solo se si può fare in modo che resti la
 * stessa anche dopo l'eventuale eliminazione dell'originale*). Quella clausola decide tutto il
 * resto: tenere l'indirizzo dell'immagine scelta sarebbe costato una riga, e il giorno che
 * quell'immagine viene cancellata o spostata la cartella tornerebbe alla copertina automatica
 * senza che nessuno abbia toccato niente. Quindi l'immagine si **copia in casa dell'app**, e da
 * quel momento la copertina non dipende più da dove è nata.
 *
 * ⚠️⚠️ **IL FILE È L'ARCHIVIO: non c'è nessuna preferenza da tenere allineata.** Una chiave che
 * dicesse 'questa cartella ha una copertina' e un file che la porta sarebbero due sorgenti della
 * stessa verità, e basta un'installazione ripulita a metà per farle divergere: un archivio che
 * promette un file che non c'è dà una cella vuota, e un file senza archivio è spazio occupato che
 * nessuno legge più. Qui la domanda *questa cartella ha una copertina?* si risponde guardando se
 * il file esiste.
 *
 * ⚠️⚠️ **VIVE IN `filesDir` E NON IN `cacheDir`, E LA DIFFERENZA È LA PROMESSA**: quello che sta
 * nella cache il sistema lo può buttare quando ha bisogno di spazio, e la copertina scelta
 * sparirebbe da sola. È lo stesso motivo per cui 'Elimina le miniature memorizzate' non la
 * tocca: quel comando svuota la cache di Coil, che è un'altra cosa.
 *
 * ⚠️ **La chiave è il `BUCKET_ID` come per [FolderTints]**, e vale la stessa nota: due cartelle
 * si possono chiamare uguale in due posti diversi, e una rinomina non deve portare via la
 * scelta. E come là, una cartella cancellata non si rincorre (sua istruzione del 2026-09-08): la
 * copia resta dov'è, pesa una manciata di kilobyte, e una cartella ricreata nello stesso posto se
 * la ritrova.
 */
object FolderCovers {

    /**
     * Le copertine scelte, per identificatore di cartella.
     *
     * ⚠️ **Si legge in blocco come [FolderTints.all]**, e per la stessa ragione: nella schermata
     * iniziale le celle sono decine, e chiedere una per una vorrebbe dire un giro sul disco per
     * cella. Qui costa una lettura di cartella, che di voci ne ha quante sono le copertine
     * scelte.
     * ⚠️ **A parità di cartella vince la più recente**: due file per lo stesso identificatore
     * non ci dovrebbero essere (li toglie [sweep]), ma se una cancellazione non riesce l'esito
     * deve restare quello che l'utente ha appena scelto, non uno a caso.
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
     * ⚠️⚠️ **LA COPIA È RIDOTTA E NON I BYTE ORIGINALI, ed è una scelta misurata**: una
     * copertina si vede al massimo a [Thumbs.PX] pixel di lato, quindi tenere in casa una
     * fotografia da dodici megapixel vorrebbe dire occupare per sempre qualche megabyte per
     * mostrarne cinquecento pixel. [COVER_PX] è il doppio di quella misura, cioè il margine che
     * serve se un domani le miniature si chiedessero più grandi.
     * ⚠️ **Quindi la copertina non è più *quell'immagine*, ed è giusto dirlo**: è una sua
     * riduzione, e chi la guarda a tutto schermo non la trova là dentro. La cosa che lui ha
     * chiesto è che resti, e resta.
     *
     * ⚠️⚠️ **IL NOME PORTA L'ISTANTE, E NON È UNA DECORAZIONE**: Coil indirizza la sua cache con
     * l'indirizzo dell'immagine, quindi riscrivendo lo stesso percorso la griglia continuerebbe a
     * mostrare la copertina di prima finché la cache non scade. Un nome nuovo è un indirizzo
     * nuovo, e nessuna chiave di cache scritta a mano deve restare allineata a niente.
     *
     * ⚠️ **La vecchia si cancella DOPO che la nuova è scritta**: al contrario, una scrittura che
     * fallisce lascerebbe la cartella senza copertina, cioè farebbe perdere una scelta per
     * colpa di un tentativo.
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
     * Porta la copertina di [da] su [a]: la cartella è la stessa, rinominata.
     *
     * ⚠️⚠️ **NASCE DALLA SUA DOMANDA, ED È LA METÀ CHE SI PUÒ EVITARE** (2026-09-08: *cosa
     * succede all'immagine memorizzata come copertina se rinomino la cartella da AIV? E se la
     * rinomino dall'esterno?*). Il `BUCKET_ID` lo ricava il MediaStore dal **percorso**, quindi
     * una rinomina lo cambia e la copertina resterebbe appesa a una cartella che non esiste più.
     * Rinominando **da AIV** i due identificatori si conoscono tutti e due, e allora il file si
     * rinomina insieme alla cartella; da fuori non si può, ed è quello di cui si occupa [sweep]
     * con il suo periodo di grazia.
     * ⚠️ **Rinomina il file invece di riscriverlo**: sono gli stessi byte, e ricomprimerli
     * perderebbe qualità per niente.
     * ⚠️ **Il nome nuovo porta un istante nuovo**, per la stessa ragione di [set]: Coil indirizza
     * la cache col percorso, e un file spostato che tenesse il nome di prima mostrerebbe la
     * miniatura vecchia finché la cache non scade.
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
     * Butta le copertine delle cartelle che non esistono più, con un periodo di grazia.
     *
     * ⚠️⚠️ **SERVE PERCHÉ LE RINOMINE FATTE DA FUORI LASCIANO ORFANI, E LO HA VISTO LUI** (*è
     * inevitabile che delle rinomine avvengano da app esterne*): là non c'è nessun travaso
     * possibile, perché quando l'app se ne accorge la cartella di prima non esiste più e quella
     * nuova non ha niente che la leghi alla vecchia. Il file resterebbe in casa per sempre, letto
     * da nessuno.
     *
     * ⚠️⚠️ **NON GIRA IN SOTTOFONDO, E NON È UNA SEMPLIFICAZIONE: È LA SUA REGOLA** (risposta
     * `d-cestino-chiusa`: *nulla deve avvenire al di fuori dell'app aperta in primo piano*).
     * Quindi niente lavoro programmato di sistema: la potatura si fa quando la schermata iniziale
     * legge le cartelle, che è anche l'unico momento in cui l'elenco vero dei bucket è in mano.
     *
     * ⚠️⚠️ **E NON CANCELLA SUBITO, PERCHÉ UN'ASSENZA NON È UNA MORTE**: una scheda SD smontata,
     * un archivio non ancora indicizzato o un permesso appena revocato fanno sparire cartelle
     * vere per qualche minuto, e una potatura immediata butterebbe una scelta che l'utente non ha
     * mai disfatto. Una copertina vista si **data**, e si cancella solo se resta assente per più
     * di [COVER_GRACE_MS].
     * ⚠️ **La data di modifica del file È l'archivio**, come per il resto di questo oggetto: una
     * seconda tabella con l'ultima volta che ogni cartella è stata vista sarebbe una fonte di
     * verità in più da tenere allineata a dei file.
     * ⚠️ **Con l'elenco vuoto non fa niente**: 'nessuna cartella' è quello che si legge senza
     * permesso o prima che il MediaStore risponda, e là fuori tutte le copertine sembrerebbero
     * orfane.
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
     * La cartella delle copertine, creata alla prima scrittura.
     *
     * ⚠️ **Torna `null` invece di lanciare**: `mkdirs` può non riuscire (spazio finito, archivio
     * smontato), e una copertina che non si scrive è un esito da dire con una notifica, non un
     * errore che porta giù la schermata.
     */
    private fun home(context: Context): File? =
        File(context.filesDir, COVER_DIR).takeIf { it.isDirectory || it.mkdirs() }

    /** Di quale cartella è la copertina questo file, letto dal suo nome. */
    private fun owner(file: File): Long? =
        file.name.substringBeforeLast('.').substringBeforeLast('-').toLongOrNull()

    /** Butta le copie vecchie di [bucket], tenendo [keep] se c'è. */
    private fun sweep(casa: File, bucket: Long, keep: File?) {
        casa.listFiles().orEmpty()
            .filter { owner(it) == bucket && it != keep }
            .forEach { it.delete() }
    }

    /**
     * L'immagine scelta, decodificata e rimpicciolita a [COVER_PX].
     *
     * ⚠️⚠️ **PASSA DA `ImageDecoder` E NON DA `BitmapFactory`, per due cose che quello non fa**:
     * applica da sé l'orientamento EXIF (con `BitmapFactory` una fotografia scattata in verticale
     * diventerebbe una copertina coricata) e legge i formati che il sistema ha imparato dopo,
     * HEIF e AVIF compresi. Di un'immagine animata prende il primo fotogramma, che è quello che
     * una copertina deve essere.
     * ⚠️⚠️ **L'ALLOCATORE SI DICHIARA SOFTWARE, o il resto non funziona**: di serie
     * `decodeBitmap` può dare un bitmap in memoria grafica, e quello non si può comprimere.
     * Chiederlo in memoria normale è la riga che rende scrivibile quello che si è appena letto.
     * ⚠️ **Il campionamento è a potenze di due** e non una misura esatta: `setTargetSize`
     * deformerebbe l'immagine se le proporzioni non tornano, mentre qui il lato finale cade fra
     * [COVER_PX] e il suo doppio, che per una copertina è già abbondante.
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
     * ⚠️⚠️ **WebP DA ANDROID 11 IN SU, E SOTTO I DUE DI SEMPRE**: WebP porta la trasparenza e
     * pesa come un JPEG, quindi è l'unico formato che copre i due casi con un file solo. Sotto
     * quella versione la costante senza perdita non esiste, e la sola alternativa che tiene
     * l'alfa è il PNG, che su una fotografia peserebbe dieci volte tanto: là si sceglie a
     * seconda di quello che l'immagine ha davvero.
     * ⚠️ **`hasAlpha` e non il tipo del file di partenza**: un PNG può essere opaco, e a
     * decidere è quello che c'è nel bitmap dopo la decodifica.
     */
    private fun squeeze(bitmap: Bitmap): Squeeze = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
            Squeeze(Bitmap.CompressFormat.WEBP_LOSSY, "webp")
        bitmap.hasAlpha() -> Squeeze(Bitmap.CompressFormat.PNG, "png")
        else -> Squeeze(Bitmap.CompressFormat.JPEG, "jpg")
    }

    /** Un formato di scrittura col suffisso che gli tocca. */
    private class Squeeze(val format: Bitmap.CompressFormat, val suffix: String)
}

/**
 * La fascia che dice che si sta scegliendo una copertina, e offre di lasciar perdere.
 *
 * ⚠️⚠️ **NON È UNA NOTIFICA, ED È LA RAGIONE PER CUI NON PASSA DA [Notices]**: una notifica dice
 * che una cosa **è** successa e se ne va da sé dopo qualche secondo, questa dice che cosa **sta**
 * succedendo e deve restare finché la modalità è viva, che può durare quanto ci vuole a
 * cambiare cartella. Un canale che tiene una riga per volta non può portarla senza diventare
 * un'altra cosa.
 * ⚠️ **La superficie è la stessa della notifica**, bordo d'accento compreso: sono tutte e due
 * righe che l'app scrive in fondo allo schermo, e due vestiti diversi le farebbero sembrare di
 * due app.
 * ⚠️ **Il nome della cartella va fra apici**, come nelle due finestre del nascondere e del
 * mostrare: è un dato dentro una frase, e senza segni si legge come una parola della frase.
 */
@Composable
fun CoverInvite(name: String, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        // ⚠️ Il rientro di sistema se lo mette da sé, come la notifica: questa vive nel `Box` di
        // radice, che arriva al bordo dello schermo.
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
 * ⚠️ **La scelta a mano vince, e non è ovvio**: la copertina automatica cambia da sé ogni volta
 * che si aggiunge una fotografia, quindi lasciarle la precedenza vorrebbe dire una scelta che
 * dura fino al prossimo scatto.
 */
fun Folder.Bucket.coverIn(covers: Map<Long, Uri>): Uri? = covers[id] ?: cover

/**
 * Che cosa si chiede al selettore di sistema quando si sceglie una copertina.
 *
 * ⚠️ **Il tipo generico e non un elenco di suffissi**: a dire quali immagini sa aprire è il
 * fornitore che risponde, e un elenco scritto qui taglierebbe fuori i formati che il telefono ha
 * imparato dopo. Quello che il selettore consegna passa comunque dalla decodifica, che è il
 * posto in cui un file che non si legge viene detto.
 */
internal const val COVER_MIME = "image/*"

/**
 * Se la voce 'Copertina predefinita' compare nel menu del FAB: **no**, dalla `1.95`.
 *
 * ⚠️⚠️ **È SPENTA E NON TOLTA, ED È LA SUA ISTRUZIONE ALLA LETTERA** (riscontro del giro della
 * `1.94`, voce `copertina-togli` non approvata: *spegni la funzionalità del FAB senza eliminarla,
 * in caso cambiassi idea, ma rinomina la voce in `Copertina predefinita`*). La ragione per cui
 * l'ha bocciata è scritta: *un tocco involontario ripristina troppo facilmente la copertina
 * PREDEFINITA, e se non ricordi più qual era la personalizzata (o non ce l'hai più) non puoi
 * selezionarla di nuovo*. Al suo posto c'è il gesto ricorsivo sull'icona dell'intestazione, che
 * di tocchi ne vuole due.
 * ⚠️ **Una costante e non una riga cancellata**: quello che deve poter tornare con un carattere è
 * la voce **intera**, testo e posizione compresi, e la storia git non è un interruttore. Chi la
 * riaccende porti `true` qui e rilegga la voce del riscontro, perché il motivo del no vale ancora.
 * ⚠️ **La stringa resta tradotta in tutte le lingue**, e non è spreco: una voce che si riaccende
 * con un carattere non può dipendere da ventotto traduzioni da rifare.
 */
internal const val COVER_MENU_ROW = false

/** Dove vivono le copertine scelte, dentro `filesDir`. */
private const val COVER_DIR = "covers"

/** L'aria intorno alla fascia dell'invito, e lo stondamento: quelli della notifica di casa. */
private val INVITE_EDGE = 12.dp
private val INVITE_ROUND = 14.dp

/**
 * Il lato massimo di una copertina copiata in casa.
 *
 * ⚠️ **Il doppio di [Thumbs.PX]**, che è la misura a cui le miniature si chiedono: il doppio è il
 * margine per uno schermo che un domani ne chiedesse di più, e oltre sarebbe spazio occupato per
 * pixel che nessuno guarda.
 */
private const val COVER_PX = 1024

/**
 * Quanto si stringe una copertina.
 *
 * ⚠️ **Novanta e non cento**: su un'immagine grande quanto una copertina la differenza fra i due
 * non si vede, e il file pesa la metà. Non è la qualità di un'esportazione, dove il criterio è
 * un altro: qui l'originale resta dov'è.
 */
private const val COVER_QUALITY = 90

/**
 * Quanto una copertina resta in casa dopo che la sua cartella è sparita: **un mese**.
 *
 * ⚠️ **Un mese e non un giorno**: quello che l'attesa protegge è una cartella che c'è ma non si
 * vede (una scheda SD smontata, un archivio esterno staccato, un permesso revocato e ridato), e
 * quelle assenze durano quanto dura un viaggio. Il costo di sbagliare per eccesso è qualche
 * decina di kilobyte tenuti un mese in più; quello di sbagliare per difetto è una scelta persa
 * che l'utente non ha mai disfatto.
 * ⚠️ **E non un anno**: oltre il mese il file non è più recuperabile nemmeno riattaccando la
 * scheda, perché la cartella rinominata nel frattempo ha un altro identificatore. Tenerlo di più
 * vorrebbe dire tenerlo per sempre.
 */
private const val COVER_GRACE_MS = 30L * 24 * 60 * 60 * 1000

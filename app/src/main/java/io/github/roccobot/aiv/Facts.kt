package io.github.roccobot.aiv

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.annotation.StringRes
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Che cosa si sa delle immagini scelte: il conto e il peso sempre, tutto il resto solo
 * quando è **una sola**.
 *
 * ⚠️ **Il peso è la somma, il resto non si somma**: nome e misure di dieci fotografie
 * diverse non si possono riassumere senza inventare, e una riga che dicesse 'varie' non
 * direbbe niente. Con una sola la domanda ha una risposta esatta, ed è [one].
 */
class Facts(val count: Int, val bytes: Long, val one: OneFile? = null)

/**
 * I dati di UN file, campo per campo.
 *
 * ⚠️⚠️ **`null` VUOL DIRE 'NON C'È', E LA RIGA NON SI SCRIVE** (richiesta dell'utente,
 * 2026-08-30: *nel caso in cui non ci sia un dato, la riga corrispondente non viene
 * mostrata*). Da qui la scelta di tenere **i campi separati** invece di una lista di
 * righe già formattate: il formato lo decide l'interfaccia, che sa la lingua e le
 * impostazioni, e questa classe dice solo che cosa il file contiene. ⚠️ Niente stringhe di
 * ripiego tipo 'n.d.': un campo vuoto qui è un campo assente là.
 * ⚠️ **I numeri restano numeri** (millisecondi, gradi decimali, secondi, millimetri): la
 * formattazione dipende dalla lingua, e un `Double` formattato troppo presto è un dato che
 * non si può più formattare bene.
 */
class OneFile(
    val name: String?,
    /** Il tipo MIME per intero (`image/jpeg`): l'interfaccia ne mostra la parte utile. */
    val mime: String?,
    val width: Int?,
    val height: Int?,
    /** Quando la fotografia è stata SCATTATA, dai dati EXIF. Millisecondi epoch. */
    val taken: Long?,
    /** Quando il FILE è stato modificato l'ultima volta, dal filesystem. */
    val modified: Long?,
    /** Il percorso della cartella che lo contiene, senza il nome del file. */
    val folder: String?,
    val encoding: Encoding?,
    /**
     * Come tiene i suoi colori. Vedi [coloursOf].
     *
     * ⚠️ **È un dato diverso da [encoding], anche se i due si leggono negli stessi byte**:
     * quello dice come l'immagine è **compressa**, questo che cosa contiene un pixel. Su un
     * PNG a tavolozza le due righe dicono 'PNG, 8 bit' e 'colore indicizzato, 256 colori', che
     * sono due informazioni e non due modi di dire la stessa.
     */
    val colours: Colours?,
    /** Fotogrammi e durata, se è animata. `null` su un'immagine ferma. Vedi [motionOf]. */
    val motion: Motion?,
    val focalMm: Double?,
    val exposureSec: Double?,
    val iso: Int?,
    val latitude: Double?,
    val longitude: Double?,
    val altitudeM: Double?
)

/**
 * I campi delle informazioni, nell'ordine in cui l'utente li ha chiesti.
 *
 * ⚠️⚠️ **L'ORDINE DI QUESTO `enum` È IL VALORE DI FABBRICA**, e viene dall'elenco
 * dell'utente parola per parola: nome, tipo, pixel, peso, data di acquisizione, ultima
 * modifica, percorso, codifica, altri EXIF, posizione. Chi riordina le costanti cambia
 * l'aspetto predefinito della schermata di ogni telefono che non ha mai toccato
 * l'impostazione, quindi non si riordina per estetica.
 * ⚠️⚠️ **I TRE CAMPI DELLA `1.16` STANNO DOPO LA CODIFICA, e non in fondo**: metodo colore,
 * fotogrammi e durata parlano di **come è fatto il file**, come la codifica, e la riga che li
 * spiega si legge accanto a lei. Chi aveva già riordinato la lista non li perde e non li vede
 * spostati: [factOrderOf] aggiunge in coda i gettoni che il suo archivio non nomina.
 * ⚠️ **[always] sono i tre non negoziabili** (nome, pixel e peso): l'utente li ha dichiarati
 * sempre visibili, quindi l'interruttore per loro non esiste. Restano **spostabili**, che è
 * un'altra cosa: la richiesta dice 'sempre visibili', non 'in posizione fissa'.
 * ⚠️ **Il nome invece è ANCHE fisso in testa**, e quello sì è una posizione: sta scritto
 * nella richiesta (*nome del file con estensione, sempre in testa*), e lo garantisce
 * [factOrderOf] invece di lasciarlo alla buona volontà dell'interfaccia.
 * ⚠️ **Il gettone non è il nome della costante**: vale la regola di [Choice], cioè che
 * rinominare una costante non deve azzerare la scelta salvata su un telefono.
 * ⚠️ **Il nome della riga sta QUI e non in un `when` dell'interfaccia**, al contrario delle
 * altre scelte di `Settings.kt`: serve in **due** posti (la riga delle impostazioni e
 * l'etichetta nel dialogo), e un `when` da dieci rami scritto due volte è il posto dove le
 * due copie prima o poi diranno due parole diverse per la stessa cosa.
 */
enum class FactField(
    override val token: String,
    @StringRes val label: Int,
    val always: Boolean = false
) : Choice {
    NAME("name", R.string.facts_name, always = true),
    KIND("kind", R.string.facts_kind),
    PIXELS("pixels", R.string.facts_pixels, always = true),
    SIZE("size", R.string.facts_size, always = true),
    TAKEN("taken", R.string.facts_taken),
    MODIFIED("modified", R.string.facts_modified),
    FOLDER("folder", R.string.facts_folder),
    ENCODING("encoding", R.string.facts_encoding),
    COLOURS("colours", R.string.facts_colours),
    FRAMES("frames", R.string.facts_frames),
    DURATION("duration", R.string.facts_duration),
    CAMERA("camera", R.string.facts_camera),
    PLACE("place", R.string.facts_place)
}

/**
 * L'ordine salvato, ricondotto a qualcosa di sensato.
 *
 * ⚠️⚠️ **I CAMPI CHE MANCANO SI AGGIUNGONO IN CODA invece di sparire**, ed è quello che
 * permette a una versione futura di aggiungere un campo senza che chi ha già riordinato la
 * lista lo perda: l'archivio di quel telefono elenca nove gettoni su dieci, e il decimo
 * compare da sé. Il contrario (mostrare solo quelli elencati) farebbe nascere ogni campo
 * nuovo **invisibile**, cioè introvabile.
 * ⚠️ **I gettoni sconosciuti si scartano**: un archivio scritto da una versione più nuova,
 * o a mano, non deve far cadere l'app.
 * ⚠️ **Il nome torna sempre in testa**, anche se l'archivio dicesse altro.
 */
fun factOrderOf(tokens: List<String>): List<FactField> {
    val known = tokens.mapNotNull { t -> FactField.entries.firstOrNull { it.token == t } }
    val ordered = known.distinct() + FactField.entries.filterNot { it in known }
    return listOf(FactField.NAME) + ordered.filterNot { it == FactField.NAME }
}

/**
 * I dati di una selezione, letti dal disco.
 *
 * ⚠️⚠️ **LE MISURE SI LEGGONO DALLE INTESTAZIONI E NON DECODIFICANDO**
 * (`inJustDecodeBounds`), ed è la differenza fra leggere qualche decina di byte e
 * ricostruire trenta megapixel per scrivere '8160 x 6120'. Vale anche per i formati che
 * il MediaStore non descrive, quindi non serve un secondo ramo per loro.
 * ⚠️ **Il peso invece NON si prende dal decodificatore**: quello dice quanto misura
 * l'immagine, non quanto pesa il file. Per un `file://` lo dice il filesystem, per tutto
 * il resto la colonna che ogni provider serve.
 *
 * ⚠️⚠️ **SI PASSA DAL FILE VERO QUANDO C'È, e non è un'ottimizzazione**: da Android 10 il
 * MediaStore **censura la posizione** nei dati EXIF che serve attraverso un `content://`, a
 * meno di chiedere un permesso in più. Questa app ha già l'accesso a tutti i file, quindi
 * legge il percorso e apre il file: le coordinate arrivano intere e senza chiedere niente.
 * ⚠️ **Per una fotografia senza percorso** (una chat, il web) la riga della posizione
 * semplicemente non compare, che è la regola di tutta la schermata e non un caso speciale.
 */
suspend fun factsOf(context: Context, uris: List<Uri>): Facts = withContext(Dispatchers.IO) {
    var bytes = 0L
    for (uri in uris) bytes += sizeOf(context, uri)
    if (uris.size != 1) return@withContext Facts(uris.size, bytes)

    val uri = uris.first()
    val file = FileTree.fileOf(context, uri)

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching {
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
    }
    val exif = runCatching {
        if (file != null) ExifInterface(file)
        else context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
    }.getOrNull()

    // ⚠️ Le misure si girano come l'EXIF dice, o una foto verticale scattata col telefono
    // di lato si annuncerebbe orizzontale: il file la conserva com'è uscita dal sensore, e
    // il numero che interessa è quello di come la si vede.
    val turned = exif?.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    ) in setOf(
        ExifInterface.ORIENTATION_ROTATE_90,
        ExifInterface.ORIENTATION_ROTATE_270,
        ExifInterface.ORIENTATION_TRANSPOSE,
        ExifInterface.ORIENTATION_TRANSVERSE
    )
    /*
     * ⚠️⚠️ **SU UN AVIF `BitmapFactory` NON DICE NEMMENO LE MISURE**, e non è un caso
     * limite: passa dallo stesso decodificatore di sistema che sui file veri si rifiuta
     * (vedi [Avif]), quindi `inJustDecodeBounds` torna a mani vuote e la scheda scriverebbe
     * '?' proprio sul formato che la `1.26` ha aggiunto. Le misure le dice l'intestazione,
     * e [Avif.dimensions] gira già i lati se il file dichiara una rotazione.
     * ⚠️⚠️ **E SU UN SVG NON LE DICE PER UN'ALTRA RAGIONE (1.31)**: là il decodificatore si
     * rifiuta, qui il formato non lo conosce nessuno, perché `BitmapFactory` legge i formati
     * a pixel. Il sintomo però è lo stesso, cioè `inJustDecodeBounds` a mani vuote, e per
     * questo le due letture stanno nello stesso posto.
     * ⚠️ **Si legge SOLO quando l'altra strada ha fallito**: su un JPEG questa riga non
     * apre niente.
     */
    val fallback = if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val head = ByteArray(Avif.HEAD)
                val got = stream.readFully(head, Avif.HEAD)
                val head2 = head.copyOf(got)
                when {
                    Avif.looksLike(head2) -> Avif.dimensions(head2)
                    /*
                     * ⚠️⚠️ **QUI SI RIAPRE IL FILE, e non si può fare altrimenti**: un SVG va
                     * letto **intero** per sapere quanto è grande, perché è un documento XML e
                     * non un'intestazione, e questo stream è già stato consumato per i primi
                     * 128 kilobyte. ⚠️ Il costo è quello di un file di testo di pochi
                     * kilobyte, non quello di una decodifica: non si disegna niente.
                     */
                    Svg.looksLike(head2) -> context.contentResolver.openInputStream(uri)
                        ?.use { Svg.dimensions(it) }
                    else -> null
                }
            }
        }.getOrNull()
    } else {
        null
    }
    val width = bounds.outWidth.takeIf { it > 0 } ?: fallback?.width
    val height = bounds.outHeight.takeIf { it > 0 } ?: fallback?.height

    val place = exif?.latLong
    Facts(
        count = 1,
        bytes = bytes,
        one = OneFile(
            name = nameOf(context, uri),
            mime = mimeOf(context, uri),
            width = if (turned) height else width,
            height = if (turned) width else height,
            taken = exifMoment(exif) ?: takenFromStore(context, uri),
            modified = file?.lastModified()?.takeIf { it > 0L } ?: modifiedFromStore(context, uri),
            folder = file?.parent,
            encoding = runCatching {
                context.contentResolver.openInputStream(uri)?.use { encodingOf(it) }
            }.getOrNull(),
            /*
             * ⚠️⚠️ **TRE APERTURE E NON UNA, ed è una scelta e non una svista**: ogni lettore
             * cammina il file in avanti e a distanze diverse (la codifica si ferma al primo
             * SOF, il metodo colore arriva ai chunk, il conteggio dei fotogrammi va in fondo),
             * quindi un flusso solo andrebbe riavvolto, e un `InputStream` di un `content://`
             * non si riavvolge. Aprire tre volte costa tre `open` e nessuna decodifica.
             * ⚠️ **La trasparenza di una GIF è una quarta lettura**, e solo per le GIF: là
             * sta nei blocchi dei fotogrammi, non nell'intestazione.
             */
            colours = runCatching {
                val read = context.contentResolver.openInputStream(uri)?.use { coloursOf(it) }
                if (read != null && read.model == Colours.Model.INDEXED && !read.transparent &&
                    context.contentResolver.openInputStream(uri)
                        ?.use { gifTransparencyOf(it) } == true
                ) {
                    Colours(read.model, read.bitsPerChannel, read.palette, true)
                } else {
                    read
                }
            }.getOrNull(),
            motion = runCatching {
                context.contentResolver.openInputStream(uri)?.use { motionOf(it) }
            }.getOrNull(),
            focalMm = exif?.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                ?.takeIf { it > 0.0 },
            exposureSec = exif?.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
                ?.takeIf { it > 0.0 },
            iso = exif?.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0)
                ?.takeIf { it > 0 },
            latitude = place?.getOrNull(0),
            longitude = place?.getOrNull(1),
            // ⚠️ Zero è un'altitudine legittima (il livello del mare), quindi il valore di
            // ripiego è un numero che nessun luogo ha: senza, una foto scattata in
            // spiaggia perderebbe la riga e una senza il dato ne guadagnerebbe una falsa.
            altitudeM = exif?.getAltitude(NO_ALTITUDE)?.takeIf { it != NO_ALTITUDE }
        )
    )
}

/** Un'altitudine che nessun posto ha, per distinguere 'zero metri' da 'non lo so'. */
private const val NO_ALTITUDE = -1e9

/**
 * La data di scatto dai dati EXIF, letta dal tag e convertita a mano.
 *
 * ⚠️⚠️ **NON SI USA `ExifInterface.dateTimeOriginal`, e non è una preferenza di stile**:
 * quella proprietà è dichiarata a uso interno della libreria (`RestrictTo`), quindi lint la
 * segnala come **errore** e il rilascio non passa. Il tag invece è pubblico ed è lo stesso
 * dato.
 * ⚠️⚠️ **SI PARSA CON `Locale.US`, e questa è la trappola vera**: `SimpleDateFormat` legge
 * l'anno secondo il **calendario** della lingua del telefono, e con una lingua che non usa
 * quello gregoriano (il tailandese conta dal 543 a.C.) l'anno 2026 diventerebbe un altro,
 * in silenzio e solo su quei telefoni.
 * ⚠️ **L'EXIF scrive l'ora dell'orologio, senza fuso**: la si interpreta nel fuso del
 * telefono, che è anche quello in cui verrà scritta, quindi la riga mostra esattamente
 * l'ora che ha scritto la fotocamera. Un fuso diverso da quello dello scatto sposterebbe
 * l'istante ma non la stringa, ed è il compromesso che fanno tutte le gallerie.
 * ⚠️ **`DATETIME` come ripiego di `DATETIME_ORIGINAL`**: qualche apparecchio scrive solo il
 * secondo, e una data giusta vale più di una riga assente.
 */
private fun exifMoment(exif: ExifInterface?): Long? {
    val text = exif?.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        ?: exif?.getAttribute(ExifInterface.TAG_DATETIME)
        ?: return null
    return runCatching {
        SimpleDateFormat(EXIF_PATTERN, Locale.US).parse(text)?.time?.takeIf { it > 0L }
    }.getOrNull()
}

/** Come l'EXIF scrive un istante, dalla sua specifica. */
private const val EXIF_PATTERN = "yyyy:MM:dd HH:mm:ss"

private fun sizeOf(context: Context, uri: Uri): Long {
    if (uri.scheme?.lowercase() == "file") {
        return uri.path?.let { runCatching { File(it).length() }.getOrNull() } ?: 0L
    }
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { c ->
                val at = c.getColumnIndex(OpenableColumns.SIZE)
                if (at >= 0 && c.moveToFirst() && !c.isNull(at)) c.getLong(at) else null
            }
    }.getOrNull() ?: 0L
}

private fun nameOf(context: Context, uri: Uri): String? {
    if (uri.scheme?.lowercase() == "file") return uri.lastPathSegment
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c ->
                val at = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (at >= 0 && c.moveToFirst()) c.getString(at) else null
            }
    }.getOrNull() ?: uri.lastPathSegment
}

/**
 * Il tipo MIME, chiesto prima a chi serve il file e poi all'estensione.
 *
 * ⚠️ **L'estensione è il ripiego e non la prima scelta**: un `content://` può servire un
 * JPEG con un nome che finisce in `.png`, e in quel caso il provider dice la verità e il
 * nome no. Ma per un `file://` il provider non c'è, e allora l'estensione è tutto quello
 * che si ha. ⚠️ **La codifica vera la dice [encodingOf]**, che guarda i byte: quando le due
 * cose non concordano, quella ha ragione.
 */
private fun mimeOf(context: Context, uri: Uri): String? {
    runCatching { context.contentResolver.getType(uri) }.getOrNull()?.let { return it }
    val extension = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()
    if (extension.isNullOrEmpty()) return null
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
}

/**
 * Il nome del formato ricavato da un tipo MIME: `image/svg+xml` diventa `SVG`.
 *
 * ⚠️⚠️ **ESISTE PERCHÉ LA STESSA REGOLA VIVEVA IN UN POSTO SOLO SU DUE, e il posto sbagliato**
 * (riscontro `info-svg` della `1.41`: *vedo ancora la vecchia dicitura con XML*). Nella `1.40`
 * il taglio del suffisso era entrato nel **pannello** delle informazioni, e la **barra delle
 * info** del visualizzatore ha continuato a scrivere `SVG+XML` per due versioni, perché
 * calcolava il formato per conto suo. Adesso il calcolo è qui e i due posti lo chiamano: è la
 * stessa cura di [InfoSideRow], e per la stessa ragione.
 *
 * ⚠️ **Il suffisso dopo il `+` non è il formato, è la SINTASSI in cui è scritto** (RFC 6839):
 * chi legge quella riga vuole sapere che immagine ha davanti, e `+xml` risponde a un'altra
 * domanda. ⚠️ **Fra i tipi che l'app dichiara è l'unico che ne ha uno** (misurato sul manifesto
 * e sui sorgenti: apng, avif, bmp, gif, jpeg, png, svg+xml, tiff, webp), quindi oggi il taglio
 * riguarda un formato solo; vale lo stesso per tutti, perché un `+json` o un `+zip` di domani
 * avrebbero lo stesso difetto.
 *
 * ⚠️ **Maiuscolo INVARIANTE e non della lingua del telefono**: un tipo MIME è ASCII e non è una
 * parola, e la regola turca della `i` cambierebbe `image/tiff` in `TİFF`.
 */
fun kindOf(mime: String?): String? = mime
    ?.substringAfterLast('/')
    ?.substringBefore('+')
    ?.takeIf { it.isNotBlank() }
    ?.uppercase(Locale.ROOT)

/**
 * La data di scatto secondo il MediaStore, quando l'EXIF non ce l'ha.
 *
 * ⚠️ Serve davvero e non è un doppione: le immagini scaricate spesso perdono l'EXIF, e il
 * MediaStore la ricava anche dal **nome** del file (`IMG_20260830_154212`), che è
 * un'informazione vera che altrimenti si butterebbe.
 */
private fun takenFromStore(context: Context, uri: Uri): Long? = runCatching {
    val column = MediaStore.Images.Media.DATE_TAKEN
    context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { c ->
        val at = c.getColumnIndex(column)
        if (at >= 0 && c.moveToFirst() && !c.isNull(at)) c.getLong(at).takeIf { it > 0L } else null
    }
}.getOrNull()

/**
 * L'ultima modifica secondo il MediaStore.
 *
 * ⚠️⚠️ **QUELLA COLONNA È IN SECONDI, non in millisecondi**, al contrario di `DATE_TAKEN`
 * che sta accanto a lei: prenderla per millisecondi darebbe il 1970 su ogni fotografia, ed
 * è lo scambio che si nota solo guardando la data stampata.
 */
private fun modifiedFromStore(context: Context, uri: Uri): Long? = runCatching {
    val column = MediaStore.Images.Media.DATE_MODIFIED
    context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { c ->
        val at = c.getColumnIndex(column)
        if (at >= 0 && c.moveToFirst() && !c.isNull(at)) {
            c.getLong(at).takeIf { it > 0L }?.times(1000L)
        } else null
    }
}.getOrNull()

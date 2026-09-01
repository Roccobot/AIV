package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * L'indicizzazione: da una galleria a un indice di vettori, un pezzo per volta.
 *
 * ⚠️⚠️ **SI PARTE DALLA MINIATURA DEL SISTEMA E NON DALLA FOTOGRAFIA INTERA, ed è la scelta
 * che decide quanto dura tutto**: il costo misurato per foto è **~104 ms**, di cui **67,9 di
 * sola decodifica** e 36 di modello (brief, 2026-08-30). Chiedendo al MediaStore la miniatura
 * che ha già, quella parte quasi sparisce, e su diecimila foto sono minuti invece di un
 * quarto d'ora. ⚠️ Il costo, dichiarato: il modello vede una versione meno dettagliata. A 256
 * pixel di lato la vedrebbe comunque.
 *
 * ⚠️⚠️ **SOLO FOTOGRAFIE, NON FILMATI**: l'encoder immagine vuole un'immagine, e un video
 * avrebbe bisogno di una scelta in più (quale fotogramma, o quanti). Non è una dimenticanza
 * ed è il posto giusto in cui aggiungerli, quando si deciderà come.
 *
 * ⚠️ **A PEZZI, e ogni pezzo si scrive**: se l'app viene chiusa a metà, il lavoro fatto resta
 * e si riprende da dove si era arrivati, perché il primo passo è chiedere all'indice che cosa
 * ha già.
 */
object ClipRun {

    /** Quante foto per pezzo: dopo ognuno si scrive l'indice e si riferisce l'avanzamento. */
    private const val CHUNK = 40

    /** Il lato che si chiede al sistema: il doppio di quello del modello, che poi riduce. */
    private const val THUMB = 512

    /**
     * Indicizza quello che manca, e torna quante foto ha aggiunto.
     *
     * ⚠️ **Chi chiama passa il motore già aperto**: aprirlo costa qualche centinaio di
     * millisecondi e i 45 MB dell'encoder immagine mappati, e va fatto una volta per tutta
     * l'indicizzazione.
     * ⚠️ **L'annullamento è vero**, come nello scaricamento: si controlla la coroutine a ogni
     * fotografia, così spegnere la funzione ferma il lavoro invece di lasciarlo correre.
     */
    suspend fun index(
        context: Context,
        engine: ClipEngine,
        hidden: Set<String>,
        onStep: (Int, Int) -> Unit
    ): Int = withContext(Dispatchers.Default) {
        val all = Folder.everyPicture(context, hidden)
        val known = ClipIndex.load(context).keys
        val todo = all.filterNot { it.toString() in known }
        if (todo.isEmpty()) return@withContext 0

        /*
         * ⚠️⚠️ **IL MODELLO SI APRE QUI, E CON UN SEGNO TUTTO SUO, dalla 1.07**: si sarebbe
         * aperto comunque alla prima fotografia, ma dentro la stessa riga, e la sicura non
         * avrebbe potuto dire quale dei due passi era in corso. Sono le due cause più
         * probabili del crollo che l'utente vede, e non si somigliano: se il segno dice
         * `modello` il sospetto è il file scaricato o il runtime nativo, se dice `indice 0/N`
         * è la prima fotografia. Distinguerle costa queste tre righe.
         */
        ClipGuard.arm(context, "modello")
        engine.warmImage()
        ClipGuard.arm(context, "indice 0/${todo.size}")

        var done = 0
        val batch = ArrayList<Pair<String, ByteArray>>(CHUNK)
        for (uri in todo) {
            coroutineContext.ensureActive()
            /*
             * ⚠️⚠️ **UNA FOTOGRAFIA SOLA NON PUÒ FERMARE TUTTO, dalla 1.07**: fino alla
             * `1.06` la lettura dei pixel stava **fuori** da ogni `runCatching`, quindi una
             * sola immagine illeggibile (un file troncato, una miniatura che il sistema non
             * sa dare, un formato che la decodifica rifiuta) faceva saltare l'intera
             * indicizzazione e lasciava il segno della sicura, cioè spegneva la funzione.
             * Con diecimila fotografie basta che **una** sia storta.
             * ⚠️ Non è la causa del crollo che si sta cercando, e non va scambiata per
             * quella: un'eccezione Kotlin la sicura la scriveva già, e l'utente vede il
             * processo **morire**. È un difetto trovato leggendo, e si ripara perché c'è.
             */
            /*
             * ⚠️⚠️ **IL SEGNO DISTINGUE LA MINIATURA DAL MODELLO, dalla 1.09**: `indice N/M`
             * copriva due cose native diverse, la decodifica dell'immagine e l'inferenza, e
             * una lapide che non si riesce a leggere lascerebbe la domanda aperta. Sono due
             * scritture di venti byte per fotografia, cioè qualche millesimo di secondo su un
             * lavoro che per ognuna ne dura cento.
             */
            val vector = runCatching {
                vectorOf(context, engine, uri) { passo ->
                    ClipGuard.arm(context, "indice $done/${todo.size} $passo")
                }
            }.getOrNull()
            if (vector != null) batch.add(uri.toString() to ClipIndex.pack(vector))
            done++
            if (batch.size >= CHUNK) {
                ClipIndex.append(context, batch)
                batch.clear()
                onStep(done, todo.size)
                /*
                 * ⚠️⚠️ **IL SEGNO SI RISCRIVE A OGNI PEZZO, dalla 1.07, e porta il punto**:
                 * prima diceva 'indice' e basta, quindi al giro dopo si sapeva che il motore
                 * non era tornato ma non **dove**. È la differenza fra due cause opposte:
                 * morire a `0/8000` vuol dire che è l'apertura del modello, morire a
                 * `6000/8000` vuol dire una fotografia o un accumulo. Una scrittura di venti
                 * byte ogni quaranta fotografie, cioè qualche centinaio in un quarto d'ora.
                 */
                ClipGuard.arm(context, "indice $done/${todo.size}")
            }
        }
        ClipIndex.append(context, batch)
        onStep(done, todo.size)
        done
    }

    /**
     * Il vettore di una fotografia, dalla miniatura ai 512 numeri.
     *
     * ⚠️ **Sta in una funzione a sé perché deve poter fallire tutta insieme**: il pezzo che
     * legge i pixel e quello che li dà al modello sono due modi di non riuscire sulla stessa
     * fotografia, e chi chiama deve solo sapere se questa foto è entrata nell'indice o no.
     */
    private fun vectorOf(
        context: Context,
        engine: ClipEngine,
        uri: Uri,
        onStage: (String) -> Unit
    ): FloatArray? {
        onStage("miniatura")
        val shot = small(context, uri) ?: return null
        // ⚠️ Le misure si prendono PRIMA di liberare la mappa di pixel: dopo `recycle`
        // quell'oggetto non è più buono, e leggerne i lati è il genere di cosa che funziona
        // finché non smette.
        val w = shot.width
        val h = shot.height
        if (w <= 0 || h <= 0) {
            shot.recycle()
            return null
        }
        val pixels = IntArray(w * h)
        shot.getPixels(pixels, 0, w, 0, 0, w, h)
        shot.recycle()
        onStage("modello")
        return engine.ofImage(pixels, w, h)
    }

    /**
     * La fotografia in piccolo, chiesta al sistema quando può darla lui.
     *
     * ⚠️ `loadThumbnail` esiste da Android 10 e restituisce la miniatura che il MediaStore ha
     * già, o se la fa con molto meno di una decodifica intera. Sotto, e per gli indirizzi che
     * non sono del MediaStore, si decodifica **campionando** (`inSampleSize`), che è la stessa
     * cosa che fa il caricatore delle miniature.
     */
    private fun small(context: Context, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.scheme == "content") {
            runCatching {
                return context.contentResolver.loadThumbnail(uri, Size(THUMB, THUMB), null)
            }
        }
        val measure = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, measure)
        }
        val long = maxOf(measure.outWidth, measure.outHeight)
        if (long <= 0) return null
        var step = 1
        while (long / (step * 2) >= THUMB) step *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = step
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }
}

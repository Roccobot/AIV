package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.memory.MemoryCache
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Le miniature che sopravvivono a un file cambiato, cioè il difetto che la `2.24` non aveva
 * chiuso.
 *
 * ⚠️⚠️ **NASCE DA UNA VOCE TORNATA INDIETRO DUE VOLTE** (riscontro del giro della `2.24`, voce
 * `mini-cestino` non approvata: *ancora sbagliata, solo nella miniatura (griglia). Era così
 * anche prima*), ed è il caso della regola scritta in `AIV/CLAUDE.md` § '🧪 Quando si scrive
 * una prova, e quando no': un difetto che è arrivato a lui torna indietro con la prova che lo
 * avrebbe fermato.
 *
 * ⚠️⚠️ **CHE COSA MISURA E CHE COSA NO.** Misura le **vie** che il codice teneva aperte: la
 * rimozione che dipendeva da una mappa con un tetto, l'indirizzo dichiarato riscritto, e i
 * percorsi del cestino, che nessuno dichiarava. **Non** misura che la miniatura che si vede sia
 * quella giusta: quella la fa il provider del telefono, il banco non ne ha uno, e la causa non è
 * accertata (il perché per esteso vive sul campo `stale` di [Thumbs]).
 */
@RunWith(AndroidJUnit4::class)
class MiniatureTest {

    /**
     * **Una miniatura che nessuno ha registrato si butta lo stesso.**
     *
     * ⚠️ Questo è il difetto vero della `2.24`: [Thumbs.forget] cercava la chiave nella mappa di
     * casa, che ha un tetto di poche decine di voci, mentre la cache di Coil ne tiene quante la
     * memoria le concede. Oltre quel numero non c'era niente da rimuovere e l'immagine restava là.
     * ⚠️ **Controprovata rimettendo il difetto** (`keys.remove(at) ?: return`): la chiave resta
     * in cache e la prova diventa rossa.
     */
    @Test
    fun `butta una chiave che la mappa di casa non conosce`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val cache = SingletonImageLoader.get(context).memoryCache!!
        val uri = Uri.parse("content://media/external/images/media/7")
        val key = MemoryCache.Key(uri.toString())
        cache[key] = MemoryCache.Value(quadratino())

        assertNotNull("la prova non misura niente se la cache non ha preso l'immagine", cache[key])
        Thumbs.forget(context, uri)

        assertNull(cache[key])
    }

    /**
     * **Si buttano tutte le misure di quell'indirizzo, e nient'altro.**
     *
     * ⚠️ La misura richiesta vive negli **extra** della chiave, quindi lo stesso file può avere
     * più voci in cache; e il file accanto non ne deve perdere nessuna, o la griglia
     * rigenererebbe mezza cartella a ogni scrittura.
     */
    @Test
    fun `prende le due misure dello stesso file e lascia stare il vicino`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val cache = SingletonImageLoader.get(context).memoryCache!!
        val uri = Uri.parse("content://media/external/images/media/8")
        val piccola = MemoryCache.Key(uri.toString(), mapOf("coil#size" to "512"))
        val grande = MemoryCache.Key(uri.toString(), mapOf("coil#size" to "1024"))
        val vicina = MemoryCache.Key("content://media/external/images/media/9")
        listOf(piccola, grande, vicina).forEach { cache[it] = MemoryCache.Value(quadratino()) }

        Thumbs.forget(context, uri)

        assertNull(cache[piccola])
        assertNull(cache[grande])
        assertNotNull(cache[vicina])
    }

    /**
     * **Un indirizzo dichiarato riscritto si consuma una volta sola.**
     *
     * ⚠️ È la riga che fa saltare la miniatura già pronta del sistema: la prima richiesta va
     * alla decodifica normale, e dalla seconda in poi si torna alla strada veloce. Senza il
     * consumo, quel file pagherebbe una decodifica per sempre.
     */
    @Test
    fun `il segno del riscritto vale per una richiesta sola`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val uri = Uri.parse("content://media/external/images/media/10")

        assertFalse("nessuno lo ha dichiarato cambiato", Thumbs.rewritten(uri))
        Thumbs.forget(context, uri)
        assertTrue(Thumbs.rewritten(uri))
        assertFalse(Thumbs.rewritten(uri))
    }

    /**
     * **Il cestino dichiara i propri percorsi, quando un file arriva e quando se ne va.**
     *
     * ⚠️⚠️ **È LA VIA CHE NESSUNO COPRIVA**: là il MediaStore non vede niente, quindi
     * `FileTree.scan` non produce nessun indirizzo e la chiamata che vive dentro non si fa mai;
     * e i percorsi del cestino si riusano, perché il nome lo sceglie `FileTree.freeName`. Il
     * perché per esteso vive su `Bin.touch`.
     * ⚠️ **Controprovata togliendo le due righe**: nessuno dei due percorsi risulta dichiarato.
     */
    @Test
    fun `il cestino dichiara il percorso di una copia che nasce e che muore`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val sorgente = File(context.filesDir, "prova-copertina.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(8))
        }

        val copia = Bin.keep(context, sorgente)
        assertNotNull("senza la copia non c'è niente da misurare", copia)
        assertTrue("il file che nasce nel cestino", Thumbs.rewritten(copia!!.toUri()))

        Bin.drop(context, copia)
        assertTrue("il percorso che si libera", Thumbs.rewritten(copia.toUri()))
    }

    /**
     * Un'immagine qualunque da mettere in cache: quello che conta è la chiave, non i pixel.
     *
     * ⚠️ Un pixel solo e non un riquadro vero: la cache di Coil misura il peso di quello che
     * tiene, e una prova non ha bisogno di occuparle memoria.
     */
    private fun quadratino() = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImage()
}

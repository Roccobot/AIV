package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.InetSocketAddress

/**
 * La cache delle immagini del web nella `6.27` (rilievo C1 della revisione della 6.25): una copia
 * scaduta si rivalida col server, quindi un'immagine cambiata arriva nuova e una uguale non si
 * riscarica.
 *
 * ⚠️ **Il server è vero**, quello del JDK su `127.0.0.1`: la prova passa da `ImageSource.load` e
 * da `HttpURLConnection` come l'app, e le immagini si distinguono dalla larghezza.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CacheRemotaTest {

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private lateinit var server: HttpServer

    /** Quello che il server risponde adesso: il corpo, il suo `ETag` e il `Cache-Control`. */
    @Volatile private var corpo = ByteArray(0)
    @Volatile private var etag = "\"1\""
    @Volatile private var regola = "no-cache"

    /** Quante volte il server ha mandato il corpo, e quante ha risposto `304`. */
    @Volatile private var corpi = 0
    @Volatile private var uguali = 0

    @Before
    fun avvia() {
        File(app.cacheDir, "remote").deleteRecursively()
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/foto.png") { scambio ->
            scambio.responseHeaders.add("Cache-Control", regola)
            scambio.responseHeaders.add("ETag", etag)
            if (scambio.requestHeaders.getFirst("If-None-Match") == etag) {
                uguali++
                scambio.sendResponseHeaders(304, -1)
            } else {
                corpi++
                scambio.responseHeaders.add("Content-Type", "image/png")
                scambio.sendResponseHeaders(200, corpo.size.toLong())
                scambio.responseBody.use { it.write(corpo) }
            }
            scambio.close()
        }
        server.start()
    }

    @After
    fun ferma() = server.stop(0)

    private val indirizzo: Uri get() = Uri.parse("http://127.0.0.1:${server.address.port}/foto.png")

    /** Una PNG larga [larghezza] pixel e alta uno. */
    private fun png(larghezza: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(larghezza, 1, Bitmap.Config.ARGB_8888)
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
    }

    private fun larghezza(): Int {
        val esito = runBlocking { ImageSource.load(app, indirizzo) }
        return (esito as LoadResult.Ok).image.pixelWidth
    }

    /**
     * **L'immagine sostituita sul server, con lo stesso indirizzo, arriva nuova.**
     *
     * ⚠️ **Controprovata** con la cache di prima, che non aveva scadenza: la seconda apertura
     * rileggeva la copia, e la larghezza restava 3.
     */
    @Test
    fun `un'immagine cambiata sul server arriva nuova`() {
        corpo = png(3)
        assertEquals(3, larghezza())
        corpo = png(5)
        etag = "\"2\""
        assertEquals("La cache ha servito l'immagine vecchia", 5, larghezza())
    }

    /** **Una copia uguale non si riscarica**: il server risponde `304` e si riapre quella. */
    @Test
    fun `una copia uguale si rivalida senza riscaricarla`() {
        corpo = png(4)
        assertEquals(4, larghezza())
        assertEquals(4, larghezza())
        assertEquals("Il corpo è arrivato due volte", 1, corpi)
        assertEquals("Il server non è stato interrogato", 1, uguali)
    }

    /** **Finché il server la dice fresca, la copia si apre senza rete**, anche a server spento. */
    @Test
    fun `una copia fresca si apre senza rete`() {
        regola = "max-age=600"
        corpo = png(6)
        assertEquals(6, larghezza())
        server.stop(0)
        assertEquals(6, larghezza())
    }

    /** **Senza rete una copia scaduta vale più di un errore.** */
    @Test
    fun `una copia scaduta si apre se la rete manca`() {
        corpo = png(7)
        assertEquals(7, larghezza())
        server.stop(0)
        assertEquals(7, larghezza())
    }

    /** Le regole della freschezza, una per riga. */
    @Test
    fun `la freschezza segue le intestazioni`() {
        val adesso = 1_000_000_000_000L
        assertNull("no-store non si tiene", RemoteCache.freshFor("no-store", 0, adesso))
        assertEquals("no-cache si chiede sempre", 0L, RemoteCache.freshFor("public, no-cache", 0, adesso))
        assertEquals(60_000L, RemoteCache.freshFor("public, max-age=60", 0, adesso))
        assertEquals("Il 10% dell'età", 100_000L, RemoteCache.freshFor(null, adesso - 1_000_000, adesso))
        assertEquals("Il tetto di un giorno", 86_400_000L, RemoteCache.freshFor(null, 1, adesso))
        assertEquals("Senza niente, dieci minuti", 600_000L, RemoteCache.freshFor(null, 0, adesso))
    }
}

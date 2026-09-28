package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Size
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.BitmapImage
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import coil3.request.allowHardware
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowMediaMetadataRetriever
import org.robolectric.shadows.util.DataSource
import java.io.File

/**
 * La miniatura di un video quando quella del sistema è nera o manca, dalla `2.98`.
 *
 * ⚠️⚠️ **NASCE DA UN DIFETTO ARRIVATO A LUI** (campo libero del giro della `2.93` e della `2.94`,
 * e in chat il 2026-09-27: *la miniatura in effetti c'è ed è nera*), quindi torna indietro con la
 * prova che lo avrebbe fermato, come vuole `Rules.md` § '🧪 Quando si scrive una prova, e
 * quando no'.
 *
 * ⚠️⚠️ **CHE COSA MISURA E CHE COSA NO.** Misura il riconoscimento del nero su bitmap scritte a
 * mano, l'ordine dei momenti da provare, la scelta del fotogramma con i fotogrammi finti di
 * `ShadowMediaMetadataRetriever`, e la catena delle miniature, cioè che la miniatura nera del
 * sistema passi la mano e che il fotogramma scelto resti su disco. **Non** misura quale fotogramma
 * scelga il telefono vero, né quanto costi aprire un video vero: quelli si guardano sul telefono.
 *
 * ⚠️ **La grafica è quella vera** (`NATIVE`): con quella di serie una bitmap non tiene i pixel che
 * le si scrivono, e il nero si riconoscerebbe su una tela che non esiste.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MiniaturaVideoTest {

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    // ⚠️ I fotogrammi finti vivono in mappe statiche dell'ombra: senza, una prova vedrebbe quelli
    // registrati dalla prova prima.
    @After
    fun ripulisci() = ShadowMediaMetadataRetriever.reset()

    /**
     * **Una miniatura tutta nera si riconosce, e anche quella quasi nera.**
     *
     * ⚠️ **Il quasi nero è il caso vero**: la compressione lascia sul nero di un video un rumore di
     * qualche livello, e una soglia a zero lo darebbe per un fotogramma con un disegno.
     * ⚠️ **Controprovata con la soglia a zero** (`> 0` al posto di `> DARK`): il grigio a 20 smette
     * di essere nero, e la prova cade.
     */
    @Test
    fun `una miniatura nera si riconosce anche con il rumore della compressione`() {
        assertTrue(ClipFrames.isBlack(tela(Color.BLACK)))
        assertTrue(ClipFrames.isBlack(tela(Color.rgb(20, 20, 20))))
    }

    /**
     * **Un titolo bianco su fondo nero non conta come nero.**
     *
     * ⚠️⚠️ **È LA RAGIONE PER CUI SI CONTANO I PUNTI E NON SI FA LA MEDIA**: una fascia bianca alta
     * il 4% della tela porta la media sotto dieci livelli, cioè 'nero' per una media, mentre è un
     * fotogramma con qualcosa da leggere.
     * ⚠️ **Controprovata con la media** (nero se la media dei punti è sotto la soglia): la fascia
     * passa per nera, e la prova cade.
     */
    @Test
    fun `un titolo bianco su nero non conta come nero`() {
        val titolo = tela(Color.BLACK).also { bmp ->
            Canvas(bmp).drawRect(0f, bmp.height * 0.48f, bmp.width.toFloat(), bmp.height * 0.52f, pieno(Color.WHITE))
        }
        assertFalse(ClipFrames.isBlack(titolo))
    }

    /**
     * **Pochi punti chiari non bastano, e un buio con un disegno sì.**
     *
     * ⚠️ Un quadratino bianco dello 0,25% della tela è un difetto, non un contenuto: la soglia è
     * l'1% dei punti. Un grigio a 40 invece è una scena buia, e resta una miniatura.
     * ⚠️ **Controprovata col confronto rovesciato** (nero se i punti chiari sono PIÙ dell'1%): la
     * prova cade, e con lei le altre cinque che guardano il nero.
     */
    @Test
    fun `pochi punti chiari non bastano e un buio con un disegno resta`() {
        val puntino = tela(Color.BLACK).also { bmp ->
            Canvas(bmp).drawRect(0f, 0f, bmp.width * 0.05f, bmp.height * 0.05f, pieno(Color.WHITE))
        }
        assertTrue(ClipFrames.isBlack(puntino))
        assertFalse(ClipFrames.isBlack(tela(Color.rgb(40, 40, 40))))
    }

    /**
     * **I momenti vanno dalla metà al decimo, e senza durata sono tre momenti fissi.**
     *
     * ⚠️ La metà per prima è la scelta scritta su [ClipFrames]: è il fotogramma che il sistema ha
     * usato per anni, ed è il più lontano da un'apertura in nero.
     * ⚠️ **Controprovata scambiando l'ordine** (un decimo per primo): la prova cade.
     */
    @Test
    fun `i momenti vanno dalla meta al decimo`() {
        assertArrayEquals(
            longArrayOf(30_000_000L, 15_000_000L, 45_000_000L, 6_000_000L),
            ClipFrames.times(60_000L)
        )
        assertArrayEquals(longArrayOf(1_000_000L, 5_000_000L, 15_000_000L), ClipFrames.times(null))
        assertArrayEquals(longArrayOf(1_000_000L, 5_000_000L, 15_000_000L), ClipFrames.times(0L))
    }

    /**
     * **Si prende il primo fotogramma che non è nero, e gli altri non si chiedono.**
     *
     * ⚠️ A metà il video è nero, a un quarto è verde e a tre quarti è blu: vince il verde, perché
     * viene prima nell'ordine.
     * ⚠️ **Controprovata tenendo il più chiaro invece del primo buono**: vince il blu, che ha più
     * punti chiari, e la prova cade.
     */
    @Test
    fun `sceglie il primo fotogramma che non e nero`() {
        val uri = Uri.parse("content://media/external/video/media/7")
        val fonte = DataSource.toDataSource(app, uri)
        ShadowMediaMetadataRetriever.addMetadata(fonte, MediaMetadataRetriever.METADATA_KEY_DURATION, "60000")
        fotogramma(fonte, 30_000_000L, tela(Color.BLACK))
        fotogramma(fonte, 15_000_000L, tela(Color.BLACK).also { mezzo(it, VERDE) })
        fotogramma(fonte, 45_000_000L, tela(BLU))

        val scelto = ClipFrames.pick(app, uri, BOX)

        assertEquals(VERDE, scelto?.getPixel(scelto.width / 2, scelto.height / 2))
    }

    /**
     * **Se sono neri tutti vince il meno buio.**
     *
     * ⚠️ Quattro fotogrammi neri con qualche punto chiaro in numero diverso: vince quello con più
     * punti, cioè il terzo momento, anche se viene dopo.
     * ⚠️ **Controprovata tenendo il primo**: vince quello di metà video, e la prova cade.
     */
    @Test
    fun `se sono tutti neri vince il meno buio`() {
        val uri = Uri.parse("content://media/external/video/media/8")
        val fonte = DataSource.toDataSource(app, uri)
        ShadowMediaMetadataRetriever.addMetadata(fonte, MediaMetadataRetriever.METADATA_KEY_DURATION, "60000")
        fotogramma(fonte, 30_000_000L, tela(Color.BLACK).also { punti(it, 2) })
        fotogramma(fonte, 15_000_000L, tela(Color.BLACK).also { punti(it, 1) })
        val meno = tela(Color.BLACK).also { punti(it, 3) }
        fotogramma(fonte, 45_000_000L, meno)
        fotogramma(fonte, 6_000_000L, tela(Color.BLACK))

        assertTrue("la prova non misura niente se il meno buio non è nero", ClipFrames.isBlack(meno))
        assertTrue(ClipFrames.pick(app, uri, BOX) === meno)
    }

    /**
     * **Un video che non si apre non dà niente**, e non fa cadere niente.
     *
     * ⚠️ **Controprovata togliendo il `catch`**: l'eccezione arriva alla prova, e la prova cade.
     */
    @Test
    fun `un video che non si apre non da niente`() {
        val uri = Uri.parse("content://media/external/video/media/9")
        ShadowMediaMetadataRetriever.addException(DataSource.toDataSource(app, uri), IllegalArgumentException("rotto"))

        assertNull(ClipFrames.pick(app, uri, BOX))
    }

    /**
     * **Nella catena delle miniature la miniatura nera del sistema passa la mano, e il fotogramma
     * scelto resta su disco.**
     *
     * ⚠️⚠️ **LA MINIATURA DI SISTEMA È QUELLA VERA DI `ThumbnailUtils`**, che sul banco chiede i
     * fotogrammi alla stessa ombra: il video dichiara 64 per 36, quindi più piccolo del riquadro, e
     * `createVideoThumbnail` prende la via di `getFrameAtTime`, dove c'è il fotogramma nero. Il
     * momento che chiede dipende dalla versione di Android della piattaforma finta, quindi il nero
     * si registra a tutti e due (`-1` e la metà).
     * ⚠️ **La seconda metà misura il disco**: tolti i fotogrammi finti, una catena nuova ritrova
     * quello scelto, e l'unico posto da cui può venire è [AvifCache].
     * ⚠️ **Controprovata in due modi**: togliendo il rifiuto del nero dal fetcher di sistema esce la
     * miniatura nera, e togliendo la scrittura su disco la seconda metà resta senza miniatura.
     */
    @Test
    fun `la miniatura nera del sistema passa la mano e il fotogramma scelto resta`() = runBlocking {
        val file = File(app.cacheDir, "nero-in-apertura.mp4").apply { writeBytes(ByteArray(16)) }
        val uri = file.toUri()
        val fonte = DataSource.toDataSource(file.absolutePath)
        ShadowMediaMetadataRetriever.addMetadata(fonte, MediaMetadataRetriever.METADATA_KEY_DURATION, "60000")
        ShadowMediaMetadataRetriever.addMetadata(fonte, MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH, "64")
        ShadowMediaMetadataRetriever.addMetadata(fonte, MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT, "36")
        val nero = Bitmap.createBitmap(64, 36, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLACK) }
        ShadowMediaMetadataRetriever.addFrame(fonte, -1L, nero)
        ShadowMediaMetadataRetriever.addFrame(fonte, 30_000_000L, nero)
        fotogramma(fonte, 30_000_000L, tela(Color.BLACK))
        fotogramma(fonte, 15_000_000L, tela(VERDE))

        val prima = miniatura(uri)
        assertEquals(VERDE, prima.getPixel(prima.width / 2, prima.height / 2))

        ShadowMediaMetadataRetriever.reset()
        val dopo = miniatura(uri)
        val c = dopo.getPixel(dopo.width / 2, dopo.height / 2)
        assertTrue(
            "dal disco doveva tornare il verde, e torna ${Integer.toHexString(c)}",
            Color.green(c) > 150 && Color.red(c) < 60 && Color.blue(c) < 60
        )
    }

    /** La miniatura di [uri] da una catena nuova, cioè senza la cache in memoria di un'altra. */
    private suspend fun miniatura(uri: Uri): Bitmap {
        val esito = Thumbs.loader(app).execute(Thumbs.request(app, uri).newBuilder().allowHardware(false).build())
        assertTrue("la catena si è fermata: ${(esito as? ErrorResult)?.throwable}", esito is SuccessResult)
        return ((esito as SuccessResult).image as BitmapImage).bitmap
    }

    /** Registra [bitmap] come fotogramma ridotto al riquadro [BOX], al momento [us]. */
    private fun fotogramma(fonte: DataSource, us: Long, bitmap: Bitmap) =
        ShadowMediaMetadataRetriever.addScaledFrame(fonte, us, BOX.width, BOX.height, bitmap)

    /** Una tela 16:9 di un colore solo, come una miniatura di video. */
    private fun tela(colore: Int): Bitmap =
        Bitmap.createBitmap(512, 288, Bitmap.Config.ARGB_8888).apply { eraseColor(colore) }

    /** Colora di [colore] il rettangolo centrale, grande un quarto della tela. */
    private fun mezzo(bmp: Bitmap, colore: Int) =
        Canvas(bmp).drawRect(bmp.width * 0.25f, bmp.height * 0.25f, bmp.width * 0.75f, bmp.height * 0.75f, pieno(colore))

    /**
     * Accende [quanti] punti della griglia di [ClipFrames], uno per cella lungo la prima riga.
     *
     * ⚠️ I punti cadono esattamente dove la griglia guarda, cioè al centro delle celle: così il
     * conteggio non dipende da un arrotondamento, e restano sotto l'1% che fa il nero.
     */
    private fun punti(bmp: Bitmap, quanti: Int) {
        val y = bmp.height / (2 * GRIGLIA)
        repeat(quanti) { i -> bmp.setPixel((2 * i + 1) * bmp.width / (2 * GRIGLIA), y, Color.WHITE) }
    }

    private fun pieno(colore: Int) = Paint().apply { color = colore; style = Paint.Style.FILL }

    private companion object {
        val BOX = Size(Thumbs.PX, Thumbs.PX)
        val VERDE = Color.rgb(0, 200, 0)
        val BLU = Color.rgb(0, 0, 200)

        /** Il lato della griglia di [ClipFrames.brightOf], ricopiato perché là è privato. */
        const val GRIGLIA = 32
    }
}

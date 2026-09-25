package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.GZIPOutputStream

/**
 * Che cosa il riconoscitore degli SVG prende per un SVG, e che cosa no.
 *
 * ⚠️⚠️ **NASCE DA UN FILE VERO, IL 2026-09-25**: un PNG di ChatGPT che si apriva e non aveva la
 * miniatura. Porta in testa il manifesto C2PA (il blocco `caBX`), e dentro il manifesto un'icona
 * SVG, col tag al byte 305: [Svg.looksLike] cercava il tag nei primi mille byte e rispondeva di
 * sì. Il perché quel sì costasse la miniatura vive sulla funzione. È il caso della regola scritta
 * in `AIV/CLAUDE.md` § '🧪 Quando si scrive una prova, e quando no': un difetto che è arrivato a
 * lui torna indietro con la prova che lo avrebbe fermato.
 *
 * ⚠️ **Il PNG si costruisce qui e non si prende dal file vero**: quello è un'immagine sua, e al
 * banco serve solo la forma che lo ha tradito, cioè un blocco ancillare con dentro il tag, allo
 * stesso byte e dietro le stesse due scatole del manifesto.
 *
 * ⚠️⚠️ **CHE COSA MISURA E CHE COSA NO.** Misura il riconoscitore sui byte, e la catena delle
 * miniature di Coil su un array. **Non** misura la scena del telefono, cioè un `content://` del
 * MediaStore: là la sorgente chiusa si scopre quando il decodificatore di serie prova a
 * riavvolgere il descrittore del file, e sul banco quel descrittore sarebbe di un provider finto.
 * Sull'array il meccanismo è lo stesso, e cambia solo il punto in cui la chiusura si vede.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RiconoscimentoTest {

    /**
     * **Un PNG col manifesto non è un SVG.**
     *
     * ⚠️ La prima asserzione è la condizione della prova: se il tag cadesse fuori dai byte che il
     * riconoscitore guarda, la risposta sarebbe no anche col difetto rimesso.
     * ⚠️ **Controprovata rimettendo il riconoscitore di prima** (il solo `contains`): cadono
     * questo caso, il gemello del JPEG e quello della miniatura, e gli SVG veri restano verdi.
     */
    @Test
    fun `un PNG col manifesto C2PA non si prende per un SVG`() {
        val testa = conManifesto(pngVero()).copyOf(Svg.SNIFF)

        assertEquals("il tag cade dove cadeva nel file vero", TAG_AT, testa.indexOf("<svg"))
        assertFalse(Svg.looksLike(testa))
    }

    /**
     * **Nemmeno un JPEG che porta il manifesto nel suo segmento APP11**, che è dove lo scrive chi
     * firma un JPEG: il caso gemello, che nella griglia non si vedeva solo perché la miniatura di
     * un JPEG arriva dal sistema e non passa dai decodificatori.
     */
    @Test
    fun `un JPEG col manifesto non si prende per un SVG`() {
        val manifesto = scatole() + SVG.toByteArray(Charsets.US_ASCII)
        val jpeg = bytes(0xFF, 0xD8, 0xFF, 0xEB) + corto(manifesto.size + 2) + manifesto

        assertTrue("il tag è nei byte guardati", jpeg.indexOf("<svg") in 0 until Svg.SNIFF)
        assertFalse(Svg.looksLike(jpeg))
    }

    /**
     * **Gli SVG veri restano SVG, in tutte le forme in cui arrivano.**
     *
     * ⚠️ Sono i casi che la condizione nuova poteva rompere, uno per ognuna delle cose che toglie
     * prima di guardare il primo segno: gli spazi che XML ammette, i tre BOM, la dichiarazione e
     * i commenti, e la compressione di un `.svgz`.
     * ⚠️ **Controprovata due volte**: senza togliere il BOM cade il caso di UTF-8, e senza gli
     * spazi di XML cade quello con l'a capo in testa.
     */
    @Test
    fun `gli SVG veri restano SVG`() {
        val nudo = SVG.toByteArray(Charsets.UTF_8)
        val casi = listOf(
            "nudo" to nudo,
            "con dichiarazione e commento" to
                ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!-- a mano -->\n$SVG").toByteArray(),
            "con spazi e a capo in testa" to ("\r\n\t  $SVG").toByteArray(),
            "col BOM di UTF-8" to bytes(0xEF, 0xBB, 0xBF) + nudo,
            "in UTF-16 little-endian col BOM" to bytes(0xFF, 0xFE) + SVG.toByteArray(Charsets.UTF_16LE),
            "in UTF-16 big-endian col BOM" to bytes(0xFE, 0xFF) + SVG.toByteArray(Charsets.UTF_16BE),
            "compresso, cioè un .svgz" to gzip(nudo),
        )

        casi.forEach { (come, file) -> assertTrue(come, Svg.looksLike(file)) }
    }

    /**
     * **E il PNG col manifesto ha la sua miniatura**, attraverso la stessa catena di decodificatori
     * della griglia.
     *
     * ⚠️ La richiesta non passa da [Thumbs.request] perché i dati sono un array e non un
     * indirizzo; la misura invece è la sua, [Thumbs.PX].
     * ⚠️ **Senza la correzione la catena si ferma con un errore**: il decodificatore SVG accetta il
     * file, il parser fallisce e chiude la sorgente, e il decodificatore di serie la trova chiusa.
     * Controprovato col riconoscitore di prima: l'errore è *BitmapFactory returned a null
     * bitmap*, cioè un decodificatore che legge una sorgente da cui non esce più niente.
     */
    @Test
    fun `il PNG col manifesto ha la miniatura`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val richiesta = ImageRequest.Builder(context)
            .data(conManifesto(pngVero()))
            .size(Thumbs.PX)
            .allowHardware(false)
            .build()

        val esito = Thumbs.loader(context).execute(richiesta)

        assertTrue("la catena si è fermata: ${(esito as? ErrorResult)?.throwable}", esito is SuccessResult)
        val immagine = (esito as SuccessResult).image
        // La misura è quella del riquadro, che Coil riempie, quindi conta la proporzione: una
        // miniatura quadrata vorrebbe dire l'icona del manifesto al posto della fotografia.
        assertEquals(Thumbs.PX, immagine.height)
        assertEquals(Thumbs.PX * LARGO / ALTO.toDouble(), immagine.width.toDouble(), 1.0)
    }

    /** Un PNG vero, con la trasparenza come il file che lo ha fatto nascere. */
    private fun pngVero(): ByteArray {
        val bitmap = Bitmap.createBitmap(LARGO, ALTO, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        bitmap.setPixel(LARGO / 2, ALTO / 2, Color.RED)
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            .toByteArray()
    }

    /**
     * Lo stesso PNG col blocco `caBX` subito dopo `IHDR`, che è dove lo mette il file vero.
     *
     * ⚠️ La prima lettera minuscola dichiara il blocco **ancillare**, quindi un decodificatore che
     * non lo conosce lo salta: è la ragione per cui il visualizzatore apriva il file.
     */
    private fun conManifesto(png: ByteArray): ByteArray {
        check(String(png, FIRMA + 4, 4, Charsets.US_ASCII) == "IHDR") { "il PNG non comincia con IHDR" }
        val dopoIhdr = FIRMA + BLOCCO_IHDR
        // I dati di un blocco cominciano otto byte dopo di lui: la lunghezza e il tipo.
        val dati = scatole(TAG_AT - dopoIhdr - 8) + SVG.toByteArray(Charsets.US_ASCII)
        return png.copyOfRange(0, dopoIhdr) + blocco("caBX", dati) + png.copyOfRange(dopoIhdr, png.size)
    }

    /**
     * Quello che nel file vero precede il tag, per nome: le scatole del manifesto in testa, e in
     * fondo la scatola che dichiara il tipo dell'icona e quella che ne apre i dati. In mezzo
     * zeri, fino a [quanti] byte.
     */
    private fun scatole(quanti: Int = 64): ByteArray {
        val testa = "jumbjumdc2pa".toByteArray(Charsets.US_ASCII)
        val coda = "bfdb".toByteArray(Charsets.US_ASCII) + bytes(0) +
            "image/svg+xml".toByteArray(Charsets.US_ASCII) + bytes(0) +
            "bidb".toByteArray(Charsets.US_ASCII)
        return testa + ByteArray(quanti - testa.size - coda.size) + coda
    }

    /** Un blocco PNG: lunghezza, tipo, dati e CRC del tipo più i dati. */
    private fun blocco(tipo: String, dati: ByteArray): ByteArray {
        val corpo = tipo.toByteArray(Charsets.US_ASCII) + dati
        val crc = CRC32().apply { update(corpo) }.value.toInt()
        return intero(dati.size) + corpo + intero(crc)
    }

    private fun intero(v: Int) =
        byteArrayOf((v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte())

    private fun corto(v: Int) = byteArrayOf((v ushr 8).toByte(), v.toByte())

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    private fun gzip(dati: ByteArray): ByteArray =
        ByteArrayOutputStream().also { fuori -> GZIPOutputStream(fuori).use { it.write(dati) } }
            .toByteArray()

    private fun ByteArray.indexOf(testo: String) = String(this, Charsets.ISO_8859_1).indexOf(testo)

    private companion object {
        /**
         * Le misure del PNG di prova, nella proporzione del file vero (992x1586).
         *
         * ⚠️ Più piccole del riquadro di proposito: con una misura dichiarata Coil ingrandisce fino
         * a riempirlo, quindi la miniatura esce alta [Thumbs.PX] e la sola cosa che la distingue
         * da un'icona quadrata è la proporzione.
         */
        const val LARGO = 31
        const val ALTO = 50

        /**
         * Il byte a cui il tag cadeva nel file vero.
         *
         * ⚠️ **Nel file, e non nel testo che il riconoscitore guarda**: là i 61 NUL che lo
         * precedono sono già tolti, e lo stesso tag cade al 244.
         */
        const val TAG_AT = 305

        /** La firma di un PNG, e il blocco `IHDR` che la segue sempre: 4 + 4 + 13 + 4. */
        const val FIRMA = 8
        const val BLOCCO_IHDR = 25

        /** L'icona del manifesto, con le misure che dichiarava nel file vero. */
        const val SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"716\" height=\"716\" " +
            "viewBox=\"0 0 716 716\"></svg>"
    }
}

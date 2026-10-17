package io.github.roccobot.aiv

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Il banco del **metodo colore di un JPEG**, cioè della riga che la scheda 'Info' scrive dal SOF.
 *
 * ⚠️⚠️ **NASCE DAL RILIEVO B27**: un JPEG a quattro componenti (CMYK o YCCK, il formato dei file
 * pronti per la stampa) si dichiarava RGB a 24 bit, proprio sui file che un grafico apre più
 * spesso. Fino alla 6.30 la risposta era nessuna; dalla 6.31 il modello ha la voce CMYK.
 * ⚠️⚠️ **E SCRIVENDOLA SI È VISTO CHE NESSUN JPEG AVEVA LA RIGA**: la lettura del SOF partiva dopo
 * i primi 1.024 byte, già consumati per riconoscere il formato, cioè a metà di un segmento, e
 * rispondeva sempre 'non lo so'. Prima della correzione erano rossi i tre casi a uno e a tre
 * componenti; quello del CMYK passava solo perché la risposta era sempre vuota, e diventa rosso
 * rimettendo 'RGB' sui quattro componenti.
 * ⚠️ **I file si costruiscono qui, byte per byte**: SOI, un segmento APP0 come quelli veri e poi
 * il SOF, che è tutto quello che la lettura guarda.
 */
class MetodoColoreTest {

    /** Un JPEG col SOF di [componenti] componenti a 8 bit, preceduto da un APP0 di [app0] byte. */
    private fun jpeg(componenti: Int, app0: Int = 16): ByteArray = ByteArrayOutputStream().apply {
        write(byteArrayOf(0xFF.toByte(), 0xD8.toByte()))
        write(byteArrayOf(0xFF.toByte(), 0xE0.toByte()))
        write(byteArrayOf(((app0 + 2) shr 8).toByte(), ((app0 + 2) and 0xFF).toByte()))
        write(ByteArray(app0))
        val lungo = 2 + 6 + 3 * componenti
        write(byteArrayOf(0xFF.toByte(), 0xC0.toByte(), (lungo shr 8).toByte(), (lungo and 0xFF).toByte()))
        write(byteArrayOf(8, 0, 16, 0, 16, componenti.toByte()))
        repeat(componenti) { write(byteArrayOf((it + 1).toByte(), 0x11, 0)) }
        write(byteArrayOf(0xFF.toByte(), 0xD9.toByte()))
    }.toByteArray()

    @Test
    fun `un JPEG a colori si dichiara RGB a 24 bit`() {
        val letto = coloursOf(ByteArrayInputStream(jpeg(3)))
        assertEquals("Un JPEG a tre componenti non si dichiara RGB", Colours.Model.RGB, letto?.model)
        assertEquals("Un JPEG a tre componenti non ha 24 bit", 24, letto?.bitsPerPixel)
    }

    @Test
    fun `un JPEG in grigio si dichiara in scala di grigi`() {
        val letto = coloursOf(ByteArrayInputStream(jpeg(1)))
        assertEquals("Un JPEG a un componente non si dichiara grigio", Colours.Model.GREY, letto?.model)
    }

    /**
     * ⚠️ **Dalla 6.31 il CMYK ha la sua riga** (scelta B1 dell'utente, `d-cmyk`): fino alla 6.30
     * la risposta era nessuna, e questa prova chiedeva `null`. Diventa rossa rimettendo 'RGB' sui
     * quattro componenti, o togliendo la voce.
     */
    @Test
    fun `un JPEG in CMYK si dichiara CMYK a 32 bit`() {
        val letto = coloursOf(ByteArrayInputStream(jpeg(4)))
        assertEquals("Un JPEG a quattro componenti non si dichiara CMYK", Colours.Model.CMYK, letto?.model)
        assertEquals("Un JPEG a quattro componenti non ha 32 bit", 32, letto?.bitsPerPixel)
    }

    @Test
    fun `un JPEG con un numero di componenti fuori formato resta senza risposta`() {
        assertNull("Un JPEG a due componenti si dichiara un metodo colore", coloursOf(ByteArrayInputStream(jpeg(2))))
    }

    /**
     * ⚠️ **Il SOF dopo i primi 1.024 byte**, come in ogni foto con l'EXIF: la lettura deve
     * trovarlo anche là, oltre la testa che si legge per riconoscere il formato.
     */
    @Test
    fun `il SOF dopo un EXIF lungo si trova`() {
        val letto = coloursOf(ByteArrayInputStream(jpeg(3, app0 = 4000)))
        assertEquals("Il SOF oltre la testa non si trova", Colours.Model.RGB, letto?.model)
    }
}

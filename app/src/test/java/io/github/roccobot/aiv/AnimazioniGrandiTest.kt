package io.github.roccobot.aiv

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Quanto si legge di un file per sapere se è un'animazione.
 *
 * ⚠️⚠️ **NASCE DA UN DIFETTO ARRIVATO A LUI** (giro della `5.70`: *un MKV di 250 MB ha fatto
 * andare AIV in crash*). Il visualizzatore chiede 'è animata?' per ogni pagina, filmati
 * compresi, e fino alla `5.70` la risposta leggeva tutto il file in memoria prima di guardarne
 * la firma e il peso. Il banco non ha un telefono con la memoria contata, quindi misura la
 * causa: quanti byte si leggono.
 * ⚠️ **Controprovata rimettendo il difetto** (il flusso letto per intero e poi guardato): il primo
 * caso legge tutti i 64 MB e cade, il secondo li legge tutti e cade.
 */
class AnimazioniGrandiTest {

    /** Un flusso di [size] byte che comincia con [head] e conta quanti ne consegna. */
    private class Contato(private val head: ByteArray, private val size: Long) : InputStream() {
        var letti = 0L
            private set

        override fun read(): Int {
            if (letti >= size) return -1
            val b = if (letti < head.size) head[letti.toInt()].toInt() and 0xFF else 0
            letti++
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (letti >= size) return -1
            val n = minOf(len.toLong(), size - letti).toInt()
            for (i in 0 until n) {
                b[off + i] = if (letti + i < head.size) head[(letti + i).toInt()] else 0
            }
            letti += n
            return n
        }
    }

    /** La testa di un filmato Matroska: la firma EBML, che non è né GIF né WebP. */
    private val mkv = byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte())

    @Test
    fun `di un filmato si leggono solo i primi byte`() {
        val flusso = Contato(mkv, 64L * 1024 * 1024)
        assertNull(Animations.candidate(flusso))
        assertTrue("letti ${flusso.letti} byte di un filmato", flusso.letti <= 64)
    }

    @Test
    fun `una GIF oltre il tetto si scarta senza leggerla tutta`() {
        val flusso = Contato("GIF89a".toByteArray(), 64L * 1024 * 1024)
        assertNull(Animations.candidate(flusso))
        assertTrue("letti ${flusso.letti} byte di una GIF enorme", flusso.letti < 40L * 1024 * 1024)
    }

    @Test
    fun `una GIF piccola si legge intera`() {
        val gif = "GIF89a".toByteArray() + ByteArray(1000) { it.toByte() }
        assertArrayEquals(gif, Animations.candidate(ByteArrayInputStream(gif)))
    }
}

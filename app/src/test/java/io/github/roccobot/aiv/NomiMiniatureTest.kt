package io.github.roccobot.aiv

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Il nome delle miniature su disco nella `6.27` (rilievo C8 della revisione della 6.25): due file
 * diversi non si scambiano più la miniatura.
 *
 * ⚠️ **La collisione è costruita, non cercata**: in Java `"Aa"` e `"BB"` hanno lo stesso
 * `hashCode`, e lo stesso vale per due testi che differiscono solo per quella coppia nello stesso
 * posto. Le due foto hanno anche la stessa data di modifica, che entra nella chiave.
 */
@RunWith(AndroidJUnit4::class)
class NomiMiniatureTest {

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * **Due file con lo stesso `hashCode` hanno due miniature diverse.**
     *
     * ⚠️ **Controprovata** col nome di prima, `hashCode` in esadecimale: i due percorsi coincidono.
     */
    @Test
    fun `due file non condividono la miniatura`() {
        val cartella = File(app.cacheDir, "nomi-miniature").also { it.mkdirs() }
        val a = File(cartella, "Aa.png").also { it.writeBytes(ByteArray(1)) }
        val b = File(cartella, "BB.png").also { it.writeBytes(ByteArray(1)) }
        a.setLastModified(1_000_000L)
        b.setLastModified(1_000_000L)
        val ua = Uri.fromFile(a)
        val ub = Uri.fromFile(b)
        assertEquals("la coppia deve collidere in hashCode", "$ua".hashCode(), "$ub".hashCode())

        assertNotEquals(AvifCache.fileFor(app, ua, 512), AvifCache.fileFor(app, ub, 512))
    }
}

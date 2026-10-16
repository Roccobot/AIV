package io.github.roccobot.aiv

import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Lo stato salvato di una selezione enorme nella `6.26` (rilievo E7 della revisione della 6.25):
 * resta lontano dal tetto di 1 MB della transazione con cui il sistema lo conserva.
 *
 * ⚠️ **Il peso si misura su un `Parcel` vero**, cioè lo stesso formato che il sistema manda nella
 * transazione: diecimila indirizzi del MediaStore, la selezione di una cartella grande con
 * 'Seleziona tutti'.
 * ⚠️ `RotazioneTest` prova insiemi piccoli, che il tetto lascia passare come prima.
 */
@RunWith(AndroidJUnit4::class)
class StatoSalvatoTest {

    private val tanti = (1..10_000).map {
        Uri.parse("content://media/external/images/media/$it")
    }

    /**
     * Quanti byte pesa nel `Parcel` quello che [saver] salva di [valore].
     *
     * ⚠️ `listSaver` salva `null` al posto di una lista vuota, cioè niente nello stato.
     */
    private fun <T> peso(saver: Saver<T, Any>, valore: T): Int {
        val salvato = with(saver) { SaverScope { true }.save(valore) } as? List<*>
        val bundle = Bundle()
        bundle.putParcelableArrayList("x", ArrayList(salvato.orEmpty().filterIsInstance<Uri>()))
        val parcel = Parcel.obtain()
        return try {
            bundle.writeToParcel(parcel, 0)
            parcel.dataSize()
        } finally {
            parcel.recycle()
        }
    }

    /**
     * **Diecimila indirizzi salvati pesano meno di 100 KB, nella selezione e nella finestra.**
     *
     * ⚠️ **Controprovata** togliendo il tetto da `UriSetSaver` e da `FileJobSaver`: il peso
     * supera il megabyte.
     */
    @Test
    fun `una selezione enorme non riempie lo stato salvato`() {
        val selezione = peso(UriSetSaver, tanti.toSet())
        val finestra = peso(FileJobSaver, FileJob.Transfer(tanti, move = false))

        assertTrue("selezione: $selezione byte", selezione < 100_000)
        assertTrue("finestra: $finestra byte", finestra < 100_000)
    }
}

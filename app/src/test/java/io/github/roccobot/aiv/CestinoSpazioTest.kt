package io.github.roccobot.aiv

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * La seconda richiesta del cestino della `2.86`: lo svuotamento dice quanto spazio ha liberato.
 *
 * ⚠️⚠️ **È SUA RICHIESTA** (2026-09-21: *quando si svuota il cestino, oltre al numero di file
 * eliminati, l'avviso deve dire anche quanti KB/MB/GB di archivio sono stati liberati*), e le
 * due metà si misurano separate: il conto dei byte e la frase che lo dice.
 * ⚠️⚠️ **VIVE IN UNA CLASSE SUA PERCHÉ QUI `OmbraArchivio` NON SI PUÒ DICHIARARE**: quell'ombra
 * si registra su `Environment` e **prende il posto** di quella di Robolectric, quindi con lei
 * `getExternalFilesDir` gira sul metodo di sistema e muore dentro `Environment` (misurato: prima
 * un `ArrayIndexOutOfBoundsException`, e dichiarando un volume un `NullPointerException`). Il
 * cestino vive proprio in quella cartella. Le prove della notifica invece montano la griglia e
 * l'ombra la vogliono, e il `@Config` di un metodo si somma a quello della classe senza poterlo
 * togliere.
 */
@RunWith(AndroidJUnit4::class)
class CestinoSpazioTest {

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * **Lo svuotamento conta i byte dei file andati via, e solo di quelli.**
     *
     * ⚠️ **È una prova su file veri**: il cestino è una cartella dell'app, quindi il banco ci può
     * scrivere tre file di peso noto e guardare che cosa torna.
     * ⚠️ **Il cestino si svuota prima**: un'altra prova può averci lasciato qualcosa, e allora il
     * conto sarebbe giusto e la prova rossa.
     * ⚠️ **Controprovata** togliendo la somma da `Bin.empty`: lo spazio torna zero.
     */
    @Test
    fun `lo svuotamento conta i byte che libera`() {
        runBlocking { Bin.empty(app) }
        val cartella = Bin.dir(app).also { it.mkdirs() }
        val pesi = listOf(1_000, 2_500, 40_000)
        pesi.forEachIndexed { i, n -> File(cartella, "prova-$i.jpg").writeBytes(ByteArray(n)) }

        val esito = runBlocking { Bin.empty(app) }

        assertEquals(pesi.size, esito.done)
        assertEquals(0, esito.failed)
        assertEquals(pesi.sum().toLong(), esito.freed)
        assertFalse("i file dovevano sparire", cartella.listFiles().orEmpty().any { it.isFile })
    }

    /**
     * **La frase dice lo spazio quando è misurato, e altrimenti è quella di sempre.**
     *
     * ⚠️ **Le due metà vanno insieme**: la prima presidia la sua richiesta, la seconda il fatto
     * che l'eliminazione di una selezione resta com'era, che è la parte che lui non ha chiesto di
     * cambiare.
     * ⚠️ **Lo spazio è scritto come il peso di una selezione**, cioè da `formatBytes`: il caso
     * confronta con quella funzione e non con un numero ricopiato, così il giorno che quel
     * formato cambia la prova resta verde e la notifica lo segue.
     * ⚠️ **Controprovata** togliendo il ramo da `outcomeText`: la frase perde lo spazio e il
     * primo confronto cade.
     */
    @Test
    fun `la frase dello svuotamento dice lo spazio liberato`() {
        val res = app.resources
        val svuotato = outcomeText(res, FileTree.Outcome(3, 0, freed = 45_000), FileKind.DELETE)
        assertEquals(
            res.getQuantityString(R.plurals.delete_freed, 3, 3, formatBytes(45_000)),
            svuotato
        )
        assertTrue("la frase doveva contenere '${formatBytes(45_000)}'", formatBytes(45_000) in svuotato)

        val selezione = outcomeText(res, FileTree.Outcome(3, 0), FileKind.DELETE)
        assertEquals(res.getQuantityString(R.plurals.delete_done, 3, 3), selezione)
    }
}

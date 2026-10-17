package io.github.roccobot.aiv

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Ogni finestra di dialogo dell'app è 'centrata' come le altre (dalla 6.30, rilievo F33 della
 * revisione Setaccio): `Modifier.lowered` la porta il 15% più in basso del centro e stende il velo
 * dietro, `loweredWindow` toglie la larghezza di serie di Android, che in orizzontale tagliava
 * l'ultimo tasto. La conferma per togliere una cartella dall'elenco non aveva né l'uno né l'altro,
 * e quattro conferme avevano il primo senza il secondo: nessun compilatore lo vede, quindi lo
 * guarda questa prova leggendo i sorgenti.
 */
class FinestreCentrateTest {

    @Test
    fun `ogni AlertDialog dell'app ha lowered e loweredWindow`() {
        val mancano = mutableListOf<String>()
        File("src/main/java").walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val righe = file.readLines()
            righe.forEachIndexed { i, riga ->
                if (!riga.contains("AlertDialog(") || riga.trimStart().startsWith("//") ||
                    riga.trimStart().startsWith("*")) return@forEachIndexed
                // Gli argomenti della finestra fino al titolo, compresa la riga che la apre: alcune
                // finestre li scrivono tutti lì.
                val testa = righe.drop(i).take(7).joinToString("\n")
                if (!testa.contains("Modifier.lowered") || !testa.contains("loweredWindow")) {
                    mancano += "${file.name}:${i + 1}"
                }
            }
        }
        assertEquals("finestre senza lowered o loweredWindow", emptyList<String>(), mancano)
    }
}

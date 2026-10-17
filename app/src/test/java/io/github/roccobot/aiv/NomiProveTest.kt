package io.github.roccobot.aiv

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Il nome di una prova contiene solo caratteri ASCII (`Rules.md` § '🧪 Quando si scrive una prova,
 * e quando no'): su una macchina con codifica di sistema stretta il rapporto non si genera, e il
 * build cade proprio quando una prova cade.
 *
 * ⚠️ La regola c'era, e nessuno la controllava: la 6.30 ha trovato 28 nomi con lettere accentate
 * (rilievo G17 della revisione Setaccio), cinque dei quali scritti nelle 6.26-6.29.
 */
class NomiProveTest {

    private fun root(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null && !File(dir, "app/src/test").isDirectory) dir = dir.parentFile
        return requireNotNull(dir) { "app/src/test non trovato" }
    }

    @Test
    fun `i nomi delle prove sono in ASCII`() {
        val nome = Regex("""fun `([^`]*)`""")
        val fuori = listOf("app/src/test", "app/src/testPlay")
            .flatMap { File(root(), it).walkTopDown().filter { f -> f.extension == "kt" }.toList() }
            .flatMap { file ->
                nome.findAll(file.readText()).map { it.groupValues[1] }
                    .filter { name -> name.any { it.code > 127 } }
                    .map { "${file.name}: $it" }
            }
        assertEquals("Nomi di prova con caratteri non ASCII", emptyList<String>(), fuori)
    }
}

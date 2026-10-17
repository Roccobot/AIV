package io.github.roccobot.aiv

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * L'elenco delle lingue per la pagina 'Lingua dell'app' di Android 13 e successivi
 * (`res/xml/locales_config.xml`, dalla 6.30): è scritto a mano, perché quello generato dalla build
 * elencava l'indonesiano due volte, quindi questa prova lo tiene allineato alle cartelle delle
 * traduzioni. Una lingua aggiunta senza la sua riga mancherebbe da quella pagina senza nessun
 * errore.
 */
class LingueAppTest {

    private val res = File("src/main/res")

    /** Il codice di una cartella di traduzioni: `values-pt-rPT` -> `pt-PT`, `values-b+es+419` -> `es-419`. */
    private fun codice(cartella: String): String {
        val resto = cartella.removePrefix("values-")
        return if (resto.startsWith("b+")) resto.removePrefix("b+").replace('+', '-')
        else resto.replace("-r", "-")
    }

    @Test
    fun `l'elenco delle lingue ha una riga per ogni cartella di traduzioni, e l'inglese`() {
        val cartelle = res.listFiles()!!
            .filter { it.isDirectory && it.name.startsWith("values-") && File(it, "strings.xml").isFile }
            .map { codice(it.name) }
        val attese = (cartelle + "en").toSortedSet()
        val elenco = Regex("""<locale android:name="([^"]+)"""")
            .findAll(File(res, "xml/locales_config.xml").readText())
            .map { it.groupValues[1] }
            .toList()
        assertEquals("una lingua è scritta due volte", elenco.size, elenco.toSet().size)
        assertEquals(attese, elenco.toSortedSet())
    }

    @Test
    fun `il manifest dichiara l'elenco delle lingue`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("""android:localeConfig="@xml/locales_config""""))
    }
}

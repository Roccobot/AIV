package io.github.roccobot.aiv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Banco della copia Impostazioni: apici → grassetto e ATTENZIONE → Attenzione
 * (giro 3.37-03 / 3.26-03), in tutte le sezioni via [emphasizeSettingsCopy].
 */
class SettingsCopyTest {

    @Test
    fun `apici diventano grassetto senza virgolette`() {
        val out = emphasizeSettingsCopy("Tocca 'Scarica' oppure 'Esporta/Converti'.")
        assertEquals("Tocca Scarica oppure Esporta/Converti.", out.text)
        assertTrue(out.spanStyles.any { it.start == out.text.indexOf("Scarica") })
        assertTrue(out.spanStyles.any { it.start == out.text.indexOf("Esporta/Converti") })
    }

    @Test
    fun `FAB resta tra apici`() {
        val out = emphasizeSettingsCopy("Il tasto fluttuante ('FAB') resta così.")
        assertTrue(out.text.contains("'FAB'"))
        assertFalse(out.spanStyles.any {
            out.text.substring(it.start, it.end) == "FAB"
        })
    }

    @Test
    fun `elisioni italiane non sono apici tipografici`() {
        val out = emphasizeSettingsCopy("Prima di sovrascrivere un'immagine, l'editor salva.")
        assertEquals("Prima di sovrascrivere un'immagine, l'editor salva.", out.text)
        assertTrue(out.spanStyles.isEmpty())
    }

    @Test
    fun `ATTENZIONE diventa Attenzione in grassetto`() {
        val out = emphasizeSettingsCopy("⚠️ ATTENZIONE: se disinstalli l'app, perdi il cestino.")
        assertEquals("⚠️ Attenzione: se disinstalli l'app, perdi il cestino.", out.text)
        assertTrue(out.spanStyles.any {
            out.text.substring(it.start, it.end) == "Attenzione"
        })
    }
}

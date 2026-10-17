package io.github.roccobot.aiv

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import java.io.File

/**
 * I modelli delle prove si chiudono (6.30, rilievo G16 della revisione Setaccio): `ModelliChiusi`
 * annulla i loro lavori alla fine della prova, e nessuna classe costruisce più il modello a mano.
 */
@RunWith(AndroidJUnit4::class)
class ModelliChiusiTest {

    @Test
    fun `alla fine della prova i lavori del modello sono annullati`() {
        val modelli = ModelliChiusi()
        val model = modelli.nuovo(ApplicationProvider.getApplicationContext<Application>())
        val lavori = model.viewModelScope.coroutineContext[Job]!!
        assertFalse("Il modello nasce già chiuso", lavori.isCancelled)
        val provaVuota = object : Statement() { override fun evaluate() {} }
        modelli.apply(provaVuota, Description.EMPTY).evaluate()
        assertTrue("I lavori del modello sono ancora vivi", lavori.isCancelled)
    }

    @Test
    fun `nessuna prova costruisce il modello a mano`() {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null && !File(dir, "app/src/test").isDirectory) dir = dir.parentFile
        val root = requireNotNull(dir)
        // Questo file nomina il costruttore per cercarlo, e l'aiuto lo usa per conto di tutte.
        val esclusi = setOf("ModelliChiusi.kt", "ModelliChiusiTest.kt")
        val aMano = listOf("app/src/test", "app/src/testPlay")
            .flatMap { File(root, it).walkTopDown().filter { f -> f.extension == "kt" }.toList() }
            .filter { it.name !in esclusi && it.readText().contains("ViewerViewModel(") }
            .map { it.name }
        assertEquals("Classi che costruiscono ViewerViewModel a mano", emptyList<String>(), aMano)
    }
}

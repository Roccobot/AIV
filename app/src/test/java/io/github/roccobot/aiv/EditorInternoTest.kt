package io.github.roccobot.aiv

import android.app.Application
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **Un solo 'Editor interno'**, dalla `5.01`, ed è la scelta dell'utente sulla nota di `5.00-02`.
 *
 * ⚠️⚠️ *Editor semplice obbligato sotto Android 13, tra le scelte non compare il completo; da
 * Android 13 in su dev'essere disponibile SOLO l'editor completo. In entrambi i casi l'editor si
 * chiamerà 'Editor interno'.* Fino alla `5.00` le voci erano due, e da Android 13 si poteva
 * scegliere anche il semplice.
 *
 * ⚠️ **Che cosa misura**: quale schermata apre la voce, sui due lati di Android 13, sia scelta
 * adesso ([Editors.INTERNAL]) sia salvata da una versione di prima ([Editors.FULL]); e che le due
 * scelte si chiamino tutte e due 'Editor interno'. ⚠️ **Che cosa non vede**: l'editor stesso, che
 * qui non si monta; la decisione è nel modello, e si misura dove la schermata finisce.
 */
@RunWith(AndroidJUnit4::class)
class EditorInternoTest {

    /** I modelli costruiti dalle prove, chiusi alla fine di ognuna (`ModelliChiusi`). */
    @get:Rule
    val modelli = ModelliChiusi()

    private val app get() = ApplicationProvider.getApplicationContext<Application>()

    /**
     * Sceglie [id] dal selettore aperto su una fotografia, e dice quale schermata si apre.
     *
     * ⚠️ **L'apertura passa da un lavoro in sottofondo** (il nome del file si legge fuori dal filo
     * principale), quindi si aspetta che la schermata cambi. Un indirizzo `file` dà il nome senza
     * toccare il disco.
     * ⚠️ **Se nessun editor si apre, la prova cade qui e lo dice**, con la schermata in cui è
     * rimasta: un'asserzione sul tipo, dopo, cadrebbe senza messaggio.
     */
    private fun apreCon(id: String): Screen {
        val model = modelli.nuovo(app)
        model.editWith(Uri.parse("file:///storage/emulated/0/Pictures/prova.jpg"))
        model.editorChosen(id)
        aspetta("nessun editor si è aperto con '$id'", dettaglio = { ": schermata ${model.screen}" }) {
            model.screen.let { it is Screen.Editor || it is Screen.FullEditor }
        }
        return model.screen
    }

    /** **Caso 1: da Android 13 l'editor interno è il completo**, anche per la scelta salvata. */
    @Test
    @Config(sdk = [36])
    fun `da Android 13 si apre il completo`() {
        assertTrue(apreCon(Editors.INTERNAL) is Screen.FullEditor)
        assertTrue(apreCon(Editors.FULL) is Screen.FullEditor)
    }

    /** **Caso 2: sotto Android 13 l'editor interno è il semplice**, anche per la scelta salvata. */
    @Test
    @Config(sdk = [28])
    fun `sotto Android 13 si apre il semplice`() {
        assertTrue(apreCon(Editors.INTERNAL) is Screen.Editor)
        assertTrue(apreCon(Editors.FULL) is Screen.Editor)
    }

    /**
     * **Caso 3: le due scelte si chiamano 'Editor interno'**, e un'app di fuori resta un'app di
     * fuori.
     */
    @Test
    fun `le due scelte si chiamano Editor interno`() {
        val nome = app.getString(R.string.editor_internal)
        assertEquals(nome, Editors.labelOf(app, Editors.INTERNAL))
        assertEquals(nome, Editors.labelOf(app, Editors.FULL))
        assertTrue(Editors.isInternal(Editors.FULL))
        assertFalse(Editors.isInternal("com.esempio/.Editor"))
        assertFalse(Editors.isInternal(""))
    }

    /**
     * **Caso 4: il nome di un'app di fuori si legge dal suo componente** (`6.28`, rilievo B13), sia
     * scelta come editor sia raggiunta condividendo; un'app disinstallata risponde `null`, e la
     * voce delle impostazioni torna a dire 'nessuna'.
     *
     * ⚠️ **L'app finta non risponde a nessuna domanda di modifica né di condivisione**: il nome
     * arriva dal solo componente, che è la strada nuova. Con quella di prima, che cercava il nome
     * nell'elenco intero, la stessa prova risponde `null` (controprovata).
     */
    @Test
    fun `il nome di un'app di fuori si legge dal componente`() {
        val pacchetto = PackageInfo().apply {
            packageName = "com.esempio.foto"
            applicationInfo = ApplicationInfo().apply { packageName = "com.esempio.foto" }
        }
        shadowOf(app.packageManager).installPackage(pacchetto)
        shadowOf(app.packageManager).addOrUpdateActivity(
            ActivityInfo().apply {
                packageName = "com.esempio.foto"
                name = "com.esempio.foto.Ritocca"
                nonLocalizedLabel = "Ritocca"
                applicationInfo = pacchetto.applicationInfo
            }
        )
        assertEquals("Ritocca", Editors.labelOf(app, "com.esempio.foto/.Ritocca"))
        assertEquals("Ritocca", Editors.labelOf(app, Editors.SHARED + "com.esempio.foto/.Ritocca"))
        assertNull(Editors.labelOf(app, "com.esempio.assente/.Editor"))
    }
}

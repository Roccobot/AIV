package io.github.roccobot.aiv

import android.app.Application
import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    private val app get() = ApplicationProvider.getApplicationContext<Application>()

    /**
     * Sceglie [id] dal selettore aperto su una fotografia, e dice quale schermata si apre.
     *
     * ⚠️ **L'apertura passa da un lavoro in sottofondo** (il nome del file si legge fuori dal filo
     * principale), quindi si aspetta che la schermata cambi, al più due secondi. Un indirizzo
     * `file` dà il nome senza toccare il disco.
     */
    private fun apreCon(id: String): Screen {
        val model = ViewerViewModel(app)
        model.editWith(Uri.parse("file:///storage/emulated/0/Pictures/prova.jpg"))
        model.editorChosen(id)
        repeat(200) {
            shadowOf(Looper.getMainLooper()).idle()
            val ora = model.screen
            if (ora is Screen.Editor || ora is Screen.FullEditor) return ora
            Thread.sleep(10)
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
}

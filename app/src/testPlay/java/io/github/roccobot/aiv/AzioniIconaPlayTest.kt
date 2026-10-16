package io.github.roccobot.aiv

import android.app.Application
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Le azioni dell'icona sulla schermata Home mandate da un'altra app, dalla `6.29` (rilievi C29 ed
 * E22 della revisione della 6.25, voce B9 del rapporto).
 *
 * ⚠️ **Una cartella che il telefono non ha non si apre**: nel banco il MediaStore è vuoto, quindi
 * ogni numero di cartella è sconosciuto, come quello inventato da un'app che vuole far mostrare una
 * cartella nascosta. ⚠️ **È una prova di AIV Play** per la ragione scritta su `ListaDiPrimaPlayTest`.
 * ⚠️ **Prima si aspettano le impostazioni**: il controllo le legge, e senza di loro l'azione resta
 * in attesa, quindi la prova passerebbe anche col controllo tolto.
 */
@RunWith(AndroidJUnit4::class)
class AzioniIconaPlayTest {

    private val main get() = shadowOf(Looper.getMainLooper())

    /** Fa girare il looper principale finché [fatto] non è vero, o per circa cinque secondi. */
    private fun aspetta(fatto: () -> Boolean = { false }) {
        main.idleFor(Duration.ofSeconds(1))
        repeat(250) {
            if (fatto()) return
            Thread.sleep(20)
            main.idle()
        }
    }

    /**
     * **L'azione 'cartella' con un numero che il telefono non ha non apre nessuna griglia.**
     *
     * ⚠️ **Controprovata** due volte: col ramo della `6.28`, che apriva la griglia su qualunque
     * numero, e col solo controllo forzato a vero dentro il ramo nuovo.
     */
    @Test
    fun `una cartella sconosciuta non si apre`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val model = ViewerViewModel(app)
        aspetta { model.settings != null }
        assertNotNull("Le impostazioni non sono arrivate", model.settings)
        val intento = Intent(Shortcuts.FOLDER)
            .putExtra(Shortcuts.BUCKET, 424242L)
            .putExtra(Shortcuts.NAME, "Nascosta")
        model.handleIntent(intento, fresh = false)
        aspetta { model.screen is Screen.Grid }
        assertFalse("Si è aperta una cartella sconosciuta", model.screen is Screen.Grid)
    }

    /**
     * **L'azione 'albero' accetta le sole cartelle dentro una memoria del telefono**, anche quando
     * il percorso comincia con la memoria e poi ne esce con `..`.
     *
     * ⚠️ **Controprovata** col confronto sul percorso scritto e non su quello canonico: il terzo
     * caso passava.
     */
    @Test
    fun `l'albero resta dentro le memorie`() {
        val model = ViewerViewModel(ApplicationProvider.getApplicationContext<Application>())
        val memoria = Tree.roots(ApplicationProvider.getApplicationContext()).firstOrNull()
        assertNotNull("Il banco non ha nessuna memoria", memoria)
        val base = memoria!!.file.absolutePath
        assertTrue("La memoria non è accettata", model.insideRoots(base))
        assertTrue("Una cartella della memoria non è accettata", model.insideRoots("$base/DCIM"))
        assertFalse("Un percorso che esce dalla memoria è accettato", model.insideRoots("$base/../.."))
        assertFalse("Una cartella fuori è accettata", model.insideRoots("/proc"))
    }
}

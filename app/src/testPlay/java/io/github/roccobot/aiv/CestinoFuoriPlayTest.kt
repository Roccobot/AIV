package io.github.roccobot.aiv

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * Il cestino di AIV Play aperto, quando il MediaStore cambia (dalla 6.30): in AIV Play il cestino è
 * quello del sistema, cioè righe del MediaStore, quindi un'altra app che lo svuota o ne ripristina
 * un elemento manda una notizia che lo riguarda, e la griglia del cestino si rilegge. Fino alla 6.29
 * il modello lo escludeva per una ragione che vale solo per AIV GitHub, dove il cestino è nella
 * cartella privata dell'app.
 *
 * ⚠️ **La spia è il lavoro di lettura della griglia**, preso per riflessione come il timbro delle
 * prove del Feed, e non la lista letta: nel banco il cestino è vuoto, la lista riletta è uguale a
 * quella di prima, e lo stato di Compose scarta una scrittura uguale tenendo l'oggetto vecchio.
 * Ogni rilettura invece parte con un lavoro nuovo.
 */
@RunWith(AndroidJUnit4::class)
class CestinoFuoriPlayTest {

    /** I modelli costruiti dalle prove, chiusi alla fine di ognuna (`ModelliChiusi`). */
    @get:Rule
    val modelli = ModelliChiusi()

    private val main get() = shadowOf(Looper.getMainLooper())

    private fun aspetta(fatto: () -> Boolean = { false }) {
        main.idleFor(Duration.ofSeconds(1))
        repeat(250) {
            if (fatto()) return
            Thread.sleep(20)
            main.idle()
        }
    }

    private fun ViewerViewModel.lettura(): Any? {
        val campo = ViewerViewModel::class.java.getDeclaredField("listJob").apply { isAccessible = true }
        return campo.get(this)
    }

    /**
     * **Col cestino aperto, una notizia dal MediaStore lo rilegge.**
     *
     * ⚠️ **Controprovata** col ramo della 6.29 (`screen == Screen.Bin -> Unit` per tutte e due le
     * varianti): il lavoro di lettura resta quello dell'apertura, e l'ultima asserzione cade.
     */
    @Test
    fun `col cestino aperto una notizia dal MediaStore lo rilegge`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val model = modelli.nuovo(app)
        aspetta { model.settings != null }
        model.openBin()
        aspetta { model.folder != null }
        val prima = model.lettura()
        assertNotNull("Il cestino non si è letto", prima)
        val timbro = model.outsideStamp

        app.contentResolver.notifyChange(Folder.TABLE, null)
        aspetta { model.lettura() !== prima }
        assertEquals("La notizia dal disco non è arrivata", timbro + 1, model.outsideStamp)
        assertNotSame("Il cestino non si è riletto dopo la notizia", prima, model.lettura())
    }
}

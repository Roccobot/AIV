package io.github.roccobot.aiv

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Il Feed durante una selezione nella `6.27` (rilievo D15 della revisione della 6.25): una notizia
 * dal disco non lo rilegge finché la selezione è viva, e lo rilegge quando finisce.
 *
 * ⚠️ **La spia è il timbro per cui il Feed è stato letto** (`feedFor`, privato, letto per
 * riflessione): nel banco il Feed è vuoto, e una pagina vuota riletta è uguale alla prima, quindi lo
 * stato di Compose non la sostituisce e da fuori non si vede niente. Il timbro è il numero delle
 * notizie dal disco (`outsideStamp`), ed è la chiave che decide se la lettura si rifà.
 * ⚠️ **È una prova di AIV Play** per la ragione scritta su `ListaDiPrimaPlayTest`: nella variante
 * GitHub il controllo del permesso va in errore nel banco.
 */
@RunWith(AndroidJUnit4::class)
class FeedOccupatoPlayTest {

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

    /** Il timbro delle notizie per cui il Feed è stato letto l'ultima volta. */
    private fun ViewerViewModel.feedStamp(): Any? {
        val campo = ViewerViewModel::class.java.getDeclaredField("feedFor").apply { isAccessible = true }
        return (campo.get(this) as List<*>)[1]
    }

    /**
     * **Con una selezione viva una notizia dal disco non rilegge il Feed; finita, lo rilegge.**
     *
     * ⚠️ **Controprovata** con la notizia che rileggeva sempre: il timbro della lettura sale durante
     * la selezione, e la prima asserzione cade.
     */
    @Test
    fun `la selezione nel Feed rimanda la rilettura`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val model = ViewerViewModel(app)
        aspetta { model.settings != null }
        model.goHome()
        model.readFeed(granted = false, selection = FolderSelection(), date = FeedDate.TAKEN)
        aspetta { model.feed != null }
        val prima = model.outsideStamp
        assertEquals(prima, model.feedStamp())

        model.feedBusy = true
        app.contentResolver.notifyChange(Folder.TABLE, null)
        aspetta { model.outsideStamp != prima }
        assertEquals("La notizia dal disco non è arrivata", prima + 1, model.outsideStamp)
        assertEquals("Il Feed si è riletto durante la selezione", prima, model.feedStamp())

        model.feedBusy = false
        assertEquals("Finita la selezione il Feed non si è riletto", prima + 1, model.feedStamp())
    }
}

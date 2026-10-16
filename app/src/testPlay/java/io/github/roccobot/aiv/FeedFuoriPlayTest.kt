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
 * Il Feed quando la casa non lo mostra, nella `6.28` (rilievo D16 della revisione della 6.25, voce
 * C11 del rapporto): una notizia dal disco non lo rilegge, e tornando sul Feed la lettura si rifà.
 *
 * ⚠️ **La spia è il timbro per cui il Feed è stato letto**, come in `FeedOccupatoPlayTest`, e per
 * la stessa ragione: nel banco il Feed è vuoto, e da fuori una rilettura non si vede.
 */
@RunWith(AndroidJUnit4::class)
class FeedFuoriPlayTest {

    private val main get() = shadowOf(Looper.getMainLooper())

    private fun aspetta(fatto: () -> Boolean = { false }) {
        main.idleFor(Duration.ofSeconds(1))
        repeat(250) {
            if (fatto()) return
            Thread.sleep(20)
            main.idle()
        }
    }

    private fun ViewerViewModel.feedStamp(): Any? {
        val campo = ViewerViewModel::class.java.getDeclaredField("feedFor").apply { isAccessible = true }
        return (campo.get(this) as List<*>?)?.get(1)
    }

    /**
     * **Lasciato il Feed, una notizia dal disco non lo rilegge; tornati, sì.**
     *
     * ⚠️ **Controprovata** senza [ViewerViewModel.leaveFeed]: il timbro della lettura sale con la
     * casa nella griglia, e la prima asserzione cade.
     */
    @Test
    fun `fuori dal Feed una notizia non lo rilegge`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val model = ViewerViewModel(app)
        aspetta { model.settings != null }
        model.goHome()
        model.readFeed(granted = false, selection = FolderSelection(), date = FeedDate.TAKEN)
        aspetta { model.feed != null }
        val prima = model.outsideStamp

        model.leaveFeed()
        app.contentResolver.notifyChange(Folder.TABLE, null)
        aspetta { model.outsideStamp != prima }
        assertEquals("La notizia dal disco non è arrivata", prima + 1, model.outsideStamp)
        assertEquals("Il Feed si è riletto fuori scena", prima, model.feedStamp())

        model.readFeed(granted = false, selection = FolderSelection(), date = FeedDate.TAKEN)
        assertEquals("Tornati sul Feed non si è riletto", prima + 1, model.feedStamp())
    }
}

package io.github.roccobot.aiv

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The writes to the preferences' store, from 6.25.
 *
 * ⚠️⚠️ **DataStore runs the change of a write on the caller's dispatcher, holding the store's write
 * lock** (`DataStoreImpl.transformAndWrite`, 1.2.1). A caller whose dispatcher stops leaves the lock
 * held, and every later write of the process waits forever: on GitHub `FeedTest` stood still for
 * four hours on its first write (6.24), because an earlier test had ended with a write half way and
 * its dispatcher, the test's own, stopped with it. The store's writes run on `Dispatchers.IO`
 * (`writeStore`, in `Settings.kt`).
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 */
@RunWith(AndroidJUnit4::class)
class ScritturaPreferenzeTest {

    /**
     * **A write whose caller stops does not stop the next one.** The first write starts on a test
     * dispatcher that runs once and never again, as a test's dispatcher does when the test ends;
     * the second, from another caller, has to arrive.
     * ⚠️ **Seen failing** with the writes back on the caller's dispatcher (`aivStore.edit` called
     * directly): the second write never arrives, and the case ends on its 10 s limit.
     */
    @Test
    fun `una scrittura rimasta a metà non ferma quelle dopo`() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        runBlocking { Hint.FEED.forget(app) }
        val fermo = StandardTestDispatcher()
        CoroutineScope(fermo).launch { Hint.FEED.remember(app) }
        // One step only: the write starts, and then nobody runs this dispatcher again.
        fermo.scheduler.runCurrent()
        val arrivata = runBlocking { withTimeoutOrNull(10_000) { Hint.FEED.forget(app); true } }
        assertEquals("La seconda scrittura non è arrivata", true, arrivata)
    }
}

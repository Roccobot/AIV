package io.github.roccobot.aiv

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.net.ServerSocket
import kotlin.concurrent.thread

/**
 * Il tocco su `Aggiorna`, dalla finestra fino alla fine del lavoro.
 *
 * ⚠️⚠️ **NASCE DA UN DIFETTO ARRIVATO A LUI** (giro della `5.70`, `5.70-01` non approvato:
 * *l'aggiornamento è stato rilevato, ed è stato anche scaricato, ma poi non succede niente*). Fino
 * alla `5.70` il lavoro partiva dentro la finestra (`rememberCoroutineScope`), e la prima cosa che
 * faceva era chiuderla: la finestra usciva di scena, il lavoro era annullato con lei, lo
 * scaricamento arrivava in fondo perché quel ciclo non si interrompe, e il passo dopo non partiva.
 * Nessun avviso, nessuna installazione.
 *
 * ⚠️ **Come si misura senza GitHub né installatore**: un server finto nella prova accetta la
 * connessione, aspetta più di quanto serve alla finestra per uscire di scena, e risponde che il
 * file non c'è. Lo scaricamento quindi fallisce *dopo* che la finestra non c'è più, e l'avviso
 * `Aggiornamento non riuscito` arriva solo se il lavoro le è sopravvissuto. È lo stesso punto in
 * cui, sul telefono, partiva l'installazione.
 * ⚠️ **Controprovata rimettendo il difetto** (il lavoro lanciato col `rememberCoroutineScope`
 * della finestra): l'avviso non arriva e la prova cade.
 */
@RunWith(AndroidJUnit4::class)
class AggiornaTest {

    @get:Rule
    val banco = createComposeRule()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private lateinit var server: ServerSocket

    @Before
    fun prepara() {
        Notices.line?.let { Notices.dismiss(it.id) }
        shadowOf(app.packageManager).setCanRequestPackageInstalls(true)
        server = ServerSocket(0)
        // ⚠️ Risponde un 404 vero dopo la pausa: una connessione chiusa senza risposta Java la
        // ritenta da sé, e il secondo tentativo resterebbe appeso fino al tempo massimo.
        thread(isDaemon = true) {
            runCatching {
                server.accept().use {
                    Thread.sleep(SILENZIO_MS)
                    it.getOutputStream().write(
                        "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray()
                    )
                }
            }
        }
    }

    @After
    fun ripulisci() {
        Updater.offer = null
        Notices.line?.let { Notices.dismiss(it.id) }
        server.close()
    }

    @Test
    fun `il lavoro sopravvive alla finestra che si chiude`() {
        Updater.offer = Updater.Release(
            version = "99.99",
            url = "http://127.0.0.1:${server.localPort}/AIV-99.99.apk",
            size = 1,
            sha256 = "00"
        )
        banco.setContent { UpdateDialog() }

        banco.onNodeWithText(app.getString(R.string.update_install)).performClick()
        banco.waitForIdle()
        assertNull("toccato 'Aggiorna', la finestra doveva chiudersi", Updater.offer)

        aspetta { Notices.line?.text == app.getString(R.string.update_failed) }
        assertEquals(app.getString(R.string.update_failed), Notices.line?.text)
    }

    /**
     * Fa girare il filo principale finché [fatto] non risponde di sì, per al più tre secondi.
     *
     * ⚠️ **Le due ruote insieme**: il ciclo di Android ([Looper]) e quello della regola di
     * Compose (`waitForIdle`), che può avere un suo distributore del filo principale. Girandone
     * una sola, il seguito del lavoro dopo lo scaricamento può restare in coda per sempre.
     */
    private fun aspetta(fatto: () -> Boolean) {
        repeat(300) {
            shadowOf(Looper.getMainLooper()).idle()
            banco.waitForIdle()
            if (fatto()) return
            Thread.sleep(10)
        }
        fail(
            "dopo la chiusura della finestra il lavoro non è arrivato in fondo: avviso " +
                "'${Notices.line?.text}'"
        )
    }

    private companion object {
        /** Quanto il server finto aspetta prima di rispondere. */
        const val SILENZIO_MS = 500L
    }
}

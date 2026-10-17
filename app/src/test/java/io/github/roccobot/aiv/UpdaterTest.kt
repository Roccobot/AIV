package io.github.roccobot.aiv

import android.content.Context
import android.content.pm.Signature
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * L'aggiornamento automatico di AIV GitHub (5.60, sue scelte C1 e D1): che cosa si propone e che
 * cosa no.
 *
 * ⚠️⚠️ **PROVA LE DECISIONI E NON LA RETE**: lo scaricamento e l'installazione passano da GitHub e
 * dall'installatore di Android, che il banco non ha. Quello che si può sbagliare in silenzio è
 * qui: proporre l'APK di AIV Play, proporre una versione più vecchia, installare un file senza
 * digest, controllare due volte nello stesso giorno, accettare una firma diversa.
 */
@RunWith(AndroidJUnit4::class)
class UpdaterTest {

    /** La risposta dell'API per una release con i due APK, come la scrive `release.yml`. */
    private fun answer(
        tag: String = "v5.60",
        digest: String? = "sha256:ABCDEF",
        play: Boolean = true,
        draft: Boolean = false
    ): String {
        val github = """{"name":"AIV-${tag.removePrefix("v")}.apk","size":11032813,""" +
            (digest?.let { """"digest":"$it",""" } ?: "") +
            """"browser_download_url":"https://example.org/AIV.apk"}"""
        val altro = """{"name":"AIV-Play-${tag.removePrefix("v")}.apk","size":10931081,""" +
            """"digest":"sha256:999","browser_download_url":"https://example.org/Play.apk"}"""
        val assets = if (play) "[$altro,$github]" else "[$github]"
        return """{"tag_name":"$tag","draft":$draft,"prerelease":false,"assets":$assets}"""
    }

    @Test
    fun `si propone l'APK di AIV GitHub, mai quello di AIV Play`() {
        // ⚠️ L'APK di AIV Play viene per primo di proposito: un elenco letto in ordine lo prenderebbe.
        val release = Updater.parse(answer())!!
        assertEquals("https://example.org/AIV.apk", release.url)
        assertEquals("5.60", release.version)
        assertEquals("abcdef", release.sha256)
    }

    @Test
    fun `senza digest la release non si propone`() {
        assertNull(Updater.parse(answer(digest = null)))
    }

    @Test
    fun `una bozza non si propone`() {
        assertNull(Updater.parse(answer(draft = true)))
    }

    @Test
    fun `le versioni si confrontano come numeri`() {
        assertTrue(Updater.newer("5.60", "5.51"))
        assertTrue(Updater.newer("6.00", "5.99"))
        // ⚠️ Il caso che un confronto fra testi sbaglierebbe: '1' viene prima di '9'.
        assertTrue(Updater.newer("10.00", "9.99"))
        assertFalse(Updater.newer("5.51", "5.51"))
        assertFalse(Updater.newer("5.50", "5.51"))
        assertFalse(Updater.newer("non so", "5.51"))
    }

    @Test
    fun `il controllo automatico si fa una volta per giorno di calendario`() {
        assertTrue(Updater.due(last = null, today = 100))
        assertTrue(Updater.due(last = 99, today = 100))
        assertFalse(Updater.due(last = 100, today = 100))
    }

    @Test
    fun `si installa solo un APK firmato con la stessa chiave`() {
        val nostra = Signature(byteArrayOf(1, 2, 3))
        val altra = Signature(byteArrayOf(9, 9, 9))
        assertTrue(Updater.sameSigners(setOf(nostra), setOf(Signature(byteArrayOf(1, 2, 3)))))
        assertFalse(Updater.sameSigners(setOf(nostra), setOf(altra)))
        assertFalse(Updater.sameSigners(setOf(nostra), setOf(nostra, altra)))
        assertFalse(Updater.sameSigners(emptySet(), emptySet()))
    }

    /**
     * **Gli APK già installati o più vecchi si cancellano dalla cache, dalla `6.29`** (rilievo E19
     * della revisione della 6.25, voce B15): resta solo quello di una versione più nuova, che si
     * può ancora installare. Prima di scaricare, resta solo quello che si sta scaricando.
     *
     * ⚠️ **Controprovata** senza la pulizia: tutti i file restavano.
     */
    @Test
    fun `la cache degli aggiornamenti tiene solo gli APK ancora installabili`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val vecchia = Updater.apkName("1.00")
        val questa = Updater.apkName(BuildConfig.VERSION_NAME)
        val nuova = Updater.apkName("999.00")
        val altra = Updater.apkName("998.00")
        listOf(vecchia, questa, nuova, altra).forEach { File(dir, it).writeText("apk") }

        Updater.sweep(context, keep = null)
        assertEquals(setOf(nuova, altra), dir.list()!!.toSet())

        Updater.sweep(context, keep = nuova)
        assertEquals(setOf(nuova), dir.list()!!.toSet())
    }

    @Test
    fun `AIV GitHub si aggiorna da solo`() {
        assertTrue(Store.updates)
    }
}

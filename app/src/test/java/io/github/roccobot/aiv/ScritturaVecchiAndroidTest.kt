package io.github.roccobot.aiv

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * AIV GitHub su Android 9 e 10: le cartelle si leggono e i comandi sui file possono scrivere.
 *
 * ⚠️⚠️ **NASCE DALLA SUA SCELTA B1 DEL 2026-10-15**, sulla voce del brief `Veterani`: fino alla
 * `6.22` la variante `github` chiedeva sotto Android 11 la sola lettura, quindi le cartelle si
 * vedevano e ogni comando che scrive con `java.io.File` (elimina, cestino, rinomina, copia, sposta)
 * falliva. Su Android 9 mancava il permesso di scrittura, su Android 10 anche l'archivio classico.
 * ⚠️ **Il banco non scrive su un archivio vero**: queste prove guardano i permessi chiesti, il
 * conto dell'accesso e il manifest; che la scrittura riesca davvero lo dice un telefono, e la voce
 * di collaudo lo dichiara.
 * ⚠️ **Controprova**: col `Store.kt` e il manifest della `6.22` cadono tutte e quattro.
 */
@RunWith(AndroidJUnit4::class)
class ScritturaVecchiAndroidTest {

    private val app get() = ApplicationProvider.getApplicationContext<Application>()

    private fun laSolaLetturaNonBasta() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
        assertEquals("Con la sola lettura i comandi sui file non scrivono", Store.Access.NONE, Store.access(app))
        shadowOf(app).grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        assertEquals(Store.Access.FULL, Store.access(app))
    }

    @Test
    @Config(sdk = [28])
    fun `su Android 9 l'accesso vuole lettura e scrittura`() = laSolaLetturaNonBasta()

    @Test
    @Config(sdk = [29])
    fun `su Android 10 l'accesso vuole lettura e scrittura`() = laSolaLetturaNonBasta()

    @Test
    @Config(sdk = [28])
    fun `la richiesta chiede lettura e scrittura insieme`() {
        assertEquals(
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            Store.dialogPermissions().toList()
        )
    }

    @Test
    @Config(sdk = [29])
    fun `il manifest dichiara la scrittura e l'archivio classico`() {
        val info = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
        assertTrue(
            "Manca il permesso di scrittura",
            Manifest.permission.WRITE_EXTERNAL_STORAGE in info.requestedPermissions.orEmpty()
        )
        // ⚠️ The method is hidden in the SDK the app compiles against, and present in the
        // platform the bench runs: the manifest's flag is read through it.
        val legacy = app.applicationInfo.javaClass.getMethod("hasRequestedLegacyExternalStorage")
        assertTrue(
            "Su Android 10 la scrittura vuole l'archivio classico",
            legacy.invoke(app.applicationInfo) as Boolean
        )
    }
}

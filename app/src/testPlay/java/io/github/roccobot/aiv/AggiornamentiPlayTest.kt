package io.github.roccobot.aiv

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/*
 * AIV Play does not update itself (5.60): the Play Store does it, and Google's policy does not
 * allow an app from the store to install packages.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️ Runs only in the `play` variant: see `AccessoPlayTest`.
 */
@RunWith(AndroidJUnit4::class)
class AggiornamentiPlayTest {

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `AIV Play non si aggiorna da sé`() {
        assertFalse(Store.updates)
    }

    @Test
    fun `AIV Play non chiede il permesso di installare pacchetti`() {
        val chiesti = app.packageManager
            .getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty()
        assertFalse(Manifest.permission.REQUEST_INSTALL_PACKAGES in chiesti)
    }

    @Test
    fun `anche chiesto a mano, il controllo non propone niente`() {
        // ⚠️ `Controlla ora` in AIV Play non c'è; la guardia dentro `check` è la seconda rete, e
        // con lei il banco non tocca la rete.
        runBlocking { Updater.check(app, asked = true) }
        assertNull(Updater.offer)
    }
}

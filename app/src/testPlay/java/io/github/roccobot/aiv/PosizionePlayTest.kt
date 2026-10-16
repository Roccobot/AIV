package io.github.roccobot.aiv

import android.Manifest
import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * La posizione delle foto in AIV Play nella `6.27` (sua scelta R1, rilievo C21 della revisione
 * della 6.25): la domanda del permesso comprende quello della posizione, e con quel permesso l'EXIF
 * si legge dall'originale, dove le coordinate ci sono.
 *
 * ⚠️ **Il banco non ha una foto vera con le coordinate**: la prova guarda le due cose che decidono
 * se la riga 'Posizione' può comparire, cioè che cosa si chiede e da quale indirizzo si legge.
 */
@RunWith(AndroidJUnit4::class)
class PosizionePlayTest {

    private val app: Application get() = ApplicationProvider.getApplicationContext()

    private val foto: Uri = Uri.parse("content://${MediaStore.AUTHORITY}/external/images/media/5")

    /**
     * **La domanda chiede anche la posizione, e col permesso l'EXIF si legge dall'originale.**
     *
     * ⚠️ **Controprovata** togliendo il permesso dalla domanda: la prima asserzione cade.
     */
    @Test
    fun `la posizione si chiede e si legge dall'originale`() {
        assertTrue(
            "La domanda del permesso non chiede la posizione",
            Manifest.permission.ACCESS_MEDIA_LOCATION in Store.dialogPermissions()
        )
        assertEquals("Senza permesso l'originale non si chiede", foto, withPlace(app, foto))

        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_MEDIA_LOCATION)
        assertEquals(MediaStore.setRequireOriginal(foto), withPlace(app, foto))
    }
}

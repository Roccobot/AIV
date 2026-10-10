package io.github.roccobot.aiv

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.app.ActivityOptionsCompat
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/*
 * AIV Play and the photo library (5.10): the permission it asks, and the partial grant.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️⚠️ **These scenes run only in the `play` variant**, because `BuildConfig.FILES` is decided at
 * build time: the class name ends in `PlayTest`, which is the filter `check.yml` and `release.yml`
 * pass to `testPlayDebugUnitTest`. Every scene mounts the real home screen (`Casa`).
 */
@RunWith(AndroidJUnit4::class)
class AccessoPlayTest {

    @get:Rule
    val banco = createAndroidComposeRule<ComponentActivity>()

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private fun voce(id: Int) = app.getString(id)

    private fun concedi(vararg permessi: String) = shadowOf(banco.activity.application).grantPermissions(*permessi)

    private fun quante(id: Int) = banco.onAllNodesWithText(voce(id)).fetchSemanticsNodes().size

    /** The onboarding veils take the first tap: here they count as already seen. */
    @Before
    fun veliGiaVisti() {
        runBlocking {
            Hint.COLUMNS.remember(app)
            Hint.BIN_EMPTY.remember(app)
        }
    }

    @Test
    fun `in AIV Play le funzioni sui file sono spente`() {
        assertFalse(Store.files)
    }

    /**
     * **Without the permission the sentence names images and videos**: the one of AIV GitHub names
     * all the files and a system page, and AIV Play asks neither.
     */
    @Test
    fun `senza permesso la frase nomina immagini e video`() {
        banco.setContent { AivTheme { Casa() } }
        banco.waitForIdle()
        assertEquals(1, quante(R.string.folders_permission_media))
        assertEquals(0, quante(R.string.folders_permission))
        assertEquals(0, quante(R.string.folders_partial))
    }

    /**
     * **'Consenti' opens the system dialog with the three media permissions**, the third one being
     * what makes Android 14 offer 'select photos'. AIV GitHub opens a settings page here.
     */
    @Test
    fun `Consenti chiede i tre permessi sulle foto`() {
        banco.setContent { AivTheme { Casa() } }
        banco.onNodeWithText(voce(R.string.folders_grant)).performClick()
        banco.waitForIdle()
        val chiesti = shadowOf(banco.activity).lastRequestedPermission?.requestedPermissions?.toList()
        assertEquals(
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ),
            chiesti
        )
    }

    /**
     * **After the second refusal the button opens the app's page in the system settings**:
     * Android no longer shows its dialog there, and answers 'no' at once. The registry stands in
     * for the system: it refuses every permission and keeps what the app launched.
     */
    @Test
    fun `dopo il secondo rifiuto Concedi apre la pagina di AIV nelle impostazioni`() {
        val lanciati = mutableListOf<Any?>()
        val registro = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?
            ) {
                lanciati += input
                if (contract is ActivityResultContracts.RequestMultiplePermissions) {
                    @Suppress("UNCHECKED_CAST")
                    dispatchResult(requestCode, (input as Array<String>).associateWith { false } as O)
                }
            }
        }
        val proprietario = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = registro
        }
        banco.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides proprietario) { AivTheme { Casa() } }
        }
        banco.onNodeWithText(voce(R.string.folders_grant)).performClick()
        banco.waitForIdle()
        val pagina = lanciati.last() as? Intent
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pagina?.action)
        assertEquals("package:${app.packageName}", pagina?.data.toString())
    }

    /**
     * **With the photos picked one by one, the home says so, and 'Consenti tutte' opens the app's
     * page in the system settings** (5.11, his note on `5.10-03`: *un tocco non rimanda alla vera
     * autorizzazione: torna alla selezione 'limitata'*). Up to 5.10 the button asked the permission
     * again, and Android answered with the choice of photos.
     */
    @Test
    fun `con le sole foto scelte Consenti tutte apre la pagina di AIV nelle impostazioni`() {
        concedi(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        val lanciati = mutableListOf<Any?>()
        val registro = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?
            ) {
                lanciati += input
            }
        }
        val proprietario = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = registro
        }
        banco.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides proprietario) { AivTheme { Casa() } }
        }
        banco.waitForIdle()
        assertEquals(1, quante(R.string.folders_partial))
        assertEquals(0, quante(R.string.folders_permission_media))
        banco.onNodeWithText(voce(R.string.folders_partial_all)).performClick()
        banco.waitForIdle()
        assertEquals("Consenti tutte ha aperto più di una cosa", 1, lanciati.size)
        val pagina = lanciati.single() as? Intent
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pagina?.action)
        assertEquals("package:${app.packageName}", pagina?.data.toString())
    }

    /**
     * **The partial row is aligned to the grid** (5.11, his note on `5.10-03`: *Consenti tutte è
     * allineato male*): the sentence starts where the covers start, the button's text ends where
     * they end. Up to 5.10 the row had 8dp more on the left, and the button 12dp of its own on the
     * right.
     */
    @Test
    fun `la riga dell'accesso parziale e allineata alla griglia`() {
        concedi(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        banco.setContent { AivTheme { Casa() } }
        banco.waitForIdle()
        val dp = app.resources.displayMetrics.density
        val larga = banco.onAllNodes(isRoot()).fetchSemanticsNodes().first().boundsInRoot.width
        fun bordi(id: Int) =
            banco.onAllNodesWithText(voce(id), useUnmergedTree = true).fetchSemanticsNodes().single().boundsInRoot
        assertEquals("La frase non comincia sul bordo della griglia", 12f * dp, bordi(R.string.folders_partial).left, 0.5f * dp)
        // ⚠️ In the unmerged tree the text's node is the text, not the touch target around it:
        // the edge that shows.
        assertEquals(
            "Consenti tutte non finisce sul bordo della griglia",
            larga - 12f * dp, bordi(R.string.folders_partial_all).right, 0.5f * dp
        )
    }

    /** **With the whole library granted, the partial row is gone.** */
    @Test
    fun `con tutta la libreria la riga non c'e`() {
        concedi(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        banco.setContent { AivTheme { Casa() } }
        banco.waitForIdle()
        assertEquals(0, quante(R.string.folders_partial))
        assertEquals(0, quante(R.string.folders_permission_media))
    }
}

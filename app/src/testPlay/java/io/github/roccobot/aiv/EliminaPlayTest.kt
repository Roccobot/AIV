package io.github.roccobot.aiv

import android.app.Activity
import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.app.RemoteAction
import android.graphics.drawable.Icon
import android.os.Build
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

/*
 * 'Elimina' in AIV Play (5.20): from Android 11 the files go into the system's trash, through the
 * system's window (his choice A1).
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️ The registry stands in for the system: it keeps what the app launched and answers yes or no.
 * ⚠️ Runs only in the `play` variant: see `AccessoPlayTest`.
 */
@RunWith(AndroidJUnit4::class)
class EliminaPlayTest {

    @get:Rule
    val banco = createAndroidComposeRule<ComponentActivity>()

    private val foto: Uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "7")

    /**
     * The MediaStore's provider as far as the requests go: it answers a request with a window and
     * keeps which request it was. Robolectric has no MediaStore, and `createTrashRequest` asks the
     * provider for its window with a `call`.
     */
    class Media : ContentProvider() {
        override fun onCreate() = true
        override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
            chieste += method
            val window = PendingIntent.getActivity(context!!, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
            return Bundle().apply { putParcelable("result", window) }
        }
        override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        /** The first deletion of a file of another app asks for consent, as Android 10 does. */
        override fun delete(uri: Uri, s: String?, a: Array<out String>?): Int {
            cancellazioni++
            if (altrui && cancellazioni == 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val window = PendingIntent.getActivity(context!!, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
                val action = RemoteAction(Icon.createWithData(ByteArray(0), 0, 0), "consenso", "consenso", window)
                throw RecoverableSecurityException(SecurityException("altrui"), "altrui", action)
            }
            return 1
        }
        override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0

        companion object {
            val chieste = mutableListOf<String>()
            var altrui = false
            var cancellazioni = 0
        }
    }

    /** A [SystemAsk] that answers [yes] to everything, and counts the windows and the permissions. */
    private class Risponde(val yes: Boolean) {
        val finestre = mutableListOf<IntentSenderRequest>()
        val permessi = mutableListOf<String>()
        lateinit var ask: SystemAsk
        fun build(context: android.content.Context) = SystemAsk(
            context,
            { finestre += it; ask.answer(yes) },
            { permessi += it; ask.answer(yes) }
        ).also { ask = it }
    }

    @Before
    fun fornitore() {
        Media.chieste.clear()
        Media.altrui = false
        Media.cancellazioni = 0
        Robolectric.buildContentProvider(Media::class.java).create(MediaStore.AUTHORITY)
    }

    /** Mounts the dialogs on a deletion, with a system that answers [yes]; returns what was launched and run. */
    private fun elimina(yes: Boolean): Pair<List<Any?>, List<Pair<FileKind, FileTree.Outcome>>> {
        val lanciati = mutableListOf<Any?>()
        val fatti = mutableListOf<Pair<FileKind, FileTree.Outcome>>()
        val registro = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?
            ) {
                lanciati += input
                @Suppress("UNCHECKED_CAST")
                dispatchResult(requestCode, ActivityResult(if (yes) Activity.RESULT_OK else Activity.RESULT_CANCELED, null) as O)
            }
        }
        val proprietario = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = registro
        }
        banco.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides proprietario) {
                FileJobDialogs(
                    job = FileJob.Delete(listOf(foto), forGood = false),
                    fields = emptyList(),
                    onClose = {},
                    onRun = { kind, work -> fatti += kind to runBlocking { work() } }
                )
            }
        }
        /*
         * ⚠️⚠️ **`waitForIdle` alone is not enough, and it failed once on 5.30**: the effect reads
         * the MediaStore rows on `Dispatchers.IO`, which the test clock does not wait for, so the
         * check could run before the system's window was launched (`0` launches where `1` was
         * due). The test waits for the launch, and then for the composition to settle.
         */
        banco.waitUntil(timeoutMillis = 5_000) { lanciati.isNotEmpty() }
        banco.waitForIdle()
        return lanciati to fatti
    }

    /** **A yes in the system's window: one file in the trash, said by the outcome.** */
    @Test
    fun `col si di Android il file va nel cestino di sistema`() {
        val (lanciati, fatti) = elimina(yes = true)
        assertTrue("Non si è chiesto il cestino: ${Media.chieste}", Media.chieste.single().contains("trash"))
        assertTrue("Non si è aperta la finestra del cestino: $lanciati", lanciati.single() is IntentSenderRequest)
        assertEquals(listOf(FileKind.TRASH to FileTree.Outcome(1, 0)), fatti)
        assertTrue("L'esito non si dice", FileKind.TRASH.speaks(fatti.single().second))
    }

    /**
     * **On Android 10 a file of another app is deleted after Android's consent** (his choice B1):
     * the first deletion answers with the consent's window, and after a yes it is tried again.
     */
    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `su Android 10 il file di un'altra app si elimina dopo il consenso`() {
        Media.altrui = true
        val sistema = Risponde(yes = true)
        val esito = runBlocking { MediaDelete.forGood(banco.activity, listOf(foto), sistema.build(banco.activity)) }
        assertEquals(FileTree.Outcome(1, 0), esito)
        assertEquals("Il consenso non è stato chiesto una volta sola", 1, sistema.finestre.size)
        assertEquals(2, Media.cancellazioni)
    }

    /** **On Android 9 the write permission comes first, and without it nothing is deleted.** */
    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun `su Android 9 senza il permesso di scrittura non si elimina niente`() {
        val sistema = Risponde(yes = false)
        val esito = runBlocking { MediaDelete.forGood(banco.activity, listOf(foto), sistema.build(banco.activity)) }
        assertEquals(FileTree.Outcome(0, 1), esito)
        assertEquals(listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), sistema.permessi)
        assertEquals(0, Media.cancellazioni)
    }

    /** **A no touches nothing and says nothing.** */
    @Test
    fun `col no di Android non succede niente`() {
        val (lanciati, fatti) = elimina(yes = false)
        assertEquals(1, lanciati.size)
        assertTrue("Col no è partito un lavoro: $fatti", fatti.isEmpty())
    }
}

package io.github.roccobot.aiv

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Looper
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.shadows.ShadowEnvironment

/**
 * Le due azioni dell'icona che portano a una cartella, quando la cartella è di quelle che il
 * telefono ha: l'ultima cartella aperta (6.10, scelta B1) e la cartella messa sulla schermata Home
 * ('Faro', 6.20).
 *
 * ⚠️ **Dalla `6.29` le due azioni controllano quello che ricevono** (voce B9; i rifiuti li prova
 * `AzioniIconaPlayTest`): la cartella deve esistere e la selezione mostrarla, il percorso deve
 * essere dentro una memoria del telefono. Fino alla `6.28` le due prove vivevano in
 * `ScorciatoieTest` con un numero e un percorso inventati, che il controllo ora rifiuta; là
 * l'archivio è chiuso di proposito (`OmbraArchivio` di serie), e senza archivio il banco non ha né
 * cartelle né memorie. Qui è aperto (`OmbraArchivio.granted`), e un fornitore finto serve la
 * cartella 42 `Camera`.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [OmbraArchivio::class])
class AzioniIconaTest {

    /** Il permesso sui file concesso: vedi [OmbraArchivio.granted]. */
    @Before
    fun archivioAperto() {
        OmbraArchivio.granted = true
    }

    /** I modelli costruiti dalle prove, chiusi alla fine di ognuna (`ModelliChiusi`). */
    @get:Rule
    val modelli = ModelliChiusi()

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val main get() = shadowOf(Looper.getMainLooper())

    @Before fun fornitore() {
        ShadowContentResolver.registerProviderInternal("media", UnaCartellaProvider())
    }

    /** Fa girare il looper principale finché [fatto] non è vero, o per circa cinque secondi. */
    private fun aspetta(fatto: () -> Boolean) {
        main.idleFor(Duration.ofSeconds(1))
        repeat(250) {
            if (fatto()) return
            Thread.sleep(20)
            main.idle()
        }
    }

    /** L'ultima cartella riapre la sua griglia, col nome che l'azione porta con sé. */
    @Test
    fun `l'ultima cartella riapre la sua griglia`() {
        val model = modelli.nuovo(app)
        aspetta { model.settings != null }
        assertNotNull("Le impostazioni non sono arrivate", model.settings)
        val intent = Intent(Shortcuts.FOLDER)
            .putExtra(Shortcuts.BUCKET, 42L)
            .putExtra(Shortcuts.NAME, "Camera")
        model.handleIntent(intent, fresh = true)
        aspetta { model.screen is Screen.Grid }
        assertEquals(Screen.Grid(42L, "Camera"), model.screen)
    }

    /**
     * **Una cartella sulla Home si apre in 'Cartelle di sistema' per una volta**, al suo percorso, e
     * l'app aperta dalla sua icona torna alla vista salvata.
     *
     * ⚠️ Controprovata, quando viveva in `ScorciatoieTest`, col ramo dell'azione tolto: la schermata
     * era la casa nella vista salvata, e l'albero restava in cima.
     */
    @Test
    fun `una cartella sulla Home si apre nelle cartelle di sistema per una volta`() {
        // ⚠️ Robolectric has no storage until one is declared, and Tree.roots takes only one whose
        // folder can be read.
        ShadowEnvironment.addExternalDir("memoria").mkdirs()
        val model = modelli.nuovo(app)
        val memoria = Tree.roots(app).firstOrNull()
        assertNotNull("Il banco non ha nessuna memoria", memoria)
        val percorso = memoria!!.file.absolutePath + "/DCIM/Camera"
        model.handleIntent(Intent(Shortcuts.TREE).putExtra(Shortcuts.PATH, percorso), fresh = true)
        assertEquals(FolderView.TREE, model.shownView(FolderView.GRID))
        assertEquals(percorso, model.treePath)
        model.handleIntent(Intent(Intent.ACTION_MAIN), fresh = false)
        assertEquals(FolderView.GRID, model.shownView(FolderView.GRID))
    }
}

/** Una sola immagine, nella cartella 42 `Camera`. */
class UnaCartellaProvider : ContentProvider() {
    override fun onCreate() = true
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val columns = projection ?: arrayOf("_id", "_data", "media_type", "bucket_id", "bucket_display_name")
        return MatrixCursor(columns).apply {
            addRow(columns.map { name -> when (name) {
                "_id" -> 1
                "_data" -> "/sdcard/DCIM/Camera/image.jpg"
                "media_type" -> MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                "bucket_id" -> 42L
                "bucket_display_name" -> "Camera"
                "date_modified" -> 1L
                else -> null
            } })
        }
    }
    override fun getType(uri: Uri): String = "image/jpeg"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
}

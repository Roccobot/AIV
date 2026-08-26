package io.github.roccobot.aiv

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/*
 * The settings, and which ones exist.
 *
 * The list is a subset of the ones 'Decent Image Viewer' exposes, plus two that
 * only make sense here. Choosing the subset is the whole design of this file, so
 * the reasons are written down rather than left to be guessed:
 *
 * - The **language** is deliberately absent. Android picks it from the system and
 *   the strings live in values/ and values-it/: a switch inside the app would be
 *   a second mechanism doing what the platform already does, and the two would
 *   disagree the first time one of them was touched.
 * - Everything about the **wheel** is gone, and it is most of the userscript's
 *   panel: direction, step, mode, sensitivity. There is no wheel on a phone, and
 *   pinch is handled by the system with its own tuning.
 * - The **SVG export** settings (two DPI values) have nothing to act on yet: this
 *   viewer does not do SVG.
 * - The **text nudge** is desktop typography, measured on a font this app never
 *   loads.
 *
 * What stays is what a finger can reach and a phone can change.
 */

/** One picture the app has opened before, as the opening screen lists it. */
data class RecentImage(val address: String, val name: String)

/**
 * What every stored choice has: a token that outlives a rename.
 *
 * ⚠️ The stored value is the token and NOT the enum constant's name. The two look
 * identical today and part company the day a constant is renamed: with the name,
 * every phone that had saved a value would silently fall back to the default, and
 * the setting would look like it had never been touched.
 */
interface Choice {
    val token: String
}

/** How the space behind the picture is painted. */
enum class BgType(override val token: String) : Choice {
    /** The checkerboard, which is what makes transparency visible. */
    CHECKER("checker"),
    /** One flat colour, for looking at a photograph without a pattern under it. */
    SOLID("solid")
}

/**
 * Which pair of greys the background uses. Separate from [BgType] on purpose:
 * they are independent axes, and merging them would force a choice between
 * transparency and colour, losing the light checkerboard entirely. The userscript
 * made that mistake once and had to undo it.
 */
enum class BgTheme(override val token: String) : Choice { AUTO("auto"), LIGHT("light"), DARK("dark") }

/**
 * What '100%' means, and on a phone the two answers are far apart.
 *
 * A phone screen has two to three device pixels per layout pixel, so an image
 * shown at one image pixel per DEVICE pixel is a third of the size it would be at
 * one image pixel per LAYOUT pixel. Neither is wrong: [PHYSICAL] shows the file
 * as it really is, [LOGICAL] shows it the size a web page would.
 */
enum class ScaleMode(override val token: String) : Choice { PHYSICAL("physical"), LOGICAL("logical") }

/** Where the one line of details sits. Asked for by the user. */
enum class InfoPosition(override val token: String) : Choice { TOP("top"), BOTTOM("bottom") }

// ⚠️ QUI VIVEVA `SearchEngine`, il motore della ricerca immagine, tolto nella
// 0.18 insieme alla funzione (istruzione dell'utente dopo la prova sul telefono).
// La chiave `search-engine` può essere rimasta scritta nell'archivio dei telefoni
// che avevano la versione vecchia: non si legge più, e non è un difetto da
// inseguire, perché DataStore ignora le chiavi che nessuno chiede.

data class Settings(
    val bgType: BgType = BgType.CHECKER,
    val bgTheme: BgTheme = BgTheme.AUTO,
    /** Whether a picture smaller than the view grows to fill it. Off, as in the userscript: blowing up a 64px icon helps nobody. */
    val fitGrow: Boolean = false,
    val zoomMax: Float = 40f,
    val scaleMode: ScaleMode = ScaleMode.PHYSICAL,
    val infoPosition: InfoPosition = InfoPosition.BOTTOM,
    val infoVisible: Boolean = true,
)

/**
 * One store for everything the app remembers, declared at file level so that both
 * the settings and the recents can reach it.
 *
 * ⚠️ It has to be ONE: two `preferencesDataStore` delegates over the same name
 * crash at the first read with 'there are multiple DataStores active for the same
 * file', and two different names would mean two files for one small set of
 * preferences.
 */
private val Context.aivStore: DataStore<Preferences> by preferencesDataStore(name = "aiv-settings")

/** Reads and writes the settings. */
object SettingsStore {

    private val BG_TYPE = stringPreferencesKey("bg-type")
    private val BG_THEME = stringPreferencesKey("bg-theme")
    private val FIT_GROW = booleanPreferencesKey("fit-grow")
    private val ZOOM_MAX = floatPreferencesKey("zoom-max")
    private val SCALE_MODE = stringPreferencesKey("scale-mode")
    private val INFO_POSITION = stringPreferencesKey("info-position")
    private val INFO_VISIBLE = booleanPreferencesKey("info-visible")

    /** Bounds of the only numeric setting, so a stored value out of range cannot reach the viewer. */
    const val ZOOM_MAX_MIN = 2f
    const val ZOOM_MAX_MAX = 200f

    fun flow(context: Context): Flow<Settings> = context.aivStore.data.map { p ->
        Settings(
            bgType = BgType.entries.byToken(p[BG_TYPE], BgType.CHECKER),
            bgTheme = BgTheme.entries.byToken(p[BG_THEME], BgTheme.AUTO),
            fitGrow = p[FIT_GROW] ?: false,
            zoomMax = (p[ZOOM_MAX] ?: 40f).coerceIn(ZOOM_MAX_MIN, ZOOM_MAX_MAX),
            scaleMode = ScaleMode.entries.byToken(p[SCALE_MODE], ScaleMode.PHYSICAL),
            infoPosition = InfoPosition.entries.byToken(p[INFO_POSITION], InfoPosition.BOTTOM),
            infoVisible = p[INFO_VISIBLE] ?: true,
        )
    }

    suspend fun save(context: Context, settings: Settings) {
        context.aivStore.edit { p ->
            p[BG_TYPE] = settings.bgType.token
            p[BG_THEME] = settings.bgTheme.token
            p[FIT_GROW] = settings.fitGrow
            p[ZOOM_MAX] = settings.zoomMax.coerceIn(ZOOM_MAX_MIN, ZOOM_MAX_MAX)
            p[SCALE_MODE] = settings.scaleMode.token
            p[INFO_POSITION] = settings.infoPosition.token
            p[INFO_VISIBLE] = settings.infoVisible
        }
    }
}

/**
 * A token that is unknown, or missing, falls back to the default instead of
 * throwing: a stored file that a future version does not recognise must not stop
 * the app from starting.
 */
private fun <T : Choice> List<T>.byToken(token: String?, fallback: T): T =
    if (token == null) fallback else firstOrNull { it.token == token } ?: fallback

/**
 * The pictures AIV has opened, most recent first.
 *
 * ⚠️⚠️ This exists INSTEAD of the thing that was asked for, which was to look at
 * what has recently been visited in Chrome or Brave and offer the pictures from
 * those pages. That cannot be done, and not for want of a permission to request:
 * a browser's history lives in its own private storage, and the public provider
 * that once exposed it (`Browser.BOOKMARKS_URI`, behind
 * READ_HISTORY_BOOKMARKS) was cut off in Android 6 precisely so that one app
 * could not read another's browsing. No app can offer that list, so this offers
 * the nearest true thing: what you opened here.
 *
 * ⚠️ Only web addresses are remembered, and a local file deliberately is not. The
 * permission on a `content://` handed over by another app lasts as long as that
 * intent does, so a remembered local picture would be a row that looks openable
 * and fails when tapped. A list that lies is worse than a shorter one.
 */
/**
 * Whether the folder permission has already been asked for once.
 *
 * ⚠️ Sta qui e non fra le impostazioni perché NON è una scelta dell'utente: è un
 * promemoria dell'app a sé stessa. Serve perché il permesso si chiede **una volta
 * sola**, alla prima immagine locale aperta: un sistema che chiede e richiede è
 * quello che insegna a rifiutare per riflesso, e chi ha detto no una volta ha
 * detto abbastanza.
 */
object FolderAsk {

    private val ASKED = booleanPreferencesKey("folder-asked")

    fun flow(context: Context): Flow<Boolean> = context.aivStore.data.map { p -> p[ASKED] ?: false }

    suspend fun remember(context: Context) {
        context.aivStore.edit { p -> p[ASKED] = true }
    }
}

object Recents {

    private val ENTRIES = stringPreferencesKey("recent")

    /** Eight, because the list has to fit under three buttons without becoming the screen. */
    private const val KEEP = 8

    /**
     * ⚠️ Tab between the two fields and newline between the entries, so no
     * escaping is needed: neither character can occur in a URL, and a name that
     * contained one would have arrived from a file system that cannot hold it.
     */
    fun flow(context: Context): Flow<List<RecentImage>> = context.aivStore.data.map { p ->
        (p[ENTRIES] ?: "").lineSequence()
            .mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size == 2 && parts[0].isNotBlank()) RecentImage(parts[0], parts[1]) else null
            }
            .take(KEEP)
            .toList()
    }

    suspend fun remember(context: Context, address: String, name: String) {
        if (!address.startsWith("http://", true) && !address.startsWith("https://", true)) return
        if (address.contains('\t') || address.contains('\n')) return
        val clean = name.replace('\t', ' ').replace('\n', ' ')
        context.aivStore.edit { p ->
            val kept = (p[ENTRIES] ?: "").lineSequence()
                .filter { it.isNotBlank() && it.substringBefore('\t') != address }
                .take(KEEP - 1)
                .toList()
            p[ENTRIES] = (listOf("$address\t$clean") + kept).joinToString("\n")
        }
    }

    suspend fun clear(context: Context) {
        context.aivStore.edit { p -> p.remove(ENTRIES) }
    }
}

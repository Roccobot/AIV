package io.github.roccobot.aiv

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.res.vectorResource
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.IconCompat
import java.io.File
import kotlin.math.roundToInt

/**
 * The actions of the long press on the app icon in the launcher, from 6.10 (his request: *voglio
 * aggiungere delle azioni utili alla pressione lunga dell'icona sulla homescreen*, choices A1, A2,
 * A4 and B1): 'Feed', 'Cerca', 'Apri un indirizzo' and the last folder opened.
 *
 * ⚠️⚠️ **All four are DYNAMIC, published from the code, and none is in a `shortcuts.xml`**: a
 * static shortcut names the package of its activity in the XML, and the app has four of them
 * (`.play` and `.debug` suffixes), so the file would need a copy per variant. From the code the
 * intent names the activity by class and holds for all of them. The cost is that the menu appears
 * after the first launch, which is when there is something to show.
 * ⚠️ **They are written at every launch** ([publish]): the labels are resolved in the language in
 * force, and a published shortcut keeps the words it was given.
 * ⚠️ **No 'Mostra nascoste'**: from the home screen it would show the hidden folders to whoever
 * holds the phone. **No 'Impostazioni'**: rarely used, and the launcher shows about four.
 * ⚠️ **The same in both variants**: the Feed, the search and the address exist in both.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 */
object Shortcuts {

    /** Opens the Feed for this time only: the view saved in the settings does not change. */
    const val FEED = "io.github.roccobot.aiv.action.FEED"
    const val SEARCH = "io.github.roccobot.aiv.action.SEARCH"
    const val ADDRESS = "io.github.roccobot.aiv.action.ADDRESS"
    /** Opens the grid of the folder in [BUCKET], named [NAME]. */
    const val FOLDER = "io.github.roccobot.aiv.action.FOLDER"
    const val BUCKET = "bucket"
    const val NAME = "name"

    /** Opens the disk folder in [PATH] in 'Cartelle di sistema': a folder pinned with 'Faro'. */
    const val TREE = "io.github.roccobot.aiv.action.TREE"
    const val PATH = "path"

    /** The actions this object answers, for [ViewerViewModel.handleIntent]. */
    val actions = setOf(FEED, SEARCH, ADDRESS, FOLDER, TREE)

    private const val ID_FOLDER = "folder"

    /** The beginning of the identifier of a folder pinned with 'Faro': the path follows. */
    private const val ID_PINNED = "faro:"

    /** The share of the icon's side the AIV folder takes: larger than an action's glyph, it is the icon. */
    private const val FOLDER_SHARE = 0.45f

    /**
     * How far the AIV folder sits below the icon's centre, in dp of the 108 of the icon, since 6.29.
     *
     * ⚠️⚠️ **HIS CHOICE, ON PREVIEWS, AND THE NUMBER IS HIS** (2026-10-16, `Cartellina`: *fa' in modo
     * che risulti la stessa centratura (bilanciamento ottico tra cartella e glifo interno)*, then
     * *1,5dp: VAI!* on three previews). Centred as a 64-unit square, the folder's body is at the
     * centre and the tab adds weight above it, so the folder reads high. He started from an eighth of
     * the drawing's side (6.075 dp) and raised it by 1.5 dp: 4.575. His SVG is not touched; the
     * whole drawing moves, as one object.
     * ⚠️ **What it comes to, measured** (`CartellinaTest`): the folder's whole box, tab included,
     * centred in the icon to a tenth of a dp; at 6.28 it sat 4.5 dp above. A drawing with another
     * outline would need its own number, so the number stays his and is not derived from the box.
     */
    internal const val FOLDER_DROP_DP = 4.575f

    /** The opacity of the AIV folder in white and in black, out of 255: *in semitrasparenza*. */
    private const val INK_WHITE = 217
    private const val INK_DARK = 140

    /** The contrast a graphic that is not text must hold on its ground. */
    private const val CONTRAST_MIN = 3.0

    /**
     * The share of the icon's side the glyph's viewport takes: a third of 108 dp is 36, so the
     * ink of a Material glyph (20 units of 24) is 30 dp inside the 72 dp the launcher shows.
     */
    private const val GLYPH_SHARE = 1f / 3f

    /** The side of an adaptive icon, in dp: the launcher crops it to its own shape. */
    private const val ICON_DP = 108

    /** The three fixed actions, in the order the launcher shows them, nearest to the icon first. */
    fun publish(context: Context) {
        val fixed = listOf(
            entry(context, "feed", FEED, R.string.view_feed, R.string.view_feed, feedGlyph(context), 0),
            entry(context, "search", SEARCH, R.string.hub_search, R.string.hub_search, Icons.Filled.Search, 1),
            entry(context, "address", ADDRESS, R.string.start_url, R.string.hub_url, Icons.Filled.Public, 2)
        )
        ShortcutManagerCompat.addDynamicShortcuts(context, fixed)
    }

    /**
     * The last folder opened becomes the fourth action, under the three fixed ones.
     *
     * ⚠️ **One entry, replaced every time**: the same identifier, so a folder opened after another
     * takes its place and does not add a fifth line.
     */
    fun lastFolder(context: Context, bucket: Long, name: String) {
        if (name.isBlank()) return
        val intent = Intent(context, ViewerActivity::class.java)
            .setAction(FOLDER)
            .putExtra(BUCKET, bucket)
            .putExtra(NAME, name)
        val info = ShortcutInfoCompat.Builder(context, ID_FOLDER)
            .setShortLabel(name)
            .setLongLabel(name)
            .setIcon(icon(context, Icons.Filled.Folder))
            .setIntent(intent)
            .setRank(3)
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(context, info)
    }

    /**
     * Takes the last folder out of the menu.
     *
     * ⚠️ **Called when the hidden folders change**: the action shows the folder's name, and a name
     * that was hidden must not stay on the home screen. The next folder opened puts one back.
     */
    fun dropFolder(context: Context) {
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(ID_FOLDER))
    }

    /** Whether the launcher takes an icon pinned by the app: not all of them do. */
    fun canPin(context: Context): Boolean = ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    /**
     * 'Faro', from the 6.20: a disk folder becomes an icon of its own on the home screen (his note C
     * on the giro of the 6.10, after his answer C of the same day).
     *
     * ⚠️⚠️ **HIS ICON, TO THE LETTER**: *adattiva, avere il nome della cartella, lo sfondo del colore
     * della cartella (se applicato) e il glifo della cartella AIV sovrapposto in semitrasparenza,
     * lasciato bianco o colorato per avere un minimo di contrasto come da norme*. So the ground is the
     * folder's colour, or the app's green without one; the glyph is his `ic_folder_home.xml`, and
     * [folderInk] picks white or the ground darkened, whichever keeps 3 to 1.
     * ⚠️ **The light variant of the colour**: the home screen does not follow the theme chosen inside
     * the app, and the light variants are the ones with more body.
     * ⚠️ **One icon per folder**: the identifier is the path, so pinning it again updates it (a new
     * colour, a new name) instead of asking for a second one. Android asks where to put it.
     */
    suspend fun pinFolder(context: Context, path: String) {
        val index = Folder.bucketOf(context, path)?.let { FolderTints.of(context, it) }
        val ground = index?.let { FRONT_TINTS.getOrNull(it) }?.light?.toArgb()
            ?: context.getColor(R.color.launcher_background)
        val name = File(path).name
        val info = ShortcutInfoCompat.Builder(context, "$ID_PINNED$path")
            .setShortLabel(name)
            .setLongLabel(name)
            .setIcon(IconCompat.createWithAdaptiveBitmap(folderBitmap(context, ground)))
            .setIntent(Intent(context, ViewerActivity::class.java).setAction(TREE).putExtra(PATH, path))
            .build()
        ShortcutManagerCompat.requestPinShortcut(context, info, null)
    }

    /** The picture of a 'Faro' icon on [ground], apart so the test bench can measure it. */
    internal fun folderBitmap(context: Context, ground: Int): Bitmap =
        iconBitmap(
            context, Glyphs.folderHome(context), ground, folderInk(ground), FOLDER_SHARE,
            dropDp = FOLDER_DROP_DP
        )

    /**
     * The colour of the AIV folder on [ground]: white in transparency, or, where white does not hold
     * 3 to 1, black in transparency, which reads as the ground darkened. 3 to 1 is the contrast of a
     * graphic that is not text (WCAG 1.4.11), his *minimo di contrasto come da norme*.
     */
    internal fun folderInk(ground: Int): Int {
        val white = ColorUtils.setAlphaComponent(android.graphics.Color.WHITE, INK_WHITE)
        val dark = ColorUtils.setAlphaComponent(android.graphics.Color.BLACK, INK_DARK)
        val contrast = { ink: Int -> ColorUtils.calculateContrast(ColorUtils.compositeColors(ink, ground), ground) }
        return if (contrast(white) >= CONTRAST_MIN) white else dark
    }

    private fun entry(
        context: Context,
        id: String,
        action: String,
        short: Int,
        long: Int,
        glyph: ImageVector,
        rank: Int
    ): ShortcutInfoCompat =
        ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(context.getString(short))
            .setLongLabel(context.getString(long))
            .setIcon(icon(context, glyph))
            .setIntent(Intent(context, ViewerActivity::class.java).setAction(action))
            .setRank(rank)
            .build()

    /** The Feed's glyph, the same file the app draws ([Glyphs.Feed]), read outside a composition. */
    private fun feedGlyph(context: Context): ImageVector =
        ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_feed)

    /**
     * The icon of an action: the app's own glyph in the launcher icon's two colours, swapped.
     *
     * ⚠️⚠️ **The glyph is the SAME ImageVector the app draws, rendered here**, and not a copy in
     * `res/drawable/`: 'Cerca' and the globe are Material glyphs, and a second file of the same
     * drawing is what `Glyphs.kt` forbids.
     * ⚠️ **An adaptive bitmap**, so every launcher crops it to its own shape, like the app icon.
     */
    private fun icon(context: Context, glyph: ImageVector): IconCompat =
        IconCompat.createWithAdaptiveBitmap(iconBitmap(context, glyph))

    /**
     * The picture of an icon, apart so the test bench can read its pixels: [glyph] in [ink] on
     * [ground], its viewport [share] of the side and centred, then [dropDp] lower. The actions take
     * the launcher icon's two colours swapped; a folder pinned with 'Faro' its own, and
     * [FOLDER_DROP_DP].
     */
    internal fun iconBitmap(
        context: Context,
        glyph: ImageVector,
        ground: Int = context.getColor(R.color.launcher_foreground),
        ink: Int = context.getColor(R.color.launcher_background),
        share: Float = GLYPH_SHARE,
        dropDp: Float = 0f
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val side = (ICON_DP * density).roundToInt()
        val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(ground)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink }
        val scale = side * share / glyph.viewportWidth
        canvas.translate(
            (side - glyph.viewportWidth * scale) / 2f,
            (side - glyph.viewportHeight * scale) / 2f + dropDp * density
        )
        canvas.scale(scale, scale)
        draw(canvas, glyph.root, paint)
        return bitmap
    }

    /**
     * Draws a group with its transformation, the way a vector drawable does.
     *
     * ⚠️ **Only the filled paths**: the glyphs used here have no strokes and no clip, and a stroke
     * painted as a fill would be a different drawing.
     */
    private fun draw(canvas: Canvas, group: VectorGroup, ink: Paint) {
        canvas.save()
        canvas.translate(group.translationX + group.pivotX, group.translationY + group.pivotY)
        canvas.rotate(group.rotation)
        canvas.scale(group.scaleX, group.scaleY)
        canvas.translate(-group.pivotX, -group.pivotY)
        for (node in group) {
            when (node) {
                is VectorGroup -> draw(canvas, node, ink)
                is VectorPath -> if (node.fill != null) {
                    val path = node.pathData.toPath()
                    path.fillType = node.pathFillType
                    canvas.drawPath(path.asAndroidPath(), ink)
                }
            }
        }
        canvas.restore()
    }
}

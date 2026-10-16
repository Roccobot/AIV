package io.github.roccobot.aiv

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.DrawableRes
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
 * Le azioni della pressione lunga sull'icona dell'app nel launcher: 'Feed', 'Cerca', 'Apri un
 * indirizzo' e l'ultima cartella aperta (scelte A1, A2, A4 e B1 dell'utente, 6.10).
 *
 * ⚠️⚠️ **Sono tutte e quattro dinamiche, pubblicate dal codice, e nessuna è in un `shortcuts.xml`**:
 * una scorciatoia statica nomina nell'XML il pacchetto della sua attività, e l'app ne ha quattro
 * (suffissi `.play` e `.debug`), quindi servirebbe una copia del file per variante. Dal codice
 * l'intent nomina l'attività per classe e vale per tutte. Il prezzo è che il menu compare dopo il
 * primo avvio.
 * ⚠️ **Si riscrivono a ogni avvio** ([publish]): le etichette si risolvono nella lingua in vigore,
 * e una scorciatoia pubblicata tiene le parole che ha ricevuto.
 * Niente 'Mostra nascoste', che dalla schermata Home mostrerebbe le cartelle nascoste a chiunque
 * abbia il telefono in mano; niente 'Impostazioni', poco usata, e il launcher ne mostra circa
 * quattro. Le azioni sono uguali nelle due varianti.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 */
object Shortcuts {

    /** Apre il Feed per questa volta sola: la vista salvata nelle impostazioni non cambia. */
    const val FEED = "io.github.roccobot.aiv.action.FEED"
    const val SEARCH = "io.github.roccobot.aiv.action.SEARCH"
    const val ADDRESS = "io.github.roccobot.aiv.action.ADDRESS"
    /** Apre la griglia della cartella [BUCKET], di nome [NAME]. */
    const val FOLDER = "io.github.roccobot.aiv.action.FOLDER"
    const val BUCKET = "bucket"
    const val NAME = "name"

    /** Apre la cartella del disco [PATH] in 'Cartelle di sistema': una cartella fissata con 'Faro'. */
    const val TREE = "io.github.roccobot.aiv.action.TREE"
    const val PATH = "path"

    /** Le azioni a cui risponde questo oggetto, per [ViewerViewModel.handleIntent]. */
    val actions = setOf(FEED, SEARCH, ADDRESS, FOLDER, TREE)

    private const val ID_FOLDER = "folder"

    /** L'inizio dell'identificativo di una cartella fissata con 'Faro': segue il percorso. */
    private const val ID_PINNED = "faro:"

    /** La parte del lato dell'icona occupata dalla cartella AIV: più del glifo di un'azione, perché è l'icona. */
    private const val FOLDER_SHARE = 0.45f

    /**
     * Di quanto la cartella AIV scende sotto il centro dell'icona, in dp dei 108 dell'icona.
     *
     * ⚠️⚠️ **Il numero è dell'utente**, scelto su tre anteprime (`Cartellina`, 6.29). Centrata come
     * quadrato di 64 unità, la cartella ha il corpo al centro e la linguetta aggiunge peso sopra,
     * quindi si legge alta: si sposta tutto il disegno, come un oggetto solo, e il suo SVG non si
     * tocca. Il risultato, presidiato da `CartellinaTest`: il riquadro intero della cartella,
     * linguetta compresa, centrato nell'icona al decimo di dp. Un disegno con un altro contorno
     * vorrebbe un numero suo, quindi il numero non si ricava dal riquadro.
     */
    internal const val FOLDER_DROP_DP = 4.575f

    /** L'opacità della cartella AIV in bianco e in nero, su 255: la semitrasparenza che ha chiesto l'utente. */
    private const val INK_WHITE = 217
    private const val INK_DARK = 140

    /** Il contrasto che una grafica che non è testo deve tenere sul suo fondo. */
    private const val CONTRAST_MIN = 3.0

    /**
     * La parte del lato dell'icona occupata dal riquadro del glifo: un terzo di 108 dp è 36, quindi
     * l'inchiostro di un glifo Material (20 unità su 24) misura 30 dp nei 72 che il launcher mostra.
     */
    private const val GLYPH_SHARE = 1f / 3f

    /** Il lato di un'icona adattiva, in dp: il launcher la ritaglia nella sua forma. */
    private const val ICON_DP = 108

    /** Le tre azioni fisse, nell'ordine in cui le mostra il launcher, dalla più vicina all'icona. */
    fun publish(context: Context) {
        val fixed = listOf(
            entry(context, "feed", FEED, R.string.view_feed, R.string.view_feed, glyph(context, R.drawable.ic_feed), 0),
            entry(context, "search", SEARCH, R.string.hub_search, R.string.hub_search, glyph(context, R.drawable.ic_m_search), 1),
            entry(context, "address", ADDRESS, R.string.start_url, R.string.hub_url, glyph(context, R.drawable.ic_m_public), 2)
        )
        ShortcutManagerCompat.addDynamicShortcuts(context, fixed)
    }

    /**
     * L'ultima cartella aperta diventa la quarta azione, sotto le tre fisse.
     *
     * ⚠️ **Una voce sola, sostituita ogni volta**: l'identificativo è lo stesso, quindi una cartella
     * aperta dopo un'altra ne prende il posto e non aggiunge una quinta riga.
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
            .setIcon(icon(context, glyph(context, R.drawable.ic_m_folder)))
            .setIntent(intent)
            .setRank(3)
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(context, info)
    }

    /**
     * Toglie l'ultima cartella dal menu.
     *
     * ⚠️ **Si chiama quando cambiano le cartelle nascoste**: l'azione mostra il nome della cartella,
     * e un nome nascosto non deve restare nella schermata Home. La prossima cartella aperta ne
     * rimette una.
     */
    fun dropFolder(context: Context) {
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(ID_FOLDER))
    }

    /** Se il launcher accetta un'icona fissata dall'app: non tutti lo fanno. */
    fun canPin(context: Context): Boolean = ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    /**
     * 'Faro': una cartella del disco diventa un'icona sua nella schermata Home (nota C dell'utente
     * sul giro della 6.10).
     *
     * ⚠️ **L'icona è quella che ha descritto l'utente**: adattiva, col nome della cartella, il fondo
     * del colore della cartella (o il verde dell'app senza colore) e il glifo `ic_folder_home.xml` in
     * semitrasparenza, bianco o del fondo scurito secondo [folderInk], quello che tiene 3 a 1.
     * Si usa la variante chiara del colore: la schermata Home non segue il tema scelto dentro l'app,
     * e le varianti chiare sono quelle con più corpo.
     * ⚠️ **Un'icona per cartella**: l'identificativo è il percorso, quindi fissarla di nuovo la
     * aggiorna (colore, nome) e non ne chiede una seconda. Android chiede dove metterla.
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

    /** L'immagine di un'icona 'Faro' su [ground], separata perché il banco di prova la misuri. */
    internal fun folderBitmap(context: Context, ground: Int): Bitmap =
        iconBitmap(
            context, Glyphs.folderHome(context), ground, folderInk(ground), FOLDER_SHARE,
            dropDp = FOLDER_DROP_DP
        )

    /**
     * Il colore della cartella AIV su [ground]: bianco in trasparenza, o, dove il bianco non tiene
     * 3 a 1, nero in trasparenza, che si legge come il fondo scurito. 3 a 1 è il contrasto di una
     * grafica che non è testo (WCAG 1.4.11).
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

    /**
     * Un glifo dell'app letto fuori da una composizione: lo stesso file che disegna l'app (il Feed è
     * [Glyphs.Feed], gli altri sono in [Glyphs.Material]), perché le scorciatoie si pubblicano
     * senza una composizione intorno.
     */
    internal fun glyph(context: Context, @DrawableRes id: Int): ImageVector =
        ImageVector.vectorResource(context.theme, context.resources, id)

    /**
     * L'icona di un'azione: il glifo dell'app nei due colori dell'icona del launcher, scambiati.
     *
     * ⚠️ **Il glifo è lo stesso `ImageVector` che disegna l'app, reso qui**: 'Cerca' e il globo sono
     * glifi Material, e una seconda copia dello stesso disegno in `res/drawable/` è quello che
     * `Glyphs.kt` vieta. È una bitmap adattiva, quindi ogni launcher la ritaglia nella sua forma,
     * come l'icona dell'app.
     */
    private fun icon(context: Context, glyph: ImageVector): IconCompat =
        IconCompat.createWithAdaptiveBitmap(iconBitmap(context, glyph))

    /**
     * L'immagine di un'icona, separata perché il banco di prova ne legga i pixel: [glyph] in [ink]
     * su [ground], col riquadro pari a [share] del lato e centrato, poi [dropDp] più in basso. Le
     * azioni usano i due colori dell'icona del launcher scambiati; una cartella fissata con 'Faro'
     * i suoi, e [FOLDER_DROP_DP].
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
     * Disegna un gruppo con la sua trasformazione, come fa un vector drawable.
     *
     * ⚠️ **Solo i tracciati pieni**: i glifi usati qui non hanno contorni né ritagli, e un contorno
     * dipinto come riempimento sarebbe un altro disegno.
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

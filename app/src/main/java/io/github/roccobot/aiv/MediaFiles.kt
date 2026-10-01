package io.github.roccobot.aiv

import java.util.Locale

/** Media formats explicitly excluded by the user, even when MediaStore calls them images. */
internal object MediaFiles {
    private val psdTypes = setOf(
        "image/vnd.adobe.photoshop", "image/x-photoshop", "image/photoshop",
        "image/psd", "image/x-psd", "application/photoshop", "application/x-photoshop",
        "application/psd", "application/x-psd"
    )

    fun ignored(name: String?, mime: String? = null): Boolean =
        name?.endsWith(".psd", ignoreCase = true) == true ||
            mime?.lowercase(Locale.ROOT) in psdTypes

    // COALESCE preserves rows from providers that do not know a name, path or MIME type.
    @Suppress("DEPRECATION")
    val selection: String = listOf(
        android.provider.MediaStore.Images.Media.DISPLAY_NAME,
        android.provider.MediaStore.Images.Media.DATA
    ).joinToString(" AND ") { "LOWER(COALESCE($it, '')) NOT LIKE '%.psd'" } +
        " AND LOWER(COALESCE(${android.provider.MediaStore.Images.Media.MIME_TYPE}, '')) NOT IN (" +
        psdTypes.joinToString(",") { "'$it'" } + ")"
}

package io.github.roccobot.aiv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URLDecoder

/**
 * The download page keeps the bare app glyph as its favicon (the user's request in the 1.46
 * round). On 2026-10-02 it was swapped for the feedback document's notepad icon, and the user
 * caught it on 2026-10-03: nothing else read the page, so nothing else could have noticed.
 */
class PaginettaTest {
    private fun root(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null && !File(dir, "publish/index.html").isFile) dir = dir.parentFile
        return requireNotNull(dir) { "publish/index.html not found above ${System.getProperty("user.dir")}" }
    }

    private fun squeeze(text: String) = text.replace(Regex("\\s+"), "")

    @Test fun `the download page favicon is the bare app glyph`() {
        val page = File(root(), "publish/index.html").readText()
        // Comments name the notepad icon on purpose; only real markup counts.
        val markup = page.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        val icons = Regex("<link[^>]*rel=\"icon\"[^>]*>").findAll(markup).map { it.value }.toList()
        assertEquals("One favicon, inline: $icons", 1, icons.size)
        assertFalse("The notepad icon belongs to the feedback document", icons[0].contains("feedback-favicon"))
        val href = Regex("href=\"([^\"]+)\"").find(icons[0])!!.groupValues[1]
        assertTrue(href.startsWith("data:image/svg+xml,"))
        val svg = URLDecoder.decode(href.removePrefix("data:image/svg+xml,"), "UTF-8")
        assertTrue("Colour #43B59E", svg.contains("fill='#43B59E'"))
        // The drawing is the app's, byte for byte: both tracings of ic_aiv_mark.xml.
        val mark = File(root(), "app/src/main/res/drawable/ic_aiv_mark.xml").readText()
        val tracings = Regex("android:pathData=\"([^\"]+)\"").findAll(mark).map { squeeze(it.groupValues[1]) }.toList()
        assertEquals(2, tracings.size)
        for (tracing in tracings) assertTrue("Tracing missing: $tracing", squeeze(svg).contains(tracing))
    }

    /**
     * **The download button takes the `github` APK by name, never the Play one** (5.10): the
     * release carries both, and the page used to take the first `.apk` in the list. The names
     * are read from `release.yml`, which writes them, and the pattern from the page, which
     * reads them, so a rename on either side fails here.
     */
    @Test fun `the download page picks the github apk and not the play one`() {
        val page = File(root(), "publish/index.html").readText()
        val js = Regex("""/(\^AIV-[^/]*)/i""").find(page)?.groupValues?.get(1)
        requireNotNull(js) { "The page no longer picks its APK with a pattern on the name" }
        val picks = Regex(js, RegexOption.IGNORE_CASE)
        val flow = File(root(), ".github/workflows/release.yml").readText()
        fun written(key: String) = Regex("""$key="([^"]+)"""").find(flow)!!.groupValues[1]
            .replace("\${{ steps.ver.outputs.version }}", "5.10")
        assertTrue("The github APK is not picked", picks.matches(written("name")))
        assertFalse("The Play APK is picked", picks.matches(written("play")))
    }
}

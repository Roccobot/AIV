package io.github.roccobot.aiv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Il passo che pubblica la release, in `release.yml`, eseguito per davvero con un `gh` finto.
 *
 * ⚠️⚠️ **NASCE DA UN DIFETTO ARRIVATO SU `main`** (6.29): il controllo sul tag che punta a un altro
 * commit leggeva come risposta anche il corpo d'errore che `gh api` scrive quando il tag non esiste
 * ancora, cioè a ogni prima pubblicazione, e la release della 6.29 si è fermata. Il controllo era
 * stato provato a mano con actionlint, che guarda la forma dello script e non quello che fa.
 * ⚠️ Lo script si prende dal workflow vero, quindi la prova segue ogni sua modifica; le uscite
 * `${{ needs.build.outputs.* }}` di GitHub si sostituiscono con un testo qualunque.
 */
class RilascioTest {

    private fun root(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null && !File(dir, ".github/workflows/release.yml").isFile) dir = dir.parentFile
        return requireNotNull(dir) { "release.yml non trovato" }
    }

    /** Lo script del passo `Attach the APK to a GitHub release`, senza il rientro dello YAML. */
    private fun script(): String {
        val lines = File(root(), ".github/workflows/release.yml").readLines()
        val step = lines.indexOfFirst { it.trim() == "- name: Attach the APK to a GitHub release" }
        val run = (step until lines.size).first { lines[it].trim() == "run: |" }
        val indent = lines[run + 1].takeWhile { it == ' ' }
        return lines.drop(run + 1)
            .takeWhile { it.isBlank() || it.startsWith(indent) }
            .joinToString("\n") { it.removePrefix(indent) }
            .replace(Regex("""\$\{\{ needs\.build\.outputs\.(\w+) }}""")) { "uscita-" + it.groupValues[1] }
    }

    /**
     * Esegue il passo con un `gh` che risponde come GitHub quando il tag [tag] punta al commit
     * dato, o non esiste (`null`). Restituisce l'uscita e quello che `gh` ha fatto.
     */
    private fun pubblica(tag: String?): Pair<Int, String> {
        val dir = Files.createTempDirectory("rilascio").toFile()
        val log = File(dir, "gh.log")
        val gh = File(dir, "gh")
        gh.writeText(
            """
            |#!/bin/bash
            |case "${'$'}1 ${'$'}2" in
            |  "api "*)
            |    if [ -z "${'$'}STUB_TAG" ]; then
            |      echo '{"message":"No commit found for SHA: v1.00","status":"422"}'
            |      exit 1
            |    fi
            |    echo "${'$'}STUB_TAG" ;;
            |  "release view") [ -n "${'$'}STUB_TAG" ] ;;
            |  "release create") echo create >> "${log.absolutePath}" ;;
            |  "release upload") echo upload >> "${log.absolutePath}" ;;
            |  *) echo "gh inatteso: ${'$'}*" >&2; exit 2 ;;
            |esac
            """.trimMargin() + "\n"
        )
        gh.setExecutable(true)
        val process = ProcessBuilder("bash", "-c", script())
            .redirectErrorStream(true)
            .apply {
                environment()["PATH"] = dir.absolutePath + File.pathSeparator + System.getenv("PATH")
                environment()["GH_REPO"] = "Roccobot/AIV"
                environment()["TAG"] = "v1.00"
                environment()["SIGNED"] = "true"
                environment()["RUNNER_TEMP"] = dir.absolutePath
                environment()["GITHUB_SHA"] = "aaaa"
                environment()["STUB_TAG"] = tag ?: ""
            }
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        val done = if (log.isFile) log.readText().trim() else ""
        dir.deleteRecursively()
        assertTrue("Il passo non è partito: $output", !output.contains("gh inatteso") && !output.contains("bad substitution"))
        return code to done
    }

    @Test
    fun `un tag che non esiste ancora si crea`() {
        assertEquals(0 to "create", pubblica(null))
    }

    @Test
    fun `lo stesso commit si ripubblica`() {
        assertEquals(0 to "upload", pubblica("aaaa"))
    }

    @Test
    fun `un tag su un altro commit ferma la release`() {
        val (code, done) = pubblica("bbbb")
        assertNotEquals(0, code)
        assertEquals("", done)
    }
}

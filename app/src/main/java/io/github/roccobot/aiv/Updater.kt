package io.github.roccobot.aiv

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.provider.Settings as SystemSettings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.LocalDate

/*
 * The self-update of AIV GitHub, since 5.60.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 *
 * ⚠️⚠️ **His request and his two choices** (2026-10-11): *quella di GitHub dovrà avere un sistema
 * di auto-aggiornamento, perché scaricare ogni volta manualmente la release è troppo scomodo e non
 * tutti usano Obtainium*. C1: AIV downloads and installs, with his confirmation. D1: it checks when
 * the app opens, at most once a day, with no background work and no notification.
 * ⚠️ **Only AIV GitHub** ([Store.updates]): AIV Play is updated by the Play Store.
 */
object Updater {

    /** The latest release of the repository, as the GitHub API serves it without a token. */
    private const val LATEST = "https://api.github.com/repos/Roccobot/AIV/releases/latest"

    /**
     * The APK of AIV GitHub among the assets of a release.
     *
     * ⚠️ **A release has two APKs since 5.10**, and `AIV-Play-x.xx.apk` must never be picked:
     * it is another app, with another identifier. The name is the one `release.yml` writes.
     */
    private val APK = Regex("""^AIV-\d+\.\d+\.apk$""")

    /** A release newer than the installed app, with what is needed to download and check it. */
    data class Release(
        val version: String,
        val url: String,
        val size: Long,
        /** The SHA-256 GitHub declares for the asset, in lowercase hexadecimal. */
        val sha256: String
    )

    /**
     * The release offered to him, which `UpdateDialog` shows at the root of the app.
     *
     * ⚠️ **It lives here and not in a screen**, because both the automatic check at opening and
     * 'Controlla ora' in the settings put it on stage, and the window must survive the screen.
     */
    var offer by mutableStateOf<Release?>(null)

    /**
     * Where the download and the installer run, for the life of the process.
     *
     * ⚠️⚠️ **NOT IN THE WINDOW, and 5.70 is why** (`5.70-01` not approved: *è stato anche
     * scaricato, ma poi non succede niente*). Until 5.70 the work ran in the window's own scope,
     * and the first thing it did was close the window: the scope was cancelled with it, the
     * download still reached its end (a blocking read does not stop), and the installer never
     * opened, with no notice. `AggiornaTest` is the proof.
     */
    private val work = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Starts [install] where closing the window cannot stop it. */
    fun start(context: Context, release: Release) {
        val app = context.applicationContext
        work.launch { install(app, release) }
    }

    /**
     * Reads the latest release from the API answer: `null` when it has no usable APK.
     *
     * ⚠️⚠️ **Without a digest there is no release**: the check he chose is the signature *and*
     * the digest, and an APK that cannot be compared with what GitHub declares is not installed.
     */
    internal fun parse(json: String): Release? {
        val root = JSONObject(json)
        if (root.optBoolean("draft") || root.optBoolean("prerelease")) return null
        val version = root.optString("tag_name").removePrefix("v")
        val assets = root.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (!APK.matches(asset.optString("name"))) continue
            val digest = asset.optString("digest")
            if (!digest.startsWith("sha256:")) return null
            return Release(
                version = version,
                url = asset.optString("browser_download_url").ifEmpty { return null },
                size = asset.optLong("size"),
                sha256 = digest.removePrefix("sha256:").lowercase()
            )
        }
        return null
    }

    /**
     * Whether [remote] is newer than [local], both SlimVer (`x.xx`).
     *
     * ⚠️ **Compared as decimals, not as text**: SlimVer always has two decimals, so `5.10` comes
     * before `5.90` either way, but `10.00` and `9.99` would compare the wrong way as strings. A
     * number that cannot be read is never newer.
     */
    internal fun newer(remote: String, local: String): Boolean {
        val a = remote.toBigDecimalOrNull() ?: return false
        val b = local.toBigDecimalOrNull() ?: return false
        return a > b
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()

    /**
     * Whether the automatic check is due: once a calendar day, his D1.
     *
     * ⚠️ **The calendar day and not 24 hours**: who opens the app every morning at about the same
     * time would otherwise skip one day out of two, a minute before the 24 hours run out.
     */
    internal fun due(last: Long?, today: Long): Boolean = last != today

    private fun prefs(context: Context) =
        context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    /**
     * Asks GitHub for the latest release, and offers it when it is newer than this app.
     *
     * @param asked `true` for 'Controlla ora': it runs even if today's check was done, and it says
     *   the outcome ('AIV è aggiornata', or the failure). The automatic check says nothing unless
     *   there is something to offer: a phone offline at opening is not news.
     */
    suspend fun check(context: Context, asked: Boolean) {
        if (!Store.updates) return
        withContext(Dispatchers.IO) { sweep(context, keep = null) }
        val today = LocalDate.now().toEpochDay()
        val store = prefs(context)
        if (!asked && !due(store.getLong(LAST, -1).takeIf { it >= 0 }, today)) return
        val release = withContext(Dispatchers.IO) {
            runCatching { parse(read(LATEST)) }
        }
        if (release.isFailure) {
            if (asked) Notices.say(context.getString(R.string.update_failed))
            return
        }
        store.edit().putLong(LAST, today).apply()
        val found = release.getOrNull()
        if (found != null && newer(found.version, BuildConfig.VERSION_NAME)) {
            offer = found
        } else if (asked) {
            Notices.say(context.getString(R.string.update_latest))
        }
    }

    /**
     * Downloads [release], checks it, and hands it to Android's installer.
     *
     * ⚠️⚠️ **The two checks he chose come before the installer**: the SHA-256 of the file must be
     * the one GitHub declares, and the APK must be signed with the key of the installed app. A file
     * that fails either is deleted and never reaches the installer, which would otherwise show an
     * error of its own with no word on why.
     * ⚠️ **The permission to install apps is asked before the download**: Android grants it on its
     * own page, and the window stays where it is, so he taps 'Aggiorna' again when he is back.
     */
    suspend fun install(context: Context, release: Release) {
        val pm = context.packageManager
        if (!pm.canRequestPackageInstalls()) {
            Notices.say(context.getString(R.string.update_permission))
            runCatching {
                context.startActivity(
                    Intent(
                        SystemSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + context.packageName)
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            return
        }
        offer = null
        Notices.say(context.getString(R.string.update_downloading), NOTICE_LONG_MS)
        val file = File(File(context.cacheDir, DIR).apply { mkdirs() }, apkName(release.version))
        val outcome = withContext(Dispatchers.IO) {
            sweep(context, keep = file.name)
            runCatching {
                val sha = download(release.url, file)
                when {
                    sha != release.sha256 -> Outcome.FAILED
                    !sameSigner(pm, context.packageName, file) -> Outcome.UNTRUSTED
                    else -> Outcome.READY
                }
            }.getOrDefault(Outcome.FAILED)
        }
        when (outcome) {
            Outcome.FAILED -> {
                file.delete()
                Notices.say(context.getString(R.string.update_failed))
            }
            Outcome.UNTRUSTED -> {
                file.delete()
                Notices.say(context.getString(R.string.update_untrusted), NOTICE_LONG_MS)
            }
            Outcome.READY -> {
                val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setDataAndType(uri, "application/vnd.android.package-archive")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: ActivityNotFoundException) {
                    Notices.say(context.getString(R.string.update_failed))
                }
            }
        }
    }

    private enum class Outcome { READY, FAILED, UNTRUSTED }

    /** The name of the downloaded APK of [version] in the cache. */
    internal fun apkName(version: String): String = "AIV-$version.apk"

    /**
     * The files of the update cache that nobody needs any more, since 6.29.
     *
     * ⚠️ **An APK stays only while it can still be installed**: one of a version newer than this
     * app, or the one being downloaded ([keep]). The others are the update already installed and
     * the ones before it, about eleven megabytes each, and until 6.28 nothing removed them.
     */
    internal fun stale(names: List<String>, current: String, keep: String?): List<String> =
        names.filter { name ->
            if (name == keep) return@filter false
            if (keep != null) return@filter true
            val version = name.removePrefix("AIV-").removeSuffix(".apk")
            name == version || !newer(version, current)
        }

    /** Deletes the [stale] files of the update cache. */
    internal fun sweep(context: Context, keep: String?) {
        val dir = File(context.cacheDir, DIR)
        val names = dir.list()?.toList() ?: return
        stale(names, BuildConfig.VERSION_NAME, keep).forEach { File(dir, it).delete() }
    }

    /** The body of [url], as text. */
    private fun read(url: String): String {
        val link = URL(url).openConnection() as HttpURLConnection
        try {
            link.connectTimeout = TIMEOUT_MS
            link.readTimeout = TIMEOUT_MS
            link.setRequestProperty("Accept", "application/vnd.github+json")
            if (link.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${link.responseCode}")
            return link.inputStream.bufferedReader().use { it.readText() }
        } finally {
            link.disconnect()
        }
    }

    /**
     * Writes [url] into [file], and returns the SHA-256 of what it wrote.
     *
     * ⚠️ **The digest is computed while the bytes arrive**: reading the file a second time would
     * cost a second pass over eleven megabytes for the same number.
     */
    private fun download(url: String, file: File): String {
        val link = URL(url).openConnection() as HttpURLConnection
        try {
            link.connectTimeout = TIMEOUT_MS
            link.readTimeout = TIMEOUT_MS
            if (link.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${link.responseCode}")
            val sha = MessageDigest.getInstance("SHA-256")
            link.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        sha.update(buffer, 0, n)
                        output.write(buffer, 0, n)
                    }
                }
            }
            return sha.digest().joinToString("") { "%02x".format(it) }
        } finally {
            link.disconnect()
        }
    }

    /**
     * Whether the APK in [file] is this app, signed with the key of the installed one.
     *
     * ⚠️ **The package name is checked too**: a file signed with the same key but carrying AIV
     * Play's identifier would be another app, and the installer would put it beside this one.
     */
    @Suppress("DEPRECATION")
    private fun sameSigner(pm: PackageManager, name: String, file: File): Boolean {
        val flags = PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_SIGNATURES
        val theirs = pm.getPackageArchiveInfo(file.path, flags) ?: return false
        if (theirs.packageName != name) return false
        val mine = pm.getPackageInfo(name, flags)
        return sameSigners(signers(mine), signers(theirs))
    }

    /**
     * The signers of a package.
     *
     * ⚠️ **`signingInfo` first, `signatures` as the fallback**: on some Android versions the info of
     * an archive leaves `signingInfo` empty, and the old field still says who signed it.
     */
    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo): Set<Signature> =
        info.signingInfo?.apkContentsSigners?.toSet()?.takeIf { it.isNotEmpty() }
            ?: info.signatures?.toSet().orEmpty()

    /** Whether two sets of signers are the same, and not empty. */
    internal fun sameSigners(a: Set<Signature>, b: Set<Signature>): Boolean = a.isNotEmpty() && a == b

    private const val LAST = "checked-day"
    private const val DIR = "updates"
    private const val TIMEOUT_MS = 15_000
    private const val BUFFER = 64 * 1024
}

/**
 * The window that offers a newer release: 'AIV x.xx è disponibile', with 'Aggiorna' and 'Più
 * tardi'.
 *
 * ⚠️ **Not a modal one** (`lowered` with a closing gesture): it asks for no input, and a tap
 * outside means 'più tardi', the same as the button. The rule lives in `Rules.md`, § '👆 Che cosa
 * fa il tocco FUORI da una finestra'.
 * ⚠️ **'Più tardi' waits for tomorrow**: the automatic check has already counted today, so the
 * offer comes back at the next day's opening, or at once with 'Controlla ora'.
 */
@Composable
fun UpdateDialog() {
    val release = Updater.offer ?: return
    val context = LocalContext.current
    val later = { Updater.offer = null }
    AlertDialog(
        onDismissRequest = later,
        modifier = Modifier.lowered(later),
        properties = loweredWindow(later),
        title = { Text(stringResource(R.string.update_title, release.version)) },
        confirmButton = {
            TextButton(onClick = { Updater.start(context, release) }) {
                Text(stringResource(R.string.update_install))
            }
        },
        dismissButton = {
            TextButton(onClick = later) { Text(stringResource(R.string.update_later)) }
        }
    )
}
